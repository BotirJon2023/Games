import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class OlympicSportSimulator extends JPanel {

    // ====== Window / Game constants ======
    public static final int WIDTH = 960;
    public static final int HEIGHT = 540;
    private static final int TARGET_FPS = 60;
    private static final int FRAME_DELAY = 1000 / TARGET_FPS;

    // ====== Game states ======
    private enum GameState {
        MAIN_MENU,
        SPORT_SELECT,
        RUNNING_SPRINT,
        RUNNING_LONG_JUMP,
        RUNNING_JAVELIN,
        RESULTS
    }

    private GameState gameState = GameState.MAIN_MENU;

    // ====== Sports enum ======
    private enum SportType {
        SPRINT_100M,
        LONG_JUMP,
        JAVELIN_THROW
    }

    // currently active sport
    private SportType currentSport = SportType.SPRINT_100M;

    // ====== Core timing / update ======
    private Timer gameTimer;
    private long lastUpdateNanos;

    // ====== Input flags ======
    private boolean keyLeft;
    private boolean keyRight;
    private boolean keyUp;
    private boolean keyDown;
    private boolean keySpace;
    private boolean keyEnter;

    // ====== Shared player model ======
    private Athlete playerAthlete;

    // ====== Sport simulations ======
    private Sprint100m sprint;
    private LongJump longJump;
    private JavelinThrow javelinThrow;

    // ====== HUD / Results ======
    private String resultsText = "";
    private double lastScore = 0.0;

    // ====== Menus ======
    private int mainMenuSelection = 0;       // 0 = Start, 1 = Quit
    private int sportMenuSelection = 0;      // 0..2

    // ====== Constructor ======
    public OlympicSportSimulator() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.DARK_GRAY);
        setDoubleBuffered(true);

        // Create a default athlete
        playerAthlete = new Athlete("Player 1", "Fictionland", 80, 75, 70);

        // Create sport simulations
        sprint = new Sprint100m(playerAthlete);
        longJump = new LongJump(playerAthlete);
        javelinThrow = new JavelinThrow(playerAthlete);

        setupInput();
        setupTimer();
    }

    // ====== Input setup using key bindings ======
    private void setupInput() {
        int condition = WHEN_IN_FOCUSED_WINDOW;
        InputMap inputMap = getInputMap(condition);
        ActionMap actionMap = getActionMap();

        // Helper to create press/release actions
        bindKey(inputMap, actionMap, "LEFT", KeyStroke.getKeyStroke("LEFT"),
                () -> keyLeft = true, () -> keyLeft = false);
        bindKey(inputMap, actionMap, "RIGHT", KeyStroke.getKeyStroke("RIGHT"),
                () -> keyRight = true, () -> keyRight = false);
        bindKey(inputMap, actionMap, "UP", KeyStroke.getKeyStroke("UP"),
                () -> keyUp = true, () -> keyUp = false);
        bindKey(inputMap, actionMap, "DOWN", KeyStroke.getKeyStroke("DOWN"),
                () -> keyDown = true, () -> keyDown = false);
        bindKey(inputMap, actionMap, "SPACE", KeyStroke.getKeyStroke("SPACE"),
                () -> keySpace = true, () -> keySpace = false);
        bindKey(inputMap, actionMap, "ENTER", KeyStroke.getKeyStroke("ENTER"),
                () -> keyEnter = true, () -> keyEnter = false);
        bindKey(inputMap, actionMap, "A", KeyStroke.getKeyStroke("A"),
                () -> keyLeft = true, () -> keyLeft = false);
        bindKey(inputMap, actionMap, "D", KeyStroke.getKeyStroke("D"),
                () -> keyRight = true, () -> keyRight = false);
        bindKey(inputMap, actionMap, "W", KeyStroke.getKeyStroke("W"),
                () -> keyUp = true, () -> keyUp = false);
        bindKey(inputMap, actionMap, "S", KeyStroke.getKeyStroke("S"),
                () -> keyDown = true, () -> keyDown = false);
    }

    private void bindKey(InputMap im, ActionMap am,
                         String name, KeyStroke keyStroke,
                         Runnable onPress, Runnable onRelease) {
        im.put(keyStroke, name + "_PRESSED");
        im.put(KeyStroke.getKeyStroke(keyStroke.getKeyCode(), 0, true),
                name + "_RELEASED");

        am.put(name + "_PRESSED", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onPress.run();
            }
        });

        am.put(name + "_RELEASED", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRelease.run();
            }
        });
    }

    // ====== Timer / game loop ======
    private void setupTimer() {
        lastUpdateNanos = System.nanoTime();
        gameTimer = new Timer(FRAME_DELAY, e -> updateAndRepaint());
        gameTimer.start();
    }

    private void updateAndRepaint() {
        long now = System.nanoTime();
        double deltaSeconds = (now - lastUpdateNanos) / 1_000_000_000.0;
        lastUpdateNanos = now;

        updateGame(deltaSeconds);
        repaint();
    }

    // ====== Update per frame ======
    private void updateGame(double dt) {
        switch (gameState) {
            case MAIN_MENU:
                updateMainMenu(dt);
                break;
            case SPORT_SELECT:
                updateSportMenu(dt);
                break;
            case RUNNING_SPRINT:
                sprint.update(dt, keyLeft, keyRight, keyUp, keyDown, keySpace);
                if (sprint.isFinished()) {
                    lastScore = sprint.getTimeSeconds();
                    resultsText = "100m Sprint time: " +
                            String.format("%.2f s", lastScore);
                    gameState = GameState.RESULTS;
                    resetInput();
                }
                break;
            case RUNNING_LONG_JUMP:
                longJump.update(dt, keyLeft, keyRight, keyUp, keyDown, keySpace);
                if (longJump.isFinished()) {
                    lastScore = longJump.getDistanceMeters();
                    resultsText = "Long Jump distance: " +
                            String.format("%.2f m", lastScore);
                    gameState = GameState.RESULTS;
                    resetInput();
                }
                break;
            case RUNNING_JAVELIN:
                javelinThrow.update(dt, keyLeft, keyRight, keyUp, keyDown, keySpace);
                if (javelinThrow.isFinished()) {
                    lastScore = javelinThrow.getDistanceMeters();
                    resultsText = "Javelin Throw distance: " +
                            String.format("%.2f m", lastScore);
                    gameState = GameState.RESULTS;
                    resetInput();
                }
                break;
            case RESULTS:
                updateResults(dt);
                break;
        }
    }

    private void resetInput() {
        keyLeft = keyRight = keyUp = keyDown = keySpace = keyEnter = false;
    }

    // ====== Menu updates ======
    private void updateMainMenu(double dt) {
        // Simple navigation with up/down, confirm with Enter
        if (keyUp) {
            mainMenuSelection = (mainMenuSelection + 1) % 2;
            keyUp = false;
        }
        if (keyDown) {
            mainMenuSelection = (mainMenuSelection + 1) % 2;
            keyDown = false;
        }
        if (keyEnter) {
            keyEnter = false;
            if (mainMenuSelection == 0) {
                gameState = GameState.SPORT_SELECT;
            } else {
                System.exit(0);
            }
        }
    }

    private void updateSportMenu(double dt) {
        if (keyUp) {
            sportMenuSelection--;
            if (sportMenuSelection < 0) sportMenuSelection = 2;
            keyUp = false;
        }
        if (keyDown) {
            sportMenuSelection++;
            if (sportMenuSelection > 2) sportMenuSelection = 0;
            keyDown = false;
        }
        if (keyEnter) {
            keyEnter = false;
            if (sportMenuSelection == 0) {
                currentSport = SportType.SPRINT_100M;
                sprint.reset();
                gameState = GameState.RUNNING_SPRINT;
            } else if (sportMenuSelection == 1) {
                currentSport = SportType.LONG_JUMP;
                longJump.reset();
                gameState = GameState.RUNNING_LONG_JUMP;
            } else if (sportMenuSelection == 2) {
                currentSport = SportType.JAVELIN_THROW;
                javelinThrow.reset();
                gameState = GameState.RUNNING_JAVELIN;
            }
        }
    }

    private void updateResults(double dt) {
        // Press Enter to go back to sport select
        if (keyEnter) {
            keyEnter = false;
            gameState = GameState.SPORT_SELECT;
        }
    }

    // ====== Rendering ======
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Use Graphics2D for better control
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        switch (gameState) {
            case MAIN_MENU:
                renderMainMenu(g2);
                break;
            case SPORT_SELECT:
                renderSportMenu(g2);
                break;
            case RUNNING_SPRINT:
                sprint.render(g2);
                renderHUD(g2);
                break;
            case RUNNING_LONG_JUMP:
                longJump.render(g2);
                renderHUD(g2);
                break;
            case RUNNING_JAVELIN:
                javelinThrow.render(g2);
                renderHUD(g2);
                break;
            case RESULTS:
                renderResults(g2);
                break;
        }

        g2.dispose();
    }

    private void renderMainMenu(Graphics2D g) {
        g.setColor(new Color(10, 10, 40));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        drawCenteredString(g, "Olympic Sport Simulator",
                new Rectangle(0, 40, WIDTH, 80),
                new Font("SansSerif", Font.BOLD, 42), Color.WHITE);

        String[] options = { "Start", "Quit" };
        for (int i = 0; i < options.length; i++) {
            boolean selected = (i == mainMenuSelection);
            Color c = selected ? Color.YELLOW : Color.LIGHT_GRAY;
            Font f = new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 28);
            drawCenteredString(g, options[i],
                    new Rectangle(0, 150 + i * 60, WIDTH, 60),
                    f, c);
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(Color.WHITE);
        g.drawString("Use UP/DOWN and ENTER to navigate.", 20, HEIGHT - 40);
    }

    private void renderSportMenu(Graphics2D g) {
        g.setColor(new Color(0, 60, 0));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        drawCenteredString(g, "Select a Sport",
                new Rectangle(0, 40, WIDTH, 80),
                new Font("SansSerif", Font.BOLD, 38), Color.WHITE);

        String[] sports = {
                "100m Sprint",
                "Long Jump",
                "Javelin Throw"
        };

        for (int i = 0; i < sports.length; i++) {
            boolean selected = (i == sportMenuSelection);
            Color c = selected ? Color.YELLOW : Color.WHITE;
            Font f = new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 28);
            drawCenteredString(g, sports[i],
                    new Rectangle(0, 140 + i * 60, WIDTH, 60),
                    f, c);
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(Color.WHITE);
        g.drawString("Use UP/DOWN and ENTER. Press ESC to close window.", 20, HEIGHT - 40);
    }

    private void renderHUD(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, WIDTH, 60);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));

        String name = playerAthlete.getName();
        String country = playerAthlete.getCountry();

        g.drawString("Athlete: " + name + " (" + country + ")", 20, 30);

        String sportName = "";
        if (gameState == GameState.RUNNING_SPRINT) sportName = "100m Sprint";
        if (gameState == GameState.RUNNING_LONG_JUMP) sportName = "Long Jump";
        if (gameState == GameState.RUNNING_JAVELIN) sportName = "Javelin Throw";

        g.drawString("Sport: " + sportName, 350, 30);

        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        if (gameState == GameState.RUNNING_SPRINT) {
            g.drawString("Controls: Rapidly tap LEFT/RIGHT to run, SPACE to lean at finish line.", 20, 50);
        } else if (gameState == GameState.RUNNING_LONG_JUMP) {
            g.drawString("Controls: RIGHT to accelerate, SPACE to jump. Try to take off near the board.", 20, 50);
        } else if (gameState == GameState.RUNNING_JAVELIN) {
            g.drawString("Controls: RIGHT to run-up, UP/DOWN to adjust angle, SPACE to throw.", 20, 50);
        }
    }

    private void renderResults(Graphics2D g) {
        g.setColor(new Color(20, 10, 40));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        drawCenteredString(g, "Results",
                new Rectangle(0, 40, WIDTH, 80),
                new Font("SansSerif", Font.BOLD, 40), Color.WHITE);

        drawCenteredString(g, resultsText,
                new Rectangle(0, 150, WIDTH, 80),
                new Font("SansSerif", Font.PLAIN, 28), Color.YELLOW);

        // Very simple rating
        String rating;
        switch (currentSport) {
            case SPRINT_100M:
                if (lastScore < 10.0) rating = "Olympic Record Level!";
                else if (lastScore < 11.0) rating = "World Class!";
                else if (lastScore < 13.0) rating = "Amateur Competitor.";
                else rating = "Needs Training.";
                break;
            case LONG_JUMP:
                if (lastScore > 8.5) rating = "World Record Level!";
                else if (lastScore > 7.5) rating = "Elite Jumper.";
                else if (lastScore > 6.0) rating = "Club Athlete.";
                else rating = "Beginner.";
                break;
            case JAVELIN_THROW:
                if (lastScore > 95.0) rating = "World Record Level!";
                else if (lastScore > 80.0) rating = "Elite Thrower.";
                else if (lastScore > 60.0) rating = "Good Throw.";
                else rating = "Keep Practicing.";
                break;
            default:
                rating = "";
        }

        drawCenteredString(g, rating,
                new Rectangle(0, 230, WIDTH, 80),
                new Font("SansSerif", Font.ITALIC, 24), Color.WHITE);

        drawCenteredString(g,
                "Press ENTER to return to sport selection.",
                new Rectangle(0, HEIGHT - 140, WIDTH, 60),
                new Font("SansSerif", Font.PLAIN, 18),
                Color.LIGHT_GRAY);
    }

    // ====== Utility: centered text ======
    private void drawCenteredString(Graphics2D g, String text, Rectangle rect,
                                    Font font, Color color) {
        FontMetrics metrics = g.getFontMetrics(font);
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        int y = rect.y + ((rect.height - metrics.getHeight()) / 2)
                + metrics.getAscent();

        g.setFont(font);
        g.setColor(color);
        g.drawString(text, x, y);
    }

    // ====== Launch method ======
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Olympic Sport Simulator");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            OlympicSportSimulator panel = new OlympicSportSimulator();
            window.setContentPane(panel);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setResizable(false);
            window.setVisible(true);
        });
    }

    // =====================================================================
    // Athlete model
    // =====================================================================
    private static class Athlete {
        private String name;
        private String country;
        private int speed;      // 0-100
        private int power;      // 0-100
        private int technique;  // 0-100

        public Athlete(String name, String country, int speed,
                       int power, int technique) {
            this.name = name;
            this.country = country;
            this.speed = speed;
            this.power = power;
            this.technique = technique;
        }

        public String getName() {
            return name;
        }

        public String getCountry() {
            return country;
        }

        public int getSpeed() {
            return speed;
        }

        public int getPower() {
            return power;
        }

        public int getTechnique() {
            return technique;
        }
    }

    // =====================================================================
    // 100m Sprint Simulation
    // =====================================================================
    private static class Sprint100m {
        private Athlete athlete;

        // Track dimensions in abstract meters
        private static final double TRACK_LENGTH_M = 100.0;

        // Runner simulation state
        private double runnerX;          // meters
        private double runnerVel;        // m/s
        private double runnerAcc;        // m/s^2
        private double friction = 1.1;

        private double raceTime;         // seconds
        private boolean finished;
        private boolean leanedAtFinish;  // if space pressed near line

        // Visual
        private double pixelPerMeter;

        public Sprint100m(Athlete athlete) {
            this.athlete = athlete;
            pixelPerMeter = (WIDTH - 100) / TRACK_LENGTH_M;
            reset();
        }

        public void reset() {
            runnerX = 0;
            runnerVel = 0;
            runnerAcc = 0;
            raceTime = 0;
            finished = false;
            leanedAtFinish = false;
        }

        public void update(double dt,
                           boolean left, boolean right, boolean up,
                           boolean down, boolean space) {
            if (finished) return;

            raceTime += dt;

            // Simulate "button mashing" to gain speed:
            // Each frame where left or right is pressed adds acceleration.
            double baseAcc = 5.0 + athlete.getSpeed() * 0.05;
            if (left || right) {
                runnerAcc = baseAcc;
            } else {
                runnerAcc = 0;
            }

            // Apply friction and acceleration
            runnerVel += (runnerAcc - friction) * dt;
            if (runnerVel < 0) runnerVel = 0;

            runnerX += runnerVel * dt;

            // Lean at finish: small bonus if pressed near line once
            if (space && !leanedAtFinish) {
                if (runnerX >= TRACK_LENGTH_M - 3 && runnerX <= TRACK_LENGTH_M + 1) {
                    raceTime -= 0.1;
                    leanedAtFinish = true;
                }
            }

            // Finish condition
            if (runnerX >= TRACK_LENGTH_M) {
                runnerX = TRACK_LENGTH_M;
                finished = true;
            }
        }

        public boolean isFinished() {
            return finished;
        }

        public double getTimeSeconds() {
            return raceTime;
        }

        public void render(Graphics2D g) {
            // Background
            g.setColor(new Color(30, 120, 30));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // Track
            int trackTop = 200;
            int trackHeight = 80;
            g.setColor(new Color(130, 60, 40));
            g.fillRect(50, trackTop, WIDTH - 100, trackHeight);

            // Lane lines
            g.setColor(Color.WHITE);
            for (int i = 0; i <= 4; i++) {
                int y = trackTop + i * (trackHeight / 4);
                g.drawLine(50, y, WIDTH - 50, y);
            }

            // Start line at x=50; finish line at 50 + TRACK_LENGTH_M * pixelPerMeter
            int startX = 50;
            int finishX = startX + (int) (TRACK_LENGTH_M * pixelPerMeter);
            g.setStroke(new BasicStroke(3f));
            g.setColor(Color.WHITE);
            g.drawLine(startX, trackTop, startX, trackTop + trackHeight);
            g.drawLine(finishX, trackTop, finishX, trackTop + trackHeight);

            // Runner representation (simple rectangle)
            int runnerPixelX = startX + (int) (runnerX * pixelPerMeter);
            int runnerPixelY = trackTop + trackHeight / 2;
            int runnerWidth = 20;
            int runnerHeight = 30;

            g.setColor(Color.BLUE);
            g.fillRect(runnerPixelX - runnerWidth / 2,
                    runnerPixelY - runnerHeight,
                    runnerWidth, runnerHeight);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("Time: " + String.format("%.2f s", raceTime), 60, 160);
            g.drawString("Speed: " + String.format("%.2f m/s", runnerVel), 60, 180);
        }
    }

    // =====================================================================
    // Long Jump Simulation
    // =====================================================================
    private static class LongJump {
        private Athlete athlete;

        // Run-up track
        private static final double RUNWAY_LENGTH_M = 40.0;
        private static final double TAKE_OFF_BOARD_M = 30.0;

        private double runnerX;      // meters along runway
        private double runnerVel;    // m/s
        private double friction = 0.8;
        private boolean hasJumped;

        // Jump physics
        private double jumpStartX;
        private double vx;           // horizontal velocity after jump
        private double vy;           // vertical velocity
        private double gravity = -9.8;
        private double jumpTime;     // time since take-off

        private boolean finished;
        private double landingDistance; // in meters

        private double pixelPerMeter;

        public LongJump(Athlete athlete) {
            this.athlete = athlete;
            pixelPerMeter = (WIDTH - 150) / (RUNWAY_LENGTH_M + 10);
            reset();
        }

        public void reset() {
            runnerX = 0;
            runnerVel = 0;
            hasJumped = false;
            jumpStartX = 0;
            vx = 0;
            vy = 0;
            jumpTime = 0;
            finished = false;
            landingDistance = 0;
        }

        public void update(double dt, boolean left, boolean right,
                           boolean up, boolean down, boolean space) {
            if (finished) return;

            if (!hasJumped) {
                // Run-up section
                double baseAcc = 4.0 + athlete.getSpeed() * 0.04;
                if (right) {
                    runnerVel += (baseAcc - friction) * dt;
                } else {
                    runnerVel -= friction * dt;
                }
                if (runnerVel < 0) runnerVel = 0;

                runnerX += runnerVel * dt;
                if (runnerX > RUNWAY_LENGTH_M) runnerX = RUNWAY_LENGTH_M;

                // Jump if space pressed
                if (space) {
                    hasJumped = true;
                    jumpStartX = runnerX;

                    double optimalAngleDeg = 20 + athlete.getTechnique() * 0.1;
                    double angleRad = Math.toRadians(Math.min(45, optimalAngleDeg));

                    vx = runnerVel * Math.cos(angleRad);
                    vy = runnerVel * Math.sin(angleRad);
                    jumpTime = 0;
                }
            } else {
                // In-flight projectile
                jumpTime += dt;

                double x = jumpStartX + vx * jumpTime;
                double y = vy * jumpTime + 0.5 * gravity * jumpTime * jumpTime;

                if (y <= 0) {
                    // Landed
                    finished = true;
                    landingDistance = x - TAKE_OFF_BOARD_M;
                    if (landingDistance < 0) landingDistance = 0;
                }
            }
        }

        public boolean isFinished() {
            return finished;
        }

        public double getDistanceMeters() {
            return landingDistance;
        }

        public void render(Graphics2D g) {
            // Background
            g.setColor(new Color(20, 120, 20));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            int groundY = 350;

            // Runway
            int runwayStartX = 50;
            int runwayWidth = (int) (RUNWAY_LENGTH_M * pixelPerMeter);
            g.setColor(new Color(120, 120, 120));
            g.fillRect(runwayStartX, groundY - 30, runwayWidth, 30);

            // Take-off board
            int boardX = runwayStartX + (int) (TAKE_OFF_BOARD_M * pixelPerMeter);
            g.setColor(Color.WHITE);
            g.fillRect(boardX - 5, groundY - 30, 10, 30);

            // Sandpit
            int sandStartX = boardX + 30;
            int sandWidth = 300;
            g.setColor(new Color(194, 178, 128));
            g.fillRect(sandStartX, groundY - 20, sandWidth, 20);

            // Draw athlete
            int athleteWidth = 22;
            int athleteHeight = 35;
            int px, py;

            if (!hasJumped) {
                px = runwayStartX + (int) (runnerX * pixelPerMeter);
                py = groundY - 5;
            } else {
                double x = jumpStartX + vx * jumpTime;
                double y = vy * jumpTime + 0.5 * gravity * jumpTime * jumpTime;
                if (y < 0) y = 0;

                px = runwayStartX + (int) (x * pixelPerMeter);
                py = groundY - (int) (y * 15) - 5;
            }

            g.setColor(Color.RED);
            g.fillRect(px - athleteWidth / 2, py - athleteHeight, athleteWidth, athleteHeight);

            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));

            g.drawString("Run-up speed: " + String.format("%.2f m/s", runnerVel),
                    60, 120);
            if (hasJumped) {
                g.drawString("Flight time: " + String.format("%.2f s", jumpTime),
                        60, 140);
            }

            if (finished) {
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 18));
                g.drawString("Jump distance: " +
                        String.format("%.2f m", landingDistance), 60, 160);
            }
        }
    }

    // =====================================================================
    // Javelin Throw Simulation
    // =====================================================================
    private static class JavelinThrow {
        private Athlete athlete;

        private static final double RUN_UP_LENGTH_M = 30.0;

        private double runnerX;
        private double runnerVel;
        private double friction = 0.9;

        private double angleDeg = 35.0;
        private double minAngle = 10.0;
        private double maxAngle = 60.0;

        private boolean hasThrown;

        private double throwStartX;
        private double vx;
        private double vy;
        private double gravity = -9.8;
        private double throwTime;
        private double landingDistance;
        private boolean finished;

        private double pixelPerMeter;

        public JavelinThrow(Athlete athlete) {
            this.athlete = athlete;
            pixelPerMeter = (WIDTH - 150) / (RUN_UP_LENGTH_M + 60);
            reset();
        }

        public void reset() {
            runnerX = 0;
            runnerVel = 0;
            angleDeg = 35.0;
            hasThrown = false;
            throwStartX = 0;
            vx = vy = 0;
            throwTime = 0;
            landingDistance = 0;
            finished = false;
        }

        public void update(double dt, boolean left, boolean right,
                           boolean up, boolean down, boolean space) {
            if (finished) return;

            if (!hasThrown) {
                // Adjust angle
                if (up) {
                    angleDeg += 40 * dt;
                }
                if (down) {
                    angleDeg -= 40 * dt;
                }
                if (angleDeg < minAngle) angleDeg = minAngle;
                if (angleDeg > maxAngle) angleDeg = maxAngle;

                // Run-up
                double baseAcc = 3.5 + athlete.getPower() * 0.04;
                if (right) {
                    runnerVel += (baseAcc - friction) * dt;
                } else {
                    runnerVel -= friction * dt;
                }
                if (runnerVel < 0) runnerVel = 0;

                runnerX += runnerVel * dt;
                if (runnerX > RUN_UP_LENGTH_M) runnerX = RUN_UP_LENGTH_M;

                if (space) {
                    hasThrown = true;
                    throwStartX = runnerX;

                    double angleRad = Math.toRadians(angleDeg);
                    double releaseSpeed = runnerVel + athlete.getPower() * 0.2;
                    vx = releaseSpeed * Math.cos(angleRad);
                    vy = releaseSpeed * Math.sin(angleRad);
                    throwTime = 0;
                }
            } else {
                throwTime += dt;

                double x = throwStartX + vx * throwTime;
                double y = vy * throwTime + 0.5 * gravity * throwTime * throwTime;

                if (y <= 0) {
                    finished = true;
                    landingDistance = x;
                    if (landingDistance < 0) landingDistance = 0;
                }
            }
        }

        public boolean isFinished() {
            return finished;
        }

        public double getDistanceMeters() {
            return landingDistance;
        }

        public void render(Graphics2D g) {
            g.setColor(new Color(30, 100, 30));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            int groundY = 350;

            // Run-up lane
            int laneStartX = 80;
            int laneWidth = (int) (RUN_UP_LENGTH_M * pixelPerMeter);
            g.setColor(new Color(150, 150, 150));
            g.fillRect(laneStartX, groundY - 30, laneWidth, 30);

            // Sector lines
            int sectorStartX = laneStartX + laneWidth;
            g.setColor(Color.WHITE);
            g.drawLine(sectorStartX, groundY, WIDTH - 20, 260);
            g.drawLine(sectorStartX, groundY, WIDTH - 20, 440);

            // Athlete / javelin position
            int px, py;
            double javelinX, javelinY;
            if (!hasThrown) {
                px = laneStartX + (int) (runnerX * pixelPerMeter);
                py = groundY - 5;

                int athleteWidth = 20;
                int athleteHeight = 35;
                g.setColor(Color.ORANGE);
                g.fillRect(px - athleteWidth / 2, py - athleteHeight,
                        athleteWidth, athleteHeight);

                double angleRad = Math.toRadians(angleDeg);
                int jx2 = px + (int) (Math.cos(angleRad) * 40);
                int jy2 = py - athleteHeight + (int) (-Math.sin(angleRad) * 40);
                g.setStroke(new BasicStroke(3f));
                g.setColor(Color.WHITE);
                g.drawLine(px, py - athleteHeight + 5, jx2, jy2);
            } else {
                throwTime = Math.max(0, throwTime);
                javelinX = throwStartX + vx * throwTime;
                javelinY = vy * throwTime + 0.5 * gravity * throwTime * throwTime;
                if (javelinY < 0) javelinY = 0;

                px = laneStartX + (int) (javelinX * pixelPerMeter);
                py = groundY - (int) (javelinY * 12);

                double angleRad = Math.atan2(vy + gravity * throwTime, vx);
                int jx1 = px;
                int jy1 = py;
                int jx2 = px + (int) (Math.cos(angleRad) * 50);
                int jy2 = py - (int) (Math.sin(angleRad) * 50);

                g.setStroke(new BasicStroke(3f));
                g.setColor(Color.WHITE);
                g.drawLine(jx1, jy1, jx2, jy2);
            }

            // HUD info inside sport
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("Run-up speed: " + String.format("%.2f m/s", runnerVel),
                    60, 120);
            g.drawString("Angle: " + String.format("%.1f°", angleDeg),
                    60, 140);

            if (hasThrown) {
                g.drawString("Flight time: " + String.format("%.2f s", throwTime),
                        60, 160);
            }

            if (finished) {
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 18));
                g.drawString("Throw distance: " +
                                String.format("%.2f m", landingDistance),
                        60, 180);
            }
        }
    }
}
