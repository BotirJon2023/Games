import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import java.util.List;

public class DartsTournament extends JFrame {
    private static final int BOARD_WIDTH = 800;
    private static final int BOARD_HEIGHT = 600;
    private static final int DARTBOARD_SIZE = 400;
    private static final int NUM_PLAYERS = 4;
    private static final int MAX_THROWS_PER_TURN = 3;
    private static final int STARTING_SCORE = 501;

    private List<Player> players;
    private int currentPlayerIndex;
    private int currentRound;
    private JLabel statusLabel;
    private DartsPanel dartsPanel;
    private JButton throwButton;
    private List<DartThrow> currentThrows;
    private Timer animationTimer;

    public DartsTournament() {
        initGame();
        initUI();
    }

    private void initGame() {
        players = new ArrayList<>();
        String[] names = {"Alice", "Bob", "Charlie", "Dave"};
        for (String name : names) {
            players.add(new Player(name, STARTING_SCORE));
        }
        currentPlayerIndex = 0;
        currentRound = 1;
        currentThrows = new ArrayList<>();
    }

    private void initUI() {
        setTitle("Darts Tournament Simulation");
        setSize(BOARD_WIDTH, BOARD_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        dartsPanel = new DartsPanel();
        add(dartsPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        throwButton = new JButton("Throw Dart");
        throwButton.addActionListener(e -> handleThrow());
        controlPanel.add(throwButton);

        statusLabel = new JLabel(getStatusText(), SwingConstants.CENTER);
        controlPanel.add(statusLabel);

        JButton resetButton = new JButton("Reset Game");
        resetButton.addActionListener(e -> resetGame());
        controlPanel.add(resetButton);

        add(controlPanel, BorderLayout.SOUTH);

        animationTimer = new Timer(50, e -> animateThrows());
        animationTimer.start();
    }

    private String getStatusText() {
        Player currentPlayer = players.get(currentPlayerIndex);
        return String.format("Round %d | %s's Turn | Score: %d | Throws Left: %d",
                currentRound, currentPlayer.getName(), currentPlayer.getScore(),
                currentPlayer.getThrowsLeft());
    }

    private void handleThrow() {
        Player currentPlayer = players.get(currentPlayerIndex);
        if (currentPlayer.getThrowsLeft() > 0) {
            DartThrow dartThrow = new DartThrow();
            currentThrows.add(dartThrow);
            currentPlayer.makeThrow(dartThrow.getScore());
            updateStatus();
            if (currentPlayer.getThrowsLeft() == 0 || currentPlayer.getScore() == 0) {
                nextPlayer();
            }
            dartsPanel.repaint();
        }
    }

    private void updateStatus() {
        statusLabel.setText(getStatusText());
        if (checkForWinner()) {
            throwButton.setEnabled(false);
            animationTimer.stop();
            JOptionPane.showMessageDialog(this,
                    players.get(currentPlayerIndex).getName() + " wins the tournament!",
                    "Game Over", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private boolean checkForWinner() {
        Player currentPlayer = players.get(currentPlayerIndex);
        return currentPlayer.getScore() == 0 && currentPlayer.isValidFinish();
    }

    private void nextPlayer() {
        Player currentPlayer = players.get(currentPlayerIndex);
        currentPlayer.resetThrows();
        currentThrows.clear();
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        if (currentPlayerIndex == 0) {
            currentRound++;
        }
        dartsPanel.repaint();
    }

    private void resetGame() {
        initGame();
        currentThrows.clear();
        throwButton.setEnabled(true);
        animationTimer.start();
        updateStatus();
        dartsPanel.repaint();
    }

    private void animateThrows() {
        for (DartThrow dartThrow : currentThrows) {
            dartThrow.updateAnimation();
        }
        dartsPanel.repaint();
    }

    class DartsPanel extends JPanel {
        private Image dartboardImage;

        public DartsPanel() {
            try {
                // Placeholder for dartboard image - in a real application, load an actual image
                dartboardImage = new BufferedImage(DARTBOARD_SIZE, DARTBOARD_SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = (Graphics2D) dartboardImage.getGraphics();
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, DARTBOARD_SIZE, DARTBOARD_SIZE);
                drawDartboard(g2d);
                g2d.dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void drawDartboard(Graphics2D g2d) {
            int centerX = DARTBOARD_SIZE / 2;
            int centerY = DARTBOARD_SIZE / 2;

            // Draw outer bullseye
            g2d.setColor(Color.RED);
            g2d.fillOval(centerX - 10, centerY - 10, 20, 20);

            // Draw bullseye
            g2d.setColor(Color.GREEN);
            g2d.fillOval(centerX - 5, centerY - 5, 10, 10);

            // Draw scoring rings
            for (int i = 1; i <= 5; i++) {
                int radius = i * 50;
                g2d.setColor(i % 2 == 0 ? Color.BLACK : Color.GREEN);
                g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
            }

            // Draw sector lines
            g2d.setColor(Color.BLACK);
            for (int i = 0; i < 20; i++) {
                double angle = Math.toRadians(i * 18);
                int x = (int) (centerX + Math.cos(angle) * DARTBOARD_SIZE / 2);
                int y = (int) (centerY + Math.sin(angle) * DARTBOARD_SIZE / 2);
                g2d.drawLine(centerX, centerY, x, y);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw dartboard
            int x = (getWidth() - DARTBOARD_SIZE) / 2;
            int y = (getHeight() - DARTBOARD_SIZE) / 2;
            g2d.drawImage(dartboardImage, x, y, null);

            // Draw darts
            for (DartThrow dartThrow : currentThrows) {
                dartThrow.draw(g2d, x, y);
            }

            // Draw player scores
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            int scoreY = 50;
            for (Player player : players) {
                g2d.drawString(player.getName() + ": " + player.getScore(), 50, scoreY);
                scoreY += 30;
            }
        }
    }

    class Player {
        private String name;
        private int score;
        private int throwsLeft;
        private List<Integer> throwHistory;

        public Player(String name, int initialScore) {
            this.name = name;
            this.score = initialScore;
            this.throwsLeft = MAX_THROWS_PER_TURN;
            this.throwHistory = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public int getScore() {
            return score;
        }

        public int getThrowsLeft() {
            return throwsLeft;
        }

        public void makeThrow(int points) {
            if (throwsLeft > 0) {
                score -= points;
                throwsLeft--;
                throwHistory.add(points);
                if (score < 0) {
                    score += points; // Revert invalid throw
                    throwHistory.remove(throwHistory.size() - 1);
                }
            }
        }

        public boolean isValidFinish() {
            if (score == 0) {
                int lastThrow = throwHistory.get(throwHistory.size() - 1);
                return lastThrow % 2 == 0 || lastThrow == 50; // Must finish on double or bullseye
            }
            return false;
        }

        public void resetThrows() {
            throwsLeft = MAX_THROWS_PER_TURN;
        }
    }

    class DartThrow {
        private double x, y;
        private double targetX, targetY;
        private double animationProgress;
        private int score;
        private Random random;

        public DartThrow() {
            random = new Random();
            animationProgress = 0;
            calculateTarget();
            score = calculateScore();
            x = random.nextInt(BOARD_WIDTH);
            y = random.nextInt(BOARD_HEIGHT);
        }

        private void calculateTarget() {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = random.nextDouble() * DARTBOARD_SIZE / 2;
            targetX = DARTBOARD_SIZE / 2 + radius * Math.cos(angle);
            targetY = DARTBOARD_SIZE / 2 + radius * Math.sin(angle);
        }

        private int calculateScore() {
            double distance = Math.sqrt(Math.pow(targetX - DARTBOARD_SIZE / 2, 2) +
                    Math.pow(targetY - DARTBOARD_SIZE / 2, 2));
            if (distance < 10) return 50; // Bullseye
            if (distance < 20) return 25; // Outer bull
            if (distance > DARTBOARD_SIZE / 2) return 0; // Miss

            double angle = Math.toDegrees(Math.atan2(targetY - DARTBOARD_SIZE / 2,
                    targetX - DARTBOARD_SIZE / 2));
            if (angle < 0) angle += 360;
            int sector = (int) (angle / 18);
            int[] scores = {6, 13, 4, 18, 1, 20, 5, 12, 9, 14, 11, 8, 16, 7, 19, 3, 17, 2, 15, 10};
            int baseScore = scores[sector];

            if (distance < 50) return baseScore * 3; // Triple ring
            if (distance < 100) return baseScore; // Normal
            if (distance < 150) return baseScore * 2; // Double ring
            return baseScore;
        }

        public void updateAnimation() {
            if (animationProgress < 1) {
                animationProgress += 0.05;
                x = x + (targetX - x) * 0.1;
                y = y + (targetY - y) * 0.1;
            }
        }

        public void draw(Graphics2D g2d, int offsetX, int offsetY) {
            g2d.setColor(Color.RED);
            int drawX = (int) (offsetX + x);
            int drawY = (int) (offsetY + y);
            g2d.fillOval(drawX - 5, drawY - 5, 10, 10);
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.valueOf(score), drawX + 10, drawY);
        }

        public int getScore() {
            return score;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DartsTournament game = new DartsTournament();
            game.setVisible(true);
        });
    }
}

// Additional classes for tournament management
class TournamentManager {
    private List<DartsTournament.Player> players;
    private int currentMatch;
    private List<Match> matches;

    public TournamentManager(List<DartsTournament.Player> players) {
        this.players = new ArrayList<>(players);
        this.currentMatch = 0;
        this.matches = new ArrayList<>();
        generateMatches();
    }

    private void generateMatches() {
        for (int i = 0; i < players.size(); i += 2) {
            if (i + 1 < players.size()) {
                matches.add(new Match(players.get(i), players.get(i + 1)));
            }
        }
    }

    public Match getCurrentMatch() {
        return matches.get(currentMatch);
    }

    public void advanceTournament() {
        currentMatch++;
        if (currentMatch >= matches.size()) {
            // Tournament complete
        }
    }
}

class Match {
    private Player player1;
    private Player player2;
    private Player winner;

    public Match(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
    }

    public Match(DartsTournament.Player player, DartsTournament.Player player1) {
    }

    public void setWinner(Player winner) {
        this.winner = winner;
    }

    public Player getWinner() {
        return winner;
    }
}

// Utility class for game statistics
class GameStatistics {
    private List<Player> players;
    private List<Integer> scores;
    private int totalThrows;

    public GameStatistics(List<Player> players) {
        this.players = players;
        this.scores = new ArrayList<>();
        this.totalThrows = 0;
    }

    public void recordThrow(int score) {
        scores.add(score);
        totalThrows++;
    }

    public double getAverageScore() {
        if (scores.isEmpty()) return 0;
        return scores.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    public int getTotalThrows() {
        return totalThrows;
    }
}

// Configuration class for game settings
class GameConfig {
    public static final int MAX_ROUNDS = 10;
    public static final int ANIMATION_SPEED = 50;
    public static final Color DART_COLOR = Color.RED;
    public static final Color BOARD_COLOR = Color.GREEN;
    public static final Font SCORE_FONT = new Font("Arial", Font.BOLD, 14);
}