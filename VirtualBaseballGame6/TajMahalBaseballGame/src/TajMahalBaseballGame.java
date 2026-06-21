// TajMahalBaseballGame.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.*;

public class TajMahalBaseballGame extends JFrame {
    public TajMahalBaseballGame() {
        setTitle("⚾ TAJ MAHAL - Royal Baseball 🇮🇳");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        add(new TajGamePanel());
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TajMahalBaseballGame());
    }
}

class TajGamePanel extends JPanel implements ActionListener {
    private Timer timer;
    private double ballX, ballY;
    private int score = 0;
    private float hue = 0;

    public TajGamePanel() {
        setBackground(new Color(135, 206, 235));
        timer = new Timer(16, this);
        timer.start();
        ballX = getWidth()/2;
        ballY = getHeight()/2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Sky gradient
        GradientPaint sky = new GradientPaint(0, 0, new Color(255, 200, 150), 0, getHeight(), new Color(255, 150, 100));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Taj Mahal
        drawTajMahal(g2d);

        // Yamuna River
        g2d.setColor(new Color(100, 150, 200));
        g2d.fillRect(0, getHeight() - 250, getWidth(), 60);

        // Gardens
        drawMughalGardens(g2d);

        // Baseball field
        drawRoyalField(g2d);

        drawBall(g2d);
        drawUI(g2d);
    }

    private void drawTajMahal(Graphics2D g2d) {
        // Main dome
        g2d.setColor(Color.WHITE);
        g2d.fillArc(500, getHeight() - 500, 200, 150, 0, 180);

        // Main building
        g2d.fillRect(520, getHeight() - 380, 160, 130);

        // Side domes
        g2d.fillArc(470, getHeight() - 430, 80, 80, 0, 180);
        g2d.fillArc(650, getHeight() - 430, 80, 80, 0, 180);

        // Minarets
        for (int i = 0; i < 4; i++) {
            int x = 480 + i * 240;
            g2d.fillRect(x, getHeight() - 450, 30, 200);
            g2d.fillOval(x - 5, getHeight() - 470, 40, 40);
        }

        // Reflection
        g2d.setColor(new Color(255, 255, 255, 100));
        for (int i = 0; i < 4; i++) {
            int x = 480 + i * 240;
            g2d.fillRect(x, getHeight() - 190, 30, 40);
        }
    }

    private void drawMughalGardens(Graphics2D g2d) {
        g2d.setColor(new Color(34, 139, 34));
        for (int i = 0; i < 4; i++) {
            g2d.fillRect(100 + i * 250, getHeight() - 180, 100, 30);
        }
    }

    private void drawRoyalField(Graphics2D g2d) {
        g2d.setColor(new Color(0, 100, 0));
        g2d.fillOval(getWidth()/2 - 200, getHeight() - 350, 400, 250);

        // Gold border
        g2d.setColor(new Color(255, 215, 0));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(getWidth()/2 - 200, getHeight() - 350, 400, 250);
    }

    private void drawBall(Graphics2D g2d) {
        // Pearl-like ball
        RadialGradientPaint pearl = new RadialGradientPaint(
                (float)ballX, (float)ballY, 12,
                new float[]{0f, 1f},
                new Color[]{Color.WHITE, new Color(230, 230, 250)}
        );
        g2d.setPaint(pearl);
        g2d.fillOval((int)ballX - 10, (int)ballY - 10, 20, 20);
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 28));
        g2d.setColor(new Color(255, 215, 0));
        g2d.drawString("🕌 TAJ MAHAL: " + score, 50, 60);
        g2d.drawString("👑 ROYAL BASEBALL", 50, 100);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        hue += 0.01;
        repaint();
    }
}