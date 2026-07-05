package com.example.basketball;

import com.jme3.audio.AudioNode;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

public class BallControl extends AbstractControl {

    private final float mass;
    private RigidBodyControl physics;
    private PlayerController owner = null;
    private final AudioNode bounce;

    private float dribblePhase = 0f;

    public BallControl(float mass, AudioNode bounce) {
        this.mass = mass;
        this.bounce = bounce;
    }

    public void setPhysics(RigidBodyControl body) { this.physics = body; }
    public float getMass() { return mass; }

    public void setPosition(Vector3f pos) {
        if (physics != null) {
            physics.setPhysicsLocation(pos);
            physics.setLinearVelocity(Vector3f.ZERO);
            physics.setAngularVelocity(Vector3f.ZERO);
        } else {
            spatial.setLocalTranslation(pos);
        }
    }

    public Vector3f getPosition() {
        return physics != null ? physics.getPhysicsLocation() : spatial.getWorldTranslation();
    }

    public void attachTo(Node playerNode) {
        physics.setKinematic(true);
        physics.setLinearVelocity(Vector3f.ZERO);
        physics.setAngularVelocity(Vector3f.ZERO);
        // owner is set via setOwner() by PlayerController.takePossession()
    }

    // FIX: proper owner tracking — called by PlayerController.takePossession()
    public void setOwner(PlayerController pc) {
        this.owner = pc;
    }

    public void followHand(Node playerNode, float tpf) {
        dribblePhase += tpf * 10f;
        float offsetY = 0.6f + FastMath.sin(dribblePhase) * 0.12f;
        Vector3f handPos = playerNode.getWorldTranslation().add(
                playerNode.getLocalRotation().mult(new Vector3f(0.28f, offsetY, -0.35f))
        );
        setPosition(handPos);

        if (bounce != null && FastMath.abs(FastMath.cos(dribblePhase)) < 0.04f) {
            bounce.setLocalTranslation(handPos);
            bounce.playInstance();
        }
    }

    public void pass(Vector3f dir, float speed) {
        detach();
        physics.setLinearVelocity(dir.normalize().mult(speed));
    }

    public void shoot(Vector3f impulse) {
        detach();
        physics.applyImpulse(impulse, Vector3f.ZERO);
    }

    public void resetBall(Vector3f pos) {
        detach();
        setPosition(pos);
    }

    public void detach() {
        physics.setKinematic(false);
        owner = null;
    }

    public PlayerController getOwner() { return owner; }

    @Override
    protected void controlUpdate(float tpf) {}

    @Override
    protected void controlRender(com.jme3.renderer.RenderManager rm, com.jme3.renderer.ViewPort vp) {}
}
