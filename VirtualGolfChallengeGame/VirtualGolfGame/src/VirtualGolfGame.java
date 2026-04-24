import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class VirtualGolfGame extends JPanel implements ActionListener, KeyListener {
    // Game states
    private enum GameState { TITLE, PLAYER1_TURN, PLAYER2_TURN, COMPUTER_TURN, RESULT }
    private GameState state = GameState.TITLE;

    // Game mode
    private enum GameMode { PLAYER_VS_PLAYER, PLAYER_VS_COMPUTER }
    private GameMode gameMode = GameMode.PLAYER_VS_PLAYER;

    // Players
    private String[] playerNames = {"Player 1", "Player 2", "Computer"};
    private int[] scores = {0, 0, 0}; // Holes won
    private int[] currentHoleShots = {0, 0, 0};
    private int[] totalShots = {0, 0, 0};
    private int currentHole = 1;
    private int currentPlayer = 0;

    // Golf ball physics
    private double[] ballX = {100, 100, 100};
    private double[] ballY = {370, 370, 370};
    private double[] ballVX = {0, 0, 0};
    private double[] ballVY = {0, 0, 0};
    private boolean[] ballMoving = {false, false, false};
    private double holeX = 750;
    private double holeY = 365;
    private boolean[] ballInHole = {false, false, false};
    private int[] holeStrokes = {0, 0, 0};

    // Swing meter variables
    private double power = 0;
    private double accuracy = 0;
    private boolean powerIncreasing = true;
    private boolean accuracyIncreasing = true;
    private int swingPhase = 0; // 0: waiting, 1: power, 2: accuracy
    private boolean swingActive = false;

    // Wind
    private double windSpeed = 0;
    private double windAngle = 0;
    private Random random = new Random();

    // Animation
    private Timer timer;
    private int cloud1X = 50, cloud2X = 300, cloud3X = 550;
    private int seagull1X = 100, seagull1Y = 80;
    private int seagull2X = 400, seagull2Y = 120;
    private double waveOffset = 0;
    private String message = "Press 1 for 2-Player, 2 vs Computer";

    // Colors
    private Color sandColor = new Color(238, 214, 175);
    private Color waterColor = new Color(64, 164, 223);
    private Color deepWaterColor = new Color(30, 100, 180);

    public VirtualGolfGame() {
        setPreferredSize(new Dimension(900, 550));
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(16, this); // ~60 FPS
        timer.start();

        generateWind();
    }

    private void generateWind() {
        windSpeed = 5 + random.nextDouble() * 15;
        windAngle = (random.nextDouble() * 60 - 30) * Math.PI / 180;
    }

    private void startNewHole() {
        // Reset ball positions
        for (int i = 0; i < 3; i++) {
            ballX[i] = 100;
            ballY[i] = 370;
            ballVX[i] = 0;
            ballVY[i] = 0;
            ballMoving[i] = false;
            ballInHole[i] = false;
            holeStrokes[i] = 0;
            currentHoleShots[i] = 0;
        }

        // Randomize hole position slightly
        holeX = 750 + random.nextDouble() * 80 - 40;
        holeY = 365 + random.nextDouble() * 60 - 30;

        generateWind();

        currentPlayer = 0;

        if (gameMode == GameMode.PLAYER_VS_COMPUTER) {
            playerNames[0] = "You";
            playerNames[2] = "Computer";
        } else {
            playerNames[0] = "Player 1";
            playerNames[1] = "Player 2";
        }

        message = "Hole " + currentHole + "! Wind: " + String.format("%.1f", windSpeed) + " mph";
        state = GameState.PLAYER1_TURN;
        swingPhase = 0;
        swingActive = false;
    }

    private void takeShot() {
        if (ballInHole[currentPlayer]) return;

        // Calculate shot based on power and accuracy
        double powerFactor = power / 100.0;
        double accuracyFactor = 1.0 - Math.abs(accuracy - 50) / 50.0; // 0 to 1, 1 is perfect

        // Base velocity
        double baseSpeed = 25 * powerFactor;
        double angle = 45 * Math.PI / 180;

        // Accuracy affects angle deviation
        double angleDeviation = (1 - accuracyFactor) * 30 * Math.PI / 180;
        double actualAngle = angle + (random.nextDouble() - 0.5) * angleDeviation;

        double vx = baseSpeed * Math.cos(actualAngle);
        double vy = -baseSpeed * Math.sin(actualAngle);

        // Apply wind
        vx += windSpeed * Math.cos(windAngle) * 0.3;
        vy += windSpeed * Math.sin(windAngle) * 0.2;

        ballVX[currentPlayer] = vx;
        ballVY[currentPlayer] = vy;
        ballMoving[currentPlayer] = true;
        currentHoleShots[currentPlayer]++;
        holeStrokes[currentPlayer]++;

        swingPhase = 0;
        swingActive = false;

        message = playerNames[currentPlayer] + " shoots! Power: " + String.format("%.0f", power) +
                "%  Accuracy: " + String.format("%.0f", accuracy) + "%";
    }

    private void updatePhysics() {
        boolean anyMoving = false;

        for (int p = 0; p < 3; p++) {
            if (ballMoving[p] && !ballInHole[p]) {
                anyMoving = true;

                // Apply gravity and friction
                ballVY[p] += 0.5;
                ballVX[p] *= 0.99;
                ballVY[p] *= 0.99;

                ballX[p] += ballVX[p];
                ballY[p] += ballVY[p];

                // Ground collision
                if (ballY[p] >= 370) {
                    ballY[p] = 370;
                    ballVY[p] = -ballVY[p] * 0.5;

                    if (Math.abs(ballVX[p]) < 0.5 && Math.abs(ballVY[p]) < 0.5) {
                        ballMoving[p] = false;
                        ballVX[p] = 0;
                        ballVY[p] = 0;
                    }
                }

                // Check if ball is in hole
                double dx = ballX[p] - holeX;
                double dy = ballY[p] - holeY;
                if (Math.sqrt(dx*dx + dy*dy) < 12) {
                    ballInHole[p] = true;
                    ballMoving[p] = false;
                    message = "🎉 " + playerNames[p] + " scores! 🎉";
                }

                // Boundaries
                if (ballX[p] < 20) ballX[p] = 20;
                if (ballX[p] > 870) ballX[p] = 870;
            }
        }

        if (!anyMoving) {
            // Check if all players have finished the hole
            if (gameMode == GameMode.PLAYER_VS_PLAYER) {
                if (ballInHole[0] && ballInHole[1]) {
                    endHole();
                }
            } else {
                if (ballInHole[0] && ballInHole[2]) {
                    endHole();
                }
            }
        }
    }

    private void endHole() {
        int winner = -1;
        int minStrokes = Integer.MAX_VALUE;

        if (gameMode == GameMode.PLAYER_VS_PLAYER) {
            for (int i = 0; i < 2; i++) {
                if (holeStrokes[i] < minStrokes) {
                    minStrokes = holeStrokes[i];
                    winner = i;
                }
            }
            if (winner != -1 && holeStrokes[0] != holeStrokes[1]) {
                scores[winner]++;
                message = playerNames[winner] + " wins hole " + currentHole + "!";
            } else {
                message = "Hole " + currentHole + " is a tie!";
            }
        } else {
            for (int i = 0; i < 3; i += 2) {
                if (holeStrokes[i] < minStrokes) {
                    minStrokes = holeStrokes[i];
                    winner = i;
                }
            }
            if (winner != -1 && holeStrokes[0] != holeStrokes[2]) {
                scores[winner]++;
                message = playerNames[winner] + " wins hole " + currentHole + "!";
            } else {
                message = "Hole " + currentHole + " is a tie!";
            }
        }

        currentHole++;

        // Check match winner
        if (scores[0] >= 2 || (gameMode == GameMode.PLAYER_VS_PLAYER && scores[1] >= 2) ||
                (gameMode == GameMode.PLAYER_VS_COMPUTER && scores[2] >= 2)) {
            state = GameState.RESULT;
            int matchWinner = scores[0] >= 2 ? 0 : (gameMode == GameMode.PLAYER_VS_PLAYER ? 1 : 2);
            message = "🏆 " + playerNames[matchWinner] + " wins the match! 🏆\nPress R to play again";
            return;
        }

        if (currentHole > 5) {
            state = GameState.RESULT;
            message = "Match over! Press R to play again";
            return;
        }

        startNewHole();
    }

    private void computerTurn() {
        // Simple AI: aims for hole with some randomness
        double dx = holeX - ballX[2];
        double dy = holeY - ballY[2];
        double distance = Math.sqrt(dx*dx + dy*dy);

        // Calculate required power
        double targetPower = Math.min(100, Math.max(30, (distance / 700) * 100));
        targetPower += (random.nextDouble() - 0.5) * 20;
        power = Math.min(100, Math.max(0, targetPower));

        // Accuracy based on difficulty (pretty good)
        accuracy = 50 + (random.nextDouble() - 0.5) * 30;

        takeShot();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_R) {
            // Reset game
            scores = new int[]{0, 0, 0};
            currentHole = 1;
            startNewHole();
            repaint();
            return;
        }

        if (key == KeyEvent.VK_1 && state == GameState.TITLE) {
            gameMode = GameMode.PLAYER_VS_PLAYER;
            startNewHole();
            repaint();
            return;
        }

        if (key == KeyEvent.VK_2 && state == GameState.TITLE) {
            gameMode = GameMode.PLAYER_VS_COMPUTER;
            startNewHole();
            repaint();
            return;
        }

        if (state == GameState.RESULT) return;

        // Computer auto-turn
        if (gameMode == GameMode.PLAYER_VS_COMPUTER && state == GameState.COMPUTER_TURN) {
            computerTurn();
            state = GameState.PLAYER1_TURN;
            return;
        }

        if (key == KeyEvent.VK_SPACE) {
            if (state == GameState.PLAYER1_TURN || state == GameState.PLAYER2_TURN) {
                if (!swingActive) {
                    swingActive = true;
                    swingPhase = 1;
                    power = 0;
                    powerIncreasing = true;
                } else if (swingPhase == 1) {
                    swingPhase = 2;
                    accuracy = 0;
                    accuracyIncreasing = true;
                } else if (swingPhase == 2) {
                    swingActive = false;
                    takeShot();

                    // Advance turn
                    if (state == GameState.PLAYER1_TURN) {
                        if (gameMode == GameMode.PLAYER_VS_PLAYER) {
                            state = GameState.PLAYER2_TURN;
                        } else {
                            state = GameState.COMPUTER_TURN;
                        }
                    } else if (state == GameState.PLAYER2_TURN) {
                        state = GameState.PLAYER1_TURN;
                    }
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Update swing meter
        if (swingActive) {
            if (swingPhase == 1) {
                if (powerIncreasing) {
                    power += 2;
                    if (power >= 100) powerIncreasing = false;
                } else {
                    power -= 2;
                    if (power <= 0) powerIncreasing = true;
                }
            } else if (swingPhase == 2) {
                if (accuracyIncreasing) {
                    accuracy += 2;
                    if (accuracy >= 100) accuracyIncreasing = false;
                } else {
                    accuracy -= 2;
                    if (accuracy <= 0) accuracyIncreasing = true;
                }
            }
        }

        // Update animations
        cloud1X = (cloud1X + 1) % 900;
        cloud2X = (cloud2X + 2) % 900;
        cloud3X = (cloud3X + 1) % 900;
        seagull1X = (seagull1X + 2) % 900;
        seagull2X = (seagull2X + 3) % 900;
        seagull1Y = 80 + (int)(Math.sin(System.currentTimeMillis() / 500.0) * 5);
        seagull2Y = 120 + (int)(Math.sin(System.currentTimeMillis() / 400.0) * 4);
        waveOffset += 0.05;

        updatePhysics();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Sky gradient
        GradientPaint skyGrad = new GradientPaint(0, 0, new Color(135, 206, 235), 0, 300, new Color(240, 248, 255));
        g2d.setPaint(skyGrad);
        g2d.fillRect(0, 0, 900, 300);

        // Sun
        RadialGradientPaint sunGrad = new RadialGradientPaint(750, 60, 40, new float[]{0, 1},
                new Color[]{Color.YELLOW, new Color(255, 200, 100)});
        g2d.setPaint(sunGrad);
        g2d.fillOval(730, 40, 80, 80);

        // Sun rays
        g2d.setColor(new Color(255, 255, 200, 80));
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI * 2 / 12;
            int x1 = 770 + (int)(Math.cos(angle) * 55);
            int y1 = 80 + (int)(Math.sin(angle) * 55);
            int x2 = 770 + (int)(Math.cos(angle) * 80);
            int y2 = 80 + (int)(Math.sin(angle) * 80);
            g2d.drawLine(x1, y1, x2, y2);
        }

        // Clouds
        drawCloud(g2d, cloud1X, 60);
        drawCloud(g2d, cloud2X, 100);
        drawCloud(g2d, cloud3X, 40);

        // Seagulls
        drawSeagull(g2d, seagull1X, seagull1Y);
        drawSeagull(g2d, seagull2X, seagull2Y);

        // Ocean
        g2d.setColor(deepWaterColor);
        g2d.fillRect(0, 250, 900, 100);
        g2d.setColor(waterColor);
        for (int i = 0; i < 20; i++) {
            int y = 270 + (int)(Math.sin(waveOffset + i * 0.5) * 8);
            g2d.fillRect(i * 50, y, 50, 15);
        }

        // Beach
        g2d.setColor(sandColor);
        g2d.fillRect(0, 320, 900, 230);

        // Sand texture dots
        g2d.setColor(new Color(210, 180, 140));
        for (int i = 0; i < 200; i++) {
            g2d.fillOval(20 + i * 37 % 880, 340 + (i * 13 % 180), 2, 2);
        }

        // Golf course - fairway
        g2d.setColor(new Color(76, 153, 0));
        g2d.fillRoundRect(50, 350, 800, 80, 20, 20);

        // Fairway stripes
        g2d.setColor(new Color(56, 133, 0));
        for (int i = 0; i < 10; i++) {
            g2d.fillRect(70 + i * 85, 355, 30, 70);
        }

        // Green around hole
        RadialGradientPaint greenGrad = new RadialGradientPaint((int)holeX, (int)holeY, 45,
                new float[]{0, 1}, new Color[]{new Color(50, 205, 50), new Color(34, 139, 34)});
        g2d.setPaint(greenGrad);
        g2d.fillOval((int)holeX - 40, (int)holeY - 30, 80, 60);

        // Flag and hole
        g2d.setColor(Color.BLACK);
        g2d.fillOval((int)holeX - 6, (int)holeY - 6, 12, 12);
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int)holeX - 4, (int)holeY - 4, 8, 8);

        // Flag pole
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine((int)holeX, (int)holeY - 25, (int)holeX, (int)holeY - 5);

        // Flag
        Polygon flag = new Polygon();
        flag.addPoint((int)holeX, (int)holeY - 25);
        flag.addPoint((int)holeX + 20, (int)holeY - 20);
        flag.addPoint((int)holeX, (int)holeY - 15);
        g2d.setColor(Color.RED);
        g2d.fillPolygon(flag);

        // Draw golf balls
        for (int p = 0; p < 3; p++) {
            if (!ballInHole[p] && (p == 0 || p == 1 || (gameMode == GameMode.PLAYER_VS_COMPUTER && p == 2))) {
                // Shadow
                g2d.setColor(new Color(0, 0, 0, 50));
                g2d.fillOval((int)ballX[p] - 6, (int)ballY[p] + 2, 14, 6);

                // Ball
                RadialGradientPaint ballGrad = new RadialGradientPaint((int)ballX[p] - 3, (int)ballY[p] - 3, 10,
                        new float[]{0, 1}, new Color[]{Color.WHITE, Color.LIGHT_GRAY});
                g2d.setPaint(ballGrad);
                g2d.fillOval((int)ballX[p] - 7, (int)ballY[p] - 7, 14, 14);

                // Dimples
                g2d.setColor(new Color(200, 200, 200));
                g2d.fillOval((int)ballX[p] - 2, (int)ballY[p] - 4, 3, 3);
                g2d.fillOval((int)ballX[p] + 1, (int)ballY[p] + 1, 3, 3);
                g2d.fillOval((int)ballX[p] - 5, (int)ballY[p] + 0, 2, 2);
            }
        }

        // Draw player indicators
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        if (gameMode == GameMode.PLAYER_VS_PLAYER) {
            drawPlayerIndicator(g2d, 0, 30, 30);
            drawPlayerIndicator(g2d, 1, 30, 60);
        } else {
            drawPlayerIndicator(g2d, 0, 30, 30);
            drawPlayerIndicator(g2d, 2, 30, 60);
        }

        // Swing meter
        if (swingActive && (state == GameState.PLAYER1_TURN || state == GameState.PLAYER2_TURN)) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRoundRect(300, 480, 300, 50, 15, 15);

            if (swingPhase == 1) {
                g2d.setColor(Color.ORANGE);
                g2d.fillRect(305, 485, (int)(power * 2.9), 40);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString("POWER: " + (int)power + "%", 420, 515);
            } else if (swingPhase == 2) {
                g2d.setColor(Color.GREEN);
                g2d.fillRect(305, 485, (int)(accuracy * 2.9), 40);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString("ACCURACY: " + (int)accuracy + "%", 410, 515);
            }
        }

        // Wind indicator
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("💨 Wind: " + String.format("%.1f", windSpeed) + " mph", 750, 520);
        double windArrowX = 820 + Math.cos(windAngle) * 20;
        double windArrowY = 505 + Math.sin(windAngle) * 15;
        g2d.drawLine(820, 505, (int)windArrowX, (int)windArrowY);

        // Current turn indicator
        if (state == GameState.PLAYER1_TURN) {
            g2d.setColor(new Color(255, 215, 0, 100));
            g2d.fillRoundRect(20, 20, 150, 40, 10, 10);
            g2d.setColor(Color.BLACK);
            g2d.drawString("▶ " + playerNames[0] + "'s turn", 30, 48);
        } else if (state == GameState.PLAYER2_TURN) {
            g2d.setColor(new Color(255, 215, 0, 100));
            g2d.fillRoundRect(20, 50, 150, 40, 10, 10);
            g2d.setColor(Color.BLACK);
            g2d.drawString("▶ " + playerNames[1] + "'s turn", 30, 78);
        } else if (state == GameState.COMPUTER_TURN) {
            g2d.setColor(new Color(255, 215, 0, 100));
            g2d.fillRoundRect(20, 50, 150, 40, 10, 10);
            g2d.setColor(Color.BLACK);
            g2d.drawString("▶ Computer thinking...", 30, 78);
        }

        // Message
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRoundRect(200, 440, 500, 35, 15, 15);
        g2d.setColor(Color.WHITE);
        g2d.drawString(message, 220, 463);

        // Title screen
        if (state == GameState.TITLE) {
            g2d.setColor(new Color(0, 0, 0, 220));
            g2d.fillRoundRect(200, 150, 500, 250, 30, 30);
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 32));
            g2d.drawString("⛳ VIRTUAL GOLF ⛳", 300, 210);
            g2d.setFont(new Font("Arial", Font.PLAIN, 18));
            g2d.setColor(Color.WHITE);
            g2d.drawString("Press 1 - Two Players", 340, 280);
            g2d.drawString("Press 2 - vs Computer", 340, 320);
            g2d.drawString("Beautiful Seaside Course", 340, 360);
        }

        // Result screen
        if (state == GameState.RESULT) {
            g2d.setColor(new Color(0, 0, 0, 220));
            g2d.fillRoundRect(250, 180, 400, 180, 30, 30);
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 28));
            g2d.drawString(message.split("\n")[0], 320, 250);
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            g2d.setColor(Color.WHITE);
            g2d.drawString("Press R to play again", 360, 320);
        }

        // Hole score display
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Hole " + currentHole + "/5", 800, 30);

        // Scores
        if (gameMode == GameMode.PLAYER_VS_PLAYER) {
            g2d.drawString(scores[0] + " - " + scores[1], 800, 55);
        } else {
            g2d.drawString(scores[0] + " - " + scores[2], 800, 55);
        }
    }

    private void drawPlayerIndicator(Graphics2D g2d, int player, int x, int y) {
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.fillRoundRect(x, y, 130, 28, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawString(playerNames[player] + ": " + holeStrokes[player], x + 5, y + 20);
        if (ballInHole[player]) {
            g2d.drawString("✓ IN!", x + 85, y + 20);
        }
    }

    private void drawCloud(Graphics2D g2d, int x, int y) {
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillOval(x, y, 50, 35);
        g2d.fillOval(x + 25, y - 10, 45, 40);
        g2d.fillOval(x + 50, y, 45, 35);
    }

    private void drawSeagull(Graphics2D g2d, int x, int y) {
        g2d.setColor(Color.BLACK);
        g2d.drawArc(x, y, 15, 10, 0, 180);
        g2d.drawArc(x + 12, y, 15, 10, 0, 180);
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Virtual Golf Challenge - Seaside Edition");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new VirtualGolfGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}