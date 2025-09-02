import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Paralympic Sports Game - A comprehensive simulation featuring multiple sports
 * with animations, athlete management, and scoring systems.
 */
public class ParalympicSportsGame extends JFrame {
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int GAME_PANEL_WIDTH = 800;
    private static final int CONTROL_PANEL_WIDTH = 400;

    private GamePanel gamePanel;
    private ControlPanel controlPanel;
    private GameEngine gameEngine;
    private SoundManager soundManager;

    public ParalympicSportsGame() {
        initializeGame();
        setupUI();
        startGameLoop();
    }

    private void initializeGame() {
        gameEngine = new GameEngine();
        soundManager = new SoundManager();

        setTitle("Paralympic Sports Game - Adaptive Athletics");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        gamePanel = new GamePanel(gameEngine);
        controlPanel = new ControlPanel(gameEngine, gamePanel);

        add(gamePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);

        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu gameMenu = new JMenu("Game");
        JMenuItem newGame = new JMenuItem("New Game");
        JMenuItem pauseGame = new JMenuItem("Pause/Resume");
        JMenuItem exitGame = new JMenuItem("Exit");

        newGame.addActionListener(e -> gameEngine.resetGame());
        pauseGame.addActionListener(e -> gameEngine.togglePause());
        exitGame.addActionListener(e -> System.exit(0));

        gameMenu.add(newGame);
        gameMenu.add(pauseGame);
        gameMenu.addSeparator();
        gameMenu.add(exitGame);

        JMenu sportsMenu = new JMenu("Sports");
        JMenuItem wheelchairRacing = new JMenuItem("Wheelchair Racing");
        JMenuItem swimming = new JMenuItem("Para Swimming");
        JMenuItem archery = new JMenuItem("Para Archery");
        JMenuItem athletics = new JMenuItem("Para Athletics");

        wheelchairRacing.addActionListener(e -> gameEngine.setSport(SportType.WHEELCHAIR_RACING));
        swimming.addActionListener(e -> gameEngine.setSport(SportType.PARA_SWIMMING));
        archery.addActionListener(e -> gameEngine.setSport(SportType.PARA_ARCHERY));
        athletics.addActionListener(e -> gameEngine.setSport(SportType.PARA_ATHLETICS));

        sportsMenu.add(wheelchairRacing);
        sportsMenu.add(swimming);
        sportsMenu.add(archery);
        sportsMenu.add(athletics);

        menuBar.add(gameMenu);
        menuBar.add(sportsMenu);

        return menuBar;
    }

    private void startGameLoop() {
        Timer gameTimer = new Timer(16, e -> { // ~60 FPS
            gameEngine.update();
            gamePanel.repaint();
        });
        gameTimer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new ParalympicSportsGame().setVisible(true);
        });
    }
}

/**
 * Game Engine - Core logic and state management
 */
class GameEngine {
    private SportType currentSport;
    private List<Athlete> athletes;
    private GameState gameState;
    private Competition currentCompetition;
    private ScoreBoard scoreBoard;
    private Random random;
    private boolean isPaused;
    private long gameTime;
    private ParticleSystem particleSystem;

    public GameEngine() {
        athletes = new ArrayList<>();
        random = new Random();
        gameState = GameState.MENU;
        currentSport = SportType.WHEELCHAIR_RACING;
        scoreBoard = new ScoreBoard();
        particleSystem = new ParticleSystem();
        initializeAthletes();
    }

    private void initializeAthletes() {
        String[] names = {"Alex", "Maria", "Chen", "Amara", "Erik", "Zara", "Omar", "Luna"};
        String[] countries = {"USA", "BRA", "CHN", "ETH", "NOR", "AUS", "EGY", "MEX"};

        for (int i = 0; i < 6; i++) {
            Athlete athlete = new Athlete(
                    names[i % names.length] + (i / names.length > 0 ? " " + (char)('A' + i) : ""),
                    countries[i % countries.length],
                    generateRandomSkills(),
                    generateRandomClassification()
            );
            athletes.add(athlete);
        }
    }

    private AthleteSkills generateRandomSkills() {
        return new AthleteSkills(
                70 + random.nextInt(30), // speed
                60 + random.nextInt(40), // strength
                65 + random.nextInt(35), // endurance
                50 + random.nextInt(50), // technique
                40 + random.nextInt(60)  // focus
        );
    }

    private String generateRandomClassification() {
        String[] classifications = {"T54", "S6", "W1", "F32", "T12", "S14"};
        return classifications[random.nextInt(classifications.length)];
    }

    public void update() {
        if (isPaused || gameState == GameState.MENU) return;

        gameTime += 16; // milliseconds
        particleSystem.update();

        if (currentCompetition != null) {
            currentCompetition.update();

            if (currentCompetition.isFinished()) {
                scoreBoard.updateResults(currentCompetition.getResults());
                gameState = GameState.RESULTS;
            }
        }
    }

    public void startCompetition() {
        if (athletes.isEmpty()) return;

        currentCompetition = createCompetition(currentSport);
        gameState = GameState.COMPETING;
    }

    private Competition createCompetition(SportType sport) {
        switch (sport) {
            case WHEELCHAIR_RACING:
                return new WheelchairRaceCompetition(new ArrayList<>(athletes), random);
            case PARA_SWIMMING:
                return new SwimmingCompetition(new ArrayList<>(athletes), random);
            case PARA_ARCHERY:
                return new ArcheryCompetition(new ArrayList<>(athletes), random);
            case PARA_ATHLETICS:
                return new AthleticsCompetition(new ArrayList<>(athletes), random);
            default:
                return new WheelchairRaceCompetition(new ArrayList<>(athletes), random);
        }
    }

    public void resetGame() {
        gameState = GameState.MENU;
        currentCompetition = null;
        scoreBoard.reset();
        particleSystem.clear();
    }

    public void togglePause() {
        isPaused = !isPaused;
    }

    // Getters and setters
    public SportType getCurrentSport() { return currentSport; }
    public void setSport(SportType sport) {
        this.currentSport = sport;
        if (gameState == GameState.COMPETING) {
            startCompetition();
        }
    }

    public GameState getGameState() { return gameState; }
    public void setGameState(GameState state) { this.gameState = state; }

    public List<Athlete> getAthletes() { return new ArrayList<>(athletes); }
    public Competition getCurrentCompetition() { return currentCompetition; }
    public ScoreBoard getScoreBoard() { return scoreBoard; }
    public ParticleSystem getParticleSystem() { return particleSystem; }
    public boolean isPaused() { return isPaused; }
}

/**
 * Game Panel - Main rendering area
 */
class GamePanel extends JPanel {
    private GameEngine gameEngine;
    private Map<SportType, SportRenderer> renderers;

    public GamePanel(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
        setPreferredSize(new Dimension(GAME_PANEL_WIDTH, WINDOW_HEIGHT));
        setBackground(new Color(45, 85, 45)); // Track green

        initializeRenderers();
        setupMouseListeners();
    }

    private void initializeRenderers() {
        renderers = new HashMap<>();
        renderers.put(SportType.WHEELCHAIR_RACING, new WheelchairRacingRenderer());
        renderers.put(SportType.PARA_SWIMMING, new SwimmingRenderer());
        renderers.put(SportType.PARA_ARCHERY, new ArcheryRenderer());
        renderers.put(SportType.PARA_ATHLETICS, new AthleticsRenderer());
    }

    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (gameEngine.getGameState() == GameState.MENU) {
                    gameEngine.startCompetition();
                } else if (gameEngine.getGameState() == GameState.RESULTS) {
                    gameEngine.setGameState(GameState.MENU);
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (gameEngine.getGameState()) {
            case MENU:
                renderMenu(g2d);
                break;
            case COMPETING:
                renderCompetition(g2d);
                break;
            case RESULTS:
                renderResults(g2d);
                break;
        }

        // Always render particles
        gameEngine.getParticleSystem().render(g2d);
    }

    private void renderMenu(Graphics2D g2d) {
        // Background gradient
        GradientPaint gradient = new GradientPaint(0, 0, new Color(30, 60, 120),
                0, getHeight(), new Color(60, 30, 120));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Paralympic rings
        renderParalympicRings(g2d, getWidth() / 2, getHeight() / 3);

        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        String title = "Paralympic Sports Game";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, getHeight() / 2);

        // Instructions
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        String instruction = "Click to start " + gameEngine.getCurrentSport().getDisplayName();
        fm = g2d.getFontMetrics();
        g2d.drawString(instruction, (getWidth() - fm.stringWidth(instruction)) / 2, getHeight() / 2 + 60);
    }

    private void renderParalympicRings(Graphics2D g2d, int centerX, int centerY) {
        Color[] colors = {Color.BLUE, Color.BLACK, Color.RED, Color.YELLOW, Color.GREEN};
        int ringSize = 40;
        int spacing = 50;

        g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Top row
        for (int i = 0; i < 3; i++) {
            g2d.setColor(colors[i]);
            g2d.drawOval(centerX + (i - 1) * spacing - ringSize/2, centerY - 25, ringSize, ringSize);
        }

        // Bottom row
        for (int i = 0; i < 2; i++) {
            g2d.setColor(colors[i + 3]);
            g2d.drawOval(centerX + (i - 0.5) * spacing - ringSize/2, centerY + 5, ringSize, ringSize);
        }
    }

    private void renderCompetition(Graphics2D g2d) {
        SportRenderer renderer = renderers.get(gameEngine.getCurrentSport());
        if (renderer != null && gameEngine.getCurrentCompetition() != null) {
            renderer.render(g2d, gameEngine.getCurrentCompetition(), getWidth(), getHeight());
        }
    }

    private void renderResults(Graphics2D g2d) {
        // Background
        g2d.setColor(new Color(40, 40, 40));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Title
        g2d.setColor(Color.GOLD);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        String title = "Results - " + gameEngine.getCurrentSport().getDisplayName();
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, 50);

        // Results
        List<CompetitionResult> results = gameEngine.getScoreBoard().getLastResults();
        if (results != null) {
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            Color[] medalColors = {Color.GOLD, new Color(192, 192, 192), new Color(205, 127, 50)};

            for (int i = 0; i < Math.min(results.size(), 3); i++) {
                CompetitionResult result = results.get(i);
                g2d.setColor(i < medalColors.length ? medalColors[i] : Color.WHITE);

                String place = (i + 1) + ". " + result.athlete.getName() +
                        " (" + result.athlete.getCountry() + ") - " + result.getFormattedScore();
                g2d.drawString(place, 50, 120 + i * 40);
            }
        }

        // Continue instruction
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Click to continue", getWidth() / 2 - 60, getHeight() - 50);
    }
}

/**
 * Control Panel - UI controls and information display
 */
class ControlPanel extends JPanel {
    private GameEngine gameEngine;
    private GamePanel gamePanel;
    private JList<Athlete> athleteList;
    private JTextArea infoArea;
    private JButton startButton;
    private JComboBox<SportType> sportSelector;

    public ControlPanel(GameEngine gameEngine, GamePanel gamePanel) {
        this.gameEngine = gameEngine;
        this.gamePanel = gamePanel;

        setPreferredSize(new Dimension(CONTROL_PANEL_WIDTH, WINDOW_HEIGHT));
        setBackground(new Color(240, 240, 240));
        setBorder(BorderFactory.createTitledBorder("Control Panel"));

        setupComponents();
        layoutComponents();
        setupListeners();
    }

    private void setupComponents() {
        // Sport selector
        sportSelector = new JComboBox<>(SportType.values());
        sportSelector.setSelectedItem(gameEngine.getCurrentSport());

        // Athlete list
        athleteList = new JList<>(gameEngine.getAthletes().toArray(new Athlete[0]));
        athleteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        athleteList.setCellRenderer(new AthleteListCellRenderer());

        // Info area
        infoArea = new JTextArea(10, 30);
        infoArea.setEditable(false);
        infoArea.setBackground(new Color(250, 250, 250));
        infoArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // Start button
        startButton = new JButton("Start Competition");
        startButton.setPreferredSize(new Dimension(200, 40));
        startButton.setBackground(new Color(70, 130, 180));
        startButton.setForeground(Color.WHITE);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        topPanel.add(new JLabel("Select Sport:"));
        topPanel.add(sportSelector);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JLabel("Athletes:"), BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(athleteList), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new JLabel("Information:"), BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        bottomPanel.add(startButton, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        sportSelector.addActionListener(e -> {
            SportType selected = (SportType) sportSelector.getSelectedItem();
            gameEngine.setSport(selected);
            updateInfo();
        });

        athleteList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateInfo();
            }
        });

        startButton.addActionListener(e -> {
            gameEngine.startCompetition();
        });

        // Update info periodically
        Timer updateTimer = new Timer(500, e -> updateInfo());
        updateTimer.start();
    }

    private void updateInfo() {
        StringBuilder info = new StringBuilder();

        info.append("=== GAME STATUS ===\n");
        info.append("Sport: ").append(gameEngine.getCurrentSport().getDisplayName()).append("\n");
        info.append("State: ").append(gameEngine.getGameState()).append("\n");
        info.append("Paused: ").append(gameEngine.isPaused()).append("\n\n");

        Athlete selected = athleteList.getSelectedValue();
        if (selected != null) {
            info.append("=== ATHLETE INFO ===\n");
            info.append("Name: ").append(selected.getName()).append("\n");
            info.append("Country: ").append(selected.getCountry()).append("\n");
            info.append("Classification: ").append(selected.getClassification()).append("\n");
            info.append("Speed: ").append(selected.getSkills().speed).append("\n");
            info.append("Strength: ").append(selected.getSkills().strength).append("\n");
            info.append("Endurance: ").append(selected.getSkills().endurance).append("\n");
            info.append("Technique: ").append(selected.getSkills().technique).append("\n");
            info.append("Focus: ").append(selected.getSkills().focus).append("\n\n");
        }

        if (gameEngine.getCurrentCompetition() != null) {
            info.append("=== COMPETITION ===\n");
            info.append("Progress: ").append(String.format("%.1f%%",
                    gameEngine.getCurrentCompetition().getProgress() * 100)).append("\n");
        }

        infoArea.setText(info.toString());
    }
}

/**
 * Athlete class representing Paralympic athletes
 */
class Athlete {
    private String name;
    private String country;
    private AthleteSkills skills;
    private String classification;
    private double currentPosition;
    private double currentSpeed;
    private Color color;

    public Athlete(String name, String country, AthleteSkills skills, String classification) {
        this.name = name;
        this.country = country;
        this.skills = skills;
        this.classification = classification;
        this.currentPosition = 0;
        this.currentSpeed = 0;
        this.color = generateColor();
    }

    private Color generateColor() {
        Random r = new Random(name.hashCode());
        return new Color(r.nextInt(128) + 127, r.nextInt(128) + 127, r.nextInt(128) + 127);
    }

    @Override
    public String toString() {
        return name + " (" + country + ") - " + classification;
    }

    // Getters and setters
    public String getName() { return name; }
    public String getCountry() { return country; }
    public AthleteSkills getSkills() { return skills; }
    public String getClassification() { return classification; }
    public double getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(double position) { this.currentPosition = position; }
    public double getCurrentSpeed() { return currentSpeed; }
    public void setCurrentSpeed(double speed) { this.currentSpeed = speed; }
    public Color getColor() { return color; }
}

/**
 * Athlete skills representation
 */
class AthleteSkills {
    public int speed;
    public int strength;
    public int endurance;
    public int technique;
    public int focus;

    public AthleteSkills(int speed, int strength, int endurance, int technique, int focus) {
        this.speed = speed;
        this.strength = strength;
        this.endurance = endurance;
        this.technique = technique;
        this.focus = focus;
    }
}

/**
 * Competition abstract class
 */
abstract class Competition {
    protected List<Athlete> athletes;
    protected Random random;
    protected boolean finished;
    protected double progress;
    protected List<CompetitionResult> results;

    public Competition(List<Athlete> athletes, Random random) {
        this.athletes = athletes;
        this.random = random;
        this.finished = false;
        this.progress = 0;
        this.results = new ArrayList<>();
    }

    public abstract void update();
    public abstract double getProgress();

    public boolean isFinished() { return finished; }
    public List<CompetitionResult> getResults() { return new ArrayList<>(results); }
    public List<Athlete> getAthletes() { return athletes; }
}

/**
 * Wheelchair Racing Competition
 */
class WheelchairRaceCompetition extends Competition {
    private static final double RACE_DISTANCE = 400.0; // meters
    private int frameCount = 0;

    public WheelchairRaceCompetition(List<Athlete> athletes, Random random) {
        super(athletes, random);
    }

    @Override
    public void update() {
        if (finished) return;

        frameCount++;
        boolean raceFinished = true;

        for (Athlete athlete : athletes) {
            if (athlete.getCurrentPosition() < RACE_DISTANCE) {
                raceFinished = false;

                // Calculate speed based on skills and fatigue
                double baseSpeed = (athlete.getSkills().speed + athlete.getSkills().endurance) / 20.0;
                double fatigue = Math.min(1.0, athlete.getCurrentPosition() / RACE_DISTANCE);
                double currentSpeed = baseSpeed * (1.0 - fatigue * 0.3) + random.nextGaussian() * 0.1;
                currentSpeed = Math.max(0.1, currentSpeed);

                athlete.setCurrentSpeed(currentSpeed);
                athlete.setCurrentPosition(athlete.getCurrentPosition() + currentSpeed);

                // Check if finished
                if (athlete.getCurrentPosition() >= RACE_DISTANCE &&
                        !results.stream().anyMatch(r -> r.athlete == athlete)) {
                    double time = frameCount / 60.0; // Convert frames to seconds
                    results.add(new CompetitionResult(athlete, time, "time"));
                }
            }
        }

        // Sort results by time
        results.sort((a, b) -> Double.compare(a.score, b.score));

        progress = athletes.stream().mapToDouble(a ->
                Math.min(1.0, a.getCurrentPosition() / RACE_DISTANCE)).average().orElse(0);

        finished = raceFinished;
    }

    @Override
    public double getProgress() {
        return progress;
    }
}

/**
 * Swimming Competition
 */
class SwimmingCompetition extends Competition {
    private static final double POOL_LENGTH = 50.0; // meters
    private static final int LAPS = 4; // 200m race
    private int frameCount = 0;

    public SwimmingCompetition(List<Athlete> athletes, Random random) {
        super(athletes, random);
    }

    @Override
    public void update() {
        if (finished) return;

        frameCount++;
        boolean raceFinished = true;

        for (Athlete athlete : athletes) {
            double totalDistance = POOL_LENGTH * LAPS;
            if (athlete.getCurrentPosition() < totalDistance) {
                raceFinished = false;

                double baseSpeed = (athlete.getSkills().speed + athlete.getSkills().technique) / 25.0;
                double currentSpeed = baseSpeed + random.nextGaussian() * 0.05;
                currentSpeed = Math.max(0.05, currentSpeed);

                athlete.setCurrentSpeed(currentSpeed);
                athlete.setCurrentPosition(athlete.getCurrentPosition() + currentSpeed);

                if (athlete.getCurrentPosition() >= totalDistance &&
                        !results.stream().anyMatch(r -> r.athlete == athlete)) {
                    double time = frameCount / 60.0;
                    results.add(new CompetitionResult(athlete, time, "time"));
                }
            }
        }

        results.sort((a, b) -> Double.compare(a.score, b.score));
        progress = athletes.stream().mapToDouble(a ->
                Math.min(1.0, a.getCurrentPosition() / (POOL_LENGTH * LAPS))).average().orElse(0);

        finished = raceFinished;
    }

    @Override
    public double getProgress() {
        return progress;
    }
}

/**
 * Archery Competition
 */
class ArcheryCompetition extends Competition {
    private static final int ARROWS_PER_ATHLETE = 12;
    private Map<Athlete, Integer> arrowsShot;
    private Map<Athlete, List<Double>> scores;

    public ArcheryCompetition(List<Athlete> athletes, Random random) {
        super(athletes, random);
        arrowsShot = new HashMap<>();
        scores = new HashMap<>();

        for (Athlete athlete : athletes) {
            arrowsShot.put(athlete, 0);
            scores.put(athlete, new ArrayList<>());
        }
    }

    @Override
    public void update() {
        if (finished) return;

        boolean allFinished = true;

        for (Athlete athlete : athletes) {
            int arrows = arrowsShot.get(athlete);
            if (arrows < ARROWS_PER_ATHLETE) {
                allFinished = false;

                if (random.nextInt(30) == 0) { // Shoot arrow occasionally
                    double accuracy = (athlete.getSkills().focus + athlete.getSkills().technique) / 200.0;
                    double score = Math.max(0, 10 * accuracy + random.nextGaussian() * 2);
                    score = Math.min(10, score);

                    scores.get(athlete).add(score);
                    arrowsShot.put(athlete, arrows + 1);

                    if (arrows + 1 >= ARROWS_PER_ATHLETE) {
                        double totalScore = scores.get(athlete).stream().mapToDouble(Double::doubleValue).sum();
                        results.add(new CompetitionResult(athlete, totalScore, "points"));
                    }
                }
            }
        }

        results.sort((a, b) -> Double.compare(b.score, a.score)); // Higher score is better

        progress = arrowsShot.values().stream().mapToInt(Integer::intValue).average().orElse(0) / ARROWS_PER_ATHLETE;
        finished = allFinished;
    }

    @Override
    public double getProgress() {
        return progress;
    }

    public Map<Athlete, List<Double>> getScores() { return scores; }
    public Map<Athlete, Integer> getArrowsShot() { return arrowsShot; }
}

/**
 * Athletics Competition (Shot Put)
 */
class AthleticsCompetition extends Competition {
    private static final int ATTEMPTS_PER_ATHLETE = 3;
    private Map<Athlete, Integer> attempts;
    private Map<Athlete, List<Double>> distances;

    public AthleticsCompetition(List<Athlete> athletes, Random random) {
        super(athletes, random);
        attempts = new HashMap<>();
        distances = new HashMap<>();

        for (Athlete athlete : athletes) {
            attempts.put(athlete, 0);
            distances.put(athlete, new ArrayList<>());
        }
    }

    @Override
    public void update() {
        if (finished) return;

        boolean allFinished = true;

        for (Athlete athlete : athletes) {
            int attemptCount = attempts.get(athlete);
            if (attemptCount < ATTEMPTS_PER_ATHLETE) {
                allFinished = false;

                if (random.nextInt(60) == 0) { // Throw occasionally
                    double power = (athlete.getSkills().strength + athlete.getSkills().technique) / 20.0;
                    double distance = Math.max(0, power + random.nextGaussian() * 2);

                    distances.get(athlete).add(distance);
                    attempts.put(athlete, attem