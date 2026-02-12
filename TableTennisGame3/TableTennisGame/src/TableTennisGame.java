import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class TableTennisGame extends JPanel implements KeyListener, ActionListener {
    // Game constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PADDLE_WIDTH = 15;
    private static final int PADDLE_HEIGHT = 100;
    private static final int BALL_SIZE = 20;
    private static final int PADDLE_SPEED = 8;
    private static final int MAX_SCORE = 11;

    // Game objects
    private Rectangle paddle1, paddle2;
    private Ball ball;
    private List<Particle> particles;

    // Game state
    private int score1 = 0;
    private int score2 = 0;
    private boolean gameRunning = false;
    private boolean gamePaused = false;
    private boolean gameOver = false;
    private int winner = 0;

    // Controls
    private boolean wPressed = false;
    private boolean sPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;

    // AI settings
    private boolean singlePlayer = false;
    private double aiDifficulty = 0.7;

    // Animation
    private Timer gameTimer;
    private int animationTick = 0;
    private Color[] courtColors = {new Color(0, 100, 0), new Color(0, 120, 0)};
    private GradientPaint netGradient;

    // Visual effects
    private List<HitEffect> hitEffects;
    private List<ScoreEffect> scoreEffects;

    // Sound simulation
    private boolean playSounds = true;

    public TableTennisGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        initializeGame();

        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();

        hitEffects = new ArrayList<>();
        scoreEffects = new ArrayList<>();
        particles = new ArrayList<>();

        netGradient = new GradientPaint(WIDTH/2, 0, Color.WHITE, WIDTH/2, 50, Color.GRAY);
    }

    private void initializeGame() {
        paddle1 = new Rectangle(50, HEIGHT/2 - PADDLE_HEIGHT/2, PADDLE_WIDTH, PADDLE_HEIGHT);
        paddle2 = new Rectangle(WIDTH - 50 - PADDLE_WIDTH, HEIGHT/2 - PADDLE_HEIGHT/2,
                PADDLE_WIDTH, PADDLE_HEIGHT);
        ball = new Ball(WIDTH/2 - BALL_SIZE/2, HEIGHT/2 - BALL_SIZE/2, BALL_SIZE);
        resetBall();
    }

    private void resetBall() {
        ball.x = WIDTH/2 - BALL_SIZE/2;
        ball.y = HEIGHT/2 - BALL_SIZE/2;
        ball.dx = (Math.random() > 0.5 ? 5 : -5);
        ball.dy = (Math.random() - 0.5) * 4;
        ball.speed = 5;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning || gamePaused) return;

        animationTick++;

        // Update ball
        ball.update();

        // Update particles
        updateParticles();

        // Update paddle 1 (player or AI)
        if (singlePlayer) {
            updateAI();
        } else {
            updatePaddle1();
        }

        // Update paddle 2 (player or AI)
        updatePaddle2();

        // Check collisions
        checkCollisions();

        // Update effects
        updateEffects();

        // Check score
        checkScore();

        repaint();
    }

    private void updateAI() {
        // Simple AI for single player mode
        double aiCenter = paddle2.y + PADDLE_HEIGHT/2;
        double ballCenter = ball.y + BALL_SIZE/2;

        if (Math.abs(aiCenter - ballCenter) > 20) {
            if (aiCenter < ballCenter && paddle2.y < HEIGHT - PADDLE_HEIGHT) {
                paddle2.y += (ballCenter - aiCenter) * 0.05 * aiDifficulty;
            } else if (aiCenter > ballCenter && paddle2.y > 0) {
                paddle2.y -= (aiCenter - ballCenter) * 0.05 * aiDifficulty;
            }
        }
    }

    private void updatePaddle1() {
        if (wPressed && paddle1.y > 0) {
            paddle1.y -= PADDLE_SPEED;
        }
        if (sPressed && paddle1.y < HEIGHT - PADDLE_HEIGHT) {
            paddle1.y += PADDLE_SPEED;
        }
    }

    private void updatePaddle2() {
        if (!singlePlayer) {
            if (upPressed && paddle2.y > 0) {
                paddle2.y -= PADDLE_SPEED;
            }
            if (downPressed && paddle2.y < HEIGHT - PADDLE_HEIGHT) {
                paddle2.y += PADDLE_SPEED;
            }
        }
    }

    private void checkCollisions() {
        // Wall collisions (top and bottom)
        if (ball.y <= 0 || ball.y >= HEIGHT - BALL_SIZE) {
            ball.dy = -ball.dy;
            createHitEffect(ball.x, ball.y);
            playSound("wall");
        }

        // Paddle collisions
        if (ball.intersects(paddle1)) {
            handlePaddleHit(paddle1, 1);
        } else if (ball.intersects(paddle2)) {
            handlePaddleHit(paddle2, -1);
        }
    }

    private void handlePaddleHit(Rectangle paddle, int direction) {
        // Calculate hit angle based on where ball hits paddle
        double relativeIntersectY = (paddle.y + (PADDLE_HEIGHT/2)) - (ball.y + (BALL_SIZE/2));
        double normalizedRelativeIntersectionY = (relativeIntersectY/(PADDLE_HEIGHT/2));
        double bounceAngle = normalizedRelativeIntersectionY * (Math.PI/4);

        // Update ball direction and speed
        ball.dx = direction * ball.speed * Math.cos(bounceAngle);
        ball.dy = -ball.speed * Math.sin(bounceAngle);

        // Increase speed slightly
        ball.speed = Math.min(ball.speed * 1.05, 12);

        // Add spin effect
        ball.spin = normalizedRelativeIntersectionY * 0.5;

        // Create visual effect
        createHitEffect(ball.x, ball.y);
        createParticles(ball.x, ball.y, 15);
        playSound("paddle");
    }

    private void checkScore() {
        if (ball.x < -50) {
            score2++;
            createScoreEffect(WIDTH/4, HEIGHT/2, "Player 2 Scores!");
            checkGameOver();
            resetBall();
            playSound("score");
        } else if (ball.x > WIDTH + 50) {
            score1++;
            createScoreEffect(3*WIDTH/4, HEIGHT/2, "Player 1 Scores!");
            checkGameOver();
            resetBall();
            playSound("score");
        }
    }

    private void checkGameOver() {
        if (score1 >= MAX_SCORE || score2 >= MAX_SCORE) {
            gameOver = true;
            gameRunning = false;
            winner = score1 > score2 ? 1 : 2;
        }
    }

    private void createHitEffect(double x, double y) {
        hitEffects.add(new HitEffect(x, y));
    }

    private void createScoreEffect(double x, double y, String text) {
        scoreEffects.add(new ScoreEffect(x, y, text));
    }

    private void createParticles(double x, double y, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y));
        }
    }

    private void updateParticles() {
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            p.update();
            if (p.life <= 0) {
                iterator.remove();
            }
        }
    }

    private void updateEffects() {
        hitEffects.removeIf(effect -> effect.life <= 0);
        scoreEffects.removeIf(effect -> effect.life <= 0);
        hitEffects.forEach(HitEffect::update);
        scoreEffects.forEach(ScoreEffect::update);
    }

    private void playSound(String sound) {
        if (playSounds) {
            // In a real implementation, this would play actual sounds
            System.out.println("Sound: " + sound);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw court
        drawCourt(g2d);

        // Draw net
        drawNet(g2d);

        // Draw paddles
        drawPaddles(g2d);

        // Draw ball
        drawBall(g2d);

        // Draw particles
        drawParticles(g2d);

        // Draw effects
        drawEffects(g2d);

        // Draw scores
        drawScores(g2d);

        // Draw UI
        drawUI(g2d);

        // Draw game state messages
        drawGameState(g2d);
    }

    private void drawCourt(Graphics2D g2d) {
        // Draw main court
        g2d.setColor(courtColors[0]);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw court markings
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(20, 20, WIDTH - 40, HEIGHT - 40);

        // Draw center line
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                0, new float[]{10, 10}, 0));
        g2d.drawLine(WIDTH/2, 20, WIDTH/2, HEIGHT - 20);

        // Draw service boxes
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(WIDTH/4, 20, WIDTH/2, HEIGHT - 40);

        // Draw animated court pattern
        drawAnimatedCourt(g2d);
    }

    private void drawAnimatedCourt(Graphics2D g2d) {
        // Create a subtle animated pattern on the court
        int tileSize = 40;
        for (int y = 0; y < HEIGHT; y += tileSize) {
            for (int x = 0; x < WIDTH; x += tileSize) {
                Color color = courtColors[(x/tileSize + y/tileSize + animationTick/20) % 2];
                g2d.setColor(color);
                g2d.fillRect(x, y, tileSize, tileSize);
            }
        }
    }

    private void drawNet(Graphics2D g2d) {
        int netX = WIDTH/2;
        g2d.setPaint(netGradient);

        // Draw net posts
        g2d.fillRect(netX - 5, 0, 10, 20);
        g2d.fillRect(netX - 5, HEIGHT - 20, 10, 20);

        // Draw net
        g2d.setStroke(new BasicStroke(2));
        for (int i = 0; i < 10; i++) {
            int y = 20 + i * 5;
            int wave = (int)(Math.sin(animationTick * 0.1 + i * 0.5) * 3);
            g2d.drawLine(netX - 10 + wave, y, netX + 10 + wave, y);
        }
    }

    private void drawPaddles(Graphics2D g2d) {
        // Paddle 1 with gradient
        GradientPaint paddle1Gradient = new GradientPaint(
                (float)paddle1.x, (float)paddle1.y, new Color(0, 150, 255),
                (float)paddle1.x + PADDLE_WIDTH, (float)paddle1.y + PADDLE_HEIGHT, new Color(0, 100, 200)
        );
        g2d.setPaint(paddle1Gradient);
        g2d.fillRoundRect(paddle1.x, paddle1.y, PADDLE_WIDTH, PADDLE_HEIGHT, 5, 5);

        // Paddle 2 with gradient
        GradientPaint paddle2Gradient = new GradientPaint(
                (float)paddle2.x, (float)paddle2.y, new Color(255, 100, 0),
                (float)paddle2.x + PADDLE_WIDTH, (float)paddle2.y + PADDLE_HEIGHT, new Color(200, 50, 0)
        );
        g2d.setPaint(paddle2Gradient);
        g2d.fillRoundRect(paddle2.x, paddle2.y, PADDLE_WIDTH, PADDLE_HEIGHT, 5, 5);

        // Paddle outlines
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(paddle1.x, paddle1.y, PADDLE_WIDTH, PADDLE_HEIGHT, 5, 5);
        g2d.drawRoundRect(paddle2.x, paddle2.y, PADDLE_WIDTH, PADDLE_HEIGHT, 5, 5);
    }

    private void drawBall(Graphics2D g2d) {
        // Apply spin effect to ball position for visualization
        double spinOffset = ball.spin * 5 * Math.sin(animationTick * 0.2);

        // Create ball with gradient and shine
        RadialGradientPaint ballGradient = new RadialGradientPaint(
                (float)(ball.x + BALL_SIZE/3 + spinOffset),
                (float)(ball.y + BALL_SIZE/3),
                BALL_SIZE,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{Color.WHITE, Color.YELLOW, Color.ORANGE}
        );

        g2d.setPaint(ballGradient);
        g2d.fillOval((int)(ball.x + spinOffset), (int)ball.y, BALL_SIZE, BALL_SIZE);

        // Ball outline and motion trail
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval((int)(ball.x + spinOffset), (int)ball.y, BALL_SIZE, BALL_SIZE);

        // Draw motion trail
        drawMotionTrail(g2d);
    }

    private void drawMotionTrail(Graphics2D g2d) {
        // Draw fading trail behind the ball
        int trailLength = 10;
        for (int i = 0; i < trailLength; i++) {
            float alpha = 0.1f * (1 - (float)i/trailLength);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.setColor(Color.YELLOW);
            int trailX = (int)(ball.x - ball.dx * i * 0.5);
            int trailY = (int)(ball.y - ball.dy * i * 0.5);
            g2d.fillOval(trailX, trailY, BALL_SIZE, BALL_SIZE);
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    private void drawParticles(Graphics2D g2d) {
        for (Particle p : particles) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.life/100f));
            g2d.setColor(p.color);
            g2d.fillOval((int)p.x, (int)p.y, p.size, p.size);
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    private void drawEffects(Graphics2D g2d) {
        for (HitEffect effect : hitEffects) {
            float alpha = effect.life/100f;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.setColor(effect.color);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval((int)(effect.x - effect.size/2), (int)(effect.y - effect.size/2),
                    effect.size, effect.size);
        }

        for (ScoreEffect effect : scoreEffects) {
            float alpha = Math.min(effect.life/50f, 1.0f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.setColor(Color.YELLOW);

            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(effect.text);
            g2d.drawString(effect.text, (int)(effect.x - textWidth/2), (int)effect.y);
        }

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    private void drawScores(Graphics2D g2d) {
        g2d.setFont(new Font("Digital-7", Font.BOLD, 60));

        // Player 1 score
        g2d.setColor(new Color(0, 150, 255));
        String score1Text = String.format("%02d", score1);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(score1Text, WIDTH/4 - fm.stringWidth(score1Text)/2, 80);

        // Player 2 score
        g2d.setColor(new Color(255, 100, 0));
        String score2Text = String.format("%02d", score2);
        g2d.drawString(score2Text, 3*WIDTH/4 - fm.stringWidth(score2Text)/2, 80);

        // Score separator
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.drawString(":", WIDTH/2 - 10, 80);
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));

        // Draw controls info
        String controls = singlePlayer ?
                "Controls: W/S (Paddle 1) | AI (Paddle 2)" :
                "Controls: W/S (Paddle 1) | UP/DOWN (Paddle 2)";
        g2d.drawString(controls, 20, HEIGHT - 60);

        // Draw game info
        String info = "P: Pause | R: Reset | 1/2: Player Mode | +/-: AI Difficulty";
        g2d.drawString(info, 20, HEIGHT - 40);

        // Draw FPS counter
        g2d.drawString("FPS: " + (gameTimer.getDelay() > 0 ? 1000/gameTimer.getDelay() : "N/A"),
                WIDTH - 100, 20);
    }

    private void drawGameState(Graphics2D g2d) {
        if (!gameRunning && !gameOver) {
            // Start screen
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            String title = "TABLE TENNIS GAME";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, WIDTH/2 - fm.stringWidth(title)/2, HEIGHT/2 - 50);

            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            String startMsg = "Press SPACE to Start";
            fm = g2d.getFontMetrics();
            g2d.drawString(startMsg, WIDTH/2 - fm.stringWidth(startMsg)/2, HEIGHT/2 + 20);

            g2d.setFont(new Font("Arial", Font.PLAIN, 18));
            String modeMsg = "Press 1 for Single Player, 2 for Two Players";
            g2d.drawString(modeMsg, WIDTH/2 - fm.stringWidth(modeMsg)/2, HEIGHT/2 + 60);
        }

        if (gamePaused) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            String pauseText = "GAME PAUSED";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(pauseText, WIDTH/2 - fm.stringWidth(pauseText)/2, HEIGHT/2);
        }

        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            String winText = "PLAYER " + winner + " WINS!";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(winText, WIDTH/2 - fm.stringWidth(winText)/2, HEIGHT/2 - 30);

            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            String restartText = "Press R to Restart";
            g2d.drawString(restartText, WIDTH/2 - fm.stringWidth(restartText)/2, HEIGHT/2 + 30);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_W:
                wPressed = true;
                break;
            case KeyEvent.VK_S:
                sPressed = true;
                break;
            case KeyEvent.VK_UP:
                upPressed = true;
                break;
            case KeyEvent.VK_DOWN:
                downPressed = true;
                break;
            case KeyEvent.VK_SPACE:
                if (!gameOver) {
                    gameRunning = !gameRunning;
                }
                break;
            case KeyEvent.VK_P:
                gamePaused = !gamePaused;
                break;
            case KeyEvent.VK_R:
                resetGame();
                break;
            case KeyEvent.VK_1:
                singlePlayer = true;
                initializeGame();
                break;
            case KeyEvent.VK_2:
                singlePlayer = false;
                initializeGame();
                break;
            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_EQUALS:
                aiDifficulty = Math.min(aiDifficulty + 0.1, 1.0);
                break;
            case KeyEvent.VK_MINUS:
                aiDifficulty = Math.max(aiDifficulty - 0.1, 0.1);
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_W:
                wPressed = false;
                break;
            case KeyEvent.VK_S:
                sPressed = false;
                break;
            case KeyEvent.VK_UP:
                upPressed = false;
                break;
            case KeyEvent.VK_DOWN:
                downPressed = false;
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void resetGame() {
        score1 = 0;
        score2 = 0;
        gameOver = false;
        gameRunning = false;
        winner = 0;
        initializeGame();
        particles.clear();
        hitEffects.clear();
        scoreEffects.clear();
    }

    // Inner classes for game objects and effects
    class Ball {
        double x, y;
        double dx, dy;
        double speed = 5;
        double spin = 0;
        int size;

        Ball(double x, double y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }

        void update() {
            x += dx;
            y += dy;

            // Apply spin effect
            dy += spin * 0.1;
            spin *= 0.95; // Dampen spin over time
        }

        boolean intersects(Rectangle r) {
            return new Rectangle((int)x, (int)y, size, size).intersects(r);
        }
    }

    class Particle {
        double x, y;
        double dx, dy;
        int size;
        int life;
        Color color;

        Particle(double x, double y) {
            this.x = x;
            this.y = y;
            this.dx = (Math.random() - 0.5) * 4;
            this.dy = (Math.random() - 0.5) * 4;
            this.size = (int)(Math.random() * 4 + 2);
            this.life = (int)(Math.random() * 50 + 50);
            this.color = new Color(
                    (int)(Math.random() * 255),
                    (int)(Math.random() * 255),
                    (int)(Math.random() * 255)
            );
        }

        void update() {
            x += dx;
            y += dy;
            dy += 0.1; // Gravity
            life--;
            size = Math.max(0, size - 1);
        }
    }

    class HitEffect {
        double x, y;
        int size;
        int life;
        Color color;

        HitEffect(double x, double y) {
            this.x = x;
            this.y = y;
            this.size = 20;
            this.life = 100;
            this.color = Color.YELLOW;
        }

        void update() {
            size += 2;
            life -= 4;
        }
    }

    class ScoreEffect {
        double x, y;
        String text;
        int life;

        ScoreEffect(double x, double y, String text) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.life = 100;
        }

        void update() {
            y -= 0.5;
            life -= 2;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Advanced Table Tennis Game");
        TableTennisGame game = new TableTennisGame();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        game.requestFocusInWindow();
    }
}