import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Baseball Batting Simulator Game
 * A comprehensive batting simulation with realistic physics and animations
 */
public class BaseballBattingSimulator extends JFrame implements ActionListener, KeyListener {

    // Game constants
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 700;
    private static final int FIELD_WIDTH = 800;
    private static final int FIELD_HEIGHT = 600;
    private static final double GRAVITY = 0.5;
    private static final double FRICTION = 0.99;

    // Game components
    private GamePanel gamePanel;
    private Timer gameTimer;
    private JLabel scoreLabel;
    private JLabel speedLabel;
    private JLabel instructionLabel;

    // Game state
    private GameState gameState;
    private Player player;
    private Ball ball;
    private Pitcher pitcher;
    private List<Particle> particles;
    private ScoreManager scoreManager;
    private SoundManager soundManager;

    // Input handling
    private boolean[] keys = new boolean[256];
    private boolean swinging = false;
    private long swingStartTime = 0;

    public BaseballBattingSimulator() {
        initializeGame();
        setupUI();
        startGame();
    }

    private static void run() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        new BaseballBattingSimulator();
    }

    private void initializeGame() {
        gameState = GameState.WAITING_FOR_PITCH;
        player = new Player(100, FIELD_HEIGHT - 150);
        ball = new Ball();
        pitcher = new Pitcher(FIELD_WIDTH - 150, FIELD_HEIGHT - 200);
        particles = new ArrayList<>();
        scoreManager = new ScoreManager();
        soundManager = new SoundManager();

        gameTimer = new Timer(16, this); // ~60 FPS
    }

    private void setupUI() {
        setTitle("Baseball Batting Simulator - Press SPACE to swing!");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Create UI components
        gamePanel = new GamePanel();
        scoreLabel = new JLabel("Score: 0 | Hits: 0 | Misses: 0");
        speedLabel = new JLabel("Ball Speed: 0 mph");
        instructionLabel = new JLabel("Press SPACE to swing, R to restart, ESC to quit");

        // Setup layout
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(scoreLabel);
        topPanel.add(speedLabel);

        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.add(instructionLabel);

        add(topPanel, BorderLayout.NORTH);
        add(gamePanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Add listeners
        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();
    }

    private void startGame() {
        gameTimer.start();
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateGame();
        gamePanel.repaint();
        updateUI();
    }

    private void updateGame() {
        switch (gameState) {
            case WAITING_FOR_PITCH:
                if (System.currentTimeMillis() - pitcher.getLastPitchTime() > 2000) {
                    pitcher.pitch(ball);
                    gameState = GameState.BALL_IN_FLIGHT;
                }
                break;

            case BALL_IN_FLIGHT:
                ball.update();
                updateParticles();

                // Check for swing
                if (swinging && !player.hasSwung()) {
                    player.swing();
                    checkHit();
                }

                // Check if ball is out of bounds
                if (ball.isOutOfBounds(FIELD_WIDTH, FIELD_HEIGHT)) {
                    if (!player.hasSwung()) {
                        scoreManager.addMiss();
                        soundManager.playMissSound();
                    }
                    resetForNextPitch();
                }
                break;

            case BALL_HIT:
                ball.update();
                updateParticles();

                if (ball.isOutOfBounds(FIELD_WIDTH, FIELD_HEIGHT)) {
                    resetForNextPitch();
                }
                break;
        }

        player.update();
        pitcher.update();
    }

    private void checkHit() {
        double distance = Math.sqrt(
                Math.pow(ball.getX() - player.getBatX(), 2) +
                        Math.pow(ball.getY() - player.getBatY(), 2)
        );

        if (distance < 30 && ball.isActive()) {
            // Hit detected!
            double hitPower = calculateHitPower();
            ball.hit(hitPower, player.getSwingAngle());

            // Create hit particles
            createHitParticles(ball.getX(), ball.getY());

            // Update score based on hit quality
            int points = calculateHitPoints(hitPower);
            scoreManager.addHit(points);
            soundManager.playHitSound();

            gameState = GameState.BALL_HIT;
        } else {
            // Swing and miss
            scoreManager.addMiss();
            soundManager.playMissSound();
        }
    }

    private double calculateHitPower() {
        long swingDuration = System.currentTimeMillis() - swingStartTime;
        double timing = Math.max(0, 1.0 - Math.abs(swingDuration - 200) / 200.0);
        return 0.5 + timing * 1.5; // Power between 0.5 and 2.0
    }

    private int calculateHitPoints(double hitPower) {
        if (hitPower > 1.8) return 10; // Home run
        if (hitPower > 1.4) return 7;  // Triple
        if (hitPower > 1.0) return 5;  // Double
        return 3; // Single
    }

    private void createHitParticles(double x, double y) {
        Random random = new Random();
        for (int i = 0; i < 15; i++) {
            particles.add(new Particle(
                    x + random.nextInt(20) - 10,
                    y + random.nextInt(20) - 10,
                    (random.nextDouble() - 0.5) * 10,
                    (random.nextDouble() - 0.5) * 10,
                    Color.YELLOW
            ));
        }
    }

    private void updateParticles() {
        particles.removeIf(particle -> {
            particle.update();
            return particle.shouldRemove();
        });
    }

    private void resetForNextPitch() {
        ball.reset();
        player.reset();
        gameState = GameState.WAITING_FOR_PITCH;
        pitcher.setLastPitchTime(System.currentTimeMillis());
        swinging = false;
    }

    private void updateUI() {
        scoreLabel.setText(String.format("Score: %d | Hits: %d | Misses: %d",
                scoreManager.getScore(), scoreManager.getHits(), scoreManager.getMisses()));
        speedLabel.setText(String.format("Ball Speed: %.1f mph", ball.getSpeed() * 2.237));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;

        if (e.getKeyCode() == KeyEvent.VK_SPACE && !swinging && gameState == GameState.BALL_IN_FLIGHT) {
            swinging = true;
            swingStartTime = System.currentTimeMillis();
        } else if (e.getKeyCode() == KeyEvent.VK_R) {
            resetGame();
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            System.exit(0);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void resetGame() {
        scoreManager.reset();
        resetForNextPitch();
    }

    // Inner classes for game components

    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawField(g2d);
            drawPitcher(g2d);
            drawPlayer(g2d);
            drawBall(g2d);
            drawParticles(g2d);
            drawGameInfo(g2d);
        }

        private void drawField(Graphics2D g2d) {
            // Background
            g2d.setColor(new Color(34, 139, 34));
            g2d.fillRect(0, 0, FIELD_WIDTH, FIELD_HEIGHT);

            // Pitcher's mound
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillOval(FIELD_WIDTH - 200, FIELD_HEIGHT - 230, 100, 60);

            // Home plate
            g2d.setColor(Color.WHITE);
            int[] plateX = {80, 100, 100, 90, 70, 80};
            int[] plateY = {FIELD_HEIGHT - 130, FIELD_HEIGHT - 130, FIELD_HEIGHT - 120,
                    FIELD_HEIGHT - 110, FIELD_HEIGHT - 120, FIELD_HEIGHT - 130};
            g2d.fillPolygon(plateX, plateY, 6);

            // Foul lines
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(90, FIELD_HEIGHT - 125, 0, 0);
            g2d.drawLine(90, FIELD_HEIGHT - 125, FIELD_WIDTH, 0);
        }

        private void drawPitcher(Graphics2D g2d) {
            pitcher.draw(g2d);
        }

        private void drawPlayer(Graphics2D g2d) {
            player.draw(g2d);
        }

        private void drawBall(Graphics2D g2d) {
            if (ball.isActive()) {
                ball.draw(g2d);

                // Draw ball trail
                g2d.setColor(new Color(255, 255, 255, 100));
                List<Point2D.Double> trail = ball.getTrail();
                for (int i = 1; i < trail.size(); i++) {
                    Point2D.Double p1 = trail.get(i - 1);
                    Point2D.Double p2 = trail.get(i);
                    g2d.setStroke(new BasicStroke(Math.max(1, i / 2)));
                    g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
                }
            }
        }

        private void drawParticles(Graphics2D g2d) {
            for (Particle particle : particles) {
                particle.draw(g2d);
            }
        }

        private void drawGameInfo(Graphics2D g2d) {
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));

            if (gameState == GameState.WAITING_FOR_PITCH) {
                g2d.drawString("Get ready... Pitch incoming!", 300, 50);
            } else if (gameState == GameState.BALL_IN_FLIGHT) {
                g2d.drawString("SWING! (Press SPACE)", 350, 50);
            }
        }
    }

    private class Player {
        private double x, y;
        private boolean hasSwung;
        private double swingAngle;
        private int animationFrame;
        private long lastAnimationUpdate;

        public Player(double x, double y) {
            this.x = x;
            this.y = y;
            reset();
        }

        public void swing() {
            hasSwung = true;
            swingAngle = Math.PI / 4;
            animationFrame = 0;
            lastAnimationUpdate = System.currentTimeMillis();
        }

        public void update() {
            if (hasSwung && System.currentTimeMillis() - lastAnimationUpdate > 50) {
                animationFrame++;
                lastAnimationUpdate = System.currentTimeMillis();
            }
        }

        public void reset() {
            hasSwung = false;
            swingAngle = 0;
            animationFrame = 0;
        }

        public void draw(Graphics2D g2d) {
            // Draw player body
            g2d.setColor(new Color(255, 220, 177));
            g2d.fillOval((int) x - 15, (int) y - 40, 30, 40);

            // Draw helmet
            g2d.setColor(Color.BLUE);
            g2d.fillOval((int) x - 12, (int) y - 55, 24, 20);

            // Draw bat
            g2d.setColor(new Color(139, 69, 19));
            g2d.setStroke(new BasicStroke(4));

            double batAngle = hasSwung ? swingAngle + (animationFrame * 0.3) : Math.PI / 6;
            double batLength = 40;
            double batEndX = x + Math.cos(batAngle) * batLength;
            double batEndY = y - 20 + Math.sin(batAngle) * batLength;

            g2d.drawLine((int) x, (int) y - 20, (int) batEndX, (int) batEndY);
        }

        public double getBatX() {
            double batAngle = hasSwung ? swingAngle + (animationFrame * 0.3) : Math.PI / 6;
            return x + Math.cos(batAngle) * 30;
        }

        public double getBatY() {
            double batAngle = hasSwung ? swingAngle + (animationFrame * 0.3) : Math.PI / 6;
            return y - 20 + Math.sin(batAngle) * 30;
        }

        public boolean hasSwung() { return hasSwung; }
        public double getSwingAngle() { return swingAngle; }
    }

    private class Ball {
        private double x, y;
        private double vx, vy;
        private boolean active;
        private List<Point2D.Double> trail;
        private Color color;

        public Ball() {
            trail = new ArrayList<>();
            reset();
        }

        public void reset() {
            x = FIELD_WIDTH - 150;
            y = FIELD_HEIGHT - 180;
            vx = 0;
            vy = 0;
            active = false;
            trail.clear();
            color = Color.WHITE;
        }

        public void pitch(double speed, double angle) {
            vx = -speed * Math.cos(angle);
            vy = speed * Math.sin(angle);
            active = true;
        }

        public void hit(double power, double angle) {
            double hitAngle = angle + Math.PI;
            vx = power * 15 * Math.cos(hitAngle);
            vy = power * 15 * Math.sin(hitAngle) - 5; // Add upward component
            color = Color.YELLOW;
        }

        public void update() {
            if (!active) return;

            x += vx;
            y += vy;

            // Apply physics
            vy += GRAVITY;
            vx *= FRICTION;
            vy *= FRICTION;

            // Ground collision
            if (y > FIELD_HEIGHT - 20) {
                y = FIELD_HEIGHT - 20;
                vy *= -0.6; // Bounce
                vx *= 0.8;  // Friction
            }

            // Update trail
            trail.add(new Point2D.Double(x, y));
            if (trail.size() > 10) {
                trail.remove(0);
            }
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.fillOval((int) x - 5, (int) y - 5, 10, 10);

            // Baseball stitching
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawArc((int) x - 4, (int) y - 4, 8, 8, 45, 90);
            g2d.drawArc((int) x - 4, (int) y - 4, 8, 8, 225, 90);
        }

        public boolean isOutOfBounds(int width, int height) {
            return x < -50 || x > width + 50 || y > height + 50;
        }

        public double getSpeed() {
            return Math.sqrt(vx * vx + vy * vy);
        }

        // Getters
        public double getX() { return x; }
        public double getY() { return y; }
        public boolean isActive() { return active; }
        public List<Point2D.Double> getTrail() { return trail; }
    }

    private class Pitcher {
        private double x, y;
        private long lastPitchTime;
        private int animationFrame;
        private boolean pitching;

        public Pitcher(double x, double y) {
            this.x = x;
            this.y = y;
            this.lastPitchTime = System.currentTimeMillis();
        }

        public void pitch(Ball ball) {
            Random random = new Random();
            double speed = 8 + random.nextDouble() * 4; // Random speed
            double angle = (random.nextDouble() - 0.5) * 0.5; // Random angle

            ball.pitch(speed, angle);
            pitching = true;
            animationFrame = 0;
            lastPitchTime = System.currentTimeMillis();
            soundManager.playPitchSound();
        }

        public void update() {
            if (pitching) {
                animationFrame++;
                if (animationFrame > 20) {
                    pitching = false;
                }
            }
        }

        public void draw(Graphics2D g2d) {
            // Draw pitcher body
            g2d.setColor(new Color(255, 220, 177));
            g2d.fillOval((int) x - 15, (int) y - 40, 30, 40);

            // Draw cap
            g2d.setColor(Color.RED);
            g2d.fillOval((int) x - 12, (int) y - 55, 24, 20);

            // Draw jersey
            g2d.setColor(Color.RED);
            g2d.fillRect((int) x - 20, (int) y - 10, 40, 30);

            // Pitching animation
            if (pitching) {
                g2d.setColor(Color.BLACK);
                double armAngle = (animationFrame / 20.0) * Math.PI;
                double armX = x + Math.cos(armAngle) * 25;
                double armY = y - 20 + Math.sin(armAngle) * 25;
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine((int) x, (int) y - 20, (int) armX, (int) armY);
            }
        }

        public long getLastPitchTime() { return lastPitchTime; }
        public void setLastPitchTime(long time) { this.lastPitchTime = time; }
    }

    private class Particle {
        private double x, y, vx, vy;
        private Color color;
        private int life, maxLife;

        public Particle(double x, double y, double vx, double vy, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.maxLife = 30;
            this.life = maxLife;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 0.2; // Gravity
            vx *= 0.95; // Air resistance
            life--;
        }

        public void draw(Graphics2D g2d) {
            int alpha = (int) (255 * (life / (double) maxLife));
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g2d.fillOval((int) x - 2, (int) y - 2, 4, 4);
        }

        public boolean shouldRemove() {
            return life <= 0;
        }
    }

    private class ScoreManager {
        private int score, hits, misses;

        public void addHit(int points) {
            hits++;
            score += points;
        }

        public void addMiss() {
            misses++;
        }

        public void reset() {
            score = hits = misses = 0;
        }

        public int getScore() { return score; }
        public int getHits() { return hits; }
        public int getMisses() { return misses; }
    }

    private class SoundManager {
        // Simulated sound effects (would use actual sound files in real implementation)
        public void playHitSound() {
            System.out.println("*CRACK!* - Ball hit!");
        }

        public void playMissSound() {
            System.out.println("*WHOOSH* - Swing and a miss!");
        }

        public void playPitchSound() {
            System.out.println("*WINDUP* - Here comes the pitch!");
        }
    }

    private enum GameState {
        WAITING_FOR_PITCH,
        BALL_IN_FLIGHT,
        BALL_HIT
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BaseballBattingSimulator::run);
    }
}