import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;

public class VolleyballGameSimulation extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VolleyballGameSimulation app = new VolleyballGameSimulation();
            app.setVisible(true);
        });
    }

    public VolleyballGameSimulation() {
        super("Volleyball Game Simulation - Single File");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GamePanel panel = new GamePanel();
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
    }

    // GamePanel does drawing, input, and game loop
    static class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
        // Dimensions
        final int WIDTH = 1200;
        final int HEIGHT = 650;
        final Rectangle2D.Double COURT = new Rectangle2D.Double(60, 400, WIDTH - 120, 160);
        final double G = 0.45;
        final double AIR_DRAG = 0.998;
        final double GROUND_DRAG = 0.85;
        final double BOUNCE_DAMP = 0.68;
        final double PLAYER_MOVE_ACC = 0.6;
        final double PLAYER_MAX_VX = 8.0;
        final double PLAYER_JUMP = 11.5;
        final double BALL_MAX_SPEED = 20.0;
        final double BALL_MIN_HIT_SPEED = 5.0;
        final double NET_X = WIDTH / 2.0;
        final double NET_WIDTH = 12.0;
        final double NET_HEIGHT = 200.0;
        final double NET_TOP_Y = COURT.getY() - NET_HEIGHT;
        final Rectangle2D.Double NET = new Rectangle2D.Double(NET_X - NET_WIDTH / 2.0, NET_TOP_Y, NET_WIDTH, NET_HEIGHT + COURT.getHeight());
        final Stroke NET_STROKE = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[]{6f, 6f}, 0);
        final int TICK_MS = 16;
        final int TRAIL_LENGTH = 20;

        // Game state
        javax.swing.Timer timer;
        boolean paused = false;
        boolean fastForward = false;
        boolean stepOnce = false;
        boolean showDebug = false;
        boolean showPrediction = true;
        boolean showTrails = true;
        boolean showParticles = true;

        // Input
        boolean leftA, leftD, leftW, leftS;
        boolean rightLeft, rightRight, rightUp, rightDown;
        boolean mousePressed = false;
        Point mousePoint = new Point(0, 0);

        // Entities
        Player leftPlayer;
        Player rightPlayer;
        Ball ball;
        ArrayList<Particle> particles = new ArrayList<>();
        Deque<Point2D.Double> ballTrail = new ArrayDeque<>();

        // Scoring
        int scoreLeft = 0;
        int scoreRight = 0;
        int setsLeft = 0;
        int setsRight = 0;
        int pointsToWin = 15;
        int touchCountLeft = 0;
        int touchCountRight = 0;
        Side lastTouch = Side.NONE;

        // Serving
        Side serveSide = Side.LEFT;
        boolean waitingForServe = true;
        int rallyCount = 0;

        // AI
        boolean aiLeft = true;
        boolean aiRight = true;

        // Misc
        Random rng = new Random(4);
        long frameIndex = 0;
        long lastTime = System.nanoTime();

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            setBackground(new Color(165, 215, 255));
            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);
            initGame();
            timer = new javax.swing.Timer(TICK_MS, this);
            timer.start();
        }

        // Initialize game objects
        private void initGame() {
            leftPlayer = new Player("L", Side.LEFT, COURT.getX() + COURT.getWidth() * 0.25, COURT.getY() - 40, new Color(32, 120, 240));
            rightPlayer = new Player("R", Side.RIGHT, COURT.getX() + COURT.getWidth() * 0.75, COURT.getY() - 40, new Color(252, 96, 64));
            leftPlayer.aiEnabled = aiLeft;
            rightPlayer.aiEnabled = aiRight;
            ball = new Ball(COURT.getCenterX(), COURT.getY() - 120);
            ballTrail.clear();
            for (int i = 0; i < TRAIL_LENGTH; i++) {
                ballTrail.add(new Point2D.Double(ball.x, ball.y));
            }
            particles.clear();
            resetForServe(serveSide);
        }

        // Reset to serve side
        private void resetForServe(Side s) {
            waitingForServe = true;
            lastTouch = Side.NONE;
            touchCountLeft = 0;
            touchCountRight = 0;
            leftPlayer.reset(COURT.getX() + COURT.getWidth() * 0.25, COURT.getY() - 40);
            rightPlayer.reset(COURT.getX() + COURT.getWidth() * 0.75, COURT.getY() - 40);
            ball.vx = 0;
            ball.vy = 0;
            if (s == Side.LEFT) {
                ball.x = leftPlayer.x + 10;
                ball.y = leftPlayer.y - leftPlayer.radius - ball.radius - 10;
            } else {
                ball.x = rightPlayer.x - 10;
                ball.y = rightPlayer.y - rightPlayer.radius - ball.radius - 10;
            }
        }

        // Game loop tick
        @Override
        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            long dtNanos = now - lastTime;
            lastTime = now;
            int steps = 1;
            if (fastForward) steps = 2;
            if (paused && !stepOnce) {
                repaint();
                return;
            }
            if (paused && stepOnce) {
                steps = 1;
                stepOnce = false;
            }
            for (int i = 0; i < steps; i++) {
                update();
            }
            repaint();
        }

        // Update simulation step
        private void update() {
            frameIndex++;
            if (waitingForServe) {
                if (!leftPlayer.aiEnabled || !rightPlayer.aiEnabled) {
                    // manual serve triggers
                } else {
                    // auto serve after short wait
                    if (frameIndex % 60 == 0) {
                        doServe(serveSide);
                    }
                }
            }
            handleInput();
            if (!waitingForServe) {
                leftPlayer.update();
                rightPlayer.update();
                ball.update();
                handleCollisions();
                enforceCourtBounds();
                updateTrail();
                spawnAutoParticles();
                updateParticles();
                checkFaults();
                checkPoint();
            } else {
                // serve preview wobble
                ball.vx *= 0.95;
                ball.vy *= 0.95;
                ball.y += Math.sin(frameIndex * 0.08) * 0.1;
                updateTrail();
                updateParticles();
            }
        }

        // Input handling to set player intent
        private void handleInput() {
            // left player
            if (!leftPlayer.aiEnabled) {
                leftPlayer.intentLeft = leftA;
                leftPlayer.intentRight = leftD;
                leftPlayer.intentJump = leftW;
            } else {
                applyAI(leftPlayer);
            }
            // right player
            if (!rightPlayer.aiEnabled) {
                rightPlayer.intentLeft = rightLeft;
                rightPlayer.intentRight = rightRight;
                rightPlayer.intentJump = rightUp;
            } else {
                applyAI(rightPlayer);
            }
            // left/right boundaries for intents
            clampIntentByCourt(leftPlayer);
            clampIntentByCourt(rightPlayer);
        }

        // AI logic per player
        private void applyAI(Player p) {
            Prediction pred = predictBallLanding(220);
            boolean onMySide = (p.side == Side.LEFT) ? (ball.x < NET.getCenterX()) : (ball.x > NET.getCenterX());
            double halfLeft = COURT.getX();
            double halfRight = COURT.getX() + COURT.getWidth();
            if (p.side == Side.LEFT) halfRight = NET.getCenterX() - p.radius - 8;
            if (p.side == Side.RIGHT) halfLeft = NET.getCenterX() + p.radius + 8;
            double targetX = clamp(pred.landingX, halfLeft + 20, halfRight - 20);
            if (!onMySide) {
                // reposition center on own side
                targetX = clamp((halfLeft + halfRight) * 0.5, halfLeft + 30, halfRight - 30);
            } else {
                if (ball.y < p.y - 60 && Math.abs(ball.x - p.x) < 40) {
                    // jump when ball is above
                    p.intentJump = true;
                } else {
                    p.intentJump = false;
                }
                if (ball.y < NET_TOP_Y + 40 && Math.abs(ball.x - NET.getCenterX()) < 120) {
                    // spike move near net
                    targetX = clamp(NET.getCenterX() + (p.side == Side.LEFT ? -40 : 40), halfLeft + 30, halfRight - 30);
                    if (Math.abs(ball.x - p.x) < 30) p.intentJump = true;
                }
            }
            // movement intents
            double dx = targetX - p.x;
            p.intentLeft = dx < -6;
            p.intentRight = dx > 6;
            // occasionally jump for digs
            if (onMySide && ball.vy > 2 && ball.y > p.y - 10 && Math.abs(ball.x - p.x) < 30 && rng.nextDouble() < 0.05) {
                p.intentJump = true;
            }
            // if ball is near and below, small hop
            if (onMySide && ball.y > p.y - 20 && Math.abs(ball.x - p.x) < 20) {
                p.intentJump |= rng.nextDouble() < 0.02;
            }
        }

        // Predict landing point of ball ignoring player collisions
        private Prediction predictBallLanding(int steps) {
            double px = ball.x;
            double py = ball.y;
            double vx = ball.vx;
            double vy = ball.vy;
            double landingX = px;
            boolean willCross = false;
            for (int i = 0; i < steps; i++) {
                vy += G;
                px += vx;
                py += vy;
                vx *= AIR_DRAG;
                // net simple collision
                if (py < NET.getMaxY() && py > NET.getY()) {
                    if (px > NET.getMinX() - ball.radius && px < NET.getMaxX() + ball.radius) {
                        if ((vx > 0 && px < NET.getCenterX()) || (vx < 0 && px > NET.getCenterX())) {
                            vx = -vx * 0.75;
                            px = (px < NET.getCenterX()) ? (NET.getMinX() - ball.radius) : (NET.getMaxX() + ball.radius);
                        }
                    }
                }
                if (py + ball.radius >= COURT.getY()) {
                    landingX = px;
                    willCross = px > NET.getCenterX();
                    break;
                }
            }
            return new Prediction(landingX, willCross);
        }

        // Clamp movement intent if near boundaries
        private void clampIntentByCourt(Player p) {
            double halfLeft = COURT.getX();
            double halfRight = COURT.getX() + COURT.getWidth();
            if (p.side == Side.LEFT) halfRight = NET.getCenterX() - p.radius - 8;
            else halfLeft = NET.getCenterX() + p.radius + 8;
            if (p.x < halfLeft + 8) {
                p.intentLeft = false;
                p.intentRight = true;
            }
            if (p.x > halfRight - 8) {
                p.intentRight = false;
                p.intentLeft = true;
            }
        }

        // Trail update for ball
        private void updateTrail() {
            if (!showTrails) {
                ballTrail.clear();
                return;
            }
            ballTrail.addLast(new Point2D.Double(ball.x, ball.y));
            while (ballTrail.size() > TRAIL_LENGTH) ballTrail.removeFirst();
        }

        // Particles auto spawn
        private void spawnAutoParticles() {
            if (!showParticles) return;
            // dust on ground contact
            if (ball.lastGroundHitFrame >= 0 && frameIndex - ball.lastGroundHitFrame < 2) {
                for (int i = 0; i < 6; i++) {
                    double ang = rng.nextDouble() * Math.PI;
                    double sp = 1 + rng.nextDouble() * 2;
                    double vx = Math.cos(ang) * sp;
                    double vy = -Math.abs(Math.sin(ang) * sp * 0.6);
                    particles.add(Particle.dust(ball.x, COURT.getY(), vx, vy));
                }
            }
            // small puffs from players landing
            if (leftPlayer.justLanded) {
                for (int i = 0; i < 4; i++) {
                    double vx = (rng.nextDouble() - 0.5) * 1.2;
                    double vy = -rng.nextDouble() * 1.2;
                    particles.add(Particle.dust(leftPlayer.x, COURT.getY(), vx, vy));
                }
                leftPlayer.justLanded = false;
            }
            if (rightPlayer.justLanded) {
                for (int i = 0; i < 4; i++) {
                    double vx = (rng.nextDouble() - 0.5) * 1.2;
                    double vy = -rng.nextDouble() * 1.2;
                    particles.add(Particle.dust(rightPlayer.x, COURT.getY(), vx, vy));
                }
                rightPlayer.justLanded = false;
            }
        }

        // Update and cull particles
        private void updateParticles() {
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.life -= 0.02;
                p.vy += G * 0.2;
                p.x += p.vx;
                p.y += p.vy;
                if (p.y > COURT.getY()) {
                    p.y = COURT.getY();
                    p.vy *= -0.3;
                    p.vx *= 0.6;
                }
                if (p.life <= 0) particles.remove(i);
            }
        }

        // Handle collisions between ball, players, court, and net
        private void handleCollisions() {
            // Ball with ground and walls
            if (ball.y + ball.radius >= COURT.getY()) {
                ball.y = COURT.getY() - ball.radius;
                ball.vy = -ball.vy * BOUNCE_DAMP;
                ball.vx *= GROUND_DRAG;
                ball.lastGroundHitFrame = frameIndex;
                // side touches reset on ground until serve
            }
            double leftWall = COURT.getX();
            double rightWall = COURT.getX() + COURT.getWidth();
            if (ball.x - ball.radius < leftWall) {
                ball.x = leftWall + ball.radius;
                ball.vx = -ball.vx * 0.9;
            }
            if (ball.x + ball.radius > rightWall) {
                ball.x = rightWall - ball.radius;
                ball.vx = -ball.vx * 0.9;
            }
            // Net rectangle collision
            if (ball.y + ball.radius > NET.getY() && ball.y - ball.radius < NET.getMaxY()) {
                if (ball.x + ball.radius > NET.getMinX() && ball.x - ball.radius < NET.getMaxX()) {
                    boolean fromLeft = ball.x < NET.getCenterX();
                    if ((fromLeft && ball.vx > 0) || (!fromLeft && ball.vx < 0)) {
                        ball.vx = -ball.vx * 0.6;
                        ball.x = fromLeft ? (NET.getMinX() - ball.radius) : (NET.getMaxX() + ball.radius);
                    }
                    // top cable
                    if (ball.y + ball.radius > NET.getY() && ball.y < NET.getY() + 4) {
                        ball.vy = -Math.abs(ball.vy) * 0.6;
                        ball.y = NET.getY() - ball.radius;
                    }
                }
            }
            // Ball with players
            collideBallWithPlayer(leftPlayer);
            collideBallWithPlayer(rightPlayer);
        }

        // Ball-player circular collision with response
        private void collideBallWithPlayer(Player p) {
            double dx = ball.x - p.x;
            double dy = ball.y - p.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            double minDist = ball.radius + p.radius;
            if (dist < minDist) {
                double nx = dx / (dist + 1e-6);
                double ny = dy / (dist + 1e-6);
                double overlap = minDist - dist;
                ball.x += nx * overlap;
                ball.y += ny * overlap;
                double rvx = ball.vx - p.vx;
                double rvy = ball.vy - p.vy;
                double vn = rvx * nx + rvy * ny;
                if (vn < 0) {
                    double cr = 0.9;
                    double j = -(1 + cr) * vn;
                    ball.vx += j * nx;
                    ball.vy += j * ny;
                    // add player's own velocity influence
                    ball.vx += p.vx * 0.4;
                    ball.vy += p.vy * 0.4;
                    // add directional aim if player is pressing keys
                    double aimX = (p.intentRight ? 1 : 0) - (p.intentLeft ? 1 : 0);
                    double aimY = p.intentJump ? -0.6 : -0.2;
                    ball.vx += aimX * 2.0;
                    ball.vy += aimY * 2.0;
                    // clamp speed
                    double sp = Math.hypot(ball.vx, ball.vy);
                    if (sp < BALL_MIN_HIT_SPEED) {
                        double s = BALL_MIN_HIT_SPEED / (sp + 1e-6);
                        ball.vx *= s;
                        ball.vy *= s;
                    } else if (sp > BALL_MAX_SPEED) {
                        double s = BALL_MAX_SPEED / sp;
                        ball.vx *= s;
                        ball.vy *= s;
                    }
                    // update last touch and touches count
                    if (p.side != lastTouch) {
                        if (p.side == Side.LEFT) {
                            touchCountLeft = 0;
                        } else {
                            touchCountRight = 0;
                        }
                    }
                    lastTouch = p.side;
                    if (p.side == Side.LEFT) {
                        touchCountLeft++;
                    } else {
                        touchCountRight++;
                    }
                    p.lastContactFrame = frameIndex;
                    spawnHitParticles(p, nx, ny);
                }
            }
        }

        // Spawn hit particles
        private void spawnHitParticles(Player p, double nx, double ny) {
            if (!showParticles) return;
            for (int i = 0; i < 8; i++) {
                double ang = Math.atan2(ny, nx) + (rng.nextDouble() - 0.5) * 0.6;
                double sp = 2 + rng.nextDouble() * 2.5;
                double vx = Math.cos(ang) * sp;
                double vy = Math.sin(ang) * sp;
                particles.add(Particle.spark(ball.x, ball.y, vx, vy, p.color));
            }
        }

        // Keep players within their half and handle ground
        private void enforceCourtBounds() {
            keepInHalf(leftPlayer);
            keepInHalf(rightPlayer);
            for (Player p : Arrays.asList(leftPlayer, rightPlayer)) {
                if (p.y + p.radius >= COURT.getY()) {
                    if (!p.onGround && p.vy > 4) p.justLanded = true;
                    p.y = COURT.getY() - p.radius;
                    p.vy = 0;
                    p.onGround = true;
                    p.jumpsLeft = 1;
                    p.vx *= GROUND_DRAG;
                } else {
                    p.onGround = false;
                }
            }
        }

        // Keep a player in their half and horizontal walls
        private void keepInHalf(Player p) {
            double left = COURT.getX() + p.radius;
            double right = COURT.getX() + COURT.getWidth() - p.radius;
            if (p.side == Side.LEFT) right = NET.getMinX() - p.radius - 6;
            else left = NET.getMaxX() + p.radius + 6;
            if (p.x < left) {
                p.x = left;
                p.vx = Math.max(0, p.vx);
            }
            if (p.x > right) {
                p.x = right;
                p.vx = Math.min(0, p.vx);
            }
        }

        // Check touches faults
        private void checkFaults() {
            if (touchCountLeft > 3) {
                awardPoint(Side.RIGHT, "Left team 4 hits");
            } else if (touchCountRight > 3) {
                awardPoint(Side.LEFT, "Right team 4 hits");
            }
            // Net touch by ball below top counts as let; handled by physics, not a point
        }

        // Check if ball grounded on one side to award point
        private void checkPoint() {
            if (ball.lastGroundHitFrame >= 0 && frameIndex - ball.lastGroundHitFrame == 1) {
                boolean onLeft = ball.x < NET.getCenterX();
                if (onLeft) {
                    awardPoint(Side.RIGHT, "Ball grounded on left");
                } else {
                    awardPoint(Side.LEFT, "Ball grounded on right");
                }
            }
        }

        // Award a point and handle serve rotation and set wins
        private void awardPoint(Side to, String reason) {
            if (to == Side.LEFT) scoreLeft++; else scoreRight++;
            rallyCount++;
            // reset touch counts
            touchCountLeft = 0;
            touchCountRight = 0;
            lastTouch = Side.NONE;
            // particles at center
            if (showParticles) {
                for (int i = 0; i < 20; i++) {
                    double ang = rng.nextDouble() * Math.PI * 2;
                    double sp = 1 + rng.nextDouble() * 3;
                    particles.add(Particle.spark(NET.getCenterX(), NET.getY(), Math.cos(ang) * sp, Math.sin(ang) * sp, to == Side.LEFT ? leftPlayer.color : rightPlayer.color));
                }
            }
            // serve to scoring team
            serveSide = to;
            // Check set victory
            if ((scoreLeft >= pointsToWin || scoreRight >= pointsToWin) && Math.abs(scoreLeft - scoreRight) >= 2) {
                if (scoreLeft > scoreRight) setsLeft++; else setsRight++;
                scoreLeft = 0;
                scoreRight = 0;
                // switch sides
                leftPlayer.side = (leftPlayer.side == Side.LEFT) ? Side.RIGHT : Side.LEFT;
                rightPlayer.side = (rightPlayer.side == Side.LEFT) ? Side.RIGHT : Side.LEFT;
                // also swap colors slightly to show side change
                Color lc = leftPlayer.color;
                leftPlayer.color = rightPlayer.color;
                rightPlayer.color = lc;
            }
            resetForServe(serveSide);
        }

        // Do serve action
        private void doServe(Side s) {
            waitingForServe = false;
            double base = s == Side.LEFT ? 7.5 : -7.5;
            double toss = -8 - rng.nextDouble() * 2;
            ball.vx = base + (rng.nextDouble() - 0.5) * 1.4;
            ball.vy = toss;
            ball.lastGroundHitFrame = -1;
        }

        // Paint callback
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawBackground(g2);
            drawCourt(g2);
            drawNet(g2);
            if (showPrediction && !waitingForServe) drawPrediction(g2);
            if (showTrails) drawTrail(g2);
            leftPlayer.draw(g2);
            rightPlayer.draw(g2);
            ball.draw(g2);
            drawParticles(g2);
            drawHUD(g2);
            g2.dispose();
        }

        // Draw background layers
        private void drawBackground(Graphics2D g2) {
            GradientPaint gp = new GradientPaint(0, 0, new Color(155, 205, 255), 0, HEIGHT, new Color(210, 235, 255));
            g2.setPaint(gp);
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            // sun
            g2.setColor(new Color(255, 245, 180, 180));
            g2.fillOval(80, 60, 90, 90);
            // distant crowd / hills
            g2.setColor(new Color(70, 120, 80, 80));
            for (int i = 0; i < WIDTH; i += 80) {
                int h = 40 + (int) (20 * Math.sin(i * 0.07));
                g2.fillRoundRect(i, (int) COURT.getY() + 100, 120, h, 50, 50);
            }
        }

        // Draw court
        private void drawCourt(Graphics2D g2) {
            // sand
            g2.setColor(new Color(238, 220, 160));
            g2.fillRect((int) COURT.getX(), (int) COURT.getY(), (int) COURT.getWidth(), (int) COURT.getHeight());
            // line
            g2.setColor(new Color(245, 240, 210));
            g2.setStroke(new BasicStroke(4f));
            g2.drawRect((int) COURT.getX(), (int) COURT.getY(), (int) COURT.getWidth(), (int) COURT.getHeight());
            // mid marker
            g2.setColor(new Color(255, 255, 255, 120));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine((int) NET.getCenterX(), (int) COURT.getY(), (int) NET.getCenterX(), (int) COURT.getMaxY());
            // shadows
            drawShadow(g2, leftPlayer.x, COURT.getY(), leftPlayer.radius);
            drawShadow(g2, rightPlayer.x, COURT.getY(), rightPlayer.radius);
            drawShadow(g2, ball.x, COURT.getY(), ball.radius * 0.66);
        }

        // Draw soft shadow on the court
        private void drawShadow(Graphics2D g2, double x, double y, double r) {
            RadialGradientPaint rg = new RadialGradientPaint(new Point2D.Double(x, y), (float) (r * 2.2f),
                    new float[]{0f, 1f}, new Color[]{new Color(0, 0, 0, 60), new Color(0, 0, 0, 0)});
            Paint old = g2.getPaint();
            g2.setPaint(rg);
            g2.fill(new Ellipse2D.Double(x - r * 2.2, y - r * 0.6, r * 4.4, r * 1.2));
            g2.setPaint(old);
        }

        // Draw net
        private void drawNet(Graphics2D g2) {
            // posts
            g2.setColor(new Color(90, 90, 110));
            g2.fillRect((int) (NET.getMinX() - 6), (int) (NET.getY() - 30), 4, (int) (NET.getHeight() + 50));
            g2.fillRect((int) (NET.getMaxX() + 2), (int) (NET.getY() - 30), 4, (int) (NET.getHeight() + 50));
            // strap top
            g2.setColor(new Color(230, 230, 240));
            g2.fillRect((int) NET.getMinX(), (int) NET.getY() - 2, (int) NET.getWidth(), 6);
            // mesh
            g2.setColor(new Color(30, 30, 40, 180));
            g2.setStroke(NET_STROKE);
            for (int y = (int) NET.getY(); y < NET.getMaxY(); y += 16) {
                g2.drawLine((int) NET.getMinX(), y, (int) NET.getMaxX(), y);
            }
        }

        // Draw predicted path
        private void drawPrediction(Graphics2D g2) {
            Prediction pred = predictBallLanding(260);
            double px = ball.x;
            double py = ball.y;
            double vx = ball.vx;
            double vy = ball.vy;
            g2.setColor(new Color(0, 0, 0, 80));
            for (int i = 0; i < 180; i++) {
                vy += G;
                px += vx;
                py += vy;
                vx *= AIR_DRAG;
                if (py + ball.radius >= COURT.getY()) {
                    break;
                }
                if (i % 6 == 0) {
                    g2.fill(new Ellipse2D.Double(px - 2, py - 2, 4, 4));
                }
                // bounce off net sides
                if (py < NET.getMaxY() && py > NET.getY()) {
                    if (px > NET.getMinX() - ball.radius && px < NET.getMaxX() + ball.radius) {
                        if ((vx > 0 && px < NET.getCenterX()) || (vx < 0 && px > NET.getCenterX())) {
                            vx = -vx * 0.75;
                            px = (px < NET.getCenterX()) ? (NET.getMinX() - ball.radius) : (NET.getMaxX() + ball.radius);
                        }
                    }
                }
            }
            // landing marker
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fill(new Ellipse2D.Double(pred.landingX - 6, COURT.getY() - 6, 12, 12));
        }

        // Draw trail behind ball
        private void drawTrail(Graphics2D g2) {
            if (ballTrail.isEmpty()) return;
            Point2D.Double prev = null;
            int i = 0;
            for (Point2D.Double p : ballTrail) {
                if (prev != null) {
                    float alpha = (float) i / (float) ballTrail.size();
                    g2.setColor(new Color(0, 0, 0, (int) (alpha * 40)));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine((int) prev.x, (int) prev.y, (int) p.x, (int) p.y);
                }
                prev = p;
                i++;
            }
        }

        // Draw particles
        private void drawParticles(Graphics2D g2) {
            for (Particle p : particles) p.draw(g2);
        }

        // Draw HUD and controls
        private void drawHUD(Graphics2D g2) {
            // scoreboard
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22f));
            String score = "Score  L " + scoreLeft + " : " + scoreRight + " R";
            String sets = "Sets  L " + setsLeft + " : " + setsRight + " R   (to " + pointsToWin + ")";
            g2.setColor(new Color(20, 40, 60));
            g2.drawString(score, 20, 36);
            g2.drawString(sets, 20, 64);
            // serve indicator
            String serve = "Serve: " + (serveSide == Side.LEFT ? "Left" : "Right") + (waitingForServe ? " [waiting]" : "");
            g2.drawString(serve, 20, 92);
            // player state
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
            g2.drawString("Left AI: " + (leftPlayer.aiEnabled ? "ON" : "OFF"), 20, 118);
            g2.drawString("Right AI: " + (rightPlayer.aiEnabled ? "ON" : "OFF"), 120, 118);
            // touches
            g2.drawString("Touches L: " + touchCountLeft + "  R: " + touchCountRight, 20, 142);
            // controls
            int y = HEIGHT - 70;
            g2.drawString("Controls:", 20, y);
            g2.drawString("[P] Pause/Resume  [N] Serve  [R] Reset  [F] Fast-Forward  [T] Trails  [X] Prediction  [C] Particles  [G] Debug", 100, y);
            g2.drawString("Left: A/D move, W jump   Right: Left/Right move, Up jump   [1]/[2] toggle AI sides", 100, y + 20);
            if (paused) {
                drawCenteredBanner(g2, "PAUSED");
            } else if (waitingForServe) {
                drawCenteredBanner(g2, "PRESS N OR SPACE TO SERVE");
            }
            if (showDebug) {
                g2.setColor(new Color(0, 0, 0, 100));
                g2.drawString("Ball v=(" + fmt(ball.vx) + "," + fmt(ball.vy) + ")  pos=(" + fmt(ball.x) + "," + fmt(ball.y) + ")", 20, HEIGHT - 100);
                g2.drawString("L pos=(" + fmt(leftPlayer.x) + "," + fmt(leftPlayer.y) + ") v=(" + fmt(leftPlayer.vx) + "," + fmt(leftPlayer.vy) + ")", 20, HEIGHT - 84);
                g2.drawString("R pos=(" + fmt(rightPlayer.x) + "," + fmt(rightPlayer.y) + ") v=(" + fmt(rightPlayer.vx) + "," + fmt(rightPlayer.vy) + ")", 20, HEIGHT - 68);
            }
        }

        // Draw centered banner text
        private void drawCenteredBanner(Graphics2D g2, String text) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 32f));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(text);
            int x = (int) (WIDTH / 2 - w / 2);
            int y = 120;
            g2.setColor(new Color(255, 255, 255, 240));
            g2.fillRoundRect(x - 16, y - 30, w + 32, 48, 12, 12);
            g2.setColor(new Color(30, 30, 30));
            g2.drawString(text, x, y);
        }

        // Helpers
        private String fmt(double v) {
            return String.format(Locale.US, "%.2f", v);
        }

        private double clamp(double v, double a, double b) {
            return Math.max(a, Math.min(b, v));
        }

        // Input events
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_A: leftA = true; break;
                case KeyEvent.VK_D: leftD = true; break;
                case KeyEvent.VK_W: leftW = true; break;
                case KeyEvent.VK_S: leftS = true; break;
                case KeyEvent.VK_LEFT: rightLeft = true; break;
                case KeyEvent.VK_RIGHT: rightRight = true; break;
                case KeyEvent.VK_UP: rightUp = true; break;
                case KeyEvent.VK_DOWN: rightDown = true; break;
                case KeyEvent.VK_SPACE:
                    if (waitingForServe) doServe(serveSide);
                    break;
                case KeyEvent.VK_P:
                    paused = !paused;
                    break;
                case KeyEvent.VK_F:
                    fastForward = !fastForward;
                    break;
                case KeyEvent.VK_N:
                    if (waitingForServe) doServe(serveSide);
                    break;
                case KeyEvent.VK_R:
                    scoreLeft = 0; scoreRight = 0; setsLeft = 0; setsRight = 0; serveSide = Side.LEFT; initGame();
                    break;
                case KeyEvent.VK_T:
                    showTrails = !showTrails;
                    break;
                case KeyEvent.VK_X:
                    showPrediction = !showPrediction;
                    break;
                case KeyEvent.VK_C:
                    showParticles = !showParticles;
                    break;
                case KeyEvent.VK_G:
                    showDebug = !showDebug;
                    break;
                case KeyEvent.VK_1:
                    aiLeft = !aiLeft; leftPlayer.aiEnabled = aiLeft;
                    break;
                case KeyEvent.VK_2:
                    aiRight = !aiRight; rightPlayer.aiEnabled = aiRight;
                    break;
                case KeyEvent.VK_PERIOD:
                    if (paused) stepOnce = true;
                    break;
                default:
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_A: leftA = false; break;
                case KeyEvent.VK_D: leftD = false; break;
                case KeyEvent.VK_W: leftW = false; break;
                case KeyEvent.VK_S: leftS = false; break;
                case KeyEvent.VK_LEFT: rightLeft = false; break;
                case KeyEvent.VK_RIGHT: rightRight = false; break;
                case KeyEvent.VK_UP: rightUp = false; break;
                case KeyEvent.VK_DOWN: rightDown = false; break;
                default: break;
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) { mousePressed = true; }

        @Override
        public void mouseReleased(MouseEvent e) { mousePressed = false; }

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}

        @Override
        public void mouseDragged(MouseEvent e) { mousePoint = e.getPoint(); }

        @Override
        public void mouseMoved(MouseEvent e) { mousePoint = e.getPoint(); }

        // Side enum
        enum Side { LEFT, RIGHT, NONE }

        // Prediction container
        static class Prediction {
            final double landingX;
            final boolean willCross;
            Prediction(double x, boolean c) { landingX = x; willCross = c; }
        }

        // Player class
        class Player {
            String name;
            Side side;
            double x, y;
            double vx, vy;
            double radius = 26;
            boolean onGround = false;
            boolean justLanded = false;
            boolean aiEnabled = true;
            boolean intentLeft = false;
            boolean intentRight = false;
            boolean intentJump = false;
            int jumpsLeft = 1;
            long lastContactFrame = -1;
            Color color;

            Player(String name, Side side, double x, double y, Color c) {
                this.name = name;
                this.side = side;
                this.x = x;
                this.y = y;
                this.color = c;
            }

            void reset(double x, double y) {
                this.x = x;
                this.y = y;
                this.vx = 0;
                this.vy = 0;
                this.onGround = false;
                this.justLanded = false;
                this.intentLeft = false;
                this.intentRight = false;
                this.intentJump = false;
                this.jumpsLeft = 1;
            }

            void update() {
                double ax = 0;
                if (intentLeft) ax -= PLAYER_MOVE_ACC;
                if (intentRight) ax += PLAYER_MOVE_ACC;
                vx += ax;
                vx *= 0.98;
                if (vx > PLAYER_MAX_VX) vx = PLAYER_MAX_VX;
                if (vx < -PLAYER_MAX_VX) vx = -PLAYER_MAX_VX;
                if (intentJump) {
                    if (onGround || jumpsLeft > 0) {
                        vy = -PLAYER_JUMP;
                        onGround = false;
                        jumpsLeft = 0;
                        intentJump = false;
                    }
                }
                vy += G;
                x += vx;
                y += vy;
            }

            void draw(Graphics2D g2) {
                // body
                g2.setColor(color);
                g2.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
                // outline
                g2.setColor(new Color(0, 0, 0, 120));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
                // head band
                g2.setColor(new Color(255, 255, 255, 180));
                g2.fillRoundRect((int) (x - radius * 0.6), (int) (y - radius * 0.9), (int) (radius * 1.2), 6, 4, 4);
                // jersey letter
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
                g2.setColor(Color.WHITE);
                String txt = name + (aiEnabled ? " AI" : "");
                drawCenteredString(g2, txt, (int) x, (int) (y + 5));
                // contact flash
                if (frameIndex - lastContactFrame < 6) {
                    float t = (6 - (frameIndex - lastContactFrame)) / 6f;
                    g2.setColor(new Color(255, 255, 255, (int) (t * 200)));
                    g2.setStroke(new BasicStroke(3f));
                    g2.draw(new Ellipse2D.Double(x - radius - 4, y - radius - 4, (radius + 4) * 2, (radius + 4) * 2));
                }
            }

            void drawCenteredString(Graphics2D g2, String s, int cx, int cy) {
                FontMetrics fm = g2.getFontMetrics();
                int w = fm.stringWidth(s);
                g2.drawString(s, cx - w / 2, cy);
            }
        }

        // Ball class
        class Ball {
            double x, y;
            double vx, vy;
            double radius = 12;
            long lastGroundHitFrame = -1;

            Ball(double x, double y) {
                this.x = x; this.y = y;
            }

            void update() {
                vy += G;
                x += vx;
                y += vy;
                vx *= AIR_DRAG;
            }

            void draw(Graphics2D g2) {
                // trail halo
                if (showTrails) {
                    for (int i = 0; i < 8; i++) {
                        float a = (8 - i) / 8f;
                        g2.setColor(new Color(50, 60, 100, (int) (a * 30)));
                        double rr = radius + i * 1.3;
                        g2.fill(new Ellipse2D.Double(x - rr, y - rr, rr * 2, rr * 2));
                    }
                }
                // ball panel
                g2.setColor(new Color(255, 250, 240));
                g2.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
                g2.setColor(new Color(220, 110, 40));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
                // seams
                g2.setColor(new Color(200, 80, 40));
                g2.drawArc((int) (x - radius), (int) (y - radius), (int) (radius * 2), (int) (radius * 2), 30, 120);
                g2.drawArc((int) (x - radius), (int) (y - radius), (int) (radius * 2), (int) (radius * 2), 210, 120);
            }
        }

        // Particle class
        static class Particle {
            double x, y, vx, vy;
            double size;
            double life;
            Color color;
            enum Type { DUST, SPARK }
            Type type;

            static Particle dust(double x, double y, double vx, double vy) {
                Particle p = new Particle();
                p.x = x; p.y = y; p.vx = vx; p.vy = vy;
                p.size = 5 + Math.random() * 4;
                p.life = 0.9 + Math.random() * 0.6;
                p.color = new Color(220, 200, 150, 160);
                p.type = Type.DUST;
                return p;
            }

            static Particle spark(double x, double y, double vx, double vy, Color base) {
                Particle p = new Particle();
                p.x = x; p.y = y; p.vx = vx; p.vy = vy;
                p.size = 3 + Math.random() * 2;
                p.life = 0.6 + Math.random() * 0.6;
                p.color = new Color(
                        Math.min(255, (int) (base.getRed() * 1.1)),
                        Math.min(255, (int) (base.getGreen() * 1.1)),
                        Math.min(255, (int) (base.getBlue() * 1.1)),
                        200);
                p.type = Type.SPARK;
                return p;
            }

            void draw(Graphics2D g2) {
                int a = (int) (Math.max(0, life) * 180);
                if (type == Type.DUST) {
                    g2.setColor(new Color(220, 200, 150, a));
                    g2.fill(new Ellipse2D.Double(x - size * 0.5, y - size * 0.5, size, size));
                } else {
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
                    g2.fill(new Ellipse2D.Double(x - size * 0.5, y - size * 0.5, size, size));
                }
            }
        }
    }
}