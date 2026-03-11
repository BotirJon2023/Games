import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import javax.sound.sampled.*;

public class VolleyballGameSimulation extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    // Game state management
    private enum GameState { SPLASH, MENU, PLAYING, PAUSED, GAME_OVER, REPLAY, SETTINGS }
    private GameState currentState = GameState.SPLASH;

    // Animation timers
    private Timer gameTimer;
    private Timer animationTimer;
    private Timer physicsTimer;
    private int frameCount = 0;

    // Physics constants
    private final double GRAVITY = 0.35;
    private final double AIR_RESISTANCE = 0.985;
    private final double SPIN_FACTOR = 0.02;
    private final double MAX_SPEED = 25;

    // Court dimensions with 3D effect
    private final int COURT_WIDTH = 1000;
    private final int COURT_HEIGHT = 600;
    private final int COURT_OFFSET_X = 50;
    private final int COURT_OFFSET_Y = 80;
    private final int COURT_FLOOR_Y = 500;

    // Ball properties
    private Ball ball;
    private ArrayList<BallTrailPoint> ballTrail;
    private int maxTrailLength = 20;

    // Players with advanced AI
    private ArrayList<Player> team1Players;
    private ArrayList<Player> team2Players;
    private HashMap<String, PlayerStats> playerStats;

    // Spectators and atmosphere
    private ArrayList<Spectator> spectators;
    private ArrayList<Confetti> confetti;
    private ArrayList<Shadow> shadows;
    private ArrayList<WeatherEffect> weatherEffects;

    // Score and game info
    private int team1Score = 0;
    private int team2Score = 0;
    private int team1Sets = 0;
    private int team2Sets = 0;
    private final int WINNING_SCORE = 25;
    private final int SETS_TO_WIN = 3;
    private int currentSet = 1;
    private double gameTime = 0;
    private String lastHitBy = "";
    private int rallyCount = 0;
    private int maxRallyRecord = 0;
    private double ballPossessionTime = 0;

    // Dynamic environment
    private double windSpeed = 0;
    private double windDirection = 0;
    private double crowdNoise = 0;
    private int timeOfDay = 12; // 0-24 hours
    private boolean isRaining = false;
    private boolean isNight = false;
    private boolean isIndoor = true;

    // Visual effects
    private ArrayList<ParticleSystem> particleSystems;
    private ArrayList<Shockwave> shockwaves;
    private ArrayList<TextPopup> textPopups;
    private TrailEffect powerTrail;
    private CameraShake cameraShake;
    private MotionBlur motionBlur;
    private boolean showHeatmap = false;
    private boolean showStats = false;
    private boolean slowMotion = false;

    // UI Components with animation
    private JButton menuButton, pauseButton, resetButton, statsButton, replayButton;
    private JSlider speedSlider, windSlider;
    private JComboBox<String> weatherSelector;
    private JProgressBar team1Progress, team2Progress;
    private JLabel timeLabel, rallyLabel, speedLabel;

    // Input handling
    private Point mousePos = new Point(0, 0);
    private boolean mousePressed = false;
    private boolean spacePressed = false;
    private boolean shiftPressed = false;

    // Camera and view
    private double cameraX = 0, cameraY = 0;
    private double zoom = 1.0;
    private boolean followingBall = false;

    // Replay system
    private ArrayList<GameSnapshot> replayFrames;
    private boolean recordingReplay = true;
    private int replayFrameIndex = 0;

    // Statistics
    private Statistics gameStats;

    // Random generator
    private Random random = new Random();

    public VolleyballGameSimulation() {
        setPreferredSize(new Dimension(COURT_WIDTH + 200, COURT_HEIGHT + 150));
        setBackground(new Color(10, 20, 30));
        setLayout(null);
        setFocusable(true);

        // Initialize all game components
        initGameComponents();

        // Setup timers with different priorities
        setupTimers();

        // Create UI
        setupUI();

        // Add listeners
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        // Initialize collections
        initializeCollections();

        // Create initial game objects
        createGameObjects();

        // Start splash screen animation
        startSplashAnimation();
    }

    private void initGameComponents() {
        playerStats = new HashMap<>();
        gameStats = new Statistics();
        replayFrames = new ArrayList<>();
    }

    private void setupTimers() {
        gameTimer = new Timer(16, e -> updateGame()); // 60 FPS
        animationTimer = new Timer(33, e -> updateAnimation()); // 30 FPS for animations
        physicsTimer = new Timer(10, e -> updatePhysics()); // 100 FPS for physics

        gameTimer.start();
        animationTimer.start();
        physicsTimer.start();
    }

    private void setupUI() {
        // Create styled buttons
        menuButton = createStyledButton("Main Menu", 20, COURT_HEIGHT + 20, new Color(70, 130, 180));
        pauseButton = createStyledButton("Pause", 120, COURT_HEIGHT + 20, new Color(255, 140, 0));
        resetButton = createStyledButton("Reset", 220, COURT_HEIGHT + 20, new Color(220, 20, 60));
        statsButton = createStyledButton("Stats", 320, COURT_HEIGHT + 20, new Color(50, 205, 50));
        replayButton = createStyledButton("Replay", 420, COURT_HEIGHT + 20, new Color(147, 112, 219));

        // Create sliders
        speedSlider = new JSlider(0, 200, 100);
        speedSlider.setBounds(600, COURT_HEIGHT + 25, 150, 30);
        speedSlider.setBackground(new Color(30, 30, 30));
        speedSlider.setForeground(Color.WHITE);
        add(speedSlider);

        windSlider = new JSlider(0, 200, 50);
        windSlider.setBounds(770, COURT_HEIGHT + 25, 150, 30);
        windSlider.setBackground(new Color(30, 30, 30));
        windSlider.setForeground(Color.WHITE);
        add(windSlider);

        // Create labels
        speedLabel = new JLabel("Speed: 100%");
        speedLabel.setBounds(600, COURT_HEIGHT + 5, 100, 20);
        speedLabel.setForeground(Color.WHITE);
        add(speedLabel);

        timeLabel = new JLabel("Time: 12:00");
        timeLabel.setBounds(900, 10, 100, 20);
        timeLabel.setForeground(Color.WHITE);
        add(timeLabel);

        rallyLabel = new JLabel("Rally: 0");
        rallyLabel.setBounds(900, 35, 100, 20);
        rallyLabel.setForeground(Color.WHITE);
        add(rallyLabel);

        // Progress bars
        team1Progress = new JProgressBar(0, WINNING_SCORE);
        team1Progress.setBounds(150, 10, 200, 20);
        team1Progress.setStringPainted(true);
        team1Progress.setForeground(new Color(70, 130, 180));
        add(team1Progress);

        team2Progress = new JProgressBar(0, WINNING_SCORE);
        team2Progress.setBounds(650, 10, 200, 20);
        team2Progress.setStringPainted(true);
        team2Progress.setForeground(new Color(220, 20, 60));
        add(team2Progress);

        // Weather selector
        String[] weatherOptions = {"Sunny", "Rainy", "Windy", "Night", "Indoor"};
        weatherSelector = new JComboBox<>(weatherOptions);
        weatherSelector.setBounds(20, 10, 100, 25);
        weatherSelector.addActionListener(e -> changeWeather(weatherSelector.getSelectedIndex()));
        add(weatherSelector);
    }

    private JButton createStyledButton(String text, int x, int y, Color color) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(color.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(color.brighter());
                } else {
                    g2.setColor(color);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 12));

                FontMetrics fm = g2.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(text)) / 2;
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, textX, textY);
                g2.dispose();
            }
        };

        button.setBounds(x, y, 90, 35);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setFocusable(false);

        button.addActionListener(e -> handleButtonAction(text));
        add(button);
        return button;
    }

    private void handleButtonAction(String buttonText) {
        switch (buttonText) {
            case "Main Menu": currentState = GameState.MENU; break;
            case "Pause":
                if (currentState == GameState.PLAYING) {
                    currentState = GameState.PAUSED;
                    pauseButton.setText("Resume");
                } else if (currentState == GameState.PAUSED) {
                    currentState = GameState.PLAYING;
                    pauseButton.setText("Pause");
                }
                break;
            case "Reset": resetGame(); break;
            case "Stats": showStats = !showStats; break;
            case "Replay": startReplay(); break;
        }
    }

    private void initializeCollections() {
        team1Players = new ArrayList<>();
        team2Players = new ArrayList<>();
        spectators = new ArrayList<>();
        confetti = new ArrayList<>();
        shadows = new ArrayList<>();
        weatherEffects = new ArrayList<>();
        particleSystems = new ArrayList<>();
        shockwaves = new ArrayList<>();
        textPopups = new ArrayList<>();
        ballTrail = new ArrayList<>();
    }

    private void createGameObjects() {
        // Create ball with advanced properties
        ball = new Ball(500, 300);

        // Create teams with dynamic positions
        createTeam1();
        createTeam2();

        // Create spectators
        createSpectators();

        // Initialize camera shake
        cameraShake = new CameraShake();

        // Initialize motion blur
        motionBlur = new MotionBlur();

        // Initialize power trail
        powerTrail = new TrailEffect();
    }

    private void createTeam1() {
        String[] names = {"Spiker", "Setter", "Blocker", "Libero", "Server"};
        String[] roles = {"Attacker", "Playmaker", "Defender", "Defense", "Specialist"};
        Color teamColor = new Color(70, 130, 180);

        int[][] positions = {
                {150, 350}, {200, 300}, {100, 300}, {150, 400}, {250, 350}
        };

        for (int i = 0; i < 5; i++) {
            Player p = new Player(
                    positions[i][0],
                    positions[i][1],
                    names[i],
                    roles[i],
                    1,
                    teamColor,
                    (int)(random.nextDouble() * 100)
            );
            team1Players.add(p);
            playerStats.put("Team1_" + names[i], new PlayerStats());
        }
    }

    private void createTeam2() {
        String[] names = {"Spiker", "Setter", "Blocker", "Libero", "Server"};
        String[] roles = {"Attacker", "Playmaker", "Defender", "Defense", "Specialist"};
        Color teamColor = new Color(220, 20, 60);

        int[][] positions = {
                {850, 350}, {800, 300}, {900, 300}, {850, 400}, {750, 350}
        };

        for (int i = 0; i < 5; i++) {
            Player p = new Player(
                    positions[i][0],
                    positions[i][1],
                    names[i],
                    roles[i],
                    2,
                    teamColor,
                    (int)(random.nextDouble() * 100)
            );
            team2Players.add(p);
            playerStats.put("Team2_" + names[i], new PlayerStats());
        }
    }

    private void createSpectators() {
        for (int i = 0; i < 50; i++) {
            spectators.add(new Spectator(
                    200 + i * 15,
                    550,
                    random.nextInt(100)
            ));
        }
    }

    private void startSplashAnimation() {
        Timer splashTimer = new Timer(3000, e -> {
            if (currentState == GameState.SPLASH) {
                currentState = GameState.MENU;
            }
        });
        splashTimer.setRepeats(false);
        splashTimer.start();
    }

    private void changeWeather(int weatherIndex) {
        weatherEffects.clear();
        switch (weatherIndex) {
            case 0: // Sunny
                isRaining = false;
                isNight = false;
                isIndoor = false;
                break;
            case 1: // Rainy
                isRaining = true;
                isNight = false;
                isIndoor = false;
                for (int i = 0; i < 100; i++) {
                    weatherEffects.add(new RainDrop());
                }
                break;
            case 2: // Windy
                windSpeed = 10;
                windDirection = Math.PI / 4;
                break;
            case 3: // Night
                isNight = true;
                isRaining = false;
                isIndoor = false;
                break;
            case 4: // Indoor
                isIndoor = true;
                isRaining = false;
                isNight = false;
                break;
        }
    }

    private void startReplay() {
        if (!replayFrames.isEmpty()) {
            currentState = GameState.REPLAY;
            replayFrameIndex = 0;
        }
    }

    private void updateGame() {
        if (currentState != GameState.PLAYING && currentState != GameState.REPLAY) return;

        frameCount++;
        gameTime += 0.016; // Approximately 60 FPS

        if (currentState == GameState.PLAYING) {
            // Update physics with speed multiplier
            double speedMultiplier = speedSlider.getValue() / 100.0;

            for (int i = 0; i < speedMultiplier; i++) {
                updateBallPhysics();
                updatePlayersAI();
                checkCollisions();
            }

            // Update environment
            updateEnvironment();

            // Record replay frame
            if (recordingReplay && frameCount % 5 == 0) {
                recordReplayFrame();
            }

            // Update UI labels
            updateUILabels();
        } else if (currentState == GameState.REPLAY) {
            playReplay();
        }

        // Update visual effects
        updateVisualEffects();

        // Clean up old effects
        cleanupEffects();

        repaint();
    }

    private void updatePhysics() {
        if (currentState != GameState.PLAYING) return;

        // High-precision physics updates
        applyAdvancedPhysics();
        updateBallSpin();
        updatePlayerFatigue();
    }

    private void updateAnimation() {
        // Update all animations
        updateSpectatorAnimations();
        updateParticleAnimations();
        updateWeatherEffects();
        updateCameraShake();
    }

    private void updateBallPhysics() {
        // Apply gravity
        ball.vy += GRAVITY;

        // Apply air resistance
        ball.vx *= AIR_RESISTANCE;
        ball.vy *= AIR_RESISTANCE;

        // Apply wind
        ball.vx += windSpeed * Math.cos(windDirection) * 0.05;
        ball.vy += windSpeed * Math.sin(windDirection) * 0.025;

        // Apply spin effect
        ball.vx += ball.spin * SPIN_FACTOR * ball.vy;

        // Update position
        ball.x += ball.vx;
        ball.y += ball.vy;

        // Add to trail
        ballTrail.add(new BallTrailPoint(ball.x, ball.y, ball.vx, ball.vy));
        if (ballTrail.size() > maxTrailLength) {
            ballTrail.remove(0);
        }

        // Speed limit
        double speed = Math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy);
        if (speed > MAX_SPEED) {
            ball.vx = (ball.vx / speed) * MAX_SPEED;
            ball.vy = (ball.vy / speed) * MAX_SPEED;
        }
    }

    private void applyAdvancedPhysics() {
        // Magnus effect (ball curve)
        if (Math.abs(ball.spin) > 0.1) {
            double magnusForce = ball.spin * 0.001;
            ball.vx += magnusForce * ball.vy;
            ball.vy -= magnusForce * ball.vx;
        }

        // Buoyancy effect (ball floats differently in different conditions)
        if (isRaining) {
            ball.vy -= 0.01; // Rain makes ball slightly lighter
        }

        if (isIndoor) {
            ball.vx *= 0.998; // Less air resistance indoors
        }
    }

    private void updateBallSpin() {
        // Spin decays over time
        ball.spin *= 0.99;

        // Spin affects trajectory
        if (Math.abs(ball.spin) > 0.5) {
            createSpinTrail(ball.x, ball.y);
        }
    }

    private void updatePlayersAI() {
        // Team 1 AI
        for (Player player : team1Players) {
            player.update(ball, true, gameTime);
            updatePlayerFatigue(player);
        }

        // Team 2 AI
        for (Player player : team2Players) {
            player.update(ball, false, gameTime);
            updatePlayerFatigue(player);
        }
    }

    private void updatePlayerFatigue(Player player) {
        // Players get tired over time
        player.stamina -= 0.001;
        if (player.stamina < 0) player.stamina = 0;

        // Recover stamina when not moving
        if (Math.abs(player.vx) < 0.1 && Math.abs(player.vy) < 0.1) {
            player.stamina += 0.002;
            if (player.stamina > 1) player.stamina = 1;
        }
    }

    private void updatePlayerFatigue() {
        // Bulk update for all players
        for (Player p : team1Players) updatePlayerFatigue(p);
        for (Player p : team2Players) updatePlayerFatigue(p);
    }

    private void checkCollisions() {
        // Boundary collisions
        checkBoundaryCollisions();

        // Player collisions
        checkPlayerCollisions();

        // Net collision
        checkNetCollision();

        // Scoring
        checkScoring();
    }

    private void checkBoundaryCollisions() {
        // Floor collision
        if (ball.y > COURT_FLOOR_Y - 20) {
            ball.y = COURT_FLOOR_Y - 20;
            ball.vy = -ball.vy * 0.6;
            createImpactEffect(ball.x, ball.y, "floor");

            if (Math.abs(ball.vy) < 1) {
                // Ball stopped rolling
                checkScoring();
            }
        }

        // Side walls
        if (ball.x < 100 || ball.x > 900) {
            ball.vx = -ball.vx * 0.7;
            ball.x = ball.x < 100 ? 100 : 900;
            createImpactEffect(ball.x, ball.y, "wall");
        }

        // Ceiling
        if (ball.y < 100) {
            ball.y = 100;
            ball.vy = -ball.vy * 0.5;
            createImpactEffect(ball.x, ball.y, "ceiling");
        }
    }

    private void checkPlayerCollisions() {
        // Team 1 hits
        for (Player player : team1Players) {
            if (player.canHitBall(ball) && ball.x < 500) {
                performHit(player, 1);
            }
        }

        // Team 2 hits
        for (Player player : team2Players) {
            if (player.canHitBall(ball) && ball.x > 500) {
                performHit(player, 2);
            }
        }
    }

    private void performHit(Player player, int team) {
        // Calculate hit power based on player type and stamina
        double hitPower = player.getHitPower();

        // Direction based on player position and aim
        double hitAngle = calculateHitAngle(player, team);

        // Apply hit force
        ball.vx = Math.cos(hitAngle) * hitPower * 15;
        ball.vy = Math.sin(hitAngle) * hitPower * 10 - 5;

        // Add spin based on hit type
        if (player.role.equals("Spiker")) {
            ball.spin += (random.nextDouble() - 0.5) * 2;
        }

        // Update stats
        rallyCount++;
        lastHitBy = player.name + " (Team " + team + ")";
        ball.lastHitBy = lastHitBy;
        ballPossessionTime = 0;

        // Create effects
        createHitEffect(ball.x, ball.y, hitPower);
        player.performHit();

        // Add camera shake for powerful hits
        if (hitPower > 0.8) {
            cameraShake.shake(10, 5);
        }

        // Update player stats
        PlayerStats stats = playerStats.get("Team" + team + "_" + player.name);
        if (stats != null) {
            stats.hits++;
            if (hitPower > 0.9) stats.powerHits++;
        }

        // Create text popup
        textPopups.add(new TextPopup(
                ball.x, ball.y - 30,
                player.getHitType() + "!",
                player.team == 1 ? Color.BLUE : Color.RED
        ));
    }

    private double calculateHitAngle(Player player, int team) {
        double baseAngle = team == 1 ? 0.2 : Math.PI - 0.2;

        // Add randomness based on player skill
        double accuracy = player.skill / 100.0;
        double randomness = (random.nextDouble() - 0.5) * (1 - accuracy) * 0.5;

        return baseAngle + randomness;
    }

    private void checkNetCollision() {
        if (ball.x > 490 && ball.x < 510) {
            if (ball.y > 250 && ball.y < 450) {
                // Ball hit the net
                ball.vx = -ball.vx * 0.3;
                ball.spin *= 2;

                createImpactEffect(ball.x, ball.y, "net");

                // Check if it's a net fault
                if (ball.vy > 0) {
                    // Ball was going down, point to other team
                    if (ball.x < 500) {
                        team2Score++;
                    } else {
                        team1Score++;
                    }
                    createScorePopup("Net Fault!");
                }
            }
        }
    }

    private void checkScoring() {
        // Check if ball hit the floor
        if (ball.y > COURT_FLOOR_Y - 20 && Math.abs(ball.vy) < 0.5) {
            if (ball.x < 500) {
                // Left side - Team 2 scores
                if (!lastHitBy.contains("Team 2")) {
                    team2Score++;
                    createScorePopup("Team 2 Scores!");
                    updateSetScore(2);
                }
            } else {
                // Right side - Team 1 scores
                if (!lastHitBy.contains("Team 1")) {
                    team1Score++;
                    createScorePopup("Team 1 Scores!");
                    updateSetScore(1);
                }
            }

            // Reset ball
            resetRally();

            // Check for set win
            checkSetWin();
        }
    }

    private void checkSetWin() {
    }

    private void updateSetScore(int scoringTeam) {
        if (team1Score >= WINNING_SCORE || team2Score >= WINNING_SCORE) {
            if (team1Score >= WINNING_SCORE) {
                team1Sets++;
                createConfetti(1);
            } else {
                team2Sets++;
                createConfetti(2);
            }

            if (team1Sets >= SETS_TO_WIN || team2Sets >= SETS_TO_WIN) {
                currentState = GameState.GAME_OVER;
                createChampionshipEffect();
            } else {
                // Next set
                currentSet++;
                resetSet();
            }
        }
    }

    private void updateEnvironment() {
        // Update time of day
        timeOfDay += 0.001;
        if (timeOfDay > 24) timeOfDay = 0;

        // Update crowd noise based on action
        double ballSpeed = Math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy);
        crowdNoise = crowdNoise * 0.95 + (ballSpeed / MAX_SPEED) * 0.05;

        // Random wind changes
        if (random.nextInt(100) < 2 && !isIndoor) {
            windSpeed = random.nextDouble() * 8;
            windDirection = random.nextDouble() * Math.PI * 2;
        }
    }

    private void updateVisualEffects() {
        // Update particle systems
        particleSystems.removeIf(p -> !p.update());

        // Update shockwaves
        shockwaves.removeIf(s -> !s.update());

        // Update text popups
        textPopups.removeIf(t -> !t.update());

        // Update confetti
        confetti.removeIf(c -> !c.update());

        // Update camera shake
        cameraShake.update();

        // Update power trail
        powerTrail.update(ball);
    }

    private void cleanupEffects() {
        // Limit trail length
        while (ballTrail.size() > maxTrailLength) {
            ballTrail.remove(0);
        }
    }

    private void updateUILabels() {
        timeLabel.setText(String.format("Time: %02d:%02d",
                (int)gameTime % 60, (int)(gameTime * 60) % 60));
        rallyLabel.setText("Rally: " + rallyCount);
        speedLabel.setText("Speed: " + speedSlider.getValue() + "%");

        team1Progress.setValue(team1Score);
        team2Progress.setValue(team2Score);
        team1Progress.setString("Team 1: " + team1Score);
        team2Progress.setString("Team 2: " + team2Score);
    }

    private void recordReplayFrame() {
        replayFrames.add(new GameSnapshot(
                ball, team1Players, team2Players, team1Score, team2Score
        ));

        // Limit replay size
        if (replayFrames.size() > 300) {
            replayFrames.remove(0);
        }
    }

    private void playReplay() {
        if (replayFrameIndex < replayFrames.size()) {
            GameSnapshot frame = replayFrames.get(replayFrameIndex);
            ball = frame.ball;
            team1Players = frame.team1Players;
            team2Players = frame.team2Players;
            team1Score = frame.team1Score;
            team2Score = frame.team2Score;
            replayFrameIndex++;
        } else {
            currentState = GameState.PLAYING;
        }
    }

    private void createImpactEffect(double x, double y, String type) {
        ParticleSystem ps = new ParticleSystem(x, y, type);
        particleSystems.add(ps);

        // Add shockwave for hard impacts
        if (type.equals("floor") && Math.abs(ball.vy) > 5) {
            shockwaves.add(new Shockwave(x, y));
        }
    }

    private void createHitEffect(double x, double y, double power) {
        ParticleSystem ps = new ParticleSystem(x, y, "hit", power);
        particleSystems.add(ps);
    }

    private void createSpinTrail(double x, double y) {
        if (frameCount % 3 == 0) {
            ParticleSystem ps = new ParticleSystem(x, y, "spin");
            particleSystems.add(ps);
        }
    }

    private void createScorePopup(String text) {
        textPopups.add(new TextPopup(500, 200, text, Color.YELLOW));
    }

    private void createConfetti(int team) {
        for (int i = 0; i < 50; i++) {
            confetti.add(new Confetti(
                    500, 300,
                    team == 1 ? Color.BLUE : Color.RED
            ));
        }
    }

    private void createChampionshipEffect() {
        for (int i = 0; i < 200; i++) {
            confetti.add(new Confetti(
                    500, 300,
                    new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255))
            ));
        }
    }

    private void resetRally() {
        ball.reset(500, 300);
        rallyCount = 0;
        maxRallyRecord = Math.max(maxRallyRecord, rallyCount);
        ballPossessionTime = 0;
        ballTrail.clear();
    }

    private void resetSet() {
        team1Score = 0;
        team2Score = 0;
        resetRally();
    }

    private void resetGame() {
        team1Score = 0;
        team2Score = 0;
        team1Sets = 0;
        team2Sets = 0;
        currentSet = 1;
        gameTime = 0;
        rallyCount = 0;
        maxRallyRecord = 0;
        resetRally();
        currentState = GameState.MENU;
    }

    private void updateSpectatorAnimations() {
        for (Spectator s : spectators) {
            s.update(crowdNoise);
        }
    }

    private void updateParticleAnimations() {
        for (ParticleSystem ps : particleSystems) {
            ps.animate();
        }
    }

    private void updateWeatherEffects() {
        if (isRaining) {
            for (WeatherEffect we : weatherEffects) {
                we.update();
            }
        }
    }

    private void updateCameraShake() {
        cameraShake.update();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Enable advanced rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Apply camera transformations
        applyCameraTransform(g2d);

        // Draw background
        drawBackground(g2d);

        // Draw court
        drawCourt(g2d);

        // Draw shadows
        drawShadows(g2d);

        // Draw net with 3D effect
        drawNet3D(g2d);

        // Draw spectators
        drawSpectators(g2d);

        // Draw players
        drawPlayers(g2d);

        // Draw ball trail
        drawBallTrail(g2d);

        // Draw ball
        ball.draw(g2d, frameCount);

        // Draw visual effects
        drawParticleSystems(g2d);
        drawShockwaves(g2d);
        drawTextPopups(g2d);
        drawConfetti(g2d);
        drawWeatherEffects(g2d);

        // Draw power trail
        powerTrail.draw(g2d);

        // Draw UI overlays
        g2d.setTransform(new AffineTransform()); // Reset transform for UI
        drawScoreboard(g2d);
        drawStatistics(g2d);
        drawHeatmap(g2d);
        drawGameStateOverlay(g2d);

        // Draw motion blur
        if (slowMotion) {
            motionBlur.apply(g2d);
        }

        g2d.dispose();
    }

    private void applyCameraTransform(Graphics2D g2d) {
        if (followingBall) {
            cameraX = ball.x - COURT_WIDTH / 2;
            cameraY = ball.y - COURT_HEIGHT / 2;
        }

        // Apply camera shake
        cameraX += cameraShake.getOffsetX();
        cameraY += cameraShake.getOffsetY();

        // Apply zoom
        g2d.scale(zoom, zoom);
        g2d.translate(-cameraX, -cameraY);
    }

    private void drawBackground(Graphics2D g2d) {
        if (isIndoor) {
            // Indoor arena
            g2d.setColor(new Color(30, 30, 30));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Arena lights
            for (int i = 0; i < 5; i++) {
                int x = 200 + i * 200;
                GradientPaint light = new GradientPaint(
                        x, 0, new Color(255, 255, 200, 50),
                        x, 200, new Color(255, 255, 200, 0)
                );
                g2d.setPaint(light);
                g2d.fillRect(x - 100, 0, 200, 200);
            }
        } else if (isNight) {
            // Night sky with stars
            g2d.setColor(new Color(10, 10, 30));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Stars
            g2d.setColor(Color.WHITE);
            for (int i = 0; i < 100; i++) {
                int x = (i * 137) % getWidth();
                int y = (i * 281) % getHeight();
                int brightness = (int)(Math.sin(gameTime + i) * 50 + 200);
                g2d.setColor(new Color(brightness, brightness, brightness));
                g2d.fillRect(x, y, 1, 1);
            }

            // Moon
            g2d.setColor(new Color(255, 255, 200));
            g2d.fillOval(800, 50, 60, 60);
        } else {
            // Day sky gradient
            GradientPaint sky = new GradientPaint(
                    0, 0, new Color(135, 206, 235),
                    0, getHeight(), new Color(255, 255, 200)
            );
            g2d.setPaint(sky);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Sun
            g2d.setColor(new Color(255, 255, 150, 100));
            g2d.fillOval(100, 50, 80, 80);
        }
    }

    private void drawCourt(Graphics2D g2d) {
        // Court floor with texture
        TexturePaint courtTexture = createCourtTexture();
        g2d.setPaint(courtTexture);
        g2d.fillRect(100, 200, 800, 350);

        // Court lines with 3D effect
        g2d.setStroke(new BasicStroke(3));

        // Main lines
        g2d.setColor(Color.WHITE);
        g2d.drawRect(100, 200, 800, 350);
        g2d.drawLine(500, 200, 500, 550);

        // Attack lines
        g2d.drawLine(250, 200, 250, 550);
        g2d.drawLine(750, 200, 750, 550);

        // Service areas
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10}, 0));
        g2d.drawLine(150, 550, 450, 550);
        g2d.drawLine(550, 550, 850, 550);

        // Center circle
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(475, 350, 50, 50);
    }

    private TexturePaint createCourtTexture() {
        BufferedImage texture = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = texture.createGraphics();
        g.setColor(new Color(222, 184, 135));
        g.fillRect(0, 0, 20, 20);
        g.setColor(new Color(205, 170, 125));
        g.drawLine(0, 0, 20, 20);
        g.drawLine(20, 0, 0, 20);
        g.dispose();
        return new TexturePaint(texture, new Rectangle(0, 0, 20, 20));
    }

    private void drawNet3D(Graphics2D g2d) {
        // Net posts with shadow
        g2d.setColor(new Color(100, 100, 100));
        g2d.fillRect(495, 150, 5, 400);
        g2d.fillRect(500, 150, 5, 400);

        // Net with transparency
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.setStroke(new BasicStroke(1));

        // Horizontal lines
        for (int y = 200; y <= 500; y += 15) {
            g2d.drawLine(490, y, 510, y);
        }

        // Vertical lines
        for (int x = 490; x <= 510; x += 5) {
            g2d.drawLine(x, 200, x, 500);
        }

        // Top of net
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(485, 190, 515, 190);
    }

    private void drawShadows(Graphics2D g2d) {
        // Player shadows
        for (Player p : team1Players) {
            drawPlayerShadow(g2d, p);
        }
        for (Player p : team2Players) {
            drawPlayerShadow(g2d, p);
        }

        // Ball shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval((int)ball.x - 15, (int)ball.y + 10, 30, 10);
    }

    private void drawPlayerShadow(Graphics2D g2d, Player p) {
        double shadowScale = 1.0 - (p.y - 200) / 350.0;
        int shadowWidth = (int)(30 * shadowScale);
        int shadowHeight = (int)(15 * shadowScale);

        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(
                (int)p.x - shadowWidth/2,
                (int)550 - shadowHeight/2,
                shadowWidth,
                shadowHeight
        );
    }

    private void drawPlayers(Graphics2D g2d) {
        for (Player p : team1Players) {
            p.draw(g2d, frameCount, isNight);
        }
        for (Player p : team2Players) {
            p.draw(g2d, frameCount, isNight);
        }
    }

    private void drawSpectators(Graphics2D g2d) {
        for (Spectator s : spectators) {
            s.draw(g2d);
        }
    }

    private void drawBallTrail(Graphics2D g2d) {
        for (int i = 0; i < ballTrail.size(); i++) {
            BallTrailPoint p = ballTrail.get(i);
            float alpha = (float)i / ballTrail.size();
            g2d.setColor(new Color(255, 255, 255, (int)(alpha * 100)));
            g2d.fillOval((int)p.x - 5, (int)p.y - 5, 10, 10);
        }
    }

    private void drawParticleSystems(Graphics2D g2d) {
        for (ParticleSystem ps : particleSystems) {
            ps.draw(g2d);
        }
    }

    private void drawShockwaves(Graphics2D g2d) {
        for (Shockwave s : shockwaves) {
            s.draw(g2d);
        }
    }

    private void drawTextPopups(Graphics2D g2d) {
        for (TextPopup tp : textPopups) {
            tp.draw(g2d);
        }
    }

    private void drawConfetti(Graphics2D g2d) {
        for (Confetti c : confetti) {
            c.draw(g2d);
        }
    }

    private void drawWeatherEffects(Graphics2D g2d) {
        if (isRaining) {
            for (WeatherEffect we : weatherEffects) {
                we.draw(g2d);
            }
        }
    }

    private void drawScoreboard(Graphics2D g2d) {
        // Main scoreboard
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(300, 10, 400, 60, 20, 20);

        // Scores
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.drawString(String.format("%02d  -  %02d", team1Score, team2Score), 380, 55);

        // Set scores
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.drawString("Sets: " + team1Sets + " - " + team2Sets, 450, 30);

        // Rally count
        g2d.drawString("Rally: " + rallyCount + " (Record: " + maxRallyRecord + ")", 550, 30);
    }

    private void drawStatistics(Graphics2D g2d) {
        if (!showStats) return;

        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(100, 100, 300, 400);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("Game Statistics", 150, 130);

        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        int y = 170;
        g2d.drawString("Total Hits: " + gameStats.totalHits, 120, y); y += 25;
        g2d.drawString("Aces: " + gameStats.aces, 120, y); y += 25;
        g2d.drawString("Blocks: " + gameStats.blocks, 120, y); y += 25;
        g2d.drawString("Spikes: " + gameStats.spikes, 120, y); y += 25;
        g2d.drawString("Digs: " + gameStats.digs, 120, y); y += 25;
        g2d.drawString("Errors: " + gameStats.errors, 120, y); y += 25;
        g2d.drawString("Hit %: " + gameStats.getHitPercentage() + "%", 120, y);
    }

    private void drawHeatmap(Graphics2D g2d) {
        if (!showHeatmap) return;

        // Simple heatmap overlay
        g2d.setColor(new Color(255, 0, 0, 50));
        for (int i = 0; i < 100; i++) {
            int x = 200 + random.nextInt(600);
            int y = 250 + random.nextInt(300);
            g2d.fillOval(x, y, 20, 20);
        }
    }

    private void drawGameStateOverlay(Graphics2D g2d) {
        switch (currentState) {
            case SPLASH:
                drawSplashScreen(g2d);
                break;
            case MENU:
                drawMenuScreen(g2d);
                break;
            case PAUSED:
                drawPausedScreen(g2d);
                break;
            case GAME_OVER:
                drawGameOverScreen(g2d);
                break;
            case SETTINGS:
                drawSettingsScreen(g2d);
                break;
        }
    }

    private void drawSplashScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Animated title
        float alpha = (float)(Math.sin(gameTime * 2) * 0.3 + 0.7);
        g2d.setColor(new Color(255, 255, 255, (int)(alpha * 255)));
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        g2d.drawString("VOLLEYBALL", 250, 250);
        g2d.setFont(new Font("Arial", Font.PLAIN, 36));
        g2d.drawString("ELITE", 450, 320);
    }

    private void drawMenuScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString("VOLLEYBALL ELITE", 280, 200);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Press SPACE to Start", 350, 300);
        g2d.drawString("Press S for Settings", 350, 350);
        g2d.drawString("Press H for Heatmap", 350, 400);
        g2d.drawString("Press F to Follow Ball", 350, 450);
        g2d.drawString("Press +/- to Zoom", 350, 500);
    }

    private void drawPausedScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        g2d.drawString("PAUSED", 350, 300);
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));

        String winner = team1Sets >= SETS_TO_WIN ? "TEAM BLUE WINS!" : "TEAM RED WINS!";
        g2d.drawString(winner, 300, 250);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Final Score: " + team1Sets + " - " + team2Sets, 350, 320);
        g2d.drawString("Press R to Restart", 350, 370);
        g2d.drawString("Press M for Menu", 350, 420);
    }

    private void drawSettingsScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.drawString("SETTINGS", 400, 150);

        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("1. Indoor Mode: " + (isIndoor ? "ON" : "OFF"), 350, 220);
        g2d.drawString("2. Night Mode: " + (isNight ? "ON" : "OFF"), 350, 260);
        g2d.drawString("3. Rain: " + (isRaining ? "ON" : "OFF"), 350, 300);
        g2d.drawString("4. Wind Speed: " + windSpeed, 350, 340);
        g2d.drawString("5. Slow Motion: " + (slowMotion ? "ON" : "OFF"), 350, 380);
    }

    // Inner classes for game objects
    class Ball {
        double x, y, vx, vy, spin;
        String lastHitBy;
        double initialX, initialY;
        Color color;

        public Ball(double x, double y) {
            this.x = x;
            this.y = y;
            this.initialX = x;
            this.initialY = y;
            this.vx = 0;
            this.vy = 0;
            this.spin = 0;
            this.lastHitBy = "";
            this.color = new Color(255, 215, 0);
        }

        public void reset(double x, double y) {
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = 0;
            this.spin = 0;
        }

        public void draw(Graphics2D g2d, int frameCount) {
            // Draw glow effect
            for (int i = 3; i > 0; i--) {
                int alpha = 50 - i * 10;
                g2d.setColor(new Color(255, 255, 255, alpha));
                g2d.fillOval((int)x - 15 - i, (int)y - 15 - i, 30 + i * 2, 30 + i * 2);
            }

            // Draw ball
            g2d.setColor(color);
            g2d.fillOval((int)x - 12, (int)y - 12, 24, 24);

            // Draw spin indicator
            if (Math.abs(spin) > 0.1) {
                g2d.setColor(new Color(255, 255, 255, (int)(Math.abs(spin) * 100)));
                g2d.drawArc((int)x - 15, (int)y - 15, 30, 30, 0, (int)(spin * 100));
            }

            // Ball details
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawOval((int)x - 12, (int)y - 12, 24, 24);

            // Moving stripes based on spin
            double rotation = frameCount * spin * 0.1;
            g2d.rotate(rotation, x, y);
            g2d.drawLine((int)x - 8, (int)y, (int)x + 8, (int)y);
            g2d.drawLine((int)x, (int)y - 8, (int)x, (int)y + 8);
            g2d.rotate(-rotation, x, y);
        }
    }

    class Player {
        double x, y, targetX, targetY;
        double vx, vy;
        String name, role;
        int team, number;
        Color color;
        double stamina = 1.0;
        double skill;
        double hitCooldown = 0;

        public Player(double x, double y, String name, String role, int team, Color color, int number) {
            this.x = x;
            this.y = y;
            this.targetX = x;
            this.targetY = y;
            this.name = name;
            this.role = role;
            this.team = team;
            this.color = color;
            this.number = number;
            this.skill = random.nextDouble() * 30 + 70; // 70-100 skill
        }

        public void update(Ball ball, boolean isLeftTeam, double gameTime) {
            // AI movement
            if (isLeftTeam && ball.x < 500 || !isLeftTeam && ball.x > 500) {
                targetX = ball.x;
                targetY = ball.y - 50;
            }

            // Move towards target
            double dx = targetX - x;
            double dy = targetY - y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > 5) {
                double moveSpeed = 3 * stamina;
                vx = (dx / dist) * moveSpeed;
                vy = (dy / dist) * moveSpeed;

                x += vx;
                y += vy;
            }

            // Stay in bounds
            x = Math.max(150, Math.min(850, x));
            y = Math.max(200, Math.min(500, y));

            // Update cooldown
            if (hitCooldown > 0) {
                hitCooldown -= 0.016;
            }
        }

        public boolean canHitBall(Ball ball) {
            double dist = Math.sqrt(Math.pow(x - ball.x, 2) + Math.pow(y - ball.y, 2));
            return dist < 50 && hitCooldown <= 0;
        }

        public double getHitPower() {
            double basePower = 0.7;
            if (role.equals("Spiker")) basePower += 0.2;
            if (role.equals("Server")) basePower += 0.1;

            return basePower * stamina * (skill / 100.0);
        }

        public String getHitType() {
            double r = random.nextDouble();
            if (r < 0.3) return "Spike";
            if (r < 0.6) return "Set";
            if (r < 0.8) return "Dig";
            return "Block";
        }

        public void performHit() {
            hitCooldown = 0.5;
            stamina -= 0.05;
        }

        public void draw(Graphics2D g2d, int frameCount, boolean isNight) {
            // Draw player based on role
            int playerHeight = 40;
            int playerWidth = 25;

            // Draw body
            g2d.setColor(color);
            g2d.fillRoundRect((int)x - playerWidth/2, (int)y - playerHeight, playerWidth, playerHeight, 10, 10);

            // Draw head
            g2d.setColor(team == 1 ? new Color(255, 220, 180) : new Color(255, 200, 150));
            g2d.fillOval((int)x - 10, (int)y - playerHeight - 15, 20, 20);

            // Draw uniform details
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(String.valueOf(number), (int)x - 5, (int)y - playerHeight + 20);

            // Draw name
            g2d.setFont(new Font("Arial", Font.PLAIN, 8));
            g2d.drawString(name, (int)x - 15, (int)y - playerHeight - 20);

            // Draw role indicator
            if (role.equals("Spiker")) {
                g2d.setColor(Color.YELLOW);
                g2d.fillRect((int)x + 15, (int)y - 25, 5, 10);
            }

            // Draw stamina bar
            g2d.setColor(Color.GREEN);
            g2d.fillRect((int)x - 20, (int)y - playerHeight - 25, (int)(40 * stamina), 3);

            // Glow effect for night
            if (isNight) {
                g2d.setColor(new Color(255, 255, 255, 50));
                g2d.fillOval((int)x - 20, (int)y - 50, 40, 60);
            }
        }
    }

    class ParticleSystem {
        ArrayList<Particle> particles;
        String type;

        public ParticleSystem(double x, double y, String type) {
            this.particles = new ArrayList<>();
            this.type = type;
            createParticles(x, y);
        }

        public ParticleSystem(double x, double y, String type, double power) {
            this(x, y, type);
            // Scale particles by power
        }

        private void createParticles(double x, double y) {
            int count = type.equals("hit") ? 20 : 10;
            for (int i = 0; i < count; i++) {
                particles.add(new Particle(x, y, type));
            }
        }

        public boolean update() {
            particles.removeIf(p -> !p.update());
            return !particles.isEmpty();
        }

        public void animate() {
            for (Particle p : particles) {
                p.move();
            }
        }

        public void draw(Graphics2D g2d) {
            for (Particle p : particles) {
                p.draw(g2d);
            }
        }
    }

    class Particle {
        double x, y, vx, vy;
        int life, maxLife;
        Color color;
        String type;

        public Particle(double x, double y, String type) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.vx = random.nextDouble() * 8 - 4;
            this.vy = random.nextDouble() * 8 - 4;
            this.life = 50;
            this.maxLife = 50;

            switch(type) {
                case "hit": color = Color.YELLOW; break;
                case "floor": color = new Color(139, 69, 19); break;
                case "wall": color = Color.GRAY; break;
                case "net": color = Color.WHITE; break;
                default: color = Color.WHITE;
            }
        }

        public boolean update() {
            life--;
            return life > 0;
        }

        public void move() {
            x += vx;
            y += vy;
            vy += 0.2; // Gravity
        }

        public void draw(Graphics2D g2d) {
            float alpha = (float)life / maxLife;
            g2d.setColor(new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    (int)(alpha * 255)
            ));
            g2d.fillRect((int)x, (int)y, 3, 3);
        }
    }

    class Shockwave {
        double x, y;
        double radius;
        double maxRadius;
        int life;

        public Shockwave(double x, double y) {
            this.x = x;
            this.y = y;
            this.radius = 10;
            this.maxRadius = 100;
            this.life = 30;
        }

        public boolean update() {
            radius += 5;
            life--;
            return life > 0;
        }

        public void draw(Graphics2D g2d) {
            float alpha = (float)life / 30;
            g2d.setColor(new Color(255, 255, 255, (int)(alpha * 100)));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval((int)(x - radius), (int)(y - radius), (int)radius * 2, (int)radius * 2);
        }
    }

    class TextPopup {
        double x, y;
        String text;
        Color color;
        int life;

        public TextPopup(double x, double y, String text, Color color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
            this.life = 60;
        }

        public boolean update() {
            y -= 1;
            life--;
            return life > 0;
        }

        public void draw(Graphics2D g2d) {
            float alpha = (float)life / 60;
            g2d.setColor(new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    (int)(alpha * 255)
            ));
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString(text, (int)x, (int)y);
        }
    }

    class Confetti {
        double x, y, vx, vy;
        Color color;
        int life;
        int rotation;

        public Confetti(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.vx = random.nextDouble() * 10 - 5;
            this.vy = random.nextDouble() * 10 - 8;
            this.color = color;
            this.life = 100;
            this.rotation = random.nextInt(360);
        }

        public boolean update() {
            x += vx;
            y += vy;
            vy += 0.2;
            rotation += 5;
            life--;
            return life > 0;
        }

        public void draw(Graphics2D g2d) {
            float alpha = (float)life / 100;
            g2d.rotate(Math.toRadians(rotation), x, y);
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255)));
            g2d.fillRect((int)x, (int)y, 5, 5);
            g2d.rotate(-Math.toRadians(rotation), x, y);
        }
    }

    class Spectator {
        double x, y;
        int mood;
        double jumpHeight;

        public Spectator(double x, double y, int mood) {
            this.x = x;
            this.y = y;
            this.mood = mood;
            this.jumpHeight = 0;
        }

        public void update(double crowdNoise) {
            if (crowdNoise > 0.5 && random.nextInt(10) == 0) {
                jumpHeight = 10;
            }
            jumpHeight *= 0.9;
        }

        public void draw(Graphics2D g2d) {
            // Simple spectator figure
            g2d.setColor(new Color(100, 100, 100));
            g2d.fillOval((int)x, (int)(y - jumpHeight), 10, 15);
            g2d.setColor(new Color(255, 200, 150));
            g2d.fillOval((int)x + 2, (int)(y - jumpHeight - 5), 6, 6);
        }
    }

    class WeatherEffect {
        double x, y;
        double vx, vy;

        public void update() {
            x += vx;
            y += vy;
        }

        public void draw(Graphics2D g2d) {
            // Overridden by subclasses
        }
    }

    class RainDrop extends WeatherEffect {
        public RainDrop() {
            this.x = random.nextInt(1000);
            this.y = random.nextInt(800);
            this.vx = 1;
            this.vy = 5;
        }

        @Override
        public void update() {
            super.update();
            if (y > 800) y = 0;
            if (x > 1000) x = 0;
        }

        @Override
        public void draw(Graphics2D g2d) {
            g2d.setColor(new Color(200, 200, 255, 100));
            g2d.drawLine((int)x, (int)y, (int)x - 2, (int)y - 10);
        }
    }

    class BallTrailPoint {
        double x, y, vx, vy;

        public BallTrailPoint(double x, double y, double vx, double vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }

    class TrailEffect {
        ArrayList<Point2D.Double> points;

        public TrailEffect() {
            points = new ArrayList<>();
        }

        public void update(Ball ball) {
            points.add(new Point2D.Double(ball.x, ball.y));
            if (points.size() > 20) {
                points.remove(0);
            }
        }

        public void draw(Graphics2D g2d) {
            for (int i = 0; i < points.size(); i++) {
                Point2D.Double p = points.get(i);
                float alpha = (float)i / points.size();
                g2d.setColor(new Color(255, 255, 255, (int)(alpha * 100)));
                g2d.fillOval((int)p.x - 5, (int)p.y - 5, 10, 10);
            }
        }
    }

    class CameraShake {
        double intensity = 0;
        double duration = 0;

        public void shake(double intensity, double duration) {
            this.intensity = intensity;
            this.duration = duration;
        }

        public void update() {
            if (duration > 0) {
                duration--;
                intensity *= 0.9;
            } else {
                intensity = 0;
            }
        }

        public double getOffsetX() {
            return (random.nextDouble() - 0.5) * intensity;
        }

        public double getOffsetY() {
            return (random.nextDouble() - 0.5) * intensity;
        }
    }

    class MotionBlur {
        BufferedImage previousFrame;

        public void apply(Graphics2D g2d) {
            // Simple motion blur effect
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        }
    }

    class PlayerStats {
        int hits = 0;
        int powerHits = 0;
        int blocks = 0;
        int errors = 0;
    }

    class Statistics {
        int totalHits = 0;
        int aces = 0;
        int blocks = 0;
        int spikes = 0;
        int digs = 0;
        int errors = 0;

        public int getHitPercentage() {
            if (totalHits == 0) return 0;
            return (totalHits - errors) * 100 / totalHits;
        }
    }

    class GameSnapshot {
        Ball ball;
        ArrayList<Player> team1Players;
        ArrayList<Player> team2Players;
        int team1Score;
        int team2Score;

        public GameSnapshot(Ball ball, ArrayList<Player> team1, ArrayList<Player> team2, int s1, int s2) {
            this.ball = new Ball(ball.x, ball.y);
            this.ball.vx = ball.vx;
            this.ball.vy = ball.vy;
            // Copy players (simplified)
            this.team1Players = new ArrayList<>(team1);
            this.team2Players = new ArrayList<>(team2);
            this.team1Score = s1;
            this.team2Score = s2;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE:
                if (currentState == GameState.MENU) {
                    currentState = GameState.PLAYING;
                } else if (currentState == GameState.PLAYING && !spacePressed) {
                    // Serve
                    ball.vx = random.nextDouble() * 4 - 2;
                    ball.vy = -8;
                    spacePressed = true;
                }
                break;
            case KeyEvent.VK_H:
                showHeatmap = !showHeatmap;
                break;
            case KeyEvent.VK_F:
                followingBall = !followingBall;
                break;
            case KeyEvent.VK_S:
                if (currentState == GameState.PLAYING) {
                    currentState = GameState.SETTINGS;
                } else if (currentState == GameState.SETTINGS) {
                    currentState = GameState.PLAYING;
                }
                break;
            case KeyEvent.VK_R:
                if (currentState == GameState.GAME_OVER) {
                    resetGame();
                }
                break;
            case KeyEvent.VK_M:
                currentState = GameState.MENU;
                break;
            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_EQUALS:
                zoom = Math.min(zoom + 0.1, 2.0);
                break;
            case KeyEvent.VK_MINUS:
                zoom = Math.max(zoom - 0.1, 0.5);
                break;
            case KeyEvent.VK_SHIFT:
                shiftPressed = true;
                slowMotion = true;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE:
                spacePressed = false;
                break;
            case KeyEvent.VK_SHIFT:
                shiftPressed = false;
                slowMotion = false;
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        mousePressed = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mousePressed = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {
        mousePos = e.getPoint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mousePos = e.getPoint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Volleyball Game Simulation - Elite Edition");
            VolleyballGameSimulation game = new VolleyballGameSimulation();

            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(true);
            frame.setVisible(true);
        });
    }
}