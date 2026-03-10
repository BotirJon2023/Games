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
        super("Volleyball Game Simulation - Dynamic Single File");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel panel = new GamePanel();
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
    }

    // GamePanel orchestrates the simulation, drawing and input
    static class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
        // Dimensions and geometry
        final int WIDTH = 1280;
        final int HEIGHT = 720;
        final Rectangle2D.Double COURT = new Rectangle2D.Double(60, 420, WIDTH - 120, 200);
        final double NET_X = WIDTH / 2.0;
        final double NET_WIDTH = 12.0;
        final double NET_HEIGHT = 220.0;
        final double NET_TOP_Y = COURT.getY() - NET_HEIGHT;
        final Rectangle2D.Double NET = new Rectangle2D.Double(NET_X - NET_WIDTH / 2.0, NET_TOP_Y, NET_WIDTH, NET_HEIGHT + COURT.getHeight());
        final Stroke NET_STROKE = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[]{6f, 6f}, 0f);

        // Time and stepping
        final int TICK_MS = 16; // ~60 fps
        long frameIndex = 0;
        long lastNano = System.nanoTime();

        // Physics constants
        final double G = 0.50;
        final double AIR_DRAG = 0.998;
        final double GROUND_DRAG = 0.84;
        final double BOUNCE_DAMP = 0.68;
        final double PLAYER_MOVE_ACC = 0.65;
        final double PLAYER_MAX_VX = 8.8;
        final double PLAYER_JUMP = 12.3;
        final double PLAYER_DIVE_SPEED = 10.0;
        final double PLAYER_DASH_SPEED = 11.0;
        final double PLAYER_DIVE_DURATION = 22;
        final double STAMINA_MAX = 100.0;
        final double STAMINA_RECOVERY = 0.18;
        final double STAMINA_JUMP_COST = 9.0;
        final double STAMINA_DIVE_COST = 16.0;
        final double STAMINA_DASH_COST = 14.0;
        final double BALL_MAX_SPEED = 22.0;
        final double BALL_MIN_HIT_SPEED = 5.4;
        final double MAGNUS_K = 0.0009; // spin effect scale
        final int TRAIL_LENGTH = 24;

        // Dynamic environment
        double wind = 0.0; // instantaneous wind (px/frame)
        double windTarget = 0.0;
        int windChangeTimer = 0;
        Random rng = new Random(7);
        double dayTime = 0.35; // 0..1 cycle
        double daySpeed = 0.0006; // per frame, slow
        double netSway = 0;      // sway angle or lateral offset
        double netSwayVel = 0;
        double netSpringK = 0.004;
        double netDamp = 0.86;

        // Camera shake
        double camShakeX = 0;
        double camShakeY = 0;
        double camShakeAmp = 0;
        double camShakeDecay = 0.90;

        // Entities
        Player leftPlayer;
        Player rightPlayer;
        Ball ball;
        ArrayList<Particle> particles = new ArrayList<>();
        Deque<Point2D.Double> ballTrail = new ArrayDeque<>();

        // Crowd, clouds, birds
        ArrayList<Cloud> clouds = new ArrayList<>();
        ArrayList<Bird> birds = new ArrayList<>();
        Crowd crowd = new Crowd();

        // Replay (ghost of last rally)
        java.util.List<Point2D.Double> lastRallyBallPath = new ArrayList<>();
        java.util.List<Point2D.Double> currentRallyBallPath = new ArrayList<>();
        boolean showReplayGhost = false;

        // Game state
        javax.swing.Timer timer;
        boolean paused = false;
        boolean fastForward = false;
        boolean stepOnce = false;
        boolean showDebug = false;
        boolean showPrediction = true;
        boolean showTrails = true;
        boolean showParticles = true;
        boolean showShadows = true;

        // Scoring
        int scoreLeft = 0;
        int scoreRight = 0;
        int setsLeft = 0;
        int setsRight = 0;
        int pointsToWin = 15;
        int rallyCount = 0;
        int touchCountLeft = 0;
        int touchCountRight = 0;
        Side lastTouch = Side.NONE;
        Side serveSide = Side.LEFT;
        boolean waitingForServe = true;

        // AI
        boolean aiLeft = false;  // let player control left by default
        boolean aiRight = true;
        AIDifficulty aiDifficulty = AIDifficulty.MEDIUM;

        // Input states
        boolean leftA, leftD, leftW, leftS, leftQ, leftZ, leftX, leftShift;
        boolean rightLeft, rightRight, rightUp, rightDown, rightO, rightSlash, rightPeriod, rightShift;
        boolean mousePressed = false;
        Point mousePoint = new Point(0, 0);

        // Constructor
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

        // Initialize game
        private void initGame() {
            leftPlayer = new Player("L", Side.LEFT, COURT.getX() + COURT.getWidth() * 0.25, COURT.getY() - 40, new Color(32, 120, 240));
            rightPlayer = new Player("R", Side.RIGHT, COURT.getX() + COURT.getWidth() * 0.75, COURT.getY() - 40, new Color(252, 96, 64));
            leftPlayer.aiEnabled = aiLeft;
            rightPlayer.aiEnabled = aiRight;
            leftPlayer.difficulty = aiDifficulty;
            rightPlayer.difficulty = aiDifficulty;

            ball = new Ball(COURT.getCenterX(), COURT.getY() - 120);
            ballTrail.clear();
            for (int i = 0; i < TRAIL_LENGTH; i++) {
                ballTrail.add(new Point2D.Double(ball.x, ball.y));
            }
            particles.clear();
            currentRallyBallPath.clear();
            lastRallyBallPath.clear();
            spawnInitialCloudsAndBirds();
            wind = 0;
            windTarget = 0.0;
            windChangeTimer = 0;
            dayTime = rng.nextDouble();
            netSway = 0;
            netSwayVel = 0;
            camShakeAmp = 0;
            resetForServe(serveSide);
        }

        private void spawnInitialCloudsAndBirds() {
            clouds.clear();
            birds.clear();
            for (int i = 0; i < 8; i++) {
                double y = 80 + rng.nextInt(120);
                double x = rng.nextInt(WIDTH);
                clouds.add(new Cloud(x, y, 0.2 + rng.nextDouble() * 0.4, 80 + rng.nextInt(80)));
            }
            for (int i = 0; i < 6; i++) {
                birds.add(new Bird(-50 - rng.nextInt(600), 120 + rng.nextInt(110), 1.0 + rng.nextDouble() * 1.5));
            }
            crowd.init(rng, WIDTH, (int) (COURT.getY() + COURT.getHeight()), 50, 8);
        }

        private void resetForServe(Side s) {
            waitingForServe = true;
            lastTouch = Side.NONE;
            touchCountLeft = 0;
            touchCountRight = 0;
            leftPlayer.reset(COURT.getX() + COURT.getWidth() * 0.25, COURT.getY() - 40);
            rightPlayer.reset(COURT.getX() + COURT.getWidth() * 0.75, COURT.getY() - 40);
            ball.vx = ball.vy = ball.spin = 0;
            ball.x = (s == Side.LEFT) ? leftPlayer.x + 10 : rightPlayer.x - 10;
            ball.y = ((s == Side.LEFT) ? leftPlayer.y : rightPlayer.y) - leftPlayer.radius - ball.radius - 12;
            currentRallyBallPath.clear();
            currentRallyBallPath.add(new Point2D.Double(ball.x, ball.y));
        }

        // Game loop tick
        @Override
        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            lastNano = now;
            int steps = fastForward ? 2 : 1;
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

        // Simulation update
        private void update() {
            frameIndex++;
            if (waitingForServe) {
                // idle sway and bob
                ball.vx *= 0.95;
                ball.vy *= 0.95;
                ball.y += Math.sin(frameIndex * 0.08) * 0.1;
                updateEnvironment();
                updateTrail();
                updateParticles();
                updateReplayGhostFade();
                if (autoServeAllowed()) {
                    if ((leftPlayer.aiEnabled || rightPlayer.aiEnabled) && frameIndex % 60 == 0) {
                        doServe(serveSide);
                    }
                }
                return;
            }

            // Input and AI
            handleInputAndAI();

            // Update environment
            updateEnvironment();

            // Update entities
            leftPlayer.update();
            rightPlayer.update();
            ball.update();

            // Record current rally path
            if (frameIndex % 2 == 0) currentRallyBallPath.add(new Point2D.Double(ball.x, ball.y));
            if (currentRallyBallPath.size() > 2000) currentRallyBallPath.remove(0);

            // Collisions and constraints
            handleCollisions();
            enforceCourtBounds();

            // Trails and particles
            updateTrail();
            spawnAutoParticles();
            updateParticles();

            // Faults and point check
            checkFaults();
            checkPoint();

            // Camera shake decay
            camShakeAmp *= camShakeDecay;
            camShakeX = (rng.nextDouble() - 0.5) * camShakeAmp;
            camShakeY = (rng.nextDouble() - 0.5) * camShakeAmp;
        }

        private void updateEnvironment() {
            // Day-night cycle
            dayTime += daySpeed;
            if (dayTime > 1) dayTime -= 1.0;
            // Wind target changes occasionally
            if (windChangeTimer-- <= 0) {
                windChangeTimer = 240 + rng.nextInt(240);
                double maxWind = 1.9;
                double sign = rng.nextBoolean() ? 1 : -1;
                windTarget = sign * rng.nextDouble() * maxWind;
            }
            wind += (windTarget - wind) * 0.01;
            // Net sway spring to wind
            double desired = wind * 10;
            double err = desired - netSway;
            netSwayVel += err * netSpringK;
            netSwayVel *= netDamp;
            netSway += netSwayVel;

            // Clouds
            for (Cloud c : clouds) {
                c.x += c.speed + wind * 0.2;
                if (c.x > WIDTH + 120) {
                    c.x = -200;
                    c.y = 60 + rng.nextInt(180);
                }
            }
            // Birds
            for (Bird b : birds) {
                b.update(wind);
                if (b.x > WIDTH + 80) {
                    b.x = -200 - rng.nextInt(300);
                    b.y = 100 + rng.nextInt(160);
                }
            }
            // Crowd bob
            crowd.update();
        }

        private void updateTrail() {
            if (!showTrails) {
                ballTrail.clear();
                return;
            }
            ballTrail.addLast(new Point2D.Double(ball.x, ball.y));
            while (ballTrail.size() > TRAIL_LENGTH) ballTrail.removeFirst();
        }

        private void spawnAutoParticles() {
            if (!showParticles) return;
            // dust when ball hits ground
            if (ball.lastGroundHitFrame >= 0 && frameIndex - ball.lastGroundHitFrame < 2) {
                for (int i = 0; i < 8; i++) {
                    double ang = rng.nextDouble() * Math.PI;
                    double sp = 1 + rng.nextDouble() * 2.5;
                    double vx = Math.cos(ang) * sp;
                    double vy = -Math.abs(Math.sin(ang) * sp * 0.7);
                    particles.add(Particle.dust(ball.x, COURT.getY(), vx, vy));
                }
                screenShake(8);
            }
            // players landing puffs
            if (leftPlayer.justLanded) {
                playerLandPuffs(leftPlayer.x);
                leftPlayer.justLanded = false;
            }
            if (rightPlayer.justLanded) {
                playerLandPuffs(rightPlayer.x);
                rightPlayer.justLanded = false;
            }
        }

        private void playerLandPuffs(double x) {
            for (int i = 0; i < 4; i++) {
                double vx = (rng.nextDouble() - 0.5) * 1.4;
                double vy = -rng.nextDouble() * 1.2;
                particles.add(Particle.dust(x, COURT.getY(), vx, vy));
            }
        }

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
                    p.vx *= 0.64;
                }
                if (p.life <= 0) particles.remove(i);
            }
        }

        private void handleInputAndAI() {
            // Left
            if (!leftPlayer.aiEnabled) {
                leftPlayer.intentLeft = leftA;
                leftPlayer.intentRight = leftD;
                leftPlayer.intentJump = leftW;
                leftPlayer.intentDash = leftShift && !leftPlayer.intentJump;
                leftPlayer.intentDive = leftZ;
                leftPlayer.intentCharge = leftQ;
            } else {
                applyAI(leftPlayer);
            }
            // Right
            if (!rightPlayer.aiEnabled) {
                rightPlayer.intentLeft = rightLeft;
                rightPlayer.intentRight = rightRight;
                rightPlayer.intentJump = rightUp;
                rightPlayer.intentDash = rightShift && !rightPlayer.intentJump;
                rightPlayer.intentDive = rightSlash;
                rightPlayer.intentCharge = rightO;
            } else {
                applyAI(rightPlayer);
            }
            // Keep intents within halves
            clampIntentByCourt(leftPlayer);
            clampIntentByCourt(rightPlayer);
        }

        private void applyAI(Player p) {
            // Basic plan: move to predicted landing spot on own side; if near net and ball near top, charge spike
            Prediction pred = predictBallLanding(260);
            boolean onMySide = (p.side == Side.LEFT) ? (ball.x < NET.getCenterX()) : (ball.x > NET.getCenterX());
            double halfLeft = COURT.getX();
            double halfRight = COURT.getX() + COURT.getWidth();
            if (p.side == Side.LEFT) halfRight = NET.getCenterX() - p.radius - 8;
            if (p.side == Side.RIGHT) halfLeft = NET.getCenterX() + p.radius + 8;

            double targetX;
            if (onMySide) {
                targetX = clamp(pred.landingX + (p.side == Side.LEFT ? -8 : 8), halfLeft + 20, halfRight - 20);
            } else {
                // idle near center of own half
                targetX = clamp((halfLeft + halfRight) * 0.5, halfLeft + 30, halfRight - 30);
            }

            // movement intents
            double dx = targetX - p.x;
            p.intentLeft = dx < -6;
            p.intentRight = dx > 6;

            // Decision to jump
            boolean shouldJump = false;
            if (onMySide && ball.y < p.y - 45 && Math.abs(ball.x - p.x) < 45) {
                shouldJump = true;
            }
            if (ball.y < NET_TOP_Y + 40 && Math.abs(ball.x - NET.getCenterX()) < 120 && onMySide) {
                // go for spike near net
                targetX = clamp(NET.getCenterX() + (p.side == Side.LEFT ? -40 : 40), halfLeft + 30, halfRight - 30);
                dx = targetX - p.x;
                p.intentLeft = dx < -4;
                p.intentRight = dx > 4;
                shouldJump = Math.abs(ball.x - p.x) < 36 || ball.vy > 0;
            }
            // random jitter improvements with difficulty
            double rand = rng.nextDouble();
            if (p.difficulty == AIDifficulty.EASY && rand < 0.02) shouldJump = false;
            if (p.difficulty == AIDifficulty.HARD && rand < 0.02) p.intentDash = true; else p.intentDash = false;

            p.intentJump = shouldJump;

            // Diving for low saves
            if (onMySide && ball.vy > 3 && ball.y > p.y - 10 && Math.abs(ball.x - p.x) < 50 && rng.nextDouble() < (p.difficulty == AIDifficulty.HARD ? 0.22 : 0.1)) {
                p.intentDive = true;
            } else {
                p.intentDive = false;
            }

            // Charge spike
            p.intentCharge = false;
            if (onMySide && Math.abs(ball.x - NET.getCenterX()) < 140 && ball.y < p.y - 10) {
                if (Math.abs(p.x - targetX) < 20 && rng.nextDouble() < (p.difficulty == AIDifficulty.HARD ? 0.9 : 0.6)) {
                    p.intentCharge = true;
                }
            }
        }

        // Predict ball landing (ignoring player collisions)
        private Prediction predictBallLanding(int steps) {
            double px = ball.x;
            double py = ball.y;
            double vx = ball.vx;
            double vy = ball.vy;
            double spin = ball.spin;
            double landingX = px;
            boolean willCross = false;
            for (int i = 0; i < steps; i++) {
                // Magnus effect
                double ax = wind + MAGNUS_K * spin * vy;
                double ay = G - MAGNUS_K * spin * vx * 0.7;
                vx += ax;
                vy += ay;

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

        private boolean autoServeAllowed() {
            // Always allow auto-serve if both are AI; else manual
            return leftPlayer.aiEnabled && rightPlayer.aiEnabled;
        }

        private void doServe(Side s) {
            waitingForServe = false;
            double base = s == Side.LEFT ? 7.8 : -7.8;
            double toss = -8.8 - rng.nextDouble() * 2.2;
            ball.vx = base + wind * 6 + (rng.nextDouble() - 0.5) * 1.4;
            ball.vy = toss;
            ball.spin = (rng.nextDouble() - 0.5) * 0.02;
            ball.lastGroundHitFrame = -1;
        }

        private void handleCollisions() {
            // Ground
            if (ball.y + ball.radius >= COURT.getY()) {
                ball.y = COURT.getY() - ball.radius;
                ball.vy = -ball.vy * BOUNCE_DAMP;
                ball.vx *= GROUND_DRAG;
                ball.spin *= 0.92;
                ball.lastGroundHitFrame = frameIndex;
            }
            double leftWall = COURT.getX();
            double rightWall = COURT.getX() + COURT.getWidth();
            // Side walls clamp
            if (ball.x - ball.radius < leftWall) {
                ball.x = leftWall + ball.radius;
                ball.vx = -ball.vx * 0.92;
                ball.spin *= 0.98;
            }
            if (ball.x + ball.radius > rightWall) {
                ball.x = rightWall - ball.radius;
                ball.vx = -ball.vx * 0.92;
                ball.spin *= 0.98;
            }
            // Net rectangle collision (includes sway horizontal offset)
            double netOffset = netSway * 0.2;
            Rectangle2D.Double swayedNet = new Rectangle2D.Double(NET.getMinX() + netOffset, NET.getY(), NET.getWidth(), NET.getHeight());
            if (ball.y + ball.radius > swayedNet.getY() && ball.y - ball.radius < swayedNet.getMaxY()) {
                if (ball.x + ball.radius > swayedNet.getMinX() && ball.x - ball.radius < swayedNet.getMaxX()) {
                    boolean fromLeft = ball.x < swayedNet.getCenterX();
                    if ((fromLeft && ball.vx > 0) || (!fromLeft && ball.vx < 0)) {
                        ball.vx = -ball.vx * 0.64;
                        ball.x = fromLeft ? (swayedNet.getMinX() - ball.radius) : (swayedNet.getMaxX() + ball.radius);
                        ball.spin *= 0.95;
                        netSwayVel += (fromLeft ? 1 : -1) * Math.abs(ball.vx) * 0.4;
                        screenShake(4);
                    }
                    // top cable bounce
                    if (ball.y + ball.radius > swayedNet.getY() && ball.y < swayedNet.getY() + 4) {
                        ball.vy = -Math.abs(ball.vy) * 0.6;
                        ball.y = swayedNet.getY() - ball.radius;
                        screenShake(3);
                    }
                }
            }
            // Player collisions
            collideBallWithPlayer(leftPlayer);
            collideBallWithPlayer(rightPlayer);
        }

        private void screenShake(double amp) {
            camShakeAmp = Math.min(20, camShakeAmp + amp);
        }

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

                // relative velocity
                double rvx = ball.vx - p.vx;
                double rvy = ball.vy - p.vy;
                double vn = rvx * nx + rvy * ny;

                if (vn < 0) {
                    // Coefficient of restitution varied with charge and dive
                    double cr = 0.9;
                    if (p.isCharging) cr += 0.25;
                    if (p.isDiving) cr += 0.08;
                    if (cr > 1.1) cr = 1.1;

                    double j = -(1 + cr) * vn;
                    ball.vx += j * nx;
                    ball.vy += j * ny;

                    // Add player's own velocity influence
                    ball.vx += p.vx * 0.45;
                    ball.vy += p.vy * 0.35;

                    // Aim influence from intents
                    double aimX = (p.intentRight ? 1 : 0) - (p.intentLeft ? 1 : 0);
                    double aimY = p.intentJump ? -0.6 : -0.15;
                    double aimScale = p.isCharging ? 2.6 : 1.6;
                    ball.vx += aimX * aimScale;
                    ball.vy += aimY * aimScale;

                    // Spin from tangential impact
                    double tx = -ny, ty = nx; // tangent
                    double vt = rvx * tx + rvy * ty;
                    double spinDelta = vt * 0.0035 + (p.isCharging ? 0.01 : 0) + (p.isDiving ? 0.006 : 0);
                    ball.spin += spinDelta;
                    // clamp spin
                    if (Math.abs(ball.spin) > 0.08) ball.spin *= 0.92;

                    // Clamp speed
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

                    // last touch
                    if (p.side != lastTouch) {
                        if (p.side == Side.LEFT) touchCountLeft = 0; else touchCountRight = 0;
                    }
                    lastTouch = p.side;
                    if (p.side == Side.LEFT) touchCountLeft++; else touchCountRight++;

                    p.lastContactFrame = frameIndex;
                    spawnHitParticles(p, nx, ny, sp);
                    screenShake(Math.min(12, sp * 0.4 + (p.isCharging ? 6 : 0)));
                }
            }
        }

        private void spawnHitParticles(Player p, double nx, double ny, double sp) {
            if (!showParticles) return;
            int count = 10 + (p.isCharging ? 8 : 0);
            for (int i = 0; i < count; i++) {
                double ang = Math.atan2(ny, nx) + (rng.nextDouble() - 0.5) * 0.7;
                double speed = 2 + rng.nextDouble() * (p.isCharging ? 3.5 : 2.5);
                double vx = Math.cos(ang) * speed;
                double vy = Math.sin(ang) * speed;
                particles.add(Particle.spark(ball.x, ball.y, vx, vy, p.color));
            }
        }

        private void enforceCourtBounds() {
            keepInHalf(leftPlayer);
            keepInHalf(rightPlayer);
            for (Player p : Arrays.asList(leftPlayer, rightPlayer)) {
                // ground
                if (p.y + p.radius >= COURT.getY()) {
                    if (!p.onGround && p.vy > 5) p.justLanded = true;
                    p.y = COURT.getY() - p.radius;
                    p.vy = 0;
                    p.onGround = true;
                    p.jumpsLeft = 1;
                    p.vx *= GROUND_DRAG;
                } else {
                    p.onGround = false;
                }
                // stamina recovery
                p.stamina = clamp(p.stamina + STAMINA_RECOVERY, 0, STAMINA_MAX);
            }
        }

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

        private void checkFaults() {
            if (touchCountLeft > 3) {
                awardPoint(Side.RIGHT, "Left team 4 hits");
            } else if (touchCountRight > 3) {
                awardPoint(Side.LEFT, "Right team 4 hits");
            }
        }

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

        private void awardPoint(Side to, String reason) {
            if (to == Side.LEFT) scoreLeft++; else scoreRight++;
            rallyCount++;
            // store replay
            lastRallyBallPath = new ArrayList<>(currentRallyBallPath);
            currentRallyBallPath.clear();

            // celebratory particles at net
            if (showParticles) {
                for (int i = 0; i < 26; i++) {
                    double ang = rng.nextDouble() * Math.PI * 2;
                    double sp = 1 + rng.nextDouble() * 3.2;
                    particles.add(Particle.spark(NET.getCenterX(), NET.getY(), Math.cos(ang) * sp, Math.sin(ang) * sp, to == Side.LEFT ? leftPlayer.color : rightPlayer.color));
                }
            }
            serveSide = to;
            // set win check
            if ((scoreLeft >= pointsToWin || scoreRight >= pointsToWin) && Math.abs(scoreLeft - scoreRight) >= 2) {
                if (scoreLeft > scoreRight) setsLeft++; else setsRight++;
                scoreLeft = 0;
                scoreRight = 0;
                // switch sides
                leftPlayer.side = (leftPlayer.side == Side.LEFT) ? Side.RIGHT : Side.LEFT;
                rightPlayer.side = (rightPlayer.side == Side.LEFT) ? Side.RIGHT : Side.LEFT;
                // swap colors to emphasize
                Color lc = leftPlayer.color;
                leftPlayer.color = rightPlayer.color;
                rightPlayer.color = lc;
            }
            resetForServe(serveSide);
        }

        private void updateReplayGhostFade() {
            // no-op for now; could animate fade of lastRallyBallPath if desired
        }

        // Drawing pipeline
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Apply camera shake transform
            g2.translate(camShakeX, camShakeY);

            drawBackground(g2);
            drawCrowd(g2);
            drawCourt(g2);
            drawNet(g2);
            drawCloudsAndBirds(g2);
            if (showReplayGhost) drawReplayGhost(g2);
            if (showPrediction && !waitingForServe) drawPrediction(g2);
            if (showTrails) drawTrail(g2);
            leftPlayer.draw(g2);
            rightPlayer.draw(g2);
            ball.draw(g2);
            drawParticles(g2);
            drawHUD(g2);

            g2.dispose();
        }

        private void drawBackground(Graphics2D g2) {
            // dayTime 0..1 -> gradient colors
            Color skyTop = lerpColor(new Color(20, 40, 90), new Color(155, 205, 255), sunriseAmount(dayTime));
            Color skyBot = lerpColor(new Color(40, 60, 120), new Color(210, 235, 255), sunriseAmount(dayTime) * 0.8 + 0.2);
            GradientPaint gp = new GradientPaint(0, 0, skyTop, 0, HEIGHT, skyBot);
            g2.setPaint(gp);
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            // Sun/moon
            double ang = dayTime * Math.PI * 2;
            double sunX = WIDTH * 0.5 + Math.cos(ang - Math.PI / 2) * 400;
            double sunY = 180 + Math.sin(ang - Math.PI / 2) * 120;
            boolean isNight = isNight(dayTime);
            if (!isNight) {
                g2.setColor(new Color(255, 245, 180, 180));
                g2.fillOval((int) (sunX - 50), (int) (sunY - 50), 100, 100);
            } else {
                g2.setColor(new Color(220, 230, 255, 180));
                g2.fillOval((int) (sunX - 26), (int) (sunY - 26), 52, 52);
            }

            // Stadium lights at night
            if (isNight) {
                g2.setColor(new Color(200, 200, 220));
                int lx1 = (int) (COURT.getX() - 30);
                int lx2 = (int) (COURT.getMaxX() + 30);
                g2.fillRect(lx1, (int) (NET.getY() - 30), 10, 150);
                g2.fillRect(lx2, (int) (NET.getY() - 30), 10, 150);
                // light beams
                drawLightBeam(g2, lx1 + 5, (int) (NET.getY() - 30));
                drawLightBeam(g2, lx2 + 5, (int) (NET.getY() - 30));
            }
        }

        private void drawLightBeam(Graphics2D g2, int x, int y) {
            Polygon beam = new Polygon();
            beam.addPoint(x, y);
            beam.addPoint((int) (x - 200), (int) (COURT.getY() + 20));
            beam.addPoint((int) (x + 200), (int) (COURT.getY() + 20));
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillPolygon(beam);
        }

        private double sunriseAmount(double t) {
            // map to 0 at midnight, 1 at noon
            double v = Math.sin(t * Math.PI);
            return clamp(v, 0, 1);
        }

        private boolean isNight(double t) {
            return t < 0.20 || t > 0.80;
        }

        private void drawCrowd(Graphics2D g2) {
            crowd.draw(g2, (int) COURT.getX(), (int) COURT.getMaxY(), (int) COURT.getWidth());
        }

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
            if (showShadows) {
                drawShadow(g2, leftPlayer.x, COURT.getY(), leftPlayer.radius);
                drawShadow(g2, rightPlayer.x, COURT.getY(), rightPlayer.radius);
                drawShadow(g2, ball.x, COURT.getY(), ball.radius * 0.9);
            }
        }

        private void drawShadow(Graphics2D g2, double x, double y, double r) {
            RadialGradientPaint rg = new RadialGradientPaint(new Point2D.Double(x, y), (float) (r * 2.3f),
                    new float[]{0f, 1f}, new Color[]{new Color(0, 0, 0, 60), new Color(0, 0, 0, 0)});
            Paint old = g2.getPaint();
            g2.setPaint(rg);
            g2.fill(new Ellipse2D.Double(x - r * 2.3, y - r * 0.6, r * 4.6, r * 1.2));
            g2.setPaint(old);
        }

        private void drawNet(Graphics2D g2) {
            double netOffset = netSway * 0.2;
            int minX = (int) (NET.getMinX() + netOffset);
            int maxX = (int) (NET.getMaxX() + netOffset);
            int yTop = (int) NET.getY();

            // posts
            g2.setColor(new Color(90, 90, 110));
            g2.fillRect(minX - 6, yTop - 30, 4, (int) (NET.getHeight() + 50));
            g2.fillRect(maxX + 2, yTop - 30, 4, (int) (NET.getHeight() + 50));
            // strap top
            g2.setColor(new Color(230, 230, 240));
            g2.fillRect(minX, yTop - 2, (int) NET.getWidth(), 6);
            // mesh
            g2.setColor(new Color(30, 30, 40, 180));
            g2.setStroke(NET_STROKE);
            for (int y = yTop; y < NET.getMaxY(); y += 16) {
                g2.drawLine(minX, y, maxX, y);
            }
        }

        private void drawCloudsAndBirds(Graphics2D g2) {
            for (Cloud c : clouds) c.draw(g2);
            for (Bird b : birds) b.draw(g2);
        }

        private void drawPrediction(Graphics2D g2) {
            Prediction pred = predictBallLanding(260);
            double px = ball.x;
            double py = ball.y;
            double vx = ball.vx;
            double vy = ball.vy;
            double spin = ball.spin;

            g2.setColor(new Color(0, 0, 0, 80));
            for (int i = 0; i < 200; i++) {
                double ax = wind + MAGNUS_K * spin * vy;
                double ay = G - MAGNUS_K * spin * vx * 0.7;
                vx += ax;
                vy += ay;
                px += vx;
                py += vy;
                vx *= AIR_DRAG;
                // ground stop
                if (py + ball.radius >= COURT.getY()) break;

                if (i % 6 == 0) {
                    g2.fill(new Ellipse2D.Double(px - 2, py - 2, 4, 4));
                }
                // net bounce approx
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

        private void drawParticles(Graphics2D g2) {
            for (Particle p : particles) p.draw(g2);
        }

        private void drawReplayGhost(Graphics2D g2) {
            if (lastRallyBallPath == null || lastRallyBallPath.size() < 2) return;
            g2.setStroke(new BasicStroke(2f));
            for (int i = 1; i < lastRallyBallPath.size(); i++) {
                Point2D.Double a = lastRallyBallPath.get(i - 1);
                Point2D.Double b = lastRallyBallPath.get(i);
                float t = (float) i / (float) lastRallyBallPath.size();
                g2.setColor(new Color(50, 80, 160, (int) (t * 70)));
                g2.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);
            }
        }

        private void drawHUD(Graphics2D g2) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22f));
            g2.setColor(new Color(20, 40, 60));
            String score = "Score  L " + scoreLeft + " : " + scoreRight + " R";
            String sets = "Sets  L " + setsLeft + " : " + setsRight + " R   (to " + pointsToWin + ")";
            g2.drawString(score, 20, 36);
            g2.drawString(sets, 20, 64);

            // Serve indicator
            String serve = "Serve: " + (serveSide == Side.LEFT ? "Left" : "Right") + (waitingForServe ? " [waiting]" : "");
            g2.drawString(serve, 20, 92);

            // AI and difficulty
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
            g2.drawString("Left AI: " + (leftPlayer.aiEnabled ? "ON" : "OFF"), 20, 118);
            g2.drawString("Right AI: " + (rightPlayer.aiEnabled ? "ON" : "OFF"), 120, 118);
            g2.drawString("AI Difficulty: " + aiDifficulty, 240, 118);
            g2.drawString("Touches L: " + touchCountLeft + "  R: " + touchCountRight, 20, 142);

            // Wind HUD
            int wx = WIDTH - 260;
            g2.drawString("Wind", wx, 32);
            drawWindBar(g2, wx, 40, 200, wind);

            // Controls
            int y = HEIGHT - 86;
            g2.drawString("Controls:", 20, y);
            g2.drawString("[P] Pause  [N]/[Space] Serve  [R] Reset  [F] 2x Speed  [T] Trails  [X] Prediction  [C] Particles  [S] Shadows  [G] Debug  [V] Ghost", 100, y);
            g2.drawString("Left: A/D move, W jump, Z dive, Q charge, Shift dash    Right: ←/→ move, ↑ jump, / dive, O charge, RightShift dash", 100, y + 20);
            g2.drawString("[1]/[2] toggle AI per side, [3] cycle AI difficulty", 100, y + 40);

            if (paused) {
                drawCenteredBanner(g2, "PAUSED");
            } else if (waitingForServe) {
                drawCenteredBanner(g2, "PRESS N OR SPACE TO SERVE");
            }
            if (showDebug) {
                g2.setColor(new Color(0, 0, 0, 120));
                g2.drawString("Ball pos=(" + fmt(ball.x) + "," + fmt(ball.y) + ") v=(" + fmt(ball.vx) + "," + fmt(ball.vy) + ") spin=" + fmt(ball.spin) + " wind=" + fmt(wind), 20, HEIGHT - 120);
                g2.drawString("L pos=(" + fmt(leftPlayer.x) + "," + fmt(leftPlayer.y) + ") v=(" + fmt(leftPlayer.vx) + "," + fmt(leftPlayer.vy) + ") stamina=" + (int) leftPlayer.stamina, 20, HEIGHT - 100);
                g2.drawString("R pos=(" + fmt(rightPlayer.x) + "," + fmt(rightPlayer.y) + ") v=(" + fmt(rightPlayer.vx) + "," + fmt(rightPlayer.vy) + ") stamina=" + (int) rightPlayer.stamina, 20, HEIGHT - 80);
            }
        }

        private void drawWindBar(Graphics2D g2, int x, int y, int w, double wind) {
            g2.setColor(new Color(230, 230, 240));
            g2.fillRoundRect(x, y, w, 10, 6, 6);
            g2.setColor(new Color(40, 80, 150));
            int cx = x + w / 2;
            int bar = (int) (wind / 2.0 * (w / 2)); // wind approx from -2..2
            g2.fillRoundRect(Math.min(cx, cx + bar), y, Math.abs(bar), 10, 6, 6);
            // arrow
            int ax = cx + (int) (wind * 50);
            g2.setColor(new Color(20, 40, 60));
            g2.drawLine(ax - 8, y + 16, ax + 8, y + 16);
            g2.drawLine(ax + (int) Math.signum(wind) * 8, y + 16, ax + (int) Math.signum(wind) * 3, y + 12);
            g2.drawLine(ax + (int) Math.signum(wind) * 8, y + 16, ax + (int) Math.signum(wind) * 3, y + 20);
        }

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

        // Formatting helper
        private String fmt(double v) {
            return String.format(Locale.US, "%.2f", v);
        }

        private double clamp(double v, double a, double b) {
            return Math.max(a, Math.min(b, v));
        }

        private Color lerpColor(Color a, Color b, double t) {
            t = clamp(t, 0, 1);
            int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bb = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            int aa = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
            return new Color(r, g, bb, aa);
        }

        // Input
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                // Left player keys
                case KeyEvent.VK_A: leftA = true; break;
                case KeyEvent.VK_D: leftD = true; break;
                case KeyEvent.VK_W: leftW = true; break;
                case KeyEvent.VK_S: leftS = true; break;
                case KeyEvent.VK_Q: leftQ = true; break;
                case KeyEvent.VK_Z: leftZ = true; break;
                case KeyEvent.VK_X: leftX = true; break;
                case KeyEvent.VK_SHIFT: leftShift = true; rightShift = true; break; // both sides can use Shift
                // Right player keys
                case KeyEvent.VK_LEFT: rightLeft = true; break;
                case KeyEvent.VK_RIGHT: rightRight = true; break;
                case KeyEvent.VK_UP: rightUp = true; break;
                case KeyEvent.VK_DOWN: rightDown = true; break;
                case KeyEvent.VK_O: rightO = true; break;
                case KeyEvent.VK_SLASH: rightSlash = true; break;
                case KeyEvent.VK_PERIOD: rightPeriod = true; break;

                // Global
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
                    scoreLeft = scoreRight = setsLeft = setsRight = 0;
                    serveSide = Side.LEFT;
                    initGame();
                    break;
                case KeyEvent.VK_T:
                    showTrails = !showTrails;
                    break;
                case KeyEvent.VK_X:
                    if (!leftX) { // avoid conflict with leftX flag
                        showPrediction = !showPrediction;
                    }
                    break;
                case KeyEvent.VK_C:
                    showParticles = !showParticles;
                    break;
                case KeyEvent.VK_S:
                    showShadows = !showShadows;
                    break;
                case KeyEvent.VK_G:
                    showDebug = !showDebug;
                    break;
                case KeyEvent.VK_V:
                    showReplayGhost = !showReplayGhost;
                    break;
                case KeyEvent.VK_1:
                    aiLeft = !aiLeft; leftPlayer.aiEnabled = aiLeft;
                    break;
                case KeyEvent.VK_2:
                    aiRight = !aiRight; rightPlayer.aiEnabled = aiRight;
                    break;
                case KeyEvent.VK_3:
                    aiDifficulty = aiDifficulty.next();
                    leftPlayer.difficulty = aiDifficulty;
                    rightPlayer.difficulty = aiDifficulty;
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
                // Left player keys
                case KeyEvent.VK_A: leftA = false; break;
                case KeyEvent.VK_D: leftD = false; break;
                case KeyEvent.VK_W: leftW = false; break;
                case KeyEvent.VK_S: leftS = false; break;
                case KeyEvent.VK_Q: leftQ = false; break;
                case KeyEvent.VK_Z: leftZ = false; break;
                case KeyEvent.VK_X: leftX = false; break;
                case KeyEvent.VK_SHIFT: leftShift = false; rightShift = false; break;

                // Right player keys
                case KeyEvent.VK_LEFT: rightLeft = false; break;
                case KeyEvent.VK_RIGHT: rightRight = false; break;
                case KeyEvent.VK_UP: rightUp = false; break;
                case KeyEvent.VK_DOWN: rightDown = false; break;
                case KeyEvent.VK_O: rightO = false; break;
                case KeyEvent.VK_SLASH: rightSlash = false; break;
                case KeyEvent.VK_PERIOD: rightPeriod = false; break;
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

        // Enums and helper classes
        enum Side { LEFT, RIGHT, NONE }

        enum AIDifficulty {
            EASY, MEDIUM, HARD;
            AIDifficulty next() {
                switch (this) {
                    case EASY: return MEDIUM;
                    case MEDIUM: return HARD;
                    case HARD: default: return EASY;
                }
            }
        }

        static class Prediction {
            final double landingX;
            final boolean willCross;
            Prediction(double x, boolean c) { landingX = x; willCross = c; }
        }

        // Player
        class Player {
            String name;
            Side side;
            double x, y;
            double vx, vy;
            double radius = 26;
            boolean onGround = false;
            boolean justLanded = false;
            boolean aiEnabled = true;

            // Intents
            boolean intentLeft = false;
            boolean intentRight = false;
            boolean intentJump = false;
            boolean intentDive = false;
            boolean intentDash = false;
            boolean intentCharge = false;

            // States
            boolean isDiving = false;
            int diveTimer = 0;
            boolean isCharging = false;
            int chargeLevel = 0; // 0..100
            int jumpsLeft = 1;
            long lastContactFrame = -1;
            double stamina = STAMINA_MAX;
            AIDifficulty difficulty = AIDifficulty.MEDIUM;

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
                this.intentDive = false;
                this.intentDash = false;
                this.intentCharge = false;
                this.isDiving = false;
                this.isCharging = false;
                this.chargeLevel = 0;
                this.jumpsLeft = 1;
                this.stamina = STAMINA_MAX;
            }

            void update() {
                // Charging
                if (intentCharge && stamina > 5) {
                    isCharging = true;
                    chargeLevel = Math.min(100, chargeLevel + 3);
                    stamina = Math.max(0, stamina - 0.3);
                } else {
                    isCharging = false;
                    chargeLevel = Math.max(0, chargeLevel - 2);
                }

                // Movement
                double ax = 0;
                if (intentLeft) ax -= PLAYER_MOVE_ACC;
                if (intentRight) ax += PLAYER_MOVE_ACC;

                if (intentDash && stamina > STAMINA_DASH_COST && onGround) {
                    vx += (ax >= 0 ? 1 : -1) * PLAYER_DASH_SPEED;
                    stamina = Math.max(0, stamina - STAMINA_DASH_COST);
                    intentDash = false;
                }

                // Diving
                if (intentDive && stamina > STAMINA_DIVE_COST && !isDiving) {
                    isDiving = true;
                    diveTimer = (int) PLAYER_DIVE_DURATION;
                    stamina = Math.max(0, stamina - STAMINA_DIVE_COST);
                    vy = -2;
                    vx += (intentRight ? 1 : intentLeft ? -1 : (side == Side.LEFT ? 1 : -1)) * PLAYER_DIVE_SPEED;
                }
                if (isDiving) {
                    diveTimer--;
                    if (diveTimer <= 0) isDiving = false;
                    // reduced control while diving
                    ax *= 0.4;
                }

                vx += ax;
                vx *= 0.985;
                if (vx > PLAYER_MAX_VX) vx = PLAYER_MAX_VX;
                if (vx < -PLAYER_MAX_VX) vx = -PLAYER_MAX_VX;

                // Jump
                if (intentJump) {
                    if (onGround && stamina > STAMINA_JUMP_COST) {
                        vy = -PLAYER_JUMP * jumpScaleByCharge();
                        onGround = false;
                        jumpsLeft = 0;
                        stamina = Math.max(0, stamina - STAMINA_JUMP_COST);
                        intentJump = false;
                    } else if (!onGround && jumpsLeft > 0 && stamina > STAMINA_JUMP_COST * 0.6) {
                        vy = -PLAYER_JUMP * 0.9;
                        jumpsLeft = 0;
                        stamina = Math.max(0, stamina - STAMINA_JUMP_COST * 0.6);
                        intentJump = false;
                    }
                }

                // gravity
                vy += G;
                x += vx;
                y += vy;
            }

            double jumpScaleByCharge() {
                return 1.0 + (chargeLevel / 100.0) * 0.12;
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
                g2.setColor(new Color(255, 255, 255, 200));
                g2.fillRoundRect((int) (x - radius * 0.6), (int) (y - radius * 0.9), (int) (radius * 1.2), 6, 4, 4);

                // eyes direction to ball
                double ex = x - radius * 0.2 + Math.signum(ball.x - x) * 2;
                double ey = y - radius * 0.3 + Math.signum(ball.y - y) * 1;
                g2.setColor(new Color(20, 30, 40));
                g2.fill(new Ellipse2D.Double(ex, ey, 4, 4));
                g2.fill(new Ellipse2D.Double(ex + 10, ey, 4, 4));

                // jersey text
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
                g2.setColor(Color.WHITE);
                String txt = name + (aiEnabled ? " AI" : "");
                drawCenteredString(g2, txt, (int) x, (int) (y + 7));

                // contact flash
                if (frameIndex - lastContactFrame < 6) {
                    float t = (6 - (frameIndex - lastContactFrame)) / 6f;
                    g2.setColor(new Color(255, 255, 255, (int) (t * 200)));
                    g2.setStroke(new BasicStroke(3f));
                    g2.draw(new Ellipse2D.Double(x - radius - 4, y - radius - 4, (radius + 4) * 2, (radius + 4) * 2));
                }

                // stamina bar
                int bw = 50, bh = 6;
                int bx = (int) (x - bw / 2), by = (int) (y - radius - 18);
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillRoundRect(bx - 1, by - 1, bw + 2, bh + 2, 6, 6);
                g2.setColor(new Color(40, 160, 60, 200));
                int fill = (int) (bw * (stamina / STAMINA_MAX));
                g2.fillRoundRect(bx, by, fill, bh, 6, 6);

                // charge halo
                if (isCharging && chargeLevel > 0) {
                    int halo = (int) (10 + chargeLevel * 0.2);
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
                    g2.fill(new Ellipse2D.Double(x - radius - halo * 0.5, y - radius - halo * 0.5, (radius * 2) + halo, (radius * 2) + halo));
                }

                // dive indicator
                if (isDiving) {
                    g2.setColor(new Color(255, 255, 255, 140));
                    g2.draw(new Ellipse2D.Double(x - radius - 2, y - radius - 2, (radius + 2) * 2, (radius + 2) * 2));
                }
            }

            void drawCenteredString(Graphics2D g2, String s, int cx, int cy) {
                FontMetrics fm = g2.getFontMetrics();
                int w = fm.stringWidth(s);
                g2.drawString(s, cx - w / 2, cy);
            }
        }

        // Ball
        class Ball {
            double x, y;
            double vx, vy;
            double spin; // positive -> curves to left relative to motion
            double radius = 12;
            long lastGroundHitFrame = -1;

            Ball(double x, double y) {
                this.x = x; this.y = y;
            }

            void update() {
                // Magnus effect + wind
                double ax = wind + MAGNUS_K * spin * vy;
                double ay = G - MAGNUS_K * spin * vx * 0.7;

                vx += ax;
                vy += ay;

                x += vx;
                y += vy;

                vx *= AIR_DRAG;
                // spin decay
                spin *= 0.999;
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
                // ball
                g2.setColor(new Color(255, 250, 240));
                g2.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
                g2.setColor(new Color(220, 110, 40));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
                // seams
                g2.setColor(new Color(200, 80, 40));
                g2.drawArc((int) (x - radius), (int) (y - radius), (int) (radius * 2), (int) (radius * 2), 30, 120);
                g2.drawArc((int) (x - radius), (int) (y - radius), (int) (radius * 2), (int) (radius * 2), 210, 120);

                // spin arrow
                if (showDebug) {
                    int len = (int) Math.signum(spin) * 16;
                    g2.setColor(new Color(60, 100, 200));
                    g2.drawLine((int) x, (int) y, (int) (x + len), (int) y);
                }
            }
        }

        // Particle
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

        // Cloud
        static class Cloud {
            double x, y, speed;
            int width;
            Cloud(double x, double y, double speed, int width) {
                this.x = x; this.y = y; this.speed = speed; this.width = width;
            }
            void draw(Graphics2D g2) {
                g2.setColor(new Color(255, 255, 255, 200));
                g2.fillRoundRect((int) x, (int) y, width, 28, 20, 20);
                g2.fillRoundRect((int) x + 18, (int) y - 12, width - 30, 26, 18, 18);
                g2.fillRoundRect((int) x - 16, (int) y - 6, width - 10, 24, 18, 18);
            }
        }

        // Bird
        static class Bird {
            double x, y, speed;
            double flap = Math.random() * Math.PI * 2;
            Bird(double x, double y, double speed) { this.x = x; this.y = y; this.speed = speed; }
            void update(double wind) {
                flap += 0.3;
                y += Math.sin(flap) * 0.3;
                x += speed + wind * 0.4;
            }
            void draw(Graphics2D g2) {
                g2.setColor(new Color(30, 30, 30, 160));
                int fx = (int) x, fy = (int) y;
                int w = 10, h = 4;
                int dy = (int) (Math.sin(flap) * 3);
                g2.drawLine(fx - w, fy, fx, fy + dy);
                g2.drawLine(fx, fy + dy, fx + w, fy);
            }
        }

        // Crowd
        static class Crowd {
            static class Person { int x, y, size; Color c; double t; }
            ArrayList<Person> people = new ArrayList<>();
            void init(Random rng, int width, int baseY, int rows, int cols) {
                people.clear();
                int spacingX = width / (cols + 1);
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols + r; c++) {
                        Person p = new Person();
                        p.x = 60 + c * spacingX + rng.nextInt(30) - 15 + r * 8;
                        p.y = baseY + 30 + r * 12 + rng.nextInt(6);
                        p.size = 6 + rng.nextInt(6);
                        p.c = new Color(80 + rng.nextInt(160), 60 + rng.nextInt(160), 60 + rng.nextInt(160), 160);
                        p.t = rng.nextDouble() * Math.PI * 2;
                        people.add(p);
                    }
                }
            }
            void update() {
                for (Person p : people) {
                    p.t += 0.05 + (Math.random() * 0.02 - 0.01);
                }
            }
            void draw(Graphics2D g2, int courtX, int crowdTop, int width) {
                Shape oldClip = g2.getClip();
                g2.setClip(courtX, (int) (crowdTop + 36), width, 120);
                for (Person p : people) {
                    int yy = (int) (p.y + Math.sin(p.t) * 2);
                    g2.setColor(p.c);
                    g2.fillOval(p.x, yy, p.size, p.size);
                }
                g2.setClip(oldClip);
            }
        }
    }
}