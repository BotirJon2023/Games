import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ExtremeSnowboardingGame extends Application {

    // App config
    private static final int APP_WIDTH = 1280;
    private static final int APP_HEIGHT = 720;

    // World/Terrain config
    private static final int TERRAIN_GRID_X = 220;   // mesh grid density (x dimension)
    private static final int TERRAIN_GRID_Z = 220;   // mesh grid density (z dimension)
    private static final float TERRAIN_SCALE = 4.0f; // spacing between grid points
    private static final float TERRAIN_HEIGHT = 60.0f; // vertical amplitude
    private static final long TERRAIN_SEED = 1337L;
    private static final float TERRAIN_STEEPNESS = 0.5f; // noise amplitude shaping

    // Physics constants
    private static final float GRAVITY = 35.0f;       // m/s^2 effective (scaled)
    private static final float FRICTION_GROUND = 0.75f; // ground friction factor
    private static final float FRICTION_AIR = 0.02f;  // air friction factor
    private static final float TURN_ACCEL = 2.5f;     // turning acceleration
    private static final float MAX_TURN_RATE = 1.8f;  // clamp radians/s turn rate
    private static final float FORWARD_PUSH = 28.0f;  // forward acceleration when holding W
    private static final float BRAKE_FORCE = 35.0f;   // braking deceleration
    private static final float JUMP_IMPULSE = 22.0f;  // vertical impulse for jump
    private static final float BOARD_LENGTH = 2.2f;
    private static final float BOARD_WIDTH = 0.32f;
    private static final float BOARD_THICK = 0.06f;

    // Game flow constants
    private static final int NUM_TREES = 140;
    private static final int NUM_ROCKS = 80;
    private static final float OBSTACLE_SPREAD = 0.85f; // portion of terrain to place obstacles
    private static final float COLLISION_RADIUS = 0.8f; // for rough bounding collisions
    private static final float RESPAWN_DELAY = 1.5f; // seconds after crash before respawn

    // Camera modes and behavior
    private static final float CAMERA_DISTANCE = 11.0f;
    private static final float CAMERA_HEIGHT = 3.5f;
    private static final float CAMERA_LAG_POS = 0.15f;
    private static final float CAMERA_LAG_LOOK = 0.2f;
    private static final float CAMERA_FOV = 70.0f;

    // Particles
    private static final int MAX_PARTICLES = 320;
    private static final float PARTICLE_EMIT_RATE = 400f; // per second while carving
    private static final float PARTICLE_LIFETIME = 1.0f; // seconds
    private static final float PARTICLE_SPEED = 7.0f;
    private static final float PARTICLE_SIZE = 0.06f;

    // Tricks thresholds
    private static final float MIN_TRICK_AIRTIME = 0.3f;
    private static final float ROTATION_FOR_360 = (float) (2 * Math.PI);
    private static final float ROTATION_FOR_180 = (float) Math.PI;

    // UI
    private static final Font HUD_FONT = Font.font("Consolas", 20.0);
    private static final Font HUD_FONT_BIG = Font.font("Consolas", 38.0);
    private static final Paint HUD_COLOR = Color.WHITE;

    // Root and scene graph
    private Group worldRoot;          // nodes inside 3D world subscene
    private SubScene worldScene3D;    // 3D subscene
    private Scene mainScene;          // top-level scene
    private StackPane rootPane;       // stacks 3D + HUD

    // Game objects
    private Terrain terrain;
    private Snowboarder player;
    private FollowCamera followCamera;
    private AmbientLight ambientLight;
    private PointLight sunLight;

    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    // HUD overlay
    private Canvas hudCanvas;
    private GraphicsContext hudGC;

    // Input
    private final InputState input = new InputState();

    // Game state variables
    private boolean paused = false;
    private boolean debug = false;
    private double lastFrameTimeSec = 0;
    private double timeSinceCrash = 0;
    private Random rng = new Random(2024);

    // Scoring
    private double distanceTravelled = 0;
    private double score = 0;
    private double airtimeAccum = 0;
    private boolean currentlyAirborne = false;
    private double trickYawAccum = 0;
    private double trickPitchAccum = 0;
    private double trickRollAccum = 0;
    private String lastTrickName = "";

    // Camera mode
    private CameraMode cameraMode = CameraMode.THIRD_PERSON;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        worldRoot = new Group();

        // Build 3D world
        terrain = new Terrain(TERRAIN_GRID_X, TERRAIN_GRID_Z, TERRAIN_SCALE, TERRAIN_HEIGHT, TERRAIN_SEED);
        MeshView terrainMesh = terrain.buildMeshView(TERRAIN_STEEPNESS);
        worldRoot.getChildren().add(terrainMesh);

        // Lighting
        ambientLight = new AmbientLight(Color.color(0.35, 0.35, 0.45));
        sunLight = new PointLight(Color.color(1.0, 0.95, 0.9));
        sunLight.setTranslateX(TERRAIN_GRID_X * TERRAIN_SCALE * 0.35);
        sunLight.setTranslateY(-150);
        sunLight.setTranslateZ(-TERRAIN_GRID_Z * TERRAIN_SCALE * 0.4);
        worldRoot.getChildren().addAll(ambientLight, sunLight);

        // Sky dome (inside-out sphere)
        Sphere sky = new Sphere(Math.max(TERRAIN_GRID_X, TERRAIN_GRID_Z) * TERRAIN_SCALE * 1.2);
        sky.setCullFace(CullFace.FRONT); // render inside
        sky.setMaterial(Materials.skyGradient());
        sky.setTranslateX((TERRAIN_GRID_X - 1) * TERRAIN_SCALE * 0.5);
        sky.setTranslateZ((TERRAIN_GRID_Z - 1) * TERRAIN_SCALE * 0.5);
        sky.setTranslateY(-2000); // high center so horizon looks low
        worldRoot.getChildren().add(sky);

        // Obstacles
        generateObstacles();

        // Player
        player = new Snowboarder();
        Vector3 startPos = new Vector3((TERRAIN_GRID_X - 1) * TERRAIN_SCALE * 0.5f, 0, 10f);
        startPos.y = terrain.sampleHeight(startPos.x, startPos.z) - 0.5f;
        player.setPosition(startPos);
        worldRoot.getChildren().add(player.root);

        // Camera
        PerspectiveCamera cam = new PerspectiveCamera(true);
        cam.setNearClip(0.05);
        cam.setFarClip(20000);
        cam.setFieldOfView(CAMERA_FOV);
        worldRoot.getChildren().add(cam);
        followCamera = new FollowCamera(cam);

        // Particles pool
        for (int i = 0; i < MAX_PARTICLES; i++) {
            Particle p = new Particle();
            particles.add(p);
            worldRoot.getChildren().add(p.node);
        }

        // 3D SubScene
        worldScene3D = new SubScene(worldRoot, APP_WIDTH, APP_HEIGHT, true, SceneAntialiasing.BALANCED);
        worldScene3D.setCamera(cam);
        worldScene3D.setFill(new RadialGradient(0, 0, 0.5, 0.4, 1.0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#88BBFF")),
                new Stop(1, Color.web("#EEF5FF"))));

        // HUD
        hudCanvas = new Canvas(APP_WIDTH, APP_HEIGHT);
        hudGC = hudCanvas.getGraphicsContext2D();

        // Main scene composition
        rootPane = new StackPane(worldScene3D, hudCanvas);
        StackPane.setAlignment(hudCanvas, Pos.TOP_LEFT);

        mainScene = new Scene(rootPane, APP_WIDTH, APP_HEIGHT, true);
        setupInputHandlers(mainScene);

        stage.setTitle("Extreme Snowboarding Game - JavaFX 3D");
        stage.setScene(mainScene);
        stage.show();

        // Start game loop
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long nowNs) {
                double nowSec = nowNs / 1_000_000_000.0;
                if (lastFrameTimeSec == 0) {
                    lastFrameTimeSec = nowSec;
                    return;
                }
                double dt = nowSec - lastFrameTimeSec;
                lastFrameTimeSec = nowSec;
                dt = Math.max(0.0005, Math.min(dt, 0.05)); // clamp dt

                if (!paused) {
                    update(dt);
                }
                renderHUD();
            }
        };
        timer.start();
    }

    private void setupInputHandlers(Scene scene) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            input.set(e.getCode(), true);
            if (e.getCode() == KeyCode.P) paused = !paused;
            if (e.getCode() == KeyCode.F3) debug = !debug;
            if (e.getCode() == KeyCode.C) switchCameraMode();
            if (e.getCode() == KeyCode.R) respawnPlayer();
        });
        scene.addEventHandler(KeyEvent.KEY_RELEASED, e -> {
            input.set(e.getCode(), false);
        });
    }

    private void switchCameraMode() {
        switch (cameraMode) {
            case THIRD_PERSON -> cameraMode = CameraMode.CLOSE_CHASE;
            case CLOSE_CHASE -> cameraMode = CameraMode.FIRST_PERSON;
            case FIRST_PERSON -> cameraMode = CameraMode.THIRD_PERSON;
        }
    }

    private void generateObstacles() {
        obstacles.clear();
        Group obstacleLayer = new Group();
        worldRoot.getChildren().add(obstacleLayer);

        float worldWidth = (TERRAIN_GRID_X - 1) * TERRAIN_SCALE;
        float worldDepth = (TERRAIN_GRID_Z - 1) * TERRAIN_SCALE;
        float minX = worldWidth * (1f - OBSTACLE_SPREAD) * 0.5f;
        float maxX = worldWidth * (1f + OBSTACLE_SPREAD) * 0.5f;
        float minZ = worldDepth * 0.15f;
        float maxZ = worldDepth * 0.98f;

        // Trees
        for (int i = 0; i < NUM_TREES; i++) {
            float x = randRange(minX, maxX);
            float z = randRange(minZ, maxZ);
            float y = terrain.sampleHeight(x, z);
            Tree tree = new Tree();
            tree.setPosition(new Vector3(x, y, z));
            obstacleLayer.getChildren().add(tree.root);
            obstacles.add(tree);
        }

        // Rocks
        for (int i = 0; i < NUM_ROCKS; i++) {
            float x = randRange(minX, maxX);
            float z = randRange(minZ, maxZ);
            float y = terrain.sampleHeight(x, z);
            Rock rock = new Rock();
            rock.setPosition(new Vector3(x, y, z));
            obstacleLayer.getChildren().add(rock.root);
            obstacles.add(rock);
        }
    }

    private float randRange(float a, float b) {
        return a + rng.nextFloat() * (b - a);
    }

    private void update(double dt) {
        // Physics step for player
        playerUpdate(dt);

        // Camera update
        followCamera.update(dt, player, cameraMode);

        // Particles update
        updateParticles(dt);

        // Score and gameplay progression
        scoreUpdate(dt);

        // Sun "animation" subtle movement
        sunLight.setTranslateX(sunLight.getTranslateX() + Math.sin(lastFrameTimeSec * 0.2) * 0.02);
        sunLight.setTranslateZ(sunLight.getTranslateZ() + Math.cos(lastFrameTimeSec * 0.13) * 0.02);
    }

    private void scoreUpdate(double dt) {
        distanceTravelled += player.speed() * dt;

        if (player.onGround) {
            if (currentlyAirborne) {
                // Landed this frame
                if (airtimeAccum > MIN_TRICK_AIRTIME) {
                    String trick = detectTrickName(trickYawAccum, trickPitchAccum, trickRollAccum);
                    if (!trick.isEmpty()) {
                        lastTrickName = trick;
                        // Score based on airtime and rotation amount
                        double trickScore = airtimeAccum * 250.0 + (Math.abs(trickYawAccum) + Math.abs(trickPitchAccum) + Math.abs(trickRollAccum)) * 20.0;
                        score += trickScore;
                    }
                }
                currentlyAirborne = false;
                airtimeAccum = 0;
                trickYawAccum = trickPitchAccum = trickRollAccum = 0;
            }
        } else {
            currentlyAirborne = true;
            airtimeAccum += dt;
            trickYawAccum += Math.abs(player.yawRate);
            trickPitchAccum += Math.abs(player.pitchRate);
            trickRollAccum += Math.abs(player.rollRate);
        }
    }

    private String detectTrickName(double yawRot, double pitchRot, double rollRot) {
        StringBuilder sb = new StringBuilder();
        // Yaw spins
        if (yawRot > ROTATION_FOR_360 * 1.5) sb.append("720 ");
        else if (yawRot > ROTATION_FOR_360 * 0.85) sb.append("360 ");
        else if (yawRot > ROTATION_FOR_180 * 0.85) sb.append("180 ");

        // Frontflip/backflip (pitch)
        if (pitchRot > ROTATION_FOR_360 * 0.85) sb.append("Double Flip ");
        else if (pitchRot > ROTATION_FOR_180 * 0.85) sb.append("Front/Back Flip ");

        // Shifty-ish roll
        if (rollRot > ROTATION_FOR_180 * 0.6) sb.append("Corkscrew ");

        String name = sb.toString().trim();
        return name;
    }

    private void updateParticles(double dt) {
        // Emit based on carving intensity when close to ground
        float carveIntensity = Math.abs(player.turnInput) + (input.isDown(KeyCode.S) ? 0.5f : 0f);
        boolean shouldEmit = player.onGround && carveIntensity > 0.15f && player.speed() > 2.0f;

        float emitCountF = (float) (PARTICLE_EMIT_RATE * carveIntensity * dt);
        int emitCount = shouldEmit ? (int) emitCountF + (rng.nextFloat() < (emitCountF % 1f) ? 1 : 0) : 0;

        // Find free particles and spawn
        for (int i = 0; i < particles.size() && emitCount > 0; i++) {
            Particle p = particles.get(i);
            if (!p.alive) {
                Vector3 spawnPos = player.position.add(new Vector3(0, 0.05f, 0)).add(player.right().mul((rng.nextFloat() - 0.5f) * 0.6f));
                Vector3 normal = terrain.sampleNormal(player.position.x, player.position.z);
                Vector3 sprayDir = normal.cross(player.forward()).normalized().mul((rng.nextFloat() * 0.6f + 0.4f) * PARTICLE_SPEED);
                sprayDir = sprayDir.add(player.forward().mul(rng.nextFloat() * 2.0f));
                p.spawn(spawnPos, sprayDir, PARTICLE_LIFETIME, PARTICLE_SIZE * (0.6f + rng.nextFloat() * 1.2f));
                emitCount--;
            }
        }

        // Update all particles
        for (Particle p : particles) {
            if (!p.alive) continue;
            p.update(dt);
            // Simple gravity
            p.vel = p.vel.add(new Vector3(0, GRAVITY * 0.4f * dt, 0));
            // Collide with ground
            float groundY = terrain.sampleHeight(p.pos.x, p.pos.z) - 0.05f;
            if (p.pos.y > groundY) {
                p.pos.y = groundY;
                p.vel = p.vel.mul(0.4f);
            }
            p.syncNode();
        }
    }

    private void playerUpdate(double dt) {
        // Handle respawn cooldown after crash
        if (player.crashed) {
            timeSinceCrash += dt;
            if (timeSinceCrash > RESPAWN_DELAY) {
                respawnPlayer();
            }
            return;
        }

        // Read inputs
        float turnTarget = 0f;
        if (input.isDown(KeyCode.A) || input.isDown(KeyCode.LEFT)) turnTarget -= 1f;
        if (input.isDown(KeyCode.D) || input.isDown(KeyCode.RIGHT)) turnTarget += 1f;
        player.turnInput = turnTarget;

        boolean wantsForward = input.isDown(KeyCode.W) || input.isDown(KeyCode.UP);
        boolean wantsBrake = input.isDown(KeyCode.S) || input.isDown(KeyCode.DOWN);
        boolean wantsJump = input.isPressed(KeyCode.SPACE); // one-shot

        // Physics integration
        float dtf = (float) dt;

        Vector3 pos = player.position;
        Vector3 vel = player.velocity;

        float groundY = terrain.sampleHeight(pos.x, pos.z);
        Vector3 normal = terrain.sampleNormal(pos.x, pos.z);

        // Determine if on ground
        float footOffset = 0.2f; // player's "foot" below position (player y pos is roughly board center)
        player.onGround = pos.y >= groundY - footOffset && vel.y <= 0;

        // Turning rate
        float targetTurnRate = MAX_TURN_RATE * turnTarget;
        player.yawRate = Approach.approach(player.yawRate, targetTurnRate, TURN_ACCEL, dtf);

        // Air vs ground handling
        if (player.onGround) {
            // Stick to ground
            pos.y = Math.min(pos.y, groundY - footOffset);

            // Forward vector along terrain slope
            Vector3 fwd = player.forward();
            Vector3 tangentDown = fwd.sub(normal.mul(fwd.dot(normal))).normalized();

            // Gravity projected along slope
            Vector3 gravityDownSlope = normal.mul(-GRAVITY).cross(player.right()).cross(normal).normalized().mul(GRAVITY * 0.6f);
            if (Float.isFinite(gravityDownSlope.length())) {
                vel = vel.add(gravityDownSlope.mul(dtf));
            }

            // Apply user acceleration/brake
            if (wantsForward) {
                vel = vel.add(tangentDown.mul(FORWARD_PUSH * dtf));
            }
            if (wantsBrake) {
                Vector3 opposite = vel.length() > 0.001f ? vel.normalized().mul(-1) : tangentDown.mul(-1);
                vel = vel.add(opposite.mul(BRAKE_FORCE * dtf));
            }

            // Friction
            vel = vel.mul((float) Math.pow(1.0 - FRICTION_GROUND, dtf * 60.0));

            // Jump
            if (wantsJump && player.speed() > 3.0f) {
                vel = vel.add(new Vector3(0, -JUMP_IMPULSE, 0));
                player.onGround = false;
            }
        } else {
            // In air: gravity
            vel = vel.add(new Vector3(0, GRAVITY * dtf, 0));
            // Some air drag
            vel = vel.mul(1.0f - FRICTION_AIR * dtf);
            // Allow trick rotations if in air
            if (input.isDown(KeyCode.Q)) {
                player.rollRate -= 1.5f * dtf;
            }
            if (input.isDown(KeyCode.E)) {
                player.rollRate += 1.5f * dtf;
            }
            if (input.isDown(KeyCode.Z)) {
                player.pitchRate -= 1.5f * dtf;
            }
            if (input.isDown(KeyCode.X)) {
                player.pitchRate += 1.5f * dtf;
            }
        }

        // Integrate position
        Vector3 forwardDir = player.forward();
        Vector3 moveDir = forwardDir.rotateAround(player.up(), player.yawRate * dtf);
        Vector3 displacement = moveDir.mul(player.speed() * dtf);
        pos = pos.add(displacement).add(vel.mul(dtf));

        // Clamp to terrain bounds
        float maxX = (TERRAIN_GRID_X - 1) * TERRAIN_SCALE;
        float maxZ = (TERRAIN_GRID_Z - 1) * TERRAIN_SCALE;
        pos.x = clamp(pos.x, 0.5f, maxX - 0.5f);
        pos.z = clamp(pos.z, 0.5f, maxZ - 0.5f);

        // Terrain collision after integration
        float newGroundY = terrain.sampleHeight(pos.x, pos.z);
        if (pos.y > newGroundY - footOffset) {
            // Collided with ground or below it; fix and zero vertical speed
            pos.y = newGroundY - footOffset;
            if (vel.y > 0) vel.y = 0;
        }

        // Update orientation to align with slope
        Vector3 newNormal = terrain.sampleNormal(pos.x, pos.z);
        player.alignToTerrain(newNormal, dtf);

        // Yaw update
        player.yaw += player.yawRate * dtf;

        // Trick rotations integrate
        if (!player.onGround) {
            player.pitch += player.pitchRate * dtf;
            player.roll += player.rollRate * dtf;
        } else {
            // Smoothly recover to neutral roll/pitch when grounded
            player.pitchRate = Approach.approach(player.pitchRate, 0, 3.5f, dtf);
            player.rollRate = Approach.approach(player.rollRate, 0, 3.5f, dtf);
            player.pitch = Approach.approach(player.pitch, 0, 4.0f, dtf);
            player.roll = Approach.approach(player.roll, 0, 4.0f, dtf);
        }

        // Collisions with obstacles
        for (Obstacle o : obstacles) {
            if (o.tryCollision(pos, COLLISION_RADIUS)) {
                // Crash!
                player.crashed = true;
                player.crashTime = 0;
                timeSinceCrash = 0;
                score = Math.max(0, score - 100);
                break;
            }
        }

        // Sync back
        player.position = pos;
        player.velocity = vel;

        // Update model transform
        player.syncNode();
    }

    private void respawnPlayer() {
        player.crashed = false;
        player.crashTime = 0;
        Vector3 startPos = new Vector3((TERRAIN_GRID_X - 1) * TERRAIN_SCALE * 0.5f, 0, 10f);
        startPos.y = terrain.sampleHeight(startPos.x, startPos.z) - 0.5f;
        player.setPosition(startPos);
        player.velocity = new Vector3(0, 0, 0);
        player.yaw = 0;
        player.pitch = 0;
        player.roll = 0;
        player.yawRate = 0;
        player.pitchRate = 0;
        player.rollRate = 0;
    }

    private void renderHUD() {
        GraphicsContext g = hudGC;
        double w = hudCanvas.getWidth();
        double h = hudCanvas.getHeight();

        g.clearRect(0, 0, w, h);

        // Speed and score
        g.setFill(Color.color(0, 0, 0, 0.35));
        g.fillRoundRect(10, 10, 270, 110, 12, 12);

        g.setFill(HUD_COLOR);
        g.setFont(HUD_FONT);
        double kph = player.speed() * 3.3;
        g.fillText(String.format("Speed: %.1f km/h", kph), 20, 38);
        g.fillText(String.format("Distance: %.1f m", distanceTravelled), 20, 64);
        g.fillText(String.format("Score: %.0f", score), 20, 90);

        // Trick text
        if (!lastTrickName.isEmpty()) {
            g.setFont(HUD_FONT_BIG);
            g.setFill(Color.WHITE);
            double textWidth = Utils.textWidth(g, lastTrickName);
            g.fillText(lastTrickName, (w - textWidth) / 2, 80);
        }

        // Status line
        g.setFont(HUD_FONT);
        String status = String.format("Ground:%s  Air:%.2fs  Cam:%s  %s",
                player.onGround ? "Y" : "N",
                currentlyAirborne ? airtimeAccum : 0.0,
                cameraMode.name(),
                debug ? "DEBUG ON" : "");
        double statusWidth = Utils.textWidth(g, status);
        g.setFill(Color.color(0, 0, 0, 0.35));
        g.fillRoundRect(w - statusWidth - 30, 10, statusWidth + 20, 30, 10, 10);
        g.setFill(HUD_COLOR);
        g.fillText(status, w - statusWidth - 20, 32);

        // Help
        g.setFill(Color.color(0, 0, 0, 0.35));
        double helpHeight = 120;
        g.fillRoundRect(10, h - helpHeight - 10, 430, helpHeight, 12, 12);

        g.setFill(HUD_COLOR);
        g.setFont(HUD_FONT);
        g.fillText("Controls:", 20, h - helpHeight + 20);
        g.fillText("W/S: Speed/BRAKE   A/D: Carve left/right   SPACE: Jump", 20, h - helpHeight + 44);
        g.fillText("Q/E: Roll trick   Z/X: Flip trick   C: Camera   P: Pause   R: Respawn   F3: Debug", 20, h - helpHeight + 68);
    }

    // ====== Supporting classes ======

    enum CameraMode {
        THIRD_PERSON,
        CLOSE_CHASE,
        FIRST_PERSON
    }

    static class Approach {
        static float approach(float value, float target, float speed, float dt) {
            float delta = target - value;
            float step = speed * dt;
            if (Math.abs(delta) <= step) return target;
            return value + Math.signum(delta) * step;
        }
    }

    static class Utils {
        static float clamp(float v, float a, float b) {
            return Math.max(a, Math.min(b, v));
        }
        static double textWidth(GraphicsContext gc, String text) {
            return com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().computeStringWidth(text, gc.getFont());
        }
    }

    static float clamp(float v, float a, float b) {
        return Math.max(a, Math.min(b, v));
    }

    static class Vector3 {
        float x, y, z;
        Vector3(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
        Vector3() { this(0,0,0); }
        Vector3 add(Vector3 o) { return new Vector3(x+o.x, y+o.y, z+o.z); }
        Vector3 sub(Vector3 o) { return new Vector3(x-o.x, y-o.y, z-o.z); }
        Vector3 mul(float s) { return new Vector3(x*s, y*s, z*s); }
        Vector3 div(float s) { return new Vector3(x/s, y/s, z/s); }
        float dot(Vector3 o) { return x*o.x + y*o.y + z*o.z; }
        Vector3 cross(Vector3 o) { return new Vector3(y*o.z - z*o.y, z*o.x - x*o.z, x*o.y - y*o.x); }
        float length() { return (float)Math.sqrt(x*x + y*y + z*z); }
        Vector3 normalized() { float len = length(); return len>1e-6f? div(len): new Vector3(0,0,0); }
        Vector3 rotateAround(Vector3 axis, float angle) {
            // Rodrigues rotation formula
            Vector3 k = axis.normalized();
            float c = (float)Math.cos(angle), s = (float)Math.sin(angle);
            return this.mul(c).add(k.cross(this).mul(s)).add(k.mul(k.dot(this)*(1-c)));
        }
        @Override public String toString(){ return String.format("(%.2f,%.2f,%.2f)", x,y,z); }
    }

    static class Materials {
        static PhongMaterial snowMaterial(Paint diffuse) {
            PhongMaterial m = new PhongMaterial();
            m.setDiffuseColor(diffuse instanceof Color ? (Color) diffuse : Color.WHITE);
            m.setSpecularColor(Color.WHITE);
            m.setSpecularPower(32);
            return m;
        }
        static PhongMaterial snowMaterial() {
            return snowMaterial(Color.color(0.94, 0.96, 1.0));
        }
        static PhongMaterial boardMaterial() {
            PhongMaterial m = new PhongMaterial();
            m.setDiffuseColor(Color.DARKRED);
            m.setSpecularColor(Color.WHITE);
            m.setSpecularPower(120);
            return m;
        }
        static PhongMaterial woodMaterial() {
            PhongMaterial m = new PhongMaterial();
            m.setDiffuseColor(Color.SADDLEBROWN);
            m.setSpecularColor(Color.color(0.2,0.1,0.05));
            m.setSpecularPower(16);
            return m;
        }
        static PhongMaterial rockMaterial() {
            PhongMaterial m = new PhongMaterial();
            m.setDiffuseColor(Color.DARKSLATEGRAY);
            m.setSpecularColor(Color.GRAY);
            m.setSpecularPower(16);
            return m;
        }
        static PhongMaterial foliageMaterial() {
            PhongMaterial m = new PhongMaterial();
            m.setDiffuseColor(Color.DARKGREEN);
            m.setSpecularColor(Color.color(0.2, 0.25, 0.2));
            m.setSpecularPower(8);
            return m;
        }
        static PhongMaterial skyGradient() {
            // Create a smooth gradient texture for the sky sphere
            int w = 2, h = 512;
            WritableImage img = new WritableImage(w, h);
            PixelWriter pw = img.getPixelWriter();
            for (int y = 0; y < h; y++) {
                double t = y / (double)(h - 1);
                Color c = Color.color(0.45 + 0.35 * (1 - t), 0.6 + 0.3 * (1 - t), 1.0);
                for (int x = 0; x < w; x++) pw.setColor(x, y, c);
            }
            PhongMaterial m = new PhongMaterial();
            m.setSelfIlluminationMap(img);
            m.setDiffuseColor(Color.LIGHTBLUE);
            return m;
        }
        static PhongMaterial particleMaterial() {
            PhongMaterial m = new PhongMaterial(Color.WHITE);
            m.setSpecularColor(Color.WHITESMOKE);
            m.setSpecularPower(8);
            return m;
        }
    }

    // Input state helper
    static class InputState {
        private final EnumMap<KeyCode, Boolean> down = new EnumMap<>(KeyCode.class);
        private final EnumMap<KeyCode, Boolean> pressedLatch = new EnumMap<>(KeyCode.class);

        void set(KeyCode code, boolean isDown) {
            if (isDown) {
                if (!down.getOrDefault(code, false)) {
                    pressedLatch.put(code, true);
                }
                down.put(code, true);
            } else {
                down.put(code, false);
                pressedLatch.put(code, false);
            }
        }

        boolean isDown(KeyCode code) {
            return down.getOrDefault(code, false);
        }

        boolean isPressed(KeyCode code) {
            boolean p = pressedLatch.getOrDefault(code, false);
            if (p) pressedLatch.put(code, false);
            return p;
        }
    }

    // Terrain class with procedural noise heightmap
    class Terrain {
        final int nx, nz;
        final float scale;
        final float height;
        final long seed;
        final float worldWidth, worldDepth;
        float[] heights; // size nx*nz
        TriangleMesh mesh;
        MeshView meshView;

        Terrain(int nx, int nz, float scale, float height, long seed) {
            this.nx = nx;
            this.nz = nz;
            this.scale = scale;
            this.height = height;
            this.seed = seed;
            this.worldWidth = (nx - 1) * scale;
            this.worldDepth = (nz - 1) * scale;
            generate();
        }

        void generate() {
            Noise noise = new Noise(seed);
            heights = new float[nx * nz];
            for (int z = 0; z < nz; z++) {
                for (int x = 0; x < nx; x++) {
                    float xx = x / (float) (nx - 1);
                    float zz = z / (float) (nz - 1);
                    // FBM noise
                    float n = 0;
                    float amp = 1f;
                    float freq = 1.2f;
                    for (int o = 0; o < 5; o++) {
                        n += noise.noise2(xx * 3f * freq, zz * 3f * freq) * amp;
                        amp *= 0.5f;
                        freq *= 2f;
                    }
                    // Shape to form a downhill slope in +Z
                    float slope = zz * 0.7f;
                    float valley = (float) Math.cos((xx - 0.5f) * Math.PI) * 0.25f + 0.75f;
                    float h = (n * 0.5f * valley - slope) * height;
                    heights[z * nx + x] = h;
                }
            }
        }

        MeshView buildMeshView(float steepness) {
            mesh = new TriangleMesh();
            // Build vertices
            for (int z = 0; z < nz; z++) {
                for (int x = 0; x < nx; x++) {
                    float px = x * scale;
                    float pz = z * scale;
                    float py = heights[z * nx + x];
                    mesh.getPoints().addAll(px, py, pz);
                }
            }

            // UVs
            int uCount = nx * nz;
            for (int i = 0; i < uCount; i++) {
                int x = i % nx;
                int z = i / nx;
                float u = x / (float) (nx - 1);
                float v = z / (float) (nz - 1);
                mesh.getTexCoords().addAll(u, v);
            }

            // Faces
            for (int z = 0; z < nz - 1; z++) {
                for (int x = 0; x < nx - 1; x++) {
                    int i0 = z * nx + x;
                    int i1 = i0 + 1;
                    int i2 = (z + 1) * nx + x;
                    int i3 = i2 + 1;
                    // Two triangles (i0,i2,i1) and (i1,i2,i3)
                    mesh.getFaces().addAll(i0, i0, i2, i2, i1, i1);
                    mesh.getFaces().addAll(i1, i1, i2, i2, i3, i3);
                }
            }

            meshView = new MeshView(mesh);
            meshView.setMaterial(terrainMaterial());
            meshView.setCullFace(CullFace.BACK);
            meshView.setDrawMode(DrawMode.FILL);
            return meshView;
        }

        PhongMaterial terrainMaterial() {
            // Create a simple color ramp texture for snow shading by slope
            int w = 2;
            int h = 512;
            WritableImage img = new WritableImage(w, h);
            PixelWriter pw = img.getPixelWriter();
            for (int y = 0; y < h; y++) {
                double t = y / (double) (h - 1);
                // t=0 -> flat, t=1 -> steep
                Color c = Color.WHITE.interpolate(Color.LIGHTGRAY, t).interpolate(Color.DARKSLATEGRAY, Math.pow(t, 3) * 0.3);
                pw.setColor(0, y, c);
                pw.setColor(1, y, c);
            }
            PhongMaterial m = new PhongMaterial();
            m.setDiffuseMap(img);
            m.setSpecularColor(Color.WHITE);
            m.setSpecularPower(32);
            return m;
        }

        float sampleHeight(float x, float z) {
            float fx = x / scale;
            float fz = z / scale;
            int x0 = (int) Math.floor(fx);
            int z0 = (int) Math.floor(fz);
            int x1 = x0 + 1;
            int z1 = z0 + 1;
            x0 = clampIndex(x0, 0, nx - 1);
            x1 = clampIndex(x1, 0, nx - 1);
            z0 = clampIndex(z0, 0, nz - 1);
            z1 = clampIndex(z1, 0, nz - 1);
            float hx0z0 = heights[z0 * nx + x0];
            float hx1z0 = heights[z0 * nx + x1];
            float hx0z1 = heights[z1 * nx + x0];
            float hx1z1 = heights[z1 * nx + x1];

            float tx = fx - x0;
            float tz = fz - z0;
            float h0 = lerp(hx0z0, hx1z0, tx);
            float h1 = lerp(hx0z1, hx1z1, tx);
            return lerp(h0, h1, tz);
        }

        Vector3 sampleNormal(float x, float z) {
            // Compute normal using central differences
            float eps = scale;
            float hL = sampleHeight(clamp(x - eps, 0, worldWidth), z);
            float hR = sampleHeight(clamp(x + eps, 0, worldWidth), z);
            float hD = sampleHeight(x, clamp(z - eps, 0, worldDepth));
            float hU = sampleHeight(x, clamp(z + eps, 0, worldDepth));
            Vector3 n = new Vector3(hL - hR, 2 * eps, hD - hU).normalized();
            return n;
        }

        private int clampIndex(int v, int a, int b) {
            return Math.max(a, Math.min(b, v));
        }

        private float lerp(float a, float b, float t) {
            return a + (b - a) * t;
        }
    }

    // Simple value noise with smooth interpolation
    static class Noise {
        private final int[] perm = new int[512];

        Noise(long seed) {
            Random r = new Random(seed);
            int[] p = new int[256];
            for (int i = 0; i < 256; i++) p[i] = i;
            for (int i = 255; i > 0; i--) {
                int j = r.nextInt(i + 1);
                int tmp = p[i]; p[i] = p[j]; p[j] = tmp;
            }
            for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
        }

        float noise2(float x, float y) {
            int xi = fastFloor(x);
            int yi = fastFloor(y);
            float xf = x - xi;
            float yf = y - yi;

            float tl = hash2(xi, yi);
            float tr = hash2(xi + 1, yi);
            float bl = hash2(xi, yi + 1);
            float br = hash2(xi + 1, yi + 1);

            float u = fade(xf);
            float v = fade(yf);

            float top = lerp(tl, tr, u);
            float bottom = lerp(bl, br, u);
            return lerp(top, bottom, v) * 2 - 1;
        }

        private float hash2(int x, int y) {
            int h = perm[(x + perm[y & 255]) & 255];
            return h / 255.0f;
        }

        private int fastFloor(float f) {
            return (f >= 0) ? (int) f : (int) f - 1;
        }

        private float fade(float t) {
            return t * t * t * (t * (t * 6 - 15) + 10);
        }

        private float lerp(float a, float b, float t) {
            return a + (b - a) * t;
        }
    }

    // Snowboarder character representation and physics state
    class Snowboarder {
        Group root = new Group();

        // Transform state
        Vector3 position = new Vector3();
        Vector3 velocity = new Vector3(0, 0, 8); // initial forward speed
        float yaw = 0;
        float pitch = 0;
        float roll = 0;

        // Rates for trick rotations and steering
        float yawRate = 0;
        float pitchRate = 0;
        float rollRate = 0;

        boolean onGround = true;
        boolean crashed = false;
        float crashTime = 0;

        float turnInput = 0;

        // Visual nodes
        MeshView boardMesh;
        Group body;

        Snowboarder() {
            buildModel();
        }

        void buildModel() {
            // Board
            TriangleMesh tm = new TriangleMesh();
            // Simple box-shaped board with rounded-ish ends
            float L = BOARD_LENGTH;
            float W = BOARD_WIDTH;
            float T = BOARD_THICK;
            // 8 corners of a box
            float[] pts = new float[]{
                    -L / 2, 0, -W / 2,
                    +L / 2, 0, -W / 2,
                    +L / 2, 0, +W / 2,
                    -L / 2, 0, +W / 2,
                    -L / 2, -T, -W / 2,
                    +L / 2, -T, -W / 2,
                    +L / 2, -T, +W / 2,
                    -L / 2, -T, +W / 2
            };
            int[] faces = new int[]{
                    0, 0, 1, 0, 2, 0,
                    0, 0, 2, 0, 3, 0, // top
                    4, 0, 6, 0, 5, 0,
                    4, 0, 7, 0, 6, 0, // bottom
                    0, 0, 4, 0, 5, 0,
                    0, 0, 5, 0, 1, 0, // side
                    1, 0, 5, 0, 6, 0,
                    1, 0, 6, 0, 2, 0, // side
                    2, 0, 6, 0, 7, 0,
                    2, 0, 7, 0, 3, 0, // side
                    3, 0, 7, 0, 4, 0,
                    3, 0, 4, 0, 0, 0  // side
            };
            for (float p : pts) tm.getPoints().addAll(p);
            tm.getTexCoords().addAll(0, 0);
            tm.getFaces().addAll(faces);
            boardMesh = new MeshView(tm);
            boardMesh.setMaterial(Materials.boardMaterial());

            // Body (very simple stick-figure using cylinders/spheres)
            body = new Group();
            Cylinder torso = new Cylinder(0.12, 0.9);
            torso.setMaterial(new PhongMaterial(Color.LIGHTGRAY));
            torso.setTranslateY(-0.7);
            Sphere head = new Sphere(0.16);
            head.setMaterial(new PhongMaterial(Color.BEIGE));
            head.setTranslateY(-1.3);

            Cylinder legL = new Cylinder(0.08, 0.7);
            legL.setMaterial(new PhongMaterial(Color.DARKGRAY));
            legL.getTransforms().add(new Translate(-0.15, -0.35, 0.0));
            Cylinder legR = new Cylinder(0.08, 0.7);
            legR.setMaterial(new PhongMaterial(Color.DARKGRAY));
            legR.getTransforms().add(new Translate(+0.15, -0.35, 0.0));

            Cylinder armL = new Cylinder(0.06, 0.7);
            armL.setMaterial(new PhongMaterial(Color.GRAY));
            armL.getTransforms().add(new Translate(-0.35, -0.9, 0.0));
            armL.getTransforms().add(new Rotate(25, Rotate.Z_AXIS));
            Cylinder armR = new Cylinder(0.06, 0.7);
            armR.setMaterial(new PhongMaterial(Color.GRAY));
            armR.getTransforms().add(new Translate(+0.35, -0.9, 0.0));
            armR.getTransforms().add(new Rotate(-25, Rotate.Z_AXIS));

            body.getChildren().addAll(torso, head, legL, legR, armL, armR);

            // Assemble
            root.getChildren().addAll(boardMesh, body);

            // Slight offset to body above board
            body.setTranslateY(-0.3);
        }

        Vector3 forward() {
            // Forward along -Z in local, rotated by yaw
            Vector3 f = new Vector3(0, 0, -1);
            return f.rotateAround(new Vector3(0, 1, 0), yaw);
        }

        Vector3 up() {
            return new Vector3(0, -1, 0); // y-down coordinate in JavaFX
        }

        Vector3 right() {
            return forward().cross(up()).normalized();
        }

        float speed() {
            return velocity.length();
        }

        void setPosition(Vector3 p) {
            position = new Vector3(p.x, p.y, p.z);
            syncNode();
        }

        void alignToTerrain(Vector3 normal, float dt) {
            // Align board normal with terrain normal; in JavaFX Y is down
            // We'll compute a target roll/pitch that approximates orientation relative to slope.
            Vector3 fwd = forward();
            Vector3 tangentForward = fwd.sub(normal.mul(fwd.dot(normal))).normalized();

            // Compute approximate pitch as angle between forward and its projection
            float cosPitch = clamp(fwd.dot(tangentForward), -1f, 1f);
            float targetPitch = (float) Math.acos(cosPitch) * (fwd.dot(normal) > 0 ? 1 : -1);

            // Compute roll using right vector vs slope
            Vector3 right = right();
            Vector3 tangentRight = right.sub(normal.mul(right.dot(normal))).normalized();
            float cosRoll = clamp(right.dot(tangentRight), -1f, 1f);
            float targetRoll = (float) Math.acos(cosRoll) * (right.dot(normal) > 0 ? 1 : -1);

            // Smoothly approach
            pitch = Approach.approach(pitch, targetPitch * 0.5f, 2.5f, dt);
            roll = Approach.approach(roll, targetRoll * 0.5f, 2.5f, dt);
        }

        void syncNode() {
            root.setTranslateX(position.x);
            root.setTranslateY(position.y);
            root.setTranslateZ(position.z);
            root.getTransforms().setAll(
                    new Rotate(Math.toDegrees(roll), Rotate.Z_AXIS),
                    new Rotate(Math.toDegrees(pitch), Rotate.X_AXIS),
                    new Rotate(Math.toDegrees(yaw), Rotate.Y_AXIS)
            );
        }
    }

    // Camera follower with smoothing and multiple modes
    class FollowCamera {
        final PerspectiveCamera cam;
        Vector3 pos = new Vector3(0, -10, 10);
        Vector3 lookAt = new Vector3(0, 0, 0);

        DoubleProperty shake = new SimpleDoubleProperty(0);

        FollowCamera(PerspectiveCamera cam) {
            this.cam = cam;
        }

        void update(double dt, Snowboarder player, CameraMode mode) {
            Vector3 targetLook = player.position.add(new Vector3(0, -0.5f, 0));
            Vector3 forward = player.forward();
            Vector3 up = new Vector3(0, -1, 0);

            float dist = CAMERA_DISTANCE;
            float height = CAMERA_HEIGHT;

            switch (mode) {
                case THIRD_PERSON -> {
                    dist = CAMERA_DISTANCE;
                    height = CAMERA_HEIGHT;
                }
                case CLOSE_CHASE -> {
                    dist = 8.0f;
                    height = 2.6f;
                }
                case FIRST_PERSON -> {
                    dist = 0.1f;
                    height = -0.1f;
                }
            }

            Vector3 desiredPos = targetLook.sub(forward.mul(dist)).add(new Vector3(0, height, 0));

            // Smooth
            pos = pos.add(desiredPos.sub(pos).mul((float) (CAMERA_LAG_POS * (mode == CameraMode.FIRST_PERSON ? 2.0 : 1.0))));
            lookAt = lookAt.add(targetLook.sub(lookAt).mul((float) CAMERA_LAG_LOOK));

            // Apply camera
            cam.setTranslateX(pos.x);
            cam.setTranslateY(pos.y);
            cam.setTranslateZ(pos.z);

            // LookAt transform: compute direction
            Vector3 dir = lookAt.sub(pos).normalized();
            // Build rotation from Z forward to dir: Y-up basis for JavaFX
            Point3D zAxis = new Point3D(0, 0, 1);
            Point3D dirFx = new Point3D(dir.x, dir.y, dir.z);
            double yaw = Math.toDegrees(Math.atan2(-dir.x, -dir.z));
            double pitch = Math.toDegrees(Math.asin(dir.y));
            cam.getTransforms().setAll(
                    new Rotate(yaw, Rotate.Y_AXIS),
                    new Rotate(pitch, Rotate.X_AXIS)
            );
        }
    }

    // Base class for obstacles
    abstract static class Obstacle {
        Group root = new Group();
        Vector3 position = new Vector3();
        float radius = 0.8f; // approximate collision radius

        void setPosition(Vector3 p) {
            this.position = p;
            root.setTranslateX(p.x);
            root.setTranslateY(p.y);
            root.setTranslateZ(p.z);
        }

        boolean tryCollision(Vector3 playerPos, float playerRadius) {
            Vector3 c = position;
            float dx = playerPos.x - c.x;
            float dz = playerPos.z - c.z;
            float dist2 = dx * dx + dz * dz;
            float rr = (radius + playerRadius);
            if (dist2 < rr * rr) {
                float dy = Math.abs(playerPos.y - c.y);
                if (dy < 2.5f) return true;
            }
            return false;
        }
    }

    // Tree obstacle
    static class Tree extends Obstacle {
        Tree() {
            float trunkH = 1.6f + (float) Math.random() * 1.1f;
            Cylinder trunk = new Cylinder(0.15f, trunkH);
            trunk.setMaterial(Materials.woodMaterial());
            trunk.setTranslateY(-trunkH / 2);

            MeshView foliage = cone(0.0f, 1.2f, 1.6f);
            foliage.setMaterial(Materials.foliageMaterial());
            foliage.setTranslateY(-trunkH - 0.7f);

            root.getChildren().addAll(trunk, foliage);
            radius = 0.7f;
        }

        static MeshView cone(float rTop, float rBase, float h) {
            int seg = 24;
            TriangleMesh m = new TriangleMesh();
            // Points: top + base ring
            m.getPoints().addAll(0, -h, 0); // tip
            for (int i = 0; i < seg; i++) {
                double ang = 2 * Math.PI * i / seg;
                float x = (float) (Math.cos(ang) * rBase);
                float z = (float) (Math.sin(ang) * rBase);
                m.getPoints().addAll(x, 0, z);
            }
            // UVs placeholder
            m.getTexCoords().addAll(0, 0);
            // Faces
            for (int i = 0; i < seg; i++) {
                int i0 = 0;
                int i1 = 1 + i;
                int i2 = 1 + ((i + 1) % seg);
                m.getFaces().addAll(i0, 0, i1, 0, i2, 0);
            }
            MeshView mv = new MeshView(m);
            mv.setCullFace(CullFace.BACK);
            return mv;
        }
    }

    // Rock obstacle
    static class Rock extends Obstacle {
        Rock() {
            MeshView mv = irregularRockMesh();
            mv.setMaterial(Materials.rockMaterial());
            root.getChildren().add(mv);
            radius = 0.6f;
        }

        static MeshView irregularRockMesh() {
            TriangleMesh m = new TriangleMesh();
            Random r = ThreadLocalRandom.current();
            int rings = 6;
            int segs = 16;
            float R = 0.6f + r.nextFloat() * 0.35f;

            // points
            for (int y = 0; y < rings; y++) {
                float v = y / (float) (rings - 1);
                float ry = (float) (Math.cos((v - 0.5) * Math.PI)) * R * 0.6f;
                float rr = (float) (Math.sin(v * Math.PI)) * R;
                for (int i = 0; i < segs; i++) {
                    double ang = 2 * Math.PI * i / segs;
                    float rx = (float) (Math.cos(ang) * rr) + (r.nextFloat() - 0.5f) * 0.05f;
                    float rz = (float) (Math.sin(ang) * rr) + (r.nextFloat() - 0.5f) * 0.05f;
                    float yv = -ry + (r.nextFloat() - 0.5f) * 0.05f;
                    m.getPoints().addAll(rx, yv, rz);
                }
            }
            // uvs
            m.getTexCoords().addAll(0, 0);
            // faces
            for (int y = 0; y < rings - 1; y++) {
                for (int i = 0; i < segs; i++) {
                    int i0 = y * segs + i;
                    int i1 = y * segs + (i + 1) % segs;
                    int i2 = (y + 1) * segs + i;
                    int i3 = (y + 1) * segs + (i + 1) % segs;
                    m.getFaces().addAll(i0, 0, i2, 0, i1, 0);
                    m.getFaces().addAll(i1, 0, i2, 0, i3, 0);
                }
            }
            MeshView mv = new MeshView(m);
            mv.setCullFace(CullFace.BACK);
            return mv;
        }
    }

    // Simple particle
    static class Particle {
        Sphere node = new Sphere(PARTICLE_SIZE);
        Vector3 pos = new Vector3();
        Vector3 vel = new Vector3();
        float life = 0;
        float maxLife = 1;
        boolean alive = false;

        Particle() {
            node.setMaterial(Materials.particleMaterial());
            node.setVisible(false);
        }

        void spawn(Vector3 position, Vector3 velocity, float lifetime, float size) {
            this.pos = new Vector3(position.x, position.y, position.z);
            this.vel = velocity;
            this.life = 0;
            this.maxLife = lifetime;
            this.alive = true;
            node.setRadius(size);
            node.setVisible(true);
            syncNode();
        }

        void update(double dt) {
            if (!alive) return;
            life += dt;
            if (life >= maxLife) {
                alive = false;
                node.setVisible(false);
                return;
            }
            pos = pos.add(vel.mul((float) dt));
            double alpha = 1.0 - (life / maxLife);
            Color c = Color.color(1.0, 1.0, 1.0, Math.max(0.0, Math.min(1.0, alpha)));
            PhongMaterial m = new PhongMaterial();
            m.setDiffuseColor(c);
            m.setSpecularColor(Color.TRANSPARENT);
            node.setMaterial(m);
        }

        void syncNode() {
            node.setTranslateX(pos.x);
            node.setTranslateY(pos.y);
            node.setTranslateZ(pos.z);
        }
    }
}