import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.*;
import javax.sound.sampled.*;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.Timer;

public class BowlingSimulatorPro extends JPanel implements ActionListener, MouseListener, MouseMotionListener {

    private static final int WIDTH = 1100;
    private static final int HEIGHT = 750;
    private static final int LANE_X = 150;
    private static final int LANE_Y = 100;
    private static final int LANE_WIDTH = 800;
    private static final int LANE_HEIGHT = 550;

    private Ball ball;
    private List<Pin> pins = new ArrayList<>();
    private List<Pin> activePins = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();

    private Timer timer;
    private boolean isRolling = false;
    private boolean gameOver = false;

    // Scoring system
    private int currentFrame = 0;
    private int ballInFrame = 0;
    private int[] rolls = new int[21];
    private int rollIndex = 0;
    private int[] frameTotals = new int[10];
    private int totalScore = 0;

    // Aiming
    private Point dragStart = null;
    private Point dragEnd = null;
    private double aimPower = 0;
    private double aimAngle = 0;

    // Visual effects
    private double cameraShake = 0;
    private BufferedImage laneShine;
    private int shineOffset = 0;
    private Random rand = new Random();

    private Random random = new Random();

    public BowlingSimulatorPro() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(15, 20, 35));
        setFocusable(true);
        addMouseListener(this);
        addMouseMotionListener(this);

        timer = new Timer(16, this);
        createLaneShine();
        resetGame();
    }

    private void createLaneShine() {
        laneShine = new BufferedImage(800, 550, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = laneShine.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gp = new GradientPaint(0, 0, new Color(255,255,255,30), 0, 550, new Color(255,255,255,0));
        g.setPaint(gp);
        g.fillRect(0, 0, 800, 550);
        g.dispose();
    }

    private void resetGame() {
        ball = new Ball(550, LANE_Y + LANE_HEIGHT - 80);
        pins.clear();
        activePins.clear();
        particles.clear();

        double spacing = 38;
        int[][] positions = {
                {550, 120},                                // 1
                {524, 158}, {576, 158},                    // 2-3
                {498, 196}, {550, 196}, {602, 196},        // 4-6
                {472, 234}, {524, 234}, {576, 234}, {628, 234}  // 7-10
        };

        for (int[] pos : positions) {
            Pin pin = new Pin(pos[0], pos[1]);
            pins.add(pin);
            activePins.add(pin);
        }

        isRolling = false;
        dragStart = dragEnd = null;
        cameraShake = 0;
        Arrays.fill(rolls, 0);
        Arrays.fill(frameTotals, 0);
        rollIndex = 0;
        currentFrame = 0;
        ballInFrame = 0;
        totalScore = 0;
        gameOver = false;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Camera shake
        if (cameraShake > 0) {
            int shakeX = (int)(Math.random() * cameraShake * 2 - cameraShake);
            int shakeY = (int)(Math.random() * cameraShake * 2 - cameraShake);
            g2d.translate(shakeX, shakeY);
            cameraShake *= 0.9;
        }

        drawLane(g2d);
        drawPins(g2d);
        drawParticles(g2d);
        if (ball != null) ball.draw(g2d);
        drawAimingLine(g2d);
        drawScoreboard(g2d);
        drawTitle(g2d);

        if (gameOver) {
            drawGameOver(g2d);
        }

        g2d.dispose();
    }

    private void drawLane(Graphics2D g2d) {
        // Wood lane
        GradientPaint wood = new GradientPaint(0, LANE_Y, new Color(180, 140, 80),
                0, LANE_Y + LANE_HEIGHT, new Color(140, 100, 50));
        g2d.setPaint(wood);
        g2d.fillRoundRect(LANE_X, LANE_Y, LANE_WIDTH, LANE_HEIGHT, 30, 30);

        // Lane arrows
        g2d.setColor(new Color(255, 255, 255, 80));
        int[] arrowX = {550, 530, 570, 510, 590, 480, 620};
        for (int x : arrowX) {
            Polygon arrow = new Polygon();
            arrow.addPoint(x, LANE_Y + 400);
            arrow.addPoint(x - 10, LANE_Y + 430);
            arrow.addPoint(x + 10, LANE_Y + 430);
            g2d.fillPolygon(arrow);
        }

        // Shine effect
        shineOffset = (shineOffset + 2) % 1000;
        g2d.drawImage(laneShine, LANE_X + shineOffset - 1000, LANE_Y, null);
        g2d.drawImage(laneShine, LANE_X + shineOffset, LANE_Y, null);

        // Gutters
        g2d.setColor(new Color(40, 40, 50));
        g2d.fillRect(100, LANE_Y, 50, LANE_HEIGHT);
        g2d.fillRect(950, LANE_Y, 50, LANE_HEIGHT);
    }

    private void drawPins(Graphics2D g2d) {
        for (Pin pin : pins) {
            pin.draw(g2d);
        }
    }

    private void drawParticles(Graphics2D g2d) {
        for (Iterator<Particle> it = particles.iterator(); it.hasNext();) {
            Particle p = it.next();
            p.draw(g2d);
            if (p.isDead()) it.remove();
        }
    }

    private void drawAimingLine(Graphics2D g2d) {
        if (dragStart != null && dragEnd != null && !isRolling) {
            g2d.setStroke(new BasicStroke(4));
            g2d.setColor(new Color(0, 255, 255, 180));
            g2d.drawLine(dragStart.x, dragStart.y, dragEnd.x, dragEnd.y);

            double dist = dragStart.distance(dragEnd);
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Consolas", Font.BOLD, 22));
            g2d.drawString(String.format("Power: %.0f%%", Math.min(dist * 0.4, 100)), dragEnd.x + 20, dragEnd.y - 10);
        }
    }

    private void drawTitle(Graphics2D g2d) {
        g2d.setFont(new Font("Impact", Font.BOLD, 48));
        g2d.setColor(new Color(255, 220, 0));
        g2d.drawString("PRO BOWLING SIMULATOR", 280, 70);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("Drag from ball to aim • Release to throw • Perfect 300 awaits!", 280, 95);
    }

    private void drawScoreboard(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 30, 200));
        g2d.fillRoundRect(50, 120, 1000, 120, 25, 25);

        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Courier New", Font.BOLD, 20));
        for (int i = 0; i < 10; i++) {
            int x = 80 + i * 95;
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRoundRect(x, 140, 85, 80, 15, 15);
            g2d.setColor(Color.YELLOW);
            g2d.drawString("" + (i + 1), x + 35, 160);

            int r1 = (i * 2 < rollIndex) ? rolls[i * 2] : -1;
            int r2 = (i * 2 + 1 < rollIndex) ? rolls[i * 2 + 1] : -1;

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            if (r1 == 10 && i < 9) {
                g2d.drawString("X", x + 15, 190);
            } else if (r1 >= 0) {
                g2d.drawString(r1 == 0 ? "-" : "" + r1, x + 15, 190);
            }
            if (r2 >= 0 && !(r1 == 10 && i < 9)) {
                if (r1 + r2 == 10) g2d.setColor(Color.MAGENTA);
                g2d.drawString(r2 == 0 ? "-" : (r1 + r2 == 10 ? "/" : "" + r2), x + 50, 190);
            }

            if (frameTotals[i] > 0) {
                g2d.setColor(Color.CYAN);
                g2d.setFont(new Font("Arial", Font.BOLD, 26));
                g2d.drawString("" + frameTotals[i], x + 15, 215);
            }
        }

        g2d.setColor(Color.GREEN);
        g2d.setFont(new Font("Impact", Font.BOLD, 40));
        g2d.drawString("TOTAL: " + totalScore, 820, 280);
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Impact", Font.BOLD, 80));
        String msg = "GAME OVER";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(msg, (WIDTH - fm.stringWidth(msg)) / 2, HEIGHT / 2 - 50);

        g2d.setColor(totalScore >= 250 ? Color.ORANGE : Color.CYAN);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        msg = "Final Score: " + totalScore;
        fm = g2d.getFontMetrics();
        g2d.drawString(msg, (WIDTH - fm.stringWidth(msg)) / 2, HEIGHT / 2 + 30);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isRolling) {
            ball.update();

            // Ball-pin collisions
            for (Iterator<Pin> it = activePins.iterator(); it.hasNext();) {
                Pin pin = it.next();
                if (ball.collidesWith(pin)) {
                    double force = ball.speed() * 1.8;
                    pin.knockOver(ball.vx * 0.4, ball.vy * 0.4 + force);
                    createImpactParticles((int) pin.x, (int) pin.y);
                    cameraShake = Math.min(force, 15);
                    ball.vx *= 0.75;
                    it.remove();
                }
            }

            // Pin-pin collisions
            for (int i = 0; i < pins.size(); i++) {
                Pin p1 = pins.get(i);
                if (!p1.fallen) continue;
                for (int j = i + 1; j < pins.size(); j++) {
                    Pin p2 = pins.get(j);
                    if (p1.collidesWith(p2)) {
                        p1.bounceOff(p2);
                    }
                }
            }

            // Update fallen pins
            for (Pin pin : pins) {
                if (pin.fallen) pin.update();
            }

            particles.removeIf(Particle::isDead);

            if (ball.y > LANE_Y + LANE_HEIGHT + 100 || ball.speed() < 0.3) {
                endRoll();
            }

            repaint();
        }
    }

    private void createImpactParticles(int x, int y) {
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(x, y));
        }
    }

    private void endRoll() {
        timer.stop();
        isRolling = false;

        int knocked = 10 - activePins.size();
        rolls[rollIndex++] = knocked;

        if (knocked == 10) {
            if (currentFrame < 9 || ballInFrame < 2) {
                ballInFrame = 0;
                currentFrame++;
            }
        } else if (ballInFrame == 1 || currentFrame == 9) {
            ballInFrame = 0;
            currentFrame++;
        } else {
            ballInFrame++;
        }

        calculateScore();

        if (currentFrame >= 10 && ballInFrame == 0) {
            gameOver = true;
        }

        new Timer(1800, e -> resetLane()).start();
        repaint();
    }

    private void resetLane() {
        ball = new Ball(550, LANE_Y + LANE_HEIGHT - 80);
        activePins.clear();
        activePins.addAll(pins);
        for (Pin p : pins) p.reset();
        particles.clear();
        repaint();
    }

    private void calculateScore() {
        totalScore = 0;
        int roll = 0;
        for (int frame = 0; frame < 10; frame++) {
            if (rolls[roll] == 10) { // Strike
                frameTotals[frame] = 10 + rolls[roll + 1] + rolls[roll + 2];
                roll++;
            } else if (roll + 1 < 21 && rolls[roll] + rolls[roll + 1] == 10) { // Spare
                frameTotals[frame] = 10 + rolls[roll + 2];
                roll += 2;
            } else {
                frameTotals[frame] = rolls[roll] + (roll + 1 < 21 ? rolls[roll + 1] : 0);
                roll += 2;
            }
            totalScore += frameTotals[frame];
        }
    }

    // Mouse controls
    @Override public void mousePressed(MouseEvent e) {
        if (isRolling || gameOver) return;
        if (e.getPoint().distance(ball.x, ball.y) < 50) {
            dragStart = e.getPoint();
        }
    }

    @Override public void mouseReleased(MouseEvent e) {
        if (dragStart != null && !isRolling && !gameOver) {
            dragEnd = e.getPoint();
            double dx = dragStart.x - dragEnd.x;
            double dy = dragStart.y - dragEnd.y;
            aimPower = Math.min(dragStart.distance(dragEnd) * 0.4, 20);
            aimAngle = Math.atan2(dy, dx);

            ball.vx = aimPower * Math.cos(aimAngle);
            ball.vy = aimPower * Math.sin(aimAngle) * 0.6;
            ball.spin = (dragEnd.x < dragStart.x) ? 0.3 : -0.3;

            isRolling = true;
            timer.start();
            dragStart = dragEnd = null;
        }
    }

    @Override public void mouseDragged(MouseEvent e) {
        if (dragStart != null) {
            dragEnd = e.getPoint();
            repaint();
        }
    }

    // Empty implementations
    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // Inner classes
    class Ball {
        double x, y;
        double vx = 0, vy = 0;
        double spin = 0;
        static final int RADIUS = 24;

        Ball(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void update() {
            vx *= 0.985;
            vy *= 0.985;
            vy += 0.15; // gravity-like curve
            x += vx;
            y += vy;
            spin *= 0.98;

            spin *= 0.98;

            if (x < LANE_X + 30) { x = LANE_X + 30; vx = -vx * 0.5; }
            if (x > LANE_X + LANE_WIDTH - 30) { x = LANE_X + LANE_WIDTH - 30; vx = -vx * 0.5; }
        }

        double speed() {
            return Math.sqrt(vx*vx + vy*vy);
        }

        boolean collidesWith(Pin pin) {
            double dx = x - pin.x;
            double dy = y - pin.y;
            return Math.sqrt(dx*dx + dy*dy) < RADIUS + 18;
        }

        void draw(Graphics2D g) {
            AffineTransform at = g.getTransform();
            g.translate(x, y);
            g.rotate(spin);

            RadialGradientPaint paint = new RadialGradientPaint(0, 0, RADIUS,
                    new float[]{0f, 1f}, new Color[]{Color.WHITE, new Color(40,40,100)});
            g.setPaint(paint);
            g.fillOval(-RADIUS, -RADIUS, RADIUS*2, RADIUS*2);

            g.setColor(Color.RED);
            g.fillOval(-8, -8, 16, 16);

            g.setTransform(at);
        }
    }

    class Pin {
        double x, y;
        double vx = 0, vy = 0;
        double angle = 0, angVel = 0;
        boolean fallen = false;

        Pin(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void knockOver(double fx, double fy) {
            vx = fx + random.nextDouble() * 6 - 3;
            vy = fy -Math.abs(fy) * 1.5 + random.nextDouble() * 3;
            angVel = (random.nextDouble() - 0.5) * 0.5;
            fallen = true;
        }

        void bounceOff(Pin other) {
            double dx = other.x - x;
            double dy = other.y - y;
            double dist = Math.hypot(dx, dy);
            if (dist < 40) {
                double nx = dx / dist;
                double ny = dy / dist;
                vx -= nx * 5;
                vy -= ny * 5;
                other.vx += nx * 5;
                other.vy += ny * 5;
            }
        }

        boolean collidesWith(Pin other) {
            return Math.hypot(x - other.x, y - other.y) < 40;
        }

        void update() {
            if (!fallen) return;
            vx *= 0.97;
            vy *= 0.97;
            vy += 0.4;
            x += vx;
            y += vy;
            angle += angVel;
            angVel *= 0.96;
        }

        void reset() {
            fallen = false;
            vx = vy = angVel = 0;
            angle = 0;
        }

        void draw(Graphics2D g) {
            AffineTransform at = g.getTransform();
            g.translate(x, y);
            g.rotate(angle);

            // Pin body
            RoundRectangle2D body = new RoundRectangle2D.Double(-15, -40, 30, 80, 20, 20);
            g.setPaint(new GradientPaint(0, -40, Color.WHITE, 0, 40, new Color(240, 220, 180)));
            g.fill(body);

            // Red ring
            g.setColor(Color.RED.darker());
            g.fillOval(-12, -12, 24, 24);

            // Stripes
            g.setColor(Color.WHITE);
            g.fillRect(-14, -25, 28, 6);
            g.fillRect(-14, 15, 28, 6);

            g.setTransform(at);
        }
    }

    class Particle {
        double x, y, vx, vy;
        int life = 60;
        Color color;

        Particle(int x, int y) {
            this.x = x + random.nextInt(20) - 10;
            this.y = y;
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 3 + random.nextDouble() * 8;
            vx = Math.cos(angle) * speed;
            vy = Math.sin(angle) * speed - 3;
            color = Color.getHSBColor((float) random.nextDouble() * 0.1f, 1f, 1f);
        }

        void draw(Graphics2D g) {
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), life * 4));
            g.fillOval((int)x - 4, (int)y - 4, 8, 8);
            x += vx;
            y += vy;
            vy += 0.3;
            life--;
        }

        boolean isDead() { return life <= 0; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bowling Simulator PRO - 720+ Lines Edition");
            BowlingSimulatorPro game = new BowlingSimulatorPro();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);

            JButton restart = new JButton("NEW GAME");
            restart.setFont(new Font("Arial", Font.BOLD, 20));
            restart.addActionListener(e -> game.resetGame());
            frame.add(restart, BorderLayout.SOUTH);

            frame.setVisible(true);
        });
    }
}