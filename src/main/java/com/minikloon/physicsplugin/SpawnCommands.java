package com.minikloon.physicsplugin;

import cloud.commandframework.annotations.CommandMethod;
import com.jme3.bullet.PhysicsSoftSpace;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.RotationOrder;
import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.bullet.joints.New6Dof;
import com.jme3.bullet.joints.motors.MotorParam;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.math.Matrix3f;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import com.minikloon.physicsplugin.boundingboxes.MinecraftBoundingBoxes;
import com.minikloon.physicsplugin.physicsobjects.ClothPhysicsObject;
import com.minikloon.physicsplugin.physicsobjects.OnlyYawPhysicsStand;
import com.minikloon.physicsplugin.physicsobjects.StandCubePhysicsObject;
import com.minikloon.physicsplugin.util.JmeUtils;
import com.minikloon.physicsplugin.util.bukkit.InertStand;
import com.minikloon.physicsplugin.util.bukkit.WorldUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class SpawnCommands {
    private final PhysicsPlugin plugin;
    private final PhysicsSpace physicsSpace;

    public SpawnCommands(PhysicsPlugin plugin) {
        this.plugin = plugin;
        this.physicsSpace = plugin.getPhysicsSpace();
    }

    @CommandMethod("physics box stand")
    private void stand(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Location spawnLoc = eyeLoc.clone()
                .add(eyeLoc.getDirection().multiply(1.5));
        spawn(player, spawnLoc, "stand");
    }

    @CommandMethod("physics box holos_full")
    private void holos_full(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Location spawnLoc = eyeLoc.clone()
                .add(eyeLoc.getDirection().multiply(8));
        spawn(player, spawnLoc, "holos_full");
    }

    @CommandMethod("physics box holos_hollow")
    private void holos_hollow(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Location spawnLoc = eyeLoc.clone()
                .add(eyeLoc.getDirection().multiply(8));
        spawn(player, spawnLoc, "holos_hollow");
    }

    @CommandMethod("physics box attached")
    private void attached(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Location bukkitSpawnLoc = eyeLoc.clone()
                .add(eyeLoc.getDirection().multiply(8));
        Vector3f spawnLoc = JmeUtils.vec(bukkitSpawnLoc);

        BukkitPhysicsObject obj = spawn(player, bukkitSpawnLoc, "holos_hollow");

        PhysicsRigidBody rigidBody = (PhysicsRigidBody) obj.getPhysicsBody();

        New6Dof joint = new New6Dof(rigidBody, new Vector3f(0, 0, 0), spawnLoc, Matrix3f.IDENTITY, Matrix3f.IDENTITY, RotationOrder.XYZ);
        joint.set(MotorParam.LowerLimit, PhysicsSpace.AXIS_X, +1f);
        joint.set(MotorParam.LowerLimit, PhysicsSpace.AXIS_Y, +1f);
        joint.set(MotorParam.LowerLimit, PhysicsSpace.AXIS_Z, +1f);
        joint.set(MotorParam.UpperLimit, PhysicsSpace.AXIS_X, -1f);
        joint.set(MotorParam.UpperLimit, PhysicsSpace.AXIS_Y, -1f);
        joint.set(MotorParam.UpperLimit, PhysicsSpace.AXIS_Z, -1f);

        joint.enableSpring(PhysicsSpace.AXIS_X, true);
        //joint.enableSpring(PhysicsSpace.AXIS_Y, true);
        joint.enableSpring(PhysicsSpace.AXIS_Z, true);
        joint.set(MotorParam.Stiffness, PhysicsSpace.AXIS_X, 25f);
        joint.set(MotorParam.Stiffness, PhysicsSpace.AXIS_Y, 10f);
        joint.set(MotorParam.Stiffness, PhysicsSpace.AXIS_Z, 25f);

        List<ArmorStand> springStands = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            ArmorStand stand = InertStand.spawn(bukkitSpawnLoc, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODMzNzVlMDc5NzA5ZGUwMjFjYTkyNGJlMTQ2ZDY0NGQ1MTliYWVjN2YzZmUzMmRjYWUxOTRmN2FjZmJhMzhiMCJ9fX0=");
            springStands.add(stand);
            if (obj instanceof StandCubePhysicsObject cube) {
                cube.attachStand(stand);
            }
        }

        new BukkitRunnable() {
            public void run() {
                if (! plugin.getPhysicsObjects().contains(obj)) {
                    cancel();
                    return;
                }

                Vector3f dir = rigidBody.getPhysicsLocation().subtract(spawnLoc);
                for (int i = 0; i < springStands.size(); ++i) {
                    float percent = (float) i / springStands.size();
                    Location standLoc = JmeUtils.loc(spawnLoc.add(dir.mult(percent)), bukkitSpawnLoc.getWorld())
                            .add(InertStand.HEAD_HEIGHT_OFFSET);
                    springStands.get(i).teleport(standLoc);
                }
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                super.cancel();
                springStands.forEach(Entity::remove);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        obj.getPhysicsSpace().addJoint(joint);
    }

    @CommandMethod("physics soft")
    private void soft(Player player) {
        World world = player.getWorld();
        Location eyeLoc = player.getEyeLocation();
        Location bukkitSpawnLoc = eyeLoc.clone()
                .add(eyeLoc.getDirection().multiply(8));

        if (! (physicsSpace instanceof PhysicsSoftSpace)) {
            player.sendMessage(ChatColor.RED + "Not a soft physics space!");
            return;
        }
        PhysicsSoftSpace softSpace = (PhysicsSoftSpace) physicsSpace;

        spawn(player, bukkitSpawnLoc, "soft");
    }

    private BukkitPhysicsObject spawn(Player player, Location spawnLoc, String type) {
        BukkitPhysicsObject physicsObject;
        if ("stand".equals(type)) {
            physicsObject = OnlyYawPhysicsStand.spawn(physicsSpace, spawnLoc);
        } else if ("holos_full".equals(type)) {
            physicsObject = StandCubePhysicsObject.spawn(physicsSpace, spawnLoc, 3f, 0.5f, false);
        } else if ("holos_hollow".equals(type)) {
            physicsObject = StandCubePhysicsObject.spawn(physicsSpace, spawnLoc, 3f, 0.5f, true);
        } else if ("soft".equals(type)) {
            physicsObject = ClothPhysicsObject.spawn((PhysicsSoftSpace) physicsSpace, spawnLoc, 0.65f, 18, 18);
        } else {
            player.sendMessage(ChatColor.RED + "Unknown box type");
            return null;
        }
        plugin.addPhysicsObject(physicsObject);

        registerFloor(spawnLoc);

        return physicsObject;
    }

    private void registerFloor(Location loc) {
        if (plugin.getFloor() != null) {
            return;
        }

        Block below = WorldUtils.findFirstSolidBelow(loc);
        double floorY;
        if (below == null) {
            floorY = 0;
        } else {
            floorY = below.getY();
            floorY += MinecraftBoundingBoxes.get(below).getHeight();
        }

        PlaneCollisionShape floorShape = new PlaneCollisionShape(new Plane(new Vector3f(0, 1, 0), (float) floorY + 0.5f));
        PhysicsRigidBody floor = new PhysicsRigidBody(floorShape, PhysicsRigidBody.massForStatic);
        floor.setFriction(2f);
        physicsSpace.addCollisionObject(floor);
        plugin.setFloor(floor);
    }
}
