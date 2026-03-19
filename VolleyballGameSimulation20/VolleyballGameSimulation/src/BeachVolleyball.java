import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class BeachVolleyball extends JPanel implements ActionListener, KeyListener {

    // Game Constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int NET_HEIGHT = 150;
    private static final int NET_WIDTH = 10;
    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 80;
    private static final int BALL_SIZE = 20;

    // Game State
    private Timer timer;
    private boolean isRunning = false;
    private int difficulty = 1; // 1=Easy, 2=Medium, 3=Hard

    // Entities
    private Rectangle player1;
    private Rectangle player2; // Computer
    private Rectangle ball;
    private Rectangle net;

    // Physics
    private int ballDx = 4;
    private int ballDy = 0;
    private double gravity = 0.4;
    private double bounceFactor = 0.7;

    // Scores
    private int score1 = 0;
    private int score2 = 0;

    // Input
    private boolean upPressed = false;
    private boolean downPressed = false;
    private int playerSpeed = 6;

    public BeachVolleyball() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(Color.CYAN); // Sky color
        this.setFocusable(true);
        this.addKeyListener(this);

        initGame();

        // Game Loop Timer (approx 60 FPS)
        timer = new Timer(16, this);
        timer.start();
    }

    private void initGame() {
        // Initialize positions
        player1 = new Rectangle(50, HEIGHT - 150 - PLAYER_HEIGHT, PLAYER_WIDTH, PLAYER_HEIGHT);
        player2 = new Rectangle(WIDTH - 50 - PLAYER_WIDTH, HEIGHT - 150 - PLAYER_HEIGHT, PLAYER_WIDTH, PLAYER_HEIGHT);
        ball = new Rectangle(WIDTH / 2 - BALL_SIZE / 2, 100, BALL_SIZE, BALL_SIZE);
        net = new Rectangle(WIDTH / 2 - NET_WIDTH / 2, HEIGHT - 150 - NET_HEIGHT, NET_WIDTH, NET_HEIGHT);

        resetBall();
    }

    private void resetBall() {
        ball.setLocation(WIDTH / 2 - BALL_SIZE / 2, 100);
        ballDx = (Math.random() > 0.5 ? 4 : -4); // Random serve direction
        ballDy = 0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isRunning) {
            updatePhysics();
            updateAI();
        }
        repaint();
    }

    private void updatePhysics() {
        // Apply Gravity
        ballDy += gravity;

        // Move Ball
        ball.x += ballDx;
        ball.y += ballDy;

        // Floor Collision (Sand)
        if (ball.y + ball.height >= HEIGHT - 50) {
            ball.y = HEIGHT - 50 - ball.height;
            ballDy = -(int)(ballDy * bounceFactor);

            // Stop ball if moving too slow (friction)
            if (Math.abs(ballDy) < 2) ballDy = 0;
        }

        // Ceiling Collision
        if (ball.y <= 0) {
            ball.y = 0;
            ballDy = -ballDy;
        }

        // Wall Collision
        if (ball.x <= 0) {
            ball.x = 0;
            ballDx = -ballDx;
        }
        if (ball.x + ball.width >= WIDTH) {
            ball.x = WIDTH - ball.width;
            ballDx = -ballDx;
        }

        // Net Collision
        if (ball.intersects(net)) {
            // Simple net physics: bounce back horizontally
            if (ball.x < net.x) {
                ball.x = net.x - ball.width;
                ballDx = -Math.abs(ballDx);
            } else {
                ball.x = net.x + net.width;
                ballDx = Math.abs(ballDx);
            }
            ballDy = -5; // Slight pop up on net hit
        }

        // Player 1 Collision
        checkPlayerCollision(player1);
        // Player 2 Collision
        checkPlayerCollision(player2);

        // Scoring
        if (ball.y > HEIGHT) {
            if (ball.x < WIDTH / 2) {
                score2++; // Computer scores
            } else {
                score1++; // Player scores
            }
            resetBall();
        }
    }

    private void checkPlayerCollision(Rectangle p) {
        if (ball.intersects(p)) {
            // Determine hit direction based on where ball hit player
            // If ball hits top of player
            if (ball.y + ball.height <= p.y + 10) {
                ballDy = -12; // High lob
                ballDx = (ball.x + ball.width/2) - (p.x + p.width/2); // Angle based on hit position
                ballDx /= 5; // Normalize angle
            } else {
                // Side hit (standard volley)
                ballDy = -8;
                ballDx = (ball.x < p.x) ? -6 : 6;
            }
        }
    }

    private void updateAI() {
        int aiSpeed = 0;
        int reactionDelay = 0;

        // Difficulty Settings
        if (difficulty == 1) { aiSpeed = 3; reactionDelay = 20; } // Easy
        else if (difficulty == 2) { aiSpeed = 5; reactionDelay = 10; } // Medium
        else if (difficulty == 3) { aiSpeed = 7; reactionDelay = 0; } // Hard

        // Simple AI Logic: Follow the ball X position
        // Only move if ball is on AI side or coming towards AI
        if (ball.x > WIDTH / 2 - 50) {
            int centerPlayer = player2.x + player2.width / 2;

            if (centerPlayer < ball.x - reactionDelay) {
                player2.x += aiSpeed;
            } else if (centerPlayer > ball.x + reactionDelay) {
                player2.x -= aiSpeed;
            }
        } else {
            // Return to base position if ball is far away
            int baseX = WIDTH - 100;
            if (player2.x < baseX) player2.x += 2;
            if (player2.x > baseX) player2.x -= 2;
        }

        // Keep AI in bounds
        if (player2.x < WIDTH / 2) player2.x = WIDTH / 2;
        if (player2.x > WIDTH - PLAYER_WIDTH) player2.x = WIDTH - PLAYER_WIDTH;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw Sky
        g2d.setColor(new Color(135, 206, 235));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw Sun
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(700, 20, 60, 60);

        // Draw Sand
        g2d.setColor(new Color(237, 201, 175));
        g2d.fillRect(0, HEIGHT - 50, WIDTH, 50);

        // Draw Net
        g2d.setColor(Color.WHITE);
        g2d.fillRect(net.x, net.y, net.width, net.height);
        // Net mesh detail
        g2d.setColor(Color.LIGHT_GRAY);
        for(int i=0; i<net.height; i+=10) {
            g2d.drawLine(net.x, net.y + i, net.x + net.width, net.y + i);
        }

        // Draw Players
        g2d.setColor(Color.RED);
        g2d.fillRoundRect(player1.x, player1.y, player1.width, player1.height, 10, 10);
        // Player 1 Head
        g2d.setColor(Color.ORANGE);
        g2d.fillOval(player1.x + 5, player1.y - 20, 30, 30);

        g2d.setColor(Color.BLUE);
        g2d.fillRoundRect(player2.x, player2.y, player2.width, player2.height, 10, 10);
        // Player 2 Head
        g2d.setColor(Color.CYAN);
        g2d.fillOval(player2.x + 5, player2.y - 20, 30, 30);

        // Draw Ball
        g2d.setColor(Color.WHITE);
        g2d.fillOval(ball.x, ball.y, ball.width, ball.height);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(ball.x, ball.y, ball.width, ball.height);

        // Draw Scores
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.drawString(String.valueOf(score1), WIDTH / 4, 50);
        g2d.drawString(String.valueOf(score2), 3 * WIDTH / 4, 50);

        // Draw Instructions / Menu
        if (!isRunning) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 30));
            g2d.drawString("BEACH VOLLEYBALL", WIDTH/2 - 150, HEIGHT/2 - 50);

            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            g2d.drawString("Press 1: Easy | 2: Medium | 3: Hard", WIDTH/2 - 180, HEIGHT/2 + 10);
            g2d.drawString("Press SPACE to Start", WIDTH/2 - 110, HEIGHT/2 + 50);
            g2d.drawString("Controls: UP/DOWN Arrows to Move", WIDTH/2 - 160, HEIGHT/2 + 90);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_SPACE) {
            isRunning = true;
        }

        if (key == KeyEvent.VK_1) { difficulty = 1; repaint(); }
        if (key == KeyEvent.VK_2) { difficulty = 2; repaint(); }
        if (key == KeyEvent.VK_3) { difficulty = 3; repaint(); }

        if (key == KeyEvent.VK_UP) {
            upPressed = true;
        }
        if (key == KeyEvent.VK_DOWN) {
            downPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_UP) upPressed = false;
        if (key == KeyEvent.VK_DOWN) downPressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Separate thread for smooth player movement independent of game loop tick
    public static void main(String[] args) {
        JFrame frame = new JFrame("Java Beach Volleyball");
        BeachVolleyball game = new BeachVolleyball();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Movement Loop
        while (true) {
            if (game.isRunning) {
                if (game.upPressed && game.player1.y > HEIGHT - 150 - game.PLAYER_HEIGHT) {
                    game.player1.y -= game.playerSpeed;
                }
                if (game.downPressed && game.player1.y + game.PLAYER_HEIGHT < HEIGHT - 50) {
                    game.player1.y += game.playerSpeed;
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}