import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.Timer;


public class FIFA2026Game extends JPanel implements ActionListener, KeyListener {

    // Game constants
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FPS = 60;
    private static final double FRICTION = 0.96;
    private static final double BALL_FRICTION = 0.988;
    private static final double SPRINT_MULTIPLIER = 1.6;
    private static final int GOAL_WIDTH = 140;
    private static final int GOAL_HEIGHT = 320;
    private static final int PLAYER_RADIUS = 20;
    private static final int BALL_RADIUS = 13;
    private static final int MATCH_TIME = 180000; // 3 minutes

    // Game state
    private enum GameState { MENU, PLAYING, PAUSED, GOAL_CELEBRATION, MATCH_END }
    private enum Weather { CLEAR, RAIN }
    private GameState gameState = GameState.MENU;
    private GameMode gameMode = GameMode.MENU;
    private enum GameMode { MENU, TWO_PLAYER, VS_CPU }
    private Weather weather = Weather.CLEAR;

    private javax.swing.Timer gameTimer;
    private long matchStartTime, pauseStartTime, totalPausedTime;
    private int scoreTeam1 = 0, scoreTeam2 = 0;
    private int possession = 50; // 0-100, 50 = even
    private long possessionStartTeam = 0;

    // Entities
    private Ball ball;
    private Team team1, team2;
    private List<Particle> particles;
    private List<RainDrop> rainDrops;
    private List<Trail> ballTrail, playerTrails;
    private List<CameraShake> cameraShakes;
    private Goal leftGoal, rightGoal;
    private Referee referee;

    // Camera & Effects
    private double cameraX = 0, cameraY = 0, cameraZoom = 1.0;
    private float celebrationTimer = 0;
    private String celebrationText = "";
    private Point celebrationPos;
    private Color celebrationColor;

    // Input
    private Set<Integer> keysPressed;
    private Map<String, Boolean> keyActions;

    // Graphics
    private Random random;
    private Font titleFont, scoreFont, uiFont, statsFont;
    private GradientPaint fieldGradient, skyGradient;
    private TexturePaint grassTexture;
    private BufferedImage crowdImage;

    // Stats
    private int shotsTeam1, shotsTeam2;
    private int passesTeam1, passesTeam2;

    public FIFA2026Game() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(25, 100, 25));
        setFocusable(true);
        addKeyListener(this);

        initGraphics();
        initGame();

        gameTimer = new javax.swing.Timer(1000 / FPS, this);
        gameTimer.start();
    }

    private void initGraphics() {
        random = new Random();
        titleFont = new Font("Arial", Font.BOLD, 52);
        scoreFont = new Font("Arial", Font.BOLD, 42);
        uiFont = new Font("Arial", Font.PLAIN, 16);
        statsFont = new Font("Arial", Font.BOLD, 14);

        // Field gradient with lighting
        fieldGradient = new GradientPaint(0, 0, new Color(30, 120, 30),
                0, HEIGHT, new Color(20, 80, 20));
        skyGradient = new GradientPaint(0, 0, new Color(135, 206, 250),
                0, 100, new Color(25, 100, 25));

        // Grass texture
        BufferedImage grassImg = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = grassImg.createGraphics();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int shade = 25 + random.nextInt(15);
                g2.setColor(new Color(shade, 100 + shade, shade));
                g2.fillRect(i, j, 1, 1);
            }
        }
        g2.dispose();
        grassTexture = new TexturePaint(grassImg, new Rectangle(0, 0, 8, 8));

        // Crowd background
        crowdImage = new BufferedImage(WIDTH, 60, BufferedImage.TYPE_INT_RGB);
        Graphics2D cg = crowdImage.createGraphics();
        for (int i = 0; i < WIDTH; i += 3) {
            for (int j = 0; j < 60; j += 3) {
                cg.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(150)));
                cg.fillRect(i, j, 3, 3);
            }
        }
        cg.dispose();
    }

    private void initGame() {
        keysPressed = new HashSet<>();
        keyActions = new HashMap<>();
        particles = new ArrayList<>();
        rainDrops = new ArrayList<>();
        ballTrail = new ArrayList<>();
        playerTrails = new ArrayList<>();
        cameraShakes = new ArrayList<>();

        resetMatch();
    }

    private void resetMatch() {
        ball = new Ball(WIDTH / 2.0, HEIGHT / 2.0);

        // Team 1 - Blue
        team1 = new Team("USA", new Color(0, 40, 200), false);
        team1.addPlayer(new Player(150, HEIGHT / 2.0, "ST"));
        team1.addPlayer(new Player(300, HEIGHT / 2.0 - 100, "LW"));
        team1.addPlayer(new Player(300, HEIGHT / 2.0 + 100, "RW"));

        // Team 2 - Red
        team2 = new Team("GER", new Color(200, 20, 20), true);
        team2.addPlayer(new Player(WIDTH - 150, HEIGHT / 2.0, "ST"));
        team2.addPlayer(new Player(WIDTH - 300, HEIGHT / 2.0 - 100, "LW"));
        team2.addPlayer(new Player(WIDTH - 300, HEIGHT / 2.0 + 100, "RW"));

        leftGoal = new Goal(0, HEIGHT / 2 - GOAL_HEIGHT / 2, false);
        rightGoal = new Goal(WIDTH - 25, HEIGHT / 2 - GOAL_HEIGHT / 2, true);
        referee = new Referee(WIDTH / 2, 80);

        scoreTeam1 = scoreTeam2 = 0;
        shotsTeam1 = shotsTeam2 = 0;
        passesTeam1 = passesTeam2 = 0;
        totalPausedTime = 0;
        matchStartTime = System.currentTimeMillis();

        particles.clear();
        ballTrail.clear();
        playerTrails.clear();
        celebrationTimer = 0;

        // Random weather
        weather = random.nextDouble() < 0.3 ? Weather.RAIN : Weather.CLEAR;
        if (weather == Weather.RAIN) initRain();
    }

    private void initRain() {
        rainDrops.clear();
        for (int i = 0; i < 200; i++) {
            rainDrops.add(new RainDrop(random.nextInt(WIDTH), random.nextInt(HEIGHT)));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.MENU) {
            repaint();
            return;
        }
        if (gameState == GameState.PAUSED) {
            repaint();
            return;
        }

        updateGame();
        repaint();
    }

    private void updateGame() {
        updateTimer();

        if (gameState == GameState.GOAL_CELEBRATION) {
            updateCelebration();
            return;
        }

        if (gameState == GameState.MATCH_END) {
            return;
        }

        handleInput();
        updatePhysics();
        updateAI();
        updateEffects();
        checkCollisions();
        checkGoals();
        checkBoundaries();
        updateCamera();
        updateStats();
    }

    private void updateTimer() {
        if (gameState != GameState.PLAYING) return;
        long elapsed = System.currentTimeMillis() - matchStartTime - totalPausedTime;
        if (elapsed >= MATCH_TIME) {
            gameState = GameState.MATCH_END;
        }
    }

    private void handleInput() {
        keyActions.clear();

        // Player 1 controls
        Player p1 = team1.getControlledPlayer();
        p1.ax = p1.ay = 0;
        boolean p1Sprint = keysPressed.contains(KeyEvent.VK_K);
        double p1Speed = p1Sprint && p1.stamina > 0 ? SPRINT_MULTIPLIER : 1.0;

        if (keysPressed.contains(KeyEvent.VK_W)) p1.ay = -0.7 * p1Speed;
        if (keysPressed.contains(KeyEvent.VK_S)) p1.ay = 0.7 * p1Speed;
        if (keysPressed.contains(KeyEvent.VK_A)) p1.ax = -0.7 * p1Speed;
        if (keysPressed.contains(KeyEvent.VK_D)) p1.ax = 0.7 * p1Speed;
        if (p1Sprint) p1.stamina -= 0.5;

        if (keysPressed.contains(KeyEvent.VK_J)) keyActions.put("P1_SHOOT", true);
        if (keysPressed.contains(KeyEvent.VK_L)) keyActions.put("P1_TACKLE", true);

        // Player 2 or CPU
        if (gameMode == GameMode.TWO_PLAYER) {
            Player p2 = team2.getControlledPlayer();
            p2.ax = p2.ay = 0;
            boolean p2Sprint = keysPressed.contains(KeyEvent.VK_2);
            double p2Speed = p2Sprint && p2.stamina > 0 ? SPRINT_MULTIPLIER : 1.0;

            if (keysPressed.contains(KeyEvent.VK_UP)) p2.ay = -0.7 * p2Speed;
            if (keysPressed.contains(KeyEvent.VK_DOWN)) p2.ay = 0.7 * p2Speed;
            if (keysPressed.contains(KeyEvent.VK_LEFT)) p2.ax = -0.7 * p2Speed;
            if (keysPressed.contains(KeyEvent.VK_RIGHT)) p2.ax = 0.7 * p2Speed;
            if (p2Sprint) p2.stamina -= 0.5;

            if (keysPressed.contains(KeyEvent.VK_1)) keyActions.put("P2_SHOOT", true);
            if (keysPressed.contains(KeyEvent.VK_3)) keyActions.put("P2_TACKLE", true);
        }

        // Actions
        if (keyActions.getOrDefault("P1_SHOOT", false)) team1.getControlledPlayer().shoot(ball);
        if (keyActions.getOrDefault("P1_TACKLE", false)) team1.getControlledPlayer().tackle(getOpponentPlayer(team1));
        if (keyActions.getOrDefault("P2_SHOOT", false)) team2.getControlledPlayer().shoot(ball);
        if (keyActions.getOrDefault("P2_TACKLE", false)) team2.getControlledPlayer().tackle(getOpponentPlayer(team2));
    }

    private void updatePhysics() {
        team1.update();
        team2.update();
        ball.update();
        referee.update(ball);

        // Update particles
        particles.removeIf(p -> !p.isAlive());
        particles.forEach(Particle::update);

        // Update trails
        ballTrail.add(new Trail(ball.x, ball.y, ball.getSpeed()));
        if (ballTrail.size() > 20) ballTrail.remove(0);

        // Update rain
        if (weather == Weather.RAIN) {
            rainDrops.forEach(RainDrop::update);
        }

        // Camera shake
        cameraShakes.removeIf(CameraShake::isFinished);
        cameraShakes.forEach(CameraShake::update);
    }

    private void updateAI() {
        if (gameMode == GameMode.VS_CPU) {
            team2.updateAI(ball, team1);
        }
        // Auto-switch controlled player to closest to ball
        team1.autoSelectPlayer(ball);
        if (gameMode == GameMode.TWO_PLAYER) team2.autoSelectPlayer(ball);
    }

    private void updateEffects() {
        // Stamina regen
        team1.regenStamina();
        team2.regenStamina();

        // Weather effects on ball
        if (weather == Weather.RAIN) {
            ball.vx *= 0.998;
            ball.vy *= 0.998;
        }
    }

    private void checkCollisions() {
        // Player-ball for all players
        for (Player p : team1.players) checkPlayerBallCollision(p);
        for (Player p : team2.players) checkPlayerBallCollision(p);

        // Player-player collisions
        for (Player p1 : team1.players) {
            for (Player p2 : team2.players) {
                resolvePlayerCollision(p1, p2);
            }
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
                double impulse = -speed * 1.6;
                ball.vx += nx * impulse;
                ball.vy += ny * impulse;
                ball.vx += p.vx * 0.4;
                ball.vy += p.vy * 0.4;

                double overlap = PLAYER_RADIUS + BALL_RADIUS - dist;
                ball.x += nx * overlap;
                ball.y += ny * overlap;

                if (ball.getSpeed() > 2) createParticles(ball.x, ball.y, 6, Color.WHITE);

                // Update possession
                if (p.team == team1) possession = Math.min(100, possession + 2);
                else possession = Math.max(0, possession - 2);
            }
        }
    }

    private void resolvePlayerCollision(Player p1, Player p2) {
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < PLAYER_RADIUS * 2 && dist > 0) {
            double overlap = PLAYER_RADIUS * 2 - dist;
            double pushX = dx / dist * overlap / 2;
            double pushY = dy / dist * overlap / 2;
            p1.x -= pushX; p1.y -= pushY;
            p2.x += pushX; p2.y += pushY;

            double tempVx = p1.vx, tempVy = p1.vy;
            p1.vx = p2.vx * 0.6; p1.vy = p2.vy * 0.6;
            p2.vx = tempVx * 0.6; p2.vy = tempVy * 0.6;
        }
    }

    private void checkGoals() {
        // Left goal - Team 2 scores
        if (ball.x - BALL_RADIUS < 25 &&
                ball.y > HEIGHT / 2 - GOAL_HEIGHT / 2 &&
                ball.y < HEIGHT / 2 + GOAL_HEIGHT / 2) {
            goalScored(team2, new Point(60, HEIGHT / 2));
        }

        // Right goal - Team 1 scores
        if (ball.x + BALL_RADIUS > WIDTH - 25 &&
                ball.y > HEIGHT / 2 - GOAL_HEIGHT / 2 &&
                ball.y < HEIGHT / 2 + GOAL_HEIGHT / 2) {
            goalScored(team1, new Point(WIDTH - 60, HEIGHT / 2));
        }
    }

    private void goalScored(Team scoringTeam, Point pos) {
        if (scoringTeam == team1) {
            scoreTeam1++;
            shotsTeam1++;
        } else {
            scoreTeam2++;
            shotsTeam2++;
        }

        gameState = GameState.GOAL_CELEBRATION;
        celebrationTimer = 180; // 3 seconds
        celebrationPos = pos;
        celebrationText = "GOAL!";
        celebrationColor = scoringTeam.color;

        createParticles(pos.x, pos.y, 50, Color.YELLOW);
        createParticles(pos.x, pos.y, 30, scoringTeam.color);
        cameraShakes.add(new CameraShake(15, 30));

        // Reset positions
        Timer resetTimer = new Timer();
        resetTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ball.reset(WIDTH / 2.0, HEIGHT / 2.0);
                team1.resetFormation(false);
                team2.resetFormation(true);
            }
        }, 1500);
    }

    private void updateCelebration() {
        celebrationTimer--;
        if (celebrationTimer <= 0) {
            gameState = GameState.PLAYING;
        }
    }

    private void checkBoundaries() {
        // Ball boundaries with bounce
        if (ball.y - BALL_RADIUS < 60 || ball.y + BALL_RADIUS > HEIGHT - 60) {
            ball.vy *= -0.7;
            ball.y = Math.max(BALL_RADIUS + 60, Math.min(HEIGHT - BALL_RADIUS - 60, ball.y));
            createParticles(ball.x, ball.y, 4, Color.WHITE);
        }

        // Side lines - throw in
        if (ball.x - BALL_RADIUS < 0 || ball.x + BALL_RADIUS > WIDTH) {
            if (!(ball.y > HEIGHT / 2 - GOAL_HEIGHT / 2 && ball.y < HEIGHT / 2 + GOAL_HEIGHT / 2)) {
                ball.vx *= -0.7;
                ball.x = Math.max(BALL_RADIUS, Math.min(WIDTH - BALL_RADIUS, ball.x));
                createParticles(ball.x, ball.y, 4, Color.WHITE);
            }
        }

        // Constrain all players
        team1.constrainPlayers();
        team2.constrainPlayers();
    }

    private void updateCamera() {
        // Follow ball with smooth damping
        double targetX = ball.x - WIDTH / 2;
        double targetY = ball.y - HEIGHT / 2;
        cameraX += (targetX - cameraX) * 0.05;
        cameraY += (targetY - cameraY) * 0.05;

        // Apply camera shake
        for (CameraShake shake : cameraShakes) {
            cameraX += shake.getOffsetX();
            cameraY += shake.getOffsetY();
        }

        // Clamp camera
        cameraX = Math.max(-100, Math.min(100, cameraX));
        cameraY = Math.max(-50, Math.min(50, cameraY));
    }

    private void updateStats() {
        // Update possession percentage
        if (ball.lastTouchedBy == team1) {
            possession = Math.min(100, possession + 0.1);
        } else if (ball.lastTouchedBy == team2) {
            possession = Math.max(0, possession - 0.1);
        }
    }

    private Player getOpponentPlayer(Team team) {
        Team opp = team == team1 ? team2 : team1;
        return opp.getControlledPlayer();
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
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (gameState == GameState.MENU) {
            drawMenu(g2);
            return;
        }

        // Apply camera transform
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(-cameraX, -cameraY);

        drawSky(g2);
        drawField(g2);
        drawWeather(g2);

        // Draw game elements in order
        drawTrails(g2);
        leftGoal.draw(g2);
        rightGoal.draw(g2);
        particles.forEach(p -> p.draw(g2));
        team1.draw(g2);
        team2.draw(g2);
        ball.draw(g2);
        referee.draw(g2);

        g2.setTransform(oldTransform);

        // UI on top - not affected by camera
        drawUI(g2);
        drawCelebration(g2);
        if (gameState == GameState.PAUSED) drawPauseScreen(g2);
        if (gameState == GameState.MATCH_END) drawMatchEnd(g2);
    }

    private void drawSky(Graphics2D g2) {
        g2.setPaint(skyGradient);
        g2.fillRect(-200, -200, WIDTH + 400, 200);
        g2.drawImage(crowdImage, -200, -60, WIDTH + 400, 60, null);
    }

    private void drawField(Graphics2D g2) {
        g2.setPaint(fieldGradient);
        g2.fillRect(-200, 60, WIDTH + 400, HEIGHT + 400);
        g2.setPaint(grassTexture);
        g2.fillRect(30, 60, WIDTH - 60, HEIGHT - 120);

        // Field markings
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(4));
        g2.drawRect(30, 60, WIDTH - 60, HEIGHT - 120); // Boundary
        g2.drawLine(WIDTH / 2, 60, WIDTH / 2, HEIGHT - 60); // Half
        g2.drawOval(WIDTH / 2 - 90, HEIGHT / 2 - 90, 180, 180); // Center circle
        g2.fillOval(WIDTH / 2 - 6, HEIGHT / 2 - 6, 12, 12); // Center spot

        // Penalty areas
        g2.drawRect(30, HEIGHT / 2 - 180, 200, 360);
        g2.drawRect(WIDTH - 230, HEIGHT / 2 - 180, 200, 360);
        g2.drawRect(30, HEIGHT / 2 - 90, 90, 180); // Goal area
        g2.drawRect(WIDTH - 120, HEIGHT / 2 - 90, 90, 180);

        // Penalty spots and arcs
        g2.fillOval(160 - 5, HEIGHT / 2 - 5, 10, 10);
        g2.fillOval(WIDTH - 160 - 5, HEIGHT / 2 - 5, 10, 10);
        g2.drawArc(160 - 90, HEIGHT / 2 - 90, 180, 180, 0, 180);
        g2.drawArc(WIDTH - 160 - 90, HEIGHT / 2 - 90, 180, 180, 0, 180);

        // Corner arcs
        g2.drawArc(20, 50, 30, 30, 270, 90);
        g2.drawArc(WIDTH - 50, 50, 30, 30, 180, 90);
        g2.drawArc(20, HEIGHT - 80, 30, 30, 0, 90);
        g2.drawArc(WIDTH - 50, HEIGHT - 80, 30, 30, 90, 90);
    }

    private void drawWeather(Graphics2D g2) {
        if (weather == Weather.RAIN) {
            g2.setColor(new Color(200, 200, 255, 30));
            for (RainDrop drop : rainDrops) {
                drop.draw(g2);
            }
        }
    }

    private void drawTrails(Graphics2D g2) {
        // Ball trail
        for (int i = 0; i < ballTrail.size(); i++) {
            Trail t = ballTrail.get(i);
            float alpha = (float) i / ballTrail.size() * 0.4f;
            g2.setColor(new Color(1f, 1f, alpha));
            int size = (int) (BALL_RADIUS * 2 * alpha);
            g2.fillOval((int) t.x - size / 2, (int) t.y - size / 2, size, size);
        }
    }

    private void drawUI(Graphics2D g2) {
        // Score board
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(WIDTH / 2 - 150, 10, 300, 80, 20, 20);

        g2.setFont(scoreFont);
        g2.setColor(Color.WHITE);
        String score = scoreTeam1 + " - " + scoreTeam2;
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(score, WIDTH / 2 - fm.stringWidth(score) / 2, 50);

        // Time
        long elapsed = System.currentTimeMillis() - matchStartTime - totalPausedTime;
        long remaining = Math.max(0, MATCH_TIME - elapsed);
        String time = String.format("%02d:%02d", remaining / 60000, (remaining / 1000) % 60);
        g2.setFont(uiFont);
        g2.drawString(time, WIDTH / 2 - 25, 75);

        // Team names
        g2.setFont(statsFont);
        g2.setColor(team1.color);
        g2.drawString(team1.name, WIDTH / 2 - 140, 35);
        g2.setColor(team2.color);
        String team2Name = team2.name;
        g2.drawString(team2Name, WIDTH / 2 + 140 - fm.stringWidth(team2Name), 35);

        // Stats bar
        drawStatsBar(g2);

        // Controls
        g2.setFont(uiFont);
        g2.setColor(new Color(255, 255, 255, 200));
        String controls = gameMode == GameMode.TWO_PLAYER ?
                "P1: WASD+JKL | P2: Arrows+123" : "WASD+JKL | P=Pause R=Restart";
        g2.drawString(controls, 20, HEIGHT - 15);

        // Weather indicator
        if (weather == Weather.RAIN) {
            g2.drawString("RAIN", WIDTH - 80, HEIGHT - 15);
        }
    }

    private void drawStatsBar(Graphics2D g2) {
        int barY = HEIGHT - 50;
        int barWidth = 400;
        int barHeight = 20;
        int barX = WIDTH / 2 - barWidth / 2;

        // Background
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(barX, barY, barWidth, barHeight, 10, 10);

        // Possession
        int possWidth = (int) (barWidth * possession / 100.0);
        g2.setColor(team1.color);
        g2.fillRoundRect(barX, barY, possWidth, barHeight, 10, 10);
        g2.setColor(team2.color);
        g2.fillRoundRect(barX + possWidth, barY, barWidth - possWidth, barHeight, 10, 10);

        // Text
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString(possession + "%", barX + 5, barY + 14);
        g2.drawString((100 - possession) + "%", barX + barWidth - 35, barY + 14);
    }

    private void drawCelebration(Graphics2D g2) {
        if (celebrationTimer > 0) {
            float alpha = Math.min(1.0f, celebrationTimer / 60f);
            g2.setColor(new Color(celebrationColor.getRed(), celebrationColor.getGreen(),
                    celebrationColor.getBlue(), (int)(alpha * 255)));
            g2.setFont(new Font("Arial", Font.BOLD, 80));
            FontMetrics fm = g2.getFontMetrics();
            int textX = celebrationPos.x - fm.stringWidth(celebrationText) / 2;
            int textY = celebrationPos.y;

            // Outline
            g2.setColor(new Color(0, 0, 0, (int)(alpha * 255)));
            g2.drawString(celebrationText, textX - 3, textY - 3);
            g2.drawString(celebrationText, textX + 3, textY + 3);

            // Text
            g2.setColor(new Color(celebrationColor.getRed(), celebrationColor.getGreen(),
                    celebrationColor.getBlue(), (int)(alpha * 255)));
            g2.drawString(celebrationText, textX, textY);
        }
    }

    private void drawMenu(Graphics2D g2) {
        // Background
        g2.setPaint(new GradientPaint(0, 0, new Color(0, 20, 60),
                0, HEIGHT, new Color(0, 60, 120)));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Title
        g2.setFont(titleFont);
        g2.setColor(new Color(255, 215, 0));
        String title = "FIFA 2026";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, WIDTH / 2 - fm.stringWidth(title) / 2, 150);

        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.setColor(Color.WHITE);
        String subtitle = "Ultimate Soccer Simulation";
        fm = g2.getFontMetrics();
        g2.drawString(subtitle, WIDTH / 2 - fm.stringWidth(subtitle) / 2, 200);

        // Options
        g2.setFont(uiFont);
        String[] options = {
                "Press 1 - 2 Player Mode",
                "Press 2 - VS Computer",
                "",
                "Player 1: WASD + J(Kick) K(Sprint) L(Tackle)",
                "Player 2: Arrows + 1(Kick) 2(Sprint) 3(Tackle)",
                "",
                "P - Pause | R - Restart | ESC - Menu"
        };

        int y = 300;
        for (String opt : options) {
            fm = g2.getFontMetrics();
            g2.drawString(opt, WIDTH / 2 - fm.stringWidth(opt) / 2, y);
            y += 35;
        }

        // Last match result
        if (scoreTeam1 > 0 || scoreTeam2 > 0) {
            g2.setColor(Color.CYAN);
            String result = "Last Match: " + team1.name + " " + scoreTeam1 + " - " +
                    scoreTeam2 + " " + team2.name;
            fm = g2.getFontMetrics();
            g2.drawString(result, WIDTH / 2 - fm.stringWidth(result) / 2, y + 40);
        }
    }

    private void drawPauseScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        g2.setFont(titleFont);
        g2.setColor(Color.WHITE);
        String text = "PAUSED";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, WIDTH / 2 - fm.stringWidth(text) / 2, HEIGHT / 2);
        g2.setFont(uiFont);
        String resume = "Press P to Resume";
        fm = g2.getFontMetrics();
        g2.drawString(resume, WIDTH / 2 - fm.stringWidth(resume) / 2, HEIGHT / 2 + 50);
    }

    private void drawMatchEnd(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setFont(titleFont);
        g2.setColor(Color.YELLOW);
        String text = "FULL TIME";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, WIDTH / 2 - fm.stringWidth(text) / 2, 200);

        g2.setFont(scoreFont);
        g2.setColor(Color.WHITE);
        String score = team1.name + " " + scoreTeam1 + " - " + scoreTeam2 + " " + team2.name;
        fm = g2.getFontMetrics();
        g2.drawString(score, WIDTH / 2 - fm.stringWidth(score) / 2, 300);

        // Winner
        g2.setFont(uiFont);
        String winner = scoreTeam1 > scoreTeam2 ? team1.name + " WINS!" :
                scoreTeam2 > scoreTeam1 ? team2.name + " WINS!" : "DRAW!";
        g2.setColor(scoreTeam1 > scoreTeam2 ? team1.color :
                scoreTeam2 > scoreTeam1 ? team2.color : Color.WHITE);
        fm = g2.getFontMetrics();
        g2.drawString(winner, WIDTH / 2 - fm.stringWidth(winner) / 2, 380);

        // Stats
        g2.setColor(Color.WHITE);
        String stats = String.format("Shots: %d - %d | Possession: %d%% - %d%%",
                shotsTeam1, shotsTeam2, possession, 100 - possession);
        fm = g2.getFontMetrics();
        g2.drawString(stats, WIDTH / 2 - fm.stringWidth(stats) / 2, 450);

        g2.drawString("Press R to Play Again", WIDTH / 2 - 100, 520);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keysPressed.add(e.getKeyCode());

        if (gameState == GameState.MENU) {
            if (e.getKeyCode() == KeyEvent.VK_1) {
                gameMode = GameMode.TWO_PLAYER;
                gameState = GameState.PLAYING;
                resetMatch();
            } else if (e.getKeyCode() == KeyEvent.VK_2) {
                gameMode = GameMode.VS_CPU;
                gameState = GameState.PLAYING;
                resetMatch();
            }
        } else {
            if (e.getKeyCode() == KeyEvent.VK_P) {
                if (gameState == GameState.PLAYING) {
                    gameState = GameState.PAUSED;
                    pauseStartTime = System.currentTimeMillis();
                } else if (gameState == GameState.PAUSED) {
                    gameState = GameState.PLAYING;
                    totalPausedTime += System.currentTimeMillis() - pauseStartTime;
                }
            } else if (e.getKeyCode() == KeyEvent.VK_R) {
                resetMatch();
                gameState = GameState.PLAYING;
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                gameState = GameState.MENU;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keysPressed.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e)