package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class FlappyBirdGame extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int BIRD_WIDTH = 40;
    private static final int BIRD_HEIGHT = 40;
    private static final int PIPE_WIDTH = 60;
    private static final int PIPE_GAP = 150;
    private static final int PIPE_VELOCITY = 5;
    private static final int GRAVITY = 1;
    private static final int JUMP_STRENGTH = -15;

    private Timer timer;
    private int birdY;
    private int birdVelocity;
    private ArrayList<Rectangle> pipes;
    private int score;
    private boolean gameOver;
    private boolean gameStarted;

    public FlappyBirdGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.cyan);
        setFocusable(true);
        addKeyListener(this);

        birdY = HEIGHT / 2 - BIRD_HEIGHT / 2;
        birdVelocity = 0;
        pipes = new ArrayList<>();
        score = 0;
        gameOver = false;
        gameStarted = false;

        timer = new Timer(20, this);
        timer.start();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBackground(g);
        drawBird(g);
        drawPipes(g);
        drawScore(g);
        if (gameOver) {
            drawGameOver(g);
        }
        if (!gameStarted) {
            drawStartMessage(g);
        }
    }

    private void drawBackground(Graphics g) {
        g.setColor(Color.cyan);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(Color.green);
        g.fillRect(0, HEIGHT - 100, WIDTH, 100);
    }

    private void drawBird(Graphics g) {
        g.setColor(Color.red);
        g.fillRect(100, birdY, BIRD_WIDTH, BIRD_HEIGHT);
    }

    private void drawPipes(Graphics g) {
        g.setColor(Color.green);
        for (Rectangle pipe : pipes) {
            g.fillRect(pipe.x, pipe.y, PIPE_WIDTH, pipe.height);
        }
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        g.drawString("Score: " + score, WIDTH / 2 - 150, 50);
    }

    private void drawGameOver(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 100));
        g.drawString("GAME OVER", WIDTH / 2 - 250, HEIGHT / 2 - 50);
    }

    private void drawStartMessage(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        g.drawString("Press SPACE to Start", WIDTH / 2 - 200, HEIGHT / 2 - 50);
    }

    private void jump() {
        if (gameOver) {
            resetGame();
        }
        if (!gameStarted) {
            gameStarted = true;
        }
        if (birdY > 0) {
            birdVelocity = JUMP_STRENGTH;
        }
    }

    private void resetGame() {
        birdY = HEIGHT / 2 - BIRD_HEIGHT / 2;
        birdVelocity = 0;
        pipes.clear();
        score = 0;
        gameOver = false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int speed = PIPE_VELOCITY;

        if (gameStarted) {
            birdVelocity += GRAVITY;
            birdY += birdVelocity;

            if (birdY > HEIGHT - BIRD_HEIGHT - 100) {
                birdY = HEIGHT - BIRD_HEIGHT - 100;
                birdVelocity = 0;
            }

            if (birdY < 0) {
                birdY = 0;
                birdVelocity = 0;
            }

            addPipes();
            movePipes();
            checkCollisions();
            repaint();
        }
    }

    private void addPipes() {
        if (pipes.isEmpty() || pipes.get(pipes.size() - 1).x < WIDTH - 300) {
            Random rand = new Random();
            int height = rand.nextInt(300) + 50;
            pipes.add(new Rectangle(WIDTH, 0, PIPE_WIDTH, height));
            pipes.add(new Rectangle(WIDTH, height + PIPE_GAP, PIPE_WIDTH, HEIGHT - height - PIPE_GAP));
        }
    }

    private void movePipes() {
        ArrayList<Rectangle> toRemove = new ArrayList<>();
        for (Rectangle pipe : pipes) {
            pipe.x -= PIPE_VELOCITY;
            if (pipe.x + PIPE_WIDTH < 0) {
                toRemove.add(pipe);
            }
        }
        pipes.removeAll(toRemove);
        score++;
    }

    private void checkCollisions() {
        for (Rectangle pipe : pipes) {
            if (pipe.intersects(new Rectangle(100, birdY, BIRD_WIDTH, BIRD_HEIGHT))) {
                gameOver = true;
            }
        }
        if (birdY > HEIGHT - BIRD_HEIGHT - 100) {
            gameOver = true;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            jump();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Flappy Bird");
        FlappyBirdGame game = new FlappyBirdGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}