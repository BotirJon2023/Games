// BowlingSimulator.java

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class BowlingSimulator extends JPanel {

    // Constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PIN_SIZE = 20;
    private static final int BALL_SIZE = 40;
    private static final int MAX_ROLL_SPEED = 10;

    // Game variables
    private int rollSpeed;
    private int ballX, ballY;
    private int[] pinX = new int[10];
    private int[] pinY = new int[10];
    private boolean[] pinDown = new boolean[10];
    private int score;
    private int rollCount;
    private int frameCount;
    private boolean isRolling;

    // Animation variables
    private int animationFrame;
    private int animationDelay;
    private Timer animationTimer;

    public BowlingSimulator() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.GREEN);
        setFocusable(true);
        requestFocus();

        // Initialize pins
        for (int i = 0; i < 10; i++) {
            pinX[i] = (i % 3) * 100 + 200;
            pinY[i] = (i / 3) * 100 + 100;
            pinDown[i] = false;
        }

        // Initialize game variables
        score = 0;
        rollCount = 0;
        frameCount = 1;
        isRolling = false;

        // Initialize animation variables
        animationFrame = 0;
        animationDelay = 16; // 60 FPS

        // Create animation timer
        animationTimer = new Timer(animationDelay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGame();
                repaint();
            }
        });

        // Add key listener
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && !isRolling) {
                    rollSpeed = new Random().nextInt(MAX_ROLL_SPEED) + 1;
                    isRolling = true;
                }
            }
        });

        // Start animation timer
        animationTimer.start();
    }

    private void updateGame() {
        if (isRolling) {
            // Move ball
            ballX += rollSpeed;
            if (ballX > WIDTH) {
                isRolling = false;
                ballX = 0;
                rollCount++;
                if (rollCount == 2) {
                    frameCount++;
                    rollCount = 0;
                }
                checkPins();
            }

            // Check for collisions
            for (int i = 0; i < 10; i++) {
                if (!pinDown[i] && distance(ballX, ballY, pinX[i], pinY[i]) < (BALL_SIZE + PIN_SIZE) / 2) {
                    pinDown[i] = true;
                    score++;
                }
            }
        }
    }

    private void checkPins() {
        for (int i = 0; i < 10; i++) {
            if (pinDown[i]) {
                // Move pin
                pinY[i] += 5;
                if (pinY[i] > HEIGHT) {
                    pinY[i] = HEIGHT;
                }
            }
        }
    }

    private double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw pins
        for (int i = 0; i < 10; i++) {
            if (pinDown[i]) {
                g.setColor(Color.RED);
            } else {
                g.setColor(Color.WHITE);
            }
            g.fillOval(pinX[i], pinY[i], PIN_SIZE, PIN_SIZE);
        }

        // Draw ball
        g.setColor(Color.BLACK);
        g.fillOval(ballX, ballY, BALL_SIZE, BALL_SIZE);

        // Draw score
        g.setColor(Color.BLACK);
        g.drawString("Score: " + score, 10, 20);
        g.drawString("Frame: " + frameCount, 10, 40);
        g.drawString("Roll: " + (rollCount + 1), 10, 60);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Bowling Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new BowlingSimulator());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}