import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class HorseRacingSimulator extends JFrame implements KeyListener {

    private static final long serialVersionUID = 1L;

    private static final int FPS = 60;
    private static final int TIMER_DELAY_MS = 1000 / FPS;

    private static final int DEFAULT_HORSE_COUNT = 6;
    private static final int MAX_HORSE_COUNT = 9;
    private static final int DEFAULT_TRACK_LENGTH = 3600; // pixels (finish line at this x)
    private static final int DEFAULT_LANES = 6;

    private static final int SIDE_PANEL_WIDTH = 320;
    private static final int INITIAL_WINDOW_WIDTH = 1200;
    private static final int INITIAL_WINDOW_HEIGHT = 720;

    private final Random rng = new Random();
    private Race race;
    private TrackPanel trackPanel;
    private SidePanel sidePanel;
    private javax.swing.Timer gameTimer;
    private double timeScale = 1.0; // 1.0 real time, can be slowed or sped
    private RaceState state = RaceState.READY;
    private double countdownTime = 3.0; // Pre-race countdown seconds
    private boolean debugOverlay = false;
    private boolean slowMotion = false;

    // Betting system
    private final BettingSystem bettingSystem = new BettingSystem();

    // Replay
    private final ReplayBuffer replayBuffer = new ReplayBuffer();
    private boolean replayMode = false;
    private double replayTime = 0.0;
    private double replaySpeed = 1.0;
    private int selectedHorseIndex = -1; // For camera follow


    private WeatherType weather = WeatherType.CLEAR;


    private CameraMode cameraMode = CameraMode.FOLLOW_LEADER;


    private final DecimalFormat df1 = new DecimalFormat("0.0");
    private final DecimalFormat df2 = new DecimalFormat("0.00");

    enum RaceState {
        READY, COUNTDOWN, RUNNING, FINISHED, PAUSED, REPLAY
    }

    enum WeatherType {
        CLEAR, RAIN, WINDY
    }

    enum CameraMode {
        FIXED, FOLLOW_LEADER, FOLLOW_SELECTED
    }

    // ------------- Constructor -------------
    public HorseRacingSimulator() {
        super("Horse Racing Simulator - Single File");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        race = new Race(DEFAULT_HORSE_COUNT, DEFAULT_LANES, DEFAULT_TRACK_LENGTH, rng, weather);
        trackPanel = new TrackPanel();
        sidePanel = new SidePanel();

        add(trackPanel, BorderLayout.CENTER);
        add(sidePanel, BorderLayout.EAST);

        setSize(INITIAL_WINDOW_WIDTH, INITIAL_WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        addKeyListener(this);
        setFocusable(true);

        // Swing Timer for the game loop
        gameTimer = new javax.swing.Timer(TIMER_DELAY_MS, e -> onTick());
        gameTimer.start();

        // Set initial camera to leader
        cameraMode = CameraMode.FOLLOW_LEADER;
        selectedHorseIndex = 0;
    }

    // ------------- Game Loop -------------
    private void onTick() {
        double dt = 1.0 / FPS;
        dt *= timeScale;
        dt = Math.max(0.0001, dt);
        double simDT = slowMotion ? dt * 0.33 : dt;

        // Update depending on state
        switch (state) {
            case READY:
                // Idle waiting for "start"
                break;
            case COUNTDOWN:
                countdownTime -= dt;
                if (countdownTime <= 0) {
                    countdownTime = 0;
                    state = RaceState.RUNNING;
                }
                break;
            case RUNNING:
                race.update(simDT);
                // store for replay
                replayBuffer.record(race);
                if (race.isFinished()) {
                    state = RaceState.FINISHED;
                    // settle bets
                    settleBets();
                }
                break;
            case FINISHED:
                // Idle at finish, allow replay or reset
                break;
            case PAUSED:
                // Do nothing
                break;
            case REPLAY:
                // Replay mode: play frames back
                replayTime += dt * replaySpeed;
                if (!replayBuffer.applyToRace(race, replayTime)) {
                    // At end of replay
                    replayTime = 0.0;
                }
                break;
        }
        trackPanel.repaint();
    }

    // ------------- Betting -------------
    private void settleBets() {
        // Determine first place horse
        Horse winner = race.getWinner();
        if (winner == null) return;

        bettingSystem.settle(winner);
        sidePanel.refreshBetLabels();
    }

    // ------------- Entry point -------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HorseRacingSimulator app = new HorseRacingSimulator();
            app.setVisible(true);
        });
    }

    // ------------- Key Controls -------------
    @Override
    public void keyTyped(KeyEvent e) {
        // not used
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        switch (code) {
            case KeyEvent.VK_SPACE:
                if (state == RaceState.RUNNING) {
                    state = RaceState.PAUSED;
                } else if (state == RaceState.PAUSED) {
                    state = RaceState.RUNNING;
                } else if (state == RaceState.READY) {
                    startCountdown();
                } else if (state == RaceState.COUNTDOWN) {
                    // skip to start
                    countdownTime = 0;
                } else if (state == RaceState.FINISHED) {
                    // restart race quickly
                    resetRace(false);
                } else if (state == RaceState.REPLAY) {
                    // exit replay, back to finished state
                    replayMode = false;
                    state = RaceState.FINISHED;
                }
                break;
            case KeyEvent.VK_R:
                resetRace(true);
                break;
            case KeyEvent.VK_D:
                debugOverlay = !debugOverlay;
                break;
            case KeyEvent.VK_P:
                toggleReplay();
                break;
            case KeyEvent.VK_C:
                cycleCameraMode();
                break;
            case KeyEvent.VK_S:
                slowMotion = !slowMotion;
                sidePanel.slowMoCheck.setSelected(slowMotion);
                break;
            case KeyEvent.VK_LEFT:
                selectHorseRelative(-1);
                break;
            case KeyEvent.VK_RIGHT:
                selectHorseRelative(1);
                break;
            default:
                // Quick bet with number keys 1..9
                if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    int idx = code - KeyEvent.VK_1;
                    if (idx < race.horses.size()) {
                        selectedHorseIndex = idx;
                        sidePanel.horseList.setSelectedIndex(idx);
                        placeQuickBet(idx);
                    }
                }
        }
        trackPanel.repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // not used
    }

    // ------------- Control helpers -------------
    private void startCountdown() {
        if (state == RaceState.READY || state == RaceState.FINISHED) {
            countdownTime = 3.0;
            state = RaceState.COUNTDOWN;
            replayBuffer.clear();
            replayMode = false;
            replayTime = 0.0;
        }
    }

    private void resetRace(boolean fullReset) {
        state = RaceState.READY;
        countdownTime = 3.0;
        if (fullReset) {
            bettingSystem.resetForNewRace();
        }
        race = new Race(DEFAULT_HORSE_COUNT, DEFAULT_LANES, DEFAULT_TRACK_LENGTH, rng, weather);
        replayBuffer.clear();
        replayMode = false;
        replayTime = 0.0;
        trackPanel.cameraX = 0;
        selectedHorseIndex = 0;
        sidePanel.refreshAll();
    }

    private void toggleReplay() {
        if (state == RaceState.FINISHED && replayBuffer.hasData()) {
            state = RaceState.REPLAY;
            replayMode = true;
            replayTime = 0.0;
        } else if (state == RaceState.REPLAY) {
            state = RaceState.FINISHED;
            replayMode = false;
        }
    }

    private void cycleCameraMode() {
        switch (cameraMode) {
            case FIXED: cameraMode = CameraMode.FOLLOW_LEADER; break;
            case FOLLOW_LEADER: cameraMode = CameraMode.FOLLOW_SELECTED; break;
            case FOLLOW_SELECTED: cameraMode = CameraMode.FIXED; break;
        }
        sidePanel.cameraModeCombo.setSelectedItem(cameraMode);
    }

    private void selectHorseRelative(int delta) {
        if (race.horses.isEmpty()) return;
        int n = race.horses.size();
        int idx = selectedHorseIndex < 0 ? 0 : selectedHorseIndex;
        idx = (idx + delta + n) % n;
        selectedHorseIndex = idx;
        sidePanel.horseList.setSelectedIndex(idx);
        trackPanel.repaint();
    }

    private void placeQuickBet(int horseIndex) {
        // Quick bet is 10% of wallet, rounded
        int amount = Math.max(10, (int) (bettingSystem.wallet * 0.10));
        bettingSystem.placeBet(horseIndex, amount, race);
        sidePanel.refreshBetLabels();
    }

    // ------------- Inner classes -------------

    /**
     * The main panel rendering the track, horses, HUD, and handling camera/visual effects.
     */
    private class TrackPanel extends JPanel {

        // Camera state
        double cameraX = 0;        // Current camera x offset in world space
        double cameraTargetX = 0;  // Target to ease towards
        double cameraEase = 0.1;   // Easing factor
        int groundMargin = 80;

        // Background elements
        double flagTick = 0;
        List<Particle> particles = new ArrayList<>(); // Rain/snow/leaves etc. Using for rain/windy debris

        TrackPanel() {
            setBackground(new Color(20, 20, 30));
            setPreferredSize(new Dimension(INITIAL_WINDOW_WIDTH - SIDE_PANEL_WIDTH, INITIAL_WINDOW_HEIGHT));
            setDoubleBuffered(true);

            // Mouse hover displays info - optional
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            // Show hint for camera and controls
            return "Space: Start/Pause | R: Reset | P: Replay | C: Camera | S: SlowMo | D: Debug | Left/Right: Select Horse";
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Paint sky
            paintSky(g2, w, h);

            // Camera logic
            updateCamera(w, h);

            // Draw far background (crowd, stands)
            paintGrandstand(g2, w, h);
            paintCrowd(g2, w, h);

            // Draw track
            paintTrack(g2, w, h);

            // Draw finish banner at trackLength
            paintFinishLine(g2);

            // Draw horses
            for (Horse horse : race.horses) {
                paintHorse(g2, horse);
            }

            // Particles (weather)
            updateAndPaintParticles(g2, w, h);

            // HUD overlays
            paintHUD(g2, w, h);

            g2.dispose();
        }

        private void updateCamera(int w, int h) {
            // Determine target camera X based on camera mode
            double margin = 160.0;
            double minCam = 0;
            double maxCam = Math.max(0, race.trackLength - (w - SIDE_PANEL_WIDTH));

            switch (cameraMode) {
                case FIXED:
                    cameraTargetX = 0;
                    break;
                case FOLLOW_LEADER:
                    Horse leader = race.getLeader();
                    if (leader != null) {
                        cameraTargetX = leader.x - (getWidth() - SIDE_PANEL_WIDTH) * 0.4;
                    }
                    break;
                case FOLLOW_SELECTED:
                    if (selectedHorseIndex >= 0 && selectedHorseIndex < race.horses.size()) {
                        Horse sel = race.horses.get(selectedHorseIndex);
                        cameraTargetX = sel.x - (getWidth() - SIDE_PANEL_WIDTH) * 0.4;
                    }
                    break;
            }
            cameraTargetX = Math.max(minCam, Math.min(maxCam, cameraTargetX));

            // Ease camera
            cameraX += (cameraTargetX - cameraX) * cameraEase;
        }

        private void paintSky(Graphics2D g2, int w, int h) {
            Color top = new Color(100, 140, 210);
            Color bottom = new Color(160, 190, 235);
            GradientPaint gp = new GradientPaint(0, 0, top, 0, h, bottom);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            // Sun
            g2.setColor(new Color(255, 255, 200, 200));
            g2.fillOval(w - 180, 50, 120, 120);

            // Flags waving
            flagTick += 0.03;
            int flagCount = 6;
            for (int i = 0; i < flagCount; i++) {
                int x = (int) (i * 180 + 120 - cameraX * 0.15);
                int y = 80;
                paintFlag(g2, x, y, i);
            }
        }

        private void paintFlag(Graphics2D g2, int x, int y, int i) {
            g2.setColor(new Color(90, 90, 90));
            g2.fillRect(x - 2, y - 50, 4, 100);
            // Flag cloth
            Path2D p = new Path2D.Double();
            double wave = Math.sin(flagTick * 1.5 + i) * 6.0;
            p.moveTo(x + 2, y - 30);
            p.curveTo(x + 40 + wave, y - 30, x + 60 + wave, y - 20, x + 100, y - 25);
            p.lineTo(x + 100, y - 5);
            p.curveTo(x + 60 + wave, y - 5, x + 40 + wave, y - 15, x + 2, y - 10);
            p.closePath();
            g2.setColor(new Color(200, 30 + i * 20, 30 + i * 10));
            g2.fill(p);
            g2.setColor(Color.BLACK);
            g2.draw(p);
        }

        private void paintGrandstand(Graphics2D g2, int w, int h) {
            int baseY = h / 2 - 60;
            g2.setColor(new Color(80, 80, 96));
            for (int i = 0; i < 4; i++) {
                int y = baseY - i * 30;
                g2.fillRoundRect(-1000, y, w + 2000, 22, 12, 12);
            }
        }

        private void paintCrowd(Graphics2D g2, int w, int h) {
            int baseY = h / 2 - 100;
            Random r = new Random(1234);
            int cols = 300;
            int spacing = 16;
            for (int i = 0; i < cols; i++) {
                int cx = (int) (i * spacing - cameraX * 0.3);
                int cy = baseY + r.nextInt(60);
                int c = 100 + r.nextInt(120);
                g2.setColor(new Color(c, 60 + r.nextInt(80), 60 + r.nextInt(80)));
                g2.fillRect(cx, cy, 10, 10);
            }
        }

        private void paintTrack(Graphics2D g2, int w, int h) {
            // Grass
            g2.setColor(new Color(80, 150, 80));
            g2.fillRect(0, h / 2, w, h / 2);

            // Dirt track
            int trackTop = h / 2 + 30;
            int trackHeight = (race.lanes + 1) * race.laneSpacing;
            g2.setColor(new Color(171, 133, 90));
            g2.fillRect(0, trackTop, w, trackHeight);

            // Lanes and start line
            for (int lane = 0; lane < race.lanes; lane++) {
                int y = race.laneY(lane);
                g2.setColor(new Color(140, 99, 66));
                g2.fillRect(0, y - race.laneSpacing / 2, w, race.laneSpacing - 2);
                // lane marker
                g2.setColor(new Color(230, 230, 230, 140));
                Stroke old = g2.getStroke();
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{10, 10}, 0));
                g2.drawLine(0, y - race.laneSpacing / 2, w, y - race.laneSpacing / 2);
                g2.setStroke(old);
            }

            // Start line at x = 0
            g2.setColor(new Color(240, 240, 240));
            int sx = (int) (-cameraX);
            g2.fillRect(sx, trackTop - 10, 6, trackHeight + 40);
        }

        private void paintFinishLine(Graphics2D g2) {
            int h = getHeight();
            int trackTop = h / 2 + 30;
            int trackHeight = (race.lanes + 1) * race.laneSpacing;

            int worldX = race.trackLength;
            int screenX = (int) (worldX - cameraX);

            // Checker finish line
            for (int y = trackTop - 10; y < trackTop + trackHeight + 40; y += 20) {
                g2.setColor(Color.WHITE);
                g2.fillRect(screenX - 4, y, 8, 10);
                g2.setColor(Color.BLACK);
                g2.fillRect(screenX - 4, y + 10, 8, 10);
            }

            // "FINISH" banner above
            g2.setColor(new Color(20, 20, 20, 180));
            g2.fillRoundRect(screenX - 50, trackTop - 60, 100, 24, 8, 8);
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            drawCenteredString(g2, "FINISH", new Rectangle(screenX - 50, trackTop - 60, 100, 24));
        }

        private void paintHorse(Graphics2D g2, Horse horse) {
            // Calculate screen position
            int sx = (int) (horse.x - cameraX);
            int sy = race.laneY(horse.lane) - 6;

            // Shadow
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillOval(sx - 24, sy + 18, 50, 10);

            // Body and head
            // Use the animationPhase to swing legs and tail slightly
            double phase = horse.animPhase;
            Color coat = horse.color;
            Color darker = coat.darker();

            // Outline
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(coat);

            // Body
            RoundRectangle2D body = new RoundRectangle2D.Double(sx - 22, sy - 6, 58, 24, 12, 12);
            g2.setColor(coat);
            g2.fill(body);
            g2.setColor(darker);
            g2.draw(body);

            // Head
            Ellipse2D head = new Ellipse2D.Double(sx + 30, sy - 2, 18, 14);
            g2.setColor(coat);
            g2.fill(head);
            g2.setColor(darker);
            g2.draw(head);

            // Ear
            Polygon ear = new Polygon(new int[]{sx + 40, sx + 44, sx + 36}, new int[]{sy - 6, sy - 10, sy - 7}, 3);
            g2.setColor(coat.darker());
            g2.fillPolygon(ear);

            // Legs - front pair and back pair with phase offset
            g2.setStroke(new BasicStroke(2.8f));
            g2.setColor(new Color(40, 30, 25));
            double legSwing = Math.sin(phase) * 6;
            double legSwing2 = Math.sin(phase + Math.PI) * 6;

            // Front legs
            int fx = sx + 20, fy = sy + 18;
            g2.drawLine(fx, fy, fx + (int) legSwing, fy + 12);
            g2.drawLine(fx + 6, fy, fx + 6 - (int) legSwing, fy + 12);

            // Back legs
            int bx = sx - 6, by = sy + 18;
            g2.drawLine(bx, by, bx + (int) legSwing2, by + 12);
            g2.drawLine(bx + 6, by, bx + 6 - (int) legSwing2, by + 12);

            // Tail
            g2.setStroke(new BasicStroke(3.2f));
            g2.setColor(new Color(60, 40, 30));
            int tailSwing = (int) (Math.sin(phase * 0.8) * 6);
            g2.drawLine(sx - 12, sy + 2, sx - 16 - tailSwing, sy - 4);

            // Number bib
            g2.setColor(new Color(250, 250, 250, 240));
            g2.fillRoundRect(sx + 4, sy, 16, 12, 6, 6);
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
            drawCenteredString(g2, "" + (horse.id + 1), new Rectangle(sx + 4, sy, 16, 12));

            // Name above horse
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
            String name = horse.name;
            FontMetrics fm = g2.getFontMetrics();
            int nx = sx - fm.stringWidth(name) / 2 + 14;
            int ny = sy - 12;
            g2.setColor(new Color(255, 255, 255, 180));
            g2.fillRoundRect(nx - 4, ny - fm.getAscent() + 2, fm.stringWidth(name) + 8, fm.getAscent() + 4, 8, 8);
            g2.setColor(Color.BLACK);
            g2.drawString(name, nx, ny);

            // If finished, draw placing medal
            if (horse.finished) {
                String placeTxt = horse.place <= 3 ? placeMedal(horse.place) : horse.place + "th";
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRoundRect(sx - 30, sy - 50, 60, 18, 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
                drawCenteredString(g2, placeTxt, new Rectangle(sx - 30, sy - 50, 60, 18));
            }

            // Debug: stamina/speed bars
            if (debugOverlay) {
                int barW = 52;
                int barH = 4;
                int bxBar = sx - 20;
                int byBar = sy + 32;
                // stamina
                g2.setColor(new Color(20, 20, 20, 120));
                g2.fillRect(bxBar, byBar, barW, barH);
                g2.setColor(new Color(80, 200, 80));
                int sw = (int) (barW * (horse.stamina / horse.maxStamina));
                g2.fillRect(bxBar, byBar, sw, barH);
                // speed
                g2.setColor(new Color(20, 20, 20, 120));
                g2.fillRect(bxBar, byBar + 6, barW, barH);
                g2.setColor(new Color(80, 140, 220));
                double speedNorm = horse.vel / horse.topSpeed;
                int vw = (int) (barW * Math.max(0, Math.min(1, speedNorm)));
                g2.fillRect(bxBar, byBar + 6, vw, barH);
            }
        }

        private String placeMedal(int place) {
            switch (place) {
                case 1: return "1st";
                case 2: return "2nd";
                case 3: return "3rd";
                default: return place + "th";
            }
        }

        private void updateAndPaintParticles(Graphics2D g2, int w, int h) {
            // Spawn depending on weather
            if (weather == WeatherType.RAIN && particles.size() < 200) {
                for (int i = 0; i < 6; i++) {
                    particles.add(Particle.rainParticle(rng, w, h, cameraX));
                }
            } else if (weather == WeatherType.WINDY && particles.size() < 120) {
                if (rng.nextDouble() < 0.5) {
                    particles.add(Particle.debrisParticle(rng, w, h, cameraX));
                }
            }

            // Update and draw
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.update(1.0 / FPS, cameraX, weather);
                if (p.isDead(h)) {
                    it.remove();
                    continue;
                }
                p.draw(g2, cameraX);
            }
        }

        private void paintHUD(Graphics2D g2, int w, int h) {
            // Top info bar
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(12, 12, 350, 86, 12, 12);
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            g2.drawString("Horse Racing Simulator", 24, 32);

            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
            String st = "State: " + state;
            if (state == RaceState.COUNTDOWN) {
                st += " (" + df1.format(countdownTime) + "s)";
            }
            g2.drawString(st, 24, 52);

            String t = "Race Time: " + df1.format(race.timeElapsed) + " s";
            g2.drawString(t, 24, 68);
            String cam = "Cam: " + cameraMode + (cameraMode == CameraMode.FOLLOW_SELECTED && selectedHorseIndex >= 0 ? " #" + (selectedHorseIndex + 1) : "");
            g2.drawString(cam, 24, 84);

            // Minimap
            int mmX = 380, mmY = 12, mmW = 220, mmH = 76;
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(mmX, mmY, mmW, mmH, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(mmX, mmY, mmW, mmH, 12, 12);
            // track line
            int lineY = mmY + mmH / 2;
            g2.drawLine(mmX + 12, lineY, mmX + mmW - 12, lineY);
            // finish marker
            g2.setColor(Color.RED);
            g2.fillRect(mmX + mmW - 24, lineY - 8, 4, 16);
            // horses
            for (Horse horse : race.horses) {
                double p = Math.max(0, Math.min(1, horse.x / (double) race.trackLength));
                int hx = (int) (mmX + 12 + p * (mmW - 36));
                g2.setColor(horse.color);
                g2.fillOval(hx - 5, lineY - 5, 10, 10);
                if (selectedHorseIndex == horse.id) {
                    g2.setColor(Color.WHITE);
                    g2.drawOval(hx - 7, lineY - 7, 14, 14);
                }
            }

            // If paused or finished, show overlay
            if (state == RaceState.PAUSED || state == RaceState.FINISHED || state == RaceState.READY || state == RaceState.REPLAY || state == RaceState.COUNTDOWN) {
                String big = "";
                if (state == RaceState.READY) big = "Press Space to Start";
                if (state == RaceState.COUNTDOWN) big = "Race Starts in " + df1.format(countdownTime) + "s";
                if (state == RaceState.PAUSED) big = "Paused";
                if (state == RaceState.FINISHED) big = "Finished - Press R to Reset or P to Replay";
                if (state == RaceState.REPLAY) big = "Replay " + df1.format(replayTime) + "s (P to Exit)";

                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRoundRect(w / 2 - 220, 20, 440, 40, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
                drawCenteredString(g2, big, new Rectangle(w / 2 - 220, 20, 440, 40));
            }

            // Leaderboard
            paintLeaderboard(g2, w, h);

            // Debug details
            if (debugOverlay) {
                paintDebug(g2, w, h);
            }
        }

        private void paintLeaderboard(Graphics2D g2, int w, int h) {
            List<Horse> order = race.getCurrentOrder();
            int x = w - SIDE_PANEL_WIDTH - 240;
            int y = 12;
            int rowH = 20;
            int boxW = 220;
            int boxH = 12 + rowH * Math.min(order.size(), 8) + 12;

            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(x, y, boxW, boxH, 12, 12);
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
            g2.drawString("Leaderboard", x + 10, y + 20);

            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
            for (int i = 0; i < order.size() && i < 8; i++) {
                Horse hhh = order.get(i);
                int yy = y + 10 + (i + 1) * rowH;
                g2.setColor(hhh.color);
                g2.fillOval(x + 10, yy - 10, 14, 14);
                g2.setColor(Color.WHITE);
                String name = (i + 1) + ". #" + (hhh.id + 1) + " " + hhh.name;
                g2.drawString(name, x + 30, yy);
                String dist = df1.format(Math.max(0, race.trackLength - hhh.x)) + " px left";
                g2.drawString(dist, x + boxW - 100, yy);
            }
        }

        private void paintDebug(Graphics2D g2, int w, int h) {
            int x = 12;
            int y = h - 180;
            int boxW = 440;
            int boxH = 160;

            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect(x, y, boxW, boxH, 12, 12);
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            g2.drawString("Debug", x + 10, y + 20);

            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
            int yy = y + 40;

            String s1 = "Horses: " + race.horses.size() + "  Weather: " + weather + "  TimeScale: " + df2.format(timeScale) + "  SlowMo: " + slowMotion;
            g2.drawString(s1, x + 10, yy); yy += 16;

            Horse leader = race.getLeader();
            if (leader != null) {
                String s2 = "Leader: #" + (leader.id + 1) + " " + leader.name + "  x=" + df1.format(leader.x) + " v=" + df1.format(leader.vel)
                        + " st=" + df1.format(leader.stamina) + "/" + df1.format(leader.maxStamina);
                g2.drawString(s2, x + 10, yy); yy += 16;
            }

            String s3 = "Camera: " + cameraMode + " camX=" + df1.format(cameraX) + " target=" + df1.format(cameraTargetX);
            g2.drawString(s3, x + 10, yy); yy += 16;

            String s4 = "Race: length=" + race.trackLength + "px  elapsed=" + df1.format(race.timeElapsed) + "s  state=" + state;
            g2.drawString(s4, x + 10, yy); yy += 16;

            String s5 = "Replay: frames=" + replayBuffer.size() + " time=" + df1.format(replayTime) + " speed=" + df1.format(replaySpeed) + " active=" + (state == RaceState.REPLAY);
            g2.drawString(s5, x + 10, yy); yy += 16;

            if (selectedHorseIndex >= 0 && selectedHorseIndex < race.horses.size()) {
                Horse sel = race.horses.get(selectedHorseIndex);
                String s6 = "Selected: #" + (sel.id + 1) + " " + sel.name + " base=" + df1.format(sel.baseSpeed)
                        + " top=" + df1.format(sel.topSpeed) + " accel=" + df2.format(sel.accel)
                        + " fatigue=" + df2.format(sel.fatigueRate) + " rnd=" + df2.format(sel.randomFactor);
                g2.drawString(s6, x + 10, yy); yy += 16;
                String s7 = "Status: x=" + df1.format(sel.x) + " v=" + df1.format(sel.vel) + " stamina=" + df1.format(sel.stamina)
                        + " burst=" + df1.format(sel.burstTimer) + "/" + df1.format(sel.burstCooldown)
                        + " stumble=" + df1.format(sel.stumbleTimer);
                g2.drawString(s7, x + 10, yy); yy += 16;
            }
        }

        private void drawCenteredString(Graphics2D g2, String text, Rectangle rect) {
            FontMetrics metrics = g2.getFontMetrics(g2.getFont());
            int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
            int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
            g2.drawString(text, x, y);
        }
    }

    /**
     * Particle used for weather effects (rain drops, debris leaves).
     */
    private static class Particle {
        double x, y;
        double vx, vy;
        double ax, ay;
        double life;
        double maxLife;
        Color color;
        boolean rain;

        static Particle rainParticle(Random rng, int w, int h, double camX) {
            Particle p = new Particle();
            p.x = rng.nextDouble() * (w + 400) + camX - 200;
            p.y = -20;
            p.vx = -20 + rng.nextDouble() * 40;
            p.vy = 250 + rng.nextDouble() * 150;
            p.ax = 0;
            p.ay = 0;
            p.life = 0;
            p.maxLife = 3.0;
            p.color = new Color(200, 220, 255, 180);
            p.rain = true;
            return p;
        }

        static Particle debrisParticle(Random rng, int w, int h, double camX) {
            Particle p = new Particle();
            p.x = rng.nextDouble() * (w + 400) + camX - 200;
            p.y = h / 2 + 40 + rng.nextDouble() * (h / 2 - 120);
            p.vx = 60 + rng.nextDouble() * 180;
            p.vy = -40 + rng.nextDouble() * 80;
            p.ax = 0;
            p.ay = 60;
            p.life = 0;
            p.maxLife = 4.0;
            p.color = new Color(180 + rng.nextInt(60), 120 + rng.nextInt(40), 60);
            p.rain = false;
            return p;
        }

        void update(double dt, double camX, WeatherType weather) {
            life += dt;
            vx += ax * dt;
            vy += ay * dt;
            x += vx * dt;
            y += vy * dt;

            if (rain) {
                // Slight wind effect
                if (weather == WeatherType.WINDY) {
                    vx += 30 * dt;
                }
            }
        }

        boolean isDead(int h) {
            return life > maxLife || y > h + 40;
        }

        void draw(Graphics2D g2, double camX) {
            if (rain) {
                g2.setColor(color);
                int sx = (int) (x - camX);
                int sy = (int) y;
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(sx, sy, sx - 2, sy - 8);
            } else {
                g2.setColor(color);
                int sx = (int) (x - camX);
                int sy = (int) y;
                g2.fillOval(sx, sy, 3, 3);
            }
        }
    }

    /**
     * The side panel with controls and betting UI.
     */
    private class SidePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        // Components
        JButton startBtn, pauseBtn, resetBtn, replayBtn, randomizeBtn;
        JCheckBox slowMoCheck, debugCheck;
        JComboBox<CameraMode> cameraModeCombo;
        JComboBox<WeatherType> weatherCombo;
        JSlider timeScaleSlider;

        JList<String> horseList;
        DefaultListModel<String> horseListModel;

        // Betting
        JLabel walletLabel, placedLabel, potentialLabel, resultLabel;
        JSpinner betAmountSpinner;
        JButton placeBetBtn, clearBetBtn;

        SidePanel() {
            setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, INITIAL_WINDOW_HEIGHT));
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(8, 8, 8, 8));
            setBackground(new Color(245, 245, 250));

            JPanel top = new JPanel();
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            top.setOpaque(false);

            // Title
            JLabel title = new JLabel("Controls");
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
            top.add(title);
            top.add(Box.createVerticalStrut(8));

            // Buttons row
            JPanel controls = new JPanel(new GridLayout(3, 2, 6, 6));
            controls.setOpaque(false);
            startBtn = new JButton("Start");
            pauseBtn = new JButton("Pause");
            resetBtn = new JButton("Reset");
            replayBtn = new JButton("Replay");
            randomizeBtn = new JButton("Randomize");
            controls.add(startBtn);
            controls.add(pauseBtn);
            controls.add(resetBtn);
            controls.add(replayBtn);
            controls.add(randomizeBtn);
            controls.add(new JLabel("")); // filler
            top.add(controls);

            // Toggles
            JPanel toggles = new JPanel(new GridLayout(2, 2, 6, 6));
            toggles.setOpaque(false);
            slowMoCheck = new JCheckBox("Slow motion (S)");
            debugCheck = new JCheckBox("Debug overlay (D)");
            cameraModeCombo = new JComboBox<>(CameraMode.values());
            weatherCombo = new JComboBox<>(WeatherType.values());
            toggles.add(slowMoCheck);
            toggles.add(debugCheck);
            toggles.add(labeled("Camera:", cameraModeCombo));
            toggles.add(labeled("Weather:", weatherCombo));
            top.add(Box.createVerticalStrut(8));
            top.add(toggles);

            // Time scale
            JPanel tsPanel = new JPanel(new BorderLayout(6, 6));
            tsPanel.setOpaque(false);
            JLabel tsLabel = new JLabel("Time Scale: ");
            timeScaleSlider = new JSlider(25, 300, 100);
            timeScaleSlider.setMajorTickSpacing(25);
            timeScaleSlider.setPaintTicks(true);
            JLabel tsValue = new JLabel("1.00x");
            tsPanel.add(tsLabel, BorderLayout.WEST);
            tsPanel.add(timeScaleSlider, BorderLayout.CENTER);
            tsPanel.add(tsValue, BorderLayout.EAST);
            top.add(Box.createVerticalStrut(8));
            top.add(tsPanel);

            // Horse List
            JLabel horsesTitle = new JLabel("Horses");
            horsesTitle.setFont(horsesTitle.getFont().deriveFont(Font.BOLD, 16f));
            top.add(Box.createVerticalStrut(10));
            top.add(horsesTitle);

            horseListModel = new DefaultListModel<>();
            horseList = new JList<>(horseListModel);
            horseList.setVisibleRowCount(8);
            horseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane listScroll = new JScrollPane(horseList);
            listScroll.setPreferredSize(new Dimension(280, 160));
            top.add(listScroll);

            // Betting
            top.add(Box.createVerticalStrut(10));
            JLabel betTitle = new JLabel("Betting");
            betTitle.setFont(betTitle.getFont().deriveFont(Font.BOLD, 16f));
            top.add(betTitle);

            JPanel betPanel = new JPanel();
            betPanel.setLayout(new BoxLayout(betPanel, BoxLayout.Y_AXIS));
            betPanel.setOpaque(false);
            walletLabel = new JLabel();
            placedLabel = new JLabel();
            potentialLabel = new JLabel();
            resultLabel = new JLabel();
            resultLabel.setForeground(new Color(30, 130, 30));
            betAmountSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 1000000, 10));
            placeBetBtn = new JButton("Place Bet");
            clearBetBtn = new JButton("Clear Bet");

            betPanel.add(walletLabel);
            betPanel.add(placedLabel);
            betPanel.add(potentialLabel);
            betPanel.add(resultLabel);
            betPanel.add(Box.createVerticalStrut(6));
            betPanel.add(labeled("Bet Amount:", betAmountSpinner));
            JPanel bpBtns = new JPanel(new GridLayout(1, 2, 6, 6));
            bpBtns.setOpaque(false);
            bpBtns.add(placeBetBtn);
            bpBtns.add(clearBetBtn);
            betPanel.add(bpBtns);

            top.add(betPanel);

            add(top, BorderLayout.NORTH);

            // Event listeners
            startBtn.addActionListener(e -> startCountdown());
            pauseBtn.addActionListener(e -> {
                if (state == RaceState.RUNNING) state = RaceState.PAUSED;
                else if (state == RaceState.PAUSED) state = RaceState.RUNNING;
            });
            resetBtn.addActionListener(e -> resetRace(true));
            replayBtn.addActionListener(e -> toggleReplay());
            randomizeBtn.addActionListener(e -> {
                resetRace(true);
                // randomize weather for variety
                weatherCombo.setSelectedIndex(rng.nextInt(WeatherType.values().length));
            });

            slowMoCheck.addActionListener(e -> slowMotion = slowMoCheck.isSelected());
            debugCheck.addActionListener(e -> debugOverlay = debugCheck.isSelected());
            cameraModeCombo.addActionListener(e -> {
                cameraMode = (CameraMode) cameraModeCombo.getSelectedItem();
            });
            weatherCombo.addActionListener(e -> {
                weather = (WeatherType) weatherCombo.getSelectedItem();
                race.applyWeather(weather);
            });

            timeScaleSlider.addChangeListener(e -> {
                timeScale = timeScaleSlider.getValue() / 100.0;
                tsValue.setText(df2.format(timeScale) + "x");
            });

            horseList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    selectedHorseIndex = horseList.getSelectedIndex();
                    if (cameraMode == CameraMode.FOLLOW_SELECTED) {
                        // nothing else needed
                    }
                }
            });

            placeBetBtn.addActionListener(e -> {
                int idx = horseList.getSelectedIndex();
                int amount = (int) (Integer) betAmountSpinner.getValue();
                bettingSystem.placeBet(idx, amount, race);
                refreshBetLabels();
            });

            clearBetBtn.addActionListener(e -> {
                bettingSystem.clearBet();
                refreshBetLabels();
            });

            // Fill initial values
            cameraModeCombo.setSelectedItem(cameraMode);
            weatherCombo.setSelectedItem(weather);
            refreshAll();
        }

        private JPanel labeled(String label, JComponent component) {
            JPanel p = new JPanel(new BorderLayout(6, 6));
            p.setOpaque(false);
            JLabel l = new JLabel(label);
            p.add(l, BorderLayout.WEST);
            p.add(component, BorderLayout.CENTER);
            return p;
        }

        private void refreshAll() {
            refreshHorseList();
            refreshBetLabels();
            slowMoCheck.setSelected(slowMotion);
            debugCheck.setSelected(debugOverlay);
        }

        private void refreshHorseList() {
            horseListModel.clear();
            for (Horse h : race.horses) {
                String s = "#" + (h.id + 1) + " " + h.name + "  spd:" + df1.format(h.baseSpeed) + "-" + df1.format(h.topSpeed);
                horseListModel.addElement(s);
            }
            if (selectedHorseIndex >= 0 && selectedHorseIndex < horseListModel.size()) {
                horseList.setSelectedIndex(selectedHorseIndex);
            } else if (horseListModel.size() > 0) {
                horseList.setSelectedIndex(0);
            }
        }

        private void refreshBetLabels() {
            walletLabel.setText("Wallet: $" + bettingSystem.wallet);
            if (bettingSystem.betHorseIndex >= 0) {
                Horse h = race.horses.get(bettingSystem.betHorseIndex);
                double odds = bettingSystem.calculateOdds(h, race);
                placedLabel.setText("Bet on: #" + (h.id + 1) + " " + h.name + " $" + bettingSystem.betAmount);
                potentialLabel.setText("Potential: $" + (int) Math.round(bettingSystem.betAmount * odds) + " (odds x" + df2.format(odds) + ")");
            } else {
                placedLabel.setText("Bet on: -");
                potentialLabel.setText("Potential: -");
            }
            resultLabel.setText(bettingSystem.lastResultText);
        }
    }

    /**
     * The core race simulation and state. Contains horses, track/lanes, physics update.
     */
    private static class Race {
        final List<Horse> horses = new ArrayList<>();
        final Random rng;
        final int lanes;
        final int trackLength;
        final int laneSpacing = 56; // px between lanes vertical
        double timeElapsed = 0.0;
        boolean finished = false;
        final List<Horse> finishOrder = new ArrayList<>();
        WeatherType weather;

        Race(int horseCount, int lanes, int trackLength, Random rng, WeatherType weather) {
            this.rng = rng;
            this.lanes = Math.min(lanes, MAX_HORSE_COUNT);
            this.trackLength = trackLength;
            this.weather = weather;

            // Create horses
            for (int i = 0; i < horseCount; i++) {
                Horse h = new Horse(i, randomName(rng), randomCoatColor(i), i % this.lanes, rng);
                // Randomly tune base abilities
                double base = 120 + rng.nextDouble() * 40; // px/s
                double top = base + 60 + rng.nextDouble() * 40;
                double accel = 70 + rng.nextDouble() * 60;
                double stamina = 80 + rng.nextDouble() * 60;
                double fatigueRate = 6 + rng.nextDouble() * 8;

                h.baseSpeed = base;
                h.topSpeed = top;
                h.accel = accel;
                h.maxStamina = stamina;
                h.stamina = h.maxStamina;
                h.fatigueRate = fatigueRate;
                h.randomFactor = 0.9 + rng.nextDouble() * 0.25;
                if (rng.nextDouble() < 0.2) {
                    // "sprinter" profile
                    h.topSpeed += 40;
                    h.accel += 30;
                    h.maxStamina -= 20;
                }
                if (rng.nextDouble() < 0.2) {
                    // "endurance" profile
                    h.maxStamina += 40;
                    h.fatigueRate *= 0.6;
                    h.baseSpeed -= 10;
                }
                applyWeatherToHorse(h, weather, rng);

                horses.add(h);
            }
        }

        void applyWeather(WeatherType weather) {
            this.weather = weather;
            for (Horse h : horses) {
                applyWeatherToHorse(h, weather, rng);
            }
        }

        private static void applyWeatherToHorse(Horse h, WeatherType weather, Random rng) {
            // Reset per-weather modifiers
            h.weatherDrag = 0;
            h.slipFactor = 1.0;
            h.staminaDrainMult = 1.0;

            switch (weather) {
                case CLEAR:
                    h.weatherDrag = 0;
                    h.slipFactor = 1.0;
                    h.staminaDrainMult = 1.0;
                    break;
                case RAIN:
                    // More slip, slightly more drain, little less speed
                    h.slipFactor = 0.88;
                    h.staminaDrainMult = 1.15;
                    h.weatherDrag = 8;
                    break;
                case WINDY:
                    // Headwinds increase drag, but occasional bursts (tailwind gust)
                    h.weatherDrag = 18;
                    h.staminaDrainMult = 0.95;
                    h.gustChance = 0.02;
                    h.gustPower = 40 + rng.nextDouble() * 20;
                    break;
            }
        }

        static String randomName(Random rng) {
            String[] first = {"Silver", "Thunder", "Midnight", "Rapid", "Crimson", "Northern", "Wild", "Sunny", "Royal", "Stormy", "Merry", "Lucky", "Bold", "Misty", "Golden", "Phantom"};
            String[] second = {"Bolt", "Runner", "Blaze", "Comet", "Whisper", "Rocket", "Spirit", "Charm", "Dancer", "Flame", "Arrow", "Glory", "Star", "Trek", "Mirage", "Echo"};
            return first[rng.nextInt(first.length)] + " " + second[rng.nextInt(second.length)];
        }

        static Color randomCoatColor(int i) {
            // Some distinct horse coat colors
            Color[] palette = {
                    new Color(115, 77, 38),  // bay
                    new Color(80, 60, 40),   // brown
                    new Color(160, 140, 120),// buckskin
                    new Color(200, 180, 160),// dun
                    new Color(220, 220, 210),// grey
                    new Color(60, 40, 30),   // dark bay
                    new Color(150, 100, 60), // chestnut
                    new Color(105, 90, 60),  // seal brown
                    new Color(230, 230, 230) // white
            };
            return palette[i % palette.length];
        }

        void update(double dt) {
            if (finished) return;
            timeElapsed += dt;

            // Update horses
            for (Horse h : horses) {
                h.update(dt, this);
            }

            // Sort by position for placing updates
            horses.sort(Comparator.comparingDouble(h -> -h.x));

            // Check finishing
            for (Horse h : horses) {
                if (!h.finished && h.x >= trackLength) {
                    h.finished = true;
                    h.finishTime = timeElapsed;
                    finishOrder.add(h);
                    h.place = finishOrder.size();
                }
            }
            // Determine finished state
            if (finishOrder.size() == horses.size()) {
                finished = true;
            }
        }

        boolean isFinished() {
            return finished;
        }

        Horse getWinner() {
            return finishOrder.isEmpty() ? null : finishOrder.get(0);
        }

        Horse getLeader() {
            if (horses.isEmpty()) return null;
            return horses.stream().max(Comparator.comparingDouble(h -> h.x)).orElse(null);
        }

        List<Horse> getCurrentOrder() {
            List<Horse> copy = new ArrayList<>(horses);
            copy.sort(Comparator.comparingDouble(h -> -h.x));
            return copy;
        }

        int laneY(int lane) {
            // Compute lane center Y
            // Track top baseline is panel height dependent, but we assume drawing uses this formula from panel:
            // We'll approximate track vertical position by a fixed baseline; actual draw uses panel height
            // Here, we only need laneY values for draw; the panel calls this method, so not exact
            int panelH = 720; // fallback
            try {
                // Try reading from SwingUtilities if needed; not necessary for logic.
            } catch (Exception ignored) {}
            int trackTop = panelH / 2 + 30;
            return trackTop + laneSpacing + lane * laneSpacing;
        }
    }

    /**
     * Represents a single horse and its parameters and animation state.
     */
    private static class Horse {
        final int id;
        final String name;
        final Color color;
        final int lane;
        final Random rng;

        // Kinematics
        double x = 0;
        double vel = 0;
        double accel = 100;

        // Abilities
        double baseSpeed = 140; // px/s comfort speed
        double topSpeed = 220;  // px/s
        double maxStamina = 100;
        double stamina = 100;
        double fatigueRate = 8;  // stamina / s drain
        double recoverRate = 6;  // not used much during race
        double randomFactor = 1.0;

        // Weather effects
        double weatherDrag = 0;   // reduced speed cap
        double slipFactor = 1.0;  // influences effective acceleration
        double staminaDrainMult = 1.0;
        double gustChance = 0.0;
        double gustPower = 0.0;

        // Random events
        double burstTimer = 0;
        double burstCooldown = 2.5;
        double stumbleTimer = 0;

        // Animation
        double animPhase = 0;
        double animRate = 8.0;

        // Status
        boolean finished = false;
        double finishTime = 0;
        int place = 0;

        Horse(int id, String name, Color color, int lane, Random rng) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.lane = lane;
            this.rng = rng;
        }

        void update(double dt, Race race) {
            if (finished) {
                vel = Math.max(0, vel - 40 * dt);
                animPhase += animRate * dt * 0.5;
                return;
            }

            // Random gusts for windy weather
            double gustExtra = 0;
            if (gustChance > 0 && rng.nextDouble() < gustChance * dt) {
                gustExtra += gustPower;
            }

            // Determine target speed = base + bursts
            double target = baseSpeed + gustExtra;

            // Occasional burst if stamina allows
            if (burstTimer <= 0 && stamina > 20 && rng.nextDouble() < 0.5 * dt) {
                target += 40 + rng.nextDouble() * 30;
                burstTimer = burstCooldown + rng.nextDouble() * 1.5;
            } else {
                burstTimer -= dt;
            }

            // Stumble from slip
            if (stumbleTimer <= 0 && rng.nextDouble() < (1.0 - slipFactor) * 0.5 * dt) {
                // stumble, reduce speed
                vel *= 0.75;
                stamina = Math.max(0, stamina - 4);
                stumbleTimer = 1.2 + rng.nextDouble() * 1.8;
            } else {
                stumbleTimer -= dt;
            }

            // Effective top speed reduced by weather drag
            double effectiveTop = topSpeed - weatherDrag;
            effectiveTop = Math.max(100, effectiveTop);

            // Accelerate towards target speed
            double desired = Math.min(target, effectiveTop);
            double a = accel * slipFactor * (desired - vel) / desired;
            vel += a * dt;

            // Natural clamp
            if (vel > effectiveTop) vel = effectiveTop;
            if (vel < 10) vel = 10; // minimal jog

            // Stamina drain depends on effort beyond baseSpeed
            double effort = Math.max(0, vel - baseSpeed) / (effectiveTop - baseSpeed + 0.0001);
            double drain = fatigueRate * staminaDrainMult * (0.6 + 1.2 * effort);
            stamina = Math.max(0, stamina - drain * dt);

            // If stamina very low, reduce achieved velocity
            if (stamina < 10) {
                vel -= 20 * dt;
            }

            // Random micro-variation
            vel *= (0.995 + rng.nextDouble() * 0.01) * randomFactor;

            // Integrate
            x += vel * dt;

            // Anim rate scales with speed
            animRate = 7.5 + Math.min(14, vel / 20);
            animPhase += animRate * dt;

            if (x >= race.trackLength) {
                x = race.trackLength;
            }
        }
    }

    private static class ReplayBuffer {
        static class Frame {
            double time;
            double[] xs;
            double[] vs;
            double[] stams;

            Frame(double time, int n) {
                this.time = time;
                xs = new double[n];
                vs = new double[n];
                stams = new double[n];
            }
        }

        final List<Frame> frames = new ArrayList<>();
        double recordTime = 0.0;

        void clear() {
            frames.clear();
            recordTime = 0.0;
        }

        boolean hasData() {
            return frames.size() > 2;
        }

        int size() {
            return frames.size();
        }

        void record(Race race) {
            int n = race.horses.size();
            Frame f = new Frame(recordTime, n);
            for (int i = 0; i < n; i++) {
                Horse h = race.horses.get(i);
                f.xs[i] = h.x;
                f.vs[i] = h.vel;
                f.stams[i] = h.stamina;
            }
            frames.add(f);
            recordTime += 1.0 / FPS;
        }

        boolean applyToRace(Race race, double time) {
            if (frames.isEmpty()) return false;
            // Find two frames around 'time'
            Frame f0 = frames.get(0);
            Frame f1 = frames.get(frames.size() - 1);
            if (time <= f0.time) time = f0.time;
            if (time >= f1.time) time = f1.time;

            int idx = Collections.binarySearch(frames, null, Comparator.comparingDouble(fr -> fr.time));
            // binarySearch above is tricky with null; do manual search
            int lo = 0, hi = frames.size() - 1;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (frames.get(mid).time < time) lo = mid + 1;
                else hi = mid;
            }
            int i1 = lo;
            int i0 = Math.max(0, i1 - 1);
            f0 = frames.get(i0);
            f1 = frames.get(i1);

            double t0 = f0.time, t1 = f1.time;
            double alpha = t1 <= t0 ? 0 : (time - t0) / (t1 - t0);

            int n = race.horses.size();
            for (int i = 0; i < n; i++) {
                Horse h = race.horses.get(i);
                h.x = lerp(f0.xs[i], f1.xs[i], alpha);
                h.vel = lerp(f0.vs[i], f1.vs[i], alpha);
                h.stamina = lerp(f0.stams[i], f1.stams[i], alpha);
                h.finished = false; // in replay, ignore finished to keep anim flowing
                h.animPhase += h.animRate * (1.0 / FPS);
            }

            race.timeElapsed = time;
            return time < frames.get(frames.size() - 1).time;
        }

        private double lerp(double a, double b, double t) {
            return a + (b - a) * t;
        }
    }

    private static class BettingSystem {
        int wallet = 1000;
        int betHorseIndex = -1;
        int betAmount = 0;
        String lastResultText = "";

        void resetForNewRace() {
            betHorseIndex = -1;
            betAmount = 0;
            lastResultText = "";
        }

        void placeBet(int horseIndex, int amount, Race race) {
            if (horseIndex < 0 || horseIndex >= race.horses.size()) {
                lastResultText = "Select a horse to bet on.";
                return;
            }
            if (amount <= 0) {
                lastResultText = "Bet amount must be > 0.";
                return;
            }
            if (amount > wallet) {
                lastResultText = "Insufficient funds.";
                return;
            }
            betHorseIndex = horseIndex;
            betAmount = amount;
            lastResultText = "Bet placed on #" + (horseIndex + 1) + " for $" + amount + ". Good luck!";
        }

        void clearBet() {
            betHorseIndex = -1;
            betAmount = 0;
            lastResultText = "Bet cleared.";
        }

        void settle(Horse winner) {
            if (betHorseIndex < 0 || betAmount <= 0) {
                lastResultText = "No bet was placed.";
                return;
            }
            if (winner.id == betHorseIndex) {
                double odds = calculateOdds(winner, null);
                int payout = (int) Math.round(betAmount * odds);
                wallet += payout;
                lastResultText = "You won! Payout: $" + payout;
            } else {
                wallet -= betAmount;
                lastResultText = "You lost $" + betAmount + ". Better luck next time!";
            }
            // Reset bet for next race
            betHorseIndex = -1;
            betAmount = 0;
        }

        double calculateOdds(Horse h, Race race) {
            // Odds based on normalized baseSpeed+topSpeed+stamina combination
            double score = h.baseSpeed * 0.4 + h.topSpeed * 0.4 + h.maxStamina * 0.2;
            // Map to multiplier: higher score -> lower odds
            // Reference around 200 -> x2.0; 240 -> x1.6; 300 -> x1.2; 160 -> x2.6
            double mult = 3.0 - (score - 160) / 100.0; // rough mapping
            mult = Math.max(1.15, Math.min(3.5, mult));
            return mult;
        }
    }

    private static void drawCenteredString(Graphics2D g2, String text, Rectangle rect) {
        FontMetrics metrics = g2.getFontMetrics(g2.getFont());
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
        g2.drawString(text, x, y);
    }

}