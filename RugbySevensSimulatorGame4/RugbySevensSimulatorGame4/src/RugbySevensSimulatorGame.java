import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class RugbySevensSimulatorGame extends JFrame implements ActionListener, KeyListener {
    // Game constants
    private static final int FIELD_WIDTH = 1000;
    private static final int FIELD_HEIGHT = 600;
    private static final int PLAYER_SIZE = 20;
    private static final int BALL_SIZE = 12;
    private static final int GAME_TIME = 14; // 14 minutes (7 each half)

    // Game components
    private GamePanel gamePanel;
    private Timer gameTimer;
    private Timer animationTimer;
    private JLabel scoreLabel;
    private JLabel timeLabel;
    private JButton startButton;
    private JButton resetButton;
    private JButton pauseButton;

    // Game state
    private List<Player> team1Players;
    private List<Player> team2Players;
    private Ball ball;
    private int team1Score = 0;
    private int team2Score = 0;
    private int gameTimeRemaining = GAME_TIME * 60; // in seconds
    private boolean gameRunning = false;
    private boolean gamePaused = false;
    private boolean firstHalf = true;
    private Player ballCarrier = null;
    private Random random = new Random();

    // Animation variables
    private int animationFrame = 0;
    private List<AnimationEffect> effects = new ArrayList<>();

    public RugbySevensSimulatorGame() {
        initializeGame();
        setupUI();
        setupGameComponents();
    }

    private void initializeGame() {
        setTitle("Rugby Sevens Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        addKeyListener(this);
        setFocusable(true);
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // Create game panel
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        gamePanel.setBackground(new Color(34, 139, 34)); // Forest green
        add(gamePanel, BorderLayout.CENTER);

        // Create control panel
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(Color.DARK_GRAY);

        startButton = new JButton("Start Game");
        startButton.addActionListener(this);
        startButton.setBackground(Color.GREEN);
        startButton.setForeground(Color.WHITE);

        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(this);
        pauseButton.setBackground(Color.ORANGE);
        pauseButton.setForeground(Color.WHITE);
        pauseButton.setEnabled(false);

        resetButton = new JButton("Reset");
        resetButton.addActionListener(this);
        resetButton.setBackground(Color.RED);
        resetButton.setForeground(Color.WHITE);

        scoreLabel = new JLabel("Red Team: 0 - Blue Team: 0");
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 16));

        timeLabel = new JLabel("Time: 07:00");
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 16));

        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(resetButton);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(scoreLabel);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(timeLabel);

        add(controlPanel, BorderLayout.SOUTH);

        // Setup timers
        gameTimer = new Timer(1000, e -> updateGameTime());
        animationTimer = new Timer(50, e -> updateAnimation());

        pack();
        setLocationRelativeTo(null);
    }

    private void setupGameComponents() {
        // Initialize teams
        team1Players = new ArrayList<>();
        team2Players = new ArrayList<>();

        // Create Team 1 (Red) players
        String[] team1Positions = {"Scrum-half", "Fly-half", "Centre", "Wing", "Fullback", "Forward", "Forward"};
        for (int i = 0; i < 7; i++) {
            Player player = new Player(
                    "Red " + (i + 1),
                    team1Positions[i],
                    200 + i * 30,
                    250 + (i % 2) * 100,
                    Color.RED,
                    1
            );
            team1Players.add(player);
        }

        // Create Team 2 (Blue) players
        String[] team2Positions = {"Scrum-half", "Fly-half", "Centre", "Wing", "Fullback", "Forward", "Forward"};
        for (int i = 0; i < 7; i++) {
            Player player = new Player(
                    "Blue " + (i + 1),
                    team2Positions[i],
                    600 + i * 30,
                    250 + (i % 2) * 100,
                    Color.BLUE,
                    2
            );
            team2Players.add(player);
        }

        // Initialize ball
        ball = new Ball(FIELD_WIDTH / 2, FIELD_HEIGHT / 2);

        // Set initial ball carrier
        ballCarrier = team1Players.get(0);
        ball.setPosition(ballCarrier.getX(), ballCarrier.getY());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            if (!gameRunning) {
                startGame();
            }
        } else if (e.getSource() == pauseButton) {
            togglePause();
        } else if (e.getSource() == resetButton) {
            resetGame();
        }
    }

    private void startGame() {
        gameRunning = true;
        gamePaused = false;
        startButton.setEnabled(false);
        pauseButton.setEnabled(true);
        gameTimer.start();
        animationTimer.start();

        // Start game simulation
        simulateGameplay();
    }

    private void togglePause() {
        if (gamePaused) {
            gamePaused = false;
            pauseButton.setText("Pause");
            gameTimer.start();
            animationTimer.start();
        } else {
            gamePaused = true;
            pauseButton.setText("Resume");
            gameTimer.stop();
            animationTimer.stop();
        }
    }

    private void resetGame() {
        gameRunning = false;
        gamePaused = false;
        gameTimer.stop();
        animationTimer.stop();

        team1Score = 0;
        team2Score = 0;
        gameTimeRemaining = GAME_TIME * 60;
        firstHalf = true;
        ballCarrier = null;
        effects.clear();

        setupGameComponents();
        updateUI();

        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        pauseButton.setText("Pause");
    }

    private void updateGameTime() {
        if (gameRunning && !gamePaused) {
            gameTimeRemaining--;

            if (gameTimeRemaining == GAME_TIME * 30 && firstHalf) {
                // Half time
                firstHalf = false;
                addEffect(new AnimationEffect(FIELD_WIDTH / 2, FIELD_HEIGHT / 2, "HALF TIME", Color.YELLOW, 120));
                switchSides();
            }

            if (gameTimeRemaining <= 0) {
                endGame();
            }

            updateUI();
        }
    }

    private void updateAnimation() {
        if (gameRunning && !gamePaused) {
            animationFrame++;

            // Update player positions
            updatePlayerMovement();

            // Update ball position
            if (ballCarrier != null) {
                ball.setPosition(ballCarrier.getX() + 15, ballCarrier.getY() + 10);
            }

            // Update effects
            effects.removeIf(effect -> !effect.update());

            // Simulate gameplay events
            if (animationFrame % 60 == 0) { // Every 3 seconds
                simulateGameEvent();
            }

            gamePanel.repaint();
        }
    }

    private void updatePlayerMovement() {
        // Move players based on game situation
        for (Player player : team1Players) {
            movePlayerIntelligently(player);
        }

        for (Player player : team2Players) {
            movePlayerIntelligently(player);
        }
    }

    private void movePlayerIntelligently(Player player) {
        double speed = 2.0;

        if (player == ballCarrier) {
            // Ball carrier moves towards opponent's try line
            if (player.getTeam() == 1) {
                player.moveTowards(FIELD_WIDTH - 50, player.getY() + random.nextInt(40) - 20, speed);
            } else {
                player.moveTowards(50, player.getY() + random.nextInt(40) - 20, speed);
            }
        } else {
            // Other players move based on their role
            if (ballCarrier != null) {
                if (player.getTeam() == ballCarrier.getTeam()) {
                    // Support the ball carrier
                    double targetX = ballCarrier.getX() + random.nextInt(80) - 40;
                    double targetY = ballCarrier.getY() + random.nextInt(60) - 30;
                    player.moveTowards(targetX, targetY, speed * 0.8);
                } else {
                    // Defend - move towards ball carrier
                    player.moveTowards(ballCarrier.getX(), ballCarrier.getY(), speed * 1.2);
                }
            }
        }

        // Keep players on field
        player.constrainToField(FIELD_WIDTH, FIELD_HEIGHT);
    }

    private void simulateGameEvent() {
        if (ballCarrier == null) return;

        int eventType = random.nextInt(100);

        if (eventType < 20) {
            // Tackle attempt
            simulateTackle();
        } else if (eventType < 35) {
            // Pass attempt
            simulatePass();
        } else if (eventType < 45) {
            // Kick attempt
            simulateKick();
        } else if (eventType < 50) {
            // Try attempt
            attemptTry();
        }
    }

    private void simulateTackle() {
        List<Player> opponents = (ballCarrier.getTeam() == 1) ? team2Players : team1Players;

        for (Player defender : opponents) {
            double distance = ballCarrier.distanceTo(defender);
            if (distance < 40) {
                if (random.nextInt(100) < 60) { // 60% tackle success rate
                    addEffect(new AnimationEffect(ballCarrier.getX(), ballCarrier.getY(), "TACKLE!", Color.RED, 60));

                    // Ball becomes loose
                    ballCarrier = null;

                    // Randomly assign to nearby player
                    List<Player> allPlayers = new ArrayList<>();
                    allPlayers.addAll(team1Players);
                    allPlayers.addAll(team2Players);

                    Player nearestPlayer = null;
                    double minDistance = Double.MAX_VALUE;

                    for (Player player : allPlayers) {
                        double dist = ball.distanceTo(player.getX(), player.getY());
                        if (dist < minDistance) {
                            minDistance = dist;
                            nearestPlayer = player;
                        }
                    }

                    if (nearestPlayer != null) {
                        ballCarrier = nearestPlayer;
                    }
                    return;
                }
            }
        }
    }

    private void simulatePass() {
        List<Player> teammates = (ballCarrier.getTeam() == 1) ? team1Players : team2Players;

        // Find a teammate to pass to
        for (Player teammate : teammates) {
            if (teammate != ballCarrier && ballCarrier.distanceTo(teammate) < 100) {
                if (random.nextInt(100) < 70) { // 70% pass success rate
                    addEffect(new AnimationEffect(
                            (ballCarrier.getX() + teammate.getX()) / 2,
                            (ballCarrier.getY() + teammate.getY()) / 2,
                            "PASS", Color.YELLOW, 40));

                    ballCarrier = teammate;
                    return;
                }
            }
        }
    }

    private void simulateKick() {
        if (random.nextInt(100) < 30) { // 30% chance of kick
            addEffect(new AnimationEffect(ballCarrier.getX(), ballCarrier.getY(), "KICK!", Color.ORANGE, 60));

            // Move ball forward
            int kickDistance = 100 + random.nextInt(200);
            if (ballCarrier.getTeam() == 1) {
                ball.setPosition(Math.min(FIELD_WIDTH - 50, ball.getX() + kickDistance), ball.getY());
            } else {
                ball.setPosition(Math.max(50, ball.getX() - kickDistance), ball.getY());
            }

            ballCarrier = null;

            // Assign to nearest player
            assignBallToNearestPlayer();
        }
    }

    private void attemptTry() {
        if (ballCarrier.getTeam() == 1 && ballCarrier.getX() > FIELD_WIDTH - 80) {
            // Team 1 try
            team1Score += 5;
            addEffect(new AnimationEffect(FIELD_WIDTH - 50, ballCarrier.getY(), "TRY! +5", Color.GREEN, 120));
            resetToCenter();
        } else if (ballCarrier.getTeam() == 2 && ballCarrier.getX() < 80) {
            // Team 2 try
            team2Score += 5;
            addEffect(new AnimationEffect(50, ballCarrier.getY(), "TRY! +5", Color.GREEN, 120));
            resetToCenter();
        }
    }

    private void assignBallToNearestPlayer() {
        List<Player> allPlayers = new ArrayList<>();
        allPlayers.addAll(team1Players);
        allPlayers.addAll(team2Players);

        Player nearestPlayer = null;
        double minDistance = Double.MAX_VALUE;

        for (Player player : allPlayers) {
            double distance = ball.distanceTo(player.getX(), player.getY());
            if (distance < minDistance) {
                minDistance = distance;
                nearestPlayer = player;
            }
        }

        ballCarrier = nearestPlayer;
    }

    private void resetToCenter() {
        // Reset all players to starting positions
        for (int i = 0; i < team1Players.size(); i++) {
            Player player = team1Players.get(i);
            player.setPosition(200 + i * 30, 250 + (i % 2) * 100);
        }

        for (int i = 0; i < team2Players.size(); i++) {
            Player player = team2Players.get(i);
            player.setPosition(600 + i * 30, 250 + (i % 2) * 100);
        }

        ball.setPosition(FIELD_WIDTH / 2, FIELD_HEIGHT / 2);
        ballCarrier = team1Players.get(0);
    }

    private void switchSides() {
        // Switch team positions for second half
        for (Player player : team1Players) {
            player.setPosition(FIELD_WIDTH - player.getX(), player.getY());
        }

        for (Player player : team2Players) {
            player.setPosition(FIELD_WIDTH - player.getX(), player.getY());
        }
    }

    private void simulateGameplay() {
        // This method sets up the initial gameplay simulation
        ballCarrier = team1Players.get(0);
        ball.setPosition(ballCarrier.getX(), ballCarrier.getY());
    }

    private void endGame() {
        gameRunning = false;
        gameTimer.stop();
        animationTimer.stop();

        String winner = (team1Score > team2Score) ? "Red Team Wins!" :
                (team2Score > team1Score) ? "Blue Team Wins!" : "It's a Draw!";

        addEffect(new AnimationEffect(FIELD_WIDTH / 2, FIELD_HEIGHT / 2, winner, Color.YELLOW, 300));

        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
    }

    private void addEffect(AnimationEffect effect) {
        effects.add(effect);
    }

    private void updateUI() {
        scoreLabel.setText("Red Team: " + team1Score + " - Blue Team: " + team2Score);
        int minutes = gameTimeRemaining / 60;
        int seconds = gameTimeRemaining % 60;
        timeLabel.setText(String.format("Time: %02d:%02d", minutes, seconds));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Manual control for ball carrier (optional feature)
        if (ballCarrier != null && gameRunning && !gamePaused) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                    ballCarrier.move(0, -5);
                    break;
                case KeyEvent.VK_DOWN:
                    ballCarrier.move(0, 5);
                    break;
                case KeyEvent.VK_LEFT:
                    ballCarrier.move(-5, 0);
                    break;
                case KeyEvent.VK_RIGHT:
                    ballCarrier.move(5, 0);
                    break;
                case KeyEvent.VK_SPACE:
                    simulatePass();
                    break;
            }
            ballCarrier.constrainToField(FIELD_WIDTH, FIELD_HEIGHT);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    // Inner classes
    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw field markings
            drawField(g2d);

            // Draw players
            drawPlayers(g2d);

            // Draw ball
            drawBall(g2d);

            // Draw effects
            drawEffects(g2d);
        }

        private void drawField(Graphics2D g) {
            // Field boundaries
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3));
            g.drawRect(20, 20, FIELD_WIDTH - 40, FIELD_HEIGHT - 40);

            // Center line
            g.drawLine(FIELD_WIDTH / 2, 20, FIELD_WIDTH / 2, FIELD_HEIGHT - 20);

            // Try lines
            g.drawLine(70, 20, 70, FIELD_HEIGHT - 20);
            g.drawLine(FIELD_WIDTH - 70, 20, FIELD_WIDTH - 70, FIELD_HEIGHT - 20);

            // Goal posts
            g.setStroke(new BasicStroke(5));
            g.drawLine(20, FIELD_HEIGHT / 2 - 30, 20, FIELD_HEIGHT / 2 + 30);
            g.drawLine(FIELD_WIDTH - 20, FIELD_HEIGHT / 2 - 30, FIELD_WIDTH - 20, FIELD_HEIGHT / 2 + 30);
        }

        private void drawPlayers(Graphics2D g) {
            // Draw team 1 players (Red)
            for (Player player : team1Players) {
                drawPlayer(g, player);
            }

            // Draw team 2 players (Blue)
            for (Player player : team2Players) {
                drawPlayer(g, player);
            }
        }

        private void drawPlayer(Graphics2D g, Player player) {
            Color playerColor = player.getColor();

            // Highlight ball carrier
            if (player == ballCarrier) {
                g.setColor(Color.YELLOW);
                g.fillOval((int) player.getX() - 3, (int) player.getY() - 3, PLAYER_SIZE + 6, PLAYER_SIZE + 6);
            }

            // Draw player
            g.setColor(playerColor);
            g.fillOval((int) player.getX(), (int) player.getY(), PLAYER_SIZE, PLAYER_SIZE);

            // Draw player border
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2));
            g.drawOval((int) player.getX(), (int) player.getY(), PLAYER_SIZE, PLAYER_SIZE);

            // Draw player number
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            String number = String.valueOf(player.getName().charAt(player.getName().length() - 1));
            FontMetrics fm = g.getFontMetrics();
            int textX = (int) (player.getX() + PLAYER_SIZE / 2 - fm.stringWidth(number) / 2);
            int textY = (int) (player.getY() + PLAYER_SIZE / 2 + fm.getAscent() / 2);
            g.drawString(number, textX, textY);
        }

        private void drawBall(Graphics2D g) {
            g.setColor(Color.ORANGE);
            g.fillOval((int) ball.getX() - BALL_SIZE / 2, (int) ball.getY() - BALL_SIZE / 2, BALL_SIZE, BALL_SIZE);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2));
            g.drawOval((int) ball.getX() - BALL_SIZE / 2, (int) ball.getY() - BALL_SIZE / 2, BALL_SIZE, BALL_SIZE);
        }

        private void drawEffects(Graphics2D g) {
            for (AnimationEffect effect : effects) {
                effect.draw(g);
            }
        }
    }

    // Additional classes for game components
    private class Player {
        private String name;
        private String position;
        private double x, y;
        private Color color;
        private int team;

        public Player(String name, String position, double x, double y, Color color, int team) {
            this.name = name;
            this.position = position;
            this.x = x;
            this.y = y;
            this.color = color;
            this.team = team;
        }

        public void move(double dx, double dy) {
            this.x += dx;
            this.y += dy;
        }

        public void setPosition(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void moveTowards(double targetX, double targetY, double speed) {
            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > 0) {
                x += (dx / distance) * speed;
                y += (dy / distance) * speed;
            }
        }

        public double distanceTo(Player other) {
            return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2));
        }

        public void constrainToField(int fieldWidth, int fieldHeight) {
            x = Math.max(30, Math.min(fieldWidth - 30, x));
            y = Math.max(30, Math.min(fieldHeight - 30, y));
        }

        // Getters
        public String getName() { return name; }
        public String getPosition() { return position; }
        public double getX() { return x; }
        public double getY() { return y; }
        public Color getColor() { return color; }
        public int getTeam() { return team; }
    }

    private class Ball {
        private double x, y;

        public Ball(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void setPosition(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double distanceTo(double px, double py) {
            return Math.sqrt(Math.pow(x - px, 2) + Math.pow(y - py, 2));
        }

        public double getX() { return x; }
        public double getY() { return y; }
    }

    private class AnimationEffect {
        private double x, y;
        private String text;
        private Color color;
        private int duration;
        private int alpha = 255;

        public AnimationEffect(double x, double y, String text, Color color, int duration) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
            this.duration = duration;
        }

        public boolean update() {
            duration--;
            alpha = Math.max(0, (int) (255 * (duration / 120.0)));
            y -= 1; // Float upward
            return duration > 0;
        }

        public void draw(Graphics2D g) {
            Color fadeColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            g.setColor(fadeColor);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g.drawString(text, (int) (x - textWidth / 2), (int) y);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            new RugbySevensSimulatorGame().setVisible(true);
        });
    }
}