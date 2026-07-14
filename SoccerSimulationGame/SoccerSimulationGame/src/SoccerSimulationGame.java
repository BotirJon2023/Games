import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class SoccerSimulationGame extends JFrame {
    private GamePanel gamePanel;
    private JPanel controlPanel;
    private JButton startButton, pauseButton, resetButton;
    private JButton playerModeButton, computerModeButton;
    private JLabel scoreLabel, timeLabel;
    private Timer gameTimer;
    private boolean isPaused = false;
    private boolean isComputerMode = false;
    private int gameTime = 0;

    public SoccerSimulationGame() {
        setTitle("Soccer Simulation Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize game panel
        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        // Initialize control panel
        controlPanel = new JPanel();
        controlPanel.setBackground(new Color(50, 50, 50));
        controlPanel.setPreferredSize(new Dimension(800, 80));
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));

        // Create buttons
        startButton = createStyledButton("Start Game", new Color(46, 204, 113));
        pauseButton = createStyledButton("Pause", new Color(241, 196, 15));
        resetButton = createStyledButton("Reset", new Color(231, 76, 60));
        playerModeButton = createStyledButton("2 Players", new Color(52, 152, 219));
        computerModeButton = createStyledButton("vs Computer", new Color(155, 89, 182));

        // Score and time labels
        scoreLabel = new JLabel("Score: 0 - 0");
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 18));

        timeLabel = new JLabel("Time: 00:00");
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 18));

        // Add components to control panel
        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(resetButton);
        controlPanel.add(playerModeButton);
        controlPanel.add(computerModeButton);
        controlPanel.add(scoreLabel);
        controlPanel.add(timeLabel);

        add(controlPanel, BorderLayout.SOUTH);

        // Add action listeners
        startButton.addActionListener(e -> startGame());
        pauseButton.addActionListener(e -> togglePause());
        resetButton.addActionListener(e -> resetGame());
        playerModeButton.addActionListener(e -> setPlayerMode(false));
        computerModeButton.addActionListener(e -> setPlayerMode(true));

        // Keyboard controls
        setupKeyBindings();

        // Initialize game timer
        gameTimer = new Timer(16, e -> {
            if (!isPaused) {
                gamePanel.update();
                updateTime();
                updateScore();
                repaint();
            }
        });

        // Set window properties
        setSize(900, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void setupKeyBindings() {
        InputMap inputMap = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = gamePanel.getActionMap();

        // Player 1 controls (WASD)
        inputMap.put(KeyStroke.getKeyStroke("W"), "player1Up");
        inputMap.put(KeyStroke.getKeyStroke("S"), "player1Down");
        inputMap.put(KeyStroke.getKeyStroke("A"), "player1Left");
        inputMap.put(KeyStroke.getKeyStroke("D"), "player1Right");
        inputMap.put(KeyStroke.getKeyStroke("SPACE"), "player1Kick");

        actionMap.put("player1Up", new KeyAction(1, 0, -1));
        actionMap.put("player1Down", new KeyAction(1, 0, 1));
        actionMap.put("player1Left", new KeyAction(1, -1, 0));
        actionMap.put("player1Right", new KeyAction(1, 1, 0));
        actionMap.put("player1Kick", new KeyAction(1, 0, 0, true));

        // Player 2 controls (Arrow keys)
        inputMap.put(KeyStroke.getKeyStroke("UP"), "player2Up");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "player2Down");
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "player2Left");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "player2Right");
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "player2Kick");

        actionMap.put("player2Up", new KeyAction(2, 0, -1));
        actionMap.put("player2Down", new KeyAction(2, 0, 1));
        actionMap.put("player2Left", new KeyAction(2, -1, 0));
        actionMap.put("player2Right", new KeyAction(2, 1, 0));
        actionMap.put("player2Kick", new KeyAction(2, 0, 0, true));

        // Release keys
        inputMap.put(KeyStroke.getKeyStroke("released W"), "releaseP1Up");
        inputMap.put(KeyStroke.getKeyStroke("released S"), "releaseP1Down");
        inputMap.put(KeyStroke.getKeyStroke("released A"), "releaseP1Left");
        inputMap.put(KeyStroke.getKeyStroke("released D"), "releaseP1Right");

        actionMap.put("releaseP1Up", new KeyAction(1, 0, 0));
        actionMap.put("releaseP1Down", new KeyAction(1, 0, 0));
        actionMap.put("releaseP1Left", new KeyAction(1, 0, 0));
        actionMap.put("releaseP1Right", new KeyAction(1, 0, 0));

        inputMap.put(KeyStroke.getKeyStroke("released UP"), "releaseP2Up");
        inputMap.put(KeyStroke.getKeyStroke("released DOWN"), "releaseP2Down");
        inputMap.put(KeyStroke.getKeyStroke("released LEFT"), "releaseP2Left");
        inputMap.put(KeyStroke.getKeyStroke("released RIGHT"), "releaseP2Right");

        actionMap.put("releaseP2Up", new KeyAction(2, 0, 0));
        actionMap.put("releaseP2Down", new KeyAction(2, 0, 0));
        actionMap.put("releaseP2Left", new KeyAction(2, 0, 0));
        actionMap.put("releaseP2Right", new KeyAction(2, 0, 0));
    }

    private class KeyAction extends AbstractAction {
        private int player;
        private int dx, dy;
        private boolean kick;

        public KeyAction(int player, int dx, int dy) {
            this(player, dx, dy, false);
        }

        public KeyAction(int player, int dx, int dy, boolean kick) {
            this.player = player;
            this.dx = dx;
            this.dy = dy;
            this.kick = kick;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (player == 1) {
                gamePanel.setPlayer1Direction(dx, dy);
                if (kick) gamePanel.player1Kick();
            } else if (player == 2) {
                gamePanel.setPlayer2Direction(dx, dy);
                if (kick) gamePanel.player2Kick();
            }
        }
    }

    private void startGame() {
        if (!gameTimer.isRunning()) {
            gameTimer.start();
            gamePanel.setGameRunning(true);
            startButton.setEnabled(false);
            pauseButton.setEnabled(true);
        }
    }

    private void togglePause() {
        isPaused = !isPaused;
        pauseButton.setText(isPaused ? "Resume" : "Pause");
        gamePanel.setPaused(isPaused);
    }

    private void resetGame() {
        gameTimer.stop();
        gamePanel.reset();
        gameTime = 0;
        timeLabel.setText("Time: 00:00");
        scoreLabel.setText("Score: 0 - 0");
        isPaused = false;
        pauseButton.setText("Pause");
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        gamePanel.setGameRunning(false);
        repaint();
    }

    private void setPlayerMode(boolean computer) {
        isComputerMode = computer;
        gamePanel.setComputerMode(computer);
        playerModeButton.setBackground(computer ? new Color(52, 152, 219) : new Color(100, 100, 100));
        computerModeButton.setBackground(computer ? new Color(100, 100, 100) : new Color(155, 89, 182));
        resetGame();
    }

    private void updateTime() {
        gameTime++;
        int seconds = gameTime / 60;
        int minutes = seconds / 60;
        seconds %= 60;
        timeLabel.setText(String.format("Time: %02d:%02d", minutes, seconds));
    }

    private void updateScore() {
        scoreLabel.setText(String.format("Score: %d - %d",
                gamePanel.getPlayer1Score(), gamePanel.getPlayer2Score()));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SoccerSimulationGame());
    }
}

class GamePanel extends JPanel {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PLAYER_SIZE = 30;
    private static final int BALL_SIZE = 16;
    private static final int GOAL_WIDTH = 120;
    private static final int GOAL_HEIGHT = 20;
    private static final double FRICTION = 0.985;
    private static final double MAX_SPEED = 5.0;
    private static final double KICK_POWER = 8.0;

    private Player player1, player2;
    private Ball ball;
    private Goal goal1, goal2;
    private List<Particle> particles;
    private List<Star> stars;
    private boolean gameRunning = false;
    private boolean isPaused = false;
    private boolean isComputerMode = false;
    private int player1Score = 0;
    private int player2Score = 0;
    private int goalCooldown = 0;

    // Animation effects
    private float fieldGradient = 0.5f;
    private boolean gradientDirection = true;
    private int goalFlashTimer = 0;
    private String goalMessage = "";
    private int goalMessageTimer = 0;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(34, 139, 34));
        setFocusable(true);

        particles = new ArrayList<>();
        stars = new ArrayList<>();

        // Initialize stars for background effect
        for (int i = 0; i < 50; i++) {
            stars.add(new Star());
        }

        reset();
    }

    public void reset() {
        player1 = new Player(100, HEIGHT/2 - PLAYER_SIZE/2, PLAYER_SIZE,
                new Color(255, 50, 50), "P1");
        player2 = new Player(WIDTH - 100 - PLAYER_SIZE, HEIGHT/2 - PLAYER_SIZE/2,
                PLAYER_SIZE, new Color(50, 50, 255), "P2");
        ball = new Ball(WIDTH/2 - BALL_SIZE/2, HEIGHT/2 - BALL_SIZE/2, BALL_SIZE);

        goal1 = new Goal(0, HEIGHT/2 - GOAL_WIDTH/2, GOAL_HEIGHT, GOAL_WIDTH,
                new Color(255, 200, 200));
        goal2 = new Goal(WIDTH - GOAL_HEIGHT, HEIGHT/2 - GOAL_WIDTH/2,
                GOAL_HEIGHT, GOAL_WIDTH, new Color(200, 200, 255));

        particles.clear();
        goalCooldown = 0;
        goalFlashTimer = 0;
        goalMessage = "";
        goalMessageTimer = 0;

        repaint();
    }

    public void update() {
        if (isPaused || !gameRunning) return;

        // Update field animation
        if (gradientDirection) {
            fieldGradient += 0.001f;
            if (fieldGradient >= 0.7f) gradientDirection = false;
        } else {
            fieldGradient -= 0.001f;
            if (fieldGradient <= 0.3f) gradientDirection = true;
        }

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

        // Update particles
        Iterator<Particle> iter = particles.iterator();
        while (iter.hasNext()) {
            Particle p = iter.next();
            p.update();
            if (p.isDead()) {
                iter.remove();
            }
        }

        // Update stars
        for (Star star : stars) {
            star.update();
        }

        if (goalCooldown > 0) {
            goalCooldown--;
        }
        if (goalMessageTimer > 0) {
            goalMessageTimer--;
        }
        if (goalFlashTimer > 0) {
            goalFlashTimer--;
        }
    }

    private void updateComputerAI() {
        // AI player follows ball with some intelligence
        double dx = ball.getX() - player2.getX();
        double dy = ball.getY() - player2.getY();
        double distance = Math.sqrt(dx*dx + dy*dy);

        if (distance > 50) {
            // Move towards ball
            double speed = Math.min(MAX_SPEED, distance / 20);
            double angle = Math.atan2(dy, dx);
            player2.setDirection((int)(Math.cos(angle) * speed),
                    (int)(Math.sin(angle) * speed));
        } else if (distance < 30) {
            // Kick ball towards goal
            double goalX = WIDTH - 20;
            double goalY = HEIGHT/2;
            double kickDx = goalX - ball.getX();
            double kickDy = goalY - ball.getY();
            double kickDist = Math.sqrt(kickDx*kickDx + kickDy*kickDy);
            if (kickDist > 0) {
                ball.setVelocity(kickDx/kickDist * KICK_POWER,
                        kickDy/kickDist * KICK_POWER);
                createKickParticles(ball.getX() + BALL_SIZE/2,
                        ball.getY() + BALL_SIZE/2);
            }
            player2.setDirection(0, 0);
        } else {
            // Position between ball and goal
            player2.setDirection(0, 0);
        }

        // Simple dribbling
        if (distance < 40 && distance > 20) {
            ball.setVelocity((ball.getX() - player2.getX()) * 0.05,
                    (ball.getY() - player2.getY()) * 0.05);
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

                // Push ball away
                ball.setX(ball.getX() + Math.cos(angle) * overlap);
                ball.setY(ball.getY() + Math.sin(angle) * overlap);

                // Transfer some player velocity to ball
                double speed = Math.sqrt(player.getVx()*player.getVx() +
                        player.getVy()*player.getVy());
                if (speed > 0.5) {
                    ball.setVelocity(ball.getVx() + player.getVx() * 0.3,
                            ball.getVy() + player.getVy() * 0.3);
                }
            }
        }
    }

    private void checkBallWallCollision() {
        // Left wall
        if (ball.getX() < 0) {
            ball.setX(0);
            ball.setVx(-ball.getVx() * 0.8);
        }
        // Right wall
        if (ball.getX() + BALL_SIZE > WIDTH) {
            ball.setX(WIDTH - BALL_SIZE);
            ball.setVx(-ball.getVx() * 0.8);
        }
        // Top wall
        if (ball.getY() < 0) {
            ball.setY(0);
            ball.setVy(-ball.getVy() * 0.8);
        }
        // Bottom wall
        if (ball.getY() + BALL_SIZE > HEIGHT) {
            ball.setY(HEIGHT - BALL_SIZE);
            ball.setVy(-ball.getVy() * 0.8);
        }

        // Goal posts collision
        Rectangle topPost1 = new Rectangle(0, HEIGHT/2 - GOAL_WIDTH/2,
                GOAL_HEIGHT, 5);
        Rectangle bottomPost1 = new Rectangle(0, HEIGHT/2 + GOAL_WIDTH/2 - 5,
                GOAL_HEIGHT, 5);
        Rectangle topPost2 = new Rectangle(WIDTH - GOAL_HEIGHT,
                HEIGHT/2 - GOAL_WIDTH/2,
                GOAL_HEIGHT, 5);
        Rectangle bottomPost2 = new Rectangle(WIDTH - GOAL_HEIGHT,
                HEIGHT/2 + GOAL_WIDTH/2 - 5,
                GOAL_HEIGHT, 5);

        checkPostCollision(topPost1);
        checkPostCollision(bottomPost1);
        checkPostCollision(topPost2);
        checkPostCollision(bottomPost2);
    }

    private void checkPostCollision(Rectangle post) {
        if (ball.intersects(post)) {
            // Simple bounce off post
            if (ball.getVx() < 0.1) {
                ball.setVx(-ball.getVx() * 0.5);
            }
            if (ball.getVy() < 0.1) {
                ball.setVy(-ball.getVy() * 0.5);
            }
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
            goalMessage = "GOAL! Player 1 Scores!";
            goalFlashTimer = 30;
            createGoalParticles(WIDTH/2, HEIGHT/2, new Color(255, 50, 50));
        } else {
            player2Score++;
            goalMessage = "GOAL! Player 2 Scores!";
            goalFlashTimer = 30;
            createGoalParticles(WIDTH/2, HEIGHT/2, new Color(50, 50, 255));
        }
        goalMessageTimer = 120; // Show message for 2 seconds
        goalCooldown = 60; // 1 second cooldown

        // Reset ball position
        ball.setX(WIDTH/2 - BALL_SIZE/2);
        ball.setY(HEIGHT/2 - BALL_SIZE/2);
        ball.setVx(0);
        ball.setVy(0);

        // Reset player positions
        player1.setX(100);
        player1.setY(HEIGHT/2 - PLAYER_SIZE/2);
        player1.setDirection(0, 0);
        player2.setX(WIDTH - 100 - PLAYER_SIZE);
        player2.setY(HEIGHT/2 - PLAYER_SIZE/2);
        player2.setDirection(0, 0);

        repaint();
    }

    private void createKickParticles(double x, double y) {
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(x, y,
                    new Color(255, 200, 100),
                    (Math.random() - 0.5) * 6,
                    (Math.random() - 0.5) * 6,
                    20 + (int)(Math.random() * 20)));
        }
    }

    private void createGoalParticles(double x, double y, Color color) {
        for (int i = 0; i < 100; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double speed = 2 + Math.random() * 8;
            particles.add(new Particle(x, y, color,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    30 + (int)(Math.random() * 30)));
        }
    }

    public void player1Kick() {
        if (gameRunning && !isPaused) {
            double dx = ball.getX() - player1.getX();
            double dy = ball.getY() - player1.getY();
            double dist = Math.sqrt(dx*dx + dy*dy);
            if (dist < 80) {
                double power = KICK_POWER * (1 - dist/80);
                ball.setVelocity(dx/dist * power, dy/dist * power);
                createKickParticles(ball.getX() + BALL_SIZE/2,
                        ball.getY() + BALL_SIZE/2);
            }
        }
    }

    public void player2Kick() {
        if (gameRunning && !isPaused && !isComputerMode) {
            double dx = ball.getX() - player2.getX();
            double dy = ball.getY() - player2.getY();
            double dist = Math.sqrt(dx*dx + dy*dy);
            if (dist < 80) {
                double power = KICK_POWER * (1 - dist/80);
                ball.setVelocity(dx/dist * power, dy/dist * power);
                createKickParticles(ball.getX() + BALL_SIZE/2,
                        ball.getY() + BALL_SIZE/2);
            }
        }
    }

    public void setPlayer1Direction(int dx, int dy) {
        if (gameRunning && !isPaused) {
            player1.setDirection(dx, dy);
        }
    }

    public void setPlayer2Direction(int dx, int dy) {
        if (gameRunning && !isPaused && !isComputerMode) {
            player2.setDirection(dx, dy);
        }
    }

    public void setGameRunning(boolean running) {
        this.gameRunning = running;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    public void setComputerMode(boolean computer) {
        this.isComputerMode = computer;
    }

    public int getPlayer1Score() { return player1Score; }
    public int getPlayer2Score() { return player2Score; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw field with gradient
        drawField(g2d);

        // Draw stars
        for (Star star : stars) {
            star.draw(g2d);
        }

        // Draw goals
        goal1.draw(g2d);
        goal2.draw(g2d);

        // Draw center line and circle
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(WIDTH/2, 0, WIDTH/2, HEIGHT);
        g2d.drawOval(WIDTH/2 - 60, HEIGHT/2 - 60, 120, 120);
        g2d.drawOval(WIDTH/2 - 3, HEIGHT/2 - 3, 6, 6);

        // Draw players
        player1.draw(g2d);
        player2.draw(g2d);

        // Draw ball with glow
        ball.draw(g2d);

        // Draw particles
        for (Particle p : particles) {
            p.draw(g2d);
        }

        // Draw goal flash
        if (goalFlashTimer > 0) {
            g2d.setColor(new Color(255, 255, 255, goalFlashTimer * 3));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        }

        // Draw goal message
        if (goalMessageTimer > 0) {
            g2d.setColor(new Color(255, 215, 0));
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (WIDTH - fm.stringWidth(goalMessage)) / 2;
            int y = HEIGHT/2 - 50;
            g2d.drawString(goalMessage, x, y);

            // Shadow
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.drawString(goalMessage, x + 2, y + 2);
        }

        // Draw controls hint
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("P1: WASD + SPACE", 10, 20);
        g2d.drawString("P2: Arrows + ENTER", 10, 35);
        if (isComputerMode) {
            g2d.drawString("Computer Mode: Player 2 is AI", 10, 50);
        }
    }

    private void drawField(Graphics2D g2d) {
        // Grass gradient
        GradientPaint gp = new GradientPaint(0, 0,
                new Color(34, (int)(139 * (0.7 + fieldGradient * 0.3)), 34),
                0, HEIGHT,
                new Color(34, (int)(139 * (0.3 + fieldGradient * 0.7)), 34));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Field markings
        g2d.setColor(new Color(255, 255, 255, 60));
        g2d.setStroke(new BasicStroke(2));

        // Outer border
        g2d.drawRect(10, 10, WIDTH - 20, HEIGHT - 20);

        // Penalty areas
        g2d.drawRect(10, HEIGHT/2 - 90, 80, 180);
        g2d.drawRect(WIDTH - 90, HEIGHT/2 - 90, 80, 180);

        // Goal areas
        g2d.drawRect(10, HEIGHT/2 - 45, 30, 90);
        g2d.drawRect(WIDTH - 40, HEIGHT/2 - 45, 30, 90);
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
    }

    public void update() {
        // Movement with inertia
        double speed = Math.sqrt(directionX*directionX + directionY*directionY);
        if (speed > 1) {
            directionX = (int)(directionX / speed);
            directionY = (int)(directionY / speed);
        }

        double targetVx = directionX * 3.0;
        double targetVy = directionY * 3.0;

        vx += (targetVx - vx) * 0.15;
        vy += (targetVy - vy) * 0.15;

        // Limit speed
        double currentSpeed = Math.sqrt(vx*vx + vy*vy);
        if (currentSpeed > 4.0) {
            vx = (vx / currentSpeed) * 4.0;
            vy = (vy / currentSpeed) * 4.0;
        }

        x += vx;
        y += vy;

        // Keep within bounds
        x = Math.max(20, Math.min(780 - size, x));
        y = Math.max(20, Math.min(580 - size, y));

        // Update angle
        if (Math.abs(vx) > 0.1 || Math.abs(vy) > 0.1) {
            angle = Math.atan2(vy, vx);
        }

        if (kickAnimation > 0) kickAnimation--;
    }

    public void setDirection(int dx, int dy) {
        this.directionX = dx;
        this.directionY = dy;
    }

    public void kick() {
        kickAnimation = 10;
    }

    public void draw(Graphics2D g2d) {
        // Shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval((int)x + 5, (int)y + 5, size, size);

        // Body with gradient
        GradientPaint gp = new GradientPaint((int)x, (int)y, color.brighter(),
                (int)(x + size), (int)(y + size), color);
        g2d.setPaint(gp);
        g2d.fillOval((int)x, (int)y, size, size);

        // Border
        g2d.setColor(color.darker());
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval((int)x, (int)y, size, size);

        // Direction indicator
        int centerX = (int)(x + size/2);
        int centerY = (int)(y + size/2);
        int arrowLen = 15;
        int endX = (int)(centerX + Math.cos(angle) * arrowLen);
        int endY = (int)(centerY + Math.sin(angle) * arrowLen);

        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(centerX, centerY, endX, endY);

        // Kick animation
        if (kickAnimation > 0) {
            g2d.setColor(new Color(255, 255, 100, kickAnimation * 20));
            int kickSize = size + kickAnimation * 2;
            g2d.drawOval((int)(x - (kickSize - size)/2),
                    (int)(y - (kickSize - size)/2),
                    kickSize, kickSize);
        }

        // Player number/name
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (int)(x + size/2 - fm.stringWidth(name)/2);
        int textY = (int)(y + size/2 + fm.getHeight()/3);
        g2d.drawString(name, textX, textY);
    }

    public boolean intersects(Rectangle rect) {
        return new Rectangle((int)x, (int)y, size, size).intersects(rect);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return size; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setDirection(int dx, int dy) { this.directionX = dx; this.directionY = dy; }
}

class Ball {
    private double x, y;
    private int size;
    private double vx, vy;
    private double rotation;
    private int trailCounter;
    private List<double[]> trail;

    public Ball(double x, double y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.vx = 0;
        this.vy = 0;
        this.rotation = 0;
        this.trail = new ArrayList<>();
        this.trailCounter = 0;
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

        rotation += Math.sqrt(vx*vx + vy*vy) * 0.05;

        // Trail
        trailCounter++;
        if (trailCounter % 2 == 0 && (Math.abs(vx) > 0.1 || Math.abs(vy) > 0.1)) {
            trail.add(new double[]{x + size/2, y + size/2});
            if (trail.size() > 15) {
                trail.remove(0);
            }
        }
    }

    public void draw(Graphics2D g2d) {
        // Draw trail
        for (int i = 0; i < trail.size(); i++) {
            double[] pos = trail.get(i);
            int alpha = (i * 17);
            g2d.setColor(new Color(255, 255, 255, Math.min(alpha, 200)));
            int trailSize = size * (i + 1) / (trail.size() + 1);
            g2d.fillOval((int)(pos[0] - trailSize/2),
                    (int)(pos[1] - trailSize/2),
                    trailSize, trailSize);
        }

        // Ball shadow
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillOval((int)x + 3, (int)y + 3, size, size);

        // Ball with gradient
        GradientPaint gp = new GradientPaint((int)x, (int)y,
                new Color(255, 255, 255),
                (int)(x + size), (int)(y + size),
                new Color(200, 200, 200));
        g2d.setPaint(gp);
        g2d.fillOval((int)x, (int)y, size, size);

        // Ball pattern
        g2d.setColor(new Color(150, 150, 150));
        g2d.setStroke(new BasicStroke(1));
        int centerX = (int)(x + size/2);
        int centerY = (int)(y + size/2);

        // Rotate pattern
        g2d.rotate(rotation, centerX, centerY);

        // Pentagon pattern
        for (int i = 0; i < 5; i++) {
            double angle = i * 2 * Math.PI / 5 - Math.PI/2;
            int dx = (int)(size * 0.35 * Math.cos(angle));
            int dy = (int)(size * 0.35 * Math.sin(angle));
            g2d.drawLine(centerX, centerY, centerX + dx, centerY + dy);
            g2d.drawOval(centerX + dx - 3, centerY + dy - 3, 6, 6);
        }

        g2d.rotate(-rotation, centerX, centerY);

        // Highlight
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.fillOval((int)(x + size * 0.2), (int)(y + size * 0.2),
                (int)(size * 0.3), (int)(size * 0.3));

        // Glow effect when moving fast
        double speed = Math.sqrt(vx*vx + vy*vy);
        if (speed > 1.0) {
            int glow = (int)(Math.min(speed * 20, 100));
            g2d.setColor(new Color(255, 255, 200, glow));
            int glowSize = size + (int)(speed * 2);
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

    public Goal(int x, int y, int width, int height, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
    }

    public void draw(Graphics2D g2d) {
        // Goal frame
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect(x, y, width, height);

        // Goal net pattern
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.setStroke(new BasicStroke(1));
        for (int i = 0; i < height; i += 10) {
            g2d.drawLine(x, y + i, x + width, y + i);
        }
        for (int i = 0; i < width; i += 10) {
            g2d.drawLine(x + i, y, x + i, y + height);
        }
    }
}

class Particle {
    private double x, y;
    private Color color;
    private double vx, vy;
    private int life;
    private int maxLife;

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
        vx *= 0.98;
        vy *= 0.98;
        life--;
    }

    public void draw(Graphics2D g2d) {
        int alpha = (life * 255) / maxLife;
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        int size = (life * 5) / maxLife + 2;
        g2d.fillOval((int)x - size/2, (int)y - size/2, size, size);
    }

    public boolean isDead() { return life <= 0; }
}

class Star {
    private double x, y;
    private int size;
    private double twinkleSpeed;
    private int phase;

    public Star() {
        this.x = Math.random() * 800;
        this.y = Math.random() * 600;
        this.size = 1 + (int)(Math.random() * 2);
        this.twinkleSpeed = 0.02 + Math.random() * 0.03;
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