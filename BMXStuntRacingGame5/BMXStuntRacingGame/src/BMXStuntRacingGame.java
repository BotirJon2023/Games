import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BMXStuntRacingGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }
}

class GameWindow extends JFrame {
    public GameWindow() {
        setTitle("BMX Stunt Racing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel panel = new GamePanel(1280, 720);
        add(panel);
        pack();
        setLocationRelativeTo(null);
        panel.startGame();
    }
}

class GamePanel extends JPanel implements Runnable {
    private final int width;
    private final int height;
    private Thread gameThread;
    private volatile boolean running = false;

    // Game state
    enum State { MENU, RUNNING, PAUSED, GAMEOVER }

    private State state = State.MENU;

    private Player player;
    private Level level;
    private HUD hud;
    private Input input;
    private ParticleSystem particles;
    private final Random rng = new Random();

    // Timing
    private final int targetFps = 60;
    private final double targetTime = 1e9 / targetFps;

    // Double buffer
    private BufferedImage backBuffer;
    private Graphics2D g2d;

    public GamePanel(int width, int height) {
        this.width = width;
        this.height = height;
        setPreferredSize(new Dimension(width, height));
        setFocusable(true);
        requestFocus();
        init();
    }

    private void init() {
        backBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = backBuffer.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        input = new Input();
        addKeyListener(input);
        addMouseListener(input);
        addMouseMotionListener(input);

        resetGame();
    }

    private void resetGame() {
        player = new Player(width / 6f, height - 140);
        level = new Level(width, height, rng);
        hud = new HUD();
        particles = new ParticleSystem();
    }

    public void startGame() {
        if (gameThread == null) {
            running = true;
            gameThread = new Thread(this, "GameThread");
            gameThread.start();
        }
    }

    public void stopGame() {
        running = false;
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double accumulator = 0;

        while (running) {
            long now = System.nanoTime();
            double delta = now - lastTime;
            lastTime = now;
            accumulator += delta;

            boolean shouldRender = false;
            while (accumulator >= targetTime) {
                update((float) (targetTime / 1e9));
                accumulator -= targetTime;
                shouldRender = true;
            }

            if (shouldRender) {
                render();
                repaint();
            }

            // Sleep a tiny bit to reduce CPU usage
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void update(float dt) {
        switch (state) {
            case MENU:
                // simple menu interactions
                if (input.isKeyPressed(KeyEvent.VK_ENTER)) {
                    state = State.RUNNING;
                    input.clear();
                }
                break;
            case RUNNING:
                handleInput();
                player.update(dt, level, input, particles);
                level.update(dt, player, particles);
                particles.update(dt);
                hud.update(dt, player);

                if (player.isDead()) {
                    state = State.GAMEOVER;
                }
                if (input.isKeyPressed(KeyEvent.VK_ESCAPE)) {
                    state = State.PAUSED;
                    input.clear();
                }
                break;
            case PAUSED:
                if (input.isKeyPressed(KeyEvent.VK_ESCAPE) || input.isKeyPressed(KeyEvent.VK_P)) {
                    state = State.RUNNING;
                    input.clear();
                }
                if (input.isKeyPressed(KeyEvent.VK_R)) {
                    resetGame();
                    state = State.MENU;
                    input.clear();
                }
                break;
            case GAMEOVER:
                if (input.isKeyPressed(KeyEvent.VK_R)) {
                    resetGame();
                    state = State.MENU;
                    input.clear();
                }
                break;
        }
    }

    private void handleInput() {
        // toggle slow-mo (for debugging)
        if (input.isKeyPressed(KeyEvent.VK_TAB)) {
            // no-op for now
            input.clear();
        }
    }

    private void render() {
        // clear
        g2d.setColor(new Color(30, 30, 40));
        g2d.fillRect(0, 0, width, height);

        // draw background layers
        level.renderBackground(g2d);

        // draw track and obstacles
        level.render(g2d);

        // draw player (bike + rider)
        player.render(g2d);

        // draw particles
        particles.render(g2d);

        // draw HUD / UI
        hud.render(g2d, width, height, state);

        // pause overlay
        if (state == State.PAUSED) {
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.fillRect(0, 0, width, height);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 36));
            drawCenteredString(g2d, "PAUSED", width / 2, height / 2 - 20);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 18));
            drawCenteredString(g2d, "Press ESC or P to resume, R to reset", width / 2, height / 2 + 20);
        }

        if (state == State.MENU) {
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.fillRect(0, 0, width, height);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
            drawCenteredString(g2d, "BMX STUNT RACING", width / 2, height / 2 - 80);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
            drawCenteredString(g2d, "Enter to Start | Arrow Keys or A/D to Lean | Space to Jump | E to Stunt", width / 2, height / 2 - 20);
            drawCenteredString(g2d, "W to Pedal | S to Brake | R to Reset After Game Over", width / 2, height / 2 + 10);
        }

        if (state == State.GAMEOVER) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, width, height);
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 52));
            drawCenteredString(g2d, "GAME OVER", width / 2, height / 2 - 40);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 22));
            drawCenteredString(g2d, "R to try again", width / 2, height / 2 + 10);
        }
    }

    private void drawCenteredString(Graphics2D g, String text, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        int tx = x - fm.stringWidth(text) / 2;
        int ty = y - fm.getHeight() / 2 + fm.getAscent();
        g.drawString(text, tx, ty);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backBuffer, 0, 0, null);
    }
}

/* ------------------------- INPUT HANDLING ------------------------- */
class Input implements KeyListener, MouseListener, MouseMotionListener {
    private final boolean[] keys = new boolean[256];
    private final boolean[] keysDown = new boolean[256];

    private int mouseX, mouseY;
    private boolean mousePressed = false;

    public boolean isKey(int key) {
        if (key < 0 || key >= keys.length) return false;
        return keys[key];
    }

    public boolean isKeyPressed(int key) {
        if (key < 0 || key >= keysDown.length) return false;
        boolean v = keysDown[key];
        keysDown[key] = false; // consume
        return v;
    }

    public void clear() {
        for (int i = 0; i < keysDown.length; i++) keysDown[i] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k >= 0 && k < keys.length) {
            if (!keys[k]) keysDown[k] = true;
            keys[k] = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k >= 0 && k < keys.length) {
            keys[k] = false;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mousePressed(MouseEvent e) {
        mousePressed = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mousePressed = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }
}

/* ------------------------- PLAYER & BIKE ------------------------- */
class Player {
    Bike bike;
    float x, y; // position of bike's center
    float vx = 0, vy = 0; // velocity
    float angle = 0; // rotation angle of bike
    int score = 0;
    int combo = 0;
    float airtime = 0;

    private boolean onGround = true;
    private boolean performingStunt = false;
    private float stuntTimer = 0;

    private int health = 100;

    public Player(float startX, float startY) {
        this.x = startX;
        this.y = startY;
        bike = new Bike();
    }

    public void update(float dt, Level level, Input input, ParticleSystem particles) {
        // Controls mapping
        boolean left = input.isKey(KeyEvent.VK_LEFT) || input.isKey(KeyEvent.VK_A);
        boolean right = input.isKey(KeyEvent.VK_RIGHT) || input.isKey(KeyEvent.VK_D);
        boolean pedal = input.isKey(KeyEvent.VK_W) || input.isKey(KeyEvent.VK_UP);
        boolean brake = input.isKey(KeyEvent.VK_S) || input.isKey(KeyEvent.VK_DOWN);
        boolean jump = input.isKey(KeyEvent.VK_SPACE);
        boolean stunt = input.isKey(KeyEvent.VK_E);

        // Apply forces
        float accel = pedal ? bike.acceleration : -bike.friction;
        if (brake) accel -= bike.brake;
        vx += accel * dt;

        // clamp speed
        vx = clamp(vx, -bike.maxReverseSpeed, bike.maxSpeed);

        // update gravity if airborne
        if (!onGround) {
            vy += bike.gravity * dt;
            airtime += dt;
        }

        // rotation control mid-air
        if (!onGround) {
            if (left) angle -= bike.airTurnRate * dt;
            if (right) angle += bike.airTurnRate * dt;
        }

        // Jump mechanics (if on ramp or ground with enough speed)
        Ramp ramp = level.checkRampAt(x);
        if (ramp != null && ramp.isOnRamp(x, y) && jump && onGround) {
            // Launch using ramp normal and speed
            float launch = bike.rampLaunchMultiplier * Math.abs(vx);
            vy = -launch;
            onGround = false;
            airtime = 0;
            particles.emit(new Particle(x, y, Particle.Type.SMOKE));
        }

        // allow hop from flat ground if enough speed
        if (onGround && jump && Math.abs(vx) > bike.minHopSpeed) {
            vy = -bike.hopStrength;
            onGround = false;
            airtime = 0;
            particles.emit(new Particle(x, y, Particle.Type.SMOKE));
        }

        // stunt initiation
        if (!onGround && stunt && !performingStunt) {
            performingStunt = true;
            stuntTimer = bike.stuntDuration;
            combo++;
            score += 100; // basic stunt score
            particles.emit(new Particle(x, y, Particle.Type.SPARK));
        }

        if (performingStunt) {
            stuntTimer -= dt;
            angle += bike.stuntSpinRate * dt * (vx >= 0 ? 1 : -1);
            if (stuntTimer <= 0) performingStunt = false;
        }

        // update position
        x += vx * dt * 60; // scale for playable feel
        y += vy * dt * 60;

        // collision with level
        float groundY = level.getHeightAt(x);
        if (y >= groundY) {
            // landed
            if (!onGround) {
                // check landing quality
                float landingAngle = angle % (2 * (float) Math.PI);
                if (landingAngle < 0) landingAngle += 2 * (float) Math.PI;
                float deg = (float) Math.toDegrees(landingAngle);
                float anglePenalty = Math.abs(180 - deg);
                if (anglePenalty < bike.landingTolerance) {
                    // great landing
                    score += 200 * combo;
                    particles.emitBurst(x, groundY, 12);
                } else {
                    // bad landing - crash
                    health -= (int) (anglePenalty / 2);
                    vx *= 0.4f; // lose speed
                    particles.emitBurst(x, groundY, 24);
                    combo = 0;
                }
            }
            y = groundY;
            vy = 0;
            onGround = true;
            airtime = 0;
            performingStunt = false;
            angle = 0;
        }

        // obstacle collisions
        for (Obstacle o : level.getObstaclesAround(x)) {
            if (o.collidesWith(x, y)) {
                // simple collision response: knock back and damage
                vx *= -0.3f;
                vy = -3;
                health -= 10;
                particles.emitBurst(x, y - 10, 18);
            }
        }

        // clamp position
        if (x < 0) x = 0;
        if (x > level.getWorldWidth() - 50) x = level.getWorldWidth() - 50;

        // passive score on travel
        score += Math.abs((int) vx);

        // regen combo timer
        if (onGround) {
            if (combo > 0) combo = Math.max(0, combo - 1);
        }
    }

    public void render(Graphics2D g) {
        // draw bike with rotation at x,y
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(angle);

        // draw wheels
        g.setColor(Color.BLACK);
        g.fillOval(-34, -8, 24, 24);
        g.fillOval(10, -8, 24, 24);

        // frame
        g.setStroke(new BasicStroke(4));
        g.setColor(Color.DARK_GRAY);
        g.drawLine(-20, -2, 20, -2);
        g.drawLine(0, -2, -10, -20);
        g.drawLine(0, -2, 18, -12);

        // rider as simple body
        g.rotate(-angle);
        g.translate(8, -22);
        g.setColor(Color.BLUE);
        g.fillOval(-8, -8, 16, 16); // head
        g.setColor(Color.ORANGE);
        g.fillRect(-6, 0, 12, 16); // torso

        // restore
        g.setTransform(old);

        // draw health bar above player
        g.setColor(Color.RED);
        g.fillRect((int) x - 40, (int) y - 80, 80, 8);
        g.setColor(Color.GREEN);
        g.fillRect((int) x - 40, (int) y - 80, Math.max(0, health * 80 / 100), 8);
        g.setColor(Color.WHITE);
        g.drawRect((int) x - 40, (int) y - 80, 80, 8);
    }

    public boolean isDead() {
        return health <= 0;
    }

    private float clamp(float v, float a, float b) {
        return Math.max(a, Math.min(b, v));
    }
}

class Bike {
    // physical properties
    float acceleration = 0.2f;
    float friction = 0.02f;
    float brake = 0.6f;
    float maxSpeed = 8f;
    float maxReverseSpeed = 2f;

    float gravity = 9.8f * 0.5f;
    float airTurnRate = 2.6f; // radians per second
    float hopStrength = 6.5f;
    float minHopSpeed = 1.2f;
    float rampLaunchMultiplier = 0.45f;

    float stuntSpinRate = (float) Math.PI * 2; // one turn per second
    float stuntDuration = 1.2f;

    float landingTolerance = 60f; // degrees allowed from upright

    // visual
    Color frameColor = Color.DARK_GRAY;
}

/* ------------------------- LEVEL, RAMPS & OBSTACLES ------------------------- */
class Level {
    private final int width;
    private final int height;
    private final int worldWidth;
    private final Random rng;

    private final List<Platform> platforms = new ArrayList<>();
    private final List<Ramp> ramps = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();

    // parallax backgrounds
    private final List<Cloud> clouds = new ArrayList<>();

    public Level(int width, int height, Random rng) {
        this.width = width;
        this.height = height;
        this.rng = rng;
        this.worldWidth = width * 4; // long track
        generate();
    }

    public int getWorldWidth() {
        return worldWidth;
    }

    private void generate() {
        // create ground platforms with some bumps and gaps
        int x = 0;
        int baseY = height - 100;
        while (x < worldWidth) {
            int len = 200 + rng.nextInt(300);
            platforms.add(new Platform(x, baseY, len));

            // occasionally add a ramp
            if (rng.nextFloat() < 0.25f) {
                int rx = x + 50 + rng.nextInt(len - 100);
                ramps.add(new Ramp(rx, baseY, 100 + rng.nextInt(120), rng.nextBoolean()));
            }

            // obstacle
            if (rng.nextFloat() < 0.3f) {
                int ox = x + 30 + rng.nextInt(len - 60);
                obstacles.add(new Obstacle(ox, baseY - 16));
            }

            x += len;
            // small variation
            baseY += rng.nextInt(41) - 20;
            baseY = clamp(baseY, height - 200, height - 70);
        }

        // clouds
        for (int i = 0; i < 20; i++) {
            clouds.add(new Cloud(rng.nextInt(worldWidth), rng.nextInt(height / 2)));
        }
    }

    public void update(float dt, Player player, ParticleSystem particles) {
        // move clouds slowly
        for (Cloud c : clouds) c.update(dt);

        // simple obstacle lifetime
        Iterator<Obstacle> it = obstacles.iterator();
        while (it.hasNext()) {
            Obstacle o = it.next();
            if (o.x < -200 || o.x > worldWidth + 200) {
                it.remove();
            }
        }

        // occasionally spawn pickups/particles based on player
        if (rng.nextFloat() < 0.02f) {
            particles.emit(new Particle(player.x + 200 + rng.nextInt(200), getHeightAt(player.x), Particle.Type.SPARK));
        }
    }

    public void renderBackground(Graphics2D g) {
        // sky gradient
        GradientPaint gp = new GradientPaint(0, 0, new Color(50, 120, 220), 0, height, new Color(20, 20, 40));
        g.setPaint(gp);
        g.fillRect(0, 0, width, height);

        // sun
        g.setColor(new Color(255, 230, 120, 200));
        g.fillOval(width - 160, 40, 120, 120);

        // clouds (parallax)
        for (Cloud c : clouds) {
            c.render(g);
        }
    }

    public void render(Graphics2D g) {
        // draw platforms
        g.setColor(new Color(80, 60, 40));
        for (Platform p : platforms) p.render(g);

        // ramps
        for (Ramp r : ramps) r.render(g);

        // obstacles
        for (Obstacle o : obstacles) o.render(g);

        // decorative foreground
        g.setColor(new Color(50, 40, 30));
        g.fillRect(0, height - 50, width, 50);

        // draw horizon line
        g.setColor(new Color(150, 100, 50));
        g.drawLine(0, height - 100, width, height - 100);
    }

    public float getHeightAt(float worldX) {
        // find nearest platform containing worldX
        for (Platform p : platforms) {
            if (worldX >= p.x && worldX <= p.x + p.width) return p.y;
        }
        // default ground
        return height - 100;
    }

    public Ramp checkRampAt(float worldX) {
        for (Ramp r : ramps) {
            if (worldX >= r.x && worldX <= r.x + r.width) return r;
        }
        return null;
    }

    public List<Obstacle> getObstaclesAround(float worldX) {
        List<Obstacle> near = new ArrayList<>();
        for (Obstacle o : obstacles) {
            if (Math.abs(o.x - worldX) < 80) near.add(o);
        }
        return near;
    }

    private int clamp(int v, int a, int b) {
        return Math.max(a, Math.min(b, v));
    }
}

class Platform {
    int x, y, width;

    public Platform(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public void render(Graphics2D g) {
        g.fillRect(x, y, width, 12);
        // grass edge
        g.setColor(new Color(60, 120, 60));
        g.fillRect(x, y - 6, width, 6);
        g.setColor(new Color(80, 60, 40));
    }
}

class Ramp {
    int x, y, width;
    boolean leftToRight; // slope direction

    public Ramp(int x, int y, int width, boolean leftToRight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.leftToRight = leftToRight;
    }

    public void render(Graphics2D g) {
        int[] xs, ys;
        if (leftToRight) {
            xs = new int[]{x, x + width, x + width};
            ys = new int[]{y, y - 30, y};
        } else {
            xs = new int[]{x, x + width, x};
            ys = new int[]{y - 30, y, y};
        }
        g.setColor(new Color(140, 90, 60));
        g.fillPolygon(xs, ys, 3);
        g.setColor(Color.BLACK);
        g.drawPolygon(xs, ys, 3);
    }

    public boolean isOnRamp(float px, float py) {
        return px >= x && px <= x + width && py >= y - 50 && py <= y + 20;
    }
}

class Obstacle {
    int x, y;
    int size = 18;

    public Obstacle(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void render(Graphics2D g) {
        g.setColor(new Color(90, 40, 20));
        g.fillRect(x - size / 2, y - size, size, size);
        g.setColor(Color.BLACK);
        g.drawRect(x - size / 2, y - size, size, size);
    }

    public boolean collidesWith(float px, float py) {
        float dx = Math.abs(px - x);
        float dy = Math.abs(py - (y - size / 2));
        return dx < size && dy < size;
    }
}

class Cloud {
    float x, y;
    float speed;

    public Cloud(float x, float y) {
        this.x = x;
        this.y = y;
        this.speed = 5 + (float) Math.random() * 15;
    }

    public void update(float dt) {
        x += speed * dt * 10;
        if (x > 5000) x = -200;
    }

    public void render(Graphics2D g) {
        g.setColor(new Color(255, 255, 255, 200));
        g.fillOval((int) x % 2000, (int) y, 60, 30);
        g.fillOval((int) x % 2000 + 20, (int) y - 10, 80, 40);
        g.fillOval((int) x % 2000 + 50, (int) y, 60, 30);
    }
}

/* ------------------------- PARTICLES ------------------------- */
class ParticleSystem {
    private final List<Particle> list = new ArrayList<>();

    public void emit(Particle p) {
        list.add(p);
    }

    public void emitBurst(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle(x + (float) (Math.random() * 30 - 15), y + (float) (Math.random() * 10 - 5), Particle.Type.DEBRIS);
            p.vx = (float) (Math.random() * 6 - 3);
            p.vy = (float) (Math.random() * -6 - 2);
            list.add(p);
        }
    }

    public void update(float dt) {
        Iterator<Particle> it = list.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(dt);
            if (!p.alive) it.remove();
        }
    }

    public void render(Graphics2D g) {
        for (Particle p : list) p.render(g);
    }
}

class Particle {
    enum Type { SMOKE, SPARK, DEBRIS }

    float x, y;
    float vx, vy;
    float life = 1.0f;
    boolean alive = true;
    Type type;

    public Particle(float x, float y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.vx = (float) (Math.random() * 2 - 1);
        this.vy = (float) (Math.random() * -2 - 0.5);
        this.life = 0.5f + (float) Math.random() * 1.2f;
    }

    public void update(float dt) {
        life -= dt;
        if (life <= 0) { alive = false; return; }
        vy += 9.8f * dt * 0.3f; // gravity
        x += vx * dt * 80;
        y += vy * dt * 80;
    }

    public void render(Graphics2D g) {
        float t = Math.max(0, Math.min(1, life));
        if (type == Type.SMOKE) {
            g.setColor(new Color(160, 160, 160, (int) (t * 200)));
            g.fillOval((int) x - 6, (int) y - 6, 12, 12);
        } else if (type == Type.SPARK) {
            g.setColor(new Color(255, 200, 80, (int) (t * 255)));
            g.fillOval((int) x - 3, (int) y - 3, 6, 6);
        } else {
            g.setColor(new Color(140, 90, 60, (int) (t * 255)));
            g.fillRect((int) x - 2, (int) y - 2, 4, 4);
        }
    }
}

/* ------------------------- HUD ------------------------- */
class HUD {
    private float timer = 0;

    public void update(float dt, Player player) {
        timer += dt;
    }

    public void render(Graphics2D g, int screenW, int screenH, GamePanel.State state) {
        // scoreboard
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        Object player;
        g.drawString("Score: " + (player == null ? 0 : player.score), 20, 30);
        g.drawString("Combo: " + (player == null ? 0 : player.combo), 20, 56);

        // speed
        if (player != null) {
            g.drawString(String.format("Speed: %.1f", player.vx), 20, 80);
        }

        // help
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(200, 200, 200, 160));
        g.drawString("W/Up: Pedal | S/Down: Brake | A/D or Left/Right: Lean | Space: Jump | E: Stunt", 20, screenH - 20);
    }
}

class Utils {
    public static float clamp(float v, float a, float b) {
        return Math.max(a, Math.min(b, v));
    }
}
