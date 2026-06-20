import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.*;
import java.util.ArrayList;

public class EgyptBaseballGame extends JFrame {
    public EgyptBaseballGame() {
        setTitle("⚾ EGYPT - Pyramid Baseball 🇪🇬");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        add(new EgyptGamePanel());
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EgyptBaseballGame());
    }
}

class EgyptGamePanel extends JPanel implements ActionListener {
    private Timer timer;
    private double ballX, ballY;
    private int score = 0;
    private ArrayList<SandParticle> sand = new ArrayList<>();

    public EgyptGamePanel() {
        setBackground(new Color(255, 220, 150));
        timer = new Timer(16, this);
        timer.start();
        ballX = getWidth()/2;
        ballY = getHeight()/2;

        for (int i = 0; i < 200; i++) {
            sand.add(new SandParticle());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Desert sky
        GradientPaint sky = new GradientPaint(0, 0, new Color(255, 180, 100), 0, getHeight(), new Color(255, 140, 50));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Sun
        g2d.setColor(new Color(255, 100, 50));
        g2d.fillOval(getWidth() - 150, 50, 80, 80);

        // Pyramids
        drawPyramids(g2d);

        // Sphinx
        drawSphinx(g2d);

        // Desert sand
        g2d.setColor(new Color(210, 180, 100));
        g2d.fillRect(0, getHeight() - 200, getWidth(), 200);

        // Baseball field on sand
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillOval(getWidth()/2 - 200, getHeight() - 300, 400, 250);

        drawBall(g2d);
        drawUI(g2d);

        for (SandParticle s : sand) {
            s.draw(g2d);
        }
    }

    private void drawPyramids(Graphics2D g2d) {
        // Great Pyramid
        int[] x1 = {100, 250, 400};
        int[] y1 = {getHeight() - 350, getHeight() - 550, getHeight() - 350};
        g2d.setColor(new Color(200, 170, 100));
        g2d.fillPolygon(x1, y1, 3);

        // Second Pyramid
        int[] x2 = {450, 550, 650};
        int[] y2 = {getHeight() - 350, getHeight() - 500, getHeight() - 350};
        g2d.fillPolygon(x2, y2, 3);

        // Pyramid lines
        g2d.setColor(new Color(150, 120, 70));
        for (int i = 0; i < 5; i++) {
            g2d.drawLine(100 + i * 30, getHeight() - 350 + i * 40, 400 - i * 30, getHeight() - 350 + i * 40);
        }
    }

    private void drawSphinx(Graphics2D g2d) {
        g2d.setColor(new Color(180, 140, 90));
        g2d.fillRect(700, getHeight() - 300, 150, 60);
        g2d.fillOval(720, getHeight() - 330, 80, 60);
        g2d.fillRect(730, getHeight() - 340, 30, 40);
    }

    private void drawBall(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int)ballX - 10, (int)ballY - 10, 20, 20);
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setFont(new Font("Papyrus", Font.BOLD, 28));
        g2d.setColor(new Color(100, 50, 20));
        g2d.drawString("🏺 EGYPT: " + score, 50, 60);
        g2d.drawString("⚱️ PHARAOH'S LEAGUE", 50, 100);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (SandParticle s : sand) {
            s.update();
        }
        repaint();
    }

    class SandParticle {
        double x, y;

        SandParticle() {
            x = Math.random() * 1200;
            y = Math.random() * 800;
        }

        void update() {
            y += 1;
            if (y > 800) {
                y = 0;
                x = Math.random() * 1200;
            }
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(210, 180, 100));
            g2d.fillOval((int)x, (int)y, 2, 2);
        }
    }
}