import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class TennisGameApp extends Application {

    // Window and court
    static final double WIDTH = 1280;
    static final double HEIGHT = 720;

    // Court dimensions (side view)
    static final double GROUND_Y = HEIGHT - 90;
    static final double COURT_LEFT = 90;
    static final double COURT_RIGHT = WIDTH - 90;
    static final double NET_X = WIDTH / 2.0;
    static final double NET_HEIGHT = 150;
    static final double NET_HALF_THICK = 5;

    // Physics
    static final double GRAVITY = 1800; // px/s^2
    static final double GROUND_RESTITUTION = 0.70;
    static final double AIR_DRAG = 0.0005;
    static final double FRICTION = 0.80;

    // Players
    static final double PLAYER_SPEED = 460;
    static final double PLAYER_JUMP = -740;
    static final double PLAYER_WIDTH = 26;
    static final double PLAYER_HEIGHT = 120;

    // Racket
    static final double RACKET_RADIUS = 26;
    static final double SWING_TIME_MS = 220;

    // Ball
    static final double BALL_RADIUS = 12;
    static final double BALL_MAX_SPEED = 1400;

    // Options: set to true and provide a valid image URL (or "file:/...") to use a photo background
    static final boolean USE_KREMLIN_IMAGE = false;
    static final String KREMLIN_IMAGE_URL = "https://upload.wikimedia.org/wikipedia/commons/3/3e/Moscow_July_2011-16a.jpg"; // CC-BY; replace with your own if needed

    private Pane world;
    private Group bgLayer;
    private Group courtLayer;
    private Group fxLayer;
    private Group uiLayer;

    private Player p1, p2;
    private Ball ball;

    private final Set<KeyCode> keys = new HashSet<>();
    private final BooleanProperty paused = new SimpleBooleanProperty(false);
    private boolean aiEnabled = true;

    // Game state
    private enum RallyState { READY, SERVE, PLAY, POINT, GAME_OVER }
    private RallyState state = RallyState.SERVE;
    private Player server;
    private Player receiver;
    private long lastTime;
    private int bounceCountSinceHit = 0;
    private Player lastHitter = null;

    private final Random rng = new Random();

    // Scoring
    private static class Score {
        int pointsP1 = 0;
        int pointsP2 = 0;
        int gamesP1 = 0;
        int gamesP2 = 0;
        int setsP1 = 0;
        int setsP2 = 0;

        static String pointsToString(int p1, int p2) {
            if (p1 >= 3 && p2 >= 3) {
                if (p1 == p2) return "Deuce";
                if (p1 == p2 + 1) return "Adv P1";
                if (p2 == p1 + 1) return "Adv P2";
            }
            return fmt(p1) + " - " + fmt(p2);
        }
        private static String fmt(int p) {
            return switch (p) {
                case 0 -> "Love";
                case 1 -> "15";
                case 2 -> "30";
                case 3 -> "40";
                default -> Integer.toString(p);
            };
        }
        void addPoint(boolean toP1) {
            if (toP1) {
                if (pointsP1 >= 3 && pointsP2 >= 3) {
                    if (pointsP1 == pointsP2) pointsP1++; // Adv
                    else if (pointsP1 == pointsP2 + 1) { // Win game
                        gamesP1++; pointsP1 = pointsP2 = 0;
                    } else { // p2 had advantage
                        pointsP2--; // back to deuce
                    }
                } else {
                    pointsP1++;
                    if (pointsP1 >= 4 && pointsP1 >= pointsP2 + 2) {
                        gamesP1++;
                        pointsP1 = pointsP2 = 0;
                    }
                }
            } else {
                if (pointsP1 >= 3 && pointsP2 >= 3) {
                    if (pointsP1 == pointsP2) pointsP2++; // Adv
                    else if (pointsP2 == pointsP1 + 1) { // Win game
                        gamesP2++; pointsP1 = pointsP2 = 0;
                    } else { // p1 had advantage
                        pointsP1--; // back to deuce
                    }
                } else {
                    pointsP2++;
                    if (pointsP2 >= 4 && pointsP2 >= pointsP1 + 2) {
                        gamesP2++;
                        pointsP1 = pointsP2 = 0;
                    }
                }
            }
            // Simple 1-set logic to 6 (win by 2)
            if ((gamesP1 >= 6 || gamesP2 >= 6) && Math.abs(gamesP1 - gamesP2) >= 2) {
                if (gamesP1 > gamesP2) setsP1++; else setsP2++;
                gamesP1 = gamesP2 = 0;
                pointsP1 = pointsP2 = 0;
            }
        }
    }
    private final Score score = new Score();

    // UI
    private Text scoreText;
    private Text setText;
    private Text modeText;
    private Text hintText;

    @Override
    public void start(Stage stage) {
        world = new Pane();
        world.setPrefSize(WIDTH, HEIGHT);
        bgLayer = new Group();
        courtLayer = new Group();
        fxLayer = new Group();
        uiLayer = new Group();

        buildBackground();
        buildCourt();
        buildPlayers();
        buildBall();
        buildUI();

        world.getChildren().addAll(bgLayer, courtLayer, fxLayer, uiLayer);

        Scene scene = new Scene(world, WIDTH, HEIGHT, Color.BLACK);
        scene.setOnKeyPressed(e -> keys.add(e.getCode()));
        scene.setOnKeyReleased(e -> keys.remove(e.getCode()));
        setupKeyShortcuts(scene);

        stage.setTitle("Tennis Match: Kremlin Court");
        stage.setScene(scene);
        stage.show();

        initServe(p1); // P1 starts serving
        lastTime = System.nanoTime();

        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (paused.get()) {
                    lastTime = now;
                    return;
                }
                double dt = (now - lastTime) / 1_000_000_000.0;
                dt = Math.min(dt, 1.0/30.0);
                lastTime = now;

                update(dt);
            }
        };
        timer.start();
    }

    private void setupKeyShortcuts(Scene scene) {
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F1) {
                aiEnabled = !aiEnabled;
                modeText.setText(aiEnabled ? "Mode: Player 1 vs Computer (F1 to toggle)" :
                        "Mode: Player 1 vs Player 2 (F1 to toggle)");
                e.consume();
            } else if (e.getCode() == KeyCode.R) {
                resetMatch();
                e.consume();
            } else if (e.getCode() == KeyCode.P) {
                paused.set(!paused.get());
                hintText.setText(paused.get() ? "Paused (P to resume)" : controlsHint());
            }
        });
    }

    private void buildBackground() {
        // Sky gradient
        Stop[] skyStops = new Stop[]{
                new Stop(0, Color.web("#3d6ff0")),
                new Stop(0.5, Color.web("#69a1ff")),
                new Stop(1, Color.web("#e6f2ff"))
        };
        Rectangle sky = new Rectangle(0, 0, WIDTH, HEIGHT);
        sky.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, skyStops));
        bgLayer.getChildren().add(sky);

        // Sun
        Circle sun = new Circle(140, 120, 70,
                new RadialGradient(0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#fff2a8")),
                        new Stop(1, Color.web("#ffcc33", 0.0))));
        sun.setEffect(new Glow(0.6));
        bgLayer.getChildren().add(sun);

        // Optional photo background
        if (USE_KREMLIN_IMAGE) {
            try {
                Image img = new Image(KREMLIN_IMAGE_URL, WIDTH, HEIGHT * 0.6, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(WIDTH);
                iv.setPreserveRatio(true);
                iv.setTranslateY(HEIGHT * 0.4 - iv.getImage().getHeight());
                iv.setOpacity(0.85);
                bgLayer.getChildren().add(iv);
            } catch (Exception ignored) {}
        } else {
            // Stylized Kremlin skyline (vector)
            Group skyline = kremlinSkyline();
            skyline.setTranslateY(HEIGHT * 0.42);
            bgLayer.getChildren().add(skyline);
        }

        // Distant clouds
        for (int i = 0; i < 6; i++) {
            double y = 80 + rng.nextDouble() * 140;
            Group cloud = cloudShape(120 + rng.nextDouble() * 220);
            cloud.setOpacity(0.55);
            cloud.setTranslateX(rng.nextDouble() * WIDTH);
            cloud.setTranslateY(y);
            bgLayer.getChildren().add(cloud);

            TranslateTransition drift = new TranslateTransition(Duration.seconds(60 + rng.nextInt(40)), cloud);
            drift.setFromX(-200);
            drift.setToX(WIDTH + 200);
            drift.setCycleCount(Animation.INDEFINITE);
            drift.setAutoReverse(false);
            drift.play();
        }
    }

    private Group kremlinSkyline() {
        Group g = new Group();

        // Wall base
        Rectangle wall = new Rectangle(0, 140, WIDTH, 130);
        wall.setFill(Color.web("#b9423a"));
        wall.setEffect(new Lighting());

        // Merlons
        Group merlons = new Group();
        for (int i = 0; i < 26; i++) {
            Rectangle r = new Rectangle(30 + i * 46, 120, 26, 24);
            r.setArcWidth(10); r.setArcHeight(10);
            r.setFill(Color.web("#a7372f"));
            merlons.getChildren().add(r);
        }

        // Tower with star
        Group tower = new Group();
        Polygon spire = new Polygon(
                NET_X - 30, 60,
                NET_X + 30, 60,
                NET_X + 12, 140,
                NET_X - 12, 140
        );
        spire.setFill(Color.web("#2a7b4f"));
        spire.setEffect(new DropShadow(10, Color.color(0,0,0,0.3)));
        Circle dome = new Circle(NET_X, 60, 18, Color.web("#d4b92b"));
        dome.setEffect(new Bloom(0.25));

        Polygon star = starPolygon(NET_X, 40, 14, 6, 5);
        star.setFill(Color.web("#ffd94d"));
        star.setEffect(new Glow(0.8));

        Rectangle body = new Rectangle(NET_X - 36, 140, 72, 110);
        body.setFill(Color.web("#b9423a"));

        tower.getChildren().addAll(body, spire, dome, star);

        // Onion domes
        Group domes = new Group();
        for (int i = 0; i < 4; i++) {
            double x = 200 + i * 220 + (i % 2 == 0 ? 40 : -30);
            Path onion = onionDome(x, 120, 42, 70);
            onion.setFill(i % 2 == 0 ? Color.web("#2b9d5b") : Color.web("#bd9c2b"));
            onion.setStroke(Color.color(1,1,1,0.2));
            domes.getChildren().add(onion);
        }

        g.getChildren().addAll(wall, merlons, domes, tower);
        g.setOpacity(0.88);
        return g;
    }

    private Polygon starPolygon(double cx, double cy, double outerR, double innerR, int points) {
        Polygon p = new Polygon();
        for (int i = 0; i < points * 2; i++) {
            double ang = Math.PI / points * i - Math.PI / 2;
            double r = (i % 2 == 0) ? outerR : innerR;
            p.getPoints().addAll(cx + Math.cos(ang) * r, cy + Math.sin(ang) * r);
        }
        return p;
    }

    private Path onionDome(double cx, double baseY, double w, double h) {
        Path path = new Path();
        double topY = baseY - h;
        MoveTo m = new MoveTo(cx, topY);
        QuadCurveTo q1 = new QuadCurveTo(cx - w * 0.9, topY + h * 0.35, cx - w, baseY);
        QuadCurveTo q2 = new QuadCurveTo(cx - w * 0.55, topY + h * 0.55, cx, baseY - h * 0.25);
        QuadCurveTo q3 = new QuadCurveTo(cx + w * 0.55, topY + h * 0.55, cx + w, baseY);
        QuadCurveTo q4 = new QuadCurveTo(cx + w * 0.9, topY + h * 0.35, cx, topY);
        path.getElements().addAll(m, q1, q2, q3, q4);
        path.setEffect(new InnerShadow(10, Color.color(0,0,0,0.2)));
        return path;
    }

    private Group cloudShape(double w) {
        Group g = new Group();
        Color c = Color.rgb(255, 255, 255, 0.85);
        g.getChildren().add(new Circle(0, 0, w * 0.25, c));
        g.getChildren().add(new Circle(w * 0.2, -8, w * 0.3, c));
        g.getChildren().add(new Circle(w * 0.45, -4, w * 0.22, c));
        g.getChildren().add(new Circle(w * 0.65, 2, w * 0.2, c));
        g.setEffect(new GaussianBlur(8));
        return g;
    }

    private void buildCourt() {
        // Court base
        Rectangle ground = new Rectangle(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);
        ground.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#37a863")),
                new Stop(1, Color.web("#22643d"))));
        courtLayer.getChildren().add(ground);

        // Court plate
        Rectangle court = new Rectangle(COURT_LEFT, GROUND_Y - 8, COURT_RIGHT - COURT_LEFT, 10);
        court.setFill(Color.web("#f8f8f8", 0.2));
        courtLayer.getChildren().add(court);

        // Baselines
        Line leftBase = new Line(COURT_LEFT, GROUND_Y, COURT_LEFT, GROUND_Y - 2);
        Line rightBase = new Line(COURT_RIGHT, GROUND_Y, COURT_RIGHT, GROUND_Y - 2);
        leftBase.setStroke(Color.WHITE);
        rightBase.setStroke(Color.WHITE);
        leftBase.setStrokeWidth(4);
        rightBase.setStrokeWidth(4);
        courtLayer.getChildren().addAll(leftBase, rightBase);

        // Net
        Rectangle netPostL = new Rectangle(NET_X - NET_HALF_THICK - 3, GROUND_Y - NET_HEIGHT - 5, 6, NET_HEIGHT + 10);
        Rectangle netPostR = new Rectangle(NET_X + NET_HALF_THICK - 3, GROUND_Y - NET_HEIGHT - 5, 6, NET_HEIGHT + 10);
        netPostL.setFill(Color.SILVER);
        netPostR.setFill(Color.SILVER);

        Rectangle netBand = new Rectangle(NET_X - NET_HALF_THICK - 80, GROUND_Y - NET_HEIGHT - 8, 160, 14);
        netBand.setFill(Color.WHITE);
        netBand.setEffect(new DropShadow(10, Color.color(0,0,0,0.25)));

        Canvas netMesh = new Canvas(170, NET_HEIGHT);
        netMesh.setTranslateX(NET_X - 85);
        netMesh.setTranslateY(GROUND_Y - NET_HEIGHT);
        GraphicsContext gc = netMesh.getGraphicsContext2D();
        gc.setStroke(Color.color(1,1,1,0.65));
        for (int y = 0; y < NET_HEIGHT; y += 10) {
            gc.strokeLine(0, y, 170, y);
        }
        for (int x = 0; x < 170; x += 10) {
            gc.strokeLine(x, 0, x, NET_HEIGHT);
        }
        netMesh.setEffect(new MotionBlur(90, 2));

        courtLayer.getChildren().addAll(netPostL, netPostR, netMesh, netBand);
    }

    private void buildPlayers() {
        p1 = new Player("P1", COURT_LEFT + 120, GROUND_Y - PLAYER_HEIGHT, Color.web("#2b8cff"), Color.web("#ffffff"));
        p2 = new Player("P2", COURT_RIGHT - 120, GROUND_Y - PLAYER_HEIGHT, Color.web("#ff4d4d"), Color.web("#ffffff"));
        courtLayer.getChildren().addAll(p1.root, p2.root);
    }

    private void buildBall() {
        ball = new Ball(NET_X - 120, GROUND_Y - 200);
        courtLayer.getChildren().add(ball.node);
    }

    private void buildUI() {
        // Glass panel
        Rectangle glass = new Rectangle(20, 20, 480, 106);
        glass.setArcWidth(16); glass.setArcHeight(16);
        glass.setFill(Color.color(0,0,0,0.25));
        glass.setStroke(Color.color(1,1,1,0.35));
        glass.setEffect(new GaussianBlur(8));

        scoreText = new Text();
        scoreText.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
        scoreText.setFill(Color.WHITE);
        scoreText.setEffect(new DropShadow(10, Color.color(0,0,0,0.45)));
        scoreText.setX(36); scoreText.setY(60);

        setText = new Text();
        setText.setFont(Font.font("Verdana", FontWeight.SEMI_BOLD, 18));
        setText.setFill(Color.WHITE);
        setText.setEffect(new DropShadow(10, Color.color(0,0,0,0.45)));
        setText.setX(36); setText.setY(92);

        modeText = new Text();
        modeText.setFont(Font.font("Verdana", FontWeight.NORMAL, 16));
        modeText.setFill(Color.WHITE);
        modeText.setEffect(new DropShadow(10, Color.color(0,0,0,0.45)));
        modeText.setX(520); modeText.setY(44);

        hintText = new Text(controlsHint());
        hintText.setFont(Font.font("Verdana", 14));
        hintText.setFill(Color.WHITE);
        hintText.setEffect(new DropShadow(10, Color.color(0,0,0,0.45)));
        hintText.setX(520); hintText.setY(74);

        uiLayer.getChildren().addAll(glass, scoreText, setText, modeText, hintText);
        updateScoreboard();
        modeText.setText(aiEnabled ? "Mode: Player 1 vs Computer (F1 to toggle)" :
                "Mode: Player 1 vs Player 2 (F1 to toggle)");
    }

    private String controlsHint() {
        return "P1: A/D move, W jump, F swing, SPACE serve | P2: Left/Right move, Up jump, / swing, ENTER serve | F1 toggle AI, R reset, P pause";
    }

    private void update(double dt) {
        handleInput(dt);

        // AI
        if (aiEnabled) {
            aiForP2(dt);
        }

        // Update players (gravity and clamp)
        p1.update(dt);
        p2.update(dt);

        // Update ball physics depending on state
        switch (state) {
            case SERVE -> {
                // Ball attached near server's racket height until served
                ball.vx = 0; ball.vy = 0;
                double sx = server == p1 ? server.x + 30 : server.x - 30;
                double sy = server.y + 40;
                ball.setPosition(sx, sy);
                ball.updateTrail(dt, false);
            }
            case PLAY -> {
                ball.updatePhysics(dt);
                ballCollisions(dt);
                ball.updateTrail(dt, true);
            }
            case POINT -> {
                ball.updateTrail(dt, false);
            }
            default -> {}
        }

        updateScoreboard();
    }

    private void handleInput(double dt) {
        // Player 1
        double dir1 = 0;
        if (keys.contains(KeyCode.A)) dir1 -= 1;
        if (keys.contains(KeyCode.D)) dir1 += 1;
        p1.vx = dir1 * PLAYER_SPEED;

        if (keys.contains(KeyCode.W)) p1.jump();

        if (keys.contains(KeyCode.F)) p1.trySwing();

        if (keys.contains(KeyCode.SPACE)) attemptServe(p1);

        // Player 2 (human if AI disabled)
        if (!aiEnabled) {
            double dir2 = 0;
            if (keys.contains(KeyCode.LEFT)) dir2 -= 1;
            if (keys.contains(KeyCode.RIGHT)) dir2 += 1;
            p2.vx = dir2 * PLAYER_SPEED;

            if (keys.contains(KeyCode.UP)) p2.jump();
            if (keys.contains(KeyCode.SLASH)) p2.trySwing();
            if (keys.contains(KeyCode.ENTER)) attemptServe(p2);
        }
    }

    private void aiForP2(double dt) {
        // Only move if not serving or in serve state for p2
        if (state == RallyState.SERVE && server == p2) {
            // Auto-serve after slight delay if user doesn't press
            if (rng.nextDouble() < 0.01) attemptServe(p2);
            return;
        }

        // Simple predictive AI: move to predicted x when ball is coming to the right
        double targetX = COURT_RIGHT - 140;
        if (state == RallyState.PLAY) {
            if (ball.vx > 0) {
                // Predict the ball ground intercept (ignoring net post-contacts)
                double timeToGround;
                if (ball.vy >= 0) {
                    // time until hits ground from current height
                    double h = (GROUND_Y - BALL_RADIUS) - ball.y;
                    if (h < 0) h = 0;
                    double a = 0.5 * GRAVITY;
                    double b = ball.vy;
                    double c = -h;
                    double disc = b*b - 4*a*c;
                    if (disc >= 0) {
                        timeToGround = (-b + Math.sqrt(disc)) / (2*a);
                    } else timeToGround = 0.6;
                } else {
                    // going up: time to apex + down to ground
                    double tApex = -ball.vy / GRAVITY;
                    double yApex = ball.y + ball.vy * tApex + 0.5 * GRAVITY * tApex * tApex;
                    double h = (GROUND_Y - BALL_RADIUS) - yApex;
                    double tDown = Math.sqrt(Math.max(0, 2*h / GRAVITY));
                    timeToGround = tApex + tDown;
                }
                targetX = ball.x + ball.vx * timeToGround * 0.94;
                // clamp inside AI side
                targetX = Math.max(NET_X + 60, Math.min(COURT_RIGHT - 80, targetX));
            } else {
                targetX = Math.max(NET_X + 80, Math.min(COURT_RIGHT - 120, (COURT_RIGHT - 120 + NET_X + 100)/2));
            }
        }
        double dx = targetX - p2.x;
        if (Math.abs(dx) > 8) {
            p2.vx = Math.signum(dx) * PLAYER_SPEED * (0.9 + 0.2 * rng.nextDouble());
        } else {
            p2.vx = 0;
        }

        // Jump for high balls near
        if (state == RallyState.PLAY && ball.vx > 0 && ball.y < p2.y - 30 && Math.abs(ball.x - p2.x) < 100) {
            p2.jump();
        }

        // Auto-swing when ball near and in front
        if (state == RallyState.PLAY && Math.abs(ball.x - p2.racketCenterX()) < 46 &&
                Math.abs(ball.y - p2.racketCenterY()) < 60 && ball.vx > 0) {
            p2.trySwing();
        }
    }

    private void attemptServe(Player who) {
        if (state != RallyState.SERVE || server != who) return;
        who.trySwing();
        // Serve: give initial upward and forward velocity
        double direction = (who == p1) ? 1 : -1;
        double speed = 850 + rng.nextDouble() * 140;
        double angle = Math.toRadians(18 + rng.nextDouble() * 6);
        ball.vx = Math.cos(angle) * speed * direction;
        ball.vy = -Math.sin(angle) * speed * 0.72;
        lastHitter = who;
        bounceCountSinceHit = 0;
        state = RallyState.PLAY;
        hintText.setText("");
    }

    private void initServe(Player who) {
        server = who;
        receiver = (who == p1) ? p2 : p1;
        state = RallyState.SERVE;
        hintText.setText((server == p1 ? "P1" : "P2") + " to serve (" + (server==p1 ? "SPACE" : (aiEnabled ? "AI" : "ENTER")) + ")");
    }

    private void ballCollisions(double dt) {
        // Ground bounce
        if (ball.y + BALL_RADIUS >= GROUND_Y) {
            if (ball.vy > 0) {
                ball.y = GROUND_Y - BALL_RADIUS - 0.1;
                ball.vy = -ball.vy * GROUND_RESTITUTION;
                ball.vx *= FRICTION;
                bounceCountSinceHit++;
                if (bounceCountSinceHit >= 2) {
                    // Double bounce: point to the opponent of last hitter
                    awardPoint(lastHitter == p1 ? p2 : p1, "Double bounce");
                } else {
                    // Small dust
                    hitSpark(ball.x, GROUND_Y - 4, Color.color(0.9,0.9,0.9), 10);
                }
            }
        }

        // Net collision (simple rect)
        if (Math.abs(ball.x - NET_X) < NET_HALF_THICK + BALL_RADIUS &&
                ball.y + BALL_RADIUS >= GROUND_Y - NET_HEIGHT) {
            // reflect X, dampen
            if (Math.signum(ball.vx) == Math.signum(ball.x - NET_X)) {
                // prevent sticking: move just outside
                ball.x = NET_X + Math.signum(ball.x - NET_X) * (NET_HALF_THICK + BALL_RADIUS + 0.5);
            }
            ball.vx = -ball.vx * 0.45;
            ball.vy *= 0.85;
            hitSpark(NET_X, ball.y, Color.WHITE, 8);
        }

        // Out left/right
        if (ball.x < COURT_LEFT - 30) {
            awardPoint(p2, "Out (left)");
        } else if (ball.x > COURT_RIGHT + 30) {
            awardPoint(p1, "Out (right)");
        }

        // Racket hits
        if (p1.tryHitBall(ball)) {
            onHit(p1);
        }
        if (p2.tryHitBall(ball)) {
            onHit(p2);
        }
    }

    private void onHit(Player hitter) {
        lastHitter = hitter;
        bounceCountSinceHit = 0;

        // Add particle and trail glow
        hitSpark(ball.x, ball.y, hitter.primary, 14);

        // Slight camera shake effect (subtle)
        TranslateTransition shake = new TranslateTransition(Duration.millis(90), world);
        shake.setFromX(0);
        shake.setToX((rng.nextDouble() - 0.5) * 4);
        shake.setAutoReverse(true);
        shake.setCycleCount(2);
        shake.play();
    }

    private void awardPoint(Player to, String reason) {
        if (state != RallyState.PLAY) return;
        state = RallyState.POINT;
        // Score update
        boolean toP1 = (to == p1);
        score.addPoint(toP1);
        Text t = new Text(reason + " • Point " + (toP1 ? "P1" : "P2"));
        t.setFont(Font.font("Verdana", FontWeight.BOLD, 26));
        t.setFill(to.primary);
        t.setEffect(new DropShadow(18, Color.color(0,0,0,0.45)));
        t.setX(WIDTH/2.0 - 150);
        t.setY(120);
        uiLayer.getChildren().add(t);
        FadeTransition ft = new FadeTransition(Duration.seconds(2.0), t);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> uiLayer.getChildren().remove(t));
        ft.play();

        // Next serve: alternate server
        server = (server == p1) ? p2 : p1;
        receiver = (server == p1) ? p2 : p1;

        // Reset ball after short delay
        PauseTransition pause = new PauseTransition(Duration.millis(900));
        pause.setOnFinished(e -> initServe(server));
        pause.play();
    }

    private void resetMatch() {
        score.pointsP1 = score.pointsP2 = 0;
        score.gamesP1 = score.gamesP2 = 0;
        score.setsP1 = score.setsP2 = 0;
        p1.setPosition(COURT_LEFT + 120, GROUND_Y - PLAYER_HEIGHT);
        p2.setPosition(COURT_RIGHT - 120, GROUND_Y - PLAYER_HEIGHT);
        ball.setPosition(NET_X - 120, GROUND_Y - 200);
        ball.vx = ball.vy = 0;
        initServe(p1);
        hintText.setText(controlsHint());
    }

    private void updateScoreboard() {
        scoreText.setText("Score: " + Score.pointsToString(score.pointsP1, score.pointsP2) +
                "   Games: " + score.gamesP1 + " - " + score.gamesP2);
        setText.setText("Sets: " + score.setsP1 + " - " + score.setsP2 +
                "   Server: " + (server == p1 ? "P1" : "P2"));
    }

    private void hitSpark(double x, double y, Color base, int count) {
        for (int i = 0; i < count; i++) {
            Circle c = new Circle(x, y, 2 + rng.nextDouble() * 2,
                    base.deriveColor(0, 1.0, 1.0, 0.9));
            c.setEffect(new Glow(0.7));
            fxLayer.getChildren().add(c);
            double ang = rng.nextDouble() * Math.PI * 2;
            double spd = 160 + rng.nextDouble() * 240;
            double vx = Math.cos(ang) * spd;
            double vy = Math.sin(ang) * spd - 120;
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(c.translateXProperty(), 0),
                            new KeyValue(c.translateYProperty(), 0),
                            new KeyValue(c.opacityProperty(), 1.0),
                            new KeyValue(c.radiusProperty(), c.getRadius())),
                    new KeyFrame(Duration.millis(400 + rng.nextInt(260)),
                            new KeyValue(c.translateXProperty(), vx * 0.3),
                            new KeyValue(c.translateYProperty(), vy * 0.3 + 120),
                            new KeyValue(c.opacityProperty(), 0.0),
                            new KeyValue(c.radiusProperty(), c.getRadius() * 0.4))
            );
            tl.setOnFinished(e -> fxLayer.getChildren().remove(c));
            tl.play();
        }
    }

    // Player class (vector character + racket)
    private class Player {
        final String name;
        final Group root = new Group();
        final Color primary;
        final Color secondary;

        double x, y;
        double vx = 0, vy = 0;
        boolean onGround = true;

        // Body parts
        Rectangle torso;
        Circle head;
        Line legL, legR, armL, armR;
        Circle racketHead;
        Line racketHandle;

        boolean swinging = false;
        long swingEndNanos = 0;

        Player(String name, double x, double y, Color primary, Color secondary) {
            this.name = name;
            this.primary = primary;
            this.secondary = secondary;
            this.x = x;
            this.y = y;
            build();
            setPosition(x, y);
        }

        void build() {
            // Torso
            torso = new Rectangle(0, 0, PLAYER_WIDTH, PLAYER_HEIGHT - 30);
            torso.setArcWidth(8); torso.setArcHeight(8);
            torso.setFill(primary);
            torso.setStroke(Color.BLACK);
            torso.setStrokeWidth(1.0);
            torso.setEffect(new DropShadow(8, Color.color(0,0,0,0.35)));

            // Head
            head = new Circle(PLAYER_WIDTH * 0.5, -18, 16, secondary);
            head.setStroke(Color.BLACK);
            head.setStrokeWidth(1);

            // Legs
            legL = new Line(PLAYER_WIDTH * 0.25, PLAYER_HEIGHT - 30, PLAYER_WIDTH * 0.25, PLAYER_HEIGHT);
            legR = new Line(PLAYER_WIDTH * 0.75, PLAYER_HEIGHT - 30, PLAYER_WIDTH * 0.75, PLAYER_HEIGHT);
            legL.setStrokeWidth(6); legR.setStrokeWidth(6);
            legL.setStroke(primary.darker()); legR.setStroke(primary.darker());

            // Arms
            armL = new Line(PLAYER_WIDTH * 0.2, 30, PLAYER_WIDTH * 0.8, 30);
            armL.setStrokeWidth(6); armL.setStroke(secondary);
            armR = new Line(PLAYER_WIDTH * 0.8, 30, PLAYER_WIDTH * 0.8, 60);
            armR.setStrokeWidth(6); armR.setStroke(secondary);

            // Racket
            racketHead = new Circle(PLAYER_WIDTH + 26, 48, RACKET_RADIUS, Color.TRANSPARENT);
            racketHead.setStroke(Color.WHITE);
            racketHead.setStrokeWidth(3);
            racketHead.setEffect(new Glow(0.4));
            racketHandle = new Line(PLAYER_WIDTH * 0.8, 40, PLAYER_WIDTH + 18, 48);
            racketHandle.setStrokeWidth(5);
            racketHandle.setStroke(Color.SADDLEBROWN);

            // Shadow
            Ellipse shadow = new Ellipse(PLAYER_WIDTH * 0.5, PLAYER_HEIGHT + 4, 28, 6);
            shadow.setFill(Color.color(0,0,0,0.3));
            shadow.setEffect(new GaussianBlur(6));

            root.getChildren().addAll(shadow, legL, legR, torso, head, armL, armR, racketHandle, racketHead);

            // Idle breathing
            ScaleTransition breathe = new ScaleTransition(Duration.seconds(2.2), torso);
            breathe.setFromY(1.0); breathe.setToY(1.03);
            breathe.setAutoReverse(true);
            breathe.setCycleCount(Animation.INDEFINITE);
            breathe.play();
        }

        void setPosition(double x, double y) {
            this.x = x; this.y = y;
            root.setTranslateX(x);
            root.setTranslateY(y);
        }

        void jump() {
            if (onGround) {
                vy = PLAYER_JUMP;
                onGround = false;
            }
        }

        void trySwing() {
            if (swinging) return;
            swinging = true;
            swingEndNanos = System.nanoTime() + (long)(SWING_TIME_MS * 1_000_000);
            // Visual swing: rotate racket head around handle
            RotateTransition rt = new RotateTransition(Duration.millis(SWING_TIME_MS), racketHead);
            rt.setFromAngle(-18);
            rt.setToAngle(42);
            rt.setAutoReverse(false);
            rt.setOnFinished(e -> {
                swinging = false;
                racketHead.setRotate(0);
            });
            rt.play();
            // Arm animation
            RotateTransition armRt = new RotateTransition(Duration.millis(SWING_TIME_MS), armR);
            armRt.setFromAngle(-10); armRt.setToAngle(30);
            armRt.play();
        }

        boolean isSwingActive() {
            return swinging && System.nanoTime() < swingEndNanos;
        }

        // Approximate racket center
        double racketCenterX() { return root.getTranslateX() + PLAYER_WIDTH + 26; }
        double racketCenterY() { return root.getTranslateY() + 48; }

        boolean tryHitBall(Ball b) {
            if (!isSwingActive() || state != RallyState.PLAY) return false;
            double dx = b.x - racketCenterX();
            double dy = b.y - racketCenterY();
            double dist2 = dx*dx + dy*dy;
            double rad = RACKET_RADIUS + BALL_RADIUS;
            if (dist2 <= rad * rad) {
                // Compute hit vector away from player
                double nx = (dx == 0 && dy == 0) ? (this==p1?1:-1) : dx / Math.sqrt(dist2);
                double ny = (dx == 0 && dy == 0) ? -0.2 : dy / Math.sqrt(dist2);

                // Base outgoing speed
                double base = 720 + rng.nextDouble() * 360;

                // Forward bias
                double dir = (this == p1) ? 1 : -1;
                nx = 0.7 * dir + 0.3 * nx;

                // Add spin depending on swing angle and vertical offset
                double topSpin = -Math.signum(dy) * 140;

                b.vx = clamp((nx) * base + vx * 0.35, -BALL_MAX_SPEED, BALL_MAX_SPEED);
                b.vy = clamp((ny) * base * 0.4 + topSpin, -BALL_MAX_SPEED, BALL_MAX_SPEED);

                // Slight pull upward if too low to help clear net
                if (b.y > GROUND_Y - 70) b.vy -= 120;

                // Visual feedback
                racketHead.setStroke(Color.YELLOW);
                PauseTransition pt = new PauseTransition(Duration.millis(90));
                pt.setOnFinished(e -> racketHead.setStroke(Color.WHITE));
                pt.play();

                return true;
            }
            return false;
        }

        void update(double dt) {
            // Horizontal movement
            x += vx * dt;

            // Boundaries: each player stays mostly on their side, but can approach net
            double minX = (this == p1) ? COURT_LEFT + 40 : NET_X + 40;
            double maxX = (this == p1) ? NET_X - 40 : COURT_RIGHT - 40;
            x = clamp(x, minX, maxX);

            // Gravity
            vy += GRAVITY * dt;
            y += vy * dt;

            // Ground collision
            if (y + PLAYER_HEIGHT >= GROUND_Y) {
                y = GROUND_Y - PLAYER_HEIGHT;
                vy = 0;
                onGround = true;
            } else {
                onGround = false;
            }

            // Apply to node
            root.setTranslateX(x);
            root.setTranslateY(y);
        }
    }

    private class Ball {
        final Circle node;
        double x, y, vx, vy;

        // Trail
        final Deque<Circle> trail = new ArrayDeque<>();
        double trailTimer = 0;

        Ball(double x, double y) {
            this.x = x; this.y = y;
            node = new Circle(BALL_RADIUS,
                    new RadialGradient(0, 0.2, 0.35, 0.35, 1, true, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.WHITE),
                            new Stop(0.6, Color.web("#ffefcc")),
                            new Stop(1, Color.web("#ffb347"))));
            node.setStroke(Color.color(0,0,0,0.25));
            node.setEffect(new DropShadow(12, Color.color(0,0,0,0.35)));
            setPosition(x, y);
        }

        void setPosition(double x, double y) {
            this.x = x; this.y = y;
            node.setTranslateX(x);
            node.setTranslateY(y);
        }

        void updatePhysics(double dt) {
            // Air drag
            vx *= (1 - AIR_DRAG);
            vy *= (1 - AIR_DRAG);

            // Gravity
            vy += GRAVITY * dt;

            // Integrate
            x += vx * dt;
            y += vy * dt;

            setPosition(x, y);
        }

        void updateTrail(double dt, boolean active) {
            trailTimer += dt;
            if (active && trailTimer >= 0.025) {
                trailTimer = 0;
                Circle t = new Circle(x, y, BALL_RADIUS * 0.75, Color.color(1, 0.85, 0.5, 0.5));
                t.setEffect(new GaussianBlur(6));
                fxLayer.getChildren().add(0, t);
                trail.addLast(t);
                if (trail.size() > 24) {
                    Circle old = trail.removeFirst();
                    fxLayer.getChildren().remove(old);
                }
                // Fade older
                int idx = 0;
                for (Circle c : trail) {
                    double alpha = 0.7 * (idx / (double) trail.size());
                    c.setOpacity(0.5 - alpha);
                    idx++;
                }
            } else if (!active) {
                // slowly clear trail
                if (!trail.isEmpty()) {
                    Circle old = trail.removeFirst();
                    FadeTransition ft = new FadeTransition(Duration.millis(200), old);
                    ft.setFromValue(old.getOpacity());
                    ft.setToValue(0);
                    ft.setOnFinished(e -> fxLayer.getChildren().remove(old));
                    ft.play();
                }
            }
        }
    }

    // Utils
    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static void main(String[] args) {
        launch(args);
    }
}