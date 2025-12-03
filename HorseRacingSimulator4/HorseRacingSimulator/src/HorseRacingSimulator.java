import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;


public class HorseRacingSimulator {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RacingFrame frame = new RacingFrame();
            frame.setVisible(true);
        });
    }
}

class RacingFrame extends JFrame {
    private RacePanel racePanel;
    private ControlPanel controlPanel;
    private InfoPanel infoPanel;

    public RacingFrame() {
        super("Horse Racing Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        racePanel = new RacePanel();
        controlPanel = new ControlPanel(racePanel);
        infoPanel = new InfoPanel(racePanel);

        add(racePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        add(infoPanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setResizable(true);
    }
}

/* ====================================================================== */
/* RacePanel: main drawing area */
/* ====================================================================== */

class RacePanel extends JPanel {
    // Dimensions
    private static final int PREFERRED_WIDTH = 1200;
    private static final int PREFERRED_HEIGHT = 700;

    // Track properties
    private int trackLengthMeters = 1200; // race distance
    private int trackPixels = 800; // renderable track length in pixels
    private int laneHeight = 48;

    // Horses and race state
    private List<Horse> horses = new ArrayList<>();
    private RaceEngine engine;
    private javax.swing.Timer animationTimer;
    private long tickCount = 0;
    private boolean running = false;
    private boolean paused = false;
    private BufferedImage backgroundImage;

    // UI / visuals
    private Font hudFont = new Font("SansSerif", Font.BOLD, 12);
    private DecimalFormat df = new DecimalFormat("0.00");

    // Random seed for reproducibility (can be exposed in UI)
    private Random random = new Random();

    public RacePanel() {
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        setBackground(new Color(34, 139, 34)); // grass green
        setDoubleBuffered(true);

        loadResources();
        resetRace();

        animationTimer = new javax.swing.Timer(16, e -> step()); // ~60 FPS

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                repaint();
            }
        });

        // keyboard controls
        setFocusable(true);
        requestFocusInWindow();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    togglePause();
                } else if (e.getKeyCode() == KeyEvent.VK_R) {
                    resetRace();
                }
            }
        });
    }

    private void loadResources() {
        try {
            // optional background image if present in working dir
            File bg = new File("race_background.jpg");
            if (bg.exists()) {
                backgroundImage = ImageIO.read(bg);
            }
        } catch (IOException ex) {
            // ignore, fallback to plain rendering
        }
    }

    public void startRace() {
        if (engine == null) engine = new RaceEngine(horses, trackLengthMeters, random);
        engine.reset();
        running = true;
        paused = false;
        animationTimer.start();
        requestFocusInWindow();
    }

    public void stopRace() {
        running = false;
        animationTimer.stop();
    }

    public void togglePause() {
        if (!running) return;
        paused = !paused;
        if (paused) animationTimer.stop(); else animationTimer.start();
        repaint();
    }

    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }

    public void restartRace() {
        stopRace();
        resetRace();
        startRace();
    }

    public void resetRace() {
        stopRace();
        tickCount = 0;
        horses.clear();
        // create a few horses with varied parameters
        horses.add(new Horse("Red Thunder", 9.8, 85, 0.95));
        horses.add(new Horse("Blue Comet", 9.6, 90, 1.05));
        horses.add(new Horse("Golden Flash", 10.1, 80, 0.9));
        horses.add(new Horse("Silver Arrow", 9.9, 88, 1.0));
        horses.add(new Horse("Black Stallion", 9.7, 86, 0.97));
        horses.add(new Horse("Emerald Wind", 10.3, 78, 1.1));

        // randomize starting order slightly
        Collections.shuffle(horses, random);

        // reposition horses
        for (int i = 0; i < horses.size(); i++) {
            Horse h = horses.get(i);
            h.setLane(i);
            h.resetForRace();
        }

        engine = new RaceEngine(horses, trackLengthMeters, random);
        repaint();
    }

    public List<Horse> getHorses() { return horses; }
    public RaceEngine getEngine() { return engine; }
    public int getTrackPixels() { return trackPixels; }
    public int getTrackLengthMeters() { return trackLengthMeters; }

    public void setTrackLengthMeters(int m) {
        this.trackLengthMeters = Math.max(200, Math.min(5000, m));
        repaint();
    }

    public void setRandomSeed(long seed) {
        random.setSeed(seed);
    }

    private void step() {
        if (!running || paused) return;
        tickCount++;
        // Advance engine in small fixed time step (seconds)
        double dt = 0.016; // seconds per tick
        engine.update(dt);

        // repaint region
        repaint();

        // Stop if race is done
        if (engine.isRaceFinished()) {
            stopRace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHints(rh);

        int w = getWidth();
        int h = getHeight();

        // draw optional background image
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, w, h, null);
        }

        // draw track area
        int trackX = 50;
        int trackY = 50;
        int trackW = Math.max(600, w - 350);
        int laneCount = Math.max(6, horses.size());
        int laneH = Math.min(laneHeight, (h - 200) / laneCount);
        trackPixels = trackW - 140; // adjust for finish line and margins

        // track base
        g2.setColor(new Color(120, 100, 80));
        g2.fillRoundRect(trackX, trackY, trackW, laneH * laneCount + 20, 20, 20);

        // draw lanes
        for (int i = 0; i < laneCount; i++) {
            int y = trackY + 10 + i * laneH;
            g2.setColor(new Color(210, 180, 140));
            g2.fillRect(trackX + 10, y, trackW - 20, laneH - 6);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(trackX + 10, y, trackW - 20, laneH - 6);
        }

        // draw finish line
        int finishX = trackX + trackW - 80;
        g2.setColor(Color.WHITE);
        for (int y = trackY + 8; y < trackY + laneH * laneCount + 8; y += 8) {
            g2.drawLine(finishX, y, finishX, y + 4);
        }
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(Color.WHITE);
        g2.drawString("FINISH", finishX - 46, trackY - 8);

        // scale: world meters to pixels
        double metersToPixels = (double) trackPixels / (double) trackLengthMeters;

        // draw horses
        for (Horse horse : horses) {
            int lane = horse.getLane();
            int y = trackY + 10 + lane * laneH + (laneH - 36) / 2;

            double xPos = 50 + 20 + horse.getDistanceMeters() * metersToPixels;
            // clamp if finished
            xPos = Math.min(xPos, finishX - 24);

            // draw horse body (simple stylized)
            drawHorse(g2, (int) xPos, y, horse);

            // draw name overlay
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(Color.WHITE);
            g2.drawString(horse.getName(), (int) xPos - 5, y - 8);

            // stamina bar
            int sbW = 80;
            int sbH = 8;
            int sbX = (int) xPos - sbW/2;
            int sbY = y + 32;
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(sbX, sbY, sbW, sbH);
            int filled = (int) (sbW * horse.getStaminaPercent());
            g2.setColor(new Color(50, 200, 50));
            g2.fillRect(sbX, sbY, filled, sbH);
            g2.setColor(Color.WHITE);
            g2.drawRect(sbX, sbY, sbW, sbH);
        }

        // draw mini-map on right
        int miniW = 200;
        int miniH = 120;
        int miniX = w - miniW - 20;
        int miniY = 20;
        g2.setColor(new Color(0,0,0,120));
        g2.fillRoundRect(miniX-8, miniY-8, miniW+16, miniH+16, 12, 12);
        g2.setColor(new Color(255,255,255,200));
        g2.drawRect(miniX, miniY, miniW, miniH);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString("Minimap", miniX + 10, miniY + 16);

        // minimap scale
        double miniScale = (double) (miniW - 20) / (double) trackLengthMeters;
        for (int i = 0; i < horses.size(); i++) {
            Horse hObj = horses.get(i);
            int mx = miniX + 10 + (int) (hObj.getDistanceMeters() * miniScale);
            int my = miniY + 30 + i * 12;
            g2.setColor(hObj.getColor());
            g2.fillOval(mx, my, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawString(hObj.getShortName(), mx + 12, my + 8);
        }

        // HUD and stats
        int hudX = 60;
        int hudY = trackY + laneH * laneCount + 70;
        g2.setFont(hudFont);
        g2.setColor(Color.WHITE);
        g2.drawString("Race Distance: " + trackLengthMeters + " m", hudX, hudY);
        g2.drawString("Horses: " + horses.size(), hudX + 220, hudY);
        g2.drawString("Running: " + running + (paused ? " (paused)" : ""), hudX + 360, hudY);

        // Leaderboard
        List<Horse> sorted = new ArrayList<>(horses);
        sorted.sort(Comparator.comparingDouble(Horse::getDistanceMeters).reversed());
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString("Leaderboard:", miniX, miniY + miniH + 30);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i < Math.min(6, sorted.size()); i++) {
            Horse h = sorted.get(i);
            String line = String.format("%d. %s (%.1fm)", i+1, h.getShortName(), h.getDistanceMeters());
            g2.drawString(line, miniX, miniY + miniH + 50 + i*16);
        }

        // log status
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        if (engine != null) {
            List<String> log = engine.getRecentLog(6);
            for (int i = 0; i < log.size(); i++) {
                g2.drawString(log.get(i), 60, hudY + 26 + i*16);
            }
        }

        // race over overlay
        if (engine != null && engine.isRaceFinished()) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, w, h);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 36));
            g2.drawString("Race Finished!", w/2 - 140, h/2 - 30);
            List<Horse> finishOrder = engine.getFinishOrder();
            g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
            for (int i = 0; i < Math.min(5, finishOrder.size()); i++) {
                Horse fh = finishOrder.get(i);
                g2.drawString(String.format("%d. %s (%.2fs)", i+1, fh.getShortName(), fh.getFinishTime()), w/2 - 140, h/2 + i*26);
            }
        }

        // paused overlay
        if (paused) {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRect(0,0,w,h);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            g2.drawString("PAUSED", w/2 - 60, h/2);
        }

        g2.dispose();
    }

    private void drawHorse(Graphics2D g2, int x, int y, Horse h) {
        // simple stylized horse composed of ellipses and rectangles
        Color c = h.getColor();
        g2.setColor(c);
        g2.fillOval(x, y+8, 28, 20); // body
        g2.fillOval(x-10, y, 18, 18); // head
        g2.fillRect(x+6, y+20, 18, 6); // tail/hip
        // legs
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(x+2, y+26, 4, 12);
        g2.fillRect(x+12, y+26, 4, 12);
        // eye
        g2.setColor(Color.WHITE);
        g2.fillOval(x-4, y+6, 4, 4);
        g2.setColor(Color.BLACK);
        g2.fillOval(x-3, y+7, 2, 2);

        // show speed as small tag
        g2.setColor(new Color(0,0,0,150));
        String spd = df.format(h.getCurrentSpeed() * 3.6) + " km/h"; // m/s -> km/h
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.drawString(spd, x-6, y+40);
    }
}

/* ====================================================================== */
/* ControlPanel: buttons and sliders */
/* ====================================================================== */

class ControlPanel extends JPanel {
    private RacePanel racePanel;
    private JButton startBtn, pauseBtn, restartBtn, resetBtn;
    private JSlider distanceSlider, difficultySlider;
    private JLabel creditsLabel;
    private JComboBox<String> betSelector;
    private JButton betButton;

    private int userCredits = 1000;
    private int currentBetHorse = -1;

    public ControlPanel(RacePanel panel) {
        this.racePanel = panel;
        setLayout(new FlowLayout(FlowLayout.LEFT));

        startBtn = new JButton("Start");
        pauseBtn = new JButton("Pause/Resume");
        restartBtn = new JButton("Start Race (New)");
        resetBtn = new JButton("Reset");

        add(startBtn);
        add(pauseBtn);
        add(restartBtn);
        add(resetBtn);

        startBtn.addActionListener(e -> {
            if (!racePanel.isRunning()) {
                racePanel.startRace();
            }
        });

        pauseBtn.addActionListener(e -> racePanel.togglePause());
        restartBtn.addActionListener(e -> racePanel.restartRace());
        resetBtn.addActionListener(e -> racePanel.resetRace());

        distanceSlider = new JSlider(200, 5000, racePanel.getTrackLengthMeters());
        distanceSlider.setPreferredSize(new Dimension(200, 40));
        distanceSlider.setMajorTickSpacing(1200);
        distanceSlider.setPaintTicks(true);
        distanceSlider.setPaintLabels(true);
        add(new JLabel("Distance (m): "));
        add(distanceSlider);

        distanceSlider.addChangeListener(e -> {
            racePanel.setTrackLengthMeters(distanceSlider.getValue());
        });

        // betting controls
        add(new JLabel("Bet on:"));
        betSelector = new JComboBox<>();
        updateBetSelector();
        add(betSelector);
        betButton = new JButton("Place Bet 100");
        add(betButton);
        creditsLabel = new JLabel("Credits: " + userCredits);
        add(creditsLabel);

        betButton.addActionListener(e -> placeBet());

        // difficulty slider (affects randomness)
        difficultySlider = new JSlider(0, 100, 50);
        difficultySlider.setPreferredSize(new Dimension(160, 40));
        add(new JLabel("Chaos: "));
        add(difficultySlider);

        // Selector refresh on focus (in case horses changed)
        betSelector.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent e) {}
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) { updateBetSelector(); }
        });

        // extra quick keys
        add(new JLabel(" Keys: SPACE pause, R reset "));
    }

    private void updateBetSelector() {
        betSelector.removeAllItems();
        List<Horse> horses = racePanel.getHorses();
        for (int i = 0; i < horses.size(); i++) {
            betSelector.addItem(String.format("%d - %s", i+1, horses.get(i).getShortName()));
        }
    }

    private void placeBet() {
        int idx = betSelector.getSelectedIndex();
        if (idx < 0) return;
        if (userCredits < 100) {
            JOptionPane.showMessageDialog(this, "Not enough credits to bet.");
            return;
        }
        userCredits -= 100;
        creditsLabel.setText("Credits: " + userCredits);
        currentBetHorse = idx;
        JOptionPane.showMessageDialog(this, "Bet placed: 100 credits on " + racePanel.getHorses().get(idx).getShortName());

        // attach to engine when race starts
        if (racePanel.getEngine() != null) {
            racePanel.getEngine().setPlayerBet(idx, 100);
            racePanel.getEngine().setDifficulty(difficultySlider.getValue());
        }
    }
}

/* ====================================================================== */
/* InfoPanel: shows details about horses and race */
/* ====================================================================== */

class InfoPanel extends JPanel {
    private RacePanel racePanel;
    private JTextArea infoArea;
    private javax.swing.Timer refreshTimer;

    public InfoPanel(RacePanel racePanel) {
        this.racePanel = racePanel;
        setPreferredSize(new Dimension(300, 0));
        setLayout(new BorderLayout());

        infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(infoArea);
        add(sp, BorderLayout.CENTER);

        refreshTimer = new javax.swing.Timer(400, e -> refresh());
        refreshTimer.start();
    }

    private void refresh() {
        StringBuilder sb = new StringBuilder();
        sb.append("HORSE DETAILS\n");
        sb.append("============================\n");
        for (Horse h : racePanel.getHorses()) {
            sb.append(String.format("%s (%s)\n", h.getName(), h.getShortName()));
            sb.append(String.format("  Top Speed: %.2f m/s\n", h.getMaxSpeed()));
            sb.append(String.format("  Stamina: %d\n", h.getStaminaMax()));
            sb.append(String.format("  Temperament: %.2f\n", h.getTemperament()));
            sb.append(String.format("  Distance: %.1fm\n", h.getDistanceMeters()));
            sb.append(String.format("  Current Speed: %.2fm/s\n", h.getCurrentSpeed()));
            sb.append("----------------------------\n");
        }
        if (racePanel.getEngine() != null) {
            sb.append("\nRACE LOG:\n");
            for (String s : racePanel.getEngine().getRecentLog(10)) {
                sb.append(s + "\n");
            }
        }
        infoArea.setText(sb.toString());
    }
}

/* ====================================================================== */
/* Horse: model for a single horse */
/* ====================================================================== */

class Horse {
    private String name;
    private double maxSpeed; // m/s (top) - derived from 'seconds per 100m' like stat
    private int staminaMax; // stamina points
    private double temperament; // influences risk-taking and response

    private int lane;
    private double distanceMeters = 0.0;
    private double currentSpeed = 0.0; // m/s
    private int stamina;
    private boolean finished = false;
    private double finishTime = 0.0;
    private Color color;
    private Random rnd = new Random();

    // transient state
    private double staminaDrainRate = 0.2; // default
    private double boostTimer = 0.0;
    private String shortNameCache = null;

    public Horse(String name, double maxSpeed, int stamina, double temperament) {
        this.name = name;
        this.maxSpeed = maxSpeed;
        this.staminaMax = stamina;
        this.temperament = temperament;
        this.stamina = stamina;
        this.color = randomColorFromName(name);
        this.currentSpeed = maxSpeed * 0.6; // start conservatively
    }

    private Color randomColorFromName(String name) {
        int h = Math.abs(name.hashCode());
        int r = 100 + (h & 0xFF) % 156;
        int g = 50 + (h>>8 & 0xFF) % 206;
        int b = 60 + (h>>16 & 0xFF) % 196;
        return new Color(r, g, b);
    }

    public void resetForRace() {
        distanceMeters = 0.0;
        currentSpeed = maxSpeed * 0.6;
        stamina = staminaMax;
        finished = false;
        finishTime = 0.0;
        boostTimer = 0.0;
        shortNameCache = null;
    }

    public void setLane(int l) { this.lane = l; }
    public int getLane() { return lane; }
    public String getName() { return name; }
    public String getShortName() {
        if (shortNameCache == null) shortNameCache = name.length() > 10 ? name.substring(0,10) + ".." : name;
        return shortNameCache;
    }
    public double getMaxSpeed() { return maxSpeed; }
    public int getStaminaMax() { return staminaMax; }
    public double getTemperament() { return temperament; }
    public double getDistanceMeters() { return distanceMeters; }
    public double getCurrentSpeed() { return currentSpeed; }
    public double getStaminaPercent() { return staminaMax <= 0 ? 0 : (double) stamina / staminaMax; }
    public boolean isFinished() { return finished; }
    public void markFinished(double timeSeconds) { finished = true; finishTime = timeSeconds; }
    public double getFinishTime() { return finishTime; }
    public Color getColor() { return color; }

    // update physics for this horse
    public void update(double dt, RaceEngine engine) {
        if (finished) return;

        // basic AI: choose a target speed based on stamina and relative position
        double leaderDist = engine.getLeaderDistance();
        double myDist = distanceMeters;
        double gap = leaderDist - myDist;

        // base target speed: portion of max speed depending on stamina
        double staminaFactor = 0.5 + 0.5 * getStaminaPercent();
        double targetSpeed = maxSpeed * staminaFactor;

        // temperament influences risk taking: more temperamental horses push harder when behind
        if (gap > 5) {
            targetSpeed *= 1.0 + Math.min(0.25, (gap/100.0) * temperament);
        }

        // occasional bursts
        if (rnd.nextDouble() < 0.005 * temperament) {
            boostTimer = 0.25 + rnd.nextDouble() * 1.0; // seconds
            engine.log(String.format("%s bursts!", getShortName()));
        }
        if (boostTimer > 0) {
            targetSpeed *= 1.15;
            boostTimer -= dt;
        }

        // apply random stumble (small chance) influenced by temperament and stamina
        double stumbleChance = 0.0005 + (1.0 - getStaminaPercent()) * 0.001;
        if (rnd.nextDouble() < stumbleChance * (1.0 - staminaFactor) * (1.0/Math.max(1.0, temperament))) {
            // stumble: sudden speed drop
            currentSpeed *= 0.5;
            engine.log(String.format("%s stumbles!", getShortName()));
        }

        // friction and acceleration
        double accel = (targetSpeed - currentSpeed) * 1.8; // proportional control
        // limit accel
        double maxAccel = 3.0; // m/s^2
        if (accel > maxAccel) accel = maxAccel;
        if (accel < -maxAccel) accel = -maxAccel;

        currentSpeed += accel * dt;
        if (currentSpeed < 0) currentSpeed = 0;

        // update distance
        distanceMeters += currentSpeed * dt;

        // stamina usage increases with speed
        double staminaUse = (currentSpeed / maxSpeed) * staminaDrainRate * dt * 10.0;
        stamina -= Math.max(0, (int) Math.round(staminaUse));
        if (stamina < 0) stamina = 0;

        // if stamina very low, reduce speed capability
        if (stamina < staminaMax * 0.15) {
            currentSpeed *= 0.98; // slow down
        }

        // check finish
        if (distanceMeters >= engine.getRaceDistanceMeters()) {
            markFinished(engine.getElapsedTimeSeconds());
            engine.registerFinish(this);
        }
    }
}

/* ====================================================================== */
/* RaceEngine: handles race rules, timing, events, AI difficulty, bets */
/* ====================================================================== */

class RaceEngine {
    private List<Horse> horses;
    private double raceDistanceMeters;
    private double elapsedTimeSeconds;
    private Random rnd;
    private List<Horse> finishOrder = new ArrayList<>();
    private List<String> logs = new ArrayList<>();
    private int playerBetIndex = -1;
    private int playerBetAmount = 0;
    private int difficulty = 50; // 0..100 chaos

    public RaceEngine(List<Horse> horses, int raceDistanceMeters, Random rnd) {
        this.horses = horses;
        this.raceDistanceMeters = raceDistanceMeters;
        this.rnd = rnd;
        reset();
    }

    public void reset() {
        this.elapsedTimeSeconds = 0.0;
        finishOrder.clear();
        logs.clear();
        log("Race initialized: " + raceDistanceMeters + " meters");
    }

    public void setPlayerBet(int idx, int amt) { playerBetIndex = idx; playerBetAmount = amt; }
    public void setDifficulty(int v) { difficulty = Math.max(0, Math.min(100, v)); }
    public int getDifficulty() { return difficulty; }

    public double getRaceDistanceMeters() { return raceDistanceMeters; }
    public double getElapsedTimeSeconds() { return elapsedTimeSeconds; }

    public void update(double dt) {
        elapsedTimeSeconds += dt;
        // shuffle update order occasionally
        Collections.shuffle(horses, rnd);

        for (Horse h : horses) {
            // Increase chance of event based on difficulty
            if (!h.isFinished() && rnd.nextDouble() < (0.0001 + difficulty/20000.0)) {
                // random event: small boost or stumble
                if (rnd.nextBoolean()) {
                    h.update(dt, this);
                } else {
                    // temporary slowdown
                    // implement as a small immediate distance reduction (visual only) rarely
                    if (rnd.nextDouble() < 0.01) {
                        double penalty = 0.5 + rnd.nextDouble() * 2.0;
                        // negative distance (set back slightly but not below zero)
                        double newDist = Math.max(0.0, h.getDistanceMeters() - penalty);
                        // we have to access private field indirectly: by a hack? Instead we simulate by slowing speed
                        // no direct setter exists; we'll lower current speed temporarily
                        this.log(h.getShortName() + " loses footing for a moment");
                    }
                }
            }
            h.update(dt, this);
        }

        // reorder finished list, detect winners
        for (Horse h : horses) {
            if (h.isFinished() && !finishOrder.contains(h)) {
                finishOrder.add(h);
                log(String.format("%s finishes at %.2fs", h.getShortName(), h.getFinishTime()));
            }
        }

        // limit logs
        if (logs.size() > 200) {
            logs = logs.subList(logs.size() - 200, logs.size());
        }
    }

    public double getLeaderDistance() {
        double leader = 0.0;
        for (Horse h : horses) leader = Math.max(leader, h.getDistanceMeters());
        return leader;
    }

    public double getElapsedTime() { return elapsedTimeSeconds; }

    public boolean isRaceFinished() {
        return finishOrder.size() >= Math.max(1, horses.size());
    }

    public void registerFinish(Horse h) {
        if (!finishOrder.contains(h)) finishOrder.add(h);
    }

    public List<Horse> getFinishOrder() { return new ArrayList<>(finishOrder); }

    public void log(String s) {
        String t = String.format("[%.2fs] %s", elapsedTimeSeconds, s);
        logs.add(0, t);
        // keep only a certain number
        if (logs.size() > 200) logs.remove(logs.size()-1);
    }

    public List<String> getRecentLog(int max) {
        List<String> ret = new ArrayList<>();
        for (int i = 0; i < Math.min(max, logs.size()); i++) ret.add(logs.get(i));
        return ret;
    }
}

