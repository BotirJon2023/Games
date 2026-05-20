import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.image.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.util.*;

public class TennisGameApp extends Application {

    // World units are pixels; Y+ is downward (JavaFX 3D default). Ground plane is at y = 0.
    // Court dimensions (approx)
    static final double COURT_HALF_LEN = 560;   // along X (baseline to baseline ~1120)
    static final double COURT_HALF_WID = 210;   // along Z (width ~420)
    static final double NET_X = 0.0;            // net plane at X=0
    static final double NET_HEIGHT = 100;       // pixels
    static final double NET_THICK = 8;

    // Physics
    static final double GRAVITY = 2200;         // px/s^2 (down)
    static final double GROUND_RESTITUTION = 0.72;
    static final double FRICTION = 0.82;
    static final double AIR_DRAG = 0.0006;
    static final double BALL_RADIUS = 10;
    static final double BALL_MAX_SPEED = 1600;

    // Players
    static final double PLAYER_SPEED = 460;
    static final double PLAYER_JUMP = -920;     // negative is up (since Y+ is down)
    static final double PLAYER_RADIUS = 18;
    static final double PLAYER_HEIGHT = 120;

    // Racket
    static final double RACKET_RADIUS = 30;
    static final long SWING_MS = 220;

    // Lighting
    static final Color AMBIENT = Color.color(0.55, 0.55, 0.6);
    static final Color COURT_COLOR = Color.web("#2da86c");

    // Optional background texture (sky dome); set to your URL or local file if desired
    static final boolean USE_SKY_IMAGE = false;
    static final String SKY_IMAGE_URL = "https://upload.wikimedia.org/wikipedia/commons/a/ab/Blue_sky_daytime.jpg";

    // Scene graph
    private Group root3D;
    private SubScene subScene;
    private PerspectiveCamera camera;

    private Group worldGroup;        // parent for all 3D game nodes
    private Group stadiumGroup;      // Colosseum ring
    private Group courtGroup;        // court/nets
    private Group fxGroup;           // particles, trails
    private Group hudLayer;          // 2D overlay

    // Entities
    private Player3D p1, p2, server;
    private Ball3D ball;

    // Input
    private final Set<KeyCode> keys = new HashSet<>();
    private boolean paused = false;
    private boolean running = true;
    private boolean aiEnabled = true;

    private enum RallyState { SERVE, PLAY, POINT }
    private RallyState state = RallyState.SERVE;

    private final Random rng = new Random();

    // Score
    private static class Score {
        int pointsP1 = 0, pointsP2 = 0;
        int gamesP1 = 0, gamesP2 = 0;
        int setsP1 = 0, setsP2 = 0;

        void reset() { pointsP1 = pointsP2 = gamesP1 = gamesP2 = setsP1 = setsP2 = 0; }

        void addPoint(boolean toP1) {
            if (toP1) {
                if (pointsP1 >= 3 && pointsP2 >= 3) {
                    if (pointsP1 == pointsP2) pointsP1++;
                    else if (pointsP1 == pointsP2 + 1) { gamesP1++; pointsP1 = pointsP2 = 0; }
                    else pointsP2--;
                } else {
                    pointsP1++;
                    if (pointsP1 >= 4 && pointsP1 >= pointsP2 + 2) { gamesP1++; pointsP1 = pointsP2 = 0; }
                }
            } else {
                if (pointsP1 >= 3 && pointsP2 >= 3) {
                    if (pointsP1 == pointsP2) pointsP2++;
                    else if (pointsP2 == pointsP1 + 1) { gamesP2++; pointsP1 = pointsP2 = 0; }
                    else pointsP1--;
                } else {
                    pointsP2++;
                    if (pointsP2 >= 4 && pointsP2 >= pointsP1 + 2) { gamesP2++; pointsP1 = pointsP2 = 0; }
                }
            }
            if ((gamesP1 >= 6 || gamesP2 >= 6) && Math.abs(gamesP1 - gamesP2) >= 2) {
                if (gamesP1 > gamesP2) setsP1++; else setsP2++;
                gamesP1 = gamesP2 = 0;
                pointsP1 = pointsP2 = 0;
            }
        }
        static String pointsToString(int p1, int p2) {
            if (p1 >= 3 && p2 >= 3) {
                if (p1 == p2) return "Deuce";
                if (p1 == p2 + 1) return "Adv P1";
                if (p2 == p1 + 1) return "Adv P2";
            }
            return fmt(p1) + " - " + fmt(p2);
        }
        private static String fmt(int p) {
            return switch (p) { case 0 -> "Love"; case 1 -> "15"; case 2 -> "30"; case 3 -> "40"; default -> ""+p; };
        }
    }
    private final Score score = new Score();

    // HUD
    private Text scoreText, setText, modeText, hintText;

    // Time
    private long lastTime;

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        hudLayer = new Group();

        root3D = new Group();
        worldGroup = new Group();
        stadiumGroup = new Group();
        courtGroup = new Group();
        fxGroup = new Group();

        root3D.getChildren().addAll(worldGroup);
        worldGroup.getChildren().addAll(stadiumGroup, courtGroup, fxGroup);

        subScene = new SubScene(root3D, 1280, 720, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#88b9ff")); // fallback sky
        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(8000);
        camera.setFieldOfView(45);
        subScene.setCamera(camera);

        // Camera position: slight angle, see whole court
        camera.getTransforms().addAll(
                new Translate(0, -380, -1350), // move up (negative Y) and back (negative Z)
                new Rotate(-10, Rotate.X_AXIS) // tilt down slightly
        );

        // Build scene
        buildLights();
        buildSkyDome();
        buildStadium();
        buildCourt();
        buildEntities();
        buildHUD();

        root.getChildren().addAll(subScene, hudLayer);

        Scene scene = new Scene(root, 1280, 720, true);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> { keys.add(e.getCode()); handleShortcuts(e); });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, e -> keys.remove(e.getCode()));

        stage.setTitle("Tennis Match 3D: Colosseum Court");
        stage.setScene(scene);
        stage.show();

        initServe(p1);
        lastTime = System.nanoTime();

        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (!running) { stop(); stage.close(); return; }
                if (paused) { lastTime = now; return; }
                double dt = (now - lastTime) / 1_000_000_000.0;
                if (dt > 1/30.0) dt = 1/30.0;
                lastTime = now;
                update(dt);
            }
        };
        timer.start();
    }

    private void handleShortcuts(KeyEvent e) {
        if (e.getCode() == KeyCode.F1) {
            aiEnabled = !aiEnabled;
            modeText.setText(aiEnabled ? "Mode: Player 1 vs Computer (F1 to toggle)" :
                    "Mode: Player 1 vs Player 2 (F1 to toggle)");
            e.consume();
        } else if (e.getCode() == KeyCode.R) {
            resetMatch();
        } else if (e.getCode() == KeyCode.P) {
            paused = !paused;
            hintText.setText(paused ? "Paused (P to resume)" : controlsHint());
        } else if (e.getCode() == KeyCode.ESCAPE) {
            running = false;
        }
    }

    private void buildLights() {
        AmbientLight amb = new AmbientLight(AMBIENT);
        PointLight p1 = new PointLight(Color.web("#ffd9a6")); p1.setTranslateY(-300); p1.setTranslateZ(-300); p1.setTranslateX(-300);
        PointLight p2 = new PointLight(Color.web("#a6d2ff")); p2.setTranslateY(-300); p2.setTranslateZ(300); p2.setTranslateX(300);
        worldGroup.getChildren().addAll(amb, p1, p2);
    }

    private void buildSkyDome() {
        Sphere sky = new Sphere(5000);
        sky.setCullFace(CullFace.FRONT); // show inside
        PhongMaterial mat = new PhongMaterial();
        if (USE_SKY_IMAGE) {
            try {
                Image img = new Image(SKY_IMAGE_URL, 4096, 2048, true, true, true);
                mat.setSelfIlluminationMap(img);
                mat.setDiffuseMap(img);
            } catch (Exception ignored) {
                mat.setDiffuseColor(Color.web("#87ceeb"));
            }
        } else {
            mat.setDiffuseColor(Color.web("#9cc9ff"));
            mat.setSpecularColor(Color.color(0.9,0.9,1.0));
        }
        sky.setMaterial(mat);
        stadiumGroup.getChildren().add(sky);
    }

    private void buildStadium() {
        // Stylized multi-ring Colosseum with arches and columns
        Group ringGroup = new Group();
        int levels = 3;
        double baseY = -10; // slightly below ground level visually
        for (int lvl = 0; lvl < levels; lvl++) {
            double radius = 1100 + lvl * 70;
            double archH = 140;
            double archW = 80;
            double y = - (240 + lvl * 140); // above ground (negative Y)
            int arches = 40 - lvl * 6;
            Color stone = Color.rgb(200 - 12*lvl, 180 - 12*lvl, 145 - 10*lvl);
            for (int i = 0; i < arches; i++) {
                double ang = 2 * Math.PI * i / arches;
                double cx = Math.cos(ang) * radius;
                double cz = Math.sin(ang) * radius;
                double rotY = Math.toDegrees(ang) + 90;
                Group arch = makeArch(archW, archH, stone);
                arch.getTransforms().addAll(
                        new Translate(cx, y, cz),
                        new Rotate(rotY, Rotate.Y_AXIS)
                );
                ringGroup.getChildren().add(arch);
            }
        }
        stadiumGroup.getChildren().add(ringGroup);
    }

    private Group makeArch(double w, double h, Color stone) {
        // Simple arch: two columns + curved top (approx with thin box)
        Group g = new Group();
        PhongMaterial m = new PhongMaterial(stone);
        double colW = w * 0.28;
        double colH = h;
        double beamH = h * 0.25;

        Box colL = new Box(colW, colH, colW);
        colL.setMaterial(m);
        colL.setTranslateX(-w/2 + colW/2);
        Box colR = new Box(colW, colH, colW);
        colR.setMaterial(m);
        colR.setTranslateX(w/2 - colW/2);

        Box beam = new Box(w, beamH, colW * 0.9);
        beam.setMaterial(m);
        beam.setTranslateY(-colH/2 + beamH/2);

        g.getChildren().addAll(colL, colR, beam);
        return g;
    }

    private void buildCourt() {
        // Court plane (thin box), white lines via thin boxes
        Group court = new Group();
        PhongMaterial grass = new PhongMaterial(COURT_COLOR);
        grass.setSpecularColor(Color.color(0.2, 0.2, 0.2));

        Box floor = new Box(COURT_HALF_LEN * 2, 2, COURT_HALF_WID * 2);
        floor.setMaterial(grass);
        floor.setTranslateY(1); // so ground plane is at y=0 above box center
        court.getChildren().add(floor);

        // Baselines and sidelines
        court.getChildren().add(lineBox(-COURT_HALF_LEN + 6, 0, 0, 2, COURT_HALF_WID*2, Color.WHITE)); // left baseline
        court.getChildren().add(lineBox(COURT_HALF_LEN - 6, 0, 0, 2, COURT_HALF_WID*2, Color.WHITE));  // right baseline
        court.getChildren().add(lineBox(0, 0, -COURT_HALF_WID + 6, 2, COURT_HALF_LEN*2, Color.WHITE, true)); // top sideline
        court.getChildren().add(lineBox(0, 0, COURT_HALF_WID - 6, 2, COURT_HALF_LEN*2, Color.WHITE, true));  // bottom sideline
        // Center service line
        court.getChildren().add(lineBox(0, 0, 0, 2, COURT_HALF_LEN*2, Color.color(1,1,1,0.5), true));

        // Net posts
        PhongMaterial postMat = new PhongMaterial(Color.SILVER);
        Box postL = new Box(10, NET_HEIGHT + 30, 10);
        postL.setMaterial(postMat);
        postL.setTranslateX(-NET_THICK/2 - 5);
        postL.setTranslateY(-NET_HEIGHT/2 - 10);
        Box postR = new Box(10, NET_HEIGHT + 30, 10);
        postR.setMaterial(postMat);
        postR.setTranslateX(NET_THICK/2 + 5);
        postR.setTranslateY(-NET_HEIGHT/2 - 10);
        court.getChildren().addAll(postL, postR);

        // Net band
        PhongMaterial bandMat = new PhongMaterial(Color.WHITE);
        Box band = new Box(180, 12, NET_THICK + 2);
        band.setMaterial(bandMat);
        band.setTranslateY(-NET_HEIGHT - 6);
        court.getChildren().add(band);

        // Net mesh (semi-transparent)
        Box net = new Box(NET_THICK, NET_HEIGHT, COURT_HALF_WID*2);
        PhongMaterial netMat = new PhongMaterial(Color.color(1,1,1,0.85));
        netMat.setDiffuseMap(makeNetTexture(256, 256));
        net.setMaterial(netMat);
        net.setTranslateY(-NET_HEIGHT/2);
        court.getChildren().add(net);

        courtGroup.getChildren().add(court);
    }

    private Image makeNetTexture(int w, int h) {
        WritableImage img = new WritableImage(w, h);
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean line = (x % 16 == 0) || (y % 16 == 0);
                int a = line ? 220 : 0;
                pw.setColor(x, y, Color.rgb(255, 255, 255, a / 255.0));
            }
        }
        return img;
    }

    private Node lineBox(double xOrZ, double y, double other, double thick, double len, Color c) {
        return lineBox(xOrZ, y, other, thick, len, c, false);
    }
    private Node lineBox(double xOrZ, double y, double other, double thick, double len, Color c, boolean alongX) {
        PhongMaterial m = new PhongMaterial(c);
        if (alongX) {
            Box b = new Box(len, 1, thick);
            b.setMaterial(m);
            b.setTranslateX(0);
            b.setTranslateZ(other);
            return b;
        } else {
            Box b = new Box(1, 1, len);
            b.setMaterial(m);
            b.setTranslateX(xOrZ);
            b.setTranslateZ(0);
            b.setScaleX(thick);
            return b;
        }
    }

    private void buildEntities() {
        // Players: left side (negative X), right side (positive X)
        p1 = new Player3D("P1", -COURT_HALF_LEN + 120, -PLAYER_HEIGHT, 0, Color.web("#2b8cff"), Color.WHITE, true);
        p2 = new Player3D("P2", COURT_HALF_LEN - 120, -PLAYER_HEIGHT, 0, Color.web("#ff4d4d"), Color.WHITE, false);

        worldGroup.getChildren().addAll(p1.root, p2.root);

        // Ball
        ball = new Ball3D(-200, -200, 0);
        worldGroup.getChildren().add(ball.node);
    }

    private void buildHUD() {
        scoreText = new Text();
        scoreText.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
        scoreText.setFill(Color.WHITE);
        scoreText.setTranslateX(36);
        scoreText.setTranslateY(60);

        setText = new Text();
        setText.setFont(Font.font("Verdana", FontWeight.SEMI_BOLD, 18));
        setText.setFill(Color.WHITE);
        setText.setTranslateX(36);
        setText.setTranslateY(92);

        modeText = new Text();
        modeText.setFont(Font.font("Verdana", 16));
        modeText.setFill(Color.WHITE);
        modeText.setTranslateX(560);
        modeText.setTranslateY(44);

        hintText = new Text(controlsHint());
        hintText.setFont(Font.font("Verdana", 14));
        hintText.setFill(Color.WHITE);
        hintText.setTranslateX(560);
        hintText.setTranslateY(74);

        Rectangle bg = new Rectangle(20, 20, 520, 106);
        bg.setArcWidth(16); bg.setArcHeight(16);
        bg.setFill(Color.color(0,0,0,0.25));
        bg.setStroke(Color.color(1,1,1,0.35));

        hudLayer.getChildren().addAll(bg, scoreText, setText, modeText, hintText);
        modeText.setText(aiEnabled ? "Mode: Player 1 vs Computer (F1 to toggle)" :
                "Mode: Player 1 vs Player 2 (F1 to toggle)");
        updateScoreboard();
    }

    private String controlsHint() {
        return "P1: A/D left-right, W/S fwd/back, SPACE jump, F swing, Q serve | P2: Left/Right, Up/Down, / swing, ENTER serve | F1 AI, R reset, P pause";
    }

    private void initServe(Player3D who) {
        server = who;
        state = RallyState.SERVE;
        ball.vx = ball.vy = ball.vz = 0;
        positionServeBall();
        hintText.setText((server == p1 ? "P1" : "P2") + " to serve (" + (server == p1 ? "Q" : (aiEnabled ? "AI" : "ENTER")) + ")");
    }

    private void positionServeBall() {
        double dir = server.leftSide ? 1 : -1; // toward net
        ball.setPosition(server.x + dir * 28, server.y - 40, server.z);
    }

    private void attemptServe(Player3D who) {
        if (state != RallyState.SERVE || server != who) return;
        who.trySwing();
        double dir = who.leftSide ? 1 : -1;
        double speed = 900 + rng.nextDouble() * 140;
        double elev = Math.toRadians(18 + rng.nextDouble() * 6);
        double spreadZ = (rng.nextDouble() - 0.5) * 160;
        ball.vx = Math.cos(elev) * speed * dir;
        ball.vy = -Math.sin(elev) * speed * 0.75; // up is negative
        ball.vz = spreadZ;
        ball.lastHitter = who;
        ball.bounceCountSinceHit = 0;
        state = RallyState.PLAY;
        hintText.setText("");
    }

    private void resetMatch() {
        score.reset();
        p1.setPosition(-COURT_HALF_LEN + 120, -PLAYER_HEIGHT, 0);
        p2.setPosition(COURT_HALF_LEN - 120, -PLAYER_HEIGHT, 0);
        ball.setPosition(-200, -200, 0);
        initServe(p1);
        hintText.setText(controlsHint());
    }

    private void update(double dt) {
        handleInput(dt);

        if (aiEnabled) aiForP2(dt);

        p1.update(dt);
        p2.update(dt);

        switch (state) {
            case SERVE -> {
                ball.vx = ball.vy = ball.vz = 0;
                positionServeBall();
                ball.updateTrail(dt, false);
            }
            case PLAY -> {
                ball.updatePhysics(dt);
                ballCollisions();
                ball.updateTrail(dt, true);
            }
            case POINT -> ball.updateTrail(dt, false);
        }

        // Fade particles
        for (int i = fxGroup.getChildren().size()-1; i >= 0; i--) {
            Node n = fxGroup.getChildren().get(i);
            if (n.getUserData() instanceof Particle3D p) {
                if (!p.update(dt)) fxGroup.getChildren().remove(i);
            }
        }

        updateScoreboard();
    }

    private void handleInput(double dt) {
        // P1
        double moveZ1 = 0, moveX1 = 0;
        if (keys.contains(KeyCode.A)) moveZ1 -= 1;
        if (keys.contains(KeyCode.D)) moveZ1 += 1;
        if (keys.contains(KeyCode.W)) moveX1 += 1; // toward net (increase X if left side)
        if (keys.contains(KeyCode.S)) moveX1 -= 1;
        p1.vz = moveZ1 * PLAYER_SPEED;
        p1.vx = (p1.leftSide ? 1 : -1) * moveX1 * PLAYER_SPEED;

        if (keys.contains(KeyCode.SPACE)) p1.jump();
        if (keys.contains(KeyCode.F)) p1.trySwing();
        if (keys.contains(KeyCode.Q)) attemptServe(p1);

        // P2 human if AI off
        if (!aiEnabled) {
            double moveZ2 = 0, moveX2 = 0;
            if (keys.contains(KeyCode.LEFT)) moveZ2 -= 1;
            if (keys.contains(KeyCode.RIGHT)) moveZ2 += 1;
            if (keys.contains(KeyCode.UP)) moveX2 += 1;
            if (keys.contains(KeyCode.DOWN)) moveX2 -= 1;
            p2.vz = moveZ2 * PLAYER_SPEED;
            p2.vx = (p2.leftSide ? 1 : -1) * moveX2 * PLAYER_SPEED;

            if (keys.contains(KeyCode.SLASH)) p2.trySwing();
            if (keys.contains(KeyCode.ENTER)) attemptServe(p2);
        }
    }

    private void aiForP2(double dt) {
        if (state == RallyState.SERVE && server == p2) {
            if (rng.nextDouble() < 0.015) attemptServe(p2);
            return;
        }
        // Simple heuristic: when ball coming to right (p2 side), move to predicted (Z,X)
        double targetZ = 0;
        double targetX = COURT_HALF_LEN - 140;
        if (state == RallyState.PLAY) {
            if (ball.vx > 0) {
                // Predict landing time to ground (y == 0)
                double t = timeToHitGround(ball.y, ball.vy, GRAVITY);
                if (t < 0.1) t = 0.1;
                targetZ = ball.z + ball.vz * t;
                targetZ = clamp(targetZ, -COURT_HALF_WID + 40, COURT_HALF_WID - 40);
                // Get closer to net if landing near net
                targetX = Math.min(COURT_HALF_LEN - 80, Math.max(120, ball.x + ball.vx * t - 120));
                targetX = Math.max(40, targetX);
            } else {
                // Idle center
                targetZ = 0;
                targetX = COURT_HALF_LEN - 160;
            }
        }
        // Convert targetX (absolute) to p2's space (p2 is on right, allowed X in [40 .. COURT_HALF_LEN - 40])
        double dx = targetX - p2.x;
        double dz = targetZ - p2.z;
        double dist = Math.hypot(dx, dz);
        if (dist > 8) {
            double speed = PLAYER_SPEED * (0.9 + 0.2 * rng.nextDouble());
            p2.vx = speed * (dx / dist);
            p2.vz = speed * (dz / dist);
        } else {
            p2.vx = p2.vz = 0;
        }

        // Jump for high balls near
        if (state == RallyState.PLAY && ball.vx > 0 && ball.y < p2.y - 30 && Math.abs(ball.x - p2.x) < 140 && Math.abs(ball.z - p2.z) < 140) {
            p2.jump();
        }
        // Auto-swing when ball near and moving toward p2
        if (state == RallyState.PLAY && ball.vx > 0 && p2.isBallInRacketRange(ball)) {
            p2.trySwing();
        }
    }

    private double timeToHitGround(double y, double vy, double g) {
        // Solve y + vy*t + 0.5*g*t^2 = 0 for t > 0 (ground at y=0)
        double a = 0.5 * g, b = vy, c = y;
        double disc = b*b - 4*a*c;
        if (disc < 0) return 0.6;
        double t1 = (-b + Math.sqrt(disc)) / (2*a);
        double t2 = (-b - Math.sqrt(disc)) / (2*a);
        double t = Math.max(t1, t2);
        return (t > 0) ? t : Math.min(t1, t2);
    }

    private void ballCollisions() {
        // Ground bounce at y=0 (remember y is negative above ground)
        if (ball.y >= -BALL_RADIUS && ball.vy > 0) {
            ball.y = -BALL_RADIUS - 0.1;
            ball.vy = -ball.vy * GROUND_RESTITUTION;
            ball.vx *= FRICTION;
            ball.vz *= FRICTION;
            ball.bounceCountSinceHit++;
            if (ball.bounceCountSinceHit >= 2) {
                awardPoint(ball.lastHitter == p1 ? p2 : p1, "Double bounce");
            } else {
                // Dust particles
                for (int i = 0; i < 10; i++) {
                    spawnParticle(ball.x, 0, ball.z, Color.color(0.9,0.9,0.9), 0.45);
                }
            }
            ball.applyTransform();
        }

        // Net collision: axis-aligned slab at X in [-NET_THICK/2 - R, NET_THICK/2 + R] and y within net height
        if (Math.abs(ball.x - NET_X) <= (NET_THICK/2.0 + BALL_RADIUS) && ball.y + BALL_RADIUS >= -NET_HEIGHT) {
            // Reflect X velocity, dampen a bit
            if (Math.signum(ball.vx) == Math.signum(ball.x - NET_X)) {
                // push outside
                ball.x = Math.copySign(NET_THICK/2.0 + BALL_RADIUS + 0.5, ball.x - NET_X);
            }
            ball.vx = -ball.vx * 0.45;
            ball.vy *= 0.85;
            for (int i = 0; i < 8; i++) spawnParticle(NET_X, ball.y, ball.z, Color.WHITE, 0.3);
        }

        // Court boundaries (out)
        if (ball.x < -COURT_HALF_LEN - 30) {
            awardPoint(p2, "Out (left)");
        } else if (ball.x > COURT_HALF_LEN + 30) {
            awardPoint(p1, "Out (right)");
        } else if (ball.z < -COURT_HALF_WID - 30 || ball.z > COURT_HALF_WID + 30) {
            // side out: point to opponent of last hitter
            awardPoint(ball.lastHitter == p1 ? p2 : p1, "Out (side)");
        }

        // Racket hits
        if (p1.tryHitBall(ball)) onHit(p1);
        if (p2.tryHitBall(ball)) onHit(p2);
    }

    private void onHit(Player3D hitter) {
        ball.lastHitter = hitter;
        ball.bounceCountSinceHit = 0;
        // Feedback particles
        for (int i = 0; i < 14; i++) spawnParticle(ball.x, ball.y, ball.z, hitter.primary, 0.45);
    }

    private void awardPoint(Player3D to, String reason) {
        if (state != RallyState.PLAY) return;
        state = RallyState.POINT;
        boolean toP1 = (to == p1);
        score.addPoint(toP1);
        showFloatingText(reason + " • Point " + (toP1 ? "P1" : "P2"), to.primary);
        // Alternate server
        server = (server == p1) ? p2 : p1;
        // Delay and re-serve
        new java.util.Timer(false).schedule(new java.util.TimerTask() {
            @Override public void run() {
                javafx.application.Platform.runLater(() -> initServe(server));
            }
        }, 900);
    }

    private void showFloatingText(String s, Color c) {
        Text t = new Text(s);
        t.setFont(Font.font("Verdana", FontWeight.BOLD, 26));
        t.setFill(c);
        t.setTranslateX(1280/2.0 - s.length() * 6);
        t.setTranslateY(120);
        hudLayer.getChildren().add(t);
        // simple fade using AnimationTimer-like approach
        final long start = System.nanoTime();
        AnimationTimer at = new AnimationTimer() {
            @Override public void handle(long now) {
                double a = 1.0 - (now - start) / 2_000_000_000.0;
                if (a <= 0) { hudLayer.getChildren().remove(t); stop(); }
                else t.setOpacity(a);
            }
        };
        at.start();
    }

    private void updateScoreboard() {
        scoreText.setText("Score: " + Score.pointsToString(score.pointsP1, score.pointsP2) +
                "   Games: " + score.gamesP1 + " - " + score.gamesP2);
        setText.setText("Sets: " + score.setsP1 + " - " + score.setsP2 + "   Server: " + (server == p1 ? "P1" : "P2"));
    }

    private void spawnParticle(double x, double y, double z, Color base, double life) {
        Sphere s = new Sphere(3 + rng.nextDouble() * 2);
        PhongMaterial m = new PhongMaterial(base);
        s.setMaterial(m);
        Particle3D p = new Particle3D(s, x, y, z,
                (rng.nextDouble()-0.5)*180,
                -120 + (rng.nextDouble()-0.5)*80,
                (rng.nextDouble()-0.5)*180, life);
        s.setUserData(p);
        fxGroup.getChildren().add(s);
    }

    // Entities

    private class Player3D {
        final String name;
        final boolean leftSide;
        final Color primary, secondary;

        Group root = new Group();
        Group model = new Group();
        MeshView racket;
        Cylinder handle;

        double x, y, z;       // position
        double vx = 0, vy = 0, vz = 0;
        boolean onGround = true;

        boolean swinging = false;
        long swingEndNanos = 0;

        Player3D(String name, double x, double y, double z, Color primary, Color secondary, boolean leftSide) {
            this.name = name; this.x = x; this.y = y; this.z = z;
            this.primary = primary; this.secondary = secondary; this.leftSide = leftSide;
            buildModel();
            applyTransform();
        }

        void buildModel() {
            // Torso (capsule-like using cylinder + spheres)
            PhongMaterial bodyMat = new PhongMaterial(primary);
            PhongMaterial skinMat = new PhongMaterial(secondary);

            Cylinder torso = new Cylinder(PLAYER_RADIUS, PLAYER_HEIGHT - 30);
            torso.setMaterial(bodyMat);

            Sphere head = new Sphere(PLAYER_RADIUS * 0.9);
            head.setMaterial(skinMat);
            head.setTranslateY(-(PLAYER_HEIGHT/2.0));

            // Legs
            Cylinder legL = new Cylinder(PLAYER_RADIUS*0.35, 30);
            legL.setMaterial(new PhongMaterial(primary.darker()));
            legL.setTranslateZ(-PLAYER_RADIUS*0.35);
            legL.setTranslateY((PLAYER_HEIGHT/2.0) - 15);
            Cylinder legR = new Cylinder(PLAYER_RADIUS*0.35, 30);
            legR.setMaterial(new PhongMaterial(primary.darker()));
            legR.setTranslateZ(PLAYER_RADIUS*0.35);
            legR.setTranslateY((PLAYER_HEIGHT/2.0) - 15);

            // Arms
            Cylinder armL = new Cylinder(PLAYER_RADIUS*0.3, 28);
            armL.setMaterial(skinMat);
            armL.getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
            armL.setTranslateY(-PLAYER_HEIGHT/2.0 + 30);
            armL.setTranslateX(-PLAYER_RADIUS - 12);

            Cylinder armR = new Cylinder(PLAYER_RADIUS*0.3, 28);
            armR.setMaterial(skinMat);
            armR.getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
            armR.setTranslateY(-PLAYER_HEIGHT/2.0 + 30);
            armR.setTranslateX(PLAYER_RADIUS + 12);

            // Racket: torus head + cylinder handle near right side
            racket = TorusMesh.createTorus(RACKET_RADIUS, 4, 28, 18, Color.WHITE);
            handle = new Cylinder(3, 24);
            handle.setMaterial(new PhongMaterial(Color.SADDLEBROWN));

            double side = leftSide ? 1 : -1;
            racket.setTranslateX(side * (PLAYER_RADIUS + 26));
            racket.setTranslateY(-PLAYER_HEIGHT/2.0 + 50);
            handle.setTranslateX(side * (PLAYER_RADIUS + 18));
            handle.setTranslateY(-PLAYER_HEIGHT/2.0 + 42);
            handle.setTranslateZ(0);
            handle.getTransforms().add(new Rotate(30 * side, Rotate.Z_AXIS));

            model.getChildren().addAll(torso, head, legL, legR, armL, armR, handle, racket);
            root.getChildren().add(model);
        }

        void setPosition(double x, double y, double z) { this.x = x; this.y = y; this.z = z; applyTransform(); }

        void applyTransform() {
            root.setTranslateX(x);
            root.setTranslateY(y);
            root.setTranslateZ(z);
        }

        void jump() {
            if (onGround) { vy = PLAYER_JUMP; onGround = false; }
        }

        void trySwing() {
            if (swinging) return;
            swinging = true;
            swingEndNanos = System.nanoTime() + SWING_MS * 1_000_000L;
            double side = leftSide ? 1 : -1;
            racket.setMaterial(new PhongMaterial(Color.YELLOW));
            // small visual swing rotation around Y
            Rotate r = new Rotate(0, Rotate.Y_AXIS);
            racket.getTransforms().add(r);
            AnimationTimer at = new AnimationTimer() {
                @Override public void handle(long now) {
                    double t = 1.0 - Math.max(0, (swingEndNanos - now) / (SWING_MS * 1_000_000.0));
                    if (t >= 1) {
                        racket.getTransforms().remove(r);
                        racket.setMaterial(new PhongMaterial(Color.WHITE));
                        swinging = false;
                        stop();
                    } else {
                        r.setAngle(Math.sin(t * Math.PI) * 40 * side);
                    }
                }
            };
            at.start();
        }

        boolean isSwingActive() {
            return swinging && System.nanoTime() < swingEndNanos;
        }

        boolean isBallInRacketRange(Ball3D b) {
            double rx = root.getTranslateX() + (leftSide ? (PLAYER_RADIUS + 26) : -(PLAYER_RADIUS + 26));
            double ry = root.getTranslateY() - PLAYER_HEIGHT/2.0 + 50;
            double rz = root.getTranslateZ();
            double dx = b.x - rx, dy = b.y - ry, dz = b.z - rz;
            double dist2 = dx*dx + dy*dy + dz*dz;
            double r = RACKET_RADIUS + BALL_RADIUS;
            return dist2 <= r*r;
        }

        boolean tryHitBall(Ball3D b) {
            if (!isSwingActive() || state != RallyState.PLAY) return false;
            if (!isBallInRacketRange(b)) return false;

            // Hit normal (from racket center to ball)
            double rx = root.getTranslateX() + (leftSide ? (PLAYER_RADIUS + 26) : -(PLAYER_RADIUS + 26));
            double ry = root.getTranslateY() - PLAYER_HEIGHT/2.0 + 50;
            double rz = root.getTranslateZ();
            double nx = b.x - rx, ny = b.y - ry, nz = b.z - rz;
            double len = Math.sqrt(nx*nx + ny*ny + nz*nz);
            if (len < 1e-6) { nx = (leftSide ? 1 : -1); ny = -0.2; nz = 0; } else { nx/=len; ny/=len; nz/=len; }

            // Bias forward (toward opponent)
            double dir = leftSide ? 1 : -1;
            nx = 0.7 * dir + 0.3 * nx;

            // Base speed + add from player motion
            double base = 780 + rng.nextDouble() * 360;
            double topSpinVy = -Math.signum(ny) * 120; // pull down/up a bit
            double sideSpinVz = (rng.nextDouble() - 0.5) * 120;

            b.vx = clamp(nx * base + vx * 0.35, -BALL_MAX_SPEED, BALL_MAX_SPEED);
            b.vy = clamp(ny * base * 0.4 + topSpinVy, -BALL_MAX_SPEED, BALL_MAX_SPEED);
            b.vz = clamp(nz * base * 0.35 + vz * 0.25 + sideSpinVz, -BALL_MAX_SPEED, BALL_MAX_SPEED);

            // Help clear net if too low
            if (b.y > -60) b.vy -= 140;

            // Visual feedback
            racket.setMaterial(new PhongMaterial(Color.YELLOW));

            return true;
        }

        void update(double dt) {
            // Integrate motion
            x += vx * dt;
            z += vz * dt;

            // Side constraints per side
            double minX = leftSide ? -COURT_HALF_LEN + 40 : 40;
            double maxX = leftSide ? -40 : COURT_HALF_LEN - 40;
            x = clamp(x, minX, maxX);
            z = clamp(z, -COURT_HALF_WID + 40, COURT_HALF_WID - 40);

            // Vertical
            vy += GRAVITY * dt;
            y += vy * dt;
            if (y >= -PLAYER_HEIGHT) {
                y = -PLAYER_HEIGHT;
                vy = 0;
                onGround = true;
            } else onGround = false;

            applyTransform();
        }
    }

    private static class TorusMesh {
        static MeshView createTorus(double majorR, double minorR, int tubeDiv, int ringDiv, Color color) {
            TriangleMesh mesh = new TriangleMesh();
            for (int i = 0; i <= ringDiv; i++) {
                double u = i * 2 * Math.PI / ringDiv;
                double cosu = Math.cos(u), sinu = Math.sin(u);
                for (int j = 0; j <= tubeDiv; j++) {
                    double v = j * 2 * Math.PI / tubeDiv;
                    double cosv = Math.cos(v), sinv = Math.sin(v);
                    float x = (float)((majorR + minorR * cosv) * cosu);
                    float y = (float)(minorR * sinv);
                    float z = (float)((majorR + minorR * cosv) * sinu);
                    mesh.getPoints().addAll(x, y, z);
                    mesh.getTexCoords().addAll((float)(i/(double)ringDiv), (float)(j/(double)tubeDiv));
                }
            }
            for (int i = 0; i < ringDiv; i++) {
                for (int j = 0; j < tubeDiv; j++) {
                    int p0 = i*(tubeDiv+1) + j;
                    int p1 = p0 + 1;
                    int p2 = (i+1)*(tubeDiv+1) + j;
                    int p3 = p2 + 1;
                    // two triangles per quad
                    mesh.getFaces().addAll(p0,0, p2,0, p1,0);
                    mesh.getFaces().addAll(p1,0, p2,0, p3,0);
                }
            }
            MeshView mv = new MeshView(mesh);
            mv.setCullFace(CullFace.BACK);
            PhongMaterial mat = new PhongMaterial(color);
            mv.setMaterial(mat);
            return mv;
        }
    }

    private class Ball3D {
        Sphere node;
        double x, y, z;
        double vx, vy, vz;

        // Trail
        final Deque<Sphere> trail = new ArrayDeque<>();
        double trailTimer = 0;

        Player3D lastHitter = null;
        int bounceCountSinceHit = 0;

        Ball3D(double x, double y, double z) {
            node = new Sphere(BALL_RADIUS);
            PhongMaterial mat = new PhongMaterial(Color.web("#ffb347"));
            mat.setSpecularColor(Color.WHITE);
            node.setMaterial(mat);
            setPosition(x, y, z);
        }

        void setPosition(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z; applyTransform();
        }

        void applyTransform() {
            node.setTranslateX(x);
            node.setTranslateY(y);
            node.setTranslateZ(z);
        }

        void updatePhysics(double dt) {
            // Air drag
            vx *= (1 - AIR_DRAG);
            vy *= (1 - AIR_DRAG);
            vz *= (1 - AIR_DRAG);
            // Gravity
            vy += GRAVITY * dt;
            // Integrate
            x += vx * dt; y += vy * dt; z += vz * dt;
            applyTransform();
        }

        void updateTrail(double dt, boolean active) {
            trailTimer += dt;
            if (active && trailTimer >= 0.03) {
                trailTimer = 0;
                Sphere s = new Sphere(BALL_RADIUS * 0.7);
                PhongMaterial m = new PhongMaterial(Color.color(1, 0.85, 0.5, 0.6));
                s.setMaterial(m);
                s.setTranslateX(x); s.setTranslateY(y); s.setTranslateZ(z);
                s.setUserData(new Particle3D(s, x, y, z, 0, 0, 0, 0.3, true));
                fxGroup.getChildren().add(0, s);
                trail.addLast(s);
                if (trail.size() > 22) {
                    Sphere old = trail.removeFirst();
                    fxGroup.getChildren().remove(old);
                }
            } else if (!active) {
                if (!trail.isEmpty()) {
                    Sphere old = trail.removeFirst();
                    fxGroup.getChildren().remove(old);
                }
            }
        }
    }

    private static class Particle3D {
        final Node node;
        double x, y, z, vx, vy, vz, life, maxLife;
        final boolean fadeOnly;

        Particle3D(Node node, double x, double y, double z, double vx, double vy, double vz, double life) {
            this(node, x, y, z, vx, vy, vz, life, false);
        }
        Particle3D(Node node, double x, double y, double z, double vx, double vy, double vz, double life, boolean fadeOnly) {
            this.node = node; this.x = x; this.y = y; this.z = z;
            this.vx = vx; this.vy = vy; this.vz = vz; this.life = this.maxLife = life; this.fadeOnly = fadeOnly;
        }

        boolean update(double dt) {
            life -= dt;
            if (life <= 0) return false;
            x += vx * dt; y += vy * dt; z += vz * dt;
            vy += 400 * dt;
            node.setTranslateX(x); node.setTranslateY(y); node.setTranslateZ(z);
            double alpha = Math.max(0, life / maxLife);
            if (node instanceof Shape3D s3d) {
                PhongMaterial m = (PhongMaterial) s3d.getMaterial();
                Color base = (m.getDiffuseColor() == null || m.getDiffuseColor().getOpacity() == 0) ? Color.WHITE : m.getDiffuseColor();
                m.setDiffuseColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), fadeOnly ? alpha * 0.6 : alpha));
            }
            return true;
        }
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    public static void main(String[] args) { launch(args); }
}