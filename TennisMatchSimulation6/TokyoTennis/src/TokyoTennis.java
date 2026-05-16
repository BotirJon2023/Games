import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class TokyoTennis extends JPanel implements ActionListener, KeyListener {

    private static final int WIDTH = 1020;
    private static final int HEIGHT = 660;

    private final Rectangle2D court = new Rectangle2D.Double(110, 90, 800, 480);

    private Player p1, p2;
    private Ball ball;

    private int p1Score = 0, p2Score = 0;
    private int gameMode = 1; // 1 = vs Computer, 2 = 2 Players

    private Timer timer;
    private Random rand = new Random();

    private boolean w, s, a, d, up, down, left, right, space, enter;
    private String status = "Press SPACE to Start - Tokyo 2020";

    public TokyoTennis() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(0, 35, 75)); // Deep Tokyo night blue
        setFocusable(true);
        addKeyListener(this);

        p1 = new Player(260, HEIGHT/2, new Color(0, 140, 255), true);   // Blue Japan-inspired
        p2 = new Player(760, HEIGHT/2, new Color(255, 45, 45), false);  // Red

        ball = new Ball(WIDTH/2, HEIGHT/2);

        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawTokyoCourt(g2);
        drawTokyoElements(g2);
        drawPlayers(g2);
        drawBall(g2);
        drawNet(g2);

        // HUD
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 42));
        g2.drawString(p1Score + "   —   " + p2Score, WIDTH/2 - 75, 58);

        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.drawString("PLAYER 1", 170, 48);
        g2.drawString("PLAYER 2", WIDTH - 310, 48);

        if (gameMode == 1) g2.drawString("VS COMPUTER", WIDTH/2 - 95, HEIGHT - 28);

        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.setColor(new Color(255, 215, 0));
        g2.drawString(status, WIDTH/2 - 160, 82);
    }

    private void drawTokyoCourt(Graphics2D g2) {
        // Tokyo Blue Hard Court
        g2.setColor(new Color(0, 105, 180));
        g2.fill(court);

        // White Lines
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(6));
        g2.draw(court);
        g2.drawLine((int)court.getCenterX(), 90, (int)court.getCenterX(), 570);

        g2.setStroke(new BasicStroke(4));
        g2.drawLine(300, 190, 720, 190);
        g2.drawLine(300, 470, 720, 470);
    }

    private void drawTokyoElements(Graphics2D g2) {
        // Rising Sun motif
        g2.setColor(new Color(255, 60, 60));
        g2.fillOval(480, 18, 60, 60);
        g2.setColor(Color.WHITE);
        g2.fillOval(495, 33, 30, 30);

        g2.setFont(new Font("Arial", Font.BOLD, 28));
        g2.setColor(new Color(255, 215, 0));
        g2.drawString("TOKYO 2020", WIDTH/2 - 95, 38);
    }

    private void drawNet(Graphics2D g2) {
        g2.setColor(new Color(230, 230, 230));
        g2.setStroke(new BasicStroke(8));
        g2.drawLine(WIDTH/2, 115, WIDTH/2, 545);

        g2.setStroke(new BasicStroke(2));
        for (int y = 115; y < 545; y += 15) {
            g2.drawLine(WIDTH/2 - 14, y, WIDTH/2 + 14, y);
        }
    }

    private void drawPlayers(Graphics2D g2) {
        drawRealisticPlayer(g2, p1);
        drawRealisticPlayer(g2, p2);
    }

    private void drawRealisticPlayer(Graphics2D g2, Player p) {
        int x = (int)p.x;
        int y = (int)p.y;

        // Shadow
        g2.setColor(new Color(0,0,0,100));
        g2.fillOval(x - 22, y + 42, 44, 16);

        // Shoes
        g2.setColor(Color.WHITE);
        g2.fillRect(x - 14, y + 32, 11, 8);
        g2.fillRect(x + 3, y + 32, 11, 8);

        // Legs
        g2.setColor(new Color(40, 40, 40));
        g2.fillRect(x - 11, y + 8, 9, 28);
        g2.fillRect(x + 2, y + 8, 9, 28);

        // Shorts
        g2.setColor(p.color.darker());
        g2.fillRect(x - 14, y - 8, 28, 18);

        // Torso / Jersey
        g2.setColor(p.color);
        g2.fillRoundRect(x - 15, y - 30, 30, 32, 10, 10);

        // Arms
        g2.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // Left arm
        g2.drawLine(x - 13, y - 18, x - 28, y + 5);
        // Right arm (racket side)
        g2.drawLine(x + 13, y - 18, x + 35, y - 8);

        // Head
        g2.setColor(new Color(255, 210, 170));
        g2.fillOval(x - 12, y - 48, 24, 24);

        // Hair
        g2.setColor(p == this.p1 ? new Color(30, 30, 30) : new Color(200, 180, 60));
        g2.fillOval(x - 13, y - 52, 26, 18);

        // Headband
        g2.setColor(p.color);
        g2.fillRect(x - 13, y - 50, 26, 7);

        // Racket
        g2.setColor(Color.DARK_GRAY);
        g2.setStroke(new BasicStroke(4));
        g2.drawLine(x + 35, y - 8, x + 58, y - 28);

        g2.setColor(new Color(240, 240, 255));
        g2.fillOval(x + 52, y - 42, 26, 32);
        g2.setColor(Color.BLACK);
        g2.drawOval(x + 52, y - 42, 26, 32);
    }

    private void drawBall(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.fillOval((int)ball.x - 10, (int)ball.y - 10, 20, 20);
        g2.setColor(new Color(255, 90, 0));
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

        // Court bounds
        p1.x = Math.max(150, Math.min(480, p1.x));
        p1.y = Math.max(140, Math.min(HEIGHT - 130, p1.y));

        p2.x = Math.max(540, Math.min(870, p2.x));
        p2.y = Math.max(140, Math.min(HEIGHT - 130, p2.y));
    }

    private void updateAI() {
        if (ball.vx > 0) {
            p2.x += (ball.x - p2.x) * 0.14;
            p2.y += (ball.y - p2.y) * 0.19;
        } else {
            p2.x += (760 - p2.x) * 0.1;
            p2.y += (HEIGHT/2 - p2.y) * 0.1;
        }
    }

    private void checkCollisions() {
        // Player 1 hit
        if (p1.distanceTo(ball) < 42 && ball.vx < 0 && space) {
            ball.vx = 8 + rand.nextDouble() * 5;
            ball.vy = (ball.y - p1.y) * 0.25;
        }

        // Player 2 hit
        if (p2.distanceTo(ball) < 42 && ball.vx > 0) {
            boolean canHit = (gameMode == 2) ? enter : true;
            if (canHit) {
                ball.vx = -8 - rand.nextDouble() * 5;
                ball.vy = (ball.y - p2.y) * 0.25;
            }
        }

        // Top / Bottom bounce
        if (ball.y < 125 || ball.y > HEIGHT - 125) {
            ball.vy = -ball.vy * 0.95;
        }
    }

    private void checkScoring() {
        if (ball.x < 80) {
            p2Score++;
            resetPoint();
        } else if (ball.x > WIDTH - 80) {
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
        p1.x = 260; p1.y = HEIGHT/2;
        p2.x = 760; p2.y = HEIGHT/2;
        status = "Press SPACE to Serve";
    }

    // ====================== Key Controls ======================
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

    // ====================== Inner Classes ======================
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
        double x, y, vx = 7, vy = 3;
        Ball(double x, double y) { this.x = x; this.y = y; }

        void update() {
            x += vx;
            y += vy;
            vx *= 0.993;
        }

        void reset() {
            x = WIDTH/2; y = HEIGHT/2;
            vx = rand.nextBoolean() ? 7.2 : -7.2;
            vy = rand.nextDouble() * 6 - 3;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("🏅 Tokyo 2020 Olympic Tennis");
        TokyoTennis game = new TokyoTennis();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        JOptionPane.showMessageDialog(frame,
                "🎾 Tokyo 2020 Tennis Simulator\n\n" +
                        "Player 1 (Blue): W A S D + SPACE\n" +
                        "Player 2 (Red): Arrow Keys + ENTER\n\n" +
                        "First to 6 points wins the Gold Medal!",
                "Tokyo Olympic Tennis", JOptionPane.INFORMATION_MESSAGE);
    }
}