import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.sun.beans.introspect.ClassInfo.clear;
import static java.util.Arrays.stream;
import static sun.util.locale.LocaleUtils.isEmpty;

public class DartsTournamentGame extends JFrame {

    // Game constants
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int DARTBOARD_SIZE = 400;
    private static final int DARTBOARD_X = 50;
    private static final int DARTBOARD_Y = 50;

    // Game components
    private GamePanel gamePanel;
    private Tournament tournament;
    private DartboardRenderer dartboard;
    private AnimationManager animationManager;
    private SoundManager soundManager;
    private GameState gameState;

    // UI Components
    private JPanel controlPanel;
    private JButton startTournamentBtn;
    private JButton nextThrowBtn;
    private JButton resetBtn;
    private JLabel currentPlayerLabel;
    private JLabel scoreLabel;
    private JLabel roundLabel;
    private JTextArea logArea;

    public DartsTournamentGame() {
        initializeGame();
        setupUI();
        startGameLoop();
    }

    private void initializeGame() {
        setTitle("Darts Tournament Simulation");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Initialize game components
        tournament = new Tournament();
        dartboard = new DartboardRenderer();
        animationManager = new AnimationManager();
        soundManager = new SoundManager();
        gameState = GameState.SETUP;

        gamePanel = new GamePanel();
        add(gamePanel);
    }

    private void setupUI() {
        controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBackground(Color.DARK_GRAY);

        startTournamentBtn = new JButton("Start Tournament");
        nextThrowBtn = new JButton("Next Throw");
        resetBtn = new JButton("Reset Game");

        currentPlayerLabel = new JLabel("Current Player: None");
        scoreLabel = new JLabel("Score: 0");
        roundLabel = new JLabel("Round: Setup");

        currentPlayerLabel.setForeground(Color.WHITE);
        scoreLabel.setForeground(Color.WHITE);
        roundLabel.setForeground(Color.WHITE);

        logArea = new JTextArea(8, 30);
        logArea.setEditable(false);
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        JScrollPane logScroll = new JScrollPane(logArea);

        setupButtonListeners();

        controlPanel.add(startTournamentBtn);
        controlPanel.add(nextThrowBtn);
        controlPanel.add(resetBtn);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(currentPlayerLabel);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(scoreLabel);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(roundLabel);

        add(controlPanel, BorderLayout.NORTH);
        add(logScroll, BorderLayout.EAST);
    }

    private void setupButtonListeners() {
        startTournamentBtn.addActionListener(e -> {
            tournament.initialize();
            gameState = GameState.IN_PROGRESS;
            startTournamentBtn.setEnabled(false);
            nextThrowBtn.setEnabled(true);
            updateUI();
            logMessage("Tournament started with " + tournament.getPlayers().size() + " players!");
        });

        nextThrowBtn.addActionListener(e -> {
            if (gameState == GameState.IN_PROGRESS) {
                performNextThrow();
            }
        });

        resetBtn.addActionListener(e -> {
            resetGame();
        });
    }

    private void performNextThrow() {
        Player currentPlayer = tournament.getCurrentPlayer();
        if (currentPlayer != null) {
            DartThrow dartThrow = currentPlayer.throwDart();
            animationManager.addDartAnimation(dartThrow);
            soundManager.playThrowSound();

            tournament.processThrow(dartThrow);
            updateUI();

            String throwResult = String.format("%s threw: %d points (%.1f, %.1f)",
                    currentPlayer.getName(), dartThrow.getScore(),
                    dartThrow.getX(), dartThrow.getY());
            logMessage(throwResult);

            if (tournament.isGameComplete()) {
                Player winner = tournament.getWinner();
                if (winner != null) {
                    logMessage("WINNER: " + winner.getName() + "!");
                    gameState = GameState.FINISHED;
                    nextThrowBtn.setEnabled(false);
                    soundManager.playWinSound();
                }
            }
        }
    }

    private void updateUI() {
        Player currentPlayer = tournament.getCurrentPlayer();
        if (currentPlayer != null) {
            currentPlayerLabel.setText("Current Player: " + currentPlayer.getName());
            scoreLabel.setText("Score: " + currentPlayer.getCurrentScore());
        }

        roundLabel.setText("Round: " + tournament.getCurrentRound() +
                " | Match: " + tournament.getCurrentMatch());

        gamePanel.repaint();
    }

    private void logMessage(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void resetGame() {
        tournament.reset();
        animationManager.clear();
        gameState = GameState.SETUP;
        startTournamentBtn.setEnabled(true);
        nextThrowBtn.setEnabled(false);
        logArea.setText("");
        updateUI();
        logMessage("Game reset. Ready for new tournament!");
    }

    private void startGameLoop() {
        Timer gameTimer = new Timer(16, e -> {
            animationManager.update();
            gamePanel.repaint();
        });
        gameTimer.start();
    }

    // Inner Classes

    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background
            g2d.setColor(new Color(20, 30, 40));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Draw dartboard
            dartboard.render(g2d, DARTBOARD_X, DARTBOARD_Y, DARTBOARD_SIZE);

            // Draw animations
            animationManager.render(g2d);

            // Draw tournament bracket
            drawTournamentBracket(g2d);

            // Draw player stats
            drawPlayerStats(g2d);
        }

        private void drawTournamentBracket(Graphics2D g2d) {
            int x = DARTBOARD_X + DARTBOARD_SIZE + 50;
            int y = 50;

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Tournament Bracket", x, y);

            y += 30;
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));

            List<Player> players = tournament.getPlayers();
            for (int i = 0; i < players.size(); i++) {
                Player player = players.get(i);
                Color playerColor = player.isEliminated() ? Color.RED :
                        player == tournament.getCurrentPlayer() ? Color.YELLOW : Color.WHITE;
                g2d.setColor(playerColor);

                String status = player.isEliminated() ? " (OUT)" :
                        player == tournament.getWinner() ? " (WINNER)" : "";
                g2d.drawString(player.getName() + status, x, y + i * 20);
            }
        }

        private void drawPlayerStats(Graphics2D g2d) {
            Player currentPlayer = tournament.getCurrentPlayer();
            if (currentPlayer != null) {
                int x = DARTBOARD_X + DARTBOARD_SIZE + 50;
                int y = 300;

                g2d.setColor(Color.CYAN);
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                g2d.drawString("Player Stats:", x, y);

                y += 25;
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.drawString("Name: " + currentPlayer.getName(), x, y);
                y += 20;
                g2d.drawString("Score: " + currentPlayer.getCurrentScore(), x, y);
                y += 20;
                g2d.drawString("Throws: " + currentPlayer.getThrowCount(), x, y);
                y += 20;
                g2d.drawString("Average: " + String.format("%.1f", currentPlayer.getAverageScore()), x, y);
                y += 20;
                g2d.drawString("Skill Level: " + currentPlayer.getSkillLevel(), x, y);
            }
        }
    }

    private class DartboardRenderer {
        private final Color[] SEGMENT_COLORS = {Color.RED, Color.GREEN};
        private final Color[] RING_COLORS = {Color.BLACK, Color.WHITE};

        public void render(Graphics2D g2d, int x, int y, int size) {
            int centerX = x + size / 2;
            int centerY = y + size / 2;
            int radius = size / 2;

            // Draw outer circle (background)
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x, y, size, size);

            // Draw dartboard segments
            for (int i = 0; i < 20; i++) {
                double angle1 = (i * 18 - 9) * Math.PI / 180;
                double angle2 = ((i + 1) * 18 - 9) * Math.PI / 180;

                Color segmentColor = SEGMENT_COLORS[i % 2];
                drawSegment(g2d, centerX, centerY, radius, angle1, angle2, segmentColor);
            }

            // Draw rings
            drawRing(g2d, centerX, centerY, radius * 0.9, radius * 0.8, Color.GREEN); // Double ring
            drawRing(g2d, centerX, centerY, radius * 0.6, radius * 0.5, Color.RED);   // Triple ring

            // Draw bullseye
            g2d.setColor(Color.RED);
            g2d.fillOval(centerX - 15, centerY - 15, 30, 30);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(centerX - 8, centerY - 8, 16, 16);

            // Draw numbers
            drawNumbers(g2d, centerX, centerY, radius);

            // Draw wire frame
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x, y, size, size);
        }

        private void drawSegment(Graphics2D g2d, int centerX, int centerY, int radius,
                                 double angle1, double angle2, Color color) {
            g2d.setColor(color);

            int[] xPoints = new int[4];
            int[] yPoints = new int[4];

            xPoints[0] = centerX;
            yPoints[0] = centerY;
            xPoints[1] = centerX + (int)(radius * Math.cos(angle1));
            yPoints[1] = centerY + (int)(radius * Math.sin(angle1));
            xPoints[2] = centerX + (int)(radius * Math.cos(angle2));
            yPoints[2] = centerY + (int)(radius * Math.sin(angle2));
            xPoints[3] = centerX;
            yPoints[3] = centerY;

            g2d.fillPolygon(xPoints, yPoints, 4);
        }

        private void drawRing(Graphics2D g2d, int centerX, int centerY,
                              double outerRadius, double innerRadius, Color color) {
            g2d.setColor(color);
            Shape outerCircle = new Ellipse2D.Double(
                    centerX - outerRadius, centerY - outerRadius,
                    outerRadius * 2, outerRadius * 2);
            Shape innerCircle = new Ellipse2D.Double(
                    centerX - innerRadius, centerY - innerRadius,
                    innerRadius * 2, innerRadius * 2);

            Area ring = new Area(outerCircle);
            ring.subtract(new Area(innerCircle));
            g2d.fill(ring);
        }

        private void drawNumbers(Graphics2D g2d, int centerX, int centerY, int radius) {
            int[] numbers = {20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5};
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g2d.getFontMetrics();

            for (int i = 0; i < numbers.length; i++) {
                double angle = (i * 18) * Math.PI / 180;
                int numberRadius = (int)(radius * 1.1);
                int x = centerX + (int)(numberRadius * Math.cos(angle));
                int y = centerY + (int)(numberRadius * Math.sin(angle));

                String num = String.valueOf(numbers[i]);
                int textWidth = fm.stringWidth(num);
                int textHeight = fm.getHeight();

                g2d.drawString(num, x - textWidth / 2, y + textHeight / 4);
            }
        }
    }

    private class AnimationManager {
        private ConcurrentLinkedQueue<DartAnimation> animations;

        public AnimationManager() {
            animations = new ConcurrentLinkedQueue<>();
        }

        public void addDartAnimation(DartThrow dartThrow) {
            animations.offer(new DartAnimation(dartThrow));
        }

        public void update() {
            animations.removeIf(animation -> !animation.update());
        }

        public void render(Graphics2D g2d) {
            for (DartAnimation animation : animations) {
                animation.render(g2d);
            }
        }

        public void clear() {
            animations.clear();
        }
    }

    private class DartAnimation {
        private DartThrow dartThrow;
        private double animationTime;
        private double duration;
        private Point2D startPoint;
        private Point2D endPoint;
        private boolean completed;

        public DartAnimation(DartThrow dartThrow) {
            this.dartThrow = dartThrow;
            this.animationTime = 0;
            this.duration = 1000; // 1 second
            this.completed = false;

            // Animation from right side of screen to dart position
            startPoint = new Point2D.Double(WINDOW_WIDTH - 100, DARTBOARD_Y + DARTBOARD_SIZE / 2);
            endPoint = new Point2D.Double(
                    DARTBOARD_X + DARTBOARD_SIZE / 2 + dartThrow.getX() * DARTBOARD_SIZE / 400,
                    DARTBOARD_Y + DARTBOARD_SIZE / 2 + dartThrow.getY() * DARTBOARD_SIZE / 400
            );
        }

        public boolean update() {
            if (completed) return false;

            animationTime += 16; // 16ms per frame
            if (animationTime >= duration) {
                completed = true;
                return false;
            }
            return true;
        }

        public void render(Graphics2D g2d) {
            if (completed) return;

            double progress = animationTime / duration;
            double easedProgress = easeInOutCubic(progress);

            double currentX = startPoint.getX() + (endPoint.getX() - startPoint.getX()) * easedProgress;
            double currentY = startPoint.getY() + (endPoint.getY() - startPoint.getY()) * easedProgress;

            // Draw dart
            g2d.setColor(Color.YELLOW);
            g2d.fillOval((int)currentX - 3, (int)currentY - 3, 6, 6);

            // Draw dart trail
            g2d.setColor(new Color(255, 255, 0, 100));
            g2d.drawLine((int)startPoint.getX(), (int)startPoint.getY(),
                    (int)currentX, (int)currentY);

            // Draw final dart position when animation completes
            if (progress > 0.9) {
                g2d.setColor(Color.RED);
                g2d.fillOval((int)endPoint.getX() - 2, (int)endPoint.getY() - 2, 4, 4);
            }
        }

        private double easeInOutCubic(double t) {
            return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
        }
    }

    private class Tournament {
        private List<Player> players;
        private int currentPlayerIndex;
        private int currentRound;
        private int currentMatch;
        private int throwsPerPlayer;
        private boolean initialized;

        public Tournament() {
            players = new ArrayList<>();
            reset();
        }

        public void initialize() {
            players.clear();

            // Create players with different skill levels
            String[] names = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Henry"};
            SkillLevel[] skills = {SkillLevel.BEGINNER, SkillLevel.INTERMEDIATE, SkillLevel.ADVANCED,
                    SkillLevel.EXPERT, SkillLevel.BEGINNER, SkillLevel.INTERMEDIATE,
                    SkillLevel.ADVANCED, SkillLevel.EXPERT};

            for (int i = 0; i < names.length; i++) {
                players.add(new Player(names[i], skills[i]));
            }

            Collections.shuffle(players);
            initialized = true;
            currentPlayerIndex = 0;
            currentRound = 1;
            currentMatch = 1;
            throwsPerPlayer = 3;
        }

        public void processThrow(DartThrow dartThrow) {
            if (!initialized || players.isEmpty()) return;

            Player currentPlayer = getCurrentPlayer();
            if (currentPlayer != null) {
                currentPlayer.addThrow(dartThrow);

                if (currentPlayer.getThrowCount() >= throwsPerPlayer) {
                    advanceToNextPlayer();
                }
            }
        }

        private void advanceToNextPlayer() {
            currentPlayerIndex = (currentPlayerIndex + 1) % getActivePlayers().size();

            if (currentPlayerIndex == 0) {
                // End of round
                processRoundEnd();
            }
        }

        private void processRoundEnd() {
            List<Player> activePlayers = getActivePlayers();
            if (activePlayers.size() <= 1) return;

            // Eliminate lowest scoring player
            Player lowestScorer = activePlayers.stream()
                    .min(Comparator.comparingInt(Player::getCurrentScore))
                    .orElse(null);

            if (lowestScorer != null) {
                lowestScorer.eliminate();
            }

            // Reset throws for remaining players
            for (Player player : activePlayers) {
                if (!player.isEliminated()) {
                    player.resetForNewRound();
                }
            }

            currentRound++;
            currentMatch++;
        }

        public Player getCurrentPlayer() {
            List<Player> activePlayers = getActivePlayers();
            if (activePlayers.isEmpty() || currentPlayerIndex >= activePlayers.size()) {
                return null;
            }
            return activePlayers.get(currentPlayerIndex);
        }

        public List<Player> getActivePlayers() {
            return players.stream()
                    .filter(p -> !p.isEliminated())
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        public boolean isGameComplete() {
            return getActivePlayers().size() <= 1;
        }

        public Player getWinner() {
            List<Player> activePlayers = getActivePlayers();
            return activePlayers.size() == 1 ? activePlayers.get(0) : null;
        }

        public void reset() {
            players.clear();
            currentPlayerIndex = 0;
            currentRound = 0;
            currentMatch = 0;
            throwsPerPlayer = 3;
            initialized = false;
        }

        // Getters
        public List<Player> getPlayers() { return players; }
        public int getCurrentRound() { return currentRound; }
        public int getCurrentMatch() { return currentMatch; }
    }

    private class Player {
        private String name;
        private SkillLevel skillLevel;
        private List<DartThrow> throws;
        private boolean eliminated;
        private Random random;

        public Player(String name, SkillLevel skillLevel) {
            this.name = name;
            this.skillLevel = skillLevel;
            this.throws = new ArrayList<>();
            this.eliminated = false;
            this.random = new Random();
        }

        public DartThrow throwDart() {
            // Generate dart throw based on skill level
            double accuracy = skillLevel.getAccuracy();
            double powerVariation = skillLevel.getPowerVariation();

            // Target center of dartboard with some variation
            double targetX = 0;
            double targetY = 0;

            // Add skill-based variation
            double actualX = targetX + (random.nextGaussian() * powerVariation);
            double actualY = targetY + (random.nextGaussian() * powerVariation);

            // Calculate score based on position
            int score = calculateScore(actualX, actualY);

            return new DartThrow(actualX, actualY, score);
        }

        private int calculateScore(double x, double y) {
            double distance = Math.sqrt(x * x + y * y);

            // Bullseye
            if (distance <= 8) return 50;
            if (distance <= 15) return 25;

            // Outside dartboard
            if (distance > 200) return 0;

            // Calculate angle for segment
            double angle = Math.atan2(y, x) * 180 / Math.PI;
            if (angle < 0) angle += 360;

            // Dartboard segments (20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)
            int[] segments = {6, 13, 4, 18, 1, 20, 5, 12, 9, 14, 11, 8, 16, 7, 19, 3, 17, 2, 15, 10};
            int segmentIndex = (int)((angle + 9) / 18) % 20;
            int baseScore = segments[segmentIndex];

            // Check for double/triple rings
            if (distance > 160 && distance <= 170) return baseScore * 2; // Double ring
            if (distance > 100 && distance <= 110) return baseScore * 3; // Triple ring

            return baseScore;
        }

        public void addThrow(DartThrow dartThrow) {
            throws.add(dartThrow);
        }

        private void add(DartThrow dartThrow) {
        }

        public int getCurrentScore() {
            return throws.stream().mapToInt(DartThrow::getScore).sum();
        }

        public double getAverageScore() {
            if (throws.isEmpty()) return 0;
            return getCurrentScore() / (double) throws.size();
        }

        public void resetForNewRound() {
            throws.clear();
        }

        public void eliminate() {
            eliminated = true;
        }

        // Getters
        public String getName() { return name; }
        public SkillLevel getSkillLevel() { return skillLevel; }
        public int getThrowCount() { return throws.size(); }
        public boolean isEliminated() { return eliminated; }
    }

    private class DartThrow {
        private double x, y;
        private int score;

        public DartThrow(double x, double y, int score) {
            this.x = x;
            this.y = y;
            this.score = score;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public int getScore() { return score; }
    }

    private enum SkillLevel {
        BEGINNER(50.0, 80.0),
        INTERMEDIATE(35.0, 60.0),
        ADVANCED(25.0, 40.0),
        EXPERT(15.0, 25.0);

        private final double accuracy;
        private final double powerVariation;

        SkillLevel(double accuracy, double powerVariation) {
            this.accuracy = accuracy;
            this.powerVariation = powerVariation;
        }

        public double getAccuracy() { return accuracy; }
        public double getPowerVariation() { return powerVariation; }
    }

    private enum GameState {
        SETUP, IN_PROGRESS, FINISHED
    }

    private class SoundManager {
        public void playThrowSound() {
            // Placeholder for sound effects
            Toolkit.getDefaultToolkit().beep();
        }

        public void playWinSound() {
            // Placeholder for win sound
            for (int i = 0; i < 3; i++) {
                Timer timer = new Timer(i * 200, e -> Toolkit.getDefaultToolkit().beep());
                timer.setRepeats(false);
                timer.start();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }

            new DartsTournamentGame().setVisible(true);
        });
    }
}