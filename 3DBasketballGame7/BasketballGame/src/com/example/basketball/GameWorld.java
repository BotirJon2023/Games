package com.example.basketball;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Torus;

public class GameWorld {

    private final BasketballGame app;
    private final PhysicsSpace physicsSpace;

    private final Node courtNode = new Node("Court");
    private final Node hoopsNode = new Node("Hoops");
    private final Node ballNode  = new Node("BallNode");

    private BallControl ballControl;

    private int scoreP1 = 0, scoreP2 = 0;
    private float timeRemaining = 180f;

    private AudioNode bounceSound, netSound;

    public GameWorld(BasketballGame app, PhysicsSpace physicsSpace) {
        this.app = app;
        this.physicsSpace = physicsSpace;
    }

    public void buildCourt(AssetManager am, Node root) {
        Spatial court;
        try {
            court = am.loadModel("Scenes/Court.j3o");
        } catch (Exception e) {
            try {
                court = am.loadModel("Scenes/Court.glb");
            } catch (Exception e2) {
                Geometry floor = new Geometry("Floor", new Box(12, 0.1f, 8));
                Material m = new Material(am, "Common/MatDefs/Light/Lighting.j3md");
                m.setBoolean("UseMaterialColors", true);
                m.setColor("Diffuse", new ColorRGBA(0.8f, 0.5f, 0.2f, 1f));
                floor.setMaterial(m);
                court = floor;
            }
        }
        courtNode.attachChild(court);
        root.attachChild(courtNode);

        RigidBodyControl floorPhy = new RigidBodyControl(CollisionShapeFactory.createMeshShape(court), 0f);
        court.addControl(floorPhy);
        physicsSpace.add(floorPhy);

        Spatial hoopModel;
        try {
            hoopModel = am.loadModel("Models/Hoop.glb");
        } catch (Exception e) {
            Node placeholder = new Node("HoopPlaceholder");
            Geometry ring = new Geometry("Rim", new Torus(16, 16, 0.025f, 0.23f));
            Material mr = new Material(am, "Common/MatDefs/Light/Lighting.j3md");
            mr.setBoolean("UseMaterialColors", true);
            mr.setColor("Diffuse", ColorRGBA.Orange);
            ring.setMaterial(mr);
            ring.setLocalTranslation(0, 3.05f, 0);
            placeholder.attachChild(ring);
            hoopModel = placeholder;
        }

        Node hoopN = (Node) hoopModel.clone();
        Node hoopS = (Node) hoopModel.clone();
        hoopN.setLocalTranslation(0, 0, -8.4f);
        hoopS.setLocalTranslation(0, 0,  8.4f);
        hoopsNode.attachChild(hoopN);
        hoopsNode.attachChild(hoopS);
        root.attachChild(hoopsNode);

        HoopControl hoopNorth = new HoopControl(new Vector3f(0, 3.05f, -8.4f), true,  this);
        HoopControl hoopSouth = new HoopControl(new Vector3f(0, 3.05f,  8.4f), false, this);
        hoopN.addControl(hoopNorth);
        hoopS.addControl(hoopSouth);

        try {
            bounceSound = new AudioNode(am, "Sounds/bounce.ogg", false);
            bounceSound.setPositional(true);
            netSound = new AudioNode(am, "Sounds/net.ogg", false);
            netSound.setPositional(true);
            root.attachChild(bounceSound);
            root.attachChild(netSound);
        } catch (Exception ignore) {}
    }

    public void spawnBall(AssetManager am, Vector3f pos) {
        Spatial ball;
        try {
            ball = am.loadModel("Models/Ball.glb");
        } catch (Exception e) {
            Geometry g = new Geometry("Ball", new Sphere(16, 16, 0.12f));
            Material m = new Material(am, "Common/MatDefs/Light/Lighting.j3md");
            m.setBoolean("UseMaterialColors", true);
            m.setColor("Diffuse", new ColorRGBA(0.9f, 0.4f, 0.1f, 1f));
            g.setMaterial(m);
            ball = g;
        }
        ballNode.detachAllChildren();
        ballNode.attachChild(ball);
        app.getRootNode().attachChild(ballNode);

        ballControl = new BallControl(0.62f, bounceSound);
        ballNode.addControl(ballControl);

        RigidBodyControl phy = new RigidBodyControl(CollisionShapeFactory.createDynamicMeshShape(ball), ballControl.getMass());
        phy.setRestitution(0.7f);
        phy.setFriction(0.6f);
        ball.addControl(phy);
        physicsSpace.add(phy);

        ballControl.setPhysics(phy);
        ballControl.setPosition(pos);
    }

    public void linkPossessionToNearest(PlayerController... players) {
        if (ballControl == null) return;
        float best = Float.MAX_VALUE;
        PlayerController nearest = null;
        for (PlayerController p : players) {
            if (p == null) continue;
            float d = p.getNode().getWorldTranslation().distance(ballNode.getWorldTranslation());
            if (d < best) { best = d; nearest = p; }
        }
        if (nearest != null && best < 2.2f) {
            nearest.takePossession(ballControl);
        }
    }

    public void score(boolean northHoop, int points) {
        if (northHoop) {
            scoreP2 += points;
        } else {
            scoreP1 += points;
        }
        if (netSound != null) {
            netSound.setLocalTranslation(northHoop ? new Vector3f(0, 3.05f, -8.4f) : new Vector3f(0, 3.05f, 8.4f));
            netSound.playInstance();
        }
        resetAfterScore(northHoop);
    }

    public void resetAfterScore(boolean northHoop) {
        Vector3f pos = northHoop ? new Vector3f(0, 1.2f, -6.5f) : new Vector3f(0, 1.2f, 6.5f);
        ballControl.resetBall(pos);
    }

    public void update(float tpf) {
        timeRemaining -= tpf;
        if (timeRemaining < 0) timeRemaining = 0;
    }

    public void reset(LevelManager.Level level) {
        scoreP1 = 0;
        scoreP2 = 0;
        timeRemaining = 180f;
        if (ballControl != null) ballControl.resetBall(new Vector3f(0, 1.2f, -6f));
    }

    public int getScoreP1()       { return scoreP1; }
    public int getScoreP2()       { return scoreP2; }
    public float getTimeRemaining() { return timeRemaining; }
    public BallControl getBall()  { return ballControl; }
}
