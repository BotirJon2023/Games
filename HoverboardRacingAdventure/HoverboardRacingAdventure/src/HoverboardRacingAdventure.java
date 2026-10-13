import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class HoverboardRacingAdventure extends JPanel implements ActionListener, KeyListener {
    // Screen settings
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final int TRACK_LENGTH = 12000; // total race distance in world units

    // Game states
    private enum State { MENU, RACING, FINISHED }
    private State state = State.MENU;
    private enum Mode { VS_COMPUTER, VS_PLAYER }
    private Mode mode = Mode.VS_COMPUTER;

    // Player 1 (blue hoverboard)
    private double p1X = 200;
    private double p1Y = HEIGHT - 200;
    private double p1Speed = 0;
    private double p1Distance = 0;
    private double p1Tilt = 0;
    private boolean p1Up, p1Down, p1Left, p1Right;
    private boolean p1Boost = false;
    private double p1BoostFuel = 100;
    private double p1BoostCooldown = 0;

    // Player 2 / AI (red hoverboard)
    private double p2X = 200;
    private double p2Y = HEIGHT - 260;
    private double p2Speed = 0;
    private double p2Distance = 0;
    private double p2Tilt = 0;
    private boolean p2Up, p2Down, p2Left, p2Right;
    private boolean p2Boost = false;
    private double p2BoostFuel = 100;
    private double p2BoostCooldown = 0;

    // AI reaction
    private Random random = new Random();
    private double aiDecisionTimer = 0;
    private double aiTargetY;

    // Obstacles
    private ArrayList<Obstacle> obstacles = new ArrayList<>();
    private ArrayList<PowerUp> powerUps = new ArrayList<>();
    private double nextObstacleSpawn = 0;
    private double nextPowerUpSpawn = 0;

    // Visual effects
    private ArrayList<Particle> particles = new ArrayList<>();
    private ArrayList<Trail> trails = new ArrayList<>();
    private double cameraShake = 0;
    private double backgroundOffset = 0;
    private double waveTime = 0;

    // Game loop
    private Timer timer;
    private int winner = -1;

    public HoverboardRacingAdventure() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(16, this); // ~60 FPS
        timer.start();

        generateWorld();
    }

    private void generateWorld() {
        obstacles.clear();
        powerUps.clear();
        Random r = new Random(42);
        for (double d = 800; d < TRACK_LENGTH - 500; d += 300 + r.nextInt(200)) {
            double y = 200 + r.nextInt(HEIGHT - 400);
            int type = r.nextInt(3);
            obstacles.add(new Obstacle(d, y, type));
        }
        for (double d = 1500; d < TRACK_LENGTH - 500; d += 1200 + r.nextInt(800)) {
            powerUps.add(new PowerUp(d, 200 + r.nextInt(HEIGHT - 400)));
        }
    }

    private void reset() {
        p1Distance = 0; p2Distance = 0;
        p1Speed = 0; p2Speed = 0;
        p1BoostFuel = 100; p2BoostFuel = 100;
        p1BoostCooldown = 0; p2BoostCooldown = 0;
        p1Tilt = 0; p2Tilt = 0;
        winner = -1;
        particles.clear();
        trails.clear();
        aiDecisionTimer = 0;
        generateWorld();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == State.RACING) {
            update();
        }
        waveTime += 0.05;
        repaint();
    }

    private void update() {
        // ---- Player 1 movement ----
        double accel = 0.6;
        double friction = 0.94;
        double maxSpeed = p1Boost ? 14 : 9;

        if (p1Boost && p1BoostFuel > 0 && p1BoostCooldown <= 0) {
            p1Speed += accel * 1.6;
            p1BoostFuel -= 0.8;
            spawnBoostParticles(p1X, p1Y, new Color(100, 200, 255));
        } else {
            p1Speed += accel * 0.5;
        }

        if (p1Up) p1Y -= 5;
        if (p1Down) p1Y += 5;
        if (p1Left) p1Speed -= 0.3;
        if (p1Right) p1Speed += 0.4;

        p1Speed = Math.max(1, Math.min(maxSpeed, p1Speed));
        p1Speed *= friction + 0.04;

        p1Y = Math.max(150, Math.min(HEIGHT - 100, p1Y));
        p1Distance += p1Speed;

        if (p1BoostCooldown > 0) p1BoostCooldown--;
        if (!p1Boost || p1BoostFuel <= 0) {
            p1BoostFuel = Math.min(100, p1BoostFuel + 0.15);
        }

        // ---- Player 2 / AI movement ----
        double accel2 = 0.6;
        double maxSpeed2 = p2Boost ? 14 : 9;

        if (mode == Mode.VS_COMPUTER) {
            aiUpdate();
        } else {
            if (p2Boost && p2BoostFuel > 0 && p2BoostCooldown <= 0) {
                p2Speed += accel2 * 1.6;
                p2BoostFuel -= 0.8;
                spawnBoostParticles(p2X, p2Y, new Color(255, 150, 100));
            } else {
                p2Speed += accel2 * 0.5;
            }
            if (p2Up) p2Y -= 5;
            if (p2Down) p2Y += 5;
            if (p2Left) p2Speed -= 0.3;
            if (p2Right) p2Speed += 0.4;
        }

        p2Speed = Math.max(1, Math.min(maxSpeed2, p2Speed));
        p2Speed *= friction + 0.04;
        p2Y = Math.max(150, Math.min(HEIGHT - 100, p2Y));
        p2Distance += p2Speed;

        if (p2BoostCooldown > 0) p2BoostCooldown--;
        if (!p2Boost || p2BoostFuel <= 0) {
            p2BoostFuel = Math.min(100, p2BoostFuel + 0.15);
        }

        // ---- World scrolling ----
        backgroundOffset += p1Speed * 0.6;
        if (backgroundOffset > WIDTH) backgroundOffset -= WIDTH;

        // ---- Update tilt for visuals ----
        p1Tilt += ((p1Up ? -0.3 : p1Down ? 0.3 : 0) - p1Tilt) * 0.1;
        p2Tilt += ((p2Up ? -0.3 : p2Down ? 0.3 : 0) - p2Tilt) * 0.1;

        // ---- Collision detection ----
        Rectangle2D p1Rect = new Rectangle2D.Double(p1X - 40, p1Y - 20, 80, 30);
        Rectangle2D p2Rect = new Rectangle2D.Double(p2X - 40, p2Y - 20, 80, 30);

        for (Obstacle o : obstacles) {
            double screenX = o.distance - p1Distance + 400;
            if (screenX > -150 && screenX < WIDTH + 150) {
                Rectangle2D oRect = new Rectangle2D.Double(screenX - o.size / 2, o.y - o.size / 2, o.size, o.size);
                if (p1Rect.intersects(oRect) && !o.hit1) {
                    p1Speed *= 0.35;
                    cameraShake = 12;
                    spawnExplosion(screenX, o.y, new Color(255, 80, 80));
                    o.hit1 = true;
                }
                if (p2Rect.intersects(oRect) && !o.hit2) {
                    p2Speed *= 0.35;
                    cameraShake = 12;
                    spawnExplosion(screenX, o.y, new Color(255, 200, 80));
                    o.hit2 = true;
                }
            }
        }

        // Reset hit flags when off-screen
        for (Obstacle o : obstacles) {
            double screenX = o.distance - p1Distance + 400;
            if (screenX < -200 || screenX > WIDTH + 200) {
                o.hit1 = false;
                o.hit2 = false;
            }
        }

        // ---- Power-ups ----
        for (PowerUp p : powerUps) {
            if (p.collected) continue;
            double screenX = p.distance - p1Distance + 400;
            if (screenX > -100 && screenX < WIDTH + 100) {
                Rectangle2D pRect = new Rectangle2D.Double(screenX - 20, p.y - 20, 40, 40);
                if (p1Rect.intersects(pRect)) {
                    p.collected = true;
                    p1BoostFuel = Math.min(100, p1BoostFuel + 50);
                    spawnExplosion(screenX, p.y, new Color(120, 255, 120));
                } else if (p2Rect.intersects(pRect)) {
                    p.collected = true;
                    p2BoostFuel = Math.min(100, p2BoostFuel + 50);
                    spawnExplosion(screenX, p.y, new Color(255, 255, 120));
                }
            }
        }

        // ---- Particles & trails ----
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.life--;
            if (p.life <= 0) particles.remove(i);
        }

        trails.add(new Trail(p1X - 30, p1Y + 5, new Color(100, 200, 255)));
        trails.add(new Trail(p2X - 30, p2Y + 5, new Color(255, 130, 130)));
        for (int i = trails.size() - 1; i >= 0; i--) {
            Trail t = trails.get(i);
            t.life -= 0.03;
            if (t.life <= 0) trails.remove(i);
        }

        if (cameraShake > 0) cameraShake *= 0.9;

        // ---- Check finish ----
        if (p1Distance >= TRACK_LENGTH && winner == -1) winner = 1;
        if (p2Distance >= TRACK_LENGTH && winner == -1) winner = 2;
        if (winner != -1) {
            state = State.FINISHED;
        }
    }

    private void aiUpdate() {
        aiDecisionTimer--;
        if (aiDecisionTimer <= 0) {
            aiDecisionTimer = 20 + random.nextInt(30);
            aiTargetY = 180 + random.nextInt(HEIGHT - 320);

            // Try to dodge nearest obstacle
            for (Obstacle o : obstacles) {
                double dx = o.distance - p2Distance;
                if (dx > 100 && dx < 600) {
                    if (Math.abs(o.y - p2Y) < 60) {
                        aiTargetY = o.y < p2Y ? o.y + 80 : o.y - 80;
                    }
                }
            }

            // Grab power-ups
            for (PowerUp p : powerUps) {
                if (p.collected) continue;
                double dx = p.distance - p2Distance;
                if (dx > 100 && dx < 500) {
                    aiTargetY = p.y;
                    break;
                }
            }
        }

        // Move toward target Y
        double diff = aiTargetY - p2Y;
        if (diff < -3) { p2Up = true; p2Down = false; }
        else if (diff > 3) { p2Down = true; p2Up = false; }
        else { p2Up = false; p2Down = false; }

        // Boost occasionally
        if (p2BoostFuel > 50 && random.nextInt(200) < 2) {
            p2Boost = true;
        }
        if (p2BoostFuel < 20 || random.nextInt(100) < 2) {
            p2Boost = false;
        }

        // Slight rubber-banding so AI keeps pace
        double distanceGap = p1Distance - p2Distance;
        if (distanceGap > 800) p2Speed += 0.4;
        else if (distanceGap < -800) p2Speed -= 0.2;

        if (p2Boost && p2BoostFuel > 0 && p2BoostCooldown <= 0) {
            p2Speed += 0.9;
            p2BoostFuel -= 0.8;
            spawnBoostParticles(p2X, p2Y, new Color(255, 150, 100));
        }
    }

    private void spawnBoostParticles(double x, double y, Color c) {
        for (int i = 0; i < 2; i++) {
            particles.add(new Particle(x - 30, y + (random.nextDouble() - 0.5) * 20,
                    -2 - random.nextDouble() * 3, (random.nextDouble() - 0.5) * 2,
                    c, 30));
        }
    }

    private void spawnExplosion(double x, double y, Color c) {
        for (int i = 0; i < 30; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1 + random.nextDouble() * 6;
            particles.add(new Particle(x, y,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    c, 40 + random.nextInt(20)));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Camera shake
        int shakeX = 0, shakeY = 0;
        if (cameraShake > 0.5) {
            shakeX = (int)((random.nextDouble() - 0.5) * cameraShake);
            shakeY = (int)((random.nextDouble() - 0.5) * cameraShake);
        }
        g2.translate(shakeX, shakeY);

        if (state == State.MENU) {
            drawMenu(g2);
            return;
        }

        // ---- Background: gradient sky ----
        GradientPaint sky = new GradientPaint(0, 0, new Color(20, 10, 60),
                0, HEIGHT, new Color(120, 30, 120));
        g2.setPaint(sky);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // ---- Neon grid floor ----
        g2.setColor(new Color(80, 0, 150, 120));
        for (int i = 0; i < 20; i++) {
            int x = (int)((i * 60 - backgroundOffset % 60 + WIDTH) % (WIDTH + 60)) - 60;
            g2.drawLine(x, HEIGHT / 2, x - 200, HEIGHT);
        }
        for (int i = 0; i < 12; i++) {
            int y = HEIGHT / 2 + i * (HEIGHT / 2) / 12;
            g2.drawLine(0, y, WIDTH, y);
        }

        // ---- Distant neon city silhouettes ----
        drawCity(g2, (int)(backgroundOffset * 0.2) % WIDTH);

        // ---- Waves of energy in background ----
        g2.setColor(new Color(255, 100, 255, 40));
        for (int i = 0; i < 3; i++) {
            Path2D wave = new Path2D.Double();
            wave.moveTo(0, HEIGHT / 2 + 80 + i * 40);
            for (int x = 0; x <= WIDTH; x += 20) {
                double y = HEIGHT / 2 + 80 + i * 40 + Math.sin((x + waveTime * 50 + i * 100) * 0.02) * 15;
                wave.lineTo(x, y);
            }
            g2.setStroke(new BasicStroke(3));
            g2.draw(wave);
        }

        // ---- Power-ups ----
        for (PowerUp p : powerUps) {
            if (p.collected) continue;
            double screenX = p.distance - p1Distance + 400;
            if (screenX < -100 || screenX > WIDTH + 100) continue;
            drawPowerUp(g2, screenX, p.y);
        }

        // ---- Obstacles ----
        for (Obstacle o : obstacles) {
            double screenX = o.distance - p1Distance + 400;
            if (screenX < -150 || screenX > WIDTH + 150) continue;
            drawObstacle(g2, screenX, o.y, o.type);
        }

        // ---- Trails ----
        for (Trail t : trails) {
            g2.setColor(new Color(t.color.getRed(), t.color.getGreen(), t.color.getBlue(),
                    (int)(t.life * 180)));
            g2.fillOval((int)t.x, (int)t.y, (int)(12 * t.life), (int)(8 * t.life));
        }

        // ---- Hoverboards ----
        drawHoverboard(g2, p1X, p1Y, p1Tilt, new Color(80, 180, 255), new Color(0, 80, 200), 1);
        drawHoverboard(g2, p2X, p2Y, p2Tilt, new Color(255, 100, 100), new Color(200, 30, 30), 2);

        // ---- Particles ----
        for (Particle p : particles) {
            int alpha = Math.min(255, p.life * 8);
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alpha));
            g2.fillOval((int)p.x, (int)p.y, 5, 5);
        }

        // ---- HUD ----
        drawHUD(g2);

        // ---- Finished overlay ----
        if (state == State.FINISHED) {
            drawFinish(g2);
        }
    }

    private void drawCity(Graphics2D g2, int offset) {
        g2.setColor(new Color(20, 0, 60, 180));
        Random r = new Random(123);
        for (int i = 0; i < 30; i++) {
            int x = i * 80 - offset;
            int w = 40 + r.nextInt(50);
            int h = 80 + r.nextInt(180);
            g2.fillRect(x, HEIGHT / 2 - h, w, h);
            // windows
            g2.setColor(new Color(255, 200, 100, 100));
            for (int wy = HEIGHT / 2 - h + 10; wy < HEIGHT / 2 - 10; wy += 20) {
                for (int wx = x + 8; wx < x + w - 8; wx += 15) {
                    if (r.nextInt(3) == 0) g2.fillRect(wx, wy, 6, 8);
                }
            }
            g2.setColor(new Color(20, 0, 60, 180));
        }
    }

    private void drawHoverboard(Graphics2D g2, double x, double y, double tilt, Color glow, Color body, int num) {
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(tilt);

        // Glow beneath
        for (int i = 4; i > 0; i--) {
            g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 30));
            g2.fillOval(-60 - i * 5, 10 - i * 2, 120 + i * 10, 25 + i * 4);
        }

        // Board shadow
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillOval(-45, 12, 90, 18);

        // Board body
        GradientPaint boardGrad = new GradientPaint(0, -15, glow, 0, 15, body);
        g2.setPaint(boardGrad);
        RoundRectangle2D board = new RoundRectangle2D.Double(-45, -10, 90, 22, 20, 20);
        g2.fill(board);

        // Highlight
        g2.setColor(new Color(255, 255, 255, 120));
        g2.fillRoundRect(-40, -8, 80, 6, 10, 10);

        // Neon edge
        g2.setStroke(new BasicStroke(2));
        g2.setColor(glow);
        g2.draw(board);

        // Rider (simple stick figure with helmet)
        g2.setColor(new Color(40, 40, 60));
        g2.fillOval(-8, -42, 16, 16); // head
        g2.setColor(num == 1 ? new Color(100, 200, 255) : new Color(255, 120, 120));
        g2.fillOval(-9, -43, 18, 18); // helmet glow
        g2.setColor(new Color(30, 30, 50));
        g2.fillRect(-6, -26, 12, 18); // torso
        // arms
        g2.setStroke(new BasicStroke(4));
        g2.setColor(new Color(30, 30, 50));
        g2.drawLine(-6, -20, -18, -28);
        g2.drawLine(6, -20, 18, -28);
        // legs
        g2.drawLine(-3, -8, -8, 0);
        g2.drawLine(3, -8, 8, 0);

        // Player number
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("P" + num, -5, -48);

        g2.setTransform(old);
    }

    private void drawObstacle(Graphics2D g2, double x, double y, int type) {
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);

        if (type == 0) {
            // Neon pyramid
            int[] xs = {0, -30, 30};
            int[] ys = {-30, 30, 30};
            g2.setColor(new Color(255, 50, 150, 100));
            g2.fillPolygon(xs, ys, 3);
            g2.setColor(new Color(255, 100, 200));
            g2.setStroke(new BasicStroke(3));
            g2.drawPolygon(xs, ys, 3);
        } else if (type == 1) {
            // Floating orb
            for (int i = 5; i > 0; i--) {
                g2.setColor(new Color(255, 200, 0, 30));
                g2.fillOval(-i * 8 - 5, -i * 8 - 5, (i * 8 + 5) * 2, (i * 8 + 5) * 2);
            }
            g2.setColor(new Color(255, 230, 100));
            g2.fillOval(-18, -18, 36, 36);
            g2.setColor(new Color(255, 100, 0));
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(-18, -18, 36, 36);
        } else {
            // Crystal
            g2.setColor(new Color(150, 0, 255, 150));
            Path2D p = new Path2D.Double();
            p.moveTo(0, -35);
            p.lineTo(25, 0);
            p.lineTo(0, 35);
            p.lineTo(-25, 0);
            p.closePath();
            g2.fill(p);
            g2.setColor(new Color(220, 150, 255));
            g2.setStroke(new BasicStroke(3));
            g2.draw(p);
        }
        g2.setTransform(old);
    }

    private void drawPowerUp(Graphics2D g2, double x, double y) {
        double pulse = 1 + Math.sin(waveTime * 4) * 0.15;
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.scale(pulse, pulse);

        // Rotating glow
        for (int i = 6; i > 0; i--) {
            g2.setColor(new Color(100, 255, 150, 25));
            g2.fillOval(-i * 5, -i * 5, i * 10, i * 10);
        }
        g2.setColor(new Color(120, 255, 150));
        g2.fillOval(-15, -15, 30, 30);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(-8, 0, 8, 0);
        g2.drawLine(0, -8, 0, 8);

        g2.setTransform(old);
    }

    private void drawHUD(Graphics2D g2) {
        // Race progress bars
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));

        // P1
        g2.setColor(Color.WHITE);
        g2.drawString("P1", 30, 30);
        drawProgressBar(g2, 60, 20, 300, 16, p1Distance / TRACK_LENGTH, new Color(80, 180, 255));
        drawBoostBar(g2, 60, 44, 200, 8, p1BoostFuel / 100.0, new Color(0, 200, 255));

        // P2
        g2.drawString(mode == Mode.VS_COMPUTER ? "CPU" : "P2", 30, 80);
        drawProgressBar(g2, 60, 70, 300, 16, p2Distance / TRACK_LENGTH, new Color(255, 100, 100));
        drawBoostBar(g2, 60, 94, 200, 8, p2BoostFuel / 100.0, new Color(255, 100, 100));

        // Distance indicator
        g2.setColor(new Color(255, 255, 255, 200));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString(String.format("Distance: %.0f / %d", p1Distance, TRACK_LENGTH), WIDTH - 220, 30);
    }

    private void drawProgressBar(Graphics2D g2, int x, int y, int w, int h, double progress, Color color) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(x, y, w, h, h, h);
        g2.setColor(color);
        g2.fillRoundRect(x, y, (int)(w * Math.min(1, progress)), h, h, h);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, w, h, h, h);
    }

    private void drawBoostBar(Graphics2D g2, int x, int y, int w, int h, double progress, Color color) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(x, y, w, h);
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 200));
        g2.fillRect(x, y, (int)(w * Math.min(1, progress)), h);
    }

    private void drawMenu(Graphics2D g2) {
        // Background
        GradientPaint sky = new GradientPaint(0, 0, new Color(20, 10, 60),
                0, HEIGHT, new Color(120, 30, 120));
        g2.setPaint(sky);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Animated grid
        g2.setColor(new Color(80, 0, 150, 120));
        for (int i = 0; i < 20; i++) {
            int x = (int)((i * 60 + waveTime * 30) % (WIDTH + 60)) - 60;
            g2.drawLine(x, HEIGHT / 2, x - 200, HEIGHT);
        }

        // Title
        g2.setFont(new Font("SansSerif", Font.BOLD, 56));
        String title = "HOVERBOARD";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (WIDTH - fm.stringWidth(title)) / 2;
        GradientPaint titleGrad = new GradientPaint(0, 100, new Color(0, 255, 255),
                WIDTH, 160, new Color(255, 0, 255));
        g2.setPaint(titleGrad);
        g2.drawString(title, tx, 180);

        g2.setFont(new Font("SansSerif", Font.BOLD, 40));
        String title2 = "RACING ADVENTURE";
        fm = g2.getFontMetrics();
        tx = (WIDTH - fm.stringWidth(title2)) / 2;
        g2.drawString(title2, tx, 240);

        // Options
        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        String[] options = {
                "1  -  Race vs Computer",
                "2  -  Race vs Player 2",
                "",
                "Controls:",
                "P1: W A S D to move, SPACE to boost",
                "P2: ARROW KEYS to move, SHIFT to boost",
                "",
                "Collect green power-ups to refill boost!",
                "Avoid neon obstacles!"
        };
        g2.setColor(Color.WHITE);
        for (int i = 0; i < options.length; i++) {
            fm = g2.getFontMetrics();
            int w = fm.stringWidth(options[i]);
            g2.drawString(options[i], (WIDTH - w) / 2, 320 + i * 35);
        }

        // Decorative hoverboards
        drawHoverboard(g2, 150, HEIGHT - 100, Math.sin(waveTime) * 0.2,
                new Color(80, 180, 255), new Color(0, 80, 200), 1);
        drawHoverboard(g2, WIDTH - 150, HEIGHT - 100, -Math.sin(waveTime) * 0.2,
                new Color(255, 100, 100), new Color(200, 30, 30), 2);
    }

    private void drawFinish(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setFont(new Font("SansSerif", Font.BOLD, 64));
        String msg;
        Color c;
        if (winner == 1) {
            msg = "PLAYER 1 WINS!";
            c = new Color(100, 200, 255);
        } else {
            msg = mode == Mode.VS_COMPUTER ? "COMPUTER WINS!" : "PLAYER 2 WINS!";
            c = new Color(255, 120, 120);
        }
        FontMetrics fm = g2.getFontMetrics();
        int tx = (WIDTH - fm.stringWidth(msg)) / 2;

        // Glow effect
        for (int i = 6; i > 0; i--) {
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 25));
            g2.drawString(msg, tx - i, HEIGHT / 2 - 40 + i);
            g2.drawString(msg, tx + i, HEIGHT / 2 - 40 + i);
        }
        g2.setColor(c);
        g2.drawString(msg, tx, HEIGHT / 2 - 40);

        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2.setColor(Color.WHITE);
        String restart = "Press ENTER to play again, ESC for menu";
        fm = g2.getFontMetrics();
        tx = (WIDTH - fm.stringWidth(restart)) / 2;
        g2.drawString(restart, tx, HEIGHT / 2 + 60);
    }

    // ---- Key handling ----
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();

        if (state == State.MENU) {
            if (k == KeyEvent.VK_1) {
                mode = Mode.VS_COMPUTER;
                reset();
                state = State.RACING;
            } else if (k == KeyEvent.VK_2) {
                mode = Mode.VS_PLAYER;
                reset();
                state = State.RACING;
            }
            return;
        }

        if (state == State.FINISHED) {
            if (k == KeyEvent.VK_ENTER) {
                reset();
                state = State.RACING;
            } else if (k == KeyEvent.VK_ESCAPE) {
                state = State.MENU;
            }
            return;
        }

        // Racing controls
        switch (k) {
            case KeyEvent.VK_W: p1Up = true; break;
            case KeyEvent.VK_S: p1Down = true; break;
            case KeyEvent.VK_A: p1Left = true; break;
            case KeyEvent.VK_D: p1Right = true; break;
            case KeyEvent.VK_SPACE: p1Boost = true; break;

            case KeyEvent.VK_UP: p2Up = true; break;
            case KeyEvent.VK_DOWN: p2Down = true; break;
            case KeyEvent.VK_LEFT: p2Left = true; break;
            case KeyEvent.VK_RIGHT: p2Right = true; break;
            case KeyEvent.VK_SHIFT: p2Boost = true; break;

            case KeyEvent.VK_ESCAPE: state = State.MENU; break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        switch (k) {
            case KeyEvent.VK_W: p1Up = false; break;
            case KeyEvent.VK_S: p1Down = false; break;
            case KeyEvent.VK_A: p1Left = false; break;
            case KeyEvent.VK_D: p1Right = false; break;
            case KeyEvent.VK_SPACE: p1Boost = false; break;

            case KeyEvent.VK_UP: p2Up = false; break;
            case KeyEvent.VK_DOWN: p2Down = false; break;
            case KeyEvent.VK_LEFT: p2Left = false; break;
            case KeyEvent.VK_RIGHT: p2Right = false; break;
            case KeyEvent.VK_SHIFT: p2Boost = false; break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // ---- Inner classes ----
    static class Obstacle {
        double distance, y;
        int type;
        int size = 60;
        boolean hit1 = false, hit2 = false;
        Obstacle(double d, double y, int t) { this.distance = d; this.y = y; this.type = t; }
    }

    static class PowerUp {
        double distance, y;
        boolean collected = false;
        PowerUp(double d, double y) { this.distance = d; this.y = y; }
    }

    static class Particle {
        double x, y, vx, vy;
        Color color;
        int life;
        Particle(double x, double y, double vx, double vy, Color c, int life) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.color = c; this.life = life;
        }
    }

    static class Trail {
        double x, y, life = 1;
        Color color;
        Trail(double x, double y, Color c) { this.x = x; this.y = y; this.color = c; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hoverboard Racing Adventure");
            HoverboardRacingAdventure game = new HoverboardRacingAdventure();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}