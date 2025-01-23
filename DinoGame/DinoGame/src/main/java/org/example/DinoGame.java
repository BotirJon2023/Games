package org.example;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class DinoGame extends JPanel implements ActionListener, KeyListener {

    private Timer timer;
    private int score;
    private Dino dino;
    private ArrayList<Cactus> cacti;
    private boolean isJumping;
    private boolean isGameOver;

    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 200;
    public static final int GROUND_Y = 150;

    public DinoGame() {
        this.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        this.setBackground(Color.WHITE);
        this.addKeyListener(this);
        this.setFocusable(true);
        this.score = 0;
        this.dino = new Dino(50, GROUND_Y - Dino.HEIGHT);
        this.cacti = new ArrayList<>();
        this.isJumping = false;
        this.isGameOver = false;
        this.timer = new Timer(1000 / 60, this); // 60 FPS
        this.timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isGameOver) {
            moveCacti();
            checkCollisions();
            dino.move();
            repaint();
        }
    }

    private void moveCacti() {
        // Add new cactus randomly
        if (Math.random() < 0.02) {
            cacti.add(new Cactus(WINDOW_WIDTH, GROUND_Y - Cactus.HEIGHT));
        }

        // Move each cactus to the left
        for (int i = 0; i < cacti.size(); i++) {
            Cactus cactus = cacti.get(i);
            cactus.move();

            if (cactus.getX() < 0) {
                cacti.remove(i);
                score++;
            }
        }
    }

    private void checkCollisions() {
        for (Cactus cactus : cacti) {
            if (dino.getBounds().intersects(cactus.getBounds())) {
                isGameOver = true;
                timer.stop();
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawGround(g);
        drawDino(g);
        drawCacti(g);
        drawScore(g);
        if (isGameOver) {
            drawGameOver(g);
        }
    }

    private void drawGround(Graphics g) {
        g.setColor(Color.GRAY);
        g.fillRect(0, GROUND_Y, WINDOW_WIDTH, 50);
    }

    private void drawDino(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillRect(dino.getX(), dino.getY(), Dino.WIDTH, Dino.HEIGHT);
    }

    private void drawCacti(Graphics g) {
        g.setColor(Color.RED);
        for (Cactus cactus : cacti) {
            g.fillRect(cactus.getX(), cactus.getY(), Cactus.WIDTH, Cactus.HEIGHT);
        }
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Score: " + score, 10, 30);
    }

    private void drawGameOver(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.drawString("GAME OVER", 200, 100);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && !isJumping && !isGameOver) {
            isJumping = true;
            dino.jump();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            isJumping = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Dino Game");
        DinoGame game = new DinoGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Dino class to represent the dinosaur
    class Dino {

        public static final int WIDTH = 20;
        public static final int HEIGHT = 30;
        private int x, y;
        private int ySpeed;

        public Dino(int x, int y) {
            this.x = x;
            this.y = y;
            this.ySpeed = 0;
        }

        public void move() {
            if (y < GROUND_Y - HEIGHT) {
                ySpeed += 1; // gravity
            } else {
                y = GROUND_Y - HEIGHT;
                ySpeed = 0;
            }
            y += ySpeed;
        }

        public void jump() {
            if (y == GROUND_Y - HEIGHT) {
                ySpeed = -15;
            }
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, WIDTH, HEIGHT);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    // Cactus class to represent the obstacles
    class Cactus {

        public static final int WIDTH = 20;
        public static final int HEIGHT = 30;
        private int x, y;

        public Cactus(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void move() {
            x -= 5;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, WIDTH, HEIGHT);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }
}
