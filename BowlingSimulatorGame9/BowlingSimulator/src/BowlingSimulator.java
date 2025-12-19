import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;


public class BowlingSimulator extends JFrame {

    // Preferred sizes
    public static final int WINDOW_WIDTH = 1024;
    public static final int WINDOW_HEIGHT = 720;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BowlingSimulator app = new BowlingSimulator();
            app.setVisible(true);
        });
    }

    private final GameController controller;
    private final LanePanel lanePanel;
    private final HUDPanel hudPanel;
    private final ControlPanel controlPanel;

    public BowlingSimulator() {
        super("Bowling Simulator - Single File Java");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        controller = new GameController();
        lanePanel = new LanePanel(controller);
        hudPanel = new HUDPanel(controller);
        controlPanel = new ControlPanel(controller);

        add(lanePanel, BorderLayout.CENTER);
        add(hudPanel, BorderLayout.EAST);
        add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------
    // Model & Controller
    // ------------------------------------------------------------

    /**
     * Central controller managing game state, input, and animation ticks.
     */
    static class GameController {

        // Game state
        private final GameState gameState;

        // Animation timer (60 FPS target)
        private final Timer timer;

        // Observers
        private final List<Runnable> onTickObservers = new ArrayList<>();

        // Input state
        private boolean chargingPower = false;
        private long powerStartTime = 0;
        private double currentPower = 0.0; // 0..1

        // Aim and spin control
        private double aimOffset = 0.0; // -1..1 mapped later to lane width offset
        private double spin = 0.0;      // -1..1 left/right curve

        // Pause
        private boolean paused = false;

        public GameController() {
            gameState = new GameState();
            // 60 fps ~16 ms
            timer = new Timer(16, e -> tick());
            timer.start();
        }

        public GameState getGameState() {
            return gameState;
        }

        public void addTickObserver(Runnable r) {
            onTickObservers.add(r);
        }

        public boolean isPaused() {
            return paused;
        }

        public void setPaused(boolean v) {
            paused = v;
        }

        public void resetGame() {
            gameState.reset();
            aimOffset = 0.0;
            spin = 0.0;
            chargingPower = false;
            currentPower = 0.0;
            paused = false;
        }

        public void leftAim() {
            aimOffset -= 0.05;
            aimOffset = clamp(aimOffset, -1.0, 1.0);
        }

        public void rightAim() {
            aimOffset += 0.05;
            aimOffset = clamp(aimOffset, -1.0, 1.0);
        }

        public void increaseSpin() {
            spin += 0.05;
            spin = clamp(spin, -1.0, 1.0);
        }

        public void decreaseSpin() {
            spin -= 0.05;
            spin = clamp(spin, -1.0, 1.0);
        }

        public double getSpin() {
            return spin;
        }

        public double getAim() {
            return aimOffset;
        }

        public double getCurrentPower() {
            return currentPower;
        }

        public boolean isChargingPower() {
            return chargingPower;
        }

        public void onSpacePressed() {
            if (paused) return;
            if (gameState.isBallRolling() || gameState.isFrameComplete()) return;
            chargingPower = true;
            powerStartTime = System.nanoTime();
        }

        public void onSpaceReleased() {
            if (paused) return;
            if (!chargingPower) return;
            chargingPower = false;

            // Spawn ball with configured aim and spin
            double p = currentPower;
            currentPower = 0.0;
            rollBall(p, aimOffset, spin);
        }

        private void rollBall(double power, double aim, double spin) {
            if (gameState.isBallRolling() || gameState.isFrameComplete()) return;
            // Map aim to lane coordinates
            LaneGeometry g = gameState.getLaneGeometry();
            double startX = g.laneX + g.laneWidth / 2.0 + aim * (g.laneWidth * 0.35);
            double startY = g.laneY + g.laneHeight - g.ballRadius - 10;

            Ball ball = new Ball(startX, startY, g.ballRadius);
            // Power maps to speed
            double speedY = - (g.baseBallSpeed * (0.4 + 0.6 * power)); // Upwards
            double lateral = (aim * 0.35) * g.laneWidth;

            ball.vx = lateral * 0.0; // initial vx negligible; spin causes curve
            ball.vy = speedY;
            ball.spin = spin;
            ball.power = power;

            gameState.spawnBall(ball);
        }

        private void tick() {
            if (!paused) {
                // Update charging power meter
                if (chargingPower) {
                    long now = System.nanoTime();
                    double elapsedSec = (now - powerStartTime) / 1_000_000_000.0;
                    // Oscillating meter 0..1 using a smooth triangular wave
                    double period = 1.4; // seconds
                    double t = (elapsedSec % period) / period;
                    double tri = t < 0.5 ? (t * 2.0) : (2.0 - t * 2.0);
                    currentPower = tri;
                }

                // Update physics and game logic
                gameState.update();
            }

            for (Runnable r : onTickObservers) r.run();
        }

        private static double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }
    }

    /**
     * Complete game state container, scoring, pins, ball, frames, lane geometry.
     */
    static class GameState {

        private final LaneGeometry geom;

        // Pins
        private List<Pin> pins = new ArrayList<>();

        // Ball
        private Ball ball = null;

        // Scoring
        private final ScoreBoard scoreBoard;

        // Frame tracking
        private int currentFrameIndex = 0;
        private int rollInFrame = 0; // 0-based: 0 or 1, special rules for 10th frame

        // Animation and effects
        private boolean collisionHappenedThisTick = false;

        // Game flags
        private boolean frameComplete = false;

        // Randomness
        private final Random random = new Random(42);

        public GameState() {
            geom = new LaneGeometry();
            scoreBoard = new ScoreBoard();
            resetFramePins();
        }

        public void reset() {
            pins.clear();
            ball = null;
            scoreBoard.reset();
            currentFrameIndex = 0;
            rollInFrame = 0;
            frameComplete = false;
            resetFramePins();
        }

        public LaneGeometry getLaneGeometry() {
            return geom;
        }

        public List<Pin> getPins() {
            return pins;
        }

        public Ball getBall() {
            return ball;
        }

        public ScoreBoard getScoreBoard() {
            return scoreBoard;
        }

        public int getCurrentFrameIndex() {
            return currentFrameIndex;
        }

        public int getRollInFrame() {
            return rollInFrame;
        }

        public boolean isFrameComplete() {
            return frameComplete;
        }

        public boolean isBallRolling() {
            return ball != null && !ball.stopped;
        }

        public void spawnBall(Ball b) {
            this.ball = b;
        }

        public void update() {
            collisionHappenedThisTick = false;

            // Update ball physics and pins states
            updateBall();
            updatePins();

            // If ball stopped and collisions resolved, determine pins down and scoring
            if (ball != null && ball.stopped) {
                boolean anyMoving = pins.stream().anyMatch(p -> p.isFalling() || p.isWobbling());
                if (!anyMoving && !collisionHappenedThisTick) {
                    // Roll ended
                    onRollEnded();
                }
            }
        }

        private void onRollEnded() {
            int knocked = (int) pins.stream().filter(p -> p.down).count();
            int standingBefore = scoreBoard.standingPinsBeforeRoll;
            int knockedThisRoll = knocked - standingBefore;
            if (knockedThisRoll < 0) knockedThisRoll = 0;

            // Update score
            scoreBoard.registerRoll(currentFrameIndex, rollInFrame, knockedThisRoll);

            // If all pins down, reset if not final frame or not done with 10th frame logic
            boolean allDown = knocked == 10;
            if (isTenthFrame()) {
                // 10th frame logic
                Frame tenth = scoreBoard.frames.get(9);
                // After each roll in 10th:
                // - If strike or spare, allow a bonus ball up to 3 rolls total.
                boolean allowBonus = tenth.isStrike() || tenth.isSpare();
                if (rollInFrame == 0) {
                    // First roll done
                    rollInFrame++;
                    prepareNextRollTenthFrame(tenth, allDown);
                } else if (rollInFrame == 1) {
                    // Second roll done
                    rollInFrame++;
                    prepareNextRollTenthFrame(tenth, allDown);
                } else {
                    // Third roll done or no more allowed
                    frameComplete = true;
                    ball = null;
                }

                // If frame complete, game may end
                if (frameComplete) {
                    // done
                    ball = null;
                }
            } else {
                // Frames 1..9
                if (rollInFrame == 0) {
                    // First roll done
                    rollInFrame++;
                    if (allDown) {
                        // strike ends frame immediately
                        frameComplete = true;
                    } else {
                        // prepare for second roll with remaining pins
                        scoreBoard.standingPinsBeforeRoll = (int) pins.stream().filter(p -> !p.down).count();
                    }
                } else {
                    // Second roll done
                    frameComplete = true;
                }
            }

            // Frame transition
            if (frameComplete) {
                currentFrameIndex++;
                rollInFrame = 0;
                frameComplete = false;
                scoreBoard.standingPinsBeforeRoll = 0;
                resetFramePins();
                ball = null;

                // If we passed frame 9, game over
                if (currentFrameIndex >= 10) {
                    // Keep last pin layout as all down for visual end
                    currentFrameIndex = 9;
                    frameComplete = true;
                }
            }
        }

        private boolean isTenthFrame() {
            return currentFrameIndex == 9;
        }

        private void prepareNextRollTenthFrame(Frame tenth, boolean allDown) {
            // For the 10th frame, pins may reset depending on strikes/spares
            if (tenth.rolls.size() == 1 && tenth.isStrike()) {
                // First was a strike: reset pins for next roll
                resetFramePins();
                scoreBoard.standingPinsBeforeRoll = 0;
            } else if (tenth.rolls.size() == 2) {
                if (tenth.isStrike() || tenth.isSpare()) {
                    // Reset for bonus ball
                    resetFramePins();
                    scoreBoard.standingPinsBeforeRoll = 0;
                } else {
                    // No bonus ball, frame complete
                    frameComplete = true;
                }
            } else if (tenth.rolls.size() == 3) {
                // No more rolls
                frameComplete = true;
            } else {
                // Otherwise, if not all down, keep remaining pins
                scoreBoard.standingPinsBeforeRoll = (int) pins.stream().filter(p -> !p.down).count();
            }
        }

        private void updateBall() {
            if (ball == null) return;
            if (ball.stopped) return;

            // Move ball forward
            ball.age += 0.016;
            ball.vy += 0.0; // no vertical acceleration (glide)
            // Apply spin as lateral curvature (small vx change over time)
            double curveStrength = geom.spinCurveStrength;
            ball.vx += ball.spin * curveStrength;

            ball.x += ball.vx;
            ball.y += ball.vy;

            // Friction: gradually reduce vy
            ball.vy *= geom.frictionY;
            ball.vx *= geom.frictionX;

            // Lane boundaries
            double left = geom.laneX + geom.gutterWidth + ball.radius;
            double right = geom.laneX + geom.laneWidth - geom.gutterWidth - ball.radius;

            if (ball.x < left) {
                ball.x = left;
                ball.vx = Math.abs(ball.vx) * 0.5; // bounce inside slight
            }
            if (ball.x > right) {
                ball.x = right;
                ball.vx = -Math.abs(ball.vx) * 0.5;
            }

            // Pin collision region
            double pinRegionTop = geom.pinTriangleY - geom.pinRegionPadding;
            if (ball.y < pinRegionTop) {
                // Check collisions with pins
                collisionHappenedThisTick |= collideBallWithPins(ball, pins, geom);
            }

            // Stop condition
            double laneTop = geom.laneY + 20;
            if (ball.y < laneTop || Math.abs(ball.vy) < 15) {
                ball.stopped = true;
            }
        }

        private boolean collideBallWithPins(Ball ball, List<Pin> pins, LaneGeometry g) {
            boolean collided = false;
            for (Pin p : pins) {
                if (p.down) continue;
                double dx = ball.x - p.x;
                double dy = ball.y - p.y;
                double dist = Math.hypot(dx, dy);
                if (dist < (ball.radius + p.radius)) {
                    // Simple collision response
                    collided = true;

                    // Push pin: mark as wobble or fall based on impact speed/power.
                    double impact = Math.hypot(ball.vx, ball.vy) / g.baseBallSpeed;
                    double fallChance = impact * 0.9 + ball.power * 0.4 + random.nextDouble() * 0.2;
                    if (fallChance > 0.35) {
                        p.startFalling(random);
                    } else {
                        p.startWobble(random);
                    }

                    // Nudge ball
                    double nx = dx / (dist + 1e-6);
                    double ny = dy / (dist + 1e-6);
                    double push = 40 * impact;
                    ball.vx += nx * push * 0.2;
                    ball.vy += ny * push * 0.05;
                }
            }
            return collided;
        }

        private void updatePins() {
            for (Pin p : pins) {
                p.update();
            }
        }

        private void resetFramePins() {
            pins.clear();
            // Standard 10-pin arrangement
            // Coordinates relative to lane geometry pin triangle
            double spacing = geom.pinSpacing;
            double baseX = geom.pinTriangleX;
            double baseY = geom.pinTriangleY;

            // Row 1: 1 pin
            pins.add(new Pin(baseX, baseY));

            // Row 2: 2 pins
            pins.add(new Pin(baseX - spacing / 2.0, baseY + spacing));
            pins.add(new Pin(baseX + spacing / 2.0, baseY + spacing));

            // Row 3: 3 pins
            pins.add(new Pin(baseX - spacing, baseY + spacing * 2));
            pins.add(new Pin(baseX, baseY + spacing * 2));
            pins.add(new Pin(baseX + spacing, baseY + spacing * 2));

            // Row 4: 4 pins
            pins.add(new Pin(baseX - spacing * 1.5, baseY + spacing * 3));
            pins.add(new Pin(baseX - spacing * 0.5, baseY + spacing * 3));
            pins.add(new Pin(baseX + spacing * 0.5, baseY + spacing * 3));
            pins.add(new Pin(baseX + spacing * 1.5, baseY + spacing * 3));

            scoreBoard.standingPinsBeforeRoll = 0;
        }
    }

    // ------------------------------------------------------------
    // Geometry & Entities
    // ------------------------------------------------------------

    /**
     * Lane dimensions and constants.
     */
    static class LaneGeometry {

        // Lane rectangle
        public final double laneX = 60;
        public final double laneY = 60;
        public final double laneWidth = 640;
        public final double laneHeight = 560;
        public final double gutterWidth = 24;

        // Ball radius
        public final double ballRadius = 16;

        // Base ball speed
        public final double baseBallSpeed = 600.0;

        // Spin curvature
        public final double spinCurveStrength = 0.35;

        // Friction
        public final double frictionY = 0.995;
        public final double frictionX = 0.987;

        // Pins
        public final double pinRadius = 10;

        // Pin triangle apex
        public final double pinTriangleX = laneX + laneWidth / 2.0;
        public final double pinTriangleY = laneY + 130;

        public final double pinSpacing = 36;

        public final double pinRegionPadding = 30;

        // Text and UI
        public final Font scoreFont = new Font("SansSerif", Font.BOLD, 14);
        public final Font hudFont = new Font("SansSerif", Font.PLAIN, 13);

        // Colors
        public final Color laneColor = new Color(193, 154, 107);
        public final Color laneWoodDark = new Color(161, 129, 92);
        public final Color gutterColor = new Color(60, 60, 60);
        public final Color background = new Color(25, 25, 28);

        public final Color pinColor = new Color(240, 240, 240);
        public final Color pinStripe = new Color(200, 30, 30);
        public final Color ballColor = new Color(30, 70, 180);
        public final Color ballStripe = new Color(255, 220, 0);

        public final Color textPrimary = Color.WHITE;
        public final Color textSecondary = new Color(230, 230, 230);
        public final Color accent = new Color(120, 220, 150);
        public final Color warning = new Color(240, 90, 90);
    }

    /**
     * Ball entity.
     */
    static class Ball {
        double x;
        double y;
        double vx;
        double vy;
        double radius;
        double spin;   // -1..1 left/right
        double power;  // 0..1
        double age = 0;
        boolean stopped = false;

        public Ball(double x, double y, double r) {
            this.x = x;
            this.y = y;
            this.radius = r;
        }
    }

    /**
     * Pin entity with wobble and fall animation.
     */
    static class Pin {
        double x;
        double y;
        double radius;
        boolean down = false;

        // Animation state
        boolean wobble = false;
        boolean falling = false;
        double wobblePhase = 0.0;
        double wobbleSpeed = 0.0;
        double tilt = 0.0; // 0..1 for falling
        double fallSpeed = 0.0;

        public Pin(double x, double y) {
            this.x = x;
            this.y = y;
            this.radius = 10;
        }

        public boolean isWobbling() {
            return wobble && !down && !falling;
        }

        public boolean isFalling() {
            return falling && !down;
        }

        public void startWobble(Random rnd) {
            wobble = true;
            wobblePhase = rnd.nextDouble() * Math.PI * 2.0;
            wobbleSpeed = 0.12 + rnd.nextDouble() * 0.15;
        }

        public void startFalling(Random rnd) {
            falling = true;
            wobble = false;
            fallSpeed = 0.03 + rnd.nextDouble() * 0.06;
        }

        public void update() {
            if (down) return;
            if (wobble) {
                wobblePhase += wobbleSpeed;
                // Occasionally transition to falling
                double chance = Math.abs(Math.sin(wobblePhase)) * 0.02;
                if (Math.random() < chance) {
                    falling = true;
                    wobble = false;
                    fallSpeed = 0.05 + Math.random() * 0.05;
                }
            }
            if (falling) {
                tilt += fallSpeed;
                if (tilt >= 1.0) {
                    down = true;
                    falling = false;
                    tilt = 1.0;
                }
            }
        }
    }

    // ------------------------------------------------------------
    // Scoring
    // ------------------------------------------------------------

    /**
     * Frame represents a scoring frame.
     */
    static class Frame {
        final List<Integer> rolls = new ArrayList<>(); // pins knocked this roll
        int score = 0;
        boolean scoredFinal = false;

        boolean isStrike() {
            return rolls.size() >= 1 && rolls.get(0) == 10;
        }

        boolean isSpare() {
            return rolls.size() >= 2 && rolls.get(0) + rolls.get(1) == 10 && !isStrike();
        }

        boolean isCompleteForFrames1to9() {
            if (isStrike()) return true;
            return rolls.size() == 2;
        }

        boolean isCompleteForFrame10() {
            if (rolls.size() < 2) return false;
            if (isStrike() || isSpare()) {
                return rolls.size() == 3;
            } else {
                return rolls.size() == 2;
            }
        }
    }

    /**
     * ScoreBoard manages frames and computes running totals with strike/spare bonuses.
     */
    static class ScoreBoard {
        final List<Frame> frames = new ArrayList<>();
        int standingPinsBeforeRoll = 0;

        public ScoreBoard() {
            reset();
        }

        public void reset() {
            frames.clear();
            for (int i = 0; i < 10; i++) {
                frames.add(new Frame());
            }
            standingPinsBeforeRoll = 0;
        }

        public void registerRoll(int frameIndex, int rollIndex, int knocked) {
            Frame f = frames.get(frameIndex);

            // For frames 1..9, ensure we don't exceed two rolls except strike
            if (frameIndex < 9) {
                if (rollIndex == 0) {
                    f.rolls.add(knocked);
                } else {
                    f.rolls.add(knocked);
                }
            } else {
                // 10th frame
                f.rolls.add(knocked);
            }

            // Recompute scores after each roll
            computeScores();
        }

        private void computeScores() {
            // Rolling computation across frames
            int[] allRolls = flattenRolls();
            int[] rollIdxPerFrame = new int[10]; // starting index into allRolls for each frame
            int pos = 0;
            for (int i = 0; i < 10; i++) {
                rollIdxPerFrame[i] = pos;
                Frame f = frames.get(i);
                if (i < 9) {
                    if (f.isStrike()) {
                        pos += 1;
                    } else if (f.rolls.size() >= 2) {
                        pos += 2;
                    } else {
                        pos += f.rolls.size();
                    }
                } else {
                    pos += f.rolls.size();
                }
            }

            // Compute each frame score
            int running = 0;
            for (int i = 0; i < 10; i++) {
                Frame f = frames.get(i);
                int base = 0;
                for (int r : f.rolls) base += r;

                int bonus = 0;
                if (i < 9) {
                    if (f.isStrike()) {
                        // strike: 10 + next two rolls
                        int idx = rollIdxPerFrame[i] + 1;
                        bonus += nextRoll(allRolls, idx, 0);
                        bonus += nextRoll(allRolls, idx, 1);
                        f.score = 10 + bonus;
                    } else if (f.isSpare()) {
                        // spare: 10 + next one roll
                        int idx = rollIdxPerFrame[i] + 2;
                        bonus += nextRoll(allRolls, idx, 0);
                        f.score = 10 + bonus;
                    } else {
                        f.score = base;
                    }
                } else {
                    // 10th frame: sum of its rolls (no external bonuses)
                    f.score = base;
                }

                running += f.score;
            }
        }

        private int[] flattenRolls() {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                Frame f = frames.get(i);
                if (f.isStrike()) {
                    list.add(10);
                } else {
                    list.add(f.rolls.size() > 0 ? f.rolls.get(0) : 0);
                    list.add(f.rolls.size() > 1 ? f.rolls.get(1) : 0);
                }
            }
            // 10th frame fully appended
            Frame tenth = frames.get(9);
            for (int r : tenth.rolls) list.add(r);
            return list.stream().mapToInt(Integer::intValue).toArray();
        }

        private int nextRoll(int[] arr, int startIdx, int offset) {
            int idx = startIdx + offset;
            if (idx >= 0 && idx < arr.length) return arr[idx];
            return 0;
        }

        public int totalScore() {
            int total = 0;
            for (Frame f : frames) total += f.score;
            return total;
        }
    }

    // ------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------

    /**
     * Main panel that draws the lane, ball, pins, and overlays.
     */
    static class LanePanel extends JPanel implements KeyListener {

        private final GameController controller;
        private final GameState state;
        private BufferedImage backBuffer;

        public LanePanel(GameController controller) {
            this.controller = controller;
            this.state = controller.getGameState();
            setBackground(Color.BLACK);
            setFocusable(true);
            requestFocusInWindow();

            controller.addTickObserver(() -> {
                repaint();
            });

            addKeyListener(this);

            // For smoother rendering
            setDoubleBuffered(true);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(760, WINDOW_HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            LaneGeometry geom = state.getLaneGeometry();

            // Draw background
            g2.setColor(geom.background);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Draw lane wood body
            drawLane(g2, geom);

            // Draw gutters
            drawGutters(g2, geom);

            // Draw aiming guide
            drawAimGuide(g2, geom);

            // Draw pins
            drawPins(g2, geom, state.getPins());

            // Draw ball
            Ball ball = state.getBall();
            if (ball != null) drawBall(g2, geom, ball);

            // Draw overlay info
            drawOverlayHUD(g2, geom);

            g2.dispose();
        }

        private void drawLane(Graphics2D g2, LaneGeometry geom) {
            Shape laneRect = new RoundRectangle2D.Double(
                    geom.laneX, geom.laneY, geom.laneWidth, geom.laneHeight, 12, 12
            );

            // Wood gradient
            GradientPaint woodGrad = new GradientPaint(
                    (float) geom.laneX, (float) (geom.laneY + geom.laneHeight),
                    geom.laneWoodDark,
                    (float) geom.laneX, (float) geom.laneY,
                    geom.laneColor
            );
            g2.setPaint(woodGrad);
            g2.fill(laneRect);

            // Foul line
            g2.setColor(new Color(255, 255, 255, 120));
            g2.fillRect((int) geom.laneX, (int) (geom.laneY + geom.laneHeight - 90), (int) geom.laneWidth, 3);

            // Arrows markers
            g2.setColor(new Color(255, 255, 255, 140));
            for (int i = -2; i <= 2; i++) {
                int ax = (int) (geom.laneX + geom.laneWidth / 2.0 + i * 60);
                int ay = (int) (geom.laneY + geom.laneHeight - 140);
                Polygon arrow = new Polygon();
                arrow.addPoint(ax, ay);
                arrow.addPoint(ax - 6, ay + 16);
                arrow.addPoint(ax + 6, ay + 16);
                g2.fill(arrow);
            }
        }

        private void drawGutters(Graphics2D g2, LaneGeometry geom) {
            // Left gutter
            g2.setColor(geom.gutterColor);
            g2.fillRect((int) (geom.laneX), (int) geom.laneY, (int) geom.gutterWidth, (int) geom.laneHeight);

            // Right gutter
            int gx = (int) (geom.laneX + geom.laneWidth - geom.gutterWidth);
            g2.fillRect(gx, (int) geom.laneY, (int) geom.gutterWidth, (int) geom.laneHeight);
        }

        private void drawAimGuide(Graphics2D g2, LaneGeometry geom) {
            double aim = controller.getAim();
            double x = geom.laneX + geom.laneWidth / 2.0 + aim * (geom.laneWidth * 0.35);
            double y1 = geom.laneY + geom.laneHeight - 60;
            double y2 = geom.laneY + 120;
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6, 8}, 0));
            g2.setColor(new Color(255, 255, 255, 80));
            g2.draw(new Line2D.Double(x, y1, x, y2));
            g2.setStroke(old);
        }

        private void drawPins(Graphics2D g2, LaneGeometry geom, List<Pin> pins) {
            for (Pin p : pins) {
                drawPin(g2, geom, p);
            }
        }

        private void drawPin(Graphics2D g2, LaneGeometry geom, Pin pin) {
            if (pin.down) {
                // Draw fallen pin (flat ellipse)
                g2.setColor(new Color(220, 220, 220));
                double w = pin.radius * 2;
                double h = pin.radius * 0.9;
                g2.fill(new Ellipse2D.Double(pin.x - w / 2.0, pin.y - h / 2.0, w, h));
                g2.setColor(geom.pinStripe);
                g2.fill(new Rectangle2D.Double(pin.x - w / 3.0, pin.y - h / 6.0, w / 1.5, 2));
                return;
            }

            double tilt = pin.tilt;
            boolean wobble = pin.isWobbling();

            double scaleY = 1.0 - tilt * 0.6;
            double scaleX = 1.0 + tilt * 0.2;
            if (wobble) {
                double w = Math.sin(pin.wobblePhase) * 0.08;
                scaleX += w;
                scaleY -= Math.abs(w) * 0.3;
            }
            double r = pin.radius;
            double px = pin.x;
            double py = pin.y;

            AffineTransform at = new AffineTransform();
            at.translate(px, py);
            at.scale(scaleX, scaleY);
            at.translate(-r, -r);

            Shape body = new Ellipse2D.Double(0, 0, r * 2, r * 2);
            Shape stripe = new Rectangle2D.Double(r * 0.35, r * 0.85, r * 1.3, r * 0.25);

            g2.setColor(new Color(250, 250, 250));
            g2.fill(at.createTransformedShape(body));
            g2.setColor(new Color(200, 30, 30));
            g2.fill(at.createTransformedShape(stripe));

            // Outline
            g2.setColor(new Color(0, 0, 0, 50));
            g2.draw(at.createTransformedShape(body));
        }

        private void drawBall(Graphics2D g2, LaneGeometry geom, Ball ball) {
            double r = ball.radius;
            Shape s = new Ellipse2D.Double(ball.x - r, ball.y - r, r * 2, r * 2);
            // Ball gradient
            RadialGradientPaint rgp = new RadialGradientPaint(
                    new Point2D.Double(ball.x - r * 0.3, ball.y - r * 0.3),
                    (float) (r * 1.6),
                    new float[]{0f, 1f},
                    new Color[]{geom.ballColor.brighter(), geom.ballColor.darker()}
            );
            g2.setPaint(rgp);
            g2.fill(s);

            // Stripe
            g2.setColor(geom.ballStripe);
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new Arc2D.Double(ball.x - r, ball.y - r, r * 2, r * 2, 40, 280, Arc2D.OPEN));
        }

        private void drawOverlayHUD(Graphics2D g2, LaneGeometry geom) {
            g2.setFont(geom.hudFont);
            g2.setColor(geom.textPrimary);

            // Frame and roll info
            String frameInfo = "Frame: " + (state.getCurrentFrameIndex() + 1);
            String rollInfo = "Roll: " + (state.getRollInFrame() + 1);
            g2.drawString(frameInfo, (int) (geom.laneX + 10), (int) (geom.laneY + geom.laneHeight + 20));
            g2.drawString(rollInfo, (int) (geom.laneX + 110), (int) (geom.laneY + geom.laneHeight + 20));

            // Power meter
            drawPowerMeter(g2, geom);

            // Spin indicator
            drawSpinIndicator(g2, geom);

            // Aim indicator numeric
            drawAimIndicatorValue(g2, geom);

            // Pause overlay
            if (controller.isPaused()) {
                drawPauseOverlay(g2);
            }

            // Instructions
            g2.setColor(new Color(220, 220, 220));
            g2.drawString("Controls: Left/Right to aim | Up/Down to spin | Hold Space to charge, release to roll", (int) geom.laneX, (int) (geom.laneY - 18));
        }

        private void drawPowerMeter(Graphics2D g2, LaneGeometry geom) {
            int x = (int) (geom.laneX + 10);
            int y = (int) (geom.laneY + geom.laneHeight + 40);
            int w = 260;
            int h = 12;

            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRoundRect(x, y, w, h, 10, 10);

            double p = controller.getCurrentPower();
            int pw = (int) (w * p);
            Color c = controller.isChargingPower() ? geom.accent : new Color(180, 180, 180);
            g2.setColor(c);
            g2.fillRoundRect(x, y, pw, h, 10, 10);

            g2.setColor(Color.WHITE);
            g2.drawString(String.format("Power: %.0f%%", p * 100), x + w + 10, y + h);
        }

        private void drawSpinIndicator(Graphics2D g2, LaneGeometry geom) {
            int x = (int) (geom.laneX + 10);
            int y = (int) (geom.laneY + geom.laneHeight + 64);
            int w = 160;
            int h = 12;

            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRoundRect(x, y, w, h, 10, 10);

            double s = controller.getSpin(); // -1..1
            int center = x + w / 2;
            int px = center + (int) (s * (w / 2 - 4));

            g2.setColor(new Color(140, 180, 255));
            g2.fillRoundRect(px - 6, y, 12, h, 8, 8);

            g2.setColor(Color.WHITE);
            g2.drawString(String.format("Spin: %s %.0f%%", s >= 0 ? "Right" : "Left", Math.abs(s) * 100), x + w + 10, y + h);
        }

        private void drawAimIndicatorValue(Graphics2D g2, LaneGeometry geom) {
            int x = (int) (geom.laneX + 10);
            int y = (int) (geom.laneY + geom.laneHeight + 88);
            double a = controller.getAim();

            g2.setColor(Color.WHITE);
            g2.drawString(String.format("Aim: %.0f%% %s", Math.abs(a) * 100, a >= 0 ? "Right" : "Left"), x, y);
        }

        private void drawPauseOverlay(Graphics2D g2) {
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.SrcOver.derive(0.25f));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setComposite(old);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            String text = "Paused";
            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(text)) / 2;
            int ty = getHeight() / 2;
            g2.drawString(text, tx, ty);
        }

        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) controller.leftAim();
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) controller.rightAim();
            if (e.getKeyCode() == KeyEvent.VK_UP) controller.increaseSpin();
            if (e.getKeyCode() == KeyEvent.VK_DOWN) controller.decreaseSpin();
            if (e.getKeyCode() == KeyEvent.VK_SPACE) controller.onSpacePressed();
            if (e.getKeyCode() == KeyEvent.VK_P) controller.setPaused(!controller.isPaused());
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) controller.onSpaceReleased();
        }
    }

    /**
     * Side panel showing scoreboard and frame details.
     */
    static class HUDPanel extends JPanel {

        private final GameController controller;
        private final GameState state;

        public HUDPanel(GameController controller) {
            this.controller = controller;
            this.state = controller.getGameState();
            setPreferredSize(new Dimension(WINDOW_WIDTH - 760, WINDOW_HEIGHT));
            setBackground(new Color(18, 18, 22));

            controller.addTickObserver(() -> repaint());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            LaneGeometry geom = state.getLaneGeometry();

            int padding = 16;
            int x = padding;
            int y = padding;

            // Title
            g2.setColor(new Color(255, 255, 255));
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            g2.drawString("Scoreboard", x, y + 20);

            y += 40;

            // Frames table
            drawFramesTable(g2, x, y, 220, 360, state.getScoreBoard());

            y += 380;

            // Total score
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.setColor(new Color(180, 220, 160));
            g2.drawString("Total: " + state.getScoreBoard().totalScore(), x, y + 20);

            y += 40;

            // Current Frame Info
            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g2.setColor(Color.WHITE);
            g2.drawString("Frame " + (state.getCurrentFrameIndex() + 1) + " of 10", x, y + 16);
            g2.drawString("Roll " + (state.getRollInFrame() + 1), x, y + 36);

            g2.dispose();
        }

        private void drawFramesTable(Graphics2D g2, int x, int y, int w, int h, ScoreBoard sb) {
            int rows = 10;
            int rowH = h / rows;

            for (int i = 0; i < rows; i++) {
                int ry = y + i * rowH;
                // Background row
                g2.setColor(new Color(30, 30, 36));
                g2.fillRoundRect(x, ry, w, rowH - 6, 8, 8);

                // Labels
                g2.setColor(new Color(220, 220, 220));
                g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                g2.drawString("Frame " + (i + 1), x + 8, ry + 18);

                Frame f = sb.frames.get(i);
                // Rolls
                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                String r1 = f.rolls.size() > 0 ? String.valueOf(f.rolls.get(0)) : "-";
                String r2 = f.rolls.size() > 1 ? String.valueOf(f.rolls.get(1)) : "-";
                String r3 = f.rolls.size() > 2 ? String.valueOf(f.rolls.get(2)) : "-";
                g2.drawString("Rolls: " + r1 + " | " + r2 + " | " + r3, x + 8, ry + 36);

                // Strike/Spare tag
                String tag = "";
                if (i < 9) {
                    if (f.isStrike()) tag = "Strike";
                    else if (f.isSpare()) tag = "Spare";
                } else {
                    if (f.isStrike()) tag = "10th: Strike";
                    else if (f.isSpare()) tag = "10th: Spare";
                }

                if (!tag.isEmpty()) {
                    g2.setColor(new Color(120, 200, 255));
                    g2.drawString(tag, x + 8, ry + 54);
                }

                // Score
                g2.setColor(new Color(200, 240, 200));
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2.drawString("Score: " + f.score, x + w - 100, ry + 24);
            }
        }
    }

    /**
     * Bottom panel with controls (buttons) for new game and pause.
     */
    static class ControlPanel extends JPanel {

        private final GameController controller;

        public ControlPanel(GameController controller) {
            this.controller = controller;
            setLayout(new FlowLayout(FlowLayout.LEFT));
            setBackground(new Color(20, 20, 24));

            JButton newGame = new JButton("New game");
            JButton pause = new JButton("Pause/Resume");

            newGame.addActionListener(e -> controller.resetGame());
            pause.addActionListener(e -> controller.setPaused(!controller.isPaused()));

            add(newGame);
            add(pause);
            add(new JLabel("Tip: Press P to pause/resume."));

            setPreferredSize(new Dimension(WINDOW_WIDTH, 50));
        }
    }
}

