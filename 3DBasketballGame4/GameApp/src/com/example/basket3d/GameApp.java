package com.example.basket3d;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;

public class GameApp extends SimpleApplication {

    public enum Mode { TWO_PLAYERS_LOCAL, VS_CPU }
    public enum Difficulty { EASY, MEDIUM, HARD }

    private BulletAppState physics;
    private ViewPort viewPortP1;
    private ViewPort viewPortP2;
    private Camera camP2;

    private Node worldNode = new Node("World");
    private Node courtNode = new Node("Court");
    private Node actorsNode = new Node("Actors");

    private BallControl ballControl;
    private Player player1;
    private Player player2;
    private AIController aiController;

    private Hoop leftHoop;
    private Hoop rightHoop;

    private GameHudAppState hud;
    private GameState gameState;
    private LevelManager levelManager;

    private Mode mode = Mode.TWO_PLAYERS_LOCAL;
    private Difficulty difficulty = Difficulty.MEDIUM;

    public static void main(String[] args) {
        GameApp app = new GameApp();
        AppSettings s = new AppSettings(true);
        s.setTitle("3D Basketball - Sydney Stadium");
        s.setResolution(1600, 900);
        s.setSamples(4);
        s.setVSync(true);
        app.setSettings(s);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        initPhysics();
        setupViewports();
        setupScene();
        setupHoops();
        setupBall();

        levelManager = new LevelManager(difficulty);
        setupPlayers();

        gameState = new GameState();
        stateManager.attach(gameState);

        hud = new GameHudAppState(gameState);
        stateManager.attach(hud);
    }

    private void initPhysics() {
        physics = new BulletAppState();
        stateManager.attach(physics);
        physics.setDebugEnabled(false);
    }

    private void setupViewports() {
        // setViewPort() is on Camera, not ViewPort
        viewPortP1 = viewPort;
        cam.setViewPort(0f, 1f, 0.5f, 1f);   // P1: top half

        camP2 = cam.clone();
        camP2.setViewPort(0f, 1f, 0f, 0.5f); // P2: bottom half
        viewPortP2 = renderManager.createMainView("P2View", camP2);
        viewPortP2.setClearFlags(true, true, true);
        viewPortP2.setBackgroundColor(ColorRGBA.Black);
        viewPortP2.attachScene(rootNode);

        rootNode.attachChild(worldNode);
        worldNode.attachChild(courtNode);
        worldNode.attachChild(actorsNode);
    }

    private void setupScene() {
        DirectionalLight sun = new DirectionalLight(
                new Vector3f(-0.4f, -1f, -0.2f).normalizeLocal(), ColorRGBA.White.mult(1.0f));
        rootNode.addLight(sun);
        rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(0.25f)));

        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 2048, 4);
        dlsr.setLight(sun);
        viewPortP1.addProcessor(dlsr);
        viewPortP2.addProcessor(dlsr);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(new BloomFilter(BloomFilter.GlowMode.Scene));
        viewPortP1.addProcessor(fpp);
        viewPortP2.addProcessor(fpp);

        // Skybox — replace texture path with your own sky texture
        try {
            Spatial sky = com.jme3.util.SkyFactory.createSky(
                    assetManager, "Textures/Sky/Bright/FullskiesBlueClear03.dds",
                    com.jme3.util.SkyFactory.EnvMapType.CubeMap);
            rootNode.attachChild(sky);
        } catch (Exception e) {
            // sky texture not found, skip skybox
        }

        // Load Stadium model; fall back to a simple court if missing
        try {
            Spatial stadium = assetManager.loadModel("Models/SydneyStadium/sydney_stadium.gltf");
            stadium.setLocalScale(1f);
            stadium.setLocalTranslation(0, 0, 0);
            worldNode.attachChild(stadium);
        } catch (Exception e) {
            courtNode.attachChild(DemoGeometries.makeCourtFloor(assetManager));
        }

        // Pass the shape in the constructor — calling setCollisionShape() after
        // construction can silently fail with the native Bullet backend.
        RigidBodyControl floorPhy = new RigidBodyControl(DemoGeometries.makeCourtCollision(), 0f);
        courtNode.addControl(floorPhy);
        physics.getPhysicsSpace().add(floorPhy);
    }

    private void setupHoops() {
        float hoopHeight = 3.05f;
        float rimZOffset = 7f;

        leftHoop  = new Hoop(assetManager, physics, new Vector3f(0f, hoopHeight, -rimZOffset), true);
        rightHoop = new Hoop(assetManager, physics, new Vector3f(0f, hoopHeight,  rimZOffset), false);
        courtNode.attachChild(leftHoop.getNode());
        courtNode.attachChild(rightHoop.getNode());
    }

    private void setupBall() {
        ballControl = new BallControl(assetManager, physics);
        actorsNode.attachChild(ballControl.getNode());
    }

    private void setupPlayers() {
        Vector3f p1Pos = new Vector3f(-2f, 0.1f, 0f);
        Vector3f p2Pos = new Vector3f( 2f, 0.1f, 0f);

        player1 = new Player("P1", assetManager, physics, p1Pos);
        actorsNode.attachChild(player1.getNode());

        boolean cpu = (mode == Mode.VS_CPU);
        player2 = new Player(cpu ? "CPU" : "P2", assetManager, physics, p2Pos);
        actorsNode.attachChild(player2.getNode());

        TwoPlayerInput inputs = new TwoPlayerInput(inputManager, player1, cpu ? null : player2, ballControl, gameState);
        stateManager.attach(inputs);

        if (cpu) {
            aiController = new AIController(player2, ballControl, leftHoop, rightHoop, levelManager, gameState);
            stateManager.attach(aiController);
        }

        stateManager.attach(new ThirdPersonCamAppState(player1, cam));
        stateManager.attach(new ThirdPersonCamAppState(player2, camP2));

        ballControl.placeInHand(player1);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (leftHoop.consumeScoreEvent()) {
            gameState.scoreRight();
            ballControl.resetToCenter();
        }
        if (rightHoop.consumeScoreEvent()) {
            gameState.scoreLeft();
            ballControl.resetToCenter();
        }
    }
}
