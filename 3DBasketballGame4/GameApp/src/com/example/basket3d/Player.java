package com.example.basket3d;

import com.jme3.anim.AnimComposer;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class Player {

    private final Node node = new Node();
    private final BetterCharacterControl charControl;
    private AnimComposer anim;
    private final Vector3f moveDir = new Vector3f();
    private boolean isShooting;

    public Player(String name, AssetManager am, BulletAppState physics, Vector3f spawn) {
        node.setName(name);
        Spatial model;
        try {
            String path = name.equals("P1") ? "Models/Players/player1.gltf" : "Models/Players/player2.gltf";
            model = am.loadModel(path);
        } catch (Exception e) {
            model = GeometryBuilder.capsule(am, name);
        }
        node.attachChild(model);

        charControl = new BetterCharacterControl(0.35f, 1.8f, 75f);
        node.addControl(charControl);
        physics.getPhysicsSpace().add(charControl);
        charControl.setGravity(new Vector3f(0, -25f, 0));
        charControl.warp(spawn);

        anim = model.getControl(AnimComposer.class);
        playIdle();
    }

    public Node getNode() { return node; }

    public void setMoveDir(Vector3f dir) { moveDir.set(dir); }

    public Vector3f getForward() {
        Vector3f fwd = moveDir.lengthSquared() > 0
                ? moveDir.normalize()
                : node.getWorldRotation().mult(Vector3f.UNIT_Z);
        return new Vector3f(fwd.x, 0, fwd.z).normalizeLocal();
    }

    public void update(float tpf) {
        if (moveDir.lengthSquared() > 0) {
            charControl.setWalkDirection(moveDir.mult(4.0f));
            Quaternion look = new Quaternion();
            look.lookAt(new Vector3f(moveDir.x, 0, moveDir.z), Vector3f.UNIT_Y);
            node.setLocalRotation(look);
            playRun();
        } else {
            charControl.setWalkDirection(Vector3f.ZERO);
            if (!isShooting) playIdle();
        }
    }

    public void playIdle() {
        if (anim != null) anim.setCurrentAction("idle");
    }

    public void playRun() {
        if (anim != null) anim.setCurrentAction("run");
    }

    public void playShoot() {
        isShooting = true;
        if (anim != null) anim.setCurrentAction("shoot");
    }

    public void endShoot() {
        isShooting = false;
        playIdle();
    }

    public Vector3f getHandWorldPos() {
        Vector3f base = node.getWorldTranslation().add(0, 1.4f, 0);
        return base.add(getForward().mult(0.5f));
    }
}
