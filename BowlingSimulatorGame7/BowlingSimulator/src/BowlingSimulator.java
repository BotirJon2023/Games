import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.Timer;


public class BowlingSimulator extends JFrame {

    // Game constants
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int LANE_WIDTH = 400;
    private static final int LANE_HEIGHT = 600;
    private static final int PIN_RADIUS = 12;
    private static final int BALL_RADIUS = 20;
    private static final int NUM_PINS = 10;
    private static final int NUM_FRAMES = 10;

    // Game components
    private GamePanel gamePanel;
    private ScorePanel scorePanel;
    private ControlPanel controlPanel;

    // Game state
    private GameState gameState;
    private List<Player> players;
    private int currentPlayerIndex;
    private int currentFrame;
    private int currentRoll;
    private boolean isAnimating;

    // Animation
    private Timer animationTimer;
    private Ball ball;
    private List<Pin> pins;
    private double ballAngle;
    private double ballPower;
    private double ballSpin;

    /**
     * Main entry point
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new BowlingSimulator().setVisible(true);
        });
    }

    /**
     * Constructor - initializes the game
     */
    public BowlingSimulator() {
        setTitle("🎳 Bowling Simulator - Java Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setResizable(false);

        initializePlayers();
        initializeGame();
        setupUI();
    }

    /**
     * Initialize players
     */
    private void initializePlayers() {
        players = new ArrayList<>();
        String name = JOptionPane.showInputDialog(this,
                "Enter player name:", "Player Setup", JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.trim().isEmpty()) {
            name = "Player 1";
        }
        players.add(new Player(name));

        int addMore = JOptionPane.showConfirmDialog(this,
                "Add another player?", "Player Setup", JOptionPane.YES_NO_OPTION);
        while (addMore == JOptionPane.YES_OPTION && players.size() < 4) {
            name = JOptionPane.showInputDialog(this,
                    "Enter player " + (players.size() + 1) + " name:",
                    "Player Setup", JOptionPane.QUESTION_MESSAGE);
            if (name != null && !name.trim().isEmpty()) {
                players.add(new Player(name));
            }
            if (players.size() < 4) {
                addMore = JOptionPane.showConfirmDialog(this,
                        "Add another player?", "Player Setup", JOptionPane.YES_NO_OPTION);
            }
        }
    }

    /**
     * Initialize game state
     */
    private void initializeGame() {
        currentPlayerIndex = 0;
        currentFrame = 0;
        currentRoll = 0;
        isAnimating = false;
        gameState = GameState.AIMING;

        ballAngle = 0;
        ballPower = 50;
        ballSpin = 0;

        initializePins();
        initializeBall();
    }

    /**
     * Initialize pins in triangle formation
     */
    private void initializePins() {
        pins = new ArrayList<>();
        int startX = WINDOW_WIDTH / 2 - 60;
        int startY = 120;
        int row = 0;
        int pinIndex = 0;

        // Create pins in triangle formation (1-2-3-4)
        int[] pinsPerRow = {1, 2, 3, 4};
        for (int r = 0; r < pinsPerRow.length; r++) {
            int rowPins = pinsPerRow[r];
            int rowStartX = startX - (rowPins - 1) * 25;
            for (int p = 0; p < rowPins; p++) {
                int x = rowStartX + p * 50;
                int y = startY + r * 40;
                pins.add(new Pin(x, y, pinIndex++));
            }
        }
    }

    /**
     * Initialize ball at starting position
     */
    private void initializeBall() {
        ball = new Ball(WINDOW_WIDTH / 2, LANE_HEIGHT + 80);
    }

    /**
     * Setup UI components
     */
    private void setupUI() {
        setLayout(new BorderLayout());

        // Game panel (center)
        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        // Score panel (top)
        scorePanel = new ScorePanel();
        add(scorePanel, BorderLayout.NORTH);

        // Control panel (bottom)
        controlPanel = new ControlPanel();
        add(controlPanel, BorderLayout.SOUTH);

        // Key bindings
        setupKeyBindings();
    }

    /**
     * Setup keyboard controls
     */
    private void setupKeyBindings() {
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isAnimating) return;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        ballAngle = Math.max(-45, ballAngle - 2);
                        controlPanel.updateAngle(ballAngle);
                        break;
                    case KeyEvent.VK_RIGHT:
                        ballAngle = Math.min(45, ballAngle + 2);
                        controlPanel.updateAngle(ballAngle);
                        break;
                    case KeyEvent.VK_UP:
                        ballPower = Math.min(100, ballPower + 2);
                        controlPanel.updatePower(ballPower);
                        break;
                    case KeyEvent.VK_DOWN:
                        ballPower = Math.max(10, ballPower - 2);
                        controlPanel.updatePower(ballPower);
                        break;
                    case KeyEvent.VK_A:
                        ballSpin = Math.max(-50, ballSpin - 2);
                        controlPanel.updateSpin(ballSpin);
                        break;
                    case KeyEvent.VK_D:
                        ballSpin = Math.min(50, ballSpin + 2);
                        controlPanel.updateSpin(ballSpin);
                        break;
                    case KeyEvent.VK_SPACE:
                        throwBall();
                        break;
                    case KeyEvent.VK_R:
                        resetGame();
                        break;
                }
                gamePanel.repaint();
            }
        });
    }

    /**
     * Throw the ball
     */
    private void throwBall() {
        if (isAnimating || gameState != GameState.AIMING) return;

        isAnimating = true;
        gameState = GameState.ROLLING;

        // Calculate velocity based on angle and power
        double angleRad = Math.toRadians(ballAngle);
        double speed = ballPower / 10.0;
        ball.setVelocity(Math.sin(angleRad) * speed, -speed);
        ball.setSpin(ballSpin / 50.0);

        // Start animation
        animationTimer = new Timer();
        animationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateAnimation();
            }
        }, 0, 16); // ~60 FPS
    }

    /**
     * Update animation frame
     */
    private void updateAnimation() {
        // Update ball position
        ball.update();

        // Apply spin effect
        ball.applySpinEffect();

        // Check collisions with pins
        checkPinCollisions();

        // Update fallen pins
        for (Pin pin : pins) {
            pin.update();
        }

        // Check if ball is out of bounds or stopped
        if (ball.getY() < -50 || ball.getY() > WINDOW_HEIGHT + 50 ||
                ball.getX() < -50 || ball.getX() > WINDOW_WIDTH + 50 ||
                (ball.getVelocityMagnitude() < 0.1 && ball.getY() < LANE_HEIGHT)) {

            endRoll();
        }

        // Repaint
        SwingUtilities.invokeLater(() -> gamePanel.repaint());
    }

    /**
     * Check for collisions between ball and pins
     */
    private void checkPinCollisions() {
        for (Pin pin : pins) {
            if (!pin.isStanding()) continue;

            double dx = ball.getX() - pin.getX();
            double dy = ball.getY() - pin.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < BALL_RADIUS + PIN_RADIUS) {
                // Collision detected
                pin.knockDown(dx, dy, ball.getVelocityMagnitude());

                // Deflect ball slightly
                ball.deflect(dx, dy, 0.3);

                // Check for pin-to-pin collisions
                checkPinToPinCollisions(pin);
            }
        }
    }

    /**
     * Check for pin-to-pin collisions (domino effect)
     */
    private void checkPinToPinCollisions(Pin fallingPin) {
        for (Pin otherPin : pins) {
            if (otherPin == fallingPin || !otherPin.isStanding()) continue;

            double dx = fallingPin.getX() - otherPin.getX();
            double dy = fallingPin.getY() - otherPin.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < PIN_RADIUS * 3 && fallingPin.getFallSpeed() > 2) {
                otherPin.knockDown(-dx, -dy, fallingPin.getFallSpeed() * 0.7);
            }
        }
    }

    /**
     * End the current roll
     */
    private void endRoll() {
        if (animationTimer != null) {
            animationTimer.cancel();
            animationTimer = null;
        }

        // Count knocked down pins
        int knockedDown = (int) pins.stream().filter(pin -> !pin.isStanding()).count();

        Player currentPlayer = players.get(currentPlayerIndex);
        int pinsThisRoll = knockedDown - currentPlayer.getPinsDownThisFrame();
        currentPlayer.recordRoll(currentFrame, currentRoll, pinsThisRoll);
        currentPlayer.setPinsDownThisFrame(knockedDown);

        // Update score panel
        scorePanel.updateScores();

        // Determine next action
        SwingUtilities.invokeLater(() -> {
            determineNextAction(knockedDown);
        });
    }

    /**
     * Determine what happens next after a roll
     */
    private void determineNextAction(int knockedDown) {
        Player currentPlayer = players.get(currentPlayerIndex);
        boolean isLastFrame = (currentFrame == NUM_FRAMES - 1);

        if (isLastFrame) {
            handleLastFrame(knockedDown, currentPlayer);
        } else {
            handleNormalFrame(knockedDown, currentPlayer);
        }

        isAnimating = false;
        gameState = GameState.AIMING;
        gamePanel.repaint();
    }

    /**
     * Handle normal frame logic (frames 1-9)
     */
    private void handleNormalFrame(int knockedDown, Player currentPlayer) {
        if (knockedDown == NUM_PINS || currentRoll == 1) {
            // Strike or second roll - next player/frame
            nextTurn();
        } else {
            // First roll, not a strike - second roll
            currentRoll = 1;
            resetBall();
        }
    }

    /**
     * Handle 10th frame special rules
     */
    private void handleLastFrame(int knockedDown, Player currentPlayer) {
        int roll1 = currentPlayer.getFrameRoll(currentFrame, 0);
        int roll2 = currentPlayer.getFrameRoll(currentFrame, 1);

        if (currentRoll == 0) {
            // First roll of 10th frame
            if (knockedDown == NUM_PINS) {
                // Strike - reset pins, continue
                resetPins();
            }
            currentRoll = 1;
            resetBall();
        } else if (currentRoll == 1) {
            // Second roll of 10th frame
            if (roll1 == NUM_PINS) {
                // Had a strike on first roll
                if (knockedDown == NUM_PINS) {
                    // Another strike - reset pins
                    resetPins();
                }
                currentRoll = 2;
                resetBall();
            } else if (roll1 + (knockedDown - roll1) == NUM_PINS) {
                // Spare - reset pins
                resetPins();
                currentRoll = 2;
                resetBall();
            } else {
                // No spare, no bonus roll
                nextTurn();
            }
        } else {
            // Third roll completed
            nextTurn();
        }
    }

    /**
     * Move to next turn (next player or next frame)
     */
    private void nextTurn() {
        Player currentPlayer = players.get(currentPlayerIndex);
        currentPlayer.setPinsDownThisFrame(0);

        currentPlayerIndex++;
        if (currentPlayerIndex >= players.size()) {
            currentPlayerIndex = 0;
            currentFrame++;
        }

        currentRoll = 0;

        if (currentFrame >= NUM_FRAMES) {
            endGame();
        } else {
            resetPins();
            resetBall();
            scorePanel.updateCurrentPlayer();
        }
    }

    /**
     * Reset pins to standing position
     */
    private void resetPins() {
        for (Pin pin : pins) {
            pin.reset();
        }
    }

    /**
     * Reset ball to starting position
     */
    private void resetBall() {
        ball.reset(WINDOW_WIDTH / 2, LANE_HEIGHT + 80);
        ballAngle = 0;
        ballSpin = 0;
        controlPanel.updateAngle(0);
        controlPanel.updateSpin(0);
    }

    /**
     * End the game and show results
     */
    private void endGame() {
        gameState = GameState.GAME_OVER;

        // Find winner
        Player winner = players.get(0);
        for (Player p : players) {
            if (p.getTotalScore() > winner.getTotalScore()) {
                winner = p;
            }
        }

        StringBuilder results = new StringBuilder();
        results.append("🎳 GAME OVER! 🎳\n\n");
        results.append("Final Scores:\n");
        for (Player p : players) {
            results.append(p.getName()).append(": ").append(p.getTotalScore()).append("\n");
        }
        results.append("\n🏆 Winner: ").append(winner.getName()).append("! 🏆");

        JOptionPane.showMessageDialog(this, results.toString(),
                "Game Over", JOptionPane.INFORMATION_MESSAGE);

        int playAgain = JOptionPane.showConfirmDialog(this,
                "Play again?", "New Game", JOptionPane.YES_NO_OPTION);
        if (playAgain == JOptionPane.YES_OPTION) {
            resetGame();
        }
    }

    /**
     * Reset entire game
     */
    private void resetGame() {
        for (Player p : players) {
            p.reset();
        }
        currentPlayerIndex = 0;
        currentFrame = 0;
        currentRoll = 0;
        isAnimating = false;
        gameState = GameState.AIMING;

        resetPins();
        resetBall();
        ballPower = 50;
        controlPanel.updatePower(50);
        scorePanel.updateScores();
        scorePanel.updateCurrentPlayer();
        gamePanel.repaint();
    }

    // ==================== INNER CLASSES ====================

    /**
     * Enum for game states
     */
    private enum GameState {
        AIMING, ROLLING, GAME_OVER
    }

    /**
     * Ball class with physics
     */
    private class Ball {
        private double x, y;
        private double vx, vy;
        private double spin;
        private double rotation;
        private final double friction = 0.995;
        private final double spinDecay = 0.98;

        public Ball(double x, double y) {
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = 0;
            this.spin = 0;
            this.rotation = 0;
        }

        public void setVelocity(double vx, double vy) {
            this.vx = vx;
            this.vy = vy;
        }

        public void setSpin(double spin) {
            this.spin = spin;
        }

        public void update() {
            x += vx;
            y += vy;
            vx *= friction;
            vy *= friction;
            rotation += getVelocityMagnitude() * 0.1;
        }

        public void applySpinEffect() {
            // Spin affects horizontal movement more as ball slows
            double spinEffect = spin * (1 - getVelocityMagnitude() / 15);
            vx += spinEffect * 0.02;
            spin *= spinDecay;
        }

        public void deflect(double dx, double dy, double factor) {
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 0) {
                vx += (dx / dist) * factor;
                vy += (dy / dist) * factor;
            }
        }

        public void reset(double x, double y) {
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = 0;
            this.spin = 0;
            this.rotation = 0;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getRotation() { return rotation; }
        public double getVelocityMagnitude() {
            return Math.sqrt(vx * vx + vy * vy);
        }

        public void draw(Graphics2D g2d) {
            // Ball shadow
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillOval((int)(x - BALL_RADIUS + 3), (int)(y - BALL_RADIUS + 3),
                    BALL_RADIUS * 2, BALL_RADIUS * 2);

            // Ball gradient
            RadialGradientPaint gradient = new RadialGradientPaint(
                    new Point2D.Double(x - 5, y - 5),
                    BALL_RADIUS,
                    new float[]{0f, 1f},
                    new Color[]{new Color(50, 50, 200), new Color(20, 20, 100)}
            );
            g2d.setPaint(gradient);
            g2d.fillOval((int)(x - BALL_RADIUS), (int)(y - BALL_RADIUS),
                    BALL_RADIUS * 2, BALL_RADIUS * 2);

            // Ball shine
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.fillOval((int)(x - BALL_RADIUS + 5), (int)(y - BALL_RADIUS + 5), 8, 8);

            // Finger holes (rotate with ball)
            g2d.setColor(new Color(10, 10, 50));
            AffineTransform old = g2d.getTransform();
            g2d.rotate(rotation, x, y);
            g2d.fillOval((int)(x - 8), (int)(y - 12), 6, 6);
            g2d.fillOval((int)(x + 2), (int)(y - 12), 6, 6);
            g2d.fillOval((int)(x - 3), (int)(y - 4), 5, 5);
            g2d.setTransform(old);
        }
    }

    /**
     * Pin class with physics
     */
    private class Pin {
        private double x, y;
        private double originalX, originalY;
        private int index;
        private boolean standing;
        private double fallAngle;
        private double fallSpeed;
        private double vx, vy;
        private double rotation;

        public Pin(double x, double y, int index) {
            this.x = x;
            this.y = y;
            this.originalX = x;
            this.originalY = y;
            this.index = index;
            this.standing = true;
            this.fallAngle = 0;
            this.fallSpeed = 0;
            this.vx = 0;
            this.vy = 0;
            this.rotation = 0;
        }

        public void knockDown(double dx, double dy, double force) {
            standing = false;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 0) {
                vx = (dx / dist) * force * 0.5;
                vy = (dy / dist) * force * 0.5;
            }
            fallSpeed = force * 0.3;
            fallAngle = Math.atan2(dy, dx);
        }

        public void update() {
            if (!standing) {
                x += vx;
                y += vy;
                vx *= 0.95;
                vy *= 0.95;
                rotation += fallSpeed * 0.1;
                fallSpeed *= 0.95;
            }
        }

        public void reset() {
            x = originalX;
            y = originalY;
            standing = true;
            fallAngle = 0;
            fallSpeed = 0;
            vx = 0;
            vy = 0;
            rotation = 0;
        }

        public boolean isStanding() { return standing; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getFallSpeed() { return fallSpeed; }

        public void draw(Graphics2D g2d) {
            AffineTransform old = g2d.getTransform();

            if (!standing) {
                g2d.rotate(rotation, x, y);
            }

            // Pin shadow
            if (standing) {
                g2d.setColor(new Color(0, 0, 0, 30));
                g2d.fillOval((int)(x - PIN_RADIUS + 2), (int)(y - PIN_RADIUS + 2),
                        PIN_RADIUS * 2, PIN_RADIUS * 2);
            }

            // Pin body
            Color pinColor = standing ? Color.WHITE : new Color(220, 220, 220);
            g2d.setColor(pinColor);

            // Draw pin shape (simplified bowling pin)
            int[] xPoints = {(int)x - 8, (int)x - 10, (int)x - 6, (int)x + 6, (int)x + 10, (int)x + 8};
            int[] yPoints = {(int)y - 20, (int)y, (int)y + 15, (int)y + 15, (int)y, (int)y - 20};
            g2d.fillPolygon(xPoints, yPoints, 6);

            // Pin head
            g2d.fillOval((int)(x - 7), (int)(y - 28), 14, 14);

            // Pin neck
            g2d.fillRect((int)(x - 4), (int)(y - 20), 8, 6);

            // Red stripes
            g2d.setColor(new Color(200, 0, 0));
            g2d.fillRect((int)(x - 10), (int)(y - 5), 20, 3);
            g2d.fillRect((int)(x - 10), (int)(y + 2), 20, 3);

            // Pin outline
            g2d.setColor(Color.DARK_GRAY);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawPolygon(xPoints, yPoints, 6);
            g2d.drawOval((int)(x - 7), (int)(y - 28), 14, 14);

            g2d.setTransform(old);
        }
    }

    /**
     * Player class with scoring
     */
    private class Player {
        private String name;
        private int[][] rolls; // [frame][roll]
        private int[] frameScores;
        private int pinsDownThisFrame;

        public Player(String name) {
            this.name = name;
            this.rolls = new int[NUM_FRAMES][3];
            this.frameScores = new int[NUM_FRAMES];
            for (int i = 0; i < NUM_FRAMES; i++) {
                Arrays.fill(rolls[i], -1);
            }
            this.pinsDownThisFrame = 0;
        }

        public void recordRoll(int frame, int roll, int pins) {
            rolls[frame][roll] = pins;
            calculateScores();
        }

        public int getFrameRoll(int frame, int roll) {
            return rolls[frame][roll];
        }

        public void calculateScores() {
            int total = 0;
            for (int f = 0; f < NUM_FRAMES; f++) {
                if (rolls[f][0] == -1) break;

                if (f < NUM_FRAMES - 1) {
                    // Frames 1-9
                    if (rolls[f][0] == 10) {
                        // Strike
                        total += 10 + getStrikeBonus(f);
                    } else if (rolls[f][1] != -1 && rolls[f][0] + rolls[f][1] == 10) {
                        // Spare
                        total += 10 + getSpareBonus(f);
                    } else if (rolls[f][1] != -1) {
                        // Normal
                        total += rolls[f][0] + rolls[f][1];
                    } else {
                        // Incomplete frame
                        total += rolls[f][0];
                    }
                } else {
                    // 10th frame
                    int frameTotal = 0;
                    for (int r = 0; r < 3; r++) {
                        if (rolls[f][r] != -1) {
                            frameTotal += rolls[f][r];
                        }
                    }
                    total += frameTotal;
                }
                frameScores[f] = total;
            }
        }

        private int getStrikeBonus(int frame) {
            int bonus = 0;
            int nextFrame = frame + 1;

            if (nextFrame < NUM_FRAMES) {
                if (rolls[nextFrame][0] != -1) {
                    bonus += rolls[nextFrame][0];
                    if (rolls[nextFrame][0] == 10 && nextFrame < NUM_FRAMES - 1) {
                        // Another strike
                        int nextNextFrame = nextFrame + 1;
                        if (rolls[nextNextFrame][0] != -1) {
                            bonus += rolls[nextNextFrame][0];
                        }
                    } else if (rolls[nextFrame][1] != -1) {
                        bonus += rolls[nextFrame][1];
                    }
                }
            }
            return bonus;
        }

        private int getSpareBonus(int frame) {
            int nextFrame = frame + 1;
            if (nextFrame < NUM_FRAMES && rolls[nextFrame][0] != -1) {
                return rolls[nextFrame][0];
            }
            return 0;
        }

        public int getTotalScore() {
            return frameScores[NUM_FRAMES - 1];
        }

        public int getFrameScore(int frame) {
            return frameScores[frame];
        }

        public String getName() { return name; }

        public int getPinsDownThisFrame() { return pinsDownThisFrame; }
        public void setPinsDownThisFrame(int pins) { this.pinsDownThisFrame = pins; }

        public void reset() {
            for (int i = 0; i < NUM_FRAMES; i++) {
                Arrays.fill(rolls[i], -1);
                frameScores[i] = 0;
            }
            pinsDownThisFrame = 0;
        }

        public String getRollDisplay(int frame, int roll) {
            int value = rolls[frame][roll];
            if (value == -1) return "";
            if (value == 10) {
                if (roll == 0 && frame < NUM_FRAMES - 1) return "X";
                if (frame == NUM_FRAMES - 1) return "X";
            }
            if (roll == 1 && frame < NUM_FRAMES - 1) {
                if (rolls[frame][0] + value == 10) return "/";
            }
            if (frame == NUM_FRAMES - 1 && roll > 0) {
                int prevTotal = 0;
                for (int r = 0; r < roll; r++) {
                    if (rolls[frame][r] == 10) prevTotal = 0;
                    else prevTotal += rolls[frame][r];
                }
                if (prevTotal + value == 10 && value != 10) return "/";
            }
            if (value == 0) return "-";
            return String.valueOf(value);
        }
    }

    /**
     * Main game rendering panel
     */
    private class GamePanel extends JPanel {
        private final Color LANE_COLOR = new Color(205, 170, 125);
        private final Color GUTTER_COLOR = new Color(80, 80, 80);
        private final Color APPROACH_COLOR = new Color(180, 150, 100);

        public GamePanel() {
            setBackground(new Color(40, 40, 40));
            setPreferredSize(new Dimension(WINDOW_WIDTH, LANE_HEIGHT + 150));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            drawLane(g2d);
            drawPins(g2d);
            drawBall(g2d);
            drawAimingGuide(g2d);
            drawGameInfo(g2d);
        }

        private void drawLane(Graphics2D g2d) {
            int laneX = (WINDOW_WIDTH - LANE_WIDTH) / 2;

            // Gutters
            g2d.setColor(GUTTER_COLOR);
            g2d.fillRect(laneX - 30, 0, 30, LANE_HEIGHT);
            g2d.fillRect(laneX + LANE_WIDTH, 0, 30, LANE_HEIGHT);

            // Main lane
            g2d.setColor(LANE_COLOR);
            g2d.fillRect(laneX, 0, LANE_WIDTH, LANE_HEIGHT);

            // Lane boards (wood grain effect)
            g2d.setColor(new Color(185, 150, 105));
            for (int i = 0; i < LANE_WIDTH; i += 20) {
                g2d.drawLine(laneX + i, 0, laneX + i, LANE_HEIGHT);
            }

            // Approach area
            g2d.setColor(APPROACH_COLOR);
            g2d.fillRect(laneX, LANE_HEIGHT, LANE_WIDTH, 150);

            // Foul line
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(laneX, LANE_HEIGHT, laneX + LANE_WIDTH, LANE_HEIGHT);

            // Arrows on lane
            g2d.setColor(new Color(100, 80, 60));
            int arrowY = LANE_HEIGHT - 150;
            int[] arrowX = {laneX + 80, laneX + 140, laneX + 200,
                    laneX + 260, laneX + 320};
            for (int ax : arrowX) {
                drawArrow(g2d, ax, arrowY);
            }

            // Dots
            g2d.setColor(new Color(80, 60, 40));
            int dotY = LANE_HEIGHT - 50;
            for (int i = 0; i < 7; i++) {
                g2d.fillOval(laneX + 50 + i * 50, dotY, 8, 8);
            }

            // Pin deck
            g2d.setColor(new Color(150, 120, 90));
            g2d.fillRect(laneX, 50, LANE_WIDTH, 150);

            // Back wall
            GradientPaint backWall = new GradientPaint(
                    0, 0, new Color(60, 60, 60),
                    0, 50, new Color(30, 30, 30)
            );
            g2d.setPaint(backWall);
            g2d.fillRect(laneX - 30, 0, LANE_WIDTH + 60, 50);
        }

        private void drawArrow(Graphics2D g2d, int x, int y) {
            int[] xPoints = {x, x - 8, x + 8};
            int[] yPoints = {y - 15, y + 5, y + 5};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }

        private void drawPins(Graphics2D g2d) {
            for (Pin pin : pins) {
                pin.draw(g2d);
            }
        }

        private void drawBall(Graphics2D g2d) {
            ball.draw(g2d);
        }

        private void drawAimingGuide(Graphics2D g2d) {
            if (gameState != GameState.AIMING) return;

            // Draw aiming line
            g2d.setColor(new Color(255, 255, 0, 100));
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND, 0, new float[]{10, 10}, 0));

            double angleRad = Math.toRadians(ballAngle);
            double length = ballPower * 4;
            int endX = (int)(ball.getX() + Math.sin(angleRad) * length);
            int endY = (int)(ball.getY() - Math.cos(angleRad) * length);

            g2d.drawLine((int)ball.getX(), (int)ball.getY(), endX, endY);

            // Draw power indicator
            g2d.setColor(new Color(255, 100, 0, 150));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawArc((int)ball.getX() - 30, (int)ball.getY() - 30,
                    60, 60, 90 - (int)ballAngle - 20, (int)(ballPower * 0.4));
        }

        private void drawGameInfo(Graphics2D g2d) {
            // Current player and frame info
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));

            Player current = players.get(currentPlayerIndex);
            String info = String.format("Player: %s | Frame: %d | Roll: %d",
                    current.getName(), currentFrame + 1, currentRoll + 1);
            g2d.drawString(info, 20, LANE_HEIGHT + 130);

            // Instructions
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.setColor(new Color(200, 200, 200));
            g2d.drawString("← → : Aim | ↑ ↓ : Power | A D : Spin | SPACE : Throw | R : Reset",
                    20, LANE_HEIGHT + 145);

            // Standing pins count
            int standing = 0;
            for (Pin pin : pins) {
                if (pin.isStanding()) standing++;
            }
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("Pins Standing: " + standing, WINDOW_WIDTH - 150, LANE_HEIGHT + 130);
        }
    }

    /**
     * Score display panel
     */
    private class ScorePanel extends JPanel {
        private JLabel[] playerLabels;
        private JLabel[][] frameLabels;
        private JLabel[][] rollLabels;
        private JLabel currentPlayerLabel;

        public ScorePanel() {
            setBackground(new Color(20, 60, 20));
            setPreferredSize(new Dimension(WINDOW_WIDTH, 100));
            setLayout(new BorderLayout());

            setupScoreBoard();
        }

        private void setupScoreBoard() {
            JPanel scoreBoard = new JPanel(new GridBagLayout());
            scoreBoard.setBackground(new Color(20, 60, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 2, 2, 2);

            // Header
            gbc.gridy = 0;
            gbc.gridx = 0;
            JLabel nameHeader = createLabel("Player", Color.YELLOW, 14, true);
            nameHeader.setPreferredSize(new Dimension(80, 20));
            scoreBoard.add(nameHeader, gbc);

            for (int f = 0; f < NUM_FRAMES; f++) {
                gbc.gridx = f + 1;
                JLabel frameHeader = createLabel(String.valueOf(f + 1), Color.YELLOW, 12, true);
                frameHeader.setPreferredSize(new Dimension(f == 9 ? 80 : 60, 20));
                scoreBoard.add(frameHeader, gbc);
            }

            // Player rows
            playerLabels = new JLabel[players.size()];
            frameLabels = new JLabel[players.size()][NUM_FRAMES];
            rollLabels = new JLabel[players.size()][NUM_FRAMES * 3];

            for (int p = 0; p < players.size(); p++) {
                // Player name
                gbc.gridy = p * 2 + 1;
                gbc.gridx = 0;
                gbc.gridheight = 2;
                playerLabels[p] = createLabel(players.get(p).getName(), Color.WHITE, 12, true);
                playerLabels[p].setPreferredSize(new Dimension(80, 40));
                scoreBoard.add(playerLabels[p], gbc);
                gbc.gridheight = 1;

                // Frame cells
                for (int f = 0; f < NUM_FRAMES; f++) {
                    // Rolls row
                    gbc.gridy = p * 2 + 1;
                    gbc.gridx = f + 1;
                    JPanel rollPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
                    rollPanel.setBackground(new Color(30, 80, 30));
                    rollPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

                    int numRolls = (f == 9) ? 3 : 2;
                    for (int r = 0; r < numRolls; r++) {
                        int rollIndex = f * 3 + r;
                        rollLabels[p][rollIndex] = createLabel("", Color.WHITE, 10, false);
                        rollLabels[p][rollIndex].setPreferredSize(new Dimension(18, 15));
                        rollLabels[p][rollIndex].setBorder(
                                BorderFactory.createLineBorder(new Color(60, 100, 60)));
                        rollPanel.add(rollLabels[p][rollIndex]);
                    }
                    scoreBoard.add(rollPanel, gbc);

                    // Score row
                    gbc.gridy = p * 2 + 2;
                    frameLabels[p][f] = createLabel("", Color.CYAN, 12, true);
                    frameLabels[p][f].setPreferredSize(new Dimension(f == 9 ? 80 : 60, 20));
                    frameLabels[p][f].setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
                    frameLabels[p][f].setBackground(new Color(30, 80, 30));
                    frameLabels[p][f].setOpaque(true);
                    scoreBoard.add(frameLabels[p][f], gbc);
                }
            }

            add(scoreBoard, BorderLayout.CENTER);

            // Current player indicator
            currentPlayerLabel = createLabel("Current: " + players.get(0).getName(),
                    Color.YELLOW, 14, true);
            currentPlayerLabel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
            add(currentPlayerLabel, BorderLayout.SOUTH);
        }

        private JLabel createLabel(String text, Color color, int size, boolean bold) {
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            label.setForeground(color);
            label.setFont(new Font("Arial", bold ? Font.BOLD : Font.PLAIN, size));
            return label;
        }

        public void updateScores() {
            for (int p = 0; p < players.size(); p++) {
                Player player = players.get(p);
                player.calculateScores();

                for (int f = 0; f < NUM_FRAMES; f++) {
                    // Update roll displays
                    int numRolls = (f == 9) ? 3 : 2;
                    for (int r = 0; r < numRolls; r++) {
                        int rollIndex = f * 3 + r;
                        String rollDisplay = player.getRollDisplay(f, r);
                        rollLabels[p][rollIndex].setText(rollDisplay);

                        // Color strikes and spares
                        if (rollDisplay.equals("X")) {
                            rollLabels[p][rollIndex].setForeground(Color.RED);
                        } else if (rollDisplay.equals("/")) {
                            rollLabels[p][rollIndex].setForeground(Color.GREEN);
                        } else {
                            rollLabels[p][rollIndex].setForeground(Color.WHITE);
                        }
                    }

                    // Update frame score
                    if (player.getFrameRoll(f, 0) != -1) {
                        // Only show score if frame is complete enough
                        boolean showScore = false;
                        if (f < NUM_FRAMES - 1) {
                            if (player.getFrameRoll(f, 0) == 10) {
                                // Strike - need next two rolls
                                showScore = getNextTwoRollsComplete(player, f);
                            } else if (player.getFrameRoll(f, 1) != -1) {
                                if (player.getFrameRoll(f, 0) + player.getFrameRoll(f, 1) == 10) {
                                    // Spare - need next roll
                                    showScore = getNextRollComplete(player, f);
                                } else {
                                    showScore = true;
                                }
                            }
                        } else {
                            // 10th frame
                            int r1 = player.getFrameRoll(f, 0);
                            int r2 = player.getFrameRoll(f, 1);
                            if (r1 == 10 || (r2 != -1 && r1 + r2 >= 10)) {
                                showScore = player.getFrameRoll(f, 2) != -1;
                            } else {
                                showScore = r2 != -1;
                            }
                        }

                        if (showScore) {
                            frameLabels[p][f].setText(String.valueOf(player.getFrameScore(f)));
                        }
                    }
                }
            }
        }

        private boolean getNextTwoRollsComplete(Player player, int frame) {
            int nextFrame = frame + 1;
            if (nextFrame >= NUM_FRAMES) return false;

            if (player.getFrameRoll(nextFrame, 0) == -1) return false;
            if (player.getFrameRoll(nextFrame, 0) == 10) {
                // Next is also strike
                if (nextFrame + 1 < NUM_FRAMES) {
                    return player.getFrameRoll(nextFrame + 1, 0) != -1;
                } else {
                    return player.getFrameRoll(nextFrame, 1) != -1;
                }
            }
            return player.getFrameRoll(nextFrame, 1) != -1;
        }

        private boolean getNextRollComplete(Player player, int frame) {
            int nextFrame = frame + 1;
            if (nextFrame >= NUM_FRAMES) return false;
            return player.getFrameRoll(nextFrame, 0) != -1;
        }

        public void updateCurrentPlayer() {
            if (currentFrame < NUM_FRAMES) {
                currentPlayerLabel.setText("Current: " + players.get(currentPlayerIndex).getName() +
                        " - Frame " + (currentFrame + 1));
            } else {
                currentPlayerLabel.setText("Game Over!");
            }
        }
    }

    /**
     * Control panel for game settings
     */
    private class ControlPanel extends JPanel {
        private JSlider angleSlider;
        private JSlider powerSlider;
        private JSlider spinSlider;
        private JLabel angleLabel;
        private JLabel powerLabel;
        private JLabel spinLabel;
        private JButton throwButton;
        private JButton resetButton;

        public ControlPanel() {
            setBackground(new Color(50, 50, 50));
            setPreferredSize(new Dimension(WINDOW_WIDTH, 80));
            setLayout(new FlowLayout(FlowLayout.CENTER, 30, 10));

            setupControls();
        }

        private void setupControls() {
            // Angle control
            JPanel anglePanel = createSliderPanel("Angle", -45, 45, 0);
            angleSlider = (JSlider) anglePanel.getComponent(1);
            angleLabel = (JLabel) anglePanel.getComponent(2);
            angleSlider.addChangeListener(e -> {
                ballAngle = angleSlider.getValue();
                angleLabel.setText(String.valueOf((int)ballAngle) + "°");
                gamePanel.repaint();
            });
            add(anglePanel);

            // Power control
            JPanel powerPanel = createSliderPanel("Power", 10, 100, 50);
            powerSlider = (JSlider) powerPanel.getComponent(1);
            powerLabel = (JLabel) powerPanel.getComponent(2);
            powerSlider.addChangeListener(e -> {
                ballPower = powerSlider.getValue();
                powerLabel.setText(String.valueOf((int)ballPower) + "%");
                gamePanel.repaint();
            });
            add(powerPanel);

            // Spin control
            JPanel spinPanel = createSliderPanel("Spin", -50, 50, 0);
            spinSlider = (JSlider) spinPanel.getComponent(1);
            spinLabel = (JLabel) spinPanel.getComponent(2);
            spinSlider.addChangeListener(e -> {
                ballSpin = spinSlider.getValue();
                String spinDir = ballSpin < 0 ? "L" : (ballSpin > 0 ? "R" : "");
                spinLabel.setText(Math.abs((int)ballSpin) + spinDir);
                gamePanel.repaint();
            });
            add(spinPanel);

            // Throw button
            throwButton = new JButton("🎳 THROW");
            throwButton.setFont(new Font("Arial", Font.BOLD, 16));
            throwButton.setBackground(new Color(0, 150, 0));
            throwButton.setForeground(Color.WHITE);
            throwButton.setFocusPainted(false);
            throwButton.addActionListener(e -> {
                throwBall();
                gamePanel.requestFocus();
            });
            add(throwButton);

            // Reset button
            resetButton = new JButton("🔄 RESET");
            resetButton.setFont(new Font("Arial", Font.BOLD, 14));
            resetButton.setBackground(new Color(150, 100, 0));
            resetButton.setForeground(Color.WHITE);
            resetButton.setFocusPainted(false);
            resetButton.addActionListener(e -> {
                resetGame();
                gamePanel.requestFocus();
            });
            add(resetButton);
        }

        private JPanel createSliderPanel(String title, int min, int max, int initial) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(new Color(50, 50, 50));

            JLabel titleLabel = new JLabel(title);
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(titleLabel);

            JSlider slider = new JSlider(min, max, initial);
            slider.setBackground(new Color(50, 50, 50));
            slider.setForeground(Color.WHITE);
            slider.setPreferredSize(new Dimension(150, 30));
            panel.add(slider);

            String initialText = title.equals("Angle") ? initial + "°" :
                    (title.equals("Power") ? initial + "%" : String.valueOf(initial));
            JLabel valueLabel = new JLabel(initialText);
            valueLabel.setForeground(Color.CYAN);
            valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(valueLabel);

            return panel;
        }

        public void updateAngle(double angle) {
            angleSlider.setValue((int) angle);
            angleLabel.setText((int) angle + "°");
        }

        public void updatePower(double power) {
            powerSlider.setValue((int) power);
            powerLabel.setText((int) power + "%");
        }

        public void updateSpin(double spin) {
            spinSlider.setValue((int) spin);
            String spinDir = spin < 0 ? "L" : (spin > 0 ? "R" : "");
            spinLabel.setText(Math.abs((int) spin) + spinDir);
        }
    }
}