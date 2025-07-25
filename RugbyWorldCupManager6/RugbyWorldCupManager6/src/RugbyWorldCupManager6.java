import GamePlayPanel.GamePlayPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Timer;

// Main Game Class
public class RugbyWorldCupManager extends JFrame {
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    private GameEngine gameEngine;
    private MainMenuPanel mainMenuPanel;
    private TeamSelectionPanel teamSelectionPanel;
    private GamePlayPanel gamePlayPanel;
    private TournamentPanel tournamentPanel;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    public RugbyWorldCupManager() {
        initializeGame();
        setupUI();
    }

    private void initializeGame() {
        gameEngine = new GameEngine();
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
    }

    private void setupUI() {
        setTitle("Rugby World Cup Manager 2024");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Initialize panels
        mainMenuPanel = new MainMenuPanel(this);
        teamSelectionPanel = new TeamSelectionPanel(this);
        gamePlayPanel = new GamePlayPanel(this);
        tournamentPanel = new TournamentPanel(this);

        // Add panels to main panel
        mainPanel.add(mainMenuPanel, "MAIN_MENU");
        mainPanel.add(teamSelectionPanel, "TEAM_SELECTION");
        mainPanel.add(gamePlayPanel, "GAMEPLAY");
        mainPanel.add(tournamentPanel, "TOURNAMENT");

        add(mainPanel);

        // Show main menu initially
        showPanel("MAIN_MENU");
    }

    public void showPanel(String panelName) {
        cardLayout.show(mainPanel, panelName);
    }

    public GameEngine getGameEngine() {
        return gameEngine;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new RugbyWorldCupManager().setVisible(true);
        });
    }
}

// Team Class
class Team {
    private String name;
    private String country;
    private int attack;
    private int defense;
    private int speed;
    private int stamina;
    private int overall;
    private int wins;
    private int losses;
    private int points;
    private Color teamColor;

    public Team(String name, String country, int attack, int defense, int speed, int stamina) {
        this.name = name;
        this.country = country;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
        this.stamina = stamina;
        this.overall = (attack + defense + speed + stamina) / 4;
        this.wins = 0;
        this.losses = 0;
        this.points = 0;
        this.teamColor = generateRandomColor();
    }

    private Color generateRandomColor() {
        Random rand = new Random();
        return new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
    }

    // Getters and setters
    public String getName() { return name; }
    public String getCountry() { return country; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getSpeed() { return speed; }
    public int getStamina() { return stamina; }
    public int getOverall() { return overall; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getPoints() { return points; }
    public Color getTeamColor() { return teamColor; }

    public void addWin() { wins++; points += 3; }
    public void addLoss() { losses++; }
    public void addDraw() { points += 1; }

    @Override
    public String toString() {
        return name + " (" + country + ") - Overall: " + overall;
    }
}

// Game Engine Class
class GameEngine {
    private List<Team> teams;
    private Team playerTeam;
    private Tournament tournament;
    private Random random;

    public GameEngine() {
        random = new Random();
        initializeTeams();
        tournament = new Tournament(teams);
    }

    private void initializeTeams() {
        teams = new ArrayList<>();

        // Add world-class rugby teams with realistic stats
        teams.add(new Team("All Blacks", "New Zealand", 95, 90, 88, 92));
        teams.add(new Team("Springboks", "South Africa", 92, 95, 85, 90));
        teams.add(new Team("Les Bleus", "France", 88, 85, 90, 87));
        teams.add(new Team("Red Rose", "England", 90, 88, 82, 89));
        teams.add(new Team("Wallabies", "Australia", 85, 82, 92, 86));
        teams.add(new Team("Dragons", "Wales", 83, 87, 80, 85));
        teams.add(new Team("Thistle", "Scotland", 80, 83, 85, 82));
        teams.add(new Team("Shamrock", "Ireland", 87, 86, 84, 88));
        teams.add(new Team("Azzurri", "Italy", 75, 78, 82, 80));
        teams.add(new Team("Cherry Blossoms", "Japan", 78, 75, 88, 83));
        teams.add(new Team("Los Pumas", "Argentina", 82, 85, 79, 84));
        teams.add(new Team("Eagles", "USA", 70, 72, 85, 75));
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setPlayerTeam(Team team) {
        this.playerTeam = team;
    }

    public Team getPlayerTeam() {
        return playerTeam;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public MatchResult simulateMatch(Team team1, Team team2) {
        // Complex match simulation based on team stats
        int team1Score = calculateTeamScore(team1);
        int team2Score = calculateTeamScore(team2);

        // Add some randomness
        team1Score += random.nextInt(21) - 10; // -10 to +10
        team2Score += random.nextInt(21) - 10;

        // Ensure non-negative scores
        team1Score = Math.max(0, team1Score);
        team2Score = Math.max(0, team2Score);

        // Update team records
        if (team1Score > team2Score) {
            team1.addWin();
            team2.addLoss();
        } else if (team2Score > team1Score) {
            team2.addWin();
            team1.addLoss();
        } else {
            team1.addDraw();
            team2.addDraw();
        }

        return new MatchResult(team1, team2, team1Score, team2Score);
    }

    private int calculateTeamScore(Team team) {
        return (team.getAttack() + team.getSpeed() + team.getStamina()) / 3 + random.nextInt(20);
    }
}

// Match Result Class
class MatchResult {
    private Team team1;
    private Team team2;
    private int team1Score;
    private int team2Score;
    private Date matchDate;

    public MatchResult(Team team1, Team team2, int team1Score, int team2Score) {
        this.team1 = team1;
        this.team2 = team2;
        this.team1Score = team1Score;
        this.team2Score = team2Score;
        this.matchDate = new Date();
    }

    public Team getWinner() {
        if (team1Score > team2Score) return team1;
        if (team2Score > team1Score) return team2;
        return null; // Draw
    }

    // Getters
    public Team getTeam1() { return team1; }
    public Team getTeam2() { return team2; }
    public int getTeam1Score() { return team1Score; }
    public int getTeam2Score() { return team2Score; }
    public Date getMatchDate() { return matchDate; }

    @Override
    public String toString() {
        return team1.getName() + " " + team1Score + " - " + team2Score + " " + team2.getName();
    }
}

// Tournament Class
class Tournament {
    private List<Team> teams;
    private List<MatchResult> results;
    private int currentRound;
    private String[] roundNames = {"Group Stage", "Quarter Finals", "Semi Finals", "Final"};

    public Tournament(List<Team> teams) {
        this.teams = new ArrayList<>(teams);
        this.results = new ArrayList<>();
        this.currentRound = 0;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public List<MatchResult> getResults() {
        return results;
    }

    public void addResult(MatchResult result) {
        results.add(result);
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void nextRound() {
        currentRound++;
    }

    public String getCurrentRoundName() {
        if (currentRound < roundNames.length) {
            return roundNames[currentRound];
        }
        return "Tournament Complete";
    }

    public List<Team> getTopTeams(int count) {
        return teams.stream()
                .sorted((t1, t2) -> Integer.compare(t2.getPoints(), t1.getPoints()))
                .limit(count)
                .collect(ArrayList::new, (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2));
    }
}

// Main Menu Panel
class MainMenuPanel extends JPanel {
    private RugbyWorldCupManager parent;
    private Timer animationTimer;
    private float titleOffset = 0;

    public MainMenuPanel(RugbyWorldCupManager parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        setBackground(new Color(0, 100, 0));

        setupComponents();
        startTitleAnimation();
    }

    private void setupComponents() {
        // Title Panel
        JPanel titlePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Animated title
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                g2d.setColor(Color.WHITE);
                String title = "RUGBY WORLD CUP MANAGER";
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(title)) / 2;
                int y = getHeight() / 2 + (int) titleOffset;
                g2d.drawString(title, x, y);

                // Subtitle
                g2d.setFont(new Font("Arial", Font.ITALIC, 24));
                String subtitle = "Lead Your Team to Glory!";
                fm = g2d.getFontMetrics();
                x = (getWidth() - fm.stringWidth(subtitle)) / 2;
                y += 60;
                g2d.drawString(subtitle, x, y);
            }
        };
        titlePanel.setOpaque(false);
        titlePanel.setPreferredSize(new Dimension(0, 300));

        // Button Panel
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);

        JButton startButton = createStyledButton("START TOURNAMENT");
        JButton instructionsButton = createStyledButton("INSTRUCTIONS");
        JButton exitButton = createStyledButton("EXIT");

        startButton.addActionListener(e -> parent.showPanel("TEAM_SELECTION"));
        instructionsButton.addActionListener(e -> showInstructions());
        exitButton.addActionListener(e -> System.exit(0));

        gbc.gridy = 0;
        buttonPanel.add(startButton, gbc);
        gbc.gridy = 1;
        buttonPanel.add(instructionsButton, gbc);
        gbc.gridy = 2;
        buttonPanel.add(exitButton, gbc);

        add(titlePanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(300, 60));
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setBackground(new Color(139, 69, 19));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(160, 82, 45));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(139, 69, 19));
            }
        });

        return button;
    }

    private void startTitleAnimation() {
        animationTimer = new Timer(50, e -> {
            titleOffset = (float) (Math.sin(System.currentTimeMillis() * 0.003) * 10);
            repaint();
        });
        animationTimer.start();
    }

    private void showInstructions() {
        String instructions = "RUGBY WORLD CUP MANAGER - INSTRUCTIONS\n\n" +
                "1. Select your team from the available options\n" +
                "2. Manage your team through the tournament\n" +
                "3. Play matches against other teams\n" +
                "4. Win the Rugby World Cup!\n\n" +
                "Team Stats:\n" +
                "- Attack: Offensive capability\n" +
                "- Defense: Defensive strength\n" +
                "- Speed: Team mobility\n" +
                "- Stamina: Endurance\n\n" +
                "Good luck, Coach!";

        JOptionPane.showMessageDialog(this, instructions, "Instructions", JOptionPane.INFORMATION_MESSAGE);
    }
}

// Team Selection Panel
class TeamSelectionPanel extends JPanel {
    private RugbyWorldCupManager parent;
    private JList<Team> teamList;
    private JPanel teamInfoPanel;
    private Team selectedTeam;

    public TeamSelectionPanel(RugbyWorldCupManager parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        setBackground(new Color(34, 139, 34));

        setupComponents();
    }

    private void setupComponents() {
        // Title
        JLabel titleLabel = new JLabel("SELECT YOUR TEAM", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        // Team list
        teamList = new JList<>(parent.getGameEngine().getTeams().toArray(new Team[0]));
        teamList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        teamList.setFont(new Font("Arial", Font.PLAIN, 16));
        teamList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedTeam = teamList.getSelectedValue();
                updateTeamInfo();
            }
        });

        JScrollPane listScrollPane = new JScrollPane(teamList);
        listScrollPane.setPreferredSize(new Dimension(400, 0));

        // Team info panel
        teamInfoPanel = new JPanel();
        teamInfoPanel.setLayout(new BoxLayout(teamInfoPanel, BoxLayout.Y_AXIS));
        teamInfoPanel.setBackground(Color.WHITE);
        teamInfoPanel.setBorder(BorderFactory.createTitledBorder("Team Information"));

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);

        JButton selectButton = new JButton("SELECT TEAM");
        JButton backButton = new JButton("BACK");

        selectButton.addActionListener(e -> {
            if (selectedTeam != null) {
                parent.getGameEngine().setPlayerTeam(selectedTeam);
                parent.showPanel("TOURNAMENT");
            } else {
                JOptionPane.showMessageDialog(this, "Please select a team first!");
            }
        });

        backButton.addActionListener(e -> parent.showPanel("MAIN_MENU"));

        buttonPanel.add(selectButton);
        buttonPanel.add(backButton);

        // Layout
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Available Teams:", JLabel.CENTER), BorderLayout.NORTH);
        leftPanel.add(listScrollPane, BorderLayout.CENTER);
        leftPanel.setOpaque(false);

        add(titleLabel, BorderLayout.NORTH);
        add(leftPanel, BorderLayout.WEST);
        add(teamInfoPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void updateTeamInfo() {
        teamInfoPanel.removeAll();

        if (selectedTeam != null) {
            teamInfoPanel.add(createInfoLabel("Team: " + selectedTeam.getName()));
            teamInfoPanel.add(createInfoLabel("Country: " + selectedTeam.getCountry()));
            teamInfoPanel.add(Box.createVerticalStrut(20));
            teamInfoPanel.add(createInfoLabel("TEAM STATISTICS:"));
            teamInfoPanel.add(createStatBar("Attack", selectedTeam.getAttack()));
            teamInfoPanel.add(createStatBar("Defense", selectedTeam.getDefense()));
            teamInfoPanel.add(createStatBar("Speed", selectedTeam.getSpeed()));
            teamInfoPanel.add(createStatBar("Stamina", selectedTeam.getStamina()));
            teamInfoPanel.add(Box.createVerticalStrut(10));
            teamInfoPanel.add(createInfoLabel("Overall Rating: " + selectedTeam.getOverall()));
        }

        teamInfoPanel.revalidate();
        teamInfoPanel.repaint();
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private JPanel createStatBar(String statName, int value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel nameLabel = new JLabel(statName + ": " + value);
        nameLabel.setPreferredSize(new Dimension(100, 20));

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(value);
        progressBar.setStringPainted(true);
        progressBar.setForeground(getStatColor(value));

        panel.add(nameLabel, BorderLayout.WEST);
        panel.add(progressBar, BorderLayout.CENTER);

        return panel;
    }

    private Color getStatColor(int value) {
        if (value >= 90) return Color.GREEN;
        if (value >= 80) return Color.YELLOW;
        if (value >= 70) return Color.ORANGE;
        return Color.RED;
    }
}

// Tournament Panel
class TournamentPanel extends JPanel {
    private RugbyWorldCupManager parent;
    private JLabel roundLabel;
    private JPanel matchesPanel;
    private JPanel standingsPanel;
    private Timer matchAnimationTimer;
    private boolean animatingMatch = false;

    public TournamentPanel(RugbyWorldCupManager parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        setBackground(new Color(25, 25, 112));

        setupComponents();
        updateTournamentDisplay();
    }

    private void setupComponents() {
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        roundLabel = new JLabel("TOURNAMENT - GROUP STAGE", JLabel.CENTER);
        roundLabel.setFont(new Font("Arial", Font.BOLD, 28));
        roundLabel.setForeground(Color.WHITE);

        JLabel playerTeamLabel = new JLabel("Your Team: " +
                (parent.getGameEngine().getPlayerTeam() != null ?
                        parent.getGameEngine().getPlayerTeam().getName() : "None"), JLabel.CENTER);
        playerTeamLabel.setFont(new Font("Arial", Font.ITALIC, 18));
        playerTeamLabel.setForeground(Color.YELLOW);

        headerPanel.add(roundLabel, BorderLayout.CENTER);
        headerPanel.add(playerTeamLabel, BorderLayout.SOUTH);

        // Main content
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // Matches panel
        matchesPanel = new JPanel();
        matchesPanel.setLayout(new BoxLayout(matchesPanel, BoxLayout.Y_AXIS));
        matchesPanel.setBackground(Color.WHITE);
        matchesPanel.setBorder(BorderFactory.createTitledBorder("Upcoming Matches"));

        JScrollPane matchesScrollPane = new JScrollPane(matchesPanel);
        matchesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Standings panel
        standingsPanel = new JPanel();
        standingsPanel.setLayout(new BoxLayout(standingsPanel, BoxLayout.Y_AXIS));
        standingsPanel.setBackground(Color.WHITE);
        standingsPanel.setBorder(BorderFactory.createTitledBorder("Tournament Standings"));

        JScrollPane standingsScrollPane = new JScrollPane(standingsPanel);
        standingsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        contentPanel.add(matchesScrollPane);
        contentPanel.add(standingsScrollPane);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);

        JButton playMatchButton = new JButton("PLAY NEXT MATCH");
        JButton simulateRoundButton = new JButton("SIMULATE ROUND");
        JButton backButton = new JButton("BACK TO MENU");

        playMatchButton.addActionListener(e -> playNextMatch());
        simulateRoundButton.addActionListener(e -> simulateRound());
        backButton.addActionListener(e -> parent.showPanel("MAIN_MENU"));

        buttonPanel.add(playMatchButton);
        buttonPanel.add(simulateRoundButton);
        buttonPanel.add(backButton);

        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void updateTournamentDisplay() {
        Tournament tournament = parent.getGameEngine().getTournament();
        roundLabel.setText("TOURNAMENT - " + tournament.getCurrentRoundName());

        updateMatchesPanel();
        updateStandingsPanel();
    }

    private void updateMatchesPanel() {
        matchesPanel.removeAll();

        // Generate some sample matches for display
        List<Team> teams = parent.getGameEngine().getTeams();
        for (int i = 0; i < Math.min(6, teams.size() - 1); i += 2) {
            if (i + 1 < teams.size()) {
                JPanel matchPanel = createMatchPanel(teams.get(i), teams.get(i + 1));
                matchesPanel.add(matchPanel);
                matchesPanel.add(Box.createVerticalStrut(5));
            }
        }

        matchesPanel.revalidate();
        matchesPanel.repaint();
    }

    private JPanel createMatchPanel(Team team1, Team team2) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel matchLabel = new JLabel(team1.getName() + " vs " + team2.getName(), JLabel.CENTER);
        matchLabel.setFont(new Font("Arial", Font.BOLD, 14));

        panel.add(matchLabel, BorderLayout.CENTER);

        return panel;
    }

    private void updateStandingsPanel() {
        standingsPanel.removeAll();

        List<Team> sortedTeams = parent.getGameEngine().getTournament().getTopTeams(12);

        // Header
        JPanel headerPanel = new JPanel(new GridLayout(1, 4));
        headerPanel.add(new JLabel("Team", JLabel.CENTER));
        headerPanel.add(new JLabel("Wins", JLabel.CENTER));
        headerPanel.add(new JLabel("Losses", JLabel.CENTER));
        headerPanel.add(new JLabel("Points", JLabel.CENTER));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));

        standingsPanel.add(headerPanel);

        // Team standings
        for (int i = 0; i < sortedTeams.size(); i++) {
            Team team = sortedTeams.get(i);
            JPanel teamPanel = new JPanel(new GridLayout(1, 4));

            // Highlight player's team
            if (team == parent.getGameEngine().getPlayerTeam()) {
                teamPanel.setBackground(Color.YELLOW);
            }

            teamPanel.add(new JLabel((i + 1) + ". " + team.getName()));
            teamPanel.add(new JLabel(String.valueOf(team.getWins()), JLabel.CENTER));
            teamPanel.add(new JLabel(String.valueOf(team.getLosses()), JLabel.CENTER));
            teamPanel.add(new JLabel(String.valueOf(team.getPoints()), JLabel.CENTER));

            standingsPanel.add(teamPanel);
        }

        standingsPanel.revalidate();
        standingsPanel.repaint();
    }

    private void playNextMatch() {
        if (animatingMatch) return;

        List<Team> teams = parent.getGameEngine().getTeams();
        if (teams.size() < 2) return;

        // Select random teams for match
        Team team1 = teams.get(ThreadLocalRandom.current().nextInt(teams.size()));
        Team team2;
        do {
            team2 = teams.get(ThreadLocalRandom.current().nextInt(teams.size()));
        } while (team2 == team1);

        animateMatch(team1, team2);
    }

    private void animateMatch(Team team1, Team team2) {
        animatingMatch = true;

        // Create match animation dialog
        JDialog matchDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Match in Progress", true);
        matchDialog.setSize(400, 200);
        matchDialog.setLocationRelativeTo(this);

        JLabel matchLabel = new JLabel(team1.getName() + " vs " + team2.getName(), JLabel.CENTER);
        matchLabel.setFont(new Font("Arial", Font.BOLD, 18));

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Match Starting...");

        matchDialog.setLayout(new BorderLayout());
        matchDialog.add(matchLabel, BorderLayout.NORTH);
        matchDialog.add(progressBar, BorderLayout.CENTER);

        // Animation timer
        Timer animTimer = new Timer(100, null);
        animTimer.addActionListener(new ActionListener() {
            int progress = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                progress += 5;
                progressBar.setValue(progress);

                if (progress <= 30) {
                    progressBar.setString("First Half...");
                } else if (progress <= 70) {
                    progressBar.setString("Second Half...");
                } else if (progress < 100) {
                    progressBar.setString("Final Minutes...");
                } else {
                    animTimer.stop();

                    // Simulate match result
                    MatchResult result = parent.getGameEngine().simulateMatch(team1, team2);
                    parent.getGameEngine().getTournament().addResult(result);

                    progressBar.setString("Match Complete!");

                    Timer closeTimer = new Timer(1000, closeEvent -> {
                        matchDialog.dispose();
                        animatingMatch = false;
                        updateTournamentDisplay();

                        // Show result
                        JOptionPane.showMessageDialog(TournamentPanel.this,
                                "Match Result:\n" + result.toString(),
                                "Match Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                    });
                    closeTimer.setRepeats(false);
                    closeTimer.wait();
                }
            }
        });

        animTimer.wait();
        matchDialog.setVisible(true);
    }

    private void simulateRound() {
        List<Team> teams = parent.getGameEngine().getTeams();
        int matchesToPlay = Math.min(5, teams.size() / 2);

        for (int i = 0; i < matchesToPlay; i++) {
            Team team1 = teams.get(ThreadLocalRandom.current().nextInt(teams.size()));
            Team team2;
            do {
                team2 = teams.get(ThreadLocalRandom.current().nextInt(teams.
