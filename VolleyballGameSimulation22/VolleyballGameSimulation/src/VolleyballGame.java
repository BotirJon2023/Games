import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class VolleyballGame extends JPanel implements ActionListener, KeyListener {

    // --- Game Constants ---
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PADDLE_WIDTH = 20;
    private static final int PADDLE_HEIGHT = 100;
    private static final int BALL_SIZE = 20;
    private static final int WINNING_SCORE = 5;

    // --- Game State ---
    private enum State { MENU, PLAYING, GAMEOVER }
    private State currentState = State.MENU;
    private boolean isTwoPlayer = false;

    // --- Physics & Objects ---
    private int paddle1Y = HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private int paddle2Y = HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private int ballX = WIDTH / 2;
    private int ballY = HEIGHT / 2;
    private double ballVelX = 5;
    private double ballVelY = 5;
    private double gravity = 0.4;

    // Input flags
    private boolean p1Up, p1Down, p2Up, p2Down;

    // Scores
    private int score1 = 0;
    private int score2 = 0;

    // Visuals
    private Timer timer;
    private Random rand = new Random();
    private int flashOpacity = 0; // For hit effects

    public VolleyballGame() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(new Color(20, 20, 30)); // Dark background
        this.setFocusable(true);
        this.addKeyListener(this);

        // Game Loop Timer (60 FPS)
        timer = new Timer(1000 / 60, this);
        timer.start();
    }

    // --- Main Logic ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentState == State.PLAYING) {
            updatePhysics();
            checkCollisions();
            checkWinCondition();
        }
        repaint();
    }

    private void updatePhysics() {
        // Move Ball
        ballVelY += gravity;
        ballX += ballVelX;
        ballY += ballVelY;

        // Move Paddle 1 (Player 1 - W/S)
        if (p1Up && paddle1Y > 0) paddle1Y -= 8;
        if (p1Down && paddle1Y < HEIGHT - PADDLE_HEIGHT) paddle1Y += 8;

        // Move Paddle 2 (Player 2 or AI)
        if (isTwoPlayer) {
            if (p2Up && paddle2Y > 0) paddle2Y -= 8;
            if (p2Down && paddle2Y < HEIGHT - PADDLE_HEIGHT) paddle2Y += 8;
        } else {
            // AI Logic
            int targetY = ballY - PADDLE_HEIGHT / 2;
            // AI Speed limit to make it beatable
            int aiSpeed = 6;
            if (paddle2Y < targetY) paddle2Y += aiSpeed;
            if (paddle2Y > targetY) paddle2Y -= aiSpeed;

            // Keep AI in bounds
            if (paddle2Y < 0) paddle2Y = 0;
            if (paddle2Y > HEIGHT - PADDLE_HEIGHT) paddle2Y = HEIGHT - PADDLE_HEIGHT;
        }

        // Floor/Ceiling bounce for ball
        if (ballY + BALL_SIZE > HEIGHT) {
            ballY = HEIGHT - BALL_SIZE;
            ballVelY = -ballVelY * 0.7; // Lose energy on bounce
        }
        if (ballY < 0) {
            ballY = 0;
            ballVelY = -ballVelY * 0.7;
        }

        // Walls
        if (ballX < 0) {
            score2++;
            resetBall(1);
        }
        if (ballX > WIDTH) {
            score1++;
            resetBall(-1);
        }

        // Fade out hit effect
        if(flashOpacity > 0) flashOpacity -= 10;
    }

    private void checkCollisions() {
        // Paddle 1 Collision (Left)
        if (ballX <= PADDLE_WIDTH + 10 && ballX >= 10 &&
                ballY + BALL_SIZE >= paddle1Y && ballY <= paddle1Y + PADDLE_HEIGHT) {

            ballVelX = Math.abs(ballVelX) + 1; // Speed up
            ballVelY = (ballY - (paddle1Y + PADDLE_HEIGHT/2)) * 0.35; // Add spin/angle
            flashOpacity = 100;
        }

        // Paddle 2 Collision (Right)
        if (ballX >= WIDTH - PADDLE_WIDTH - 30 && ballX <= WIDTH - 10 &&
                ballY + BALL_SIZE >= paddle2Y && ballY <= paddle2Y + PADDLE_HEIGHT) {

            ballVelX = -Math.abs(ballVelX) - 1;
            ballVelY = (ballY - (paddle2Y + PADDLE_HEIGHT/2)) * 0.35;
            flashOpacity = 100;
        }
    }

    private void resetBall(int direction) {
        ballX = WIDTH / 2;
        ballY = HEIGHT / 2;
        ballVelX = direction * 5;
        ballVelY = (rand.nextDouble() * 4) - 2;
        flashOpacity = 0;
    }

    private void checkWinCondition() {
        if (score1 >= WINNING_SCORE || score2 >= WINNING_SCORE) {
            currentState = State.GAMEOVER;
        }
    }

    // --- Rendering (The "Beautiful" Part) ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Draw Background Gradient
        GradientPaint gp = new GradientPaint(0, 0, new Color(30, 30, 50), 0, HEIGHT, new Color(10, 10, 20));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 2. Draw Net
        g2d.setColor(new Color(100, 100, 100, 100));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(WIDTH / 2, 0, WIDTH / 2, HEIGHT);
        // Net holes
        for(int i=0; i<HEIGHT; i+=40) {
            g2d.drawLine(WIDTH/2 - 10, i, WIDTH/2 + 10, i);
        }

        if (currentState == State.MENU) {
            drawMenu(g2d);
        } else if (currentState == State.PLAYING) {
            drawGame(g2d);
        } else if (currentState == State.GAMEOVER) {
            drawGameOver(g2d);
        }
    }

    private void drawGame(Graphics2D g2d) {
        // Draw Scores
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.drawString(String.valueOf(score1), WIDTH / 4 - 20, 100);
        g2d.drawString(String.valueOf(score2), 3 * WIDTH / 4 - 20, 100);

        // Draw Paddles (Neon Style)
        g2d.setColor(new Color(0, 255, 200)); // Cyan for P1
        g2d.fillRoundRect(10, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT, 10, 10);
        // Glow effect
        g2d.setColor(new Color(0, 255, 200, 50));
        g2d.fillRoundRect(5, paddle1Y - 5, PADDLE_WIDTH + 10, PADDLE_HEIGHT + 10, 15, 15);

        g2d.setColor(new Color(255, 50, 50)); // Red for P2
        g2d.fillRoundRect(WIDTH - PADDLE_WIDTH - 10, paddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT, 10, 10);
        // Glow effect
        g2d.setColor(new Color(255, 50, 50, 50));
        g2d.fillRoundRect(WIDTH - PADDLE_WIDTH - 15, paddle2Y - 5, PADDLE_WIDTH + 10, PADDLE_HEIGHT + 10, 15, 15);

        // Draw Ball (Volleyball Style)
        g2d.setColor(Color.WHITE);
        g2d.fillOval(ballX, ballY, BALL_SIZE, BALL_SIZE);
        // Ball Shine
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.fillOval(ballX + 4, ballY + 4, BALL_SIZE/2, BALL_SIZE/2);

        // Hit Flash Effect
        if (flashOpacity > 0) {
            g2d.setColor(new Color(255, 255, 255, flashOpacity));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        }

        // Controls Hint
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.setColor(Color.GRAY);
        String p2Text = isTwoPlayer ? "P2: Arrow Keys" : "Mode: Vs Computer";
        g2d.drawString("P1: W/S Keys   |   " + p2Text, WIDTH/2 - 100, HEIGHT - 20);
    }

    private void drawMenu(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 50));
        g2d.drawString("VOLLEYBALL", WIDTH / 2 - 180, HEIGHT / 2 - 50);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Press [1] for 1 Player (Vs CPU)", WIDTH / 2 - 150, HEIGHT / 2 + 20);
        g2d.drawString("Press [2] for 2 Players", WIDTH / 2 - 130, HEIGHT / 2 + 60);
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 50));
        String winner = score1 >= WINNING_SCORE ? "PLAYER 1 WINS!" : (isTwoPlayer ? "PLAYER 2 WINS!" : "COMPUTER WINS!");
        g2d.drawString(winner, WIDTH / 2 - 200, HEIGHT / 2);

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Press [R] to Restart", WIDTH / 2 - 80, HEIGHT / 2 + 50);
    }

    // --- Input Handling ---

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (currentState == State.MENU) {
            if (key == KeyEvent.VK_1) {
                isTwoPlayer = false;
                startGame();
            } else if (key == KeyEvent.VK_2) {
                isTwoPlayer = true;
                startGame();
            }
        } else if (currentState == State.PLAYING) {
            if (key == KeyEvent.VK_W) p1Up = true;
            if (key == KeyEvent.VK_S) p1Down = true;
            if (isTwoPlayer) {
                if (key == KeyEvent.VK_UP) p2Up = true;
                if (key == KeyEvent.VK_DOWN) p2Down = true;
            }
        } else if (currentState == State.GAMEOVER) {
            if (key == KeyEvent.VK_R) {
                score1 = 0; score2 = 0;
                currentState = State.MENU;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W) p1Up = false;
        if (key == KeyEvent.VK_S) p1Down = false;
        if (key == KeyEvent.VK_UP) p2Up = false;
        if (key == KeyEvent.VK_DOWN) p2Down = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void startGame() {
        score1 = 0;
        score2 = 0;
        resetBall(1);
        currentState = State.PLAYING;
    }

    // --- Main Entry Point ---
    public static void main(String[] args) {
        JFrame frame = new JFrame("Java Volleyball");
        VolleyballGame game = new VolleyballGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}