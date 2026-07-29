import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GlobalRacingTourGame extends JPanel implements KeyListener, ActionListener {
    // Game constants
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 750;
    private static final int GROUND_Y = 650;
    private static final int MAX_LAPS = 3;
    private static final int TRACK_WIDTH = 800;
    private static final int TRACK_OFFSET_X = 100;

    // Game state
    private GameState state;
    private PlayerCar player1;
    private PlayerCar player2;
    private List<AICar> aiCars;
    private List<Obstacle> obstacles;
    private List<PowerUp> powerUps;
    private List<Particle> particles;
    private List<TrailEffect> trails;
    private TrackRenderer trackRenderer;
    private HUD hud;
    private Timer gameLoop;
    private Timer animationTimer;
    private int score1, score2;
    private int lap1, lap2;
    private long startTime;
    private boolean[] keys;
    private int frameCount;
    private float cameraX, cameraY;
    private boolean isNightMode;
    private float dayNightCycle;
    private WeatherEffect weather;
    private float[] trackPoints;
    private boolean gameMode2Player;
    private float difficultyLevel;

    // Animation properties
    private float cloudOffset;
    private float treeOffset;
    private float roadOffset;
    private List<SceneryObject> sceneryObjects;
    private float shakeIntensity;

    public GlobalRacingTourGame(boolean twoPlayer) {
        this.gameMode2Player = twoPlayer;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(30, 40, 60));
        setFocusable(true);
        addKeyListener(this);

        initializeGame();
        startGameLoop();
        startAnimationTimer();
    }

    private void initializeGame() {
        state = GameState.MENU;
        keys = new boolean[256];
        score1 = 0;
        score2 = 0;
        lap1 = 0;
        lap2 = 0;
        startTime = System.currentTimeMillis();
        frameCount = 0;
        cloudOffset = 0;
        treeOffset = 0;
        roadOffset = 0;
        isNightMode = false;
        dayNightCycle = 0;
        shakeIntensity = 0;
        difficultyLevel = 0.5f;

        // Initialize track points for realistic racing line
        trackPoints = generateTrackPoints();

        // Initialize scenery
        sceneryObjects = new ArrayList<>();
        generateScenery();

        // Initialize players
        player1 = new PlayerCar(150, GROUND_Y - 80, 50, 90, Color.RED, 1);
        if (gameMode2Player) {
            player2 = new PlayerCar(300, GROUND_Y - 80, 50, 90, Color.BLUE, 2);
        }

        // Initialize AI cars
        aiCars = new ArrayList<>();
        String[] aiNames = {"AI-Racer", "TurboBot", "SpeedMaster", "DriftKing"};
        Color[] aiColors = {Color.ORANGE, Color.MAGENTA, Color.CYAN, Color.GREEN};
        for (int i = 0; i < (gameMode2Player ? 3 : 4); i++) {
            AICar ai = new AICar(
                    100 + i * 180,
                    GROUND_Y - 80 - i * 70,
                    48, 85,
                    aiColors[i % aiColors.length],
                    aiNames[i % aiNames.length]
            );
            ai.setDifficulty(difficultyLevel + (i * 0.1f));
            aiCars.add(ai);
        }

        // Initialize obstacles
        obstacles = new ArrayList<>();
        generateObstacles();

        // Initialize power-ups
        powerUps = new ArrayList<>();
        generatePowerUps();

        // Initialize particles
        particles = new ArrayList<>();

        // Initialize trails
        trails = new ArrayList<>();

        // Initialize track renderer
        trackRenderer = new TrackRenderer();

        // Initialize weather
        weather = new WeatherEffect();

        // Initialize HUD
        hud = new HUD();

        cameraX = 0;
        cameraY = 0;
    }

    private float[] generateTrackPoints() {
        float[] points = new float[40];
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            float radius = 300 + (float)(Math.sin(angle * 3) * 100);
            points[i * 2] = 400 + (float)(radius * Math.cos(angle));
            points[i * 2 + 1] = 350 + (float)(radius * Math.sin(angle));
        }
        return points;
    }

    private void generateScenery() {
        Random rand = new Random();
        for (int i = 0; i < 30; i++) {
            int x = rand.nextInt(WIDTH);
            int y = rand.nextInt(HEIGHT);
            SceneryType type = SceneryType.values()[rand.nextInt(SceneryType.values().length)];
            sceneryObjects.add(new SceneryObject(x, y, type));
        }
    }

    private void generateObstacles() {
        Random rand = new Random();
        for (int i = 0; i < 12; i++) {
            int x = TRACK_OFFSET_X + 50 + rand.nextInt(TRACK_WIDTH - 100);
            int y = 50 + rand.nextInt(500);
            ObstacleType type = ObstacleType.values()[rand.nextInt(ObstacleType.values().length)];
            obstacles.add(new Obstacle(x, y, type));
        }
    }

    private void generatePowerUps() {
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            int x = TRACK_OFFSET_X + 50 + rand.nextInt(TRACK_WIDTH - 100);
            int y = 100 + rand.nextInt(400);
            PowerUpType type = PowerUpType.values()[rand.nextInt(PowerUpType.values().length)];
            powerUps.add(new PowerUp(x, y, type));
        }
    }

    private void startGameLoop() {
        gameLoop = new Timer(16, this);
        gameLoop.start();
    }

    private void startAnimationTimer() {
        animationTimer = new Timer();
        animationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateAnimation();
            }
        }, 0, 33);
    }

    private void updateAnimation() {
        cloudOffset += 0.3f;
        if (cloudOffset > WIDTH + 200) cloudOffset = -200;

        treeOffset += 0.8f;
        if (treeOffset > WIDTH) treeOffset = -150;

        roadOffset += 1.5f;
        if (roadOffset > 80) roadOffset = 0;

        dayNightCycle += 0.005f;
        if (dayNightCycle > 1.0f) dayNightCycle = 0;
        isNightMode = dayNightCycle > 0.65f && dayNightCycle < 0.9f;

        // Update weather
        weather.update();

        // Update scenery animation
        for (SceneryObject obj : sceneryObjects) {
            obj.update();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameState.PLAYING) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        frameCount++;
        float deltaTime = 1.0f / 60.0f;

        // Update camera to follow player
        updateCamera();

        // Update player 1
        player1.update(keys, deltaTime);
        player1.boundToScreen(TRACK_OFFSET_X, TRACK_OFFSET_X + TRACK_WIDTH, 20, HEIGHT - 20);
        player1.setLap(lap1);

        // Update player 2 if in 2-player mode
        if (gameMode2Player && player2 != null) {
            player2.update(keys, deltaTime);
            player2.boundToScreen(TRACK_OFFSET_X, TRACK_OFFSET_X + TRACK_WIDTH, 20, HEIGHT - 20);
            player2.setLap(lap2);

            // Check collision between players
            if (player1.getBounds().intersects(player2.getBounds())) {
                handlePlayerCollision(player1, player2);
            }
        }

        // Update AI cars
        for (AICar ai : aiCars) {
            ai.updateAI(player1, obstacles, trackPoints, deltaTime);
            ai.move(deltaTime);
            ai.boundToScreen(TRACK_OFFSET_X, TRACK_OFFSET_X + TRACK_WIDTH, 20, HEIGHT - 20);

            // Check AI collision with player
            if (player1.getBounds().intersects(ai.getBounds())) {
                handleAICollision(player1, ai);
            }

            if (gameMode2Player && player2 != null) {
                if (player2.getBounds().intersects(ai.getBounds())) {
                    handleAICollision(player2, ai);
                }
            }

            // AI collision with obstacles
            for (Obstacle obs : obstacles) {
                if (ai.getBounds().intersects(obs.getBounds())) {
                    ai.avoidObstacle(obs);
                }
            }
        }

        // Update obstacles
        for (Obstacle obs : obstacles) {
            obs.update(deltaTime);
            if (player1.getBounds().intersects(obs.getBounds()) && !player1.hasShield()) {
                handleObstacleCollision(player1, obs);
            }
            if (gameMode2Player && player2 != null) {
                if (player2.getBounds().intersects(obs.getBounds()) && !player2.hasShield()) {
                    handleObstacleCollision(player2, obs);
                }
            }
        }

        // Update power-ups
        for (PowerUp pu : powerUps) {
            pu.update(deltaTime);
            if (player1.getBounds().intersects(pu.getBounds())) {
                handlePowerUp(player1, pu);
            }
            if (gameMode2Player && player2 != null) {
                if (player2.getBounds().intersects(pu.getBounds())) {
                    handlePowerUp(player2, pu);
                }
            }
        }

        // Update particles
        Iterator<Particle> iter = particles.iterator();
        while (iter.hasNext()) {
            Particle p = iter.next();
            p.update(deltaTime);
            if (p.isDead()) iter.remove();
        }

        // Update trails
        addTrails();
        Iterator<TrailEffect> trailIter = trails.iterator();
        while (trailIter.hasNext()) {
            TrailEffect t = trailIter.next();
            t.update(deltaTime);
            if (t.isDead()) trailIter.remove();
        }

        // Update track
        trackRenderer.update(player1.getX(), player1.getY());

        // Update scores
        if (frameCount % 10 == 0) {
            score1 += player1.getSpeed() * 0.3f;
            if (gameMode2Player && player2 != null) {
                score2 += player2.getSpeed() * 0.3f;
            }
        }

        // Check lap completion
        checkLapCompletion(player1, 1);
        if (gameMode2Player && player2 != null) {
            checkLapCompletion(player2, 2);
        }

        // Check race completion
        if (lap1 >= MAX_LAPS || (gameMode2Player && lap2 >= MAX_LAPS)) {
            state = GameState.GAME_OVER;
            gameLoop.stop();
            animationTimer.cancel();
        }

        // Update shake intensity
        if (shakeIntensity > 0) {
            shakeIntensity *= 0.95f;
            if (shakeIntensity < 0.01f) shakeIntensity = 0;
        }

        // Update HUD
        hud.update(score1, lap1, player1.getSpeed(), MAX_LAPS,
                gameMode2Player ? score2 : 0,
                gameMode2Player ? lap2 : 0);
    }

    private void updateCamera() {
        float targetX = player1.getX() - WIDTH / 2 + 50;
        float targetY = player1.getY() - HEIGHT / 2 + 50;
        cameraX += (targetX - cameraX) * 0.05f;
        cameraY += (targetY - cameraY) * 0.05f;
    }

    private void addTrails() {
        if (player1.getSpeed() > 2.0f && frameCount % 3 == 0) {
            trails.add(new TrailEffect(
                    player1.getX() + player1.getWidth()/2,
                    player1.getY() + player1.getHeight(),
                    Color.RED
            ));
        }

        if (gameMode2Player && player2 != null && player2.getSpeed() > 2.0f && frameCount % 3 == 0) {
            trails.add(new TrailEffect(
                    player2.getX() + player2.getWidth()/2,
                    player2.getY() + player2.getHeight(),
                    Color.BLUE
            ));
        }

        for (AICar ai : aiCars) {
            if (ai.getSpeed() > 2.0f && frameCount % 4 == 0) {
                trails.add(new TrailEffect(
                        ai.getX() + ai.getWidth()/2,
                        ai.getY() + ai.getHeight(),
                        ai.getColor()
                ));
            }
        }
    }

    private void checkLapCompletion(PlayerCar player, int playerNum) {
        // Simple lap detection: crossing the finish line
        if (player.getY() < 80 && player.getPrevY() > 80) {
            if (playerNum == 1) {
                lap1++;
                if (lap1 >= MAX_LAPS) {
                    // Race finished for player 1
                }
            } else if (playerNum == 2) {
                lap2++;
                if (lap2 >= MAX_LAPS) {
                    // Race finished for player 2
                }
            }

            // Create celebration particles
            for (int i = 0; i < 20; i++) {
                particles.add(new Particle(
                        player.getX() + player.getWidth()/2,
                        player.getY(),
                        new Color(255, 215, 0),
                        5 + (int)(Math.random() * 5)
                ));
            }
        }
    }

    private void handlePlayerCollision(PlayerCar p1, PlayerCar p2) {
        // Push cars apart
        int dx = p1.getX() - p2.getX();
        int dy = p1.getY() - p2.getY();
        float distance = (float)Math.sqrt(dx*dx + dy*dy);
        if (distance < 1) distance = 1;

        float overlap = (p1.getWidth()/2 + p2.getWidth()/2 - distance) / 2;
        p1.setX((int)(p1.getX() + dx/distance * overlap));
        p1.setY((int)(p1.getY() + dy/distance * overlap));
        p2.setX((int)(p2.getX() - dx/distance * overlap));
        p2.setY((int)(p2.getY() - dy/distance * overlap));

        // Create collision particles
        for (int i = 0; i < 15; i++) {
            particles.add(new Particle(
                    (p1.getX() + p2.getX()) / 2,
                    (p1.getY() + p2.getY()) / 2,
                    Color.WHITE,
                    3 + (int)(Math.random() * 4)
            ));
        }

        shakeIntensity = 3.0f;
    }

    private void handleAICollision(PlayerCar player, AICar ai) {
        // Create explosion particles
        for (int i = 0; i < 25; i++) {
            particles.add(new Particle(
                    ai.getX() + ai.getWidth()/2,
                    ai.getY() + ai.getHeight()/2,
                    new Color(255, 150, 50),
                    4 + (int)(Math.random() * 6)
            ));
        }

        score1 = Math.max(0, score1 - 30);
        ai.resetPosition();
        shakeIntensity = 4.0f;

        // Play impact effect
        for (int i = 0; i < 10; i++) {
            particles.add(new Particle(
                    player.getX() + player.getWidth()/2,
                    player.getY() + player.getHeight()/2,
                    Color.YELLOW,
                    2 + (int)(Math.random() * 3)
            ));
        }
    }

    private void handleObstacleCollision(PlayerCar player, Obstacle obs) {
        if (player.hasShield()) {
            // Shield absorbs the hit
            for (int i = 0; i < 10; i++) {
                particles.add(new Particle(
                        obs.getX() + obs.getWidth()/2,
                        obs.getY() + obs.getHeight()/2,
                        new Color(0, 255, 255),
                        2 + (int)(Math.random() * 3)
                ));
            }
            obs.reset();
            return;
        }

        score1 = Math.max(0, score1 - 25);
        obs.reset();
        shakeIntensity = 5.0f;

        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(
                    obs.getX() + obs.getWidth()/2,
                    obs.getY() + obs.getHeight()/2,
                    Color.GRAY,
                    3 + (int)(Math.random() * 5)
            ));
        }
    }

    private void handlePowerUp(PlayerCar player, PowerUp pu) {
        switch (pu.getType()) {
            case SPEED:
                player.boostSpeed(2.0f, 3000);
                if (player == player1) score1 += 150;
                else if (player == player2) score2 += 150;
                break;
            case SHIELD:
                player.activateShield(3000);
                if (player == player1) score1 += 100;
                else if (player == player2) score2 += 100;
                break;
            case COIN:
                if (player == player1) score1 += 250;
                else if (player == player2) score2 += 250;
                break;
            case MAGNET:
                player.activateMagnet(2000);
                if (player == player1) score1 += 75;
                else if (player == player2) score2 += 75;
                break;
            case NITRO:
                player.activateNitro(1500);
                if (player == player1) score1 += 200;
                else if (player == player2) score2 += 200;
                break;
            case REPAIR:
                player.repair();
                if (player == player1) score1 += 50;
                else if (player == player2) score2 += 50;
                break;
        }
        pu.respawn();

        // Power-up pickup effect
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(
                    pu.getX() + pu.getWidth()/2,
                    pu.getY() + pu.getHeight()/2,
                    new Color(0, 255, 100),
                    3 + (int)(Math.random() * 4)
            ));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Apply camera transformation
        if (state == GameState.PLAYING || state == GameState.GAME_OVER) {
            g2d.translate(-cameraX * 0.3f, -cameraY * 0.3f);
        }

        // Apply shake
        if (shakeIntensity > 0) {
            Random rand = new Random();
            int shakeX = (int)((rand.nextFloat() - 0.5f) * shakeIntensity * 2);
            int shakeY = (int)((rand.nextFloat() - 0.5f) * shakeIntensity * 2);
            g2d.translate(shakeX, shakeY);
        }

        if (state == GameState.MENU) {
            drawMenu(g2d);
        } else if (state == GameState.PLAYING) {
            drawGame(g2d);
        } else if (state == GameState.GAME_OVER) {
            drawGame(g2d);
            drawGameOver(g2d);
        } else if (state == GameState.PAUSED) {
            drawGame(g2d);
            drawPauseOverlay(g2d);
        }
    }

    private void drawMenu(Graphics2D g) {
        // Animated background
        GradientPaint gradient = new GradientPaint(0, 0, new Color(10, 20, 40),
                WIDTH, HEIGHT, new Color(20, 10, 40));
        g.setPaint(gradient);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Animated stars
        for (int i = 0; i < 50; i++) {
            int x = (int)((i * 137 + System.currentTimeMillis() / 100) % WIDTH);
            int y = (int)((i * 251 + System.currentTimeMillis() / 150) % HEIGHT);
            g.setColor(new Color(255, 255, 255, 50 + (int)(Math.sin(i + System.currentTimeMillis()/1000) * 50 + 50)));
            g.fillOval(x, y, 2, 2);
        }

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 65));
        String title = "🏎️ GLOBAL RACING TOUR";
        FontMetrics fm = g.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(title)) / 2;
        g.drawString(title, x, 180);

        // Subtitle
        g.setFont(new Font("Arial", Font.PLAIN, 28));
        g.setColor(new Color(200, 200, 255));
        String subtitle = "★ WORLD CHAMPIONSHIP ★";
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(subtitle)) / 2;
        g.drawString(subtitle, x, 225);

        // Menu options
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.setColor(Color.YELLOW);
        String option1 = "1. Press SPACE for 1-Player Mode";
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(option1)) / 2;
        g.drawString(option1, x, 350);

        g.setColor(Color.CYAN);
        String option2 = "2. Press 2 for 2-Player Mode";
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(option2)) / 2;
        g.drawString(option2, x, 410);

        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.setColor(Color.LIGHT_GRAY);
        String controls = "P1: ↑ ↓ ← →  |  P2: W A S D  |  P: Pause  |  R: Reset";
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(controls)) / 2;
        g.drawString(controls, x, 500);

        // Animated racing cars
        drawAnimatedCars(g);

        // Version
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(Color.GRAY);
        g.drawString("v3.0 - 700+ lines", 10, HEIGHT - 10);
    }

    private void drawAnimatedCars(Graphics2D g) {
        int time = (int)(System.currentTimeMillis() / 100);
        for (int i = 0; i < 6; i++) {
            float phase = i * 1.2f;
            int x = 50 + i * 160 + (int)(Math.sin(time * 0.03 + phase) * 50);
            int y = 280 + (int)(Math.sin(time * 0.04 + phase * 0.7) * 40);

            // Car body
            g.setColor(new Color(100 + i * 25, 50 + i * 20, 200 - i * 20));
            g.fillRoundRect(x, y, 45, 80, 10, 10);

            // Windows
            g.setColor(new Color(150, 200, 255));
            g.fillRect(x + 5, y + 10, 35, 15);
            g.fillRect(x + 5, y + 35, 15, 20);
            g.fillRect(x + 25, y + 35, 15, 20);

            // Headlights
            g.setColor(Color.YELLOW);
            g.fillOval(x + 5, y + 5, 8, 8);
            g.fillOval(x + 32, y + 5, 8, 8);
        }
    }

    private void drawGame(Graphics2D g) {
        // Draw sky with day/night cycle
        if (isNightMode) {
            g.setColor(new Color(5, 5, 25));
        } else {
            GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 235),
                    0, HEIGHT/2, new Color(200, 230, 255));
            g.setPaint(sky);
        }
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw scenery background
        drawScenery(g);

        // Draw clouds
        drawClouds(g);

        // Draw track
        trackRenderer.draw(g);

        // Draw road markings
        drawRoadMarkings(g);

        // Draw obstacles
        for (Obstacle obs : obstacles) {
            obs.draw(g);
        }

        // Draw power-ups
        for (PowerUp pu : powerUps) {
            pu.draw(g);
        }

        // Draw trails
        for (TrailEffect t : trails) {
            t.draw(g);
        }

        // Draw AI cars
        for (AICar ai : aiCars) {
            ai.draw(g);
        }

        // Draw players
        player1.draw(g);
        if (gameMode2Player && player2 != null) {
            player2.draw(g);
        }

        // Draw particles
        for (Particle p : particles) {
            p.draw(g);
        }

        // Draw weather
        weather.draw(g);

        // Draw HUD
        hud.draw(g);

        // Draw minimap
        drawMinimap(g);
    }

    private void drawScenery(Graphics2D g) {
        for (SceneryObject obj : sceneryObjects) {
            obj.draw(g);
        }
    }

    private void drawClouds(Graphics2D g) {
        g.setColor(new Color(255, 255, 255, isNightMode ? 50 : 150));
        for (int i = 0; i < 6; i++) {
            int x = (int)(i * 180 + cloudOffset) % (WIDTH + 300) - 150;
            int y = 20 + i * 30;
            g.fillOval(x, y, 100, 50);
            g.fillOval(x + 40, y - 20, 80, 45);
            g.fillOval(x + 70, y + 10, 60, 35);
        }
    }

    private void drawRoadMarkings(Graphics2D g) {
        g.setColor(Color.WHITE);
        for (int i = 0; i < 25; i++) {
            int y = (int)(i * 35 + roadOffset) % (HEIGHT + 50) - 25;
            g.fillRoundRect(TRACK_OFFSET_X + TRACK_WIDTH/2 - 20, y, 40, 20, 10, 10);
        }

        // Side lines
        g.setColor(new Color(255, 255, 0, 80));
        g.fillRect(TRACK_OFFSET_X, 0, 5, HEIGHT);
        g.fillRect(TRACK_OFFSET_X + TRACK_WIDTH - 5, 0, 5, HEIGHT);

        // Track boundaries
        g.setColor(new Color(255, 0, 0, 50));
        for (int i = 0; i < HEIGHT; i += 20) {
            g.fillRect(TRACK_OFFSET_X + 5, i, 3, 10);
            g.fillRect(TRACK_OFFSET_X + TRACK_WIDTH - 8, i, 3, 10);
        }
    }

    private void drawMinimap(Graphics2D g) {
        // Minimap background
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(WIDTH - 180, 10, 165, 130, 10, 10);
        g.setColor(Color.WHITE);
        g.drawRoundRect(WIDTH - 180, 10, 165, 130, 10, 10);

        // Scale factor
        float scale = 0.15f;
        int offsetX = WIDTH - 170;
        int offsetY = 20;

        // Draw track outline
        g.setColor(new Color(100, 100, 100));
        for (int i = 0; i < trackPoints.length - 2; i += 2) {
            int x1 = offsetX + (int)(trackPoints[i] * scale);
            int y1 = offsetY + (int)(trackPoints[i+1] * scale);
            int x2 = offsetX + (int)(trackPoints[i+2] * scale);
            int y2 = offsetY + (int)(trackPoints[i+3] * scale);
            g.drawLine(x1, y1, x2, y2);
        }

        // Draw player positions
        g.setColor(Color.RED);
        g.fillOval(offsetX + (int)(player1.getX() * scale) - 3,
                offsetY + (int)(player1.getY() * scale) - 3, 6, 6);

        if (gameMode2Player && player2 != null) {
            g.setColor(Color.BLUE);
            g.fillOval(offsetX + (int)(player2.getX() * scale) - 3,
                    offsetY + (int)(player2.getY() * scale) - 3, 6, 6);
        }

        // Draw AI positions
        for (AICar ai : aiCars) {
            g.setColor(ai.getColor());
            g.fillRect(offsetX + (int)(ai.getX() * scale) - 2,
                    offsetY + (int)(ai.getY() * scale) - 2, 4, 4);
        }
    }

    private void drawPauseOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 70));
        String msg = "⏸ PAUSED";
        FontMetrics fm = g.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(msg)) / 2;
        g.drawString(msg, x, HEIGHT/2 - 50);

        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.setColor(Color.LIGHT_GRAY);
        String sub = "Press P to Resume";
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(sub)) / 2;
        g.drawString(sub, x, HEIGHT/2 + 30);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 60));
        String msg = "🏆 RACE COMPLETE! 🏆";
        FontMetrics fm = g.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(msg)) / 2;
        g.drawString(msg, x, 200);

        // Player 1 results
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.setColor(Color.RED);
        String p1Result = "Player 1: Score " + score1 + " | Laps " + lap1 + "/" + MAX_LAPS;
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(p1Result)) / 2;
        g.drawString(p1Result, x, 300);

        // Player 2 results (if 2-player)
        if (gameMode2Player) {
            g.setColor(Color.BLUE);
            String p2Result = "Player 2: Score " + score2 + " | Laps " + lap2 + "/" + MAX_LAPS;
            fm = g.getFontMetrics();
            x = (WIDTH - fm.stringWidth(p2Result)) / 2;
            g.drawString(p2Result, x, 360);

            // Winner
            g.setFont(new Font("Arial", Font.BOLD, 40));
            String winner;
            if (lap1 > lap2 || (lap1 == lap2 && score1 > score2)) {
                winner = "🏅 PLAYER 1 WINS! 🏅";
                g.setColor(Color.RED);
            } else if (lap2 > lap1 || (lap2 == lap1 && score2 > score1)) {
                winner = "🏅 PLAYER 2 WINS! 🏅";
                g.setColor(Color.BLUE);
            } else {
                winner = "🤝 TIE GAME! 🤝";
                g.setColor(Color.YELLOW);
            }
            fm = g.getFontMetrics();
            x = (WIDTH - fm.stringWidth(winner)) / 2;
            g.drawString(winner, x, 440);
        }

        // AI results
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.setColor(Color.ORANGE);
        int yPos = gameMode2Player ? 500 : 400;
        g.drawString("AI Opponents:", 50, yPos);
        yPos += 30;
        for (AICar ai : aiCars) {
            g.setColor(ai.getColor());
            g.drawString("• " + ai.getName() + " - Position: " + (aiCars.indexOf(ai) + 1), 70, yPos);
            yPos += 25;
        }

        // Time
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        long minutes = elapsed / 60;
        long seconds = elapsed % 60;
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        String timeMsg = "⏱️ Race Time: " + String.format("%02d:%02d", minutes, seconds);
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(timeMsg)) / 2;
        g.drawString(timeMsg, x, gameMode2Player ? 560 : 480);

        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.GREEN);
        String restartMsg = "Press R to Restart  |  Press M for Menu";
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(restartMsg)) / 2;
        g.drawString(restartMsg, x, gameMode2Player ? 620 : 540);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key >= 0 && key < keys.length) {
            keys[key] = true;
        }

        if (state == GameState.MENU) {
            if (key == KeyEvent.VK_SPACE) {
                gameMode2Player = false;
                state = GameState.PLAYING;
                startTime = System.currentTimeMillis();
            } else if (key == KeyEvent.VK_2) {
                gameMode2Player = true;
                state = GameState.PLAYING;
                startTime = System.currentTimeMillis();
            }
        } else if (state == GameState.PLAYING && key == KeyEvent.VK_P) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED && key == KeyEvent.VK_P) {
            state = GameState.PLAYING;
        } else if (key == KeyEvent.VK_R) {
            resetGame();
        } else if (key == KeyEvent.VK_M && state == GameState.GAME_OVER) {
            state = GameState.MENU;
            resetGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key >= 0 && key < keys.length) {
            keys[key] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void resetGame() {
        if (gameLoop != null) gameLoop.stop();
        if (animationTimer != null) animationTimer.cancel();
        initializeGame();
        startGameLoop();
        startAnimationTimer();
    }

    // Inner classes

    enum GameState {
        MENU, PLAYING, PAUSED, GAME_OVER
    }

    enum ObstacleType {
        CONE, BARREL, TIRE, ROCK, OIL_SPILL
    }

    enum PowerUpType {
        SPEED, SHIELD, COIN, MAGNET, NITRO, REPAIR
    }

    enum SceneryType {
        TREE, BUSH, SIGN, LAMP_POST, BUILDING
    }

    // --- Player Car Class ---
    class PlayerCar {
        private int x, y, width, height;
        private Color color;
        private float speed, maxSpeed;
        private int prevY;
        private boolean shieldActive, magnetActive, nitroActive;
        private long shieldEndTime, magnetEndTime, nitroEndTime;
        private float boostMultiplier;
        private long boostEndTime;
        private int playerNum;
        private float acceleration;
        private float rotation;
        private int health;
        private float driftAngle;
        private boolean isDrifting;

        public PlayerCar(int x, int y, int w, int h, Color c, int num) {
            this.x = x; this.y = y;
            width = w; height = h;
            color = c;
            playerNum = num;
            speed = 0;
            maxSpeed = 4.5f;
            acceleration = 0.15f;
            prevY = y;
            shieldActive = false;
            magnetActive = false;
            nitroActive = false;
            boostMultiplier = 1.0f;
            health = 100;
            rotation = 0;
            driftAngle = 0;
            isDrifting = false;
        }

        public void update(boolean[] keys, float delta) {
            prevY = y;
            float currentMaxSpeed = maxSpeed * boostMultiplier;
            if (nitroActive) currentMaxSpeed *= 1.5f;

            // Player 1 controls (Arrow keys)
            boolean up = keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W];
            boolean down = keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S];
            boolean left = keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A];
            boolean right = keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D];

            // Player 2 controls (WASD) - only if 2-player mode
            if (playerNum == 2 && gameMode2Player) {
                up = keys[KeyEvent.VK_W];
                down = keys[KeyEvent.VK_S];
                left = keys[KeyEvent.VK_A];
                right = keys[KeyEvent.VK_D];
            }

            // Acceleration
            if (up) {
                speed = Math.min(speed + acceleration * delta * 60, currentMaxSpeed);
            } else if (down) {
                speed = Math.max(speed - acceleration * delta * 60, -currentMaxSpeed * 0.4f);
            } else {
                // Friction
                if (speed > 0) speed = Math.max(0, speed - 0.05f);
                else if (speed < 0) speed = Math.min(0, speed + 0.05f);
            }

            // Rotation and turning
            float turnSpeed = 0.04f * (1 + Math.abs(speed) / maxSpeed);
            if (left) {
                rotation -= turnSpeed * delta * 60;
                if (Math.abs(speed) > 1.0f) isDrifting = true;
            } else if (right) {
                rotation += turnSpeed * delta * 60;
                if (Math.abs(speed) > 1.0f) isDrifting = true;
            } else {
                isDrifting = false;
                // Smooth rotation return
                rotation *= 0.95f;
            }

            // Movement with rotation
            float moveX = (float)(speed * Math.sin(rotation));
            float moveY = (float)(-speed * Math.cos(rotation));
            x += moveX * delta * 60;
            y += moveY * delta * 60;

            // Drift effect
            if (isDrifting && Math.abs(speed) > 2.0f) {
                driftAngle += (left ? -0.1f : right ? 0.1f : 0) * delta * 60;
                x += (float)(Math.sin(driftAngle) * speed * 0.3f);
            }

            // Update boost timer
            if (boostMultiplier > 1.0f && System.currentTimeMillis() > boostEndTime) {
                boostMultiplier = 1.0f;
            }

            // Update shield timer
            if (shieldActive && System.currentTimeMillis() > shieldEndTime) {
                shieldActive = false;
            }

            // Update magnet timer
            if (magnetActive && System.currentTimeMillis() > magnetEndTime) {
                magnetActive = false;
            }

            // Update nitro timer
            if (nitroActive && System.currentTimeMillis() > nitroEndTime) {
                nitroActive = false;
            }
        }

        public void boundToScreen(int minX, int maxX, int minY, int maxY) {
            x = Math.max(minX + 10, Math.min(maxX - width - 10, x));
            y = Math.max(minY + 10, Math.min(maxY - height - 10, y));
        }

        public void boostSpeed(float multiplier, long duration) {
            boostMultiplier = multiplier;
            boostEndTime = System.currentTimeMillis() + duration;
        }

        public void activateShield(long duration) {
            shieldActive = true;
            shieldEndTime = System.currentTimeMillis() + duration;
        }

        public void activateMagnet(long duration) {
            magnetActive = true;
            magnetEndTime = System.currentTimeMillis() + duration;
        }

        public void activateNitro(long duration) {
            nitroActive = true;
            nitroEndTime = System.currentTimeMillis() + duration;
        }

        public void repair() {
            health = Math.min(100, health + 30);
        }

        public void draw(Graphics2D g) {
            Graphics2D g2d = (Graphics2D) g.create();

            // Translate and rotate car
            int centerX = x + width / 2;
            int centerY = y + height / 2;
            g2d.rotate(rotation, centerX, centerY);

            // Shadow
            g2d.setColor(new Color(0, 0, 0, 40));
            g2d.fillRoundRect(x + 3, y + 5, width, height, 10, 10);

            // Shield glow
            if (shieldActive) {
                g2d.setColor(new Color(0, 255, 255, 60));
                g2d.fillOval(x - 15, y - 15, width + 30, height + 30);
                // Shield ring
                g2d.setColor(new Color(0, 255, 255, 100));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(x - 10, y - 10, width + 20, height + 20);
            }

            // Magnet glow
            if (magnetActive) {
                g2d.setColor(new Color(255, 0, 255, 40));
                g2d.fillOval(x - 25, y - 25, width + 50, height + 50);
            }

            // Nitro flame
            if (nitroActive) {
                g2d.setColor(new Color(255, 100, 0, 80));
                for (int i = 0; i < 3; i++) {
                    int fx = x + 10 + i * 15;
                    int fy = y + height + 5 + i * 5;
                    g2d.fillOval(fx, fy, 10, 15 + (int)(Math.random() * 10));
                }
            }

            // Car body with gradient
            GradientPaint carGrad = new GradientPaint(x, y, color,
                    x, y + height, color.darker());
            g2d.setPaint(carGrad);
            g2d.fillRoundRect(x, y, width, height, 12, 12);

            // Car body highlight
            g2d.setColor(new Color(255, 255, 255, 30));
            g2d.fillRoundRect(x + 5, y + 3, width - 10, height / 3, 8, 8);

            // Windshield
            g2d.setColor(new Color(135, 206, 250, 180));
            g2d.fillRoundRect(x + 5, y + 8, width - 10, 18, 5, 5);

            // Windows (side)
            g2d.setColor(new Color(100, 180, 255, 150));
            g2d.fillRect(x + 5, y + 30, 12, 20);
            g2d.fillRect(x + width - 17, y + 30, 12, 20);

            // Headlights
            g2d.setColor(Color.YELLOW);
            g2d.fillOval(x + 3, y + 3, 10, 8);
            g2d.fillOval(x + width - 13, y + 3, 10, 8);
            // Headlight glow
            g2d.setColor(new Color(255, 255, 0, 50));
            g2d.fillOval(x - 10, y, 15, 12);
            g2d.fillOval(x + width - 5, y, 15, 12);

            // Taillights
            g2d.setColor(Color.RED);
            g2d.fillOval(x + 3, y + height - 10, 10, 8);
            g2d.fillOval(x + width - 13, y + height - 10, 10, 8);

            // Spoiler
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(x + 8, y - 5, width - 16, 5);
            g2d.fillRect(x + 12, y - 10, width - 24, 5);

            // Racing stripe
            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.fillRect(x + width/2 - 3, y, 6, height);

            // Player number
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            String num = String.valueOf(playerNum);
            FontMetrics fm = g2d.getFontMetrics();
            int tx = x + (width - fm.stringWidth(num)) / 2;
            g2d.drawString(num, tx, y + height - 10);

            // Health bar
            if (health < 100) {
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.fillRect(x, y + height + 5, width, 5);
                g2d.setColor(health > 70 ? Color.GREEN : health > 40 ? Color.YELLOW : Color.RED);
                g2d.fillRect(x + 1, y + height + 6, (int)((width - 2) * health / 100.0f), 3);
            }

            g2d.dispose();
        }

        public Rectangle getBounds() {
            return new Rectangle(x + 5, y + 5, width - 10, height - 10);
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getPrevY() { return prevY; }
        public float getSpeed() { return speed; }
        public boolean hasShield() { return shieldActive; }
        public boolean hasMagnet() { return magnetActive; }
        public void setX(int x) { this.x = x; }
        public void setY(int y) { this.y = y; }
        public void setLap(int lap) {}
    }

    // --- AI Car Class ---
    class AICar {
        private int x, y, width, height;
        private Color color;
        private String name;
        private float speed;
        private float targetSpeed;
        private float maxSpeed;
        private float difficulty;
        private int targetX, targetY;
        private int aiTimer;
        private Random rand;
        private float rotation;
        private float driftAngle;
        private int health;
        private boolean isAvoiding;
        private int avoidTimer;

        public AICar(int x, int y, int w, int h, Color c, String name) {
            this.x = x; this.y = y;
            width = w; height = h;
            color = c;
            this.name = name;
            speed = 1.5f;
            maxSpeed = 2.0f + (float)Math.random() * 1.5f;
            difficulty = 0.5f;
            rand = new Random();
            aiTimer = 0;
            health = 100;
            rotation = 0;
            isAvoiding = false;
            avoidTimer = 0;
            targetX = x;
            targetY = y;
        }

        public void setDifficulty(float diff) {
            difficulty = Math.min(1.0f, diff);
            maxSpeed = 1.8f + difficulty * 2.0f;
        }

        public void updateAI(PlayerCar player, List<Obstacle> obstacles, float[] trackPoints, float delta) {
            aiTimer++;

            // Simple AI: follow player or track
            if (aiTimer > 20) {
                aiTimer = 0;

                // Determine if AI should chase or follow track
                float chaseChance = 0.3f + difficulty * 0.4f;
                if (rand.nextFloat() < chaseChance && player != null) {
                    // Chase player with some offset
                    int offsetX = rand.nextInt(100) - 50;
                    int offsetY = rand.nextInt(100) - 50;
                    targetX = player.getX() + offsetX;
                    targetY = player.getY() + offsetY;
                } else {
                    // Follow track
                    int trackIndex = rand.nextInt(trackPoints.length / 2);
                    targetX = (int)trackPoints[trackIndex * 2] + rand.nextInt(100) - 50;
                    targetY = (int)trackPoints[trackIndex * 2 + 1] + rand.nextInt(100) - 50;
                }

                // Bound targets
                targetX = Math.max(TRACK_OFFSET_X + 20, Math.min(TRACK_OFFSET_X + TRACK_WIDTH - 20, targetX));
                targetY = Math.max(20, Math.min(HEIGHT - 20, targetY));
            }

            // Avoid obstacles
            isAvoiding = false;
            for (Obstacle obs : obstacles) {
                if (Math.abs(x - obs.getX()) < 150 && Math.abs(y - obs.getY()) < 100) {
                    isAvoiding = true;
                    avoidTimer = 30;
                    if (x < obs.getX()) {
                        targetX = x - 80;
                    } else {
                        targetX = x + 80;
                    }
                    targetY = y + 50;
                    break;
                }
            }

            if (avoidTimer > 0) avoidTimer--;

            // Move towards target
            float dx = targetX - x;
            float dy = targetY - y;
            float distance = (float)Math.sqrt(dx * dx + dy * dy);

            if (distance > 5) {
                // Rotation towards target
                float targetRotation = (float)Math.atan2(dx, -dy);
                float diffRotation = targetRotation - rotation;
                while (diffRotation > Math.PI) diffRotation -= 2 * Math.PI;
                while (diffRotation < -Math.PI) diffRotation += 2 * Math.PI;
                rotation += diffRotation * 0.05f;

                // Speed control
                float targetSpeedFactor = Math.min(1.0f, distance / 100.0f);
                targetSpeed = maxSpeed * (0.5f + 0.5f * targetSpeedFactor);
                targetSpeed *= (0.8f + 0.2f * difficulty);

                // Adjust speed based on difficulty
                if (isAvoiding) targetSpeed *= 0.7f;

                // Acceleration
                if (speed < targetSpeed) {
                    speed += 0.05f * (0.5f + 0.5f * difficulty);
                } else {
                    speed -= 0.03f;
                }
                speed = Math.max(0.5f, Math.min(maxSpeed, speed));
            } else {
                speed *= 0.95f;
                if (speed < 0.5f) speed = 0.5f;
            }

            // Move car
            float moveX = (float)(speed * Math.sin(rotation));
            float moveY = (float)(-speed * Math.cos(rotation));
            x += moveX * delta * 60;
            y += moveY * delta * 60;
        }

        public void move(float delta) {
            // Additional movement logic if needed
        }

        public void boundToScreen(int minX, int maxX, int minY, int maxY) {
            x = Math.max(minX + 10, Math.min(maxX - width - 10, x));
            y = Math.max(minY + 10, Math.min(maxY - height - 10, y));
        }

        public void resetPosition() {
            y = -height - rand.nextInt(200);
            x = TRACK_OFFSET_X + 50 + rand.nextInt(TRACK_WIDTH - 100);
            speed = 1.0f;
            health = 100;
        }

        public void avoidObstacle(Obstacle obs) {
            isAvoiding = true;
            avoidTimer = 20;
            if (x < obs.getX()) {
                x -= 20;
            } else {
                x += 20;
            }
        }

        public void draw(Graphics2D g) {
            Graphics2D g2d = (Graphics2D) g.create();

            int centerX = x + width / 2;
            int centerY = y + height / 2;
            g2d.rotate(rotation, centerX, centerY);

            // Shadow
            g2d.setColor(new Color(0, 0, 0, 30));
            g2d.fillRoundRect(x + 2, y + 4, width, height, 8, 8);

            // Car body
            g2d.setColor(color);
            g2d.fillRoundRect(x, y, width, height, 10, 10);

            // Car details
            g2d.setColor(new Color(200, 200, 200, 80));
            g2d.fillRoundRect(x + 5, y + 10, width - 10, 15, 5, 5);

            // Windows
            g2d.setColor(new Color(150, 200, 255, 130));
            g2d.fillRect(x + 5, y + 28, 10, 15);
            g2d.fillRect(x + width - 15, y + 28, 10, 15);

            // AI label
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            String label = "AI";
            FontMetrics fm = g2d.getFontMetrics();
            int tx = x + (width - fm.stringWidth(label)) / 2;
            g2d.drawString(label, tx, y + 60);

            g2d.dispose();

            // Name above car
            g2d = (Graphics2D) g.create();
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            FontMetrics fm = g2d.getFontMetrics();
            int tx = x + (width - fm.stringWidth(name)) / 2;
            g2d.drawString(name, tx, y - 5);
            g2d.dispose();
        }

        public Rectangle getBounds() {
            return new Rectangle(x + 5, y + 5, width - 10, height - 10);
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public float getSpeed() { return speed; }
        public Color getColor() { return color; }
        public String getName() { return name; }
    }

    // --- Obstacle Class ---
    class Obstacle {
        private int x, y, width, height;
        private ObstacleType type;
        private Color color;
        private boolean active;
        private float rotation;
        private float pulse;

        public Obstacle(int x, int y, ObstacleType type) {
            this.x = x; this.y = y;
            this.type = type;
            this.active = true;
            rotation = 0;
            pulse = 0;

            switch(type) {
                case CONE:
                    width = 20; height = 30;
                    color = new Color(255, 165, 0);
                    break;
                case BARREL:
                    width = 30; height = 36;
                    color = new Color(139, 69, 19);
                    break;
                case TIRE:
                    width = 32; height = 32;
                    color = Color.DARK_GRAY;
                    break;
                case ROCK:
                    width = 28; height = 24;
                    color = Color.GRAY;
                    break;
                case OIL_SPILL:
                    width = 40; height = 40;
                    color = new Color(30, 30, 30, 100);
                    break;
            }
        }

        public void update(float delta) {
            y += 1.5f;
            rotation += 0.02f;
            pulse += 0.05f;
            if (y > HEIGHT) {
                reset();
            }
        }

        public void reset() {
            y = -height - 50;
            x = TRACK_OFFSET_X + 50 + (int)(Math.random() * (TRACK_WIDTH - 100));
            active = true;
        }

        public void draw(Graphics2D g) {
            if (!active) return;

            Graphics2D g2d = (Graphics2D) g.create();

            if (type == ObstacleType.OIL_SPILL) {
                // Animated oil spill
                float alpha = 0.3f + (float)Math.sin(pulse) * 0.1f;
                g2d.setColor(new Color(30, 30, 30, (int)(alpha * 255)));
                g2d.fillOval(x, y, width, height);
                g2d.setColor(new Color(50, 50, 50, (int)(alpha * 150)));
                g2d.fillOval(x + 10, y + 10, width - 20, height - 20);
                g2d.dispose();
                return;
            }

            // Shadow
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillOval(x + 5, y + height - 5, width - 10, 8);

            // Rotate
            int cx = x + width/2;
            int cy = y + height/2;
            g2d.rotate(rotation, cx, cy);

            g2d.setColor(color);
            g2d.fillRect(x, y, width, height);

            // Details based on type
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            if (type == ObstacleType.CONE) {
                g2d.drawLine(x + width/2, y, x + width/2, y + height);
                g2d.setColor(Color.WHITE);
                g2d.fillRect(x + 5, y + 10, width - 10, 4);
                g2d.fillRect(x + 5, y + 20, width - 10, 4);
            } else if (type == ObstacleType.BARREL) {
                g2d.drawLine(x, y + height/2, x + width, y + height/2);
                g2d.drawLine(x, y + height/3, x + width, y + height/3);
                g2d.drawLine(x, y + 2*height/3, x + width, y + 2*height/3);
                // Barrel rings
                g2d.setColor(Color.GRAY);
                g2d.drawOval(x - 2, y + 5, width + 4, 6);
                g2d.drawOval(x - 2, y + height - 11, width + 4, 6);
            } else if (type == ObstacleType.TIRE) {
                g2d.setColor(Color.DARK_GRAY);
                g2d.drawOval(x + 4, y + 4, width - 8, height - 8);
                g2d.setColor(Color.BLACK);
                g2d.fillOval(x + 10, y + 10, width - 20, height - 20);
                // Tread pattern
                for (int i = 0; i < 8; i++) {
                    double angle = i * Math.PI / 4;
                    int lx = cx + (int)((width/2 - 2) * Math.cos(angle));
                    int ly = cy + (int)((height/2 - 2) * Math.sin(angle));
                    g2d.drawLine(cx, cy, lx, ly);
                }
            } else if (type == ObstacleType.ROCK) {
                g2d.setColor(Color.DARK_GRAY);
                g2d.drawLine(x + 5, y + 5, x + width - 5, y + height - 5);
                g2d.drawLine(x + width - 5, y + 5, x + 5, y + height - 5);
                // Texture
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.fillOval(x + 8, y + 8, 5, 5);
                g2d.fillOval(x + 15, y + 12, 4, 4);
            }

            g2d.dispose();
        }

        public Rectangle getBounds() {
            return new Rectangle(x + 3, y + 3, width - 6, height - 6);
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }

    // --- PowerUp Class ---
    class PowerUp {
        private int x, y, width, height;
        private PowerUpType type;
        private Color color;
        private boolean active;
        private float pulse;
        private int pulseDir;
        private float rotation;
        private String icon;

        public PowerUp(int x, int y, PowerUpType type) {
            this.x = x; this.y = y;
            this.type = type;
            this.active = true;
            width = 30; height = 30;
            pulse = 0;
            pulseDir = 1;
            rotation = 0;

            switch(type) {
                case SPEED:
                    color = new Color(0, 255, 255);
                    icon = "⚡";
                    break;
                case SHIELD:
                    color = new Color(0, 200, 255);
                    icon = "🛡️";
                    break;
                case COIN:
                    color = new Color(255, 215, 0);
                    icon = "💰";
                    break;
                case MAGNET:
                    color = new Color(255, 0, 255);
                    icon = "🧲";
                    break;
                case NITRO:
                    color = new Color(255, 50, 0);
                    icon = "🔥";
                    break;
                case REPAIR:
                    color = new Color(0, 255, 100);
                    icon = "❤️";
                    break;
            }
        }

        public void update(float delta) {
            y += 1.0f;
            rotation += 0.03f;
            if (y > HEIGHT) {
                respawn();
            }

            pulse += 0.05f * pulseDir;
            if (pulse > 1.0f || pulse < 0) pulseDir *= -1;
        }

        public void respawn() {
            y = -height - 50;
            x = TRACK_OFFSET_X + 50 + (int)(Math.random() * (TRACK_WIDTH - 100));
            active = true;
        }

        public void draw(Graphics2D g) {
            if (!active) return;

            Graphics2D g2d = (Graphics2D) g.create();

            int size = (int)(width + pulse * 8);
            int cx = x + width/2;
            int cy = y + height/2;

            // Glow effect
            RadialGradientPaint glow = new RadialGradientPaint(
                    cx, cy, size,
                    new float[]{0.0f, 0.5f, 1.0f},
                    new Color[]{new Color(color.getRed(), color.getGreen(), color.getBlue(), 80),
                            new Color(color.getRed(), color.getGreen(), color.getBlue(), 30),
                            new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)}
            );
            g2d.setPaint(glow);
            g2d.fillOval(cx - size, cy - size, size * 2, size * 2);

            // Rotating platform
            g2d.rotate(rotation, cx, cy);

            // Main shape
            g2d.setColor(color);
            g2d.fillRoundRect(cx - width/2, cy - height/2, width, height, 8, 8);

            // Border
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(cx - width/2, cy - height/2, width, height, 8, 8);

            // Icon
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            FontMetrics fm = g2d.getFontMetrics();
            int tx = cx - fm.stringWidth(icon) / 2;
            int ty = cy + fm.getAscent() / 2 - 2;
            g2d.drawString(icon, tx, ty);

            g2d.dispose();
        }

        public Rectangle getBounds() {
            return new Rectangle(x + 5, y + 5, width - 10, height - 10);
        }

        public PowerUpType getType() { return type; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }

    // --- Particle Class ---
    class Particle {
        private float x, y;
        private float vx, vy;
        private int life;
        private int maxLife;
        private Color color;
        private int size;
        private float gravity;
        private float friction;

        public Particle(float x, float y, Color color, int size) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.size = size;
            maxLife = 20 + (int)(Math.random() * 40);
            life = maxLife;
            gravity = 0.1f;
            friction = 0.98f;

            float angle = (float)(Math.random() * 2 * Math.PI);
            float speed = 1 + (float)(Math.random() * 5);
            vx = (float)(Math.cos(angle) * speed);
            vy = (float)(Math.sin(angle) * speed) - 2;
        }

        public void update(float delta) {
            x += vx * delta * 60;
            y += vy * delta * 60;
            vy += gravity * delta * 60;
            vx *= friction;
            vy *= friction;
            life--;
            size = Math.max(1, size - 1);
        }

        public boolean isDead() {
            return life <= 0 || size <= 0;
        }

        public void draw(Graphics2D g) {
            float alpha = (float)life / maxLife;
            int r = color.getRed();
            int gc = color.getGreen();
            int b = color.getBlue();
            g.setColor(new Color(r, gc, b, (int)(alpha * 255)));
            int s = (int)(size * (0.5f + 0.5f * alpha));
            g.fillOval((int)x - s/2, (int)y - s/2, s, s);

            // Glow
            if (size > 3) {
                g.setColor(new Color(r, gc, b, (int)(alpha * 50)));
                g.fillOval((int)x - s, (int)y - s, s * 2, s * 2);
            }
        }
    }

    // --- Trail Effect Class ---
    class TrailEffect {
        private float x, y;
        private Color color;
        private int life;
        private int maxLife;
        private float width;

        public TrailEffect(float x, float y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            maxLife = 15 + (int)(Math.random() * 10);
            life = maxLife;
            width = 3 + (float)Math.random() * 3;
        }

        public void update(float delta) {
            life--;
            width *= 0.95f;
        }

        public boolean isDead() {
            return life <= 0 || width < 0.5f;
        }

        public void draw(Graphics2D g) {
            float alpha = (float)life / maxLife;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 150)));
            g.setStroke(new BasicStroke(width));
            g.drawLine((int)x, (int)y - 5, (int)x, (int)y + 5);
        }
    }

    // --- Scenery Object Class ---
    class SceneryObject {
        private int x, y;
        private SceneryType type;
        private float scale;
        private float sway;

        public SceneryObject(int x, int y, SceneryType type) {
            this.x = x;
            this.y = y;
            this.type = type;
            scale = 0.5f + (float)Math.random() * 1.0f;
            sway = (float)Math.random() * 100;
        }

        public void update() {
            sway += 0.02f;
        }

        public void draw(Graphics2D g) {
            int size = (int)(20 * scale);
            int swayX = (int)(Math.sin(sway) * 2);

            switch(type) {
                case TREE:
                    g.setColor(new Color(101, 67, 33));
                    g.fillRect(x + size/2 - 3 + swayX, y + size/2, 6, size/2);
                    g.setColor(new Color(34, 139, 34));
                    g.fillOval(x + swayX - size/2, y, size, size);
                    g.fillOval(x + swayX, y - size/3, size * 0.8f, size * 0.8f);
                    break;
                case BUSH:
                    g.setColor(new Color(50, 150, 50));
                    g.fillOval(x + swayX - size/2, y - size/4, size, size);
                    g.fillOval(x + swayX - size/4, y, size * 0.7f, size * 0.7f);
                    break;
                case SIGN:
                    g.setColor(new Color(200, 200, 200));
                    g.fillRect(x + swayX - 2, y, 4, size * 0.6f);
                    g.setColor(new Color(255, 0, 0));
                    g.fillRect(x + swayX - size/3, y - size/4, size * 0.7f, size * 0.3f);
                    break;
                case LAMP_POST:
                    g.setColor(new Color(80, 80, 80));
                    g.fillRect(x + swayX - 2, y, 4, size);
                    g.setColor(new Color(255, 200, 50, 80));
                    g.fillOval(x + swayX - size/3, y - size/4, size * 0.7f, size * 0.2f);
                    break;
                case BUILDING:
                    g.setColor(new Color(180, 180, 200));
                    g.fillRect(x + swayX, y, size, size);
                    g.setColor(new Color(255, 255, 0, 50));
                    for (int i = 0; i < 3; i++) {
                        for (int j = 0; j < 3; j++) {
                            g.fillRect(x + swayX + 5 + i * 10, y + 5 + j * 10, 5, 5);
                        }
                    }
                    break;
            }
        }
    }

    // --- Track Renderer Class ---
    class TrackRenderer {
        private int offset;
        private Color grass1, grass2;
        private Color roadColor;
        private Color curbColor;

        public TrackRenderer() {
            offset = 0;
            grass1 = new Color(34, 139, 34);
            grass2 = new Color(50, 180, 50);
            roadColor = new Color(70, 70, 70);
            curbColor = new Color(200, 50, 50);
        }

        public void update(int playerX, int playerY) {
            offset = (offset + 2) % 100;
        }

        public void draw(Graphics2D g) {
            // Grass background
            for (int y = 0; y < HEIGHT; y += 50) {
                int yOff = (y + offset) % 100;
                g.setColor(yOff < 50 ? grass1 : grass2);
                g.fillRect(0, y, TRACK_OFFSET_X, 50);
                g.fillRect(TRACK_OFFSET_X + TRACK_WIDTH, y,
                        WIDTH - TRACK_OFFSET_X - TRACK_WIDTH, 50);
            }

            // Road
            g.setColor(roadColor);
            g.fillRect(TRACK_OFFSET_X, 0, TRACK_WIDTH, HEIGHT);

            // Road edge
            g.setColor(Color.WHITE);
            g.fillRect(TRACK_OFFSET_X, 0, 3, HEIGHT);
            g.fillRect(TRACK_OFFSET_X + TRACK_WIDTH - 3, 0, 3, HEIGHT);

            // Curb (red and white)
            for (int i = 0; i < HEIGHT; i += 20) {
                g.setColor((i / 20) % 2 == 0 ? curbColor : Color.WHITE);
                g.fillRect(TRACK_OFFSET_X + 5, i, 8, 10);
                g.fillRect(TRACK_OFFSET_X + TRACK_WIDTH - 13, i, 8, 10);
            }

            // Road texture (subtle)
            g.setColor(new Color(100, 100, 100, 30));
            for (int i = 0; i < 20; i++) {
                int x = TRACK_OFFSET_X + 20 + i * 40;
                g.fillRect(x, offset % 100, 2, 2);
            }
        }
    }

    // --- Weather Effect Class ---
    class WeatherEffect {
        private List<RainDrop> raindrops;
        private boolean isRaining;
        private float intensity;

        public WeatherEffect() {
            raindrops = new ArrayList<>();
            isRaining = false;
            intensity = 0;
        }

        public void update() {
            // Random weather changes
            if (Math.random() < 0.001) {
                isRaining = !isRaining;
            }

            if (isRaining) {
                intensity = Math.min(1.0f, intensity + 0.01f);
                if (raindrops.size() < 100) {
                    for (int i = 0; i < 3; i++) {
                        raindrops.add(new RainDrop());
                    }
                }
            } else {
                intensity = Math.max(0, intensity - 0.005f);
                if (raindrops.size() > 0 && Math.random() < 0.1) {
                    raindrops.remove(0);
                }
            }

            for (RainDrop drop : raindrops) {
                drop.update();
            }
            raindrops.removeIf(RainDrop::isDead);
        }

        public void draw(Graphics2D g) {
            if (raindrops.isEmpty()) return;

            g.setColor(new Color(150, 200, 255, (int)(intensity * 80)));
            for (RainDrop drop : raindrops) {
                drop.draw(g);
            }
        }

        class RainDrop {
            float x, y;
            float vx, vy;
            int life;

            public RainDrop() {
                x = (float)(Math.random() * WIDTH);
                y = (float)(Math.random() * HEIGHT);
                vx = -1 - (float)Math.random() * 2;
                vy = 5 + (float)Math.random() * 10;
                life = 30 + (int)(Math.random() * 30);
            }

            public void update() {
                x += vx;
                y += vy;
                life--;
            }

            public boolean isDead() {
                return life <= 0 || x < 0 || x > WIDTH || y > HEIGHT;
            }

            public void draw(Graphics2D g) {
                g.drawLine((int)x, (int)y, (int)(x + vx * 2), (int)(y + vy * 2));
            }
        }
    }

    // --- HUD Class ---
    class HUD {
        private int score1, score2;
        private int lap1, lap2;
        private float speed1;
        private int maxLaps;
        private long lastUpdate;
        private boolean show2Player;

        public HUD() {
            score1 = 0;
            score2 = 0;
            lap1 = 0;
            lap2 = 0;
            speed1 = 0;
            maxLaps = MAX_LAPS;
            lastUpdate = System.currentTimeMillis();
            show2Player = false;
        }

        public void update(int score1, int lap1, float speed1, int maxLaps,
                           int score2, int lap2) {
            this.score1 = score1;
            this.lap1 = lap1;
            this.speed1 = speed1;
            this.maxLaps = maxLaps;
            this.score2 = score2;
            this.lap2 = lap2;
            this.show2Player = gameMode2Player;
        }

        public void draw(Graphics2D g) {
            // Main HUD background
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRoundRect(10, 10, show2Player ? 350 : 250, 130, 15, 15);

            // Border
            g.setColor(new Color(255, 215, 0, 80));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(10, 10, show2Player ? 350 : 250, 130, 15, 15);

            g.setFont(new Font("Arial", Font.BOLD, 16));

            // Player 1 info
            g.setColor(Color.RED);
            g.drawString("P1 ⭐ " + score1, 25, 40);
            g.setColor(Color.WHITE);
            g.drawString("🏁 " + lap1 + "/" + maxLaps, 25, 65);
            g.drawString("🚀 " + (int)(speed1 * 25) + " km/h", 25, 90);

            // Player 2 info (if 2-player)
            if (show2Player) {
                g.setColor(Color.BLUE);
                g.drawString("P2 ⭐ " + score2, 180, 40);
                g.setColor(Color.WHITE);
                g.drawString("🏁 " + lap2 + "/" + maxLaps, 180, 65);
            }

            // Time
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            long minutes = elapsed / 60;
            long seconds = elapsed % 60;
            g.setColor(new Color(200, 200, 200));
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            String timeStr = String.format("⏱️ %02d:%02d", minutes, seconds);
            if (show2Player) {
                g.drawString(timeStr, 180, 90);
            } else {
                g.drawString(timeStr, 140, 90);
            }

            // Power-up indicators
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            int yOffset = show2Player ? 115 : 100;

            if (player1.hasShield()) {
                g.setColor(new Color(0, 200, 255));
                g.drawString("🛡️ SHIELD", 25, yOffset);
            }
            if (player1.hasMagnet()) {
                g.setColor(new Color(255, 0, 255));
                g.drawString("🧲 MAGNET", 100, yOffset);
            }
            if (player1.getSpeed() > 5.0f) {
                g.setColor(new Color(255, 50, 0));
                g.drawString("⚡ BOOST!", 180, yOffset);
            }

            // Show active power-ups for player 2
            if (show2Player && player2 != null) {
                if (player2.hasShield()) {
                    g.setColor(new Color(0, 200, 255));
                    g.drawString("🛡️", 310, 40);
                }
                if (player2.hasMagnet()) {
                    g.setColor(new Color(255, 0, 255));
                    g.drawString("🧲", 330, 40);
                }
            }
        }
    }

    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("🏎️ Global Racing Tour - 2 Player & AI");
            GlobalRacingTourGame game = new GlobalRacingTourGame(true);
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}