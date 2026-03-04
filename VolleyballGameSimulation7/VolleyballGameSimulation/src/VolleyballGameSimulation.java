import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Random;

public class VolleyballGameSimulation extends JFrame implements Runnable {

    // Game constants
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final int GROUND_Y = 700;
    private static final int NET_X = 600;
    private static final int NET_HEIGHT = 200;
    private static final int GRAVITY = 1;
    private static final double FRICTION = 0.98;
    private static final double BOUNCE_DAMPING = 0.8;

    // Game state
    private volatile boolean running = true;
    private boolean paused = false;
    private int playerScore = 0;
    private int aiScore = 0;
    private int gameState = 0; // 0: menu, 1: playing, 2: game over
    private int rallyCount = 0;
    private int maxRallies = 21;

    // Game objects
    private Ball ball;
    private Player player;
    private AIPlayer aiPlayer;
    private Net net;
    private ArrayList<Particle> particles;
    private ArrayList<Cloud> clouds;
    private ArrayList<ScorePopup> scorePopups;

    // Input handling
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean spacePressed = false;

    // Rendering
    private GamePanel gamePanel;
    private BufferStrategy bufferStrategy;
    private double deltaTime = 0;
    private long lastTime = 0;
    private int fps = 0;
    private int frameCount = 0;
    private long lastFpsTime = 0;

    // Random for effects
    private Random random = new Random();

    // Colors
    private static final Color SKY_TOP = new Color(135, 206, 235);
    private static final Color SKY_BOTTOM = new Color(255, 255, 255);
    private static final Color SAND_COLOR = new Color(238, 214, 175);
    private static final Color SAND_DARK = new Color(218, 194, 155);
    private static final Color NET_COLOR = new Color(255, 255, 255, 180);
    private static final Color BALL_COLOR = new Color(255, 100, 50);

    public VolleyballGameSimulation() {
        setTitle("Volleyball Championship Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        add(gamePanel);

        pack();
        setLocationRelativeTo(null);

        initializeGame();
        setupInput();

        setVisible(true);
        gamePanel.createBufferStrategy(2);
        bufferStrategy = gamePanel.getBufferStrategy();

        lastTime = System.nanoTime();
        new Thread(this).start();
    }

    private void initializeGame() {
        ball = new Ball(200, 300);
        player = new Player(150, GROUND_Y - 100, true);
        aiPlayer = new AIPlayer(850, GROUND_Y - 100, false);
        net = new Net(NET_X, GROUND_Y - NET_HEIGHT);
        particles = new ArrayList<>();
        clouds = new ArrayList<>();
        scorePopups = new ArrayList<>();

        // Initialize clouds
        for (int i = 0; i < 5; i++) {
            clouds.add(new Cloud(random.nextInt(WIDTH), random.nextInt(200)));
        }
    }

    private void setupInput() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT: leftPressed = true; break;
                    case KeyEvent.VK_RIGHT: rightPressed = true; break;
                    case KeyEvent.VK_UP: upPressed = true; break;
                    case KeyEvent.VK_SPACE: spacePressed = true; break;
                    case KeyEvent.VK_P: paused = !paused; break;
                    case KeyEvent.VK_R: if (gameState == 2) resetGame(); break;
                    case KeyEvent.VK_ENTER: if (gameState == 0) gameState = 1; break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getID()) {
                    case KeyEvent.VK_LEFT: leftPressed = false; break;
                    case KeyEvent.VK_RIGHT: rightPressed = false; break;
                    case KeyEvent.VK_UP: upPressed = false; break;
                    case KeyEvent.VK_SPACE: spacePressed = false; break;
                }
            }
        });

        gamePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gameState == 0 && e.getButton() == MouseEvent.BUTTON1) {
                    gameState = 1;
                } else if (gameState == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    resetGame();
                }
            }
        });

        gamePanel.setFocusable(true);
        gamePanel.requestFocusInWindow();
    }

    private void resetGame() {
        playerScore = 0;
        aiScore = 0;
        rallyCount = 0;
        ball.reset();
        player.reset();
        aiPlayer.reset();
        gameState = 1;
        particles.clear();
        scorePopups.clear();
    }

    @Override
    public void run() {
        while (running) {
            long now = System.nanoTime();
            deltaTime = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;

            if (!paused && gameState == 1) {
                update(deltaTime);
            }

            render();

            // FPS calculation
            frameCount++;
            if (now - lastFpsTime >= 1_000_000_000) {
                fps = frameCount;
                frameCount = 0;
                lastFpsTime = now;
            }

            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update(double dt) {
        // Update clouds
        for (Cloud cloud : clouds) {
            cloud.update(dt);
        }

        // Update player
        player.update(dt);
        if (leftPressed) player.moveLeft();
        if (rightPressed) player.moveRight();
        if (upPressed && player.onGround) player.jump();
        if (spacePressed) player.attemptHit(ball);

        // Update AI
        aiPlayer.update(dt, ball);

        // Update ball physics
        ball.update(dt);

        // Check collisions
        checkCollisions();

        // Update particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            particles.get(i).update(dt);
            if (particles.get(i).isDead()) {
                particles.remove(i);
            }
        }

        // Update score popups
        for (int i = scorePopups.size() - 1; i >= 0; i--) {
            scorePopups.get(i).update(dt);
            if (scorePopups.get(i).isDead()) {
                scorePopups.remove(i);
            }
        }

        // Check win condition
        if (playerScore >= maxRallies || aiScore >= maxRallies) {
            gameState = 2;
        }
    }

    private void checkCollisions() {
        // Ball with ground
        if (ball.y + ball.radius >= GROUND_Y) {
            ball.y = GROUND_Y - ball.radius;
            ball.vy *= -BOUNCE_DAMPING;
            ball.vx *= FRICTION;

            createDustEffect(ball.x, GROUND_Y);

            // Score detection
            if (Math.abs(ball.vy) < 5) {
                if (ball.x < NET_X) {
                    aiScore++;
                    scorePopups.add(new ScorePopup("AI SCORES!", NET_X + 100, 200, Color.RED));
                } else {
                    playerScore++;
                    scorePopups.add(new ScorePopup("YOU SCORE!", NET_X - 100, 200, Color.GREEN));
                }
                ball.reset();
                rallyCount++;
            }
        }

        // Ball with net
        if (ball.x + ball.radius > NET_X - 10 && ball.x - ball.radius < NET_X + 10) {
            if (ball.y + ball.radius > GROUND_Y - NET_HEIGHT) {
                ball.vx *= -0.5;
                ball.x = ball.x < NET_X ? NET_X - 10 - ball.radius : NET_X + 10 + ball.radius;
                createSparkEffect(ball.x, ball.y);
            }
        }

        // Ball with player
        if (checkBallPlayerCollision(ball, player)) {
            handleBallHit(ball, player);
        }

        // Ball with AI
        if (checkBallPlayerCollision(ball, aiPlayer)) {
            handleBallHit(ball, aiPlayer);
        }

        // Keep ball in bounds
        if (ball.x < ball.radius) {
            ball.x = ball.radius;
            ball.vx *= -BOUNCE_DAMPING;
        }
        if (ball.x > WIDTH - ball.radius) {
            ball.x = WIDTH - ball.radius;
            ball.vx *= -BOUNCE_DAMPING;
        }
    }

    private boolean checkBallPlayerCollision(Ball ball, Player p) {
        double dx = ball.x - (p.x + p.width/2);
        double dy = ball.y - (p.y + p.height/2);
        double distance = Math.sqrt(dx*dx + dy*dy);
        return distance < (ball.radius + Math.max(p.width, p.height)/2);
    }

    private void handleBallHit(Ball ball, Player p) {
        double dx = ball.x - (p.x + p.width/2);
        double dy = ball.y - (p.y + p.height/2);

        ball.vx = dx * 0.3 + (p.isMovingRight() ? 10 : p.isMovingLeft() ? -10 : 0);
        ball.vy = dy * 0.3 - 15;

        // Add some randomness
        ball.vx += (random.nextDouble() - 0.5) * 5;

        createHitEffect(ball.x, ball.y, p.isPlayer ? Color.CYAN : Color.ORANGE);
    }

    private void createDustEffect(double x, double y) {
        for (int i = 0; i < 10; i++) {
            particles.add(new Particle(x, y,
                    (random.nextDouble() - 0.5) * 10,
                    -random.nextDouble() * 5,
                    SAND_DARK, 30));
        }
    }

    private void createSparkEffect(double x, double y) {
        for (int i = 0; i < 15; i++) {
            particles.add(new Particle(x, y,
                    (random.nextDouble() - 0.5) * 20,
                    (random.nextDouble() - 0.5) * 20,
                    Color.YELLOW, 20));
        }
    }

    private void createHitEffect(double x, double y, Color c) {
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(x, y,
                    (random.nextDouble() - 0.5) * 15,
                    (random.nextDouble() - 0.5) * 15,
                    c, 25));
        }
    }

    private void render() {
        Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();

        // Clear screen with gradient
        GradientPaint skyGradient = new GradientPaint(0, 0, SKY_TOP, 0, HEIGHT, SKY_BOTTOM);
        g.setPaint(skyGradient);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw clouds
        for (Cloud cloud : clouds) {
            cloud.draw(g);
        }

        // Draw court
        drawCourt(g);

        // Draw game objects
        net.draw(g);
        player.draw(g);
        aiPlayer.draw(g);
        ball.draw(g);

        // Draw effects
        for (Particle p : particles) {
            p.draw(g);
        }

        for (ScorePopup sp : scorePopups) {
            sp.draw(g);
        }

        // Draw UI
        drawUI(g);

        // Draw overlays based on game state
        if (gameState == 0) {
            drawMenu(g);
        } else if (gameState == 2) {
            drawGameOver(g);
        }

        if (paused) {
            drawPauseOverlay(g);
        }

        g.dispose();
        bufferStrategy.show();
    }

    private void drawCourt(Graphics2D g) {
        // Sand
        g.setColor(SAND_COLOR);
        g.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        // Sand texture
        g.setColor(SAND_DARK);
        for (int i = 0; i < 100; i++) {
            int x = (i * 137) % WIDTH;
            int y = GROUND_Y + (i * 53) % (HEIGHT - GROUND_Y);
            g.fillOval(x, y, 3, 2);
        }

        // Court lines
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(5));
        g.drawLine(0, GROUND_Y, WIDTH, GROUND_Y);
        g.drawLine(NET_X, GROUND_Y, NET_X, GROUND_Y - NET_HEIGHT);

        // Center line
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10, 10}, 0));
        g.drawLine(NET_X, GROUND_Y, NET_X, HEIGHT);

        // Shadows
        g.setColor(new Color(0, 0, 0, 50));
        g.fillOval((int)ball.x - 20, GROUND_Y - 5, 40, 10);
        g.fillOval(player.x, GROUND_Y - 5, player.width, 10);
        g.fillOval(aiPlayer.x, GROUND_Y - 5, aiPlayer.width, 10);
    }

    private void drawUI(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Score board
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(WIDTH/2 - 150, 20, 300, 80, 20, 20);

        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.setColor(Color.WHITE);
        String score = playerScore + " - " + aiScore;
        FontMetrics fm = g.getFontMetrics();
        int scoreWidth = fm.stringWidth(score);
        g.drawString(score, WIDTH/2 - scoreWidth/2, 75);

        // Labels
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(Color.CYAN);
        g.drawString("YOU", WIDTH/2 - 120, 50);
        g.setColor(Color.ORANGE);
        g.drawString("AI", WIDTH/2 + 90, 50);

        // Rally counter
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("Rally: " + rallyCount, 20, 30);

        // FPS
        g.drawString("FPS: " + fps, 20, 50);

        // Controls hint
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Controls: ARROWS to move, SPACE to hit, P to pause", 20, HEIGHT - 20);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("Arial", Font.BOLD, 72));
        g.setColor(Color.WHITE);
        String title = "VOLLEYBALL CHAMPIONSHIP";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, WIDTH/2 - fm.stringWidth(title)/2, 300);

        g.setFont(new Font("Arial", Font.PLAIN, 36));
        g.setColor(Color.YELLOW);
        String subtitle = "Click or Press ENTER to Start";
        fm = g.getFontMetrics();
        g.drawString(subtitle, WIDTH/2 - fm.stringWidth(subtitle)/2, 400);

        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.setColor(Color.WHITE);
        String instructions = "First to 21 points wins!";
        fm = g.getFontMetrics();
        g.drawString(instructions, WIDTH/2 - fm.stringWidth(instructions)/2, 500);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("Arial", Font.BOLD, 72));

        boolean playerWon = playerScore >= maxRallies;
        g.setColor(playerWon ? Color.GREEN : Color.RED);
        String result = playerWon ? "YOU WIN!" : "AI WINS!";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(result, WIDTH/2 - fm.stringWidth(result)/2, 350);

        g.setFont(new Font("Arial", Font.PLAIN, 36));
        g.setColor(Color.WHITE);
        String restart = "Press R or Click to Restart";
        fm = g.getFontMetrics();
        g.drawString(restart, WIDTH/2 - fm.stringWidth(restart)/2, 450);

        g.setFont(new Font("Arial", Font.PLAIN, 24));
        String finalScore = "Final Score: " + playerScore + " - " + aiScore;
        fm = g.getFontMetrics();
        g.drawString(finalScore, WIDTH/2 - fm.stringWidth(finalScore)/2, 520);
    }

    private void drawPauseOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setFont(new Font("Arial", Font.BOLD, 72));
        g.setColor(Color.WHITE);
        String pause = "PAUSED";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(pause, WIDTH/2 - fm.stringWidth(pause)/2, HEIGHT/2);
    }

    // Inner classes

    class Ball {
        double x, y, vx, vy;
        int radius = 20;
        double rotation = 0;

        Ball(double x, double y) {
            this.x = x;
            this.y = y;
            this.vx = 5;
            this.vy = 0;
        }

        void update(double dt) {
            vy += GRAVITY;
            x += vx;
            y += vy;
            rotation += vx * 0.1;

            // Air resistance
            vx *= 0.999;
            vy *= 0.999;
        }

        void reset() {
            x = random.nextBoolean() ? 200 : 1000;
            y = 300;
            vx = (NET_X - x) * 0.01;
            vy = -10;
        }

        void draw(Graphics2D g) {
            g.translate(x, y);
            g.rotate(rotation);

            // Ball body
            g.setColor(BALL_COLOR);
            g.fillOval(-radius, -radius, radius*2, radius*2);

            // Ball pattern
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3));
            g.drawArc(-radius+5, -radius+5, (radius-5)*2, (radius-5)*2, 0, 180);
            g.drawArc(-radius+5, -radius+5, (radius-5)*2, (radius-5)*2, 180, 180);

            // Highlight
            g.setColor(new Color(255, 255, 255, 100));
            g.fillOval(-radius+5, -radius+5, radius, radius/2);

            g.rotate(-rotation);
            g.translate(-x, -y);
        }
    }

    class Player {
        double x, y;
        double vx, vy;
        int width = 60, height = 100;
        boolean onGround = false;
        boolean isPlayer;
        int animFrame = 0;
        boolean facingRight = true;

        Player(double x, double y, boolean isPlayer) {
            this.x = x;
            this.y = y;
            this.isPlayer = isPlayer;
        }

        void update(double dt) {
            vy += GRAVITY * 2;
            x += vx;
            y += vy;

            // Ground collision
            if (y + height >= GROUND_Y) {
                y = GROUND_Y - height;
                vy = 0;
                onGround = true;
            } else {
                onGround = false;
            }

            vx *= FRICTION;

            // Animation
            if (Math.abs(vx) > 0.5) {
                animFrame++;
            } else {
                animFrame = 0;
            }

            // Boundary constraints
            if (isPlayer) {
                if (x < 0) x = 0;
                if (x > NET_X - width - 10) x = NET_X - width - 10;
            } else {
                if (x < NET_X + 10) x = NET_X + 10;
                if (x > WIDTH - width) x = WIDTH - width;
            }
        }

        void moveLeft() {
            vx -= 2;
            facingRight = false;
        }

        void moveRight() {
            vx += 2;
            facingRight = true;
        }

        void jump() {
            if (onGround) {
                vy = -25;
                onGround = false;
                createDustEffect(x + width/2, y + height);
            }
        }

        void attemptHit(Ball ball) {
            double dx = ball.x - (x + width/2);
            double dy = ball.y - (y + height/2);
            double dist = Math.sqrt(dx*dx + dy*dy);

            if (dist < 100 && ball.y < y + height) {
                ball.vx = dx * 0.4 + (facingRight ? 15 : -15);
                ball.vy = -20;
                createHitEffect(ball.x, ball.y, isPlayer ? Color.CYAN : Color.ORANGE);
            }
        }

        boolean isMovingRight() { return vx > 1; }
        boolean isMovingLeft() { return vx < -1; }

        void reset() {
            x = isPlayer ? 150 : 850;
            y = GROUND_Y - height;
            vx = vy = 0;
        }

        void draw(Graphics2D g) {
            // Shadow
            g.setColor(new Color(0, 0, 0, 50));
            g.fillOval((int)x, GROUND_Y - 5, width, 10);

            // Body
            g.setColor(isPlayer ? new Color(100, 200, 255) : new Color(255, 150, 100));
            g.fillRoundRect((int)x, (int)y, width, height, 10, 10);

            // Head
            int headY = (int)y - 30 + (animFrame % 20 < 10 ? 0 : 2);
            g.setColor(new Color(255, 220, 180));
            g.fillOval((int)x + 10, headY, 40, 40);

            // Eyes
            g.setColor(Color.BLACK);
            if (facingRight) {
                g.fillOval((int)x + 30, headY + 12, 6, 6);
                g.fillOval((int)x + 38, headY + 12, 6, 6);
            } else {
                g.fillOval((int)x + 16, headY + 12, 6, 6);
                g.fillOval((int)x + 24, headY + 12, 6, 6);
            }

            // Arms
            g.setColor(isPlayer ? new Color(80, 180, 235) : new Color(235, 130, 80));
            int armOffset = (animFrame % 20 < 10) ? 0 : -10;
            g.fillRoundRect((int)x - 10, (int)y + 20 + armOffset, 15, 50, 5, 5);
            g.fillRoundRect((int)x + width - 5, (int)y + 20 - armOffset, 15, 50, 5, 5);
        }
    }

    class AIPlayer extends Player {
        double targetX;
        int reactionDelay = 0;
        int difficulty = 3; // 1-5

        AIPlayer(double x, double y, boolean isPlayer) {
            super(x, y, isPlayer);
        }

        void update(double dt, Ball ball) {
            super.update(dt);

            if (reactionDelay > 0) {
                reactionDelay--;
                return;
            }

            // AI Strategy
            if (ball.x > NET_X) {
                // Ball on AI side
                targetX = ball.x - width/2;

                // Predict where ball will land
                if (ball.vy > 0) {
                    double timeToGround = (GROUND_Y - ball.y) / ball.vy;
                    double predictedX = ball.x + ball.vx * timeToGround;
                    targetX = predictedX - width/2;
                }

                // Move towards target
                if (Math.abs(x - targetX) > 10) {
                    if (x < targetX) moveRight();
                    else moveLeft();
                }

                // Jump if ball is high and close
                if (ball.y < y && Math.abs(ball.x - (x + width/2)) < 100 && onGround) {
                    if (random.nextInt(10) < difficulty * 2) {
                        jump();
                    }
                }

                // Hit ball if close
                if (Math.abs(ball.x - (x + width/2)) < 80 &&
                        Math.abs(ball.y - (y + height/2)) < 80 &&
                        ball.y < y + height) {
                    if (random.nextInt(10) < difficulty * 2) {
                        attemptHit(ball);
                        reactionDelay = 20 - difficulty * 3;
                    }
                }
            } else {
                // Return to center position
                targetX = 850;
                if (Math.abs(x - targetX) > 50) {
                    if (x < targetX) moveRight();
                    else moveLeft();
                }
            }
        }
    }

    class Net {
        int x, y;

        Net(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g) {
            // Poles
            g.setColor(new Color(100, 100, 100));
            g.fillRect(x - 15, y, 10, GROUND_Y - y);
            g.fillRect(x + 5, y, 10, GROUND_Y - y);

            // Net mesh
            g.setColor(NET_COLOR);
            for (int i = 0; i < NET_HEIGHT; i += 10) {
                g.drawLine(x - 10, y + i, x + 10, y + i);
            }
            for (int i = -10; i <= 10; i += 5) {
                g.drawLine(x + i, y, x + i, GROUND_Y);
            }

            // Top tape
            g.setColor(Color.WHITE);
            g.fillRect(x - 12, y - 5, 24, 10);
        }
    }

    class Particle {
        double x, y, vx, vy;
        Color color;
        int life, maxLife;
        double size;

        Particle(double x, double y, double vx, double vy, Color c, int life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = c;
            this.life = life;
            this.maxLife = life;
            this.size = random.nextInt(5) + 3;
        }

        void update(double dt) {
            x += vx;
            y += vy;
            vy += 0.5; // gravity
            life--;
            size *= 0.95;
        }

        boolean isDead() { return life <= 0; }

        void draw(Graphics2D g) {
            int alpha = (int)(255 * ((double)life / maxLife));
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.fillOval((int)x, (int)y, (int)size, (int)size);
        }
    }

    class Cloud {
        double x, y;
        double speed;
        int size;

        Cloud(double x, double y) {
            this.x = x;
            this.y = y;
            this.speed = random.nextDouble() * 0.5 + 0.1;
            this.size = random.nextInt(30) + 40;
        }

        void update(double dt) {
            x += speed;
            if (x > WIDTH + 100) x = -100;
        }

        void draw(Graphics2D g) {
            g.setColor(new Color(255, 255, 255, 180));
            g.fillOval((int)x, (int)y, size, size/2);
            g.fillOval((int)x + size/3, (int)y - size/4, size/2, size/2);
            g.fillOval((int)x - size/4, (int)y, size/2, size/3);
        }
    }

    class ScorePopup {
        String text;
        double x, y;
        Color color;
        int life = 60;
        double vy = -2;

        ScorePopup(String text, double x, double y, Color c) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = c;
        }

        void update(double dt) {
            y += vy;
            life--;
        }

        boolean isDead() { return life <= 0; }

        void draw(Graphics2D g) {
            float alpha = life / 60.0f;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255 * alpha)));
            g.setFont(new Font("Arial", Font.BOLD, 36));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (int)x - fm.stringWidth(text)/2, (int)y);
        }
    }

    class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VolleyballGameSimulation();
        });
    }
}