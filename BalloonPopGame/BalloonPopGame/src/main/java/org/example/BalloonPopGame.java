package org.example;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class BalloonPopGame extends JPanel implements ActionListener, MouseListener {

    private Timer timer;
    private List<Balloon> balloons;
    private int score;
    private final int panelWidth = 600;
    private final int panelHeight = 800;

    public BalloonPopGame() {
        setPreferredSize(new Dimension(panelWidth, panelHeight));
        setBackground(Color.CYAN);
        setFocusable(true);
        addMouseListener(this);

        balloons = new ArrayList<>();
        score = 0;
        timer = new Timer(15, this);
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Update the position of balloons
        for (Balloon balloon : balloons) {
            balloon.updatePosition();
        }

        // Remove balloons that have floated off the screen
        balloons.removeIf(balloon -> balloon.getY() < 0);

        // Add new balloons randomly
        if (Math.random() < 0.05) {
            balloons.add(new Balloon());
        }

        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw all balloons
        for (Balloon balloon : balloons) {
            balloon.draw(g);
        }

        // Draw the score
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.BLACK);
        g.drawString("Score: " + score, 20, 40);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Point mousePos = e.getPoint();
        for (Balloon balloon : balloons) {
            if (balloon.contains(mousePos)) {
                balloons.remove(balloon);
                score++;
                break;
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Balloon Pop Game");
        BalloonPopGame game = new BalloonPopGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game);
        frame.pack();
        frame.setVisible(true);
    }

    // Balloon class to represent each balloon
    class Balloon {
        private int x, y, size;
        private Color color;
        private Random rand;

        public Balloon() {
            rand = new Random();
            size = rand.nextInt(30) + 30; // Balloons between 30px and 60px in diameter
            x = rand.nextInt(panelWidth - size); // Random X position
            y = panelHeight + size; // Start from the bottom
            color = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)); // Random color
        }

        public void updatePosition() {
            y -= 2; // Move upwards
        }

        public void draw(Graphics g) {
            g.setColor(color);
            g.fillOval(x, y, size, size); // Draw balloon
            g.setColor(Color.BLACK);
            g.drawOval(x, y, size, size); // Draw border around balloon
        }

        public boolean contains(Point p) {
            int distX = p.x - (x + size / 2);
            int distY = p.y - (y + size / 2);
            return (distX * distX + distY * distY) <= (size / 2) * (size / 2);
        }

        public int getY() {
            return y;
        }
    }
}