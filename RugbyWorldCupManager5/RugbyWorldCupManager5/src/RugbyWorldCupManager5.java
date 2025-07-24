import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class RugbyWorldCupManager5 extends JFrame {
    private static final int WIDTH = 900;
    private static final int HEIGHT = 600;

    private List<Team> teams;
    private List<Match> matches;
    private Team userTeam;
    private int currentRound = 1;
    private int totalRounds = 4;
    private boolean gameOver = false;

    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JTextArea gameLog;
    private JLabel animationLabel;
    private ImageIcon[] rugbyAnimationFrames;
    private int currentFrame = 0;

    public RugbyWorldCupManager5() {
        super("Rugby World Cup Manager");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        initializeTeams();
        initializeMatches();

        // Setup GUI
        setupGUI();

        // Load animation frames
        loadAnimationFrames();

        // Start animation timer
        startAnimation();
    }

    private void initializeTeams() {
        teams = new ArrayList<>();

        // Create 16 international rugby teams with realistic attributes
        teams.add(new Team("New Zealand", 95, "All Blacks", Color.BLACK));
        teams.add(new Team("South Africa", 93, "Springboks", new Color(0, 119, 73)));
        teams.add(new Team("England", 90, "Red Roses", new Color(200, 16, 46)));
        teams.add(new Team("Australia", 89, "Wallabies", new Color(255, 209, 0)));
        teams.add(new Team("Ireland", 88, "Men in Green", new Color(0, 154, 68)));
        teams.add(new Team("France", 87, "Les Bleus", new Color(0, 85, 164)));
        teams.add(new Team("Wales", 86, "Dragons", new Color(206, 17, 38)));
        teams.add(new Team("Scotland", 84, "Thistles", new Color(0, 122, 94)));
        teams.add(new Team("Argentina", 83, "Pumas", new Color(116, 172, 223)));
        teams.add(new Team("Japan", 82, "Brave Blossoms", new Color(188, 0, 45)));
        teams.add(new Team("Fiji", 80, "Flying Fijians", new Color(0, 53, 128)));
        teams.add(new Team("Italy", 78, "Azzurri", new Color(0, 140, 69)));
        teams.add(new Team("Samoa", 76, "Manu Samoa", new Color(0, 56, 168)));
        teams.add(new Team("Tonga", 75, "Ikale Tahi", new Color(200, 16, 46)));
        teams.add(new Team("Georgia", 74, "Lelos", new Color(255, 215, 0)));
        teams.add(new Team("USA", 72, "Eagles", new Color(60, 59, 110)));

        // Let user pick their team
        userTeam = (Team) JOptionPane.showInputDialog(
                this,
                "Choose your team to manage:",
                "Team Selection",
                JOptionPane.QUESTION_MESSAGE,
                null,
                teams.toArray(),
                teams.get(0));
    }

    private void initializeMatches() {
        matches = new ArrayList<>();
        Collections.shuffle(teams);

        // Create round-robin matches (simplified tournament)
        for (int i = 0; i < teams.size(); i += 2) {
            matches.add(new Match(teams.get(i), teams.get(i+1)));
        }
    }

    private void setupGUI() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Create menu panel
        JPanel menuPanel = new JPanel(new BorderLayout());
        menuPanel.setBackground(new Color(240, 240, 240));

        JLabel titleLabel = new JLabel("RUGBY WORLD CUP MANAGER", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(new Color(0, 50, 150));
        menuPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(50, 150, 50, 150));

        JButton teamButton = createMenuButton("Team Management");
        JButton matchButton = createMenuButton("Next Match");
        JButton standingsButton = createMenuButton("Tournament Standings");
        JButton quitButton = createMenuButton("Quit Game");

        teamButton.addActionListener(e -> showTeamManagement());
        matchButton.addActionListener(e -> playNextMatch());
        standingsButton.addActionListener(e -> showStandings());
        quitButton.addActionListener(e -> System.exit(0));

        buttonPanel.add(teamButton);
        buttonPanel.add(matchButton);
        buttonPanel.add(standingsButton);
        buttonPanel.add(quitButton);

        menuPanel.add(buttonPanel, BorderLayout.CENTER);

        // Create team management panel
        JPanel teamPanel = createTeamManagementPanel();

        // Create match panel
        JPanel matchPanel = createMatchPanel();

        // Create standings panel
        JPanel standingsPanel = createStandingsPanel();

        // Add all panels to main panel
        mainPanel.add(menuPanel, "menu");
        mainPanel.add(teamPanel, "team");
        mainPanel.add(matchPanel, "match");
        mainPanel.add(standingsPanel, "standings");

        add(mainPanel);
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setBackground(new Color(0, 80, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return button;
    }

    private JPanel createTeamManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));

        JLabel title = new JLabel("Team Management: " + userTeam.getName(), JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(title, BorderLayout.NORTH);

        JPanel infoPanel = new JPanel(new GridLayout(5, 1));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        JLabel nameLabel = new JLabel("Team: " + userTeam.getNickname());
        JLabel ratingLabel = new JLabel("Rating: " + userTeam.getRating());
        JLabel winsLabel = new JLabel("Wins: " + userTeam.getWins());
        JLabel lossesLabel = new JLabel("Losses: " + userTeam.getLosses());
        JLabel pointsLabel = new JLabel("Points Scored: " + userTeam.getPointsFor());

        nameLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        ratingLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        winsLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        lossesLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        pointsLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        infoPanel.add(nameLabel);
        infoPanel.add(ratingLabel);
        infoPanel.add(winsLabel);
        infoPanel.add(lossesLabel);
        infoPanel.add(pointsLabel);

        panel.add(infoPanel, BorderLayout.CENTER);

        JButton backButton = new JButton("Back to Menu");
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "menu"));
        panel.add(backButton, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMatchPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));

        JLabel title = new JLabel("Match Day " + currentRound + " of " + totalRounds, JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(title, BorderLayout.NORTH);

        animationLabel = new JLabel("", JLabel.CENTER);
        panel.add(animationLabel, BorderLayout.CENTER);

        gameLog = new JTextArea();
        gameLog.setEditable(false);
        gameLog.setFont(new Font("Courier New", Font.PLAIN, 14));
        gameLog.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(gameLog);
        panel.add(scrollPane, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStandingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));

        JLabel title = new JLabel("Tournament Standings", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(title, BorderLayout.NORTH);

        // Sort teams by wins (simple ranking)
        teams.sort((t1, t2) -> t2.getWins() - t1.getWins());

        String[] columns = {"Position", "Team", "Wins", "Losses", "Points For", "Points Against"};
        Object[][] data = new Object[teams.size()][6];

        for (int i = 0; i < teams.size(); i++) {
            Team t = teams.get(i);
            data[i][0] = i + 1;
            data[i][1] = t.getName();
            data[i][2] = t.getWins();
            data[i][3] = t.getLosses();
            data[i][4] = t.getPointsFor();
            data[i][5] = t.getPointsAgainst();
        }

        JTable table = new JTable(data, columns);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));

        // Highlight user's team
        int userTeamIndex = teams.indexOf(userTeam);
        if (userTeamIndex >= 0) {
            table.setRowSelectionInterval(userTeamIndex, userTeamIndex);
            table.setSelectionBackground(new Color(173, 216, 230));
        }

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton backButton = new JButton("Back to Menu");
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "menu"));
        panel.add(backButton, BorderLayout.SOUTH);

        return panel;
    }

    private void showTeamManagement() {
        cardLayout.show(mainPanel, "team");
    }

    private void showStandings() {
        cardLayout.show(mainPanel, "standings");
    }

    private void playNextMatch() {
        if (gameOver) {
            JOptionPane.showMessageDialog(this, "The tournament is already over!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        cardLayout.show(mainPanel, "match");
        gameLog.setText("");

        // Find user's match
        Match userMatch = null;
        for (Match m : matches) {
            if (m.getTeam1() == userTeam || m.getTeam2() == userTeam) {
                userMatch = m;
                break;
            }
        }

        if (userMatch == null) {
            gameLog.append("No matches scheduled for your team in this round.\n");
            return;
        }

        gameLog.append("Upcoming Match:\n");
        gameLog.append(userMatch.getTeam1().getName() + " vs " + userMatch.getTeam2().getName() + "\n\n");

        // Simulate match with animation
        simulateMatchWithAnimation(userMatch);

        currentRound++;
        if (currentRound > totalRounds) {
            gameOver = true;
            showTournamentResults();
        }
    }

    private void simulateMatchWithAnimation(Match match) {
        Timer timer = new Timer();
        final int[] step = {0};

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                switch (step[0]) {
                    case 0:
                        gameLog.append("Match starting...\n");
                        break;
                    case 1:
                        gameLog.append("First half underway!\n");
                        break;
                    case 2:
                        int score1 = match.simulateFirstHalf();
                        gameLog.append("First half score: " + score1 + "\n");
                        break;
                    case 3:
                        gameLog.append("Second half underway!\n");
                        break;
                    case 4:
                        int score2 = match.simulateSecondHalf();
                        gameLog.append("Second half score: " + score2 + "\n");
                        break;
                    case 5:
                        Team winner = match.getWinner();
                        if (winner != null) {
                            gameLog.append("Match winner: " + winner.getName() + "\n");
                        } else {
                            gameLog.append("Match ended in a draw!\n");
                        }
                        gameLog.append("Final score: " + match.getTeam1().getName() + " " +
                                match.getTeam1Score() + " - " + match.getTeam2Score() + " " +
                                match.getTeam2().getName() + "\n");
                        timer.cancel();
                        break;
                }
                step[0]++;
            }
        }, 1000, 1500);
    }

    private void showTournamentResults() {
        // Sort teams by wins
        teams.sort((t1, t2) -> t2.getWins() - t1.getWins());

        StringBuilder result = new StringBuilder();
        result.append("TOURNAMENT FINAL STANDINGS:\n\n");

        for (int i = 0; i < teams.size(); i++) {
            Team t = teams.get(i);
            result.append((i+1)).append(". ").append(t.getName()).append(" - Wins: ").append(t.getWins())
                    .append(", Points: ").append(t.getPointsFor()).append("\n");
        }

        result.append("\n");

        if (teams.get(0) == userTeam) {
            result.append("CONGRATULATIONS! Your team won the tournament!\n");
        } else {
            result.append("The tournament winner is: ").append(teams.get(0).getName()).append("\n");
        }

        gameLog.append(result.toString());
    }

    private void loadAnimationFrames() {
        rugbyAnimationFrames = new ImageIcon[8];

        // In a real application, you would load actual images here
        // For this example, we'll create colored rectangles as placeholders

        for (int i = 0; i < rugbyAnimationFrames.length; i++) {
            BufferedImage image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            // Draw field background
            g.setColor(new Color(0, 100, 0));
            g.fillRect(0, 0, 400, 200);

            // Draw field markings
            g.setColor(Color.WHITE);
            g.drawRect(20, 20, 360, 160);
            g.drawLine(200, 20, 200, 180);

            // Draw players based on frame
            if (i % 2 == 0) {
                g.setColor(userTeam.getColor());
                g.fillOval(150 + (i*5), 80, 20, 20);
                g.setColor(Color.WHITE);
                g.fillOval(230 - (i*5), 100, 20, 20);
            } else {
                g.setColor(Color.WHITE);
                g.fillOval(160 + (i*5), 90, 20, 20);
                g.setColor(userTeam.getColor());
                g.fillOval(220 - (i*5), 110, 20, 20);
            }

            // Draw ball
            g.setColor(Color.BLACK);
            g.fillOval(190 + (i*10), 95, 10, 10);

            rugbyAnimationFrames[i] = new ImageIcon(image);
        }
    }

    private void startAnimation() {
        Timer animationTimer = new Timer();
        animationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                currentFrame = (currentFrame + 1) % rugbyAnimationFrames.length;
                animationLabel.setIcon(rugbyAnimationFrames[currentFrame]);
            }
        }, 0, 200);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RugbyWorldCupManager5 game = new RugbyWorldCupManager5();
            game.setVisible(true);
        });
    }
}

class Team {
    private String name;
    private String nickname;
    private int rating;
    private int wins;
    private int losses;
    private int pointsFor;
    private int pointsAgainst;
    private Color color;

    public Team(String name, int rating, String nickname, Color color) {
        this.name = name;
        this.rating = rating;
        this.nickname = nickname;
        this.wins = 0;
        this.losses = 0;
        this.pointsFor = 0;
        this.pointsAgainst = 0;
        this.color = color;
    }

    // Getters and setters
    public String getName() { return name; }
    public String getNickname() { return nickname; }
    public int getRating() { return rating; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getPointsFor() { return pointsFor; }
    public int getPointsAgainst() { return pointsAgainst; }
    public Color getColor() { return color; }

    public void addWin(int pointsScored, int pointsConceded) {
        wins++;
        pointsFor += pointsScored;
        pointsAgainst += pointsConceded;
    }

    public void addLoss(int pointsScored, int pointsConceded) {
        losses++;
        pointsFor += pointsScored;
        pointsAgainst += pointsConceded;
    }
}

class Match {
    private Team team1;
    private Team team2;
    private int team1Score;
    private int team2Score;
    private boolean played;

    public Match(Team team1, Team team2) {
        this.team1 = team1;
        this.team2 = team2;
        this.team1Score = 0;
        this.team2Score = 0;
        this.played = false;
    }

    public Team getTeam1() { return team1; }
    public Team getTeam2() { return team2; }
    public int getTeam1Score() { return team1Score; }
    public int getTeam2Score() { return team2Score; }
    public boolean isPlayed() { return played; }

    public int simulateFirstHalf() {
        Random rand = new Random();

        // Base score based on team rating
        int score1 = rand.nextInt(5) + (team1.getRating() - 70) / 5;
        int score2 = rand.nextInt(5) + (team2.getRating() - 70) / 5;

        // Add some randomness
        score1 += rand.nextInt(10) - 3;
        score2 += rand.nextInt(10) - 3;

        // Ensure scores are positive
        score1 = Math.max(0, score1);
        score2 = Math.max(0, score2);

        team1Score = score1;
        team2Score = score2;

        return score1 + score2;
    }

    public int simulateSecondHalf() {
        Random rand = new Random();

        // Second half scores (usually higher than first half)
        int score1 = rand.nextInt(7) + (team1.getRating() - 70) / 5;
        int score2 = rand.nextInt(7) + (team2.getRating() - 70) / 5;

        // Add some randomness
        score1 += rand.nextInt(12) - 3;
        score2 += rand.nextInt(12) - 3;

        // Ensure scores are positive
        score1 = Math.max(0, score1);
        score2 = Math.max(0, score2);

        team1Score += score1;
        team2Score += score2;

        played = true;

        // Update team records
        if (team1Score > team2Score) {
            team1.addWin(team1Score, team2Score);
            team2.addLoss(team2Score, team1Score);
        } else if (team2Score > team1Score) {
            team2.addWin(team2Score, team1Score);
            team1.addLoss(team1Score, team2Score);
        } else {
            // Draw - both teams get a "win" for simplicity
            team1.addWin(team1Score, team2Score);
            team2.addWin(team2Score, team1Score);
        }

        return score1 + score2;
    }

    public Team getWinner() {
        if (!played) return null;
        if (team1Score > team2Score) return team1;
        if (team2Score > team1Score) return team2;
        return null; // Draw
    }
}