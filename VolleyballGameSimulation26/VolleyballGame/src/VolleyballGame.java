import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class VolleyballGame extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;
    private static final int GROUND_Y = 500;
    private static final int NET_X = 500;
    private static final int NET_HEIGHT = 150;

    // Game objects
    private Ball ball;
    private Player player1; // Left player (human)
    private Player player2; // Right player (computer or human)
    private boolean vsComputer = true;

    // Game state
    private int score1 = 0;
    private int score2 = 0;
    private boolean gameRunning = true;
    private Timer timer;

    // Physics constants
    private static final double GRAVITY = 0.4;
    private static final double FRICTION = 0.98;
    private static final double BOUNCE_DAMPING = 0.8;

    // Animation
    private double waveOffset = 0;
    private double cloudOffset = 0;

    public VolleyballGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky blue
        setFocusable(true);
        addKeyListener(this);

        resetGame();

        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    private void resetGame() {
        ball = new Ball(NET_X, 200);
        player1 = new Player(150, GROUND_Y - 80, Color.RED, true);
        player2 = new Player(850, GROUND_Y - 80, Color.BLUE, false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw sky gradient
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, HEIGHT, new Color(255, 248, 220));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw animated clouds
        drawClouds(g2d);

        // Draw sun
        g2d.setColor(new Color(255, 215, 0, 180));
        g2d.fillOval(800, 50, 80, 80);
        g2d.setColor(new Color(255, 255, 0, 100));
        g2d.fillOval(790, 40, 100, 100);

        // Draw ocean with animated waves
        drawOcean(g2d);

        // Draw beach/sand
        GradientPaint sandGradient = new GradientPaint(0, GROUND_Y, new Color(238, 203, 140),
                0, HEIGHT, new Color(194, 178, 128));
        g2d.setPaint(sandGradient);
        g2d.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        // Draw net
        drawNet(g2d);

        // Draw players
        player1.draw(g2d);
        player2.draw(g2d);

        // Draw ball
        ball.draw(g2d);

        // Draw score
        drawScore(g2d);

        // Draw controls hint
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Player 1: A/D to move, W to jump, SPACE to hit | Player 2: ←/→ to move, ↑ to jump, ENTER to hit", 20, 30);
        g2d.drawString("Press 'C' to toggle Computer opponent", 20, 50);
    }

    private void drawClouds(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 200));
        int[] cloudX = {100, 300, 600, 800};
        for (int i = 0; i < cloudX.length; i++) {
            int x = (int)((cloudX[i] + cloudOffset * (0.5 + i * 0.2)) % (WIDTH + 200)) - 100;
            int y = 80 + i * 30;
            g2d.fillOval(x, y, 60, 40);
            g2d.fillOval(x + 20, y - 10, 50, 35);
            g2d.fillOval(x + 40, y, 55, 40);
        }
    }

    private void drawOcean(Graphics2D g2d) {
        // Ocean background
        g2d.setColor(new Color(0, 105, 148));
        g2d.fillRect(0, GROUND_Y - 20, WIDTH, 20);

        // Animated waves
        g2d.setColor(new Color(0, 150, 199, 150));
        for (int i = 0; i < WIDTH; i += 10) {
            int waveHeight = (int)(10 * Math.sin((i + waveOffset) * 0.02));
            g2d.fillOval(i, GROUND_Y - 25 + waveHeight, 20, 15);
        }

        // Foam
        g2d.setColor(new Color(255, 255, 255, 100));
        for (int i = 0; i < WIDTH; i += 30) {
            int foamHeight = (int)(8 * Math.sin((i + waveOffset * 1.5) * 0.03));
            g2d.fillOval(i, GROUND_Y - 15 + foamHeight, 15, 8);
        }
    }

    private void drawNet(Graphics2D g2d) {
        // Net posts
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect(NET_X - 5, GROUND_Y - NET_HEIGHT, 10, NET_HEIGHT);

        // Net mesh
        g2d.setColor(new Color(255, 255, 255, 180));
        for (int y = GROUND_Y - NET_HEIGHT; y < GROUND_Y; y += 15) {
            g2d.drawLine(NET_X - 60, y, NET_X + 60, y);
        }
        for (int x = NET_X - 60; x <= NET_X + 60; x += 15) {
            g2d.drawLine(x, GROUND_Y - NET_HEIGHT, x, GROUND_Y);
        }

        // Net top tape
        g2d.setColor(Color.WHITE);
        g2d.fillRect(NET_X - 65, GROUND_Y - NET_HEIGHT - 5, 130, 10);
    }

    private void drawScore(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        String scoreText = score1 + " - " + score2;
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(scoreText);
        g2d.drawString(scoreText, (WIDTH - textWidth) / 2, 80);

        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        String modeText = vsComputer ? "vs Computer" : "2 Players";
        g2d.drawString(modeText, (WIDTH - fm.stringWidth(modeText)) / 2, 100);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning) return;

        // Update animations
        waveOffset += 2;
        cloudOffset += 0.5;

        // Update physics
        ball.update();
        player1.update();

        if (vsComputer) {
            updateComputer();
        } else {
            player2.update();
        }

        // Check collisions
        checkCollisions();

        // Check scoring
        checkScore();

        repaint();
    }

    private void updateComputer() {
        // Simple AI
        double ballX = ball.x;
        double ballY = ball.y;

        // Move towards ball prediction
        if (ball.vx > 0 && ball.x > NET_X) {
            double targetX = ballX - 30;
            if (player2.x < targetX - 10) {
                player2.vx = 3;
            } else if (player2.x > targetX + 10) {
                player2.vx = -3;
            } else {
                player2.vx = 0;
            }

            // Jump if ball is high and close
            if (ballY < GROUND_Y - 150 && Math.abs(ballX - player2.x) < 80 && player2.onGround) {
                player2.jump();
            }

            // Hit ball when close
            if (Math.abs(ballX - player2.x) < 60 && Math.abs(ballY - player2.y) < 80) {
                player2.hit(ball);
            }
        } else {
            // Return to center
            if (player2.x < 800) {
                player2.vx = 2;
            } else if (player2.x > 900) {
                player2.vx = -2;
            } else {
                player2.vx = 0;
            }
        }

        player2.update();
    }

    private void checkCollisions() {
        // Ball with ground
        if (ball.y + ball.radius > GROUND_Y) {
            ball.y = GROUND_Y - ball.radius;
            ball.vy *= -BOUNCE_DAMPING;
            ball.vx *= FRICTION;
        }

        // Ball with net
        if (ball.x > NET_X - 10 && ball.x < NET_X + 10 &&
                ball.y > GROUND_Y - NET_HEIGHT) {
            ball.vx *= -BOUNCE_DAMPING;
            if (ball.x < NET_X) ball.x = NET_X - 10;
            else ball.x = NET_X + 10;
        }

        // Ball with players
        player1.checkBallCollision(ball);
        player2.checkBallCollision(ball);

        // Players with ground
        if (player1.y + player1.height > GROUND_Y) {
            player1.y = GROUND_Y - player1.height;
            player1.vy = 0;
            player1.onGround = true;
        }
        if (player2.y + player2.height > GROUND_Y) {
            player2.y = GROUND_Y - player2.height;
            player2.vy = 0;
            player2.onGround = true;
        }

        // Players with net
        if (player1.x + player1.width > NET_X - 10 && player1.x < NET_X + 10) {
            if (player1.x < NET_X) player1.x = NET_X - 10 - player1.width;
        }
        if (player2.x < NET_X + 10 && player2.x + player2.width > NET_X - 10) {
            if (player2.x > NET_X) player2.x = NET_X + 10;
        }

        // Keep players in bounds
        player1.x = Math.max(0, Math.min(player1.x, NET_X - 10 - player1.width));
        player2.x = Math.max(NET_X + 10, Math.min(player2.x, WIDTH - player2.width));
    }

    private void checkScore() {
        // Ball hits ground on either side
        if (ball.y + ball.radius >= GROUND_Y - 1 && Math.abs(ball.vy) < 1) {
            if (ball.x < NET_X) {
                score2++;
            } else {
                score1++;
            }
            resetRound();
        }

        // Ball out of bounds
        if (ball.x < 0 || ball.x > WIDTH) {
            if (ball.x < 0) score2++;
            else score1++;
            resetRound();
        }
    }

    private void resetRound() {
        ball.reset(NET_X, 200);
        player1.reset(150, GROUND_Y - 80);
        player2.reset(850, GROUND_Y - 80);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // Player 1 controls
        if (key == KeyEvent.VK_A) player1.vx = -5;
        if (key == KeyEvent.VK_D) player1.vx = 5;
        if (key == KeyEvent.VK_W && player1.onGround) player1.jump();
        if (key == KeyEvent.VK_SPACE) player1.hit(ball);

        // Player 2 controls (only in 2 player mode)
        if (!vsComputer) {
            if (key == KeyEvent.VK_LEFT) player2.vx = -5;
            if (key == KeyEvent.VK_RIGHT) player2.vx = 5;
            if (key == KeyEvent.VK_UP && player2.onGround) player2.jump();
            if (key == KeyEvent.VK_ENTER) player2.hit(ball);
        }

        // Toggle computer opponent
        if (key == KeyEvent.VK_C) {
            vsComputer = !vsComputer;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        // Player 1
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_D) player1.vx = 0;

        // Player 2
        if (!vsComputer) {
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) player2.vx = 0;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner classes
    class Ball {
        double x, y;
        double vx, vy;
        double radius = 20;

        Ball(double x, double y) {
            this.x = x;
            this.y = y;
            this.vx = (Math.random() - 0.5) * 4;
            this.vy = 0;
        }

        void update() {
            vy += GRAVITY;
            x += vx;
            y += vy;
        }

        void reset(double x, double y) {
            this.x = x;
            this.y = y;
            this.vx = (Math.random() - 0.5) * 4;
            this.vy = -5;
        }

        void draw(Graphics2D g2d) {
            // Ball shadow
            g2d.setColor(new Color(0, 0, 0, 50));
            int shadowSize = (int)(radius * (1 - (GROUND_Y - y) / 300));
            g2d.fillOval((int)x - shadowSize/2, GROUND_Y - 5, shadowSize, 10);

            // Ball body
            GradientPaint ballGradient = new GradientPaint(
                    (int)(x - radius), (int)(y - radius), new Color(255, 140, 0),
                    (int)(x + radius), (int)(y + radius), new Color(255, 69, 0));
            g2d.setPaint(ballGradient);
            g2d.fillOval((int)(x - radius), (int)(y - radius), (int)(radius * 2), (int)(radius * 2));

            // Ball lines
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval((int)(x - radius), (int)(y - radius), (int)(radius * 2), (int)(radius * 2));
            g2d.drawLine((int)(x - radius), (int)y, (int)(x + radius), (int)y);
            g2d.drawLine((int)x, (int)(y - radius), (int)x, (int)(y + radius));
        }
    }

    class Player {
        double x, y;
        double vx, vy;
        int width = 50;
        int height = 80;
        Color color;
        boolean isPlayer1;
        boolean onGround = false;
        int armAngle = 0;

        Player(double x, double y, Color color, boolean isPlayer1) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.isPlayer1 = isPlayer1;
        }

        void update() {
            vy += GRAVITY;
            x += vx;
            y += vy;

            if (vy > 0) onGround = false;

            // Arm animation
            if (armAngle > 0) armAngle -= 5;
        }

        void jump() {
            vy = -12;
            onGround = false;
        }

        void hit(Ball ball) {
            double dx = ball.x - (x + width/2);
            double dy = ball.y - (y + height/3);
            double dist = Math.sqrt(dx*dx + dy*dy);

            if (dist < 70) {
                armAngle = 45;
                // Hit the ball away
                double force = 15;
                double angle = Math.atan2(dy, dx);
                ball.vx = Math.cos(angle) * force;
                ball.vy = Math.sin(angle) * force - 5;
            }
        }

        void checkBallCollision(Ball ball) {
            double dx = ball.x - (x + width/2);
            double dy = ball.y - (y + height/3);
            double dist = Math.sqrt(dx*dx + dy*dy);

            if (dist < ball.radius + 30) {
                // Bounce off player
                double angle = Math.atan2(dy, dx);
                ball.vx = Math.cos(angle) * 5;
                ball.vy = Math.sin(angle) * 5;
                ball.x = x + width/2 + Math.cos(angle) * (ball.radius + 35);
                ball.y = y + height/3 + Math.sin(angle) * (ball.radius + 35);
            }
        }

        void reset(double x, double y) {
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = 0;
            this.onGround = true;
        }

        void draw(Graphics2D g2d) {
            // Shadow
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillOval((int)x + 5, GROUND_Y - 5, width - 10, 10);

            // Body
            g2d.setColor(color);
            g2d.fillOval((int)x + 10, (int)y, width - 20, height - 30);

            // Head
            g2d.setColor(new Color(255, 220, 177));
            g2d.fillOval((int)x + 15, (int)y - 15, width - 30, 30);

            // Arms
            g2d.setColor(color.darker());
            int armX = isPlayer1 ? (int)x + width : (int)x;
            int armY = (int)y + 20;
            g2d.drawLine(armX, armY, armX + (isPlayer1 ? 20 : -20), armY - armAngle);

            // Legs
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillRect((int)x + 15, (int)y + height - 30, 8, 30);
            g2d.fillRect((int)x + width - 23, (int)y + height - 30, 8, 30);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("🏐 Seaside Volleyball");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new VolleyballGame());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}