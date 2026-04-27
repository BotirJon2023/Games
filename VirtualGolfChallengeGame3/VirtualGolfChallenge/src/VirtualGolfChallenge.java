import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class VirtualGolfChallenge extends JPanel implements ActionListener, KeyListener {

    private Timer timer;
    private Random random = new Random();

    private int screenWidth = 1000;
    private int screenHeight = 650;

    private double ballX = 120;
    private double ballY = 500;
    private double ballVX = 0;
    private double ballVY = 0;

    private int holeX = 850;
    private int holeY = 510;

    private boolean ballMoving = false;
    private boolean chargingPower = false;

    private int power = 0;
    private boolean powerIncreasing = true;

    private int currentPlayer = 1;
    private int player1Shots = 0;
    private int player2Shots = 0;

    private boolean vsComputer = false;
    private boolean gameOver = false;

    private double waveOffset = 0;
    private int cloudX1 = 100;
    private int cloudX2 = 600;

    private JButton twoPlayerButton;
    private JButton computerButton;
    private JButton restartButton;

    public VirtualGolfChallenge() {
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setFocusable(true);
        addKeyListener(this);

        setLayout(null);

        twoPlayerButton = new JButton("Two Player Mode");
        computerButton = new JButton("Play With Computer");
        restartButton = new JButton("Restart Game");

        twoPlayerButton.setBounds(330, 20, 160, 35);
        computerButton.setBounds(510, 20, 180, 35);
        restartButton.setBounds(420, 60, 160, 35);

        add(twoPlayerButton);
        add(computerButton);
        add(restartButton);

        twoPlayerButton.addActionListener(e -> {
            vsComputer = false;
            resetGame();
            requestFocusInWindow();
        });

        computerButton.addActionListener(e -> {
            vsComputer = true;
            resetGame();
            requestFocusInWindow();
        });

        restartButton.addActionListener(e -> {
            resetGame();
            requestFocusInWindow();
        });

        timer = new Timer(16, this);
        timer.start();
    }

    private void resetGame() {
        ballX = 120;
        ballY = 500;
        ballVX = 0;
        ballVY = 0;

        currentPlayer = 1;
        player1Shots = 0;
        player2Shots = 0;

        power = 0;
        chargingPower = false;
        ballMoving = false;
        gameOver = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawSky(g2);
        drawSun(g2);
        drawClouds(g2);
        drawSea(g2);
        drawBeach(g2);
        drawPalmTree(g2);
        drawGolfCourse(g2);
        drawHole(g2);
        drawBall(g2);
        drawPowerBar(g2);
        drawHUD(g2);

        if (gameOver) {
            drawGameOver(g2);
        }
    }

    private void drawSky(Graphics2D g2) {
        GradientPaint sky = new GradientPaint(
                0, 0, new Color(70, 190, 255),
                0, 350, new Color(180, 235, 255)
        );

        g2.setPaint(sky);
        g2.fillRect(0, 0, screenWidth, screenHeight);
    }

    private void drawSun(Graphics2D g2) {
        g2.setColor(new Color(255, 225, 70));
        g2.fillOval(780, 80, 100, 100);

        g2.setColor(new Color(255, 240, 130, 100));
        g2.fillOval(750, 50, 160, 160);
    }

    private void drawClouds(Graphics2D g2) {
        drawCloud(g2, cloudX1, 100);
        drawCloud(g2, cloudX2, 150);
    }

    private void drawCloud(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(255, 255, 255, 220));

        g2.fillOval(x, y, 90, 45);
        g2.fillOval(x + 35, y - 25, 80, 65);
        g2.fillOval(x + 80, y, 90, 45);
        g2.fillOval(x + 25, y + 15, 120, 35);
    }

    private void drawSea(Graphics2D g2) {
        GradientPaint sea = new GradientPaint(
                0, 260, new Color(0, 145, 210),
                0, 430, new Color(0, 95, 175)
        );

        g2.setPaint(sea);
        g2.fillRect(0, 260, screenWidth, 190);

        g2.setStroke(new BasicStroke(3));
        g2.setColor(new Color(255, 255, 255, 140));

        for (int y = 285; y < 430; y += 30) {
            Path2D wave = new Path2D.Double();
            wave.moveTo(0, y);

            for (int x = 0; x <= screenWidth; x += 40) {
                double waveY = y + Math.sin((x + waveOffset) * 0.04) * 8;
                wave.lineTo(x, waveY);
            }

            g2.draw(wave);
        }
    }

    private void drawBeach(Graphics2D g2) {
        GradientPaint sand = new GradientPaint(
                0, 430, new Color(255, 224, 150),
                0, screenHeight, new Color(230, 175, 90)
        );

        g2.setPaint(sand);
        g2.fillRect(0, 430, screenWidth, screenHeight - 430);

        g2.setColor(new Color(255, 245, 190, 120));
        for (int i = 0; i < 80; i++) {
            int x = random.nextInt(screenWidth);
            int y = 440 + random.nextInt(180);
            g2.fillOval(x, y, 3, 3);
        }
    }

    private void drawPalmTree(Graphics2D g2) {
        g2.setColor(new Color(120, 70, 25));
        g2.setStroke(new BasicStroke(16));
        g2.drawLine(70, 450, 115, 270);

        g2.setStroke(new BasicStroke(1));
        g2.setColor(new Color(40, 160, 65));

        int cx = 115;
        int cy = 270;

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            int x2 = cx + (int) (Math.cos(angle) * 95);
            int y2 = cy + (int) (Math.sin(angle) * 45);

            g2.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx, cy, x2, y2);
        }

        g2.setColor(new Color(115, 55, 20));
        g2.fillOval(cx - 18, cy + 8, 18, 22);
        g2.fillOval(cx + 4, cy + 6, 18, 22);
    }

    private void drawGolfCourse(Graphics2D g2) {
        GradientPaint grass = new GradientPaint(
                0, 470, new Color(60, 210, 90),
                0, 610, new Color(25, 150, 55)
        );

        g2.setPaint(grass);
        g2.fillRoundRect(80, 470, 840, 85, 45, 45);

        g2.setColor(new Color(35, 120, 45));
        g2.fillRoundRect(80, 530, 840, 40, 45, 45);

        g2.setColor(new Color(255, 255, 255, 90));
        g2.drawArc(120, 480, 760, 70, 0, 180);
    }

    private void drawHole(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillOval(holeX - 18, holeY - 7, 36, 14);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(holeX, holeY - 5, holeX, holeY - 85);

        g2.setColor(new Color(255, 60, 60));
        int[] xPoints = {holeX, holeX + 60, holeX};
        int[] yPoints = {holeY - 85, holeY - 65, holeY - 45};
        g2.fillPolygon(xPoints, yPoints, 3);
    }

    private void drawBall(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval((int) ballX - 12, (int) ballY + 12, 28, 8);

        RadialGradientPaint ballGradient = new RadialGradientPaint(
                new Point2D.Double(ballX - 5, ballY - 5),
                18,
                new float[]{0f, 1f},
                new Color[]{Color.WHITE, new Color(200, 200, 200)}
        );

        g2.setPaint(ballGradient);
        g2.fillOval((int) ballX - 15, (int) ballY - 15, 30, 30);

        g2.setColor(new Color(160, 160, 160));
        g2.drawOval((int) ballX - 15, (int) ballY - 15, 30, 30);
    }

    private void drawPowerBar(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRoundRect(80, 590, 300, 30, 20, 20);

        GradientPaint powerPaint = new GradientPaint(
                80, 590, Color.GREEN,
                380, 590, Color.RED
        );

        g2.setPaint(powerPaint);
        g2.fillRoundRect(80, 590, power * 3, 30, 20, 20);

        g2.setColor(Color.WHITE);
        g2.drawRoundRect(80, 590, 300, 30, 20, 20);

        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("POWER", 190, 612);
    }

    private void drawHUD(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(20, 20, 280, 120, 20, 20);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 18));

        String mode = vsComputer ? "Mode: Player vs Computer" : "Mode: Two Players";
        g2.drawString(mode, 40, 50);

        g2.drawString("Current Turn: Player " + currentPlayer, 40, 80);
        g2.drawString("Player 1 Shots: " + player1Shots, 40, 105);

        if (vsComputer) {
            g2.drawString("Computer Shots: " + player2Shots, 40, 130);
        } else {
            g2.drawString("Player 2 Shots: " + player2Shots, 40, 130);
        }

        g2.setColor(new Color(255, 255, 255, 200));
        g2.setFont(new Font("Arial", Font.PLAIN, 15));
        g2.drawString("Hold SPACE to power shot, release to hit", 620, 610);
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(290, 210, 420, 180, 30, 30);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 32));
        g2.drawString("GAME OVER", 390, 260);

        g2.setFont(new Font("Arial", Font.BOLD, 20));

        String result;

        if (player1Shots < player2Shots) {
            result = "Player 1 Wins!";
        } else if (player2Shots < player1Shots) {
            result = vsComputer ? "Computer Wins!" : "Player 2 Wins!";
        } else {
            result = "It's a Draw!";
        }

        g2.drawString(result, 420, 310);

        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.drawString("Click Restart Game to play again", 390, 350);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        waveOffset += 2;

        cloudX1 += 1;
        cloudX2 += 1;

        if (cloudX1 > screenWidth + 150) {
            cloudX1 = -200;
        }

        if (cloudX2 > screenWidth + 150) {
            cloudX2 = -200;
        }

        if (chargingPower) {
            if (powerIncreasing) {
                power += 2;
                if (power >= 100) {
                    power = 100;
                    powerIncreasing = false;
                }
            } else {
                power -= 2;
                if (power <= 0) {
                    power = 0;
                    powerIncreasing = true;
                }
            }
        }

        updateBall();

        if (!ballMoving && vsComputer && currentPlayer == 2 && !gameOver) {
            computerShot();
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

        if (ballX < 90) {
            ballX = 90;
            ballVX *= -0.4;
        }

        if (ballX > 910) {
            ballX = 910;
            ballVX *= -0.4;
        }

        if (ballY < 475) {
            ballY = 475;
            ballVY *= -0.4;
        }

        if (ballY > 540) {
            ballY = 540;
            ballVY *= -0.4;
        }

        double distanceToHole = Math.hypot(ballX - holeX, ballY - holeY);

        if (distanceToHole < 22 && Math.abs(ballVX) < 5 && Math.abs(ballVY) < 5) {
            ballX = holeX;
            ballY = holeY;
            ballVX = 0;
            ballVY = 0;
            ballMoving = false;

            nextTurnOrEnd();
            return;
        }

        if (Math.abs(ballVX) < 0.15 && Math.abs(ballVY) < 0.15) {
            ballVX = 0;
            ballVY = 0;
            ballMoving = false;
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

        double dx = holeX - ballX;
        double dy = holeY - ballY;
        double distance = Math.hypot(dx, dy);

        double directionX = dx / distance;
        double directionY = dy / distance;

        double force = power / 7.0;

        ballVX = directionX * force;
        ballVY = directionY * force;

        ballMoving = true;
        power = 0;
    }

    private void computerShot() {
        player2Shots++;

        double dx = holeX - ballX;
        double dy = holeY - ballY;
        double distance = Math.hypot(dx, dy);

        double directionX = dx / distance;
        double directionY = dy / distance;

        double accuracyError = random.nextDouble() * 0.4 - 0.2;
        double force = 10 + random.nextDouble() * 4;

        ballVX = directionX * force + accuracyError;
        ballVY = directionY * force + accuracyError;

        ballMoving = true;
    }

    private void nextTurnOrEnd() {
        if (currentPlayer == 1) {
            currentPlayer = 2;
            resetBallPosition();
        } else {
            gameOver = true;
        }
    }

    private void resetBallPosition() {
        ballX = 120;
        ballY = 500;
        ballVX = 0;
        ballVY = 0;
        power = 0;
        chargingPower = false;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && !ballMoving && !gameOver) {
            chargingPower = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && chargingPower && !ballMoving && !gameOver) {
            chargingPower = false;
            shootBall();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Virtual Golf Challenge - Seaside Edition");
        VirtualGolfChallenge game = new VirtualGolfChallenge();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        game.requestFocusInWindow();
    }
}
