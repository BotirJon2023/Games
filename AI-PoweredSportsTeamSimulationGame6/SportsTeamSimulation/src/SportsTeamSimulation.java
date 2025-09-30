import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class SportsTeamSimulation extends JFrame implements ActionListener {

    // Game constants
    private static final int FIELD_WIDTH = 800;
    private static final int FIELD_HEIGHT = 600;
    private static final int PLAYER_SIZE = 20;
    private static final int BALL_SIZE = 15;
    private static final int GAME_SPEED = 50;

    // Game components
    private GamePanel gamePanel;
    private Timer gameTimer;
    private Timer animationTimer;
    private JPanel controlPanel;
    private JLabel scoreLabel;
    private JLabel timeLabel;
    private JButton startButton, pauseButton, resetButton;
    private JComboBox<Strategy> strategyCombo;

    // Game state
    private Team homeTeam;
    private Team awayTeam;
    private Ball ball;
    private GameState gameState;
    private int gameTime;
    private boolean isGameRunning;
    private String currentEvent;
    private List<String> gameLog;

    // AI Strategy patterns
    private enum Strategy {
        AGGRESSIVE("Aggressive - High pressure defense"),
        BALANCED("Balanced - Equal offense/defense"),
        DEFENSIVE("Defensive - Focus on ball control"),
        FAST_BREAK("Fast Break - Quick transitions");

        private final String description;
        Strategy(String description) { this.description = description; }
        @Override
        public String toString() { return description; }
    }

    public SportsTeamSimulation() {
        initializeGame();
        setupUI();
        startGameLoop();
    }

    private void initializeGame() {
        gameState = GameState.MENU;
        gameTime = 0;
        isGameRunning = false;
        currentEvent = "Game Ready";
        gameLog = new ArrayList<>();

        // Initialize teams
        homeTeam = new Team("Thunder Hawks", Color.BLUE, true);
        awayTeam = new Team("Fire Eagles", Color.RED, false);

        // Initialize ball
        ball = new Ball(FIELD_WIDTH / 2, FIELD_HEIGHT / 2);

        // Place players on field
        positionPlayersInitial();
    }

    private void setupUI() {
        setTitle("AI-Powered Basketball Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Game panel
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        gamePanel.setBackground(new Color(34, 139, 34));

        // Control panel
        controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBackground(Color.DARK_GRAY);

        startButton = new JButton("Start Game");
        pauseButton = new JButton("Pause");
        resetButton = new JButton("Reset");
        strategyCombo = new JComboBox<Strategy>(Strategy.values());

        startButton.addActionListener(this);
        pauseButton.addActionListener(this);
        resetButton.addActionListener(this);
        strategyCombo.addActionListener(this);

        scoreLabel = new JLabel("Home: 0 - Away: 0");
        timeLabel = new JLabel("Time: 0:00");

        scoreLabel.setForeground(Color.WHITE);
        timeLabel.setForeground(Color.WHITE);

        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(resetButton);
        controlPanel.add(new JLabel("Strategy:"));
        controlPanel.add(strategyCombo);
        controlPanel.add(scoreLabel);
        controlPanel.add(timeLabel);

        add(gamePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private void startGameLoop() {
        gameTimer = new Timer(GAME_SPEED, e -> updateGame());
        animationTimer = new Timer(16, e -> gamePanel.repaint()); // 60 FPS
        animationTimer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            if (gameState == GameState.MENU || gameState == GameState.PAUSED) {
                gameState = GameState.PLAYING;
                isGameRunning = true;
                gameTimer.start();
                currentEvent = "Game Started!";
                addToGameLog("Game has started!");
            }
        } else if (e.getSource() == pauseButton) {
            if (gameState == GameState.PLAYING) {
                gameState = GameState.PAUSED;
                isGameRunning = false;
                gameTimer.stop();
                currentEvent = "Game Paused";
            }
        } else if (e.getSource() == resetButton) {
            resetGame();
        } else if (e.getSource() == strategyCombo) {
            Strategy selected = (Strategy) strategyCombo.getSelectedItem();
            homeTeam.setStrategy(selected);
            currentEvent = "Strategy changed to: " + selected.name();
        }
    }

    private void resetGame() {
        gameTimer.stop();
        gameState = GameState.MENU;
        isGameRunning = false;
        gameTime = 0;
        homeTeam.resetScore();
        awayTeam.resetScore();
        ball.reset(FIELD_WIDTH / 2, FIELD_HEIGHT / 2);
        positionPlayersInitial();
        currentEvent = "Game Reset";
        gameLog.clear();
        updateLabels();
    }

    private void updateGame() {
        if (!isGameRunning) return;

        gameTime++;

        // Update AI behavior for all players
        updatePlayerAI();

        // Update ball physics
        ball.update();

        // Check for scoring
        checkScoring();

        // Check for game events
        processGameEvents();

        // Update UI
        updateLabels();

        // Check for game end (simplified - 300 game ticks = ~15 seconds for demo)
        if (gameTime >= 3000) {
            endGame();
        }
    }

    private void updatePlayerAI() {
        Strategy homeStrategy = homeTeam.getStrategy();

        // Update home team AI
        for (Player player : homeTeam.getPlayers()) {
            updatePlayerBehavior(player, homeStrategy, true);
        }

        // Update away team AI (always uses balanced strategy)
        for (Player player : awayTeam.getPlayers()) {
            updatePlayerBehavior(player, Strategy.BALANCED, false);
        }

        // Ball possession logic
        updateBallPossession();
    }

    private void updatePlayerBehavior(Player player, Strategy strategy, boolean isHomeTeam) {
        double dx = 0, dy = 0;

        // Calculate distance to ball
        double ballDistance = Math.sqrt(Math.pow(player.x - ball.x, 2) + Math.pow(player.y - ball.y, 2));

        // AI decision making based on strategy
        switch (strategy) {
            case AGGRESSIVE:
                // Players move aggressively toward ball
                if (ballDistance < 100) {
                    dx = (ball.x - player.x) * 0.05;
                    dy = (ball.y - player.y) * 0.05;
                } else {
                    // Move toward optimal position
                    Point target = calculateOptimalPosition(player, isHomeTeam);
                    dx = (target.x - player.x) * 0.03;
                    dy = (target.y - player.y) * 0.03;
                }
                break;

            case DEFENSIVE:
                // Stay closer to own goal
                int goalX = isHomeTeam ? 50 : FIELD_WIDTH - 50;
                if (ballDistance < 80) {
                    dx = (ball.x - player.x) * 0.04;
                    dy = (ball.y - player.y) * 0.04;
                } else {
                    dx = (goalX - player.x) * 0.02;
                    dy = (FIELD_HEIGHT / 2 - player.y) * 0.02;
                }
                break;

            case FAST_BREAK:
                // Quick movements and position changes
                if (ballDistance < 60) {
                    dx = (ball.x - player.x) * 0.06;
                    dy = (ball.y - player.y) * 0.06;
                } else {
                    // Random movement for unpredictability
                    dx = (Math.random() - 0.5) * 2;
                    dy = (Math.random() - 0.5) * 2;
                }
                break;

            case BALANCED:
            default:
                // Balanced approach - moderate aggression
                if (ballDistance < 70) {
                    dx = (ball.x - player.x) * 0.04;
                    dy = (ball.y - player.y) * 0.04;
                } else {
                    Point target = calculateOptimalPosition(player, isHomeTeam);
                    dx = (target.x - player.x) * 0.025;
                    dy = (target.y - player.y) * 0.025;
                }
                break;
        }

        // Apply movement with bounds checking
        player.move(dx, dy, FIELD_WIDTH, FIELD_HEIGHT);

        // Update player animation
        player.updateAnimation();
    }

    private Point calculateOptimalPosition(Player player, boolean isHomeTeam) {
        // Calculate optimal position based on player role and game situation
        int baseX = isHomeTeam ? FIELD_WIDTH / 4 : 3 * FIELD_WIDTH / 4;
        int baseY = FIELD_HEIGHT / 2;

        // Add some variation based on player index
        int variation = player.getId() * 80;
        baseY += (variation % FIELD_HEIGHT) - FIELD_HEIGHT / 2;

        return new Point(baseX, Math.max(PLAYER_SIZE, Math.min(FIELD_HEIGHT - PLAYER_SIZE, baseY)));
    }

    private void updateBallPossession() {
        Player closestPlayer = null;
        double minDistance = Double.MAX_VALUE;

        // Find closest player to ball
        for (Player player : homeTeam.getPlayers()) {
            double distance = Math.sqrt(Math.pow(player.x - ball.x, 2) + Math.pow(player.y - ball.y, 2));
            if (distance < minDistance) {
                minDistance = distance;
                closestPlayer = player;
            }
        }

        for (Player player : awayTeam.getPlayers()) {
            double distance = Math.sqrt(Math.pow(player.x - ball.x, 2) + Math.pow(player.y - ball.y, 2));
            if (distance < minDistance) {
                minDistance = distance;
                closestPlayer = player;
            }
        }

        // Ball possession and movement
        if (closestPlayer != null && minDistance < 25) {
            ball.setOwner(closestPlayer);

            // Move ball with player
            ball.x = closestPlayer.x;
            ball.y = closestPlayer.y;

            // Occasionally shoot toward goal
            if (Math.random() < 0.02) { // 2% chance per frame
                shootBall(closestPlayer);
            }
        } else {
            ball.setOwner(null);
        }
    }

    private void shootBall(Player shooter) {
        boolean isHomeTeam = homeTeam.getPlayers().contains(shooter);
        int targetX = isHomeTeam ? FIELD_WIDTH - 20 : 20;
        int targetY = FIELD_HEIGHT / 2 + (int)((Math.random() - 0.5) * 100);

        double dx = (targetX - ball.x) * 0.1;
        double dy = (targetY - ball.y) * 0.1;

        ball.shoot(dx, dy);

        String teamName = isHomeTeam ? homeTeam.getName() : awayTeam.getName();
        currentEvent = teamName + " shoots!";
        addToGameLog(shooter.getName() + " takes a shot!");
    }

    private void checkScoring() {
        // Check if ball is in goal area
        if (ball.x < 30 && ball.y > FIELD_HEIGHT/2 - 50 && ball.y < FIELD_HEIGHT/2 + 50) {
            // Away team scores
            awayTeam.addScore(1);
            currentEvent = awayTeam.getName() + " SCORES!";
            addToGameLog(awayTeam.getName() + " scores a goal!");
            resetBallPosition();
        } else if (ball.x > FIELD_WIDTH - 30 && ball.y > FIELD_HEIGHT/2 - 50 && ball.y < FIELD_HEIGHT/2 + 50) {
            // Home team scores
            homeTeam.addScore(1);
            currentEvent = homeTeam.getName() + " SCORES!";
            addToGameLog(homeTeam.getName() + " scores a goal!");
            resetBallPosition();
        }
    }

    private void resetBallPosition() {
        ball.reset(FIELD_WIDTH / 2, FIELD_HEIGHT / 2);
        // Brief pause after scoring
        Timer pauseTimer = new Timer(1000, e -> {
            currentEvent = "Game continues...";
            ((Timer)e.getSource()).stop();
        });
        pauseTimer.start();
    }

    private void processGameEvents() {
        // Random game events
        if (Math.random() < 0.001) { // 0.1% chance per frame
            String[] events = {
                    "Great defensive play!",
                    "Nice ball movement!",
                    "Intense pressure!",
                    "Quick transition!",
                    "Defensive stop!"
            };
            currentEvent = events[(int)(Math.random() * events.length)];
        }
    }

    private void updateLabels() {
        scoreLabel.setText(String.format("Home: %d - Away: %d",
                homeTeam.getScore(), awayTeam.getScore()));

        int minutes = gameTime / 1200; // Rough conversion to minutes
        int seconds = (gameTime / 20) % 60; // Rough conversion to seconds
        timeLabel.setText(String.format("Time: %d:%02d", minutes, seconds));
    }

    private void endGame() {
        gameTimer.stop();
        isGameRunning = false;
        gameState = GameState.FINISHED;

        String winner = homeTeam.getScore() > awayTeam.getScore() ?
                homeTeam.getName() :
                (awayTeam.getScore() > homeTeam.getScore() ?
                        awayTeam.getName() : "TIE");

        currentEvent = "GAME OVER! Winner: " + winner;
        addToGameLog("Game finished! Final score: " +
                homeTeam.getScore() + "-" + awayTeam.getScore());

        JOptionPane.showMessageDialog(this,
                "Game Over!\n" + winner + " wins!\nFinal Score: " +
                        homeTeam.getScore() + " - " + awayTeam.getScore(),
                "Game Over", JOptionPane.INFORMATION_MESSAGE);
    }

    private void positionPlayersInitial() {
        // Position home team players
        List<Player> homePlayers = homeTeam.getPlayers();
        for (int i = 0; i < homePlayers.size(); i++) {
            homePlayers.get(i).setPosition(
                    100 + i * 40,
                    150 + i * 80
            );
        }

        // Position away team players
        List<Player> awayPlayers = awayTeam.getPlayers();
        for (int i = 0; i < awayPlayers.size(); i++) {
            awayPlayers.get(i).setPosition(
                    FIELD_WIDTH - 140 - i * 40,
                    150 + i * 80
            );
        }
    }

    private void addToGameLog(String event) {
        gameLog.add(String.format("[%d:%02d] %s",
                gameTime / 1200, (gameTime / 20) % 60, event));
        if (gameLog.size() > 50) {
            gameLog.remove(0); // Keep only last 50 events
        }
    }

    // Inner classes
    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawField(g2d);
            drawPlayers(g2d);
            drawBall(g2d);
            drawUI(g2d);
        }

        private void drawField(Graphics2D g) {
            // Draw field boundaries
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3));
            g.drawRect(10, 10, FIELD_WIDTH - 20, FIELD_HEIGHT - 20);

            // Draw center line
            g.drawLine(FIELD_WIDTH / 2, 10, FIELD_WIDTH / 2, FIELD_HEIGHT - 10);

            // Draw center circle
            g.drawOval(FIELD_WIDTH / 2 - 50, FIELD_HEIGHT / 2 - 50, 100, 100);

            // Draw goals
            g.setColor(Color.YELLOW);
            g.fillRect(10, FIELD_HEIGHT / 2 - 50, 20, 100);
            g.fillRect(FIELD_WIDTH - 30, FIELD_HEIGHT / 2 - 50, 20, 100);
        }

        private void drawPlayers(Graphics2D g) {
            // Draw home team
            for (Player player : homeTeam.getPlayers()) {
                player.draw(g);
            }

            // Draw away team
            for (Player player : awayTeam.getPlayers()) {
                player.draw(g);
            }
        }

        private void drawBall(Graphics2D g) {
            ball.draw(g);
        }

        private void drawUI(Graphics2D g) {
            // Draw current event
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString(currentEvent, 20, 30);

            // Draw team names and strategies
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            g.setColor(homeTeam.getColor());
            g.drawString(homeTeam.getName() + " (" + homeTeam.getStrategy() + ")", 20, FIELD_HEIGHT - 20);

            g.setColor(awayTeam.getColor());
            g.drawString(awayTeam.getName(), FIELD_WIDTH - 150, FIELD_HEIGHT - 20);
        }
    }

    private class Team {
        private String name;
        private Color color;
        private List<Player> players;
        private int score;
        private Strategy strategy;
        private boolean isHome;

        public Team(String name, Color color, boolean isHome) {
            this.name = name;
            this.color = color;
            this.isHome = isHome;
            this.score = 0;
            this.strategy = Strategy.BALANCED;
            this.players = new ArrayList<>();

            // Create 5 players per team
            for (int i = 0; i < 5; i++) {
                players.add(new Player(i, "Player " + (i + 1), color, isHome));
            }
        }

        // Getters and setters
        public String getName() { return name; }
        public Color getColor() { return color; }
        public List<Player> getPlayers() { return players; }
        public int getScore() { return score; }
        public Strategy getStrategy() { return strategy; }
        public void setStrategy(Strategy strategy) { this.strategy = strategy; }
        public void addScore(int points) { this.score += points; }
        public void resetScore() { this.score = 0; }
    }

    private class Player {
        private int id;
        private String name;
        private double x, y;
        private Color color;
        private boolean isHome;
        private double animationPhase;
        private double energy;

        public Player(int id, String name, Color color, boolean isHome) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.isHome = isHome;
            this.animationPhase = Math.random() * Math.PI * 2;
            this.energy = 1.0;
        }

        public void setPosition(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void move(double dx, double dy, int maxX, int maxY) {
            x += dx;
            y += dy;

            // Keep within bounds
            x = Math.max(PLAYER_SIZE, Math.min(maxX - PLAYER_SIZE, x));
            y = Math.max(PLAYER_SIZE, Math.min(maxY - PLAYER_SIZE, y));

            // Reduce energy slightly with movement
            energy = Math.max(0.3, energy - 0.0001);
        }

        public void updateAnimation() {
            animationPhase += 0.2;
            if (animationPhase > Math.PI * 2) {
                animationPhase -= Math.PI * 2;
            }
        }

        public void draw(Graphics2D g) {
            // Player body with animation
            int animOffset = (int)(Math.sin(animationPhase) * 3);

            g.setColor(color);
            g.fillOval((int)x - PLAYER_SIZE/2, (int)y - PLAYER_SIZE/2 + animOffset,
                    PLAYER_SIZE, PLAYER_SIZE);

            // Player outline
            g.setColor(Color.BLACK);
            g.drawOval((int)x - PLAYER_SIZE/2, (int)y - PLAYER_SIZE/2 + animOffset,
                    PLAYER_SIZE, PLAYER_SIZE);

            // Player number
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.drawString(String.valueOf(id + 1), (int)x - 3, (int)y + 3 + animOffset);
        }

        public int getId() { return id; }
        public String getName() { return name; }
    }

    private class Ball {
        private double x, y;
        private double vx, vy; // Velocity
        private Player owner;
        private Color color;
        private double spin;

        public Ball(double x, double y) {
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = 0;
            this.color = Color.ORANGE;
            this.spin = 0;
        }

        public void update() {
            if (owner == null) {
                // Apply physics when ball is free
                x += vx;
                y += vy;

                // Friction
                vx *= 0.98;
                vy *= 0.98;

                // Boundary collisions
                if (x < BALL_SIZE || x > FIELD_WIDTH - BALL_SIZE) {
                    vx = -vx * 0.8;
                    x = Math.max(BALL_SIZE, Math.min(FIELD_WIDTH - BALL_SIZE, x));
                }
                if (y < BALL_SIZE || y > FIELD_HEIGHT - BALL_SIZE) {
                    vy = -vy * 0.8;
                    y = Math.max(BALL_SIZE, Math.min(FIELD_HEIGHT - BALL_SIZE, y));
                }

                spin += 0.3;
            }
        }

        public void shoot(double dx, double dy) {
            owner = null;
            vx = dx;
            vy = dy;
        }

        public void reset(double newX, double newY) {
            x = newX;
            y = newY;
            vx = 0;
            vy = 0;
            owner = null;
        }

        public void draw(Graphics2D g) {
            // Ball shadow
            g.setColor(new Color(0, 0, 0, 50));
            g.fillOval((int)x - BALL_SIZE/2 + 2, (int)y - BALL_SIZE/2 + 2,
                    BALL_SIZE, BALL_SIZE);

            // Ball
            g.setColor(color);
            g.fillOval((int)x - BALL_SIZE/2, (int)y - BALL_SIZE/2, BALL_SIZE, BALL_SIZE);

            // Ball lines for spinning effect
            g.setColor(Color.BLACK);
            g.drawOval((int)x - BALL_SIZE/2, (int)y - BALL_SIZE/2, BALL_SIZE, BALL_SIZE);

            double lineAngle = spin;
            int lineX1 = (int)(x + Math.cos(lineAngle) * BALL_SIZE/3);
            int lineY1 = (int)(y + Math.sin(lineAngle) * BALL_SIZE/3);
            int lineX2 = (int)(x - Math.cos(lineAngle) * BALL_SIZE/3);
            int lineY2 = (int)(y - Math.sin(lineAngle) * BALL_SIZE/3);

            g.drawLine(lineX1, lineY1, lineX2, lineY2);
        }

        public void setOwner(Player owner) { this.owner = owner; }
        public Player getOwner() { return owner; }
    }

    private enum GameState {
        MENU, PLAYING, PAUSED, FINISHED
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            new SportsTeamSimulation().setVisible(true);
        });
    }
}