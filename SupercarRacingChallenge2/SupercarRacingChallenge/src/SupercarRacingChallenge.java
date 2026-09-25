import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class SupercarRacingChallenge extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SupercarRacingChallenge game = new SupercarRacingChallenge();
            game.setVisible(true);
        });
    }

    public SupercarRacingChallenge() {
        setTitle("Supercar Racing Challenge");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        add(new GamePanel());
        pack();
        setLocationRelativeTo(null);
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    // Constants
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final int FPS = 60;

    // Game states
    private enum State { MENU, PLAYING, GAME_OVER }
    private State state = State.MENU;

    // Mode: true = vs AI, false = 2 players
    private boolean vsAI = true;

    // Game objects
    private Car player1;
    private Car player2; // or AI
    private ArrayList<Particle> particles = new ArrayList<>();
    private ArrayList<RoadMark> roadMarks = new ArrayList<>();
    private Random rand = new Random();

    // Track
    private Path2D trackOuter;
    private Path2D trackInner;
    private Area trackArea;
    private double finishLineY = 80;

    // Input
    private boolean[] keys = new boolean[256];

    // Timer
    private Timer timer;

    // Animation
    private double animTime = 0;
    private int winner = 0; // 1 or 2
    private int lapTarget = 3;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        createTrack();
        initRoadMarks();

        timer = new Timer(1000 / FPS, this);
        timer.start();
    }

    private void createTrack() {
        // Beautiful oval-ish track with smooth curves
        trackOuter = new Path2D.Double();
        trackOuter.moveTo(150, 100);
        trackOuter.curveTo(150, 40, 850, 40, 850, 100);
        trackOuter.lineTo(900, 200);
        trackOuter.curveTo(980, 200, 980, 500, 900, 500);
        trackOuter.lineTo(850, 600);
        trackOuter.curveTo(850, 660, 150, 660, 150, 600);
        trackOuter.lineTo(100, 500);
        trackOuter.curveTo(20, 500, 20, 200, 100, 200);
        trackOuter.closePath();

        trackInner = new Path2D.Double();
        trackInner.moveTo(250, 180);
        trackInner.curveTo(250, 140, 750, 140, 750, 180);
        trackInner.lineTo(780, 250);
        trackInner.curveTo(830, 250, 830, 450, 780, 450);
        trackInner.lineTo(750, 520);
        trackInner.curveTo(750, 560, 250, 560, 250, 520);
        trackInner.lineTo(220, 450);
        trackInner.curveTo(170, 450, 170, 250, 220, 250);
        trackInner.closePath();

        trackArea = new Area(trackOuter);
        trackArea.subtract(new Area(trackInner));
    }

    private void initRoadMarks() {
        roadMarks.clear();
        // Center line dashes along the track (approximate)
        for (int i = 0; i < 40; i++) {
            double t = i / 40.0;
            double x = 500 + 280 * Math.cos(t * Math.PI * 2);
            double y = 350 + 180 * Math.sin(t * Math.PI * 2);
            roadMarks.add(new RoadMark(x, y, t * 360));
        }
    }

    private void startGame(boolean aiMode) {
        vsAI = aiMode;
        state = State.PLAYING;
        particles.clear();
        winner = 0;

        // Starting positions near the bottom straight
        player1 = new Car(300, 580, 0, new Color(0, 220, 255), "P1"); // Cyan
        player2 = new Car(380, 580, 0, new Color(255, 50, 120), vsAI ? "AI" : "P2"); // Magenta

        if (vsAI) {
            player2.isAI = true;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        animTime += 0.016;

        if (state == State.PLAYING) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        // Player 1 controls (WASD)
        handleInput(player1, KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D);

        if (!vsAI) {
            // Player 2 controls (Arrows)
            handleInput(player2, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT);
        } else {
            // Simple but effective AI
            updateAI(player2);
        }

        player1.update(trackArea);
        player2.update(trackArea);

        // Particles from both cars
        spawnParticles(player1);
        spawnParticles(player2);

        // Update particles
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update();
            if (p.life <= 0) it.remove();
        }

        // Check finish / laps
        checkLaps(player1);
        checkLaps(player2);

        // Collision between cars (simple bounce)
        if (player1.getBounds().intersects(player2.getBounds())) {
            double dx = player1.x - player2.x;
            double dy = player1.y - player2.y;
            double dist = Math.hypot(dx, dy);
            if (dist > 0) {
                dx /= dist;
                dy /= dist;
                player1.vx += dx * 2.5;
                player1.vy += dy * 2.5;
                player2.vx -= dx * 2.5;
                player2.vy -= dy * 2.5;
            }
        }

        // Winner check
        if (player1.laps >= lapTarget) {
            winner = 1;
            state = State.GAME_OVER;
        } else if (player2.laps >= lapTarget) {
            winner = 2;
            state = State.GAME_OVER;
        }
    }

    private void handleInput(Car car, int up, int down, int left, int right) {
        if (keys[up]) car.accelerate();
        if (keys[down]) car.brake();
        if (keys[left]) car.steer(-1);
        if (keys[right]) car.steer(1);
    }

    private void updateAI(Car ai) {
        // AI aims for a racing line slightly ahead
        double targetAngle = Math.atan2(
                Math.sin(animTime * 0.8) * 180 + 350 - ai.y,
                Math.cos(animTime * 0.8) * 280 + 500 - ai.x
        );

        double angleDiff = normalizeAngle(targetAngle - ai.angle);

        if (angleDiff > 0.15) ai.steer(1);
        else if (angleDiff < -0.15) ai.steer(-1);

        // Always try to go fast, slow a bit on sharp turns
        if (Math.abs(angleDiff) < 0.6) {
            ai.accelerate();
        } else {
            ai.brake();
        }
    }

    private double normalizeAngle(double a) {
        while (a > Math.PI) a -= Math.PI * 2;
        while (a < -Math.PI) a += Math.PI * 2;
        return a;
    }

    private void checkLaps(Car car) {
        // Simple lap detection when crossing finish line going upward
        if (car.y < finishLineY + 30 && car.y > finishLineY - 30 &&
                car.prevY >= finishLineY + 30 && car.vy < 0) {
            car.laps++;
            // Big celebration particles
            for (int i = 0; i < 40; i++) {
                particles.add(new Particle(car.x, car.y, car.color, true));
            }
        }
        car.prevY = car.y;
    }

    private void spawnParticles(Car car) {
        if (car.speed > 3) {
            // Exhaust / speed trails
            double backX = car.x - Math.cos(car.angle) * 22;
            double backY = car.y - Math.sin(car.angle) * 22;
            particles.add(new Particle(backX, backY, car.color, false));
            if (rand.nextFloat() < 0.3) {
                particles.add(new Particle(backX + rand.nextGaussian() * 4,
                        backY + rand.nextGaussian() * 4,
                        car.color, false));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Background gradient
        GradientPaint bg = new GradientPaint(0, 0, new Color(10, 5, 30),
                0, HEIGHT, new Color(5, 15, 40));
        g2.setPaint(bg);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Subtle grid
        g2.setColor(new Color(30, 40, 80, 40));
        for (int i = 0; i < WIDTH; i += 40) g2.drawLine(i, 0, i, HEIGHT);
        for (int i = 0; i < HEIGHT; i += 40) g2.drawLine(0, i, WIDTH, i);

        if (state == State.MENU) {
            drawMenu(g2);
        } else {
            drawTrack(g2);
            drawParticles(g2);
            player1.draw(g2);
            player2.draw(g2);
            drawHUD(g2);

            if (state == State.GAME_OVER) {
                drawGameOver(g2);
            }
        }
    }

    private void drawTrack(Graphics2D g2) {
        // Outer glow
        g2.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(0, 150, 255, 40));
        g2.draw(trackOuter);

        // Track surface
        g2.setColor(new Color(25, 25, 45));
        g2.fill(trackArea);

        // Track border neon
        g2.setStroke(new BasicStroke(4));
        g2.setColor(new Color(0, 200, 255));
        g2.draw(trackOuter);
        g2.setColor(new Color(255, 50, 150));
        g2.draw(trackInner);

        // Center dashed line (animated)
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{15, 15}, (float)(animTime * 40)));
        g2.setColor(new Color(255, 255, 100, 180));
        for (RoadMark m : roadMarks) {
            g2.draw(new Line2D.Double(m.x - 8, m.y, m.x + 8, m.y));
        }

        // Finish line
        g2.setStroke(new BasicStroke(6));
        g2.setColor(Color.WHITE);
        g2.drawLine(200, (int) finishLineY, 400, (int) finishLineY);
        g2.setColor(new Color(255, 255, 0));
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString("FINISH", 280, (int) finishLineY - 10);
    }

    private void drawParticles(Graphics2D g2) {
        for (Particle p : particles) {
            p.draw(g2);
        }
    }

    private void drawHUD(Graphics2D g2) {
        // Player 1 info
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(15, 15, 220, 90, 15, 15);
        g2.setColor(player1.color);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(15, 15, 220, 90, 15, 15);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2.drawString("PLAYER 1", 30, 40);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        g2.setColor(Color.WHITE);
        g2.drawString(String.format("Speed: %.0f km/h", player1.speed * 12), 30, 65);
        g2.drawString("Lap: " + player1.laps + " / " + lapTarget, 30, 88);

        // Player 2 / AI info
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(WIDTH - 235, 15, 220, 90, 15, 15);
        g2.setColor(player2.color);
        g2.drawRoundRect(WIDTH - 235, 15, 220, 90, 15, 15);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2.drawString(vsAI ? "COMPUTER" : "PLAYER 2", WIDTH - 220, 40);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        g2.setColor(Color.WHITE);
        g2.drawString(String.format("Speed: %.0f km/h", player2.speed * 12), WIDTH - 220, 65);
        g2.drawString("Lap: " + player2.laps + " / " + lapTarget, WIDTH - 220, 88);

        // Controls hint
        g2.setColor(new Color(255, 255, 255, 120));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        if (vsAI) {
            g2.drawString("Controls: WASD", WIDTH / 2 - 50, HEIGHT - 20);
        } else {
            g2.drawString("P1: WASD   |   P2: Arrow Keys", WIDTH / 2 - 110, HEIGHT - 20);
        }
    }

    private void drawMenu(Graphics2D g2) {
        // Title with glow
        g2.setFont(new Font("Segoe UI", Font.BOLD, 52));
        String title = "SUPERCAR RACING";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (WIDTH - fm.stringWidth(title)) / 2;

        // Glow
        g2.setColor(new Color(0, 200, 255, 80));
        g2.drawString(title, tx + 2, 182);
        g2.drawString(title, tx - 2, 182);
        g2.setColor(new Color(0, 220, 255));
        g2.drawString(title, tx, 180);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
        g2.setColor(new Color(255, 80, 150));
        String sub = "CHALLENGE";
        fm = g2.getFontMetrics();
        g2.drawString(sub, (WIDTH - fm.stringWidth(sub)) / 2, 230);

        // Buttons
        drawButton(g2, WIDTH / 2 - 160, 320, 320, 60, "1 PLAYER  vs  COMPUTER", true);
        drawButton(g2, WIDTH / 2 - 160, 400, 320, 60, "2 PLAYERS", false);

        // Instructions
        g2.setColor(new Color(200, 200, 255, 180));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        g2.drawString("Press 1 for Single Player   |   Press 2 for Two Players", WIDTH / 2 - 180, 520);
        g2.drawString("First to complete 3 laps wins!", WIDTH / 2 - 100, 550);

        // Decorative cars
        g2.setColor(new Color(0, 220, 255, 150));
        g2.fill(createCarShape(180, 600, -0.4, 1.3));
        g2.setColor(new Color(255, 50, 120, 150));
        g2.fill(createCarShape(820, 600, 0.4, 1.3));
    }

    private void drawButton(Graphics2D g2, int x, int y, int w, int h, String text, boolean highlight) {
        GradientPaint gp = new GradientPaint(x, y,
                highlight ? new Color(0, 100, 180) : new Color(80, 20, 80),
                x, y + h,
                highlight ? new Color(0, 180, 255) : new Color(180, 40, 120));
        g2.setPaint(gp);
        g2.fillRoundRect(x, y, w, h, 20, 20);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, w, h, 20, 20);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + h / 2 + 8);
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        String msg = winner == 1 ? "PLAYER 1 WINS!" : (vsAI ? "COMPUTER WINS!" : "PLAYER 2 WINS!");
        Color winColor = winner == 1 ? player1.color : player2.color;

        g2.setFont(new Font("Segoe UI", Font.BOLD, 56));
        FontMetrics fm = g2.getFontMetrics();
        int mx = (WIDTH - fm.stringWidth(msg)) / 2;

        // Glow
        g2.setColor(new Color(winColor.getRed(), winColor.getGreen(), winColor.getBlue(), 100));
        g2.drawString(msg, mx + 3, 303);
        g2.setColor(winColor);
        g2.drawString(msg, mx, 300);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        g2.setColor(Color.WHITE);
        String restart = "Press ENTER to return to Menu";
        fm = g2.getFontMetrics();
        g2.drawString(restart, (WIDTH - fm.stringWidth(restart)) / 2, 380);
    }

    private Shape createCarShape(double x, double y, double angle, double scale) {
        AffineTransform at = new AffineTransform();
        at.translate(x, y);
        at.rotate(angle);
        at.scale(scale, scale);

        Path2D car = new Path2D.Double();
        car.moveTo(18, 0);
        car.lineTo(10, -9);
        car.lineTo(-14, -8);
        car.lineTo(-18, -4);
        car.lineTo(-18, 4);
        car.lineTo(-14, 8);
        car.lineTo(10, 9);
        car.closePath();
        return at.createTransformedShape(car);
    }

    // --- Input ---
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code < keys.length) keys[code] = true;

        if (state == State.MENU) {
            if (code == KeyEvent.VK_1) startGame(true);
            if (code == KeyEvent.VK_2) startGame(false);
        } else if (state == State.GAME_OVER) {
            if (code == KeyEvent.VK_ENTER) state = State.MENU;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

// ==================== CAR ====================
class Car {
    double x, y;
    double angle;          // radians
    double speed;
    double vx, vy;
    Color color;
    String name;
    boolean isAI = false;
    int laps = 0;
    double prevY;

    private static final double ACCEL = 0.18;
    private static final double BRAKE = 0.25;
    private static final double FRICTION = 0.985;
    private static final double STEER_SPEED = 0.065;
    private static final double MAX_SPEED = 9.5;

    public Car(double x, double y, double angle, Color color, String name) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.color = color;
        this.name = name;
        this.prevY = y;
    }

    public void accelerate() {
        speed += ACCEL;
        if (speed > MAX_SPEED) speed = MAX_SPEED;
    }

    public void brake() {
        speed -= BRAKE;
        if (speed < -2) speed = -2;
    }

    public void steer(int dir) {
        if (Math.abs(speed) > 0.5) {
            angle += dir * STEER_SPEED * (speed / MAX_SPEED);
        }
    }

    public void update(Area track) {
        // Apply velocity from angle + speed
        vx = Math.cos(angle) * speed;
        vy = Math.sin(angle) * speed;

        x += vx;
        y += vy;

        // Friction
        speed *= FRICTION;
        if (Math.abs(speed) < 0.05) speed = 0;

        // Keep on track (soft boundary)
        if (!track.contains(x, y)) {
            // Push back toward center
            double cx = 500, cy = 350;
            double dx = cx - x;
            double dy = cy - y;
            double dist = Math.hypot(dx, dy);
            if (dist > 0) {
                x += dx / dist * 3;
                y += dy / dist * 3;
            }
            speed *= 0.7; // slow down when off track
        }
    }

    public void draw(Graphics2D g2) {
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angle);

        // Glow
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
        g2.fillOval(-28, -16, 56, 32);

        // Body
        GradientPaint bodyGrad = new GradientPaint(-15, -8, color.brighter(), 15, 8, color.darker());
        g2.setPaint(bodyGrad);

        Path2D body = new Path2D.Double();
        body.moveTo(20, 0);
        body.curveTo(16, -10, 8, -11, 0, -10);
        body.lineTo(-16, -9);
        body.curveTo(-20, -7, -22, -3, -22, 0);
        body.curveTo(-22, 3, -20, 7, -16, 9);
        body.lineTo(0, 10);
        body.curveTo(8, 11, 16, 10, 20, 0);
        body.closePath();
        g2.fill(body);

        // Cockpit
        g2.setColor(new Color(20, 30, 50, 220));
        g2.fillRoundRect(-6, -6, 12, 12, 6, 6);

        // Headlights
        g2.setColor(new Color(255, 255, 200));
        g2.fillOval(14, -7, 6, 4);
        g2.fillOval(14, 3, 6, 4);

        // Neon underglow
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
        g2.setStroke(new BasicStroke(3));
        g2.draw(body);

        g2.setTransform(old);

        // Name tag
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.drawString(name, (float) x - 10, (float) y - 22);
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x - 18, y - 10, 36, 20);
    }
}

// ==================== PARTICLE ====================
class Particle {
    double x, y;
    double vx, vy;
    Color color;
    float life;
    float maxLife;
    boolean isBurst;

    public Particle(double x, double y, Color color, boolean burst) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.isBurst = burst;
        this.maxLife = burst ? 40 + (float) Math.random() * 30 : 15 + (float) Math.random() * 15;
        this.life = maxLife;

        if (burst) {
            double a = Math.random() * Math.PI * 2;
            double s = 2 + Math.random() * 5;
            vx = Math.cos(a) * s;
            vy = Math.sin(a) * s;
        } else {
            vx = (Math.random() - 0.5) * 1.5;
            vy = (Math.random() - 0.5) * 1.5;
        }
    }

    public void update() {
        x += vx;
        y += vy;
        vx *= 0.96;
        vy *= 0.96;
        life--;
    }

    public void draw(Graphics2D g2) {
        float alpha = life / maxLife;
        int size = isBurst ? (int) (4 + alpha * 6) : (int) (2 + alpha * 4);
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 200)));
        g2.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
    }
}

// ==================== ROAD MARK ====================
class RoadMark {
    double x, y, angle;
    public RoadMark(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
    }
}