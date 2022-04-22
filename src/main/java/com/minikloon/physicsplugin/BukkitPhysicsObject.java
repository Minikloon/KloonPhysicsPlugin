package com.minikloon.physicsplugin;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.objects.PhysicsBody;
import org.bukkit.entity.Entity;

import javax.annotation.Nullable;
import java.util.Collection;

public interface BukkitPhysicsObject {
    void updateVisuals();

    @Nullable
    default Collection<? extends Entity> getEntities() {
        return null;
    }

    Entity getEntity();

    PhysicsSpace getPhysicsSpace();

    PhysicsBody getPhysicsBody();

    default void remove() {
        Collection<? extends Entity> entities = getEntities();
        if (entities == null) {
            getEntity().remove();
        } else {
            entities.forEach(Entity::remove);
        }

        getPhysicsSpace().removeCollisionObject(getPhysicsBody());
    }
}
