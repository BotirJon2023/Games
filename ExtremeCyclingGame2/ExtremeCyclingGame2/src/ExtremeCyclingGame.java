import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

// --- Main Game Frame ---
public class ExtremeCyclingGame extends JFrame {

    private GamePanel gamePanel;

    public ExtremeCyclingGame() {
        setTitle("Extreme Cycling Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);
        pack(); // Sizes the frame so that all its contents are at or above their preferred sizes
        setLocationRelativeTo(null); // Center the window
    }

    public static void main(String[] args) {
        // Run the game on the Event Dispatch Thread for Swing safety
        SwingUtilities.invokeLater(() -> {
            ExtremeCyclingGame game = new ExtremeCyclingGame();
            game.setVisible(true);
            game.gamePanel.startGame();
        });
    }
}

// --- Game Panel where all drawing and game logic happens ---
class GamePanel extends JPanel implements Runnable, KeyListener {

    // Game Dimensions
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    // Game States
    public enum GameState {
        MENU, PLAYING, GAME_OVER
    }
    private GameState currentGameState = GameState.MENU;

    // Game Loop Variables
    private Thread gameThread;
    private boolean running;
    private long lastTime;
    private double nsPerTick = 1_000_000_000.0 / 60.0; // 60 updates per second

    // Game Entities
    private Cyclist player;
    private ArrayList<Obstacle> obstacles;
    private Background background;

    // Animation variables
    private long lastAnimationUpdateTime;
    private final long animationFrameDelay = 100; // milliseconds per animation frame

    // Images
    private BufferedImage[] cyclistRunFrames;
    private BufferedImage[] cyclistJumpFrames;
    private BufferedImage obstacleImage;
    private BufferedImage backgroundImage;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        loadGameAssets();
        initGame();
    }

    private void loadGameAssets() {
        try {
            // Placeholder images - you'll replace these with actual sprite sheets/images
            // For a real game, you'd have multiple frames for different animations (run, jump, crash)
            cyclistRunFrames = new BufferedImage[4]; // Example: 4 frames for running
            cyclistRunFrames[0] = ImageIO.read(getClass().getResource("/images/cyclist_run_0.png"));
            cyclistRunFrames[1] = ImageIO.read(getClass().getResource("/images/cyclist_run_1.png"));
            cyclistRunFrames[2] = ImageIO.read(getClass().getResource("/images/cyclist_run_2.png"));
            cyclistRunFrames[3] = ImageIO.read(getClass().getResource("/images/cyclist_run_3.png"));

            cyclistJumpFrames = new BufferedImage[1]; // Example: 1 frame for jumping
            cyclistJumpFrames[0] = ImageIO.read(getClass().getResource("/images/cyclist_jump_0.png"));


            obstacleImage = ImageIO.read(getClass().getResource("/images/obstacle.png"));
            backgroundImage = ImageIO.read(getClass().getResource("/images/background.png"));

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading game assets. Make sure your /images/ folder and files exist.");
            // Provide dummy images or exit gracefully if assets are critical
            cyclistRunFrames = new BufferedImage[1];
            cyclistRunFrames[0] = createDummyImage(50, 50, Color.BLUE);
            cyclistJumpFrames = new BufferedImage[1];
            cyclistJumpFrames[0] = createDummyImage(50, 50, Color.CYAN);
            obstacleImage = createDummyImage(40, 40, Color.RED);
            backgroundImage = createDummyImage(WIDTH, HEIGHT, Color.LIGHT_GRAY);
        }
    }

    private BufferedImage createDummyImage(int width, int height, Color color) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        return img;
    }

    private void initGame() {
        player = new Cyclist(100, HEIGHT - 100, 50, 50, cyclistRunFrames, cyclistJumpFrames); // x, y, width, height, sprites
        obstacles = new ArrayList<>();
        background = new Background(backgroundImage, 0, 0, WIDTH, HEIGHT);
        // Add initial obstacles (you'd have more complex spawning logic)
        obstacles.add(new Obstacle(WIDTH + 100, HEIGHT - 80, 40, 40, obstacleImage));
        obstacles.add(new Obstacle(WIDTH + 400, HEIGHT - 120, 60, 60, obstacleImage));
    }

    public void startGame() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void stopGame() {
        running = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        lastTime = System.nanoTime();
        lastAnimationUpdateTime = System.currentTimeMillis();
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            // Update game state at a fixed rate (60 FPS)
            while (delta >= 1) {
                updateGame();
                delta--;
            }

            // Render as fast as possible (but tied to updates for consistency)
            repaint(); // Calls paintComponent

            // Throttle rendering slightly to prevent 100% CPU usage if not capped
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateGame() {
        if (currentGameState == GameState.PLAYING) {
            player.update();
            background.update(); // Scroll background

            // Update obstacles and check for collisions
            for (int i = 0; i < obstacles.size(); i++) {
                Obstacle obs = obstacles.get(i);
                obs.update();

                // Simple collision detection (Bounding Box)
                if (player.getBounds().intersects(obs.getBounds())) {
                    System.out.println("Collision detected!");
                    // Handle collision (e.g., reduce health, game over)
                    currentGameState = GameState.GAME_OVER;
                }

                // Remove off-screen obstacles and add new ones
                if (obs.getX() + obs.getWidth() < 0) {
                    obstacles.remove(i);
                    i--; // Adjust index after removal
                    // Add a new obstacle randomly
                    obstacles.add(new Obstacle(WIDTH + (int)(Math.random() * 300) + 100,
                            HEIGHT - (80 + (int)(Math.random() * 50)), // Vary height slightly
                            40 + (int)(Math.random() * 30),
                            40 + (int)(Math.random() * 30),
                            obstacleImage));
                }
            }

            // Update player animation frame
            if (System.currentTimeMillis() - lastAnimationUpdateTime > animationFrameDelay) {
                player.nextAnimationFrame();
                lastAnimationUpdateTime = System.currentTimeMillis();
            }

        } else if (currentGameState == GameState.MENU) {
            // Logic for menu screen
        } else if (currentGameState == GameState.GAME_OVER) {
            // Logic for game over screen
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Render based on game state
        if (currentGameState == GameState.PLAYING) {
            background.draw(g2d);
            player.draw(g2d);
            for (Obstacle obs : obstacles) {
                obs.draw(g2d);
            }
            // Draw score, etc.
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Score: 0", 10, 20); // Placeholder score

        } else if (currentGameState == GameState.MENU) {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            String title = "Extreme Cycling Game";
            String startMsg = "Press ENTER to Start";
            int titleWidth = g2d.getFontMetrics().stringWidth(title);
            int startMsgWidth = g2d.getFontMetrics().stringWidth(startMsg);
            g2d.drawString(title, (WIDTH - titleWidth) / 2, HEIGHT / 2 - 50);
            g2d.drawString(startMsg, (WIDTH - startMsgWidth) / 2, HEIGHT / 2 + 50);

        } else if (currentGameState == GameState.GAME_OVER) {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            String gameOverMsg = "GAME OVER!";
            String restartMsg = "Press R to Restart";
            int gameOverWidth = g2d.getFontMetrics().stringWidth(gameOverMsg);
            int restartMsgWidth = g2d.getFontMetrics().stringWidth(restartMsg);
            g2d.drawString(gameOverMsg, (WIDTH - gameOverWidth) / 2, HEIGHT / 2 - 50);
            g2d.drawString(restartMsg, (WIDTH - restartMsgWidth) / 2, HEIGHT / 2 + 50);
        }

        Toolkit.getDefaultToolkit().sync(); // Ensures all graphics operations are finished
    }

    // --- KeyListener Implementations ---
    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (currentGameState == GameState.PLAYING) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_UP) {
                player.jump();
            }
            // Add more controls (e.g., for stunts, changing speed)
            // if (e.getKeyCode() == KeyEvent.VK_LEFT) { player.moveLeft(); }
            // if (e.getKeyCode() == KeyEvent.VK_RIGHT) { player.moveRight(); }
        } else if (currentGameState == GameState.MENU) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                currentGameState = GameState.PLAYING;
            }
        } else if (currentGameState == GameState.GAME_OVER) {
            if (e.getKeyCode() == KeyEvent.VK_R) {
                initGame(); // Reset game state
                currentGameState = GameState.PLAYING;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Not used in this basic example, but useful for continuous movement
    }
}

// --- Player Class (Cyclist) ---
class Cyclist {
    private int x, y, width, height;
    private int yVelocity = 0;
    private boolean isJumping = false;
    private final int JUMP_STRENGTH = 15;
    private final int GRAVITY = 1; // Pixels per frame
    private int groundY; // The Y coordinate where the cyclist lands

    private BufferedImage[] runFrames;
    private BufferedImage[] jumpFrames;
    private int currentFrameIndex = 0;

    public Cyclist(int x, int y, int width, int height, BufferedImage[] runFrames, BufferedImage[] jumpFrames) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.groundY = y; // Initial ground level
        this.runFrames = runFrames;
        this.jumpFrames = jumpFrames;
    }

    public void update() {
        if (isJumping) {
            y += yVelocity;
            yVelocity += GRAVITY; // Apply gravity

            if (y >= groundY) { // Landed on ground
                y = groundY;
                isJumping = false;
                yVelocity = 0;
            }
        }
        // No horizontal movement in this simple example as game scrolls
    }

    public void jump() {
        if (!isJumping) {
            isJumping = true;
            yVelocity = -JUMP_STRENGTH; // Move upwards
            currentFrameIndex = 0; // Reset animation for jump
        }
    }

    public void nextAnimationFrame() {
        if (!isJumping) {
            currentFrameIndex = (currentFrameIndex + 1) % runFrames.length;
        } else {
            // If jumping, we might only have one jump frame or a simple sequence
            // For now, it sticks to the first jump frame
        }
    }

    public void draw(Graphics2D g2d) {
        BufferedImage currentFrame;
        if (isJumping && jumpFrames != null && jumpFrames.length > 0) {
            currentFrame = jumpFrames[0]; // Or cycle through jumpFrames if multiple
        } else {
            currentFrame = runFrames[currentFrameIndex];
        }
        g2d.drawImage(currentFrame, x, y, width, height, null);
        // Optional: draw bounding box for debugging
        // g2d.setColor(Color.GREEN);
        // g2d.drawRect(getBounds().x, getBounds().y, getBounds().width, getBounds().height);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public int getX() { return x; }
    public int getY() { return y; }
}

// --- Obstacle Class ---
class Obstacle {
    private int x, y, width, height;
    private final int SPEED = 5; // How fast obstacles move towards the player
    private BufferedImage image;

    public Obstacle(int x, int y, int width, int height, BufferedImage image) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.image = image;
    }

    public void update() {
        x -= SPEED; // Move left
    }

    public void draw(Graphics2D g2d) {
        g2d.drawImage(image, x, y, width, height, null);
        // Optional: draw bounding box for debugging
        // g2d.setColor(Color.RED);
        // g2d.drawRect(getBounds().x, getBounds().y, getBounds().width, getBounds().height);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public int getX() { return x; }
    public int getWidth() { return width; }
}

// --- Background Class for Parallax Scrolling ---
class Background {
    private BufferedImage image;
    private int x1, y1; // Position of the first background image
    private int x2, y2; // Position of the second background image for continuous scrolling
    private int width, height;
    private final int SCROLL_SPEED = 3; // Should be less than obstacle speed for parallax effect

    public Background(BufferedImage image, int x, int y, int width, int height) {
        this.image = image;
        this.x1 = x;
        this.y1 = y;
        this.x2 = x + width; // Second image starts where the first one ends
        this.width = width;
        this.height = height;
    }

    public void update() {
        x1 -= SCROLL_SPEED;
        x2 -= SCROLL_SPEED;

        // If the first image goes off-screen, reset its position to follow the second
        if (x1 + width < 0) {
            x1 = x2 + width;
        }
        // If the second image goes off-screen, reset its position to follow the first
        if (x2 + width < 0) {
            x2 = x1 + width;
        }
    }

    public void draw(Graphics2D g2d) {
        g2d.drawImage(image, x1, y1, width, height, null);
        g2d.drawImage(image, x2, y2, width, height, null);
    }
}