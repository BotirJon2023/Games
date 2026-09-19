import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class ExtremeMountainBikeRacingGame extends JPanel implements Runnable {
    private static final int W = 1280, H = 720;
    private static final int TRACK_LENGTH = 5200;

    private final Random rng = new Random(42);
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Tree> trees = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private final Bike p1 = new Bike("PLAYER 1", 0.0, 0.0);
    private final Bike p2 = new Bike("PLAYER 2", 0.0, 0.0);
    private final Bike cpu = new Bike("CPU", 0.0, 0.0);

    private boolean twoPlayers = false;
    private boolean running = true;
    private boolean raceStarted = false;
    private boolean raceFinished = false;
    private long countdownStart;
    private long raceStartNanos;
    private long finishNanos;

    private double cameraX = 0;
    private double shake = 0;
    private long lastNanos = System.nanoTime();

    private final boolean[] keys = new boolean[600];

    public ExtremeMountainBikeRacingGame() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setupWorld();

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k >= 0 && k < keys.length) keys[k] = true;

                if (k == KeyEvent.VK_ENTER && !raceStarted) startRace();
                if (k == KeyEvent.VK_R && raceFinished) resetRace();
                if (k == KeyEvent.VK_ESCAPE) System.exit(0);
                if (k == KeyEvent.VK_F2) {
                    twoPlayers = !twoPlayers;
                    resetRace();
                }
            }

            @Override public void keyReleased(KeyEvent e) {
                int k = e.getKeyCode();
                if (k >= 0 && k < keys.length) keys[k] = false;
            }
        });

        new Thread(this, "MTB-Game-Loop").start();
    }

    private void setupWorld() {
        // Procedural obstacles and scenery.
        for (int i = 260; i < TRACK_LENGTH - 150; i += 170 + rng.nextInt(120)) {
            obstacles.add(new Obstacle(i, -0.58 + rng.nextDouble() * 1.16,
                    rng.nextBoolean() ? Type.ROCK : Type.LOG));
        }

        for (int i = 0; i < TRACK_LENGTH; i += 70) {
            int count = 2 + rng.nextInt(3);
            for (int j = 0; j < count; j++) {
                double side = rng.nextBoolean() ? -1 : 1;
                double x = side * (0.82 + rng.nextDouble() * 0.75);
                trees.add(new Tree(i + rng.nextInt(45), x, 0.8 + rng.nextDouble() * 0.8));
            }
        }
    }

    private void startRace() {
        if (raceStarted) return;
        raceStarted = true;
        raceFinished = false;
        countdownStart = System.nanoTime();
        raceStartNanos = countdownStart + 4_000_000_000L;
    }

    private void resetRace() {
        p1.reset(0);
        p2.reset(0);
        cpu.reset(0);
        raceStarted = false;
        raceFinished = false;
        particles.clear();
        shake = 0;
        cameraX = 0;
    }

    @Override
    public void run() {
        while (running) {
            long now = System.nanoTime();
            double dt = Math.min(0.033, (now - lastNanos) / 1_000_000_000.0);
            lastNanos = now;

            update(dt);
            repaint();

            try { Thread.sleep(8); } catch (InterruptedException ignored) {}
        }
    }

    private void update(double dt) {
        if (!raceStarted || raceFinished) return;

        long now = System.nanoTime();
        if (now < raceStartNanos) return;

        if (raceStartNanos == now) return;

        updatePlayer(p1, dt, KeyMap.P1);

        if (twoPlayers) {
            updatePlayer(p2, dt, KeyMap.P2);
        } else {
            updateCPU(dt);
        }

        updateParticles(dt);

        double leader = Math.max(p1.distance, twoPlayers ? p2.distance : cpu.distance);
        cameraX += (leader - cameraX) * Math.min(1, dt * 5.0);

        shake *= Math.pow(0.03, dt);
        if (shake < 0.05) shake = 0;

        if (p1.distance >= TRACK_LENGTH ||
                (twoPlayers && p2.distance >= TRACK_LENGTH) ||
                (!twoPlayers && cpu.distance >= TRACK_LENGTH)) {
            raceFinished = true;
            finishNanos = now;
        }
    }

    private void updatePlayer(Bike b, double dt, KeyMap km) {
        boolean accel = down(km.accel);
        boolean brake = down(km.brake);
        boolean left = down(km.left);
        boolean right = down(km.right);
        boolean boost = down(km.boost);

        double target = accel ? b.maxSpeed : b.maxSpeed * 0.43;
        if (brake) target *= 0.35;

        b.speed += (target - b.speed) * dt * (brake ? 7 : 2.6);

        if (boost && b.boost > 0.01 && accel) {
            b.speed += 23 * dt;
            b.boost -= 0.27 * dt;
            emitExhaust(b, 3);
        } else {
            b.boost = Math.min(1, b.boost + 0.035 * dt);
        }

        double steer = (left ? -1 : 0) + (right ? 1 : 0);
        b.lane += steer * dt * (0.85 + b.speed / 48.0);
        b.lane *= 0.996;
        b.lane = clamp(b.lane, -0.82, 0.82);

        // Air / jump physics.
        if (b.jumpTimer > 0) {
            b.jumpTimer -= dt;
            b.vertical = Math.sin((1 - b.jumpTimer / b.jumpDuration) * Math.PI) * 0.9;
        } else {
            b.vertical *= Math.pow(0.01, dt);
        }

        b.distance += b.speed * dt;

        for (Obstacle o : obstacles) {
            if (!o.hit && Math.abs(o.distance - b.distance) < 9 &&
                    Math.abs(o.lane - b.lane) < 0.18 && b.vertical < 0.25) {
                o.hit = true;
                b.speed *= 0.48;
                shake = 9;
                emitDust(b, 20);
            }
        }

        if (b.speed > 39 && rng.nextDouble() < dt * 2.0) emitDust(b, 2);
        if (rng.nextDouble() < dt * 7) emitTrail(b);
    }

    private void updateCPU(double dt) {
        double idealLane = Math.sin(cpu.distance * 0.0031) * 0.48;

        for (Obstacle o : obstacles) {
            if (!o.hit && o.distance > cpu.distance && o.distance - cpu.distance < 130
                    && Math.abs(o.lane - cpu.lane) < 0.2) {
                idealLane += o.lane > 0 ? -0.42 : 0.42;
            }
        }

        if (cpu.lane < idealLane - 0.04) cpu.lane += dt * 0.55;
        if (cpu.lane > idealLane + 0.04) cpu.lane -= dt * 0.55;

        cpu.speed += (cpu.maxSpeed * 0.88 - cpu.speed) * dt * 1.8;
        cpu.distance += cpu.speed * dt;

        if (rng.nextDouble() < dt * 5) emitTrail(cpu);
    }

    private void updateParticles(double dt) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life -= dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vy += 18 * dt;
            p.size *= 0.992;
            if (p.life <= 0) it.remove();
        }
    }

    private void emitDust(Bike b, int n) {
        for (int i = 0; i < n; i++) {
            particles.add(new Particle(
                    0.5 + b.lane * 0.22,
                    H * 0.79 + rng.nextDouble() * 10,
                    (rng.nextDouble() - 0.5) * 45,
                    -rng.nextDouble() * 45,
                    0.35 + rng.nextDouble() * 0.45,
                    5 + rng.nextDouble() * 12
            ));
        }
    }

    private void emitTrail(Bike b) {
        particles.add(new Particle(
                0.5 + b.lane * 0.20,
                H * 0.82,
                (rng.nextDouble() - 0.5) * 16,
                -rng.nextDouble() * 12,
                0.22,
                2 + rng.nextDouble() * 5
        ));
    }

    private void emitExhaust(Bike b, int n) {
        for (int i = 0; i < n; i++) {
            particles.add(new Particle(
                    0.5 + b.lane * 0.18,
                    H * 0.80,
                    (rng.nextDouble() - 0.5) * 50,
                    rng.nextDouble() * 25,
                    0.25,
                    3 + rng.nextDouble() * 5
            ));
        }
    }

    private boolean down(int code) {
        return code >= 0 && code < keys.length && keys[code];
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        drawSky(g);
        drawMountains(g);
        drawTrack(g);
        drawScenery(g);
        drawBikes(g);
        drawParticles(g);
        drawHUD(g);

        if (!raceStarted) drawStartScreen(g);
        else if (!raceFinished) drawCountdown(g);
        else drawFinishScreen(g);

        g.dispose();
    }

    private void drawSky(Graphics2D g) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(50, 125, 190),
                0, H * 0.72f, new Color(210, 230, 220));
        g.setPaint(sky);
        g.fillRect(0, 0, W, H);

        // Sun glow.
        for (int r = 160; r > 15; r -= 12) {
            int alpha = Math.max(4, 28 - r / 7);
            g.setColor(new Color(255, 238, 175, alpha));
            g.fillOval(W - 260 - r / 2, 80 - r / 2, r, r);
        }
        g.setColor(new Color(255, 241, 185, 235));
        g.fillOval(W - 300, 55, 90, 90);
    }

    private void drawMountains(Graphics2D g) {
        Polygon far = new Polygon();
        far.addPoint(0, 370);
        for (int x = 0; x <= W; x += 70) {
            int y = 260 + (int)(40 * Math.sin(x * 0.008) + 28 * Math.sin(x * 0.021));
            far.addPoint(x, y);
        }
        far.addPoint(W, 440);
        far.addPoint(0, 440);
        g.setColor(new Color(91, 119, 134));
        g.fillPolygon(far);

        Polygon snow = new Polygon();
        snow.addPoint(0, 335);
        for (int x = 0; x <= W; x += 75) {
            int y = 245 + (int)(35 * Math.sin(x * 0.009 + 1));
            snow.addPoint(x, y);
            snow.addPoint(x + 38, y - 45 - (x % 90));
            snow.addPoint(x + 72, y);
        }
        snow.addPoint(W, 370);
        snow.addPoint(0, 370);
        g.setColor(new Color(238, 243, 238, 210));
        g.fillPolygon(snow);

        // Distant lake.
        Path2D lake = new Path2D.Double();
        lake.moveTo(700, 370);
        lake.lineTo(1110, 350);
        lake.lineTo(1220, 455);
        lake.lineTo(760, 455);
        lake.closePath();
        g.setColor(new Color(72, 151, 177, 190));
        g.fill(lake);
    }

    private void drawTrack(Graphics2D g) {
        // Large ground.
        g.setColor(new Color(63, 92, 55));
        g.fillRect(0, 380, W, H - 380);

        // Perspective road edges.
        int[] xsL = new int[20], xsR = new int[20], ys = new int[20];
        for (int i = 0; i < 20; i++) {
            double t = i / 19.0;
            double y = 390 + Math.pow(t, 1.6) * 330;
            double width = 80 + Math.pow(t, 1.5) * 560;
            double curve = Math.sin((cameraX + i * 120) * 0.004) * 85 * t;
            xsL[i] = (int)(W / 2 - width + curve);
            xsR[i] = (int)(W / 2 + width + curve);
            ys[i] = (int)y;
        }

        Polygon road = new Polygon();
        for (int i = 0; i < 20; i++) road.addPoint(xsL[i], ys[i]);
        for (int i = 19; i >= 0; i--) road.addPoint(xsR[i], ys[i]);

        GradientPaint dirt = new GradientPaint(0, 400, new Color(128, 91, 57),
                0, H, new Color(71, 47, 33));
        g.setPaint(dirt);
        g.fillPolygon(road);

        // Rocky highlights.
        for (int i = 0; i < 70; i++) {
            int y = 410 + rng.nextInt(290);
            double t = (y - 390) / 330.0;
            double width = 80 + Math.pow(clamp(t, 0, 1), 1.5) * 560;
            int x = (int)(W / 2 - width + rng.nextDouble() * width * 2);
            if (rng.nextDouble() < 0.65) {
                g.setColor(new Color(195, 160, 118, 80));
                int s = 2 + rng.nextInt(8);
                g.fillOval(x, y, s, s / 2 + 1);
            }
        }
    }

    private void drawScenery(Graphics2D g) {
        // Trees ordered roughly by distance to create a depth illusion.
        for (Tree t : trees) {
            double rel = t.distance - cameraX;
            if (rel < -300 || rel > 900) continue;

            double z = 1.0 - rel / 900.0;
            double y = 420 + z * 260;
            double x = W / 2 + t.lane * (110 + z * 460);

            double s = t.scale * (0.35 + z * 1.5);
            int trunkH = (int)(55 * s);
            int crown = (int)(60 * s);

            g.setColor(new Color(72, 52, 38));
            g.fillRect((int)x - Math.max(2, (int)(4*s)), (int)y - trunkH,
                    Math.max(4, (int)(8*s)), trunkH);

            Path2D pine = new Path2D.Double();
            pine.moveTo(x, y - trunkH - crown);
            pine.lineTo(x - crown * 0.62, y - trunkH * 0.25);
            pine.lineTo(x + crown * 0.62, y - trunkH * 0.25);
            pine.closePath();
            g.setColor(new Color(23, 70, 47));
            g.fill(pine);
        }
    }

    private void drawBikes(Graphics2D g) {
        if (twoPlayers) {
            drawBike(g, p2, 0.34, new Color(225, 72, 45), "P2");
            drawBike(g, p1, 0.58, new Color(30, 155, 235), "P1");
        } else {
            double p1Y = 0.57;
            double cpuY = 0.50;
            drawBike(g, cpu, cpuY, new Color(224, 60, 45), "CPU");
            drawBike(g, p1, p1Y, new Color(25, 155, 235), "YOU");
        }
    }

    private void drawBike(Graphics2D g, Bike b, double screenY, Color suit, String label) {
        double rel = b.distance - cameraX;
        double depth = clamp(0.58 + rel * 0.0007, 0.32, 1.15);

        int cx = (int)(W / 2 + b.lane * 330 * depth);
        int cy = (int)(H * screenY - b.vertical * 130);

        int size = (int)(62 * depth);
        int wheel = Math.max(18, size / 2);

        // Shadow.
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(cx - wheel, cy + 24, wheel * 2, 12);

        g.setStroke(new BasicStroke(Math.max(2, 4 * (float)depth),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Wheels.
        g.setColor(new Color(28, 28, 30));
        g.drawOval(cx - wheel, cy - 10, wheel, wheel);
        g.drawOval(cx + 2, cy - 10, wheel, wheel);

        g.setColor(new Color(150, 150, 150, 150));
        g.drawLine(cx - wheel / 2, cy + wheel / 2 - 10, cx + wheel / 2, cy + wheel / 2 - 10);
        g.drawLine(cx - wheel / 2, cy - 10, cx + wheel / 2, cy + wheel - 10);

        // Frame.
        g.setColor(suit);
        g.drawLine(cx - wheel / 2, cy + wheel / 2 - 10, cx + 5, cy + 5);
        g.drawLine(cx + 5, cy + 5, cx + wheel / 2, cy + wheel / 2 - 10);
        g.drawLine(cx - wheel / 2, cy + wheel / 2 - 10, cx + wheel / 2, cy + wheel / 2 - 10);
        g.drawLine(cx + 5, cy + 5, cx + 18, cy - 17);

        // Rider torso.
        g.setColor(suit);
        g.fillRoundRect(cx - 17, cy - 49, 30, 40, 10, 10);

        // Helmet.
        g.setColor(new Color(25, 28, 33));
        g.fillOval(cx - 18, cy - 76, 36, 32);
        g.setColor(suit.brighter());
        g.fillArc(cx - 18, cy - 76, 36, 30, 15, 150);

        // Arms.
        g.setColor(new Color(25, 25, 28));
        g.setStroke(new BasicStroke(Math.max(3, (int)(6*depth))));
        g.drawLine(cx - 7, cy - 39, cx + 17, cy - 25);
        g.drawLine(cx + 8, cy - 38, cx + 28, cy - 28);

        // Speed streak.
        if (b.speed > 35) {
            g.setColor(new Color(255,255,255,70));
            g.drawLine(cx - 70, cy + 10, cx - 30, cy + 7);
            g.drawLine(cx - 60, cy + 20, cx - 25, cy + 17);
        }

        // Name tag.
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        int tw = g.getFontMetrics().stringWidth(label);
        g.setColor(new Color(0,0,0,160));
        g.fillRoundRect(cx - tw/2 - 7, cy - 103, tw + 14, 21, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString(label, cx - tw/2, cy - 88);
    }

    private void drawParticles(Graphics2D g) {
        for (Particle p : particles) {
            int x = (int)(p.x * W);
            int y = (int)p.y;
            int a = (int)(255 * clamp(p.life / 0.45, 0, 1));
            g.setColor(new Color(225, 202, 165, a));
            int s = (int)p.size;
            g.fillOval(x - s/2, y - s/2, s, s);
        }
    }

    private void drawHUD(Graphics2D g) {
        if (!raceStarted) return;

        long now = System.nanoTime();
        double elapsed = Math.max(0, (now - raceStartNanos) / 1e9);

        // Top glass panels.
        panel(g, 24, 22, 260, 116);
        panel(g, W - 300, 22, 276, 116);

        int placeP1 = placeOf(p1);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(120, 210, 255));
        g.drawString("POSITION", 46, 52);
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        g.setColor(Color.WHITE);
        g.drawString(placeP1 + " / " + (twoPlayers ? 2 : 2), 43, 99);

        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(205, 220, 230));
        g.drawString(twoPlayers ? "P1  WASD" : "CPU RIVAL", 155, 58);
        g.drawString(twoPlayers ? "P2  ARROWS" : "1 LAP • EXTREME DESCENT", 155, 84);

        g.setFont(new Font("Monospaced", Font.BOLD, 36));
        g.setColor(Color.WHITE);
        g.drawString(formatTime(elapsed), W - 272, 78);

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(120, 210, 255));
        g.drawString("TIME", W - 270, 45);
        g.setColor(Color.WHITE);
        g.drawString("DISTANCE " + (int)Math.min(TRACK_LENGTH, p1.distance) + " / " + TRACK_LENGTH + " m",
                W - 270, 105);

        // Speedometer.
        int sx = W - 145, sy = H - 132;
        g.setColor(new Color(0,0,0,170));
        g.fillOval(sx - 78, sy - 78, 156, 156);
        g.setColor(new Color(75, 190, 240));
        g.setStroke(new BasicStroke(8));
        g.drawArc(sx - 63, sy - 63, 126, 126, 220, 100);

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(190,205,215));
        g.drawString("KM/H", sx - 20, sy - 18);
        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf((int)(p1.speed * 2.15)), sx - 30, sy + 20);

        // Boost.
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(Color.WHITE);
        g.drawString("NITRO", 42, H - 48);
        g.setColor(new Color(0,0,0,140));
        g.fillRoundRect(42, H - 38, 220, 12, 8, 8);
        g.setColor(new Color(65, 195, 255));
        g.fillRoundRect(42, H - 38, (int)(220 * p1.boost), 12, 8, 8);

        // Controls.
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(235,240,245));
        String controls = twoPlayers
                ? "P1: WASD + SHIFT     P2: ARROWS + ENTER     F2: MODE"
                : "WASD: STEER / ACCELERATE     SHIFT: NITRO     F2: 2 PLAYERS";
        g.drawString(controls, 330, H - 28);

        drawMiniMap(g);
    }

    private void drawMiniMap(Graphics2D g) {
        int x = 24, y = 160, w = 130, h = 170;
        panel(g, x, y, w, h);

        g.setColor(new Color(185, 190, 190));
        g.setStroke(new BasicStroke(4));
        Path2D path = new Path2D.Double();
        path.moveTo(x + 65, y + 15);
        for (int i = 1; i <= 40; i++) {
            double t = i / 40.0;
            double px = x + 65 + Math.sin(t * 10) * 38;
            double py = y + 15 + t * 140;
            path.lineTo(px, py);
        }
        g.draw(path);

        double p = clamp(p1.distance / TRACK_LENGTH, 0, 1);
        int px = (int)(x + 65 + Math.sin(p * 10) * 38);
        int py = (int)(y + 15 + p * 140);

        g.setColor(new Color(40, 170, 240));
        g.fillOval(px - 7, py - 7, 14, 14);

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(Color.WHITE);
        g.drawString("TRACK", x + 40, y + h - 10);
    }

    private void drawStartScreen(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 125));
        g.fillRect(0, 0, W, H);

        centerText(g, "EXTREME", 175, 64, new Color(110, 215, 255));
        centerText(g, "MOUNTAIN BIKE", 238, 54, Color.WHITE);
        centerText(g, "RACING GAME", 292, 36, new Color(230, 235, 238));

        panel(g, W/2 - 300, 340, 600, 210);
        centerText(g, "ENTER  •  START RACE", 395, 22, Color.WHITE);
        centerText(g, "F2  •  Toggle Single Player / Two Players", 435, 17,
                new Color(160, 215, 240));
        centerText(g, "Single: WASD + SHIFT", 470, 16, Color.WHITE);
        centerText(g, "Two players: P1 WASD / P2 ARROWS", 498, 16, Color.WHITE);
        centerText(g, "R after finish  •  ESC quit", 526, 14, new Color(185,190,195));

        String mode = twoPlayers ? "TWO PLAYERS" : "SINGLE PLAYER";
        centerText(g, mode, 590, 22, new Color(90, 210, 255));
    }

    private void drawCountdown(Graphics2D g) {
        long remaining = raceStartNanos - System.nanoTime();
        int n = (int)Math.ceil(remaining / 1e9);

        if (n > 0) {
            g.setColor(new Color(0,0,0,75));
            g.fillRect(0, 0, W, H);
            centerText(g, String.valueOf(n), 360, 110, Color.WHITE);
        } else if (remaining > -900_000_000L) {
            centerText(g, "GO!", 360, 90, new Color(85, 220, 255));
        }
    }

    private void drawFinishScreen(Graphics2D g) {
        g.setColor(new Color(0,0,0,155));
        g.fillRect(0, 0, W, H);

        boolean p1Won = p1.distance >= TRACK_LENGTH &&
                (!twoPlayers ? p1.distance >= cpu.distance : p1.distance >= p2.distance);

        centerText(g, p1Won ? "FINISH!" : "RACE OVER", 235, 68,
                p1Won ? new Color(90, 220, 255) : Color.WHITE);

        double elapsed = Math.max(0, (finishNanos - raceStartNanos) / 1e9);
        centerText(g, "TIME  " + formatTime(elapsed), 295, 27, Color.WHITE);
        centerText(g, "Press R to race again", 380, 21, new Color(185,215,230));
        centerText(g, "Press F2 to switch game mode", 418, 17, new Color(160,175,185));
    }

    private void panel(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(5, 12, 18, 195));
        g.fillRoundRect(x, y, w, h, 18, 18);
        g.setColor(new Color(130, 200, 230, 70));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(x, y, w, h, 18, 18);
    }

    private void centerText(Graphics2D g, String s, int y, int size, Color c) {
        g.setFont(new Font("SansSerif", Font.BOLD, size));
        FontMetrics fm = g.getFontMetrics();
        int x = (W - fm.stringWidth(s)) / 2;
        g.setColor(new Color(0,0,0,120));
        g.drawString(s, x + 3, y + 3);
        g.setColor(c);
        g.drawString(s, x, y);
    }

    private int placeOf(Bike b) {
        if (twoPlayers) return b.distance >= p2.distance ? 1 : 2;
        return b.distance >= cpu.distance ? 1 : 2;
    }

    private String formatTime(double seconds) {
        int m = (int)(seconds / 60);
        double s = seconds - m * 60;
        return String.format(Locale.US, "%02d:%05.2f", m, s);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    enum Type { ROCK, LOG }

    static class Obstacle {
        double distance, lane;
        Type type;
        boolean hit;
        Obstacle(double distance, double lane, Type type) {
            this.distance = distance; this.lane = lane; this.type = type;
        }
    }

    static class Tree {
        double distance, lane, scale;
        Tree(double distance, double lane, double scale) {
            this.distance = distance; this.lane = lane; this.scale = scale;
        }
    }

    static class Particle {
        double x, y, vx, vy, life, size;
        Particle(double x, double y, double vx, double vy, double life, double size) {
            this.x=x; this.y=y; this.vx=vx; this.vy=vy; this.life=life; this.size=size;
        }
    }

    static class Bike {
        String name;
        double distance, lane, speed, maxSpeed = 27;
        double boost = 1;
        double vertical, jumpTimer, jumpDuration;

        Bike(String name, double distance, double lane) {
            this.name=name; this.distance=distance; this.lane=lane;
        }

        void reset(double start) {
            distance=start;
            lane=0;
            speed=0;
            boost=1;
            vertical=0;
            jumpTimer=0;
        }
    }

    static class KeyMap {
        final int accel, brake, left, right, boost;
        KeyMap(int accel, int brake, int left, int right, int boost) {
            this.accel=accel; this.brake=brake; this.left=left; this.right=right; this.boost=boost;
        }
        static final KeyMap P1 = new KeyMap(
                KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_SHIFT);
        static final KeyMap P2 = new KeyMap(
                KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_ENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ExtremeMountainBikeRacingGame");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setContentPane(new ExtremeMountainBikeRacingGame());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
