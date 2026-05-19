import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import java.util.List;

public class TennisBrandenburgGame extends Canvas implements Runnable {

    // Window and court
    static final int WIDTH = 1280;
    static final int HEIGHT = 720;

    static final double GROUND_Y = HEIGHT - 90;
    static final double COURT_LEFT = 90;
    static final double COURT_RIGHT = WIDTH - 90;
    static final double NET_X = WIDTH / 2.0;
    static final double NET_HEIGHT = 150;
    static final double NET_HALF_THICK = 5;

    // Physics
    static final double GRAVITY = 1800; // px/s^2
    static final double GROUND_RESTITUTION = 0.70;
    static final double AIR_DRAG = 0.0005;
    static final double FRICTION = 0.80;

    // Players
    static final double PLAYER_SPEED = 460;
    static final double PLAYER_JUMP = -740;
    static final double PLAYER_WIDTH = 26;
    static final double PLAYER_HEIGHT = 120;

    // Racket
    static final double RACKET_RADIUS = 26;
    static final double SWING_TIME_MS = 220;

    // Ball
    static final double BALL_RADIUS = 12;
    static final double BALL_MAX_SPEED = 1400;

    // Background options (set to true and put a valid URL or file URL "file:/..."):
    static final boolean USE_BRANDENBURG_IMAGE = false;
    static final String BRANDENBURG_IMAGE_URL = "https://upload.wikimedia.org/wikipedia/commons/1/1d/Brandenburger_Tor_abends.jpg"; // example; replace if needed

    private volatile boolean running = true;
    private Thread loopThread;

    // Input
    private final Set<Integer> keys = Collections.synchronizedSet(new HashSet<>());

    // Game state
    private enum RallyState { SERVE, PLAY, POINT }
    private RallyState state = RallyState.SERVE;
    private Player p1, p2, server, receiver;
    private Ball ball;
    private boolean aiEnabled = true;
    private boolean paused = false;

    private long lastTime;
    private final Random rng = new Random();

    // Particles and clouds
    private final List<Particle> particles = new ArrayList<>();
    private final List<Cloud> clouds = new ArrayList<>();

    // Score
    private final Score score = new Score();

    // Background image (optional)
    private BufferedImage bgImage;

    public TennisBrandenburgGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setIgnoreRepaint(true);

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                keys.add(e.getKeyCode());
                handleKeyShortcuts(e);
            }
            @Override public void keyReleased(KeyEvent e) {
                keys.remove(e.getKeyCode());
            }
        });

        // Build entities
        p1 = new Player("P1", COURT_LEFT + 120, GROUND_Y - PLAYER_HEIGHT,
                new Color(0x2B8CFF), Color.WHITE, true);
        p2 = new Player("P2", COURT_RIGHT - 120, GROUND_Y - PLAYER_HEIGHT,
                new Color(0xFF4D4D), Color.WHITE, false);
        ball = new Ball(NET_X - 120, GROUND_Y - 200);

        // Setup serve
        initServe(p1);

        // Clouds
        for (int i = 0; i < 7; i++) {
            double y = 80 + rng.nextDouble() * 140;
            double w = 120 + rng.nextDouble() * 250;
            double spd = 12 + rng.nextDouble() * 22;
            clouds.add(new Cloud(rng.nextDouble() * WIDTH, y, w, spd));
        }

        // Optional background image
        if (USE_BRANDENBURG_IMAGE) {
            try {
                bgImage = scaledImage(new URL(BRANDENBURG_IMAGE_URL));
            } catch (Exception ignored) {}
        }
    }

    private void handleKeyShortcuts(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_F1) {
            aiEnabled = !aiEnabled;
        } else if (code == KeyEvent.VK_R) {
            resetMatch();
        } else if (code == KeyEvent.VK_P) {
            paused = !paused;
        } else if (code == KeyEvent.VK_ESCAPE) {
            running = false;
        }
    }

    private void initServe(Player who) {
        server = who;
        receiver = (who == p1) ? p2 : p1;
        state = RallyState.SERVE;
        ball.vx = ball.vy = 0;
        positionServeBall();
    }

    private void positionServeBall() {
        double sx = server.leftSide ? server.x + 30 : server.x - 30;
        double sy = server.y + 40;
        ball.setPosition(sx, sy);
    }

    private void attemptServe(Player who) {
        if (state != RallyState.SERVE || server != who) return;
        who.trySwing();
        double direction = (who == p1) ? 1 : -1;
        double speed = 850 + rng.nextDouble() * 140;
        double angle = Math.toRadians(18 + rng.nextDouble() * 6);
        ball.vx = Math.cos(angle) * speed * direction;
        ball.vy = -Math.sin(angle) * speed * 0.72;
        ball.lastHitter = who;
        ball.bounceCountSinceHit = 0;
        state = RallyState.PLAY;
    }

    private void awardPoint(Player to, String reason) {
        if (state != RallyState.PLAY) return;
        state = RallyState.POINT;
        boolean toP1 = (to == p1);
        score.addPoint(toP1);
        // Simple floating text particle
        particles.add(Particle.text(ball.x, 120, reason + " • Point " + (toP1 ? "P1" : "P2"),
                to.color, 2.0));

        // Alternate server
        server = (server == p1) ? p2 : p1;
        receiver = (server == p1) ? p2 : p1;

        // Reset after delay
        Timer t = new Timer(900, e -> initServe(server));
        t.setRepeats(false);
        t.start();
    }

    private void resetMatch() {
        score.reset();
        p1.setPosition(COURT_LEFT + 120, GROUND_Y - PLAYER_HEIGHT);
        p2.setPosition(COURT_RIGHT - 120, GROUND_Y - PLAYER_HEIGHT);
        ball.setPosition(NET_X - 120, GROUND_Y - 200);
        ball.vx = ball.vy = 0;
        initServe(p1);
        particles.clear();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocus();
        if (loopThread == null) {
            loopThread = new Thread(this, "GameLoop");
            loopThread.start();
        }
    }

    @Override
    public void run() {
        createBufferStrategy(3);
        BufferStrategy bs = getBufferStrategy();
        lastTime = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            double dt = (now - lastTime) / 1_000_000_000.0;
            if (dt > 0.05) dt = 0.05;
            lastTime = now;

            if (!paused) {
                update(dt);
            }

            do {
                do {
                    Graphics2D g = (Graphics2D) bs.getDrawGraphics();
                    try {
                        render(g);
                    } finally {
                        g.dispose();
                    }
                } while (bs.contentsRestored());
                bs.show();
            } while (bs.contentsLost());
            Toolkit.getDefaultToolkit().sync();
        }
        System.exit(0);
    }

    private void update(double dt) {
        handleInput(dt);

        if (aiEnabled) aiForP2(dt);

        p1.update(dt);
        p2.update(dt);

        // Ball logic
        switch (state) {
            case SERVE -> {
                ball.vx = ball.vy = 0;
                positionServeBall();
                ball.updateTrail(dt, false);
            }
            case PLAY -> {
                ball.updatePhysics(dt);
                ballCollisions();
                ball.updateTrail(dt, true);
            }
            case POINT -> {
                ball.updateTrail(dt, false);
            }
        }

        // Particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            if (!particles.get(i).update(dt)) {
                particles.remove(i);
            }
        }

        // Clouds
        for (Cloud c : clouds) c.update(dt);
    }

    private void handleInput(double dt) {
        // Player 1
        double dir1 = 0;
        if (keys.contains(KeyEvent.VK_A)) dir1 -= 1;
        if (keys.contains(KeyEvent.VK_D)) dir1 += 1;
        p1.vx = dir1 * PLAYER_SPEED;
        if (keys.contains(KeyEvent.VK_W)) p1.jump();
        if (keys.contains(KeyEvent.VK_F)) p1.trySwing();
        if (keys.contains(KeyEvent.VK_SPACE)) attemptServe(p1);

        // Player 2 (human if AI disabled)
        if (!aiEnabled) {
            double dir2 = 0;
            if (keys.contains(KeyEvent.VK_LEFT)) dir2 -= 1;
            if (keys.contains(KeyEvent.VK_RIGHT)) dir2 += 1;
            p2.vx = dir2 * PLAYER_SPEED;
            if (keys.contains(KeyEvent.VK_UP)) p2.jump();
            if (keys.contains(KeyEvent.VK_SLASH)) p2.trySwing();
            if (keys.contains(KeyEvent.VK_ENTER)) attemptServe(p2);
        }
    }

    private void aiForP2(double dt) {
        if (state == RallyState.SERVE && server == p2) {
            if (rng.nextDouble() < 0.015) attemptServe(p2);
            return;
        }
        double targetX = COURT_RIGHT - 140;
        if (state == RallyState.PLAY) {
            if (ball.vx > 0) {
                // Predict intercept
                double timeToGround;
                if (ball.vy >= 0) {
                    double h = (GROUND_Y - BALL_RADIUS) - ball.y;
                    if (h < 0) h = 0;
                    double a = 0.5 * GRAVITY;
                    double b = ball.vy;
                    double c = -h;
                    double disc = b*b - 4*a*c;
                    timeToGround = disc >= 0 ? (-b + Math.sqrt(disc)) / (2*a) : 0.6;
                } else {
                    double tApex = -ball.vy / GRAVITY;
                    double yApex = ball.y + ball.vy * tApex + 0.5 * GRAVITY * tApex * tApex;
                    double h = (GROUND_Y - BALL_RADIUS) - yApex;
                    double tDown = Math.sqrt(Math.max(0, 2*h / GRAVITY));
                    timeToGround = tApex + tDown;
                }
                targetX = ball.x + ball.vx * timeToGround * 0.94;
                targetX = Math.max(NET_X + 60, Math.min(COURT_RIGHT - 80, targetX));
            } else {
                targetX = Math.max(NET_X + 80, Math.min(COURT_RIGHT - 120, (COURT_RIGHT - 120 + NET_X + 100)/2));
            }
        }
        double dx = targetX - p2.x;
        if (Math.abs(dx) > 8) {
            p2.vx = Math.signum(dx) * PLAYER_SPEED * (0.9 + 0.2 * rng.nextDouble());
        } else {
            p2.vx = 0;
        }
        if (state == RallyState.PLAY && ball.vx > 0 && ball.y < p2.y - 30 && Math.abs(ball.x - p2.racketCenterX()) < 110) {
            p2.jump();
        }
        if (state == RallyState.PLAY && Math.abs(ball.x - p2.racketCenterX()) < 46 &&
                Math.abs(ball.y - p2.racketCenterY()) < 60 && ball.vx > 0) {
            p2.trySwing();
        }
    }

    private void ballCollisions() {
        // Ground
        if (ball.y + BALL_RADIUS >= GROUND_Y && ball.vy > 0) {
            ball.y = GROUND_Y - BALL_RADIUS - 0.1;
            ball.vy = -ball.vy * GROUND_RESTITUTION;
            ball.vx *= FRICTION;
            ball.bounceCountSinceHit++;
            if (ball.bounceCountSinceHit >= 2) {
                awardPoint(ball.lastHitter == p1 ? p2 : p1, "Double bounce");
            } else {
                // Dust particles
                for (int i = 0; i < 10; i++) {
                    particles.add(Particle.circle(ball.x, GROUND_Y - 4, 160 + rng.nextDouble()*240,
                            Math.toRadians(rng.nextDouble()*180 + 180), new Color(230,230,230,200), 0.45));
                }
            }
        }

        // Net
        if (Math.abs(ball.x - NET_X) < NET_HALF_THICK + BALL_RADIUS &&
                ball.y + BALL_RADIUS >= GROUND_Y - NET_HEIGHT) {
            if (Math.signum(ball.vx) == Math.signum(ball.x - NET_X)) {
                ball.x = NET_X + Math.signum(ball.x - NET_X) * (NET_HALF_THICK + BALL_RADIUS + 0.5);
            }
            ball.vx = -ball.vx * 0.45;
            ball.vy *= 0.85;
            for (int i = 0; i < 8; i++) {
                particles.add(Particle.circle(NET_X, ball.y, 140 + rng.nextDouble()*180,
                        rng.nextDouble() * Math.PI * 2, new Color(255,255,255,220), 0.3));
            }
        }

        // Out
        if (ball.x < COURT_LEFT - 30) {
            awardPoint(p2, "Out (left)");
        } else if (ball.x > COURT_RIGHT + 30) {
            awardPoint(p1, "Out (right)");
        }

        // Racket hits
        if (p1.tryHitBall(ball)) onHit(p1);
        if (p2.tryHitBall(ball)) onHit(p2);
    }

    private void onHit(Player hitter) {
        ball.lastHitter = hitter;
        ball.bounceCountSinceHit = 0;
        for (int i = 0; i < 14; i++) {
            particles.add(Particle.circle(ball.x, ball.y, 160 + rng.nextDouble()*240,
                    rng.nextDouble() * Math.PI * 2, tint(hitter.color, 1.0f, 1.0f, 0.9f), 0.45));
        }
    }

    private void render(Graphics2D g) {
        // Clear + AA
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawBackground(g);

        // Court base
        drawCourt(g);

        // Entities
        p1.draw(g);
        p2.draw(g);

        ball.drawTrail(g);
        ball.draw(g);

        // Particles
        for (Particle p : particles) p.draw(g);

        drawUI(g);
    }

    private void drawBackground(Graphics2D g) {
        // Sky gradient
        Paint sky = new GradientPaint(0, 0, new Color(0x3D6FF0),
                0, HEIGHT, new Color(0xE6F2FF));
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Sun
        RadialGradientPaint sun = new RadialGradientPaint(
                new Point2D.Double(140, 120), 90,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 242, 168, 255), new Color(255, 204, 51, 0)});
        g.setPaint(sun);
        g.fill(new Ellipse2D.Double(70, 50, 140, 140));

        // Clouds
        for (Cloud c : clouds) c.draw(g);

        // Brandenburg Gate: image or vector
        if (bgImage != null) {
            int h = (int)(HEIGHT * 0.6);
            int w = (int)(h * (bgImage.getWidth() / (double) bgImage.getHeight()));
            int x = (WIDTH - w) / 2;
            int y = (int)(HEIGHT * 0.42) - h + 40;
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
            g.drawImage(bgImage, x, y, w, h, null);
            g.setComposite(old);
        } else {
            drawBrandenburgGateVector(g, (int)(HEIGHT*0.42));
        }
    }

    private void drawCourt(Graphics2D g) {
        // Grass
        GradientPaint grass = new GradientPaint(0, (float)GROUND_Y, new Color(0x37A863),
                0, HEIGHT, new Color(0x22643D));
        g.setPaint(grass);
        g.fillRect(0, (int)GROUND_Y, WIDTH, HEIGHT - (int)GROUND_Y);

        // Court plate highlight
        g.setColor(new Color(248,248,248,50));
        g.fillRect((int)COURT_LEFT, (int)GROUND_Y - 8, (int)(COURT_RIGHT - COURT_LEFT), 10);

        // Baselines
        g.setStroke(new BasicStroke(4f));
        g.setColor(Color.WHITE);
        g.drawLine((int)COURT_LEFT, (int)GROUND_Y, (int)COURT_LEFT, (int)GROUND_Y - 2);
        g.drawLine((int)COURT_RIGHT, (int)GROUND_Y, (int)COURT_RIGHT, (int)GROUND_Y - 2);

        // Net posts
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect((int)(NET_X - NET_HALF_THICK - 3), (int)(GROUND_Y - NET_HEIGHT - 5), 6, (int)NET_HEIGHT + 10);
        g.fillRect((int)(NET_X + NET_HALF_THICK - 3), (int)(GROUND_Y - NET_HEIGHT - 5), 6, (int)NET_HEIGHT + 10);
        // Net band
        g.setColor(Color.WHITE);
        g.fillRoundRect((int)(NET_X - NET_HALF_THICK - 80), (int)(GROUND_Y - NET_HEIGHT - 8), 160, 14, 8, 8);
        // Net mesh
        g.setColor(new Color(255,255,255,165));
        for (int y = 0; y < NET_HEIGHT; y += 10) {
            g.drawLine((int)(NET_X - 85), (int)(GROUND_Y - NET_HEIGHT + y), (int)(NET_X + 85), (int)(GROUND_Y - NET_HEIGHT + y));
        }
        for (int x = -85; x <= 85; x += 10) {
            g.drawLine((int)(NET_X + x), (int)(GROUND_Y - NET_HEIGHT), (int)(NET_X + x), (int)GROUND_Y);
        }
    }

    private void drawBrandenburgGateVector(Graphics2D g, int baselineY) {
        // Stylized, recognizable silhouette with columns and quadriga
        int y = baselineY;
        AffineTransform at = g.getTransform();
        g.translate(0, y);

        // Wall base
        g.setColor(new Color(185, 66, 58, 230));
        g.fillRect(0, 140, WIDTH, 130);

        // Gate podium
        g.setColor(new Color(199, 176, 140, 230));
        g.fillRect((int)(WIDTH*0.2), 100, (int)(WIDTH*0.6), 30);

        // Entablature
        g.setColor(new Color(181, 160, 124, 240));
        g.fillRect((int)(WIDTH*0.2), 40, (int)(WIDTH*0.6), 20);

        // Columns (6)
        int cols = 6;
        int colW = (int)(WIDTH * 0.6 / (cols*2));
        int gap = colW;
        int startX = (int)(WIDTH*0.2) + gap/2;
        for (int i = 0; i < cols; i++) {
            int x = startX + i * (colW + gap);
            g.setColor(new Color(210, 190, 150, 240));
            g.fillRoundRect(x, 60, colW, 90, 6, 6);
            g.setColor(new Color(160, 140, 110, 80));
            g.drawRoundRect(x, 60, colW, 90, 6, 6);
        }

        // Quadriga silhouette
        g.setColor(new Color(90, 90, 90, 220));
        GeneralPath quad = new GeneralPath();
        int cx = WIDTH/2;
        quad.moveTo(cx - 40, 32);
        quad.curveTo(cx - 30, 18, cx - 22, 16, cx - 12, 16);
        quad.curveTo(cx - 6, 6, cx + 6, 6, cx + 12, 16);
        quad.curveTo(cx + 22, 16, cx + 30, 18, cx + 40, 32);
        quad.lineTo(cx + 24, 32);
        quad.lineTo(cx + 22, 26);
        quad.lineTo(cx + 12, 26);
        quad.lineTo(cx + 12, 32);
        quad.lineTo(cx - 12, 32);
        quad.lineTo(cx - 12, 26);
        quad.lineTo(cx - 22, 26);
        quad.lineTo(cx - 24, 32);
        quad.closePath();
        g.fill(quad);

        // Front wall merlons (stylized)
        g.setColor(new Color(167, 55, 47, 200));
        for (int i = 0; i < 26; i++) {
            int mx = 30 + i * 46;
            g.fillRoundRect(mx, 120, 26, 22, 8, 8);
        }

        g.setTransform(at);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.88f));
    }

    private static Color tint(Color c, float h, float s, float b) {
        float[] hs = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        return Color.getHSBColor(hs[0]*h, Math.min(1f, hs[1]*s), Math.min(1f, hs[2]*b));
    }

    private BufferedImage scaledImage(URL url) throws Exception {
        BufferedImage src = javax.imageio.ImageIO.read(url);
        if (src == null) return null;
        int targetH = (int)(HEIGHT * 0.6);
        int targetW = (int)(src.getWidth() * (targetH / (double)src.getHeight()));
        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, targetW, targetH, null);
        g.dispose();
        return out;
    }

    // Entities

    private class Player {
        final String name;
        double x, y, vx = 0, vy = 0;
        boolean onGround = true;
        final Color color, accent;
        final boolean leftSide;

        boolean swinging = false;
        long swingEndNanos = 0;

        Player(String name, double x, double y, Color color, Color accent, boolean leftSide) {
            this.name = name;
            this.x = x; this.y = y;
            this.color = color; this.accent = accent;
            this.leftSide = leftSide;
        }

        void setPosition(double nx, double ny) {
            x = nx; y = ny;
        }

        void jump() {
            if (onGround) {
                vy = PLAYER_JUMP;
                onGround = false;
            }
        }

        void trySwing() {
            if (swinging) return;
            swinging = true;
            swingEndNanos = System.nanoTime() + (long)(SWING_TIME_MS * 1_000_000);
        }

        boolean isSwingActive() {
            if (!swinging) return false;
            boolean active = System.nanoTime() < swingEndNanos;
            if (!active) swinging = false;
            return active;
        }

        double racketCenterX() {
            return x + (leftSide ? PLAYER_WIDTH + 26 : -26);
        }
        double racketCenterY() {
            return y + 48;
        }

        boolean tryHitBall(Ball b) {
            if (!isSwingActive() || state != RallyState.PLAY) return false;
            double dx = b.x - racketCenterX();
            double dy = b.y - racketCenterY();
            double dist2 = dx*dx + dy*dy;
            double rad = RACKET_RADIUS + BALL_RADIUS;
            if (dist2 <= rad * rad) {
                double dist = Math.sqrt(Math.max(1e-6, dist2));
                double nx = dx / dist;
                double ny = dy / dist;
                double base = 720 + rng.nextDouble() * 360;
                double dir = (this == p1) ? 1 : -1;
                nx = 0.7 * dir + 0.3 * nx;
                double topSpin = -Math.signum(dy) * 140;

                b.vx = clamp((nx) * base + vx * 0.35, -BALL_MAX_SPEED, BALL_MAX_SPEED);
                b.vy = clamp((ny) * base * 0.4 + topSpin, -BALL_MAX_SPEED, BALL_MAX_SPEED);
                if (b.y > GROUND_Y - 70) b.vy -= 120;

                // Feedback particles
                for (int i = 0; i < 14; i++) {
                    particles.add(Particle.circle(b.x, b.y, 160 + rng.nextDouble()*240,
                            rng.nextDouble() * Math.PI * 2, tint(color, 1f, 1f, 0.9f), 0.45));
                }
                return true;
            }
            return false;
        }

        void update(double dt) {
            // Horizontal move
            x += vx * dt;

            // Side boundaries
            double minX = leftSide ? COURT_LEFT + 40 : NET_X + 40;
            double maxX = leftSide ? NET_X - 40 : COURT_RIGHT - 40;
            x = clamp(x, minX, maxX);

            // Gravity
            vy += GRAVITY * dt;
            y += vy * dt;

            // Ground
            if (y + PLAYER_HEIGHT >= GROUND_Y) {
                y = GROUND_Y - PLAYER_HEIGHT;
                vy = 0;
                onGround = true;
            } else {
                onGround = false;
            }
        }

        void draw(Graphics2D g) {
            // Shadow
            g.setColor(new Color(0,0,0,80));
            g.fill(new Ellipse2D.Double(x + PLAYER_WIDTH*0.5 - 28, y + PLAYER_HEIGHT + 2, 56, 10));

            // Torso
            g.setColor(color);
            RoundRectangle2D torso = new RoundRectangle2D.Double(x, y, PLAYER_WIDTH, PLAYER_HEIGHT - 30, 8, 8);
            g.fill(torso);
            g.setColor(new Color(0,0,0,120));
            g.draw(torso);

            // Head
            g.setColor(accent);
            g.fill(new Ellipse2D.Double(x + PLAYER_WIDTH*0.5 - 16, y - 18, 32, 32));
            g.setColor(new Color(0,0,0,120));
            g.draw(new Ellipse2D.Double(x + PLAYER_WIDTH*0.5 - 16, y - 18, 32, 32));

            // Legs
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(color.darker());
            g.drawLine((int)(x + PLAYER_WIDTH*0.25), (int)(y + PLAYER_HEIGHT - 30), (int)(x + PLAYER_WIDTH*0.25), (int)(y + PLAYER_HEIGHT));
            g.drawLine((int)(x + PLAYER_WIDTH*0.75), (int)(y + PLAYER_HEIGHT - 30), (int)(x + PLAYER_WIDTH*0.75), (int)(y + PLAYER_HEIGHT));

            // Arms
            g.setColor(accent);
            g.drawLine((int)(x + PLAYER_WIDTH*0.2), (int)(y + 30), (int)(x + PLAYER_WIDTH*0.8), (int)(y + 30));
            g.drawLine((int)(x + PLAYER_WIDTH*0.8), (int)(y + 30), (int)(x + PLAYER_WIDTH*0.8), (int)(y + 60));

            // Racket
            double rx = leftSide ? x + PLAYER_WIDTH + 26 : x - 26;
            double ry = y + 48;
            // Simulate swing with small offset
            double swingPhase = 0;
            if (isSwingActive()) {
                double t = 1.0 - (swingEndNanos - System.nanoTime()) / (SWING_TIME_MS * 1_000_000.0);
                swingPhase = Math.sin(Math.PI * t) * 12 * (leftSide ? 1 : -1);
            }
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(139, 69, 19));
            g.drawLine((int)(x + PLAYER_WIDTH*0.8), (int)(y + 40), (int)(rx - (leftSide?8:-8)), (int)(ry));

            g.setStroke(new BasicStroke(3f));
            g.setColor(isSwingActive() ? Color.YELLOW : Color.WHITE);
            g.draw(new Ellipse2D.Double(rx - RACKET_RADIUS + swingPhase, ry - RACKET_RADIUS, RACKET_RADIUS*2, RACKET_RADIUS*2));
        }
    }

    private class Ball {
        double x, y, vx, vy;
        final Deque<TrailPoint> trail = new ArrayDeque<>();
        double trailTimer = 0;
        Player lastHitter = null;
        int bounceCountSinceHit = 0;

        Ball(double x, double y) { setPosition(x, y); }

        void setPosition(double nx, double ny) {
            x = nx; y = ny;
        }

        void updatePhysics(double dt) {
            vx *= (1 - AIR_DRAG);
            vy *= (1 - AIR_DRAG);
            vy += GRAVITY * dt;
            x += vx * dt;
            y += vy * dt;
        }

        void updateTrail(double dt, boolean active) {
            trailTimer += dt;
            if (active && trailTimer >= 0.025) {
                trailTimer = 0;
                trail.addLast(new TrailPoint(x, y, BALL_RADIUS * 0.75, 0.5));
                if (trail.size() > 26) trail.removeFirst();
                int idx = 0;
                for (TrailPoint p : trail) {
                    double a = 0.5 - 0.7 * (idx / (double) trail.size());
                    p.alpha = Math.max(0, a);
                    idx++;
                }
            } else if (!active && !trail.isEmpty()) {
                trail.removeFirst();
            }
        }

        void draw(Graphics2D g) {
            // Ball gradient
            Point2D center = new Point2D.Double(x, y);
            RadialGradientPaint rg = new RadialGradientPaint(center, (float)BALL_RADIUS,
                    new float[]{0f, 0.6f, 1f},
                    new Color[]{Color.WHITE, new Color(255, 239, 204), new Color(255, 179, 71)});
            g.setPaint(rg);
            g.fill(new Ellipse2D.Double(x - BALL_RADIUS, y - BALL_RADIUS, BALL_RADIUS*2, BALL_RADIUS*2));
            g.setColor(new Color(0,0,0,80));
            g.draw(new Ellipse2D.Double(x - BALL_RADIUS, y - BALL_RADIUS, BALL_RADIUS*2, BALL_RADIUS*2));
        }

        void drawTrail(Graphics2D g) {
            for (TrailPoint p : trail) {
                g.setColor(new Color(255, 217, 163, (int)(p.alpha * 255)));
                g.fill(new Ellipse2D.Double(p.x - p.size, p.y - p.size, p.size*2, p.size*2));
            }
        }
    }

    private static class TrailPoint {
        double x, y, size, alpha;
        TrailPoint(double x, double y, double size, double alpha) {
            this.x = x; this.y = y; this.size = size; this.alpha = alpha;
        }
    }

    private static class Particle {
        double x, y, vx, vy, life, maxLife, size;
        Color color;
        String text; // for floating text
        boolean textMode;

        static Particle circle(double x, double y, double speed, double angle, Color color, double lifeSec) {
            Particle p = new Particle();
            p.x = x; p.y = y;
            p.vx = Math.cos(angle) * speed * 0.3;
            p.vy = Math.sin(angle) * speed * 0.3 - 120;
            p.color = color;
            p.size = 3 + Math.random() * 3;
            p.life = p.maxLife = lifeSec;
            return p;
        }

        static Particle text(double x, double y, String s, Color c, double life) {
            Particle p = new Particle();
            p.x = x; p.y = y;
            p.vx = 0; p.vy = -10;
            p.text = s; p.color = c;
            p.textMode = true;
            p.life = p.maxLife = life;
            return p;
        }

        boolean update(double dt) {
            life -= dt;
            if (life <= 0) return false;
            x += vx * dt;
            y += vy * dt;
            vy += 200 * dt;
            return true;
        }

        void draw(Graphics2D g) {
            float a = (float)Math.max(0, life / maxLife);
            if (textMode) {
                Composite old = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
                g.setFont(new Font("Verdana", Font.BOLD, 22));
                g.setColor(color);
                g.drawString(text, (int)(x - text.length()*5), (int)y);
                g.setComposite(old);
            } else {
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(a * 255)));
                g.fill(new Ellipse2D.Double(x - size, y - size, size*2, size*2));
            }
        }
    }

    private class Cloud {
        double x, y, w, speed;
        Cloud(double x, double y, double w, double speed) {
            this.x = x; this.y = y; this.w = w; this.speed = speed;
        }
        void update(double dt) {
            x += speed * dt;
            if (x - w > WIDTH + 200) x = -200 - w;
        }
        void draw(Graphics2D g) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            g.setColor(new Color(255,255,255,220));
            double[] offs = {0, -8, -4, 2};
            double[] rs = {w*0.25, w*0.3, w*0.22, w*0.2};
            double accX = x;
            for (int i = 0; i < offs.length; i++) {
                g.fill(new Ellipse2D.Double(accX + (i==0?0:w*0.2*i) - rs[i], y + offs[i] - rs[i], rs[i]*2, rs[i]*2));
            }
            g.setComposite(old);
        }
    }

    private static class Score {
        int pointsP1 = 0, pointsP2 = 0;
        int gamesP1 = 0, gamesP2 = 0;
        int setsP1 = 0, setsP2 = 0;

        void reset() {
            pointsP1 = pointsP2 = gamesP1 = gamesP2 = setsP1 = setsP2 = 0;
        }

        void addPoint(boolean toP1) {
            if (toP1) {
                if (pointsP1 >= 3 && pointsP2 >= 3) {
                    if (pointsP1 == pointsP2) pointsP1++;
                    else if (pointsP1 == pointsP2 + 1) {
                        gamesP1++; pointsP1 = pointsP2 = 0;
                    } else {
                        pointsP2--;
                    }
                } else {
                    pointsP1++;
                    if (pointsP1 >= 4 && pointsP1 >= pointsP2 + 2) {
                        gamesP1++; pointsP1 = pointsP2 = 0;
                    }
                }
            } else {
                if (pointsP1 >= 3 && pointsP2 >= 3) {
                    if (pointsP1 == pointsP2) pointsP2++;
                    else if (pointsP2 == pointsP1 + 1) {
                        gamesP2++; pointsP1 = pointsP2 = 0;
                    } else {
                        pointsP1--;
                    }
                } else {
                    pointsP2++;
                    if (pointsP2 >= 4 && pointsP2 >= pointsP1 + 2) {
                        gamesP2++; pointsP1 = pointsP2 = 0;
                    }
                }
            }
            if ((gamesP1 >= 6 || gamesP2 >= 6) && Math.abs(gamesP1 - gamesP2) >= 2) {
                if (gamesP1 > gamesP2) setsP1++; else setsP2++;
                gamesP1 = gamesP2 = 0;
                pointsP1 = pointsP2 = 0;
            }
        }

        static String pointsToString(int p1, int p2) {
            if (p1 >= 3 && p2 >= 3) {
                if (p1 == p2) return "Deuce";
                if (p1 == p2 + 1) return "Adv P1";
                if (p2 == p1 + 1) return "Adv P2";
            }
            return fmt(p1) + " - " + fmt(p2);
        }

        private static String fmt(int p) {
            return switch (p) {
                case 0 -> "Love";
                case 1 -> "15";
                case 2 -> "30";
                case 3 -> "40";
                default -> Integer.toString(p);
            };
        }
    }

    private void drawUI(Graphics2D g) {
        // Glass panel
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        g.setColor(Color.BLACK);
        RoundRectangle2D rr = new RoundRectangle2D.Double(20, 20, 540, 110, 16, 16);
        g.fill(rr);
        g.setComposite(old);
        g.setColor(new Color(255,255,255,90));
        g.draw(rr);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Verdana", Font.BOLD, 24));
        g.drawString("Score: " + Score.pointsToString(score.pointsP1, score.pointsP2) +
                "   Games: " + score.gamesP1 + " - " + score.gamesP2, 36, 60);

        g.setFont(new Font("Verdana", Font.PLAIN, 18));
        g.drawString("Sets: " + score.setsP1 + " - " + score.setsP2 +
                "   Server: " + (server == p1 ? "P1" : "P2"), 36, 92);

        g.setFont(new Font("Verdana", Font.PLAIN, 16));
        g.drawString(aiEnabled ? "Mode: Player 1 vs Computer (F1 to toggle)" :
                "Mode: Player 1 vs Player 2 (F1 to toggle)", 580, 44);

        g.drawString("P1: A/D move, W jump, F swing, SPACE serve | P2: Left/Right, Up, / swing, ENTER serve | R reset, P pause, ESC exit", 580, 74);

        if (paused) {
            g.setFont(new Font("Verdana", Font.BOLD, 32));
            g.setColor(new Color(255,255,255,220));
            g.drawString("Paused (P to resume)", WIDTH/2 - 170, 120);
        } else if (state == RallyState.SERVE) {
            g.setFont(new Font("Verdana", Font.BOLD, 24));
            g.setColor(new Color(255,255,255,220));
            String sKey = server == p1 ? "SPACE" : (aiEnabled ? "AI" : "ENTER");
            g.drawString((server==p1?"P1":"P2") + " to serve (" + sKey + ")", WIDTH/2 - 140, 120);
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Tennis Match: Brandenburg Gate");
            TennisBrandenburgGame game = new TennisBrandenburgGame();
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.add(game);
            f.pack();
            f.setResizable(false);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}