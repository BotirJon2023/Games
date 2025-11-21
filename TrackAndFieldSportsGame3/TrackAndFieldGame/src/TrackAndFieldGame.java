import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;

/**
 * Track and Field Sports - a single-file Java game with animation
 *
 * Features:
 * - Menu with multiple events: 100m Dash, 110m Hurdles, Long Jump, Javelin Throw, All-Around
 * - Animated athletes with simple physics
 * - Keyboard input rhythm mechanics for sprinting
 * - AI opponents
 * - Results, medals, and confetti celebration
 *
 * Controls (shown in-game as well):
 * - Left/Right arrows (or A/D) alternation to build speed in running events
 * - Up arrow (or W/Space) to jump over hurdles or take-off in long jump
 * - Javelin: Hold Space to build power, Up/Down to adjust throw angle, release Space to throw
 *
 * Notes:
 * - This is a teaching/demo project focusing on clarity of structure in a single file.
 * - Uses Java Swing only, no external assets required.
 */
public class TrackAndFieldGame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Track and Field Sports");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);
            GamePanel panel = new GamePanel(1024, 720);
            f.setContentPane(panel);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            panel.start();
        });
    }

    // Main game panel and loop
    static class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
        private final int width;
        private final int height;
        private final Timer timer;
        private long lastNanos;
        private boolean running = false;
        private Scene scene;
        private final Input input = new Input();
        private final Random rng = new Random();
        private final GameState gameState = new GameState();
        private BufferedImage backBuffer;
        private Graphics2D bufferG;

        public GamePanel(int width, int height) {
            this.width = width;
            this.height = height;
            setPreferredSize(new Dimension(width, height));
            setFocusable(true);
            setDoubleBuffered(true);
            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);
            timer = new Timer(1000 / 60, this);
            switchScene(new MenuScene(this));
        }

        public void start() {
            running = true;
            lastNanos = System.nanoTime();
            timer.start();
            requestFocusInWindow();
        }

        public void switchScene(Scene s) {
            if (scene != null) scene.onExit();
            scene = s;
            scene.onEnter();
        }

        public Random rng() {
            return rng;
        }

        public GameState state() {
            return gameState;
        }

        public Input input() {
            return input;
        }

        public int getGameWidth() {
            return width;
        }

        public int getGameHeight() {
            return height;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!running) return;
            long now = System.nanoTime();
            double dt = (now - lastNanos) / 1_000_000_000.0;
            dt = Math.min(dt, 0.05); // clamp
            lastNanos = now;
            if (scene != null) scene.update(dt);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Render to back buffer for stability
            if (backBuffer == null || backBuffer.getWidth() != width || backBuffer.getHeight() != height) {
                backBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                if (bufferG != null) bufferG.dispose();
                bufferG = backBuffer.createGraphics();
                bufferG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                bufferG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }
            bufferG.setColor(new Color(15, 20, 35));
            bufferG.fillRect(0, 0, width, height);
            if (scene != null) scene.render(bufferG);
            g.drawImage(backBuffer, 0, 0, null);
        }

        // Input handlers
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            input.setKey(e.getKeyCode(), true);
            if (scene != null) scene.keyPressed(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            input.setKey(e.getKeyCode(), false);
            if (scene != null) scene.keyReleased(e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (scene != null) scene.mouseClicked(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (scene != null) scene.mousePressed(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (scene != null) scene.mouseReleased(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}

        @Override
        public void mouseDragged(MouseEvent e) {
            if (scene != null) scene.mouseDragged(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (scene != null) scene.mouseMoved(e);
        }
    }

    // Game state shared across scenes
    static class GameState {
        public String playerName = "You";
        public int playerLane = 3; // lanes 1..6 for 400m oval illusions; here just used to place runners
        public Results results = new Results();
        public boolean allAround = false;
        public int currentEventIndex = 0;
        public final String[] allEvents = new String[] { "100m Dash", "110m Hurdles", "Long Jump", "Javelin Throw" };

        public void resetAllAround() {
            results = new Results();
            allAround = true;
            currentEventIndex = 0;
        }
    }

    // Results + scoring
    static class Results {
        public double hundredTime = -1;
        public double hurdlesTime = -1;
        public double longJumpDistance = -1; // meters
        public double javelinDistance = -1; // meters
        public int golds = 0, silvers = 0, bronzes = 0;

        public int totalPoints() {
            // Simple arbitrary scoring
            int points = 0;
            if (hundredTime > 0) points += Math.max(0, (int)(2000 - hundredTime * 100));
            if (hurdlesTime > 0) points += Math.max(0, (int)(2500 - hurdlesTime * 120));
            if (longJumpDistance > 0) points += (int)(longJumpDistance * 150);
            if (javelinDistance > 0) points += (int)(javelinDistance * 60);
            return points;
        }

        public void addMedal(int place) {
            if (place == 1) golds++;
            else if (place == 2) silvers++;
            else if (place == 3) bronzes++;
        }
    }

    // Input helper
    static class Input {
        private final boolean[] keys = new boolean[512];
        public boolean isDown(int keyCode) {
            int idx = clampIndex(keyCode);
            return keys[idx];
        }
        public void setKey(int keyCode, boolean down) {
            int idx = clampIndex(keyCode);
            keys[idx] = down;
        }
        private int clampIndex(int keyCode) {
            if (keyCode < 0) return 0;
            if (keyCode >= keys.length) return keys.length - 1;
            return keyCode;
        }
        public boolean anyDown(int... ks) {
            for (int k : ks) if (isDown(k)) return true;
            return false;
        }
    }

    // Scene interface
    interface Scene {
        void onEnter();
        void onExit();
        void update(double dt);
        void render(Graphics2D g);
        default void keyPressed(KeyEvent e) {}
        default void keyReleased(KeyEvent e) {}
        default void mouseClicked(MouseEvent e) {}
        default void mousePressed(MouseEvent e) {}
        default void mouseReleased(MouseEvent e) {}
        default void mouseDragged(MouseEvent e) {}
        default void mouseMoved(MouseEvent e) {}
    }

    // Menu Scene
    static class MenuScene implements Scene {
        private final GamePanel gp;
        private int sel = 0;
        private int tick = 0;
        private final String[] items = {
                "100m Dash",
                "110m Hurdles",
                "Long Jump",
                "Javelin Throw",
                "All-Around",
                "Quit"
        };
        private final Color accent = new Color(245, 197, 66);

        public MenuScene(GamePanel gp) {
            this.gp = gp;
        }

        @Override
        public void onEnter() {
            // nothing
        }

        @Override
        public void onExit() {

        }

        @Override
        public void update(double dt) {
            tick++;
        }

        @Override
        public void render(Graphics2D g) {
            int w = gp.getGameWidth();
            int h = gp.getGameHeight();

            // Background gradient
            Paint old = g.getPaint();
            GradientPaint gpnt = new GradientPaint(0, 0, new Color(18, 28, 55), 0, h, new Color(26, 72, 82));
            g.setPaint(gpnt);
            g.fillRect(0, 0, w, h);
            g.setPaint(old);

            // Title
            g.setColor(Color.WHITE);
            Font fTitle = g.getFont().deriveFont(Font.BOLD, 48f);
            g.setFont(fTitle);
            String title = "Track and Field Sports";
            drawCenteredString(g, title, w / 2, 90);

            // Subtitle
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 16f));
            drawCenteredString(g, "Arrow keys to navigate, Enter to select", w / 2, 120);
            drawCenteredString(g, "100% Java / Swing", w / 2, 140);

            // Menu Items
            int baseY = 220;
            int step = 42;
            for (int i = 0; i < items.length; i++) {
                boolean selI = (i == sel);
                g.setFont(g.getFont().deriveFont(selI ? Font.BOLD : Font.PLAIN, selI ? 28f : 22f));
                if (selI) {
                    g.setColor(accent);
                    drawCenteredString(g, "> " + items[i] + " <", w / 2, baseY + i * step);
                } else {
                    g.setColor(new Color(220, 230, 240));
                    drawCenteredString(g, items[i], w / 2, baseY + i * step);
                }
            }

            // Decorative lanes
            drawDecorativeTrack(g, w, h, tick);

            // Credits
            g.setColor(new Color(220,220,220,150));
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 12f));
            drawCenteredString(g, "Controls: Left/Right (or A/D) to run, Up/W/Space to jump, Space hold+release for throws.", w / 2, h - 24);
        }

        private void drawDecorativeTrack(Graphics2D g, int w, int h, int t) {
            int lanes = 6;
            int laneH = 14;
            int trackY = h - lanes * (laneH + 6) - 80;
            g.setColor(new Color(165, 46, 46));
            g.fillRoundRect(40, trackY - 20, w - 80, lanes * (laneH + 6) + 40, 20, 20);

            for (int i = 0; i < lanes; i++) {
                int y = trackY + i * (laneH + 6);
                g.setColor(new Color(220, 120, 100));
                g.fillRect(60, y, w - 120, laneH);
                g.setColor(Color.WHITE);
                g.fillRect(60, y - 2, w - 120, 2);
            }

            // Runners as moving dots
            for (int i = 0; i < lanes; i++) {
                double period = 3000 + i * 350;
                double prog = ((t * 16) % period) / period;
                int x = (int)(60 + prog * (w - 120 - 20));
                int y = trackY + i * (laneH + 6) + laneH / 2;
                g.setColor(new Color(255, 245, 230));
                g.fillOval(x, y - 4, 12, 12);
                g.setColor(new Color(40, 40, 40, 80));
                g.fillOval(x - 4, y + 6, 20, 8);
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) {
                sel = (sel - 1 + items.length) % items.length;
            } else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) {
                sel = (sel + 1) % items.length;
            } else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
                handleSelect();
            }
        }

        private void handleSelect() {
            String it = items[sel];
            switch (it) {
                case "100m Dash":
                    gp.switchScene(new SprintScene(gp, false));
                    break;
                case "110m Hurdles":
                    gp.switchScene(new HurdlesScene(gp));
                    break;
                case "Long Jump":
                    gp.switchScene(new LongJumpScene(gp));
                    break;
                case "Javelin Throw":
                    gp.switchScene(new JavelinScene(gp));
                    break;
                case "All-Around":
                    gp.state().resetAllAround();
                    gp.switchScene(new SprintScene(gp, true));
                    break;
                case "Quit":
                    System.exit(0);
                    break;
            }
        }

        @Override
        public void onExit() {}
    }

    // Base classes/utilities shared by events
    static class Camera {
        public double x, y;
        public double targetX, targetY;
        public double damping = 0.12;

        public void update(double dt) {
            x += (targetX - x) * Math.min(1.0, damping * 60 * dt);
            y += (targetY - y) * Math.min(1.0, damping * 60 * dt);
        }
    }

    static class Athlete {
        public String name;
        public Color jersey;
        public int lane;
        public double energy = 1.0;     // 0..1
        public double fatigue = 0.0;    // accumulates
        public double speed = 0.0;      // m/s
        public double maxSpeed = 11.0;  // m/s (player varies)
        public double accel = 6.0;      // m/s^2
        public double stamina = 1.0;    // lighten reduces max
        public double x = 0.0;          // world position or approach dist
        public double y = 0.0;
        public double stridePhase = 0.0;
        public boolean finished = false;
        public double finishTime = 0.0;

        public Athlete(String name, Color jersey, int lane) {
            this.name = name;
            this.jersey = jersey;
            this.lane = lane;
        }

        public void resetForRun() {
            energy = 1.0;
            fatigue = 0.0;
            speed = 0.0;
            finished = false;
            finishTime = 0.0;
            stridePhase = 0.0;
            x = 0.0;
        }

        public void updateStride(double dt) {
            stridePhase += speed * dt * 3.0; // higher speed = faster animation
            if (stridePhase > Math.PI * 2) stridePhase -= Math.PI * 2;
        }
    }

    static class OpponentAI {
        private final Random rng;
        private final double talent; // 0..1 scaling for capability
        private double targetPace;   // desired speed (m/s)
        public OpponentAI(Random rng, double talent) {
            this.rng = rng;
            this.talent = talent;
            this.targetPace = 8 + 4 * talent + rng.nextDouble() * 0.6;
        }
        public void updateRun(Athlete a, double dt) {
            double tPace = targetPace * (0.88 + 0.24 * Math.sin(a.stridePhase * 0.7 + talent * 5));
            // Stamina/fatigue influences
            tPace *= (0.9 + 0.1 * a.stamina);
            if (a.speed < tPace) {
                a.speed += a.accel * dt * (0.4 + 0.6 * talent);
            } else {
                a.speed -= 2.0 * dt;
            }
            // Energy drain
            a.energy -= dt * (0.25 + 0.35 * (a.speed / 11.0));
            if (a.energy < 0) {
                a.energy = 0;
                a.speed -= 2.5 * dt;
            }
            a.speed = clamp(a.speed, 0, a.maxSpeed * (0.92 + 0.08 * talent));
        }
    }

    // Rhythm running mechanic for player
    static class RhythmRunner {
        private int lastKey = -1;
        private double comboTime = 0;
        private double comboWindow = 0.4;
        private int combo = 0;
        private double bonus = 0;

        public void update(double dt) {
            comboTime += dt;
            // slight decay of bonus
            bonus = Math.max(0, bonus - dt * 0.3);
            if (comboTime > comboWindow) {
                combo = Math.max(0, combo - 1);
                comboTime = comboWindow * 0.95;
            }
        }

        public void reset() {
            lastKey = -1;
            comboTime = 0;
            combo = 0;
            bonus = 0;
        }

        public void key(int keyCode) {
            if (!isRunKey(keyCode)) return;
            if (lastKey == -1) {
                lastKey = keyCode;
                combo = 1;
                comboTime = 0;
                bonus += 0.2;
            } else {
                if (keyCode != lastKey) {
                    // Alternated correctly
                    lastKey = keyCode;
                    combo++;
                    comboTime = 0;
                    // diminishing return
                    bonus += Math.max(0.1, 0.45 - combo * 0.035);
                    bonus = Math.min(bonus, 3.0);
                } else {
                    // repeated same key - penalize slightly
                    combo = Math.max(0, combo - 1);
                    bonus = Math.max(0, bonus - 0.2);
                }
            }
        }

        public double currentBoost() {
            // Map bonus to a multiplicative speed add
            return bonus; // will be scaled externally
        }

        public int combo() { return combo; }

        private boolean isRunKey(int k) {
            return k == KeyEvent.VK_LEFT || k == KeyEvent.VK_RIGHT ||
                    k == KeyEvent.VK_A || k == KeyEvent.VK_D;
        }
    }

    // Drawing helpers
    static void drawCenteredString(Graphics2D g, String s, int cx, int y) {
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(s);
        g.drawString(s, cx - w / 2, y);
    }

    static double clamp(double v, double a, double b) {
        return Math.max(a, Math.min(b, v));
    }

    static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    static String formatTime(double sec) {
        if (sec < 0) return "--.--";
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(sec);
    }

    static String formatMeters(double m) {
        if (m < 0) return "--.-- m";
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(m) + " m";
    }

    // Track drawing utilities
    static class TrackDrawer {
        public static void drawTrack(Graphics2D g, int w, int h, double cameraX, int lanes, double worldLenPx, boolean showFinish) {
            int trackTop = 150;
            int laneH = 50;
            int marginX = 60;
            int trackH = lanes * laneH + 20;
            // Track base
            g.setColor(new Color(164, 52, 44));
            g.fillRoundRect(0, trackTop - 40, w, trackH + 60, 30, 30);

            // Track lanes
            for (int i = 0; i < lanes; i++) {
                int y = trackTop + i * laneH + 10;
                g.setColor(new Color(214, 130, 110));
                g.fillRect(0, y, w, laneH);
                g.setColor(Color.WHITE);
                g.fillRect(0, y, w, 2);
            }

            // Tick marks moving relative to cameraX
            g.setStroke(new BasicStroke(4));
            for (int i = -3; i < w / 40 + 3; i++) {
                double worldX = (int)((cameraX / 40) + i) * 40.0;
                int x = (int)(i * 40 - (cameraX % 40));
                g.setColor(new Color(255,255,255,60));
                g.drawLine(x, trackTop + 10, x, trackTop + 10 + lanes * laneH);
            }
            g.setStroke(new BasicStroke(1));

            // Finish line
            if (showFinish) {
                int finishX = (int)(marginX + worldLenPx - cameraX);
                g.setColor(new Color(255,255,255,200));
                g.fillRect(finishX - 5, trackTop, 10, lanes * laneH + 10);
                // Checker pattern
                g.setColor(Color.BLACK);
                for (int y = trackTop; y < trackTop + lanes * laneH + 10; y += 10) {
                    g.fillRect(finishX - 5, y, 5, 5);
                    g.fillRect(finishX, y + 5, 5, 5);
                }
            }
        }

        public static int laneY(int laneIndex, int baseY, int laneH) {
            return baseY + laneIndex * laneH + laneH / 2;
        }
    }

    // Athlete drawing
    static class AthleteDrawer {
        public static void drawRunner(Graphics2D g, Athlete a, double camX, int baseY, int laneH, boolean player) {
            int y = TrackDrawer.laneY(a.lane, baseY + 10, laneH);
            int screenX = (int)(a.x - camX);
            // Shadow
            g.setColor(new Color(0, 0, 0, 60));
            g.fillOval(screenX - 22, y + 10, 48, 12);

            // Body animation
            double phase = Math.sin(a.stridePhase);
            double lean = clamp(a.speed / 11.0, 0, 1);
            AffineTransform old = g.getTransform();
            g.translate(screenX, y);
            g.rotate(-0.2 * lean);
            // Torso
            g.setColor(a.jersey);
            g.fillRoundRect(-12, -26, 24, 30, 8, 8);
            // Head
            g.setColor(new Color(255, 227, 190));
            g.fillOval(-10, -46, 20, 20);
            g.setColor(Color.BLACK);
            g.drawOval(-10, -46, 20, 20);
            // Arms
            g.setStroke(new BasicStroke(4));
            g.setColor(new Color(255, 227, 190));
            int armSwing = (int)(phase * 8);
            g.drawLine(-10, -18, -20, -6 - armSwing);
            g.drawLine(10, -18, 20, -6 + armSwing);
            // Legs
            g.setStroke(new BasicStroke(6));
            int legSwing = (int)(Math.cos(a.stridePhase * 0.9) * 10);
            g.setColor(new Color(40, 40, 40));
            g.drawLine(-6, 2, -10, 12 + legSwing);
            g.drawLine(6, 2, 10, 12 - legSwing);
            // Number
            g.setColor(new Color(255, 255, 255, 220));
            g.fillRoundRect(-7, -20, 14, 14, 4, 4);
            g.setColor(new Color(30,30,30));
            g.setFont(g.getFont().deriveFont(Font.BOLD, 10f));
            String label = player ? "P" : String.valueOf(a.lane + 1);
            int w = g.getFontMetrics().stringWidth(label);
            g.drawString(label, -w/2, -9);
            g.setTransform(old);
        }
    }

    // 100m Sprint Scene
    static class SprintScene implements Scene {
        private final GamePanel gp;
        private final boolean chainedAllAround;
        private final int lanes = 6;
        private final int laneH = 65;
        private final int trackTop = 160;
        private final double meters = 100.0;
        private final double mToPx = 12.0; // scale for world
        private final double worldLenPx = meters * mToPx;
        private double time = 0;
        private boolean started = false;
        private boolean finished = false;
        private boolean countdownDone = false;
        private double countdown = 2.5;
        private final Camera cam = new Camera();

        private final Athlete player = new Athlete("You", new Color(66, 184, 131), 2);
        private final List<Athlete> rivals = new ArrayList<>();
        private final List<OpponentAI> ai = new ArrayList<>();
        private final RhythmRunner rhythm = new RhythmRunner();
        private int placement = 1;
        private double wind = 0;

        public SprintScene(GamePanel gp, boolean chainedAllAround) {
            this.gp = gp;
            this.chainedAllAround = chainedAllAround;
        }

        @Override
        public void onEnter() {
            // Setup player and AI
            player.resetForRun();
            player.maxSpeed = 11.5;
            player.accel = 7.0;
            player.stamina = 1.0;
            player.lane = gp.state().playerLane;
            rivals.clear(); ai.clear();

            Color[] jerseys = {
                    new Color(85, 158, 219),
                    new Color(209, 84, 84),
                    new Color(169, 114, 242),
                    new Color(244, 169, 61),
                    new Color(63, 205, 215),
                    new Color(221, 119, 182)
            };
            int r = 0;
            for (int i = 0; i < lanes; i++) {
                if (i == player.lane) continue;
                Athlete a = new Athlete("Rival " + (i+1), jerseys[r % jerseys.length], i);
                a.resetForRun();
                a.maxSpeed = 10.4 + gp.rng().nextDouble() * 1.1;
                a.accel = 6.2 + gp.rng().nextDouble() * 0.8;
                a.stamina = 0.9 + gp.rng().nextDouble() * 0.2;
                rivals.add(a);
                ai.add(new OpponentAI(gp.rng(), 0.55 + gp.rng().nextDouble() * 0.35));
                r++;
            }

            // Place the player at left start
            player.x = 60;

            // Align others
            for (Athlete a : rivals) a.x = 60;

            // Camera initial
            cam.x = 0;
            cam.targetX = 0;

            // Wind randomization
            wind = -1.0 + gp.rng().nextDouble() * 2.0; // -1..1 m/s effect

            rhythm.reset();
            time = 0;
            started = false;
            finished = false;
            countdownDone = false;
            countdown = 2.5;
        }

        @Override
        public void onExit() {}

        @Override
        public void update(double dt) {
            // Update camera
            cam.targetX = Math.max(0, Math.min(player.x - gp.getGameWidth() * 0.35, worldLenPx - gp.getGameWidth() * 0.55));
            cam.update(dt);

            if (!started) {
                countdown -= dt;
                if (countdown <= 0 && !countdownDone) {
                    countdownDone = true;
                }
                // Ready set go
                if (countdown <= -0.2) {
                    started = true;
                    time = 0;
                }
            } else if (!finished) {
                time += dt;
            }

            if (!finished) {
                rhythm.update(dt);

                // Player controls
                if (started) {
                    // Speed gain from rhythm; base acceleration to get off blocks
                    double boost = rhythm.currentBoost(); // 0.. ~3
                    double accel = player.accel * (0.5 + Math.min(1.2, boost * 0.6));
                    player.speed += accel * dt;

                    // Drag, stamina, wind
                    double drag = 0.6 + player.speed * 0.04;
                    player.speed -= drag * dt;
                    player.speed += wind * 0.02 * dt;

                    // Clamp and energy drain
                    player.speed = clamp(player.speed, 0, player.maxSpeed * (0.95 + 0.05 * player.stamina));
                    player.energy -= dt * (0.35 + 0.25 * (player.speed / (player.maxSpeed + 0.01)));
                    if (player.energy <= 0) {
                        player.energy = 0;
                        player.speed -= 1.5 * dt; // fade when exhausted
                    }
                }

                // AI update
                for (int i = 0; i < rivals.size(); i++) {
                    Athlete a = rivals.get(i);
                    a.updateStride(dt);
                    if (started) {
                        ai.get(i).updateRun(a, dt);
                        a.x += a.speed * mToPx * dt;
                    }
                }

                // Player motion
                player.updateStride(dt);
                if (started) {
                    player.x += player.speed * mToPx * dt;
                }

                // Finish line check
                double finishX = 60 + worldLenPx;
                if (!player.finished && player.x >= finishX) {
                    player.finished = true;
                    player.finishTime = time;
                }
                for (Athlete r : rivals) {
                    if (!r.finished && r.x >= finishX) {
                        r.finished = true;
                        r.finishTime = time;
                    }
                }

                // Determine placement at first player's finish
                if (player.finished && !finished) {
                    finished = true;
                    placement = 1;
                    for (Athlete r : rivals) {
                        if (r.finishTime <= player.finishTime && r.finished) placement++;
                    }

                    // Save results
                    gp.state().results.hundredTime = player.finishTime;
                    gp.state().results.addMedal(placement);

                    if (chainedAllAround) {
                        // Go to next event after short pause
                        nextAllAroundAfter(1.7);
                    }
                }
            }
        }

        private void nextAllAroundAfter(double sec) {
            Timer t = new Timer((int)(sec * 1000), e -> {
                // advance to hurdles
                gp.state().currentEventIndex = 1;
                gp.switchScene(new HurdlesScene(gp, true));
            });
            t.setRepeats(false);
            t.start();
        }

        @Override
        public void render(Graphics2D g) {
            int w = gp.getGameWidth();
            int h = gp.getGameHeight();

            // Sky
            g.setColor(new Color(88, 153, 206));
            g.fillRect(0, 0, w, h);

            // Stadium stands
            drawStadium(g, w, h);

            // Track
            TrackDrawer.drawTrack(g, w, h, cam.x, lanes, worldLenPx + 80, true);

            // Draw athletes
            for (Athlete a : rivals) AthleteDrawer.drawRunner(g, a, cam.x, trackTop, laneH, false);
            AthleteDrawer.drawRunner(g, player, cam.x, trackTop, laneH, true);

            // HUD
            drawHUD(g, w, h);

            // Start/Countdown text
            if (!started) {
                String txt = countdown > 1.5 ? "ON YOUR MARKS" : countdown > 0.6 ? "SET" : "GO!";
                g.setColor(Color.WHITE);
                g.setFont(g.getFont().deriveFont(Font.BOLD, 42f));
                drawCenteredString(g, txt, w / 2, 120);
            }

            // Finish overlay
            if (finished) {
                g.setColor(new Color(0,0,0,120));
                g.fillRect(w/2 - 210, 50, 420, 120);
                g.setColor(Color.WHITE);
                g.setFont(g.getFont().deriveFont(Font.BOLD, 24f));
                drawCenteredString(g, "100m Finished!", w/2, 85);
                g.setFont(g.getFont().deriveFont(Font.PLAIN, 20f));
                drawCenteredString(g, "Your time: " + formatTime(player.finishTime) + "    Place: " + placeText(placement), w/2, 115);
                g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
                drawCenteredString(g, "Press Enter to continue", w/2, 145);
            }
        }

        private String placeText(int p) {
            switch (p) {
                case 1: return "1st";
                case 2: return "2nd";
                case 3: return "3rd";
                default: return p + "th";
            }
        }

        private void drawHUD(Graphics2D g, int w, int h) {
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(20, 20, 320, 90, 12, 12);
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 18f));
            g.drawString("100m Dash", 30, 46);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
            g.drawString("Time: " + formatTime(time), 30, 68);
            g.drawString("Wind: " + new DecimalFormat("+0.0;-0.0").format(wind) + " m/s", 180, 68);
            g.drawString("Energy", 30, 88);
            g.setColor(new Color(255, 255, 255, 100));
            g.fillRect(85, 78, 200, 12);
            g.setColor(new Color(66, 184, 131));
            g.fillRect(85, 78, (int)(200 * clamp(player.energy, 0, 1)), 12);

            // Controls hint
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(w - 320, 20, 300, 72, 12, 12);
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
            g.drawString("Controls:", w - 310, 42);
            g.drawString("Alternate Left/Right (or A/D) to run faster", w - 310, 62);
            g.drawString("Try to keep a steady rhythm!", w - 310, 80);

            // Progress bar
            int barW = w - 120;
            int barX = 60;
            int barY = h - 50;
            g.setColor(new Color(0,0,0,120));
            g.fillRoundRect(barX, barY, barW, 10, 8, 8);
            g.setColor(new Color(250, 234, 98));
            double prog = clamp((player.x - 60) / worldLenPx, 0, 1);
            g.fillRoundRect(barX, barY, (int)(barW * prog), 10, 8, 8);
        }

        private void drawStadium(Graphics2D g, int w, int h) {
            int base = 130;
            // stands
            g.setColor(new Color(40, 40, 60));
            g.fillRect(0, 0, w, base);
            // rows
            for (int i = 0; i < 10; i++) {
                int y = 20 + i * 10;
                g.setColor(new Color(60 + i*8, 60 + i*8, 90 + i*10));
                g.fillRect(0, y, w, 8);
            }
            // crowd dots
            Random r = new Random(12345);
            for (int i = 0; i < 400; i++) {
                int x = r.nextInt(w);
                int y = 15 + r.nextInt(base - 30);
                g.setColor(new Color(150 + r.nextInt(105), 120 + r.nextInt(135), 120 + r.nextInt(135), 120));
                g.fillRect(x, y, 2, 2);
            }
            // banners
            g.setColor(new Color(222, 222, 240));
            for (int i = 0; i < 6; i++) {
                int wB = 120;
                int x = 30 + i * (wB + 20);
                g.fillRoundRect(x, base - 30, wB, 16, 8, 8);
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (k == KeyEvent.VK_ESCAPE) {
                gp.switchScene(new MenuScene(gp));
                return;
            }
            if (!finished) {
                if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_A || k == KeyEvent.VK_D) {
                    if (countdownDone) rhythm.key(k);
                }
            } else {
                if (k == KeyEvent.VK_ENTER) {
                    if (chainedAllAround) {
                        gp.switchScene(new HurdlesScene(gp, true));
                    } else {
                        gp.switchScene(new MenuScene(gp));
                    }
                }
            }
        }
    }

    // Hurdles Scene
    static class HurdlesScene implements Scene {
        private final GamePanel gp;
        private final boolean chainedAllAround;
        private final int lanes = 6;
        private final int laneH = 65;
        private final int trackTop = 160;
        private final double meters = 110.0;
        private final double mToPx = 12.0; // scale
        private final double worldLenPx = meters * mToPx;
        private final List<Double> hurdlePositions = new ArrayList<>();
        private double time = 0;
        private boolean started = false;
        private boolean finished = false;
        private boolean countdownDone = false;
        private double countdown = 2.5;
        private final Camera cam = new Camera();

        private final Athlete player = new Athlete("You", new Color(66, 184, 131), 2);
        private final List<Athlete> rivals = new ArrayList<>();
        private final List<OpponentAI> ai = new ArrayList<>();
        private final RhythmRunner rhythm = new RhythmRunner();
        private final List<Hurdle> hurdles = new ArrayList<>();
        private int placement = 1;
        private double wind = 0;
        private boolean jumpHeld = false;

        static class Hurdle {
            double x;    // world x
            boolean knocked = false;
        }

        public HurdlesScene(GamePanel gp) { this(gp, false); }
        public HurdlesScene(GamePanel gp, boolean chainedAllAround) {
            this.gp = gp;
            this.chainedAllAround = chainedAllAround;
        }

        @Override
        public void onEnter() {
            // Setup hurdles every 9.14 m after 13.72 m, 10 hurdles total (classic 110m hurdles)
            hurdlePositions.clear();
            hurdles.clear();
            double start = 13.72;
            double spacing = 9.14;
            for (int i = 0; i < 10; i++) {
                double m = start + i * spacing;
                double px = 60 + m * mToPx;
                Hurdle h = new Hurdle();
                h.x = px;
                hurdles.add(h);
                hurdlePositions.add(px);
            }

            // Setup player and AI
            player.resetForRun();
            player.maxSpeed = 10.8;
            player.accel = 7.0;
            player.stamina = 1.0;
            player.lane = gp.state().playerLane;

            rivals.clear(); ai.clear();
            Color[] jerseys = {
                    new Color(85, 158, 219),
                    new Color(209, 84, 84),
                    new Color(169, 114, 242),
                    new Color(244, 169, 61),
                    new Color(63, 205, 215),
                    new Color(221, 119, 182)
            };
            int r = 0;
            for (int i = 0; i < lanes; i++) {
                if (i == player.lane) continue;
                Athlete a = new Athlete("Rival " + (i+1), jerseys[r % jerseys.length], i);
                a.resetForRun();
                a.maxSpeed = 10.2 + gp.rng().nextDouble() * 0.9;
                a.accel = 6.0 + gp.rng().nextDouble() * 0.6;
                a.stamina = 0.9 + gp.rng().nextDouble() * 0.2;
                rivals.add(a);
                ai.add(new OpponentAI(gp.rng(), 0.55 + gp.rng().nextDouble() * 0.35));
                r++;
            }

            // Position start
            player.x = 60;
            for (Athlete a : rivals) a.x = 60;

            cam.x = 0;
            cam.targetX = 0;

            wind = -0.8 + gp.rng().nextDouble() * 1.6;
            rhythm.reset();
            time = 0;
            started = false;
            finished = false;
            countdownDone = false;
            countdown = 2.5;
            jumpHeld = false;
        }

        @Override
        public void onExit() {}

        @Override
        public void update(double dt) {
            cam.targetX = Math.max(0, Math.min(player.x - gp.getGameWidth() * 0.35, worldLenPx - gp.getGameWidth() * 0.55));
            cam.update(dt);

            if (!started) {
                countdown -= dt;
                if (countdown <= 0 && !countdownDone) {
                    countdownDone = true;
                }
                if (countdown <= -0.2) {
                    started = true;
                    time = 0;
                }
            } else if (!finished) {
                time += dt;
            }

            if (!finished) {
                rhythm.update(dt);

                // Player controls
                if (started) {
                    double boost = rhythm.currentBoost(); // from alternation
                    double accel = player.accel * (0.45 + Math.min(1.1, boost * 0.6));
                    player.speed += accel * dt;
                    double drag = 0.6 + player.speed * 0.05;
                    player.speed -= drag * dt;
                    player.speed += wind * 0.02 * dt;
                    player.speed = clamp(player.speed, 0, player.maxSpeed);

                    player.energy -= dt * (0.35 + 0.25 * (player.speed / (player.maxSpeed + 0.01)));
                    if (player.energy <= 0) {
                        player.energy = 0;
                        player.speed -= 1.8 * dt;
                    }

                    // Jump over hurdles: if near a hurdle, need to press up to leap
                    Hurdle nextH = nextHurdleAhead(player.x + 15);
                    if (nextH != null) {
                        double dist = nextH.x - player.x;
                        // "optimal" takeoff if within 32-60 px
                        if (jumpHeld) {
                            if (dist < 16 && !nextH.knocked) {
                                // overshot takeoff; likely hit hurdle
                            }
                        }
                        // If at the hurdle and not currently jumping, collision
                    }
                }

                // Simple vertical bob to represent jump over hurdle
                // We'll track pseudoY offset for player's body. Use stridePhase to approximate.
                player.updateStride(dt);

                // AI update and hurdle collisions
                for (int i = 0; i < rivals.size(); i++) {
                    Athlete a = rivals.get(i);
                    a.updateStride(dt);
                    if (started) {
                        ai.get(i).updateRun(a, dt);
                        // AI jump logic: detect hurdle and slow or adjust speed; for simplicity,
                        // reduce speed slightly near hurdle to simulate clearing
                        Hurdle h = nextHurdleAhead(a.x + 10);
                        if (h != null) {
                            double d = h.x - a.x;
                            if (d < 50 && !h.knocked) {
                                a.speed *= 0.99;
                            }
                        }
                        a.x += a.speed * mToPx * dt;
                        // AI hurdle collision chance if too fast
                        Hurdle h = hurdleAt(a.x);
                        if (h != null && !h.knocked) {
                            if (gp.rng().nextDouble() < 0.05) {
                                h.knocked = true;
                                a.speed *= 0.6;
                            } else {
                                // "jumped" ok
                            }
                        }
                    }
                }

                // Player motion and hurdle collision
                if (started) {
                    player.x += player.speed * mToPx * dt;

                    // If we are pressing jump near the hurdle, we clear it and gain slight speed; else collision and time penalty
                    Hurdle hit = hurdleAt(player.x);
                    if (hit != null && !hit.knocked) {
                        if (jumpHeld) {
                            // Clear hurdle: small speed bump
                            player.speed *= 0.98;
                            hit.knocked = true; // mark as already "taken" to avoid multiple hits
                        } else {
                            // Hit hurdle
                            hit.knocked = true;
                            player.speed *= 0.55;
                            player.energy = Math.max(0, player.energy - 0.12);
                        }
                    }
                }

                double finishX = 60 + worldLenPx;
                if (!player.finished && player.x >= finishX) {
                    player.finished = true;
                    player.finishTime = time;
                }
                for (Athlete r : rivals) {
                    if (!r.finished && r.x >= finishX) {
                        r.finished = true;
                        r.finishTime = time;
                    }
                }
                if (player.finished && !finished) {
                    finished = true;
                    placement = 1;
                    for (Athlete r : rivals) {
                        if (r.finishTime <= player.finishTime && r.finished) placement++;
                    }
                    gp.state().results.hurdlesTime = player.finishTime;
                    gp.state().results.addMedal(placement);

                    if (chainedAllAround) nextAllAroundAfter(1.7);
                }
            }
        }

        private void nextAllAroundAfter(double sec) {
            Timer t = new Timer((int)(sec * 1000), e -> {
                gp.state().currentEventIndex = 2;
                gp.switchScene(new LongJumpScene(gp, true));
            });
            t.setRepeats(false);
            t.start();
        }

        private Hurdle nextHurdleAhead(double x) {
            for (Hurdle h : hurdles) {
                if (!h.knocked && h.x > x) return h;
            }
            return null;
        }

        private Hurdle hurdleAt(double x) {
            for (Hurdle h : hurdles) {
                if (!h.knocked && Math.abs(h.x - x) < 8) return h;
            }
            return null;
        }

        @Override
        public void render(Graphics2D g) {
            int w = gp.getGameWidth();
            int h = gp.getGameHeight();

            // Sky + stadium
            g.setColor(new Color(88, 153, 206));
            g.fillRect(0, 0, w, h);
            drawStadium(g, w, h);

            // Track
            TrackDrawer.drawTrack(g, w, h, cam.x, lanes, worldLenPx + 80, true);

            // Hurdles
            drawHurdles(g);

            // Runners
            for (Athlete a : rivals) AthleteDrawer.drawRunner(g, a, cam.x, trackTop, laneH, false);
            AthleteDrawer.drawRunner(g, player, cam.x, trackTop, laneH, true);

            // HUD
            drawHUD(g, w, h);

            if (!started) {
                String txt = countdown > 1.5 ? "ON YOUR MARKS" : countdown > 0.6 ? "SET" : "GO!";
                g.setColor(Color.WHITE);
                g.setFont(g.getFont().deriveFont(Font.BOLD, 42f));
                drawCenteredString(g, txt, w / 2, 120);
            }

            if (finished) {
                g.setColor(new Color(0,0,0,120));
                g.fillRect(w/2 - 210, 50, 420, 120);
                g.setColor(Color.WHITE);
                g.setFont(g.getFont().deriveFont(Font.BOLD, 24f));
                drawCenteredString(g, "110m Hurdles Finished!", w/2, 85);
                g.setFont(g.getFont().deriveFont(Font.PLAIN, 20f));
                drawCenteredString(g, "Your time: " + formatTime(player.finishTime) + "    Place: " + placeText(placement), w/2, 115);
                g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
                drawCenteredString(g, "Press Enter to continue", w/2, 145);
            }
        }

        private void drawHurdles(Graphics2D g) {
            for (Hurdle h : hurdles) {
                int x = (int)(h.x - cam.x);
                int yTop = trackTop + 10 + player.lane * laneH; // draw hurdles on every lane visually
                for (int lane = 0; lane < lanes; lane++) {
                    int y = trackTop + 10 + lane * laneH + laneH/2;
                    g.setColor(new Color(230, 230, 230));
                    g.fillRect(x - 2, y - 24, 4, 24);
                    g.fillRect(x - 14, y - 24, 28, 6);
                    if (h.knocked) {
                        g.setColor(new Color(250, 100, 100, 150));
                        g.fillRect(x - 16, y - 24, 32, 6);
                    }
                }
            }
        }

        private void drawHUD(Graphics2D g, int w, int h) {
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(20, 20, 360, 90, 12, 12);
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 18f));
            g.drawString("110m Hurdles", 30, 46);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
            g.drawString("Time: " + formatTime(time), 30, 68);
            g.drawString("Wind: " + new DecimalFormat("+0.0;-0.0").format(wind) + " m/s", 180, 68);
            g.drawString("Energy", 30, 88);
            g.setColor(new Color(255, 255, 255, 100));
            g.fillRect(85, 78, 200, 12);
            g.setColor(new Color(66, 184, 131));
            g.fillRect(85, 78, (int)(200 * clamp(player.energy, 0, 1)), 12);

            // Controls hint
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(w - 380, 20, 360, 72, 12, 12);
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
            g.drawString("Controls:", w - 370, 42);
            g.drawString("Alternate Left/Right (or A/D) to run", w - 370, 62);
            g.drawString("Up/W/Space to jump over hurdles", w - 370, 80);

            // Progress bar
            int barW = w - 120;
            int barX = 60;
            int barY = h - 50;
            g.setColor(new Color(0,0,0,120));
            g.fillRoundRect(barX, barY, barW, 10, 8, 8);
            g.setColor(new Color(250, 234, 98));
            double prog = clamp((player.x - 60) / worldLenPx, 0, 1);
            g.fillRoundRect(barX, barY, (int)(barW * prog), 10, 8, 8);
        }

        private String placeText(int p) {
            switch (p) {
                case 1: return "1st";
                case 2: return "2nd";
                case 3: return "3rd";
                default: return p + "th";
            }
        }

        private void drawStadium(Graphics2D g, int w, int h) {
            int base = 130;
            g.setColor(new Color(40, 40, 60));
            g.fillRect(0, 0, w, base);
            for (int i = 0; i < 10; i++) {
                int y = 20 + i * 10;
                g.setColor(new Color(60 + i*8, 60 + i*8, 90 + i*10));
                g.fillRect(0, y, w, 8);
            }
            Random r = new Random(54321);
            for (int i = 0; i < 400; i++) {
                int x = r.nextInt(w);
                int y = 15 + r.nextInt(base - 30);
                g.setColor(new Color(150 + r.nextInt(105), 120 + r.nextInt(135), 120 + r.nextInt(135), 120));
                g.fillRect(x, y, 2, 2);
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (k == KeyEvent.VK_ESCAPE) {
                gp.switchScene(new MenuScene(gp));
                return;
            }
            if (!finished) {
                if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_A || k == KeyEvent.VK_D) {
                    if (countdownDone) rhythm.key(k);
                }
                if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W || k == KeyEvent.VK_SPACE) {
                    jumpHeld = true;
                }
            } else {
                if (k == KeyEvent.VK_ENTER) {
                    if (chainedAllAround) {
                        gp.switchScene(new LongJumpScene(gp, true));
                    } else {
                        gp.switchScene(new MenuScene(gp));
                    }
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int k = e.getKeyCode();
            if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W || k == KeyEvent.VK_SPACE) {
                jumpHeld = false;
            }
        }
    }

    // Long Jump Scene
    static class LongJumpScene implements Scene {
        private final GamePanel gp;
        private final boolean chainedAllAround;
        private final Camera cam = new Camera();

        private final double mToPx = 28.0; // landing pit scale
        private final double approachLen = 45.0; // meters approach length
        private final double worldLenPx = approachLen * mToPx + 600;

        private final Athlete player = new Athlete("You", new Color(66, 184, 131), 0);
        private final List<Athlete> rivals = new ArrayList<>();
        private final RhythmRunner rhythm = new RhythmRunner();
        private double time = 0;
        private boolean started = false;
        private boolean finished = false;
        private boolean countdownDone = false;
        private double countdown = 1.3;

        // Jump physics
        private boolean inAir = false;
        private double vx = 0;
        private double vy = 0;
        private double gravity = 9.81 * mToPx; // px/s^2
        private double takeoffX; // board position
        private boolean fouled = false;
        private double jumpDistance = -1;

        // board/pit positions
        private double boardX;
        private double pitStartX;
        private double pitEndX;

        // AI results
        private final List<Double> rivalJumps = new ArrayList<>();
        private int placement = 1;

        public LongJumpScene(GamePanel gp) { this(gp, false); }
        public LongJumpScene(GamePanel gp, boolean chainedAllAround) {
            this.gp = gp;
            this.chainedAllAround = chainedAllAround;
        }

        @Override
        public void onEnter() {
            // Setup geometry: board at 1m before pit start
            boardX = 80 + 2.0 * mToPx + approachLen * 0.4 * mToPx; // place mid
            pitStartX = boardX + 1.0 * mToPx;
            pitEndX = pitStartX + 10.0 * mToPx; // 10 m sand pit
            player.x = 80;
            player.y = gp.getGameHeight() - 220;

            player.speed = 0;
            player.maxSpeed = 10.8;
            player.accel = 7.0;
            player.energy = 1.0;
            inAir = false;
            fouled = false;
            jumpDistance = -1;

            rivals.clear();
            rivalJumps.clear();
            Random r = gp.rng();
            for (int i = 0; i < 5; i++) {
                double perf = 6.5 + r.nextDouble() * 2.0; // 6.5m to 8.5m
                rivalJumps.add(perf);
            }

            cam.x = 0; cam.targetX = 0;
            rhythm.reset();
            time = 0; started = false; finished = false; countdownDone = false; countdown = 1.3;
        }

        @Override
        public void onExit() {}

        @Override
        public void update(double dt) {
            cam.targetX = Math.max(0, Math.min(player.x - gp.getGameWidth() * 0.35, worldLenPx - gp.getGameWidth() * 0.55));
            cam.update(dt);

            if (!started) {
                countdown -= dt;
                if (countdown <= 0 && !countdownDone) countdownDone = true;
                if (countdown <= -0.2) {
                    started = true;
                    time = 0;
                }
                return;
            }

            if (finished) return;

            time += dt;
            rhythm.update(dt);

            if (!inAir) {
                // Approach run: alternation increases speed
                double boost = rhythm.currentBoost();
                double accel = player.accel * (0.45 + Math.min(1.1, boost * 0.6));
                player.speed += accel * dt;
                double drag = 0.6 + player.speed * 0.05;
                player.speed -= drag * dt;
                player.speed = clamp(player.speed, 0, player.maxSpeed);
                player.energy -= dt * (0.2 + 0.2 * (player.speed / (player.maxSpeed + 0.01)));
                player.x += player.speed * 10.0 * dt;
                player.updateStride(dt);

                // Auto finish if past end
                if (player.x > pitEndX + 200) {
                    finished = true;
                    if (chainedAllAround) nextAllAroundAfter(1.7);
                }
            } else {
                // Flight
                vy += gravity * dt;
                player.x += vx * dt;
                player.y += vy * dt;

                // Land condition
                double groundY = gp.getGameHeight() - 220;
                if (player.y >= groundY) {
                    player.y = groundY;
                    inAir = false;
                    finished = true;
                    if (!fouled) {
                        double landingX = player.x;
                        jumpDistance = (landingX - boardX) / mToPx;
                        jumpDistance = Math.max(0, jumpDistance);
                    } else {
                        jumpDistance = 0.0;
                    }
                    // Determine placement
                    placement = 1;
                    for (double r : rivalJumps) {
                        if (r >= jumpDistance) placement++;
                    }
                    gp.state().results.longJumpDistance = jumpDistance;
                    gp.state().results.addMedal(placement);

                    if (chainedAllAround) nextAllAroundAfter(1.7);
                }
            }
        }

        private void nextAllAroundAfter(double sec) {
            Timer t = new Timer((int)(sec * 1000), e -> {
                gp.state().currentEventIndex = 3;
                gp.switchScene(new JavelinScene(gp, true));
            });
            t.setRepeats(false);
            t.start();
        }

        @Override
        public void render(Graphics2D g) {
            int w = gp.getGameWidth();
            int h = gp.getGameHeight();
            // Sky
            g.setColor(new Color(196, 231, 255));
            g.fillRect(0, 0, w, h);

            drawRunwayAndPit(g);

            // Player
            drawLongJumper(g, player, cam.x);

            // HUD
            drawHUD(g, w, h);

            if (!started) {
                String txt = countdown > 0.6 ? "READY" : "GO!";
                g.setColor(new Color(30,30,30));
                g.setFont(g.getFont().deriveFont(Font.BOLD, 42f));
                drawCenteredString(g, txt, w / 2, 120);
            }

            if (finished) {
                g.setColor(new Color(0,0,0,140));
                g.fillRect(w/2 - 240, 50, 480, 130);
                g.setColor(Color.WHITE);
                g.setFont(g.getFont().deriveFont(Font.BOLD, 24f));
                drawCenteredString(g, "Long Jump Result", w/2, 85);
                g.setFont(g.getFont().deriveFont(Font.PLAIN, 20f));
                drawCenteredString(g, "Your jump: " + formatMeters(jumpDistance) + (fouled ? " (FOUL)" : ""), w/2, 115);
                drawCenteredString(g, "Place: " + placeText(placement) + "   Best Rival: " + formatMeters(bestOf(rivalJumps)), w/2, 140);
                g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
                drawCenteredString(g, "Press Enter to continue", w/2, 164);
            }
        }

        private double bestOf(List<Double> list) {
            double b = 0;
            for (double v : list) b = Math.max(b, v);
            return b;
        }

        private String placeText(int p) {
            switch (p) {
                case 1: return "1st";
                case 2: return "2nd";
                case 3: return "3rd";
                default: return p + "th";
            }
        }

        private void drawHUD(Graphics2D g, int w, int h) {
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(20, 20, 380, 90, 12, 12);
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 18f));
            g.drawString("Long Jump", 30, 46);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
            g.drawString("Approach speed: " + new DecimalFormat("0.0").format(player.speed) + " m/s", 30, 68);
            g.drawString("Angle: press Up/W/Space at board", 30, 88);

            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(w - 420, 20, 400, 72, 12, 12);
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
            g.drawString("Controls:", w - 410, 42);
            g.drawString("Alternate Left/Right (or A/D) to sprint", w - 410, 62);
            g.drawString("Press Up/W/Space near board to jump", w - 410, 80);
        }

        private void drawRunwayAndPit(Graphics2D g) {
            int h = gp.getGameHeight();
            int groundY = h - 200;

            // grass
            g.setColor(new Color(94, 167, 91));
            g.fillRect(0, 0, gp.getGameWidth(), h);

            // runway
            g.setColor(new Color(153, 86, 48));
            g.fillRect(0, groundY - 40, gp.getGameWidth(), 120);
            g.setColor(new Color(221, 163, 120));
            g.fillRect(0, groundY - 20, gp.getGameWidth(), 80);

            // board
            int boardScreenX = (int)(boardX - cam.x);
            g.setColor(new Color(245,245,245));
            g.fillRect(boardScreenX - 10, groundY - 20, 20, 6);
            g.setColor(Color.BLACK);
            g.drawRect(boardScreenX - 10, groundY - 20, 20, 6);

            // pit
            int pitS = (int)(pitStartX - cam.x);
            int pitE = (int)(pitEndX - cam.x);
            g.setColor(new Color(214, 186, 124));
            g.fillRect(pitS, groundY - 20, pitE - pitS, 60);
            // marks
            g.setColor(new Color(255,255,255,150));
            for (int i = 0; i < 12; i++) {
                int x = pitS + i * (int)(1.0 * mToPx);
                g.fillRect(x, groundY + 24, 2, 16);
            }
        }

        private void drawLongJumper(Graphics2D g, Athlete a, double camX) {
            int screenX = (int)(a.x - camX);
            int y = (int)a.y;

            // Shadow
            g.setColor(new Color(0,0,0,60));
            g.fillOval(screenX - 20, y + 16, 44, 12);

            AffineTransform old = g.getTransform();
            g.translate(screenX, y);
            double phase = Math.sin(a.stridePhase);
            if (!inAir) {
                // running pose
                g.setColor(a.jersey);
                g.fillRoundRect(-12, -26, 24, 30, 8, 8);
                g.setColor(new Color(255, 227, 190));
                g.fillOval(-10, -46, 20, 20);
                g.setColor(Color.BLACK);
                g.drawOval(-10, -46, 20, 20);
                g.setStroke(new BasicStroke(4));
                g.setColor(new Color(255, 227, 190));
                int armSwing = (int)(phase * 8);
                g.drawLine(-10, -18, -20, -6 - armSwing);
                g.drawLine(10, -18, 20, -6 + armSwing);
                g.setStroke(new BasicStroke(6));
                int legSwing = (int)(Math.cos(a.stridePhase * 0.9) * 10);
                g.setColor(new Color(40, 40, 40));
                g.drawLine(-6, 2, -10, 12 + legSwing);
                g.drawLine(6, 2, 10, 12 - legSwing);
            } else {
                // flight pose
                g.rotate(-Math.atan2(vy, vx));
                g.setColor(a.jersey);
                g.fillRoundRect(-14, -20, 28, 24, 8, 8);
                g.setColor(new Color(255, 227, 190));
                g.fillOval(-10, -38, 20, 20);
                g.setColor(Color.BLACK);
                g.drawOval(-10, -38, 20, 20);
                g.setStroke(new BasicStroke(4));
                g.setColor(new Color(255, 227, 190));
                g.drawLine(-8, -12, -24, -2);
                g.drawLine(8, -12, 24, -2);
                g.setStroke(new BasicStroke(6));
                g.setColor(new Color(40, 40, 40));
                g.drawLine(-6, 6, -20, 16);
                g.drawLine(6, 6, 20, 16);
            }
            g.setTransform(old);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (k == KeyEvent.VK_ESCAPE) {
                gp.switchScene(new MenuScene(gp));
                return;
            }
            if (!finished) {
                if (!inAir && (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_A || k == KeyEvent.VK_D)) {
                    if (countdownDone) rhythm.key(k);
                }
                if (!inAir && (k == KeyEvent.VK_UP || k == KeyEvent.VK_W || k == KeyEvent.VK_SPACE)) {
                    // Check if near the board
                    double dist = Math.abs(player.x - boardX);
                    if (dist < 40) {
                        // Takeoff
                        inAir = true;
                        takeoffX = player.x;
                        double speedPx = player.speed * 10.0;
                        double angle = Math.toRadians(20 + Math.min(20, dist)); // forgiving angle by how close to board
                        vx = Math.cos(angle) * speedPx;
                        vy = -Math.sin(angle) * speedPx;
                        // Foul if passed the board line by more than 5 px
                        fouled = player.x > boardX + 4;
                    }
                }
            } else {
                if (k == KeyEvent.VK_ENTER) {
                    if (chainedAllAround) {
                        gp.switchScene(new JavelinScene(gp, true));
                    } else {
                        gp.switchScene(new MenuScene(gp));
                    }
                }
            }
        }
    }

    // Javelin Scene
    static class JavelinScene implements Scene {
        private final GamePanel gp;
        private final boolean chainedAllAround;
        private final Camera cam = new Camera();

        private final double mToPx = 4.0;
        private final double fieldLenM = 100.0;
        private final double worldLenPx = fieldLenM * mToPx + 400;

        private final Athlete player = new Athlete("You", new Color(66, 184, 131), 0);
        private final RhythmRunner rhythm = new RhythmRunner();
        private double time = 0;
        private boolean started = false;
        private boolean finished = false;
        private boolean countdownDone = false;
        private double countdown = 1.3;

        // run-up
        private double lineX;
        private boolean throwing = false;
        private boolean holding = false;
        private double holdTime = 0;
        private double angleDeg = 36.0;

        // projectile
        private boolean inAir = false;
        private double jx, jy, jvx, jvy;
        private double gravity = 9.81 * mToPx * 0.7;
        private double drag = 0.002; // simple air drag
        private double distance = -1;

        // AI results
        private final List<Double> rivalThrows = new ArrayList<>();
        private int placement = 1;

        public JavelinScene(GamePanel gp) { this(gp, false); }
        public JavelinScene(GamePanel gp, boolean chainedAllAround) {
            this.gp = gp;
            this.chainedAllAround = chainedAllAround;
        }

        @Override
        public void onEnter() {
            player.x = 80;
            player.y = gp.getGameHeight() - 220;
            player.speed = 0;
            player.maxSpeed = 9.8;
            player.accel = 6.5;
            player.energy = 1.0;

            lineX = 80 + 36 * mToPx; // throw line
            inAir = false;
            throwing = false;
            holding = false;
            holdTime = 0;
            angleDeg = 36.0;
            distance = -1;

            rivalThrows.clear();
            Random r = gp.rng();
            for (int i = 0; i < 5; i++) {
                double perf = 55 + r.nextDouble() * 15; // 55m to 70m
                rivalThrows.add(perf);
            }

            cam.x = 0; cam.targetX = 0;
            rhythm.reset();
            time = 0; started = false; finished = false; countdownDone = false; countdown = 1.3;
        }

        @Override
        public void onExit() {}

        @Override
        public void update(double dt) {
            cam.targetX = Math.max(0, Math.min(player.x - gp.getGameWidth() * 0.35, worldLenPx - gp.getGameWidth() * 0.55));
            cam.update(dt);

            if (!started) {
                countdown -= dt;
                if (countdown <= 0 && !countdownDone) countdownDone = true;
                if (countdown <= -0.2) {
                    started = true;
                    time = 0;
                }
                return;
            }

            if (finished) return;
            time += dt;
            rhythm.update(dt);

            if (!inAir) {
                // Approach
                if (!throwing) {
                    double boost = rhythm.currentBoost();
                    double accel = player.accel * (0.45 + Math.min(1.1, boost * 0.6));
                    player.speed += accel * dt;
                    double fric = 0.6 + player.speed * 0.05;
                    player.speed -= fric * dt;
                    player.speed = clamp(player.speed, 0, player.maxSpeed);
                    player.x += player.speed * 10.0 * dt;
                    player.updateStride(dt);

                    // if near line, transition to throw
                    if (player.x > lineX - 30) {
                        throwing = true;
                    }
                } else {
                    // Throw phase: hold space for power, up/down to set angle
                    if (holding) {
                        holdTime += dt;
                        holdTime = Math.min(holdTime, 1.8);
                    }
                }
            } else {
                // Projectile flight
                // Drag reduces velocity a bit
                double v = Math.hypot(jvx, jvy);
                double dragFx = drag * v * jvx;
                double dragFy = drag * v * jvy;

                jvx -= dragFx * dt;
                jvy += gravity * dt - dragFy * dt;
                jx += jvx * dt;
                jy += jvy * dt;

                // Land?
                double groundY = gp.getGameHeight() - 220;
                if (jy >= groundY) {
                    jy = groundY;
                    inAir = false;
                    finished = true;
                    distance = (jx - lineX) / mToPx;
                    distance = Math.max(0, distance);

                    placement = 1;
                    for (double r : rivalThrows) {
                        if (r >= distance) placement++;
                    }
                    gp.state().results.javelinDistance = distance;
                    gp.state().results.addMedal(placement);

                    if (chainedAllAround) {
                        // End all-around
                        Timer t = new Timer(1500, e -> gp.switchScene(new MedalScene(gp)));
                        t.setRepeats(false);
                        t.start();
                    }
                }
            }
        }

        @Override
        public void render(Graphics2D g) {
            int w = gp.getGameWidth();
            int h = gp.getGameHeight();

            // Sky and field
            g.setColor(new Color(196, 231, 255));
            g.fillRect(0, 0, w, h);
            drawJavelinField(g);

            // Player or projectile
            if (!inAir) {
                drawJavelinRunner(g, player, cam.x, throwing, angleDeg);
            } else {
                // Draw projectile
                int sx = (int)(jx - cam.x);
                int sy = (int)jy;
                // javelin as line with angle
                g.setStroke(new BasicStroke(3));
                g.setColor(new Color(230, 230, 230));
                double ang = Math.atan2(jvy, jvx);
                int len = 60;
                int x2 = sx + (int)(Math.cos(ang) * len);
                int y2 = sy + (int)(Math.sin(ang) * len);
                g.drawLine(sx, sy, x2, y2);
                g.setStroke(new BasicStroke(1));
            }

            // HUD
            drawHUD(g, w, h);

            if (!started) {
                String txt = countdown > 0.6 ? "READY" : "GO!";
                g.setColor(new Color(30,30,30));
                g.setFont(g.getFont().deriveFont(Font.BOLD, 42f));
                drawCenteredString(g, txt, w / 2, 120);
            }

            if (finished) {
                g.setColor(new Color(0,0,0,140));
                g.fillRect(w/2 - 240, 50, 480, 130);
                g.setColor(Color.WHITE);
                g.setFont(g.getFont().deriveFont(Font.BOLD, 24f));
                drawCenteredString(g, "Javelin Result", w/2, 85);
                g.setFont(g.getFont().deriveFont(Font.PLAIN, 20f));
                drawCenteredString(g, "Your throw: " + formatMeters(distance), w/2, 115);
                drawCenteredString(g, "Place: " + placeText(placement) + "   Best Rival: " + formatMeters(bestOf(rivalThrows)), w/2, 140);
                g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
                drawCenteredString(g, "Press Enter to continue", w/2, 164);
            }
        }

        private double bestOf(List<Double> list) {
            double b = 0;
            for (double v : list) b = Math.max(b, v);
            return b;
        }

        private String placeText(int p) {
            switch (p) {
                case 1: return "1st";
                case 2: return "2nd";
                case 3: return "3rd";
                default: return p + "th";
            }
        }

        private void drawHUD(Graphics2D g, int w, int h) {
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(20, 20, 420, 98, 12, 12);
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 18f));
            g.drawString("Javelin Throw", 30, 46);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
            if (!throwing) {
                g.drawString("Approach speed: " + new DecimalFormat("0.0").format(player.speed) + " m/s", 30, 68);
                g.drawString("Get close to the line, then hold Space to build power", 30, 88);
            } else if (!inAir) {
                int power = (int)(Math.min(1.0, holdTime / 1.5) * 100);
                g.drawString("Hold Space to build power: " + power + "%", 30, 68);
                g.drawString("Up/Down to set angle: " + new DecimalFormat("0").format(angleDeg) + "°   Release Space to throw", 30, 88);
            } else {
                g.drawString("Flight...", 30, 68);
            }

            // Controls hint
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(w - 420, 20, 400, 72, 12, 12);
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
            g.drawString("Controls:", w - 410, 42);
            g.drawString("Alternate Left/Right (or A/D) to sprint to the line", w - 410, 62);
            g.drawString("Hold Space for power, Up/Down for angle, release to throw", w - 410, 80);
        }

        private void drawJavelinField(Graphics2D g) {
            int h = gp.getGameHeight();
            int groundY = h - 200;

            // grass
            g.setColor(new Color(94, 167, 91));
            g.fillRect(0, 0, gp.getGameWidth(), h);

            // runway
            g.setColor(new Color(153, 86, 48));
            g.fillRect(0, groundY - 40, gp.getGameWidth(), 120);

            // line
            int lineSx = (int)(lineX - cam.x);
            g.setColor(Color.WHITE);
            g.fillRect(lineSx - 2, groundY - 40, 4, 120);

            // sector marks
            g.setColor(new Color(255,255,255,150));
            for (int i = 0; i <= 100; i += 5) {
                int x = (int)(80 + i * mToPx - cam.x);
                g.fillRect(x, groundY + 20, 2, 20);
            }

            // far field lines
            g.setColor(new Color(221, 163, 120));
            g.fillRect(0, groundY - 20, gp.getGameWidth(), 80);
        }

        private void drawJavelinRunner(Graphics2D g, Athlete a, double camX, boolean throwingPhase, double angle) {
            int screenX = (int)(a.x - camX);
            int y = (int)a.y;

            // Shadow
            g.setColor(new Color(0,0,0,60));
            g.fillOval(screenX - 20, y + 16, 44, 12);

            AffineTransform old = g.getTransform();
            g.translate(screenX, y);
            if (!throwingPhase) {
                // running pose
                double phase = Math.sin(a.stridePhase);
                g.setColor(a.jersey);
                g.fillRoundRect(-12, -26, 24, 30, 8, 8);
                g.setColor(new Color(255, 227, 190));
                g.fillOval(-10, -46, 20, 20);
                g.setColor(Color.BLACK);
                g.drawOval(-10, -46, 20, 20);
                g.setStroke(new BasicStroke(4));
                g.setColor(new Color(255, 227, 190));
                int armSwing = (int)(phase * 8);
                g.drawLine(-10, -18, -20, -6 - armSwing);
                g.drawLine(10, -18, 20, -6 + armSwing);
                g.setStroke(new BasicStroke(6));
                int legSwing = (int)(Math.cos(a.stridePhase * 0.9) * 10);
                g.setColor(new Color(40, 40, 40));
                g.drawLine(-6, 2, -10, 12 + legSwing);
                g.drawLine(6, 2, 10, 12 - legSwing);
            } else {
                // Prepare to throw: hold javelin
                g.setColor(a.jersey);
                g.fillRoundRect(-12, -26, 24, 30, 8, 8);
                g.setColor(new Color(255, 227, 190));
                g.fillOval(-10, -46, 20, 20);
                // Javelin
                g.setStroke(new BasicStroke(3));
                g.setColor(new Color(230, 230, 230));
                double ang = Math.toRadians(angle);
                int len = 60;
                int x2 = (int)(Math.cos(ang) * len);
                int y2 = (int)(-Math.sin(ang) * len);
                g.drawLine(0, -30, x2, -30 + y2);
                g.setStroke(new BasicStroke(1));
                // Arms up
                g.setColor(new Color(255, 227, 190));
                g.drawLine(-10, -18, -24, -20);
                g.drawLine(10, -18, 12, -30);
            }
            g.setTransform(old);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (k == KeyEvent.VK_ESCAPE) {
                gp.switchScene(new MenuScene(gp));
                return;
            }
            if (!finished) {
                if (!throwing && (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_A || k == KeyEvent.VK_D)) {
                    if (countdownDone) rhythm.key(k);
                }
                if (throwing) {
                    if (k == KeyEvent.VK_SPACE) {
                        holding = true;
                    } else if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) {
                        angleDeg = clamp(angleDeg + 2, 20, 45);
                    } else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) {
                        angleDeg = clamp(angleDeg - 2, 20, 45);
                    }
                }
                if (k == KeyEvent.VK_ENTER && finished) {
                    if (chainedAllAround) gp.switchScene(new MedalScene(gp)); else gp.switchScene(new MenuScene(gp));
                }
            } else {
                if (k == KeyEvent.VK_ENTER) {
                    if (chainedAllAround) gp.switchScene(new MedalScene(gp));
                    else gp.switchScene(new MenuScene(gp));
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (throwing && e.getKeyCode() == KeyEvent.VK_SPACE && !inAir) {
                // release to throw
                holding = false;
                double power = Math.min(1.0, holdTime / 1.5);
                double speed = 22 + 12 * power; // m/s
                double vxM = Math.cos(Math.toRadians(angleDeg)) * speed;
                double vyM = Math.sin(Math.toRadians(angleDeg)) * speed;
                jx = player.x + 20;
                jy = player.y - 30;
                jvx = vxM * mToPx;
                jvy = -vyM * mToPx;
                inAir = true;
            }
        }
    }

    // Medal Scene (summary for All-Around)
    static class MedalScene implements Scene {
        private final GamePanel gp;
        private final List<Confetti> confetti = new ArrayList<>();
        private int tick = 0;

        static class Confetti {
            double x, y, vx, vy, rot, vr;
            Color color;
        }

        public MedalScene(GamePanel gp) {
            this.gp = gp;
        }

        @Override
        public void onEnter() {
            confetti.clear();
            Random r = gp.rng();
            for (int i = 0; i < 150; i++) {
                Confetti c = new Confetti();
                c.x = r.nextInt(gp.getGameWidth());
                c.y = -r.nextInt(300);
                c.vx = -40 + r.nextDouble() * 80;
                c.vy = 60 + r.nextDouble() * 120;
                c.rot = r.nextDouble() * Math.PI * 2;
                c.vr = -2 + r.nextDouble() * 4;
                c.color = new Color(100 + r.nextInt(155), 100 + r.nextInt(155), 100 + r.nextInt(155));
                confetti.add(c);
            }
        }

        @Override
        public void onExit() {}

        @Override
        public void update(double dt) {
            tick++;
            for (Confetti c : confetti) {
                c.x += c.vx * dt;
                c.y += c.vy * dt;
                c.rot += c.vr * dt;
                c.vy += 80 * dt;
                if (c.y > gp.getGameHeight() + 30) {
                    c.y = -20; c.x = gp.rng().nextInt(gp.getGameWidth());
                    c.vy = 60 + gp.rng().nextDouble() * 120;
                }
            }
        }

        @Override
        public void render(Graphics2D g) {
            int w = gp.getGameWidth();
            int h = gp.getGameHeight();

            // Background
            g.setColor(new Color(20, 30, 50));
            g.fillRect(0, 0, w, h);

            // Podium
            int px = w/2 - 240;
            int py = h - 180;
            g.setColor(new Color(220, 220, 240));
            g.fillRoundRect(px, py, 160, 120, 12, 12);
            g.fillRoundRect(px + 160, py - 40, 160, 160, 12, 12);
            g.fillRoundRect(px + 320, py, 160, 120, 12, 12);

            // Medals
            Results r = gp.state().results;
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 36f));
            drawCenteredString(g, "All-Around Results", w/2, 80);

            g.setFont(g.getFont().deriveFont(Font.PLAIN, 18f));
            String s1 = "100m: " + formatTime(r.hundredTime) + "    Hurdles: " + formatTime(r.hurdlesTime);
            String s2 = "Long Jump: " + formatMeters(r.longJumpDistance) + "    Javelin: " + formatMeters(r.javelinDistance);
            drawCenteredString(g, s1, w/2, 120);
            drawCenteredString(g, s2, w/2, 144);

            int pts = r.totalPoints();
            g.setFont(g.getFont().deriveFont(Font.BOLD, 28f));
            drawCenteredString(g, "Total Points: " + pts, w/2, 180);

            // Player on podium (middle)
            drawMedalist(g, px + 240, py - 80, new Color(66, 184, 131), "You", 1);
            // Other medals as decorative
            drawMedalist(g, px + 80, py - 40, new Color(85, 158, 219), "CPU 1", 2);
            drawMedalist(g, px + 400, py - 40, new Color(169, 114, 242), "CPU 2", 3);

            // Confetti
            for (Confetti c : confetti) {
                g.setColor(c.color);
                AffineTransform old = g.getTransform();
                g.translate(c.x, c.y);
                g.rotate(c.rot);
                g.fillRect(-3, -8, 6, 16);
                g.setTransform(old);
            }

            // Buttons
            g.setColor(new Color(255,255,255,200));
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 16f));
            drawCenteredString(g, "Press Enter to return to Menu", w/2, h - 40);
        }

        private void drawMedalist(Graphics2D g, int x, int y, Color jersey, String name, int place) {
            // Athlete standing
            g.setColor(jersey);
            g.fillRoundRect(x - 12, y - 26, 24, 30, 8, 8);
            g.setColor(new Color(255, 227, 190));
            g.fillOval(x - 10, y - 46, 20, 20);
            g.setColor(Color.BLACK);
            g.drawOval(x - 10, y - 46, 20, 20);
            // Medal ribbon
            g.setColor(Color.BLUE);
            g.fillRect(x - 2, y - 16, 4, 10);
            // Medal
            Color medalC = place == 1 ? new Color(245, 197, 66) : place == 2 ? new Color(210, 210, 220) : new Color(205, 144, 100);
            g.setColor(medalC);
            g.fillOval(x - 8, y - 4, 16, 16);

            // Name
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 14f));
            int w = g.getFontMetrics().stringWidth(name);
            g.drawString(name, x - w/2, y + 24);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                gp.switchScene(new MenuScene(gp));
            }
        }
    }
}