package com.minikloon.physicsplugin.util.bukkit;

import org.bukkit.Location;
import org.bukkit.block.Block;

import javax.annotation.Nullable;
import java.text.NumberFormat;

public final class WorldUtils {
    public static Block findFirstSolidBelow(Location loc) {
        for (int y = loc.getBlockY(); y >= 0; --y) {
            Block rel = loc.getWorld().getBlockAt(loc.getBlockX(), y, loc.getBlockZ());
            if (rel.isSolid()) {
                return rel;
            }
        }
        return null;
    }

    @Nullable
    public static Block findFirstSolidBelow(Location loc, int max) {
        Block base = loc.getBlock();
        for (int dy = 1; dy <= max; ++dy) {
            Block rel = base.getRelative(0, -dy, 0);
            if (rel.isSolid()) {
                return rel;
            }
        }
        return null;
    }

    public static String fmtLoc(Location loc) {
        NumberFormat nf = NumberFormat.getInstance();
        return nf.format(loc.getX()) + " " + nf.format(loc.getY()) + " " + nf.format(loc.getZ());
    }
}
