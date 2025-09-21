import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class RollerbladingAdventure extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int GROUND_LEVEL = 500;
    private static final int PLAYER_SIZE = 40;
    private static final int OBSTACLE_WIDTH = 30;
    private static final int OBSTACLE_HEIGHT = 50;
    private static final int ITEM_SIZE = 25;
    private static final int GAME_SPEED = 15;

    // Game states
    private enum GameState { START, PLAYING, PAUSED, GAME_OVER }
    private GameState gameState = GameState.START;

    // Player properties
    private int playerX = 100;
    private int playerY = GROUND_LEVEL - PLAYER_SIZE;
    private int playerSpeed = 5;
    private int jumpHeight = 0;
    private boolean isJumping = false;
    private boolean isCrouching = false;
    private int playerScore = 0;
    private int playerLives = 3;

    // Animation properties
    private int frameCount = 0;
    private int playerFrame = 0;
    private int backgroundOffset = 0;

    // Game objects
    private List<Rectangle> obstacles;
    private List<Rectangle> items;
    private List<Rectangle> backgrounds;

    // Timer for game loop
    private Timer gameTimer;

    public RollerbladingAdventure() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.CYAN);
        setFocusable(true);
        addKeyListener(this);

        obstacles = new ArrayList<>();
        items = new ArrayList<>();
        backgrounds = new ArrayList<>();

        // Initialize background elements
        for (int i = 0; i < 5; i++) {
            backgrounds.add(new Rectangle(i * 200, GROUND_LEVEL - 50, 200, 50));
        }

        gameTimer = new Timer(1000 / GAME_SPEED, this);
        gameTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw sky
        g.setColor(new Color(135, 206, 235));
        g.fillRect(0, 0, WIDTH, GROUND_LEVEL);

        // Draw sun
        g.setColor(Color.YELLOW);
        g.fillOval(700, 50, 60, 60);

        // Draw clouds
        g.setColor(Color.WHITE);
        for (int i = 0; i < 5; i++) {
            int cloudX = (i * 200 + backgroundOffset / 2) % (WIDTH + 400) - 200;
            g.fillOval(cloudX, 80 + i * 20, 60, 30);
            g.fillOval(cloudX + 20, 70 + i * 20, 70, 40);
            g.fillOval(cloudX + 50, 80 + i * 20, 60, 30);
        }

        // Draw ground
        g.setColor(new Color(34, 139, 34));
        g.fillRect(0, GROUND_LEVEL, WIDTH, HEIGHT - GROUND_LEVEL);

        // Draw road markings
        g.setColor(Color.YELLOW);
        for (int i = -1; i < WIDTH / 50 + 1; i++) {
            int lineX = (i * 50 - backgroundOffset / 3) % (WIDTH + 100) - 50;
            g.fillRect(lineX, GROUND_LEVEL + 20, 30, 5);
        }

        // Draw background elements (trees, buildings, etc.)
        g.setColor(new Color(139, 69, 19));
        for (Rectangle bg : backgrounds) {
            int treeX = (bg.x - backgroundOffset / 4) % (WIDTH + 400) - 100;
            if (treeX > -100 && treeX < WIDTH) {
                // Draw tree trunk
                g.fillRect(treeX + 15, GROUND_LEVEL - 100, 10, 100);
                // Draw tree leaves
                g.setColor(new Color(0, 100, 0));
                g.fillOval(treeX, GROUND_LEVEL - 150, 40, 50);
            }
        }

        // Draw items (coins, power-ups)
        g.setColor(Color.YELLOW);
        for (Rectangle item : items) {
            int itemX = item.x - backgroundOffset;
            if (itemX > -ITEM_SIZE && itemX < WIDTH) {
                g.fillOval(itemX, item.y, ITEM_SIZE, ITEM_SIZE);
                g.setColor(Color.ORANGE);
                g.fillOval(itemX + 5, item.y + 5, ITEM_SIZE - 10, ITEM_SIZE - 10);
            }
        }

        // Draw obstacles
        g.setColor(Color.RED);
        for (Rectangle obstacle : obstacles) {
            int obstacleX = obstacle.x - backgroundOffset;
            if (obstacleX > -OBSTACLE_WIDTH && obstacleX < WIDTH) {
                if (obstacle.height > OBSTACLE_HEIGHT) {
                    // High obstacle (jump over)
                    g.fillRect(obstacleX, obstacle.y, OBSTACLE_WIDTH, obstacle.height);
                    g.setColor(new Color(100, 0, 0));
                    for (int i = 0; i < obstacle.height; i += 10) {
                        g.drawLine(obstacleX, obstacle.y + i, obstacleX + OBSTACLE_WIDTH, obstacle.y + i);
                    }
                } else {
                    // Low obstacle (crouch under)
                    g.fillRect(obstacleX, obstacle.y, OBSTACLE_WIDTH, obstacle.height);
                    g.setColor(new Color(100, 0, 0));
                    for (int i = 0; i < OBSTACLE_WIDTH; i += 5) {
                        g.drawLine(obstacleX + i, obstacle.y, obstacleX + i, obstacle.y + obstacle.height);
                    }
                }
            }
        }

        // Draw player
        drawPlayer(g);

        // Draw score and lives
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + playerScore, 20, 30);
        g.drawString("Lives: " + playerLives, 20, 60);

        // Draw game state messages
        if (gameState == GameState.START) {
            drawStartScreen(g);
        } else if (gameState == GameState.PAUSED) {
            drawPauseScreen(g);
        } else if (gameState == GameState.GAME_OVER) {
            drawGameOverScreen(g);
        }
    }

    private void drawPlayer(Graphics g) {
        int drawY = playerY;
        int drawHeight = PLAYER_SIZE;

        if (isCrouching) {
            drawY = playerY + PLAYER_SIZE / 2;
            drawHeight = PLAYER_SIZE / 2;
        }

        // Draw rollerblader body
        g.setColor(Color.BLUE);
        g.fillOval(playerX, drawY, PLAYER_SIZE / 2, PLAYER_SIZE / 2);

        // Draw rollerblader legs based on animation frame
        g.setColor(new Color(50, 50, 200));
        if (isJumping) {
            // Jumping pose
            g.drawLine(playerX + PLAYER_SIZE / 4, drawY + PLAYER_SIZE / 2,
                    playerX, drawY + PLAYER_SIZE);
            g.drawLine(playerX + PLAYER_SIZE / 4, drawY + PLAYER_SIZE / 2,
                    playerX + PLAYER_SIZE / 2, drawY + PLAYER_SIZE);
        } else if (isCrouching) {
            // Crouching pose
            g.fillRect(playerX, drawY + drawHeight - 10, PLAYER_SIZE / 2, 10);
        } else {
            // Skating animation
            if (playerFrame % 2 == 0) {
                g.drawLine(playerX + PLAYER_SIZE / 4, drawY + PLAYER_SIZE / 2,
                        playerX - 5, drawY + PLAYER_SIZE);
                g.drawLine(playerX + PLAYER_SIZE / 4, drawY + PLAYER_SIZE / 2,
                        playerX + PLAYER_SIZE / 2 + 5, drawY + PLAYER_SIZE);
            } else {
                g.drawLine(playerX + PLAYER_SIZE / 4, drawY + PLAYER_SIZE / 2,
                        playerX + 5, drawY + PLAYER_SIZE);
                g.drawLine(playerX + PLAYER_SIZE / 4, drawY + PLAYER_SIZE / 2,
                        playerX + PLAYER_SIZE / 2 - 5, drawY + PLAYER_SIZE);
            }
        }

        // Draw rollerblades
        g.setColor(Color.BLACK);
        g.fillOval(playerX - 5, drawY + PLAYER_SIZE - 5, 15, 10);
        g.fillOval(playerX + PLAYER_SIZE / 2 - 5, drawY + PLAYER_SIZE - 5, 15, 10);

        // Draw wheels
        g.setColor(Color.RED);
        g.fillOval(playerX - 2, drawY + PLAYER_SIZE - 3, 8, 8);
        g.fillOval(playerX + PLAYER_SIZE / 2 - 2, drawY + PLAYER_SIZE - 3, 8, 8);
    }

    private void drawStartScreen(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Rollerblading Adventure", WIDTH / 2 - 200, HEIGHT / 2 - 50);

        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("Press SPACE to start", WIDTH / 2 - 100, HEIGHT / 2 + 20);
        g.drawString("Use UP to jump, DOWN to crouch", WIDTH / 2 - 150, HEIGHT / 2 + 60);
        g.drawString("Avoid obstacles and collect items", WIDTH / 2 - 160, HEIGHT / 2 + 100);
    }

    private void drawPauseScreen(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Game Paused", WIDTH / 2 - 120, HEIGHT / 2 - 20);

        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("Press P to resume", WIDTH / 2 - 80, HEIGHT / 2 + 30);
    }

    private void drawGameOverScreen(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Game Over", WIDTH / 2 - 100, HEIGHT / 2 - 50);

        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.drawString("Final Score: " + playerScore, WIDTH / 2 - 100, HEIGHT / 2);

        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("Press R to restart", WIDTH / 2 - 80, HEIGHT / 2 + 50);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.PLAYING) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        frameCount++;

        // Update animation frame
        if (frameCount % 5 == 0) {
            playerFrame++;
        }

        // Update background
        backgroundOffset += playerSpeed;

        // Handle jumping
        if (isJumping) {
            jumpHeight += 5;
            playerY = GROUND_LEVEL - PLAYER_SIZE - jumpHeight;

            if (jumpHeight > 100) {
                isJumping = false;
            }
        } else if (playerY < GROUND_LEVEL - PLAYER_SIZE) {
            // Falling
            jumpHeight -= 5;
            playerY = GROUND_LEVEL - PLAYER_SIZE - jumpHeight;
        } else {
            // On ground
            playerY = GROUND_LEVEL - PLAYER_SIZE;
            jumpHeight = 0;
        }

        // Generate new obstacles and items
        if (frameCount % 60 == 0) {
            // Randomly decide whether to create an obstacle or item
            if (Math.random() < 0.7) {
                // Create obstacle (70% chance)
                int obstacleType = (int) (Math.random() * 2);
                int obstacleHeight = obstacleType == 0 ? OBSTACLE_HEIGHT : OBSTACLE_HEIGHT * 2;
                int obstacleY = GROUND_LEVEL - obstacleHeight;
                obstacles.add(new Rectangle(WIDTH + backgroundOffset, obstacleY, OBSTACLE_WIDTH, obstacleHeight));
            } else {
                // Create item (30% chance)
                int itemY = GROUND_LEVEL - ITEM_SIZE - (int) (Math.random() * 100);
                items.add(new Rectangle(WIDTH + backgroundOffset, itemY, ITEM_SIZE, ITEM_SIZE));
            }
        }

        // Check collisions with obstacles
        Rectangle playerBounds = new Rectangle(playerX, playerY, PLAYER_SIZE / 2,
                isCrouching ? PLAYER_SIZE / 2 : PLAYER_SIZE);

        Iterator<Rectangle> obstacleIter = obstacles.iterator();
        while (obstacleIter.hasNext()) {
            Rectangle obstacle = obstacleIter.next();
            int obstacleX = obstacle.x - backgroundOffset;

            // Remove obstacles that are off-screen
            if (obstacleX < -OBSTACLE_WIDTH) {
                obstacleIter.remove();
                continue;
            }

            // Check collision
            Rectangle obstacleBounds = new Rectangle(obstacleX, obstacle.y, obstacle.width, obstacle.height);
            if (playerBounds.intersects(obstacleBounds)) {
                playerLives--;
                obstacleIter.remove();

                if (playerLives <= 0) {
                    gameState = GameState.GAME_OVER;
                }
            }
        }

        // Check collection of items
        Iterator<Rectangle> itemIter = items.iterator();
        while (itemIter.hasNext()) {
            Rectangle item = itemIter.next();
            int itemX = item.x - backgroundOffset;

            // Remove items that are off-screen
            if (itemX < -ITEM_SIZE) {
                itemIter.remove();
                continue;
            }

            // Check collection
            Rectangle itemBounds = new Rectangle(itemX, item.y, item.width, item.height);
            if (playerBounds.intersects(itemBounds)) {
                playerScore += 10;
                itemIter.remove();
            }
        }

        // Increase score over time
        if (frameCount % 30 == 0) {
            playerScore++;
        }

        // Increase difficulty over time
        if (frameCount % 500 == 0) {
            playerSpeed++;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (gameState == GameState.START) {
            if (key == KeyEvent.VK_SPACE) {
                gameState = GameState.PLAYING;
            }
        } else if (gameState == GameState.PLAYING) {
            if (key == KeyEvent.VK_UP && !isJumping && playerY >= GROUND_LEVEL - PLAYER_SIZE) {
                isJumping = true;
            } else if (key == KeyEvent.VK_DOWN) {
                isCrouching = true;
            } else if (key == KeyEvent.VK_P) {
                gameState = GameState.PAUSED;
            }
        } else if (gameState == GameState.PAUSED) {
            if (key == KeyEvent.VK_P) {
                gameState = GameState.PLAYING;
            }
        } else if (gameState == GameState.GAME_OVER) {
            if (key == KeyEvent.VK_R) {
                resetGame();
                gameState = GameState.PLAYING;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_DOWN) {
            isCrouching = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    private void resetGame() {
        playerScore = 0;
        playerLives = 3;
        playerSpeed = 5;
        obstacles.clear();
        items.clear();
        backgroundOffset = 0;
        frameCount = 0;
        playerX = 100;
        playerY = GROUND_LEVEL - PLAYER_SIZE;
        isJumping = false;
        isCrouching = false;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Rollerblading Adventure");
        RollerbladingAdventure game = new RollerbladingAdventure();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}