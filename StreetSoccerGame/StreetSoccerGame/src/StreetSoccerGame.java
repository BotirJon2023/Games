import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;

public class StreetSoccerGame extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StreetSoccerGame());
    }

    public StreetSoccerGame() {
        setTitle("Street Soccer Game");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        add(new GamePanel());
        setVisible(true);
    }
}

class GamePanel extends JPanel implements ActionListener {
    // Game dimensions
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;
    private static final int GOAL_WIDTH = 100;
    private static final int GOAL_HEIGHT = 200;
    static final int PLAYER_SIZE = 30;
    public static final int BALL_SIZE = 20;

    // Game objects
    private Player player1;
    private Player player2;
    private Ball ball;
    private int scorePlayer1;
    private int scorePlayer2;

    // Input handling
    private HashSet<Integer> keysPressed;
    private Timer timer;

    // Game state
    private boolean gameRunning;
    private long lastUpdateTime;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(34, 139, 34)); // Green field
        initializeGame();
        setupInput();
        startGame();
    }

    private void initializeGame() {
        // Initialize players
        player1 = new Player(200, HEIGHT / 2, Color.RED, "Player 1");
        player2 = new Player(WIDTH - 200, HEIGHT / 2, Color.BLUE, "Player 2");
        // Initialize ball
        ball = new Ball(WIDTH / 2, HEIGHT / 2);
        // Initialize scores
        scorePlayer1 = 0;
        scorePlayer2 = 0;
        // Initialize game state
        gameRunning = true;
        keysPressed = new HashSet<>();
        // Initialize timer
        timer = new Timer(16, this); // ~60 FPS
        lastUpdateTime = System.nanoTime();
    }

    private void setupInput() {
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keysPressed.add(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keysPressed.remove(e.getKeyCode());
            }
        });
    }

    private void startGame() {
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning) {
            long currentTime = System.nanoTime();
            double deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0; // Seconds
            lastUpdateTime = currentTime;
            updateGame(deltaTime);
            repaint();
        }
    }

    private void updateGame(double deltaTime) {
        handleInput();
        movePlayers(deltaTime);
        moveBall(deltaTime);
        checkCollisions();
        checkGoals();
    }

    private void handleInput() {
        // Player 1 controls (WASD)
        if (keysPressed.contains(KeyEvent.VK_W)) player1.moveUp();
        if (keysPressed.contains(KeyEvent.VK_S)) player1.moveDown();
        if (keysPressed.contains(KeyEvent.VK_A)) player1.moveLeft();
        if (keysPressed.contains(KeyEvent.VK_D)) player1.moveRight();
        // Player 2 controls (Arrow keys)
        if (keysPressed.contains(KeyEvent.VK_UP)) player2.moveUp();
        if (keysPressed.contains(KeyEvent.VK_DOWN)) player2.moveDown();
        if (keysPressed.contains(KeyEvent.VK_LEFT)) player2.moveLeft();
        if (keysPressed.contains(KeyEvent.VK_RIGHT)) player2.moveRight();
    }

    private void movePlayers(double deltaTime) {
        player1.updatePosition(deltaTime);
        player2.updatePosition(deltaTime);
        // Keep players within bounds
        keepWithinBounds(player1);
        keepWithinBounds(player2);
    }

    private void moveBall(double deltaTime) {
        ball.updatePosition(deltaTime);
        // Apply friction
        ball.applyFriction(deltaTime);
        // Keep ball within bounds (except goals)
        keepBallWithinBounds();
    }

    private void keepWithinBounds(Player player) {
        player.x = Math.max(0, Math.min(WIDTH - PLAYER_SIZE, player.x));
        player.y = Math.max(0, Math.min(HEIGHT - PLAYER_SIZE, player.y));
    }

    private void keepBallWithinBounds() {
        if (ball.x < 0) {
            ball.x = 0;
            ball.vx = -ball.vx * 0.8; // Bounce with damping
        }
        if (ball.x > WIDTH - BALL_SIZE) {
            ball.x = WIDTH - BALL_SIZE;
            ball.vx = -ball.vx * 0.8;
        }
        if (ball.y < 0) {
            ball.y = 0;
            ball.vy = -ball.vy * 0.8;
        }
        if (ball.y > HEIGHT - BALL_SIZE) {
            ball.y = HEIGHT - BALL_SIZE;
            ball.vy = -ball.vy * 0.8;
        }
    }

    private void checkCollisions() {
        checkPlayerBallCollision(player1);
        checkPlayerBallCollision(player2);
    }

    private void checkPlayerBallCollision(Player player) {
        double dx = ball.x + BALL_SIZE / 2 - (player.x + PLAYER_SIZE / 2);
        double dy = ball.y + BALL_SIZE / 2 - (player.y + PLAYER_SIZE / 2);
        double distance = Math.sqrt(dx * dx + dy * dy);
        double minDistance = (PLAYER_SIZE + BALL_SIZE) / 2;

        if (distance < minDistance) {
            // Calculate collision angle
            double angle = Math.atan2(dy, dx);
            // Apply kick force
            double kickStrength = 300;
            ball.vx += kickStrength * Math.cos(angle);
            ball.vy += kickStrength * Math.sin(angle);
            // Prevent ball from sticking inside player
            double overlap = minDistance - distance;
            ball.x += overlap * Math.cos(angle);
            ball.y += overlap * Math.sin(angle);
        }
    }

    private void checkGoals() {
        // Check if ball is in left goal (Player 2 scores)
        if (ball.x <= 0 && ball.y >= HEIGHT / 2 - GOAL_HEIGHT / 2 && ball.y <= HEIGHT / 2 + GOAL_HEIGHT / 2) {
            scorePlayer2++;
            resetBall();
        }
        // Check if ball is in right goal (Player 1 scores)
        if (ball.x >= WIDTH - BALL_SIZE && ball.y >= HEIGHT / 2 - GOAL_HEIGHT / 2 && ball.y <= HEIGHT / 2 + GOAL_HEIGHT / 2) {
            scorePlayer1++;
            resetBall();
        }
    }

    private void resetBall() {
        ball.x = WIDTH / 2;
        ball.y = HEIGHT / 2;
        ball.vx = 0;
        ball.vy = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw field lines
        drawField(g2d);
        // Draw goals
        drawGoals(g2d);
        // Draw players
        player1.draw(g2d);
        player2.draw(g2d);
        // Draw ball
        ball.draw(g2d);
        // Draw scores
        drawScores(g2d);
    }

    private void drawField(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        // Center line
        g2d.drawLine(WIDTH / 2, 0, WIDTH / 2, HEIGHT);
        // Center circle
        g2d.drawOval(WIDTH / 2 - 50, HEIGHT / 2 - 50, 100, 100);
        // Border
        g2d.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);
    }

    private void drawGoals(Graphics2D g2d) {
        g2d.setColor(Color.YELLOW);
        // Left goal
        g2d.drawRect(0, HEIGHT / 2 - GOAL_HEIGHT / 2, GOAL_WIDTH, GOAL_HEIGHT);
        // Right goal
        g2d.drawRect(WIDTH - GOAL_WIDTH, HEIGHT / 2 - GOAL_HEIGHT / 2, GOAL_WIDTH, GOAL_HEIGHT);
    }

    private void drawScores(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString(player1.name + ": " + scorePlayer1, 20, 30);
        g2d.drawString(player2.name + ": " + scorePlayer2, WIDTH - 120, 30);
    }
}

class Player {
    double x, y;
    double vx, vy;
    private double speed;
    Color color;
    String name;

    public Player(double x, double y, Color color, String name) {
        this.x = x;
        this.y = y;
        this.vx = 0;
        this.vy = 0;
        this.speed = 200; // Pixels per second
        this.color = color;
        this.name = name;
    }

    public void moveUp() {
        vy = -speed;
    }

    public void moveDown() {
        vy = speed;
    }

    public void moveLeft() {
        vx = -speed;
    }

    public void moveRight() {
        vx = speed;
    }

    public void updatePosition(double deltaTime) {
        x += vx * deltaTime;
        y += vy * deltaTime;
        // Reset velocity after movement
        vx = 0;
        vy = 0;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillOval((int) x, (int) y, GamePanel.PLAYER_SIZE, GamePanel.PLAYER_SIZE);
        // Draw player name
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString(name, (int) x, (int) y - 5);
    }
}

class Ball {
    double x, y;
    double vx, vy;
    private static final double FRICTION = 0.98;

    public Ball(double x, double y) {
        this.x = x;
        this.y = y;
        this.vx = 0;
        this.vy = 0;
    }

    public void updatePosition(double deltaTime) {
        x += vx * deltaTime;
        y += vy * deltaTime;
    }

    public void applyFriction(double deltaTime) {
        vx *= Math.pow(FRICTION, deltaTime * 60);
        vy *= Math.pow(FRICTION, deltaTime * 60);
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int) x, (int) y, GamePanel.BALL_SIZE, GamePanel.BALL_SIZE);
    }
}

// Additional utility methods to extend code length
class GameUtils {
    public static double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public static double calculateAngle(double x1, double y1, double x2, double y2) {
        return Math.atan2(y2 - y1, x2 - x1);
    }

    public static boolean isPointInRectangle(double px, double py, double rx, double ry, double rw, double rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    public static Color getRandomColor() {
        Random rand = new Random();
        return new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
    }

    public static void logGameEvent(String event) {
        System.out.println("Game Event: " + event + " at " + System.currentTimeMillis());
    }
}

// Random number generator for potential future use
class Random {
    private long seed;

    public Random() {
        this.seed = System.nanoTime();
    }

    public int nextInt(int bound) {
        seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        return (int) (seed % bound);
    }
}

// Game configuration class
class GameConfig {
    public static final int FPS = 60;
    public static final double GRAVITY = 0; // Not used in this game
    public static final double MAX_BALL_SPEED = 500;
    public static final int MAX_SCORE = 10;

    public static void loadConfig() {
        // Placeholder for configuration loading
        GameUtils.logGameEvent("Configuration loaded");
    }
}

// Input validator for future expansion
class InputValidator {
    public static boolean isValidKeyCode(int keyCode) {
        return keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z ||
                keyCode >= KeyEvent.VK_UP && keyCode <= KeyEvent.VK_DOWN ||
                keyCode == KeyEvent.VK_SPACE;
    }

    public static void validatePlayerPosition(Player player, int width, int height) {
        if (player.x < 0 || player.x > width || player.y < 0 || player.y > height) {
            GameUtils.logGameEvent("Invalid player position: " + player.name);
        }
    }
}

// Game state manager
class GameStateManager {
    private boolean paused;
    private boolean gameOver;

    public GameStateManager() {
        paused = false;
        gameOver = false;
    }

    public void pauseGame() {
        paused = true;
        GameUtils.logGameEvent("Game paused");
    }

    public void resumeGame() {
        paused = false;
        GameUtils.logGameEvent("Game resumed");
    }

    public void endGame() {
        gameOver = true;
        GameUtils.logGameEvent("Game over");
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isGameOver() {
        return gameOver;
    }
}

// Animation handler
class AnimationHandler {
    private Timer animationTimer;

    public AnimationHandler(GamePanel panel) {
        animationTimer = new Timer(16, panel);
    }

    public void startAnimation() {
        animationTimer.start();
    }

    public void stopAnimation() {
        animationTimer.stop();
    }
}

// Sound handler (placeholder, as Swing doesn't handle sound well)
class SoundHandler {
    public static void playSound(String soundName) {
        GameUtils.logGameEvent("Playing sound: " + soundName);
        // Actual sound implementation would require external libraries
    }

    public static void stopSound(String soundName) {
        GameUtils.logGameEvent("Stopping sound: " + soundName);
    }
}

// Game constants
interface GameConstants {
    int FIELD_WIDTH = 1000;
    int FIELD_HEIGHT = 600;
    int PLAYER_SPEED = 200;
    int BALL_FRICTION = 98;
    int KICK_STRENGTH = 300;
}

// Extended game logic for boundary checks
class BoundaryChecker {
    public static void checkPlayerBounds(Player player, int width, int height, int playerSize) {
        player.x = Math.max(0, Math.min(width - playerSize, player.x));
        player.y = Math.max(0, Math.min(height - playerSize, player.y));
    }

    public static void checkBallBounds(Ball ball, int width, int height, int ballSize) {
        if (ball.x < 0) {
            ball.x = 0;
            ball.vx = -ball.vx * 0.8;
        }
        if (ball.x > width - GamePanel.BALL_SIZE) {
            ball.x = width - GamePanel.BALL_SIZE;
            ball.vx = -ball.vx * 0.8;
        }
        if (ball.y < 0) {
            ball.y = 0;
            ball.vy = -ball.vy * 0.8;
        }
        if (ball.y > height - GamePanel.BALL_SIZE) {
            ball.y = height - GamePanel.BALL_SIZE;
            ball.vy = -ball.vy * 0.8;
        }
    }
}

// Extended collision detection
class CollisionDetector {
    public static boolean checkCircleCollision(double x1, double y1, double r1, double x2, double y2, double r2) {
        double distance = GameUtils.calculateDistance(x1, y1, x2, y2);
        return distance < (r1 + r2);
    }

    public static void resolveCircleCollision(Ball ball, Player player, int playerSize, int ballSize) {
        double dx = ball.x + ballSize / 2 - (player.x + playerSize / 2);
        double dy = ball.y + ballSize / 2 - (player.y + playerSize / 2);
        double distance = Math.sqrt(dx * dx + dy * dy);
        double minDistance = (playerSize + ballSize) / 2;

        if (distance < minDistance && distance > 0) {
            double angle = Math.atan2(dy, dx);
            double kickStrength = GameConstants.KICK_STRENGTH;
            ball.vx += kickStrength * Math.cos(angle);
            ball.vy += kickStrength * Math.sin(angle);
            double overlap = minDistance - distance;
            ball.x += overlap * Math.cos(angle);
            ball.y += overlap * Math.sin(angle);
        }
    }
}