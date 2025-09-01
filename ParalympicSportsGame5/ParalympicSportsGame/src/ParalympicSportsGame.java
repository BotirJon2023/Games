import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import javax.sound.sampled.*;
import javax.swing.Timer;


public class ParalympicSportsGame extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ParalympicSportsGame game = new ParalympicSportsGame();
            game.setVisible(true);
        });
    }

    // Window constants
    static final int GAME_WIDTH = 1000;
    static final int GAME_HEIGHT = 650;

    public ParalympicSportsGame() {
        super("ParalympicSportsGame - Java2D");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel panel = new GamePanel();
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
    }

    // The main game panel hosting the loop, input, and scene management
    static class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener, FocusListener {
        // Loop
        private Timer timer;
        private long lastNano;
        private double smoothedDt = 0.016; // smoothing for dt
        private int fps;
        private long fpsTimer;
        private int framesCounted;

        // Scenes
        private Scene current;
        private MainMenuScene menuScene;
        private WheelchairRaceScene raceScene;
        private GoalballScene goalballScene;
        private BocciaScene bocciaScene;

        // Input states
        private final boolean[] keys = new boolean[256];
        private int mouseX, mouseY;
        private boolean mousePressed;

        // Double-buffer for manual painting if needed (optional)
        private BufferedImage backBuffer;

        public GamePanel() {
            setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
            setFocusable(true);
            requestFocusInWindow();

            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);
            addFocusListener(this);

            // Prepare scenes
            menuScene = new MainMenuScene(this);
            raceScene = new WheelchairRaceScene(this);
            goalballScene = new GoalballScene(this);
            bocciaScene = new BocciaScene(this);

            // Start in menu
            setScene(menuScene);

            // Game loop timer ~60 FPS
            timer = new Timer(16, this);
            timer.start();

            lastNano = System.nanoTime();
            fpsTimer = System.nanoTime();

            backBuffer = new BufferedImage(GAME_WIDTH, GAME_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        }

        public void setScene(Scene s) {
            if (current != null) current.onExit();
            current = s;
            if (current != null) current.onEnter();
        }

        public void startWheelchairRace() {
            raceScene.reset();
            setScene(raceScene);
        }

        public void startGoalball() {
            goalballScene.reset();
            setScene(goalballScene);
        }

        public void startBoccia() {
            bocciaScene.reset();
            setScene(bocciaScene);
        }

        public void backToMenu() {
            setScene(menuScene);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Calculate dt with smoothing
            long now = System.nanoTime();
            double dt = (now - lastNano) / 1_000_000_000.0;
            lastNano = now;

            // Smooth dt
            smoothedDt = smoothedDt * 0.9 + dt * 0.1;

            // Update scene
            if (current != null) {
                current.update(smoothedDt);
            }

            // FPS counting
            framesCounted++;
            if ((now - fpsTimer) >= 1_000_000_000L) {
                fps = framesCounted;
                framesCounted = 0;
                fpsTimer = now;
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Optionally draw into back buffer then blit to screen (not mandatory but can reduce flicker)
            Graphics2D g2 = (Graphics2D) g;
            Graphics2D bufferG = (Graphics2D) backBuffer.getGraphics();
            bufferG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            bufferG.setColor(new Color(24, 24, 28));
            bufferG.fillRect(0, 0, getWidth(), getHeight());

            if (current != null) {
                current.render(bufferG);
            }

            // Debug overlay
            drawHUD(bufferG);

            g2.drawImage(backBuffer, 0, 0, null);
            bufferG.dispose();
        }

        private void drawHUD(Graphics2D g) {
            // Show FPS and input focus info
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(255, 255, 255, 180));
            g.drawString("FPS: " + fps, 10, 16);
            if (!hasFocus()) {
                g.setColor(new Color(255, 80, 80, 200));
                g.drawString("Click the window to focus and play", 10, 32);
            }
        }

        // Input helpers
        public boolean isKeyDown(int keyCode) {
            if (keyCode < 0 || keyCode >= keys.length) return false;
            return keys[keyCode];
        }

        public int getMouseX() { return mouseX; }
        public int getMouseY() { return mouseY; }
        public boolean isMousePressed() { return mousePressed; }

        // KeyListener
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            if (code >= 0 && code < keys.length) keys[code] = true;
            if (current != null) current.keyPressed(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();
            if (code >= 0 && code < keys.length) keys[code] = false;
            if (current != null) current.keyReleased(e);
        }

        // Mouse
        @Override
        public void mouseClicked(MouseEvent e) {
            if (current != null) current.mouseClicked(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            mousePressed = true;
            if (current != null) current.mousePressed(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mousePressed = false;
            if (current != null) current.mouseReleased(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {}
        @Override
        public void mouseExited(MouseEvent e) {}

        @Override
        public void mouseDragged(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();
            if (current != null) current.mouseDragged(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();
            if (current != null) current.mouseMoved(e);
        }

        // Focus
        @Override
        public void focusGained(FocusEvent e) {}
        @Override
        public void focusLost(FocusEvent e) {
            Arrays.fill(keys, false);
            mousePressed = false;
        }
    }

    // Scene interface (simple state machine)
    interface Scene {
        void onEnter();
        void onExit();
        void update(double dt);
        void render(Graphics2D g);
        void keyPressed(KeyEvent e);
        void keyReleased(KeyEvent e);
        void mousePressed(MouseEvent e);
        void mouseReleased(MouseEvent e);
        void mouseClicked(MouseEvent e);
        void mouseDragged(MouseEvent e);
        void mouseMoved(MouseEvent e);
    }

    // Utility: color palette used across scenes (high contrast, colorblind-friendly tendency)
    static class Pal {
        static final Color BG = new Color(24, 24, 28);
        static final Color PANEL = new Color(34, 36, 44);
        static final Color ACCENT = new Color(58, 134, 255);
        static final Color ACCENT2 = new Color(255, 205, 0);
        static final Color OK = new Color(46, 204, 113);
        static final Color WARN = new Color(231, 76, 60);
        static final Color TEXT = new Color(240, 240, 240);
        static final Color TEXT_SOFT = new Color(200, 200, 210);
        static final Color LINE = new Color(100, 110, 130);
    }

    // Simple helpful math
    static class M {
        static double clamp(double v, double a, double b) {
            return Math.max(a, Math.min(b, v));
        }
        static double lerp(double a, double b, double t) {
            return a + (b - a) * t;
        }
        static double easeOutCubic(double t) {
            double u = 1 - t; return 1 - u*u*u;
        }
        static double length(double x, double y) {
            return Math.sqrt(x*x + y*y);
        }
        static double angle(double x, double y) {
            return Math.atan2(y, x);
        }
        static double randRange(double min, double max) {
            return ThreadLocalRandom.current().nextDouble(min, max);
        }
        static int randInt(int min, int max) {
            return ThreadLocalRandom.current().nextInt(min, max + 1);
        }
        static double approach(double current, double target, double delta) {
            if (current < target) return Math.min(target, current + delta);
            else return Math.max(target, current - delta);
        }
    }

    // Tiny sound helper: Play a short sine beep in a background thread (optional)
    static class Sound {
        public static void beep(double freq, int ms, float volume) {
            new Thread(() -> {
                try {
                    float sampleRate = 44100f;
                    int numSamples = (int) (ms * sampleRate / 1000.0);
                    byte[] data = new byte[2 * numSamples];
                    // Sine
                    for (int i = 0; i < numSamples; i++) {
                        double t = i / sampleRate;
                        double v = Math.sin(2 * Math.PI * freq * t) * volume;
                        short s = (short) (v * Short.MAX_VALUE);
                        data[2*i] = (byte) (s & 0xff);
                        data[2*i + 1] = (byte) ((s >> 8) & 0xff);
                    }
                    AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
                    try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                        line.open(format, data.length);
                        line.start();
                        line.write(data, 0, data.length);
                        line.drain();
                        line.stop();
                    }
                } catch (Exception ex) {
                    // Ignore sound errors to keep the game running in limited environments
                }
            }, "BeepThread").start();
        }
    }

    // Simple UI Button
    static class Button {
        Rectangle bounds;
        String text;
        boolean hover;
        Runnable onClick;

        public Button(int x, int y, int w, int h, String text, Runnable onClick) {
            this.bounds = new Rectangle(x, y, w, h);
            this.text = text;
            this.onClick = onClick;
        }

        public void render(Graphics2D g) {
            g.setColor(hover ? Pal.ACCENT : Pal.PANEL);
            g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 16, 16);
            g.setColor(hover ? Color.white : Pal.TEXT);
            drawCenteredString(g, text, bounds, new Font("SansSerif", Font.BOLD, 18));
        }

        public void updateHover(int mx, int my) {
            hover = bounds.contains(mx, my);
        }

        public void click() {
            Sound.beep(880, 40, 0.3f);
            if (onClick != null) onClick.run();
        }
    }

    static void drawCenteredString(Graphics2D g, String text, Rectangle rect, Font font) {
        FontMetrics metrics = g.getFontMetrics(font);
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
        g.setFont(font);
        g.drawString(text, x, y);
    }

    // Animated background motif for the menu (floating ribbons and bubbles)
    static class BackgroundDecor {
        static class Ribbon {
            double y;
            double speed;
            double phase;
            Color color;
            public Ribbon(double y, double speed, double phase, Color color) {
                this.y = y; this.speed = speed; this.phase = phase; this.color = color;
            }
        }
        static class Bubble {
            double x, y, r, vy;
            float alpha;
        }
        List<Ribbon> ribbons = new ArrayList<>();
        List<Bubble> bubbles = new ArrayList<>();
        Random rng = new Random();

        public BackgroundDecor() {
            for (int i = 0; i < 5; i++) {
                Color c = i % 2 == 0 ? new Color(90, 160, 255, 100) : new Color(255, 180, 50, 100);
                ribbons.add(new Ribbon(50 + i * 110, M.randRange(20, 35), M.randRange(0, Math.PI * 2), c));
            }
            for (int i = 0; i < 40; i++) {
                bubbles.add(randomBubble());
            }
        }

        private Bubble randomBubble() {
            Bubble b = new Bubble();
            b.x = M.randRange(0, GAME_WIDTH);
            b.y = M.randRange(0, GAME_HEIGHT);
            b.r = M.randRange(3, 7);
            b.vy = M.randRange(-30, -10);
            b.alpha = (float) M.randRange(0.2, 0.5);
            return b;
        }

        public void update(double dt) {
            // Move ribbons horizontally using sine
            for (Ribbon r : ribbons) {
                r.phase += dt * 0.5;
            }
            // Bubbles rising
            for (Bubble b : bubbles) {
                b.y += b.vy * dt;
            }
            // Recycle bubbles that leave the top
            for (int i = 0; i < bubbles.size(); i++) {
                Bubble b = bubbles.get(i);
                if (b.y < -10) {
                    bubbles.set(i, randomBubble());
                    bubbles.get(i).y = GAME_HEIGHT + 10;
                }
            }
        }

        public void render(Graphics2D g) {
            // Gradient background
            GradientPaint gp = new GradientPaint(0, 0, new Color(30, 32, 38), 0, GAME_HEIGHT, new Color(18, 18, 22));
            g.setPaint(gp);
            g.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

            // Ribbons
            for (Ribbon r : ribbons) {
                Path2D path = new Path2D.Double();
                path.moveTo(0, r.y);
                for (int x = 0; x <= GAME_WIDTH; x += 20) {
                    double y = r.y + Math.sin((x * 0.01) + r.phase) * 12;
                    path.lineTo(x, y);
                }
                g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(r.color);
                g.draw(path);
            }

            // Bubbles
            for (Bubble b : bubbles) {
                g.setColor(new Color(255, 255, 255, (int)(b.alpha * 255)));
                g.fill(new Ellipse2D.Double(b.x - b.r, b.y - b.r, b.r * 2, b.r * 2));
            }
        }
    }

    // Main Menu Scene
    static class MainMenuScene implements Scene {
        private final GamePanel panel;
        private final BackgroundDecor bg = new BackgroundDecor();

        private List<Button> buttons = new ArrayList<>();
        private int selectedIndex = 0;
        private double titleAnimTime = 0;

        public MainMenuScene(GamePanel panel) {
            this.panel = panel;

            // Create buttons with actions
            int bw = 340, bh = 48;
            int cx = GAME_WIDTH / 2 - bw / 2;
            int cy = 250;
            buttons.add(new Button(cx, cy, bw, bh, "Wheelchair Sprint", panel::startWheelchairRace));
            buttons.add(new Button(cx, cy + 60, bw, bh, "Goalball", panel::startGoalball));
            buttons.add(new Button(cx, cy + 120, bw, bh, "Boccia", panel::startBoccia));
            buttons.add(new Button(cx, cy + 180, bw, bh, "Quit", () -> System.exit(0)));
        }

        @Override
        public void onEnter() {}

        @Override
        public void onExit() {}

        @Override
        public void update(double dt) {
            bg.update(dt);
            titleAnimTime += dt;

            // Update hover state
            for (Button b : buttons) {
                b.updateHover(panel.getMouseX(), panel.getMouseY());
            }

            // Keyboard selection
            if (panel.isKeyDown(KeyEvent.VK_UP) || panel.isKeyDown(KeyEvent.VK_W)) {
                // Slow down repeated changes by a small trick: not needed; use key press events for discrete changes instead.
            }
        }

        @Override
        public void render(Graphics2D g) {
            bg.render(g);

            // Title
            String title = "ParalympicSportsGame";
            g.setFont(new Font("SansSerif", Font.BOLD, 42));
            // Pulsing color accent
            float pulse = (float)(0.5 + 0.5 * Math.sin(titleAnimTime * 2));
            g.setColor(lerpColor(Pal.ACCENT, Pal.ACCENT2, pulse));
            // Soft shadow
            g.setColor(new Color(0, 0, 0, 120));
            g.drawString(title, 50 + 2, 80 + 2);
            g.setColor(new Color(240, 240, 255));
            g.drawString(title, 50, 80);

            // Subtitle
            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            g.setColor(Pal.TEXT_SOFT);
            g.drawString("Celebrate skills, strategy, and speed. Choose an event:", 50, 110);

            // Buttons
            for (int i = 0; i < buttons.size(); i++) {
                Button b = buttons.get(i);
                // Focus highlight for selected index (keyboard users)
                if (i == selectedIndex) {
                    g.setColor(new Color(255, 255, 255, 50));
                    g.fillRoundRect(b.bounds.x - 4, b.bounds.y - 4, b.bounds.width + 8, b.bounds.height + 8, 20, 20);
                }
                b.render(g);
            }

            // Helper text
            g.setColor(Pal.TEXT_SOFT);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.drawString("Use Up/Down and Enter, or click to choose. Esc quits.", 50, GAME_HEIGHT - 60);
            g.drawString("Accessibility: High-contrast UI, keyboard navigation, low-vision demo in Goalball (press V).", 50, GAME_HEIGHT - 40);
        }

        private Color lerpColor(Color a, Color b, float t) {
            int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            return new Color(r, g, bl);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_S:
                    selectedIndex = Math.min(selectedIndex + 1, buttons.size() - 1);
                    Sound.beep(660, 30, 0.2f);
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W:
                    selectedIndex = Math.max(selectedIndex - 1, 0);
                    Sound.beep(520, 30, 0.2f);
                    break;
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE:
                    buttons.get(selectedIndex).click();
                    break;
                case KeyEvent.VK_ESCAPE:
                    System.exit(0);
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) {
            for (int i = 0; i < buttons.size(); i++) {
                Button b = buttons.get(i);
                if (b.bounds.contains(e.getPoint())) {
                    selectedIndex = i;
                    b.click();
                    return;
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseClicked(MouseEvent e) {}
        @Override
        public void mouseDragged(MouseEvent e) {}
        @Override
        public void mouseMoved(MouseEvent e) {
            // Hover updates done in update() but we can update here too for immediate feedback
            for (int i = 0; i < buttons.size(); i++) {
                Button b = buttons.get(i);
                b.updateHover(e.getX(), e.getY());
                if (b.hover) selectedIndex = i;
            }
        }
    }

    // Wheelchair Sprint Scene (Side-view race)
    static class WheelchairRaceScene implements Scene {
        private final GamePanel panel;

        // Race parameters
        private final double trackLength = 1200; // pixels represent distance
        private double cameraX = 0;

        // Player physics
        static class Racer {
            String name;
            double x;
            double laneY;
            double speed;
            double stamina = 1.0; // 0..1
            double fatigue = 0.0; // increases with too-fast tapping
            Color bodyColor;
            Color chairColor;
            boolean isAI;
            double aiTargetSpeed;
            double aiVariabilityTime;

            // Alternating push detection
            int lastPushKey = 0; // -1 left, +1 right
            double pushCooldown = 0;
            double cadence; // push rhythm feedback
        }

        private Racer player;
        private List<Racer> ai = new ArrayList<>();

        // Obstacles (cones) to avoid by changing lanes
        static class Cone {
            double x;
            double y;
        }
        private List<Cone> cones = new ArrayList<>();

        // Race state
        private boolean finished = false;
        private double finishTime = 0;
        private double raceTime = 0;

        public WheelchairRaceScene(GamePanel panel) {
            this.panel = panel;
            reset();
        }

        public void reset() {
            cameraX = 0;
            raceTime = 0;
            finished = false;
            finishTime = 0;

            player = new Racer();
            player.name = "You";
            player.x = 0;
            player.laneY = GAME_HEIGHT - 220;
            player.speed = 0;
            player.bodyColor = new Color(80, 220, 120);
            player.chairColor = new Color(80, 140, 240);
            player.isAI = false;

            ai.clear();
            // Three AI opponents in different lanes
            for (int i = 0; i < 3; i++) {
                Racer r = new Racer();
                r.name = "AI " + (i + 1);
                r.x = 0;
                r.laneY = GAME_HEIGHT - 220 - (i + 1) * 70;
                r.speed = M.randRange(80, 100);
                r.bodyColor = new Color(220, 120, 80);
                r.chairColor = new Color(220, 220, 80);
                r.isAI = true;
                r.aiTargetSpeed = r.speed;
                ai.add(r);
            }

            // Cones
            cones.clear();
            for (int i = 0; i < 10; i++) {
                Cone c = new Cone();
                c.x = M.randRange(200, trackLength - 100);
                // Place randomly in the three upper lanes (player lane included)
                int laneIndex = M.randInt(0, 3);
                double baseY = GAME_HEIGHT - 220 - laneIndex * 70;
                c.y = baseY - 18; // ground offset
                cones.add(c);
            }
        }

        @Override
        public void onEnter() {
            // Slight beep enter
            Sound.beep(700, 60, 0.2f);
        }

        @Override
        public void onExit() {}

        @Override
        public void update(double dt) {
            if (!finished) raceTime += dt;

            // Input for player acceleration: alternate Left/Right (or A/D)
            handlePlayerInput(dt);

            // AI simple logic: keep average speed, vary slightly, avoid cones by shifting lanes
            for (Racer r : ai) {
                if (r.isAI) {
                    r.aiVariabilityTime += dt;
                    if (r.aiVariabilityTime > 0.7) {
                        r.aiVariabilityTime = 0;
                        r.aiTargetSpeed = M.clamp(r.aiTargetSpeed + M.randRange(-5, 5), 70, 120);
                    }
                    r.speed = M.approach(r.speed, r.aiTargetSpeed, 30 * dt);
                    // Basic cone avoidance: detect nearby cone in same lane -> nudge laneY
                    Cone nearest = findNearestCone(r.x, r.laneY);
                    if (nearest != null && Math.abs(nearest.x - r.x) < 120) {
                        // Move up or down if room
                        if (r.laneY > GAME_HEIGHT - 220 - 3 * 70) {
                            r.laneY -= 70 * dt * 2.5;
                        } else {
                            r.laneY += 70 * dt * 2.5;
                        }
                    }
                    // movement
                    r.x += r.speed * dt;
                }
            }

            // Player movement and collisions
            player.x += player.speed * dt;
            // Collide with cones: speed penalty if overlaps same lane and within x range
            Cone hit = findHitCone(player.x, player.laneY);
            if (hit != null) {
                // Penalty: reduce speed and beep
                player.speed *= 0.6;
                Sound.beep(200, 70, 0.4f);
            }

            // Camera follows player
            cameraX = player.x - 300;
            cameraX = M.clamp(cameraX, -50, trackLength - GAME_WIDTH + 100);

            // Finish line check
            if (!finished && player.x >= trackLength) {
                finished = true;
                finishTime = raceTime;
                Sound.beep(1200, 200, 0.4f);
            }
        }

        private Cone findNearestCone(double x, double laneY) {
            Cone best = null;
            double bestDist = 999999;
            for (Cone c : cones) {
                if (Math.abs(c.y - (laneY - 18)) < 10) { // same lane (approx)
                    double d = Math.abs(c.x - x);
                    if (d < bestDist) { bestDist = d; best = c; }
                }
            }
            return best;
        }

        private Cone findHitCone(double x, double laneY) {
            for (Cone c : cones) {
                if (Math.abs(c.y - (laneY - 18)) < 10) { // same lane
                    if (Math.abs(c.x - x) < 22) return c;
                }
            }
            return null;
        }

        private void handlePlayerInput(double dt) {
            // Lane movement
            if (panel.isKeyDown(KeyEvent.VK_UP) || panel.isKeyDown(KeyEvent.VK_W)) {
                player.laneY = M.approach(player.laneY, GAME_HEIGHT - 220 - 3 * 70, 180 * dt);
            }
            if (panel.isKeyDown(KeyEvent.VK_DOWN) || panel.isKeyDown(KeyEvent.VK_S)) {
                player.laneY = M.approach(player.laneY, GAME_HEIGHT - 220, 180 * dt);
            }

            // Alternating pushes: detect discrete key press events via last push key record
            // We'll also allow A/D keys. The user must alternate left and right to boost speed.
            boolean leftDown = panel.isKeyDown(KeyEvent.VK_LEFT) || panel.isKeyDown(KeyEvent.VK_A);
            boolean rightDown = panel.isKeyDown(KeyEvent.VK_RIGHT) || panel.isKeyDown(KeyEvent.VK_D);

            // Push cooldown
            player.pushCooldown = Math.max(0, player.pushCooldown - dt);

            // Simulate discrete presses by checking transitions based on cooldown and key states
            // If left is down and last push was right (or none), register a push
            if (player.pushCooldown <= 0 && leftDown && player.lastPushKey != -1) {
                registerPush(-1);
            }
            if (player.pushCooldown <= 0 && rightDown && player.lastPushKey != +1) {
                registerPush(+1);
            }

            // Natural drag and stamina
            double baseDrag = 12;
            player.speed = M.approach(player.speed, 0, baseDrag * dt);
            // Recover stamina slowly; fatigue decays
            player.stamina = M.clamp(player.stamina + 0.12 * dt, 0, 1);
            player.fatigue = M.clamp(player.fatigue - 0.25 * dt, 0, 1);

            // Gentle cadence feedback; not used directly, but could be drawn as UI
            player.cadence = M.approach(player.cadence, 0, 1.2 * dt);
        }

        private void registerPush(int dir) {
            // Evaluate alternation
            boolean alternated = (player.lastPushKey == -dir || player.lastPushKey == 0);
            player.lastPushKey = dir;
            player.pushCooldown = 0.12; // limit push rate

            double power = alternated ? 55 : 15; // good boost if alternated, else small
            // Stamina and fatigue influence
            power *= (0.5 + 0.5 * player.stamina) * (1.0 - 0.5 * player.fatigue);

            player.speed += power;

            // Fatigue grows if pushing frequently
            player.fatigue = M.clamp(player.fatigue + (alternated ? 0.03 : 0.015), 0, 1);
            player.stamina = M.clamp(player.stamina - (alternated ? 0.05 : 0.025), 0, 1);
            player.cadence = 1.0;

            // Sound feedback: higher pitch for good alternation
            Sound.beep(alternated ? 880 : 520, 30, 0.25f);
        }

        @Override
        public void render(Graphics2D g) {
            // Sky
            g.setPaint(new GradientPaint(0, 0, new Color(40, 44, 60), 0, GAME_HEIGHT, new Color(20, 22, 28)));
            g.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

            // Draw track with lanes, offset by cameraX
            drawTrack(g);

            // Draw cones
            for (Cone c : cones) {
                double sx = c.x - cameraX;
                if (sx < -50 || sx > GAME_WIDTH + 50) continue;
                drawCone(g, sx, c.y);
            }

            // Draw racers
            drawRacer(g, player);

            for (Racer r : ai) {
                drawRacer(g, r);
            }

            // Finish line
            double finishX = trackLength - cameraX;
            g.setColor(new Color(255, 255, 255, 180));
            for (int y = GAME_HEIGHT - 350; y <= GAME_HEIGHT - 140; y += 20) {
                g.fillRect((int)finishX - 2, y, 4, 10);
            }

            // HUD
            drawRaceHUD(g);
        }

        private void drawTrack(Graphics2D g) {
            // Ground
            g.setColor(new Color(90, 50, 50));
            g.fillRect(0, GAME_HEIGHT - 260, GAME_WIDTH, 260);

            // Lanes
            for (int i = 0; i < 4; i++) {
                int y = GAME_HEIGHT - 220 - i * 70;
                // Lane strip pattern
                g.setColor(new Color(160, 160, 160, 200));
                for (double x = -50; x <= GAME_WIDTH + 50; x += 30) {
                    double worldX = x + cameraX;
                    if (((int)(worldX / 30)) % 2 == 0) {
                        g.fillRect((int) x, y - 2, 20, 4);
                    }
                }
            }

            // Infield grass
            g.setColor(new Color(40, 100, 60));
            g.fillRect(0, GAME_HEIGHT - 370, GAME_WIDTH, 110);

            // Background silhouettes: audience
            g.setColor(new Color(0, 0, 0, 90));
            for (int i = 0; i < 120; i++) {
                int x = (i * 50) % (GAME_WIDTH + 200) - 100;
                int y = (int) (GAME_HEIGHT - 380 + Math.sin(i * 1223) * 3);
                g.fillOval(x, y, 18, 18);
            }

            // Scoreboard banner
            g.setColor(new Color(30, 30, 36));
            g.fillRoundRect(20, 20, 280, 80, 18, 18);
            g.setColor(Pal.TEXT);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString("Wheelchair Sprint", 40, 48);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(Pal.TEXT_SOFT);
            g.drawString("Alternate Left/Right to accelerate. Up/Down to switch lanes.", 40, 70);
        }

        private void drawCone(Graphics2D g, double x, double y) {
            int baseW = 26;
            int baseH = 14;
            g.setColor(new Color(220, 100, 40));
            Polygon p = new Polygon();
            p.addPoint((int)x, (int)(y - 24));
            p.addPoint((int)(x - 10), (int)(y));
            p.addPoint((int)(x + 10), (int)(y));
            g.fillPolygon(p);
            g.setColor(new Color(255, 255, 255, 180));
            g.fillRect((int)(x - baseW/2), (int)y, baseW, 3);
            g.setColor(new Color(60, 40, 40, 120));
            g.fillOval((int)(x - baseW/2), (int)(y + baseH - 4), baseW, 8);
        }

        private void drawRacer(Graphics2D g, Racer r) {
            double sx = r.x - cameraX;
            if (sx < -100 || sx > GAME_WIDTH + 100) return;

            // Wheelchair: two wheels
            double y = r.laneY;
            g.setColor(new Color(0, 0, 0, 60));
            g.fillOval((int)(sx - 30), (int)(y + 22), 52, 12);

            // Chair frame
            g.setColor(r.chairColor);
            g.setStroke(new BasicStroke(3f));
            g.drawLine((int)sx - 20, (int)y, (int)sx + 24, (int)y);
            g.drawLine((int)sx - 10, (int)y, (int)sx - 20, (int)y - 14);
            g.drawLine((int)sx + 10, (int)y, (int)sx + 20, (int)y - 10);

            // Wheels with spinning illusion
            double spin = r.x * 0.2;
            drawWheel(g, sx - 20, y + 10, 18, spin);
            drawWheel(g, sx + 20, y + 10, 18, spin);

            // Athlete body
            g.setColor(r.bodyColor);
            g.fillOval((int)sx - 8, (int)y - 26, 16, 16); // head
            g.fillRect((int)sx - 6, (int)y - 10, 12, 10); // torso
            // arms pushing
            double armPhase = Math.sin(r.cadence * Math.PI);
            g.drawLine((int)sx, (int)y - 10, (int)(sx - 12 - 8 * armPhase), (int)(y + 4));
            g.drawLine((int)sx, (int)y - 10, (int)(sx + 12 + 8 * armPhase), (int)(y + 4));

            // Name label
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(255, 255, 255, 220));
            g.drawString(r.name, (int)sx - 20, (int)y - 32);
        }

        private void drawWheel(Graphics2D g, double cx, double cy, double r, double spin) {
            g.setColor(new Color(60, 60, 60));
            g.fillOval((int)(cx - r), (int)(cy - r), (int)(2*r), (int)(2*r));
            g.setColor(new Color(200, 200, 200));
            g.setStroke(new BasicStroke(2f));
            for (int i = 0; i < 8; i++) {
                double a = spin + i * Math.PI / 4;
                int x2 = (int)(cx + Math.cos(a) * (r - 2));
                int y2 = (int)(cy + Math.sin(a) * (r - 2));
                g.drawLine((int)cx, (int)cy, x2, y2);
            }
            g.setColor(new Color(250, 250, 250));
            g.drawOval((int)(cx - r), (int)(cy - r), (int)(2*r), (int)(2*r));
        }

        private void drawRaceHUD(Graphics2D g) {
            // HUD panel
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(GAME_WIDTH - 240, 20, 220, 120, 12, 12);

            g.setColor(Pal.TEXT);
            g.setFont(new Font("Monospaced", Font.BOLD, 16));
            g.drawString(String.format("Time: %.2fs", raceTime), GAME_WIDTH - 220, 50);
            g.drawString(String.format("Speed: %.1f", player.speed), GAME_WIDTH - 220, 75);
            g.drawString(String.format("Stamina: %d%%", (int)(player.stamina * 100)), GAME_WIDTH - 220, 100);

            // Finish result
            if (finished) {
                g.setColor(new Color(0, 0, 0, 160));
                g.fillRoundRect(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 - 120, 440, 240, 16, 16);
                g.setColor(Color.white);
                drawCenteredString(g, "Finished!", new Rectangle(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 - 110, 440, 40), new Font("SansSerif", Font.BOLD, 28));
                drawCenteredString(g, String.format("Your time: %.2fs", finishTime), new Rectangle(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 - 60, 440, 30), new Font("SansSerif", Font.PLAIN, 20));
                drawCenteredString(g, "Press Enter or Esc to return to Menu", new Rectangle(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 - 20, 440, 30), new Font("SansSerif", Font.PLAIN, 16));
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (finished) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    panel.backToMenu();
                }
            } else {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    panel.backToMenu();
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseClicked(MouseEvent e) {}
        @Override
        public void mouseDragged(MouseEvent e) {}
        @Override
        public void mouseMoved(MouseEvent e) {}
    }

    // Goalball Scene
    static class GoalballScene implements Scene {
        private final GamePanel panel;

        // Court geometry
        private Rectangle court = new Rectangle(80, 80, GAME_WIDTH - 160, GAME_HEIGHT - 180);
        private Rectangle goalRight = new Rectangle(GAME_WIDTH - 80 - 20, 140, 20, GAME_HEIGHT - 260);
        private Rectangle goalLeft = new Rectangle(80, 140, 20, GAME_HEIGHT - 260);

        // Ball state
        static class Ball {
            double x, y;
            double vx, vy;
            double radius = 10;
            boolean rolling;
            double soundTimer;
        }
        private Ball ball = new Ball();

        // Players (defenders on right side)
        static class Defender {
            double x, y;
            double speed = 220;
            double hearingSensitivity = 1.0;
            Color color = new Color(255, 200, 80);
            double diveCooldown;
        }
        private List<Defender> defenders = new ArrayList<>();

        // Throwing
        private boolean dragging = false;
        private int dragStartX, dragStartY;

        // State
        private int scorePlayer = 0;
        private int scoreAI = 0;
        private double roundTimer = 0;
        private boolean goalScored = false;
        private boolean lowVisionMode = false;

        public GoalballScene(GamePanel panel) {
            this.panel = panel;
            reset();
        }

        public void reset() {
            // Place ball at left-center
            ball.x = court.x + court.width * 0.2;
            ball.y = court.y + court.height / 2.0;
            ball.vx = 0; ball.vy = 0;
            ball.rolling = false;
            ball.soundTimer = 0;

            defenders.clear();
            for (int i = 0; i < 3; i++) {
                Defender d = new Defender();
                d.x = goalRight.x - 60;
                double frac = (i + 1) / 4.0;
                d.y = court.y + court.height * frac;
                d.speed = 220 + i * 10;
                defenders.add(d);
            }
            roundTimer = 0;
            goalScored = false;
        }

        @Override
        public void onEnter() {
            Sound.beep(900, 60, 0.2f);
        }

        @Override
        public void onExit() {}

        @Override
        public void update(double dt) {
            roundTimer += dt;

            // Update ball
            if (ball.rolling) {
                // Friction
                double friction = 0.98;
                ball.vx *= Math.pow(friction, dt * 60);
                ball.vy *= Math.pow(friction, dt * 60);
                ball.x += ball.vx * dt;
                ball.y += ball.vy * dt;

                // Bounce walls inside court
                if (ball.x - ball.radius < court.x) { ball.x = court.x + ball.radius; ball.vx = -ball.vx * 0.75; beepBall(); }
                if (ball.x + ball.radius > court.x + court.width) { ball.x = court.x + court.width - ball.radius; ball.vx = -ball.vx * 0.75; beepBall(); }
                if (ball.y - ball.radius < court.y) { ball.y = court.y + ball.radius; ball.vy = -ball.vy * 0.75; beepBall(); }
                if (ball.y + ball.radius > court.y + court.height) { ball.y = court.y + court.height - ball.radius; ball.vy = -ball.vy * 0.75; beepBall(); }

                // Stop threshold
                if (Math.hypot(ball.vx, ball.vy) < 8) {
                    ball.vx = 0; ball.vy = 0;
                    ball.rolling = false;
                }

                // Emit sound while rolling
                ball.soundTimer -= dt;
                if (ball.soundTimer <= 0) {
                    beepBall();
                    ball.soundTimer = 0.25; // beep cadence while moving
                }
            }

            // Defenders "listen" for the ball and move towards prediction
            for (Defender d : defenders) {
                if (d.diveCooldown > 0) d.diveCooldown -= dt;
                double hearX = ball.x, hearY = ball.y;
                double dx = hearX - d.x;
                double dy = hearY - d.y;
                double dist = Math.hypot(dx, dy);

                // Reduced movement if ball slow
                double moveScale = M.clamp(Math.hypot(ball.vx, ball.vy) / 300.0, 0.2, 1.0);
                double speed = d.speed * moveScale;

                if (dist > 1) {
                    d.x += (dx / dist) * speed * dt;
                    d.y += (dy / dist) * speed * dt;
                }

                // If ball near their x-line, attempt to block (simulate dive)
                if (ball.rolling && Math.abs(ball.x - (goalRight.x - 40)) < 120 && d.diveCooldown <= 0) {
                    d.diveCooldown = 1.0;
                    // Quick lateral move
                    d.y += Math.signum(dy) * 40;
                }

                // Clamp defenders to court
                d.x = M.clamp(d.x, court.x + 40, goalRight.x - 40);
                d.y = M.clamp(d.y, court.y + 20, court.y + court.height - 20);

                // Collision with ball (block)
                double bdx = ball.x - d.x;
                double bdy = ball.y - d.y;
                double bdist = Math.hypot(bdx, bdy);
                if (bdist < (ball.radius + 20)) {
                    // Bounce the ball away
                    double nx = bdx / (bdist + 1e-6);
                    double ny = bdy / (bdist + 1e-6);
                    double speedBall = Math.hypot(ball.vx, ball.vy);
                    ball.vx = -nx * Math.max(120, speedBall * 0.8);
                    ball.vy = -ny * Math.max(120, speedBall * 0.8);
                    beepBall();
                }
            }

            // Goal check: ball enters right goal
            if (!goalScored && ball.x + ball.radius > goalRight.x && ball.y > goalRight.y && ball.y < goalRight.y + goalRight.height) {
                goalScored = true;
                scorePlayer++;
                Sound.beep(1200, 180, 0.5f);
                // Reset after short delay
                Timer t = new Timer(900, e -> reset());
                t.setRepeats(false);
                t.start();
            }

            // Simple AI serve if ball stops on right side (to keep play going)
            if (!ball.rolling && !dragging && ball.x > court.x + court.width * 0.6 && !goalScored) {
                aiServe();
            }
        }

        private void beepBall() {
            // Frequency modulated by velocity to simulate audible ball
            double v = Math.hypot(ball.vx, ball.vy);
            double freq = 300 + Math.min(700, v);
            Sound.beep(freq, 40, 0.25f);
        }

        private void aiServe() {
            // AI throws back toward left goal area
            double aimX = court.x + court.width * 0.2;
            double aimY = court.y + court.height * M.randRange(0.3, 0.7);
            double dx = aimX - ball.x;
            double dy = aimY - ball.y;
            double dist = Math.hypot(dx, dy);
            double power = 340;
            ball.vx = dx / (dist + 1e-6) * power;
            ball.vy = dy / (dist + 1e-6) * power;
            ball.rolling = true;
            ball.soundTimer = 0;
        }

        @Override
        public void render(Graphics2D g) {
            // Court floor
            g.setColor(new Color(28, 34, 45));
            g.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

            // Draw court boundary and goals
            g.setColor(new Color(20, 80, 120));
            g.fillRoundRect(court.x - 20, court.y - 20, court.width + 40, court.height + 40, 28, 28);
            g.setColor(new Color(60, 120, 180));
            g.fillRect(court.x, court.y, court.width, court.height);
            g.setColor(Color.white);
            g.setStroke(new BasicStroke(3f));
            g.drawRect(court.x, court.y, court.width, court.height);

            // Goals
            g.setColor(new Color(220, 220, 220));
            g.fill(goalRight);
            g.fill(goalLeft);

            // Center line
            g.setColor(new Color(255, 255, 255, 160));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(court.x + court.width / 2, court.y, court.x + court.width / 2, court.y + court.height);

            // "Low-vision" mask demo
            if (lowVisionMode) {
                // Darken screen except around the ball with soft vignette
                BufferedImage mask = new BufferedImage(GAME_WIDTH, GAME_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D mg = mask.createGraphics();
                mg.setComposite(AlphaComposite.Src);
                mg.setColor(new Color(0, 0, 0, 200));
                mg.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

                // Light circle around ball
                RadialGradientPaint rgp = new RadialGradientPaint(new Point2D.Double(ball.x, ball.y), 160,
                        new float[]{0f, 1f}, new Color[]{new Color(0,0,0,0), new Color(0,0,0,200)});
                mg.setPaint(rgp);
                mg.setComposite(AlphaComposite.DstOut);
                mg.fill(new Ellipse2D.Double(ball.x - 160, ball.y - 160, 320, 320));
                mg.dispose();
                g.drawImage(mask, 0, 0, null);
            }

            // Defenders
            for (Defender d : defenders) {
                g.setColor(new Color(0, 0, 0, 80));
                g.fillOval((int)d.x - 24, (int)d.y + 14, 48, 12);
                g.setColor(d.color);
                g.fillRoundRect((int)d.x - 16, (int)d.y - 20, 32, 38, 10, 10);
                g.setColor(new Color(30, 30, 30));
                g.fillOval((int)d.x - 8, (int)d.y - 26, 16, 16);
            }

            // Ball sound waves
            if (ball.rolling) {
                double v = Math.hypot(ball.vx, ball.vy);
                int rings = 3;
                for (int i = 0; i < rings; i++) {
                    double t = ((System.currentTimeMillis() / 100.0) + i * 10) % 50;
                    double r = 20 + t * (0.6 + v / 500.0);
                    int alpha = (int) (120 - t * 2);
                    if (alpha <= 0) continue;
                    g.setColor(new Color(255, 255, 255, alpha));
                    g.setStroke(new BasicStroke(2f));
                    g.draw(new Ellipse2D.Double(ball.x - r, ball.y - r, 2 * r, 2 * r));
                }
            }

            // Ball
            g.setColor(new Color(255, 240, 90));
            g.fill(new Ellipse2D.Double(ball.x - ball.radius, ball.y - ball.radius, 2 * ball.radius, 2 * ball.radius));
            g.setColor(new Color(60, 50, 20, 120));
            g.fillOval((int)(ball.x - 10), (int)(ball.y + 8), 20, 10);

            // Drag aim line
            if (dragging) {
                int mx = panel.getMouseX();
                int my = panel.getMouseY();
                g.setColor(new Color(255, 255, 255, 180));
                g.setStroke(new BasicStroke(2f));
                g.drawLine(dragStartX, dragStartY, mx, my);
                g.setColor(new Color(255, 255, 255, 120));
                g.fillOval(dragStartX - 4, dragStartY - 4, 8, 8);
            }

            // HUD
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRoundRect(20, 20, 280, 90, 12, 12);
            g.setColor(Pal.TEXT);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("Goalball", 36, 42);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(Pal.TEXT_SOFT);
            g.drawString("Drag to aim and release to throw. Press V for low-vision mode.", 36, 66);
            g.drawString("Score: You " + scorePlayer + " - " + scoreAI + " AI", 36, 86);

            // Post-goal overlay (if AI scores we would display too)
            if (goalScored) {
                g.setColor(new Color(0, 0, 0, 160));
                g.fillRoundRect(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 - 100, 440, 200, 16, 16);
                g.setColor(Color.white);
                drawCenteredString(g, "Goal!", new Rectangle(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 - 90, 440, 40), new Font("SansSerif", Font.BOLD, 28));
                drawCenteredString(g, "Resetting...", new Rectangle(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 - 50, 440, 40), new Font("SansSerif", Font.PLAIN, 18));
                drawCenteredString(g, "Esc: Menu", new Rectangle(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 - 10, 440, 40), new Font("SansSerif", Font.PLAIN, 16));
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) panel.backToMenu();
            if (e.getKeyCode() == KeyEvent.VK_V) {
                lowVisionMode = !lowVisionMode;
                Sound.beep(lowVisionMode ? 500 : 700, 80, 0.3f);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) {
            if (goalScored) return;
            if (!ball.rolling && court.contains(e.getPoint())) {
                dragging = true;
                dragStartX = e.getX();
                dragStartY = e.getY();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (dragging) {
                dragging = false;
                int mx = e.getX();
                int my = e.getY();
                double dx = mx - dragStartX;
                double dy = my - dragStartY;
                double power = M.clamp(Math.hypot(dx, dy), 20, 220);
                double ang = Math.atan2(dy, dx);
                ball.vx = Math.cos(ang) * power * 1.6;
                ball.vy = Math.sin(ang) * power * 1.6;
                ball.rolling = true;
                ball.soundTimer = 0;
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {}
        @Override
        public void mouseDragged(MouseEvent e) {}
        @Override
        public void mouseMoved(MouseEvent e) {}
    }

    // Boccia Scene (precision throwing)
    static class BocciaScene implements Scene {
        private final GamePanel panel;

        // Field
        private Rectangle field = new Rectangle(80, 120, GAME_WIDTH - 160, GAME_HEIGHT - 220);

        // Jack (target)
        private Point2D.Double jack = new Point2D.Double();

        // Balls
        static class BocciaBall {
            double x, y, vx, vy;
            Color color;
            boolean moving;
        }
        private List<BocciaBall> balls = new ArrayList<>();

        // Player aim state
        private double aimAngle = Math.toRadians(0);
        private double aimPower = 220;
        private boolean thrownThisRound = false;

        // Rounds
        private int round = 1;
        private int totalRounds = 3;
        private int playerPoints = 0;
        private int aiPoints = 0;
        private boolean roundOver = false;

        // AI ball for each round
        private BocciaBall aiBall = null;

        public BocciaScene(GamePanel panel) {
            this.panel = panel;
            reset();
        }

        public void reset() {
            balls.clear();
            jack.x = field.x + field.width * 0.8;
            jack.y = field.y + field.height * M.randRange(0.2, 0.8);

            round = 1;
            playerPoints = 0;
            aiPoints = 0;
            thrownThisRound = false;
            roundOver = false;
            aiBall = null;

            aimAngle = Math.toRadians(-8);
            aimPower = 220;
        }

        @Override
        public void onEnter() {
            Sound.beep(740, 60, 0.25f);
        }

        @Override
        public void onExit() {}

        @Override
        public void update(double dt) {
            // Update ball movement with friction and simple collisions
            for (BocciaBall b : balls) {
                if (!b.moving) continue;
                b.x += b.vx * dt;
                b.y += b.vy * dt;
                b.vx *= Math.pow(0.98, dt * 60);
                b.vy *= Math.pow(0.98, dt * 60);

                // Walls
                if (b.x < field.x + 10) { b.x = field.x + 10; b.vx = -b.vx * 0.7; }
                if (b.x > field.x + field.width - 10) { b.x = field.x + field.width - 10; b.vx = -b.vx * 0.7; }
                if (b.y < field.y + 10) { b.y = field.y + 10; b.vy = -b.vy * 0.7; }
                if (b.y > field.y + field.height - 10) { b.y = field.y + field.height - 10; b.vy = -b.vy * 0.7; }

                // Stop threshold
                if (Math.hypot(b.vx, b.vy) < 10) {
                    b.vx = 0; b.vy = 0; b.moving = false;
                }
            }

            // Round state handling
            if (roundOver) {
                // Wait for player input to continue (Enter)
                return;
            }

            // If player thrown and balls stopped, AI throws
            boolean anyMoving = balls.stream().anyMatch(b -> b.moving);
            if (thrownThisRound && aiBall == null && !anyMoving) {
                doAIThrow();
            }

            // After AI throw, wait to stop, then score round
            if (aiBall != null && !anyMoving) {
                scoreRound();
            }
        }

        private void doAIThrow() {
            aiBall = new BocciaBall();
            aiBall.x = field.x + field.width * 0.2;
            aiBall.y = field.y + field.height / 2.0;
            aiBall.color = new Color(230, 80, 80);

            // Aim toward jack with some error
            double dx = jack.x - aiBall.x;
            double dy = jack.y - aiBall.y;
            double baseAngle = Math.atan2(dy, dx);
            double errAngle = M.randRange(-0.10, 0.10); // AI precision
            double power = 240 + M.randRange(-30, 30);
            aiBall.vx = Math.cos(baseAngle + errAngle) * power;
            aiBall.vy = Math.sin(baseAngle + errAngle) * power;
            aiBall.moving = true;

            balls.add(aiBall);
            Sound.beep(660, 60, 0.25f);
        }

        private void scoreRound() {
            roundOver = true;

            // Compute closest distances to jack
            double bestPlayer = Double.POSITIVE_INFINITY;
            double bestAI = Double.POSITIVE_INFINITY;

            for (BocciaBall b : balls) {
                double d = Point2D.distance(b.x, b.y, jack.x, jack.y);
                if (b.color.equals(new Color(80, 160, 240))) {
                    bestPlayer = Math.min(bestPlayer, d);
                } else if (b.color.equals(new Color(230, 80, 80))) {
                    bestAI = Math.min(bestAI, d);
                }
            }

            if (bestPlayer < bestAI) {
                playerPoints++;
                Sound.beep(1200, 140, 0.4f);
            } else {
                aiPoints++;
                Sound.beep(400, 140, 0.4f);
            }
        }

        private void nextRoundOrEnd() {
            if (round >= totalRounds) {
                // End -> back to menu after user confirms
                panel.backToMenu();
                return;
            }
            // Prepare next round
            round++;
            thrownThisRound = false;
            roundOver = false;
            aiBall = null;
            balls.clear();
            // Randomize jack within a range
            jack.x = field.x + field.width * 0.8;
            jack.y = field.y + field.height * M.randRange(0.2, 0.8);
        }

        @Override
        public void render(Graphics2D g) {
            // Field background
            g.setColor(new Color(26, 36, 26));
            g.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
            g.setColor(new Color(46, 86, 46));
            g.fillRect(field.x, field.y, field.width, field.height);

            // Jack
            g.setColor(new Color(245, 245, 245));
            g.fill(new Ellipse2D.Double(jack.x - 6, jack.y - 6, 12, 12));

            // Balls
            for (BocciaBall b : balls) {
                g.setColor(b.color);
                g.fill(new Ellipse2D.Double(b.x - 10, b.y - 10, 20, 20));
                g.setColor(new Color(0, 0, 0, 80));
                g.fillOval((int)(b.x - 10), (int)(b.y + 8), 20, 10);
            }

            // Aim indicator (if not thrown yet)
            if (!thrownThisRound && !roundOver) {
                double sx = field.x + field.width * 0.2;
                double sy = field.y + field.height / 2.0;
                double ex = sx + Math.cos(aimAngle) * (aimPower * 0.7);
                double ey = sy + Math.sin(aimAngle) * (aimPower * 0.7);

                g.setColor(new Color(0, 0, 0, 100));
                g.fillOval((int)sx - 12, (int)sy + 10, 24, 12);
                g.setColor(new Color(80, 160, 240));
                g.fill(new Ellipse2D.Double(sx - 10, sy - 10, 20, 20));

                g.setColor(new Color(255, 255, 255, 180));
                g.setStroke(new BasicStroke(2f));
                g.drawLine((int)sx, (int)sy, (int)ex, (int)ey);
                // Arrow head
                double ang = Math.atan2(ey - sy, ex - sx);
                int ax1 = (int)(ex - 10 * Math.cos(ang - 0.4));
                int ay1 = (int)(ey - 10 * Math.sin(ang - 0.4));
                int ax2 = (int)(ex - 10 * Math.cos(ang + 0.4));
                int ay2 = (int)(ey - 10 * Math.sin(ang + 0.4));
                g.drawLine((int)ex, (int)ey, ax1, ay1);
                g.drawLine((int)ex, (int)ey, ax2, ay2);
            }

            // HUD
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRoundRect(20, 20, 360, 120, 12, 12);
            g.setColor(Pal.TEXT);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("Boccia - Round " + round + " / " + totalRounds, 36, 44);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(Pal.TEXT_SOFT);
            g.drawString("Aim: Left/Right | Power: Up/Down | Throw: Space/Enter", 36, 66);
            g.drawString("Score: You " + playerPoints + " - " + aiPoints + " AI", 36, 88);
            g.drawString("Esc: Menu", 36, 108);

            if (roundOver) {
                g.setColor(new Color(0, 0, 0, 170));
                g.fillRoundRect(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 - 110, 440, 220, 16, 16);
                g.setColor(Color.white);
                String text = "Round " + round + " complete!";
                drawCenteredString(g, text, new Rectangle(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 - 102, 440, 36), new Font("SansSerif", Font.BOLD, 24));
                drawCenteredString(g, "Press Enter for next round", new Rectangle(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 - 66, 440, 30), new Font("SansSerif", Font.PLAIN, 18));
                drawCenteredString(g, "Current Score - You " + playerPoints + " : " + aiPoints + " AI", new Rectangle(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 - 26, 440, 30), new Font("SansSerif", Font.PLAIN, 18));
            }

            // If all rounds done, show final
            if (roundOver && round >= totalRounds) {
                String result = playerPoints >= aiPoints ? "You Win!" : "AI Wins";
                g.setColor(new Color(0, 0, 0, 200));
                g.fillRoundRect(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 + 20, 440, 120, 16, 16);
                g.setColor(Color.white);
                drawCenteredString(g, "Final: " + result, new Rectangle(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 + 28, 440, 30), new Font("SansSerif", Font.BOLD, 20));
                drawCenteredString(g, "Press Enter to return to Menu", new Rectangle(GAME_WIDTH / 2 - 220, GAME_HEIGHT / 2 + 60, 440, 30), new Font("SansSerif", Font.PLAIN, 16));
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                panel.backToMenu();
                return;
            }

            if (!roundOver) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_A:
                        aimAngle -= Math.toRadians(4);
                        break;
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_D:
                        aimAngle += Math.toRadians(4);
                        break;
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_W:
                        aimPower = M.clamp(aimPower + 10, 60, 320);
                        break;
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_S:
                        aimPower = M.clamp(aimPower - 10, 60, 320);
                        break;
                    case KeyEvent.VK_SPACE:
                    case KeyEvent.VK_ENTER:
                        if (!thrownThisRound) {
                            throwPlayerBall();
                        }
                        break;
                }
            } else {
                // On roundOver
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    nextRoundOrEnd();
                }
            }
        }

        private void throwPlayerBall() {
            BocciaBall b = new BocciaBall();
            b.x = field.x + field.width * 0.2;
            b.y = field.y + field.height / 2.0;
            b.color = new Color(80, 160, 240);
            b.vx = Math.cos(aimAngle) * aimPower;
            b.vy = Math.sin(aimAngle) * aimPower;
            b.moving = true;
            balls.add(b);

            thrownThisRound = true;
            Sound.beep(900, 60, 0.3f);
        }

        @Override
        public void keyReleased(KeyEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseClicked(MouseEvent e) {}
        @Override
        public void mouseDragged(MouseEvent e) {}
        @Override
        public void mouseMoved(MouseEvent e) {}
    }
}
