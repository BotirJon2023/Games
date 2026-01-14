import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class OlympicSportSimulator {

    // Main entry
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppFrame frame = new AppFrame();
            frame.setVisible(true);
        });
    }

    // Root frame
    public static class AppFrame extends JFrame {
        private final GamePanel panel;

        public AppFrame() {
            super("Olympic Sport Simulator");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());

            panel = new GamePanel();
            add(panel, BorderLayout.CENTER);

            // Menu / top bar
            JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            topBar.setBorder(new EmptyBorder(4, 6, 4, 6));
            topBar.setBackground(new Color(20, 20, 25));

            JLabel eventLabel = new JLabel("Event:");
            eventLabel.setForeground(Color.WHITE);

            JComboBox<SportType> eventSelect = new JComboBox<>(SportType.values());
            eventSelect.setSelectedItem(SportType.SPRINT_100M);
            eventSelect.addActionListener(e -> {
                SportType sel = (SportType) eventSelect.getSelectedItem();
                if (sel != null) {
                    panel.switchEvent(sel);
                }
            });

            JButton startBtn = new JButton("Start");
            startBtn.addActionListener(e -> panel.startEvent());

            JButton pauseBtn = new JButton("Pause/Resume");
            pauseBtn.addActionListener(e -> panel.togglePause());

            JButton resetBtn = new JButton("Reset");
            resetBtn.addActionListener(e -> panel.resetEvent());

            JCheckBox showDbg = new JCheckBox("Show Debug");
            showDbg.setForeground(Color.WHITE);
            showDbg.setOpaque(false);
            showDbg.addActionListener(e -> panel.setDebug(showDbg.isSelected()));

            JButton fullBtn = new JButton("Fullscreen");
            fullBtn.addActionListener(e -> panel.toggleFullscreen(this));

            topBar.add(eventLabel);
            topBar.add(eventSelect);
            topBar.add(startBtn);
            topBar.add(pauseBtn);
            topBar.add(resetBtn);
            topBar.add(showDbg);
            topBar.add(fullBtn);

            add(topBar, BorderLayout.NORTH);

            setMinimumSize(new Dimension(1000, 640));
            pack();
            setLocationRelativeTo(null);

            // Relay keyboard focus
            addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowGainedFocus(WindowEvent e) {
                    panel.requestFocusInWindow();
                }
            });
        }
    }

    // Types of sports supported
    public enum SportType {
        SPRINT_100M("100m Sprint"),
        SWIMMING_FREESTYLE("Swimming Freestyle"),
        JAVELIN("Javelin Throw");

        private final String label;
        SportType(String label) { this.label = label; }
        public String toString() { return label; }
    }

    // Core game panel with animation
    public static class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener, FocusListener {
        // Timing and rendering
        private final Timer timer;
        private long lastNano;
        private boolean paused = false;
        private boolean showDebug = false;
        private boolean hasFocus = true;
        private boolean fullscreen = false;
        private GraphicsDevice fsDevice;

        // Events and participants
        private SportType currentType = SportType.SPRINT_100M;
        private SportEvent currentEvent;
        private final Scoreboard scoreboard = new Scoreboard();

        // Input state
        private final Set<Integer> keysDown = new HashSet<>();
        private Point mousePos = new Point(0, 0);
        private boolean mouseDown = false;

        // UI / Theme
        private final Color bgColor = new Color(18, 18, 22);
        private final Color panelBg = new Color(26, 26, 34);
        private final Font hudFont = new Font("SansSerif", Font.PLAIN, 14);
        private final Font titleFont = new Font("SansSerif", Font.BOLD, 20);

        // Random
        private final Random rng = new Random();

        public GamePanel() {
            setPreferredSize(new Dimension(1200, 720));
            setBackground(bgColor);
            setFocusable(true);
            requestFocusInWindow();

            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);
            addFocusListener(this);

            // Start on default event
            switchEvent(currentType);

            timer = new Timer(1000 / 60, this);
            lastNano = System.nanoTime();
            timer.start();
        }

        // Switch event
        public void switchEvent(SportType type) {
            this.currentType = type;
            // Create new event
            if (type == SportType.SPRINT_100M) {
                currentEvent = new SprintEvent(this, scoreboard);
            } else if (type == SportType.SWIMMING_FREESTYLE) {
                currentEvent = new SwimmingEvent(this, scoreboard);
            } else if (type == SportType.JAVELIN) {
                currentEvent = new JavelinEvent(this, scoreboard);
            } else {
                currentEvent = new SprintEvent(this, scoreboard);
            }
            paused = true; // paused initially, user hits Start
            repaint();
        }

        public void startEvent() {
            if (currentEvent != null) {
                currentEvent.start();
                paused = false;
            }
        }

        public void resetEvent() {
            if (currentEvent != null) {
                currentEvent.reset();
                paused = true;
            }
        }

        public void togglePause() {
            paused = !paused;
        }

        public void setDebug(boolean dbg) {
            this.showDebug = dbg;
            repaint();
        }

        // Fullscreen toggling for the parent frame
        public void toggleFullscreen(JFrame frame) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            fsDevice = ge.getDefaultScreenDevice();
            fullscreen = !fullscreen;

            if (fullscreen && fsDevice.isFullScreenSupported()) {
                frame.dispose();
                frame.setUndecorated(true);
                fsDevice.setFullScreenWindow(frame);
                frame.setVisible(true);
                requestFocusInWindow();
            } else {
                fsDevice.setFullScreenWindow(null);
                frame.dispose();
                frame.setUndecorated(false);
                frame.setVisible(true);
                requestFocusInWindow();
            }
        }

        // Main animation loop
        @Override
        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            double dt = (now - lastNano) / 1e9;
            lastNano = now;

            if (!paused && currentEvent != null) {
                currentEvent.update(dt);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Background gradient
            paintBackgroundGradient(g2, getWidth(), getHeight());

            // Draw current event
            if (currentEvent != null) {
                currentEvent.render(g2, getWidth(), getHeight());
            }

            // HUD banner
            drawHUD(g2);

            // Debug overlay
            if (showDebug) {
                drawDebug(g2);
            }

            g2.dispose();
        }

        private void paintBackgroundGradient(Graphics2D g2, int w, int h) {
            Color c1 = new Color(16, 16, 24);
            Color c2 = new Color(30, 30, 42);
            GradientPaint gp = new GradientPaint(0, 0, c1, 0, h, c2);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
        }

        private void drawHUD(Graphics2D g2) {
            g2.setFont(titleFont);
            g2.setColor(Color.WHITE);
            String title = currentType.toString();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, 16, 34);

            if (currentEvent != null) {
                g2.setFont(hudFont);
                int y = 60;
                g2.setColor(new Color(220, 220, 225));
                for (String line : currentEvent.getHUDStrings()) {
                    g2.drawString(line, 16, y);
                    y += 18;
                }
            }

            // Pause/Focus warnings
            if (paused) {
                String text = "PAUSED - Press P to resume";
                drawCenteredBanner(g2, text, new Color(0, 0, 0, 160), Color.WHITE, 20, 44);
            }
            if (!hasFocus) {
                String text = "CLICK TO FOCUS";
                drawCenteredBanner(g2, text, new Color(0, 0, 0, 180), Color.ORANGE, 20, 44);
            }
        }

        private void drawCenteredBanner(Graphics2D g2, String text, Color bg, Color fg, int vPad, int radius) {
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            int th = fm.getAscent();
            int bw = tw + 40;
            int bh = th + vPad;
            int x = (getWidth() - bw) / 2;
            int y = (getHeight() - bh) / 2;

            g2.setColor(bg);
            g2.fillRoundRect(x, y, bw, bh, radius, radius);
            g2.setColor(fg);
            g2.drawString(text, x + (bw - tw) / 2, y + (bh + th) / 2 - 6);
        }

        private void drawDebug(Graphics2D g2) {
            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            int y = getHeight() - 90;
            int x = 12;
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(x - 6, y - 14, 330, 90, 10, 10);
            g2.setColor(Color.GREEN);
            g2.drawString("Debug:", x, y);
            g2.setColor(Color.WHITE);
            g2.drawString("Event: " + currentType, x, y + 16);
            g2.drawString("Paused: " + paused, x, y + 32);
            g2.drawString("Mouse: " + mousePos.x + "," + mousePos.y + (mouseDown ? " [down]" : ""), x, y + 48);
            g2.drawString("Focus: " + hasFocus, x, y + 64);
        }

        // Keyboard/mouse input
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            keysDown.add(e.getKeyCode());

            if (e.getKeyCode() == KeyEvent.VK_P) {
                togglePause();
            } else if (e.getKeyCode() == KeyEvent.VK_R) {
                resetEvent();
            } else if (e.getKeyCode() == KeyEvent.VK_N) {
                nextEvent();
            } else if (e.getKeyCode() == KeyEvent.VK_B) {
                prevEvent();
            } else if (e.getKeyCode() == KeyEvent.VK_F11) {
                Container top = SwingUtilities.getWindowAncestor(this);
                if (top instanceof JFrame) {
                    toggleFullscreen((JFrame) top);
                }
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE && fullscreen) {
                Container top = SwingUtilities.getWindowAncestor(this);
                if (top instanceof JFrame) {
                    toggleFullscreen((JFrame) top);
                }
            }

            if (currentEvent != null) {
                currentEvent.handleKeyPressed(e);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            keysDown.remove(e.getKeyCode());
            if (currentEvent != null) {
                currentEvent.handleKeyReleased(e);
            }
        }

        private void nextEvent() {
            SportType[] vals = SportType.values();
            int idx = Arrays.asList(vals).indexOf(currentType);
            idx = (idx + 1) % vals.length;
            switchEvent(vals[idx]);
        }

        private void prevEvent() {
            SportType[] vals = SportType.values();
            int idx = Arrays.asList(vals).indexOf(currentType);
            idx = (idx - 1 + vals.length) % vals.length;
            switchEvent(vals[idx]);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (!hasFocus) requestFocusInWindow();
            if (currentEvent != null) currentEvent.handleMouseClicked(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            mouseDown = true;
            if (currentEvent != null) currentEvent.handleMousePressed(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mouseDown = false;
            if (currentEvent != null) currentEvent.handleMouseReleased(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}

        @Override
        public void mouseDragged(MouseEvent e) {
            mousePos = e.getPoint();
            if (currentEvent != null) currentEvent.handleMouseMoved(e, true);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mousePos = e.getPoint();
            if (currentEvent != null) currentEvent.handleMouseMoved(e, false);
        }

        @Override
        public void focusGained(FocusEvent e) {
            hasFocus = true;
        }

        @Override
        public void focusLost(FocusEvent e) {
            hasFocus = false;
        }

        public boolean isKeyDown(int keyCode) {
            return keysDown.contains(keyCode);
        }

        public Random rng() {
            return rng;
        }
    }

    // Interface for sport events
    public interface SportEvent {
        void reset();
        void start();
        void update(double dt);
        void render(Graphics2D g2, int width, int height);
        boolean isFinished();
        List<String> getHUDStrings();
        void handleKeyPressed(KeyEvent e);
        void handleKeyReleased(KeyEvent e);
        void handleMouseMoved(MouseEvent e, boolean dragged);
        void handleMousePressed(MouseEvent e);
        void handleMouseReleased(MouseEvent e);
        void handleMouseClicked(MouseEvent e);
        String getName();
    }

    // Scoreboard to persist bests across events
    public static class Scoreboard {
        private final Map<String, Double> bestTimes = new HashMap<>();
        private final Map<String, Double> bestDistances = new HashMap<>();
        private final Map<String, String> holderTime = new HashMap<>();
        private final Map<String, String> holderDistance = new HashMap<>();

        public synchronized void registerTime(String event, String athlete, double timeSeconds) {
            Double best = bestTimes.get(event);
            if (best == null || timeSeconds < best) {
                bestTimes.put(event, timeSeconds);
                holderTime.put(event, athlete);
            }
        }

        public synchronized void registerDistance(String event, String athlete, double distanceMeters) {
            Double best = bestDistances.get(event);
            if (best == null || distanceMeters > best) {
                bestDistances.put(event, distanceMeters);
                holderDistance.put(event, athlete);
            }
        }

        public synchronized String bestTimeLine(String event) {
            Double best = bestTimes.get(event);
            String who = holderTime.get(event);
            if (best == null) return "Best: --";
            return String.format("Best: %.3fs by %s", best, who);
        }

        public synchronized String bestDistanceLine(String event) {
            Double best = bestDistances.get(event);
            String who = holderDistance.get(event);
            if (best == null) return "Best: --";
            return String.format("Best: %.2fm by %s", best, who);
        }
    }

    // Basic athlete
    public static class Athlete {
        public String name;
        public Color color;
        public double staminaMax = 100;
        public double stamina = 100;
        public double baseSpeed; // m/s
        public double skill; // 0..1
        public double reaction; // 0..1 (lower is slower reaction in sprint start)
        public int lane;

        public Athlete(String name, Color color, double baseSpeed, double skill, double reaction, int lane) {
            this.name = name;
            this.color = color;
            this.baseSpeed = baseSpeed;
            this.skill = skill;
            this.reaction = reaction;
            this.lane = lane;
        }

        public void recover(double dt, double rate) {
            stamina = Math.min(staminaMax, stamina + rate * dt);
        }

        public Color darker() {
            return color.darker();
        }
    }

    // Utility: random athlete names and colors
    public static class AthleteFactory {
        private static final String[] FIRST = {
                "Alex", "Sam", "Jordan", "Taylor", "Chris", "Jamie", "Avery", "Casey", "Dakota", "Riley",
                "Morgan", "Quinn", "Harper", "Skyler", "Emerson", "Parker", "Reese", "Rowan", "Shay", "Jules"
        };
        private static final String[] LAST = {
                "Nguyen", "Kim", "Garcia", "Lopez", "Patel", "Singh", "Williams", "Brown", "Ivanov", "Kowalski",
                "Schmidt", "Dubois", "Rossi", "Silva", "Hernandez", "Khan", "Haddad", "O'Neil", "Sato", "Yamada"
        };

        public static Athlete create(Random rng, int lane, boolean isPlayer) {
            String name = isPlayer ? "You" : FIRST[rng.nextInt(FIRST.length)] + " " + LAST[rng.nextInt(LAST.length)];
            Color color = isPlayer ? new Color(60, 180, 255) : rainbowColor(rng, 0.6f);
            double base = 8.0 + rng.nextDouble() * 3.0; // m/s typical
            double skill = 0.55 + rng.nextDouble() * 0.4;
            double reaction = 0.15 + rng.nextDouble() * 0.35;
            if (isPlayer) {
                base += 0.2;
                skill += 0.05;
                reaction -= 0.05;
            }
            return new Athlete(name, color, base, clamp(skill, 0.0, 1.0), clamp(reaction, 0.05, 0.6), lane);
        }

        private static Color rainbowColor(Random rng, float sat) {
            float h = rng.nextFloat();
            return Color.getHSBColor(h, sat, 0.95f);
        }
    }

    // 100m Sprint Event
    public static class SprintEvent implements SportEvent {
        private final GamePanel gp;
        private final Scoreboard scoreboard;
        private final Random rng;

        // Track setup
        private int lanes = 6;
        private double distanceMeters = 100.0;
        private double trackPadding = 20;
        private double laneHeight = 70;
        private double metersToPixels = 8;
        private double trackVisualWidth;

        // Athletes
        private List<Athlete> athletes = new ArrayList<>();
        private int playerIdx = 0;
        private double[] xMeters; // distance progressed
        private double[] v;       // m/s
        private boolean started = false;
        private boolean finished = false;
        private boolean falseStart = false;
        private double time = 0.0;
        private double startGunTime = 0.0; // time when gun fired
        private boolean gunFired = false;

        // Input rhythm for player (alternate L/R)
        private boolean expectLeft = true;
        private double cadenceBonus = 0.0; // transient on good alternation
        private double perfectWindow = 0.14; // time window
        private double lastKeyTime = -10;

        // AI cadence/time offsets
        private double[] aiCadence; // frequency of steps per sec influence
        private double[] aiStartDelay; // reaction time offsets

        // Camera/animation
        private double cameraOffsetPixels = 0;

        // Results
        private final List<Result> results = new ArrayList<>();

        public SprintEvent(GamePanel gp, Scoreboard sb) {
            this.gp = gp;
            this.scoreboard = sb;
            this.rng = gp.rng();
            setup();
        }

        private void setup() {
            // Create athletes
            athletes.clear();
            lanes = 6;
            for (int i = 0; i < lanes; i++) {
                boolean isPlayer = (i == 2); // middle lane for player
                Athlete a = AthleteFactory.create(rng, i, isPlayer);
                if (isPlayer) playerIdx = i;
                a.staminaMax = 100;
                a.stamina = a.staminaMax;
                athletes.add(a);
            }
            xMeters = new double[lanes];
            v = new double[lanes];
            Arrays.fill(xMeters, 0);
            Arrays.fill(v, 0);

            aiCadence = new double[lanes];
            aiStartDelay = new double[lanes];
            for (int i = 0; i < lanes; i++) {
                aiCadence[i] = 3.0 + rng.nextDouble() * 3.0;
                aiStartDelay[i] = athletes.get(i).reaction + rng.nextDouble() * 0.1;
            }
            aiStartDelay[playerIdx] = 0.10 + rng.nextDouble() * 0.05; // player best reaction potential

            started = false;
            finished = false;
            falseStart = false;
            time = 0;
            gunFired = false;
            startGunTime = 0;
            lastKeyTime = -10;
            expectLeft = true;
            cadenceBonus = 0;

            results.clear();
        }

        @Override
        public void reset() {
            setup();
        }

        @Override
        public void start() {
            started = true;
            finished = false;
            falseStart = false;
            time = 0;
            gunFired = false;
            startGunTime = 0;
        }

        @Override
        public void update(double dt) {
            if (!started || finished) return;

            time += dt;

            // Start gun after random delay
            if (!gunFired) {
                if (startGunTime == 0) {
                    startGunTime = time + (0.8 + rng.nextDouble() * 1.2); // 0.8..2.0 seconds after start pressed
                }
                if (time >= startGunTime) {
                    gunFired = true;
                } else {
                    // False start detection: if player tries to move before gun
                    if (Math.abs(v[playerIdx]) > 0.2 || xMeters[playerIdx] > 0.1 || cadenceBonus > 0.01) {
                        falseStart = true;
                        finished = true;
                        results.add(new Result(athletes.get(playerIdx), time, "False Start"));
                        return;
                    }
                }
            }

            // Constants
            double drag = 0.10; // minor velocity damp
            double staminaUse = 12.0; // per second at max push
            double speedBoostFromCadence = Math.min(1.6, cadenceBonus);
            cadenceBonus = Math.max(0, cadenceBonus - dt * 1.4); // decays

            for (int i = 0; i < lanes; i++) {
                Athlete a = athletes.get(i);

                // Determine push
                double push = 0.0;
                if (i == playerIdx) {
                    if (!gunFired) {
                        // cannot push before gun, keep 0
                        push = 0.0;
                    } else {
                        // push is interactive: alternating keys raise push scaled by stamina
                        push = Math.min(1.0, speedBoostFromCadence + (gp.isKeyDown(KeyEvent.VK_SHIFT) ? 0.35 : 0));
                    }
                } else {
                    // AI logic: ramp from reaction delay, cadence noise
                    if (gunFired && time > startGunTime + aiStartDelay[i]) {
                        double t = time - (startGunTime + aiStartDelay[i]);
                        double cadence = aiCadence[i] + Math.sin(time * (0.8 + 0.4 * i)) * 0.5;
                        double ramp = clamp(t / 0.7, 0, 1);
                        push = clamp(0.35 + 0.65 * Math.abs(Math.sin(time * cadence * 0.5 + i)), 0, 1) * ramp;
                        push += a.skill * 0.05;
                    } else {
                        push = 0.0;
                    }
                }

                // Stamina and speed
                double staminaFactor = clamp(a.stamina / a.staminaMax, 0.1, 1.0);
                double targetSpeed = a.baseSpeed * (0.7 + 0.45 * push) * staminaFactor;
                if (i == playerIdx && push > 0.01) {
                    a.stamina -= staminaUse * push * dt * (gp.isKeyDown(KeyEvent.VK_SHIFT) ? 1.35 : 1.0);
                } else if (i != playerIdx) {
                    a.stamina -= staminaUse * 0.65 * push * dt;
                }

                a.stamina = Math.max(0, a.stamina);
                if (a.stamina < 15) {
                    targetSpeed *= 0.85;
                }

                // Velocity approach to target
                double accel = 6.0; // m/s^2 simplified
                if (!gunFired) targetSpeed = 0;
                v[i] += clamp(targetSpeed - v[i], -accel * dt, accel * dt);
                v[i] = Math.max(0, v[i] - drag * dt);

                xMeters[i] += v[i] * dt;

                // Finish detection
                if (xMeters[i] >= distanceMeters && !containsAthleteInResults(athletes.get(i))) {
                    results.add(new Result(athletes.get(i), time, null));
                    if (i == playerIdx) {
                        scoreboard.registerTime(getName(), athletes.get(i).name, time);
                    }
                }
            }

            // If all finished or a false start ended it
            if (results.size() >= lanes || falseStart) {
                finished = true;
                sortResultsByTime();
            }

            // Camera: follow player
            double finishPixel = distanceMeters * metersToPixels;
            trackVisualWidth = Math.max(finishPixel + 300, gp.getWidth() - 100);
            double px = xMeters[playerIdx] * metersToPixels;
            double centerTarget = px - gp.getWidth() * 0.35;
            centerTarget = clamp(centerTarget, -40, trackVisualWidth - gp.getWidth() + 40);
            cameraOffsetPixels += (centerTarget - cameraOffsetPixels) * 0.12;
        }

        private boolean containsAthleteInResults(Athlete a) {
            for (Result r : results) if (r.athlete == a) return true;
            return false;
        }

        private void sortResultsByTime() {
            results.sort(Comparator.comparingDouble(r -> r.time));
        }

        @Override
        public void render(Graphics2D g2, int width, int height) {
            // Track area
            int margin = 80;
            int trackTop = margin + 30;
            int trackBottom = height - margin;
            double trackHeight = trackBottom - trackTop;
            laneHeight = trackHeight / lanes;

            // Draw stadium gradient
            drawStadiumBackdrop(g2, width, height);

            // Draw track lanes
            drawTrack(g2, width, trackTop, trackBottom);

            // Draw start/finish lines
            double startX = -cameraOffsetPixels + 120;
            double finishX = -cameraOffsetPixels + 120 + distanceMeters * metersToPixels;
            drawLineWithLabel(g2, startX, trackTop, trackBottom, "START", new Color(230, 230, 230), new Color(50, 50, 50));
            drawLineWithLabel(g2, finishX, trackTop, trackBottom, "FINISH", new Color(230, 230, 230), new Color(50, 50, 50));

            // Draw athletes as rectangles on their lanes
            for (int i = 0; i < lanes; i++) {
                Athlete a = athletes.get(i);
                double x = -cameraOffsetPixels + 120 + xMeters[i] * metersToPixels;
                double laneY = trackTop + i * laneHeight + laneHeight * 0.1;
                double h = laneHeight * 0.8;
                double w = 34;

                // Trail/ghost for speed
                float alpha = (float) clamp(v[i] / 12.0, 0.2, 0.9);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.3f));
                g2.setColor(a.color.darker());
                g2.fill(new RoundRectangle2D.Double(x - 10, laneY + 8, w + 20, h - 16, 10, 10));
                g2.setComposite(AlphaComposite.SrcOver);

                g2.setColor(a.color);
                g2.fill(new RoundRectangle2D.Double(x, laneY, w, h, 14, 14));

                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Double(x, laneY, w, h, 14, 14));

                // Name and speed
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString(a.name, (int) x + 6, (int) (laneY + 16));
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2.drawString(String.format("%.1f m/s", v[i]), (int) x + 6, (int) (laneY + h - 8));

                // Stamina bar
                drawBar(g2, (int) x + 4, (int) (laneY + h + 6), 80, 8, a.stamina / a.staminaMax, new Color(60, 200, 90), new Color(60, 90, 60));
            }

            // Start gun indicator
            drawStartGunIndicator(g2, width, trackTop);

            // If finished, show results board
            if (finished) {
                drawSprintResults(g2, width, height);
            }

            // Player cadence helper
            if (!finished) {
                drawCadenceHelper(g2, width, height);
            }
        }

        private void drawStadiumBackdrop(Graphics2D g2, int w, int h) {
            GradientPaint gp = new GradientPaint(0, h * 0.2f, new Color(30, 30, 37), 0, h, new Color(12, 12, 18));
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            // Crowd dots
            Random lrng = new Random(1234);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
            for (int i = 0; i < 550; i++) {
                int x = lrng.nextInt(w);
                int y = (int) (lrng.nextDouble() * h * 0.22 + 40);
                int s = lrng.nextInt(2) + 1;
                g2.setColor(new Color(80 + lrng.nextInt(120), 60 + lrng.nextInt(140), 60 + lrng.nextInt(120)));
                g2.fillRect(x, y, s, s);
            }
            g2.setComposite(AlphaComposite.SrcOver);
        }

        private void drawTrack(Graphics2D g2, int width, int trackTop, int trackBottom) {
            // Track base
            g2.setColor(new Color(180, 70, 60));
            g2.fillRect(0, trackTop, width, trackBottom - trackTop);

            // Lanes
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            for (int i = 0; i <= lanes; i++) {
                int y = (int) (trackTop + i * laneHeight);
                g2.drawLine(0, y, width, y);
            }
        }

        private void drawLineWithLabel(Graphics2D g2, double x, int top, int bottom, String label, Color lineColor, Color labelBg) {
            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine((int) x, top, (int) x, bottom);

            // Label box
            int w = 70;
            int h = 22;
            int lx = (int) (x - w / 2);
            int ly = top - h - 8;
            g2.setColor(labelBg);
            g2.fillRoundRect(lx, ly, w, h, 8, 8);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(label);
            g2.drawString(label, lx + (w - tw) / 2, ly + h - 7);
        }

        private void drawBar(Graphics2D g2, int x, int y, int w, int h, double pct, Color good, Color bad) {
            pct = clamp(pct, 0, 1);
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(x - 2, y - 2, w + 4, h + 4, 8, 8);

            GradientPaint gp = new GradientPaint(x, y, good, x, y + h, bad);
            g2.setPaint(gp);
            g2.fillRoundRect(x, y, (int) (w * pct), h, 6, 6);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRoundRect(x, y, w, h, 6, 6);
        }

        private void drawStartGunIndicator(Graphics2D g2, int width, int trackTop) {
            String s;
            Color c;
            if (!started) {
                s = "Press Start to arm the blocks";
                c = Color.LIGHT_GRAY;
            } else if (!gunFired) {
                s = "Set... Hold steady...";
                c = Color.YELLOW;
            } else {
                s = "Go!";
                c = Color.GREEN;
            }
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(s);
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect((width - tw) / 2 - 16, trackTop - 50, tw + 32, 34, 12, 12);
            g2.setColor(c);
            g2.drawString(s, (width - tw) / 2, trackTop - 28);
        }

        private void drawSprintResults(Graphics2D g2, int width, int height) {
            int boxW = 420;
            int boxH = 40 + results.size() * 28 + 30;
            int x = width - boxW - 24;
            int y = 80;

            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(x, y, boxW, boxH, 16, 16);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString("Results - 100m Sprint", x + 16, y + 26);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            int yy = y + 52;
            int place = 1;
            for (Result r : results) {
                String line;
                if (r.penalty != null) {
                    line = String.format("%d. %-18s  %s", place, r.athlete.name, r.penalty);
                } else {
                    line = String.format("%d. %-18s  %.3f s", place, r.athlete.name, r.time);
                }
                g2.setColor(r.athlete.color);
                g2.fillOval(x + 14, yy - 12, 10, 10);
                g2.setColor(Color.WHITE);
                g2.drawString(line, x + 32, yy);
                place++;
                yy += 24;
            }

            // Best time
            g2.setColor(new Color(200, 200, 240));
            g2.drawString(scoreboard.bestTimeLine(getName()), x + 16, yy + 10);
        }

        private void drawCadenceHelper(Graphics2D g2, int width, int height) {
            // Indicator bottom center
            int cx = width / 2;
            int cy = height - 70;
            int w = 240;
            int h = 14;
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(cx - w / 2 - 6, cy - h / 2 - 6, w + 12, h + 12, 12, 12);

            // Fill shows current cadenceBonus
            double pct = clamp(cadenceBonus / 1.6, 0, 1);
            GradientPaint gp = new GradientPaint(cx - w / 2, cy, new Color(60, 180, 255), cx + w / 2, cy, new Color(40, 120, 220));
            g2.setPaint(gp);
            g2.fillRoundRect(cx - w / 2, cy - h / 2, (int) (w * pct), h, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 10, 10);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            String tip = "Alternate Left/Right arrows. Hold Shift to boost (consumes stamina).";
            int tw = g2.getFontMetrics().stringWidth(tip);
            g2.drawString(tip, cx - tw / 2, cy - 16);
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public List<String> getHUDStrings() {
            List<String> s = new ArrayList<>();
            s.add("Controls (Sprint): Alternate Left/Right arrows. Shift for brief boost. Space for perfect start.");
            s.add("Status: " + (started ? (gunFired ? "Racing" : "Waiting for gun") : "Not started"));
            if (finished) {
                if (falseStart) {
                    s.add("Result: False Start");
                } else {
                    Optional<Result> me = results.stream().filter(r -> r.athlete == athletes.get(playerIdx)).findFirst();
                    me.ifPresent(result -> s.add(String.format("Your time: %s",
                            result.penalty != null ? result.penalty : String.format("%.3f s", result.time))));
                }
            }
            return s;
        }

        @Override
        public void handleKeyPressed(KeyEvent e) {
            if (finished) return;
            if (!started) return;

            if (!gunFired) {
                // Perfect start attempt
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    // If pressed within small window after gun, we reward, else ignore; but before gun it's false start handled in update
                }
            } else {
                // Handle alternating keys
                if (e.getKeyCode() == KeyEvent.VK_LEFT && expectLeft) {
                    registerStep();
                    expectLeft = false;
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && !expectLeft) {
                    registerStep();
                    expectLeft = true;
                }
            }
        }

        private void registerStep() {
            double now = time;
            double dt = now - lastKeyTime;
            lastKeyTime = now;
            // Reward cadence close to sweet spot
            double ideal = 0.22; // seconds between alternating presses
            double diff = Math.abs(dt - ideal);
            double score = clamp(1.0 - diff / perfectWindow, 0, 1);
            cadenceBonus = Math.min(1.6, cadenceBonus + 0.35 * score + 0.1);
        }

        @Override
        public void handleKeyReleased(KeyEvent e) {}

        @Override
        public void handleMouseMoved(MouseEvent e, boolean dragged) {}

        @Override
        public void handleMousePressed(MouseEvent e) {}

        @Override
        public void handleMouseReleased(MouseEvent e) {}

        @Override
        public void handleMouseClicked(MouseEvent e) {}

        @Override
        public String getName() { return "100m Sprint"; }

        private static class Result {
            Athlete athlete;
            double time;
            String penalty; // e.g., "False Start"
            public Result(Athlete a, double time, String pen) {
                this.athlete = a;
                this.time = time;
                this.penalty = pen;
            }
        }
    }

    // Swimming freestyle event
    public static class SwimmingEvent implements SportEvent {
        private final GamePanel gp;
        private final Scoreboard scoreboard;
        private final Random rng;

        // Pool setup
        private int lanes = 6;
        private double distanceMeters = 100.0; // 2x50 pool
        private double metersToPixels = 7.5;
        private double poolPadding = 40;
        private double laneHeight;
        private double poolVisualWidth;

        private List<Athlete> athletes = new ArrayList<>();
        private int playerIdx = 0;
        private double[] xMeters; // progress
        private double[] v; // m/s
        private double[] laneOffset; // -1..1 for lateral drift
        private double time = 0;
        private boolean started = false;
        private boolean finished = false;

        // Rhythm-based input
        private double beatTime = 0.55; // time between strokes ideal
        private double sinceStroke = 0; // time since last space
        private double strokeMeter = 0; // accumulates with good timing
        private double strokeWindow = 0.18;

        // Camera
        private double cameraOffsetPixels = 0;

        // Results
        private final List<Result> results = new ArrayList<>();

        public SwimmingEvent(GamePanel gp, Scoreboard sb) {
            this.gp = gp;
            this.scoreboard = sb;
            this.rng = gp.rng();
            setup();
        }

        private void setup() {
            athletes.clear();
            lanes = 6;
            for (int i = 0; i < lanes; i++) {
                boolean isPlayer = (i == 2);
                Athlete a = AthleteFactory.create(rng, i, isPlayer);
                if (isPlayer) playerIdx = i;
                a.staminaMax = 120;
                a.stamina = 120;
                a.baseSpeed = 1.8 + rng.nextDouble() * 0.6; // swimming speed baseline ~ 2 m/s
                athletes.add(a);
            }
            xMeters = new double[lanes];
            v = new double[lanes];
            laneOffset = new double[lanes];
            Arrays.fill(xMeters, 0);
            Arrays.fill(v, 0);
            Arrays.fill(laneOffset, 0);

            time = 0;
            started = false;
            finished = false;
            strokeMeter = 0;
            sinceStroke = 0;

            results.clear();
        }

        @Override
        public void reset() {
            setup();
        }

        @Override
        public void start() {
            started = true;
            finished = false;
            time = 0;
            sinceStroke = 0;
            strokeMeter = 0;
        }

        @Override
        public void update(double dt) {
            if (!started || finished) return;
            time += dt;
            sinceStroke += dt;

            double drag = 0.25;
            double strokeBoost = clamp(strokeMeter / 3.0, 0, 1); // 0..1
            strokeMeter = Math.max(0, strokeMeter - dt * 0.8);

            for (int i = 0; i < lanes; i++) {
                Athlete a = athletes.get(i);

                double push;
                if (i == playerIdx) {
                    // Based on strokeMeter and stamina
                    double staminaFactor = clamp(a.stamina / a.staminaMax, 0.2, 1.0);
                    push = (0.5 + 0.7 * strokeBoost) * staminaFactor;
                    // Steering minor friction if near ropes
                    if (gp.isKeyDown(KeyEvent.VK_LEFT)) laneOffset[i] -= dt * 0.9;
                    if (gp.isKeyDown(KeyEvent.VK_RIGHT)) laneOffset[i] += dt * 0.9;
                    laneOffset[i] = clamp(laneOffset[i], -1, 1);
                    if (Math.abs(laneOffset[i]) > 0.85) push *= 0.82; // rubbing rope
                    a.stamina -= dt * (8 + 10 * strokeBoost);
                    a.stamina = Math.max(0, a.stamina);
                } else {
                    // AI: simple sinusoidal strokes
                    double cadence = 2.0 + rng.nextDouble() * 0.3;
                    double p = Math.abs(Math.sin(time * cadence + i)) * (0.6 + 0.4 * a.skill);
                    push = clamp(0.5 + 0.5 * p, 0, 1);
                    laneOffset[i] += Math.sin(time * 0.5 + i) * 0.1 * dt;
                    laneOffset[i] = clamp(laneOffset[i], -0.9, 0.9);
                    a.stamina -= dt * (8 + 6 * p);
                    a.stamina = Math.max(0, a.stamina);
                }

                double targetSpeed = a.baseSpeed * (0.6 + 0.9 * push);
                if (a.stamina < 20) targetSpeed *= 0.85;
                v[i] += (targetSpeed - v[i]) * clamp(dt * 2.5, 0, 1);
                v[i] = Math.max(0, v[i] - drag * dt);

                xMeters[i] += v[i] * dt;

                if (xMeters[i] >= distanceMeters && !containsAthlete(athletes.get(i))) {
                    results.add(new Result(athletes.get(i), time));
                    if (i == playerIdx) {
                        scoreboard.registerTime(getName(), athletes.get(i).name, time);
                    }
                }
            }

            if (results.size() >= lanes) {
                finished = true;
                results.sort(Comparator.comparingDouble(r -> r.time));
            }

            // Camera
            double finishPixel = distanceMeters * metersToPixels;
            poolVisualWidth = Math.max(finishPixel + 300, gp.getWidth() - 100);
            double px = xMeters[playerIdx] * metersToPixels;
            double centerTarget = px - gp.getWidth() * 0.3;
            centerTarget = clamp(centerTarget, -40, poolVisualWidth - gp.getWidth() + 40);
            cameraOffsetPixels += (centerTarget - cameraOffsetPixels) * 0.12;
        }

        private boolean containsAthlete(Athlete a) {
            for (Result r : results) if (r.athlete == a) return true;
            return false;
        }

        @Override
        public void render(Graphics2D g2, int width, int height) {
            int margin = 70;
            int poolTop = margin + 30;
            int poolBottom = height - margin;
            double poolHeight = poolBottom - poolTop;
            laneHeight = poolHeight / lanes;

            // Pool water background with subtle waves
            drawPoolBackground(g2, width, height, poolTop, poolBottom);

            // Lane ropes
            for (int i = 0; i <= lanes; i++) {
                int y = (int) (poolTop + i * laneHeight);
                drawLaneRope(g2, 0, y, width);
            }

            // Start/finish flags
            double startX = -cameraOffsetPixels + 120;
            double finishX = -cameraOffsetPixels + 120 + distanceMeters * metersToPixels;
            drawFlag(g2, startX, poolTop, poolBottom, "START", new Color(240, 240, 240));
            drawFlag(g2, finishX, poolTop, poolBottom, "FINISH", new Color(240, 240, 240));

            // Swimmers
            for (int i = 0; i < lanes; i++) {
                Athlete a = athletes.get(i);
                double x = -cameraOffsetPixels + 120 + xMeters[i] * metersToPixels;
                double laneY = poolTop + i * laneHeight;
                double y = laneY + laneHeight * 0.5 + laneOffset[i] * laneHeight * 0.4;

                // Wake effect
                g2.setColor(new Color(255, 255, 255, 30));
                for (int k = 0; k < 5; k++) {
                    int rx = (int) (x - 24 - k * 8);
                    int ry = (int) (y - 8 + Math.sin((x + k * 12) * 0.06 + i) * 4);
                    g2.drawArc(rx, ry, 48, 16, 0, 180);
                }

                // Swimmer body
                Shape body = new RoundRectangle2D.Double(x - 18, y - 8, 36, 16, 10, 10);
                g2.setColor(a.color);
                g2.fill(body);
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(body);

                // Name and speed
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString(a.name, (int) (x - 18), (int) (laneY + 16));
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2.drawString(String.format("%.2f m/s", v[i]), (int) (x - 18), (int) (y + 24));

                // Stamina bar
                drawBar(g2, (int) (x - 40), (int) (y + 30), 110, 8, a.stamina / a.staminaMax, new Color(60, 200, 90), new Color(60, 90, 60));
            }

            // Stroke meter
            if (!finished) drawStrokeMeter(g2, width, height);

            // Results
            if (finished) drawSwimmingResults(g2, width, height);
        }

        private void drawBar(Graphics2D g2, int i, int i1, int i2, int i3, double v, Color color, Color color1) {
        }

        private void drawPoolBackground(Graphics2D g2, int width, int height, int poolTop, int poolBottom) {
            // Pool base
            GradientPaint gp = new GradientPaint(0, poolTop, new Color(20, 120, 180), 0, poolBottom, new Color(10, 80, 130));
            g2.setPaint(gp);
            g2.fillRect(0, poolTop, width, poolBottom - poolTop);

            // Subtle waves overlay
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
            g2.setColor(Color.WHITE);
            for (int y = poolTop + 8; y < poolBottom; y += 16) {
                int amplitude = 8;
                Path2D wave = new Path2D.Double();
                wave.moveTo(0, y);
                for (int x = 0; x < width; x += 12) {
                    int yy = (int) (y + Math.sin((x + y * 0.2 + time * 50) * 0.06) * amplitude);
                    wave.lineTo(x, yy);
                }
                g2.draw(wave);
            }
            g2.setComposite(AlphaComposite.SrcOver);
        }

        private void drawLaneRope(Graphics2D g2, int x0, int y, int width) {
            for (int x = x0; x < width; x += 20) {
                g2.setColor(new Color(255, 200, 0));
                g2.fillRect(x, y - 2, 10, 4);
                g2.setColor(new Color(255, 60, 60));
                g2.fillRect(x + 10, y - 2, 10, 4);
            }
        }

        private void drawFlag(Graphics2D g2, double x, int top, int bottom, String label, Color col) {
            g2.setColor(col);
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine((int) x, top, (int) x, bottom);

            // Flag label
            int w = 70;
            int h = 22;
            int lx = (int) (x - w / 2);
            int ly = top - h - 8;
            g2.setColor(new Color(10, 10, 10, 140));
            g2.fillRoundRect(lx, ly, w, h, 8, 8);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(label);
            g2.drawString(label, lx + (w - tw) / 2, ly + h - 7);
        }

        private void drawStrokeMeter(Graphics2D g2, int width, int height) {
            int cx = width / 2;
            int cy = height - 70;
            int w = 280;
            int h = 14;

            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(cx - w / 2 - 6, cy - h / 2 - 26, w + 12, h + 54, 12, 12);

            // Beat visualization
            double phase = (sinceStroke % beatTime) / beatTime;
            int markerX = (int) (cx - w / 2 + phase * w);

            // Fill shows strokeMeter charged
            double pct = clamp(strokeMeter / 3.0, 0, 1);
            GradientPaint gp = new GradientPaint(cx - w / 2, cy, new Color(60, 220, 180), cx + w / 2, cy, new Color(30, 140, 110));
            g2.setPaint(gp);
            g2.fillRoundRect(cx - w / 2, cy - h / 2, (int) (w * pct), h, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 10, 10);

            // Beat marker
            g2.setColor(new Color(255, 255, 255, 180));
            g2.drawLine(markerX, cy - h / 2 - 10, markerX, cy + h / 2 + 10);

            // Tip
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            String tip = "Press Space in rhythm to build stroke power. Arrow keys steer.";
            int tw = g2.getFontMetrics().stringWidth(tip);
            g2.drawString(tip, cx - tw / 2, cy - 18);
        }

        private void drawSwimmingResults(Graphics2D g2, int width, int height) {
            int boxW = 420;
            int boxH = 40 + results.size() * 28 + 30;
            int x = width - boxW - 24;
            int y = 80;

            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(x, y, boxW, boxH, 16, 16);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString("Results - Swimming Freestyle", x + 16, y + 26);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            int yy = y + 52;
            int place = 1;
            for (Result r : results) {
                String line = String.format("%d. %-18s  %.3f s", place, r.athlete.name, r.time);
                g2.setColor(r.athlete.color);
                g2.fillOval(x + 14, yy - 12, 10, 10);
                g2.setColor(Color.WHITE);
                g2.drawString(line, x + 32, yy);
                place++;
                yy += 24;
            }

            // Best time
            g2.setColor(new Color(200, 200, 240));
            g2.drawString(scoreboard.bestTimeLine(getName()), x + 16, yy + 10);
        }

        @Override
        public List<String> getHUDStrings() {
            List<String> s = new ArrayList<>();
            s.add("Controls (Swim): Space on beat for strokes, Left/Right to steer within lane.");
            s.add("Status: " + (started ? (finished ? "Finished" : "Swimming") : "Not started"));
            if (finished) {
                Optional<Result> me = results.stream().filter(r -> r.athlete == athletes.get(playerIdx)).findFirst();
                me.ifPresent(result -> s.add(String.format("Your time: %.3f s", result.time)));
            }
            return s;
        }

        @Override
        public void handleKeyPressed(KeyEvent e) {
            if (finished) return;
            if (!started) return;
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                // Evaluate stroke timing
                double phase = Math.abs((sinceStroke % beatTime) - beatTime / 2.0);
                double diff = Math.abs(phase - beatTime / 2.0);
                double score = clamp(1.0 - (diff / strokeWindow), 0, 1);
                strokeMeter = Math.min(3.0, strokeMeter + 0.65 + score * 0.7);
                sinceStroke = 0;
            }
        }

        @Override
        public void handleKeyReleased(KeyEvent e) {}

        @Override
        public void handleMouseMoved(MouseEvent e, boolean dragged) {}

        @Override
        public void handleMousePressed(MouseEvent e) {}

        @Override
        public void handleMouseReleased(MouseEvent e) {}

        @Override
        public void handleMouseClicked(MouseEvent e) {}

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public String getName() { return "Swimming Freestyle"; }

        private static class Result {
            Athlete athlete;
            double time;
            public Result(Athlete a, double time) {
                this.athlete = a;
                this.time = time;
            }
        }
    }

    // Javelin Throw event
    public static class JavelinEvent implements SportEvent {
        private final GamePanel gp;
        private final Scoreboard scoreboard;
        private final Random rng;

        // Runway
        private double runwayLengthMeters = 35;
        private double metersToPixels = 16;
        private double foulLineX; // in meters from start
        private double sectorAngle = Math.toRadians(28); // typical javelin sector width

        // Player and AI throwers (3 rounds each)
        private List<Athlete> athletes = new ArrayList<>();
        private int playerIdx = 0;
        private int rounds = 3;
        private int currentRound = 1;
        private int throwerIndex = 0; // whose turn
        private boolean inRunup = false;
        private boolean inAim = false;
        private boolean inFlight = false;
        private boolean finished = false;
        private boolean started = false;

        // Run-up dynamics
        private double[] xMeters; // runup position per athlete (only active for current)
        private double[] v; // runup velocity
        private double cadence; // A/D or left/right cadence effect

        // Throw parameters
        private double chargePower = 0;
        private double maxChargePower = 1.0; // normalized
        private double aimAngle = Math.toRadians(32);
        private double aimAngleMin = Math.toRadians(10);
        private double aimAngleMax = Math.toRadians(45);
        private double wind = 0.0; // m/s assisting (x) or crosswind (y) simplified as x
        private Vec2 projectilePos = null; // in meters relative to foul line
        private Vec2 projectileVel = null;
        private double projectileStartX = 0; // absolute x (m)
        private double gravity = 9.81;

        // Results: store best of rounds
        private final Map<Athlete, Double> bestThrow = new HashMap<>();
        private final Map<Athlete, List<Double>> throwsByAthlete = new HashMap<>();

        public JavelinEvent(GamePanel gp, Scoreboard sb) {
            this.gp = gp;
            this.scoreboard = sb;
            this.rng = gp.rng();
            setup();
        }

        private void setup() {
            athletes.clear();
            for (int i = 0; i < 6; i++) {
                boolean isPlayer = (i == 2);
                Athlete a = AthleteFactory.create(rng, i, isPlayer);
                if (isPlayer) playerIdx = i;
                a.baseSpeed = 6 + rng.nextDouble() * 2.5; // run-up speed
                a.staminaMax = 80;
                a.stamina = 80;
                athletes.add(a);
                bestThrow.put(a, Double.NEGATIVE_INFINITY);
                throwsByAthlete.put(a, new ArrayList<>());
            }
            xMeters = new double[athletes.size()];
            v = new double[athletes.size()];
            Arrays.fill(xMeters, 0);
            Arrays.fill(v, 0);

            foulLineX = runwayLengthMeters; // end of runway
            currentRound = 1;
            throwerIndex = 0;
            inRunup = false;
            inAim = false;
            inFlight = false;
            started = false;
            finished = false;
            cadence = 0;
            chargePower = 0;
            aimAngle = Math.toRadians(32);
            wind = rng.nextDouble() * 6 - 3; // -3 to +3 m/s

            projectilePos = null;
            projectileVel = null;
        }

        @Override
        public void reset() {
            setup();
        }

        @Override
        public void start() {
            started = true;
            // Begin first thrower's attempt
            beginAttempt();
        }

        private void beginAttempt() {
            inRunup = true;
            inAim = false;
            inFlight = false;
            chargePower = 0;
            aimAngle = Math.toRadians(30 + rng.nextDouble() * 8);
            xMeters[throwerIndex] = 0;
            v[throwerIndex] = 0;
        }

        @Override
        public void update(double dt) {
            if (!started || finished) return;

            Athlete current = athletes.get(throwerIndex);

            if (inRunup) {
                // Run-up: player taps A/D or Left/Right; AI uses smooth ramp
                if (throwerIndex == playerIdx) {
                    double boost = clamp(Math.abs(cadence), 0, 1.0);
                    double targetV = current.baseSpeed * (0.6 + 0.7 * boost);
                    v[throwerIndex] += (targetV - v[throwerIndex]) * clamp(dt * 3.5, 0, 1);
                    cadence *= 0.9; // decays
                } else {
                    double t = System.nanoTime() * 1e-9;
                    double boost = 0.5 + 0.5 * Math.abs(Math.sin(t * 1.4 + throwerIndex));
                    double targetV = current.baseSpeed * (0.6 + 0.7 * boost);
                    v[throwerIndex] += (targetV - v[throwerIndex]) * clamp(dt * 3.5, 0, 1);
                }
                // Friction
                v[throwerIndex] = Math.max(0, v[throwerIndex] - dt * 0.3);

                xMeters[throwerIndex] += v[throwerIndex] * dt;

                // When near foul line, automatically enter aim phase (or if player holds mouse)
                double remaining = foulLineX - xMeters[throwerIndex];
                if (remaining < 4.0 || chargePower > 0.01) {
                    inRunup = false;
                    inAim = true;
                }
            } else if (inAim) {
                // Aim: charging power, adjusting angle by mouse Y; if release or near foul line -> launch
                if (throwerIndex == playerIdx) {
                    // Player aiming
                    // chargePower controlled in mousePressed/Released; here we can clamp
                    chargePower = clamp(chargePower, 0, maxChargePower);
                    // Auto-launch if too close to the line
                    if (foulLineX - xMeters[throwerIndex] < 0.7 && chargePower > 0.05) {
                        launch(current);
                    } else if (foulLineX - xMeters[throwerIndex] < 0.2) {
                        // Foul: stepped over line without throwing
                        recordThrow(current, Double.NEGATIVE_INFINITY);
                        proceedNextAttempt();
                    }
                } else {
                    // AI: automatic aim and throw with some noise
                    chargePower += dt * (0.9 + 0.2 * current.skill);
                    aimAngle += (rng.nextDouble() - 0.5) * dt * 0.4;
                    aimAngle = clamp(aimAngle, aimAngleMin, aimAngleMax);
                    if (chargePower >= 0.8 + rng.nextDouble() * 0.2 || (foulLineX - xMeters[throwerIndex]) < 1.1) {
                        launch(current);
                    }
                }

                // Continue walking slightly during aim
                double creep = Math.max(0, v[throwerIndex] - 0.6);
                xMeters[throwerIndex] += creep * dt;
            } else if (inFlight) {
                // Projectile motion with wind as constant horizontal acceleration
                if (projectilePos != null && projectileVel != null) {
                    projectileVel.x += wind * 0.2 * dt; // simplified wind effect
                    projectileVel.y -= gravity * dt;
                    projectilePos = projectilePos.add(projectileVel.scl(dt));
                    // Check ground (y <= 0)
                    if (projectilePos.y <= 0) {
                        double dist = Math.max(0, projectilePos.x);
                        recordThrow(current, dist);
                        proceedNextAttempt();
                    }
                }
            } else {
                // Between attempts: small idle, handled by proceedNextAttempt
            }

            // Check finish condition
            if (currentRound > rounds) {
                finished = true;
                // Register scoreboard for player
                Double pb = bestThrow.get(athletes.get(playerIdx));
                if (pb != null && pb > 0) {
                    scoreboard.registerDistance(getName(), athletes.get(playerIdx).name, pb);
                }
            }
        }

        private void launch(Athlete current) {
            inAim = false;
            inFlight = true;
            // Starting from foul line offset
            projectileStartX = xMeters[throwerIndex];
            double speed = 18 + 10 * chargePower + current.skill * 2.0; // m/s
            double vx = speed * Math.cos(aimAngle);
            double vy = speed * Math.sin(aimAngle);
            // Position relative to foul line
            double relX = projectileStartX - foulLineX;
            projectilePos = new Vec2(relX, 1.6); // release height ~1.6m
            projectileVel = new Vec2(vx, vy);
        }

        private void recordThrow(Athlete a, double dist) {
            // Determine if foul: out of sector or negative distance
            boolean foul = false;
            // Sector check simplified: assume straight throw; no crosswind y; if angle too off? We'll skip angular check visually
            if (dist <= 0.5) foul = true;

            if (!foul) {
                double prev = bestThrow.getOrDefault(a, Double.NEGATIVE_INFINITY);
                bestThrow.put(a, Math.max(prev, dist));
                throwsByAthlete.get(a).add(dist);
            } else {
                throwsByAthlete.get(a).add(Double.NEGATIVE_INFINITY);
            }
        }

        private void proceedNextAttempt() {
            inRunup = false;
            inAim = false;
            inFlight = false;
            projectilePos = null;
            projectileVel = null;
            chargePower = 0;
            // Next thrower
            throwerIndex++;
            if (throwerIndex >= athletes.size()) {
                throwerIndex = 0;
                currentRound++;
                if (currentRound > rounds) {
                    // event finished
                    return;
                }
            }
            beginAttempt();
        }

        @Override
        public void render(Graphics2D g2, int width, int height) {
            // Background field
            drawField(g2, width, height);

            // Runway
            int margin = 80;
            int groundY = height - margin;
            double startX = 120;

            // Foul line
            double foulLinePx = startX + foulLineX * metersToPixels;
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine((int) foulLinePx, groundY - 120, (int) foulLinePx, groundY);

            // Sector lines
            g2.setColor(new Color(255, 255, 255, 130));
            int sectorLen = 800;
            double leftAngle = Math.PI - sectorAngle / 2.0;
            double rightAngle = Math.PI + sectorAngle / 2.0;
            int x0 = (int) foulLinePx;
            int y0 = groundY;
            int leftX = (int) (x0 + Math.cos(leftAngle) * sectorLen);
            int leftY = (int) (y0 + Math.sin(leftAngle) * sectorLen);
            int rightX = (int) (x0 + Math.cos(rightAngle) * sectorLen);
            int rightY = (int) (y0 + Math.sin(rightAngle) * sectorLen);
            g2.drawLine(x0, y0, leftX, leftY);
            g2.drawLine(x0, y0, rightX, rightY);

            // Draw athletes on runway (only active is moving)
            for (int i = 0; i < athletes.size(); i++) {
                Athlete a = athletes.get(i);
                double x = startX + xMeters[i] * metersToPixels;
                double y = groundY - 20 - (i == throwerIndex ? 0 : 14);
                Shape body = new RoundRectangle2D.Double(x - 10, y - 20, 28, 40, 10, 10);
                g2.setColor(a.color);
                g2.fill(body);
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(body);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString(a.name + (i == throwerIndex ? " (Throwing)" : ""), (int) (x - 12), (int) (y - 26));
            }

            // Javelin flight
            if (inFlight && projectilePos != null) {
                // Convert javelin position to screen
                double jx = startX + (projectileStartX + projectilePos.x) * metersToPixels;
                double jy = groundY - projectilePos.y * metersToPixels;
                // Draw javelin
                g2.setColor(new Color(250, 250, 250));
                Stroke old = g2.getStroke();
                g2.setStroke(new BasicStroke(3f));
                int jlen = 50;
                double angle = Math.atan2(projectileVel.y, projectileVel.x);
                int x2 = (int) (jx + Math.cos(angle) * jlen);
                int y2 = (int) (jy - Math.sin(angle) * jlen);
                g2.drawLine((int) jx, (int) jy, x2, y2);
                g2.setStroke(old);

                // Shadow
                int shadowY = groundY - 2;
                g2.setColor(new Color(0, 0, 0, 60));
                g2.drawLine((int) jx, shadowY, x2, shadowY);

                // Predicted landing marker (rough)
                double tLand = (projectileVel.y + Math.sqrt(projectileVel.y * projectileVel.y + 2 * gravity * projectilePos.y)) / gravity;
                double predX = projectilePos.x + projectileVel.x * tLand + 0.5 * wind * 0.2 * tLand * tLand;
                double predPx = startX + (projectileStartX + predX) * metersToPixels;
                g2.setColor(new Color(255, 200, 100));
                g2.drawLine((int) predPx, groundY - 10, (int) predPx, groundY);
            }

            // UI panels
            drawJavelinHUD(g2, width, height, startX, groundY);

            // Results if finished
            if (finished) {
                drawJavelinResults(g2, width, height);
            }
        }

        private void drawField(Graphics2D g2, int width, int height) {
            GradientPaint gp = new GradientPaint(0, 0, new Color(24, 90, 32), 0, height, new Color(14, 60, 22));
            g2.setPaint(gp);
            g2.fillRect(0, 0, width, height);

            // Crowd and stands
            g2.setColor(new Color(20, 20, 25, 180));
            g2.fillRect(0, 0, width, 120);
            Random lrng = new Random(5678);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            for (int i = 0; i < 600; i++) {
                int x = lrng.nextInt(width);
                int y = lrng.nextInt(90) + 20;
                g2.setColor(new Color(60 + lrng.nextInt(160), 60 + lrng.nextInt(160), 60 + lrng.nextInt(160)));
                g2.fillRect(x, y, 2, 2);
            }
            g2.setComposite(AlphaComposite.SrcOver);
        }

        private void drawJavelinHUD(Graphics2D g2, int width, int height, double startX, int groundY) {
            // Wind indicator
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(16, 80, 210, 60, 12, 12);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("Wind: " + String.format("%.1f m/s", wind), 26, 104);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString("(positive assists throw)", 26, 122);

            // Round/turn indicator
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(16, 150, 280, 90, 12, 12);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("Round " + currentRound + " of " + rounds, 26, 172);
            g2.drawString("Thrower: " + athletes.get(throwerIndex).name + (throwerIndex == playerIdx ? " (You)" : ""), 26, 194);

            // Power/angle panel for player if aiming
            if (throwerIndex == playerIdx && (inRunup || inAim)) {
                // Power bar
                int bx = 16;
                int by = height - 110;
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRoundRect(bx, by, 280, 84, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2.drawString("Run-up & Throw Controls", bx + 12, by + 20);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g2.drawString("A/D or Left/Right to build cadence during run-up", bx + 12, by + 38);
                g2.drawString("Hold LMB to charge, move mouse up/down to aim, release to throw", bx + 12, by + 56);

                // Bars
                drawBar(g2, bx + 12, by + 64, 180, 12, clamp(Math.abs(cadence), 0, 1), new Color(100, 200, 255), new Color(60, 120, 190));
                drawBar(g2, bx + 200, by + 64, 68, 12, clamp((aimAngle - aimAngleMin) / (aimAngleMax - aimAngleMin), 0, 1), new Color(255, 180, 80), new Color(160, 110, 60));

                // Charge bar above foul line
                g2.setColor(new Color(0, 0, 0, 130));
                g2.fillRoundRect((int) (startX + foulLineX * metersToPixels - 120), groundY - 160, 240, 24, 12, 12);
                drawBar(g2, (int) (startX + foulLineX * metersToPixels - 110), groundY - 152, 220, 8, chargePower, new Color(240, 80, 80), new Color(180, 40, 40));
                g2.setColor(Color.WHITE);
                g2.drawString("Power", (int) (startX + foulLineX * metersToPixels - 20), groundY - 140);
            }

            // Scoreboard of best throws
            int sbx = width - 300;
            int sby = 80;
            int sbw = 280;
            int sbh = 28 + 22 * athletes.size() + 20;
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(sbx, sby, sbw, sbh, 12, 12);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("Javelin Standings (best of " + rounds + ")", sbx + 12, sby + 20);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            int y = sby + 40;
            List<Map.Entry<Athlete, Double>> list = new ArrayList<>(bestThrow.entrySet());
            list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            int place = 1;
            for (Map.Entry<Athlete, Double> e : list) {
                String val = e.getValue().isInfinite() ? "Foul" : String.format("%.2f m", e.getValue());
                g2.setColor(e.getKey().color);
                g2.fillOval(sbx + 12, y - 8, 8, 8);
                g2.setColor(Color.WHITE);
                g2.drawString(String.format("%d. %-14s  %s", place, e.getKey().name, val), sbx + 26, y);
                y += 20;
                place++;
            }

            // Tip when finished
            if (finished) {
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRoundRect(16, height - 70, 380, 40, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
                g2.drawString("Event finished. Press R to restart, or N/B to switch events.", 26, height - 44);
                g2.drawString(scoreboard.bestDistanceLine(getName()), 26, height - 26);
            }
        }

        private void drawBar(Graphics2D g2, int i, int i1, int i2, int i3, double clamp, Color color, Color color1) {
        }

        private void drawJavelinResults(Graphics2D g2, int width, int height) {
            int boxW = 460;
            int boxH = 40 + athletes.size() * 28 + 30;
            int x = width / 2 - boxW / 2;
            int y = 120;

            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRoundRect(x, y, boxW, boxH, 16, 16);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString("Final Results - Javelin Throw", x + 16, y + 26);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            int yy = y + 52;
            int place = 1;

            List<Map.Entry<Athlete, Double>> list = new ArrayList<>(bestThrow.entrySet());
            list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            for (Map.Entry<Athlete, Double> e : list) {
                String val = e.getValue().isInfinite() || e.getValue() <= 0 ? "Foul" : String.format("%.2f m", e.getValue());
                g2.setColor(e.getKey().color);
                g2.fillOval(x + 14, yy - 12, 10, 10);
                g2.setColor(Color.WHITE);
                g2.drawString(String.format("%d. %-18s  %s", place, e.getKey().name, val), x + 32, yy);
                yy += 24;
                place++;
            }

            // Best distance
            g2.setColor(new Color(200, 200, 240));
            g2.drawString(scoreboard.bestDistanceLine(getName()), x + 16, yy + 10);
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public List<String> getHUDStrings() {
            List<String> s = new ArrayList<>();
            s.add("Controls (Javelin): Run-up with A/D or Left/Right. Hold LMB to charge, move mouse up/down to aim, release to throw.");
            s.add("Rounds: " + currentRound + "/" + rounds + "; Wind: " + String.format("%.1f m/s", wind));
            if (finished) {
                Double you = bestThrow.get(athletes.get(playerIdx));
                s.add("Your best: " + (you == null || you <= 0 ? "Foul" : String.format("%.2f m", you)));
            }
            return s;
        }

        @Override
        public void handleKeyPressed(KeyEvent e) {
            if (!started || finished) return;

            if (throwerIndex == playerIdx) {
                if (inRunup) {
                    if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) {
                        cadence += 0.35;
                    } else if (e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        cadence += 0.35;
                    }
                } else if (inAim) {
                    // Allow fine-angle adjust with up/down
                    if (e.getKeyCode() == KeyEvent.VK_UP) aimAngle += Math.toRadians(1.5);
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) aimAngle -= Math.toRadians(1.5);
                    aimAngle = clamp(aimAngle, aimAngleMin, aimAngleMax);
                }
            }
        }

        @Override
        public void handleKeyReleased(KeyEvent e) {}

        @Override
        public void handleMouseMoved(MouseEvent e, boolean dragged) {
            if (!started || finished) return;
            if (throwerIndex != playerIdx) return;
            if (inAim) {
                // Map vertical mouse to angle
                int h = gp.getHeight();
                double t = 1.0 - clamp(e.getY() / (double) h, 0, 1);
                aimAngle = aimAngleMin + t * (aimAngleMax - aimAngleMin);
            }
        }

        @Override
        public void handleMousePressed(MouseEvent e) {
            if (!started || finished) return;
            if (throwerIndex != playerIdx) return;

            if (SwingUtilities.isLeftMouseButton(e)) {
                if (inRunup) {
                    inRunup = false;
                    inAim = true;
                }
                if (inAim) {
                    // start charging
                    Timer t = new Timer(16, null);
                    t.addActionListener(ev -> {
                        if (!SwingUtilities.isLeftMouseButton((MouseEvent) e) || !inAim) {
                            t.stop();
                            return;
                        }
                        chargePower = clamp(chargePower + 0.0125, 0, maxChargePower);
                    });
                    // We'll not store Timer; charge based on update path to avoid complexity
                    // Instead, simply increment on update; we emulate with a flag:
                }
            }
        }

        @Override
        public void handleMouseReleased(MouseEvent e) {
            if (!started || finished) return;
            if (throwerIndex != playerIdx) return;
            if (!SwingUtilities.isLeftMouseButton(e)) return;

            if (inAim) {
                // Throw
                launch(athletes.get(throwerIndex));
            }
        }

        @Override
        public void handleMouseClicked(MouseEvent e) {

        }

        @Override
        public String getName() { return "Javelin Throw"; }
    }

    // Simple 2D vector for javelin physics
    public static class Vec2 {
        public double x;
        public double y;
        public Vec2(double x, double y) { this.x = x; this.y = y; }
        public Vec2 add(Vec2 o) { return new Vec2(x + o.x, y + o.y); }
        public Vec2 scl(double s) { return new Vec2(x * s, y * s); }
    }

    // Utility functions
    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}