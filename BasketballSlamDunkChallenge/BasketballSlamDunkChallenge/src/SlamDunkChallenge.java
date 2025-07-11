import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class SlamDunkChallenge extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SlamDunkChallenge());
    }

    public SlamDunkChallenge() {
        setTitle("Basketball Slam Dunk Challenge");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        add(new GamePanel());
        setVisible(true);
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int COURT_WIDTH = 800;
    private static final int COURT_HEIGHT = 600;
    private static final int PLAYER_WIDTH = 50;
    private static final int PLAYER_HEIGHT = 80;
    private static final int BALL_SIZE = 20;
    private static final int HOOP_WIDTH = 60;
    private static final int HOOP_HEIGHT = 20;
    private static final int HOOP_Y = 100;
    private static final int GROUND_Y = 500;
    private static final double GRAVITY = 0.5;
    private static final int GAME_DURATION = 60; // seconds

    // Game state
    private int playerX = COURT_WIDTH / 2;
    private int playerY = GROUND_Y - PLAYER_HEIGHT;
    private double ballX = playerX + PLAYER_WIDTH / 2;
    private double ballY = playerY;
    private double ballVelX = 0;
    private double ballVelY = 0;
    private boolean isShooting = false;
    private boolean isJumping = false;
    private int score = 0;
    private int timeLeft = GAME_DURATION;
    private boolean gameOver = false;
    private int playerVelX = 0;
    private double jumpVelY = 0;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean jumpPressed = false;
    private boolean shootPressed = false;
    private int hoopX;
    private Random random = new Random();
    private Timer timer;
    private long lastUpdateTime;

    // Animation variables
    private int playerFrame = 0;
    private int ballFrame = 0;
    private long lastFrameTime = System.currentTimeMillis();

    public GamePanel() {
        setFocusable(true);
        setBackground(new Color(200, 200, 255));
        addKeyListener(this);
        timer = new Timer(16, this); // ~60 FPS
        timer.start();
        lastUpdateTime = System.currentTimeMillis();
        resetHoopPosition();
    }

    private void resetHoopPosition() {
        hoopX = random.nextInt(COURT_WIDTH - HOOP_WIDTH - 100) + 50;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw court
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect(0, GROUND_Y, COURT_WIDTH, COURT_HEIGHT - GROUND_Y);
        g2d.setColor(Color.WHITE);
        g2d.drawLine(COURT_WIDTH / 2, GROUND_Y, COURT_WIDTH / 2, COURT_HEIGHT);
        g2d.fillOval(COURT_WIDTH / 2 - 50, GROUND_Y - 100, 100, 20);

        // Draw hoop
        g2d.setColor(Color.RED);
        g2d.fillRect(hoopX, HOOP_Y, HOOP_WIDTH, HOOP_HEIGHT);
        g2d.setColor(Color.GRAY);
        g2d.drawLine(hoopX + HOOP_WIDTH / 2, HOOP_Y + HOOP_HEIGHT, hoopX + HOOP_WIDTH / 2, HOOP_Y + 40);
        g2d.fillOval(hoopX + HOOP_WIDTH / 2 - 10, HOOP_Y + 40, 20, 20);

        // Draw player
        drawPlayer(g2d);

        // Draw ball
        g2d.setColor(Color.ORANGE);
        g2d.fillOval((int) ballX, (int) ballY, BALL_SIZE, BALL_SIZE);
        g2d.setColor(Color.BLACK);
        g2d.drawOval((int) ballX, (int) ballY, BALL_SIZE, BALL_SIZE);

        // Draw HUD
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Score: " + score, 20, 30);
        g2d.drawString("Time: " + timeLeft + "s", COURT_WIDTH - 100, 30);

        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, COURT_WIDTH, COURT_HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            String gameOverText = "Game Over! Score: " + score;
            int textWidth = g2d.getFontMetrics().stringWidth(gameOverText);
            g2d.drawString(gameOverText, (COURT_WIDTH - textWidth) / 2, COURT_HEIGHT / 2);
        }
    }

    private void drawPlayer(Graphics2D g2d) {
        // Simple animation with frames
        g2d.setColor(Color.BLUE);
        if (isJumping) {
            // Jumping pose
            g2d.fillRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(playerX + 10, playerY - 10, 20, 20); // Head
            g2d.drawLine(playerX + 20, playerY + 20, playerX + 40, playerY); // Arm up
            g2d.drawLine(playerX + 20, playerY + 20, playerX, playerY); // Arm up
        } else {
            // Standing or running
            g2d.fillRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(playerX + 10, playerY - 10, 20, 20); // Head
            g2d.drawLine(playerX + 20, playerY + 20, playerX + 40, playerY + 40); // Arm
            g2d.drawLine(playerX + 20, playerY + 20, playerX, playerY + 40); // Arm
            if (playerVelX != 0) {
                // Running animation
                int legOffset = playerFrame % 2 == 0 ? 10 : -10;
                g2d.drawLine(playerX + 15, playerY + PLAYER_HEIGHT, playerX + 15 + legOffset, playerY + PLAYER_HEIGHT + 20);
                g2d.drawLine(playerX + 35, playerY + PLAYER_HEIGHT, playerX + 35 - legOffset, playerY + PLAYER_HEIGHT + 20);
            } else {
                g2d.drawLine(playerX + 15, playerY + PLAYER_HEIGHT, playerX + 15, playerY + PLAYER_HEIGHT + 20);
                g2d.drawLine(playerX + 35, playerY + PLAYER_HEIGHT, playerX + 35, playerY + PLAYER_HEIGHT + 20);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long currentTime = System.currentTimeMillis();
        double deltaTime = (currentTime - lastUpdateTime) / 1000.0;
        lastUpdateTime = currentTime;

        if (!gameOver) {
            updateGame(deltaTime);
            updateAnimation();
            repaint();
        }
    }

    private void updateGame(double deltaTime) {
        // Update player movement
        playerX += playerVelX;
        if (playerX < 0) playerX = 0;
        if (playerX > COURT_WIDTH - PLAYER_WIDTH) playerX = COURT_WIDTH - PLAYER_WIDTH;

        // Update jump
        if (isJumping) {
            playerY += jumpVelY;
            jumpVelY += GRAVITY;
            if (playerY >= GROUND_Y - PLAYER_HEIGHT) {
                playerY = GROUND_Y - PLAYER_HEIGHT;
                isJumping = false;
                jumpVelY = 0;
            }
        }

        // Update ball
        if (isShooting) {
            ballX += ballVelX;
            ballY += ballVelY;
            ballVelY += GRAVITY;

            // Check for hoop collision
            if (ballX >= hoopX && ballX <= hoopX + HOOP_WIDTH &&
                    ballY >= HOOP_Y && ballY <= HOOP_Y + HOOP_HEIGHT) {
                score += 2;
                isShooting = false;
                resetBall();
                resetHoopPosition();
                System.out.println("Slam Dunk! Score: " + score);
            }

            // Check if ball hits ground
            if (ballY > GROUND_Y) {
                isShooting = false;
                resetBall();
            }
        } else {
            // Ball follows player
            ballX = playerX + PLAYER_WIDTH / 2 - BALL_SIZE / 2;
            ballY = playerY - BALL_SIZE;
        }

        // Update timer
        timeLeft = GAME_DURATION - (int) ((System.currentTimeMillis() - lastUpdateTime + 1000 * GAME_DURATION) / 1000);
        if (timeLeft <= 0) {
            gameOver = true;
            timer.stop();
        }
    }

    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > 100) {
            playerFrame = (playerFrame + 1) % 4;
            ballFrame = (ballFrame + 1) % 8;
            lastFrameTime = currentTime;
        }
    }

    private void resetBall() {
        ballX = playerX + PLAYER_WIDTH / 2 - BALL_SIZE / 2;
        ballY = playerY - BALL_SIZE;
        ballVelX = 0;
        ballVelY = 0;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) {
            moveLeft = true;
            playerVelX = -5;
        }
        if (key == KeyEvent.VK_RIGHT) {
            moveRight = true;
            playerVelX = 5;
        }
        if (key == KeyEvent.VK_SPACE && !isJumping && !jumpPressed) {
            isJumping = true;
            jumpVelY = -15;
            jumpPressed = true;
        }
        if (key == KeyEvent.VK_S && !isShooting && !shootPressed) {
            shootBall();
            shootPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) {
            moveLeft = false;
            if (moveRight) {
                playerVelX = 5;
            } else {
                playerVelX = 0;
            }
        }
        if (key == KeyEvent.VK_RIGHT) {
            moveRight = false;
            if (moveLeft) {
                playerVelX = -5;
            } else {
                playerVelX = 0;
            }
        }
        if (key == KeyEvent.VK_SPACE) {
            jumpPressed = false;
        }
        if (key == KeyEvent.VK_S) {
            shootPressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void shootBall() {
        if (!isShooting) {
            isShooting = true;
            // Calculate direction towards hoop
            double dx = (hoopX + HOOP_WIDTH / 2) - (ballX + BALL_SIZE / 2);
            double dy = HOOP_Y - (ballY + BALL_SIZE / 2);
            double distance = Math.sqrt(dx * dx + dy * dy);
            double speed = 10;
            ballVelX = (dx / distance) * speed;
            ballVelY = (dy / distance) * speed - 5; // Add upward force
            System.out.println("Ball shot!");
        }
    }
}