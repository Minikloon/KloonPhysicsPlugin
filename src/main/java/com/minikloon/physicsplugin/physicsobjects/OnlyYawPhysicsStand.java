package com.minikloon.physicsplugin.physicsobjects;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.minikloon.physicsplugin.BukkitPhysicsObject;
import com.minikloon.physicsplugin.util.ParticleUtils;
import com.minikloon.physicsplugin.util.JmeUtils;
import com.minikloon.physicsplugin.util.bukkit.InertStand;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import static com.minikloon.physicsplugin.util.JmeUtils.loc;
import static com.minikloon.physicsplugin.util.JmeUtils.vec;

public class OnlyYawPhysicsStand implements BukkitPhysicsObject {
    private final PhysicsSpace physicsSpace;
    private final PhysicsRigidBody cube;
    private final ArmorStand stand;

    private OnlyYawPhysicsStand(PhysicsSpace physicsSpace, PhysicsRigidBody cube, ArmorStand stand) {
        this.physicsSpace = physicsSpace;
        this.cube = cube;
        this.stand = stand;
    }

    public void updateVisuals() {
        World world = stand.getWorld();

        Quaternion rotation = cube.getPhysicsRotation();

        /*Vector3f rotOffset = InertStand.getHeadRotationOffset(rotation);
        Location tp = loc(cube.getPhysicsLocation(), world)
                .add(InertStand.HEAD_HEIGHT_OFFSET)
                .add(rotOffset.x, rotOffset.y, rotOffset.z)
                .add(rotOffset.x, rotOffset.y, 0);
        EulerAngle pose = JmeUtils.toBukkitEulerAngles(rotation);
        tp.setYaw((float) Math.toDegrees(pose.getY()));

        stand.teleport(tp);
        stand.setHeadPose(pose);*/

        Location tp = loc(cube.getPhysicsLocation(), world)
                .add(InertStand.HEAD_HEIGHT_OFFSET);
        float[] angles = rotation.toAngles(null);
        tp.setYaw((float) Math.toDegrees(-angles[1]));
        stand.teleport(tp);

        Location center = loc(cube.getPhysicsLocation(), world);
        //ParticleUtils.drawCubeParticles(center, rotation);
    }

    @Override
    public Entity getEntity() {
        return stand;
    }

    @Override
    public PhysicsSpace getPhysicsSpace() {
        return physicsSpace;
    }

    @Override
    public PhysicsCollisionObject getCollisionObject() {
        return cube;
    }

    public static OnlyYawPhysicsStand spawn(PhysicsSpace physicsSpace, Location spawnLoc) {
        Location visualLoc = spawnLoc.clone();
        visualLoc.setYaw(spawnLoc.getYaw() + 90);
        ArmorStand stand = InertStand.spawn(visualLoc, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZThhNDZjNGM1NWFlNzFmMzk3ZjZlMDk2ZWFmNzFkZDdhNmQzYmZlYTQzMjI5OTgxNDkzZDgxYzc2NGJiOTIyMCJ9fX0=");

        BoxCollisionShape collisionShape = new BoxCollisionShape(0.3f / 2);
        PhysicsRigidBody cube = new PhysicsRigidBody(collisionShape, 1f);
        physicsSpace.addCollisionObject(cube);

        cube.setPhysicsLocation(vec(spawnLoc));

        Quaternion quaternion = new Quaternion();
        quaternion.fromAngles(
                0f,
                (float) Math.toRadians(-spawnLoc.getYaw() - 90),
                0f);
        cube.setPhysicsRotation(quaternion);

        return new OnlyYawPhysicsStand(physicsSpace, cube, stand);
    }
}
