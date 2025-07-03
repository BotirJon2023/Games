import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class RugbySevensSimulatorGame {
    private JFrame frame;
    private GamePanel gamePanel;
    private Team homeTeam;
    private Team awayTeam;
    private Match currentMatch;
    private boolean gameRunning = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RugbySevensSimulatorGame().initialize());
    }

    private void initialize() {
        // Create teams
        homeTeam = new Team("New Zealand", 90);
        awayTeam = new Team("South Africa", 88);

        // Set up the main frame
        frame = new JFrame("Rugby Sevens Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout());

        // Create game panel
        gamePanel = new GamePanel();
        frame.add(gamePanel, BorderLayout.CENTER);

        // Create control panel
        JPanel controlPanel = new JPanel();
        JButton startButton = new JButton("Start Match");
        startButton.addActionListener(e -> startMatch());
        controlPanel.add(startButton);

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));
        controlPanel.add(exitButton);

        frame.add(controlPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void startMatch() {
        if (!gameRunning) {
            currentMatch = new Match(homeTeam, awayTeam);
            gameRunning = true;
            new MatchThread().start();
        }
    }

    class MatchThread extends Thread {
        @Override
        public void run() {
            currentMatch.playMatch(gamePanel);
            gameRunning = false;
        }
    }

    class GamePanel extends JPanel {
        private List<Player> homePlayers = new ArrayList<>();
        private List<Player> awayPlayers = new ArrayList<>();
        private Ball ball;
        private String matchStatus = "Pre-match";
        private int homeScore = 0;
        private int awayScore = 0;
        private int gameTime = 0;
        private int halfTime = 7 * 60; // 7 minutes per half in seconds

        public GamePanel() {
            setBackground(new Color(0, 100, 0)); // Rugby field green
            setPreferredSize(new Dimension(900, 500));
        }

        public void updateGameState(List<Player> homePlayers, List<Player> awayPlayers, Ball ball,
                                    String status, int homeScore, int awayScore, int time) {
            this.homePlayers = new ArrayList<>(homePlayers);
            this.awayPlayers = new ArrayList<>(awayPlayers);
            this.ball = ball;
            this.matchStatus = status;
            this.homeScore = homeScore;
            this.awayScore = awayScore;
            this.gameTime = time;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            // Draw field markings
            drawField(g2d);

            // Draw players
            for (Player player : homePlayers) {
                player.draw(g2d, Color.BLACK);
            }
            for (Player player : awayPlayers) {
                player.draw(g2d, Color.WHITE);
            }

            // Draw ball
            if (ball != null) {
                ball.draw(g2d);
            }

            // Draw scoreboard
            drawScoreboard(g2d);

            // Draw match status
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString(matchStatus, 20, 30);

            // Draw game time
            int minutes = gameTime / 60;
            int seconds = gameTime % 60;
            g2d.drawString(String.format("Time: %02d:%02d", minutes, seconds), getWidth() - 150, 30);
        }

        private void drawField(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));

            // Halfway line
            g2d.drawLine(getWidth() / 2, 50, getWidth() / 2, getHeight() - 50);

            // 22-meter lines
            g2d.drawLine(150, 50, 150, getHeight() - 50);
            g2d.drawLine(getWidth() - 150, 50, getWidth() - 150, getHeight() - 50);

            // Try lines (goals)
            g2d.drawLine(50, 50, 50, getHeight() - 50);
            g2d.drawLine(getWidth() - 50, 50, getWidth() - 50, getHeight() - 50);

            // Draw goal posts
            g2d.drawLine(50, getHeight() / 2 - 30, 30, getHeight() / 2 - 30);
            g2d.drawLine(50, getHeight() / 2 + 30, 30, getHeight() / 2 + 30);
            g2d.drawLine(getWidth() - 50, getHeight() / 2 - 30, getWidth() - 30, getHeight() / 2 - 30);
            g2d.drawLine(getWidth() - 50, getHeight() / 2 + 30, getWidth() - 30, getHeight() / 2 + 30);
        }

        private void drawScoreboard(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(getWidth() / 2 - 100, 10, 200, 40);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString(homeTeam.getName() + " " + homeScore + " - " + awayScore + " " + awayTeam.getName(),
                    getWidth() / 2 - 90, 35);
        }
    }

    class Team {
        private String name;
        private int overallRating;
        private List<Player> players;

        public Team(String name, int overallRating) {
            this.name = name;
            this.overallRating = overallRating;
            this.players = new ArrayList<>();
            initializePlayers();
        }

        private void initializePlayers() {
            Random rand = new Random();
            for (int i = 0; i < 7; i++) {
                int speed = 70 + rand.nextInt(20) + (overallRating - 80);
                int strength = 70 + rand.nextInt(20) + (overallRating - 80);
                int skill = 70 + rand.nextInt(20) + (overallRating - 80);
                players.add(new Player("Player " + (i + 1), speed, strength, skill));
            }
        }

        public String getName() {
            return name;
        }

        public List<Player> getPlayers() {
            return players;
        }

        public int getOverallRating() {
            return overallRating;
        }
    }

    class Player {
        private String name;
        private int speed;
        private int strength;
        private int skill;
        private int x, y;
        private boolean hasBall = false;

        public Player(String name, int speed, int strength, int skill) {
            this.name = name;
            this.speed = speed;
            this.strength = strength;
            this.skill = skill;
        }

        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void setHasBall(boolean hasBall) {
            this.hasBall = hasBall;
        }

        public void draw(Graphics2D g2d, Color teamColor) {
            g2d.setColor(teamColor);
            g2d.fillOval(x - 10, y - 10, 20, 20);

            if (hasBall) {
                g2d.setColor(Color.WHITE);
                g2d.fillOval(x - 5, y - 5, 10, 10);
            }
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getSpeed() {
            return speed;
        }

        public int getStrength() {
            return strength;
        }

        public int getSkill() {
            return skill;
        }

        public boolean hasBall() {
            return false;
        }
    }

    class Ball {
        private int x, y;

        public Ball(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.fillOval(x - 5, y - 5, 10, 10);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    class Match {
        private Team homeTeam;
        private Team awayTeam;
        private int homeScore;
        private int awayScore;
        private int gameTime;
        private boolean inProgress;
        private Ball ball;
        private Random random;

        public Match(Team homeTeam, Team awayTeam) {
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
            this.homeScore = 0;
            this.awayScore = 0;
            this.gameTime = 0;
            this.inProgress = false;
            this.random = new Random();
            this.ball = new Ball(450, 250); // Center of the field
        }

        public void playMatch(GamePanel gamePanel) {
            inProgress = true;
            gameTime = 0;
            int halfTime = 7 * 60; // 7 minutes per half in seconds
            int matchDuration = 2 * halfTime;

            // Initial player positions
            setupInitialPositions();

            // Kickoff
            gamePanel.updateGameState(homeTeam.getPlayers(), awayTeam.getPlayers(), ball,
                    "Kickoff!", homeScore, awayScore, gameTime);
            pause(1000);

            // Main match loop
            while (gameTime < matchDuration && inProgress) {
                gameTime++;
                simulateGameSecond();

                // Update game panel
                gamePanel.updateGameState(homeTeam.getPlayers(), awayTeam.getPlayers(), ball,
                        getMatchStatus(), homeScore, awayScore, gameTime);

                // Check for tries
                checkForTry();

                // Pause for animation
                pause(50);

                // Half time break
                if (gameTime == halfTime) {
                    gamePanel.updateGameState(homeTeam.getPlayers(), awayTeam.getPlayers(), ball,
                            "Half Time", homeScore, awayScore, gameTime);
                    pause(2000);
                }
            }

            // Match ended
            inProgress = false;
            gamePanel.updateGameState(homeTeam.getPlayers(), awayTeam.getPlayers(), ball,
                    "Match Ended", homeScore, awayScore, gameTime);
        }

        private void setupInitialPositions() {
            // Home team positions (left side)
            List<Player> homePlayers = homeTeam.getPlayers();
            homePlayers.get(0).setPosition(200, 150); // Forward
            homePlayers.get(1).setPosition(250, 200);
            homePlayers.get(2).setPosition(250, 300);
            homePlayers.get(3).setPosition(300, 150);
            homePlayers.get(4).setPosition(300, 250);
            homePlayers.get(5).setPosition(300, 350);
            homePlayers.get(6).setPosition(350, 250); // Scrum half

            // Away team positions (right side)
            List<Player> awayPlayers = awayTeam.getPlayers();
            awayPlayers.get(0).setPosition(700, 150); // Forward
            awayPlayers.get(1).setPosition(650, 200);
            awayPlayers.get(2).setPosition(650, 300);
            awayPlayers.get(3).setPosition(600, 150);
            awayPlayers.get(4).setPosition(600, 250);
            awayPlayers.get(5).setPosition(600, 350);
            awayPlayers.get(6).setPosition(550, 250); // Scrum half

            // Ball starts with home team
            homePlayers.get(6).setHasBall(true);
            ball.setPosition(homePlayers.get(6).getX(), homePlayers.get(6).getY());
        }

        private void simulateGameSecond() {
            // Simple simulation logic - in a real game this would be much more complex
            List<Player> allPlayers = new ArrayList<>();
            allPlayers.addAll(homeTeam.getPlayers());
            allPlayers.addAll(awayTeam.getPlayers());

            // Find player with ball
            Player ballCarrier = null;
            for (Player player : allPlayers) {
                if (player.hasBall()) {
                    ballCarrier = player;
                    break;
                }
            }

            if (ballCarrier != null) {
                // Move ball carrier
                movePlayerWithBall(ballCarrier);

                // Chance to pass or be tackled
                if (random.nextInt(100) < 20) { // 20% chance per second of an event
                    if (random.nextBoolean()) {
                        // Pass the ball
                        passBall(ballCarrier);
                    } else {
                        // Tackle attempt
                        attemptTackle(ballCarrier);
                    }
                }
            } else {
                // Ball is loose - players try to get it
                scrambleForBall();
            }
        }

        private void movePlayerWithBall(Player player) {
            int direction = random.nextInt(4);
            int distance = player.getSpeed() / 20;

            int newX = player.getX();
            int newY = player.getY();

            switch (direction) {
                case 0: // Up
                    newY -= distance;
                    break;
                case 1: // Right
                    newX += distance;
                    break;
                case 2: // Down
                    newY += distance;
                    break;
                case 3: // Left
                    newX -= distance;
                    break;
            }

            // Keep player within bounds
            newX = Math.max(50, Math.min(850, newX));
            newY = Math.max(70, Math.min(430, newY));

            player.setPosition(newX, newY);
            ball.setPosition(newX, newY);
        }

        private void passBall(Player passer) {
            List<Player> teammates = passer.getY() < 250 ? homeTeam.getPlayers() : awayTeam.getPlayers();
            List<Player> potentialReceivers = new ArrayList<>();

            for (Player teammate : teammates) {
                if (teammate != passer && !teammate.hasBall()) {
                    double distance = Math.sqrt(Math.pow(teammate.getX() - passer.getX(), 2) +
                            Math.pow(teammate.getY() - passer.getY(), 2));
                    if (distance < 150) { // Only consider nearby players
                        potentialReceivers.add(teammate);
                    }
                }
            }

            if (!potentialReceivers.isEmpty()) {
                Player receiver = potentialReceivers.get(random.nextInt(potentialReceivers.size()));
                passer.setHasBall(false);
                receiver.setHasBall(true);
                ball.setPosition(receiver.getX(), receiver.getY());
            }
        }

        private void attemptTackle(Player ballCarrier) {
            List<Player> opponents = ballCarrier.getY() < 250 ? awayTeam.getPlayers() : homeTeam.getPlayers();
            Player tackler = null;
            double minDistance = Double.MAX_VALUE;

            for (Player opponent : opponents) {
                double distance = Math.sqrt(Math.pow(opponent.getX() - ballCarrier.getX(), 2) +
                        Math.pow(opponent.getY() - ballCarrier.getY(), 2));
                if (distance < 50 && distance < minDistance) { // Only consider nearby players
                    minDistance = distance;
                    tackler = opponent;
                }
            }

            if (tackler != null) {
                // Tackle success depends on player attributes
                int tackleChance = 50 + (tackler.getStrength() - ballCarrier.getStrength()) / 2;
                if (random.nextInt(100) < tackleChance) {
                    // Successful tackle - ball goes loose
                    ballCarrier.setHasBall(false);
                    ball.setPosition(ballCarrier.getX(), ballCarrier.getY());
                }
            }
        }

        private void scrambleForBall() {
            List<Player> allPlayers = new ArrayList<>();
            allPlayers.addAll(homeTeam.getPlayers());
            allPlayers.addAll(awayTeam.getPlayers());

            // Find closest player to the ball
            Player closestPlayer = null;
            double minDistance = Double.MAX_VALUE;

            for (Player player : allPlayers) {
                double distance = Math.sqrt(Math.pow(player.getX() - ball.getX(), 2) +
                        Math.pow(player.getY() - ball.getY(), 2));
                if (distance < minDistance) {
                    minDistance = distance;
                    closestPlayer = player;
                }
            }

            if (closestPlayer != null && minDistance < 30) {
                // Player picks up the ball
                closestPlayer.setHasBall(true);
                ball.setPosition(closestPlayer.getX(), closestPlayer.getY());
            } else if (ball.getX() < 50 || ball.getX() > 850) {
                // Ball went into touch (out of bounds)
                // In a real game, this would result in a lineout
                ball.setPosition(450, 250); // Reset to center
            }
        }

        private void checkForTry() {
            // Check if ball is over try line
            if (ball.getX() < 60) { // Away team try
                awayScore += 5;
                resetAfterScore();
            } else if (ball.getX() > 840) { // Home team try
                homeScore += 5;
                resetAfterScore();
            }
        }

        private void resetAfterScore() {
            // Reset positions after a try is scored
            setupInitialPositions();

            // The team that was scored against gets the ball
            if (ball.getX() < 60) { // Away team scored, so home team gets ball
                for (Player player : homeTeam.getPlayers()) {
                    player.setHasBall(false);
                }
                homeTeam.getPlayers().get(6).setHasBall(true);
                ball.setPosition(homeTeam.getPlayers().get(6).getX(), homeTeam.getPlayers().get(6).getY());
            } else { // Home team scored, so away team gets ball
                for (Player player : awayTeam.getPlayers()) {
                    player.setHasBall(false);
                }
                awayTeam.getPlayers().get(6).setHasBall(true);
                ball.setPosition(awayTeam.getPlayers().get(6).getX(), awayTeam.getPlayers().get(6).getY());
            }
        }

        private String getMatchStatus() {
            if (gameTime < 7 * 60) {
                return "First Half";
            } else if (gameTime < 14 * 60) {
                return "Second Half";
            } else {
                return "Match Ended";
            }
        }

        private void pause(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}