import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.Timer;

public class IceHockeySimulator extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 800;
    private static final int GOAL_WIDTH = 200;
    private static final int GOAL_Y = HEIGHT/2 - GOAL_WIDTH/2;
    private static final int PUCK_SIZE = 14;
    private static final int PLAYER_SIZE = 40;
    private static final int MAX_SPEED = 10;
    private static final double FRICTION = 0.98;
    private static final double PUCK_FRICTION = 0.995;

    // Game objects
    private Puck puck;
    private HockeyPlayer player1, player2;
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

    // Input states
    private Set<Integer> pressedKeys = new HashSet<>();

    // AI variables
    private Random random = new Random();
    private int aiDecisionDelay = 0;
    private int aiStickCheck = 0;

    public IceHockeySimulator(boolean vsComputer) {
        this.againstComputer = vsComputer;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(200, 220, 255));
        setFocusable(true);
        addKeyListener(this);

        // Initialize game objects
        puck = new Puck(WIDTH/2, HEIGHT/2);

        // Create realistic hockey players
        player1 = new HockeyPlayer(120, HEIGHT/2, "RED", "Player 1",
                KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,
                KeyEvent.VK_SPACE, KeyEvent.VK_SHIFT);

        if (againstComputer) {
            player2 = new HockeyPlayer(WIDTH - 120, HEIGHT/2, "BLUE", "Computer",
                    0, 0, 0, 0, 0, 0);
            player2.isAI = true;
        } else {
            player2 = new HockeyPlayer(WIDTH - 120, HEIGHT/2, "BLUE", "Player 2",
                    KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                    KeyEvent.VK_ENTER, KeyEvent.VK_NUMPAD0);
        }

        goal1 = new Goal(15, GOAL_Y, 12, GOAL_WIDTH);
        goal2 = new Goal(WIDTH - 27, GOAL_Y, 12, GOAL_WIDTH);

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

    private void nextPeriod() {
        if (period < 3) {
            period++;
            gameTime = 20 * 60;
            resetPosition();
            showMessage("Period " + period + " Started!", 2000);
        } else {
            endGame();
        }
    }

    private void endGame() {
        gameRunning = false;
        gameLoop.stop();
        gameClock.stop();
        String winner = score1 > score2 ? "Player 1 Wins!" :
                (score2 > score1 ? "Player 2 Wins!" : "Tie Game!");
        JOptionPane.showMessageDialog(this,
                "🏆 GAME OVER 🏆\n" + winner + "\nFinal Score: " + score1 + " - " + score2,
                "Game Over", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    private void showMessage(String msg, int duration) {
        final JLabel message = new JLabel(msg, SwingConstants.CENTER);
        message.setFont(new Font("Arial", Font.BOLD, 48));
        message.setForeground(Color.RED);
        message.setBounds(WIDTH/2 - 200, HEIGHT/2 - 50, 400, 100);
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
        player1.x = 120;
        player1.y = HEIGHT/2;
        player1.hasPuck = false;
        player2.x = WIDTH - 120;
        player2.y = HEIGHT/2;
        player2.hasPuck = false;
        goalScored = false;
    }

    private void checkGoal() {
        if (goalScored) return;

        boolean goal = false;
        String scorer = "";

        // Check if puck entered goal 1 (Player 2 scores)
        if (puck.x - PUCK_SIZE/2 < goal1.x + goal1.width &&
                puck.x + PUCK_SIZE/2 > goal1.x &&
                puck.y + PUCK_SIZE/2 > goal1.y && puck.y - PUCK_SIZE/2 < goal1.y + goal1.height) {
            score2++;
            scorer = "🔵 Player 2";
            goal = true;
        }
        // Check if puck entered goal 2 (Player 1 scores)
        else if (puck.x + PUCK_SIZE/2 > goal2.x &&
                puck.x - PUCK_SIZE/2 < goal2.x + goal2.width &&
                puck.y + PUCK_SIZE/2 > goal2.y && puck.y - PUCK_SIZE/2 < goal2.y + goal2.height) {
            score1++;
            scorer = "🔴 Player 1";
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

        // Update player movement
        player1.update(pressedKeys);
        player2.update(pressedKeys);

        // AI logic
        if (player2.isAI) {
            aiDecisionDelay++;
            aiStickCheck++;
            if (aiDecisionDelay > 3) {
                aiDecisionDelay = 0;
                updateAI();
            }
        }

        // Boundary checks for players (keep in their halves)
        player1.x = Math.max(PLAYER_SIZE/2 + 20, Math.min(WIDTH/2 - 60, player1.x));
        player1.y = Math.max(PLAYER_SIZE/2, Math.min(HEIGHT - PLAYER_SIZE/2, player1.y));

        if (player2.isAI || !againstComputer) {
            player2.x = Math.max(WIDTH/2 + 60, Math.min(WIDTH - PLAYER_SIZE/2 - 20, player2.x));
            player2.y = Math.max(PLAYER_SIZE/2, Math.min(HEIGHT - PLAYER_SIZE/2, player2.y));
        }

        // Update puck with realistic physics
        puck.vx *= PUCK_FRICTION;
        puck.vy *= PUCK_FRICTION;
        puck.x += puck.vx;
        puck.y += puck.vy;

        // Puck boundary collisions with boards
        if (puck.y - PUCK_SIZE/2 < 0) {
            puck.vy = Math.abs(puck.vy) * 0.8;
            puck.y = PUCK_SIZE/2;
        }
        if (puck.y + PUCK_SIZE/2 > HEIGHT) {
            puck.vy = -Math.abs(puck.vy) * 0.8;
            puck.y = HEIGHT - PUCK_SIZE/2;
        }

        if (puck.x - PUCK_SIZE/2 < 0) {
            puck.vx = Math.abs(puck.vx) * 0.8;
            puck.x = PUCK_SIZE/2;
        }
        if (puck.x + PUCK_SIZE/2 > WIDTH) {
            puck.vx = -Math.abs(puck.vx) * 0.8;
            puck.x = WIDTH - PUCK_SIZE/2;
        }

        // Player collisions with puck (stick handling)
        handleStickCollision(player1, puck);
        handleStickCollision(player2, puck);

        // Check for goals
        checkGoal();
    }

    private void handleStickCollision(HockeyPlayer player, Puck puck) {
        // Calculate stick position (in front of player based on direction)
        double stickX = player.x + (player.facingRight ? 20 : -20);
        double stickY = player.y;
        double dx = puck.x - stickX;
        double dy = puck.y - stickY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double stickReach = 25;

        if (distance < stickReach) {
            // Stick handling
            double angle = Math.atan2(dy, dx);
            double power = Math.min(MAX_SPEED * 1.2,
                    Math.hypot(player.vx, player.vy) + 8);

            // Shooting mechanic
            if (player.isShooting) {
                power = MAX_SPEED * 2.0;
                player.isShooting = false;
                player.shootCooldown = 30;
                // Add some random aiming error for realism
                angle += (random.nextDouble() - 0.5) * 0.3;
            }

            // Stick lift / poke check
            if (player.isPokeChecking) {
                power = MAX_SPEED * 1.5;
                player.isPokeChecking = false;
                player.pokeCooldown = 20;
            }

            puck.vx = Math.cos(angle) * power + player.vx * 0.5;
            puck.vy = Math.sin(angle) * power + player.vy * 0.5;

            // Player gains puck possession
            player.hasPuck = true;

            // Separate puck from stick
            double overlap = stickReach - distance;
            puck.x += Math.cos(angle) * overlap;
            puck.y += Math.sin(angle) * overlap;
        } else {
            player.hasPuck = false;
        }
    }

    private void updateAI() {
        // Smart AI that tries to intercept puck and position strategically
        double dxToPuck = puck.x - player2.x;
        double dyToPuck = puck.y - player2.y;
        double distanceToPuck = Math.hypot(dxToPuck, dyToPuck);

        // AI positioning - go to puck or defensive position
        double targetX, targetY;

        if (distanceToPuck < 150) {
            // Chase the puck
            targetX = puck.x;
            targetY = puck.y;

            // AI tries to intercept puck's trajectory
            if (Math.abs(puck.vx) > 2 || Math.abs(puck.vy) > 2) {
                targetX += puck.vx * 15;
                targetY += puck.vy * 15;
            }
        } else {
            // Defensive positioning - stay between puck and own goal
            targetX = Math.max(WIDTH - 200, puck.x * 0.3 + (WIDTH - 100) * 0.7);
            targetY = Math.min(HEIGHT - 50, Math.max(50, puck.y));
        }

        // Move towards target
        double dx = targetX - player2.x;
        double dy = targetY - player2.y;

        player2.vx = Math.min(MAX_SPEED, Math.max(-MAX_SPEED, dx * 0.1));
        player2.vy = Math.min(MAX_SPEED, Math.max(-MAX_SPEED, dy * 0.1));

        // Apply friction
        player2.vx *= FRICTION;
        player2.vy *= FRICTION;
        player2.x += player2.vx;
        player2.y += player2.vy;

        // AI decision making - shoot or poke check
        if (distanceToPuck < 40 && !player2.hasPuck) {
            if (random.nextInt(100) < 20 && player2.pokeCooldown == 0) {
                player2.isPokeChecking = true;
            }
        }

        if (player2.hasPuck && distanceToPuck < 30) {
            // AI aims for goal
            double angleToGoal = Math.atan2(goal1.y + goal1.height/2 - player2.y,
                    goal1.x + goal1.width/2 - player2.x);
            if (random.nextInt(100) < 25 && player2.shootCooldown == 0) {
                player2.isShooting = true;
            }
        }

        // Update cooldowns
        if (player2.shootCooldown > 0) player2.shootCooldown--;
        if (player2.pokeCooldown > 0) player2.pokeCooldown--;
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

        // Draw players
        player1.draw(g2d);
        player2.draw(g2d);

        // Draw puck with glow effect if near stick
        drawPuck(g2d);

        // Draw scoreboard
        drawScoreboard(g2d);

        // Draw game info
        drawGameInfo(g2d);

        if (paused) {
            drawPauseScreen(g2d);
        }

        // Draw controls help
        drawControls(g2d);
    }

    private void drawIceRink(Graphics2D g) {
        // Ice surface
        g.setColor(new Color(235, 245, 255));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Rink borders
        g.setColor(new Color(150, 100, 50));
        g.setStroke(new BasicStroke(8));
        g.drawRect(10, 10, WIDTH - 20, HEIGHT - 20);

        // Center line
        g.setColor(Color.BLUE);
        g.setStroke(new BasicStroke(3));
        g.drawLine(WIDTH/2, 10, WIDTH/2, HEIGHT - 10);

        // Center circle
        g.drawOval(WIDTH/2 - 60, HEIGHT/2 - 60, 120, 120);

        // Faceoff circles
        g.drawOval(200, HEIGHT/2 - 60, 120, 120);
        g.drawOval(WIDTH - 320, HEIGHT/2 - 60, 120, 120);

        // Faceoff dots
        g.fillOval(WIDTH/2 - 5, HEIGHT/2 - 5, 10, 10);
        g.fillOval(255, HEIGHT/2 - 5, 10, 10);
        g.fillOval(WIDTH - 265, HEIGHT/2 - 5, 10, 10);

        // Goal crease areas
        g.setColor(new Color(200, 200, 255, 100));
        g.fillArc(goal1.x - 20, goal1.y - 20, 60, goal1.height + 40, -90, 180);
        g.fillArc(goal2.x - 40, goal2.y - 20, 60, goal1.height + 40, 90, 180);
    }

    private void drawGoals(Graphics2D g) {
        // Goal 1 (left)
        g.setColor(new Color(180, 180, 180));
        g.fillRect(goal1.x, goal1.y, goal1.width, goal1.height);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(3));
        g.drawRect(goal1.x, goal1.y, goal1.width, goal1.height);
        g.setColor(Color.RED);
        g.drawLine(goal1.x + goal1.width, goal1.y, goal1.x + goal1.width, goal1.y + goal1.height);

        // Goal 2 (right)
        g.fillRect(goal2.x, goal2.y, goal2.width, goal2.height);
        g.setColor(Color.BLACK);
        g.drawRect(goal2.x, goal2.y, goal2.width, goal2.height);
        g.setColor(Color.RED);
        g.drawLine(goal2.x, goal2.y, goal2.x, goal2.y + goal2.height);
    }

    private void drawPuck(Graphics2D g) {
        // Puck shadow
        g.setColor(new Color(0, 0, 0, 50));
        g.fillOval((int)(puck.x - PUCK_SIZE/2 + 3), (int)(puck.y - PUCK_SIZE/2 + 3),
                PUCK_SIZE, PUCK_SIZE);

        // Puck
        RadialGradientPaint puckGrad = new RadialGradientPaint(
                (float)puck.x, (float)puck.y, PUCK_SIZE/2,
                new float[]{0f, 1f},
                new Color[]{Color.DARK_GRAY, Color.BLACK}
        );
        g.setPaint(puckGrad);
        g.fillOval((int)(puck.x - PUCK_SIZE/2), (int)(puck.y - PUCK_SIZE/2),
                PUCK_SIZE, PUCK_SIZE);

        // Puck highlight
        g.setColor(Color.GRAY);
        g.fillOval((int)(puck.x - PUCK_SIZE/4), (int)(puck.y - PUCK_SIZE/4),
                PUCK_SIZE/2, PUCK_SIZE/2);
    }

    private void drawScoreboard(Graphics2D g) {
        // Scoreboard background
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(WIDTH/2 - 150, 20, 300, 80, 15, 15);

        // Team names and scores
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.setColor(Color.RED);
        g.drawString(String.valueOf(score1), WIDTH/2 - 90, 85);
        g.setColor(Color.WHITE);
        g.drawString("-", WIDTH/2 - 15, 85);
        g.setColor(Color.BLUE);
        g.drawString(String.valueOf(score2), WIDTH/2 + 50, 85);

        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.setColor(Color.WHITE);
        g.drawString("RED TEAM", WIDTH/2 - 130, 45);
        g.drawString("BLUE TEAM", WIDTH/2 + 70, 45);
    }

    private void drawGameInfo(Graphics2D g) {
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.BLACK);
        int minutes = gameTime / 60;
        int seconds = gameTime % 60;
        String timeStr = String.format("Period %d: %02d:%02d", period, minutes, seconds);

        // Time background
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(20, 20, 220, 40, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString(timeStr, 30, 50);

        if (againstComputer) {
            g.drawString("🤖 VS COMPUTER", WIDTH - 200, 50);
        } else {
            g.drawString("👥 2 PLAYER MODE", WIDTH - 200, 50);
        }
    }

    private void drawPauseScreen(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setFont(new Font("Arial", Font.BOLD, 72));
        g.setColor(Color.WHITE);
        g.drawString("PAUSED", WIDTH/2 - 110, HEIGHT/2);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Press P to Resume", WIDTH/2 - 100, HEIGHT/2 + 60);
    }

    private void drawControls(Graphics2D g) {
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(Color.DARK_GRAY);
        g.drawString("🔴 Player 1: WASD + Space (Shoot) + Shift (Poke Check)", 20, HEIGHT - 60);
        if (againstComputer) {
            g.drawString("🔵 Computer: AI Controlled", 20, HEIGHT - 40);
        } else {
            g.drawString("🔵 Player 2: Arrow Keys + Enter (Shoot) + Numpad0 (Poke Check)", 20, HEIGHT - 40);
        }
        g.drawString("⏸ P - Pause | ❌ ESC - Quit", 20, HEIGHT - 20);
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
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner classes for game objects
    class Puck {
        double x, y, vx, vy;
        Puck(double x, double y) { this.x = x; this.y = y; vx = 0; vy = 0; }
    }

    class HockeyPlayer {
        double x, y, vx, vy;
        String teamColor;
        String playerName;
        int up, down, left, right, shoot, pokeCheck;
        boolean isAI = false;
        boolean isShooting = false;
        boolean isPokeChecking = false;
        boolean hasPuck = false;
        int shootCooldown = 0;
        int pokeCooldown = 0;
        boolean facingRight = true;

        HockeyPlayer(int x, int y, String teamColor, String playerName,
                     int up, int down, int left, int right, int shoot, int pokeCheck) {
            this.x = x; this.y = y;
            this.teamColor = teamColor;
            this.playerName = playerName;
            this.up = up; this.down = down; this.left = left; this.right = right;
            this.shoot = shoot; this.pokeCheck = pokeCheck;
        }

        void update(Set<Integer> keys) {
            if (isAI) return;

            // Movement
            if (keys.contains(up)) vy = Math.max(-MAX_SPEED, vy - 0.6);
            if (keys.contains(down)) vy = Math.min(MAX_SPEED, vy + 0.6);
            if (keys.contains(left)) {
                vx = Math.max(-MAX_SPEED, vx - 0.6);
                facingRight = false;
            }
            if (keys.contains(right)) {
                vx = Math.min(MAX_SPEED, vx + 0.6);
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

            // Apply friction and update position
            vx *= FRICTION;
            vy *= FRICTION;
            x += vx;
            y += vy;

            // Update cooldowns
            if (shootCooldown > 0) shootCooldown--;
            if (pokeCooldown > 0) pokeCooldown--;
        }

        void draw(Graphics2D g) {
            // Player shadow
            g.setColor(new Color(0, 0, 0, 50));
            g.fillOval((int)(x - PLAYER_SIZE/2 + 5), (int)(y - PLAYER_SIZE/2 + 5),
                    PLAYER_SIZE, PLAYER_SIZE);

            // Body
            Color mainColor = teamColor.equals("RED") ? new Color(200, 50, 50) : new Color(50, 50, 200);
            g.setColor(mainColor);
            g.fillOval((int)(x - PLAYER_SIZE/2), (int)(y - PLAYER_SIZE/2), PLAYER_SIZE, PLAYER_SIZE);

            // Jersey number
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            String number = teamColor.equals("RED") ? "99" : "88";
            FontMetrics fm = g.getFontMetrics();
            int numWidth = fm.stringWidth(number);
            g.drawString(number, (int)x - numWidth/2, (int)y + 6);

            // Helmet
            g.setColor(Color.BLACK);
            g.fillArc((int)(x - PLAYER_SIZE/2), (int)(y - PLAYER_SIZE/2),
                    PLAYER_SIZE, PLAYER_SIZE, 0, 180);

            // Stick
            g.setColor(new Color(160, 100, 60));
            g.setStroke(new BasicStroke(4));
            int stickX = (int)x + (facingRight ? 15 : -25);
            int stickY = (int)y + 5;
            g.drawLine((int)x + (facingRight ? 10 : -10), (int)y,
                    stickX + (facingRight ? 20 : -20), stickY);

            // Stick blade
            g.fillRect(stickX, stickY, facingRight ? 20 : -20, 6);

            // Skates
            g.setColor(Color.DARK_GRAY);
            g.fillRect((int)x - 15, (int)y + 15, 10, 8);
            g.fillRect((int)x + 5, (int)y + 15, 10, 8);

            // Effect indicators
            if (isShooting) {
                g.setColor(Color.YELLOW);
                g.setStroke(new BasicStroke(3));
                g.drawOval((int)(x - PLAYER_SIZE/1.5), (int)(y - PLAYER_SIZE/1.5),
                        PLAYER_SIZE + 15, PLAYER_SIZE + 15);
            }

            if (isPokeChecking) {
                g.setColor(Color.ORANGE);
                g.setStroke(new BasicStroke(2));
                g.drawLine((int)x + (facingRight ? 20 : -20), (int)y,
                        (int)x + (facingRight ? 60 : -60), (int)y);
            }

            if (hasPuck) {
                g.setColor(Color.CYAN);
                g.setStroke(new BasicStroke(2));
                g.drawOval((int)(x - PLAYER_SIZE/2 - 5), (int)(y - PLAYER_SIZE/2 - 5),
                        PLAYER_SIZE + 10, PLAYER_SIZE + 10);
            }

            // Name tag
            g.setFont(new Font("Arial", Font.PLAIN, 11));
            g.setColor(Color.WHITE);
            String name = playerName;
            int nameWidth = g.getFontMetrics().stringWidth(name);
            g.drawString(name, (int)x - nameWidth/2, (int)y - PLAYER_SIZE/2 - 5);
        }
    }

    class Goal {
        int x, y, width, height;
        Goal(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    public static void main(String[] args) {
        String[] options = {"🏒 Play vs Computer", "👥 Two Players"};
        int choice = JOptionPane.showOptionDialog(null,
                "ICE HOCKEY SIMULATOR\nSelect Game Mode",
                "Hockey Game",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        JFrame frame = new JFrame("🏒 ICE HOCKEY SIMULATOR - Realistic Hockey Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new IceHockeySimulator(choice == 0));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}