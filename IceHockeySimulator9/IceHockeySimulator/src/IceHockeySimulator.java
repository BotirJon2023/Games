import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.Timer;

public class IceHockeySimulator extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 1400;
    private static final int HEIGHT = 900;
    private static final int GOAL_WIDTH = 180;
    private static final int GOAL_Y = HEIGHT/2 - GOAL_WIDTH/2;
    private static final int PUCK_SIZE = 22;
    private static final int PLAYER_SIZE = 42;
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
    private int gameTime = 20 * 60;
    private Timer gameLoop, gameClock;
    private boolean gameRunning = true;
    private boolean againstComputer = true;
    private boolean paused = false;
    private boolean goalScored = false;

    // Input handling
    private Set<Integer> pressedKeys = new HashSet<>();

    // AI
    private Random random = new Random();

    // SPECTACULAR VISUAL EFFECTS
    private ArrayList<RainbowTrail> trails = new ArrayList<>();
    private ArrayList<GoalExplosion> goalExplosions = new ArrayList<>();
    private ArrayList<ColorSpark> colorSparks = new ArrayList<>();
    private ArrayList<Confetti> confetti = new ArrayList<>();
    private ArrayList<Star> stars = new ArrayList<>();
    private float hue = 0;
    private float rainbowCycle = 0;

    // Rainbow background colors
    private Color[] rainbowColors = {
            new Color(255, 50, 50),   // Red
            new Color(255, 150, 50),  // Orange
            new Color(255, 255, 50),  // Yellow
            new Color(50, 255, 50),   // Green
            new Color(50, 150, 255),  // Blue
            new Color(150, 50, 255),  // Indigo
            new Color(255, 50, 150)   // Violet
    };

    public IceHockeySimulator(boolean vsComputer) {
        this.againstComputer = vsComputer;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        // Initialize puck
        puck = new Puck(WIDTH/2, HEIGHT/2);

        // Create Red Team - Rainbow Red theme
        redTeam.add(new HockeyPlayer(200, HEIGHT/2 - 100, "RED", "🌈 RAINBOW", 1,
                KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,
                KeyEvent.VK_SPACE, KeyEvent.VK_SHIFT));
        redTeam.add(new HockeyPlayer(200, HEIGHT/2, "RED", "✨ SPARKLE", 2,
                KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,
                KeyEvent.VK_SPACE, KeyEvent.VK_SHIFT));
        redTeam.add(new HockeyPlayer(200, HEIGHT/2 + 100, "RED", "🎨 ARTIST", 3,
                KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,
                KeyEvent.VK_SPACE, KeyEvent.VK_SHIFT));

        // Create Blue Team - Rainbow Blue theme
        if (vsComputer) {
            blueTeam.add(new HockeyPlayer(WIDTH - 200, HEIGHT/2 - 100, "BLUE", "🌊 WAVE", 4));
            blueTeam.add(new HockeyPlayer(WIDTH - 200, HEIGHT/2, "BLUE", "💎 CRYSTAL", 5));
            blueTeam.add(new HockeyPlayer(WIDTH - 200, HEIGHT/2 + 100, "BLUE", "🌟 STAR", 6));
            for (HockeyPlayer p : blueTeam) p.isAI = true;
        } else {
            blueTeam.add(new HockeyPlayer(WIDTH - 200, HEIGHT/2 - 100, "BLUE", "🌊 WAVE", 4,
                    KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                    KeyEvent.VK_ENTER, KeyEvent.VK_NUMPAD0));
            blueTeam.add(new HockeyPlayer(WIDTH - 200, HEIGHT/2, "BLUE", "💎 CRYSTAL", 5,
                    KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                    KeyEvent.VK_ENTER, KeyEvent.VK_NUMPAD0));
            blueTeam.add(new HockeyPlayer(WIDTH - 200, HEIGHT/2 + 100, "BLUE", "🌟 STAR", 6,
                    KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                    KeyEvent.VK_ENTER, KeyEvent.VK_NUMPAD0));
        }

        // Goals
        leftGoal = new Goal(10, GOAL_Y, 15, GOAL_WIDTH);
        rightGoal = new Goal(WIDTH - 25, GOAL_Y, 15, GOAL_WIDTH);

        // Create decorative stars
        for (int i = 0; i < 150; i++) {
            stars.add(new Star(random.nextInt(WIDTH), random.nextInt(HEIGHT)));
        }

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
            showMessage("🎉 PERIOD " + period + " 🎉", 2000);
            addColorExplosion(WIDTH/2, HEIGHT/2);
        } else {
            endGame();
        }
    }

    private void endGame() {
        gameRunning = false;
        gameLoop.stop();
        gameClock.stop();

        // Final celebration
        for (int i = 0; i < 200; i++) {
            confetti.add(new Confetti(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    rainbowColors[random.nextInt(rainbowColors.length)]));
        }

        String winner = scoreRed > scoreBlue ? "🏆🌈 RED TEAM VICTORY! 🌈🏆" :
                (scoreBlue > scoreRed ? "🏆💙 BLUE TEAM VICTORY! 💙🏆" : "⚡✨ COSMIC TIE! ✨⚡");
        JOptionPane.showMessageDialog(this,
                "═══════════════════════════════════\n" +
                        "        🎉 GAME OVER 🎉\n" +
                        "═══════════════════════════════════\n" +
                        winner + "\n" +
                        "Final Score: " + scoreRed + " - " + scoreBlue + "\n" +
                        "═══════════════════════════════════",
                "Game Over", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    private void showMessage(String msg, int duration) {
        final JLabel message = new JLabel(msg, SwingConstants.CENTER);
        message.setFont(new Font("Arial", Font.BOLD, 48));
        message.setForeground(new Color(255, 215, 0));
        message.setBounds(WIDTH/2 - 300, HEIGHT/2 - 50, 600, 100);
        add(message);
        Timer timer = new Timer(duration, e -> remove(message));
        timer.setRepeats(false);
        timer.start();
    }

    private void resetPositions() {
        puck.x = WIDTH/2;
        puck.y = HEIGHT/2;
        puck.vx = 0;
        puck.vy = 0;

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

        if (puck.x - PUCK_SIZE/2 < leftGoal.x + leftGoal.width &&
                puck.x + PUCK_SIZE/2 > leftGoal.x &&
                puck.y + PUCK_SIZE/2 > leftGoal.y &&
                puck.y - PUCK_SIZE/2 < leftGoal.y + leftGoal.height) {

            scoreBlue++;
            goalScored = true;
            addGoalCelebration(goal2.x, goal2.y + goal2.height/2, "BLUE");
            showMessage("💙⚡ GOAL! BLUE TEAM SCORES! ⚡💙 " + scoreRed + " - " + scoreBlue, 2000);

            Timer resetTimer = new Timer(2000, e -> {
                resetPositions();
                goalScored = false;
            });
            resetTimer.setRepeats(false);
            resetTimer.start();
        }
        else if (puck.x + PUCK_SIZE/2 > rightGoal.x &&
                puck.x - PUCK_SIZE/2 < rightGoal.x + rightGoal.width &&
                puck.y + PUCK_SIZE/2 > rightGoal.y &&
                puck.y - PUCK_SIZE/2 < rightGoal.y + rightGoal.height) {

            scoreRed++;
            goalScored = true;
            addGoalCelebration(goal1.x, goal1.y + goal1.height/2, "RED");
            showMessage("❤️🌈 GOAL! RED TEAM SCORES! 🌈❤️ " + scoreRed + " - " + scoreBlue, 2000);

            Timer resetTimer = new Timer(2000, e -> {
                resetPositions();
                goalScored = false;
            });
            resetTimer.setRepeats(false);
            resetTimer.start();
        }
    }

    private void addGoalCelebration(int x, int y, String team) {
        // Massive particle explosion
        for (int i = 0; i < 150; i++) {
            Color color = team.equals("RED") ?
                    rainbowColors[random.nextInt(rainbowColors.length)] :
                    new Color(50 + random.nextInt(200), 50 + random.nextInt(200), 255);
            goalExplosions.add(new GoalExplosion(x, y, color));
        }

        // Add confetti
        for (int i = 0; i < 100; i++) {
            confetti.add(new Confetti(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    rainbowColors[random.nextInt(rainbowColors.length)]));
        }

        // Add color sparks
        for (int i = 0; i < 80; i++) {
            colorSparks.add(new ColorSpark(x, y, team));
        }
    }

    private void addColorExplosion(int x, int y) {
        for (int i = 0; i < 100; i++) {
            goalExplosions.add(new GoalExplosion(x, y, rainbowColors[random.nextInt(rainbowColors.length)]));
        }
    }

    private void updateGame() {
        if (!gameRunning || paused || goalScored) return;

        rainbowCycle += 0.02f;
        hue += 0.01f;

        // Update players
        for (HockeyPlayer player : redTeam) {
            player.update(pressedKeys);
            applyBoundaries(player);
        }

        for (HockeyPlayer player : blueTeam) {
            if (player.isAI) updateAI(player);
            else player.update(pressedKeys);
            applyBoundaries(player);
        }

        // Update puck
        updatePuck();

        // Handle collisions
        handleCollisions();

        // Check goals
        checkGoal();

        // Update ALL effects
        updateTrails();
        updateGoalExplosions();
        updateColorSparks();
        updateConfetti();
        updateStars();

        // Prevent overlap
        preventOverlap();
    }

    private void applyBoundaries(HockeyPlayer player) {
        player.x = Math.max(PLAYER_SIZE/2 + 10, Math.min(WIDTH - PLAYER_SIZE/2 - 10, player.x));
        player.y = Math.max(PLAYER_SIZE/2 + 10, Math.min(HEIGHT - PLAYER_SIZE/2 - 10, player.y));

        player.vx *= FRICTION;
        player.vy *= FRICTION;
        player.x += player.vx;
        player.y += player.vy;

        if (Math.abs(player.vx) > 0.5) player.facingRight = player.vx > 0;

        if (player.shootCooldown > 0) player.shootCooldown--;
        if (player.pokeCooldown > 0) player.pokeCooldown--;
    }

    private void updatePuck() {
        puck.vx *= PUCK_FRICTION;
        puck.vy *= PUCK_FRICTION;
        puck.x += puck.vx;
        puck.y += puck.vy;

        if (puck.y - PUCK_SIZE/2 < 10) {
            puck.vy = Math.abs(puck.vy) * 0.8;
            puck.y = PUCK_SIZE/2 + 10;
            addRainbowTrail(puck.x, puck.y);
        }
        if (puck.y + PUCK_SIZE/2 > HEIGHT - 10) {
            puck.vy = -Math.abs(puck.vy) * 0.8;
            puck.y = HEIGHT - PUCK_SIZE/2 - 10;
            addRainbowTrail(puck.x, puck.y);
        }
        if (puck.x - PUCK_SIZE/2 < 10) {
            puck.vx = Math.abs(puck.vx) * 0.8;
            puck.x = PUCK_SIZE/2 + 10;
            addRainbowTrail(puck.x, puck.y);
        }
        if (puck.x + PUCK_SIZE/2 > WIDTH - 10) {
            puck.vx = -Math.abs(puck.vx) * 0.8;
            puck.x = WIDTH - PUCK_SIZE/2 - 10;
            addRainbowTrail(puck.x, puck.y);
        }
    }

    private void handleCollisions() {
        for (HockeyPlayer player : redTeam) handlePlayerPuckCollision(player);
        for (HockeyPlayer player : blueTeam) handlePlayerPuckCollision(player);
    }

    private void handlePlayerPuckCollision(HockeyPlayer player) {
        double dx = puck.x - player.x;
        double dy = puck.y - player.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double collisionDist = (player.size + PUCK_SIZE) / 2.0;

        if (distance < collisionDist) {
            double angle = Math.atan2(dy, dx);
            double playerSpeed = Math.hypot(player.vx, player.vy);
            double power = Math.min(MAX_SPEED * 1.5, playerSpeed + 6);

            if (player.isShooting) {
                power = MAX_SPEED * 2.8;
                player.isShooting = false;
                player.shootCooldown = 20;
                addColorExplosion((int) puck.x, (int) puck.y);
            }

            if (player.isPokeChecking) {
                power = MAX_SPEED * 2.0;
                player.isPokeChecking = false;
                player.pokeCooldown = 15;
                addColorSparks(puck.x, puck.y);
            }

            puck.vx = Math.cos(angle) * power + player.vx * 0.5;
            puck.vy = Math.sin(angle) * power + player.vy * 0.5;
            player.hasPuck = true;

            double overlap = collisionDist - distance;
            puck.x += Math.cos(angle) * overlap;
            puck.y += Math.sin(angle) * overlap;

            if (Math.abs(puck.vx) > 3 || Math.abs(puck.vy) > 3) {
                addRainbowTrail(puck.x, puck.y);
            }
        } else {
            player.hasPuck = false;
        }
    }

    private void updateAI(HockeyPlayer ai) {
        double dx = puck.x - ai.x;
        double dy = puck.y - ai.y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 5) {
            double angle = Math.atan2(dy, dx);
            double speed = Math.min(MAX_SPEED, distance / 10);
            ai.vx = Math.cos(angle) * speed;
            ai.vy = Math.sin(angle) * speed;
        }

        if (distance < 50 && !ai.hasPuck && ai.shootCooldown == 0) {
            if (random.nextInt(100) < 15) ai.isShooting = true;
        }

        if (distance < 40 && !ai.hasPuck && ai.pokeCooldown == 0) {
            if (random.nextInt(100) < 10) ai.isPokeChecking = true;
        }

        if (ai.hasPuck && distance < 45 && ai.shootCooldown == 0) {
            double targetGoalX = (ai.teamColor.equals("RED")) ? WIDTH - 50 : 50;
            double targetGoalY = HEIGHT/2;
            double angleToGoal = Math.atan2(targetGoalY - ai.y, targetGoalX - ai.x);
            puck.vx = Math.cos(angleToGoal) * MAX_SPEED * 2.2;
            puck.vy = Math.sin(angleToGoal) * MAX_SPEED * 2.2;
            ai.shootCooldown = 30;
            addRainbowTrail(puck.x, puck.y);
        }

        ai.facingRight = ai.vx > 0;
    }

    private void preventOverlap() {
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

    private void addRainbowTrail(double x, double y) {
        trails.add(new RainbowTrail(x, y, rainbowColors[random.nextInt(rainbowColors.length)]));
    }

    private void addColorSparks(double x, double y) {
        for (int i = 0; i < 15; i++) {
            colorSparks.add(new ColorSpark(x, y, "RAINBOW"));
        }
    }

    private void updateTrails() {
        Iterator<RainbowTrail> it = trails.iterator();
        while (it.hasNext()) {
            RainbowTrail t = it.next();
            t.life--;
            if (t.life <= 0) it.remove();
        }
    }

    private void updateGoalExplosions() {
        Iterator<GoalExplosion> it = goalExplosions.iterator();
        while (it.hasNext()) {
            GoalExplosion e = it.next();
            e.life--;
            e.x += e.vx;
            e.y += e.vy;
            e.vy += 0.3; // Gravity
            if (e.life <= 0) it.remove();
        }
    }

    private void updateColorSparks() {
        Iterator<ColorSpark> it = colorSparks.iterator();
        while (it.hasNext()) {
            ColorSpark s = it.next();
            s.life--;
            s.x += s.vx;
            s.y += s.vy;
            if (s.life <= 0) it.remove();
        }
    }

    private void updateConfetti() {
        Iterator<Confetti> it = confetti.iterator();
        while (it.hasNext()) {
            Confetti c = it.next();
            c.life--;
            c.x += c.vx;
            c.y += c.vy;
            c.vy += 0.2;
            if (c.life <= 0 || c.y > HEIGHT) it.remove();
        }
    }

    private void updateStars() {
        for (Star s : stars) {
            s.update();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // RAINBOW BACKGROUND
        drawRainbowArena(g2d);

        // Draw stars (background)
        for (Star s : stars) s.draw(g2d);

        // Draw confetti
        for (Confetti c : confetti) c.draw(g2d);

        // Draw goals with rainbow effects
        drawRainbowGoals(g2d);

        // Draw trails
        for (RainbowTrail t : trails) t.draw(g2d);

        // Draw color sparks
        for (ColorSpark s : colorSparks) s.draw(g2d);

        // Draw players
        for (HockeyPlayer p : redTeam) p.draw(g2d);
        for (HockeyPlayer p : blueTeam) p.draw(g2d);

        // Draw puck
        drawRainbowPuck(g2d);

        // Draw explosions
        for (GoalExplosion e : goalExplosions) e.draw(g2d);

        // Draw HUD
        drawRainbowHUD(g2d);

        if (paused) drawPause(g2d);
        drawRainbowInstructions(g2d);
    }

    private void drawRainbowArena(Graphics2D g) {
        // Rainbow gradient background
        float[] fractions = {0f, 0.16f, 0.33f, 0.5f, 0.66f, 0.83f, 1f};
        Color[] colors = {
                new Color(255, 50, 50, 60),
                new Color(255, 150, 50, 60),
                new Color(255, 255, 50, 60),
                new Color(50, 255, 50, 60),
                new Color(50, 150, 255, 60),
                new Color(150, 50, 255, 60),
                new Color(255, 50, 150, 60)
        };

        LinearGradientPaint rainbowBg = new LinearGradientPaint(0, 0, WIDTH, HEIGHT, fractions, colors);
        g.setPaint(rainbowBg);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Ice surface with sparkle
        g.setColor(new Color(200, 220, 255, 100));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Rainbow rink border
        for (int i = 0; i < rainbowColors.length; i++) {
            g.setColor(rainbowColors[i]);
            g.setStroke(new BasicStroke(3));
            g.drawRect(10 + i, 10 + i, WIDTH - 20 - (i*2), HEIGHT - 20 - (i*2));
        }

        // Rainbow center line
        g.setStroke(new BasicStroke(5));
        for (int i = 0; i < rainbowColors.length; i++) {
            g.setColor(rainbowColors[i]);
            g.drawLine(WIDTH/2, 10 + i, WIDTH/2, HEIGHT - 10 - i);
        }

        // Animated rainbow center circle
        for (int i = 0; i < 7; i++) {
            g.setColor(rainbowColors[(int)((rainbowCycle + i) * 10) % rainbowColors.length]);
            g.drawOval(WIDTH/2 - 70 + i, HEIGHT/2 - 70 + i, 140 - (i*2), 140 - (i*2));
        }

        // Rainbow faceoff circles
        for (int i = 0; i < rainbowColors.length; i++) {
            g.setColor(rainbowColors[i]);
            g.drawOval(180 + i, HEIGHT/2 - 60 + i, 120 - (i*2), 120 - (i*2));
            g.drawOval(WIDTH - 300 + i, HEIGHT/2 - 60 + i, 120 - (i*2), 120 - (i*2));
        }

        // Glowing faceoff dots
        for (int i = 0; i < 3; i++) {
            g.setColor(rainbowColors[random.nextInt(rainbowColors.length)]);
            g.fillOval(WIDTH/2 - 8 + i*2, HEIGHT/2 - 8 + i*2, 16 - i*2, 16 - i*2);
        }
    }

    private void drawRainbowGoals(Graphics2D g) {
        // Left goal with rainbow effect
        for (int i = 0; i < 5; i++) {
            g.setColor(rainbowColors[i % rainbowColors.length]);
            g.fillRect(leftGoal.x + i, leftGoal.y + i, leftGoal.width, leftGoal.height - (i*2));
        }
        g.setColor(Color.WHITE);
        g.drawLine(leftGoal.x + leftGoal.width, leftGoal.y, leftGoal.x + leftGoal.width, leftGoal.y + leftGoal.height);

        // Rainbow netting
        for (int i = 0; i < 8; i++) {
            g.setColor(rainbowColors[i % rainbowColors.length]);
            g.drawLine(leftGoal.x + leftGoal.width, leftGoal.y + i * (leftGoal.height/8),
                    leftGoal.x + leftGoal.width + 25, leftGoal.y + i * (leftGoal.height/8));
        }

        // Right goal
        for (int i = 0; i < 5; i++) {
            g.setColor(rainbowColors[(i+3) % rainbowColors.length]);
            g.fillRect(rightGoal.x - i, rightGoal.y + i, rightGoal.width, rightGoal.height - (i*2));
        }
        g.setColor(Color.WHITE);
        g.drawLine(rightGoal.x, rightGoal.y, rightGoal.x, rightGoal.y + rightGoal.height);

        for (int i = 0; i < 8; i++) {
            g.setColor(rainbowColors[(i+4) % rainbowColors.length]);
            g.drawLine(rightGoal.x, rightGoal.y + i * (rightGoal.height/8),
                    rightGoal.x - 25, rightGoal.y + i * (rightGoal.height/8));
        }
    }

    private void drawRainbowPuck(Graphics2D g) {
        // Rainbow glow
        for (int i = 5; i > 0; i--) {
            g.setColor(rainbowColors[(int)((rainbowCycle + i) * 20) % rainbowColors.length]);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g.fillOval((int)(puck.x - PUCK_SIZE/2 - i*2), (int)(puck.y - PUCK_SIZE/2 - i*2),
                    PUCK_SIZE + i*4, PUCK_SIZE + i*4);
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // Puck body - rainbow gradient
        RadialGradientPaint puckGrad = new RadialGradientPaint(
                (float)puck.x, (float)puck.y, PUCK_SIZE/2,
                new float[]{0f, 0.5f, 1f},
                new Color[]{Color.YELLOW, Color.RED, Color.BLUE});
        g.setPaint(puckGrad);
        g.fillOval((int)(puck.x - PUCK_SIZE/2), (int)(puck.y - PUCK_SIZE/2), PUCK_SIZE, PUCK_SIZE);

        // Sparkle on puck
        g.setColor(Color.WHITE);
        g.fillOval((int)(puck.x - PUCK_SIZE/4), (int)(puck.y - PUCK_SIZE/4), PUCK_SIZE/2, PUCK_SIZE/2);
    }

    private void drawRainbowHUD(Graphics2D g) {
        // Rainbow scoreboard
        for (int i = 0; i < rainbowColors.length; i++) {
            g.setColor(rainbowColors[i]);
            g.drawRoundRect(WIDTH/2 - 200 + i, 15 + i, 400 - (i*2), 100 - (i*2), 20, 20);
        }

        g.setColor(new Color(0, 0, 0, 200));
        g.fillRoundRect(WIDTH/2 - 195, 20, 390, 90, 15, 15);

        // Team names with rainbow text
        g.setFont(new Font("Arial", Font.BOLD, 20));
        for (int i = 0; i < rainbowColors.length; i++) {
            g.setColor(rainbowColors[i]);
            g.drawString("🌈 RED TEAM 🌈", WIDTH/2 - 170 + i, 50 + i);
            g.drawString("💙 BLUE TEAM 💙", WIDTH/2 + 70 + i, 50 + i);
        }

        // Scores with glow
        g.setFont(new Font("Arial", Font.BOLD, 54));
        for (int i = 0; i < 3; i++) {
            g.setColor(rainbowColors[i]);
            g.drawString(String.valueOf(scoreRed), WIDTH/2 - 110 + i, 95 + i);
            g.drawString(String.valueOf(scoreBlue), WIDTH/2 + 50 + i, 95 + i);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.drawString(":", WIDTH/2 - 25, 95);

        // Period and time with rainbow
        g.setFont(new Font("Arial", Font.BOLD, 16));
        int minutes = gameTime / 60;
        int seconds = gameTime % 60;
        String timeStr = String.format("🎨 PERIOD %d  ⏱ %02d:%02d 🎨", period, minutes, seconds);
        for (int i = 0; i < rainbowColors.length; i++) {
            g.setColor(rainbowColors[i]);
            g.drawString(timeStr, WIDTH/2 - 90 + i, 75 + i);
        }
    }

    private void drawPause(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 220));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setFont(new Font("Arial", Font.BOLD, 72));
        for (int i = 0; i < rainbowColors.length; i++) {
            g.setColor(rainbowColors[i]);
            g.drawString("⏸ RAINBOW PAUSE ⏸", WIDTH/2 - 200 + i, HEIGHT/2 + i);
        }
    }

    private void drawRainbowInstructions(Graphics2D g) {
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRoundRect(15, HEIGHT - 130, 750, 115, 10, 10);

        for (int i = 0; i < rainbowColors.length; i++) {
            g.setColor(rainbowColors[i]);
            g.drawRect(15 + i, HEIGHT - 130 + i, 750 - (i*2), 115 - (i*2));
        }

        g.setColor(Color.WHITE);
        g.drawString("🌈✨ RAINBOW CONTROLS ✨🌈", 25, HEIGHT - 115);
        g.setColor(new Color(255, 100, 100));
        g.drawString("🔴 RED TEAM: WASD - Move | SPACE - Shoot (RAINBOW BLAST!) | SHIFT - Poke Check", 25, HEIGHT - 95);
        g.setColor(new Color(100, 150, 255));
        g.drawString("🔵 BLUE TEAM: Arrow Keys - Move | ENTER - Shoot | NUMPAD0 - Poke Check", 25, HEIGHT - 75);
        g.setColor(new Color(255, 255, 100));
        g.drawString("🎨 SPECTACULAR FEATURES: Rainbow trails | Color explosions | Confetti | Sparkles!", 25, HEIGHT - 55);
        g.drawString("💫 TIPS: Speed = Rainbow Power | Shoot for colorful explosions | Every goal is a PARTY!", 25, HEIGHT - 35);
        g.drawString("⏸ P - Pause | ❌ ESC - Quit | 🎉 Score = FIREWORKS! 🎉", 25, HEIGHT - 15);
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

    // ===== INNER CLASSES WITH RAINBOW EFFECTS =====

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
        float glowIntensity = 0;

        HockeyPlayer(int x, int y, String teamColor, String playerType, int number) {
            this(x, y, teamColor, playerType, number, 0, 0, 0, 0, 0, 0);
            this.isAI = true;
        }

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
            glowIntensity = (float)(Math.sin(System.currentTimeMillis() * 0.008) * 0.5 + 0.5);

            // Rainbow aura
            for (int i = 0; i < 5; i++) {
                g.setColor(rainbowColors[(int)((System.currentTimeMillis() / 50 + i * 10) % rainbowColors.length)]);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                g.fillOval((int)(x - size/2 - i*2), (int)(y - size/2 - i*2), size + i*4, size + i*4);
            }
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

            // Player body with rainbow gradient
            Color playerColor = teamColor.equals("RED") ?
                    rainbowColors[(int)(System.currentTimeMillis() / 30) % rainbowColors.length] :
                    rainbowColors[(int)(System.currentTimeMillis() / 30 + 3) % rainbowColors.length];

            RadialGradientPaint bodyGrad = new RadialGradientPaint(
                    (float)x, (float)y, size/2,
                    new float[]{0f, 1f},
                    new Color[]{playerColor.brighter(), playerColor.darker()});
            g.setPaint(bodyGrad);
            g.fillOval((int)(x - size/2), (int)(y - size/2), size, size);

            // Jersey number
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.setColor(Color.WHITE);
            String num = String.valueOf(number);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(num, (int)x - fm.stringWidth(num)/2, (int)y + 7);

            // Rainbow helmet
            for (int i = 0; i < 3; i++) {
                g.setColor(rainbowColors[(i*2) % rainbowColors.length]);
                g.fillArc((int)(x - size/2 + i), (int)(y - size/2 + i), size - i*2, size - i*2, 0, 180);
            }

            // Rainbow stick
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
                for (int i = 0; i < 3; i++) {
                    g.setColor(rainbowColors[random.nextInt(rainbowColors.length)]);
                    g.setStroke(new BasicStroke(2));
                    g.drawOval((int)(x - size/1.3 - i*2), (int)(y - size/1.3 - i*2),
                            size + 15 + i*4, size + 15 + i*4);
                }
            }

            if (hasPuck) {
                g.setColor(new Color(255, 255, 0, 150));
                g.setStroke(new BasicStroke(3));
                g.drawOval((int)(x - size/2 - 5), (int)(y - size/2 - 5), size + 10, size + 10);
            }

            // Name tag
            g.setFont(new Font("Arial", Font.BOLD, 11));
            String name = playerType;
            int nameWidth = g.getFontMetrics().stringWidth(name);
            g.setColor(new Color(0, 0, 0, 200));
            g.fillRoundRect((int)x - nameWidth/2 - 3, (int)y - size/2 - 15, nameWidth + 6, 14, 5, 5);
            g.setColor(rainbowColors[(int)(System.currentTimeMillis() / 20) % rainbowColors.length]);
            g.drawString(name, (int)x - nameWidth/2, (int)y - size/2 - 5);
        }
    }

    class Goal {
        int x, y, width, height;
        Goal(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    class RainbowTrail {
        double x, y;
        int life = 15;
        Color color;
        RainbowTrail(double x, double y, Color color) {
            this.x = x; this.y = y;
            this.color = color;
        }
        void draw(Graphics2D g) {
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), life * 15));
            g.fillOval((int)x - 4, (int)y - 4, 8, 8);
        }
    }

    class GoalExplosion {
        double x, y, vx, vy;
        int life = 40;
        Color color;
        GoalExplosion(double x, double y, Color color) {
            this.x = x; this.y = y;
            this.color = color;
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = random.nextDouble() * 12;
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed;
        }
        void draw(Graphics2D g) {
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), life * 6));
            g.fillOval((int)x - 4, (int)y - 4, 8, 8);
        }
    }

    class ColorSpark {
        double x, y, vx, vy;
        int life = 20;
        Color color;
        ColorSpark(double x, double y, String team) {
            this.x = x;
            this.y = y;
            this.color = rainbowColors[random.nextInt(rainbowColors.length)];
            this.vx = (random.nextDouble() - 0.5) * 15;
            this.vy = (random.nextDouble() - 0.5) * 15;
        }
        void draw(Graphics2D g) {
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), life * 12));
            g.fillRect((int)x - 2, (int)y - 2, 4, 4);
        }
    }

    class Confetti {
        double x, y, vx, vy;
        int life = 60;
        Color color;
        Confetti(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.vx = (random.nextDouble() - 0.5) * 8;
            this.vy = -random.nextDouble() * 10;
        }
        void draw(Graphics2D g) {
            g.setColor(color);
            g.fillRect((int)x, (int)y, 6, 6);
        }
    }

    class Star {
        double x, y;
        float brightness;
        Star(double x, double y) {
            this.x = x;
            this.y = y;
            brightness = random.nextFloat();
        }
        void update() {
            brightness += 0.02f;
            if (brightness > 1) brightness = 0;
        }
        void draw(Graphics2D g) {
            g.setColor(new Color(255, 255, 255, (int)(brightness * 100)));
            g.fillRect((int)x, (int)y, 2, 2);
        }
    }

    public static void main(String[] args) {
        String[] options = {"🌈 PLAY VS COMPUTER (3v3)", "🎨 TWO PLAYERS (3v3)"};
        int choice = JOptionPane.showOptionDialog(null,
                "🎨🌈✨ RAINBOW ICE HOCKEY SPECTACULAR! ✨🌈🎨\n" +
                        "The MOST COLORFUL hockey game ever created!\n" +
                        "Rainbow trails | Color explosions | Confetti | Sparkles\n\n" +
                        "Select Game Mode:",
                "RAINBOW HOCKEY SPECTACULAR",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        JFrame frame = new JFrame("🌈 RAINBOW ICE HOCKEY - SPECTACULAR EDITION 🌈");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new IceHockeySimulator(choice == 0));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}