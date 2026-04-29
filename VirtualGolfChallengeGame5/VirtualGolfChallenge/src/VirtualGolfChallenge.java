import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class VirtualGolfChallenge extends JPanel implements ActionListener, KeyListener {

    private final Timer timer;
    private final Random random = new Random();

    private int width = 1000;
    private int height = 650;

    private double waveOffset = 0;
    private double cloudOffset = 0;
    private double birdOffset = 0;
    private double playerAnim = 0;

    private boolean vsComputer = false;
    private boolean gameStarted = false;
    private boolean gameOver = false;

    private int currentPlayer = 1;
    private int player1Shots = 0;
    private int player2Shots = 0;

    private double ballX = 180;
    private double ballY = 500;
    private double ballVX = 0;
    private double ballVY = 0;

    private final double holeX = 850;
    private final double holeY = 500;

    private boolean ballMoving = false;
    private boolean charging = false;
    private int power = 0;
    private boolean powerUp = true;

    private JButton twoPlayerButton;
    private JButton computerButton;
    private JButton restartButton;

    public VirtualGolfChallenge() {
        setPreferredSize(new Dimension(width, height));
        setFocusable(true);
        setLayout(null);
        addKeyListener(this);

        twoPlayerButton = new JButton("Two Player Mode");
        computerButton = new JButton("Play With Computer");
        restartButton = new JButton("Restart Game");

        twoPlayerButton.setBounds(350, 250, 300, 45);
        computerButton.setBounds(350, 310, 300, 45);
        restartButton.setBounds(410, 570, 180, 40);

        styleButton(twoPlayerButton);
        styleButton(computerButton);
        styleButton(restartButton);

        add(twoPlayerButton);
        add(computerButton);
        add(restartButton);

        restartButton.setVisible(false);

        twoPlayerButton.addActionListener(e -> {
            vsComputer = false;
            startGame();
        });

        computerButton.addActionListener(e -> {
            vsComputer = true;
            startGame();
        });

        restartButton.addActionListener(e -> resetToMenu());

        timer = new Timer(16, this);
        timer.start();
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setBackground(new Color(255, 190, 80));
        button.setForeground(new Color(50, 40, 30));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(120, 80, 30), 2));
    }

    private void startGame() {
        gameStarted = true;
        gameOver = false;

        currentPlayer = 1;
        player1Shots = 0;
        player2Shots = 0;

        resetBall();

        twoPlayerButton.setVisible(false);
        computerButton.setVisible(false);
        restartButton.setVisible(false);

        requestFocusInWindow();
    }

    private void resetToMenu() {
        gameStarted = false;
        gameOver = false;

        twoPlayerButton.setVisible(true);
        computerButton.setVisible(true);
        restartButton.setVisible(false);

        resetBall();
        repaint();
    }

    private void resetBall() {
        ballX = 180;
        ballY = 500;
        ballVX = 0;
        ballVY = 0;
        ballMoving = false;
        charging = false;
        power = 0;
        powerUp = true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        width = getWidth();
        height = getHeight();

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawSeasideBackground(g2);

        if (!gameStarted) {
            drawMenu(g2);
        } else {
            drawGolfCourse(g2);
            drawPlayers(g2);
            drawBall(g2);
            drawHUD(g2);

            if (gameOver) {
                drawGameOver(g2);
            }
        }
    }

    private void drawSeasideBackground(Graphics2D g2) {
        GradientPaint sky = new GradientPaint(
                0, 0, new Color(70, 190, 255),
                0, height, new Color(255, 230, 170)
        );
        g2.setPaint(sky);
        g2.fillRect(0, 0, width, height);

        drawSun(g2);
        drawClouds(g2);
        drawBirds(g2);
        drawOcean(g2);
        drawPalmTrees(g2);
    }

    private void drawSun(Graphics2D g2) {
        int sunX = 800;
        int sunY = 90;

        for (int r = 90; r > 45; r -= 8) {
            g2.setColor(new Color(255, 230, 90, 25));
            g2.fillOval(sunX - r, sunY - r, r * 2, r * 2);
        }

        g2.setColor(new Color(255, 220, 60));
        g2.fillOval(sunX - 45, sunY - 45, 90, 90);
    }

    private void drawClouds(Graphics2D g2) {
        drawCloud(g2, (int) ((120 + cloudOffset) % (width + 250)) - 150, 90);
        drawCloud(g2, (int) ((420 + cloudOffset * 0.7) % (width + 250)) - 150, 130);
        drawCloud(g2, (int) ((720 + cloudOffset * 0.5) % (width + 250)) - 150, 70);
    }

    private void drawCloud(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillOval(x, y, 80, 40);
        g2.fillOval(x + 35, y - 20, 70, 60);
        g2.fillOval(x + 85, y, 80, 40);
        g2.fillRoundRect(x + 25, y + 15, 120, 35, 30, 30);
    }

    private void drawBirds(Graphics2D g2) {
        g2.setStroke(new BasicStroke(3));
        g2.setColor(new Color(40, 60, 90));

        for (int i = 0; i < 4; i++) {
            int x = (int) ((150 + i * 160 + birdOffset) % (width + 100));
            int y = 120 + i * 20;

            g2.drawArc(x, y, 20, 12, 0, 180);
            g2.drawArc(x + 20, y, 20, 12, 0, 180);
        }
    }

    private void drawOcean(Graphics2D g2) {
        int oceanY = 300;

        GradientPaint ocean = new GradientPaint(
                0, oceanY, new Color(0, 150, 220),
                0, height, new Color(0, 80, 170)
        );
        g2.setPaint(ocean);
        g2.fillRect(0, oceanY, width, height - oceanY);

        for (int layer = 0; layer < 4; layer++) {
            Path2D wave = new Path2D.Double();
            int y = oceanY + 30 + layer * 35;
            wave.moveTo(0, y);

            for (int x = 0; x <= width; x += 20) {
                double yy = y + Math.sin((x + waveOffset * (layer + 1)) * 0.025) * 8;
                wave.lineTo(x, yy);
            }

            g2.setColor(new Color(255, 255, 255, 80));
            g2.setStroke(new BasicStroke(3));
            g2.draw(wave);
        }
    }

    private void drawPalmTrees(Graphics2D g2) {
        drawPalmTree(g2, 70, 335, 1.0);
        drawPalmTree(g2, 930, 330, 0.9);
    }

    private void drawPalmTree(Graphics2D g2, int x, int y, double scale) {
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.scale(scale, scale);

        g2.setColor(new Color(120, 70, 30));
        Polygon trunk = new Polygon();
        trunk.addPoint(-12, 0);
        trunk.addPoint(12, 0);
        trunk.addPoint(22, 180);
        trunk.addPoint(-20, 180);
        g2.fillPolygon(trunk);

        g2.setColor(new Color(90, 50, 25));
        for (int i = 20; i < 170; i += 25) {
            g2.drawLine(-10, i, 15, i + 10);
        }

        g2.setColor(new Color(30, 150, 60));
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45 + Math.sin(playerAnim) * 4);
            int x2 = (int) (Math.cos(angle) * 95);
            int y2 = (int) (Math.sin(angle) * 45);

            g2.setStroke(new BasicStroke(14, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(0, 0, x2, y2);
        }

        g2.setTransform(old);
    }

    private void drawMenu(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(260, 130, 480, 310, 30, 30);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 42));
        drawCenteredString(g2, "Virtual Golf Challenge", 0, 160, width);

        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.setColor(new Color(255, 245, 160));
        drawCenteredString(g2, "Seaside Golf Adventure", 0, 205, width);

        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.setColor(Color.WHITE);
        drawCenteredString(g2, "Choose a game mode to begin", 0, 230, width);
    }

    private void drawGolfCourse(Graphics2D g2) {
        int grassY = 430;

        GradientPaint sand = new GradientPaint(
                0, 360, new Color(245, 210, 130),
                0, height, new Color(230, 180, 90)
        );
        g2.setPaint(sand);
        g2.fillRect(0, 360, width, height - 360);

        GradientPaint green = new GradientPaint(
                0, grassY, new Color(80, 210, 80),
                0, height, new Color(30, 150, 45)
        );
        g2.setPaint(green);

        Path2D fairway = new Path2D.Double();
        fairway.moveTo(70, 560);
        fairway.curveTo(280, 420, 650, 420, 940, 540);
        fairway.lineTo(940, height);
        fairway.lineTo(70, height);
        fairway.closePath();
        g2.fill(fairway);

        g2.setColor(new Color(255, 235, 150));
        g2.fillOval(430, 485, 150, 70);

        g2.setColor(new Color(20, 90, 30));
        g2.fillOval((int) holeX - 15, (int) holeY - 6, 30, 12);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(4));
        g2.drawLine((int) holeX, (int) holeY, (int) holeX, (int) holeY - 90);

        g2.setColor(new Color(255, 60, 60));
        Polygon flag = new Polygon();
        flag.addPoint((int) holeX, (int) holeY - 90);
        flag.addPoint((int) holeX + 60, (int) holeY - 70);
        flag.addPoint((int) holeX, (int) holeY - 50);
        g2.fillPolygon(flag);
    }

    private void drawPlayers(Graphics2D g2) {
        drawGolfer(g2, 120, 470, new Color(40, 90, 220), "Player 1", currentPlayer == 1);
        drawGolfer(g2, 250, 470, new Color(230, 70, 80), vsComputer ? "Computer" : "Player 2", currentPlayer == 2);
    }

    private void drawGolfer(Graphics2D g2, int x, int y, Color shirtColor, String name, boolean active) {
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);

        if (active) {
            g2.setColor(new Color(255, 255, 120, 130));
            g2.fillOval(-35, -130, 70, 150);
        }

        double swing = active && charging ? Math.sin(playerAnim * 8) * 0.35 : Math.sin(playerAnim * 2) * 0.06;

        g2.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g2.setColor(new Color(245, 190, 140));
        g2.fillOval(-14, -110, 28, 28);

        g2.setColor(new Color(60, 35, 20));
        g2.fillArc(-16, -116, 32, 24, 0, 180);

        g2.setColor(shirtColor);
        g2.fillRoundRect(-18, -82, 36, 55, 15, 15);

        g2.setColor(new Color(30, 40, 70));
        g2.drawLine(-8, -28, -20, 25);
        g2.drawLine(8, -28, 20, 25);

        g2.setColor(Color.WHITE);
        g2.fillOval(-25, 22, 20, 8);
        g2.fillOval(7, 22, 20, 8);

        g2.setColor(new Color(245, 190, 140));

        int armX = (int) (Math.cos(swing) * 40);
        int armY = (int) (-60 + Math.sin(swing) * 45);

        g2.drawLine(-12, -70, armX, armY);
        g2.drawLine(12, -70, armX + 8, armY + 5);

        g2.setColor(new Color(80, 80, 80));
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(armX + 8, armY + 5, armX + 55, armY + 70);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(name, -fm.stringWidth(name) / 2, 50);

        g2.setTransform(old);
    }

    private void drawBall(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval((int) ballX - 8, (int) ballY + 8, 18, 6);

        g2.setColor(Color.WHITE);
        g2.fillOval((int) ballX - 8, (int) ballY - 8, 16, 16);

        g2.setColor(new Color(210, 210, 210));
        g2.drawOval((int) ballX - 8, (int) ballY - 8, 16, 16);
    }

    private void drawHUD(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRoundRect(20, 20, 310, 140, 20, 20);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("Current Turn: " + getCurrentPlayerName(), 40, 55);

        g2.setFont(new Font("Arial", Font.PLAIN, 17));
        g2.drawString("Player 1 Shots: " + player1Shots, 40, 90);
        g2.drawString((vsComputer ? "Computer" : "Player 2") + " Shots: " + player2Shots, 40, 120);

        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(new Color(255, 245, 160));
        g2.drawString("Hold SPACE to charge, release to shoot", 40, 145);

        drawPowerMeter(g2);
    }

    private void drawPowerMeter(Graphics2D g2) {
        int x = 700;
        int y = 35;
        int w = 230;
        int h = 30;

        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRoundRect(x - 15, y - 15, w + 30, h + 50, 18, 18);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("Power", x, y - 2);

        g2.setColor(new Color(60, 60, 60));
        g2.fillRoundRect(x, y + 10, w, h, 15, 15);

        GradientPaint powerPaint = new GradientPaint(
                x, y, Color.GREEN,
                x + w, y, Color.RED
        );
        g2.setPaint(powerPaint);
        g2.fillRoundRect(x, y + 10, (int) (w * (power / 100.0)), h, 15, 15);

        g2.setColor(Color.WHITE);
        g2.drawRoundRect(x, y + 10, w, h, 15, 15);
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(250, 180, 500, 300, 30, 30);

        g2.setColor(new Color(255, 240, 120));
        g2.setFont(new Font("Arial", Font.BOLD, 42));
        drawCenteredString(g2, "Game Over!", 250, 240, 500);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 24));

        String winner;

        if (player1Shots < player2Shots) {
            winner = "Player 1 Wins!";
        } else if (player2Shots < player1Shots) {
            winner = vsComputer ? "Computer Wins!" : "Player 2 Wins!";
        } else {
            winner = "It's a Tie!";
        }

        drawCenteredString(g2, winner, 250, 300, 500);

        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        drawCenteredString(g2, "Player 1 Shots: " + player1Shots, 250, 350, 500);
        drawCenteredString(g2, (vsComputer ? "Computer" : "Player 2") + " Shots: " + player2Shots, 250, 385, 500);

        restartButton.setVisible(true);
    }

    private void drawCenteredString(Graphics2D g2, String text, int x, int y, int w) {
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (w - fm.stringWidth(text)) / 2;
        g2.drawString(text, textX, y);
    }

    private String getCurrentPlayerName() {
        if (currentPlayer == 1) {
            return "Player 1";
        }

        return vsComputer ? "Computer" : "Player 2";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        waveOffset += 1.4;
        cloudOffset += 0.25;
        birdOffset += 1.1;
        playerAnim += 0.04;

        if (charging && !ballMoving && !gameOver) {
            if (powerUp) {
                power += 2;
                if (power >= 100) {
                    power = 100;
                    powerUp = false;
                }
            } else {
                power -= 2;
                if (power <= 0) {
                    power = 0;
                    powerUp = true;
                }
            }
        }

        if (gameStarted && !gameOver) {
            updateBall();

            if (vsComputer && currentPlayer == 2 && !ballMoving && !charging) {
                computerShot();
            }
        }

        repaint();
    }

    private void updateBall() {
        if (!ballMoving) {
            return;
        }

        ballX += ballVX;
        ballY += ballVY;

        ballVX *= 0.985;
        ballVY *= 0.985;

        if (ballX < 20 || ballX > width - 20) {
            ballVX *= -0.6;
        }

        if (ballY < 380 || ballY > height - 40) {
            ballVY *= -0.6;
        }

        double speed = Math.sqrt(ballVX * ballVX + ballVY * ballVY);

        double dx = ballX - holeX;
        double dy = ballY - holeY;
        double distanceToHole = Math.sqrt(dx * dx + dy * dy);

        if (distanceToHole < 18 && speed < 5.5) {
            ballX = holeX;
            ballY = holeY;
            ballVX = 0;
            ballVY = 0;
            ballMoving = false;

            nextTurnOrEnd();
            return;
        }

        if (speed < 0.15) {
            ballVX = 0;
            ballVY = 0;
            ballMoving = false;
            nextTurnOrEnd();
        }
    }

    private void shootBall() {
        if (ballMoving || gameOver) {
            return;
        }

        if (currentPlayer == 1) {
            player1Shots++;
        } else {
            player2Shots++;
        }

        double targetAngle = Math.atan2(holeY - ballY, holeX - ballX);

        double strength = power / 100.0;
        double speed = 5 + strength * 18;

        double accuracyError = random.nextDouble() * 0.25 - 0.125;

        ballVX = Math.cos(targetAngle + accuracyError) * speed;
        ballVY = Math.sin(targetAngle + accuracyError) * speed;

        ballMoving = true;
        charging = false;
        power = 0;
        powerUp = true;
    }

    private void computerShot() {
        Timer computerDelay = new Timer(900, e -> {
            if (currentPlayer == 2 && vsComputer && !ballMoving && !gameOver) {
                player2Shots++;

                double targetAngle = Math.atan2(holeY - ballY, holeX - ballX);
                double distance = Math.sqrt(Math.pow(holeX - ballX, 2) + Math.pow(holeY - ballY, 2));

                double speed = Math.min(22, Math.max(8, distance / 35));
                double accuracyError = random.nextDouble() * 0.35 - 0.175;

                ballVX = Math.cos(targetAngle + accuracyError) * speed;
                ballVY = Math.sin(targetAngle + accuracyError) * speed;

                ballMoving = true;
            }
        });

        computerDelay.setRepeats(false);
        computerDelay.start();
    }

    private void nextTurnOrEnd() {
        double dx = ballX - holeX;
        double dy = ballY - holeY;
        double distanceToHole = Math.sqrt(dx * dx + dy * dy);

        if (distanceToHole < 20) {
            if (currentPlayer == 1) {
                currentPlayer = 2;
                resetBall();
            } else {
                gameOver = true;
            }
        } else {
            currentPlayer = currentPlayer == 1 ? 2 : 1;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!gameStarted || gameOver) {
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_SPACE && !ballMoving) {
            if (!(vsComputer && currentPlayer == 2)) {
                charging = true;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!gameStarted || gameOver) {
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_SPACE && charging) {
            shootBall();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Virtual Golf Challenge - Seaside Edition");
            VirtualGolfChallenge game = new VirtualGolfChallenge();

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}
