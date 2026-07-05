package com.example.basketball;

import com.jme3.math.Vector3f;
import com.jme3.scene.control.AbstractControl;

public class HoopControl extends AbstractControl {

    private final Vector3f rimCenter;
    private final boolean isNorthHoop;
    private final GameWorld world;

    // FIX: prevent scoring multiple times per basket while ball lingers in detection zone
    private float scoredCooldown = 0f;

    public HoopControl(Vector3f rimCenter, boolean isNorthHoop, GameWorld world) {
        this.rimCenter = rimCenter;
        this.isNorthHoop = isNorthHoop;
        this.world = world;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (scoredCooldown > 0) {
            scoredCooldown -= tpf;
            return;
        }
        BallControl ball = world.getBall();
        if (ball == null) return;
        Vector3f p = ball.getPosition();
        if (fastDistSq2D(p, rimCenter) < 0.24f * 0.24f) {
            if ((isNorthHoop  && p.y < rimCenter.y - 0.05f && p.z < rimCenter.z + 0.05f) ||
                (!isNorthHoop && p.y < rimCenter.y - 0.05f && p.z > rimCenter.z - 0.05f)) {
                world.score(isNorthHoop, 2);
                scoredCooldown = 2f;
            }
        }
    }

    private float fastDistSq2D(Vector3f a, Vector3f b) {
        float dx = a.x - b.x;
        float dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    @Override
    protected void controlRender(com.jme3.renderer.RenderManager rm, com.jme3.renderer.ViewPort vp) {}
}
