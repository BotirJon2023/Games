package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WalkingCatAnimation extends JPanel implements ActionListener {

    private Timer timer;
    private int catX = 0;
    private double legAngle = 0;
    private boolean legDirection = true; // for leg swing direction

    public WalkingCatAnimation() {
        setPreferredSize(new Dimension(800, 300));
        setBackground(Color.WHITE);
        timer = new Timer(50, this); // ~20 FPS
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawCat(g2d, catX, 200);
    }

    private void drawCat(Graphics2D g, int x, int groundY) {
        int bodyX = x + 50;
        int bodyY = groundY - 60;

        // Body
        g.setColor(Color.GRAY);
        g.fillOval(bodyX, bodyY, 100, 60);

        // Head
        g.fillOval(bodyX + 90, bodyY - 10, 40, 40);

        // Ears
        g.fillPolygon(new int[]{bodyX + 100, bodyX + 108, bodyX + 116}, new int[]{bodyY - 10, bodyY - 30, bodyY - 10}, 3);
        g.fillPolygon(new int[]{bodyX + 120, bodyX + 128, bodyX + 136}, new int[]{bodyY - 10, bodyY - 30, bodyY - 10}, 3);

        // Eyes
        g.setColor(Color.BLACK);
        g.fillOval(bodyX + 110, bodyY, 10, 15);
        g.fillOval(bodyX + 125, bodyY, 10, 15);

        // Nose
        g.setColor(Color.PINK);
        g.fillOval(bodyX + 118, bodyY + 10, 6, 4);

        // Tail
        int tailX = bodyX;
        int tailY = bodyY + 20;
        drawCurvedTail(g, tailX, tailY, (int)(Math.sin(legAngle) * 10));

        // Legs
        int frontLegY = (int)(Math.sin(legAngle) * 10);
        int backLegY = (int)(Math.sin(legAngle + Math.PI) * 10);

        // Front legs
        g.fillRect(x + 60, groundY + frontLegY, 8, 20);
        g.fillRect(x + 90, groundY + frontLegY, 8, 20);

        // Back legs
        g.fillRect(x + 130, groundY + backLegY, 8, 20);
        g.fillRect(x + 160, groundY + backLegY, 8, 20);
    }

    private void drawCurvedTail(Graphics2D g, int x, int y, int offset) {
        g.setStroke(new BasicStroke(6));
        g.setColor(Color.GRAY);
        int length = 60;
        int controlOffset = offset * 5;
        g.drawArc(x - length / 2, y - length / 2, length, length, 90, 135 + controlOffset);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        catX += 5;
        if (catX > getWidth()) {
            catX = -200; // Reset position
        }

        // Animate legs
        legAngle += 0.2;
        if (legAngle > Math.PI * 2) {
            legAngle = 0;
        }

        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Walking Cat Animation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new WalkingCatAnimation());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}