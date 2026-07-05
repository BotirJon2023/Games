package com.example.basketball;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public class ShotUtil {

    public static Vector3f computeShotImpulse(Vector3f src, Vector3f dst, float planarDist,
                                              float angleDeg, float mass, float g, float powerScale) {
        float angle = angleDeg * FastMath.DEG_TO_RAD;
        float dy = dst.y - src.y;
        float cos = FastMath.cos(angle);
        float sin = FastMath.sin(angle);
        float denom = 2f * cos * cos * (planarDist * FastMath.tan(angle) - dy);
        float v2 = (g * planarDist * planarDist) / Math.max(0.001f, denom);
        float v = FastMath.sqrt(Math.max(0.1f, v2)) * powerScale;

        Vector3f flatDir = dst.subtract(src);
        flatDir.y = 0;
        flatDir.normalizeLocal();

        Vector3f vel = flatDir.mult(v * cos).add(0, v * sin, 0);
        return vel.mult(mass);
    }
}
