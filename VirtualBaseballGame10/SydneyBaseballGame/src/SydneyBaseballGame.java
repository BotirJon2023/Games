// SydneyBaseballGame.java
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class SydneyBaseballGame extends JFrame {
    public SydneyBaseballGame() {
        setTitle("⚾ SYDNEY - Harbour Baseball 🇦🇺");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        add(new SydneyGamePanel());
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SydneyBaseballGame());
    }
}

class SydneyGamePanel extends JPanel implements ActionListener {
    private Timer timer;
    private double ballX, ballY;
    private int score = 0;
    private ArrayList<Wave> waves = new ArrayList<>();

    public SydneyGamePanel() {
        setBackground(new Color(100, 150, 200));
        timer = new Timer(16, this);
        timer.start();
        ballX = getWidth()/2;
        ballY = getHeight()/2;

        for (int i = 0; i < 20; i++) {
            waves.add(new Wave());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Sydney sky
        GradientPaint sky = new GradientPaint(0, 0, new Color(100, 150, 200), 0, getHeight(), new Color(255, 200, 150));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Sydney Harbour Bridge
        drawHarbourBridge(g2d);

        // Opera House
        drawOperaHouse(g2d);

        // Harbour
        g2d.setColor(new Color(50, 100, 150));
        g2d.fillRect(0, getHeight() - 200, getWidth(), 200);

        // Baseball field
        drawHarbourField(g2d);

        drawBall(g2d);
        drawUI(g2d);

        for (Wave wave : waves) {
            wave.draw(g2d);
        }
    }

    private void drawHarbourBridge(Graphics2D g2d) {
        g2d.setColor(new Color(100, 100, 120));
        g2d.fillRect(300, getHeight() - 350, 600, 40);

        // Arch
        g2d.drawArc(350, getHeight() - 450, 500, 200, 0, 180);

        // Towers
        g2d.fillRect(320, getHeight() - 400, 40, 200);
        g2d.fillRect(840, getHeight() - 400, 40, 200);
    }

    private void drawOperaHouse(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);

        // Sails
        int[][] sails = {
                {600, getHeight() - 250, 650, getHeight() - 400, 700, getHeight() - 250},
                {650, getHeight() - 250, 720, getHeight() - 420, 780, getHeight() - 250},
                {550, getHeight() - 250, 600, getHeight() - 380, 650, getHeight() - 250}
        };

        for (int[] sail : sails) {
            g2d.fillPolygon(new int[]{sail[0], sail[2], sail[4]}, new int[]{sail[1], sail[3], sail[5]}, 3);
        }
    }

    private void drawHarbourField(Graphics2D g2d) {
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillOval(getWidth()/2 - 200, getHeight() - 300, 400, 250);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(getWidth()/2 - 200, getHeight() - 300, 400, 250);
    }

    private void drawBall(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int)ballX - 10, (int)ballY - 10, 20, 20);
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.setColor(Color.WHITE);
        g2d.drawString("🦘 SYDNEY: " + score, 50, 60);
        g2d.drawString("🏄‍♂️ HARBOUR LEAGUE", 50, 100);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (Wave wave : waves) {
            wave.update();
        }
        repaint();
    }

    class Wave {
        double x;

        Wave() {
            x = Math.random() * 1200;
        }

        void update() {
            x += 1;
            if (x > 1200) {
                x = 0;
            }
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.drawArc((int)x, getHeight() - 180, 50, 20, 0, 180);
        }
    }
}