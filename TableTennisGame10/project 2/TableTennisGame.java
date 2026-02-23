import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class TableTennisGame extends JFrame {
    private GamePanel gamePanel;

    public TableTennisGame() {
        setTitle("Table Tennis Game - Ultimate Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TableTennisGame());
    }
}

class GamePanel extends JPanel implements Runnable {
    // Screen dimensions
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;
    private static final int PADDLE_WIDTH = 15;
    private static final int PADDLE_HEIGHT = 100;
    private static final int BALL_SIZE = 20;

    // Game objects
    private Paddle player1;
    private Paddle player2;
    private Ball ball;
    private ArrayList<Particle> particles;
    private ArrayList<PowerUp> powerUps;

    // Game state
    private Thread gameThread;
    private boolean running;
    private GameState gameState;
    private int player1Score;
    private int player2Score;
    private int winningScore = 11;

    // Difficulty levels
    private DifficultyLevel difficulty;
    private double aiSpeed;
    private double aiReactionDelay;

    // Animation and effects
    private ArrayList<TrailPoint> ballTrail;
    private int maxTrailLength = 15;
    private boolean showParticles = true;
    private boolean showTrail = true;

    // Menu selection
    private int menuSelection = 0;
    private int difficultySelection = 1;

    // Power-up system
    private long lastPowerUpSpawn = 0;
    private static final long POWER_UP_SPAWN_INTERVAL = 15000; // 15 seconds

    // Visual effects
    private Rectangle2D courtLine;
    private Color backgroundColor = new Color(20, 30, 50);
    private Color courtColor = new Color(50, 100, 150);
    private long gameStartTime;
    private int flashTimer = 0;
    private boolean scoreFlash = false;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(backgroundColor);
        setFocusable(true);

        // Initialize game objects
        player1 = new Paddle(30, HEIGHT / 2 - PADDLE_HEIGHT / 2, PADDLE_WIDTH, PADDLE_HEIGHT, Color.CYAN);
        player2 = new Paddle(WIDTH - 30 - PADDLE_WIDTH, HEIGHT / 2 - PADDLE_HEIGHT / 2, PADDLE_WIDTH, PADDLE_HEIGHT, Color.MAGENTA);
        ball = new Ball(WIDTH / 2 - BALL_SIZE / 2, HEIGHT / 2 - BALL_SIZE / 2, BALL_SIZE);

        particles = new ArrayList<>();
        powerUps = new ArrayList<>();
        ballTrail = new ArrayList<>();

        courtLine = new Rectangle2D.Double(WIDTH / 2 - 2, 0, 4, HEIGHT);

        gameState = GameState.MENU;
        difficulty = DifficultyLevel.MEDIUM;
        setDifficulty(difficulty);

        // Add keyboard controls
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                handleKeyRelease(e.getKeyCode());
            }
        });

        // Start game loop
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    private void handleKeyPress(int keyCode) {
        switch (gameState) {
            case MENU:
                if (keyCode == KeyEvent.VK_UP) {
                    menuSelection = Math.max(0, menuSelection - 1);
                } else if (keyCode == KeyEvent.VK_DOWN) {
                    menuSelection = Math.min(2, menuSelection + 1);
                } else if (keyCode == KeyEvent.VK_ENTER) {
                    handleMenuSelection();
                }
                break;

            case DIFFICULTY_SELECT:
                if (keyCode == KeyEvent.VK_UP) {
                    difficultySelection = Math.max(0, difficultySelection - 1);
                } else if (keyCode == KeyEvent.VK_DOWN) {
                    difficultySelection = Math.min(2, difficultySelection + 1);
                } else if (keyCode == KeyEvent.VK_ENTER) {
                    setDifficulty(DifficultyLevel.values()[difficultySelection]);
                    resetGame();
                    gameState = GameState.PLAYING;
                    gameStartTime = System.currentTimeMillis();
                } else if (keyCode == KeyEvent.VK_ESCAPE) {
                    gameState = GameState.MENU;
                }
                break;

            case PLAYING:
                if (keyCode == KeyEvent.VK_W) {
                    player1.setUp(true);
                } else if (keyCode == KeyEvent.VK_S) {
                    player1.setDown(true);
                } else if (keyCode == KeyEvent.VK_ESCAPE) {
                    gameState = GameState.PAUSED;
                }
                break;

            case PAUSED:
                if (keyCode == KeyEvent.VK_ESCAPE) {
                    gameState = GameState.PLAYING;
                } else if (keyCode == KeyEvent.VK_Q) {
                    gameState = GameState.MENU;
                    resetGame();
                }
                break;

            case GAME_OVER:
                if (keyCode == KeyEvent.VK_ENTER) {
                    gameState = GameState.MENU;
                    resetGame();
                }
                break;
        }
    }

    private void handleKeyRelease(int keyCode) {
        if (gameState == GameState.PLAYING) {
            if (keyCode == KeyEvent.VK_W) {
                player1.setUp(false);
            } else if (keyCode == KeyEvent.VK_S) {
                player1.setDown(false);
            }
        }
    }

    private void handleMenuSelection() {
        switch (menuSelection) {
            case 0: // Start Game
                gameState = GameState.DIFFICULTY_SELECT;
                break;
            case 1: // Instructions
                gameState = GameState.INSTRUCTIONS;
                break;
            case 2: // Exit
                System.exit(0);
                break;
        }
    }

    private void setDifficulty(DifficultyLevel level) {
        difficulty = level;
        switch (level) {
            case EASY:
                aiSpeed = 3.5;
                aiReactionDelay = 0.15;
                break;
            case MEDIUM:
                aiSpeed = 5.0;
                aiReactionDelay = 0.1;
                break;
            case HARD:
                aiSpeed = 7.0;
                aiReactionDelay = 0.05;
                break;
        }
    }

    private void resetGame() {
        player1Score = 0;
        player2Score = 0;
        player1.reset();
        player2.reset();
        ball.reset();
        particles.clear();
        powerUps.clear();
        ballTrail.clear();
        lastPowerUpSpawn = System.currentTimeMillis();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60.0; // 60 FPS
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        if (gameState == GameState.PLAYING) {
            // Update paddles
            player1.update();
            updateAI();

            // Update ball
            ball.update();

            // Add trail point
            if (showTrail) {
                ballTrail.add(new TrailPoint(ball.x + ball.size / 2, ball.y + ball.size / 2));
                if (ballTrail.size() > maxTrailLength) {
                    ballTrail.remove(0);
                }
            }

            // Check collisions
            checkPaddleCollision(player1);
            checkPaddleCollision(player2);
            checkWallCollision();
            checkPowerUpCollision();

            // Update particles
            updateParticles();

            // Update power-ups
            updatePowerUps();

            // Spawn power-ups
            spawnPowerUps();

            // Check scoring
            checkScoring();

            // Update flash effect
            if (flashTimer > 0) {
                flashTimer--;
            }
        }
    }

    private void updateAI() {
        double paddleCenter = player2.y + player2.height / 2;
        double ballCenter = ball.y + ball.size / 2;

        // AI prediction with difficulty-based reaction
        double targetY = ballCenter;
        if (ball.dx > 0) { // Ball moving towards AI
            targetY = ballCenter + (ball.dy * aiReactionDelay * 60);
        }

        // Move AI paddle
        if (paddleCenter < targetY - 10) {
            player2.y += aiSpeed;
        } else if (paddleCenter > targetY + 10) {
            player2.y -= aiSpeed;
        }

        // Keep AI paddle in bounds
        player2.y = Math.max(0, Math.min(HEIGHT - player2.height, player2.y));
    }

    private void checkPaddleCollision(Paddle paddle) {
        if (ball.getBounds().intersects(paddle.getBounds())) {
            // Calculate hit position (-1 to 1, where 0 is center)
            double hitPos = ((ball.y + ball.size / 2) - (paddle.y + paddle.height / 2)) / (paddle.height / 2);

            // Reverse horizontal direction
            ball.dx *= -1.05; // Slight speed increase

            // Adjust vertical direction based on hit position
            ball.dy = hitPos * 8;

            // Move ball out of paddle
            if (paddle == player1) {
                ball.x = paddle.x + paddle.width + 1;
            } else {
                ball.x = paddle.x - ball.size - 1;
            }

            // Create particles
            if (showParticles) {
                createHitParticles(ball.x + ball.size / 2, ball.y + ball.size / 2, paddle.color);
            }

            // Add power-up effect
            if (paddle.powerUpActive) {
                ball.dx *= 1.2;
                paddle.powerUpDuration--;
                if (paddle.powerUpDuration <= 0) {
                    paddle.powerUpActive = false;
                }
            }
        }
    }

    private void checkWallCollision() {
        if (ball.y <= 0 || ball.y + ball.size >= HEIGHT) {
            ball.dy *= -1;
            ball.y = Math.max(0, Math.min(HEIGHT - ball.size, ball.y));

            if (showParticles) {
                createHitParticles(ball.x + ball.size / 2, ball.y <= 0 ? 0 : HEIGHT, Color.WHITE);
            }
        }
    }

    private void checkPowerUpCollision() {
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp powerUp = powerUps.get(i);
            if (ball.getBounds().intersects(powerUp.getBounds())) {
                applyPowerUp(powerUp);
                powerUps.remove(i);
                createPowerUpParticles(powerUp.x, powerUp.y);
            }
        }
    }

    private void applyPowerUp(PowerUp powerUp) {
        switch (powerUp.type) {
            case SPEED_BOOST:
                if (ball.dx > 0) {
                    player2.powerUpActive = true;
                    player2.powerUpDuration = 180; // 3 seconds at 60 FPS
                } else {
                    player1.powerUpActive = true;
                    player1.powerUpDuration = 180;
                }
                break;
            case ENLARGE_PADDLE:
                if (ball.dx > 0) {
                    player2.height = Math.min(200, player2.height + 30);
                } else {
                    player1.height = Math.min(200, player1.height + 30);
                }
                break;
            case SLOW_BALL:
                ball.dx *= 0.7;
                ball.dy *= 0.7;
                break;
        }
    }

    private void spawnPowerUps() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPowerUpSpawn > POWER_UP_SPAWN_INTERVAL && powerUps.isEmpty()) {
            Random rand = new Random();
            int x = WIDTH / 4 + rand.nextInt(WIDTH / 2);
            int y = 50 + rand.nextInt(HEIGHT - 100);
            PowerUpType type = PowerUpType.values()[rand.nextInt(PowerUpType.values().length)];
            powerUps.add(new PowerUp(x, y, type));
            lastPowerUpSpawn = currentTime;
        }
    }

    private void updatePowerUps() {
        for (PowerUp powerUp : powerUps) {
            powerUp.update();
        }
    }

    private void checkScoring() {
        if (ball.x < 0) {
            player2Score++;
            scoreFlash = true;
            flashTimer = 30;
            ball.reset();
            ballTrail.clear();
            createScoreParticles(WIDTH / 4, HEIGHT / 2, player2.color);
            resetPaddleSizes();
        } else if (ball.x > WIDTH) {
            player1Score++;
            scoreFlash = true;
            flashTimer = 30;
            ball.reset();
            ballTrail.clear();
            createScoreParticles(3 * WIDTH / 4, HEIGHT / 2, player1.color);
            resetPaddleSizes();
        }

        if (player1Score >= winningScore || player2Score >= winningScore) {
            gameState = GameState.GAME_OVER;
        }
    }

    private void resetPaddleSizes() {
        player1.height = PADDLE_HEIGHT;
        player2.height = PADDLE_HEIGHT;
        player1.powerUpActive = false;
        player2.powerUpActive = false;
    }

    private void createHitParticles(double x, double y, Color color) {
        Random rand = new Random();
        for (int i = 0; i < 15; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double speed = 2 + rand.nextDouble() * 3;
            particles.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed, color));
        }
    }

    private void createScoreParticles(double x, double y, Color color) {
        Random rand = new Random();
        for (int i = 0; i < 50; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double speed = 3 + rand.nextDouble() * 5;
            particles.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed, color));
        }
    }

    private void createPowerUpParticles(double x, double y) {
        Random rand = new Random();
        for (int i = 0; i < 30; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double speed = 2 + rand.nextDouble() * 4;
            Color color = new Color(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255));
            particles.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed, color));
        }
    }

    private void updateParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.isDead()) {
                particles.remove(i);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (gameState) {
            case MENU:
                drawMenu(g2d);
                break;
            case DIFFICULTY_SELECT:
                drawDifficultySelect(g2d);
                break;
            case INSTRUCTIONS:
                drawInstructions(g2d);
                break;
            case PLAYING:
                drawGame(g2d);
                break;
            case PAUSED:
                drawGame(g2d);
                drawPauseOverlay(g2d);
                break;
            case GAME_OVER:
                drawGame(g2d);
                drawGameOver(g2d);
                break;
        }
    }

    private void drawMenu(Graphics2D g2d) {
        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String title = "TABLE TENNIS";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, 150);

        // Subtitle
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String subtitle = "Ultimate Edition";
        fm = g2d.getFontMetrics();
        g2d.drawString(subtitle, (WIDTH - fm.stringWidth(subtitle)) / 2, 200);

        // Menu options
        String[] options = {"Start Game", "Instructions", "Exit"};
        g2d.setFont(new Font("Arial", Font.PLAIN, 30));

        for (int i = 0; i < options.length; i++) {
            if (i == menuSelection) {
                g2d.setColor(Color.YELLOW);
                g2d.fillRect(WIDTH / 2 - 150, 300 + i * 60 - 5, 300, 50);
                g2d.setColor(Color.BLACK);
            } else {
                g2d.setColor(Color.WHITE);
            }
            fm = g2d.getFontMetrics();
            g2d.drawString(options[i], (WIDTH - fm.stringWidth(options[i])) / 2, 300 + i * 60 + 30);
        }

        // Controls hint
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        String hint = "Use Arrow Keys to navigate, Enter to select";
        fm = g2d.getFontMetrics();
        g2d.drawString(hint, (WIDTH - fm.stringWidth(hint)) / 2, HEIGHT - 50);
    }

    private void drawDifficultySelect(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        String title = "SELECT DIFFICULTY";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, 150);

        String[] difficulties = {"Easy", "Medium", "Hard"};
        String[] descriptions = {
            "Relaxed pace, perfect for beginners",
            "Balanced challenge for casual players",
            "Fast and challenging for experts"
        };

        g2d.setFont(new Font("Arial", Font.PLAIN, 30));

        for (int i = 0; i < difficulties.length; i++) {
            if (i == difficultySelection) {
                g2d.setColor(Color.CYAN);
                g2d.fillRect(WIDTH / 2 - 200, 250 + i * 100 - 5, 400, 80);
                g2d.setColor(Color.BLACK);
            } else {
                g2d.setColor(Color.WHITE);
            }
            fm = g2d.getFontMetrics();
            g2d.drawString(difficulties[i], (WIDTH - fm.stringWidth(difficulties[i])) / 2, 250 + i * 100 + 25);

            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            fm = g2d.getFontMetrics();
            if (i == difficultySelection) {
                g2d.drawString(descriptions[i], (WIDTH - fm.stringWidth(descriptions[i])) / 2, 250 + i * 100 + 50);
            }
            g2d.setFont(new Font("Arial", Font.PLAIN, 30));
        }

        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        String hint = "Press ESC to go back";
        fm = g2d.getFontMetrics();
        g2d.drawString(hint, (WIDTH - fm.stringWidth(hint)) / 2, HEIGHT - 50);
    }

    private void drawInstructions(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        String title = "HOW TO PLAY";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, 100);

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        String[] instructions = {
            "Controls:",
            "  W - Move paddle up",
            "  S - Move paddle down",
            "  ESC - Pause game / Return to menu",
            "",
            "Gameplay:",
            "  - First to 11 points wins",
            "  - Ball speed increases after each hit",
            "  - Power-ups appear randomly during play",
            "",
            "Power-ups:",
            "  Speed Boost - Increase hitting power",
            "  Enlarge Paddle - Make your paddle bigger",
            "  Slow Ball - Reduce ball speed",
            "",
            "Press ESC to return to menu"
        };

        int y = 180;
        for (String line : instructions) {
            g2d.drawString(line, 100, y);
            y += 30;
        }
    }

    private void drawGame(Graphics2D g2d) {
        // Draw court
        g2d.setColor(courtColor);
        for (int i = 0; i < HEIGHT; i += 40) {
            g2d.fillRect(WIDTH / 2 - 2, i, 4, 20);
        }

        // Draw ball trail
        if (showTrail) {
            for (int i = 0; i < ballTrail.size(); i++) {
                TrailPoint tp = ballTrail.get(i);
                float alpha = (float) i / ballTrail.size() * 0.5f;
                g2d.setColor(new Color(1f, 1f, 1f, alpha));
                int size = (int) (BALL_SIZE * alpha);
                g2d.fillOval((int) tp.x - size / 2, (int) tp.y - size / 2, size, size);
            }
        }

        // Draw particles
        for (Particle p : particles) {
            p.draw(g2d);
        }

        // Draw power-ups
        for (PowerUp powerUp : powerUps) {
            powerUp.draw(g2d);
        }

        // Draw paddles
        player1.draw(g2d);
        player2.draw(g2d);

        // Draw ball
        ball.draw(g2d);

        // Draw scores
        g2d.setColor(scoreFlash && flashTimer > 0 ? Color.YELLOW : Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString(String.valueOf(player1Score), WIDTH / 4, 60);
        g2d.drawString(String.valueOf(player2Score), 3 * WIDTH / 4, 60);

        // Draw difficulty indicator
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Difficulty: " + difficulty.name(), 10, HEIGHT - 10);
    }

    private void drawPauseOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String text = "PAUSED";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, (WIDTH - fm.stringWidth(text)) / 2, HEIGHT / 2 - 50);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        text = "Press ESC to resume";
        fm = g2d.getFontMetrics();
        g2d.drawString(text, (WIDTH - fm.stringWidth(text)) / 2, HEIGHT / 2 + 20);

        text = "Press Q to quit to menu";
        fm = g2d.getFontMetrics();
        g2d.drawString(text, (WIDTH - fm.stringWidth(text)) / 2, HEIGHT / 2 + 60);
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        String winner = player1Score >= winningScore ? "PLAYER WINS!" : "AI WINS!";
        Color winnerColor = player1Score >= winningScore ? player1.color : player2.color;

        g2d.setColor(winnerColor);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(winner, (WIDTH - fm.stringWidth(winner)) / 2, HEIGHT / 2 - 50);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 30));
        String score = player1Score + " - " + player2Score;
        fm = g2d.getFontMetrics();
        g2d.drawString(score, (WIDTH - fm.stringWidth(score)) / 2, HEIGHT / 2 + 20);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String text = "Press ENTER to return to menu";
        fm = g2d.getFontMetrics();
        g2d.drawString(text, (WIDTH - fm.stringWidth(text)) / 2, HEIGHT / 2 + 80);
    }
}

class Paddle {
    double x, y, width, height;
    Color color;
    boolean movingUp, movingDown;
    double speed = 6.0;
    boolean powerUpActive = false;
    int powerUpDuration = 0;

    public Paddle(double x, double y, double width, double height, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
    }

    public void update() {
        if (movingUp) {
            y -= speed;
        }
        if (movingDown) {
            y += speed;
        }

        y = Math.max(0, Math.min(600 - height, y));
    }

    public void draw(Graphics2D g2d) {
        if (powerUpActive) {
            g2d.setColor(new Color(255, 255, 0, 100));
            g2d.fillRect((int) x - 5, (int) y - 5, (int) width + 10, (int) height + 10);
        }

        GradientPaint gradient = new GradientPaint(
            (float) x, (float) y, color,
            (float) (x + width), (float) (y + height), color.darker()
        );
        g2d.setPaint(gradient);
        g2d.fillRoundRect((int) x, (int) y, (int) width, (int) height, 10, 10);

        g2d.setColor(color.brighter());
        g2d.drawRoundRect((int) x, (int) y, (int) width, (int) height, 10, 10);
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x, y, width, height);
    }

    public void setUp(boolean moving) {
        this.movingUp = moving;
    }

    public void setDown(boolean moving) {
        this.movingDown = moving;
    }

    public void reset() {
        movingUp = false;
        movingDown = false;
        powerUpActive = false;
        powerUpDuration = 0;
    }
}

class Ball {
    double x, y, size;
    double dx, dy;
    double initialSpeed = 5.0;
    Color color = Color.WHITE;

    public Ball(double x, double y, double size) {
        this.x = x;
        this.y = y;
        this.size = size;
        reset();
    }

    public void update() {
        x += dx;
        y += dy;
    }

    public void draw(Graphics2D g2d) {
        RadialGradientPaint gradient = new RadialGradientPaint(
            (float) (x + size / 2), (float) (y + size / 2), (float) size / 2,
            new float[]{0f, 1f},
            new Color[]{Color.WHITE, color}
        );
        g2d.setPaint(gradient);
        g2d.fillOval((int) x, (int) y, (int) size, (int) size);

        g2d.setColor(Color.WHITE);
        g2d.drawOval((int) x, (int) y, (int) size, (int) size);
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x, y, size, size);
    }

    public void reset() {
        x = 500 - size / 2;
        y = 300 - size / 2;
        Random rand = new Random();
        dx = (rand.nextBoolean() ? 1 : -1) * initialSpeed;
        dy = (rand.nextDouble() - 0.5) * 4;
    }
}

class Particle {
    double x, y, dx, dy;
    Color color;
    int life;
    int maxLife = 60;

    public Particle(double x, double y, double dx, double dy, Color color) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.color = color;
        this.life = maxLife;
    }

    public void update() {
        x += dx;
        y += dy;
        dy += 0.2; // Gravity
        life--;
    }

    public void draw(Graphics2D g2d) {
        float alpha = (float) life / maxLife;
        g2d.setColor(new Color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha));
        int size = (int) (6 * alpha);
        g2d.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
    }

    public boolean isDead() {
        return life <= 0;
    }
}

class PowerUp {
    double x, y, size = 30;
    PowerUpType type;
    double rotation = 0;

    public PowerUp(double x, double y, PowerUpType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void update() {
        rotation += 0.05;
        y += Math.sin(rotation * 2) * 0.5; // Floating animation
    }

    public void draw(Graphics2D g2d) {
        g2d.rotate(rotation, x + size / 2, y + size / 2);

        g2d.setColor(type.color);
        g2d.fillRoundRect((int) x, (int) y, (int) size, (int) size, 10, 10);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect((int) x, (int) y, (int) size, (int) size, 10, 10);

        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        String symbol = type.symbol;
        g2d.drawString(symbol, (int) (x + (size - fm.stringWidth(symbol)) / 2), (int) (y + (size + fm.getHeight()) / 2 - 2));

        g2d.rotate(-rotation, x + size / 2, y + size / 2);
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x, y, size, size);
    }
}

class TrailPoint {
    double x, y;

    public TrailPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

enum GameState {
    MENU, DIFFICULTY_SELECT, INSTRUCTIONS, PLAYING, PAUSED, GAME_OVER
}

enum DifficultyLevel {
    EASY, MEDIUM, HARD
}

enum PowerUpType {
    SPEED_BOOST("⚡", new Color(255, 215, 0)),
    ENLARGE_PADDLE("⬆", new Color(0, 255, 127)),
    SLOW_BALL("🐌", new Color(100, 149, 237));

    String symbol;
    Color color;

    PowerUpType(String symbol, Color color) {
        this.symbol = symbol;
        this.color = color;
    }
}