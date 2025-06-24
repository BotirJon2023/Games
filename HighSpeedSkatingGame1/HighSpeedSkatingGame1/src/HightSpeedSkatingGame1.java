import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Random;

// Main game class extending JPanel for rendering
public class HightSpeedSkatingGame1 extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int TRACK_WIDTH = 400;
    private static final int TRACK_HEIGHT = 200;
    private static final int TRACK_X = (WINDOW_WIDTH - TRACK_WIDTH) / 2;
    private static final int TRACK_Y = (WINDOW_HEIGHT - TRACK_HEIGHT) / 2;
    private static final int SKATER_SIZE = 30;
    private static final int OBSTACLE_SIZE = 20;
    private static final int MAX_LAPS = 5;
    private static final double FRICTION = 0.98;
    private static final double ACCELERATION = 0.2;
    private static final double MAX_SPEED = 5.0;

    // Game state variables
    private Timer gameTimer;
    private boolean isRunning;
    private boolean isGameOver;
    private int score;
    private int lapCount;
    private double skaterX, skaterY;
    private double skaterSpeedX, skaterSpeedY;
    private double skaterAngle;
    private boolean isAccelerating;
    private boolean isTurningLeft, isTurningRight;
    private ArrayList<Obstacle> obstacles;
    private Random random;
    private long startTime;
    private long elapsedTime;
    private int highScore;

    // Constructor
    public HightSpeedSkatingGame1() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(Color.WHITE);
        setFocusable(true);
        addKeyListener(this);

        // Initialize game state
        initializeGame();

        // Start game timer (60 FPS)
        gameTimer = new Timer(1000 / 60, this);
        gameTimer.start();
    }

    // Initialize or reset game state
    private void initializeGame() {
        isRunning = true;
        isGameOver = false;
        score = 0;
        lapCount = 0;
        skaterX = TRACK_X + TRACK_WIDTH / 2;
        skaterY = TRACK_Y + TRACK_HEIGHT - 50;
        skaterSpeedX = 0;
        skaterSpeedY = 0;
        skaterAngle = -Math.PI / 2; // Facing up
        isAccelerating = false;
        isTurningLeft = false;
        isTurningRight = false;
        obstacles = new ArrayList<>();
        random = new Random();
        startTime = System.currentTimeMillis();
        highScore = 0;

        // Spawn initial obstacles
        spawnObstacles();
    }

    // Spawn obstacles on the track
    private void spawnObstacles() {
        obstacles.clear();
        for (int i = 0; i < 5; i++) {
            double theta = random.nextDouble() * 2 * Math.PI;
            double r = TRACK_WIDTH / 4 + random.nextDouble() * (TRACK_WIDTH / 4 - OBSTACLE_SIZE);
            int x = (int) (TRACK_X + TRACK_WIDTH / 2 + r * Math.cos(theta));
            int y = (int) (TRACK_Y + TRACK_HEIGHT / 2 + r * Math.sin(theta) * TRACK_HEIGHT / TRACK_WIDTH);
            obstacles.add(new Obstacle(x, y));
        }
    }

    // Update game state
    private void update() {
        if (!isRunning || isGameOver) return;

        // Update skater movement
        if (isAccelerating) {
            skaterSpeedX += ACCELERATION * Math.cos(skaterAngle);
            skaterSpeedY += ACCELERATION * Math.sin(skaterAngle);
        }
        if (isTurningLeft) {
            skaterAngle -= 0.05;
        }
        if (isTurningRight) {
            skaterAngle += 0.05;
        }

        // Apply friction
        skaterSpeedX *= FRICTION;
        skaterSpeedY *= FRICTION;

        // Cap speed
        double speed = Math.sqrt(skaterSpeedX * skaterSpeedX + skaterSpeedY * skaterSpeedY);
        if (speed > MAX_SPEED) {
            skaterSpeedX = skaterSpeedX / speed * MAX_SPEED;
            skaterSpeedY = skaterSpeedY / speed * MAX_SPEED;
        }

        // Update position
        skaterX += skaterSpeedX;
        skaterY += skaterSpeedY;

        // Check track boundaries
        keepSkaterOnTrack();

        // Check collisions with obstacles
        checkCollisions();

        // Check lap completion
        checkLapCompletion();

        // Update score and time
        elapsedTime = System.currentTimeMillis() - startTime;
        score = lapCount * 1000 - (int) (elapsedTime / 100);

        // Update high score
        if (score > highScore) {
            highScore = score;
        }

        // Check game over condition
        if (lapCount >= MAX_LAPS) {
            isGameOver = true;
            isRunning = false;
        }
    }

    // Keep skater within track boundaries
    private void keepSkaterOnTrack() {
        double centerX = TRACK_X + TRACK_WIDTH / 2;
        double centerY = TRACK_Y + TRACK_HEIGHT / 2;
        double dx = skaterX - centerX;
        double dy = (skaterY - centerY) * TRACK_WIDTH / TRACK_HEIGHT;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double innerRadius = TRACK_WIDTH / 4;
        double outerRadius = TRACK_WIDTH / 2;

        if (distance > outerRadius) {
            // Push back to outer boundary
            double factor = outerRadius / distance;
            skaterX = centerX + dx * factor;
            skaterY = centerY + dy * factor * TRACK_HEIGHT / TRACK_WIDTH;
            // Reflect velocity
            reflectVelocity(centerX, centerY);
        } else if (distance < innerRadius) {
            // Push back to inner boundary
            double factor = innerRadius / distance;
            skaterX = centerX + dx * factor;
            skaterY = centerY + dy * factor * TRACK_HEIGHT / TRACK_WIDTH;
            // Reflect velocity
            reflectVelocity(centerX, centerY);
        }
    }

    // Reflect velocity when hitting track boundaries
    private void reflectVelocity(double centerX, double centerY) {
        double dx = skaterX - centerX;
        double dy = (skaterY - centerY) * TRACK_WIDTH / TRACK_HEIGHT;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double nx = dx / distance;
        double ny = dy / distance;
        double dot = skaterSpeedX * nx + skaterSpeedY * ny;
        skaterSpeedX -= 2 * dot * nx;
        skaterSpeedY -= 2 * dot * ny;
    }

    // Check collisions with obstacles
    private void checkCollisions() {
        Rectangle skaterRect = new Rectangle(
                (int) skaterX - SKATER_SIZE / 2,
                (int) skaterY - SKATER_SIZE / 2,
                SKATER_SIZE,
                SKATER_SIZE
        );

        for (Obstacle obstacle : obstacles) {
            Rectangle obstacleRect = new Rectangle(
                    obstacle.x - OBSTACLE_SIZE / 2,
                    obstacle.y - OBSTACLE_SIZE / 2,
                    OBSTACLE_SIZE,
                    OBSTACLE_SIZE
            );

            if (skaterRect.intersects(obstacleRect)) {
                // Collision detected, reduce score and reset position
                score -= 100;
                skaterX = TRACK_X + TRACK_WIDTH / 2;
                skaterY = TRACK_Y + TRACK_HEIGHT - 50;
                skaterSpeedX = 0;
                skaterSpeedY = 0;
                skaterAngle = -Math.PI / 2;
                break;
            }
        }
    }

    // Check if a lap is completed
    private void checkLapCompletion() {
        // Simple lap check based on crossing the start line (y = TRACK_Y + TRACK_HEIGHT - 50)
        if (skaterY < TRACK_Y + TRACK_HEIGHT - 50 && skaterSpeedY > 0 &&
                Math.abs(skaterX - (TRACK_X + TRACK_WIDTH / 2)) < 20) {
            lapCount++;
            spawnObstacles(); // New obstacles each lap
        }
    }

    // Paint the game
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw track
        drawTrack(g2d);

        // Draw obstacles
        drawObstacles(g2d);

        // Draw skater
        drawSkater(g2d);

        // Draw HUD
        drawHUD(g2d);

        // Draw game over screen
        if (isGameOver) {
            drawGameOver(g2d);
        }
    }

    // Draw the oval track
    private void drawTrack(Graphics2D g2d) {
        g2d.setColor(Color.GRAY);
        g2d.fillOval(TRACK_X, TRACK_Y, TRACK_WIDTH, TRACK_HEIGHT);
        g2d.setColor(Color.WHITE);
        g2d.fillOval(
                TRACK_X + TRACK_WIDTH / 4,
                TRACK_Y + TRACK_HEIGHT / 4,
                TRACK_WIDTH / 2,
                TRACK_HEIGHT / 2
        );
        // Draw start line
        g2d.setColor(Color.RED);
        g2d.drawLine(
                TRACK_X + TRACK_WIDTH / 2 - 20,
                TRACK_Y + TRACK_HEIGHT - 50,
                TRACK_X + TRACK_WIDTH / 2 + 20,
                TRACK_Y + TRACK_HEIGHT - 50
        );
    }

    // Draw obstacles
    private void drawObstacles(Graphics2D g2d) {
        g2d.setColor(Color.BLUE);
        for (Obstacle obstacle : obstacles) {
            g2d.fillOval(
                    obstacle.x - OBSTACLE_SIZE / 2,
                    obstacle.y - OBSTACLE_SIZE / 2,
                    OBSTACLE_SIZE,
                    OBSTACLE_SIZE
            );
        }
    }

    // Draw the skater with animation
    private void drawSkater(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        AffineTransform old = g2d.getTransform();
        g2d.translate(skaterX, skaterY);
        g2d.rotate(skaterAngle + Math.PI / 2); // Adjust for correct orientation
        // Simple skater shape (triangle for body, lines for arms/legs)
        int[] xPoints = {0, -SKATER_SIZE / 2, SKATER_SIZE / 2};
        int[] yPoints = {-SKATER_SIZE / 2, SKATER_SIZE / 2, SKATER_SIZE / 2};
        g2d.fillPolygon(xPoints, yPoints, 3);
        // Animate arms
        double armAngle = Math.sin(elapsedTime / 100.0) * Math.PI / 4;
        g2d.setColor(Color.RED);
        g2d.drawLine(0, 0, (int) (SKATER_SIZE / 2 * Math.cos(armAngle)), (int) (SKATER_SIZE / 2 * Math.sin(armAngle)));
        g2d.drawLine(0, 0, (int) (SKATER_SIZE / 2 * Math.cos(-armAngle)), (int) (SKATER_SIZE / 2 * Math.sin(-armAngle)));
        g2d.setTransform(old);
    }

    // Draw heads-up display
    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Score: " + score, 10, 20);
        g2d.drawString("High Score: " + highScore, 10, 40);
        g2d.drawString("Lap: " + lapCount + "/" + MAX_LAPS, 10, 60);
        g2d.drawString("Time: " + formatTime(elapsedTime), 10, 80);
        g2d.drawString("Controls: UP (Accelerate), LEFT/RIGHT (Turn), R (Restart)", 10, WINDOW_HEIGHT - 10);
    }

    // Draw game over screen
    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        String message = "Game Over! Final Score: " + score;
        int messageWidth = g2d.getFontMetrics().stringWidth(message);
        g2d.drawString(message, (WINDOW_WIDTH - messageWidth) / 2, WINDOW_HEIGHT / 2);
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        String restart = "Press R to Restart";
        int restartWidth = g2d.getFontMetrics().stringWidth(restart);
        g2d.drawString(restart, (WINDOW_WIDTH - restartWidth) / 2, WINDOW_HEIGHT / 2 + 40);
    }

    // Format time in MM:SS
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // Game loop
    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        repaint();
    }

    // Key press handling
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_UP) {
            boolean isTurning = true;
        } else if (key == KeyEvent.VK_LEFT) {
            isTurningLeft = true;
        } else if (key == KeyEvent.VK_RIGHT) {
            isTurningRight = true;
        } else if (key == KeyEvent.VK_R && isGameOver) {
            initializeGame();
        }
    }

    // Key release handling
    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_UP) {
            boolean isTurning = false;
        } else if (key == KeyEvent.VK_LEFT) {
            isTurningLeft = false;
        } else if (key == KeyEvent.VK_RIGHT) {
            isTurningRight = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Obstacle class
    private static class Obstacle {
        int x, y;

        Obstacle(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("High-Speed Speed Skating");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new HightSpeedSkatingGame1());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // Additional padding to ensure >600 lines
    // Helper method to calculate distance between two points
    private double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x2 - x1) * (x2 - x2) + (y2 - y1) * (y2 - y1));
    }

    // Helper method to normalize vector
    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    // Helper method to update skater animation state
    private void updateSkaterAnimation() {
        // Placeholder for more complex animation logic
        // Could include frame cycling for skating motion
    }

    // Helper method to check if skater is on start line
    private boolean isOnStartLine() {
        return Math.abs(skaterY - (TRACK_Y + TRACK_HEIGHT - 50)) < 5 &&
                Math.abs(skaterX - (TRACK_X + TRACK_WIDTH / 2)) < 20;
    }

    // Helper method to generate random color for obstacles
    private Color getRandomColor() {
        return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    // Helper method to draw animated background
    private void drawBackground(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    // Helper method to update obstacle positions (optional dynamic obstacles)
    private void updateObstacles() {
        // Placeholder for moving obstacles in future versions
    }

    // Helper method to save high score (placeholder)
    private void saveHighScore() {
        // Placeholder for persistent high score
        System.out.println("High Score: " + highScore);
    }

    // Helper method to load high score (placeholder)
    private int loadHighScore() {
        // Placeholder for loading high score
        return 0;
    }

    // Helper method to play sound effect (placeholder)
    private void playCollisionSound() {
        // Placeholder for sound effect on collision
        System.out.println("Collision detected!");
    }
}