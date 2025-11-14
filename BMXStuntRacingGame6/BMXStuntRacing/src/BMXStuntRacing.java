import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class BMXStuntRacing extends JPanel implements ActionListener, KeyListener {
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 700;
    private static final int GROUND_LEVEL = 550;
    private static final int FPS = 60;

    // Game state
    private Timer gameTimer;
    private boolean gameRunning = false;
    private boolean gameOver = false;
    private int score = 0;
    private int highScore = 0;
    private int level = 1;
    private double gameSpeed = 5.0;

    // Player bike
    private BMXBike bike;

    // Game objects
    private ArrayList<Obstacle> obstacles;
    private ArrayList<Ramp> ramps;
    private ArrayList<Particle> particles;
    private ArrayList<Coin> coins;

    // Background scrolling
    private double backgroundOffset = 0;
    private double terrainOffset = 0;

    // Input handling
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean spacePressed = false;

    // Random generator
    private Random random = new Random();

    // Spawn timers
    private int obstacleSpawnTimer = 0;
    private int rampSpawnTimer = 0;
    private int coinSpawnTimer = 0;

    public BMXStuntRacing() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky blue
        setFocusable(true);
        addKeyListener(this);

        initGame();

        gameTimer = new Timer(1000 / FPS, this);
        gameTimer.start();
    }

    private void initGame() {
        bike = new BMXBike(200, GROUND_LEVEL);
        obstacles = new ArrayList<>();
        ramps = new ArrayList<>();
        particles = new ArrayList<>();
        coins = new ArrayList<>();

        score = 0;
        level = 1;
        gameSpeed = 5.0;
        gameOver = false;
        gameRunning = false;

        backgroundOffset = 0;
        terrainOffset = 0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning && !gameOver) {
            return;
        }

        if (gameOver) {
            return;
        }

        updateGame();
        repaint();
    }

    private void updateGame() {
        // Update bike
        bike.update(leftPressed, rightPressed, upPressed, downPressed, spacePressed);

        // Update background scrolling
        backgroundOffset += gameSpeed * 0.3;
        terrainOffset += gameSpeed;

        if (backgroundOffset > WINDOW_WIDTH) {
            backgroundOffset = 0;
        }
        if (terrainOffset > 100) {
            terrainOffset = 0;
        }

        // Spawn obstacles
        obstacleSpawnTimer++;
        if (obstacleSpawnTimer > 120 - (level * 5)) {
            if (random.nextDouble() < 0.7) {
                int type = random.nextInt(3);
                obstacles.add(new Obstacle(WINDOW_WIDTH, GROUND_LEVEL, type));
            }
            obstacleSpawnTimer = 0;
        }

        // Spawn ramps
        rampSpawnTimer++;
        if (rampSpawnTimer > 200 - (level * 10)) {
            if (random.nextDouble() < 0.6) {
                ramps.add(new Ramp(WINDOW_WIDTH, GROUND_LEVEL));
            }
            rampSpawnTimer = 0;
        }

        // Spawn coins
        coinSpawnTimer++;
        if (coinSpawnTimer > 80) {
            if (random.nextDouble() < 0.5) {
                int coinY = GROUND_LEVEL - random.nextInt(200) - 50;
                coins.add(new Coin(WINDOW_WIDTH, coinY));
            }
            coinSpawnTimer = 0;
        }

        // Update and check obstacles
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obs = obstacles.get(i);
            obs.x -= gameSpeed;

            if (obs.x < -100) {
                obstacles.remove(i);
                score += 10;
            } else if (bike.checkCollision(obs) && !bike.isInAir()) {
                if (bike.rotation == 0) {
                    gameOver();
                    return;
                }
            }
        }

        // Update and check ramps
        for (int i = ramps.size() - 1; i >= 0; i--) {
            Ramp ramp = ramps.get(i);
            ramp.x -= gameSpeed;

            if (ramp.x < -150) {
                ramps.remove(i);
            } else if (bike.checkRampCollision(ramp)) {
                bike.launchFromRamp();
                createParticles(bike.x, bike.y, Color.ORANGE);
            }
        }

        // Update and check coins
        for (int i = coins.size() - 1; i >= 0; i--) {
            Coin coin = coins.get(i);
            coin.update();
            coin.x -= gameSpeed;

            if (coin.x < -30) {
                coins.remove(i);
            } else if (bike.checkCoinCollision(coin)) {
                coins.remove(i);
                score += 50;
                createParticles(coin.x, coin.y, Color.YELLOW);
            }
        }

        // Update particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.isDead()) {
                particles.remove(i);
            }
        }

        // Check for successful tricks
        if (bike.trickCompleted) {
            score += bike.trickScore;
            createParticles(bike.x, bike.y, Color.CYAN);
            bike.trickCompleted = false;
        }

        // Level progression
        if (score > level * 1000) {
            level++;
            gameSpeed += 0.5;
        }

        // Update high score
        if (score > highScore) {
            highScore = score;
        }
    }

    private void createParticles(double x, double y, Color color) {
        for (int i = 0; i < 15; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    private void gameOver() {
        gameOver = true;
        gameRunning = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background
        drawBackground(g2d);

        // Draw game objects
        if (gameRunning || gameOver) {
            // Draw ramps
            for (Ramp ramp : ramps) {
                ramp.draw(g2d);
            }

            // Draw obstacles
            for (Obstacle obs : obstacles) {
                obs.draw(g2d);
            }

            // Draw coins
            for (Coin coin : coins) {
                coin.draw(g2d);
            }

            // Draw particles
            for (Particle p : particles) {
                p.draw(g2d);
            }

            // Draw bike
            bike.draw(g2d);

            // Draw ground
            drawGround(g2d);

            // Draw HUD
            drawHUD(g2d);
        }

        // Draw start screen
        if (!gameRunning && !gameOver) {
            drawStartScreen(g2d);
        }

        // Draw game over screen
        if (gameOver) {
            drawGameOverScreen(g2d);
        }
    }

    private void drawBackground(Graphics2D g2d) {
        // Sky gradient
        GradientPaint skyGradient = new GradientPaint(
                0, 0, new Color(135, 206, 235),
                0, GROUND_LEVEL, new Color(176, 224, 230)
        );
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WINDOW_WIDTH, GROUND_LEVEL);

        // Clouds
        g2d.setColor(new Color(255, 255, 255, 150));
        for (int i = 0; i < 5; i++) {
            int cloudX = (int)((i * 300 - backgroundOffset) % (WINDOW_WIDTH + 300));
            drawCloud(g2d, cloudX, 80 + i * 30);
        }

        // Mountains
        g2d.setColor(new Color(100, 100, 100, 100));
        for (int i = 0; i < 3; i++) {
            int mountainX = (int)((i * 400 - backgroundOffset * 0.5) % (WINDOW_WIDTH + 400));
            drawMountain(g2d, mountainX, GROUND_LEVEL - 100);
        }
    }

    private void drawCloud(Graphics2D g2d, int x, int y) {
        g2d.fillOval(x, y, 60, 30);
        g2d.fillOval(x + 20, y - 10, 50, 30);
        g2d.fillOval(x + 40, y, 60, 30);
    }

    private void drawMountain(Graphics2D g2d, int x, int y) {
        int[] xPoints = {x, x + 100, x + 200};
        int[] yPoints = {y + 100, y - 50, y + 100};
        g2d.fillPolygon(xPoints, yPoints, 3);
    }

    private void drawGround(Graphics2D g2d) {
        // Ground
        GradientPaint groundGradient = new GradientPaint(
                0, GROUND_LEVEL, new Color(101, 67, 33),
                0, WINDOW_HEIGHT, new Color(76, 51, 25)
        );
        g2d.setPaint(groundGradient);
        g2d.fillRect(0, GROUND_LEVEL, WINDOW_WIDTH, WINDOW_HEIGHT - GROUND_LEVEL);

        // Grass on top
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(0, GROUND_LEVEL, WINDOW_WIDTH, 5);

        // Terrain details
        g2d.setColor(new Color(101, 67, 33, 150));
        for (int i = 0; i < WINDOW_WIDTH / 50 + 1; i++) {
            int x = (int)((i * 50 - terrainOffset) % WINDOW_WIDTH);
            g2d.fillRect(x, GROUND_LEVEL + 10, 30, 3);
            g2d.fillRect(x + 10, GROUND_LEVEL + 20, 20, 2);
        }
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 280, 120, 10, 10);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("Score: " + score, 25, 40);
        g2d.drawString("High Score: " + highScore, 25, 70);
        g2d.drawString("Level: " + level, 25, 100);

        // Speed indicator
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("Speed: " + String.format("%.1f", gameSpeed), 25, 125);

        // Trick meter
        if (bike.isInAir() && bike.rotation != 0) {
            g2d.setColor(new Color(255, 215, 0, 200));
            g2d.fillRoundRect(WINDOW_WIDTH - 220, 10, 200, 60, 10, 10);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            int rotations = Math.abs((int)(bike.rotation / 360));
            if (rotations > 0) {
                g2d.drawString("Trick: " + rotations + " Flip(s)!", WINDOW_WIDTH - 210, 40);
            }
        }
    }

    private void drawStartScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        g2d.setColor(new Color(255, 215, 0));
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        String title = "BMX STUNT RACING";
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, (WINDOW_WIDTH - titleWidth) / 2, 200);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 28));
        String[] instructions = {
                "Controls:",
                "UP ARROW - Accelerate/Jump",
                "DOWN ARROW - Brake",
                "LEFT/RIGHT ARROWS - Rotate (in air)",
                "SPACE - Boost",
                "",
                "Perform flips for bonus points!",
                "Collect coins for extra score!",
                "",
                "Press ENTER to Start"
        };

        int y = 280;
        for (String line : instructions) {
            int width = g2d.getFontMetrics().stringWidth(line);
            g2d.drawString(line, (WINDOW_WIDTH - width) / 2, y);
            y += 40;
        }
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        String gameOverText = "GAME OVER";
        int textWidth = g2d.getFontMetrics().stringWidth(gameOverText);
        g2d.drawString(gameOverText, (WINDOW_WIDTH - textWidth) / 2, 250);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        String scoreText = "Final Score: " + score;
        textWidth = g2d.getFontMetrics().stringWidth(scoreText);
        g2d.drawString(scoreText, (WINDOW_WIDTH - textWidth) / 2, 320);

        String highScoreText = "High Score: " + highScore;
        textWidth = g2d.getFontMetrics().stringWidth(highScoreText);
        g2d.drawString(highScoreText, (WINDOW_WIDTH - textWidth) / 2, 370);

        g2d.setFont(new Font("Arial", Font.PLAIN, 28));
        String restartText = "Press ENTER to Restart";
        textWidth = g2d.getFontMetrics().stringWidth(restartText);
        g2d.drawString(restartText, (WINDOW_WIDTH - textWidth) / 2, 450);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_ENTER) {
            if (!gameRunning) {
                initGame();
                gameRunning = true;
            }
        }

        if (key == KeyEvent.VK_LEFT) leftPressed = true;
        if (key == KeyEvent.VK_RIGHT) rightPressed = true;
        if (key == KeyEvent.VK_UP) upPressed = true;
        if (key == KeyEvent.VK_DOWN) downPressed = true;
        if (key == KeyEvent.VK_SPACE) spacePressed = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT) leftPressed = false;
        if (key == KeyEvent.VK_RIGHT) rightPressed = false;
        if (key == KeyEvent.VK_UP) upPressed = false;
        if (key == KeyEvent.VK_DOWN) downPressed = false;
        if (key == KeyEvent.VK_SPACE) spacePressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // BMX Bike Class
    class BMXBike {
        double x, y;
        double velocityY = 0;
        double velocityX = 0;
        double rotation = 0;
        double rotationSpeed = 0;
        boolean isJumping = false;
        boolean canJump = true;
        int trickScore = 0;
        boolean trickCompleted = false;

        final double GRAVITY = 0.6;
        final double JUMP_POWER = -15;
        final int BIKE_WIDTH = 50;
        final int BIKE_HEIGHT = 30;

        BMXBike(double x, double y) {
            this.x = x;
            this.y = y;
        }

        void update(boolean left, boolean right, boolean up, boolean down, boolean boost) {
            // Apply gravity
            if (y < GROUND_LEVEL - BIKE_HEIGHT) {
                velocityY += GRAVITY;
                isJumping = true;
            } else {
                velocityY = 0;
                y = GROUND_LEVEL - BIKE_HEIGHT;
                isJumping = false;

                // Complete trick on landing
                if (rotation != 0) {
                    int flips = Math.abs((int)(rotation / 360));
                    trickScore = flips * 100;
                    trickCompleted = true;
                    rotation = 0;
                    rotationSpeed = 0;
                }

                canJump = true;
            }

            // Jump
            if (up && canJump && !isJumping) {
                velocityY = JUMP_POWER;
                canJump = false;
            }

            // Rotation in air
            if (isJumping) {
                if (left) {
                    rotationSpeed -= 2;
                }
                if (right) {
                    rotationSpeed += 2;
                }
                rotation += rotationSpeed;
            }

            // Apply velocities
            y += velocityY;
            x += velocityX;

            // Damping
            velocityX *= 0.95;
            rotationSpeed *= 0.98;

            // Keep bike in bounds
            if (x < 50) x = 50;
            if (x > 300) x = 300;
        }

        void launchFromRamp() {
            if (!isJumping) {
                velocityY = JUMP_POWER * 1.3;
                velocityX = 5;
                canJump = false;
            }
        }

        boolean isInAir() {
            return isJumping;
        }

        boolean checkCollision(Obstacle obs) {
            Rectangle bikeBounds = new Rectangle((int)x, (int)y, BIKE_WIDTH, BIKE_HEIGHT);
            Rectangle obsBounds = new Rectangle((int)obs.x, (int)obs.y - obs.height, obs.width, obs.height);
            return bikeBounds.intersects(obsBounds);
        }

        boolean checkRampCollision(Ramp ramp) {
            Rectangle bikeBounds = new Rectangle((int)x, (int)y, BIKE_WIDTH, BIKE_HEIGHT);
            Rectangle rampBounds = new Rectangle((int)ramp.x, (int)ramp.y - ramp.height, ramp.width, ramp.height);
            return bikeBounds.intersects(rampBounds) && !isJumping;
        }

        boolean checkCoinCollision(Coin coin) {
            double dx = x + BIKE_WIDTH/2 - coin.x;
            double dy = y + BIKE_HEIGHT/2 - coin.y;
            double distance = Math.sqrt(dx*dx + dy*dy);
            return distance < 30;
        }

        void draw(Graphics2D g2d) {
            AffineTransform oldTransform = g2d.getTransform();

            // Apply rotation
            g2d.translate(x + BIKE_WIDTH/2, y + BIKE_HEIGHT/2);
            g2d.rotate(Math.toRadians(rotation));
            g2d.translate(-(x + BIKE_WIDTH/2), -(y + BIKE_HEIGHT/2));

            // Draw bike frame
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(3));

            // Main frame
            g2d.drawLine((int)x + 10, (int)y + 15, (int)x + 40, (int)y + 15);
            g2d.drawLine((int)x + 10, (int)y + 15, (int)x + 20, (int)y + 5);
            g2d.drawLine((int)x + 40, (int)y + 15, (int)x + 30, (int)y + 5);
            g2d.drawLine((int)x + 20, (int)y + 5, (int)x + 30, (int)y + 5);

            // Wheels
            g2d.setColor(Color.BLACK);
            g2d.fillOval((int)x + 5, (int)y + 15, 15, 15);
            g2d.fillOval((int)x + 35, (int)y + 15, 15, 15);

            g2d.setColor(Color.GRAY);
            g2d.fillOval((int)x + 8, (int)y + 18, 9, 9);
            g2d.fillOval((int)x + 38, (int)y + 18, 9, 9);

            // Rider
            g2d.setColor(new Color(255, 200, 150));
            g2d.fillOval((int)x + 22, (int)y - 5, 12, 12);

            g2d.setColor(Color.BLUE);
            g2d.fillRect((int)x + 20, (int)y + 5, 10, 12);

            g2d.setTransform(oldTransform);
        }
    }

    // Obstacle Class
    class Obstacle {
        double x, y;
        int width, height;
        int type;
        Color color;

        Obstacle(double x, double y, int type) {
            this.x = x;
            this.y = y;
            this.type = type;

            switch(type) {
                case 0: // Small rock
                    width = 30;
                    height = 25;
                    color = new Color(128, 128, 128);
                    break;
                case 1: // Traffic cone
                    width = 25;
                    height = 40;
                    color = new Color(255, 140, 0);
                    break;
                case 2: // Barrier
                    width = 60;
                    height = 30;
                    color = new Color(255, 0, 0);
                    break;
            }
        }

        void draw(Graphics2D g2d) {
            switch(type) {
                case 0: // Rock
                    g2d.setColor(color);
                    g2d.fillOval((int)x, (int)y - height, width, height);
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.drawOval((int)x, (int)y - height, width, height);
                    break;
                case 1: // Cone
                    int[] xPoints = {(int)x + width/2, (int)x, (int)x + width};
                    int[] yPoints = {(int)y - height, (int)y, (int)y};
                    g2d.setColor(color);
                    g2d.fillPolygon(xPoints, yPoints, 3);
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect((int)x + 5, (int)y - 20, width - 10, 5);
                    break;
                case 2: // Barrier
                    g2d.setColor(color);
                    g2d.fillRect((int)x, (int)y - height, width, height);
                    g2d.setColor(Color.WHITE);
                    for (int i = 0; i < width; i += 15) {
                        g2d.fillRect((int)x + i, (int)y - height, 7, height);
                    }
                    break;
            }
        }
    }

    // Ramp Class
    class Ramp {
        double x, y;
        int width = 120;
        int height = 50;

        Ramp(double x, double y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g2d) {
            int[] xPoints = {(int)x, (int)x + width, (int)x + width};
            int[] yPoints = {(int)y, (int)y - height, (int)y};

            GradientPaint rampGradient = new GradientPaint(
                    (int)x, (int)y - height, new Color(150, 75, 0),
                    (int)x + width, (int)y, new Color(101, 67, 33)
            );
            g2d.setPaint(rampGradient);
            g2d.fillPolygon(xPoints, yPoints, 3);

            g2d.setColor(new Color(80, 50, 20));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawPolygon(xPoints, yPoints, 3);
        }
    }

    // Particle Class
    class Particle {
        double x, y;
        double vx, vy;
        Color color;
        int life = 30;
        int maxLife = 30;

        Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            Random rand = new Random();
            vx = rand.nextDouble() * 6 - 3;
            vy = rand.nextDouble() * 6 - 3;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.3; // Gravity
            life--;
        }

        boolean isDead() {
            return life <= 0;
        }

        void draw(Graphics2D g2d) {
            int alpha = (int)(255 * (life / (double)maxLife));
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            int size = 6 * life / maxLife + 2;
            g2d.fillOval((int)x, (int)y, size, size);
        }
    }

    // Coin Class
    class Coin {
        double x, y;
        double rotation = 0;

        Coin(double x, double y) {
            this.x = x;
            this.y = y;
        }

        void update() {
            rotation += 5;
        }

        void draw(Graphics2D g2d) {
            AffineTransform oldTransform = g2d.getTransform();
            g2d.translate(x, y);
            g2d.rotate(Math.toRadians(rotation));

            // Coin
            GradientPaint coinGradient = new GradientPaint(
                    -15, -15, new Color(255, 223, 0),
                    15, 15, new Color(255, 215, 0)
            );
            g2d.setPaint(coinGradient);
            g2d.fillOval(-15, -15, 30, 30);

            g2d.setColor(new Color(218, 165, 32));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(-15, -15, 30, 30);
            g2d.drawOval(-10, -10, 20, 20);

            g2d.setTransform(oldTransform);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("BMX Stunt Racing");
            BMXStuntRacing game = new BMXStuntRacing();

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