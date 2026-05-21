import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TennisGameApp extends Application {

    private static final double WINDOW_W = 1280;
    private static final double WINDOW_H = 720;

    // Court units (rough scale; not real meters)
    private static final double COURT_LEN   = 780;
    private static final double COURT_WIDTH = 285;
    private static final double NET_HEIGHT  = 55;
    private static final double BALL_R      = 8;
    private static final double GRAVITY     = 680;

    private static final double PLAYER_SPEED = 230;
    private static final double SWING_RANGE  = 75;
    private static final long   SWING_COOLDOWN_MS = 350;

    private Stage primaryStage;
    private boolean vsComputer = false;

    private Group worldRoot;
    private PerspectiveCamera camera;
    private SubScene gameSub;
    private Player p1, p2;
    private Ball ball;
    private Player server;
    private Player lastHitter;
    private int bounceCount = 0;

    private enum State { SERVE, PLAY, POINT }
    private State state = State.SERVE;

    private int p1Pts = 0, p2Pts = 0;
    private int p1Games = 0, p2Games = 0;

    private final Set<KeyCode> keys = new HashSet<>();
    private final Set<KeyCode> pressedEdge = new HashSet<>();
    private AnimationTimer loop;
    private final Random rng = new Random();

    private Text scoreText;
    private Text infoText;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Wimbledon Tennis 3D");
        stage.setResizable(false);
        showMenu();
        stage.show();
    }

    // ----- Menu -----

    private void showMenu() {
        StackPane root = new StackPane();
        root.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#0d4a26")),
                        new Stop(1, Color.web("#03190d"))),
                CornerRadii.EMPTY, Insets.EMPTY)));

        VBox box = new VBox(22);
        box.setAlignment(Pos.CENTER);

        Text title = new Text("WIMBLEDON TENNIS 3D");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 56));
        title.setFill(Color.WHITE);
        title.setEffect(new DropShadow(22, Color.color(0, 0, 0, 0.6)));

        Text subtitle = new Text("Would you like to play with computer or with other player?");
        subtitle.setFont(Font.font("Verdana", FontWeight.NORMAL, 20));
        subtitle.setFill(Color.web("#e1efe6"));

        Button vsCpu = menuButton("Play vs Computer");
        Button vsTwo = menuButton("Play vs Other Player");
        Button quit  = menuButton("Quit");

        vsCpu.setOnAction(e -> { vsComputer = true;  startGame(); });
        vsTwo.setOnAction(e -> { vsComputer = false; startGame(); });
        quit.setOnAction(e -> primaryStage.close());

        Text controls = new Text(
                "Controls — P1: A/D move, W/S forward/back, SPACE swing/serve\n" +
                "P2: Left/Right move, Up/Down forward/back, ENTER swing/serve\n" +
                "ESC returns to this menu");
        controls.setFont(Font.font("Verdana", 13));
        controls.setFill(Color.web("#bcd5c3"));
        controls.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        box.getChildren().addAll(title, subtitle, vsCpu, vsTwo, quit, controls);
        root.getChildren().add(box);

        Scene scene = new Scene(root, WINDOW_W, WINDOW_H);
        primaryStage.setScene(scene);
    }

    private Button menuButton(String text) {
        Button b = new Button(text);
        String base  = "-fx-font-size: 20px; -fx-padding: 12 30 12 30; -fx-text-fill: white; -fx-background-radius: 28; ";
        String idle  = base + "-fx-background-color: #1f7038;";
        String hover = base + "-fx-background-color: #2c9a4d;";
        b.setStyle(idle);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(idle));
        return b;
    }

    // ----- Game scene -----

    private void startGame() {
        keys.clear();
        pressedEdge.clear();
        p1Pts = p2Pts = p1Games = p2Games = 0;

        worldRoot = new Group();
        buildLighting();
        buildSky();
        buildCourt();
        buildPlayers();
        buildBall();
        buildStadium();

        camera = new PerspectiveCamera(true);
        camera.setFieldOfView(50);
        camera.setNearClip(0.1);
        camera.setFarClip(6000);
        camera.getTransforms().addAll(
                new Translate(0, -260, -COURT_LEN / 2 - 320),
                new Rotate(-18, Rotate.X_AXIS)
        );

        gameSub = new SubScene(worldRoot, WINDOW_W, WINDOW_H, true, SceneAntialiasing.BALANCED);
        gameSub.setFill(Color.web("#8fb6dc"));
        gameSub.setCamera(camera);

        scoreText = new Text(36, 56, "");
        scoreText.setFont(Font.font("Verdana", FontWeight.BOLD, 26));
        scoreText.setFill(Color.WHITE);
        scoreText.setEffect(new DropShadow(8, Color.color(0, 0, 0, 0.7)));

        infoText = new Text(36, 92, "");
        infoText.setFont(Font.font("Verdana", FontWeight.NORMAL, 16));
        infoText.setFill(Color.web("#fff4b5"));
        infoText.setEffect(new DropShadow(8, Color.color(0, 0, 0, 0.7)));

        Pane hud = new Pane(scoreText, infoText);
        hud.setPickOnBounds(false);

        StackPane root = new StackPane(gameSub, hud);

        Scene scene = new Scene(root, WINDOW_W, WINDOW_H);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stopLoop();
                showMenu();
                return;
            }
            if (!keys.contains(e.getCode())) pressedEdge.add(e.getCode());
            keys.add(e.getCode());
        });
        scene.setOnKeyReleased(e -> keys.remove(e.getCode()));

        primaryStage.setScene(scene);

        server = p1;
        initServe();
        startLoop();
        updateScore();
    }

    private void stopLoop() {
        if (loop != null) {
            loop.stop();
            loop = null;
        }
    }

    // ----- World construction -----

    private void buildLighting() {
        AmbientLight ambient = new AmbientLight(Color.color(0.55, 0.55, 0.55));
        PointLight sun = new PointLight(Color.color(1.0, 0.95, 0.85));
        sun.setTranslateX(0);
        sun.setTranslateY(-900);
        sun.setTranslateZ(-200);
        worldRoot.getChildren().addAll(ambient, sun);
    }

    private void buildSky() {
        // Large faintly textured sky cube far back
        Box sky = new Box(6000, 2000, 10);
        PhongMaterial m = new PhongMaterial(Color.web("#a9cfe9"));
        sky.setMaterial(m);
        sky.setTranslateY(-700);
        sky.setTranslateZ(2500);
        sky.setMouseTransparent(true);
        worldRoot.getChildren().add(sky);
    }

    private void buildCourt() {
        // Grass plane (Wimbledon style)
        Box grass = new Box(COURT_WIDTH + 320, 4, COURT_LEN + 320);
        PhongMaterial grassMat = new PhongMaterial(Color.web("#2f8a3a"));
        grassMat.setSpecularColor(Color.web("#67ab73"));
        grass.setMaterial(grassMat);
        grass.setTranslateY(2);
        worldRoot.getChildren().add(grass);

        // Mowed stripes for realism
        for (int i = -4; i <= 4; i++) {
            Box stripe = new Box(COURT_WIDTH + 40, 1, 80);
            PhongMaterial sm = new PhongMaterial(
                    i % 2 == 0 ? Color.web("#3a9844") : Color.web("#2b7d35"));
            stripe.setMaterial(sm);
            stripe.setTranslateY(0.5);
            stripe.setTranslateZ(i * 80);
            worldRoot.getChildren().add(stripe);
        }

        // White lines
        addLine(0, -COURT_LEN / 2, COURT_WIDTH, 5);   // far baseline
        addLine(0,  COURT_LEN / 2, COURT_WIDTH, 5);   // near baseline
        addLine(-COURT_WIDTH / 2, 0, 5, COURT_LEN);   // left singles
        addLine( COURT_WIDTH / 2, 0, 5, COURT_LEN);   // right singles
        addLine(0, -COURT_LEN / 4, COURT_WIDTH, 4);   // service line far
        addLine(0,  COURT_LEN / 4, COURT_WIDTH, 4);   // service line near
        addLine(0, 0, 4, COURT_LEN / 2);              // center service line

        // Net
        Box netCloth = new Box(COURT_WIDTH + 60, NET_HEIGHT, 3);
        PhongMaterial netMat = new PhongMaterial(Color.color(0.95, 0.95, 0.95, 0.85));
        netMat.setSpecularColor(Color.WHITE);
        netCloth.setMaterial(netMat);
        netCloth.setTranslateY(-NET_HEIGHT / 2);
        worldRoot.getChildren().add(netCloth);

        Box netBand = new Box(COURT_WIDTH + 60, 6, 6);
        netBand.setMaterial(new PhongMaterial(Color.WHITE));
        netBand.setTranslateY(-NET_HEIGHT - 1);
        worldRoot.getChildren().add(netBand);

        for (double x : new double[]{-(COURT_WIDTH / 2 + 30), (COURT_WIDTH / 2 + 30)}) {
            Cylinder post = new Cylinder(4, NET_HEIGHT + 16);
            post.setMaterial(new PhongMaterial(Color.web("#c9c9c9")));
            post.setTranslateX(x);
            post.setTranslateY(-(NET_HEIGHT + 16) / 2);
            worldRoot.getChildren().add(post);
        }
    }

    private void addLine(double x, double z, double w, double l) {
        Box line = new Box(w, 1, l);
        line.setMaterial(new PhongMaterial(Color.WHITE));
        line.setTranslateX(x);
        line.setTranslateY(-0.5);
        line.setTranslateZ(z);
        worldRoot.getChildren().add(line);
    }

    private void buildPlayers() {
        p1 = new Player(0, -COURT_LEN / 2 + 70, Color.web("#1259bd"), true);
        p2 = new Player(0,  COURT_LEN / 2 - 70, Color.web("#bd2424"), false);
        worldRoot.getChildren().addAll(p1.root, p2.root);
    }

    private void buildBall() {
        ball = new Ball();
        worldRoot.getChildren().add(ball.node);
    }

    private void buildStadium() {
        // Wimbledon flower-box ring on far sides
        for (int i = -10; i <= 10; i++) {
            Box planter = new Box(60, 18, 16);
            PhongMaterial m = new PhongMaterial(Color.web("#4f2716"));
            planter.setMaterial(m);
            planter.setTranslateY(-9);
            planter.setTranslateX(i * 80);
            planter.setTranslateZ(-COURT_LEN / 2 - 200);
            worldRoot.getChildren().add(planter);

            Box planter2 = new Box(60, 18, 16);
            planter2.setMaterial(m);
            planter2.setTranslateY(-9);
            planter2.setTranslateX(i * 80);
            planter2.setTranslateZ(COURT_LEN / 2 + 200);
            worldRoot.getChildren().add(planter2);
        }

        // Crowd stands (giant boxes around the court, deep purple/green)
        addStand(0, -COURT_LEN / 2 - 380, COURT_WIDTH + 1200, 320, 150);
        addStand(0,  COURT_LEN / 2 + 380, COURT_WIDTH + 1200, 320, 150);
        addStand(-(COURT_WIDTH / 2 + 380), 0, 150, COURT_LEN + 600, 320);
        addStand( (COURT_WIDTH / 2 + 380), 0, 150, COURT_LEN + 600, 320);
    }

    private void addStand(double x, double z, double w, double l, double h) {
        Box b = new Box(w, h, l);
        PhongMaterial m = new PhongMaterial(Color.web("#2b1d4a"));
        m.setSpecularColor(Color.web("#503a82"));
        b.setMaterial(m);
        b.setTranslateX(x);
        b.setTranslateY(-h / 2);
        b.setTranslateZ(z);
        worldRoot.getChildren().add(b);

        // Yellow trim at top
        Box trim = new Box(w + 2, 6, l + 2);
        trim.setMaterial(new PhongMaterial(Color.web("#e9c83b")));
        trim.setTranslateX(x);
        trim.setTranslateY(-h - 3);
        trim.setTranslateZ(z);
        worldRoot.getChildren().add(trim);
    }

    // ----- Loop -----

    private void startLoop() {
        loop = new AnimationTimer() {
            long last = 0;
            @Override public void handle(long now) {
                if (last == 0) { last = now; return; }
                double dt = Math.min((now - last) / 1e9, 1.0 / 30.0);
                last = now;
                step(dt);
                pressedEdge.clear();
            }
        };
        loop.start();
    }

    private void step(double dt) {
        handleInput(dt);
        if (vsComputer) updateAI(dt);

        if (state == State.PLAY) {
            ball.update(dt);
            handleCollisions();
        }
    }

    private void handleInput(double dt) {
        double dx1 = 0, dz1 = 0;
        if (keys.contains(KeyCode.A)) dx1 -= 1;
        if (keys.contains(KeyCode.D)) dx1 += 1;
        if (keys.contains(KeyCode.W)) dz1 += 1; // P1 forward toward net (+z toward 0)
        if (keys.contains(KeyCode.S)) dz1 -= 1;
        normalizeAndMove(p1, dx1, dz1, dt);

        if (pressedEdge.contains(KeyCode.SPACE)) {
            if (state == State.SERVE && server == p1) doServe(p1);
            else if (state == State.PLAY) p1.trySwing(ball);
        }

        if (!vsComputer) {
            double dx2 = 0, dz2 = 0;
            if (keys.contains(KeyCode.LEFT))  dx2 -= 1;
            if (keys.contains(KeyCode.RIGHT)) dx2 += 1;
            if (keys.contains(KeyCode.UP))    dz2 -= 1; // P2 forward toward net (-z toward 0)
            if (keys.contains(KeyCode.DOWN))  dz2 += 1;
            normalizeAndMove(p2, dx2, dz2, dt);

            if (pressedEdge.contains(KeyCode.ENTER)) {
                if (state == State.SERVE && server == p2) doServe(p2);
                else if (state == State.PLAY) p2.trySwing(ball);
            }
        }
    }

    private void normalizeAndMove(Player p, double dx, double dz, double dt) {
        double m = Math.hypot(dx, dz);
        if (m > 0) {
            dx /= m; dz /= m;
            p.move(dx * PLAYER_SPEED * dt, dz * PLAYER_SPEED * dt);
        }
    }

    private void updateAI(double dt) {
        if (state == State.SERVE && server == p2) {
            if (rng.nextDouble() < 0.025) doServe(p2);
            return;
        }
        if (state != State.PLAY) return;

        double targetX = ball.x;
        double targetZ = COURT_LEN / 2 - 90;
        if (ball.vz > 0) {
            double t = (p2.z - ball.z) / Math.max(40, ball.vz);
            if (t > 0 && t < 3) {
                targetX = ball.x + ball.vx * t;
                targetX = Math.max(-COURT_WIDTH / 2 - 20, Math.min(COURT_WIDTH / 2 + 20, targetX));
            }
        }
        double dx = targetX - p2.x;
        double dz = targetZ - p2.z;
        double m = Math.hypot(dx, dz);
        if (m > 5) {
            double f = PLAYER_SPEED * 0.95 * dt / m;
            p2.move(dx * f, dz * f);
        }

        // Swing when ball close
        double bd = Math.hypot(ball.x - p2.x, ball.z - p2.z);
        if (bd < SWING_RANGE - 10 && ball.y > -160 && ball.y < 20) {
            p2.trySwing(ball);
        }
    }

    private void doServe(Player who) {
        double dir = (who == p1) ? 1 : -1;
        ball.x = who.x;
        ball.y = -130;
        ball.z = who.z + dir * 15;
        ball.vx = (rng.nextDouble() - 0.5) * 80;
        ball.vy = -90;
        ball.vz = dir * (470 + rng.nextDouble() * 80);
        state = State.PLAY;
        lastHitter = who;
        bounceCount = 0;
        infoText.setText("");
    }

    private void handleCollisions() {
        // Ground bounce
        if (ball.y + BALL_R >= 0 && ball.vy > 0) {
            ball.y = -BALL_R - 0.1;
            ball.vy = -ball.vy * 0.62;
            ball.vx *= 0.94;
            ball.vz *= 0.94;
            bounceCount++;

            // First bounce: check inside court bounds
            if (bounceCount == 1) {
                if (Math.abs(ball.x) > COURT_WIDTH / 2 + 6
                        || Math.abs(ball.z) > COURT_LEN / 2 + 6) {
                    awardPoint(other(lastHitter), "Out");
                    return;
                }
            } else if (bounceCount >= 2) {
                awardPoint(other(lastHitter), "Double bounce");
                return;
            }
        }

        // Net collision (very rough)
        if (Math.abs(ball.z) < BALL_R + 3 && ball.y > -NET_HEIGHT) {
            double side = ball.vz >= 0 ? 1 : -1;
            ball.z = side * (BALL_R + 4);
            ball.vz = -ball.vz * 0.35;
            ball.vx *= 0.7;
        }

        // Way out of bounds
        if (Math.abs(ball.z) > COURT_LEN / 2 + 200
                || Math.abs(ball.x) > COURT_WIDTH / 2 + 200) {
            awardPoint(other(lastHitter), "Out");
        }
    }

    private Player other(Player p) { return p == p1 ? p2 : p1; }

    private void awardPoint(Player to, String reason) {
        if (state != State.PLAY) return;
        state = State.POINT;
        if (to == p1) p1Pts++; else p2Pts++;

        if (p1Pts >= 4 && p1Pts - p2Pts >= 2) { p1Games++; p1Pts = 0; p2Pts = 0; }
        else if (p2Pts >= 4 && p2Pts - p1Pts >= 2) { p2Games++; p1Pts = 0; p2Pts = 0; }

        server = (server == p1) ? p2 : p1;

        infoText.setText("Point: " + nameOf(to) + "  (" + reason + ")");
        updateScore();

        PauseTransition pause = new PauseTransition(Duration.seconds(1.4));
        pause.setOnFinished(e -> initServe());
        pause.play();
    }

    private String nameOf(Player p) {
        if (p == p1) return "Player 1";
        return vsComputer ? "Computer" : "Player 2";
    }

    private void initServe() {
        state = State.SERVE;
        bounceCount = 0;
        lastHitter = null;
        ball.x = server.x;
        ball.y = -120;
        ball.z = server.z + (server == p1 ? -15 : 15);
        ball.vx = ball.vy = ball.vz = 0;
        ball.apply();
        infoText.setText("Serve: " + nameOf(server) +
                (server == p1 ? "  (press SPACE)" :
                        (vsComputer ? "  (computer serving...)" : "  (press ENTER)")));
    }

    private void updateScore() {
        scoreText.setText("Score: " + pt(p1Pts) + " - " + pt(p2Pts) +
                "   Games: " + p1Games + " - " + p2Games);
    }

    private String pt(int p) {
        return switch (p) {
            case 0 -> "0";
            case 1 -> "15";
            case 2 -> "30";
            case 3 -> "40";
            default -> Integer.toString(p);
        };
    }

    // ----- Player -----

    private class Player {
        double x, z;
        final Color color;
        final boolean isP1;
        final Group root = new Group();
        Box racket;
        long lastSwingMs = 0;

        Player(double x, double z, Color color, boolean isP1) {
            this.x = x; this.z = z; this.color = color; this.isP1 = isP1;
            build();
            applyPos();
        }

        void build() {
            Cylinder torso = new Cylinder(18, 110);
            torso.setMaterial(new PhongMaterial(color));
            torso.setTranslateY(-95);

            Sphere head = new Sphere(15);
            head.setMaterial(new PhongMaterial(Color.web("#e7c39a")));
            head.setTranslateY(-165);

            Cylinder legL = new Cylinder(6, 50);
            legL.setMaterial(new PhongMaterial(color.darker()));
            legL.setTranslateX(-8);
            legL.setTranslateY(-25);

            Cylinder legR = new Cylinder(6, 50);
            legR.setMaterial(new PhongMaterial(color.darker()));
            legR.setTranslateX(8);
            legR.setTranslateY(-25);

            racket = new Box(48, 6, 9);
            racket.setMaterial(new PhongMaterial(Color.WHITE));
            racket.setTranslateX(isP1 ? 32 : -32);
            racket.setTranslateY(-125);
            racket.setTranslateZ(isP1 ? 10 : -10);

            root.getChildren().addAll(torso, head, legL, legR, racket);
        }

        void move(double dx, double dz) {
            x += dx; z += dz;
            x = Math.max(-COURT_WIDTH / 2 - 90, Math.min(COURT_WIDTH / 2 + 90, x));
            if (isP1) z = Math.max(-COURT_LEN / 2 - 50, Math.min(-30, z));
            else      z = Math.max(30, Math.min(COURT_LEN / 2 + 50, z));
            applyPos();
        }

        void applyPos() {
            root.setTranslateX(x);
            root.setTranslateZ(z);
        }

        void trySwing(Ball b) {
            long now = System.currentTimeMillis();
            if (now - lastSwingMs < SWING_COOLDOWN_MS) return;
            double dx = b.x - x;
            double dz = b.z - z;
            double dist = Math.hypot(dx, dz);
            if (dist > SWING_RANGE || b.y < -200 || b.y > 30) return;

            lastSwingMs = now;
            double dir = isP1 ? 1 : -1;
            double power = 470 + rng.nextDouble() * 140;

            b.vx = dx * 1.8 + (rng.nextDouble() - 0.5) * 60;
            b.vy = -260 - rng.nextDouble() * 60;
            b.vz = dir * power;

            racket.setMaterial(new PhongMaterial(Color.web("#ffe680")));
            PauseTransition pt = new PauseTransition(Duration.millis(110));
            pt.setOnFinished(ev -> racket.setMaterial(new PhongMaterial(Color.WHITE)));
            pt.play();

            lastHitter = this;
            bounceCount = 0;
        }
    }

    // ----- Ball -----

    private class Ball {
        double x, y, z;
        double vx, vy, vz;
        final Sphere node;

        Ball() {
            node = new Sphere(BALL_R);
            PhongMaterial m = new PhongMaterial(Color.web("#e8ef9b"));
            m.setSpecularColor(Color.WHITE);
            node.setMaterial(m);
            x = 0; y = -120; z = 0;
            apply();
        }

        void update(double dt) {
            vy += GRAVITY * dt;
            vx *= (1 - 0.04 * dt);
            vz *= (1 - 0.04 * dt);
            vy *= (1 - 0.01 * dt);
            x += vx * dt;
            y += vy * dt;
            z += vz * dt;
            apply();
        }

        void apply() {
            node.setTranslateX(x);
            node.setTranslateY(y);
            node.setTranslateZ(z);
        }
    }
}
