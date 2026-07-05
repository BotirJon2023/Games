package com.example.basketball;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.system.AppSettings;

public class BasketballGame extends SimpleApplication implements ActionListener {

    private BulletAppState bulletAppState;
    private GameWorld world;
    private LevelManager levels;
    private HUD hud;
    private GameConfig config;

    private PlayerController player1;
    private PlayerController player2;
    private AIController ai;

    private Camera camP1, camP2;
    private ViewPort viewP1, viewP2;
    private boolean splitScreen = false;

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setTitle("3D Basketball Game");
        settings.setResolution(1600, 900);
        settings.setVSync(true);
        settings.setSamples(4);
        settings.setGammaCorrection(true);

        BasketballGame app = new BasketballGame();
        app.setShowSettings(true);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        assetManager.registerLocator("assets", FileLocator.class);
        flyCam.setEnabled(false);

        config = new GameConfig();
        config.mode = GameConfig.Mode.PVE;
        config.levelIndex = 0;

        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.setDebugEnabled(false);

        levels = new LevelManager();
        hud = new HUD(this, guiNode, guiFont);

        setupLights();
        world = new GameWorld(this, bulletAppState.getPhysicsSpace());
        world.buildCourt(assetManager, rootNode);

        setupPlayers();
        setupInputs();
        setupCameras();

        hud.showMainMenu(config, levels);
        stateManager.attach(new ScreenshotAppState("screens"));

        setPaused(true);
    }

    private void setupPlayers() {
        player1 = PlayerFactory.createHumanPlayer(assetManager, world, "Player1", new Vector3f(0, 0, -7f));
        rootNode.attachChild(player1.getNode());

        if (config.mode == GameConfig.Mode.PVP) {
            player2 = PlayerFactory.createHumanPlayer(assetManager, world, "Player2", new Vector3f(0, 0, 7f));
            rootNode.attachChild(player2.getNode());
        } else {
            PlayerController aiPawn = PlayerFactory.createAIPawn(assetManager, world, "AI", new Vector3f(0, 0, 7f));
            rootNode.attachChild(aiPawn.getNode());
            ai = new AIController(aiPawn, player1, world, levels.get(config.levelIndex));
            stateManager.attach(ai);
        }

        world.spawnBall(assetManager, new Vector3f(0, 1.2f, -6f));
        world.linkPossessionToNearest(player1, player2, ai != null ? ai.getPawn() : null);
    }

    private void setupInputs() {
        inputManager.addMapping("START", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "START");

        inputManager.addMapping("P1_LEFT",   new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("P1_RIGHT",  new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("P1_UP",     new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("P1_DOWN",   new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("P1_SHOOT",  new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("P1_SPRINT", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("P1_ACTION", new KeyTrigger(KeyInput.KEY_E));

        inputManager.addMapping("P2_LEFT",   new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("P2_RIGHT",  new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("P2_UP",     new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("P2_DOWN",   new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("P2_SHOOT",  new KeyTrigger(KeyInput.KEY_RCONTROL));
        inputManager.addMapping("P2_SPRINT", new KeyTrigger(KeyInput.KEY_RSHIFT));
        inputManager.addMapping("P2_ACTION", new KeyTrigger(KeyInput.KEY_RMENU));

        if (player1 != null) player1.bindInputs(inputManager, "P1_");
        if (player2 != null) player2.bindInputs(inputManager, "P2_");
    }

    private void setupCameras() {
        if (config.mode == GameConfig.Mode.PVP) {
            splitScreen = true;
            camP1 = cam;
            camP1.setFrustumPerspective(55, (float) settings.getWidth() / (settings.getHeight() / 2f), 0.1f, 500f);
            viewP1 = viewPort;

            camP2 = cam.clone();
            camP2.setFrustumPerspective(55, (float) settings.getWidth() / (settings.getHeight() / 2f), 0.1f, 500f);

            viewP1.setClearFlags(true, true, true);
            viewP1.setBackgroundColor(ColorRGBA.Black);
            viewP1.setViewPort(0f, 1f, 0.5f, 1f);

            viewP2 = renderManager.createMainView("P2View", camP2);
            viewP2.attachScene(rootNode);
            viewP2.setBackgroundColor(ColorRGBA.Black);
            viewP2.setViewPort(0f, 1f, 0f, 0.5f);
        } else {
            splitScreen = false;
            cam.setFrustumPerspective(60, (float) settings.getWidth() / settings.getHeight(), 0.1f, 500f);
            viewPort.setClearFlags(true, true, true);
        }
    }

    private void setupLights() {
        AmbientLight amb = new AmbientLight();
        amb.setColor(new ColorRGBA(0.3f, 0.3f, 0.35f, 1f));
        rootNode.addLight(amb);

        DirectionalLight key = new DirectionalLight();
        key.setDirection(new Vector3f(-1, -2, -1).normalizeLocal());
        key.setColor(ColorRGBA.White);
        rootNode.addLight(key);

        DirectionalLight fill = new DirectionalLight();
        fill.setDirection(new Vector3f(1, -1, 0.5f).normalizeLocal());
        fill.setColor(new ColorRGBA(0.7f, 0.7f, 0.8f, 1f));
        rootNode.addLight(fill);
    }

    @Override
    public void simpleUpdate(float tpf) {
        // FIX: update player controllers so movement/dribble/shooting work
        if (player1 != null) player1.update(tpf);
        if (player2 != null) player2.update(tpf);

        if (splitScreen) {
            if (player1 != null) player1.updateCamera(cam);
            if (player2 != null) player2.updateCamera(camP2);
        } else {
            if (player1 != null) player1.updateCamera(cam);
        }
        world.update(tpf);
        hud.update(world.getScoreP1(), world.getScoreP2(), world.getTimeRemaining());
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if ("START".equals(name) && isPressed) {
            if (isPaused()) {
                startMatch();
            } else {
                setPaused(true);
                hud.showPauseMenu();
            }
        }
    }

    public void startMatch() {
        world.reset(levels.get(config.levelIndex));
        if (ai != null) {
            ai.setDifficulty(levels.get(config.levelIndex));
        }
        setPaused(false);
        hud.hideMenu();
    }

    public void setPaused(boolean paused) {
        if (paused) {
            setSpeed(0);
        } else {
            setSpeed(1);
        }
    }

    public boolean isPaused() {
        return speed == 0f;
    }

    public void applyConfig(GameConfig cfg) {
        this.config = cfg;
        rootNode.detachAllChildren();
        stateManager.detachAllStates();
        simpleInitApp();
    }

    public PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }
}
