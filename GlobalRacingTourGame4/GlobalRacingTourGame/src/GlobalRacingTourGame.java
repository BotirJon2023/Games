import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;

public class GlobalRacingTourGame extends JFrame {
    public GlobalRacingTourGame() {
        setTitle("Global Racing Tour - Realistic Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GlobalRacingTourGame());
    }
}

class GamePanel extends JPanel implements Runnable, KeyListener {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    private Thread gameThread;
    private boolean running;

    public enum GameState { MENU, COUNTDOWN, PLAYING, FINISHED }
    private GameState currentState;

    private Track track;
    private ArrayList<Car> cars;
    private PlayerCar player1, player2;
    private AICar aiCar;
    private SkidMarkManager skidManager;
    private ParticleSystem particleSystem;
    private TourManager tourManager;
    private InputManager input;

    private int countdownTimer;
    private int countdownValue;
    private boolean isTwoPlayerMode;
    private int maxLaps = 3;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        input = new InputManager();
        track = new Track();
        skidManager = new SkidMarkManager(WIDTH, HEIGHT);
        particleSystem = new ParticleSystem();
        tourManager = new TourManager();
        cars = new ArrayList<>();

        currentState = GameState.MENU;
    }

    private void initRace() {
        cars.clear();
        skidManager.clear();
        particleSystem.clear();
        tourManager.reset();

        // Starting positions
        double startX = track.centerLine.get(0).x;
        double startY = track.centerLine.get(0).y;
        double startAngle = Math.atan2(
                track.centerLine.get(1).y - startY,
                track.centerLine.get(1).x - startX
        );

        player1 = new PlayerCar(startX, startY - 30, startAngle, Color.RED, KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_SPACE);
        cars.add(player1);

        if (isTwoPlayerMode) {
            player2 = new PlayerCar(startX, startY + 30, startAngle, Color.GREEN, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_ENTER);
            cars.add(player2);
        } else {
            aiCar = new AICar(track, startX + 40, startY, startAngle, Color.BLUE);
            cars.add(aiCar);
        }

        countdownValue = 3;
        countdownTimer = 60;
        currentState = GameState.COUNTDOWN;
    }

    public void start() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (gameThread == null) start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    private void update() {
        if (currentState == GameState.COUNTDOWN) {
            countdownTimer--;
            if (countdownTimer <= 0) {
                countdownValue--;
                countdownTimer = 60;
                if (countdownValue < 0) currentState = GameState.PLAYING;
            }
        }
        else if (currentState == GameState.PLAYING) {
            for (Car c : cars) {
                c.handleInput(input);
                c.updatePhysics();

                // Check track bounds (grass penalty)
                if (!track.isOnTrack(c.x, c.y)) {
                    c.applyOffRoadPenalty();
                } else {
                    c.resetOffRoad();
                }

                // Generate skid marks and smoke if drifting
                if (c.isDrifting) {
                    skidManager.addMark(c.x, c.y, c.angle, c.width * 0.8);
                    if (Math.random() > 0.5) {
                        particleSystem.addSmoke(c.x - Math.cos(c.angle) * 20, c.y - Math.sin(c.angle) * 20);
                    }
                }

                // Check lap completion
                track.checkLap(c);
            }

            particleSystem.update();
            skidManager.fade();

            // Check for race finish
            boolean allFinished = true;
            for (Car c : cars) {
                if (c.laps < maxLaps) allFinished = false;
            }
            if (allFinished) currentState = GameState.FINISHED;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw Track
        track.draw(g2d, tourManager);

        // Draw Skid Marks
        skidManager.draw(g2d);

        if (currentState != GameState.MENU) {
            // Draw Particles (under cars)
            particleSystem.draw(g2d);

            // Draw Cars
            for (Car c : cars) c.draw(g2d);

            // Draw HUD
            drawHUD(g2d);
        }

        if (currentState == GameState.MENU) drawMenu(g2d);
        if (currentState == GameState.COUNTDOWN) drawCountdown(g2d);
        if (currentState == GameState.FINISHED) drawFinish(g2d);

        g2d.dispose();
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(20, 20, 300, 120, 15, 15);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));

        g2d.drawString("Tour Leg: " + tourManager.getCurrentLegName(), 40, 50);
        g2d.drawString("Laps: " + maxLaps, 40, 80);

        if (!isTwoPlayerMode) {
            g2d.drawString("P1 Lap: " + player1.laps + " | AI Lap: " + aiCar.laps, 40, 110);
        } else {
            g2d.drawString("P1 Lap: " + player1.laps + " | P2 Lap: " + player2.laps, 40, 110);
        }

        // Speedometer for P1
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(WIDTH - 220, 20, 200, 80, 15, 15);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 30));
        int kmh = (int)(Math.abs(player1.speed) * 25);
        g2d.drawString(kmh + " km/h", WIDTH - 200, 70);
    }

    private void drawMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 60));
        String title = "GLOBAL RACING TOUR";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, 200);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 30));
        String opt1 = "Press 1 for 1 Player (vs Computer)";
        String opt2 = "Press 2 for 2 Players";
        fm = g2d.getFontMetrics();
        g2d.drawString(opt1, (WIDTH - fm.stringWidth(opt1)) / 2, 350);
        g2d.drawString(opt2, (WIDTH - fm.stringWidth(opt2)) / 2, 420);

        g2d.setFont(new Font("SansSerif", Font.ITALIC, 20));
        String controls = "P1: WASD + Space(Drift) | P2: Arrows + Enter(Drift)";
        fm = g2d.getFontMetrics();
        g2d.drawString(controls, (WIDTH - fm.stringWidth(controls)) / 2, 550);
    }

    private void drawCountdown(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 150));
        String text = countdownValue > 0 ? String.valueOf(countdownValue) : "GO!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, (WIDTH - fm.stringWidth(text)) / 2, HEIGHT / 2 + 50);
    }

    private void drawFinish(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 80));
        String text = "RACE FINISHED!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, (WIDTH - fm.stringWidth(text)) / 2, 300);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 30));
        String restart = "Press ENTER to return to Menu";
        fm = g2d.getFontMetrics();
        g2d.drawString(restart, (WIDTH - fm.stringWidth(restart)) / 2, 450);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        input.pressed(e.getKeyCode());
        if (currentState == GameState.MENU) {
            if (e.getKeyCode() == KeyEvent.VK_1) { isTwoPlayerMode = false; initRace(); }
            if (e.getKeyCode() == KeyEvent.VK_2) { isTwoPlayerMode = true; initRace(); }
        }
        if (currentState == GameState.FINISHED && e.getKeyCode() == KeyEvent.VK_ENTER) {
            currentState = GameState.MENU;
        }
    }
    @Override
    public void keyReleased(KeyEvent e) { input.released(e.getKeyCode()); }
    @Override
    public void keyTyped(KeyEvent e) {}
}

class InputManager {
    public boolean up, down, left, right, drift;
    public boolean p2_up, p2_down, p2_left, p2_right, p2_drift;

    public void pressed(int code) {
        if (code == KeyEvent.VK_W) up = true;
        if (code == KeyEvent.VK_S) down = true;
        if (code == KeyEvent.VK_A) left = true;
        if (code == KeyEvent.VK_D) right = true;
        if (code == KeyEvent.VK_SPACE) drift = true;

        if (code == KeyEvent.VK_UP) p2_up = true;
        if (code == KeyEvent.VK_DOWN) p2_down = true;
        if (code == KeyEvent.VK_LEFT) p2_left = true;
        if (code == KeyEvent.VK_RIGHT) p2_right = true;
        if (code == KeyEvent.VK_ENTER) p2_drift = true;
    }
    public void released(int code) {
        if (code == KeyEvent.VK_W) up = false;
        if (code == KeyEvent.VK_S) down = false;
        if (code == KeyEvent.VK_A) left = false;
        if (code == KeyEvent.VK_D) right = false;
        if (code == KeyEvent.VK_SPACE) drift = false;

        if (code == KeyEvent.VK_UP) p2_up = false;
        if (code == KeyEvent.VK_DOWN) p2_down = false;
        if (code == KeyEvent.VK_LEFT) p2_left = false;
        if (code == KeyEvent.VK_RIGHT) p2_right = false;
        if (code == KeyEvent.VK_ENTER) p2_drift = false;
    }
}

class Track {
    ArrayList<Point> centerLine;
    double trackWidth = 140;

    public Track() {
        centerLine = new ArrayList<>();
        int cx = 600, cy = 400;
        int rx = 450, ry = 280;
        for (int i = 0; i < 360; i += 4) {
            double rad = Math.toRadians(i);
            double var = Math.sin(rad * 3) * 60 + Math.cos(rad * 2) * 40;
            double x = cx + (rx + var) * Math.cos(rad);
            double y = cy + (ry + var) * Math.sin(rad);
            centerLine.add(new Point((int)x, (int)y));
        }
    }

    public void draw(Graphics2D g2d, TourManager tour) {
        // Grass
        g2d.setColor(tour.getGrassColor());
        g2d.fillRect(0, 0, 1200, 800);

        // Track Base
        g2d.setColor(tour.getTrackColor());
        BasicStroke trackStroke = new BasicStroke((float)trackWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g2d.setStroke(trackStroke);

        Path2D path = new Path2D.Double();
        path.moveTo(centerLine.get(0).x, centerLine.get(0).y);
        for (int i = 1; i < centerLine.size(); i++) {
            path.lineTo(centerLine.get(i).x, centerLine.get(i).y);
        }
        path.closePath();
        g2d.draw(path);

        // Curbs (Red/White)
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke((float)trackWidth + 10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(path);

        g2d.setColor(tour.getTrackColor());
        g2d.setStroke(trackStroke);
        g2d.draw(path);

        // Center dashed line
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{15, 15}, 0));
        g2d.draw(path);

        // Start/Finish Line
        Point p1 = centerLine.get(0);
        Point p2 = centerLine.get(1);
        double angle = Math.atan2(p2.y - p1.y, p2.x - p1.x) + Math.PI/2;
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(10));
        g2d.drawLine((int)(p1.x - Math.cos(angle)*trackWidth/2), (int)(p1.y - Math.sin(angle)*trackWidth/2),
                (int)(p1.x + Math.cos(angle)*trackWidth/2), (int)(p1.y + Math.sin(angle)*trackWidth/2));
    }

    public boolean isOnTrack(double px, double py) {
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < centerLine.size(); i++) {
            Point p1 = centerLine.get(i);
            Point p2 = centerLine.get((i + 1) % centerLine.size());
            double dist = distanceToSegment(px, py, p1.x, p1.y, p2.x, p2.y);
            if (dist < minDist) minDist = dist;
        }
        return minDist < trackWidth / 2;
    }

    private double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double nearX = x1 + t * dx, nearY = y1 + t * dy;
        return Math.sqrt((px - nearX)*(px - nearX) + (py - nearY)*(py - nearY));
    }

    public void checkLap(Car c) {
        Point start = centerLine.get(0);
        Point next = centerLine.get(1);
        double dx = next.x - start.x;
        double dy = next.y - start.y;

        // Simple line crossing detection
        double cross = (c.x - start.x) * dy - (c.y - start.y) * dx;
        double dot = (c.x - start.x) * dx + (c.y - start.y) * dy;

        if (Math.abs(cross) < 30 && dot > 0 && dot < 50) {
            if (!c.passedStartLine) {
                c.passedStartLine = true;
            } else {
                c.laps++;
                c.passedStartLine = false;
            }
        }
    }
}

abstract class Car {
    double x, y, angle, speed;
    double vx, vy;
    double width = 36, height = 70;
    Color color;
    boolean isDrifting;
    double slipAngle;
    double steerVisual = 0;
    int laps = 0;
    boolean passedStartLine = false;
    boolean isOffRoad = false;

    double maxSpeed = 9;
    double acceleration = 0.18;
    double brakeForce = 0.25;
    double friction = 0.98;
    double turnSpeed = 0.045;
    double grip = 0.15;

    public Car(double x, double y, double angle, Color color) {
        this.x = x; this.y = y; this.angle = angle; this.color = color;
    }

    public void updatePhysics() {
        double forwardX = Math.cos(angle);
        double forwardY = Math.sin(angle);

        if (speed > maxSpeed) speed = maxSpeed;
        if (speed < -maxSpeed/2) speed = -maxSpeed/2;

        speed *= isOffRoad ? 0.94 : friction;

        double steerFactor = Math.min(1.0, Math.abs(speed) / 3.0);
        if (speed < 0) steerFactor = -steerFactor;
        angle += steerVisual * turnSpeed * steerFactor;

        double targetVx = forwardX * speed;
        double targetVy = forwardY * speed;

        vx += (targetVx - vx) * grip;
        vy += (targetVy - vy) * grip;

        x += vx;
        y += vy;

        double actualAngle = Math.atan2(vy, vx);
        double slip = angle - actualAngle;
        while (slip > Math.PI) slip -= 2 * Math.PI;
        while (slip < -Math.PI) slip += 2 * Math.PI;
        slipAngle = Math.abs(slip);

        isDrifting = slipAngle > 0.25 && Math.abs(speed) > 2.5;
    }

    public void applyOffRoadPenalty() { isOffRoad = true; }
    public void resetOffRoad() { isOffRoad = false; }

    public abstract void handleInput(InputManager input);

    public void draw(Graphics2D g2d) {
        AffineTransform old = g2d.getTransform();
        g2d.translate(x, y);
        g2d.rotate(angle);

        // Shadow
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRoundRect(-width/2 + 4, -height/2 + 6, width, height, 10, 10);

        // Suspension squat visual
        double squat = 0;
        if (speed > 2) squat = 2;
        if (speed < -1) squat = -2;

        // Car Body
        GradientPaint gp = new GradientPaint(0, -height/2, color.brighter(), 0, height/2, color.darker());
        g2d.setPaint(gp);
        g2d.fillRoundRect(-width/2, -height/2 + squat, width, height, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.5));
        g2d.drawRoundRect(-width/2, -height/2 + squat, width, height, 10, 10);

        // Windshield
        g2d.setColor(new Color(30, 30, 50, 220));
        g2d.fillRoundRect(-width/2 + 5, -height/2 + 12 + squat, width - 10, 18, 5, 5);

        // Rear window
        g2d.fillRoundRect(-width/2 + 5, height/2 - 22 + squat, width - 10, 12, 5, 5);

        // Wheels
        drawWheel(g2d, -width/2 - 3, -height/2 + 12, true);
        drawWheel(g2d, width/2 - 5, -height/2 + 12, true);
        drawWheel(g2d, -width/2 - 3, height/2 - 22, false);
        drawWheel(g2d, width/2 - 5, height/2 - 22, false);

        // Headlights
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(-width/2 + 4, -height/2 + 2 + squat, 8, 5);
        g2d.fillOval(width/2 - 12, -height/2 + 2 + squat, 8, 5);

        // Taillights
        g2d.setColor(Color.RED);
        g2d.fillOval(-width/2 + 4, height/2 - 7 + squat, 8, 5);
        g2d.fillOval(width/2 - 12, height/2 - 7 + squat, 8, 5);

        g2d.setTransform(old);
    }

    private void drawWheel(Graphics2D g2d, double wx, double wy, boolean isFront) {
        AffineTransform old = g2d.getTransform();
        g2d.translate(wx + 4, wy + 6);
        if (isFront) g2d.rotate(steerVisual * 0.6);

        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(-4, -7, 8, 14);

        // Tire tread animation
        g2d.setColor(Color.LIGHT_GRAY);
        double offset = (System.currentTimeMillis() % 150) / 150.0 * 14;
        g2d.drawLine(-3, -7 + (int)offset, 3, -7 + (int)offset);
        g2d.setTransform(old);
    }
}

class PlayerCar extends Car {
    int k_up, k_down, k_left, k_right, k_drift;

    public PlayerCar(double x, double y, double angle, Color color, int ku, int kd, int kl, int kr, int kdft) {
        super(x, y, angle, color);
        k_up = ku; k_down = kd; k_left = kl; k_right = kr; k_drift = kdft;
    }

    @Override
    public void handleInput(InputManager input) {
        boolean up = (k_up == KeyEvent.VK_W) ? input.up : input.p2_up;
        boolean down = (k_down == KeyEvent.VK_S) ? input.down : input.p2_down;
        boolean left = (k_left == KeyEvent.VK_A) ? input.left : input.p2_left;
        boolean right = (k_right == KeyEvent.VK_D) ? input.right : input.p2_right;
        boolean drift = (k_drift == KeyEvent.VK_SPACE) ? input.drift : input.p2_drift;

        if (up) speed += acceleration;
        if (down) speed -= brakeForce;

        double steerInput = 0;
        if (left) steerInput = -1;
        if (right) steerInput = 1;
        steerVisual += (steerInput - steerVisual) * 0.2; // Smooth visual steering

        if (drift) {
            grip = 0.04; // Massive drift
            speed *= 0.98;
        } else {
            grip = isOffRoad ? 0.08 : 0.15; // Normal grip
        }
    }
}

class AICar extends Car {
    int currentWaypoint = 0;
    Track track;

    public AICar(Track track, double x, double y, double angle, Color color) {
        super(x, y, angle, color);
        this.track = track;
        this.maxSpeed = 8.5; // Slightly slower than perfect player
    }

    @Override
    public void handleInput(InputManager input) {
        Point target = track.centerLine.get(currentWaypoint);
        double dx = target.x - x;
        double dy = target.y - y;
        double dist = Math.sqrt(dx*dx + dy*dy);

        if (dist < 60) {
            currentWaypoint = (currentWaypoint + 1) % track.centerLine.size();
        }

        double targetAngle = Math.atan2(dy, dx);
        double angleDiff = targetAngle - angle;
        while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

        double steerInput = angleDiff * 1.5;
        steerInput = Math.max(-1, Math.min(1, steerInput));
        steerVisual += (steerInput - steerVisual) * 0.2;

        // Look ahead for braking
        int lookAhead = (currentWaypoint + 8) % track.centerLine.size();
        Point nextTarget = track.centerLine.get(lookAhead);
        double dx2 = nextTarget.x - target.x;
        double dy2 = nextTarget.y - target.y;
        double nextAngle = Math.atan2(dy2, dx2);
        double cornerAngle = Math.abs(nextAngle - targetAngle);
        while (cornerAngle > Math.PI) cornerAngle -= 2 * Math.PI;
        while (cornerAngle < -Math.PI) cornerAngle += 2 * Math.PI;

        boolean brake = cornerAngle > 0.6 && speed > 4;

        if (!brake) speed += acceleration;
        else speed -= brakeForce * 0.8;

        grip = 0.12; // AI has good but not perfect grip
    }
}

class SkidMarkManager {
    BufferedImage skidBuffer;
    Graphics2D skidG2d;

    public SkidMarkManager(int w, int h) {
        skidBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        skidG2d = skidBuffer.createGraphics();
        skidG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    public void addMark(double x, double y, double angle, double width) {
        skidG2d.setColor(new Color(0, 0, 0, 50));
        AffineTransform old = skidG2d.getTransform();
        skidG2d.translate(x, y);
        skidG2d.rotate(angle);
        skidG2d.fillOval(-width/2, -3, width, 6);
        skidG2d.setTransform(old);
    }

    public void fade() {
        skidG2d.setComposite(AlphaComposite.SrcOut);
        skidG2d.setColor(new Color(0, 0, 0, 3));
        skidG2d.fillRect(0, 0, skidBuffer.getWidth(), skidBuffer.getHeight());
        skidG2d.setComposite(AlphaComposite.SrcOver);
    }

    public void draw(Graphics2D g2d) {
        g2d.drawImage(skidBuffer, 0, 0, null);
    }

    public void clear() {
        skidG2d.setComposite(AlphaComposite.Clear);
        skidG2d.fillRect(0, 0, skidBuffer.getWidth(), skidBuffer.getHeight());
        skidG2d.setComposite(AlphaComposite.SrcOver);
    }
}

class ParticleSystem {
    ArrayList<Particle> particles;
    public ParticleSystem() { particles = new ArrayList<>(); }

    public void addSmoke(double x, double y) {
        particles.add(new Particle(x, y));
    }

    public void update() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update();
            if (p.life <= 0) it.remove();
        }
    }

    public void draw(Graphics2D g2d) {
        for (Particle p : particles) p.draw(g2d);
    }

    public void clear() { particles.clear(); }
}

class Particle {
    double x, y, vx, vy, life, maxLife, size;
    public Particle(double x, double y) {
        this.x = x; this.y = y;
        vx = (Math.random() - 0.5) * 2;
        vy = (Math.random() - 0.5) * 2;
        maxLife = 30 + Math.random() * 20;
        life = maxLife;
        size = 5 + Math.random() * 10;
    }
    public void update() {
        x += vx; y += vy;
        life--;
        size += 0.5;
    }
    public void draw(Graphics2D g2d) {
        int alpha = (int)((life / maxLife) * 150);
        g2d.setColor(new Color(200, 200, 200, alpha));
        g2d.fillOval((int)x, (int)y, (int)size, (int)size);
    }
}

class TourManager {
    int currentLeg = 0;
    String[] legNames = {"TOKYO", "PARIS", "NEW YORK", "CAIRO"};
    Color[] grassColors = {new Color(34, 139, 34), new Color(60, 179, 113), new Color(107, 142, 35), new Color(210, 180, 140)};
    Color[] trackColors = {new Color(60, 60, 60), new Color(80, 80, 80), new Color(50, 50, 50), new Color(100, 80, 60)};

    public void reset() { currentLeg = 0; }
    public String getCurrentLegName() { return legNames[currentLeg]; }
    public Color getGrassColor() { return grassColors[currentLeg]; }
    public Color getTrackColor() { return trackColors[currentLeg]; }
}