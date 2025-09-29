import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

// Player class representing a team member
class Player {
    private int x, y;
    private int targetX, targetY;
    private int speed;
    private int team; // 0 for home, 1 for away
    private int number;
    private boolean hasBall;
    private Color color;
    private String role; // Guard, Forward, Center
    private int skill; // 1-10 rating

    public Player(int x, int y, int team, int number, String role, Color color) {
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.team = team;
        this.number = number;
        this.role = role;
        this.color = color;
        this.speed = 3 + (int)(Math.random() * 3);
        this.skill = 5 + (int)(Math.random() * 6); // Skill between 5-10
        this.hasBall = false;
    }

    public void setTarget(int targetX, int targetY) {
        this.targetX = targetX;
        this.targetY = targetY;
    }

    public void update() {
        // Move towards target
        int dx = targetX - x;
        int dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > speed) {
            x += (dx / distance) * speed;
            y += (dy / distance) * speed;
        } else {
            x = targetX;
            y = targetY;
        }
    }

    public void draw(Graphics g) {
        // Draw player
        g.setColor(color);
        g.fillOval(x - 10, y - 10, 20, 20);

        // Draw number
        g.setColor(team == 0 ? Color.WHITE : Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString(Integer.toString(number), x - 4, y + 4);

        // Draw ball if player has it
        if (hasBall) {
            g.setColor(Color.ORANGE);
            g.fillOval(x + 5, y - 5, 10, 10);
        }
    }

    // Getters and setters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getTeam() { return team; }
    public boolean hasBall() { return hasBall; }
    public void setHasBall(boolean hasBall) { this.hasBall = hasBall; }
    public int getSkill() { return skill; }
    public String getRole() { return role; }
}

// Team class representing a sports team
class Team {
    private String name;
    private Color color;
    private List<Player> players;
    private int score;
    private String strategy; // "Offensive", "Defensive", "Balanced"

    public Team(String name, Color color, String strategy) {
        this.name = name;
        this.color = color;
        this.strategy = strategy;
        this.players = new ArrayList<>();
        this.score = 0;
        initializePlayers();
    }

    private void initializePlayers() {
        // Create 5 players with different roles
        String[] roles = {"Point Guard", "Shooting Guard", "Small Forward", "Power Forward", "Center"};

        for (int i = 0; i < 5; i++) {
            int x = 100 + (i * 80);
            int y = 300;
            players.add(new Player(x, y, 0, i + 1, roles[i], color));
        }
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void updatePlayers() {
        for (Player player : players) {
            player.update();
        }
    }

    public void drawPlayers(Graphics g) {
        for (Player player : players) {
            player.draw(g);
        }
    }

    public void addScore(int points) {
        score += points;
    }

    public int getScore() {
        return score;
    }

    public String getName() {
        return name;
    }

    public String getStrategy() {
        return strategy;
    }
}

// Ball class for game physics
class Ball {
    private int x, y;
    private int targetX, targetY;
    private int speed;
    private boolean isMoving;
    private Player holder;

    public Ball(int x, int y) {
        this.x = x;
        this.y = y;
        this.isMoving = false;
        this.holder = null;
        this.speed = 8;
    }

    public void setTarget(int targetX, int targetY) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.isMoving = true;
        this.holder = null;
    }

    public void update() {
        if (isMoving) {
            int dx = targetX - x;
            int dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > speed) {
                x += (dx / distance) * speed;
                y += (dy / distance) * speed;
            } else {
                x = targetX;
                y = targetY;
                isMoving = false;
            }
        }
    }

    public void draw(Graphics g) {
        if (holder == null) {
            g.setColor(Color.ORANGE);
            g.fillOval(x - 5, y - 5, 10, 10);
        }
    }

    // Getters and setters
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isMoving() { return isMoving; }
    public void setHolder(Player holder) {
        this.holder = holder;
        if (holder != null) {
            this.x = holder.getX() + 5;
            this.y = holder.getY() - 5;
            this.isMoving = false;
        }
    }
    public Player getHolder() { return holder; }
}

// AI System for team decision making
class AICoach {
    private Team team;
    private Team opponent;
    private Ball ball;
    private Random random;

    public AICoach(Team team, Team opponent, Ball ball) {
        this.team = team;
        this.opponent = opponent;
        this.ball = ball;
        this.random = new Random();
    }

    public void makeDecisions() {
        // If our team has the ball, decide on offense
        if (hasBall()) {
            decideOffense();
        } else {
            decideDefense();
        }
    }

    private boolean hasBall() {
        for (Player player : team.getPlayers()) {
            if (player.hasBall()) return true;
        }
        return false;
    }

    private void decideOffense() {
        Player ballHandler = getBallHandler();
        if (ballHandler == null) return;

        // Based on strategy, decide what to do
        String strategy = team.getStrategy();

        switch (strategy) {
            case "Offensive":
                offensivePlay(ballHandler);
                break;
            case "Defensive":
                defensivePlay(ballHandler);
                break;
            default: // Balanced
                balancedPlay(ballHandler);
                break;
        }
    }

    private void decideDefense() {
        // Set defensive positions based on ball position
        int ballX = ball.getX();
        int ballY = ball.getY();

        for (Player player : team.getPlayers()) {
            // Move toward ball or defensive position
            int targetX, targetY;

            if (random.nextInt(100) < 30) { // 30% chance to pressure ball handler
                targetX = ballX + random.nextInt(40) - 20;
                targetY = ballY + random.nextInt(40) - 20;
            } else { // Play position defense
                // Simple zone defense positioning
                int playerIndex = team.getPlayers().indexOf(player);
                targetX = 400 + (playerIndex % 3) * 80 - 120;
                targetY = 200 + (playerIndex / 3) * 100;
            }

            player.setTarget(targetX, targetY);
        }
    }

    private void offensivePlay(Player ballHandler) {
        // Aggressive offensive strategy - look for shot or drive
        if (random.nextInt(100) < 40) { // 40% chance to shoot
            attemptShot(ballHandler);
        } else if (random.nextInt(100) < 60) { // 60% chance to drive
            driveToBasket(ballHandler);
        } else { // Pass to a teammate
            passToTeammate(ballHandler);
        }
    }

    private void defensivePlay(Player ballHandler) {
        // Conservative offensive strategy - prioritize ball possession
        if (random.nextInt(100) < 20) { // 20% chance to shoot
            attemptShot(ballHandler);
        } else if (random.nextInt(100) < 40) { // 40% chance to drive
            driveToBasket(ballHandler);
        } else { // 40% chance to pass
            passToTeammate(ballHandler);
        }
    }

    private void balancedPlay(Player ballHandler) {
        // Balanced offensive strategy
        if (random.nextInt(100) < 30) { // 30% chance to shoot
            attemptShot(ballHandler);
        } else if (random.nextInt(100) < 50) { // 50% chance to drive
            driveToBasket(ballHandler);
        } else { // 50% chance to pass
            passToTeammate(ballHandler);
        }
    }

    private void attemptShot(Player shooter) {
        // Determine shot success based on skill and distance
        int basketX = shooter.getTeam() == 0 ? 750 : 50;
        int basketY = 300;

        double distance = Math.sqrt(
                Math.pow(shooter.getX() - basketX, 2) +
                        Math.pow(shooter.getY() - basketY, 2)
        );

        // Skill affects success rate, distance makes it harder
        int successChance = shooter.getSkill() * 10 - (int)(distance / 10);
        successChance = Math.max(10, Math.min(90, successChance));

        if (random.nextInt(100) < successChance) {
            // Shot successful - score!
            ball.setTarget(basketX, basketY);
            team.addScore(2);
        } else {
            // Shot missed - ball goes to random location
            int missX = basketX + random.nextInt(100) - 50;
            int missY = basketY + random.nextInt(100) - 50;
            ball.setTarget(missX, missY);
        }

        shooter.setHasBall(false);
    }

    private void driveToBasket(Player driver) {
        int basketX = driver.getTeam() == 0 ? 750 : 50;
        int basketY = 300;

        // Move toward basket
        driver.setTarget(basketX, basketY);

        // 70% chance to keep driving, 30% chance to pass or shoot during drive
        if (random.nextInt(100) < 70) {
            // Continue driving
            if (Math.abs(driver.getX() - basketX) < 50 &&
                    Math.abs(driver.getY() - basketY) < 50) {
                // Close to basket - attempt layup
                attemptShot(driver);
            }
        } else {
            // Pass during drive
            passToTeammate(driver);
        }
    }

    private void passToTeammate(Player passer) {
        List<Player> teammates = new ArrayList<>(team.getPlayers());
        teammates.remove(passer); // Can't pass to yourself

        if (teammates.isEmpty()) return;

        // Select a teammate to pass to (prefer open teammates)
        Player receiver = teammates.get(random.nextInt(teammates.size()));

        // Set ball target to receiver
        ball.setTarget(receiver.getX(), receiver.getY());
        passer.setHasBall(false);
    }

    private Player getBallHandler() {
        for (Player player : team.getPlayers()) {
            if (player.hasBall()) return player;
        }
        return null;
    }
}

// Main game panel
class SportsGamePanel extends JPanel implements ActionListener {
    private Team homeTeam;
    private Team awayTeam;
    private Ball ball;
    private AICoach homeCoach;
    private AICoach awayCoach;
    private Timer gameTimer;
    private int gameTime; // in seconds
    private int quarter;
    private boolean gameActive;
    private JLabel scoreLabel;
    private JLabel timeLabel;
    private JTextArea gameLog;

    public SportsGamePanel() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(new Color(50, 120, 50)); // Court color

        // Initialize teams
        homeTeam = new Team("Home Team", Color.BLUE, "Balanced");
        awayTeam = new Team("Away Team", Color.RED, "Offensive");

        // Initialize ball
        ball = new Ball(400, 300);

        // Initialize AI coaches
        homeCoach = new AICoach(homeTeam, awayTeam, ball);
        awayCoach = new AICoach(awayTeam, homeTeam, ball);

        // Give ball to a random player to start
        Player firstPlayer = homeTeam.getPlayers().get(new Random().nextInt(5));
        firstPlayer.setHasBall(true);
        ball.setHolder(firstPlayer);

        // Game state
        gameTime = 12 * 60; // 12-minute quarters
        quarter = 1;
        gameActive = true;

        // Set up UI components
        setupUI();

        // Start game timer
        gameTimer = new Timer(50, this); // ~20 FPS
        gameTimer.start();
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // Score panel at top
        JPanel topPanel = new JPanel(new GridLayout(1, 3));
        topPanel.setBackground(Color.DARK_GRAY);
        topPanel.setForeground(Color.WHITE);

        scoreLabel = new JLabel("Home: 0 - Away: 0", JLabel.CENTER);
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 18));

        timeLabel = new JLabel("Q" + quarter + " 12:00", JLabel.CENTER);
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 18));

        JLabel titleLabel = new JLabel("AI Sports Simulation", JLabel.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));

        topPanel.add(scoreLabel);
        topPanel.add(titleLabel);
        topPanel.add(timeLabel);
        add(topPanel, BorderLayout.NORTH);

        // Game log at bottom
        gameLog = new JTextArea(5, 80);
        gameLog.setEditable(false);
        gameLog.setBackground(Color.BLACK);
        gameLog.setForeground(Color.WHITE);
        gameLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(gameLog);
        add(scrollPane, BorderLayout.SOUTH);

        logEvent("Game started! " + homeTeam.getName() + " vs " + awayTeam.getName());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw court
        drawCourt(g);

        // Draw teams and ball
        homeTeam.drawPlayers(g);
        awayTeam.drawPlayers(g);
        ball.draw(g);

        // Draw baskets
        g.setColor(Color.ORANGE);
        g.fillRect(40, 290, 10, 20);  // Left basket
        g.fillRect(750, 290, 10, 20); // Right basket
    }

    private void drawCourt(Graphics g) {
        // Court outline
        g.setColor(new Color(30, 100, 30));
        g.fillRect(0, 0, 800, 600);

        // Court lines
        g.setColor(Color.WHITE);
        g.drawRect(50, 50, 700, 500); // Outer boundary

        // Center circle
        g.drawOval(375, 275, 50, 50);

        // Center line
        g.drawLine(400, 50, 400, 550);

        // Free throw circles
        g.drawOval(100, 250, 100, 100);  // Left
        g.drawOval(600, 250, 100, 100);  // Right

        // Key (paint area)
        g.drawRect(50, 250, 100, 100);   // Left
        g.drawRect(650, 250, 100, 100);  // Right
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameActive) {
            // Update game time
            gameTime--;
            updateTimeDisplay();

            // Check for end of quarter
            if (gameTime <= 0) {
                endQuarter();
                return;
            }

            // Update ball position
            ball.update();

            // Update player positions
            homeTeam.updatePlayers();
            awayTeam.updatePlayers();

            // Check for ball possession changes
            checkBallPossession();

            // AI makes decisions
            if (ball.getHolder() != null) {
                if (ball.getHolder().getTeam() == 0) {
                    homeCoach.makeDecisions();
                } else {
                    awayCoach.makeDecisions();
                }
            } else if (!ball.isMoving()) {
                // Ball is loose - players should go for it
                for (Player player : homeTeam.getPlayers()) {
                    player.setTarget(ball.getX(), ball.getY());
                }
                for (Player player : awayTeam.getPlayers()) {
                    player.setTarget(ball.getX(), ball.getY());
                }
            }

            // Repaint the game
            repaint();
        }
    }

    private void checkBallPossession() {
        // If ball is not moving and not held, check if a player can grab it
        if (!ball.isMoving() && ball.getHolder() == null) {
            // Check home team players
            for (Player player : homeTeam.getPlayers()) {
                double distance = Math.sqrt(
                        Math.pow(player.getX() - ball.getX(), 2) +
                                Math.pow(player.getY() - ball.getY(), 2)
                );

                if (distance < 20) { // Close enough to grab the ball
                    player.setHasBall(true);
                    ball.setHolder(player);
                    logEvent(homeTeam.getName() + " gains possession!");
                    return;
                }
            }

            // Check away team players
            for (Player player : awayTeam.getPlayers()) {
                double distance = Math.sqrt(
                        Math.pow(player.getX() - ball.getX(), 2) +
                                Math.pow(player.getY() - ball.getY(), 2)
                );

                if (distance < 20) { // Close enough to grab the ball
                    player.setHasBall(true);
                    ball.setHolder(player);
                    logEvent(awayTeam.getName() + " gains possession!");
                    return;
                }
            }
        }

        // If ball is moving toward a player, they might catch it
        if (ball.isMoving()) {
            // Check all players for interception
            for (Player player : homeTeam.getPlayers()) {
                double distance = Math.sqrt(
                        Math.pow(player.getX() - ball.getX(), 2) +
                                Math.pow(player.getY() - ball.getY(), 2)
                );

                if (distance < 15) { // Close enough to intercept
                    player.setHasBall(true);
                    ball.setHolder(player);
                    logEvent(homeTeam.getName() + " intercepts the ball!");
                    return;
                }
            }

            for (Player player : awayTeam.getPlayers()) {
                double distance = Math.sqrt(
                        Math.pow(player.getX() - ball.getX(), 2) +
                                Math.pow(player.getY() - ball.getY(), 2)
                );

                if (distance < 15) { // Close enough to intercept
                    player.setHasBall(true);
                    ball.setHolder(player);
                    logEvent(awayTeam.getName() + " intercepts the ball!");
                    return;
                }
            }
        }
    }

    private void updateTimeDisplay() {
        int minutes = gameTime / 60;
        int seconds = gameTime % 60;
        timeLabel.setText(String.format("Q%d %02d:%02d", quarter, minutes, seconds));
        scoreLabel.setText(String.format("%s: %d - %s: %d",
                homeTeam.getName(), homeTeam.getScore(),
                awayTeam.getName(), awayTeam.getScore()));
    }

    private void endQuarter() {
        gameActive = false;
        logEvent("End of Quarter " + quarter);

        if (quarter < 4) {
            // Start next quarter after a delay
            Timer quarterTimer = new Timer(3000, e -> {
                quarter++;
                gameTime = 12 * 60; // 12-minute quarters
                gameActive = true;

                // Reset player positions
                resetPlayers();

                // Give ball to the team that didn't have it last
                Team startingTeam = (ball.getHolder() != null && ball.getHolder().getTeam() == 0) ? awayTeam : homeTeam;
                Player firstPlayer = startingTeam.getPlayers().get(new Random().nextInt(5));
                firstPlayer.setHasBall(true);
                ball.setHolder(firstPlayer);

                logEvent("Quarter " + quarter + " begins! " + startingTeam.getName() + " with the ball.");
            });
            quarterTimer.setRepeats(false);
            quarterTimer.start();
        } else {
            // Game over
            logEvent("Game Over!");
            String winner = homeTeam.getScore() > awayTeam.getScore() ?
                    homeTeam.getName() : awayTeam.getName();
            logEvent(winner + " wins!");

            // Show final score dialog
            JOptionPane.showMessageDialog(this,
                    "Final Score:\n" +
                            homeTeam.getName() + ": " + homeTeam.getScore() + "\n" +
                            awayTeam.getName() + ": " + awayTeam.getScore() + "\n\n" +
                            winner + " wins!",
                    "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void resetPlayers() {
        // Reset home team positions
        List<Player> homePlayers = homeTeam.getPlayers();
        for (int i = 0; i < homePlayers.size(); i++) {
            Player player = homePlayers.get(i);
            player.setTarget(100 + (i * 80), 300);
        }

        // Reset away team positions
        List<Player> awayPlayers = awayTeam.getPlayers();
        for (int i = 0; i < awayPlayers.size(); i++) {
            Player player = awayPlayers.get(i);
            player.setTarget(500 + (i * 80), 300);
        }
    }

    private void logEvent(String event) {
        gameLog.append("[" + getFormattedTime() + "] " + event + "\n");
        gameLog.setCaretPosition(gameLog.getDocument().getLength());
    }

    private String getFormattedTime() {
        int minutes = gameTime / 60;
        int seconds = gameTime % 60;
        return String.format("Q%d %02d:%02d", quarter, minutes, seconds);
    }
}

// Main game frame
public class AISportsSimulation extends JFrame {
    public AISportsSimulation() {
        setTitle("AI-Powered Sports Team Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        SportsGamePanel gamePanel = new SportsGamePanel();
        add(gamePanel);

        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AISportsSimulation game = new AISportsSimulation();
            game.setVisible(true);
        });
    }
}