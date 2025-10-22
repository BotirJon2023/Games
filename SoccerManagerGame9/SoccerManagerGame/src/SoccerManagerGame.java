import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;

// Main Game Class
public class SoccerManagerGame extends JFrame {
    private Team playerTeam;
    private List<Team> leagueTeams;
    private int budget = 1000000;
    private int season = 1;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private Match currentMatch;
    private Timer matchTimer;
    private int matchMinute = 0;

    public SoccerManagerGame() {
        setTitle("Soccer Manager Game");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.add(createMenuPanel(), "MENU");
        add(mainPanel);
        setVisible(true);
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(34, 139, 34));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel titleLabel = new JLabel("SOCCER MANAGER");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Build Your Dream Team!");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 24));
        subtitleLabel.setForeground(Color.YELLOW);
        gbc.gridy = 1;
        panel.add(subtitleLabel, gbc);

        JLabel nameLabel = new JLabel("Enter Team Name:");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 18));
        nameLabel.setForeground(Color.WHITE);
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(nameLabel, gbc);

        JTextField teamNameField = new JTextField(20);
        teamNameField.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridx = 1;
        panel.add(teamNameField, gbc);

        JButton startButton = new JButton("START GAME");
        startButton.setFont(new Font("Arial", Font.BOLD, 20));
        startButton.setBackground(new Color(255, 215, 0));
        startButton.setForeground(Color.BLACK);
        startButton.setFocusPainted(false);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        startButton.addActionListener(e -> {
            String teamName = teamNameField.getText().trim();
            if (!teamName.isEmpty()) {
                initializeGame(teamName);
                showDashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Please enter a team name!");
            }
        });
        panel.add(startButton, gbc);

        return panel;
    }

    private void initializeGame(String teamName) {
        playerTeam = new Team(teamName, false);
        leagueTeams = new ArrayList<>();
        leagueTeams.add(playerTeam);

        for (int i = 1; i <= 9; i++) {
            leagueTeams.add(new Team("Team " + i, true));
        }
    }

    private void showDashboard() {
        JPanel dashboardPanel = createDashboardPanel();
        mainPanel.add(dashboardPanel, "DASHBOARD");
        cardLayout.show(mainPanel, "DASHBOARD");
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.LIGHT_GRAY);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 100, 200));
        headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel teamNameLabel = new JLabel(playerTeam.name);
        teamNameLabel.setFont(new Font("Arial", Font.BOLD, 32));
        teamNameLabel.setForeground(Color.WHITE);
        headerPanel.add(teamNameLabel, BorderLayout.WEST);

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        statsPanel.setOpaque(false);

        JLabel budgetLabel = new JLabel("Budget: $" + (budget / 1000) + "K");
        budgetLabel.setFont(new Font("Arial", Font.BOLD, 18));
        budgetLabel.setForeground(Color.YELLOW);
        statsPanel.add(budgetLabel);

        JLabel seasonLabel = new JLabel("Season: " + season);
        seasonLabel.setFont(new Font("Arial", Font.BOLD, 18));
        seasonLabel.setForeground(Color.WHITE);
        statsPanel.add(seasonLabel);

        headerPanel.add(statsPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        centerPanel.setBackground(Color.LIGHT_GRAY);

        JPanel teamStatsCard = createStatsCard("Team Statistics",
                "Wins: " + playerTeam.wins,
                "Draws: " + playerTeam.draws,
                "Losses: " + playerTeam.losses,
                "Points: " + playerTeam.points,
                "Goals For: " + playerTeam.goalsFor,
                "Goals Against: " + playerTeam.goalsAgainst);
        centerPanel.add(teamStatsCard);

        JPanel formationCard = createFormationCard();
        centerPanel.add(formationCard);

        JPanel actionsCard = createActionsCard();
        centerPanel.add(actionsCard);

        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel leagueTablePanel = createLeagueTablePanel();
        panel.add(leagueTablePanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatsCard(String title, String... stats) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.add(titleLabel, BorderLayout.NORTH);

        JPanel statsPanel = new JPanel(new GridLayout(stats.length, 1, 5, 5));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        for (String stat : stats) {
            JLabel statLabel = new JLabel(stat);
            statLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            statsPanel.add(statLabel);
        }

        card.add(statsPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFormationCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));

        JLabel titleLabel = new JLabel("Formation");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        String[] formations = {"4-4-2", "4-3-3", "3-5-2", "5-3-2"};
        JComboBox<String> formationBox = new JComboBox<>(formations);
        formationBox.setFont(new Font("Arial", Font.PLAIN, 16));
        formationBox.setSelectedItem(playerTeam.formation);
        formationBox.addActionListener(e -> {
            playerTeam.formation = (String) formationBox.getSelectedItem();
        });
        contentPanel.add(formationBox);

        JLabel strengthLabel = new JLabel("Team Strength: " +
                String.format("%.0f", playerTeam.calculateStrength()));
        strengthLabel.setFont(new Font("Arial", Font.BOLD, 16));
        contentPanel.add(strengthLabel);

        card.add(contentPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createActionsCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));

        JLabel titleLabel = new JLabel("Quick Actions");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.add(titleLabel, BorderLayout.NORTH);

        JPanel buttonsPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        buttonsPanel.setBackground(Color.WHITE);
        buttonsPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        JButton squadButton = new JButton("Manage Squad");
        squadButton.setFont(new Font("Arial", Font.BOLD, 16));
        squadButton.setBackground(new Color(0, 100, 200));
        squadButton.setForeground(Color.WHITE);
        squadButton.setFocusPainted(false);
        squadButton.addActionListener(e -> showSquadManagement());
        buttonsPanel.add(squadButton);

        JButton matchButton = new JButton("Play Match");
        matchButton.setFont(new Font("Arial", Font.BOLD, 16));
        matchButton.setBackground(new Color(34, 139, 34));
        matchButton.setForeground(Color.WHITE);
        matchButton.setFocusPainted(false);
        matchButton.addActionListener(e -> showFixtures());
        buttonsPanel.add(matchButton);

        JButton refreshButton = new JButton("Refresh Dashboard");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 16));
        refreshButton.setBackground(new Color(150, 150, 150));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> refreshDashboard());
        buttonsPanel.add(refreshButton);

        card.add(buttonsPanel, BorderLayout.CENTER);
        return card;
    }

    private void refreshDashboard() {
        for (Component comp : mainPanel.getComponents()) {
            mainPanel.remove(comp);
        }
        mainPanel.add(createMenuPanel(), "MENU");
        showDashboard();
    }

    private JPanel createLeagueTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("League Table"));

        String[] columns = {"Pos", "Team", "P", "W", "D", "L", "GF", "GA", "GD", "Pts"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        leagueTeams.sort((t1, t2) -> {
            if (t2.points != t1.points) return t2.points - t1.points;
            return (t2.goalsFor - t2.goalsAgainst) - (t1.goalsFor - t1.goalsAgainst);
        });

        for (int i = 0; i < leagueTeams.size(); i++) {
            Team t = leagueTeams.get(i);
            int played = t.wins + t.draws + t.losses;
            int gd = t.goalsFor - t.goalsAgainst;
            model.addRow(new Object[]{i + 1, t.name, played, t.wins, t.draws, t.losses,
                    t.goalsFor, t.goalsAgainst, gd, t.points});
        }

        JTable table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void showSquadManagement() {
        JPanel squadPanel = createSquadManagementPanel();
        mainPanel.add(squadPanel, "SQUAD");
        cardLayout.show(mainPanel, "SQUAD");
    }

    private JPanel createSquadManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.LIGHT_GRAY);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 100, 200));
        headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("Squad Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton backButton = new JButton("Back to Dashboard");
        backButton.setFont(new Font("Arial", Font.BOLD, 16));
        backButton.addActionListener(e -> refreshDashboard());
        headerPanel.add(backButton, BorderLayout.EAST);

        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel buyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buyPanel.setBackground(Color.WHITE);
        buyPanel.setBorder(BorderFactory.createTitledBorder("Buy Players ($200K each)"));

        String[] positions = {"GK", "DEF", "MID", "FWD"};
        for (String pos : positions) {
            JButton buyButton = new JButton("Buy " + pos);
            buyButton.setFont(new Font("Arial", Font.BOLD, 14));
            buyButton.setBackground(new Color(34, 139, 34));
            buyButton.setForeground(Color.WHITE);
            buyButton.addActionListener(e -> buyPlayer(pos));
            buyPanel.add(buyButton);
        }

        panel.add(buyPanel, BorderLayout.NORTH);

        String[] columns = {"Name", "Pos", "Age", "Skill", "Form", "Stamina", "Goals", "Assists"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        for (Player p : playerTeam.players) {
            model.addRow(new Object[]{p.name, p.position, p.age, p.skill, p.form,
                    p.stamina, p.goals, p.assists});
        }

        JTable table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout());
        actionPanel.setBackground(Color.WHITE);
        JButton trainButton = new JButton("Train Selected Player ($10K)");
        trainButton.setFont(new Font("Arial", Font.BOLD, 14));
        trainButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && budget >= 10000) {
                Player p = playerTeam.players.get(row);
                p.skill = Math.min(99, p.skill + new Random().nextInt(3) + 1);
                budget -= 10000;
                showSquadManagement();
            } else if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select a player!");
            } else {
                JOptionPane.showMessageDialog(this, "Insufficient funds!");
            }
        });
        actionPanel.add(trainButton);
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void buyPlayer(String position) {
        if (budget >= 200000 && playerTeam.players.size() < 16) {
            playerTeam.players.add(new Player(position));
            budget -= 200000;
            showSquadManagement();
        } else {
            JOptionPane.showMessageDialog(this,
                    budget < 200000 ? "Insufficient funds!" : "Squad is full!");
        }
    }

    private void showFixtures() {
        JPanel fixturesPanel = createFixturesPanel();
        mainPanel.add(fixturesPanel, "FIXTURES");
        cardLayout.show(mainPanel, "FIXTURES");
    }

    private JPanel createFixturesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.LIGHT_GRAY);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 100, 200));
        headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("Fixtures");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton backButton = new JButton("Back");
        backButton.setFont(new Font("Arial", Font.BOLD, 16));
        backButton.addActionListener(e -> refreshDashboard());
        headerPanel.add(backButton, BorderLayout.EAST);

        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel fixturesListPanel = new JPanel();
        fixturesListPanel.setLayout(new BoxLayout(fixturesListPanel, BoxLayout.Y_AXIS));
        fixturesListPanel.setBackground(Color.WHITE);

        for (Team opponent : leagueTeams) {
            if (!opponent.name.equals(playerTeam.name)) {
                JPanel fixturePanel = new JPanel(new BorderLayout(10, 10));
                fixturePanel.setBackground(Color.WHITE);
                fixturePanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY),
                        new EmptyBorder(15, 15, 15, 15)));
                fixturePanel.setMaximumSize(new Dimension(800, 80));

                JLabel matchLabel = new JLabel(playerTeam.name + " vs " + opponent.name);
                matchLabel.setFont(new Font("Arial", Font.BOLD, 18));
                fixturePanel.add(matchLabel, BorderLayout.WEST);

                JButton playButton = new JButton("Play Match");
                playButton.setFont(new Font("Arial", Font.BOLD, 16));
                playButton.setBackground(new Color(34, 139, 34));
                playButton.setForeground(Color.WHITE);
                playButton.addActionListener(e -> startMatch(opponent));
                fixturePanel.add(playButton, BorderLayout.EAST);

                fixturesListPanel.add(fixturePanel);
                fixturesListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }

        JScrollPane scrollPane = new JScrollPane(fixturesListPanel);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void startMatch(Team opponent) {
        currentMatch = new Match(playerTeam, opponent);
        matchMinute = 0;
        showMatchPanel();
    }

    private void showMatchPanel() {
        JPanel matchPanel = createMatchPanel();
        mainPanel.add(matchPanel, "MATCH");
        cardLayout.show(mainPanel, "MATCH");

        matchTimer = new Timer(100, e -> {
            matchMinute++;
            if (matchMinute <= 90) {
                updateMatch();
            } else {
                matchTimer.stop();
                finishMatch();
            }
        });
        matchTimer.start();
    }

    private JPanel createMatchPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(34, 139, 34));

        JPanel scorePanel = new JPanel(new BorderLayout());
        scorePanel.setBackground(new Color(30, 30, 30));
        scorePanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel teamsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        teamsPanel.setOpaque(false);

        JLabel homeLabel = new JLabel(currentMatch.homeTeam.name, SwingConstants.CENTER);
        homeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        homeLabel.setForeground(Color.WHITE);
        teamsPanel.add(homeLabel);

        JLabel scoreLabel = new JLabel(currentMatch.homeScore + " - " + currentMatch.awayScore,
                SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 36));
        scoreLabel.setForeground(Color.YELLOW);
        scoreLabel.setName("scoreLabel");
        teamsPanel.add(scoreLabel);

        JLabel awayLabel = new JLabel(currentMatch.awayTeam.name, SwingConstants.CENTER);
        awayLabel.setFont(new Font("Arial", Font.BOLD, 24));
        awayLabel.setForeground(Color.WHITE);
        teamsPanel.add(awayLabel);

        scorePanel.add(teamsPanel, BorderLayout.CENTER);

        JLabel minuteLabel = new JLabel("0'", SwingConstants.CENTER);
        minuteLabel.setFont(new Font("Arial", Font.BOLD, 28));
        minuteLabel.setForeground(Color.WHITE);
        minuteLabel.setName("minuteLabel");
        scorePanel.add(minuteLabel, BorderLayout.SOUTH);

        panel.add(scorePanel, BorderLayout.NORTH);

        MatchAnimationPanel pitchPanel = new MatchAnimationPanel();
        pitchPanel.setPreferredSize(new Dimension(800, 400));
        panel.add(pitchPanel, BorderLayout.CENTER);

        JTextArea eventsArea = new JTextArea();
        eventsArea.setEditable(false);
        eventsArea.setFont(new Font("Arial", Font.PLAIN, 14));
        eventsArea.setName("eventsArea");
        JScrollPane scrollPane = new JScrollPane(eventsArea);
        scrollPane.setPreferredSize(new Dimension(0, 150));
        panel.add(scrollPane, BorderLayout.SOUTH);

        return panel;
    }

    private void updateMatch() {
        if (matchMinute % 3 == 0) {
            currentMatch.simulateEvent(matchMinute);
        }
        updateMatchUI();
    }

    private void updateMatchUI() {
        for (Component comp : mainPanel.getComponents()) {
            if (comp.isVisible() && comp instanceof JPanel) {
                updatePanelComponents((JPanel) comp);
            }
        }
    }

    private void updatePanelComponents(JPanel panel) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JPanel) {
                updatePanelComponents((JPanel) comp);
            } else if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if ("scoreLabel".equals(label.getName())) {
                    label.setText(currentMatch.homeScore + " - " + currentMatch.awayScore);
                } else if ("minuteLabel".equals(label.getName())) {
                    label.setText(matchMinute + "'");
                }
            } else if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                Component view = scrollPane.getViewport().getView();
                if (view instanceof JTextArea) {
                    JTextArea eventsArea = (JTextArea) view;
                    if ("eventsArea".equals(eventsArea.getName())) {
                        StringBuilder sb = new StringBuilder();
                        for (MatchEvent event : currentMatch.events) {
                            sb.append(event.minute).append("' - ").append(event.description).append("\n");
                        }
                        eventsArea.setText(sb.toString());
                        eventsArea.setCaretPosition(eventsArea.getDocument().getLength());
                    }
                }
            }
        }
    }

    private void finishMatch() {
        int homeScore = currentMatch.homeScore;
        int awayScore = currentMatch.awayScore;

        String result;
        int points;
        if (homeScore > awayScore) {
            result = "win";
            points = 3;
            playerTeam.wins++;
        } else if (homeScore < awayScore) {
            result = "loss";
            points = 0;
            playerTeam.losses++;
        } else {
            result = "draw";
            points = 1;
            playerTeam.draws++;
        }

        playerTeam.goalsFor += homeScore;
        playerTeam.goalsAgainst += awayScore;
        playerTeam.points += points;
        budget += 50000 + (points * 10000);

        Timer delayTimer = new Timer(2000, e -> {
            showPostMatch(result);
            ((Timer) e.getSource()).stop();
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

    private void showPostMatch(String result) {
        JPanel postMatchPanel = createPostMatchPanel(result);
        mainPanel.add(postMatchPanel, "POSTMATCH");
        cardLayout.show(mainPanel, "POSTMATCH");
    }

    private JPanel createPostMatchPanel(String result) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.LIGHT_GRAY);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);

        JLabel titleLabel = new JLabel("Full Time!");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 40));
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(titleLabel, gbc);

        String scoreText = currentMatch.homeTeam.name + " " + currentMatch.homeScore +
                " - " + currentMatch.awayScore + " " + currentMatch.awayTeam.name;
        JLabel scoreLabel = new JLabel(scoreText);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 28));
        gbc.gridy = 1;
        panel.add(scoreLabel, gbc);

        String resultText;
        Color resultColor;
        if (result.equals("win")) {
            resultText = "Victory!";
            resultColor = new Color(34, 139, 34);
        } else if (result.equals("loss")) {
            resultText = "Defeat";
            resultColor = Color.RED;
        } else {
            resultText = "Draw";
            resultColor = Color.ORANGE;
        }

        JLabel resultLabel = new JLabel(resultText);
        resultLabel.setFont(new Font("Arial", Font.BOLD, 32));
        resultLabel.setForeground(resultColor);
        gbc.gridy = 2;
        panel.add(resultLabel, gbc);

        JButton backButton = new JButton("Return to Dashboard");
        backButton.setFont(new Font("Arial", Font.BOLD, 18));
        backButton.setBackground(new Color(0, 100, 200));
        backButton.setForeground(Color.WHITE);
        backButton.addActionListener(e -> refreshDashboard());
        gbc.gridy = 3;
        panel.add(backButton, gbc);

        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SoccerManagerGame());
    }
}

// Player Class
class Player {
    String name;
    String position;
    int skill;
    int stamina;
    int form;
    int goals;
    int assists;
    int age;

    private static final String[] FIRST_NAMES = {"James", "Michael", "Robert", "John", "David",
            "Carlos", "Luis", "Marco", "Andre", "Thomas", "Diego", "Paulo", "Gabriel", "Lucas"};
    private static final String[] LAST_NAMES = {"Smith", "Johnson", "Williams", "Brown", "Jones",
            "Garcia", "Rodriguez", "Silva", "Santos", "Fernandez", "Lopez", "Martinez", "Gonzalez"};

    public Player(String position) {
        Random rand = new Random();
        this.position = position;
        this.name = FIRST_NAMES[rand.nextInt(FIRST_NAMES.length)] + " " +
                LAST_NAMES[rand.nextInt(LAST_NAMES.length)];
        this.skill = rand.nextInt(30) + 50; // 50-79
        this.stamina = rand.nextInt(20) + 70; // 70-89
        this.form = rand.nextInt(20) + 70; // 70-89
        this.goals = 0;
        this.assists = 0;
        this.age = rand.nextInt(10) + 18; // 18-27
    }

    public Player(String name, String position, int skill) {
        this.name = name;
        this.position = position;
        this.skill = skill;
        this.stamina = 80;
        this.form = 80;
        this.goals = 0;
        this.assists = 0;
        this.age = 22;
    }
}

// Team Class
class Team {
    String name;
    List<Player> players;
    int wins;
    int draws;
    int losses;
    int goalsFor;
    int goalsAgainst;
    int points;
    String formation;
    boolean isAI;

    public Team(String name, boolean isAI) {
        this.name = name;
        this.isAI = isAI;
        this.players = new ArrayList<>();
        this.wins = 0;
        this.draws = 0;
        this.losses = 0;
        this.goalsFor = 0;
        this.goalsAgainst = 0;
        this.points = 0;
        this.formation = "4-4-2";

        // Initialize with some players
        if (isAI) {
            initializeAITeam();
        } else {
            // Player team starts with basic squad
            initializePlayerTeam();
        }
    }

    private void initializePlayerTeam() {
        // Start with 5 basic players
        players.add(new Player("GK"));
        players.add(new Player("DEF"));
        players.add(new Player("MID"));
        players.add(new Player("MID"));
        players.add(new Player("FWD"));
    }

    private void initializeAITeam() {
        Random rand = new Random();
        int teamSize = rand.nextInt(6) + 10; // 10-15 players

        for (int i = 0; i < teamSize; i++) {
            String[] positions = {"GK", "DEF", "MID", "FWD"};
            String position = positions[rand.nextInt(positions.length)];
            players.add(new Player(position));
        }
    }

    public double calculateStrength() {
        if (players.isEmpty()) return 0;

        double totalSkill = 0;
        for (Player p : players) {
            totalSkill += p.skill;
        }
        return totalSkill / players.size();
    }

    public Player getRandomPlayer() {
        if (players.isEmpty()) return null;
        Random rand = new Random();
        return players.get(rand.nextInt(players.size()));
    }

    public Player getRandomPlayerByPosition(String position) {
        List<Player> positionPlayers = new ArrayList<>();
        for (Player p : players) {
            if (p.position.equals(position)) {
                positionPlayers.add(p);
            }
        }
        if (positionPlayers.isEmpty()) return getRandomPlayer();

        Random rand = new Random();
        return positionPlayers.get(rand.nextInt(positionPlayers.size()));
    }
}

// Match Event Class
class MatchEvent {
    int minute;
    String description;
    String team;

    public MatchEvent(int minute, String description, String team) {
        this.minute = minute;
        this.description = description;
        this.team = team;
    }
}

// Match Class
class Match {
    Team homeTeam;
    Team awayTeam;
    int homeScore;
    int awayScore;
    List<MatchEvent> events;
    Random random;

    public Match(Team homeTeam, Team awayTeam) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeScore = 0;
        this.awayScore = 0;
        this.events = new ArrayList<>();
        this.random = new Random();
    }

    public void simulateEvent(int minute) {
        double homeStrength = homeTeam.calculateStrength();
        double awayStrength = awayTeam.calculateStrength();

        // Home team advantage
        homeStrength *= 1.1;

        double totalStrength = homeStrength + awayStrength;
        double homeProb = homeStrength / totalStrength;
        double awayProb = awayStrength / totalStrength;

        double event = random.nextDouble();

        if (event < 0.6) {
            // No event
            return;
        } else if (event < 0.6 + homeProb * 0.3) {
            // Home team scores
            homeScore++;
            Player scorer = homeTeam.getRandomPlayerByPosition("FWD");
            if (scorer == null) scorer = homeTeam.getRandomPlayer();
            if (scorer != null) {
                scorer.goals++;
                events.add(new MatchEvent(minute,
                        "GOAL! " + scorer.name + " scores for " + homeTeam.name, "HOME"));
            }
        } else if (event < 0.6 + (homeProb + awayProb) * 0.3) {
            // Away team scores
            awayScore++;
            Player scorer = awayTeam.getRandomPlayerByPosition("FWD");
            if (scorer == null) scorer = awayTeam.getRandomPlayer();
            if (scorer != null) {
                scorer.goals++;
                events.add(new MatchEvent(minute,
                        "GOAL! " + scorer.name + " scores for " + awayTeam.name, "AWAY"));
            }
        } else {
            // Other events (missed chances, saves, etc.)
            String[] missEvents = {
                    "Great save by the goalkeeper!",
                    "Shot hits the post!",
                    "Missed opportunity!",
                    "Brilliant defensive tackle!",
                    "Close call! Just wide of the goal."
            };
            String eventDesc = missEvents[random.nextInt(missEvents.length)];
            events.add(new MatchEvent(minute, eventDesc, "NEUTRAL"));
        }
    }
}

// Match Animation Panel
class MatchAnimationPanel extends JPanel {
    private int ballX = 400;
    private int ballY = 200;
    private boolean homeAttack = true;
    private Timer animationTimer;

    public MatchAnimationPanel() {
        setBackground(new Color(34, 139, 34)); // Green pitch
        setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));

        animationTimer = new Timer(100, e -> {
            animateBall();
            repaint();
        });
        animationTimer.start();
    }

    private void animateBall() {
        Scanner random = null;
        if (homeAttack) {
            ballX += 5;
            if (ballX > 750) {  // When ball reaches right side
                homeAttack = false;  // Switch to away team attack
                ballY = random.nextInt(300) + 50;  // Random vertical position
            }
        } else {
            ballX -= 5;
            if (ballX < 50) {  // When ball reaches left side
                homeAttack = true;  // Switch to home team attack
                ballY = random.nextInt(300) + 50;  // Random vertical position
            }
        }

        // Add some random vertical movement for realism
        if (random.nextDouble() < 0.3) {
            ballY += random.nextInt(11) - 5; // Move -5 to +5 pixels vertically
            // Keep ball within pitch bounds (50 to 350, considering ball size)
            ballY = Math.max(50, Math.min(350, ballY));
        }
    }
// Match Animation Panel
class MatchAnimationPanel1 extends JPanel {
    private int ballX = 400;
    private int ballY = 200;
    private boolean homeAttack = true;
    private Timer animationTimer;
    private Random random = new Random();

    public MatchAnimationPanel1() {
        setBackground(new Color(34, 139, 34)); // Green pitch
        setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));

        animationTimer = new Timer(100, e -> {
            animateBall();
            repaint();
        });
        animationTimer.start();
    }

    private void animateBall() {
        if (homeAttack) {
            ballX += 5;
            if (ballX > 750) {
                homeAttack = false;
                ballY = random.nextInt(300) + 50;
            }
        } else {
            ballX -= 5;
            if (ballX < 50) {
                homeAttack = true;
                ballY = random.nextInt(300) + 50;
            }
        }

        // Add some vertical movement
        if (random.nextDouble() < 0.3) {
            ballY += random.nextInt(11) - 5; // -5 to +5
            ballY = Math.max(20, Math.min(380, ballY)); // Keep within bounds
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw pitch markings
        g.setColor(Color.WHITE);
        g.drawRect(50, 50, 700, 300); // Outer boundary
        g.drawRect(150, 50, 500, 300); // Center circle area
        g.drawLine(400, 50, 400, 350); // Halfway line

        // Draw center circle
        g.drawOval(350, 175, 100, 100);

        // Draw penalty areas
        g.drawRect(50, 125, 100, 150); // Left penalty area
        g.drawRect(650, 125, 100, 150); // Right penalty area

        // Draw goals
        g.setColor(Color.YELLOW);
        g.fillRect(45, 175, 5, 50); // Left goal
        g.fillRect(750, 175, 5, 50); // Right goal

        // Draw players
        drawPlayers(g);

        // Draw ball
        g.setColor(Color.WHITE);
        g.fillOval(ballX, ballY, 10, 10);
        g.setColor(Color.BLACK);
        g.drawOval(ballX, ballY, 10, 10);
    }

    private void drawPlayers(Graphics g) {
        // Home team players (blue)
        g.setColor(Color.BLUE);
        if (homeAttack) {
            // Attacking formation
            int[] homeX = {200, 150, 250, 150, 250, 300, 350, 400, 450, 500, 550};
            int[] homeY = {200, 100, 100, 300, 300, 150, 200, 250, 150, 200, 250};

            for (int i = 0; i < Math.min(11, homeX.length); i++) {
                g.fillOval(homeX[i], homeY[i], 15, 15);
            }
        } else {
            // Defending formation
            int[] homeX = {100, 150, 150, 150, 200, 200, 250, 250, 300, 300, 350};
            int[] homeY = {200, 100, 200, 300, 150, 250, 100, 300, 150, 250, 200};

            for (int i = 0; i < Math.min(11, homeX.length); i++) {
                g.fillOval(homeX[i], homeY[i], 15, 15);
            }
        }

        // Away team players (red)
        g.setColor(Color.RED);
        if (!homeAttack) {
            // Attacking formation
            int[] awayX = {600, 650, 550, 650, 550, 500, 450, 400, 350, 300, 250};
            int[] awayY = {200, 100, 100, 300, 300, 150, 200, 250, 150, 200, 250};

            for (int i = 0; i < Math.min(11, awayX.length); i++) {
                g.fillOval(awayX[i], awayY[i], 15, 15);
            }
        } else {
            // Defending formation
            int[] awayX = {700, 650, 650, 650, 600, 600, 550, 550, 500, 500, 450};
            int[] awayY = {200, 100, 200, 300, 150, 250, 100, 300, 150, 250, 200};

            for (int i = 0; i < Math.min(11, awayX.length); i++) {
                g.fillOval(awayX[i], awayY[i], 15, 15);
            }
        }
    }

    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }
}