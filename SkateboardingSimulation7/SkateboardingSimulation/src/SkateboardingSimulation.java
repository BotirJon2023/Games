import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class SkateboardingSimulation extends JFrame {

    private GamePanel gamePanel;
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 700;

    public SkateboardingSimulation() {
        setTitle("🛹 Skateboarding Simulation - Ultimate Edition");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        setVisible(true);
        gamePanel.startGame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SkateboardingSimulation());
    }
}

/**
 * Main game panel handling all game logic, rendering, and input
 */
class GamePanel extends JPanel implements ActionListener, KeyListener {

    // Game state
    private javax.swing.Timer gameTimer;
    private boolean gameRunning = true;
    private boolean gamePaused = false;
    private int score = 0;
    private int highScore = 0;
    private int comboMultiplier = 1;
    private long comboStartTime = 0;
    private static final long COMBO_TIMEOUT = 2000;
    private List<String> currentCombo = new ArrayList<>();

    // Skater properties
    private Skater skater;

    // Level elements
    private List<Obstacle> obstacles = new ArrayList<>();
    private List<Ramp> ramps = new ArrayList<>();
    private List<Rail> rails = new ArrayList<>();
    private List<Collectible> collectibles = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();
    private List<ScorePopup> scorePopups = new ArrayList<>();

    // Camera
    private double cameraX = 0;
    private double cameraY = 0;
    private int cameraMode = 1; // 1: Follow, 2: Static, 3: Dynamic

    // Level properties
    private double levelWidth = 5000;
    private double groundLevel;

    // Visual effects
    private float sunAngle = 0;
    private List<Cloud> clouds = new ArrayList<>();
    private Color skyColorTop = new Color(135, 206, 235);
    private Color skyColorBottom = new Color(255, 200, 150);

    // Input state
    private Set<Integer> pressedKeys = new HashSet<>();

    // Fonts
    private Font gameFont;
    private Font comboFont;
    private Font trickFont;

    // Time tracking
    private long lastUpdateTime;
    private double deltaTime;

    public GamePanel() {
        setPreferredSize(new Dimension(1200, 700));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        groundLevel = 550;

        initializeFonts();
        initializeLevel();
        initializeSkater();
        initializeClouds();

        gameTimer = new javax.swing.Timer(16, this); // ~60 FPS
        lastUpdateTime = System.nanoTime();
    }

    private void initializeFonts() {
        gameFont = new Font("Verdana", Font.BOLD, 24);
        comboFont = new Font("Impact", Font.PLAIN, 36);
        trickFont = new Font("Arial", Font.BOLD, 18);
    }

    private void initializeSkater() {
        skater = new Skater(200, groundLevel - 60);
    }

    private void initializeLevel() {
        // Create obstacles
        obstacles.add(new Obstacle(500, groundLevel - 40, 100, 40, ObstacleType.BOX));
        obstacles.add(new Obstacle(800, groundLevel - 30, 80, 30, ObstacleType.BOX));
        obstacles.add(new Obstacle(1400, groundLevel - 50, 120, 50, ObstacleType.BOX));
        obstacles.add(new Obstacle(2000, groundLevel - 35, 90, 35, ObstacleType.BENCH));
        obstacles.add(new Obstacle(2600, groundLevel - 45, 110, 45, ObstacleType.BOX));
        obstacles.add(new Obstacle(3200, groundLevel - 60, 150, 60, ObstacleType.PLATFORM));
        obstacles.add(new Obstacle(3800, groundLevel - 40, 100, 40, ObstacleType.BOX));
        obstacles.add(new Obstacle(4400, groundLevel - 55, 130, 55, ObstacleType.PLATFORM));

        // Create ramps
        ramps.add(new Ramp(1000, groundLevel, 150, 80, RampType.QUARTER_PIPE));
        ramps.add(new Ramp(1700, groundLevel, 120, 60, RampType.KICKER));
        ramps.add(new Ramp(2300, groundLevel, 180, 100, RampType.HALF_PIPE));
        ramps.add(new Ramp(2900, groundLevel, 100, 50, RampType.KICKER));
        ramps.add(new Ramp(3500, groundLevel, 200, 120, RampType.QUARTER_PIPE));
        ramps.add(new Ramp(4100, groundLevel, 140, 70, RampType.KICKER));

        // Create rails
        rails.add(new Rail(600, groundLevel - 50, 200, 10));
        rails.add(new Rail(1250, groundLevel - 60, 180, 10));
        rails.add(new Rail(1900, groundLevel - 45, 220, 10));
        rails.add(new Rail(2750, groundLevel - 55, 190, 10));
        rails.add(new Rail(3350, groundLevel - 50, 210, 10));
        rails.add(new Rail(4000, groundLevel - 65, 250, 10));

        // Create collectibles
        for (int i = 0; i < 30; i++) {
            double x = 300 + Math.random() * (levelWidth - 600);
            double y = groundLevel - 100 - Math.random() * 200;
            collectibles.add(new Collectible(x, y, CollectibleType.values()[(int)(Math.random() * 3)]));
        }
    }

    private void initializeClouds() {
        for (int i = 0; i < 8; i++) {
            clouds.add(new Cloud(Math.random() * levelWidth, 50 + Math.random() * 150,
                    80 + Math.random() * 120, 30 + Math.random() * 40));
        }
    }

    public void startGame() {
        gameTimer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gamePaused && gameRunning) {
            long currentTime = System.nanoTime();
            deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0;
            lastUpdateTime = currentTime;

            update(deltaTime);
        }
        repaint();
    }

    private void update(double dt) {
        // Update skater
        updateSkater(dt);

        // Update camera
        updateCamera(dt);

        // Update particles
        updateParticles(dt);

        // Update score popups
        updateScorePopups(dt);

        // Update clouds
        updateClouds(dt);

        // Update collectibles
        updateCollectibles(dt);

        // Check combo timeout
        checkComboTimeout();

        // Update sun angle for lighting effects
        sunAngle += 0.001f;
    }

    private void updateSkater(double dt) {
        // Handle input
        handleInput(dt);

        // Apply physics
        skater.update(dt, groundLevel, obstacles, ramps, rails, this);

        // Check boundaries
        if (skater.x < 50) skater.x = 50;
        if (skater.x > levelWidth - 50) skater.x = levelWidth - 50;

        // Generate trail particles when moving fast
        if (Math.abs(skater.velocityX) > 200 && skater.isOnGround && Math.random() < 0.3) {
            particles.add(new Particle(skater.x, skater.y + skater.height,
                    ParticleType.DUST,
                    -skater.velocityX * 0.1, -20 - Math.random() * 30));
        }
    }

    private void handleInput(double dt) {
        // Movement
        if (pressedKeys.contains(KeyEvent.VK_LEFT)) {
            skater.moveLeft(dt);
        }
        if (pressedKeys.contains(KeyEvent.VK_RIGHT)) {
            skater.moveRight(dt);
        }
        if (pressedKeys.contains(KeyEvent.VK_DOWN)) {
            skater.crouch();
        } else {
            skater.standUp();
        }

        // Air rotation
        if (!skater.isOnGround) {
            if (pressedKeys.contains(KeyEvent.VK_A)) {
                skater.rotateLeft(dt);
            }
            if (pressedKeys.contains(KeyEvent.VK_D)) {
                skater.rotateRight(dt);
            }
        }
    }

    private void updateCamera(double dt) {
        double targetX, targetY;

        switch (cameraMode) {
            case 1: // Follow mode
                targetX = skater.x - getWidth() / 2;
                targetY = Math.min(0, skater.y - getHeight() / 2 + 100);
                break;
            case 2: // Static mode
                targetX = Math.floor(skater.x / getWidth()) * getWidth();
                targetY = 0;
                break;
            case 3: // Dynamic mode
                targetX = skater.x - getWidth() / 3;
                targetY = Math.min(0, skater.y - getHeight() / 2);
                if (skater.velocityX > 0) targetX -= 100;
                else if (skater.velocityX < 0) targetX += 100;
                break;
            default:
                targetX = cameraX;
                targetY = cameraY;
        }

        // Smooth camera movement
        cameraX += (targetX - cameraX) * 5 * dt;
        cameraY += (targetY - cameraY) * 5 * dt;

        // Clamp camera
        cameraX = Math.max(0, Math.min(cameraX, levelWidth - getWidth()));
    }

    private void updateParticles(double dt) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(dt);
            if (p.isDead()) {
                it.remove();
            }
        }
    }

    private void updateScorePopups(double dt) {
        Iterator<ScorePopup> it = scorePopups.iterator();
        while (it.hasNext()) {
            ScorePopup sp = it.next();
            sp.update(dt);
            if (sp.isDead()) {
                it.remove();
            }
        }
    }

    private void updateClouds(double dt) {
        for (Cloud cloud : clouds) {
            cloud.x += cloud.speed * dt;
            if (cloud.x > levelWidth + 200) {
                cloud.x = -cloud.width;
            }
        }
    }

    private void updateCollectibles(double dt) {
        Iterator<Collectible> it = collectibles.iterator();
        while (it.hasNext()) {
            Collectible c = it.next();
            c.update(dt);

            // Check collision with skater
            if (c.isActive && skater.getBounds().intersects(c.getBounds())) {
                c.collect();
                int points = c.getPoints();
                addScore(points, c.type.name());

                // Create sparkle particles
                for (int i = 0; i < 10; i++) {
                    particles.add(new Particle(c.x, c.y, ParticleType.SPARKLE,
                            (Math.random() - 0.5) * 100,
                            (Math.random() - 0.5) * 100));
                }
            }
        }
    }

    private void checkComboTimeout() {
        if (!currentCombo.isEmpty() && System.currentTimeMillis() - comboStartTime > COMBO_TIMEOUT) {
            finalizeCombo();
        }
    }

    public void addTrickToCombo(String trickName, int basePoints) {
        currentCombo.add(trickName);
        comboMultiplier = Math.min(currentCombo.size(), 10);
        comboStartTime = System.currentTimeMillis();

        int points = basePoints * comboMultiplier;
        addScore(points, trickName);
    }

    public void addScore(int points, String reason) {
        score += points;
        if (score > highScore) {
            highScore = score;
        }

        scorePopups.add(new ScorePopup(skater.x, skater.y - 50, "+" + points + " " + reason, comboMultiplier));
    }

    private void finalizeCombo() {
        if (currentCombo.size() >= 2) {
            int bonus = currentCombo.size() * 50 * comboMultiplier;
            score += bonus;
            scorePopups.add(new ScorePopup(skater.x, skater.y - 80,
                    "COMBO x" + comboMultiplier + "! +" + bonus, comboMultiplier + 1));
        }
        currentCombo.clear();
        comboMultiplier = 1;
    }

    public void createLandingParticles() {
        for (int i = 0; i < 15; i++) {
            particles.add(new Particle(skater.x + (Math.random() - 0.5) * skater.width,
                    skater.y + skater.height,
                    ParticleType.DUST,
                    (Math.random() - 0.5) * 80,
                    -Math.random() * 50));
        }
    }

    public void createGrindSparks() {
        for (int i = 0; i < 3; i++) {
            particles.add(new Particle(skater.x + (Math.random() - 0.5) * 20,
                    skater.y + skater.height,
                    ParticleType.SPARK,
                    (Math.random() - 0.5) * 60,
                    -Math.random() * 40));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw background
        drawBackground(g2d);

        // Apply camera transform
        g2d.translate(-cameraX, -cameraY);

        // Draw level elements
        drawClouds(g2d);
        drawGround(g2d);
        drawRamps(g2d);
        drawRails(g2d);
        drawObstacles(g2d);
        drawCollectibles(g2d);

        // Draw skater
        skater.draw(g2d);

        // Draw particles
        drawParticles(g2d);

        // Draw score popups
        drawScorePopups(g2d);

        // Reset transform for UI
        g2d.translate(cameraX, cameraY);

        // Draw UI
        drawUI(g2d);

        // Draw pause screen if paused
        if (gamePaused) {
            drawPauseScreen(g2d);
        }
    }

    private void drawBackground(Graphics2D g2d) {
        // Gradient sky
        GradientPaint sky = new GradientPaint(0, 0, skyColorTop, 0, getHeight(), skyColorBottom);
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Sun
        int sunX = getWidth() - 150;
        int sunY = 100;

        // Sun glow
        for (int i = 5; i >= 0; i--) {
            float alpha = 0.1f - i * 0.015f;
            g2d.setColor(new Color(255, 200, 100, (int)(alpha * 255)));
            int size = 80 + i * 30;
            g2d.fillOval(sunX - size/2, sunY - size/2, size, size);
        }

        // Sun core
        g2d.setColor(new Color(255, 220, 150));
        g2d.fillOval(sunX - 40, sunY - 40, 80, 80);

        // Mountains in background
        drawMountains(g2d);
    }

    private void drawMountains(Graphics2D g2d) {
        // Far mountains
        g2d.setColor(new Color(100, 120, 140, 150));
        int[] xPoints1 = {0, 200, 400, 600, 800, 1000, 1200, 1200, 0};
        int[] yPoints1 = {400, 300, 350, 280, 320, 290, 380, 500, 500};
        g2d.fillPolygon(xPoints1, yPoints1, 9);

        // Near mountains
        g2d.setColor(new Color(80, 100, 120, 180));
        int[] xPoints2 = {0, 150, 350, 500, 700, 900, 1100, 1200, 1200, 0};
        int[] yPoints2 = {450, 380, 420, 350, 400, 360, 410, 430, 550, 550};
        g2d.fillPolygon(xPoints2, yPoints2, 10);
    }

    private void drawClouds(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 200));
        for (Cloud cloud : clouds) {
            drawCloud(g2d, cloud.x, cloud.y, cloud.width, cloud.height);
        }
    }

    private void drawCloud(Graphics2D g2d, double x, double y, double width, double height) {
        Ellipse2D.Double main = new Ellipse2D.Double(x, y, width, height);
        Ellipse2D.Double left = new Ellipse2D.Double(x - width * 0.3, y + height * 0.2, width * 0.6, height * 0.8);
        Ellipse2D.Double right = new Ellipse2D.Double(x + width * 0.5, y + height * 0.3, width * 0.5, height * 0.7);
        Ellipse2D.Double top = new Ellipse2D.Double(x + width * 0.2, y - height * 0.3, width * 0.5, height * 0.8);

        g2d.fill(main);
        g2d.fill(left);
        g2d.fill(right);
        g2d.fill(top);
    }

    private void drawGround(Graphics2D g2d) {
        // Main ground
        GradientPaint groundGradient = new GradientPaint(
                0, (float)groundLevel, new Color(80, 80, 80),
                0, (float)groundLevel + 200, new Color(40, 40, 40)
        );
        g2d.setPaint(groundGradient);
        g2d.fillRect(0, (int)groundLevel, (int)levelWidth, 300);

        // Concrete texture lines
        g2d.setColor(new Color(100, 100, 100));
        g2d.setStroke(new BasicStroke(2));
        for (int i = 0; i < levelWidth; i += 200) {
            g2d.drawLine(i, (int)groundLevel, i, (int)groundLevel + 300);
        }

        // Ground edge
        g2d.setColor(new Color(60, 60, 60));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(0, (int)groundLevel, (int)levelWidth, (int)groundLevel);
    }

    private void drawRamps(Graphics2D g2d) {
        for (Ramp ramp : ramps) {
            ramp.draw(g2d);
        }
    }

    private void drawRails(Graphics2D g2d) {
        for (Rail rail : rails) {
            rail.draw(g2d);
        }
    }

    private void drawObstacles(Graphics2D g2d) {
        for (Obstacle obs : obstacles) {
            obs.draw(g2d);
        }
    }

    private void drawCollectibles(Graphics2D g2d) {
        for (Collectible c : collectibles) {
            if (c.isActive) {
                c.draw(g2d);
            }
        }
    }

    private void drawParticles(Graphics2D g2d) {
        for (Particle p : particles) {
            p.draw(g2d);
        }
    }

    private void drawScorePopups(Graphics2D g2d) {
        for (ScorePopup sp : scorePopups) {
            sp.draw(g2d);
        }
    }

    private void drawUI(Graphics2D g2d) {
        // Score panel background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 250, 100, 15, 15);

        // Score
        g2d.setFont(gameFont);
        g2d.setColor(Color.WHITE);
        g2d.drawString("SCORE: " + score, 25, 45);

        // High score
        g2d.setFont(new Font("Verdana", Font.PLAIN, 16));
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawString("HIGH: " + highScore, 25, 70);

        // Combo display
        if (!currentCombo.isEmpty()) {
            long timeLeft = COMBO_TIMEOUT - (System.currentTimeMillis() - comboStartTime);
            float progress = Math.max(0, timeLeft / (float)COMBO_TIMEOUT);

            // Combo bar background
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRoundRect(10, 120, 250, 60, 10, 10);

            // Combo bar
            g2d.setColor(new Color(255, 200, 0, 200));
            g2d.fillRoundRect(15, 150, (int)(240 * progress), 20, 5, 5);

            // Combo text
            g2d.setFont(comboFont);
            g2d.setColor(Color.YELLOW);
            g2d.drawString("x" + comboMultiplier, 25, 148);

            // Current combo tricks
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.setColor(Color.WHITE);
            String comboStr = String.join(" + ", currentCombo);
            if (comboStr.length() > 30) comboStr = comboStr.substring(0, 27) + "...";
            g2d.drawString(comboStr, 80, 148);
        }

        // Current trick display
        if (skater.currentTrick != null && !skater.currentTrick.isEmpty()) {
            g2d.setFont(trickFont);
            g2d.setColor(new Color(255, 255, 0, 230));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(skater.currentTrick);
            g2d.drawString(skater.currentTrick, getWidth()/2 - textWidth/2, 50);
        }

        // Speed indicator
        double speed = Math.abs(skater.velocityX);
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(getWidth() - 160, 10, 150, 40, 10, 10);

        g2d.setFont(new Font("Verdana", Font.PLAIN, 14));
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.format("SPEED: %.0f", speed), getWidth() - 145, 35);

        // Speed bar
        float speedPercent = (float)Math.min(speed / 500, 1.0);
        Color speedColor = new Color(
                (int)(255 * speedPercent),
                (int)(255 * (1 - speedPercent * 0.5)),
                0
        );
        g2d.setColor(speedColor);
        g2d.fillRect(getWidth() - 145, 42, (int)(130 * speedPercent), 5);

        // Controls help
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRoundRect(getWidth() - 300, getHeight() - 120, 290, 110, 10, 10);

        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.setColor(new Color(200, 200, 200));
        String[] controls = {
                "← → Move | ↓ Crouch | SPACE Jump",
                "Z Kickflip | X Heelflip | C 360Flip | V Shuvit",
                "S Manual | G Grab | A/D Rotate",
                "R Reset | P Pause | 1-3 Camera"
        };
        for (int i = 0; i < controls.length; i++) {
            g2d.drawString(controls[i], getWidth() - 290, getHeight() - 95 + i * 25);
        }

        // Camera mode indicator
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(getWidth() - 160, 60, 150, 30, 10, 10);
        g2d.setFont(new Font("Verdana", Font.PLAIN, 12));
        g2d.setColor(Color.WHITE);
        String[] cameraModes = {"FOLLOW", "STATIC", "DYNAMIC"};
        g2d.drawString("CAM: " + cameraModes[cameraMode - 1], getWidth() - 145, 80);
    }

    private void drawPauseScreen(Graphics2D g2d) {
        // Dim background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Pause text
        g2d.setFont(new Font("Impact", Font.PLAIN, 72));
        g2d.setColor(Color.WHITE);
        String pauseText = "PAUSED";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(pauseText);
        g2d.drawString(pauseText, getWidth()/2 - textWidth/2, getHeight()/2);

        // Sub text
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String subText = "Press P to continue";
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth(subText);
        g2d.drawString(subText, getWidth()/2 - textWidth/2, getHeight()/2 + 50);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys.add(e.getKeyCode());

        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE:
                skater.jump();
                break;
            case KeyEvent.VK_Z:
                skater.performTrick(TrickType.KICKFLIP, this);
                break;
            case KeyEvent.VK_X:
                skater.performTrick(TrickType.HEELFLIP, this);
                break;
            case KeyEvent.VK_C:
                skater.performTrick(TrickType.TREFLIP, this);
                break;
            case KeyEvent.VK_V:
                skater.performTrick(TrickType.SHUVIT, this);
                break;
            case KeyEvent.VK_S:
                skater.performTrick(TrickType.MANUAL, this);
                break;
            case KeyEvent.VK_G:
                skater.performTrick(TrickType.GRAB, this);
                break;
            case KeyEvent.VK_R:
                resetSkater();
                break;
            case KeyEvent.VK_P:
                gamePaused = !gamePaused;
                break;
            case KeyEvent.VK_1:
                cameraMode = 1;
                break;
            case KeyEvent.VK_2:
                cameraMode = 2;
                break;
            case KeyEvent.VK_3:
                cameraMode = 3;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void resetSkater() {
        skater = new Skater(200, groundLevel - 60);
        finalizeCombo();
        cameraX = 0;
    }
}

/**
 * Skater class handling player character physics and animations
 */
class Skater {
    double x, y;
    double velocityX = 0, velocityY = 0;
    double width = 40, height = 60;
    double rotation = 0;
    double rotationVelocity = 0;

    boolean isOnGround = true;
    boolean isCrouching = false;
    boolean isGrinding = false;
    boolean isInManual = false;

    String currentTrick = "";
    private long trickStartTime = 0;
    private TrickType activeTrick = null;
    private double trickProgress = 0;

    // Physics constants
    private static final double GRAVITY = 1200;
    private static final double MOVE_SPEED = 400;
    private static final double JUMP_POWER = 550;
    private static final double FRICTION = 0.98;
    private static final double AIR_RESISTANCE = 0.995;
    private static final double MAX_SPEED = 600;

    // Animation
    private double boardRotation = 0;
    private double legAngle = 0;
    private double armAngle = 0;

    public Skater(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void update(double dt, double groundLevel, List<Obstacle> obstacles,
                       List<Ramp> ramps, List<Rail> rails, GamePanel game) {
        // Apply gravity if not on ground
        if (!isOnGround) {
            velocityY += GRAVITY * dt;
        }

        // Apply movement
        x += velocityX * dt;
        y += velocityY * dt;

        // Apply rotation in air
        if (!isOnGround) {
            rotation += rotationVelocity * dt;
        } else {
            // Smoothly return to upright
            rotation *= 0.9;
            rotationVelocity *= 0.8;
        }

        // Apply friction/air resistance
        if (isOnGround) {
            velocityX *= FRICTION;
        } else {
            velocityX *= AIR_RESISTANCE;
        }

        // Clamp speed
        velocityX = Math.max(-MAX_SPEED, Math.min(MAX_SPEED, velocityX));

        // Ground collision
        if (y >= groundLevel - height) {
            if (!isOnGround && velocityY > 100) {
                game.createLandingParticles();
                completeTrick(game);
            }
            y = groundLevel - height;
            velocityY = 0;
            isOnGround = true;
            isGrinding = false;
        } else {
            isOnGround = false;
        }

        // Check ramp collisions
        for (Ramp ramp : ramps) {
            if (ramp.checkCollision(this)) {
                ramp.applyEffect(this);
            }
        }

        // Check rail collisions
        for (Rail rail : rails) {
            if (!isOnGround && rail.checkCollision(this)) {
                isGrinding = true;
                y = rail.y - height;
                velocityY = 0;

                // Grinding particles
                game.createGrindSparks();

                // Add grind points
                if (currentTrick.isEmpty() || !currentTrick.contains("GRIND")) {
                    currentTrick = "50-50 GRIND";
                    game.addTrickToCombo("GRIND", 10);
                }
            }
        }

        // Check obstacle collisions
        for (Obstacle obs : obstacles) {
            if (obs.checkCollision(this)) {
                obs.resolveCollision(this);
            }
        }

        // Update trick animation
        updateTrickAnimation(dt);

        // Update animations
        updateAnimations(dt);
    }

    private void updateTrickAnimation(double dt) {
        if (activeTrick != null) {
            trickProgress += dt * 3; // Trick speed

            switch (activeTrick) {
                case KICKFLIP:
                case HEELFLIP:
                    boardRotation += dt * 1500;
                    break;
                case TREFLIP:
                    boardRotation += dt * 2000;
                    rotation += dt * 200;
                    break;
                case SHUVIT:
                    boardRotation += dt * 800;
                    break;
                case GRAB:
                    legAngle = Math.sin(trickProgress * 2) * 30;
                    break;
                default:
                    break;
            }

            if (trickProgress >= 1.0) {
                boardRotation = 0;
                if (isOnGround) {
                    activeTrick = null;
                    trickProgress = 0;
                }
            }
        }
    }

    private void updateAnimations(double dt) {
        // Leg animation based on speed
        if (isOnGround && Math.abs(velocityX) > 50) {
            legAngle = Math.sin(System.currentTimeMillis() * 0.01) * 10;
        } else {
            legAngle *= 0.9;
        }

        // Arm animation
        if (!isOnGround) {
            armAngle = 20 + Math.sin(System.currentTimeMillis() * 0.005) * 10;
        } else {
            armAngle *= 0.9;
        }
    }

    public void draw(Graphics2D g2d) {
        AffineTransform oldTransform = g2d.getTransform();

        // Apply rotation around center
        g2d.translate(x, y + height/2);
        g2d.rotate(Math.toRadians(rotation));
        g2d.translate(-x, -(y + height/2));

        // Draw shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval((int)(x - width/2 - 5), (int)(y + height + 2), (int)(width + 10), 8);

        // Draw skateboard
        drawSkateboard(g2d);

        // Draw skater body
        drawBody(g2d);

        g2d.setTransform(oldTransform);
    }

    private void drawSkateboard(Graphics2D g2d) {
        AffineTransform boardTransform = g2d.getTransform();

        // Board rotation for tricks
        g2d.translate(x, y + height - 5);
        g2d.rotate(Math.toRadians(boardRotation));
        g2d.translate(-x, -(y + height - 5));

        // Board deck
        g2d.setColor(new Color(139, 90, 43)); // Wood color
        RoundRectangle2D board = new RoundRectangle2D.Double(
                x - 25, y + height - 10, 50, 8, 8, 8
        );
        g2d.fill(board);

        // Board graphic
        g2d.setColor(new Color(200, 50, 50));
        g2d.fillRect((int)(x - 15), (int)(y + height - 9), 30, 6);

        // Wheels
        g2d.setColor(new Color(250, 250, 250));
        g2d.fillOval((int)(x - 22), (int)(y + height - 5), 10, 10);
        g2d.fillOval((int)(x + 12), (int)(y + height - 5), 10, 10);

        // Wheel details
        g2d.setColor(new Color(100, 100, 100));
        g2d.drawOval((int)(x - 22), (int)(y + height - 5), 10, 10);
        g2d.drawOval((int)(x + 12), (int)(y + height - 5), 10, 10);

        // Trucks
        g2d.setColor(new Color(180, 180, 180));
        g2d.fillRect((int)(x - 20), (int)(y + height - 4), 8, 4);
        g2d.fillRect((int)(x + 12), (int)(y + height - 4), 8, 4);

        g2d.setTransform(boardTransform);
    }

    private void drawBody(Graphics2D g2d) {
        double drawHeight = isCrouching ? height * 0.7 : height;
        double offsetY = isCrouching ? height * 0.3 : 0;

        // Legs
        g2d.setColor(new Color(30, 30, 100)); // Jeans
        g2d.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Back leg
        int legX1 = (int)(x - 5);
        int legY1 = (int)(y + offsetY + drawHeight * 0.5);
        int legX2 = (int)(x - 8 + Math.sin(Math.toRadians(legAngle)) * 5);
        int legY2 = (int)(y + offsetY + drawHeight - 12);
        g2d.drawLine(legX1, legY1, legX2, legY2);

        // Front leg
        legX1 = (int)(x + 5);
        legX2 = (int)(x + 8 - Math.sin(Math.toRadians(legAngle)) * 5);
        g2d.drawLine(legX1, legY1, legX2, legY2);

        // Shoes
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRoundRect((int)(x - 15), (int)(y + offsetY + drawHeight - 15), 14, 8, 4, 4);
        g2d.fillRoundRect((int)(x + 3), (int)(y + offsetY + drawHeight - 15), 14, 8, 4, 4);

        // Torso
        g2d.setColor(new Color(200, 50, 50)); // Red shirt
        g2d.fillRoundRect((int)(x - 12), (int)(y + offsetY + 15), 24, 30, 8, 8);

        // Shirt detail
        g2d.setColor(new Color(180, 40, 40));
        g2d.drawLine((int)(x), (int)(y + offsetY + 18), (int)(x), (int)(y + offsetY + 42));

        // Arms
        g2d.setColor(new Color(230, 180, 150)); // Skin
        g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Left arm
        int armX1 = (int)(x - 12);
        int armY1 = (int)(y + offsetY + 20);
        int armX2 = (int)(x - 20 - Math.sin(Math.toRadians(armAngle)) * 8);
        int armY2 = (int)(y + offsetY + 35 + Math.cos(Math.toRadians(armAngle)) * 5);
        g2d.drawLine(armX1, armY1, armX2, armY2);

        // Right arm
        armX1 = (int)(x + 12);
        armX2 = (int)(x + 20 + Math.sin(Math.toRadians(armAngle)) * 8);
        g2d.drawLine(armX1, armY1, armX2, armY2);

        // Head
        g2d.setColor(new Color(230, 180, 150)); // Skin
        g2d.fillOval((int)(x - 10), (int)(y + offsetY), 20, 20);

        // Hair/Cap
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillArc((int)(x - 12), (int)(y + offsetY - 2), 24, 16, 0, 180);

        // Cap brim
        g2d.fillRect((int)(x - 14), (int)(y + offsetY + 5), 8, 4);

        // Eyes
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int)(x - 6), (int)(y + offsetY + 7), 5, 5);
        g2d.fillOval((int)(x + 1), (int)(y + offsetY + 7), 5, 5);

        g2d.setColor(Color.BLACK);
        g2d.fillOval((int)(x - 5), (int)(y + offsetY + 8), 3, 3);
        g2d.fillOval((int)(x + 2), (int)(y + offsetY + 8), 3, 3);
    }

    public void moveLeft(double dt) {
        velocityX -= MOVE_SPEED * dt * (isCrouching ? 0.5 : 1.0);
    }

    public void moveRight(double dt) {
        velocityX += MOVE_SPEED * dt * (isCrouching ? 0.5 : 1.0);
    }

    public void crouch() {
        isCrouching = true;
    }

    public void standUp() {
        isCrouching = false;
    }

    public void jump() {
        if (isOnGround || isGrinding) {
            velocityY = -JUMP_POWER * (isCrouching ? 1.3 : 1.0);
            isOnGround = false;
            isGrinding = false;
        }
    }

    public void rotateLeft(double dt) {
        rotationVelocity -= 300 * dt;
    }

    public void rotateRight(double dt) {
        rotationVelocity += 300 * dt;
    }

    public void performTrick(TrickType trick, GamePanel game) {
        if (!isOnGround && activeTrick == null) {
            activeTrick = trick;
            trickProgress = 0;

            switch (trick) {
                case KICKFLIP:
                    currentTrick = "KICKFLIP";
                    game.addTrickToCombo("KICKFLIP", 100);
                    break;
                case HEELFLIP:
                    currentTrick = "HEELFLIP";
                    game.addTrickToCombo("HEELFLIP", 100);
                    break;
                case TREFLIP:
                    currentTrick = "360 FLIP";
                    game.addTrickToCombo("360 FLIP", 250);
                    break;
                case SHUVIT:
                    currentTrick = "SHUVIT";
                    game.addTrickToCombo("SHUVIT", 75);
                    break;
                case GRAB:
                    currentTrick = "INDY GRAB";
                    game.addTrickToCombo("GRAB", 50);
                    break;
                default:
                    break;
            }
        } else if (isOnGround && trick == TrickType.MANUAL) {
            isInManual = !isInManual;
            if (isInManual) {
                currentTrick = "MANUAL";
                game.addTrickToCombo("MANUAL", 25);
            } else {
                currentTrick = "";
            }
        }
    }

    public void completeTrick(GamePanel game) {
        if (activeTrick != null) {
            // Bonus for clean landing
            if (Math.abs(rotation) < 15) {
                game.addScore(50, "CLEAN LANDING");
            }
        }
        activeTrick = null;
        currentTrick = "";
        trickProgress = 0;
        boardRotation = 0;
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x - width/2, y, width, height);
    }
}

// Enum definitions
enum TrickType {
    KICKFLIP, HEELFLIP, TREFLIP, SHUVIT, MANUAL, GRAB
}

enum ObstacleType {
    BOX, BENCH, PLATFORM
}

enum RampType {
    QUARTER_PIPE, HALF_PIPE, KICKER
}

enum CollectibleType {
    COIN, GEM, STAR
}

enum ParticleType {
    DUST, SPARK, SPARKLE
}

/**
 * Obstacle class for boxes, benches, and platforms
 */
class Obstacle {
    double x, y, width, height;
    ObstacleType type;

    public Obstacle(double x, double y, double width, double height, ObstacleType type) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
    }

    public boolean checkCollision(Skater skater) {
        return skater.getBounds().intersects(new Rectangle2D.Double(x, y, width, height));
    }

    public void resolveCollision(Skater skater) {
        Rectangle2D skaterBounds = skater.getBounds();
        Rectangle2D obstacleBounds = new Rectangle2D.Double(x, y, width, height);

        // Calculate overlap
        double overlapLeft = (skaterBounds.getX() + skaterBounds.getWidth()) - obstacleBounds.getX();
        double overlapRight = (obstacleBounds.getX() + obstacleBounds.getWidth()) - skaterBounds.getX();
        double overlapTop = (skaterBounds.getY() + skaterBounds.getHeight()) - obstacleBounds.getY();
        double overlapBottom = (obstacleBounds.getY() + obstacleBounds.getHeight()) - skaterBounds.getY();

        // Find smallest overlap
        double minOverlap = Math.min(Math.min(overlapLeft, overlapRight), Math.min(overlapTop, overlapBottom));

        if (minOverlap == overlapTop && skater.velocityY > 0) {
            // Landing on top
            skater.y = y - skater.height;
            skater.velocityY = 0;
            skater.isOnGround = true;
        } else if (minOverlap == overlapLeft) {
            skater.x = x - skater.width/2 - 1;
            skater.velocityX = -skater.velocityX * 0.3;
        } else if (minOverlap == overlapRight) {
            skater.x = x + width + skater.width/2 + 1;
            skater.velocityX = -skater.velocityX * 0.3;
        }
    }

    public void draw(Graphics2D g2d) {
        switch (type) {
            case BOX:
                // Main box
                GradientPaint boxGradient = new GradientPaint(
                        (float)x, (float)y, new Color(100, 100, 100),
                        (float)x, (float)(y + height), new Color(60, 60, 60)
                );
                g2d.setPaint(boxGradient);
                g2d.fill(new Rectangle2D.Double(x, y, width, height));

                // Top surface
                g2d.setColor(new Color(120, 120, 120));
                g2d.fill(new Rectangle2D.Double(x, y, width, 5));

                // Edge highlight
                g2d.setColor(new Color(150, 150, 150));
                g2d.setStroke(new BasicStroke(2));
                g2d.draw(new Rectangle2D.Double(x, y, width, height));
                break;

            case BENCH:
                // Bench seat
                g2d.setColor(new Color(139, 90, 43));
                g2d.fill(new Rectangle2D.Double(x, y, width, height * 0.4));

                // Legs
                g2d.setColor(new Color(80, 80, 80));
                g2d.fillRect((int)(x + 5), (int)(y + height * 0.4), 8, (int)(height * 0.6));
                g2d.fillRect((int)(x + width - 13), (int)(y + height * 0.4), 8, (int)(height * 0.6));
                break;

            case PLATFORM:
                // Platform with steps
                g2d.setColor(new Color(90, 90, 90));
                g2d.fill(new Rectangle2D.Double(x, y, width, height));

                // Step lines
                g2d.setColor(new Color(70, 70, 70));
                for (int i = 1; i < 3; i++) {
                    g2d.drawLine((int)(x), (int)(y + i * height/3),
                            (int)(x + width), (int)(y + i * height/3));
                }

                // Top edge
                g2d.setColor(new Color(110, 110, 110));
                g2d.fillRect((int)x, (int)y, (int)width, 4);
                break;
        }
    }
}

/**
 * Ramp class for quarter pipes, half pipes, and kickers
 */
class Ramp {
    double x, y, width, height;
    RampType type;

    public Ramp(double x, double y, double width, double height, RampType type) {
        this.x = x;
        this.y = y - height;
        this.width = width;
        this.height = height;
        this.type = type;
    }

    public boolean checkCollision(Skater skater) {
        // Simplified collision check
        if (skater.x >= x && skater.x <= x + width) {
            double rampY = getRampYAtX(skater.x);
            if (skater.y + skater.height >= rampY && skater.y + skater.height <= rampY + 20) {
                return true;
            }
        }
        return false;
    }

    public double getRampYAtX(double px) {
        double relativeX = (px - x) / width;
        switch (type) {
            case QUARTER_PIPE:
                return y + height - Math.sqrt(1 - relativeX * relativeX) * height;
            case KICKER:
                return y + height * (1 - relativeX);
            case HALF_PIPE:
                if (relativeX < 0.3) {
                    double t = relativeX / 0.3;
                    return y + height - Math.sqrt(1 - (1-t) * (1-t)) * height;
                } else if (relativeX > 0.7) {
                    double t = (relativeX - 0.7) / 0.3;
                    return y + height - Math.sqrt(1 - t * t) * height;
                } else {
                    return y + height;
                }
            default:
                return y + height;
        }
    }

    public void applyEffect(Skater skater) {
        double relativeX = (skater.x - x) / width;
        double angle = 0;

        switch (type) {
            case QUARTER_PIPE:
                angle = Math.atan2(height * relativeX / Math.sqrt(1 - relativeX * relativeX), 1);
                break;
            case KICKER:
                angle = Math.atan2(height, width);
                break;
            case HALF_PIPE:
                if (relativeX < 0.3) {
                    double t = relativeX / 0.3;
                    angle = -Math.atan2(height * (1-t) / Math.sqrt(1 - (1-t) * (1-t)), 1);
                } else if (relativeX > 0.7) {
                    double t = (relativeX - 0.7) / 0.3;
                    angle = Math.atan2(height * t / Math.sqrt(1 - t * t), 1);
                }
                break;
        }

        // Apply boost based on speed and angle
        double speed = Math.sqrt(skater.velocityX * skater.velocityX + skater.velocityY * skater.velocityY);
        if (angle != 0 && speed > 100) {
            skater.velocityY = -Math.abs(Math.sin(angle) * speed * 1.2);
            skater.velocityX = Math.cos(angle) * speed * 1.1 * Math.signum(skater.velocityX);
        }

        // Set position on ramp
        skater.y = getRampYAtX(skater.x) - skater.height;
        skater.isOnGround = true;
    }

    public void draw(Graphics2D g2d) {
        Path2D.Double rampPath = new Path2D.Double();

        switch (type) {
            case QUARTER_PIPE:
                rampPath.moveTo(x, y + height);
                for (int i = 0; i <= 20; i++) {
                    double t = i / 20.0;
                    double px = x + t * width;
                    double py = y + height - Math.sqrt(1 - t * t) * height;
                    rampPath.lineTo(px, py);
                }
                rampPath.lineTo(x + width, y + height);
                rampPath.closePath();
                break;

            case KICKER:
                rampPath.moveTo(x, y + height);
                rampPath.lineTo(x + width, y);
                rampPath.lineTo(x + width, y + height);
                rampPath.closePath();
                break;

            case HALF_PIPE:
                rampPath.moveTo(x, y);
                // Left curve
                for (int i = 0; i <= 10; i++) {
                    double t = i / 10.0;
                    double px = x + t * width * 0.3;
                    double py = y + height - Math.sqrt(1 - (1-t) * (1-t)) * height;
                    rampPath.lineTo(px, py);
                }
                // Bottom flat
                rampPath.lineTo(x + width * 0.7, y + height);
                // Right curve
                for (int i = 0; i <= 10; i++) {
                    double t = i / 10.0;
                    double px = x + width * 0.7 + t * width * 0.3;
                    double py = y + height - Math.sqrt(1 - t * t) * height;
                    rampPath.lineTo(px, py);
                }
                rampPath.lineTo(x + width, y);
                rampPath.lineTo(x + width, y + height + 20);
                rampPath.lineTo(x, y + height + 20);
                rampPath.closePath();
                break;
        }

        // Ramp surface
        GradientPaint rampGradient = new GradientPaint(
                (float)x, (float)y, new Color(120, 100, 80),
                (float)x, (float)(y + height), new Color(80, 60, 40)
        );
        g2d.setPaint(rampGradient);
        g2d.fill(rampPath);

        // Ramp edge
        g2d.setColor(new Color(60, 50, 40));
        g2d.setStroke(new BasicStroke(3));
        g2d.draw(rampPath);

        // Surface texture
        g2d.setColor(new Color(100, 80, 60));
        g2d.setStroke(new BasicStroke(1));
        for (int i = 1; i < 5; i++) {
            double yOffset = i * height / 5;
            g2d.drawLine((int)x, (int)(y + yOffset), (int)(x + width), (int)(y + yOffset));
        }
    }
}

/**
 * Rail class for grinding
 */
class Rail {
    double x, y, width, height;

    public Rail(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean checkCollision(Skater skater) {
        Rectangle2D skaterBounds = skater.getBounds();
        Rectangle2D railBounds = new Rectangle2D.Double(x, y - 10, width, 20);
        return skaterBounds.intersects(railBounds) && skater.velocityY >= 0;
    }

    public void draw(Graphics2D g2d) {
        // Rail supports
        g2d.setColor(new Color(100, 100, 100));
        g2d.fillRect((int)x, (int)y, 8, 60);
        g2d.fillRect((int)(x + width - 8), (int)y, 8, 60);

        // Rail bar
        GradientPaint railGradient = new GradientPaint(
                (float)x, (float)y, new Color(200, 200, 200),
                (float)x, (float)(y + height), new Color(150, 150, 150)
        );
        g2d.setPaint(railGradient);
        g2d.fill(new RoundRectangle2D.Double(x - 5, y, width + 10, height, 5, 5));

        // Rail highlight
        g2d.setColor(new Color(230, 230, 230));
        g2d.drawLine((int)x, (int)y + 2, (int)(x + width), (int)y + 2);
    }
}

/**
 * Collectible items
 */
class Collectible {
    double x, y;
    CollectibleType type;
    boolean isActive = true;
    private double animationOffset = Math.random() * Math.PI * 2;
    private double collectAnimation = 0;

    public Collectible(double x, double y, CollectibleType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void update(double dt) {
        // Floating animation
        y += Math.sin(System.currentTimeMillis() * 0.003 + animationOffset) * 0.5;
    }

    public void collect() {
        isActive = false;
    }

    public int getPoints() {
        switch (type) {
            case COIN: return 50;
            case GEM: return 150;
            case STAR: return 300;
            default: return 0;
        }
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x - 15, y - 15, 30, 30);
    }

    public void draw(Graphics2D g2d) {
        switch (type) {
            case COIN:
                // Coin glow
                g2d.setColor(new Color(255, 215, 0, 100));
                g2d.fillOval((int)(x - 18), (int)(y - 18), 36, 36);

                // Coin
                g2d.setColor(new Color(255, 215, 0));
                g2d.fillOval((int)(x - 12), (int)(y - 12), 24, 24);

                // Coin highlight
                g2d.setColor(new Color(255, 240, 150));
                g2d.fillOval((int)(x - 8), (int)(y - 8), 8, 8);

                // Dollar sign
                g2d.setColor(new Color(200, 160, 0));
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                g2d.drawString("$", (int)x - 4, (int)y + 5);
                break;

            case GEM:
                // Gem glow
                g2d.setColor(new Color(0, 200, 255, 100));
                g2d.fillOval((int)(x - 20), (int)(y - 20), 40, 40);

                // Gem shape
                int[] gemX = {(int)x, (int)(x + 15), (int)(x + 10), (int)(x - 10), (int)(x - 15)};
                int[] gemY = {(int)(y - 15), (int)(y - 5), (int)(y + 15), (int)(y + 15), (int)(y - 5)};
                g2d.setColor(new Color(0, 200, 255));
                g2d.fillPolygon(gemX, gemY, 5);

                // Gem highlight
                g2d.setColor(new Color(150, 230, 255));
                g2d.drawLine((int)x, (int)(y - 12), (int)(x + 8), (int)(y - 2));
                break;

            case STAR:
                // Star glow
                g2d.setColor(new Color(255, 255, 100, 150));
                g2d.fillOval((int)(x - 25), (int)(y - 25), 50, 50);

                // Star shape
                Path2D.Double star = new Path2D.Double();
                double outerRadius = 18;
                double innerRadius = 8;
                for (int i = 0; i < 10; i++) {
                    double angle = Math.PI / 2 + i * Math.PI / 5;
                    double radius = (i % 2 == 0) ? outerRadius : innerRadius;
                    double px = x + Math.cos(angle) * radius;
                    double py = y - Math.sin(angle) * radius;
                    if (i == 0) star.moveTo(px, py);
                    else star.lineTo(px, py);
                }
                star.closePath();

                g2d.setColor(new Color(255, 255, 0));
                g2d.fill(star);

                // Star outline
                g2d.setColor(new Color(255, 200, 0));
                g2d.setStroke(new BasicStroke(2));
                g2d.draw(star);
                break;
        }
    }
}

/**
 * Particle system for visual effects
 */
class Particle {
    double x, y;
    double velocityX, velocityY;
    ParticleType type;
    double life = 1.0;
    double maxLife = 1.0;
    double size;
    Color color;

    public Particle(double x, double y, ParticleType type, double vx, double vy) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.velocityX = vx;
        this.velocityY = vy;

        switch (type) {
            case DUST:
                size = 3 + Math.random() * 5;
                maxLife = 0.5 + Math.random() * 0.3;
                color = new Color(150, 140, 130);
                break;
            case SPARK:
                size = 2 + Math.random() * 3;
                maxLife = 0.3 + Math.random() * 0.2;
                color = new Color(255, 200, 50);
                break;
            case SPARKLE:
                size = 4 + Math.random() * 4;
                maxLife = 0.6 + Math.random() * 0.4;
                color = new Color(255, 255, 200);
                break;
        }
        life = maxLife;
    }

    public void update(double dt) {
        x += velocityX * dt;
        y += velocityY * dt;

        // Apply gravity to dust
        if (type == ParticleType.DUST) {
            velocityY += 200 * dt;
        }

        // Slow down
        velocityX *= 0.98;
        velocityY *= 0.98;

        life -= dt;
    }

    public boolean isDead() {
        return life <= 0;
    }

    public void draw(Graphics2D g2d) {
        float alpha = (float)(life / maxLife);

        switch (type) {
            case DUST:
                g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 150)));
                g2d.fillOval((int)(x - size/2), (int)(y - size/2), (int)size, (int)size);
                break;

            case SPARK:
                g2d.setColor(new Color(255, 200, 50, (int)(alpha * 255)));
                g2d.fillOval((int)(x - size/2), (int)(y - size/2), (int)size, (int)size);
                // Glow
                g2d.setColor(new Color(255, 150, 0, (int)(alpha * 100)));
                g2d.fillOval((int)(x - size), (int)(y - size), (int)(size * 2), (int)(size * 2));
                break;

            case SPARKLE:
                g2d.setColor(new Color(255, 255, 200, (int)(alpha * 255)));
                // Cross shape
                g2d.setStroke(new BasicStroke(2));
                g2d.drawLine((int)(x - size/2), (int)y, (int)(x + size/2), (int)y);
                g2d.drawLine((int)x, (int)(y - size/2), (int)x, (int)(y + size/2));
                break;
        }
    }
}

/**
 * Score popup for trick points
 */
class ScorePopup {
    double x, y;
    String text;
    double life = 1.5;
    int multiplier;

    public ScorePopup(double x, double y, String text, int multiplier) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.multiplier = multiplier;
    }

    public void update(double dt) {
        y -= 50 * dt;
        life -= dt;
    }

    public boolean isDead() {
        return life <= 0;
    }

    public void draw(Graphics2D g2d) {
        float alpha = Math.min(1.0f, (float)(life / 0.5));
        float scale = 1.0f + (1.5f - (float)life) * 0.2f;

        // Color based on multiplier
        Color textColor;
        if (multiplier >= 5) textColor = new Color(255, 50, 50);
        else if (multiplier >= 3) textColor = new Color(255, 150, 0);
        else textColor = new Color(255, 255, 0);

        g2d.setFont(new Font("Impact", Font.PLAIN, (int)(18 * scale)));
        g2d.setColor(new Color(0, 0, 0, (int)(alpha * 150)));
        g2d.drawString(text, (int)x + 2, (int)y + 2);

        g2d.setColor(new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), (int)(alpha * 255)));
        g2d.drawString(text, (int)x, (int)y);
    }
}

/**
 * Cloud class for background decoration
 */
class Cloud {
    double x, y, width, height;
    double speed;

    public Cloud(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = 10 + Math.random() * 20;
    }
}