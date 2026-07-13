import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorXYZ;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;

public class BasketballGame extends SimpleApplication implements ActionListener {

    // Game States & Modes
    public enum GameMode { PVP, VS_COMPUTER }
    public enum Difficulty { EASY, MEDIUM, HARD }

    private GameMode currentMode = GameMode.VS_COMPUTER;
    private Difficulty currentDifficulty = Difficulty.MEDIUM;

    // Physics State
    private BulletAppState bulletAppState;

    // Game Objects
    private Geometry basketball;
    private RigidBodyControl ballPhysics;
    private Node courtNode;

    // Gameplay Variables
    private int player1Score = 0;
    private int player2Score = 0;
    private BitmapText hudText;

    public static void main(String[] args) {
        BasketballGame app = new BasketballGame();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // 1. Initialize Physics Engine
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        // 2. Setup Camera and Lights
        flyCam.setMoveSpeed(20f);
        cam.setLocation(new Vector3f(0, 10, 25));
        cam.lookAt(new Vector3f(0, 5, 0), Vector3f.UNIT_Y);

        // 3. Create Environment & Basketball hoop
        initCourt();
        initHoop();

        // 4. Create Ball
        initBasketball();

        // 5. Setup Inputs (Controls)
        initInputs();

        // 6. Setup HUD UI
        initHUD();
    }

    private void initCourt() {
        // Floor
        Box floorBox = new Box(20f, 0.1f, 15f);
        Geometry floor = new Geometry("CourtFloor", floorBox);
        Material floorMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        floorMat.setColor("Color", ColorRGBA.Brown); // Hardwood color placeholder
        floor.setMaterial(floorMat);
        floor.setLocalTranslation(0, -0.1f, 0);

        // Add physics to floor so ball doesn't fall through eternity
        RigidBodyControl floorPhysics = new RigidBodyControl(0.0f); // 0 mass = static
        floor.addControl(floorPhysics);
        rootNode.attachChild(floor);
        bulletAppState.getPhysicsSpace().add(floorPhysics);
    }

    private void initHoop() {
        // Simplified Backboard
        Box board = new Box(3f, 2f, 0.2f);
        Geometry backboard = new Geometry("Backboard", board);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.White);
        backboard.setMaterial(mat);
        backboard.setLocalTranslation(0, 10f, -14f);

        RigidBodyControl boardPhysics = new RigidBodyControl(0.0f);
        backboard.addControl(boardPhysics);
        rootNode.attachChild(backboard);
        bulletAppState.getPhysicsSpace().add(boardPhysics);
    }

    private void initBasketball() {
        Sphere sphere = new Sphere(32, 32, 0.7f);
        basketball = new Geometry("Basketball", sphere);
        Material ballMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        ballMat.setColor("Color", ColorRGBA.Orange);
        basketball.setMaterial(ballMat);

        // Starting position (In front of player)
        basketball.setLocalTranslation(0, 4f, 10f);

        // Physics - mass > 0 makes it dynamic (falls and bounces)
        ballPhysics = new RigidBodyControl(1.0f);
        basketball.addControl(ballPhysics);

        // Bounciness (Restitution)
        ballPhysics.setRestitution(0.8f);

        rootNode.attachChild(basketball);
        bulletAppState.getPhysicsSpace().add(ballPhysics);
    }

    private void initInputs() {
        inputManager.addMapping("Shoot_P1", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Shoot_P2", new KeyTrigger(KeyInput.KEY_ENTER));

        inputManager.addListener(this, "Shoot_P1", "Shoot_P2");
    }

    private void initHUD() {
        guiNode.detachAllChildren();
        hudText = new BitmapText(guiFont, false);
        hudText.setSize(guiFont.getCharSet().getRenderedSize() * 1.5f);
        hudText.setColor(ColorRGBA.White);
        hudText.setText("P1: " + player1Score + "  |  P2/CPU: " + player2Score + "\nMode: " + currentMode + " | Level: " + currentDifficulty);
        hudText.setLocalTranslation(20, settings.getHeight() - 20, 0);
        guiNode.attachChild(hudText);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
            if (name.equals("Shoot_P1")) {
                shootBall(new Vector3f(0, 12f, -15f)); // Applies force toward hoop
            }
            if (name.equals("Shoot_P2") && currentMode == GameMode.PVP) {
                shootBall(new Vector3f(1f, 11f, -15f));
            }
        }
    }

    private void shootBall(Vector3f direction) {
        // Reset ball position to shooting hand before launching
        ballPhysics.setPhysicsLocation(new Vector3f(0, 4f, 10f));
        ballPhysics.setLinearVelocity(Vector3f.ZERO);
        ballPhysics.setAngularVelocity(Vector3f.ZERO);

        // Apply an impulse force to simulate a throw
        ballPhysics.applyImpulse(direction, Vector3f.ZERO);
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Handle basic AI Logic if it's the computer's turn
        if (currentMode == GameMode.VS_COMPUTER) {
            runAIModule(tpf);
        }

        // Check for Scoring Conditions (Simplistic trigger check)
        if (basketball.getLocalTranslation().getY() < 0) {
            // Reset if it goes out of bounds
            ballPhysics.setPhysicsLocation(new Vector3f(0, 4f, 10f));
            ballPhysics.setLinearVelocity(Vector3f.ZERO);
        }
    }

    private float aiTimer = 0;
    private void runAIModule(float tpf) {
        aiTimer += tpf;
        // Computer shoots automatically every 4 seconds based on difficulty
        if (aiTimer > 4.0f) {
            aiTimer = 0;
            Vector3f aiTarget;

            // Adjust accuracy dynamically based on competition levels
            switch (currentDifficulty) {
                case EASY:
                    aiTarget = new Vector3f((float)Math.random() * 4f - 2f, 10f, -15f); // High variance
                    break;
                case HARD:
                    aiTarget = new Vector3f(0, 12.2f, -14.8f); // Very accurate
                    break;
                case MEDIUM:
                default:
                    aiTarget = new Vector3f((float)Math.random() * 2f - 1f, 11.5f, -15f);
                    break;
            }
            shootBall(aiTarget);
            player2Score += (Math.random() > 0.4) ? 2 : 0; // Simulated AI score update for demo
            initHUD();
        }
    }
}