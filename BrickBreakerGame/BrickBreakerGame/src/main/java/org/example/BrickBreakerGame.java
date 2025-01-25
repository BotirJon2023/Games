package org.example;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class BrickBreakerGame extends JPanel implements KeyListener, ActionListener {

    private static final int WIDTH = 800, HEIGHT = 600;
    private static final int BALL_RADIUS = 10, PADDLE_WIDTH = 100, PADDLE_HEIGHT = 15;
    private static final int BRICK_WIDTH = 75, BRICK_HEIGHT = 20;
    private static final int NUM_ROWS = 5, NUM_COLS = 8;

    private Timer timer;
    private int ballX, ballY, ballDX, ballDY;
    private int paddleX, paddleDX;
    private boolean[][] bricks;
    private int score;

    public BrickBreakerGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        ballX = WIDTH / 2;
        ballY = HEIGHT - 50;
        ballDX = 2;
        ballDY = -2;

        paddleX = WIDTH / 2 - PADDLE_WIDTH / 2;
        paddleDX = 0;

        bricks = new boolean[NUM_ROWS][NUM_COLS];
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                bricks[row][col] = true;
            }
        }

        score = 0;
        timer = new Timer(5, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the ball
        g.setColor(Color.WHITE);
        g.fillOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS, BALL_RADIUS * 2, BALL_RADIUS * 2);

        // Draw the paddle
        g.setColor(Color.BLUE);
        g.fillRect(paddleX, HEIGHT - PADDLE_HEIGHT - 10, PADDLE_WIDTH, PADDLE_HEIGHT);

        // Draw the bricks
        g.setColor(Color.RED);
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                if (bricks[row][col]) {
                    g.fillRect(col * BRICK_WIDTH + 10, row * BRICK_HEIGHT + 50, BRICK_WIDTH, BRICK_HEIGHT);
                }
            }
        }

        // Draw the score
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 20, 20);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_LEFT) {
            paddleDX = -5;
        } else if (keyCode == KeyEvent.VK_RIGHT) {
            paddleDX = 5;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        paddleDX = 0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Ball movement
        ballX += ballDX;
        ballY += ballDY;

        // Ball collision with walls
        if (ballX <= BALL_RADIUS || ballX >= WIDTH - BALL_RADIUS) {
            ballDX = -ballDX;
        }
        if (ballY <= BALL_RADIUS) {
            ballDY = -ballDY;
        }

        // Ball collision with the paddle
        if (ballY >= HEIGHT - PADDLE_HEIGHT - 10 - BALL_RADIUS && ballX >= paddleX && ballX <= paddleX + PADDLE_WIDTH) {
            ballDY = -ballDY;
        }

        // Ball falling off screen (game over condition)
        if (ballY >= HEIGHT) {
            JOptionPane.showMessageDialog(this, "Game Over! Final Score: " + score);
            System.exit(0);
        }

        // Move the paddle
        paddleX += paddleDX;
        if (paddleX < 0) paddleX = 0;
        if (paddleX > WIDTH - PADDLE_WIDTH) paddleX = WIDTH - PADDLE_WIDTH;

        // Ball collision with bricks
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                if (bricks[row][col]) {
                    int brickX = col * BRICK_WIDTH + 10;
                    int brickY = row * BRICK_HEIGHT + 50;

                    if (ballX + BALL_RADIUS > brickX && ballX - BALL_RADIUS < brickX + BRICK_WIDTH &&
                            ballY + BALL_RADIUS > brickY && ballY - BALL_RADIUS < brickY + BRICK_HEIGHT) {
                        bricks[row][col] = false;
                        ballDY = -ballDY;
                        score += 10;
                    }
                }
            }
        }

        // Repaint the screen
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Brick Breaker Game");
        BrickBreakerGame gamePanel = new BrickBreakerGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(gamePanel);
        frame.pack();
        frame.setVisible(true);
    }
}