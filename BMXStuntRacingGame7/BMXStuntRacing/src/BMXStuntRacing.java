import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class BMXStuntRacing extends JPanel implements Runnable, KeyListener {
    // Game constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int GROUND_Y = 500;
    private static final int FRAME_RATE = 60;

    // Game state
    private Thread gameThread;
    private boolean running = false;
    private boolean[] keys = new boolean[256];

    // Player state
    private int playerX = 100;
    private int playerY = GROUND_Y;
    private double velocityY = 0;
    private boolean onGround = true;
    private boolean doingStunt = false;
    private int stuntScore = 0;
    private int totalScore = 0;

    // Animation
    private BufferedImage[] bikeFrames;
    private int currentFrame = 0;
    private long lastFrameTime = 0;
    private long frameDelay = 100;

    // Terrain
    private int[] terrainHeights = new int[WIDTH];
    private Random rand = new Random();

    // Constructor
    public BMXStuntRacing() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.CYAN);
        addKeyListener(this);
        setFocusable(true);
        generateTerrain();
        loadBikeFrames();
    }

    // Load bike animation frames (placeholder)
    private void loadBikeFrames() {
        bikeFrames = new BufferedImage[4];
        for (int i = 0; i < 4; i++) {
            bikeFrames[i] = new BufferedImage(50, 30, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bikeFrames[i].createGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(10, 10, 30, 10);
            g.setColor(Color.RED);
            g.drawString("Bike" + i, 5, 25);
            g.dispose();
        }
    }

    // Generate terrain
    private void generateTerrain() {
        for (int i = 0; i < WIDTH; i++) {
            terrainHeights[i] = GROUND_Y - rand.nextInt(20);
        }
    }

    // Start game loop
    public void startGame() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    // Game loop
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerFrame = 1_000_000_000.0 / FRAME_RATE;

        while (running) {
            long now = System.nanoTime();
            if ((now - lastTime) >= nsPerFrame) {
                updateGame();
                repaint();
                lastTime = now;
            }
        }
    }

    // Update game state
    private void updateGame() {
        handleInput();
        applyPhysics();
        updateAnimation();
        checkStunt();
    }

    // Handle keyboard input
    private void handleInput() {
        if (keys[KeyEvent.VK_SPACE] && onGround) {
            velocityY = -12;
            onGround = false;
        }
        if (keys[KeyEvent.VK_S]) {
            doingStunt = true;
        } else {
            if (doingStunt) {
                totalScore += stuntScore;
                stuntScore = 0;
            }
            doingStunt = false;
        }
    }

    // Apply gravity and movement
    private void applyPhysics() {
        if (!onGround) {
            velocityY += 0.6;
            playerY += velocityY;
            if (playerY >= terrainHeights[playerX]) {
                playerY = terrainHeights[playerX];
                velocityY = 0;
                onGround = true;
            }
        }
    }

    // Update animation frame
    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > frameDelay) {
            currentFrame = (currentFrame + 1) % bikeFrames.length;
            lastFrameTime = currentTime;
        }
    }

    // Check stunt scoring
    private void checkStunt() {
        if (doingStunt && !onGround) {
            stuntScore++;
        }
    }

    // Draw game
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBackground(g);
        drawTerrain(g);
        drawPlayer(g);
        drawHUD(g);
    }

    // Draw background
    private void drawBackground(Graphics g) {
        g.setColor(new Color(135, 206, 235));
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    // Draw terrain
    private void drawTerrain(Graphics g) {
        g.setColor(Color.GREEN);
        for (int i = 0; i < WIDTH - 1; i++) {
            g.drawLine(i, terrainHeights[i], i + 1, terrainHeights[i + 1]);
        }
    }

    // Draw player
    private void drawPlayer(Graphics g) {
        g.drawImage(bikeFrames[currentFrame], playerX, playerY - 30, null);
        if (doingStunt) {
            g.setColor(Color.ORANGE);
            g.drawString("Stunt!", playerX, playerY - 40);
        }
    }

    // Draw HUD
    private void drawHUD(Graphics g) {
        g.setColor(Color.BLACK);
        g.drawString("Total Score: " + totalScore, 10, 20);
        g.drawString("Current Stunt Score: " + stuntScore, 10, 40);
        g.drawString("Press SPACE to jump, S to stunt", 10, 60);
    }

    // Key events
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
    }

    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    public void keyTyped(KeyEvent e) {}

    // Main method
    public static void main(String[] args) {
        JFrame frame = new JFrame("BMX Stunt Racing");
        BMXStuntRacing game = new BMXStuntRacing();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game);
        frame.pack();
        frame.setVisible(true);
        game.startGame();
    }
}
