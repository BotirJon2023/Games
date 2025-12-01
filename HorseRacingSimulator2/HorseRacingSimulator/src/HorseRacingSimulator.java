import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class HorseRacingSimulator extends JFrame {
    private static final int TRACK_LENGTH = 800;
    private static final int NUM_HORSES = 6;
    private static final int HORSE_HEIGHT = 60;
    private static final int FINISH_LINE = TRACK_LENGTH - 50;

    private RaceTrackPanel trackPanel;
    private ControlPanel controlPanel;
    private RaceManager raceManager;
    private JTextArea infoArea;

    public HorseRacingSimulator() {
        initializeGame();
        setupUI();
    }

    private void initializeGame() {
        setTitle("Horse Racing Simulator Pro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        raceManager = new RaceManager();
        trackPanel = new RaceTrackPanel();
        controlPanel = new ControlPanel();
        infoArea = new JTextArea(8, 30);

        add(trackPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        JScrollPane scrollPane = new JScrollPane(infoArea);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        add(scrollPane, BorderLayout.EAST);

        pack();
        setSize(1200, 700);
        setLocationRelativeTo(null);
    }

    private void setupUI() {
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        updateInfo("Welcome to Horse Racing Simulator!\nPlace your bets and start the race!");
    }

    private void updateInfo(String message) {
        infoArea.append(message + "\n");
        infoArea.setCaretPosition(infoArea.getDocument().getLength());
    }

    // Inner class for race track visualization
    class RaceTrackPanel extends JPanel {
        private Image trackBackground;
        private Image finishLineImg;
        private Map<String, Image> horseImages;

        public RaceTrackPanel() {
            setPreferredSize(new Dimension(TRACK_LENGTH + 100, NUM_HORSES * HORSE_HEIGHT + 100));
            setBackground(new Color(34, 139, 34)); // Forest green
            loadImages();
        }

        private void loadImages() {
            horseImages = new HashMap<>();
            // Create simple horse images programmatically
            for (int i = 1; i <= NUM_HORSES; i++) {
                horseImages.put("Horse" + i, createHorseImage(i));
            }

            // Create track and finish line images
            trackBackground = createTrackBackground();
            finishLineImg = createFinishLineImage();
        }

        private Image createHorseImage(int horseNumber) {
            int width = 80;
            int height = 50;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();

            // Horse body color based on number
            Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN};
            Color horseColor = colors[(horseNumber - 1) % colors.length];

            // Draw horse
            g2d.setColor(horseColor);
            g2d.fillRect(10, 15, 40, 20); // Body
            g2d.fillRect(45, 10, 15, 10); // Neck
            g2d.fillRect(55, 5, 10, 15); // Head
            g2d.fillRect(15, 35, 5, 15); // Leg 1
            g2d.fillRect(25, 35, 5, 15); // Leg 2
            g2d.fillRect(35, 35, 5, 15); // Leg 3
            g2d.fillRect(45, 35, 5, 15); // Leg 4

            // Draw saddle
            g2d.setColor(Color.BLACK);
            g2d.fillRect(20, 15, 20, 10);

            // Draw number
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString(String.valueOf(horseNumber), 60, 15);

            g2d.dispose();
            return img;
        }

        private Image createTrackBackground() {
            BufferedImage img = new BufferedImage(TRACK_LENGTH + 100, NUM_HORSES * HORSE_HEIGHT + 100,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = img.createGraphics();

            // Draw grass
            g2d.setColor(new Color(34, 139, 34));
            g2d.fillRect(0, 0, img.getWidth(), img.getHeight());

            // Draw track lanes
            for (int i = 0; i < NUM_HORSES; i++) {
                int y = 50 + i * HORSE_HEIGHT;
                g2d.setColor(new Color(210, 180, 140)); // Tan color for track
                g2d.fillRect(0, y, TRACK_LENGTH, 40);

                // Lane markings
                g2d.setColor(Color.WHITE);
                g2d.drawLine(0, y, TRACK_LENGTH, y);
                g2d.drawLine(0, y + 40, TRACK_LENGTH, y + 40);

                // Lane numbers
                g2d.drawString("Lane " + (i + 1), 10, y + 25);
            }

            g2d.dispose();
            return img;
        }

        private Image createFinishLineImage() {
            BufferedImage img = new BufferedImage(10, NUM_HORSES * HORSE_HEIGHT + 100, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = img.createGraphics();

            // Create checkered flag pattern
            boolean black = true;
            for (int y = 0; y < img.getHeight(); y += 10) {
                for (int x = 0; x < img.getWidth(); x += 10) {
                    g2d.setColor(black ? Color.BLACK : Color.WHITE);
                    g2d.fillRect(x, y, 10, 10);
                    black = !black;
                }
                black = !black;
            }

            g2d.dispose();
            return img;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Draw track background
            g.drawImage(trackBackground, 0, 0, this);

            // Draw finish line
            g.drawImage(finishLineImg, FINISH_LINE, 0, this);

            // Draw horses
            List<Horse> horses = raceManager.getHorses();
            for (int i = 0; i < horses.size(); i++) {
                Horse horse = horses.get(i);
                Image horseImg = horseImages.get(horse.getName());
                int y = 50 + i * HORSE_HEIGHT;
                g.drawImage(horseImg, horse.getPosition(), y, this);

                // Draw horse info
                g.setColor(Color.WHITE);
                g.drawString(horse.getName() + " (" + horse.getSpeed() + ")",
                        horse.getPosition() + 85, y + 25);
            }

            // Draw race status
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("Race Status: " + raceManager.getRaceStatus(), 20, 30);

            if (raceManager.isRaceFinished()) {
                g.setColor(Color.RED);
                g.setFont(new Font("Arial", Font.BOLD, 24));
                g.drawString("RACE FINISHED!", TRACK_LENGTH / 2 - 80, 30);

                // Display winner
                Horse winner = raceManager.getWinner();
                if (winner != null) {
                    g.drawString("WINNER: " + winner.getName(), TRACK_LENGTH / 2 - 60, 60);
                }
            }
        }
    }

    // Inner class for control panel
    class ControlPanel extends JPanel {
        private JButton startButton, resetButton, betButton;
        private JComboBox<String> horseComboBox;
        private JSpinner betAmountSpinner;
        private JLabel balanceLabel;

        public ControlPanel() {
            setLayout(new FlowLayout());
            setupComponents();
            setupEventListeners();
        }

        private void setupComponents() {
            // Start button
            startButton = new JButton("Start Race");
            startButton.setBackground(Color.GREEN);

            // Reset button
            resetButton = new JButton("Reset Race");
            resetButton.setBackground(Color.ORANGE);

            // Betting controls
            betButton = new JButton("Place Bet");
            betButton.setBackground(Color.CYAN);

            String[] horseNames = new String[NUM_HORSES];
            for (int i = 0; i < NUM_HORSES; i++) {
                horseNames[i] = "Horse" + (i + 1);
            }
            horseComboBox = new JComboBox<>(horseNames);

            betAmountSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 10));
            balanceLabel = new JLabel("Balance: $" + raceManager.getPlayerBalance());

            add(new JLabel("Select Horse:"));
            add(horseComboBox);
            add(new JLabel("Bet Amount: $"));
            add(betAmountSpinner);
            add(betButton);
            add(startButton);
            add(resetButton);
            add(balanceLabel);
        }

        private void setupEventListeners() {
            startButton.addActionListener(e -> startRace());
            resetButton.addActionListener(e -> resetRace());
            betButton.addActionListener(e -> placeBet());
        }

        private void startRace() {
            if (!raceManager.isRaceInProgress()) {
                new Thread(() -> {
                    raceManager.startRace();
                    while (!raceManager.isRaceFinished()) {
                        raceManager.updateRace();
                        trackPanel.repaint();
                        updateRaceInfo();
                        try {
                            Thread.sleep(50); // Animation speed
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    showRaceResults();
                }).start();
            }
        }

        private void resetRace() {
            raceManager.resetRace();
            trackPanel.repaint();
            updateInfo("Race reset! Place your bets and start again.");
            updateBalance();
        }

        private void placeBet() {
            String selectedHorse = (String) horseComboBox.getSelectedItem();
            int betAmount = (Integer) betAmountSpinner.getValue();

            if (raceManager.placeBet(selectedHorse, betAmount)) {
                updateInfo("Bet placed: $" + betAmount + " on " + selectedHorse);
                updateBalance();
            } else {
                updateInfo("Cannot place bet: " + (raceManager.isRaceInProgress() ?
                        "Race in progress" : "Insufficient funds"));
            }
        }

        private void updateBalance() {
            balanceLabel.setText("Balance: $" + raceManager.getPlayerBalance());
        }

        private void updateRaceInfo() {
            // Update race progress in info area periodically
            if (raceManager.getRaceTime() % 10 == 0) {
                List<Horse> horses = raceManager.getHorses();
                StringBuilder sb = new StringBuilder();
                sb.append("Race Progress (Time: ").append(raceManager.getRaceTime()).append("s):\n");
                for (Horse horse : horses) {
                    sb.append(String.format("%s: %d/%d (%.1f%%)\n",
                            horse.getName(), horse.getPosition(), FINISH_LINE,
                            (horse.getPosition() * 100.0 / FINISH_LINE)));
                }
                updateInfo(sb.toString());
            }
        }

        private void showRaceResults() {
            Horse winner = raceManager.getWinner();
            if (winner != null) {
                updateInfo("\n=== RACE RESULTS ===");
                updateInfo("WINNER: " + winner.getName() + "!");
                updateInfo("Winning Time: " + String.format("%.2f", winner.getFinishTime()) + " seconds");

                int winnings = raceManager.calculateWinnings();
                if (winnings > 0) {
                    updateInfo("Congratulations! You won $" + winnings + "!");
                } else {
                    updateInfo("Better luck next time!");
                }
                updateBalance();

                // Show standings
                updateInfo("\nFinal Standings:");
                List<Horse> standings = raceManager.getStandings();
                for (int i = 0; i < standings.size(); i++) {
                    Horse horse = standings.get(i);
                    updateInfo((i + 1) + ". " + horse.getName() +
                            " - " + String.format("%.2f", horse.getFinishTime()) + "s");
                }
            }
        }
    }

    // Horse class representing each racing horse
    static class Horse {
        private String name;
        private int speed; // Base speed (1-10)
        private int stamina; // Stamina affecting performance
        private int position;
        private double finishTime;
        private boolean finished;
        private Random random;
        private int consistency; // How consistent the horse performs

        public Horse(String name, int speed, int stamina, int consistency) {
            this.name = name;
            this.speed = speed;
            this.stamina = stamina;
            this.consistency = consistency;
            this.position = 0;
            this.finished = false;
            this.random = new Random();
        }

        public void move() {
            if (!finished) {
                // Calculate movement with randomness and stamina factor
                double staminaFactor = 1.0 - (position / (double)FINISH_LINE) * (1.0 - stamina / 100.0);
                double consistencyFactor = 1.0 - (random.nextDouble() * (10 - consistency) / 50.0);
                int moveDistance = (int)(speed * staminaFactor * consistencyFactor * 2);

                position += moveDistance;

                if (position >= FINISH_LINE) {
                    position = FINISH_LINE;
                    finished = true;
                }
            }
        }

        // Getters
        public String getName() { return name; }
        public int getSpeed() { return speed; }
        public int getPosition() { return position; }
        public double getFinishTime() { return finishTime; }
        public boolean isFinished() { return finished; }

        public void setFinishTime(double time) { finishTime = time; }
        public void reset() {
            position = 0;
            finished = false;
            finishTime = 0;
        }
    }

    // Main race management class
    class RaceManager {
        private List<Horse> horses;
        private boolean raceInProgress;
        private boolean raceFinished;
        private int raceTime;
        private Horse winner;
        private int playerBalance;
        private String betHorse;
        private int betAmount;
        private Random random;

        public RaceManager() {
            random = new Random();
            playerBalance = 1000; // Starting balance
            initializeHorses();
            resetRace();
        }

        private void initializeHorses() {
            horses = new ArrayList<>();
            String[] names = {"Thunder Bolt", "Midnight Runner", "Golden Spirit",
                    "Silver Arrow", "Red Comet", "Ocean Wave"};

            for (int i = 0; i < NUM_HORSES; i++) {
                int speed = 5 + random.nextInt(6); // Speed between 5-10
                int stamina = 70 + random.nextInt(31); // Stamina between 70-100
                int consistency = 5 + random.nextInt(6); // Consistency between 5-10
                horses.add(new Horse(names[i], speed, stamina, consistency));
            }
        }

        public void startRace() {
            if (!raceInProgress && !raceFinished) {
                raceInProgress = true;
                raceFinished = false;
                raceTime = 0;
                winner = null;
                updateInfo("Race started! Good luck!");
            }
        }

        public void updateRace() {
            if (raceInProgress && !raceFinished) {
                raceTime++;

                // Move each horse
                for (Horse horse : horses) {
                    if (!horse.isFinished()) {
                        horse.move();

                        // Check if this horse finished
                        if (horse.isFinished() && horse.getPosition() >= FINISH_LINE) {
                            horse.setFinishTime(raceTime + random.nextDouble());
                            if (winner == null) {
                                winner = horse;
                                updateInfo(horse.getName() + " has finished first!");
                            }
                        }
                    }
                }

                // Check if all horses finished
                if (horses.stream().allMatch(Horse::isFinished)) {
                    finishRace();
                }
            }
        }

        private void finishRace() {
            raceInProgress = false;
            raceFinished = true;
            updateInfo("Race completed!");
        }

        public void resetRace() {
            raceInProgress = false;
            raceFinished = false;
            raceTime = 0;
            winner = null;
            betHorse = null;
            betAmount = 0;

            for (Horse horse : horses) {
                horse.reset();
            }
        }

        public boolean placeBet(String horseName, int amount) {
            if (raceInProgress || raceFinished) {
                return false;
            }

            if (amount <= playerBalance) {
                betHorse = horseName;
                betAmount = amount;
                playerBalance -= amount;
                return true;
            }
            return false;
        }

        public int calculateWinnings() {
            if (raceFinished && betHorse != null && winner != null &&
                    betHorse.equals(winner.getName())) {
                int winnings = betAmount * 2; // 2:1 payout
                playerBalance += winnings;
                return winnings;
            }
            return 0;
        }

        public List<Horse> getStandings() {
            List<Horse> standings = new ArrayList<>(horses);
            standings.sort(Comparator.comparingDouble(Horse::getFinishTime));
            return standings;
        }

        // Getters
        public List<Horse> getHorses() { return horses; }
        public boolean isRaceInProgress() { return raceInProgress; }
        public boolean isRaceFinished() { return raceFinished; }
        public int getRaceTime() { return raceTime; }
        public Horse getWinner() { return winner; }
        public int getPlayerBalance() { return playerBalance; }

        public String getRaceStatus() {
            if (raceInProgress) return "IN PROGRESS";
            if (raceFinished) return "FINISHED";
            return "READY";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            HorseRacingSimulator game = new HorseRacingSimulator();
            game.setVisible(true);
        });
    }
}