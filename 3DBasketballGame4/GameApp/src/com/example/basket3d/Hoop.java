package com.example.basket3d;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class Hoop implements PhysicsCollisionListener {

    private final Node node = new Node("Hoop");
    private final GhostControl scoreTrigger;
    private final BulletAppState physics;
    private boolean scored = false;

    public Hoop(AssetManager am, BulletAppState physics, Vector3f rimPos, boolean left) {
        this.physics = physics;

        try {
            Spatial hoop = am.loadModel("Models/Hoop/hoop.gltf");
            hoop.setLocalTranslation(rimPos.x, 0, rimPos.z - (left ? -0.1f : 0.1f));
            node.attachChild(hoop);
        } catch (Exception e) {
            node.attachChild(fallbackRing(am, rimPos));
        }

        RigidBodyControl stand = new RigidBodyControl(new BoxCollisionShape(new Vector3f(0.2f, 1.5f, 0.2f)), 0);
        node.addControl(stand);
        physics.getPhysicsSpace().add(stand);

        BoxCollisionShape triggerShape = new BoxCollisionShape(new Vector3f(0.22f, 0.05f, 0.22f));
        scoreTrigger = new GhostControl(triggerShape);
        Node triggerNode = new Node("HoopTrigger");
        triggerNode.setLocalTranslation(rimPos.add(0, -0.05f, 0));
        triggerNode.addControl(scoreTrigger);
        node.attachChild(triggerNode);
        physics.getPhysicsSpace().add(scoreTrigger);
        physics.getPhysicsSpace().addCollisionListener(this);
    }

    private Spatial fallbackRing(AssetManager am, Vector3f rimPos) {
        return GeometryBuilder.capsule(am, "HoopRing").move(rimPos);
    }

    public Node getNode() { return node; }

    public boolean consumeScoreEvent() {
        boolean s = scored;
        scored = false;
        return s;
    }

    @Override
    public void collision(PhysicsCollisionEvent event) {
        boolean aIsTrigger = event.getObjectA() == scoreTrigger;
        boolean bIsTrigger = event.getObjectB() == scoreTrigger;
        if (!aIsTrigger && !bIsTrigger) return;

        Object other = aIsTrigger ? event.getObjectB() : event.getObjectA();
        if (other instanceof RigidBodyControl) {
            RigidBodyControl rb = (RigidBodyControl) other;
            if (rb.getMass() > 0.5f && rb.getLinearVelocity().y < -0.5f) {
                scored = true;
            }
        }
    }
}
