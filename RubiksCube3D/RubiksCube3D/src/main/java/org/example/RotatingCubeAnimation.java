package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RotatingCubeAnimation extends JPanel implements ActionListener {

    private Timer timer;
    private double angle = 0;
    private final int WIDTH = 600;
    private final int HEIGHT = 600;

    // Cube vertices (x, y, z)
    private final Point3D[] vertices = {
            new Point3D(-1, -1, -1), new Point3D(-1,  1, -1),
            new Point3D( 1,  1, -1), new Point3D( 1, -1, -1),
            new Point3D(-1, -1,  1), new Point3D(-1,  1,  1),
            new Point3D( 1,  1,  1), new Point3D( 1, -1,  1)
    };

    // Faces: each face is made of 4 vertex indices
    private final int[][] faces = {
            {0, 1, 2, 3}, // Back
            {4, 5, 6, 7}, // Front
            {0, 1, 5, 4}, // Left
            {2, 3, 7, 6}, // Right
            {1, 2, 6, 5}, // Top
            {0, 3, 7, 4}  // Bottom
    };

    // Face colors
    private final Color[] faceColors = {
            Color.red, Color.blue, Color.green,
            Color.yellow, Color.orange, Color.cyan
    };

    public RotatingCubeAnimation() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Point[] projectedPoints = new Point[vertices.length];

        // Rotate all points around Y and X axes
        for (int i = 0; i < vertices.length; i++) {
            Point3D rotated = vertices[i].rotateY(angle).rotateX(angle);
            projectedPoints[i] = rotated.project(WIDTH, HEIGHT, 200, 4);
        }

        // Draw faces
        for (int i = 0; i < faces.length; i++) {
            int[] face = faces[i];
            Polygon poly = new Polygon();
            for (int idx : face) {
                poly.addPoint(projectedPoints[idx].x, projectedPoints[idx].y);
            }
            g2d.setColor(faceColors[i]);
            g2d.fillPolygon(poly);
            g2d.setColor(Color.BLACK);
            g2d.drawPolygon(poly);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        angle += 0.03;
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Rotating Cube Animation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new RotatingCubeAnimation());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static class Point3D {
        double x, y, z;

        public Point3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Point3D rotateY(double angle) {
            double cosA = Math.cos(angle);
            double sinA = Math.sin(angle);
            return new Point3D(x * cosA + z * sinA, y, -x * sinA + z * cosA);
        }

        public Point3D rotateX(double angle) {
            double cosA = Math.cos(angle);
            double sinA = Math.sin(angle);
            return new Point3D(x, y * cosA - z * sinA, y * sinA + z * cosA);
        }

        public Point project(int screenWidth, int screenHeight, double fov, double distance) {
            double factor = fov / (distance + z);
            int px = (int)(x * factor + screenWidth / 2);
            int py = (int)(y * factor + screenHeight / 2);
            return new Point(px, py);
        }
    }
}