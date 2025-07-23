import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

/**
 * Rugby World Cup Manager Game
 * A comprehensive rugby team management simulation with animations
 */
public class RugbyWorldCupManager extends JFrame {

    // Game constants
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int ANIMATION_DELAY = 100;
    private static final int MATCH_DURATION = 5000; // 5 seconds simulation

    // Game components
    private GameEngine gameEngine;
    private AnimationPanel animationPanel;
    private ControlPanel controlPanel;
    private InfoPanel infoPanel;
    private Timer animationTimer;
    private boolean matchInProgress = false;

    // Current game state
    private Team playerTeam;
    private Tournament tournament;
    private Match currentMatch;

    public RugbyWorldCupManager() {
        initializeGame();
        setupUI();
        startAnimationLoop();
    }

    private void initializeGame() {
        gameEngine = new GameEngine();
        tournament = gameEngine.createTournament();
        playerTeam = gameEngine.getPlayerTeam();
    }

    private void setupUI() {
        setTitle("Rugby World Cup Manager 2025");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setResizable(false);

        // Create panels
        animationPanel = new AnimationPanel();
        controlPanel = new ControlPanel();
        infoPanel = new InfoPanel();

        // Layout
        setLayout(new BorderLayout());
        add(animationPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        add(infoPanel, BorderLayout.EAST);

        // Setup event listeners
        setupEventListeners();
    }

    private void setupEventListeners() {
        controlPanel.playMatchBtn.addActionListener(e -> playNextMatch());
        controlPanel.manageTeamBtn.addActionListener(e -> showTeamManagement());
        controlPanel.viewStatsBtn.addActionListener(e -> showStatistics());
        controlPanel.pauseBtn.addActionListener(e -> togglePause());
    }

    private void startAnimationLoop() {
        animationTimer = new Timer();
        animationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    animationPanel.updateAnimation();
                    animationPanel.repaint();
                });
            }
        }, 0, ANIMATION_DELAY);
    }

    private void playNextMatch() {
        if (matchInProgress) return;

        Match nextMatch = tournament.getNextMatch(playerTeam);
        if (nextMatch == null) {
            showTournamentComplete();
            return;
        }

        currentMatch = nextMatch;
        matchInProgress = true;
        controlPanel.playMatchBtn.setEnabled(false);

        // Start match animation
        animationPanel.startMatchAnimation(currentMatch);

        // Simulate match in background
        new Thread(() -> {
            gameEngine.simulateMatch(currentMatch);
            SwingUtilities.invokeLater(() -> {
                matchInProgress = false;
                controlPanel.playMatchBtn.setEnabled(true);
                animationPanel.endMatchAnimation();
                showMatchResult(currentMatch);
                updateUI();
            });
        }).start();
    }

    private void showMatchResult(Match match) {
        String result = String.format(
                "Match Result:\n%s %d - %d %s\n\n%s",
                match.homeTeam.name, match.homeScore, match.awayScore, match.awayTeam.name,
                match.homeScore > match.awayScore ? match.homeTeam.name + " wins!" :
                        match.awayScore > match.homeScore ? match.awayTeam.name + " wins!" : "Draw!"
        );

        JOptionPane.showMessageDialog(this, result, "Match Result", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showTeamManagement() {
        new TeamManagementDialog(this, playerTeam).setVisible(true);
        updateUI();
    }

    private void showStatistics() {
        new StatisticsDialog(this, tournament, playerTeam).setVisible(true);
    }

    private void togglePause() {
        // Toggle animation pause/resume
        animationPanel.togglePause();
    }

    private void showTournamentComplete() {
        String message = "Tournament Complete!\n\nFinal Position: " +
                tournament.getTeamPosition(playerTeam) + "\n" +
                "Points: " + playerTeam.points;
        JOptionPane.showMessageDialog(this, message, "Tournament Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateUI() {
        infoPanel.updateInfo(playerTeam, tournament);
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new RugbyWorldCupManager().setVisible(true);
        });
    }
}

/**
 * Main game engine handling all game logic
 */
class GameEngine {
    private static final int MATCH_DURATION = 5000; // 5 seconds simulation
    private Random random = new Random();
    private Team playerTeam;
    private List<Team> allTeams;

    public GameEngine() {
        createTeams();
    }

    private void createTeams() {
        allTeams = new ArrayList<>();
        String[] teamNames = {
                "New Zealand", "South Africa", "France", "Ireland", "England",
                "Wales", "Scotland", "Argentina", "Australia", "Japan",
                "Italy", "Fiji", "Samoa", "Tonga", "Georgia", "Uruguay"
        };

        for (int i = 0; i < teamNames.length; i++) {
            Team team = new Team(teamNames[i]);
            // Randomize team stats
            team.attack = 60 + random.nextInt(40);
            team.defense = 60 + random.nextInt(40);
            team.fitness = 60 + random.nextInt(40);
            team.morale = 70 + random.nextInt(30);

            allTeams.add(team);

            // Player team is first team
            if (i == 0) {
                playerTeam = team;
                playerTeam.isPlayerTeam = true;
            }
        }
    }

    public Tournament createTournament() {
        return new Tournament(allTeams);
    }

    public Team getPlayerTeam() {
        return playerTeam;
    }

    public void simulateMatch(Match match) {
        // Simulate match with realistic rugby scoring
        int homeTotal = match.homeTeam.attack + match.homeTeam.fitness + match.homeTeam.morale;
        int awayTotal = match.awayTeam.attack + match.awayTeam.fitness + match.awayTeam.morale;

        // Add random factors
        homeTotal += random.nextInt(50) - 25;
        awayTotal += random.nextInt(50) - 25;

        // Calculate scores (rugby typical scores)
        match.homeScore = Math.max(0, (homeTotal / 10) + random.nextInt(20));
        match.awayScore = Math.max(0, (awayTotal / 10) + random.nextInt(20));

        // Ensure realistic rugby scores
        match.homeScore = (match.homeScore / 3) * 3; // Multiple of 3
        match.awayScore = (match.awayScore / 3) * 3;

        // Update team stats
        updateTeamStats(match);

        // Simulate match duration
        try {
            Thread.sleep(MATCH_DURATION);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void updateTeamStats(Match match) {
        Team winner = match.homeScore > match.awayScore ? match.homeTeam :
                match.awayScore > match.homeScore ? match.awayTeam : null;

        if (winner != null) {
            winner.wins++;
            winner.points += 3;
            winner.morale += 2;

            Team loser = winner == match.homeTeam ? match.awayTeam : match.homeTeam;
            loser.losses++;
            loser.morale = Math.max(0, loser.morale - 1);
        } else {
            // Draw
            match.homeTeam.draws++;
            match.awayTeam.draws++;
            match.homeTeam.points += 1;
            match.awayTeam.points += 1;
        }

        // Update fitness (decreases after match)
        match.homeTeam.fitness = Math.max(0, match.homeTeam.fitness - 1);
        match.awayTeam.fitness = Math.max(0, match.awayTeam.fitness - 1);
    }
}

/**
 * Tournament management class
 */
class Tournament {
    private List<Team> teams;
    private List<Match> matches;
    private int currentRound = 0;

    public Tournament(List<Team> teams) {
        this.teams = new ArrayList<>(teams);
        generateMatches();
    }

    private void generateMatches() {
        matches = new ArrayList<>();

        // Generate round-robin style matches
        for (int i = 0; i < teams.size(); i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                matches.add(new Match(teams.get(i), teams.get(j)));
            }
        }

        // Shuffle matches
        Collections.shuffle(matches);
    }

    public Match getNextMatch(Team playerTeam) {
        for (Match match : matches) {
            if (!match.played && (match.homeTeam == playerTeam || match.awayTeam == playerTeam)) {
                match.played = true;
                return match;
            }
        }
        return null;
    }

    public List<Team> getLeaderboard() {
        List<Team> sorted = new ArrayList<>(teams);
        sorted.sort((t1, t2) -> {
            if (t1.points != t2.points) return t2.points - t1.points;
            return t2.wins - t1.wins;
        });
        return sorted;
    }

    public int getTeamPosition(Team team) {
        List<Team> leaderboard = getLeaderboard();
        return leaderboard.indexOf(team) + 1;
    }

    public List<Match> getCompletedMatches() {
        return matches.stream().filter(m -> m.played).collect(java.util.stream.Collectors.toList());
    }
}

/**
 * Team class representing a rugby team
 */
class Team {
    String name;
    int attack, defense, fitness, morale;
    int wins, losses, draws, points;
    boolean isPlayerTeam = false;

    public Team(String name) {
        this.name = name;
        this.points = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
    }

    public int getOverallRating() {
        return (attack + defense + fitness + morale) / 4;
    }
}

/**
 * Match class representing a rugby match
 */
class Match {
    Team homeTeam, awayTeam;
    int homeScore, awayScore;
    boolean played = false;

    public Match(Team home, Team away) {
        this.homeTeam = home;
        this.awayTeam = away;
    }
}

/**
 * Animation panel for visual effects
 */
class AnimationPanel extends JPanel {
    private boolean matchAnimationActive = false;
    private Match currentMatch;
    private int animationFrame = 0;
    private boolean paused = false;
    private List<AnimatedPlayer> players = new ArrayList<>();
    private List<AnimatedBall> balls = new ArrayList<>();

    public AnimationPanel() {
        setBackground(new Color(34, 139, 34)); // Rugby field green
        setPreferredSize(new Dimension(800, 600));
    }

    public void startMatchAnimation(Match match) {
        currentMatch = match;
        matchAnimationActive = true;
        animationFrame = 0;
        initializeMatchAnimation();
    }

    public void endMatchAnimation() {
        matchAnimationActive = false;
        players.clear();
        balls.clear();
    }

    public void togglePause() {
        paused = !paused;
    }

    private void initializeMatchAnimation() {
        players.clear();
        balls.clear();

        // Create animated players
        Random rand = new Random();
        for (int i = 0; i < 10; i++) {
            players.add(new AnimatedPlayer(
                    rand.nextInt(getWidth()),
                    rand.nextInt(getHeight()),
                    i < 5 ? Color.RED : Color.BLUE
            ));
        }

        // Create ball
        balls.add(new AnimatedBall(getWidth() / 2, getHeight() / 2));
    }

    public void updateAnimation() {
        if (paused || !matchAnimationActive) return;

        animationFrame++;

        // Update player positions
        for (AnimatedPlayer player : players) {
            player.update(getWidth(), getHeight());
        }

        // Update ball positions
        for (AnimatedBall ball : balls) {
            ball.update(getWidth(), getHeight());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawField(g2d);

        if (matchAnimationActive) {
            drawMatchAnimation(g2d);
        } else {
            drawWelcomeScreen(g2d);
        }
    }

    private void drawField(Graphics2D g2d) {
        // Draw rugby field lines
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));

        // Center line
        g2d.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());

        // Try lines
        g2d.drawLine(0, 50, getWidth(), 50);
        g2d.drawLine(0, getHeight() - 50, getWidth(), getHeight() - 50);

        // Goal posts
        drawGoalPosts(g2d, getWidth() / 2, 20);
        drawGoalPosts(g2d, getWidth() / 2, getHeight() - 20);
    }

    private void drawGoalPosts(Graphics2D g2d, int x, int y) {
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(x - 20, y - 5, 40, 10);
        g2d.fillRect(x - 2, y - 30, 4, 30);
        g2d.fillRect(x - 12, y - 30, 4, 15);
        g2d.fillRect(x + 8, y - 30, 4, 15);
    }

    private void drawMatchAnimation(Graphics2D g2d) {
        // Draw players
        for (AnimatedPlayer player : players) {
            player.draw(g2d);
        }

        // Draw balls
        for (AnimatedBall ball : balls) {
            ball.draw(g2d);
        }

        // Draw score
        if (currentMatch != null) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            String score = currentMatch.homeTeam.name + " " + currentMatch.homeScore +
                    " - " + currentMatch.awayScore + " " + currentMatch.awayTeam.name;
            g2d.drawString(score, 20, 30);
        }
    }

    private void drawWelcomeScreen(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String title = "Rugby World Cup Manager";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(title)) / 2;
        g2d.drawString(title, x, getHeight() / 2);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String subtitle = "Click 'Play Match' to start your journey!";
        fm = g2d.getFontMetrics();
        x = (getWidth() - fm.stringWidth(subtitle)) / 2;
        g2d.drawString(subtitle, x, getHeight() / 2 + 50);
    }
}

/**
 * Animated player class
 */
class AnimatedPlayer {
    private double x, y, vx, vy;
    private Color color;
    private Random rand = new Random();

    public AnimatedPlayer(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.vx = (rand.nextDouble() - 0.5) * 4;
        this.vy = (rand.nextDouble() - 0.5) * 4;
    }

    public void update(int width, int height) {
        x += vx;
        y += vy;

        // Bounce off walls
        if (x < 0 || x > width) vx *= -1;
        if (y < 0 || y > height) vy *= -1;

        // Keep in bounds
        x = Math.max(0, Math.min(width, x));
        y = Math.max(0, Math.min(height, y));
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillOval((int)x - 5, (int)y - 5, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawOval((int)x - 5, (int)y - 5, 10, 10);
    }
}

/**
 * Animated ball class
 */
class AnimatedBall {
    private double x, y, vx, vy;
    private Random rand = new Random();

    public AnimatedBall(int x, int y) {
        this.x = x;
        this.y = y;
        this.vx = (rand.nextDouble() - 0.5) * 6;
        this.vy = (rand.nextDouble() - 0.5) * 6;
    }

    public void update(int width, int height) {
        x += vx;
        y += vy;

        // Bounce off walls
        if (x < 0 || x > width) vx *= -1;
        if (y < 0 || y > height) vy *= -1;

        // Keep in bounds
        x = Math.max(0, Math.min(width, x));
        y = Math.max(0, Math.min(height, y));
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int)x - 4, (int)y - 4, 8, 8);
        g2d.setColor(Color.BLACK);
        g2d.drawOval((int)x - 4, (int)y - 4, 8, 8);
    }
}

/**
 * Control panel for game actions
 */
class ControlPanel extends JPanel {
    JButton playMatchBtn, manageTeamBtn, viewStatsBtn, pauseBtn;

    public ControlPanel() {
        setLayout(new FlowLayout());
        setBackground(new Color(70, 70, 70));

        playMatchBtn = new JButton("Play Match");
        manageTeamBtn = new JButton("Manage Team");
        viewStatsBtn = new JButton("View Statistics");
        pauseBtn = new JButton("Pause/Resume");

        // Style buttons
        JButton[] buttons = {playMatchBtn, manageTeamBtn, viewStatsBtn, pauseBtn};
        for (JButton btn : buttons) {
            btn.setPreferredSize(new Dimension(150, 40));
            btn.setBackground(new Color(100, 149, 237));
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("Arial", Font.BOLD, 12));
            add(btn);
        }
    }
}

/**
 * Information panel showing team stats
 */
class InfoPanel extends JPanel {
    private JTextArea infoArea;

    public InfoPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 600));
        setBackground(new Color(50, 50, 50));

        infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setBackground(new Color(40, 40, 40));
        infoArea.setForeground(Color.WHITE);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(infoArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateInfo(Team playerTeam, Tournament tournament) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== TEAM STATUS ===\n");
        sb.append("Team: ").append(playerTeam.name).append("\n");
        sb.append("Position: ").append(tournament.getTeamPosition(playerTeam)).append("\n");
        sb.append("Points: ").append(playerTeam.points).append("\n");
        sb.append("W-L-D: ").append(playerTeam.wins).append("-")
                .append(playerTeam.losses).append("-").append(playerTeam.draws).append("\n\n");

        sb.append("=== TEAM STATS ===\n");
        sb.append("Attack: ").append(playerTeam.attack).append("\n");
        sb.append("Defense: ").append(playerTeam.defense).append("\n");
        sb.append("Fitness: ").append(playerTeam.fitness).append("\n");
        sb.append("Morale: ").append(playerTeam.morale).append("\n");
        sb.append("Overall: ").append(playerTeam.getOverallRating()).append("\n\n");

        sb.append("=== LEADERBOARD ===\n");
        List<Team> leaderboard = tournament.getLeaderboard();
        for (int i = 0; i < Math.min(8, leaderboard.size()); i++) {
            Team team = leaderboard.get(i);
            sb.append(String.format("%d. %s (%d pts)\n", i + 1, team.name, team.points));
        }

        infoArea.setText(sb.toString());
    }
}

/**
 * Team management dialog
 */
class TeamManagementDialog extends JDialog {
    private Team team;

    public TeamManagementDialog(Frame parent, Team team) {
        super(parent, "Team Management", true);
        this.team = team;
        initializeDialog();
    }

    private void initializeDialog() {
        setSize(400, 300);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Team stats sliders
        JSlider attackSlider = new JSlider(0, 100, team.attack);
        JSlider defenseSlider = new JSlider(0, 100, team.defense);
        JSlider fitnessSlider = new JSlider(0, 100, team.fitness);
        JSlider moraleSlider = new JSlider(0, 100, team.morale);

        mainPanel.add(new JLabel("Attack:"));
        mainPanel.add(attackSlider);
        mainPanel.add(new JLabel("Defense:"));
        mainPanel.add(defenseSlider);
        mainPanel.add(new JLabel("Fitness:"));
        mainPanel.add(fitnessSlider);
        mainPanel.add(new JLabel("Morale:"));
        mainPanel.add(moraleSlider);

        JButton saveBtn = new JButton("Save Changes");
        saveBtn.addActionListener(e -> {
            team.attack = attackSlider.getValue();
            team.defense = defenseSlider.getValue();
            team.fitness = fitnessSlider.getValue();
            team.morale = moraleSlider.getValue();
            dispose();
        });

        mainPanel.add(new JLabel(""));
        mainPanel.add(saveBtn);

        add(mainPanel, BorderLayout.CENTER);
    }
}

/**
 * Statistics dialog
 */
class StatisticsDialog extends JDialog {
    public StatisticsDialog(Frame parent, Tournament tournament, Team playerTeam) {
        super(parent, "Tournament Statistics", true);
        setSize(500, 400);
        setLocationRelativeTo(parent);

        JTextArea statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        StringBuilder sb = new StringBuilder();
        sb.append("=== TOURNAMENT STATISTICS ===\n\n");

        sb.append("Your Team Performance:\n");
        sb.append("Position: ").append(tournament.getTeamPosition(playerTeam)).append("\n");
        sb.append("Points: ").append(playerTeam.points).append("\n");
        sb.append("Wins: ").append(playerTeam.wins).append("\n");
        sb.append("Losses: ").append(playerTeam.losses).append("\n");
        sb.append("Draws: ").append(playerTeam.draws).append("\n\n");

        sb.append("Full Leaderboard:\n");
        List<Team> leaderboard = tournament.getLeaderboard();
        for (int i = 0; i < leaderboard.size(); i++) {
            Team team = leaderboard.get(i);
            sb.append(String.format("%2d. %-15s %3d pts (%d-%d-%d)\n",
                    i + 1, team.name, team.points, team.wins, team.losses, team.draws));
        }

        statsArea.setText(sb.toString());

        add(new JScrollPane(statsArea));
    }
}