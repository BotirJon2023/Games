import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class VolleyballGameSimulation extends JFrame {

    // ==================== CONSTANTS ====================
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;
    public static final int COURT_WIDTH = 900;
    public static final int COURT_HEIGHT = 550;
    public static final int NET_HEIGHT = 200;
    public static final int NET_X = COURT_WIDTH / 2;
    public static final int FPS = 60;

    // ==================== GAME MODES ====================
    enum GameMode {
        QUICK_MATCH,
        TOURNAMENT,
        PRACTICE,
        CHAMPIONSHIP
    }

    enum GameState {
        MAIN_MENU,
        MODE_SELECT,
        SERVING,
        PLAYING,
        POINT_OVER,
        GAME_OVER,
        TOURNAMENT_BRACKET,
        PAUSED,
        SETTINGS
    }

    enum Difficulty {
        EASY,
        NORMAL,
        HARD,
        EXTREME
    }

    // ==================== MAIN CLASS ====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VolleyballGameSimulation game = new VolleyballGameSimulation();
            game.setVisible(true);
        });
    }

    private GamePanel gamePanel;
    private CardLayout cardLayout;
    private JPanel mainContainer;

    public VolleyballGameSimulation() {
        super("🏐 Advanced Volleyball Championship 🏐");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        this.setResizable(false);
        this.setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        gamePanel = new GamePanel();
        mainContainer.add(gamePanel, "GAME");

        this.add(mainContainer);
        this.addKeyListener(gamePanel);
        this.setFocusable(true);

        // Apply dark theme
        this.setBackground(new Color(20, 20, 30));
    }

    // ==================== VECTOR2D CLASS ====================
    static class Vector2D {
        public double x, y;

        public Vector2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Vector2D add(Vector2D v) {
            return new Vector2D(this.x + v.x, this.y + v.y);
        }

        public Vector2D subtract(Vector2D v) {
            return new Vector2D(this.x - v.x, this.y - v.y);
        }

        public Vector2D multiply(double scalar) {
            return new Vector2D(this.x * scalar, this.y * scalar);
        }

        public double magnitude() {
            return Math.sqrt(x * x + y * y);
        }

        public Vector2D normalize() {
            double mag = magnitude();
            if (mag > 0) {
                return new Vector2D(x / mag, y / mag);
            }
            return new Vector2D(0, 0);
        }

        public double dot(Vector2D v) {
            return this.x * v.x + this.y * v.y;
        }

        public double distance(Vector2D v) {
            return Math.sqrt(Math.pow(x - v.x, 2) + Math.pow(y - v.y, 2));
        }
    }

    // ==================== PARTICLE SYSTEM ====================
    static class Particle {
        double x, y, vx, vy;
        int life, maxLife;
        Color color;
        double size;
        boolean fadeOut;

        public Particle(double x, double y, Color color, double size) {
            this.x = x;
            this.y = y;
            this.vx = (Math.random() - 0.5) * 6;
            this.vy = (Math.random() - 0.5) * 6;
            this.color = color;
            this.size = size;
            this.life = 40 + (int)(Math.random() * 30);
            this.maxLife = life;
            this.fadeOut = true;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 0.15; // Gravity
            vx *= 0.98; // Air resistance
            life--;
        }

        public void draw(Graphics2D g) {
            float alpha = (float)life / maxLife;
            Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int)(alpha * 255));
            g.setColor(c);
            g.fillOval((int)x, (int)y, (int)size, (int)size);
        }

        public boolean isAlive() {
            return life > 0;
        }
    }

    // ==================== POWER-UP SYSTEM ====================
    static class PowerUp {
        double x, y;
        String type;
        int duration;
        boolean active;
        Color color;
        long spawnTime;

        public static final String SPEED_BOOST = "SPEED";
        public static final String POWER_HIT = "POWER";
        public static final String SLOW_MO = "SLOW";
        public static final String SHIELD = "SHIELD";

        public PowerUp(double x, double y, String type) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.active = true;
            this.duration = 5000; // 5 seconds
            this.spawnTime = System.currentTimeMillis();

            switch(type) {
                case SPEED_BOOST: color = Color.CYAN; break;
                case POWER_HIT: color = Color.RED; break;
                case SLOW_MO: color = Color.BLUE; break;
                case SHIELD: color = Color.GREEN; break;
                default: color = Color.WHITE;
            }
        }

        public void draw(Graphics2D g) {
            if (!active) return;

            // Glow effect
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
            g.fillOval((int)x - 15, (int)y - 15, 30, 30);

            g.setColor(color);
            g.fillOval((int)x - 10, (int)y - 10, 20, 20);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.drawString(type.substring(0, 1), (int)x - 3, (int)y + 4);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - spawnTime > duration;
        }
    }

    // ==================== BALL CLASS ====================
    static class Ball {
        double x, y, vx, vy;
        double radius = 15;
        double rotation = 0;
        double rotationSpeed = 0;

        // Physics constants
        final double GRAVITY = 0.5;
        final double AIR_RESISTANCE = 0.992;
        final double BOUNCE_DAMPING = 0.7;

        // Trail effect
        List<Vector2D> trail = new ArrayList<>();
        int maxTrailLength = 20;

        public Ball() {
            reset();
        }

        public void reset() {
            x = 150;
            y = 300;
            vx = 0;
            vy = 0;
            rotation = 0;
            rotationSpeed = 0;
            trail.clear();
        }

        public void serve(boolean toRight, double power) {
            y = 280;
            x = toRight ? 150 : COURT_WIDTH - 150;
            vx = toRight ? power : -power;
            vy = -7 - Math.random() * 3;
            rotationSpeed = vx * 3;
        }

        public void spike(double angle, double power) {
            vx = Math.cos(angle) * power;
            vy = Math.sin(angle) * power;
            rotationSpeed = power * 2;
        }

        public void update(double timeScale) {
            // Store trail position
            trail.add(new Vector2D(x, y));
            if (trail.size() > maxTrailLength) {
                trail.remove(0);
            }

            // Apply Physics
            vy += GRAVITY * timeScale;
            vx *= AIR_RESISTANCE;
            vy *= AIR_RESISTANCE;

            x += vx * timeScale;
            y += vy * timeScale;

            rotation += rotationSpeed * timeScale;
            rotationSpeed *= 0.98; // Slow rotation

            // Floor Collision
            if (y + radius > COURT_HEIGHT) {
                y = COURT_HEIGHT - radius;
                vy = -vy * BOUNCE_DAMPING;
                vx *= 0.7;
                rotationSpeed *= 0.5;
                if (Math.abs(vy) < 0.5) vy = 0;
            }

            // Ceiling Collision
            if (y - radius < 0) {
                y = radius;
                vy = -vy * BOUNCE_DAMPING;
            }

            // Wall Collisions
            if (x - radius < 0) {
                x = radius;
                vx = -vx * BOUNCE_DAMPING;
            }
            if (x + radius > COURT_WIDTH) {
                x = COURT_WIDTH - radius;
                vx = -vx * BOUNCE_DAMPING;
            }
        }

        public void draw(Graphics2D g) {
            // Draw trail
            for (int i = 0; i < trail.size(); i++) {
                Vector2D pos = trail.get(i);
                float alpha = (float)i / trail.size() * 0.5f;
                g.setColor(new Color(255, 140, 0, (int)(alpha * 255)));
                double size = radius * (i / (double)trail.size());
                g.fillOval((int)(pos.x - size), (int)(pos.y - size),
                        (int)(size * 2), (int)(size * 2));
            }

            // Draw ball
            g.setColor(new Color(255, 140, 0));
            g.fillOval((int)(x - radius), (int)(y - radius),
                    (int)(radius * 2), (int)(radius * 2));

            // Draw ball pattern
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2));
            g.drawOval((int)(x - radius), (int)(y - radius),
                    (int)(radius * 2), (int)(radius * 2));

            // Draw rotation indicator
            int seamX = (int)(x + Math.cos(rotation) * radius * 0.7);
            int seamY = (int)(y + Math.sin(rotation) * radius * 0.7);
            g.drawLine((int)x, (int)y, seamX, seamY);
        }

        public Rectangle getBounds() {
            return new Rectangle((int)(x - radius), (int)(y - radius),
                    (int)(radius * 2), (int)(radius * 2));
        }
    }

    // ==================== PLAYER CLASS ====================
    static class Player {
        double x, y, vx, vy;
        double width = 45, height = 75;
        boolean isPlayer1;
        boolean isJumping, isMoving, isDashing;
        Color teamColor;
        String name;
        int number;

        // Stats
        double moveSpeed = 5.0;
        double jumpForce = -15.0;
        double hitPower = 1.0;
        boolean hasShield = false;
        int dashCooldown = 0;

        // Animation
        int frame = 0;
        int animationState = 0; // 0: idle, 1: run, 2: jump, 3: hit

        // Statistics
        int hits = 0;
        int spikes = 0;
        int blocks = 0;
        int aces = 0;

        final double GRAVITY = 0.6;
        final double DASH_SPEED = 12.0;

        public Player(boolean isPlayer1, String name, int number) {
            this.isPlayer1 = isPlayer1;
            this.name = name;
            this.number = number;
            this.teamColor = isPlayer1 ? new Color(30, 144, 255) : new Color(220, 20, 60);
            reset();
        }

        public void reset() {
            x = isPlayer1 ? 200 : COURT_WIDTH - 245;
            y = COURT_HEIGHT - height;
            vx = 0;
            vy = 0;
            isJumping = false;
            isMoving = false;
            isDashing = false;
            dashCooldown = 0;
            hasShield = false;
            hits = 0;
            spikes = 0;
            blocks = 0;
            aces = 0;
        }

        public void activatePowerUp(String type) {
            switch(type) {
                case PowerUp.SPEED_BOOST:
                    moveSpeed = 8.0;
                    break;
                case PowerUp.POWER_HIT:
                    hitPower = 1.5;
                    break;
                case PowerUp.SHIELD:
                    hasShield = true;
                    break;
            }
        }

        public void resetPowerUps() {
            moveSpeed = 5.0;
            hitPower = 1.0;
            hasShield = false;
        }

        public void updateInput(boolean left, boolean right, boolean jump, boolean dash) {
            if (dashCooldown > 0) dashCooldown--;

            if (isJumping) {
                vy += GRAVITY;
                y += vy;

                if (y >= COURT_HEIGHT - height) {
                    y = COURT_HEIGHT - height;
                    vy = 0;
                    isJumping = false;
                    animationState = 0;
                }
            } else {
                if (dash && dashCooldown <= 0) {
                    isDashing = true;
                    dashCooldown = 60; // 1 second cooldown
                    vx = (right ? 1 : -1) * DASH_SPEED;
                }

                if (isDashing) {
                    vx *= 0.9;
                    if (Math.abs(vx) < 1) {
                        isDashing = false;
                        vx = 0;
                    }
                } else {
                    if (left) {
                        vx = -moveSpeed;
                        isMoving = true;
                        animationState = 1;
                    } else if (right) {
                        vx = moveSpeed;
                        isMoving = true;
                        animationState = 1;
                    } else {
                        vx = 0;
                        isMoving = false;
                        animationState = 0;
                    }

                    if (jump) {
                        vy = jumpForce;
                        isJumping = true;
                        animationState = 2;
                    }
                }

                x += vx;

                // Boundaries
                if (isPlayer1) {
                    if (x < 0) x = 0;
                    if (x > NET_X - width) x = NET_X - width;
                } else {
                    if (x < NET_X) x = NET_X;
                    if (x > COURT_WIDTH - width) x = COURT_WIDTH - width;
                }
            }

            frame++;
        }

        public void updateAI(Ball ball, Difficulty difficulty) {
            double reactionSpeed = 1.0;
            double accuracy = 1.0;

            switch(difficulty) {
                case EASY: reactionSpeed = 0.5; accuracy = 0.6; break;
                case NORMAL: reactionSpeed = 0.75; accuracy = 0.8; break;
                case HARD: reactionSpeed = 0.9; accuracy = 0.9; break;
                case EXTREME: reactionSpeed = 1.0; accuracy = 0.95; break;
            }

            double targetX = ball.x;
            boolean ballOnMySide = isPlayer1 ? ball.x < NET_X : ball.x > NET_X;
            boolean ballComingToMe = isPlayer1 ? ball.vx > 0 : ball.vx < 0;

            if (ballOnMySide || ballComingToMe) {
                // Add some randomness based on difficulty
                targetX += (Math.random() - 0.5) * (1 - accuracy) * 100;

                double center = x + width / 2;
                if (Math.abs(center - targetX) > 15 * reactionSpeed) {
                    if (targetX < center) {
                        x -= moveSpeed * reactionSpeed;
                        isMoving = true;
                    } else {
                        x += moveSpeed * reactionSpeed;
                        isMoving = true;
                    }
                } else {
                    isMoving = false;
                }

                // Jump logic
                if (ball.y > COURT_HEIGHT - 180 &&
                        Math.abs(center - targetX) < 70 &&
                        !isJumping && Math.random() > 0.1 * (1 - accuracy)) {
                    vy = jumpForce;
                    isJumping = true;
                    animationState = 2;
                }
            } else {
                // Return to base position
                double baseX = isPlayer1 ? 200 : COURT_WIDTH - 245;
                if (Math.abs(x - baseX) > 5) {
                    x += (baseX > x) ? 2 : -2;
                    isMoving = true;
                } else {
                    isMoving = false;
                }
            }

            // Gravity
            if (isJumping) {
                vy += GRAVITY;
                y += vy;
                if (y >= COURT_HEIGHT - height) {
                    y = COURT_HEIGHT - height;
                    vy = 0;
                    isJumping = false;
                    animationState = 0;
                }
            }

            // Boundaries
            if (isPlayer1) {
                if (x < 0) x = 0;
                if (x > NET_X - width) x = NET_X - width;
            } else {
                if (x < NET_X) x = NET_X;
                if (x > COURT_WIDTH - width) x = COURT_WIDTH - width;
            }

            frame++;
        }

        public void draw(Graphics2D g) {
            // Shadow
            g.setColor(new Color(0, 0, 0, 100));
            g.fillOval((int)x + 5, COURT_HEIGHT - 10, (int)width, 10);

            // Shield effect
            if (hasShield) {
                g.setColor(new Color(0, 255, 0, 100));
                g.setStroke(new BasicStroke(3));
                g.drawOval((int)x - 10, (int)y - 30, (int)width + 20, (int)height + 40);
            }

            // Body
            g.setColor(teamColor);
            g.fillRoundRect((int)x, (int)y, (int)width, (int)height, 10, 10);

            // Jersey stripes
            g.setColor(new Color(teamColor.getRed(), teamColor.getGreen(),
                    teamColor.getBlue(), 150));
            for (int i = 0; i < 3; i++) {
                g.fillRect((int)x + 5, (int)y + 20 + i * 15, (int)width - 10, 3);
            }

            // Head
            g.setColor(new Color(255, 200, 150));
            g.fillOval((int)(x + 12), (int)(y - 25), 22, 22);

            // Hair
            g.setColor(new Color(60, 40, 20));
            g.fillArc((int)(x + 10), (int)(y - 30), 26, 15, 0, 180);

            // Eyes
            g.setColor(Color.WHITE);
            int eyeOffset = isPlayer1 ? 14 : 6;
            g.fillOval((int)(x + eyeOffset), (int)(y - 18), 6, 6);
            g.setColor(Color.BLACK);
            g.fillOval((int)(x + eyeOffset + (isPlayer1 ? 2 : 0)), (int)(y - 16), 3, 3);

            // Number
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString(String.valueOf(number), (int)(x + 15), (int)(y + 45));

            // Dash effect
            if (isDashing) {
                g.setColor(new Color(255, 255, 255, 150));
                for (int i = 1; i <= 3; i++) {
                    int offset = isPlayer1 ? -i * 10 : i * 10;
                    g.fillRect((int)x + offset, (int)y + 5, (int)width, (int)height - 10);
                }
            }

            // Name tag
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRoundRect((int)x - 5, (int)y - 45, (int)width + 10, 20, 5, 5);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            int nameWidth = fm.stringWidth(name);
            g.drawString(name, (int) ((int)x + (width - nameWidth) / 2), (int)y - 32);
        }

        public Rectangle getBounds() {
            return new Rectangle((int)x - 5, (int)y - 30, (int)width + 10, (int)height + 30);
        }
    }

    // ==================== STATISTICS TRACKER ====================
    static class Statistics {
        int totalMatches = 0;
        int player1Wins = 0;
        int player2Wins = 0;
        int totalRallies = 0;
        int longestRally = 0;
        int totalPoints = 0;
        int aces = 0;
        int blocks = 0;
        long totalPlayTime = 0;
        long sessionStartTime = 0;

        public Statistics() {
            sessionStartTime = System.currentTimeMillis();
        }

        public void recordMatch(boolean player1Won, int rallies, int points) {
            totalMatches++;
            if (player1Won) player1Wins++;
            else player2Wins++;

            totalRallies += rallies;
            if (rallies > longestRally) longestRally = rallies;
            totalPoints += points;
        }

        public void draw(Graphics2D g, int x, int y) {
            g.setColor(new Color(0, 0, 0, 200));
            g.fillRoundRect(x, y, 250, 200, 10, 10);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("📊 MATCH STATISTICS", x + 15, y + 30);

            g.setFont(new Font("Arial", Font.PLAIN, 14));
            int lineY = y + 55;
            int lineHeight = 20;

            g.drawString("Matches: " + totalMatches, x + 15, lineY);
            g.drawString("P1 Wins: " + player1Wins, x + 15, lineY + lineHeight);
            g.drawString("P2 Wins: " + player2Wins, x + 15, lineY + lineHeight * 2);
            g.drawString("Longest Rally: " + longestRally, x + 15, lineY + lineHeight * 3);
            g.drawString("Total Points: " + totalPoints, x + 15, lineY + lineHeight * 4);

            long playTime = (System.currentTimeMillis() - sessionStartTime) / 1000;
            g.drawString("Play Time: " + playTime + "s", x + 15, lineY + lineHeight * 5);
        }
    }

    // ==================== REPLAY SYSTEM ====================
    static class ReplaySystem {
        List<ReplayFrame> frames = new ArrayList<>();
        boolean isRecording = false;
        boolean isPlaying = false;
        int currentFrame = 0;

        static class ReplayFrame {
            double ballX, ballY, ballVX, ballVY;
            double p1X, p1Y, p2X, p2Y;
            int score1, score2;

            public ReplayFrame(Ball ball, Player p1, Player p2, int s1, int s2) {
                this.ballX = ball.x;
                this.ballY = ball.y;
                this.ballVX = ball.vx;
                this.ballVY = ball.vy;
                this.p1X = p1.x;
                this.p1Y = p1.y;
                this.p2X = p2.x;
                this.p2Y = p2.y;
                this.score1 = s1;
                this.score2 = s2;
            }
        }

        public void startRecording() {
            frames.clear();
            isRecording = true;
        }

        public void recordFrame(Ball ball, Player p1, Player p2, int s1, int s2) {
            if (isRecording) {
                frames.add(new ReplayFrame(ball, p1, p2, s1, s2));
            }
        }

        public void stopRecording() {
            isRecording = false;
        }

        public void playReplay() {
            if (!frames.isEmpty()) {
                isPlaying = true;
                currentFrame = 0;
            }
        }

        public ReplayFrame getCurrentFrame() {
            if (currentFrame < frames.size()) {
                return frames.get(currentFrame++);
            }
            isPlaying = false;
            return null;
        }

        public int getTotalFrames() {
            return frames.size();
        }
    }

    // ==================== SOUND EFFECTS (VISUAL) ====================
    static class SoundEffect {
        String type;
        double x, y;
        int life;
        Color color;

        public static final String HIT = "HIT";
        public static final String ACE = "ACE";
        public static final String BLOCK = "BLOCK";
        public static final String WHISTLE = "WHISTLE";

        public SoundEffect(String type, double x, double y) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.life = 30;

            switch(type) {
                case HIT: color = Color.YELLOW; break;
                case ACE: color = Color.GRAY; break;
                case BLOCK: color = Color.CYAN; break;
                case WHISTLE: color = Color.WHITE; break;
                default: color = Color.WHITE;
            }
        }

        public void update() {
            life--;
        }

        public void draw(Graphics2D g) {
            if (life <= 0) return;

            float alpha = (float)life / 30;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int)(alpha * 255)));

            g.setFont(new Font("Arial", Font.BOLD, 20));
            FontMetrics fm = g.getFontMetrics();
            int width = fm.stringWidth(type);
            g.drawString(type, (int)(x - width / 2), (int)y);

            // Sound waves
            g.setStroke(new BasicStroke(2));
            int radius = (30 - life) * 2;
            g.drawOval((int)(x - radius), (int)(y - radius), radius * 2, radius * 2);
        }

        public boolean isAlive() {
            return life > 0;
        }
    }

    // ==================== MAIN GAME PANEL ====================
    class GamePanel extends JPanel implements ActionListener, KeyListener {

        private Timer timer;
        private GameState state = GameState.MAIN_MENU;
        private GameMode gameMode = GameMode.QUICK_MATCH;
        private Difficulty difficulty = Difficulty.NORMAL;

        private Ball ball;
        private Player player1;
        private Player player2;
        private List<Particle> particles;
        private List<PowerUp> powerUps;
        private List<SoundEffect> soundEffects;
        private Statistics statistics;
        private ReplaySystem replay;

        private int score1 = 0, score2 = 0;
        private int serveTurn = 1;
        private String commentary = "Welcome to Volleyball Championship!";
        private int rallyCount = 0;
        private long pointStartTime = 0;

        // Tournament mode
        private int tournamentRound = 0;
        private int tournamentWins = 0;
        private String[] opponents = {"CPU Easy", "CPU Normal", "CPU Hard", "CPU Extreme"};

        // Camera shake
        private double cameraShake = 0;
        private double cameraShakeDecay = 0.9;

        // Input state
        private boolean keyLeft = false, keyRight = false, keyJump = false, keyDash = false;
        private boolean keyPause = false, keyReplay = false;

        // Menu buttons
        private Rectangle btnQuickMatch, btnTournament, btnPractice, btnSettings, btnExit;
        private Rectangle btnEasy, btnNormal, btnHard, btnExtreme, btnBack;

        public GamePanel() {
            this.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
            this.setBackground(new Color(25, 25, 40));
            this.setFocusable(true);

            ball = new Ball();
            player1 = new Player(true, "Player 1", 10);
            player2 = new Player(false, "CPU", 7);
            particles = new ArrayList<>();
            powerUps = new ArrayList<>();
            soundEffects = new ArrayList<>();
            statistics = new Statistics();
            replay = new ReplaySystem();

            // Initialize menu buttons
            int btnWidth = 200, btnHeight = 50;
            int btnX = WINDOW_WIDTH / 2 - btnWidth / 2;
            btnQuickMatch = new Rectangle(btnX, 200, btnWidth, btnHeight);
            btnTournament = new Rectangle(btnX, 270, btnWidth, btnHeight);
            btnPractice = new Rectangle(btnX, 340, btnWidth, btnHeight);
            btnSettings = new Rectangle(btnX, 410, btnWidth, btnHeight);
            btnExit = new Rectangle(btnX, 480, btnWidth, btnHeight);

            btnEasy = new Rectangle(btnX, 200, btnWidth, btnHeight);
            btnNormal = new Rectangle(btnX, 270, btnWidth, btnHeight);
            btnHard = new Rectangle(btnX, 340, btnWidth, btnHeight);
            btnExtreme = new Rectangle(btnX, 410, btnWidth, btnHeight);
            btnBack = new Rectangle(btnX, 500, btnWidth, btnHeight);

            timer = new Timer(1000 / FPS, this);
            timer.start();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            update();
            repaint();
        }

        private void update() {
            if (state == GameState.MAIN_MENU || state == GameState.MODE_SELECT ||
                    state == GameState.SETTINGS || state == GameState.PAUSED) {
                return;
            }

            // Camera shake decay
            if (cameraShake > 0.1) {
                cameraShake *= cameraShakeDecay;
            } else {
                cameraShake = 0;
            }

            // Update entities
            player1.updateInput(keyLeft, keyRight, keyJump, keyDash);
            player2.updateAI(ball, difficulty);
            ball.update(1.0);

            // Collision detection
            checkPlayerBallCollision(player1);
            checkPlayerBallCollision(player2);
            checkNetCollision();
            checkPowerUpCollision();

            // Game logic
            checkScoring();

            // Spawn power-ups randomly
            if (Math.random() < 0.001 && powerUps.size() < 2) {
                double px = 100 + Math.random() * (COURT_WIDTH - 200);
                String[] types = {PowerUp.SPEED_BOOST, PowerUp.POWER_HIT,
                        PowerUp.SLOW_MO, PowerUp.SHIELD};
                powerUps.add(new PowerUp(px, 200, types[(int)(Math.random() * types.length)]));
            }

            // Update particles
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.update();
                if (!p.isAlive()) particles.remove(i);
            }

            // Update sound effects
            for (int i = soundEffects.size() - 1; i >= 0; i--) {
                SoundEffect s = soundEffects.get(i);
                s.update();
                if (!s.isAlive()) soundEffects.remove(i);
            }

            // Update power-ups
            for (int i = powerUps.size() - 1; i >= 0; i--) {
                PowerUp p = powerUps.get(i);
                if (p.isExpired()) powerUps.remove(i);
            }

            // Record replay
            if (state == GameState.PLAYING) {
                replay.recordFrame(ball, player1, player2, score1, score2);
            }
        }

        private void checkPlayerBallCollision(Player p) {
            Rectangle pBounds = p.getBounds();
            Rectangle bBounds = ball.getBounds();

            if (pBounds.intersects(bBounds)) {
                double playerCenterX = p.x + p.width / 2;
                double hitOffset = ball.x - playerCenterX;
                double angle = hitOffset / (p.width / 2);

                double hitPower = 13 * p.hitPower;
                if (p.isJumping) {
                    hitPower = 17 * p.hitPower; // Spike
                    p.spikes++;
                    createParticles(ball.x, ball.y, Color.ORANGE, 15);
                    cameraShake = 5;
                    soundEffects.add(new SoundEffect(SoundEffect.HIT, ball.x, ball.y - 30));
                } else {
                    soundEffects.add(new SoundEffect(SoundEffect.HIT, ball.x, ball.y - 30));
                }

                ball.vx = angle * hitPower;
                ball.vy = -9 - Math.random() * 3;
                ball.rotationSpeed = ball.vx * 3;

                p.hits++;
                createParticles(ball.x, ball.y, Color.YELLOW, 8);
                rallyCount++;
                commentary = "Hit! Rally: " + rallyCount;
            }
        }

        private void checkNetCollision() {
            int netTopY = COURT_HEIGHT - NET_HEIGHT;

            if (ball.x + ball.radius > NET_X - 5 && ball.x - ball.radius < NET_X + 5) {
                if (ball.y + ball.radius > netTopY) {
                    if (ball.x < NET_X) {
                        ball.x = NET_X - 5 - ball.radius;
                        ball.vx = -Math.abs(ball.vx) * 0.5;
                    } else {
                        ball.x = NET_X + 5 + ball.radius;
                        ball.vx = Math.abs(ball.vx) * 0.5;
                    }
                    commentary = "Net Touch!";
                    createParticles(ball.x, ball.y, Color.WHITE, 5);
                }
            }
        }

        private void checkPowerUpCollision() {
            for (int i = powerUps.size() - 1; i >= 0; i--) {
                PowerUp p = powerUps.get(i);
                double dist = Math.sqrt(Math.pow(ball.x - p.x, 2) + Math.pow(ball.y - p.y, 2));

                if (dist < 30) {
                    // Give power-up to the player who last hit the ball
                    Player target = ball.vx > 0 ? player1 : player2;
                    target.activatePowerUp(p.type);
                    commentary = target.name + " got " + p.type + "!";
                    powerUps.remove(i);
                    createParticles(p.x, p.y, p.color, 20);
                }
            }
        }

        private void checkScoring() {
            if (ball.y + ball.radius >= COURT_HEIGHT) {
                handlePointEnd();
            }
        }

        private void handlePointEnd() {
            state = GameState.POINT_OVER;

            boolean p1Scored = ball.x > NET_X;
            if (ball.x < NET_X) p1Scored = false;

            if (p1Scored) {
                score1++;
                commentary = "Player 1 Scores!";
                serveTurn = 1;
                player1.aces++;
                soundEffects.add(new SoundEffect(SoundEffect.ACE, NET_X, COURT_HEIGHT / 2));
            } else {
                score2++;
                commentary = "Player 2 Scores!";
                serveTurn = 2;
                player2.aces++;
                soundEffects.add(new SoundEffect(SoundEffect.ACE, NET_X, COURT_HEIGHT / 2));
            }

            long pointTime = (System.currentTimeMillis() - pointStartTime) / 1000;
            statistics.recordMatch(p1Scored, rallyCount, score1 + score2);

            if (score1 >= 5 || score2 >= 5) {
                state = GameState.GAME_OVER;
                commentary = (score1 >= 5 ? "Player 1" : "Player 2") + " Wins!";
                soundEffects.add(new SoundEffect(SoundEffect.WHISTLE, WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2));
            } else {
                Timer resetTimer = new Timer(1500, evt -> {
                    resetRound();
                    ((Timer)evt.getSource()).stop();
                });
                resetTimer.setRepeats(false);
                resetTimer.start();
            }
        }

        private void resetRound() {
            ball.reset();
            player1.reset();
            player2.reset();
            player1.resetPowerUps();
            player2.resetPowerUps();
            rallyCount = 0;
            powerUps.clear();
            pointStartTime = System.currentTimeMillis();

            if (serveTurn == 1) {
                ball.serve(true, 10 + Math.random() * 3);
                commentary = "Player 1 Serving...";
            } else {
                ball.serve(false, 10 + Math.random() * 3);
                commentary = "Player 2 Serving...";
            }

            state = GameState.PLAYING;
            replay.startRecording();
        }

        private void createParticles(double x, double y, Color color, int count) {
            for (int i = 0; i < count; i++) {
                particles.add(new Particle(x, y, color, 3 + Math.random() * 4));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Apply camera shake
            if (cameraShake > 0) {
                g2d.translate((Math.random() - 0.5) * cameraShake,
                        (Math.random() - 0.5) * cameraShake);
            }

            // Draw based on game state
            switch(state) {
                case MAIN_MENU:
                    drawMainMenu(g2d);
                    break;
                case MODE_SELECT:
                case SETTINGS:
                    drawSettingsMenu(g2d);
                    break;
                case PLAYING:
                case SERVING:
                case POINT_OVER:
                    drawGame(g2d);
                    break;
                case GAME_OVER:
                    drawGame(g2d);
                    drawGameOver(g2d);
                    break;
                case PAUSED:
                    drawGame(g2d);
                    drawPauseMenu(g2d);
                    break;
            }
        }

        private void drawMainMenu(Graphics2D g2d) {
            // Background gradient
            GradientPaint gp = new GradientPaint(0, 0, new Color(30, 30, 60),
                    0, WINDOW_HEIGHT, new Color(20, 20, 40));
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

            // Title
            g2d.setColor(new Color(255, 215, 0));
            g2d.setFont(new Font("Arial", Font.BOLD, 60));
            String title = "🏐 VOLLEYBALL CHAMPIONSHIP 🏐";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, (WINDOW_WIDTH - fm.stringWidth(title)) / 2, 120);

            // Subtitle
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            String subtitle = "Advanced Physics Simulation";
            fm = g2d.getFontMetrics();
            g2d.drawString(subtitle, (WINDOW_WIDTH - fm.stringWidth(subtitle)) / 2, 160);

            // Menu buttons
            drawButton(g2d, btnQuickMatch, "Quick Match", state == GameState.MAIN_MENU);
            drawButton(g2d, btnTournament, "Tournament", state == GameState.MAIN_MENU);
            drawButton(g2d, btnPractice, "Practice Mode", state == GameState.MAIN_MENU);
            drawButton(g2d, btnSettings, "Settings", state == GameState.MAIN_MENU);
            drawButton(g2d, btnExit, "Exit", state == GameState.MAIN_MENU);

            // Statistics
            statistics.draw(g2d, WINDOW_WIDTH - 270, 20);

            // Controls info
            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            g2d.drawString("Controls: Arrow Keys to Move/Jump, SPACE to Select, P to Pause",
                    20, WINDOW_HEIGHT - 20);
        }

        private void drawSettingsMenu(Graphics2D g2d) {
            // Background
            g2d.setColor(new Color(25, 25, 40));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            String title = "Select Difficulty";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, (WINDOW_WIDTH - fm.stringWidth(title)) / 2, 120);

            drawButton(g2d, btnEasy, "Easy", true);
            drawButton(g2d, btnNormal, "Normal", true);
            drawButton(g2d, btnHard, "Hard", true);
            drawButton(g2d, btnExtreme, "Extreme", true);
            drawButton(g2d, btnBack, "Back", true);

            // Current selection
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.PLAIN, 18));
            g2d.drawString("Current: " + difficulty, WINDOW_WIDTH / 2 - 50, 550);
        }

        private void drawGame(Graphics2D g2d) {
            // Draw court
            drawCourt(g2d);

            // Draw entities
            player1.draw(g2d);
            player2.draw(g2d);
            ball.draw(g2d);

            // Draw particles
            for (Particle p : particles) p.draw(g2d);

            // Draw power-ups
            for (PowerUp p : powerUps) p.draw(g2d);

            // Draw sound effects
            for (SoundEffect s : soundEffects) s.draw(g2d);

            // Draw UI
            drawUI(g2d);
        }

        private void drawCourt(Graphics2D g2d) {
            // Court floor with gradient
            GradientPaint gp = new GradientPaint(0, 0, new Color(50, 70, 100),
                    0, COURT_HEIGHT, new Color(40, 60, 90));
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, COURT_WIDTH, COURT_HEIGHT);

            // Court lines
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(0, 0, COURT_WIDTH, COURT_HEIGHT);
            g2d.drawLine(NET_X, 0, NET_X, COURT_HEIGHT);

            // Attack lines
            int attackOffset = 180;
            g2d.drawLine(NET_X - attackOffset, 0, NET_X - attackOffset, COURT_HEIGHT);
            g2d.drawLine(NET_X + attackOffset, 0, NET_X + attackOffset, COURT_HEIGHT);

            // Center circle
            g2d.drawOval(NET_X - 30, COURT_HEIGHT / 2 - 30, 60, 60);

            // Net
            g2d.setColor(new Color(220, 220, 220));
            g2d.setStroke(new BasicStroke(6));
            g2d.drawLine(NET_X, COURT_HEIGHT - NET_HEIGHT, NET_X, COURT_HEIGHT);

            // Net mesh
            g2d.setStroke(new BasicStroke(1));
            for (int i = 0; i < NET_HEIGHT; i += 12) {
                g2d.drawLine(NET_X - 12, COURT_HEIGHT - i, NET_X + 12, COURT_HEIGHT - i);
            }
            for (int i = -12; i <= 12; i += 6) {
                g2d.drawLine(NET_X + i, COURT_HEIGHT - NET_HEIGHT, NET_X + i, COURT_HEIGHT);
            }

            // Audience (decorative)
            g2d.setColor(new Color(40, 40, 60));
            g2d.fillRect(0, COURT_HEIGHT, WINDOW_WIDTH, WINDOW_HEIGHT - COURT_HEIGHT);

            // Crowd dots
            Random rand = new Random(42);
            for (int i = 0; i < 100; i++) {
                int x = rand.nextInt(WINDOW_WIDTH);
                int y = COURT_HEIGHT + 10 + rand.nextInt(WINDOW_HEIGHT - COURT_HEIGHT - 20);
                g2d.setColor(new Color(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255)));
                g2d.fillOval(x, y, 4, 4);
            }
        }

        private void drawUI(Graphics2D g2d) {
            // Scoreboard
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRoundRect(WINDOW_WIDTH / 2 - 180, 15, 360, 70, 15, 15);

            g2d.setColor(player1.teamColor);
            g2d.setFont(new Font("Arial", Font.BOLD, 35));
            g2d.drawString("P1: " + score1, WINDOW_WIDTH / 2 - 160, 60);

            g2d.setColor(player2.teamColor);
            g2d.drawString("P2: " + score2, WINDOW_WIDTH / 2 + 60, 60);

            // Rally counter
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString("Rally: " + rallyCount, WINDOW_WIDTH / 2 - 40, 100);

            // Commentary
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, WINDOW_HEIGHT - 50, WINDOW_WIDTH, 50);
            g2d.setColor(Color.CYAN);
            g2d.setFont(new Font("Courier", Font.BOLD, 16));
            g2d.drawString("📢 " + commentary, 20, WINDOW_HEIGHT - 20);

            // Player stats
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRoundRect(10, 10, 200, 100, 10, 10);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            g2d.drawString("Hits: " + player1.hits, 20, 35);
            g2d.drawString("Spikes: " + player1.spikes, 20, 55);
            g2d.drawString("Aces: " + player1.aces, 20, 75);
            g2d.drawString("Power: " + (int)(player1.hitPower * 100) + "%", 20, 95);

            // Timer
            long elapsed = (System.currentTimeMillis() - pointStartTime) / 1000;
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString("⏱ " + elapsed + "s", WINDOW_WIDTH - 100, 40);
        }

        private void drawGameOver(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

            Color GOLD = null;
            g2d.setColor(GOLD);
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            String msg = "GAME OVER";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(msg, (WINDOW_WIDTH - fm.stringWidth(msg)) / 2, WINDOW_HEIGHT / 2 - 50);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 30));
            msg = commentary;
            fm = g2d.getFontMetrics();
            g2d.drawString(msg, (WINDOW_WIDTH - fm.stringWidth(msg)) / 2, WINDOW_HEIGHT / 2 + 10);

            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            msg = "Press SPACE to Continue";
            fm = g2d.getFontMetrics();
            g2d.drawString(msg, (WINDOW_WIDTH - fm.stringWidth(msg)) / 2, WINDOW_HEIGHT / 2 + 60);
        }

        private void drawPauseMenu(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            String msg = "PAUSED";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(msg, (WINDOW_WIDTH - fm.stringWidth(msg)) / 2, WINDOW_HEIGHT / 2 - 30);

            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            msg = "Press P to Resume";
            fm = g2d.getFontMetrics();
            g2d.drawString(msg, (WINDOW_WIDTH - fm.stringWidth(msg)) / 2, WINDOW_HEIGHT / 2 + 20);
        }

        private void drawButton(Graphics2D g2d, Rectangle btn, String text, boolean clickable) {
            if (clickable) {
                GradientPaint gp = new GradientPaint(0, btn.y, new Color(60, 60, 100),
                        0, btn.y + btn.height, new Color(40, 40, 80));
                g2d.setPaint(gp);
            } else {
                g2d.setColor(new Color(50, 50, 70));
            }

            g2d.fillRoundRect(btn.x, btn.y, btn.width, btn.height, 10, 10);
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(btn.x, btn.y, btn.width, btn.height, 10, 10);

            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g2d.getFontMetrics();
            int textX = btn.x + (btn.width - fm.stringWidth(text)) / 2;
            int textY = btn.y + (btn.height + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(text, textX, textY);
        }

        private boolean isMouseOver(Rectangle rect, MouseEvent e) {
            return rect.contains(e.getPoint());
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (state == GameState.MAIN_MENU) {
                if (key == KeyEvent.VK_SPACE || key == KeyEvent.VK_ENTER) {
                    // Check which button is "selected" (simplified - just start quick match)
                    state = GameState.MODE_SELECT;
                }
                return;
            }

            if (state == GameState.MODE_SELECT || state == GameState.SETTINGS) {
                if (key == KeyEvent.VK_1) difficulty = Difficulty.EASY;
                if (key == KeyEvent.VK_2) difficulty = Difficulty.NORMAL;
                if (key == KeyEvent.VK_3) difficulty = Difficulty.HARD;
                if (key == KeyEvent.VK_4) difficulty = Difficulty.EXTREME;
                if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_B) {
                    state = GameState.MAIN_MENU;
                }
                if (key == KeyEvent.VK_SPACE) {
                    state = GameState.PLAYING;
                    resetRound();
                }
                return;
            }

            if (key == KeyEvent.VK_P) {
                if (state == GameState.PLAYING) {
                    state = GameState.PAUSED;
                } else if (state == GameState.PAUSED) {
                    state = GameState.PLAYING;
                }
            }

            if (key == KeyEvent.VK_SPACE) {
                if (state == GameState.GAME_OVER) {
                    score1 = 0;
                    score2 = 0;
                    state = GameState.MAIN_MENU;
                }
            }

            if (state == GameState.PLAYING || state == GameState.SERVING) {
                if (key == KeyEvent.VK_LEFT) keyLeft = true;
                if (key == KeyEvent.VK_RIGHT) keyRight = true;
                if (key == KeyEvent.VK_UP) keyJump = true;
                if (key == KeyEvent.VK_DOWN) keyDash = true;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int key = e.getKeyCode();
            if (key == KeyEvent.VK_LEFT) keyLeft = false;
            if (key == KeyEvent.VK_RIGHT) keyRight = false;
            if (key == KeyEvent.VK_UP) keyJump = false;
            if (key == KeyEvent.VK_DOWN) keyDash = false;
        }

        @Override
        public void keyTyped(KeyEvent e) {}

        private class GOLD {
        }
    }
}