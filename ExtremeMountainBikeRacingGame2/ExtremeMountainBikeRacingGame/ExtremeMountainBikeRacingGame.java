import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Extreme Mountain Bike Racing
 * A side-scrolling downhill/uphill mountain bike race with procedural terrain,
 * physics-based bike animation, dust particles and a parallax mountain backdrop.
 *
 * Modes:
 *   1  - Race vs Computer
 *   2  - Two Player (same keyboard)
 *
 * Controls:
 *   Player 1 : D = accelerate, A = brake,  W = jump/lean back, S = lean forward
 *   Player 2 : RIGHT = accelerate, LEFT = brake, UP = jump/lean back, DOWN = lean forward
 *   ESC      : back to menu    R (on results screen) : race again
 */
public class ExtremeMountainBikeRacingGame extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ExtremeMountainBikeRacingGame frame = new ExtremeMountainBikeRacingGame();
            frame.setVisible(true);
        });
    }

    public ExtremeMountainBikeRacingGame() {
        setTitle("Extreme Mountain Bike Racing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);
        panel.requestFocusInWindow();
    }
}

/* ===================== ENUMS ===================== */

enum GState { MENU, COUNTDOWN, RACING, FINISHED }
enum Mode { VS_COMPUTER, TWO_PLAYER }

/* ===================== INPUT ===================== */

class InputState {
    boolean accelerate, brake, leanBack, leanForward;
}

/* ===================== PARTICLE ===================== */

class Particle {
    double x, y, vx, vy, life, maxLife, radius;
    Color color;

    Particle(double x, double y, double vx, double vy, double life, double radius, Color color) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.life = life; this.maxLife = life; this.radius = radius; this.color = color;
    }

    void update(double dt) {
        x += vx * dt;
        y += vy * dt;
        vy += 200 * dt;
        vx *= (1 - 0.8 * dt);
        life -= dt;
    }
}

/* ===================== DECORATION ===================== */

class Decoration {
    double x;
    int type; // 0 = tree, 1 = rock, 2 = bush
    double scale;
}

/* ===================== TERRAIN ===================== */

class Terrain {
    final double trackLength;
    final List<double[]> ramps = new ArrayList<>(); // {x, width, height}
    static final double BASE = 430;

    Terrain(double length, long seed) {
        this.trackLength = length;
        Random r = new Random(seed);
        double x = 700;
        while (x < length - 400) {
            double width = 45 + r.nextDouble() * 40;
            double height = 18 + r.nextDouble() * 42;
            ramps.add(new double[]{x, width, height});
            x += 260 + r.nextDouble() * 220;
        }
    }

    double groundY(double x) {
        double h = BASE;
        h -= 45 * Math.sin(x * 0.0035 + 1.0);
        h -= 25 * Math.sin(x * 0.011 + 2.3);
        h -= 14 * Math.sin(x * 0.023 + 0.7);
        h -= 7 * Math.sin(x * 0.05 + 4.0);
        for (double[] r : ramps) {
            double d = x - r[0];
            if (Math.abs(d) < r[1]) {
                h -= r[2] * (1 - Math.abs(d) / r[1]);
            }
        }
        if (x < 260) {
            double t = Math.max(0, x) / 260.0;
            h = h * t + BASE * (1 - t);
        }
        return h;
    }

    double slope(double x) {
        double h1 = groundY(x - 1.0);
        double h2 = groundY(x + 1.0);
        return (h2 - h1) / 2.0;
    }

    List<Decoration> generateDecorations(Random r) {
        List<Decoration> list = new ArrayList<>();
        double x = 150;
        while (x < trackLength - 100) {
            if (r.nextDouble() < 0.55) {
                Decoration d = new Decoration();
                d.x = x;
                d.type = r.nextInt(3);
                d.scale = 0.7 + r.nextDouble() * 0.9;
                list.add(d);
            }
            x += 90 + r.nextDouble() * 170;
        }
        return list;
    }
}

/* ===================== BIKE ===================== */

class Bike {
    static final double WHEEL_RADIUS = 15;
    static final double WHEELBASE = 34;
    static final double GRAVITY = 1400;

    double x, y, vx = 0, vy = 0, angle = 0, wheelRotation = 0;
    boolean grounded = true;
    boolean finished = false;
    Double finishTime = null;
    final String name;
    final Color color;
    final boolean isAI;
    double aiBoost = 1.0;

    Bike(double startX, String name, Color color, boolean isAI) {
        this.x = startX;
        this.name = name;
        this.color = color;
        this.isAI = isAI;
    }

    void update(double dt, Terrain terrain, InputState input, List<Particle> particles, Random rnd) {
        double groundY = terrain.groundY(x);
        double slope = terrain.slope(x);
        double slopeAngle = Math.atan(slope);

        if (y >= groundY - 1.0) {
            boolean wasAirborne = !grounded;
            grounded = true;
            y = groundY;
            vy = 0;

            double accelMult = 1.0 - Math.max(0, -slope) * 0.5;
            if (accelMult < 0.3) accelMult = 0.3;

            if (input.accelerate) vx += 340 * accelMult * aiBoost * dt;
            if (input.brake) vx -= 420 * dt;
            vx += slope * 260 * dt;
            vx -= vx * 0.6 * dt;

            if (vx > 520) vx = 520;
            if (vx < -140) vx = -140;

            x += vx * dt;
            if (x < 0) { x = 0; if (vx < 0) vx = 0; }

            double targetAngle = slopeAngle;
            angle += (targetAngle - angle) * Math.min(1, 10 * dt);

            if (wasAirborne && Math.abs(vx) > 60) spawnDust(particles, rnd, 9);

            if (input.leanBack && vx > 90) {
                vy = -420;
                grounded = false;
                spawnDust(particles, rnd, 6);
            } else if (Math.abs(vx) > 60 && (input.accelerate || input.brake) && rnd.nextDouble() < 0.55) {
                spawnDust(particles, rnd, 1);
            }
        } else {
            grounded = false;
            vy += GRAVITY * dt;
            y += vy * dt;
            x += vx * dt;

            if (input.leanBack) angle -= 2.6 * dt;
            if (input.leanForward) angle += 2.6 * dt;

            double gy = terrain.groundY(x);
            if (y >= gy) { y = gy; vy = 0; }
        }

        wheelRotation += vx * dt / WHEEL_RADIUS;
    }

    private void spawnDust(List<Particle> particles, Random rnd, int count) {
        double dir = vx >= 0 ? 1 : -1;
        for (int i = 0; i < count; i++) {
            double speed = 40 + rnd.nextDouble() * 70;
            double dx = -dir * speed * (0.4 + rnd.nextDouble() * 0.6);
            double dy = -30 - rnd.nextDouble() * 40;
            particles.add(new Particle(
                    x - dir * WHEELBASE * 0.4, y - 4,
                    dx, dy,
                    0.45 + rnd.nextDouble() * 0.3,
                    2 + rnd.nextDouble() * 3,
                    new Color(150, 120, 90)));
        }
    }
}

/* ===================== GAME PANEL ===================== */

class GamePanel extends JPanel implements ActionListener, KeyListener {

    static final int WIDTH = 1100, HEIGHT = 650;
    static final double TRACK_LENGTH = 6000;

    GState state = GState.MENU;
    Mode mode = Mode.VS_COMPUTER;

    Terrain terrain;
    Bike p1, p2;
    final List<Particle> particles = new ArrayList<>();
    List<Decoration> decorations = new ArrayList<>();
    final Set<Integer> keysDown = new HashSet<>();

    double cameraX = 0;
    double countdownTimer = 0;
    double raceClock = 0;
    double cloudOffset = 0;
    int finishCount = 0;

    final Random rnd = new Random();

    GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        javax.swing.Timer timer = new javax.swing.Timer(16, this);
        timer.start();
    }

    void newRace(Mode m) {
        mode = m;
        terrain = new Terrain(TRACK_LENGTH, System.nanoTime());
        decorations = terrain.generateDecorations(rnd);
        p1 = new Bike(60, "PLAYER 1", new Color(220, 60, 60), false);
        p2 = new Bike(30, mode == Mode.TWO_PLAYER ? "PLAYER 2" : "COMPUTER",
                new Color(60, 130, 230), mode == Mode.VS_COMPUTER);
        p1.y = terrain.groundY(p1.x);
        p2.y = terrain.groundY(p2.x);
        particles.clear();
        cameraX = 0;
        raceClock = 0;
        finishCount = 0;
        countdownTimer = 3.999;
        state = GState.COUNTDOWN;
    }

    /* ---------- update loop ---------- */

    public void actionPerformed(ActionEvent e) {
        update(0.016);
        repaint();
    }

    void update(double dt) {
        cloudOffset += dt * 8;
        switch (state) {
            case MENU:
                break;
            case COUNTDOWN:
                countdownTimer -= dt;
                if (countdownTimer <= 0) state = GState.RACING;
                break;
            case RACING:
                updateRacing(dt);
                break;
            case FINISHED:
                break;
        }
    }

    void updateRacing(double dt) {
        raceClock += dt;

        InputState in1 = new InputState();
        in1.accelerate = keysDown.contains(KeyEvent.VK_D);
        in1.brake = keysDown.contains(KeyEvent.VK_A);
        in1.leanBack = keysDown.contains(KeyEvent.VK_W);
        in1.leanForward = keysDown.contains(KeyEvent.VK_S);
        p1.update(dt, terrain, in1, particles, rnd);

        InputState in2;
        if (mode == Mode.TWO_PLAYER) {
            in2 = new InputState();
            in2.accelerate = keysDown.contains(KeyEvent.VK_RIGHT);
            in2.brake = keysDown.contains(KeyEvent.VK_LEFT);
            in2.leanBack = keysDown.contains(KeyEvent.VK_UP);
            in2.leanForward = keysDown.contains(KeyEvent.VK_DOWN);
        } else {
            in2 = computeAI(p2, p1);
        }
        p2.update(dt, terrain, in2, particles, rnd);

        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle pt = it.next();
            pt.update(dt);
            if (pt.life <= 0) it.remove();
        }

        double leaderX = Math.max(p1.x, p2.x);
        double desiredCam = leaderX - WIDTH * 0.35;
        desiredCam = Math.max(0, Math.min(desiredCam, terrain.trackLength - WIDTH + 200));
        cameraX += (desiredCam - cameraX) * Math.min(1, 6 * dt);

        if (!p1.finished && p1.x >= terrain.trackLength) { p1.finished = true; p1.finishTime = raceClock; finishCount++; }
        if (!p2.finished && p2.x >= terrain.trackLength) { p2.finished = true; p2.finishTime = raceClock; finishCount++; }
        if (p1.finished && p2.finished) state = GState.FINISHED;
    }

    InputState computeAI(Bike ai, Bike player) {
        InputState in = new InputState();
        double diff = player.x - ai.x;
        double rubberBand = 1.0 + Math.max(-0.25, Math.min(0.55, diff / 900.0 * 0.4));
        ai.aiBoost = rubberBand;
        in.accelerate = true;
        double aheadRise = terrain.groundY(ai.x + 60) - terrain.groundY(ai.x);
        in.leanBack = ai.grounded && ai.vx > 140 && aheadRise < -12 && rnd.nextDouble() < 0.12;
        in.brake = false;
        return in;
    }

    /* ---------- rendering ---------- */

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawSky(g2);

        if (state == GState.MENU) {
            drawMenu(g2);
            return;
        }

        drawClouds(g2);
        drawMountains(g2, 0.25, 130, new Color(90, 95, 130, 180));
        drawMountains(g2, 0.5, 90, new Color(60, 65, 95, 200));
        drawTerrain(g2);
        drawDecorations(g2);
        drawFinishFlag(g2);
        drawParticles(g2);

        if (p1 != null && p2 != null) {
            drawShadow(g2, p1);
            drawShadow(g2, p2);
            drawBike(g2, p2);
            drawBike(g2, p1);
            drawHud(g2);
        }

        if (state == GState.COUNTDOWN) drawCountdown(g2);
        if (state == GState.FINISHED) drawFinished(g2);
    }

    void drawSky(Graphics2D g2) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 250), 0, HEIGHT, new Color(224, 246, 255));
        g2.setPaint(sky);
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        RadialGradientPaint sun = new RadialGradientPaint(new Point(920, 110), 90,
                new float[]{0f, 1f}, new Color[]{new Color(255, 250, 200, 220), new Color(255, 250, 200, 0)});
        g2.setPaint(sun);
        g2.fillOval(920 - 90, 110 - 90, 180, 180);
        g2.setColor(new Color(255, 245, 180));
        g2.fillOval(890, 80, 60, 60);
    }

    void drawClouds(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 210));
        double factor = 0.1;
        for (int i = 0; i < 8; i++) {
            double baseX = i * 420 - 200;
            double sx = ((baseX - cameraX * factor - cloudOffset * 10) % 3400 + 3400) % 3400 - 300;
            double sy = 60 + (i % 3) * 45;
            drawCloud(g2, sx, sy, 1.0 + (i % 3) * 0.3);
        }
    }

    void drawCloud(Graphics2D g2, double x, double y, double s) {
        g2.fillOval((int) (x), (int) (y), (int) (60 * s), (int) (28 * s));
        g2.fillOval((int) (x + 25 * s), (int) (y - 12 * s), (int) (50 * s), (int) (34 * s));
        g2.fillOval((int) (x + 55 * s), (int) (y), (int) (45 * s), (int) (26 * s));
    }

    void drawMountains(Graphics2D g2, double factor, double amp, Color color) {
        g2.setColor(color);
        Path2D path = new Path2D.Double();
        path.moveTo(0, HEIGHT);
        int step = 20;
        for (int sx = 0; sx <= WIDTH; sx += step) {
            double wx = cameraX * factor + sx;
            double y = 380 - amp * Math.abs(Math.sin(wx * 0.0025 + factor * 10)) - amp * 0.4 * Math.sin(wx * 0.0009);
            path.lineTo(sx, y);
        }
        path.lineTo(WIDTH, HEIGHT);
        path.closePath();
        g2.fill(path);
    }

    void drawTerrain(Graphics2D g2) {
        Path2D path = new Path2D.Double();
        path.moveTo(0, HEIGHT);
        int step = 6;
        double[] ys = new double[WIDTH / step + 2];
        int idx = 0;
        for (int sx = 0; sx <= WIDTH; sx += step) {
            double wx = cameraX + sx;
            double y = terrain.groundY(wx);
            ys[idx++] = y;
            path.lineTo(sx, y);
        }
        path.lineTo(WIDTH, HEIGHT);
        path.closePath();
        GradientPaint dirt = new GradientPaint(0, 300, new Color(120, 84, 52), 0, HEIGHT, new Color(70, 48, 30));
        g2.setPaint(dirt);
        g2.fill(path);

        g2.setColor(new Color(90, 180, 90));
        g2.setStroke(new BasicStroke(5));
        idx = 0;
        Path2D grass = new Path2D.Double();
        boolean started = false;
        for (int sx = 0; sx <= WIDTH; sx += step) {
            double y = ys[idx++];
            if (!started) { grass.moveTo(sx, y); started = true; }
            else grass.lineTo(sx, y);
        }
        g2.draw(grass);
    }

    void drawDecorations(Graphics2D g2) {
        for (Decoration d : decorations) {
            double sx = d.x - cameraX;
            if (sx < -60 || sx > WIDTH + 60) continue;
            double gy = terrain.groundY(d.x);
            switch (d.type) {
                case 0: drawTree(g2, sx, gy, d.scale); break;
                case 1: drawRock(g2, sx, gy, d.scale); break;
                default: drawBush(g2, sx, gy, d.scale); break;
            }
        }
    }

    void drawTree(Graphics2D g2, double x, double y, double s) {
        g2.setColor(new Color(101, 67, 33));
        g2.fillRect((int) (x - 3 * s), (int) (y - 22 * s), (int) (6 * s), (int) (22 * s));
        g2.setColor(new Color(34, 110, 46));
        g2.fillOval((int) (x - 20 * s), (int) (y - 55 * s), (int) (40 * s), (int) (38 * s));
        g2.setColor(new Color(46, 130, 60));
        g2.fillOval((int) (x - 15 * s), (int) (y - 45 * s), (int) (30 * s), (int) (28 * s));
    }

    void drawRock(Graphics2D g2, double x, double y, double s) {
        g2.setColor(new Color(120, 120, 120));
        g2.fillOval((int) (x - 14 * s), (int) (y - 14 * s), (int) (28 * s), (int) (16 * s));
        g2.setColor(new Color(150, 150, 150));
        g2.fillOval((int) (x - 10 * s), (int) (y - 16 * s), (int) (14 * s), (int) (10 * s));
    }

    void drawBush(Graphics2D g2, double x, double y, double s) {
        g2.setColor(new Color(52, 140, 66));
        g2.fillOval((int) (x - 14 * s), (int) (y - 12 * s), (int) (28 * s), (int) (16 * s));
        g2.fillOval((int) (x - 6 * s), (int) (y - 18 * s), (int) (20 * s), (int) (16 * s));
    }

    void drawFinishFlag(Graphics2D g2) {
        double sx = terrain.trackLength - cameraX;
        if (sx < -50 || sx > WIDTH + 50) return;
        double gy = terrain.groundY(terrain.trackLength);
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect((int) sx - 2, (int) gy - 90, 4, 90);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                g2.setColor((row + col) % 2 == 0 ? Color.BLACK : Color.WHITE);
                g2.fillRect((int) sx + 2 + col * 8, (int) gy - 90 + row * 8, 8, 8);
            }
        }
    }

    void drawParticles(Graphics2D g2) {
        for (Particle p : particles) {
            double sx = p.x - cameraX;
            float alpha = (float) Math.max(0, Math.min(1, p.life / p.maxLife));
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int) (alpha * 180)));
            double r = p.radius;
            g2.fill(new Ellipse2D.Double(sx - r, p.y - r, r * 2, r * 2));
        }
    }

    void drawShadow(Graphics2D g2, Bike b) {
        double gy = terrain.groundY(b.x);
        double sx = b.x - cameraX;
        double heightAbove = Math.max(0, gy - b.y);
        double scale = Math.max(0.25, 1 - heightAbove / 150.0);
        int alpha = (int) Math.max(30, 110 * scale);
        g2.setColor(new Color(0, 0, 0, alpha));
        g2.fill(new Ellipse2D.Double(sx - 18 * scale, gy - 3, 36 * scale, 7 * scale));
    }

    void drawWheel(Graphics2D g2, double cx, double cy, double r, double rotation, Color color) {
        g2.setColor(new Color(30, 30, 30));
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g2.setColor(new Color(200, 200, 200));
        g2.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 6; i++) {
            double a = rotation + i * Math.PI / 3;
            double x1 = cx + Math.cos(a) * 3, y1 = cy + Math.sin(a) * 3;
            double x2 = cx + Math.cos(a) * (r - 2), y2 = cy + Math.sin(a) * (r - 2);
            g2.draw(new Line2D.Double(x1, y1, x2, y2));
        }
        g2.setColor(color);
        g2.fill(new Ellipse2D.Double(cx - 3, cy - 3, 6, 6));
    }

    void drawBike(Graphics2D g2, Bike b) {
        double sx = b.x - cameraX;
        if (sx < -80 || sx > WIDTH + 80) return;

        if (Math.abs(b.vx) > 360) {
            g2.setColor(new Color(255, 255, 255, 90));
            g2.setStroke(new BasicStroke(2));
            double dir = b.vx >= 0 ? -1 : 1;
            for (int i = 1; i <= 3; i++) {
                g2.draw(new Line2D.Double(sx + dir * 20 * i, b.y - 10, sx + dir * (20 * i + 14), b.y - 10));
            }
        }

        AffineTransform old = g2.getTransform();
        g2.translate(sx, b.y);
        g2.rotate(b.angle);

        double wb = Bike.WHEELBASE;
        double r = Bike.WHEEL_RADIUS;

        drawWheel(g2, -wb / 2, 0, r, b.wheelRotation, b.color);
        drawWheel(g2, wb / 2, 0, r, b.wheelRotation, b.color);

        g2.setColor(b.color);
        g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(-wb / 2, 0, 0, -26));
        g2.draw(new Line2D.Double(0, -26, wb / 2, 0));
        g2.draw(new Line2D.Double(0, -26, 10, -32));
        g2.draw(new Line2D.Double(10, -32, wb / 2 - 2, -6));
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(10, -32, 16, -40));

        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(b.color.darker());
        g2.draw(new Line2D.Double(-2, -24, 8, -44));
        g2.draw(new Line2D.Double(8, -44, 16, -38));
        g2.draw(new Line2D.Double(-2, -24, -12, -8));

        g2.setColor(Color.BLACK);
        g2.fill(new Ellipse2D.Double(8 - 6, -44 - 11, 13, 13));

        g2.setTransform(old);
    }

    void drawHud(Graphics2D g2) {
        g2.setColor(new Color(20, 20, 30, 160));
        g2.fillRoundRect(10, 10, 300, 74, 12, 12);
        g2.fillRoundRect(WIDTH - 310, 10, 300, 74, 12, 12);

        drawPlayerHud(g2, p1, 10);
        drawPlayerHud(g2, p2, WIDTH - 310);

        if (state == GState.RACING) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 20));
            String t = String.format("%.1fs", raceClock);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(t, WIDTH / 2 - fm.stringWidth(t) / 2, 32);
        }
    }

    void drawPlayerHud(Graphics2D g2, Bike b, int boxX) {
        g2.setColor(b.color);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString(b.name, boxX + 14, boxX == 10 ? 28 : 28);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        int speed = (int) Math.max(0, b.vx * 0.6);
        g2.drawString(speed + " km/h", boxX + 14, 46);

        double progress = Math.max(0, Math.min(1, b.x / terrain.trackLength));
        g2.setColor(new Color(255, 255, 255, 60));
        g2.fillRoundRect(boxX + 14, 56, 272, 10, 6, 6);
        g2.setColor(b.color);
        g2.fillRoundRect(boxX + 14, 56, (int) (272 * progress), 10, 6, 6);
    }

    void drawCentered(Graphics2D g2, String s, int y, Font f, Color c) {
        g2.setFont(f);
        g2.setColor(c);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, WIDTH / 2 - fm.stringWidth(s) / 2, y);
    }

    void drawMenu(Graphics2D g2) {
        drawMountains(g2, 0.25, 130, new Color(90, 95, 130, 180));
        drawMountains(g2, 0.5, 90, new Color(60, 65, 95, 200));

        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        drawCentered(g2, "EXTREME MOUNTAIN BIKE RACING", 220, new Font("SansSerif", Font.BOLD, 44), new Color(255, 210, 60));
        drawCentered(g2, "Conquer the hills. Beat your rival.", 260, new Font("SansSerif", Font.ITALIC, 18), Color.WHITE);

        drawCentered(g2, "Press 1  -  Race vs Computer", 350, new Font("SansSerif", Font.BOLD, 26), Color.WHITE);
        drawCentered(g2, "Press 2  -  Two Players", 390, new Font("SansSerif", Font.BOLD, 26), Color.WHITE);

        drawCentered(g2, "P1: D accelerate / A brake / W jump / S tuck", 460, new Font("SansSerif", Font.PLAIN, 16), new Color(220, 60, 60));
        drawCentered(g2, "P2: RIGHT accelerate / LEFT brake / UP jump / DOWN tuck", 486, new Font("SansSerif", Font.PLAIN, 16), new Color(60, 130, 230));
        drawCentered(g2, "ESC returns to this menu at any time", 520, new Font("SansSerif", Font.PLAIN, 14), Color.LIGHT_GRAY);
    }

    void drawCountdown(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        int n = (int) Math.ceil(countdownTimer);
        String txt = n > 0 ? String.valueOf(n) : "GO!";
        double frac = countdownTimer - Math.floor(countdownTimer);
        float scale = (float) (1.6 - 0.6 * frac);
        Font f = new Font("SansSerif", Font.BOLD, (int) (90 * scale));
        drawCentered(g2, txt, HEIGHT / 2, f, new Color(255, 230, 80));
    }

    void drawFinished(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        String winner;
        if (p1.finishTime != null && p2.finishTime != null) {
            winner = p1.finishTime <= p2.finishTime ? p1.name + " WINS!" : p2.name + " WINS!";
        } else {
            winner = "RACE COMPLETE";
        }
        drawCentered(g2, winner, HEIGHT / 2 - 60, new Font("SansSerif", Font.BOLD, 42), new Color(255, 210, 60));
        drawCentered(g2, p1.name + ": " + fmt(p1.finishTime), HEIGHT / 2, new Font("SansSerif", Font.BOLD, 22), new Color(220, 60, 60));
        drawCentered(g2, p2.name + ": " + fmt(p2.finishTime), HEIGHT / 2 + 32, new Font("SansSerif", Font.BOLD, 22), new Color(60, 130, 230));
        drawCentered(g2, "Press R to race again  -  ESC for menu", HEIGHT / 2 + 90, new Font("SansSerif", Font.PLAIN, 16), Color.WHITE);
    }

    String fmt(Double t) {
        return t == null ? "--" : String.format("%.2fs", t);
    }

    /* ---------- input ---------- */

    public void keyPressed(KeyEvent e) {
        keysDown.add(e.getKeyCode());
        if (state == GState.MENU) {
            if (e.getKeyCode() == KeyEvent.VK_1) newRace(Mode.VS_COMPUTER);
            if (e.getKeyCode() == KeyEvent.VK_2) newRace(Mode.TWO_PLAYER);
        } else if (state == GState.FINISHED) {
            if (e.getKeyCode() == KeyEvent.VK_R) newRace(mode);
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) state = GState.MENU;
        } else {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) state = GState.MENU;
        }
    }

    public void keyReleased(KeyEvent e) {
        keysDown.remove(e.getKeyCode());
    }

    public void keyTyped(KeyEvent e) { }
}
