package com.basketball;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;

public class BasketballGame extends SimpleApplication implements ActionListener {

    // Game modes
    public static final int MODE_2P = 1;
    public static final int MODE_VS_CPU = 2;

    // Difficulty
    public static final int EASY = 1;
    public static final int MEDIUM = 2;
    public static final int HARD = 3;

    private BulletAppState bulletAppState;
    private RigidBodyControl ballPhysics;
    private Geometry ballGeom;

    private Node player1, player2;
    private Vector3f p1Pos = new Vector3f(-4, 1, 0);
    private Vector3f p2Pos = new Vector3f(4, 1, 0);

    private int score1 = 0, score2 = 0;
    private int currentPlayer = 1; // whose turn
    private int gameMode = MODE_VS_CPU;
    private int difficulty = MEDIUM;
    private boolean ballInHand = true;
    private boolean charging = false;
    private float chargeTime = 0;
    private float maxCharge = 1.5f;

    private BitmapText hudText;
    private BitmapText powerBar;
    private BitmapText messageText;

    private Node hoop1, hoop2;
    private Vector3f hoop1Pos = new Vector3f(-8, 4.5f, 0);
    private Vector3f hoop2Pos = new Vector3f(8, 4.5f, 0);

    // AI state
    private float aiTimer = 0;
    private boolean aiReady = false;

    public static void main(String[] args) {
        BasketballGame app = new BasketballGame();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("🏀 3D Basketball - Press 1: 2P | 2: vs CPU | 3: Easy | 4: Medium | 5: Hard");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setVSync(true);
        app.setSettings(settings);
        app.setShowSettings(true);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0, -20f, 0));

        // Camera
        cam.setLocation(new Vector3f(0, 8, 18));
        cam.lookAt(new Vector3f(0, 3, 0), Vector3f.UNIT_Y);
        flyCam.setMoveSpeed(0);
        flyCam.setDragToRotate(true);

        setupLighting();
        setupCourt();
        setupHoops();
        setupBall();
        setupPlayers();
        setupHUD();
        setupKeys();

        rootNode.attachChild(player1);
        rootNode.attachChild(player2);

        showMessage("PLAYER 1 - HOLD SPACE TO SHOOT!");
    }

    private void setupLighting() {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -1f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(1.2f));
        rootNode.addLight(sun);

        DirectionalLight sun2 = new DirectionalLight();
        sun2.setDirection(new Vector3f(0.5f, -1f, 0.5f).normalizeLocal());
        sun2.setColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        rootNode.addLight(sun2);

        AmbientLight amb = new AmbientLight();
        amb.setColor(ColorRGBA.White.mult(0.4f));
        rootNode.addLight(amb);
    }

    private void setupCourt() {
        // Floor
        Box floorBox = new Box(15, 0.1f, 10);
        Geometry floor = new Geometry("Floor", floorBox);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.75f, 0.45f, 0.2f, 1f)); // wood
        floor.setMaterial(mat);
        floor.setLocalTranslation(0, 0, 0);
        rootNode.attachChild(floor);

        RigidBodyControl floorPhys = new RigidBodyControl(new BoxCollisionShape(new Vector3f(15, 0.1f, 10)), 0);
        floor.addControl(floorPhys);
        bulletAppState.getPhysicsSpace().add(floorPhys);

        // Court lines (simple)
        Box line = new Box(0.05f, 0.01f, 20);
        Geometry centerLine = new Geometry("Line", line);
        Material lineMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        lineMat.setColor("Color", ColorRGBA.White);
        centerLine.setMaterial(lineMat);
        centerLine.setLocalTranslation(0, 0.12f, 0);
        rootNode.attachChild(centerLine);
    }

    private void setupHoops() {
        hoop1 = createHoop(hoop1Pos);
        hoop2 = createHoop(hoop2Pos);
        rootNode.attachChild(hoop1);
        rootNode.attachChild(hoop2);
    }

    private Node createHoop(Vector3f pos) {
        Node hoop = new Node("hoop");

        // Backboard
        Box bb = new Box(0.1f, 1.2f, 1.5f);
        Geometry backboard = new Geometry("backboard", bb);
        Material bbMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        bbMat.setColor("Color", ColorRGBA.White);
        backboard.setMaterial(bbMat);
        backboard.setLocalTranslation(0, 1.2f, 0);
        hoop.attachChild(backboard);
        RigidBodyControl bbPhys = new RigidBodyControl(new BoxCollisionShape(new Vector3f(0.1f, 1.2f, 1.5f)), 0);
        backboard.addControl(bbPhys);
        bulletAppState.getPhysicsSpace().add(bbPhys);

        // Rim (torus approximated with thin box ring)
        Material rimMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        rimMat.setColor("Color", new ColorRGBA(1f, 0.3f, 0f, 1f));

        // Rim as 4 small boxes forming a square ring
        float r = 0.45f;
        float thickness = 0.04f;
        Vector3f rimCenter = new Vector3f(-0.6f, 0.5f, 0);

        Box rimF = new Box(thickness, thickness, r * 2);
        Geometry rimFront = new Geometry("rimF", rimF);
        rimFront.setMaterial(rimMat);
        rimFront.setLocalTranslation(rimCenter.x, rimCenter.y, rimCenter.z);
        hoop.attachChild(rimFront);
        RigidBodyControl rfPhys = new RigidBodyControl(new BoxCollisionShape(new Vector3f(thickness, thickness, r * 2)), 0);
        rimFront.addControl(rfPhys);
        bulletAppState.getPhysicsSpace().add(rfPhys);

        // Pole
        Box pole = new Box(0.1f, 3f, 0.1f);
        Geometry poleG = new Geometry("pole", pole);
        Material poleMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        poleMat.setColor("Color", ColorRGBA.Gray);
        poleG.setMaterial(poleMat);
        poleG.setLocalTranslation(0, -1.5f, 0);
        hoop.attachChild(poleG);

        hoop.setLocalTranslation(pos);
        return hoop;
    }

    private void setupBall() {
        Sphere sphere = new Sphere(32, 32, 0.24f);
        ballGeom = new Geometry("Ball", sphere);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", new ColorRGBA(0.9f, 0.4f, 0.1f, 1f));
        mat.setColor("Ambient", new ColorRGBA(0.4f, 0.2f, 0.05f, 1f));
        mat.setColor("Specular", ColorRGBA.White);
        mat.setFloat("Shininess", 20f);
        ballGeom.setMaterial(mat);
        rootNode.attachChild(ballGeom);

        ballPhysics = new RigidBodyControl(new SphereCollisionShape(0.24f), 1.0f);
        ballPhysics.setRestitution(0.75f);
        ballPhysics.setFriction(0.5f);
        ballPhysics.setLinearDamping(0.1f);
        ballPhysics.setAngularDamping(0.3f);
        ballGeom.addControl(ballPhysics);
        bulletAppState.getPhysicsSpace().add(ballPhysics);

        resetBall();
    }

    private void resetBall() {
        ballInHand = true;
        charging = false;
        chargeTime = 0;
        ballPhysics.setLinearVelocity(Vector3f.ZERO);
        ballPhysics.setAngularVelocity(Vector3f.ZERO);
        updateBallPosition();
    }

    private void setupPlayers() {
        player1 = createPlayer(ColorRGBA.Blue, "P1");
        player2 = createPlayer(ColorRGBA.Red, "P2");
        player1.setLocalTranslation(p1Pos);
        player2.setLocalTranslation(p2Pos);
    }

    private Node createPlayer(ColorRGBA color, String name) {
        Node player = new Node(name);

        // Body
        Box body = new Box(0.3f, 0.6f, 0.2f);
        Geometry bodyG = new Geometry("body", body);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        bodyG.setMaterial(mat);
        bodyG.setLocalTranslation(0, 0.6f, 0);
        player.attachChild(bodyG);

        // Head
        Sphere head = new Sphere(16, 16, 0.25f);
        Geometry headG = new Geometry("head", head);
        Material hMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        hMat.setColor("Color", new ColorRGBA(0.9f, 0.7f, 0.5f, 1f));
        headG.setMaterial(hMat);
        headG.setLocalTranslation(0, 1.4f, 0);
        player.attachChild(headG);

        // Legs
        Box leg = new Box(0.12f, 0.5f, 0.15f);
        Geometry legL = new Geometry("legL", leg);
        legL.setMaterial(mat);
        legL.setLocalTranslation(-0.15f, -0.1f, 0);
        player.attachChild(legL);

        Geometry legR = new Geometry("legR", leg);
        legR.setMaterial(mat);
        legR.setLocalTranslation(0.15f, -0.1f, 0);
        player.attachChild(legR);

        return player;
    }

    private void setupHUD() {
        BitmapFont font = guiFont;

        hudText = new BitmapText(font, false);
        hudText.setSize(font.getCharSet().getRenderedSize());
        hudText.setColor(ColorRGBA.White);
        hudText.setLocalTranslation(10, cam.getHeight() - 20, 0);
        guiNode.attachChild(hudText);

        powerBar = new BitmapText(font, false);
        powerBar.setSize(font.getCharSet().getRenderedSize() * 0.8f);
        powerBar.setColor(ColorRGBA.Yellow);
        powerBar.setLocalTranslation(10, cam.getHeight() - 60, 0);
        guiNode.attachChild(powerBar);

        messageText = new BitmapText(font, false);
        messageText.setSize(font.getCharSet().getRenderedSize() * 1.5f);
        messageText.setColor(ColorRGBA.Yellow);
        messageText.setLocalTranslation(cam.getWidth() / 2 - 200, cam.getHeight() / 2, 0);
        guiNode.attachChild(messageText);
    }

    private void setupKeys() {
        // Player 1: WASD + Space
        inputManager.addMapping("P1_Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("P1_Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("P1_Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("P1_Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("P1_Shoot", new KeyTrigger(KeyInput.KEY_SPACE));

        // Player 2: Arrows + Enter
        inputManager.addMapping("P2_Left", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("P2_Right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("P2_Up", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("P2_Down", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("P2_Shoot", new KeyTrigger(KeyInput.KEY_RETURN));

        // Mode/difficulty
        inputManager.addMapping("Mode1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("Mode2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("Diff1", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("Diff2", new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping("Diff3", new KeyTrigger(KeyInput.KEY_5));
        inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_R));

        String[] mappings = {"P1_Left", "P1_Right", "P1_Up", "P1_Down", "P1_Shoot",
                "P2_Left", "P2_Right", "P2_Up", "P2_Down", "P2_Shoot",
                "Mode1", "Mode2", "Diff1", "Diff2", "Diff3", "Reset"};
        for (String m : mappings) inputManager.addListener(this, m);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("Mode1") && isPressed) { gameMode = MODE_2P; showMessage("MODE: 2 PLAYERS"); }
        if (name.equals("Mode2") && isPressed) { gameMode = MODE_VS_CPU; showMessage("MODE: VS CPU"); }
        if (name.equals("Diff1") && isPressed) { difficulty = EASY; showMessage("DIFFICULTY: EASY"); }
        if (name.equals("Diff2") && isPressed) { difficulty = MEDIUM; showMessage("DIFFICULTY: MEDIUM"); }
        if (name.equals("Diff3") && isPressed) { difficulty = HARD; showMessage("DIFFICULTY: HARD"); }
        if (name.equals("Reset") && isPressed) { resetGame(); }

        if (currentPlayer == 1) {
            if (name.equals("P1_Left") && isPressed) p1Pos.x -= 0.5f;
            if (name.equals("P1_Right") && isPressed) p1Pos.x += 0.5f;
            if (name.equals("P1_Up") && isPressed) p1Pos.z -= 0.5f;
            if (name.equals("P1_Down") && isPressed) p1Pos.z += 0.5f;
            if (name.equals("P1_Shoot")) {
                if (isPressed && ballInHand) { charging = true; chargeTime = 0; }
                if (!isPressed && charging) shoot();
            }
        } else if (currentPlayer == 2 && gameMode == MODE_2P) {
            if (name.equals("P2_Left") && isPressed) p2Pos.x -= 0.5f;
            if (name.equals("P2_Right") && isPressed) p2Pos.x += 0.5f;
            if (name.equals("P2_Up") && isPressed) p2Pos.z -= 0.5f;
            if (name.equals("P2_Down") && isPressed) p2Pos.z += 0.5f;
            if (name.equals("P2_Shoot")) {
                if (isPressed && ballInHand) { charging = true; chargeTime = 0; }
                if (!isPressed && charging) shoot();
            }
        }
    }

    private void shoot() {
        if (!ballInHand) return;
        charging = false;

        Vector3f shooterPos = currentPlayer == 1 ? p1Pos : p2Pos;
        Vector3f targetHoop = currentPlayer == 1 ? hoop1Pos : hoop2Pos;

        // Direction to hoop
        Vector3f dir = targetHoop.subtract(shooterPos).normalizeLocal();
        float distance = shooterPos.distance(targetHoop);

        // Power based on charge time (0..1)
        float power = Math.min(chargeTime / maxCharge, 1f);
        float speed = 8f + power * 12f;

        // Add randomness based on difficulty (for CPU) or small for human
        float inaccuracy = 0f;
        if (gameMode == MODE_VS_CPU && currentPlayer == 2) {
            if (difficulty == EASY) inaccuracy = 0.25f;
            else if (difficulty == MEDIUM) inaccuracy = 0.12f;
            else inaccuracy = 0.04f;
        } else {
            inaccuracy = 0.03f;
        }

        dir.x += (float) (Math.random() - 0.5) * inaccuracy;
        dir.z += (float) (Math.random() - 0.5) * inaccuracy;
        dir.normalizeLocal();

        // Add upward arc
        Vector3f velocity = dir.mult(speed);
        velocity.y += 6f + distance * 0.3f;

        ballPhysics.setLinearVelocity(velocity);
        ballPhysics.setAngularVelocity(new Vector3f(
                (float) Math.random() * 10,
                (float) Math.random() * 10,
                (float) Math.random() * 10));

        ballInHand = false;
        showMessage("");
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Update player positions (clamped)
        p1Pos.x = Math.max(-14, Math.min(14, p1Pos.x));
        p1Pos.z = Math.max(-9, Math.min(9, p1Pos.z));
        p2Pos.x = Math.max(-14, Math.min(14, p2Pos.x));
        p2Pos.z = Math.max(-9, Math.min(9, p2Pos.z));

        player1.setLocalTranslation(p1Pos);
        player2.setLocalTranslation(p2Pos);

        // Face each other / face hoop
        if (currentPlayer == 1) {
            player1.lookAt(hoop1Pos, Vector3f.UNIT_Y);
        } else {
            player2.lookAt(hoop2Pos, Vector3f.UNIT_Y);
        }

        // Update ball in hand
        if (ballInHand) {
            updateBallPosition();
        }

        // Charging
        if (charging) {
            chargeTime += tpf;
            if (chargeTime > maxCharge) chargeTime = maxCharge;
        }

        // AI logic
        if (gameMode == MODE_VS_CPU && currentPlayer == 2 && ballInHand) {
            updateAI(tpf);
        }

        // Check scoring
        if (!ballInHand) {
            checkScore();
            // Reset if ball falls off court
            if (ballPhysics.getLinearVelocity().length() < 0.1f && ballGeom.getLocalTranslation().y < 0.5f) {
                switchTurn();
            }
            if (ballGeom.getLocalTranslation().y < -5) {
                switchTurn();
            }
        }

        // Update HUD
        String modeStr = gameMode == MODE_2P ? "2 PLAYERS" : "VS CPU (" + diffName() + ")";
        hudText.setText(String.format("P1: %d   |   P2: %d   |   Turn: P%d   |   Mode: %s   |   [R] Reset",
                score1, score2, currentPlayer, modeStr));

        if (charging) {
            int bars = (int) (chargeTime / maxCharge * 20);
            powerBar.setText("POWER: " + "█".repeat(bars) + "░".repeat(20 - bars));
        } else {
            powerBar.setText("");
        }
    }

    private void updateBallPosition() {
        Vector3f holder = currentPlayer == 1 ? p1Pos : p2Pos;
        ballPhysics.setPhysicsLocation(holder.add(0, 1.2f, 0));
    }

    private void updateAI(float tpf) {
        aiTimer += tpf;
        // CPU moves to good position
        Vector3f idealPos = hoop2Pos.add(new Vector3f(4, 0, 0));
        Vector3f diff = idealPos.subtract(p2Pos);
        if (diff.length() > 0.3f) {
            p2Pos.addLocal(diff.normalizeLocal().multLocal(3f * tpf));
        }
        // Shoot after delay
        if (aiTimer > 1.5f) {
            charging = true;
            chargeTime = maxCharge * (0.7f + (float) Math.random() * 0.3f);
            shoot();
            aiTimer = 0;
        }
    }

    private void checkScore() {
        Vector3f bp = ballGeom.getLocalTranslation();
        // Simple scoring: ball passes through hoop area with downward velocity
        if (bp.y < hoop1Pos.y && bp.y > hoop1Pos.y - 1 &&
                Math.abs(bp.x - hoop1Pos.x + 0.6f) < 0.4f &&
                Math.abs(bp.z - hoop1Pos.z) < 0.4f &&
                ballPhysics.getLinearVelocity().y < 0) {
            if (currentPlayer == 1) {
                score1 += 2;
                showMessage("🏀 PLAYER 1 SCORES! +2");
            } else {
                score2 += 2;
                showMessage("🏀 PLAYER 2 SCORES! +2");
            }
            switchTurn();
        }
        if (bp.y < hoop2Pos.y && bp.y > hoop2Pos.y - 1 &&
                Math.abs(bp.x - hoop2Pos.x + 0.6f) < 0.4f &&
                Math.abs(bp.z - hoop2Pos.z) < 0.4f &&
                ballPhysics.getLinearVelocity().y < 0) {
            if (currentPlayer == 2) {
                score2 += 2;
                showMessage("🏀 PLAYER 2 SCORES! +2");
            } else {
                score1 += 2;
                showMessage("🏀 PLAYER 1 SCORES! +2");
            }
            switchTurn();
        }
    }

    private void switchTurn() {
        currentPlayer = currentPlayer == 1 ? 2 : 1;
        aiTimer = 0;
        resetBall();
    }

    private void resetGame() {
        score1 = 0;
        score2 = 0;
        currentPlayer = 1;
        p1Pos.set(-4, 1, 0);
        p2Pos.set(4, 1, 0);
        resetBall();
        showMessage("GAME RESET!");
    }

    private void showMessage(String msg) {
        if (messageText != null) messageText.setText(msg);
    }

    private String diffName() {
        return difficulty == EASY ? "EASY" : difficulty == MEDIUM ? "MEDIUM" : "HARD";
    }
}