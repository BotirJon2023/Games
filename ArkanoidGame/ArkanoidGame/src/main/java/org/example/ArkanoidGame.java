package org.example;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

public class ArkanoidGame extends JPanel implements ActionListener {

    private final int WIDTH = 800;
    private final int HEIGHT = 600;
    private final int BRICK_WIDTH = 60;
    private final int BRICK_HEIGHT = 30;
    private final int NUM_BRICKS = 10;
    private final int PADDLE_WIDTH = 100;
    private final int PADDLE_HEIGHT = 10;
    private final int BALL_SIZE = 10;
    private final int INIT_BALL_X = WIDTH / 2;
    private final int INIT_BALL_Y = HEIGHT - 100;

    private boolean gameOver = false;
    private boolean gameWin = false;
    private int ballX = INIT_BALL_X;
    private int ballY = INIT_BALL_Y;
    private int ballDX = 2;
    private int ballDY = -2;
    private int paddleX = WIDTH / 2 - PADDLE_WIDTH / 2;
    private int paddleDX = 0;
    private int brickCount = NUM_BRICKS;

    private Timer timer;
    private Rectangle[] bricks;

    public ArkanoidGame() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);

        bricks = new Rectangle[NUM_BRICKS];
        for (int i = 0; i < NUM_BRICKS; i++) {
            bricks[i] = new Rectangle(i * (BRICK_WIDTH + 5) + 30, 50, BRICK_WIDTH, BRICK_HEIGHT);
        }

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_LEFT) {
                    paddleDX = -5;
                }
                if (key == KeyEvent.VK_RIGHT) {
                    paddleDX = 5;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                    paddleDX = 0;
                }
            }
        });

        timer = new Timer(5, this);
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver || gameWin) {
            return;
        }

        moveBall();
        movePaddle();
        checkCollisions();
        repaint();
    }

    private void moveBall() {
        ballX += ballDX;
        ballY += ballDY;

        if (ballX <= 0 || ballX >= WIDTH - BALL_SIZE) {
            ballDX = -ballDX;
        }
        if (ballY <= 0) {
            ballDY = -ballDY;
        }
        if (ballY >= HEIGHT - BALL_SIZE) {
            gameOver = true;
        }
    }

    private void movePaddle() {
        paddleX += paddleDX;
        if (paddleX < 0) {
            paddleX = 0;
        }
        if (paddleX > WIDTH - PADDLE_WIDTH) {
            paddleX = WIDTH - PADDLE_WIDTH;
        }
    }

    private void checkCollisions() {
        // Ball collision with paddle
        if (ballY + BALL_SIZE >= HEIGHT - PADDLE_HEIGHT && ballX + BALL_SIZE >= paddleX && ballX <= paddleX + PADDLE_WIDTH) {
            ballDY = -ballDY;
        }

        // Ball collision with bricks
        for (int i = 0; i < NUM_BRICKS; i++) {
            Rectangle brick = bricks[i];
            if (brick != null && brick.contains(ballX, ballY)) {
                ballDY = -ballDY;
                bricks[i] = null;
                brickCount--;
                if (brickCount == 0) {
                    gameWin = true;
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("Game Over", WIDTH / 3, HEIGHT / 2);
        } else if (gameWin) {
            g.setColor(Color.GREEN);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("You Win!", WIDTH / 3, HEIGHT / 2);
        } else {
            g.setColor(Color.WHITE);
            g.fillRect(paddleX, HEIGHT - PADDLE_HEIGHT, PADDLE_WIDTH, PADDLE_HEIGHT);

            g.setColor(Color.RED);
            for (int i = 0; i < NUM_BRICKS; i++) {
                if (bricks[i] != null) {
                    g.fillRect(bricks[i].x, bricks[i].y, BRICK_WIDTH, BRICK_HEIGHT);
                }
            }

            g.setColor(Color.YELLOW);
            g.fillOval(ballX, ballY, BALL_SIZE, BALL_SIZE);
        }
    }

    public static void main(String[] args) {
        javax.swing.JFrame frame = new javax.swing.JFrame("Arkanoid Game");
        ArkanoidGame game = new ArkanoidGame();
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(game);
        frame.pack();
        frame.setVisible(true);
    }

    private static class Rectangle {
        int x, y, width, height;

        public Rectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public boolean contains(int px, int py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }
}