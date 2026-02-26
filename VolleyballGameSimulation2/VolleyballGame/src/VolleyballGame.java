// VolleyballGame.java - Main Application Window
import javax.swing.JFrame;
import java.awt.*;

public class VolleyballGame extends JFrame {
    private GamePanel gamePanel;
    public VolleyballGame() {
        setTitle("Volleyball Game Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(800, 600)); // Set initial size
        add(gamePanel);
        pack(); // Adjusts window size to fit preferred size of subcomponents
        setLocationRelativeTo(null); // Center the window
    }
    public static void main(String[] args) {
        // Run the game on the Event Dispatch Thread for thread safety
        javax.swing.SwingUtilities.invokeLater(() -> {
            VolleyballGame game = new VolleyballGame();
            game.setVisible(true);
            game.startGame(); // Start the game loop
        });
    }
    public void startGame() {
        gamePanel.startGameThread();
    }
}
// GamePanel.java - Handles Drawing and Game Loop
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.Timer; // For game loop
import java.util.ArrayList;
import java.util.List;
public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private Timer gameTimer;
    private final int DELAY = 15; // Milliseconds between updates (approx 60 FPS)
    // Game Objects
    private Ball ball;
    private Player player1;
    private Player player2;
    private Court court;
    private GameLogic gameLogic;
    // Game state variables
    private boolean running = false;
    private int scorePlayer1 = 0;
    private int scorePlayer2 = 0;
    public GamePanel() {
        setBackground(new Color(135, 206, 235)); // Sky blue background
        setFocusable(true);
        addKeyListener(this);
        initGame();
    }
    private void initGame() {
        court = new Court(0, 500, 800, 100); // Ground for court
        ball = new Ball(400, 300, 15, Color.WHITE);
        player1 = new Player(100, 450, 30, 50, Color.RED, "Player 1");
        player2 = new Player(700, 450, 30, 50, Color.BLUE, "Player 2");
        gameLogic = new GameLogic(ball, player1, player2, court);
        resetGame(); // Reset positions etc.
    }
    public void startGameThread() {
        running = true;
        gameTimer = new Timer(DELAY, this);
        gameTimer.start();
    }
    public void stopGameThread() {
        running = false;
        if (gameTimer != null) {
            gameTimer.stop();
        }
    }
    private void updateGame() {
        if (running) {
            gameLogic.update();
            // Check for scoring
            if (ball.getY() > getHeight() - ball.getSize()) { // Ball hit ground
                if (ball.getX() < getWidth() / 2) { // Player 2 scored
                    scorePlayer2++;
                } else { // Player 1 scored
                    scorePlayer1++;
                }
                resetRound();
            }
        }
    }
    private void resetRound() {
        ball.reset(getWidth() / 2, getHeight() / 2);
        player1.reset(100, 450);
        player2.reset(700, 450);
        // Maybe add a slight delay before next round starts
    }
    private void resetGame() {
        scorePlayer1 = 0;
        scorePlayer2 = 0;
        resetRound();
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        // Draw Court
        court.draw(g2d, getWidth(), getHeight());
        // Draw Net (part of court or separate)
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(getWidth() / 2 - 5, 250, 10, getHeight() - 250); // Simple net
        // Draw Players
        player1.draw(g2d);
        player2.draw(g2d);
        // Draw Ball
        ball.draw(g2d);
        // Draw Scores
        g2d.setColor(Color.BLACK);
        g2d.setFont(g2d.getFont().deriveFont(24f));
        g2d.drawString("P1: " + scorePlayer1, getWidth() / 4 - 50, 50);
        g2d.drawString("P2: " + scorePlayer2, getWidth() * 3 / 4 - 50, 50);
        // Sync graphics
        java.awt.Toolkit.getDefaultToolkit().sync();
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        updateGame();
        repaint(); // Request repaint to update animation
    }
    // KeyListener methods for player control
    @Override
    public void keyPressed(KeyEvent e) {
        // Player 1 controls (e.g., A/D for move, W for jump)
        if (e.getKeyCode() == KeyEvent.VK_A) {
            player1.setMovingLeft(true);
        }
        if (e.getKeyCode() == KeyEvent.VK_D) {
            player1.setMovingRight(true);
        }
        if (e.getKeyCode() == KeyEvent.VK_W) {
            player1.jump();
        }
        // Player 2 controls (e.g., Left/Right arrow for move, Up arrow for jump)
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            player2.setMovingLeft(true);
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            player2.setMovingRight(true);
        }
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            player2.jump();
        }
    }
    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_A) {
            player1.setMovingLeft(false);
        }
        if (e.getKeyCode() == KeyEvent.VK_D) {
            player1.setMovingRight(false);
        }
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            player2.setMovingLeft(false);
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            player2.setMovingRight(false);
        }
    }
    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
}
// Ball.java - Represents the Volleyball
import java.awt.Color;
import java.awt.Graphics2D;
public class Ball {
    private double x, y; // Position
    private double velX, velY; // Velocity
    private int size;
    private Color color;
    private double gravity = 0.3; // Gravity effect
    private double friction = 0.98; // Air resistance / ground friction
    private double bounceFactor = 0.8; // How much velocity is retained on bounce
    public Ball(double x, double y, int size, Color color) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = color;
        this.velX = 0; // Initial velocity
        this.velY = 0;
    }
    public void update(int panelWidth, int panelHeight, Court court) {
        // Apply gravity
        velY += gravity;
        // Apply friction
        velX *= friction;
        // Update position
        x += velX;
        y += velY;
        // Wall collisions
        if (x < size / 2) {
            x = size / 2;
            velX *= -bounceFactor;
        } else if (x > panelWidth - size / 2) {
            x = panelWidth - size / 2;
            velX *= -bounceFactor;
        }
        // Ground/ceiling collisions
        if (y < size / 2) { // Ceiling
            y = size / 2;
            velY *= -bounceFactor;
        } else if (y > court.getGroundY() - size / 2) { // Ground (above court floor)
            y = court.getGroundY() - size / 2;
            velY *= -bounceFactor;
            // Additional friction on ground
            velX *= 0.8; // More friction on ground
        }
        // NOTE: Ball falling below court.getGroundY() is handled in GamePanel for scoring
    }
    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillOval((int) (x - size / 2), (int) (y - size / 2), size, size);
        g2d.setColor(Color.BLACK);
        g2d.drawOval((int) (x - size / 2), (int) (y - size / 2), size, size);
    }
    public void applyHit(double hitVelX, double hitVelY) {
        this.velX = hitVelX;
        this.velY = hitVelY;
    }
    public void reset(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.velX = 0;
        this.velY = 0;
    }
    // Getters for position and size (used for collision detection)
    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return size; }
    public double getVelX() { return velX; }
    public double getVelY() { return velY; }
    public void setVelX(double velX) { this.velX = velX; }
    public void setVelY(double velY) { this.velY = velY; }

    public void setX(int i) {
    }
}
// Player.java - Represents a Player
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle; // For collision detection
public class Player {
    private double x, y;
    private double velX, velY;
    private int width, height;
    private Color color;
    private String name;
    private double speed = 2.5;
    private double jumpStrength = -8; // Negative for upward movement
    private double gravity = 0.3;
    private boolean onGround = true;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    public Player(double x, double y, int width, int height, Color color, String name) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.name = name;
        this.velX = 0;
        this.velY = 0;
    }
    public void update(int panelWidth, int panelHeight, Court court) {
        // Apply horizontal movement
        if (movingLeft) {
            velX = -speed;
        } else if (movingRight) {
            velX = speed;
        } else {
            velX = 0; // Stop moving if no key pressed
        }
        // Apply gravity
        if (!onGround) {
            velY += gravity;
        } else {
            velY = 0; // Reset vertical velocity if on ground
        }
        // Update position
        x += velX;
        y += velY;
        // Keep player within horizontal bounds
        if (x < 0) {
            x = 0;
        } else if (x > panelWidth - width) {
            x = panelWidth - width;
        }
        // Collision with ground
        if (y > court.getGroundY() - height) {
            y = court.getGroundY() - height;
            onGround = true;
        } else {
            onGround = false;
        }
    }
    public void jump() {
        if (onGround) {
            velY = jumpStrength;
            onGround = false;
        }
    }
    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillRect((int) x, (int) y, width, height);
        g2d.setColor(Color.BLACK);
        g2d.drawRect((int) x, (int) y, width, height);
        g2d.drawString(name, (int) x, (int) y - 5);
    }
    // Getters for position and bounds
    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public double getVelX() { return velX; }
    public double getVelY() { return velY; }
    // Setters for movement
    public void setMovingLeft(boolean movingLeft) { this.movingLeft = movingLeft; }
    public void setMovingRight(boolean movingRight) { this.movingRight = movingRight; }
    public void reset(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.velX = 0;
        this.velY = 0;
        this.onGround = true;
    }
}
// Court.java - Represents the Game Court
import java.awt.Color;
import java.awt.Graphics2D;
public class Court {
    private int x, y, width, height; // x, y are top-left of the court area
    private Color groundColor = new Color(50, 150, 50); // Green ground
    public Court(int x, int y, int width, int height) {
        this.x = x;
        this.y = y; // This 'y' will be the top of the ground patch
        this.width = width;
        this.height = height; // This height is for the ground patch
    }
    public void draw(Graphics2D g2d, int panelWidth, int panelHeight) {
        // Draw the ground
        g2d.setColor(groundColor);
        g2d.fillRect(x, y, panelWidth, height); // Extend to full panel width
        // Draw court lines (optional)
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, y, panelWidth, 5); // White line on top of ground
        // You could add more lines here for service areas, etc.
    }
    public int getGroundY() {
        return y; // The Y coordinate where the ground starts
    }
}
// GameLogic.java - Handles Game State and Collisions
import java.awt.Rectangle;
import java.util.Random;
public class GameLogic {
    private Ball ball;
    private Player player1;
    private Player player2;
    private Court court;
    private Random random;
    public GameLogic(Ball ball, Player player1, Player player2, Court court) {
        this.ball = ball;
        this.player1 = player1;
        this.player2 = player2;
        this.court = court;
        this.random = new Random();
    }
    public void update() {
        // Update player positions
        player1.update(800, 600, court); // Panel dimensions passed for bounds
        player2.update(800, 600, court);
        // Update ball position
        ball.update(800, 600, court);
        // Check for collisions
        checkBallPlayerCollision(ball, player1);
        checkBallPlayerCollision(ball, player2);
        // Add net collision logic here
        checkBallNetCollision();
    }
    private void checkBallPlayerCollision(Ball ball, Player player) {
        Rectangle ballBounds = new Rectangle(
                (int) (ball.getX() - ball.getSize() / 2),
                (int) (ball.getY() - ball.getSize() / 2),
                ball.getSize(),
                ball.getSize()
        );
        Rectangle playerBounds = player.getBounds();
        if (ballBounds.intersects(playerBounds)) {
            // Simple collision response: reverse vertical velocity and add some horizontal kick
            double newVelX = ball.getVelX() + player.getVelX() * 0.5 + (random.nextDouble() * 2 - 1); // Add some randomness
            double newVelY = -Math.abs(ball.getVelY()) * 0.9; // Always go up, slightly reduced bounce
            if (newVelY > -5) newVelY = -5; // Ensure minimum upward velocity on hit
            // If ball is above player, give it more vertical kick
            if (ball.getY() < player.getY()) {
                newVelY -= 2;
            }
            ball.applyHit(newVelX, newVelY);
            // Prevent ball from sticking to player by slightly moving it out
            if (ball.getY() > player.getY() + player.getHeight() / 2) {
                ball.setVelY(Math.abs(ball.getVelY())); // If hitting player's bottom, move down
            } else {
                ball.setVelY(-Math.abs(ball.getVelY())); // Move up
            }
        }
    }
    private void checkBallNetCollision() {
        // Assume net is at x = panelWidth / 2, from y = 250 to panelHeight
        int netX = 800 / 2; // Midpoint of panel
        int netY_top = 250;
        int netWidth = 10;
        int netHeight = 600 - netY_top; // From net top to bottom of screen
        Rectangle netBounds = new Rectangle(netX - netWidth / 2, netY_top, netWidth, netHeight);
        Rectangle ballBounds = new Rectangle(
                (int) (ball.getX() - ball.getSize() / 2),
                (int) (ball.getY() - ball.getSize() / 2),
                ball.getSize(),
                ball.getSize()
        );
        if (ballBounds.intersects(netBounds)) {
            // Simple net bounce logic
            // If ball hits top of net, bounce up
            if (ball.getY() < netY_top + netWidth) { // Check if it's hitting the top edge of net
                ball.setVelY(-Math.abs(ball.getVelY()) * 0.7); // Bounce up, lose some energy
                // Prevent ball from getting stuck in the net by slightly moving it up
                ball.setVelX(ball.getVelX() * 0.8); // Lose some horizontal speed too
            } else { // Hits side of net
                ball.setVelX(-ball.getVelX() * 0.7); // Reverse horizontal velocity, lose energy
                // Adjust position to prevent sticking
                if (ball.getX() < netX) {
                    ball.setX(netX - ball.getSize()/2 - 1);
                } else {
                    ball.setX(netX + netWidth + ball.getSize()/2 + 1);
                }
            }
        }
    }
}