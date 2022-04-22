package com.minikloon.physicsplugin.physicsobjects;

import com.jme3.bullet.PhysicsSoftSpace;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.bullet.util.NativeSoftBodyUtil;
import com.jme3.math.Rectangle;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.RectangleMesh;
import com.jme3.util.BufferUtils;
import com.minikloon.physicsplugin.BukkitPhysicsObject;
import com.minikloon.physicsplugin.util.JmeUtils;
import com.minikloon.physicsplugin.util.bukkit.InertStand;
import com.minikloon.physicsplugin.util.bukkit.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClothPhysicsObject implements BukkitPhysicsObject {
    private final PhysicsSoftSpace physicsSpace;

    private final World world;
    private final List<ArmorStand> standsList;

    private final PhysicsSoftBody physicsBody;

    public ClothPhysicsObject(PhysicsSoftSpace physicsSpace, List<ArmorStand> standsList, PhysicsSoftBody physicsBody) {
        this.physicsSpace = physicsSpace;
        this.standsList = standsList;
        this.physicsBody = physicsBody;
        this.world = standsList.get(0).getWorld();
    }

    @Override
    public void updateVisuals() {
        FloatBuffer locsBuffer = BufferUtils.createFloatBuffer(standsList.size() * 3);
        physicsBody.copyLocations(locsBuffer);

        for (int i = 0; i < standsList.size(); ++i) {
            ArmorStand stand = standsList.get(i);

            float[] localCoords = new float[3];
            locsBuffer.get(i * 3, localCoords);

            Location standLoc = new Location(world, localCoords[0], localCoords[1], localCoords[2])
                    .add(InertStand.HEAD_HEIGHT_OFFSET);

            stand.teleport(standLoc);
        }
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
    public PhysicsBody getPhysicsBody() {
        return physicsBody;
    }

    public static ClothPhysicsObject spawn(PhysicsSoftSpace physicsSpace, Location center, float betweenPoints, float width, float height) {
        World world = center.getWorld();
        Vector3f spawnLoc = JmeUtils.vec(center);

        PhysicsSoftBody softBody = new PhysicsSoftBody();
        softBody.setMargin(0.1f);

        Rectangle rectangle = new Rectangle(new Vector3f(0, 0, 0), new Vector3f(0, height, 0), new Vector3f(width, 0, 0));
        RectangleMesh mesh = new RectangleMesh(rectangle);
        NativeSoftBodyUtil.appendFromTriMesh(mesh, softBody);

        List<ArmorStand> stands = new ArrayList<>(softBody.countNodes());

        FloatBuffer locsBuffer = BufferUtils.createFloatBuffer(softBody.countNodes());
        softBody.copyLocations(locsBuffer);

        for (int i = 0; i < softBody.countNodes(); ++i) {
            float[] localCoords = new float[3];
            locsBuffer.get(i * 3, localCoords);

            Vector3f headPos = spawnLoc.add(localCoords[0], localCoords[1], localCoords[2]);
            ArmorStand stand = InertStand.spawn(JmeUtils.loc(headPos, world), "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmIyZDEyYjk4NTgyNjA2ZTkzNjAxMzEyMmQ0ZDRjMTM5Zjg4Yjc0NDFjMmExMGFjMWJmYzkzMmFlNWYzMTNjOSJ9fX0=");
            stands.add(stand);
        }

        softBody.applyTranslation(spawnLoc);

        physicsSpace.addCollisionObject(softBody);
        return new ClothPhysicsObject(physicsSpace, stands, softBody);
    }
}
