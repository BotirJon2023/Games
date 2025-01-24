package org.example;

import java.awt.*;
import java.awt.event.*;
import javax.management.timer.Timer;
import javax.swing.*;
import java.util.*;

public class BubbleShooterGame extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int BUBBLE_RADIUS = 20;
    private static final int SHOOTER_RADIUS = 15;
    private static final int BUBBLE_SPEED = 10;

    private Timer timer;
    private ArrayList<Bubble> bubbles;
    private BubbleShooter shooter;
    private Point targetPoint;

    public BubbleShooterGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addMouseListener(this);
        addMouseMotionListener(this);

        shooter = new BubbleShooter(WIDTH / 2, HEIGHT - 50);
        bubbles = new ArrayList<>();
        targetPoint = new Point(WIDTH / 2, HEIGHT - 50);

        timer = new Timer();
        timer.start();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw the shooter
        g2d.setColor(Color.RED);
        g2d.fillOval(shooter.getX() - SHOOTER_RADIUS, shooter.getY() - SHOOTER_RADIUS, SHOOTER_RADIUS * 2, SHOOTER_RADIUS * 2);

        // Draw the bubbles
        for (Bubble bubble : bubbles) {
            g2d.setColor(bubble.getColor());
            g2d.fillOval(bubble.getX() - BUBBLE_RADIUS, bubble.getY() - BUBBLE_RADIUS, BUBBLE_RADIUS * 2, BUBBLE_RADIUS * 2);
        }

        // Draw the aiming line
        g2d.setColor(Color.WHITE);
        g2d.drawLine(shooter.getX(), shooter.getY(), targetPoint.x, targetPoint.y);
    }

    public void actionPerformed(ActionEvent e) {
        // Update the bubbles
        ArrayList<Bubble> toRemove = new ArrayList<>();
        for (Bubble bubble : bubbles) {
            bubble.move();
            if (bubble.getY() < 0) {
                toRemove.add(bubble);
            }
        }
        bubbles.removeAll(toRemove);

        // Repaint the screen
        repaint();
    }

    public void mousePressed(MouseEvent e) {
        if (shooter != null) {
            double angle = Math.atan2(targetPoint.y - shooter.getY(), targetPoint.x - shooter.getX());
            Bubble newBubble = new Bubble(shooter.getX(), shooter.getY(), angle);
            bubbles.add(newBubble);
        }
    }

    public void mouseMoved(MouseEvent e) {
        targetPoint.setLocation(e.getX(), e.getY());
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Bubble Shooter Game");
        BubbleShooterGame gamePanel = new BubbleShooterGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(gamePanel);
        frame.pack();
        frame.setVisible(true);
    }

    // Bubble class to represent the bubble objects in the game
    private class Bubble {
        private int x, y;
        private double angle;
        private Color color;

        public Bubble(int startX, int startY, double angle) {
            this.x = startX;
            this.y = startY;
            this.angle = angle;
            this.color = new Color((int) (Math.random() * 0x1000000));
        }

        public void move() {
            x += (int) (BUBBLE_SPEED * Math.cos(angle));
            y += (int) (BUBBLE_SPEED * Math.sin(angle));
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Color getColor() {
            return color;
        }
    }

    // BubbleShooter class to represent the player's shooter
    private class BubbleShooter {
        private int x, y;

        public BubbleShooter(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }
}