
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class SurfingAdventureGame extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SurfingAdventureGame());
    }

    public SurfingAdventureGame() {
        setTitle("Surfing Adventure Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        add(new GamePanel());
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PLAYER_WIDTH = 50;
    private static final int PLAYER_HEIGHT = 70;
    private static final int WAVE_HEIGHT = 50;
    private static final int OBSTACLE_SIZE = 30;
    private static final int COLLECTIBLE_SIZE = 20;
    private static final int FPS = 60;
    private static final double GRAVITY = 0.5;
    private static final double JUMP_FORCE = -12;

    // Game state
    private boolean isRunning;
    private boolean isGameOver;
    private int score;
    private int highScore;
    private double playerY;
    private double playerVelY;
    private int playerX;
    private ArrayList<Wave> waves;
    private ArrayList<Obstacle> obstacles;
    private ArrayList<Collectible> collectibles;
    private Random random;
    private Timer timer;
    private BufferedImage playerImage;
    private int playerFrame;
    private long lastFrameTime;
    private boolean isJumping;

    // Animation and graphics
    private static final int ANIMATION_FRAMES = 4;
    private static final long FRAME_DURATION = 150_000_000; // 150ms per frame
    private Color oceanColor = new Color(0, 105, 148);
    private Color skyColor = new Color(135, 206, 235);
    private Font scoreFont = new Font("Arial", Font.BOLD, 24);
    private Font gameOverFont = new Font("Arial", Font.BOLD, 48);

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        initGame();
    }

    private void initGame() {
        isRunning = true;
        isGameOver = false;
        score = 0;
        playerX = 100;
        playerY = HEIGHT - PLAYER_HEIGHT - WAVE_HEIGHT;
        playerVelY = 0;
        playerFrame = 0;
        isJumping = false;
        waves = new ArrayList<>();
        obstacles = new ArrayList<>();
        collectibles = new ArrayList<>();
        random = new Random();
        timer = new Timer(1000 / FPS, this);
        lastFrameTime = System.nanoTime();
        createPlayerImage();
        spawnInitialWaves();
        timer.start();
    }

    private void createPlayerImage() {
        playerImage = new BufferedImage(PLAYER_WIDTH * ANIMATION_FRAMES, PLAYER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = playerImage.createGraphics();
        for (int i = 0; i < ANIMATION_FRAMES; i++) {
            g.setColor(Color.RED);
            g.fillRect(i * PLAYER_WIDTH + 10, 10, 30, 50); // Surfboard
            g.setColor(Color.BLACK);
            g.fillOval(i * PLAYER_WIDTH + 15, 5, 20, 20); // Head
            g.setColor(Color.BLUE);
            g.fillRect(i * PLAYER_WIDTH + 10, 25, 30, 20); // Body
            // Slight variation for animation
            if (i % 2 == 0) {
                g.fillRect(i * PLAYER_WIDTH + 5, 45, 10, 15); // Left arm
                g.fillRect(i * PLAYER_WIDTH + 35, 45, 10, 15); // Right arm
            } else {
                g.fillRect(i * PLAYER_WIDTH + 5, 40, 10, 15); // Left arm
                g.fillRect(i * PLAYER_WIDTH + 35, 40, 10, 15); // Right arm
            }
        }
        g.dispose();
    }

    private void spawnInitialWaves() {
        for (int x = 0; x < WIDTH; x += 50) {
            waves.add(new Wave(x, HEIGHT - WAVE_HEIGHT));
        }
    }

    private void spawnObstacle() {
        if (random.nextInt(100) < 5 && obstacles.size() < 5) {
            obstacles.add(new Obstacle(WIDTH, HEIGHT - WAVE_HEIGHT - OBSTACLE_SIZE));
        }
    }

    private void spawnCollectible() {
        if (random.nextInt(100) < 3 && collectibles.size() < 3) {
            int y = HEIGHT - WAVE_HEIGHT - COLLECTIBLE_SIZE - random.nextInt(100);
            collectibles.add(new Collectible(WIDTH, y));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background
        g2d.setColor(skyColor);
        g2d.fillRect(0, 0, WIDTH, HEIGHT / 2);
        g2d.setColor(oceanColor);
        g2d.fillRect(0, HEIGHT / 2, WIDTH, HEIGHT / 2);

        // Draw waves
        for (Wave wave : waves) {
            wave.draw(g2d);
        }

        // Draw player
        g2d.drawImage(playerImage,
                playerX, (int) playerY,
                playerX + PLAYER_WIDTH, (int) playerY + PLAYER_HEIGHT,
                playerFrame * PLAYER_WIDTH, 0,
                (playerFrame + 1) * PLAYER_WIDTH, PLAYER_HEIGHT,
                null);

        // Draw obstacles
        for (Obstacle obstacle : obstacles) {
            obstacle.draw(g2d);
        }

        // Draw collectibles
        for (Collectible collectible : collectibles) {
            collectible.draw(g2d);
        }

        // Draw score
        g2d.setColor(Color.WHITE);
        g2d.setFont(scoreFont);
        g2d.drawString("Score: " + score, 10, 30);
        g2d.drawString("High Score: " + highScore, 10, 60);

        // Draw game over
        if (isGameOver) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.RED);
            g2d.setFont(gameOverFont);
            String message = "Game Over!";
            int messageWidth = g2d.getFontMetrics().stringWidth(message);
            g2d.drawString(message, (WIDTH - messageWidth) / 2, HEIGHT / 2 - 50);
            g2d.setFont(scoreFont);
            String scoreText = "Final Score: " + score;
            int scoreWidth = g2d.getFontMetrics().stringWidth(scoreText);
            g2d.drawString(scoreText, (WIDTH - scoreWidth) / 2, HEIGHT / 2);
            String restartText = "Press R to Restart";
            int restartWidth = g2d.getFontMetrics().stringWidth(restartText);
            g2d.drawString(restartText, (WIDTH - restartWidth) / 2, HEIGHT / 2 + 50);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isRunning && !isGameOver) {
            updateGame();
            updateAnimation();
            repaint();
        }
    }

    private void updateGame() {
        // Update player
        playerVelY += GRAVITY;
        playerY += playerVelY;
        if (playerY > HEIGHT - PLAYER_HEIGHT - WAVE_HEIGHT) {
            playerY = HEIGHT - PLAYER_HEIGHT - WAVE_HEIGHT;
            playerVelY = 0;
            isJumping = false;
        }

        // Update waves
        for (Wave wave : waves) {
            wave.update();
        }
        if (waves.get(0).x < -50) {
            waves.remove(0);
            waves.add(new Wave(WIDTH, HEIGHT - WAVE_HEIGHT));
        }

        // Update obstacles
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obstacle = obstacles.get(i);
            obstacle.update();
            if (obstacle.x < -OBSTACLE_SIZE) {
                obstacles.remove(i);
            } else if (checkCollision(obstacle)) {
                isGameOver = true;
                isRunning = false;
                highScore = Math.max(score, highScore);
            }
        }

        // Update collectibles
        for (int i = collectibles.size() - 1; i >= 0; i--) {
            Collectible collectible = collectibles.get(i);
            collectible.update();
            if (collectible.x < -COLLECTIBLE_SIZE) {
                collectibles.remove(i);
            } else if (checkCollection(collectible)) {
                collectibles.remove(i);
                score += 10;
            }
        }

        // Spawn new objects
        spawnObstacle();
        spawnCollectible();

        // Increment score
        score++;
    }

    private void updateAnimation() {
        long currentTime = System.nanoTime();
        if (currentTime - lastFrameTime >= FRAME_DURATION) {
            playerFrame = (playerFrame + 1) % ANIMATION_FRAMES;
            lastFrameTime = currentTime;
        }
    }

    private boolean checkCollision(Obstacle obstacle) {
        Rectangle playerRect = new Rectangle(playerX + 10, (int) playerY + 10,
                PLAYER_WIDTH - 20, PLAYER_HEIGHT - 20);
        Rectangle obstacleRect = new Rectangle(obstacle.x, obstacle.y,
                OBSTACLE_SIZE, OBSTACLE_SIZE);
        return playerRect.intersects(obstacleRect);
    }

    private boolean checkCollection(Collectible collectible) {
        Rectangle playerRect = new Rectangle(playerX + 10, (int) playerY + 10,
                PLAYER_WIDTH - 20, PLAYER_HEIGHT - 20);
        Rectangle collectibleRect = new Rectangle(collectible.x, collectible.y,
                COLLECTIBLE_SIZE, COLLECTIBLE_SIZE);
        return playerRect.intersects(collectibleRect);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_SPACE && !isJumping && !isGameOver) {
            playerVelY = JUMP_FORCE;
            isJumping = true;
        } else if (key == KeyEvent.VK_R && isGameOver) {
            initGame();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    // Wave class
    private class Wave {
        int x;
        int y;
        int speed = 5;
        Color waveColor = new Color(0, 150, 200);

        Wave(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void update() {
            x -= speed;
        }

        void draw(Graphics2D g) {
            g.setColor(waveColor);
            g.fillRect(x, y, 50, WAVE_HEIGHT);
            g.setColor(Color.WHITE);
            g.drawLine(x, y, x + 50, y);
        }
    }

    // Obstacle class
    private class Obstacle {
        int x;
        int y;
        int speed = 7;
        Color obstacleColor = Color.GRAY;

        Obstacle(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void update() {
            x -= speed;
        }

        void draw(Graphics2D g) {
            g.setColor(obstacleColor);
            g.fillRect(x, y, OBSTACLE_SIZE, OBSTACLE_SIZE);
        }
    }

    // Collectible class
    private class Collectible {
        int x;
        int y;
        int speed = 6;
        Color collectibleColor = Color.YELLOW;

        Collectible(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void update() {
            x -= speed;
        }

        void draw(Graphics2D g) {
            g.setColor(collectibleColor);
            g.fillOval(x, y, COLLECTIBLE_SIZE, COLLECTIBLE_SIZE);
        }
    }
}
