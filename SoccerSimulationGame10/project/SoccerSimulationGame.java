import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * SoccerSimulationGame
 * A full-featured 2D soccer simulation with:
 *   - Player vs Player (2-player local) mode
 *   - Player vs Computer (AI) mode
 *   - Smooth 60 FPS animation with interpolation
 *   - Particle effects, trail rendering, screen shake
 *   - Score tracking, timer, goal celebrations
 *   - Beautiful stadium rendering with stands, field markings, lighting
 */
public class SoccerSimulationGame extends JFrame {

    private GamePanel gamePanel;
    private MenuPanel menuPanel;
    private CardLayout cardLayout;
    private JPanel cardPanel;

    public static final int WINDOW_WIDTH = 1000;
    public static final int WINDOW_HEIGHT = 680;

    public enum GameMode { TWO_PLAYER, VS_COMPUTER }

    private GameMode currentMode = GameMode.TWO_PLAYER;

    public SoccerSimulationGame() {
        setTitle("Soccer Simulation Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        menuPanel = new MenuPanel(this);
        gamePanel = new GamePanel(this);

        cardPanel.add(menuPanel, "menu");
        cardPanel.add(gamePanel, "game");

        add(cardPanel);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
    }

    public void startGame(GameMode mode) {
        this.currentMode = mode;
        gamePanel.startMatch(mode);
        cardLayout.show(cardPanel, "game");
        gamePanel.requestFocusInWindow();
    }

    public void returnToMenu() {
        gamePanel.stopMatch();
        cardLayout.show(cardPanel, "menu");
    }

    public GameMode getCurrentMode() {
        return currentMode;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            SoccerSimulationGame game = new SoccerSimulationGame();
            game.setVisible(true);
        });
    }

    // ============================================================
    //  MENU PANEL
    // ============================================================
    class MenuPanel extends JPanel {
        private SoccerSimulationGame parent;
        private float pulse = 0f;
        private Timer animTimer;

        public MenuPanel(SoccerSimulationGame parent) {
            this.parent = parent;
            setLayout(null);
            setBackground(new Color(18, 42, 24));

            JButton btnTwoPlayer = createMenuButton("2 PLAYERS", new Color(34, 153, 84));
            btnTwoPlayer.setBounds(350, 340, 300, 60);
            btnTwoPlayer.addActionListener(e -> parent.startGame(GameMode.TWO_PLAYER));
            add(btnTwoPlayer);

            JButton btnVsComputer = createMenuButton("VS COMPUTER", new Color(231, 76, 60));
            btnVsComputer.setBounds(350, 420, 300, 60);
            btnVsComputer.addActionListener(e -> parent.startGame(GameMode.VS_COMPUTER));
            add(btnVsComputer);

            JButton btnExit = createMenuButton("EXIT", new Color(100, 100, 100));
            btnExit.setBounds(350, 500, 300, 60);
            btnExit.addActionListener(e -> System.exit(0));
            add(btnExit);

            animTimer = new Timer(30, e -> {
                pulse += 0.05f;
                repaint();
            });
            animTimer.start();
        }

        private JButton createMenuButton(String text, Color baseColor) {
            JButton btn = new JButton(text) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int arc = 20;
                    if (getModel().isPressed()) {
                        g2.setColor(baseColor.darker());
                    } else if (getModel().isRollover()) {
                        g2.setColor(baseColor.brighter());
                    } else {
                        g2.setColor(baseColor);
                    }
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);
                    g2.setFont(new Font("Arial", Font.BOLD, 22));
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                    int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(getText(), tx, ty);
                    g2.dispose();
                }
            };
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("Arial", Font.BOLD, 22));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return btn;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Gradient background
            GradientPaint bg = new GradientPaint(0, 0, new Color(10, 30, 15),
                    0, getHeight(), new Color(20, 60, 30));
            g2.setPaint(bg);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Decorative soccer field lines in background
            g2.setColor(new Color(255, 255, 255, 20));
            g2.setStroke(new BasicStroke(2f));
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            g2.drawOval(cx - 120, cy - 120, 240, 240);
            g2.drawLine(cx, 80, cx, getHeight() - 80);

            // Title with pulsing glow
            float glow = (float) (0.5 + 0.5 * Math.sin(pulse));
            g2.setFont(new Font("Arial", Font.BOLD, 52));
            FontMetrics fm = g2.getFontMetrics();
            String title = "SOCCER SIMULATION";
            int tx = (getWidth() - fm.stringWidth(title)) / 2;
            int ty = 160;
            // Glow
            g2.setColor(new Color(255, 255, 255, (int)(40 + 60 * glow)));
            for (int i = 0; i < 6; i++) {
                g2.drawString(title, tx - i, ty);
                g2.drawString(title, tx + i, ty);
            }
            g2.setColor(new Color(255, 255, 255, (int)(180 + 75 * glow)));
            g2.drawString(title, tx, ty);

            // Subtitle
            g2.setFont(new Font("Arial", Font.PLAIN, 18));
            g2.setColor(new Color(200, 220, 210));
            String sub = "Choose your game mode";
            int sx = (getWidth() - g2.getFontMetrics().stringWidth(sub)) / 2;
            g2.drawString(sub, sx, 210);

            // Animated soccer ball
            int ballX = cx + (int)(Math.sin(pulse * 0.5) * 150);
            int ballY = 270 + (int)(Math.cos(pulse * 0.7) * 15);
            drawSoccerBall(g2, ballX, ballY, 25, pulse);

            // Footer
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.setColor(new Color(150, 170, 160));
            g2.drawString("Controls: Player1 = W/A/S/D  |  Player2 / Computer = Arrow Keys", 260, 600);

            g2.dispose();
        }

        private void drawSoccerBall(Graphics2D g2, int x, int y, int r, float rotation) {
            g2 = (Graphics2D) g2.create();
            g2.translate(x, y);
            g2.rotate(rotation * 0.3);
            // Shadow
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillOval(-r + 3, -r + 5, r * 2, r * 2);
            // Ball body
            RadialGradientPaint rgp = new RadialGradientPaint(
                    -r / 3f, -r / 3f, r * 1.2f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(245, 245, 245), new Color(180, 180, 190)});
            g2.setPaint(rgp);
            g2.fillOval(-r, -r, r * 2, r * 2);
            // Pentagon patterns
            g2.setColor(new Color(40, 40, 50));
            int[] px = {0, -r/2, r/2, r/2, -r/2};
            int[] py = {-r, -r/4, -r/4, r/2, r/2};
            // simplified pentagon
            g2.fillPolygon(new int[]{0, -8, -5, 5, 8}, new int[]{-12, -4, 4, 4, -4}, 5);
            g2.fillPolygon(new int[]{-14, -18, -16, -12, -10}, new int[]{2, 8, 14, 14, 8}, 5);
            g2.fillPolygon(new int[]{14, 18, 16, 12, 10}, new int[]{2, 8, 14, 14, 8}, 5);
            g2.fillPolygon(new int[]{-6, -10, -8, 8, 10}, new int[]{14, 18, 22, 22, 18}, 5);
            // Outline
            g2.setColor(new Color(60, 60, 70));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(-r, -r, r * 2, r * 2);
            g2.dispose();
        }
    }

    // ============================================================
    //  GAME PANEL
    // ============================================================
    class GamePanel extends JPanel implements KeyListener {
        private SoccerSimulationGame parent;

        // Field dimensions
        private static final int FIELD_MARGIN = 40;
        private static final int FIELD_TOP = 80;
        private int fieldLeft, fieldRight, fieldTop, fieldBottom, fieldWidth, fieldHeight;

        // Game state
        private boolean running = false;
        private Timer gameTimer;
        private long lastTime;
        private int scoreLeft = 0;
        private int scoreRight = 0;
        private int matchTime; // seconds remaining
        private int matchDuration = 120; // 2 minutes
        private long timeAccumulator;

        // Entities
        private SoccerBall ball;
        private List<SoccerPlayer> players;
        private List<Particle> particles;
        private List<GoalCelebration> celebrations;

        // Screen shake
        private float shakeIntensity = 0f;
        private float shakeOffsetX = 0f;
        private float shakeOffsetY = 0f;

        // Goal state
        private boolean goalScored = false;
        private long goalResetTimer = 0;
        private String lastScorer = "";

        // Input
        private Set<Integer> keysPressed = new HashSet<>();

        // AI
        private long aiThinkTimer = 0;

        // Constants
        private static final double PLAYER_SPEED = 3.2;
        private static final double PLAYER_SPEED_AI = 3.0;
        private static final double BALL_FRICTION = 0.985;
        private static final double KICK_POWER = 9.0;
        private static final double MAX_BALL_SPEED = 14.0;

        public GamePanel(SoccerSimulationGame parent) {
            this.parent = parent;
            setLayout(null);
            setBackground(new Color(20, 70, 30));
            setFocusable(true);
            addKeyListener(this);

            // Back to menu button
            JButton btnMenu = new JButton("MENU");
            btnMenu.setBounds(10, 10, 80, 30);
            btnMenu.setFont(new Font("Arial", Font.BOLD, 12));
            btnMenu.setBackground(new Color(50, 50, 50));
            btnMenu.setForeground(Color.WHITE);
            btnMenu.setBorderPainted(false);
            btnMenu.setFocusPainted(false);
            btnMenu.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnMenu.addActionListener(e -> parent.returnToMenu());
            add(btnMenu);

            initField();
        }

        private void initField() {
            fieldLeft = FIELD_MARGIN;
            fieldRight = WINDOW_WIDTH - FIELD_MARGIN;
            fieldTop = FIELD_TOP;
            fieldBottom = WINDOW_HEIGHT - FIELD_MARGIN;
            fieldWidth = fieldRight - fieldLeft;
            fieldHeight = fieldBottom - fieldTop;
        }

        public void startMatch(GameMode mode) {
            initField();
            scoreLeft = 0;
            scoreRight = 0;
            matchTime = matchDuration;
            timeAccumulator = 0;
            particles = new ArrayList<>();
            celebrations = new ArrayList<>();
            shakeIntensity = 0;
            goalScored = false;

            ball = new SoccerBall(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2);

            players = new ArrayList<>();
            // Left team (Player 1)
            players.add(new SoccerPlayer(fieldLeft + 80, WINDOW_HEIGHT / 2, 0, new Color(34, 153, 84), "P1"));
            players.add(new SoccerPlayer(fieldLeft + 150, WINDOW_HEIGHT / 2 - 100, 0, new Color(34, 153, 84), "P1"));
            players.add(new SoccerPlayer(fieldLeft + 150, WINDOW_HEIGHT / 2 + 100, 0, new Color(34, 153, 84), "P1"));

            // Right team (Player 2 or Computer)
            Color rightColor = (mode == GameMode.VS_COMPUTER) ? new Color(231, 76, 60) : new Color(52, 152, 219);
            String rightLabel = (mode == GameMode.VS_COMPUTER) ? "CPU" : "P2";
            players.add(new SoccerPlayer(fieldRight - 80, WINDOW_HEIGHT / 2, 1, rightColor, rightLabel));
            players.add(new SoccerPlayer(fieldRight - 150, WINDOW_HEIGHT / 2 - 100, 1, rightColor, rightLabel));
            players.add(new SoccerPlayer(fieldRight - 150, WINDOW_HEIGHT / 2 + 100, 1, rightColor, rightLabel));

            running = true;
            lastTime = System.nanoTime();

            gameTimer = new Timer(16, e -> gameLoop());
            gameTimer.start();
        }

        public void stopMatch() {
            running = false;
            if (gameTimer != null) gameTimer.stop();
        }

        private void gameLoop() {
            long now = System.nanoTime();
            double dt = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;
            dt = Math.min(dt, 0.033); // clamp

            if (running && !goalScored) {
                update(dt);
            } else if (goalScored) {
                goalResetTimer -= dt;
                updateParticles(dt);
                updateCelebrations(dt);
                if (goalResetTimer <= 0) {
                    resetPositions();
                    goalScored = false;
                }
            }

            // Screen shake decay
            shakeIntensity *= 0.90;
            shakeOffsetX = (float)(Math.random() - 0.5) * shakeIntensity;
            shakeOffsetY = (float)(Math.random() - 0.5) * shakeIntensity;

            repaint();
        }

        private void update(double dt) {
            // Timer
            timeAccumulator += dt * 1000;
            if (timeAccumulator >= 1000) {
                timeAccumulator -= 1000;
                matchTime--;
                if (matchTime <= 0) {
                    matchTime = 0;
                    running = false;
                }
            }

            handleInput();
            if (parent.getCurrentMode() == GameMode.VS_COMPUTER) {
                updateAI(dt);
            }

            // Update players
            for (SoccerPlayer p : players) {
                p.update(dt, fieldLeft, fieldRight, fieldTop, fieldBottom);
            }

            // Player-ball collision
            for (SoccerPlayer p : players) {
                double dx = ball.x - p.x;
                double dy = ball.y - p.y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double minDist = p.radius + ball.radius;
                if (dist < minDist && dist > 0) {
                    // Push ball away
                    double nx = dx / dist;
                    double ny = dy / dist;
                    double overlap = minDist - dist;
                    ball.x += nx * overlap;
                    ball.y += ny * overlap;

                    // Transfer velocity
                    double pSpeed = Math.sqrt(p.vx * p.vx + p.vy * p.vy);
                    double kickStrength = KICK_POWER * (0.5 + pSpeed / PLAYER_SPEED * 0.5);
                    ball.vx = nx * kickStrength + p.vx * 0.6;
                    ball.vy = ny * kickStrength + p.vy * 0.6;

                    // Clamp ball speed
                    double bs = Math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy);
                    if (bs > MAX_BALL_SPEED) {
                        ball.vx = ball.vx / bs * MAX_BALL_SPEED;
                        ball.vy = ball.vy / bs * MAX_BALL_SPEED;
                    }

                    // Kick particles
                    for (int i = 0; i < 6; i++) {
                        particles.add(new Particle(ball.x, ball.y,
                            -nx * 3 + (Math.random() - 0.5) * 4,
                            -ny * 3 + (Math.random() - 0.5) * 4,
                            p.team == 0 ? new Color(100, 255, 150) : new Color(100, 180, 255), 0.5));
                    }

                    // Player kick recoil
                    p.vx -= nx * 0.5;
                    p.vy -= ny * 0.5;
                }
            }

            // Player-player collision
            for (int i = 0; i < players.size(); i++) {
                for (int j = i + 1; j < players.size(); j++) {
                    SoccerPlayer a = players.get(i);
                    SoccerPlayer b = players.get(j);
                    double dx = b.x - a.x;
                    double dy = b.y - a.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    double minDist = a.radius + b.radius;
                    if (dist < minDist && dist > 0) {
                        double nx = dx / dist;
                        double ny = dy / dist;
                        double overlap = (minDist - dist) / 2;
                        a.x -= nx * overlap;
                        a.y -= ny * overlap;
                        b.x += nx * overlap;
                        b.y += ny * overlap;
                    }
                }
            }

            // Update ball
            ball.update(dt);

            // Ball trail
            ball.addTrailPoint();

            // Ball-field collision (walls)
            int goalHeight = 140;
            int goalTop = WINDOW_HEIGHT / 2 - goalHeight / 2;
            int goalBottom = goalTop + goalHeight;

            // Left wall (check for goal)
            if (ball.x - ball.radius < fieldLeft) {
                if (ball.y > goalTop && ball.y < goalBottom) {
                    scoreGoal("right");
                } else {
                    ball.x = fieldLeft + ball.radius;
                    ball.vx = -ball.vx * 0.7;
                    spawnWallParticles(ball.x, ball.y, 1, 0);
                }
            }
            // Right wall (check for goal)
            if (ball.x + ball.radius > fieldRight) {
                if (ball.y > goalTop && ball.y < goalBottom) {
                    scoreGoal("left");
                } else {
                    ball.x = fieldRight - ball.radius;
                    ball.vx = -ball.vx * 0.7;
                    spawnWallParticles(ball.x, ball.y, -1, 0);
                }
            }
            // Top wall
            if (ball.y - ball.radius < fieldTop) {
                ball.y = fieldTop + ball.radius;
                ball.vy = -ball.vy * 0.7;
                spawnWallParticles(ball.x, ball.y, 0, 1);
            }
            // Bottom wall
            if (ball.y + ball.radius > fieldBottom) {
                ball.y = fieldBottom - ball.radius;
                ball.vy = -ball.vy * 0.7;
                spawnWallParticles(ball.x, ball.y, 0, -1);
            }

            updateParticles(dt);
            updateCelebrations(dt);
        }

        private void spawnWallParticles(double x, double y, double dx, double dy) {
            for (int i = 0; i < 5; i++) {
                particles.add(new Particle(x, y,
                    dx * 2 + (Math.random() - 0.5) * 3,
                    dy * 2 + (Math.random() - 0.5) * 3,
                    Color.WHITE, 0.3));
            }
        }

        private void scoreGoal(String side) {
            if (side.equals("left")) {
                scoreLeft++;
                lastScorer = "GREEN TEAM";
            } else {
                scoreRight++;
                lastScorer = (parent.getCurrentMode() == GameMode.VS_COMPUTER) ? "COMPUTER" : "BLUE TEAM";
            }
            goalScored = true;
            goalResetTimer = 2.5;
            shakeIntensity = 15;

            // Celebration particles
            Color celebColor = side.equals("left") ? new Color(34, 200, 84) : new Color(231, 76, 60);
            for (int i = 0; i < 80; i++) {
                double angle = Math.random() * Math.PI * 2;
                double speed = 2 + Math.random() * 6;
                particles.add(new Particle(ball.x, ball.y,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    celebColor, 1.5 + Math.random()));
            }
            celebrations.add(new GoalCelebration(side.equals("left") ? "GOAL!" : "GOAL!",
                celebColor, WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2));
        }

        private void resetPositions() {
            ball.x = WINDOW_WIDTH / 2;
            ball.y = WINDOW_HEIGHT / 2;
            ball.vx = 0;
            ball.vy = 0;
            ball.trail.clear();

            int idx = 0;
            int[][] leftPos = {{fieldLeft + 80, WINDOW_HEIGHT / 2},
                              {fieldLeft + 150, WINDOW_HEIGHT / 2 - 100},
                              {fieldLeft + 150, WINDOW_HEIGHT / 2 + 100}};
            int[][] rightPos = {{fieldRight - 80, WINDOW_HEIGHT / 2},
                               {fieldRight - 150, WINDOW_HEIGHT / 2 - 100},
                               {fieldRight - 150, WINDOW_HEIGHT / 2 + 100}};
            for (int i = 0; i < 3; i++) {
                players.get(i).x = leftPos[i][0];
                players.get(i).y = leftPos[i][1];
                players.get(i).vx = 0;
                players.get(i).vy = 0;
            }
            for (int i = 0; i < 3; i++) {
                players.get(i + 3).x = rightPos[i][0];
                players.get(i + 3).y = rightPos[i][1];
                players.get(i + 3).vx = 0;
                players.get(i + 3).vy = 0;
            }
        }

        private void updateParticles(double dt) {
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.update(dt);
                if (p.life <= 0) it.remove();
            }
        }

        private void updateCelebrations(double dt) {
            Iterator<GoalCelebration> it = celebrations.iterator();
            while (it.hasNext()) {
                GoalCelebration c = it.next();
                c.update(dt);
                if (c.life <= 0) it.remove();
            }
        }

        private void handleInput() {
            // Player 1: WASD
            SoccerPlayer p1 = players.get(0);
            double p1x = 0, p1y = 0;
            if (keysPressed.contains(KeyEvent.VK_W)) p1y -= 1;
            if (keysPressed.contains(KeyEvent.VK_S)) p1y += 1;
            if (keysPressed.contains(KeyEvent.VK_A)) p1x -= 1;
            if (keysPressed.contains(KeyEvent.VK_D)) p1x += 1;
            normalizeAndSetVelocity(p1, p1x, p1y, PLAYER_SPEED);

            // Player 2: Arrow keys (only in two-player mode)
            if (parent.getCurrentMode() == GameMode.TWO_PLAYER) {
                SoccerPlayer p2 = players.get(3);
                double p2x = 0, p2y = 0;
                if (keysPressed.contains(KeyEvent.VK_UP)) p2y -= 1;
                if (keysPressed.contains(KeyEvent.VK_DOWN)) p2y += 1;
                if (keysPressed.contains(KeyEvent.VK_LEFT)) p2x -= 1;
                if (keysPressed.contains(KeyEvent.VK_RIGHT)) p2x += 1;
                normalizeAndSetVelocity(p2, p2x, p2y, PLAYER_SPEED);
            }
        }

        private void normalizeAndSetVelocity(SoccerPlayer p, double dx, double dy, double speed) {
            if (dx == 0 && dy == 0) {
                p.vx *= 0.7;
                p.vy *= 0.7;
            } else {
                double len = Math.sqrt(dx * dx + dy * dy);
                p.vx = (dx / len) * speed;
                p.vy = (dy / len) * speed;
            }
        }

        // ============================================================
        //  AI LOGIC
        // ============================================================
        private void updateAI(double dt) {
            aiThinkTimer += dt * 1000;

            // AI controls team 1 (right side, players 3, 4, 5)
            // Player 3 is the main attacker/defender
            SoccerPlayer ai0 = players.get(3);
            SoccerPlayer ai1 = players.get(4);
            SoccerPlayer ai2 = players.get(5);

            // Determine which AI player is closest to ball
            SoccerPlayer closest = ai0;
            double minDist = dist(ai0, ball);
            double d1 = dist(ai1, ball);
            double d2 = dist(ai2, ball);
            if (d1 < minDist) { closest = ai1; minDist = d1; }
            if (d2 < minDist) { closest = ai2; minDist = d2; }

            // Closest player chases the ball and aims toward left goal
            chaseBall(closest, dt);

            // Other two players maintain formation
            maintainFormation(ai0, ai1, ai2, closest);
        }

        private double dist(SoccerPlayer p, SoccerBall b) {
            return Math.sqrt((p.x - b.x) * (p.x - b.x) + (p.y - b.y) * (p.y - b.y));
        }

        private void chaseBall(SoccerPlayer p, double dt) {
            double dx = ball.x - p.x;
            double dy = ball.y - p.y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist < 5) {
                // Kick toward left goal
                double targetX = fieldLeft;
                double targetY = WINDOW_HEIGHT / 2 + (Math.random() - 0.5) * 100;
                double kdx = targetX - p.x;
                double kdy = targetY - p.y;
                double klen = Math.sqrt(kdx * kdx + kdy * kdy);
                p.vx = (kdx / klen) * PLAYER_SPEED_AI;
                p.vy = (kdy / klen) * PLAYER_SPEED_AI;
            } else {
                // Chase ball, but aim slightly ahead of it
                double predictX = ball.x + ball.vx * 5;
                double predictY = ball.y + ball.vy * 5;
                double tx = predictX - p.x;
                double ty = predictY - p.y;
                double tlen = Math.sqrt(tx * tx + ty * ty);
                if (tlen > 0) {
                    p.vx = (tx / tlen) * PLAYER_SPEED_AI;
                    p.vy = (ty / tlen) * PLAYER_SPEED_AI;
                }
            }
        }

        private void maintainFormation(SoccerPlayer a, SoccerPlayer b, SoccerPlayer c, SoccerPlayer chaser) {
            // Non-chasing players hold defensive/formation positions
            if (a != chaser) holdPosition(a, fieldRight - 120, WINDOW_HEIGHT / 2);
            if (b != chaser) holdPosition(b, fieldRight - 200, WINDOW_HEIGHT / 2 - 100);
            if (c != chaser) holdPosition(c, fieldRight - 200, WINDOW_HEIGHT / 2 + 100);
        }

        private void holdPosition(SoccerPlayer p, double tx, double ty) {
            double dx = tx - p.x;
            double dy = ty - p.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 15) {
                p.vx = (dx / dist) * PLAYER_SPEED_AI * 0.7;
                p.vy = (dy / dist) * PLAYER_SPEED_AI * 0.7;
            } else {
                p.vx *= 0.5;
                p.vy *= 0.5;
            }
        }

        // ============================================================
        //  RENDERING
        // ============================================================
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // Apply screen shake
            g2.translate(shakeOffsetX, shakeOffsetY);

            drawStadium(g2);
            drawField(g2);
            drawGoals(g2);

            // Draw ball trail
            ball.drawTrail(g2);

            // Draw particles (behind players)
            for (Particle p : particles) p.draw(g2);

            // Draw players
            for (SoccerPlayer p : players) p.draw(g2);

            // Draw ball
            ball.draw(g2);

            // Draw celebrations on top
            for (GoalCelebration c : celebrations) c.draw(g2);

            // Draw HUD
            drawHUD(g2);

            // Draw game over
            if (!running && matchTime <= 0) {
                drawGameOver(g2);
            }

            g2.dispose();
        }

        private void drawStadium(Graphics2D g2) {
            // Dark stadium background
            g2.setColor(new Color(15, 15, 20));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Stands (top)
            GradientPaint standsTop = new GradientPaint(0, 0, new Color(40, 40, 50),
                    0, FIELD_TOP, new Color(25, 25, 35));
            g2.setPaint(standsTop);
            g2.fillRect(0, 0, getWidth(), FIELD_TOP);

            // Stands (bottom)
            GradientPaint standsBot = new GradientPaint(0, fieldBottom, new Color(25, 25, 35),
                    0, getHeight(), new Color(40, 40, 50));
            g2.setPaint(standsBot);
            g2.fillRect(0, fieldBottom, getWidth(), getHeight() - fieldBottom);

            // Crowd dots in stands
            Random rnd = new Random(42); // fixed seed for stable crowd
            g2.setColor(new Color(80, 80, 100));
            for (int i = 0; i < 200; i++) {
                int x = rnd.nextInt(getWidth());
                int y = 10 + rnd.nextInt(FIELD_TOP - 20);
                g2.fillRect(x, y, 2, 2);
            }
            for (int i = 0; i < 200; i++) {
                int x = rnd.nextInt(getWidth());
                int y = fieldBottom + 5 + rnd.nextInt(getHeight() - fieldBottom - 10);
                g2.fillRect(x, y, 2, 2);
            }

            // Floodlight glows at corners
            drawFloodlight(g2, 50, 30);
            drawFloodlight(g2, getWidth() - 50, 30);
            drawFloodlight(g2, 50, getHeight() - 20);
            drawFloodlight(g2, getWidth() - 50, getHeight() - 20);
        }

        private void drawFloodlight(Graphics2D g2, int x, int y) {
            RadialGradientPaint glow = new RadialGradientPaint(x, y, 80,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 255, 220, 40), new Color(255, 255, 220, 0)});
            g2.setPaint(glow);
            g2.fillOval(x - 80, y - 80, 160, 160);
        }

        private void drawField(Graphics2D g2) {
            // Grass with mow stripes
            int stripes = 12;
            int stripeWidth = fieldWidth / stripes;
            for (int i = 0; i < stripes; i++) {
                g2.setColor(i % 2 == 0 ? new Color(34, 139, 34) : new Color(28, 120, 28));
                g2.fillRect(fieldLeft + i * stripeWidth, fieldTop, stripeWidth, fieldHeight);
            }

            // Field border
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRect(fieldLeft, fieldTop, fieldWidth, fieldHeight);

            // Center line
            int cx = fieldLeft + fieldWidth / 2;
            g2.drawLine(cx, fieldTop, cx, fieldBottom);

            // Center circle
            g2.drawOval(cx - 70, WINDOW_HEIGHT / 2 - 70, 140, 140);
            g2.fillOval(cx - 3, WINDOW_HEIGHT / 2 - 3, 6, 6);

            // Penalty boxes
            int boxHeight = 200;
            int boxTop = WINDOW_HEIGHT / 2 - boxHeight / 2;
            int boxWidth = 100;
            g2.drawRect(fieldLeft, boxTop, boxWidth, boxHeight);
            g2.drawRect(fieldRight - boxWidth, boxTop, boxWidth, boxHeight);

            // Goal areas (small boxes)
            int smallBoxH = 120;
            int smallBoxTop = WINDOW_HEIGHT / 2 - smallBoxH / 2;
            int smallBoxW = 45;
            g2.drawRect(fieldLeft, smallBoxTop, smallBoxW, smallBoxH);
            g2.drawRect(fieldRight - smallBoxW, smallBoxTop, smallBoxW, smallBoxH);

            // Corner arcs
            int arcR = 20;
            g2.drawArc(fieldLeft, fieldTop, arcR * 2, arcR * 2, 270, 90);
            g2.drawArc(fieldRight - arcR * 2, fieldTop, arcR * 2, arcR * 2, 0, 90);
            g2.drawArc(fieldLeft, fieldBottom - arcR * 2, arcR * 2, arcR * 2, 180, 90);
            g2.drawArc(fieldRight - arcR * 2, fieldBottom - arcR * 2, arcR * 2, arcR * 2, 90, 90);
        }

        private void drawGoals(Graphics2D g2) {
            int goalHeight = 140;
            int goalTop = WINDOW_HEIGHT / 2 - goalHeight / 2;

            // Left goal
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(fieldLeft, goalTop, fieldLeft - 25, goalTop);
            g2.drawLine(fieldLeft, goalTop + goalHeight, fieldLeft - 25, goalTop + goalHeight);
            g2.drawLine(fieldLeft - 25, goalTop, fieldLeft - 25, goalTop + goalHeight);

            // Net pattern left
            g2.setColor(new Color(255, 255, 255, 60));
            g2.setStroke(new BasicStroke(1f));
            for (int i = 0; i <= 5; i++) {
                int y = goalTop + i * (goalHeight / 5);
                g2.drawLine(fieldLeft - 25, y, fieldLeft, y);
            }
            for (int i = 0; i <= 5; i++) {
                int x = fieldLeft - 25 + i * 5;
                g2.drawLine(x, goalTop, x, goalTop + goalHeight);
            }

            // Right goal
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(fieldRight, goalTop, fieldRight + 25, goalTop);
            g2.drawLine(fieldRight, goalTop + goalHeight, fieldRight + 25, goalTop + goalHeight);
            g2.drawLine(fieldRight + 25, goalTop, fieldRight + 25, goalTop + goalHeight);

            // Net pattern right
            g2.setColor(new Color(255, 255, 255, 60));
            g2.setStroke(new BasicStroke(1f));
            for (int i = 0; i <= 5; i++) {
                int y = goalTop + i * (goalHeight / 5);
                g2.drawLine(fieldRight, y, fieldRight + 25, y);
            }
            for (int i = 0; i <= 5; i++) {
                int x = fieldRight + i * 5;
                g2.drawLine(x, goalTop, x, goalTop + goalHeight);
            }
        }

        private void drawHUD(Graphics2D g2) {
            // Score bar background
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRoundRect(getWidth() / 2 - 130, 10, 260, 50, 15, 15);

            // Scores
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            g2.setColor(new Color(34, 200, 84));
            String leftScore = String.format("%02d", scoreLeft);
            g2.drawString(leftScore, getWidth() / 2 - 90, 45);

            g2.setColor(Color.WHITE);
            g2.drawString(":", getWidth() / 2 - 10, 43);

            Color rightColor = (parent.getCurrentMode() == GameMode.VS_COMPUTER) ?
                new Color(231, 76, 60) : new Color(52, 152, 219);
            g2.setColor(rightColor);
            String rightScore = String.format("%02d", scoreRight);
            g2.drawString(rightScore, getWidth() / 2 + 30, 45);

            // Timer
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.setColor(Color.WHITE);
            int mins = matchTime / 60;
            int secs = matchTime % 60;
            String timeStr = String.format("%d:%02d", mins, secs);
            int tw = g2.getFontMetrics().stringWidth(timeStr);
            g2.drawString(timeStr, getWidth() / 2 - tw / 2, 72);

            // Mode indicator
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.setColor(new Color(200, 200, 200));
            String mode = (parent.getCurrentMode() == GameMode.VS_COMPUTER) ?
                "VS COMPUTER" : "2 PLAYERS";
            g2.drawString(mode, 100, 30);
        }

        private void drawGameOver(Graphics2D g2) {
            // Overlay
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setFont(new Font("Arial", Font.BOLD, 48));
            g2.setColor(Color.WHITE);
            String result;
            if (scoreLeft > scoreRight) result = "GREEN TEAM WINS!";
            else if (scoreRight > scoreLeft) {
                result = (parent.getCurrentMode() == GameMode.VS_COMPUTER) ? "COMPUTER WINS!" : "BLUE TEAM WINS!";
            } else result = "DRAW!";

            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(result)) / 2;
            g2.drawString(result, tx, getHeight() / 2 - 20);

            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            g2.setColor(new Color(200, 200, 200));
            String sub = "Final Score: " + scoreLeft + " - " + scoreRight;
            int sx = (getWidth() - g2.getFontMetrics().stringWidth(sub)) / 2;
            g2.drawString(sub, sx, getHeight() / 2 + 20);

            g2.setFont(new Font("Arial", Font.PLAIN, 16));
            g2.setColor(new Color(180, 180, 180));
            String hint = "Press MENU to return";
            int hx = (getWidth() - g2.getFontMetrics().stringWidth(hint)) / 2;
            g2.drawString(hint, hx, getHeight() / 2 + 60);
        }

        // ============================================================
        //  KEY LISTENER
        // ============================================================
        @Override
        public void keyPressed(KeyEvent e) {
            keysPressed.add(e.getKeyCode());
        }

        @Override
        public void keyReleased(KeyEvent e) {
            keysPressed.remove(e.getKeyCode());
        }

        @Override
        public void keyTyped(KeyEvent e) {}
    }

    // ============================================================
    //  SOCCER BALL
    // ============================================================
    static class SoccerBall {
        double x, y;
        double vx, vy;
        double radius = 10;
        double rotation = 0;
        List<Point2D.Double> trail = new ArrayList<>();

        public SoccerBall(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void update(double dt) {
            x += vx;
            y += vy;
            vx *= BALL_FRICTION;
            vy *= BALL_FRICTION;
            if (Math.abs(vx) < 0.05) vx = 0;
            if (Math.abs(vy) < 0.05) vy = 0;

            double speed = Math.sqrt(vx * vx + vy * vy);
            rotation += speed * 0.08;
        }

        public void addTrailPoint() {
            trail.add(new Point2D.Double(x, y));
            if (trail.size() > 15) trail.remove(0);
        }

        public void drawTrail(Graphics2D g2) {
            for (int i = 0; i < trail.size(); i++) {
                Point2D.Double p = trail.get(i);
                float alpha = (float) i / trail.size();
                int r = (int)(radius * alpha * 0.6);
                if (r > 0) {
                    g2.setColor(new Color(255, 255, 255, (int)(alpha * 80)));
                    g2.fillOval((int)(p.x - r), (int)(p.y - r), r * 2, r * 2);
                }
            }
        }

        public void draw(Graphics2D g2) {
            // Shadow
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillOval((int)(x - radius + 2), (int)(y - radius + 4), (int)(radius * 2), (int)(radius * 2));

            g2 = (Graphics2D) g2.create();
            g2.translate(x, y);
            g2.rotate(rotation);

            // Ball body with gradient
            RadialGradientPaint rgp = new RadialGradientPaint(
                (float)(-radius / 3), (float)(-radius / 3), (float)(radius * 1.5f),
                new float[]{0f, 1f},
                new Color[]{new Color(250, 250, 250), new Color(170, 170, 180)});
            g2.setPaint(rgp);
            g2.fillOval((int)(-radius), (int)(-radius), (int)(radius * 2), (int)(radius * 2));

            // Pentagon pattern
            g2.setColor(new Color(30, 30, 40));
            g2.fillPolygon(new int[]{0, -5, -3, 3, 5}, new int[]{-8, -3, 2, 2, -3}, 5);
            g2.fillPolygon(new int[]{-9, -12, -10, -7, -6}, new int[]{1, 5, 9, 9, 5}, 5);
            g2.fillPolygon(new int[]{9, 12, 10, 7, 6}, new int[]{1, 5, 9, 9, 5}, 5);
            g2.fillPolygon(new int[]{-4, -7, -5, 5, 7}, new int[]{8, 11, 14, 14, 11}, 5);

            // Outline
            g2.setColor(new Color(50, 50, 60));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval((int)(-radius), (int)(-radius), (int)(radius * 2), (int)(radius * 2));

            g2.dispose();
        }
    }

    // ============================================================
    //  SOCCER PLAYER
    // ============================================================
    static class SoccerPlayer {
        double x, y;
        double vx, vy;
        double radius = 14;
        int team; // 0 = left, 1 = right
        Color color;
        String label;
        double legSwing = 0;
        boolean moving = false;

        public SoccerPlayer(double x, double y, int team, Color color, String label) {
            this.x = x;
            this.y = y;
            this.team = team;
            this.color = color;
            this.label = label;
        }

        public void update(double dt, int fieldLeft, int fieldRight, int fieldTop, int fieldBottom) {
            x += vx;
            y += vy;

            // Keep in field
            x = Math.max(fieldLeft + radius, Math.min(fieldRight - radius, x));
            y = Math.max(fieldTop + radius, Math.min(fieldBottom - radius, y));

            double speed = Math.sqrt(vx * vx + vy * vy);
            moving = speed > 0.5;
            if (moving) {
                legSwing += speed * 0.15;
            } else {
                legSwing *= 0.8;
            }
        }

        public void draw(Graphics2D g2) {
            // Shadow
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval((int)(x - radius + 2), (int)(y + radius - 3), (int)(radius * 2), (int)(radius * 0.6));

            // Legs (animated)
            double legOffset = Math.sin(legSwing) * 4;
            g2.setColor(new Color(40, 40, 50));
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int)(x - 4), (int)(y + radius - 2), (int)(x - 4 + legOffset), (int)(y + radius + 6));
            g2.drawLine((int)(x + 4), (int)(y + radius - 2), (int)(x + 4 - legOffset), (int)(y + radius + 6));

            // Body
            RadialGradientPaint bodyPaint = new RadialGradientPaint(
                (float)(x - radius / 3), (float)(y - radius / 3), (float)(radius * 1.5f),
                new float[]{0f, 1f},
                new Color[]{color.brighter(), color.darker()});
            g2.setPaint(bodyPaint);
            g2.fillOval((int)(x - radius), (int)(y - radius), (int)(radius * 2), (int)(radius * 2));

            // Jersey outline
            g2.setColor(color.darker().darker());
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval((int)(x - radius), (int)(y - radius), (int)(radius * 2), (int)(radius * 2));

            // Team indicator dot
            g2.setColor(team == 0 ? new Color(255, 255, 100) : new Color(255, 200, 100));
            g2.fillOval((int)(x - 3), (int)(y - radius + 2), 6, 6);

            // Label
            g2.setFont(new Font("Arial", Font.BOLD, 9));
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            int lw = fm.stringWidth(label);
            g2.drawString(label, (int)(x - lw / 2), (int)(y + radius + 18));
        }
    }

    // ============================================================
    //  PARTICLE
    // ============================================================
    static class Particle {
        double x, y, vx, vy;
        Color color;
        double life;
        double maxLife;
        double size;

        public Particle(double x, double y, double vx, double vy, Color color, double life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.life = life;
            this.maxLife = life;
            this.size = 2 + Math.random() * 3;
        }

        public void update(double dt) {
            x += vx;
            y += vy;
            vx *= 0.95;
            vy *= 0.95;
            vy += 0.1; // slight gravity
            life -= dt;
        }

        public void draw(Graphics2D g2) {
            float alpha = (float)(life / maxLife);
            if (alpha < 0) alpha = 0;
            if (alpha > 1) alpha = 1;
            int r = (int)(color.getRed() * alpha);
            int gr = (int)(color.getGreen() * alpha);
            int b = (int)(color.getBlue() * alpha);
            g2.setColor(new Color(r, gr, b, (int)(alpha * 255)));
            int s = (int)(size * alpha);
            if (s > 0) g2.fillOval((int)(x - s), (int)(y - s), s * 2, s * 2);
        }
    }

    // ============================================================
    //  GOAL CELEBRATION
    // ============================================================
    static class GoalCelebration {
        String text;
        Color color;
        double x, y;
        double life = 2.5;
        double maxLife = 2.5;
        double scale = 0;

        public GoalCelebration(String text, Color color, double x, double y) {
            this.text = text;
            this.color = color;
            this.x = x;
            this.y = y;
        }

        public void update(double dt) {
            life -= dt;
            double progress = 1 - (life / maxLife);
            // Bounce-in scale animation
            if (progress < 0.3) {
                scale = progress / 0.3 * 1.3;
            } else if (progress < 0.4) {
                scale = 1.3 - (progress - 0.3) / 0.1 * 0.3;
            } else {
                scale = 1.0;
            }
        }

        public void draw(Graphics2D g2) {
            float alpha = (float)(life / maxLife);
            if (alpha < 0) alpha = 0;
            if (alpha > 1) alpha = 1;

            g2 = (Graphics2D) g2.create();
            g2.translate(x, y);
            g2.scale(scale, scale);

            g2.setFont(new Font("Arial", Font.BOLD, 80));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);

            // Glow
            for (int i = 0; i < 8; i++) {
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int)(alpha * 30)));
                g2.drawString(text, -tw / 2 - i, 0);
                g2.drawString(text, -tw / 2 + i, 0);
                g2.drawString(text, -tw / 2, 0 - i);
                g2.drawString(text, -tw / 2, 0 + i);
            }

            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                (int)(alpha * 255)));
            g2.drawString(text, -tw / 2, 0);

            g2.dispose();
        }
    }
}
