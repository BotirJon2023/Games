package org.example;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Random;

public class FlappyBird extends JPanel {
    private final int WIDTH = 800, HEIGHT = 600;
    private final int BIRD_SIZE = 30;
    private int birdY = HEIGHT / 2, birdDy = 0;
    private final int GRAVITY = 1, LIFT = -15;
    private boolean gameOver = false;
    private ArrayList<Rectangle> pipes = new ArrayList<>();
    private final int PIPE_WIDTH = 50, PIPE_GAP = 150, PIPE_SPACING = 300;
    private int pipeSpeed = 5;

    public FlappyBird() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(Color.CYAN);
        this.setFocusable(true);
        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && !gameOver) {
                    birdDy = LIFT;
                }
            }
        });

        Timer timer = new Timer(10, e -> {
            if (!gameOver) {
                birdY += birdDy;
                birdDy += GRAVITY;
                movePipes();
                checkCollisions();
                repaint();
            }
        });
        timer.start();
    }

    private void movePipes() {
        ArrayList<Rectangle> toRemove = new ArrayList<>();
        for (Rectangle pipe : pipes) {
            pipe.x -= pipeSpeed;
            if (pipe.x + PIPE_WIDTH < 0) {
                toRemove.add(pipe);
            }
        }
        pipes.removeAll(toRemove);

        if (pipes.isEmpty() || pipes.get(pipes.size() - 1).x < WIDTH - PIPE_SPACING) {
            addPipe();
        }
    }

    private void addPipe() {
        Random rand = new Random();
        int height = rand.nextInt(300) + 100;
        pipes.add(new Rectangle(WIDTH, 0, PIPE_WIDTH, height));
        pipes.add(new Rectangle(WIDTH, height + PIPE_GAP, PIPE_WIDTH, HEIGHT - height - PIPE_GAP));
    }

    private void checkCollisions() {
        if (birdY > HEIGHT || birdY < 0) {
            gameOver = true;
        }

        for (Rectangle pipe : pipes) {
            if (pipe.intersects(new Rectangle(100, birdY, BIRD_SIZE, BIRD_SIZE))) {
                gameOver = true;
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.YELLOW);
        g.fillRect(100, birdY, BIRD_SIZE, BIRD_SIZE);

        g.setColor(Color.GREEN);
        for (Rectangle pipe : pipes) {
            g.fillRect(pipe.x, pipe.y, pipe.width, pipe.height);
        }

        if (gameOver) {
            g.setColor(Color.RED);
            g.drawString("Game Over", WIDTH / 2 - 50, HEIGHT / 2);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Flappy Bird");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new FlappyBird());
        frame.pack();
        frame.setVisible(true);
    }
}
