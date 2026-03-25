
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class VolleyballGame extends JPanel implements ActionListener, KeyListener {

    // Game constants
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;
    private static final int GROUND_Y = 500;
    private static final int NET_X = WIDTH / 2;
    private static final int NET_HEIGHT = 150;
    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 80;
    private static final int BALL_RADIUS = 15;
    private static final int GRAVITY = 1;
    private static final int JUMP_STRENGTH = -18;
    private static final int MOVE_SPEED = 8;

    // Game state
    private Timer timer;
    private boolean gameStarted = false;
    private boolean againstComputer = false;
    private int player1Score = 0;
    private int player2Score = 0;
    private int winningScore = 10;

    // Players
    private Rectangle player1;
    private Rectangle player2;
    private int player1VelY = 0;
    private int player2VelY = 0;
    private boolean player1OnGround = true;
    private boolean player2OnGround = true;

    // Ball
    private double ballX = WIDTH / 2;
    private double ballY = 200;
    private double ballVelX = 0;
    private double ballVelY = 0;
    private boolean ballInAir = true;

    // Input
    private boolean left1, right1, up1;
    private boolean left2, right2, up2;

    // Animation
    private int sunY = 80;
    private float sunAlpha = 1.0f;
    private int frame = 0;
    private int cloudX1 = 100, cloudX2 = 400, cloudX3 = 700;

    public VolleyballGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky blue
        setFocusable(true);
        addKeyListener(this);

        // Initialize players
        player1 = new Rectangle(150, GROUND_Y - PLAYER_HEIGHT, PLAYER_WIDTH, PLAYER_HEIGHT);
        player2 = new Rectangle(WIDTH - 190, GROUND_Y - PLAYER_HEIGHT, PLAYER_WIDTH, PLAYER_HEIGHT);

        // Start with menu
        showMenu();
    }

    private void showMenu() {
        String[] options = {"2 Players", "vs Computer", "Exit"};
        int choice = JOptionPane.showOptionDialog(this,
                "Welcome to Giza Volleyball!\nPlay at the Great Pyramids",
                "Volleyball Game",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            againstComputer = false;
            startGame();
        } else if (choice == 1) {
            againstComputer = true;
            startGame();
        } else {
            System.exit(0);
        }
    }

    private void startGame() {
        gameStarted = true;
        resetBall();
        timer = new Timer(16, this); // ~60 FPS
        timer.start();
        requestFocus();
    }

    private void resetBall() {
        ballX = WIDTH / 2;
        ballY = 200;
        ballVelX = (Math.random() > 0.5 ? 5 : -5);
        ballVelY = -10;
        ballInAir = true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameStarted) return;

        frame++;
        updateGame();
        repaint();
    }

    private void updateGame() {
        // Update clouds
        cloudX1 = (cloudX1 + 1) % (WIDTH + 100);
        cloudX2 = (cloudX2 + 2) % (WIDTH + 100);
        cloudX3 = (cloudX3 + 1) % (WIDTH + 100);

        // Player 1 movement
        if (left1 && player1.x > 0) player1.x -= MOVE_SPEED;
        if (right1 && player1.x < NET_X - PLAYER_WIDTH - 10) player1.x += MOVE_SPEED;
        if (up1 && player1OnGround) {
            player1VelY = JUMP_STRENGTH;
            player1OnGround = false;
        }

        // Player 1 physics
        player1VelY += GRAVITY;
        player1.y += player1VelY;
        if (player1.y >= GROUND_Y - PLAYER_HEIGHT) {
            player1.y = GROUND_Y - PLAYER_HEIGHT;
            player1VelY = 0;
            player1OnGround = true;
        }

        // Player 2 movement (or AI)
        if (againstComputer) {
            updateAI();
        } else {
            if (left2 && player2.x > NET_X + 10) player2.x -= MOVE_SPEED;
            if (right2 && player2.x < WIDTH - PLAYER_WIDTH) player2.x += MOVE_SPEED;
            if (up2 && player2OnGround) {
                player2VelY = JUMP_STRENGTH;
                player2OnGround = false;
            }
        }

        // Player 2 physics
        player2VelY += GRAVITY;
        player2.y += player2VelY;
        if (player2.y >= GROUND_Y - PLAYER_HEIGHT) {
            player2.y = GROUND_Y - PLAYER_HEIGHT;
            player2VelY = 0;
            player2OnGround = true;
        }

        // Ball physics
        ballVelY += GRAVITY * 0.5;
        ballX += ballVelX;
        ballY += ballVelY;

        // Ball collision with net
        if (ballX + BALL_RADIUS > NET_X - 5 && ballX - BALL_RADIUS < NET_X + 5 &&
                ballY + BALL_RADIUS > GROUND_Y - NET_HEIGHT) {
            ballVelX = -ballVelX * 0.8;
        }

        // Ball collision with players
        checkPlayerCollision(player1, 1);
        checkPlayerCollision(player2, 2);

        // Ball ground collision
        if (ballY + BALL_RADIUS > GROUND_Y) {
            if (ballX < NET_X) {
                player2Score++;
            } else {
                player1Score++;
            }
            checkWin();
            resetBall();
        }

        // Ball out of bounds
        if (ballX < 0 || ballX > WIDTH) {
            resetBall();
        }
    }

    private void updateAI() {
        int aiSpeed = 5;
        int targetX = (int)ballX - PLAYER_WIDTH / 2;

        // AI movement logic
        if (ballX > NET_X) { // Ball on AI side
            if (player2.x < targetX && player2.x < WIDTH - PLAYER_WIDTH) {
                player2.x += aiSpeed;
            } else if (player2.x > targetX && player2.x > NET_X + 10) {
                player2.x -= aiSpeed;
            }

            // AI jumping
            if (ballY < 300 && Math.abs(player2.x - targetX) < 50 && player2OnGround) {
                if (Math.random() > 0.3) { // Not perfect AI
                    player2VelY = JUMP_STRENGTH;
                    player2OnGround = false;
                }
            }
        } else { // Return to center
            int centerX = WIDTH - 200;
            if (player2.x < centerX) player2.x += aiSpeed / 2;
            if (player2.x > centerX) player2.x -= aiSpeed / 2;
        }
    }

    private void checkPlayerCollision(Rectangle player, int playerNum) {
        double ballCenterX = ballX;
        double ballCenterY = ballY;

        if (ballCenterX > player.x - BALL_RADIUS &&
                ballCenterX < player.x + player.width + BALL_RADIUS &&
                ballCenterY > player.y - BALL_RADIUS &&
                ballCenterY < player.y + player.height + BALL_RADIUS) {

            // Calculate hit direction
            double hitX = (ballCenterX - (player.x + player.width / 2)) / (player.width / 2);
            ballVelX = hitX * 10 + (playerNum == 1 ? 3 : -3);
            ballVelY = -15;
            ballY = player.y - BALL_RADIUS - 5;
        }
    }

    private void checkWin() {
        if (player1Score >= winningScore || player2Score >= winningScore) {
            timer.stop();
            String winner = player1Score >= winningScore ? "Player 1" :
                    (againstComputer ? "Computer" : "Player 2");
            JOptionPane.showMessageDialog(this, winner + " wins!\nFinal Score: " +
                    player1Score + " - " + player2Score);
            player1Score = 0;
            player2Score = 0;
            showMenu();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw Giza Pyramids background
        drawBackground(g2d);

        // Draw ground
        g2d.setColor(new Color(194, 178, 128)); // Sand color
        g2d.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        // Draw sand texture
        g2d.setColor(new Color(184, 168, 118));
        for (int i = 0; i < WIDTH; i += 20) {
            g2d.fillOval(i, GROUND_Y + 10, 15, 5);
        }

        // Draw net
        drawNet(g2d);

        // Draw players
        drawPlayer(g2d, player1, new Color(255, 100, 100), "P1");
        drawPlayer(g2d, player2, new Color(100, 100, 255), againstComputer ? "AI" : "P2");

        // Draw ball
        drawBall(g2d);

        // Draw score
        drawScore(g2d);

        // Draw controls
        drawControls(g2d);
    }

    private void drawBackground(Graphics2D g2d) {
        // Sky gradient
        GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, HEIGHT, new Color(255, 200, 150));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Sun
        g2d.setColor(new Color(255, 220, 100, (int)(sunAlpha * 255)));
        g2d.fillOval(WIDTH - 150, sunY, 60, 60);
        g2d.setColor(new Color(255, 200, 80, (int)(sunAlpha * 150)));
        g2d.fillOval(WIDTH - 160, sunY - 10, 80, 80);

        // Clouds
        g2d.setColor(new Color(255, 255, 255, 200));
        drawCloud(g2d, cloudX1 - 50, 80);
        drawCloud(g2d, cloudX2 - 50, 120);
        drawCloud(g2d, cloudX3 - 50, 60);

        // Great Pyramids of Giza
        drawPyramid(g2d, 100, GROUND_Y - 200, 300, 200, new Color(218, 165, 105));
        drawPyramid(g2d, 350, GROUND_Y - 150, 220, 150, new Color(200, 150, 90));
        drawPyramid(g2d, 550, GROUND_Y - 120, 180, 120, new Color(190, 140, 80));

        // Pyramid shadows
        g2d.setColor(new Color(0, 0, 0, 30));
        int[] shadowX = {250, 400, 300};
        int[] shadowY = {GROUND_Y, GROUND_Y, GROUND_Y + 50};
        g2d.fillPolygon(shadowX, shadowY, 3);
    }

    private void drawCloud(Graphics2D g2d, int x, int y) {
        g2d.fillOval(x, y, 40, 30);
        g2d.fillOval(x + 20, y - 10, 50, 40);
        g2d.fillOval(x + 50, y, 40, 30);
    }

    private void drawPyramid(Graphics2D g2d, int x, int y, int width, int height, Color color) {
        // Main pyramid shape
        int[] xPoints = {x, x + width / 2, x + width};
        int[] yPoints = {y + height, y, y + height};

        // Gradient for 3D effect
        GradientPaint pyramidGrad = new GradientPaint(x, y, color.brighter(),
                x + width, y + height, color.darker());
        g2d.setPaint(pyramidGrad);
        g2d.fillPolygon(xPoints, yPoints, 3);

        // Edges
        g2d.setColor(color.darker());
        g2d.drawPolygon(xPoints, yPoints, 3);

        // Center line (pyramid edge)
        g2d.setColor(color.darker().darker());
        g2d.drawLine(x + width / 2, y, x + width / 2, y + height);
    }

    private void drawNet(Graphics2D g2d) {
        int netTop = GROUND_Y - NET_HEIGHT;

        // Net posts
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect(NET_X - 5, netTop, 10, NET_HEIGHT);

        // Net mesh
        g2d.setColor(new Color(200, 200, 200, 180));
        for (int y = netTop; y < GROUND_Y; y += 10) {
            g2d.drawLine(NET_X - 40, y, NET_X + 40, y);
        }
        for (int x = NET_X - 40; x <= NET_X + 40; x += 10) {
            g2d.drawLine(x, netTop, x, GROUND_Y);
        }

        // Top tape
        g2d.setColor(Color.WHITE);
        g2d.fillRect(NET_X - 42, netTop - 3, 84, 6);
    }

    private void drawPlayer(Graphics2D g2d, Rectangle player, Color color, String label) {
        // Body
        g2d.setColor(color);
        g2d.fillRoundRect(player.x, player.y, player.width, player.height, 10, 10);

        // Head
        g2d.setColor(new Color(255, 220, 177));
        g2d.fillOval(player.x + 5, player.y - 15, 30, 30);

        // Eyes
        g2d.setColor(Color.BLACK);
        if (label.equals("P1") || label.equals("AI")) {
            g2d.fillOval(player.x + 20, player.y - 8, 4, 4);
        } else {
            g2d.fillOval(player.x + 16, player.y - 8, 4, 4);
        }

        // Label
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.setColor(Color.WHITE);
        g2d.drawString(label, player.x + 8, player.y + 45);
    }

    private void drawBall(Graphics2D g2d) {
        // Ball shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        int shadowSize = (int)(BALL_RADIUS * (1 - (ballY / GROUND_Y)));
        g2d.fillOval((int)ballX - shadowSize / 2, GROUND_Y - 10, shadowSize, shadowSize / 2);

        // Ball
        GradientPaint ballGrad = new GradientPaint(
                (int)ballX - BALL_RADIUS, (int)ballY - BALL_RADIUS, Color.YELLOW,
                (int)ballX + BALL_RADIUS, (int)ballY + BALL_RADIUS, Color.ORANGE);
        g2d.setPaint(ballGrad);
        g2d.fillOval((int)ballX - BALL_RADIUS, (int)ballY - BALL_RADIUS,
                BALL_RADIUS * 2, BALL_RADIUS * 2);

        // Ball lines (volleyball pattern)
        g2d.setColor(Color.RED);
        g2d.drawOval((int)ballX - BALL_RADIUS, (int)ballY - BALL_RADIUS,
                BALL_RADIUS * 2, BALL_RADIUS * 2);
        g2d.drawLine((int)ballX - BALL_RADIUS, (int)ballY,
                (int)ballX + BALL_RADIUS, (int)ballY);
        g2d.drawLine((int)ballX, (int)ballY - BALL_RADIUS,
                (int)ballX, (int)ballY + BALL_RADIUS);
    }

    private void drawScore(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.valueOf(player1Score), NET_X - 80, 50);
        g2d.drawString(String.valueOf(player2Score), NET_X + 60, 50);

        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("First to " + winningScore + " wins!", NET_X - 50, 80);
    }

    private void drawControls(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(Color.WHITE);

        if (againstComputer) {
            g2d.drawString("Player 1: A/D = Move, W = Jump", 20, HEIGHT - 40);
            g2d.drawString("Press ESC for menu", 20, HEIGHT - 20);
        } else {
            g2d.drawString("Player 1: A/D = Move, W = Jump", 20, HEIGHT - 40);
            g2d.drawString("Player 2: Left/Right = Move, Up = Jump", 20, HEIGHT - 20);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // Player 1 controls
        if (key == KeyEvent.VK_A) left1 = true;
        if (key == KeyEvent.VK_D) right1 = true;
        if (key == KeyEvent.VK_W) up1 = true;

        // Player 2 controls
        if (!againstComputer) {
            if (key == KeyEvent.VK_LEFT) left2 = true;
            if (key == KeyEvent.VK_RIGHT) right2 = true;
            if (key == KeyEvent.VK_UP) up2 = true;
        }

        // Menu
        if (key == KeyEvent.VK_ESCAPE) {
            timer.stop();
            showMenu();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_A) left1 = false;
        if (key == KeyEvent.VK_D) right1 = false;
        if (key == KeyEvent.VK_W) up1 = false;

        if (!againstComputer) {
            if (key == KeyEvent.VK_LEFT) left2 = false;
            if (key == KeyEvent.VK_RIGHT) right2 = false;
            if (key == KeyEvent.VK_UP) up2 = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Volleyball at Giza Pyramids");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new VolleyballGame());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}