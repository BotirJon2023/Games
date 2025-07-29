import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class PingPongBattle extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 600;
    private static final int PADDLE_WIDTH = 15;
    private static final int PADDLE_HEIGHT = 80;
    private static final int BALL_SIZE = 20;
    private static final int FPS = 60;

    // Game objects
    private Timer gameTimer;
    private Rectangle playerPaddle;
    private Rectangle aiPaddle;
    private Ball ball;
    private GameState gameState;
    private AIPlayer ai;

    // Game variables
    private int playerScore = 0;
    private int aiScore = 0;
    private int targetScore = 5;
    private boolean[] keys = new boolean[256];
    private long lastTime = System.nanoTime();
    private double deltaTime = 0;

    // Visual effects
    private List<Particle> particles = new ArrayList<>();
    private List<TrailPoint> ballTrail = new ArrayList<>();
    private Font gameFont;
    private Font titleFont;
    private Color playerColor = new Color(100, 200, 255);
    private Color aiColor = new Color(255, 100, 100);
    private Color backgroundColor = new Color(20, 20, 30);

    // Menu system
    private MenuState menuState = MenuState.MAIN_MENU;
    private int selectedDifficulty = 1; // 0=Easy, 1=Medium, 2=Hard
    private boolean gameStarted = false;

    // Animation variables
    private double paddleAnimationOffset = 0;
    private double scoreFlashTimer = 0;
    private boolean showingWinScreen = false;
    private double winScreenTimer = 0;

    public enum GameState {
        MENU, PLAYING, PAUSED, GAME_OVER
    }

    public enum MenuState {
        MAIN_MENU, DIFFICULTY_SELECT, CONTROLS, GAME_OVER_SCREEN
    }

    public PingPongBattle() {
        initializeGame();
        setupUI();
        startGameLoop();
    }

    private void initializeGame() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(backgroundColor);
        setFocusable(true);
        addKeyListener(this);

        // Initialize fonts
        try {
            gameFont = new Font("Arial", Font.BOLD, 24);
            titleFont = new Font("Arial", Font.BOLD, 48);
        } catch (Exception e) {
            gameFont = new Font(Font.SANS_SERIF, Font.BOLD, 24);
            titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 48);
        }

        gameState = GameState.MENU;
        resetGame();
    }

    private void setupUI() {
        JFrame frame = new JFrame("Ping-Pong Battle with AI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void startGameLoop() {
        gameTimer = new Timer(1000 / FPS, this);
        gameTimer.start();
    }

    private void resetGame() {
        // Initialize paddles
        playerPaddle = new Rectangle(30, WINDOW_HEIGHT/2 - PADDLE_HEIGHT/2,
                PADDLE_WIDTH, PADDLE_HEIGHT);
        aiPaddle = new Rectangle(WINDOW_WIDTH - 30 - PADDLE_WIDTH,
                WINDOW_HEIGHT/2 - PADDLE_HEIGHT/2,
                PADDLE_WIDTH, PADDLE_HEIGHT);

        // Initialize ball
        ball = new Ball(WINDOW_WIDTH/2, WINDOW_HEIGHT/2);

        // Initialize AI
        ai = new AIPlayer(selectedDifficulty);

        // Clear effects
        particles.clear();
        ballTrail.clear();

        // Reset scores if starting new game
        if (!gameStarted) {
            playerScore = 0;
            aiScore = 0;
            gameStarted = true;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastTime) / 1_000_000_000.0;
        lastTime = currentTime;

        update();
        repaint();
    }

    private void update() {
        switch (gameState) {
            case MENU:
                updateMenu();
                break;
            case PLAYING:
                updateGame();
                break;
            case PAUSED:
                // Game is paused, only update visual effects
                updateParticles();
                break;
            case GAME_OVER:
                updateGameOver();
                break;
        }

        paddleAnimationOffset += deltaTime * 2;
        if (scoreFlashTimer > 0) {
            scoreFlashTimer -= deltaTime;
        }
    }

    private void updateMenu() {
        updateParticles();

        // Handle menu input
        if (keys[KeyEvent.VK_ENTER]) {
            keys[KeyEvent.VK_ENTER] = false;
            handleMenuSelection();
        }

        if (keys[KeyEvent.VK_UP]) {
            keys[KeyEvent.VK_UP] = false;
            if (menuState == MenuState.DIFFICULTY_SELECT && selectedDifficulty > 0) {
                selectedDifficulty--;
            }
        }

        if (keys[KeyEvent.VK_DOWN]) {
            keys[KeyEvent.VK_DOWN] = false;
            if (menuState == MenuState.DIFFICULTY_SELECT && selectedDifficulty < 2) {
                selectedDifficulty++;
            }
        }

        if (keys[KeyEvent.VK_ESCAPE]) {
            keys[KeyEvent.VK_ESCAPE] = false;
            if (menuState != MenuState.MAIN_MENU) {
                menuState = MenuState.MAIN_MENU;
            }
        }
    }

    private void updateGame() {
        // Update player paddle
        updatePlayerPaddle();

        // Update AI paddle
        ai.update(aiPaddle, ball, deltaTime);

        // Update ball
        ball.update(deltaTime);

        // Handle collisions
        handleCollisions();

        // Update effects
        updateParticles();
        updateBallTrail();

        // Check for scoring
        checkScoring();

        // Handle pause
        if (keys[KeyEvent.VK_ESCAPE]) {
            keys[KeyEvent.VK_ESCAPE] = false;
            gameState = GameState.PAUSED;
        }
    }

    private void updatePlayerPaddle() {
        int speed = 300;

        if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP]) {
            playerPaddle.y -= speed * deltaTime;
        }
        if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN]) {
            playerPaddle.y += speed * deltaTime;
        }

        // Keep paddle in bounds
        if (playerPaddle.y < 0) playerPaddle.y = 0;
        if (playerPaddle.y > WINDOW_HEIGHT - PADDLE_HEIGHT) {
            playerPaddle.y = WINDOW_HEIGHT - PADDLE_HEIGHT;
        }
    }

    private void handleCollisions() {
        Rectangle ballRect = new Rectangle((int)ball.x - BALL_SIZE/2,
                (int)ball.y - BALL_SIZE/2,
                BALL_SIZE, BALL_SIZE);

        // Ball collision with top/bottom walls
        if (ball.y <= BALL_SIZE/2 || ball.y >= WINDOW_HEIGHT - BALL_SIZE/2) {
            ball.velocityY = -ball.velocityY;
            ball.y = Math.max(BALL_SIZE/2, Math.min(WINDOW_HEIGHT - BALL_SIZE/2, ball.y));
            createWallHitEffect();
        }

        // Ball collision with player paddle
        if (ballRect.intersects(playerPaddle) && ball.velocityX < 0) {
            handlePaddleCollision(playerPaddle, true);
        }

        // Ball collision with AI paddle
        if (ballRect.intersects(aiPaddle) && ball.velocityX > 0) {
            handlePaddleCollision(aiPaddle, false);
        }
    }

    private void handlePaddleCollision(Rectangle paddle, boolean isPlayer) {
        // Calculate hit position on paddle (-1 to 1)
        double hitPos = ((ball.y - (paddle.y + paddle.height/2.0)) / (paddle.height/2.0));
        hitPos = Math.max(-1, Math.min(1, hitPos));

        // Reverse X direction and add spin based on hit position
        ball.velocityX = -ball.velocityX * 1.05; // Slight speed increase
        ball.velocityY = hitPos * 200 + ball.velocityY * 0.3;

        // Limit maximum speed
        double maxSpeed = 400;
        if (Math.abs(ball.velocityX) > maxSpeed) {
            ball.velocityX = Math.signum(ball.velocityX) * maxSpeed;
        }
        if (Math.abs(ball.velocityY) > maxSpeed) {
            ball.velocityY = Math.signum(ball.velocityY) * maxSpeed;
        }

        // Move ball away from paddle to prevent sticking
        if (isPlayer) {
            ball.x = paddle.x + paddle.width + BALL_SIZE/2 + 5;
        } else {
            ball.x = paddle.x - BALL_SIZE/2 - 5;
        }

        // Create hit effect
        createPaddleHitEffect(isPlayer);
    }

    private void checkScoring() {
        if (ball.x < -BALL_SIZE) {
            // AI scores
            aiScore++;
            scoreFlashTimer = 2.0;
            createScoreEffect(false);
            resetBall();
        } else if (ball.x > WINDOW_WIDTH + BALL_SIZE) {
            // Player scores
            playerScore++;
            scoreFlashTimer = 2.0;
            createScoreEffect(true);
            resetBall();
        }

        // Check for game over
        if (playerScore >= targetScore || aiScore >= targetScore) {
            gameState = GameState.GAME_OVER;
            showingWinScreen = true;
            winScreenTimer = 0;
        }
    }

    private void resetBall() {
        ball.x = WINDOW_WIDTH / 2;
        ball.y = WINDOW_HEIGHT / 2;

        // Random direction but not too steep
        double angle = (Math.random() - 0.5) * Math.PI / 3; // ±60 degrees
        int direction = Math.random() < 0.5 ? -1 : 1;

        ball.velocityX = direction * 200 * Math.cos(angle);
        ball.velocityY = 200 * Math.sin(angle);

        ballTrail.clear();
    }

    private void updateParticles() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(deltaTime);
            if (p.isDead()) {
                it.remove();
            }
        }
    }

    private void updateBallTrail() {
        ballTrail.add(new TrailPoint(ball.x, ball.y));
        if (ballTrail.size() > 10) {
            ballTrail.remove(0);
        }
    }

    private void updateGameOver() {
        updateParticles();
        winScreenTimer += deltaTime;

        if (keys[KeyEvent.VK_ENTER] && winScreenTimer > 2.0) {
            keys[KeyEvent.VK_ENTER] = false;
            gameState = GameState.MENU;
            menuState = MenuState.MAIN_MENU;
            gameStarted = false;
            showingWinScreen = false;
        }

        if (keys[KeyEvent.VK_R] && winScreenTimer > 2.0) {
            keys[KeyEvent.VK_R] = false;
            playerScore = 0;
            aiScore = 0;
            resetGame();
            gameState = GameState.PLAYING;
            showingWinScreen = false;
        }
    }

    private void handleMenuSelection() {
        switch (menuState) {
            case MAIN_MENU:
                menuState = MenuState.DIFFICULTY_SELECT;
                break;
            case DIFFICULTY_SELECT:
                gameState = GameState.PLAYING;
                resetGame();
                break;
        }
    }

    // Visual effects methods
    private void createPaddleHitEffect(boolean isPlayer) {
        Color color = isPlayer ? playerColor : aiColor;
        double x = isPlayer ? playerPaddle.x + playerPaddle.width : aiPaddle.x;

        for (int i = 0; i < 10; i++) {
            particles.add(new Particle(x, ball.y, color));
        }
    }

    private void createWallHitEffect() {
        for (int i = 0; i < 8; i++) {
            particles.add(new Particle(ball.x, ball.y, Color.WHITE));
        }
    }

    private void createScoreEffect(boolean playerScored) {
        Color color = playerScored ? playerColor : aiColor;
        double x = WINDOW_WIDTH / 2;
        double y = WINDOW_HEIGHT / 2;

        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(x, y, color));
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
            case PLAYING:
            case PAUSED:
                drawGame(g2d);
                if (gameState == GameState.PAUSED) {
                    drawPauseOverlay(g2d);
                }
                break;
            case GAME_OVER:
                drawGame(g2d);
                drawGameOverScreen(g2d);
                break;
        }
    }

    private void drawMenu(Graphics2D g2d) {
        // Background with animated particles
        drawBackground(g2d);
        drawParticles(g2d);

        switch (menuState) {
            case MAIN_MENU:
                drawMainMenu(g2d);
                break;
            case DIFFICULTY_SELECT:
                drawDifficultyMenu(g2d);
                break;
        }
    }

    private void drawMainMenu(Graphics2D g2d) {
        g2d.setFont(titleFont);
        g2d.setColor(Color.WHITE);
        drawCenteredText(g2d, "PING-PONG BATTLE", WINDOW_HEIGHT/2 - 100);

        g2d.setFont(gameFont);
        drawCenteredText(g2d, "Press ENTER to Start", WINDOW_HEIGHT/2);
        drawCenteredText(g2d, "W/S or ↑/↓ to move paddle", WINDOW_HEIGHT/2 + 40);
    }

    private void drawDifficultyMenu(Graphics2D g2d) {
        g2d.setFont(titleFont);
        g2d.setColor(Color.WHITE);
        drawCenteredText(g2d, "SELECT DIFFICULTY", WINDOW_HEIGHT/2 - 100);

        g2d.setFont(gameFont);
        String[] difficulties = {"EASY", "MEDIUM", "HARD"};
        for (int i = 0; i < difficulties.length; i++) {
            if (i == selectedDifficulty) {
                g2d.setColor(playerColor);
                drawCenteredText(g2d, "> " + difficulties[i] + " <", WINDOW_HEIGHT/2 + i * 40);
            } else {
                g2d.setColor(Color.GRAY);
                drawCenteredText(g2d, difficulties[i], WINDOW_HEIGHT/2 + i * 40);
            }
        }

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        drawCenteredText(g2d, "Use ↑/↓ to select, ENTER to confirm, ESC to go back", WINDOW_HEIGHT - 50);
    }

    private void drawGame(Graphics2D g2d) {
        drawBackground(g2d);
        drawField(g2d);
        drawBallTrail(g2d);
        drawBall(g2d);
        drawPaddles(g2d);
        drawScore(g2d);
        drawParticles(g2d);
    }

    private void drawBackground(Graphics2D g2d) {
        // Gradient background
        GradientPaint gradient = new GradientPaint(0, 0, backgroundColor,
                0, WINDOW_HEIGHT,
                backgroundColor.darker());
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    private void drawField(Graphics2D g2d) {
        g2d.setColor(new Color(100, 100, 100, 100));
        g2d.setStroke(new BasicStroke(3));

        // Center line
        g2d.drawLine(WINDOW_WIDTH/2, 0, WINDOW_WIDTH/2, WINDOW_HEIGHT);

        // Center circle
        g2d.drawOval(WINDOW_WIDTH/2 - 50, WINDOW_HEIGHT/2 - 50, 100, 100);
    }

    private void drawBallTrail(Graphics2D g2d) {
        if (ballTrail.size() < 2) return;

        for (int i = 0; i < ballTrail.size() - 1; i++) {
            float alpha = (float) i / ballTrail.size() * 0.5f;
            g2d.setColor(new Color(1f, 1f, 1f, alpha));
            TrailPoint p = ballTrail.get(i);
            g2d.fillOval((int)p.x - 3, (int)p.y - 3, 6, 6);
        }
    }

    private void drawBall(Graphics2D g2d) {
        // Ball glow effect
        RadialGradientPaint glow = new RadialGradientPaint(
                (float)ball.x, (float)ball.y, BALL_SIZE,
                new float[]{0f, 0.7f, 1f},
                new Color[]{Color.WHITE, new Color(255, 255, 255, 100), new Color(255, 255, 255, 0)}
        );
        g2d.setPaint(glow);
        g2d.fillOval((int)ball.x - BALL_SIZE, (int)ball.y - BALL_SIZE, BALL_SIZE * 2, BALL_SIZE * 2);

        // Ball core
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int)ball.x - BALL_SIZE/2, (int)ball.y - BALL_SIZE/2, BALL_SIZE, BALL_SIZE);
    }

    private void drawPaddles(Graphics2D g2d) {
        // Player paddle with animation
        g2d.setColor(playerColor);
        double playerOffset = Math.sin(paddleAnimationOffset) * 2;
        g2d.fillRoundRect(playerPaddle.x + (int)playerOffset, playerPaddle.y,
                playerPaddle.width, playerPaddle.height, 10, 10);

        // AI paddle with animation
        g2d.setColor(aiColor);
        double aiOffset = Math.sin(paddleAnimationOffset + Math.PI) * 2;
        g2d.fillRoundRect(aiPaddle.x + (int)aiOffset, aiPaddle.y,
                aiPaddle.width, aiPaddle.height, 10, 10);
    }

    private void drawScore(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 48));

        // Player score
        if (scoreFlashTimer > 0 && Math.sin(scoreFlashTimer * 10) > 0) {
            g2d.setColor(playerColor.brighter());
        } else {
            g2d.setColor(playerColor);
        }
        g2d.drawString(String.valueOf(playerScore), WINDOW_WIDTH/4 - 20, 80);

        // AI score
        if (scoreFlashTimer > 0 && Math.sin(scoreFlashTimer * 10) > 0) {
            g2d.setColor(aiColor.brighter());
        } else {
            g2d.setColor(aiColor);
        }
        g2d.drawString(String.valueOf(aiScore), 3*WINDOW_WIDTH/4 - 20, 80);
    }

    private void drawParticles(Graphics2D g2d) {
        for (Particle p : particles) {
            p.draw(g2d);
        }
    }

    private void drawPauseOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        drawCenteredText(g2d, "PAUSED", WINDOW_HEIGHT/2);

        g2d.setFont(gameFont);
        drawCenteredText(g2d, "Press ESC to resume", WINDOW_HEIGHT/2 + 60);
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);

        if (playerScore >= targetScore) {
            g2d.setColor(playerColor);
            drawCenteredText(g2d, "YOU WIN!", WINDOW_HEIGHT/2 - 50);
        } else {
            g2d.setColor(aiColor);
            drawCenteredText(g2d, "AI WINS!", WINDOW_HEIGHT/2 - 50);
        }

        g2d.setColor(Color.WHITE);
        g2d.setFont(gameFont);
        drawCenteredText(g2d, "Final Score: " + playerScore + " - " + aiScore, WINDOW_HEIGHT/2 + 20);

        if (winScreenTimer > 2.0) {
            drawCenteredText(g2d, "Press R to play again or ENTER for menu", WINDOW_HEIGHT/2 + 80);
        }
    }

    private void drawCenteredText(Graphics2D g2d, String text, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int x = (WINDOW_WIDTH - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, y);
    }

    // Key event handlers
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() < keys.length) {
            keys[e.getKeyCode()] = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && gameState == GameState.PAUSED) {
            gameState = GameState.PLAYING;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < keys.length) {
            keys[e.getKeyCode()] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner classes
    class Ball {
        double x, y;
        double velocityX, velocityY;

        Ball(double x, double y) {
            this.x = x;
            this.y = y;
            resetVelocity();
        }

        void resetVelocity() {
            double angle = (Math.random() - 0.5) * Math.PI / 4;
            int direction = Math.random() < 0.5 ? -1 : 1;
            velocityX = direction * 200 * Math.cos(angle);
            velocityY = 200 * Math.sin(angle);
        }

        void update(double dt) {
            x += velocityX * dt;
            y += velocityY * dt;
        }
    }

    class AIPlayer {
        private int difficulty;
        private double reactionDelay = 0;
        private double targetY;
        private Random random = new Random();

        AIPlayer(int difficulty) {
            this.difficulty = difficulty;
        }

        void update(Rectangle paddle, Ball ball, double dt) {
            // Calculate reaction delay based on difficulty
            double[] delays = {0.2, 0.1, 0.05}; // Easy, Medium, Hard
            double[] speeds = {200, 300, 400};
            double[] accuracies = {0.7, 0.85, 0.95};

            reactionDelay -= dt;

            if (reactionDelay <= 0) {
                // Add some randomness to AI prediction
                double prediction = ball.y;
                if (ball.velocityX > 0) { // Ball moving towards AI
                    double timeToReach = (paddle.x - ball.x) / ball.velocityX;
                    prediction = ball.y + ball.velocityY * timeToReach;

                    // Add inaccuracy based on difficulty
                    double inaccuracy = (1.0 - accuracies[difficulty]) * 100;
                    prediction += (random.nextGaussian() * inaccuracy);
                }

                targetY = prediction - paddle.height / 2.0;
                reactionDelay = delays[difficulty];
            }

            // Move paddle towards target
            double currentCenter = paddle.y + paddle.height / 2.0;
            double targetCenter = targetY + paddle.height / 2.0;
            double diff = targetCenter - currentCenter;

            if (Math.abs(diff) > 5) {
                double moveSpeed = speeds[difficulty] * dt;
                if (diff > 0) {
                    paddle.y += Math.min(moveSpeed, diff);
                } else {
                    paddle.y += Math.max(-moveSpeed, diff);
                }
            }

            // Keep in bounds
            paddle.y = Math.max(0, Math.min(WINDOW_HEIGHT - paddle.height, paddle.y));
        }
    }

    class Particle {
        double x, y, vx, vy;
        Color color;
        double life, maxLife;

        Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;

            double angle = Math.random() * 2 * Math.PI;
            double speed = 50 + Math.random() * 100;
            vx = Math.cos(angle) * speed;
            vy = Math.sin(angle) * speed;

            maxLife = life = 0.5 + Math.random() * 1.0;
        }

        void update(double dt) {
            x += vx * dt;
            y += vy * dt;
            vy += 200 * dt; // Gravity
            life -= dt;
        }

        boolean isDead() {
            return life <= 0;
        }

        void draw(Graphics2D g2d) {
            float alpha = (float)(life / maxLife) * 0.8f;
            Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    Math.max(0, Math.min(255, (int)(alpha * 255))));
            g2d.setColor(c);
            int size = (int)(life / maxLife * 8) + 2;
            g2d.fillOval((int)x - size/2, (int)y - size/2, size, size);
        }
    }

    class TrailPoint {
        double x, y;

        TrailPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new PingPongBattle();
        });
    }
}