import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class MinionRacingAdventure extends Application {

    // ---------- Config ----------
    private static final int W = 1200;
    private static final int H = 680;
    private static final int TRACK_LENGTH = 100;
    private static final double GROUND_Y = 480;
    private static final double LANE_GAP = 130;

    // ---------- State ----------
    private enum Mode { TWO_PLAYER, VS_COMPUTER }
    private enum Move { SPRINT, STEADY, RECOVER }

    private Mode mode = Mode.TWO_PLAYER;
    private Minion minionA, minionB;
    private boolean raceRunning = false;
    private boolean turnLocked = false;
    private int turn = 1;
    private String statusText = "Choose a mode to start!";
    private final List<Particle> particles = new ArrayList<>();
    private final List<Cloud> clouds = new ArrayList<>();
    private final List<Hill> farHills = new ArrayList<>();
    private final List<Hill> nearHills = new ArrayList<>();
    private final List<GrassTuft> grass = new ArrayList<>();
    private final List<Bird> birds = new ArrayList<>();

    private Canvas canvas;
    private Label statusLabel;
    private double timeSec = 0;

    // ---------- Launch ----------
    @Override
    public void start(Stage stage) {
        canvas = new Canvas(W, H);

        statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-text-fill:white; -fx-font-size:16px; -fx-font-weight:bold; -fx-padding:8 20;");
        statusLabel.setBackground(new Background(new BackgroundFill(
                Color.color(0.1, 0.1, 0.15, 0.6), new CornerRadii(10), Insets.EMPTY)));

        HBox buttons = buildButtons();
        StackPane root = new StackPane(canvas, statusLabel, buttons);
        StackPane.setAlignment(statusLabel, Pos.TOP_CENTER);
        StackPane.setMargin(statusLabel, new Insets(14));
        StackPane.setAlignment(buttons, Pos.BOTTOM_CENTER);
        StackPane.setMargin(buttons, new Insets(0, 0, 16, 0));

        Scene scene = new Scene(root, W, H);
        scene.setOnKeyPressed(this::handleKey);

        stage.setTitle("🍌 Minion Cross-Country Racing Adventure");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        initScenery();
        AnimationTimer timer = new AnimationTimer() {
            long last = 0;
            @Override public void handle(long now) {
                if (last == 0) { last = now; return; }
                double dt = (now - last) / 1e9;
                last = now;
                timeSec += dt;
                update(dt);
                render();
            }
        };
        timer.start();
    }

    // ---------- UI ----------
    private HBox buildButtons() {
        Button sprint = mk("🏃 Sprint", "#f39c12");
        Button steady = mk("🚶 Steady", "#27ae60");
        Button recover = mk("🌿 Recover", "#2980b9");
        Button twoP = mk("👥 2 Players", "#9b59b6");
        Button vsAI = mk("🤖 vs Computer", "#c0392b");
        Button restart = mk("↻ Restart", "#7f8c8d");

        sprint.setOnAction(e -> humanMove(Move.SPRINT));
        steady.setOnAction(e -> humanMove(Move.STEADY));
        recover.setOnAction(e -> humanMove(Move.RECOVER));
        twoP.setOnAction(e -> startGame(Mode.TWO_PLAYER));
        vsAI.setOnAction(e -> startGame(Mode.VS_COMPUTER));
        restart.setOnAction(e -> startGame(mode));

        HBox box = new HBox(10, sprint, steady, recover,
                new Separator(Orientation.VERTICAL), twoP, vsAI, restart);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 16, 10, 16));
        box.setBackground(new Background(new BackgroundFill(
                Color.color(0.1, 0.1, 0.15, 0.6), new CornerRadii(12), Insets.EMPTY)));
        return box;
    }

    private Button mk(String text, String c) {
        Button b = new Button(text);
        b.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:white;" +
                " -fx-background-color:" + c + "; -fx-background-radius:8; -fx-padding:8 14; -fx-cursor:hand;");
        b.setOnMouseEntered(e -> b.setOpacity(0.85));
        b.setOnMouseExited(e -> b.setOpacity(1));
        return b;
    }

    // ---------- Flow ----------
    private void startGame(Mode m) {
        mode = m;
        minionA = new Minion("Player 1", 0, Color.web("#f1c40f"), Color.web("#e74c3c"), false);
        if (m == Mode.TWO_PLAYER)
            minionB = new Minion("Player 2", 0, Color.web("#f1c40f"), Color.web("#3498db"), false);
        else
            minionB = new Minion("Computer", 0, Color.web("#f1c40f"), Color.web("#8e44ad"), true);

        minionA.visualX = 80;
        minionB.visualX = 80;
        turn = 1;
        raceRunning = true;
        turnLocked = false;
        particles.clear();
        statusText = "🏁 " + active().name + "'s turn — choose your move!";
        statusLabel.setText(statusText);
    }

    private Minion active() { return turn % 2 == 1 ? minionA : minionB; }

    private void handleKey(javafx.scene.input.KeyEvent e) {
        if (!raceRunning || turnLocked) return;
        if (e.getCode() == KeyCode.DIGIT1 || e.getCode() == KeyCode.NUMPAD1) humanMove(Move.SPRINT);
        else if (e.getCode() == KeyCode.DIGIT2 || e.getCode() == KeyCode.NUMPAD2) humanMove(Move.STEADY);
        else if (e.getCode() == KeyCode.DIGIT3 || e.getCode() == KeyCode.NUMPAD3) humanMove(Move.RECOVER);
    }

    private void humanMove(Move m) {
        if (!raceRunning || turnLocked) return;
        Minion cur = active();
        if (cur.isComputer) return;
        performMove(cur, m);
    }

    private void performMove(Minion m, Move mv) {
        turnLocked = true;
        m.currentMove = mv;
        int gain = computeGain(m, mv);
        statusText = m.name + " → " + mv.name().toLowerCase() + " (+" + gain + ")";
        statusLabel.setText(statusText);

        animateMinion(m, gain, () -> {
            if (m.position >= TRACK_LENGTH) { endRace(m); return; }
            turn++;
            Minion next = active();
            if (next.isComputer) {
                statusText = next.name + " is thinking...";
                statusLabel.setText(statusText);
                PauseTransition p = new PauseTransition(Duration.millis(900));
                p.setOnFinished(e -> performMove(next, aiChoose(next)));
                p.play();
            } else {
                turnLocked = false;
                statusText = "🎯 " + next.name + "'s turn";
                statusLabel.setText(statusText);
            }
        });
    }

    private Move aiChoose(Minion ai) {
        int left = TRACK_LENGTH - ai.position;
        int r = new Random().nextInt(100);
        if (left <= 15) {
            if (r < 65) return Move.SPRINT;
            if (r < 90) return Move.STEADY;
            return Move.RECOVER;
        }
        if (r < 35) return Move.SPRINT;
        if (r < 78) return Move.STEADY;
        return Move.RECOVER;
    }

    private int computeGain(Minion m, Move mv) {
        Random r = new Random();
        switch (mv) {
            case SPRINT:
                if (r.nextInt(100) < 25) { spawnDust(m, 5); return 1; }
                spawnDust(m, 12); return 4 + r.nextInt(4);
            case STEADY:
                spawnDust(m, 5); return 2 + r.nextInt(3);
            case RECOVER:
                if (r.nextInt(100) < 15) return 0;
                return 1 + r.nextInt(3);
            default: return 0;
        }
    }

    private void spawnDust(Minion m, int n) {
        double x = m.visualX - 10;
        double y = laneY(m) + 30;
        for (int i = 0; i < n; i++) {
            particles.add(new Particle(x, y,
                    (Math.random() - 0.5) * 90,
                    -50 - Math.random() * 70,
                    0.7 + Math.random() * 0.5,
                    Color.web("#c9b48a", 0.75), 3 + Math.random() * 3));
        }
    }

    private void animateMinion(Minion m, int gain, Runnable done) {
        double startX = m.visualX;
        double endX = xForPos(Math.min(TRACK_LENGTH, m.position + gain));
        m.position = Math.min(TRACK_LENGTH, m.position + gain);
        m.runCycle = 0;

        Timeline tl = new Timeline();
        int steps = 34;
        for (int i = 1; i <= steps; i++) {
            final double t = i / (double) steps;
            tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis(420 * t),
                    e -> {
                        m.visualX = startX + (endX - startX) * t;
                        m.runCycle = 1; // running during move
                    }));
        }
        tl.setOnFinished(e -> { m.runCycle = 0; done.run(); });
        tl.play();
    }

    private void endRace(Minion w) {
        raceRunning = false;
        turnLocked = true;
        statusText = "🏆 " + w.name + " WINS! 🍌";
        statusLabel.setText(statusText);
        // victory fireworks
        double x = w.visualX, y = laneY(w) - 60;
        Color[] cols = {Color.GOLD, Color.ORANGE, Color.web("#ff5577"), Color.web("#66ccff"), Color.web("#a5ff77")};
        for (int i = 0; i < 80; i++) {
            double a = Math.random() * Math.PI * 2, s = 100 + Math.random() * 240;
            particles.add(new Particle(x, y, Math.cos(a) * s, Math.sin(a) * s,
                    1.3 + Math.random() * 0.6, cols[i % cols.length], 4));
        }
    }

    // ---------- Scene ----------
    private void initScenery() {
        Random r = new Random(7);
        for (int i = 0; i < 7; i++)
            clouds.add(new Cloud(r.nextDouble() * W, 30 + r.nextDouble() * 110,
                    0.15 + r.nextDouble() * 0.35, 0.8 + r.nextDouble() * 1.2));
        for (int i = 0; i < 8; i++)
            farHills.add(new Hill(i * 260 + r.nextDouble() * 100,
                    260 + r.nextDouble() * 30, 320 + r.nextDouble() * 180));
        for (int i = 0; i < 10; i++)
            nearHills.add(new Hill(i * 200 + r.nextDouble() * 80,
                    340 + r.nextDouble() * 30, 180 + r.nextDouble() * 100));
        for (int i = 0; i < 120; i++)
            grass.add(new GrassTuft(60 + r.nextDouble() * (W - 160),
                    GROUND_Y + 30 + r.nextDouble() * 150,
                    6 + r.nextDouble() * 10));
        for (int i = 0; i < 3; i++)
            birds.add(new Bird(r.nextDouble() * W, 120 + r.nextDouble() * 60, 0.6 + r.nextDouble() * 0.4));
    }

    private void update(double dt) {
        for (Cloud c : clouds) { c.x += c.speed * 10 * dt; if (c.x > W + 100) c.x = -100; }
        for (Bird b : birds) { b.x += b.speed * 40 * dt; if (b.x > W + 60) b.x = -60; }
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx * dt; p.y += p.vy * dt;
            p.vy += 200 * dt;
            p.life -= dt;
            if (p.life <= 0) it.remove();
        }
        if (minionA != null && minionA.runCycle > 0) minionA.animPhase += dt * 16;
        if (minionB != null && minionB.runCycle > 0) minionB.animPhase += dt * 16;
        if (minionA != null) minionA.blinkTimer -= dt;
        if (minionB != null) minionB.blinkTimer -= dt;
    }

    // ---------- Render ----------
    private void render() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        // Sky gradient (warm afternoon)
        LinearGradient sky = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#4a9de0")),
                new Stop(0.5, Color.web("#8ec7f0")),
                new Stop(0.85, Color.web("#fce9b5")));
        g.setFill(sky);
        g.fillRect(0, 0, W, H);

        // Sun
        RadialGradient sun = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(0.35, Color.web("#fff0a0")),
                new Stop(1, Color.web("#ffd95c", 0)));
        g.setFill(sun);
        g.fillOval(W - 260, 40, 240, 240);

        // Clouds
        for (Cloud c : clouds) drawCloud(g, c);

        // Birds
        for (Bird b : birds) drawBird(g, b, timeSec);

        // Far hills
        for (Hill h : farHills) {
            g.setFill(Color.web("#7fae6a"));
            g.fillOval(h.x, h.y, h.w, h.w * 0.7);
        }
        // Near hills
        for (Hill h : nearHills) {
            g.setFill(Color.web("#5c8a48"));
            g.fillOval(h.x, h.y, h.w, h.w * 0.8);
        }

        // Ground
        LinearGradient ground = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7cb342")),
                new Stop(0.08, Color.web("#a3733e")),
                new Stop(0.5, Color.web("#7a5230")),
                new Stop(1, Color.web("#4a3218")));
        g.setFill(ground);
        g.fillRect(0, GROUND_Y, W, H - GROUND_Y);

        // Track texture stripes
        g.setStroke(Color.web("#8c6a40", 0.4));
        g.setLineWidth(2);
        for (int x = 0; x < W; x += 50) {
            g.strokeLine(x, GROUND_Y + 60, x + 25, GROUND_Y + 60);
            g.strokeLine(x + 30, GROUND_Y + 120, x + 55, GROUND_Y + 120);
        }

        // Grass tufts
        for (GrassTuft t : grass) {
            g.setStroke(Color.web("#3f7a2b"));
            g.setLineWidth(2);
            for (int i = 0; i < 3; i++) {
                double dx = (i - 1) * 3;
                g.strokeLine(t.x + dx, t.y, t.x + dx + Math.sin(timeSec * 2 + t.x) * 2, t.y - t.h);
            }
        }

        // Finish line
        drawFinishLine(g);

        // Progress HUD
        if (minionA != null) {
            drawHUD(g, minionA, 20, 70);
            drawHUD(g, minionB, 20, 130);
        }

        // Minions
        if (minionA != null) drawMinion(g, minionA);
        if (minionB != null) drawMinion(g, minionB);

        // Particles above
        for (Particle p : particles) {
            double a = Math.max(0, Math.min(1, p.life));
            g.setFill(Color.color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), a));
            g.fillOval(p.x, p.y, p.size, p.size);
        }

        // Intro overlay
        if (minionA == null) {
            g.setFill(Color.color(0, 0, 0, 0.55));
            g.fillRoundRect(W / 2 - 300, H / 2 - 90, 600, 180, 22, 22);
            g.setTextAlign(TextAlignment.CENTER);
            g.setFill(Color.web("#ffe066"));
            g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 34));
            g.fillText("🍌 MINION CROSS-COUNTRY", W / 2, H / 2 - 20);
            g.setFill(Color.WHITE);
            g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
            g.fillText("RACING ADVENTURE", W / 2, H / 2 + 15);
            g.setFont(Font.font("Segoe UI", 16));
            g.fillText("Pick a mode below to start the race!", W / 2, H / 2 + 55);
        }
    }

    // ---------- Drawing helpers ----------

    private void drawCloud(GraphicsContext g, Cloud c) {
        g.setFill(Color.color(1, 1, 1, 0.9));
        double s = c.scale;
        g.fillOval(c.x, c.y, 70 * s, 34 * s);
        g.fillOval(c.x + 24 * s, c.y - 14 * s, 56 * s, 40 * s);
        g.fillOval(c.x + 46 * s, c.y, 62 * s, 34 * s);
    }

    private void drawBird(GraphicsContext g, Bird b, double t) {
        g.setStroke(Color.web("#333"));
        g.setLineWidth(2);
        double flap = Math.sin(t * 6 + b.x) * 4;
        g.strokeLine(b.x, b.y, b.x + 8, b.y - 4 + flap);
        g.strokeLine(b.x, b.y, b.x - 8, b.y - 4 + flap);
    }

    private void drawFinishLine(GraphicsContext g) {
        double fx = xForPos(TRACK_LENGTH);
        int sq = 14;
        for (int y = (int) GROUND_Y - 100; y < GROUND_Y + 220; y += sq) {
            for (int col = 0; col < 2; col++) {
                boolean white = ((y / sq) + col) % 2 == 0;
                g.setFill(white ? Color.WHITE : Color.BLACK);
                g.fillRect(fx + col * sq, y, sq, sq);
            }
        }
        // Flagpole
        g.setStroke(Color.web("#444"));
        g.setLineWidth(4);
        g.strokeLine(fx + sq, GROUND_Y - 100, fx + sq, GROUND_Y - 180);
        // Waving flag
        g.setFill(Color.web("#e74c3c"));
        double wave = Math.sin(timeSec * 4) * 4;
        g.fillPolygon(
                new double[]{fx + sq, fx + sq + 60, fx + sq + 55, fx + sq},
                new double[]{GROUND_Y - 180, GROUND_Y - 175 + wave, GROUND_Y - 145 + wave, GROUND_Y - 140}, 4);
        g.setFill(Color.web("#f1c40f"));
        g.fillOval(fx + sq + 20, GROUND_Y - 168 + wave / 2, 16, 16);
    }

    private void drawHUD(GraphicsContext g, Minion m, double x, double y) {
        g.setFill(Color.color(0.05, 0.05, 0.1, 0.55));
        g.fillRoundRect(x, y, 320, 50, 12, 12);
        // mini head
        g.setFill(m.bodyColor);
        g.fillOval(x + 10, y + 10, 30, 30);
        g.setFill(Color.web("#444"));
        g.fillOval(x + 14, y + 18, 22, 14);
        g.setFill(Color.WHITE);
        g.fillOval(x + 18, y + 20, 14, 14);
        g.setFill(Color.web("#222"));
        g.fillOval(x + 24, y + 24, 6, 6);

        g.setFill(Color.WHITE);
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        g.fillText(m.name, x + 52, y + 22);

        double bx = x + 52, by = y + 30, bw = 250, bh = 12;
        g.setFill(Color.color(0, 0, 0, 0.5));
        g.fillRoundRect(bx, by, bw, bh, 6, 6);
        double frac = m.position / (double) TRACK_LENGTH;
        g.setFill(m.overallColor);
        g.fillRoundRect(bx, by, bw * frac, bh, 6, 6);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Segoe UI", 10));
        g.fillText(Math.round(frac * 100) + "%", bx + bw + 6, by + 11);
    }

    private double laneY(Minion m) { return m == minionA ? GROUND_Y + 80 : GROUND_Y + 80 + LANE_GAP; }

    private double xForPos(int pos) {
        double left = 90, right = W - 130;
        return left + (right - left) * (pos / (double) TRACK_LENGTH);
    }

    // ---------- Minion drawing (the star of the show) ----------
    private void drawMinion(GraphicsContext g, Minion m) {
        double x = m.visualX;
        double y = laneY(m);
        double run = m.runCycle;

        // Shadow
        g.setFill(Color.color(0, 0, 0, 0.28));
        g.fillOval(x - 28, y + 34, 62, 12);

        // Wobble / bob (when running)
        double bob = run > 0 ? Math.sin(m.animPhase) * 3 : Math.sin(timeSec * 2) * 1.2;
        y -= bob;

        // Legs (animated)
        double legSwing = run > 0 ? Math.sin(m.animPhase) * 12 : 0;
        g.setFill(Color.web("#2c2c2c"));
        // left leg
        g.fillRoundRect(x - 18 + legSwing, y + 18, 14, 22, 6, 6);
        // right leg
        g.fillRoundRect(x + 4 - legSwing, y + 18, 14, 22, 6, 6);
        // shoes
        g.setFill(Color.web("#111"));
        g.fillOval(x - 20 + legSwing, y + 34, 18, 8);
        g.fillOval(x + 4 - legSwing, y + 34, 18, 8);

        // Body (yellow oval)
        RadialGradient bodyGrad = new RadialGradient(0, 0,
                0.35, 0.35, 0.9, true, CycleMethod.NO_CYCLE,
                new Stop(0, m.bodyColor.brighter()),
                new Stop(0.7, m.bodyColor),
                new Stop(1, m.bodyColor.darker()));
        g.setFill(bodyGrad);
        g.fillOval(x - 30, y - 45, 60, 75);

        // Overalls
        g.setFill(m.overallColor);
        g.fillRoundRect(x - 24, y + 4, 48, 40, 10, 10);
        g.setFill(m.overallColor.brighter());
        // straps
        g.setLineWidth(6);
        g.setStroke(m.overallColor);
        g.strokeLine(x - 18, y + 4, x - 10, y - 30);
        g.strokeLine(x + 18, y + 4, x + 10, y - 30);
        // pocket emblem (banana!)
        g.setFill(Color.web("#2c1e00"));
        g.setFont(Font.font("Segoe UI Emoji", 14));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText("🍌", x, y + 28);

        // Goggle strap
        g.setStroke(Color.web("#2a2a2a"));
        g.setLineWidth(6);
        g.strokeLine(x - 28, y - 25, x + 28, y - 25);

        // Goggle frame
        g.setFill(Color.web("#7b7b7b"));
        g.fillOval(x - 26, y - 42, 52, 38);

        // Goggle rim
        g.setStroke(Color.web("#4a4a4a"));
        g.setLineWidth(2);
        g.strokeOval(x - 26, y - 42, 52, 38);
        // goggle strap lines
        g.setStroke(Color.web("#333"));
        g.strokeLine(x - 26, y - 22, x + 26, y - 22);

        // Two eyes (with occasional blink)
        Random blinkRng = new Random();
        boolean blinking = m.blinkTimer < 0.12;
        double eyeOpen = blinking ? 0.15 : 1.0;

        // Left eye
        g.setFill(Color.WHITE);
        g.fillOval(x - 22, y - 38, 22, 22 * eyeOpen + 3);
        g.setStroke(Color.web("#666"));
        g.setLineWidth(1.5);
        g.strokeOval(x - 22, y - 38, 22, 22 * eyeOpen + 3);
        // Left pupil
        g.setFill(Color.web("#3b2a1a"));
        g.fillOval(x - 15 + Math.sin(timeSec + 1) * 2, y - 32, 9, 9 * eyeOpen + 1);
        g.setFill(Color.BLACK);
        g.fillOval(x - 13 + Math.sin(timeSec + 1) * 2, y - 30, 5, 5 * eyeOpen + 1);

        // Right eye
        g.setFill(Color.WHITE);
        g.fillOval(x + 1, y - 38, 22, 22 * eyeOpen + 3);
        g.setStroke(Color.web("#666"));
        g.strokeOval(x + 1, y - 38, 22, 22 * eyeOpen + 3);
        g.setFill(Color.web("#3b2a1a"));
        g.fillOval(x + 6 + Math.sin(timeSec + 1) * 2, y - 32, 9, 9 * eyeOpen + 1);
        g.setFill(Color.BLACK);
        g.fillOval(x + 8 + Math.sin(timeSec + 1) * 2, y - 30, 5, 5 * eyeOpen + 1);

        // Goggle bridge (connecting the eyes)
        g.setFill(Color.web("#666"));
        g.fillRect(x - 2, y - 30, 4, 8);

        // Hair strands (springy)
        g.setStroke(Color.web("#111"));
        g.setLineWidth(2);
        for (int i = 0; i < 5; i++) {
            double hx = x - 18 + i * 9;
            double sway = Math.sin(timeSec * 3 + i) * 3 + (run > 0 ? Math.sin(m.animPhase + i) * 4 : 0);
            g.strokeLine(hx, y - 44, hx + sway, y - 56);
        }

        // Mouth
        g.setStroke(Color.web("#333"));
        g.setLineWidth(2.5);
        double smile = 8 + Math.sin(timeSec * 2) * 1.5;
        g.strokeArc(x - 12, y - 8, 24, smile * 2, 200, 140, javafx.scene.shape.ArcType.OPEN);

        // Teeth (two little white rectangles)
        g.setFill(Color.WHITE);
        g.fillRect(x - 7, y - 4, 5, 5);
        g.fillRect(x + 2, y - 4, 5, 5);
        g.setStroke(Color.web("#999"));
        g.setLineWidth(0.8);
        g.strokeRect(x - 7, y - 4, 5, 5);
        g.strokeRect(x + 2, y - 4, 5, 5);

        // Arms (swing when running)
        g.setLineWidth(7);
        g.setStroke(m.bodyColor.darker());
        g.setLineCap(StrokeLineCap.ROUND);
        double armSwing = run > 0 ? Math.sin(m.animPhase + Math.PI) * 14 : 0;
        // Left arm
        g.strokeLine(x - 28, y - 20, x - 42, y - 5 + armSwing);
        // Right arm
        g.strokeLine(x + 28, y - 20, x + 42, y - 5 - armSwing);
        // Hands
        g.setFill(m.bodyColor);
        g.fillOval(x - 47, y - 10 + armSwing, 12, 12);
        g.fillOval(x + 36, y - 10 - armSwing, 12, 12);

        // Name tag
        g.setFill(Color.color(0.1, 0.1, 0.15, 0.65));
        g.fillRoundRect(x - 40, y - 100, 80, 22, 8, 8);
        g.setFill(Color.web("#ffe066"));
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(m.name, x, y - 85);
    }

    // ---------- Data classes ----------
    private static class Minion {
        String name;
        int position;
        double visualX = 80;
        double animPhase = 0;
        double runCycle = 0;
        double blinkTimer = 2 + Math.random() * 3;
        Color bodyColor, overallColor;
        boolean isComputer;
        Move currentMove;

        Minion(String name, int pos, Color body, Color overall, boolean ai) {
            this.name = name;
            this.position = pos;
            this.bodyColor = body;
            this.overallColor = overall;
            this.isComputer = ai;
        }
    }

    private static class Particle {
        double x, y, vx, vy, life, size;
        Color color;
        Particle(double x, double y, double vx, double vy, double life, Color c, double size) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.life = life; this.color = c; this.size = size;
        }
    }

    private static class Cloud {
        double x, y, speed, scale;
        Cloud(double x, double y, double s, double sc) { this.x = x; this.y = y; speed = s; scale = sc; }
    }
    private static class Hill {
        double x, y, w;
        Hill(double x, double y, double w) { this.x = x; this.y = y; this.w = w; }
    }
    private static class GrassTuft {
        double x, y, h;
        GrassTuft(double x, double y, double h) { this.x = x; this.y = y; this.h = h; }
    }
    private static class Bird {
        double x, y, speed;
        Bird(double x, double y, double s) { this.x = x; this.y = y; this.speed = s; }
    }

    public static void main(String[] args) { launch(args); }
}