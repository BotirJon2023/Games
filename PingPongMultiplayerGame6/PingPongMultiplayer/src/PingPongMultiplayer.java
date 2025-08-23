import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import java.util.*;

/**
 * PingPongMultiplayer — a single-file Java (Swing) arcade game
 *
 * Features
 * - Local multiplayer (Left: W/S, Right: ↑/↓)
 * - Smooth animation with Swing Timer
 * - Particles and motion trails (toggleable)
 * - Power-ups (toggleable): Grow/Shrink paddles, Slow/Fast ball, Sticky paddle, Ghost ball
 * - Score to win / win-by-2 rules
 * - Pause, Resume, Restart, Quick Settings
 * - Optional right-side AI for practice (toggleable)
 * - Single .java file > 600 lines
 *
 * This program avoids external dependencies and JavaFX. It should run on any
 * standard JRE with AWT/Swing support.
 */
public class PingPongMultiplayer {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Ping-Pong Multiplayer (Swing)");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setResizable(false);

            GamePanel panel = new GamePanel();
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.requestFocusInWindow();
        });
    }

    /** GamePanel handles game loop, rendering, and input */
    static class GamePanel extends JPanel implements ActionListener {
        // Canvas dimensions
        public static final int WIDTH = 1000;
        public static final int HEIGHT = 600;

        // Gameplay constants
        private static final int PADDING = 30;
        private static final int BASE_PADDLE_W = 14;
        private static final int BASE_PADDLE_H = 120;
        private static final double PADDLE_SPEED = 7.0;
        private static final double BALL_SPEED = 6.0;
        private static final int TARGET_SCORE = 11; // first to 11, win by 2

        // Loop timing
        private static final int FPS = 120; // frames per second
        private static final int TIMER_DELAY = 1000 / FPS;

        // Game state
        private enum State { MENU, COUNTDOWN, PLAYING, PAUSED, GAME_OVER }
        private State state = State.MENU;

        // Entities
        private final Paddle leftPaddle;
        private final Paddle rightPaddle;
        private final List<Ball> balls = new ArrayList<>();
        private final List<Particle> particles = new ArrayList<>();
        private final List<PowerUp> powerUps = new ArrayList<>();
        private final Random rng = new Random();

        // Score
        private int leftScore = 0;
        private int rightScore = 0;

        // Options / toggles
        private boolean particlesEnabled = true;
        private boolean powerUpsEnabled = true;
        private boolean rightAIEnabled = false; // practice mode
        private boolean sfxEnabled = true;

        // UI helpers
        private final javax.swing.Timer timer;
        private long lastTimeNanos = System.nanoTime();
        private double countdown = 0; // seconds for COUNTDOWN state
        private String message = ""; // transient HUD message
        private long messageUntil = 0; // system time millis until message hides

        // Key state
        private final Set<Integer> keys = new HashSet<>();

        // Theme
        private final Theme theme = new Theme();

        GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            setBackground(theme.bg);

            leftPaddle = new Paddle(PADDING, HEIGHT / 2 - BASE_PADDLE_H / 2, BASE_PADDLE_W, BASE_PADDLE_H, PADDLE_SPEED);
            rightPaddle = new Paddle(WIDTH - PADDING - BASE_PADDLE_W, HEIGHT / 2 - BASE_PADDLE_H / 2, BASE_PADDLE_W, BASE_PADDLE_H, PADDLE_SPEED);

            spawnBall(true);

            setupKeyBindings();
            timer = new javax.swing.Timer(TIMER_DELAY, this);
            timer.start();
        }

        private void setupKeyBindings() {
            // Use Key Bindings instead of KeyListener for smoother input
            InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = getActionMap();

            String[] onKeys = {"pressed W", "pressed S", "pressed UP", "pressed DOWN", "pressed SPACE", "pressed P", "pressed R", "pressed ESCAPE", "pressed A", "pressed D", "pressed LEFT", "pressed RIGHT", "pressed 1", "pressed 2", "pressed 3", "pressed 4", "pressed 5", "pressed 6", "pressed 7"};
            String[] offKeys = {"released W", "released S", "released UP", "released DOWN", "released SPACE", "released P", "released R", "released ESCAPE", "released A", "released D", "released LEFT", "released RIGHT", "released 1", "released 2", "released 3", "released 4", "released 5", "released 6", "released 7"};

            int[] keyCodes = {KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_SPACE, KeyEvent.VK_P, KeyEvent.VK_R, KeyEvent.VK_ESCAPE, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7};

            for (int i = 0; i < keyCodes.length; i++) {
                final int code = keyCodes[i];
                im.put(KeyStroke.getKeyStroke(code, 0, false), onKeys[i]);
                im.put(KeyStroke.getKeyStroke(code, 0, true), offKeys[i]);
                am.put(onKeys[i], new AbstractAction() { public void actionPerformed(ActionEvent e) { keys.add(code); onKeyPress(code); }});
                am.put(offKeys[i], new AbstractAction() { public void actionPerformed(ActionEvent e) { keys.remove(code); onKeyRelease(code); }});
            }
        }

        private void onKeyPress(int code) {
            switch (code) {
                case KeyEvent.VK_SPACE:
                    if (state == State.MENU) startCountdown();
                    else if (state == State.PAUSED) resumeGame();
                    else if (state == State.PLAYING) pauseGame();
                    else if (state == State.GAME_OVER) resetMatch();
                    break;
                case KeyEvent.VK_P:
                    if (state == State.PLAYING) pauseGame();
                    else if (state == State.PAUSED) resumeGame();
                    break;
                case KeyEvent.VK_R:
                    resetMatch();
                    break;
                case KeyEvent.VK_ESCAPE:
                    state = State.MENU;
                    showMessage("Back to menu", 1200);
                    break;
                case KeyEvent.VK_1:
                    particlesEnabled = !particlesEnabled;
                    showMessage("Particles: " + (particlesEnabled ? "ON" : "OFF"), 1200);
                    break;
                case KeyEvent.VK_2:
                    powerUpsEnabled = !powerUpsEnabled;
                    showMessage("Power-ups: " + (powerUpsEnabled ? "ON" : "OFF"), 1200);
                    break;
                case KeyEvent.VK_3:
                    rightAIEnabled = !rightAIEnabled;
                    showMessage("Right AI: " + (rightAIEnabled ? "ON" : "OFF"), 1200);
                    break;
                case KeyEvent.VK_4:
                    theme.nextPalette();
                    setBackground(theme.bg);
                    repaint();
                    showMessage("Theme switched", 1000);
                    break;
                case KeyEvent.VK_5:
                    sfxEnabled = !sfxEnabled;
                    showMessage("SFX: " + (sfxEnabled ? "ON" : "OFF"), 1000);
                    break;
                case KeyEvent.VK_6:
                    // spawn an extra ball for fun during play
                    if (state == State.PLAYING) {
                        spawnBall(false);
                        showMessage("Extra ball!", 900);
                    }
                    break;
                case KeyEvent.VK_7:
                    // quick nudge to randomize ball
                    if (state == State.PLAYING && !balls.isEmpty()) {
                        Ball b = balls.get(0);
                        b.vx *= 1.1; b.vy *= 1.1;
                        showMessage("Speed boost", 800);
                    }
                    break;
            }
        }

        private void onKeyRelease(int code) {
            // No-op but reserved for future use
        }

        private void startCountdown() {
            if (state == State.MENU || state == State.PAUSED || state == State.GAME_OVER) {
                state = State.COUNTDOWN;
                countdown = 3.0; // 3 seconds countdown
            }
        }

        private void pauseGame() {
            if (state == State.PLAYING) {
                state = State.PAUSED;
                showMessage("Paused — press SPACE to resume", 1500);
            }
        }

        private void resumeGame() {
            if (state == State.PAUSED) {
                startCountdown();
            }
        }

        private void resetMatch() {
            leftScore = 0;
            rightScore = 0;
            leftPaddle.reset(BASE_PADDLE_W, BASE_PADDLE_H, PADDLE_SPEED);
            rightPaddle.reset(BASE_PADDLE_W, BASE_PADDLE_H, PADDLE_SPEED);
            balls.clear();
            powerUps.clear();
            spawnBall(true);
            state = State.MENU;
            showMessage("Match reset", 1000);
        }

        private void spawnBall(boolean center) {
            Ball b = new Ball(WIDTH / 2.0, HEIGHT / 2.0, 10);
            if (!center) {
                // Spawn near center but offset
                b.x = WIDTH / 2.0 + rng.nextInt(31) - 15;
                b.y = HEIGHT / 2.0 + rng.nextInt(31) - 15;
            }
            // Random initial direction
            double angle = Math.toRadians(15 + rng.nextInt(60)); // between 15 and 75 degrees
            double speed = BALL_SPEED * (0.9 + rng.nextDouble() * 0.2);
            int dir = rng.nextBoolean() ? 1 : -1;
            b.vx = dir * speed * Math.cos(angle);
            b.vy = speed * Math.sin(angle) * (rng.nextBoolean() ? 1 : -1);
            balls.add(b);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Main loop via Swing Timer
            long now = System.nanoTime();
            double dt = (now - lastTimeNanos) / 1_000_000_000.0; // seconds
            lastTimeNanos = now;

            update(dt);
            repaint();
        }

        private void update(double dt) {
            switch (state) {
                case MENU:
                    // idle animations: slowly move paddles
                    leftPaddle.aiIdleWobble(dt);
                    rightPaddle.aiIdleWobble(dt);
                    break;
                case COUNTDOWN:
                    countdown -= dt;
                    if (countdown <= 0) {
                        state = State.PLAYING;
                        countdown = 0;
                    }
                    break;
                case PLAYING:
                    handleInput(dt);
                    if (rightAIEnabled) rightPaddle.followAI(balls, dt);
                    for (Paddle p : Arrays.asList(leftPaddle, rightPaddle)) {
                        p.update(dt);
                        p.clamp(0, HEIGHT);
                    }
                    updateBalls(dt);
                    updatePowerUps(dt);
                    updateParticles(dt);
                    maybeSpawnPowerUps(dt);
                    break;
                case PAUSED:
                case GAME_OVER:
                    // No physics
                    break;
            }
        }

        private void handleInput(double dt) {
            // Left paddle: W/S; with A/D for tiny horizontal shake (visual flair)
            double vyL = 0;
            if (keys.contains(KeyEvent.VK_W)) vyL -= 1;
            if (keys.contains(KeyEvent.VK_S)) vyL += 1;
            leftPaddle.vy = vyL * leftPaddle.speed;

            // Right paddle: Up/Down
            if (!rightAIEnabled) {
                double vyR = 0;
                if (keys.contains(KeyEvent.VK_UP)) vyR -= 1;
                if (keys.contains(KeyEvent.VK_DOWN)) vyR += 1;
                rightPaddle.vy = vyR * rightPaddle.speed;
            } else {
                rightPaddle.vy = 0; // AI controls it in update()
            }
        }

        private void updateBalls(double dt) {
            // Using an iterator because balls can be removed
            for (int i = 0; i < balls.size(); i++) {
                Ball b = balls.get(i);
                b.update(dt);

                // Wall collisions
                if (b.y - b.r <= 0) {
                    b.y = b.r;
                    b.vy = Math.abs(b.vy);
                    sfxBeep();
                    spawnParticles(b.x, b.y, 6);
                } else if (b.y + b.r >= HEIGHT) {
                    b.y = HEIGHT - b.r;
                    b.vy = -Math.abs(b.vy);
                    sfxBeep();
                    spawnParticles(b.x, b.y, 6);
                }

                // Paddle collisions
                if (b.vx < 0 && b.x - b.r <= leftPaddle.x + leftPaddle.w && b.y >= leftPaddle.y && b.y <= leftPaddle.y + leftPaddle.h) {
                    // impact position
                    double rel = ((b.y - leftPaddle.y) / leftPaddle.h) - 0.5; // -0.5..0.5
                    double angle = rel * Math.toRadians(120); // spread
                    double speed = Math.hypot(b.vx, b.vy) * 1.04 + 0.2;
                    b.vx = Math.abs(speed * Math.cos(angle));
                    b.vy = speed * Math.sin(angle);
                    b.x = leftPaddle.x + leftPaddle.w + b.r; // prevent sticking
                    if (leftPaddle.stickyTimer > 0) {
                        b.stickTo(leftPaddle, 0.4);
                    }
                    sfxBeep();
                    spawnParticles(b.x, b.y, 10);
                }
                if (b.vx > 0 && b.x + b.r >= rightPaddle.x && b.y >= rightPaddle.y && b.y <= rightPaddle.y + rightPaddle.h) {
                    double rel = ((b.y - rightPaddle.y) / rightPaddle.h) - 0.5;
                    double angle = -rel * Math.toRadians(120);
                    double speed = Math.hypot(b.vx, b.vy) * 1.04 + 0.2;
                    b.vx = -Math.abs(speed * Math.cos(angle));
                    b.vy = speed * Math.sin(angle);
                    b.x = rightPaddle.x - b.r; // prevent sticking
                    if (rightPaddle.stickyTimer > 0) {
                        b.stickTo(rightPaddle, 0.4);
                    }
                    sfxBeep();
                    spawnParticles(b.x, b.y, 10);
                }

                // Ghost mode: briefly pass through paddles (effect visualized via alpha)
                if (b.ghostTimer > 0) {
                    b.ghostTimer -= dt;
                }

                // Goal check
                if (b.x + b.r < 0) {
                    // right scores
                    rightScore++;
                    roundOver(false);
                    return;
                } else if (b.x - b.r > WIDTH) {
                    // left scores
                    leftScore++;
                    roundOver(true);
                    return;
                }

                // Particle trail
                if (particlesEnabled && rng.nextFloat() < 0.45f) {
                    particles.add(Particle.fromBall(b, theme));
                }
            }
        }

        private void roundOver(boolean leftScored) {
            balls.clear();
            powerUps.clear();
            leftPaddle.onRoundEnd();
            rightPaddle.onRoundEnd();

            String scorer = leftScored ? "Left" : "Right";
            showMessage(scorer + " scored!", 800);
            if (checkGameOver()) {
                state = State.GAME_OVER;
                return;
            }
            spawnBall(true);
            state = State.COUNTDOWN;
            countdown = 2.0;
        }

        private boolean checkGameOver() {
            int lead = Math.abs(leftScore - rightScore);
            int max = Math.max(leftScore, rightScore);
            if (max >= TARGET_SCORE && lead >= 2) {
                return true;
            }
            return false;
        }

        private void updateParticles(double dt) {
            if (!particlesEnabled) {
                particles.clear();
                return;
            }
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.update(dt);
                if (p.life <= 0) particles.remove(i);
            }
        }

        private void updatePowerUps(double dt) {
            for (int i = powerUps.size() - 1; i >= 0; i--) {
                PowerUp pu = powerUps.get(i);
                pu.update(dt);
                if (pu.life <= 0) { powerUps.remove(i); continue; }

                // Collision with ball: apply effect to appropriate side based on ball direction
                for (Ball b : balls) {
                    if (pu.bounds().intersects(b.bounds())) {
                        applyPowerUp(pu, b);
                        powerUps.remove(i);
                        break;
                    }
                }
            }

            // Decay temporary paddle effects
            leftPaddle.updateEffects(dt);
            rightPaddle.updateEffects(dt);
        }

        private void applyPowerUp(PowerUp pu, Ball b) {
            switch (pu.type) {
                case GROW_PADDLE:
                    targetPaddle(b).grow(1.25, 6.0);
                    showMessage(targetName(b) + " paddle grew", 900);
                    break;
                case SHRINK_OPPONENT:
                    opponentPaddle(b).shrink(0.8, 6.0);
                    showMessage(opponentName(b) + " paddle shrank", 900);
                    break;
                case BALL_SLOW:
                    b.scaleSpeed(0.8, 4.0);
                    showMessage("Ball slowed", 800);
                    break;
                case BALL_FAST:
                    b.scaleSpeed(1.25, 4.0);
                    showMessage("Ball sped up", 800);
                    break;
                case STICKY_PADDLE:
                    targetPaddle(b).makeSticky(4.5);
                    showMessage(targetName(b) + " got sticky paddle", 1100);
                    break;
                case GHOST_BALL:
                    b.ghostTimer = 3.5;
                    showMessage("Ghost ball!", 900);
                    break;
            }
            sfxBeep();
            spawnParticles(pu.x + pu.size/2, pu.y + pu.size/2, 14);
        }

        private Paddle targetPaddle(Ball b) {
            return b.vx > 0 ? rightPaddle : leftPaddle;
        }
        private Paddle opponentPaddle(Ball b) {
            return b.vx > 0 ? leftPaddle : rightPaddle;
        }
        private String targetName(Ball b) { return b.vx > 0 ? "Right" : "Left"; }
        private String opponentName(Ball b) { return b.vx > 0 ? "Left" : "Right"; }

        private double powerupTimer = 0;
        private void maybeSpawnPowerUps(double dt) {
            if (!powerUpsEnabled) return;
            powerupTimer -= dt;
            if (powerupTimer <= 0) {
                powerupTimer = 3.0 + rng.nextDouble() * 3.5; // every ~3-6.5s
                PowerUp.Type[] types = PowerUp.Type.values();
                PowerUp.Type t = types[rng.nextInt(types.length)];
                int size = 24;
                double x = WIDTH * (0.25 + rng.nextDouble() * 0.5) - size/2.0;
                double y = HEIGHT * (0.2 + rng.nextDouble() * 0.6) - size/2.0;
                powerUps.add(new PowerUp(x, y, size, t, theme));
            }
        }

        private void spawnParticles(double x, double y, int n) {
            if (!particlesEnabled) return;
            for (int i = 0; i < n; i++) particles.add(Particle.burst(x, y, theme));
        }

        private void sfxBeep() {
            if (!sfxEnabled) return;
            Toolkit.getDefaultToolkit().beep();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background gradient
            Paint old = g2.getPaint();
            GradientPaint gp = new GradientPaint(0, 0, theme.bg, 0, HEIGHT, theme.bg2);
            g2.setPaint(gp);
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            g2.setPaint(old);

            // Middle dashed line
            g2.setColor(theme.fieldLines);
            Stroke oldStroke = g2.getStroke();
            float[] dash = {10f, 14f};
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10f, dash, 0f));
            g2.drawLine(WIDTH/2, 0, WIDTH/2, HEIGHT);
            g2.setStroke(oldStroke);

            // Draw power-ups first (beneath balls)
            for (PowerUp pu : powerUps) pu.draw(g2);

            // Draw paddles
            leftPaddle.draw(g2, theme);
            rightPaddle.draw(g2, theme);

            // Draw balls
            for (Ball b : balls) b.draw(g2, theme);

            // Particles
            for (Particle p : particles) p.draw(g2);

            // HUD
            drawHUD(g2);

            g2.dispose();
        }

        private void drawHUD(Graphics2D g2) {
            g2.setFont(theme.uiFontLarge());
            String scoreText = leftScore + "  :  " + rightScore;
            FontMetrics fm = g2.getFontMetrics();
            int sw = fm.stringWidth(scoreText);
            g2.setColor(theme.uiText);
            g2.drawString(scoreText, WIDTH / 2 - sw / 2, 60);

            g2.setFont(theme.uiFontSmall());
            int y = HEIGHT - 18;
            String help = "SPACE: start/pause • R: reset • ESC: menu • 1: particles • 2: power-ups • 3: right AI • 4: theme • 5: sfx • 6: extra ball";
            drawShadowText(g2, help, 16, y);

            switch (state) {
                case MENU:
                    drawCenteredFancy(g2, "Ping-Pong Multiplayer", HEIGHT/2 - 40, theme.uiText, theme.shadow);
                    g2.setFont(theme.uiFontMedium());
                    drawShadowText(g2, "W/S vs ↑/↓ — Press SPACE to play", WIDTH/2 - 180, HEIGHT/2 + 10);
                    drawShadowText(g2, "First to " + TARGET_SCORE + ", win by 2.", WIDTH/2 - 105, HEIGHT/2 + 34);
                    drawShadowText(g2, "Toggle options with 1–5. Practice with AI via 3.", WIDTH/2 - 200, HEIGHT/2 + 58);
                    break;
                case COUNTDOWN:
                    g2.setFont(theme.uiFontHuge());
                    String t = String.valueOf(Math.max(1, (int)Math.ceil(countdown)));
                    drawCenteredFancy(g2, t, HEIGHT/2 - 20, theme.accent, theme.shadow);
                    break;
                case PAUSED:
                    drawCenteredFancy(g2, "Paused", HEIGHT/2 - 20, theme.uiText, theme.shadow);
                    g2.setFont(theme.uiFontMedium());
                    drawShadowText(g2, "Press SPACE to resume", WIDTH/2 - 105, HEIGHT/2 + 20);
                    break;
                case GAME_OVER:
                    String win = leftScore > rightScore ? "Left Player Wins!" : "Right Player Wins!";
                    drawCenteredFancy(g2, win, HEIGHT/2 - 20, theme.accent, theme.shadow);
                    g2.setFont(theme.uiFontMedium());
                    drawShadowText(g2, "Press SPACE to return to menu", WIDTH/2 - 150, HEIGHT/2 + 22);
                    break;
                case PLAYING:
                    // inline message if any
                    break;
            }

            if (message != null && !message.isEmpty() && System.currentTimeMillis() < messageUntil) {
                g2.setFont(theme.uiFontMedium());
                String msg = message;
                FontMetrics fm2 = g2.getFontMetrics();
                int w = fm2.stringWidth(msg);
                int x = WIDTH/2 - w/2;
                int boxPad = 8;
                int boxH = fm2.getHeight() + 6;
                int boxY = 90;
                g2.setColor(new Color(0,0,0,120));
                g2.fillRoundRect(x - boxPad, boxY - fm2.getAscent(), w + boxPad*2, boxH, 12, 12);
                g2.setColor(theme.accent);
                g2.drawRoundRect(x - boxPad, boxY - fm2.getAscent(), w + boxPad*2, boxH, 12, 12);
                g2.setColor(Color.WHITE);
                g2.drawString(msg, x, boxY);
            }
        }

        private void showMessage(String msg, int durationMs) {
            this.message = msg;
            this.messageUntil = System.currentTimeMillis() + durationMs;
        }

        private void drawCenteredFancy(Graphics2D g2, String text, int y, Color main, Color shadow) {
            g2.setFont(theme.uiFontHuge());
            FontMetrics fm = g2.getFontMetrics();
            int sw = fm.stringWidth(text);
            int x = WIDTH/2 - sw/2;
            g2.setColor(shadow);
            g2.drawString(text, x+2, y+2);
            g2.setColor(main);
            g2.drawString(text, x, y);
        }

        private void drawShadowText(Graphics2D g2, String text, int x, int y) {
            g2.setColor(theme.shadow);
            g2.drawString(text, x+1, y+1);
            g2.setColor(theme.uiText);
            g2.drawString(text, x, y);
        }
    }

    /** Simple paddle with temporary effects */
    static class Paddle {
        double x, y, w, h;
        double speed;
        double vy;

        // Temporary effect modifiers
        double sizeScale = 1.0;
        double sizeTimer = 0.0;
        double speedScale = 1.0;
        double speedTimer = 0.0;
        double stickyTimer = 0.0;

        // Idle wobble phase for menu
        double wobblePhase = Math.random() * Math.PI * 2;

        // AI helper
        double aiLag = 0.18; // reaction lag seconds
        double aiTimer = 0.0;

        Paddle(double x, double y, double w, double h, double speed) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.speed = speed; this.vy = 0;
        }

        void reset(double w, double h, double speed) {
            this.w = w; this.h = h; this.speed = speed;
            sizeScale = 1.0; sizeTimer = 0; speedScale = 1.0; speedTimer = 0; stickyTimer = 0;
        }

        void update(double dt) {
            y += vy * dt;
        }

        void clamp(double top, double bottom) {
            double hh = h * sizeScale;
            if (y < top) y = top;
            if (y + hh > bottom) y = bottom - hh;
        }

        void draw(Graphics2D g2, Theme theme) {
            double hh = h * sizeScale;
            Shape r = new RoundRectangle2D.Double(x, y, w, hh, 16, 16);
            g2.setColor(theme.paddleFill);
            g2.fill(r);
            g2.setColor(theme.paddleEdge);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(r);

            // sticky indicator
            if (stickyTimer > 0) {
                g2.setColor(new Color(255, 255, 255, 100));
                for (int i = 0; i < 4; i++) g2.draw(new RoundRectangle2D.Double(x-2-i, y-2-i, w+4+2*i, hh+4+2*i, 16, 16));
            }
        }

        void aiIdleWobble(double dt) {
            wobblePhase += dt * 0.7;
            y += Math.sin(wobblePhase) * 0.4;
        }

        void followAI(List<Ball> balls, double dt) {
            // Follow the closest approaching ball
            Ball target = null;
            double bestT = Double.POSITIVE_INFINITY;
            for (Ball b : balls) {
                if (b.vx > 0 && x > GamePanel.WIDTH/2 - 30) { // right paddle behavior
                    double t = (x - b.x) / b.vx; // time to reach paddle x
                    if (t > 0 && t < bestT) { bestT = t; target = b; }
                }
            }
            aiTimer -= dt;
            if (aiTimer <= 0) {
                aiTimer = aiLag;
                if (target != null) {
                    double aimY = target.y - (h * sizeScale)/2;
                    double dy = aimY - y;
                    vy = Math.signum(dy) * speed * speedScale * 0.95;
                } else {
                    vy = 0;
                }
            }
        }

        void grow(double factor, double seconds) {
            sizeScale = clamp(sizeScale * factor, 0.6, 1.8);
            sizeTimer = Math.max(sizeTimer, seconds);
        }

        void shrink(double factor, double seconds) {
            sizeScale = clamp(sizeScale * factor, 0.6, 1.8);
            sizeTimer = Math.max(sizeTimer, seconds);
        }

        void makeSticky(double seconds) {
            stickyTimer = Math.max(stickyTimer, seconds);
        }

        void updateEffects(double dt) {
            if (sizeTimer > 0) { sizeTimer -= dt; if (sizeTimer <= 0) sizeScale = 1.0; }
            if (speedTimer > 0) { speedTimer -= dt; if (speedTimer <= 0) speedScale = 1.0; }
            if (stickyTimer > 0) stickyTimer -= dt;
        }

        void onRoundEnd() {
            // soften effects slightly between rounds
            sizeTimer *= 0.5;
            speedTimer *= 0.5;
            stickyTimer *= 0.5;
        }

        private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    }

    /** Ball with optional stick/ghost/speed effects */
    static class Ball {
        double x, y, r;
        double vx, vy;
        double colorPhase = Math.random() * Math.PI * 2;
        double speedScale = 1.0;
        double speedTimer = 0.0;
        double ghostTimer = 0.0;

        // Stick-to-paddle implementation
        boolean stuck = false;
        Paddle stuckTo = null;
        double stickOffsetY = 0.0; // relative offset within paddle
        double releaseCooldown = 0.0;

        Ball(double x, double y, double r) { this.x = x; this.y = y; this.r = r; }

        void update(double dt) {
            colorPhase += dt * 2.0;
            if (stuck && stuckTo != null) {
                // Follow paddle
                double hh = stuckTo.h * stuckTo.sizeScale;
                y = stuckTo.y + hh * stickOffsetY;
                x = (stuckTo.x < GamePanel.WIDTH/2) ? (stuckTo.x + stuckTo.w + r + 1) : (stuckTo.x - r - 1);
                releaseCooldown -= dt;
                // Release on player action (nudge keys) or timeout
                // For left paddle we check A/D, for right LEFT/RIGHT
                boolean release = false;
                if (stuckTo.x < GamePanel.WIDTH/2) {
                    release = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() != null &&
                            (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK) == Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK));
                    // Above dummy read to jitter; we'll also auto-release after cooldown
                }
                if (releaseCooldown <= 0) {
                    unstuckWithImpulse();
                }
                return;
            }

            // Normal motion
            double s = speedScale;
            x += vx * dt * s;
            y += vy * dt * s;

            if (speedTimer > 0) { speedTimer -= dt; if (speedTimer <= 0) speedScale = 1.0; }
        }

        void draw(Graphics2D g2, Theme theme) {
            float alpha = ghostTimer > 0 ? 0.5f : 1f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            Ellipse2D.Double c = new Ellipse2D.Double(x - r, y - r, r*2, r*2);
            g2.setColor(theme.ballColor(colorPhase));
            g2.fill(c);
            g2.setColor(theme.ballEdge);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(c);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        Rectangle2D bounds() { return new Rectangle2D.Double(x - r, y - r, r*2, r*2); }

        void scaleSpeed(double factor, double seconds) {
            speedScale = Math.max(0.5, Math.min(1.8, speedScale * factor));
            speedTimer = Math.max(speedTimer, seconds);
        }

        void stickTo(Paddle p, double seconds) {
            if (p.stickyTimer <= 0) return;
            stuck = true;
            stuckTo = p;
            double hh = p.h * p.sizeScale;
            stickOffsetY = (y - p.y) / hh; // 0..1
            releaseCooldown = seconds;
            // Freeze ball; small nudge perpendicular to paddle on release
            vx = 0; vy = 0;
        }

        void unstuckWithImpulse() {
            if (!stuck || stuckTo == null) return;
            stuck = false;
            double angle = Math.toRadians( (stuckTo.x < GamePanel.WIDTH/2) ? (rngAngle(20, 70)) : (180 - rngAngle(20, 70)) );
            double speed = 7.0;
            vx = speed * Math.cos(angle);
            vy = speed * Math.sin(angle);
            if (stuckTo.x > GamePanel.WIDTH/2) vx *= -1; // right paddle shoot left
            stuckTo = null;
        }

        private double rngAngle(int min, int max) {
            return min + Math.random() * (max - min);
        }
    }

    /** Particle for trails and bursts */
    static class Particle {
        double x, y, vx, vy, life, r;
        Color color;

        static Particle fromBall(Ball b, Theme theme) {
            Particle p = new Particle();
            p.x = b.x; p.y = b.y; p.r = 2 + Math.random() * 2;
            double ang = Math.random() * Math.PI * 2;
            double sp = 14 + Math.random() * 26;
            p.vx = Math.cos(ang) * sp; p.vy = Math.sin(ang) * sp;
            p.life = 0.35 + Math.random() * 0.25;
            p.color = theme.trailColor();
            return p;
        }

        static Particle burst(double x, double y, Theme theme) {
            Particle p = new Particle();
            p.x = x; p.y = y; p.r = 2 + Math.random() * 3;
            double ang = Math.random() * Math.PI * 2;
            double sp = 60 + Math.random() * 80;
            p.vx = Math.cos(ang) * sp; p.vy = Math.sin(ang) * sp;
            p.life = 0.6 + Math.random() * 0.3;
            p.color = theme.accent;
            return p;
        }

        void update(double dt) {
            life -= dt;
            x += vx * dt; y += vy * dt;
            vx *= 0.96; vy *= 0.96;
        }

        void draw(Graphics2D g2) {
            if (life <= 0) return;
            float a = (float)Math.max(0, Math.min(1, life * 1.2));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g2.setColor(color);
            g2.fill(new Ellipse2D.Double(x - r, y - r, r*2, r*2));
            g2.setComposite(AlphaComposite.SrcOver);
        }
    }

    /** Power-up pickup */
    static class PowerUp {
        enum Type { GROW_PADDLE, SHRINK_OPPONENT, BALL_SLOW, BALL_FAST, STICKY_PADDLE, GHOST_BALL }
        double x, y; int size; Type type; double life = 10.0;
        Theme theme;

        PowerUp(double x, double y, int size, Type type, Theme theme) {
            this.x = x; this.y = y; this.size = size; this.type = type; this.theme = theme;
        }

        void update(double dt) { life -= dt; }

        Rectangle2D bounds() { return new Rectangle2D.Double(x, y, size, size); }

        void draw(Graphics2D g2) {
            Shape s = new RoundRectangle2D.Double(x, y, size, size, 10, 10);
            g2.setColor(theme.puFill(type));
            g2.fill(s);
            g2.setColor(theme.puEdge);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(s);
            g2.setColor(Color.WHITE);
            g2.setFont(theme.uiFontSmall());
            String label = icon(type);
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(label);
            g2.drawString(label, (int)(x + size/2 - w/2), (int)(y + size/2 + fm.getAscent()/2 - 2));
        }

        private String icon(Type t) {
            switch (t) {
                case GROW_PADDLE: return "+H";
                case SHRINK_OPPONENT: return "-O";
                case BALL_SLOW: return "⏱";
                case BALL_FAST: return "⚡";
                case STICKY_PADDLE: return "☍";
                case GHOST_BALL: return "👻";
            }
            return "?";
        }
    }

    /** Visual theme / palette */
    static class Theme {
        Color bg = new Color(16, 18, 25);
        Color bg2 = new Color(10, 10, 16);
        Color fieldLines = new Color(255, 255, 255, 100);
        Color paddleFill = new Color(230, 230, 240);
        Color paddleEdge = new Color(200, 200, 210);
        Color ballEdge = new Color(240, 240, 255);
        Color uiText = new Color(240, 240, 240);
        Color accent = new Color(80, 200, 255);
        Color shadow = new Color(0, 0, 0, 180);
        Color puEdge = new Color(240, 240, 255);

        private int paletteIndex = 0;

        void nextPalette() {
            paletteIndex = (paletteIndex + 1) % 4;
            switch (paletteIndex) {
                case 0:
                    bg = new Color(16, 18, 25); bg2 = new Color(10, 10, 16);
                    paddleFill = new Color(230, 230, 240); paddleEdge = new Color(200, 200, 210);
                    uiText = new Color(240, 240, 240); accent = new Color(80, 200, 255);
                    fieldLines = new Color(255, 255, 255, 100); ballEdge = new Color(240, 240, 255); puEdge = new Color(240, 240, 255);
                    break;
                case 1:
                    bg = new Color(24, 8, 32); bg2 = new Color(10, 6, 20);
                    paddleFill = new Color(255, 200, 230); paddleEdge = new Color(250, 160, 210);
                    uiText = new Color(255, 235, 250); accent = new Color(255, 120, 180);
                    fieldLines = new Color(255, 200, 230, 100); ballEdge = new Color(255, 220, 240); puEdge = new Color(255, 220, 240);
                    break;
                case 2:
                    bg = new Color(6, 26, 22); bg2 = new Color(2, 14, 12);
                    paddleFill = new Color(220, 255, 235); paddleEdge = new Color(180, 240, 210);
                    uiText = new Color(220, 255, 240); accent = new Color(120, 255, 200);
                    fieldLines = new Color(200, 255, 230, 100); ballEdge = new Color(220, 255, 240); puEdge = new Color(220, 255, 240);
                    break;
                case 3:
                    bg = new Color(28, 28, 28); bg2 = new Color(16, 16, 16);
                    paddleFill = new Color(240, 240, 240); paddleEdge = new Color(210, 210, 210);
                    uiText = new Color(250, 250, 250); accent = new Color(255, 160, 80);
                    fieldLines = new Color(255, 255, 255, 100); ballEdge = new Color(250, 250, 250); puEdge = new Color(250, 250, 250);
                    break;
            }
        }

        Font uiFontHuge() { return new Font("SansSerif", Font.BOLD, 64); }
        Font uiFontLarge() { return new Font("SansSerif", Font.BOLD, 48); }
        Font uiFontMedium() { return new Font("SansSerif", Font.PLAIN, 18); }
        Font uiFontSmall() { return new Font("SansSerif", Font.PLAIN, 13); }

        Color ballColor(double phase) {
            float s = 0.6f;
            float b = 1.0f;
            float h = (float)((Math.sin(phase) * 0.5 + 0.5) * 0.6); // 0..0.6
            return Color.getHSBColor(h, s, b);
        }

        Color trailColor() {
            return new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 140);
        }

        Color puFill(PowerUp.Type t) {
            switch (t) {
                case GROW_PADDLE: return new Color(100, 220, 140);
                case SHRINK_OPPONENT: return new Color(220, 120, 120);
                case BALL_SLOW: return new Color(130, 130, 255);
                case BALL_FAST: return new Color(255, 180, 90);
                case STICKY_PADDLE: return new Color(240, 200, 120);
                case GHOST_BALL: return new Color(200, 200, 200);
            }
            return new Color(200, 200, 200);
        }
    }
}
