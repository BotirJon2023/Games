import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TableTennisGame3D extends JPanel implements ActionListener, KeyListener {

    /* ============================================================
       WINDOW + LOOP
       ============================================================ */

    private Timer timer;
    private final int WIDTH = 1000;
    private final int HEIGHT = 700;

    /* ============================================================
       CAMERA / 3D SETTINGS
       ============================================================ */

    private double cameraZ = -600;
    private double cameraTilt = 0.0;
    private double perspective = 600;

    /* ============================================================
       GAME OBJECTS
       ============================================================ */

    private Paddle player;
    private Paddle ai;
    private Ball ball;
    private Table table;
    private List<Particle> particles = new ArrayList<>();

    /* ============================================================
       INPUT
       ============================================================ */

    private boolean upPressed;
    private boolean downPressed;

    /* ============================================================
       GAME STATE
       ============================================================ */

    private int playerScore = 0;
    private int aiScore = 0;
    private boolean running = true;

    private Random random = new Random();

    /* ============================================================
       CONSTRUCTOR
       ============================================================ */

    public TableTennisGame3D() {

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(15, 20, 25));
        setFocusable(true);
        addKeyListener(this);

        initGame();

        timer = new Timer(16, this);
        timer.start();
    }

    private void initGame() {

        table = new Table();

        player = new Paddle(-300);
        ai = new Paddle(300);

        ball = new Ball();

        particles.clear();
    }

    /* ============================================================
       GAME LOOP
       ============================================================ */

    @Override
    public void actionPerformed(ActionEvent e) {

        if (running) {
            updateGame();
        }

        repaint();
    }

    private void updateGame() {

        updatePlayer();
        updateAI();
        updateBall();
        updateParticles();
    }

    /* ============================================================
       PLAYER CONTROL
       ============================================================ */

    private void updatePlayer() {

        if (upPressed) {
            player.y -= 6;
        }

        if (downPressed) {
            player.y += 6;
        }

        player.y = clamp(player.y, -180, 180);
    }

    /* ============================================================
       AI LOGIC
       ============================================================ */

    private void updateAI() {

        double target = ball.y;

        if (target > ai.y + 5) {
            ai.y += 4;
        } else if (target < ai.y - 5) {
            ai.y -= 4;
        }

        ai.y = clamp(ai.y, -180, 180);
    }

    /* ============================================================
       BALL PHYSICS
       ============================================================ */

    private void updateBall() {

        ball.x += ball.vx;
        ball.y += ball.vy;
        ball.z += ball.vz;

        // bounce top/bottom
        if (ball.y < -200 || ball.y > 200) {
            ball.vy *= -1;
            spawnParticles(ball.x, ball.y, ball.z);
        }

        // bounce table depth
        if (ball.z < -200 || ball.z > 200) {
            ball.vz *= -1;
        }

        // player collision
        if (ball.x < player.x + 15 && ball.x > player.x - 20) {

            if (Math.abs(ball.y - player.y) < 60) {
                ball.vx = Math.abs(ball.vx);
                ball.vy += (ball.y - player.y) * 0.02;
                spawnParticles(ball.x, ball.y, ball.z);
            }
        }

        // AI collision
        if (ball.x > ai.x - 15 && ball.x < ai.x + 20) {

            if (Math.abs(ball.y - ai.y) < 60) {
                ball.vx = -Math.abs(ball.vx);
                ball.vy += (ball.y - ai.y) * 0.02;
                spawnParticles(ball.x, ball.y, ball.z);
            }
        }

        // scoring
        if (ball.x < -450) {
            aiScore++;
            resetRound();
        }

        if (ball.x > 450) {
            playerScore++;
            resetRound();
        }
    }

    private void resetRound() {

        ball.reset();

        running = false;
    }

    /* ============================================================
       PARTICLES
       ============================================================ */

    private void spawnParticles(double x, double y, double z) {

        for (int i = 0; i < 10; i++) {
            particles.add(new Particle(x, y, z));
        }
    }

    private void updateParticles() {

        for (int i = particles.size() - 1; i >= 0; i--) {

            Particle p = particles.get(i);
            p.update();

            if (p.life <= 0) {
                particles.remove(i);
            }
        }
    }

    /* ============================================================
       RENDERING
       ============================================================ */

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        drawTable(g2);
        drawPaddle(g2, player, Color.GREEN);
        drawPaddle(g2, ai, Color.RED);
        drawBall(g2);
        drawParticles(g2);
        drawUI(g2);
    }

    /* ============================================================
       PROJECTION
       ============================================================ */

    private Point project(double x, double y, double z) {

        double scale = perspective / (perspective + z - cameraZ);

        int sx = (int) (WIDTH / 2 + x * scale);
        int sy = (int) (HEIGHT / 2 + (y + cameraTilt * z) * scale);

        return new Point(sx, sy);
    }

    /* ============================================================
       DRAW TABLE
       ============================================================ */

    private void drawTable(Graphics2D g2) {

        g2.setColor(new Color(30, 120, 70));

        drawQuad(g2,
                -400, -220, -200,
                400, -220, -200,
                400, 220, 200,
                -400, 220, 200);
    }

    private void drawQuad(Graphics2D g2,
                          double x1, double y1, double z1,
                          double x2, double y2, double z2,
                          double x3, double y3, double z3,
                          double x4, double y4, double z4) {

        Point p1 = project(x1, y1, z1);
        Point p2 = project(x2, y2, z2);
        Point p3 = project(x3, y3, z3);
        Point p4 = project(x4, y4, z4);

        Polygon poly = new Polygon();

        poly.addPoint(p1.x, p1.y);
        poly.addPoint(p2.x, p2.y);
        poly.addPoint(p3.x, p3.y);
        poly.addPoint(p4.x, p4.y);

        g2.fillPolygon(poly);
    }

    /* ============================================================
       DRAW PADDLES
       ============================================================ */

    private void drawPaddle(Graphics2D g2, Paddle p, Color color) {

        g2.setColor(color);

        Point top = project(p.x, p.y - 50, 0);
        Point bottom = project(p.x, p.y + 50, 0);

        int height = Math.abs(bottom.y - top.y);

        g2.fillRoundRect(top.x - 10, top.y, 20, height, 10, 10);
    }

    /* ============================================================
       DRAW BALL
       ============================================================ */

    private void drawBall(Graphics2D g2) {

        Point p = project(ball.x, ball.y, ball.z);

        double scale = perspective / (perspective + ball.z - cameraZ);

        int size = (int) (14 * scale);

        g2.setColor(Color.WHITE);
        g2.fillOval(p.x - size / 2, p.y - size / 2, size, size);
    }

    /* ============================================================
       DRAW PARTICLES
       ============================================================ */

    private void drawParticles(Graphics2D g2) {

        g2.setColor(Color.YELLOW);

        for (Particle p : particles) {

            Point pt = project(p.x, p.y, p.z);

            int size = (int) (4 * p.life);

            g2.fillOval(pt.x, pt.y, size, size);
        }
    }

    /* ============================================================
       UI
       ============================================================ */

    private void drawUI(Graphics2D g2) {

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 20));

        g2.drawString("Player: " + playerScore, 30, 30);
        g2.drawString("AI: " + aiScore, WIDTH - 120, 30);

        if (!running) {
            g2.drawString("Press SPACE to serve", WIDTH / 2 - 100, 50);
        }
    }

    /* ============================================================
       UTILS
       ============================================================ */

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /* ============================================================
       KEY INPUT
       ============================================================ */

    @Override
    public void keyPressed(KeyEvent e) {

        int k = e.getKeyCode();

        if (k == KeyEvent.VK_W) upPressed = true;
        if (k == KeyEvent.VK_S) downPressed = true;

        if (k == KeyEvent.VK_UP) cameraTilt -= 0.02;
        if (k == KeyEvent.VK_DOWN) cameraTilt += 0.02;

        if (k == KeyEvent.VK_SPACE && !running) {
            running = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

        int k = e.getKeyCode();

        if (k == KeyEvent.VK_W) upPressed = false;
        if (k == KeyEvent.VK_S) downPressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    /* ============================================================
       INNER CLASSES
       ============================================================ */

    class Paddle {

        double x;
        double y;

        Paddle(double x) {
            this.x = x;
            this.y = 0;
        }
    }

    class Ball {

        double x, y, z;
        double vx, vy, vz;

        Ball() {
            reset();
        }

        void reset() {

            x = 0;
            y = random.nextInt(100) - 50;
            z = random.nextInt(100) - 50;

            vx = random.nextBoolean() ? 6 : -6;
            vy = random.nextDouble() * 4 - 2;
            vz = random.nextDouble() * 4 - 2;
        }
    }

    class Particle {

        double x, y, z;
        double vx, vy, vz;
        double life = 1.0;

        Particle(double x, double y, double z) {

            this.x = x;
            this.y = y;
            this.z = z;

            vx = random.nextDouble() * 6 - 3;
            vy = random.nextDouble() * 6 - 3;
            vz = random.nextDouble() * 6 - 3;
        }

        void update() {

            x += vx;
            y += vy;
            z += vz;

            life -= 0.05;
        }
    }

    class Table {
        double width = 800;
        double height = 400;
    }

    /* ============================================================
       MAIN
       ============================================================ */

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame("3D Table Tennis Game");

            TableTennisGame3D game = new TableTennisGame3D();

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
