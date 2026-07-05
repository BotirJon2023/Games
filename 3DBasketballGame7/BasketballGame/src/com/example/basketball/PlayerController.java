package com.example.basketball;

import com.jme3.animation.AnimClip;
import com.jme3.animation.AnimComposer;
import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class PlayerController {

    private final String name;
    private final GameWorld world;
    private final Node node = new Node();
    private Spatial model;

    private AnimComposer anim;
    private String currentAnim = "Idle";

    private boolean isHuman = true;

    private final Vector3f moveDir = new Vector3f();
    private final float speed = 4.6f;
    private final float sprintMultiplier = 1.35f;
    private boolean sprint = false;

    private boolean shootPressed = false;
    private float shootCharge = 0f;

    private BallControl possession = null;

    private final float camDistance = 6f;
    private final float camHeight   = 3f;

    public PlayerController(String name, GameWorld world) {
        this.name = name;
        this.world = world;
        node.setName(name);
    }

    public void loadModel(com.jme3.asset.AssetManager am, String path) {
        try {
            model = am.loadModel(path);
            node.attachChild(model);
        } catch (Exception e) {
            Geometry g = new Geometry("PlayerCapsule",
                    new com.jme3.scene.shape.Cylinder(16, 16, 0.4f, 1.8f, true));
            g.setLocalTranslation(0, 0.9f, 0);
            model = g;
            node.attachChild(model);
        }
        anim = model.getControl(AnimComposer.class);
        playAnim("Idle");
    }

    public Node getNode() { return node; }

    public void setHuman(boolean human) { this.isHuman = human; }

    public void bindInputs(InputManager input, String prefix) {
        input.addListener(analogListener,
                prefix + "LEFT", prefix + "RIGHT", prefix + "UP", prefix + "DOWN");
        input.addListener(actionListener,
                prefix + "SHOOT", prefix + "SPRINT", prefix + "ACTION");
    }

    private final AnalogListener analogListener = (name, value, tpf) -> {
        switch (name) {
            case "P1_LEFT":  case "P2_LEFT":  moveDir.x = -1; break;
            case "P1_RIGHT": case "P2_RIGHT": moveDir.x =  1; break;
            case "P1_UP":    case "P2_UP":    moveDir.z = -1; break;
            case "P1_DOWN":  case "P2_DOWN":  moveDir.z =  1; break;
        }
    };

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (name.endsWith("SPRINT")) {
            sprint = isPressed;
        } else if (name.endsWith("SHOOT")) {
            shootPressed = isPressed;
            if (!isPressed) tryShoot();
        } else if (name.endsWith("ACTION")) {
            if (isPressed) tryPickupOrPass();
        }
    };

    public void update(float tpf) {
        if (moveDir.lengthSquared() > FastMath.ZERO_TOLERANCE) {
            Vector3f dir = moveDir.normalize();
            float spd = speed * (sprint ? sprintMultiplier : 1f);
            node.move(dir.x * spd * tpf, 0, dir.z * spd * tpf);
            node.lookAt(node.getWorldTranslation().add(dir.x, 0, dir.z), Vector3f.UNIT_Y);
            playAnim(possession != null ? "Dribble" : "Run");
        } else {
            playAnim(possession != null ? "Dribble" : "Idle");
        }

        if (shootPressed) {
            shootCharge = Math.min(1f, shootCharge + tpf * 0.6f);
        } else {
            shootCharge = Math.max(0f, shootCharge - tpf * 0.6f);
        }

        if (possession != null) {
            possession.followHand(node, tpf);
        }

        moveDir.set(0, 0, 0);
    }

    public void updateCamera(Camera cam) {
        Vector3f target = node.getWorldTranslation();
        Vector3f back = node.getLocalRotation().mult(Vector3f.UNIT_Z).normalizeLocal();
        Vector3f camPos = target.add(back.mult(camDistance)).add(0, camHeight, 0);
        cam.setLocation(camPos);
        cam.lookAt(target.add(0, 1.2f, 0), Vector3f.UNIT_Y);
    }

    private void tryPickupOrPass() {
        if (possession == null) {
            if (world.getBall() != null) {
                float d = world.getBall().getPosition().distance(node.getWorldTranslation());
                if (d < 1.5f) takePossession(world.getBall());
            }
        } else {
            Vector3f dir = node.getLocalRotation().mult(Vector3f.UNIT_Z).negate();
            possession.pass(dir, 6f + 4f * shootCharge);
            possession = null;
            playAnim("Pass");
        }
    }

    private void tryShoot() {
        if (possession == null) return;
        Vector3f myPos = node.getWorldTranslation().add(0, 1.6f, 0);
        boolean attackNorth = myPos.z > 0;
        Vector3f hoop = new Vector3f(0, 3.05f, attackNorth ? -8.4f : 8.4f);
        float dist = myPos.distance(new Vector3f(hoop.x, myPos.y, hoop.z));
        float angleDeg = 52f;
        Vector3f impulse = ShotUtil.computeShotImpulse(
                myPos, hoop, dist, angleDeg, possession.getMass(), 9.81f, 0.02f + 0.98f * shootCharge);
        possession.shoot(impulse);
        possession = null;
        playAnim("Shoot");
    }

    public void takePossession(BallControl ball) {
        possession = ball;
        ball.attachTo(node);
        ball.setOwner(this); // FIX: properly track who owns the ball
        playAnim("Dribble");
    }

    public void onBallStolen() {
        possession = null;
    }

    private void playAnim(String animName) {
        if (anim == null) return;
        if (currentAnim.equals(animName)) return;
        AnimClip clip = anim.getAnimClip(animName);
        if (clip == null) {
            if (!currentAnim.equals("Idle")) {
                anim.setCurrentAction("Idle");
                currentAnim = "Idle";
            }
            return;
        }
        anim.setCurrentAction(animName);
        currentAnim = animName;
    }
}
