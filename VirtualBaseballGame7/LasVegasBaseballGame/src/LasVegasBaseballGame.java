// LasVegasBaseballGame.java
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class LasVegasBaseballGame extends JFrame {
    public LasVegasBaseballGame() {
        setTitle("⚾ LAS VEGAS - Strip Baseball 🎰");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        add(new VegasGamePanel());
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LasVegasBaseballGame());
    }
}

class VegasGamePanel extends JPanel implements ActionListener {
    private Timer timer;
    private double ballX, ballY;
    private int score = 0;
    private ArrayList<CasinoLight> lights = new ArrayList<>();

    public VegasGamePanel() {
        setBackground(Color.BLACK);
        timer = new Timer(16, this);
        timer.start();
        ballX = getWidth()/2;
        ballY = getHeight()/2;

        for (int i = 0; i < 50; i++) {
            lights.add(new CasinoLight());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Vegas Strip skyline
        drawVegasStrip(g2d);

        // Casinos
        drawCasinos(g2d);

        // Welcome sign
        drawWelcomeSign(g2d);

        // Baseball field
        drawCasinoField(g2d);

        drawBall(g2d);
        drawUI(g2d);

        for (CasinoLight light : lights) {
            light.draw(g2d);
        }
    }

    private void drawVegasStrip(Graphics2D g2d) {
        Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN};

        for (int i = 0; i < 12; i++) {
            int x = 50 + i * 90;
            int height = 200 + (int)(Math.random() * 150);
            g2d.setColor(colors[i % colors.length]);
            g2d.fillRect(x, getHeight() - height, 70, height);

            // Windows
            g2d.setColor(Color.YELLOW);
            for (int w = 0; w < 4; w++) {
                g2d.fillRect(x + 10 + w * 15, getHeight() - height + 20, 8, 15);
            }
        }
    }

    private void drawCasinos(Graphics2D g2d) {
        // The Luxor pyramid
        int[] x = {800, 900, 1000};
        int[] y = {getHeight() - 300, getHeight() - 500, getHeight() - 300};
        g2d.setColor(new Color(255, 215, 0));
        g2d.fillPolygon(x, y, 3);

        // Light beam
        g2d.setColor(new Color(255, 255, 0, 50));
        g2d.fillRect(850, 0, 100, getHeight());
    }

    private void drawWelcomeSign(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.fillRect(100, 100, 300, 80);
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("WELCOME TO", 130, 130);
        g2d.setColor(Color.BLUE);
        g2d.drawString("LAS VEGAS", 150, 160);
    }

    private void drawCasinoField(Graphics2D g2d) {
        // Diamond-shaped field
        int[] x = {getWidth()/2, getWidth()/2 + 200, getWidth()/2, getWidth()/2 - 200};
        int[] y = {getHeight() - 150, getHeight() - 350, getHeight() - 400, getHeight() - 350};
        g2d.setColor(new Color(0, 100, 0));
        g2d.fillPolygon(x, y, 4);

        // Poker chip bases
        g2d.setColor(new Color(255, 215, 0));
        for (int i = 0; i < 4; i++) {
            g2d.fillOval(x[i] - 15, y[i] - 15, 30, 30);
        }
    }

    private void drawBall(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int)ballX - 10, (int)ballY - 10, 20, 20);
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.setColor(Color.YELLOW);
        g2d.drawString("🎰 VEGAS: " + score, 50, 200);
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.drawString("💰 JACKPOT LEAGUE", 50, 240);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (CasinoLight light : lights) {
            light.update();
        }
        repaint();
    }

    class CasinoLight {
        double x, y;
        boolean on;

        CasinoLight() {
            x = Math.random() * 1200;
            y = Math.random() * 800;
            on = true;
        }

        void update() {
            on = Math.random() > 0.9;
        }

        void draw(Graphics2D g2d) {
            if (on) {
                g2d.setColor(new Color((int)(Math.random() * 255), (int)(Math.random() * 255), (int)(Math.random() * 255)));
                g2d.fillOval((int)x, (int)y, 6, 6);
            }
        }
    }
}