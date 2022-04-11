package com.minikloon.physicsplugin.util.bukkit;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public final class InertStand {
    public static final Vector HEAD_HEIGHT_OFFSET = new Vector(0, -1.8, 0);

    public static ArmorStand spawn(Location headLoc) {
        Location spawnLoc = headLoc.clone().add(HEAD_HEIGHT_OFFSET);
        ArmorStand stand = (ArmorStand) headLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        stand.setGravity(false);
        stand.setVisible(false);
        stand.setCollidable(false);
        stand.setMarker(true);
        return stand;
    }

    public static ArmorStand spawn(Location headLoc, ItemStack helmet) {
        ArmorStand stand = spawn(headLoc);
        stand.getEquipment().setHelmet(helmet);
        return stand;
    }

    public static ArmorStand spawn(Location headLoc, String skinUrl) {
        return spawn(headLoc, SkullUtils.createItem(skinUrl));
    }

    public static Vector3f getHeadRotationOffset2(Quaternion rotation) {
        Vector3f headVec = new Vector3f(0, 0.3f, 0);
        Matrix3f mat = rotation.toRotationMatrix();

        float wtf = mat.get(0, 1);
        float x;
        float y;
        boolean singularity = false;
        if (Math.abs(wtf) < 0.9999f) {
            x = FastMath.atan2(-mat.get(1, 2), mat.get(1, 1));
            y = FastMath.atan2(-mat.get(2, 0), mat.get(0, 0));
        } else {
            x = 0;
            y = FastMath.atan2(mat.get(0, 2), mat.get(2, 2));
            singularity = true;
        }

        boolean reverse = !(Math.abs(x) == FastMath.PI && Math.abs(y) == FastMath.PI);

        if (reverse && singularity) {
            //reverse = false;
        }

        //Bukkit.broadcastMessage("reverse: " + reverse + " " + singularity);

        mat.multLocal(headVec);
        return headVec.multLocal(reverse ? -1 : 1);
    }

    public static Vector3f getHeadRotationOffset(Quaternion rotation) {
        Vector3f headVec = new Vector3f(0, 0.3f, 0);
        rotation.toRotationMatrix().multLocal(headVec);
        return headVec.multLocal(-1);
    }
}
