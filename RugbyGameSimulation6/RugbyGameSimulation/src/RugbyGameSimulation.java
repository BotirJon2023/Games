import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RugbyGameSimulation extends JPanel implements ActionListener, KeyListener {

    // Game constants
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 700;
    private static final int FIELD_MARGIN = 50;
    private static final int PLAYER_RADIUS = 12;
    private static final int BALL_RADIUS = 6;
    private static final int TEAM_SIZE = 15;
    private static final int TRY_ZONE_WIDTH = 80;
    private static final int GOAL_POST_WIDTH = 60;
    private static final int GOAL_POST_HEIGHT = 30;

    // Game state
    private enum GameState { KICKOFF, PLAYING, SCRUM, LINEOUT, TRY_SCORED, CONVERSION, HALFTIME, GAMEOVER }
    private GameState gameState = GameState.KICKOFF;

    // Teams
    private List<Player> teamA;
    private List<Player> teamB;
    private Player ballCarrier;
    private Player selectedPlayer;

    // Ball
    private Ball ball;
    private boolean ballInAir = false;

    // Scores
    private int scoreTeamA = 0;
    private int scoreTeamB = 0;

    // Game timing
    private int gameTime = 0;
    private int halfTime = 2400; // 40 minutes (in frames at 60fps = ~40 seconds real time)
    private int fullTime = 4800;
    private boolean firstHalf = true;

    // Animation
    private Timer gameTimer;
    private int animationFrame = 0;
    private int stateTimer = 0;

    // Input
    private boolean[] keys = new boolean[256];
    private Point mousePosition = new Point(0, 0);

    // Random generator
    private Random random = new Random();

    // Field dimensions
    private Rectangle fieldBounds;
    private Rectangle tryZoneA;
    private Rectangle tryZoneB;

    // Particle effects
    private List<Particle> particles = new ArrayList<>();

    // Messages
    private String displayMessage = "";
    private int messageTimer = 0;

    // Constructor
    public RugbyGameSimulation() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(new Color(34, 139, 34));
        setFocusable(true);
        addKeyListener(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseClick(e.getPoint());
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePosition = e.getPoint();
            }
        });

        initializeField();
        initializeTeams();
        initializeBall();

        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();
    }

    // Initialize field boundaries
    private void initializeField() {
        fieldBounds = new Rectangle(
                FIELD_MARGIN + TRY_ZONE_WIDTH,
                FIELD_MARGIN,
                WINDOW_WIDTH - 2 * FIELD_MARGIN - 2 * TRY_ZONE_WIDTH,
                WINDOW_HEIGHT - 2 * FIELD_MARGIN
        );

        tryZoneA = new Rectangle(
                FIELD_MARGIN,
                FIELD_MARGIN,
                TRY_ZONE_WIDTH,
                WINDOW_HEIGHT - 2 * FIELD_MARGIN
        );

        tryZoneB = new Rectangle(
                WINDOW_WIDTH - FIELD_MARGIN - TRY_ZONE_WIDTH,
                FIELD_MARGIN,
                TRY_ZONE_WIDTH,
                WINDOW_HEIGHT - 2 * FIELD_MARGIN
        );
    }

    // Initialize teams
    private void initializeTeams() {
        teamA = new ArrayList<>();
        teamB = new ArrayList<>();

        // Team A positions (left side - attacking right)
        String[] positionsA = {"Fullback", "Wing", "Centre", "Centre", "Wing", "Fly-half", "Scrum-half",
                "Number 8", "Flanker", "Flanker", "Lock", "Lock", "Prop", "Hooker", "Prop"};

        double[][] formationA = {
                {200, 350}, {150, 100}, {250, 200}, {250, 500}, {150, 600},
                {350, 350}, {400, 350}, {450, 350}, {480, 250}, {480, 450},
                {520, 300}, {520, 400}, {560, 280}, {560, 350}, {560, 420}
        };

        for (int i = 0; i < TEAM_SIZE; i++) {
            Player p = new Player(formationA[i][0], formationA[i][1], true, positionsA[i], i);
            teamA.add(p);
        }

        // Team B positions (right side - attacking left)
        String[] positionsB = {"Fullback", "Wing", "Centre", "Centre", "Wing", "Fly-half", "Scrum-half",
                "Number 8", "Flanker", "Flanker", "Lock", "Lock", "Prop", "Hooker", "Prop"};

        double[][] formationB = {
                {1000, 350}, {1050, 100}, {950, 200}, {950, 500}, {1050, 600},
                {850, 350}, {800, 350}, {750, 350}, {720, 250}, {720, 450},
                {680, 300}, {680, 400}, {640, 280}, {640, 350}, {640, 420}
        };

        for (int i = 0; i < TEAM_SIZE; i++) {
            Player p = new Player(formationB[i][0], formationB[i][1], false, positionsB[i], i);
            teamB.add(p);
        }

        selectedPlayer = teamA.get(5); // Start with fly-half selected
    }

    // Initialize ball
    private void initializeBall() {
        ball = new Ball(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2);
        ballCarrier = null;
    }

    // Handle mouse clicks
    private void handleMouseClick(Point p) {
        if (gameState == GameState.PLAYING && ballCarrier != null && ballCarrier.isTeamA) {
            // Pass or kick to clicked location
            if (keys[KeyEvent.VK_SHIFT]) {
                // Kick
                kickBall(p.x, p.y, 15);
                showMessage("KICK!");
            } else {
                // Find nearest teammate to pass to
                Player target = findNearestTeammate(ballCarrier, p.x, p.y);
                if (target != null) {
                    passBall(target);
                }
            }
        }
    }

    // Find nearest teammate to a point
    private Player findNearestTeammate(Player from, double x, double y) {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;

        List<Player> teammates = from.isTeamA ? teamA : teamB;
        for (Player p : teammates) {
            if (p != from) {
                double dist = Math.sqrt(Math.pow(p.x - x, 2) + Math.pow(p.y - y, 2));
                if (dist < minDist) {
                    minDist = dist;
                    nearest = p;
                }
            }
        }
        return nearest;
    }

    // Pass ball to target
    private void passBall(Player target) {
        if (ballCarrier != null) {
            // Check for forward pass (illegal in rugby)
            boolean forwardPass = (ballCarrier.isTeamA && target.x > ballCarrier.x) ||
                    (!ballCarrier.isTeamA && target.x < ballCarrier.x);

            ball.x = ballCarrier.x;
            ball.y = ballCarrier.y;
            ball.targetX = target.x;
            ball.targetY = target.y;
            ball.inFlight = true;
            ball.flightProgress = 0;
            ballCarrier = null;
            ballInAir = true;

            if (forwardPass && random.nextDouble() > 0.3) {
                showMessage("FORWARD PASS! Scrum awarded");
                stateTimer = 60;
                // Will trigger scrum after ball lands
            }
        }
    }

    // Kick ball
    private void kickBall(double targetX, double targetY, double power) {
        if (ballCarrier != null) {
            ball.x = ballCarrier.x;
            ball.y = ballCarrier.y;
            ball.targetX = targetX;
            ball.targetY = targetY;
            ball.inFlight = true;
            ball.flightProgress = 0;
            ball.kickPower = power;
            ball.height = 0;
            ballCarrier = null;
            ballInAir = true;
            createKickParticles(ball.x, ball.y);
        }
    }

    // Create particle effects
    private void createKickParticles(double x, double y) {
        for (int i = 0; i < 15; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 2 + random.nextDouble() * 4;
            particles.add(new Particle(x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    new Color(139, 69, 19)));
        }
    }

    // Create try scored particles
    private void createTryParticles(double x, double y, Color color) {
        for (int i = 0; i < 50; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 3 + random.nextDouble() * 6;
            particles.add(new Particle(x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    color));
        }
    }

    // Show message
    private void showMessage(String msg) {
        displayMessage = msg;
        messageTimer = 120;
    }

    // Main game update
    @Override
    public void actionPerformed(ActionEvent e) {
        animationFrame++;

        if (messageTimer > 0) messageTimer--;

        updateParticles();

        switch (gameState) {
            case KICKOFF:
                updateKickoff();
                break;
            case PLAYING:
                updatePlaying();
                break;
            case SCRUM:
                updateScrum();
                break;
            case LINEOUT:
                updateLineout();
                break;
            case TRY_SCORED:
                updateTryScored();
                break;
            case CONVERSION:
                updateConversion();
                break;
            case HALFTIME:
                updateHalftime();
                break;
            case GAMEOVER:
                break;
        }

        // Update game time
        if (gameState == GameState.PLAYING) {
            gameTime++;
            if (gameTime >= halfTime && firstHalf) {
                gameState = GameState.HALFTIME;
                showMessage("HALF TIME!");
                stateTimer = 180;
            } else if (gameTime >= fullTime) {
                gameState = GameState.GAMEOVER;
                showMessage("FULL TIME! " + (scoreTeamA > scoreTeamB ? "TEAM A WINS!" :
                        scoreTeamB > scoreTeamA ? "TEAM B WINS!" : "DRAW!"));
            }
        }

        repaint();
    }

    // Update particles
    private void updateParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.life <= 0) {
                particles.remove(i);
            }
        }
    }

    // Update kickoff state
    private void updateKickoff() {
        stateTimer++;
        if (stateTimer > 120) {
            // Position ball at center for kickoff
            ball.x = WINDOW_WIDTH / 2;
            ball.y = WINDOW_HEIGHT / 2;

            // Team A kicks off
            Player kicker = teamA.get(5); // Fly-half kicks
            ballCarrier = kicker;
            kicker.x = ball.x - 30;
            kicker.y = ball.y;

            gameState = GameState.PLAYING;
            stateTimer = 0;
            showMessage("KICK OFF!");
        }
    }

    // Update playing state
    private void updatePlaying() {
        // Handle player input
        handlePlayerInput();

        // Update ball
        updateBall();

        // Update all players
        updatePlayers();

        // Check for tackles
        checkTackles();

        // Check for tries
        checkTries();

        // Check ball out of bounds
        checkBounds();
    }

    // Handle player input
    private void handlePlayerInput() {
        if (selectedPlayer != null && selectedPlayer.isTeamA) {
            double speed = 4;
            double targetX = selectedPlayer.x;
            double targetY = selectedPlayer.y;

            if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP]) targetY -= speed;
            if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN]) targetY += speed;
            if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT]) targetX -= speed;
            if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT]) targetX += speed;

            // Keep within bounds
            targetX = Math.max(FIELD_MARGIN, Math.min(WINDOW_WIDTH - FIELD_MARGIN, targetX));
            targetY = Math.max(FIELD_MARGIN, Math.min(WINDOW_HEIGHT - FIELD_MARGIN, targetY));

            selectedPlayer.targetX = targetX;
            selectedPlayer.targetY = targetY;
            selectedPlayer.moveToTarget(0.3);

            // If this player has the ball, move ball with them
            if (ballCarrier == selectedPlayer) {
                ball.x = selectedPlayer.x;
                ball.y = selectedPlayer.y;
            }
        }
    }

    // Update ball physics
    private void updateBall() {
        if (ball.inFlight) {
            ball.flightProgress += 0.05;

            if (ball.flightProgress >= 1.0) {
                ball.inFlight = false;
                ball.x = ball.targetX;
                ball.y = ball.targetY;
                ball.height = 0;
                ballInAir = false;

                // Check if any player can catch
                Player catcher = findNearestPlayer(ball.x, ball.y, 40);
                if (catcher != null) {
                    ballCarrier = catcher;
                    showMessage(catcher.position + " catches!");
                }
            } else {
                // Parabolic flight path
                double startX = ball.x;
                double startY = ball.y;
                ball.x = startX + (ball.targetX - startX) * ball.flightProgress;
                ball.y = startY + (ball.targetY - startY) * ball.flightProgress;

                // Height follows parabola
                ball.height = Math.sin(ball.flightProgress * Math.PI) * ball.kickPower * 3;
            }
        } else if (ballCarrier != null) {
            ball.x = ballCarrier.x;
            ball.y = ballCarrier.y;
        }
    }

    // Find nearest player to a point
    private Player findNearestPlayer(double x, double y, double maxDist) {
        Player nearest = null;
        double minDist = maxDist;

        for (Player p : teamA) {
            double dist = Math.sqrt(Math.pow(p.x - x, 2) + Math.pow(p.y - y, 2));
            if (dist < minDist) {
                minDist = dist;
                nearest = p;
            }
        }

        for (Player p : teamB) {
            double dist = Math.sqrt(Math.pow(p.x - x, 2) + Math.pow(p.y - y, 2));
            if (dist < minDist) {
                minDist = dist;
                nearest = p;
            }
        }

        return nearest;
    }

    // Update all players with AI
    private void updatePlayers() {
        // Update Team A (player controlled team)
        for (Player p : teamA) {
            if (p != selectedPlayer) {
                updatePlayerAI(p, true);
            }
            p.update();
        }

        // Update Team B (AI controlled)
        for (Player p : teamB) {
            updatePlayerAI(p, false);
            p.update();
        }
    }

    // Player AI behavior
    private void updatePlayerAI(Player player, boolean isAttacking) {
        if (player.tackled) return;

        double speed = 2.5;

        if (ballCarrier != null) {
            if (ballCarrier.isTeamA == player.isTeamA) {
                // Same team as ball carrier - support play
                if (player != ballCarrier) {
                    // Move to support position
                    double offsetX = player.isTeamA ? -50 - random.nextInt(50) : 50 + random.nextInt(50);
                    double offsetY = (player.y < ballCarrier.y) ? -40 : 40;

                    player.targetX = ballCarrier.x + offsetX;
                    player.targetY = ballCarrier.y + offsetY + (player.number % 5) * 30 - 60;
                }
            } else {
                // Defending - chase ball carrier
                double chaseX = ballCarrier.x;
                double chaseY = ballCarrier.y;

                // Add some prediction
                if (player.number < 8) { // Forwards tackle more directly
                    player.targetX = chaseX;
                    player.targetY = chaseY;
                } else { // Backs try to cut off
                    double predictionFactor = 30;
                    player.targetX = chaseX + (ballCarrier.isTeamA ? predictionFactor : -predictionFactor);
                    player.targetY = chaseY;
                }
            }
        } else if (ball.inFlight) {
            // Chase the ball
            player.targetX = ball.targetX + (random.nextDouble() - 0.5) * 40;
            player.targetY = ball.targetY + (random.nextDouble() - 0.5) * 40;
        } else {
            // Return to formation
            player.targetX = player.homeX + (random.nextDouble() - 0.5) * 20;
            player.targetY = player.homeY + (random.nextDouble() - 0.5) * 20;
        }

        // Keep within bounds
        player.targetX = Math.max(FIELD_MARGIN, Math.min(WINDOW_WIDTH - FIELD_MARGIN, player.targetX));
        player.targetY = Math.max(FIELD_MARGIN, Math.min(WINDOW_HEIGHT - FIELD_MARGIN, player.targetY));

        player.moveToTarget(0.1);
    }

    // Check for tackles
    private void checkTackles() {
        if (ballCarrier == null || ballCarrier.tackled) return;

        List<Player> defenders = ballCarrier.isTeamA ? teamB : teamA;

        for (Player defender : defenders) {
            if (!defender.tackled) {
                double dist = Math.sqrt(Math.pow(defender.x - ballCarrier.x, 2) +
                        Math.pow(defender.y - ballCarrier.y, 2));

                if (dist < PLAYER_RADIUS * 2) {
                    // Tackle attempt
                    if (random.nextDouble() < 0.7) { // 70% tackle success
                        performTackle(defender, ballCarrier);
                        break;
                    }
                }
            }
        }
    }

    // Perform tackle
    private void performTackle(Player tackler, Player ballCarrier) {
        ballCarrier.tackled = true;
        ballCarrier.tackleTimer = 60;
        tackler.tackleTimer = 30;

        showMessage("TACKLE by " + tackler.position + "!");

        // Create tackle particles
        for (int i = 0; i < 20; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 2 + random.nextDouble() * 3;
            particles.add(new Particle(ballCarrier.x, ballCarrier.y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    Color.YELLOW));
        }

        // Ball becomes loose after short delay
        stateTimer = 0;
    }

    // Check for tries
    private void checkTries() {
        if (ballCarrier == null) return;

        // Team A scores in try zone B (right side)
        if (ballCarrier.isTeamA && tryZoneB.contains(ballCarrier.x, ballCarrier.y)) {
            scoreTry(true);
        }
        // Team B scores in try zone A (left side)
        else if (!ballCarrier.isTeamA && tryZoneA.contains(ballCarrier.x, ballCarrier.y)) {
            scoreTry(false);
        }
    }

    // Score a try
    private void scoreTry(boolean teamAScored) {
        gameState = GameState.TRY_SCORED;
        stateTimer = 0;

        if (teamAScored) {
            scoreTeamA += 5;
            showMessage("TRY! Team A scores! +5 points");
            createTryParticles(ballCarrier.x, ballCarrier.y, Color.RED);
        } else {
            scoreTeamB += 5;
            showMessage("TRY! Team B scores! +5 points");
            createTryParticles(ballCarrier.x, ballCarrier.y, Color.BLUE);
        }
    }

    // Check if ball goes out of bounds
    private void checkBounds() {
        if (ball.y < FIELD_MARGIN || ball.y > WINDOW_HEIGHT - FIELD_MARGIN) {
            // Ball out - lineout
            gameState = GameState.LINEOUT;
            stateTimer = 0;
            showMessage("LINEOUT!");

            ball.y = Math.max(FIELD_MARGIN + 20, Math.min(WINDOW_HEIGHT - FIELD_MARGIN - 20, ball.y));
            ballCarrier = null;
        }
    }

    // Update try scored celebration
    private void updateTryScored() {
        stateTimer++;
        if (stateTimer > 180) {
            gameState = GameState.CONVERSION;
            stateTimer = 0;
            showMessage("CONVERSION ATTEMPT!");
        }
    }

    // Update conversion attempt
    private void updateConversion() {
        stateTimer++;

        if (stateTimer == 60) {
            // Simulate conversion attempt
            boolean successful = random.nextDouble() < 0.7; // 70% success rate

            if (successful) {
                if (scoreTeamA > scoreTeamB || (scoreTeamA == scoreTeamB && random.nextBoolean())) {
                    scoreTeamA += 2;
                } else {
                    scoreTeamB += 2;
                }
                showMessage("CONVERSION SUCCESSFUL! +2 points");
            } else {
                showMessage("Conversion missed!");
            }
        }

        if (stateTimer > 180) {
            resetForKickoff();
        }
    }

    // Update scrum
    private void updateScrum() {
        stateTimer++;

        if (stateTimer > 120) {
            // Scrum winner (50/50)
            boolean teamAWins = random.nextBoolean();

            List<Player> winners = teamAWins ? teamA : teamB;
            ballCarrier = winners.get(6); // Scrum-half gets ball
            ball.x = ballCarrier.x;
            ball.y = ballCarrier.y;

            showMessage((teamAWins ? "Team A" : "Team B") + " wins the scrum!");

            gameState = GameState.PLAYING;
            stateTimer = 0;
        }
    }

    // Update lineout
    private void updateLineout() {
        stateTimer++;

        if (stateTimer > 90) {
            // Lineout winner
            boolean teamAWins = random.nextDouble() < 0.6;

            List<Player> winners = teamAWins ? teamA : teamB;
            ballCarrier = winners.get(3); // Lock catches
            ball.x = ballCarrier.x;
            ball.y = ballCarrier.y;

            showMessage((teamAWins ? "Team A" : "Team B") + " wins the lineout!");

            gameState = GameState.PLAYING;
            stateTimer = 0;
        }
    }

    // Update halftime
    private void updateHalftime() {
        stateTimer++;

        if (stateTimer > 300) {
            firstHalf = false;
            resetForKickoff();
            showMessage("SECOND HALF!");
        }
    }

    // Reset positions for kickoff
    private void resetForKickoff() {
        // Reset all players to home positions
        for (Player p : teamA) {
            p.x = p.homeX;
            p.y = p.homeY;
            p.tackled = false;
            p.tackleTimer = 0;
        }

        for (Player p : teamB) {
            p.x = p.homeX;
            p.y = p.homeY;
            p.tackled = false;
            p.tackleTimer = 0;
        }

        ball.x = WINDOW_WIDTH / 2;
        ball.y = WINDOW_HEIGHT / 2;
        ballCarrier = null;
        ball.inFlight = false;

        gameState = GameState.KICKOFF;
        stateTimer = 0;
    }

    // Paint the game
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawField(g2d);
        drawParticles(g2d);
        drawPlayers(g2d);
        drawBall(g2d);
        drawUI(g2d);
        drawMessage(g2d);
    }

    // Draw the rugby field
    private void drawField(Graphics2D g2d) {
        // Main field
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Field outline
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.draw(fieldBounds);

        // Try zones
        g2d.setColor(new Color(0, 100, 0));
        g2d.fill(tryZoneA);
        g2d.fill(tryZoneB);

        g2d.setColor(Color.WHITE);
        g2d.draw(tryZoneA);
        g2d.draw(tryZoneB);

        // Field lines
        g2d.setStroke(new BasicStroke(2));

        // Center line
        int centerX = WINDOW_WIDTH / 2;
        g2d.drawLine(centerX, FIELD_MARGIN, centerX, WINDOW_HEIGHT - FIELD_MARGIN);

        // Center circle
        g2d.drawOval(centerX - 50, WINDOW_HEIGHT / 2 - 50, 100, 100);

        // 22-meter lines
        int line22Left = FIELD_MARGIN + TRY_ZONE_WIDTH + 100;
        int line22Right = WINDOW_WIDTH - FIELD_MARGIN - TRY_ZONE_WIDTH - 100;
        g2d.drawLine(line22Left, FIELD_MARGIN, line22Left, WINDOW_HEIGHT - FIELD_MARGIN);
        g2d.drawLine(line22Right, FIELD_MARGIN, line22Right, WINDOW_HEIGHT - FIELD_MARGIN);

        // 10-meter lines (dashed)
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{10}, 0));
        g2d.drawLine(centerX - 80, FIELD_MARGIN, centerX - 80, WINDOW_HEIGHT - FIELD_MARGIN);
        g2d.drawLine(centerX + 80, FIELD_MARGIN, centerX + 80, WINDOW_HEIGHT - FIELD_MARGIN);

        // Goal posts
        g2d.setStroke(new BasicStroke(4));
        g2d.setColor(Color.YELLOW);

        // Left goal post
        int postY1 = WINDOW_HEIGHT / 2 - GOAL_POST_WIDTH / 2;
        int postY2 = WINDOW_HEIGHT / 2 + GOAL_POST_WIDTH / 2;
        g2d.drawLine(FIELD_MARGIN + TRY_ZONE_WIDTH / 2, postY1, FIELD_MARGIN + TRY_ZONE_WIDTH / 2, postY2);
        g2d.drawLine(FIELD_MARGIN + TRY_ZONE_WIDTH / 2, postY1, FIELD_MARGIN + TRY_ZONE_WIDTH / 2 - 20, postY1 - GOAL_POST_HEIGHT);
        g2d.drawLine(FIELD_MARGIN + TRY_ZONE_WIDTH / 2, postY2, FIELD_MARGIN + TRY_ZONE_WIDTH / 2 - 20, postY2 + GOAL_POST_HEIGHT);

        // Right goal post
        int rightPostX = WINDOW_WIDTH - FIELD_MARGIN - TRY_ZONE_WIDTH / 2;
        g2d.drawLine(rightPostX, postY1, rightPostX, postY2);
        g2d.drawLine(rightPostX, postY1, rightPostX + 20, postY1 - GOAL_POST_HEIGHT);
        g2d.drawLine(rightPostX, postY2, rightPostX + 20, postY2 + GOAL_POST_HEIGHT);

        // Field markings - 5 meter marks
        g2d.setStroke(new BasicStroke(1));
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < 12; i++) {
            int y = FIELD_MARGIN + 50 + i * 50;
            g2d.drawLine(FIELD_MARGIN + TRY_ZONE_WIDTH, y, FIELD_MARGIN + TRY_ZONE_WIDTH + 10, y);
            g2d.drawLine(WINDOW_WIDTH - FIELD_MARGIN - TRY_ZONE_WIDTH - 10, y, WINDOW_WIDTH - FIELD_MARGIN - TRY_ZONE_WIDTH, y);
        }
    }

    // Draw particles
    private void drawParticles(Graphics2D g2d) {
        for (Particle p : particles) {
            g2d.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(),
                    (int)(255 * p.life / 60.0)));
            g2d.fillOval((int)(p.x - 3), (int)(p.y - 3), 6, 6);
        }
    }

    // Draw all players
    private void drawPlayers(Graphics2D g2d) {
        // Draw Team A (red)
        for (Player p : teamA) {
            drawPlayer(g2d, p, new Color(200, 50, 50), new Color(255, 100, 100));
        }

        // Draw Team B (blue)
        for (Player p : teamB) {
            drawPlayer(g2d, p, new Color(50, 50, 200), new Color(100, 100, 255));
        }
    }

    // Draw individual player
    private void drawPlayer(Graphics2D g2d, Player player, Color mainColor, Color highlightColor) {
        int x = (int) player.x;
        int y = (int) player.y;

        // Shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(x - PLAYER_RADIUS + 2, y - PLAYER_RADIUS + 4, PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);

        // Player body with animation
        double bounce = Math.sin(animationFrame * 0.2 + player.number) * 2;
        int drawY = (int)(y + bounce);

        // Tackled players lie flat
        if (player.tackled) {
            g2d.setColor(mainColor.darker());
            g2d.fillOval(x - PLAYER_RADIUS - 5, drawY - 5, PLAYER_RADIUS * 2 + 10, 10);
        } else {
            // Main body
            g2d.setColor(mainColor);
            g2d.fillOval(x - PLAYER_RADIUS, drawY - PLAYER_RADIUS, PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);

            // Highlight
            g2d.setColor(highlightColor);
            g2d.fillOval(x - PLAYER_RADIUS + 3, drawY - PLAYER_RADIUS + 2, 8, 6);

            // Jersey number
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            String num = String.valueOf(player.number + 1);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(num, x - fm.stringWidth(num) / 2, drawY + 4);
        }

        // Selected player indicator
        if (player == selectedPlayer) {
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x - PLAYER_RADIUS - 4, drawY - PLAYER_RADIUS - 4,
                    PLAYER_RADIUS * 2 + 8, PLAYER_RADIUS * 2 + 8);

            // Direction arrow
            if (ballCarrier == player) {
                double angle = Math.atan2(mousePosition.y - y, mousePosition.x - x);
                int arrowLength = 25;
                int arrowX = x + (int)(Math.cos(angle) * arrowLength);
                int arrowY = drawY + (int)(Math.sin(angle) * arrowLength);
                g2d.drawLine(x, drawY, arrowX, arrowY);
            }
        }

        // Ball carrier indicator
        if (player == ballCarrier) {
            g2d.setColor(new Color(255, 215, 0));
            g2d.setStroke(new BasicStroke(3));
            double pulse = 1 + Math.sin(animationFrame * 0.3) * 0.2;
            int pulseRadius = (int)(PLAYER_RADIUS * pulse) + 5;
            g2d.drawOval(x - pulseRadius, drawY - pulseRadius, pulseRadius * 2, pulseRadius * 2);
        }
    }

    // Draw the ball
    private void drawBall(Graphics2D g2d) {
        int x = (int) ball.x;
        int y = (int) ball.y;
        int height = (int) ball.height;

        // Ball shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(x - BALL_RADIUS, y - BALL_RADIUS / 2 + 4, BALL_RADIUS * 2, BALL_RADIUS);

        // Ball (rugby ball shape - oval)
        int drawY = y - height;

        // Rotation animation when in flight
        double rotation = ball.inFlight ? animationFrame * 0.3 : 0;

        AffineTransform old = g2d.getTransform();
        g2d.rotate(rotation, x, drawY);

        // Ball body (brown leather color)
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillOval(x - BALL_RADIUS - 4, drawY - BALL_RADIUS, BALL_RADIUS * 2 + 8, BALL_RADIUS * 2);

        // Ball seam
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(x, drawY - BALL_RADIUS + 2, x, drawY + BALL_RADIUS - 2);

        // Ball laces
        for (int i = -2; i <= 2; i++) {
            g2d.drawLine(x - 3, drawY + i * 3, x + 3, drawY + i * 3);
        }

        g2d.setTransform(old);
    }

    // Draw UI elements
    private void drawUI(Graphics2D g2d) {
        // Scoreboard background
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(WINDOW_WIDTH / 2 - 150, 5, 300, 40, 10, 10);

        // Team names and scores
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(new Color(255, 100, 100));
        g2d.drawString("TEAM A", WINDOW_WIDTH / 2 - 140, 30);

        g2d.setColor(Color.WHITE);
        g2d.drawString(String.valueOf(scoreTeamA), WINDOW_WIDTH / 2 - 40, 30);
        g2d.drawString("-", WINDOW_WIDTH / 2 - 5, 30);
        g2d.drawString(String.valueOf(scoreTeamB), WINDOW_WIDTH / 2 + 20, 30);

        g2d.setColor(new Color(100, 100, 255));
        g2d.drawString("TEAM B", WINDOW_WIDTH / 2 + 60, 30);

        // Game time
        int minutes = gameTime / 60;
        int seconds = gameTime % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString(timeStr, WINDOW_WIDTH / 2 - 25, 55);

        // Half indicator
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(Color.WHITE);
        g2d.drawString(firstHalf ? "1ST HALF" : "2ND HALF", WINDOW_WIDTH / 2 - 25, 70);

        // Controls help
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String[] controls = {
                "WASD/Arrows: Move player",
                "Click: Pass to nearest teammate",
                "Shift+Click: Kick",
                "Space: Switch player",
                "R: Reset game"
        };

        for (int i = 0; i < controls.length; i++) {
            g2d.drawString(controls[i], 10, WINDOW_HEIGHT - 80 + i * 15);
        }

        // Game state indicator
        String stateStr = "";
        switch (gameState) {
            case KICKOFF: stateStr = "KICKOFF"; break;
            case PLAYING: stateStr = "PLAYING"; break;
            case SCRUM: stateStr = "SCRUM"; break;
            case LINEOUT: stateStr = "LINEOUT"; break;
            case TRY_SCORED: stateStr = "TRY!"; break;
            case CONVERSION: stateStr = "CONVERSION"; break;
            case HALFTIME: stateStr = "HALF TIME"; break;
            case GAMEOVER: stateStr = "FULL TIME"; break;
        }

        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString(stateStr, WINDOW_WIDTH - 100, 25);
    }

    // Draw message
    private void drawMessage(Graphics2D g2d) {
        if (messageTimer > 0 && !displayMessage.isEmpty()) {
            float alpha = Math.min(1.0f, messageTimer / 30.0f);

            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(displayMessage);
            int x = WINDOW_WIDTH / 2 - textWidth / 2;
            int y = WINDOW_HEIGHT / 2;

            // Background
            g2d.setColor(new Color(0, 0, 0, (int)(180 * alpha)));
            g2d.fillRoundRect(x - 20, y - 35, textWidth + 40, 50, 15, 15);

            // Text with outline
            g2d.setColor(new Color(0, 0, 0, (int)(255 * alpha)));
            g2d.drawString(displayMessage, x + 2, y + 2);

            g2d.setColor(new Color(255, 255, 0, (int)(255 * alpha)));
            g2d.drawString(displayMessage, x, y);
        }
    }

    // Key events
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code < keys.length) keys[code] = true;

        // Space to switch player
        if (code == KeyEvent.VK_SPACE) {
            int currentIndex = teamA.indexOf(selectedPlayer);
            selectedPlayer = teamA.get((currentIndex + 1) % teamA.size());
        }

        // R to reset
        if (code == KeyEvent.VK_R) {
            scoreTeamA = 0;
            scoreTeamB = 0;
            gameTime = 0;
            firstHalf = true;
            resetForKickoff();
        }

        // P to pass (keyboard alternative)
        if (code == KeyEvent.VK_P && ballCarrier != null && ballCarrier.isTeamA) {
            Player nearest = findNearestTeammate(ballCarrier, mousePosition.x, mousePosition.y);
            if (nearest != null) {
                passBall(nearest);
            }
        }

        // K to kick
        if (code == KeyEvent.VK_K && ballCarrier != null && ballCarrier.isTeamA) {
            kickBall(mousePosition.x, mousePosition.y, 12);
            showMessage("KICK!");
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code < keys.length) keys[code] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Player class
    private class Player {
        double x, y;
        double homeX, homeY;
        double targetX, targetY;
        double vx, vy;
        boolean isTeamA;
        String position;
        int number;
        boolean tackled = false;
        int tackleTimer = 0;

        Player(double x, double y, boolean isTeamA, String position, int number) {
            this.x = x;
            this.y = y;
            this.homeX = x;
            this.homeY = y;
            this.targetX = x;
            this.targetY = y;
            this.isTeamA = isTeamA;
            this.position = position;
            this.number = number;
        }

        void moveToTarget(double speed) {
            double dx = targetX - x;
            double dy = targetY - y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > 1) {
                vx = (dx / dist) * speed * 10;
                vy = (dy / dist) * speed * 10;
            } else {
                vx = 0;
                vy = 0;
            }
        }

        void update() {
            if (tackled) {
                tackleTimer--;
                if (tackleTimer <= 0) {
                    tackled = false;
                    if (ballCarrier == this) {
                        // Release ball after tackle
                        ball.x = x;
                        ball.y = y;
                        ballCarrier = null;

                        // Ruck situation - closest player picks up
                        Player closest = findNearestPlayer(x, y, 50);
                        if (closest != null) {
                            ballCarrier = closest;
                        }
                    }
                }
                return;
            }

            x += vx * 0.1;
            y += vy * 0.1;

            // Friction
            vx *= 0.95;
            vy *= 0.95;

            // Keep in bounds
            x = Math.max(FIELD_MARGIN, Math.min(WINDOW_WIDTH - FIELD_MARGIN, x));
            y = Math.max(FIELD_MARGIN, Math.min(WINDOW_HEIGHT - FIELD_MARGIN, y));
        }
    }

    // Ball class
    private class Ball {
        double x, y;
        double targetX, targetY;
        double height = 0;
        double kickPower = 10;
        boolean inFlight = false;
        double flightProgress = 0;

        Ball(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    // Particle class for effects
    private class Particle {
        double x, y;
        double vx, vy;
        Color color;
        int life = 60;

        Particle(double x, double y, double vx, double vy, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
        }

        void update() {
            x += vx;
            y += vy;
            vx *= 0.95;
            vy *= 0.95;
            life--;
        }
    }

    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Rugby Game Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            RugbyGameSimulation game = new RugbyGameSimulation();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}