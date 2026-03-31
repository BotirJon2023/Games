import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class VolleyballGame extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;
    private static final int NET_HEIGHT = 200;
    private static final int NET_WIDTH = 10;
    private static final int BALL_RADIUS = 15;
    private static final int PADDLE_WIDTH = 80;
    private static final int PADDLE_HEIGHT = 15;

    // Game objects
    private int ballX;
    private int ballY;
    private double ballVelX;
    private int ballVelY;
    private int player1X, player2X;
    private int player1Score, player2Score;
    private boolean gameRunning;
    private boolean vsComputer;
    private Random random;

    // Animation
    private Timer timer;
    private int frameCount;

    // Background elements
    private int[] pyramidHeights;
    private int[] pyramidX;

    public VolleyballGame(boolean vsComputer) {
        this.vsComputer = vsComputer;
        this.random = new Random();
        this.frameCount = 0;

        // Initialize pyramids
        pyramidHeights = new int[]{150, 200, 120};
        pyramidX = new int[]{200, 500, 750};

        // Set up panel
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky blue
        setFocusable(true);
        addKeyListener(this);

        // Initialize game
        resetGame();

        // Start animation timer (60 FPS)
        timer = new Timer(16, this);
        timer.start();
    }

    private void resetGame() {
        ballX = WIDTH / 2;
        ballY = HEIGHT / 3;
        ballVelX = random.nextBoolean() ? 5 : -5;
        ballVelY = 3;

        player1X = WIDTH / 4;
        player2X = 3 * WIDTH / 4;

        player1Score = 0;
        player2Score = 0;
        gameRunning = true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw sky gradient
        GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, HEIGHT, new Color(255, 215, 0));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw sun
        g2d.setColor(new Color(255, 255, 0));
        g2d.fillOval(800, 50, 80, 80);

        // Draw Giza Pyramids
        drawPyramids(g2d);

        // Draw ground (sand)
        g2d.setColor(new Color(210, 180, 140));
        g2d.fillRect(0, HEIGHT - 50, WIDTH, 50);

        // Draw net
        drawNet(g2d);

        // Draw players
        drawPlayer(g2d, player1X, HEIGHT - 80, Color.BLUE, "P1");
        drawPlayer(g2d, player2X, HEIGHT - 80, Color.RED, vsComputer ? "CPU" : "P2");

        // Draw ball
        drawBall(g2d);

        // Draw scores
        drawScores(g2d);

        // Draw game info
        drawGameInfo(g2d);
    }

    private void drawPyramids(Graphics2D g2d) {
        g2d.setColor(new Color(210, 180, 140));

        for (int i = 0; i < pyramidHeights.length; i++) {
            int x = pyramidX[i];
            int y = HEIGHT - 50;
            int h = pyramidHeights[i];
            int w = h * 2;

            // Pyramid triangle
            int[] xPoints = {x, x + w/2, x + w};
            int[] yPoints = {y, y - h, y};
            g2d.fillPolygon(xPoints, yPoints, 3);

            // Pyramid outline
            g2d.setColor(new Color(180, 150, 110));
            g2d.drawPolygon(xPoints, yPoints, 3);

            // Pyramid details (lines)
            g2d.setColor(new Color(160, 130, 90));
            for (int j = 1; j < 5; j++) {
                int lineY = y - (h * j / 5);
                int lineWidth = w * (5 - j) / 5;
                int lineX = x + (w - lineWidth) / 2;
                g2d.drawLine(lineX, lineY, lineX + lineWidth, lineY);
            }
        }
    }

    private void drawNet(Graphics2D g2d) {
        int netX = WIDTH / 2 - NET_WIDTH / 2;
        int netY = HEIGHT - NET_HEIGHT - 50;

        // Net posts
        g2d.setColor(Color.GRAY);
        g2d.fillRect(netX - 5, netY, 5, NET_HEIGHT + 50);
        g2d.fillRect(netX + NET_WIDTH, netY, 5, NET_HEIGHT + 50);

        // Net mesh
        g2d.setColor(new Color(200, 200, 200));
        for (int i = 0; i < NET_HEIGHT; i += 10) {
            g2d.drawLine(netX, netY + i, netX + NET_WIDTH, netY + i);
        }
        for (int i = 0; i <= NET_WIDTH; i += 5) {
            g2d.drawLine(netX + i, netY, netX + i, netY + NET_HEIGHT);
        }
    }

    private void drawPlayer(Graphics2D g2d, int x, int y, Color color, String label) {
        // Body
        g2d.setColor(color);
        g2d.fillRoundRect(x, y, PADDLE_WIDTH, PADDLE_HEIGHT, 10, 10);

        // Outline
        g2d.setColor(color.darker());
        g2d.drawRoundRect(x, y, PADDLE_WIDTH, PADDLE_HEIGHT, 10, 10);

        // Label
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString(label, x + PADDLE_WIDTH/2 - 10, y + PADDLE_HEIGHT + 15);
    }

    private void drawBall(Graphics2D g2d) {
        // Ball shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(ballX - BALL_RADIUS + 5, HEIGHT - 60, BALL_RADIUS * 2, 10);

        // Ball
        GradientPaint ballGradient = new GradientPaint(
                ballX - BALL_RADIUS, ballY - BALL_RADIUS, Color.WHITE,
                ballX + BALL_RADIUS, ballY + BALL_RADIUS, Color.ORANGE
        );
        g2d.setPaint(ballGradient);
        g2d.fillOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS,
                BALL_RADIUS * 2, BALL_RADIUS * 2);

        // Ball outline
        g2d.setColor(Color.BLACK);
        g2d.drawOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS,
                BALL_RADIUS * 2, BALL_RADIUS * 2);

        // Ball lines (volleyball pattern)
        g2d.setColor(new Color(200, 100, 0));
        g2d.drawLine(ballX - BALL_RADIUS, ballY, ballX + BALL_RADIUS, ballY);
        g2d.drawLine(ballX, ballY - BALL_RADIUS, ballX, ballY + BALL_RADIUS);
    }

    private void drawScores(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));

        // Player 1 score
        g2d.drawString(String.valueOf(player1Score), WIDTH / 4 - 30, 80);

        // Player 2/CPU score
        g2d.drawString(String.valueOf(player2Score), 3 * WIDTH / 4 - 30, 80);

        // Divider
        g2d.setColor(Color.GRAY);
        g2d.drawLine(WIDTH / 2 - 50, 60, WIDTH / 2 + 50, 60);
    }

    private void drawGameInfo(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));

        if (vsComputer) {
            g2d.drawString("Mode: vs Computer", 20, 25);
            g2d.drawString("Controls: A/D or Left/Right arrows", 20, 45);
        } else {
            g2d.drawString("Mode: 2 Players", 20, 25);
            g2d.drawString("P1: A/D | P2: Left/Right arrows", 20, 45);
        }

        g2d.drawString("Press R to Reset | Press M for Menu", 20, HEIGHT - 20);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning) {
            updateGame();
        }
        repaint();
        frameCount++;
    }

    private void updateGame() {
        // Update ball position
        ballX += ballVelX;
        ballY += ballVelY;

        // Ball gravity
        ballVelY += 0.3;

        // Ball collision with ground
        if (ballY + BALL_RADIUS >= HEIGHT - 50) {
            ballVelY = (int) (-Math.abs(ballVelY) * 0.8);
            ballY = HEIGHT - 50 - BALL_RADIUS;

            // Ball stopped
            if (Math.abs(ballVelY) < 2) {
                scorePoint();
            }
        }

        // Ball collision with walls
        if (ballX - BALL_RADIUS <= 0) {
            ballX = BALL_RADIUS;
            ballVelX = Math.abs(ballVelX);
        }
        if (ballX + BALL_RADIUS >= WIDTH) {
            ballX = WIDTH - BALL_RADIUS;
            ballVelX = -Math.abs(ballVelX);
        }

        // Ball collision with net
        int netX = WIDTH / 2 - NET_WIDTH / 2;
        if (ballX + BALL_RADIUS >= netX && ballX - BALL_RADIUS <= netX + NET_WIDTH) {
            if (ballY + BALL_RADIUS >= HEIGHT - NET_HEIGHT - 50) {
                ballVelX = -ballVelX;
            }
        }

        // Player 1 collision
        if (ballY + BALL_RADIUS >= HEIGHT - 80 &&
                ballY - BALL_RADIUS <= HEIGHT - 65 &&
                ballX >= player1X && ballX <= player1X + PADDLE_WIDTH) {
            ballVelY = (int) (-Math.abs(ballVelY) * 0.9);
            ballVelX = (ballX - (player1X + PADDLE_WIDTH/2)) * 0.3;
            ballY = HEIGHT - 80 - BALL_RADIUS;
        }

        // Player 2 collision
        if (ballY + BALL_RADIUS >= HEIGHT - 80 &&
                ballY - BALL_RADIUS <= HEIGHT - 65 &&
                ballX >= player2X && ballX <= player2X + PADDLE_WIDTH) {
            ballVelY = (int) (-Math.abs(ballVelY) * 0.9);
            ballVelX = (int) ((ballX - (player2X + PADDLE_WIDTH/2)) * 0.3);
            ballY = HEIGHT - 80 - BALL_RADIUS;
        }

        // Computer AI
        if (vsComputer) {
            updateComputerAI();
        }
    }

    private void updateComputerAI() {
        int targetX = ballX - PADDLE_WIDTH / 2;

        // Only move when ball is coming towards computer
        if (ballVelX > 0) {
            if (player2X < targetX - 10) {
                player2X += 6;
            } else if (player2X > targetX + 10) {
                player2X -= 6;
            }
        } else {
            // Return to center when ball is going away
            int centerX = 3 * WIDTH / 4 - PADDLE_WIDTH / 2;
            if (player2X < centerX - 10) {
                player2X += 3;
            } else if (player2X > centerX + 10) {
                player2X -= 3;
            }
        }

        // Keep player in bounds
        player2X = Math.max(WIDTH / 2 + NET_WIDTH,
                Math.min(WIDTH - PADDLE_WIDTH - 20, player2X));
    }

    private void scorePoint() {
        if (ballX < WIDTH / 2) {
            player2Score++;
        } else {
            player1Score++;
        }

        // Check for winner (first to 5 points)
        if (player1Score >= 5 || player2Score >= 5) {
            gameRunning = false;
            showWinner();
        } else {
            // Reset ball
            ballX = WIDTH / 2;
            ballY = HEIGHT / 3;
            ballVelX = random.nextBoolean() ? 5 : -5;
            ballVelY = 3;
        }
    }

    private void showWinner() {
        String winner = player1Score >= 5 ? "Player 1" : (vsComputer ? "Computer" : "Player 2");
        JOptionPane.showMessageDialog(this,
                winner + " Wins!\nFinal Score: " + player1Score + " - " + player2Score,
                "Game Over", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        int moveSpeed = 30;

        // Player 1 controls (A/D or Left/Right)
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) {
            player1X = Math.max(20, player1X - moveSpeed);
        }
        if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) {
            player1X = Math.min(WIDTH / 2 - PADDLE_WIDTH - 20, player1X + moveSpeed);
        }

        // Player 2 controls (only in 2-player mode)
        if (!vsComputer) {
            if (key == KeyEvent.VK_LEFT) {
                player2X = Math.max(WIDTH / 2 + NET_WIDTH, player2X - moveSpeed);
            }
            if (key == KeyEvent.VK_RIGHT) {
                player2X = Math.min(WIDTH - PADDLE_WIDTH - 20, player2X + moveSpeed);
            }
        }

        // Reset game
        if (key == KeyEvent.VK_R) {
            resetGame();
        }

        // Show menu
        if (key == KeyEvent.VK_M) {
            showMenu();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    private void showMenu() {
        timer.stop();

        String[] options = {"2 Players", "vs Computer", "Quit"};
        int choice = JOptionPane.showOptionDialog(this,
                "Select Game Mode",
                "Volleyball - Giza Pyramids",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 0) {
            vsComputer = false;
            resetGame();
            timer.start();
        } else if (choice == 1) {
            vsComputer = true;
            resetGame();
            timer.start();
        } else {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("🏐 Volleyball - Giza Pyramids");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            // Show initial menu
            String[] options = {"2 Players", "vs Computer"};
            int choice = JOptionPane.showOptionDialog(null,
                    "Select Game Mode",
                    "Volleyball - Giza Pyramids",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            boolean vsComputer = (choice == 1);
            VolleyballGame game = new VolleyballGame(vsComputer);

            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}