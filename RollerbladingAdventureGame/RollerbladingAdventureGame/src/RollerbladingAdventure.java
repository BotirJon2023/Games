import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class RollerbladingAdventure extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RollerbladingAdventure().setVisible(true));
    }

    public RollerbladingAdventure() {
        setTitle("Rollerblading Adventure Game");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        add(new GamePanel());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 60;
    private static final int GROUND_HEIGHT = 100;
    private static final int OBSTACLE_WIDTH = 30;
    private static final int OBSTACLE_HEIGHT = 50;
    private static final int COIN_SIZE = 20;
    private static final int MAX_JUMP_HEIGHT = 150;
    private static final int ANIMATION_FRAMES = 4;
    private static final int FRAME_DELAY = 100;

    private Timer timer;
    private Player player;
    private ArrayList<Obstacle> obstacles;
    private ArrayList<Coin> coins;
    private int score;
    private int highScore;
    private double backgroundX;
    private double groundX;
    private boolean gameOver;
    private boolean isJumping;
    private boolean isDucking;
    private Random random;
    private BufferedImage[] playerRunFrames;
    private BufferedImage[] playerJumpFrames;
    private BufferedImage[] playerDuckFrames;
    private BufferedImage coinImage;
    private BufferedImage obstacleImage;
    private BufferedImage backgroundImage;
    private BufferedImage groundImage;
    private int frameCounter;
    private int currentFrame;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.CYAN);
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(16, this); // ~60 FPS
        player = new Player(100, HEIGHT - GROUND_HEIGHT - PLAYER_HEIGHT);
        obstacles = new ArrayList<>();
        coins = new ArrayList<>();
        score = 0;
        highScore = 0;
        backgroundX = 0;
        groundX = 0;
        gameOver = false;
        isJumping = false;
        isDucking = false;
        random = new Random();
        frameCounter = 0;
        currentFrame = 0;

        // Initialize images (simulated with colored rectangles for simplicity)
        playerRunFrames = new BufferedImage[ANIMATION_FRAMES];
        playerJumpFrames = new BufferedImage[ANIMATION_FRAMES];
        playerDuckFrames = new BufferedImage[ANIMATION_FRAMES];
        initializeImages();

        timer.start();
    }

    private void initializeImages() {
        // Simulate player running animation frames
        for (int i = 0; i < ANIMATION_FRAMES; i++) {
            playerRunFrames[i] = new BufferedImage(PLAYER_WIDTH, PLAYER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = playerRunFrames[i].createGraphics();
            g2d.setColor(i % 2 == 0 ? Color.BLUE : Color.DARK_GRAY);
            g2d.fillRect(0, 0, PLAYER_WIDTH, PLAYER_HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Run" + (i + 1), 5, 20);
            g2d.dispose();
        }

        // Simulate player jumping animation frames
        for (int i = 0; i < ANIMATION_FRAMES; i++) {
            playerJumpFrames[i] = new BufferedImage(PLAYER_WIDTH, PLAYER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = playerJumpFrames[i].createGraphics();
            g2d.setColor(i % 2 == 0 ? Color.GREEN : Color.DARK_GREEN);
            g2d.fillRect(0, 0, PLAYER_WIDTH, PLAYER_HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Jump" + (i + 1), 5, 20);
            g2d.dispose();
        }

        // Simulate player ducking animation frames
        for (int i = 0; i < ANIMATION_FRAMES; i++) {
            playerDuckFrames[i] = new BufferedImage(PLAYER_WIDTH, PLAYER_HEIGHT / 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = playerDuckFrames[i].createGraphics();
            g2d.setColor(i % 2 == 0 ? Color.RED : Color.darkGray);
            g2d.fillRect(0, 0, PLAYER_WIDTH, PLAYER_HEIGHT / 2);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Duck" + (i + 1), 5, 20);
            g2d.dispose();
        }

        // Simulate coin image
        coinImage = new BufferedImage(COIN_SIZE, COIN_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dCoin = coinImage.createGraphics();
        g2dCoin.setColor(Color.YELLOW);
        g2dCoin.fillOval(0, 0, COIN_SIZE, COIN_SIZE);
        g2dCoin.setColor(Color.BLACK);
        g2dCoin.drawString("$", 5, 15);
        g2dCoin.dispose();

        // Simulate obstacle image
        obstacleImage = new BufferedImage(OBSTACLE_WIDTH, OBSTACLE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dObstacle = obstacleImage.createGraphics();
        g2dObstacle.setColor(Color.GRAY);
        g2dObstacle.fillRect(0, 0, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
        g2dObstacle.setColor(Color.BLACK);
        g2dObstacle.drawString("X", 10, 30);
        g2dObstacle.dispose();

        // Simulate background and ground images
        backgroundImage = new BufferedImage(WIDTH * 2, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dBg = backgroundImage.createGraphics();
        g2dBg.setColor(Color.CYAN);
        g2dBg.fillRect(0, 0, WIDTH * 2, HEIGHT);
        g2dBg.setColor(Color.WHITE);
        for (int i = 0; i < WIDTH * 2; i += 50) {
            g2dBg.fillOval(i, 50, 30, 30); // Clouds
        }
        g2dBg.dispose();

        groundImage = new BufferedImage(WIDTH * 2, GROUND_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dGround = groundImage.createGraphics();
        g2dGround.setColor(Color.GREEN.darker());
        g2dGround.fillRect(0, 0, WIDTH * 2, GROUND_HEIGHT);
        g2dGround.setColor(Brown);
        for (int i = 0; i < WIDTH * 2; i += 20) {
            g2dGround.fillRect(i, GROUND_HEIGHT - 10, 10, 10); // Grass patches
        }
        g2dGround.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background
        g2d.drawImage(backgroundImage, (int) backgroundX, 0, null);
        g2d.drawImage(backgroundImage, (int) backgroundX + WIDTH, 0, null);

        // Draw ground
        g2d.drawImage(groundImage, (int) groundX, HEIGHT - GROUND_HEIGHT, null);
        g2d.drawImage(groundImage, (int) groundX + WIDTH, HEIGHT - GROUND_HEIGHT, null);

        // Draw player
        BufferedImage playerImage;
        int playerHeight = PLAYER_HEIGHT;
        if (isDucking) {
            playerImage = playerDuckFrames[currentFrame];
            playerHeight = PLAYER_HEIGHT / 2;
        } else if (isJumping) {
            playerImage = playerJumpFrames[currentFrame];
        } else {
            playerImage = playerRunFrames[currentFrame];
        }
        g2d.drawImage(playerImage, player.x, player.y, PLAYER_WIDTH, playerHeight, null);

        // Draw obstacles
        for (Obstacle obstacle : obstacles) {
            g2d.drawImage(obstacleImage, obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT, null);
        }

        // Draw coins
        for (Coin coin : coins) {
            g2d.drawImage(coinImage, coin.x, coin.y, COIN_SIZE, COIN_SIZE, null);
        }

        // Draw score and high score
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Score: " + score, 10, 30);
        g2d.drawString("High Score: " + highScore, 10, 60);

        // Draw game over screen
        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            g2d.drawString("Game Over", WIDTH / 2 - 100, HEIGHT / 2 - 20);
            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            g2d.drawString("Press R to Restart", WIDTH / 2 - 80, HEIGHT / 2 + 20);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            updateGame();
            spawnObstaclesAndCoins();
            checkCollisions();
            updateAnimation();
        }
        repaint();
    }

    private void updateGame() {
        // Update background and ground scrolling
        backgroundX -= 1;
        groundX -= 3;
        if (backgroundX <= -WIDTH) backgroundX += WIDTH;
        if (groundX <= -WIDTH) groundX += WIDTH;

        // Update player
        player.update();

        // Update obstacles
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obstacle = obstacles.get(i);
            obstacle.x -= 5;
            if (obstacle.x + OBSTACLE_WIDTH < 0) {
                obstacles.remove(i);
                score += 10;
            }
        }

        // Update coins
        for (int i = coins.size() - 1; i >= 0; i--) {
            Coin coin = coins.get(i);
            coin.x -= 5;
            if (coin.x + COIN_SIZE < 0) {
                coins.remove(i);
            }
        }

        // Update high score
        if (score > highScore) {
            highScore = score;
        }
    }

    private void spawnObstaclesAndCoins() {
        if (random.nextInt(100) < 2) {
            obstacles.add(new Obstacle(WIDTH, HEIGHT - GROUND_HEIGHT - OBSTACLE_HEIGHT));
        }
        if (random.nextInt(100) < 3) {
            int coinY = HEIGHT - GROUND_HEIGHT - COIN_SIZE - random.nextInt(100);
            coins.add(new Coin(WIDTH, coinY));
        }
    }

    private void checkCollisions() {
        Rectangle playerRect = new Rectangle(player.x, player.y, PLAYER_WIDTH, isDucking ? PLAYER_HEIGHT / 2 : PLAYER_HEIGHT);
        for (Obstacle obstacle : obstacles) {
            Rectangle obstacleRect = new Rectangle(obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
            if (playerRect.intersects(obstacleRect)) {
                gameOver = true;
                timer.stop();
            }
        }
        for (int i = coins.size() - 1; i >= 0; i--) {
            Coin coin = coins.get(i);
            Rectangle coinRect = new Rectangle(coin.x, coin.y, COIN_SIZE, COIN_SIZE);
            if (playerRect.intersects(coinRect)) {
                coins.remove(i);
                score += 50;
            }
        }
    }

    private void updateAnimation() {
        frameCounter++;
        if (frameCounter >= FRAME_DELAY / 16) {
            currentFrame = (currentFrame + 1) % ANIMATION_FRAMES;
            frameCounter = 0;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_SPACE && !isJumping && !gameOver) {
            player.jump();
            isJumping = true;
        } else if (key == KeyEvent.VK_DOWN && !isJumping && !gameOver) {
            isDucking = true;
        } else if (key == KeyEvent.VK_R && gameOver) {
            restartGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            isDucking = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void restartGame() {
        player = new Player(100, HEIGHT - GROUND_HEIGHT - PLAYER_HEIGHT);
        obstacles.clear();
        coins.clear();
        score = 0;
        backgroundX = 0;
        groundX = 0;
        gameOver = false;
        isJumping = false;
        isDucking = false;
        timer.start();
    }

    class Player {
        int x, y;
        double velocityY;
        double gravity = 0.5;
        int jumpStrength = -12;

        Player(int x, int y) {
            this.x = x;
            this.y = y;
            this.velocityY = 0;
        }

        void jump() {
            velocityY = jumpStrength;
        }

        void update() {
            if (isJumping) {
                y += velocityY;
                velocityY += gravity;
                if (y >= HEIGHT - GROUND_HEIGHT - PLAYER_HEIGHT) {
                    y = HEIGHT - GROUND_HEIGHT - PLAYER_HEIGHT;
                    velocityY = 0;
                    isJumping = false;
                }
            }
        }
    }

    class Obstacle {
        int x, y;

        Obstacle(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    class Coin {
        int x, y;

        Coin(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private class Brown {
    }
}