import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ThreadLocalRandom;

public class DartsTournament extends JFrame {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final Color BOARD_COLOR = new Color(0, 100, 0);
    private static final Color BULLSEYE_COLOR = Color.RED;
    private static final Color OUTER_BULL_COLOR = Color.GREEN;
    private static final Color SEGMENT_COLOR1 = Color.WHITE;
    private static final Color SEGMENT_COLOR2 = Color.BLACK;
    private static final Color WIRE_COLOR = new Color(150, 150, 150);

    private List<Player> players = new ArrayList<>();
    private List<Player> activePlayers = new ArrayList<>();
    private Player currentPlayer;
    private int currentRound = 1;
    private int currentTurn = 1;
    private int currentThrow = 1;
    private int tournamentPhase = 0; // 0: setup, 1: group, 2: knockout, 3: final
    private boolean animationInProgress = false;
    private DartAnimation currentAnimation;

    private JPanel mainPanel;
    private JPanel controlPanel;
    private JPanel scorePanel;
    private JPanel animationPanel;
    private JTextArea infoArea;
    private JButton nextButton;
    private JButton startButton;
    private JComboBox<String> playerCountCombo;

    public DartsTournament() {
        setTitle("Darts Tournament Simulation");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        initializePlayers();
        setupUI();
    }

    private void initializePlayers() {
        String[] playerNames = {
                "Phil Taylor", "Michael van Gerwen", "Gary Anderson",
                "Peter Wright", "Raymond van Barneveld", "Rob Cross",
                "James Wade", "Glen Durrant", "Daryl Gurney", "Gerwyn Price",
                "Adrian Lewis", "Mensur Suljovic", "Simon Whitlock", "Dave Chisnall",
                "Nathan Aspinall", "Ian White", "Joe Cullen", "Jonny Clayton"
        };

        for (String name : playerNames) {
            players.add(new Player(name));
        }
    }

    private void setupUI() {
        mainPanel = new JPanel(new BorderLayout());
        controlPanel = new JPanel(new FlowLayout());
        scorePanel = new JPanel(new GridLayout(0, 1));
        animationPanel = new DartBoardPanel();

        infoArea = new JTextArea(10, 40);
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(infoArea);

        nextButton = new JButton("Next Throw");
        nextButton.addActionListener(e -> nextAction());
        nextButton.setEnabled(false);

        startButton = new JButton("Start Tournament");
        startButton.addActionListener(e -> startTournament());

        playerCountCombo = new JComboBox<>(new String[]{"4 Players", "8 Players", "16 Players"});

        controlPanel.add(startButton);
        controlPanel.add(playerCountCombo);
        controlPanel.add(nextButton);

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.SOUTH);
        mainPanel.add(animationPanel, BorderLayout.CENTER);
        mainPanel.add(scorePanel, BorderLayout.EAST);

        add(mainPanel);

        updateInfo("Welcome to Darts Tournament Simulation!\nSelect number of players and click Start.");
    }

    private void startTournament() {
        int playerCount = 4;
        switch (playerCountCombo.getSelectedIndex()) {
            case 0: playerCount = 4; break;
            case 1: playerCount = 8; break;
            case 2: playerCount = 16; break;
        }

        // Select random players
        Collections.shuffle(players);
        activePlayers = new ArrayList<>(players.subList(0, playerCount));

        tournamentPhase = 1; // Group phase
        currentRound = 1;
        currentTurn = 1;
        currentThrow = 1;
        currentPlayer = activePlayers.get(0);

        nextButton.setEnabled(true);
        startButton.setEnabled(false);
        playerCountCombo.setEnabled(false);

        updateInfo("Tournament started with " + playerCount + " players!\n\n" +
                "First match: " + activePlayers.get(0).getName() + " vs " +
                activePlayers.get(1).getName());

        updateScorePanel();
    }

    private void nextAction() {
        if (animationInProgress) {
            return;
        }

        if (currentThrow == 1) {
            updateInfo(currentPlayer.getName() + "'s turn (Round " + currentRound + ")");
        }

        // Simulate dart throw
        DartThrow dartThrow = currentPlayer.throwDart(currentThrow);

        // Animate the throw
        animateDartThrow(dartThrow);

        // Update throw counter
        currentThrow++;
        if (currentThrow > 3) {
            currentThrow = 1;
            currentTurn++;

            // Check if match is over
            if (currentTurn > 10) { // Best of 10 legs
                endMatch();
                return;
            }

            // Switch player
            currentPlayer = (currentPlayer == activePlayers.get(0)) ?
                    activePlayers.get(1) : activePlayers.get(0);
        }
    }

    private void animateDartThrow(DartThrow dartThrow) {
        animationInProgress = true;
        currentAnimation = new DartAnimation(dartThrow);
        Timer timer = new Timer(20, e -> {
            if (!currentAnimation.update()) {
                ((Timer)e.getSource()).stop();
                animationInProgress = false;
                handleThrowResult(currentAnimation.getThrow());
                animationPanel.repaint();
            } else {
                animationPanel.repaint();
            }
        });
        timer.start();
    }

    private void handleThrowResult(DartThrow dartThrow) {
        currentPlayer.addScore(dartThrow.getScore());
        updateInfo(currentPlayer.getName() + " scored " + dartThrow.getScore() + " points!\n" +
                "Current score: " + currentPlayer.getScore() + "\n" +
                "Throw: " + dartThrow.getDescription());

        updateScorePanel();
    }

    private void endMatch() {
        Player winner = (activePlayers.get(0).getScore() > activePlayers.get(1).getScore()) ?
                activePlayers.get(0) : activePlayers.get(1);
        Player loser = (winner == activePlayers.get(0)) ? activePlayers.get(1) : activePlayers.get(0);

        winner.addWin();
        loser.addLoss();

        updateInfo("Match finished!\nWinner: " + winner.getName() + " (" + winner.getScore() + ")\n" +
                "Loser: " + loser.getName() + " (" + loser.getScore() + ")\n\n" +
                "Preparing next match...");

        // Tournament progression logic would go here
        // For simplicity, we'll just reset for a new match
        resetForNewMatch();
    }

    private void resetForNewMatch() {
        // In a real tournament, this would handle bracket progression
        Collections.shuffle(activePlayers);
        activePlayers.get(0).resetScore();
        activePlayers.get(1).resetScore();
        currentPlayer = activePlayers.get(0);
        currentTurn = 1;
        currentThrow = 1;
        currentRound++;

        updateInfo("Next match: " + activePlayers.get(0).getName() + " vs " +
                activePlayers.get(1).getName() + "\nRound " + currentRound);

        updateScorePanel();
    }

    private void updateScorePanel() {
        scorePanel.removeAll();

        // Tournament standings
        JLabel standingsLabel = new JLabel("TOURNAMENT STANDINGS");
        standingsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        scorePanel.add(standingsLabel);

        // Sort by wins
        activePlayers.sort((p1, p2) -> p2.getWins() - p1.getWins());

        for (Player player : activePlayers) {
            JLabel playerLabel = new JLabel(String.format("%-20s W:%-2d L:%-2d Avg:%.1f",
                    player.getName(), player.getWins(), player.getLosses(), player.getAverage()));
            playerLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));

            if (player == currentPlayer) {
                playerLabel.setForeground(Color.RED);
                playerLabel.setFont(playerLabel.getFont().deriveFont(Font.BOLD));
            }

            scorePanel.add(playerLabel);
        }

        // Current match info
        JLabel matchLabel = new JLabel("CURRENT MATCH");
        matchLabel.setFont(new Font("Arial", Font.BOLD, 14));
        scorePanel.add(matchLabel);

        for (Player player : activePlayers.subList(0, 2)) {
            JLabel scoreLabel = new JLabel(String.format("%-20s %3d",
                    player.getName(), player.getScore()));
            scoreLabel.setFont(new Font("Monospaced", Font.PLAIN, 14));
            scorePanel.add(scoreLabel);
        }

        scorePanel.revalidate();
        scorePanel.repaint();
    }

    private void updateInfo(String text) {
        infoArea.setText(text);
    }

    class DartBoardPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D)g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw dartboard
            drawDartBoard(g2d);

            // Draw dart if animation is in progress
            if (animationInProgress && currentAnimation != null) {
                currentAnimation.draw(g2d);
            }
        }

        private void drawDartBoard(Graphics2D g2d) {
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            int maxRadius = Math.min(getWidth(), getHeight()) / 2 - 20;

            // Draw outer board
            g2d.setColor(BOARD_COLOR);
            g2d.fillOval(centerX - maxRadius, centerY - maxRadius, maxRadius * 2, maxRadius * 2);

            // Draw segments
            double segmentAngle = Math.PI / 10;
            for (int i = 0; i < 20; i++) {
                if (i % 2 == 0) {
                    g2d.setColor(SEGMENT_COLOR1);
                } else {
                    g2d.setColor(SEGMENT_COLOR2);
                }

                double startAngle = i * segmentAngle - segmentAngle / 2;
                double endAngle = startAngle + segmentAngle;

                // Triple ring
                g2d.fill(new Arc2D.Double(
                        centerX - maxRadius * 0.8, centerY - maxRadius * 0.8,
                        maxRadius * 1.6, maxRadius * 1.6,
                        Math.toDegrees(startAngle), Math.toDegrees(segmentAngle),
                        Arc2D.PIE));

                // Double ring
                g2d.fill(new Arc2D.Double(
                        centerX - maxRadius * 0.4, centerY - maxRadius * 0.4,
                        maxRadius * 0.8, maxRadius * 0.8,
                        Math.toDegrees(startAngle), Math.toDegrees(segmentAngle),
                        Arc2D.PIE));
            }

            // Draw wires
            g2d.setColor(WIRE_COLOR);
            for (int i = 0; i < 20; i++) {
                double angle = i * segmentAngle;
                int x = (int)(centerX + Math.cos(angle) * maxRadius);
                int y = (int)(centerY + Math.sin(angle) * maxRadius);
                g2d.drawLine(centerX, centerY, x, y);
            }

            // Draw rings
            g2d.drawOval(centerX - maxRadius, centerY - maxRadius, maxRadius * 2, maxRadius * 2);
            g2d.drawOval(centerX - (int)(maxRadius * 0.8), centerY - (int)(maxRadius * 0.8),
                    (int)(maxRadius * 1.6), (int)(maxRadius * 1.6));
            g2d.drawOval(centerX - (int)(maxRadius * 0.4), centerY - (int)(maxRadius * 0.4),
                    (int)(maxRadius * 0.8), (int)(maxRadius * 0.8));

            // Draw bullseye
            g2d.setColor(OUTER_BULL_COLOR);
            g2d.fillOval(centerX - maxRadius / 10, centerY - maxRadius / 10,
                    maxRadius / 5, maxRadius / 5);
            g2d.setColor(BULLSEYE_COLOR);
            g2d.fillOval(centerX - maxRadius / 20, centerY - maxRadius / 20,
                    maxRadius / 10, maxRadius / 10);
        }
    }

    class DartAnimation {
        private DartThrow dartThrow;
        private double x, y;
        private double dx, dy;
        private double progress = 0;
        private static final double ANIMATION_DURATION = 1.0; // seconds
        private long startTime;

        public DartAnimation(DartThrow dartThrow) {
            this.dartThrow = dartThrow;
            this.startTime = System.currentTimeMillis();

            // Start from random position off-screen
            Random rand = new Random();
            int side = rand.nextInt(4);
            switch (side) {
                case 0: // top
                    x = rand.nextInt(animationPanel.getWidth());
                    y = -20;
                    break;
                case 1: // right
                    x = animationPanel.getWidth() + 20;
                    y = rand.nextInt(animationPanel.getHeight());
                    break;
                case 2: // bottom
                    x = rand.nextInt(animationPanel.getWidth());
                    y = animationPanel.getHeight() + 20;
                    break;
                case 3: // left
                    x = -20;
                    y = rand.nextInt(animationPanel.getHeight());
                    break;
            }

            // Calculate target position based on dart throw
            Point target = calculateTargetPosition(dartThrow);
            dx = target.x - x;
            dy = target.y - y;
        }

        public boolean update() {
            long currentTime = System.currentTimeMillis();
            progress = (currentTime - startTime) / (ANIMATION_DURATION * 1000);

            if (progress >= 1.0) {
                progress = 1.0;
                return false;
            }

            // Ease-out function
            double easedProgress = 1 - Math.pow(1 - progress, 3);
            x += dx * easedProgress;
            y += dy * easedProgress;

            return true;
        }

        public void draw(Graphics2D g2d) {
            int centerX = animationPanel.getWidth() / 2;
            int centerY = animationPanel.getHeight() / 2;

            // Draw dart
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine((int)x, (int)y, (int)(x + dx * 0.1), (int)(y + dy * 0.1));

            // Draw flight
            g2d.setColor(Color.RED);
            int[] xPoints = {(int)x, (int)(x - dy * 0.05), (int)(x - dy * 0.1)};
            int[] yPoints = {(int)y, (int)(y + dx * 0.05), (int)(y + dx * 0.1)};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }

        public DartThrow getThrow() {
            return dartThrow;
        }

        private Point calculateTargetPosition(DartThrow dartThrow) {
            int centerX = animationPanel.getWidth() / 2;
            int centerY = animationPanel.getHeight() / 2;
            int maxRadius = Math.min(animationPanel.getWidth(), animationPanel.getHeight()) / 2 - 20;

            // Calculate angle based on segment (simplified)
            double angle = Math.PI * 2 * (dartThrow.getSegment() / 20.0);

            // Calculate distance based on multiplier
            double distance;
            switch (dartThrow.getMultiplier()) {
                case 3: distance = maxRadius * 0.7; break; // Triple ring
                case 2: distance = maxRadius * 0.3; break; // Double ring
                case 1: distance = maxRadius * 0.5; break; // Single area
                default: distance = maxRadius * 0.05; // Bullseye
            }

            // Add some randomness to make it look more natural
            Random rand = new Random();
            distance *= 0.9 + 0.2 * rand.nextDouble();
            angle += (rand.nextDouble() - 0.5) * Math.PI / 10;

            int targetX = centerX + (int)(Math.cos(angle) * distance);
            int targetY = centerY + (int)(Math.sin(angle) * distance);

            return new Point(targetX, targetY);
        }
    }

    class Player {
        private String name;
        private int score;
        private int wins;
        private int losses;
        private List<Integer> throwHistory = new ArrayList<>();
        private double skill; // 0.0 to 1.0

        public Player(String name) {
            this.name = name;
            this.score = 0;
            this.wins = 0;
            this.losses = 0;
            this.skill = 0.7 + 0.3 * Math.random(); // Random skill level
        }

        public String getName() {
            return name;
        }

        public int getScore() {
            return score;
        }

        public void addScore(int points) {
            score += points;
            throwHistory.add(points);
        }

        public void resetScore() {
            score = 0;
        }

        public int getWins() {
            return wins;
        }

        public void addWin() {
            wins++;
        }

        public int getLosses() {
            return losses;
        }

        public void addLoss() {
            losses++;
        }

        public double getAverage() {
            if (throwHistory.isEmpty()) return 0;
            return throwHistory.stream().mapToInt(Integer::intValue).average().orElse(0);
        }

        public DartThrow throwDart(int throwNumber) {
            Random rand = new Random();

            // Determine target based on strategy and skill
            int segment, multiplier;
            double r = rand.nextDouble();

            if (r < 0.05 * skill) {
                // Bullseye attempt
                segment = 0;
                multiplier = (rand.nextDouble() < 0.5) ? 2 : 1; // Outer or inner bull
            } else {
                segment = 1 + rand.nextInt(20);
                if (r < 0.15 * skill) {
                    multiplier = 3; // Triple
                } else if (r < 0.3 * skill) {
                    multiplier = 2; // Double
                } else {
                    multiplier = 1; // Single
                }
            }

            // Add some randomness based on skill
            if (rand.nextDouble() > skill) {
                // Miss the intended target
                segment = (segment + rand.nextInt(3) - 1);
                if (segment < 1) segment = 20;
                if (segment > 20) segment = 1;

                if (rand.nextDouble() > 0.7) {
                    multiplier = 1;
                }
            }

            // Special case for bullseye
            if (segment == 0) {
                if (multiplier == 1) {
                    return new DartThrow(0, 1, "Bullseye (50)");
                } else {
                    return new DartThrow(0, 1, "Outer Bull (25)");
                }
            }

            // Calculate score
            int score = segment * multiplier;
            String description = "";

            switch (multiplier) {
                case 3: description = "Triple " + segment; break;
                case 2: description = "Double " + segment; break;
                default: description = "Single " + segment;
            }

            return new DartThrow(segment, multiplier, description + " (" + score + ")");
        }
    }

    class DartThrow {
        private int segment;
        private int multiplier;
        private String description;
        private int score;

        public DartThrow(int segment, int multiplier, String description) {
            this.segment = segment;
            this.multiplier = multiplier;
            this.description = description;
            this.score = (segment == 0) ?
                    (multiplier == 1 ? 50 : 25) :
                    segment * multiplier;
        }

        public int getSegment() {
            return segment;
        }

        public int getMultiplier() {
            return multiplier;
        }

        public String getDescription() {
            return description;
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