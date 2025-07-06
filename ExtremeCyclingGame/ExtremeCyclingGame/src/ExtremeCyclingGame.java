import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class ExtremeCyclingGame extends JFrame {
    private GamePanel gamePanel;
    private JLabel scoreLabel;
    private JLabel livesLabel;
    private JButton startButton;
    private JButton pauseButton;
    private boolean isRunning = false;
    private boolean isPaused = false;

    public ExtremeCyclingGame() {
        setTitle("Extreme Cycling Game");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Initialize components
        gamePanel = new GamePanel();
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 16));
        livesLabel = new JLabel("Lives: 3");
        livesLabel.setFont(new Font("Arial", Font.BOLD, 16));
        startButton = new JButton("Start Game");
        pauseButton = new JButton("Pause");
        pauseButton.setEnabled(false);

        // Layout
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(scoreLabel);
        controlPanel.add(livesLabel);

        // Add components to frame
        add(controlPanel, BorderLayout.NORTH);
        add(gamePanel, BorderLayout.CENTER);

        // Button listeners
        startButton.addActionListener(e -> startGame());
        pauseButton.addActionListener(e -> togglePause());

        // Keyboard input
        gamePanel.setFocusable(true);
        gamePanel.requestFocusInWindow();
    }

    private void startGame() {
        if (!isRunning) {
            gamePanel.startGame();
            startButton.setText("Restart Game");
            pauseButton.setEnabled(true);
            isRunning = true;
            gamePanel.requestFocusInWindow();
        } else {
            gamePanel.resetGame();
            scoreLabel.setText("Score: 0");
            livesLabel.setText("Lives: 3");
        }
    }

    private void togglePause() {
        isPaused = !isPaused;
        pauseButton.setText(isPaused ? "Resume" : "Pause");
        gamePanel.togglePause();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ExtremeCyclingGame game = new ExtremeCyclingGame();
            game.setVisible(true);
        });
    }

    class GamePanel extends JPanel {
        private static final int WIDTH = 800;
        private static final int HEIGHT = 600;
        private static final int GROUND_HEIGHT = 100;
        private static final int PLAYER_WIDTH = 40;
        private static final int PLAYER_HEIGHT = 60;
        private static final int OBSTACLE_WIDTH = 20;
        private static final int OBSTACLE_HEIGHT = 40;
        private static final int JUMP_HEIGHT = 100;
        private static final int GRAVITY = 2;
        private static final int SCROLL_SPEED = 5;

        private int playerX = 100;
        private int playerY = HEIGHT - GROUND_HEIGHT - PLAYER_HEIGHT;
        private int playerVelocityY = 0;
        private boolean isJumping = false;
        private int score = 0;
        private int lives = 3;
        private ArrayList<Obstacle> obstacles;
        private ArrayList<Terrain> terrain;
        private Timer timer;
        private Random random;
        private int backgroundX = 0;
        private int terrainOffset = 0;
        private boolean gameOver = false;

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            obstacles = new ArrayList<>();
            terrain = new ArrayList<>();
            random = new Random();
            initializeTerrain();

            timer = new Timer(16, e -> {
                if (isRunning && !isPaused && !gameOver) {
                    updateGame();
                    repaint();
                }
            });

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE && !isJumping && !gameOver) {
                        playerVelocityY = -25;
                        isJumping = true;
                    }
                    if (e.getKeyCode() == KeyEvent.VK_R && gameOver) {
                        resetGame();
                    }
                }
            });
        }

        private void initializeTerrain() {
            terrain.clear();
            int x = 0;
            while (x < WIDTH * 2) {
                int height = random.nextInt(50) + GROUND_HEIGHT;
                terrain.add(new Terrain(x, HEIGHT - height, 50, height));
                x += 50;
            }
        }

        public void startGame() {
            resetGame();
            timer.start();
        }

        public void togglePause() {
            if (isPaused) {
                timer.stop();
            } else {
                timer.start();
            }
        }

        public void resetGame() {
            playerX = 100;
            playerY = HEIGHT - GROUND_HEIGHT - PLAYER_HEIGHT;
            playerVelocityY = 0;
            isJumping = false;
            score = 0;
            lives = 3;
            obstacles.clear();
            initializeTerrain();
            backgroundX = 0;
            terrainOffset = 0;
            gameOver = false;
            scoreLabel.setText("Score: 0");
            livesLabel.setText("Lives: 3");
            timer.restart();
        }

        private void updateGame() {
            // Update player position
            playerY += playerVelocityY;
            playerVelocityY += GRAVITY;

            // Check ground collision
            int groundY = HEIGHT - getTerrainHeight(playerX + PLAYER_WIDTH / 2);
            if (playerY > groundY - PLAYER_HEIGHT) {
                playerY = groundY - PLAYER_HEIGHT;
                playerVelocityY = 0;
                isJumping = false;
            }

            // Scroll background and terrain
            backgroundX -= SCROLL_SPEED;
            if (backgroundX <= -WIDTH) {
                backgroundX = 0;
            }
            terrainOffset += SCROLL_SPEED;

            // Update terrain
            if (terrainOffset >= 50) {
                terrain.remove(0);
                int lastX = terrain.get(terrain.size() - 1).x;
                int newHeight = random.nextInt(50) + GROUND_HEIGHT;
                terrain.add(new Terrain(lastX + 50, HEIGHT - newHeight, 50, newHeight));
                terrainOffset = 0;
            }

            // Spawn obstacles
            if (random.nextInt(100) < 2 && obstacles.size() < 5) {
                obstacles.add(new Obstacle(WIDTH, HEIGHT - GROUND_HEIGHT - OBSTACLE_HEIGHT, OBSTACLE_WIDTH, OBSTACLE_HEIGHT));
            }

            // Update obstacles
            for (int i = obstacles.size() - 1; i >= 0; i--) {
                Obstacle obs = obstacles.get(i);
                obs.x -= SCROLL_SPEED;
                if (obs.x + OBSTACLE_WIDTH < 0) {
                    obstacles.remove(i);
                    score += 10;
                    scoreLabel.setText("Score: " + score);
                }
            }

            // Collision detection
            Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
            for (Obstacle obs : obstacles) {
                Rectangle obsRect = new Rectangle(obs.x, obs.y, obs.width, obs.height);
                if (playerRect.intersects(obsRect)) {
                    lives--;
                    livesLabel.setText("Lives: " + lives);
                    obstacles.remove(obs);
                    if (lives <= 0) {
                        gameOver = true;
                        timer.stop();
                    }
                    break;
                }
            }
        }

        private int getTerrainHeight(int x) {
            for (Terrain t : terrain) {
                if (x >= t.x && x <= t.x + t.width) {
                    return t.height;
                }
            }
            return GROUND_HEIGHT;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw background
            g2d.setColor(new Color(135, 206, 235)); // Sky blue
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            // Draw scrolling background
            g2d.setColor(new Color(34, 139, 34)); // Forest green
            g2d.fillRect(backgroundX, HEIGHT - GROUND_HEIGHT, WIDTH, GROUND_HEIGHT);
            g2d.fillRect(backgroundX + WIDTH, HEIGHT - GROUND_HEIGHT, WIDTH, GROUND_HEIGHT);

            // Draw terrain
            g2d.setColor(new Color(139, 69, 19)); // Brown
            for (Terrain t : terrain) {
                g2d.fillRect(t.x, t.y, t.width, t.height);
            }

            // Draw player
            g2d.setColor(Color.RED);
            g2d.fillRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(playerX + 5, playerY + PLAYER_HEIGHT - 15, 10, 10); // Front wheel
            g2d.fillOval(playerX + 25, playerY + PLAYER_HEIGHT - 15, 10, 10); // Rear wheel
            g2d.drawLine(playerX + 10, playerY + PLAYER_HEIGHT - 10, playerX + 30, playerY + PLAYER_HEIGHT - 10); // Frame
            g2d.drawLine(playerX + 20, playerY + PLAYER_HEIGHT - 10, playerX + 20, playerY + 20); // Seat tube
            g2d.drawLine(playerX + 20, playerY + 20, playerX + 30, playerY); // Handlebars

            // Draw obstacles
            g2d.setColor(Color.GRAY);
            for (Obstacle obs : obstacles) {
                g2d.fillRect(obs.x, obs.y, obs.width, obs.height);
            }

            // Draw score and lives
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Score: " + score, 10, 20);
            g2d.drawString("Lives: " + lives, 10, 40);

            // Draw game over screen
            if (gameOver) {
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRect(0, 0, WIDTH, HEIGHT);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 32));
                g2d.drawString("Game Over!", WIDTH / 2 - 80, HEIGHT / 2 - 20);
                g2d.setFont(new Font("Arial", Font.PLAIN, 20));
                g2d.drawString("Final Score: " + score, WIDTH / 2 - 60, HEIGHT / 2 + 20);
                g2d.drawString("Press R to Restart", WIDTH / 2 - 80, HEIGHT / 2 + 60);
            }
        }

        class Obstacle {
            int x, y, width, height;

            Obstacle(int x, int y, int width, int height) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }
        }

        class Terrain {
            int x, y, width, height;

            Terrain(int x, int y, int width, int height) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }
        }
    }
}