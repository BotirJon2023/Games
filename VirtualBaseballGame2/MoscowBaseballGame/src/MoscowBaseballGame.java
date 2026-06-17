// MoscowBaseballGame.java
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.*;
import java.util.*;

public class MoscowBaseballGame extends JFrame {
    public MoscowBaseballGame() {
        setTitle("⚾ MOSCOW - Red Square Baseball 🇷🇺");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        MoscowGamePanel panel = new MoscowGamePanel();
        add(panel);

        setVisible(true);
        panel.startGame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MoscowBaseballGame());
    }
}

class MoscowGamePanel extends JPanel implements ActionListener {
    private Timer timer;
    private double ballX = 600, ballY = 400;
    private double batAngle = 0;
    private boolean swinging = false;
    private int score1 = 0, score2 = 0;
    private int strikes = 0, balls = 0, outs = 0;
    private int inning = 1;
    private boolean topInning = true;
    private String message = "";
    private ArrayList<Snowflake> snowflakes = new ArrayList<>();

    public MoscowGamePanel() {
        setPreferredSize(new Dimension(1200, 800));
        setBackground(new Color(50, 70, 100));
        timer = new Timer(16, this);
        timer.start();

        // Create snow effect
        for (int i = 0; i < 100; i++) {
            snowflakes.add(new Snowflake());
        }
    }

    void startGame() {
        message = "Welcome to MOSCOW! ⚾";
        Timer msgTimer = new Timer(2000, e -> message = "");
        msgTimer.setRepeats(false);
        msgTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Sky gradient
        GradientPaint sky = new GradientPaint(0, 0, new Color(70, 100, 150), 0, getHeight(), new Color(200, 220, 240));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // St. Basil's Cathedral
        drawStBasils(g2d);

        // Kremlin Wall
        drawKremlinWall(g2d);

        // Red Square ground
        g2d.setColor(new Color(180, 50, 50));
        g2d.fillRect(0, getHeight() - 200, getWidth(), 200);

        // Cobblestone pattern
        g2d.setColor(new Color(150, 40, 40));
        for (int i = 0; i < 50; i++) {
            g2d.fillRect(i * 30, getHeight() - 190, 20, 15);
        }

        // Baseball field overlay
        drawBaseballField(g2d);

        // Draw snowflakes
        for (Snowflake s : snowflakes) {
            s.draw(g2d);
        }

        drawBall(g2d);
        drawBat(g2d);
        drawUI(g2d);

        if (!message.isEmpty()) {
            drawMessage(g2d);
        }
    }

    private void drawStBasils(Graphics2D g2d) {
        // Onion domes
        int[] domeColors = {0xFF4500, 0x228B22, 0x4169E1, 0xFFD700, 0xFF69B4};
        for (int i = 0; i < 5; i++) {
            g2d.setColor(new Color(domeColors[i]));
            g2d.fillArc(150 + i * 60, getHeight() - 350, 40, 60, 0, 180);
            g2d.fillRect(160 + i * 60, getHeight() - 290, 20, 90);
        }

        // Main building
        g2d.setColor(new Color(200, 100, 100));
        g2d.fillRect(180, getHeight() - 300, 200, 100);
    }

    private void drawKremlinWall(Graphics2D g2d) {
        g2d.setColor(new Color(180, 50, 50));
        for (int i = 0; i < 20; i++) {
            g2d.fillRect(50 + i * 40, getHeight() - 250, 30, 50);
        }
    }

    private void drawBaseballField(Graphics2D g2d) {
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillOval(getWidth()/2 - 250, getHeight() - 400, 500, 300);
        g2d.setColor(new Color(210, 150, 75));
        g2d.fillOval(getWidth()/2 - 150, getHeight() - 350, 300, 200);

        // Home plate
        int[] x = {getWidth()/2 - 15, getWidth()/2 + 15, getWidth()/2 + 10, getWidth()/2, getWidth()/2 - 10};
        int[] y = {getHeight() - 250, getHeight() - 250, getHeight() - 265, getHeight() - 280, getHeight() - 265};
        g2d.setColor(Color.WHITE);
        g2d.fillPolygon(x, y, 5);
    }

    private void drawBall(Graphics2D g2d) {
        RadialGradientPaint ballPaint = new RadialGradientPaint(
                (float)ballX, (float)ballY, 12,
                new float[]{0f, 1f},
                new Color[]{Color.WHITE, new Color(200, 200, 200)}
        );
        g2d.setPaint(ballPaint);
        g2d.fillOval((int)ballX - 10, (int)ballY - 10, 20, 20);
        g2d.setColor(Color.RED);
        g2d.drawArc((int)ballX - 7, (int)ballY - 5, 14, 10, 0, 180);
    }

    private void drawBat(Graphics2D g2d) {
        if (swinging) {
            g2d.rotate(Math.toRadians(batAngle), getWidth()/2, getHeight() - 230);
        }
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRoundRect(getWidth()/2 - 8, getHeight() - 250, 16, 80, 8, 8);
        if (swinging) {
            g2d.rotate(-Math.toRadians(batAngle), getWidth()/2, getHeight() - 230);
        }
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setFont(new Font("Cyrillic", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        g2d.drawString("MOSCOW: " + score1 + " - " + score2, 50, 50);
        g2d.drawString("Strikes: " + strikes + "  Balls: " + balls + "  Outs: " + outs, 50, 90);
        g2d.drawString("Inning: " + inning + (topInning ? " Top" : " Bottom"), 50, 130);
    }

    private void drawMessage(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.setColor(new Color(255, 215, 0));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(message)) / 2;
        g2d.drawString(message, x, getHeight()/2);
    }


    @Override
    public void actionPerformed(ActionEvent e) {

    }

    class Snowflake {
        double x, y;
        double speed;

        Snowflake() {
            x = Math.random() * 1200;
            y = Math.random() * 800;
            speed = Math.random() * 3 + 1;
        }

        void update() {
            y += speed;
            if (y > 800) {
                y = 0;
                x = Math.random() * 1200;
            }
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int)x, (int)y, 3, 3);
        }
    }
}