// NewYorkBaseballGame.java
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class NewYorkBaseballGame extends JFrame {
    public NewYorkBaseballGame() {
        setTitle("⚾ NEW YORK - Big Apple Baseball 🗽");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        add(new NYGamePanel());
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NewYorkBaseballGame());
    }
}

class NYGamePanel extends JPanel implements ActionListener {
    private Timer timer;
    private double ballX, ballY;
    private int score = 0;
    private ArrayList<Taxi> taxis = new ArrayList<>();

    public NYGamePanel() {
        setBackground(new Color(135, 206, 235));
        timer = new Timer(16, this);
        timer.start();
        ballX = getWidth()/2;
        ballY = getHeight()/2;

        for (int i = 0; i < 15; i++) {
            taxis.add(new Taxi());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // NYC Skyline
        drawNYCSkyline(g2d);

        // Statue of Liberty
        drawStatueOfLiberty(g2d);

        // Times Square
        drawTimesSquare(g2d);

        // Baseball field
        drawYankeeStadium(g2d);

        drawBall(g2d);
        drawUI(g2d);

        for (Taxi taxi : taxis) {
            taxi.draw(g2d);
        }
    }

    private void drawNYCSkyline(Graphics2D g2d) {
        int[] heights = {300, 450, 380, 420, 350, 400, 380, 350, 420, 380, 350};
        g2d.setColor(new Color(100, 100, 120));

        for (int i = 0; i < heights.length; i++) {
            g2d.fillRect(50 + i * 90, getHeight() - heights[i], 70, heights[i]);
        }

        // Empire State Building
        g2d.setColor(new Color(120, 120, 140));
        g2d.fillRect(450, getHeight() - 450, 80, 450);
        g2d.fillRect(470, getHeight() - 480, 40, 30);
    }

    private void drawStatueOfLiberty(Graphics2D g2d) {
        g2d.setColor(new Color(100, 150, 100));
        g2d.fillRect(900, getHeight() - 250, 40, 100);
        g2d.fillOval(895, getHeight() - 270, 50, 50);

        // Torch
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(930, getHeight() - 290, 15, 15);
    }

    private void drawTimesSquare(Graphics2D g2d) {
        // Billboards
        g2d.setColor(Color.RED);
        g2d.fillRect(800, 100, 200, 60);
        g2d.setColor(Color.WHITE);
        g2d.drawString("BROADWAY", 830, 140);

        g2d.setColor(Color.BLUE);
        g2d.fillRect(800, 170, 200, 50);
        g2d.setColor(Color.WHITE);
        g2d.drawString("TIMES SQ", 840, 205);
    }

    private void drawYankeeStadium(Graphics2D g2d) {
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillOval(getWidth()/2 - 220, getHeight() - 350, 440, 300);

        g2d.setColor(new Color(0, 0, 100));
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("YANKEE STADIUM", getWidth()/2 - 80, getHeight() - 320);
    }

    private void drawBall(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int)ballX - 10, (int)ballY - 10, 20, 20);
        g2d.setColor(Color.RED);
        g2d.drawLine((int)ballX, (int)ballY - 8, (int)ballX, (int)ballY + 8);
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.setColor(new Color(0, 100, 0));
        g2d.drawString("🗽 NEW YORK: " + score, 50, 60);
        g2d.drawString("⚾ THE BIG APPLE", 50, 100);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (Taxi taxi : taxis) {
            taxi.update();
        }
        repaint();
    }

    class Taxi {
        double x;

        Taxi() {
            x = Math.random() * 1200;
        }

        void update() {
            x += 2;
            if (x > 1200) {
                x = -50;
            }
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(Color.YELLOW);
            g2d.fillRect((int)x, getHeight() - 100, 40, 20);
            g2d.fillRect((int)x + 10, getHeight() - 110, 20, 10);
            g2d.setColor(Color.BLACK);
            g2d.fillOval((int)x + 5, getHeight() - 85, 8, 8);
            g2d.fillOval((int)x + 27, getHeight() - 85, 8, 8);
        }
    }
}