package com.minikloon.physicsplugin.boundingboxes;


import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;

public final class MinecraftBoundingBoxes {
    public static BoundingBox get(Block block) {
        return BoundingBox.of(block); // lol
    }

    public static BoxCollisionShape toShape(BoundingBox bb) {
        double width = bb.getWidthX();
        double height = bb.getHeight();
        double depth = bb.getWidthZ();

        return new BoxCollisionShape(
                (float) width / 2,
                (float) height / 2,
                (float) depth / 2);
    }
}
