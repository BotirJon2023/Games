import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class KremlinTennis extends JPanel implements ActionListener, KeyListener {

    private static final int WIDTH = 1020;
    private static final int HEIGHT = 670;

    private final Rectangle2D court = new Rectangle2D.Double(110, 95, 800, 480);

    private Player p1, p2;
    private Ball ball;

    private int p1Score = 0, p2Score = 0;
    private int gameMode = 1; // 1 = vs Computer, 2 = 2 Players

    private Timer timer;
    private Random rand = new Random();

    private boolean w, s, a, d, up, down, left, right, space, enter;
    private String status = "Press SPACE to Start - Kremlin Cup";

    public KremlinTennis() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(25, 25, 45)); // Dark Moscow night background
        setFocusable(true);
        addKeyListener(this);

        p1 = new Player(260, HEIGHT/2, new Color(220, 30, 30), true);   // Red
        p2 = new Player(760, HEIGHT/2, new Color(0, 120, 200), false);  // Blue

        ball = new Ball(WIDTH/2, HEIGHT/2);

        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawKremlinBackground(g2);
        drawClayCourt(g2);
        drawKremlinElements(g2);
        drawPlayers(g2);
        drawBall(g2);
        drawNet(g2);

        // HUD
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 44));
        g2.drawString(p1Score + "   —   " + p2Score, WIDTH/2 - 80, 62);

        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.setColor(new Color(255, 215, 0));
        g2.drawString("PLAYER 1", 165, 50);
        g2.drawString("PLAYER 2", WIDTH - 305, 50);

        if (gameMode == 1) g2.drawString("VS COMPUTER", WIDTH/2 - 100, HEIGHT - 30);

        g2.setFont(new Font("Arial", Font.BOLD, 23));
        g2.setColor(new Color(255, 215, 0));
        g2.drawString(status, WIDTH/2 - 175, 88);
    }

    private void drawKremlinBackground(Graphics2D g2) {
        // Kremlin Wall
        g2.setColor(new Color(180, 40, 40));
        g2.fillRect(0, 80, WIDTH, 40);

        // Kremlin Towers (simple silhouette)
        g2.setColor(new Color(160, 30, 30));
        int[] towerX = {120, 280, 520, 720, 880};
        for (int x : towerX) {
            g2.fillRect(x, 45, 28, 45);
            g2.fillPolygon(new int[]{x-8, x+14, x+36}, new int[]{45, 25, 45}, 3);
        }
    }

    private void drawClayCourt(Graphics2D g2) {
        // Red Clay Court (typical for Russian tournaments)
        g2.setColor(new Color(200, 70, 40));
        g2.fill(court);

        // White Lines
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(6));
        g2.draw(court);

        // Center line
        g2.drawLine(WIDTH/2, 95, WIDTH/2, 575);

        // Service lines
        g2.setStroke(new BasicStroke(4));
        g2.drawLine(290, 200, 730, 200);
        g2.drawLine(290, 470, 730, 470);
    }

    private void drawKremlinElements(Graphics2D g2) {
        g2.setFont(new Font("Serif", Font.BOLD, 32));
        g2.setColor(new Color(255, 215, 0));
        g2.drawString("KREMLIN CUP", WIDTH/2 - 115, 38);

        // Russian Flag accents
        g2.setColor(new Color(220, 20, 20));
        g2.fillRect(380, 15, 40, 25);
        g2.setColor(Color.WHITE);
        g2.fillRect(420, 15, 40, 25);
        g2.setColor(new Color(0, 80, 180));
        g2.fillRect(460, 15, 40, 25);
    }

    private void drawNet(Graphics2D g2) {
        g2.setColor(new Color(240, 240, 240));
        g2.setStroke(new BasicStroke(8));
        g2.drawLine(WIDTH/2, 120, WIDTH/2, 550);

        g2.setStroke(new BasicStroke(2));
        for (int y = 120; y < 550; y += 16) {
            g2.drawLine(WIDTH/2 - 15, y, WIDTH/2 + 15, y);
        }
    }

    private void drawPlayers(Graphics2D g2) {
        drawRealisticPlayer(g2, p1);
        drawRealisticPlayer(g2, p2);
    }

    private void drawRealisticPlayer(Graphics2D g2, Player p) {
        int x = (int) p.x;
        int y = (int) p.y;

        // Shadow
        g2.setColor(new Color(0, 0, 0, 110));
        g2.fillOval(x - 23, y + 44, 46, 16);

        // Shoes
        g2.setColor(Color.BLACK);
        g2.fillRect(x - 15, y + 35, 13, 9);
        g2.fillRect(x + 3, y + 35, 13, 9);

        // Socks
        g2.setColor(Color.WHITE);
        g2.fillRect(x - 13, y + 28, 9, 9);
        g2.fillRect(x + 4, y + 28, 9, 9);

        // Legs
        g2.setColor(new Color(30, 30, 35));
        g2.fillRect(x - 12, y + 10, 10, 28);
        g2.fillRect(x + 2, y + 10, 10, 28);

        // Shorts
        g2.setColor(p.color.darker());
        g2.fillRect(x - 16, y - 8, 32, 20);

        // Jersey
        g2.setColor(p.color);
        g2.fillRoundRect(x - 17, y - 32, 34, 35, 12, 12);

        // Arms
        g2.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x - 14, y - 20, x - 32, y + 2);     // Left arm
        g2.drawLine(x + 14, y - 20, x + 38, y - 12);    // Right arm (racket)

        // Head
        g2.setColor(new Color(255, 215, 180));
        g2.fillOval(x - 13, y - 48, 26, 26);

        // Hair
        g2.setColor(p == this.p1 ? new Color(40, 25, 15) : new Color(240, 220, 80));
        g2.fillOval(x - 14, y - 53, 28, 20);

        // Headband
        g2.setColor(p.color);
        g2.fillRect(x - 14, y - 51, 28, 8);

        // Racket
        g2.setColor(new Color(50, 50, 50));
        g2.setStroke(new BasicStroke(5));
        g2.drawLine(x + 38, y - 12, x + 62, y - 35);

        g2.setColor(new Color(230, 230, 255));
        g2.fillRoundRect(x + 55, y - 48, 26, 34, 8, 8);
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(x + 55, y - 48, 26, 34, 8, 8);
    }

    private void drawBall(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.fillOval((int)ball.x - 10, (int)ball.y - 10, 20, 20);
        g2.setColor(new Color(255, 80, 0));
        g2.fillOval((int)ball.x - 7, (int)ball.y - 7, 14, 14);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateMovement();
        if (gameMode == 1) updateAI();

        ball.update();
        checkCollisions();
        checkScoring();

        repaint();
    }

    private void updateMovement() {
        if (w) p1.y -= 7;
        if (s) p1.y += 7;
        if (a) p1.x -= 7;
        if (d) p1.x += 7;

        if (gameMode == 2) {
            if (up) p2.y -= 7;
            if (down) p2.y += 7;
            if (left) p2.x -= 7;
            if (right) p2.x += 7;
        }

        p1.x = Math.max(150, Math.min(490, p1.x));
        p1.y = Math.max(150, Math.min(HEIGHT - 140, p1.y));

        p2.x = Math.max(530, Math.min(870, p2.x));
        p2.y = Math.max(150, Math.min(HEIGHT - 140, p2.y));
    }

    private void updateAI() {
        if (ball.vx > 0) {
            p2.x += (ball.x - p2.x) * 0.135;
            p2.y += (ball.y - p2.y) * 0.185;
        } else {
            p2.x += (760 - p2.x) * 0.1;
            p2.y += (HEIGHT/2 - p2.y) * 0.1;
        }
    }

    private void checkCollisions() {
        if (p1.distanceTo(ball) < 43 && ball.vx < 0 && space) {
            ball.vx = 8.5 + rand.nextDouble() * 4.5;
            ball.vy = (ball.y - p1.y) * 0.24;
        }

        if (p2.distanceTo(ball) < 43 && ball.vx > 0) {
            boolean hit = (gameMode == 2) ? enter : true;
            if (hit) {
                ball.vx = -8.5 - rand.nextDouble() * 4.5;
                ball.vy = (ball.y - p2.y) * 0.24;
            }
        }

        if (ball.y < 130 || ball.y > HEIGHT - 130) {
            ball.vy = -ball.vy * 0.94;
        }
    }

    private void checkScoring() {
        if (ball.x < 75) {
            p2Score++;
            resetPoint();
        } else if (ball.x > WIDTH - 75) {
            p1Score++;
            resetPoint();
        }

        if (p1Score >= 6 || p2Score >= 6) {
            status = (p1Score > p2Score) ? "🏆 PLAYER 1 WINS THE KREMLIN CUP!" : "🏆 PLAYER 2 WINS THE KREMLIN CUP!";
            timer.stop();
        }
    }

    private void resetPoint() {
        ball.reset();
        p1.x = 260; p1.y = HEIGHT/2;
        p2.x = 760; p2.y = HEIGHT/2;
        status = "Press SPACE to Serve";
    }

    // Key Controls
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> w = true;
            case KeyEvent.VK_S -> s = true;
            case KeyEvent.VK_A -> a = true;
            case KeyEvent.VK_D -> d = true;
            case KeyEvent.VK_SPACE -> { space = true; if (p1Score < 6 && p2Score < 6) timer.start(); }
            case KeyEvent.VK_UP -> up = true;
            case KeyEvent.VK_DOWN -> down = true;
            case KeyEvent.VK_LEFT -> left = true;
            case KeyEvent.VK_RIGHT -> right = true;
            case KeyEvent.VK_ENTER -> enter = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> w = false;
            case KeyEvent.VK_S -> s = false;
            case KeyEvent.VK_A -> a = false;
            case KeyEvent.VK_D -> d = false;
            case KeyEvent.VK_SPACE -> space = false;
            case KeyEvent.VK_UP -> up = false;
            case KeyEvent.VK_DOWN -> down = false;
            case KeyEvent.VK_LEFT -> left = false;
            case KeyEvent.VK_RIGHT -> right = false;
            case KeyEvent.VK_ENTER -> enter = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner Classes
    class Player {
        double x, y;
        Color color;
        Player(double x, double y, Color color, boolean left) {
            this.x = x; this.y = y; this.color = color;
        }
        double distanceTo(Ball b) {
            return Math.hypot(x - b.x, y - b.y);
        }
    }

    class Ball {
        double x, y, vx = 7.2, vy = 2.8;
        Ball(double x, double y) { this.x = x; this.y = y; }

        void update() {
            x += vx;
            y += vy;
            vx *= 0.992;
        }

        void reset() {
            x = WIDTH/2; y = HEIGHT/2;
            vx = rand.nextBoolean() ? 7.5 : -7.5;
            vy = rand.nextDouble() * 5.5 - 2.7;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("🕌 Kremlin Cup - Moscow Tennis");
        KremlinTennis game = new KremlinTennis();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        JOptionPane.showMessageDialog(frame,
                "🎾 Kremlin Cup Tennis Simulator\n\n" +
                        "Player 1 (Red): W A S D + SPACE\n" +
                        "Player 2 (Blue): Arrow Keys + ENTER\n\n" +
                        "First to 6 points wins the Cup!",
                "Kremlin Tennis", JOptionPane.INFORMATION_MESSAGE);
    }
}