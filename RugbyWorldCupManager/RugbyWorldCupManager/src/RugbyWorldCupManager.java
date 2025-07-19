import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class RugbyWorldCupManager extends JFrame {
    private List<Team> teams;
    private List<Match> matches;
    private JPanel mainPanel, teamPanel, matchPanel, animationPanel;
    private JTextArea standingsArea;
    private JComboBox<String> team1Combo, team2Combo;
    private JButton simulateMatchButton, resetButton;
    private JLabel scoreLabel, matchStatusLabel;
    private RugbyField rugbyField;
    private Timer animationTimer;
    private int ballX, ballY, ballSpeedX, ballSpeedY;
    private boolean isAnimating;

    // Team class to hold team details
    static class Team {
        private String name;
        private int ranking;
        private int points;
        private int tries;
        private int matchesPlayed;

        public Team(String name, int ranking) {
            this.name = name;
            this.ranking = ranking;
            this.points = 0;
            this.tries = 0;
            this.matchesPlayed = 0;
        }

        public String getName() { return name; }
        public int getRanking() { return ranking; }
        public int getPoints() { return points; }
        public int getTries() { return tries; }
        public int getMatchesPlayed() { return matchesPlayed; }
        public void addPoints(int points) { this.points += points; }
        public void addTries(int tries) { this.tries += tries; }
        public void incrementMatches() { this.matchesPlayed++; }
        @Override
        public String toString() {
            return name + " (Rank: " + ranking + ", Points: " + points + ", Tries: " + tries + ")";
        }
    }

    // Match class to simulate a rugby match
    static class Match {
        private Team team1, team2;
        private int team1Tries, team2Tries;
        private int team1Points, team2Points;
        private Random random;

        public Match(Team team1, Team team2) {
            this.team1 = team1;
            this.team2 = team2;
            this.random = new Random();
        }

        public void simulate() {
            // Simulate tries based on team rankings
            int rankDiff = team1.getRanking() - team2.getRanking();
            team1Tries = random.nextInt(5) + (rankDiff < 0 ? 2 : 0);
            team2Tries = random.nextInt(5) + (rankDiff > 0 ? 2 : 0);
            // Points: 5 per try, plus possible conversions (2 points, 70% chance)
            team1Points = team1Tries * 5 + (random.nextDouble() < 0.7 ? team1Tries * 2 : 0);
            team2Points = team2Tries * 5 + (random.nextDouble() < 0.7 ? team2Tries * 2 : 0);
            // Update team stats
            team1.addTries(team1Tries);
            team2.addTries(team2Tries);
            team1.incrementMatches();
            team2.incrementMatches();
            // Award points: 4 for win, 2 for draw, 1 for loss with <7 points difference
            if (team1Points > team2Points) {
                team1.addPoints(4);
                if (team1Points - team2Points < 7) team2.addPoints(1);
            } else if (team2Points > team1Points) {
                team2.addPoints(4);
                if (team2Points - team1Points < 7) team1.addPoints(1);
            } else {
                team1.addPoints(2);
                team2.addPoints(2);
            }
        }

        public String getResult() {
            return team1.getName() + " " + team1Points + " (" + team1Tries + " tries) - " +
                    team2.getName() + " " + team2Points + " (" + team2Tries + " tries)";
        }
    }

    // Panel for animating the rugby field
    class RugbyField extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Draw rugby field (green with white lines)
            g.setColor(new Color(0, 128, 0));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.WHITE);
            // Draw field lines
            int w = getWidth(), h = getHeight();
            g.drawLine(w / 2, 0, w / 2, h); // Halfway line
            g.drawLine(50, 0, 50, h); // 10m line left
            g.drawLine(w - 50, 0, w - 50, h); // 10m line right
            g.drawLine(100, 0, 100, h); // 22m line left
            g.drawLine(w - 100, 0, w - 100, h); // 22m line right
            // Draw try lines
            g.setColor(Color.YELLOW);
            g.drawLine(20, 0, 20, h); // Left try line
            g.drawLine(w - 20, 0, w - 20, h); // Right try line
            // Draw rugby ball (oval)
            if (isAnimating) {
                Color BROWN = null;
                g.setColor(BROWN);
                g.fillOval(ballX, ballY, 20, 10);
            }
        }

        private class BROWN {
        }
    }

    public RugbyWorldCupManager() {
        // Initialize teams
        teams = new ArrayList<>();
        initializeTeams();
        matches = new ArrayList<>();
        isAnimating = false;

        // Set up main window
        setTitle("Rugby World Cup Manager");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Main panel with card layout
        mainPanel = new JPanel(new CardLayout());
        add(mainPanel, BorderLayout.CENTER);

        // Team selection panel
        teamPanel = new JPanel(new GridLayout(6, 1, 10, 10));
        teamPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel titleLabel = new JLabel("Rugby World Cup Manager", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        teamPanel.add(titleLabel);

        team1Combo = new JComboBox<>();
        team2Combo = new JComboBox<>();
        for (Team team : teams) {
            team1Combo.addItem(team.getName());
            team2Combo.addItem(team.getName());
        }
        teamPanel.add(new JLabel("Select Team 1:"));
        teamPanel.add(team1Combo);
        teamPanel.add(new JLabel("Select Team 2:"));
        teamPanel.add(team2Combo);

        simulateMatchButton = new JButton("Simulate Match");
        teamPanel.add(simulateMatchButton);
        mainPanel.add(teamPanel, "TeamSelection");

        // Match panel
        matchPanel = new JPanel(new BorderLayout());
        scoreLabel = new JLabel("Select teams and simulate a match", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        matchPanel.add(scoreLabel, BorderLayout.NORTH);
        rugbyField = new RugbyField();
        rugbyField.setPreferredSize(new Dimension(600, 400));
        matchPanel.add(rugbyField, BorderLayout.CENTER);
        matchStatusLabel = new JLabel("Match Status: Ready", SwingConstants.CENTER);
        matchPanel.add(matchStatusLabel, BorderLayout.SOUTH);
        mainPanel.add(matchPanel, "MatchSimulation");

        // Standings panel
        JPanel standingsPanel = new JPanel(new BorderLayout());
        standingsArea = new JTextArea();
        standingsArea.setEditable(false);
        standingsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(standingsArea);
        standingsPanel.add(scrollPane, BorderLayout.CENTER);
        resetButton = new JButton("Reset Tournament");
        standingsPanel.add(resetButton, BorderLayout.SOUTH);
        mainPanel.add(standingsPanel, "Standings");

        // Navigation buttons
        JPanel navPanel = new JPanel(new FlowLayout());
        JButton toTeamsButton = new JButton("Team Selection");
        JButton toMatchButton = new JButton("Match Simulation");
        JButton toStandingsButton = new JButton("Standings");
        navPanel.add(toTeamsButton);
        navPanel.add(toMatchButton);
        navPanel.add(toStandingsButton);
        add(navPanel, BorderLayout.SOUTH);

        // Animation timer
        animationTimer = new Timer(50, e -> animateBall());
        animationTimer.setRepeats(true);

        // Event listeners
        simulateMatchButton.addActionListener(e -> simulateMatch());
        resetButton.addActionListener(e -> resetTournament());
        toTeamsButton.addActionListener(e -> showPanel("TeamSelection"));
        toMatchButton.addActionListener(e -> showPanel("MatchSimulation"));
        toStandingsButton.addActionListener(e -> {
            updateStandings();
            showPanel("Standings");
        });

        // Initialize ball position
        ballX = rugbyField.getWidth() / 2;
        ballY = rugbyField.getHeight() / 2;
        ballSpeedX = 5;
        ballSpeedY = 3;

        // Show initial panel
        showPanel("TeamSelection");
    }

    private void initializeTeams() {
        teams.add(new Team("New Zealand", 1));
        teams.add(new Team("South Africa", 2));
        teams.add(new Team("England", 3));
        teams.add(new Team("France", 4));
        teams.add(new Team("Australia", 5));
        teams.add(new Team("Ireland", 6));
        teams.add(new Team("Wales", 7));
        teams.add(new Team("Argentina", 8));
        teams.add(new Team("Scotland", 9));
        teams.add(new Team("Japan", 10));
    }

    private void simulateMatch() {
        String team1Name = (String) team1Combo.getSelectedItem();
        String team2Name = (String) team2Combo.getSelectedItem();
        if (team1Name.equals(team2Name)) {
            JOptionPane.showMessageDialog(this, "Please select different teams!");
            return;
        }
        Team team1 = teams.stream().filter(t -> t.getName().equals(team1Name)).findFirst().orElse(null);
        Team team2 = teams.stream().filter(t -> t.getName().equals(team2Name)).findFirst().orElse(null);
        Match match = new Match(team1, team2);
        match.simulate();
        matches.add(match);
        scoreLabel.setText(match.getResult());
        matchStatusLabel.setText("Match Status: Simulating...");
        startAnimation();
        new Timer(5000, e -> {
            stopAnimation();
            matchStatusLabel.setText("Match Status: Completed");
            ((Timer)e.getSource()).stop();
        }).start();
    }

    private void startAnimation() {
        isAnimating = true;
        ballX = rugbyField.getWidth() / 2;
        ballY = rugbyField.getHeight() / 2;
        ballSpeedX = (int) (Math.random() * 10 - 5);
        ballSpeedY = (int) (Math.random() * 10 - 5);
        animationTimer.start();
    }

    private void stopAnimation() {
        isAnimating = false;
        animationTimer.stop();
        rugbyField.repaint();
    }

    private void animateBall() {
        ballX += ballSpeedX;
        ballY += ballSpeedY;
        // Bounce off field edges
        if (ballX <= 20 || ballX >= rugbyField.getWidth() - 20) {
            ballSpeedX = -ballSpeedX;
            // Simulate try if near try line
            if (ballX <= 20) matchStatusLabel.setText("Match Status: Try scored by Team 2!");
            else if (ballX >= rugbyField.getWidth() - 20) matchStatusLabel.setText("Match Status: Try scored by Team 1!");
        }
        if (ballY <= 0 || ballY >= rugbyField.getHeight() - 10) {
            ballSpeedY = -ballSpeedY;
        }
        rugbyField.repaint();
    }

    private void updateStandings() {
        StringBuilder sb = new StringBuilder();
        sb.append("Rugby World Cup Standings\n\n");
        sb.append(String.format("%-20s %-10s %-10s %-10s\n", "Team", "Points", "Tries", "Matches"));
        sb.append("-".repeat(50)).append("\n");
        teams.sort((t1, t2) -> t2.getPoints() - t1.getPoints());
        for (Team team : teams) {
            sb.append(String.format("%-20s %-10d %-10d %-10d\n",
                    team.getName(), team.getPoints(), team.getTries(), team.getMatchesPlayed()));
        }
        standingsArea.setText(sb.toString());
    }

    private void resetTournament() {
        teams.forEach(t -> {
            t.addPoints(-t.getPoints());
            t.addTries(-t.getTries());
            t.incrementMatches(-t.getMatchesPlayed());
        });
        matches.clear();
        updateStandings();
        scoreLabel.setText("Select teams and simulate a match");
        matchStatusLabel.setText("Match Status: Ready");
        JOptionPane.showMessageDialog(this, "Tournament reset!");
    }

    private void showPanel(String panelName) {
        CardLayout cl = (CardLayout) mainPanel.getLayout();
        cl.show(mainPanel, panelName);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RugbyWorldCupManager game = new RugbyWorldCupManager();
            game.setVisible(true);
        });
    }
}