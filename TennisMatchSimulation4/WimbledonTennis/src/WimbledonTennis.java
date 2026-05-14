import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class WimbledonTennis extends JPanel implements ActionListener, KeyListener {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 650;

    // Court
    private final Rectangle2D court = new Rectangle2D.Double(100, 80, 800, 480);

    // Players (vertical standing look)
    private Player p1, p2;
    private Ball ball;

    private int p1Score = 0, p2Score = 0;
    private int gameMode = 1; // 1 = vs AI, 2 = 2 Players

    private Timer timer;
    private Random rand = new Random();

    private boolean w, s, a, d, up, down, left, right, space, enter;

    // Simple scoring
    private String status = "Press SPACE to Start";

    public WimbledonTennis() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(0, 80, 0)); // Dark grass background
        setFocusable(true);
        addKeyListener(this);

        p1 = new Player(250, HEIGHT/2, new Color(0, 120, 255), true);   // Left - Blue
        p2 = new Player(750, HEIGHT/2, new Color(255, 60, 60), false);  // Right - Red

        ball = new Ball(WIDTH/2, HEIGHT/2);

        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawWimbledonCourt(g2);
        drawPlayers(g2);
        drawBall(g2);
        drawNet(g2);

        // HUD
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 36));
        g2.drawString(p1Score + "  -  " + p2Score, WIDTH/2 - 50, 50);

        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.drawString("PLAYER 1 (Blue)", 180, 45);
        g2.drawString("PLAYER 2 (Red)", WIDTH - 380, 45);

        if (gameMode == 1) g2.drawString("VS COMPUTER", WIDTH/2 - 80, HEIGHT - 20);

        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString(status, WIDTH/2 - 120, 80);
    }

    private void drawWimbledonCourt(Graphics2D g2) {
        // Grass
        g2.setColor(new Color(0, 160, 0));
        g2.fill(court);

        // Lines
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(4));
        // Outer lines
        g2.draw(court);
        // Center line
        g2.drawLine((int)court.getCenterX(), (int)court.getY(),
                (int)court.getCenterX(), (int)(court.getY() + court.getHeight()));
        // Service lines
        g2.drawLine(300, 180, 700, 180);
        g2.drawLine(300, 480, 700, 480);
    }

    private void drawNet(Graphics2D g2) {
        g2.setColor(new Color(200, 200, 200));
        g2.setStroke(new BasicStroke(6));
        g2.drawLine(WIDTH/2, 100, WIDTH/2, 520);

        // Net mesh
        g2.setStroke(new BasicStroke(2));
        for (int y = 100; y < 520; y += 12) {
            g2.drawLine(WIDTH/2 - 8, y, WIDTH/2 + 8, y);
        }
    }

    private void drawPlayers(Graphics2D g2) {
        drawPlayer(g2, p1);
        drawPlayer(g2, p2);
    }

    private void drawPlayer(Graphics2D g2, Player p) {
        // Shadow
        g2.setColor(new Color(0,0,0,80));
        g2.fillOval((int)p.x - 18, (int)p.y + 35, 36, 12);

        // Body (vertical stance)
        g2.setColor(p.color);
        g2.fillRect((int)p.x - 12, (int)p.y - 25, 24, 45); // torso

        // Head
        g2.fillOval((int)p.x - 10, (int)p.y - 38, 20, 20);

        // Arms
        g2.setStroke(new BasicStroke(8));
        g2.drawLine((int)p.x - 12, (int)p.y - 10, (int)p.x - 25, (int)p.y + 5);  // left arm
        g2.drawLine((int)p.x + 12, (int)p.y - 10, (int)p.x + 28, (int)p.y + 8);   // right arm (racket side)

        // Racket
        g2.setColor(Color.DARK_GRAY);
        g2.drawLine((int)p.x + 28, (int)p.y + 8, (int)p.x + 45, (int)p.y - 5);
        g2.setColor(new Color(255, 220, 100));
        g2.fillOval((int)p.x + 40, (int)p.y - 18, 18, 28);
    }

    private void drawBall(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.fillOval((int)ball.x - 8, (int)ball.y - 8, 16, 16);
        g2.setColor(Color.ORANGE);
        g2.fillOval((int)ball.x - 6, (int)ball.y - 6, 12, 12);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!timer.isRunning()) return;

        updatePlayerMovement();
        if (gameMode == 1) updateAI();

        ball.update();
        checkCollisions();
        checkScoring();

        repaint();
    }

    private void updatePlayerMovement() {
        if (w) p1.y -= 6;
        if (s) p1.y += 6;
        if (a) p1.x -= 6;
        if (d) p1.x += 6;

        if (gameMode == 2) {
            if (up) p2.y -= 6;
            if (down) p2.y += 6;
            if (left) p2.x -= 6;
            if (right) p2.x += 6;
        }

        // Keep players in bounds
        p1.x = Math.max(120, Math.min(480, p1.x));
        p1.y = Math.max(120, Math.min(HEIGHT-120, p1.y));

        p2.x = Math.max(520, Math.min(880, p2.x));
        p2.y = Math.max(120, Math.min(HEIGHT-120, p2.y));
    }

    private void updateAI() {
        // Simple but effective AI
        double targetY = ball.y;
        if (ball.vx > 0) { // Ball coming towards AI
            p2.x += (ball.x - p2.x) * 0.12;
            p2.y += (targetY - p2.y) * 0.18;
        } else {
            p2.x += (750 - p2.x) * 0.1; // Return to home
            p2.y += (HEIGHT/2 - p2.y) * 0.1;
        }
    }

    private void checkCollisions() {
        // Player 1 hit
        if (p1.distanceTo(ball) < 35 && ball.vx < 0 && space) {
            ball.vx = 8 + rand.nextDouble() * 4;
            ball.vy = (ball.y - p1.y) * 0.2;
        }

        // Player 2 hit
        if (p2.distanceTo(ball) < 35 && ball.vx > 0) {
            boolean hit = (gameMode == 2) ? enter : true;
            if (hit) {
                ball.vx = -8 - rand.nextDouble() * 4;
                ball.vy = (ball.y - p2.y) * 0.2;
            }
        }

        // Wall bounce
        if (ball.y < 110 || ball.y > HEIGHT - 110) {
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
            status = (p1Score > p2Score ? "PLAYER 1 WINS!" : "PLAYER 2 WINS!");
            timer.stop();
        }
    }

    private void resetPoint() {
        ball.reset();
        p1.x = 250; p1.y = HEIGHT/2;
        p2.x = 750; p2.y = HEIGHT/2;
        status = "Press SPACE to serve";
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> w = true;
            case KeyEvent.VK_S -> s = true;
            case KeyEvent.VK_A -> a = true;
            case KeyEvent.VK_D -> d = true;
            case KeyEvent.VK_SPACE -> { space = true; if (!timer.isRunning()) timer.start(); }
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

    // Inner classes
    class Player {
        double x, y;
        Color color;
        Player(double x, double y, Color c, boolean isLeft) {
            this.x = x; this.y = y; this.color = c;
        }
        double distanceTo(Ball b) {
            return Math.hypot(x - b.x, y - b.y);
        }
    }

    class Ball {
        double x, y, vx = 6, vy = 2;
        Ball(double x, double y) { this.x = x; this.y = y; }

        void update() {
            x += vx;
            y += vy;
            vx *= 0.995; // slight friction
        }

        void reset() {
            x = WIDTH/2; y = HEIGHT/2;
            vx = rand.nextBoolean() ? 6 : -6;
            vy = rand.nextDouble() * 4 - 2;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("🏆 Wimbledon Tennis Simulator");
        WimbledonTennis game = new WimbledonTennis();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        JOptionPane.showMessageDialog(frame,
                "Controls:\n" +
                        "Player 1 (Blue): W A S D + SPACE\n" +
                        "Player 2 (Red): Arrow Keys + ENTER\n\n" +
                        "First to 6 points wins!",
                "Wimbledon Tennis", JOptionPane.INFORMATION_MESSAGE);
    }
}