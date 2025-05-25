import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class BowlingLeagueSimulator2 {
    private static final int NUM_TEAMS = 4;
    private static final int PLAYERS_PER_TEAM = 4;
    private static final int NUM_WEEKS = 10;

    private List<Team> teams = new ArrayList<>();
    private int currentWeek = 0;
    private boolean simulationRunning = false;

    private JFrame frame;
    private SimulationPanel simulationPanel;
    private JTextArea outputArea;
    private JButton startButton;
    private JButton nextWeekButton;
    private JButton completeSeasonButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BowlingLeagueSimulator2().initialize());
    }

    private void initialize() {
        createTeams();
        setupGUI();
    }

    private void createTeams() {
        String[] teamNames = {"Strikers", "Spare Masters", "Pin Crushers", "Gutter Gang"};
        String[][] playerNames = {
                {"Alice", "Bob", "Charlie", "Dana"},
                {"Eve", "Frank", "Grace", "Hank"},
                {"Ivy", "Jack", "Kara", "Leo"},
                {"Mona", "Neil", "Olivia", "Paul"}
        };

        for (int i = 0; i < NUM_TEAMS; i++) {
            Team team = new Team(teamNames[i]);
            for (int j = 0; j < PLAYERS_PER_TEAM; j++) {
                team.addPlayer(new Player(playerNames[i][j], 130 + (int)(Math.random() * 40)));
            }
            teams.add(team);
        }
    }

    private void setupGUI() {
        frame = new JFrame("Bowling League Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLayout(new BorderLayout());

        simulationPanel = new SimulationPanel();
        frame.add(simulationPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        startButton = new JButton("Start Simulation");
        nextWeekButton = new JButton("Simulate Next Week");
        completeSeasonButton = new JButton("Complete Entire Season");

        startButton.addActionListener(e -> startSimulation());
        nextWeekButton.addActionListener(e -> simulateNextWeek());
        completeSeasonButton.addActionListener(e -> completeSeason());

        nextWeekButton.setEnabled(false);
        completeSeasonButton.setEnabled(false);

        controlPanel.add(startButton);
        controlPanel.add(nextWeekButton);
        controlPanel.add(completeSeasonButton);

        outputArea = new JTextArea(10, 80);
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(controlPanel, BorderLayout.NORTH);
        southPanel.add(scrollPane, BorderLayout.CENTER);

        frame.add(southPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void startSimulation() {
        simulationRunning = true;
        startButton.setEnabled(false);
        nextWeekButton.setEnabled(true);
        completeSeasonButton.setEnabled(true);
        outputArea.append("=== Bowling League Simulation Started ===\n");
        outputArea.append("League consists of " + NUM_TEAMS + " teams with " + PLAYERS_PER_TEAM + " players each.\n");
        outputArea.append("Season will run for " + NUM_WEEKS + " weeks.\n\n");

        displayTeamStandings();
        simulationPanel.repaint();
    }

    private void simulateNextWeek() {
        if (!simulationRunning || currentWeek >= NUM_WEEKS) return;

        currentWeek++;
        outputArea.append("\n=== Week " + currentWeek + " Results ===\n");

        // Simulate matches
        for (int i = 0; i < teams.size(); i += 2) {
            Team team1 = teams.get(i);
            Team team2 = teams.get(i+1);
            simulateMatch(team1, team2);
        }

        // Update standings
        Collections.sort(teams, (t1, t2) -> Integer.compare(t2.getTotalPoints(), t1.getTotalPoints()));

        displayWeeklyResults();
        displayTeamStandings();

        if (currentWeek >= NUM_WEEKS) {
            endSeason();
        }

        simulationPanel.repaint();
    }

    private void completeSeason() {
        if (!simulationRunning) return;

        while (currentWeek < NUM_WEEKS) {
            simulateNextWeek();
            try {
                Thread.sleep(500); // Small delay between weeks for animation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void simulateMatch(Team team1, Team team2) {
        outputArea.append(team1.getName() + " vs " + team2.getName() + "\n");

        int team1Points = 0;
        int team2Points = 0;

        // Each player bowls 3 games
        for (Player p1 : team1.getPlayers()) {
            for (int i = 0; i < 3; i++) {
                int score = p1.bowlGame();
                team1Points += score;
                outputArea.append("  " + p1.getName() + " scores: " + score + "\n");
            }
        }

        for (Player p2 : team2.getPlayers()) {
            for (int i = 0; i < 3; i++) {
                int score = p2.bowlGame();
                team2Points += score;
                outputArea.append("  " + p2.getName() + " scores: " + score + "\n");
            }
        }

        // Determine match winner
        if (team1Points > team2Points) {
            team1.addPoints(2);
            outputArea.append("  " + team1.getName() + " wins! (" + team1Points + " to " + team2Points + ")\n");
        } else if (team2Points > team1Points) {
            team2.addPoints(2);
            outputArea.append("  " + team2.getName() + " wins! (" + team2Points + " to " + team1Points + ")\n");
        } else {
            team1.addPoints(1);
            team2.addPoints(1);
            outputArea.append("  Match tied! (" + team1Points + " to " + team2Points + ")\n");
        }
    }

    private void displayWeeklyResults() {
        outputArea.append("\nCurrent Standings after Week " + currentWeek + ":\n");
    }

    private void displayTeamStandings() {
        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            outputArea.append((i+1) + ". " + team.getName() + ": " +
                    team.getTotalPoints() + " points (" + team.getWins() + " wins, " +
                    team.getTies() + " ties, " + team.getLosses() + " losses)\n");
        }
    }

    private void endSeason() {
        simulationRunning = false;
        nextWeekButton.setEnabled(false);
        completeSeasonButton.setEnabled(false);

        outputArea.append("\n=== SEASON COMPLETE ===\n");
        outputArea.append("Final Standings:\n");
        displayTeamStandings();

        Team champion = teams.get(0);
        outputArea.append("\nCongratulations to the " + champion.getName() + " for winning the league!\n");
    }

    class SimulationPanel extends JPanel {
        private static final int LANE_WIDTH = 800;
        private static final int LANE_HEIGHT = 30;
        private static final int BALL_SIZE = 20;
        private static final int PIN_WIDTH = 10;
        private static final int PIN_HEIGHT = 20;

        private int ballPosition = 0;
        private boolean ballRolling = false;
        private boolean[] pinsStanding = new boolean[10];

        public SimulationPanel() {
            setBackground(new Color(240, 240, 240));
            Arrays.fill(pinsStanding, true);

            // Animation timer
            Timer animationTimer = new Timer(30, e -> {
                if (ballRolling) {
                    ballPosition += 15;
                    if (ballPosition > LANE_WIDTH - 100) {
                        // Ball hits pins
                        for (int i = 0; i < pinsStanding.length; i++) {
                            if (pinsStanding[i] && Math.random() > 0.6) {
                                pinsStanding[i] = false;
                            }
                        }
                        ballRolling = false;
                    }
                    repaint();
                }
            });
            animationTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Draw lane
            int laneY = 100;
            g.setColor(new Color(200, 170, 100));
            g.fillRect(50, laneY, LANE_WIDTH, LANE_HEIGHT);
            g.setColor(Color.BLACK);
            g.drawRect(50, laneY, LANE_WIDTH, LANE_HEIGHT);

            // Draw gutters
            g.setColor(Color.GRAY);
            g.fillRect(50, laneY - 10, LANE_WIDTH, 10);
            g.fillRect(50, laneY + LANE_HEIGHT, LANE_WIDTH, 10);

            // Draw ball if rolling
            if (ballRolling) {
                g.setColor(Color.BLACK);
                g.fillOval(50 + ballPosition, laneY + (LANE_HEIGHT/2) - (BALL_SIZE/2), BALL_SIZE, BALL_SIZE);
            }

            // Draw pins
            int pinAreaX = 50 + LANE_WIDTH - 80;
            int pinAreaY = laneY - 30;

            // Pin positions in bowling triangle
            int[][] pinPositions = {
                    {0, 0},     // 7
                    {-15, 20},  // 4
                    {15, 20},   // 2
                    {-30, 40},  // 1
                    {0, 40},    // 6
                    {30, 40},   // 3
                    {-45, 60},  // 10
                    {-15, 60},  // 5
                    {15, 60},   // 8
                    {45, 60}    // 9
            };

            for (int i = 0; i < pinsStanding.length; i++) {
                if (pinsStanding[i]) {
                    g.setColor(Color.WHITE);
                    g.fillRect(pinAreaX + pinPositions[i][0], pinAreaY + pinPositions[i][1], PIN_WIDTH, PIN_HEIGHT);
                    g.setColor(Color.BLACK);
                    g.drawRect(pinAreaX + pinPositions[i][0], pinAreaY + pinPositions[i][1], PIN_WIDTH, PIN_HEIGHT);
                }
            }

            // Draw team information
            int teamInfoY = laneY + 100;
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("Current Week: " + currentWeek + " of " + NUM_WEEKS, 50, teamInfoY);

            teamInfoY += 30;
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("Team Standings:", 50, teamInfoY);

            teamInfoY += 20;
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            for (int i = 0; i < teams.size(); i++) {
                Team team = teams.get(i);
                String standing = (i+1) + ". " + team.getName() + ": " + team.getTotalPoints() + " pts";
                g.drawString(standing, 50, teamInfoY);
                teamInfoY += 15;
            }

            // Draw current match info if simulation is running
            if (simulationRunning && currentWeek > 0) {
                teamInfoY += 20;
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString("Current Matches:", 50, teamInfoY);

                for (int i = 0; i < teams.size(); i += 2) {
                    teamInfoY += 20;
                    Team team1 = teams.get(i);
                    Team team2 = teams.get(i+1);
                    g.setFont(new Font("Arial", Font.PLAIN, 12));
                    g.drawString(team1.getName() + " vs " + team2.getName(), 50, teamInfoY);
                }
            }
        }

        public void rollBall() {
            ballPosition = 0;
            Arrays.fill(pinsStanding, true);
            ballRolling = true;
        }
    }

    static class Team {
        private String name;
        private List<Player> players = new ArrayList<>();
        private int points = 0;
        private int wins = 0;
        private int ties = 0;
        private int losses = 0;

        public Team(String name) {
            this.name = name;
        }

        public void addPlayer(Player player) {
            players.add(player);
        }

        public void addPoints(int points) {
            this.points += points;
            if (points == 2) wins++;
            else if (points == 1) ties++;
            else losses++;
        }

        public String getName() { return name; }
        public List<Player> getPlayers() { return players; }
        public int getTotalPoints() { return points; }
        public int getWins() { return wins; }
        public int getTies() { return ties; }
        public int getLosses() { return losses; }
    }

    static class Player {
        private String name;
        private int skillLevel; // Average score (e.g., 150)
        private Random random = new Random();

        public Player(String name, int skillLevel) {
            this.name = name;
            this.skillLevel = skillLevel;
        }

        public int bowlGame() {
            // Base score based on skill level with some randomness
            int baseScore = skillLevel - 30 + random.nextInt(61);

            // Simulate strikes and spares
            if (random.nextDouble() < 0.05) { // 5% chance of perfect game
                return 300;
            } else if (random.nextDouble() < 0.1) { // 10% chance of strike
                return Math.min(300, baseScore + 30);
            } else if (random.nextDouble() < 0.2) { // 20% chance of spare
                return Math.min(300, baseScore + 15);
            }

            return Math.max(0, Math.min(300, baseScore));
        }

        public String getName() { return name; }
        public int getSkillLevel() { return skillLevel; }
    }
}