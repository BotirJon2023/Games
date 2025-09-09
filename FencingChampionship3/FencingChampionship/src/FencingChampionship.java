import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;

public class FencingChampionship extends JFrame {

    public FencingChampionship() {
        super("Fencing Championship");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel panel = new GamePanel();
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        panel.requestFocusInWindow();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FencingChampionship());
    }

    // ========================= GAME PANEL =========================
    static class GamePanel extends JPanel implements ActionListener, KeyListener, FocusListener {

        // Canvas size
        public static final int VIEW_W = 1024;
        public static final int VIEW_H = 576;

        // Timing/loop
        private Timer timer;
        private long lastTimeNs;

        // Game states
        private enum State { MENU, PLAYING, INTERMISSION, GAME_OVER, PAUSED }
        private State state = State.MENU;

        // Championship/bout parameters
        private int targetTouches = 5;
        private int totalOpponents = 3;
        private int currentOpponentIdx = 0;
        private int roundTimeSeconds = 60;

        // Camera
        private double cameraX = 0;
        private double cameraTargetX = 0;

        // Entities
        private Fencer p1;
        private Fencer p2;
        private AIController ai;
        private boolean twoPlayer = false;

        // Particles
        private java.util.List<Particle> particles = new ArrayList<>();

        // HUD and UI knobs
        private int fontSize = 18;
        private long boutStartTimeMs = 0;
        private long pausedAtMs = 0;
        private long totalPausedMs = 0;
        private boolean showDebug = false;

        // Round flashes
        private double flashTimer = 0;

        // Background audience dots
        private java.util.List<Point> audienceDots = new ArrayList<>();
        private Random rng = new Random(0xBEEF);

        // Difficulty
        private AIDifficulty difficulty = AIDifficulty.PRO;

        // Controls help strings
        private String[] helpLines = new String[] {
                "Player 1: A/D move, W jump, F lunge, G parry",
                "Global: Enter start/next, P pause, R reset, 1/2/3 difficulty, T toggle 2P",
                "Player 2 (2P): Arrow keys move/jump, '/' lunge, '.' parry"
        };

        public GamePanel() {
            setPreferredSize(new Dimension(VIEW_W, VIEW_H));
            setFocusable(true);
            addKeyListener(this);
            addFocusListener(this);

            // Initialize fencers
            resetChampionship();

            // Audience dots for background
            for (int i = 0; i < 600; i++) {
                audienceDots.add(new Point(rng.nextInt(2000) - 500, rng.nextInt(200)));
            }

            timer = new Timer(1000 / 60, this);
            lastTimeNs = System.nanoTime();
            timer.start();
        }

        // ========================= RESET / SETUP =========================

        private void resetChampionship() {
            particles.clear();
            p1 = new Fencer("Player 1", Color.WHITE, Color.BLUE.darker(), true);
            p1.setPosition(300, 0);

            p2 = createOpponent(0);
            currentOpponentIdx = 0;

            ai = new AIController(difficulty);
            twoPlayer = false;

            state = State.MENU;
            p1.score = 0;
            p2.score = 0;
            totalPausedMs = 0;
            pausedAtMs = 0;
            flashTimer = 0;
        }

        private Fencer createOpponent(int idx) {
            // Different color schemes per opponent
            Color suit = new Color(245, 245, 245);
            Color accent;
            if (idx == 0) accent = new Color(200, 50, 50);
            else if (idx == 1) accent = new Color(50, 200, 50);
            else accent = new Color(220, 180, 40);

            Fencer opp = new Fencer("Opponent " + (idx + 1), suit, accent, false);
            opp.setPosition(700, 0);
            opp.setFacingLeft(true);
            return opp;
        }

        private void startNewBout() {
            // Reset positions, scores for this bout
            particles.clear();
            p1.resetForBout();
            p2 = createOpponent(currentOpponentIdx);
            cameraX = 0;
            cameraTargetX = 0;
            flashTimer = 0;

            boutStartTimeMs = System.currentTimeMillis();
            totalPausedMs = 0;
            state = State.PLAYING;
        }

        private void nextOpponentOrFinish() {
            // Move to next opponent or championship victory
            currentOpponentIdx++;
            if (currentOpponentIdx >= totalOpponents) {
                state = State.GAME_OVER;
                spawnConfetti();
                Toolkit.getDefaultToolkit().beep();
            } else {
                state = State.INTERMISSION;
                p1.resetForBout(); // Keep P1 score to 0 for new bout display reset
                p2 = createOpponent(currentOpponentIdx);
                flashTimer = 2.0;
            }
        }

        private void spawnConfetti() {
            for (int i = 0; i < 300; i++) {
                Color c = Color.getHSBColor(rng.nextFloat(), 0.7f, 1f);
                double x = VIEW_W / 2 + rng.nextGaussian() * 180;
                double y = 100 + rng.nextDouble() * 50;
                double vx = (rng.nextDouble() - 0.5) * 200;
                double vy = -50 - rng.nextDouble() * 180;
                double life = 2 + rng.nextDouble() * 2;
                double size = 3 + rng.nextDouble() * 4;
                particles.add(new Particle(x, y, vx, vy, life, c, size, true));
            }
        }

        // ========================= UPDATE LOOP =========================

        @Override
        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            double dt = Math.min(0.05, (now - lastTimeNs) / 1_000_000_000.0);
            lastTimeNs = now;

            if (state == State.PAUSED || state == State.MENU) {
                repaint();
                return;
            }

            if (state == State.INTERMISSION) {
                flashTimer -= dt;
                if (flashTimer <= 0) {
                    state = State.PLAYING;
                    startNewBout();
                }
                updateParticles(dt);
                repaint();
                return;
            }

            if (state == State.GAME_OVER) {
                updateParticles(dt);
                cameraUpdate(dt);
                repaint();
                return;
            }

            // Playing
            handleAIIfNeeded(dt);
            updateFencers(dt);
            handleCollisionsAndScoring(dt);
            updateParticles(dt);
            cameraUpdate(dt);

            // Bout timer
            if (state == State.PLAYING) {
                int timeLeft = getTimeLeftSeconds();
                if (timeLeft <= 0) {
                    // Time up: higher score wins or sudden death if tie
                    if (p1.score != p2.score) {
                        if (p1.score > p2.score) {
                            // Player wins this bout
                            nextOpponentOrFinish();
                        } else {
                            // Loss -> reset championship to menu
                            state = State.GAME_OVER;
                            Toolkit.getDefaultToolkit().beep();
                        }
                    } else {
                        // Sudden death touch next
                        // Keep playing with "priority" fudge: keep as is; first to touch wins
                        // To visually indicate, change flashTimer to flicker
                        flashTimer += dt;
                    }
                }
            }

            repaint();
        }

        private void handleAIIfNeeded(double dt) {
            if (!twoPlayer) {
                InputState aiInput = ai.computeInput(p2, p1, dt);
                p2.applyInput(aiInput);
            }
        }

        private void updateFencers(double dt) {
            // Update inputs from players (Player 1 always, Player 2 only in 2P)
            p1.update(this, p2, dt);
            p2.update(this, p1, dt);
        }

        private void handleCollisionsAndScoring(double dt) {
            // Sword vs target detection
            // We check both sides, but only score if neither already touched during freeze
            if (!p1.roundFrozen && !p2.roundFrozen) {
                // P1 attacking P2
                TouchResult r1 = p1.tryHit(p2);
                handleTouchResult(r1, p1, p2);

                // P2 attacking P1
                TouchResult r2 = p2.tryHit(p1);
                handleTouchResult(r2, p2, p1);
            }

            // After a touch, freeze briefly and reset
            if (p1.roundFrozen || p2.roundFrozen) {
                p1.roundFreezeTimer -= dt;
                p2.roundFreezeTimer -= dt;
                if (p1.roundFreezeTimer <= 0 && p2.roundFreezeTimer <= 0) {
                    // Reset to en garde
                    resetEnGarde();
                }
            }
        }

        private void handleTouchResult(TouchResult res, Fencer attacker, Fencer defender) {
            if (res == TouchResult.NONE) return;

            if (res == TouchResult.PARRY) {
                // Attacker gets stunned; defender gains riposte window
                // Spawn spark particles near blades
                Point2D tip = attacker.getSwordTip();
                for (int i = 0; i < 10; i++) {
                    double ang = rng.nextDouble() * Math.PI * 2;
                    double sp = 80 + rng.nextDouble() * 120;
                    particles.add(new Particle(
                            tip.getX(), tip.getY(),
                            Math.cos(ang) * sp, Math.sin(ang) * sp,
                            0.3 + rng.nextDouble() * 0.4,
                            new Color(255, 240, 200), 2 + rng.nextDouble() * 2, false
                    ));
                }
                attacker.stun(0.35);
                defender.parrySuccessFlash = 0.25;
                Toolkit.getDefaultToolkit().beep();
                return;
            }

            if (res == TouchResult.HIT) {
                attacker.score++;
                defender.hitFlash = 0.35;
                defender.stun(0.25);
                freezeRound();

                // Particles at hit location (defender torso)
                Point2D pt = defender.getTorsoCenter();
                for (int i = 0; i < 20; i++) {
                    double ang = rng.nextDouble() * Math.PI * 2;
                    double sp = 50 + rng.nextDouble() * 180;
                    Color c = (i % 2 == 0) ? attacker.accentColor : new Color(255, 255, 255);
                    particles.add(new Particle(
                            pt.getX(), pt.getY(),
                            Math.cos(ang) * sp, Math.sin(ang) * sp,
                            0.4 + rng.nextDouble() * 0.6,
                            c,
                            2 + rng.nextDouble() * 3,
                            false
                    ));
                }

                // Check bout end
                if (attacker.score >= targetTouches) {
                    if (attacker == p1) {
                        // Player wins bout -> next opponent or finish
                        nextOpponentOrFinish();
                    } else {
                        // Player lost
                        state = State.GAME_OVER;
                        Toolkit.getDefaultToolkit().beep();
                    }
                }
                return;
            }
        }

        private void freezeRound() {
            p1.roundFrozen = true;
            p2.roundFrozen = true;
            p1.roundFreezeTimer = 1.2;
            p2.roundFreezeTimer = 1.2;
        }

        private void resetEnGarde() {
            p1.setPosition(300, 0);
            p2.setPosition(700, 0);
            p1.resetAfterPoint();
            p2.resetAfterPoint();
            p1.setFacingLeft(false);
            p2.setFacingLeft(true);
        }

        private void updateParticles(double dt) {
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.update(dt);
                if (p.life <= 0) particles.remove(i);
            }
        }

        private void cameraUpdate(double dt) {
            // Follow midpoint of fencers, keep within bounds
            double mid = (p1.x + p2.x) / 2.0;
            cameraTargetX = mid - VIEW_W / 2.0;
            cameraTargetX = Math.max(-200, Math.min(cameraTargetX, 600));
            cameraX += (cameraTargetX - cameraX) * Math.min(1.0, dt * 5);
        }

        // ========================= DRAWING =========================

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background
            drawBackground(g2);

            // Translate for camera
            AffineTransform old = g2.getTransform();
            g2.translate(-cameraX, 0);

            // Draw piste
            drawPiste(g2);

            // Draw particles behind? Let's draw fencers first then particles on top for hits
            p1.draw(g2);
            p2.draw(g2);

            // Particles
            for (Particle p : particles) p.draw(g2);

            g2.setTransform(old);

            // HUD
            drawHUD(g2);

            g2.dispose();
        }

        private void drawBackground(Graphics2D g2) {
            // Gradient background
            GradientPaint gp = new GradientPaint(0, 0, new Color(20, 24, 40), 0, VIEW_H, new Color(10, 14, 24));
            g2.setPaint(gp);
            g2.fillRect(0, 0, VIEW_W, VIEW_H);

            // Stadium stands area
            int standsTop = 80;
            int standsBottom = 250;
            GradientPaint stands = new GradientPaint(0, standsTop, new Color(30, 35, 55), 0, standsBottom, new Color(15, 18, 30));
            g2.setPaint(stands);
            g2.fillRect(0, standsTop, VIEW_W, standsBottom - standsTop);

            // Audience dots
            g2.setColor(new Color(200, 200, 220, 140));
            for (Point p : audienceDots) {
                int x = (int) (p.x - cameraX * 0.3);
                int y = 110 + p.y / 2;
                if (x < -10 || x > VIEW_W + 10) continue;
                g2.fillRect(x, y, 2, 2);
            }

            // Spotlight beams
            g2.setColor(new Color(255, 255, 255, 12));
            for (int i = 0; i < 6; i++) {
                int bx = (int) (i * VIEW_W / 6.0);
                Polygon poly = new Polygon();
                poly.addPoint(bx + 60, 0);
                poly.addPoint(bx + 120, 0);
                poly.addPoint(bx + 220, 260);
                poly.addPoint(bx + 20, 260);
                g2.fillPolygon(poly);
            }
        }

        private void drawPiste(Graphics2D g2) {
            // Floor
            int floorY = Fencer.groundY();
            g2.setColor(new Color(70, 70, 80));
            g2.fillRect((int) (-4000), floorY, 8000, VIEW_H - floorY);

            // Piste (strip)
            int pisteY = floorY - 20;
            int pisteH = 40;
            g2.setColor(new Color(200, 200, 210));
            RoundRectangle2D rr = new RoundRectangle2D.Double(-2000, pisteY, 4000, pisteH, 16, 16);
            g2.fill(rr);

            // Center line
            g2.setColor(new Color(20, 20, 30, 140));
            g2.fillRect(-2000, floorY, 4000, 2);

            // Lines and boxes
            g2.setColor(new Color(40, 40, 60, 100));
            for (int i = -1500; i <= 1500; i += 100) {
                g2.fillRect(i, pisteY, 2, pisteH);
            }
        }

        private void drawHUD(Graphics2D g2) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, (float) fontSize));
            // Score boxes
            int pad = 10;
            int boxW = 200;
            int boxH = 60;
            int y = pad;

            // P1 score box
            drawScoreBox(g2, pad, y, boxW, boxH, p1, true);
            // P2 score box
            drawScoreBox(g2, VIEW_W - pad - boxW, y, boxW, boxH, p2, false);

            // Timer and opponent info centered
            String oppName = (state == State.GAME_OVER && currentOpponentIdx >= totalOpponents) ? "Champion!" :
                    (state == State.MENU ? "Press Enter to Start" : "Opponent " + (currentOpponentIdx + 1) + " / " + totalOpponents);
            int timeLeft = Math.max(0, getTimeLeftSeconds());
            String tStr;
            if (state == State.MENU) tStr = "Menu";
            else if (state == State.INTERMISSION) tStr = "Get Ready";
            else if (state == State.GAME_OVER) tStr = "Finished";
            else tStr = String.format("%02d:%02d", timeLeft / 60, timeLeft % 60);

            String center = oppName + "    Time: " + tStr + "    Target: " + targetTouches;

            g2.setColor(new Color(255, 255, 255, 200));
            drawCenteredString(g2, center, VIEW_W / 2, y + 40);

            // Help lines
            if (state == State.MENU) {
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
                int ty = VIEW_H - 80;
                for (String s : helpLines) {
                    drawCenteredString(g2, s, VIEW_W / 2, ty);
                    ty += 20;
                }
                String diff = "Difficulty: " + difficulty.name() + "   Two-Player: " + (twoPlayer ? "ON" : "OFF");
                drawCenteredString(g2, diff, VIEW_W / 2, VIEW_H - 30);
            }

            // Pause overlay
            if (state == State.PAUSED) {
                g2.setColor(new Color(0, 0, 0, 140));
                g2.fillRect(0, 0, VIEW_W, VIEW_H);
                g2.setColor(Color.WHITE);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
                drawCenteredString(g2, "PAUSED", VIEW_W / 2, VIEW_H / 2 - 20);
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
                drawCenteredString(g2, "Press P to resume", VIEW_W / 2, VIEW_H / 2 + 10);
            }

            // Intermission overlay
            if (state == State.INTERMISSION) {
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRect(0, 0, VIEW_W, VIEW_H);
                g2.setColor(Color.WHITE);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
                drawCenteredString(g2, "Next Bout: " + p2.name, VIEW_W / 2, VIEW_H / 2 - 30);
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));
                drawCenteredString(g2, "Get Ready...", VIEW_W / 2, VIEW_H / 2 + 10);
            }

            // Game over overlay
            if (state == State.GAME_OVER) {
                g2.setColor(new Color(0, 0, 0, 140));
                g2.fillRect(0, 0, VIEW_W, VIEW_H);
                g2.setColor(Color.WHITE);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
                String msg = (currentOpponentIdx >= totalOpponents) ? "You are the Champion!" : "Defeat";
                drawCenteredString(g2, msg, VIEW_W / 2, VIEW_H / 2 - 30);
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));
                drawCenteredString(g2, "Press R to reset, Enter for Menu", VIEW_W / 2, VIEW_H / 2 + 10);
            }

            if (showDebug) {
                g2.setColor(new Color(255, 255, 255, 200));
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
                int dy = 16;
                int ty = VIEW_H - 8 - 6 * dy;
                g2.drawString(String.format("CameraX: %.1f", cameraX), 8, ty); ty += dy;
                g2.drawString(String.format("P1 x=%.1f state=%s", p1.x, p1.state), 8, ty); ty += dy;
                g2.drawString(String.format("P2 x=%.1f state=%s", p2.x, p2.state), 8, ty); ty += dy;
                g2.drawString("Particles: " + particles.size(), 8, ty); ty += dy;
                g2.drawString("TwoPlayer: " + twoPlayer + "  Diff: " + difficulty, 8, ty); ty += dy;
                g2.drawString("State: " + state, 8, ty);
            }
        }

        private void drawScoreBox(Graphics2D g2, int x, int y, int w, int h, Fencer f, boolean left) {
            // Background with accent
            g2.setColor(new Color(30, 30, 50, 180));
            g2.fillRoundRect(x, y, w, h, 12, 12);
            g2.setColor(f.accentColor);
            g2.fillRoundRect(x, y + h - 8, w, 8, 12, 12);

            // Name and score
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
            String name = f.name;
            String score = "" + f.score;
            g2.drawString(name, x + 12, y + 24);

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
            FontMetrics fm = g2.getFontMetrics();
            int sw = fm.stringWidth(score);
            g2.drawString(score, x + w - sw - 12, y + 40);
        }

        private void drawCenteredString(Graphics2D g2, String s, int cx, int cy) {
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(s);
            int a = fm.getAscent();
            g2.drawString(s, cx - w / 2, cy + a / 2);
        }

        // ========================= TIME HELPERS =========================

        private int getTimeLeftSeconds() {
            if (state != State.PLAYING) return roundTimeSeconds;
            long now = System.currentTimeMillis();
            long elapsed = now - boutStartTimeMs - totalPausedMs;
            int left = roundTimeSeconds - (int) (elapsed / 1000);
            return Math.max(0, left);
        }

        // ========================= INPUT =========================

        @Override
        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (state == State.MENU) {
                if (k == KeyEvent.VK_ENTER) {
                    startNewBout();
                } else if (k == KeyEvent.VK_T) {
                    twoPlayer = !twoPlayer;
                } else if (k == KeyEvent.VK_1) {
                    difficulty = AIDifficulty.ROOKIE;
                } else if (k == KeyEvent.VK_2) {
                    difficulty = AIDifficulty.PRO;
                } else if (k == KeyEvent.VK_3) {
                    difficulty = AIDifficulty.ELITE;
                } else if (k == KeyEvent.VK_R) {
                    resetChampionship();
                } else if (k == KeyEvent.VK_P) {
                    // ignore pause in menu
                } else if (k == KeyEvent.VK_F3) {
                    showDebug = !showDebug;
                }
                repaint();
                return;
            }

            if (k == KeyEvent.VK_P) {
                if (state == State.PAUSED) {
                    state = State.PLAYING;
                    totalPausedMs += (System.currentTimeMillis() - pausedAtMs);
                } else if (state == State.PLAYING) {
                    pausedAtMs = System.currentTimeMillis();
                    state = State.PAUSED;
                }
                return;
            }

            if (state == State.INTERMISSION) {
                if (k == KeyEvent.VK_ENTER) {
                    startNewBout();
                } else if (k == KeyEvent.VK_R) {
                    resetChampionship();
                }
                return;
            }

            if (state == State.GAME_OVER) {
                if (k == KeyEvent.VK_R) {
                    resetChampionship();
                } else if (k == KeyEvent.VK_ENTER) {
                    state = State.MENU;
                }
                return;
            }

            if (state == State.PLAYING) {
                // Player 1 controls
                if (k == KeyEvent.VK_A) p1.input.left = true;
                if (k == KeyEvent.VK_D) p1.input.right = true;
                if (k == KeyEvent.VK_W) p1.input.jump = true;
                if (k == KeyEvent.VK_F) p1.input.lunge = true;
                if (k == KeyEvent.VK_G) p1.input.parry = true;

                // Two player controls
                if (twoPlayer) {
                    if (k == KeyEvent.VK_LEFT) p2.input.left = true;
                    if (k == KeyEvent.VK_RIGHT) p2.input.right = true;
                    if (k == KeyEvent.VK_UP) p2.input.jump = true;
                    if (k == KeyEvent.VK_SLASH) p2.input.lunge = true;
                    if (k == KeyEvent.VK_PERIOD) p2.input.parry = true;
                }

                // Utilities
                if (k == KeyEvent.VK_R) {
                    resetChampionship();
                }
                if (k == KeyEvent.VK_T) {
                    twoPlayer = !twoPlayer;
                }
                if (k == KeyEvent.VK_1) difficulty = AIDifficulty.ROOKIE;
                if (k == KeyEvent.VK_2) difficulty = AIDifficulty.PRO;
                if (k == KeyEvent.VK_3) difficulty = AIDifficulty.ELITE;
                if (k == KeyEvent.VK_F3) showDebug = !showDebug;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int k = e.getKeyCode();
            // Player 1
            if (k == KeyEvent.VK_A) p1.input.left = false;
            if (k == KeyEvent.VK_D) p1.input.right = false;
            if (k == KeyEvent.VK_W) p1.input.jump = false;
            if (k == KeyEvent.VK_F) p1.input.lunge = false;
            if (k == KeyEvent.VK_G) p1.input.parry = false;

            // Player 2
            if (twoPlayer) {
                if (k == KeyEvent.VK_LEFT) p2.input.left = false;
                if (k == KeyEvent.VK_RIGHT) p2.input.right = false;
                if (k == KeyEvent.VK_UP) p2.input.jump = false;
                if (k == KeyEvent.VK_SLASH) p2.input.lunge = false;
                if (k == KeyEvent.VK_PERIOD) p2.input.parry = false;
            }
        }

        @Override public void keyTyped(KeyEvent e) {}

        @Override
        public void focusGained(FocusEvent e) {}
        @Override
        public void focusLost(FocusEvent e) {
            // Release keys if focus lost
            p1.input.clear();
            p2.input.clear();
        }
    }

    // ========================= FENCER AND RELATED =========================

    static enum FencerState {
        IDLE, ADVANCING, RETREATING, LUNGING, RECOVERING, PARRYING, STUNNED, JUMPING
    }

    static enum TouchResult {
        NONE, HIT, PARRY
    }

    static class InputState {
        boolean left, right, jump, lunge, parry;

        void clear() {
            left = right = jump = lunge = parry = false;
        }
    }

    static class Fencer {
        String name;
        Color suitColor;
        Color accentColor;

        // Position (x only; y is vertical offset for jump)
        double x;
        double y; // jump offset
        double vy;

        // Facing: true if facing left, else right
        boolean facingLeft = false;

        // Stats / speeds
        double speedAdvance = 120;
        double speedRetreat = 100;
        double lungeSpeed = 460;
        double lungeReach = 90;   // extra reach when lunging
        double baseReach = 70;    // base arm + blade
        double parryDuration = 0.25;
        double parryCooldown = 0.35;

        // State
        FencerState state = FencerState.IDLE;
        double stateTimer = 0;
        boolean onGround = true;

        // Sword animation
        double bladeExtension = 0; // 0..1
        double bladeExtendSpeed = 3.8;
        double bladeRetractSpeed = 2.6;

        // Parry
        boolean parryActive = false;
        double parryTimer = 0;
        double parryCDTimer = 0;
        double parrySuccessFlash = 0;

        // Round freeze for hit stop
        boolean roundFrozen = false;
        double roundFreezeTimer = 0;

        // Hit flash
        double hitFlash = 0;

        // Scoring
        int score = 0;

        // Input
        InputState input = new InputState();

        // Seed for drawing jitter
        Random rng = new Random();

        // Constructor
        public Fencer(String name, Color suit, Color accent, boolean leftSide) {
            this.name = name;
            this.suitColor = suit;
            this.accentColor = accent;
            this.facingLeft = !leftSide ? true : false;
        }

        public static int groundY() {
            // Floor baseline
            return 400;
        }

        public void setFacingLeft(boolean val) {
            this.facingLeft = val;
        }

        public void setPosition(double nx, double ny) {
            this.x = nx;
            this.y = ny;
        }

        public void resetForBout() {
            this.score = 0;
            this.state = FencerState.IDLE;
            this.stateTimer = 0;
            this.bladeExtension = 0;
            this.parryActive = false;
            this.parryTimer = 0;
            this.parryCDTimer = 0;
            this.roundFrozen = false;
            this.roundFreezeTimer = 0;
            this.hitFlash = 0;
            this.y = 0;
            this.vy = 0;
            this.onGround = true;
        }

        public void resetAfterPoint() {
            this.state = FencerState.IDLE;
            this.stateTimer = 0;
            this.bladeExtension = 0;
            this.parryActive = false;
            this.parryTimer = 0;
            this.parryCDTimer = 0;
            this.roundFrozen = false;
            this.roundFreezeTimer = 0;
            this.hitFlash = 0;
            this.y = 0;
            this.vy = 0;
            this.onGround = true;
            input.clear();
        }

        public void stun(double duration) {
            state = FencerState.STUNNED;
            stateTimer = duration;
            parryActive = false;
            parryTimer = 0;
        }

        public void applyInput(InputState in) {
            // Used by AI to set input directly
            this.input.left = in.left;
            this.input.right = in.right;
            this.input.jump = in.jump;
            this.input.lunge = in.lunge;
            this.input.parry = in.parry;
        }

        public void update(GamePanel gp, Fencer opponent, double dt) {
            // Facing towards opponent
            if (this.x < opponent.x) facingLeft = false; else facingLeft = true;

            // Visual flashes
            if (hitFlash > 0) hitFlash -= dt;
            if (parrySuccessFlash > 0) parrySuccessFlash -= dt;

            // Round freeze: no movement but maintain minor retract of blade
            if (roundFrozen) {
                bladeExtension = Math.max(0, bladeExtension - dt * bladeRetractSpeed * 0.5);
                return;
            }

            // Apply gravity for cosmetic jumps
            int groundY = groundY();
            if (!onGround) {
                vy += 900 * dt;
                y += vy * dt;
                if (y >= 0) {
                    y = 0;
                    vy = 0;
                    onGround = true;
                    if (state == FencerState.JUMPING) {
                        state = FencerState.IDLE;
                        stateTimer = 0;
                    }
                }
            }

            // Parry cooldown
            if (parryCDTimer > 0) parryCDTimer -= dt;

            // State machine
            switch (state) {
                case IDLE:
                case ADVANCING:
                case RETREATING:
                    movementLogic(dt);
                    attackParryLogic(dt);
                    break;
                case LUNGING:
                    // Move forward quickly, extend blade
                    double dir = facingLeft ? -1 : 1;
                    this.x += dir * lungeSpeed * dt;
                    bladeExtension += bladeExtendSpeed * dt;
                    if (bladeExtension >= 1) {
                        bladeExtension = 1;
                        state = FencerState.RECOVERING;
                        stateTimer = 0.15; // short hold before retract/recover
                    }
                    // Input parry ignored during lunge
                    if (input.jump && onGround) {
                        // ignore jump during lunge
                    }
                    break;
                case RECOVERING:
                    stateTimer -= dt;
                    bladeExtension -= bladeRetractSpeed * dt;
                    if (bladeExtension < 0) bladeExtension = 0;
                    if (stateTimer <= 0) {
                        state = FencerState.IDLE;
                    }
                    movementLogic(dt); // allow footwork during recovery
                    break;
                case PARRYING:
                    parryTimer -= dt;
                    if (parryTimer <= 0) {
                        parryActive = false;
                        state = FencerState.IDLE;
                        parryCDTimer = parryCDTimer + 0.1; // small cooldown buffer
                    }
                    // slight backward drift during parry
                    double dir2 = facingLeft ? 1 : -1;
                    this.x += dir2 * 40 * dt;
                    break;
                case STUNNED:
                    stateTimer -= dt;
                    bladeExtension -= bladeRetractSpeed * dt;
                    if (bladeExtension < 0) bladeExtension = 0;
                    if (stateTimer <= 0) {
                        state = FencerState.IDLE;
                    }
                    break;
                case JUMPING:
                    // Horizontal control mid-air
                    movementLogic(dt);
                    attackParryLogic(dt);
                    break;
            }

            // Clamp x within arena bounds
            this.x = Math.max(-1500, Math.min(1500, this.x));
        }

        private void movementLogic(double dt) {
            // Footwork: advance/retreat based on input and facing
            double dirAdvance = facingLeft ? -1 : 1;
            double dirRetreat = -dirAdvance;

            boolean advancing = false, retreating = false;

            if (input.left) {
                // Move left relative to world; may mean advance or retreat depending on facing
                double worldDir = -1;
                if ((worldDir == dirAdvance)) advancing = true; else retreating = true;
            }
            if (input.right) {
                double worldDir = 1;
                if ((worldDir == dirAdvance)) advancing = true; else retreating = true;
            }

            if (advancing && !retreating) {
                this.x += dirAdvance * speedAdvance * dt;
                state = FencerState.ADVANCING;
            } else if (retreating && !advancing) {
                this.x += dirRetreat * speedRetreat * dt;
                state = FencerState.RETREATING;
            } else {
                if (state == FencerState.ADVANCING || state == FencerState.RETREATING) {
                    state = FencerState.IDLE;
                }
            }

            // Jump (cosmetic)
            if (input.jump && onGround && (state == FencerState.IDLE || state == FencerState.ADVANCING || state == FencerState.RETREATING)) {
                onGround = false;
                vy = -300;
                state = FencerState.JUMPING;
            }
        }

        private void attackParryLogic(double dt) {
            // Attack: lunge
            if (input.lunge) {
                // Begin lunge if not parrying and not stunned/recovering
                if (state != FencerState.LUNGING && state != FencerState.RECOVERING && state != FencerState.STUNNED && state != FencerState.PARRYING) {
                    state = FencerState.LUNGING;
                    // Quick initial extension
                    bladeExtension = Math.max(bladeExtension, 0.2);
                }
            }

            // Parry
            if (input.parry && parryCDTimer <= 0) {
                if (state != FencerState.LUNGING && state != FencerState.STUNNED) {
                    state = FencerState.PARRYING;
                    parryActive = true;
                    parryTimer = parryDuration;
                    parryCDTimer = parryCooldown;
                }
            }

            // Retract blade slowly when not lunging
            if (state != FencerState.LUNGING) {
                bladeExtension -= dt * bladeRetractSpeed * 0.5;
                if (bladeExtension < 0) bladeExtension = 0;
            }
        }

        public TouchResult tryHit(Fencer defender) {
            // Pre-check: only can hit if blade extended meaningfully
            if (bladeExtension < 0.45) return TouchResult.NONE;

            // Tip position
            Point2D tip = getSwordTip();

            // Defender target area: torso ellipse
            Ellipse2D target = defender.getTargetArea();

            if (defender.parryActive) {
                // Check if tip is near defender's guard area for parry deflection
                Ellipse2D guard = defender.getGuardArea();
                if (guard.contains(tip)) {
                    return TouchResult.PARRY;
                }
            }

            if (target.contains(tip)) {
                return TouchResult.HIT;
            }

            return TouchResult.NONE;
        }

        public Ellipse2D getTargetArea() {
            // Torso ellipse position
            Point2D c = getTorsoCenter();
            return new Ellipse2D.Double(c.getX() - 16, c.getY() - 22, 32, 44);
        }

        public Ellipse2D getGuardArea() {
            // Small circle in front of defender where parries can deflect
            Point2D h = getHandPosition();
            return new Ellipse2D.Double(h.getX() - 14, h.getY() - 14, 28, 28);
        }

        public Point2D getTorsoCenter() {
            int gy = groundY();
            double baseY = gy - 40 - y; // higher when jumping negative y
            return new Point2D.Double(x, baseY);
        }

        public Point2D getHandPosition() {
            Point2D torso = getTorsoCenter();
            double dir = facingLeft ? -1 : 1;
            return new Point2D.Double(torso.getX() + dir * 20, torso.getY() + 5);
        }

        public Point2D getSwordTip() {
            Point2D hand = getHandPosition();
            double dir = facingLeft ? -1 : 1;
            double reach = baseReach + bladeExtension * lungeReach;
            return new Point2D.Double(hand.getX() + dir * reach, hand.getY());
        }

        public void draw(Graphics2D g2) {
            int gy = groundY();

            // Shadows
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval((int) (x - 18), gy + 8, 36, 10);

            // Flashes
            double flashAlpha = Math.max(0, hitFlash) * 0.9;
            Color suit = blend(suitColor, Color.WHITE, flashAlpha);
            double parryAlpha = Math.max(0, parrySuccessFlash) * 0.9;
            Color accent = blend(accentColor, Color.WHITE, parryAlpha);

            // Torso
            Point2D torso = getTorsoCenter();
            g2.setColor(suit);
            g2.fillOval((int) (torso.getX() - 14), (int) (torso.getY() - 20), 28, 36);

            // Head
            g2.setColor(new Color(230, 230, 230));
            g2.fillOval((int) (torso.getX() - 12), (int) (torso.getY() - 42), 24, 24);
            // Mask stripe
            g2.setColor(new Color(60, 60, 80));
            g2.fillRect((int) (torso.getX() - 12), (int) (torso.getY() - 32), 24, 6);

            // Arms and guard
            Point2D hand = getHandPosition();
            g2.setStroke(new BasicStroke(4f));
            g2.setColor(suit);
            // Arm from torso to hand
            g2.drawLine((int) torso.getX(), (int) torso.getY(), (int) hand.getX(), (int) hand.getY());
            // Guard
            g2.setColor(accent);
            g2.fillOval((int) (hand.getX() - 6), (int) (hand.getY() - 6), 12, 12);

            // Sword blade
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(210, 210, 230));
            Point2D tip = getSwordTip();
            g2.drawLine((int) hand.getX(), (int) hand.getY(), (int) tip.getX(), (int) tip.getY());

            // Small tip dot
            g2.setColor(new Color(255, 180, 180));
            g2.fillOval((int) tip.getX() - 2, (int) tip.getY() - 2, 4, 4);

            // Legs
            double dir = facingLeft ? -1 : 1;
            int footY = gy - (int) y;
            int footX1 = (int) (x - 10 * dir);
            int footX2 = (int) (x + 18 * dir);

            g2.setStroke(new BasicStroke(3f));
            g2.setColor(suit);
            g2.drawLine((int) torso.getX(), (int) (torso.getY() + 16), footX1, footY);
            g2.drawLine((int) torso.getX(), (int) (torso.getY() + 16), footX2, footY);

            // Belt or accent stripe
            g2.setColor(accent);
            g2.fillRect((int) (torso.getX() - 15), (int) (torso.getY() - 5), 30, 4);

            // Optional debug target
            // g2.setColor(new Color(255,0,0,80));
            // Ellipse2D target = getTargetArea();
            // g2.fill(target);
        }

        private Color blend(Color c1, Color c2, double t) {
            t = Math.max(0, Math.min(1, t));
            int r = (int) (c1.getRed() * (1 - t) + c2.getRed() * t);
            int g = (int) (c1.getGreen() * (1 - t) + c2.getGreen() * t);
            int b = (int) (c1.getBlue() * (1 - t) + c2.getBlue() * t);
            int a = (int) (255);
            return new Color(r, g, b, a);
        }
    }

    // ========================= AI =========================

    enum AIDifficulty {
        ROOKIE, PRO, ELITE
    }

    static class AIController {
        private AIDifficulty difficulty;
        private Random rng = new Random();
        private double lungeCooldown = 0;
        private double parryReactTimer = 0;

        public AIController(AIDifficulty diff) {
            this.difficulty = diff;
        }

        public void setDifficulty(AIDifficulty diff) {
            this.difficulty = diff;
        }

        public InputState computeInput(Fencer me, Fencer opp, double dt) {
            InputState in = new InputState();

            // Timers
            if (lungeCooldown > 0) lungeCooldown -= dt;
            if (parryReactTimer > 0) parryReactTimer -= dt;

            // Distance management
            double dist = Math.abs(me.x - opp.x);
            double comfortMin, comfortMax, lungeRange, aggressiveness, parrySkill;

            switch (difficulty) {
                case ROOKIE:
                    comfortMin = 220;
                    comfortMax = 300;
                    lungeRange = 150;
                    aggressiveness = 0.35;
                    parrySkill = 0.35;
                    break;
                case PRO:
                    comfortMin = 200;
                    comfortMax = 280;
                    lungeRange = 160;
                    aggressiveness = 0.55;
                    parrySkill = 0.6;
                    break;
                default: // ELITE
                    comfortMin = 180;
                    comfortMax = 260;
                    lungeRange = 170;
                    aggressiveness = 0.75;
                    parrySkill = 0.85;
                    break;
            }

            // Footwork: move to maintain comfort distance and seek opportunity
            if (dist < comfortMin) {
                // Retreat
                if (me.facingLeft) {
                    in.right = true;
                } else {
                    in.left = true;
                }
            } else if (dist > comfortMax) {
                // Advance
                if (me.facingLeft) {
                    in.left = true;
                } else {
                    in.right = true;
                }
            } else {
                // Within comfort: micro-adjust with small randomness
                if (rng.nextDouble() < 0.02) {
                    if (rng.nextBoolean()) {
                        if (me.facingLeft) in.left = true; else in.right = true;
                    } else {
                        if (me.facingLeft) in.right = true; else in.left = true;
                    }
                }
            }

            // Attack logic: lunge when within range and cooldown done
            boolean oppIsOpen = opp.state != FencerState.PARRYING && opp.state != FencerState.LUNGING;
            boolean inRange = dist < lungeRange + rng.nextDouble() * 20;
            if (opp.hitFlash > 0) oppIsOpen = true; // just got hit
            if (inRange && oppIsOpen && lungeCooldown <= 0) {
                if (rng.nextDouble() < aggressiveness) {
                    in.lunge = true;
                    lungeCooldown = 0.6 + rng.nextDouble() * 0.8;
                }
            }

            // Reactive parry: if opponent lunges and tip close to guard area
            if (opp.state == FencerState.LUNGING) {
                Point2D tip = opp.getSwordTip();
                Ellipse2D guard = me.getGuardArea();
                if (guard.contains(tip)) {
                    if (rng.nextDouble() < parrySkill) {
                        in.parry = true;
                    }
                } else if (parryReactTimer <= 0) {
                    // Anticipate parry if in mid-range
                    if (dist < lungeRange + 10 && rng.nextDouble() < parrySkill * 0.4) {
                        in.parry = true;
                        parryReactTimer = 0.25;
                    }
                }
            }

            // Occasional jump to look lively (cosmetic)
            if (rng.nextDouble() < 0.005) in.jump = true;

            return in;
        }
    }

    // ========================= PARTICLES =========================

    static class Particle {
        double x, y;
        double vx, vy;
        double life;
        Color color;
        double size;
        boolean affectedByGravity;

        public Particle(double x, double y, double vx, double vy, double life, Color color, double size, boolean gravity) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.color = color;
            this.size = size;
            this.affectedByGravity = gravity;
        }

        public void update(double dt) {
            if (affectedByGravity) vy += 300 * dt;
            x += vx * dt;
            y += vy * dt;
            life -= dt;
            // Slight drag
            vx *= (1 - 0.5 * dt);
            vy *= (1 - 0.1 * dt);
        }

        public void draw(Graphics2D g2) {
            if (life <= 0) return;
            int alpha = (int) (255 * Math.max(0, Math.min(1, life)));
            Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
            g2.setColor(c);
            g2.fillOval((int) (x - size / 2), (int) (y - size / 2), (int) size, (int) size);
        }
    }
}
