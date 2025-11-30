import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;

public class HorseRacingSimulator extends JPanel implements ActionListener {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 700;
    private static final int FINISH_LINE = 1050;
    private static final int TRACK_Y = 100;
    private static final int LANE_HEIGHT = 80;
    private static final int HORSE_SIZE = 60;

    private ArrayList<Horse> horses;
    private Timer timer;
    private boolean raceStarted = false;
    private boolean raceFinished = false;
    private Horse winner = null;
    private int countdown = 3;
    private long startTime;

    private String weather = "Sunny";
    private String trackCondition = "Fast";
    private Random random = new Random();

    // Betting system
    private int playerMoney = 1000;
    private int betAmount = 0;
    private Horse betHorse = null;

    // UI Components
    private JLabel moneyLabel;
    private JLabel weatherLabel;
    private JLabel statusLabel;
    private JComboBox<Horse> horseComboBox;
    private JTextField betField;
    private JButton placeBetButton;
    private JButton startButton;

    public HorseRacingSimulator() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(34, 139, 34)); // Grass green
        setLayout(null);

        setupUI();
        initializeHorses();
        timer = new Timer(30, this); // ~33 FPS
    }

    private void setupUI() {
        // Title
        JLabel title = new JLabel("DERBY CHAMPION 2025 - Horse Racing Simulator", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        title.setBounds(0, 10, WIDTH, 40);
        add(title);

        // Money display
        moneyLabel = new JLabel("Money: $1000");
        moneyLabel.setFont(new Font("Arial", Font.BOLD, 20));
        moneyLabel.setForeground(Color.YELLOW);
        moneyLabel.setBounds(20, 60, 200, 30);
        add(moneyLabel);

        // Weather
        weatherLabel = new JLabel("Weather: Sunny | Track: Fast");
        weatherLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        weatherLabel.setForeground(Color.CYAN);
        weatherLabel.setBounds(800, 60, 400, 30);
        add(weatherLabel);

        // Status
        statusLabel = new JLabel("Place your bet and press START RACE!", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 20));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBounds(0, HEIGHT - 100, WIDTH, 40);
        add(statusLabel);

        // Betting panel
        JLabel betLabel = new JLabel("Select Horse:");
        betLabel.setForeground(Color.WHITE);
        betLabel.setBounds(50, HEIGHT - 180, 120, 30);
        add(betLabel);

        horseComboBox = new JComboBox<>();
        horseComboBox.setBounds(170, HEIGHT - 180, 200, 30);
        add(horseComboBox);

        JLabel amountLabel = new JLabel("Bet Amount:");
        amountLabel.setForeground(Color.WHITE);
        amountLabel.setBounds(390, HEIGHT - 180, 100, 30);
        add(amountLabel);

        betField = new JTextField("100");
        betField.setBounds(490, HEIGHT - 180, 100, 30);
        add(betField);

        placeBetButton = new JButton("Place Bet");
        placeBetButton.setBounds(610, HEIGHT - 180, 120, 30);
        placeBetButton.addActionListener(e -> placeBet());
        add(placeBetButton);

        startButton = new JButton("START RACE");
        startButton.setFont(new Font("Arial", Font.BOLD, 24));
        startButton.setBackground(Color.ORANGE);
        startButton.setBounds(WIDTH / 2 - 100, HEIGHT - 140, 200, 60);
        startButton.addActionListener(e -> startRace());
        add(startButton);

        randomizeWeatherAndTrack();
    }

    private void initializeHorses() {
        horses = new ArrayList<>();
        String[] names = {"Thunderbolt", "Midnight Star", "Golden Blaze", "Silver Storm", "Crimson Fury",
                "Emerald Dash", "Phantom Hoof", "Royal Charger"};
        Color[] colors = {Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN,
                Color.MAGENTA, Color.CYAN, Color.ORANGE, Color.PINK};

        for (int i = 0; i < 8; i++) {
            double speed = 2.0 + random.nextDouble() * 3.0; // 2.0 to 5.0 base speed
            double stamina = 0.7 + random.nextDouble() * 0.3;
            double acceleration = 0.05 + random.nextDouble() * 0.1;
            Horse horse = new Horse(names[i], colors[i], i, speed, stamina, acceleration);
            horses.add(horse);
            horseComboBox.addItem(horse);
        }
    }

    private void randomizeWeatherAndTrack() {
        String[] weathers = {"Sunny", "Cloudy", "Rainy", "Windy"};
        String[] conditions = {"Fast", "Good", "Soft", "Heavy"};

        weather = weathers[random.nextInt(weathers.length)];
        trackCondition = conditions[random.nextInt(conditions.length)];

        weatherLabel.setText("Weather: " + weather + " | Track: " + trackCondition);

        // Adjust horse speeds based on conditions
        double trackFactor = switch (trackCondition) {
            case "Fast" -> 1.1;
            case "Good" -> 1.0;
            case "Soft" -> 0.9;
            case "Heavy" -> 0.75;
            default -> 1.0;
        };

        for (Horse h : horses) {
            h.adjustSpeedForTrack(trackFactor);
        }
    }

    private void placeBet() {
        try {
            betAmount = Integer.parseInt(betField.getText());
            if (betAmount <= 0 || betAmount > playerMoney) {
                JOptionPane.showMessageDialog(this, "Invalid bet amount!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            betHorse = (Horse) horseComboBox.getSelectedItem();
            JOptionPane.showMessageDialog(this,
                    "Bet placed: $" + betAmount + " on " + betHorse.name,
                    "Bet Confirmed", JOptionPane.INFORMATION_MESSAGE);
            placeBetButton.setEnabled(false);
            betField.setEnabled(false);
            horseComboBox.setEnabled(false);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid number!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startRace() {
        if (betHorse == null) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "You haven't placed a bet. Start race anyway?", "No Bet",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;
        }

        raceStarted = true;
        raceFinished = false;
        winner = null;
        countdown = 3;
        startTime = System.currentTimeMillis();

        statusLabel.setText("Get Ready... " + countdown);
        startButton.setEnabled(false);

        // Reset horses
        for (Horse h : horses) {
            h.reset();
        }

        // Countdown timer
        new Timer(1000, new ActionListener() {
            int count = 3;
            public void actionPerformed(ActionEvent e) {
                count--;
                if (count > 0) {
                    statusLabel.setText("Race starts in... " + count + "!");
                } else if (count == 0) {
                    statusLabel.setText("GO!!!");
                } else {
                    ((Timer)e.getSource()).stop();
                    timer.start();
                    statusLabel.setText("RACE IN PROGRESS...");
                }
            }
        }).start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!raceFinished) {
            boolean allFinished = true;
            for (Horse horse : horses) {
                horse.update();
                if (horse.x >= FINISH_LINE && !horse.finished) {
                    horse.finished = true;
                    if (winner == null) {
                        winner = horse;
                    }
                }
                if (!horse.finished) allFinished = false;
            }

            if (allFinished) {
                endRace();
            }
            repaint();
        }
    }

    private void endRace() {
        timer.stop();
        raceFinished = true;

        String result = "WINNER: " + winner.name + "!";
        statusLabel.setText(result);
        statusLabel.setForeground(Color.black);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 32));

        // Payout
        if (betHorse != null && betHorse == winner) {
            int payout = (int)(betAmount * (3.0 + random.nextDouble() * 7.0)); // Odds 3x to 10x
            playerMoney += payout;
            JOptionPane.showMessageDialog(this,
                    "YOU WON! " + winner.name + " came first!\nPayout: $" + payout,
                    "Congratulations!", JOptionPane.INFORMATION_MESSAGE);
        } else if (betHorse != null) {
            playerMoney -= betAmount;
            JOptionPane.showMessageDialog(this,
                    "You lost. Winner was " + winner.name,
                    "Better luck next time", JOptionPane.WARNING_MESSAGE);
        }

        moneyLabel.setText("Money: $" + playerMoney);
        resetForNextRace();
    }

    private void resetForNextRace() {
        startButton.setEnabled(true);
        placeBetButton.setEnabled(true);
        betField.setEnabled(true);
        horseComboBox.setEnabled(true);
        betHorse = null;
        betAmount = 0;
        randomizeWeatherAndTrack();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw track
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect(0, TRACK_Y - 20, WIDTH, LANE_HEIGHT * 8 + 40);

        // Draw lanes
        g2d.setColor(Color.WHITE);
        for (int i = 0; i <= 8; i++) {
            int y = TRACK_Y + i * LANE_HEIGHT;
            g2d.drawLine(0, y, WIDTH, y);
            // Lane number
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Lane " + (i + 1), 10, y - 30);
        }

        // Finish line
        g2d.setStroke(new BasicStroke(5));
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < HEIGHT; i += 20) {
            g2d.drawLine(FINISH_LINE, i, FINISH_LINE, i + 10);
        }
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.drawString("FINISH", FINISH_LINE + 20, 100);

        // Draw horses
        for (Horse horse : horses) {
            horse.draw(g2d);
        }

        // Starting gate
        if (!raceStarted || countdown > 0) {
            g2d.setColor(new Color(100, 100, 100, 200));
            g2d.fillRect(50, TRACK_Y - 20, 100, LANE_HEIGHT * 8 + 40);
        }
    }

    // ==================== HORSE CLASS ====================
    class Horse {
        String name;
        Color color;
        int lane;
        double baseSpeed;
        double currentSpeed;
        double stamina;
        double acceleration;
        double x = 80;
        boolean finished = false;

        public Horse(String name, Color color, int lane, double speed, double stamina, double accel) {
            this.name = name;
            this.color = color;
            this.lane = lane;
            this.baseSpeed = speed;
            this.currentSpeed = 0;
            this.stamina = stamina;
            this.acceleration = accel;
        }

        public void adjustSpeedForTrack(double factor) {
            baseSpeed *= factor;
        }

        public void reset() {
            x = 80;
            currentSpeed = 0;
            finished = false;
        }

        public void update() {
            if (finished) return;

            // Acceleration phase
            if (currentSpeed < baseSpeed) {
                currentSpeed += acceleration * (1.2 - stamina); // Slower accel if high stamina
            }

            // Random burst
            if (random.nextDouble() < 0.02) {
                currentSpeed += random.nextDouble() * 2.0;
            }

            // Fatigue after 700px
            if (x > 700 && random.nextDouble() < 0.03) {
                currentSpeed *= 0.9;
            }

            x += currentSpeed;

            // Weather effect
            if (weather.equals("Rainy") && random.nextDouble() < 0.01) {
                x -= 2; // Slip
            }
        }

        public void draw(Graphics2D g2d) {
            int y = TRACK_Y + lane * LANE_HEIGHT + 10;

            // Body
            g2d.setColor(color);
            g2d.fillRoundRect((int)x, y, 80, 40, 20, 20);

            // Head
            g2d.setColor(color.darker());
            g2d.fillOval((int)x + 70, y + 5, 30, 30);

            // Legs animation
            double legPhase = System.currentTimeMillis() / 100.0 % 4;
            int legOffset = (int)(Math.sin(legPhase) * 10);
            g2d.setColor(Color.BLACK);
            g2d.fillRect((int)x + 20 + legOffset, y + 35, 8, 20);
            g2d.fillRect((int)x + 50 - legOffset, y + 35, 8, 20);

            // Name tag
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString(name, (int)x + 10, y - 5);

            // Speed bar
            g2d.setColor(Color.GRAY);
            g2d.fillRect((int)x, y - 20, 80, 8);
            g2d.setColor(Color.GREEN);
            int speedWidth = (int)(currentSpeed / 8.0 * 80);
            g2d.fillRect((int)x, y - 20, speedWidth, 8);
        }

        @Override
        public String toString() {
            return name + " (Lane " + (lane + 1) + ")";
        }
    }

    // ==================== MAIN ====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Horse Racing Simulator 2025");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            HorseRacingSimulator game = new HorseRacingSimulator();
            frame.add(game);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}