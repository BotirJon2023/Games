import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BowlingSimulator {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Bowling Simulator - Single File");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(true);
            GamePanel panel = new GamePanel();
            f.setContentPane(panel);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            panel.requestFocusInWindow();
        });
    }


    static class GamePanel extends JPanel implements KeyListener, MouseMotionListener, MouseListener, ComponentListener {
        // Timing
        private final int targetFps = 60;
        private final double dt = 1.0 / targetFps;
        private Timer timer;

        // Core systems
        private Lane lane;
        private PhysicsEngine physics;
        private ScoreManager scoreManager;
        private InputState input;

        // Entities
        private Ball ball;
        private final List<Pin> pins = new ArrayList<>();

        // Game state
        private GamePhase phase = GamePhase.AIMING;
        private int frameIndex = 0; // 0..9
        private int rollInFrame = 0; // 0,1 (or 2 for 10th)
        private boolean gameOver = false;

        // Aiming/throw parameters
        private double aimPosX;   // horizontal position on foul line
        private double aimAngle;  // direction angle in radians (negative is up)
        private double spin;      // -1 (left hook) .. +1 (right hook)
        private double power;     // 0..1 (charged by holding space)
        private boolean charging = false;
        private double powerOscPhase = 0;

        // UI style
        private final Color bgColor = new Color(10, 15, 20);
        private final Color laneWood = new Color(180, 140, 90);
        private final Color laneEdge = new Color(60, 40, 20);
        private final Color gutterColor = new Color(30, 30, 30);
        private final Color pinColor = new Color(240, 240, 240);
        private final Color pinStripe = new Color(220, 40, 40);
        private final Color ballColor = new Color(30, 80, 200);
        private final Color uiText = new Color(240, 240, 245);
        private final Font uiFont = new Font("SansSerif", Font.PLAIN, 12);
        private final Font titleFont = new Font("SansSerif", Font.BOLD, 18);

        // Visual effects
        private double cameraShake = 0;
        private double settleTimer = 0;  // time while pins settle after roll
        private double messageTimer = 0;
        private String floatingMessage = "";

        // Random for tiny variations
        private final Random rng = new Random();

        // Layout
        private int panelW = 1100;
        private int panelH = 720;

        // Debug flags
        private boolean paused = false;
        private boolean debug = false;

        public GamePanel() {
            setPreferredSize(new Dimension(panelW, panelH));
            setBackground(bgColor);
            setFocusable(true);
            addKeyListener(this);
            addMouseMotionListener(this);
            addMouseListener(this);
            addComponentListener(this);

            // Setup systems
            lane = new Lane();
            physics = new PhysicsEngine();
            scoreManager = new ScoreManager();
            input = new InputState();

            initGame();

            timer = new Timer(1000 / targetFps, e -> {
                if (!paused) update();
                repaint();
            });
            timer.start();
        }

        private void initGame() {
            gameOver = false;
            frameIndex = 0;
            rollInFrame = 0;
            scoreManager.newGame();

            // Rebuild lane geometry based on panel size
            lane.build(panelW, panelH);

            // Ball at foul line center by default
            aimPosX = lane.getCenterX();
            aimAngle = -Math.toRadians(90 - 10); // default slight angle toward pins
            spin = 0;
            power = 0;
            charging = false;
            powerOscPhase = 0;

            // Entities
            ball = new Ball(lane.startX, lane.startY, 14);
            resetPins();

            phase = GamePhase.AIMING;
            floatingMessage = "Frame 1 - Aim and throw!";
            messageTimer = 2.2;
        }

        private void resetPins() {
            pins.clear();
            // Arrange 10 pins in triangle at pin deck
            double r = 10; // visual radius
            double gap = 2.0; // small spacing beyond diameter
            double d = 2 * r + gap;

            // Head pin position
            double px = lane.getCenterX();
            double py = lane.pinDeckY + r + 6;

            // Rows
            // Row 1
            pins.add(new Pin(px, py, r));
            // Row 2
            pins.add(new Pin(px - d / 2, py + d, r));
            pins.add(new Pin(px + d / 2, py + d, r));
            // Row 3
            pins.add(new Pin(px - d, py + 2 * d, r));
            pins.add(new Pin(px, py + 2 * d, r));
            pins.add(new Pin(px + d, py + 2 * d, r));
            // Row 4
            pins.add(new Pin(px - 1.5 * d, py + 3 * d, r));
            pins.add(new Pin(px - 0.5 * d, py + 3 * d, r));
            pins.add(new Pin(px + 0.5 * d, py + 3 * d, r));
            pins.add(new Pin(px + 1.5 * d, py + 3 * d, r));
        }

        private void update() {
            double dtLocal = dt;

            // Camera shake decay
            cameraShake *= 0.94;

            // Floating message timer
            if (messageTimer > 0) {
                messageTimer -= dtLocal;
                if (messageTimer < 0) messageTimer = 0;
            }

            // Handle phases
            if (phase == GamePhase.AIMING) {
                updateAiming(dtLocal);
            } else if (phase == GamePhase.CHARGING) {
                updateCharging(dtLocal);
            } else if (phase == GamePhase.ROLLING) {
                updateRolling(dtLocal);
            } else if (phase == GamePhase.SETTLING) {
                updateSettling(dtLocal);
            } else if (phase == GamePhase.FRAME_END) {
                // small wait before next frame or roll
            } else if (phase == GamePhase.GAME_OVER) {
                // idle
            }
        }

        private void updateAiming(double dtLocal) {
            // Smooth aim using key inputs
            double aimSpeed = 480; // px/s
            double angleSpeed = Math.toRadians(70); // rad/s
            double spinSpeed = 1.7; // per second

            if (input.left) aimPosX -= aimSpeed * dtLocal;
            if (input.right) aimPosX += aimSpeed * dtLocal;

            if (input.up) aimAngle -= angleSpeed * dtLocal;
            if (input.down) aimAngle += angleSpeed * dtLocal;

            if (input.a) spin -= spinSpeed * dtLocal;
            if (input.d) spin += spinSpeed * dtLocal;

            // Clamp ranges
            double margin = 24;
            aimPosX = clamp(aimPosX, lane.laneLeft + margin, lane.laneRight - margin);
            // angle: aim towards pins generally upwards (from -pi to 0),
            // limit to [-120 deg, -60 deg]
            aimAngle = clamp(aimAngle, -Math.toRadians(120), -Math.toRadians(60));
            spin = clamp(spin, -1.0, 1.0);

            // Update ball preview position
            ball.x = aimPosX;
            ball.y = lane.startY;
            ball.vx = 0;
            ball.vy = 0;
            ball.inGutter = false;

            if (charging) {
                phase = GamePhase.CHARGING;
            }
        }

        private void updateCharging(double dtLocal) {
            // Power charges with a pendulum-like oscillation; release Space to lock
            powerOscPhase += dtLocal * 2.2; // frequency of the oscillation
            double wave = 0.5 + 0.5 * Math.sin(powerOscPhase * Math.PI);
            power += (wave - power) * 8 * dtLocal;
            power = clamp(power, 0, 1);

            if (!charging) {
                startThrow();
            }
        }

        private void startThrow() {
            // compute velocity based on power, aim, and slight randomization
            phase = GamePhase.ROLLING;

            double baseSpeed = 1000; // px/s at full power
            double speed = 200 + power * baseSpeed;

            // Add a tiny random error to aim and speed to feel natural
            double angle = aimAngle + Math.toRadians(rng.nextGaussian() * 1.8);
            speed *= (0.98 + rng.nextDouble() * 0.06);

            ball.x = aimPosX;
            ball.y = lane.startY;
            ball.vx = Math.cos(angle) * speed;
            ball.vy = Math.sin(angle) * speed;
            ball.spin = spin;
            ball.launched = true;
            ball.inGutter = false;
            ball.traveled = 0;

            // Reset timers
            settleTimer = 0;

            // Clear power/spin adjustments slightly for next throw
            spin *= 0.7;
            power = 0;
            powerOscPhase = 0;

            floatingMessage = "Rolling...";
            messageTimer = 0.8;
        }

        private void updateRolling(double dtLocal) {
            // Update ball and pin physics
            physics.updateBall(ball, dtLocal, lane);
            // When ball reaches pins region or passes pin deck, check collisions
            physics.handleCollisions(ball, pins, lane, dtLocal);

            // When ball passes beyond pin deck or stops, move to settling
            boolean ballPastDeck = ball.y < lane.pinDeckY - 60 || ball.traveled > lane.laneLength + 100;
            boolean ballStopped = Math.hypot(ball.vx, ball.vy) < 4 || ball.inGutter;

            if (ballPastDeck || ballStopped) {
                // allow pins to settle a bit
                settleTimer = 0.8;
                phase = GamePhase.SETTLING;

                // Add camera shake if many pins fell
                int knocked = countKnockedThisRoll();
                cameraShake += Math.min(12, knocked * 0.9);

                if (knocked == 0) {
                    floatingMessage = ball.inGutter ? "Gutter ball!" : "Miss...";
                    messageTimer = 1.2;
                } else if (knocked >= 7) {
                    floatingMessage = "Great hit!";
                    messageTimer = 1.2;
                }
            }
        }

        private void updateSettling(double dtLocal) {
            // Simple pin "settlement": tiny damping effect (no active physics here to keep it lighter)
            settleTimer -= dtLocal;
            if (settleTimer <= 0) {
                finishRoll();
            }
        }

        private int countStandingPins() {
            int n = 0;
            for (Pin p : pins) if (!p.knocked) n++;
            return n;
        }

        private int countKnockedThisRoll() {
            int n = 0;
            for (Pin p : pins) if (p.knocked && !p.alreadyCountedThisRoll) n++;
            return n;
        }

        private void markCountedPinsForThisRoll() {
            for (Pin p : pins) {
                if (p.knocked) p.alreadyCountedThisRoll = true;
            }
        }

        private void finishRoll() {
            // Count pins downed this roll
            int knockedThisRoll = 0;
            for (Pin p : pins) {
                if (p.knocked && !p.countedInScore) {
                    knockedThisRoll++;
                    p.countedInScore = true;
                }
            }

            // Guard scenario: if ball knocked pins but we already flagged them counted due to previous roll
            // knockedThisRoll here includes only newly knocked pins by this roll (countedInScore prevents double).
            // Score update:
            scoreManager.recordRoll(frameIndex, knockedThisRoll, rollInFrame, isTenthFrame());

            // Special messages for strike/spare
            if (!isTenthFrame()) {
                if (rollInFrame == 0) {
                    if (knockedThisRoll == 10) {
                        floatingMessage = "STRIKE!";
                        messageTimer = 1.5;
                    }
                } else {
                    int pinsDownInFrame = 10 - countStandingPins();
                    // We already knocked pins this frame; compute if it's spare now
                    // Another approach: check from score manager
                    if (scoreManager.isSpare(frameIndex)) {
                        floatingMessage = "Spare!";
                        messageTimer = 1.5;
                    }
                }
            } else {
                // 10th frame messages
                if (scoreManager.lastRollWasStrike(frameIndex, rollInFrame)) {
                    floatingMessage = rollInFrame == 0 ? "STRIKE!" : "Another Strike!";
                    messageTimer = 1.5;
                } else if (scoreManager.lastRollWasSpare(frameIndex, rollInFrame)) {
                    floatingMessage = "Spare!";
                    messageTimer = 1.5;
                }
            }

            // Advance roll/frame logic
            if (isTenthFrame()) {
                // 10th frame rules: up to 3 rolls
                rollInFrame++;
                boolean allowThird = scoreManager.allowThirdRollInTenth(frameIndex);
                boolean frameDone = scoreManager.isFrameComplete(frameIndex, rollInFrame, true);

                if (!frameDone) {
                    // Set up for next roll: keep standing pins if strike/spare rules require reset?
                    // In 10th frame: after strike or spare, pins reset
                    if (scoreManager.shouldResetPinsForNextRollInTenth(frameIndex, rollInFrame)) {
                        resetPins();
                        markPinsNewFrame();
                    } else {
                        // Keep the already knocked pins removed
                    }
                    phase = GamePhase.AIMING;
                    ball.launched = false;
                    ball.vx = ball.vy = 0;
                    charging = false;
                } else {
                    // End game
                    gameOver = true;
                    phase = GamePhase.GAME_OVER;
                    scoreManager.computeCumulative();
                    floatingMessage = "Game Over! Press N for new game";
                    messageTimer = 6.0;
                }
            } else {
                // Frames 1-9
                if (rollInFrame == 0) {
                    if (knockedThisRoll == 10) {
                        // Strike: frame ends
                        frameIndex++;
                        rollInFrame = 0;
                        if (frameIndex >= 10) {
                            endGame();
                        } else {
                            // New frame
                            resetPins();
                            markPinsNewFrame();
                            ball.launched = false;
                            phase = GamePhase.AIMING;
                            floatingMessage = "Frame " + (frameIndex + 1);
                            messageTimer = 1.0;
                        }
                    } else {
                        // Second roll in same frame
                        rollInFrame = 1;
                        // remain standing pins as is
                        ball.launched = false;
                        phase = GamePhase.AIMING;
                    }
                } else {
                    // After second roll, frame ends
                    frameIndex++;
                    rollInFrame = 0;
                    if (frameIndex >= 10) {
                        endGame();
                    } else {
                        resetPins();
                        markPinsNewFrame();
                        ball.launched = false;
                        phase = GamePhase.AIMING;
                        floatingMessage = "Frame " + (frameIndex + 1);
                        messageTimer = 1.0;
                    }
                }
            }
        }

        private void markPinsNewFrame() {
            for (Pin p : pins) {
                p.countedInScore = false;
                p.alreadyCountedThisRoll = false;
            }
        }

        private void endGame() {
            gameOver = true;
            phase = GamePhase.GAME_OVER;
            scoreManager.computeCumulative();
            floatingMessage = "Game Over! Press N for new game";
            messageTimer = 6.0;
        }

        private boolean isTenthFrame() {
            return frameIndex == 9;
        }

        // ========================= Rendering =========================

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Camera shake
            if (cameraShake > 0.1) {
                int sx = (int) ((rng.nextDouble() - 0.5) * cameraShake);
                int sy = (int) ((rng.nextDouble() - 0.5) * cameraShake);
                g2.translate(sx, sy);
            }

            // Draw lane and gutters
            renderLane(g2);

            // Draw guiding marks
            renderLaneArrows(g2);

            // Draw pins
            for (Pin p : pins) {
                if (!p.knocked) {
                    drawPin(g2, p);
                }
            }

            // Draw ball
            drawBall(g2, ball);

            // UI overlay
            g2.setTransform(new AffineTransform()); // reset any camera transform
            renderUI(g2);

            g2.dispose();
        }

        private void renderLane(Graphics2D g2) {
            // Gutter background
            g2.setColor(gutterColor);
            g2.fillRect((int) lane.gutterLeft, (int) lane.foulLineY - 20, (int) (lane.laneWidth + lane.gutterWidth * 2), (int) (lane.laneLength + 100));

            // Lane wood
            Shape laneShape = new RoundRectangle2D.Double(lane.laneLeft, lane.foulLineY, lane.laneWidth, lane.laneLength, 28, 28);
            g2.setColor(laneWood);
            g2.fill(laneShape);

            // Side rails
            g2.setStroke(new BasicStroke(6));
            g2.setColor(laneEdge);
            g2.draw(laneShape);

            // Foul line
            g2.setColor(new Color(40, 30, 20));
            g2.fillRect((int) lane.laneLeft, (int) lane.foulLineY + 1, (int) lane.laneWidth, 6);
        }

        private void renderLaneArrows(Graphics2D g2) {
            // Draw aiming arrows around 15 feet (around 180px from foul line (scaled))
            g2.setColor(new Color(220, 200, 160, 160));
            int rows = 2;
            for (int r = 0; r < rows; r++) {
                double y = lane.foulLineY + 120 + r * 40;
                double spacing = lane.laneWidth / 8.0;
                for (int i = 1; i < 8; i++) {
                    double x = lane.laneLeft + i * spacing;
                    Polygon arrow = new Polygon();
                    arrow.addPoint((int) x, (int) y);
                    arrow.addPoint((int) (x - 6), (int) (y + 12));
                    arrow.addPoint((int) (x + 6), (int) (y + 12));
                    g2.fillPolygon(arrow);
                }
            }

            // Pin deck line
            g2.setColor(new Color(40, 30, 20));
            g2.fillRect((int) lane.laneLeft, (int) (lane.pinDeckY + 2), (int) lane.laneWidth, 4);

            // Aim indicator (only when aiming)
            if (phase == GamePhase.AIMING || phase == GamePhase.CHARGING) {
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(new Color(50, 120, 255, 180));
                double len = 80;
                double x1 = aimPosX;
                double y1 = lane.startY;
                double x2 = x1 + Math.cos(aimAngle) * len;
                double y2 = y1 + Math.sin(aimAngle) * len;
                g2.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
            }
        }

        private void drawPin(Graphics2D g2, Pin p) {
            // body
            g2.setColor(pinColor);
            int r = (int) p.r;
            g2.fillOval((int) (p.x - r), (int) (p.y - r), r * 2, r * 2);

            // stripe
            g2.setColor(pinStripe);
            g2.fillOval((int) (p.x - r), (int) (p.y - r / 2.0), r * 2, r);

            // subtle shading
            g2.setColor(new Color(0, 0, 0, 40));
            g2.drawOval((int) (p.x - r), (int) (p.y - r), r * 2, r * 2);
        }

        private void drawBall(Graphics2D g2, Ball b) {
            // Ball trail
            if (b.launched) {
                g2.setColor(new Color(60, 120, 240, 40));
                for (int i = 0; i < b.trail.size(); i++) {
                    double[] p = b.trail.get(i);
                    int alpha = (int) (40 * (i + 1) / (double) b.trail.size());
                    g2.setColor(new Color(60, 120, 240, alpha));
                    int rr = (int) (b.r * (0.6 + 0.4 * (i + 1) / b.trail.size()));
                    g2.fillOval((int) (p[0] - rr), (int) (p[1] - rr), rr * 2, rr * 2);
                }
            }

            // Ball body
            g2.setColor(ballColor);
            int r = (int) b.r;
            g2.fillOval((int) (b.x - r), (int) (b.y - r), r * 2, r * 2);
            // Shine
            g2.setColor(new Color(255, 255, 255, 90));
            g2.fillOval((int) (b.x - r * 0.4), (int) (b.y - r * 0.8), (int) (r * 0.8), (int) (r * 0.7));
            g2.setColor(new Color(0, 0, 0, 70));
            g2.drawOval((int) (b.x - r), (int) (b.y - r), r * 2, r * 2);
        }

        private void renderUI(Graphics2D g2) {
            // Sidebar for controls and scoreboard
            int sidebarX = (int) (lane.laneRight + lane.gutterWidth + 20);
            int sidebarW = panelW - sidebarX - 20;
            int sidebarY = 20;

            // Panel background
            g2.setColor(new Color(20, 26, 32, 220));
            g2.fillRoundRect(sidebarX, sidebarY, sidebarW, panelH - 40, 16, 16);

            // Title
            g2.setColor(uiText);
            g2.setFont(titleFont);
            g2.drawString("Bowling Simulator", sidebarX + 16, sidebarY + 28);

            // Status line
            g2.setFont(uiFont.deriveFont(Font.BOLD));
            String phaseText = gameOver ? "Game Over" : phase.toString();
            String frameText = "Frame " + (Math.min(frameIndex, 9) + 1);
            String rollText = isTenthFrame() ? ("Roll " + (rollInFrame + 1)) : ("Roll " + (rollInFrame + 1));
            g2.drawString("Status: " + phaseText, sidebarX + 16, sidebarY + 50);
            g2.drawString(frameText + " | " + rollText, sidebarX + 16, sidebarY + 68);

            // Controls
            int y = sidebarY + 96;
            g2.setFont(uiFont);
            g2.drawString("Controls:", sidebarX + 16, y);
            y += 18;
            g2.drawString("Left/Right: Aim position", sidebarX + 16, y); y += 16;
            g2.drawString("Up/Down: Aim angle", sidebarX + 16, y); y += 16;
            g2.drawString("A/D: Spin", sidebarX + 16, y); y += 16;
            g2.drawString("Space: Hold to charge, release to throw", sidebarX + 16, y); y += 16;
            g2.drawString("P: Pause | R: Reset throw (when aiming)", sidebarX + 16, y); y += 16;
            g2.drawString("N: New game (after game over)", sidebarX + 16, y); y += 24;

            // Gauges: Power and Spin
            drawGauge(g2, sidebarX + 16, y, sidebarW - 32, "Power", power, new Color(70, 170, 90));
            y += 36;
            drawGauge(g2, sidebarX + 16, y, sidebarW - 32, "Spin (hook)", (spin + 1) / 2.0, new Color(170, 90, 70));
            y += 36;

            // Aim angle display
            double angleDeg = Math.toDegrees(aimAngle);
            g2.drawString(String.format("Aim angle: %.1f°", angleDeg), sidebarX + 16, y);
            y += 18;

            // Standings
            int standing = countStandingPins();
            g2.drawString("Standing pins: " + standing, sidebarX + 16, y);
            y += 24;

            // Scoreboard
            drawScoreboard(g2, sidebarX + 12, y, sidebarW - 24);

            // Floating message
            if (messageTimer > 0 && floatingMessage != null && !floatingMessage.isEmpty()) {
                float t = (float) Math.min(1.0, messageTimer);
                int alpha = (int) (255 * Math.max(0, Math.min(1, t)));
                g2.setFont(titleFont);
                g2.setColor(new Color(255, 255, 255, alpha));
                FontMetrics fm = g2.getFontMetrics();
                int w = fm.stringWidth(floatingMessage);
                g2.drawString(floatingMessage, (int) (lane.getCenterX() - w / 2.0), (int) (lane.foulLineY - 16));
            }
        }

        private void drawGauge(Graphics2D g2, int x, int y, int w, String label, double value, Color color) {
            int h = 16;
            g2.setColor(uiText);
            g2.drawString(label, x, y - 4);
            g2.setColor(new Color(40, 50, 60));
            g2.fillRoundRect(x, y + 4, w, h, 8, 8);
            g2.setColor(color);
            int fill = (int) (w * clamp(value, 0, 1));
            g2.fillRoundRect(x, y + 4, fill, h, 8, 8);
            g2.setColor(new Color(0, 0, 0, 80));
            g2.drawRoundRect(x, y + 4, w, h, 8, 8);
        }

        private void drawScoreboard(Graphics2D g2, int x, int y, int w) {
            // Draw 10 frames boxes with per-roll marks and totals
            int frameW = Math.max(60, w / 10);
            int h = 80;
            g2.setColor(new Color(25, 32, 40));
            g2.fillRoundRect(x, y, frameW * 10 + 8, h + 8, 10, 10);
            g2.setColor(new Color(0, 0, 0, 60));
            g2.drawRoundRect(x, y, frameW * 10 + 8, h + 8, 10, 10);

            int fx = x + 4;
            int fy = y + 4;
            for (int i = 0; i < 10; i++) {
                int boxW = frameW;
                int boxH = h;
                g2.setColor(new Color(34, 44, 54));
                g2.fillRect(fx + i * boxW, fy, boxW - 2, boxH);
                g2.setColor(new Color(0, 0, 0, 80));
                g2.drawRect(fx + i * boxW, fy, boxW - 2, boxH);

                // Frame number
                g2.setColor(uiText);
                g2.setFont(uiFont.deriveFont(Font.BOLD));
                g2.drawString("" + (i + 1), fx + i * boxW + 4, fy + 14);

                // Rolls
                g2.setFont(uiFont);
                String[] marks = scoreManager.getMarksForFrame(i);
                if (i < 9) {
                    // show two small boxes
                    int bx = fx + i * boxW + boxW - 40;
                    int by = fy + 4;
                    g2.drawRect(bx, by, 16, 16);
                    g2.drawRect(bx + 18, by, 16, 16);
                    if (marks.length > 0) {
                        drawCenteredString(g2, marks[0], new Rectangle(bx, by, 16, 16));
                    }
                    if (marks.length > 1) {
                        drawCenteredString(g2, marks[1], new Rectangle(bx + 18, by, 16, 16));
                    }
                } else {
                    // 10th frame up to three boxes
                    int bx = fx + i * boxW + boxW - 58;
                    int by = fy + 4;
                    g2.drawRect(bx, by, 16, 16);
                    g2.drawRect(bx + 18, by, 16, 16);
                    g2.drawRect(bx + 36, by, 16, 16);
                    for (int k = 0; k < marks.length && k < 3; k++) {
                        drawCenteredString(g2, marks[k], new Rectangle(bx + 18 * k, by, 16, 16));
                    }
                }

                // Total
                String total = scoreManager.getFrameTotal(i);
                if (total != null) {
                    g2.setFont(uiFont.deriveFont(Font.BOLD));
                    drawCenteredString(g2, total, new Rectangle(fx + i * boxW + 2, fy + 26, boxW - 6, 52));
                }
            }
        }

        private void drawCenteredString(Graphics2D g2, String text, Rectangle rect) {
            FontMetrics metrics = g2.getFontMetrics(g2.getFont());
            int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
            int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
            g2.drawString(text, x, y);
        }

        // ========================= Input Handling =========================

        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) input.left = true;
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) input.right = true;
            if (e.getKeyCode() == KeyEvent.VK_UP) input.up = true;
            if (e.getKeyCode() == KeyEvent.VK_DOWN) input.down = true;
            if (e.getKeyCode() == KeyEvent.VK_A) input.a = true;
            if (e.getKeyCode() == KeyEvent.VK_D) input.d = true;

            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                if ((phase == GamePhase.AIMING || phase == GamePhase.CHARGING) && !charging) {
                    charging = true;
                    phase = GamePhase.CHARGING;
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_P) {
                paused = !paused;
            }
            if (e.getKeyCode() == KeyEvent.VK_R) {
                if (phase == GamePhase.AIMING) {
                    // Reset aim to defaults
                    aimPosX = lane.getCenterX();
                    aimAngle = -Math.toRadians(80);
                    spin = 0;
                    power = 0;
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_N) {
                if (phase == GamePhase.GAME_OVER) {
                    initGame();
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_F1) {
                debug = !debug;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) input.left = false;
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) input.right = false;
            if (e.getKeyCode() == KeyEvent.VK_UP) input.up = false;
            if (e.getKeyCode() == KeyEvent.VK_DOWN) input.down = false;
            if (e.getKeyCode() == KeyEvent.VK_A) input.a = false;
            if (e.getKeyCode() == KeyEvent.VK_D) input.d = false;

            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (phase == GamePhase.CHARGING) {
                    charging = false;
                }
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {}

        @Override
        public void mouseMoved(MouseEvent e) {
            // Optional: align aimPosX based on mouse x within lane
            if (phase == GamePhase.AIMING || phase == GamePhase.CHARGING) {
                double mx = e.getX();
                if (mx > lane.laneLeft && mx < lane.laneRight) {
                    aimPosX = clamp(mx, lane.laneLeft + 24, lane.laneRight - 24);
                }
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // Left click toggles charge if aiming
            if (SwingUtilities.isLeftMouseButton(e)) {
                if (phase == GamePhase.AIMING && !charging) {
                    charging = true;
                    phase = GamePhase.CHARGING;
                } else if (phase == GamePhase.CHARGING) {
                    charging = false; // release to throw
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseEntered(MouseEvent e) {}
        @Override
        public void mouseExited(MouseEvent e) {}

        @Override
        public void componentResized(ComponentEvent e) {
            panelW = getWidth();
            panelH = getHeight();
            lane.build(panelW, panelH);
            // Re-center aimPos within lane bounds
            aimPosX = clamp(aimPosX, lane.laneLeft + 24, lane.laneRight - 24);
        }

        @Override public void componentMoved(ComponentEvent e) {}
        @Override public void componentShown(ComponentEvent e) {}
        @Override public void componentHidden(ComponentEvent e) {}
    }

    // ========================= GamePhase Enum =========================

    enum GamePhase {
        AIMING,
        CHARGING,
        ROLLING,
        SETTLING,
        FRAME_END,
        GAME_OVER
    }

    // ========================= Lane =========================

    static class Lane {
        // Geometry (screen coordinates)
        double laneLeft;
        double laneRight;
        double laneWidth;
        double gutterWidth;
        double foulLineY;
        double laneLength;
        double pinDeckY;
        double startX, startY; // initial ball position

        double gutterLeft;

        void build(int panelW, int panelH) {
            // Keep lane aspect ratio
            laneWidth = Math.max(380, Math.min(520, panelW * 0.5));
            gutterWidth = 40;
            laneLeft = 20 + gutterWidth;
            laneRight = laneLeft + laneWidth;
            gutterLeft = laneLeft - gutterWidth;

            // Vertical: foul line near bottom, pins near top
            laneLength = Math.max(520, panelH * 0.75);
            foulLineY = panelH - 140;
            pinDeckY = foulLineY - laneLength + 70;

            startX = getCenterX();
            startY = foulLineY - 20;
        }

        double getCenterX() {
            return (laneLeft + laneRight) / 2.0;
        }
    }

    // ========================= Entities =========================

    static class Ball {
        double x, y;
        double vx, vy;
        double r;
        double spin;       // -1..1
        boolean launched;
        boolean inGutter;
        double traveled;

        // Trail positions for visuals
        final List<double[]> trail = new ArrayList<>();
        private double trailTimer = 0;

        Ball(double x, double y, double r) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.vx = 0;
            this.vy = 0;
            this.spin = 0;
            this.launched = false;
            this.inGutter = false;
            this.traveled = 0;
        }

        void addTrail(double dt) {
            trailTimer += dt;
            if (trailTimer > 0.02) {
                trailTimer = 0;
                trail.add(0, new double[]{x, y});
                if (trail.size() > 12) trail.remove(trail.size() - 1);
            }
        }
    }

    static class Pin {
        double x, y;
        double r;
        boolean knocked;
        boolean countedInScore; // ensures per-roll counting
        boolean alreadyCountedThisRoll;

        Pin(double x, double y, double r) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.knocked = false;
            this.countedInScore = false;
            this.alreadyCountedThisRoll = false;
        }
    }

    // ========================= Physics =========================

    static class PhysicsEngine {
        // Simplified physics: rolling friction, hook curvature, ball-pin hit detection
        private final double laneFriction = 0.992; // per frame factor
        private final double airDrag = 0.999;      // tiny
        private final double hookStrength = 280;   // lateral acceleration factor for spin
        private final double gutterXMargin = 6;    // adjust when to mark as gutter

        private final Random rng = new Random();

        void updateBall(Ball b, double dt, Lane lane) {
            if (!b.launched) return;

            // Hook effect: lateral acceleration perpendicular to velocity
            double speed = Math.hypot(b.vx, b.vy);
            if (speed > 1) {
                // Perpendicular vector to velocity
                double nx = -b.vy / speed;
                double ny = b.vx / speed;
                // Spin sign decides direction
                double a = b.spin * hookStrength; // px/s^2
                b.vx += nx * a * dt;
                b.vy += ny * a * dt;
            }

            // Friction/drag
            b.vx *= Math.pow(laneFriction, dt * 60);
            b.vy *= Math.pow(laneFriction, dt * 60);
            b.vx *= Math.pow(airDrag, dt * 60);
            b.vy *= Math.pow(airDrag, dt * 60);

            // Integrate
            b.x += b.vx * dt;
            b.y += b.vy * dt;

            double stepDist = Math.hypot(b.vx * dt, b.vy * dt);
            b.traveled += stepDist;

            // Trail
            b.addTrail(dt);

            // Gutter detection
            if (b.x < lane.laneLeft - gutterXMargin || b.x > lane.laneRight + gutterXMargin) {
                b.inGutter = true;
            }

            // Bounce softly inside lane edges to avoid leaving visuals too quickly (optional)
            if (b.x - b.r < lane.laneLeft) {
                if (!b.inGutter) {
                    b.x = lane.laneLeft + b.r + 1;
                    b.vx = Math.abs(b.vx) * 0.4;
                }
            } else if (b.x + b.r > lane.laneRight) {
                if (!b.inGutter) {
                    b.x = lane.laneRight - b.r - 1;
                    b.vx = -Math.abs(b.vx) * 0.4;
                }
            }
        }

        void handleCollisions(Ball b, List<Pin> pins, Lane lane, double dt) {
            if (!b.launched) return;

            // Ball-pin collisions
            for (Pin p : pins) {
                if (p.knocked) continue;
                double dx = p.x - b.x;
                double dy = p.y - b.y;
                double dist = Math.hypot(dx, dy);
                double minDist = p.r + b.r;
                if (dist < minDist) {
                    // Mark knocked with some probability weighting based on impact speed
                    p.knocked = true;
                    // impart tiny camera shake effect via probabilistic wobble (handled in panel)
                    // Slight deflection of ball:
                    double overlap = minDist - dist;
                    if (dist > 0.0001) {
                        double nx = dx / dist;
                        double ny = dy / dist;
                        // push ball back
                        b.x -= nx * overlap * 0.5;
                        b.y -= ny * overlap * 0.5;
                        // reflect some velocity component
                        double vn = b.vx * nx + b.vy * ny;
                        double vt_x = b.vx - vn * nx;
                        double vt_y = b.vy - vn * ny;
                        double restitution = 0.3 + rng.nextDouble() * 0.2;
                        double newVn = -vn * restitution;
                        b.vx = vt_x + newVn * nx;
                        b.vy = vt_y + newVn * ny;
                        // reduce speed a bit to simulate energy loss
                        b.vx *= 0.92;
                        b.vy *= 0.92;
                    }

                    // Chain reaction: chance to knock neighboring pins
                    for (Pin q : pins) {
                        if (q == p || q.knocked) continue;
                        double ddx = q.x - p.x;
                        double ddy = q.y - p.y;
                        double d = Math.hypot(ddx, ddy);
                        if (d < p.r * 2.4) {
                            // knock with probability influenced by impact speed
                            double chance = clamp((Math.hypot(b.vx, b.vy) / 1000.0) * 0.7, 0.2, 0.9);
                            if (rng.nextDouble() < chance) {
                                q.knocked = true;
                            }
                        }
                    }
                }
            }

            // Pin-pin gentle interactions for remaining standing pins (slight nudge to resolve overlaps)
            for (int i = 0; i < pins.size(); i++) {
                Pin a = pins.get(i);
                if (a.knocked) continue;
                for (int j = i + 1; j < pins.size(); j++) {
                    Pin c = pins.get(j);
                    if (c.knocked) continue;
                    double dx = c.x - a.x;
                    double dy = c.y - a.y;
                    double dist = Math.hypot(dx, dy);
                    double min = a.r + c.r - 0.8;
                    if (dist < min && dist > 0.0001) {
                        double nx = dx / dist;
                        double ny = dy / dist;
                        double push = (min - dist) * 0.5;
                        a.x -= nx * push;
                        a.y -= ny * push;
                        c.x += nx * push;
                        c.y += ny * push;
                    }
                }
            }
        }
    }

    // ========================= Scoring =========================

    static class ScoreManager {
        private final FrameScore[] frames = new FrameScore[10];

        ScoreManager() {
            newGame();
        }

        void newGame() {
            for (int i = 0; i < 10; i++) frames[i] = new FrameScore();
        }

        void recordRoll(int frameIndex, int pinsKnocked, int rollInFrame, boolean tenth) {
            FrameScore f = frames[frameIndex];
            if (!tenth) {
                if (rollInFrame == 0) {
                    f.addRoll(pinsKnocked);
                    if (pinsKnocked == 10) {
                        f.strike = true;
                    }
                } else {
                    f.addRoll(pinsKnocked);
                    if (f.rolls.size() == 2 && f.getSumFirstTwo() == 10 && !f.strike) {
                        f.spare = true;
                    }
                }
            } else {
                // 10th frame
                f.addRoll(pinsKnocked);
                if (f.rolls.size() == 1 && pinsKnocked == 10) f.strike = true;
                if (f.rolls.size() == 2) {
                    if ((f.rolls.get(0) != 10) && (f.rolls.get(0) + f.rolls.get(1) == 10)) {
                        f.spare = true;
                    }
                }
            }
            computeCumulative(); // update after each roll
        }

        boolean isSpare(int frameIndex) {
            return frames[frameIndex].spare;
        }

        boolean lastRollWasStrike(int frameIndex, int rollInFrame) {
            FrameScore f = frames[frameIndex];
            return rollInFrame >= 0 && rollInFrame < f.rolls.size() && f.rolls.get(rollInFrame) == 10;
        }

        boolean lastRollWasSpare(int frameIndex, int rollInFrame) {
            FrameScore f = frames[frameIndex];
            if (frameIndex < 9) {
                return f.rolls.size() >= 2 && f.rolls.get(0) + f.rolls.get(1) == 10 && f.rolls.get(0) != 10;
            } else {
                return f.rolls.size() >= 2 && f.rolls.get(0) != 10 && (f.rolls.get(0) + f.rolls.get(1) == 10);
            }
        }

        boolean isFrameComplete(int frameIndex, int rollInFrame, boolean tenth) {
            FrameScore f = frames[frameIndex];
            if (!tenth) {
                if (f.strike) return true;
                return f.rolls.size() >= 2;
            } else {
                if (f.rolls.size() < 2) return false;
                if (f.rolls.size() == 2) {
                    if (f.rolls.get(0) == 10 || (f.rolls.get(0) + f.rolls.get(1) == 10)) {
                        return false; // needs third roll
                    } else {
                        return true; // finished without bonus
                    }
                }
                return f.rolls.size() >= 3;
            }
        }

        boolean allowThirdRollInTenth(int frameIndex) {
            FrameScore f = frames[frameIndex];
            if (f.rolls.size() < 2) return false;
            return f.rolls.get(0) == 10 || (f.rolls.get(0) + f.rolls.get(1) == 10);
        }

        boolean shouldResetPinsForNextRollInTenth(int frameIndex, int rollInFrame) {
            FrameScore f = frames[frameIndex];
            if (rollInFrame == 1) {
                // after first roll, if strike, reset
                return f.rolls.size() >= 1 && f.rolls.get(0) == 10;
            }
            if (rollInFrame == 2) {
                // after second roll, if strike+strike or spare, reset for third already done outside
                if (f.rolls.size() >= 2) {
                    if (f.rolls.get(0) == 10 && f.rolls.get(1) == 10) return true;
                    if (f.rolls.get(0) != 10 && f.rolls.get(0) + f.rolls.get(1) == 10) return true;
                }
            }
            return false;
        }

        void computeCumulative() {
            // Compute per-frame totals including bonuses
            int running = 0;
            for (int i = 0; i < 10; i++) {
                FrameScore f = frames[i];
                Integer base = frameBaseScore(i);
                if (base == null) {
                    f.total = null;
                } else {
                    running += base;
                    f.total = running;
                }
            }
        }

        private Integer frameBaseScore(int i) {
            FrameScore f = frames[i];
            if (i < 9) {
                if (f.rolls.isEmpty()) return null;
                if (f.strike) {
                    Integer bonus = nextTwoRolls(i);
                    if (bonus == null) return null;
                    return 10 + bonus;
                } else if (f.spare) {
                    Integer bonus = nextOneRoll(i);
                    if (bonus == null) return null;
                    return 10 + bonus;
                } else {
                    if (f.rolls.size() < 2) return null;
                    return f.rolls.get(0) + f.rolls.get(1);
                }
            } else {
                // 10th frame: sum of all rolls present (up to 3)
                if (f.rolls.isEmpty()) return null;
                // Ensure frame complete to show final total
                if (!isFrameComplete(9, f.rolls.size() - 1, true)) return null;
                int sum = 0;
                for (Integer r : f.rolls) sum += r;
                return sum;
            }
        }

        private Integer nextTwoRolls(int idx) {
            List<Integer> list = new ArrayList<>();
            for (int j = idx + 1; j < 10; j++) {
                list.addAll(frames[j].rolls);
            }
            if (list.size() < 2) return null;
            return list.get(0) + list.get(1);
        }

        private Integer nextOneRoll(int idx) {
            for (int j = idx + 1; j < 10; j++) {
                if (!frames[j].rolls.isEmpty()) return frames[j].rolls.get(0);
            }
            return null;
        }

        String[] getMarksForFrame(int i) {
            FrameScore f = frames[i];
            if (i < 9) {
                String a = "", b = "";
                if (f.rolls.size() >= 1) {
                    a = f.rolls.get(0) == 10 ? "X" : String.valueOf(f.rolls.get(0));
                }
                if (f.rolls.size() >= 2) {
                    if (f.rolls.get(0) != 10 && f.rolls.get(0) + f.rolls.get(1) == 10) {
                        b = "/";
                    } else {
                        b = String.valueOf(f.rolls.get(1));
                    }
                }
                return new String[]{a, b};
            } else {
                // 10th frame
                String[] out = new String[]{"", "", ""};
                for (int r = 0; r < f.rolls.size() && r < 3; r++) {
                    int pins = f.rolls.get(r);
                    if (pins == 10) out[r] = "X";
                    else {
                        if (r == 1 && f.rolls.size() >= 2) {
                            if (f.rolls.get(0) != 10 && f.rolls.get(0) + f.rolls.get(1) == 10) {
                                out[r] = "/";
                                continue;
                            }
                        }
                        if (r == 2 && f.rolls.size() >= 3) {
                            if (f.rolls.get(1) != 10 && f.rolls.get(1) + f.rolls.get(2) == 10 && f.rolls.get(0) == 10) {
                                out[r] = "/";
                                continue;
                            }
                        }
                        out[r] = String.valueOf(pins);
                    }
                }
                return out;
            }
        }

        String getFrameTotal(int i) {
            Integer t = frames[i].total;
            return t == null ? null : String.valueOf(t);
        }
    }

    static class FrameScore {
        List<Integer> rolls = new ArrayList<>();
        boolean strike = false;
        boolean spare = false;
        Integer total = null;

        void addRoll(int pins) {
            // clamp 0..10 but let 10th frame handle third roll >10 sums by logic
            pins = Math.max(0, Math.min(10, pins));
            rolls.add(pins);
        }

        int getSumFirstTwo() {
            if (rolls.size() < 2) return rolls.stream().mapToInt(i -> i).sum();
            return rolls.get(0) + rolls.get(1);
        }
    }


    static class InputState {
        boolean left, right, up, down, a, d;
    }


    static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }


}