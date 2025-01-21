package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class HelicopterGame extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private Helicopter helicopter;
    private ArrayList<Pipe> pipes;
    private int score;
    private boolean gameOver;

    public HelicopterGame() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.CYAN);
        setFocusable(true);
        addKeyListener(this);

        helicopter = new Helicopter(100, 250);
        pipes = new ArrayList<>();
        score = 0;
        gameOver = false;

        timer = new Timer(20, this);
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        helicopter.update();
        for (Pipe pipe : pipes) {
            pipe.update();
        }

        if (helicopter.getY() > getHeight() || helicopter.getY() < 0) {
            gameOver = true;
        }

        for (Pipe pipe : pipes) {
            if (pipe.getX() + Pipe.WIDTH < 0) {
                pipes.remove(pipe);
                score++;
            } else if (pipe.collidesWith(helicopter)) {
                gameOver = true;
            }
        }

        if (pipes.size() == 0 || pipes.get(pipes.size() - 1).getX() < getWidth() - 300) {
            pipes.add(new Pipe(getWidth(), new Random().nextInt(200) + 100));
        }

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            helicopter.flap();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    private void draw(Graphics g) {
        helicopter.draw(g);

        for (Pipe pipe : pipes) {
            pipe.draw(g);
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        if (gameOver) {
            g.drawString("Game Over! Score: " + score, getWidth() / 4, getHeight() / 2);
        } else {
            g.drawString("Score: " + score, 10, 30);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Helicopter Game");
        HelicopterGame game = new HelicopterGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game);
        frame.pack();
        frame.setVisible(true);
    }

    // Helicopter class
    private class Helicopter {
        private static final int WIDTH = 60;
        private static final int HEIGHT = 40;
        private int x, y;
        private int velocity;
        private final int gravity = 1;

        public Helicopter(int x, int y) {
            this.x = x;
            this.y = y;
            this.velocity = 0;
        }

        public void update() {
            velocity += gravity;
            y += velocity;
        }

        public void flap() {
            velocity = -15; // Flap gives a boost
        }

        public int getY() {
            return y;
        }

        public void draw(Graphics g) {
            g.setColor(Color.RED);
            g.fillRect(x, y, WIDTH, HEIGHT);
        }
    }

    // Pipe class
    private class Pipe {
        public static final int WIDTH = 60;
        private int x, gapY;
        private static final int GAP_SIZE = 150;
        private static final int PIPE_HEIGHT = 600;

        public Pipe(int x, int gapY) {
            this.x = x;
            this.gapY = gapY;
        }

        public void update() {
            x -= 5;
        }

        public boolean collidesWith(Helicopter helicopter) {
            return helicopter.getY() < gapY || helicopter.getY() + Helicopter.HEIGHT > gapY + GAP_SIZE;
        }

        public int getX() {
            return x;
        }

        public void draw(Graphics g) {
            g.setColor(Color.GREEN);
            g.fillRect(x, 0, WIDTH, gapY);
            g.fillRect(x, gapY + GAP_SIZE, WIDTH, PIPE_HEIGHT - gapY - GAP_SIZE);
        }
    }
}
