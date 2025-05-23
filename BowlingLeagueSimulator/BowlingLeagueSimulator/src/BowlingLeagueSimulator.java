import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class BowlingLeagueSimulator extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BowlingLeagueSimulator simulator = new BowlingLeagueSimulator();
            simulator.setVisible(true);
        });
    }

    public BowlingLeagueSimulator() {
        setTitle("Bowling League Simulator");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        add(new BowlingPanel());
    }
}

class BowlingPanel extends JPanel implements ActionListener {
    private Timer timer;
    private ArrayList<Player> players;
    private int currentPlayerIndex;
    private int currentFrame;
    private boolean isRolling;
    private Ball ball;
    private Pin[] pins;
    private int animationStep;
    private Random random;
    private JButton rollButton;
    private JLabel statusLabel;
    private JTextArea scoreDisplay;

    public BowlingPanel() {
        setLayout(new BorderLayout());
        initializeComponents();
        initializeGame();
        timer = new Timer(50, this);
        timer.start();
    }

    private void initializeComponents() {
        rollButton = new JButton("Roll Ball");
        rollButton.addActionListener(e -> rollBall());
        statusLabel = new JLabel("Welcome to Bowling League Simulator!");
        scoreDisplay = new JTextArea(10, 20);
        scoreDisplay.setEditable(false);
        JPanel controlPanel = new JPanel();
        controlPanel.add(rollButton);
        controlPanel.add(statusLabel);
        add(controlPanel, BorderLayout.NORTH);
        add(new JScrollPane(scoreDisplay), BorderLayout.EAST);
    }

    private void initializeGame() {
        players = new ArrayList<>();
        players.add(new Player("Alice"));
        players.add(new Player("Bob"));
        players.add(new Player("Charlie"));
        players.add(new Player("Diana"));
        currentPlayerIndex = 0;
        currentFrame = 1;
        isRolling = false;
        ball = new Ball();
        pins = new Pin[10];
        for (int i = 0; i < pins.length; i++) {
            pins[i] = new Pin(i);
        }
        random = new Random();
        animationStep = 0;
        updateScoreDisplay();
    }

    private void rollBall() {
        if (!isRolling && currentFrame <= 10) {
            isRolling = true;
            rollButton.setEnabled(false);
            ball.reset();
            animationStep = 0;
            statusLabel.setText(players.get(currentPlayerIndex).getName() + " is rolling...");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw lane
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect(200, 50, 400, 400);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(200, 50, 400, 400);

        // Draw pins
        for (Pin pin : pins) {
            pin.draw(g2d);
        }

        // Draw ball
        ball.draw(g2d);

        // Draw frame and player info
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Frame: " + currentFrame, 20, 30);
        g2d.drawString("Player: " + players.get(currentPlayerIndex).getName(), 20, 50);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isRolling) {
            animateRoll();
        }
        repaint();
    }

    private void animateRoll() {
        animationStep++;
        ball.move();

        if (animationStep > 40) {
            int pinsKnocked = simulateRoll();
            updatePins(pinsKnocked);
            updateScore(pinsKnocked);
            isRolling = false;
            rollButton.setEnabled(true);
            advanceGame();
        }
    }

    private void advanceGame() {
    }

    private int simulateRoll() {
        int pinsLeft = countStandingPins();
        int maxKnock = Math.min(pinsLeft, random.nextInt(11));
        return maxKnock;
    }

    private int countStandingPins() {
        int count = 0;
        for (Pin pin : pins) {
            if (pin.isStanding()) count++;
        }
        return count;
    }

    private void updatePins(int pinsKnocked) {
        int knocked = 0;
        for (Pin pin : pins) {
            if (pin.isStanding() && knocked < pinsKnocked) {
                pin.knockDown();
                knocked++;
            }
        }
    }

    private void updateScore(int pinsKnocked) {
        Player player = players.get(currentPlayerIndex);
        Frame frame = player.getFrames()[currentFrame - 1];
        if (frame.getRoll1() == -1) {
            frame.setRoll1(pinsKnocked);
            if (pinsKnocked == 10 && currentFrame < 10) {
                // Strike
                advancePlayer();
            }
        } else {
            frame.setRoll2(pinsKnocked);
            advancePlayer();
        }
        updateScoreDisplay();
    }

    private void advancePlayer() {
        currentPlayerIndex++;
        if (currentPlayerIndex >= players.size()) {
            currentPlayerIndex = 0;
            currentFrame++;
            resetPins();
        }
        if (currentFrame > 10) {
            endGame();
        } else {
            statusLabel.setText(players.get(currentPlayerIndex).getName() + "'s turn");
        }
    }

    private void resetPins() {
        for (Pin pin : pins) {
            pin.reset();
        }
    }

    private void endGame() {
        rollButton.setEnabled(false);
        statusLabel.setText("Game Over! Final Scores:");
        StringBuilder sb = new StringBuilder();
        for (Player player : players) {
            sb.append(player.getName()).append(": ").append(player.calculateTotalScore()).append("\n");
        }
        scoreDisplay.setText(sb.toString());
    }

    private void updateScoreDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("Scoreboard:\n\n");
        for (Player player : players) {
            sb.append(player.getName()).append(":\n");
            for (int i = 0; i < 10; i++) {
                Frame frame = player.getFrames()[i];
                sb.append("Frame ").append(i + 1).append(": ");
                if (frame.getRoll1() == -1) {
                    sb.append("-");
                } else if (frame.getRoll1() == 10) {
                    sb.append("X");
                } else {
                    sb.append(frame.getRoll1());
                    if (frame.getRoll2() != -1) {
                        if (frame.getRoll1() + frame.getRoll2() == 10) {
                            sb.append("/").append(frame.getRoll2());
                        } else {
                            sb.append(",").append(frame.getRoll2());
                        }
                    }
                }
                sb.append(" (").append(frame.calculateScore()).append(")\n");
            }
            sb.append("Total: ").append(player.calculateTotalScore()).append("\n\n");
        }
        scoreDisplay.setText(sb.toString());
    }
}

class Ball {
    private int x, y;
    private static final int SIZE = 20;

    public Ball() {
        reset();
    }

    public void reset() {
        x = 400;
        y = 430;
    }

    public void move() {
        y -= 5;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        g2d.fillOval(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
    }
}

class Pin {
    private int id;
    private boolean standing;
    private int x, y;
    private static final int SIZE = 15;

    public Pin(int id) {
        this.id = id;
        this.standing = true;
        setPosition();
    }

    private void setPosition() {
        int row = (int) Math.floor((-1 + Math.sqrt(1 + 8 * id)) / 2);
        int pinsInRow = row + 1;
        int firstPinInRow = (row * (row + 1)) / 2;
        int positionInRow = id - firstPinInRow;
        x = 400 - (pinsInRow - 1) * 20 + positionInRow * 40;
        y = 100 + row * 40;
    }

    public void knockDown() {
        standing = false;
    }

    public void reset() {
        standing = true;
    }

    public boolean isStanding() {
        return standing;
    }

    public void draw(Graphics2D g2d) {
        if (standing) {
            g2d.setColor(Color.WHITE);
            g2d.fillOval(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
        }
    }
}

class Frame {
    private int roll1, roll2;
    private int score;

    public Frame() {
        roll1 = -1;
        roll2 = -1;
        score = 0;
    }

    public int getRoll1() {
        return roll1;
    }

    public void setRoll1(int roll1) {
        this.roll1 = roll1;
    }

    public int getRoll2() {
        return roll2;
    }

    public void setRoll2(int roll2) {
        this.roll2 = roll2;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int calculateScore() {
        if (roll1 == -1) return 0;
        if (roll1 == 10) return 10; // Strike, bonus handled in Player
        if (roll2 == -1) return roll1;
        return roll1 + roll2;
    }
}

class Player {
    private String name;
    private Frame[] frames;

    public Player(String name) {
        this.name = name;
        frames = new Frame[10];
        for (int i = 0; i < 10; i++) {
            frames[i] = new Frame();
        }
    }

    public String getName() {
        return name;
    }

    public Frame[] getFrames() {
        return frames;
    }

    public int calculateTotalScore() {
        int total = 0;
        for (int i = 0; i < 10; i++) {
            Frame frame = frames[i];
            total += frame.calculateScore();
            if (frame.getRoll1() == 10 && i < 9) {
                // Strike bonus
                Frame next = frames[i + 1];
                total += next.getRoll1();
                if (next.getRoll2() != -1) {
                    total += next.getRoll2();
                } else if (i < 8) {
                    Frame nextNext = frames[i + 2];
                    total += nextNext.getRoll1();
                }
            } else if (frame.getRoll1() + frame.getRoll2() == 10 && i < 9) {
                // Spare bonus
                total += frames[i + 1].getRoll1();
            }
        }
        return total;
    }
}