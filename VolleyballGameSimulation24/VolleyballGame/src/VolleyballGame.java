import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class VolleyballGame extends JPanel implements Runnable, KeyListener {
    // Game constants
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final int GROUND_Y = 700;
    private static final int NET_X = WIDTH / 2;
    private static final int NET_HEIGHT = 200;
    private static final int NET_WIDTH = 20;
    private static final int PLAYER_WIDTH = 60;
    private static final int PLAYER_HEIGHT = 100;
    private static final int BALL_RADIUS = 25;
    private static final int GRAVITY = 1;
    private static final int JUMP_STRENGTH = -22;
    private static final int MOVE_SPEED = 8;

    // Game state
    private boolean running = true;
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private int scoreLeft = 0;
    private int scoreRight = 0;
    private int winningScore = 10;
    private boolean vsComputer = true;

    // Entities
    private Player player1;
    private Player player2;
    private Ball ball;
    private ParticleSystem particles;

    // Animation
    private float time = 0;
    private BufferedImage buffer;
    private Thread gameThread;

    // Input
    private boolean[] keys = new boolean[256];

    public VolleyballGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky blue
        setFocusable(true);
        addKeyListener(this);

        initGame();

        gameThread = new Thread(this);
        gameThread.start();
    }

    private void initGame() {
        player1 = new Player(200, GROUND_Y - PLAYER_HEIGHT, Color.BLUE, true);
        player2 = new Player(WIDTH - 260, GROUND_Y - PLAYER_HEIGHT, Color.RED, false);
        ball = new Ball(NET_X, 300);
        particles = new ParticleSystem();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60.0;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            while (delta >= 1) {
                update();
                delta--;
            }

            repaint();

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        time += 0.05f;

        if (!gameStarted || gameOver) return;

        // Player 1 controls (WASD)
        if (keys[KeyEvent.VK_A]) player1.moveLeft();
        if (keys[KeyEvent.VK_D]) player1.moveRight();
        if (keys[KeyEvent.VK_W]) player1.jump();

        // Player 2 controls (Arrow keys) or AI
        if (!vsComputer) {
            if (keys[KeyEvent.VK_LEFT]) player2.moveLeft();
            if (keys[KeyEvent.VK_RIGHT]) player2.moveRight();
            if (keys[KeyEvent.VK_UP]) player2.jump();
        } else {
            updateAI();
        }

        // Update entities
        player1.update();
        player2.update();
        ball.update();
        particles.update();

        // Check collisions
        checkCollisions();

        // Check scoring
        checkScore();
    }

    private void updateAI() {
        // Simple AI for computer player
        float ballX = ball.x;
        float ballY = ball.y;
        float aiX = player2.x + PLAYER_WIDTH / 2;

        // Only move if ball is on AI's side or approaching
        if (ballX > NET_X - 100 || ball.vx > 0) {
            float diff = ballX - aiX;

            if (Math.abs(diff) > 30) {
                if (diff > 0) player2.moveRight();
                else player2.moveLeft();
            }

            // Jump if ball is above and close
            if (ballY < player2.y && Math.abs(ballX - aiX) < 150 && player2.onGround) {
                if (Math.random() < 0.1) player2.jump();
            }
        } else {
            // Return to center position
            float centerX = WIDTH - 300;
            float diff = centerX - player2.x;
            if (Math.abs(diff) > 50) {
                if (diff > 0) player2.moveRight();
                else player2.moveLeft();
            }
        }
    }

    private void checkCollisions() {
        // Ball with ground
        if (ball.y + BALL_RADIUS >= GROUND_Y) {
            ball.y = GROUND_Y - BALL_RADIUS;
            ball.vy = -ball.vy * 0.8f;
            ball.vx *= 0.95f;

            // Create impact particles
            for (int i = 0; i < 10; i++) {
                particles.addParticle(ball.x, ball.y + BALL_RADIUS,
                        (float)(Math.random() - 0.5) * 10,
                        (float)(Math.random() - 1) * 5,
                        Color.YELLOW);
            }
        }

        // Ball with net
        if (ball.x + BALL_RADIUS > NET_X - NET_WIDTH/2 &&
                ball.x - BALL_RADIUS < NET_X + NET_WIDTH/2 &&
                ball.y + BALL_RADIUS > GROUND_Y - NET_HEIGHT) {

            if (ball.y < GROUND_Y - NET_HEIGHT + 20) {
                ball.vy = -Math.abs(ball.vy) * 0.5f;
                ball.y = GROUND_Y - NET_HEIGHT - BALL_RADIUS;
            } else {
                ball.vx = -ball.vx * 0.8f;
                if (ball.x < NET_X) ball.x = NET_X - NET_WIDTH/2 - BALL_RADIUS;
                else ball.x = NET_X + NET_WIDTH/2 + BALL_RADIUS;
            }
        }

        // Ball with players
        checkPlayerCollision(player1);
        checkPlayerCollision(player2);
    }

    private void checkPlayerCollision(Player player) {
        float dx = ball.x - (player.x + PLAYER_WIDTH/2);
        float dy = ball.y - (player.y + PLAYER_HEIGHT/2);
        float distance = (float)Math.sqrt(dx*dx + dy*dy);

        if (distance < BALL_RADIUS + PLAYER_WIDTH/2 + 10) {
            // Calculate hit direction
            float angle = (float)Math.atan2(dy, dx);
            float speed = 15;

            ball.vx = (float)Math.cos(angle) * speed;
            ball.vy = (float)Math.sin(angle) * speed - 5;

            // Add some randomness for fun
            ball.vx += (float)(Math.random() - 0.5) * 5;

            // Create hit particles
            Color particleColor = player.isPlayer1 ? new Color(100, 150, 255) : new Color(255, 100, 100);
            for (int i = 0; i < 15; i++) {
                particles.addParticle(ball.x, ball.y,
                        (float) ((float)Math.cos(angle) * (5 + Math.random() * 5)),
                        (float) ((float)Math.sin(angle) * (5 + Math.random() * 5)),
                        particleColor);
            }

            // Ensure minimum upward velocity
            if (ball.vy > -5) ball.vy = -8;
        }
    }

    private void checkScore() {
        // Ball hits ground on left side
        if (ball.y + BALL_RADIUS >= GROUND_Y - 5 && ball.x < NET_X) {
            scoreRight++;
            createScoreEffect(ball.x, ball.y, false);
            resetRound();
        }
        // Ball hits ground on right side
        else if (ball.y + BALL_RADIUS >= GROUND_Y - 5 && ball.x > NET_X) {
            scoreLeft++;
            createScoreEffect(ball.x, ball.y, true);
            resetRound();
        }

        // Check win condition
        if (scoreLeft >= winningScore || scoreRight >= winningScore) {
            gameOver = true;
        }
    }

    private void createScoreEffect(float x, float y, boolean leftScored) {
        Color color = leftScored ? Color.BLUE : Color.RED;
        for (int i = 0; i < 30; i++) {
            particles.addParticle(x, y,
                    (float)(Math.random() - 0.5) * 15,
                    (float)(Math.random() - 1) * 10,
                    color);
        }
    }

    private void resetRound() {
        ball.x = NET_X;
        ball.y = 200;
        ball.vx = (float)(Math.random() - 0.5) * 10;
        ball.vy = 0;

        player1.x = 200;
        player1.y = GROUND_Y - PLAYER_HEIGHT;
        player1.vx = 0;
        player1.vy = 0;

        player2.x = WIDTH - 260;
        player2.y = GROUND_Y - PLAYER_HEIGHT;
        player2.vx = 0;
        player2.vy = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background gradient
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, HEIGHT, new Color(255, 255, 200));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw animated clouds
        drawClouds(g2d);

        // Draw court
        drawCourt(g2d);

        // Draw net with animation
        drawNet(g2d);

        // Draw entities
        particles.draw(g2d);
        player1.draw(g2d);
        player2.draw(g2d);
        ball.draw(g2d);

        // Draw UI
        drawUI(g2d);

        // Draw start/game over screens
        if (!gameStarted) drawStartScreen(g2d);
        if (gameOver) drawGameOverScreen(g2d);
    }

    private void drawClouds(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 180));
        int cloudOffset = (int)(time * 2) % (WIDTH + 200);

        // Cloud 1
        drawCloud(g2d, 100 + cloudOffset, 100, 80);
        drawCloud(g2d, (int) (400 + cloudOffset * 0.7), 150, 60);
        drawCloud(g2d, (int) (800 + cloudOffset * 0.5), 80, 100);

        // Wrap around clouds
        drawCloud(g2d, 100 + cloudOffset - WIDTH - 200, 100, 80);
    }

    private void drawCloud(Graphics2D g2d, int x, int y, int size) {
        g2d.fillOval(x, y, size, (int) (size * 0.6));
        g2d.fillOval((int) (x + size * 0.3), (int) (y - size * 0.2), (int) (size * 0.8), (int) (size * 0.6));
        g2d.fillOval((int) (x + size * 0.6), y, (int) (size * 0.7), (int) (size * 0.5));
    }

    private void drawCourt(Graphics2D g2d) {
        // Sand gradient
        GradientPaint sandGradient = new GradientPaint(0, GROUND_Y, new Color(238, 203, 140),
                0, HEIGHT, new Color(194, 154, 108));
        g2d.setPaint(sandGradient);
        g2d.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        // Court lines
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(5));
        g2d.drawLine(0, GROUND_Y, WIDTH, GROUND_Y);
        g2d.drawLine(NET_X, GROUND_Y, NET_X, GROUND_Y - 10);

        // Shadow under net
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillRect(NET_X - NET_WIDTH/2, GROUND_Y - 5, NET_WIDTH, 10);
    }

    private void drawNet(Graphics2D g2d) {
        int netTop = GROUND_Y - NET_HEIGHT;

        // Net posts
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect(NET_X - NET_WIDTH/2 - 5, netTop, 10, NET_HEIGHT);
        g2d.fillRect(NET_X + NET_WIDTH/2 - 5, netTop, 10, NET_HEIGHT);

        // Net mesh with wave animation
        g2d.setColor(new Color(255, 255, 255, 200));
        for (int y = netTop; y < GROUND_Y; y += 15) {
            float wave = (float)Math.sin(time * 2 + y * 0.05) * 3;
            g2d.drawLine(NET_X - NET_WIDTH/2, y, NET_X + NET_WIDTH/2, (int)(y + wave));
        }
        for (int x = NET_X - NET_WIDTH/2; x <= NET_X + NET_WIDTH/2; x += 10) {
            float wave = (float)Math.sin(time * 3 + x * 0.1) * 2;
            g2d.drawLine(x, netTop, x, GROUND_Y + (int)wave);
        }

        // Net top tape
        g2d.setColor(Color.WHITE);
        g2d.fillRect(NET_X - NET_WIDTH/2 - 5, netTop - 5, NET_WIDTH + 10, 10);
    }

    private void drawUI(Graphics2D g2d) {
        // Scoreboard background
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRoundRect(WIDTH/2 - 150, 20, 300, 80, 20, 20);

        // Scores
        g2d.setFont(new Font("Arial", Font.BOLD, 48));

        // Player 1 score (Blue)
        g2d.setColor(Color.BLUE);
        String score1 = String.valueOf(scoreLeft);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(score1, WIDTH/2 - 80 - fm.stringWidth(score1)/2, 80);

        // Divider
        g2d.setColor(Color.WHITE);
        g2d.drawString("-", WIDTH/2 - 10, 80);

        // Player 2 score (Red)
        g2d.setColor(Color.RED);
        String score2 = String.valueOf(scoreRight);
        g2d.drawString(score2, WIDTH/2 + 80 - fm.stringWidth(score2)/2, 80);

        // Player labels
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.BLUE);
        g2d.drawString("PLAYER 1 (WASD)", 50, 50);
        g2d.setColor(vsComputer ? Color.ORANGE : Color.RED);
        g2d.drawString(vsComputer ? "COMPUTER" : "PLAYER 2 (ARROWS)", WIDTH - 200, 50);

        // Mode indicator
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Press 'M' to toggle mode: " + (vsComputer ? "vs Computer" : "2 Players"), 50, HEIGHT - 30);
    }

    private void drawStartScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        g2d.setColor(Color.WHITE);
        String title = "BEACH VOLLEYBALL";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, WIDTH/2 - fm.stringWidth(title)/2, 300);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String subtitle = "Press SPACE to Start";
        fm = g2d.getFontMetrics();
        g2d.setColor(Color.YELLOW);
        g2d.drawString(subtitle, WIDTH/2 - fm.stringWidth(subtitle)/2, 400);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        String controls1 = "Player 1: W (Jump), A (Left), D (Right)";
        String controls2 = vsComputer ? "Playing against Computer" : "Player 2: UP (Jump), LEFT/RIGHT (Move)";
        String mode = "Press 'M' to toggle: 2 Players vs Computer";

        g2d.drawString(controls1, WIDTH/2 - 150, 500);
        g2d.drawString(controls2, WIDTH/2 - 150, 530);
        g2d.drawString(mode, WIDTH/2 - 150, 580);
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setFont(new Font("Arial", Font.BOLD, 64));
        g2d.setColor(Color.WHITE);
        String winner = scoreLeft > scoreRight ? "PLAYER 1 WINS!" :
                (vsComputer ? "COMPUTER WINS!" : "PLAYER 2 WINS!");
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(winner, WIDTH/2 - fm.stringWidth(winner)/2, 350);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.setColor(Color.YELLOW);
        String restart = "Press SPACE to Play Again";
        fm = g2d.getFontMetrics();
        g2d.drawString(restart, WIDTH/2 - fm.stringWidth(restart)/2, 450);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        String finalScore = "Final Score: " + scoreLeft + " - " + scoreRight;
        g2d.drawString(finalScore, WIDTH/2 - 60, 500);
    }

    // KeyListener methods
    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (!gameStarted) {
                gameStarted = true;
                gameOver = false;
                scoreLeft = 0;
                scoreRight = 0;
                resetRound();
            } else if (gameOver) {
                gameOver = false;
                scoreLeft = 0;
                scoreRight = 0;
                resetRound();
            }
        }

        if (e.getKeyCode() == KeyEvent.VK_M && !gameStarted) {
            vsComputer = !vsComputer;
            repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner classes
    class Player {
        float x, y;
        float vx, vy;
        Color color;
        boolean isPlayer1;
        boolean onGround;
        private float animOffset;

        public Player(float x, float y, Color color, boolean isPlayer1) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.isPlayer1 = isPlayer1;
            this.onGround = true;
        }

        public void moveLeft() {
            vx = -MOVE_SPEED;
        }

        public void moveRight() {
            vx = MOVE_SPEED;
        }

        public void jump() {
            if (onGround) {
                vy = JUMP_STRENGTH;
                onGround = false;
                // Jump particles
                for (int i = 0; i < 8; i++) {
                    particles.addParticle(x + PLAYER_WIDTH/2, y + PLAYER_HEIGHT,
                            (float)(Math.random() - 0.5) * 8,
                            (float)(Math.random() * -3),
                            new Color(194, 154, 108));
                }
            }
        }

        public void update() {
            // Apply gravity
            vy += GRAVITY;

            // Update position
            x += vx;
            y += vy;

            // Friction
            vx *= 0.8;

            // Ground collision
            if (y + PLAYER_HEIGHT >= GROUND_Y) {
                y = GROUND_Y - PLAYER_HEIGHT;
                vy = 0;
                onGround = true;
            }

            // Wall collision
            if (x < 0) x = 0;
            if (x > NET_X - PLAYER_WIDTH - NET_WIDTH/2 && isPlayer1)
                x = NET_X - PLAYER_WIDTH - NET_WIDTH/2;
            if (x > WIDTH - PLAYER_WIDTH) x = WIDTH - PLAYER_WIDTH;
            if (x < NET_X + NET_WIDTH/2 && !isPlayer1)
                x = NET_X + NET_WIDTH/2;

            // Animation
            if (Math.abs(vx) > 1) {
                animOffset = (float)Math.sin(time * 10) * 5;
            } else {
                animOffset = 0;
            }
        }

        public void draw(Graphics2D g2d) {
            // Shadow
            g2d.setColor(new Color(0, 0, 0, 50));
            int shadowWidth = PLAYER_WIDTH + (int)(animOffset * 0.5);
            g2d.fillOval((int)x + (PLAYER_WIDTH - shadowWidth)/2, GROUND_Y - 10, shadowWidth, 10);

            // Body with gradient
            GradientPaint bodyGradient = new GradientPaint(x, y, color.brighter(),
                    x, y + PLAYER_HEIGHT, color.darker());
            g2d.setPaint(bodyGradient);

            // Animated body (squash and stretch)
            int bodyY = (int)(y + Math.abs(animOffset) * 0.3);
            int bodyHeight = PLAYER_HEIGHT - (int)(Math.abs(animOffset) * 0.3);
            g2d.fillRoundRect((int)x, bodyY, PLAYER_WIDTH, bodyHeight, 20, 20);

            // Head
            int headY = bodyY - 30 + (int)(animOffset * 0.2);
            g2d.setColor(new Color(255, 220, 177));
            g2d.fillOval((int)x + 10, headY, 40, 40);

            // Eyes (looking at ball)
            g2d.setColor(Color.BLACK);
            int eyeOffset = ball.x > x ? 5 : -5;
            g2d.fillOval((int)x + 18 + eyeOffset, headY + 12, 6, 6);
            g2d.fillOval((int)x + 32 + eyeOffset, headY + 12, 6, 6);

            // Headband
            g2d.setColor(color.darker());
            g2d.fillRect((int)x + 10, headY + 8, 40, 8);

            // Arms
            g2d.setColor(new Color(255, 220, 177));
            int armY = bodyY + 25;
            if (vy < 0) { // Jumping - arms up
                g2d.fillRoundRect((int)x - 10, armY - 20, 15, 50, 10, 10);
                g2d.fillRoundRect((int)x + PLAYER_WIDTH - 5, armY - 20, 15, 50, 10, 10);
            } else {
                g2d.fillRoundRect((int)x - 5, armY, 15, 40, 10, 10);
                g2d.fillRoundRect((int)x + PLAYER_WIDTH - 10, armY, 15, 40, 10, 10);
            }
        }
    }

    class Ball {
        float x, y;
        float vx, vy;
        float rotation;
        private Color[] gradientColors;

        public Ball(float x, float y) {
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = 0;
            this.rotation = 0;

            gradientColors = new Color[]{
                    Color.YELLOW,
                    new Color(255, 200, 0),
                    new Color(255, 140, 0),
                    Color.WHITE
            };
        }

        public void update() {
            x += vx;
            y += vy;
            rotation += vx * 2;

            // Air resistance
            vx *= 0.995;
            vy *= 0.995;
        }

        public void draw(Graphics2D g2d) {
            // Ball shadow
            float heightFromGround = GROUND_Y - (y + BALL_RADIUS);
            float shadowAlpha = Math.max(0, 100 - heightFromGround * 0.3f);
            float shadowScale = 1 - heightFromGround * 0.002f;

            g2d.setColor(new Color(0, 0, 0, (int)shadowAlpha));
            int shadowSize = (int)(BALL_RADIUS * 2 * shadowScale);
            g2d.fillOval((int)x - shadowSize/2 + (int)(heightFromGround * 0.2),
                    GROUND_Y - 5, shadowSize, shadowSize/3);

            // Ball with 3D effect
            for (int i = 0; i < 5; i++) {
                float offset = i * 3;
                Color c = gradientColors[i % gradientColors.length];
                g2d.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 255 - i * 40));
                g2d.fillOval((int)(x - BALL_RADIUS + offset), (int)(y - BALL_RADIUS + offset),
                        BALL_RADIUS * 2 - (int)(offset * 2), BALL_RADIUS * 2 - (int)(offset * 2));
            }

            // Volleyball lines (rotating)
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));

            // Draw curved lines to simulate volleyball pattern
            int cx = (int)x;
            int cy = (int)y;

            for (int i = 0; i < 3; i++) {
                double angle = rotation * Math.PI / 180 + i * Math.PI * 2 / 3;
                int x1 = cx + (int)(Math.cos(angle) * BALL_RADIUS * 0.8);
                int y1 = cy + (int)(Math.sin(angle) * BALL_RADIUS * 0.8);
                int x2 = cx + (int)(Math.cos(angle + Math.PI) * BALL_RADIUS * 0.8);
                int y2 = cy + (int)(Math.sin(angle + Math.PI) * BALL_RADIUS * 0.8);
                g2d.drawLine(x1, y1, x2, y2);
            }

            // Highlight
            g2d.setColor(new Color(255, 255, 255, 150));
            g2d.fillOval(cx - BALL_RADIUS/3, cy - BALL_RADIUS/3, BALL_RADIUS/2, BALL_RADIUS/2);
        }
    }

    class ParticleSystem {
        private java.util.List<Particle> particles = new java.util.ArrayList<>();

        public void addParticle(float x, float y, float vx, float vy, Color color) {
            particles.add(new Particle(x, y, vx, vy, color));
        }

        public void update() {
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.update();
                if (p.life <= 0) {
                    particles.remove(i);
                }
            }
        }

        public void draw(Graphics2D g2d) {
            for (Particle p : particles) {
                p.draw(g2d);
            }
        }
    }

    class Particle {
        float x, y;
        float vx, vy;
        Color color;
        float life;
        float maxLife;

        public Particle(float x, float y, float vx, float vy, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.maxLife = 1.0f;
            this.life = maxLife;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 0.3f; // gravity
            vx *= 0.95f;
            life -= 0.02f;
        }

        public void draw(Graphics2D g2d) {
            int alpha = (int)(255 * (life / maxLife));
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            int size = (int)(8 * (life / maxLife));
            g2d.fillOval((int)x - size/2, (int)y - size/2, size, size);
        }
    }

    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Beach Volleyball");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            VolleyballGame game = new VolleyballGame();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}