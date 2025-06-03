import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BoatRacingTournament extends JFrame {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int NUM_BOATS = 5;
    private static final int FINISH_LINE = 700;
    private static final int BOAT_WIDTH = 30;
    private static final int BOAT_HEIGHT = 15;
    private static final int LANE_HEIGHT = 80;
    private static final int OBSTACLE_SIZE = 20;
    private static final int MAX_OBSTACLES = 10;

    private RacePanel racePanel;
    private JButton startButton;
    private JButton resetButton;
    private JLabel statusLabel;
    private List<Boat> boats;
    private List<Obstacle> obstacles;
    private boolean raceStarted;
    private boolean raceFinished;
    private Timer animationTimer;
    private int tournamentRound;
    private List<String> leaderboard;

    public BoatRacingTournament() {
        setTitle("Boat Racing Tournament");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        raceStarted = false;
        raceFinished = false;
        tournamentRound = 1;
        boats = new ArrayList<>();
        obstacles = new ArrayList<>();
        leaderboard = new ArrayList<>();

        initializeComponents();
        initializeBoats();
        initializeObstacles();
        setVisible(true);
    }

    private void initializeComponents() {
        racePanel = new RacePanel();
        startButton = new JButton("Start Race");
        resetButton = new JButton("Reset Tournament");
        statusLabel = new JLabel("Round " + tournamentRound + ": Press Start to begin!");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        startButton.addActionListener(e -> startRace());
        resetButton.addActionListener(e -> resetTournament());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        buttonPanel.add(resetButton);

        setLayout(new BorderLayout());
        add(racePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);

        animationTimer = new Timer(16, e -> {
            if (raceStarted && !raceFinished) {
                updateBoats();
                checkCollisions();
                checkFinish();
                racePanel.repaint();
            }
        });
    }

    private void initializeBoats() {
        boats.clear();
        Random rand = new Random();
        for (int i = 0; i < NUM_BOATS; i++) {
            int yPos = 50 + i * LANE_HEIGHT;
            boats.add(new Boat("Boat " + (i + 1), 50, yPos, rand.nextDouble() * 2 + 1));
        }
    }

    private void initializeObstacles() {
        obstacles.clear();
        Random rand = new Random();
        for (int i = 0; i < MAX_OBSTACLES; i++) {
            int lane = rand.nextInt(NUM_BOATS);
            int x = 200 + rand.nextInt(FINISH_LINE - 200);
            int y = 50 + lane * LANE_HEIGHT + LANE_HEIGHT / 2 - OBSTACLE_SIZE / 2;
            obstacles.add(new Obstacle(x, y));
        }
    }

    private void startRace() {
        if (!raceStarted) {
            raceStarted = true;
            raceFinished = false;
            startButton.setEnabled(false);
            animationTimer.start();
            statusLabel.setText("Round " + tournamentRound + ": Race in progress...");
        }
    }

    private void resetTournament() {
        raceStarted = false;
        raceFinished = false;
        tournamentRound = 1;
        leaderboard.clear();
        initializeBoats();
        initializeObstacles();
        startButton.setEnabled(true);
        statusLabel.setText("Round " + tournamentRound + ": Press Start to begin!");
        racePanel.repaint();
    }

    private void updateBoats() {
        for (Boat boat : boats) {
            if (!boat.isFinished()) {
                boat.move();
                avoidObstacles(boat);
            }
        }
    }

    private void avoidObstacles(Boat boat) {
        for (Obstacle obstacle : obstacles) {
            if (Math.abs(boat.getX() + BOAT_WIDTH - obstacle.getX()) < 50 &&
                    Math.abs(boat.getY() - obstacle.getY()) < BOAT_HEIGHT) {
                boat.setSpeed(boat.getSpeed() * 0.8);
            }
        }
    }

    private void checkCollisions() {
        for (int i = 0; i < boats.size(); i++) {
            Boat boat1 = boats.get(i);
            if (boat1.isFinished()) continue;
            for (int j = i + 1; j < boats.size(); j++) {
                Boat boat2 = boats.get(j);
                if (boat2.isFinished()) continue;
                if (Math.abs(boat1.getX() - boat2.getX()) < BOAT_WIDTH &&
                        Math.abs(boat1.getY() - boat2.getY()) < BOAT_HEIGHT) {
                    boat1.setSpeed(boat1.getSpeed() * 0.9);
                    boat2.setSpeed(boat2.getSpeed() * 0.9);
                }
            }
        }
    }

    private void checkFinish() {
        boolean allFinished = true;
        for (Boat boat : boats) {
            if (boat.getX() >= FINISH_LINE) {
                boat.setFinished(true);
                if (!leaderboard.contains(boat.getName())) {
                    leaderboard.add(boat.getName());
                }
            } else {
                allFinished = false;
            }
        }
        if (allFinished) {
            raceFinished = true;
            animationTimer.stop();
            displayResults();
        }
    }

    private void displayResults() {
        StringBuilder result = new StringBuilder("Round " + tournamentRound + " Results:\n");
        for (int i = 0; i < leaderboard.size(); i++) {
            result.append(i + 1).append(". ").append(leaderboard.get(i)).append("\n");
        }
        tournamentRound++;
        statusLabel.setText("Round " + (tournamentRound - 1) + " finished! Starting next round...");
        if (tournamentRound <= 3) {
            JOptionPane.showMessageDialog(this, result.toString(), "Race Results", JOptionPane.INFORMATION_MESSAGE);
            prepareNextRound();
        } else {
            String finalResult = "Tournament Over!\nFinal Leaderboard:\n" + result.toString();
            JOptionPane.showMessageDialog(this, finalResult, "Tournament Results", JOptionPane.INFORMATION_MESSAGE);
            resetTournament();
        }
    }

    private void prepareNextRound() {
        raceStarted = false;
        raceFinished = false;
        initializeBoats();
        initializeObstacles();
        startButton.setEnabled(true);
        racePanel.repaint();
    }

    class Boat {
        private String name;
        private double x, y;
        private double speed;
        private boolean finished;
        private Color color;

        public Boat(String name, double x, double y, double speed) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.finished = false;
            Random rand = new Random();
            this.color = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
        }

        public String getName() { return name; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getSpeed() { return speed; }
        public boolean isFinished() { return finished; }
        public Color getColor() { return color; }

        public void setSpeed(double speed) { this.speed = speed; }
        public void setFinished(boolean finished) { this.finished = finished; }

        public void move() {
            if (!finished) {
                x += speed;
                Random rand = new Random();
                if (rand.nextDouble() < 0.05) {
                    speed += (rand.nextDouble() - 0.5) * 0.5;
                    speed = Math.max(0.5, Math.min(speed, 3.0));
                }
            }
        }
    }

    class Obstacle {
        private int x, y;

        public Obstacle(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() { return x; }
        public int getY() { return y; }
    }

    class RacePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawBackground(g);
            drawLanes(g);
            drawObstacles(g);
            drawBoats(g);
            drawFinishLine(g);
            drawLeaderboard(g);
        }

        private void drawBackground(Graphics g) {
            g.setColor(new Color(135, 206, 235));
            g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        }

        private void drawLanes(Graphics g) {
            g.setColor(Color.BLUE);
            for (int i = 0; i <= NUM_BOATS; i++) {
                int y = 50 + i * LANE_HEIGHT;
                g.drawLine(0, y, WINDOW_WIDTH, y);
            }
        }

        private void drawObstacles(Graphics g) {
            g.setColor(Color.RED);
            for (Obstacle obstacle : obstacles) {
                g.fillOval(obstacle.getX(), obstacle.getY(), OBSTACLE_SIZE, OBSTACLE_SIZE);
            }
        }

        private void drawBoats(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (Boat boat : boats) {
                g2d.setColor(boat.getColor());
                g2d.fillRect((int) boat.getX(), (int) boat.getY(), BOAT_WIDTH, BOAT_HEIGHT);
                g2d.setColor(Color.BLACK);
                g2d.drawString(boat.getName(), (int) boat.getX(), (int) boat.getY() - 5);
            }
        }

        private void drawFinishLine(Graphics g) {
            g.setColor(Color.BLACK);
            g.fillRect(FINISH_LINE, 50, 5, NUM_BOATS * LANE_HEIGHT);
            g.setColor(Color.WHITE);
            g.drawString("Finish", FINISH_LINE - 20, 40);
        }

        private void drawLeaderboard(Graphics g) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("Leaderboard:", 10, 30);
            for (int i = 0; i < leaderboard.size(); i++) {
                g.drawString((i + 1) + ". " + leaderboard.get(i), 10, 50 + i * 20);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BoatRacingTournament::new);
    }
}