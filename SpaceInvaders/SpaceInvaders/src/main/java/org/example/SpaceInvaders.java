package org.example;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class SpaceInvaders extends JPanel {
    private int shipX = 300, shipWidth = 40, shipHeight = 20;
    private ArrayList<Rectangle> bullets = new ArrayList<>();
    private ArrayList<Rectangle> invaders = new ArrayList<>();
    private final int INVADER_WIDTH = 40, INVADER_HEIGHT = 40;
    private boolean gameOver = false;

    public SpaceInvaders() {
        this.setPreferredSize(new Dimension(600, 600));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);

        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT && shipX > 0) {
                    shipX -= 10;
                }
                if (e.getKeyCode() == KeyEvent.VK_RIGHT && shipX < getWidth() - shipWidth) {
                    shipX += 10;
                }
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    bullets.add(new Rectangle(shipX + shipWidth / 2 - 5, getHeight() - 50, 10, 20));
                }
            }
        });

        Timer timer = new Timer(15, e -> {
            if (!gameOver) {
                moveBullets();
                checkCollisions();
                repaint();
            }
        });
        timer.start();

        spawnInvaders();
    }

    private void moveBullets() {
        ArrayList<Rectangle> toRemove = new ArrayList<>();
        for (Rectangle bullet : bullets) {
            bullet.y -= 10;
            if (bullet.y < 0) {
                toRemove.add(bullet);
            }
        }
        bullets.removeAll(toRemove);
    }

    private void spawnInvaders() {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 10; j++) {
                invaders.add(new Rectangle(j * INVADER_WIDTH + 50, i * INVADER_HEIGHT + 50, INVADER_WIDTH, INVADER_HEIGHT));
            }
        }
    }

    private void checkCollisions() {
        for (Rectangle bullet : bullets) {
            for (Rectangle invader : invaders) {
                if (bullet.intersects(invader)) {
                    invaders.remove(invader);
                    bullets.remove(bullet);
                    break;
                }
            }
        }

        for (Rectangle invader : invaders) {
            if (invader.intersects(new Rectangle(shipX, getHeight() - shipHeight, shipWidth, shipHeight))) {
                gameOver = true;
                break;
            }
        }

        if (invaders.isEmpty()) {
            gameOver = true;
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.GREEN);
        for (Rectangle bullet : bullets) {
            g.fillRect(bullet.x, bullet.y, bullet.width, bullet.height);
        }

        g.setColor(Color.RED);
        for (Rectangle invader : invaders) {
            g.fillRect(invader.x, invader.y,);
        }
    }
}
