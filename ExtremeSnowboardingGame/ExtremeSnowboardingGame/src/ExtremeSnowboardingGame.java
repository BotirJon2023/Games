import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ExtremeSnowboardingGame extends JPanel implements ActionListener, KeyListener {

    // =============================================================
    //  Constants – Game Tuning
    // =============================================================
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FPS = 60;
    private static final double GRAVITY = 0.48;
    private static final double FRICTION = 0.984;
    private static final double AIR_FRICTION = 0.995;
    private static final double MAX_SPEED = 18.5;
    private static final double MIN_SPEED = 2.8;
    private static final double JUMP_POWER = 11.2;
    private static final double ROTATION_SPEED = 4.8;
    private static final double LEAN_ANGLE_MAX = 38;
    private static final double CAMERA_LAG = 0.085;
    private static final double TERRAIN_SEGMENT_SIZE = 48;
    private static final int VISIBLE_SEGMENTS = 45;
    private static final int TERRAIN_HISTORY = 220;
    private static final Color SKY_TOP = new Color(68, 142, 245);
    private static final Color SKY_BOTTOM = new Color(145, 190, 255);
    private static final Color SNOW_COLOR = new Color(248, 248, 255);
    private static final Color SNOW_SHADOW = new Color(220, 225, 235);
    private static final Color BOARD_COLOR = new Color(30, 30, 36);
    private static final Color BINDING_COLOR = new Color(220, 40, 20);

    // =============================================================
    //  Game state variables
    // =============================================================
    private Timer timer;
    private double playerX = 400;
    private double playerY = 300;
    private double velocityX = 4.2;
    private double velocityY = 0;
    private double rotation = 0;           // degrees
    private double leanAngle = 0;
    private double targetLean = 0;
    private boolean onGround = true;
    private boolean crashed = false;
    private int score = 0;
    private int combo = 0;
    private int bestCombo = 0;
    private double distanceTraveled = 0;
    private double slowMotionFactor = 1.0;
    private double slowMotionTimer = 0;
    private Random random = new Random(42L);

    // Camera
    private double cameraY = 0;
    private double targetCameraY = 0;

    // Terrain (y-heights)
    private final ArrayDeque<Double> terrainHeights = new ArrayDeque<>();
    private final ArrayDeque<Double> terrainSlopes = new ArrayDeque<>();

    // Particles
    private final List<Particle> particles = new ArrayList<>();

    // Tricks & combo system
    private final Set<String> activeTricks = new HashSet<>();
    private String lastTrick = "";
    private double trickScoreMultiplier = 1.0;
    private int trickPointsThisJump = 0;

    // Input flags
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean spacePressed = false;
    private boolean jumpWasPressed = false; // edge detection
    private boolean xPressed = false;       // grab
    private boolean cPressed = false;       // spin
    private boolean zPressed = false;       // tweak

    // =============================================================
    //  Inner classes
    // =============================================================

    static class Particle {
        double x, y;
        double vx, vy;
        double life;
        double maxLife;
        Color color;
        double size;

        Particle(double x, double y, double vx, double vy, double life, Color c, double size) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.life = this.maxLife = life;
            this.color = c;
            this.size = size;
        }

        boolean update(double dt) {
            life -= dt;
            if (life <= 0) return false;
            x += vx * dt * 60;
            y += vy * dt * 60;
            vx *= 0.97;
            vy *= 0.97;
            vy += GRAVITY * 0.4 * dt * 60;
            return true;
        }

        void draw(Graphics2D g) {
            float alpha = (float)(life / maxLife);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 220)));
            g.fillOval((int)(x - size/2), (int)(y - size/2), (int)size, (int)size);
        }
    }

    // =============================================================
    //  Constructor & init
    // =============================================================

    public ExtremeSnowboardingGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        initializeTerrain();

        timer = new Timer(1000 / FPS, this);
        timer.start();
    }

    private void initializeTerrain() {
        terrainHeights.clear();
        terrainSlopes.clear();

        double currentY = HEIGHT * 0.65;
        double currentSlope = 0.02;

        for (int i = 0; i < TERRAIN_HISTORY + VISIBLE_SEGMENTS + 20; i++) {
            terrainHeights.addLast(currentY);
            terrainSlopes.addLast(currentSlope);

            currentSlope += random.nextGaussian() * 0.008 - 0.003;
            currentSlope = Math.max(-0.28, Math.min(0.32, currentSlope));

            currentY += currentSlope * TERRAIN_SEGMENT_SIZE;
            currentY = Math.max(120, Math.min(HEIGHT - 80, currentY));
        }
    }

    // =============================================================
    //  Main game loop (ActionListener)
    // =============================================================

    @Override
    public void actionPerformed(ActionEvent e) {
        if (crashed) {
            repaint();
            return;
        }

        double dt = 1.0 / FPS;
        if (slowMotionTimer > 0) {
            slowMotionFactor = 0.28;
            slowMotionTimer -= dt;
        } else {
            slowMotionFactor = 1.0;
        }

        dt *= slowMotionFactor;

        updateInput();
        updatePhysics(dt);
        updateTerrain();
        updateCamera();
        updateParticles(dt);
        updateTricksAndCombo();
        checkCollisions();
        updateScoreAndDistance();

        repaint();
    }

    private void updateInput() {
        if (leftPressed && !rightPressed) {
            targetLean = -LEAN_ANGLE_MAX;
            velocityX -= 0.38 * slowMotionFactor;
        } else if (rightPressed && !leftPressed) {
            targetLean = LEAN_ANGLE_MAX;
            velocityX += 0.38 * slowMotionFactor;
        } else {
            targetLean = 0;
        }

        if (upPressed) {
            velocityX += 0.22 * slowMotionFactor;
        }
        if (downPressed) {
            velocityX *= 0.965;
            targetLean *= 0.7;
        }

        velocityX = Math.max(MIN_SPEED, Math.min(MAX_SPEED, velocityX));

        // Jump
        if (spacePressed && !jumpWasPressed && onGround) {
            velocityY = -JUMP_POWER;
            onGround = false;
            createJumpDust();
        }
        jumpWasPressed = spacePressed;
    }

    private void updatePhysics(double dt) {
        // Rotation input
        if (cPressed) rotation += ROTATION_SPEED * dt * 60;
        if (zPressed) rotation -= ROTATION_SPEED * dt * 60;

        // Air control
        if (!onGround) {
            velocityX *= AIR_FRICTION;
            leanAngle = targetLean * 0.4;
        } else {
            leanAngle += (targetLean - leanAngle) * 0.22;
        }

        velocityY += GRAVITY * dt * 60;

        playerX += velocityX * dt * 60;
        playerY += velocityY * dt * 60;

        rotation = (rotation + 360) % 360;

        distanceTraveled += velocityX * dt * 0.8;
    }

    private void updateTerrain() {
        while (playerX > (terrainHeights.size() - VISIBLE_SEGMENTS) * TERRAIN_SEGMENT_SIZE * 0.6) {
            double lastY = terrainHeights.peekLast();
            double lastSlope = terrainSlopes.peekLast();

            double noise = random.nextGaussian() * 0.014 + Math.sin(distanceTraveled * 0.008) * 0.06;
            double newSlope = lastSlope + noise - 0.004;
            newSlope = Math.max(-0.32, Math.min(0.36, newSlope));

            double newY = lastY + newSlope * TERRAIN_SEGMENT_SIZE;

            terrainHeights.addLast(newY);
            terrainSlopes.addLast(newSlope);

            if (terrainHeights.size() > TERRAIN_HISTORY + 50) {
                terrainHeights.removeFirst();
                terrainSlopes.removeFirst();
            }
        }
    }

    private void updateCamera() {
        targetCameraY = playerY - HEIGHT / 2.8 + 80;
        cameraY += (targetCameraY - cameraY) * CAMERA_LAG;
    }

    private void updateParticles(double dt) {
        particles.removeIf(p -> !p.update(dt));

        if (onGround && Math.abs(velocityX) > 5.5) {
            if (random.nextInt(3) == 0) {
                double px = playerX + random.nextDouble() * 20 - 10;
                double py = getTerrainYAtX(playerX) + 8;
                double angle = leanAngle * 0.01745 + Math.PI / 2;
                double speed = velocityX * 0.7 + random.nextDouble() * 2;
                double vx = Math.cos(angle) * speed;
                double vy = Math.sin(angle) * speed - 1.2;
                particles.add(new Particle(px, py, vx, vy, 0.6 + random.nextDouble()*0.7,
                        SNOW_COLOR, 2.8 + random.nextDouble()*2.4));
            }
        }
    }

    private void updateTricksAndCombo() {
        if (!onGround) {
            if (xPressed && !activeTricks.contains("Grab")) {
                activeTricks.add("Grab");
                lastTrick = "Grab";
                trickPointsThisJump += 120;
                createSparkParticles(8, new Color(255,240,180));
            }
            if (Math.abs(rotation - 180) < 30 && !activeTricks.contains("Backflip")) {
                activeTricks.add("Backflip");
                lastTrick = "Backflip";
                trickPointsThisJump += 350;
                slowMotionTimer = 0.9;
                createSparkParticles(14, new Color(220,180,255));
            }
            if (Math.abs(rotation - 360) < 25 && !activeTricks.contains("360")) {
                activeTricks.add("360");
                lastTrick = "360";
                trickPointsThisJump += 280;
                createSparkParticles(10, new Color(180,255,220));
            }
        } else {
            if (!activeTricks.isEmpty()) {
                combo++;
                bestCombo = Math.max(bestCombo, combo);
                score += (int)(trickPointsThisJump * trickScoreMultiplier * (1 + combo * 0.15));
                trickScoreMultiplier += 0.12;
                activeTricks.clear();
                trickPointsThisJump = 0;
                slowMotionTimer = 0.6;
            }
        }
    }

    private void checkCollisions() {
        double terrainY = getTerrainYAtX(playerX);
        double boardBottomY = playerY + 38 * Math.cos(leanAngle * 0.01745);

        if (boardBottomY >= terrainY - 4 && velocityY >= 0) {
            if (!onGround) {
                land();
            }
            onGround = true;
            playerY = terrainY - 38 * Math.cos(leanAngle * 0.01745) - 4;
            velocityY = 0;

            double slopeAngle = getSlopeAngleAtX(playerX);
            double expectedVY = velocityX * Math.tan(slopeAngle);

            if (Math.abs(expectedVY) > 9.5 && Math.abs(leanAngle) < 12) {
                crash("Bad landing angle!");
            }
        } else {
            onGround = false;
        }
    }

    private void land() {
        if (trickPointsThisJump > 0) {
            createLandCelebrationParticles();
        }
        rotation = Math.round(rotation / 90) * 90 % 360;
        activeTricks.clear();
    }

    private void crash(String reason) {
        crashed = true;
        createExplosionParticles();
        System.out.println("Crash! " + reason + "   Score: " + score + "   Best combo: " + bestCombo);
        // You can add restart logic here
    }

    private void updateScoreAndDistance() {
        score += (int)(velocityX * 1.4);
    }

    // =============================================================
    //  Terrain helpers
    // =============================================================

    private double getTerrainYAtX(double worldX) {
        int seg = (int)(worldX / TERRAIN_SEGMENT_SIZE);
        if (seg < 0 || seg >= terrainHeights.size() - 1) return HEIGHT / 2.0;

        double frac = (worldX % TERRAIN_SEGMENT_SIZE) / TERRAIN_SEGMENT_SIZE;
        double y1 = terrainHeights.getLast();
        double ay2 = terrainHeights.getLast(seg + 1);
        return y1 + (y2 - y1) * frac;
    }

    private double getSlopeAngleAtX(double worldX) {
        int seg = (int)(worldX / TERRAIN_SEGMENT_SIZE);
        if (seg < 0 || seg >= terrainSlopes.size()) return 0;

        double atan = Math.atan(terrainSlopes.getLast(seg));
        return atan;
    }

    // =============================================================
    //  Particle effects
    // =============================================================

    private void createJumpDust() {
        for (int i = 0; i < 18; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 3 + random.nextDouble() * 5;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed - 4;
            particles.add(new Particle(playerX, playerY + 30, vx, vy, 0.8, SNOW_SHADOW, 3.2));
        }
    }

    private void createSparkParticles(int count, Color baseColor) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 5 + random.nextDouble() * 8;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed - 3;
            particles.add(new Particle(playerX, playerY - 20, vx, vy, 0.5 + random.nextDouble()*0.6, baseColor, 1.8));
        }
    }

    private void createLandCelebrationParticles() {
        for (int i = 0; i < 24; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 6 + random.nextDouble() * 7;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed - 8;
            particles.add(new Particle(playerX, playerY, vx, vy, 1.1, new Color(255,220,100), 2.5));
        }
    }

    private void createExplosionParticles() {
        for (int i = 0; i < 60; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 8 + random.nextDouble() * 12;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed - 6;
            Color c = random.nextBoolean() ? Color.ORANGE : Color.RED;
            particles.add(new Particle(playerX, playerY, vx, vy, 1.2 + random.nextDouble()*0.9, c, 3.8));
        }
    }

    // =============================================================
    //  Rendering
    // =============================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2);
        drawTerrain(g2);
        drawParticles(g2);
        drawPlayer(g2);
        drawUI(g2);

        if (crashed) {
            drawGameOver(g2);
        }
    }

    private void drawBackground(Graphics2D g) {
        GradientPaint sky = new GradientPaint(0, 0, SKY_TOP, 0, HEIGHT, SKY_BOTTOM);
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Distant mountains
        g.setColor(new Color(120, 140, 170, 140));
        int[] mx = {0, WIDTH/3, WIDTH*2/3, WIDTH};
        int[] my = {HEIGHT-180, HEIGHT-340, HEIGHT-290, HEIGHT-220};
        g.fillPolygon(mx, my, 4);
    }

    private void drawTerrain(Graphics2D g) {
        g.setColor(SNOW_COLOR);
        Path2D path = new Path2D.Double();
        path.moveTo(0, HEIGHT);

        double worldX = playerX - WIDTH / 2.0;
        int startSeg = (int)(worldX / TERRAIN_SEGMENT_SIZE) - 5;

        for (int i = 0; i < VISIBLE_SEGMENTS + 10; i++) {
            int idx = startSeg + i;
            if (idx < 0 || idx >= terrainHeights.size()) continue;

            double x = (idx * TERRAIN_SEGMENT_SIZE) - worldX + WIDTH / 2.0;
            double y = terrainHeights.get(idx) - cameraY;

            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }

        path.lineTo(WIDTH, HEIGHT);
        path.closePath();
        g.fill(path);

        // Snow shadows / layers
        g.setColor(SNOW_SHADOW);
        Path2D shadowPath = new Path2D.Double();
        shadowPath.moveTo(0, HEIGHT);

        for (int i = 0; i < VISIBLE_SEGMENTS + 10; i++) {
            int idx = startSeg + i;
            if (idx < 0 || idx >= terrainHeights.size()) continue;
            double x = (idx * TERRAIN_SEGMENT_SIZE) - worldX + WIDTH / 2.0;
            double y = terrainHeights.get(idx) - cameraY + 14;
            if (i == 0) shadowPath.moveTo(x, y);
            else shadowPath.lineTo(x, y);
        }
        shadowPath.lineTo(WIDTH, HEIGHT);
        shadowPath.closePath();
        g.fill(shadowPath);
    }

    private void drawParticles(Graphics2D g) {
        for (Particle p : particles) {
            p.draw(g);
        }
    }

    private void drawPlayer(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(playerX - (playerX - WIDTH/2.0) + WIDTH/2.0 - cameraY * 0.0, playerY - cameraY);

        AffineTransform at = new AffineTransform();
        at.rotate(Math.toRadians(rotation + leanAngle * 0.4));
        g2.transform(at);

        // Board
        g2.setColor(BOARD_COLOR);
        g2.fillRoundRect(-64, 12, 128, 18, 24, 24);

        // Bindings
        g2.setColor(BINDING_COLOR);
        g2.fillRect(-44, -8, 28, 38);
        g2.fillRect( 16, -8, 28, 38);

        // Body (simple capsule)
        g2.setColor(new Color(40, 100, 220));
        g2.fillOval(-22, -60, 44, 80);

        // Head
        g2.setColor(new Color(255, 220, 180));
        g2.fillOval(-16, -80, 32, 32);
        g2.setColor(Color.BLACK);
        g2.fillOval(-8, -72, 6, 6);
        g2.fillOval( 2, -72, 6, 6);

        // Helmet
        g2.setColor(new Color(220, 40, 20));
        g2.fillArc(-18, -84, 36, 40, 0, 180);

        // Trick name popup
        if (!lastTrick.isEmpty() && System.currentTimeMillis() % 1200 < 800) {
            g2.setFont(new Font("Arial", Font.BOLD, 22));
            g2.setColor(new Color(255,255,100,180));
            g2.drawString(lastTrick, -lastTrick.length()*6, -110);
        }

        g2.dispose();
    }

    private void drawUI(Graphics2D g) {
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(Color.WHITE);

        g.drawString("Score: " + score, 30, 50);
        g.drawString("Combo: " + combo + "×", 30, 90);
        g.drawString("Best: " + bestCombo, 30, 130);
        g.drawString(String.format("Dist: %.1f m", distanceTraveled), WIDTH - 220, 50);

        if (slowMotionTimer > 0) {
            g.setColor(new Color(255, 80, 80, 140));
            g.setFont(new Font("Arial", Font.BOLD, 60));
            g.drawString("SLOW MOTION", WIDTH/2 - 220, HEIGHT/2 - 80);
        }
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setFont(new Font("Arial", Font.BOLD, 72));
        g.setColor(Color.RED);
        g.drawString("CRASH!", WIDTH/2 - 180, HEIGHT/2 - 80);

        g.setFont(new Font("Arial", Font.BOLD, 38));
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, WIDTH/2 - 120, HEIGHT/2 + 20);
        g.drawString("Best Combo: " + bestCombo, WIDTH/2 - 160, HEIGHT/2 + 80);

        g.setFont(new Font("Arial", Font.PLAIN, 28));
        g.drawString("Press R to restart", WIDTH/2 - 140, HEIGHT/2 + 160);
    }

    // =============================================================
    //  Input handling
    // =============================================================

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT  -> leftPressed  = true;
            case KeyEvent.VK_RIGHT -> rightPressed = true;
            case KeyEvent.VK_UP    -> upPressed    = true;
            case KeyEvent.VK_DOWN  -> downPressed  = true;
            case KeyEvent.VK_SPACE -> spacePressed = true;
            case KeyEvent.VK_X     -> xPressed     = true;
            case KeyEvent.VK_C     -> cPressed     = true;
            case KeyEvent.VK_Z     -> zPressed     = true;
            case KeyEvent.VK_R     -> { if (crashed) resetGame(); }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT  -> leftPressed  = false;
            case KeyEvent.VK_RIGHT -> rightPressed = false;
            case KeyEvent.VK_UP    -> upPressed    = false;
            case KeyEvent.VK_DOWN  -> downPressed  = false;
            case KeyEvent.VK_SPACE -> spacePressed = false;
            case KeyEvent.VK_X     -> xPressed     = false;
            case KeyEvent.VK_C     -> cPressed     = false;
            case KeyEvent.VK_Z     -> zPressed     = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void resetGame() {
        playerX = 400;
        playerY = 300;
        velocityX = 4.2;
        velocityY = 0;
        rotation = 0;
        leanAngle = 0;
        targetLean = 0;
        onGround = true;
        crashed = false;
        score = 0;
        combo = 0;
        distanceTraveled = 0;
        slowMotionTimer = 0;
        particles.clear();
        activeTricks.clear();
        lastTrick = "";
        initializeTerrain();
    }

    // =============================================================
    //  Main method
    // =============================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Extreme Snowboarding Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new ExtremeSnowboardingGame());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }
}