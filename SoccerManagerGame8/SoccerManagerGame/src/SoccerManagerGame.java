import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.text.DecimalFormat;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.geom.*;

/**
 * Soccer Manager Game with Animation
 * A comprehensive soccer management simulation with animated match visualization
 */
public class SoccerManagerGame extends JFrame {
    // Main game variables
    private ArrayList<Team> teams;
    private ArrayList<Player> transferMarket;
    private Team userTeam;
    private League league;
    private Calendar gameCalendar;
    private int money;
    private int weekNumber;
    private int season;
    private MatchEngine matchEngine;
    private MatchVisualization matchVisualization;

    // UI Components
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JPanel dashboardPanel;
    private JPanel teamManagementPanel;
    private JPanel transfersPanel;
    private JPanel matchPanel;
    private JPanel leagueTablePanel;
    private JPanel calendarPanel;
    private JPanel trainingPanel;
    private JLabel statusLabel;
    private JLabel moneyLabel;
    private JLabel weekLabel;
    private JLabel seasonLabel;

    // Constants
    private static final int STARTING_MONEY = 10000000; // $10M
    private static final int INITIAL_TEAMS = 20;
    private static final int PLAYERS_PER_TEAM = 25;
    private static final int FREE_AGENTS = 50;
    private static final String[] POSITIONS = {"GK", "DEF", "MID", "FWD"};
    private static final String SAVE_FILE = "soccermanager_save.dat";

    /**
     * Constructor initializes the game
     */
    public SoccerManagerGame() {
        super("Soccer Manager Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        initializeGame();
        setupUI();

        setVisible(true);
    }

    /**
     * Initialize the game state
     */
    private void initializeGame() {
        teams = new ArrayList<>();
        transferMarket = new ArrayList<>();
        league = new League("Premier League");
        gameCalendar = new Calendar();
        money = STARTING_MONEY;
        weekNumber = 1;
        season = 1;

        // Initialize teams and players
        generateTeams();
        generateTransferMarket();

        // Select a random team for the user
        userTeam = teams.get(new Random().nextInt(teams.size()));
        userTeam.setUserControlled(true);

        // Initialize match engine
        matchEngine = new MatchEngine();
        matchVisualization = new MatchVisualization();

        // Generate schedule
        league.generateSchedule(teams);
    }

    /**
     * Generate initial teams
     */
    private void generateTeams() {
        String[] teamNames = {
                "Arsenal", "Chelsea", "Liverpool", "Manchester United",
                "Manchester City", "Tottenham", "Everton", "Leicester",
                "West Ham", "Newcastle", "Wolves", "Aston Villa",
                "Crystal Palace", "Brighton", "Southampton", "Burnley",
                "Watford", "Norwich", "Sheffield", "Bournemouth"
        };

        for (int i = 0; i < INITIAL_TEAMS; i++) {
            Team team = new Team(teamNames[i]);
            generatePlayersForTeam(team);
            teams.add(team);
            league.addTeam(team);
        }
    }

    /**
     * Generate players for a specific team
     */
    private void generatePlayersForTeam(Team team) {
        Random rand = new Random();

        // Generate goalkeepers (2-3)
        int goalkeepers = rand.nextInt(2) + 2;
        for (int i = 0; i < goalkeepers; i++) {
            Player player = generatePlayer("GK", 18 + rand.nextInt(20));
            team.addPlayer(player);
        }

        // Generate defenders (7-9)
        int defenders = rand.nextInt(3) + 7;
        for (int i = 0; i < defenders; i++) {
            Player player = generatePlayer("DEF", 18 + rand.nextInt(20));
            team.addPlayer(player);
        }

        // Generate midfielders (7-9)
        int midfielders = rand.nextInt(3) + 7;
        for (int i = 0; i < midfielders; i++) {
            Player player = generatePlayer("MID", 18 + rand.nextInt(20));
            team.addPlayer(player);
        }

        // Generate forwards (4-6)
        int forwards = rand.nextInt(3) + 4;
        for (int i = 0; i < forwards; i++) {
            Player player = generatePlayer("FWD", 18 + rand.nextInt(20));
            team.addPlayer(player);
        }
    }

    /**
     * Generate transfer market players
     */
    private void generateTransferMarket() {
        Random rand = new Random();
        for (int i = 0; i < FREE_AGENTS; i++) {
            String position = POSITIONS[rand.nextInt(POSITIONS.length)];
            Player player = generatePlayer(position, 18 + rand.nextInt(20));
            transferMarket.add(player);
        }
    }

    /**
     * Generate a random player
     */
    private Player generatePlayer(String position, int age) {
        String[] firstNames = {
                "James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph",
                "Thomas", "Charles", "Christopher", "Daniel", "Matthew", "Anthony", "Mark", "Donald",
                "Steven", "Paul", "Andrew", "Joshua", "Kenneth", "Kevin", "Brian", "George"
        };

        String[] lastNames = {
                "Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Garcia",
                "Rodriguez", "Wilson", "Martinez", "Anderson", "Taylor", "Thomas", "Hernandez", "Moore",
                "Martin", "Jackson", "Thompson", "White", "Lopez", "Lee", "Gonzalez", "Harris"
        };

        Random rand = new Random();
        String name = firstNames[rand.nextInt(firstNames.length)] + " " +
                lastNames[rand.nextInt(lastNames.length)];

        int overall = generateOverall(position);
        int potential = overall + rand.nextInt(15);
        if (potential > 99) potential = 99;

        int value = calculatePlayerValue(overall, age, potential);

        Player player = new Player(name, position, age, overall, potential, value);

        // Generate random attributes based on position
        generatePlayerAttributes(player, position);

        return player;
    }

    /**
     * Generate overall rating based on position
     */
    private int generateOverall(String position) {
        Random rand = new Random();
        // Base overall between 60-85
        return 60 + rand.nextInt(26);
    }

    /**
     * Calculate player transfer value
     */
    private int calculatePlayerValue(int overall, int age, int potential) {
        // Base value calculations
        int baseValue = 10000 * (int)Math.pow(overall, 2);

        // Age modifier
        double ageModifier = 1.0;
        if (age < 24) {
            ageModifier = 1.5;  // Young players are valuable
        } else if (age > 30) {
            ageModifier = 0.7;  // Older players less valuable
        }

        // Potential modifier
        double potentialModifier = 1.0 + ((potential - overall) / 50.0);

        return (int)(baseValue * ageModifier * potentialModifier);
    }

    /**
     * Generate player attributes based on position
     */
    private void generatePlayerAttributes(Player player, String position) {
        Random rand = new Random();

        // Base attributes (all players)
        PlayerAttributes attributes = new PlayerAttributes();
        attributes.setStamina(50 + rand.nextInt(50));
        attributes.setAgility(50 + rand.nextInt(50));
        attributes.setStrength(50 + rand.nextInt(50));
        attributes.setSpeed(50 + rand.nextInt(50));

        // Position-specific attributes
        switch (position) {
            case "GK":
                attributes.setGoalkeeping(70 + rand.nextInt(30));
                attributes.setReflexes(70 + rand.nextInt(30));
                attributes.setPositioning(60 + rand.nextInt(40));
                attributes.setHandling(60 + rand.nextInt(40));
                attributes.setKicking(50 + rand.nextInt(50));

                // Lower for GK
                attributes.setShooting(30 + rand.nextInt(20));
                attributes.setPassing(40 + rand.nextInt(30));
                attributes.setDribbling(30 + rand.nextInt(20));
                attributes.setTackling(30 + rand.nextInt(20));
                break;

            case "DEF":
                attributes.setTackling(70 + rand.nextInt(30));
                attributes.setMarking(70 + rand.nextInt(30));
                attributes.setHeading(60 + rand.nextInt(40));
                attributes.setPositioning(60 + rand.nextInt(40));

                attributes.setShooting(30 + rand.nextInt(40));
                attributes.setPassing(40 + rand.nextInt(40));
                attributes.setDribbling(40 + rand.nextInt(30));
                break;

            case "MID":
                attributes.setPassing(70 + rand.nextInt(30));
                attributes.setDribbling(60 + rand.nextInt(40));
                attributes.setVision(60 + rand.nextInt(40));
                attributes.setTackling(50 + rand.nextInt(50));

                attributes.setShooting(40 + rand.nextInt(50));
                attributes.setHeading(40 + rand.nextInt(40));
                break;

            case "FWD":
                attributes.setShooting(70 + rand.nextInt(30));
                attributes.setFinishing(70 + rand.nextInt(30));
                attributes.setHeading(60 + rand.nextInt(40));
                attributes.setDribbling(60 + rand.nextInt(40));

                attributes.setTackling(30 + rand.nextInt(30));
                attributes.setPassing(40 + rand.nextInt(50));
                break;
        }

        player.setAttributes(attributes);
    }

    /**
     * Set up the user interface
     */
    private void setupUI() {
        // Main panel with CardLayout for navigation
        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);

        // Create all game panels
        createDashboardPanel();
        createTeamManagementPanel();
        createTransfersPanel();
        createMatchPanel();
        createLeagueTablePanel();
        createCalendarPanel();
        createTrainingPanel();

        // Add panels to card layout
        mainPanel.add(dashboardPanel, "DASHBOARD");
        mainPanel.add(teamManagementPanel, "TEAM");
        mainPanel.add(transfersPanel, "TRANSFERS");
        mainPanel.add(matchPanel, "MATCH");
        mainPanel.add(leagueTablePanel, "LEAGUE");
        mainPanel.add(calendarPanel, "CALENDAR");
        mainPanel.add(trainingPanel, "TRAINING");

        // Navigation panel at the top
        JPanel navPanel = createNavigationPanel();

        // Status bar at the bottom
        JPanel statusPanel = createStatusBar();

        // Add everything to the frame
        setLayout(new BorderLayout());
        add(navPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        // Start with dashboard
        cardLayout.show(mainPanel, "DASHBOARD");

        // Add window listeners for saving game
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveGame();
            }
        });
    }

    /**
     * Create navigation panel with buttons
     */
    private JPanel createNavigationPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(40, 45, 60));
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        String[] buttonLabels = {"Dashboard", "Team", "Transfers", "Match", "League", "Calendar", "Training"};
        String[] cardNames = {"DASHBOARD", "TEAM", "TRANSFERS", "MATCH", "LEAGUE", "CALENDAR", "TRAINING"};

        for (int i = 0; i < buttonLabels.length; i++) {
            JButton button = new JButton(buttonLabels[i]);
            button.setFocusPainted(false);
            button.setBackground(new Color(70, 80, 100));
            button.setForeground(Color.WHITE);
            button.setBorderPainted(false);

            final String cardName = cardNames[i];
            button.addActionListener(e -> cardLayout.show(mainPanel, cardName));

            panel.add(button);
        }

        return panel;
    }

    /**
     * Create status bar
     */
    private JPanel createStatusBar() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(40, 45, 60));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10));

        // Team
        JLabel teamLabel = new JLabel("Team: " + userTeam.getName());
        teamLabel.setForeground(Color.WHITE);

        // Money
        moneyLabel = new JLabel("Budget: $" + formatMoney(money));
        moneyLabel.setForeground(Color.WHITE);

        // Week
        weekLabel = new JLabel("Week: " + weekNumber);
        weekLabel.setForeground(Color.WHITE);

        // Season
        seasonLabel = new JLabel("Season: " + season);
        seasonLabel.setForeground(Color.WHITE);

        // Status
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(Color.WHITE);

        panel.add(teamLabel);
        panel.add(moneyLabel);
        panel.add(weekLabel);
        panel.add(seasonLabel);
        panel.add(statusLabel);

        return panel;
    }

    /**
     * Format money with commas
     */
    private String formatMoney(int amount) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(amount);
    }

    /**
     * Create dashboard panel
     */
    private void createDashboardPanel() {
        dashboardPanel = new JPanel();
        dashboardPanel.setLayout(new BorderLayout());
        dashboardPanel.setBackground(new Color(240, 240, 245));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(50, 60, 80));
        headerPanel.setLayout(new BorderLayout());

        JLabel headerLabel = new JLabel("  " + userTeam.getName() + " - Manager Dashboard", SwingConstants.LEFT);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 0));

        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Dashboard content
        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(new Color(240, 240, 245));
        contentPanel.setLayout(new GridLayout(2, 2, 15, 15));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Team summary panel
        JPanel teamSummaryPanel = createDashboardCard("Team Summary", Color.WHITE);
        JPanel teamContent = new JPanel();
        teamContent.setLayout(new BorderLayout());
        teamContent.setBackground(Color.WHITE);

        int gk = 0, def = 0, mid = 0, fwd = 0;
        int totalOvr = 0;

        for (Player p : userTeam.getPlayers()) {
            totalOvr += p.getOverall();
            switch (p.getPosition()) {
                case "GK": gk++; break;
                case "DEF": def++; break;
                case "MID": mid++; break;
                case "FWD": fwd++; break;
            }
        }

        int avgOvr = totalOvr / userTeam.getPlayers().size();

        JPanel teamInfoPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        teamInfoPanel.setBackground(Color.WHITE);

        teamInfoPanel.add(new JLabel("Squad Size:"));
        teamInfoPanel.add(new JLabel(userTeam.getPlayers().size() + " players"));

        teamInfoPanel.add(new JLabel("Average Rating:"));
        teamInfoPanel.add(new JLabel(avgOvr + " OVR"));

        teamInfoPanel.add(new JLabel("Goalkeepers:"));
        teamInfoPanel.add(new JLabel(gk + ""));

        teamInfoPanel.add(new JLabel("Defenders:"));
        teamInfoPanel.add(new JLabel(def + ""));

        teamInfoPanel.add(new JLabel("Midfielders:"));
        teamInfoPanel.add(new JLabel(mid + ""));

        teamInfoPanel.add(new JLabel("Forwards:"));
        teamInfoPanel.add(new JLabel(fwd + ""));

        teamContent.add(teamInfoPanel, BorderLayout.NORTH);
        teamSummaryPanel.add(teamContent);

        // League position panel
        JPanel leaguePosPanel = createDashboardCard("League Position", Color.WHITE);
        JPanel leagueContent = new JPanel(new BorderLayout());
        leagueContent.setBackground(Color.WHITE);

        int position = league.getTeamPosition(userTeam);
        JLabel positionLabel = new JLabel("#" + position, SwingConstants.CENTER);
        positionLabel.setFont(new Font("Arial", Font.BOLD, 48));

        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        statsPanel.setBackground(Color.WHITE);

        JLabel winsLabel = new JLabel("Wins: " + userTeam.getWins(), SwingConstants.CENTER);
        JLabel drawsLabel = new JLabel("Draws: " + userTeam.getDraws(), SwingConstants.CENTER);
        JLabel lossesLabel = new JLabel("Losses: " + userTeam.getLosses(), SwingConstants.CENTER);

        statsPanel.add(winsLabel);
        statsPanel.add(drawsLabel);
        statsPanel.add(lossesLabel);

        leagueContent.add(positionLabel, BorderLayout.CENTER);
        leagueContent.add(statsPanel, BorderLayout.SOUTH);
        leaguePosPanel.add(leagueContent);

        // Upcoming match panel
        JPanel upcomingMatchPanel = createDashboardCard("Next Match", Color.WHITE);
        JPanel matchContent = new JPanel(new BorderLayout());
        matchContent.setBackground(Color.WHITE);

        Match nextMatch = gameCalendar.getNextMatch(userTeam);
        JLabel vsLabel;

        if (nextMatch != null) {
            String opponent = nextMatch.getHomeTeam().equals(userTeam) ?
                    nextMatch.getAwayTeam().getName() : nextMatch.getHomeTeam().getName();
            String venue = nextMatch.getHomeTeam().equals(userTeam) ? "Home" : "Away";

            vsLabel = new JLabel("<html>vs. <b>" + opponent + "</b><br>" + venue + "</html>", SwingConstants.CENTER);
        } else {
            vsLabel = new JLabel("No upcoming matches", SwingConstants.CENTER);
        }

        vsLabel.setFont(new Font("Arial", Font.BOLD, 20));
        matchContent.add(vsLabel, BorderLayout.CENTER);

        JButton playMatchButton = new JButton("Play Match");
        playMatchButton.addActionListener(e -> {
            if (nextMatch != null) {
                playMatch(nextMatch);
            } else {
                JOptionPane.showMessageDialog(this, "No upcoming matches scheduled.");
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(playMatchButton);
        matchContent.add(buttonPanel, BorderLayout.SOUTH);

        upcomingMatchPanel.add(matchContent);

        // Finance panel
        JPanel financePanel = createDashboardCard("Finances", Color.WHITE);
        JPanel financeContent = new JPanel(new BorderLayout());
        financeContent.setBackground(Color.WHITE);

        JLabel budgetLabel = new JLabel("$" + formatMoney(money), SwingConstants.CENTER);
        budgetLabel.setFont(new Font("Arial", Font.BOLD, 32));
        budgetLabel.setForeground(new Color(50, 150, 50));

        JPanel financeDetailsPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        financeDetailsPanel.setBackground(Color.WHITE);

        financeDetailsPanel.add(new JLabel("Weekly Wage:"));
        financeDetailsPanel.add(new JLabel("$" + formatMoney(userTeam.calculateWeeklyWage())));

        financeDetailsPanel.add(new JLabel("Match Income:"));
        financeDetailsPanel.add(new JLabel("$" + formatMoney(calculateMatchIncome())));

        financeDetailsPanel.add(new JLabel("Sponsorships:"));
        financeDetailsPanel.add(new JLabel("$" + formatMoney(calculateSponsorIncome())));

        financeDetailsPanel.add(new JLabel("Net Weekly:"));
        int netWeekly = calculateNetWeeklyIncome();
        JLabel netLabel = new JLabel("$" + formatMoney(netWeekly));
        netLabel.setForeground(netWeekly >= 0 ? new Color(50, 150, 50) : new Color(200, 50, 50));
        financeDetailsPanel.add(netLabel);

        financeContent.add(budgetLabel, BorderLayout.CENTER);
        financeContent.add(financeDetailsPanel, BorderLayout.SOUTH);

        financePanel.add(financeContent);

        // Add all panels to the dashboard
        contentPanel.add(teamSummaryPanel);
        contentPanel.add(leaguePosPanel);
        contentPanel.add(upcomingMatchPanel);
        contentPanel.add(financePanel);

        dashboardPanel.add(headerPanel, BorderLayout.NORTH);
        dashboardPanel.add(contentPanel, BorderLayout.CENTER);

        // Footer with advance button
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setBackground(new Color(240, 240, 245));

        JButton advanceButton = new JButton("Advance Week");
        advanceButton.addActionListener(e -> advanceWeek());
        footerPanel.add(advanceButton);

        dashboardPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    /**
     * Create a card panel for the dashboard
     */
    private JPanel createDashboardCard(String title, Color bgColor) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(60, 60, 60));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBackground(bgColor);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Calculate match income
     */
    private int calculateMatchIncome() {
        // Base on team popularity, league position, etc.
        int baseIncome = 100000;
        int leaguePosition = league.getTeamPosition(userTeam);

        // Better position = more income
        double positionModifier = 1.0 + ((INITIAL_TEAMS - leaguePosition) / 20.0);

        return (int)(baseIncome * positionModifier);
    }

    /**
     * Calculate sponsor income
     */
    private int calculateSponsorIncome() {
        // Base on team performance, league position, etc.
        int baseIncome = 50000;
        int leaguePosition = league.getTeamPosition(userTeam);

        // Better position = more income
        double positionModifier = 1.0 + ((INITIAL_TEAMS - leaguePosition) / 10.0);

        return (int)(baseIncome * positionModifier);
    }

    /**
     * Calculate net weekly income
     */
    private int calculateNetWeeklyIncome() {
        return calculateMatchIncome() + calculateSponsorIncome() - userTeam.calculateWeeklyWage();
    }

    /**
     * Create team management panel
     */
    private void createTeamManagementPanel() {
        teamManagementPanel = new JPanel();
        teamManagementPanel.setLayout(new BorderLayout());
        teamManagementPanel.setBackground(new Color(240, 240, 245));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(50, 60, 80));
        headerPanel.setLayout(new BorderLayout());

        JLabel headerLabel = new JLabel("  Team Management", SwingConstants.LEFT);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 0));

        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Team content
        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(new Color(240, 240, 245));
        contentPanel.setLayout(new BorderLayout(15, 15));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Player list
        JPanel playerListPanel = new JPanel(new BorderLayout());
        playerListPanel.setBackground(Color.WHITE);
        playerListPanel.setBorder(BorderFactory.createTitledBorder("Squad Players"));

        String[] columns = {"Name", "Pos", "Age", "OVR", "POT", "Value", "Wage"};
        Object[][] data = new Object[userTeam.getPlayers().size()][columns.length];

        for (int i = 0; i < userTeam.getPlayers().size(); i++) {
            Player p = userTeam.getPlayers().get(i);
            data[i][0] = p.getName();
            data[i][1] = p.getPosition();
            data[i][2] = p.getAge();
            data[i][3] = p.getOverall();
            data[i][4] = p.getPotential();
            data[i][5] = "$" + formatMoney(p.getValue());
            data[i][6] = "$" + formatMoney(p.getWage());
        }

        JTable playerTable = new JTable(data, columns);
        playerTable.setFillsViewportHeight(true);
        playerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(playerTable);
        playerListPanel.add(scrollPane, BorderLayout.CENTER);

        // Player detail panel
        JPanel playerDetailPanel = new JPanel(new BorderLayout());
        playerDetailPanel.setBackground(Color.WHITE);
        playerDetailPanel.setBorder(BorderFactory.createTitledBorder("Player Details"));

        JPanel detailsContent = new JPanel(new BorderLayout());
        detailsContent.setBackground(Color.WHITE);

        JPanel playerInfoPanel = new JPanel();
        playerInfoPanel.setLayout(new BoxLayout(playerInfoPanel, BoxLayout.Y_AXIS));
        playerInfoPanel.setBackground(Color.WHITE);
        playerInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel playerNameLabel = new JLabel("Select a player to view details");
        playerNameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        playerInfoPanel.add(playerNameLabel);

        JPanel attributesPanel = new JPanel(new GridLayout(0, 2));
        attributesPanel.setBackground(Color.WHITE);

        detailsContent.add(playerInfoPanel, BorderLayout.NORTH);
        detailsContent.add(attributesPanel, BorderLayout.CENTER);

        playerDetailPanel.add(detailsContent);

        // Add selection listener to show player details
        playerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && playerTable.getSelectedRow() != -1) {
                Player selected = userTeam.getPlayers().get(playerTable.getSelectedRow());
                updatePlayerDetailPanel(playerInfoPanel, attributesPanel, selected);
            }
        });

        // Formation panel
        JPanel formationPanel = new JPanel(new BorderLayout());
        formationPanel.setBackground(Color.WHITE);
        formationPanel.setBorder(BorderFactory.createTitledBorder("Formation"));

        // Simple formation visualization
        FormationPanel formationVisual = new FormationPanel(userTeam);
        formationPanel.add(formationVisual, BorderLayout.CENTER);

        // Add panels to content
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(playerListPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 0, 15));
        rightPanel.add(playerDetailPanel);
        rightPanel.add(formationPanel);

        contentPanel.add(leftPanel, BorderLayout.CENTER);
        contentPanel.add(rightPanel, BorderLayout.EAST);

        teamManagementPanel.add(headerPanel, BorderLayout.NORTH);
        teamManagementPanel.add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Update player detail panel with selected player info
     */
    private void updatePlayerDetailPanel(JPanel infoPanel, JPanel attributesPanel, Player player) {
        // Clear panels
        infoPanel.removeAll();
        attributesPanel.removeAll();

        // Add player info
        JLabel nameLabel = new JLabel(player.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 18));

        JLabel posAgeLabel = new JLabel(player.getPosition() + " | " + player.getAge() + " years old");
        JLabel ratingLabel = new JLabel("Rating: " + player.getOverall() + " OVR | Potential: " + player.getPotential() + " POT");
        JLabel valueLabel = new JLabel("Value: $" + formatMoney(player.getValue()) + " | Wage: $" + formatMoney(player.getWage()) + "/week");

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(posAgeLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(ratingLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(valueLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Add attributes
        JLabel attributesHeader = new JLabel("Player Attributes");
        attributesHeader.setFont(new Font("Arial", Font.BOLD, 14));
        infoPanel.add(attributesHeader);

        PlayerAttributes attr = player.getAttributes();

        // Key attributes based on position
        if (player.getPosition().equals("GK")) {
            addAttributeBar(attributesPanel, "Goalkeeping", attr.getGoalkeeping());
            addAttributeBar(attributesPanel, "Reflexes", attr.getReflexes());
            addAttributeBar(attributesPanel, "Handling", attr.getHandling());
            addAttributeBar(attributesPanel, "Positioning", attr.getPositioning());
            addAttributeBar(attributesPanel, "Kicking", attr.getKicking());
        } else if (player.getPosition().equals("DEF")) {
            addAttributeBar(attributesPanel, "Tackling", attr.getTackling());
            addAttributeBar(attributesPanel, "Marking", attr.getMarking());
            addAttributeBar(attributesPanel, "Heading", attr.getHeading());
            addAttributeBar(attributesPanel, "Positioning", attr.getPositioning());
            addAttributeBar(attributesPanel, "Strength", attr.getStrength());
        } else if (player.getPosition().equals("MID")) {
            addAttributeBar(attributesPanel, "Passing", attr.getPassing());
            addAttributeBar(attributesPanel, "Dribbling", attr.getDribbling());
            addAttributeBar(attributesPanel, "Vision", attr.getVision());
            addAttributeBar(attributesPanel, "Tackling", attr.getTackling());
            addAttributeBar(attributesPanel, "Stamina", attr.getStamina());
        } else if (player.getPosition().equals("FWD")) {
            addAttributeBar(attributesPanel, "Shooting", attr.getShooting());
            addAttributeBar(attributesPanel, "Finishing", attr.getFinishing());
            addAttributeBar(attributesPanel, "Dribbling", attr.getDribbling());
addAttributeBar(attributesPanel, "Heading", attr.getHeading());
addAttributeBar(attributesPanel, "Speed", attr.getSpeed());
        }

// Refresh panels
        infoPanel.revalidate();
infoPanel.repaint();
attributesPanel.revalidate();
attributesPanel.repaint();
}

/**
 * Add an attribute bar to the attributes panel
 */
private void addAttributeBar(JPanel panel, String attributeName, int value) {
    JPanel attrPanel = new JPanel(new BorderLayout());
    attrPanel.setBackground(Color.WHITE);

    JLabel nameLabel = new JLabel(attributeName);
    JLabel valueLabel = new JLabel(value + "");
    valueLabel.setForeground(new Color(70, 70, 70));

    JProgressBar bar = new JProgressBar(0, 100);
    bar.setValue(value);
    bar.setStringPainted(true);
    bar.setForeground(getAttributeColor(value));

    attrPanel.add(nameLabel, BorderLayout.WEST);
    attrPanel.add(bar, BorderLayout.CENTER);
    attrPanel.add(valueLabel, BorderLayout.EAST);
    attrPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

    panel.add(attrPanel);
}

/**
 * Get color for attribute value
 */
private Color getAttributeColor(int value) {
    if (value >= 80) return new Color(50, 180, 50);    // Green for excellent
    if (value >= 70) return new Color(100, 180, 100);  // Light green for good
    if (value >= 60) return new Color(200, 180, 50);   // Yellow for average
    if (value >= 50) return new Color(220, 150, 50);   // Orange for below average
    return new Color(220, 80, 80);                     // Red for poor
}

/**
 * Create transfers panel
 */
private void createTransfersPanel() {
    transfersPanel = new JPanel();
    transfersPanel.setLayout(new BorderLayout());
    transfersPanel.setBackground(new Color(240, 240, 245));

    JPanel headerPanel = new JPanel();
    headerPanel.setBackground(new Color(50, 60, 80));
    headerPanel.setLayout(new BorderLayout());

    JLabel headerLabel = new JLabel("  Transfer Market", SwingConstants.LEFT);
    headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
    headerLabel.setForeground(Color.WHITE);
    headerLabel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 0));

    headerPanel.add(headerLabel, BorderLayout.CENTER);

    // Content panel with tabs
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.setBackground(new Color(240, 240, 245));

    // Transfer Market tab
    JPanel marketPanel = createTransferMarketTab();
    tabbedPane.addTab("Transfer Market", marketPanel);

    // My Team tab
    JPanel myTeamPanel = createMyTeamTab();
    tabbedPane.addTab("My Team", myTeamPanel);

    transfersPanel.add(headerPanel, BorderLayout.NORTH);
    transfersPanel.add(tabbedPane, BorderLayout.CENTER);
}

/**
 * Create transfer market tab
 */
private JPanel createTransferMarketTab() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(Color.WHITE);

    // Search/filter panel
    JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    filterPanel.setBackground(Color.WHITE);

    JComboBox<String> positionFilter = new JComboBox<>(new String[]{"All", "GK", "DEF", "MID", "FWD"});
    JComboBox<String> ratingFilter = new JComboBox<>(new String[]{"All", "70+", "75+", "80+", "85+"});
    JButton searchButton = new JButton("Search");

    filterPanel.add(new JLabel("Position:"));
    filterPanel.add(positionFilter);
    filterPanel.add(new JLabel("Rating:"));
    filterPanel.add(ratingFilter);
    filterPanel.add(searchButton);

    // Player table
    String[] columns = {"Name", "Pos", "Age", "OVR", "POT", "Value", "Wage", "Action"};
    Object[][] data = new Object[transferMarket.size()][columns.length];

    for (int i = 0; i < transferMarket.size(); i++) {
        Player p = transferMarket.get(i);
        data[i][0] = p.getName();
        data[i][1] = p.getPosition();
        data[i][2] = p.getAge();
        data[i][3] = p.getOverall();
        data[i][4] = p.getPotential();
        data[i][5] = "$" + formatMoney(p.getValue());
        data[i][6] = "$" + formatMoney(p.getWage());
        data[i][7] = "Buy";
    }

    JTable marketTable = new JTable(data, columns);
    marketTable.setFillsViewportHeight(true);

    // Add button renderer and editor for buy action
    marketTable.getColumn("Action").setCellRenderer(new ButtonRenderer());
    marketTable.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox(), marketTable));

    JScrollPane scrollPane = new JScrollPane(marketTable);

    panel.add(filterPanel, BorderLayout.NORTH);
    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
}

/**
 * Button renderer for transfer table
 */
class ButtonRenderer extends JButton implements TableCellRenderer {
    public ButtonRenderer() {
        setOpaque(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        setText((value == null) ? "" : value.toString());
        return this;
    }
}

/**
 * Button editor for transfer table
 */
class ButtonEditor extends DefaultCellEditor {
    private JButton button;
    private String label;
    private boolean isPushed;
    private JTable table;

    public ButtonEditor(JCheckBox checkBox, JTable table) {
        super(checkBox);
        this.table = table;
        button = new JButton();
        button.setOpaque(true);
        button.addActionListener(e -> fireEditingStopped());
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        label = (value == null) ? "" : value.toString();
        button.setText(label);
        isPushed = true;
        return button;
    }

    public Object getCellEditorValue() {
        if (isPushed) {
            int row = table.getEditingRow();
            Player player = transferMarket.get(row);
            attemptTransfer(player);
        }
        isPushed = false;
        return label;
    }

    public boolean stopCellEditing() {
        isPushed = false;
        return super.stopCellEditing();
    }
}

/**
 * Attempt to transfer a player
 */
private void attemptTransfer(Player player) {
    if (money >= player.getValue()) {
        int response = JOptionPane.showConfirmDialog(this,
                "Buy " + player.getName() + " for $" + formatMoney(player.getValue()) + "?",
                "Confirm Transfer",
                JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.YES_OPTION) {
            // Complete transfer
            money -= player.getValue();
            userTeam.addPlayer(player);
            transferMarket.remove(player);
            updateStatus("Transfer completed: " + player.getName() + " joined your team!");
            refreshUI();
        }
    } else {
        JOptionPane.showMessageDialog(this,
                "Insufficient funds! You need $" + formatMoney(player.getValue() - money) + " more.",
                "Transfer Failed",
                JOptionPane.ERROR_MESSAGE);
    }
}

/**
 * Create my team tab for transfers
 */
private JPanel createMyTeamTab() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(Color.WHITE);

    String[] columns = {"Name", "Pos", "Age", "OVR", "POT", "Value", "Wage", "Action"};
    Object[][] data = new Object[userTeam.getPlayers().size()][columns.length];

    for (int i = 0; i < userTeam.getPlayers().size(); i++) {
        Player p = userTeam.getPlayers().get(i);
        data[i][0] = p.getName();
        data[i][1] = p.getPosition();
        data[i][2] = p.getAge();
        data[i][3] = p.getOverall();
        data[i][4] = p.getPotential();
        data[i][5] = "$" + formatMoney(p.getValue());
        data[i][6] = "$" + formatMoney(p.getWage());
        data[i][7] = "Sell";
    }

    JTable teamTable = new JTable(data, columns);
    teamTable.setFillsViewportHeight(true);

    // Add sell button functionality
    teamTable.getColumn("Action").setCellRenderer(new SellButtonRenderer());
    teamTable.getColumn("Action").setCellEditor(new SellButtonEditor(new JCheckBox(), teamTable));

    JScrollPane scrollPane = new JScrollPane(teamTable);
    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
}

/**
 * Sell button renderer
 */
class SellButtonRenderer extends JButton implements TableCellRenderer {
    public SellButtonRenderer() {
        setOpaque(true);
        setBackground(new Color(220, 100, 100));
        setForeground(Color.WHITE);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        setText((value == null) ? "" : value.toString());
        return this;
    }
}

/**
 * Sell button editor
 */
class SellButtonEditor extends DefaultCellEditor {
    private JButton button;
    private String label;
    private boolean isPushed;
    private JTable table;

    public SellButtonEditor(JCheckBox checkBox, JTable table) {
        super(checkBox);
        this.table = table;
        button = new JButton();
        button.setOpaque(true);
        button.setBackground(new Color(220, 100, 100));
        button.setForeground(Color.WHITE);
        button.addActionListener(e -> fireEditingStopped());
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        label = (value == null) ? "" : value.toString();
        button.setText(label);
        isPushed = true;
        return button;
    }

    public Object getCellEditorValue() {
        if (isPushed) {
            int row = table.getEditingRow();
            Player player = userTeam.getPlayers().get(row);
            attemptSellPlayer(player);
        }
        isPushed = false;
        return label;
    }

    public boolean stopCellEditing() {
        isPushed = false;
        return super.stopCellEditing();
    }
}

/**
 * Attempt to sell a player
 */
private void attemptSellPlayer(Player player) {
    int sellPrice = (int)(player.getValue() * 0.9); // 10% commission

    int response = JOptionPane.showConfirmDialog(this,
            "Sell " + player.getName() + " for $" + formatMoney(sellPrice) + "?",
            "Confirm Sale",
            JOptionPane.YES_NO_OPTION);

    if (response == JOptionPane.YES_OPTION) {
        money += sellPrice;
        userTeam.removePlayer(player);
        transferMarket.add(player);
        updateStatus("Player sold: " + player.getName() + " for $" + formatMoney(sellPrice));
        refreshUI();
    }
}

/**
 * Create match panel
 */
private void createMatchPanel() {
    matchPanel = new JPanel();
    matchPanel.setLayout(new BorderLayout());
    matchPanel.setBackground(new Color(240, 240, 245));

    JPanel headerPanel = new JPanel();
    headerPanel.setBackground(new Color(50, 60, 80));
    headerPanel.setLayout(new BorderLayout());

    JLabel headerLabel = new JLabel("  Match Center", SwingConstants.LEFT);
    headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
    headerLabel.setForeground(Color.WHITE);
    headerLabel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 0));

    headerPanel.add(headerLabel, BorderLayout.CENTER);

    // Match content
    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBackground(new Color(240, 240, 245));
    contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    // Match visualization
    JPanel matchVisualPanel = new JPanel(new BorderLayout());
    matchVisualPanel.setBackground(Color.WHITE);
    matchVisualPanel.setBorder(BorderFactory.createTitledBorder("Match Preview"));
    matchVisualPanel.add(matchVisualization, BorderLayout.CENTER);

    // Match controls
    JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    controlPanel.setBackground(Color.WHITE);

    JButton playMatchButton = new JButton("Play Next Match");
    playMatchButton.addActionListener(e -> {
        Match nextMatch = gameCalendar.getNextMatch(userTeam);
        if (nextMatch != null) {
            playMatch(nextMatch);
        } else {
            JOptionPane.showMessageDialog(this, "No upcoming matches scheduled.");
        }
    });

    JButton simMatchButton = new JButton("Simulate Match");
    simMatchButton.addActionListener(e -> {
        Match nextMatch = gameCalendar.getNextMatch(userTeam);
        if (nextMatch != null) {
            simulateMatch(nextMatch);
        } else {
            JOptionPane.showMessageDialog(this, "No upcoming matches scheduled.");
        }
    });

    controlPanel.add(playMatchButton);
    controlPanel.add(simMatchButton);

    contentPanel.add(matchVisualPanel, BorderLayout.CENTER);
    contentPanel.add(controlPanel, BorderLayout.SOUTH);

    matchPanel.add(headerPanel, BorderLayout.NORTH);
    matchPanel.add(contentPanel, BorderLayout.CENTER);
}

/**
 * Create league table panel
 */
private void createLeagueTablePanel() {
    leagueTablePanel = new JPanel();
    leagueTablePanel.setLayout(new BorderLayout());
    leagueTablePanel.setBackground(new Color(240, 240, 245));

    JPanel headerPanel = new JPanel();
    headerPanel.setBackground(new Color(50, 60, 80));
    headerPanel.setLayout(new BorderLayout());

    JLabel headerLabel = new JLabel("  League Table", SwingConstants.LEFT);
    headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
    headerLabel.setForeground(Color.WHITE);
    headerLabel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 0));

    headerPanel.add(headerLabel, BorderLayout.CENTER);

    // League table content
    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBackground(Color.WHITE);
    contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    String[] columns = {"Pos", "Team", "Played", "Won", "Drawn", "Lost", "GF", "GA", "GD", "Points"};
    Object[][] data = league.getLeagueTableData();

    JTable leagueTable = new JTable(data, columns);
    leagueTable.setFillsViewportHeight(true);

    // Highlight user team
    leagueTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String teamName = (String) table.getValueAt(row, 1);
            if (teamName.equals(userTeam.getName())) {
                c.setBackground(new Color(220, 240, 255));
            } else {
                c.setBackground(Color.WHITE);
            }

            return c;
        }
    });

    JScrollPane scrollPane = new JScrollPane(leagueTable);
    contentPanel.add(scrollPane, BorderLayout.CENTER);

    leagueTablePanel.add(headerPanel, BorderLayout.NORTH);
    leagueTablePanel.add(contentPanel, BorderLayout.CENTER);
}

/**
 * Create calendar panel
 */
private void createCalendarPanel() {
    calendarPanel = new JPanel();
    calendarPanel.setLayout(new BorderLayout());
    calendarPanel.setBackground(new Color(240, 240, 245));

    JPanel headerPanel = new JPanel();
    headerPanel.setBackground(new Color(50, 60, 80));
    headerPanel.setLayout(new BorderLayout());

    JLabel headerLabel = new JLabel("  Fixture Calendar", SwingConstants.LEFT);
    headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
    headerLabel.setForeground(Color.WHITE);
    headerLabel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 0));

    headerPanel.add(headerLabel, BorderLayout.CENTER);

    // Calendar content
    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBackground(Color.WHITE);
    contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    // Get upcoming fixtures
    ArrayList<Match> fixtures = gameCalendar.getUpcomingFixtures(userTeam, 10);
    String[] columns = {"Date", "Home Team", "Away Team", "Result"};
    Object[][] data = new Object[fixtures.size()][columns.length];

    for (int i = 0; i < fixtures.size(); i++) {
        Match match = fixtures.get(i);
        data[i][0] = "Week " + match.getWeek();
        data[i][1] = match.getHomeTeam().getName();
        data[i][2] = match.getAwayTeam().getName();
        data[i][3] = match.isPlayed() ? match.getHomeScore() + " - " + match.getAwayScore() : "TBD";
    }

    JTable calendarTable = new JTable(data, columns);
    calendarTable.setFillsViewportHeight(true);

    JScrollPane scrollPane = new JScrollPane(calendarTable);
    contentPanel.add(scrollPane, BorderLayout.CENTER);

    calendarPanel.add(headerPanel, BorderLayout.NORTH);
    calendarPanel.add(contentPanel, BorderLayout.CENTER);
}

/**
 * Create training panel
 */
private void createTrainingPanel() {
    trainingPanel = new JPanel();
    trainingPanel.setLayout(new BorderLayout());
    trainingPanel.setBackground(new Color(240, 240, 245));

    JPanel headerPanel = new JPanel();
    headerPanel.setBackground(new Color(50, 60, 80));
    headerPanel.setLayout(new BorderLayout());

    JLabel headerLabel = new JLabel("  Training Center", SwingConstants.LEFT);
    headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
    headerLabel.setForeground(Color.WHITE);
    headerLabel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 0));

    headerPanel.add(headerLabel, BorderLayout.CENTER);

    // Training content
    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBackground(new Color(240, 240, 245));
    contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    JLabel trainingInfo = new JLabel("<html><center>Training features will be implemented in future updates.<br>"
            + "Players will improve their attributes through training sessions.</center></html>",
            SwingConstants.CENTER);
    trainingInfo.setFont(new Font("Arial", Font.PLAIN, 16));

    contentPanel.add(trainingInfo, BorderLayout.CENTER);

    trainingPanel.add(headerPanel, BorderLayout.NORTH);
    trainingPanel.add(contentPanel, BorderLayout.CENTER);
}

/**
 * Play a match with animation
 */
private void playMatch(Match match) {
    // Show match preparation dialog
    int option = JOptionPane.showConfirmDialog(this,
            "Play match against " + (match.getHomeTeam().equals(userTeam) ?
                    match.getAwayTeam().getName() : match.getHomeTeam().getName()) + "?",
            "Match Preparation",
            JOptionPane.YES_NO_OPTION);

    if (option == JOptionPane.YES_OPTION) {
        // Switch to match panel
        cardLayout.show(mainPanel, "MATCH");

        // Start the match with visualization
        matchEngine.playMatch(match, matchVisualization);

        // Update game state after match
        updateAfterMatch(match);
    }
}

/**
 * Simulate a match without animation
 */
private void simulateMatch(Match match) {
    matchEngine.simulateMatch(match);
    updateAfterMatch(match);

    String result = match.getHomeTeam().getName() + " " + match.getHomeScore() +
            " - " + match.getAwayScore() + " " + match.getAwayTeam().getName();

    JOptionPane.showMessageDialog(this,
            "Match Result:\n" + result,
            "Match Simulation",
            JOptionPane.INFORMATION_MESSAGE);
}

/**
 * Update game state after a match
 */
private void updateAfterMatch(Match match) {
    // Update team stats
    match.getHomeTeam().updateMatchStats(match.getHomeScore(), match.getAwayScore());
    match.getAwayTeam().updateMatchStats(match.getAwayScore(), match.getHomeScore());

    // Award money for match
    int matchIncome = calculateMatchIncome();
    money += matchIncome;

    // Pay wages
    int weeklyWage = userTeam.calculateWeeklyWage();
    money -= weeklyWage;

    // Advance week
    advanceWeek();

    updateStatus("Match completed! Income: $" + formatMoney(matchIncome) +
            " | Wages: $" + formatMoney(weeklyWage));
}

/**
 * Advance to the next week
 */
private void advanceWeek() {
    weekNumber++;

    // Check if season ended
    if (weekNumber > 38) { // Typical league season
        season++;
        weekNumber = 1;
        league.generateSchedule(teams); // New season schedule
        updateStatus("New season started! Season " + season);
    } else {
        updateStatus("Advanced to week " + weekNumber);
    }

    // Player development
    for (Player player : userTeam.getPlayers()) {
        player.develop(); // Players improve over time
    }

    refreshUI();
}

/**
 * Update status message
 */
private void updateStatus(String message) {
    statusLabel.setText(message);
}

/**
 * Refresh UI components
 */
private void refreshUI() {
    // Update money label
    moneyLabel.setText("Budget: $" + formatMoney(money));

    // Update week and season labels
    weekLabel.setText("Week: " + weekNumber);
    seasonLabel.setText("Season: " + season);

    // Refresh all panels
    dashboardPanel.removeAll();
    createDashboardPanel();

    teamManagementPanel.removeAll();
    createTeamManagementPanel();

    transfersPanel.removeAll();
    createTransfersPanel();

    leagueTablePanel.removeAll();
    createLeagueTablePanel();

    calendarPanel.removeAll();
    createCalendarPanel();

    // Repaint
    revalidate();
    repaint();
}

/**
 * Save game state
 */
private void saveGame() {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
        // Save game state
        HashMap<String, Object> saveData = new HashMap<>();
        saveData.put("teams", teams);
        saveData.put("userTeam", userTeam);
        saveData.put("money", money);
        saveData.put("weekNumber", weekNumber);
        saveData.put("season", season);

        oos.writeObject(saveData);
        updateStatus("Game saved successfully!");
    } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "Error saving game: " + e.getMessage(),
                "Save Error", JOptionPane.ERROR_MESSAGE);
    }
}

/**
 * Load game state
 */
private void loadGame() {
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
        @SuppressWarnings("unchecked")
        HashMap<String, Object> saveData = (HashMap<String, Object>) ois.readObject();

        teams = (ArrayList<Team>) saveData.get("teams");
        userTeam = (Team) saveData.get("userTeam");
        money = (Integer) saveData.get("money");
        weekNumber = (Integer) saveData.get("weekNumber");
        season = (Integer) saveData.get("season");

        updateStatus("Game loaded successfully!");
        refreshUI();
    } catch (IOException | ClassNotFoundException e) {
        JOptionPane.showMessageDialog(this, "Error loading game: " + e.getMessage(),
                "Load Error", JOptionPane.ERROR_MESSAGE);
    }
}

/**
 * Main method
 */
public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
        } catch (Exception e) {
            e.printStackTrace();
        }
        new SoccerManagerGame();
    });
}
}

// Supporting classes (these would be in separate files in a real project)

class Team implements Serializable {
    private String name;
    private ArrayList<Player> players;
    private boolean userControlled;
    private int wins, draws, losses;
    private int goalsFor, goalsAgainst;

    public Team(String name) {
        this.name = name;
        this.players = new ArrayList<>();
        this.wins = this.draws = this.losses = 0;
        this.goalsFor = this.goalsAgainst = 0;
    }

    // Getters and setters
    public String getName() { return name; }
    public ArrayList<Player> getPlayers() { return players; }
    public boolean isUserControlled() { return userControlled; }
    public void setUserControlled(boolean userControlled) { this.userControlled = userControlled; }
    public int getWins() { return wins; }
    public int getDraws() { return draws; }
    public int getLosses() { return losses; }

    public void addPlayer(Player player) { players.add(player); }
    public void removePlayer(Player player) { players.remove(player); }

    public int calculateWeeklyWage() {
        int totalWage = 0;
        for (Player p : players) {
            totalWage += p.getWage();
        }
        return totalWage;
    }

    public void updateMatchStats(int goalsFor, int goalsAgainst) {
        this.goalsFor += goalsFor;
        this.goalsAgainst += goalsAgainst;

        if (goalsFor > goalsAgainst) {
            wins++;
        } else if (goalsFor == goalsAgainst) {
            draws++;
        } else {
            losses++;
        }
    }

    public int getPoints() {
        return wins * 3 + draws;
    }

    public int getGoalDifference() {
        return goalsFor - goalsAgainst;
    }

    public Object getGoalsFor() {
    }

    public Object getGoalsAgainst() {
    }
}

class Player implements Serializable {
    private String name;
    private String position;
    private int age;
    private int overall;
    private int potential;
    private int value;
    private int wage;
    private PlayerAttributes attributes;

    public Player(String name, String position, int age, int overall, int potential, int value) {
        this.name = name;
        this.position = position;
        this.age = age;
        this.overall = overall;
        this.potential = potential;
        this.value = value;
        this.wage = value / 1000; // Simplified wage calculation
        this.attributes = new PlayerAttributes();
    }

    // Getters and setters
    public String getName() { return name; }
    public String getPosition() { return position; }
    public int getAge() { return age; }
    public int getOverall() { return overall; }
    public int getPotential() { return potential; }
    public int getValue() { return value; }
    public int getWage() { return wage; }
    public PlayerAttributes getAttributes() { return attributes; }
    public void setAttributes(PlayerAttributes attributes) { this.attributes = attributes; }

    public void develop() {
        // Young players have higher chance to improve
        if (age < 25 && overall < potential) {
            Random rand = new Random();
            if (rand.nextDouble() < 0.3) { // 30% chance to improve each week
                overall++;
                value = (int)(value * 1.1); // Value increases with improvement
            }
        }
    }
}

class PlayerAttributes implements Serializable {
    private int shooting;
    private int passing;
    private int dribbling;
    private int tackling;
    private int heading;
    private int positioning;
    private int goalkeeping;
    private int reflexes;
    private int handling;
    private int kicking;
    private int marking;
    private int vision;
    private int finishing;
    private int stamina;
    private int agility;
    private int strength;
    private int speed;

    // Getters and setters for all attributes
    public int getShooting() { return shooting; }
    public void setShooting(int shooting) { this.shooting = shooting; }
    public int getPassing() { return passing; }
    public void setPassing(int passing) { this.passing = passing; }
    public int getDribbling() { return dribbling; }
    public void setDribbling(int dribbling) { this.dribbling = dribbling; }
    public int getTackling() { return tackling; }
    public void setTackling(int tackling) { this.tackling = tackling; }
    public int getHeading() { return heading; }
    public void setHeading(int heading) { this.heading = heading; }
    public int getPositioning() { return positioning; }
    public void setPositioning(int positioning) { this.positioning = positioning; }
    public int getGoalkeeping() { return goalkeeping; }
    public void setGoalkeeping(int goalkeeping) { this.goalkeeping = goalkeeping; }
    public int getReflexes() { return reflexes; }
    public void setReflexes(int reflexes) { this.reflexes = reflexes; }
    public int getHandling() { return handling; }
    public void setHandling(int handling) { this.handling = handling; }
    public int getKicking() { return kicking; }
    public void setKicking(int kicking) { this.kicking = kicking; }
    public int getMarking() { return marking; }
    public void setMarking(int marking) { this.marking = marking; }
    public int getVision() { return vision; }
    public void setVision(int vision) { this.vision = vision; }
    public int getFinishing() { return finishing; }
    public void setFinishing(int finishing) { this.finishing = finishing; }
    public int getStamina() { return stamina; }
    public void setStamina(int stamina) { this.stamina = stamina; }
    public int getAgility() { return agility; }
    public void setAgility(int agility) { this.agility = agility; }
    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; }
    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }
}

class League implements Serializable {
    private String name;
    private ArrayList<Team> teams;
    private ArrayList<Match> matches;

    public League(String name) {
        this.name = name;
        this.teams = new ArrayList<>();
        this.matches = new ArrayList<>();
    }

    public void addTeam(Team team) {
        teams.add(team);
    }

    public void generateSchedule(ArrayList<Team> allTeams) {
        matches.clear();
        // Simple round-robin schedule
        for (int i = 0; i < allTeams.size(); i++) {
            for (int j = i + 1; j < allTeams.size(); j++) {
                Team home = allTeams.get(i);
                Team away = allTeams.get(j);
                matches.add(new Match(home, away, (i + j) % 38 + 1));
                matches.add(new Match(away, home, (i + j + 19) % 38 + 1));
            }
        }
    }

    public int getTeamPosition(Team team) {
        // Sort teams by points, goal difference, etc.
        ArrayList<Team> sortedTeams = new ArrayList<>(teams);
        sortedTeams.sort((t1, t2) -> {
            int pointsDiff = t2.getPoints() - t1.getPoints();
            if (pointsDiff != 0) return pointsDiff;
            return t2.getGoalDifference() - t1.getGoalDifference();
        });

        return sortedTeams.indexOf(team) + 1;
    }

    public Object[][] getLeagueTableData() {
        ArrayList<Team> sortedTeams = new ArrayList<>(teams);
        sortedTeams.sort((t1, t2) -> {
            int pointsDiff = t2.getPoints() - t1.getPoints();
            if (pointsDiff != 0) return pointsDiff;
            return t2.getGoalDifference() - t1.getGoalDifference();
        });

        Object[][] data = new Object[sortedTeams.size()][10];
        for (int i = 0; i < sortedTeams.size(); i++) {
            Team team = sortedTeams.get(i);
            data[i][0] = i + 1;
            data[i][1] = team.getName();
            data[i][2] = team.getWins() + team.getDraws() + team.getLosses();
            data[i][3] = team.getWins();
            data[i][4] = team.getDraws();
            data[i][5] = team.getLosses();
            data[i][6] = team.getGoalsFor();
            data[i][7] = team.getGoalsAgainst();
            data[i][8] = team.getGoalDifference();
            data[i][9] = team.getPoints();
        }

        return data;
    }
}

class Match implements Serializable {
    private Team homeTeam;
    private Team awayTeam;
    private int homeScore;
    private int awayScore;
    private int week;
    private boolean played;

    public Match(Team homeTeam, Team awayTeam, int week) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.week = week;
        this.played = false;
    }

    // Getters
    public Team getHomeTeam() { return homeTeam; }
    public Team getAwayTeam() { return awayTeam; }
    public int getHomeScore() { return homeScore; }
    public int getAwayScore() { return awayScore; }
    public int getWeek() { return week; }
    public boolean isPlayed() { return played; }

    public void setResult(int homeScore, int awayScore) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.played = true;
    }
}

class Calendar {
    private ArrayList<Match> matches;

    public Calendar() {
        this.matches = new ArrayList<>();
    }

    public Match getNextMatch(Team team) {
        for (Match match : matches) {
            if (!match.isPlayed() &&
                    (match.getHomeTeam().equals(team) || match.getAwayTeam().equals(team))) {
                return match;
            }
        }
        return null;
    }

    public ArrayList<Match> getUpcomingFixtures(Team team, int count) {
        ArrayList<Match> fixtures = new ArrayList<>();
        for (Match match : matches) {
            if ((match.getHomeTeam().equals(team) || match.getAwayTeam().equals(team)) &&
                    !match.isPlayed()) {
                fixtures.add(match);
                if (fixtures.size() >= count) break;
            }
        }
        return fixtures;
    }
}

class MatchEngine {
    public void playMatch(Match match, MatchVisualization visualization) {
        // Simulate match with visualization
        simulateMatch(match);
        // In a full implementation, this would control the match flow and animation
    }

    public void simulateMatch(Match match) {
        Random rand = new Random();

        // Calculate team strengths
        int homeStrength = calculateTeamStrength(match.getHomeTeam());
        int awayStrength = calculateTeamStrength(match.getAwayTeam());

        // Home advantage
        homeStrength = (int)(homeStrength * 1.1);

        // Determine goals based on strength difference
        double homeGoalChance = homeStrength / 100.0;
        double awayGoalChance = awayStrength / 100.0;

        int homeGoals = 0;
        int awayGoals = 0;

        // Simulate 90 minutes (in chunks)
        for (int i = 0; i < 90; i++) {
            if (rand.nextDouble() < homeGoalChance / 90) {
                homeGoals++;
            }
            if (rand.nextDouble() < awayGoalChance / 90) {
                awayGoals++;
            }
        }

        match.setResult(homeGoals, awayGoals);
    }

    private int calculateTeamStrength(Team team) {
        int totalOvr = 0;
        for (Player player : team.getPlayers()) {
            totalOvr += player.getOverall();
        }
        return totalOvr / team.getPlayers().size();
    }
}

class MatchVisualization extends JPanel {
    private int homeScore = 0;
    private int awayScore = 0;
    private String homeTeam = "Home";
    private String awayTeam = "Away";
    private ArrayList<String> matchEvents;

    public MatchVisualization() {
        setPreferredSize(new Dimension(600, 400));
        setBackground(new Color(100, 150, 100)); // Green pitch
        matchEvents = new ArrayList<>();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw pitch
        drawPitch(g2d);

        // Draw score
        drawScore(g2d);

        // Draw players (simplified)
        drawPlayers(g2d);

        // Draw match events
        drawEvents(g2d);
    }

    private void drawPitch(Graphics2D g2d) {
        g2d.setColor(new Color(80, 130, 80));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Pitch markings
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));

        // Outer lines
        g2d.drawRect(10, 10, getWidth() - 20, getHeight() - 20);

        // Center line
        g2d.drawLine(getWidth() / 2, 10, getWidth() / 2, getHeight() - 10);

        // Center circle
        g2d.drawOval(getWidth() / 2 - 50, getHeight() / 2 - 50, 100, 100);
    }

    private void drawScore(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));

        String score = homeTeam + " " + homeScore + " - " + awayScore + " " + awayTeam;
        g2d.drawString(score, getWidth() / 2 - 100, 30);
    }

    private void drawPlayers(Graphics2D g2d) {
        // Simplified player positions
        int[] homeX = {100, 150, 150, 200, 200, 250, 250, 300, 300, 350, 350};
        int[] homeY = {100, 150, 250, 120, 280, 100, 300, 150, 250, 200, 200};

        int[] awayX = {500, 450, 450, 400, 400, 350, 350, 300, 300, 250, 250};
        int[] awayY = {100, 150, 250, 120, 280, 100, 300, 150, 250, 200, 200};

        g2d.setColor(Color.RED);
        for (int i = 0; i < Math.min(11, homeX.length); i++) {
            g2d.fillOval(homeX[i], homeY[i], 15, 15);
        }

        g2d.setColor(Color.BLUE);
        for (int i = 0; i < Math.min(11, awayX.length); i++) {
            g2d.fillOval(awayX[i], awayY[i], 15, 15);
        }
    }

    private void drawEvents(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));

        int y = 350;
        for (int i = Math.max(0, matchEvents.size() - 5); i < matchEvents.size(); i++) {
            g2d.drawString(matchEvents.get(i), 20, y);
            y += 20;
        }
    }

    public void addMatchEvent(String event) {
        matchEvents.add(event);
        repaint();
    }

    public void updateScore(String homeTeam, String awayTeam, int homeScore, int awayScore) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        repaint();
    }
}

class FormationPanel extends JPanel {
    private Team team;

    public FormationPanel(Team team) {
        this.team = team;
        setPreferredSize(new Dimension(200, 300));
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw formation (simplified 4-4-2)
        g2d.setColor(Color.BLACK);

        // Goalkeeper
        g2d.fillOval(95, 20, 10, 10);
        g2d.drawString("GK", 90, 45);

        // Defenders
        int[] defX = {50, 75, 115, 140};
        for (int x : defX) {
            g2d.fillOval(x, 60, 10, 10);
        }
        g2d.drawString("DEF", 65, 85);

        // Midfielders
        int[] midX = {40, 75, 115, 150};
        for (int x : midX) {
            g2d.fillOval(x, 100, 10, 10);
        }
        g2d.drawString("MID", 65, 125);

        // Forwards
        int[] fwdX = {75, 115};
        for (int x : fwdX) {
            g2d.fillOval(x, 140, 10, 10);
        }
        g2d.drawString("FWD", 85, 165);

        g2d.drawString("Formation: 4-4-2", 60, 190);
    }
}