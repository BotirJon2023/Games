package org.example;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
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


public class SnakeGame extends JPanel implements ActionListener {
    private final int TILE_SIZE = 30;
    private final int WIDTH = 600;
    private final int HEIGHT = 600;
    private final int TOTAL_TILES = (WIDTH * HEIGHT) / TILE_SIZE;
    private final int BOARD_SIZE = 20;
    private int score = 0;
    private boolean running = false;
    private int length = 3;
    private int[] x = new int[TOTAL_TILES];
    private int[] y = new int[TOTAL_TILES];
    private int appleX, appleY;
    private char direction = 'R';
    private Timer timer;

    public SnakeGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        if (direction != 'R') direction = 'L';
                        break;
                    case KeyEvent.VK_RIGHT:
                        if (direction != 'L') direction = 'R';
                        break;
                    case KeyEvent.VK_UP:
                        if (direction != 'D') direction = 'U';
                        break;
                    case KeyEvent.VK_DOWN:
                        if (direction != 'U') direction = 'D';
                        break;
                }
            }
        });
        startGame();
    }

    public void startGame() {
        running = true;
        length = 3;
        x[0] = 100;
        y[0] = 100;
        spawnApple();
        timer = new Timer(100, this);
        timer.start();
    }

    public void spawnApple() {
        appleX = (int) (Math.random() * (WIDTH / TILE_SIZE)) * TILE_SIZE;
        appleY = (int) (Math.random() * (HEIGHT / TILE_SIZE)) * TILE_SIZE;
    }

    public void move() {
        for (int i = length; i > 0; i--) {
            x[i] = x[i - 1];
            y[i] = y[i - 1];
        }
        if (direction == 'L') x[0] -= TILE_SIZE;
        if (direction == 'R') x[0] += TILE_SIZE;
        if (direction == 'U') y[0] -= TILE_SIZE;
        if (direction == 'D') y[0] += TILE_SIZE;
    }

    public void checkCollision() {
        if (x[0] < 0 || x[0] >= WIDTH || y[0] < 0 || y[0] >= HEIGHT) {
            running = false;
        }
        for (int i = length - 1; i > 0; i--) {
            if (x[0] == x[i] && y[0] == y[i]) {
                running = false;
            }
        }
    }

    public void checkApple() {
        if (x[0] == appleX && y[0] == appleY) {
            length++;
            score++;
            spawnApple();
        }
    }

    public void paint(Graphics g) {
        super.paint(g);
        if (running) {
            g.setColor(Color.green);
            for (int i = 0; i < length; i++) {
                g.fillRect(x[i], y[i], TILE_SIZE, TILE_SIZE);
            }
            g.setColor(Color.red);
            g.fillRect(appleX, appleY, TILE_SIZE, TILE_SIZE);
            g.setColor(Color.white);
            g.setFont(new Font("Helvetica", Font.BOLD, 20));
            FontMetrics metrics = getFontMetrics(g.getFont());
            g.drawString("Score: " + score, (WIDTH - metrics.stringWidth("Score: " + score)) / 2, g.getFont().getSize());
        } else {
            gameOver(g);
        }
    }

    public void gameOver(Graphics g) {
        String message = "Game Over! Score: " + score;
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.setColor(Color.white);
        g.setFont(new Font("Helvetica", Font.BOLD, 30));
        g.drawString(message, (WIDTH - metrics.stringWidth(message)) / 2, HEIGHT / 2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            move();
            checkCollision();
            checkApple();
            repaint();
        }
    }

    public static void main(String[] args) {
        javax.swing.JFrame frame = new javax.swing.JFrame();
        SnakeGame snakeGame = new SnakeGame();
        frame.setTitle("Snake Game");
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        frame.add(snakeGame);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
