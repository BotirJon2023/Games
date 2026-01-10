import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;


public class HandballChampionship extends JPanel implements ActionListener, KeyListener {

    // Game Constants
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 700;
    private static final int FIELD_MARGIN = 50;
    private static final int GOAL_WIDTH = 20;
    private static final int GOAL_HEIGHT = 120;
    private static final int PLAYER_RADIUS = 18;
    private static final int BALL_RADIUS = 10;
    private static final int GAME_DURATION = 120; // seconds per half

    // Game States
    private enum GameState { MENU, TEAM_SELECT, PLAYING, HALFTIME, GAME_OVER, CHAMPIONSHIP_RESULTS }
    private GameState gameState = GameState.MENU;

    // Teams
    private String[] teamNames = {"Germany", "France", "Spain", "Denmark", "Sweden", "Norway", "Croatia", "Poland"};
    private Color[][] teamColors = {
            {Color.BLACK, Color.RED, Color.YELLOW},      // Germany
            {Color.BLUE, Color.WHITE, Color.RED},        // France
            {Color.RED, Color.YELLOW, Color.RED},        // Spain
            {Color.RED, Color.WHITE, Color.RED},         // Denmark
            {Color.BLUE, Color.YELLOW, Color.BLUE},      // Sweden
            {Color.RED, Color.BLUE, Color.WHITE},        // Norway
            {Color.RED, Color.WHITE, Color.BLUE},        // Croatia
            {Color.WHITE, Color.RED, Color.WHITE}        // Poland
    };

    // Championship
    private int playerTeamIndex = 0;
    private int currentOpponentIndex = 1;
    private int[] championshipPoints = new int[8];
    private int matchesPlayed = 0;
    private List<String> matchResults = new ArrayList<>();

    // Players
    private List<Player> homeTeam = new ArrayList<>();
    private List<Player> awayTeam = new ArrayList<>();
    private Player controlledPlayer;
    private Player goalkeeper1, goalkeeper2;

    // Ball
    private Ball ball;
    private Player ballHolder = null;

    // Scoring
    private int homeScore = 0;
    private int awayScore = 0;
    private int gameTime = 0;
    private int half = 1;
    private boolean isPaused = false;

    // Controls
    private Set<Integer> pressedKeys = new HashSet<>();

    // Animation
    private Timer gameTimer;
    private int animationFrame = 0;
    private List<ScoreAnimation> scoreAnimations = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();

    // Menu
    private int menuSelection = 0;
    private int teamSelectCursor = 0;

    // Field positions for formations
    private double[][] homeFormation = {
            {0.15, 0.5},  // Goalkeeper
            {0.25, 0.2}, {0.25, 0.5}, {0.25, 0.8},  // Defense
            {0.35, 0.35}, {0.35, 0.65},  // Midfield
            {0.45, 0.5}   // Forward
    };

    private double[][] awayFormation = {
            {0.85, 0.5},  // Goalkeeper
            {0.75, 0.2}, {0.75, 0.5}, {0.75, 0.8},  // Defense
            {0.65, 0.35}, {0.65, 0.65},  // Midfield
            {0.55, 0.5}   // Forward
    };

    // Inner Classes
    class Player {
        double x, y;
        double vx, vy;
        double targetX, targetY;
        Color primaryColor, secondaryColor;
        boolean isGoalkeeper;
        boolean isHomeTeam;
        int number;
        double stamina = 100;
        double shootPower = 0;
        boolean isCharging = false;
        double animOffset;

        Player(double x, double y, Color primary, Color secondary, boolean isHome, int number, boolean isGK) {
            this.x = x;
            this.y = y;
            this.targetX = x;
            this.targetY = y;
            this.primaryColor = primary;
            this.secondaryColor = secondary;
            this.isHomeTeam = isHome;
            this.number = number;
            this.isGoalkeeper = isGK;
            this.animOffset = Math.random() * Math.PI * 2;
        }

        void update() {
            // Movement towards target
            double dx = targetX - x;
            double dy = targetY - y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > 5) {
                double speed = isGoalkeeper ? 4 : 3;
                if (this == controlledPlayer && pressedKeys.contains(KeyEvent.VK_SHIFT) && stamina > 0) {
                    speed = 5;
                    stamina -= 0.5;
                }
                vx = (dx / dist) * speed;
                vy = (dy / dist) * speed;
            } else {
                vx *= 0.8;
                vy *= 0.8;
            }

            x += vx;
            y += vy;

            // Boundary constraints
            int fieldLeft = FIELD_MARGIN + (isGoalkeeper ? 0 : 30);
            int fieldRight = WIDTH - FIELD_MARGIN - (isGoalkeeper ? 0 : 30);
            int fieldTop = FIELD_MARGIN + 30;
            int fieldBottom = HEIGHT - FIELD_MARGIN - 30;

            x = Math.max(fieldLeft, Math.min(fieldRight, x));
            y = Math.max(fieldTop, Math.min(fieldBottom, y));

            // Stamina recovery
            if (stamina < 100) stamina += 0.1;

            // Shooting charge
            if (isCharging && shootPower < 100) {
                shootPower += 2;
            }
        }

        void draw(Graphics2D g2d, int frame) {
            // Shadow
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillOval((int)x - PLAYER_RADIUS + 3, (int)y - PLAYER_RADIUS + 5,
                    PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);

            // Body animation
            double bounce = Math.sin(frame * 0.2 + animOffset) * 2;
            int drawY = (int)(y + bounce);

            // Player body
            g2d.setColor(primaryColor);
            g2d.fillOval((int)x - PLAYER_RADIUS, drawY - PLAYER_RADIUS,
                    PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);

            // Jersey stripe
            g2d.setColor(secondaryColor);
            g2d.fillRect((int)x - 5, drawY - PLAYER_RADIUS + 5, 10, PLAYER_RADIUS);

            // Outline
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval((int)x - PLAYER_RADIUS, drawY - PLAYER_RADIUS,
                    PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);

            // Number
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String numStr = String.valueOf(number);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(numStr, (int)x - fm.stringWidth(numStr)/2, drawY + 5);

            // Selection indicator for controlled player
            if (this == controlledPlayer) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval((int)x - PLAYER_RADIUS - 5, drawY - PLAYER_RADIUS - 5,
                        PLAYER_RADIUS * 2 + 10, PLAYER_RADIUS * 2 + 10);

                // Arrow indicator
                int arrowY = drawY - PLAYER_RADIUS - 15;
                int[] xPoints = {(int)x - 8, (int)x, (int)x + 8};
                int[] yPoints = {arrowY - 10, arrowY, arrowY - 10};
                g2d.fillPolygon(xPoints, yPoints, 3);
            }

            // Power bar when charging
            if (isCharging && shootPower > 0) {
                g2d.setColor(Color.DARK_GRAY);
                g2d.fillRect((int)x - 25, drawY - PLAYER_RADIUS - 25, 50, 8);
                g2d.setColor(shootPower < 50 ? Color.GREEN : shootPower < 80 ? Color.YELLOW : Color.RED);
                g2d.fillRect((int)x - 25, drawY - PLAYER_RADIUS - 25, (int)(shootPower / 2), 8);
                g2d.setColor(Color.WHITE);
                g2d.drawRect((int)x - 25, drawY - PLAYER_RADIUS - 25, 50, 8);
            }

            // Stamina bar for controlled player
            if (this == controlledPlayer) {
                g2d.setColor(Color.DARK_GRAY);
                g2d.fillRect((int)x - 20, drawY + PLAYER_RADIUS + 5, 40, 5);
                g2d.setColor(stamina > 50 ? Color.GREEN : stamina > 25 ? Color.ORANGE : Color.RED);
                g2d.fillRect((int)x - 20, drawY + PLAYER_RADIUS + 5, (int)(stamina * 0.4), 5);
            }
        }
    }

    class Ball {
        double x, y;
        double vx, vy;
        double z = 0; // Height for 3D effect
        double vz = 0;
        double rotation = 0;

        Ball(double x, double y) {
            this.x = x;
            this.y = y;
        }

        void update() {
            x += vx;
            y += vy;
            z += vz;

            // Gravity
            if (z > 0 || vz > 0) {
                vz -= 0.5;
                if (z < 0) {
                    z = 0;
                    vz = -vz * 0.5;
                    if (Math.abs(vz) < 1) vz = 0;
                }
            }

            // Friction
            vx *= 0.98;
            vy *= 0.98;

            // Rotation
            rotation += Math.sqrt(vx * vx + vy * vy) * 0.1;

            // Boundary bounce
            if (x < FIELD_MARGIN + BALL_RADIUS) {
                x = FIELD_MARGIN + BALL_RADIUS;
                vx = -vx * 0.7;
            }
            if (x > WIDTH - FIELD_MARGIN - BALL_RADIUS) {
                x = WIDTH - FIELD_MARGIN - BALL_RADIUS;
                vx = -vx * 0.7;
            }
            if (y < FIELD_MARGIN + BALL_RADIUS) {
                y = FIELD_MARGIN + BALL_RADIUS;
                vy = -vy * 0.7;
            }
            if (y > HEIGHT - FIELD_MARGIN - BALL_RADIUS) {
                y = HEIGHT - FIELD_MARGIN - BALL_RADIUS;
                vy = -vy * 0.7;
            }
        }

        void draw(Graphics2D g2d) {
            int drawY = (int)(y - z);
            int size = BALL_RADIUS * 2 + (int)(z / 10);

            // Shadow
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillOval((int)x - BALL_RADIUS + 2, (int)y - BALL_RADIUS + 2,
                    BALL_RADIUS * 2, BALL_RADIUS * 2);

            // Ball
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int)x - size/2, drawY - size/2, size, size);

            // Pattern
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawOval((int)x - size/2, drawY - size/2, size, size);

            // Rotating pattern
            double patternX = x + Math.cos(rotation) * size/4;
            double patternY = drawY + Math.sin(rotation) * size/4;
            g2d.fillOval((int)patternX - 3, (int)patternY - 3, 6, 6);
        }
    }

    class ScoreAnimation {
        String text;
        double x, y;
        double alpha = 1.0;
        int life = 60;
        Color color;

        ScoreAnimation(String text, double x, double y, Color color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
        }

        void update() {
            y -= 2;
            life--;
            alpha = life / 60.0;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255)));
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            g2d.drawString(text, (int)x, (int)y);
        }
    }

    class Particle {
        double x, y, vx, vy;
        Color color;
        int life = 30;

        Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.vx = (Math.random() - 0.5) * 10;
            this.vy = (Math.random() - 0.5) * 10;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.3;
            life--;
        }

        void draw(Graphics2D g2d) {
            float alpha = life / 30.0f;
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255)));
            g2d.fillOval((int)x - 3, (int)y - 3, 6, 6);
        }
    }

    // Constructor
    public HandballChampionship() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(60, 120, 60));
        setFocusable(true);
        addKeyListener(this);

        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();
    }

    private void initGame() {
        homeTeam.clear();
        awayTeam.clear();
        particles.clear();
        scoreAnimations.clear();

        Color[] homeColors = teamColors[playerTeamIndex];
        Color[] awayColors = teamColors[currentOpponentIndex];

        // Create home team
        for (int i = 0; i < 7; i++) {
            double x = homeFormation[i][0] * (WIDTH - 2 * FIELD_MARGIN) + FIELD_MARGIN;
            double y = homeFormation[i][1] * (HEIGHT - 2 * FIELD_MARGIN) + FIELD_MARGIN;
            Player p = new Player(x, y, homeColors[0], homeColors[1], true, i + 1, i == 0);
            homeTeam.add(p);
            if (i == 0) goalkeeper1 = p;
        }

        // Create away team
        for (int i = 0; i < 7; i++) {
            double x = awayFormation[i][0] * (WIDTH - 2 * FIELD_MARGIN) + FIELD_MARGIN;
            double y = awayFormation[i][1] * (HEIGHT - 2 * FIELD_MARGIN) + FIELD_MARGIN;
            Player p = new Player(x, y, awayColors[0], awayColors[1], false, i + 1, i == 0);
            awayTeam.add(p);
            if (i == 0) goalkeeper2 = p;
        }

        controlledPlayer = homeTeam.get(6); // Start with forward

        // Initialize ball
        ball = new Ball(WIDTH / 2, HEIGHT / 2);
        ballHolder = null;

        homeScore = 0;
        awayScore = 0;
        gameTime = 0;
        half = 1;
    }

    private void resetPositions() {
        for (int i = 0; i < 7; i++) {
            double x = homeFormation[i][0] * (WIDTH - 2 * FIELD_MARGIN) + FIELD_MARGIN;
            double y = homeFormation[i][1] * (HEIGHT - 2 * FIELD_MARGIN) + FIELD_MARGIN;
            homeTeam.get(i).x = x;
            homeTeam.get(i).y = y;
            homeTeam.get(i).targetX = x;
            homeTeam.get(i).targetY = y;
        }

        for (int i = 0; i < 7; i++) {
            double x = awayFormation[i][0] * (WIDTH - 2 * FIELD_MARGIN) + FIELD_MARGIN;
            double y = awayFormation[i][1] * (HEIGHT - 2 * FIELD_MARGIN) + FIELD_MARGIN;
            awayTeam.get(i).x = x;
            awayTeam.get(i).y = y;
            awayTeam.get(i).targetX = x;
            awayTeam.get(i).targetY = y;
        }

        ball.x = WIDTH / 2;
        ball.y = HEIGHT / 2;
        ball.vx = 0;
        ball.vy = 0;
        ball.z = 0;
        ball.vz = 0;
        ballHolder = null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        animationFrame++;

        switch (gameState) {
            case PLAYING:
                if (!isPaused) {
                    updateGame();
                }
                break;
            case MENU:
            case TEAM_SELECT:
            case HALFTIME:
            case GAME_OVER:
            case CHAMPIONSHIP_RESULTS:
                // Animation updates only
                break;
        }

        repaint();
    }

    private void updateGame() {
        // Update game time
        if (animationFrame % 60 == 0) {
            gameTime++;
            if (gameTime >= GAME_DURATION) {
                if (half == 1) {
                    half = 2;
                    gameTime = 0;
                    gameState = GameState.HALFTIME;
                    swapSides();
                } else {
                    endMatch();
                }
            }
        }

        // Handle player input
        handleInput();

        // Update all players
        for (Player p : homeTeam) p.update();
        for (Player p : awayTeam) p.update();

        // AI for away team
        updateAI();

        // AI for home team (except controlled player)
        updateHomeTeamAI();

        // Update ball
        if (ballHolder != null) {
            ball.x = ballHolder.x + (ballHolder.isHomeTeam ? 15 : -15);
            ball.y = ballHolder.y;
            ball.vx = 0;
            ball.vy = 0;
        } else {
            ball.update();
            checkBallPickup();
        }

        // Check for goals
        checkGoals();

        // Goalkeeper AI
        updateGoalkeepers();

        // Update particles
        particles.removeIf(p -> {
            p.update();
            return p.life <= 0;
        });

        // Update score animations
        scoreAnimations.removeIf(s -> {
            s.update();
            return s.life <= 0;
        });
    }

    private void handleInput() {
        if (controlledPlayer == null) return;

        double moveX = 0, moveY = 0;

        if (pressedKeys.contains(KeyEvent.VK_W) || pressedKeys.contains(KeyEvent.VK_UP)) moveY = -1;
        if (pressedKeys.contains(KeyEvent.VK_S) || pressedKeys.contains(KeyEvent.VK_DOWN)) moveY = 1;
        if (pressedKeys.contains(KeyEvent.VK_A) || pressedKeys.contains(KeyEvent.VK_LEFT)) moveX = -1;
        if (pressedKeys.contains(KeyEvent.VK_D) || pressedKeys.contains(KeyEvent.VK_RIGHT)) moveX = 1;

        if (moveX != 0 || moveY != 0) {
            double length = Math.sqrt(moveX * moveX + moveY * moveY);
            moveX /= length;
            moveY /= length;

            double speed = 60;
            controlledPlayer.targetX = controlledPlayer.x + moveX * speed;
            controlledPlayer.targetY = controlledPlayer.y + moveY * speed;
        } else {
            controlledPlayer.targetX = controlledPlayer.x;
            controlledPlayer.targetY = controlledPlayer.y;
        }
    }

    private void updateAI() {
        for (int i = 1; i < awayTeam.size(); i++) {
            Player p = awayTeam.get(i);

            if (ballHolder == p) {
                // Move towards goal and shoot
                double goalX = FIELD_MARGIN + GOAL_WIDTH;
                double goalY = HEIGHT / 2;

                if (p.x < 300) {
                    // Shoot!
                    shootBall(p, goalX, goalY + (Math.random() - 0.5) * GOAL_HEIGHT);
                } else {
                    p.targetX = goalX + 100;
                    p.targetY = goalY + (Math.random() - 0.5) * 100;
                }
            } else if (ballHolder != null && ballHolder.isHomeTeam) {
                // Defend
                double defX = awayFormation[i][0] * (WIDTH - 2 * FIELD_MARGIN) + FIELD_MARGIN;
                double defY = awayFormation[i][1] * (HEIGHT - 2 * FIELD_MARGIN) + FIELD_MARGIN;

                // Move towards ball holder
                double toBallX = ballHolder.x - p.x;
                double toBallY = ballHolder.y - p.y;
                double dist = Math.sqrt(toBallX * toBallX + toBallY * toBallY);

                if (dist < 200) {
                    p.targetX = ballHolder.x;
                    p.targetY = ballHolder.y;
                } else {
                    p.targetX = defX - 50;
                    p.targetY = defY;
                }
            } else if (ballHolder != null && !ballHolder.isHomeTeam) {
                // Support attack
                if (ballHolder == p) continue;

                double supportX = ballHolder.x - 80 + (Math.random() - 0.5) * 100;
                double supportY = ballHolder.y + (i - 3) * 80;
                p.targetX = Math.max(200, Math.min(WIDTH - 200, supportX));
                p.targetY = Math.max(100, Math.min(HEIGHT - 100, supportY));
            } else {
                // Go for loose ball
                p.targetX = ball.x;
                p.targetY = ball.y;
            }
        }
    }

    private void updateHomeTeamAI() {
        for (int i = 1; i < homeTeam.size(); i++) {
            Player p = homeTeam.get(i);
            if (p == controlledPlayer) continue;

            if (ballHolder != null && ballHolder.isHomeTeam) {
                // Support attack
                if (ballHolder == controlledPlayer) {
                    double supportX = controlledPlayer.x + 80 + (i - 3) * 30;
                    double supportY = controlledPlayer.y + (i - 3) * 60;
                    p.targetX = Math.max(200, Math.min(WIDTH - 200, supportX));
                    p.targetY = Math.max(100, Math.min(HEIGHT - 100, supportY));
                }
            } else if (ballHolder != null && !ballHolder.isHomeTeam) {
                // Defend
                double defX = homeFormation[i][0] * (WIDTH - 2 * FIELD_MARGIN) + FIELD_MARGIN;
                double defY = homeFormation[i][1] * (HEIGHT - 2 * FIELD_MARGIN) + FIELD_MARGIN;

                double toBallX = ballHolder.x - p.x;
                double toBallY = ballHolder.y - p.y;
                double dist = Math.sqrt(toBallX * toBallX + toBallY * toBallY);

                if (dist < 150) {
                    p.targetX = ballHolder.x;
                    p.targetY = ballHolder.y;
                } else {
                    p.targetX = defX + 50;
                    p.targetY = defY;
                }
            } else {
                // Go for loose ball (but let controlled player have priority)
                double distToBall = Math.sqrt(Math.pow(p.x - ball.x, 2) + Math.pow(p.y - ball.y, 2));
                double controlledDist = Math.sqrt(Math.pow(controlledPlayer.x - ball.x, 2) +
                        Math.pow(controlledPlayer.y - ball.y, 2));

                if (distToBall < controlledDist - 50) {
                    p.targetX = ball.x;
                    p.targetY = ball.y;
                }
            }
        }
    }

    private void updateGoalkeepers() {
        // Home goalkeeper
        double ballDistToGoal1 = ball.x - FIELD_MARGIN;
        if (ballDistToGoal1 < 300) {
            goalkeeper1.targetY = ball.y;
            goalkeeper1.targetX = FIELD_MARGIN + 40;
        } else {
            goalkeeper1.targetY = HEIGHT / 2;
            goalkeeper1.targetX = FIELD_MARGIN + 30;
        }

        // Away goalkeeper
        double ballDistToGoal2 = WIDTH - FIELD_MARGIN - ball.x;
        if (ballDistToGoal2 < 300) {
            goalkeeper2.targetY = ball.y;
            goalkeeper2.targetX = WIDTH - FIELD_MARGIN - 40;
        } else {
            goalkeeper2.targetY = HEIGHT / 2;
            goalkeeper2.targetX = WIDTH - FIELD_MARGIN - 30;
        }
    }

    private void checkBallPickup() {
        // Check all players for ball pickup
        List<Player> allPlayers = new ArrayList<>();
        allPlayers.addAll(homeTeam);
        allPlayers.addAll(awayTeam);

        for (Player p : allPlayers) {
            double dist = Math.sqrt(Math.pow(p.x - ball.x, 2) + Math.pow(p.y - ball.y, 2));
            if (dist < PLAYER_RADIUS + BALL_RADIUS && ball.z < 20) {
                ballHolder = p;
                break;
            }
        }
    }

    private void checkGoals() {
        // Home goal (left side)
        if (ball.x < FIELD_MARGIN + GOAL_WIDTH &&
                ball.y > HEIGHT/2 - GOAL_HEIGHT/2 &&
                ball.y < HEIGHT/2 + GOAL_HEIGHT/2 &&
                ball.z < 30) {
            awayScore++;
            scoreAnimations.add(new ScoreAnimation("GOAL!", WIDTH/2 - 50, HEIGHT/2, Color.RED));
            createGoalParticles(FIELD_MARGIN, HEIGHT/2);
            resetPositions();
        }

        // Away goal (right side)
        if (ball.x > WIDTH - FIELD_MARGIN - GOAL_WIDTH &&
                ball.y > HEIGHT/2 - GOAL_HEIGHT/2 &&
                ball.y < HEIGHT/2 + GOAL_HEIGHT/2 &&
                ball.z < 30) {
            homeScore++;
            scoreAnimations.add(new ScoreAnimation("GOAL!", WIDTH/2 - 50, HEIGHT/2, Color.GREEN));
            createGoalParticles(WIDTH - FIELD_MARGIN, HEIGHT/2);
            resetPositions();
        }
    }

    private void createGoalParticles(double x, double y) {
        for (int i = 0; i < 50; i++) {
            particles.add(new Particle(x, y, new Color(
                    (int)(Math.random() * 255),
                    (int)(Math.random() * 255),
                    (int)(Math.random() * 255)
            )));
        }
    }

    private void shootBall(Player shooter, double targetX, double targetY) {
        double dx = targetX - shooter.x;
        double dy = targetY - shooter.y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        double power = shooter.isCharging ? shooter.shootPower / 10 : 8;

        ball.vx = (dx / dist) * power;
        ball.vy = (dy / dist) * power;
        ball.vz = 3 + Math.random() * 3;
        ballHolder = null;

        shooter.shootPower = 0;
        shooter.isCharging = false;
    }

    private void passBall(Player passer) {
        // Find nearest teammate
        List<Player> teammates = passer.isHomeTeam ? homeTeam : awayTeam;
        Player nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Player p : teammates) {
            if (p == passer) continue;
            double dist = Math.sqrt(Math.pow(p.x - passer.x, 2) + Math.pow(p.y - passer.y, 2));
            if (dist < minDist) {
                minDist = dist;
                nearest = p;
            }
        }

        if (nearest != null) {
            shootBall(passer, nearest.x, nearest.y);
        }
    }

    private void switchPlayer() {
        if (ballHolder != null && ballHolder.isHomeTeam) {
            controlledPlayer = ballHolder;
            return;
        }

        // Find nearest home team player to ball
        Player nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Player p : homeTeam) {
            if (p.isGoalkeeper) continue;
            double dist = Math.sqrt(Math.pow(p.x - ball.x, 2) + Math.pow(p.y - ball.y, 2));
            if (dist < minDist) {
                minDist = dist;
                nearest = p;
            }
        }

        if (nearest != null) {
            controlledPlayer = nearest;
        }
    }

    private void swapSides() {
        // Swap formations
        double[][] temp = homeFormation;
        homeFormation = mirrorFormation(awayFormation);
        awayFormation = mirrorFormation(temp);
    }

    private double[][] mirrorFormation(double[][] formation) {
        double[][] mirrored = new double[formation.length][2];
        for (int i = 0; i < formation.length; i++) {
            mirrored[i][0] = 1.0 - formation[i][0];
            mirrored[i][1] = formation[i][1];
        }
        return mirrored;
    }

    private void endMatch() {
        gameState = GameState.GAME_OVER;

        // Update championship standings
        if (homeScore > awayScore) {
            championshipPoints[playerTeamIndex] += 2;
            matchResults.add(teamNames[playerTeamIndex] + " " + homeScore + " - " + awayScore + " " + teamNames[currentOpponentIndex] + " (WIN)");
        } else if (awayScore > homeScore) {
            championshipPoints[currentOpponentIndex] += 2;
            matchResults.add(teamNames[playerTeamIndex] + " " + homeScore + " - " + awayScore + " " + teamNames[currentOpponentIndex] + " (LOSS)");
        } else {
            championshipPoints[playerTeamIndex] += 1;
            championshipPoints[currentOpponentIndex] += 1;
            matchResults.add(teamNames[playerTeamIndex] + " " + homeScore + " - " + awayScore + " " + teamNames[currentOpponentIndex] + " (DRAW)");
        }

        matchesPlayed++;
    }

    private void nextMatch() {
        if (matchesPlayed >= 7) {
            gameState = GameState.CHAMPIONSHIP_RESULTS;
            return;
        }

        // Find next opponent
        do {
            currentOpponentIndex = (currentOpponentIndex + 1) % 8;
        } while (currentOpponentIndex == playerTeamIndex);

        initGame();
        gameState = GameState.PLAYING;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (gameState) {
            case MENU:
                drawMenu(g2d);
                break;
            case TEAM_SELECT:
                drawTeamSelect(g2d);
                break;
            case PLAYING:
                drawGame(g2d);
                break;
            case HALFTIME:
                drawGame(g2d);
                drawHalftime(g2d);
                break;
            case GAME_OVER:
                drawGame(g2d);
                drawGameOver(g2d);
                break;
            case CHAMPIONSHIP_RESULTS:
                drawChampionshipResults(g2d);
                break;
        }
    }

    private void drawMenu(Graphics2D g2d) {
        // Background
        GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 60, 100),
                WIDTH, HEIGHT, new Color(40, 80, 120));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String title = "HANDBALL CHAMPIONSHIP";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, WIDTH/2 - fm.stringWidth(title)/2, 150);

        // Animated ball
        double ballX = WIDTH/2 + Math.sin(animationFrame * 0.05) * 100;
        double ballY = 220 + Math.abs(Math.sin(animationFrame * 0.1)) * 30;
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int)ballX - 25, (int)ballY - 25, 50, 50);
        g2d.setColor(Color.BLACK);
        g2d.drawOval((int)ballX - 25, (int)ballY - 25, 50, 50);

        // Menu options
        String[] options = {"START CHAMPIONSHIP", "QUICK MATCH", "EXIT"};
        g2d.setFont(new Font("Arial", Font.BOLD, 30));

        for (int i = 0; i < options.length; i++) {
            if (i == menuSelection) {
                g2d.setColor(Color.YELLOW);
                g2d.drawString("► " + options[i], WIDTH/2 - 150, 350 + i * 60);
            } else {
                g2d.setColor(Color.WHITE);
                g2d.drawString("  " + options[i], WIDTH/2 - 150, 350 + i * 60);
            }
        }

        // Controls info
        g2d.setColor(new Color(200, 200, 200));
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("Controls: WASD - Move | SPACE - Shoot/Pass | SHIFT - Sprint | E - Switch Player",
                WIDTH/2 - 300, HEIGHT - 50);
    }

    private void drawTeamSelect(Graphics2D g2d) {
        // Background
        g2d.setColor(new Color(30, 50, 80));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.drawString("SELECT YOUR TEAM", WIDTH/2 - 180, 80);

        // Team grid
        for (int i = 0; i < 8; i++) {
            int row = i / 4;
            int col = i % 4;
            int x = 100 + col * 270;
            int y = 150 + row * 200;

            // Team box
            if (i == teamSelectCursor) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(4));
                g2d.drawRoundRect(x - 10, y - 10, 240, 170, 20, 20);
            }

            // Team colors preview
            g2d.setColor(teamColors[i][0]);
            g2d.fillRoundRect(x, y, 220, 100, 15, 15);
            g2d.setColor(teamColors[i][1]);
            g2d.fillRect(x + 90, y + 20, 40, 60);

            // Team name
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(teamNames[i], x + 110 - fm.stringWidth(teamNames[i])/2, y + 140);
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Use Arrow Keys to select, ENTER to confirm", WIDTH/2 - 180, HEIGHT - 50);
    }

    private void drawGame(Graphics2D g2d) {
        // Field background
        g2d.setColor(new Color(180, 140, 100)); // Handball court color
        g2d.fillRect(FIELD_MARGIN, FIELD_MARGIN, WIDTH - 2 * FIELD_MARGIN, HEIGHT - 2 * FIELD_MARGIN);

        // Field lines
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));

        // Outer boundary
        g2d.drawRect(FIELD_MARGIN, FIELD_MARGIN, WIDTH - 2 * FIELD_MARGIN, HEIGHT - 2 * FIELD_MARGIN);

        // Center line
        g2d.drawLine(WIDTH/2, FIELD_MARGIN, WIDTH/2, HEIGHT - FIELD_MARGIN);

        // Center circle
        g2d.drawOval(WIDTH/2 - 60, HEIGHT/2 - 60, 120, 120);

        // Goal areas (6m line - semicircle)
        g2d.setColor(new Color(100, 80, 60));
        g2d.fillArc(FIELD_MARGIN - 80, HEIGHT/2 - 100, 200, 200, -90, 180);
        g2d.fillArc(WIDTH - FIELD_MARGIN - 120, HEIGHT/2 - 100, 200, 200, 90, 180);

        // 6m line
        g2d.setColor(Color.WHITE);
        g2d.drawArc(FIELD_MARGIN - 80, HEIGHT/2 - 100, 200, 200, -90, 180);
        g2d.drawArc(WIDTH - FIELD_MARGIN - 120, HEIGHT/2 - 100, 200, 200, 90, 180);

        // 9m line (dashed)
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{10}, 0));
        g2d.drawArc(FIELD_MARGIN - 130, HEIGHT/2 - 150, 300, 300, -90, 180);
        g2d.drawArc(WIDTH - FIELD_MARGIN - 170, HEIGHT/2 - 150, 300, 300, 90, 180);

        // Goals
        g2d.setStroke(new BasicStroke(5));
        g2d.setColor(Color.RED);
        g2d.drawRect(FIELD_MARGIN - GOAL_WIDTH, HEIGHT/2 - GOAL_HEIGHT/2, GOAL_WIDTH, GOAL_HEIGHT);
        g2d.drawRect(WIDTH - FIELD_MARGIN, HEIGHT/2 - GOAL_HEIGHT/2, GOAL_WIDTH, GOAL_HEIGHT);

        // Goal nets (simple representation)
        g2d.setColor(new Color(200, 200, 200, 100));
        g2d.fillRect(FIELD_MARGIN - GOAL_WIDTH + 2, HEIGHT/2 - GOAL_HEIGHT/2 + 2, GOAL_WIDTH - 4, GOAL_HEIGHT - 4);
        g2d.fillRect(WIDTH - FIELD_MARGIN + 2, HEIGHT/2 - GOAL_HEIGHT/2 + 2, GOAL_WIDTH - 4, GOAL_HEIGHT - 4);

        // Draw players
        for (Player p : homeTeam) p.draw(g2d, animationFrame);
        for (Player p : awayTeam) p.draw(g2d, animationFrame);

        // Draw ball
        if (ball != null) ball.draw(g2d);

        // Draw particles
        for (Particle p : particles) p.draw(g2d);

        // Draw score animations
        for (ScoreAnimation s : scoreAnimations) s.draw(g2d);

        // HUD
        drawHUD(g2d);
    }

    private void drawHUD(Graphics2D g2d) {
        // Score panel
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(WIDTH/2 - 200, 5, 400, 40, 10, 10);

        // Team names and scores
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(teamColors[playerTeamIndex][0]);
        g2d.drawString(teamNames[playerTeamIndex], WIDTH/2 - 180, 32);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.drawString(homeScore + " - " + awayScore, WIDTH/2 - 30, 35);

        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(teamColors[currentOpponentIndex][0]);
        String oppName = teamNames[currentOpponentIndex];
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(oppName, WIDTH/2 + 180 - fm.stringWidth(oppName), 32);

        // Time
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(10, 5, 100, 40, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        int minutes = gameTime / 60;
        int seconds = gameTime % 60;
        String timeStr = String.format("%d:%02d", minutes + (half - 1) * 2, seconds);
        g2d.drawString("Half " + half, 20, 22);
        g2d.drawString(timeStr, 20, 40);

        // Match info (championship)
        if (matchesPlayed > 0) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRoundRect(WIDTH - 150, 5, 140, 40, 10, 10);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            g2d.drawString("Match " + (matchesPlayed + 1) + "/7", WIDTH - 140, 22);
            g2d.drawString("Points: " + championshipPoints[playerTeamIndex], WIDTH - 140, 40);
        }

        // Pause indicator
        if (isPaused) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 60));
            g2d.drawString("PAUSED", WIDTH/2 - 120, HEIGHT/2);
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            g2d.drawString("Press P to continue", WIDTH/2 - 100, HEIGHT/2 + 50);
        }
    }

    private void drawHalftime(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(WIDTH/2 - 200, HEIGHT/2 - 100, 400, 200);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.drawString("HALFTIME", WIDTH/2 - 100, HEIGHT/2 - 30);

        g2d.setFont(new Font("Arial", Font.BOLD, 50));
        g2d.drawString(homeScore + " - " + awayScore, WIDTH/2 - 50, HEIGHT/2 + 40);

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Press ENTER to continue", WIDTH/2 - 110, HEIGHT/2 + 80);
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(WIDTH/2 - 250, HEIGHT/2 - 150, 500, 300);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.drawString("FULL TIME", WIDTH/2 - 100, HEIGHT/2 - 80);

        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        g2d.drawString(homeScore + " - " + awayScore, WIDTH/2 - 60, HEIGHT/2);

        String result;
        Color resultColor;
        if (homeScore > awayScore) {
            result = "VICTORY!";
            resultColor = Color.GREEN;
        } else if (awayScore > homeScore) {
            result = "DEFEAT";
            resultColor = Color.RED;
        } else {
            result = "DRAW";
            resultColor = Color.YELLOW;
        }

        g2d.setColor(resultColor);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(result, WIDTH/2 - fm.stringWidth(result)/2, HEIGHT/2 + 60);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        if (matchesPlayed < 7) {
            g2d.drawString("Press ENTER for next match", WIDTH/2 - 130, HEIGHT/2 + 120);
        } else {
            g2d.drawString("Press ENTER for championship results", WIDTH/2 - 160, HEIGHT/2 + 120);
        }
    }

    private void drawChampionshipResults(Graphics2D g2d) {
        // Background
        GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 40, 80),
                WIDTH, HEIGHT, new Color(40, 60, 100));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 50));
        g2d.drawString("CHAMPIONSHIP RESULTS", WIDTH/2 - 280, 70);

        // Sort teams by points
        Integer[] indices = new Integer[8];
        for (int i = 0; i < 8; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> championshipPoints[b] - championshipPoints[a]);

        // Standings table
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        g2d.drawString("FINAL STANDINGS", 100, 130);

        for (int i = 0; i < 8; i++) {
            int teamIdx = indices[i];
            int y = 170 + i * 40;

            if (teamIdx == playerTeamIndex) {
                g2d.setColor(new Color(255, 255, 0, 100));
                g2d.fillRect(80, y - 25, 350, 35);
            }

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 22));
            g2d.drawString((i + 1) + ".", 100, y);

            g2d.setColor(teamColors[teamIdx][0]);
            g2d.fillOval(130, y - 18, 24, 24);

            g2d.setColor(Color.WHITE);
            g2d.drawString(teamNames[teamIdx], 165, y);
            g2d.drawString(championshipPoints[teamIdx] + " pts", 350, y);
        }

        // Match results
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("YOUR MATCHES", 550, 130);

        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        for (int i = 0; i < matchResults.size(); i++) {
            g2d.drawString(matchResults.get(i), 550, 170 + i * 35);
        }

        // Winner announcement
        if (indices[0] == playerTeamIndex) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            g2d.drawString("🏆 CHAMPION! 🏆", WIDTH/2 - 150, HEIGHT - 80);
        }

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Press ENTER to return to menu", WIDTH/2 - 140, HEIGHT - 30);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys.add(e.getKeyCode());

        switch (gameState) {
            case MENU:
                handleMenuInput(e);
                break;
            case TEAM_SELECT:
                handleTeamSelectInput(e);
                break;
            case PLAYING:
                handleGameInput(e);
                break;
            case HALFTIME:
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    resetPositions();
                    gameState = GameState.PLAYING;
                }
                break;
            case GAME_OVER:
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    nextMatch();
                }
                break;
            case CHAMPIONSHIP_RESULTS:
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    resetChampionship();
                    gameState = GameState.MENU;
                }
                break;
        }
    }

    private void handleMenuInput(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                menuSelection = (menuSelection - 1 + 3) % 3;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                menuSelection = (menuSelection + 1) % 3;
                break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                if (menuSelection == 0) {
                    gameState = GameState.TEAM_SELECT;
                } else if (menuSelection == 1) {
                    playerTeamIndex = 0;
                    currentOpponentIndex = 1;
                    initGame();
                    gameState = GameState.PLAYING;
                } else {
                    System.exit(0);
                }
                break;
        }
    }

    private void handleTeamSelectInput(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                teamSelectCursor = (teamSelectCursor - 4 + 8) % 8;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                teamSelectCursor = (teamSelectCursor + 4) % 8;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                teamSelectCursor = (teamSelectCursor - 1 + 8) % 8;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                teamSelectCursor = (teamSelectCursor + 1) % 8;
                break;
            case KeyEvent.VK_ENTER:
                playerTeamIndex = teamSelectCursor;
                currentOpponentIndex = (playerTeamIndex + 1) % 8;
                resetChampionship();
                initGame();
                gameState = GameState.PLAYING;
                break;
            case KeyEvent.VK_ESCAPE:
                gameState = GameState.MENU;
                break;
        }
    }

    private void handleGameInput(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE:
                if (ballHolder == controlledPlayer) {
                    controlledPlayer.isCharging = true;
                }
                break;
            case KeyEvent.VK_E:
                switchPlayer();
                break;
            case KeyEvent.VK_P:
                isPaused = !isPaused;
                break;
            case KeyEvent.VK_ESCAPE:
                gameState = GameState.MENU;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());

        if (e.getKeyCode() == KeyEvent.VK_SPACE && gameState == GameState.PLAYING) {
            if (ballHolder == controlledPlayer) {
                if (controlledPlayer.shootPower > 30) {
                    // Powerful shot towards goal
                    double goalX = controlledPlayer.isHomeTeam ? WIDTH - FIELD_MARGIN : FIELD_MARGIN;
                    double goalY = HEIGHT / 2 + (Math.random() - 0.5) * GOAL_HEIGHT * 0.8;
                    shootBall(controlledPlayer, goalX, goalY);
                } else {
                    // Quick pass
                    passBall(controlledPlayer);
                }
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void resetChampionship() {
        Arrays.fill(championshipPoints, 0);
        matchesPlayed = 0;
        matchResults.clear();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Handball Championship");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            HandballChampionship game = new HandballChampionship();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}