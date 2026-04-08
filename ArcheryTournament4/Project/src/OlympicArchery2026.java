// OlympicArchery2026.java
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.awt.image.BufferedImage;

public class OlympicArchery2026 extends JFrame {
    private GamePanel gamePanel;

    public OlympicArchery2026() {
        setTitle("Olympic Archery 2026 - Paris Games");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OlympicArchery2026());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private enum GameState { MENU, PLAYING, GAME_OVER, REPLAY }
    private GameState state = GameState.MENU;

    // Game objects
    private OlympicTarget target;
    private Arrow arrow;
    private OlympicAthlete currentAthlete;
    private OlympicAthlete player1, player2, computer;
    private boolean isComputerMode;
    private int currentRound;
    private int maxRounds = 3;
    private List<ShotRecord> shotRecords = new ArrayList<>();

    // Animation states
    private Timer gameTimer;
    private Timer arrowAnimationTimer;
    private Timer celebrationTimer;
    private double arrowPower;
    private double arrowAngle;
    private boolean isDrawingArrow;
    private boolean isAnimating;
    private Point2D arrowStartPos;
    private Point2D arrowCurrentPos;
    private int animationDuration = 40;
    private int animationFrame = 0;
    private double currentScore = 0;
    private boolean showCelebration = false;
    private String celebrationText = "";

    // Olympic rings animation
    private float ringRotation = 0;
    private List<Firework> fireworks = new ArrayList<>();
    private List<Confetti> confetti = new ArrayList<>();

    // Background elements
    private List<OlympicTorch> torches = new ArrayList<>();
    private List<Flag> flags = new ArrayList<>();
    private StadiumLight[] lights = new StadiumLight[8];
    private float timeOfDay = 0;

    // UI Components
    private JButton onePlayerButton, twoPlayerButton, restartButton, menuButton;
    private JLabel statusLabel, scoreLabel1, scoreLabel2, roundLabel, medalLabel;
    private JProgressBar powerBar;

    // Colors
    private Color olympicBlue = new Color(0, 82, 165);
    private Color olympicYellow = new Color(244, 185, 66);
    private Color olympicBlack = new Color(0, 0, 0);
    private Color olympicGreen = new Color(0, 129, 66);
    private Color olympicRed = new Color(228, 0, 43);

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        setLayout(null);

        initializeGame();
        initializeUI();
        initializeEffects();

        gameTimer = new Timer(30, this);
        gameTimer.start();

        // Create torches
        for (int i = 0; i < 6; i++) {
            torches.add(new OlympicTorch(100 + i * 200, 750));
        }

        // Create flags
        String[] countries = {"USA", "CHN", "JPN", "FRA", "GBR", "GER", "AUS", "ITA"};
        for (int i = 0; i < countries.length; i++) {
            flags.add(new Flag(50 + i * 120, 100, countries[i]));
        }

        // Create stadium lights
        for (int i = 0; i < lights.length; i++) {
            lights[i] = new StadiumLight(100 + i * 150, 50);
        }
    }

    private void initializeEffects() {
        // Initialize fireworks
        for (int i = 0; i < 20; i++) {
            fireworks.add(new Firework());
        }
    }

    private void initializeUI() {
        // Styled buttons
        onePlayerButton = createStyledButton("VS COMPUTER", 500, 500);
        onePlayerButton.addActionListener(e -> startGame(true));

        twoPlayerButton = createStyledButton("2 PLAYERS", 500, 580);
        twoPlayerButton.addActionListener(e -> startGame(false));

        restartButton = createStyledButton("RESTART", 50, 800);
        restartButton.addActionListener(e -> restartGame());
        restartButton.setVisible(false);

        menuButton = createStyledButton("MENU", 200, 800);
        menuButton.addActionListener(e -> showMenu());
        menuButton.setVisible(false);

        // Labels with Olympic styling
        statusLabel = new JLabel("");
        statusLabel.setBounds(50, 200, 400, 40);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 20));
        statusLabel.setForeground(Color.WHITE);

        scoreLabel1 = new JLabel("");
        scoreLabel1.setBounds(50, 250, 350, 30);
        scoreLabel1.setFont(new Font("Arial", Font.BOLD, 16));
        scoreLabel1.setForeground(olympicBlue);

        scoreLabel2 = new JLabel("");
        scoreLabel2.setBounds(50, 290, 350, 30);
        scoreLabel2.setFont(new Font("Arial", Font.BOLD, 16));
        scoreLabel2.setForeground(olympicRed);

        roundLabel = new JLabel("");
        roundLabel.setBounds(50, 330, 350, 30);
        roundLabel.setFont(new Font("Arial", Font.BOLD, 14));
        roundLabel.setForeground(Color.WHITE);

        medalLabel = new JLabel("");
        medalLabel.setBounds(50, 370, 350, 30);
        medalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        medalLabel.setForeground(new Color(255, 215, 0));

        // Power bar
        powerBar = new JProgressBar(0, 200);
        powerBar.setBounds(50, 450, 300, 25);
        powerBar.setStringPainted(true);
        powerBar.setForeground(new Color(255, 69, 0));
        powerBar.setBackground(Color.DARK_GRAY);
        powerBar.setVisible(false);

        add(onePlayerButton);
        add(twoPlayerButton);
        add(restartButton);
        add(menuButton);
        add(statusLabel);
        add(scoreLabel1);
        add(scoreLabel2);
        add(roundLabel);
        add(medalLabel);
        add(powerBar);
    }

    private JButton createStyledButton(String text, int x, int y) {
        JButton button = new JButton(text);
        button.setBounds(x, y, 250, 60);
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setBackground(new Color(0, 82, 165));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(0, 102, 185));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(0, 82, 165));
            }
        });

        return button;
    }

    private void initializeGame() {
        target = new OlympicTarget(1100, 400);
        player1 = new OlympicAthlete("Michael", "USA", olympicBlue, "🇺🇸");
        player2 = new OlympicAthlete("Emma", "GBR", olympicRed, "🇬🇧");
        computer = new OlympicAthlete("AI Archer", "JPN", olympicGreen, "🤖");
        arrow = new Arrow();
        arrowAngle = -Math.PI / 4;
        arrowPower = 0;
    }

    private void startGame(boolean vsComputer) {
        isComputerMode = vsComputer;
        currentAthlete = player1;
        currentRound = 1;
        player1.resetScore();
        player2.resetScore();
        computer.resetScore();
        shotRecords.clear();
        arrowPower = 0;
        arrowAngle = -Math.PI / 4;
        state = GameState.PLAYING;

        onePlayerButton.setVisible(false);
        twoPlayerButton.setVisible(false);
        restartButton.setVisible(true);
        menuButton.setVisible(true);
        powerBar.setVisible(true);

        updateUI();
        requestFocus();
    }

    private void restartGame() {
        if (isComputerMode) {
            startGame(true);
        } else {
            startGame(false);
        }
    }

    private void showMenu() {
        state = GameState.MENU;
        onePlayerButton.setVisible(true);
        twoPlayerButton.setVisible(true);
        restartButton.setVisible(false);
        menuButton.setVisible(false);
        powerBar.setVisible(false);
        statusLabel.setText("");
        scoreLabel1.setText("");
        scoreLabel2.setText("");
        roundLabel.setText("");
        medalLabel.setText("");
        repaint();
    }

    public void updateUI() {
        if (state == GameState.PLAYING) {
            statusLabel.setText("🏹 " + currentAthlete.name + " (" + currentAthlete.country + ") - AIM! 🏹");
            scoreLabel1.setText(player1.name + " (" + player1.country + "): " + player1.score + " pts");

            if (isComputerMode) {
                scoreLabel2.setText(computer.name + " (" + computer.country + "): " + computer.score + " pts");
                roundLabel.setText("🎯 ROUND " + currentRound + " of " + maxRounds + " 🎯");
            } else {
                scoreLabel2.setText(player2.name + " (" + player2.country + "): " + player2.score + " pts");
                roundLabel.setText("🎯 ROUND " + currentRound + " of " + maxRounds + " 🎯");
            }

            // Update medal prediction
            updateMedalPrediction();
        } else if (state == GameState.GAME_OVER) {
            determineWinner();
        }
    }

    private void updateMedalPrediction() {
        int total = isComputerMode ? player1.score + computer.score : player1.score + player2.score;
        if (total > 0) {
            double percentage = (double)currentAthlete.score / total * 100;
            if (percentage > 60) {
                medalLabel.setText("🥇 GOLD MEDAL PACE! 🥇");
            } else if (percentage > 40) {
                medalLabel.setText("🥈 SILVER MEDAL PACE! 🥈");
            } else if (percentage > 20) {
                medalLabel.setText("🥉 BRONZE MEDAL PACE! 🥉");
            } else {
                medalLabel.setText("🎯 KEEP PRACTICING! 🎯");
            }
        }
    }

    private void determineWinner() {
        String winner;
        String medalEmoji = "";

        if (isComputerMode) {
            if (player1.score > computer.score) {
                winner = player1.name + " from " + player1.country;
                medalEmoji = "🥇";
                celebrationText = "GOLD MEDAL WINNER!";
            } else if (computer.score > player1.score) {
                winner = computer.name + " from " + computer.country;
                medalEmoji = "🥇";
                celebrationText = "AI TAKES GOLD!";
            } else {
                winner = "It's a Tie!";
                medalEmoji = "🤝";
                celebrationText = "SHARED VICTORY!";
            }
        } else {
            if (player1.score > player2.score) {
                winner = player1.name + " from " + player1.country;
                medalEmoji = "🥇";
                celebrationText = "OLYMPIC CHAMPION!";
            } else if (player2.score > player1.score) {
                winner = player2.name + " from " + player2.country;
                medalEmoji = "🥇";
                celebrationText = "OLYMPIC CHAMPION!";
            } else {
                winner = "It's a Tie!";
                medalEmoji = "🤝";
                celebrationText = "TIE BREAKER NEEDED!";
            }
        }

        statusLabel.setText("🏆 GAME OVER - " + winner + " " + medalEmoji + " 🏆");

        // Start celebration effects
        showCelebration = true;
        for (int i = 0; i < 50; i++) {
            confetti.add(new Confetti());
        }

        if (celebrationTimer != null) celebrationTimer.stop();
        celebrationTimer = new Timer(3000, e -> {
            showCelebration = false;
            confetti.clear();
        });
        celebrationTimer.setRepeats(false);
        celebrationTimer.start();
    }

    private void shootArrow() {
        if (isAnimating) return;

        currentScore = target.calculateScore(arrowAngle, arrowPower);
        currentAthlete.addScore((int)currentScore);

        // Add shot record
        shotRecords.add(new ShotRecord(currentAthlete.name, currentRound, currentScore));

        // Start arrow animation
        isAnimating = true;
        animationFrame = 0;
        arrowStartPos = new Point2D.Double(200, 500);

        double endX = 1100 + (arrowPower * Math.cos(arrowAngle)) * 2.5;
        double endY = 400 + (arrowPower * Math.sin(arrowAngle)) * 2.5;
        arrowCurrentPos = arrowStartPos;

        arrowAnimationTimer = new Timer(16, e -> {
            animationFrame++;
            double t = Math.min(1.0, (double) animationFrame / animationDuration);
            // Easing function for smoother animation
            double easeOut = 1 - Math.pow(1 - t, 3);
            arrowCurrentPos = new Point2D.Double(
                    arrowStartPos.getX() + (endX - arrowStartPos.getX()) * easeOut,
                    arrowStartPos.getY() + (endY - arrowStartPos.getY()) * easeOut
            );

            if (animationFrame >= animationDuration) {
                arrowAnimationTimer.stop();
                isAnimating = false;

                // Show score popup
                showScoreAnimation((int)currentScore);

                // Switch players
                switchPlayer();

                // Check for computer turn
                if (isComputerMode && currentAthlete == computer && state == GameState.PLAYING) {
                    computerTurn();
                }
            }
            repaint();
        });
        arrowAnimationTimer.start();

        // Reset power
        arrowPower = 0;
        powerBar.setValue(0);
        repaint();
    }

    private void showScoreAnimation(int score) {
        JWindow popup = new JWindow();
        popup.setSize(300, 150);
        popup.setLocationRelativeTo(this);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradient background
                GradientPaint gp = new GradientPaint(0, 0, olympicBlue, 300, 150, olympicRed);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, 300, 150);

                // Score text
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 36));
                String scoreText = "+" + score + " POINTS!";
                FontMetrics fm = g2d.getFontMetrics();
                int width = fm.stringWidth(scoreText);
                g2d.drawString(scoreText, 150 - width/2, 80);
            }
        };

        popup.add(panel);
        popup.setOpacity(0.95f);
        popup.setVisible(true);

        Timer timer = new Timer(1500, e -> popup.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    private void switchPlayer() {
        if (isComputerMode) {
            if (currentAthlete == player1) {
                currentAthlete = computer;
            } else {
                currentAthlete = player1;
                currentRound++;
                if (currentRound > maxRounds) {
                    endGame();
                }
            }
        } else {
            if (currentAthlete == player1) {
                currentAthlete = player2;
            } else {
                currentAthlete = player1;
                currentRound++;
                if (currentRound > maxRounds) {
                    endGame();
                }
            }
        }
        updateUI();
    }

    private void computerTurn() {
        Timer timer = new Timer(800, e -> {
            // Smart AI: adjusts based on current score
            double targetScore = 100 - (computer.score - currentAthlete.score) * 2;
            targetScore = Math.max(50, Math.min(150, targetScore));

            arrowPower = 80 + Math.random() * 80;
            arrowAngle = -Math.PI / 3 + (Math.random() * Math.PI / 6);
            shootArrow();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void endGame() {
        state = GameState.GAME_OVER;
        updateUI();
        powerBar.setVisible(false);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw stadium background
        drawStadiumBackground(g2d);

        // Draw Olympic rings
        drawOlympicRings(g2d);

        // Draw flags
        for (Flag flag : flags) {
            flag.draw(g2d);
        }

        // Draw stadium lights
        for (StadiumLight light : lights) {
            light.draw(g2d);
        }

        // Draw torches
        for (OlympicTorch torch : torches) {
            torch.draw(g2d);
        }

        // Draw target
        target.draw(g2d);

        // Draw shot records on target
        drawShotRecords(g2d);

        // Draw athlete
        if (state == GameState.PLAYING && !isAnimating && !(isComputerMode && currentAthlete == computer)) {
            currentAthlete.draw(g2d, 200, 500, arrowAngle, arrowPower);
            drawBowAndArrow(g2d);
        } else if (state == GameState.PLAYING && currentAthlete != null) {
            currentAthlete.drawIdle(g2d, 200, 500);
        }

        // Draw animating arrow
        if (isAnimating && arrowCurrentPos != null) {
            drawFlyingArrow(g2d);
        }

        // Draw power meter with Olympic style
        if (state == GameState.PLAYING && !isAnimating && !(isComputerMode && currentAthlete == computer)) {
            powerBar.setValue((int)arrowPower);
        }

        // Draw fireworks
        for (Firework firework : fireworks) {
            firework.draw(g2d);
            firework.update();
        }

        // Draw confetti
        for (Confetti c : confetti) {
            c.draw(g2d);
            c.update();
        }

        // Draw celebration text
        if (showCelebration) {
            drawCelebration(g2d);
        }

        // Draw menu if in menu state
        if (state == GameState.MENU) {
            drawOlympicMenu(g2d);
        }
    }

    private void drawStadiumBackground(Graphics2D g2d) {
        // Sky gradient (time of day changes slowly)
        timeOfDay += 0.002;
        float skyR = 0.2f + (float)(Math.sin(timeOfDay) * 0.1f);
        float skyG = 0.3f + (float)(Math.sin(timeOfDay) * 0.1f);
        float skyB = 0.6f + (float)(Math.sin(timeOfDay) * 0.1f);

        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(skyR, skyG, skyB),
                0, 600, new Color(0.1f, 0.2f, 0.4f));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Stadium seats
        g2d.setColor(new Color(100, 100, 120));
        for (int i = 0; i < 10; i++) {
            int y = 700 + i * 15;
            g2d.fillRect(0, y, getWidth(), 10);
        }

        // Field
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(0, 650, getWidth(), 150);

        // Track
        g2d.setColor(new Color(160, 82, 45));
        g2d.fillRect(0, 640, getWidth(), 10);

        // Audience (simple representation)
        g2d.setColor(Color.DARK_GRAY);
        for (int i = 0; i < 100; i++) {
            int x = (int)(Math.random() * getWidth());
            int y = 600 + (int)(Math.random() * 50);
            g2d.fillOval(x, y, 4, 4);
        }
    }

    private void drawOlympicRings(Graphics2D g2d) {
        ringRotation += 0.01;
        int centerX = getWidth() / 2;
        int centerY = 150;
        int radius = 40;
        int[] offsetsX = {-50, 0, 50, -25, 25};
        int[] offsetsY = {0, 0, 0, 35, 35};
        Color[] colors = {olympicBlue, olympicYellow, olympicBlack, olympicGreen, olympicRed};

        g2d.setStroke(new BasicStroke(4));
        for (int i = 0; i < 5; i++) {
            g2d.setColor(colors[i]);
            g2d.drawOval(centerX + offsetsX[i] - radius, centerY + offsetsY[i] - radius, radius * 2, radius * 2);
        }
    }

    private void drawBowAndArrow(Graphics2D g2d) {
        int bowX = 180;
        int bowY = 500;

        double arrowEndX = bowX + arrowPower * Math.cos(arrowAngle);
        double arrowEndY = bowY + arrowPower * Math.sin(arrowAngle);

        // Draw bow
        g2d.setColor(new Color(139, 69, 19));
        g2d.setStroke(new BasicStroke(6));
        g2d.drawArc(bowX - 25, bowY - 40, 50, 80, 0, 180);

        // Bow string
        g2d.setColor(Color.WHITE);
        g2d.drawLine(bowX - 20, bowY - 35, (int)arrowEndX, (int)arrowEndY);
        g2d.drawLine(bowX + 20, bowY - 35, (int)arrowEndX, (int)arrowEndY);

        // Arrow
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(bowX, bowY, (int)arrowEndX, (int)arrowEndY);

        // Arrowhead
        g2d.setColor(Color.GRAY);
        double angle = Math.atan2(arrowEndY - bowY, arrowEndX - bowX);
        int arrowheadX = (int)(arrowEndX + 15 * Math.cos(angle));
        int arrowheadY = (int)(arrowEndY + 15 * Math.sin(angle));
        g2d.fillPolygon(
                new int[]{(int)arrowEndX, arrowheadX, (int)arrowEndX},
                new int[]{(int)arrowEndY, arrowheadY, (int)(arrowEndY + 5)},
                3
        );

        // Feathers
        g2d.setColor(Color.RED);
        int featherX = (int)(bowX + arrowPower * 0.6 * Math.cos(angle));
        int featherY = (int)(bowY + arrowPower * 0.6 * Math.sin(angle));
        g2d.drawLine(featherX, featherY, featherX - 10, featherY - 5);
        g2d.drawLine(featherX, featherY, featherX - 10, featherY + 5);
    }

    private void drawFlyingArrow(Graphics2D g2d) {
        if (arrowCurrentPos == null) return;

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine((int)arrowCurrentPos.getX(), (int)arrowCurrentPos.getY(),
                (int)(arrowCurrentPos.getX() + 20), (int)(arrowCurrentPos.getY() + 5));

        // Motion blur effect
        g2d.setColor(new Color(0, 0, 0, 50));
        for (int i = 1; i <= 3; i++) {
            g2d.drawLine((int)(arrowCurrentPos.getX() - i * 5), (int)(arrowCurrentPos.getY() - i * 2),
                    (int)(arrowCurrentPos.getX() + 20 - i * 5), (int)(arrowCurrentPos.getY() + 5 - i * 2));
        }
    }

    private void drawShotRecords(Graphics2D g2d) {
        int y = 550;
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        for (ShotRecord record : shotRecords) {
            g2d.setColor(Color.WHITE);
            g2d.drawString(record.player + ": " + (int)record.score + " pts", 1150, y);
            y += 20;
            if (y > 700) break;
        }
    }

    private void drawCelebration(Graphics2D g2d) {
        g2d.setColor(new Color(255, 215, 0, 200));
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String text = celebrationText;
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(text);
        g2d.drawString(text, getWidth()/2 - width/2, getHeight()/2);

        // Draw gold medal effect
        g2d.setColor(new Color(255, 215, 0, 100));
        for (int i = 0; i < 3; i++) {
            g2d.drawOval(getWidth()/2 - 100 - i*20, getHeight()/2 - 50 - i*10,
                    200 + i*40, 100 + i*20);
        }
    }

    private void drawOlympicMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Animated torch flame
        int flameHeight = 150 + (int)(Math.random() * 20);
        g2d.setColor(new Color(255, 100, 0, 200));
        g2d.fillOval(getWidth()/2 - 30, 100, 60, flameHeight);
        g2d.setColor(new Color(255, 200, 0, 200));
        g2d.fillOval(getWidth()/2 - 20, 120, 40, flameHeight - 30);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 64));
        String title = "OLYMPIC ARCHERY 2026";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, getWidth()/2 - titleWidth/2, 350);

        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        String subtitle = "Paris Games - Aim for Gold!";
        int subWidth = fm.stringWidth(subtitle);
        g2d.drawString(subtitle, getWidth()/2 - subWidth/2, 420);

        // Draw Olympic rings in menu
        drawOlympicRings(g2d);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameState.PLAYING) {
            repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (state != GameState.PLAYING) return;
        if (isAnimating) return;
        if (isComputerMode && currentAthlete == computer) return;

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            isDrawingArrow = true;
        } else if (e.getKeyCode() == KeyEvent.VK_UP && isDrawingArrow && arrowPower < 200) {
            arrowPower = Math.min(arrowPower + 8, 200);
            powerBar.setValue((int)arrowPower);
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN && isDrawingArrow && arrowPower > 0) {
            arrowPower = Math.max(arrowPower - 5, 0);
            powerBar.setValue((int)arrowPower);
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT && isDrawingArrow) {
            arrowAngle = Math.max(arrowAngle - 0.05, -Math.PI / 1.5);
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && isDrawingArrow) {
            arrowAngle = Math.min(arrowAngle + 0.05, -Math.PI / 8);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && isDrawingArrow && state == GameState.PLAYING) {
            isDrawingArrow = false;
            if (!isAnimating) {
                shootArrow();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner classes for game objects
    class OlympicTarget {
        int x, y;

        OlympicTarget(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g2d) {
            int[] radii = {90, 75, 60, 45, 30};
            Color[] colors = {Color.WHITE, Color.BLACK, olympicBlue, olympicRed, Color.YELLOW};

            for (int i = 0; i < radii.length; i++) {
                g2d.setColor(colors[i]);
                g2d.fillOval(x - radii[i], y - radii[i], radii[i] * 2, radii[i] * 2);
            }

            // Olympic rings on target
            g2d.setStroke(new BasicStroke(2));
            for (int i = 0; i < 5; i++) {
                g2d.setColor(colors[i % colors.length]);
                g2d.drawOval(x - 15, y - 15, 30, 30);
            }

            // Target stand with Olympic logo
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillRect(x - 15, y + 20, 30, 80);
            g2d.fillRect(x - 50, y + 90, 100, 15);
        }

        double calculateScore(double angle, double power) {
            double endX = 1100 + power * Math.cos(angle) * 2.5;
            double endY = 400 + power * Math.sin(angle) * 2.5;
            double distance = Math.sqrt(Math.pow(endX - x, 2) + Math.pow(endY - y, 2));

            if (distance < 30) return 100;
            if (distance < 45) return 80;
            if (distance < 60) return 60;
            if (distance < 75) return 40;
            if (distance < 90) return 20;
            return 0;
        }
    }

    class OlympicAthlete {
        String name;
        String country;
        Color color;
        String flag;
        int score;
        float animationOffset = 0;

        OlympicAthlete(String name, String country, Color color, String flag) {
            this.name = name;
            this.country = country;
            this.color = color;
            this.flag = flag;
            this.score = 0;
        }

        void draw(Graphics2D g2d, int x, int y, double angle, double power) {
            animationOffset += 0.1f;

            // Draw athlete body
            g2d.setColor(color);
            g2d.fillOval(x - 15, y - 40, 30, 40); // Head
            g2d.fillRect(x - 10, y - 20, 20, 40); // Body

            // Draw uniform number
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString("2026", x - 8, y);

            // Draw bow arm (animated)
            int armX = (int)(x + 20 * Math.cos(animationOffset));
            int armY = (int)(y - 20 * Math.sin(animationOffset));
            g2d.drawLine(x + 10, y - 10, armX, armY);

            // Draw flag
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 20));
            g2d.drawString(flag, x + 15, y - 30);

            // Draw name
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString(name, x - 20, y + 20);
        }

        void drawIdle(Graphics2D g2d, int x, int y) {
            animationOffset += 0.05f;
            int bounceY = (int)(Math.sin(animationOffset) * 3);

            g2d.setColor(color);
            g2d.fillOval(x - 15, y - 40 + bounceY, 30, 40);
            g2d.fillRect(x - 10, y - 20 + bounceY, 20, 40);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString("2026", x - 8, y + bounceY);

            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 20));
            g2d.drawString(flag, x + 15, y - 30 + bounceY);

            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString(name + " (READY)", x - 25, y + 20 + bounceY);
        }

        void addScore(int points) {
            score += points;
        }

        void resetScore() {
            score = 0;
        }
    }

    class Arrow {
        // Placeholder class
    }

    class ShotRecord {
        String player;
        int round;
        double score;

        ShotRecord(String player, int round, double score) {
            this.player = player;
            this.round = round;
            this.score = score;
        }
    }

    class OlympicTorch {
        int x, y;
        float flameHeight = 0;

        OlympicTorch(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g2d) {
            flameHeight = 30 + (float)(Math.random() * 20);

            // Torch handle
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillRect(x, y - 40, 10, 40);

            // Flame
            g2d.setColor(new Color(255, 100, 0));
            g2d.fillOval(x - 5, (int)(y - 40 - flameHeight), 20, (int)flameHeight);
            g2d.setColor(new Color(255, 200, 0));
            g2d.fillOval(x - 2, (int)(y - 35 - flameHeight * 0.7f), 14, (int)(flameHeight * 0.7f));
        }
    }

    class Flag {
        int x, y;
        String country;
        float waveOffset = 0;

        Flag(int x, int y, String country) {
            this.x = x;
            this.y = y;
            this.country = country;
        }

        void draw(Graphics2D g2d) {
            waveOffset += 0.1f;

            // Flag pole
            g2d.setColor(Color.GRAY);
            g2d.fillRect(x, y, 3, 80);

            // Flag (waving effect)
            g2d.setColor(Color.WHITE);
            for (int i = 0; i < 40; i++) {
                int waveX = x + 3 + i;
                int waveY = y + (int)(Math.sin(waveOffset + i * 0.2) * 3);
                g2d.fillRect(waveX, waveY, 1, 20);
            }

            // Country name
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.setColor(Color.WHITE);
            g2d.drawString(country, x, y + 90);
        }
    }

    class StadiumLight {
        int x, y;
        float intensity = 0;

        StadiumLight(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g2d) {
            intensity = 0.5f + (float)(Math.random() * 0.5f);

            // Light pole
            g2d.setColor(Color.GRAY);
            g2d.fillRect(x, y, 5, 100);

            // Light
            g2d.setColor(new Color(1, 1, 0.5f, intensity));
            g2d.fillOval(x - 10, y - 5, 25, 15);

            // Light beam
            g2d.setColor(new Color(1, 1, 0.5f, intensity * 0.3f));
            for (int i = 0; i < 3; i++) {
                g2d.fillOval(x - 20 + i * 20, y + 20, 30, 100);
            }
        }
    }

    class Firework {
        int x, y, color, life;

        Firework() {
            reset();
        }

        void reset() {
            x = (int)(Math.random() * 1400);
            y = (int)(100 + Math.random() * 300);
            color = (int)(Math.random() * 5);
            life = 100;
        }

        void update() {
            life--;
            if (life <= 0) {
                reset();
            }
        }

        void draw(Graphics2D g2d) {
            Color[] colors = {olympicBlue, olympicYellow, olympicBlack, olympicGreen, olympicRed};
            g2d.setColor(colors[color]);
            int size = 5 + (100 - life) / 20;
            g2d.fillOval(x, y, size, size);
        }
    }

    class Confetti {
        int x, y, vx, vy, color, life;

        Confetti() {
            x = (int)(Math.random() * 1400);
            y = (int)(Math.random() * 900);
            vx = (int)(Math.random() * 10 - 5);
            vy = (int)(Math.random() * 5 + 2);
            color = (int)(Math.random() * 5);
            life = 100 + (int)(Math.random() * 100);
        }

        void update() {
            x += vx;
            y += vy;
            life--;
        }

        void draw(Graphics2D g2d) {
            Color[] colors = {olympicBlue, olympicYellow, olympicBlack, olympicGreen, olympicRed};
            g2d.setColor(colors[color]);
            g2d.fillRect(x, y, 5, 5);
        }
    }
}