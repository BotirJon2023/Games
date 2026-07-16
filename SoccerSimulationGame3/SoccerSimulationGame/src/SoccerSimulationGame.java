import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class SoccerSimulationGame extends JPanel implements ActionListener, KeyListener {

    // Game constants
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 700;
    private static final int FPS = 60;
    private static final double FRICTION = 0.97;
    private static final double BALL_FRICTION = 0.985;
    private static final int GOAL_WIDTH = 120;
    private static final int GOAL_HEIGHT = 300;
    private static final int PLAYER_RADIUS = 18;
    private static final int BALL_RADIUS = 12;

    // Game state
    private enum GameMode { MENU, TWO_PLAYER, VS_COMPUTER, PAUSED }
    private GameMode gameMode = GameMode.MENU;
    private javax.swing.Timer gameTimer;
    private boolean isPaused = false;
    private int scoreTeam1 = 0;
    private int scoreTeam2 = 0;
    private long gameStartTime;
    private long matchDuration = 120000; // 2 minutes

    // Entities
    private Ball ball;
    private Player player1;
    private Player player2;
    private Player computer;
    private List<Particle> particles;
    private List<Trail> ballTrail;
    private Goal leftGoal, rightGoal;

    // Input handling
    private Set<Integer> keysPressed;

    // Visual effects
    private float goalFlashAlpha = 0;
    private Point goalFlashPos;
    private Random random;
    private Font scoreFont, uiFont, titleFont;
    private GradientPaint fieldGradient;
    private TexturePaint grassTexture;

    public SoccerSimulationGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(34, 139, 34));
        setFocusable(true);
        addKeyListener(this);

        initGame();

        gameTimer = new javax.swing.Timer(1000 / FPS, this);
        gameTimer.start();
    }

    private void initGame() {
        random = new Random();
        keysPressed = new HashSet<>();
        particles = new ArrayList<>();
        ballTrail = new ArrayList<>();

        // Fonts
        titleFont = new Font("Arial", Font.BOLD, 48);
        scoreFont = new Font("Arial", Font.BOLD, 36);
        uiFont = new Font("Arial", Font.PLAIN, 18);

        // Field gradient
        fieldGradient = new GradientPaint(0, 0, new Color(34, 139, 34),
                0, HEIGHT, new Color(28, 115, 28));

        // Create grass texture
        BufferedImage grassImg = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = grassImg.createGraphics();
        g2.setColor(new Color(34, 139, 34));
        g2.fillRect(0, 0, 4, 4);
        g2.setColor(new Color(30, 125, 30));
        g2.fillRect(1, 1, 2, 2);
        g2.dispose();
        grassTexture = new TexturePaint(grassImg, new Rectangle(0, 0, 4, 4));

        resetGame();
    }

    private void resetGame() {
        ball = new Ball(WIDTH / 2.0, HEIGHT / 2.0);
        player1 = new Player(200, HEIGHT / 2.0, Color.BLUE, "P1");
        player2 = new Player(WIDTH - 200, HEIGHT / 2.0, Color.RED, "P2");
        computer = new Player(WIDTH - 200, HEIGHT / 2.0, Color.RED, "CPU");
        computer.isAI = true;

        leftGoal = new Goal(0, HEIGHT / 2 - GOAL_HEIGHT / 2, false);
        rightGoal = new Goal(WIDTH - 20, HEIGHT / 2 - GOAL_HEIGHT / 2, true);

        scoreTeam1 = 0;
        scoreTeam2 = 0;
        gameStartTime = System.currentTimeMillis();
        particles.clear();
        ballTrail.clear();
        goalFlashAlpha = 0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameMode == GameMode.MENU || isPaused) {
            repaint();
            return;
        }

        updateGame();
        repaint();
    }

    private void updateGame() {
        // Check time
        long elapsed = System.currentTimeMillis() - gameStartTime;
        if (elapsed > matchDuration) {
            gameMode = GameMode.MENU;
            return;
        }

        // Update players
        handleInput();
        player1.update();
        if (gameMode == GameMode.TWO_PLAYER) {
            player2.update();
        } else {
            computer.updateAI(ball, player1);
            computer.update();
        }

        // Update ball
        ball.update();

        // Update particles
        particles.removeIf(p -> !p.isAlive());
        particles.forEach(Particle::update);

        // Update ball trail
        ballTrail.add(new Trail(ball.x, ball.y));
        if (ballTrail.size() > 15) ballTrail.remove(0);

        // Update goal flash
        if (goalFlashAlpha > 0) {
            goalFlashAlpha -= 0.02f;
        }

        // Collisions
        checkCollisions();
        checkGoals();
        checkBoundaries();
    }

    private void handleInput() {
        // Player 1 - WASD
        player1.ax = 0; player1.ay = 0;
        if (keysPressed.contains(KeyEvent.VK_W)) player1.ay = -0.8;
        if (keysPressed.contains(KeyEvent.VK_S)) player1.ay = 0.8;
        if (keysPressed.contains(KeyEvent.VK_A)) player1.ax = -0.8;
        if (keysPressed.contains(KeyEvent.VK_D)) player1.ax = 0.8;
        if (keysPressed.contains(KeyEvent.VK_SPACE)) player1.kick(ball);

        // Player 2 - Arrows
        if (gameMode == GameMode.TWO_PLAYER) {
            player2.ax = 0; player2.ay = 0;
            if (keysPressed.contains(KeyEvent.VK_UP)) player2.ay = -0.8;
            if (keysPressed.contains(KeyEvent.VK_DOWN)) player2.ay = 0.8;
            if (keysPressed.contains(KeyEvent.VK_LEFT)) player2.ax = -0.8;
            if (keysPressed.contains(KeyEvent.VK_RIGHT)) player2.ax = 0.8;
            if (keysPressed.contains(KeyEvent.VK_ENTER)) player2.kick(ball);
        }
    }

    private void checkCollisions() {
        // Player-ball collision
        checkPlayerBallCollision(player1);
        if (gameMode == GameMode.TWO_PLAYER) {
            checkPlayerBallCollision(player2);
        } else {
            checkPlayerBallCollision(computer);
        }

        // Player-player collision
        Player other = gameMode == GameMode.TWO_PLAYER ? player2 : computer;
        double dx = other.x - player1.x;
        double dy = other.y - player1.y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < PLAYER_RADIUS * 2 && dist > 0) {
            double overlap = PLAYER_RADIUS * 2 - dist;
            double pushX = dx / dist * overlap / 2;
            double pushY = dy / dist * overlap / 2;
            player1.x -= pushX; player1.y -= pushY;
            other.x += pushX; other.y += pushY;

            // Bounce velocity
            double tempVx = player1.vx; double tempVy = player1.vy;
            player1.vx = other.vx * 0.5; player1.vy = other.vy * 0.5;
            other.vx = tempVx * 0.5; other.vy = tempVy * 0.5;
        }
    }

    private void checkPlayerBallCollision(Player p) {
        double dx = ball.x - p.x;
        double dy = ball.y - p.y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < PLAYER_RADIUS + BALL_RADIUS && dist > 0) {
            double nx = dx / dist;
            double ny = dy / dist;
            double relVelX = ball.vx - p.vx;
            double relVelY = ball.vy - p.vy;
            double speed = relVelX * nx + relVelY * ny;

            if (speed < 0) {
                ball.vx -= speed * nx * 1.5;
                ball.vy -= speed * ny * 1.5;
                ball.vx += p.vx * 0.3;
                ball.vy += p.vy * 0.3;

                // Push ball out
                double overlap = PLAYER_RADIUS + BALL_RADIUS - dist;
                ball.x += nx * overlap;
                ball.y += ny * overlap;

                createParticles(ball.x, ball.y, 5, Color.WHITE);
            }
        }
    }

    private void checkGoals() {
        // Left goal - Team 2 scores
        if (ball.x - BALL_RADIUS < 20 &&
                ball.y > HEIGHT / 2 - GOAL_HEIGHT / 2 &&
                ball.y < HEIGHT / 2 + GOAL_HEIGHT / 2) {
            scoreTeam2++;
            goalScored(new Point(50, HEIGHT / 2));
        }

        // Right goal - Team 1 scores
        if (ball.x + BALL_RADIUS > WIDTH - 20 &&
                ball.y > HEIGHT / 2 - GOAL_HEIGHT / 2 &&
                ball.y < HEIGHT / 2 + GOAL_HEIGHT / 2) {
            scoreTeam1++;
            goalScored(new Point(WIDTH - 50, HEIGHT / 2));
        }
    }

    private void goalScored(Point pos) {
        goalFlashAlpha = 1.0f;
        goalFlashPos = pos;
        createParticles(pos.x, pos.y, 30, Color.YELLOW);
        ball.reset(WIDTH / 2.0, HEIGHT / 2.0);
        player1.reset(200, HEIGHT / 2.0);
        if (gameMode == GameMode.TWO_PLAYER) {
            player2.reset(WIDTH - 200, HEIGHT / 2.0);
        } else {
            computer.reset(WIDTH - 200, HEIGHT / 2.0);
        }
    }

    private void checkBoundaries() {
        // Ball boundaries
        if (ball.y - BALL_RADIUS < 50 || ball.y + BALL_RADIUS > HEIGHT - 50) {
            ball.vy *= -0.8;
            ball.y = Math.max(BALL_RADIUS + 50, Math.min(HEIGHT - BALL_RADIUS - 50, ball.y));
            createParticles(ball.x, ball.y, 3, Color.WHITE);
        }
        if (ball.x - BALL_RADIUS < 0 || ball.x + BALL_RADIUS > WIDTH) {
            if (!(ball.y > HEIGHT / 2 - GOAL_HEIGHT / 2 && ball.y < HEIGHT / 2 + GOAL_HEIGHT / 2)) {
                ball.vx *= -0.8;
                ball.x = Math.max(BALL_RADIUS, Math.min(WIDTH - BALL_RADIUS, ball.x));
                createParticles(ball.x, ball.y, 3, Color.WHITE);
            }
        }

        // Player boundaries
        constrainPlayer(player1);
        if (gameMode == GameMode.TWO_PLAYER) constrainPlayer(player2);
        else constrainPlayer(computer);
    }

    private void constrainPlayer(Player p) {
        p.x = Math.max(PLAYER_RADIUS + 20, Math.min(WIDTH - PLAYER_RADIUS - 20, p.x));
        p.y = Math.max(PLAYER_RADIUS + 50, Math.min(HEIGHT - PLAYER_RADIUS - 50, p.y));
    }

    private void createParticles(double x, double y, int count, Color color) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawField(g2);

        if (gameMode == GameMode.MENU) {
            drawMenu(g2);
            return;
        }

        // Draw trails
        drawBallTrail(g2);

        // Draw goals
        leftGoal.draw(g2);
        rightGoal.draw(g2);

        // Draw particles
        particles.forEach(p -> p.draw(g2));

        // Draw entities
        ball.draw(g2);
        player1.draw(g2);
        if (gameMode == GameMode.TWO_PLAYER) {
            player2.draw(g2);
        } else {
            computer.draw(g2);
        }

        // Draw UI
        drawUI(g2);
        drawGoalFlash(g2);

        if (isPaused) drawPauseScreen(g2);
    }

    private void drawField(Graphics2D g2) {
        // Grass background
        g2.setPaint(fieldGradient);
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        g2.setPaint(grassTexture);
        g2.fillRect(0, 50, WIDTH, HEIGHT - 100);

        // Field lines
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));

        // Outer boundary
        g2.drawRect(20, 50, WIDTH - 40, HEIGHT - 100);

        // Center line
        g2.drawLine(WIDTH / 2, 50, WIDTH / 2, HEIGHT - 50);

        // Center circle
        g2.drawOval(WIDTH / 2 - 80, HEIGHT / 2 - 80, 160, 160);
        g2.fillOval(WIDTH / 2 - 5, HEIGHT / 2 - 5, 10, 10);

        // Penalty areas
        g2.drawRect(20, HEIGHT / 2 - 150, 180, 300);
        g2.drawRect(WIDTH - 200, HEIGHT / 2 - 150, 180, 300);

        // Goal areas
        g2.drawRect(20, HEIGHT / 2 - 80, 80, 160);
        g2.drawRect(WIDTH - 100, HEIGHT / 2 - 80, 80, 160);

        // Penalty spots
        g2.fillOval(140 - 4, HEIGHT / 2 - 4, 8, 8);
        g2.fillOval(WIDTH - 140 - 4, HEIGHT / 2 - 4, 8, 8);

        // Corner arcs
        g2.drawArc(10, 40, 20, 20, 270, 90);
        g2.drawArc(WIDTH - 30, 40, 20, 20, 180, 90);
        g2.drawArc(10, HEIGHT - 60, 20, 20, 0, 90);
        g2.drawArc(WIDTH - 30, HEIGHT - 60, 20, 20, 90, 90);
    }

    private void drawBallTrail(Graphics2D g2) {
        for (int i = 0; i < ballTrail.size(); i++) {
            Trail t = ballTrail.get(i);
            float alpha = (float) i / ballTrail.size() * 0.3f;
            g2.setColor(new Color(1f, 1f, alpha));
            int size = (int) (BALL_RADIUS * 2 * alpha);
            g2.fillOval((int) t.x - size / 2, (int) t.y - size / 2, size, size);
        }
    }

    private void drawUI(Graphics2D g2) {
        // Score
        g2.setFont(scoreFont);
        g2.setColor(Color.WHITE);
        String score = scoreTeam1 + " - " + scoreTeam2;
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(score, WIDTH / 2 - fm.stringWidth(score) / 2, 40);

        // Time
        long elapsed = System.currentTimeMillis() - gameStartTime;
        long remaining = Math.max(0, matchDuration - elapsed);
        String time = String.format("%02d:%02d", remaining / 60000, (remaining / 1000) % 60);
        g2.setFont(uiFont);
        g2.drawString(time, WIDTH / 2 - 25, 65);

        // Controls
        g2.setFont(uiFont);
        g2.setColor(new Color(255, 255, 255, 180));
        if (gameMode == GameMode.TWO_PLAYER) {
            g2.drawString("P1: WASD + SPACE | P2: Arrows + ENTER", 20, HEIGHT - 20);
        } else {
            g2.drawString("P1: WASD + SPACE | R: Reset | P: Pause", 20, HEIGHT - 20);
        }
    }

    private void drawGoalFlash(Graphics2D g2) {
        if (goalFlashAlpha > 0 && goalFlashPos != null) {
            g2.setColor(new Color(1f, 1f, 0f, goalFlashAlpha));
            g2.setFont(new Font("Arial", Font.BOLD, 72));
            String text = "GOAL!";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, goalFlashPos.x - fm.stringWidth(text) / 2, goalFlashPos.y);
        }
    }

    private void drawMenu(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setFont(titleFont);
        g2.setColor(Color.YELLOW);
        String title = "SOCCER SIMULATION";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, WIDTH / 2 - fm.stringWidth(title) / 2, 200);

        g2.setFont(uiFont);
        g2.setColor(Color.WHITE);
        String[] options = {
                "Press 1 - 2 Player Mode",
                "Press 2 - VS Computer",
                "",
                "Player 1: WASD to move, SPACE to kick",
                "Player 2: Arrow Keys to move, ENTER to kick",
                "R - Reset | P - Pause"
        };

        int y = 300;
        for (String opt : options) {
            fm = g2.getFontMetrics();
            g2.drawString(opt, WIDTH / 2 - fm.stringWidth(opt) / 2, y);
            y += 40;
        }

        if (scoreTeam1 > 0 || scoreTeam2 > 0) {
            g2.setColor(Color.CYAN);
            String result = "Final Score: " + scoreTeam1 + " - " + scoreTeam2;
            fm = g2.getFontMetrics();
            g2.drawString(result, WIDTH / 2 - fm.stringWidth(result) / 2, y + 40);
        }
    }

    private void drawPauseScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        g2.setFont(titleFont);
        g2.setColor(Color.WHITE);
        String text = "PAUSED";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, WIDTH / 2 - fm.stringWidth(text) / 2, HEIGHT / 2);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keysPressed.add(e.getKeyCode());

        if (e.getKeyCode() == KeyEvent.VK_1) {
            gameMode = GameMode.TWO_PLAYER;
            resetGame();
        } else if (e.getKeyCode() == KeyEvent.VK_2) {
            gameMode = GameMode.VS_COMPUTER;
            resetGame();
        } else if (e.getKeyCode() == KeyEvent.VK_R) {
            resetGame();
            gameMode = GameMode.MENU;
        } else if (e.getKeyCode() == KeyEvent.VK_P && gameMode != GameMode.MENU) {
            isPaused = !isPaused;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keysPressed.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner Classes

    class Player {
        double x, y, vx, vy, ax, ay;
        Color color;
        String name;
        boolean isAI = false;
        private double kickCooldown = 0;
        private double animFrame = 0;

        Player(double x, double y, Color color, String name) {
            this.x = x; this.y = y; this.color = color; this.name = name;
        }

        void reset(double x, double y) {
            this.x = x; this.y = y; vx = vy = ax = ay = 0;
        }

        void update() {
            // Apply acceleration
            vx += ax;
            vy += ay;

            // Apply friction
            vx *= FRICTION;
            vy *= FRICTION;

            // Max speed
            double speed = Math.sqrt(vx * vx + vy * vy);
            if (speed > 6) {
                vx = vx / speed * 6;
                vy = vy / speed * 6;
            }

            x += vx;
            y += vy;

            if (kickCooldown > 0) kickCooldown--;
            if (Math.abs(vx) > 0.1 || Math.abs(vy) > 0.1) {
                animFrame += 0.3;
            }
        }

        void updateAI(Ball ball, Player opponent) {
            // Simple AI: move toward ball, kick toward goal
            double dx = ball.x - x;
            double dy = ball.y - y;
            double distToBall = Math.sqrt(dx * dx + dy * dy);

            // Decide target
            double targetX, targetY;
            if (distToBall < 200) {
                // Go for ball
                targetX = ball.x;
                targetY = ball.y;
            } else {
                // Defensive position
                targetX = WIDTH - 300;
                targetY = ball.y;
            }

            // Move toward target
            double tdx = targetX - x;
            double tdy = targetY - y;
            double tdist = Math.sqrt(tdx * tdx + tdy * tdy);
            if (tdist > 5) {
                ax = tdx / tdist * 0.6;
                ay = tdy / tdist * 0.6;
            } else {
                ax = ay = 0;
            }

            // Kick if close to ball and facing goal
            if (distToBall < PLAYER_RADIUS + BALL_RADIUS + 10 && kickCooldown == 0) {
                kick(ball);
                // Aim toward opponent goal
                double goalX = 0;
                double goalY = HEIGHT / 2;
                double kdx = goalX - ball.x;
                double kdy = goalY - ball.y;
                double kdist = Math.sqrt(kdx * kdx + kdy * kdy);
                ball.vx += kdx / kdist * 3;
                ball.vy += kdy / kdist * 3;
            }
        }

        void kick(Ball ball) {
            if (kickCooldown > 0) return;
            double dx = ball.x - x;
            double dy = ball.y - y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < PLAYER_RADIUS + BALL_RADIUS + 15) {
                double nx = dx / dist;
                double ny = dy / dist;
                ball.vx += nx * 8;
                ball.vy += ny * 8;
                kickCooldown = 20;
                createParticles(ball.x, ball.y, 8, Color.ORANGE);
            }
        }

        void draw(Graphics2D g2) {
            // Shadow
            g2.setColor(new Color(0, 0, 0, 50));
            g2.fillOval((int) x - PLAYER_RADIUS, (int) y - PLAYER_RADIUS / 2 + PLAYER_RADIUS,
                    PLAYER_RADIUS * 2, PLAYER_RADIUS);

            // Body
            g2.setColor(color);
            g2.fillOval((int) x - PLAYER_RADIUS, (int) y - PLAYER_RADIUS,
                    PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);

            // Highlight
            g2.setColor(color.brighter());
            g2.fillOval((int) x - PLAYER_RADIUS / 2, (int) y - PLAYER_RADIUS / 2,
                    PLAYER_RADIUS, PLAYER_RADIUS);

            // Jersey number
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(name, (int) x - fm.stringWidth(name) / 2, (int) y + 5);

            // Direction indicator when moving
            if (Math.abs(vx) > 0.5 || Math.abs(vy) > 0.5) {
                double angle = Math.atan2(vy, vx);
                int x2 = (int) (x + Math.cos(angle) * PLAYER_RADIUS);
                int y2 = (int) (y + Math.sin(angle) * PLAYER_RADIUS);
                g2.setStroke(new BasicStroke(3));
                g2.setColor(Color.YELLOW);
                g2.drawLine((int) x, (int) y, x2, y2);
            }
        }
    }

    class Ball {
        double x, y, vx, vy;
        double rotation = 0;

        Ball(double x, double y) {
            this.x = x; this.y = y;
        }

        void reset(double x, double y) {
            this.x = x; this.y = y; vx = vy = 0;
        }

        void update() {
            x += vx;
            y += vy;
            vx *= BALL_FRICTION;
            vy *= BALL_FRICTION;
            rotation += Math.sqrt(vx * vx + vy * vy) * 0.1;

            if (Math.abs(vx) < 0.1) vx = 0;
            if (Math.abs(vy) < 0.1) vy = 0;
        }

        void draw(Graphics2D g2) {
            // Shadow
            g2.setColor(new Color(0, 0, 0, 50));
            g2.fillOval((int) x - BALL_RADIUS, (int) y - BALL_RADIUS / 2 + BALL_RADIUS,
                    BALL_RADIUS * 2, BALL_RADIUS);

            // Ball
            g2.setColor(Color.WHITE);
            g2.fillOval((int) x - BALL_RADIUS, (int) y - BALL_RADIUS,
                    BALL_RADIUS * 2, BALL_RADIUS * 2);

            // Pattern
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            AffineTransform old = g2.getTransform();
            g2.translate(x, y);
            g2.rotate(rotation);
            // Pentagon pattern
            for (int i = 0; i < 5; i++) {
                double angle = i * Math.PI * 2 / 5;
                int px = (int) (Math.cos(angle) * BALL_RADIUS / 2);
                int py = (int) (Math.sin(angle) * BALL_RADIUS / 2);
                g2.fillOval(px - 3, py - 3, 6, 6);
            }
            g2.setTransform(old);

            // Outline
            g2.setColor(Color.BLACK);
            g2.drawOval((int) x - BALL_RADIUS, (int) y - BALL_RADIUS,
                    BALL_RADIUS * 2, BALL_RADIUS * 2);
        }
    }

    class Goal {
        int x, y;
        boolean isRight;

        Goal(int x, int y, boolean isRight) {
            this.x = x; this.y = y; this.isRight = isRight;
        }

        void draw(Graphics2D g2) {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(6));
            // Posts
            g2.drawLine(x, y, x, y + GOAL_HEIGHT);
            g2.drawLine(x, y, x + (isRight ? -20 : 20), y);
            g2.drawLine(x, y + GOAL_HEIGHT, x + (isRight ? -20 : 20), y + GOAL_HEIGHT);

            // Net
            g2.setStroke(new BasicStroke(1));
            g2.setColor(new Color(255, 255, 255, 100));
            for (int i = 0; i <= 10; i++) {
                int ny = y + i * GOAL_HEIGHT / 10;
                g2.drawLine(x, ny, x + (isRight ? -20 : 20), ny);
            }
            for (int i = 0; i <= 3; i++) {
                int nx = x + (isRight ? -1 : 1) * i * 20 / 3;
                g2.drawLine(nx, y, nx, y + GOAL_HEIGHT);
            }
        }
    }

    class Particle {
        double x, y, vx, vy;
        int life;
        Color color;

        Particle(double x, double y, Color color) {
            this.x = x; this.y = y;
            this.color = color;
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = random.nextDouble() * 3 + 1;
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed;
            this.life = 30 + random.nextInt(20);
        }

        void update() {
            x += vx; y += vy;
            vy += 0.1; // gravity
            vx *= 0.98;
            life--;
        }

        boolean isAlive() { return life > 0; }

        void draw(Graphics2D g2) {
            float alpha = life / 50f;
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int) (alpha * 255)));
            g2.fillOval((int) x - 3, (int) y - 3, 6, 6);
        }
    }

    class Trail {
        double x, y;
        Trail(double x, double y) { this.x = x; this.y = y; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Soccer Simulation Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new SoccerSimulationGame());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}