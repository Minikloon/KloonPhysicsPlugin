package com.minikloon.physicsplugin.physicsobjects;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.minikloon.physicsplugin.BukkitPhysicsObject;
import com.minikloon.physicsplugin.util.ParticleUtils;
import com.minikloon.physicsplugin.util.bukkit.InertStand;
import com.minikloon.physicsplugin.util.bukkit.SkullUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.minikloon.physicsplugin.util.JmeUtils.*;

public class StandCubePhysicsObject implements BukkitPhysicsObject {
    private final PhysicsSpace physicsSpace;

    private final List<CubePoint> cubePoints;
    private final List<ArmorStand> standsList;

    private final List<ArmorStand> attachedStands = new ArrayList<>();

    private final float edgeSize;
    private final float step;

    private final PhysicsRigidBody rigidBody;

    public StandCubePhysicsObject(PhysicsSpace physicsSpace, List<CubePoint> cubePoints, float edgeSize, float step, PhysicsRigidBody rigidBody) {
        this.physicsSpace = physicsSpace;
        this.cubePoints = cubePoints;
        this.standsList = cubePoints.stream().map(c -> c.stand).collect(Collectors.toList());
        this.edgeSize = edgeSize;
        this.step = step;
        this.rigidBody = rigidBody;
    }

    public void attachStand(ArmorStand stand) {
        attachedStands.add(stand);
    }

    public List<ArmorStand> getAttachedStands() {
        return attachedStands;
    }

    @Override
    public void updateVisuals() {
        World world = cubePoints.get(0).stand.getWorld();
        Vector3f center = rigidBody.getPhysicsLocation();
        Quaternion rotation = rigidBody.getPhysicsRotation();
        Map<CubePoint, Location> locs = getLocs(world, cubePoints, center, rotation, edgeSize, step);
        cubePoints.forEach(point -> {
            if (point.stand == null) {
                return;
            }

            Location loc = locs.get(point);
            point.stand.teleport(loc);
        });

        //ParticleUtils.drawCubeParticles(loc(center, world), rotation, edgeSize / 2);
    }

    @Override
    public void remove() {
        BukkitPhysicsObject.super.remove();
        getAttachedStands().forEach(Entity::remove);
    }

    @Nullable
    @Override
    public Collection<? extends Entity> getEntities() {
        return standsList;
    }

    @Override
    public Entity getEntity() {
        return null;
    }

    @Override
    public PhysicsSpace getPhysicsSpace() {
        return physicsSpace;
    }

    @Override
    public PhysicsRigidBody getPhysicsBody() {
        return rigidBody;
    }

    private static final List<String> COLORS = Arrays.asList(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjhhZmExNTU1ZTlmODc2NDgxZTNjNDI5OWVjNmU5MWQyMmI0MDc1ZTY3ZTU4ZWY4MGRjZDE5MGFjZTY1MTlmIn19fQ==",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGU5YjI3ZmNjZDgwOTIxYmQyNjNjOTFkYzUxMWQwOWU5YTc0NjU1NWU2YzljYWQ1MmU4NTYyZWQwMTgyYTJmIn19fQ==",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZkZTNiZmNlMmQ4Y2I3MjRkZTg1NTZlNWVjMjFiN2YxNWY1ODQ2ODRhYjc4NTIxNGFkZDE2NGJlNzYyNGIifX19",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTMyYWUyY2I4ZDJhZTYxNTE0MWQyYzY1ODkyZjM2NGZjYWRkZjczZmRlYzk5YmUxYWE2ODc0ODYzZWViNWMifX19",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmM0ODg2ZWYzNjJiMmM4MjNhNmFhNjUyNDFjNWM3ZGU3MWM5NGQ4ZWM1ODIyYzUxZTk2OTc2NjQxZjUzZWEzNSJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzY0MTY4MmY0MzYwNmM1YzlhZDI2YmM3ZWE4YTMwZWU0NzU0N2M5ZGZkM2M2Y2RhNDllMWMxYTI4MTZjZjBiYSJ9fX0="
    );

    public static StandCubePhysicsObject spawn(PhysicsSpace physicsSpace, Location center, float edgeSize, float step, boolean hollow) {
        Quaternion rotation = new Quaternion().fromAngles(0, (float) Math.toRadians(center.getYaw()), 0f);

        int steps = (int) (edgeSize / step);

        List<CubePoint> points = new ArrayList<>(steps * steps * steps);
        for (int x = 0; x < steps; ++x) {
            for (int y = 0; y < steps; ++y) {
                for (int z = 0; z < steps; ++z) {
                    boolean isSide = x == 0 || x == steps - 1
                            || y == 0 || y == steps - 1
                            || z == 0 || z == steps - 1;
                    if (!hollow || isSide) {
                        points.add(new CubePoint(x, y, z));
                    }
                }
            }
        }

        Map<CubePoint, Location> locs = getLocs(center.getWorld(), points, vec(center), rotation, edgeSize, step);

        String helmet = COLORS.get(ThreadLocalRandom.current().nextInt(COLORS.size()));

        points.forEach(point -> {
            Location loc = locs.get(point);
            ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);

            stand.getEquipment().setHelmet(SkullUtils.createItem(helmet));
            stand.setSmall(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setVisible(false);
            stand.setCollidable(false);

            point.stand = stand;
        });

        BoxCollisionShape box = new BoxCollisionShape(edgeSize / 2);
        PhysicsRigidBody rigidBody = new PhysicsRigidBody(box);
        rigidBody.setEnableSleep(false);
        physicsSpace.addCollisionObject(rigidBody);

        rigidBody.setMass(10f);

        rigidBody.setPhysicsLocation(vec(center));
        rigidBody.setPhysicsRotation(rotation);

        return new StandCubePhysicsObject(physicsSpace, points, edgeSize, step, rigidBody);
    }

    private static Map<CubePoint, Location> getLocs(World world, List<CubePoint> points, Vector3f center, Quaternion rotation, float edgeSize, float step) {
        Matrix3f matrix = rotation.toRotationMatrix();

        Map<CubePoint, Location> locs = new HashMap<>(points.size());
        points.forEach(point -> {
            Vector3f offset = new Vector3f(point.x, point.y, point.z)
                    .multLocal(step)
                    .subtractLocal(edgeSize / 2, edgeSize / 2, edgeSize / 2);

            matrix.multLocal(offset);

            Location loc = loc(center, world)
                    .add(offset.x, offset.y, offset.z)
                    .add(vec(matrix.mult(vec(InertStand.SMALL_HEAD_SIZE)).multLocal(0.8f)))
                    .add(InertStand.SMALL_HEAD_HEIGHT_OFFSET);

            locs.put(point, loc);
        });

        return locs;
    }

    private static class CubePoint {
        private final int x;
        private final int y;
        private final int z;

        private ArmorStand stand;

        private CubePoint(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
