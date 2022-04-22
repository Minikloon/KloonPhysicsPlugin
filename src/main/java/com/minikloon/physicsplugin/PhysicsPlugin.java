package com.minikloon.physicsplugin;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import com.jme3.bullet.PhysicsSoftSpace;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.minikloon.physicsplugin.boundingboxes.MinecraftBoundingBoxes;
import com.minikloon.physicsplugin.util.ParticleUtils;
import com.minikloon.physicsplugin.util.bukkit.InertStand;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.logging.Level;

import static com.minikloon.physicsplugin.util.JmeUtils.*;

public class PhysicsPlugin extends JavaPlugin implements Listener {
    private PaperCommandManager<Player> commandManager;
    private BukkitAudiences bukkitAudiences;
    private MinecraftHelp<Player> minecraftHelp;

    private PhysicsSoftSpace physicsSpace;
    private Map<UUID, PhysicsRigidBody> playerBodies = new HashMap<>();
    private Set<BukkitPhysicsObject> physicsObjects = new HashSet<>();
    private Set<ArmorStand> stands = new HashSet<>();
    private PhysicsRigidBody floor;

    @Override
    public void onEnable() {
        if (! loadNativeLibraries()) {
            return;
        }

        float worldSize = 1_000f;
        physicsSpace = new PhysicsSoftSpace(new Vector3f(-worldSize, 0, -worldSize), new Vector3f(worldSize, 256, worldSize), PhysicsSpace.BroadphaseType.DBVT);

        try {
            commandManager = new PaperCommandManager<>(this,
                    CommandExecutionCoordinator.simpleCoordinator(),
                    sender -> (Player) sender,
                    player -> player);
            commandManager.registerBrigadier();
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Error with commands", t);
        }

        AnnotationParser<Player> annotationParser = new AnnotationParser<>(commandManager, Player.class, params -> SimpleCommandMeta.empty());
        annotationParser.parse(this);
        annotationParser.parse(new SpawnCommands(this));

        this.bukkitAudiences = BukkitAudiences.create(this);
        this.minecraftHelp = new MinecraftHelp<>(
                "/physics help",
                bukkitAudiences::sender,
                commandManager
        );

        Bukkit.getOnlinePlayers().forEach(this::addPlayerRigidBody);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                PhysicsRigidBody rigidBody = playerBodies.get(player.getUniqueId());
                updatePlayerRigidBody(player, rigidBody);
            });

            physicsSpace.update(0.05f);
            physicsObjects.forEach(BukkitPhysicsObject::updateVisuals);
        }, 0L, 1L);
    }

    private boolean loadNativeLibraries() {
        getLogger().log(Level.INFO, "Loading native libraries...");
        long startMs = System.currentTimeMillis();
        try {
            CustomNativeLibraryLoader.extractAndLoad(this, "/native/windows/x86_64/bulletjme.dll");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error extracting/loading native libraries", e);
            return false;
        }
        getLogger().log(Level.INFO, "Loaded native libraries in " + NumberFormat.getInstance().format(System.currentTimeMillis() - startMs) + " ms");
        return true;
    }

    public PhysicsSpace getPhysicsSpace() {
        return physicsSpace;
    }

    public Set<BukkitPhysicsObject> getPhysicsObjects() {
        return physicsObjects;
    }

    public Set<ArmorStand> getStands() {
        return stands;
    }

    public void addPhysicsObject(BukkitPhysicsObject obj) {
        physicsObjects.add(obj);
    }

    public void setFloor(PhysicsRigidBody floor) {
        this.floor = floor;
    }

    public PhysicsRigidBody getFloor() {
        return floor;
    }

    @Override
    public void onDisable() {
        physicsObjects.forEach(BukkitPhysicsObject::remove);
        stands.forEach(Entity::remove);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        addPlayerRigidBody(e.getPlayer());
    }

    @EventHandler
    public void onHitMidair(PlayerInteractEvent e) {
        if (! e.getAction().isLeftClick()) {
            return;
        }

        Player player = e.getPlayer();
        World world = player.getWorld();

        Location eyeLoc = player.getEyeLocation();
        float range = 30;

        Vector3f start = vec(eyeLoc);
        Vector3f dir = vec(eyeLoc.getDirection());
        Vector3f end = start.add(dir.mult(range));

        List<PhysicsRayTestResult> collisions = physicsSpace.rayTest(start, end);

        Particle particle = Particle.SMALL_FLAME;
        for (PhysicsRayTestResult result : collisions) {
            PhysicsCollisionObject collisionObject = result.getCollisionObject();
            if (! (collisionObject instanceof PhysicsRigidBody rigidBody)) {
                continue;
            }
            if (rigidBody == floor) {
                continue;
            }

            particle = Particle.SOUL_FIRE_FLAME;

            rigidBody.applyCentralForce(dir.mult(500));
            //rigidBody.applyCentralImpulse(dir.mult(10));
            player.sendMessage("Force applied!");

            Vector3f diff = end.subtract(start);
            Vector3f hitLoc = start.add(diff.mult(result.getHitFraction()));
            Vector3f normal = result.getHitNormalLocal();
            ThreadLocalRandom rand = ThreadLocalRandom.current();
            for (int i = 0; i < 10; ++i) {
                Function<Float, Float> directionMult = x -> x * (1.0f + (float) rand.nextGaussian() * 0.5f);
                Vector3f particleDir = new Vector3f(directionMult.apply(normal.x), directionMult.apply(normal.y), directionMult.apply(normal.z));
                player.spawnParticle(Particle.FLAME, loc(hitLoc, world), 0, particleDir.x, particleDir.y, particleDir.z, 0.05f);
            }

            break;
        }

        for (float i = 0.5f; i < range; i += 0.33) {
            Location particleLoc = loc(start.add(dir.mult(i)), world);
            player.spawnParticle(particle, particleLoc, 1, 0f, 0f, 0f, 0f);
        }
    }

    private void addPlayerRigidBody(Player player) {
        org.bukkit.util.BoundingBox boundingBox = player.getBoundingBox();
        BoxCollisionShape box = MinecraftBoundingBoxes.toShape(boundingBox);
        PhysicsRigidBody rigidBody = new PhysicsRigidBody(box, PhysicsBody.massForStatic);
        updatePlayerRigidBody(player, rigidBody);
        physicsSpace.addCollisionObject(rigidBody);
        playerBodies.put(player.getUniqueId(), rigidBody);
    }

    private void updatePlayerRigidBody(Player player, PhysicsRigidBody rigidBody) {
        Location playerloc = player.getLocation();

        rigidBody.setPhysicsLocation(vec(playerloc));

        Quaternion rotation = new Quaternion();
        rotation.fromAngles(0f, (float) Math.toRadians(playerloc.getYaw()), 0f);
        rigidBody.setPhysicsRotation(rotation);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        PhysicsRigidBody rigidBody = playerBodies.remove(player.getUniqueId());
        if (rigidBody != null) {
            physicsSpace.removeCollisionObject(rigidBody);
        }
    }

    @CommandMethod("sbclear")
    private void sbclear(Player player) {
        player.performCommand("clear");
    }

    @CommandMethod("physics help [query]")
    @CommandDescription("It helps")
    private void help(Player player, @Argument("query") @Greedy String query) {
        minecraftHelp.queryCommands(query == null ? "" : query, player);
    }

    @CommandMethod("makeabigfloor")
    private void makeabigfloor(Player player) {
        Block base = player.getLocation().getBlock();
        int radius = 50;
        for (int x = -radius; x <= radius; ++x) {
            for (int z = -radius; z <= radius; ++z) {
                for (int y = -20; y <= 20; ++y) {
                    if (y == -20) {
                        base.getRelative(x, y, z).setType(Material.IRON_BLOCK);
                    } else {
                        base.getRelative(x, y, z).setType(Material.AIR);
                    }
                }
            }
        }
    }

    @CommandMethod("physics standrot <mode>")
    private void standrot(Player player, @Argument("mode") int mode) {
        Location eyeLoc = player.getEyeLocation();

        Location aBitInFront = eyeLoc.clone()
                .add(eyeLoc.getDirection().multiply(1.5));
        aBitInFront.setYaw(0);

        ArmorStand stand = InertStand.spawn(aBitInFront, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZThhNDZjNGM1NWFlNzFmMzk3ZjZlMDk2ZWFmNzFkZDdhNmQzYmZlYTQzMjI5OTgxNDkzZDgxYzc2NGJiOTIyMCJ9fX0=");
        stands.add(stand);

        if (mode == 0) {
            return;
        }

        World world = stand.getWorld();

        new BukkitRunnable() {
            private int tick = 0;
            public void run() {
                float angle = ((float) tick / 20) * 360 / 6;
                ++tick;

                Quaternion rotation = new Quaternion();

                if (mode == 1) {
                    rotation.fromAngles(0, (float) Math.toRadians(angle), 0);
                } else if (mode == 2) {
                    rotation.fromAngles((float) Math.toRadians(angle), 0, 0);
                } else if (mode == 3) {
                    rotation.fromAngles((float) 0, 0, (float) Math.toRadians(angle));
                } else if (mode == 4) {
                    rotation.fromAngles((float) Math.toRadians(180), 0, (float) Math.toRadians(angle));
                }

                world.getPlayers().forEach(player -> {
                    ParticleUtils.drawCubeParticles(aBitInFront, rotation);

                    Vector3f rotOffset = InertStand.getHeadRotationOffset(rotation);
                    stand.teleport(aBitInFront.clone()
                            .add(InertStand.HEAD_HEIGHT_OFFSET)
                            .add(rotOffset.x, rotOffset.y, rotOffset.z));

                    EulerAngle eulerAngle = toBukkitEulerAngles(rotation);

                    stand.setHeadPose(eulerAngle);
                });
            }
        }.runTaskTimer(this, 0L, 1L);
    }
}
