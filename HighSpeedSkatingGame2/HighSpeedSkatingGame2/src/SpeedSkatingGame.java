import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class SpeedSkatingGame extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 60;
    private static final int OBSTACLE_WIDTH = 30;
    private static final int OBSTACLE_HEIGHT = 30;
    private static final int TRACK_WIDTH = 300;
    private static final int TRACK_CENTER = WIDTH / 2;
    private static final int FINISH_LINE = 10000;

    // Game variables
    private int playerX = TRACK_CENTER;
    private int playerY = HEIGHT - 100;
    private int playerSpeed = 5;
    private int playerStamina = 100;
    private int distance = 0;
    private int score = 0;
    private boolean gameOver = false;
    private boolean gameStarted = false;
    private boolean isJumping = false;
    private int jumpHeight = 0;
    private int maxJumpHeight = 100;

    // Physics
    private double velocity = 0;
    private double acceleration = 0.1;
    private double maxVelocity = 15;
    private double friction = 0.02;

    // Track elements
    private ArrayList<Obstacle> obstacles = new ArrayList<>();
    private ArrayList<Bonus> bonuses = new ArrayList<>();
    private Random random = new Random();

    // Timer for game loop
    private Timer timer;

    // Images (we'll use simple shapes for this example)
    private Color playerColor = new Color(0, 100, 255);
    private Color trackColor = new Color(200, 200, 200);
    private Color iceColor = new Color(200, 240, 255);
    private Color obstacleColor = new Color(150, 75, 0);
    private Color bonusColor = new Color(0, 255, 0);

    // Keyboard states
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;

    public SpeedSkatingGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(iceColor);
        addKeyListener(this);
        setFocusable(true);

        timer = new Timer(16, this); // ~60 FPS
    }

    public void startGame() {
        gameStarted = true;
        gameOver = false;
        distance = 0;
        score = 0;
        playerStamina = 100;
        velocity = 5;
        playerX = TRACK_CENTER;
        obstacles.clear();
        bonuses.clear();
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (!gameStarted) {
            drawMenu(g);
            return;
        }

        if (gameOver) {
            drawGameOver(g);
            return;
        }

        // Draw ice rink background
        drawIceRink(g);

        // Draw track boundaries
        drawTrack(g);

        // Draw obstacles
        for (Obstacle obstacle : obstacles) {
            drawObstacle(g, obstacle);
        }

        // Draw bonuses
        for (Bonus bonus : bonuses) {
            drawBonus(g, bonus);
        }

        // Draw player
        drawPlayer(g);

        // Draw HUD
        drawHUD(g);
    }

    private void drawMenu(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString("SPEED SKATING CHALLENGE", WIDTH/2 - 250, HEIGHT/2 - 50);

        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Press SPACE to Start", WIDTH/2 - 100, HEIGHT/2 + 50);

        g.drawString("Controls:", WIDTH/2 - 60, HEIGHT/2 + 100);
        g.drawString("LEFT/RIGHT - Move", WIDTH/2 - 80, HEIGHT/2 + 130);
        g.drawString("UP - Sprint (consumes stamina)", WIDTH/2 - 150, HEIGHT/2 + 160);
        g.drawString("SPACE - Jump", WIDTH/2 - 90, HEIGHT/2 + 190);
    }

    private void drawGameOver(Graphics g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(WIDTH/2 - 200, HEIGHT/2 - 100, 400, 200);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString("GAME OVER", WIDTH/2 - 90, HEIGHT/2 - 40);

        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Distance: " + distance + "m", WIDTH/2 - 70, HEIGHT/2);
        g.drawString("Score: " + score, WIDTH/2 - 50, HEIGHT/2 + 40);

        g.drawString("Press SPACE to Restart", WIDTH/2 - 110, HEIGHT/2 + 80);
    }

    private void drawIceRink(Graphics g) {
        // Draw ice surface
        g.setColor(iceColor);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw some ice details
        g.setColor(new Color(220, 240, 255));
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            int size = random.nextInt(5) + 1;
            g.fillOval(x, y, size, size);
        }
    }

    private void drawTrack(Graphics g) {
        // Draw track
        g.setColor(trackColor);
        g.fillRect(TRACK_CENTER - TRACK_WIDTH/2, 0, TRACK_WIDTH, HEIGHT);

        // Draw track markings
        g.setColor(Color.WHITE);
        // Center line
        g.drawLine(TRACK_CENTER, 0, TRACK_CENTER, HEIGHT);
        // Side lines
        g.drawLine(TRACK_CENTER - TRACK_WIDTH/2, 0, TRACK_CENTER - TRACK_WIDTH/2, HEIGHT);
        g.drawLine(TRACK_CENTER + TRACK_WIDTH/2, 0, TRACK_CENTER + TRACK_WIDTH/2, HEIGHT);

        // Draw distance markers
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        for (int y = HEIGHT - (distance % 100); y > 0; y -= 100) {
            int markerDistance = distance + (HEIGHT - y);
            g.drawString(markerDistance + "m", TRACK_CENTER + TRACK_WIDTH/2 + 10, y);
        }
    }

    private void drawPlayer(Graphics g) {
        // Draw skater body
        g.setColor(playerColor);
        int drawY = playerY - jumpHeight;
        g.fillOval(playerX - PLAYER_WIDTH/2, drawY - PLAYER_HEIGHT/2, PLAYER_WIDTH, PLAYER_HEIGHT);

        // Draw skates
        g.setColor(Color.BLACK);
        g.fillRect(playerX - PLAYER_WIDTH/2 - 5, drawY + PLAYER_HEIGHT/2 - 5, 10, 10);
        g.fillRect(playerX + PLAYER_WIDTH/2 - 5, drawY + PLAYER_HEIGHT/2 - 5, 10, 10);

        // Draw motion lines if moving fast
        if (velocity > maxVelocity * 0.7) {
            g.setColor(new Color(255, 255, 255, 100));
            for (int i = 0; i < 3; i++) {
                int lineX = playerX - PLAYER_WIDTH/2 - 10 - i * 10;
                int lineY1 = drawY + PLAYER_HEIGHT/4 - i * 5;
                int lineY2 = drawY + PLAYER_HEIGHT/2 + i * 5;
                g.drawLine(lineX, lineY1, lineX, lineY2);
            }
        }
    }

    private void drawObstacle(Graphics g, Obstacle obstacle) {
        g.setColor(obstacleColor);
        g.fillRect(obstacle.x - OBSTACLE_WIDTH/2, obstacle.y - OBSTACLE_HEIGHT/2,
                OBSTACLE_WIDTH, OBSTACLE_HEIGHT);

        // Add some details
        g.setColor(new Color(100, 50, 0));
        g.drawRect(obstacle.x - OBSTACLE_WIDTH/2 + 5, obstacle.y - OBSTACLE_HEIGHT/2 + 5,
                OBSTACLE_WIDTH - 10, OBSTACLE_HEIGHT - 10);
    }

    private void drawBonus(Graphics g, Bonus bonus) {
        g.setColor(bonusColor);
        g.fillOval(bonus.x - OBSTACLE_WIDTH/2, bonus.y - OBSTACLE_HEIGHT/2,
                OBSTACLE_WIDTH, OBSTACLE_HEIGHT);

        // Add some details
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("+", bonus.x - 5, bonus.y + 5);
    }

    private void drawHUD(Graphics g) {
        // Draw distance
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Distance: " + distance + "m", 20, 30);

        // Draw score
        g.drawString("Score: " + score, 20, 60);

        // Draw speed meter
        int speed = (int)(velocity * 10);
        g.drawString("Speed: " + speed + " km/h", 20, 90);

        // Draw stamina bar
        g.drawString("Stamina: ", 20, 120);
        g.setColor(Color.GREEN);
        g.fillRect(120, 110, playerStamina * 2, 20);
        g.setColor(Color.BLACK);
        g.drawRect(120, 110, 200, 20);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameStarted || gameOver) return;

        // Update player movement
        updatePlayerPosition();

        // Update game physics
        updatePhysics();

        // Generate obstacles and bonuses
        generateObjects();

        // Update object positions
        updateObjects();

        // Check collisions
        checkCollisions();

        // Check if player finished the race
        if (distance >= FINISH_LINE) {
            gameOver = true;
            score += 5000; // Bonus for finishing
        }

        // Repaint the game
        repaint();
    }

    private void updatePlayerPosition() {
        // Horizontal movement
        if (leftPressed && playerX > TRACK_CENTER - TRACK_WIDTH/2 + PLAYER_WIDTH/2) {
            playerX -= playerSpeed;
        }
        if (rightPressed && playerX < TRACK_CENTER + TRACK_WIDTH/2 - PLAYER_WIDTH/2) {
            playerX += playerSpeed;
        }

        // Jumping logic
        if (isJumping) {
            jumpHeight += 5;
            if (jumpHeight >= maxJumpHeight) {
                isJumping = false;
            }
        } else if (jumpHeight > 0) {
            jumpHeight -= 5;
        }
    }

    private void updatePhysics() {
        // Apply acceleration if sprinting
        if (upPressed && playerStamina > 0) {
            velocity += acceleration * 1.5;
            playerStamina -= 0.5;
        } else {
            velocity += acceleration;
        }

        // Apply friction
        velocity -= velocity * friction;

        // Cap velocity
        if (velocity > maxVelocity) {
            velocity = maxVelocity;
        }
        if (velocity < 3) { // Minimum speed
            velocity = 3;
        }

        // Update distance based on velocity
        distance += (int)velocity;
    }

    private void generateObjects() {
        // Generate obstacles
        if (random.nextInt(100) < 3) { // 3% chance per frame
            int x = TRACK_CENTER - TRACK_WIDTH/2 + random.nextInt(TRACK_WIDTH);
            int y = -OBSTACLE_HEIGHT;
            obstacles.add(new Obstacle(x, y));
        }

        // Generate bonuses less frequently
        if (random.nextInt(200) < 1 && playerStamina < 80) { // 0.5% chance per frame when stamina is low
            int x = TRACK_CENTER - TRACK_WIDTH/2 + random.nextInt(TRACK_WIDTH);
            int y = -OBSTACLE_HEIGHT;
            bonuses.add(new Bonus(x, y));
        }
    }

    private void updateObjects() {
        // Move obstacles down (player is moving up)
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle obstacle = obstacles.get(i);
            obstacle.y += velocity;

            // Remove if off screen
            if (obstacle.y > HEIGHT + OBSTACLE_HEIGHT) {
                obstacles.remove(i);
                i--;
                score += 10; // Small bonus for avoiding
            }
        }

        // Move bonuses down
        for (int i = 0; i < bonuses.size(); i++) {
            Bonus bonus = bonuses.get(i);
            bonus.y += velocity;

            // Remove if off screen
            if (bonus.y > HEIGHT + OBSTACLE_HEIGHT) {
                bonuses.remove(i);
                i--;
            }
        }
    }

    private void checkCollisions() {
        Rectangle playerRect = new Rectangle(
                playerX - PLAYER_WIDTH/2,
                playerY - PLAYER_HEIGHT/2 - jumpHeight,
                PLAYER_WIDTH,
                PLAYER_HEIGHT
        );

        // Check obstacle collisions
        for (Obstacle obstacle : obstacles) {
            Rectangle obstacleRect = new Rectangle(
                    obstacle.x - OBSTACLE_WIDTH/2,
                    obstacle.y - OBSTACLE_HEIGHT/2,
                    OBSTACLE_WIDTH,
                    OBSTACLE_HEIGHT
            );

            if (playerRect.intersects(obstacleRect)) {
                if (jumpHeight > maxJumpHeight * 0.8) {
                    // Jumped over the obstacle
                    score += 50;
                    obstacles.remove(obstacle);
                    break;
                } else {
                    // Collision
                    velocity *= 0.5; // Slow down
                    playerStamina -= 10;
                    score -= 20;
                    obstacles.remove(obstacle);
                    break;
                }
            }
        }

        // Check bonus collisions
        for (Bonus bonus : bonuses) {
            Rectangle bonusRect = new Rectangle(
                    bonus.x - OBSTACLE_WIDTH/2,
                    bonus.y - OBSTACLE_HEIGHT/2,
                    OBSTACLE_WIDTH,
                    OBSTACLE_HEIGHT
            );

            if (playerRect.intersects(bonusRect)) {
                playerStamina += 20;
                if (playerStamina > 100) playerStamina = 100;
                score += 100;
                bonuses.remove(bonus);
                break;
            }
        }

        // Check if stamina is depleted
        if (playerStamina <= 0) {
            playerStamina = 0;
            velocity *= 0.95; // Additional penalty when exhausted
        }

        // Check if player is too slow (game over condition)
        if (velocity < 2) {
            gameOver = true;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (!gameStarted && key == KeyEvent.VK_SPACE) {
            startGame();
            return;
        }

        if (gameOver && key == KeyEvent.VK_SPACE) {
            startGame();
            return;
        }

        if (key == KeyEvent.VK_LEFT) {
            leftPressed = true;
        }
        if (key == KeyEvent.VK_RIGHT) {
            rightPressed = true;
        }
        if (key == KeyEvent.VK_UP) {
            upPressed = true;
        }
        if (key == KeyEvent.VK_SPACE && !isJumping && jumpHeight == 0) {
            isJumping = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT) {
            leftPressed = false;
        }
        if (key == KeyEvent.VK_RIGHT) {
            rightPressed = false;
        }
        if (key == KeyEvent.VK_UP) {
            upPressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private class Obstacle {
        int x, y;

        Obstacle(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private class Bonus {
        int x, y;

        Bonus(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("High-Speed Speed Skating");
        SpeedSkatingGame game = new SpeedSkatingGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}