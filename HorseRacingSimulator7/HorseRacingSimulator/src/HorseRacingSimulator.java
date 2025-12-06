import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class HorseRacingSimulator {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }

    static class GameWindow extends JFrame {

        private final RacePanel racePanel;
        private final HUDPanel hudPanel;
        private final ControlBar controlBar;
        private final CommentatorPanel commentatorPanel;

        GameWindow() {
            super("Horse Racing Simulator");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setMinimumSize(new Dimension(1100, 720));
            setLocationByPlatform(true);

            racePanel = new RacePanel();
            hudPanel = new HUDPanel(racePanel);
            commentatorPanel = new CommentatorPanel(racePanel);
            controlBar = new ControlBar(racePanel, hudPanel, commentatorPanel, this);

            JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            verticalSplit.setDividerSize(6);
            verticalSplit.setResizeWeight(0.85);
            verticalSplit.setTopComponent(racePanel);

            JPanel bottomArea = new JPanel(new BorderLayout());
            bottomArea.add(hudPanel, BorderLayout.WEST);
            bottomArea.add(controlBar, BorderLayout.CENTER);
            bottomArea.add(commentatorPanel, BorderLayout.EAST);
            verticalSplit.setBottomComponent(bottomArea);

            setLayout(new BorderLayout());
            add(verticalSplit, BorderLayout.CENTER);

            pack();
            racePanel.requestFocusInWindow();
        }
    }

    static class Horse {
        // Identity and visuals
        final int id;
        final String name;
        final Color colorPrimary;
        final Color colorSecondary;

        // Stats and capabilities
        final double baseSpeed;   // meters per second
        final double burstSpeed;  // meters per second for short bursts
        final double stamina;     // range ~ [0.5, 1.5] influences fatigue
        final double agility;     // cornering efficiency
        final double weight;      // influences acceleration slightly
        final double luck;        // random factor influences events

        // Runtime state
        double position;          // horizontal meters from start
        double laneOffset;        // vertical lane offset in meters (rendered as Y)
        double velocity;          // current m/s
        double fatigue;           // increases over time, lowers speed
        double burstTimer;        // time remaining for current burst
        double slipTimer;         // time remaining for slip or slow event
        double cheerBoostTimer;   // crowd cheer boost
        boolean finished;         // finished race
        long finishTimeNanos;     // timestamp when horse crossed finish
        int rank;                 // finishing place

        double trotPhase;

        Horse(int id, String name, Color primary, Color secondary, Random rng) {
            this.id = id;
            this.name = name;
            this.colorPrimary = primary;
            this.colorSecondary = secondary;

            // Base attributes randomized within balanced ranges
            this.baseSpeed = 14.0 + rng.nextDouble() * 2.0;      // ~14-16 m/s
            this.burstSpeed = this.baseSpeed + 2.0 + rng.nextDouble() * 2.0;
            this.stamina = 0.8 + rng.nextDouble() * 0.7;         // 0.8-1.5
            this.agility = 0.8 + rng.nextDouble() * 0.4;         // 0.8-1.2
            this.weight = 450 + rng.nextDouble() * 50;           // kg
            this.luck = 0.8 + rng.nextDouble() * 0.6;            // 0.8-1.4

            this.position = 0;
            this.laneOffset = id;  // lane ordering by id initially
            this.velocity = 0;
            this.fatigue = 0;
            this.burstTimer = 0;
            this.slipTimer = 0;
            this.cheerBoostTimer = 0;
            this.finished = false;
            this.finishTimeNanos = 0;
            this.rank = 0;
            this.trotPhase = rng.nextDouble() * Math.PI * 2;
        }

        void resetForNewRace(double lane) {
            position = 0;
            laneOffset = lane;
            velocity = 0;
            fatigue = 0;
            burstTimer = 0;
            slipTimer = 0;
            cheerBoostTimer = 0;
            finished = false;
            finishTimeNanos = 0;
            rank = 0;
            trotPhase = Math.random() * Math.PI * 2;
        }
    }

    static class RaceConfig {
        int numHorses = 8;
        double trackLengthMeters = 1200; // 1200m sprint
        double laneHeight = 50;          // pixel spacing for lanes
        boolean wetTrack = false;
        Weather weather = Weather.SUNNY;
        CameraMode cameraMode = CameraMode.OVERVIEW;
        double globalSpeedMultiplier = 1.0;

        enum Weather { SUNNY, RAINY, FOGGY }
        enum CameraMode { OVERVIEW, FOLLOW }

        void toggleWetTrack() { wetTrack = !wetTrack; }

        void cycleWeather() {
            if (weather == Weather.SUNNY) weather = Weather.RAINY;
            else if (weather == Weather.RAINY) weather = Weather.FOGGY;
            else weather = Weather.SUNNY;
        }

        void cycleCamera() {
            cameraMode = (cameraMode == CameraMode.OVERVIEW) ? CameraMode.FOLLOW : CameraMode.OVERVIEW;
        }
    }

    static class Betting {
        static class Bet {
            final String bettorName;
            final int horseId;
            final double amount;

            Bet(String bettorName, int horseId, double amount) {
                this.bettorName = bettorName;
                this.horseId = horseId;
                this.amount = amount;
            }
        }

        final List<Bet> bets = new ArrayList<>();
        final Map<Integer, Double> odds = new HashMap<>(); // horseId -> odds multiplier

        void clear() {
            bets.clear();
            odds.clear();
        }

        void placeBet(String bettorName, int horseId, double amount) {
            bets.add(new Bet(bettorName, horseId, amount));
        }

        void computeOdds(List<Horse> horses, RaceConfig config) {
            odds.clear();
            // Simple odds: based on baseSpeed, stamina, and weather adjustments
            double totalScore = 0;
            Map<Integer, Double> scoreMap = new HashMap<>();
            for (Horse h : horses) {
                double score = h.baseSpeed * h.stamina;
                if (config.wetTrack) {
                    score *= (0.92 + 0.06 * h.agility); // agility helps slightly on wet track
                }
                if (config.weather == RaceConfig.Weather.FOGGY) {
                    score *= (0.95 + 0.05 * h.luck);
                }
                totalScore += score;
                scoreMap.put(h.id, score);
            }
            for (Horse h : horses) {
                double p = scoreMap.get(h.id) / totalScore;
                // Convert probability to fair odds, then add margin
                double fairOdds = (p > 0) ? ((1.0 - p) / p) : 10.0;
                double houseMargin = 0.15;
                odds.put(h.id, Math.max(1.1, fairOdds * (1.0 + houseMargin)));
            }
        }

        double payoutForBettor(String bettorName, int winningHorseId) {
            double payout = 0.0;
            for (Bet b : bets) {
                if (b.bettorName.equals(bettorName) && b.horseId == winningHorseId) {
                    double o = odds.getOrDefault(b.horseId, 1.0);
                    payout += b.amount * o;
                }
            }
            return payout;
        }
    }

    /**
     * Commentator producing text lines based on race events.
     */
    static class Commentator {
        private final Deque<String> lines = new ArrayDeque<>();
        private final int maxLines = 12;

        void say(String text) {
            lines.addFirst(text);
            while (lines.size() > maxLines) lines.removeLast();
        }

        List<String> getLines() {
            return new ArrayList<>(lines);
        }

        void clear() { lines.clear(); }
    }

    /**
     * Utility easing functions for smooth animations.
     */
    static class Easing {
        static double smoothStep(double t) {
            t = Math.max(0, Math.min(1, t));
            return t * t * (3 - 2 * t);
        }

        static double easeInOutQuad(double t) {
            t = Math.max(0, Math.min(1, t));
            return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
        }

        static double clamp(double v, double min, double max) {
            return Math.max(min, Math.min(max, v));
        }
    }

    /**
     * Random color generation with good contrast.
     */
    static class ColorUtil {
        static Color randomVividColor(Random rng) {
            float h = rng.nextFloat();
            float s = 0.75f + rng.nextFloat() * 0.25f;
            float b = 0.85f + rng.nextFloat() * 0.15f;
            return Color.getHSBColor(h, s, b);
        }

        static Color darker(Color c, float factor) {
            factor = Math.min(1f, Math.max(0f, factor));
            return new Color(
                    (int) (c.getRed() * (1 - factor)),
                    (int) (c.getGreen() * (1 - factor)),
                    (int) (c.getBlue() * (1 - factor))
            );
        }

        static Color withAlpha(Color c, int alpha) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }
    }

    // ---------------------------
    // Race Engine and Rendering
    // ---------------------------

    /**
     * Core panel that renders the race track, horses, and handles the simulation loop.
     */
    static class RacePanel extends JPanel implements ActionListener, KeyListener, MouseWheelListener {

        // Timing
        private final Timer timer;
        private long lastNanos;

        // State
        private final RaceConfig config = new RaceConfig();
        private final List<Horse> horses = new ArrayList<>();
        private final Random rng = new Random();
        private boolean running = false;
        private boolean debugOverlay = false;
        private boolean showCommentator = true;

        // Visual params
        private double cameraX = 0;
        private double cameraTargetX = 0;

        // Track visuals
        private BufferedImage offscreen;
        private int frameCount = 0;

        // Betting model
        private final Betting betting = new Betting();
        // Commentator
        private final Commentator commentator = new Commentator();

        // Countdown state
        private long raceStartCountdownNanos = 0;
        private boolean countdownActive = false;

        // Results
        private int finishedCount = 0;
        private boolean raceCompleted = false;
        private int winningHorseId = -1;

        RacePanel() {
            setBackground(new Color(30, 60, 30));
            setFocusable(true);
            addKeyListener(this);
            addMouseWheelListener(this);
            setPreferredSize(new Dimension(1100, 500));

            timer = new Timer(16, this); // ~60 FPS
            timer.setInitialDelay(0);
            timer.start();

            initRace();
        }

        void initRace() {
            horses.clear();
            commentator.clear();
            betting.clear();
            raceCompleted = false;
            finishedCount = 0;
            winningHorseId = -1;
            rng.setSeed(System.nanoTime());

            // Create horses with distinct colors and names
            for (int i = 0; i < config.numHorses; i++) {
                Color primary = ColorUtil.randomVividColor(rng);
                Color secondary = ColorUtil.darker(primary, 0.35f);
                String name = randomHorseName(i);
                Horse h = new Horse(i, name, primary, secondary, rng);
                double lane = 1 + i;
                h.resetForNewRace(lane);
                horses.add(h);
            }

            betting.computeOdds(horses, config);
            commentator.say("Welcome to the Horse Racing Simulator!");
            commentator.say("Today's track length: " + (int) config.trackLengthMeters + "m.");
            commentator.say("Track is " + (config.wetTrack ? "wet" : "dry") + ", weather: " + config.weather + ".");
            commentator.say("Place your bets before the race begins!");
            cameraX = 0;
            cameraTargetX = 0;
        }

        // Randomly pick a lively horse name
        String randomHorseName(int idx) {
            String[] prefixes = {
                    "Silver", "Thunder", "Midnight", "Nebula", "Crimson", "Velvet", "Rocket",
                    "Mystic", "Tempest", "Aurora", "Shadow", "Cascade", "Ember", "Sable"
            };
            String[] suffixes = {
                    "Comet", "Runner", "Blaze", "Whisper", "Charger", "Spirit", "Silhouette",
                    "Storm", "Echo", "Stride", "Rider", "Glide", "Dancer", "Sprint"
            };
            return prefixes[idx % prefixes.length] + " " + suffixes[(idx * 3 + 1) % suffixes.length];
        }

        // ------------------------
        // Game Loop and Simulation
        // ------------------------

        @Override
        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            if (lastNanos == 0) lastNanos = now;
            double dt = (now - lastNanos) / 1_000_000_000.0;
            lastNanos = now;

            // Avoid overly large dt when window is unfocused
            dt = Math.min(dt, 0.05);

            if (running) {
                updateSimulation(dt);
            } else {
                // When not running, animate small effects like crowd oscillation
                idleEffects(dt);
            }

            // Camera smoothing
            updateCamera(dt);

            frameCount++;
            repaint();
        }

        void updateSimulation(double dt) {
            if (countdownActive) {
                long elapsed = (System.nanoTime() - raceStartCountdownNanos);
                if (elapsed > 3_500_000_000L) {
                    countdownActive = false;
                    commentator.say("And they're off!");
                }
                // During countdown, no movement
                return;
            }

            if (raceCompleted) {
                // Animate idle post-race
                idleEffects(dt);
                return;
            }

            double wetPenalty = config.wetTrack ? 0.85 : 1.0;
            double weatherVisibility = (config.weather == RaceConfig.Weather.FOGGY) ? 0.9 : 1.0;

            // Simulate each horse
            for (Horse h : horses) {
                if (h.finished) continue;

                // Increase fatigue with speed; stamina reduces fatigue accumulation
                double fatigueRate = 0.05 * (h.velocity / h.baseSpeed) / h.stamina;
                h.fatigue += fatigueRate * dt;

                // Natural decay of temporary states
                if (h.burstTimer > 0) h.burstTimer = Math.max(0, h.burstTimer - dt);
                if (h.slipTimer > 0) h.slipTimer = Math.max(0, h.slipTimer - dt);
                if (h.cheerBoostTimer > 0) h.cheerBoostTimer = Math.max(0, h.cheerBoostTimer - dt);

                // Stochastic events: bursts, slips, cheers
                if (rng.nextDouble() < 0.003 * dt * config.globalSpeedMultiplier * h.luck) {
                    // Burst event: small acceleration for ~2 seconds
                    h.burstTimer = 1.5 + rng.nextDouble() * 1.0;
                    commentator.say(h.name + " finds a burst of speed!");
                }
                if (config.wetTrack && rng.nextDouble() < 0.002 * dt * (1.3 - 0.3 * h.agility)) {
                    h.slipTimer = 0.8 + rng.nextDouble() * 0.6;
                    commentator.say(h.name + " slips slightly on the wet track.");
                }
                if (rng.nextDouble() < 0.0015 * dt * weatherVisibility) {
                    h.cheerBoostTimer = 1.0;
                    commentator.say("Crowd cheers as " + h.name + " surges!");
                }

                // Compute target speed with conditions and states
                double targetSpeed = h.baseSpeed * wetPenalty;
                targetSpeed *= (1.0 - 0.30 * Easing.clamp(h.fatigue, 0, 1.5)); // fatigue impact
                if (h.burstTimer > 0) targetSpeed = Math.max(targetSpeed, h.burstSpeed * wetPenalty);
                if (h.slipTimer > 0) targetSpeed *= 0.75;
                if (h.cheerBoostTimer > 0) targetSpeed *= 1.08;

                // Apply global speed multiplier
                targetSpeed *= Easing.clamp(config.globalSpeedMultiplier, 0.3, 3.0);

                // Smooth acceleration based on weight (heavier -> slower accel)
                double accel = (targetSpeed - h.velocity) * (2.5 / (h.weight / 450.0));
                h.velocity += accel * dt;

                // Position update
                h.position += h.velocity * dt;

                // Gentle lane bobbing for animation
                h.trotPhase += dt * 6.0;
                h.laneOffset += Math.sin(h.trotPhase) * 0.2; // subtle vertical oscillation

                // Finish line
                if (h.position >= config.trackLengthMeters) {
                    h.finished = true;
                    h.finishTimeNanos = System.nanoTime();
                    h.rank = ++finishedCount;
                    h.velocity = 0;
                    if (h.rank == 1) {
                        winningHorseId = h.id;
                        commentator.say(h.name + " wins the race!");
                        concludeRace();
                    } else {
                        commentator.say(h.name + " finishes in position " + h.rank + ".");
                        if (finishedCount == horses.size()) {
                            concludeRace();
                        }
                    }
                }
            }
        }

        void concludeRace() {
            raceCompleted = true;
            running = false;

            // Payout summary for a fictitious bettor "You"
            double payout = betting.payoutForBettor("You", winningHorseId);
            if (payout > 0) {
                commentator.say("Your bet paid out: " + formatCurrency(payout));
            } else {
                commentator.say("No payout this time. Better luck next race.");
            }
            commentator.say("Press R to restart, B to bet, Space to run again.");
        }

        String formatCurrency(double amount) {
            DecimalFormat df = new DecimalFormat("#,##0.00");
            return "$" + df.format(amount);
        }

        void idleEffects(double dt) {
            // Subtle crowd and flag animations; no physics changes
            for (Horse h : horses) {
                if (!h.finished) {
                    // Gentle breathing motion
                    h.trotPhase += dt * 2.0;
                    h.laneOffset += Math.sin(h.trotPhase * 0.5) * 0.05;
                }
            }
        }

        void updateCamera(double dt) {
            if (config.cameraMode == RaceConfig.CameraMode.FOLLOW) {
                double leaderX = 0;
                for (Horse h : horses) leaderX = Math.max(leaderX, h.position);
                cameraTargetX = Easing.clamp(leaderX - 100, 0, config.trackLengthMeters - 100);
            } else {
                // Overview shows the entire track; cameraX to 0
                cameraTargetX = 0;
            }

            // Smooth camera movement
            cameraX += (cameraTargetX - cameraX) * Easing.smoothStep(Math.min(1.0, 2.0 * dt));
        }

        // ------------------------
        // Rendering
        // ------------------------

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int w = getWidth();
            int h = getHeight();

            if (offscreen == null || offscreen.getWidth() != w || offscreen.getHeight() != h) {
                offscreen = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            }

            Graphics2D g2 = offscreen.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Background sky depending on weather
            paintSky(g2, w, h);

            // Track and infield
            paintTrack(g2, w, h);

            // Horses
            paintHorses(g2, w, h);

            // Finish banner
            paintFinishLine(g2, w, h);

            // Crowd
            paintCrowd(g2, w, h);

            // Overlays
            paintOverlay(g2, w, h);

            g2.dispose();
            g.drawImage(offscreen, 0, 0, null);
        }

        void paintSky(Graphics2D g2, int w, int h) {
            Color skyTop;
            Color skyBottom;
            switch (config.weather) {
                case SUNNY:
                    skyTop = new Color(120, 170, 255);
                    skyBottom = new Color(180, 210, 255);
                    break;
                case RAINY:
                    skyTop = new Color(90, 110, 140);
                    skyBottom = new Color(130, 150, 170);
                    break;
                default: // FOGGY
                    skyTop = new Color(170, 180, 190);
                    skyBottom = new Color(195, 200, 205);
                    break;
            }

            GradientPaint gp = new GradientPaint(0, 0, skyTop, 0, h / 2f, skyBottom);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h / 2);

            // Distant hills
            g2.setColor(new Color(60, 100, 60));
            Shape hill = new RoundRectangle2D.Double(-50, h / 2.0 - 60, w + 100, 120, 200, 200);
            g2.fill(hill);

            // Weather effects: rain streaks or fog overlay
            if (config.weather == RaceConfig.Weather.RAINY) {
                paintRain(g2, w, h);
            } else if (config.weather == RaceConfig.Weather.FOGGY) {
                g2.setColor(new Color(220, 220, 230, 110));
                g2.fillRect(0, 0, w, h);
            }
        }

        void paintRain(Graphics2D g2, int w, int h) {
            g2.setStroke(new BasicStroke(1f));
            for (int i = 0; i < 120; i++) {
                int x = (int) (rng.nextInt(w));
                int y = rng.nextInt(h);
                int len = 10 + rng.nextInt(15);
                g2.setColor(new Color(190, 200, 230, 90));
                g2.drawLine(x, y, x + 3, y + len);
            }
        }

        void paintTrack(Graphics2D g2, int w, int h) {
            int trackTop = (int) (h * 0.58);
            int trackHeight = (int) (h * 0.28);

            // Infield grass
            g2.setColor(new Color(70, 120, 70));
            g2.fillRect(0, trackTop - trackHeight, w, trackHeight);

            // Track dirt
            Color dirt = config.wetTrack ? new Color(110, 85, 65) : new Color(140, 110, 85);
            g2.setColor(dirt);
            g2.fillRect(0, trackTop, w, trackHeight);

            // Lane lines
            g2.setColor(new Color(250, 250, 250, 200));
            g2.setStroke(new BasicStroke(2.0f));
            for (int i = 0; i <= horses.size(); i++) {
                int y = trackTop + (int) (i * config.laneHeight);
                g2.drawLine(0, y, w, y);
            }

            // Distance markers
            g2.setColor(new Color(230, 230, 230, 160));
            int markerCount = (int) (config.trackLengthMeters / 100.0);
            for (int i = 0; i <= markerCount; i++) {
                int mx = worldToScreenX(i * 100.0, w);
                g2.fillRect(mx, trackTop, 3, 20);
                if (i % 2 == 0) {
                    String text = i * 100 + "m";
                    g2.setFont(getFont().deriveFont(Font.BOLD, 10f));
                    g2.drawString(text, mx - 8, trackTop - 6);
                }
            }

            // Shadow overlay for depth
            g2.setPaint(new GradientPaint(0, trackTop, new Color(0, 0, 0, 25), 0, trackTop + trackHeight, new Color(0, 0, 0, 80)));
            g2.fillRect(0, trackTop, w, trackHeight);
        }

        void paintHorses(Graphics2D g2, int w, int h) {
            int trackTop = (int) (h * 0.58);
            for (Horse horse : horses) {
                int laneY = trackTop + (int) (horse.laneOffset * config.laneHeight);
                int x = worldToScreenX(horse.position, w);

                // Body wiggle based on trot
                double wiggle = Math.sin(horse.trotPhase) * 2.5;

                // Horse body
                int bodyWidth = 40;
                int bodyHeight = 18;
                Shape body = new RoundRectangle2D.Double(x - bodyWidth / 2.0, laneY - bodyHeight / 2.0 + wiggle, bodyWidth, bodyHeight, 10, 10);
                g2.setColor(horse.colorPrimary);
                g2.fill(body);

                // Saddle
                g2.setColor(horse.colorSecondary);
                g2.fill(new RoundRectangle2D.Double(x - 8, laneY - 7 + wiggle, 16, 14, 8, 8));

                // Head
                Shape head = new RoundRectangle2D.Double(x + bodyWidth / 2.0 - 6, laneY - 7 + wiggle, 12, 12, 6, 6);
                g2.setColor(ColorUtil.darker(horse.colorPrimary, 0.1f));
                g2.fill(head);

                // Simple legs animation
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(ColorUtil.darker(horse.colorPrimary, 0.25f));
                int legSpan = 12;
                int legY = laneY + bodyHeight / 2;
                int phase = (int) (Math.abs(Math.sin(horse.trotPhase * 1.5)) * 6);
                g2.drawLine(x - 12, legY, x - 12 + phase, legY + legSpan);
                g2.drawLine(x - 6, legY, x - 6 - phase, legY + legSpan);
                g2.drawLine(x + 6, legY, x + 6 + phase, legY + legSpan);
                g2.drawLine(x + 12, legY, x + 12 - phase, legY + legSpan);

                // Name tag
                g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
                String label = horse.name + (horse.finished ? " (" + horse.rank + ")" : "");
                g2.setColor(new Color(255, 255, 255));
                drawStringWithShadow(g2, label, x - bodyWidth / 2, laneY - bodyHeight - 8);

                // Particle splash on wet track
                if (config.wetTrack && horse.velocity > 1) {
                    paintMudSplash(g2, x, laneY + bodyHeight / 2);
                }
            }
        }

        void paintMudSplash(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(90, 70, 50, 130));
            for (int i = 0; i < 5; i++) {
                int dx = rng.nextInt(10) - 5;
                int dy = rng.nextInt(8);
                g2.fillOval(x + dx, y + dy, 3, 3);
            }
        }

        void paintFinishLine(Graphics2D g2, int w, int h) {
            int trackTop = (int) (h * 0.58);
            int x = worldToScreenX(config.trackLengthMeters, w);

            g2.setColor(new Color(255, 255, 255, 220));
            g2.fillRect(x - 2, trackTop, 4, (int) (h * 0.28));

            // Banner
            g2.setColor(new Color(230, 50, 50));
            g2.fillRoundRect(x - 50, trackTop - 40, 100, 30, 10, 10);
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
            drawStringCentered(g2, "FINISH", x, trackTop - 20);
        }

        void paintCrowd(Graphics2D g2, int w, int h) {
            int crowdTop = (int) (h * 0.2);
            int crowdHeight = (int) (h * 0.18);

            // Stands
            g2.setColor(new Color(60, 60, 70));
            g2.fillRect(0, crowdTop, w, crowdHeight);

            // People dots
            for (int i = 0; i < w; i += 8) {
                int jitterY = (int) (Math.sin(frameCount * 0.05 + i * 0.03) * 2);
                Color c = new Color(120 + rng.nextInt(80), 80 + rng.nextInt(80), 70 + rng.nextInt(70));
                g2.setColor(c);
                g2.fillOval(i, crowdTop + crowdHeight - 25 + jitterY, 5, 5);
            }

            // Flags
            for (int i = 0; i < 8; i++) {
                int fx = i * (w / 8) + 40;
                int fy = crowdTop - 10;
                g2.setColor(new Color(220, 220, 230));
                g2.fillRect(fx, fy, 4, 18);
                int flap = (int) (Math.sin(frameCount * 0.1 + i * 0.7) * 6);
                g2.setColor(new Color(230, 70, 70));
                g2.fillPolygon(new int[]{fx + 4, fx + 22, fx + 4}, new int[]{fy, fy + 8 + flap, fy + 16}, 3);
            }
        }

        void paintOverlay(Graphics2D g2, int w, int h) {
            // HUD info
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(10, 10, 220, 100, 12, 12);

            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));

            String status = running ? "Running" : (countdownActive ? "Starting..." : (raceCompleted ? "Finished" : "Ready"));
            g2.drawString("Race: " + status, 20, 30);
            g2.drawString("Track: " + (config.wetTrack ? "Wet" : "Dry") + ", Weather: " + config.weather, 20, 48);
            g2.drawString("Camera: " + config.cameraMode + ", Speed x" + new DecimalFormat("#0.0").format(config.globalSpeedMultiplier), 20, 66);
            g2.drawString("Distance: " + (int) cameraX + "m / " + (int) config.trackLengthMeters + "m", 20, 84);

            // Countdown visualization
            if (countdownActive) {
                long elapsed = (System.nanoTime() - raceStartCountdownNanos);
                double t = elapsed / 1_000_000_000.0;
                int count = Math.max(0, 3 - (int) t);
                String text = (count > 0) ? String.valueOf(count) : "GO!";
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRoundRect(w / 2 - 60, 30, 120, 60, 16, 16);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD, 32f));
                drawStringCentered(g2, text, w / 2, 68);
            }

            // Commentator area
            if (showCommentator) {
                paintCommentatorBox(g2, w, h);
            }

            // Debug overlay
            if (debugOverlay) {
                paintDebugOverlay(g2, w, h);
            }
        }

        void paintCommentatorBox(Graphics2D g2, int w, int h) {
            List<String> lines = commentator.getLines();
            int boxW = Math.min(420, (int) (w * 0.4));
            int boxH = Math.min(240, (int) (h * 0.34));
            int x = w - boxW - 12;
            int y = 12;

            g2.setColor(new Color(10, 10, 20, 160));
            g2.fillRoundRect(x, y, boxW, boxH, 16, 16);

            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g2.drawString("Commentator", x + 12, y + 22);

            g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            int yy = y + 40;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                g2.setColor(new Color(240, 240, 245));
                g2.drawString("• " + line, x + 12, yy);
                yy += 18;
                if (yy > y + boxH - 12) break;
            }
        }

        void paintDebugOverlay(Graphics2D g2, int w, int h) {
            int x = 12;
            int y = h - 150;
            g2.setColor(new Color(20, 20, 20, 160));
            g2.fillRoundRect(x, y, 360, 140, 16, 16);

            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            g2.drawString("Debug Overlay", x + 14, y + 22);
            g2.drawString("Frame: " + frameCount, x + 14, y + 40);
            g2.drawString("CameraX: " + new DecimalFormat("#0.0").format(cameraX), x + 14, y + 58);
            g2.drawString("Countdown: " + (countdownActive ? "Yes" : "No"), x + 14, y + 76);
            g2.drawString("Finished: " + finishedCount + "/" + horses.size(), x + 14, y + 94);

            int yy = y + 112;
            g2.drawString("Leader: " + getLeaderName(), x + 14, yy);
        }

        String getLeaderName() {
            Horse leader = null;
            for (Horse h : horses) {
                if (leader == null || h.position > leader.position) leader = h;
            }
            return (leader != null) ? leader.name : "-";
        }

        int worldToScreenX(double worldX, int w) {
            // Track drawing scale: 1 meter -> 1 pixel (simple), with camera offset
            double sx = worldX - cameraX;
            return (int) Math.round(sx);
        }

        void drawStringCentered(Graphics2D g2, String s, int cx, int cy) {
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(s);
            int a = fm.getAscent();
            g2.drawString(s, cx - w / 2, cy + a / 2 - 2);
        }

        void drawStringWithShadow(Graphics2D g2, String s, int x, int y) {
            g2.setColor(new Color(0, 0, 0, 140));
            g2.drawString(s, x + 1, y + 1);
            g2.setColor(Color.WHITE);
            g2.drawString(s, x, y);
        }

        // ------------------------
        // Controls
        // ------------------------

        void toggleRunning() {
            if (raceCompleted) {
                commentator.say("Race already completed. Press R to restart.");
                return;
            }
            if (!running) {
                // Start with a countdown
                countdownActive = true;
                raceStartCountdownNanos = System.nanoTime();
                commentator.say("Race starting in 3...");
            }
            running = !running;
        }

        void restartRace() {
            initRace();
            running = false;
            countdownActive = false;
            lastNanos = 0;
            commentator.say("Race restarted.");
        }

        void adjustSpeed(boolean faster) {
            double step = faster ? 0.1 : -0.1;
            config.globalSpeedMultiplier = Easing.clamp(config.globalSpeedMultiplier + step, 0.5, 2.5);
            commentator.say("Speed set to x" + new DecimalFormat("#0.0").format(config.globalSpeedMultiplier));
        }

        void toggleDebug() {
            debugOverlay = !debugOverlay;
        }

        void toggleCommentator() {
            showCommentator = !showCommentator;
        }

        void toggleCameraMode() {
            config.cycleCamera();
            commentator.say("Camera: " + config.cameraMode);
        }

        void toggleWetTrack() {
            config.toggleWetTrack();
            commentator.say("Track is now " + (config.wetTrack ? "wet" : "dry") + ".");
            betting.computeOdds(horses, config);
        }

        void toggleWeather() {
            config.cycleWeather();
            commentator.say("Weather changed to " + config.weather + ".");
            betting.computeOdds(horses, config);
        }

        void openBetDialog(Component parent) {
            if (running && !raceCompleted) {
                JOptionPane.showMessageDialog(parent,
                        "Betting is only available before the race begins or after it ends.",
                        "Betting Unavailable",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            panel.add(new JLabel("Your name:"), gbc);

            gbc.gridx = 1;
            JTextField nameField = new JTextField("You", 12);
            panel.add(nameField, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            panel.add(new JLabel("Horse:"), gbc);

            gbc.gridx = 1;
            String[] horseNames = horses.stream().map(h -> h.id + ": " + h.name).toArray(String[]::new);
            JComboBox<String> horseBox = new JComboBox<>(horseNames);
            panel.add(horseBox, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            panel.add(new JLabel("Amount ($):"), gbc);

            gbc.gridx = 1;
            JTextField amountField = new JTextField("100", 8);
            panel.add(amountField, gbc);

            // Odds info
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 2;
            JTextArea oddsInfo = new JTextArea(6, 26);
            oddsInfo.setEditable(false);
            StringBuilder sb = new StringBuilder("Current Odds:\n");
            for (Horse h : horses) {
                double o = betting.odds.getOrDefault(h.id, 1.0);
                sb.append(String.format("- %s: x%.2f\n", h.name, o));
            }
            oddsInfo.setText(sb.toString());
            panel.add(new JScrollPane(oddsInfo), gbc);

            int result = JOptionPane.showConfirmDialog(parent, panel, "Place Bet", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String bettor = nameField.getText().trim();
                int horseId = horseBox.getSelectedIndex(); // matches order
                double amount;
                try {
                    amount = Double.parseDouble(amountField.getText().trim());
                    if (amount <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(parent, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                betting.placeBet(bettor, horses.get(horseId).id, amount);
                commentator.say("Bet placed: " + bettor + " -> $" + (int) amount + " on " + horses.get(horseId).name);
            }
        }

        // ------------------------
        // Input Handling
        // ------------------------

        @Override
        public void keyTyped(KeyEvent e) { }

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_SPACE:
                    toggleRunning();
                    break;
                case KeyEvent.VK_R:
                    restartRace();
                    break;
                case KeyEvent.VK_C:
                    toggleCameraMode();
                    break;
                case KeyEvent.VK_UP:
                    adjustSpeed(true);
                    break;
                case KeyEvent.VK_DOWN:
                    adjustSpeed(false);
                    break;
                case KeyEvent.VK_D:
                    toggleDebug();
                    break;
                case KeyEvent.VK_H:
                    toggleCommentator();
                    break;
                case KeyEvent.VK_T:
                    toggleWetTrack();
                    break;
                case KeyEvent.VK_W:
                    toggleWeather();
                    break;
                case KeyEvent.VK_B:
                    openBetDialog(this);
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) { }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            // Mouse wheel adjusts global speed
            adjustSpeed(e.getWheelRotation() < 0);
        }
    }

    // ---------------------------
    // Heads-Up Display (HUD)
    // ---------------------------

    /**
     * Displays horse stats and live metrics in a compact side panel.
     */
    static class HUDPanel extends JPanel {

        private final RacePanel race;
        private final JTable table;
        private final HUDTableModel tableModel;
        private final JLabel statusLabel;
        private final JLabel tipLabel;

        HUDPanel(RacePanel race) {
            this.race = race;
            setPreferredSize(new Dimension(360, 180));
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(6, 6, 6, 6));

            tableModel = new HUDTableModel(race);
            table = new JTable(tableModel);
            table.setFillsViewportHeight(true);
            table.setRowHeight(22);
            table.setAutoCreateRowSorter(true);

            statusLabel = new JLabel("Ready");
            tipLabel = new JLabel("Tip: Press B to bet, Space to start.");

            JPanel top = new JPanel(new GridLayout(2, 1));
            top.add(statusLabel);
            top.add(tipLabel);

            add(top, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);

            // Refresh timer
            Timer t = new Timer(250, e -> refresh());
            t.start();
        }

        void refresh() {
            tableModel.fireTableDataChanged();
            String status = raceStatus();
            statusLabel.setText("Status: " + status);
        }

        String raceStatus() {
            if (race.countdownActive) return "Starting...";
            if (race.raceCompleted) return "Finished";
            if (race.running) return "Running";
            return "Ready";
        }

        static class HUDTableModel extends javax.swing.table.AbstractTableModel {
            private final RacePanel race;

            HUDTableModel(RacePanel race) {
                this.race = race;
            }

            private final String[] cols = {"#", "Horse", "Pos (m)", "Vel (m/s)", "Fatigue", "Odds"};

            @Override
            public int getRowCount() {
                return race.horses.size();
            }

            @Override
            public int getColumnCount() {
                return cols.length;
            }

            @Override
            public String getColumnName(int column) {
                return cols[column];
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Horse h = race.horses.get(rowIndex);
                DecimalFormat df1 = new DecimalFormat("#0.0");
                DecimalFormat df2 = new DecimalFormat("#0.00");
                switch (columnIndex) {
                    case 0: return h.id + 1;
                    case 1: return h.name + (h.finished ? " (" + h.rank + ")" : "");
                    case 2: return df1.format(h.position);
                    case 3: return df1.format(h.velocity);
                    case 4: return df2.format(h.fatigue);
                    case 5:
                        double o = race.betting.odds.getOrDefault(h.id, 1.0);
                        return "x" + df1.format(o);
                }
                return "";
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Integer.class;
                return String.class;
            }
        }
    }

    // ---------------------------
    // Control Bar Panel
    // ---------------------------

    /**
     * Provides primary controls and actions: start, pause, restart, camera, weather, track, betting.
     */
    static class ControlBar extends JPanel {

        ControlBar(RacePanel race, HUDPanel hud, CommentatorPanel commentatorPanel, JFrame frame) {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            setBorder(new EmptyBorder(6, 6, 6, 6));

            JButton startPause = new JButton("Start / Pause (Space)");
            JButton restart = new JButton("Restart (R)");
            JButton camera = new JButton("Camera (C)");
            JButton debug = new JButton("Debug (D)");
            JButton commentator = new JButton("Toggle Commentary (H)");
            JButton wet = new JButton("Track Wet (T)");
            JButton weather = new JButton("Weather (W)");
            JButton bet = new JButton("Bet (B)");
            JSlider speed = new JSlider(50, 250, 100);
            speed.setPreferredSize(new Dimension(160, 36));
            speed.setPaintTicks(true);
            speed.setPaintLabels(true);
            speed.setMinorTickSpacing(5);
            speed.setMajorTickSpacing(50);
            speed.setLabelTable(speed.createStandardLabels(50));

            add(startPause);
            add(restart);
            add(camera);
            add(debug);
            add(commentator);
            add(wet);
            add(weather);
            add(bet);
            add(new JLabel("Speed x"));
            add(speed);

            startPause.addActionListener(e -> race.toggleRunning());
            restart.addActionListener(e -> race.restartRace());
            camera.addActionListener(e -> race.toggleCameraMode());
            debug.addActionListener(e -> race.toggleDebug());
            commentator.addActionListener(e -> race.toggleCommentator());
            wet.addActionListener(e -> race.toggleWetTrack());
            weather.addActionListener(e -> race.toggleWeather());
            bet.addActionListener(e -> race.openBetDialog(frame));

            speed.addChangeListener(e -> {
                race.config.globalSpeedMultiplier = speed.getValue() / 100.0;
            });
        }
    }

    // ---------------------------
    // Commentator Side Panel
    // ---------------------------

    /**
     * Compact panel summarizing the latest commentator lines with a manual refresh control.
     */
    static class CommentatorPanel extends JPanel {

        private final RacePanel race;
        private final JTextArea area;

        CommentatorPanel(RacePanel race) {
            this.race = race;
            setPreferredSize(new Dimension(320, 180));
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(6, 6, 6, 6));

            area = new JTextArea();
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            JButton refresh = new JButton("Refresh Commentary");
            refresh.addActionListener(e -> updateText());

            add(new JScrollPane(area), BorderLayout.CENTER);
            add(refresh, BorderLayout.SOUTH);

            Timer t = new Timer(1500, e -> updateText());
            t.start();
        }

        void updateText() {
            List<String> lines = race.commentator.getLines();
            StringBuilder sb = new StringBuilder();
            for (int i = lines.size() - 1; i >= 0; i--) {
                sb.append(lines.get(i)).append("\n");
            }
            area.setText(sb.toString());
        }
    }
}
