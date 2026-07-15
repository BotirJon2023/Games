import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import java.awt.image.BufferedImage;

public class FIFA2026Game extends JFrame {
    private GamePanel gamePanel;
    private JPanel controlPanel, statsPanel;
    private JButton startButton, pauseButton, resetButton;
    private JButton playerModeButton, computerModeButton;
    private JButton easyButton, mediumButton, hardButton;
    private JLabel scoreLabel, timeLabel, statsLabel;
    private Timer gameTimer;
    private boolean isPaused = false;
    private boolean isComputerMode = false;
    private int gameTime = 0;
    private String difficulty = "Medium";

    public FIFA2026Game() {
        setTitle("FIFA 2026 - Ultimate Soccer Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setBackground(new Color(20, 20, 30));

        // Initialize game panel
        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        // Initialize control panel
        controlPanel = new JPanel();
        controlPanel.setBackground(new Color(30, 30, 45));
        controlPanel.setPreferredSize(new Dimension(900, 100));
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        // Create styled buttons
        startButton = createStyledButton("▶ Start", new Color(46, 204, 113));
        pauseButton = createStyledButton("⏸ Pause", new Color(241, 196, 15));
        resetButton = createStyledButton("⟳ Reset", new Color(231, 76, 60));
        playerModeButton = createStyledButton("👥 2 Players", new Color(52, 152, 219));
        computerModeButton = createStyledButton("🤖 vs Computer", new Color(155, 89, 182));
        easyButton = createStyledButton("Easy", new Color(46, 204, 113));
        mediumButton = createStyledButton("Medium", new Color(241, 196, 15));
        hardButton = createStyledButton("Hard", new Color(231, 76, 60));

        // Stats panel
        statsPanel = new JPanel();
        statsPanel.setBackground(new Color(30, 30, 45));
        statsPanel.setPreferredSize(new Dimension(200, 100));
        statsPanel.setLayout(new GridLayout(3, 1, 5, 5));

        scoreLabel = new JLabel("⚽ 0 - 0");
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 20));
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);

        timeLabel = new JLabel("⏱ 00:00");
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        timeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        statsLabel = new JLabel("Shots: 0-0");
        statsLabel.setForeground(new Color(200, 200, 200));
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        statsLabel.setHorizontalAlignment(SwingConstants.CENTER);

        statsPanel.add(scoreLabel);
        statsPanel.add(timeLabel);
        statsPanel.add(statsLabel);

        // Add components to control panel
        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(resetButton);
        controlPanel.add(playerModeButton);
        controlPanel.add(computerModeButton);
        controlPanel.add(easyButton);
        controlPanel.add(mediumButton);
        controlPanel.add(hardButton);
        controlPanel.add(statsPanel);

        add(controlPanel, BorderLayout.SOUTH);

        // Add action listeners
        startButton.addActionListener(e -> startGame());
        pauseButton.addActionListener(e -> togglePause());
        resetButton.addActionListener(e -> resetGame());
        playerModeButton.addActionListener(e -> setPlayerMode(false));
        computerModeButton.addActionListener(e -> setPlayerMode(true));
        easyButton.addActionListener(e -> setDifficulty("Easy"));
        mediumButton.addActionListener(e -> setDifficulty("Medium"));
        hardButton.addActionListener(e -> setDifficulty("Hard"));

        // Keyboard controls
        setupKeyBindings();

        // Initialize game timer
        gameTimer = new Timer(16, e -> {
            if (!isPaused) {
                gamePanel.update();
                updateTime();
                updateScore();
                updateStats();
                repaint();
            }
        });

        // Set window properties
        setSize(1000, 750);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 2),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });
        return button;
    }

    private void setupKeyBindings() {
        InputMap inputMap = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = gamePanel.getActionMap();

        // Player 1 controls (WASD + Q for sprint, E for kick)
        inputMap.put(KeyStroke.getKeyStroke("W"), "player1Up");
        inputMap.put(KeyStroke.getKeyStroke("S"), "player1Down");
        inputMap.put(KeyStroke.getKeyStroke("A"), "player1Left");
        inputMap.put(KeyStroke.getKeyStroke("D"), "player1Right");
        inputMap.put(KeyStroke.getKeyStroke("Q"), "player1Sprint");
        inputMap.put(KeyStroke.getKeyStroke("E"), "player1Kick");
        inputMap.put(KeyStroke.getKeyStroke("R"), "player1Pass");

        actionMap.put("player1Up", new KeyAction(1, 0, -1));
        actionMap.put("player1Down", new KeyAction(1, 0, 1));
        actionMap.put("player1Left", new KeyAction(1, -1, 0));
        actionMap.put("player1Right", new KeyAction(1, 1, 0));
        actionMap.put("player1Sprint", new KeyAction(1, 0, 0, false, true));
        actionMap.put("player1Kick", new KeyAction(1, 0, 0, true, false));
        actionMap.put("player1Pass", new KeyAction(1, 0, 0, false, false));

        // Player 2 controls (Arrow keys + NumPad)
        inputMap.put(KeyStroke.getKeyStroke("UP"), "player2Up");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "player2Down");
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "player2Left");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "player2Right");
        inputMap.put(KeyStroke.getKeyStroke("NUMPAD1"), "player2Sprint");
        inputMap.put(KeyStroke.getKeyStroke("NUMPAD2"), "player2Kick");
        inputMap.put(KeyStroke.getKeyStroke("NUMPAD3"), "player2Pass");

        actionMap.put("player2Up", new KeyAction(2, 0, -1));
        actionMap.put("player2Down", new KeyAction(2, 0, 1));
        actionMap.put("player2Left", new KeyAction(2, -1, 0));
        actionMap.put("player2Right", new KeyAction(2, 1, 0));
        actionMap.put("player2Sprint", new KeyAction(2, 0, 0, false, true));
        actionMap.put("player2Kick", new KeyAction(2, 0, 0, true, false));
        actionMap.put("player2Pass", new KeyAction(2, 0, 0, false, false));

        // Release keys
        String[] keys = {"W", "S", "A", "D", "UP", "DOWN", "LEFT", "RIGHT"};
        for (String key : keys) {
            inputMap.put(KeyStroke.getKeyStroke("released " + key), "release_" + key);
            actionMap.put("release_" + key, new KeyAction(0, 0, 0));
        }
    }

    private class KeyAction extends AbstractAction {
        private int player;
        private int dx, dy;
        private boolean kick, sprint;

        public KeyAction(int player, int dx, int dy) {
            this(player, dx, dy, false, false);
        }

        public KeyAction(int player, int dx, int dy, boolean kick, boolean sprint) {
            this.player = player;
            this.dx = dx;
            this.dy = dy;
            this.kick = kick;
            this.sprint = sprint;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (player == 1) {
                if (sprint) gamePanel.setPlayer1Sprint(true);
                else if (kick) gamePanel.player1Kick();
                else if (e.getActionCommand().startsWith("release")) {
                    gamePanel.setPlayer1Direction(0, 0);
                    gamePanel.setPlayer1Sprint(false);
                } else {
                    gamePanel.setPlayer1Direction(dx, dy);
                }
            } else if (player == 2) {
                if (sprint) gamePanel.setPlayer2Sprint(true);
                else if (kick) gamePanel.player2Kick();
                else if (e.getActionCommand().startsWith("release")) {
                    gamePanel.setPlayer2Direction(0, 0);
                    gamePanel.setPlayer2Sprint(false);
                } else {
                    gamePanel.setPlayer2Direction(dx, dy);
                }
            }
        }
    }

    private void startGame() {
        if (!gameTimer.isRunning()) {
            gameTimer.start();
            gamePanel.setGameRunning(true);
            startButton.setEnabled(false);
            pauseButton.setEnabled(true);
            startButton.setText("▶ Playing...");
        }
    }

    private void togglePause() {
        isPaused = !isPaused;
        pauseButton.setText(isPaused ? "▶ Resume" : "⏸ Pause");
        gamePanel.setPaused(isPaused);
    }

    private void resetGame() {
        gameTimer.stop();
        gamePanel.reset();
        gameTime = 0;
        timeLabel.setText("⏱ 00:00");
        scoreLabel.setText("⚽ 0 - 0");
        statsLabel.setText("Shots: 0-0");
        isPaused = false;
        pauseButton.setText("⏸ Pause");
        startButton.setEnabled(true);
        startButton.setText("▶ Start");
        pauseButton.setEnabled(false);
        gamePanel.setGameRunning(false);
        repaint();
    }

    private void setPlayerMode(boolean computer) {
        isComputerMode = computer;
        gamePanel.setComputerMode(computer);
        playerModeButton.setBackground(computer ? new Color(52, 152, 219) : new Color(80, 80, 80));
        computerModeButton.setBackground(computer ? new Color(80, 80, 80) : new Color(155, 89, 182));
        resetGame();
    }

    private void setDifficulty(String diff) {
        this.difficulty = diff;
        gamePanel.setDifficulty(diff);
        easyButton.setBackground(diff.equals("Easy") ? new Color(46, 204, 113) : new Color(80, 80, 80));
        mediumButton.setBackground(diff.equals("Medium") ? new Color(241, 196, 15) : new Color(80, 80, 80));
        hardButton.setBackground(diff.equals("Hard") ? new Color(231, 76, 60) : new Color(80, 80, 80));
    }

    private void updateTime() {
        gameTime++;
        int seconds = gameTime / 60;
        int minutes = seconds / 60;
        seconds %= 60;
        timeLabel.setText(String.format("⏱ %02d:%02d", minutes, seconds));
    }

    private void updateScore() {
        scoreLabel.setText(String.format("⚽ %d - %d",
                gamePanel.getPlayer1Score(), gamePanel.getPlayer2Score()));
    }

    private void updateStats() {
        statsLabel.setText(String.format("Shots: %d-%d | Possession: %d%%-%d%%",
                gamePanel.getPlayer1Shots(), gamePanel.getPlayer2Shots(),
                gamePanel.getPlayer1Possession(), gamePanel.getPlayer2Possession()));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FIFA2026Game());
    }
}

class GamePanel extends JPanel {
    private static final int WIDTH = 900;
    private static final int HEIGHT = 650;
    private static final int PLAYER_SIZE = 35;
    private static final int BALL_SIZE = 18;
    private static final int GOAL_WIDTH = 140;
    private static final int GOAL_HEIGHT = 25;

    private Player player1, player2;
    private Ball ball;
    private Goal goal1, goal2;
    private List<Particle> particles;
    private List<Star> stars;
    private List<Confetti> confetti;
    private boolean gameRunning = false;
    private boolean isPaused = false;
    private boolean isComputerMode = false;
    private String difficulty = "Medium";
    private int player1Score = 0, player2Score = 0;
    private int player1Shots = 0, player2Shots = 0;
    private int player1Possession = 50, player2Possession = 50;
    private int goalCooldown = 0;
    private int possessionTimer = 0;
    private String possessionHolder = "";

    // Animation effects
    private float fieldGradient = 0.5f;
    private boolean gradientDirection = true;
    private int goalFlashTimer = 0;
    private String goalMessage = "";
    private int goalMessageTimer = 0;
    private int celebrationTimer = 0;
    private double stadiumZoom = 1.0;

    // Player names
    private String player1Name = "⚡ FC Barcelona";
    private String player2Name = "🌟 Real Madrid";

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(20, 30, 20));
        setFocusable(true);

        particles = new ArrayList<>();
        stars = new ArrayList<>();
        confetti = new ArrayList<>();

        // Initialize stars
        for (int i = 0; i < 80; i++) {
            stars.add(new Star());
        }

        reset();
    }

    public void reset() {
        player1 = new Player(120, HEIGHT/2 - PLAYER_SIZE/2, PLAYER_SIZE,
                new Color(0, 100, 200), player1Name);
        player2 = new Player(WIDTH - 120 - PLAYER_SIZE, HEIGHT/2 - PLAYER_SIZE/2,
                PLAYER_SIZE, new Color(200, 50, 50), player2Name);
        ball = new Ball(WIDTH/2 - BALL_SIZE/2, HEIGHT/2 - BALL_SIZE/2, BALL_SIZE);

        goal1 = new Goal(0, HEIGHT/2 - GOAL_WIDTH/2, GOAL_HEIGHT, GOAL_WIDTH,
                new Color(0, 100, 200, 100));
        goal2 = new Goal(WIDTH - GOAL_HEIGHT, HEIGHT/2 - GOAL_WIDTH/2,
                GOAL_HEIGHT, GOAL_WIDTH, new Color(200, 50, 50, 100));

        particles.clear();
        confetti.clear();
        goalCooldown = 0;
        goalFlashTimer = 0;
        goalMessage = "";
        goalMessageTimer = 0;
        celebrationTimer = 0;
        player1Shots = 0;
        player2Shots = 0;
        player1Possession = 50;
        player2Possession = 50;
        possessionTimer = 0;

        repaint();
    }

    public void update() {
        if (isPaused || !gameRunning) return;

        // Update field animation
        if (gradientDirection) {
            fieldGradient += 0.002f;
            if (fieldGradient >= 0.8f) gradientDirection = false;
        } else {
            fieldGradient -= 0.002f;
            if (fieldGradient <= 0.2f) gradientDirection = true;
        }

        // Update stadium zoom
        stadiumZoom = 1.0 + Math.sin(System.currentTimeMillis() / 10000.0) * 0.01;

        // Update players
        player1.update();
        player2.update();

        // Computer AI
        if (isComputerMode) {
            updateComputerAI();
        }

        // Update ball
        ball.update();

        // Collision detection
        checkPlayerBallCollision(player1);
        checkPlayerBallCollision(player2);
        checkBallWallCollision();
        checkGoalScored();

        // Update possession
        updatePossession();

        // Update particles
        Iterator<Particle> iter = particles.iterator();
        while (iter.hasNext()) {
            Particle p = iter.next();
            p.update();
            if (p.isDead()) {
                iter.remove();
            }
        }

        // Update confetti
        Iterator<Confetti> cit = confetti.iterator();
        while (cit.hasNext()) {
            Confetti c = cit.next();
            c.update();
            if (c.isDead()) {
                cit.remove();
            }
        }

        // Update stars
        for (Star star : stars) {
            star.update();
        }

        if (goalCooldown > 0) goalCooldown--;
        if (goalMessageTimer > 0) goalMessageTimer--;
        if (goalFlashTimer > 0) goalFlashTimer--;
        if (celebrationTimer > 0) celebrationTimer--;
    }

    private void updateComputerAI() {
        double dx = ball.getX() - player2.getX();
        double dy = ball.getY() - player2.getY();
        double distance = Math.sqrt(dx*dx + dy*dy);

        // Different AI behavior based on difficulty
        double speedMultiplier = 1.0;
        double reactionDistance = 50;
        double kickPower = 1.0;

        switch(difficulty) {
            case "Easy":
                speedMultiplier = 0.6;
                reactionDistance = 80;
                kickPower = 0.7;
                break;
            case "Hard":
                speedMultiplier = 1.3;
                reactionDistance = 30;
                kickPower = 1.3;
                break;
            default: // Medium
                speedMultiplier = 1.0;
                reactionDistance = 50;
                kickPower = 1.0;
        }

        if (distance > reactionDistance) {
            // Move towards ball with some randomness
            double speed = Math.min(4.0 * speedMultiplier, distance / 15);
            double angle = Math.atan2(dy, dx) + (Math.random() - 0.5) * 0.2;
            player2.setDirection((int)(Math.cos(angle) * speed),
                    (int)(Math.sin(angle) * speed));
        } else if (distance < 40) {
            // Kick ball towards goal with precision
            double goalX = WIDTH - 30;
            double goalY = HEIGHT/2 + (Math.random() - 0.5) * 60;
            double kickDx = goalX - ball.getX();
            double kickDy = goalY - ball.getY();
            double kickDist = Math.sqrt(kickDx*kickDx + kickDy*kickDy);
            if (kickDist > 0) {
                double power = 6.0 * kickPower;
                ball.setVelocity(kickDx/kickDist * power,
                        kickDy/kickDist * power);
                createKickParticles(ball.getX() + BALL_SIZE/2,
                        ball.getY() + BALL_SIZE/2);
                player2Shots++;
            }
            player2.setDirection(0, 0);
        } else {
            // Intelligent positioning
            double goalX = WIDTH - 30;
            double goalY = HEIGHT/2;
            double targetX = (ball.getX() + goalX) / 2;
            double targetY = (ball.getY() + goalY) / 2 + (Math.random() - 0.5) * 20;
            dx = targetX - player2.getX();
            dy = targetY - player2.getY();
            double dist = Math.sqrt(dx*dx + dy*dy);
            if (dist > 10) {
                double speed = Math.min(3.0 * speedMultiplier, dist / 20);
                player2.setDirection((int)(dx/dist * speed),
                        (int)(dy/dist * speed));
            } else {
                player2.setDirection(0, 0);
            }
        }

        // Dribbling
        if (distance < 50 && distance > 20) {
            double dribbleStrength = 0.03 * speedMultiplier;
            ball.setVelocity((ball.getX() - player2.getX()) * dribbleStrength,
                    (ball.getY() - player2.getY()) * dribbleStrength);
        }
    }

    private void checkPlayerBallCollision(Player player) {
        if (ball.intersects(player)) {
            double dx = ball.getX() - player.getX();
            double dy = ball.getY() - player.getY();
            double dist = Math.sqrt(dx*dx + dy*dy);

            if (dist > 0) {
                double overlap = (player.getSize()/2 + BALL_SIZE/2) - dist;
                double angle = Math.atan2(dy, dx);

                ball.setX(ball.getX() + Math.cos(angle) * overlap);
                ball.setY(ball.getY() + Math.sin(angle) * overlap);

                double speed = Math.sqrt(player.getVx()*player.getVx() +
                        player.getVy()*player.getVy());
                if (speed > 0.5) {
                    double transfer = player.isSprinting() ? 0.5 : 0.3;
                    ball.setVelocity(ball.getVx() + player.getVx() * transfer,
                            ball.getVy() + player.getVy() * transfer);
                }

                // Update possession
                possessionHolder = player.getName();
            }
        }
    }

    private void checkBallWallCollision() {
        // Wall collisions with bounce
        if (ball.getX() < 0) {
            ball.setX(0);
            ball.setVx(-ball.getVx() * 0.7);
        }
        if (ball.getX() + BALL_SIZE > WIDTH) {
            ball.setX(WIDTH - BALL_SIZE);
            ball.setVx(-ball.getVx() * 0.7);
        }
        if (ball.getY() < 0) {
            ball.setY(0);
            ball.setVy(-ball.getVy() * 0.7);
        }
        if (ball.getY() + BALL_SIZE > HEIGHT) {
            ball.setY(HEIGHT - BALL_SIZE);
            ball.setVy(-ball.getVy() * 0.7);
        }
    }

    private void checkGoalScored() {
        if (goalCooldown > 0) return;

        // Goal 1 (left)
        if (ball.getX() + BALL_SIZE > 0 && ball.getX() < GOAL_HEIGHT) {
            if (ball.getY() + BALL_SIZE/2 > HEIGHT/2 - GOAL_WIDTH/2 &&
                    ball.getY() + BALL_SIZE/2 < HEIGHT/2 + GOAL_WIDTH/2) {
                scoreGoal(2);
                return;
            }
        }

        // Goal 2 (right)
        if (ball.getX() < WIDTH && ball.getX() + BALL_SIZE > WIDTH - GOAL_HEIGHT) {
            if (ball.getY() + BALL_SIZE/2 > HEIGHT/2 - GOAL_WIDTH/2 &&
                    ball.getY() + BALL_SIZE/2 < HEIGHT/2 + GOAL_WIDTH/2) {
                scoreGoal(1);
            }
        }
    }

    private void scoreGoal(int scoringPlayer) {
        if (scoringPlayer == 1) {
            player1Score++;
            player1Shots++;
            goalMessage = "⚽ GOAL! " + player1Name + " SCORES! ⚽";
            goalFlashTimer = 40;
            createGoalParticles(WIDTH/2, HEIGHT/2, new Color(0, 100, 200));
            celebrationTimer = 60;
            createConfetti(new Color(0, 100, 200));
        } else {
            player2Score++;
            player2Shots++;
            goalMessage = "⚽ GOAL! " + player2Name + " SCORES! ⚽";
            goalFlashTimer = 40;
            createGoalParticles(WIDTH/2, HEIGHT/2, new Color(200, 50, 50));
            celebrationTimer = 60;
            createConfetti(new Color(200, 50, 50));
        }
        goalMessageTimer = 150;
        goalCooldown = 60;

        // Reset ball and players
        ball.setX(WIDTH/2 - BALL_SIZE/2);
        ball.setY(HEIGHT/2 - BALL_SIZE/2);
        ball.setVx(0);
        ball.setVy(0);

        player1.setX(120);
        player1.setY(HEIGHT/2 - PLAYER_SIZE/2);
        player1.setDirection(0, 0);
        player1.setSprinting(false);
        player2.setX(WIDTH - 120 - PLAYER_SIZE);
        player2.setY(HEIGHT/2 - PLAYER_SIZE/2);
        player2.setDirection(0, 0);
        player2.setSprinting(false);

        repaint();
    }

    private void createKickParticles(double x, double y) {
        for (int i = 0; i < 25; i++) {
            particles.add(new Particle(x, y,
                    new Color(255, 200, 100),
                    (Math.random() - 0.5) * 8,
                    (Math.random() - 0.5) * 8,
                    25 + (int)(Math.random() * 25)));
        }
    }

    private void createGoalParticles(double x, double y, Color color) {
        for (int i = 0; i < 150; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double speed = 3 + Math.random() * 10;
            particles.add(new Particle(x, y, color,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    40 + (int)(Math.random() * 40)));
        }
    }

    private void createConfetti(Color color) {
        for (int i = 0; i < 100; i++) {
            confetti.add(new Confetti(
                    (int)(Math.random() * WIDTH),
                    (int)(Math.random() * HEIGHT / 2),
                    color,
                    (Math.random() - 0.5) * 4,
                    1 + Math.random() * 3,
                    50 + (int)(Math.random() * 50)
            ));
        }
    }

    private void updatePossession() {
        if (!possessionHolder.isEmpty()) {
            possessionTimer++;
            if (possessionTimer > 60) {
                if (possessionHolder.equals(player1.getName())) {
                    player1Possession = Math.min(100, player1Possession + 1);
                    player2Possession = Math.max(0, player2Possession - 1);
                } else {
                    player2Possession = Math.min(100, player2Possession + 1);
                    player1Possession = Math.max(0, player1Possession - 1);
                }
                possessionTimer = 0;
            }
        }
    }

    public void player1Kick() {
        if (gameRunning && !isPaused) {
            double dx = ball.getX() - player1.getX();
            double dy = ball.getY() - player1.getY();
            double dist = Math.sqrt(dx*dx + dy*dy);
            if (dist < 100) {
                double power = 7.0 * (1 - Math.min(1, dist/100));
                ball.setVelocity(dx/dist * power, dy/dist * power);
                createKickParticles(ball.getX() + BALL_SIZE/2,
                        ball.getY() + BALL_SIZE/2);
                player1Shots++;
                player1.kick();
            }
        }
    }

    public void player2Kick() {
        if (gameRunning && !isPaused && !isComputerMode) {
            double dx = ball.getX() - player2.getX();
            double dy = ball.getY() - player2.getY();
            double dist = Math.sqrt(dx*dx + dy*dy);
            if (dist < 100) {
                double power = 7.0 * (1 - Math.min(1, dist/100));
                ball.setVelocity(dx/dist * power, dy/dist * power);
                createKickParticles(ball.getX() + BALL_SIZE/2,
                        ball.getY() + BALL_SIZE/2);
                player2Shots++;
                player2.kick();
            }
        }
    }

    public void setPlayer1Direction(int dx, int dy) {
        if (gameRunning && !isPaused) player1.setDirection(dx, dy);
    }

    public void setPlayer2Direction(int dx, int dy) {
        if (gameRunning && !isPaused && !isComputerMode) player2.setDirection(dx, dy);
    }

    public void setPlayer1Sprint(boolean sprint) {
        if (gameRunning && !isPaused) player1.setSprinting(sprint);
    }

    public void setPlayer2Sprint(boolean sprint) {
        if (gameRunning && !isPaused && !isComputerMode) player2.setSprinting(sprint);
    }

    public void setGameRunning(boolean running) { this.gameRunning = running; }
    public void setPaused(boolean paused) { this.isPaused = paused; }
    public void setComputerMode(boolean computer) { this.isComputerMode = computer; }
    public void setDifficulty(String diff) { this.difficulty = diff; }
    public int getPlayer1Score() { return player1Score; }
    public int getPlayer2Score() { return player2Score; }
    public int getPlayer1Shots() { return player1Shots; }
    public int getPlayer2Shots() { return player2Shots; }
    public int getPlayer1Possession() { return player1Possession; }
    public int getPlayer2Possession() { return player2Possession; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        // Apply stadium zoom
        AffineTransform oldTransform = g2d.getTransform();
        g2d.scale(stadiumZoom, stadiumZoom);

        // Draw field
        drawField(g2d);

        // Draw stars
        for (Star star : stars) {
            star.draw(g2d);
        }

        // Draw goals
        goal1.draw(g2d);
        goal2.draw(g2d);

        // Draw field markings
        drawFieldMarkings(g2d);

        // Draw players
        player1.draw(g2d);
        player2.draw(g2d);

        // Draw ball
        ball.draw(g2d);

        // Draw particles
        for (Particle p : particles) {
            p.draw(g2d);
        }

        // Draw confetti
        for (Confetti c : confetti) {
            c.draw(g2d);
        }

        // Draw goal flash
        if (goalFlashTimer > 0) {
            g2d.setColor(new Color(255, 255, 255, goalFlashTimer * 3));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        }

        // Draw goal message with shadow
        if (goalMessageTimer > 0) {
            // Shadow
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.setFont(new Font("Arial", Font.BOLD, 60));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (WIDTH - fm.stringWidth(goalMessage)) / 2;
            int y = HEIGHT/2 - 60;
            g2d.drawString(goalMessage, x + 3, y + 3);

            // Main text
            GradientPaint gp = new GradientPaint(x, y, Color.YELLOW,
                    x + fm.stringWidth(goalMessage), y, Color.ORANGE);
            g2d.setPaint(gp);
            g2d.drawString(goalMessage, x, y);
        }

        // Draw controls hint
        drawControlsHint(g2d);

        g2d.setTransform(oldTransform);
    }

    private void drawField(Graphics2D g2d) {
        // Stadium background
        GradientPaint bgGp = new GradientPaint(0, 0, new Color(10, 10, 20),
                0, HEIGHT, new Color(20, 10, 30));
        g2d.setPaint(bgGp);
        g2d.fillRect(-50, -50, WIDTH + 100, HEIGHT + 100);

        // Grass gradient
        float green1 = 0.3f + fieldGradient * 0.3f;
        float green2 = 0.5f + fieldGradient * 0.2f;
        GradientPaint gp = new GradientPaint(0, 0,
                new Color(30, (int)(120 * green1), 30),
                0, HEIGHT,
                new Color(20, (int)(120 * green2), 20));
        g2d.setPaint(gp);
        g2d.fillRect(20, 20, WIDTH - 40, HEIGHT - 40);

        // Grass stripes
        g2d.setColor(new Color(255, 255, 255, 10));
        for (int i = 0; i < WIDTH - 40; i += 60) {
            g2d.fillRect(20 + i, 20, 30, HEIGHT - 40);
        }
    }

    private void drawFieldMarkings(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 80));
        g2d.setStroke(new BasicStroke(2));

        // Outer border
        g2d.drawRect(20, 20, WIDTH - 40, HEIGHT - 40);

        // Center line
        g2d.drawLine(WIDTH/2, 20, WIDTH/2, HEIGHT - 20);

        // Center circle
        g2d.drawOval(WIDTH/2 - 70, HEIGHT/2 - 70, 140, 140);
        g2d.drawOval(WIDTH/2 - 5, HEIGHT/2 - 5, 10, 10);

        // Penalty areas
        g2d.drawRect(20, HEIGHT/2 - 100, 100, 200);
        g2d.drawRect(WIDTH - 120, HEIGHT/2 - 100, 100, 200);

        // Goal areas
        g2d.drawRect(20, HEIGHT/2 - 50, 40, 100);
        g2d.drawRect(WIDTH - 60, HEIGHT/2 - 50, 40, 100);

        // Corner arcs
        g2d.drawArc(20, 20, 40, 40, 0, 90);
        g2d.drawArc(WIDTH - 60, 20, 40, 40, 90, 90);
        g2d.drawArc(20, HEIGHT - 60, 40, 40, 270, 90);
        g2d.drawArc(WIDTH - 60, HEIGHT - 60, 40, 40, 180, 90);

        // Penalty spots
        g2d.fillOval(WIDTH/2 - 120 - 5, HEIGHT/2 - 5, 10, 10);
        g2d.fillOval(WIDTH/2 + 120 - 5, HEIGHT/2 - 5, 10, 10);
    }

    private void drawControlsHint(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 120));
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));

        String[] hints = {
                "P1: WASD | Sprint: Q | Shoot: E",
                "P2: Arrows | Sprint: Num1 | Shoot: Num2"
        };

        if (isComputerMode) {
            hints[1] = "🤖 Computer (" + difficulty + ")";
        }

        for (int i = 0; i < hints.length; i++) {
            g2d.drawString(hints[i], 30, 40 + i * 18);
        }
    }
}

class Player {
    private double x, y;
    private int size;
    private Color color;
    private String name;
    private double vx, vy;
    private int directionX, directionY;
    private double angle;
    private int kickAnimation;
    private boolean sprinting;
    private double stamina = 100;
    private int jerseyNumber;
    private static int nextNumber = 1;

    public Player(double x, double y, int size, Color color, String name) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = color;
        this.name = name;
        this.vx = 0;
        this.vy = 0;
        this.directionX = 0;
        this.directionY = 0;
        this.angle = 0;
        this.kickAnimation = 0;
        this.sprinting = false;
        this.jerseyNumber = nextNumber++;
    }

    public void update() {
        // Movement with inertia and stamina
        double speed = Math.sqrt(directionX*directionX + directionY*directionY);
        if (speed > 1) {
            directionX = (int)(directionX / speed);
            directionY = (int)(directionY / speed);
        }

        double maxSpeed = sprinting ? 5.0 : 3.0;
        double acceleration = sprinting ? 0.25 : 0.15;

        // Stamina management
        if (sprinting) {
            stamina -= 0.3;
            if (stamina < 0) {
                stamina = 0;
                sprinting = false;
            }
        } else {
            stamina = Math.min(100, stamina + 0.1);
        }

        double targetVx = directionX * maxSpeed * (stamina/100 + 0.2);
        double targetVy = directionY * maxSpeed * (stamina/100 + 0.2);

        vx += (targetVx - vx) * acceleration;
        vy += (targetVy - vy) * acceleration;

        x += vx;
        y += vy;

        // Keep within bounds
        x = Math.max(25, Math.min(875 - size, x));
        y = Math.max(25, Math.min(625 - size, y));

        // Update angle
        if (Math.abs(vx) > 0.1 || Math.abs(vy) > 0.1) {
            angle = Math.atan2(vy, vx);
        }

        if (kickAnimation > 0) kickAnimation--;
    }

    public void kick() {
        kickAnimation = 15;
    }

    public void draw(Graphics2D g2d) {
        // Shadow
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillOval((int)x + 8, (int)y + 8, size, size);

        // Body with gradient
        GradientPaint gp = new GradientPaint((int)x, (int)y, color.brighter(),
                (int)(x + size), (int)(y + size), color);
        g2d.setPaint(gp);
        g2d.fillOval((int)x, (int)y, size, size);

        // Border
        g2d.setColor(color.darker());
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval((int)x, (int)y, size, size);

        // Jersey number
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics fm = g2d.getFontMetrics();
        String numStr = String.valueOf(jerseyNumber);
        int textX = (int)(x + size/2 - fm.stringWidth(numStr)/2);
        int textY = (int)(y + size/2 + fm.getHeight()/3);
        g2d.drawString(numStr, textX, textY);

        // Direction indicator
        int centerX = (int)(x + size/2);
        int centerY = (int)(y + size/2);
        int arrowLen = 18;
        int endX = (int)(centerX + Math.cos(angle) * arrowLen);
        int endY = (int)(centerY + Math.sin(angle) * arrowLen);

        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(centerX, centerY, endX, endY);

        // Sprint indicator
        if (sprinting) {
            g2d.setColor(new Color(255, 200, 0, 100));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval((int)(x - 5), (int)(y - 5), size + 10, size + 10);

            // Stamina bar
            int barWidth = size + 20;
            int barX = (int)(x - 10);
            int barY = (int)(y - 12);
            g2d.setColor(new Color(50, 50, 50, 150));
            g2d.fillRect(barX, barY, barWidth, 4);
            g2d.setColor(new Color(0, 200, 0));
            g2d.fillRect(barX, barY, (int)(barWidth * stamina / 100), 4);
        }

        // Kick animation
        if (kickAnimation > 0) {
            g2d.setColor(new Color(255, 255, 100, kickAnimation * 15));
            int kickSize = size + kickAnimation * 3;
            g2d.drawOval((int)(x - (kickSize - size)/2),
                    (int)(y - (kickSize - size)/2),
                    kickSize, kickSize);
        }

        // Player name
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        fm = g2d.getFontMetrics();
        textX = (int)(x + size/2 - fm.stringWidth(name)/2);
        textY = (int)(y - 8);
        g2d.drawString(name, textX, textY);
    }

    public boolean intersects(Rectangle rect) {
        return new Rectangle((int)x, (int)y, size, size).intersects(rect);
    }

    // Getters and setters
    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return size; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public String getName() { return name; }
    public boolean isSprinting() { return sprinting; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setDirection(int dx, int dy) { this.directionX = dx; this.directionY = dy; }
    public void setSprinting(boolean sprint) { this.sprinting = sprint && stamina > 20; }
}

class Ball {
    private double x, y;
    private int size;
    private double vx, vy;
    private double rotation;
    private List<double[]> trail;
    private double bounceHeight;
    private boolean inAir;

    public Ball(double x, double y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.vx = 0;
        this.vy = 0;
        this.rotation = 0;
        this.trail = new ArrayList<>();
        this.bounceHeight = 0;
        this.inAir = false;
    }

    public void update() {
        // Apply friction
        vx *= 0.985;
        vy *= 0.985;

        // Stop if very slow
        if (Math.abs(vx) < 0.01) vx = 0;
        if (Math.abs(vy) < 0.01) vy = 0;

        x += vx;
        y += vy;

        rotation += Math.sqrt(vx*vx + vy*vy) * 0.08;

        // Trail
        if (Math.abs(vx) > 0.5 || Math.abs(vy) > 0.5) {
            trail.add(new double[]{x + size/2, y + size/2});
            if (trail.size() > 20) {
                trail.remove(0);
            }
        }
    }

    public void draw(Graphics2D g2d) {
        // Draw trail
        for (int i = 0; i < trail.size(); i++) {
            double[] pos = trail.get(i);
            int alpha = (i * 12);
            g2d.setColor(new Color(255, 255, 255, Math.min(alpha, 180)));
            int trailSize = size * (i + 1) / (trail.size() + 1);
            g2d.fillOval((int)(pos[0] - trailSize/2),
                    (int)(pos[1] - trailSize/2),
                    trailSize, trailSize);
        }

        // Ball shadow
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillOval((int)x + 5, (int)y + 8, size, size/2);

        // Ball
        GradientPaint gp = new GradientPaint((int)x, (int)y,
                new Color(255, 255, 255),
                (int)(x + size), (int)(y + size),
                new Color(200, 200, 200));
        g2d.setPaint(gp);
        g2d.fillOval((int)x, (int)y, size, size);

        // Ball pattern
        g2d.setColor(new Color(150, 150, 150));
        g2d.setStroke(new BasicStroke(1.5f));
        int centerX = (int)(x + size/2);
        int centerY = (int)(y + size/2);

        g2d.rotate(rotation, centerX, centerY);

        // Pentagon pattern
        for (int i = 0; i < 5; i++) {
            double angle = i * 2 * Math.PI / 5 - Math.PI/2;
            int dx = (int)(size * 0.38 * Math.cos(angle));
            int dy = (int)(size * 0.38 * Math.sin(angle));
            g2d.drawLine(centerX, centerY, centerX + dx, centerY + dy);
            g2d.drawOval(centerX + dx - 4, centerY + dy - 4, 8, 8);
        }

        g2d.rotate(-rotation, centerX, centerY);

        // Highlight
        g2d.setColor(new Color(255, 255, 255, 120));
        g2d.fillOval((int)(x + size * 0.2), (int)(y + size * 0.15),
                (int)(size * 0.35), (int)(size * 0.3));

        // Glow effect
        double speed = Math.sqrt(vx*vx + vy*vy);
        if (speed > 2.0) {
            int glow = (int)(Math.min(speed * 15, 120));
            g2d.setColor(new Color(255, 255, 200, glow));
            int glowSize = size + (int)(speed * 3);
            g2d.drawOval((int)(x - (glowSize - size)/2),
                    (int)(y - (glowSize - size)/2),
                    glowSize, glowSize);
        }
    }

    public boolean intersects(Player player) {
        double dx = (x + size/2) - (player.getX() + player.getSize()/2);
        double dy = (y + size/2) - (player.getY() + player.getSize()/2);
        double dist = Math.sqrt(dx*dx + dy*dy);
        return dist < (size/2 + player.getSize()/2);
    }

    public boolean intersects(Rectangle rect) {
        return new Rectangle((int)x, (int)y, size, size).intersects(rect);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setVx(double vx) { this.vx = vx; }
    public void setVy(double vy) { this.vy = vy; }
}

class Goal {
    private int x, y, width, height;
    private Color color;
    private boolean scored;

    public Goal(int x, int y, int width, int height, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.scored = false;
    }

    public void draw(Graphics2D g2d) {
        // Goal frame with glow
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(5));
        g2d.drawRect(x, y, width, height);

        // Goal net
        g2d.setColor(new Color(255, 255, 255, 20));
        g2d.setStroke(new BasicStroke(1));
        for (int i = 0; i < height; i += 12) {
            g2d.drawLine(x, y + i, x + width, y + i);
        }
        for (int i = 0; i < width; i += 12) {
            g2d.drawLine(x + i, y, x + i, y + height);
        }

        // Diagonal net lines
        g2d.setColor(new Color(255, 255, 255, 10));
        for (int i = -height; i < width + height; i += 15) {
            g2d.drawLine(x + i, y, x + i + height, y + height);
        }
    }
}

class Particle {
    private double x, y;
    private Color color;
    private double vx, vy;
    private int life;
    private int maxLife;
    private double gravity = 0.1;

    public Particle(double x, double y, Color color, double vx, double vy, int life) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
    }

    public void update() {
        x += vx;
        y += vy;
        vy += gravity;
        vx *= 0.98;
        vy *= 0.98;
        life--;
    }

    public void draw(Graphics2D g2d) {
        int alpha = (life * 255) / maxLife;
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        int size = (life * 6) / maxLife + 3;
        g2d.fillOval((int)x - size/2, (int)y - size/2, size, size);
    }

    public boolean isDead() { return life <= 0; }
}

class Confetti {
    private double x, y;
    private Color color;
    private double vx, vy;
    private double rotation;
    private double rotSpeed;
    private int width, height;
    private int life;
    private int maxLife;

    public Confetti(int x, int y, Color color, double vx, double vy, int life) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.vx = vx;
        this.vy = vy;
        this.rotation = Math.random() * Math.PI * 2;
        this.rotSpeed = (Math.random() - 0.5) * 0.2;
        this.width = 5 + (int)(Math.random() * 8);
        this.height = 3 + (int)(Math.random() * 5);
        this.life = life;
        this.maxLife = life;
    }

    public void update() {
        x += vx;
        y += vy;
        vy += 0.1;
        rotation += rotSpeed;
        life--;
    }

    public void draw(Graphics2D g2d) {
        int alpha = (life * 255) / maxLife;
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g2d.rotate(rotation, x, y);
        g2d.fillRect((int)x - width/2, (int)y - height/2, width, height);
        g2d.rotate(-rotation, x, y);
    }

    public boolean isDead() { return life <= 0; }
}

class Star {
    private double x, y;
    private int size;
    private double twinkleSpeed;
    private int phase;

    public Star() {
        this.x = Math.random() * 900;
        this.y = Math.random() * 650;
        this.size = 1 + (int)(Math.random() * 3);
        this.twinkleSpeed = 0.015 + Math.random() * 0.025;
        this.phase = (int)(Math.random() * 100);
    }

    public void update() {
        phase++;
    }

    public void draw(Graphics2D g2d) {
        int alpha = (int)(Math.sin(phase * twinkleSpeed) * 127 + 128);
        g2d.setColor(new Color(255, 255, 255, alpha));
        g2d.fillOval((int)x, (int)y, size, size);
    }
}