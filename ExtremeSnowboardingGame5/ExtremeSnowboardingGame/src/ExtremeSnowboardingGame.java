import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class ExtremeSnowboardingGame extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ExtremeSnowboardingGame game = new ExtremeSnowboardingGame();
            game.setVisible(true);
        });
    }

    public ExtremeSnowboardingGame() {
        setTitle("Extreme Snowboarding - Press SPACE for tricks!");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        setLocationRelativeTo(null);

        gamePanel.startGame();
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {

    // Window dimensions
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;

    // Game states
    private enum GameState { MENU, PLAYING, PAUSED, GAME_OVER }
    private GameState gameState = GameState.MENU;

    // Game timer
    private Timer gameTimer;
    private static final int FPS = 60;
    private static final int DELAY = 1000 / FPS;

    // Player (Snowboarder)
    private Snowboarder player;

    // Terrain and obstacles
    private List<TerrainSegment> terrain;
    private List<Obstacle> obstacles;
    private List<Collectible> collectibles;
    private List<SnowParticle> snowParticles;
    private List<TrickParticle> trickParticles;

    // Game variables
    private double scrollSpeed = 5.0;
    private double maxScrollSpeed = 15.0;
    private int score = 0;
    private int highScore = 0;
    private int distance = 0;
    private int combo = 0;
    private long comboTimer = 0;
    private double terrainOffset = 0;

    // Visual effects
    private float snowIntensity = 1.0f;
    private Color skyGradientTop = new Color(135, 206, 235);
    private Color skyGradientBottom = new Color(200, 230, 255);
    private List<Mountain> mountains;
    private List<Cloud> clouds;

    // Random generator
    private Random random = new Random();

    // Input states
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.WHITE);
        setFocusable(true);
        addKeyListener(this);

        initializeGame();
    }

    /**
     * Initialize all game objects and collections
     */
    private void initializeGame() {
        player = new Snowboarder(WIDTH / 2, HEIGHT / 2);

        terrain = new ArrayList<>();
        obstacles = new ArrayList<>();
        collectibles = new ArrayList<>();
        snowParticles = new ArrayList<>();
        trickParticles = new ArrayList<>();
        mountains = new ArrayList<>();
        clouds = new ArrayList<>();

        generateInitialTerrain();
        generateMountains();
        generateClouds();
        generateSnowParticles(200);

        gameTimer = new Timer(DELAY, this);
    }

    /**
     * Start the game loop
     */
    public void startGame() {
        gameTimer.start();
    }

    /**
     * Reset game to initial state
     */
    private void resetGame() {
        player = new Snowboarder(WIDTH / 2, HEIGHT / 2);
        terrain.clear();
        obstacles.clear();
        collectibles.clear();
        trickParticles.clear();

        generateInitialTerrain();

        scrollSpeed = 5.0;
        score = 0;
        distance = 0;
        combo = 0;
        comboTimer = 0;
        terrainOffset = 0;

        gameState = GameState.PLAYING;
    }

    /**
     * Generate initial terrain segments
     */
    private void generateInitialTerrain() {
        double lastY = HEIGHT * 0.6;
        for (int i = 0; i < 20; i++) {
            double newY = lastY + (random.nextDouble() - 0.4) * 30;
            newY = Math.max(HEIGHT * 0.4, Math.min(HEIGHT * 0.8, newY));
            terrain.add(new TerrainSegment(i * 60, lastY, (i + 1) * 60, newY));
            lastY = newY;
        }
    }

    /**
     * Generate background mountains
     */
    private void generateMountains() {
        for (int i = 0; i < 8; i++) {
            mountains.add(new Mountain(
                    i * 200 - 100,
                    150 + random.nextInt(100),
                    100 + random.nextInt(150),
                    new Color(180 + random.nextInt(40), 190 + random.nextInt(40), 210 + random.nextInt(30))
            ));
        }
    }

    /**
     * Generate background clouds
     */
    private void generateClouds() {
        for (int i = 0; i < 6; i++) {
            clouds.add(new Cloud(
                    random.nextInt(WIDTH),
                    30 + random.nextInt(100),
                    50 + random.nextInt(80),
                    0.2 + random.nextDouble() * 0.5
            ));
        }
    }

    /**
     * Generate snow particles for visual effect
     */
    private void generateSnowParticles(int count) {
        for (int i = 0; i < count; i++) {
            snowParticles.add(new SnowParticle(
                    random.nextInt(WIDTH),
                    random.nextInt(HEIGHT),
                    1 + random.nextInt(3),
                    0.5 + random.nextDouble() * 2
            ));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (gameState) {
            case MENU:
                updateMenu();
                break;
            case PLAYING:
                updateGame();
                break;
            case PAUSED:
                // Just render, no updates
                break;
            case GAME_OVER:
                updateGameOver();
                break;
        }
        repaint();
    }

    /**
     * Update menu state animations
     */
    private void updateMenu() {
        updateSnowParticles();
        updateClouds();
    }

    /**
     * Update game over state
     */
    private void updateGameOver() {
        updateSnowParticles();
        updateTrickParticles();
    }

    /**
     * Main game update loop
     */
    private void updateGame() {
        // Gradually increase speed
        if (scrollSpeed < maxScrollSpeed) {
            scrollSpeed += 0.001;
        }

        // Update distance
        distance += (int) scrollSpeed;

        // Update combo timer
        if (combo > 0 && System.currentTimeMillis() - comboTimer > 3000) {
            score += combo * 100;
            combo = 0;
        }

        // Handle player input
        handlePlayerInput();

        // Update player
        player.update(terrain, scrollSpeed);

        // Update terrain
        updateTerrain();

        // Update obstacles
        updateObstacles();

        // Update collectibles
        updateCollectibles();

        // Update visual effects
        updateSnowParticles();
        updateClouds();
        updateTrickParticles();

        // Check collisions
        checkCollisions();

        // Spawn new elements
        spawnElements();
    }

    /**
     * Handle player movement input
     */
    private void handlePlayerInput() {
        if (leftPressed) {
            player.moveLeft();
        }
        if (rightPressed) {
            player.moveRight();
        }
        if (upPressed && !player.isJumping()) {
            player.jump();
        }
        if (downPressed) {
            player.crouch();
        } else {
            player.standUp();
        }
    }

    /**
     * Update terrain segments, remove old ones, add new ones
     */
    private void updateTerrain() {
        terrainOffset += scrollSpeed;

        // Move terrain left
        for (TerrainSegment segment : terrain) {
            segment.scroll(scrollSpeed);
        }

        // Remove off-screen segments
        terrain.removeIf(s -> s.getEndX() < -50);

        // Add new segments
        while (terrain.size() < 20) {
            TerrainSegment last = terrain.get(terrain.size() - 1);
            double newY = last.getEndY() + (random.nextDouble() - 0.4) * 40;
            newY = Math.max(HEIGHT * 0.35, Math.min(HEIGHT * 0.85, newY));

            // Add variety - occasional steep sections or jumps
            if (random.nextDouble() < 0.1) {
                newY = last.getEndY() - 50 - random.nextInt(30); // Jump ramp
            }

            terrain.add(new TerrainSegment(last.getEndX(), last.getEndY(), last.getEndX() + 60, newY));
        }
    }

    /**
     * Update obstacles
     */
    private void updateObstacles() {
        for (Obstacle obstacle : obstacles) {
            obstacle.scroll(scrollSpeed);
        }
        obstacles.removeIf(o -> o.getX() < -50);
    }

    /**
     * Update collectibles
     */
    private void updateCollectibles() {
        for (Collectible collectible : collectibles) {
            collectible.scroll(scrollSpeed);
            collectible.update();
        }
        collectibles.removeIf(c -> c.getX() < -50);
    }

    /**
     * Update snow particle effects
     */
    private void updateSnowParticles() {
        for (SnowParticle particle : snowParticles) {
            particle.update(scrollSpeed * 0.3);
            if (particle.getY() > HEIGHT) {
                particle.reset(random.nextInt(WIDTH), -10);
            }
            if (particle.getX() < -10) {
                particle.reset(WIDTH + 10, random.nextInt(HEIGHT));
            }
        }
    }

    /**
     * Update trick particle effects
     */
    private void updateTrickParticles() {
        for (TrickParticle particle : trickParticles) {
            particle.update();
        }
        trickParticles.removeIf(p -> !p.isAlive());
    }

    /**
     * Update cloud positions
     */
    private void updateClouds() {
        for (Cloud cloud : clouds) {
            cloud.update(scrollSpeed * 0.1);
            if (cloud.getX() < -cloud.getWidth()) {
                cloud.reset(WIDTH + 50, 30 + random.nextInt(100));
            }
        }
    }

    /**
     * Check all collisions
     */
    private void checkCollisions() {
        Rectangle playerBounds = player.getBounds();

        // Check obstacle collisions
        for (Obstacle obstacle : obstacles) {
            if (obstacle.isActive() && playerBounds.intersects(obstacle.getBounds())) {
                if (player.isJumping() && player.getVelocityY() > 0) {
                    // Land on obstacle - survive but slow down
                    obstacle.setActive(false);
                    scrollSpeed = Math.max(3, scrollSpeed - 2);
                } else if (!player.isInvincible()) {
                    // Crash!
                    createCrashEffect();
                    gameOver();
                    return;
                }
            }
        }

        // Check collectible collisions
        Iterator<Collectible> collectibleIterator = collectibles.iterator();
        while (collectibleIterator.hasNext()) {
            Collectible collectible = collectibleIterator.next();
            if (playerBounds.intersects(collectible.getBounds())) {
                collectible.collect();
                score += collectible.getValue() * (1 + combo);
                combo++;
                comboTimer = System.currentTimeMillis();
                createCollectEffect(collectible.getX(), collectible.getY());
                collectibleIterator.remove();
            }
        }

        // Check if player fell off screen
        if (player.getY() > HEIGHT + 50) {
            gameOver();
        }
    }

    /**
     * Create crash visual effect
     */
    private void createCrashEffect() {
        for (int i = 0; i < 30; i++) {
            trickParticles.add(new TrickParticle(
                    player.getX(), player.getY(),
                    new Color(255, 100 + random.nextInt(100), 50),
                    random.nextDouble() * Math.PI * 2,
                    2 + random.nextDouble() * 5
            ));
        }
    }

    /**
     * Create collection visual effect
     */
    private void createCollectEffect(double x, double y) {
        for (int i = 0; i < 15; i++) {
            trickParticles.add(new TrickParticle(
                    x, y,
                    new Color(255, 215, 0),
                    random.nextDouble() * Math.PI * 2,
                    1 + random.nextDouble() * 3
            ));
        }
    }

    /**
     * Create trick visual effect
     */
    private void createTrickEffect() {
        for (int i = 0; i < 20; i++) {
            trickParticles.add(new TrickParticle(
                    player.getX(), player.getY(),
                    new Color(100 + random.nextInt(155), 200 + random.nextInt(55), 255),
                    random.nextDouble() * Math.PI * 2,
                    1 + random.nextDouble() * 4
            ));
        }
    }

    /**
     * Spawn new obstacles and collectibles
     */
    private void spawnElements() {
        // Spawn obstacles based on difficulty
        if (random.nextDouble() < 0.02 + (scrollSpeed / maxScrollSpeed) * 0.02) {
            double spawnY = getTerrainYAt(WIDTH + 50) - 30;
            int type = random.nextInt(3);
            obstacles.add(new Obstacle(WIDTH + 50, spawnY, type));
        }

        // Spawn collectibles
        if (random.nextDouble() < 0.03) {
            double spawnY = getTerrainYAt(WIDTH + 50) - 50 - random.nextInt(80);
            collectibles.add(new Collectible(WIDTH + 50, spawnY, random.nextInt(3)));
        }
    }

    /**
     * Get terrain height at given X position
     */
    private double getTerrainYAt(double x) {
        for (TerrainSegment segment : terrain) {
            if (x >= segment.getStartX() && x <= segment.getEndX()) {
                return segment.getYAt(x);
            }
        }
        return HEIGHT * 0.6;
    }

    /**
     * Handle game over state
     */
    private void gameOver() {
        gameState = GameState.GAME_OVER;
        if (score > highScore) {
            highScore = score;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw sky gradient
        drawSkyGradient(g2d);

        // Draw mountains
        drawMountains(g2d);

        // Draw clouds
        drawClouds(g2d);

        // Draw terrain
        drawTerrain(g2d);

        // Draw collectibles
        for (Collectible collectible : collectibles) {
            collectible.draw(g2d);
        }

        // Draw obstacles
        for (Obstacle obstacle : obstacles) {
            obstacle.draw(g2d);
        }

        // Draw player
        if (gameState == GameState.PLAYING || gameState == GameState.PAUSED) {
            player.draw(g2d);
        }

        // Draw snow particles
        drawSnowParticles(g2d);

        // Draw trick particles
        for (TrickParticle particle : trickParticles) {
            particle.draw(g2d);
        }

        // Draw UI
        drawUI(g2d);

        // Draw overlays based on game state
        switch (gameState) {
            case MENU:
                drawMenuOverlay(g2d);
                break;
            case PAUSED:
                drawPausedOverlay(g2d);
                break;
            case GAME_OVER:
                drawGameOverOverlay(g2d);
                break;
        }
    }

    /**
     * Draw sky gradient background
     */
    private void drawSkyGradient(Graphics2D g2d) {
        GradientPaint gradient = new GradientPaint(
                0, 0, skyGradientTop,
                0, HEIGHT, skyGradientBottom
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
    }

    /**
     * Draw background mountains
     */
    private void drawMountains(Graphics2D g2d) {
        for (Mountain mountain : mountains) {
            mountain.draw(g2d);
        }
    }

    /**
     * Draw clouds
     */
    private void drawClouds(Graphics2D g2d) {
        for (Cloud cloud : clouds) {
            cloud.draw(g2d);
        }
    }

    /**
     * Draw terrain with snow effect
     */
    private void drawTerrain(Graphics2D g2d) {
        // Draw snow base
        GeneralPath snowPath = new GeneralPath();
        snowPath.moveTo(0, HEIGHT);

        for (TerrainSegment segment : terrain) {
            snowPath.lineTo(segment.getStartX(), segment.getStartY());
        }
        if (!terrain.isEmpty()) {
            TerrainSegment last = terrain.get(terrain.size() - 1);
            snowPath.lineTo(last.getEndX(), last.getEndY());
        }
        snowPath.lineTo(WIDTH, HEIGHT);
        snowPath.closePath();

        // Snow gradient
        GradientPaint snowGradient = new GradientPaint(
                0, HEIGHT * 0.4f, new Color(250, 250, 255),
                0, HEIGHT, new Color(220, 230, 245)
        );
        g2d.setPaint(snowGradient);
        g2d.fill(snowPath);

        // Draw terrain surface with shine
        g2d.setColor(new Color(240, 245, 255));
        g2d.setStroke(new BasicStroke(3f));
        for (TerrainSegment segment : terrain) {
            g2d.draw(new Line2D.Double(
                    segment.getStartX(), segment.getStartY(),
                    segment.getEndX(), segment.getEndY()
            ));
        }

        // Add ice sparkles
        g2d.setColor(new Color(255, 255, 255, 200));
        for (TerrainSegment segment : terrain) {
            if (random.nextDouble() < 0.3) {
                double sparkleX = segment.getStartX() + random.nextDouble() * 60;
                double sparkleY = segment.getYAt(sparkleX) - 2;
                g2d.fillOval((int) sparkleX, (int) sparkleY, 3, 3);
            }
        }
    }

    /**
     * Draw snow particles
     */
    private void drawSnowParticles(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 200));
        for (SnowParticle particle : snowParticles) {
            particle.draw(g2d);
        }
    }

    /**
     * Draw game UI (score, distance, combo)
     */
    private void drawUI(Graphics2D g2d) {
        if (gameState != GameState.MENU) {
            // Score panel background
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRoundRect(10, 10, 200, 90, 15, 15);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString("Score: " + score, 25, 35);
            g2d.drawString("Distance: " + (distance / 10) + "m", 25, 58);
            g2d.drawString("Speed: " + String.format("%.1f", scrollSpeed), 25, 81);

            // High score
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            g2d.drawString("High Score: " + highScore, 25, 115);

            // Combo display
            if (combo > 0) {
                g2d.setColor(new Color(255, 215, 0));
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                g2d.drawString("COMBO x" + combo, WIDTH / 2 - 60, 50);
            }
        }
    }

    /**
     * Draw menu overlay
     */
    private void drawMenuOverlay(Graphics2D g2d) {
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 50, 150));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 56));
        String title = "EXTREME SNOWBOARDING";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, HEIGHT / 3);

        // Subtitle with animation
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        double pulse = Math.sin(System.currentTimeMillis() / 300.0) * 0.3 + 0.7;
        g2d.setColor(new Color(255, 255, 255, (int) (pulse * 255)));
        String subtitle = "Press ENTER to Start";
        fm = g2d.getFontMetrics();
        g2d.drawString(subtitle, (WIDTH - fm.stringWidth(subtitle)) / 2, HEIGHT / 2);

        // Controls
        g2d.setColor(new Color(200, 200, 200));
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        String[] controls = {
                "Controls:",
                "← → : Steer",
                "↑ : Jump",
                "↓ : Crouch (faster)",
                "SPACE : Trick (in air)",
                "P : Pause",
                "R : Restart"
        };
        int y = HEIGHT / 2 + 60;
        for (String control : controls) {
            fm = g2d.getFontMetrics();
            g2d.drawString(control, (WIDTH - fm.stringWidth(control)) / 2, y);
            y += 25;
        }
    }

    /**
     * Draw paused overlay
     */
    private void drawPausedOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String paused = "PAUSED";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(paused, (WIDTH - fm.stringWidth(paused)) / 2, HEIGHT / 2);

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        String resume = "Press P to Resume";
        fm = g2d.getFontMetrics();
        g2d.drawString(resume, (WIDTH - fm.stringWidth(resume)) / 2, HEIGHT / 2 + 50);
    }

    /**
     * Draw game over overlay
     */
    private void drawGameOverOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(50, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(new Color(255, 100, 100));
        g2d.setFont(new Font("Arial", Font.BOLD, 56));
        String gameOver = "GAME OVER";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(gameOver, (WIDTH - fm.stringWidth(gameOver)) / 2, HEIGHT / 3);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        String finalScore = "Final Score: " + score;
        fm = g2d.getFontMetrics();
        g2d.drawString(finalScore, (WIDTH - fm.stringWidth(finalScore)) / 2, HEIGHT / 2);

        String distanceStr = "Distance: " + (distance / 10) + " meters";
        fm = g2d.getFontMetrics();
        g2d.drawString(distanceStr, (WIDTH - fm.stringWidth(distanceStr)) / 2, HEIGHT / 2 + 40);

        if (score >= highScore && score > 0) {
            g2d.setColor(new Color(255, 215, 0));
            String newHigh = "NEW HIGH SCORE!";
            fm = g2d.getFontMetrics();
            g2d.drawString(newHigh, (WIDTH - fm.stringWidth(newHigh)) / 2, HEIGHT / 2 + 90);
        }

        g2d.setColor(new Color(200, 200, 200));
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        String restart = "Press R to Restart";
        fm = g2d.getFontMetrics();
        g2d.drawString(restart, (WIDTH - fm.stringWidth(restart)) / 2, HEIGHT / 2 + 140);
    }

    // KeyListener implementation
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (gameState) {
            case MENU:
                if (key == KeyEvent.VK_ENTER) {
                    resetGame();
                }
                break;

            case PLAYING:
                if (key == KeyEvent.VK_LEFT) leftPressed = true;
                if (key == KeyEvent.VK_RIGHT) rightPressed = true;
                if (key == KeyEvent.VK_UP) upPressed = true;
                if (key == KeyEvent.VK_DOWN) downPressed = true;
                if (key == KeyEvent.VK_SPACE && player.isJumping()) {
                    player.performTrick();
                    createTrickEffect();
                    score += 50 * (1 + combo);
                    combo++;
                    comboTimer = System.currentTimeMillis();
                }
                if (key == KeyEvent.VK_P) {
                    gameState = GameState.PAUSED;
                }
                if (key == KeyEvent.VK_R) {
                    resetGame();
                }
                break;

            case PAUSED:
                if (key == KeyEvent.VK_P) {
                    gameState = GameState.PLAYING;
                }
                if (key == KeyEvent.VK_R) {
                    resetGame();
                }
                break;

            case GAME_OVER:
                if (key == KeyEvent.VK_R || key == KeyEvent.VK_ENTER) {
                    resetGame();
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) leftPressed = false;
        if (key == KeyEvent.VK_RIGHT) rightPressed = false;
        if (key == KeyEvent.VK_UP) upPressed = false;
        if (key == KeyEvent.VK_DOWN) downPressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

/**
 * Snowboarder player class with physics and animation
 */
class Snowboarder {
    private double x, y;
    private double velocityX = 0;
    private double velocityY = 0;
    private double rotation = 0;
    private boolean jumping = false;
    private boolean crouching = false;
    private boolean invincible = false;
    private int trickRotation = 0;
    private boolean performingTrick = false;

    private static final double GRAVITY = 0.5;
    private static final double JUMP_FORCE = -12;
    private static final double MOVE_SPEED = 0.8;
    private static final double MAX_SPEED_X = 8;
    private static final double FRICTION = 0.92;

    public Snowboarder(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void update(List<TerrainSegment> terrain, double scrollSpeed) {
        // Apply gravity
        velocityY += GRAVITY;

        // Apply velocity
        x += velocityX;
        y += velocityY;

        // Apply friction
        velocityX *= FRICTION;

        // Keep player in bounds horizontally
        x = Math.max(50, Math.min(900, x));

        // Check terrain collision
        double terrainY = getTerrainYAt(x, terrain);

        if (y >= terrainY - 25 && velocityY >= 0) {
            y = terrainY - 25;
            velocityY = 0;
            jumping = false;
            performingTrick = false;
            trickRotation = 0;

            // Calculate rotation based on terrain slope
            double slope = getTerrainSlope(x, terrain);
            rotation = Math.atan(slope);
        } else {
            jumping = true;

            // Handle trick rotation
            if (performingTrick) {
                trickRotation += 15;
                if (trickRotation >= 360) {
                    trickRotation = 0;
                    performingTrick = false;
                }
            }
        }
    }

    private double getTerrainYAt(double x, List<TerrainSegment> terrain) {
        for (TerrainSegment segment : terrain) {
            if (x >= segment.getStartX() && x <= segment.getEndX()) {
                return segment.getYAt(x);
            }
        }
        return 500;
    }

    private double getTerrainSlope(double x, List<TerrainSegment> terrain) {
        for (TerrainSegment segment : terrain) {
            if (x >= segment.getStartX() && x <= segment.getEndX()) {
                return segment.getSlope();
            }
        }
        return 0;
    }

    public void moveLeft() {
        velocityX -= MOVE_SPEED;
        velocityX = Math.max(-MAX_SPEED_X, velocityX);
    }

    public void moveRight() {
        velocityX += MOVE_SPEED;
        velocityX = Math.min(MAX_SPEED_X, velocityX);
    }

    public void jump() {
        if (!jumping) {
            velocityY = JUMP_FORCE;
            jumping = true;
        }
    }

    public void crouch() {
        crouching = true;
    }

    public void standUp() {
        crouching = false;
    }

    public void performTrick() {
        if (jumping && !performingTrick) {
            performingTrick = true;
            trickRotation = 0;
        }
    }

    public void draw(Graphics2D g2d) {
        AffineTransform oldTransform = g2d.getTransform();

        // Apply transformations
        g2d.translate(x, y);
        g2d.rotate(rotation);

        if (performingTrick) {
            g2d.rotate(Math.toRadians(trickRotation));
        }

        int height = crouching ? 20 : 35;
        int yOffset = crouching ? 10 : 0;

        // Draw shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(-15, 20 + yOffset, 30, 10);

        // Draw snowboard
        g2d.setColor(new Color(50, 50, 150));
        g2d.fillRoundRect(-25, 15 + yOffset, 50, 8, 5, 5);
        g2d.setColor(new Color(100, 100, 200));
        g2d.fillRoundRect(-23, 17 + yOffset, 46, 3, 3, 3);

        // Draw body
        g2d.setColor(new Color(200, 50, 50)); // Red jacket
        g2d.fillRoundRect(-12, -height + yOffset, 24, height, 10, 10);

        // Draw head
        g2d.setColor(new Color(255, 220, 180)); // Skin tone
        g2d.fillOval(-8, -height - 15 + yOffset, 16, 16);

        // Draw helmet/goggles
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillArc(-8, -height - 15 + yOffset, 16, 10, 0, 180);
        g2d.setColor(new Color(100, 200, 255));
        g2d.fillRoundRect(-6, -height - 8 + yOffset, 12, 5, 3, 3);

        // Draw arms
        g2d.setColor(new Color(200, 50, 50));
        g2d.setStroke(new BasicStroke(4));
        if (jumping) {
            // Arms up when jumping
            g2d.drawLine(-12, -height + 10 + yOffset, -20, -height - 5 + yOffset);
            g2d.drawLine(12, -height + 10 + yOffset, 20, -height - 5 + yOffset);
        } else {
            // Arms forward when riding
            g2d.drawLine(-12, -height + 10 + yOffset, -18, -height + 5 + yOffset);
            g2d.drawLine(12, -height + 10 + yOffset, 18, -height + 5 + yOffset);
        }

        g2d.setTransform(oldTransform);

        // Draw invincibility effect
        if (invincible) {
            g2d.setColor(new Color(255, 255, 0, 100));
            g2d.fillOval((int) x - 30, (int) y - 50, 60, 70);
        }
    }

    public Rectangle getBounds() {
        int height = crouching ? 30 : 50;
        return new Rectangle((int) x - 15, (int) y - height, 30, height);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getVelocityY() { return velocityY; }
    public boolean isJumping() { return jumping; }
    public boolean isInvincible() { return invincible; }
}

/**
 * Terrain segment representing a slope section
 */
class TerrainSegment {
    private double startX, startY, endX, endY;

    public TerrainSegment(double startX, double startY, double endX, double endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public void scroll(double amount) {
        startX -= amount;
        endX -= amount;
    }

    public double getYAt(double x) {
        if (x < startX || x > endX) return startY;
        double t = (x - startX) / (endX - startX);
        return startY + t * (endY - startY);
    }

    public double getSlope() {
        return (endY - startY) / (endX - startX);
    }

    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getEndX() { return endX; }
    public double getEndY() { return endY; }
}

/**
 * Obstacle class (trees, rocks, etc.)
 */
class Obstacle {
    private double x, y;
    private int type;
    private boolean active = true;

    public Obstacle(double x, double y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void scroll(double amount) {
        x -= amount;
    }

    public void draw(Graphics2D g2d) {
        if (!active) return;

        switch (type) {
            case 0: // Tree
                drawTree(g2d);
                break;
            case 1: // Rock
                drawRock(g2d);
                break;
            case 2: // Flag
                drawFlag(g2d);
                break;
        }
    }

    private void drawTree(Graphics2D g2d) {
        // Trunk
        g2d.setColor(new Color(100, 70, 40));
        g2d.fillRect((int) x - 5, (int) y - 10, 10, 40);

        // Foliage layers
        int[] xPoints = {(int) x - 25, (int) x, (int) x + 25};
        int[] yPoints = {(int) y - 10, (int) y - 50, (int) y - 10};
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillPolygon(xPoints, yPoints, 3);

        xPoints = new int[]{(int) x - 20, (int) x, (int) x + 20};
        yPoints = new int[]{(int) y - 30, (int) y - 65, (int) y - 30};
        g2d.setColor(new Color(0, 128, 0));
        g2d.fillPolygon(xPoints, yPoints, 3);

        xPoints = new int[]{(int) x - 15, (int) x, (int) x + 15};
        yPoints = new int[]{(int) y - 50, (int) y - 80, (int) y - 50};
        g2d.setColor(new Color(0, 100, 0));
        g2d.fillPolygon(xPoints, yPoints, 3);

        // Snow on tree
        g2d.setColor(Color.WHITE);
        g2d.fillArc((int) x - 10, (int) y - 82, 20, 10, 0, 180);
    }

    private void drawRock(Graphics2D g2d) {
        g2d.setColor(new Color(100, 100, 110));
        g2d.fillOval((int) x - 20, (int) y - 15, 40, 30);
        g2d.setColor(new Color(80, 80, 90));
        g2d.fillOval((int) x - 15, (int) y - 20, 25, 20);
        g2d.setColor(new Color(120, 120, 130));
        g2d.fillOval((int) x - 5, (int) y - 10, 15, 10);

        // Snow patches
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int) x - 8, (int) y - 18, 10, 5);
    }

    private void drawFlag(Graphics2D g2d) {
        // Pole
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect((int) x - 2, (int) y - 60, 4, 60);

        // Flag
        g2d.setColor(Color.RED);
        int[] xPoints = {(int) x + 2, (int) x + 30, (int) x + 2};
        int[] yPoints = {(int) y - 60, (int) y - 50, (int) y - 40};
        g2d.fillPolygon(xPoints, yPoints, 3);
    }

    public Rectangle getBounds() {
        switch (type) {
            case 0: return new Rectangle((int) x - 15, (int) y - 80, 30, 80);
            case 1: return new Rectangle((int) x - 20, (int) y - 20, 40, 30);
            case 2: return new Rectangle((int) x - 2, (int) y - 60, 32, 60);
            default: return new Rectangle((int) x - 15, (int) y - 30, 30, 30);
        }
    }

    public double getX() { return x; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

/**
 * Collectible item (coins, stars, etc.)
 */
class Collectible {
    private double x, y;
    private int type;
    private double bobOffset = 0;
    private double rotation = 0;

    public Collectible(double x, double y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void scroll(double amount) {
        x -= amount;
    }

    public void update() {
        bobOffset = Math.sin(System.currentTimeMillis() / 200.0) * 5;
        rotation += 3;
    }

    public void collect() {
        // Collection logic handled in GamePanel
    }

    public void draw(Graphics2D g2d) {
        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(x, y + bobOffset);
        g2d.rotate(Math.toRadians(rotation));

        switch (type) {
            case 0: // Coin
                g2d.setColor(new Color(255, 215, 0));
                g2d.fillOval(-12, -12, 24, 24);
                g2d.setColor(new Color(255, 235, 100));
                g2d.fillOval(-8, -8, 16, 16);
                g2d.setColor(new Color(200, 170, 0));
                g2d.drawOval(-12, -12, 24, 24);
                break;

            case 1: // Star
                drawStar(g2d, 15);
                break;

            case 2: // Diamond
                g2d.setColor(new Color(100, 200, 255));
                int[] xPoints = {0, 12, 0, -12};
                int[] yPoints = {-15, 0, 15, 0};
                g2d.fillPolygon(xPoints, yPoints, 4);
                g2d.setColor(new Color(150, 230, 255));
                g2d.fillPolygon(new int[]{0, 6, 0, -6}, new int[]{-10, 0, 10, 0}, 4);
                break;
        }

        g2d.setTransform(oldTransform);
    }

    private void drawStar(Graphics2D g2d, int size) {
        int[] xPoints = new int[10];
        int[] yPoints = new int[10];

        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 + i * Math.PI / 5;
            double radius = (i % 2 == 0) ? size : size / 2.5;
            xPoints[i] = (int) (Math.cos(angle) * radius);
            yPoints[i] = (int) (-Math.sin(angle) * radius);
        }

        g2d.setColor(new Color(255, 255, 100));
        g2d.fillPolygon(xPoints, yPoints, 10);
        g2d.setColor(new Color(255, 200, 50));
        g2d.drawPolygon(xPoints, yPoints, 10);
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x - 15, (int) y - 15 + (int) bobOffset, 30, 30);
    }

    public int getValue() {
        switch (type) {
            case 0: return 10;
            case 1: return 25;
            case 2: return 50;
            default: return 10;
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
}

/**
 * Snow particle for visual effect
 */
class SnowParticle {
    private double x, y;
    private int size;
    private double speed;
    private double wobble = 0;

    public SnowParticle(double x, double y, int size, double speed) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.speed = speed;
    }

    public void update(double windSpeed) {
        wobble += 0.1;
        x -= windSpeed + Math.sin(wobble) * 0.5;
        y += speed;
    }

    public void reset(double newX, double newY) {
        this.x = newX;
        this.y = newY;
    }

    public void draw(Graphics2D g2d) {
        g2d.fillOval((int) x, (int) y, size, size);
    }

    public double getX() { return x; }
    public double getY() { return y; }
}

/**
 * Trick particle for visual effects
 */
class TrickParticle {
    private double x, y;
    private double velocityX, velocityY;
    private Color color;
    private int life = 60;
    private int size = 8;

    public TrickParticle(double x, double y, Color color, double angle, double speed) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.velocityX = Math.cos(angle) * speed;
        this.velocityY = Math.sin(angle) * speed;
    }

    public void update() {
        x += velocityX;
        y += velocityY;
        velocityY += 0.1; // Gravity
        life--;
        size = Math.max(1, size - 1);
    }

    public void draw(Graphics2D g2d) {
        int alpha = Math.min(255, life * 4);
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g2d.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
    }

    public boolean isAlive() {
        return life > 0;
    }
}

/**
 * Background mountain
 */
class Mountain {
    private double x;
    private int height;
    private int width;
    private Color color;

    public Mountain(double x, int height, int width, Color color) {
        this.x = x;
        this.height = height;
        this.width = width;
        this.color = color;
    }

    public void draw(Graphics2D g2d) {
        int baseY = 350;
        int[] xPoints = {(int) x, (int) x + width / 2, (int) x + width};
        int[] yPoints = {baseY, baseY - height, baseY};

        g2d.setColor(color);
        g2d.fillPolygon(xPoints, yPoints, 3);

        // Snow cap
        g2d.setColor(Color.WHITE);
        int capHeight = height / 4;
        int[] capX = {(int) x + width / 4, (int) x + width / 2, (int) x + width * 3 / 4};
        int[] capY = {baseY - height + capHeight, baseY - height, baseY - height + capHeight};
        g2d.fillPolygon(capX, capY, 3);
    }
}

/**
 * Background cloud
 */
class Cloud {
    private double x, y;
    private int width;
    private double speed;

    public Cloud(double x, double y, int width, double speed) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.speed = speed;
    }

    public void update(double baseSpeed) {
        x -= baseSpeed * speed;
    }

    public void reset(double newX, double newY) {
        this.x = newX;
        this.y = newY;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.fillOval((int) x, (int) y, width, width / 2);
        g2d.fillOval((int) x + width / 4, (int) y - width / 6, width / 2, width / 3);
        g2d.fillOval((int) x + width / 2, (int) y, width / 2, width / 3);
    }

    public double getX() { return x; }
    public int getWidth() { return width; }
}