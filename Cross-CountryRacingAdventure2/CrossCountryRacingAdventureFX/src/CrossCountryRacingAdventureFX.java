import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class CrossCountryRacingAdventureFX extends Application {

    // ---------- Configuration ----------
    private static final int WIDTH = 1100;
    private static final int HEIGHT = 620;
    private static final int TRACK_LENGTH = 100;      // logical race distance
    private static final double GROUND_Y = 470;
    private static final double LANE_OFFSET = 60;

    // ---------- Game State ----------
    private enum Mode { TWO_PLAYER, VS_COMPUTER }
    private enum Move { SPRINT, STEADY, RECOVER }

    private Mode mode = Mode.TWO_PLAYER;
    private Racer racerA, racerB;
    private boolean raceRunning = false;
    private boolean turnLocked = false;
    private int turnNumber = 1;
    private String statusText = "Choose a game mode to begin!";
    private final List<Particle> particles = new ArrayList<>();
    private final List<Cloud> clouds = new ArrayList<>();
    private final List<Hill> farHills = new ArrayList<>();
    private final List<Hill> nearHills = new ArrayList<>();

    private Canvas canvas;
    private Label statusLabel;
    private AnimationTimer animTimer;
    private long startTimeNanos;

    // ---------- Startup ----------
    @Override
    public void start(Stage stage) {
        canvas = new Canvas(WIDTH, HEIGHT);

        statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-text-fill:white; -fx-font-size:16px; -fx-font-weight:bold; -fx-padding:8 20;");
        statusLabel.setBackground(new Background(new BackgroundFill(
                Color.color(0, 0, 0, 0.55), new CornerRadii(10), Insets.EMPTY)));

        HBox buttons = buildButtons();
        StackPane root = new StackPane(canvas, statusLabel, buttons);
        StackPane.setAlignment(statusLabel, Pos.TOP_CENTER);
        StackPane.setMargin(statusLabel, new Insets(12));
        StackPane.setAlignment(buttons, Pos.BOTTOM_CENTER);
        StackPane.setMargin(buttons, new Insets(0, 0, 14, 0));

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.setOnKeyPressed(this::handleKey);
        scene.setFill(Color.BLACK);

        stage.setTitle("Cross-Country Racing Adventure");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        initScenery();
        startAnimation();
    }

    // ---------- UI ----------
    private HBox buildButtons() {
        Button sprint = mkBtn("🏃 Sprint", "#e67e22");
        Button steady = mkBtn("🚶 Steady", "#27ae60");
        Button recover = mkBtn("🌿 Recover", "#2980b9");
        Button twoP = mkBtn("👥 2 Players", "#8e44ad");
        Button vsAI = mkBtn("🤖 vs Computer", "#c0392b");
        Button restart = mkBtn("↻ Restart", "#7f8c8d");

        sprint.setOnAction(e -> makeMove(Move.SPRINT));
        steady.setOnAction(e -> makeMove(Move.STEADY));
        recover.setOnAction(e -> makeMove(Move.RECOVER));
        twoP.setOnAction(e -> startGame(Mode.TWO_PLAYER));
        vsAI.setOnAction(e -> startGame(Mode.VS_COMPUTER));
        restart.setOnAction(e -> startGame(mode));

        HBox box = new HBox(10, sprint, steady, recover,
                new Separator(Orientation.VERTICAL),
                twoP, vsAI, restart);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding: 10 14; -fx-background-radius: 12;");
        box.setBackground(new Background(new BackgroundFill(
                Color.color(0, 0, 0, 0.55), new CornerRadii(12), Insets.EMPTY)));
        return box;
    }

    private Button mkBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:white;" +
                " -fx-background-color:" + color + "; -fx-background-radius:8;" +
                " -fx-padding:8 14; -fx-cursor:hand;");
        b.setOnMouseEntered(e -> b.setOpacity(0.85));
        b.setOnMouseExited(e -> b.setOpacity(1));
        return b;
    }

    // ---------- Game Flow ----------
    private void startGame(Mode m) {
        mode = m;
        racerA = new Racer("Player 1", 0, Color.web("#3498db"), false);
        if (m == Mode.TWO_PLAYER) {
            racerB = new Racer("Player 2", 0, Color.web("#e74c3c"), false);
        } else {
            racerB = new Racer("Computer", 0, Color.web("#e74c3c"), true);
        }
        turnNumber = 1;
        raceRunning = true;
        turnLocked = false;
        particles.clear();
        statusText = "🏁 Race started! " + activeRacerName() + " — choose your move.";
        statusLabel.setText(statusText);

        // If A is AI (never in current modes, but kept generic)
        if (racerA.isComputer) scheduleComputerMove(racerA);
    }

    private String activeRacerName() {
        return turnNumber % 2 == 1 ? racerA.name : racerB.name;
    }

    private Racer activeRacer() {
        return turnNumber % 2 == 1 ? racerA : racerB;
    }

    private void handleKey(javafx.scene.input.KeyEvent e) {
        if (!raceRunning || turnLocked) return;
        if (e.getCode() == KeyCode.DIGIT1 || e.getCode() == KeyCode.NUMPAD1) makeMove(Move.SPRINT);
        else if (e.getCode() == KeyCode.DIGIT2 || e.getCode() == KeyCode.NUMPAD2) makeMove(Move.STEADY);
        else if (e.getCode() == KeyCode.DIGIT3 || e.getCode() == KeyCode.NUMPAD3) makeMove(Move.RECOVER);
    }

    private void makeMove(Move m) {
        if (!raceRunning || turnLocked) return;
        Racer r = activeRacer();
        if (r.isComputer) return;
        performMove(r, m);
    }

    private void performMove(Racer r, Move m) {
        turnLocked = true;
        int gain = computeGain(r, m);
        String msg = r.name + " chose " + m.name().toLowerCase() + " → +" + gain + " units";
        statusText = msg;
        statusLabel.setText(msg);
        animateRacer(r, gain, () -> {
            if (r.position >= TRACK_LENGTH) {
                endRace(r);
                return;
            }
            turnNumber++;
            Racer next = activeRacer();
            if (next.isComputer) {
                turnLocked = true;
                statusText = next.name + " is thinking...";
                statusLabel.setText(statusText);
                scheduleComputerMove(next);
            } else {
                turnLocked = false;
                statusText = "🎯 " + next.name + "'s turn — Sprint / Steady / Recover?";
                statusLabel.setText(statusText);
            }
        });
    }

    private void scheduleComputerMove(Racer ai) {
        PauseTransition pause = new PauseTransition(Duration.seconds(0.9));
        pause.setOnFinished(e -> {
            Move choice = chooseAIMove(ai);
            performMove(ai, choice);
        });
        pause.play();
    }

    private Move chooseAIMove(Racer ai) {
        int remaining = TRACK_LENGTH - ai.position;
        int d = new Random().nextInt(100);
        if (remaining <= 15) {
            if (d < 65) return Move.SPRINT;
            if (d < 90) return Move.STEADY;
            return Move.RECOVER;
        } else {
            if (d < 35) return Move.SPRINT;
            if (d < 78) return Move.STEADY;
            return Move.RECOVER;
        }
    }

    private int computeGain(Racer r, Move m) {
        Random rnd = new Random();
        switch (m) {
            case SPRINT:
                if (rnd.nextInt(100) < 25) { spawnDust(r, 6); return 1; }
                else { spawnDust(r, 14); return 4 + rnd.nextInt(4); }
            case STEADY:
                spawnDust(r, 6);
                return 2 + rnd.nextInt(3);
            case RECOVER:
                if (rnd.nextInt(100) < 15) { return 0; }
                return 1 + rnd.nextInt(3);
            default: return 0;
        }
    }

    private void spawnDust(Racer r, int count) {
        double x = r.visualX + 12;
        double y = laneY(r) + 20;
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y,
                    (Math.random() - 0.5) * 100,
                    -40 - Math.random() * 60,
                    0.6 + Math.random() * 0.4,
                    Color.web("#d9c8a9", 0.7)));
        }
    }

    private void animateRacer(Racer r, int gain, Runnable onDone) {
        double targetX = xForPosition(r.position + gain);
        double startX = r.visualX;
        r.position = Math.min(TRACK_LENGTH, r.position + gain);

        Timeline tl = new Timeline();
        int steps = 30;
        for (int i = 1; i <= steps; i++) {
            final double t = i / (double) steps;
            final double nx = startX + (targetX - startX) * t;
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(400 * t), e -> r.visualX = nx));
        }
        tl.setOnFinished(e -> onDone.run());
        tl.play();
    }

    private void endRace(Racer winner) {
        raceRunning = false;
        turnLocked = true;
        statusText = "🏆 " + winner.name + " WINS THE RACE!";
        statusLabel.setText(statusText);
        spawnVictoryFireworks(winner);
    }

    private void spawnVictoryFireworks(Racer r) {
        double x = r.visualX;
        double y = laneY(r) - 40;
        Color[] cols = {Color.GOLD, Color.ORANGE, Color.web("#ff5577"), Color.web("#66ccff")};
        for (int i = 0; i < 60; i++) {
            double ang = Math.random() * Math.PI * 2;
            double spd = 80 + Math.random() * 200;
            particles.add(new Particle(x, y,
                    Math.cos(ang) * spd, Math.sin(ang) * spd,
                    1.2 + Math.random() * 0.6,
                    cols[i % cols.length]));
        }
    }

    // ---------- Rendering ----------
    private void initScenery() {
        Random rnd = new Random(42);
        for (int i = 0; i < 6; i++) {
            clouds.add(new Cloud(rnd.nextDouble() * WIDTH, 40 + rnd.nextDouble() * 100,
                    0.2 + rnd.nextDouble() * 0.4, 1 + rnd.nextDouble() * 1.2));
        }
        for (int i = 0; i < 8; i++) {
            farHills.add(new Hill(i * 220 + rnd.nextDouble() * 80,
                    200 + rnd.nextDouble() * 40, 240 + rnd.nextDouble() * 160, 0.15));
        }
        for (int i = 0; i < 10; i++) {
            nearHills.add(new Hill(i * 180 + rnd.nextDouble() * 60,
                    300 + rnd.nextDouble() * 40, 140 + rnd.nextDouble() * 100, 0.3));
        }
    }

    private void startAnimation() {
        startTimeNanos = System.nanoTime();
        animTimer = new AnimationTimer() {
            long last = 0;
            @Override public void handle(long now) {
                if (last == 0) { last = now; return; }
                double dt = (now - last) / 1_000_000_000.0;
                last = now;
                update(dt, now / 1_000_000_000.0);
                render();
            }
        };
        animTimer.start();
    }

    private void update(double dt, double time) {
        // clouds drift
        for (Cloud c : clouds) {
            c.x += c.speed * 12 * dt;
            if (c.x > WIDTH + 80) c.x = -80;
        }
        // particles
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vy += 180 * dt; // gravity
            p.life -= dt;
            if (p.life <= 0) it.remove();
        }
        // animate racers' limbs
        if (racerA != null) animateLimb(racerA, dt, raceRunning);
        if (racerB != null) animateLimb(racerB, dt, raceRunning);
    }

    private void animateLimb(Racer r, double dt, boolean running) {
        if (!running) { r.limbPhase = 0; return; }
        r.limbPhase += dt * 10;
    }

    private void render() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        // Sky gradient (time-of-day bounce)
        double hueShift = Math.sin(System.nanoTime() / 4e9) * 0.02;
        LinearGradient sky = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.30 + hueShift, 0.55, 0.92)),
                new Stop(0.5, Color.color(0.62, 0.78, 0.98)),
                new Stop(1, Color.color(0.98, 0.85, 0.70)));
        g.setFill(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Sun with soft glow
        g.setFill(Color.color(1, 0.95, 0.6, 0.35));
        g.fillOval(WIDTH - 200, 60, 160, 160);
        RadialGradient sun = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(0.4, Color.web("#fff2a8")),
                new Stop(1, Color.web("#ffcc55", 0)));
        g.setFill(sun);
        g.fillOval(WIDTH - 180, 80, 120, 120);

        // Clouds
        for (Cloud c : clouds) drawCloud(g, c);

        // Far hills (parallax)
        for (Hill h : farHills) {
            g.setFill(Color.web("#6b9c5c", 0.85));
            g.fillOval(h.x, h.y, h.w, h.w * 0.6);
        }
        // Near hills
        for (Hill h : nearHills) {
            g.setFill(Color.web("#4d7a3d"));
            g.fillOval(h.x, h.y, h.w, h.w * 0.7);
        }

        // Ground (dirt + grass)
        LinearGradient ground = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#5b8d3d")),
                new Stop(0.15, Color.web("#7a5a34")),
                new Stop(1, Color.web("#4a3218")));
        g.setFill(ground);
        g.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        // Track stripes
        g.setStroke(Color.web("#8a6a44", 0.6));
        g.setLineWidth(2);
        for (int x = 0; x < WIDTH; x += 40) {
            g.strokeLine(x, GROUND_Y + 40, x + 20, GROUND_Y + 40);
        }

        // Lanes
        g.setStroke(Color.web("#000000", 0.15));
        g.setLineWidth(2);
        g.strokeLine(0, GROUND_Y + LANE_OFFSET / 2, WIDTH, GROUND_Y + LANE_OFFSET / 2);

        // Finish line
        drawFinishLine(g);

        // Racers
        if (racerA != null) drawRacer(g, racerA, 1);
        if (racerB != null) drawRacer(g, racerB, 2);

        // Particles on top
        for (Particle p : particles) {
            g.setFill(Color.color(p.color.getRed(), p.color.getGreen(),
                    p.color.getBlue(), Math.max(0, Math.min(1, p.life))));
            g.fillOval(p.x, p.y, 4, 4);
        }

        // Progress HUD
        drawHUD(g);

        // Title if not started
        if (racerA == null) {
            g.setFill(Color.web("#000", 0.55));
            g.fillRoundRect(WIDTH / 2 - 260, HEIGHT / 2 - 60, 520, 120, 20, 20);
            g.setFill(Color.WHITE);
            g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("Cross-Country Racing Adventure", WIDTH / 2, HEIGHT / 2 - 5);
            g.setFont(Font.font("Segoe UI", 16));
            g.fillText("Pick a mode below to start racing!", WIDTH / 2, HEIGHT / 2 + 30);
        }
    }

    private void drawCloud(GraphicsContext g, Cloud c) {
        g.setFill(Color.color(1, 1, 1, 0.85));
        double s = c.scale;
        g.fillOval(c.x, c.y, 60 * s, 30 * s);
        g.fillOval(c.x + 20 * s, c.y - 12 * s, 50 * s, 34 * s);
        g.fillOval(c.x + 40 * s, c.y, 55 * s, 30 * s);
    }

    private void drawFinishLine(GraphicsContext g) {
        double fx = xForPosition(TRACK_LENGTH);
        int sq = 12;
        for (int y = (int) GROUND_Y - 80; y < GROUND_Y + LANE_OFFSET * 2 + 30; y += sq) {
            for (int row = 0; row < 2; row++) {
                boolean white = ((y / sq) + row) % 2 == 0;
                g.setFill(white ? Color.WHITE : Color.BLACK);
                g.fillRect(fx + row * sq, y, sq, sq);
            }
        }
        // flag
        g.setStroke(Color.web("#222"));
        g.setLineWidth(3);
        g.strokeLine(fx + sq, GROUND_Y - 80, fx + sq, GROUND_Y - 130);
        g.setFill(Color.web("#e74c3c"));
        g.fillPolygon(new double[]{fx + sq, fx + sq + 50, fx + sq},
                new double[]{GROUND_Y - 130, GROUND_Y - 118, GROUND_Y - 106}, 3);
    }

    private void drawHUD(GraphicsContext g) {
        if (racerA == null) return;
        drawHUDEntry(g, racerA, 20, 70);
        drawHUDEntry(g, racerB, 20, 130);
    }

    private void drawHUDEntry(GraphicsContext g, Racer r, double x, double y) {
        g.setFill(Color.web("#000", 0.5));
        g.fillRoundRect(x, y, 300, 50, 12, 12);
        g.setFill(r.color);
        g.fillOval(x + 10, y + 12, 24, 24);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText(r.name, x + 44, y + 22);

        // progress bar
        double barX = x + 44, barY = y + 30, barW = 240, barH = 12;
        g.setFill(Color.web("#000", 0.4));
        g.fillRoundRect(barX, barY, barW, barH, 6, 6);
        double frac = r.position / (double) TRACK_LENGTH;
        g.setFill(r.color);
        g.fillRoundRect(barX, barY, barW * frac, barH, 6, 6);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Segoe UI", 11));
        g.fillText(Math.round(frac * 100) + "%", barX + barW + 8, barY + 11);
    }

    private void drawRacer(GraphicsContext g, Racer r, int lane) {
        double x = r.visualX;
        double y = laneY(r);

        // shadow
        g.setFill(Color.color(0, 0, 0, 0.25));
        g.fillOval(x - 6, y + 22, 40, 10);

        // body
        g.setFill(r.color);
        g.fillRoundRect(x - 6, y - 20, 24, 30, 10, 10);

        // head
        g.setFill(Color.web("#f2c79b"));
        g.fillOval(x + 2, y - 34, 16, 16);

        // cap / hair
        g.setFill(r.color.darker());
        g.fillOval(x + 2, y - 38, 16, 10);

        // running limbs
        double phase = r.limbPhase;
        double legSwing = Math.sin(phase) * 8;
        g.setStroke(r.color.darker());
        g.setLineWidth(4);
        g.setLineCap(StrokeLineCap.ROUND);
        // legs
        g.strokeLine(x + 6, y + 10, x + 6 + legSwing, y + 24);
        g.strokeLine(x + 6, y + 10, x + 6 - legSwing, y + 24);
        // arms
        double armSwing = Math.sin(phase + Math.PI) * 7;
        g.strokeLine(x + 4, y - 12, x + 4 + armSwing, y - 2);
        g.strokeLine(x + 4, y - 12, x + 4 - armSwing, y - 2);

        // name tag
        g.setFill(Color.web("#000", 0.55));
        g.fillRoundRect(x - 10, y - 66, 60, 20, 8, 8);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(r.name.length() > 9 ? r.name.substring(0, 9) : r.name, x + 20, y - 52);
    }

    private double laneY(Racer r) {
        return r == racerA ? GROUND_Y + 30 : GROUND_Y + LANE_OFFSET + 30;
    }

    private double xForPosition(int pos) {
        double trackLeft = 60;
        double trackRight = WIDTH - 120;
        double frac = pos / (double) TRACK_LENGTH;
        return trackLeft + (trackRight - trackLeft) * frac;
    }

    // ---------- Data Classes ----------
    private static class Racer {
        String name;
        int position;
        double visualX;
        Color color;
        boolean isComputer;
        double limbPhase = 0;

        Racer(String name, int pos, Color color, boolean ai) {
            this.name = name;
            this.position = pos;
            this.color = color;
            this.isComputer = ai;
            this.visualX = 60;
        }
    }

    private static class Particle {
        double x, y, vx, vy, life;
        Color color;
        Particle(double x, double y, double vx, double vy, double life, Color color) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.life = life; this.color = color;
        }
    }

    private static class Cloud {
        double x, y, speed, scale;
        Cloud(double x, double y, double speed, double scale) {
            this.x = x; this.y = y; this.speed = speed; this.scale = scale;
        }
    }

    private static class Hill {
        double x, y, w, speed;
        Hill(double x, double y, double w, double speed) {
            this.x = x; this.y = y; this.w = w; this.speed = speed;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}