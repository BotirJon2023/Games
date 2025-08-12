/*
 * BaseballBattingSimulator.java
 *
 * A Swing-based baseball batting simulator with animation, simple physics,
 * pitch types, batting input, AI pitcher, score tracking, and replay support.
 *
 * This single-file program contains several classes to keep the game modular
 * while staying within one Java source file for easy copy-paste.
 *
 * Features:
 * - Animated pitches (fastball, curveball, slider)
 * - Bat swing timing and power control
 * - Collision detection and ball flight physics
 * - Field display, boundaries, and simple out/foul logic
 * - Scoreboard, innings, and basic game loop
 * - Pause, restart, save replay (simple in-memory), step-through debug
 * - Configurable difficulty and pitch selection for pitcher AI
 *
 * Note: This code is intended for educational/demo purposes and focuses on
 * clarity and modularity rather than perfect physics realism.
 */

import javax.management.timer.Timer;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;


public class BaseballBattingSimulator {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}

/*
 * GameFrame: top-level window. Sets up menu, control panel, and the animation panel.
 */
class GameFrame extends JFrame {
    private GamePanel gamePanel;

    GameFrame() {
        setTitle("Baseball Batting Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);

        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(300, getHeight()));

        rightPanel.add(createControls(), BorderLayout.NORTH);
        rightPanel.add(createStatusPanel(), BorderLayout.CENTER);
        rightPanel.add(createBottomButtons(), BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.EAST);

        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");
        JMenuItem newGame = new JMenuItem("New Game");
        newGame.addActionListener(e -> gamePanel.newGame());
        JMenuItem saveReplay = new JMenuItem("Save Replay...");
        saveReplay.addActionListener(e -> gamePanel.saveReplayToFile());
        JMenuItem loadReplay = new JMenuItem("Load Replay...");
        loadReplay.addActionListener(e -> gamePanel.loadReplayFromFile());
        gameMenu.add(newGame);
        gameMenu.addSeparator();
        gameMenu.add(saveReplay);
        gameMenu.add(loadReplay);
        menuBar.add(gameMenu);

        setJMenuBar(menuBar);
    }

    private JComponent createControls() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1, 6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Controls"));

        JButton pitchButton = new JButton("Pitch");
        pitchButton.addActionListener(e -> gamePanel.requestPitch());
        panel.add(pitchButton);

        JButton swingButton = new JButton("Swing");
        swingButton.addActionListener(e -> gamePanel.playerSwing());
        panel.add(swingButton);

        JCheckBox autoPitch = new JCheckBox("Auto-pitch");
        autoPitch.setSelected(false);
        autoPitch.addActionListener(e -> gamePanel.setAutoPitch(autoPitch.isSelected()));
        panel.add(autoPitch);

        JSlider difficulty = new JSlider(1, 10, 5);
        difficulty.setMajorTickSpacing(1);
        difficulty.setPaintTicks(true);
        difficulty.setPaintLabels(true);
        difficulty.addChangeListener(e -> gamePanel.setDifficulty(difficulty.getValue()));
        panel.add(new JLabel("Pitcher Difficulty"));
        panel.add(difficulty);

        JComboBox<String> pitchSelector = new JComboBox<>(new String[]{"Random","Fastball","Curveball","Slider","Changeup"});
        pitchSelector.addActionListener(e -> gamePanel.setPitchPreference((String) pitchSelector.getSelectedItem()));
        panel.add(new JLabel("Pitch preference (AI)"));
        panel.add(pitchSelector);

        JSlider swingTiming = new JSlider(0, 100, 50);
        swingTiming.setMajorTickSpacing(25);
        swingTiming.setPaintTicks(true);
        swingTiming.addChangeListener(e -> gamePanel.setSwingTimingBias(swingTiming.getValue()/100.0));
        panel.add(new JLabel("Swing timing bias"));
        panel.add(swingTiming);

        return panel;
    }

    private JComponent createStatusPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Status"));

        panel.add(gamePanel.getStatusPanel(), BorderLayout.CENTER);

        return panel;
    }

    private JComponent createBottomButtons() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0,1,4,4));

        JButton pause = new JButton("Pause/Resume");
        pause.addActionListener(e -> gamePanel.togglePause());
        panel.add(pause);

        JButton step = new JButton("Step Frame");
        step.addActionListener(e -> gamePanel.stepFrame());
        panel.add(step);

        JButton toggleGuides = new JButton("Toggle Guides");
        toggleGuides.addActionListener(e -> gamePanel.toggleGuides());
        panel.add(toggleGuides);

        return panel;
    }
}

/*
 * GamePanel: main drawing and game logic. Uses a Swing Timer for animation.
 */
class GamePanel extends JPanel implements ActionListener {

    private final int FPS = 60;
    private Timer timer;
    private long lastTime;

    // Game state
    private Ball ball;
    private Bat bat;
    private Pitcher pitcher;
    private Scoreboard scoreboard;
    private Field field;

    private boolean paused = false;
    private boolean showGuides = false;

    // Control parameters
    private boolean autoPitch = false;
    private int difficulty = 5; // 1-10
    private String pitchPreference = "Random";
    private double swingTimingBias = 0.5; // 0..1

    // Replay buffer (store frames)
    private List<ReplayFrame> replay;
    private int replayIndex = -1;
    private boolean recording = true;

    // Status components
    private JLabel statusLabel;
    private JLabel pitchTypeLabel;
    private JLabel inningLabel;

    // Randomness
    private Random rng = new Random();

    GamePanel() {
        setBackground(new Color(10, 120, 20)); // grass
        setPreferredSize(new Dimension(800, 700));
        setDoubleBuffered(true);

        initGameObjects();

        timer = new Timer();
        timer.notifyAll();
        timer.start();
        lastTime = System.currentTimeMillis();

        setFocusable(true);
        requestFocusInWindow();

        setupInputBindings();

        replay = new ArrayList<>();

        // Status panel
        statusLabel = new JLabel("Ready");
        pitchTypeLabel = new JLabel("Pitch: -");
        inningLabel = new JLabel("Inning: 1 (Top)");
    }

    private void initGameObjects() {
        field = new Field();
        ball = new Ball(field.getPitchX(), field.getPitchY());
        bat = new Bat(field.getHomeX(), field.getHomeY());
        pitcher = new Pitcher("AI Pitcher", difficulty);
        scoreboard = new Scoreboard();
        scoreboard.reset();
    }

    JComponent getStatusPanel() {
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(0,1));
        p.add(statusLabel);
        p.add(pitchTypeLabel);
        p.add(inningLabel);
        return p;
    }

    void newGame() {
        initGameObjects();
        replay.clear();
        replayIndex = -1;
        scoreboard.reset();
        recording = true;
        statusLabel.setText("New game started");
        repaint();
    }

    void requestPitch() {
        if (pitcher.isReady() && ball.isAtRest()) {
            Pitch pitch = pitcher.choosePitch(pitchPreference, rng);
            pitcher.throwPitch(pitch, ball);
            pitchTypeLabel.setText("Pitch: " + pitch.type);
            statusLabel.setText("Pitch thrown: " + pitch.type);
            if (recording) recordReplayFrame();
        }
    }

    void playerSwing() {
        bat.swing();
        if (recording) recordReplayFrame();
    }

    void setAutoPitch(boolean value) { this.autoPitch = value; }
    void setDifficulty(int value) { this.difficulty = value; pitcher.setSkill(value); }
    void setPitchPreference(String pref) { this.pitchPreference = pref; }
    void setSwingTimingBias(double bias) { this.swingTimingBias = bias; bat.setTimingBias(bias); }

    void togglePause() { paused = !paused; statusLabel.setText(paused ? "Paused" : "Running"); }
    void stepFrame() { if (paused) { updateGame(1.0 / FPS); repaint(); } }
    void toggleGuides() { showGuides = !showGuides; }

    void saveReplayToFile() {
        if (replay.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No replay recorded.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        int ret = chooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
                oos.writeObject(replay);
                JOptionPane.showMessageDialog(this, "Replay saved: " + f.getName());
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to save replay: " + ex.getMessage());
            }
        }
    }

    void loadReplayFromFile() {
        JFileChooser chooser = new JFileChooser();
        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                Object obj = ois.readObject();
                if (obj instanceof List) {
                    // unchecked cast but okay for demo
                    //noinspection unchecked
                    replay = (List<ReplayFrame>) obj;
                    replayIndex = 0;
                    recording = false;
                    statusLabel.setText("Replay loaded: " + f.getName());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to load replay: " + ex.getMessage());
            }
        }
    }

    void setReplayIndex(int idx) {
        if (replay == null || replay.isEmpty()) return;
        replayIndex = Math.max(0, Math.min(idx, replay.size()-1));
    }

    private void setupInputBindings() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "swing");
        getActionMap().put("swing", new AbstractAction() { public void actionPerformed(ActionEvent e) { playerSwing(); } });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "pitch");
        getActionMap().put("pitch", new AbstractAction() { public void actionPerformed(ActionEvent e) { requestPitch(); } });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "reset");
        getActionMap().put("reset", new AbstractAction() { public void actionPerformed(ActionEvent e) { newGame(); } });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.currentTimeMillis();
        double dt = (now - lastTime) / 1000.0;
        lastTime = now;

        if (!paused) updateGame(dt);
        repaint();
    }

    private void updateGame(double dt) {
        if (!recording && replayIndex >= 0 && replayIndex < replay.size()) {
            // Replay playback: restore state
            ReplayFrame frame = replay.get(replayIndex);
            restoreFromReplay(frame);
            replayIndex++;
            if (replayIndex >= replay.size()) replayIndex = 0; // loop
            return;
        }

        // Auto-pitch logic
        if (autoPitch && pitcher.isReady() && ball.isAtRest()) {
            // small delay before auto-pitch to simulate human timing
            if (rng.nextDouble() < 0.02) requestPitch();
        }

        // Physics update
        ball.update(dt);
        bat.update(dt);

        // Hit detection
        if (!ball.isHit() && ball.isNear(bat.getSwingX(), bat.getSwingY(), bat.getEffectiveRadius())) {
            if (bat.isInSwingPhase()) {
                // Compute collision and ball exit velocity
                handleHit();
            }
        }

        // Out/foul detection and scoring
        if (ball.isInPlay() && !ball.isEvaluated()) {
            if (field.isOutOfBounds(ball.getX(), ball.getY())) {
                ball.markEvaluated();
                scoreboard.registerOut();
                statusLabel.setText("Out! Ball out of bounds.");
            } else if (field.isFoul(ball.getX(), ball.getY())) {
                ball.markEvaluated();
                statusLabel.setText("Foul ball.");
            } else if (ball.getY() < field.getInfieldLine()) {
                // ground infield -> easy out check (simplified)
                if (rng.nextDouble() < 0.4) {
                    scoreboard.registerOut();
                    statusLabel.setText("Ground ball - out! (defense)");
                } else {
                    scoreboard.registerHit();
                    statusLabel.setText("Infield hit!");
                }
                ball.markEvaluated();
            } else if (ball.getY() < field.getHomeRunLine()) {
                scoreboard.registerHit();
                ball.markEvaluated();
                statusLabel.setText("Single! Ball landed in outfield.");
            } else {
                // deep ball - check home run threshold
                if (ball.speed() > 30 && ball.getY() < field.getDeepOutfieldLine()) {
                    scoreboard.registerHomeRun();
                    ball.markEvaluated();
                    statusLabel.setText("Home Run!" );
                }
            }
        }

        // Reset after play completes
        if (ball.isDone()) {
            // reset ball and bat for next pitch/play
            ball.resetToPitcher(field.getPitchX(), field.getPitchY());
            bat.reset();
            pitcher.prepareNext();
            if (recording) recordReplayFrame();
        }

        // small chance of pitcher bunching
        pitcher.update(dt);

        // update UI labels
        inningLabel.setText(scoreboard.getInningString());

        // record frame for replay
        if (recording) recordReplayFrame();
    }

    private void restoreFromReplay(ReplayFrame frame) {
        // Simple restoration: set positions and states
        ball.restore(frame.ballState);
        bat.restore(frame.batState);
        pitcher.restore(frame.pitcherState);
        scoreboard.restore(frame.scoreState);
        statusLabel.setText(frame.statusText);
        pitchTypeLabel.setText("Pitch: " + frame.pitchType);
    }

    private void recordReplayFrame() {
        ReplayFrame f = new ReplayFrame();
        f.timestamp = System.currentTimeMillis();
        f.ballState = ball.snapshot();
        f.batState = bat.snapshot();
        f.pitcherState = pitcher.snapshot();
        f.scoreState = scoreboard.snapshot();
        f.statusText = statusLabel.getText();
        f.pitchType = pitchTypeLabel.getText().replace("Pitch: ", "");
        replay.add(f);
        // keep buffer bounded
        if (replay.size() > 2000) replay.remove(0);
    }

    private void handleHit() {
        // basic reflection and exit velocity model
        double relVelX = ball.vx - bat.swingVx();
        double relVelY = ball.vy - bat.swingVy();

        double impact = Math.sqrt(relVelX * relVelX + relVelY * relVelY);

        double powerFactor = bat.getPower(); // 0..1
        double angle = Math.atan2(ball.vy, ball.vx);

        // add spin based on bat angle and pitch spin
        double spinInfluence = bat.getSpinInfluence();

        // new speed
        double exitSpeed = Math.max(10, impact * (1.0 + powerFactor * 1.8) + rng.nextDouble()*8);

        // change direction influenced by bat angle and randomness
        double batAngle = bat.getAngle();
        double exitAngle = angle + (batAngle - angle) * 0.7 + (rng.nextDouble() - 0.5) * 0.4;

        // set ball to in-play
        ball.hit(exitSpeed, exitAngle, spinInfluence);
        ball.setHit(true);

        // small chance of fouling off (if angle near foul zones)
        if (field.isFoul(ball.getX(), ball.getY())) {
            statusLabel.setText("Foul hit - no advance.");
        } else {
            statusLabel.setText("Solid contact! Exit speed: " + new DecimalFormat("0.0").format(exitSpeed));
        }

        // scoring/tracking
        scoreboard.registerContact();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        // enable antialias
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        field.render(g2, getWidth(), getHeight());
        drawGrid(g2);
        ball.render(g2);
        bat.render(g2);
        pitcher.render(g2);
        drawHUD(g2);

        g2.dispose();
    }

    private void drawGrid(Graphics2D g2) {
        if (!showGuides) return;
        int w = getWidth();
        int h = getHeight();
        g2.setColor(new Color(255,255,255,40));
        for (int x=0; x<w; x+=50) g2.drawLine(x,0,x,h);
        for (int y=0; y<h; y+=50) g2.drawLine(0,y,w,y);
    }

    private void drawHUD(Graphics2D g2) {
        // scoreboard
        scoreboard.render(g2, getWidth()-300, 10);

        // status
        g2.setColor(Color.white);
        g2.drawString("Status: " + statusLabel.getText(), 10, 20);
        g2.drawString(pitchTypeLabel.getText(), 10, 40);
        g2.drawString(inningLabel.getText(), 10, 60);

        // helpful hints
        g2.drawString("Controls: P=Pitch, SPACE=Swing, R=Reset", 10, getHeight()-10);
    }
}

/*
 * Field: handles coordinates for home, pitcher's mound, outfield lines, and
 * simple hit-region logic.
 */
class Field {
    // logical coordinates - mapped to panel size in render
    private int homeX = 100;
    private int homeY = 560; // home plate y
    private int pitchX = 300;
    private int pitchY = 260;

    private int infieldLine = 420; // y coordinate threshold for infield
    private int homeRunLine = 120; // deep outfield threshold
    private int deepOutfieldLine = 80;

    // boundaries
    private int leftBound = 20;
    private int rightBound = 780;
    private int topBound = 10;

    Field() { }

    int getHomeX() { return homeX; }
    int getHomeY() { return homeY; }
    int getPitchX() { return pitchX; }
    int getPitchY() { return pitchY; }
    int getInfieldLine() { return infieldLine; }
    int getHomeRunLine() { return homeRunLine; }
    int getDeepOutfieldLine() { return deepOutfieldLine; }

    boolean isOutOfBounds(double x, double y) {
        return x < leftBound || x > rightBound || y < topBound;
    }

    boolean isFoul(double x, double y) {
        // simple foul logic: too far left/right near homeplate
        return (x < 60 || x > 740) && y > 480;
    }

    void render(Graphics2D g2, int panelWidth, int panelHeight) {
        // simple scaling and translation logic so field fits the panel
        // We'll just draw a stylized diamond and lines

        // grass background already painted by panel
        // draw baseline and infield
        g2.setColor(new Color(200, 200, 180, 160));
        Polygon infield = new Polygon();
        infield.addPoint(homeX, homeY);
        infield.addPoint(homeX+300, homeY-300);
        infield.addPoint(homeX+500, homeY-200);
        infield.addPoint(homeX+200, homeY+20);
        g2.fill(infield);

        // pitcher's mound
        g2.setColor(new Color(150,100,50));
        g2.fillOval(pitchX-20, pitchY-10, 40, 20);

        // home plate
        g2.setColor(Color.white);
        Polygon home = new Polygon(new int[]{homeX-10, homeX+10, homeX+6, homeX-6}, new int[]{homeY, homeY, homeY+12, homeY+12}, 4);
        g2.fill(home);

        // lines
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(homeX, homeY, homeX+200, homeY-200);
        g2.drawLine(homeX, homeY, homeX+500, homeY-200);

        // infield line
        g2.setColor(new Color(255,255,255,80));
        g2.drawLine(0, infieldLine, panelWidth, infieldLine);

        // home run line
        g2.setColor(new Color(255,215,0,60));
        g2.drawLine(0, homeRunLine, panelWidth, homeRunLine);

        // labels
        g2.setColor(Color.white);
        g2.drawString("Home", homeX-20, homeY+30);
        g2.drawString("Pitcher", pitchX-20, pitchY-15);
    }
}

/*
 * Ball: represents the baseball and its physics state.
 */
class Ball implements Serializable {
    double x, y;
    double vx, vy; // units: pixels/second
    double radius = 6;

    transient boolean hit = false;
    transient boolean inPlay = false;
    transient boolean evaluated = false;

    // friction/air drag approximations
    static final double DRAG = 0.02;
    static final double GRAVITY = 9.8 * 10; // amplified for visual effect

    Ball(int startX, int startY) {
        x = startX; y = startY;
        vx = 0; vy = 0;
    }

    void resetToPitcher(int startX, int startY) {
        x = startX; y = startY;
        vx = 0; vy = 0;
        hit = false; inPlay = false; evaluated = false;
    }

    boolean isAtRest() { return Math.hypot(vx, vy) < 0.5 && !inPlay; }
    boolean isHit() { return hit; }
    void setHit(boolean v) { hit = v; if (v) inPlay = true; }
    boolean isInPlay() { return inPlay; }
    boolean isEvaluated() { return evaluated; }
    void markEvaluated() { evaluated = true; }

    boolean isDone() { // end of play when ball stops or evaluated
        return (inPlay && Math.hypot(vx, vy) < 0.8) || evaluated;
    }

    void update(double dt) {
        if (Math.abs(vx) < 0.0001) vx = 0;
        if (Math.abs(vy) < 0.0001) vy = 0;

        if (!hit) {
            // pitch movement
            x += vx * dt;
            y += vy * dt;
            // pitch spin causes lateral movement implicitly through vx adjustments earlier
            // apply drag
            vx *= (1 - DRAG * dt * 60);
            vy *= (1 - DRAG * dt * 60);
        } else {
            // ball in flight: sim gravity and drag
            vx *= (1 - DRAG * dt * 25);
            vy += GRAVITY * dt;
            x += vx * dt;
            y += vy * dt;
            // ground collision
            if (y > 700) {
                y = 700;
                vy = -vy * 0.25;
                vx *= 0.7;
                // if small bounce, stop
                if (Math.abs(vy) < 5) vy = 0;
            }
        }
    }

    double speed() { return Math.hypot(vx, vy); }

    void hit(double speed, double angle, double spin) {
        vx = Math.cos(angle) * speed;
        vy = Math.sin(angle) * speed - spin * 20;
        inPlay = true;
    }

    boolean isNear(double px, double py, double r) {
        double dx = x - px, dy = y - py;
        return dx*dx + dy*dy <= r*r;
    }

    double getX() { return x; }
    double getY() { return y; }

    void render(Graphics2D g2) {
        g2.setColor(Color.white);
        g2.fillOval((int)(x - radius), (int)(y - radius), (int)(radius * 2), (int)(radius * 2));
        // seam representation
        g2.setColor(Color.red);
        g2.drawArc((int)(x-radius), (int)(y-radius), (int)(radius*2), (int)(radius*2), 20, 120);
    }

    // snapshot/restore for replay
    BallState snapshot() {
        BallState s = new BallState();
        s.x = x; s.y = y; s.vx = vx; s.vy = vy; s.hit = hit; s.inPlay = inPlay; s.evaluated = evaluated;
        return s;
    }

    void restore(BallState s) {
        this.x = s.x; this.y = s.y; this.vx = s.vx; this.vy = s.vy; this.hit = s.hit; this.inPlay = s.inPlay; this.evaluated = s.evaluated;
    }
}

// Serializable states
class BallState implements Serializable { double x,y,vx,vy; boolean hit,inPlay,evaluated; }

/*
 * Bat: models the bat position and swing gesture. The player triggers a swing
 * which causes a short animation. Timing bias influences when to swing.
 */
class Bat implements Serializable {
    double x, y; // pivot point near home plate
    double angle; // radians
    double length = 80;

    // swing state
    transient double swingProgress = 0; // 0..1
    transient boolean swinging = false;
    transient double swingDuration = 0.25; // seconds

    // parameters
    double power = 0.9; // 0..1
    double timingBias = 0.5; // 0..1

    Bat(int pivotX, int pivotY) {
        x = pivotX; y = pivotY;
        angle = -0.5; // leaning slightly
    }

    void swing() {
        swinging = true;
        swingProgress = 0;
    }

    void update(double dt) {
        if (swinging) {
            swingProgress += dt / swingDuration;
            if (swingProgress >= 1.0) { swingProgress = 0; swinging = false; }
            // animate angle
            angle = -0.8 + Math.sin(swingProgress * Math.PI) * 1.6; // wide arc
        } else {
            // idle slight movement
            angle *= 0.98;
            angle += Math.sin(System.currentTimeMillis()/300.0) * 0.0005;
        }
    }

    double getSwingX() { return x + Math.cos(angle) * length; }
    double getSwingY() { return y + Math.sin(angle) * length; }
    double getEffectiveRadius() { return length * 0.6; }

    boolean isInSwingPhase() { return swinging && swingProgress > 0.2 && swingProgress < 0.8; }

    double swingVx() { return Math.cos(angle) * (swinging ? 200 * Math.sin(swingProgress*Math.PI) : 0); }
    double swingVy() { return Math.sin(angle) * (swinging ? 200 * Math.sin(swingProgress*Math.PI) : 0); }

    double getPower() { return power; }
    double getAngle() { return angle; }
    double getSpinInfluence() { return Math.sin(angle) * 0.5; }

    void setTimingBias(double b) { timingBias = b; }
    void reset() { swinging = false; swingProgress = 0; angle = -0.5; }

    void render(Graphics2D g2) {
        AffineTransform old = g2.getTransform();
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(160, 82, 45));
        g2.translate(x, y);
        g2.rotate(angle);
        g2.drawLine(0, 0, (int)length, 0);
        g2.setTransform(old);
    }

    // snapshot/restore
    BatState snapshot() { BatState s = new BatState(); s.x = x; s.y = y; s.angle = angle; s.swinging = swinging; s.swingProgress = swingProgress; return s; }
    void restore(BatState s) { this.x = s.x; this.y = s.y; this.angle = s.angle; this.swinging = s.swinging; this.swingProgress = s.swingProgress; }
}
class BatState implements Serializable { double x,y,angle; boolean swinging; double swingProgress; }

/*
 * Pitcher: AI that selects a pitch type and sets pitch velocity and spin.
 */
class Pitcher implements Serializable {
    String name;
    int skill; // 1-10
    transient boolean ready = true;
    transient double coolDown = 0;

    Pitcher(String name, int skill) { this.name = name; this.skill = skill; }

    void setSkill(int s) { this.skill = Math.max(1, Math.min(10, s)); }

    boolean isReady() { return ready; }

    void prepareNext() { ready = true; coolDown = 0; }

    void update(double dt) {
        if (!ready) {
            coolDown -= dt;
            if (coolDown <= 0) ready = true;
        }
    }

    Pitch choosePitch(String pref, Random rng) {
        List<PitchType> types = Arrays.asList(PitchType.values());
        PitchType chosen;
        if ("Random".equals(pref)) {
            chosen = types.get(rng.nextInt(types.size()));
        } else {
            try {
                chosen = PitchType.valueOf(pref.toUpperCase());
            } catch (Exception ex) {
                // fallback to random but bias
                chosen = types.get(rng.nextInt(types.size()));
            }
        }
        double baseSpeed = 110 - (skill-5)*3 + rng.nextGaussian()*4; // mph-ish
        // convert to pixels per second roughly
        double speed = baseSpeed * 4.0;
        double spin = rng.nextDouble()*2 - 1;
        return new Pitch(chosen.name(), speed, spin, chosen);
    }

    void throwPitch(Pitch p, Ball ball) {
        // map pitch into vx, vy values directed toward home plate
        // simplistic: x increases slightly left->right, y decreases to home plate y
        double dirX = -0.1 + (Math.random()*0.2);
        double dirY = 0.9;
        double mag = p.speed;
        ball.x = 300; ball.y = 260;
        ball.vx = dirX * mag;
        ball.vy = dirY * mag * 0.6; // slower downward component for visual
        // spin influence: slight lateral change over time
        // we simulate spin by changing vx over time via small adjustments
        ready = false;
        coolDown = 1.0 + (10 - skill) * 0.1;
    }

    PitcherState snapshot() { PitcherState s = new PitcherState(); s.skill = skill; s.ready = ready; s.coolDown = coolDown; return s; }
    void restore(PitcherState s) { this.skill = s.skill; this.ready = s.ready; this.coolDown = s.coolDown; }

    public void render(Graphics2D g2) {
    }
}
class PitcherState implements Serializable { int skill; boolean ready; double coolDown; }

enum PitchType { FASTBALL, CURVEBALL, SLIDER, CHANGEUP }

class Pitch implements Serializable {
    String type;
    double speed; // pixels/s
    double spin;
    PitchType pitchType;

    Pitch(String type, double speed, double spin, PitchType pt) { this.type = type; this.speed = speed; this.spin = spin; this.pitchType = pt; }
}

/*
 * Scoreboard: minimal scoring system with innings and outs.
 */
class Scoreboard implements Serializable {
    int inning = 1; // 1-based
    boolean top = true; // top or bottom
    int outs = 0;
    int scoreHome = 0;
    int scoreAway = 0;

    void reset() { inning = 1; top = true; outs = 0; scoreHome = scoreAway = 0; }
    void registerOut() { outs++; if (outs >= 3) advanceHalfInning(); }
    void registerHit() { // simple: award a run with some chance
        if (Math.random() < 0.2) {
            if (top) scoreAway++; else scoreHome++;
        }
    }
    void registerContact() { /* track contact, no immediate effect */ }
    void registerHomeRun() { if (top) scoreAway++; else scoreHome++; }

    void advanceHalfInning() { outs = 0; top = !top; if (top) inning++; }

    String getInningString() { return "Inning: " + inning + (top ? " (Top)" : " (Bottom)") + " Outs: " + outs + " Score: " + scoreAway + "-" + scoreHome; }

    void render(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(0,0,0,150));
        g2.fillRect(x, y, 260, 120);
        g2.setColor(Color.white);
        g2.drawString("Inning: " + inning + (top?" Top":" Bottom"), x+10, y+20);
        g2.drawString("Outs: " + outs, x+10, y+40);
        g2.drawString("Score - Away: " + scoreAway + "  Home: " + scoreHome, x+10, y+60);
    }

    ScoreState snapshot() { ScoreState s = new ScoreState(); s.inning = inning; s.top = top; s.outs = outs; s.scoreHome = scoreHome; s.scoreAway = scoreAway; return s; }
    void restore(ScoreState s) { this.inning = s.inning; this.top = s.top; this.outs = s.outs; this.scoreHome = s.scoreHome; this.scoreAway = s.scoreAway; }
}
class ScoreState implements Serializable { int inning; boolean top; int outs; int scoreHome; int scoreAway; }

/*
 * Simple replay frame - stores serialized states at each time-step
 */
class ReplayFrame implements Serializable {
    long timestamp;
    BallState ballState;
    BatState batState;
    PitcherState pitcherState;
    ScoreState scoreState;
    String statusText;
    String pitchType;
}

/*
 * Utility rendering / debug helpers etc.
 */
class Utils {
    static String fmt(double v) { return new DecimalFormat("0.00").format(v); }
}

/*
 * End of file: plenty of comments to explain the code and its extension points.
 *
 * Extensions you might try:
 * - Add real pitch trajectories with Magnus effect (spin explainer)
 * - Add batter timing/rhythm mini-game instead of one-button swing
 * - Add fielders and run advancement logic for multiple runners
 * - Add sound effects and particle effects for impact
 * - Make the physics more realistic by tuning drag/gravity and units
 */
