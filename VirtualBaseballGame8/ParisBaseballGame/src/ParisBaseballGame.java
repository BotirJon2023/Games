// ParisBaseballGame.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.*;
import java.util.ArrayList;

public class ParisBaseballGame extends JFrame {
    public ParisBaseballGame() {
        setTitle("⚾ PARIS - Romantic Baseball 🇫🇷");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        add(new ParisGamePanel());
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ParisBaseballGame());
    }
}

class ParisGamePanel extends JPanel implements ActionListener {
    private Timer timer;
    private double ballX, ballY;
    private int score = 0;
    private ArrayList<HeartParticle> hearts = new ArrayList<>();

    public ParisGamePanel() {
        setBackground(new Color(200, 220, 255));
        timer = new Timer(16, this);
        timer.start();
        ballX = getWidth()/2;
        ballY = getHeight()/2;

        for (int i = 0; i < 30; i++) {
            hearts.add(new HeartParticle());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Paris sky
        GradientPaint sky = new GradientPaint(0, 0, new Color(255, 200, 200), 0, getHeight(), new Color(200, 180, 255));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Eiffel Tower
        drawEiffelTower(g2d);

        // Seine River
        g2d.setColor(new Color(100, 150, 200));
        g2d.fillRect(0, getHeight() - 200, getWidth(), 80);

        // Louvre
        drawLouvre(g2d);

        // Baseball field
        drawFrenchField(g2d);

        drawBall(g2d);
        drawUI(g2d);

        for (HeartParticle heart : hearts) {
            heart.draw(g2d);
        }
    }

    private void drawEiffelTower(Graphics2D g2d) {
        g2d.setColor(new Color(100, 80, 60));
        int[] x = {550, 600, 650, 620, 580};
        int[] y = {getHeight() - 400, getHeight() - 550, getHeight() - 400, getHeight() - 350, getHeight() - 350};
        g2d.fillPolygon(x, y, 5);

        // Cross beams
        for (int i = 0; i < 4; i++) {
            g2d.drawLine(570, getHeight() - 450 + i * 30, 630, getHeight() - 450 + i * 30);
        }

        // Light effect
        g2d.setColor(new Color(255, 255, 0, 50));
        for (int i = 0; i < 10; i++) {
            g2d.fillOval(580, getHeight() - 500 + i * 15, 40, 10);
        }
    }

    private void drawLouvre(Graphics2D g2d) {
        g2d.setColor(new Color(200, 180, 150));
        g2d.fillRect(100, getHeight() - 300, 300, 100);

        // Glass pyramid
        int[] x = {200, 250, 300};
        int[] y = {getHeight() - 300, getHeight() - 350, getHeight() - 300};
        g2d.setColor(new Color(150, 200, 255, 150));
        g2d.fillPolygon(x, y, 3);
    }

    private void drawFrenchField(Graphics2D g2d) {
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillOval(getWidth()/2 - 180, getHeight() - 320, 360, 250);

        // French flag colors on field
        g2d.setColor(new Color(0, 0, 255, 50));
        g2d.fillOval(getWidth()/2 - 180, getHeight() - 320, 120, 250);
        g2d.setColor(new Color(255, 0, 0, 50));
        g2d.fillOval(getWidth()/2 + 60, getHeight() - 320, 120, 250);
    }

    private void drawBall(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int)ballX - 10, (int)ballY - 10, 20, 20);
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 28));
        g2d.setColor(new Color(255, 100, 150));
        g2d.drawString("🗼 PARIS: " + score, 50, 60);
        g2d.drawString("💕 CITY OF LOVE", 50, 100);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (HeartParticle heart : hearts) {
            heart.update();
        }
        repaint();
    }

    class HeartParticle {
        double x, y;

        HeartParticle() {
            x = Math.random() * 1200;
            y = Math.random() * 800;
        }

        void update() {
            y -= 1;
            if (y < 0) {
                y = 800;
                x = Math.random() * 1200;
            }
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(255, 100, 150));
            g2d.fillOval((int)x, (int)y, 4, 4);
        }
    }
}