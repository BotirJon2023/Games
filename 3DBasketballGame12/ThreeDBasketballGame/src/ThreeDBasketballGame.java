import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;

public class ThreeDBasketballGame extends SimpleApplication {

    public enum GameMode {
        PLAYER_VS_PLAYER,
        PLAYER_VS_AI
    }

    public enum DifficultyLevel {
        EASY,
        MEDIUM,
        HARD
    }

    private BulletAppState physics;
    private GameMode gameMode = GameMode.PLAYER_VS_AI;
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    private PlayerController player1;
    private PlayerController player2; // or AIController
    private BallController ball;

    private Node courtNode = new Node("Court");

    public static void main(String[] args) {
        ThreeDBasketballGame app = new ThreeDBasketballGame();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        initPhysics();
        initCamera();
        initLighting();
        initCourt();
        initHoops();
        initBall();
        initPlayers();
        initInput();
    }

    private void initPhysics() {
        physics = new BulletAppState();
        stateManager.attach(physics);
    }

    private void initCamera() {
        cam.setLocation(new Vector3f(0, 10, 25));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        flyCam.setMoveSpeed(20);
    }

    private void initLighting() {
        // Add ambient + directional light for realistic shading
        // (You can refine this with shadows, HDR, etc.)
    }

    private void initCourt() {
        Box floorBox = new Box(15, 0.1f, 8);
        Geometry floorGeom = new Geometry("CourtFloor", floorBox);
        Material floorMat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        floorMat.setBoolean("UseMaterialColors", true);
        floorMat.setColor("Diffuse", ColorRGBA.Brown);
        floorMat.setColor("Ambient", ColorRGBA.Brown.mult(0.5f));
        floorGeom.setMaterial(floorMat);

        RigidBodyControl floorPhys = new RigidBodyControl(0);
        floorGeom.addControl(floorPhys);
        physics.getPhysicsSpace().add(floorPhys);

        courtNode.attachChild(floorGeom);
        rootNode.attachChild(courtNode);
    }

    private void initHoops() {
        // Left hoop
        Node leftHoop = HoopFactory.createHoop(assetManager, physics,
                new Vector3f(-10, 3, 0));
        // Right hoop
        Node rightHoop = HoopFactory.createHoop(assetManager, physics,
                new Vector3f(10, 3, 0));

        courtNode.attachChild(leftHoop);
        courtNode.attachChild(rightHoop);
    }

    private void initBall() {
        Sphere ballMesh = new Sphere(32, 32, 0.4f);
        Geometry ballGeom = new Geometry("Ball", ballMesh);
        Material ballMat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        ballMat.setBoolean("UseMaterialColors", true);
        ballMat.setColor("Diffuse", ColorRGBA.Orange);
        ballMat.setColor("Ambient", ColorRGBA.Orange.mult(0.5f));
        ballGeom.setMaterial(ballMat);

        ball = new BallController(ballGeom, physics);
        rootNode.attachChild(ballGeom);
    }

    private void initPlayers() {
        player1 = new PlayerController("Player1", assetManager, physics,
                new Vector3f(-5, 1, 0));

        if (gameMode == GameMode.PLAYER_VS_PLAYER) {
            player2 = new PlayerController("Player2", assetManager, physics,
                    new Vector3f(5, 1, 0));
        } else {
            player2 = new AIController("AIPlayer", assetManager, physics,
                    new Vector3f(5, 1, 0), difficulty, ball);
        }

        rootNode.attachChild(player1.getModel());
        rootNode.attachChild(player2.getModel());
    }

    private void initInput() {
        // Player 1 controls (WASD + Space to shoot)
        inputManager.addMapping("P1_Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("P1_Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("P1_Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("P1_Back", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("P1_Shoot", new KeyTrigger(KeyInput.KEY_SPACE));

        // Player 2 controls (Arrow keys + RCTRL to shoot) if PvP
        if (gameMode == GameMode.PLAYER_VS_PLAYER) {
            inputManager.addMapping("P2_Left", new KeyTrigger(KeyInput.KEY_LEFT));
            inputManager.addMapping("P2_Right", new KeyTrigger(KeyInput.KEY_RIGHT));
            inputManager.addMapping("P2_Forward", new KeyTrigger(KeyInput.KEY_UP));
            inputManager.addMapping("P2_Back", new KeyTrigger(KeyInput.KEY_DOWN));
            inputManager.addMapping("P2_Shoot", new KeyTrigger(KeyInput.KEY_RCONTROL));
        }

        inputManager.addListener(actionListener,
                "P1_Left", "P1_Right", "P1_Forward", "P1_Back", "P1_Shoot",
                "P2_Left", "P2_Right", "P2_Forward", "P2_Back", "P2_Shoot");
    }

    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (!isPressed) return;

            switch (name) {
                case "P1_Left":   player1.moveLeft(); break;
                case "P1_Right":  player1.moveRight(); break;
                case "P1_Forward":player1.moveForward(); break;
                case "P1_Back":   player1.moveBack(); break;
                case "P1_Shoot":  player1.shoot(ball); break;

                case "P2_Left":   player2.moveLeft(); break;
                case "P2_Right":  player2.moveRight(); break;
                case "P2_Forward":player2.moveForward(); break;
                case "P2_Back":   player2.moveBack(); break;
                case "P2_Shoot":  player2.shoot(ball); break;
            }
        }
    };

    @Override
    public void simpleUpdate(float tpf) {
        // Update AI, animations, and level logic
        player1.update(tpf);
        player2.update(tpf);
        ball.update(tpf);

        // Example: level-based competition logic
        // e.g., time limit, score target, difficulty-based AI aggressiveness
    }
}
