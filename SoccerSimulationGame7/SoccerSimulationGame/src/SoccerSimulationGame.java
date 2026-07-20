import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class SoccerSimulationGame extends JPanel implements ActionListener, KeyListener {

    private static final int WIDTH = 1100;
    private static final int HEIGHT = 700;
    private static final int FIELD_X = 60;
    private static final int FIELD_Y = 60;
    private static final int FIELD_W = WIDTH - 120;
    private static final int FIELD_H = HEIGHT - 120;
    private static final int GOAL_H = 180;
    private static final int GOAL_DEPTH = 30;
    private static final double FRICTION = 0.985;
    private static final double BALL_FRICTION = 0.992;
    private static final double PLAYER_SPEED = 3.2;
    private static final double PLAYER_BOOST = 5.0;
    private static final double KICK_POWER = 12.0;
    private static final int GAME_DURATION = 120; // seconds
    private static final int TARGET_FPS = 60;

    // -----------------------------
    // Game state
    // -----------------------------
    private enum State { MENU, PLAYING, GOAL, GAMEOVER }
    private State state = State.MENU;
    private boolean vsComputer = true;

    private Player player1;
    private Player player2;
    private Ball ball;
    private AIController ai;
    private List<Particle> particles = new ArrayList<>();
    private List<Trail> trails = new ArrayList<>();

    private int score1 = 0;
    private int score2 = 0;
    private int timeLeft = GAME_DURATION;
    private long lastTick = System.currentTimeMillis();
    private long goalTimer = 0;
    private String lastScorer = "";

    private final Set<Integer> pressedKeys = new HashSet<>();
    private Timer gameTimer;
    private Random rng = new Random();

    // -----------------------------
    // Entry point
    // -----------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Soccer Simulation Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            SoccerSimulationGame game = new SoccerSimulationGame();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // -----------------------------
    // Constructor
    // -----------------------------
    public SoccerSimulationGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        Timer animator = new Timer(1000 / TARGET_FPS, this);
        animator.start();

        gameTimer = new Timer(1000, e -> {
            if (state == State.PLAYING) {
                timeLeft--;
                if (timeLeft <= 0) {
                    state = State.GAMEOVER;
                }
            }
        });
        gameTimer.start();
    }

    // -----------------------------
    // Game setup
    // -----------------------------
    private void startGame(boolean computer) {
        vsComputer = computer;
        score1 = 0;
        score2 = 0;
        timeLeft = GAME_DURATION;
        particles.clear();
        trails.clear();

        int midY = FIELD_Y + FIELD_H / 2;
        int p1X = FIELD_X + 120;
        int p2X = FIELD_X + FIELD_W - 120;

        player1 = new Player(p1X, midY, Color.RED, new Color(255, 90, 90), "P1");
        player2 = new Player(p2X, midY, Color.BLUE, new Color(90, 140, 255), "P2");

        ball = new Ball(WIDTH / 2, HEIGHT / 2);
        ai = new AIController(player2, ball, this);
        state = State.PLAYING;
    }

    private void resetAfterGoal() {
        int midY = FIELD_Y + FIELD_H / 2;
        player1.setPosition(FIELD_X + 120, midY);
        player2.setPosition(FIELD_X + FIELD_W - 120, midY);
        ball.reset(WIDTH / 2, HEIGHT / 2);
        player1.setVelocity(0, 0);
        player2.setVelocity(0, 0);
    }

    // -----------------------------
    // Game loop
    // -----------------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.currentTimeMillis();
        double dt = (now - lastTick) / 1000.0;
        lastTick = now;

        update(dt);
        repaint();
    }

    private void update(double dt) {
        if (state == State.PLAYING) {
            handleInput();
            if (vsComputer) {
                ai.update(dt);
            }
            player1.update();
            player2.update();
            ball.update();
            constrainPlayer(player1);
            constrainPlayer(player2);
            handleCollisions();
            checkGoal();
            updateTrails();
        } else if (state == State.GOAL) {
            if (System.currentTimeMillis() - goalTimer > 1800) {
                resetAfterGoal();
                state = State.PLAYING;
            }
        }
        updateParticles();
    }

    // -----------------------------
    // Input
    // -----------------------------
    private void handleInput() {
        double dx1 = 0, dy1 = 0;
        if (pressedKeys.contains(KeyEvent.VK_A)) dx1 -= 1;
        if (pressedKeys.contains(KeyEvent.VK_D)) dx1 += 1;
        if (pressedKeys.contains(KeyEvent.VK_W)) dy1 -= 1;
        if (pressedKeys.contains(KeyEvent.VK_S)) dy1 += 1;
        double len1 = Math.hypot(dx1, dy1);
        if (len1 > 0) { dx1 /= len1; dy1 /= len1; }
        player1.applyForce(dx1 * PLAYER_SPEED, dy1 * PLAYER_SPEED);
        if (pressedKeys.contains(KeyEvent.VK_SPACE)) {
            tryKick(player1);
        }

        if (!vsComputer) {
            double dx2 = 0, dy2 = 0;
            if (pressedKeys.contains(KeyEvent.VK_LEFT))  dx2 -= 1;
            if (pressedKeys.contains(KeyEvent.VK_RIGHT)) dx2 += 1;
            if (pressedKeys.contains(KeyEvent.VK_UP))    dy2 -= 1;
            if (pressedKeys.contains(KeyEvent.VK_DOWN))  dy2 += 1;
            double len2 = Math.hypot(dx2, dy2);
            if (len2 > 0) { dx2 /= len2; dy2 /= len2; }
            player2.applyForce(dx2 * PLAYER_SPEED, dy2 * PLAYER_SPEED);
            if (pressedKeys.contains(KeyEvent.VK_ENTER)) {
                tryKick(player2);
            }
        }
    }

    private void tryKick(Player p) {
        double dx = ball.getX() - p.getX();
        double dy = ball.getY() - p.getY();
        double dist = Math.hypot(dx, dy);
        if (dist < p.getRadius() + ball.getRadius() + 10) {
            double len = Math.max(dist, 0.0001);
            double nx = dx / len;
            double ny = dy / len;
            ball.setVelocity(nx * KICK_POWER + p.getVX() * 0.4,
                    ny * KICK_POWER + p.getVY() * 0.4);
            spawnKickParticles(ball.getX(), ball.getY(), p.getColor());
        }
    }

    // -----------------------------
    // Physics helpers
    // -----------------------------
    private void constrainPlayer(Player p) {
        int r = p.getRadius();
        if (p.getX() - r < FIELD_X) { p.setX(FIELD_X + r); p.setVX(0); }
        if (p.getX() + r > FIELD_X + FIELD_W) { p.setX(FIELD_X + FIELD_W - r); p.setVX(0); }
        if (p.getY() - r < FIELD_Y) { p.setY(FIELD_Y + r); p.setVY(0); }
        if (p.getY() + r > FIELD_Y + FIELD_H) { p.setY(FIELD_Y + FIELD_H - r); p.setVY(0); }
    }

    private void handleCollisions() {
        collidePlayerBall(player1);
        collidePlayerBall(player2);
        collidePlayers();
    }

    private void collidePlayerBall(Player p) {
        double dx = ball.getX() - p.getX();
        double dy = ball.getY() - p.getY();
        double dist = Math.hypot(dx, dy);
        double minDist = p.getRadius() + ball.getRadius();
        if (dist < minDist && dist > 0.0001) {
            double nx = dx / dist;
            double ny = dy / dist;
            double overlap = minDist - dist;
            ball.setX(ball.getX() + nx * overlap);
            ball.setY(ball.getY() + ny * overlap);

            double relVx = ball.getVX() - p.getVX();
            double relVy = ball.getVY() - p.getVY();
            double dot = relVx * nx + relVy * ny;
            if (dot < 0) {
                ball.setVelocity(ball.getVX() - 1.6 * dot * nx + p.getVX() * 0.3,
                        ball.getVY() - 1.6 * dot * ny + p.getVY() * 0.3);
            }
        }
    }

    private void collidePlayers() {
        double dx = player2.getX() - player1.getX();
        double dy = player2.getY() - player1.getY();
        double dist = Math.hypot(dx, dy);
        double minDist = player1.getRadius() + player2.getRadius();
        if (dist < minDist && dist > 0.0001) {
            double nx = dx / dist;
            double ny = dy / dist;
            double overlap = minDist - dist;
            player1.setX(player1.getX() - nx * overlap / 2);
            player1.setY(player1.getY() - ny * overlap / 2);
            player2.setX(player2.getX() + nx * overlap / 2);
            player2.setY(player2.getY() + ny * overlap / 2);
        }
    }

    private void checkGoal() {
        int midY = FIELD_Y + FIELD_H / 2;
        int goalTop = midY - GOAL_H / 2;
        int goalBot = midY + GOAL_H / 2;

        // Left goal (scored by player2)
        if (ball.getX() - ball.getRadius() < FIELD_X &&
                ball.getY() > goalTop && ball.getY() < goalBot) {
            score2++;
            lastScorer = vsComputer ? "Computer" : "Player 2";
            triggerGoal();
        }
        // Right goal (scored by player1)
        if (ball.getX() + ball.getRadius() > FIELD_X + FIELD_W &&
                ball.getY() > goalTop && ball.getY() < goalBot) {
            score1++;
            lastScorer = "Player 1";
            triggerGoal();
        }
    }

    private void triggerGoal() {
        state = State.GOAL;
        goalTimer = System.currentTimeMillis();
        spawnGoalExplosion(ball.getX(), ball.getY());
    }

    // -----------------------------
    // Particles / trails
    // -----------------------------
    private void spawnKickParticles(double x, double y, Color c) {
        for (int i = 0; i < 10; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double sp = 1 + rng.nextDouble() * 3;
            particles.add(new Particle(x, y, Math.cos(ang) * sp, Math.sin(ang) * sp, c, 30));
        }
    }

    private void spawnGoalExplosion(double x, double y) {
        Color[] colors = { Color.YELLOW, Color.ORANGE, Color.WHITE, Color.RED };
        for (int i = 0; i < 120; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double sp = 2 + rng.nextDouble() * 7;
            Color c = colors[rng.nextInt(colors.length)];
            particles.add(new Particle(x, y, Math.cos(ang) * sp, Math.sin(ang) * sp, c, 60 + rng.nextInt(40)));
        }
    }

    private void updateParticles() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update();
            if (p.getLife() <= 0) it.remove();
        }
    }

    private void updateTrails() {
        trails.add(new Trail(ball.getX(), ball.getY(), new Color(255, 255, 255, 120)));
        Iterator<Trail> it = trails.iterator();
        while (it.hasNext()) {
            Trail t = it.next();
            t.age();
            if (t.getLife() <= 0) it.remove();
        }
    }

    // -----------------------------
    // Rendering
    // -----------------------------
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g);

        if (state == State.MENU) {
            drawMenu(g);
        } else {
            drawField(g);
            drawTrails(g);
            drawGoals(g);
            drawPlayers(g);
            drawBall(g);
            drawParticles(g);
            drawHUD(g);
            if (state == State.GOAL) drawGoalBanner(g);
            if (state == State.GAMEOVER) drawGameOver(g);
        }
    }

    private void drawBackground(Graphics2D g) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(20, 30, 60),
                0, HEIGHT, new Color(5, 10, 25));
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Stadium lights (simple glow)
        for (int i = 0; i < 4; i++) {
            int x = 100 + i * 300;
            RadialGradientPaint glow = new RadialGradientPaint(
                    x, 30, 80,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 240, 180, 120), new Color(255, 240, 180, 0)});
            g.setPaint(glow);
            g.fillOval(x - 80, -50, 160, 160);
        }
    }

    private void drawField(Graphics2D g) {
        // Grass with stripes
        for (int i = 0; i < 10; i++) {
            Color c = (i % 2 == 0) ? new Color(40, 140, 60) : new Color(50, 160, 70);
            g.setColor(c);
            int stripeW = FIELD_W / 10;
            g.fillRect(FIELD_X + i * stripeW, FIELD_Y, stripeW + 1, FIELD_H);
        }

        // Field outline
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));
        g.drawRect(FIELD_X, FIELD_Y, FIELD_W, FIELD_H);

        // Center line
        g.drawLine(FIELD_X + FIELD_W / 2, FIELD_Y, FIELD_X + FIELD_W / 2, FIELD_Y + FIELD_H);

        // Center circle
        g.drawOval(FIELD_X + FIELD_W / 2 - 70, FIELD_Y + FIELD_H / 2 - 70, 140, 140);
        g.fillOval(FIELD_X + FIELD_W / 2 - 4, FIELD_Y + FIELD_H / 2 - 4, 8, 8);

        // Penalty boxes
        int pbW = 140, pbH = 300;
        int pbY = FIELD_Y + FIELD_H / 2 - pbH / 2;
        g.drawRect(FIELD_X, pbY, pbW, pbH);
        g.drawRect(FIELD_X + FIELD_W - pbW, pbY, pbW, pbH);

        // Goal boxes
        int gbW = 60, gbH = 160;
        int gbY = FIELD_Y + FIELD_H / 2 - gbH / 2;
        g.drawRect(FIELD_X, gbY, gbW, gbH);
        g.drawRect(FIELD_X + FIELD_W - gbW, gbY, gbW, gbH);

        // Penalty arcs
        g.drawArc(FIELD_X + pbW - 50, FIELD_Y + FIELD_H / 2 - 50, 100, 100, -50, 100);
        g.drawArc(FIELD_X + FIELD_W - pbW - 50, FIELD_Y + FIELD_H / 2 - 50, 100, 100, 230, 100);

        // Corner arcs
        g.drawArc(FIELD_X - 10, FIELD_Y - 10, 20, 20, 270, 90);
        g.drawArc(FIELD_X + FIELD_W - 10, FIELD_Y - 10, 20, 20, 180, 90);
        g.drawArc(FIELD_X - 10, FIELD_Y + FIELD_H - 10, 20, 20, 0, 90);
        g.drawArc(FIELD_X + FIELD_W - 10, FIELD_Y + FIELD_H - 10, 20, 20, 90, 90);
    }

    private void drawGoals(Graphics2D g) {
        int midY = FIELD_Y + FIELD_H / 2;
        int goalTop = midY - GOAL_H / 2;

        // Left goal
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(4));
        g.drawRect(FIELD_X - GOAL_DEPTH, goalTop, GOAL_DEPTH, GOAL_H);
        // Net pattern
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(255, 255, 255, 80));
        for (int i = 0; i < GOAL_DEPTH; i += 8) {
            g.drawLine(FIELD_X - GOAL_DEPTH + i, goalTop, FIELD_X - GOAL_DEPTH + i, goalTop + GOAL_H);
        }
        for (int i = 0; i < GOAL_H; i += 8) {
            g.drawLine(FIELD_X - GOAL_DEPTH, goalTop + i, FIELD_X, goalTop + i);
        }

        // Right goal
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(4));
        g.drawRect(FIELD_X + FIELD_W, goalTop, GOAL_DEPTH, GOAL_H);
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(255, 255, 255, 80));
        for (int i = 0; i < GOAL_DEPTH; i += 8) {
            g.drawLine(FIELD_X + FIELD_W + i, goalTop, FIELD_X + FIELD_W + i, goalTop + GOAL_H);
        }
        for (int i = 0; i < GOAL_H; i += 8) {
            g.drawLine(FIELD_X + FIELD_W, goalTop + i, FIELD_X + FIELD_W + GOAL_DEPTH, goalTop + i);
        }
    }

    private void drawPlayers(Graphics2D g) {
        drawPlayer(g, player1);
        drawPlayer(g, player2);
    }

    private void drawPlayer(Graphics2D g, Player p) {
        // Shadow
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval((int) p.getX() - p.getRadius() + 3, (int) p.getY() - p.getRadius() + 6,
                p.getRadius() * 2, p.getRadius() * 2);

        // Body gradient
        RadialGradientPaint body = new RadialGradientPaint(
                (float) p.getX() - 4, (float) p.getY() - 4, p.getRadius() * 1.5f,
                new float[]{0f, 1f},
                new Color[]{p.getLight(), p.getColor()});
        g.setPaint(body);
        g.fillOval((int) p.getX() - p.getRadius(), (int) p.getY() - p.getRadius(),
                p.getRadius() * 2, p.getRadius() * 2);

        // Outline
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawOval((int) p.getX() - p.getRadius(), (int) p.getY() - p.getRadius(),
                p.getRadius() * 2, p.getRadius() * 2);

        // Number
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        String label = p.getLabel();
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(label);
        g.drawString(label, (int) p.getX() - tw / 2, (int) p.getY() + 5);
    }

    private void drawBall(Graphics2D g) {
        // Shadow
        g.setColor(new Color(0, 0, 0, 100));
        g.fillOval((int) ball.getX() - ball.getRadius() + 3, (int) ball.getY() - ball.getRadius() + 5,
                ball.getRadius() * 2, ball.getRadius() * 2);

        // Ball
        RadialGradientPaint ballPaint = new RadialGradientPaint(
                (float) ball.getX() - 3, (float) ball.getY() - 3, ball.getRadius() * 1.4f,
                new float[]{0f, 0.7f, 1f},
                new Color[]{Color.WHITE, new Color(220, 220, 220), new Color(120, 120, 120)});
        g.setPaint(ballPaint);
        g.fillOval((int) ball.getX() - ball.getRadius(), (int) ball.getY() - ball.getRadius(),
                ball.getRadius() * 2, ball.getRadius() * 2);

        // Pentagon pattern
        g.setColor(Color.BLACK);
        int cx = (int) ball.getX();
        int cy = (int) ball.getY();
        int r = ball.getRadius() / 2;
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5;
            int px = cx + (int) (Math.cos(a) * r);
            int py = cy + (int) (Math.sin(a) * r);
            g.fillOval(px - 2, py - 2, 4, 4);
        }

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1));
        g.drawOval((int) ball.getX() - ball.getRadius(), (int) ball.getY() - ball.getRadius(),
                ball.getRadius() * 2, ball.getRadius() * 2);
    }

    private void drawTrails(Graphics2D g) {
        for (Trail t : trails) {
            int alpha = (int) (t.getLife() * 2);
            g.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, alpha))));
            int size = 4 + (int) (t.getLife() / 10);
            g.fillOval((int) t.getX() - size / 2, (int) t.getY() - size / 2, size, size);
        }
    }

    private void drawParticles(Graphics2D g) {
        for (Particle p : particles) {
            int alpha = (int) (p.getLife() * 4);
            Color c = p.getColor();
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha))));
            int size = 3 + (int) (p.getLife() / 20);
            g.fillOval((int) p.getX() - size / 2, (int) p.getY() - size / 2, size, size);
        }
    }

    private void drawHUD(Graphics2D g) {
        // Scoreboard background
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(WIDTH / 2 - 180, 10, 360, 44, 16, 16);
        g.setColor(new Color(255, 215, 0));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(WIDTH / 2 - 180, 10, 360, 44, 16, 16);

        // Scores
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.setColor(player1.getLight());
        g.drawString("P1", WIDTH / 2 - 150, 40);
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(score1), WIDTH / 2 - 90, 42);

        g.setColor(Color.YELLOW);
        g.drawString(":", WIDTH / 2 - 10, 42);

        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(score2), WIDTH / 2 + 70, 42);
        g.setColor(player2.getLight());
        String p2Name = vsComputer ? "CPU" : "P2";
        g.drawString(p2Name, WIDTH / 2 + 110, 40);

        // Timer
        int min = timeLeft / 60;
        int sec = timeLeft % 60;
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        String timeStr = String.format("%d:%02d", min, sec);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(timeStr, WIDTH / 2 - fm.stringWidth(timeStr) / 2, 75);

        // Mode indicator
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(200, 200, 200));
        String mode = vsComputer ? "Mode: vs Computer   [Esc = Menu]" : "Mode: 2 Players   [Esc = Menu]";
        g.drawString(mode, 20, HEIGHT - 15);

        // Controls hint
        g.drawString("P1: WASD + Space", 20, HEIGHT - 35);
        if (!vsComputer) g.drawString("P2: Arrows + Enter", WIDTH - 170, HEIGHT - 35);
    }

    private void drawMenu(Graphics2D g) {
        // Title
        g.setFont(new Font("SansSerif", Font.BOLD, 64));
        GradientPaint titleGrad = new GradientPaint(0, 100, Color.YELLOW, 0, 180, Color.ORANGE);
        g.setPaint(titleGrad);
        String title = "SOCCER SIMULATION";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, WIDTH / 2 - fm.stringWidth(title) / 2, 200);

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(Color.WHITE);
        String sub = "Choose your match";
        g.drawString(sub, WIDTH / 2 - fm.stringWidth(sub) / 2 + 200, 260);

        // Buttons
        drawMenuButton(g, WIDTH / 2 - 180, 320, 360, 70, "1  -  Player vs Computer", Color.CYAN);
        drawMenuButton(g, WIDTH / 2 - 180, 420, 360, 70, "2  -  Two Players", new Color(180, 255, 180));

        // Footer
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(180, 180, 180));
        String hint1 = "Player 1:  W A S D  to move,  SPACE  to kick";
        String hint2 = "Player 2:  Arrow keys  to move,  ENTER  to kick";
        g.drawString(hint1, WIDTH / 2 - fm.stringWidth(hint1) / 2 + 150, 560);
        g.drawString(hint2, WIDTH / 2 - fm.stringWidth(hint2) / 2 + 150, 585);
    }

    private void drawMenuButton(Graphics2D g, int x, int y, int w, int h, String text, Color accent) {
        GradientPaint gp = new GradientPaint(x, y, new Color(40, 40, 60), x + w, y + h, new Color(20, 20, 40));
        g.setPaint(gp);
        g.fillRoundRect(x, y, w, h, 20, 20);
        g.setColor(accent);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x, y, w, h, 20, 20);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x + w / 2 - fm.stringWidth(text) / 2, y + h / 2 + 8);
    }

    private void drawGoalBanner(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, HEIGHT / 2 - 80, WIDTH, 160);
        g.setFont(new Font("SansSerif", Font.BOLD, 90));
        GradientPaint gp = new GradientPaint(0, HEIGHT / 2 - 40, Color.YELLOW, 0, HEIGHT / 2 + 40, Color.RED);
        g.setPaint(gp);
        String txt = "GOAL!";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(txt, WIDTH / 2 - fm.stringWidth(txt) / 2, HEIGHT / 2 + 30);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.setColor(Color.WHITE);
        g.drawString("Scored by " + lastScorer, WIDTH / 2 - 100, HEIGHT / 2 + 70);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setFont(new Font("SansSerif", Font.BOLD, 72));
        g.setColor(Color.YELLOW);
        String title = "FULL TIME";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, WIDTH / 2 - fm.stringWidth(title) / 2, 240);

        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        g.setColor(Color.WHITE);
        String score = score1 + "  -  " + score2;
        g.drawString(score, WIDTH / 2 - fm.stringWidth(score) / 2, 330);

        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        String result;
        if (score1 > score2) result = "Player 1 Wins!";
        else if (score2 > score1) result = (vsComputer ? "Computer" : "Player 2") + " Wins!";
        else result = "Draw!";
        g.setColor(new Color(255, 220, 100));
        g.drawString(result, WIDTH / 2 - fm.stringWidth(result) / 2, 400);

        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.setColor(Color.WHITE);
        String hint = "Press  R  to restart  |  Press  Esc  for menu";
        g.drawString(hint, WIDTH / 2 - fm.stringWidth(hint) / 2, 480);
    }

    // -----------------------------
    // Key handling
    // -----------------------------
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        pressedKeys.add(k);

        if (state == State.MENU) {
            if (k == KeyEvent.VK_1) startGame(true);
            else if (k == KeyEvent.VK_2) startGame(false);
        } else if (state == State.GAMEOVER) {
            if (k == KeyEvent.VK_R) startGame(vsComputer);
            else if (k == KeyEvent.VK_ESCAPE) state = State.MENU;
        } else if (state == State.PLAYING || state == State.GOAL) {
            if (k == KeyEvent.VK_ESCAPE) state = State.MENU;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // ============================================================
    // Inner classes
    // ============================================================

    // -----------------------------
    // Player
    // -----------------------------
    private static class Player {
        private double x, y;
        private double vx, vy;
        private final int radius = 22;
        private final Color color;
        private final Color light;
        private final String label;

        Player(double x, double y, Color color, Color light, String label) {
            this.x = x; this.y = y;
            this.color = color; this.light = light; this.label = label;
        }

        void applyForce(double fx, double fy) {
            vx += fx;
            vy += fy;
            double sp = Math.hypot(vx, vy);
            double max = PLAYER_BOOST;
            if (sp > max) {
                vx = vx / sp * max;
                vy = vy / sp * max;
            }
        }

        void update() {
            x += vx;
            y += vy;
            vx *= FRICTION;
            vy *= FRICTION;
        }

        void setPosition(double nx, double ny) { x = nx; y = ny; }
        void setVelocity(double nvx, double nvy) { vx = nvx; vy = nvy; }
        double getX() { return x; }
        double getY() { return y; }
        double getVX() { return vx; }
        double getVY() { return vy; }
        int getRadius() { return radius; }
        Color getColor() { return color; }
        Color getLight() { return light; }
        String getLabel() { return label; }
    }

    // -----------------------------
    // Ball
    // -----------------------------
    private static class Ball {
        private double x, y;
        private double vx, vy;
        private final int radius = 10;

        Ball(double x, double y) {
            this.x = x; this.y = y;
        }

        void reset(double nx, double ny) {
            x = nx; y = ny; vx = 0; vy = 0;
        }

        void update() {
            x += vx;
            y += vy;
            vx *= BALL_FRICTION;
            vy *= BALL_FRICTION;

            // Bounce off top/bottom walls
            if (y - radius < FIELD_Y) { y = FIELD_Y + radius; vy = -vy * 0.8; }
            if (y + radius > FIELD_Y + FIELD_H) { y = FIELD_Y + FIELD_H - radius; vy = -vy * 0.8; }

            // Bounce off left/right walls, but allow entry into goal area
            int midY = FIELD_Y + FIELD_H / 2;
            int goalTop = midY - GOAL_H / 2;
            int goalBot = midY + GOAL_H / 2;
            boolean inGoalY = y > goalTop && y < goalBot;

            if (x - radius < FIELD_X) {
                if (!inGoalY) { x = FIELD_X + radius; vx = -vx * 0.8; }
                else if (x - radius < FIELD_X - GOAL_DEPTH) {
                    x = FIELD_X - GOAL_DEPTH + radius; vx = -vx * 0.5;
                }
            }
            if (x + radius > FIELD_X + FIELD_W) {
                if (!inGoalY) { x = FIELD_X + FIELD_W - radius; vx = -vx * 0.8; }
                else if (x + radius > FIELD_X + FIELD_W + GOAL_DEPTH) {
                    x = FIELD_X + FIELD_W + GOAL_DEPTH - radius; vx = -vx * 0.5;
                }
            }

            // Cap speed
            double sp = Math.hypot(vx, vy);
            if (sp > 18) { vx = vx / sp * 18; vy = vy / sp * 18; }
        }

        void setVelocity(double nvx, double nvy) { vx = nvx; vy = nvy; }
        double getX() { return x; }
        double getY() { return y; }
        double getVX() { return vx; }
        double getVY() { return vy; }
        int getRadius() { return radius; }
    }

    // -----------------------------
    // Particle
    // -----------------------------
    private static class Particle {
        private double x, y, vx, vy;
        private int life;
        private final Color color;

        Particle(double x, double y, double vx, double vy, Color color, int life) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.color = color;
            this.life = life;
        }

        void update() {
            x += vx; y += vy;
            vx *= 0.95; vy *= 0.95;
            vy += 0.08;
            life--;
        }

        int getLife() { return life; }
        Color getColor() { return color; }
        double getX() { return x; }
        double getY() { return y; }
    }

    // -----------------------------
    // Trail
    // -----------------------------
    private static class Trail {
        private double x, y;
        private int life;

        Trail(double x, double y, Color c) {
            this.x = x; this.y = y; this.life = 20;
        }

        void age() { life--; }
        int getLife() { return life; }
        double getX() { return x; }
        double getY() { return y; }
    }

    // -----------------------------
    // AI Controller
    // -----------------------------
    private static class AIController {
        private final Player self;
        private final Ball ball;
        private final SoccerSimulationGame game;
        private double thinkTimer = 0;
        private double targetX, targetY;

        AIController(Player self, Ball ball, SoccerSimulationGame game) {
            this.self = self;
            this.ball = ball;
            this.game = game;
            this.targetX = self.getX();
            this.targetY = self.getY();
        }

        void update(double dt) {
            thinkTimer -= dt;
            if (thinkTimer <= 0) {
                thinkTimer = 0.15 + game.rng.nextDouble() * 0.1;
                decideTarget();
            }

            double dx = targetX - self.getX();
            double dy = targetY - self.getY();
            double dist = Math.hypot(dx, dy);
            if (dist > 1) {
                double nx = dx / dist;
                double ny = dy / dist;
                self.applyForce(nx * PLAYER_SPEED * 0.95, ny * PLAYER_SPEED * 0.95);
            }

            // Kick if close to ball and facing opponent's goal
            double toBallX = ball.getX() - self.getX();
            double toBallY = ball.getY() - self.getY();
            double ballDist = Math.hypot(toBallX, toBallY);
            if (ballDist < self.getRadius() + ball.getRadius() + 8) {
                // Aim toward left goal (AI defends right, attacks left)
                int goalX = FIELD_X + 20;
                int goalY = FIELD_Y + FIELD_H / 2 + (game.rng.nextInt(80) - 40);
                double aimX = goalX - ball.getX();
                double aimY = goalY - ball.getY();
                double aimLen = Math.hypot(aimX, aimY);
                if (aimLen > 0) {
                    aimX /= aimLen; aimY /= aimLen;
                    ball.setVelocity(aimX * KICK_POWER * 0.95 + self.getVX() * 0.3,
                            aimY * KICK_POWER * 0.95 + self.getVY() * 0.3);
                    game.spawnKickParticles(ball.getX(), ball.getY(), self.getColor());
                }
            }
        }

        private void decideTarget() {
            int myGoalX = FIELD_X + FIELD_W - 40;
            int myGoalY = FIELD_Y + FIELD_H / 2;

            // If ball is on our side and moving toward us, defend
            boolean ballOnOurSide = ball.getX() > FIELD_X + FIELD_W / 2;
            boolean ballMovingToward = ball.getVX() > 0.5;

            if (ballOnOurSide && ballMovingToward &&
                    Math.abs(ball.getX() - myGoalX) < 250) {
                // Intercept between ball and our goal
                targetX = ball.getX() + 20;
                targetY = ball.getY();
            } else {
                // Chase ball to attack
                targetX = ball.getX() + 15;
                targetY = ball.getY();
            }

            // Add slight randomness for human-like behavior
            targetX += (game.rng.nextDouble() - 0.5) * 10;
            targetY += (game.rng.nextDouble() - 0.5) * 10;
        }
    }
}