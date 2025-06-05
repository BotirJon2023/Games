import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class BoatRacingTournament3 {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new BoatRacingGame().setVisible(true);
        });
    }
}

class BoatRacingGame extends JFrame {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;
    private static final int WATER_LEVEL = 400;
    private static final int NUM_BOATS = 5;
    private static final int NUM_ROUNDS = 3;

    private GamePanel gamePanel;
    private int currentRound = 1;
    private Map<String, Integer> scores = new HashMap<>();

    public BoatRacingGame() {
        setTitle("Boat Racing Tournament");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel(WIDTH, HEIGHT, WATER_LEVEL, NUM_BOATS);
        add(gamePanel);

        initializeScores();

        JButton startButton = new JButton("Start Race");
        startButton.addActionListener(e -> startRace());

        JPanel controlPanel = new JPanel();
        controlPanel.add(startButton);
        add(controlPanel, BorderLayout.SOUTH);
    }

    private void initializeScores() {
        for (int i = 1; i <= NUM_BOATS; i++) {
            scores.put("Boat " + i, 0);
        }
    }

    private void startRace() {
        if (currentRound > NUM_ROUNDS) {
            JOptionPane.showMessageDialog(this, "Tournament completed!\n" + getFinalResults());
            currentRound = 1;
            initializeScores();
        } else {
            gamePanel.startNewRace(currentRound);
            currentRound++;
        }
    }

    private String getFinalResults() {
        StringBuilder sb = new StringBuilder("Final Results:\n");
        scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(" points\n"));
        return sb.toString();
    }

    public void updateScores(String boatName, int position) {
        int points = scores.get(boatName) + (NUM_BOATS - position + 1);
        scores.put(boatName, points);
    }

    class GamePanel extends JPanel implements ActionListener {
        private final int width;
        private final int height;
        private final int waterLevel;
        private final int numBoats;

        private List<Boat> boats = new ArrayList<>();
        private List<Wave> waves = new ArrayList<>();
        private Timer timer;
        private boolean raceInProgress = false;
        private int raceDistance;
        private int round;

        public GamePanel(int width, int height, int waterLevel, int numBoats) {
            this.width = width;
            this.height = height;
            this.waterLevel = waterLevel;
            this.numBoats = numBoats;

            setPreferredSize(new Dimension(width, height));
            setBackground(new Color(135, 206, 250)); // Sky blue

            // Initialize waves
            for (int i = 0; i < 50; i++) {
                waves.add(new Wave(
                        (int)(Math.random() * width),
                        waterLevel + (int)(Math.random() * 30) - 15,
                        (int)(Math.random() * 5) + 1
                ));
            }

            // Initialize boats
            for (int i = 0; i < numBoats; i++) {
                boats.add(new Boat(
                        50,
                        waterLevel - 30 - (i * 10),
                        60, 30,
                        "Boat " + (i + 1),
                        new Color(
                                (int)(Math.random() * 200),
                                (int)(Math.random() * 200),
                                (int)(Math.random() * 200)
                        ),
                        (int)(Math.random() * 3) + 1
                ));
            }

            timer = new Timer(30, this);
            timer.start();
        }

        public void startNewRace(int round) {
            this.round = round;
            raceDistance = width - 200;
            raceInProgress = true;

            // Reset boat positions
            for (int i = 0; i < boats.size(); i++) {
                Boat boat = boats.get(i);
                boat.reset();
                boat.setY(waterLevel - 30 - (i * 10));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Draw sky
            g.setColor(new Color(135, 206, 250));
            g.fillRect(0, 0, width, waterLevel);

            // Draw water
            g.setColor(new Color(0, 105, 148));
            g.fillRect(0, waterLevel, width, height - waterLevel);

            // Draw waves
            g.setColor(new Color(100, 149, 237));
            for (Wave wave : waves) {
                g.fillOval(wave.x - 5, wave.y - 2, 10, 4);
            }

            // Draw finish line
            if (raceInProgress) {
                g.setColor(Color.RED);
                g.drawLine(raceDistance, waterLevel, raceDistance, height);
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString("FINISH", raceDistance - 30, waterLevel + 20);
            }

            // Draw boats
            for (Boat boat : boats) {
                boat.draw(g);
            }

            // Draw race info
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("Round: " + round + "/" + NUM_ROUNDS, 20, 30);
            g.drawString(raceInProgress ? "Race in progress!" : "Press Start Race", 20, 60);

            // Draw standings if race is over
            if (!raceInProgress && round > 1) {
                drawStandings(g);
            }
        }

        private void drawStandings(Graphics g) {
            g.setColor(new Color(255, 255, 255, 200));
            g.fillRect(width - 250, 50, 200, 100 + boats.size() * 20);

            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("Tournament Standings", width - 240, 70);

            List<Boat> sortedBoats = new ArrayList<>(boats);
            sortedBoats.sort(Comparator.comparingInt(Boat::getDistanceTraveled).reversed());

            for (int i = 0; i < sortedBoats.size(); i++) {
                Boat boat = sortedBoats.get(i);
                g.drawString((i + 1) + ". " + boat.getName() + ": " +
                        scores.get(boat.getName()) + " pts", width - 240, 100 + i * 20);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (raceInProgress) {
                // Update waves
                for (Wave wave : waves) {
                    wave.move();
                    if (wave.x > width) {
                        wave.x = 0;
                        wave.y = waterLevel + (int)(Math.random() * 30) - 15;
                    }
                }

                // Move boats
                boolean raceFinished = true;
                List<Boat> finishedBoats = new ArrayList<>();

                for (Boat boat : boats) {
                    if (boat.getDistanceTraveled() < raceDistance) {
                        boat.move();
                        raceFinished = false;
                    } else if (!boat.hasFinished()) {
                        boat.setFinished(true);
                        finishedBoats.add(boat);
                    }
                }

                // Check for collisions
                checkCollisions();

                // Update scores for finished boats
                if (!finishedBoats.isEmpty()) {
                    int position = boats.size() - finishedBoats.size() + 1;
                    for (Boat boat : finishedBoats) {
                        updateScores(boat.getName(), position);
                        position++;
                    }
                }

                if (raceFinished) {
                    raceInProgress = false;
                }
            }

            repaint();
        }

        private void checkCollisions() {
            for (int i = 0; i < boats.size(); i++) {
                for (int j = i + 1; j < boats.size(); j++) {
                    Boat boat1 = boats.get(i);
                    Boat boat2 = boats.get(j);

                    if (boat1.collidesWith(boat2)) {
                        // Simple collision response - bounce apart
                        if (boat1.getX() < boat2.getX()) {
                            boat1.setX(boat1.getX() - 5);
                            boat2.setX(boat2.getX() + 5);
                        } else {
                            boat1.setX(boat1.getX() + 5);
                            boat2.setX(boat2.getX() - 5);
                        }

                        // Random speed change after collision
                        boat1.setSpeed(boat1.getSpeed() + (Math.random() > 0.5 ? 0.5 : -0.5));
                        boat2.setSpeed(boat2.getSpeed() + (Math.random() > 0.5 ? 0.5 : -0.5));

                        // Ensure speed stays within bounds
                        boat1.setSpeed(Math.max(1, Math.min(5, boat1.getSpeed())));
                        boat2.setSpeed(Math.max(1, Math.min(5, boat2.getSpeed())));
                    }
                }
            }
        }
    }
}

class Boat {
    private int x, y;
    private int width, height;
    private String name;
    private Color color;
    private double speed;
    private int distanceTraveled;
    private boolean finished;

    public Boat(int x, int y, int width, int height, String name, Color color, double speed) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.name = name;
        this.color = color;
        this.speed = speed;
        this.distanceTraveled = 0;
        this.finished = false;
    }

    public void draw(Graphics g) {
        // Boat hull
        g.setColor(color);
        g.fillRoundRect(x, y, width, height / 2, 10, 10);

        // Boat mast
        g.setColor(Color.BLACK);
        g.fillRect(x + width / 3, y - height, 3, height);

        // Boat sail
        g.setColor(Color.WHITE);
        Polygon sail = new Polygon();
        sail.addPoint(x + width / 3, y - height / 2);
        sail.addPoint(x + width / 3, y - height);
        sail.addPoint(x + width / 2 + 10, y - height / 2);
        g.fillPolygon(sail);

        // Boat name
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString(name, x + 5, y + height / 2 - 5);

        // Speed indicator
        g.setFont(new Font("Arial", Font.PLAIN, 8));
        g.drawString(String.format("%.1f", speed) + " m/s", x, y - 5);
    }

    public void move() {
        double actualSpeed = speed * (0.8 + Math.random() * 0.4); // Add some randomness
        x += actualSpeed;
        distanceTraveled += actualSpeed;

        // Add some vertical movement for wave effect
        y += (int)(Math.random() * 3) - 1;
    }

    public boolean collidesWith(Boat other) {
        return x < other.x + other.width &&
                x + width > other.x &&
                y < other.y + other.height &&
                y + height > other.y;
    }

    public void reset() {
        x = 50;
        distanceTraveled = 0;
        finished = false;
    }

    // Getters and setters
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public String getName() { return name; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public int getDistanceTraveled() { return distanceTraveled; }
    public boolean hasFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
}

class Wave {
    public int x, y;
    private int speed;

    public Wave(int x, int y, int speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
    }

    public void move() {
        x += speed;
    }
}