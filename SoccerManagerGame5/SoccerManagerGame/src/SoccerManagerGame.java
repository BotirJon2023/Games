import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Main Game Class
public class SoccerManagerGame extends JFrame {
    private GameEngine gameEngine;
    private MainMenuPanel mainMenuPanel;
    private MatchPanel matchPanel;
    private TeamManagementPanel teamManagementPanel;
    private TransferMarketPanel transferMarketPanel;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    public SoccerManagerGame() {
        initializeGame();
        setupUI();
    }

    private void initializeGame() {
        gameEngine = new GameEngine();
        gameEngine.initializeGame();
    }

    private void setupUI() {
        setTitle("Soccer Manager 2024");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Create different panels
        mainMenuPanel = new MainMenuPanel(this);
        matchPanel = new MatchPanel(this, gameEngine);
        teamManagementPanel = new TeamManagementPanel(this, gameEngine);
        transferMarketPanel = new TransferMarketPanel(this, gameEngine);

        mainPanel.add(mainMenuPanel, "MAIN_MENU");
        mainPanel.add(matchPanel, "MATCH");
        mainPanel.add(teamManagementPanel, "TEAM_MANAGEMENT");
        mainPanel.add(transferMarketPanel, "TRANSFER_MARKET");

        add(mainPanel);
        showPanel("MAIN_MENU");
    }

    public void showPanel(String panelName) {
        cardLayout.show(mainPanel, panelName);
        if (panelName.equals("MATCH")) {
            matchPanel.startNewMatch();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SoccerManagerGame().setVisible(true);
        });
    }
}

// Game Engine
class GameEngine {
    private Team userTeam;
    private Team opponentTeam;
    private List<Player> transferMarket;
    private int budget;
    private int week;
    private Random random;

    public void initializeGame() {
        random = new Random();
        budget = 50000000; // 50 million
        week = 1;

        // Create user team
        userTeam = createDefaultTeam("Manchester United");

        // Create opponent team
        opponentTeam = createDefaultTeam("Manchester City");

        // Initialize transfer market
        initializeTransferMarket();
    }

    private Team createDefaultTeam(String name) {
        Team team = new Team(name);

        // Create players with different positions and skills
        team.addPlayer(new Player("David De Gea", "GK", 85, 32, 25000000));
        team.addPlayer(new Player("Aaron Wan-Bissaka", "DEF", 78, 25, 20000000));
        team.addPlayer(new Player("Raphael Varane", "DEF", 84, 30, 35000000));
        team.addPlayer(new Player("Lisandro Martinez", "DEF", 82, 25, 40000000));
        team.addPlayer(new Player("Luke Shaw", "DEF", 81, 27, 25000000));
        team.addPlayer(new Player("Casemiro", "MID", 87, 31, 40000000));
        team.addPlayer(new Player("Bruno Fernandes", "MID", 88, 28, 80000000));
        team.addPlayer(new Player("Christian Eriksen", "MID", 84, 31, 25000000));
        team.addPlayer(new Player("Marcus Rashford", "ATT", 85, 25, 70000000));
        team.addPlayer(new Player("Jadon Sancho", "ATT", 82, 23, 50000000));
        team.addPlayer(new Player("Anthony Martial", "ATT", 80, 27, 25000000));

        return team;
    }

    private void initializeTransferMarket() {
        transferMarket = new ArrayList<>();

        // Add some players to transfer market
        transferMarket.add(new Player("Kylian Mbappe", "ATT", 91, 24, 180000000));
        transferMarket.add(new Player("Erling Haaland", "ATT", 90, 23, 170000000));
        transferMarket.add(new Player("Kevin De Bruyne", "MID", 91, 32, 80000000));
        transferMarket.add(new Player("Virgil van Dijk", "DEF", 89, 32, 45000000));
        transferMarket.add(new Player("Alisson Becker", "GK", 89, 30, 60000000));
        transferMarket.add(new Player("Joshua Kimmich", "MID", 88, 28, 70000000));
        transferMarket.add(new Player("Trent Alexander-Arnold", "DEF", 87, 24, 65000000));
        transferMarket.add(new Player("Phil Foden", "MID", 86, 23, 90000000));
        transferMarket.add(new Player("Jude Bellingham", "MID", 89, 20, 120000000));
        transferMarket.add(new Player("Vinícius Júnior", "ATT", 88, 23, 120000000));
    }

    public MatchResult playMatch() {
        return new MatchResult(userTeam, opponentTeam);
    }

    public boolean buyPlayer(Player player) {
        if (budget >= player.getValue() && !userTeam.getPlayers().contains(player)) {
            budget -= player.getValue();
            userTeam.addPlayer(player);
            transferMarket.remove(player);
            return true;
        }
        return false;
    }

    public boolean sellPlayer(Player player) {
        if (userTeam.getPlayers().contains(player)) {
            budget += player.getValue();
            userTeam.removePlayer(player);
            transferMarket.add(player);
            return true;
        }
        return false;
    }

    // Getters
    public Team getUserTeam() { return userTeam; }
    public Team getOpponentTeam() { return opponentTeam; }
    public List<Player> getTransferMarket() { return transferMarket; }
    public int getBudget() { return budget; }
    public int getWeek() { return week; }
}

// Player Class
class Player {
    private String name;
    private String position;
    private int rating;
    private int age;
    private int value;

    public Player(String name, String position, int rating, int age, int value) {
        this.name = name;
        this.position = position;
        this.rating = rating;
        this.age = age;
        this.value = value;
    }

    // Getters
    public String getName() { return name; }
    public String getPosition() { return position; }
    public int getRating() { return rating; }
    public int getAge() { return age; }
    public int getValue() { return value; }

    @Override
    public String toString() {
        return String.format("%s (%s) - Rating: %d, Age: %d, Value: $%dM",
                name, position, rating, age, value/1000000);
    }
}

// Team Class
class Team {
    private String name;
    private List<Player> players;

    public Team(String name) {
        this.name = name;
        this.players = new ArrayList<>();
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public int getTeamRating() {
        if (players.isEmpty()) return 0;
        return (int) players.stream().mapToInt(Player::getRating).average().orElse(0);
    }

    // Getters
    public String getName() { return name; }
    public List<Player> getPlayers() { return players; }
}

// Match Result Class
class MatchResult {
    private Team homeTeam;
    private Team awayTeam;
    private int homeGoals;
    private int awayGoals;
    private List<MatchEvent> events;
    private Random random;

    public MatchResult(Team homeTeam, Team awayTeam) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.events = new ArrayList<>();
        this.random = new Random();
        simulateMatch();
    }

    private void simulateMatch() {
        homeGoals = 0;
        awayGoals = 0;

        int homeRating = homeTeam.getTeamRating();
        int awayRating = awayTeam.getTeamRating();

        // Simulate 90 minutes with random events
        for (int minute = 1; minute <= 90; minute++) {
            if (random.nextInt(100) < 5) { // 5% chance of event per minute
                boolean isHomeTeam = random.nextBoolean();
                Team scoringTeam = isHomeTeam ? homeTeam : awayTeam;
                Team concedingTeam = isHomeTeam ? awayTeam : homeTeam;

                if (random.nextInt(100) < calculateGoalProbability(scoringTeam, concedingTeam)) {
                    if (isHomeTeam) homeGoals++;
                    else awayGoals++;

                    Player scorer = getRandomPlayer(scoringTeam, "ATT");
                    events.add(new MatchEvent(minute, "GOAL", scoringTeam.getName(),
                            scorer != null ? scorer.getName() : "Unknown Player"));
                } else {
                    events.add(new MatchEvent(minute, "CHANCE", scoringTeam.getName(),
                            "Missed opportunity"));
                }
            }
        }
    }

    private int calculateGoalProbability(Team attacking, Team defending) {
        int baseProbability = 30;
        int ratingDifference = attacking.getTeamRating() - defending.getTeamRating();
        return Math.max(10, Math.min(70, baseProbability + ratingDifference));
    }

    private Player getRandomPlayer(Team team, String preferredPosition) {
        List<Player> players = team.getPlayers();
        if (players.isEmpty()) return null;

        // Prefer players in the preferred position
        List<Player> preferredPlayers = players.stream()
                .filter(p -> p.getPosition().equals(preferredPosition))
                .toList();

        if (!preferredPlayers.isEmpty()) {
            return preferredPlayers.get(random.nextInt(preferredPlayers.size()));
        }

        return players.get(random.nextInt(players.size()));
    }

    // Getters
    public int getHomeGoals() { return homeGoals; }
    public int getAwayGoals() { return awayGoals; }
    public List<MatchEvent> getEvents() { return events; }
    public String getScore() { return homeGoals + " - " + awayGoals; }
}

// Match Event Class
class MatchEvent {
    private int minute;
    private String type;
    private String team;
    private String description;

    public MatchEvent(int minute, String type, String team, String description) {
        this.minute = minute;
        this.type = type;
        this.team = team;
        this.description = description;
    }

    // Getters
    public int getMinute() { return minute; }
    public String getType() { return type; }
    public String getTeam() { return team; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return String.format("%d' - %s: %s - %s", minute, team, type, description);
    }
}

// Main Menu Panel
class MainMenuPanel extends JPanel {
    private SoccerManagerGame game;

    public MainMenuPanel(SoccerManagerGame game) {
        this.game = game;
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 60));

        // Title
        JLabel titleLabel = new JLabel("SOCCER MANAGER 2024", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(50, 0, 50, 0));

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 0, 20));
        buttonPanel.setBackground(new Color(30, 30, 60));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 200, 100, 200));

        String[] buttonLabels = {"Play Match", "Team Management", "Transfer Market", "Exit"};
        String[] panelNames = {"MATCH", "TEAM_MANAGEMENT", "TRANSFER_MARKET", "EXIT"};

        for (int i = 0; i < buttonLabels.length; i++) {
            JButton button = createMenuButton(buttonLabels[i]);
            final String panelName = panelNames[i];
            button.addActionListener(e -> {
                if (panelName.equals("EXIT")) {
                    System.exit(0);
                } else {
                    game.showPanel(panelName);
                }
            });
            buttonPanel.add(button);
        }

        add(titleLabel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);

        // Add background animation
        startBackgroundAnimation();
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradient background
                GradientPaint gradient = new GradientPaint(0, 0, new Color(70, 130, 180),
                        0, getHeight(), new Color(30, 144, 255));
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);

                // Border
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);

                // Text
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 20));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };

        button.setPreferredSize(new Dimension(300, 60));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.repaint();
            }
        });

        return button;
    }

    private void startBackgroundAnimation() {
        Timer timer = new Timer(50, e -> repaint());
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Animated background particles
        drawAnimatedBackground(g2d);
    }

    private void drawAnimatedBackground(Graphics2D g2d) {
        long time = System.currentTimeMillis();

        for (int i = 0; i < 50; i++) {
            double x = (time / 20 + i * 37) % getWidth();
            double y = (Math.sin(time / 1000.0 + i) * 50 + i * 15) % getHeight();

            float alpha = (float) (0.3 + 0.7 * Math.sin(time / 1000.0 + i));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.fillOval((int)x, (int)y, 3, 3);
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
}

// Match Panel with Animation
class MatchPanel extends JPanel {
    private SoccerManagerGame game;
    private GameEngine gameEngine;
    private MatchResult currentMatch;
    private Timer animationTimer;
    private int currentMinute;
    private int animationPhase;

    private JTextArea commentaryArea;
    private JLabel scoreLabel;
    private JLabel minuteLabel;
    private JProgressBar matchProgress;
    private JButton backButton;

    public MatchPanel(SoccerManagerGame game, GameEngine gameEngine) {
        this.game = game;
        this.gameEngine = gameEngine;
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        setBackground(Color.DARK_GRAY);

        // Header with score and time
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.BLACK);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        scoreLabel = new JLabel("0 - 0", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 36));
        scoreLabel.setForeground(Color.WHITE);

        minuteLabel = new JLabel("0'", SwingConstants.CENTER);
        minuteLabel.setFont(new Font("Arial", Font.BOLD, 24));
        minuteLabel.setForeground(Color.YELLOW);

        matchProgress = new JProgressBar(0, 90);
        matchProgress.setForeground(Color.GREEN);

        headerPanel.add(scoreLabel, BorderLayout.CENTER);
        headerPanel.add(minuteLabel, BorderLayout.EAST);
        headerPanel.add(matchProgress, BorderLayout.SOUTH);

        // Commentary area
        commentaryArea = new JTextArea();
        commentaryArea.setEditable(false);
        commentaryArea.setBackground(Color.BLACK);
        commentaryArea.setForeground(Color.WHITE);
        commentaryArea.setFont(new Font("Courier", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(commentaryArea);

        // Control panel
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(Color.DARK_GRAY);

        backButton = new JButton("Back to Menu");
        backButton.addActionListener(e -> game.showPanel("MAIN_MENU"));
        controlPanel.add(backButton);

        JButton simulateButton = new JButton("Simulate Match");
        simulateButton.addActionListener(e -> startNewMatch());
        controlPanel.add(simulateButton);

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
    }

    public void startNewMatch() {
        currentMatch = gameEngine.playMatch();
        currentMinute = 0;
        animationPhase = 0;
        commentaryArea.setText("");
        scoreLabel.setText("0 - 0");
        minuteLabel.setText("0'");
        matchProgress.setValue(0);

        startMatchAnimation();
    }

    private void startMatchAnimation() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        animationTimer = new Timer(100, e -> animateMatch());
        animationTimer.start();
    }

    private void animateMatch() {
        animationPhase++;

        if (animationPhase % 8 == 0 && currentMinute < 90) {
            currentMinute++;
            minuteLabel.setText(currentMinute + "'");
            matchProgress.setValue(currentMinute);

            // Add commentary for events at this minute
            for (MatchEvent event : currentMatch.getEvents()) {
                if (event.getMinute() == currentMinute) {
                    String commentary = String.format("%d' - %s: %s\n",
                            event.getMinute(), event.getTeam(), event.getDescription());
                    commentaryArea.append(commentary);
                    commentaryArea.setCaretPosition(commentaryArea.getDocument().getLength());

                    if (event.getType().equals("GOAL")) {
                        scoreLabel.setText(currentMatch.getHomeGoals() + " - " + currentMatch.getAwayGoals());
                        // Visual effect for goal
                        flashScoreLabel();
                    }
                }
            }
        }

        if (currentMinute >= 90) {
            animationTimer.stop();
            commentaryArea.append("\n*** FULL TIME ***\n");
            commentaryArea.append("Final Score: " + currentMatch.getScore() + "\n");
        }

        repaint();
    }

    private void flashScoreLabel() {
        Timer flashTimer = new Timer(100, new ActionListener() {
            private int flashCount = 0;
            private Color originalColor = scoreLabel.getForeground();

            @Override
            public void actionPerformed(ActionEvent e) {
                if (flashCount < 6) {
                    if (flashCount % 2 == 0) {
                        scoreLabel.setForeground(Color.RED);
                    } else {
                        scoreLabel.setForeground(originalColor);
                    }
                    flashCount++;
                } else {
                    scoreLabel.setForeground(originalColor);
                    ((Timer)e.getSource()).stop();
                }
            }
        });
        flashTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw animated field
        drawFootballField(g2d);
    }

    private void drawFootballField(Graphics2D g2d) {
        // Field background
        g2d.setColor(new Color(0, 100, 0));
        g2d.fillRect(50, 50, getWidth() - 100, getHeight() - 200);

        // Field markings
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(50, 50, getWidth() - 100, getHeight() - 200);

        // Center line and circle
        g2d.drawLine(getWidth() / 2, 50, getWidth() / 2, getHeight() - 150);
        g2d.drawOval(getWidth() / 2 - 50, (getHeight() - 200) / 2 + 25, 100, 100);

        // Animated ball
        drawAnimatedBall(g2d);
    }

    private void drawAnimatedBall(Graphics2D g2d) {
        int ballSize = 20;
        int maxX = getWidth() - 100 - ballSize;
        int maxY = getHeight() - 200 - ballSize;

        // Animate ball position based on current minute
        double progress = currentMinute / 90.0;
        int ballX = 50 + (int)(progress * maxX);
        int ballY = 50 + (int)(Math.sin(progress * Math.PI * 4) * maxY / 2 + maxY / 2);

        // Draw ball with pattern
        g2d.setColor(Color.WHITE);
        g2d.fillOval(ballX, ballY, ballSize, ballSize);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(ballX, ballY, ballSize, ballSize);

        // Ball pattern
        g2d.drawLine(ballX + ballSize/2, ballY, ballX + ballSize/2, ballY + ballSize);
        g2d.drawLine(ballX, ballY + ballSize/2, ballX + ballSize, ballY + ballSize/2);
    }
}

// Team Management Panel
class TeamManagementPanel extends JPanel {
    private SoccerManagerGame game;
    private GameEngine gameEngine;
    private JList<Player> playerList;
    private DefaultListModel<Player> listModel;
    private JLabel budgetLabel;
    private JLabel teamRatingLabel;

    public TeamManagementPanel(SoccerManagerGame game, GameEngine gameEngine) {
        this.game = game;
        this.gameEngine = gameEngine;
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 240));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(50, 50, 80));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel titleLabel = new JLabel("TEAM MANAGEMENT", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        budgetLabel = new JLabel("Budget: $" + (gameEngine.getBudget()/1000000) + "M");
        budgetLabel.setFont(new Font("Arial", Font.BOLD, 16));
        budgetLabel.setForeground(Color.YELLOW);

        teamRatingLabel = new JLabel("Team Rating: " + gameEngine.getUserTeam().getTeamRating());
        teamRatingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        teamRatingLabel.setForeground(Color.GREEN);

        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(budgetLabel, BorderLayout.WEST);
        headerPanel.add(teamRatingLabel, BorderLayout.EAST);

        // Player list
        listModel = new DefaultListModel<>();
        updatePlayerList();

        playerList = new JList<>(listModel);
        playerList.setCellRenderer(new PlayerListRenderer());
        playerList.setFont(new Font("Arial", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(playerList);

        // Control buttons
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBackground(new Color(200, 200, 220));

        JButton backButton = new JButton("Back to Menu");
        backButton.addActionListener(e -> game.showPanel("MAIN_MENU"));

        JButton transferButton = new JButton("Go to Transfer Market");
        transferButton.addActionListener(e -> game.showPanel("TRANSFER_MARKET"));

        controlPanel.add(backButton);
        controlPanel.add(transferButton);

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
    }

    private void updatePlayerList() {
        listModel.clear();
        for (Player player : gameEngine.getUserTeam().getPlayers()) {
            listModel.addElement(player);
        }
        budgetLabel.setText("Budget: $" + (gameEngine.getBudget()/1000000) + "M");
        teamRatingLabel.setText("Team Rating: " + gameEngine.getUserTeam().getTeamRating());
    }

    // Custom list renderer for players
    private class PlayerListRenderer extends JLabel implements ListCellRenderer<Player> {
        public PlayerListRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Player> list, Player player,
                                                      int index, boolean isSelected, boolean cellHasFocus) {

            setText(player.toString());

            if (isSelected) {
                setBackground(new Color(200, 220, 255));
                setForeground(Color.BLACK);
            } else {
                setBackground(index % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                setForeground(Color.BLACK);
            }

            // Color code by position
            switch (player.getPosition()) {
                case "GK": setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, Color.BLUE)); break;
                case "DEF": setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, Color.GREEN)); break;
                case "MID": setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, Color.ORANGE)); break;
                case "ATT": setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, Color.RED)); break;
            }

            return this;
        }
    }
}

// Transfer Market Panel
class TransferMarketPanel extends JPanel {
    private SoccerManagerGame game;
    private GameEngine gameEngine;
    private JList<Player> marketList;
    private DefaultListModel<Player> listModel;
    private JLabel budgetLabel;

    public TransferMarketPanel(SoccerManagerGame game, GameEngine gameEngine) {
        this.game = game;
        this.gameEngine = gameEngine;
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 240));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(80, 50, 50));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel titleLabel = new JLabel("TRANSFER MARKET", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        budgetLabel = new JLabel("Budget: $" + (gameEngine.getBudget()/1000000) + "M");
        budgetLabel.setFont(new Font("Arial", Font.BOLD, 16));
        budgetLabel.setForeground(Color.YELLOW);

        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(budgetLabel, BorderLayout.WEST);

        // Market list
        listModel = new DefaultListModel<>();
        updateMarketList();

        marketList = new JList<>(listModel);
        marketList.setCellRenderer(new MarketListRenderer());
        marketList.setFont(new Font("Arial", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(marketList);

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBackground(new Color(220, 200, 200));

        JButton backButton = new JButton("Back to Menu");
        backButton.addActionListener(e -> game.showPanel("MAIN_MENU"));

        JButton buyButton = new JButton("Buy Player");
        buyButton.addActionListener(e -> buySelectedPlayer());

        JButton refreshButton = new JButton("Refresh Market");
        refreshButton.addActionListener(e -> updateMarketList());

        controlPanel.add(backButton);
        controlPanel.add(buyButton);
        controlPanel.add(refreshButton);

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
    }

    private void updateMarketList() {
        listModel.clear();
        for (Player player : gameEngine.getTransferMarket()) {
            listModel.addElement(player);
        }
        budgetLabel.setText("Budget: $" + (gameEngine.getBudget()/1000000) + "M");
    }

    private void buySelectedPlayer() {
        Player selectedPlayer = marketList.getSelectedValue();
        if (selectedPlayer != null) {
            if (gameEngine.buyPlayer(selectedPlayer)) {
                JOptionPane.showMessageDialog(this,
                        "Successfully bought " + selectedPlayer.getName() + "!",
                        "Transfer Complete", JOptionPane.INFORMATION_MESSAGE);
                updateMarketList();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Cannot buy " + selectedPlayer.getName() + "!\nNot enough budget or player already in team.",
                        "Transfer Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Custom list renderer for transfer market
    private class MarketListRenderer extends JLabel implements ListCellRenderer<Player> {
        public MarketListRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Player> list, Player player,
                                                      int index, boolean isSelected, boolean cellHasFocus) {

            setText(player.toString());

            if (isSelected) {
                setBackground(new Color(255, 220, 220));
                setForeground(Color.BLACK);
            } else {
                setBackground(index % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                setForeground(Color.BLACK);
            }

            // Highlight affordable players
            if (player.getValue() <= gameEngine.getBudget()) {
                setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, Color.GREEN));
            } else {
                setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, Color.RED));
            }

            return this;
        }
    }
}