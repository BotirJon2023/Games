import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class VirtualGolf extends JPanel implements ActionListener, KeyListener {
    // Game Constants
    private final int WIDTH = 800;
    private final int HEIGHT = 600;
    private final int BALL_SIZE = 15;
    private final int HOLE_SIZE = 25;

    // Game State
    private float ballX = 100, ballY = 450;
    private float velX = 0, velY = 0;
    private float holeX = 650, holeY = 150;
    private float power = 0;
    private boolean isCharging = false;
    private boolean isMoving = false;
    private int currentPlayer = 1;
    private int[] scores = {0, 0};
    private String message = "Player 1: Hold SPACE to aim and charge!";

    public VirtualGolf() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky Blue
        setFocusable(true);
        addKeyListener(this);
        new Timer(16, this).start(); // ~60 FPS
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Draw Seaside Background
        g2.setColor(new Color(0, 105, 148)); // Deep Sea
        g2.fillRect(0, 300, WIDTH, 300);
        g2.setColor(new Color(238, 214, 175)); // Sand
        g2.fillOval(-100, 350, 1000, 400);

        // 2. Draw the Green
        g2.setColor(new Color(34, 139, 34)); // Forest Green
        g2.fillOval((int)holeX - 50, (int)holeY - 50, 120, 120);

        // 3. Draw the Hole
        g2.setColor(Color.BLACK);
        g2.fillOval((int)holeX, (int)holeY, HOLE_SIZE, HOLE_SIZE);

        // 4. Draw the Ball
        g2.setColor(currentPlayer == 1 ? Color.WHITE : Color.YELLOW);
        g2.fillOval((int)ballX, (int)ballY, BALL_SIZE, BALL_SIZE);

        // 5. UI Elements
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString(message, 20, 30);
        g2.drawString("P1 Score: " + scores[0] + " | P2 Score: " + scores[1], 20, 55);

        // Power Bar
        if (isCharging) {
            g2.setColor(Color.RED);
            g2.fillRect(20, 70, (int)power * 2, 10);
            g2.drawRect(20, 70, 200, 10);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isMoving) {
            ballX += velX;
            ballY += velY;

            // Friction (The "Grass" effect)
            velX *= 0.98;
            velY *= 0.98;

            // Check if ball is in hole
            double dist = Math.sqrt(Math.pow(ballX - holeX, 2) + Math.pow(ballY - holeY, 2));
            if (dist < 15 && Math.abs(velX) < 2) {
                handleWin();
            }

            // Stop ball when slow
            if (Math.abs(velX) < 0.2 && Math.abs(velY) < 0.2) {
                isMoving = false;
                velX = 0; velY = 0;
                switchTurn();
            }
        }

        // Bounce off walls
        if (ballX < 0 || ballX > WIDTH - BALL_SIZE) velX *= -1;
        if (ballY < 0 || ballY > HEIGHT - BALL_SIZE) velY *= -1;

        repaint();
    }

    private void handleWin() {
        scores[currentPlayer - 1]++;
        message = "PLAYER " + currentPlayer + " SCORED!";
        resetBall();
    }

    private void resetBall() {
        ballX = 100; ballY = 450;
        velX = 0; velY = 0;
        isMoving = false;
    }

    private void switchTurn() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        message = "Player " + currentPlayer + "'s turn!";
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && !isMoving) {
            isCharging = true;
            power = (power + 2) % 100;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && isCharging) {
            // Physics: Aiming toward the hole with power
            float dx = holeX - ballX;
            float dy = holeY - ballY;
            float angle = (float) Math.atan2(dy, dx);

            velX = (float) Math.cos(angle) * (power / 5);
            velY = (float) Math.sin(angle) * (power / 5);

            isMoving = true;
            isCharging = false;
            power = 0;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Seaside Virtual Golf");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new VirtualGolf());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}