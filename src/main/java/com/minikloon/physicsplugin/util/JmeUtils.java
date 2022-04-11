package com.minikloon.physicsplugin.util;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.text.NumberFormat;

public final class JmeUtils {
    public static Vector3f vec(Location bukkitLoc) {
        return new Vector3f((float) bukkitLoc.getX(), (float) bukkitLoc.getY(), (float) bukkitLoc.getZ());
    }

    public static Vector3f vec(Vector bukkitVec) {
        return new Vector3f((float) bukkitVec.getX(), (float) bukkitVec.getY(), (float) bukkitVec.getZ());
    }

    public static Location loc(Vector3f jmeVec, World world) {
        return new Location(world, jmeVec.getX(), jmeVec.getY(), jmeVec.getZ());
    }

    public static Vector vec(Vector3f jmeVec) {
        return new Vector(jmeVec.x, jmeVec.y, jmeVec.z);
    }

    // https://github.com/mrdoob/three.js/blob/dev/src/math/Euler.js 'YZX'
    public static EulerAngle toBukkitEulerAngles(Quaternion quaternion) {
        Matrix3f mat = quaternion.toRotationMatrix();

        float wtf = mat.get(0, 1);
        float x;
        float z = FastMath.asin(FastMath.clamp(wtf, -1f, 1f));
        float y;
        if (Math.abs(wtf) < 0.9999f) {
            x = FastMath.atan2(-mat.get(1, 2), mat.get(1, 1));
            y = FastMath.atan2(-mat.get(2, 0), mat.get(0, 0));
        } else {
            x = 0;
            y = FastMath.atan2(mat.get(0, 2), mat.get(2, 2));
        }

        boolean rev = false;
        if (Math.abs(x) == FastMath.PI && Math.abs(y) == FastMath.PI) {
            rev = true;
            z = FastMath.TWO_PI - z;
        }

        NumberFormat nf = NumberFormat.getInstance();
        Bukkit.broadcastMessage(rev + ""
                + " x=" + nf.format(Math.toDegrees(x))
                + " y=" + nf.format(Math.toDegrees(y))
                + " z=" + nf.format(Math.toDegrees(z))
        );

        return new EulerAngle(x, -y, z);
    }

    public static EulerAngle toBukkitAngles2(Quaternion quaternion) {
        float[] angles = new float[3];

        float w = quaternion.getW();
        float x = quaternion.getX();
        float y = quaternion.getY();
        float z = quaternion.getZ();

        float sqw = w * w;
        float sqx = x * x;
        float sqy = y * y;
        float sqz = z * z;
        float unit = sqx + sqy + sqz + sqw; // if normalized is one, otherwise
        // is correction factor
        float test = x * y + z * w;
        if (test > 0.4999 * unit) { // singularity at north pole
            angles[1] = 2 * FastMath.atan2(x, w);
            angles[2] = FastMath.HALF_PI;
            angles[0] = 0;
        } else if (test < -0.4999 * unit) { // singularity at south pole
            angles[1] = -2 * FastMath.atan2(x, w);
            angles[2] = -FastMath.HALF_PI;
            angles[0] = 0;
        } else {
            angles[1] = FastMath.atan2(2 * y * w - 2 * x * z, sqx - sqy - sqz + sqw); // yaw or heading
            angles[2] = FastMath.asin(2 * test / unit); // roll or bank
            angles[0] = FastMath.atan2(2 * x * w - 2 * y * z, -sqx + sqy - sqz + sqw); // pitch or attitude
        }

        return new EulerAngle(angles[0], -angles[1], angles[2]);
    }
}
