package com.minikloon.physicsplugin.util;

import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.stream.Stream;

public final class ParticleUtils {
    public static void drawCubeParticles(Location center, Quaternion rotation) {
        World world = center.getWorld();

        world.getPlayers().forEach(player -> {
            Matrix3f matrix3f = rotation.toRotationMatrix();

            Stream.of(
                    new Vector3f(1, 1, 1),
                    new Vector3f(1, 1, -1),
                    new Vector3f(1, -1, 1),
                    new Vector3f(1, -1, -1),
                    new Vector3f(-1, 1, 1),
                    new Vector3f(-1, 1, -1),
                    new Vector3f(-1, -1, 1),
                    new Vector3f(-1, -1, -1)
            ).forEach(corner -> {
                corner.multLocal(0.3f);
                matrix3f.multLocal(corner);
                Location loc = center.clone()
                        .add(corner.x, corner.y, corner.z);
                player.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0f, 0f, 0f, 0);
            });

            Vector3f dir = matrix3f.multLocal(new Vector3f(1, 0, 0));
            drawLineParticles(player, Particle.CRIT, center, dir);

            if (! dir.equals(Vector3f.UNIT_Z)) {
                Vector3f up = dir.cross(Vector3f.UNIT_Z.mult(-1));
                drawLineParticles(player, Particle.SMALL_FLAME, center, up);
            }
        });
    }

    public static void drawLineParticles(Player player, Particle particle, Location start, Vector3f dir) {
        for (float i = 1; i <= 3; i += 0.1) {
            Vector3f offset = dir.mult(i);
            Location particleLoc = start.clone().add(offset.x, offset.y, offset.z);
            player.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
        }
    }
}
