import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.Timer;

public class IceHockeySimulator extends JPanel implements ActionListener, KeyListener {
    // Game constants - Full rink, no movement restrictions
    private static final int WIDTH = 1400;
    private static final int HEIGHT = 900;
    private static final int GOAL_WIDTH = 180;
    private static final int GOAL_Y = HEIGHT/2 - GOAL_WIDTH/2;
    private static final int PUCK_SIZE = 20;
    private static final int PLAYER_SIZE = 40;
    private static final int MAX_SPEED = 10;
    private static final double FRICTION = 0.96;
    private static final double PUCK_FRICTION = 0.99;

    // Game objects
    private Puck puck;
    private ArrayList<HockeyPlayer> redTeam = new ArrayList<>();
    private ArrayList<HockeyPlayer> blueTeam = new ArrayList<>();
    private Goal leftGoal, rightGoal;
    private int scoreRed = 0, scoreBlue = 0;
    private int period = 1;
    private int gameTime = 20 * 60; // 20 minutes per period
    private Timer gameLoop, gameClock;
    private boolean gameRunning = true;
    private boolean againstComputer = true;
    private boolean paused = false;
    private boolean goalScored = false;
    private int goalResetTimer = 0;

    // Input handling
    private Set<Integer> pressedKeys = new HashSet<>();

    // AI variables
    private Random random = new Random();

    // Visual effects
    private ArrayList<TrailParticle> trails = new ArrayList<>();
    private ArrayList<GoalFlash> goalFlashes = new ArrayList<>();

    public IceHockeySimulator(boolean vsComputer) {
        this.againstComputer = vsComputer;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(200, 220, 255));
        setFocusable(true);
        addKeyListener(this);

        // Initialize puck in center
        puck = new Puck(WIDTH/2, HEIGHT/2);

        // Create Red Team (Left side) - 3 players, can move anywhere
        redTeam.add(new HockeyPlayer(200, HEIGHT/2 - 100, "RED", "WINGER", 1,
                KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,
                KeyEvent.VK_SPACE, KeyEvent.VK_SHIFT));
        redTeam.add(new HockeyPlayer(200, HEIGHT/2, "RED", "CENTER", 2,
                KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,
                KeyEvent.VK_SPACE, KeyEvent.VK_SHIFT));
        redTeam.add(new HockeyPlayer(200, HEIGHT/2 + 100, "RED", "DEFENDER", 3,
                KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,
                KeyEvent.VK_SPACE, KeyEvent.VK_SHIFT));

        // Create Blue Team (Right side)
        if (vsComputer) {
            blueTeam.add(new HockeyPlayer(WIDTH - 200, HEIGHT/2 - 100, "BLUE", "WINGER", 4));
            blueTeam.add(new HockeyPlayer(WIDTH - 200, HEIGHT/2, "BLUE", "CENTER", 5));
            blueTeam.add(new HockeyPlayer(WIDTH - 200, HEIGHT/2 + 100, "BLUE", "DEFENDER", 6));
            for (HockeyPlayer p : blueTeam) p.isAI = true;
        } else {
            blueTeam.add(new HockeyPlayer(WIDTH - 200, HEIGHT/2 - 100, "BLUE", "WINGER", 4,
                    KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                    KeyEvent.VK_ENTER, KeyEvent.VK_NUMPAD0));
            blueTeam.add(new HockeyPlayer(WIDTH - 200, HEIGHT/2, "BLUE", "CENTER", 5,
                    KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                    KeyEvent.VK_ENTER, KeyEvent.VK_NUMPAD0));
            blueTeam.add(new HockeyPlayer(WIDTH - 200, HEIGHT/2 + 100, "BLUE", "DEFENDER", 6,
                    KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                    KeyEvent.VK_ENTER, KeyEvent.VK_NUMPAD0));
        }

        // Goals - positioned at both ends
        leftGoal = new Goal(10, GOAL_Y, 15, GOAL_WIDTH);
        rightGoal = new Goal(WIDTH - 25, GOAL_Y, 15, GOAL_WIDTH);

        gameLoop = new Timer(1000 / 60, this);
        gameLoop.start();

        gameClock = new Timer(1000, e -> {
            if (gameRunning && !paused && !goalScored) {
                gameTime--;
                if (gameTime <= 0) nextPeriod();
            }
        });
        gameClock.start();
    }

    private void nextPeriod() {
        if (period < 3) {
            period++;
            gameTime = 20 * 60;
            resetPositions();
            showMessage("PERIOD " + period + " STARTED!", 2000);
        } else {
            endGame();
        }
    }

    private void endGame() {
        gameRunning = false;
        gameLoop.stop();
        gameClock.stop();
        String winner = scoreRed > scoreBlue ? "🔴 RED TEAM WINS!" :
                (scoreBlue > scoreRed ? "🔵 BLUE TEAM WINS!" : "🤝 TIE GAME!");
        JOptionPane.showMessageDialog(this,
                "═══════════════════════════\n" +
                        "     GAME OVER\n" +
                        "═══════════════════════════\n" +
                        winner + "\n" +
                        "Final Score: " + scoreRed + " - " + scoreBlue + "\n" +
                        "═══════════════════════════",
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

    private void resetPositions() {
        // Reset puck to center
        puck.x = WIDTH/2;
        puck.y = HEIGHT/2;
        puck.vx = 0;
        puck.vy = 0;

        // Reset players to their zones but not restricted
        redTeam.get(0).x = 200; redTeam.get(0).y = HEIGHT/2 - 100;
        redTeam.get(1).x = 200; redTeam.get(1).y = HEIGHT/2;
        redTeam.get(2).x = 200; redTeam.get(2).y = HEIGHT/2 + 100;

        blueTeam.get(0).x = WIDTH - 200; blueTeam.get(0).y = HEIGHT/2 - 100;
        blueTeam.get(1).x = WIDTH - 200; blueTeam.get(1).y = HEIGHT/2;
        blueTeam.get(2).x = WIDTH - 200; blueTeam.get(2).y = HEIGHT/2 + 100;

        for (HockeyPlayer p : redTeam) {
            p.vx = 0; p.vy = 0; p.hasPuck = false;
        }
        for (HockeyPlayer p : blueTeam) {
            p.vx = 0; p.vy = 0; p.hasPuck = false;
        }

        goalScored = false;
    }

    private void checkGoal() {
        if (goalScored) return;

        // Check if puck entered left goal (Blue scores)
        if (puck.x - PUCK_SIZE/2 < leftGoal.x + leftGoal.width &&
                puck.x + PUCK_SIZE/2 > leftGoal.x &&
                puck.y + PUCK_SIZE/2 > leftGoal.y &&
                puck.y - PUCK_SIZE/2 < leftGoal.y + leftGoal.height) {

            scoreBlue++;
            goalScored = true;
            goalFlashes.add(new GoalFlash(WIDTH/2, HEIGHT/2, "BLUE"));
            showMessage("🔵 BLUE TEAM SCORES! " + scoreRed + " - " + scoreBlue, 2000);

            Timer resetTimer = new Timer(2000, e -> {
                resetPositions();
                goalScored = false;
            });
            resetTimer.setRepeats(false);
            resetTimer.start();
        }
        // Check if puck entered right goal (Red scores)
        else if (puck.x + PUCK_SIZE/2 > rightGoal.x &&
                puck.x - PUCK_SIZE/2 < rightGoal.x + rightGoal.width &&
                puck.y + PUCK_SIZE/2 > rightGoal.y &&
                puck.y - PUCK_SIZE/2 < rightGoal.y + rightGoal.height) {

            scoreRed++;
            goalScored = true;
            goalFlashes.add(new GoalFlash(WIDTH/2, HEIGHT/2, "RED"));
            showMessage("🔴 RED TEAM SCORES! " + scoreRed + " - " + scoreBlue, 2000);

            Timer resetTimer = new Timer(2000, e -> {
                resetPositions();
                goalScored = false;
            });
            resetTimer.setRepeats(false);
            resetTimer.start();
        }
    }

    private void updateGame() {
        if (!gameRunning || paused || goalScored) return;

        // Update all players (free movement everywhere)
        for (HockeyPlayer player : redTeam) {
            player.update(pressedKeys);
            applyBoundaries(player);
        }

        for (HockeyPlayer player : blueTeam) {
            if (player.isAI) {
                updateAI(player);
            } else {
                player.update(pressedKeys);
            }
            applyBoundaries(player);
        }

        // Update puck physics
        updatePuck();

        // Handle collisions between players and puck
        handleCollisions();

        // Check for goals
        checkGoal();

        // Update effects
        updateTrails();
        updateGoalFlashes();

        // Prevent players from overlapping
        preventOverlap();
    }

    private void applyBoundaries(HockeyPlayer player) {
        // Players can move anywhere on the ice, just stay within rink bounds
        player.x = Math.max(PLAYER_SIZE/2 + 10, Math.min(WIDTH - PLAYER_SIZE/2 - 10, player.x));
        player.y = Math.max(PLAYER_SIZE/2 + 10, Math.min(HEIGHT - PLAYER_SIZE/2 - 10, player.y));

        // Apply friction
        player.vx *= FRICTION;
        player.vy *= FRICTION;
        player.x += player.vx;
        player.y += player.vy;

        // Update facing direction based on movement
        if (Math.abs(player.vx) > 0.5) {
            player.facingRight = player.vx > 0;
        }

        // Update cooldowns
        if (player.shootCooldown > 0) player.shootCooldown--;
        if (player.pokeCooldown > 0) player.pokeCooldown--;
    }

    private void updatePuck() {
        puck.vx *= PUCK_FRICTION;
        puck.vy *= PUCK_FRICTION;
        puck.x += puck.vx;
        puck.y += puck.vy;

        // Bounce off walls
        if (puck.y - PUCK_SIZE/2 < 10) {
            puck.vy = Math.abs(puck.vy) * 0.8;
            puck.y = PUCK_SIZE/2 + 10;
            addTrail(puck.x, puck.y);
        }
        if (puck.y + PUCK_SIZE/2 > HEIGHT - 10) {
            puck.vy = -Math.abs(puck.vy) * 0.8;
            puck.y = HEIGHT - PUCK_SIZE/2 - 10;
            addTrail(puck.x, puck.y);
        }

        // Bounce off left/right walls (except goal areas)
        if (puck.x - PUCK_SIZE/2 < 10) {
            puck.vx = Math.abs(puck.vx) * 0.8;
            puck.x = PUCK_SIZE/2 + 10;
            addTrail(puck.x, puck.y);
        }
        if (puck.x + PUCK_SIZE/2 > WIDTH - 10) {
            puck.vx = -Math.abs(puck.vx) * 0.8;
            puck.x = WIDTH - PUCK_SIZE/2 - 10;
            addTrail(puck.x, puck.y);
        }
    }

    private void handleCollisions() {
        // Check collisions with all players
        for (HockeyPlayer player : redTeam) {
            handlePlayerPuckCollision(player);
        }
        for (HockeyPlayer player : blueTeam) {
            handlePlayerPuckCollision(player);
        }
    }

    private void handlePlayerPuckCollision(HockeyPlayer player) {
        double dx = puck.x - player.x;
        double dy = puck.y - player.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double collisionDist = (player.size + PUCK_SIZE) / 2.0;

        if (distance < collisionDist) {
            // Calculate collision angle and force
            double angle = Math.atan2(dy, dx);
            double playerSpeed = Math.hypot(player.vx, player.vy);
            double power = Math.min(MAX_SPEED * 1.5, playerSpeed + 6);

            // Shooting boost
            if (player.isShooting) {
                power = MAX_SPEED * 2.5;
                player.isShooting = false;
                player.shootCooldown = 20;
                addTrail(puck.x, puck.y);
            }

            // Poke check
            if (player.isPokeChecking) {
                power = MAX_SPEED * 1.8;
                player.isPokeChecking = false;
                player.pokeCooldown = 15;
            }

            // Apply force to puck
            puck.vx = Math.cos(angle) * power + player.vx * 0.5;
            puck.vy = Math.sin(angle) * power + player.vy * 0.5;

            // Mark player as having puck possession
            player.hasPuck = true;

            // Separate puck and player
            double overlap = collisionDist - distance;
            puck.x += Math.cos(angle) * overlap;
            puck.y += Math.sin(angle) * overlap;

            // Add trail effect
            if (Math.abs(puck.vx) > 3 || Math.abs(puck.vy) > 3) {
                addTrail(puck.x, puck.y);
            }
        } else {
            player.hasPuck = false;
        }
    }

    private void updateAI(HockeyPlayer ai) {
        // Simple but effective AI - chase the puck anywhere on the ice
        double dx = puck.x - ai.x;
        double dy = puck.y - ai.y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Move toward puck
        if (distance > 5) {
            double angle = Math.atan2(dy, dx);
            double speed = Math.min(MAX_SPEED, distance / 10);
            ai.vx = Math.cos(angle) * speed;
            ai.vy = Math.sin(angle) * speed;
        }

        // AI shooting
        if (distance < 50 && !ai.hasPuck && ai.shootCooldown == 0) {
            if (random.nextInt(100) < 15) {
                ai.isShooting = true;
            }
        }

        // AI poke check
        if (distance < 40 && !ai.hasPuck && ai.pokeCooldown == 0) {
            if (random.nextInt(100) < 10) {
                ai.isPokeChecking = true;
            }
        }

        // AI takes shot when has puck and near opponent's goal
        if (ai.hasPuck && distance < 45 && ai.shootCooldown == 0) {
            // Aim at opponent's goal
            double targetGoalX = (ai.teamColor.equals("RED")) ? WIDTH - 50 : 50;
            double targetGoalY = HEIGHT/2;
            double angleToGoal = Math.atan2(targetGoalY - ai.y, targetGoalX - ai.x);

            // Smart shooting - aim at goal
            puck.vx = Math.cos(angleToGoal) * MAX_SPEED * 2;
            puck.vy = Math.sin(angleToGoal) * MAX_SPEED * 2;
            ai.shootCooldown = 30;
            addTrail(puck.x, puck.y);
        }

        // Update facing
        ai.facingRight = ai.vx > 0;
    }

    private void preventOverlap() {
        // Prevent players from overlapping with each other
        ArrayList<HockeyPlayer> allPlayers = new ArrayList<>();
        allPlayers.addAll(redTeam);
        allPlayers.addAll(blueTeam);

        for (int i = 0; i < allPlayers.size(); i++) {
            for (int j = i + 1; j < allPlayers.size(); j++) {
                HockeyPlayer p1 = allPlayers.get(i);
                HockeyPlayer p2 = allPlayers.get(j);
                double dx = p1.x - p2.x;
                double dy = p1.y - p2.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                double minDist = PLAYER_SIZE;

                if (distance < minDist) {
                    double angle = Math.atan2(dy, dx);
                    double overlap = minDist - distance;
                    p1.x += Math.cos(angle) * overlap / 2;
                    p1.y += Math.sin(angle) * overlap / 2;
                    p2.x -= Math.cos(angle) * overlap / 2;
                    p2.y -= Math.sin(angle) * overlap / 2;
                }
            }
        }
    }

    private void addTrail(double x, double y) {
        trails.add(new TrailParticle(x, y));
    }

    private void updateTrails() {
        Iterator<TrailParticle> it = trails.iterator();
        while (it.hasNext()) {
            TrailParticle t = it.next();
            t.life--;
            if (t.life <= 0) it.remove();
        }
    }

    private void updateGoalFlashes() {
        Iterator<GoalFlash> it = goalFlashes.iterator();
        while (it.hasNext()) {
            GoalFlash f = it.next();
            f.life--;
            if (f.life <= 0) it.remove();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw ice rink
        drawRink(g2d);

        // Draw goals
        drawGoals(g2d);

        // Draw trails
        for (TrailParticle t : trails) t.draw(g2d);

        // Draw players
        for (HockeyPlayer p : redTeam) p.draw(g2d);
        for (HockeyPlayer p : blueTeam) p.draw(g2d);

        // Draw puck
        drawPuck(g2d);

        // Draw goal flashes
        for (GoalFlash f : goalFlashes) f.draw(g2d);

        // Draw UI
        drawUI(g2d);

        if (paused) drawPause(g2d);
        drawInstructions(g2d);
    }

    private void drawRink(Graphics2D g) {
        // Ice surface
        GradientPaint iceGrad = new GradientPaint(0, 0, new Color(200, 225, 250),
                WIDTH, HEIGHT, new Color(170, 200, 235));
        g.setPaint(iceGrad);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Rink border
        g.setColor(new Color(139, 69, 19));
        g.setStroke(new BasicStroke(8));
        g.drawRect(10, 10, WIDTH - 20, HEIGHT - 20);

        // Center line
        g.setColor(Color.BLUE);
        g.setStroke(new BasicStroke(4));
        g.drawLine(WIDTH/2, 10, WIDTH/2, HEIGHT - 10);

        // Center circle
        g.drawOval(WIDTH/2 - 70, HEIGHT/2 - 70, 140, 140);

        // Faceoff circles
        g.drawOval(180, HEIGHT/2 - 60, 120, 120);
        g.drawOval(WIDTH - 300, HEIGHT/2 - 60, 120, 120);

        // Faceoff dots
        g.fillOval(WIDTH/2 - 6, HEIGHT/2 - 6, 12, 12);
        g.fillOval(235, HEIGHT/2 - 6, 12, 12);
        g.fillOval(WIDTH - 247, HEIGHT/2 - 6, 12, 12);

        // Goal creases
        g.setColor(new Color(200, 200, 255, 100));
        g.fillArc(leftGoal.x - 25, leftGoal.y - 25, 60, leftGoal.height + 50, -90, 180);
        g.fillArc(rightGoal.x - 35, rightGoal.y - 25, 60, rightGoal.height + 50, 90, 180);
    }

    private void drawGoals(Graphics2D g) {
        // Left goal
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(leftGoal.x, leftGoal.y, leftGoal.width, leftGoal.height);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(3));
        g.drawRect(leftGoal.x, leftGoal.y, leftGoal.width, leftGoal.height);
        g.setColor(Color.RED);
        g.drawLine(leftGoal.x + leftGoal.width, leftGoal.y, leftGoal.x + leftGoal.width, leftGoal.y + leftGoal.height);

        // Goal netting
        g.setColor(new Color(150, 150, 150, 100));
        for (int i = 0; i < 8; i++) {
            g.drawLine(leftGoal.x + leftGoal.width, leftGoal.y + i * (leftGoal.height/8),
                    leftGoal.x + leftGoal.width + 25, leftGoal.y + i * (leftGoal.height/8));
        }

        // Right goal
        g.fillRect(rightGoal.x, rightGoal.y, rightGoal.width, rightGoal.height);
        g.setColor(Color.BLACK);
        g.drawRect(rightGoal.x, rightGoal.y, rightGoal.width, rightGoal.height);
        g.setColor(Color.RED);
        g.drawLine(rightGoal.x, rightGoal.y, rightGoal.x, rightGoal.y + rightGoal.height);

        for (int i = 0; i < 8; i++) {
            g.drawLine(rightGoal.x, rightGoal.y + i * (rightGoal.height/8),
                    rightGoal.x - 25, rightGoal.y + i * (rightGoal.height/8));
        }
    }

    private void drawPuck(Graphics2D g) {
        // Puck shadow
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval((int)(puck.x - PUCK_SIZE/2 + 4), (int)(puck.y - PUCK_SIZE/2 + 4), PUCK_SIZE, PUCK_SIZE);

        // Puck body
        RadialGradientPaint puckGrad = new RadialGradientPaint(
                (float)puck.x, (float)puck.y, PUCK_SIZE/2,
                new float[]{0f, 0.7f, 1f},
                new Color[]{Color.GRAY, Color.DARK_GRAY, Color.BLACK});
        g.setPaint(puckGrad);
        g.fillOval((int)(puck.x - PUCK_SIZE/2), (int)(puck.y - PUCK_SIZE/2), PUCK_SIZE, PUCK_SIZE);

        // Puck highlight
        g.setColor(new Color(255, 255, 255, 100));
        g.fillOval((int)(puck.x - PUCK_SIZE/3), (int)(puck.y - PUCK_SIZE/3), (int) (PUCK_SIZE/1.5), (int) (PUCK_SIZE/1.5));
    }

    private void drawUI(Graphics2D g) {
        // Scoreboard
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRoundRect(WIDTH/2 - 180, 15, 360, 90, 20, 20);

        // Scores
        g.setFont(new Font("Arial", Font.BOLD, 52));
        g.setColor(Color.RED);
        g.drawString(String.valueOf(scoreRed), WIDTH/2 - 110, 85);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.drawString(":", WIDTH/2 - 20, 85);
        g.setColor(Color.BLUE);
        g.setFont(new Font("Arial", Font.BOLD, 52));
        g.drawString(String.valueOf(scoreBlue), WIDTH/2 + 60, 85);

        // Period and time
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        int minutes = gameTime / 60;
        int seconds = gameTime % 60;
        String timeStr = String.format("PERIOD %d   %02d:%02d", period, minutes, seconds);
        g.drawString(timeStr, WIDTH/2 - 80, 45);
    }

    private void drawPause(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setFont(new Font("Arial", Font.BOLD, 72));
        g.setColor(Color.WHITE);
        g.drawString("PAUSED", WIDTH/2 - 120, HEIGHT/2);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Press P to Resume", WIDTH/2 - 100, HEIGHT/2 + 60);
    }

    private void drawInstructions(Graphics2D g) {
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(15, HEIGHT - 130, 650, 115, 10, 10);
        g.setColor(Color.WHITE);

        g.drawString("🎮 CONTROLS:", 25, HEIGHT - 115);
        g.drawString("🔴 RED TEAM: WASD - Move | SPACE - Shoot | SHIFT - Poke Check", 25, HEIGHT - 95);
        g.drawString("🔵 BLUE TEAM: Arrow Keys - Move | ENTER - Shoot | NUMPAD0 - Poke Check (2P mode)", 25, HEIGHT - 75);
        g.drawString("🏒 GAMEPLAY: Hit the puck with your player's stick to shoot toward opponent's goal!", 25, HEIGHT - 55);
        g.drawString("💡 TIPS: Full ice movement - chase the puck anywhere! Shoot when close to goal!", 25, HEIGHT - 35);
        g.drawString("⏸ P - Pause | ❌ ESC - Quit", 25, HEIGHT - 15);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateGame();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys.add(e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_P) paused = !paused;
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner Classes
    class Puck {
        double x, y, vx, vy;
        Puck(double x, double y) { this.x = x; this.y = y; }
    }

    class HockeyPlayer {
        double x, y, vx, vy;
        String teamColor, playerType;
        int number, size = PLAYER_SIZE;
        int up, down, left, right, shoot, pokeCheck;
        boolean isAI = false;
        boolean isShooting = false;
        boolean isPokeChecking = false;
        boolean hasPuck = false;
        int shootCooldown = 0;
        int pokeCooldown = 0;
        boolean facingRight = true;

        // Constructor for AI players
        HockeyPlayer(int x, int y, String teamColor, String playerType, int number) {
            this(x, y, teamColor, playerType, number, 0, 0, 0, 0, 0, 0);
            this.isAI = true;
        }

        // Constructor for human players
        HockeyPlayer(int x, int y, String teamColor, String playerType, int number,
                     int up, int down, int left, int right, int shoot, int pokeCheck) {
            this.x = x; this.y = y;
            this.teamColor = teamColor;
            this.playerType = playerType;
            this.number = number;
            this.up = up; this.down = down; this.left = left; this.right = right;
            this.shoot = shoot; this.pokeCheck = pokeCheck;
        }

        void update(Set<Integer> keys) {
            if (isAI) return;

            // Movement
            if (keys.contains(up)) vy = Math.max(-MAX_SPEED, vy - 0.7);
            if (keys.contains(down)) vy = Math.min(MAX_SPEED, vy + 0.7);
            if (keys.contains(left)) vx = Math.max(-MAX_SPEED, vx - 0.7);
            if (keys.contains(right)) vx = Math.min(MAX_SPEED, vx + 0.7);
            if (keys.contains(shoot) && shootCooldown == 0) {
                isShooting = true;
                shootCooldown = 20;
            }
            if (keys.contains(pokeCheck) && pokeCooldown == 0) {
                isPokeChecking = true;
                pokeCooldown = 15;
            }
        }

        void draw(Graphics2D g) {
            // Shadow
            g.setColor(new Color(0, 0, 0, 60));
            g.fillOval((int)(x - size/2 + 5), (int)(y - size/2 + 5), size, size);

            // Body
            Color mainColor = teamColor.equals("RED") ? new Color(220, 50, 50) : new Color(50, 50, 220);
            RadialGradientPaint bodyGrad = new RadialGradientPaint(
                    (float)x, (float)y, size/2,
                    new float[]{0f, 1f},
                    new Color[]{mainColor.brighter(), mainColor});
            g.setPaint(bodyGrad);
            g.fillOval((int)(x - size/2), (int)(y - size/2), size, size);

            // Jersey number
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.setColor(Color.WHITE);
            String num = String.valueOf(number);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(num, (int)x - fm.stringWidth(num)/2, (int)y + 7);

            // Helmet
            g.setColor(Color.BLACK);
            g.fillArc((int)(x - size/2), (int)(y - size/2), size, size, 0, 180);

            // Stick
            g.setColor(new Color(180, 100, 40));
            g.setStroke(new BasicStroke(4));
            int stickX = (int)x + (facingRight ? 18 : -28);
            g.drawLine((int)x + (facingRight ? 10 : -10), (int)y + 5,
                    stickX + (facingRight ? 25 : -25), (int)y + 8);
            g.fillRect(stickX, (int)y + 5, facingRight ? 22 : -22, 7);

            // Skates
            g.setColor(Color.DARK_GRAY);
            g.fillRoundRect((int)x - 16, (int)y + 18, 12, 10, 3, 3);
            g.fillRoundRect((int)x + 4, (int)y + 18, 12, 10, 3, 3);

            // Effects
            if (isShooting) {
                g.setColor(Color.YELLOW);
                g.setStroke(new BasicStroke(2));
                g.drawOval((int)(x - size/1.3), (int)(y - size/1.3), size + 15, size + 15);
            }

            if (hasPuck) {
                g.setColor(Color.CYAN);
                g.setStroke(new BasicStroke(2));
                g.drawOval((int)(x - size/2 - 3), (int)(y - size/2 - 3), size + 6, size + 6);
            }

            // Name tag
            g.setFont(new Font("Arial", Font.BOLD, 11));
            String name = playerType;
            int nameWidth = g.getFontMetrics().stringWidth(name);
            g.setColor(Color.BLACK);
            g.fillRoundRect((int)x - nameWidth/2 - 3, (int)y - size/2 - 15, nameWidth + 6, 14, 5, 5);
            g.setColor(Color.WHITE);
            g.drawString(name, (int)x - nameWidth/2, (int)y - size/2 - 5);
        }
    }

    class Goal {
        int x, y, width, height;
        Goal(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    class TrailParticle {
        double x, y;
        int life = 20;
        TrailParticle(double x, double y) { this.x = x; this.y = y; }
        void draw(Graphics2D g) {
            g.setColor(new Color(200, 200, 255, life * 5));
            g.fillOval((int)x - 3, (int)y - 3, 6, 6);
        }
    }

    class GoalFlash {
        double x, y;
        int life = 30;
        String team;
        GoalFlash(double x, double y, String team) { this.x = x; this.y = y; this.team = team; }
        void draw(Graphics2D g) {
            Color color = team.equals("RED") ? new Color(255, 0, 0, life * 8) : new Color(0, 0, 255, life * 8);
            g.setColor(color);
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }

    public static void main(String[] args) {
        String[] options = {"🏒 PLAY VS COMPUTER (3v3)", "👥 TWO PLAYERS (3v3)"};
        int choice = JOptionPane.showOptionDialog(null,
                "🏒 ICE HOCKEY SIMULATOR - FULL ICE 3v3 🏒\n" +
                        "Complete freedom of movement across entire rink!\n" +
                        "Control your team, shoot the puck, score goals!\n\n" +
                        "Select Game Mode:",
                "Ice Hockey Game",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        JFrame frame = new JFrame("🏒 ICE HOCKEY SIMULATOR - 3v3 Full Ice Hockey");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new IceHockeySimulator(choice == 0));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}