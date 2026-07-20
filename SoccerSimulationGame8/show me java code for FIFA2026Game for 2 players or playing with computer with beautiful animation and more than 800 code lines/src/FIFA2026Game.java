import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class FIFA2026Game extends JPanel implements ActionListener, KeyListener {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 750;
    private static final int FIELD_X = 70;
    private static final int FIELD_Y = 80;
    private static final int FIELD_W = WIDTH - 140;
    private static final int FIELD_H = HEIGHT - 160;
    private static final int GOAL_H = 200;
    private static final int GOAL_DEPTH = 35;
    private static final double FRICTION = 0.982;
    private static final double BALL_FRICTION = 0.993;
    private static final double PLAYER_SPEED = 3.4;
    private static final double PLAYER_BOOST = 5.2;
    private static final double KICK_POWER = 13.0;
    private static final double POWER_SHOT = 18.0;
    private static final int GAME_DURATION = 150;
    private static final int TARGET_FPS = 60;

    // -----------------------------
    // Game state
    // -----------------------------
    private enum State { MENU, TEAM_SELECT, INTRO, PLAYING, GOAL, GAMEOVER }
    private State state = State.MENU;
    private boolean vsComputer = true;

    private Player player1;
    private Player player2;
    private Ball ball;
    private AIController ai;
    private List<Particle> particles = new ArrayList<>();
    private List<Trail> trails = new ArrayList<>();
    private List<Confetti> confetti = new ArrayList<>();
    private List<CrowdMember> crowd = new ArrayList<>();

    private Team team1, team2;
    private int team1Choice = 0;
    private int team2Choice = 1;
    private boolean selectingTeam1 = true;

    private int score1 = 0;
    private int score2 = 0;
    private int timeLeft = GAME_DURATION;
    private long lastTick = System.currentTimeMillis();
    private long goalTimer = 0;
    private long introTimer = 0;
    private String lastScorer = "";
    private String commentary = "";
    private long commentaryTimer = 0;

    private final Set<Integer> pressedKeys = new HashSet<>();
    private Timer gameTimer;
    private Random rng = new Random();
    private int frameCount = 0;

    // -----------------------------
    // Entry point
    // -----------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("FIFA 2026 - World Cup");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            FIFA2026Game game = new FIFA2026Game();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // -----------------------------
    // Constructor
    // -----------------------------
    public FIFA2026Game() {
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
                if (timeLeft == 45) setCommentary("HALF TIME APPROACHING!");
                if (timeLeft == 30) setCommentary("FINAL MINUTES!");
            }
        });
        gameTimer.start();

        initCrowd();
    }

    private void initCrowd() {
        for (int i = 0; i < 200; i++) {
            int x = rng.nextInt(WIDTH);
            int y;
            if (x < FIELD_X || x > FIELD_X + FIELD_W) {
                y = rng.nextInt(HEIGHT);
            } else {
                y = rng.nextBoolean() ? rng.nextInt(FIELD_Y - 10) : FIELD_Y + FIELD_H + 10 + rng.nextInt(40);
            }
            Color c = new Color(50 + rng.nextInt(150), 50 + rng.nextInt(150), 50 + rng.nextInt(150));
            crowd.add(new CrowdMember(x, y, c));
        }
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
        confetti.clear();

        team1 = Team.TEAMS[team1Choice];
        team2 = Team.TEAMS[team2Choice];

        int midY = FIELD_Y + FIELD_H / 2;
        int p1X = FIELD_X + 130;
        int p2X = FIELD_X + FIELD_W - 130;

        player1 = new Player(p1X, midY, team1.primaryColor, team1.secondaryColor, team1.name);
        player2 = new Player(p2X, midY, team2.primaryColor, team2.secondaryColor, team2.name);

        ball = new Ball(WIDTH / 2, HEIGHT / 2);
        ai = new AIController(player2, ball, this);
        state = State.INTRO;
        introTimer = System.currentTimeMillis();
        setCommentary("KICK OFF! " + team1.name + " vs " + team2.name);
    }

    private void resetAfterGoal() {
        int midY = FIELD_Y + FIELD_H / 2;
        player1.setPosition(FIELD_X + 130, midY);
        player2.setPosition(FIELD_X + FIELD_W - 130, midY);
        ball.reset(WIDTH / 2, HEIGHT / 2);
        player1.setVelocity(0, 0);
        player2.setVelocity(0, 0);
    }

    private void setCommentary(String text) {
        commentary = text;
        commentaryTimer = System.currentTimeMillis();
    }

    // -----------------------------
    // Game loop
    // -----------------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.currentTimeMillis();
        double dt = (now - lastTick) / 1000.0;
        lastTick = now;
        frameCount++;

        update(dt);
        repaint();
    }

    private void update(double dt) {
        if (state == State.INTRO) {
            if (System.currentTimeMillis() - introTimer > 2500) {
                state = State.PLAYING;
            }
        } else if (state == State.PLAYING) {
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
            if (System.currentTimeMillis() - goalTimer > 2500) {
                resetAfterGoal();
                state = State.PLAYING;
            }
        }
        updateParticles();
        updateConfetti();
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
            tryKick(player1, pressedKeys.contains(KeyEvent.VK_SHIFT) ? POWER_SHOT : KICK_POWER);
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
                tryKick(player2, pressedKeys.contains(KeyEvent.VK_SHIFT) ? POWER_SHOT : KICK_POWER);
            }
        }
    }

    private void tryKick(Player p, double power) {
        double dx = ball.getX() - p.getX();
        double dy = ball.getY() - p.getY();
        double dist = Math.hypot(dx, dy);
        if (dist < p.getRadius() + ball.getRadius() + 12) {
            double len = Math.max(dist, 0.0001);
            double nx = dx / len;
            double ny = dy / len;
            ball.setVelocity(nx * power + p.getVX() * 0.4,
                    ny * power + p.getVY() * 0.4);
            spawnKickParticles(ball.getX(), ball.getY(), p.getColor());
            if (power >= POWER_SHOT) {
                setCommentary("POWER SHOT!");
            }
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
                ball.setVelocity(ball.getVX() - 1.7 * dot * nx + p.getVX() * 0.3,
                        ball.getVY() - 1.7 * dot * ny + p.getVY() * 0.3);
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

        if (ball.getX() - ball.getRadius() < FIELD_X &&
                ball.getY() > goalTop && ball.getY() < goalBot) {
            score2++;
            lastScorer = vsComputer ? team2.name : team2.name;
            triggerGoal();
        }
        if (ball.getX() + ball.getRadius() > FIELD_X + FIELD_W &&
                ball.getY() > goalTop && ball.getY() < goalBot) {
            score1++;
            lastScorer = team1.name;
            triggerGoal();
        }
    }

    private void triggerGoal() {
        state = State.GOAL;
        goalTimer = System.currentTimeMillis();
        spawnGoalExplosion(ball.getX(), ball.getY());
        spawnConfetti();
        setCommentary("GOOOAL! " + lastScorer + " SCORES!");
    }

    // -----------------------------
    // Particles / trails / confetti
    // -----------------------------
    private void spawnKickParticles(double x, double y, Color c) {
        for (int i = 0; i < 12; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double sp = 1.5 + rng.nextDouble() * 3.5;
            particles.add(new Particle(x, y, Math.cos(ang) * sp, Math.sin(ang) * sp, c, 35));
        }
    }

    private void spawnGoalExplosion(double x, double y) {
        Color[] colors = { Color.YELLOW, Color.ORANGE, Color.WHITE, Color.RED, Color.MAGENTA };
        for (int i = 0; i < 150; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double sp = 2 + rng.nextDouble() * 8;
            Color c = colors[rng.nextInt(colors.length)];
            particles.add(new Particle(x, y, Math.cos(ang) * sp, Math.sin(ang) * sp, c, 70 + rng.nextInt(40)));
        }
    }

    private void spawnConfetti() {
        for (int i = 0; i < 200; i++) {
            double x = rng.nextInt(WIDTH);
            double y = -rng.nextInt(100);
            double vx = (rng.nextDouble() - 0.5) * 4;
            double vy = 2 + rng.nextDouble() * 3;
            Color c = new Color(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
            confetti.add(new Confetti(x, y, vx, vy, c, 180));
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

    private void updateConfetti() {
        Iterator<Confetti> it = confetti.iterator();
        while (it.hasNext()) {
            Confetti c = it.next();
            c.update();
            if (c.getLife() <= 0 || c.getY() > HEIGHT) it.remove();
        }
    }

    private void updateTrails() {
        trails.add(new Trail(ball.getX(), ball.getY()));
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
        } else if (state == State.TEAM_SELECT) {
            drawTeamSelect(g);
        } else if (state == State.INTRO) {
            drawIntro(g);
        } else {
            drawField(g);
            drawCrowd(g);
            drawTrails(g);
            drawGoals(g);
            drawPlayers(g);
            drawBall(g);
            drawParticles(g);
            drawConfetti(g);
            drawHUD(g);
            if (state == State.GOAL) drawGoalBanner(g);
            if (state == State.GAMEOVER) drawGameOver(g);
        }
    }

    private void drawBackground(Graphics2D g) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(10, 20, 50),
                0, HEIGHT, new Color(0, 5, 20));
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Stadium lights
        for (int i = 0; i < 5; i++) {
            int x = 80 + i * 260;
            RadialGradientPaint glow = new RadialGradientPaint(
                    x, 25, 100,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 240, 180, 140), new Color(255, 240, 180, 0)});
            g.setPaint(glow);
            g.fillOval(x - 100, -75, 200, 200);
        }
    }

    private void drawCrowd(Graphics2D g) {
        for (CrowdMember c : crowd) {
            g.setColor(c.getColor());
            int bob = (int) (Math.sin(frameCount * 0.05 + c.getX() * 0.1) * 2);
            g.fillOval(c.getX(), c.getY() + bob, 4, 6);
        }
    }

    private void drawField(Graphics2D g) {
        // Grass with stripes
        for (int i = 0; i < 12; i++) {
            Color c = (i % 2 == 0) ? new Color(35, 130, 55) : new Color(45, 150, 65);
            g.setColor(c);
            int stripeW = FIELD_W / 12;
            g.fillRect(FIELD_X + i * stripeW, FIELD_Y, stripeW + 1, FIELD_H);
        }

        // Field outline
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));
        g.drawRect(FIELD_X, FIELD_Y, FIELD_W, FIELD_H);

        // Center line
        g.drawLine(FIELD_X + FIELD_W / 2, FIELD_Y, FIELD_X + FIELD_W / 2, FIELD_Y + FIELD_H);

        // Center circle
        g.drawOval(FIELD_X + FIELD_W / 2 - 75, FIELD_Y + FIELD_H / 2 - 75, 150, 150);
        g.fillOval(FIELD_X + FIELD_W / 2 - 4, FIELD_Y + FIELD_H / 2 - 4, 8, 8);

        // Penalty boxes
        int pbW = 150, pbH = 320;
        int pbY = FIELD_Y + FIELD_H / 2 - pbH / 2;
        g.drawRect(FIELD_X, pbY, pbW, pbH);
        g.drawRect(FIELD_X + FIELD_W - pbW, pbY, pbW, pbH);

        // Goal boxes
        int gbW = 65, gbH = 180;
        int gbY = FIELD_Y + FIELD_H / 2 - gbH / 2;
        g.drawRect(FIELD_X, gbY, gbW, gbH);
        g.drawRect(FIELD_X + FIELD_W - gbW, gbY, gbW, gbH);

        // Penalty arcs
        g.drawArc(FIELD_X + pbW - 55, FIELD_Y + FIELD_H / 2 - 55, 110, 110, -50, 100);
        g.drawArc(FIELD_X + FIELD_W - pbW - 55, FIELD_Y + FIELD_H / 2 - 55, 110, 110, 230, 100);

        // Corner arcs
        g.drawArc(FIELD_X - 12, FIELD_Y - 12, 24, 24, 270, 90);
        g.drawArc(FIELD_X + FIELD_W - 12, FIELD_Y - 12, 24, 24, 180, 90);
        g.drawArc(FIELD_X - 12, FIELD_Y + FIELD_H - 12, 24, 24, 0, 90);
        g.drawArc(FIELD_X + FIELD_W - 12, FIELD_Y + FIELD_H - 12, 24, 24, 90, 90);

        // FIFA 2026 logo in center
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(255, 255, 255, 100));
        g.drawString("FIFA 2026", FIELD_X + FIELD_W / 2 - 35, FIELD_Y + FIELD_H / 2 + 5);
    }

    private void drawGoals(Graphics2D g) {
        int midY = FIELD_Y + FIELD_H / 2;
        int goalTop = midY - GOAL_H / 2;

        // Left goal
        g.setColor(new Color(255, 255, 255, 220));
        g.setStroke(new BasicStroke(5));
        g.drawRect(FIELD_X - GOAL_DEPTH, goalTop, GOAL_DEPTH, GOAL_H);
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(255, 255, 255, 100));
        for (int i = 0; i < GOAL_DEPTH; i += 7) {
            g.drawLine(FIELD_X - GOAL_DEPTH + i, goalTop, FIELD_X - GOAL_DEPTH + i, goalTop + GOAL_H);
        }
        for (int i = 0; i < GOAL_H; i += 7) {
            g.drawLine(FIELD_X - GOAL_DEPTH, goalTop + i, FIELD_X, goalTop + i);
        }

        // Right goal
        g.setColor(new Color(255, 255, 255, 220));
        g.setStroke(new BasicStroke(5));
        g.drawRect(FIELD_X + FIELD_W, goalTop, GOAL_DEPTH, GOAL_H);
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(255, 255, 255, 100));
        for (int i = 0; i < GOAL_DEPTH; i += 7) {
            g.drawLine(FIELD_X + FIELD_W + i, goalTop, FIELD_X + FIELD_W + i, goalTop + GOAL_H);
        }
        for (int i = 0; i < GOAL_H; i += 7) {
            g.drawLine(FIELD_X + FIELD_W, goalTop + i, FIELD_X + FIELD_W + GOAL_DEPTH, goalTop + i);
        }
    }

    private void drawPlayers(Graphics2D g) {
        drawPlayer(g, player1);
        drawPlayer(g, player2);
    }

    private void drawPlayer(Graphics2D g, Player p) {
        // Shadow
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval((int) p.getX() - p.getRadius() + 4, (int) p.getY() - p.getRadius() + 7,
                p.getRadius() * 2, p.getRadius() * 2);

        // Running animation (legs)
        double speed = Math.hypot(p.getVX(), p.getVY());
        if (speed > 0.5) {
            int legOffset = (int) (Math.sin(frameCount * 0.3) * 4);
            g.setColor(p.getColor().darker());
            g.fillOval((int) p.getX() - 6, (int) p.getY() + p.getRadius() - 4 + legOffset, 6, 8);
            g.fillOval((int) p.getX() + 2, (int) p.getY() + p.getRadius() - 4 - legOffset, 6, 8);
        }

        // Body gradient
        RadialGradientPaint body = new RadialGradientPaint(
                (float) p.getX() - 5, (float) p.getY() - 5, p.getRadius() * 1.6f,
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

        // Team name
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        String label = p.getLabel().substring(0, Math.min(3, p.getLabel().length()));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(label);
        g.drawString(label, (int) p.getX() - tw / 2, (int) p.getY() + 4);
    }

    private void drawBall(Graphics2D g) {
        // Shadow
        g.setColor(new Color(0, 0, 0, 110));
        g.fillOval((int) ball.getX() - ball.getRadius() + 3, (int) ball.getY() - ball.getRadius() + 6,
                ball.getRadius() * 2, ball.getRadius() * 2);

        // Ball with spin
        double spin = frameCount * 0.1;
        RadialGradientPaint ballPaint = new RadialGradientPaint(
                (float) ball.getX() - 3, (float) ball.getY() - 3, ball.getRadius() * 1.5f,
                new float[]{0f, 0.7f, 1f},
                new Color[]{Color.WHITE, new Color(230, 230, 230), new Color(130, 130, 130)});
        g.setPaint(ballPaint);
        g.fillOval((int) ball.getX() - ball.getRadius(), (int) ball.getY() - ball.getRadius(),
                ball.getRadius() * 2, ball.getRadius() * 2);

        // Pentagon pattern (rotating)
        g.setColor(Color.BLACK);
        int cx = (int) ball.getX();
        int cy = (int) ball.getY();
        int r = ball.getRadius() / 2;
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5 + spin;
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
            int alpha = (int) (t.getLife() * 2.5);
            g.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, alpha))));
            int size = 5 + (int) (t.getLife() / 8);
            g.fillOval((int) t.getX() - size / 2, (int) t.getY() - size / 2, size, size);
        }
    }

    private void drawParticles(Graphics2D g) {
        for (Particle p : particles) {
            int alpha = (int) (p.getLife() * 4);
            Color c = p.getColor();
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha))));
            int size = 4 + (int) (p.getLife() / 18);
            g.fillOval((int) p.getX() - size / 2, (int) p.getY() - size / 2, size, size);
        }
    }

    private void drawConfetti(Graphics2D g) {
        for (Confetti c : confetti) {
            g.setColor(c.getColor());
            int size = 4 + (int) (c.getLife() / 40);
            g.fillRect((int) c.getX(), (int) c.getY(), size, size);
        }
    }

    private void drawHUD(Graphics2D g) {
        // Scoreboard
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRoundRect(WIDTH / 2 - 220, 10, 440, 55, 20, 20);
        g.setColor(new Color(255, 215, 0));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(WIDTH / 2 - 220, 10, 440, 55, 20, 20);

        // Team names and flags
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(team1.primaryColor);
        g.fillRect(WIDTH / 2 - 200, 22, 20, 14);
        g.setColor(team1.secondaryColor);
        g.fillRect(WIDTH / 2 - 200, 36, 20, 7);
        g.setColor(Color.WHITE);
        g.drawString(team1.name, WIDTH / 2 - 170, 38);

        g.setColor(team2.primaryColor);
        g.fillRect(WIDTH / 2 + 180, 22, 20, 14);
        g.setColor(team2.secondaryColor);
        g.fillRect(WIDTH / 2 + 180, 36, 20, 7);
        g.setColor(Color.WHITE);
        String t2 = team2.name;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(t2, WIDTH / 2 + 160 - fm.stringWidth(t2), 38);

        // Scores
        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(score1), WIDTH / 2 - 60, 48);
        g.setColor(Color.YELLOW);
        g.drawString(":", WIDTH / 2 - 10, 48);
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(score2), WIDTH / 2 + 40, 48);

        // Timer
        int min = timeLeft / 60;
        int sec = timeLeft % 60;
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        String timeStr = String.format("%d:%02d", min, sec);
        g.drawString(timeStr, WIDTH / 2 - fm.stringWidth(timeStr) / 2, 85);

        // Commentary
        if (System.currentTimeMillis() - commentaryTimer < 3000) {
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(255, 255, 100));
            g.drawString(commentary, WIDTH / 2 - fm.stringWidth(commentary) / 2, 110);
        }

        // Mode indicator
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(200, 200, 200));
        String mode = vsComputer ? "vs Computer   [Esc = Menu]" : "2 Players   [Esc = Menu]";
        g.drawString(mode, 20, HEIGHT - 15);

        g.drawString("P1: WASD + Space (+Shift=Power)", 20, HEIGHT - 35);
        if (!vsComputer) g.drawString("P2: Arrows + Enter (+Shift=Power)", WIDTH - 250, HEIGHT - 35);
    }

    private void drawMenu(Graphics2D g) {
        // Title
        g.setFont(new Font("SansSerif", Font.BOLD, 72));
        GradientPaint titleGrad = new GradientPaint(0, 120, Color.YELLOW, 0, 200, new Color(255, 100, 0));
        g.setPaint(titleGrad);
        String title = "FIFA 2026";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, WIDTH / 2 - fm.stringWidth(title) / 2, 180);

        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        g.setColor(Color.WHITE);
        String sub = "WORLD CUP";
        g.drawString(sub, WIDTH / 2 - fm.stringWidth(sub) / 2, 230);

        // Buttons
        drawMenuButton(g, WIDTH / 2 - 200, 320, 400, 80, "1  -  Player vs Computer", Color.CYAN);
        drawMenuButton(g, WIDTH / 2 - 200, 430, 400, 80, "2  -  Two Players", new Color(180, 255, 180));

        // Footer
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(180, 180, 180));
        String hint1 = "Player 1:  WASD  to move,  SPACE  to kick,  SHIFT  for power shot";
        String hint2 = "Player 2:  Arrows  to move,  ENTER  to kick,  SHIFT  for power shot";
        g.drawString(hint1, WIDTH / 2 - fm.stringWidth(hint1) / 2, 580);
        g.drawString(hint2, WIDTH / 2 - fm.stringWidth(hint2) / 2, 605);
    }

    private void drawTeamSelect(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        g.setColor(Color.YELLOW);
        String title = "SELECT YOUR TEAM";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, WIDTH / 2 - fm.stringWidth(title) / 2, 100);

        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.setColor(Color.WHITE);
        String prompt = selectingTeam1 ? "Player 1 - Press 1-8" : "Player 2 - Press 1-8";
        g.drawString(prompt, WIDTH / 2 - fm.stringWidth(prompt) / 2, 150);

        int cols = 4;
        int rows = 2;
        int boxW = 200, boxH = 180;
        int startX = WIDTH / 2 - (cols * boxW + (cols - 1) * 20) / 2;
        int startY = 200;

        for (int i = 0; i < Team.TEAMS.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (boxW + 20);
            int y = startY + row * (boxH + 20);

            Team t = Team.TEAMS[i];
            boolean selected = (selectingTeam1 && i == team1Choice) || (!selectingTeam1 && i == team2Choice);

            g.setColor(selected ? new Color(100, 100, 150) : new Color(40, 40, 60));
            g.fillRoundRect(x, y, boxW, boxH, 15, 15);
            g.setColor(selected ? Color.YELLOW : Color.GRAY);
            g.setStroke(new BasicStroke(selected ? 3 : 1));
            g.drawRoundRect(x, y, boxW, boxH, 15, 15);

            // Flag
            g.setColor(t.primaryColor);
            g.fillRect(x + 20, y + 20, 160, 60);
            g.setColor(t.secondaryColor);
            g.fillRect(x + 20, y + 50, 160, 30);

            // Team name
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.setColor(Color.WHITE);
            g.drawString(t.name, x + boxW / 2 - fm.stringWidth(t.name) / 2, y + 120);

            // Number
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(Color.YELLOW);
            g.drawString(String.valueOf(i + 1), x + 10, y + 25);
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(new Color(200, 200, 200));
        g.drawString("Press ENTER to confirm", WIDTH / 2 - 100, HEIGHT - 50);
    }

    private void drawIntro(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 220));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setFont(new Font("SansSerif", Font.BOLD, 64));
        GradientPaint gp = new GradientPaint(0, HEIGHT / 2 - 50, Color.YELLOW, 0, HEIGHT / 2 + 50, Color.RED);
        g.setPaint(gp);
        String vs = team1.name + "  vs  " + team2.name;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(vs, WIDTH / 2 - fm.stringWidth(vs) / 2, HEIGHT / 2);

        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        g.setColor(Color.WHITE);
        g.drawString("FIFA 2026 WORLD CUP", WIDTH / 2 - 200, HEIGHT / 2 + 80);
    }

    private void drawMenuButton(Graphics2D g, int x, int y, int w, int h, String text, Color accent) {
        GradientPaint gp = new GradientPaint(x, y, new Color(50, 50, 80), x + w, y + h, new Color(20, 20, 40));
        g.setPaint(gp);
        g.fillRoundRect(x, y, w, h, 25, 25);
        g.setColor(accent);
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(x, y, w, h, 25, 25);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x + w / 2 - fm.stringWidth(text) / 2, y + h / 2 + 10);
    }

    private void drawGoalBanner(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, HEIGHT / 2 - 100, WIDTH, 200);
        g.setFont(new Font("SansSerif", Font.BOLD, 100));
        GradientPaint gp = new GradientPaint(0, HEIGHT / 2 - 50, Color.YELLOW, 0, HEIGHT / 2 + 50, Color.RED);
        g.setPaint(gp);
        String txt = "GOAL!";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(txt, WIDTH / 2 - fm.stringWidth(txt) / 2, HEIGHT / 2 + 35);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(Color.WHITE);
        g.drawString("Scored by " + lastScorer, WIDTH / 2 - 120, HEIGHT / 2 + 85);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 220));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setFont(new Font("SansSerif", Font.BOLD, 80));
        g.setColor(Color.YELLOW);
        String title = "FULL TIME";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, WIDTH / 2 - fm.stringWidth(title) / 2, 220);

        g.setFont(new Font("SansSerif", Font.BOLD, 56));
        g.setColor(Color.WHITE);
        String score = score1 + "  -  " + score2;
        g.drawString(score, WIDTH / 2 - fm.stringWidth(score) / 2, 320);

        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        String result;
        if (score1 > score2) result = team1.name + " Wins!";
        else if (score2 > score1) result = team2.name + " Wins!";
        else result = "Draw!";
        g.setColor(new Color(255, 220, 100));
        g.drawString(result, WIDTH / 2 - fm.stringWidth(result) / 2, 400);

        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.setColor(Color.WHITE);
        String hint = "Press  R  to restart  |  Press  Esc  for menu";
        g.drawString(hint, WIDTH / 2 - fm.stringWidth(hint) / 2, 490);
    }

    // -----------------------------
    // Key handling
    // -----------------------------
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        pressedKeys.add(k);

        if (state == State.MENU) {
            if (k == KeyEvent.VK_1) {
                state = State.TEAM_SELECT;
                selectingTeam1 = true;
            } else if (k == KeyEvent.VK_2) {
                state = State.TEAM_SELECT;
                selectingTeam1 = true;
            }
        } else if (state == State.TEAM_SELECT) {
            if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_8) {
                int choice = k - KeyEvent.VK_1;
                if (selectingTeam1) {
                    team1Choice = choice;
                    if (vsComputer && state == State.TEAM_SELECT) {
                        // If vs computer, auto-pick team2
                        team2Choice = (choice + 1) % Team.TEAMS.length;
                        startGame(true);
                    } else {
                        selectingTeam1 = false;
                    }
                } else {
                    team2Choice = choice;
                    startGame(false);
                }
            } else if (k == KeyEvent.VK_ESCAPE) {
                state = State.MENU;
            }
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
    // Team
    // -----------------------------
    private static class Team {
        String name;
        Color primaryColor;
        Color secondaryColor;

        Team(String name, Color primary, Color secondary) {
            this.name = name;
            this.primaryColor = primary;
            this.secondaryColor = secondary;
        }

        static final Team[] TEAMS = {
                new Team("USA", new Color(200, 30, 50), Color.WHITE),
                new Team("MEXICO", new Color(0, 100, 60), Color.WHITE),
                new Team("CANADA", new Color(220, 30, 30), Color.WHITE),
                new Team("BRAZIL", new Color(255, 210, 0), new Color(0, 130, 60)),
                new Team("ARGENTINA", new Color(100, 180, 255), Color.WHITE),
                new Team("FRANCE", new Color(0, 50, 180), Color.WHITE),
                new Team("GERMANY", Color.BLACK, Color.RED),
                new Team("SPAIN", new Color(200, 30, 30), new Color(255, 210, 0))
        };
    }

    // -----------------------------
    // Player
    // -----------------------------
    private static class Player {
        private double x, y;
        private double vx, vy;
        private final int radius = 24;
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
        private final int radius = 11;

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

            if (y - radius < FIELD_Y) { y = FIELD_Y + radius; vy = -vy * 0.8; }
            if (y + radius > FIELD_Y + FIELD_H) { y = FIELD_Y + FIELD_H - radius; vy = -vy * 0.8; }

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

            double sp = Math.hypot(vx, vy);
            if (sp > 20) { vx = vx / sp * 20; vy = vy / sp * 20; }
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
            vx *= 0.94; vy *= 0.94;
            vy += 0.1;
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

        Trail(double x, double y) {
            this.x = x; this.y = y; this.life = 25;
        }

        void age() { life--; }
        int getLife() { return life; }
        double getX() { return x; }
        double getY() { return y; }
    }

    // -----------------------------
    // Confetti
    // -----------------------------
    private static class Confetti {
        private double x, y, vx, vy;
        private final Color color;
        private int life;

        Confetti(double x, double y, double vx, double vy, Color color, int life) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.color = color;
            this.life = life;
        }

        void update() {
            x += vx;
            y += vy;
            vx *= 0.98;
            vy += 0.05;
            life--;
        }

        int getLife() { return life; }
        Color getColor() { return color; }
        double getX() { return x; }
        double getY() { return y; }
    }

    // -----------------------------
    // Crowd member
    // -----------------------------
    private static class CrowdMember {
        private int x, y;
        private Color color;

        CrowdMember(int x, int y, Color color) {
            this.x = x; this.y = y; this.color = color;
        }

        int getX() { return x; }
        int getY() { return y; }
        Color getColor() { return color; }
    }

    // -----------------------------
    // AI Controller
    // -----------------------------
    private static class AIController {
        private final Player self;
        private final Ball ball;
        private final FIFA2026Game game;
        private double thinkTimer = 0;
        private double targetX, targetY;

        AIController(Player self, Ball ball, FIFA2026Game game) {
            this.self = self;
            this.ball = ball;
            this.game = game;
            this.targetX = self.getX();
            this.targetY = self.getY();
        }

        void update(double dt) {
            thinkTimer -= dt;
            if (thinkTimer <= 0) {
                thinkTimer = 0.12 + game.rng.nextDouble() * 0.08;
                decideTarget();
            }

            double dx = targetX - self.getX();
            double dy = targetY - self.getY();
            double dist = Math.hypot(dx, dy);
            if (dist > 1) {
                double nx = dx / dist;
                double ny = dy / dist;
                self.applyForce(nx * PLAYER_SPEED * 0.98, ny * PLAYER_SPEED * 0.98);
            }

            double toBallX = ball.getX() - self.getX();
            double toBallY = ball.getY() - self.getY();
            double ballDist = Math.hypot(toBallX, toBallY);
            if (ballDist < self.getRadius() + ball.getRadius() + 10) {
                int goalX = FIELD_X + 25;
                int goalY = FIELD_Y + FIELD_H / 2 + (game.rng.nextInt(100) - 50);
                double aimX = goalX - ball.getX();
                double aimY = goalY - ball.getY();
                double aimLen = Math.hypot(aimX, aimY);
                if (aimLen > 0) {
                    aimX /= aimLen; aimY /= aimLen;
                    double power = KICK_POWER * (0.9 + game.rng.nextDouble() * 0.2);
                    ball.setVelocity(aimX * power + self.getVX() * 0.3,
                            aimY * power + self.getVY() * 0.3);
                    game.spawnKickParticles(ball.getX(), ball.getY(), self.getColor());
                }
            }
        }

        private void decideTarget() {
            int myGoalX = FIELD_X + FIELD_W - 50;
            int myGoalY = FIELD_Y + FIELD_H / 2;

            boolean ballOnOurSide = ball.getX() > FIELD_X + FIELD_W / 2;
            boolean ballMovingToward = ball.getVX() > 0.5;

            if (ballOnOurSide && ballMovingToward &&
                    Math.abs(ball.getX() - myGoalX) < 280) {
                targetX = ball.getX() + 25;
                targetY = ball.getY();
            } else {
                targetX = ball.getX() + 18;
                targetY = ball.getY();
            }

            targetX += (game.rng.nextDouble() - 0.5) * 12;
            targetY += (game.rng.nextDouble() - 0.5) * 12;
        }
    }
}