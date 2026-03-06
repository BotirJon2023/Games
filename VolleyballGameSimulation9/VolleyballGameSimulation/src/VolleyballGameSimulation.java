import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.Random;
import javax.swing.*;

public class VolleyballGameSimulation {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(VolleyballGameSimulation::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Volleyball Game Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        GamePanel panel = new GamePanel();
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        panel.start();
    }

    // ---------------------------------------------------------------------
    // GamePanel: core of the simulation
    // ---------------------------------------------------------------------
    static class GamePanel extends JPanel implements ActionListener, KeyListener {

        // Logical size
        private static final int WIDTH = 960;
        private static final int HEIGHT = 540;

        // Ground and net
        private static final int GROUND_Y = 460;
        private static final int NET_WIDTH = 12;
        private static final int NET_HEIGHT = 200;

        // Ball constants
        private static final double BALL_RADIUS = 14;
        private static final double GRAVITY = 0.45;
        private static final double BOUNCE_DAMPING = 0.78;
        private static final double AIR_DAMPING = 0.999;
        private static final double MAX_BALL_SPEED = 18.0;

        // Player constants
        private static final int PLAYER_WIDTH = 26;
        private static final int PLAYER_HEIGHT = 70;
        private static final double PLAYER_MOVE_SPEED = 5.0;
        private static final double PLAYER_JUMP_SPEED = -11.5;
        private static final double PLAYER_GRAVITY = 0.55;
        private static final double PLAYER_FRICTION = 0.85;
        private static final double PLAYER_MAX_VX = 7.0;

        // Game state
        private Ball ball;
        private Player leftPlayer;
        private Player rightPlayer;

        private int leftScore = 0;
        private int rightScore = 0;

        private boolean running = false;
        private boolean paused = false;
        private boolean showDebug = false;

        private Timer timer;
        private int tickDelay = 16; // ~60 FPS

        // Input flags
        private boolean leftMoveLeft;
        private boolean leftMoveRight;
        private boolean leftJump;
        private boolean leftHit;

        private boolean rightMoveLeft;
        private boolean rightMoveRight;
        private boolean rightJump;
        private boolean rightHit;

        // AI flags
        private boolean rightAIEnabled = true;
        private double aiReactionTimer = 0;
        private double aiDecisionInterval = 0.25;

        // Random
        private final Random random = new Random();

        // Fonts
        private Font scoreFont;
        private Font infoFont;
        private Font smallFont;

        // Animation helpers
        private double timeAccumulator = 0;
        private double timeSeconds = 0;

        // Camera shake (subtle)
        private double shakeTime = 0;
        private double shakeMagnitude = 0;

        // Rally / serve
        private boolean ballInPlay = false;
        private boolean leftServing = true;
        private double serveTimer = 0;
        private double serveDelay = 1.2;

        // UI toggles
        private boolean showTrails = true;
        private boolean showShadows = true;
        private boolean showNetShadow = true;

        // Ball trail
        private static final int TRAIL_SIZE = 20;
        private final double[] trailX = new double[TRAIL_SIZE];
        private final double[] trailY = new double[TRAIL_SIZE];
        private int trailIndex = 0;
        private boolean trailFilled = false;

        // Constructor
        GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setBackground(new Color(120, 180, 255));
            setFocusable(true);
            requestFocusInWindow();
            addKeyListener(this);

            initGameObjects();
            initFonts();

            timer = new Timer(tickDelay, this);
        }

        private void initFonts() {
            scoreFont = new Font("SansSerif", Font.BOLD, 40);
            infoFont = new Font("SansSerif", Font.BOLD, 18);
            smallFont = new Font("SansSerif", Font.PLAIN, 13);
        }

        private void initGameObjects() {
            ball = new Ball(WIDTH * 0.25, 200, 0, 0);

            leftPlayer = new Player(
                    WIDTH * 0.20,
                    GROUND_Y - PLAYER_HEIGHT,
                    PLAYER_WIDTH,
                    PLAYER_HEIGHT,
                    new Color(240, 240, 255),
                    "LEFT"
            );

            rightPlayer = new Player(
                    WIDTH * 0.80,
                    GROUND_Y - PLAYER_HEIGHT,
                    PLAYER_WIDTH,
                    PLAYER_HEIGHT,
                    new Color(255, 230, 230),
                    "RIGHT"
            );

            resetTrail();
        }

        private void resetTrail() {
            for (int i = 0; i < TRAIL_SIZE; i++) {
                trailX[i] = ball.x;
                trailY[i] = ball.y;
            }
            trailIndex = 0;
            trailFilled = false;
        }

        void start() {
            running = true;
            paused = false;
            timer.start();
        }

        void stop() {
            running = false;
            timer.stop();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!running) return;
            if (paused) {
                repaint();
                return;
            }

            double dt = tickDelay / 1000.0;
            timeSeconds += dt;
            timeAccumulator += dt;

            updateGame(dt);
            repaint();
        }

        // -----------------------------------------------------------------
        // Game update
        // -----------------------------------------------------------------
        private void updateGame(double dt) {
            updateServe(dt);
            updatePlayers(dt);
            updateBall(dt);
            updateAI(dt);
            updateCameraShake(dt);
        }

        private void updateServe(double dt) {
            if (!ballInPlay) {
                serveTimer += dt;
                if (serveTimer >= serveDelay) {
                    serveTimer = 0;
                    performServe();
                }
            }
        }

        private void performServe() {
            ballInPlay = true;
            resetTrail();

            double angle = Math.toRadians(40 + random.nextInt(20));
            double speed = 10 + random.nextDouble() * 4;

            if (leftServing) {
                ball.x = leftPlayer.x + leftPlayer.width + BALL_RADIUS + 5;
                ball.y = leftPlayer.y - 20;
                ball.vx = Math.cos(angle) * speed;
                ball.vy = -Math.sin(angle) * speed;
            } else {
                ball.x = rightPlayer.x - BALL_RADIUS - 5;
                ball.y = rightPlayer.y - 20;
                ball.vx = -Math.cos(angle) * speed;
                ball.vy = -Math.sin(angle) * speed;
            }
        }

        private void updatePlayers(double dt) {
            updatePlayer(leftPlayer, dt, leftMoveLeft, leftMoveRight, leftJump, leftHit, true);
            updatePlayer(rightPlayer, dt, rightMoveLeft, rightMoveRight, rightJump, rightHit, false);
        }

        private void updatePlayer(Player p,
                                  double dt,
                                  boolean moveLeft,
                                  boolean moveRight,
                                  boolean jump,
                                  boolean hit,
                                  boolean isLeft) {

            if (moveLeft) {
                p.vx -= PLAYER_MOVE_SPEED * dt * 60;
            }
            if (moveRight) {
                p.vx += PLAYER_MOVE_SPEED * dt * 60;
            }

            if (!moveLeft && !moveRight) {
                p.vx *= PLAYER_FRICTION;
            }

            if (p.vx > PLAYER_MAX_VX) p.vx = PLAYER_MAX_VX;
            if (p.vx < -PLAYER_MAX_VX) p.vx = -PLAYER_MAX_VX;

            if (jump && p.onGround) {
                p.vy = PLAYER_JUMP_SPEED;
                p.onGround = false;
            }

            p.vy += PLAYER_GRAVITY;

            p.x += p.vx;
            p.y += p.vy;

            if (p.y + p.height >= GROUND_Y) {
                p.y = GROUND_Y - p.height;
                p.vy = 0;
                p.onGround = true;
            }

            if (isLeft) {
                if (p.x < 20) p.x = 20;
                if (p.x + p.width > WIDTH / 2 - NET_WIDTH / 2 - 10) {
                    p.x = WIDTH / 2 - NET_WIDTH / 2 - 10 - p.width;
                }
            } else {
                if (p.x < WIDTH / 2 + NET_WIDTH / 2 + 10) {
                    p.x = WIDTH / 2 + NET_WIDTH / 2 + 10;
                }
                if (p.x + p.width > WIDTH - 20) {
                    p.x = WIDTH - 20 - p.width;
                }
            }

            if (hit && ballInPlay) {
                attemptHit(p, isLeft);
            }
        }

        private void attemptHit(Player p, boolean isLeft) {
            double pxCenter = p.x + p.width / 2.0;
            double pyCenter = p.y + p.height / 3.0;

            double dx = ball.x - pxCenter;
            double dy = ball.y - pyCenter;
            double dist = Math.sqrt(dx * dx + dy * dy);

            double maxReach = 60;

            if (dist < maxReach) {
                double nx = dx / (dist + 0.0001);
                double ny = dy / (dist + 0.0001);

                double power = 14 + random.nextDouble() * 4;

                ball.vx = nx * power;
                ball.vy = ny * power;

                if (isLeft && ball.vx < 0) ball.vx = -ball.vx;
                if (!isLeft && ball.vx > 0) ball.vx = -ball.vx;

                ball.vy -= 4;

                clampBallSpeed();

                addCameraShake(0.25, 4);
            }
        }

        private void updateBall(double dt) {
            if (!ballInPlay) {
                if (leftServing) {
                    ball.x = leftPlayer.x + leftPlayer.width + BALL_RADIUS + 5;
                    ball.y = leftPlayer.y - 20;
                } else {
                    ball.x = rightPlayer.x - BALL_RADIUS - 5;
                    ball.y = rightPlayer.y - 20;
                }
                return;
            }

            ball.vy += GRAVITY;
            ball.vx *= AIR_DAMPING;
            ball.vy *= AIR_DAMPING;

            ball.x += ball.vx;
            ball.y += ball.vy;

            clampBallSpeed();

            if (ball.y + BALL_RADIUS >= GROUND_Y) {
                ball.y = GROUND_Y - BALL_RADIUS;
                ball.vy = -ball.vy * BOUNCE_DAMPING;
                ball.vx *= 0.95;

                if (Math.abs(ball.vy) < 1.2) {
                    ball.vy = 0;
                }

                addCameraShake(0.2, 3);

                handleBallGrounded();
            }

            if (ball.y - BALL_RADIUS < 0) {
                ball.y = BALL_RADIUS;
                ball.vy = -ball.vy * BOUNCE_DAMPING;
            }

            if (ball.x - BALL_RADIUS < 0) {
                ball.x = BALL_RADIUS;
                ball.vx = -ball.vx * BOUNCE_DAMPING;
            }

            if (ball.x + BALL_RADIUS > WIDTH) {
                ball.x = WIDTH - BALL_RADIUS;
                ball.vx = -ball.vx * BOUNCE_DAMPING;
            }

            handleNetCollision();
            handlePlayerCollision(leftPlayer);
            handlePlayerCollision(rightPlayer);

            updateTrail();
        }

        private void clampBallSpeed() {
            double speed = Math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy);
            if (speed > MAX_BALL_SPEED) {
                double scale = MAX_BALL_SPEED / (speed + 0.0001);
                ball.vx *= scale;
                ball.vy *= scale;
            }
        }

        private void handleBallGrounded() {
            if (!ballInPlay) return;

            boolean leftSide = ball.x < WIDTH / 2.0;
            if (leftSide) {
                rightScore++;
                leftServing = true;
            } else {
                leftScore++;
                leftServing = false;
            }

            ballInPlay = false;
            serveTimer = 0;

            addCameraShake(0.4, 6);
        }

        private void handleNetCollision() {
            double netX = WIDTH / 2.0 - NET_WIDTH / 2.0;
            double netY = GROUND_Y - NET_HEIGHT;

            Rectangle netRect = new Rectangle(
                    (int) netX,
                    (int) netY,
                    NET_WIDTH,
                    NET_HEIGHT
            );

            Ellipse2D.Double ballShape = new Ellipse2D.Double(
                    ball.x - BALL_RADIUS,
                    ball.y - BALL_RADIUS,
                    BALL_RADIUS * 2,
                    BALL_RADIUS * 2
            );

            if (ballShape.intersects(netRect)) {
                if (ball.x < WIDTH / 2.0) {
                    ball.x = netX - BALL_RADIUS;
                    ball.vx = -Math.abs(ball.vx) * BOUNCE_DAMPING;
                } else {
                    ball.x = netX + NET_WIDTH + BALL_RADIUS;
                    ball.vx = Math.abs(ball.vx) * BOUNCE_DAMPING;
                }

                if (ball.y > netY) {
                    ball.y = netY + NET_HEIGHT + BALL_RADIUS;
                    ball.vy = Math.abs(ball.vy) * BOUNCE_DAMPING;
                } else {
                    ball.y = netY - BALL_RADIUS;
                    ball.vy = -Math.abs(ball.vy) * BOUNCE_DAMPING;
                }

                addCameraShake(0.25, 4);
            }
        }

        private void handlePlayerCollision(Player p) {
            double pxCenter = p.x + p.width / 2.0;
            double pyCenter = p.y + p.height / 2.0;

            double dx = ball.x - pxCenter;
            double dy = ball.y - pyCenter;
            double dist = Math.sqrt(dx * dx + dy * dy);

            double minDist = BALL_RADIUS + Math.min(p.width, p.height) * 0.35;

            if (dist < minDist) {
                double nx = dx / (dist + 0.0001);
                double ny = dy / (dist + 0.0001);

                double overlap = minDist - dist;
                ball.x += nx * overlap;
                ball.y += ny * overlap;

                double relativeVx = ball.vx - p.vx;
                double relativeVy = ball.vy - p.vy;

                double dot = relativeVx * nx + relativeVy * ny;

                if (dot < 0) {
                    ball.vx -= 1.6 * dot * nx;
                    ball.vy -= 1.6 * dot * ny;
                }

                clampBallSpeed();
                addCameraShake(0.18, 3);
            }
        }

        private void updateTrail() {
            trailX[trailIndex] = ball.x;
            trailY[trailIndex] = ball.y;
            trailIndex++;
            if (trailIndex >= TRAIL_SIZE) {
                trailIndex = 0;
                trailFilled = true;
            }
        }

        private void updateAI(double dt) {
            if (!rightAIEnabled) return;

            aiReactionTimer += dt;
            if (aiReactionTimer < aiDecisionInterval) return;
            aiReactionTimer = 0;

            rightMoveLeft = false;
            rightMoveRight = false;
            rightJump = false;
            rightHit = false;

            double targetX = WIDTH * 0.75;

            if (ballInPlay) {
                if (ball.x > WIDTH / 2.0) {
                    targetX = ball.x + (ball.vx * 0.6);
                } else {
                    targetX = WIDTH * 0.75;
                }
            }

            if (targetX < rightPlayer.x + rightPlayer.width / 2.0 - 5) {
                rightMoveLeft = true;
            } else if (targetX > rightPlayer.x + rightPlayer.width / 2.0 + 5) {
                rightMoveRight = true;
            }

            if (ballInPlay && ball.y < rightPlayer.y && Math.abs(ball.x - rightPlayer.x) < 80) {
                if (rightPlayer.onGround && random.nextDouble() < 0.6) {
                    rightJump = true;
                }
            }

            if (ballInPlay && Math.abs(ball.x - (rightPlayer.x + rightPlayer.width / 2.0)) < 70 &&
                    ball.y < rightPlayer.y + 20) {
                if (random.nextDouble() < 0.7) {
                    rightHit = true;
                }
            }
        }

        private void addCameraShake(double duration, double magnitude) {
            shakeTime = Math.max(shakeTime, duration);
            shakeMagnitude = Math.max(shakeMagnitude, magnitude);
        }

        private void updateCameraShake(double dt) {
            if (shakeTime > 0) {
                shakeTime -= dt;
                if (shakeTime <= 0) {
                    shakeTime = 0;
                    shakeMagnitude = 0;
                }
            }
        }

        // -----------------------------------------------------------------
        // Rendering
        // -----------------------------------------------------------------
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double offsetX = 0;
            double offsetY = 0;
            if (shakeTime > 0 && shakeMagnitude > 0) {
                offsetX = (random.nextDouble() - 0.5) * shakeMagnitude;
                offsetY = (random.nextDouble() - 0.5) * shakeMagnitude;
            }

            g2.translate(offsetX, offsetY);

            drawBackground(g2);
            drawCourt(g2);
            drawNet(g2);
            drawPlayers(g2);
            drawBall(g2);
            drawScore(g2);
            drawInfo(g2);

            if (showDebug) {
                drawDebug(g2);
            }

            g2.dispose();
        }

        private void drawBackground(Graphics2D g2) {
            GradientPaint sky = new GradientPaint(
                    0, 0, new Color(120, 180, 255),
                    0, HEIGHT, new Color(180, 220, 255)
            );
            g2.setPaint(sky);
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            g2.setColor(new Color(255, 255, 255, 180));
            for (int i = 0; i < 5; i++) {
                int cx = 100 + i * 180;
                int cy = 80 + (int) (Math.sin(timeSeconds * 0.3 + i) * 10);
                g2.fillOval(cx, cy, 80, 40);
                g2.fillOval(cx + 30, cy - 10, 90, 50);
                g2.fillOval(cx - 40, cy - 5, 70, 35);
            }

            g2.setColor(new Color(255, 255, 0, 220));
            int sunX = WIDTH - 120;
            int sunY = 80;
            g2.fillOval(sunX - 40, sunY - 40, 80, 80);
        }

        private void drawCourt(Graphics2D g2) {
            g2.setColor(new Color(230, 210, 160));
            g2.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

            g2.setColor(new Color(210, 190, 140));
            for (int i = 0; i < 20; i++) {
                int y = GROUND_Y + i * 4;
                g2.drawLine(0, y, WIDTH, y);
            }

            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(40, GROUND_Y, WIDTH - 40, GROUND_Y);
            g2.drawLine(40, GROUND_Y, 40, GROUND_Y - 120);
            g2.drawLine(WIDTH - 40, GROUND_Y, WIDTH - 40, GROUND_Y - 120);
            g2.drawLine(WIDTH / 2, GROUND_Y, WIDTH / 2, GROUND_Y - 120);
        }

        private void drawNet(Graphics2D g2) {
            double netX = WIDTH / 2.0 - NET_WIDTH / 2.0;
            double netY = GROUND_Y - NET_HEIGHT;

            if (showNetShadow) {
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillRect((int) netX + 6, (int) netY + 6, NET_WIDTH, NET_HEIGHT);
            }

            g2.setColor(new Color(230, 230, 230));
            g2.fillRect((int) netX, (int) netY, NET_WIDTH, NET_HEIGHT);

            g2.setColor(new Color(200, 200, 200));
            for (int y = 0; y < NET_HEIGHT; y += 12) {
                g2.drawLine((int) netX, (int) netY + y, (int) netX + NET_WIDTH, (int) netY + y);
            }

            g2.setColor(new Color(200, 200, 200));
            g2.fillRect((int) netX - 3, (int) netY, 3, NET_HEIGHT + 10);
            g2.fillRect((int) netX + NET_WIDTH, (int) netY, 3, NET_HEIGHT + 10);

            g2.setColor(new Color(255, 255, 255));
            g2.fillRect((int) netX - 5, (int) netY - 8, NET_WIDTH + 10, 6);
        }

        private void drawPlayers(Graphics2D g2) {
            drawPlayer(g2, leftPlayer, true);
            drawPlayer(g2, rightPlayer, false);
        }

        private void drawPlayer(Graphics2D g2, Player p, boolean isLeft) {
            if (showShadows) {
                g2.setColor(new Color(0, 0, 0, 60));
                int shadowWidth = p.width + 10;
                int shadowHeight = 12;
                int sx = (int) (p.x + p.width / 2.0 - shadowWidth / 2.0);
                int sy = GROUND_Y - shadowHeight / 2;
                g2.fillOval(sx, sy, shadowWidth, shadowHeight);
            }

            g2.setColor(p.color);
            g2.fillRoundRect((int) p.x, (int) p.y, p.width, p.height, 10, 10);

            g2.setColor(new Color(80, 80, 80));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect((int) p.x, (int) p.y, p.width, p.height, 10, 10);

            int headRadius = 18;
            int headX = (int) (p.x + p.width / 2.0 - headRadius / 2.0);
            int headY = (int) (p.y - headRadius + 6);
            g2.setColor(new Color(255, 230, 200));
            g2.fillOval(headX, headY, headRadius, headRadius);

            g2.setColor(new Color(60, 60, 60));
            g2.drawOval(headX, headY, headRadius, headRadius);

            int eyeY = headY + 6;
            if (isLeft) {
                g2.fillOval(headX + 4, eyeY, 4, 4);
                g2.fillOval(headX + 10, eyeY, 4, 4);
            } else {
                g2.fillOval(headX + 4, eyeY, 4, 4);
                g2.fillOval(headX + 10, eyeY, 4, 4);
            }

            int mouthY = headY + 12;
            g2.drawArc(headX + 4, mouthY, 10, 6, 0, -180);

            g2.setColor(new Color(255, 255, 255, 80));
            int jerseyY = (int) p.y + 10;
            g2.fillRect((int) p.x + 4, jerseyY, p.width - 8, 12);

            g2.setFont(smallFont);
            g2.setColor(new Color(40, 40, 40, 200));
            String label = isLeft ? "L" : (rightAIEnabled ? "AI" : "R");
            g2.drawString(label, (int) (p.x + p.width / 2.0 - 4), jerseyY + 10);
        }

        private void drawBall(Graphics2D g2) {
            if (showShadows) {
                g2.setColor(new Color(0, 0, 0, 60));
                int shadowWidth = (int) (BALL_RADIUS * 2.2);
                int shadowHeight = 12;
                int sx = (int) (ball.x - shadowWidth / 2.0);
                int sy = GROUND_Y - shadowHeight / 2;
                g2.fillOval(sx, sy, shadowWidth, shadowHeight);
            }

            if (showTrails && (trailFilled || trailIndex > 2)) {
                for (int i = 0; i < TRAIL_SIZE; i++) {
                    int idx = (trailIndex - 1 - i + TRAIL_SIZE) % TRAIL_SIZE;
                    if (!trailFilled && idx < 0) break;

                    double tx = trailX[idx];
                    double ty = trailY[idx];

                    float alpha = (float) (80 * (1.0 - i / (double) TRAIL_SIZE));
                    if (alpha < 0) alpha = 0;
                    g2.setColor(new Color(255, 255, 255, (int) alpha));
                    int r = (int) BALL_RADIUS;
                    g2.fillOval((int) (tx - r), (int) (ty - r), r * 2, r * 2);
                }
            }

            int r = (int) BALL_RADIUS;
            int x = (int) (ball.x - r);
            int y = (int) (ball.y - r);

            GradientPaint gp = new GradientPaint(
                    x, y, new Color(255, 255, 255),
                    x + r * 2, y + r * 2, new Color(230, 230, 230)
            );
            g2.setPaint(gp);
            g2.fillOval(x, y, r * 2, r * 2);

            g2.setColor(new Color(200, 200, 200));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x, y, r * 2, r * 2);

            g2.setColor(new Color(255, 220, 120));
            g2.setStroke(new BasicStroke(2f));
            g2.drawArc(x + 2, y + 2, r * 2 - 4, r * 2 - 4, 30, 120);
            g2.drawArc(x + 2, y + 2, r * 2 - 4, r * 2 - 4, 210, 120);

            g2.setColor(new Color(120, 160, 255));
            g2.drawArc(x + 4, y + 4, r * 2 - 8, r * 2 - 8, 330, 120);
            g2.drawArc(x + 4, y + 4, r * 2 - 8, r * 2 - 8, 150, 120);
        }

        private void drawScore(Graphics2D g2) {
            g2.setFont(scoreFont);
            g2.setColor(new Color(255, 255, 255, 230));
            String leftText = String.valueOf(leftScore);
            String rightText = String.valueOf(rightScore);

            int centerX = WIDTH / 2;
            int y = 60;

            FontMetrics fm = g2.getFontMetrics();
            int leftWidth = fm.stringWidth(leftText);
            int rightWidth = fm.stringWidth(rightText);

            g2.drawString(leftText, centerX - 40 - leftWidth, y);
            g2.drawString(rightText, centerX + 40, y);

            g2.setFont(infoFont);
            g2.setColor(new Color(255, 255, 255, 200));
            String serveText = leftServing ? "Serve: LEFT" : "Serve: RIGHT";
            int serveWidth = g2.getFontMetrics().stringWidth(serveText);
            g2.drawString(serveText, centerX - serveWidth / 2, y + 30);
        }

        private void drawInfo(Graphics2D g2) {
            g2.setFont(infoFont);
            g2.setColor(new Color(255, 255, 255, 220));

            String status = paused ? "PAUSED" : (ballInPlay ? "RALLY" : "SERVE");
            g2.drawString("Status: " + status, 20, 30);

            g2.setFont(smallFont);
            g2.setColor(new Color(255, 255, 255, 210));

            int y = HEIGHT - 80;
            g2.drawString("Controls:", 20, y);
            y += 16;
            g2.drawString("Left: A/D move, W jump, SPACE hit", 20, y);
            y += 16;
            g2.drawString("Right: Arrow keys move/jump, ENTER hit (AI can be toggled)", 20, y);
            y += 16;
            g2.drawString("P: pause, R: reset scores, T: toggle trails, N: toggle net shadow, F1: debug, F2: toggle AI", 20, y);
        }

        private void drawDebug(Graphics2D g2) {
            g2.setFont(smallFont);
            g2.setColor(new Color(0, 0, 0, 180));

            int y = 40;
            int x = WIDTH - 260;

            g2.fillRoundRect(x - 10, y - 20, 250, 160, 10, 10);
            g2.setColor(new Color(255, 255, 255, 230));

            g2.drawString("DEBUG", x, y);
            y += 16;
            g2.drawString(String.format("Ball: (%.1f, %.1f)", ball.x, ball.y), x, y);
            y += 16;
            g2.drawString(String.format("Ball v: (%.2f, %.2f)", ball.vx, ball.vy), x, y);
            y += 16;
            g2.drawString(String.format("Left: (%.1f, %.1f) v(%.2f, %.2f)", leftPlayer.x, leftPlayer.y, leftPlayer.vx, leftPlayer.vy), x, y);
            y += 16;
            g2.drawString(String.format("Right: (%.1f, %.1f) v(%.2f, %.2f)", rightPlayer.x, rightPlayer.y, rightPlayer.vx, rightPlayer.vy), x, y);
            y += 16;
            g2.drawString("BallInPlay: " + ballInPlay + "  ServeTimer: " + String.format("%.2f", serveTimer), x, y);
            y += 16;
            g2.drawString("AI: " + (rightAIEnabled ? "ON" : "OFF") + "  dt: " + (tickDelay / 1000.0), x, y);
            y += 16;
            g2.drawString("Shake: t=" + String.format("%.2f", shakeTime) + " mag=" + String.format("%.2f", shakeMagnitude), x, y);
        }

        // -----------------------------------------------------------------
        // Input handling
        // -----------------------------------------------------------------
        @Override
        public void keyTyped(KeyEvent e) {
            // not used
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();

            if (code == KeyEvent.VK_A) leftMoveLeft = true;
            if (code == KeyEvent.VK_D) leftMoveRight = true;
            if (code == KeyEvent.VK_W) leftJump = true;
            if (code == KeyEvent.VK_SPACE) leftHit = true;

            if (!rightAIEnabled) {
                if (code == KeyEvent.VK_LEFT) rightMoveLeft = true;
                if (code == KeyEvent.VK_RIGHT) rightMoveRight = true;
                if (code == KeyEvent.VK_UP) rightJump = true;
                if (code == KeyEvent.VK_ENTER) rightHit = true;
            }

            if (code == KeyEvent.VK_P) {
                paused = !paused;
            }

            if (code == KeyEvent.VK_R) {
                resetScoresAndPositions();
            }

            if (code == KeyEvent.VK_T) {
                showTrails = !showTrails;
            }

            if (code == KeyEvent.VK_N) {
                showNetShadow = !showNetShadow;
            }

            if (code == KeyEvent.VK_F1) {
                showDebug = !showDebug;
            }

            if (code == KeyEvent.VK_F2) {
                rightAIEnabled = !rightAIEnabled;
                rightMoveLeft = rightMoveRight = rightJump = rightHit = false;
            }

            if (code == KeyEvent.VK_MINUS || code == KeyEvent.VK_SUBTRACT) {
                tickDelay = Math.min(40, tickDelay + 2);
                timer.setDelay(tickDelay);
            }

            if (code == KeyEvent.VK_EQUALS || code == KeyEvent.VK_ADD) {
                tickDelay = Math.max(8, tickDelay - 2);
                timer.setDelay(tickDelay);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();

            if (code == KeyEvent.VK_A) leftMoveLeft = false;
            if (code == KeyEvent.VK_D) leftMoveRight = false;
            if (code == KeyEvent.VK_W) leftJump = false;
            if (code == KeyEvent.VK_SPACE) leftHit = false;

            if (!rightAIEnabled) {
                if (code == KeyEvent.VK_LEFT) rightMoveLeft = false;
                if (code == KeyEvent.VK_RIGHT) rightMoveRight = false;
                if (code == KeyEvent.VK_UP) rightJump = false;
                if (code == KeyEvent.VK_ENTER) rightHit = false;
            }
        }

        private void resetScoresAndPositions() {
            leftScore = 0;
            rightScore = 0;
            leftServing = true;
            ballInPlay = false;
            serveTimer = 0;

            leftPlayer.x = WIDTH * 0.20;
            leftPlayer.y = GROUND_Y - PLAYER_HEIGHT;
            leftPlayer.vx = leftPlayer.vy = 0;
            leftPlayer.onGround = true;

            rightPlayer.x = WIDTH * 0.80;
            rightPlayer.y = GROUND_Y - PLAYER_HEIGHT;
            rightPlayer.vx = rightPlayer.vy = 0;
            rightPlayer.onGround = true;

            ball.x = WIDTH * 0.25;
            ball.y = 200;
            ball.vx = ball.vy = 0;

            resetTrail();
        }
    }

    // ---------------------------------------------------------------------
    // Ball class
    // ---------------------------------------------------------------------
    static class Ball {
        double x, y;
        double vx, vy;

        Ball(double x, double y, double vx, double vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }

    // ---------------------------------------------------------------------
    // Player class
    // ---------------------------------------------------------------------
    static class Player {
        double x, y;
        double vx, vy;
        int width, height;
        boolean onGround;
        Color color;
        String name;

        Player(double x, double y, int width, int height, Color color, String name) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
            this.name = name;
            this.onGround = true;
        }
    }
}
