import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

public class VolleyballGameSimulation extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VolleyballGameSimulation sim = new VolleyballGameSimulation();
            sim.setVisible(true);
        });
    }

    // Core panel where everything is drawn
    private GamePanel gamePanel;

    public VolleyballGameSimulation() {
        setTitle("Volleyball Game Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        setContentPane(gamePanel);
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * GamePanel encapsulates the entire simulation and rendering logic.
     */
    static class GamePanel extends JPanel implements ActionListener, KeyListener {

        // Panel size
        private static final int PREF_W = 960;
        private static final int PREF_H = 540;

        // Court dimensions
        private static final int FLOOR_Y = 420;
        private static final int NET_X = PREF_W / 2;
        private static final int NET_WIDTH = 10;
        private static final int NET_HEIGHT = 160;

        // Ball constants
        private static final int BALL_RADIUS = 12;
        private static final double GRAVITY = 0.45;
        private static final double BALL_DAMPING = 0.78;
        private static final double BALL_MAX_SPEED_Y = 18.0;

        // Player constants
        private static final int PLAYER_WIDTH = 28;
        private static final int PLAYER_HEIGHT = 60;
        private static final double PLAYER_MOVE_SPEED = 4.0;
        private static final double PLAYER_JUMP_SPEED = -11.5;
        private static final double PLAYER_GRAVITY = 0.55;
        private static final double PLAYER_FRICTION = 0.85;

        // Game logic constants
        private static final int MAX_SCORE = 25;
        private static final int WIN_BY = 2;
        private static final int POINT_DELAY_FRAMES = 160;

        // Timer
        private Timer timer;
        private int frameDelayMs = 16; // ~60 FPS

        // World state
        private Ball ball;
        private Player leftPlayer;
        private Player rightPlayer;

        // Input flags
        private boolean leftMoveLeft;
        private boolean leftMoveRight;
        private boolean leftJump;
        private boolean leftHit;

        // Game state flags
        private boolean paused = false;
        private boolean rallyInProgress = true;
        private boolean pointJustScored = false;
        private int pointDelayCounter = 0;

        // Score
        private int leftScore = 0;
        private int rightScore = 0;
        private int leftSets = 0;
        private int rightSets = 0;

        // Randomness
        private Random random = new Random();

        // Message to show on screen (e.g., "Left scores!", "Right wins set!")
        private String statusMessage = "";

        // Simple match configuration: best-of-5 sets, normal volley rules simplified
        private int currentSetNumber = 1;

        // AI control flags
        private boolean aiEnabledForRight = true;

        // Hit cooldowns
        private int leftHitCooldown = 0;
        private int rightHitCooldown = 0;

        // Colors
        private Color courtColor = new Color(78, 154, 6);
        private Color skyColor = new Color(135, 206, 235);
        private Color sandColor = new Color(237, 201, 175);
        private Color netColor = Color.WHITE;

        public GamePanel() {
            setPreferredSize(new Dimension(PREF_W, PREF_H));
            setBackground(Color.BLACK);
            setFocusable(true);
            requestFocusInWindow();
            addKeyListener(this);

            resetWholeMatch();

            timer = new Timer(frameDelayMs, this);
            timer.start();
        }

        // ========== Core game reset methods ==========

        private void resetWholeMatch() {
            leftScore = 0;
            rightScore = 0;
            leftSets = 0;
            rightSets = 0;
            currentSetNumber = 1;
            resetRally(true);
            statusMessage = "New match started. Left to serve first.";
        }

        private void resetSet(boolean leftServe) {
            leftScore = 0;
            rightScore = 0;
            resetRally(leftServe);
            statusMessage = "Set " + currentSetNumber + " started. " +
                    (leftServe ? "Left" : "Right") + " serves.";
        }

        private void resetRally(boolean leftServe) {
            // Reset ball in the serving half
            if (leftServe) {
                ball = new Ball(NET_X - 120, FLOOR_Y - 150, 0, 0);
            } else {
                ball = new Ball(NET_X + 120, FLOOR_Y - 150, 0, 0);
            }

            leftPlayer = new Player(200, FLOOR_Y - PLAYER_HEIGHT);
            rightPlayer = new Player(PREF_W - 200 - PLAYER_WIDTH, FLOOR_Y - PLAYER_HEIGHT);

            rallyInProgress = true;
            pointJustScored = false;
            pointDelayCounter = 0;
            leftHitCooldown = 0;
            rightHitCooldown = 0;
        }

        // ========== Game loop ==========

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!paused) {
                updateGame();
                repaint();
            }
        }

        private void updateGame() {
            if (!rallyInProgress) {
                handlePointDelay();
                return;
            }

            // Update players
            updateLeftPlayer();
            updateRightPlayerAI();

            // Update ball physics
            updateBallPhysics();

            // Handle collisions
            handleBallPlayerCollisions();
            handleBallNetCollision();

            // Check rally end
            checkRallyEnd();

            // Decrease cooldowns
            if (leftHitCooldown > 0) leftHitCooldown--;
            if (rightHitCooldown > 0) rightHitCooldown--;
        }

        private void handlePointDelay() {
            if (pointJustScored) {
                pointDelayCounter++;
                if (pointDelayCounter >= POINT_DELAY_FRAMES) {
                    boolean leftServe = rightScore > leftScore;
                    pointJustScored = false;
                    resetRally(leftServe);
                }
            }
        }

        // ========== Player logic ==========

        private void updateLeftPlayer() {
            // Horizontal movement from input
            if (leftMoveLeft && !leftMoveRight) {
                leftPlayer.vx -= PLAYER_MOVE_SPEED * 0.4;
            } else if (leftMoveRight && !leftMoveLeft) {
                leftPlayer.vx += PLAYER_MOVE_SPEED * 0.4;
            } else {
                leftPlayer.vx *= PLAYER_FRICTION;
            }

            // Jump
            if (leftJump && leftPlayer.onGround) {
                leftPlayer.vy = PLAYER_JUMP_SPEED;
                leftPlayer.onGround = false;
            }

            // Apply gravity
            leftPlayer.vy += PLAYER_GRAVITY;

            // Apply position
            leftPlayer.x += leftPlayer.vx;
            leftPlayer.y += leftPlayer.vy;

            // Clamp to court and floor
            if (leftPlayer.y + PLAYER_HEIGHT >= FLOOR_Y) {
                leftPlayer.y = FLOOR_Y - PLAYER_HEIGHT;
                leftPlayer.vy = 0;
                leftPlayer.onGround = true;
            } else {
                leftPlayer.onGround = false;
            }

            if (leftPlayer.x < 40) {
                leftPlayer.x = 40;
                leftPlayer.vx = 0;
            }

            if (leftPlayer.x + PLAYER_WIDTH > NET_X - 20) {
                leftPlayer.x = NET_X - 20 - PLAYER_WIDTH;
                leftPlayer.vx = 0;
            }

            // Player hit
            if (leftHit && leftHitCooldown == 0) {
                tryHitBall(leftPlayer, true);
                leftHitCooldown = 40;
            }
        }

        private void updateRightPlayerAI() {
            if (!aiEnabledForRight) {
                updateRightPlayerNoAI();
                return;
            }

            // Simple AI: move toward the predicted landing point or ball position
            double targetX = predictBallLandingX();
            if (Double.isNaN(targetX)) {
                targetX = ball.x;
            }

            // Keep AI on right side
            double desiredX = Math.max(NET_X + 20, Math.min(targetX, PREF_W - 40));

            if (desiredX < rightPlayer.getCenterX() - 8) {
                rightPlayer.vx -= PLAYER_MOVE_SPEED * 0.3;
            } else if (desiredX > rightPlayer.getCenterX() + 8) {
                rightPlayer.vx += PLAYER_MOVE_SPEED * 0.3;
            } else {
                rightPlayer.vx *= PLAYER_FRICTION;
            }

            // Jump AI: if ball is coming down nearby, attempt jump
            if (ball.y < rightPlayer.y - 40 && ball.vy > 0 && rightPlayer.onGround) {
                if (Math.abs(ball.x - rightPlayer.getCenterX()) < 80) {
                    rightPlayer.vy = PLAYER_JUMP_SPEED;
                    rightPlayer.onGround = false;
                }
            }

            // Apply gravity
            rightPlayer.vy += PLAYER_GRAVITY;

            // Apply position
            rightPlayer.x += rightPlayer.vx;
            rightPlayer.y += rightPlayer.vy;

            // Clamp to floor
            if (rightPlayer.y + PLAYER_HEIGHT >= FLOOR_Y) {
                rightPlayer.y = FLOOR_Y - PLAYER_HEIGHT;
                rightPlayer.vy = 0;
                rightPlayer.onGround = true;
            } else {
                rightPlayer.onGround = false;
            }

            // Clamp horizontal
            if (rightPlayer.x < NET_X + 20) {
                rightPlayer.x = NET_X + 20;
                rightPlayer.vx = 0;
            }
            if (rightPlayer.x + PLAYER_WIDTH > PREF_W - 40) {
                rightPlayer.x = PREF_W - 40 - PLAYER_WIDTH;
                rightPlayer.vx = 0;
            }

            // AI hit logic
            if (rightHitCooldown == 0) {
                if (Math.abs(ball.x - rightPlayer.getCenterX()) < 40 &&
                        ball.y < rightPlayer.y + 5 &&
                        ball.y > rightPlayer.y - 80) {
                    tryHitBall(rightPlayer, false);
                    rightHitCooldown = 40;
                }
            }
        }

        private void updateRightPlayerNoAI() {
            // Placeholder for manual control if desired
            rightPlayer.vx *= PLAYER_FRICTION;
            rightPlayer.vy += PLAYER_GRAVITY;
            rightPlayer.x += rightPlayer.vx;
            rightPlayer.y += rightPlayer.vy;

            if (rightPlayer.y + PLAYER_HEIGHT >= FLOOR_Y) {
                rightPlayer.y = FLOOR_Y - PLAYER_HEIGHT;
                rightPlayer.vy = 0;
                rightPlayer.onGround = true;
            } else {
                rightPlayer.onGround = false;
            }

            if (rightPlayer.x < NET_X + 20) {
                rightPlayer.x = NET_X + 20;
                rightPlayer.vx = 0;
            }
            if (rightPlayer.x + PLAYER_WIDTH > PREF_W - 40) {
                rightPlayer.x = PREF_W - 40 - PLAYER_WIDTH;
                rightPlayer.vx = 0;
            }
        }

        // Predict X position where ball will land on floor, used by AI
        private double predictBallLandingX() {
            double simX = ball.x;
            double simY = ball.y;
            double simVx = ball.vx;
            double simVy = ball.vy;
            int steps = 0;

            while (steps < 240) {
                simVy += GRAVITY;
                if (simVy > BALL_MAX_SPEED_Y) simVy = BALL_MAX_SPEED_Y;
                simX += simVx;
                simY += simVy;
                if (simY + BALL_RADIUS >= FLOOR_Y) {
                    return simX;
                }
                steps++;
            }
            return Double.NaN;
        }

        // ========== Ball physics and collisions ==========

        private void updateBallPhysics() {
            ball.vy += GRAVITY;
            if (ball.vy > BALL_MAX_SPEED_Y) {
                ball.vy = BALL_MAX_SPEED_Y;
            }

            ball.x += ball.vx;
            ball.y += ball.vy;

            if (ball.y + BALL_RADIUS >= FLOOR_Y) {
                ball.y = FLOOR_Y - BALL_RADIUS;
                ball.vy = -Math.abs(ball.vy) * BALL_DAMPING;
                ball.vx *= 0.96;
                if (Math.abs(ball.vy) < 1.4) {
                    ball.vy = 0;
                }
            }

            if (ball.y - BALL_RADIUS < 60) {
                ball.y = 60 + BALL_RADIUS;
                ball.vy = Math.abs(ball.vy) * BALL_DAMPING;
            }

            if (ball.x - BALL_RADIUS < 40) {
                ball.x = 40 + BALL_RADIUS;
                ball.vx = Math.abs(ball.vx) * BALL_DAMPING;
            }
            if (ball.x + BALL_RADIUS > PREF_W - 40) {
                ball.x = PREF_W - 40 - BALL_RADIUS;
                ball.vx = -Math.abs(ball.vx) * BALL_DAMPING;
            }
        }

        private void handleBallPlayerCollisions() {
            handleBallPlayerCollision(ball, leftPlayer);
            handleBallPlayerCollision(ball, rightPlayer);
        }

        private void handleBallPlayerCollision(Ball ball, Player player) {
            Rectangle2D playerRect = new Rectangle2D.Double(
                    player.x,
                    player.y,
                    PLAYER_WIDTH,
                    PLAYER_HEIGHT
            );
            Ellipse2D ballCircle = new Ellipse2D.Double(
                    ball.x - BALL_RADIUS,
                    ball.y - BALL_RADIUS,
                    BALL_RADIUS * 2,
                    BALL_RADIUS * 2
            );

            if (ballCircle.intersects(playerRect)) {
                double px = player.getCenterX();
                double py = player.getCenterY();
                double dx = ball.x - px;
                double dy = ball.y - py;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist == 0) dist = 0.01;

                double overlap = BALL_RADIUS + PLAYER_WIDTH / 2.0 - dist;
                if (overlap < 0) overlap = 2;

                ball.x += (dx / dist) * overlap * 0.6;
                ball.y += (dy / dist) * overlap * 0.6;

                double relativeVx = ball.vx - player.vx;
                double relativeVy = ball.vy - player.vy;
                double dot = relativeVx * (dx / dist) + relativeVy * (dy / dist);

                if (dot < 0) {
                    ball.vx -= 1.2 * dot * (dx / dist);
                    ball.vy -= 1.2 * dot * (dy / dist);
                }

                ball.vx += player.vx * 0.3;
                ball.vy += player.vy * 0.3;
            }
        }

        private void handleBallNetCollision() {
            Rectangle2D netRect = new Rectangle2D.Double(
                    NET_X - NET_WIDTH / 2.0,
                    FLOOR_Y - NET_HEIGHT,
                    NET_WIDTH,
                    NET_HEIGHT
            );

            Ellipse2D ballCircle = new Ellipse2D.Double(
                    ball.x - BALL_RADIUS,
                    ball.y - BALL_RADIUS,
                    BALL_RADIUS * 2,
                    BALL_RADIUS * 2
            );

            if (ballCircle.intersects(netRect)) {
                if (ball.x < NET_X) {
                    ball.x = NET_X - NET_WIDTH / 2.0 - BALL_RADIUS - 1;
                    ball.vx = -Math.abs(ball.vx) * 0.7;
                } else {
                    ball.x = NET_X + NET_WIDTH / 2.0 + BALL_RADIUS + 1;
                    ball.vx = Math.abs(ball.vx) * 0.7;
                }
                ball.vy *= 0.8;
            }
        }

        // ========== Rally and scoring logic ==========

        private void checkRallyEnd() {
            if (ball.y + BALL_RADIUS >= FLOOR_Y - 1) {
                rallyInProgress = false;
                pointJustScored = true;
                pointDelayCounter = 0;

                if (ball.x < NET_X) {
                    rightScorePoint();
                } else {
                    leftScorePoint();
                }
            }
        }

        private void leftScorePoint() {
            leftScore++;
            statusMessage = "Left scores! Rally over.";
            checkSetEnd();
        }

        private void rightScorePoint() {
            rightScore++;
            statusMessage = "Right scores! Rally over.";
            checkSetEnd();
        }

        private void checkSetEnd() {
            if (isSetWon()) {
                if (leftScore > rightScore) {
                    leftSets++;
                    statusMessage = "Left wins set " + currentSetNumber + "!";
                } else {
                    rightSets++;
                    statusMessage = "Right wins set " + currentSetNumber + "!";
                }

                if (isMatchWon()) {
                    statusMessage += " Match over.";
                } else {
                    currentSetNumber++;
                    boolean leftServeNextSet = currentSetNumber % 2 == 1;
                    resetSet(leftServeNextSet);
                    rallyInProgress = false;
                    pointJustScored = true;
                    pointDelayCounter = 0;
                }
            }
        }

        private boolean isSetWon() {
            if (leftScore >= MAX_SCORE || rightScore >= MAX_SCORE) {
                int diff = Math.abs(leftScore - rightScore);
                return diff >= WIN_BY;
            }
            return false;
        }

        private boolean isMatchWon() {
            int setsNeeded = 3;
            return leftSets >= setsNeeded || rightSets >= setsNeeded;
        }

        // ========== Hit logic ==========

        private void tryHitBall(Player player, boolean isLeft) {
            double dx = ball.x - player.getCenterX();
            double dy = ball.y - (player.y - 8);

            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance > 80) {
                return;
            }

            double targetX;
            double targetY = FLOOR_Y - 260;

            if (isLeft) {
                targetX = NET_X + 120 + random.nextInt(80);
            } else {
                targetX = NET_X - 120 - random.nextInt(80);
            }

            double time = 28.0;

            double vx = (targetX - ball.x) / time;
            double vy = (targetY - ball.y - 0.5 * GRAVITY * time * time) / time;

            double maxSpeed = 14.0;
            double speed = Math.sqrt(vx * vx + vy * vy);
            if (speed > maxSpeed) {
                double scale = maxSpeed / speed;
                vx *= scale;
                vy *= scale;
            }

            ball.vx = vx;
            ball.vy = vy;

            double dir = isLeft ? 1.0 : -1.0;
            ball.x += dir * 4;
            ball.y -= 4;

            statusMessage = (isLeft ? "Left" : "Right") + " hits!";
        }

        // ========== Rendering ==========

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            drawBackground(g2);
            drawCourt(g2);
            drawNet(g2);
            drawBall(g2);
            drawPlayers(g2);
            drawHUD(g2);

            g2.dispose();
        }

        private void drawBackground(Graphics2D g2) {
            g2.setColor(skyColor);
            g2.fillRect(0, 0, getWidth(), FLOOR_Y);

            g2.setColor(sandColor);
            g2.fillRect(0, FLOOR_Y, getWidth(), getHeight() - FLOOR_Y);

            g2.setColor(new Color(255, 255, 0, 200));
            g2.fillOval(60, 60, 80, 80);
        }

        private void drawCourt(Graphics2D g2) {
            g2.setColor(courtColor);
            g2.fillRect(40, FLOOR_Y - 6, PREF_W - 80, 6);

            g2.setColor(new Color(220, 220, 220));
            g2.drawLine(40, FLOOR_Y - 1, PREF_W - 40, FLOOR_Y - 1);

            g2.setColor(new Color(200, 200, 200));
            g2.drawLine(40, FLOOR_Y - 1, PREF_W - 40, FLOOR_Y - 1);
        }

        private void drawNet(Graphics2D g2) {
            int baseY = FLOOR_Y;
            int topY = FLOOR_Y - NET_HEIGHT;

            g2.setColor(new Color(139, 69, 19));
            g2.fillRect(NET_X - 5, topY - 20, 10, 20);
            g2.fillRect(NET_X - 5, baseY, 10, 20);

            g2.setColor(netColor);
            g2.fillRect(NET_X - NET_WIDTH / 2, topY, NET_WIDTH, NET_HEIGHT);

            g2.setColor(new Color(220, 220, 220));
            int gridSpacing = 12;
            for (int y = topY; y <= baseY; y += gridSpacing) {
                g2.drawLine(NET_X - NET_WIDTH / 2, y, NET_X + NET_WIDTH / 2, y);
            }
        }

        private void drawBall(Graphics2D g2) {
            g2.setColor(Color.WHITE);
            g2.fillOval(
                    (int) (ball.x - BALL_RADIUS),
                    (int) (ball.y - BALL_RADIUS),
                    BALL_RADIUS * 2,
                    BALL_RADIUS * 2
            );
            g2.setColor(Color.DARK_GRAY);
            g2.drawOval(
                    (int) (ball.x - BALL_RADIUS),
                    (int) (ball.y - BALL_RADIUS),
                    BALL_RADIUS * 2,
                    BALL_RADIUS * 2
            );

            g2.setColor(new Color(240, 240, 0));
            g2.drawArc(
                    (int) (ball.x - BALL_RADIUS + 3),
                    (int) (ball.y - BALL_RADIUS + 3),
                    BALL_RADIUS * 2 - 6,
                    BALL_RADIUS * 2 - 6,
                    30,
                    120
            );
            g2.setColor(new Color(0, 100, 255));
            g2.drawArc(
                    (int) (ball.x - BALL_RADIUS + 3),
                    (int) (ball.y - BALL_RADIUS + 3),
                    BALL_RADIUS * 2 - 6,
                    BALL_RADIUS * 2 - 6,
                    210,
                    120
            );
        }

        private void drawPlayers(Graphics2D g2) {
            drawPlayer(g2, leftPlayer, new Color(52, 152, 219));
            drawPlayer(g2, rightPlayer, new Color(231, 76, 60));
        }

        private void drawPlayer(Graphics2D g2, Player p, Color jerseyColor) {
            int x = (int) p.x;
            int y = (int) p.y;

            g2.setColor(new Color(160, 82, 45));
            int headRadius = 14;
            int headX = x + PLAYER_WIDTH / 2 - headRadius;
            int headY = y - headRadius;
            g2.fillOval(headX, headY, headRadius * 2, headRadius * 2);

            g2.setColor(jerseyColor);
            int bodyWidth = PLAYER_WIDTH;
            int bodyHeight = PLAYER_HEIGHT - 16;
            g2.fillRect(x, y, bodyWidth, bodyHeight);

            g2.setColor(Color.BLACK);
            int legWidth = 6;
            int legHeight = 18;
            int legY = y + bodyHeight;
            g2.fillRect(x + 4, legY, legWidth, legHeight);
            g2.fillRect(x + bodyWidth - legWidth - 4, legY, legWidth, legHeight);

            g2.setColor(new Color(160, 82, 45));
            int armWidth = 5;
            int armHeight = 18;
            int armY = y + 6;
            g2.fillRect(x - armWidth, armY, armWidth, armHeight);
            g2.fillRect(x + bodyWidth, armY, armWidth, armHeight);
        }

        private void drawHUD(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(20, 14, PREF_W - 40, 60, 16, 16);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));

            String scoreText = "Set " + currentSetNumber +
                    " | Left " + leftScore +
                    " - " + rightScore + " Right";
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(scoreText);
            g2.drawString(scoreText, PREF_W / 2 - textWidth / 2, 40);

            String setsText = "Sets  " + leftSets + " : " + rightSets;
            g2.drawString(setsText, PREF_W / 2 - fm.stringWidth(setsText) / 2, 60);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            String controls = "Controls: A/D move, W jump, Space hit, P pause, R reset, +/- speed";
            g2.drawString(controls, 30, 40);

            if (!statusMessage.isEmpty()) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 18));
                g2.setColor(new Color(255, 255, 255, 230));
                String msg = statusMessage;
                int w = g2.getFontMetrics().stringWidth(msg);
                g2.drawString(msg, PREF_W / 2 - w / 2, 90);
            }

            if (paused) {
                g2.setColor(new Color(0, 0, 0, 130));
                g2.fillRect(0, 0, PREF_W, PREF_H);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 40));
                String pText = "PAUSED";
                int w = g2.getFontMetrics().stringWidth(pText);
                g2.drawString(pText, PREF_W / 2 - w / 2, PREF_H / 2);
            }
        }

        // ========== KeyListener ==========

        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            switch (code) {
                case KeyEvent.VK_A:
                    leftMoveLeft = true;
                    break;
                case KeyEvent.VK_D:
                    leftMoveRight = true;
                    break;
                case KeyEvent.VK_W:
                    leftJump = true;
                    break;
                case KeyEvent.VK_SPACE:
                    leftHit = true;
                    break;
                case KeyEvent.VK_P:
                    paused = !paused;
                    statusMessage = paused ? "Paused" : "Resumed";
                    break;
                case KeyEvent.VK_R:
                    resetWholeMatch();
                    break;
                case KeyEvent.VK_PLUS:
                case KeyEvent.VK_EQUALS:
                case KeyEvent.VK_ADD:
                    speedUp();
                    break;
                case KeyEvent.VK_MINUS:
                case KeyEvent.VK_SUBTRACT:
                    slowDown();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();
            switch (code) {
                case KeyEvent.VK_A:
                    leftMoveLeft = false;
                    break;
                case KeyEvent.VK_D:
                    leftMoveRight = false;
                    break;
                case KeyEvent.VK_W:
                    leftJump = false;
                    break;
                case KeyEvent.VK_SPACE:
                    leftHit = false;
                    break;
                default:
                    break;
            }
        }

        private void speedUp() {
            frameDelayMs = Math.max(4, frameDelayMs - 2);
            timer.setDelay(frameDelayMs);
            statusMessage = "Speed: " + (1000 / frameDelayMs) + " FPS approx";
        }

        private void slowDown() {
            frameDelayMs = Math.min(40, frameDelayMs + 2);
            timer.setDelay(frameDelayMs);
            statusMessage = "Speed: " + (1000 / frameDelayMs) + " FPS approx";
        }

        // ========== Internal helper classes ==========

        private static class Ball {
            double x, y;
            double vx, vy;

            Ball(double x, double y, double vx, double vy) {
                this.x = x;
                this.y = y;
                this.vx = vx;
                this.vy = vy;
            }
        }

        private static class Player {
            double x, y;
            double vx, vy;
            boolean onGround;

            Player(double x, double y) {
                this.x = x;
                this.y = y;
                this.vx = 0;
                this.vy = 0;
                this.onGround = true;
            }

            double getCenterX() {
                return x + PLAYER_WIDTH / 2.0;
            }

            double getCenterY() {
                return y + PLAYER_HEIGHT / 2.0;
            }
        }
    }
}
