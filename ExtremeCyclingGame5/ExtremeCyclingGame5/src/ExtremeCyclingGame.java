import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ExtremeCyclingGame extends JPanel implements ActionListener, KeyListener {
    private static final int GAME_WIDTH = 1200;
    private static final int GAME_HEIGHT = 800;
    private static final int GROUND_HEIGHT = 150;
    private static final int FPS = 60;

    // Game state
    private Timer gameTimer;
    private boolean gameRunning = false;
    private boolean gameOver = false;
    private int score = 0;
    private int lives = 3;
    private int level = 1;
    private double gameSpeed = 2.0;
    private Random random = new Random();

    // Player bike
    private Bike player;

    // Game objects
    private List<Obstacle> obstacles;
    private List<Collectible> collectibles;
    private List<Ramp> ramps;
    private List<Particle> particles;
    private List<Cloud> clouds;
    private Background background;

    // Input handling
    private boolean upPressed, downPressed, leftPressed, rightPressed;
    private boolean spacePressed;

    // Animation variables
    private int frameCount = 0;
    private double cameraShake = 0;
    private int invulnerabilityTimer = 0;

    public ExtremeCyclingGame() {
        this.setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        this.setBackground(Color.CYAN);
        this.setFocusable(true);
        this.addKeyListener(this);

        initializeGame();

        gameTimer = new Timer(1000 / FPS, this);
        gameTimer.start();
    }

    private void initializeGame() {
        player = new Bike(100, GAME_HEIGHT - GROUND_HEIGHT - 100);
        obstacles = new ArrayList<>();
        collectibles = new ArrayList<>();
        ramps = new ArrayList<>();
        particles = new ArrayList<>();
        clouds = new ArrayList<>();
        background = new Background();

        // Initialize clouds
        for (int i = 0; i < 8; i++) {
            clouds.add(new Cloud(random.nextInt(GAME_WIDTH * 2),
                    random.nextInt(200) + 50));
        }

        gameRunning = true;
        gameOver = false;
        score = 0;
        lives = 3;
        level = 1;
        gameSpeed = 2.0;
        invulnerabilityTimer = 0;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Apply camera shake
        if (cameraShake > 0) {
            g2d.translate(random.nextInt((int)cameraShake) - cameraShake/2,
                    random.nextInt((int)cameraShake) - cameraShake/2);
            cameraShake *= 0.9;
        }

        // Draw background
        background.draw(g2d);

        // Draw clouds
        for (Cloud cloud : clouds) {
            cloud.draw(g2d);
        }

        // Draw ground
        drawGround(g2d);

        // Draw ramps
        for (Ramp ramp : ramps) {
            ramp.draw(g2d);
        }

        // Draw obstacles
        for (Obstacle obstacle : obstacles) {
            obstacle.draw(g2d);
        }

        // Draw collectibles
        for (Collectible collectible : collectibles) {
            collectible.draw(g2d);
        }

        // Draw particles
        for (Particle particle : particles) {
            particle.draw(g2d);
        }

        // Draw player
        player.draw(g2d);

        // Draw UI
        drawUI(g2d);

        if (gameOver) {
            drawGameOver(g2d);
        }
    }

    private void drawGround(Graphics2D g2d) {
        // Grass
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(0, GAME_HEIGHT - GROUND_HEIGHT, GAME_WIDTH, GROUND_HEIGHT);

        // Dirt
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect(0, GAME_HEIGHT - 50, GAME_WIDTH, 50);

        // Road
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, GAME_HEIGHT - GROUND_HEIGHT, GAME_WIDTH, 20);

        // Road lines
        g2d.setColor(Color.YELLOW);
        for (int i = 0; i < GAME_WIDTH; i += 100) {
            g2d.fillRect(i + (frameCount % 100), GAME_HEIGHT - GROUND_HEIGHT + 8, 50, 4);
        }
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Score: " + score, 20, 30);
        g2d.drawString("Lives: " + lives, 20, 60);
        g2d.drawString("Level: " + level, 20, 90);
        g2d.drawString("Speed: " + String.format("%.1f", gameSpeed), 20, 120);

        // Speed meter
        g2d.setColor(Color.RED);
        g2d.fillRect(GAME_WIDTH - 220, 20, 200, 20);
        g2d.setColor(Color.GREEN);
        g2d.fillRect(GAME_WIDTH - 220, 20, (int)(200 * Math.min(gameSpeed / 10.0, 1.0)), 20);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(GAME_WIDTH - 220, 20, 200, 20);
        g2d.drawString("Speed Meter", GAME_WIDTH - 220, 60);
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String gameOverText = "GAME OVER";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (GAME_WIDTH - fm.stringWidth(gameOverText)) / 2;
        g2d.drawString(gameOverText, x, GAME_HEIGHT / 2 - 50);

        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        String scoreText = "Final Score: " + score;
        fm = g2d.getFontMetrics();
        x = (GAME_WIDTH - fm.stringWidth(scoreText)) / 2;
        g2d.drawString(scoreText, x, GAME_HEIGHT / 2);

        String restartText = "Press SPACE to restart";
        fm = g2d.getFontMetrics();
        x = (GAME_WIDTH - fm.stringWidth(restartText)) / 2;
        g2d.drawString(restartText, x, GAME_HEIGHT / 2 + 50);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning && !gameOver) {
            update();
        }
        repaint();
    }

    private void update() {
        frameCount++;

        // Update player
        player.update();
        handleInput();

        // Update game objects
        updateObstacles();
        updateCollectibles();
        updateRamps();
        updateParticles();
        updateClouds();
        background.update();

        // Spawn new objects
        spawnObjects();

        // Check collisions
        checkCollisions();

        // Update game state
        updateGameState();

        // Reduce invulnerability timer
        if (invulnerabilityTimer > 0) {
            invulnerabilityTimer--;
        }
    }

    private void handleInput() {
        if (upPressed && player.y > 0) {
            player.velocityY = -8;
        }
        if (downPressed && !player.onGround) {
            player.velocityY += 2;
        }
        if (leftPressed && player.x > 0) {
            player.x -= 3;
        }
        if (rightPressed && player.x < GAME_WIDTH - 100) {
            player.x += 3;
        }
        if (spacePressed) {
            player.boost();
        }
    }

    private void updateObstacles() {
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obstacle = obstacles.get(i);
            obstacle.update();
            if (obstacle.x + obstacle.width < 0) {
                obstacles.remove(i);
                score += 10;
            }
        }
    }

    private void updateCollectibles() {
        for (int i = collectibles.size() - 1; i >= 0; i--) {
            Collectible collectible = collectibles.get(i);
            collectible.update();
            if (collectible.x + collectible.width < 0) {
                collectibles.remove(i);
            }
        }
    }

    private void updateRamps() {
        for (int i = ramps.size() - 1; i >= 0; i--) {
            Ramp ramp = ramps.get(i);
            ramp.update();
            if (ramp.x + ramp.width < 0) {
                ramps.remove(i);
            }
        }
    }

    private void updateParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle particle = particles.get(i);
            particle.update();
            if (particle.life <= 0) {
                particles.remove(i);
            }
        }
    }

    private void updateClouds() {
        for (Cloud cloud : clouds) {
            cloud.update();
            if (cloud.x + cloud.width < 0) {
                cloud.x = GAME_WIDTH + random.nextInt(400);
                cloud.y = random.nextInt(200) + 50;
            }
        }
    }

    private void spawnObjects() {
        // Spawn obstacles
        if (frameCount % (int)(120 / gameSpeed) == 0) {
            obstacles.add(new Obstacle(GAME_WIDTH,
                    GAME_HEIGHT - GROUND_HEIGHT - random.nextInt(60) - 40));
        }

        // Spawn collectibles
        if (frameCount % (int)(180 / gameSpeed) == 0) {
            collectibles.add(new Collectible(GAME_WIDTH,
                    GAME_HEIGHT - GROUND_HEIGHT - random.nextInt(150) - 50));
        }

        // Spawn ramps
        if (frameCount % (int)(300 / gameSpeed) == 0) {
            ramps.add(new Ramp(GAME_WIDTH, GAME_HEIGHT - GROUND_HEIGHT));
        }

        // Add engine particles
        if (frameCount % 3 == 0) {
            particles.add(new Particle(player.x - 10, player.y + 25,
                    -gameSpeed - random.nextDouble() * 2,
                    random.nextDouble() * 2 - 1));
        }
    }

    private void checkCollisions() {
        Rectangle playerRect = new Rectangle((int)player.x, (int)player.y,
                player.width, player.height);

        // Check obstacle collisions
        for (Obstacle obstacle : obstacles) {
            Rectangle obstacleRect = new Rectangle((int)obstacle.x, (int)obstacle.y,
                    obstacle.width, obstacle.height);
            if (playerRect.intersects(obstacleRect) && invulnerabilityTimer <= 0) {
                handleCollision();
                break;
            }
        }

        // Check collectible collisions
        for (int i = collectibles.size() - 1; i >= 0; i--) {
            Collectible collectible = collectibles.get(i);
            Rectangle collectibleRect = new Rectangle((int)collectible.x, (int)collectible.y,
                    collectible.width, collectible.height);
            if (playerRect.intersects(collectibleRect)) {
                collectibles.remove(i);
                score += 50;
                // Add collection particles
                for (int j = 0; j < 8; j++) {
                    particles.add(new Particle(collectible.x + collectible.width/2,
                            collectible.y + collectible.height/2,
                            random.nextDouble() * 8 - 4,
                            random.nextDouble() * 8 - 4));
                }
            }
        }

        // Check ramp collisions
        for (Ramp ramp : ramps) {
            Rectangle rampRect = new Rectangle((int)ramp.x, (int)ramp.y,
                    ramp.width, ramp.height);
            if (playerRect.intersects(rampRect)) {
                player.velocityY = -15;
                player.onRamp = true;
                break;
            }
        }
    }

    private void handleCollision() {
        lives--;
        invulnerabilityTimer = 120; // 2 seconds at 60 FPS
        cameraShake = 20;

        // Add crash particles
        for (int i = 0; i < 15; i++) {
            particles.add(new Particle(player.x + player.width/2, player.y + player.height/2,
                    random.nextDouble() * 12 - 6,
                    random.nextDouble() * 12 - 6));
        }

        if (lives <= 0) {
            gameOver = true;
        }
    }

    private void updateGameState() {
        // Increase difficulty
        gameSpeed += 0.001;

        // Level progression
        if (score / 500 + 1 > level) {
            level++;
            lives++; // Bonus life for new level
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                upPressed = true;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                downPressed = true;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                leftPressed = true;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                rightPressed = true;
                break;
            case KeyEvent.VK_SPACE:
                if (gameOver) {
                    initializeGame();
                } else {
                    spacePressed = true;
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                upPressed = false;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                downPressed = false;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                leftPressed = false;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                rightPressed = false;
                break;
            case KeyEvent.VK_SPACE:
                spacePressed = false;
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Game object classes
    class Bike {
        double x, y, velocityX, velocityY;
        int width = 80, height = 60;
        boolean onGround = false;
        boolean onRamp = false;
        double rotation = 0;
        int boostTimer = 0;

        public Bike(double x, double y) {
            this.x = x;
            this.y = y;
            this.velocityX = 0;
            this.velocityY = 0;
        }

        public void update() {
            // Apply gravity
            if (!onGround) {
                velocityY += 0.5;
            }

            // Update position
            y += velocityY;

            // Ground collision
            if (y >= GAME_HEIGHT - GROUND_HEIGHT - height) {
                y = GAME_HEIGHT - GROUND_HEIGHT - height;
                velocityY = 0;
                onGround = true;
                onRamp = false;
            } else {
                onGround = false;
            }

            // Update rotation based on velocity
            if (!onGround) {
                rotation += velocityY * 0.02;
            } else {
                rotation *= 0.9;
            }

            // Update boost
            if (boostTimer > 0) {
                boostTimer--;
            }
        }

        public void boost() {
            if (boostTimer <= 0) {
                boostTimer = 60;
                // Add boost particles
                for (int i = 0; i < 10; i++) {
                    particles.add(new Particle(x - 20, y + height/2,
                            -gameSpeed * 2 - random.nextDouble() * 4,
                            random.nextDouble() * 6 - 3));
                }
            }
        }

        public void draw(Graphics2D g2d) {
            AffineTransform old = g2d.getTransform();

            // Apply invulnerability flashing
            if (invulnerabilityTimer > 0 && invulnerabilityTimer % 10 < 5) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            }

            // Apply rotation
            g2d.rotate(rotation, x + width/2, y + height/2);

            // Draw bike body
            g2d.setColor(Color.RED);
            g2d.fillRect((int)x, (int)y + 20, width, 20);

            // Draw wheels
            g2d.setColor(Color.BLACK);
            g2d.fillOval((int)x + 5, (int)y + 35, 25, 25);
            g2d.fillOval((int)x + 50, (int)y + 35, 25, 25);

            // Draw rider
            g2d.setColor(Color.BLUE);
            g2d.fillRect((int)x + 25, (int)y, 30, 25);

            // Draw handlebars
            g2d.setColor(Color.GRAY);
            g2d.fillRect((int)x + 15, (int)y + 5, 20, 5);

            // Draw boost effect
            if (boostTimer > 0) {
                g2d.setColor(Color.ORANGE);
                g2d.fillRect((int)x - 30, (int)y + 20, 25, 8);
            }

            g2d.setTransform(old);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }

    class Obstacle {
        double x, y;
        int width, height;
        Color color;

        public Obstacle(double x, double y) {
            this.x = x;
            this.y = y;
            this.width = random.nextInt(30) + 20;
            this.height = random.nextInt(40) + 30;
            this.color = new Color(random.nextInt(100) + 100, 0, 0);
        }

        public void update() {
            x -= gameSpeed;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.fillRect((int)x, (int)y, width, height);
            g2d.setColor(Color.BLACK);
            g2d.drawRect((int)x, (int)y, width, height);
        }
    }

    class Collectible {
        double x, y;
        int width = 30, height = 30;
        double rotation = 0;

        public Collectible(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void update() {
            x -= gameSpeed;
            rotation += 0.1;
        }

        public void draw(Graphics2D g2d) {
            AffineTransform old = g2d.getTransform();
            g2d.rotate(rotation, x + width/2, y + height/2);

            g2d.setColor(Color.YELLOW);
            g2d.fillOval((int)x, (int)y, width, height);
            g2d.setColor(Color.ORANGE);
            g2d.fillOval((int)x + 5, (int)y + 5, width - 10, height - 10);

            g2d.setTransform(old);
        }
    }

    class Ramp {
        double x, y;
        int width = 100, height = 30;

        public Ramp(double x, double y) {
            this.x = x;
            this.y = y - height;
        }

        public void update() {
            x -= gameSpeed;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.GRAY);
            int[] xPoints = {(int)x, (int)x + width, (int)x + width, (int)x};
            int[] yPoints = {(int)y + height, (int)y, (int)y + height, (int)y + height};
            g2d.fillPolygon(xPoints, yPoints, 4);
            g2d.setColor(Color.BLACK);
            g2d.drawPolygon(xPoints, yPoints, 4);
        }
    }

    class Particle {
        double x, y, velocityX, velocityY;
        int life = 60;
        Color color;

        public Particle(double x, double y, double velocityX, double velocityY) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.color = new Color(255, random.nextInt(200) + 55, 0);
        }

        public void update() {
            x += velocityX;
            y += velocityY;
            velocityY += 0.1;
            life--;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    Math.max(0, life * 4)));
            g2d.fillOval((int)x, (int)y, 4, 4);
        }
    }

    class Cloud {
        double x, y;
        int width, height;

        public Cloud(double x, double y) {
            this.x = x;
            this.y = y;
            this.width = random.nextInt(80) + 60;
            this.height = random.nextInt(40) + 30;
        }

        public void update() {
            x -= gameSpeed * 0.2;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int)x, (int)y, width, height);
            g2d.fillOval((int)x + 20, (int)y - 10, width - 20, height);
            g2d.fillOval((int)x + 40, (int)y - 5, width - 40, height - 10);
        }
    }

    class Background {
        private double mountainOffset = 0;

        public void update() {
            mountainOffset -= gameSpeed * 0.1;
            if (mountainOffset <= -GAME_WIDTH) {
                mountainOffset = 0;
            }
        }

        public void draw(Graphics2D g2d) {
            // Sky gradient
            GradientPaint skyGradient = new GradientPaint(0, 0, Color.CYAN,
                    0, GAME_HEIGHT/2, Color.BLUE);
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT/2);

            // Mountains
            g2d.setColor(new Color(100, 100, 100));
            for (int i = 0; i < 3; i++) {
                int[] xPoints = {(int)mountainOffset + i * 400,
                        (int)mountainOffset + i * 400 + 200,
                        (int)mountainOffset + i * 400 + 400};
                int[] yPoints = {GAME_HEIGHT/2, GAME_HEIGHT/2 - 150, GAME_HEIGHT/2};
                g2d.fillPolygon(xPoints, yPoints, 3);
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Extreme Cycling Game");
        ExtremeCyclingGame game = new ExtremeCyclingGame();

        frame.add(game);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}