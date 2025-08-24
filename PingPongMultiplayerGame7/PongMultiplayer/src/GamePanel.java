import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.util.*;
import java.util.List;


public class GamePanel {

// Virtual resolution; canvas is scaled to fit window
private int VIRTUAL_WIDTH = 1066;
private int VIRTUAL_HEIGHT = 600;

private Thread gameThread;
private volatile boolean running = false;

// Timing
private long lastTimeNanos = 0L;
private double accumulator = 0.0;
private final double dtFixed = 1.0 / 120.0; // fixed update step

// Input
private final Set<Integer> keys = new HashSet<>();
private boolean hasFocus = true;

// Game state
private enum State { MENU, COUNTDOWN, PLAYING, PAUSED, SETTINGS, HELP, GAME_OVER }
private State state = State.MENU;

// Themes
static class Theme {
    Color bg, left, right, mid, hud;
    Theme(Color bg, Color left, Color right, Color mid, Color hud) {
        this.bg = bg; this.left = left; this.right = right; this.mid = mid; this.hud = hud;
    }
}
private int themeIndex = 0;
private final List<Theme> themes = Arrays.asList(
        new Theme(new Color(0x0b0f14), new Color(0x5588ff), new Color(0xff5555), new Color(0x223344), new Color(0xe6f2ff)),
        new Theme(new Color(0x101010), new Color(0x00ff00), new Color(0xffff00), new Color(0x333333), new Color(0xe6e6e6)),
        new Theme(new Color(0x051013), new Color(0x46f1b2), new Color(0xff5f8e), new Color(0x0c222a), new Color(0xe6f2ff)),
        new Theme(new Color(0x190c25), new Color(0xffb700), new Color(0x00d9ff), new Color(0x35174e), new Color(0xfff4cc))
);

// Settings
private boolean aiLeft = false;
private boolean aiRight = false;
private int winScore = 7;
private boolean showFPS = false;

// Visual fx toggles
private boolean fxGlow = true;       // not real glow, but layered rendering
private boolean fxTrails = true;
private boolean fxParticles = true;
private boolean fxScreenShake = true;

        // Entities
        private Paddle leftPaddle, rightPaddle;
private final List<Ball> balls = new ArrayList<>();
private final List<Particle> particles = new ArrayList<>();
private final List<PowerUp> powerUps = new ArrayList<>();

// Spawning power-ups
private double nextPowerIn = 6.0;

// Scores and serve
private int scoreLeft = 0;
private int scoreRight = 0;
private int servingDir = 0; // -1 left serves, +1 right serves, 0 random
private double countdown = 0.0;
private long roundStartMillis = 0L;

// Misc
private String hudMessage = "";
private double hudAlpha = 0.0;
private final Random rng = new Random();
private String lastHitBy = null; // "left" or "right"

// FPS
private long fpsLastTime = 0L;
private int fpsFrames = 0;
private int fpsValue = 0;

// Screen shake
private double shakeMag = 0.0;

// Trails buffer image for fading effect (optional)
private BufferedImage trailLayer;
private Graphics2D trailG;

// Offscreen buffer for letterbox scaling
private BufferedImage backBuffer;

// Constructor
GamePanel() {
    setPreferredSize(new Dimension((int)(VIRTUAL_WIDTH * 0.9), (int)(VIRTUAL_HEIGHT * 0.9)));
    setBackground(Color.BLACK);
    setFocusable(true);
    setDoubleBuffered(true);
    addKeyListener(this);
    addMouseListener(this);
    addMouseMotionListener(this);
    addFocusListener(this);
    addComponentListener(this);
    initGame();
}

private void initGame() {
    applyTheme(0);
    leftPaddle = new Paddle("left");
    rightPaddle = new Paddle("right");
    balls.clear();
    particles.clear();
    powerUps.clear();
    scoreLeft = 0;
    scoreRight = 0;
    servingDir = 0;
    countdown = 0.0;
    state = State.MENU;
    lastHitBy = null;
    nextPowerIn = 6.0;
    initBuffers();
}

private void initBuffers() {
    backBuffer = new BufferedImage(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    trailLayer = new BufferedImage(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    if (trailG != null) {
        trailG.dispose();
    }
    trailG = trailLayer.createGraphics();
    trailG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
}

private void applyTheme(int index) {
    themeIndex = (index + themes.size()) % themes.size();
    setBackground(themes.get(themeIndex).bg);
    repaint();
}

void startGameThread() {
    if (running) return;
    running = true;
    gameThread = new Thread(this, "GameThread");
    gameThread.start();
}

@Override
public void run() {
    lastTimeNanos = System.nanoTime();
    fpsLastTime = System.nanoTime();

    while (running) {
        long now = System.nanoTime();
        double delta = (now - lastTimeNanos) / 1_000_000_000.0;
        lastTimeNanos = now;
        accumulator += Math.min(delta, 0.05);

        // Fixed updates for stable physics
        while (accumulator >= dtFixed) {
            update(dtFixed);
            accumulator -= dtFixed;
        }

        repaint();

        // FPS calc
        fpsFrames++;
        if (now - fpsLastTime >= 1_000_000_000L) {
            fpsValue = fpsFrames;
            fpsFrames = 0;
            fpsLastTime = now;
        }

        try {
            Thread.sleep(2);
        } catch (InterruptedException ignored) {}
    }
}

private void update(double dt) {
    // state-machine
    if (state == State.MENU) {
        // idle
    } else if (state == State.COUNTDOWN) {
        countdown -= dt;
        if (countdown <= 0) {
            state = State.PLAYING;
            serve();
        } else {
            // lightly tick world
            updateWorld(dt * 0.2, true);
        }
    } else if (state == State.PLAYING) {
        updateWorld(dt, false);
    } else if (state == State.PAUSED) {
        // idle
    } else if (state == State.SETTINGS) {
        // idle
    } else if (state == State.HELP) {
        // idle
    } else if (state == State.GAME_OVER) {
        // idle
    }

    if (hudAlpha > 0.01) {
        hudAlpha *= 0.98;
    }
}

private void updateWorld(double dt, boolean idle) {
    // Update paddles
    leftPaddle.update(dt, aiLeft, keys, KeyEvent.VK_W, KeyEvent.VK_S);
    rightPaddle.update(dt, aiRight, keys, KeyEvent.VK_UP, KeyEvent.VK_DOWN);

    // Spawn power-ups
    nextPowerIn -= dt;
    if (nextPowerIn <= 0 && state == State.PLAYING) {
        powerUps.add(new PowerUp(randomType()));
        nextPowerIn = 7.0 + rng.nextDouble() * 5.0;
        shake(3.0);
    }

    // Update balls and collisions
    Iterator<Ball> itBall = balls.iterator();
    while (itBall.hasNext()) {
        Ball b = itBall.next();
        if (!b.alive) {
            itBall.remove();
            continue;
        }
        b.update(dt);
        // Wall collisions handled inside Ball.update()

        // Paddle collisions
        if (b.collidePaddle(leftPaddle)) {
            lastHitBy = "left";
            shake(3.0);
            spawnCollisionParticles(b.x, b.y, leftPaddle.color.brighter());
        }
        if (b.collidePaddle(rightPaddle)) {
            lastHitBy = "right";
            shake(3.0);
            spawnCollisionParticles(b.x, b.y, rightPaddle.color.brighter());
        }

        // Out of bounds for scoring
        if (b.x < -b.radius - 100) {
            itBall.remove();
            scoreRight++;
            hud("Point: Right");
            checkWinOrReset("right");
            continue;
        }
        if (b.x > VIRTUAL_WIDTH + b.radius + 100) {
            itBall.remove();
            scoreLeft++;
            hud("Point: Left");
            checkWinOrReset("left");
            continue;
        }
    }

    // Ensure we have a ball during play
    if (state == State.PLAYING && balls.isEmpty()) {
        resetRound();
    }

    // Particles
    Iterator<Particle> itP = particles.iterator();
    while (itP.hasNext()) {
        if (!itP.next().update(dt)) itP.remove();
    }

    // Power-ups
    Iterator<PowerUp> itPow = powerUps.iterator();
    while (itPow.hasNext()) {
        PowerUp p = itPow.next();
        if (!p.update(dt)) {
            itPow.remove();
            continue;
        }
        // collect with balls
        for (Ball b : balls) {
            if (!b.alive) continue;
            double dx = b.x - p.x;
            double dy = b.y - p.y;
            double rr = (b.radius + p.radius);
            if (dx*dx + dy*dy <= rr*rr) {
                String target = lastHitBy != null ? lastHitBy : (rng.nextBoolean() ? "left" : "right");
                p.apply(target);
                itPow.remove();
                shake(4.0);
                spawnCollisionParticles(p.x, p.y, p.color.darker());
                break;
            }
        }
    }

    // Screen shake decay
    shakeMag *= 0.9;
    if (shakeMag < 0.1) shakeMag = 0.0;
}

private void checkWinOrReset(String scorer) {
    // decide serving direction based on who conceded
    servingDir = "left".equals(scorer) ? 1 : -1;

    if (scoreLeft >= winScore || scoreRight >= winScore) {
        state = State.GAME_OVER;
        return;
    }
    resetRound();
}

private void resetRound() {
    balls.clear();
    spawnBall(0);
    countdown = 2.2;
    state = State.COUNTDOWN;
    roundStartMillis = System.currentTimeMillis();
    nextPowerIn = 6.0 + rng.nextDouble() * 4.0;
}

private void serve() {
    for (Ball b : balls) {
        b.serve(servingDir);
    }
}

private void spawnBall(int dir) {
    Ball b = new Ball();
    b.reset(dir);
    balls.add(b);
}

private void spawnExtraBall(String side) {
    int dir = "left".equals(side) ? -1 : 1;
    Ball b = new Ball();
    b.serve(dir);
    b.vx *= 1.05;
    b.vy *= 1.05;
    balls.add(b);
}

private void hud(String msg) {
    hudMessage = msg;
    hudAlpha = 1.0;
}

private void shake(double mag) {
    if (fxScreenShake) {
        shakeMag = Math.max(shakeMag, mag);
    }
}

private PowerUp.Type randomType() {
    PowerUp.Type[] types = PowerUp.Type.values();
    return types[rng.nextInt(types.length)];
}

private void spawnCollisionParticles(double x, double y, Color c) {
    if (!fxParticles) return;
    for (int i = 0; i < 14; i++) {
        particles.add(Particle.spray(x, y, c));
    }
}
}