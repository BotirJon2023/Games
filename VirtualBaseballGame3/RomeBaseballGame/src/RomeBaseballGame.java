// RomeBaseballGame.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.*;
import java.util.ArrayList;

public class RomeBaseballGame extends JFrame {
    public RomeBaseballGame() {
        setTitle("⚾ ROME - Gladiator Baseball 🇮🇹");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        add(new RomeGamePanel());
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RomeBaseballGame());
    }
}

class RomeGamePanel extends JPanel implements ActionListener {
    private Timer timer;
    private double ballX, ballY;
    private double batAngle = 0;
    private boolean swinging = false;
    private int score = 0;
    private String status = "⚔️ READY ⚔️";
    private ArrayList<Particle> particles = new ArrayList<>();

    public RomeGamePanel() {
        setPreferredSize(new Dimension(1200, 800));
        setBackground(new Color(255, 200, 150));
        timer = new Timer(16, this);
        timer.start();
        ballX = getWidth()/2;
        ballY = getHeight() - 200;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Sky
        g2d.setColor(new Color(255, 180, 100));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Colosseum
        drawColosseum(g2d);

        // Roman Forum ruins
        drawRomanForum(g2d);

        // Baseball field
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillOval(getWidth()/2 - 300, getHeight() - 350, 600, 400);

        drawBall(g2d);
        drawBat(g2d);
        drawUI(g2d);

        for (Particle p : particles) {
            p.draw(g2d);
        }
    }

    private void drawColosseum(Graphics2D g2d) {
        g2d.setColor(new Color(180, 140, 100));
        g2d.fillOval(100, getHeight() - 500, 300, 200);

        // Arches
        g2d.setColor(new Color(100, 70, 50));
        for (int i = 0; i < 8; i++) {
            g2d.fillRect(120 + i * 35, getHeight() - 450, 20, 40);
        }

        // Top tier
        g2d.fillRect(100, getHeight() - 500, 300, 30);
    }

    private void drawRomanForum(Graphics2D g2d) {
        g2d.setColor(new Color(200, 170, 130));
        for (int i = 0; i < 6; i++) {
            g2d.fillRect(450 + i * 40, getHeight() - 300, 15, 60);
        }
    }

    private void drawBall(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int)ballX - 10, (int)ballY - 10, 20, 20);
        g2d.setColor(Color.RED);
        g2d.drawLine((int)ballX, (int)ballY - 8, (int)ballX, (int)ballY + 8);
    }

    private void drawBat(Graphics2D g2d) {
        if (swinging) {
            g2d.rotate(Math.toRadians(batAngle), getWidth()/2, getHeight() - 200);
        }
        g2d.setColor(new Color(160, 82, 45));
        g2d.fillRoundRect(getWidth()/2 - 8, getHeight() - 220, 16, 70, 8, 8);
        if (swinging) {
            g2d.rotate(-Math.toRadians(batAngle), getWidth()/2, getHeight() - 200);
        }
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setFont(new Font("Roman", Font.BOLD, 28));
        g2d.setColor(new Color(255, 215, 0));
        g2d.drawString("🏛️ ROME SCORE: " + score, 50, 60);
        g2d.setFont(new Font("Roman", Font.ITALIC, 20));
        g2d.drawString(status, 50, 110);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (swinging) {
            batAngle += 12;
            if (batAngle >= 90) {
                swinging = false;
                batAngle = 0;
            }
        }

        particles.removeIf(p -> !p.update());
        repaint();
    }

    class Particle {
        double x, y;
        int life;

        Particle(double x, double y) {
            this.x = x;
            this.y = y;
            this.life = 30;
        }

        boolean update() {
            life--;
            return life > 0;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(255, 200, 0, life * 8));
            g2d.fillOval((int)x, (int)y, 6, 6);
        }
    }
}