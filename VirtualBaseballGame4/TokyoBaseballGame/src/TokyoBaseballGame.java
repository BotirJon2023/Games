// TokyoBaseballGame.java
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class TokyoBaseballGame extends JFrame {
    public TokyoBaseballGame() {
        setTitle("⚾ TOKYO - Neon Cyber Baseball 🇯🇵");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        add(new TokyoGamePanel());
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TokyoBaseballGame());
    }
}

class TokyoGamePanel extends JPanel implements ActionListener {
    private Timer timer;
    private double ballX, ballY;
    private ArrayList<NeonLight> neonLights = new ArrayList<>();
    private int score = 0;
    private boolean isAnimating = false;

    public TokyoGamePanel() {
        setBackground(Color.BLACK);
        timer = new Timer(16, this);
        timer.start();
        ballX = getWidth()/2;
        ballY = getHeight()/2;

        // Create neon lights
        for (int i = 0; i < 20; i++) {
            neonLights.add(new NeonLight());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Tokyo skyline
        drawTokyoSkyline(g2d);

        // Neon signs
        drawNeonSigns(g2d);

        // Baseball field with neon edges
        drawNeonField(g2d);

        drawBall(g2d);
        drawUI(g2d);
    }

    private void drawTokyoSkyline(Graphics2D g2d) {
        for (int i = 0; i < 15; i++) {
            int height = 100 + (int)(Math.random() * 200);
            g2d.setColor(new Color(50, 100 + i * 10, 150));
            g2d.fillRect(50 + i * 80, getHeight() - height, 60, height);
        }

        // Tokyo Tower
        g2d.setColor(new Color(255, 100, 100));
        g2d.fillRect(600, getHeight() - 400, 40, 400);
        g2d.fillOval(590, getHeight() - 450, 60, 60);
    }

    private void drawNeonSigns(Graphics2D g2d) {
        String[] signs = {"⚾ BASEBALL", "🎌 SAMURAI", "🍜 RAMEN", "🌸 SAKURA"};
        g2d.setFont(new Font("Monospaced", Font.BOLD, 24));

        for (int i = 0; i < signs.length; i++) {
            g2d.setColor(new Color(0, 255, (int)(Math.sin(System.currentTimeMillis() * 0.003 + i) * 100 + 155)));
            g2d.drawString(signs[i], 800, 100 + i * 50);
        }
    }

    private void drawNeonField(Graphics2D g2d) {
        // Glowing field
        g2d.setColor(new Color(0, 100, 0, 150));
        g2d.fillOval(getWidth()/2 - 250, getHeight() - 350, 500, 300);

        g2d.setColor(new Color(0, 255, 0));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(getWidth()/2 - 250, getHeight() - 350, 500, 300);

        for (NeonLight light : neonLights) {
            light.draw(g2d);
        }
    }

    private void drawBall(Graphics2D g2d) {
        // Glowing ball
        for (int i = 3; i > 0; i--) {
            g2d.setColor(new Color(255, 100, 100, 50 / i));
            g2d.fillOval((int)ballX - 10 - i, (int)ballY - 10 - i, 20 + i * 2, 20 + i * 2);
        }
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int)ballX - 10, (int)ballY - 10, 20, 20);
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setFont(new Font("Monospaced", Font.BOLD, 32));
        g2d.setColor(new Color(0, 255, 255));
        g2d.drawString("TOKYO SCORE: " + score, 50, 60);
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 16));
        g2d.drawString("NEON BASEBALL ⚡", 50, 100);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (NeonLight light : neonLights) {
            light.update();
        }
        repaint();
    }

    class NeonLight {
        double x, y;
        double brightness;

        NeonLight() {
            x = Math.random() * 1200;
            y = Math.random() * 800;
            brightness = Math.random();
        }

        void update() {
            brightness += (Math.random() - 0.5) * 0.1;
            brightness = Math.max(0.3, Math.min(1, brightness));
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(255, 0, 255, (int)(brightness * 100)));
            g2d.fillOval((int)x, (int)y, 5, 5);
        }
    }
}