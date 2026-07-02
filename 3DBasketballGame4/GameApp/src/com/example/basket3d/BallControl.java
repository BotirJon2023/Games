package com.example.basket3d;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;

public class BallControl {

    private final Node node = new Node("Ball");
    private final RigidBodyControl body;
    private Node holder;

    public BallControl(AssetManager am, BulletAppState physics) {
        Sphere s = new Sphere(16, 16, 0.12f);
        Geometry g = new Geometry("BallGeom", s);
        g.setMaterial(MaterialFactory.basketball(am));
        node.attachChild(g);

        body = new RigidBodyControl(new SphereCollisionShape(0.12f), 0.6f);
        node.addControl(body);
        physics.getPhysicsSpace().add(body);
        body.setFriction(0.6f);
        body.setRestitution(0.65f);
        body.setLinearDamping(0.1f);
        body.setAngularDamping(0.2f);
    }

    public Node getNode() { return node; }

    public void placeInHand(Player p) {
        holder = p.getNode();
        body.setPhysicsLocation(p.getHandWorldPos());
        body.setLinearVelocity(Vector3f.ZERO);
        body.setAngularVelocity(Vector3f.ZERO);
        body.setKinematic(true);
        node.setLocalTranslation(p.getHandWorldPos());
    }

    public boolean isHeld() { return holder != null; }

    public void drop() {
        holder = null;
        body.setKinematic(false);
    }

    public void resetToCenter() {
        drop();
        body.setPhysicsLocation(new Vector3f(0, 1.5f, 0));
        body.setLinearVelocity(Vector3f.ZERO);
        body.setAngularVelocity(Vector3f.ZERO);
    }

    public void shootToward(Vector3f target, float power, float arcUpBias) {
        drop();
        Vector3f pos = body.getPhysicsLocation();
        Vector3f to = target.subtract(pos);
        Vector3f dir = new Vector3f(to.x, 0, to.z).normalizeLocal();
        float horizDist = FastMath.sqrt(to.x * to.x + to.z * to.z);
        float up = FastMath.clamp(arcUpBias + (0.4f + 0.03f * horizDist), 0.35f, 1.2f);
        Vector3f impulse = dir.mult(power).addLocal(0, up * power, 0);
        body.applyImpulse(impulse, Vector3f.ZERO);
        body.applyTorqueImpulse(new Vector3f(0, 2f, 0));
    }

    public void updateFollow(Player p) {
        if (holder == p.getNode()) {
            Vector3f hand = p.getHandWorldPos();
            node.setLocalTranslation(hand);
            body.setPhysicsLocation(hand);
        }
    }
}
