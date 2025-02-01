package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BouncingBallGame extends JPanel implements ActionListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int BALL_SIZE = 30;
    private static final int DELAY = 10;
    private static final int MAX_BALLS = 10;
    private static final int MAX_SPEED = 8;

    private List<Ball> balls;
    private Timer timer;
    private int score;
    private boolean gameOver;

    public BouncingBallGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        balls = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < MAX_BALLS; i++) {
            int x = rand.nextInt(WIDTH - BALL_SIZE);
            int y = rand.nextInt(HEIGHT - BALL_SIZE);
            int xSpeed = rand.nextInt(MAX_SPEED * 2) - MAX_SPEED;
            int ySpeed = rand.nextInt(MAX_SPEED * 2) - MAX_SPEED;
            Color color = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            balls.add(new Ball(x, y, xSpeed, ySpeed, color));
        }

        timer = new Timer(DELAY, this);
        timer.start();

        score = 0;
        gameOver = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gameOver) {
            drawGameOver(g);
            return;
        }

        for (Ball ball : balls) {
            ball.draw(g);
        }

        drawScore(g);
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, 20, 30);
    }

    private void drawGameOver(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Game Over!", WIDTH / 2 - 100, HEIGHT / 2 - 20);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Final Score: " + score, WIDTH / 2 - 80, HEIGHT / 2 + 20);
    }

    private void moveBalls() {
        for (Ball ball : balls) {
            ball.move();

            // Check for collision with the edges of the window
            if (ball.getX() <= 0 || ball.getX() >= WIDTH - BALL_SIZE) {
                ball.reverseX();
                score++;
            }
            if (ball.getY() <= 0 || ball.getY() >= HEIGHT - BALL_SIZE) {
                ball.reverseY();
                score++;
            }
        }

        // Check if all balls have stopped moving
        gameOver = true;
        for (Ball ball : balls) {
            if (ball.getXSpeed() != 0 || ball.getYSpeed() != 0) {
                gameOver = false;
                break;
            }
        }

        if (gameOver) {
            timer.stop();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        moveBalls();
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Bouncing Ball Game");
        BouncingBallGame game = new BouncingBallGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private class Ball {
        private int x;
        private int y;
        private int xSpeed;
        private int ySpeed;
        private Color color;

        public Ball(int x, int y, int xSpeed, int ySpeed, Color color) {
            this.x = x;
            this.y = y;
            this.xSpeed = xSpeed;
            this.ySpeed = ySpeed;
            this.color = color;
        }

        public void move() {
            x += xSpeed;
            y += ySpeed;
        }

        public void reverseX() {
            xSpeed = -xSpeed;
        }

        public void reverseY() {
            ySpeed = -ySpeed;
        }

        public void draw(Graphics g) {
            g.setColor(color);
            g.fillOval(x, y, BALL_SIZE, BALL_SIZE);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getXSpeed() {
            return xSpeed;
        }

        public int getYSpeed() {
            return ySpeed;
        }
    }
}
