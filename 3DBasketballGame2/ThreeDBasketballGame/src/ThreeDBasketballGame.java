import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class ThreeDBasketballGame extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 900;
    private static final int HEIGHT = 650;
    private static final int GROUND_Y = 500;
    private static final int HOOP_X = 650;
    private static final int HOOP_Y = 220;
    private static final int HOOP_WIDTH = 120;
    private static final int BACKBOARD_X = 640;
    private static final int BACKBOARD_Y = 160;
    private static final int BACKBOARD_HEIGHT = 140;

    // Ball properties
    private int ballX, ballY;
    private double ballVX, ballVY;
    private double ballRotation;
    private static final int BALL_SIZE = 24;
    private boolean isBallMoving = false;
    private boolean isBallShot = false;

    // Player properties
    private int player1X = 300;
    private int player2X = 500;
    private int computerX = 500;
    private int player1Score = 0;
    private int player2Score = 0;
    private int computerScore = 0;
    private int currentPlayer = 1; // 1 or 2

    // Game state
    private boolean isTwoPlayerMode = false;
    private int difficulty = 1; // 1=Easy, 2=Medium, 3=Hard
    private boolean isGameOver = false;
    private String gameMessage = "";
    private int messageTimer = 0;

    // Timer and random
    private Timer timer;
    private Random random = new Random();

    // Animation variables
    private int starRotation = 0;
    private int cloudOffset = 0;
    private int crowdAnimation = 0;
    private boolean[] crowdWave = new boolean[20];
    private double[] shotTrajectoryX = new double[30];
    private double[] shotTrajectoryY = new double[30];
    private int trajectoryIndex = 0;

    // Colors
    private Color courtColor = new Color(200, 120, 70);
    private Color courtLineColor = new Color(255, 200, 100);
    private Color hoopColor = new Color(200, 50, 50);
    private Color backboardColor = new Color(50, 50, 80);
    private Color netColor = new Color(220, 220, 220, 180);

    public ThreeDBasketballGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky blue
        setFocusable(true);
        addKeyListener(this);

        // Initialize ball position
        resetBall();

        // Initialize crowd wave
        for (int i = 0; i < crowdWave.length; i++) {
            crowdWave[i] = random.nextBoolean();
        }

        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    private void resetBall() {
        ballX = player1X + 20;
        ballY = GROUND_Y - BALL_SIZE;
        ballVX = 0;
        ballVY = 0;
        ballRotation = 0;
        isBallMoving = false;
        isBallShot = false;
        trajectoryIndex = 0;
        currentPlayer = 1;
    }

    private void shootBall(int shooterX, int shooterY, double power, double angleOffset) {
        if (isBallShot) return;

        isBallShot = true;
        isBallMoving = true;
        ballX = shooterX + 20;
        ballY = shooterY - 20;

        // Calculate trajectory with power and angle
        double angle = Math.toRadians(65 + angleOffset);
        double basePower = 8 + power;

        // Add some randomness for realism
        double randomFactor = 0.95 + (random.nextDouble() * 0.1);
        ballVX = basePower * Math.cos(angle) * randomFactor;
        ballVY = -basePower * Math.sin(angle) * randomFactor;

        // Store trajectory for trail effect
        trajectoryIndex = 0;
    }

    private void computerShoot() {
        if (isBallShot || !isBallMoving) return;

        // Computer aims at hoop with accuracy based on difficulty
        double dx = HOOP_X - computerX;
        double dy = HOOP_Y - (GROUND_Y - 50);
        double distance = Math.sqrt(dx * dx + dy * dy);

        double accuracy;
        switch (difficulty) {
            case 1: accuracy = 0.5 + random.nextDouble() * 0.3; break; // Easy
            case 2: accuracy = 0.7 + random.nextDouble() * 0.2; break; // Medium
            case 3: accuracy = 0.9 + random.nextDouble() * 0.1; break; // Hard
            default: accuracy = 0.7;
        }

        // Calculate shot with accuracy
        double targetX = HOOP_X + (1 - accuracy) * (random.nextDouble() - 0.5) * 80;
        double targetY = HOOP_Y + (1 - accuracy) * (random.nextDouble() - 0.5) * 40;

        double angleToTarget = Math.atan2(targetY - (GROUND_Y - 50), targetX - computerX);
        double power = 7 + random.nextDouble() * 2;

        ballX = computerX + 20;
        ballY = GROUND_Y - BALL_SIZE;
        ballVX = power * Math.cos(angleToTarget);
        ballVY = power * Math.sin(angleToTarget) - 2;
        isBallShot = true;
        isBallMoving = true;
        trajectoryIndex = 0;
    }

    private void checkScoring() {
        // Check if ball is near hoop
        double ballCenterX = ballX + BALL_SIZE/2;
        double ballCenterY = ballY + BALL_SIZE/2;

        // Check if ball goes through hoop (from above)
        if (ballY + BALL_SIZE > HOOP_Y && ballY < HOOP_Y + 20) {
            if (ballX + BALL_SIZE > HOOP_X && ballX < HOOP_X + HOOP_WIDTH) {
                // Score!
                if (currentPlayer == 1) {
                    player1Score++;
                    gameMessage = "Player 1 Scores!";
                } else if (isTwoPlayerMode && currentPlayer == 2) {
                    player2Score++;
                    gameMessage = "Player 2 Scores!";
                } else if (!isTwoPlayerMode) {
                    computerScore++;
                    gameMessage = "Computer Scores!";
                }
                messageTimer = 60;
                isBallShot = false;
                isBallMoving = false;
                resetBall();
                return;
            }
        }

        // Check if ball hits backboard (bounce)
        if (ballX + BALL_SIZE > BACKBOARD_X && ballX < BACKBOARD_X + 10) {
            if (ballY + BALL_SIZE > BACKBOARD_Y && ballY < BACKBOARD_Y + BACKBOARD_HEIGHT) {
                ballVX = -ballVX * 0.6;
                ballX = BACKBOARD_X - BALL_SIZE;
            }
        }

        // Check if ball hits rim
        if (ballY + BALL_SIZE > HOOP_Y && ballY < HOOP_Y + 15) {
            if (ballX + BALL_SIZE > HOOP_X - 5 && ballX < HOOP_X + 5) {
                ballVX = -ballVX * 0.5;
                ballX = HOOP_X - BALL_SIZE;
            }
            if (ballX + BALL_SIZE > HOOP_X + HOOP_WIDTH - 5 && ballX < HOOP_X + HOOP_WIDTH + 5) {
                ballVX = -ballVX * 0.5;
                ballX = HOOP_X + HOOP_WIDTH;
            }
        }

        // Check if ball goes out of bounds
        if (ballY > GROUND_Y + 50 || ballX < -50 || ballX > WIDTH + 50) {
            isBallShot = false;
            isBallMoving = false;
            if (currentPlayer == 1) {
                currentPlayer = isTwoPlayerMode ? 2 : 1;
            } else {
                currentPlayer = 1;
            }
            resetBall();
            gameMessage = "Miss!";
            messageTimer = 30;
        }
    }

    private void updateGame() {
        // Update ball physics
        if (isBallMoving && isBallShot) {
            // Gravity
            ballVY += 0.35;

            // Air resistance (slight)
            ballVX *= 0.999;

            // Update position
            ballX += ballVX;
            ballY += ballVY;

            // Ball rotation
            ballRotation += ballVX * 0.05;

            // Store trajectory
            if (trajectoryIndex < shotTrajectoryX.length) {
                shotTrajectoryX[trajectoryIndex] = ballX + BALL_SIZE/2;
                shotTrajectoryY[trajectoryIndex] = ballY + BALL_SIZE/2;
                trajectoryIndex++;
            }

            // Bounce off ground
            if (ballY + BALL_SIZE > GROUND_Y) {
                ballY = GROUND_Y - BALL_SIZE;
                ballVY = -ballVY * 0.4;
                ballVX *= 0.9;
                if (Math.abs(ballVY) < 1) {
                    ballVY = 0;
                    isBallMoving = false;
                    checkScoring();
                }
            }

            // Check for scoring
            checkScoring();
        }

        // Update computer AI
        if (!isTwoPlayerMode && !isBallShot && currentPlayer == 2) {
            // Computer moves toward ball position
            int targetX = (ballX > 0 && ballX < WIDTH) ? ballX : HOOP_X;
            if (computerX < targetX - 10) computerX += 2 + difficulty;
            else if (computerX > targetX + 10) computerX -= 2 + difficulty;

            // Computer shoots when in position
            if (Math.abs(computerX - HOOP_X) < 150 && !isBallShot) {
                computerShoot();
            }
        }

        // Update animations
        starRotation += 2;
        cloudOffset = (cloudOffset + 1) % 1000;
        crowdAnimation = (crowdAnimation + 1) % 120;
        if (crowdAnimation % 20 == 0) {
            for (int i = 0; i < crowdWave.length; i++) {
                if (random.nextDouble() < 0.3) {
                    crowdWave[i] = !crowdWave[i];
                }
            }
        }

        // Update message timer
        if (messageTimer > 0) messageTimer--;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw sky gradient
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, HEIGHT/2, new Color(200, 230, 255));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT/2);

        // Draw stadium elements
        drawStadium(g2d);

        // Draw city skyline (New York)
        drawSkyline(g2d);

        // Draw court
        drawCourt(g2d);

        // Draw hoop
        drawHoop(g2d);

        // Draw backboard
        drawBackboard(g2d);

        // Draw players
        if (isTwoPlayerMode) {
            drawPlayer(g2d, player1X, GROUND_Y - 50, "P1", Color.BLUE, 1);
            drawPlayer(g2d, player2X, GROUND_Y - 50, "P2", Color.RED, 2);
        } else {
            drawPlayer(g2d, player1X, GROUND_Y - 50, "You", Color.BLUE, 1);
            drawPlayer(g2d, computerX, GROUND_Y - 50, "CPU", Color.ORANGE, 2);
        }

        // Draw ball
        drawBall(g2d);

        // Draw shot trajectory
        drawTrajectory(g2d);

        // Draw score board
        drawScoreBoard(g2d);

        // Draw game message
        if (messageTimer > 0) {
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));
            String text = gameMessage;
            FontMetrics fm = g2d.getFontMetrics();
            int x = (WIDTH - fm.stringWidth(text)) / 2;
            int y = HEIGHT / 2 - 50;

            // Shadow
            g2d.setColor(Color.BLACK);
            g2d.drawString(text, x+2, y+2);
            g2d.setColor(Color.YELLOW);
            g2d.drawString(text, x, y);
        }

        // Draw controls info
        drawControls(g2d);

        // Draw difficulty indicator
        drawDifficulty(g2d);
    }

    private void drawStadium(Graphics2D g2d) {
        // Stadium background
        g2d.setColor(new Color(180, 180, 200));
        g2d.fillRect(0, 0, WIDTH, 40);
        g2d.setColor(new Color(160, 160, 180));
        g2d.fillRect(0, 40, WIDTH, 10);

        // Stadium lights
        for (int i = 100; i < WIDTH; i += 150) {
            g2d.setColor(new Color(255, 255, 200, 100));
            g2d.fillOval(i, 10, 30, 20);
            g2d.setColor(new Color(255, 255, 150, 50));
            g2d.fillOval(i + 10, 25, 10, 15);
        }

        // Crowd (simple representation)
        for (int i = 0; i < 40; i++) {
            int x = 20 + i * 25;
            int y = 30 + (int)(Math.sin(i * 0.5 + crowdAnimation * 0.02) * 5);
            Color crowdColor = crowdWave[i % crowdWave.length] ?
                    new Color(100, 150, 200) : new Color(80, 120, 170);
            g2d.setColor(crowdColor);
            g2d.fillOval(x, y, 15, 12);
            g2d.setColor(new Color(200, 180, 150));
            g2d.fillOval(x + 3, y - 3, 9, 8);
        }
    }

    private void drawSkyline(Graphics2D g2d) {
        // Simple NYC skyline
        int[] buildingHeights = {80, 150, 120, 200, 170, 250, 90, 130, 180, 100, 160};
        int[] buildingWidths = {30, 40, 35, 45, 40, 50, 30, 35, 40, 30, 45};
        int x = 20;

        g2d.setColor(new Color(50, 50, 70));
        for (int i = 0; i < buildingHeights.length; i++) {
            int height = buildingHeights[i];
            int width = buildingWidths[i];

            // Building body
            g2d.fillRect(x, 90 - height + 50, width, height);

            // Windows
            g2d.setColor(new Color(255, 255, 200, 120));
            for (int wy = 0; wy < height - 10; wy += 15) {
                for (int wx = 3; wx < width - 3; wx += 10) {
                    if (random.nextDouble() < 0.6) {
                        g2d.fillRect(x + wx, 90 - height + 60 + wy, 4, 6);
                    }
                }
            }
            g2d.setColor(new Color(50, 50, 70));
            x += width + 5;
        }

        // Empire State Building (iconic)
        g2d.setColor(new Color(60, 60, 80));
        int esbX = 300;
        g2d.fillRect(esbX, 90 - 220 + 50, 50, 220);
        g2d.fillRect(esbX + 15, 90 - 240 + 50, 20, 20);
        g2d.setColor(new Color(255, 200, 100, 80));
        g2d.fillRect(esbX + 22, 90 - 235 + 50, 6, 10);

        // More buildings
        for (int i = 0; i < 15; i++) {
            int bx = 500 + i * 25;
            int bh = 60 + random.nextInt(100);
            g2d.setColor(new Color(40, 40, 60));
            g2d.fillRect(bx, 90 - bh + 50, 20, bh);
            g2d.setColor(new Color(255, 255, 200, 80));
            for (int wy = 5; wy < bh - 5; wy += 12) {
                for (int wx = 3; wx < 17; wx += 8) {
                    if (random.nextDouble() < 0.5) {
                        g2d.fillRect(bx + wx, 90 - bh + 55 + wy, 4, 5);
                    }
                }
            }
        }
    }

    private void drawCourt(Graphics2D g2d) {
        // Main court
        g2d.setColor(courtColor);
        g2d.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        // Court lines
        g2d.setColor(courtLineColor);
        g2d.setStroke(new BasicStroke(2));

        // Center line
        g2d.drawLine(WIDTH/2, GROUND_Y, WIDTH/2, HEIGHT);

        // Three-point arc (partial)
        g2d.drawArc(50, GROUND_Y - 100, 200, 200, 0, 180);
        g2d.drawArc(WIDTH - 250, GROUND_Y - 100, 200, 200, 0, 180);

        // Free throw lines
        g2d.drawLine(80, GROUND_Y, 80, GROUND_Y + 100);
        g2d.drawLine(WIDTH - 80, GROUND_Y, WIDTH - 80, GROUND_Y + 100);

        // Key areas
        g2d.drawRect(50, GROUND_Y, 80, 120);
        g2d.drawRect(WIDTH - 130, GROUND_Y, 80, 120);

        // Court texture (wood grain)
        g2d.setColor(new Color(180, 110, 60, 30));
        for (int i = 0; i < 20; i++) {
            int y = GROUND_Y + 10 + i * 12;
            g2d.drawLine(20, y, WIDTH - 20, y + (i % 3 - 1) * 3);
        }
    }

    private void drawHoop(Graphics2D g2d) {
        // Net
        g2d.setColor(netColor);
        g2d.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 12; i++) {
            int x1 = HOOP_X + 5 + i * 10;
            int y1 = HOOP_Y;
            int x2 = HOOP_X + 20 + i * 7;
            int y2 = HOOP_Y + 40 + (int)(Math.sin(i * 0.5 + System.currentTimeMillis() * 0.003) * 5);
            g2d.drawLine(x1, y1, x2, y2);
        }
        for (int i = 0; i < 12; i++) {
            int x1 = HOOP_X + 5 + i * 10;
            int y1 = HOOP_Y;
            int x2 = HOOP_X + 15 + i * 7;
            int y2 = HOOP_Y + 45 + (int)(Math.cos(i * 0.3 + System.currentTimeMillis() * 0.002) * 5);
            g2d.drawLine(x1, y1, x2, y2);
        }

        // Rim
        g2d.setColor(hoopColor);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(HOOP_X, HOOP_Y, HOOP_X + HOOP_WIDTH, HOOP_Y);

        // Rim connectors
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(HOOP_X, HOOP_Y, HOOP_X, HOOP_Y + 10);
        g2d.drawLine(HOOP_X + HOOP_WIDTH, HOOP_Y, HOOP_X + HOOP_WIDTH, HOOP_Y + 10);
        g2d.drawLine(HOOP_X + HOOP_WIDTH/2, HOOP_Y, HOOP_X + HOOP_WIDTH/2, HOOP_Y + 15);

        // 3D effect on rim
        g2d.setColor(new Color(180, 40, 40));
        g2d.drawLine(HOOP_X + 5, HOOP_Y + 2, HOOP_X + HOOP_WIDTH - 5, HOOP_Y + 2);
    }

    private void drawBackboard(Graphics2D g2d) {
        // Backboard
        g2d.setColor(backboardColor);
        g2d.fillRect(BACKBOARD_X, BACKBOARD_Y, 12, BACKBOARD_HEIGHT);

        // Square on backboard
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(BACKBOARD_X + 1, BACKBOARD_Y + 40, 10, 60);

        // Highlight
        g2d.setColor(new Color(100, 100, 150, 50));
        g2d.fillRect(BACKBOARD_X + 1, BACKBOARD_Y + 10, 5, 20);

        // Pole
        g2d.setColor(new Color(150, 150, 150));
        g2d.fillRect(BACKBOARD_X + 3, BACKBOARD_Y + BACKBOARD_HEIGHT, 6, GROUND_Y - BACKBOARD_Y - BACKBOARD_HEIGHT);
    }

    private void drawPlayer(Graphics2D g2d, int x, int y, String name, Color color, int playerNum) {
        // Shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(x - 15, y + 20, 30, 10);

        // Body
        g2d.setColor(color);
        g2d.fillOval(x - 15, y - 40, 30, 30); // Head
        g2d.fillRect(x - 10, y - 15, 20, 35); // Body

        // Arms
        g2d.setColor(color.darker());
        if (playerNum == 1 || !isTwoPlayerMode) {
            // Shooting arm
            g2d.fillRect(x + 10, y - 5, 15, 6);
            g2d.fillRect(x + 20, y - 10, 6, 12);
        } else {
            g2d.fillRect(x - 25, y - 5, 15, 6);
            g2d.fillRect(x - 26, y - 10, 6, 12);
        }

        // Legs
        g2d.setColor(color.darker().darker());
        g2d.fillRect(x - 8, y + 18, 6, 15);
        g2d.fillRect(x + 2, y + 18, 6, 15);

        // Jersey number
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString(String.valueOf(playerNum), x - 4, y + 3);

        // Name tag
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(name, x - fm.stringWidth(name)/2, y - 45);

        // Movement indicator for player 1
        if (playerNum == 1) {
            g2d.setColor(new Color(255, 255, 255, 50));
            g2d.drawOval(x - 20, y - 50, 40, 10);
        }
    }

    private void drawBall(Graphics2D g2d) {
        // Ball shadow
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillOval(ballX + 5, ballY + BALL_SIZE - 5, BALL_SIZE, 8);

        // Ball with 3D effect
        int centerX = ballX + BALL_SIZE/2;
        int centerY = ballY + BALL_SIZE/2;

        // Main ball
        GradientPaint ballGradient = new GradientPaint(
                ballX + 5, ballY + 5, new Color(210, 140, 60),
                ballX + BALL_SIZE - 5, ballY + BALL_SIZE - 5, new Color(150, 80, 30)
        );
        g2d.setPaint(ballGradient);
        g2d.fillOval(ballX, ballY, BALL_SIZE, BALL_SIZE);

        // Ball outline
        g2d.setColor(new Color(100, 60, 20));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(ballX, ballY, BALL_SIZE, BALL_SIZE);

        // Ball rotation lines
        g2d.setColor(new Color(80, 50, 20, 80));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawArc(ballX + 2, ballY + 2, BALL_SIZE - 4, BALL_SIZE - 4,
                (int)(ballRotation), 120);
        g2d.drawArc(ballX + 2, ballY + 2, BALL_SIZE - 4, BALL_SIZE - 4,
                (int)(ballRotation + 120), 120);
        g2d.drawArc(ballX + 2, ballY + 2, BALL_SIZE - 4, BALL_SIZE - 4,
                (int)(ballRotation + 240), 120);

        // Highlight
        g2d.setColor(new Color(255, 255, 255, 60));
        g2d.fillOval(ballX + 4, ballY + 3, 8, 6);
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.fillOval(ballX + 2, ballY + 1, 5, 4);
    }

    private void drawTrajectory(Graphics2D g2d) {
        if (trajectoryIndex > 1) {
            g2d.setColor(new Color(255, 200, 100, 80));
            g2d.setStroke(new BasicStroke(2));
            for (int i = 1; i < trajectoryIndex; i++) {
                int alpha = 100 - (i * 3);
                if (alpha > 0) {
                    g2d.setColor(new Color(255, 200, 100, alpha));
                    g2d.drawLine((int)shotTrajectoryX[i-1], (int)shotTrajectoryY[i-1],
                            (int)shotTrajectoryX[i], (int)shotTrajectoryY[i]);
                }
            }
        }
    }

    private void drawScoreBoard(Graphics2D g2d) {
        // Scoreboard background
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(WIDTH/2 - 140, 10, 280, 50, 15, 15);
        g2d.setColor(new Color(255, 215, 0, 50));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(WIDTH/2 - 140, 10, 280, 50, 15, 15);

        // Scores
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);

        if (isTwoPlayerMode) {
            String scoreText = "P1: " + player1Score + "  |  P2: " + player2Score;
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(scoreText, WIDTH/2 - fm.stringWidth(scoreText)/2, 45);
        } else {
            String scoreText = "You: " + player1Score + "  |  CPU: " + computerScore;
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(scoreText, WIDTH/2 - fm.stringWidth(scoreText)/2, 45);
        }

        // Current player indicator
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String turnText = isBallShot ? "Shooting..." :
                (currentPlayer == 1 ? "Your Turn" :
                        (isTwoPlayerMode ? "Player 2's Turn" : "Computer's Turn"));
        g2d.setColor(currentPlayer == 1 ? Color.GREEN : Color.RED);
        FontMetrics fm2 = g2d.getFontMetrics();
        g2d.drawString(turnText, WIDTH - 150, 80);
    }

    private void drawControls(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String controls = isTwoPlayerMode ?
                "P1: A/D Move, W Shoot | P2: ←/→ Move, ↑ Shoot | M: Mode | R: Reset" :
                "A/D Move, W Shoot | M: Mode | R: Reset | 1-3: Difficulty";
        g2d.drawString(controls, 20, HEIGHT - 15);
    }

    private void drawDifficulty(Graphics2D g2d) {
        if (!isTwoPlayerMode) {
            g2d.setColor(new Color(255, 255, 255, 180));
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            String diffText = "Difficulty: " +
                    (difficulty == 1 ? "Easy" : difficulty == 2 ? "Medium" : "Hard");
            g2d.drawString(diffText, 20, 80);

            // Difficulty bars
            for (int i = 0; i < 3; i++) {
                g2d.setColor(i < difficulty ? Color.GREEN : new Color(100, 100, 100, 100));
                g2d.fillRect(20 + i * 25, 85, 20, 5);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateGame();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // Mode switching
        if (key == KeyEvent.VK_M) {
            isTwoPlayerMode = !isTwoPlayerMode;
            resetBall();
            player1Score = 0;
            player2Score = 0;
            computerScore = 0;
            currentPlayer = 1;
            gameMessage = isTwoPlayerMode ? "Two Player Mode" : "VS Computer Mode";
            messageTimer = 60;
        }

        // Reset
        if (key == KeyEvent.VK_R) {
            resetBall();
            player1Score = 0;
            player2Score = 0;
            computerScore = 0;
            currentPlayer = 1;
            gameMessage = "Game Reset!";
            messageTimer = 30;
        }

        // Difficulty selection
        if (!isTwoPlayerMode) {
            if (key == KeyEvent.VK_1) { difficulty = 1; gameMessage = "Easy Mode"; messageTimer = 30; }
            if (key == KeyEvent.VK_2) { difficulty = 2; gameMessage = "Medium Mode"; messageTimer = 30; }
            if (key == KeyEvent.VK_3) { difficulty = 3; gameMessage = "Hard Mode"; messageTimer = 30; }
        }

        // Player 1 controls (A, D, W)
        if (key == KeyEvent.VK_A) {
            if (!isBallShot && currentPlayer == 1) {
                player1X = Math.max(50, player1X - 8);
                if (!isBallMoving) {
                    ballX = player1X + 20;
                    ballY = GROUND_Y - BALL_SIZE;
                }
            }
        }
        if (key == KeyEvent.VK_D) {
            if (!isBallShot && currentPlayer == 1) {
                player1X = Math.min(WIDTH - 150, player1X + 8);
                if (!isBallMoving) {
                    ballX = player1X + 20;
                    ballY = GROUND_Y - BALL_SIZE;
                }
            }
        }
        if (key == KeyEvent.VK_W) {
            if (!isBallShot && currentPlayer == 1) {
                shootBall(player1X, GROUND_Y - 50, 2 + random.nextDouble() * 3, random.nextDouble() * 6 - 3);
                currentPlayer = isTwoPlayerMode ? 2 : 2;
            }
        }

        // Player 2 controls (Arrow keys)
        if (isTwoPlayerMode) {
            if (key == KeyEvent.VK_LEFT) {
                if (!isBallShot && currentPlayer == 2) {
                    player2X = Math.max(50, player2X - 8);
                }
            }
            if (key == KeyEvent.VK_RIGHT) {
                if (!isBallShot && currentPlayer == 2) {
                    player2X = Math.min(WIDTH - 150, player2X + 8);
                }
            }
            if (key == KeyEvent.VK_UP) {
                if (!isBallShot && currentPlayer == 2) {
                    shootBall(player2X, GROUND_Y - 50, 2 + random.nextDouble() * 3, random.nextDouble() * 6 - 3);
                    currentPlayer = 1;
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("3D Basketball Game - New York Stadium");
        ThreeDBasketballGame game = new ThreeDBasketballGame();
        frame.add(game);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setResizable(false);

        // Instructions dialog
        String instructions = "🏀 3D BASKETBALL GAME - NEW YORK STADIUM 🏀\n\n" +
                "MODES:\n" +
                "• Press M to switch between VS Computer and Two Player modes\n\n" +
                "CONTROLS:\n" +
                "Player 1: A/D to move, W to shoot\n" +
                "Player 2: ←/→ to move, ↑ to shoot\n\n" +
                "DIFFICULTY (VS Computer only):\n" +
                "Press 1: Easy  2: Medium  3: Hard\n\n" +
                "Press R to reset the game\n\n" +
                "Score by shooting the ball through the hoop!";
        JOptionPane.showMessageDialog(frame, instructions, "How to Play",
                JOptionPane.INFORMATION_MESSAGE);
    }
}