import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

public class PingPongGame extends JPanel {

    // Game constants
    private final int SCREEN_WIDTH = 800;
    private final int SCREEN_HEIGHT = 600;
    private final int BALL_RADIUS = 20;
    private final int PADDLE_WIDTH = 10;
    private final int PADDLE_HEIGHT = 100;
    private final int FPS = 60;

    // Colors
    private final Color WHITE = Color.WHITE;
    private final Color BLACK = Color.BLACK;

    // Ball properties
    private double ballX = SCREEN_WIDTH / 2;
    private double ballY = SCREEN_HEIGHT / 2;
    private double ballSpeedX = 5;
    private double ballSpeedY = 5;

    // Paddle properties
    private double paddle1X = 0;
    private double paddle1Y = SCREEN_HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private double paddle2X = SCREEN_WIDTH - PADDLE_WIDTH;
    private double paddle2Y = SCREEN_HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private double paddleSpeed = 5;

    // Score
    private int score1 = 0;
    private int score2 = 0;

    // Key presses
    private boolean wPressed = false;
    private boolean sPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;

    public PingPongGame() {
        setBackground(BLACK);
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));

        // Key listener
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
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
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
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
        });
        setFocusable(true);

        // Timer
        Timer timer = new Timer(1000 / FPS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGame();
                repaint();
            }
        });
        timer.start();
    }

    private void updateGame() {
        // Update paddles
        if (wPressed && paddle1Y > 0) {
            paddle1Y -= paddleSpeed;
        }
        if (sPressed && paddle1Y < SCREEN_HEIGHT - PADDLE_HEIGHT) {
            paddle1Y += paddleSpeed;
        }
        if (upPressed && paddle2Y > 0) {
            paddle2Y -= paddleSpeed;
        }
        if (downPressed && paddle2Y < SCREEN_HEIGHT - PADDLE_HEIGHT) {
            paddle2Y += paddleSpeed;
        }

        // Update ball
        ballX += ballSpeedX;
        ballY += ballSpeedY;

        // Bounce off walls
        if (ballY < BALL_RADIUS || ballY > SCREEN_HEIGHT - BALL_RADIUS) {
            ballSpeedY *= -1;
        }

        // Score points
        if (ballX < BALL_RADIUS) {
            score2++;
            resetBall();
        } else if (ballX > SCREEN_WIDTH - BALL_RADIUS) {
            score1++;
            resetBall();
        }

        // Bounce off paddles
        if (ballX < PADDLE_WIDTH + BALL_RADIUS && ballY > paddle1Y && ballY < paddle1Y + PADDLE_HEIGHT) {
            ballSpeedX *= -1;
        } else if (ballX > SCREEN_WIDTH - PADDLE_WIDTH - BALL_RADIUS && ballY > paddle2Y && ballY < paddle2Y + PADDLE_HEIGHT) {
            ballSpeedX *= -1;
        }
    }

    private void resetBall() {
        ballX = SCREEN_WIDTH / 2;
        ballY = SCREEN_HEIGHT / 2;
        ballSpeedX *= -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw ball
        g2d.setColor(WHITE);
        g2d.fill(new Ellipse2D.Double(ballX - BALL_RADIUS, ballY - BALL_RADIUS, BALL_RADIUS * 2, BALL_RADIUS * 2));

        // Draw paddles
        g2d.fill(new Rectangle2D.Double(paddle1X, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT));
        g2d.fill(new Rectangle2D.Double(paddle2X, paddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT));

        // Draw score
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        String scoreText = score1 + " - " + score2;
        int stringWidth = g2d.getFontMetrics().stringWidth(scoreText);
        g2d.drawString(scoreText, SCREEN_WIDTH / 2 - stringWidth / 2, 100);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Ping Pong Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new PingPongGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}