package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class DonkeyKongClone extends JPanel implements ActionListener, KeyListener {

    private final Timer timer;
    private final Player player;
    private final List<Platform> platforms;
    private final int GRAVITY = 1;

    public DonkeyKongClone() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(17, this); // Roughly 60 FPS
        player = new Player(100, 500);
        platforms = new ArrayList<>();

        // Add some platforms
        platforms.add(new Platform(50, 400, 200, 10));
        platforms.add(new Platform(300, 300, 200, 10));
        platforms.add(new Platform(550, 200, 200, 10));

        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        player.draw(g);
        for (Platform p : platforms) {
            p.draw(g);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        player.update(platforms);
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        player.keyPressed(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        player.keyReleased(e.getKeyCode());
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Donkey Kong Clone");
        DonkeyKongClone gamePanel = new DonkeyKongClone();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(gamePanel);
        frame.pack();
        frame.setVisible(true);
    }

    class Player {
        private int x, y, dx, dy;
        private boolean jumping, falling, movingLeft, movingRight;
        private final int WIDTH = 50, HEIGHT = 50;
        private final int JUMP_STRENGTH = -15;
        private final int MOVE_SPEED = 5;

        public Player(int startX, int startY) {
            this.x = startX;
            this.y = startY;
        }

        public void update(List<Platform> platforms) {
            if (movingLeft) dx = -MOVE_SPEED;
            else if (movingRight) dx = MOVE_SPEED;
            else dx = 0;

            // Apply gravity
            if (falling) dy += GRAVITY;
            if (jumping) dy = JUMP_STRENGTH;

            x += dx;
            y += dy;

            // Collision with platforms
            boolean onGround = false;
            for (Platform p : platforms) {
                if (y + HEIGHT <= p.y + 10 && y + HEIGHT + dy >= p.y &&
                        x + WIDTH > p.x && x < p.x + p.width) {
                    y = p.y - HEIGHT;
                    dy = 0;
                    falling = false;
                    jumping = false;
                    onGround = true;
                    break;
                }
            }

            if (!onGround) falling = true;
        }

        public void draw(Graphics g) {
            g.setColor(Color.BLUE);
            g.fillRect(x, y, WIDTH, HEIGHT);
        }

        public void keyPressed(int keyCode) {
            if (keyCode == KeyEvent.VK_LEFT) movingLeft = true;
            if (keyCode == KeyEvent.VK_RIGHT) movingRight = true;
            if (keyCode == KeyEvent.VK_SPACE && !falling) jumping = true;
        }

        public void keyReleased(int keyCode) {
            if (keyCode == KeyEvent.VK_LEFT) movingLeft = false;
            if (keyCode == KeyEvent.VK_RIGHT) movingRight = false;
        }
    }

    class Platform {
        int x, y, width, height;

        public Platform(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void draw(Graphics g) {
            g.setColor(Color.RED);
            g.fillRect(x, y, width, height);
        }
    }
}