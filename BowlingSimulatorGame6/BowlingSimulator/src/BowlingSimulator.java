import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class BowlingSimulator extends JFrame {

    // Game Constants
    private static final int TOTAL_PINS = 10;
    private static final int MAX_FRAMES = 10;
    private static final int MAX_ROLLS = 21; // Maximum possible rolls in a game

    // Animation constants
    private static final int BALL_RADIUS = 20;
    private static final int PIN_RADIUS = 10;
    private static final int LANE_WIDTH = 800;
    private static final int LANE_HEIGHT = 600;
    private static final int FOUL_LINE = 100;

    // Game State
    private List<Frame> frames;
    private int currentFrame;
    private int currentRoll;
    private int totalScore;
    private boolean gameOver;
    private List<Integer> rollScores;

    // Animation State
    private boolean animationRunning;
    private int ballX, ballY;
    private int ballTargetX, ballTargetY;
    private double ballVelocityX, ballVelocityY;
    private List<Pin> pins;
    private Timer animationTimer;
    private List<Pin> fallenPins;
    private int pinsKnockedDown;
    private Random random;

    // UI Components
    private GamePanel gamePanel;
    private ScorePanel scorePanel;
    private ControlPanel controlPanel;
    private JLabel statusLabel;

    // Animation thread pool
    private ExecutorService animationExecutor;

    public BowlingSimulator() {
        super("Bowling Simulator Pro");
        initializeGame();
        setupUI();
        setupAnimation();
    }

    private void initializeGame() {
        frames = new ArrayList<>();
        for (int i = 0; i < MAX_FRAMES; i++) {
            frames.add(new Frame(i + 1));
        }

        rollScores = new ArrayList<>();
        currentFrame = 0;
        currentRoll = 0;
        totalScore = 0;
        gameOver = false;
        random = new Random();

        resetPins();
        animationExecutor = Executors.newSingleThreadExecutor();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);

        // Game Panel for animation
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(LANE_WIDTH, LANE_HEIGHT));
        gamePanel.setBackground(new Color(50, 50, 50));

        // Score Panel
        scorePanel = new ScorePanel();
        scorePanel.setPreferredSize(new Dimension(400, 600));
        scorePanel.setBackground(new Color(240, 240, 240));

        // Control Panel
        controlPanel = new ControlPanel();

        // Status Label
        statusLabel = new JLabel("Ready to bowl! Click 'Roll Ball' to start.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add components
        add(gamePanel, BorderLayout.CENTER);
        add(scorePanel, BorderLayout.EAST);
        add(controlPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setupAnimation() {
        animationTimer = new Timer(16, e -> { // ~60 FPS
            if (animationRunning) {
                updateBallPosition();
                checkCollisions();
                gamePanel.repaint();

                if (ballY <= 0) {
                    animationRunning = false;
                    completeRoll();
                }
            }
        });
        animationTimer.start();
    }

    private void resetPins() {
        pins = new ArrayList<>();
        fallenPins = new ArrayList<>();

        // Classic bowling pin arrangement (triangle)
        int[][] pinPositions = {
                {400, 100},  // Pin 1 (head pin)
                {380, 130}, {420, 130},  // Pins 2, 3
                {360, 160}, {400, 160}, {440, 160},  // Pins 4, 5, 6
                {340, 190}, {380, 190}, {420, 190}, {460, 190}  // Pins 7, 8, 9, 10
        };

        for (int i = 0; i < TOTAL_PINS; i++) {
            pins.add(new Pin(pinPositions[i][0], pinPositions[i][1], i + 1));
        }

        ballX = LANE_WIDTH / 2;
        ballY = LANE_HEIGHT - 50;
        pinsKnockedDown = 0;
    }

    private void updateBallPosition() {
        ballX += ballVelocityX;
        ballY += ballVelocityY;

        // Add some curve (hook) to the ball
        if (ballY < 300 && Math.abs(ballVelocityX) < 5) {
            ballVelocityX += (random.nextDouble() - 0.5) * 0.5;
        }
    }

    private void checkCollisions() {
        Iterator<Pin> iterator = pins.iterator();
        while (iterator.hasNext()) {
            Pin pin = iterator.next();
            if (!pin.isFallen) {
                double distance = Math.sqrt(
                        Math.pow(ballX - pin.x, 2) +
                                Math.pow(ballY - pin.y, 2)
                );

                if (distance < (BALL_RADIUS + PIN_RADIUS)) {
                    pin.isFallen = true;
                    pin.fallDirectionX = (pin.x - ballX) * 0.1;
                    pin.fallDirectionY = (pin.y - ballY) * 0.1;
                    fallenPins.add(pin);
                    iterator.remove();
                    pinsKnockedDown++;

                    // Chain reaction - knock down nearby pins
                    for (Pin nearbyPin : pins) {
                        if (!nearbyPin.isFallen) {
                            double pinDistance = Math.sqrt(
                                    Math.pow(pin.x - nearbyPin.x, 2) +
                                            Math.pow(pin.y - nearbyPin.y, 2)
                            );
                            if (pinDistance < PIN_RADIUS * 3) {
                                nearbyPin.isFallen = true;
                                nearbyPin.fallDirectionX = (nearbyPin.x - pin.x) * 0.05;
                                nearbyPin.fallDirectionY = (nearbyPin.y - pin.y) * 0.05;
                                fallenPins.add(nearbyPin);
                                pinsKnockedDown++;
                            }
                        }
                    }
                }
            }
        }
    }

    private void rollBall() {
        if (gameOver || animationRunning) return;

        if (currentFrame >= MAX_FRAMES) {
            gameOver = true;
            return;
        }

        // Random ball speed and direction with some user influence
        ballVelocityY = -(8 + random.nextDouble() * 4); // Speed
        ballVelocityX = (random.nextDouble() - 0.5) * 6; // Curve

        animationRunning = true;
        statusLabel.setText("Ball is rolling...");
    }

    private void completeRoll() {
        Frame frame = frames.get(currentFrame);

        // Record the roll
        rollScores.add(pinsKnockedDown);
        frame.addRoll(pinsKnockedDown);

        // Update game state
        if (currentFrame == 9) { // 10th frame special rules
            if (currentRoll == 0) {
                if (pinsKnockedDown == 10) { // Strike
                    currentRoll++;
                } else {
                    currentRoll++;
                    resetPins();
                }
            } else if (currentRoll == 1) {
                if (frame.getRoll(0) + pinsKnockedDown >= 10) { // Spare or double strike
                    currentRoll++;
                    resetPins();
                } else {
                    endFrame();
                }
            } else if (currentRoll == 2) {
                endFrame();
            }
        } else {
            if (pinsKnockedDown == 10 || currentRoll == 1) { // Strike or second roll
                endFrame();
            } else {
                currentRoll++;
                resetPins();
            }
        }

        updateScores();
        scorePanel.repaint();
        statusLabel.setText(getStatusMessage());

        // Reset for next roll if not game over
        if (!gameOver && !animationRunning) {
            resetPins();
        }
    }

    private void endFrame() {
        currentFrame++;
        currentRoll = 0;
        if (currentFrame >= MAX_FRAMES) {
            gameOver = true;
        }
    }

    private void updateScores() {
        totalScore = 0;
        int rollIndex = 0;

        for (int i = 0; i <= Math.min(currentFrame, MAX_FRAMES - 1); i++) {
            Frame frame = frames.get(i);

            if (i == 9) { // 10th frame
                frame.calculateTenthFrameScore(rollScores, rollIndex);
                totalScore += frame.score;
            } else {
                if (frame.isStrike()) {
                    if (rollIndex + 2 < rollScores.size()) {
                        frame.score = 10 + rollScores.get(rollIndex + 1) +
                                (rollIndex + 2 < rollScores.size() ? rollScores.get(rollIndex + 2) : 0);
                    }
                    rollIndex += 1;
                } else if (frame.isSpare()) {
                    if (rollIndex + 2 < rollScores.size()) {
                        frame.score = 10 + rollScores.get(rollIndex + 2);
                    }
                    rollIndex += 2;
                } else {
                    frame.score = frame.getRoll(0) + frame.getRoll(1);
                    rollIndex += 2;
                }
                totalScore += frame.score;
                frame.cumulativeScore = totalScore;
            }
        }
    }

    private String getStatusMessage() {
        if (gameOver) {
            return "Game Over! Final Score: " + totalScore;
        }

        Frame frame = frames.get(currentFrame);
        if (pinsKnockedDown == 10) {
            if (currentRoll == 0) {
                return "STRIKE!";
            } else {
                return "SPARE!";
            }
        }

        return String.format("Frame %d, Roll %d - Knocked down: %d pins",
                currentFrame + 1, currentRoll + 1, pinsKnockedDown);
    }

    private void resetGame() {
        initializeGame();
        gamePanel.repaint();
        scorePanel.repaint();
        statusLabel.setText("New game started! Click 'Roll Ball' to begin.");
    }

    // Inner Classes
    class Frame {
        int frameNumber;
        int[] rolls;
        int rollCount;
        int score;
        int cumulativeScore;

        Frame(int frameNumber) {
            this.frameNumber = frameNumber;
            this.rolls = new int[3]; // 3 rolls for 10th frame
            this.rollCount = 0;
            this.score = 0;
            this.cumulativeScore = 0;
        }

        void addRoll(int pins) {
            if (rollCount < rolls.length) {
                rolls[rollCount++] = pins;
            }
        }

        int getRoll(int index) {
            return index < rollCount ? rolls[index] : 0;
        }

        boolean isStrike() {
            return rollCount > 0 && rolls[0] == 10;
        }

        boolean isSpare() {
            return rollCount >= 2 && (rolls[0] + rolls[1] == 10);
        }

        void calculateTenthFrameScore(List<Integer> allRolls, int startIndex) {
            score = 0;
            for (int i = 0; i < Math.min(3, allRolls.size() - startIndex); i++) {
                score += allRolls.get(startIndex + i);
            }
            cumulativeScore = totalScore + score;
        }
    }

    class Pin {
        int x, y;
        int number;
        boolean isFallen;
        double fallDirectionX, fallDirectionY;
        double fallRotation;

        Pin(int x, int y, int number) {
            this.x = x;
            this.y = y;
            this.number = number;
            this.isFallen = false;
            this.fallDirectionX = 0;
            this.fallDirectionY = 0;
            this.fallRotation = 0;
        }

        void update() {
            if (isFallen) {
                x += fallDirectionX;
                y += fallDirectionY;
                fallDirectionY += 0.2; // Gravity
                fallRotation += 0.1;
            }
        }

        void draw(Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            if (isFallen) {
                g2d.setColor(new Color(139, 69, 19)); // Brown for fallen pins
                g2d.fillOval(x - PIN_RADIUS, y - PIN_RADIUS,
                        PIN_RADIUS * 2, PIN_RADIUS * 2);
                g2d.setColor(Color.BLACK);
                g2d.drawString(String.valueOf(number), x - 4, y + 4);
            } else {
                g2d.setColor(Color.WHITE);
                g2d.fillOval(x - PIN_RADIUS, y - PIN_RADIUS,
                        PIN_RADIUS * 2, PIN_RADIUS * 2);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(x - PIN_RADIUS, y - PIN_RADIUS,
                        PIN_RADIUS * 2, PIN_RADIUS * 2);
                g2d.drawString(String.valueOf(number), x - 4, y + 4);
            }
        }
    }

    class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            // Draw lane
            drawLane(g2d);

            // Draw foul line
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(0, LANE_HEIGHT - FOUL_LINE, LANE_WIDTH, LANE_HEIGHT - FOUL_LINE);
            g2d.drawString("FOUL LINE", 10, LANE_HEIGHT - FOUL_LINE - 5);

            // Draw ball
            g2d.setColor(new Color(255, 200, 0)); // Gold ball
            g2d.fillOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS,
                    BALL_RADIUS * 2, BALL_RADIUS * 2);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS,
                    BALL_RADIUS * 2, BALL_RADIUS * 2);

            // Draw finger holes
            g2d.setColor(Color.BLACK);
            g2d.fillOval(ballX - 5, ballY - 5, 6, 6);
            g2d.fillOval(ballX + 2, ballY - 8, 4, 4);
            g2d.fillOval(ballX + 2, ballY + 3, 4, 4);

            // Draw pins
            for (Pin pin : pins) {
                pin.draw(g2d);
            }

            // Draw fallen pins
            for (Pin pin : fallenPins) {
                pin.update();
                pin.draw(g2d);
            }

            // Draw pin count
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Pins Standing: " + pins.size(), 650, 30);
            g2d.drawString("Pins Knocked: " + pinsKnockedDown, 650, 50);
        }

        private void drawLane(Graphics2D g2d) {
            // Lane background
            GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(100, 100, 100),
                    0, LANE_HEIGHT, new Color(50, 50, 50)
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, LANE_WIDTH, LANE_HEIGHT);

            // Lane boards
            g2d.setColor(new Color(139, 69, 19)); // Wood color
            for (int i = 0; i < LANE_WIDTH; i += 40) {
                g2d.setStroke(new BasicStroke(20));
                g2d.drawLine(i, 0, i, LANE_HEIGHT);
            }

            // Lane markings
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            // Arrows
            int[] arrowX = {200, 205, 195};
            int[] arrowY = {300, 320, 320};
            g2d.drawPolyline(arrowX, arrowY, 3);

            int[] arrowX2 = {600, 605, 595};
            g2d.drawPolyline(arrowX2, arrowY, 3);

            // Dots
            g2d.fillOval(300, 400, 6, 6);
            g2d.fillOval(500, 400, 6, 6);
        }
    }

    class ScorePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw scoreboard
            drawScoreboard(g2d);
        }

        private void drawScoreboard(Graphics2D g2d) {
            int cellWidth = 40;
            int cellHeight = 30;
            int startX = 20;
            int startY = 50;

            // Draw header
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.setColor(Color.BLUE);
            g2d.drawString("BOWLING SCOREBOARD", 100, 30);

            // Draw frame numbers
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            for (int i = 0; i < MAX_FRAMES; i++) {
                g2d.setColor(i == currentFrame && !gameOver ? Color.RED : Color.BLACK);
                g2d.drawString(String.valueOf(i + 1),
                        startX + i * cellWidth * 3 + cellWidth,
                        startY - 20);
            }

            // Draw rolls and scores
            for (int i = 0; i < MAX_FRAMES; i++) {
                Frame frame = frames.get(i);
                int frameX = startX + i * cellWidth * 3;

                // Draw roll boxes
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.fillRect(frameX, startY, cellWidth * 2, cellHeight);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(frameX, startY, cellWidth * 2, cellHeight);

                // Draw rolls
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                if (i == 9) { // 10th frame has 3 boxes
                    g2d.drawRect(frameX + cellWidth * 2, startY, cellWidth, cellHeight);

                    // Draw roll marks
                    for (int r = 0; r < 3; r++) {
                        int roll = frame.getRoll(r);
                        String mark = getMark(i, r, roll, frame);
                        g2d.drawString(mark,
                                frameX + r * cellWidth + 15,
                                startY + 20);
                    }
                } else {
                    // Draw divider
                    g2d.drawLine(frameX + cellWidth, startY,
                            frameX + cellWidth, startY + cellHeight);

                    // Draw roll marks
                    for (int r = 0; r < 2; r++) {
                        int roll = frame.getRoll(r);
                        String mark = getMark(i, r, roll, frame);
                        g2d.drawString(mark,
                                frameX + r * cellWidth + 15,
                                startY + 20);
                    }
                }

                // Draw frame score
                if (frame.score > 0 || (i < currentFrame && frame.score == 0)) {
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.BOLD, 14));
                    g2d.drawString(String.valueOf(frame.cumulativeScore),
                            frameX + 10, startY + 70);
                }
            }

            // Draw total score
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.drawString("TOTAL: " + totalScore, 150, 400);

            // Draw game info
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            g2d.drawString("Current Frame: " + (currentFrame + 1), 20, 450);
            g2d.drawString("Current Roll: " + (currentRoll + 1), 20, 470);
            g2d.drawString("Game Status: " + (gameOver ? "COMPLETED" : "IN PROGRESS"), 20, 490);

            // Draw legend
            g2d.setColor(Color.BLUE);
            g2d.drawString("Legend:", 20, 530);
            g2d.setColor(Color.BLACK);
            g2d.drawString("X - Strike", 20, 550);
            g2d.drawString("/ - Spare", 20, 570);
            g2d.drawString("- - Miss", 20, 590);
        }

        private String getMark(int frameIndex, int rollIndex, int pins, Frame frame) {
            if (frameIndex == 9) { // 10th frame
                if (rollIndex == 0 && pins == 10) return "X";
                if (rollIndex == 1 && pins == 10) return "X";
                if (rollIndex == 2 && pins == 10) return "X";
                if (rollIndex == 1 && (frame.getRoll(0) + pins == 10)) return "/";
                if (rollIndex == 2 && (frame.getRoll(1) + pins == 10)) return "/";
            } else {
                if (rollIndex == 0 && pins == 10) return "X";
                if (rollIndex == 1 && (frame.getRoll(0) + pins == 10)) return "/";
            }
            return pins == 0 ? "-" : String.valueOf(pins);
        }
    }

    class ControlPanel extends JPanel {
        private JButton rollButton;
        private JButton resetButton;
        private JButton autoPlayButton;
        private JButton hintButton;
        private JSlider speedSlider;

        ControlPanel() {
            setLayout(new FlowLayout());

            rollButton = new JButton("Roll Ball");
            rollButton.setFont(new Font("Arial", Font.BOLD, 14));
            rollButton.setBackground(new Color(0, 150, 0));
            rollButton.setForeground(Color.WHITE);
            rollButton.addActionListener(e -> rollBall());

            resetButton = new JButton("New Game");
            resetButton.setFont(new Font("Arial", Font.BOLD, 14));
            resetButton.addActionListener(e -> resetGame());

            autoPlayButton = new JButton("Auto Play");
            autoPlayButton.setFont(new Font("Arial", Font.BOLD, 14));
            autoPlayButton.addActionListener(e -> autoPlay());

            hintButton = new JButton("Hint");
            hintButton.setFont(new Font("Arial", Font.BOLD, 14));
            hintButton.addActionListener(e -> showHint());

            speedSlider = new JSlider(1, 10, 5);
            speedSlider.setMajorTickSpacing(1);
            speedSlider.setPaintTicks(true);
            speedSlider.setPaintLabels(true);
            speedSlider.addChangeListener(e -> {
                int speed = speedSlider.getValue();
                animationTimer.setDelay(20 - speed * 2);
            });

            add(rollButton);
            add(resetButton);
            add(autoPlayButton);
            add(hintButton);
            add(new JLabel("Animation Speed:"));
            add(speedSlider);
        }

        private void autoPlay() {
            if (gameOver) return;

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    while (!gameOver && !isCancelled()) {
                        rollBall();
                        Thread.sleep(1000); // Wait for animation
                        while (animationRunning) {
                            Thread.sleep(100);
                        }
                        Thread.sleep(500); // Pause between rolls
                    }
                    return null;
                }
            };
            worker.execute();
        }

        private void showHint() {
            String hint = "";
            if (currentRoll == 0) {
                hint = "Aim for the pocket between the 1 and 3 pins for a strike!";
            } else if (currentRoll == 1 && pinsKnockedDown < 10) {
                hint = "Try to hit the remaining pins at an angle for a spare!";
            } else {
                hint = "Keep your arm straight and follow through!";
            }

            JOptionPane.showMessageDialog(BowlingSimulator.this,
                    hint, "Bowling Hint", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new BowlingSimulator();
        });
    }
}