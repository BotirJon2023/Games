import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.*;

public class ExtremeMountainBikeRacingGame extends Application {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    enum GameMode { MENU, VS_AI, TWO_PLAYER, GAME_OVER }

    private GameMode mode = GameMode.MENU;
    private Canvas canvas;
    private GraphicsContext gc;

    // Game objects
    private Bike player1;
    private Bike player2;          // used as AI or second human
    private Track track;
    private List<Particle> particles = new ArrayList<>();
    private Camera camera;

    // Input
    private Set<KeyCode> keys = new HashSet<>();

    // Timing
    private long lastTime = 0;
    private boolean raceStarted = false;
    private double raceTime = 0;
    private boolean finished = false;
    private String winner = "";

    // UI
    private VBox menuBox;
    private Label statusLabel;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Extreme Mountain Bike Racing");
        stage.setResizable(false);

        BorderPane root = new BorderPane();
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();

        // Menu overlay
        menuBox = createMenu();
        StackPane stack = new StackPane(canvas, menuBox);
        root.setCenter(stack);

        statusLabel = new Label();
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        statusLabel.setTextFill(Color.WHITE);
        statusLabel.setEffect(new DropShadow(8, Color.BLACK));
        StackPane.setAlignment(statusLabel, Pos.TOP_CENTER);
        StackPane.setMargin(statusLabel, new Insets(20));
        stack.getChildren().add(statusLabel);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.setOnKeyPressed(e -> keys.add(e.getCode()));
        scene.setOnKeyReleased(e -> keys.remove(e.getCode()));

        stage.setScene(scene);
        stage.show();

        // Game loop
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTime == 0) lastTime = now;
                double dt = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                if (dt > 0.05) dt = 0.05; // clamp

                update(dt);
                render();
            }
        };
        timer.start();
    }

    private VBox createMenu() {
        VBox box = new VBox(25);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(0,0,0,0.75); -fx-background-radius: 20;");
        box.setPadding(new Insets(40));
        box.setMaxWidth(480);

        Label title = new Label("EXTREME MOUNTAIN BIKE");
        title.setFont(Font.font("Impact", 42));
        title.setTextFill(Color.ORANGE);
        title.setEffect(new Glow(0.6));

        Label sub = new Label("RACING");
        sub.setFont(Font.font("Impact", 36));
        sub.setTextFill(Color.CYAN);

        Button btnAI = createMenuButton("▶  Player vs Computer");
        btnAI.setOnAction(e -> startGame(GameMode.VS_AI));

        Button btn2P = createMenuButton("▶  Two Players");
        btn2P.setOnAction(e -> startGame(GameMode.TWO_PLAYER));

        Label controls = new Label(
                "Controls:\n" +
                        "Player 1  →  A / D  (move)   W  (jump)\n" +
                        "Player 2  →  ← / →  (move)   ↑  (jump)\n" +
                        "ESC       →  Menu");
        controls.setFont(Font.font("Consolas", 15));
        controls.setTextFill(Color.LIGHTGRAY);
        controls.setAlignment(Pos.CENTER);

        box.getChildren().addAll(title, sub, btnAI, btn2P, controls);
        return box;
    }

    private Button createMenuButton(String text) {
        Button b = new Button(text);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        b.setPrefWidth(340);
        b.setPrefHeight(50);
        b.setStyle(
                "-fx-background-color: linear-gradient(#ff8c00, #e65c00);" +
                        "-fx-text-fill: white; -fx-background-radius: 12;" +
                        "-fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle(
                "-fx-background-color: linear-gradient(#ffa500, #ff8c00);" +
                        "-fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle(
                "-fx-background-color: linear-gradient(#ff8c00, #e65c00);" +
                        "-fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand;"));
        return b;
    }

    private void startGame(GameMode m) {
        mode = m;
        menuBox.setVisible(false);
        raceStarted = true;
        finished = false;
        raceTime = 0;
        winner = "";
        particles.clear();

        track = new Track(8000); // long track
        player1 = new Bike(120, track.getY(120) - 25, Color.DODGERBLUE, "P1");
        if (m == GameMode.VS_AI) {
            player2 = new Bike(80, track.getY(80) - 25, Color.CRIMSON, "AI");
            player2.isAI = true;
        } else {
            player2 = new Bike(80, track.getY(80) - 25, Color.LIME, "P2");
        }
        camera = new Camera();
    }

    private void update(double dt) {
        if (mode == GameMode.MENU) return;

        if (keys.contains(KeyCode.ESCAPE)) {
            mode = GameMode.MENU;
            menuBox.setVisible(true);
            raceStarted = false;
            return;
        }

        if (!raceStarted || finished) return;

        raceTime += dt;

        // Player 1 controls
        handleInput(player1, KeyCode.A, KeyCode.D, KeyCode.W, dt);

        // Player 2 / AI
        if (mode == GameMode.TWO_PLAYER) {
            handleInput(player2, KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, dt);
        } else {
            updateAI(player2, dt);
        }

        // Physics
        updateBike(player1, dt);
        updateBike(player2, dt);

        // Camera follows the leader
        double leadX = Math.max(player1.x, player2.x);
        camera.follow(leadX, dt);

        // Particles
        updateParticles(dt);

        // Finish check
        if (!finished) {
            if (player1.x > track.length - 200) {
                finished = true;
                winner = player1.name + " WINS!";
            } else if (player2.x > track.length - 200) {
                finished = true;
                winner = player2.name + " WINS!";
            }
        }
    }

    private void handleInput(Bike b, KeyCode left, KeyCode right, KeyCode jump, double dt) {
        if (keys.contains(left))  b.vx -= 380 * dt;
        if (keys.contains(right)) b.vx += 420 * dt;
        if (keys.contains(jump) && b.onGround) {
            b.vy = -520;
            b.onGround = false;
            spawnDust(b.x, b.y + 20, 8);
        }
        // friction
        b.vx *= 0.985;
        b.vx = Math.max(-280, Math.min(520, b.vx));
    }

    private void updateAI(Bike ai, double dt) {
        // Simple but effective AI
        double lookAhead = ai.x + 180;
        double targetY = track.getY(lookAhead);
        double slope = track.getSlope(ai.x);

        // Accelerate
        if (ai.vx < 340) ai.vx += 300 * dt;

        // Jump when approaching a drop or jump ramp
        if (ai.onGround && slope < -0.35 && Math.random() < 0.04) {
            ai.vy = -480 - Math.random() * 80;
            ai.onGround = false;
            spawnDust(ai.x, ai.y + 20, 6);
        }

        // Small random correction
        if (Math.random() < 0.02) ai.vx += (Math.random() - 0.5) * 40;

        ai.vx *= 0.988;
        ai.vx = Math.max(-200, Math.min(480, ai.vx));
    }

    private void updateBike(Bike b, double dt) {
        // Gravity
        b.vy += 1450 * dt;

        // Move
        b.x += b.vx * dt;
        b.y += b.vy * dt;

        // Ground collision + slope
        double groundY = track.getY(b.x);
        double slope = track.getSlope(b.x);

        if (b.y >= groundY - 22) {
            b.y = groundY - 22;
            if (b.vy > 0) {
                // landing
                if (b.vy > 300) spawnDust(b.x, b.y + 18, 12);
                b.vy *= -0.25; // bounce
                if (Math.abs(b.vy) < 40) b.vy = 0;
            }
            b.onGround = true;

            // Align to slope
            b.targetAngle = Math.atan(slope);
        } else {
            b.onGround = false;
            b.targetAngle = b.vy * 0.0008; // slight pitch in air
        }

        // Smooth lean
        b.angle += (b.targetAngle - b.angle) * 8 * dt;

        // Wheel rotation based on speed
        b.wheelAngle += b.vx * 0.045 * dt;

        // Keep on track bounds
        if (b.x < 40) b.x = 40;
    }

    private void spawnDust(double x, double y, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x + (Math.random() - 0.5) * 20,
                    y + (Math.random() - 0.5) * 10,
                    (Math.random() - 0.5) * 120,
                    -Math.random() * 180 - 40,
                    0.4 + Math.random() * 0.5));
        }
    }

    private void updateParticles(double dt) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vy += 400 * dt;
            p.life -= dt;
            if (p.life <= 0) it.remove();
        }
    }

    private void render() {
        // Sky gradient
        gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a3a6e")),
                new Stop(0.5, Color.web("#4a90c8")),
                new Stop(1, Color.web("#87ceeb"))));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        if (mode == GameMode.MENU) {
            // nice background mountains even on menu
            drawParallaxMountains(0.3);
            return;
        }

        double camX = camera.x;

        // Parallax mountains
        drawParallaxMountains(camX * 0.15);

        // Track
        track.draw(gc, camX);

        // Finish line
        double finishScreenX = track.length - 150 - camX;
        if (finishScreenX > -50 && finishScreenX < WIDTH + 50) {
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(6);
            gc.strokeLine(finishScreenX, 0, finishScreenX, HEIGHT);
            gc.setFill(Color.YELLOW);
            gc.setFont(Font.font("Impact", 28));
            gc.fillText("FINISH", finishScreenX - 40, 80);
        }

        // Particles
        for (Particle p : particles) {
            double alpha = Math.max(0, p.life);
            gc.setFill(Color.rgb(180, 140, 80, alpha * 0.7));
            gc.fillOval(p.x - camX - 4, p.y - 4, 8, 8);
        }

        // Bikes
        drawBike(player1, camX);
        drawBike(player2, camX);

        // HUD
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        gc.fillText(String.format("Time: %.1fs", raceTime), 20, 35);
        gc.setFill(player1.color);
        gc.fillText(player1.name + "  " + (int) player1.vx + " km/h", 20, 60);
        gc.setFill(player2.color);
        gc.fillText(player2.name + "  " + (int) player2.vx + " km/h", 20, 85);

        if (finished) {
            gc.setFill(Color.rgb(0, 0, 0, 0.55));
            gc.fillRect(0, 0, WIDTH, HEIGHT);
            gc.setFill(Color.ORANGE);
            gc.setFont(Font.font("Impact", 64));
            gc.fillText(winner, WIDTH / 2.0 - 180, HEIGHT / 2.0);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 22));
            gc.fillText("Press ESC for Menu", WIDTH / 2.0 - 100, HEIGHT / 2.0 + 50);
        }
    }

    private void drawParallaxMountains(double offset) {
        // Far mountains
        gc.setFill(Color.web("#2c4a6e"));
        double[] far = {0, 420, 180, 280, 360, 350, 520, 250, 700, 380, 900, 300, 1100, 400, 1280, 350, 1280, 720, 0, 720};
        gc.fillPolygon(shift(far, offset * 0.4), yCoords(far), far.length / 2);

        // Mid mountains
        gc.setFill(Color.web("#3a5f8a"));
        double[] mid = {0, 480, 220, 320, 400, 400, 580, 290, 750, 420, 950, 340, 1150, 450, 1280, 380, 1280, 720, 0, 720};
        gc.fillPolygon(shift(mid, offset * 0.7), yCoords(mid), mid.length / 2);
    }

    private double[] shift(double[] poly, double off) {
        double[] r = new double[poly.length / 2];
        for (int i = 0; i < r.length; i++) r[i] = poly[i * 2] - (off % 400);
        return r;
    }

    private double[] yCoords(double[] poly) {
        double[] r = new double[poly.length / 2];
        for (int i = 0; i < r.length; i++) r[i] = poly[i * 2 + 1];
        return r;
    }

    private void drawBike(Bike b, double camX) {
        double sx = b.x - camX;
        double sy = b.y;

        gc.save();
        gc.translate(sx, sy);
        gc.rotate(Math.toDegrees(b.angle));

        // Shadow
        gc.setFill(Color.rgb(0, 0, 0, 0.25));
        gc.fillOval(-28, 18, 56, 12);

        // Frame
        gc.setStroke(b.color.brighter());
        gc.setLineWidth(5);
        gc.strokeLine(-18, 0, 18, 0);          // main tube
        gc.strokeLine(-18, 0, -8, -18);        // seat tube
        gc.strokeLine(18, 0, 8, -16);          // head tube
        gc.strokeLine(-8, -18, 8, -16);        // top tube

        // Wheels
        drawWheel(-18, 8, b.wheelAngle);
        drawWheel(18, 8, b.wheelAngle);

        // Rider (simple)
        gc.setFill(Color.web("#222"));
        gc.fillOval(-6, -32, 14, 14); // head
        gc.setStroke(Color.web("#333"));
        gc.setLineWidth(3);
        gc.strokeLine(0, -18, 0, -5); // body

        // Name tag
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.fillText(b.name, -12, -40);

        gc.restore();
    }

    private void drawWheel(double x, double y, double angle) {
        gc.setStroke(Color.web("#222"));
        gc.setLineWidth(4);
        gc.strokeOval(x - 11, y - 11, 22, 22);

        // spokes
        gc.setStroke(Color.web("#555"));
        gc.setLineWidth(1.5);
        for (int i = 0; i < 6; i++) {
            double a = angle + i * Math.PI / 3;
            gc.strokeLine(x, y,
                    x + Math.cos(a) * 9,
                    y + Math.sin(a) * 9);
        }

        // hub
        gc.setFill(Color.ORANGE);
        gc.fillOval(x - 3, y - 3, 6, 6);
    }

    // ==================== Helper Classes ====================

    static class Bike {
        double x, y, vx, vy;
        double angle, targetAngle, wheelAngle;
        boolean onGround = true;
        boolean isAI = false;
        Color color;
        String name;

        Bike(double x, double y, Color c, String n) {
            this.x = x; this.y = y; this.color = c; this.name = n;
        }
    }

    static class Camera {
        double x = 0;
        void follow(double targetX, double dt) {
            double desired = targetX - WIDTH * 0.35;
            x += (desired - x) * 4.5 * dt;
            if (x < 0) x = 0;
        }
    }

    static class Particle {
        double x, y, vx, vy, life;
        Particle(double x, double y, double vx, double vy, double life) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = life;
        }
    }

    static class Track {
        double length;
        private final List<double[]> segments = new ArrayList<>(); // x, y pairs

        Track(double len) {
            this.length = len;
            generate();
        }

        private void generate() {
            Random rnd = new Random(42);
            double x = 0, y = 520;
            segments.add(new double[]{x, y});

            while (x < length) {
                x += 80 + rnd.nextDouble() * 140;
                // create hills and jumps
                double dy = (rnd.nextDouble() - 0.48) * 160;
                // force some big jumps
                if (rnd.nextDouble() < 0.12) dy = -120 - rnd.nextDouble() * 90;
                y = Math.max(280, Math.min(620, y + dy));
                segments.add(new double[]{x, y});
            }
            // flat finish
            segments.add(new double[]{length + 300, y});
        }

        double getY(double px) {
            for (int i = 0; i < segments.size() - 1; i++) {
                double[] a = segments.get(i);
                double[] b = segments.get(i + 1);
                if (px >= a[0] && px <= b[0]) {
                    double t = (px - a[0]) / (b[0] - a[0]);
                    return a[1] + t * (b[1] - a[1]);
                }
            }
            return segments.get(segments.size() - 1)[1];
        }

        double getSlope(double px) {
            for (int i = 0; i < segments.size() - 1; i++) {
                double[] a = segments.get(i);
                double[] b = segments.get(i + 1);
                if (px >= a[0] && px <= b[0]) {
                    return (b[1] - a[1]) / (b[0] - a[0] + 0.001);
                }
            }
            return 0;
        }

        void draw(GraphicsContext gc, double camX) {
            // Ground fill
            gc.setFill(Color.web("#3d6b3a"));
            gc.beginPath();
            gc.moveTo(-camX, HEIGHT);
            for (double[] s : segments) {
                gc.lineTo(s[0] - camX, s[1]);
            }
            gc.lineTo(length + 400 - camX, HEIGHT);
            gc.closePath();
            gc.fill();

            // Dirt surface line
            gc.setStroke(Color.web("#5a3e1b"));
            gc.setLineWidth(7);
            gc.beginPath();
            boolean first = true;
            for (double[] s : segments) {
                double sx = s[0] - camX;
                if (first) { gc.moveTo(sx, s[1]); first = false; }
                else gc.lineTo(sx, s[1]);
            }
            gc.stroke();

            // Grass highlight
            gc.setStroke(Color.web("#6b9b4a"));
            gc.setLineWidth(3);
            gc.beginPath();
            first = true;
            for (double[] s : segments) {
                double sx = s[0] - camX;
                if (first) { gc.moveTo(sx, s[1] - 3); first = false; }
                else gc.lineTo(sx, s[1] - 3);
            }
            gc.stroke();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}