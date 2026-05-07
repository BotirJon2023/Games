import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class VirtualGolfGirlsChallenge extends JPanel implements ActionListener, KeyListener {

    private final Timer timer;
    private final Random random = new Random();

    private double ballX = 170;
    private double ballY = 470;
    private double ballVX = 0;
    private double ballVY = 0;

    private final int holeX = 710;
    private final int holeY = 430;

    private boolean ballMoving = false;
    private boolean charging = false;

    private int power = 0;
    private boolean powerUp = true;

    private int currentPlayer = 1;
    private int player1Shots = 0;
    private int player2Shots = 0;

    private boolean vsComputer = false;
    private boolean gameOver = false;

    private double waveOffset = 0;
    private double cloudOffset = 0;
    private double girlAnim = 0;

    private JButton twoPlayerButton;
    private JButton computerButton;
    private JButton restartButton;

    public VirtualGolfGirlsChallenge() {
        setPreferredSize(new Dimension(900, 620));
        setFocusable(true);
        setLayout(null);
        addKeyListener(this);

        twoPlayerButton = new JButton("Two Girls Mode");
        computerButton = new JButton("Play With Computer");
        restartButton = new JButton("Restart Game");

        twoPlayerButton.setBounds(20, 20, 150, 35);
        computerButton.setBounds(180, 20, 180, 35);
        restartButton.setBounds(370, 20, 140, 35);

        styleButton(twoPlayerButton);
        styleButton(computerButton);
        styleButton(restartButton);

        add(twoPlayerButton);
        add(computerButton);
        add(restartButton);

        twoPlayerButton.addActionListener(e -> {
            vsComputer = false;
            restartGame();
            requestFocusInWindow();
        });

        computerButton.addActionListener(e -> {
            vsComputer = true;
            restartGame();
            requestFocusInWindow();
        });

        restartButton.addActionListener(e -> {
            restartGame();
            requestFocusInWindow();
        });

        timer = new Timer(16, this);
        timer.start();
    }

    private void styleButton(JButton button) {
        button.setBackground(new Color(255, 118, 188));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
    }

    private void restartGame() {
        ballX = 170;
        ballY = 470;
        ballVX = 0;
        ballVY = 0;
        ballMoving = false;
        charging = false;
        power = 0;
        currentPlayer = 1;
        player1Shots = 0;
        player2Shots = 0;
        gameOver = false;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        enableQuality(g2);

        drawSky(g2);
        drawSun(g2);
        drawClouds(g2);
        drawOcean(g2);
        drawBeach(g2);
        drawGrassCourse(g2);
        drawPalmTree(g2);
        drawHole(g2);
        drawGolfBall(g2);
        drawGirlPlayers(g2);
        drawHUD(g2);
        drawPowerMeter(g2);

        if (gameOver) {
            drawGameOver(g2);
        }
    }

    private void enableQuality(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private void drawSky(Graphics2D g2) {
        GradientPaint sky = new GradientPaint(
                0, 0, new Color(95, 210, 255),
                0, 300, new Color(255, 205, 235)
        );
        g2.setPaint(sky);
        g2.fillRect(0, 0, getWidth(), 320);
    }

    private void drawSun(Graphics2D g2) {
        int x = 760;
        int y = 85;

        g2.setColor(new Color(255, 235, 90, 90));
        g2.fillOval(x - 45, y - 45, 130, 130);

        g2.setColor(new Color(255, 222, 55));
        g2.fillOval(x, y, 70, 70);

        g2.setColor(new Color(255, 255, 180));
        g2.fillOval(x + 15, y + 12, 22, 22);
    }

    private void drawClouds(Graphics2D g2) {
        drawCloud(g2, (int) ((80 + cloudOffset) % 1000) - 100, 90);
        drawCloud(g2, (int) ((360 + cloudOffset * 0.7) % 1000) - 100, 65);
        drawCloud(g2, (int) ((620 + cloudOffset * 0.5) % 1000) - 100, 130);
    }

    private void drawCloud(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(255, 255, 255, 215));
        g2.fillOval(x, y, 60, 35);
        g2.fillOval(x + 28, y - 15, 70, 50);
        g2.fillOval(x + 75, y, 65, 38);
        g2.fillRoundRect(x + 20, y + 18, 100, 25, 25, 25);
    }

    private void drawOcean(Graphics2D g2) {
        GradientPaint ocean = new GradientPaint(
                0, 220, new Color(15, 165, 220),
                0, 355, new Color(0, 95, 185)
        );
        g2.setPaint(ocean);
        g2.fillRect(0, 220, getWidth(), 150);

        for (int i = 0; i < 4; i++) {
            Path2D wave = new Path2D.Double();
            int baseY = 250 + i * 28;
            wave.moveTo(0, baseY);

            for (int x = 0; x <= getWidth(); x += 20) {
                double y = baseY + Math.sin((x + waveOffset * (2 + i)) * 0.035) * 8;
                wave.lineTo(x, y);
            }

            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255, 255, 255, 120));
            g2.draw(wave);
        }
    }

    private void drawBeach(Graphics2D g2) {
        GradientPaint sand = new GradientPaint(
                0, 350, new Color(255, 230, 150),
                0, 620, new Color(236, 184, 95)
        );
        g2.setPaint(sand);
        g2.fillRect(0, 350, getWidth(), 270);

        g2.setColor(new Color(255, 245, 190, 120));
        for (int i = 0; i < 80; i++) {
            int x = random.nextInt(900);
            int y = 360 + random.nextInt(250);
            g2.fillOval(x, y, 2, 2);
        }
    }

    private void drawGrassCourse(Graphics2D g2) {
        GradientPaint grass = new GradientPaint(
                80, 405, new Color(95, 220, 105),
                760, 520, new Color(35, 160, 70)
        );

        RoundRectangle2D course = new RoundRectangle2D.Double(90, 395, 690, 135, 75, 75);
        g2.setPaint(grass);
        g2.fill(course);

        g2.setColor(new Color(20, 120, 50));
        g2.setStroke(new BasicStroke(4f));
        g2.draw(course);

        g2.setColor(new Color(255, 255, 255, 45));
        for (int x = 110; x < 760; x += 35) {
            g2.drawArc(x, 400, 70, 110, 0, 180);
        }
    }

    private void drawPalmTree(Graphics2D g2) {
        int x = 70;
        int y = 345;

        g2.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(135, 82, 35));
        g2.drawLine(x, y + 150, x + 35, y);

        g2.setStroke(new BasicStroke(6f));
        g2.setColor(new Color(105, 62, 25));
        for (int i = 0; i < 7; i++) {
            g2.drawLine(x + i * 5, y + 145 - i * 20, x + 25 + i * 3, y + 130 - i * 20);
        }

        int topX = x + 35;
        int topY = y;

        g2.setColor(new Color(20, 155, 70));
        drawLeaf(g2, topX, topY, -95);
        drawLeaf(g2, topX, topY, -55);
        drawLeaf(g2, topX, topY, -20);
        drawLeaf(g2, topX, topY, 20);
        drawLeaf(g2, topX, topY, 60);
        drawLeaf(g2, topX, topY, 105);

        g2.setColor(new Color(120, 70, 25));
        g2.fillOval(topX - 10, topY + 5, 18, 22);
        g2.fillOval(topX + 5, topY + 8, 18, 22);
    }

    private void drawLeaf(Graphics2D g2, int x, int y, int angle) {
        AffineTransform old = g2.getTransform();
        g2.rotate(Math.toRadians(angle), x, y);

        Path2D leaf = new Path2D.Double();
        leaf.moveTo(x, y);
        leaf.curveTo(x + 40, y - 25, x + 85, y - 20, x + 120, y);
        leaf.curveTo(x + 80, y + 15, x + 40, y + 12, x, y);

        g2.fill(leaf);
        g2.setTransform(old);
    }

    private void drawHole(Graphics2D g2) {
        g2.setColor(new Color(25, 25, 25));
        g2.fillOval(holeX - 18, holeY - 8, 36, 16);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(holeX, holeY - 8, holeX, holeY - 80);

        g2.setColor(new Color(255, 70, 110));
        Path2D flag = new Path2D.Double();
        flag.moveTo(holeX, holeY - 80);
        flag.lineTo(holeX + 45, holeY - 65);
        flag.lineTo(holeX, holeY - 50);
        flag.closePath();
        g2.fill(flag);
    }

    private void drawGolfBall(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval((int) ballX - 10, (int) ballY + 8, 24, 8);

        RadialGradientPaint ballPaint = new RadialGradientPaint(
                new Point2D.Double(ballX - 4, ballY - 4),
                15,
                new float[]{0f, 1f},
                new Color[]{Color.WHITE, new Color(210, 210, 210)}
        );

        g2.setPaint(ballPaint);
        g2.fillOval((int) ballX - 10, (int) ballY - 10, 20, 20);

        g2.setColor(new Color(160, 160, 160));
        g2.drawOval((int) ballX - 10, (int) ballY - 10, 20, 20);
    }

    private void drawGirlPlayers(Graphics2D g2) {
        drawGirlGolfer(g2, 115, 465, new Color(255, 80, 170), new Color(90, 55, 25), currentPlayer == 1);
        drawGirlGolfer(g2, 790, 465, new Color(120, 95, 255), new Color(55, 28, 15), currentPlayer == 2);
    }

    private void drawGirlGolfer(Graphics2D g2, int x, int y, Color outfit, Color hair, boolean active) {
        double swing = active && charging ? Math.sin(girlAnim) * 18 : 0;

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(x - 28, y + 62, 70, 14);

        if (active) {
            g2.setColor(new Color(255, 255, 120, 120));
            g2.setStroke(new BasicStroke(4f));
            g2.drawOval(x - 42, y - 42, 95, 135);
        }

        // Legs
        g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(245, 190, 155));
        g2.drawLine(x - 8, y + 45, x - 18, y + 70);
        g2.drawLine(x + 12, y + 45, x + 25, y + 70);

        // Shoes
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x - 28, y + 66, 24, 9, 8, 8);
        g2.fillRoundRect(x + 14, y + 66, 24, 9, 8, 8);

        // Skirt
        g2.setColor(outfit.darker());
        Path2D skirt = new Path2D.Double();
        skirt.moveTo(x - 22, y + 25);
        skirt.lineTo(x + 25, y + 25);
        skirt.lineTo(x + 35, y + 50);
        skirt.lineTo(x - 32, y + 50);
        skirt.closePath();
        g2.fill(skirt);

        // Body
        GradientPaint shirt = new GradientPaint(
                x - 25, y - 10, outfit.brighter(),
                x + 25, y + 35, outfit.darker()
        );
        g2.setPaint(shirt);
        g2.fillRoundRect(x - 23, y - 10, 48, 45, 20, 20);

        // Arms
        g2.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(245, 190, 155));

        int clubX = x + 46;
        int clubY = y + 28;

        g2.drawLine(x - 15, y + 5, x - 35, y + 28);
        g2.drawLine(x + 15, y + 5, clubX, clubY);

        // Neck
        g2.setColor(new Color(245, 190, 155));
        g2.fillRoundRect(x - 6, y - 22, 12, 20, 8, 8);

        // Hair behind head
        g2.setColor(hair);
        g2.fillOval(x - 28, y - 70, 56, 62);
        g2.fillRoundRect(x - 30, y - 45, 60, 55, 25, 25);

        // Face
        g2.setColor(new Color(255, 205, 175));
        g2.fillOval(x - 22, y - 62, 44, 45);

        // Hair fringe
        g2.setColor(hair);
        g2.fillArc(x - 25, y - 66, 50, 35, 0, 180);
        g2.fillOval(x - 25, y - 48, 13, 25);
        g2.fillOval(x + 12, y - 48, 13, 25);

        // Eyes
        g2.setColor(Color.BLACK);
        g2.fillOval(x - 10, y - 45, 4, 5);
        g2.fillOval(x + 8, y - 45, 4, 5);

        // Smile
        g2.setColor(new Color(180, 60, 90));
        g2.setStroke(new BasicStroke(2f));
        g2.drawArc(x - 8, y - 38, 18, 10, 200, 140);

        // Hat / visor
        g2.setColor(Color.WHITE);
        g2.fillArc(x - 25, y - 72, 50, 22, 0, 180);
        g2.setColor(outfit);
        g2.fillRoundRect(x - 18, y - 72, 36, 8, 8, 8);

        // Golf club
        AffineTransform old = g2.getTransform();
        g2.rotate(Math.toRadians(swing), clubX, clubY);

        g2.setColor(new Color(80, 80, 90));
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(clubX, clubY, clubX + 50, clubY + 55);

        g2.setColor(new Color(55, 55, 65));
        g2.fillRoundRect(clubX + 42, clubY + 53, 28, 8, 7, 7);

        g2.setTransform(old);
    }

    private void drawHUD(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(535, 15, 340, 95, 20, 20);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 18));

        String mode = vsComputer ? "Mode: Girl vs Computer" : "Mode: Two Girls";
        g2.drawString(mode, 555, 42);

        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.drawString("Girl Player 1 Shots: " + player1Shots, 555, 67);

        if (vsComputer) {
            g2.drawString("Computer Shots: " + player2Shots, 555, 90);
        } else {
            g2.drawString("Girl Player 2 Shots: " + player2Shots, 555, 90);
        }

        g2.setFont(new Font("Arial", Font.BOLD, 17));
        if (!gameOver) {
            String turn;
            if (vsComputer && currentPlayer == 2) {
                turn = "Computer is thinking...";
            } else {
                turn = "Turn: Girl Player " + currentPlayer;
            }

            g2.setColor(new Color(255, 255, 130));
            g2.drawString(turn, 40, 92);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.PLAIN, 15));
            g2.drawString("Hold SPACE to charge. Release SPACE to shoot.", 40, 118);
        }
    }

    private void drawPowerMeter(Graphics2D g2) {
        if (gameOver || ballMoving || currentPlayer == 2 && vsComputer) {
            return;
        }

        int x = 40;
        int y = 540;
        int w = 260;
        int h = 28;

        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(x - 10, y - 35, w + 25, 75, 18, 18);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("Power", x, y - 12);

        g2.setColor(new Color(255, 255, 255, 180));
        g2.fillRoundRect(x, y, w, h, 15, 15);

        GradientPaint powerPaint = new GradientPaint(
                x, y, new Color(80, 255, 120),
                x + w, y, new Color(255, 60, 90)
        );

        g2.setPaint(powerPaint);
        g2.fillRoundRect(x, y, (int) (w * power / 100.0), h, 15, 15);

        g2.setColor(Color.WHITE);
        g2.drawRoundRect(x, y, w, h, 15, 15);
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(210, 170, 480, 230, 30, 30);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 36));
        g2.drawString("Game Over!", 345, 225);

        g2.setFont(new Font("Arial", Font.BOLD, 22));

        String winner;
        if (player1Shots < player2Shots) {
            winner = "Girl Player 1 Wins!";
        } else if (player2Shots < player1Shots) {
            winner = vsComputer ? "Computer Wins!" : "Girl Player 2 Wins!";
        } else {
            winner = "It's a Tie!";
        }

        g2.setColor(new Color(255, 230, 100));
        g2.drawString(winner, 345, 275);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.drawString("Player 1 Shots: " + player1Shots, 360, 315);

        if (vsComputer) {
            g2.drawString("Computer Shots: " + player2Shots, 360, 345);
        } else {
            g2.drawString("Player 2 Shots: " + player2Shots, 360, 345);
        }

        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("Click Restart Game to play again", 330, 380);
    }

    private void shootBall(double angle, double force) {
        ballVX = Math.cos(angle) * force;
        ballVY = Math.sin(angle) * force;
        ballMoving = true;

        if (currentPlayer == 1) {
            player1Shots++;
        } else {
            player2Shots++;
        }
    }

    private void playerShoot() {
        double dx = holeX - ballX;
        double dy = holeY - ballY;
        double angle = Math.atan2(dy, dx);

        double force = 3.0 + power * 0.18;
        shootBall(angle, force);

        power = 0;
        charging = false;
    }

    private void computerShoot() {
        if (gameOver || ballMoving || currentPlayer != 2) {
            return;
        }

        double dx = holeX - ballX;
        double dy = holeY - ballY;

        double angle = Math.atan2(dy, dx);
        angle += Math.toRadians(random.nextInt(18) - 9);

        double distance = Math.sqrt(dx * dx + dy * dy);
        double force = Math.min(18, Math.max(6, distance / 38.0));

        shootBall(angle, force);
    }

    private void updateBall() {
        if (!ballMoving) {
            return;
        }

        ballX += ballVX;
        ballY += ballVY;

        ballVX *= 0.985;
        ballVY *= 0.985;

        if (ballX < 100) {
            ballX = 100;
            ballVX *= -0.55;
        }

        if (ballX > 775) {
            ballX = 775;
            ballVX *= -0.55;
        }

        if (ballY < 395) {
            ballY = 395;
            ballVY *= -0.55;
        }

        if (ballY > 530) {
            ballY = 530;
            ballVY *= -0.55;
        }

        double speed = Math.sqrt(ballVX * ballVX + ballVY * ballVY);
        double distanceToHole = Point2D.distance(ballX, ballY, holeX, holeY);

        if (distanceToHole < 18 && speed < 6.0) {
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

    private void nextTurnOrEnd() {
        if (currentPlayer == 1) {
            currentPlayer = 2;
            resetBallForNextTurn();

            if (vsComputer) {
                Timer computerDelay = new Timer(900, e -> {
                    computerShoot();
                    ((Timer) e.getSource()).stop();
                });
                computerDelay.setRepeats(false);
                computerDelay.start();
            }
        } else {
            gameOver = true;
        }
    }

    private void resetBallForNextTurn() {
        ballX = 170;
        ballY = 470;
        ballVX = 0;
        ballVY = 0;
        power = 0;
        charging = false;
        ballMoving = false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        waveOffset += 1.3;
        cloudOffset += 0.25;
        girlAnim += 0.18;

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

        updateBall();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (
                e.getKeyCode() == KeyEvent.VK_SPACE &&
                        !ballMoving &&
                        !gameOver &&
                        !(vsComputer && currentPlayer == 2)
        ) {
            charging = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (
                e.getKeyCode() == KeyEvent.VK_SPACE &&
                        charging &&
                        !ballMoving &&
                        !gameOver &&
                        !(vsComputer && currentPlayer == 2)
        ) {
            playerShoot();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Virtual Golf Challenge - Seaside Girls Edition");
            VirtualGolfGirlsChallenge game = new VirtualGolfGirlsChallenge();

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
