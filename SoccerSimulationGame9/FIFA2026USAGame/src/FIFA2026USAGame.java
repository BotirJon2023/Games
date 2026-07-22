import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class FIFA2026USAGame extends JPanel implements ActionListener, KeyListener {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 800;
    private static final int FIELD_X = 90;
    private static final int FIELD_Y = 130;
    private static final int FIELD_W = WIDTH - 180;
    private static final int FIELD_H = HEIGHT - 240;
    private static final int GOAL_H = 210;
    private static final int GOAL_DEPTH = 38;
    private static final double FRICTION = 0.982;
    private static final double BALL_FRICTION = 0.993;
    private static final double PLAYER_SPEED = 3.4;
    private static final double PLAYER_BOOST = 5.2;
    private static final double KICK_POWER = 13.0;
    private static final double POWER_SHOT = 18.5;
    private static final int GAME_DURATION = 150;
    private static final int TARGET_FPS = 60;

    // -----------------------------
    // Game state
    // -----------------------------
    private enum State { MENU, STADIUM_SELECT, TEAM_SELECT, INTRO, PLAYING, GOAL, HALFTIME, GAMEOVER }
    private State state = State.MENU;
    private boolean vsComputer = true;

    private Player player1;
    private Player player2;
    private Ball ball;
    private AIController ai;
    private List<Particle> particles = new ArrayList<>();
    private List<Trail> trails = new ArrayList<>();
    private List<Firework> fireworks = new ArrayList<>();
    private List<CrowdFan> crowd = new ArrayList<>();

    private Stadium stadium;
    private Team team1, team2;
    private int stadiumChoice = 0;
    private int team1Choice = 0;
    private int team2Choice = 1;
    private int selectPhase = 0; // 0 = stadium, 1 = team1, 2 = team2

    private int score1 = 0;
    private int score2 = 0;
    private int timeLeft = GAME_DURATION;
    private long lastTick = System.currentTimeMillis();
    private long goalTimer = 0;
    private long introTimer = 0;
    private long halftimeTimer = 0;
    private String lastScorer = "";
    private String commentary = "";
    private long commentaryTimer = 0;

    private final Set<Integer> pressedKeys = new HashSet<>();
    private Timer gameTimer;
    private Random rng = new Random();
    private int frameCount = 0;
    private int crowdWavePhase = 0;

    // -----------------------------
    // Entry point
    // -----------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("FIFA 2026 - USA Stadium Edition");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            FIFA2026USAGame game = new FIFA2026USAGame();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // -----------------------------
    // Constructor
    // -----------------------------
    public FIFA2026USAGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        Timer animator = new Timer(1000 / TARGET_FPS, this);
        animator.start();

        gameTimer = new Timer(1000, e -> {
            if (state == State.PLAYING) {
                timeLeft--;
                if (timeLeft == GAME_DURATION / 2) {
                    state = State.HALFTIME;
                    halftimeTimer = System.currentTimeMillis();
                    setCommentary("HALF TIME - " + team1.name + " " + score1 + ":" + score2 + " " + team2.name);
                }
                if (timeLeft <= 0) {
                    state = State.GAMEOVER;
                    spawnGrandFireworks();
                }
                if (timeLeft == 30) setCommentary("FINAL MINUTES IN " + stadium.city + "!");
                if (timeLeft == 10) setCommentary("INJURY TIME!");
            }
        });
        gameTimer.start();

        initCrowd();
    }

    private void initCrowd() {
        crowd.clear();
        // Top stands
        for (int i = 0; i < 300; i++) {
            int x = FIELD_X - 40 + rng.nextInt(FIELD_W + 80);
            int y = 20 + rng.nextInt(FIELD_Y - 40);
            crowd.add(new CrowdFan(x, y, rng.nextInt(1000)));
        }
        // Bottom stands
        for (int i = 0; i < 300; i++) {
            int x = FIELD_X - 40 + rng.nextInt(FIELD_W + 80);
            int y = FIELD_Y + FIELD_H + 20 + rng.nextInt(HEIGHT - FIELD_Y - FIELD_H - 40);
            crowd.add(new CrowdFan(x, y, rng.nextInt(1000)));
        }
        // Side stands
        for (int i = 0; i < 200; i++) {
            int y = FIELD_Y + rng.nextInt(FIELD_H);
            int x;
            if (rng.nextBoolean()) x = rng.nextInt(FIELD_X - 20);
            else x = FIELD_X + FIELD_W + 20 + rng.nextInt(WIDTH - FIELD_X - FIELD_W - 20);
            crowd.add(new CrowdFan(x, y, rng.nextInt(1000)));
        }
    }

    // -----------------------------
    // Game setup
    // -----------------------------
    private void startMatch() {
        score1 = 0;
        score2 = 0;
        timeLeft = GAME_DURATION;
        particles.clear();
        trails.clear();
        fireworks.clear();

        stadium = Stadium.STADIUMS[stadiumChoice];
        team1 = Team.TEAMS[team1Choice];
        team2 = Team.TEAMS[team2Choice];

        int midY = FIELD_Y + FIELD_H / 2;
        int p1X = FIELD_X + 140;
        int p2X = FIELD_X + FIELD_W - 140;

        player1 = new Player(p1X, midY, team1.primaryColor, team1.secondaryColor, team1.name);
        player2 = new Player(p2X, midY, team2.primaryColor, team2.secondaryColor, team2.name);

        ball = new Ball(WIDTH / 2, HEIGHT / 2);
        ai = new AIController(player2, ball, this);
        state = State.INTRO;
        introTimer = System.currentTimeMillis();
        setCommentary("WELCOME TO " + stadium.name.toUpperCase() + " - " + stadium.city);
        initCrowd();
    }

    private void resetAfterGoal() {
        int midY = FIELD_Y + FIELD_H / 2;
        player1.setPosition(FIELD_X + 140, midY);
        player2.setPosition(FIELD_X + FIELD_W - 140, midY);
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
        crowdWavePhase = (crowdWavePhase + 1) % 360;

        update(dt);
        repaint();
    }

    private void update(double dt) {
        if (state == State.INTRO) {
            if (System.currentTimeMillis() - introTimer > 2800) state = State.PLAYING;
        } else if (state == State.PLAYING) {
            handleInput();
            if (vsComputer) ai.update(dt);
            player1.update();
            player2.update();
            ball.update();
            constrainPlayer(player1);
            constrainPlayer(player2);
            handleCollisions();
            checkGoal();
            updateTrails();
        } else if (state == State.GOAL) {
            if (System.currentTimeMillis() - goalTimer > 3200) {
                resetAfterGoal();
                state = State.PLAYING;
            }
        } else if (state == State.HALFTIME) {
            if (System.currentTimeMillis() - halftimeTimer > 3000) state = State.PLAYING;
        }
        updateParticles();
        updateFireworks();
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
            if (power >= POWER_SHOT) setCommentary("POWER SHOT by " + p.getLabel() + "!");
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
            lastScorer = team2.name;
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
        spawnFireworks();
        setCommentary("GOOOAL! " + lastScorer + " SCORES IN " + stadium.city + "!");
    }

    // -----------------------------
    // Particles / trails / fireworks
    // -----------------------------
    private void spawnKickParticles(double x, double y, Color c) {
        for (int i = 0; i < 14; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double sp = 1.5 + rng.nextDouble() * 3.5;
            particles.add(new Particle(x, y, Math.cos(ang) * sp, Math.sin(ang) * sp, c, 35));
        }
    }

    private void spawnGoalExplosion(double x, double y) {
        Color[] colors = { Color.YELLOW, Color.ORANGE, Color.WHITE, Color.RED, Color.MAGENTA, Color.CYAN };
        for (int i = 0; i < 160; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double sp = 2 + rng.nextDouble() * 8;
            Color c = colors[rng.nextInt(colors.length)];
            particles.add(new Particle(x, y, Math.cos(ang) * sp, Math.sin(ang) * sp, c, 70 + rng.nextInt(40)));
        }
    }

    private void spawnFireworks() {
        for (int i = 0; i < 6; i++) {
            double x = 150 + rng.nextInt(WIDTH - 300);
            double targetY = 80 + rng.nextInt(200);
            Color c = new Color(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
            fireworks.add(new Firework(x, HEIGHT, x, targetY, c, 60 + rng.nextInt(30)));
        }
    }

    private void spawnGrandFireworks() {
        for (int i = 0; i < 15; i++) {
            Timer t = new Timer(i * 200, ev -> spawnFireworks());
            t.setRepeats(false);
            t.start();
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

    private void updateFireworks() {
        Iterator<Firework> it = fireworks.iterator();
        while (it.hasNext()) {
            Firework f = it.next();
            f.update();
            if (f.isDead()) it.remove();
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

        if (state == State.MENU) {
            drawMainMenu(g);
        } else if (state == State.STADIUM_SELECT) {
            drawStadiumSelect(g);
        } else if (state == State.TEAM_SELECT) {
            drawTeamSelect(g);
        } else if (state == State.INTRO) {
            drawIntro(g);
        } else if (state == State.HALFTIME) {
            drawHalftime(g);
        } else {
            drawStadiumBackground(g);
            drawField(g);
            drawCrowd(g);
            drawTrails(g);
            drawGoals(g);
            drawPlayers(g);
            drawBall(g);
            drawParticles(g);
            drawFireworks(g);
            drawJumbotron(g);
            drawHUD(g);
            if (state == State.GOAL) drawGoalBanner(g);
            if (state == State.GAMEOVER) drawGameOver(g);
        }
    }

    private void drawMainMenu(Graphics2D g) {
        // USA themed background
        GradientPaint sky = new GradientPaint(0, 0, new Color(10, 20, 80),
                0, HEIGHT, new Color(80, 10, 20));
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // American flag stripes (subtle background)
        for (int i = 0; i < 13; i++) {
            Color c = (i % 2 == 0) ? new Color(180, 30, 45, 40) : new Color(255, 255, 255, 25);
            g.setColor(c);
            g.fillRect(0, i * (HEIGHT / 13), WIDTH, HEIGHT / 13);
        }

        // Stars
        g.setColor(new Color(255, 255, 255, 80));
        for (int i = 0; i < 50; i++) {
            int x = (i * 137) % WIDTH;
            int y = (i * 89) % HEIGHT;
            drawStar(g, x, y, 3);
        }

        // Title
        g.setFont(new Font("SansSerif", Font.BOLD, 84));
        GradientPaint titleGrad = new GradientPaint(0, 150, Color.WHITE, 0, 250, new Color(200, 220, 255));
        g.setPaint(titleGrad);
        String title = "FIFA 2026";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, WIDTH / 2 - fm.stringWidth(title) / 2, 200);

        g.setFont(new Font("SansSerif", Font.BOLD, 40));
        GradientPaint subGrad = new GradientPaint(0, 240, new Color(255, 200, 100), 0, 290, new Color(255, 100, 50));
        g.setPaint(subGrad);
        String sub = "UNITED STATES EDITION";
        g.drawString(sub, WIDTH / 2 - fm.stringWidth(sub) / 2 + 100, 270);

        // Eagle silhouette
        drawEagle(g, WIDTH / 2, 380, 80);

        // Buttons
        drawMenuButton(g, WIDTH / 2 - 220, 480, 440, 80, "1  -  Player vs Computer", new Color(100, 150, 255));
        drawMenuButton(g, WIDTH / 2 - 220, 590, 440, 80, "2  -  Two Players", new Color(255, 150, 100));

        // Footer
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(220, 220, 220));
        String hint1 = "P1: WASD move, SPACE kick, SHIFT power shot";
        String hint2 = "P2: Arrows move, ENTER kick, SHIFT power shot";
        g.drawString(hint1, WIDTH / 2 - fm.stringWidth(hint1) / 2, 720);
        g.drawString(hint2, WIDTH / 2 - fm.stringWidth(hint2) / 2, 745);
    }

    private void drawEagle(Graphics2D g, int cx, int cy, int size) {
        g.setColor(new Color(80, 50, 20));
        // Body
        g.fillOval(cx - size / 4, cy - size / 6, size / 2, size / 3);
        // Wings
        int[] xWing = {cx - size, cx - size / 4, cx + size / 4, cx + size};
        int[] yWing = {cy - size / 3, cy - size / 8, cy - size / 8, cy - size / 3};
        g.fillPolygon(xWing, yWing, 4);
        // Head
        g.setColor(Color.WHITE);
        g.fillOval(cx - size / 8, cy - size / 3, size / 4, size / 4);
        // Beak
        g.setColor(new Color(255, 200, 0));
        int[] xBeak = {cx, cx + size / 6, cx};
        int[] yBeak = {cy - size / 5, cy - size / 7, cy - size / 10};
        g.fillPolygon(xBeak, yBeak, 3);
    }

    private void drawStar(Graphics2D g, int cx, int cy, int r) {
        int[] x = new int[10];
        int[] y = new int[10];
        for (int i = 0; i < 10; i++) {
            double ang = -Math.PI / 2 + i * Math.PI / 5;
            int rad = (i % 2 == 0) ? r : r / 2;
            x[i] = cx + (int) (Math.cos(ang) * rad);
            y[i] = cy + (int) (Math.sin(ang) * rad);
        }
        g.fillPolygon(x, y, 10);
    }

    private void drawStadiumSelect(Graphics2D g) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(20, 30, 60), 0, HEIGHT, new Color(60, 20, 30));
        g.setPaint(bg);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        g.setColor(Color.YELLOW);
        String title = "SELECT STADIUM";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, WIDTH / 2 - fm.stringWidth(title) / 2, 80);

        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        g.drawString("Press 1-8 to choose, ENTER to confirm", WIDTH / 2 - 250, 120);

        int cols = 4;
        int rows = 2;
        int boxW = 240, boxH = 240;
        int startX = WIDTH / 2 - (cols * boxW + (cols - 1) * 20) / 2;
        int startY = 160;

        for (int i = 0; i < Stadium.STADIUMS.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (boxW + 20);
            int y = startY + row * (boxH + 20);

            Stadium s = Stadium.STADIUMS[i];
            boolean selected = i == stadiumChoice;

            GradientPaint gp = new GradientPaint(x, y, s.skyColor1, x + boxW, y + boxH, s.skyColor2);
            g.setPaint(gp);
            g.fillRoundRect(x, y, boxW, boxH, 15, 15);

            g.setColor(selected ? Color.YELLOW : new Color(255, 255, 255, 100));
            g.setStroke(new BasicStroke(selected ? 4 : 1));
            g.drawRoundRect(x, y, boxW, boxH, 15, 15);

            // Stadium silhouette
            g.setColor(new Color(0, 0, 0, 120));
            int[] xStad = {x + 20, x + 40, x + 60, x + boxW - 60, x + boxW - 40, x + boxW - 20};
            int[] yStad = {y + 140, y + 100, y + 80, y + 80, y + 100, y + 140};
            g.fillPolygon(xStad, yStad, 6);

            // Lights
            g.setColor(new Color(255, 240, 180, 200));
            g.fillOval(x + 30, y + 70, 8, 8);
            g.fillOval(x + boxW - 38, y + 70, 8, 8);

            // Stadium name
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(Color.WHITE);
            g.drawString(s.name, x + 15, y + 180);

            // City
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(220, 220, 220));
            g.drawString(s.city, x + 15, y + 200);

            // Capacity
            g.setFont(new Font("SansSerif", Font.ITALIC, 12));
            g.setColor(new Color(200, 200, 200));
            g.drawString("Cap: " + s.capacity, x + 15, y + 220);

            // Number
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.setColor(Color.YELLOW);
            g.drawString(String.valueOf(i + 1), x + boxW - 30, y + 30);
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(180, 180, 180));
        g.drawString("ESC to go back", 30, HEIGHT - 20);
    }

    private void drawTeamSelect(Graphics2D g) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(20, 30, 60), 0, HEIGHT, new Color(60, 20, 30));
        g.setPaint(bg);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setFont(new Font("SansSerif", Font.BOLD, 44));
        g.setColor(Color.YELLOW);
        String title = "SELECT YOUR TEAM";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, WIDTH / 2 - fm.stringWidth(title) / 2, 80);

        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        String prompt = selectPhase == 1 ? "Player 1 - Press 1-8" : "Player 2 - Press 1-8";
        g.drawString(prompt, WIDTH / 2 - fm.stringWidth(prompt) / 2, 120);

        int cols = 4;
        int boxW = 200, boxH = 170;
        int startX = WIDTH / 2 - (cols * boxW + (cols - 1) * 20) / 2;
        int startY = 160;

        for (int i = 0; i < Team.TEAMS.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (boxW + 20);
            int y = startY + row * (boxH + 20);

            Team t = Team.TEAMS[i];
            boolean selected = (selectPhase == 1 && i == team1Choice) || (selectPhase == 2 && i == team2Choice);

            g.setColor(selected ? new Color(100, 100, 150) : new Color(40, 40, 60));
            g.fillRoundRect(x, y, boxW, boxH, 15, 15);
            g.setColor(selected ? Color.YELLOW : Color.GRAY);
            g.setStroke(new BasicStroke(selected ? 3 : 1));
            g.drawRoundRect(x, y, boxW, boxH, 15, 15);

            // Flag
            g.setColor(t.primaryColor);
            g.fillRect(x + 20, y + 20, 160, 55);
            g.setColor(t.secondaryColor);
            g.fillRect(x + 20, y + 50, 160, 25);

            // Team name
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.setColor(Color.WHITE);
            g.drawString(t.name, x + boxW / 2 - fm.stringWidth(t.name) / 2, y + 115);

            // Number
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(Color.YELLOW);
            g.drawString(String.valueOf(i + 1), x + 10, y + 25);
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(new Color(200, 200, 200));
        g.drawString("Press ENTER to confirm  |  ESC to go back", WIDTH / 2 - 200, HEIGHT - 40);
    }

    private void drawIntro(Graphics2D g) {
        drawStadiumBackground(g);
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Stadium name
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.setColor(new Color(255, 220, 100));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(stadium.name + " - " + stadium.city, WIDTH / 2 - fm.stringWidth(stadium.name + " - " + stadium.city) / 2, 200);

        // VS
        g.setFont(new Font("SansSerif", Font.BOLD, 80));
        GradientPaint gp = new GradientPaint(0, HEIGHT / 2 - 50, Color.WHITE, 0, HEIGHT / 2 + 50, new Color(200, 220, 255));
        g.setPaint(gp);
        String vs = team1.name + "  vs  " + team2.name;
        g.drawString(vs, WIDTH / 2 - fm.stringWidth(vs) / 2, HEIGHT / 2);

        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        g.setColor(Color.YELLOW);
        g.drawString("FIFA 2026 WORLD CUP", WIDTH / 2 - 180, HEIGHT / 2 + 80);

        // American flag
        drawAmericanFlag(g, WIDTH / 2 - 60, HEIGHT / 2 + 120, 120, 70);
    }

    private void drawAmericanFlag(Graphics2D g, int x, int y, int w, int h) {
        // Stripes
        for (int i = 0; i < 13; i++) {
            g.setColor((i % 2 == 0) ? new Color(180, 30, 45) : Color.WHITE);
            g.fillRect(x, y + i * (h / 13), w, h / 13 + 1);
        }
        // Blue canton
        g.setColor(new Color(40, 60, 140));
        g.fillRect(x, y, w / 2, h * 7 / 13);
        // Stars
        g.setColor(Color.WHITE);
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 6; col++) {
                int sx = x + 8 + col * (w / 2 - 16) / 5;
                int sy = y + 6 + row * (h * 7 / 13 - 12) / 4;
                drawStar(g, sx, sy, 3);
            }
        }
    }

    private void drawStadiumBackground(Graphics2D g) {
        // Sky gradient based on stadium
        GradientPaint sky = new GradientPaint(0, 0, stadium.skyColor1, 0, HEIGHT, stadium.skyColor2);
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Stadium stands (upper tier)
        g.setColor(stadium.standColor.darker());
        g.fillRect(0, 0, WIDTH, FIELD_Y - 10);
        g.fillRect(0, FIELD_Y + FIELD_H + 10, WIDTH, HEIGHT - FIELD_Y - FIELD_H - 10);
        g.fillRect(0, FIELD_Y, FIELD_X - 10, FIELD_H);
        g.fillRect(FIELD_X + FIELD_W + 10, FIELD_Y, WIDTH - FIELD_X - FIELD_W - 10, FIELD_H);

        // Stand tier lines
        g.setColor(new Color(0, 0, 0, 60));
        for (int i = 0; i < 5; i++) {
            g.drawLine(0, 20 + i * 20, WIDTH, 20 + i * 20);
            int by = FIELD_Y + FIELD_H + 20 + i * 20;
            if (by < HEIGHT) g.drawLine(0, by, WIDTH, by);
        }

        // Stadium lights (4 corner towers)
        drawStadiumLight(g, FIELD_X - 30, 30);
        drawStadiumLight(g, FIELD_X + FIELD_W + 30, 30);
        drawStadiumLight(g, FIELD_X - 30, HEIGHT - 60);
        drawStadiumLight(g, FIELD_X + FIELD_W + 30, HEIGHT - 60);

        // American flags on stands
        drawAmericanFlag(g, 40, 40, 60, 35);
        drawAmericanFlag(g, WIDTH - 100, 40, 60, 35);
    }

    private void drawStadiumLight(Graphics2D g, int x, int y) {
        // Tower
        g.setColor(new Color(60, 60, 70));
        g.fillRect(x - 4, y, 8, 40);
        // Light bank
        g.setColor(new Color(40, 40, 50));
        g.fillRect(x - 25, y - 15, 50, 20);
        // Glow
        RadialGradientPaint glow = new RadialGradientPaint(
                x, y, 80,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 240, 180, 180), new Color(255, 240, 180, 0)});
        g.setPaint(glow);
        g.fillOval(x - 80, y - 80, 160, 160);
    }

    private void drawCrowd(Graphics2D g) {
        for (CrowdFan f : crowd) {
            // Crowd wave effect
            double wave = Math.sin((crowdWavePhase + f.phase) * 0.05);
            int bob = (int) (wave * 3);
            Color c = f.getColor(stadium, team1, team2);
            g.setColor(c);
            g.fillOval(f.getX(), f.getY() + bob, 4, 6);
        }
    }

    private void drawField(Graphics2D g) {
        // Grass with stripes
        for (int i = 0; i < 14; i++) {
            Color c = (i % 2 == 0) ? new Color(35, 130, 55) : new Color(45, 150, 65);
            g.setColor(c);
            int stripeW = FIELD_W / 14;
            g.fillRect(FIELD_X + i * stripeW, FIELD_Y, stripeW + 1, FIELD_H);
        }

        // Field outline
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));
        g.drawRect(FIELD_X, FIELD_Y, FIELD_W, FIELD_H);

        // Center line
        g.drawLine(FIELD_X + FIELD_W / 2, FIELD_Y, FIELD_X + FIELD_W / 2, FIELD_Y + FIELD_H);

        // Center circle
        g.drawOval(FIELD_X + FIELD_W / 2 - 80, FIELD_Y + FIELD_H / 2 - 80, 160, 160);
        g.fillOval(FIELD_X + FIELD_W / 2 - 4, FIELD_Y + FIELD_H / 2 - 4, 8, 8);

        // Penalty boxes
        int pbW = 160, pbH = 340;
        int pbY = FIELD_Y + FIELD_H / 2 - pbH / 2;
        g.drawRect(FIELD_X, pbY, pbW, pbH);
        g.drawRect(FIELD_X + FIELD_W - pbW, pbY, pbW, pbH);

        // Goal boxes
        int gbW = 70, gbH = 200;
        int gbY = FIELD_Y + FIELD_H / 2 - gbH / 2;
        g.drawRect(FIELD_X, gbY, gbW, gbH);
        g.drawRect(FIELD_X + FIELD_W - gbW, gbY, gbW, gbH);

        // Penalty arcs
        g.drawArc(FIELD_X + pbW - 60, FIELD_Y + FIELD_H / 2 - 60, 120, 120, -50, 100);
        g.drawArc(FIELD_X + FIELD_W - pbW - 60, FIELD_Y + FIELD_H / 2 - 60, 120, 120, 230, 100);

        // Corner arcs
        g.drawArc(FIELD_X - 14, FIELD_Y - 14, 28, 28, 270, 90);
        g.drawArc(FIELD_X + FIELD_W - 14, FIELD_Y - 14, 28, 28, 180, 90);
        g.drawArc(FIELD_X - 14, FIELD_Y + FIELD_H - 14, 28, 28, 0, 90);
        g.drawArc(FIELD_X + FIELD_W - 14, FIELD_Y + FIELD_H - 14, 28, 28, 90, 90);

        // FIFA 2026 USA center logo
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(255, 255, 255, 120));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("USA 2026", FIELD_X + FIELD_W / 2 - fm.stringWidth("USA 2026") / 2, FIELD_Y + FIELD_H / 2 + 5);
    }

    private void drawGoals(Graphics2D g) {
        int midY = FIELD_Y + FIELD_H / 2;
        int goalTop = midY - GOAL_H / 2;

        // Left goal
        g.setColor(new Color(255, 255, 255, 230));
        g.setStroke(new BasicStroke(5));
        g.drawRect(FIELD_X - GOAL_DEPTH, goalTop, GOAL_DEPTH, GOAL_H);
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(255, 255, 255, 110));
        for (int i = 0; i < GOAL_DEPTH; i += 7) {
            g.drawLine(FIELD_X - GOAL_DEPTH + i, goalTop, FIELD_X - GOAL_DEPTH + i, goalTop + GOAL_H);
        }
        for (int i = 0; i < GOAL_H; i += 7) {
            g.drawLine(FIELD_X - GOAL_DEPTH, goalTop + i, FIELD_X, goalTop + i);
        }

        // Right goal
        g.setColor(new Color(255, 255, 255, 230));
        g.setStroke(new BasicStroke(5));
        g.drawRect(FIELD_X + FIELD_W, goalTop, GOAL_DEPTH, GOAL_H);
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(255, 255, 255, 110));
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
        g.setColor(new Color(0, 0, 0, 100));
        g.fillOval((int) p.getX() - p.getRadius() + 4, (int) p.getY() - p.getRadius() + 8,
                p.getRadius() * 2, p.getRadius() * 2);

        // Running animation
        double speed = Math.hypot(p.getVX(), p.getVY());
        if (speed > 0.5) {
            int legOffset = (int) (Math.sin(frameCount * 0.3) * 5);
            g.setColor(p.getColor().darker());
            g.fillOval((int) p.getX() - 7, (int) p.getY() + p.getRadius() - 4 + legOffset, 7, 9);
            g.fillOval((int) p.getX() + 2, (int) p.getY() + p.getRadius() - 4 - legOffset, 7, 9);
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
        g.setColor(new Color(0, 0, 0, 120));
        g.fillOval((int) ball.getX() - ball.getRadius() + 3, (int) ball.getY() - ball.getRadius() + 7,
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

        // Pentagon pattern
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

    private void drawFireworks(Graphics2D g) {
        for (Firework f : fireworks) {
            if (!f.exploded) {
                // Rising rocket
                g.setColor(new Color(255, 200, 100));
                g.fillOval((int) f.x - 2, (int) f.y - 2, 4, 4);
                // Trail
                g.setColor(new Color(255, 150, 50, 150));
                g.fillOval((int) f.x - 1, (int) f.y + 4, 2, 8);
            } else {
                // Explosion sparks
                for (Spark s : f.sparks) {
                    int alpha = (int) (s.life * 4);
                    g.setColor(new Color(f.color.getRed(), f.color.getGreen(), f.color.getBlue(),
                            Math.max(0, Math.min(255, alpha))));
                    int size = 3 + (int) (s.life / 15);
                    g.fillOval((int) s.x - size / 2, (int) s.y - size / 2, size, size);
                }
            }
        }
    }

    private void drawJumbotron(Graphics2D g) {
        // Jumbotron at top center
        int jw = 300, jh = 60;
        int jx = WIDTH / 2 - jw / 2;
        int jy = 10;

        g.setColor(new Color(20, 20, 30));
        g.fillRoundRect(jx, jy, jw, jh, 10, 10);
        g.setColor(new Color(100, 100, 120));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(jx, jy, jw, jh, 10, 10);

        // Screen glow
        GradientPaint screen = new GradientPaint(jx, jy, new Color(30, 40, 80), jx + jw, jy + jh, new Color(80, 30, 40));
        g.setPaint(screen);
        g.fillRoundRect(jx + 5, jy + 5, jw - 10, jh - 10, 8, 8);

        // Team names
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(team1.primaryColor);
        g.drawString(team1.name, jx + 15, jy + 25);
        g.setColor(team2.primaryColor);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(team2.name, jx + jw - 15 - fm.stringWidth(team2.name), jy + 25);

        // Score
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(Color.WHITE);
        String score = score1 + " - " + score2;
        g.drawString(score, jx + jw / 2 - fm.stringWidth(score) / 2, jy + 50);
    }

    private void drawHUD(Graphics2D g) {
        // Timer
        int min = timeLeft / 60;
        int sec = timeLeft % 60;
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        String timeStr = String.format("%d:%02d", min, sec);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(timeStr, WIDTH / 2 - fm.stringWidth(timeStr) / 2, 100);

        // Commentary
        if (System.currentTimeMillis() - commentaryTimer < 3500) {
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.setColor(new Color(255, 255, 100));
            g.drawString(commentary, WIDTH / 2 - fm.stringWidth(commentary) / 2, 125);
        }

        // Stadium name at bottom
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(255, 220, 100));
        g.drawString(stadium.name + " - " + stadium.city, 20, HEIGHT - 40);

        // Mode indicator
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(200, 200, 200));
        String mode = vsComputer ? "vs Computer   [Esc = Menu]" : "2 Players   [Esc = Menu]";
        g.drawString(mode, 20, HEIGHT - 15);

        g.drawString("P1: WASD + Space (+Shift=Power)", 20, HEIGHT - 60);
        if (!vsComputer) g.drawString("P2: Arrows + Enter (+Shift=Power)", WIDTH - 250, HEIGHT - 60);
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
        g.fillRect(0, HEIGHT / 2 - 110, WIDTH, 220);

        // American flag banner edges
        drawAmericanFlag(g, 0, HEIGHT / 2 - 110, 100, 220);
        drawAmericanFlag(g, WIDTH - 100, HEIGHT / 2 - 110, 100, 220);

        g.setFont(new Font("SansSerif", Font.BOLD, 110));
        GradientPaint gp = new GradientPaint(0, HEIGHT / 2 - 50, Color.YELLOW, 0, HEIGHT / 2 + 50, Color.RED);
        g.setPaint(gp);
        String txt = "GOAL!";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(txt, WIDTH / 2 - fm.stringWidth(txt) / 2, HEIGHT / 2 + 40);

        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.setColor(Color.WHITE);
        g.drawString("Scored by " + lastScorer, WIDTH / 2 - 130, HEIGHT / 2 + 90);
    }

    private void drawHalftime(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setFont(new Font("SansSerif", Font.BOLD, 72));
        g.setColor(Color.YELLOW);
        FontMetrics fm = g.getFontMetrics();
        String title = "HALF TIME";
        g.drawString(title, WIDTH / 2 - fm.stringWidth(title) / 2, 300);

        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        g.setColor(Color.WHITE);
        String score = team1.name + "  " + score1 + " - " + score2 + "  " + team2.name;
        g.drawString(score, WIDTH / 2 - fm.stringWidth(score) / 2, 400);

        drawAmericanFlag(g, WIDTH / 2 - 80, 450, 160, 90);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 220));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setFont(new Font("SansSerif", Font.BOLD, 80));
        g.setColor(Color.YELLOW);
        FontMetrics fm = g.getFontMetrics();
        String title = "FULL TIME";
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

        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.setColor(Color.WHITE);
        g.drawString("Champions of " + stadium.city, WIDTH / 2 - 150, 460);

        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        String hint = "Press  R  to restart  |  Press  Esc  for menu";
        g.drawString(hint, WIDTH / 2 - fm.stringWidth(hint) / 2, 540);
    }

    // -----------------------------
    // Key handling
    // -----------------------------
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        pressedKeys.add(k);

        if (state == State.MENU) {
            if (k == KeyEvent.VK_1) { vsComputer = true; state = State.STADIUM_SELECT; selectPhase = 0; }
            else if (k == KeyEvent.VK_2) { vsComputer = false; state = State.STADIUM_SELECT; selectPhase = 0; }
        } else if (state == State.STADIUM_SELECT) {
            if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_8) stadiumChoice = k - KeyEvent.VK_1;
            else if (k == KeyEvent.VK_ENTER) { state = State.TEAM_SELECT; selectPhase = 1; }
            else if (k == KeyEvent.VK_ESCAPE) state = State.MENU;
        } else if (state == State.TEAM_SELECT) {
            if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_8) {
                int choice = k - KeyEvent.VK_1;
                if (selectPhase == 1) team1Choice = choice;
                else if (selectPhase == 2) team2Choice = choice;
            } else if (k == KeyEvent.VK_ENTER) {
                if (selectPhase == 1) {
                    if (vsComputer) {
                        team2Choice = (team1Choice + 1) % Team.TEAMS.length;
                        startMatch();
                    } else {
                        selectPhase = 2;
                    }
                } else if (selectPhase == 2) {
                    startMatch();
                }
            } else if (k == KeyEvent.VK_ESCAPE) {
                if (selectPhase == 2) selectPhase = 1;
                else state = State.STADIUM_SELECT;
            }
        } else if (state == State.GAMEOVER) {
            if (k == KeyEvent.VK_R) startMatch();
            else if (k == KeyEvent.VK_ESCAPE) state = State.MENU;
        } else if (state == State.PLAYING || state == State.GOAL || state == State.HALFTIME) {
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
    // Stadium
    // -----------------------------
    private static class Stadium {
        String name;
        String city;
        int capacity;
        Color skyColor1, skyColor2;
        Color standColor;

        Stadium(String name, String city, int capacity, Color sky1, Color sky2, Color stand) {
            this.name = name;
            this.city = city;
            this.capacity = capacity;
            this.skyColor1 = sky1;
            this.skyColor2 = sky2;
            this.standColor = stand;
        }

        static final Stadium[] STADIUMS = {
                new Stadium("MetLife Stadium", "New York / New Jersey", 82500,
                        new Color(30, 40, 90), new Color(10, 15, 40), new Color(60, 70, 100)),
                new Stadium("SoFi Stadium", "Los Angeles", 70240,
                        new Color(80, 50, 30), new Color(30, 15, 10), new Color(100, 80, 60)),
                new Stadium("AT&T Stadium", "Dallas", 80000,
                        new Color(20, 30, 70), new Color(5, 10, 30), new Color(50, 60, 90)),
                new Stadium("Hard Rock Stadium", "Miami", 65326,
                        new Color(40, 80, 120), new Color(10, 30, 60), new Color(70, 100, 130)),
                new Stadium("NRG Stadium", "Houston", 72220,
                        new Color(50, 30, 80), new Color(20, 10, 40), new Color(80, 60, 110)),
                new Stadium("Lumen Field", "Seattle", 69000,
                        new Color(30, 60, 50), new Color(10, 25, 20), new Color(60, 90, 80)),
                new Stadium("Gillette Stadium", "Foxborough", 65878,
                        new Color(40, 40, 60), new Color(15, 15, 30), new Color(70, 70, 90)),
                new Stadium("Levi's Stadium", "San Francisco", 68500,
                        new Color(60, 70, 90), new Color(20, 25, 40), new Color(90, 100, 120))
        };
    }

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
        private double x, y, vx, vy;
        private final int radius = 24;
        private final Color color;
        private final Color light;
        private final String label;

        Player(double x, double y, Color color, Color light, String label) {
            this.x = x; this.y = y;
            this.color = color; this.light = light; this.label = label;
        }

        void applyForce(double fx, double fy) {
            vx += fx; vy += fy;
            double sp = Math.hypot(vx, vy);
            if (sp > PLAYER_BOOST) { vx = vx / sp * PLAYER_BOOST; vy = vy / sp * PLAYER_BOOST; }
        }

        void update() {
            x += vx; y += vy;
            vx *= FRICTION; vy *= FRICTION;
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
        private double x, y, vx, vy;
        private final int radius = 11;

        Ball(double x, double y) { this.x = x; this.y = y; }

        void reset(double nx, double ny) { x = nx; y = ny; vx = 0; vy = 0; }

        void update() {
            x += vx; y += vy;
            vx *= BALL_FRICTION; vy *= BALL_FRICTION;

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
            this.color = color; this.life = life;
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

        Trail(double x, double y) { this.x = x; this.y = y; this.life = 25; }

        void age() { life--; }
        int getLife() { return life; }
        double getX() { return x; }
        double getY() { return y; }
    }

    // -----------------------------
    // Firework (with sparks)
    // -----------------------------
    private static class Firework {
        double x, y;
        double targetX, targetY;
        double vy;
        Color color;
        boolean exploded;
        List<Spark> sparks = new ArrayList<>();
        int life;

        Firework(double x, double y, double targetX, double targetY, Color color, int life) {
            this.x = x; this.y = y;
            this.targetX = targetX; this.targetY = targetY;
            this.color = color;
            this.life = life;
            this.vy = -8 - Math.random() * 3;
            this.exploded = false;
        }

        void update() {
            if (!exploded) {
                y += vy;
                vy += 0.15;
                if (y <= targetY || vy >= 0) {
                    exploded = true;
                    for (int i = 0; i < 60; i++) {
                        double ang = Math.random() * Math.PI * 2;
                        double sp = 1 + Math.random() * 5;
                        sparks.add(new Spark(x, y, Math.cos(ang) * sp, Math.sin(ang) * sp, 50 + (int)(Math.random() * 30)));
                    }
                }
            } else {
                for (Spark s : sparks) s.update();
                sparks.removeIf(s -> s.life <= 0);
                life--;
            }
        }

        boolean isDead() {
            return exploded && sparks.isEmpty();
        }
    }

    private static class Spark {
        double x, y, vx, vy;
        int life;

        Spark(double x, double y, double vx, double vy, int life) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.life = life;
        }

        void update() {
            x += vx; y += vy;
            vx *= 0.95; vy *= 0.95;
            vy += 0.08;
            life--;
        }
    }

    // -----------------------------
    // Crowd Fan
    // -----------------------------
    private static class CrowdFan {
        private int x, y;
        private int phase;
        private int colorType; // 0 = random, 1 = team1, 2 = team2, 3 = USA

        CrowdFan(int x, int y, int phase) {
            this.x = x; this.y = y; this.phase = phase;
            this.colorType = (int) (Math.random() * 4);
        }

        int getX() { return x; }
        int getY() { return y; }

        Color getColor(Stadium s, Team t1, Team t2) {
            switch (colorType) {
                case 1: return t1.primaryColor;
                case 2: return t2.primaryColor;
                case 3: return new Color(200, 30, 50); // USA red
                default:
                    int r = 80 + (int) (Math.random() * 150);
                    int g = 80 + (int) (Math.random() * 150);
                    int b = 80 + (int) (Math.random() * 150);
                    return new Color(r, g, b);
            }
        }
    }

    // -----------------------------
    // AI Controller
    // -----------------------------
    private static class AIController {
        private final Player self;
        private final Ball ball;
        private final FIFA2026USAGame game;
        private double thinkTimer = 0;
        private double targetX, targetY;

        AIController(Player self, Ball ball, FIFA2026USAGame game) {
            this.self = self; this.ball = ball; this.game = game;
            this.targetX = self.getX(); this.targetY = self.getY();
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