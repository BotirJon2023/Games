import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.Timer;

public class IceHockeySimulator extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 1400;
    private static final int HEIGHT = 900;
    private static final int GOAL_WIDTH = 220;
    private static final int GOAL_Y = HEIGHT/2 - GOAL_WIDTH/2;
    private static final int PUCK_SIZE = 16;
    private static final int PLAYER_SIZE = 38;
    private static final int MAX_SPEED = 9;
    private static final double FRICTION = 0.96;
    private static final double PUCK_FRICTION = 0.992;

    // Game objects
    private Puck puck;
    private ArrayList<HockeyPlayer> teamRed = new ArrayList<>();
    private ArrayList<HockeyPlayer> teamBlue = new ArrayList<>();
    private Goal goal1, goal2;
    private int score1 = 0, score2 = 0;
    private int period = 1;
    private int gameTime = 20 * 60; // 20 minutes in seconds
    private Timer gameLoop, gameClock;
    private boolean gameRunning = true;
    private boolean againstComputer = true;
    private boolean paused = false;
    private boolean goalScored = false;
    private int goalDelay = 0;
    private int currentPlayerIndex = 0; // For player switching in team mode

    // Input states
    private Set<Integer> pressedKeys = new HashSet<>();

    // AI variables
    private Random random = new Random();
    private int[] aiDecisionDelay = new int[3];

    // Camera/Viewport for following action
    private int cameraX = 0;
    private int cameraY = 0;

    public IceHockeySimulator(boolean vsComputer) {
        this.againstComputer = vsComputer;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(200, 220, 255));
        setFocusable(true);
        addKeyListener(this);

        // Initialize puck
        puck = new Puck(WIDTH/2, HEIGHT/2);

        // Create Red Team (Left side) - 3 players
        createTeamRed();

        // Create Blue Team (Right side)
        createTeamBlue();

        // Goals
        goal1 = new Goal(20, GOAL_Y, 12, GOAL_WIDTH);
        goal2 = new Goal(WIDTH - 32, GOAL_Y, 12, GOAL_WIDTH);

        gameLoop = new Timer(1000 / 60, this);
        gameLoop.start();

        // Game clock timer
        gameClock = new Timer(1000, e -> {
            if (gameRunning && !paused && !goalScored) {
                gameTime--;
                if (gameTime <= 0) {
                    nextPeriod();
                }
            }
        });
        gameClock.start();
    }

    private void createTeamRed() {
        // Forward
        teamRed.add(new HockeyPlayer(150, HEIGHT/2 - 100, "RED", "Forward", 1,
                KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,
                KeyEvent.VK_SPACE, KeyEvent.VK_SHIFT, true));

        // Center
        teamRed.add(new HockeyPlayer(150, HEIGHT/2, "RED", "Center", 2,
                KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,
                KeyEvent.VK_SPACE, KeyEvent.VK_SHIFT, true));

        // Defender
        teamRed.add(new HockeyPlayer(150, HEIGHT/2 + 100, "RED", "Defender", 3,
                KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,
                KeyEvent.VK_SPACE, KeyEvent.VK_SHIFT, true));

        // Set positions
        for (int i = 0; i < teamRed.size(); i++) {
            teamRed.get(i).formationPosition = i;
        }
    }

    private void createTeamBlue() {
        if (againstComputer) {
            // AI-controlled team
            teamBlue.add(new HockeyPlayer(WIDTH - 150, HEIGHT/2 - 100, "BLUE", "Forward", 4,
                    0, 0, 0, 0, 0, 0, false));
            teamBlue.add(new HockeyPlayer(WIDTH - 150, HEIGHT/2, "BLUE", "Center", 5,
                    0, 0, 0, 0, 0, 0, false));
            teamBlue.add(new HockeyPlayer(WIDTH - 150, HEIGHT/2 + 100, "BLUE", "Defender", 6,
                    0, 0, 0, 0, 0, 0, false));

            for (HockeyPlayer p : teamBlue) {
                p.isAI = true;
            }
        } else {
            // Human-controlled team with different keys
            teamBlue.add(new HockeyPlayer(WIDTH - 150, HEIGHT/2 - 100, "BLUE", "Forward", 4,
                    KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                    KeyEvent.VK_ENTER, KeyEvent.VK_NUMPAD0, false));
            teamBlue.add(new HockeyPlayer(WIDTH - 150, HEIGHT/2, "BLUE", "Center", 5,
                    KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                    KeyEvent.VK_ENTER, KeyEvent.VK_NUMPAD0, false));
            teamBlue.add(new HockeyPlayer(WIDTH - 150, HEIGHT/2 + 100, "BLUE", "Defender", 6,
                    KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                    KeyEvent.VK_ENTER, KeyEvent.VK_NUMPAD0, false));
        }

        // Set positions
        for (int i = 0; i < teamBlue.size(); i++) {
            teamBlue.get(i).formationPosition = i;
        }
    }

    private void nextPeriod() {
        if (period < 3) {
            period++;
            gameTime = 20 * 60;
            resetPosition();
            showMessage("PERIOD " + period + " STARTED!", 2000);
        } else {
            endGame();
        }
    }

    private void endGame() {
        gameRunning = false;
        gameLoop.stop();
        gameClock.stop();
        String winner = score1 > score2 ? "🔴 RED TEAM WINS!" :
                (score2 > score1 ? "🔵 BLUE TEAM WINS!" : "🤝 TIE GAME!");
        JOptionPane.showMessageDialog(this,
                "🏆 GAME OVER 🏆\n" + winner + "\nFinal Score: " + score1 + " - " + score2,
                "Game Over", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    private void showMessage(String msg, int duration) {
        final JLabel message = new JLabel(msg, SwingConstants.CENTER);
        message.setFont(new Font("Arial", Font.BOLD, 48));
        message.setForeground(Color.RED);
        message.setBounds(WIDTH/2 - 300, HEIGHT/2 - 50, 600, 100);
        add(message);
        Timer timer = new Timer(duration, e -> remove(message));
        timer.setRepeats(false);
        timer.start();
    }

    private void resetPosition() {
        puck.x = WIDTH/2;
        puck.y = HEIGHT/2;
        puck.vx = 0;
        puck.vy = 0;

        // Reset team positions
        resetTeamPositions(teamRed, 150);
        resetTeamPositions(teamBlue, WIDTH - 150);

        for (HockeyPlayer p : teamRed) p.hasPuck = false;
        for (HockeyPlayer p : teamBlue) p.hasPuck = false;

        goalScored = false;
    }

    private void resetTeamPositions(ArrayList<HockeyPlayer> team, int baseX) {
        team.get(0).x = baseX;
        team.get(0).y = HEIGHT/2 - 100;
        team.get(1).x = baseX;
        team.get(1).y = HEIGHT/2;
        team.get(2).x = baseX;
        team.get(2).y = HEIGHT/2 + 100;

        for (HockeyPlayer p : team) {
            p.vx = 0;
            p.vy = 0;
        }
    }

    private void checkGoal() {
        if (goalScored) return;

        boolean goal = false;
        String scorer = "";

        // Check if puck entered goal 1 (Blue scores)
        if (puck.x - PUCK_SIZE/2 < goal1.x + goal1.width &&
                puck.x + PUCK_SIZE/2 > goal1.x &&
                puck.y + PUCK_SIZE/2 > goal1.y && puck.y - PUCK_SIZE/2 < goal1.y + goal1.height) {
            score2++;
            scorer = "🔵 BLUE TEAM";
            goal = true;
        }
        // Check if puck entered goal 2 (Red scores)
        else if (puck.x + PUCK_SIZE/2 > goal2.x &&
                puck.x - PUCK_SIZE/2 < goal2.x + goal2.width &&
                puck.y + PUCK_SIZE/2 > goal2.y && puck.y - PUCK_SIZE/2 < goal2.y + goal2.height) {
            score1++;
            scorer = "🔴 RED TEAM";
            goal = true;
        }

        if (goal) {
            goalScored = true;
            showMessage("⚡ GOAL! " + scorer + " SCORES! ⚡\n" + score1 + " - " + score2, 2500);
            Timer resetTimer = new Timer(2500, e -> {
                resetPosition();
                goalScored = false;
            });
            resetTimer.setRepeats(false);
            resetTimer.start();
        }
    }

    private void updatePhysics() {
        if (!gameRunning || paused || goalScored) return;

        // Update all players
        for (HockeyPlayer player : teamRed) {
            player.update(pressedKeys);
        }

        for (HockeyPlayer player : teamBlue) {
            if (player.isAI) {
                updateAI(player);
            } else {
                player.update(pressedKeys);
            }
        }

        // Apply team formation and boundaries
        applyTeamFormation(teamRed, true);
        applyTeamFormation(teamBlue, false);

        // Update puck physics
        puck.vx *= PUCK_FRICTION;
        puck.vy *= PUCK_FRICTION;
        puck.x += puck.vx;
        puck.y += puck.vy;

        // Puck boundary collisions
        handlePuckBoundaries();

        // Handle collisions between players and puck
        handleAllCollisions();

        // Check for goals
        checkGoal();

        // Update camera to follow puck
        updateCamera();
    }

    private void applyTeamFormation(ArrayList<HockeyPlayer> team, boolean isRedTeam) {
        for (HockeyPlayer player : team) {
            // Keep players within their zones
            if (isRedTeam) {
                player.x = Math.max(PLAYER_SIZE/2 + 30, Math.min(WIDTH/2 - 80, player.x));
            } else {
                player.x = Math.max(WIDTH/2 + 80, Math.min(WIDTH - PLAYER_SIZE/2 - 30, player.x));
            }
            player.y = Math.max(PLAYER_SIZE/2 + 20, Math.min(HEIGHT - PLAYER_SIZE/2 - 20, player.y));

            // Apply friction
            player.vx *= FRICTION;
            player.vy *= FRICTION;
            player.x += player.vx;
            player.y += player.vy;

            // Update cooldowns
            if (player.shootCooldown > 0) player.shootCooldown--;
            if (player.pokeCooldown > 0) player.pokeCooldown--;
        }

        // Maintain formation spacing
        maintainFormation(team);
    }

    private void maintainFormation(ArrayList<HockeyPlayer> team) {
        // Keep players from overlapping
        for (int i = 0; i < team.size(); i++) {
            for (int j = i + 1; j < team.size(); j++) {
                HockeyPlayer p1 = team.get(i);
                HockeyPlayer p2 = team.get(j);
                double dx = p1.x - p2.x;
                double dy = p1.y - p2.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                double minDistance = PLAYER_SIZE;

                if (distance < minDistance) {
                    double angle = Math.atan2(dy, dx);
                    double overlap = minDistance - distance;
                    p1.x += Math.cos(angle) * overlap / 2;
                    p1.y += Math.sin(angle) * overlap / 2;
                    p2.x -= Math.cos(angle) * overlap / 2;
                    p2.y -= Math.sin(angle) * overlap / 2;
                }
            }
        }
    }

    private void handlePuckBoundaries() {
        // Board collisions with realistic bounce
        if (puck.y - PUCK_SIZE/2 < 10) {
            puck.vy = Math.abs(puck.vy) * 0.85;
            puck.y = PUCK_SIZE/2 + 10;
        }
        if (puck.y + PUCK_SIZE/2 > HEIGHT - 10) {
            puck.vy = -Math.abs(puck.vy) * 0.85;
            puck.y = HEIGHT - PUCK_SIZE/2 - 10;
        }

        if (puck.x - PUCK_SIZE/2 < 10) {
            puck.vx = Math.abs(puck.vx) * 0.85;
            puck.x = PUCK_SIZE/2 + 10;
        }
        if (puck.x + PUCK_SIZE/2 > WIDTH - 10) {
            puck.vx = -Math.abs(puck.vx) * 0.85;
            puck.x = WIDTH - PUCK_SIZE/2 - 10;
        }
    }

    private void handleAllCollisions() {
        // Check collisions with all players
        for (HockeyPlayer player : teamRed) {
            handleStickCollision(player, puck);
        }
        for (HockeyPlayer player : teamBlue) {
            handleStickCollision(player, puck);
        }
    }

    private void handleStickCollision(HockeyPlayer player, Puck puck) {
        // Calculate stick position (in front of player based on direction)
        double stickX = player.x + (player.facingRight ? 22 : -22);
        double stickY = player.y;
        double dx = puck.x - stickX;
        double dy = puck.y - stickY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double stickReach = 28;

        if (distance < stickReach) {
            double angle = Math.atan2(dy, dx);
            double power = Math.min(MAX_SPEED * 1.3,
                    Math.hypot(player.vx, player.vy) + 7);

            // Shooting mechanic
            if (player.isShooting) {
                power = MAX_SPEED * 2.2;
                player.isShooting = false;
                player.shootCooldown = 25;
                angle += (random.nextDouble() - 0.5) * 0.2;
            }

            // Poke check
            if (player.isPokeChecking) {
                power = MAX_SPEED * 1.6;
                player.isPokeChecking = false;
                player.pokeCooldown = 20;
            }

            puck.vx = Math.cos(angle) * power + player.vx * 0.6;
            puck.vy = Math.sin(angle) * power + player.vy * 0.6;

            player.hasPuck = true;

            // Separate puck from stick
            double overlap = stickReach - distance;
            puck.x += Math.cos(angle) * overlap;
            puck.y += Math.sin(angle) * overlap;
        } else {
            player.hasPuck = false;
        }
    }

    private void updateAI(HockeyPlayer ai) {
        // Find closest teammate to puck
        double closestDistance = Double.MAX_VALUE;
        HockeyPlayer closestPlayer = null;

        for (HockeyPlayer player : teamBlue) {
            double dist = Math.hypot(puck.x - player.x, puck.y - player.y);
            if (dist < closestDistance) {
                closestDistance = dist;
                closestPlayer = player;
            }
        }

        // AI behavior based on role
        double dxToPuck = puck.x - ai.x;
        double dyToPuck = puck.y - ai.y;
        double distanceToPuck = Math.hypot(dxToPuck, dyToPuck);

        // Role-based positioning
        double targetX, targetY;

        if (ai.playerName.equals("Forward")) {
            // Forward - aggressive, chase puck
            targetX = puck.x;
            targetY = puck.y;
            if (distanceToPuck < 100) {
                targetX += puck.vx * 10;
                targetY += puck.vy * 10;
            }
        } else if (ai.playerName.equals("Center")) {
            // Center - playmaker, stay in middle
            targetX = (puck.x + WIDTH/2) / 2;
            targetY = puck.y;
        } else {
            // Defender - stay back, protect goal
            targetX = Math.max(WIDTH - 250, puck.x * 0.4 + (WIDTH - 100) * 0.6);
            targetY = Math.min(HEIGHT - 80, Math.max(80, puck.y));
        }

        // Move towards target
        double dx = targetX - ai.x;
        double dy = targetY - ai.y;

        ai.vx += dx * 0.08;
        ai.vy += dy * 0.08;

        // Limit speed
        double speed = Math.hypot(ai.vx, ai.vy);
        if (speed > MAX_SPEED) {
            ai.vx = ai.vx / speed * MAX_SPEED;
            ai.vy = ai.vy / speed * MAX_SPEED;
        }

        // AI decision making
        if (distanceToPuck < 45 && !ai.hasPuck) {
            if (random.nextInt(100) < 15 && ai.pokeCooldown == 0) {
                ai.isPokeChecking = true;
            }
        }

        if (ai.hasPuck && distanceToPuck < 35) {
            // AI aims for goal
            double angleToGoal = Math.atan2(goal1.y + goal1.height/2 - ai.y,
                    goal1.x + goal1.width/2 - ai.x);
            if (random.nextInt(100) < 20 && ai.shootCooldown == 0) {
                ai.isShooting = true;
            }
        }

        // Update facing direction
        ai.facingRight = ai.x < WIDTH/2;
    }

    private void updateCamera() {
        // Smooth camera follow
        cameraX = (int)(puck.x - WIDTH/2);
        cameraY = (int)(puck.y - HEIGHT/2);

        // Clamp camera to world bounds
        cameraX = Math.max(0, Math.min(cameraX, WIDTH - WIDTH));
        cameraY = Math.max(0, Math.min(cameraY, HEIGHT - HEIGHT));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw ice rink
        drawIceRink(g2d);

        // Draw goals
        drawGoals(g2d);

        // Draw all players
        for (HockeyPlayer player : teamRed) {
            player.draw(g2d);
        }
        for (HockeyPlayer player : teamBlue) {
            player.draw(g2d);
        }

        // Draw puck
        drawPuck(g2d);

        // Draw scoreboard and UI
        drawScoreboard(g2d);
        drawGameInfo(g2d);
        drawPlayerIndicators(g2d);

        if (paused) {
            drawPauseScreen(g2d);
        }

        // Draw controls
        drawControls(g2d);
    }

    private void drawIceRink(Graphics2D g) {
        // Ice surface
        GradientPaint iceGradient = new GradientPaint(0, 0, new Color(220, 235, 255),
                WIDTH, HEIGHT, new Color(200, 220, 250));
        g.setPaint(iceGradient);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Rink borders
        g.setColor(new Color(139, 69, 19));
        g.setStroke(new BasicStroke(10));
        g.drawRect(15, 15, WIDTH - 30, HEIGHT - 30);

        // Center line
        g.setColor(Color.BLUE);
        g.setStroke(new BasicStroke(4));
        g.drawLine(WIDTH/2, 15, WIDTH/2, HEIGHT - 15);

        // Center circle
        g.drawOval(WIDTH/2 - 75, HEIGHT/2 - 75, 150, 150);

        // Faceoff circles
        g.drawOval(220, HEIGHT/2 - 70, 140, 140);
        g.drawOval(WIDTH - 360, HEIGHT/2 - 70, 140, 140);

        // Faceoff dots
        g.fillOval(WIDTH/2 - 6, HEIGHT/2 - 6, 12, 12);
        g.fillOval(285, HEIGHT/2 - 6, 12, 12);
        g.fillOval(WIDTH - 297, HEIGHT/2 - 6, 12, 12);

        // Goal creases
        g.setColor(new Color(200, 200, 255, 120));
        g.fillArc(goal1.x - 25, goal1.y - 25, 70, goal1.height + 50, -90, 180);
        g.fillArc(goal2.x - 45, goal2.y - 25, 70, goal1.height + 50, 90, 180);

        // Team benches
        g.setColor(new Color(100, 100, 100, 100));
        g.fillRect(50, HEIGHT - 80, 200, 60);
        g.fillRect(WIDTH - 250, HEIGHT - 80, 200, 60);
        g.fillRect(50, 20, 200, 60);
        g.fillRect(WIDTH - 250, 20, 200, 60);
    }

    private void drawGoals(Graphics2D g) {
        // Goal 1 (left)
        g.setColor(new Color(160, 160, 160));
        g.fillRect(goal1.x, goal1.y, goal1.width, goal1.height);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(4));
        g.drawRect(goal1.x, goal1.y, goal1.width, goal1.height);
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(3));
        g.drawLine(goal1.x + goal1.width, goal1.y, goal1.x + goal1.width, goal1.y + goal1.height);

        // Goal netting
        g.setColor(new Color(200, 200, 200, 100));
        for (int i = 0; i < 10; i++) {
            g.drawLine(goal1.x + goal1.width, goal1.y + i * (goal1.height/10),
                    goal1.x + goal1.width + 20, goal1.y + i * (goal1.height/10));
        }

        // Goal 2 (right)
        g.fillRect(goal2.x, goal2.y, goal2.width, goal2.height);
        g.setColor(Color.BLACK);
        g.drawRect(goal2.x, goal2.y, goal2.width, goal2.height);
        g.setColor(Color.RED);
        g.drawLine(goal2.x, goal2.y, goal2.x, goal2.y + goal2.height);

        for (int i = 0; i < 10; i++) {
            g.drawLine(goal2.x, goal2.y + i * (goal2.height/10),
                    goal2.x - 20, goal2.y + i * (goal2.height/10));
        }
    }

    private void drawPuck(Graphics2D g) {
        // Puck shadow
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval((int)(puck.x - PUCK_SIZE/2 + 4), (int)(puck.y - PUCK_SIZE/2 + 4),
                PUCK_SIZE, PUCK_SIZE);

        // Puck body
        RadialGradientPaint puckGrad = new RadialGradientPaint(
                (float)puck.x, (float)puck.y, PUCK_SIZE/2,
                new float[]{0f, 0.7f, 1f},
                new Color[]{Color.GRAY, Color.DARK_GRAY, Color.BLACK}
        );
        g.setPaint(puckGrad);
        g.fillOval((int)(puck.x - PUCK_SIZE/2), (int)(puck.y - PUCK_SIZE/2),
                PUCK_SIZE, PUCK_SIZE);

        // Puck highlight
        g.setColor(new Color(255, 255, 255, 80));
        g.fillOval((int)(puck.x - PUCK_SIZE/3), (int)(puck.y - PUCK_SIZE/3),
                (int) (PUCK_SIZE/1.5), (int) (PUCK_SIZE/1.5));
    }

    private void drawScoreboard(Graphics2D g) {
        // Scoreboard background with gradient
        GradientPaint scoreGrad = new GradientPaint(WIDTH/2 - 180, 15, new Color(0, 0, 0, 200),
                WIDTH/2 + 180, 95, new Color(50, 50, 50, 200));
        g.setPaint(scoreGrad);
        g.fillRoundRect(WIDTH/2 - 180, 15, 360, 100, 20, 20);

        // Team logos and scores
        g.setFont(new Font("Arial", Font.BOLD, 52));

        // Red team score
        g.setColor(Color.RED);
        g.drawString(String.valueOf(score1), WIDTH/2 - 110, 90);

        // VS separator
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString("VS", WIDTH/2 - 25, 80);

        // Blue team score
        g.setColor(Color.BLUE);
        g.setFont(new Font("Arial", Font.BOLD, 52));
        g.drawString(String.valueOf(score2), WIDTH/2 + 70, 90);

        // Team names
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(Color.RED);
        g.drawString("RED TEAM", WIDTH/2 - 140, 45);
        g.setColor(Color.BLUE);
        g.drawString("BLUE TEAM", WIDTH/2 + 80, 45);
    }

    private void drawGameInfo(Graphics2D g) {
        // Period and time
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(20, 20, 280, 50, 15, 15);
        g.setColor(Color.WHITE);
        int minutes = gameTime / 60;
        int seconds = gameTime % 60;
        String timeStr = String.format("PERIOD %d  •  %02d:%02d", period, minutes, seconds);
        g.drawString(timeStr, 35, 55);

        // Game mode indicator
        if (againstComputer) {
            g.fillRoundRect(WIDTH - 220, 20, 200, 40, 15, 15);
            g.setColor(Color.CYAN);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("🤖 VS COMPUTER", WIDTH - 200, 47);
        } else {
            g.fillRoundRect(WIDTH - 220, 20, 200, 40, 15, 15);
            g.setColor(Color.YELLOW);
            g.drawString("👥 2 PLAYER MODE", WIDTH - 200, 47);
        }
    }

    private void drawPlayerIndicators(Graphics2D g) {
        // Draw player numbers and puck possession indicators
        for (HockeyPlayer player : teamRed) {
            if (player.hasPuck) {
                g.setColor(new Color(255, 100, 100, 150));
                g.setStroke(new BasicStroke(3));
                g.drawOval((int)(player.x - PLAYER_SIZE/2 - 8),
                        (int)(player.y - PLAYER_SIZE/2 - 8),
                        PLAYER_SIZE + 16, PLAYER_SIZE + 16);
            }
        }

        for (HockeyPlayer player : teamBlue) {
            if (player.hasPuck) {
                g.setColor(new Color(100, 100, 255, 150));
                g.setStroke(new BasicStroke(3));
                g.drawOval((int)(player.x - PLAYER_SIZE/2 - 8),
                        (int)(player.y - PLAYER_SIZE/2 - 8),
                        PLAYER_SIZE + 16, PLAYER_SIZE + 16);
            }
        }
    }

    private void drawPauseScreen(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 220));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setFont(new Font("Arial", Font.BOLD, 72));
        g.setColor(Color.WHITE);
        g.drawString("⏸ PAUSED", WIDTH/2 - 140, HEIGHT/2 - 50);
        g.setFont(new Font("Arial", Font.PLAIN, 28));
        g.drawString("Press P to Resume", WIDTH/2 - 110, HEIGHT/2 + 30);
        g.drawString("Press ESC to Quit", WIDTH/2 - 100, HEIGHT/2 + 80);
    }

    private void drawControls(Graphics2D g) {
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(20, HEIGHT - 100, 500, 80, 10, 10);
        g.setColor(Color.WHITE);

        g.drawString("🔴 RED TEAM (All players): WASD + Space (Shoot) + Shift (Poke Check)", 30, HEIGHT - 75);

        if (againstComputer) {
            g.drawString("🔵 BLUE TEAM: AI Controlled - Intelligent positioning and tactics", 30, HEIGHT - 55);
        } else {
            g.drawString("🔵 BLUE TEAM (All players): Arrow Keys + Enter (Shoot) + Numpad0 (Poke Check)", 30, HEIGHT - 55);
        }

        g.drawString("⚡ All players on your team move together! ⚡", 30, HEIGHT - 35);
        g.drawString("⏸ P - Pause | ❌ ESC - Quit", 30, HEIGHT - 15);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updatePhysics();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys.add(e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_P) {
            paused = !paused;
        }
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            System.exit(0);
        }
        // Switch player control (optional feature)
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            currentPlayerIndex = (currentPlayerIndex + 1) % 3;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner classes
    class Puck {
        double x, y, vx, vy;
        Puck(double x, double y) { this.x = x; this.y = y; vx = 0; vy = 0; }
    }

    class HockeyPlayer {
        double x, y, vx, vy;
        String teamColor;
        String playerName;
        int playerNumber;
        int up, down, left, right, shoot, pokeCheck;
        boolean isAI = false;
        boolean isShooting = false;
        boolean isPokeChecking = false;
        boolean hasPuck = false;
        int shootCooldown = 0;
        int pokeCooldown = 0;
        boolean facingRight = true;
        int formationPosition;
        boolean isUserControlled = true;

        HockeyPlayer(int x, int y, String teamColor, String playerName, int playerNumber,
                     int up, int down, int left, int right, int shoot, int pokeCheck,
                     boolean isUserControlled) {
            this.x = x; this.y = y;
            this.teamColor = teamColor;
            this.playerName = playerName;
            this.playerNumber = playerNumber;
            this.up = up; this.down = down; this.left = left; this.right = right;
            this.shoot = shoot; this.pokeCheck = pokeCheck;
            this.isUserControlled = isUserControlled;
        }

        void update(Set<Integer> keys) {
            if (isAI) return;

            // All players on the team move together
            if (isUserControlled) {
                if (keys.contains(up)) vy = Math.max(-MAX_SPEED, vy - 0.7);
                if (keys.contains(down)) vy = Math.min(MAX_SPEED, vy + 0.7);
                if (keys.contains(left)) {
                    vx = Math.max(-MAX_SPEED, vx - 0.7);
                    facingRight = false;
                }
                if (keys.contains(right)) {
                    vx = Math.min(MAX_SPEED, vx + 0.7);
                    facingRight = true;
                }
                if (keys.contains(shoot) && shootCooldown == 0) {
                    isShooting = true;
                    shootCooldown = 20;
                }
                if (keys.contains(pokeCheck) && pokeCooldown == 0) {
                    isPokeChecking = true;
                    pokeCooldown = 15;
                }
            }
        }

        void draw(Graphics2D g) {
            // Player shadow
            g.setColor(new Color(0, 0, 0, 60));
            g.fillOval((int)(x - PLAYER_SIZE/2 + 6), (int)(y - PLAYER_SIZE/2 + 6),
                    PLAYER_SIZE, PLAYER_SIZE);

            // Body
            Color mainColor = teamColor.equals("RED") ? new Color(220, 60, 60) : new Color(60, 60, 220);
            g.setColor(mainColor);
            g.fillOval((int)(x - PLAYER_SIZE/2), (int)(y - PLAYER_SIZE/2), PLAYER_SIZE, PLAYER_SIZE);

            // Jersey stripes
            g.setColor(Color.WHITE);
            g.fillRect((int)(x - PLAYER_SIZE/2), (int)(y - PLAYER_SIZE/4), PLAYER_SIZE, PLAYER_SIZE/8);
            g.fillRect((int)(x - PLAYER_SIZE/2), (int)(y + PLAYER_SIZE/8), PLAYER_SIZE, PLAYER_SIZE/8);

            // Player number
            g.setFont(new Font("Arial", Font.BOLD, 18));
            String number = String.valueOf(playerNumber);
            FontMetrics fm = g.getFontMetrics();
            int numWidth = fm.stringWidth(number);
            g.drawString(number, (int)x - numWidth/2, (int)y + 8);

            // Helmet
            g.setColor(Color.BLACK);
            g.fillArc((int)(x - PLAYER_SIZE/2), (int)(y - PLAYER_SIZE/2),
                    PLAYER_SIZE, PLAYER_SIZE, 0, 180);

            // Visor
            g.setColor(new Color(100, 150, 200, 150));
            g.fillArc((int)(x - PLAYER_SIZE/3), (int)(y - PLAYER_SIZE/2.5),
                    (int) (PLAYER_SIZE/1.5), PLAYER_SIZE/3, 0, 180);

            // Stick
            g.setColor(new Color(180, 110, 50));
            g.setStroke(new BasicStroke(5));
            int stickX = (int)x + (facingRight ? 18 : -28);
            int stickY = (int)y + 8;
            g.drawLine((int)x + (facingRight ? 12 : -12), (int)y + 5,
                    stickX + (facingRight ? 25 : -25), stickY);

            // Stick blade
            g.fillRect(stickX, stickY, facingRight ? 22 : -22, 8);

            // Skates
            g.setColor(Color.DARK_GRAY);
            g.fillRoundRect((int)x - 16, (int)y + 18, 12, 10, 3, 3);
            g.fillRoundRect((int)x + 4, (int)y + 18, 12, 10, 3, 3);

            // Effect indicators
            if (isShooting) {
                g.setColor(Color.YELLOW);
                g.setStroke(new BasicStroke(3));
                g.drawOval((int)(x - PLAYER_SIZE/1.4), (int)(y - PLAYER_SIZE/1.4),
                        PLAYER_SIZE + 18, PLAYER_SIZE + 18);
            }

            if (isPokeChecking) {
                g.setColor(Color.ORANGE);
                g.setStroke(new BasicStroke(2.5f));
                g.drawLine((int)x + (facingRight ? 25 : -25), (int)y,
                        (int)x + (facingRight ? 75 : -75), (int)y);
            }

            // Name tag
            g.setFont(new Font("Arial", Font.BOLD, 11));
            g.setColor(Color.WHITE);
            String name = playerName;
            int nameWidth = g.getFontMetrics().stringWidth(name);
            g.fillRoundRect((int)x - nameWidth/2 - 4, (int)y - PLAYER_SIZE/2 - 18,
                    nameWidth + 8, 16, 5, 5);
            g.setColor(Color.BLACK);
            g.drawString(name, (int)x - nameWidth/2, (int)y - PLAYER_SIZE/2 - 7);
        }
    }

    class Goal {
        int x, y, width, height;
        Goal(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    public static void main(String[] args) {
        String[] options = {"🏒 Play vs Computer (3v3)", "👥 Two Players (3v3)"};
        int choice = JOptionPane.showOptionDialog(null,
                "🏒 ICE HOCKEY SIMULATOR - 3V3 LINEUP 🏒\n" +
                        "Experience full team hockey with 3 players per side!\n\n" +
                        "Select Game Mode:",
                "Hockey Game",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        JFrame frame = new JFrame("🏒 ICE HOCKEY SIMULATOR - 3v3 Full Team Hockey");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new IceHockeySimulator(choice == 0));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}