import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class ExtremeCyclingGame extends JPanel implements Runnable, KeyListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int GROUND_HEIGHT = 500;
    private static final int FPS = 60;

    private Thread gameThread;
    private boolean running;
    private BufferedImage image;
    private Graphics2D g;

    // Player variables
    private int playerX = 100;
    private int playerY = GROUND_HEIGHT - 50;
    private int playerWidth = 40;
    private int playerHeight = 60;
    private double playerVelX = 0;
    private double playerVelY = 0;
    private boolean onGround = true;
    private boolean facingRight = true;
    private int playerHealth = 100;
    private int score = 0;
    private int level = 1;

    // Animation variables
    private int frameCount = 0;
    private int animationFrame = 0;
    private final int[] animationSequence = {0, 1, 2, 1};
    private boolean pedaling = false;

    // Game objects
    private ArrayList<Obstacle> obstacles = new ArrayList<>();
    private ArrayList<PowerUp> powerUps = new ArrayList<>();
    private ArrayList<BackgroundElement> backgroundElements = new ArrayList<>();

    // Physics
    private final double gravity = 0.5;
    private final double friction = 0.9;

    // Game states
    private boolean gameOver = false;
    private boolean levelComplete = false;
    private boolean gameStarted = false;

    // Input flags
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean spacePressed = false;

    public ExtremeCyclingGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        g = (Graphics2D) image.getGraphics();

        initializeLevel(level);
    }

    private void initializeLevel(int level) {
        obstacles.clear();
        powerUps.clear();
        backgroundElements.clear();

        // Create terrain based on level
        createTerrain(level);

        // Add obstacles
        int obstacleCount = 3 + level * 2;
        Random rand = new Random();
        for (int i = 0; i < obstacleCount; i++) {
            int x = 300 + rand.nextInt(WIDTH * 2);
            int width = 30 + rand.nextInt(50);
            int height = 20 + rand.nextInt(40);
            obstacles.add(new Obstacle(x, GROUND_HEIGHT - height, width, height));
        }

        // Add power-ups
        for (int i = 0; i < 2 + level; i++) {
            int x = 400 + rand.nextInt(WIDTH * 2);
            powerUps.add(new PowerUp(x, GROUND_HEIGHT - 70, 20, 20, rand.nextInt(3)));
        }

        // Add background elements
        for (int i = 0; i < 10; i++) {
            backgroundElements.add(new BackgroundElement(
                    rand.nextInt(WIDTH * 3),
                    GROUND_HEIGHT - 50 - rand.nextInt(100),
                    30 + rand.nextInt(70),
                    50 + rand.nextInt(100),
                    rand.nextInt(3)
            ));
        }

        levelComplete = false;
    }

    private void createTerrain(int level) {
        // Simple terrain generation - could be enhanced
        int prevHeight = GROUND_HEIGHT;
        Random rand = new Random();

        for (int x = 0; x < WIDTH * 3; x += 50) {
            int heightChange = rand.nextInt(30) - 15;
            if (level > 2) heightChange = rand.nextInt(50) - 25;
            if (level > 5) heightChange = rand.nextInt(80) - 40;

            int newHeight = prevHeight + heightChange;
            if (newHeight < GROUND_HEIGHT - 100) newHeight = GROUND_HEIGHT - 100;
            if (newHeight > GROUND_HEIGHT + 100) newHeight = GROUND_HEIGHT + 100;

            obstacles.add(new Obstacle(x, newHeight, 50, HEIGHT - newHeight));
            prevHeight = newHeight;
        }
    }

    public void startGame() {
        if (gameThread == null) {
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    @Override
    public void run() {
        running = true;

        long lastTime = System.nanoTime();
        double nsPerFrame = 1000000000.0 / FPS;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerFrame;
            lastTime = now;

            while (delta >= 1) {
                update();
                delta--;
            }

            render();
            draw();

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        if (!gameStarted || gameOver) return;

        // Handle player input
        handleInput();

        // Update player position
        playerX += playerVelX;
        playerY += playerVelY;

        // Apply gravity
        if (!onGround) {
            playerVelY += gravity;
        }

        // Apply friction
        if (onGround) {
            playerVelX *= friction;
        }

        // Check boundaries
        if (playerX < 0) playerX = 0;
        if (playerX > WIDTH * 3 - playerWidth) playerX = WIDTH * 3 - playerWidth;
        if (playerY > HEIGHT) {
            playerHealth = 0;
            gameOver = true;
        }

        // Check collisions
        checkCollisions();

        // Update animation
        frameCount++;
        if (frameCount % 5 == 0 && pedaling) {
            animationFrame = (animationFrame + 1) % animationSequence.length;
        }

        // Check level completion
        if (playerX > WIDTH * 2.5) {
            levelComplete = true;
            level++;
            if (level > 10) {
                gameOver = true; // Game won
            } else {
                initializeLevel(level);
                playerX = 100;
                playerY = GROUND_HEIGHT - 50;
            }
        }

        // Update game objects
        updateGameObjects();
    }

    private void handleInput() {
        if (leftPressed) {
            playerVelX = -5;
            facingRight = false;
            pedaling = true;
        }
        if (rightPressed) {
            playerVelX = 5;
            facingRight = true;
            pedaling = true;
        }
        if (!leftPressed && !rightPressed) {
            pedaling = false;
            animationFrame = 0;
        }
        if ((upPressed || spacePressed) && onGround) {
            playerVelY = -12;
            onGround = false;
        }
    }

    private void checkCollisions() {
        onGround = false;

        Rectangle playerRect = new Rectangle(playerX, playerY, playerWidth, playerHeight);

        // Check terrain collisions
        for (Obstacle obstacle : obstacles) {
            Rectangle obstacleRect = new Rectangle(obstacle.x, obstacle.y, obstacle.width, obstacle.height);

            if (playerRect.intersects(obstacleRect)) {
                // Check if collision is from above
                if (playerVelY > 0 && playerY + playerHeight - playerVelY <= obstacle.y) {
                    playerY = obstacle.y - playerHeight;
                    playerVelY = 0;
                    onGround = true;
                }
                // Check if collision is from below
                else if (playerVelY < 0 && playerY - playerVelY >= obstacle.y + obstacle.height) {
                    playerY = obstacle.y + obstacle.height;
                    playerVelY = 0;
                }
                // Check if collision is from left
                else if (playerVelX > 0 && playerX + playerWidth - playerVelX <= obstacle.x) {
                    playerX = obstacle.x - playerWidth;
                    playerVelX = 0;
                    playerHealth -= 5;
                }
                // Check if collision is from right
                else if (playerVelX < 0 && playerX - playerVelX >= obstacle.x + obstacle.width) {
                    playerX = obstacle.x + obstacle.width;
                    playerVelX = 0;
                    playerHealth -= 5;
                }
            }
        }

        // Check power-up collisions
        for (int i = 0; i < powerUps.size(); i++) {
            PowerUp powerUp = powerUps.get(i);
            Rectangle powerUpRect = new Rectangle(powerUp.x, powerUp.y, powerUp.width, powerUp.height);

            if (playerRect.intersects(powerUpRect)) {
                applyPowerUp(powerUp.type);
                powerUps.remove(i);
                i--;
                score += 50;
            }
        }

        // Check game over condition
        if (playerHealth <= 0) {
            gameOver = true;
        }
    }

    private void applyPowerUp(int type) {
        switch (type) {
            case 0: // Health
                playerHealth = Math.min(100, playerHealth + 30);
                break;
            case 1: // Speed boost
                playerVelX *= 1.5;
                break;
            case 2: // Jump boost
                playerVelY = -15;
                onGround = false;
                break;
        }
    }

    private void updateGameObjects() {
        // Remove off-screen obstacles and power-ups
        for (int i = 0; i < obstacles.size(); i++) {
            if (obstacles.get(i).x + obstacles.get(i).width < playerX - WIDTH) {
                obstacles.remove(i);
                i--;
            }
        }

        for (int i = 0; i < powerUps.size(); i++) {
            if (powerUps.get(i).x + powerUps.get(i).width < playerX - WIDTH) {
                powerUps.remove(i);
                i--;
            }
        }

        // Add new obstacles and power-ups as player progresses
        if (Math.random() < 0.02) {
            Random rand = new Random();
            int x = playerX + WIDTH + rand.nextInt(500);
            int width = 30 + rand.nextInt(50);
            int height = 20 + rand.nextInt(40);
            obstacles.add(new Obstacle(x, GROUND_HEIGHT - height, width, height));
        }

        if (Math.random() < 0.01) {
            Random rand = new Random();
            int x = playerX + WIDTH + rand.nextInt(500);
            powerUps.add(new PowerUp(x, GROUND_HEIGHT - 70, 20, 20, rand.nextInt(3)));
        }

        // Update score based on distance
        score += (int) (playerVelX * 0.1);
    }

    private void render() {
        // Clear screen
        g.setColor(new Color(135, 206, 235)); // Sky blue
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw background elements
        for (BackgroundElement element : backgroundElements) {
            if (element.x + element.width > playerX - WIDTH / 2 && element.x < playerX + WIDTH * 1.5) {
                // Calculate parallax effect
                int parallaxOffset = (int) ((element.x - playerX) * 0.3);
                int drawX = WIDTH / 2 + parallaxOffset - playerWidth / 2;

                if (drawX + element.width > 0 && drawX < WIDTH) {
                    Color[] colors = {Color.GREEN.darker(), Color.ORANGE, Color.RED};
                    g.setColor(colors[element.type]);
                    g.fillRect(drawX, element.y, element.width, element.height);
                }
            }
        }

        // Draw ground
        g.setColor(new Color(34, 139, 34)); // Forest green
        g.fillRect(0, GROUND_HEIGHT, WIDTH, HEIGHT - GROUND_HEIGHT);

        // Draw obstacles
        g.setColor(new Color(139, 69, 19)); // Brown
        for (Obstacle obstacle : obstacles) {
            int drawX = obstacle.x - playerX + WIDTH / 2 - playerWidth / 2;
            if (drawX + obstacle.width > 0 && drawX < WIDTH) {
                g.fillRect(drawX, obstacle.y, obstacle.width, obstacle.height);
            }
        }

        // Draw power-ups
        for (PowerUp powerUp : powerUps) {
            int drawX = powerUp.x - playerX + WIDTH / 2 - playerWidth / 2;
            int drawY = powerUp.y;

            if (drawX + powerUp.width > 0 && drawX < WIDTH) {
                Color[] colors = {Color.RED, Color.YELLOW, Color.CYAN};
                g.setColor(colors[powerUp.type]);
                g.fillOval(drawX, drawY, powerUp.width, powerUp.height);
                g.setColor(Color.WHITE);
                g.drawOval(drawX, drawY, powerUp.width, powerUp.height);
            }
        }

        // Draw player
        int drawPlayerX = WIDTH / 2 - playerWidth / 2;
        int drawPlayerY = playerY;

        // Simple bike animation
        g.setColor(Color.BLACK);

        // Bike frame
        g.fillRect(drawPlayerX + 10, drawPlayerY + 20, 20, 10);
        g.fillRect(drawPlayerX + 15, drawPlayerY + 10, 5, 10);

        // Wheels
        g.fillOval(drawPlayerX, drawPlayerY + 25, 15, 15);
        g.fillOval(drawPlayerX + 25, drawPlayerY + 25, 15, 15);

        // Rider
        g.setColor(Color.BLUE);
        g.fillOval(drawPlayerX + 10, drawPlayerY, 15, 15); // Head
        g.fillRect(drawPlayerX + 15, drawPlayerY + 15, 5, 15); // Body

        // Animated legs
        int legFrame = animationSequence[animationFrame];
        if (facingRight) {
            g.fillRect(drawPlayerX + 15, drawPlayerY + 30, 5, 10); // Right leg (stationary)
            g.fillRect(drawPlayerX + 10 - legFrame, drawPlayerY + 30, 5, 10); // Left leg (moving)
        } else {
            g.fillRect(drawPlayerX + 15, drawPlayerY + 30, 5, 10); // Left leg (stationary)
            g.fillRect(drawPlayerX + 20 + legFrame, drawPlayerY + 30, 5, 10); // Right leg (moving)
        }

        // Draw HUD
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Health: " + playerHealth, 20, 20);
        g.drawString("Score: " + score, 20, 40);
        g.drawString("Level: " + level, 20, 60);

        // Draw game messages
        if (!gameStarted) {
            drawCenteredString(g, "EXTREME CYCLING", new Font("Arial", Font.BOLD, 48), WIDTH / 2, HEIGHT / 2 - 60);
            drawCenteredString(g, "Press SPACE to Start", new Font("Arial", Font.PLAIN, 24), WIDTH / 2, HEIGHT / 2);
            drawCenteredString(g, "Arrow Keys to Move | Space to Jump", new Font("Arial", Font.PLAIN, 18), WIDTH / 2, HEIGHT / 2 + 40);
        }

        if (gameOver) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            if (level > 10) {
                drawCenteredString(g, "YOU WIN!", new Font("Arial", Font.BOLD, 48), WIDTH / 2, HEIGHT / 2 - 60);
            } else {
                drawCenteredString(g, "GAME OVER", new Font("Arial", Font.BOLD, 48), WIDTH / 2, HEIGHT / 2 - 60);
            }
            drawCenteredString(g, "Final Score: " + score, new Font("Arial", Font.PLAIN, 24), WIDTH / 2, HEIGHT / 2);
            drawCenteredString(g, "Press R to Restart", new Font("Arial", Font.PLAIN, 24), WIDTH / 2, HEIGHT / 2 + 60);
        }

        if (levelComplete) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            drawCenteredString(g, "LEVEL COMPLETE!", new Font("Arial", Font.BOLD, 48), WIDTH / 2, HEIGHT / 2);
            drawCenteredString(g, "Preparing level " + level + "...", new Font("Arial", Font.PLAIN, 24), WIDTH / 2, HEIGHT / 2 + 60);
        }
    }

    private void drawCenteredString(Graphics2D g, String text, Font font, int x, int y) {
        FontMetrics metrics = g.getFontMetrics(font);
        int textX = x - metrics.stringWidth(text) / 2;
        int textY = y - metrics.getHeight() / 2 + metrics.getAscent();
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.drawString(text, textX, textY);
    }

    private void draw() {
        Graphics g2 = getGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT) leftPressed = true;
        if (key == KeyEvent.VK_RIGHT) rightPressed = true;
        if (key == KeyEvent.VK_UP) upPressed = true;
        if (key == KeyEvent.VK_SPACE) spacePressed = true;

        if (key == KeyEvent.VK_R && gameOver) {
            resetGame();
        }

        if (key == KeyEvent.VK_SPACE && !gameStarted) {
            gameStarted = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT) leftPressed = false;
        if (key == KeyEvent.VK_RIGHT) rightPressed = false;
        if (key == KeyEvent.VK_UP) upPressed = false;
        if (key == KeyEvent.VK_SPACE) spacePressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void resetGame() {
        playerX = 100;
        playerY = GROUND_HEIGHT - 50;
        playerVelX = 0;
        playerVelY = 0;
        playerHealth = 100;
        score = 0;
        level = 1;
        gameOver = false;
        gameStarted = false;
        initializeLevel(level);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Extreme Cycling Game");
        ExtremeCyclingGame game = new ExtremeCyclingGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        game.startGame();
    }

    // Inner classes for game objects
    class Obstacle {
        int x, y, width, height;

        Obstacle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    class PowerUp {
        int x, y, width, height, type;

        PowerUp(int x, int y, int width, int height, int type) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.type = type;
        }
    }

    class BackgroundElement {
        int x, y, width, height, type;

        BackgroundElement(int x, int y, int width, int height, int type) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.type = type;
        }
    }
}