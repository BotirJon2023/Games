import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class OlympicTennis extends JPanel implements ActionListener, KeyListener {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 650;

    // Court (Hard Court - Olympic Style)
    private final Rectangle2D court = new Rectangle2D.Double(100, 80, 800, 480);

    private Player p1, p2;
    private Ball ball;

    private int p1Score = 0, p2Score = 0;
    private int gameMode = 1; // 1 = vs Computer, 2 = 2 Players

    private Timer timer;
    private Random rand = new Random();

    private boolean w, s, a, d, up, down, left, right, space, enter;
    private String status = "Press SPACE to Start Match";

    public OlympicTennis() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(0, 40, 80)); // Deep Olympic blue background
        setFocusable(true);
        addKeyListener(this);

        p1 = new Player(250, HEIGHT/2, new Color(0, 180, 255), true);   // Blue
        p2 = new Player(750, HEIGHT/2, new Color(255, 50, 50), false);  // Red

        ball = new Ball(WIDTH/2, HEIGHT/2);

        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawOlympicCourt(g2);
        drawOlympicRings(g2);
        drawPlayers(g2);
        drawBall(g2);
        drawNet(g2);

        // Score & Title
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 42));
        g2.drawString(p1Score + "   -   " + p2Score, WIDTH/2 - 70, 55);

        g2.setFont(new Font("Arial", Font.BOLD, 26));
        g2.drawString("PLAYER 1", 160, 45);
        g2.drawString("PLAYER 2", WIDTH - 320, 45);

        if (gameMode == 1) {
            g2.drawString("VS COMPUTER", WIDTH/2 - 90, HEIGHT - 25);
        }

        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.setColor(new Color(255, 215, 0));
        g2.drawString(status, WIDTH/2 - 140, 85);
    }

    private void drawOlympicCourt(Graphics2D g2) {
        // Olympic Hard Court (Blue)
        g2.setColor(new Color(0, 120, 200));
        g2.fill(court);

        // White Lines
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(5));
        g2.draw(court);

        // Center Line
        g2.drawLine((int)court.getCenterX(), (int)court.getY(),
                (int)court.getCenterX(), (int)(court.getY() + court.getHeight()));

        // Service Lines
        g2.drawLine(280, 180, 720, 180);
        g2.drawLine(280, 480, 720, 480);

        // Inner service boxes
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(280, 180, 220, 150);
        g2.drawRect(500, 180, 220, 150);
        g2.drawRect(280, 330, 220, 150);
        g2.drawRect(500, 330, 220, 150);
    }

    private void drawOlympicRings(Graphics2D g2) {
        g2.setStroke(new BasicStroke(8));
        int r = 22;

        // Olympic Rings (Blue, Yellow, Black, Green, Red)
        g2.setColor(new Color(0, 120, 200));
        g2.drawOval(420, 15, r*2, r*2);
        g2.setColor(new Color(255, 200, 0));
        g2.drawOval(455, 15, r*2, r*2);
        g2.setColor(Color.BLACK);
        g2.drawOval(490, 15, r*2, r*2);
        g2.setColor(new Color(0, 160, 80));
        g2.drawOval(525, 15, r*2, r*2);
        g2.setColor(new Color(220, 40, 40));
        g2.drawOval(560, 15, r*2, r*2);
    }

    private void drawNet(Graphics2D g2) {
        g2.setColor(new Color(240, 240, 240));
        g2.setStroke(new BasicStroke(7));
        g2.drawLine(WIDTH/2, 105, WIDTH/2, 535);

        g2.setStroke(new BasicStroke(2));
        for (int y = 105; y < 535; y += 14) {
            g2.drawLine(WIDTH/2 - 12, y, WIDTH/2 + 12, y);
        }
    }

    private void drawPlayers(Graphics2D g2) {
        drawPlayer(g2, p1);
        drawPlayer(g2, p2);
    }

    private void drawPlayer(Graphics2D g2, Player p) {
        // Shadow
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillOval((int)p.x - 19, (int)p.y + 38, 38, 14);

        // Torso (vertical stance)
        g2.setColor(p.color);
        g2.fillRoundRect((int)p.x - 13, (int)p.y - 28, 26, 48, 12, 12);

        // Head
        g2.setColor(new Color(255, 220, 180));
        g2.fillOval((int)p.x - 11, (int)p.y - 42, 22, 22);

        // Headband (Olympic style)
        g2.setColor(p.color);
        g2.fillRect((int)p.x - 12, (int)p.y - 43, 24, 6);

        // Arms
        g2.setStroke(new BasicStroke(9));
        g2.drawLine((int)p.x - 13, (int)p.y - 12, (int)p.x - 28, (int)p.y + 8);   // left arm
        g2.drawLine((int)p.x + 13, (int)p.y - 12, (int)p.x + 32, (int)p.y - 2);    // right arm (racket)

        // Racket
        g2.setColor(Color.DARK_GRAY);
        g2.drawLine((int)p.x + 32, (int)p.y - 2, (int)p.x + 52, (int)p.y - 18);
        g2.setColor(new Color(255, 230, 100));
        g2.fillOval((int)p.x + 47, (int)p.y - 32, 22, 32);
        g2.setColor(Color.BLACK);
        g2.drawOval((int)p.x + 47, (int)p.y - 32, 22, 32);
    }

    private void drawBall(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.fillOval((int)ball.x - 9, (int)ball.y - 9, 18, 18);
        g2.setColor(new Color(255, 100, 0));
        g2.fillOval((int)ball.x - 7, (int)ball.y - 7, 14, 14);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updatePlayerMovement();
        if (gameMode == 1) updateAI();

        ball.update();
        checkCollisions();
        checkScoring();

        repaint();
    }

    private void updatePlayerMovement() {
        if (w) p1.y -= 6.5;
        if (s) p1.y += 6.5;
        if (a) p1.x -= 6.5;
        if (d) p1.x += 6.5;

        if (gameMode == 2) {
            if (up) p2.y -= 6.5;
            if (down) p2.y += 6.5;
            if (left) p2.x -= 6.5;
            if (right) p2.x += 6.5;
        }

        // Bounds
        p1.x = Math.max(130, Math.min(470, p1.x));
        p1.y = Math.max(130, Math.min(HEIGHT - 130, p1.y));

        p2.x = Math.max(530, Math.min(870, p2.x));
        p2.y = Math.max(130, Math.min(HEIGHT - 130, p2.y));
    }

    private void updateAI() {
        if (ball.vx > 0) { // Ball moving toward AI
            p2.x += (ball.x - p2.x) * 0.13;
            p2.y += (ball.y - p2.y) * 0.17;
        } else {
            p2.x += (760 - p2.x) * 0.09;
            p2.y += (HEIGHT/2 - p2.y) * 0.09;
        }
    }

    private void checkCollisions() {
        // P1 Hit
        if (p1.distanceTo(ball) < 38 && ball.vx < 0 && space) {
            ball.vx = 7.5 + rand.nextDouble() * 4;
            ball.vy = (ball.y - p1.y) * 0.22;
        }

        // P2 Hit
        if (p2.distanceTo(ball) < 38 && ball.vx > 0) {
            boolean hit = (gameMode == 2) ? enter : rand.nextBoolean();
            if (hit) {
                ball.vx = -7.5 - rand.nextDouble() * 4;
                ball.vy = (ball.y - p2.y) * 0.22;
            }
        }

        // Top/Bottom bounce
        if (ball.y < 115 || ball.y > HEIGHT - 115) {
            ball.vy = -ball.vy * 0.96;
        }
    }

    private void checkScoring() {
        if (ball.x < 70) {
            p2Score++;
            resetPoint();
        } else if (ball.x > WIDTH - 70) {
            p1Score++;
            resetPoint();
        }

        if (p1Score >= 6 || p2Score >= 6) {
            status = (p1Score > p2Score) ? "🏅 PLAYER 1 WINS GOLD!" : "🏅 PLAYER 2 WINS GOLD!";
            timer.stop();
        }
    }

    private void resetPoint() {
        ball.reset();
        p1.x = 250; p1.y = HEIGHT/2;
        p2.x = 750; p2.y = HEIGHT/2;
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
            case KeyEvent.VK_SPACE -> { space = true; if (!timer.isRunning() && (p1Score < 6 && p2Score < 6)) timer.start(); }
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
        Player(double x, double y, Color c, boolean left) {
            this.x = x; this.y = y; this.color = c;
        }
        double distanceTo(Ball b) {
            return Math.hypot(x - b.x, y - b.y);
        }
    }

    class Ball {
        double x, y, vx = 6.5, vy = 2.5;
        Ball(double x, double y) { this.x = x; this.y = y; }

        void update() {
            x += vx;
            y += vy;
            vx *= 0.994; // air resistance
        }

        void reset() {
            x = WIDTH/2; y = HEIGHT/2;
            vx = rand.nextBoolean() ? 6.8 : -6.8;
            vy = rand.nextDouble() * 5 - 2.5;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("🏅 Olympic Games - Tennis Match");
        OlympicTennis game = new OlympicTennis();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        JOptionPane.showMessageDialog(frame,
                "🎾 Olympic Tennis Controls:\n\n" +
                        "Player 1 (Blue): W A S D + SPACE\n" +
                        "Player 2 (Red): Arrow Keys + ENTER\n\n" +
                        "First to 6 points wins Gold!\n",
                "Olympic Tennis Simulator", JOptionPane.INFORMATION_MESSAGE);
    }
}