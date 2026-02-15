import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class TableTennisGame extends JPanel implements ActionListener, KeyListener, MouseMotionListener {

    private static final int WIDTH = 1100;
    private static final int HEIGHT = 700;
    private static final int FPS = 60;
    private static final int GAME_TICK_MS = 1000 / FPS;

    // Colors
    private static final Color DARK_TABLE_GREEN = new Color(0, 100, 0);
    private static final Color LIGHT_TABLE_GREEN = new Color(0, 140, 0);
    private static final Color NET_COLOR = new Color(220, 220, 220);
    private static final Color BALL_COLOR = new Color(255, 245, 180);
    private static final Color RACKET_RED = new Color(220, 40, 40);
    private static final Color RACKET_BLUE = new Color(40, 80, 220);
    private static final Color ROCKET_TRAIL = new Color(255, 180, 60, 140);
    private static final Color ROCKET_FIRE = new Color(255, 120, 30, 180);
    private static final Color TEXT_HIGHLIGHT = new Color(255, 255, 180);

    // Game objects sizes
    private static final int PADDLE_WIDTH = 18;
    private static final int PADDLE_HEIGHT = 110;
    private static final int BALL_DIAMETER = 22;
    private static final int NET_WIDTH = 8;
    private static final int NET_HEIGHT = HEIGHT - 140;

    // Physics constants
    private static final double BALL_BASE_SPEED = 5.8;
    private static final double MAX_BALL_SPEED = 14.2;
    private static final double SPEED_INCREASE_PER_HIT = 0.24;
    private static final double PADDLE_HIT_ANGLE_FACTOR = 1.45;
    private static final double GRAVITY_EFFECT = 0.014;     // very light curve
    private static final double SPIN_INFLUENCE = 0.38;

    // Scoring & game rules
    private static final int POINTS_TO_WIN = 11;
    private static final int WIN_MARGIN = 2;
    private static final int SERVE_RIGHT = 1;
    private static final int SERVE_LEFT = -1;

    // ================================================================
    //   GAME STATE VARIABLES
    // ================================================================

    // Entities
    private Paddle player1;     // left  - mouse / keys W/S
    private Paddle player2;     // right - AI or keys ↑↓
    private Ball ball;

    // Game flow
    private int scorePlayer1 = 0;
    private int scorePlayer2 = 0;
    private boolean gameRunning = false;
    private boolean gameOver = false;
    private int servingPlayer = SERVE_RIGHT;   // who serves next
    private int rallyLength = 0;
    private int maxRallyThisGame = 0;

    // Animation & effects
    private ArrayList<RocketParticle> rocketParticles = new ArrayList<>();
    private ArrayList<TrailPoint> ballTrail = new ArrayList<>();
    private int trailLength = 18;
    private boolean rocketMode = false;         // activated after long rally
    private int rocketCountdown = 0;
    private Random random = new Random();

    // Input
    private boolean keyW = false, keyS = false;
    private boolean keyUp = false, keyDown = false;
    private int mouseY = HEIGHT / 2;

    // UI / screens
    private String gameMessage = "Click or press SPACE to start";
    private Font fontLarge = new Font("Segoe UI", Font.BOLD, 72);
    private Font fontMedium = new Font("Segoe UI", Font.BOLD, 42);
    private Font fontSmall = new Font("Segoe UI", Font.PLAIN, 24);

    // Timer
    private Timer gameTimer;

    // ================================================================
    //   MAIN ENTRY POINT
    // ================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Table Tennis – Rocket Edition");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            TableTennisGame game = new TableTennisGame();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }

    // ================================================================
    //   CONSTRUCTOR – initialize everything
    // ================================================================

    public TableTennisGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        addKeyListener(this);
        addMouseMotionListener(this);

        // Create game objects
        resetGameEntities();

        // Game loop timer
        gameTimer = new Timer(GAME_TICK_MS, this);
        gameTimer.start();
    }

    private void resetGameEntities() {
        player1 = new Paddle(40, HEIGHT/2 - PADDLE_HEIGHT/2, true);
        player2 = new Paddle(WIDTH - 40 - PADDLE_WIDTH, HEIGHT/2 - PADDLE_HEIGHT/2, false);
        ball = new Ball(WIDTH/2.0, HEIGHT/2.0);

        ballTrail.clear();
        rocketParticles.clear();
        rocketMode = false;
        rocketCountdown = 0;
        rallyLength = 0;
    }

    // ================================================================
    //   GAME LOOP – actionPerformed (called ~60 times/sec)
    // ================================================================

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning && !gameOver) {
            repaint();
            return;
        }

        if (gameOver) {
            repaint();
            return;
        }

        // Update logic
        updateInput();
        updateBallPhysics();
        updatePaddles();
        updateParticlesAndEffects();
        checkScoringAndReset();

        // Auto-activate rocket mode after long rally
        if (rallyLength >= 18 && !rocketMode) {
            activateRocketMode();
        }

        repaint();
    }

    private void updateInput() {
        // Player 1 (left) – keyboard W/S or mouse
        if (keyW && !keyS) {
            player1.targetY -= 14;
        }
        if (keyS && !keyW) {
            player1.targetY += 14;
        }

        // Player 2 (right) – keyboard arrows or simple AI
        if (keyUp && !keyDown) {
            player2.targetY -= 12;
        }
        if (keyDown && !keyUp) {
            player2.targetY += 12;
        } else if (!gameRunning) {
            // no keys → AI follows ball when serving/playing
        } else {
            // Very simple AI – can be improved later
            double aiSpeed = 6.2 + (rallyLength * 0.12);
            if (ball.x > WIDTH * 0.55) {
                if (ball.y + BALL_DIAMETER/2 < player2.y + PADDLE_HEIGHT/2 - 18) {
                    player2.targetY -= aiSpeed;
                } else if (ball.y + BALL_DIAMETER/2 > player2.y + PADDLE_HEIGHT/2 + 18) {
                    player2.targetY += aiSpeed;
                }
            }
        }

        // Mouse always overrides player 1 vertical position
        player1.targetY = mouseY - PADDLE_HEIGHT / 2;
    }

    private void updatePaddles() {
        player1.update();
        player2.update();
    }

    private void updateBallPhysics() {
        if (!gameRunning) return;

        ballTrail.add(new TrailPoint(ball.x, ball.y, ball.speed));

        if (ballTrail.size() > trailLength) {
            ballTrail.remove(0);
        }

        // Apply very light gravity curve
        ball.vy += GRAVITY_EFFECT * (ball.y < HEIGHT/2 ? -0.3 : 0.4);

        // Move
        ball.x += ball.vx;
        ball.y += ball.vy;

        // Wall bounce (top & bottom)
        if (ball.y <= 40 || ball.y >= HEIGHT - 40 - BALL_DIAMETER) {
            ball.vy = -ball.vy * 0.97;   // slight energy loss
            ball.y = Math.max(40, Math.min(HEIGHT - 40 - BALL_DIAMETER, ball.y));
            addWallHitParticles();
        }

        // Paddle collision detection
        checkPaddleCollision(player1);
        checkPaddleCollision(player2);
    }

    private void checkPaddleCollision(Paddle paddle) {
        boolean leftPaddle = paddle.isLeft;

        double paddleLeft   = paddle.x;
        double paddleRight  = paddle.x + PADDLE_WIDTH;
        double paddleTop    = paddle.y;
        double paddleBottom = paddle.y + PADDLE_HEIGHT;

        double ballLeft   = ball.x;
        double ballRight  = ball.x + BALL_DIAMETER;
        double ballTop    = ball.y;
        double ballBottom = ball.y + BALL_DIAMETER;

        if (ballRight < paddleLeft || ballLeft > paddleRight ||
                ballBottom < paddleTop || ballTop > paddleBottom) {
            return;
        }

        // Hit!
        rallyLength++;
        ball.speed = Math.min(MAX_BALL_SPEED, ball.speed + SPEED_INCREASE_PER_HIT);

        // Direction & angle depending on hit position
        double hitPos = (ball.y + BALL_DIAMETER/2 - (paddle.y + PADDLE_HEIGHT/2)) / (PADDLE_HEIGHT/2.0);
        hitPos = Math.max(-1.0, Math.min(1.0, hitPos));

        double angle = hitPos * PADDLE_HIT_ANGLE_FACTOR;

        // Spin simulation (simple)
        double spinFactor = (random.nextDouble() - 0.5) * SPIN_INFLUENCE;

        if (leftPaddle) {
            // Left paddle → ball goes right
            ball.vx = Math.cos(angle) * ball.speed + spinFactor;
            ball.vy = Math.sin(angle) * ball.speed * 1.15;
        } else {
            // Right paddle → ball goes left
            ball.vx = -Math.cos(angle) * ball.speed + spinFactor;
            ball.vy = Math.sin(angle) * ball.speed * 1.15;
        }

        // Small kick away from paddle to prevent sticking
        if (leftPaddle) {
            ball.x = paddle.x + PADDLE_WIDTH + 4;
        } else {
            ball.x = paddle.x - BALL_DIAMETER - 4;
        }

        addHitParticles(leftPaddle ? RACKET_RED : RACKET_BLUE);
        playHitSoundSimulation();
    }

    private void checkScoringAndReset() {
        // Ball passed left side → player 2 scores
        if (ball.x < -30) {
            scorePlayer2++;
            servingPlayer = SERVE_LEFT;
            resetRound();
        }
        // Ball passed right side → player 1 scores
        else if (ball.x > WIDTH + 30) {
            scorePlayer1++;
            servingPlayer = SERVE_RIGHT;
            resetRound();
        }

        // Check win condition
        if (scorePlayer1 >= POINTS_TO_WIN || scorePlayer2 >= POINTS_TO_WIN) {
            if (Math.abs(scorePlayer1 - scorePlayer2) >= WIN_MARGIN) {
                gameOver = true;
                gameRunning = false;
                gameMessage = scorePlayer1 > scorePlayer2
                        ? "LEFT PLAYER WINS!"
                        : "RIGHT PLAYER WINS!";
            }
        }
    }

    private void resetRound() {
        gameRunning = false;
        ball.reset(WIDTH/2.0, HEIGHT/2.0);
        ballTrail.clear();

        player1.y = HEIGHT/2.0 - PADDLE_HEIGHT/2;
        player2.y = HEIGHT/2.0 - PADDLE_HEIGHT/2;

        gameMessage = servingPlayer == SERVE_RIGHT
                ? "Right player serves – click / SPACE"
                : "Left player serves – click / SPACE";

        rocketMode = false;
        rocketCountdown = 0;
        rallyLength = 0;
    }

    private void activateRocketMode() {
        rocketMode = true;
        rocketCountdown = 180; // ~3 seconds
        gameMessage = "ROCKET MODE ACTIVATED!";
        trailLength = 36;
        addRocketExplosion();
    }

    // ================================================================
    //   PARTICLE & VISUAL EFFECTS
    // ================================================================

    private void addHitParticles(Color baseColor) {
        for (int i = 0; i < 14; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1.8 + random.nextDouble() * 3.4;
            rocketParticles.add(new RocketParticle(
                    ball.x + BALL_DIAMETER/2,
                    ball.y + BALL_DIAMETER/2,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    baseColor, 22 + random.nextInt(18)
            ));
        }
    }

    private void addWallHitParticles() {
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1.2 + random.nextDouble() * 2.6;
            rocketParticles.add(new RocketParticle(
                    ball.x + BALL_DIAMETER/2,
                    ball.y + BALL_DIAMETER/2,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    Color.WHITE, 14
            ));
        }
    }

    private void addRocketExplosion() {
        for (int i = 0; i < 60; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 3.5 + random.nextDouble() * 7.0;
            Color c = random.nextBoolean() ? ROCKET_FIRE : ROCKET_TRAIL;
            rocketParticles.add(new RocketParticle(
                    ball.x + BALL_DIAMETER/2,
                    ball.y + BALL_DIAMETER/2,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    c, 30 + random.nextInt(30)
            ));
        }
    }

    private void updateParticlesAndEffects() {
        // Update & remove old particles
        for (int i = rocketParticles.size() - 1; i >= 0; i--) {
            RocketParticle p = rocketParticles.get(i);
            p.update();
            if (p.life <= 0) {
                rocketParticles.remove(i);
            }
        }

        if (rocketCountdown > 0) {
            rocketCountdown--;
            if (rocketCountdown % 8 == 0 && rocketMode) {
                addRocketTrailEffect();
            }
            if (rocketCountdown <= 0) {
                rocketMode = false;
                trailLength = 18;
            }
        }
    }

    private void addRocketTrailEffect() {
        for (int i = 0; i < 5; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 30;
            double offsetY = (random.nextDouble() - 0.5) * 30;
            rocketParticles.add(new RocketParticle(
                    ball.x + BALL_DIAMETER/2 + offsetX,
                    ball.y + BALL_DIAMETER/2 + offsetY,
                    0, 0,
                    ROCKET_FIRE, 16
            ));
        }
    }

    private void playHitSoundSimulation() {
        // In real game you would use Clip / AudioSystem
        // Here we just visualize it with bigger particles
    }

    // ================================================================
    //   RENDERING – paintComponent
    // ================================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawTable(g2);
        drawNet(g2);
        drawCenterLine(g2);

        drawBallTrail(g2);
        drawBall(g2);

        drawPaddle(player1, g2);
        drawPaddle(player2, g2);

        drawParticles(g2);

        drawScoresAndUI(g2);

        if (!gameRunning && !gameOver) {
            drawStartMessage(g2);
        }
        if (gameOver) {
            drawGameOverScreen(g2);
        }
        if (rocketMode) {
            drawRocketOverlay(g2);
        }
    }

    private void drawTable(Graphics2D g2) {
        // Gradient table
        GradientPaint gradient = new GradientPaint(
                0, 0, DARK_TABLE_GREEN,
                WIDTH, HEIGHT, LIGHT_TABLE_GREEN);
        g2.setPaint(gradient);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Border
        g2.setColor(new Color(180, 140, 60));
        g2.setStroke(new BasicStroke(14));
        g2.drawRect(20, 20, WIDTH-40, HEIGHT-40);
    }

    private void drawNet(Graphics2D g2) {
        g2.setColor(NET_COLOR);
        g2.setStroke(new BasicStroke(NET_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{12, 18}, 0));
        g2.drawLine(WIDTH/2, 30, WIDTH/2, HEIGHT-30);
    }

    private void drawCenterLine(Graphics2D g2) {
        g2.setColor(new Color(255,255,255,90));
        g2.setStroke(new BasicStroke(4));
        g2.drawLine(0, HEIGHT/2, WIDTH, HEIGHT/2);
    }

    private void drawBallTrail(Graphics2D g2) {
        for (int i = 0; i < ballTrail.size(); i++) {
            TrailPoint p = ballTrail.get(i);
            float alpha = (float)i / ballTrail.size();
            int size = (int)(BALL_DIAMETER * (0.3 + alpha * 0.7));

            g2.setColor(new Color(255, 240, 140, (int)(60 + alpha * 140)));
            g2.fillOval(
                    (int)(p.x - size/2),
                    (int)(p.y - size/2),
                    size, size
            );
        }
    }

    private void drawBall(Graphics2D g2) {
        // Shadow
        g2.setColor(new Color(0,0,0,80));
        g2.fillOval((int)ball.x + 6, (int)ball.y + 6, BALL_DIAMETER, BALL_DIAMETER);

        // Ball
        g2.setColor(BALL_COLOR);
        g2.fillOval((int)ball.x, (int)ball.y, BALL_DIAMETER, BALL_DIAMETER);

        // Highlight
        g2.setColor(new Color(255,255,255,180));
        g2.fillOval((int)ball.x + 5, (int)ball.y + 5, BALL_DIAMETER-10, BALL_DIAMETER/3);
    }

    private void drawPaddle(Paddle p, Graphics2D g2) {
        Color c = p.isLeft ? RACKET_RED : RACKET_BLUE;

        // Shadow
        g2.setColor(new Color(0,0,0,100));
        g2.fillRoundRect(p.x + 4, p.y + 4, PADDLE_WIDTH, PADDLE_HEIGHT, 24, 24);

        // Main paddle
        g2.setColor(c);
        g2.fillRoundRect(p.x, p.y, PADDLE_WIDTH, PADDLE_HEIGHT, 20, 20);

        // Shine
        GradientPaint shine = new GradientPaint(
                (float) p.x, (float) p.y, new Color(255,255,255,90),
                p.x, p.y + PADDLE_HEIGHT, new Color(255,255,255,10));
        g2.setPaint(shine);
        g2.fillRoundRect(p.x+3, p.y+3, PADDLE_WIDTH-6, PADDLE_HEIGHT-6, 16, 16);

        // Grip
        g2.setColor(new Color(80,50,30));
        g2.fillRect((int) (p.x + PADDLE_WIDTH/2 - 6), (int) (p.y + PADDLE_HEIGHT - 40), 12, 40);
    }

    private void drawParticles(Graphics2D g2) {
        for (RocketParticle p : rocketParticles) {
            g2.setColor(p.color);
            int size = (int)(p.size * (p.life / 30.0));
            g2.fillOval((int)(p.x - size/2), (int)(p.y - size/2), size, size);
        }
    }

    private void drawScoresAndUI(Graphics2D g2) {
        g2.setColor(TEXT_HIGHLIGHT);
        g2.setFont(fontMedium);

        // Scores
        String s1 = String.valueOf(scorePlayer1);
        String s2 = String.valueOf(scorePlayer2);

        FontMetrics fm = g2.getFontMetrics();
        int w1 = fm.stringWidth(s1);
        int w2 = fm.stringWidth(s2);

        g2.drawString(s1, WIDTH/2 - 80 - w1, 80);
        g2.drawString(s2, WIDTH/2 + 80, 80);

        // Rally counter
        if (rallyLength > 5) {
            g2.setFont(fontSmall);
            g2.setColor(new Color(255,220,100,180));
            g2.drawString("Rally: " + rallyLength, WIDTH/2 - 60, HEIGHT - 40);
        }
    }

    private void drawStartMessage(Graphics2D g2) {
        g2.setColor(new Color(255,255,220,220));
        g2.setFont(fontLarge);
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(gameMessage);
        int h = fm.getHeight();

        g2.drawString(gameMessage, (WIDTH - w)/2, (HEIGHT + h)/2 - 40);

        g2.setFont(fontSmall);
        g2.drawString("(Mouse moves left paddle • Arrows or AI controls right)", WIDTH/2 - 300, HEIGHT/2 + 60);
    }

    private void drawGameOverScreen(Graphics2D g2) {
        g2.setColor(new Color(0,0,0,160));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setColor(TEXT_HIGHLIGHT);
        g2.setFont(fontLarge);
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(gameMessage);
        g2.drawString(gameMessage, (WIDTH - w)/2, HEIGHT/2 - 40);

        g2.setFont(fontMedium);
        String finalScore = scorePlayer1 + " : " + scorePlayer2;
        w = fm.stringWidth(finalScore);
        g2.drawString(finalScore, (WIDTH - w)/2, HEIGHT/2 + 60);

        g2.setFont(fontSmall);
        g2.drawString("Press SPACE or click to play again", WIDTH/2 - 220, HEIGHT/2 + 140);
    }

    private void drawRocketOverlay(Graphics2D g2) {
        // Flashing background effect
        int alpha = 40 + (int)(Math.sin(System.currentTimeMillis() * 0.008) * 30);
        g2.setColor(new Color(255, 120, 40, alpha));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // "ROCKET MODE" text
        g2.setColor(new Color(255, 220, 60, 220));
        g2.setFont(new Font("Segoe UI Black", Font.BOLD, 68));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth("ROCKET MODE");
        g2.drawString("ROCKET MODE", (WIDTH - w)/2, 140);
    }

    // ================================================================
    //   INPUT HANDLING
    // ================================================================

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W) keyW = true;
        if (code == KeyEvent.VK_S) keyS = true;
        if (code == KeyEvent.VK_UP) keyUp = true;
        if (code == KeyEvent.VK_DOWN) keyDown = true;

        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_ENTER) {
            if (!gameRunning && !gameOver) {
                startNewRound();
            } else if (gameOver) {
                restartGame();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W) keyW = false;
        if (code == KeyEvent.VK_S) keyS = false;
        if (code == KeyEvent.VK_UP) keyUp = false;
        if (code == KeyEvent.VK_DOWN) keyDown = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseY = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseY = e.getY();
    }

    private void startNewRound() {
        gameRunning = true;
        gameMessage = "";

        // Serve from right or left side
        ball.x = servingPlayer == SERVE_RIGHT ? WIDTH * 0.65 : WIDTH * 0.35;
        ball.y = HEIGHT / 2.0;

        double serveAngle = (random.nextDouble() - 0.5) * 0.9;
        ball.vx = servingPlayer == SERVE_RIGHT ? -BALL_BASE_SPEED : BALL_BASE_SPEED;
        ball.vy = serveAngle * 3.2;

        ball.speed = BALL_BASE_SPEED;
    }

    private void restartGame() {
        scorePlayer1 = 0;
        scorePlayer2 = 0;
        gameOver = false;
        servingPlayer = SERVE_RIGHT;
        resetGameEntities();
        gameMessage = "Click or press SPACE to start";
    }

    // ================================================================
    //   INNER CLASSES – Game objects
    // ================================================================

    private static class Paddle {
        double x, y, targetY;
        final boolean isLeft;

        Paddle(double x, double y, boolean left) {
            this.x = x;
            this.y = y;
            this.targetY = y;
            this.isLeft = left;
        }

        void update() {
            double diff = targetY - y;
            y += diff * 0.38;   // smooth follow

            // Keep inside screen
            y = Math.max(40, Math.min(HEIGHT - 40 - PADDLE_HEIGHT, y));
        }
    }

    private static class Ball {
        double x, y, vx, vy, speed;

        Ball(double x, double y) {
            this.x = x;
            this.y = y;
            this.speed = BALL_BASE_SPEED;
            this.vx = 0;
            this.vy = 0;
        }

        void reset(double x, double y) {
            this.x = x;
            this.y = y;
            vx = 0;
            vy = 0;
            speed = BALL_BASE_SPEED;
        }
    }

    private static class TrailPoint {
        double x, y;
        double speed;

        TrailPoint(double x, double y, double speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
        }
    }

    private static class RocketParticle {
        double x, y, vx, vy;
        Color color;
        int life;
        int maxLife;
        double size;

        RocketParticle(double x, double y, double vx, double vy, Color c, int life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = c;
            this.life = life;
            this.maxLife = life;
            this.size = 8 + random.nextDouble() * 12;
        }

        void update() {
            x += vx;
            y += vy;
            life--;

            // Slow down & fade
            vx *= 0.96;
            vy *= 0.96;
            vy += 0.08; // light gravity
        }
    }
}