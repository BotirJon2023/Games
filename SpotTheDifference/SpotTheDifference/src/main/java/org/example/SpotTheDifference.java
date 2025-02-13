package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import javax.imageio.ImageIO;

public class SpotTheDifference extends JFrame {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private BufferedImage image1, image2;
    private boolean[][] differencesSpotted;
    private int differenceCount;

    public SpotTheDifference() {
        super("Spot The Difference Game");

        // Attempt to load the images
        try {
            image1 = ImageIO.read(new File("image1.png"));  // Change path as needed
            image2 = ImageIO.read(new File("image2.png"));  // Change path as needed

            if (image1 == null || image2 == null) {
                throw new Exception("One or both images could not be loaded.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading images: " + e.getMessage());
            System.exit(1);  // Exit the game if images can't be loaded
        }

        differencesSpotted = new boolean[image1.getWidth()][image1.getHeight()];
        differenceCount = 0;

        this.setSize(WIDTH, HEIGHT);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                if (x >= 0 && x < image1.getWidth() && y >= 0 && y < image1.getHeight()) {
                    checkDifference(x, y);
                }
            }
        });
        this.setVisible(true);
    }

    public void paint(Graphics g) {
        super.paint(g);

        // Check if images are null before attempting to draw
        if (image1 == null || image2 == null) {
            g.setColor(Color.RED);
            g.drawString("Error loading images.", 10, 30);
            return;  // Exit the method to prevent further drawing
        }

        // Draw the images on the frame
        g.drawImage(image1, 0, 0, null);
        g.drawImage(image2, image1.getWidth(), 0, null);

        // Highlight spotted differences
        g.setColor(Color.RED);
        for (int i = 0; i < image1.getWidth(); i++) {
            for (int j = 0; j < image1.getHeight(); j++) {
                if (differencesSpotted[i][j]) {
                    g.fillRect(i, j, 5, 5);
                }
            }
        }

        // Display difference count
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Differences found: " + differenceCount, 10, 30);
    }

    private void checkDifference(int x, int y) {
        if (differencesSpotted[x][y]) {
            return; // Already spotted this difference
        }

        // Check if the pixel at (x, y) is different between the two images
        if (isDifference(x, y)) {
            differencesSpotted[x][y] = true;
            differenceCount++;
            repaint();
            checkWin();
        }
    }

    private boolean isDifference(int x, int y) {
        int rgb1 = image1.getRGB(x, y);
        int rgb2 = image2.getRGB(x, y);
        return rgb1 != rgb2;
    }

    private void checkWin() {
        if (differenceCount == getTotalDifferences()) {
            JOptionPane.showMessageDialog(this, "You Win! All differences found.");
            System.exit(0);
        }
    }

    private int getTotalDifferences() {
        int count = 0;
        for (int i = 0; i < image1.getWidth(); i++) {
            for (int j = 0; j < image1.getHeight(); j++) {
                if (isDifference(i, j)) {
                    count++;
                }
            }
        }
        return count;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SpotTheDifference());
    }
}