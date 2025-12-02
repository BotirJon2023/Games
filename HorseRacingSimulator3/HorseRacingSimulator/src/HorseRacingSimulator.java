import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class HorseRacingSimulator extends JFrame {

    // Game constants
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int NUM_HORSES = 8;
    private static final int TRACK_START_X = 100;
    private static final int TRACK_END_X = 1050;
    private static final int TRACK_LENGTH = TRACK_END_X - TRACK_START_X;
    private static final int HORSE_HEIGHT = 60;
    private static final int ANIMATION_DELAY = 30;

    // Game state
    private List<Horse> horses;
    private List<RaceResult> raceHistory;
    private int playerMoney = 1000;
    private int currentBet = 0;
    private int selectedHorseIndex = -1;
    private boolean raceInProgress = false;
    private boolean raceFinished = false;
    private Weather currentWeather = Weather.SUNNY;
    private TrackType currentTrack = TrackType.GRASS;
    private int raceNumber = 0;

    // UI Components
    private RacePanel racePanel;
    private JPanel controlPanel;
    private JPanel statsPanel;
    private JLabel moneyLabel;
    private JLabel weatherLabel;
    private JLabel trackLabel;
    private JComboBox<String> horseSelector;
    private JSpinner betSpinner;
    private JButton startButton;
    private JButton resetButton;
    private JTextArea raceLog;
    private JList<String> leaderboardList;
    private DefaultListModel<String> leaderboardModel;

    // Animation
    private Timer animationTimer;
    private long raceStartTime;
    private List<Horse> finishOrder;

    // Horse names for variety
    private static final String[] HORSE_NAMES = {
            "Thunder Bolt", "Silver Arrow", "Golden Star", "Midnight Runner",
            "Storm Chaser", "Desert Wind", "Ocean Wave", "Fire Spirit",
            "Shadow Dancer", "Lucky Charm", "Swift Justice", "Royal Pride"
    };

    // Horse colors
    private static final Color[] HORSE_COLORS = {
            new Color(139, 69, 19),   // Brown
            new Color(64, 64, 64),    // Dark Gray
            new Color(255, 215, 0),   // Gold
            new Color(0, 0, 0),       // Black
            new Color(255, 255, 255), // White
            new Color(205, 133, 63),  // Tan
            new Color(128, 0, 0),     // Maroon
            new Color(169, 169, 169)  // Silver
    };

    /**
     * Weather conditions that affect race outcomes
     */
    enum Weather {
        SUNNY("Sunny ☀️", 1.0),
        CLOUDY("Cloudy ☁️", 0.95),
        RAINY("Rainy 🌧️", 0.85),
        STORMY("Stormy ⛈️", 0.75),
        WINDY("Windy 💨", 0.90);

        final String display;
        final double speedModifier;

        Weather(String display, double speedModifier) {
            this.display = display;
            this.speedModifier = speedModifier;
        }
    }

    /**
     * Track types with different characteristics
     */
    enum TrackType {
        GRASS("Grass Track", new Color(34, 139, 34), 1.0),
        DIRT("Dirt Track", new Color(139, 90, 43), 0.95),
        SAND("Sand Track", new Color(194, 178, 128), 0.85),
        SYNTHETIC("Synthetic Track", new Color(100, 100, 100), 1.05);

        final String name;
        final Color color;
        final double speedModifier;

        TrackType(String name, Color color, double speedModifier) {
            this.name = name;
            this.color = color;
            this.speedModifier = speedModifier;
        }
    }

    /**
     * Horse class representing a racing horse
     */
    class Horse {
        String name;
        Color color;
        double baseSpeed;
        double stamina;
        double acceleration;
        double currentSpeed;
        double position;
        int wins;
        int races;
        double totalEarnings;
        boolean finished;
        long finishTime;
        int legFrame;
        double energy;

        Horse(String name, Color color) {
            this.name = name;
            this.color = color;
            this.baseSpeed = 2.0 + Math.random() * 2.0;
            this.stamina = 0.7 + Math.random() * 0.3;
            this.acceleration = 0.8 + Math.random() * 0.4;
            this.wins = 0;
            this.races = 0;
            this.totalEarnings = 0;
            reset();
        }

        void reset() {
            this.position = 0;
            this.currentSpeed = 0;
            this.finished = false;
            this.finishTime = 0;
            this.legFrame = 0;
            this.energy = 1.0;
        }

        void update(double weatherMod, double trackMod) {
            if (finished) return;

            // Update leg animation frame
            legFrame = (legFrame + 1) % 8;

            // Calculate target speed based on conditions
            double targetSpeed = baseSpeed * weatherMod * trackMod;

            // Energy management
            if (position > TRACK_LENGTH * 0.7) {
                // Final stretch - use remaining energy
                targetSpeed *= (0.9 + energy * 0.3);
                energy -= 0.002;
            } else {
                // Pace management
                energy -= 0.0005;
                energy = Math.max(0.3, energy);
            }

            // Acceleration towards target speed
            if (currentSpeed < targetSpeed) {
                currentSpeed += acceleration * 0.1 * (1 + Math.random() * 0.2);
            }

            // Random speed variation for realism
            currentSpeed *= (0.95 + Math.random() * 0.1);

            // Stamina effect
            currentSpeed *= stamina;

            // Update position
            position += currentSpeed;

            // Check if finished
            if (position >= TRACK_LENGTH) {
                position = TRACK_LENGTH;
                finished = true;
                finishTime = System.currentTimeMillis() - raceStartTime;
            }
        }

        double getWinRate() {
            return races > 0 ? (double) wins / races * 100 : 0;
        }

        double getOdds() {
            double baseOdds = 5.0 - (baseSpeed - 2.0);
            if (races > 0) {
                baseOdds -= getWinRate() / 25;
            }
            return Math.max(1.5, Math.min(10.0, baseOdds));
        }
    }

    /**
     * Race result for history tracking
     */
    class RaceResult {
        int raceNum;
        String winner;
        long winTime;
        Weather weather;
        TrackType track;
        List<String> positions;

        RaceResult(int raceNum, String winner, long winTime, Weather weather,
                   TrackType track, List<String> positions) {
            this.raceNum = raceNum;
            this.winner = winner;
            this.winTime = winTime;
            this.weather = weather;
            this.track = track;
            this.positions = positions;
        }
    }

    /**
     * Custom panel for rendering the race track and horses
     */
    class RacePanel extends JPanel {
        private Image bgBuffer;
        private Graphics2D bgGraphics;

        RacePanel() {
            setPreferredSize(new Dimension(WINDOW_WIDTH, 550));
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            drawBackground(g2d);
            drawTrack(g2d);
            drawHorses(g2d);
            drawFinishLine(g2d);
            drawRaceInfo(g2d);

            if (raceFinished) {
                drawResults(g2d);
            }
        }

        private void drawBackground(Graphics2D g2d) {
            // Sky gradient based on weather
            Color skyTop, skyBottom;
            switch (currentWeather) {
                case RAINY:
                case STORMY:
                    skyTop = new Color(70, 80, 90);
                    skyBottom = new Color(120, 130, 140);
                    break;
                case CLOUDY:
                    skyTop = new Color(150, 160, 170);
                    skyBottom = new Color(200, 210, 220);
                    break;
                default:
                    skyTop = new Color(135, 206, 235);
                    skyBottom = new Color(176, 226, 255);
            }

            GradientPaint skyGradient = new GradientPaint(
                    0, 0, skyTop, 0, 150, skyBottom);
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, getWidth(), 150);

            // Clouds
            g2d.setColor(new Color(255, 255, 255, 180));
            drawCloud(g2d, 100, 50, 60);
            drawCloud(g2d, 300, 30, 80);
            drawCloud(g2d, 600, 60, 50);
            drawCloud(g2d, 900, 40, 70);

            // Sun or rain
            if (currentWeather == Weather.SUNNY) {
                g2d.setColor(new Color(255, 255, 0, 200));
                g2d.fillOval(1050, 20, 80, 80);
                g2d.setColor(new Color(255, 200, 0, 100));
                g2d.fillOval(1040, 10, 100, 100);
            } else if (currentWeather == Weather.RAINY || currentWeather == Weather.STORMY) {
                drawRain(g2d);
            }

            // Distant mountains
            g2d.setColor(new Color(100, 120, 100));
            int[] mountainX = {0, 150, 300, 450, 600, 750, 900, 1050, 1200, 1200, 0};
            int[] mountainY = {150, 100, 130, 80, 120, 90, 140, 110, 150, 150, 150};
            g2d.fillPolygon(mountainX, mountainY, mountainX.length);

            // Ground
            g2d.setColor(currentTrack.color.darker());
            g2d.fillRect(0, 150, getWidth(), getHeight() - 150);
        }

        private void drawCloud(Graphics2D g2d, int x, int y, int size) {
            g2d.fillOval(x, y, size, size / 2);
            g2d.fillOval(x + size / 3, y - size / 4, size, size / 2);
            g2d.fillOval(x + size * 2 / 3, y, size, size / 2);
        }

        private void drawRain(Graphics2D g2d) {
            g2d.setColor(new Color(100, 100, 200, 100));
            Random rand = new Random(System.currentTimeMillis() / 100);
            for (int i = 0; i < 100; i++) {
                int x = rand.nextInt(getWidth());
                int y = rand.nextInt(getHeight());
                g2d.drawLine(x, y, x - 2, y + 10);
            }
        }

        private void drawTrack(Graphics2D g2d) {
            int trackY = 170;
            int laneHeight = HORSE_HEIGHT + 15;

            // Track surface
            g2d.setColor(currentTrack.color);
            g2d.fillRect(TRACK_START_X - 20, trackY - 10,
                    TRACK_LENGTH + 60, NUM_HORSES * laneHeight + 20);

            // Track border
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(TRACK_START_X - 20, trackY - 10,
                    TRACK_LENGTH + 60, NUM_HORSES * laneHeight + 20);

            // Lane dividers and numbers
            g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10, new float[]{10, 5}, 0));
            g2d.setFont(new Font("Arial", Font.BOLD, 14));

            for (int i = 0; i < NUM_HORSES; i++) {
                int laneY = trackY + i * laneHeight;

                // Lane background alternating colors
                if (i % 2 == 0) {
                    g2d.setColor(new Color(255, 255, 255, 30));
                    g2d.fillRect(TRACK_START_X, laneY, TRACK_LENGTH, laneHeight);
                }

                // Lane divider
                g2d.setColor(Color.WHITE);
                g2d.drawLine(TRACK_START_X, laneY + laneHeight,
                        TRACK_END_X + 20, laneY + laneHeight);

                // Lane number
                g2d.setColor(Color.WHITE);
                g2d.drawString(String.valueOf(i + 1), TRACK_START_X - 15, laneY + laneHeight / 2 + 5);

                // Horse name
                if (horses != null && i < horses.size()) {
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    g2d.drawString(horses.get(i).name, TRACK_START_X + 5, laneY + 12);
                    g2d.setFont(new Font("Arial", Font.BOLD, 14));
                }
            }

            // Distance markers
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(1));
            for (int i = 0; i <= 10; i++) {
                int markerX = TRACK_START_X + (TRACK_LENGTH * i / 10);
                g2d.drawLine(markerX, trackY - 10, markerX, trackY - 5);
                g2d.setFont(new Font("Arial", Font.PLAIN, 9));
                g2d.drawString((i * 10) + "%", markerX - 10, trackY - 15);
            }
        }

        private void drawHorses(Graphics2D g2d) {
            if (horses == null) return;

            int trackY = 170;
            int laneHeight = HORSE_HEIGHT + 15;

            for (int i = 0; i < horses.size(); i++) {
                Horse horse = horses.get(i);
                int horseX = TRACK_START_X + (int) horse.position;
                int horseY = trackY + i * laneHeight + 10;

                drawHorse(g2d, horse, horseX, horseY);

                // Progress indicator
                drawProgressBar(g2d, horse, TRACK_START_X, horseY + HORSE_HEIGHT, 80, 5);
            }
        }

        private void drawHorse(Graphics2D g2d, Horse horse, int x, int y) {
            // Horse body
            g2d.setColor(horse.color);

            // Body (oval)
            g2d.fillOval(x, y + 15, 50, 30);

            // Neck
            int[] neckX = {x + 45, x + 55, x + 60, x + 50};
            int[] neckY = {y + 20, y + 5, y + 10, y + 25};
            g2d.fillPolygon(neckX, neckY, 4);

            // Head
            g2d.fillOval(x + 52, y, 20, 15);

            // Ear
            int[] earX = {x + 60, x + 63, x + 66};
            int[] earY = {y + 3, y - 5, y + 3};
            g2d.fillPolygon(earX, earY, 3);

            // Eye
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x + 65, y + 4, 4, 4);

            // Mane
            g2d.setColor(horse.color.darker().darker());
            g2d.setStroke(new BasicStroke(2));
            for (int i = 0; i < 5; i++) {
                int maneX = x + 45 + i * 3;
                g2d.drawLine(maneX, y + 10, maneX - 5, y + 5);
            }

            // Legs with animation
            g2d.setColor(horse.color);
            g2d.setStroke(new BasicStroke(4));

            int legOffset1 = (int) (Math.sin(horse.legFrame * Math.PI / 4) * 8);
            int legOffset2 = (int) (Math.sin((horse.legFrame + 4) * Math.PI / 4) * 8);

            // Front legs
            g2d.drawLine(x + 40, y + 40, x + 40 + legOffset1, y + 55);
            g2d.drawLine(x + 35, y + 40, x + 35 - legOffset2, y + 55);

            // Back legs
            g2d.drawLine(x + 10, y + 40, x + 10 + legOffset2, y + 55);
            g2d.drawLine(x + 5, y + 40, x + 5 - legOffset1, y + 55);

            // Tail
            g2d.setColor(horse.color.darker().darker());
            g2d.setStroke(new BasicStroke(3));
            int tailWave = (int) (Math.sin(horse.legFrame * Math.PI / 4) * 5);
            g2d.drawLine(x, y + 25, x - 15 + tailWave, y + 35);
            g2d.drawLine(x - 15 + tailWave, y + 35, x - 20 + tailWave, y + 45);

            // Jockey
            drawJockey(g2d, x + 20, y + 5, horse);

            // Horse number on blanket
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            int horseNum = horses.indexOf(horse) + 1;
            g2d.drawString(String.valueOf(horseNum), x + 20, y + 35);
        }

        private void drawJockey(Graphics2D g2d, int x, int y, Horse horse) {
            // Jockey colors based on horse index
            int index = horses.indexOf(horse);
            Color[] jerseyColors = {
                    Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
                    Color.ORANGE, Color.PINK, Color.CYAN, Color.MAGENTA
            };
            Color jerseyColor = jerseyColors[index % jerseyColors.length];

            // Body
            g2d.setColor(jerseyColor);
            g2d.fillOval(x, y, 15, 20);

            // Head
            g2d.setColor(new Color(255, 220, 185));
            g2d.fillOval(x + 2, y - 8, 10, 10);

            // Helmet
            g2d.setColor(jerseyColor.darker());
            g2d.fillArc(x + 1, y - 10, 12, 10, 0, 180);

            // Arms holding reins
            g2d.setColor(jerseyColor);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(x + 15, y + 5, x + 25, y + 10);

            // Reins
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawLine(x + 25, y + 10, x + 40, y);
        }

        private void drawProgressBar(Graphics2D g2d, Horse horse, int x, int y, int width, int height) {
            // Background
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(x, y, width, height);

            // Progress
            double progress = horse.position / TRACK_LENGTH;
            int progressWidth = (int) (width * progress);

            // Color based on position
            Color progressColor;
            if (progress > 0.9) {
                progressColor = Color.GREEN;
            } else if (progress > 0.5) {
                progressColor = Color.YELLOW;
            } else {
                progressColor = Color.RED;
            }

            g2d.setColor(progressColor);
            g2d.fillRect(x, y, progressWidth, height);

            // Border
            g2d.setColor(Color.WHITE);
            g2d.drawRect(x, y, width, height);
        }

        private void drawFinishLine(Graphics2D g2d) {
            int trackY = 170;
            int laneHeight = HORSE_HEIGHT + 15;
            int finishX = TRACK_END_X;

            // Checkered pattern
            g2d.setColor(Color.WHITE);
            int squareSize = 10;
            for (int row = 0; row < NUM_HORSES * laneHeight / squareSize + 2; row++) {
                for (int col = 0; col < 2; col++) {
                    if ((row + col) % 2 == 0) {
                        g2d.setColor(Color.WHITE);
                    } else {
                        g2d.setColor(Color.BLACK);
                    }
                    g2d.fillRect(finishX + col * squareSize,
                            trackY - 10 + row * squareSize,
                            squareSize, squareSize);
                }
            }

            // Finish banner
            g2d.setColor(Color.RED);
            g2d.fillRect(finishX - 5, trackY - 40, 30, 25);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString("FINISH", finishX - 2, trackY - 22);
        }

        private void drawRaceInfo(Graphics2D g2d) {
            // Race info panel
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRoundRect(10, 10, 200, 80, 10, 10);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("Race #" + (raceNumber + 1), 20, 30);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("Weather: " + currentWeather.display, 20, 48);
            g2d.drawString("Track: " + currentTrack.name, 20, 66);
            g2d.drawString("Balance: $" + playerMoney, 20, 84);

            // Live standings during race
            if (raceInProgress && !raceFinished) {
                drawLiveStandings(g2d);
            }
        }

        private void drawLiveStandings(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRoundRect(getWidth() - 180, 10, 170, NUM_HORSES * 18 + 30, 10, 10);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("LIVE STANDINGS", getWidth() - 170, 28);

            // Sort horses by position
            List<Horse> sorted = new ArrayList<>(horses);
            sorted.sort((a, b) -> Double.compare(b.position, a.position));

            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            for (int i = 0; i < sorted.size(); i++) {
                Horse h = sorted.get(i);
                String status = h.finished ? "✓" : String.format("%.0f%%", h.position / TRACK_LENGTH * 100);
                g2d.drawString((i + 1) + ". " + h.name + " " + status,
                        getWidth() - 170, 45 + i * 18);
            }
        }

        private void drawResults(Graphics2D g2d) {
            // Semi-transparent overlay
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Results panel
            int panelWidth = 400;
            int panelHeight = 350;
            int panelX = (getWidth() - panelWidth) / 2;
            int panelY = (getHeight() - panelHeight) / 2;

            // Panel background
            g2d.setColor(new Color(40, 40, 60));
            g2d.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 20, 20);

            // Gold border
            g2d.setColor(new Color(255, 215, 0));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 20, 20);

            // Title
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            String title = "🏆 RACE RESULTS 🏆";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, panelX + (panelWidth - fm.stringWidth(title)) / 2, panelY + 40);

            // Results
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            String[] medals = {"🥇", "🥈", "🥉"};

            for (int i = 0; i < Math.min(finishOrder.size(), 8); i++) {
                Horse h = finishOrder.get(i);
                String medal = i < 3 ? medals[i] : (i + 1) + ".";
                String time = String.format("%.2fs", h.finishTime / 1000.0);

                // Highlight if player's horse
                if (horses.indexOf(h) == selectedHorseIndex) {
                    g2d.setColor(new Color(255, 215, 0, 100));
                    g2d.fillRect(panelX + 20, panelY + 55 + i * 28, panelWidth - 40, 25);
                }

                g2d.setColor(i < 3 ? new Color(255, 215, 0) : Color.WHITE);
                g2d.drawString(medal + " " + h.name, panelX + 30, panelY + 75 + i * 28);
                g2d.drawString(time, panelX + panelWidth - 80, panelY + 75 + i * 28);
            }

            // Betting result
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            int resultY = panelY + panelHeight - 50;

            if (selectedHorseIndex >= 0 && currentBet > 0) {
                Horse betHorse = horses.get(selectedHorseIndex);
                int position = finishOrder.indexOf(betHorse) + 1;

                if (position == 1) {
                    int winnings = (int) (currentBet * betHorse.getOdds());
                    g2d.setColor(Color.GREEN);
                    g2d.drawString("YOU WON $" + winnings + "! 🎉", panelX + 100, resultY);
                } else if (position <= 3) {
                    int winnings = (int) (currentBet * 0.5);
                    g2d.setColor(Color.YELLOW);
                    g2d.drawString("Placed " + position + "! Won $" + winnings, panelX + 100, resultY);
                } else {
                    g2d.setColor(Color.RED);
                    g2d.drawString("Better luck next time!", panelX + 100, resultY);
                }
            } else {
                g2d.setColor(Color.GRAY);
                g2d.drawString("No bet placed", panelX + 130, resultY);
            }
        }
    }

    /**
     * Main constructor - initializes the game
     */
    public HorseRacingSimulator() {
        setTitle("🏇 Horse Racing Simulator 🏇");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        // Initialize game state
        horses = new ArrayList<>();
        raceHistory = new ArrayList<>();
        finishOrder = new ArrayList<>();

        initializeHorses();
        createUI();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // Welcome message
        logMessage("Welcome to Horse Racing Simulator!");
        logMessage("Place your bets and enjoy the race!");
        logMessage("=".repeat(40));
    }

    /**
     * Initialize the horses with random attributes
     */
    private void initializeHorses() {
        horses.clear();
        Random rand = new Random();
        List<String> availableNames = new ArrayList<>(Arrays.asList(HORSE_NAMES));
        Collections.shuffle(availableNames);

        for (int i = 0; i < NUM_HORSES; i++) {
            String name = availableNames.get(i);
            Color color = HORSE_COLORS[i % HORSE_COLORS.length];
            horses.add(new Horse(name, color));
        }
    }

    /**
     * Create all UI components
     */
    private void createUI() {
        // Race panel (center)
        racePanel = new RacePanel();
        add(racePanel, BorderLayout.CENTER);

        // Control panel (right side)
        createControlPanel();
        add(controlPanel, BorderLayout.EAST);

        // Stats panel (bottom)
        createStatsPanel();
        add(statsPanel, BorderLayout.SOUTH);
    }

    /**
     * Create the control panel with betting and race controls
     */
    private void createControlPanel() {
        controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setPreferredSize(new Dimension(280, 550));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        controlPanel.setBackground(new Color(45, 45, 65));

        // Title
        JLabel titleLabel = createStyledLabel("🎰 BETTING BOOTH 🎰", 18, true);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(titleLabel);
        controlPanel.add(Box.createVerticalStrut(15));

        // Money display
        moneyLabel = createStyledLabel("Balance: $" + playerMoney, 16, true);
        moneyLabel.setForeground(Color.GREEN);
        moneyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(moneyLabel);
        controlPanel.add(Box.createVerticalStrut(15));

        // Horse selection
        JLabel selectLabel = createStyledLabel("Select Horse:", 14, false);
        controlPanel.add(selectLabel);

        String[] horseOptions = new String[NUM_HORSES + 1];
        horseOptions[0] = "-- Select a horse --";
        for (int i = 0; i < NUM_HORSES; i++) {
            Horse h = horses.get(i);
            horseOptions[i + 1] = String.format("%d. %s (%.1fx)", i + 1, h.name, h.getOdds());
        }

        horseSelector = new JComboBox<>(horseOptions);
        horseSelector.setMaximumSize(new Dimension(260, 30));
        horseSelector.addActionListener(e -> {
            selectedHorseIndex = horseSelector.getSelectedIndex() - 1;
            updateHorseStats();
        });
        controlPanel.add(horseSelector);
        controlPanel.add(Box.createVerticalStrut(10));

        // Bet amount
        JLabel betLabel = createStyledLabel("Bet Amount:", 14, false);
        controlPanel.add(betLabel);

        SpinnerNumberModel betModel = new SpinnerNumberModel(50, 10, playerMoney, 10);
        betSpinner = new JSpinner(betModel);
        betSpinner.setMaximumSize(new Dimension(260, 30));
        controlPanel.add(betSpinner);
        controlPanel.add(Box.createVerticalStrut(10));

        // Quick bet buttons
        JPanel quickBetPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        quickBetPanel.setOpaque(false);
        quickBetPanel.setMaximumSize(new Dimension(260, 35));

        int[] quickBets = {50, 100, 250, 500};
        for (int bet : quickBets) {
            JButton qb = createQuickBetButton("$" + bet, bet);
            quickBetPanel.add(qb);
        }
        controlPanel.add(quickBetPanel);
        controlPanel.add(Box.createVerticalStrut(15));

        // Weather selection
        JLabel weatherTitleLabel = createStyledLabel("Weather:", 14, false);
        controlPanel.add(weatherTitleLabel);

        JComboBox<String> weatherSelector = new JComboBox<>();
        for (Weather w : Weather.values()) {
            weatherSelector.addItem(w.display);
        }
        weatherSelector.setMaximumSize(new Dimension(260, 30));
        weatherSelector.addActionListener(e -> {
            currentWeather = Weather.values()[weatherSelector.getSelectedIndex()];
            racePanel.repaint();
        });
        controlPanel.add(weatherSelector);
        controlPanel.add(Box.createVerticalStrut(10));

        // Track selection
        JLabel trackTitleLabel = createStyledLabel("Track Type:", 14, false);
        controlPanel.add(trackTitleLabel);

        JComboBox<String> trackSelector = new JComboBox<>();
        for (TrackType t : TrackType.values()) {
            trackSelector.addItem(t.name);
        }
        trackSelector.setMaximumSize(new Dimension(260, 30));
        trackSelector.addActionListener(e -> {
            currentTrack = TrackType.values()[trackSelector.getSelectedIndex()];
            racePanel.repaint();
        });
        controlPanel.add(trackSelector);
        controlPanel.add(Box.createVerticalStrut(20));

        // Race buttons
        startButton = createStyledButton("🏁 START RACE 🏁", new Color(0, 150, 0));
        startButton.addActionListener(e -> startRace());
        controlPanel.add(startButton);
        controlPanel.add(Box.createVerticalStrut(10));

        resetButton = createStyledButton("🔄 NEW RACE", new Color(100, 100, 150));
        resetButton.addActionListener(e -> resetRace());
        resetButton.setEnabled(false);
        controlPanel.add(resetButton);
        controlPanel.add(Box.createVerticalStrut(20));

        // Selected horse stats
        JLabel statsTitle = createStyledLabel("Horse Statistics:", 14, true);
        controlPanel.add(statsTitle);
        controlPanel.add(Box.createVerticalStrut(5));

        // Stats panel for selected horse
        JPanel horseStatsPanel = new JPanel();
        horseStatsPanel.setLayout(new BoxLayout(horseStatsPanel, BoxLayout.Y_AXIS));
        horseStatsPanel.setOpaque(false);
        horseStatsPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        horseStatsPanel.setMaximumSize(new Dimension(260, 100));

        controlPanel.add(horseStatsPanel);
    }

    /**
     * Create the stats panel at the bottom
     */
    private void createStatsPanel() {
        statsPanel = new JPanel(new BorderLayout());
        statsPanel.setPreferredSize(new Dimension(WINDOW_WIDTH, 200));
        statsPanel.setBackground(new Color(35, 35, 55));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Race log (left)
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setOpaque(false);
        logPanel.setPreferredSize(new Dimension(500, 180));

        JLabel logTitle = createStyledLabel("📝 Race Log", 14, true);
        logPanel.add(logTitle, BorderLayout.NORTH);

        raceLog = new JTextArea();
        raceLog.setEditable(false);
        raceLog.setBackground(new Color(25, 25, 45));
        raceLog.setForeground(Color.WHITE);
        raceLog.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JScrollPane logScroll = new JScrollPane(raceLog);
        logScroll.setPreferredSize(new Dimension(480, 150));
        logPanel.add(logScroll, BorderLayout.CENTER);

        statsPanel.add(logPanel, BorderLayout.WEST);

        // Leaderboard (center)
        JPanel leaderPanel = new JPanel(new BorderLayout());
        leaderPanel.setOpaque(false);
        leaderPanel.setPreferredSize(new Dimension(300, 180));

        JLabel leaderTitle = createStyledLabel("🏆 All-Time Leaderboard", 14, true);
        leaderPanel.add(leaderTitle, BorderLayout.NORTH);

        leaderboardModel = new DefaultListModel<>();
        updateLeaderboard();

        leaderboardList = new JList<>(leaderboardModel);
        leaderboardList.setBackground(new Color(25, 25, 45));
        leaderboardList.setForeground(Color.WHITE);
        leaderboardList.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JScrollPane leaderScroll = new JScrollPane(leaderboardList);
        leaderPanel.add(leaderScroll, BorderLayout.CENTER);

        statsPanel.add(leaderPanel, BorderLayout.CENTER);

        // Horse stats table (right)
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);
        tablePanel.setPreferredSize(new Dimension(380, 180));

        JLabel tableTitle = createStyledLabel("📊 Horse Performance", 14, true);
        tablePanel.add(tableTitle, BorderLayout.NORTH);

        String[] columns = {"Horse", "Races", "Wins", "Win%", "Odds"};
        Object[][] data = new Object[NUM_HORSES][5];

        for (int i = 0; i < NUM_HORSES; i++) {
            Horse h = horses.get(i);
            data[i] = new Object[]{
                    h.name,
                    h.races,
                    h.wins,
                    String.format("%.1f%%", h.getWinRate()),
                    String.format("%.1fx", h.getOdds())
            };
        }

        JTable horseTable = new JTable(data, columns);
        horseTable.setBackground(new Color(25, 25, 45));
        horseTable.setForeground(Color.WHITE);
        horseTable.setGridColor(Color.GRAY);
        horseTable.getTableHeader().setBackground(new Color(45, 45, 65));
        horseTable.getTableHeader().setForeground(Color.WHITE);

        JScrollPane tableScroll = new JScrollPane(horseTable);
        tablePanel.add(tableScroll, BorderLayout.CENTER);

        statsPanel.add(tablePanel, BorderLayout.EAST);
    }

    /**
     * Helper method to create styled labels
     */
    private JLabel createStyledLabel(String text, int fontSize, boolean bold) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", bold ? Font.BOLD : Font.PLAIN, fontSize));
        return label;
    }

    /**
     * Helper method to create styled buttons
     */
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setMaximumSize(new Dimension(260, 40));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    /**
     * Create quick bet button
     */
    private JButton createQuickBetButton(String text, int amount) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 10));
        button.setPreferredSize(new Dimension(55, 25));
        button.addActionListener(e -> {
            if (amount <= playerMoney) {
                betSpinner.setValue(amount);
            }
        });
        return button;
    }

    /**
     * Update horse stats display
     */
    private void updateHorseStats() {
        if (selectedHorseIndex >= 0 && selectedHorseIndex < horses.size()) {
            Horse h = horses.get(selectedHorseIndex);
            logMessage("Selected: " + h.name);
            logMessage(String.format("  Speed: %.1f | Stamina: %.0f%% | Accel: %.1f",
                    h.baseSpeed, h.stamina * 100, h.acceleration));
            logMessage(String.format("  Odds: %.1fx | Win Rate: %.1f%%",
                    h.getOdds(), h.getWinRate()));
        }
    }

    /**
     * Update the leaderboard display
     */
    private void updateLeaderboard() {
        leaderboardModel.clear();

        List<Horse> sorted = new ArrayList<>(horses);
        sorted.sort((a, b) -> {
            if (b.wins != a.wins) return b.wins - a.wins;
            return Double.compare(b.getWinRate(), a.getWinRate());
        });

        for (int i = 0; i < sorted.size(); i++) {
            Horse h = sorted.get(i);
            String entry = String.format("%d. %-15s W:%d R:%d (%.0f%%)",
                    i + 1, h.name, h.wins, h.races, h.getWinRate());
            leaderboardModel.addElement(entry);
        }
    }

    /**
     * Log a message to the race log
     */
    private void logMessage(String message) {
        raceLog.append(message + "\n");
        raceLog.setCaretPosition(raceLog.getDocument().getLength());
    }

    /**
     * Start the race
     */
    private void startRace() {
        if (raceInProgress) return;

        // Validate bet
        currentBet = (Integer) betSpinner.getValue();
        if (currentBet > playerMoney) {
            JOptionPane.showMessageDialog(this,
                    "You don't have enough money for this bet!",
                    "Insufficient Funds", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedHorseIndex < 0 && currentBet > 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a horse to bet on!",
                    "No Horse Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Deduct bet
        if (currentBet > 0) {
            playerMoney -= currentBet;
            updateMoneyDisplay();
            logMessage("=".repeat(40));
            logMessage("Bet placed: $" + currentBet + " on " +
                    horses.get(selectedHorseIndex).name);
        }

        // Reset horses
        for (Horse h : horses) {
            h.reset();
        }
        finishOrder.clear();

        // Update UI
        raceInProgress = true;
        raceFinished = false;
        startButton.setEnabled(false);
        resetButton.setEnabled(false);
        horseSelector.setEnabled(false);
        betSpinner.setEnabled(false);

        raceStartTime = System.currentTimeMillis();
        logMessage("🏁 Race #" + (raceNumber + 1) + " has started!");
        logMessage("Weather: " + currentWeather.display + " | Track: " + currentTrack.name);

        // Start animation timer
        animationTimer = new Timer();
        animationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateRace();
            }
        }, 0, ANIMATION_DELAY);
    }

    /**
     * Update race state - called by animation timer
     */
    private void updateRace() {
        boolean allFinished = true;

        for (Horse h : horses) {
            if (!h.finished) {
                allFinished = false;
                h.update(currentWeather.speedModifier, currentTrack.speedModifier);

                if (h.finished) {
                    finishOrder.add(h);
                    SwingUtilities.invokeLater(() -> {
                        logMessage(finishOrder.size() + ". " + h.name +
                                " finished! (" + String.format("%.2fs", h.finishTime / 1000.0) + ")");
                    });
                }
            }
        }

        // Update display
        SwingUtilities.invokeLater(() -> racePanel.repaint());

        if (allFinished) {
            endRace();
        }
    }

    /**
     * End the race and calculate results
     */
    private void endRace() {
        animationTimer.cancel();
        raceInProgress = false;
        raceFinished = true;

        // Update statistics
        for (int i = 0; i < horses.size(); i++) {
            Horse h = horses.get(i);
            h.races++;
        }

        Horse winner = finishOrder.get(0);
        winner.wins++;

        // Calculate winnings
        if (selectedHorseIndex >= 0 && currentBet > 0) {
            Horse betHorse = horses.get(selectedHorseIndex);
            int position = finishOrder.indexOf(betHorse) + 1;

            if (position == 1) {
                int winnings = (int) (currentBet * betHorse.getOdds());
                playerMoney += winnings + currentBet;
                logMessage("🎉 YOU WON! +" + winnings);
            } else if (position <= 3) {
                int winnings = (int) (currentBet * 0.5);
                playerMoney += winnings;
                logMessage("📈 Placed " + position + "! +" + winnings);
            } else {
                logMessage("😢 Your horse finished " + position + ". Better luck next time!");
            }
        }

        // Save race result
        List<String> positions = new ArrayList<>();
        for (Horse h : finishOrder) {
            positions.add(h.name);
        }
        raceHistory.add(new RaceResult(raceNumber, winner.name, winner.finishTime,
                currentWeather, currentTrack, positions));

        raceNumber++;

        // Update UI
        SwingUtilities.invokeLater(() -> {
            updateMoneyDisplay();
            updateLeaderboard();
            resetButton.setEnabled(true);
            racePanel.repaint();

            logMessage("=".repeat(40));
            logMessage("🏆 Winner: " + winner.name + "!");
            logMessage("Time: " + String.format("%.2fs", winner.finishTime / 1000.0));
        });
    }

    /**
     * Reset for a new race
     */
    private void resetRace() {
        // Reset horses
        for (Horse h : horses) {
            h.reset();
        }
        finishOrder.clear();

        // Reset state
        raceFinished = false;
        currentBet = 0;

        // Update UI
        startButton.setEnabled(true);
        resetButton.setEnabled(false);
        horseSelector.setEnabled(true);
        betSpinner.setEnabled(true);

        // Update spinner max value
        SpinnerNumberModel model = (SpinnerNumberModel) betSpinner.getModel();
        model.setMaximum(Math.max(10, playerMoney));

        // Update horse selector with new odds
        horseSelector.removeAllItems();
        horseSelector.addItem("-- Select a horse --");
        for (int i = 0; i < NUM_HORSES; i++) {
            Horse h = horses.get(i);
            horseSelector.addItem(String.format("%d. %s (%.1fx)", i + 1, h.name, h.getOdds()));
        }

        // Randomize weather occasionally
        if (Math.random() < 0.3) {
            currentWeather = Weather.values()[(int) (Math.random() * Weather.values().length)];
        }

        racePanel.repaint();
        logMessage("\n" + "=".repeat(40));
        logMessage("New race is ready! Place your bets!");

        // Check if player is out of money
        if (playerMoney < 10) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "You're running low on funds! Would you like a loan of $500?",
                    "Low Funds", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                playerMoney += 500;
                updateMoneyDisplay();
                logMessage("💰 Received $500 loan. Good luck!");
            }
        }
    }

    /**
     * Update money display
     */
    private void updateMoneyDisplay() {
        moneyLabel.setText("Balance: $" + playerMoney);
        if (playerMoney < 100) {
            moneyLabel.setForeground(Color.RED);
        } else if (playerMoney < 500) {
            moneyLabel.setForeground(Color.YELLOW);
        } else {
            moneyLabel.setForeground(Color.GREEN);
        }
    }

    /**
     * Main entry point
     */
    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }

        // Run on EDT
        SwingUtilities.invokeLater(() -> {
            new HorseRacingSimulator();
        });
    }
}