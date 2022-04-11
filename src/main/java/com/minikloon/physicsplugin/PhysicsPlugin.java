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
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Plane;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.minikloon.physicsplugin.boundingboxes.MinecraftBoundingBoxes;
import com.minikloon.physicsplugin.physicsobjects.OnlyYawPhysicsStand;
import com.minikloon.physicsplugin.util.ParticleUtils;
import com.minikloon.physicsplugin.util.JmeUtils;
import com.minikloon.physicsplugin.util.bukkit.InertStand;
import com.minikloon.physicsplugin.util.bukkit.WorldUtils;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.EulerAngle;

import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;

public class PhysicsPlugin extends JavaPlugin {
    private PaperCommandManager<Player> commandManager;
    private BukkitAudiences bukkitAudiences;
    private MinecraftHelp<Player> minecraftHelp;

    private PhysicsSpace physicsSpace;
    private Map<UUID, PhysicsRigidBody> playerBodies = new HashMap<>();
    private Set<BukkitPhysicsObject> physicsObjects = new HashSet<>();
    private Set<ArmorStand> stands = new HashSet<>();

    @Override
    public void onEnable() {
        if (! loadNativeLibraries()) {
            return;
        }

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

        this.bukkitAudiences = BukkitAudiences.create(this);
        this.minecraftHelp = new MinecraftHelp<>(
                "/physics help",
                bukkitAudiences::sender,
                commandManager
        );

        physicsSpace = new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);

        Bukkit.getOnlinePlayers().forEach(this::addPlayerRididBody);

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

    @Override
    public void onDisable() {
        physicsObjects.forEach(BukkitPhysicsObject::remove);
        stands.forEach(Entity::remove);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        addPlayerRididBody(e.getPlayer());
    }

    private void addPlayerRididBody(Player player) {
        BoundingBox boundingBox = player.getBoundingBox();
        BoxCollisionShape box = MinecraftBoundingBoxes.toShape(boundingBox);
        PhysicsRigidBody rigidBody = new PhysicsRigidBody(box, PhysicsBody.massForStatic);
        updatePlayerRigidBody(player, rigidBody);
        physicsSpace.addCollisionObject(rigidBody);
        playerBodies.put(player.getUniqueId(), rigidBody);
    }

    private void updatePlayerRigidBody(Player player, PhysicsRigidBody rigidBody) {
        Location playerloc = player.getLocation();

        rigidBody.setPhysicsLocation(JmeUtils.vec(playerloc));

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

    private boolean registeredFloor = false;

    @CommandMethod("physics box")
    private void box(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Location aBitInFront = eyeLoc.clone()
                .add(eyeLoc.getDirection().multiply(1.5));

        OnlyYawPhysicsStand physicsObject = OnlyYawPhysicsStand.spawn(physicsSpace, aBitInFront);
        physicsObjects.add(physicsObject);

        if (! registeredFloor) {
            registeredFloor = true;

            Block below = WorldUtils.findFirstSolidBelow(aBitInFront);
            double floorY;
            if (below == null) {
                floorY = 0;
            } else {
                floorY = below.getY() - 0.2;
                floorY += MinecraftBoundingBoxes.get(below).getHeight();
            }

            PlaneCollisionShape floor = new PlaneCollisionShape(new Plane(new Vector3f(0, 1, 0), (float) floorY + 0.5f));
            physicsSpace.addCollisionObject(new PhysicsRigidBody(floor, PhysicsRigidBody.massForStatic));
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

                    EulerAngle eulerAngle = JmeUtils.toBukkitEulerAngles(rotation);

                    stand.setHeadPose(eulerAngle);
                });
            }
        }.runTaskTimer(this, 0L, 1L);
    }
}
