import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HoverboardRacingAdventure extends JFrame {

    public HoverboardRacingAdventure() {
        setTitle("Hoverboard Racing Adventure");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new HoverboardRacingAdventure().setVisible(true);
        });
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {

    private final int WIDTH = 1000;
    private final int HEIGHT = 700;
    private Timer timer;

    // Game States
    private enum State { MENU, PLAYING, GAME_OVER }
    private State currentState = State.MENU;
    private boolean vsAI = true;

    // Track Parameters
    private Path2D innerTrack;
    private Path2D outerTrack;
    private Path2D centerlineTrack;
    private final int TOTAL_LAPS = 3;

    // Game Objects
    private Hoverboard p1;
    private Hoverboard p2;
    private List<Star> stars;
    private List<Particle> particles;
    private List<SpeedPad> speedPads;
    private List<Obstacle> obstacles;
    private Random rand = new Random();

    // Key Input Flags
    private boolean[] keys = new boolean[256];

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(15, 12, 35));
        setFocusable(true);
        addKeyListener(this);

        initTrack();
        initDecorations();

        // 60 FPS Game Loop
        timer = new Timer(16, this);
        timer.start();
    }

    private void initTrack() {
        // Oval track geometry
        innerTrack = new Path2D.Double();
        outerTrack = new Path2D.Double();
        centerlineTrack = new Path2D.Double();

        // Center oval path
        double cx = WIDTH / 2.0;
        double cy = HEIGHT / 2.0;
        double rx = 340;
        double ry = 220;
        double width = 90;

        for (int i = 0; i <= 360; i += 5) {
            double rad = Math.toRadians(i);
            double x = cx + rx * Math.cos(rad);
            double y = cy + ry * Math.sin(rad);

            double xOut = cx + (rx + width) * Math.cos(rad);
            double yOut = cy + (ry + width) * Math.sin(rad);

            double xIn = cx + (rx - width) * Math.cos(rad);
            double yIn = cy + (ry - width) * Math.sin(rad);

            if (i == 0) {
                centerlineTrack.moveTo(x, y);
                outerTrack.moveTo(xOut, yOut);
                innerTrack.moveTo(xIn, yIn);
            } else {
                centerlineTrack.lineTo(x, y);
                outerTrack.lineTo(xOut, yOut);
                innerTrack.lineTo(xIn, yIn);
            }
        }

        // Add Speed Pads & Obstacles
        speedPads = new ArrayList<>();
        obstacles = new ArrayList<>();

        double[] padAngles = {45, 135, 225, 315};
        for (double angle : padAngles) {
            double rad = Math.toRadians(angle);
            double px = cx + rx * Math.cos(rad);
            double py = cy + ry * Math.sin(rad);
            speedPads.add(new SpeedPad(px, py));
        }

        double[] obsAngles = {90, 180, 270};
        for (double angle : obsAngles) {
            double rad = Math.toRadians(angle);
            double ox = cx + (rx + 25) * Math.cos(rad);
            double oy = cy + (ry + 25) * Math.sin(rad);
            obstacles.add(new Obstacle(ox, oy));
        }
    }

    private void initDecorations() {
        stars = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            stars.add(new Star(rand.nextInt(WIDTH), rand.nextInt(HEIGHT), rand.nextFloat() * 3 + 1));
        }
        particles = new ArrayList<>();
    }

    private void resetGame() {
        double startX = WIDTH / 2.0 + 340;
        double startY1 = HEIGHT / 2.0 - 25;
        double startY2 = HEIGHT / 2.0 + 25;

        p1 = new Hoverboard(startX, startY1, new Color(255, 120, 0), "P1 (Orange)");
        p2 = new Hoverboard(startX, startY2, new Color(0, 230, 255), vsAI ? "AI (Cyan)" : "P2 (Cyan)");
        particles.clear();
        currentState = State.PLAYING;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentState == State.PLAYING) {
            updatePlayerInputs();
            p1.update(innerTrack, outerTrack, speedPads, obstacles, particles);

            if (vsAI) {
                updateAI();
            }
            p2.update(innerTrack, outerTrack, speedPads, obstacles, particles);

            // Check Win Condition
            if (p1.laps >= TOTAL_LAPS || p2.laps >= TOTAL_LAPS) {
                currentState = State.GAME_OVER;
            }
        }

        // Particle updates
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.alpha <= 0) particles.remove(i);
        }

        // Stars twinkles
        for (Star s : stars) s.update();

        repaint();
    }

    private void updatePlayerInputs() {
        // Player 1 controls (W, A, D)
        if (keys[KeyEvent.VK_W]) p1.accelerate();
        if (keys[KeyEvent.VK_A]) p1.steer(-1);
        if (keys[KeyEvent.VK_D]) p1.steer(1);

        // Player 2 controls (UP, LEFT, RIGHT)
        if (!vsAI) {
            if (keys[KeyEvent.VK_UP]) p2.accelerate();
            if (keys[KeyEvent.VK_LEFT]) p2.steer(-1);
            if (keys[KeyEvent.VK_RIGHT]) p2.steer(1);
        }
    }

    private void updateAI() {
        // AI Steering calculation using target track center
        double cx = WIDTH / 2.0;
        double cy = HEIGHT / 2.0;
        double rx = 340;
        double ry = 220;

        // Target angle ahead of current position
        double currentAngle = Math.atan2(p2.y - cy, p2.x - cx);
        double targetAngle = currentAngle + 0.35; // Look ahead

        double targetX = cx + rx * Math.cos(targetAngle);
        double targetY = cy + ry * Math.sin(targetAngle);

        double desiredAngle = Math.atan2(targetY - p2.y, targetX - p2.x);
        double angleDiff = desiredAngle - p2.angle;

        // Normalize angle difference to [-PI, PI]
        while (angleDiff < -Math.PI) angleDiff += Math.PI * 2;
        while (angleDiff > Math.PI) angleDiff -= Math.PI * 2;

        if (angleDiff < -0.1) p2.steer(-1);
        else if (angleDiff > 0.1) p2.steer(1);

        p2.accelerate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Background Starfield
        drawBackground(g2d);

        // 2. Track & Elements
        drawTrack(g2d);

        // 3. Particles
        for (Particle p : particles) p.draw(g2d);

        // 4. Hoverboards
        if (currentState == State.PLAYING || currentState == State.GAME_OVER) {
            p1.draw(g2d);
            p2.draw(g2d);
        }

        // 5. UI / Overlay
        drawUI(g2d);
    }

    private void drawBackground(Graphics2D g2d) {
        // Space Gradient
        GradientPaint bgGradient = new GradientPaint(0, 0, new Color(10, 8, 25), 0, HEIGHT, new Color(25, 15, 45));
        g2d.setPaint(bgGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Stars
        for (Star s : stars) s.draw(g2d);
    }

    private void drawTrack(Graphics2D g2d) {
        // Outer glow track area
        g2d.setStroke(new BasicStroke(180, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(40, 20, 70, 150));
        g2d.draw(centerlineTrack);

        // Track Asphalt
        g2d.setStroke(new BasicStroke(160, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(25, 25, 40));
        g2d.draw(centerlineTrack);

        // Track Neon Borders
        g2d.setStroke(new BasicStroke(4));
        g2d.setColor(new Color(255, 0, 128));
        g2d.draw(outerTrack);
        g2d.setColor(new Color(0, 230, 255));
        g2d.draw(innerTrack);

        // Finish Line
        double startX = WIDTH / 2.0 + 340;
        double startY = HEIGHT / 2.0;
        g2d.setStroke(new BasicStroke(8, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{12}, 0));
        g2d.setColor(Color.WHITE);
        g2d.drawLine((int) startX - 80, (int) startY, (int) startX + 80, (int) startY);

        // Draw SpeedPads and Obstacles
        for (SpeedPad pad : speedPads) pad.draw(g2d);
        for (Obstacle obs : obstacles) obs.draw(g2d);
    }

    private void drawUI(Graphics2D g2d) {
        if (currentState == State.MENU) {
            // Menu Overlay
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
            g2d.setColor(Color.CYAN);
            drawCenteredString(g2d, "HOVERBOARD RACING ADVENTURE", HEIGHT / 3);

            g2d.setFont(new Font("SansSerif", Font.PLAIN, 24));
            g2d.setColor(Color.WHITE);
            drawCenteredString(g2d, "Press [ 1 ] for Player vs Computer (AI)", HEIGHT / 2);
            drawCenteredString(g2d, "Press [ 2 ] for 2-Player Mode (Local)", HEIGHT / 2 + 50);

            g2d.setFont(new Font("SansSerif", Font.ITALIC, 16));
            g2d.setColor(Color.LIGHT_GRAY);
            drawCenteredString(g2d, "P1 Controls: W (Gas), A/D (Steer)  |  P2 Controls: UP (Gas), LEFT/RIGHT (Steer)", HEIGHT / 2 + 130);

        } else if (currentState == State.PLAYING) {
            // HUD
            g2d.setFont(new Font("SansSerif", Font.BOLD, 20));

            // P1 HUD
            g2d.setColor(p1.color);
            g2d.drawString(p1.name + " | Lap: " + Math.min(p1.laps + 1, TOTAL_LAPS) + "/" + TOTAL_LAPS, 30, 40);

            // P2 HUD
            g2d.setColor(p2.color);
            g2d.drawString(p2.name + " | Lap: " + Math.min(p2.laps + 1, TOTAL_LAPS) + "/" + TOTAL_LAPS, WIDTH - 260, 40);

        } else if (currentState == State.GAME_OVER) {
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            String winner = p1.laps >= TOTAL_LAPS ? p1.name : p2.name;
            Color winColor = p1.laps >= TOTAL_LAPS ? p1.color : p2.color;

            g2d.setFont(new Font("SansSerif", Font.BOLD, 52));
            g2d.setColor(winColor);
            drawCenteredString(g2d, winner + " WINS!", HEIGHT / 3);

            g2d.setFont(new Font("SansSerif", Font.PLAIN, 24));
            g2d.setColor(Color.WHITE);
            drawCenteredString(g2d, "Press [ SPACE ] to return to Menu", HEIGHT / 2 + 40);
        }
    }

    private void drawCenteredString(Graphics2D g2d, String text, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, y);
    }

    // Key Listener Implementations
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code < keys.length) keys[code] = true;

        if (currentState == State.MENU) {
            if (code == KeyEvent.VK_1) {
                vsAI = true;
                resetGame();
            } else if (code == KeyEvent.VK_2) {
                vsAI = false;
                resetGame();
            }
        } else if (currentState == State.GAME_OVER) {
            if (code == KeyEvent.VK_SPACE) {
                currentState = State.MENU;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code < keys.length) keys[code] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

// Hoverboard Entity
class Hoverboard {
    double x, y;
    double vx, vy;
    double angle = -Math.PI / 2; // Face UP initially
    double speed = 0;

    final double maxSpeed = 7.5;
    final double accel = 0.22;
    final double friction = 0.97;
    final double turnSpeed = 0.06;

    Color color;
    String name;
    int laps = 0;
    private boolean crossedLine = false;

    public Hoverboard(double x, double y, Color color, String name) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.name = name;
    }

    public void accelerate() {
        speed = Math.min(speed + accel, maxSpeed);
    }

    public void steer(int dir) {
        angle += dir * turnSpeed;
    }

    public void update(Path2D inner, Path2D outer, List<SpeedPad> speedPads, List<Obstacle> obstacles, List<Particle> particles) {
        // Friction & Inertia
        speed *= friction;
        vx = Math.cos(angle) * speed;
        vy = Math.sin(angle) * speed;

        x += vx;
        y += vy;

        // Collision with Outer/Inner Walls
        if (inner.contains(x, y) || !outer.contains(x, y)) {
            speed = -speed * 0.5; // Bounce back
            x -= vx * 1.5;
            y -= vy * 1.5;
        }

        // Speed Pad Interaction
        for (SpeedPad pad : speedPads) {
            if (Point2D.distance(x, y, pad.x, pad.y) < 30) {
                speed = maxSpeed * 1.4; // Boost!
                for (int i = 0; i < 5; i++) {
                    particles.add(new Particle(x, y, Color.YELLOW, 4, 15));
                }
            }
        }

        // Obstacle Interaction
        for (Obstacle obs : obstacles) {
            if (Point2D.distance(x, y, obs.x, obs.y) < 25) {
                speed = 1.0; // Slow down penalty
                for (int i = 0; i < 8; i++) {
                    particles.add(new Particle(x, y, Color.RED, 5, 20));
                }
            }
        }

        // Thruster Particle FX
        if (speed > 0.5) {
            double tailX = x - Math.cos(angle) * 15;
            double tailY = y - Math.sin(angle) * 15;
            particles.add(new Particle(tailX, tailY, color, 3, 20));
        }

        // Lap Detection (Crossing X = Center + 340 moving upward)
        double finishX = 1000 / 2.0 + 340;
        double finishY = 700 / 2.0;

        if (Math.abs(x - finishX) < 50 && Math.abs(y - finishY) < 40) {
            if (!crossedLine && vy < 0) { // Moving upwards through line
                laps++;
                crossedLine = true;
            }
        } else {
            crossedLine = false;
        }
    }

    public void draw(Graphics2D g2d) {
        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(x, y);
        g2d.rotate(angle);

        // Hoverboard Body
        g2d.setColor(color);
        g2d.fillRoundRect(-18, -8, 36, 16, 10, 10);

        // Inner Tech Core
        g2d.setColor(Color.WHITE);
        g2d.fillOval(-6, -4, 12, 8);

        // Glow Thruster (Back)
        g2d.setColor(new Color(0, 255, 255, 200));
        g2d.fillOval(-22, -5, 8, 10);

        g2d.setTransform(oldTransform);
    }
}

// Particle Effect
class Particle {
    double x, y, vx, vy;
    Color color;
    float size;
    int alpha = 255;
    int fadeRate;

    public Particle(double x, double y, Color color, float size, int fadeRate) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.size = size;
        this.fadeRate = fadeRate;

        Random r = new Random();
        this.vx = (r.nextDouble() - 0.5) * 3;
        this.vy = (r.nextDouble() - 0.5) * 3;
    }

    public void update() {
        x += vx;
        y += vy;
        alpha = Math.max(0, alpha - fadeRate);
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g2d.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
    }
}

// Track Elements
class SpeedPad {
    double x, y;

    public SpeedPad(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(255, 230, 0, 180));
        g2d.fillOval((int) x - 18, (int) y - 18, 36, 36);
        g2d.setColor(Color.WHITE);
        g2d.drawOval((int) x - 18, (int) y - 18, 36, 36);
    }
}

class Obstacle {
    double x, y;

    public Obstacle(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(255, 50, 50, 200));
        g2d.fillRect((int) x - 12, (int) y - 12, 24, 24);
        g2d.setColor(Color.BLACK);
        g2d.drawRect((int) x - 12, (int) y - 12, 24, 24);
    }
}

// Background Starfield
class Star {
    double x, y;
    float size;
    float alpha;
    boolean dimming = false;

    public Star(double x, double y, float size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.alpha = (float) Math.random();
    }

    public void update() {
        if (dimming) {
            alpha -= 0.02f;
            if (alpha <= 0.2f) dimming = false;
        } else {
            alpha += 0.02f;
            if (alpha >= 1.0f) dimming = true;
        }
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(1.0f, 1.0f, 1.0f, Math.max(0f, Math.min(1f, alpha))));
        g2d.fill(new Ellipse2D.Double(x, y, size, size));
    }
}