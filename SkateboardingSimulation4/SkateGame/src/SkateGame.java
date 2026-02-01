import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.sound.sampled.*;
import javax.swing.Timer;
import java.io.ByteArrayInputStream;

public class SkateGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Skateboarding Simulation - SkateGame");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            GamePanel panel = new GamePanel();
            f.setContentPane(panel);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            panel.start();
        });
    }
}

/**
 * Main panel hosting the game loop, input, and rendering.
 */
class GamePanel extends JPanel implements ActionListener, KeyListener, MouseWheelListener {
    // Logical resolution
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    private Timer timer;
    private long lastNano;
    private double accumulator; // not used for fixed-step; we run variable step with clamp

    private Level level;
    private Player player;
    private Camera camera;
    private HUD hud;
    private ParticleSystem particleSystem;
    private Input input;
    private ReplayGhost ghost;
    private Sound sound;
    private Random rng = new Random(42);

    private boolean paused = false;
    private boolean debug = false;

    private double timeScale = 1.0;

    private int score = 0;
    private int combo = 0;
    private String comboLabel = "";
    private double comboTimer = 0; // seconds to maintain combo chain

    private double elapsedSeconds = 0;

    private int frameCount = 0;
    private int fps = 0;
    private double fpsTimer = 0;

    private GameState state = GameState.RUNNING;

    private ParallaxBackground background;

    private JLabel helpLabel;

    enum GameState {
        RUNNING, PAUSED
    }

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(19, 24, 31));
        setFocusable(true);
        requestFocusInWindow();
        addKeyListener(this);
        addMouseWheelListener(this);

        input = new Input();
        hud = new HUD(this);
        particleSystem = new ParticleSystem();
        level = new Level();
        camera = new Camera();
        player = new Player(level, particleSystem);
        ghost = new ReplayGhost(2000, new Color(255, 255, 255, 40));
        sound = new Sound();
        background = new ParallaxBackground();

        // Initial camera setup
        camera.setTarget(() -> new Point2D.Double(player.pos.x, player.pos.y));
        camera.setScreenSize(WIDTH, HEIGHT);
        camera.setMode(Camera.Mode.FOLLOW);

        helpLabel = new JLabel("Controls: Left/Right=move, Up=ollie, Down=crouch, Space=trick/manual, P=pause, R=restart, 1/2/3=camera, +/-=time scale, F1=debug");
        helpLabel.setForeground(Color.LIGHT_GRAY);
        helpLabel.setFont(helpLabel.getFont().deriveFont(Font.PLAIN, 12f));
    }

    public void start() {
        lastNano = System.nanoTime();
        timer = new Timer(1000 / 60, this);
        timer.start();
    }

    public void restart() {
        score = 0;
        combo = 0;
        comboLabel = "";
        comboTimer = 0;
        elapsedSeconds = 0;
        particleSystem.clear();
        player.reset(level);
        ghost.clear();
        camera.reset();
        background.reset();
        // randomize level or change seed
        level = new Level(); // new world
        player.level = level;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = (now - lastNano) / 1e9;
        lastNano = now;

        // Clamp delta to avoid jumps due to pause/breakpoints
        dt = Math.min(dt, 1.0 / 15.0); // max 1/15 sec
        dt *= timeScale;

        if (!paused && state == GameState.RUNNING) {
            update(dt);
        }

        repaint();
    }

    private void update(double dt) {
        elapsedSeconds += dt;

        // Input to player
        player.handleInput(input, dt);

        // Update player physics
        player.update(dt);

        // Update camera
        camera.update(dt);

        // Update background layers based on camera
        background.update(dt, camera);

        // Particle system
        particleSystem.update(dt);

        // Combo timer decay
        if (combo > 0) {
            comboTimer -= dt;
            if (comboTimer <= 0) {
                combo = 0;
                comboLabel = "";
            }
        }

        // Score: distance traveled reward
        score += (int)(player.vel.x * dt * 0.2);

        // replay ghost
        ghost.record(player.pos.x, player.pos.y);

        // FPS estimate
        fpsTimer += dt;
        frameCount++;
        if (fpsTimer >= 1.0) {
            fps = frameCount;
            frameCount = 0;
            fpsTimer -= 1.0;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Setup Graphics2D
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Clear with background gradient
        drawSkyGradient(g2);

        // Camera transform for world rendering
        AffineTransform old = g2.getTransform();
        g2.translate(-camera.screenX(), -camera.screenY());

        // Draw parallax background elements behind terrain
        background.draw(g2, camera);

        // Draw level terrain, rails, collectibles
        level.draw(g2, camera);

        // Draw particles behind player (dust trails)
        particleSystem.draw(g2, Particle.Layer.BACK);

        // Draw replay ghost
        ghost.draw(g2, camera);

        // Draw player
        player.draw(g2, camera, debug);

        // Draw particles above player (sparks)
        particleSystem.draw(g2, Particle.Layer.FRONT);

        // Debug overlay in world space
        if (debug) {
            drawDebugWorld(g2);
        }

        // Reset to screen space
        g2.setTransform(old);

        // HUD in screen space
        hud.draw(g2, this, player, score, combo, comboLabel, elapsedSeconds, fps, timeScale, paused, debug);

        // Help label
        paintHelp(g2);

        g2.dispose();
    }

    private void paintHelp(Graphics2D g2) {
        g2.setFont(new Font("Dialog", Font.PLAIN, 12));
        g2.setColor(new Color(255,255,255,180));
        String text = "Left/Right=move, Up=ollie, Down=crouch, Space=trick/manual, P=pause, R=restart, 1/2/3=camera, +/-=time scale, F1=debug";
        int pad = 8;
        int y = HEIGHT - 18;
        // Shadow
        g2.setColor(new Color(0,0,0,120));
        g2.drawString(text, pad+1, y+1);
        g2.setColor(new Color(255,255,255,200));
        g2.drawString(text, pad, y);
    }

    private void drawDebugWorld(Graphics2D g2) {
        // World grid
        g2.setColor(new Color(255, 255, 255, 20));
        for (int x = ((int)camera.screenX()/100)*100 - 5000; x < camera.screenX() + WIDTH + 5000; x += 100) {
            g2.drawLine(x, -10000, x, 10000);
        }
        for (int y = -10000; y < 10000; y += 100) {
            g2.drawLine((int)(camera.screenX()-5000), y, (int)(camera.screenX()+WIDTH+5000), y);
        }

        // Player collision box
        g2.setColor(Color.MAGENTA);
        Rectangle2D.Double aabb = player.getAABB();
        g2.draw(aabb);

        // Terrain debug
        level.drawDebug(g2);
    }

    private void drawSkyGradient(Graphics2D g2) {
        Color top = new Color(24, 31, 45);
        Color mid = new Color(49, 74, 104);
        Color bot = new Color(110, 154, 196);
        int h = HEIGHT;
        GradientPaint gp1 = new GradientPaint(0, 0, top, 0, h * 2 / 3, mid);
        g2.setPaint(gp1);
        g2.fillRect(0, 0, WIDTH, h);
        GradientPaint gp2 = new GradientPaint(0, h * 2 / 3, mid, 0, h, bot);
        g2.setPaint(gp2);
        g2.fillRect(0, h * 2 / 3, WIDTH, h / 3 + 1);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // not used
    }

    @Override
    public void keyPressed(KeyEvent e) {
        input.setKey(e.getKeyCode(), true);

        switch (e.getKeyCode()) {
            case KeyEvent.VK_P:
                paused = !paused;
                state = paused ? GameState.PAUSED : GameState.RUNNING;
                break;
            case KeyEvent.VK_R:
                restart();
                break;
            case KeyEvent.VK_1:
                camera.setMode(Camera.Mode.FOLLOW);
                break;
            case KeyEvent.VK_2:
                camera.setMode(Camera.Mode.LOOKAHEAD);
                break;
            case KeyEvent.VK_3:
                camera.setMode(Camera.Mode.CINEMATIC);
                break;
            case KeyEvent.VK_F1:
                debug = !debug;
                break;
            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_EQUALS:
                timeScale = Math.min(2.5, timeScale + 0.1);
                break;
            case KeyEvent.VK_MINUS:
            case KeyEvent.VK_UNDERSCORE:
                timeScale = Math.max(0.2, timeScale - 0.1);
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        input.setKey(e.getKeyCode(), false);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // Zoom camera on wheel (optional)
        double notches = e.getPreciseWheelRotation();
        double s = camera.getScale();
        s *= Math.pow(1.1, notches);
        s = Math.max(0.6, Math.min(1.6, s));
        camera.setScale(s);
    }

    // Callbacks from player to update score/combo
    public void onTrick(String trickName, int points) {
        combo++;
        comboLabel = trickName;
        comboTimer = 3.0; // 3 seconds to chain
        int comboMultiplier = Math.max(1, combo);
        score += points * comboMultiplier;
        // Play a beep for trick
        sound.beep(440 + Math.min(800, combo * 60), 0.05);
        // HUD pop
        hud.addPopup(trickName + " +" + (points * comboMultiplier), player.pos.x, player.pos.y - 60);
    }

    public void onCollectCoin(int points, double x, double y) {
        score += points;
        sound.beep(880, 0.04);
        hud.addPopup("+" + points, x, y);
    }
}

/**
 * Input state helper.
 */
class Input {
    private final Set<Integer> pressed = new HashSet<>();

    public void setKey(int keyCode, boolean down) {
        if (down) pressed.add(keyCode);
        else pressed.remove(keyCode);
    }

    public boolean left() { return pressed.contains(KeyEvent.VK_LEFT) || pressed.contains(KeyEvent.VK_A); }
    public boolean right() { return pressed.contains(KeyEvent.VK_RIGHT) || pressed.contains(KeyEvent.VK_D); }
    public boolean up() { return pressed.contains(KeyEvent.VK_UP) || pressed.contains(KeyEvent.VK_W); }
    public boolean down() { return pressed.contains(KeyEvent.VK_DOWN) || pressed.contains(KeyEvent.VK_S); }
    public boolean space() { return pressed.contains(KeyEvent.VK_SPACE); }
}

/**
 * 2D vector
 */
class Vec2 {
    public double x, y;
    public Vec2() { this(0, 0); }
    public Vec2(double x, double y) { this.x = x; this.y = y; }
    public Vec2 copy() { return new Vec2(x, y); }
    public Vec2 set(double nx, double ny) { x = nx; y = ny; return this; }
    public Vec2 add(Vec2 o) { x += o.x; y += o.y; return this; }
    public Vec2 add(double ax, double ay) { x += ax; y += ay; return this; }
    public Vec2 sub(Vec2 o) { x -= o.x; y -= o.y; return this; }
    public Vec2 scale(double s) { x *= s; y *= s; return this; }
    public double dot(Vec2 o) { return x * o.x + y * o.y; }
    public double len() { return Math.sqrt(x*x + y*y); }
    public Vec2 normalized() { double L = len(); return L > 1e-8 ? new Vec2(x/L, y/L) : new Vec2(); }
}

/**
 * Camera class for following the player smoothly, with modes and optional shake.
 */
class Camera {
    public enum Mode { FOLLOW, LOOKAHEAD, CINEMATIC }

    private double x, y;
    private double targetX, targetY;
    private double smooth = 0.12; // smoothing factor
    private int screenW = 1280, screenH = 720;
    private double scale = 1.0;

    private Mode mode = Mode.FOLLOW;

    private SupplierPoint targetSupplier;

    private double lookAhead = 170;
    private double lookAheadY = -40;

    private double shakeTime = 0;
    private double shakeIntensity = 0;

    interface SupplierPoint {
        Point2D.Double get();
    }

    public void setTarget(SupplierPoint supplier) {
        this.targetSupplier = supplier;
    }

    public void setScreenSize(int w, int h) {
        this.screenW = w;
        this.screenH = h;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() { return mode; }

    public void update(double dt) {
        if (targetSupplier == null) return;
        Point2D.Double p = targetSupplier.get();
        // compute desired center
        double dx = 0;
        double dy = 0;
        switch (mode) {
            case FOLLOW:
                dx = 0;
                dy = 0;
                break;
            case LOOKAHEAD:
                dx = lookAhead;
                dy = lookAheadY;
                break;
            case CINEMATIC:
                dx = lookAhead * 1.3;
                dy = lookAheadY * 0.6;
                break;
        }

        targetX = p.x + dx - screenW / 2.0 / scale;
        targetY = p.y + dy - screenH / 2.0 / scale;

        // Smoothly approach target
        x += (targetX - x) * smooth;
        y += (targetY - y) * smooth;

        // Shake decay
        if (shakeTime > 0) {
            shakeTime -= dt;
            if (shakeTime < 0) shakeTime = 0;
        }
    }

    public void shake(double intensity, double duration) {
        this.shakeIntensity = intensity;
        this.shakeTime = duration;
    }

    public double worldX() { return x; }
    public double worldY() { return y; }
    public double screenX() {
        double sx = x;
        if (shakeTime > 0) {
            sx += (Math.random() - 0.5) * shakeIntensity;
        }
        return sx;
    }
    public double screenY() {
        double sy = y;
        if (shakeTime > 0) {
            sy += (Math.random() - 0.5) * shakeIntensity;
        }
        return sy;
    }

    public void reset() {
        x = 0; y = 0; targetX = 0; targetY = 0; shakeTime = 0; shakeIntensity = 0;
    }

    public void setScale(double s) { this.scale = s; }
    public double getScale() { return scale; }
}

/**
 * Level holds terrain segments, rails, and collectibles; and draws them.
 */
class Level {
    public List<TerrainSegment> ground = new ArrayList<>();
    public List<Rail> rails = new ArrayList<>();
    public List<Coin> coins = new ArrayList<>();
    private Random rng = new Random(123);

    private Color groundColor = new Color(60, 78, 80);
    private Color groundFill = new Color(30, 40, 42);

    public Level() {
        generate();
    }

    public void generate() {
        ground.clear();
        rails.clear();
        coins.clear();

        // Generate piecewise linear terrain across 8000 units
        double x = -200;
        double y = 420;
        for (int i = 0; i < 120; i++) {
            double length = 100 + rng.nextInt(120);
            double dy = rng.nextInt(2) == 0 ? rng.nextDouble()*40 : -rng.nextDouble()*40;
            double ny = y + dy;

            // occasional ramps
            if (rng.nextDouble() < 0.1) {
                ny = y - 120 - rng.nextDouble() * 60;
                length = 160 + rng.nextInt(200);
            }

            TerrainSegment seg = new TerrainSegment(x, y, x + length, ny);
            ground.add(seg);
            x += length;
            y = ny;
        }

        // Add a long flat stretch at the end
        ground.add(new TerrainSegment(x, y, x + 600, y));

        // Rails
        for (int i = 0; i < 10; i++) {
            TerrainSegment base = ground.get(rng.nextInt(ground.size() - 5) + 2);
            double rx1 = base.x1 + rng.nextDouble() * (base.x2 - base.x1 - 200);
            double rx2 = rx1 + 160 + rng.nextDouble() * 240;
            rx2 = Math.min(rx2, base.x2 - 10);
            double ry1 = heightAt(rx1) - 25 - rng.nextDouble() * 60;
            double ry2 = ry1 + (rng.nextDouble() - 0.5) * 40; // small slope on rail
            rails.add(new Rail(rx1, ry1, rx2, ry2));
        }

        // Coins
        for (int i = 0; i < 60; i++) {
            double cx = -150 + rng.nextDouble() * (x + 500);
            double cy = heightAt(cx) - 80 - rng.nextDouble() * 160;
            coins.add(new Coin(cx, cy));
        }
    }

    public void draw(Graphics2D g2, Camera camera) {
        // draw fill under terrain
        Path2D path = new Path2D.Double();
        boolean started = false;
        for (int i = 0; i < ground.size(); i++) {
            TerrainSegment s = ground.get(i);
            if (!started) {
                path.moveTo(s.x1 - 2000, 2000);
                path.lineTo(s.x1, s.y1);
                started = true;
            }
            path.lineTo(s.x2, s.y2);
            if (i == ground.size() - 1) {
                path.lineTo(s.x2 + 3000, 2000);
                path.closePath();
            }
        }
        g2.setColor(groundFill);
        g2.fill(path);

        // draw terrain lines
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(groundColor);
        for (TerrainSegment s : ground) {
            g2.draw(new Line2D.Double(s.x1, s.y1, s.x2, s.y2));
        }

        // draw rails
        for (Rail r : rails) {
            r.draw(g2);
        }

        // draw coins
        for (Coin c : coins) {
            c.draw(g2);
        }
    }

    public void drawDebug(Graphics2D g2) {
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(Color.YELLOW);
        for (TerrainSegment s : ground) {
            g2.draw(new Line2D.Double(s.x1, s.y1, s.x2, s.y2));
            // normals
            double cx = (s.x1 + s.x2) * 0.5;
            double cy = (s.y1 + s.y2) * 0.5;
            Vec2 n = s.normal();
            g2.draw(new Line2D.Double(cx, cy, cx + n.x * 20, cy + n.y * 20));
        }
    }

    public double heightAt(double x) {
        // find segment containing x
        for (TerrainSegment s : ground) {
            if (x >= s.x1 && x <= s.x2) {
                return s.heightAt(x);
            }
        }
        // extrapolate beyond bounds
        if (!ground.isEmpty()) {
            TerrainSegment first = ground.get(0);
            TerrainSegment last = ground.get(ground.size() - 1);
            if (x < first.x1) return first.heightAt(x);
            if (x > last.x2) return last.heightAt(x);
        }
        return 500;
    }

    public Vec2 normalAt(double x) {
        for (TerrainSegment s : ground) {
            if (x >= s.x1 && x <= s.x2) {
                return s.normal();
            }
        }
        if (!ground.isEmpty()) {
            TerrainSegment first = ground.get(0);
            TerrainSegment last = ground.get(ground.size() - 1);
            if (x < first.x1) return first.normal();
            if (x > last.x2) return last.normal();
        }
        return new Vec2(0, -1);
    }

    public Rail findRailToGrind(double px, double py, double snapDist) {
        Rail best = null;
        double bestD = snapDist;
        for (Rail r : rails) {
            double d = r.distToPoint(px, py);
            if (d < bestD) {
                bestD = d;
                best = r;
            }
        }
        return best;
    }

    public Coin findCoinColliding(Rectangle2D.Double aabb) {
        for (Coin c : coins) {
            if (!c.collected && aabb.contains(c.x, c.y)) {
                return c;
            }
        }
        return null;
    }

    public void removeCoin(Coin c) {
        c.collected = true;
    }
}

/**
 * Terrain segment: line from (x1,y1) to (x2,y2). Provides height and normal.
 */
class TerrainSegment {
    public double x1, y1, x2, y2;
    private double dx, dy, len;

    public TerrainSegment(double x1, double y1, double x2, double y2) {
        if (x2 < x1) {
            double tx = x1; x1 = x2; x2 = tx;
            double ty = y1; y1 = y2; y2 = ty;
        }
        this.x1 = x1; this.y1 = y1;
        this.x2 = x2; this.y2 = y2;
        dx = x2 - x1; dy = y2 - y1;
        len = Math.sqrt(dx*dx + dy*dy);
        if (len < 1e-6) len = 1e-6;
    }

    public double heightAt(double x) {
        double t = (x - x1) / (x2 - x1);
        t = Math.max(0, Math.min(1, t));
        return y1 + t * (y2 - y1);
    }

    public Vec2 normal() {
        Vec2 n = new Vec2(-(y2 - y1), x2 - x1);
        return n.normalized();
    }

    public double slope() {
        return (y2 - y1) / (x2 - x1);
    }
}

/**
 * Simple rail segment; supports grinding.
 */
class Rail {
    public double x1, y1, x2, y2;

    public Rail(double x1, double y1, double x2, double y2) {
        if (x2 < x1) {
            double tx = x1; x1 = x2; x2 = tx;
            double ty = y1; y1 = y2; y2 = ty;
        }
        this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
    }

    public void draw(Graphics2D g2) {
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(180,180,200));
        g2.draw(new Line2D.Double(x1, y1, x2, y2));

        g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(80,80,90,70));
        g2.draw(new Line2D.Double(x1, y1+3, x2, y2+3));
    }

    public double distToPoint(double px, double py) {
        // distance from point to segment
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len2 = dx*dx + dy*dy;
        if (len2 < 1e-9) return Math.hypot(px - x1, py - y1);
        double t = ((px - x1) * dx + (py - y1) * dy) / len2;
        t = Math.max(0, Math.min(1, t));
        double cx = x1 + t * dx;
        double cy = y1 + t * dy;
        return Math.hypot(px - cx, py - cy);
    }

    public double paramAt(double px, double py) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len2 = dx*dx + dy*dy;
        if (len2 < 1e-9) return 0;
        double t = ((px - x1) * dx + (py - y1) * dy) / len2;
        return Math.max(0, Math.min(1, t));
    }

    public Point2D.Double pointAt(double t) {
        return new Point2D.Double(x1 + t * (x2 - x1), y1 + t * (y2 - y1));
    }

    public Vec2 tangent() {
        return new Vec2(x2 - x1, y2 - y1).normalized();
    }
}

/**
 * Coin collectible.
 */
class Coin {
    public double x, y;
    public boolean collected = false;
    private double animPhase = Math.random() * Math.PI * 2;

    public Coin(double x, double y) {
        this.x = x; this.y = y;
    }

    public void draw(Graphics2D g2) {
        if (collected) return;
        double t = System.nanoTime() / 1e9 + animPhase;
        double r = 9 + Math.sin(t * 3) * 2;
        Shape s = new Ellipse2D.Double(x - r, y - r, r*2, r*2);
        g2.setColor(new Color(255, 220, 120));
        g2.fill(s);
        g2.setColor(new Color(255, 235, 160));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(s);
    }
}

/**
 * Player with basic skateboard physics: slopes, ollie, grind, manual, kickflip trick.
 */
class Player {
    public Vec2 pos = new Vec2(0, 0);
    public Vec2 vel = new Vec2(0, 0);
    public Vec2 acc = new Vec2(0, 0);

    public Level level;
    private ParticleSystem particles;

    // Board geometry
    private double boardLen = 80;
    private double boardWidth = 16;
    private double boardAngle = 0; // radians
    private double angVel = 0;

    // State
    private boolean onGround = false;
    private boolean grinding = false;
    private boolean manual = false;
    private Rail grindRail = null;
    private double grindT = 0; // parameter along rail
    private boolean facingRight = true;

    // Air/trick
    private boolean inKickflip = false;
    private double kickflipTime = 0;

    // Timers
    private double coyoteTime = 0; // small grace after leaving ground to still jump
    private double jumpBuffer = 0; // buffer jump key for early press
    private double airTime = 0;

    // Input memory
    private boolean jumpPressed = false;
    private boolean trickPressed = false;

    // Constants
    private static final double GRAVITY = 1300;
    private static final double GROUND_FRICTION = 6.0;
    private static final double AIR_DRAG = 0.22;
    private static final double JUMP_VEL = 460;
    private static final double MAX_SPEED = 520;
    private static final double PUSH_ACCEL = 700;
    private static final double BRAKE_ACCEL = 900;
    private static final double SLOPE_ACC = 900;

    // For reference from outside
    public double lastGroundY = 0;

    private GameDebug debug = new GameDebug();

    public Player(Level level, ParticleSystem particles) {
        this.level = level;
        this.particles = particles;
        reset(level);
    }

    public void reset(Level level) {
        pos.set(-50, level.heightAt(-50) - 30);
        vel.set(160, 0);
        acc.set(0, 0);
        boardAngle = 0;
        angVel = 0;
        onGround = true;
        grinding = false;
        manual = false;
        inKickflip = false;
        kickflipTime = 0;
        coyoteTime = 0;
        jumpBuffer = 0;
        airTime = 0;
    }

    public void handleInput(Input input, double dt) {
        // Left/right accelerations
        if (input.left()) {
            vel.x -= BRAKE_ACCEL * dt;
            facingRight = vel.x >= 0 ? false : facingRight;
        }
        if (input.right()) {
            vel.x += PUSH_ACCEL * dt;
            facingRight = vel.x >= 0 ? true : facingRight;
        }

        if (input.up()) {
            jumpBuffer = 0.15; // buffer jump
        }

        if (input.space()) {
            if (!trickPressed) {
                trickPressed = true;
                tryTrick();
            }
        } else {
            trickPressed = false;
            if (!onGround && inKickflip) {
                // holding space not needed; we let it run
            }
        }

        // crouch for speed and grind snap
        if (input.down()) {
            vel.x += 80 * dt * (facingRight ? 1 : -1);
            vel.x = clamp(vel.x, -MAX_SPEED * 1.1, MAX_SPEED * 1.1);
        }
    }

    public void update(double dt) {
        // Apply gravity unless on rail
        if (!onGround && !grinding) {
            vel.y += GRAVITY * dt;
        }

        // Horizontal drag / friction
        if (onGround && !grinding) {
            double friction = GROUND_FRICTION;
            // slope assist: accelerate down slopes
            double slope = level.normalAt(pos.x).x; // normal.x ~ -slopeNormalizedY; but we can approximate
            vel.x += (SLOPE_ACC * -slope) * dt;
            vel.x -= vel.x * friction * dt;
        } else if (!grinding) {
            vel.x -= vel.x * AIR_DRAG * dt;
        }

        // Cap speed
        vel.x = clamp(vel.x, -MAX_SPEED, MAX_SPEED);

        // Jump buffering
        if (jumpBuffer > 0) {
            tryJump();
            jumpBuffer -= dt;
        }
        // coyote time decays
        if (coyoteTime > 0) {
            coyoteTime -= dt;
        }

        // Grinding movement
        if (grinding && grindRail != null) {
            // Project velocity along rail tangent
            Vec2 tang = grindRail.tangent();
            double speedAlong = vel.x * tang.x + vel.y * tang.y;
            // apply gravity projected along normal to keep on rail slightly
            double gx = 0, gy = GRAVITY;
            double along = (gx * tang.x + gy * tang.y);
            speedAlong += along * dt;
            speedAlong *= (1 - 0.5 * dt); // friction on rail
            // advance parameter along rail by speed
            double railLen = Math.hypot(grindRail.x2 - grindRail.x1, grindRail.y2 - grindRail.y1);
            if (railLen < 1e-6) railLen = 1;
            grindT += (speedAlong * dt) / railLen;

            // End conditions
            if (grindT < 0 || grindT > 1) {
                // Leave rail
                Point2D.Double p = grindRail.pointAt(Math.max(0, Math.min(1, grindT)));
                pos.x = p.x; pos.y = p.y - 5;
                grinding = false;
                onGround = false;
                // keep velocity in last tangent direction
                vel.x = tang.x * speedAlong;
                vel.y = tang.y * speedAlong - 50;
                // small pop-off
                vel.y -= 100;
                boardAngle = Math.atan2(tang.y, tang.x);
            } else {
                Point2D.Double p = grindRail.pointAt(grindT);
                pos.x = p.x;
                pos.y = p.y - 6; // offset above rail
                vel.x = tang.x * speedAlong;
                vel.y = tang.y * speedAlong;
                boardAngle = Math.atan2(tang.y, tang.x);
                // sparks particles
                particles.emit(new Particle(
                        pos.x, pos.y + 4,
                        -tang.y * (40 + Math.random() * 60),
                        tang.x * (40 + Math.random() * 60),
                        0.25 + Math.random() * 0.25,
                        new Color(255, 200, 100),
                        Particle.Layer.FRONT,
                        Particle.Type.SPARK));
            }
        } else {
            // Integrate position
            pos.add(vel.x * dt, vel.y * dt);

            // Ground collision
            double groundY = level.heightAt(pos.x);
            lastGroundY = groundY;
            if (pos.y >= groundY - 12) {
                // Collide with ground
                if (!onGround) {
                    // landing
                    if (inKickflip) {
                        // good landing: trick success
                        inKickflip = false;
                        kickflipTime = 0;
                        // callback trick
                        getPanel().onTrick("Kickflip", 200);
                    }
                    if (Math.abs(vel.y) > 280) {
                        // big landing -> particles
                        for (int i = 0; i < 12; i++) {
                            particles.emit(new Particle(
                                    pos.x, groundY,
                                    -120 + Math.random() * 240,
                                    -Math.random() * 200,
                                    0.5 + Math.random() * 0.6,
                                    new Color(200, 200, 200, 150),
                                    Particle.Layer.BACK,
                                    Particle.Type.DUST));
                        }
                    }
                }
                onGround = true;
                coyoteTime = 0.12; // resetting for very short time won't be used here
                manual = false;

                // Project velocity along ground tangent
                Vec2 n = level.normalAt(pos.x);
                Vec2 t = new Vec2(n.y, -n.x); // tangent
                double speed = vel.x * t.x + vel.y * t.y;
                vel.x = t.x * speed;
                vel.y = t.y * speed;

                // Snap to ground surface
                pos.y = groundY - 12;

                // board angle aligns with ground
                boardAngle = Math.atan2(t.y, t.x);
                angVel *= 0.8;

                // Check for rail snap if crouching (handled by input). We'll also auto-snap if very close
                // But actual snap is in tryGrind() called in tryJump/handleInput if pressing down
                tryGrindIfClose();
            } else {
                // Leave ground
                if (onGround) {
                    coyoteTime = 0.12;
                }
                onGround = false;
                airTime += dt;
            }
        }

        // Kickflip rotation animation
        if (inKickflip) {
            kickflipTime += dt;
            angVel = 8.5; // spin rate
            boardAngle += angVel * dt;
            if (kickflipTime > 0.65) {
                // trick ends; wait for landing for points
                inKickflip = false;
            }
        } else if (!grinding) {
            // board rotate toward velocity direction a bit in air
            double target = Math.atan2(vel.y, vel.x);
            double delta = wrapAngle(target - boardAngle);
            boardAngle += delta * 0.08;
            angVel *= 0.95;
        }

        // Coin collection AABB
        Coin coin = level.findCoinColliding(getAABB());
        if (coin != null) {
            level.removeCoin(coin);
            getPanel().onCollectCoin(50, coin.x, coin.y);
        }
    }

    private void tryJump() {
        if (onGround || coyoteTime > 0 || grinding) {
            // Jump
            if (grinding) {
                grinding = false;
                grindRail = null;
            }
            vel.y = -JUMP_VEL;
            onGround = false;
            coyoteTime = 0;
            airTime = 0;
            // particle burst
            for (int i = 0; i < 8; i++) {
                particles.emit(new Particle(
                        pos.x, pos.y + 8,
                        -100 + Math.random() * 200,
                        -100 - Math.random() * 120,
                        0.4 + Math.random() * 0.4,
                        new Color(180, 180, 180, 160),
                        Particle.Layer.BACK,
                        Particle.Type.DUST));
            }
        }
    }

    private void tryTrick() {
        if (!onGround && !grinding) {
            // Kickflip
            if (!inKickflip) {
                inKickflip = true;
                kickflipTime = 0;
            }
        } else if (onGround && Math.abs(level.normalAt(pos.x).y) > 0.95) {
            // Manual on flat-ish ground: balance for points
            manual = !manual;
            if (manual) {
                getPanel().onTrick("Manual", 100);
            }
        }
    }

    private void tryGrindIfClose() {
        if (grinding) return;
        // Snap to rail if within snap distance and moving forward
        Rail r = level.findRailToGrind(pos.x, pos.y, 24);
        if (r != null) {
            double t = r.paramAt(pos.x, pos.y + 8);
            Point2D.Double p = r.pointAt(t);
            if (Math.abs(p.y - pos.y) < 30) {
                grinding = true;
                grindRail = r;
                grindT = t;
                onGround = false;
                // align to rail
                Vec2 tang = r.tangent();
                double speedAlong = vel.x * tang.x + vel.y * tang.y;
                if (Math.abs(speedAlong) < 40) {
                    speedAlong = (facingRight ? 1 : -1) * 130;
                }
                vel.x = tang.x * speedAlong;
                vel.y = tang.y * speedAlong;
                pos.x = p.x; pos.y = p.y - 6;
                boardAngle = Math.atan2(tang.y, tang.x);

                // sparks
                for (int i = 0; i < 6; i++) {
                    particles.emit(new Particle(
                            pos.x, pos.y + 2,
                            -tang.y * (30 + Math.random() * 60),
                            tang.x * (30 + Math.random() * 60),
                            0.25 + Math.random() * 0.25,
                            new Color(255, 220, 140),
                            Particle.Layer.FRONT,
                            Particle.Type.SPARK));
                }
                // trick points
                getPanel().onTrick("Grind", 150);
            }
        }
    }

    private double wrapAngle(double a) {
        while (a > Math.PI) a -= Math.PI * 2;
        while (a < -Math.PI) a += Math.PI * 2;
        return a;
    }

    public Rectangle2D.Double getAABB() {
        // simple bounding box around player
        return new Rectangle2D.Double(pos.x - 15, pos.y - 35, 30, 40);
    }

    public void draw(Graphics2D g2, Camera camera, boolean debug) {
        // Draw shadow
        double groundY = level.heightAt(pos.x);
        double shadowScale = clamp(1.0 - (pos.y - groundY) / 150, 0.2, 1.0);
        Ellipse2D shadow = new Ellipse2D.Double(pos.x - 25 * shadowScale, groundY - 8, 50 * shadowScale, 12 * shadowScale);
        g2.setColor(new Color(0,0,0,80));
        g2.fill(shadow);

        // Board
        AffineTransform old = g2.getTransform();
        g2.translate(pos.x, pos.y);
        g2.rotate(boardAngle);

        Shape board = new RoundRectangle2D.Double(-boardLen/2, -boardWidth/2, boardLen, boardWidth, 8, 8);
        g2.setColor(new Color(120, 140, 160));
        g2.fill(board);
        g2.setColor(new Color(70, 80, 95));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(board);

        // Wheels
        Shape wheel1 = new Ellipse2D.Double(-boardLen/2 + 10, boardWidth/2 - 3, 8, 8);
        Shape wheel2 = new Ellipse2D.Double(boardLen/2 - 18, boardWidth/2 - 3, 8, 8);
        g2.setColor(new Color(230,230,230));
        g2.fill(wheel1); g2.fill(wheel2);
        g2.setColor(new Color(100,100,100));
        g2.draw(wheel1); g2.draw(wheel2);

        // Character as simple silhouette above board
        g2.rotate(-boardAngle); // character upright relative to world
        g2.translate(0, -26);
        g2.setColor(new Color(230, 230, 240));
        Shape body = new RoundRectangle2D.Double(-7, -18, 14, 28, 8, 8);
        g2.fill(body);
        Shape head = new Ellipse2D.Double(-7, -33, 14, 14);
        g2.fill(head);
        // arms
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(-7, -10, -18, -2));
        g2.draw(new Line2D.Double(7, -10, 18, -2));

        g2.setTransform(old);

        // Debug center
        if (debug) {
            g2.setColor(Color.RED);
            g2.fill(new Ellipse2D.Double(pos.x - 2, pos.y - 2, 4, 4));
        }
    }

    private GamePanel getPanel() {
        // Find container up the Swing hierarchy (a bit hacky but fine here)
        for (Frame f : Frame.getFrames()) {
            if (f.getComponent() instanceof GamePanel) return (GamePanel) f.getComponent();
        }
        return null;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static class GameDebug {
        // placeholder for internal debug flags
    }
}

/**
 * HUD class draws score, speed, etc. Also handles ephemeral popups.
 */
class HUD {
    private List<Popup> popups = new ArrayList<>();
    private Font titleFont = new Font("Dialog", Font.BOLD, 18);
    private Font smallFont = new Font("Dialog", Font.PLAIN, 13);
    private Font monoFont = new Font("Monospaced", Font.PLAIN, 12);

    private GamePanel panel;

    public HUD(GamePanel panel) {
        this.panel = panel;
    }

    public void draw(Graphics2D g2, GamePanel panel, Player player, int score, int combo, String comboLabel,
                     double elapsed, int fps, double timeScale, boolean paused, boolean debug) {
        // Panel overlay gradient
        Paint gp = new GradientPaint(0, 0, new Color(0,0,0,80), 0, 80, new Color(0,0,0,0));
        g2.setPaint(gp);
        g2.fillRect(0, 0, GamePanel.WIDTH, 90);

        g2.setColor(Color.WHITE);
        g2.setFont(titleFont);
        g2.drawString("Score: " + score, 16, 28);

        g2.setFont(smallFont);
        g2.drawString("Speed: " + String.format("%.0f", Math.abs(player.vel.x)) + " u/s", 16, 48);
        g2.drawString("Air: " + String.format("%.2f s", player.lastGroundY < player.pos.y ? 0 : 0), 16, 68);

        if (combo > 0) {
            g2.setColor(new Color(255, 220, 120));
            g2.setFont(titleFont);
            g2.drawString("Combo x" + combo + " " + comboLabel, 200, 28);
        }

        g2.setColor(new Color(200, 220, 240));
        g2.setFont(monoFont);
        g2.drawString("Time: " + String.format("%.1f s", elapsed), 1100, 24);
        g2.drawString("FPS: " + fps, 1100, 40);
        g2.drawString(String.format("TimeScale: %.2f", timeScale), 1100, 56);

        if (paused) {
            g2.setFont(new Font("Dialog", Font.BOLD, 48));
            g2.setColor(new Color(255,255,255,200));
            String p = "PAUSED";
            int w = g2.getFontMetrics().stringWidth(p);
            g2.drawString(p, (GamePanel.WIDTH - w) / 2, 160);
        }

        // Popups
        for (int i = popups.size() - 1; i >= 0; i--) {
            Popup p = popups.get(i);
            if (!p.alive()) popups.remove(i);
            else p.draw(g2, panel, player);
        }
    }

    public void addPopup(String text, double worldX, double worldY) {
        popups.add(new Popup(text, worldX, worldY));
    }

    static class Popup {
        String text;
        double worldX, worldY;
        double life = 1.2;

        public Popup(String text, double worldX, double worldY) {
            this.text = text;
            this.worldX = worldX;
            this.worldY = worldY;
        }

        public boolean alive() {
            return life > 0;
        }

        public void draw(Graphics2D g2, GamePanel panel, Player player) {
            life -= 1.0 / 60.0;
            // Convert world to screen approx by subtracting camera; but we'll fetch camera via panel?
            Camera cam = null;
            for (Frame f : Frame.getFrames()) {
                if (f.getContentPane() instanceof GamePanel) {
                    cam = ((GamePanel) f.getContentPane()).camera;
                }
            }
            if (cam == null) return;
            double sx = worldX - cam.screenX();
            double sy = worldY - cam.screenY() - (1.0 - life) * 50;

            g2.setFont(new Font("Dialog", Font.BOLD, 16));
            int w = g2.getFontMetrics().stringWidth(text);
            // shadow
            g2.setColor(new Color(0,0,0,140));
            g2.drawString(text, (int)sx - w/2 + 1, (int)sy + 1);
            g2.setColor(new Color(255, 240, 150, (int)(220 * life)));
            g2.drawString(text, (int)sx - w/2, (int)sy);
        }
    }
}

/**
 * Simple particle system for dust and sparks.
 */
class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();

    public void emit(Particle p) {
        particles.add(p);
    }

    public void update(double dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0) {
                particles.remove(i);
                continue;
            }
            // physics
            p.vx += (p.type == Particle.Type.DUST ? 0 : 0) * dt;
            p.vy += (p.type == Particle.Type.DUST ? 260 : 100) * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
        }
    }

    public void draw(Graphics2D g2, Particle.Layer layer) {
        for (Particle p : particles) {
            if (p.layer != layer) continue;
            p.draw(g2);
        }
    }

    public void clear() {
        particles.clear();
    }
}

class Particle {
    public enum Layer { BACK, FRONT }
    public enum Type { DUST, SPARK }
    double x, y, vx, vy, life;
    Color color;
    Layer layer;
    Type type;

    public Particle(double x, double y, double vx, double vy, double life, Color color, Layer layer, Type type) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = life; this.color = color; this.layer = layer; this.type = type;
    }

    public void draw(Graphics2D g2) {
        float alpha = (float)Math.max(0, Math.min(1, life));
        Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(color.getAlpha() * alpha));
        g2.setColor(c);
        if (type == Type.DUST) {
            Shape s = new Ellipse2D.Double(x - 4, y - 4, 8, 8);
            g2.fill(s);
        } else {
            Shape s = new Line2D.Double(x, y, x - vx * 0.06, y - vy * 0.06);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(s);
        }
    }
}

/**
 * Simple parallax background with layered hills and skyline.
 */
class ParallaxBackground {
    private List<Layer> layers = new ArrayList<>();
    private Random rng = new Random(1);

    static class Layer {
        double depth;
        Color color;
        List<Point2D.Double> points = new ArrayList<>();
    }

    public ParallaxBackground() {
        reset();
    }

    public void reset() {
        layers.clear();
        // distant hills
        for (int i = 0; i < 3; i++) {
            Layer L = new Layer();
            L.depth = 0.2 + i * 0.2;
            L.color = new Color(60 + i * 20, 80 + i * 20, 110 + i * 20);
            generateHills(L, 2000, 40 + i * 20, 80 + i * 30);
            layers.add(L);
        }
        // city skyline layer
        Layer city = new Layer();
        city.depth = 0.6;
        city.color = new Color(40, 60, 80);
        generateCity(city, 3000);
        layers.add(city);
    }

    private void generateHills(Layer layer, int width, int amplitude, int step) {
        layer.points.clear();
        double yBase = 380 + (1.0 - layer.depth) * 80;
        for (int x = -4000; x <= 8000; x += step) {
            double y = yBase + Math.sin(x * 0.002) * amplitude + Math.cos(x * 0.0013) * amplitude * 0.6;
            layer.points.add(new Point2D.Double(x, y));
        }
    }

    private void generateCity(Layer layer, int width) {
        layer.points.clear();
        double yBase = 420;
        int x = -4000;
        while (x <= 8000) {
            int w = 60 + rng.nextInt(120);
            int h = 80 + rng.nextInt(220);
            layer.points.add(new Point2D.Double(x, yBase - h));
            layer.points.add(new Point2D.Double(x + w, yBase - h));
            x += w + 30 + rng.nextInt(80);
        }
    }

    public void update(double dt, Camera cam) {
        // nothing dynamic for now
    }

    public void draw(Graphics2D g2, Camera cam) {
        for (Layer L : layers) {
            g2.setColor(L.color);
            if (L.points.isEmpty()) continue;
            if (L.depth < 0.6) {
                // hills
                Path2D path = new Path2D.Double();
                Point2D.Double first = L.points.get(0);
                path.moveTo(first.x * L.depth, first.y);
                for (Point2D.Double p : L.points) {
                    path.lineTo(p.x * L.depth, p.y);
                }
                path.lineTo(L.points.get(L.points.size()-1).x * L.depth, 2000);
                path.lineTo(L.points.get(0).x * L.depth, 2000);
                path.closePath();
                g2.fill(path);
            } else {
                // city boxes
                for (int i = 0; i < L.points.size()-1; i+=2) {
                    Point2D.Double a = L.points.get(i);
                    Point2D.Double b = L.points.get(i+1);
                    Shape s = new Rectangle2D.Double(a.x * L.depth, a.y, b.x * L.depth - a.x * L.depth, 420 - a.y);
                    g2.fill(s);
                }
            }
        }
    }
}

/**
 * Replay ghost trail: keeps a history of positions and draws a fading path.
 */
class ReplayGhost {
    private double[] xs, ys, ages;
    private int writeIdx = 0;
    private int capacity;
    private Color color;

    public ReplayGhost(int capacity, Color color) {
        this.capacity = capacity;
        xs = new double[capacity];
        ys = new double[capacity];
        ages = new double[capacity];
        this.color = color;
    }

    public void record(double x, double y) {
        xs[writeIdx] = x;
        ys[writeIdx] = y;
        ages[writeIdx] = 1.0;
        writeIdx = (writeIdx + 1) % capacity;

        // age all
        for (int i = 0; i < capacity; i++) {
            ages[i] *= 0.995;
        }
    }

    public void clear() {
        Arrays.fill(ages, 0);
        writeIdx = 0;
    }

    public void draw(Graphics2D g2, Camera cam) {
        g2.setStroke(new BasicStroke(2f));
        for (int i = 0; i < capacity - 1; i++) {
            int idx1 = (writeIdx + i) % capacity;
            int idx2 = (writeIdx + i + 1) % capacity;
            double a = ages[idx1];
            if (a < 0.02) continue;
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(a * color.getAlpha())));
            g2.draw(new Line2D.Double(xs[idx1], ys[idx1], xs[idx2], ys[idx2]));
        }
    }
}

/**
 * Small sound helper. Creates beeps using javax.sound.sampled.
 */
class Sound {
    public void beep(int freq, double seconds) {
        // Generate a short sine wave
        try {
            float sampleRate = 44100f;
            int samples = (int)(seconds * sampleRate);
            byte[] data = new byte[samples];
            for (int i = 0; i < samples; i++) {
                double t = i / sampleRate;
                double v = Math.sin(2 * Math.PI * freq * t);
                data[i] = (byte)(v * 120);
            }

            AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
            try (Clip clip = AudioSystem.getClip()) {
                clip.open(format, data, 0, data.length);
                clip.start();
            }
        } catch (Exception ignored) {
        }
    }
}