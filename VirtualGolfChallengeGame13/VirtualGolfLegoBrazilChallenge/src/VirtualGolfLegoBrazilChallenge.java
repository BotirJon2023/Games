import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class VirtualGolfLegoBrazilChallenge extends JPanel implements ActionListener, KeyListener {

    private final Timer timer = new Timer(16, this);
    private final Random random = new Random();

    private double ballX = 190;
    private double ballY = 530;
    private double ballVX = 0;
    private double ballVY = 0;

    private final double startX = 190;
    private final double startY = 530;

    private final double holeX = 750;
    private final double holeY = 500;

    private int currentPlayer = 1;
    private int shotsP1 = 0;
    private int shotsP2 = 0;

    private boolean vsComputer = false;
    private boolean gameOver = false;
    private boolean charging = false;

    private double power = 0;
    private boolean powerUp = true;

    private double waveOffset = 0;
    private double cloudOffset = 0;
    private double swingAnim = 0;
    private boolean swinging = false;

    private JButton twoPlayerButton;
    private JButton computerButton;
    private JButton restartButton;

    public VirtualGolfLegoBrazilChallenge() {
        setPreferredSize(new Dimension(950, 650));
        setFocusable(true);
        setLayout(null);
        addKeyListener(this);

        twoPlayerButton = new JButton("Two LEGO Players");
        computerButton = new JButton("Play With Computer");
        restartButton = new JButton("Restart Game");

        twoPlayerButton.setBounds(20, 15, 160, 35);
        computerButton.setBounds(190, 15, 180, 35);
        restartButton.setBounds(380, 15, 140, 35);

        styleButton(twoPlayerButton, new Color(255, 203, 5));
        styleButton(computerButton, new Color(0, 156, 59));
        styleButton(restartButton, new Color(0, 39, 118));

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

        timer.start();
    }

    private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
    }

    private void restartGame() {
        ballX = startX;
        ballY = startY;
        ballVX = 0;
        ballVY = 0;
        shotsP1 = 0;
        shotsP2 = 0;
        currentPlayer = 1;
        gameOver = false;
        charging = false;
        power = 0;
        swinging = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBrazilSeasideBackground(g2);
        drawGolfCourse(g2);
        drawHole(g2);
        drawBall(g2);
        drawLegoPlayers(g2);
        drawHUD(g2);

        if (gameOver) {
            drawGameOver(g2);
        }
    }

    private void drawBrazilSeasideBackground(Graphics2D g2) {
        int w = getWidth();
        int h = getHeight();

        GradientPaint sky = new GradientPaint(
                0, 0, new Color(35, 180, 255),
                0, h / 2, new Color(255, 232, 120)
        );
        g2.setPaint(sky);
        g2.fillRect(0, 0, w, h);

        drawSun(g2);
        drawClouds(g2);
        drawChristRedeemer(g2);
        drawSugarloafMountain(g2);
        drawOcean(g2);
        drawAnimatedWaves(g2);
        drawCopacabanaBeach(g2);
        drawPalmTrees(g2);
    }

    private void drawSun(Graphics2D g2) {
        g2.setColor(new Color(255, 230, 50));
        g2.fillOval(775, 65, 90, 90);

        g2.setStroke(new BasicStroke(3));
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            int x1 = (int) (820 + Math.cos(a) * 55);
            int y1 = (int) (110 + Math.sin(a) * 55);
            int x2 = (int) (820 + Math.cos(a) * 75);
            int y2 = (int) (110 + Math.sin(a) * 75);
            g2.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawClouds(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 210));
        drawCloud(g2, (int) ((80 + cloudOffset) % 1050) - 100, 95);
        drawCloud(g2, (int) ((360 + cloudOffset * 0.7) % 1050) - 100, 70);
        drawCloud(g2, (int) ((650 + cloudOffset * 0.9) % 1050) - 100, 130);
    }

    private void drawCloud(Graphics2D g2, int x, int y) {
        g2.fillOval(x, y + 20, 60, 35);
        g2.fillOval(x + 35, y, 70, 55);
        g2.fillOval(x + 90, y + 18, 65, 38);
        g2.fillRoundRect(x + 20, y + 28, 115, 30, 20, 20);
    }

    private void drawChristRedeemer(Graphics2D g2) {
        g2.setColor(new Color(75, 95, 105, 180));

        int baseX = 145;
        int baseY = 245;

        g2.fillRoundRect(baseX + 35, baseY - 75, 20, 75, 10, 10);
        g2.fillOval(baseX + 30, baseY - 102, 30, 30);

        g2.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(baseX - 35, baseY - 62, baseX + 125, baseY - 62);

        g2.setStroke(new BasicStroke(8));
        g2.drawLine(baseX + 45, baseY, baseX + 20, baseY + 35);
        g2.drawLine(baseX + 45, baseY, baseX + 70, baseY + 35);

        g2.setColor(new Color(55, 85, 75));
        g2.fillOval(baseX - 70, baseY + 20, 220, 60);
    }

    private void drawSugarloafMountain(Graphics2D g2) {
        GradientPaint mountain = new GradientPaint(
                0, 220, new Color(45, 110, 88),
                0, 360, new Color(20, 70, 65)
        );
        g2.setPaint(mountain);

        Path2D.Double m1 = new Path2D.Double();
        m1.moveTo(390, 330);
        m1.curveTo(455, 170, 530, 180, 585, 330);
        m1.closePath();
        g2.fill(m1);

        g2.setColor(new Color(30, 92, 77));
        Path2D.Double m2 = new Path2D.Double();
        m2.moveTo(520, 335);
        m2.curveTo(610, 210, 725, 205, 800, 335);
        m2.closePath();
        g2.fill(m2);
    }

    private void drawOcean(Graphics2D g2) {
        GradientPaint ocean = new GradientPaint(
                0, 300, new Color(0, 145, 205),
                0, 455, new Color(0, 80, 170)
        );
        g2.setPaint(ocean);
        g2.fillRect(0, 295, getWidth(), 160);
    }

    private void drawAnimatedWaves(Graphics2D g2) {
        g2.setStroke(new BasicStroke(3));

        for (int row = 0; row < 4; row++) {
            int y = 330 + row * 30;
            g2.setColor(new Color(255, 255, 255, 150 - row * 20));

            Path2D.Double wave = new Path2D.Double();
            wave.moveTo(0, y);

            for (int x = 0; x <= getWidth(); x += 25) {
                double yy = y + Math.sin((x + waveOffset + row * 40) * 0.035) * 8;
                wave.lineTo(x, yy);
            }

            g2.draw(wave);
        }
    }

    private void drawCopacabanaBeach(Graphics2D g2) {
        GradientPaint sand = new GradientPaint(
                0, 420, new Color(255, 222, 145),
                0, getHeight(), new Color(235, 178, 90)
        );
        g2.setPaint(sand);
        g2.fillRect(0, 420, getWidth(), getHeight() - 420);

        g2.setStroke(new BasicStroke(4));

        for (int i = -100; i < getWidth(); i += 80) {
            g2.setColor(new Color(40, 40, 40, 70));
            Path2D.Double curve = new Path2D.Double();
            curve.moveTo(i, 445);
            curve.curveTo(i + 35, 465, i + 45, 485, i + 80, 505);
            g2.draw(curve);

            g2.setColor(new Color(255, 255, 255, 120));
            Path2D.Double curve2 = new Path2D.Double();
            curve2.moveTo(i + 35, 445);
            curve2.curveTo(i + 70, 465, i + 80, 485, i + 115, 505);
            g2.draw(curve2);
        }
    }

    private void drawPalmTrees(Graphics2D g2) {
        drawPalm(g2, 70, 420, 1.0);
        drawPalm(g2, 875, 430, 0.85);
    }

    private void drawPalm(Graphics2D g2, int x, int y, double s) {
        g2.setStroke(new BasicStroke((float) (10 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(120, 70, 25));
        g2.drawLine(x, y, (int) (x + 25 * s), (int) (y - 115 * s));

        g2.setColor(new Color(20, 145, 55));
        g2.setStroke(new BasicStroke((float) (8 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int topX = (int) (x + 25 * s);
        int topY = (int) (y - 115 * s);

        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            int ex = (int) (topX + Math.cos(a) * 65 * s);
            int ey = (int) (topY + Math.sin(a) * 35 * s);
            g2.drawLine(topX, topY, ex, ey);
        }

        g2.setColor(new Color(125, 70, 20));
        g2.fillOval(topX - 9, topY - 3, 13, 16);
        g2.fillOval(topX + 5, topY + 2, 13, 16);
    }

    private void drawGolfCourse(Graphics2D g2) {
        GradientPaint green = new GradientPaint(
                0, 465, new Color(50, 210, 75),
                0, 650, new Color(20, 145, 45)
        );
        g2.setPaint(green);

        RoundRectangle2D course = new RoundRectangle2D.Double(100, 455, 760, 145, 80, 80);
        g2.fill(course);

        g2.setColor(new Color(255, 255, 255, 70));
        g2.setStroke(new BasicStroke(2));
        for (int i = 0; i < 7; i++) {
            g2.drawArc(130 + i * 95, 470, 120, 90, 10, 140);
        }

        g2.setColor(new Color(30, 130, 45));
        g2.fillOval(620, 545, 90, 35);

        g2.setColor(new Color(240, 210, 110));
        g2.fillOval(350, 550, 120, 38);
    }

    private void drawHole(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillOval((int) holeX - 13, (int) holeY - 7, 26, 14);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawLine((int) holeX, (int) holeY, (int) holeX, (int) holeY - 70);

        g2.setColor(new Color(0, 156, 59));
        Path2D.Double flag = new Path2D.Double();
        flag.moveTo(holeX, holeY - 70);
        flag.lineTo(holeX + 55, holeY - 55);
        flag.lineTo(holeX, holeY - 40);
        flag.closePath();
        g2.fill(flag);

        g2.setColor(new Color(255, 223, 0));
        g2.fillOval((int) holeX + 18, (int) holeY - 61, 10, 10);
    }

    private void drawBall(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval((int) ballX - 8, (int) ballY + 7, 18, 6);

        RadialGradientPaint ballPaint = new RadialGradientPaint(
                new Point2D.Double(ballX - 4, ballY - 4),
                16,
                new float[]{0f, 1f},
                new Color[]{Color.WHITE, new Color(210, 210, 210)}
        );

        g2.setPaint(ballPaint);
        g2.fillOval((int) ballX - 8, (int) ballY - 8, 16, 16);

        g2.setColor(new Color(180, 180, 180));
        g2.drawOval((int) ballX - 8, (int) ballY - 8, 16, 16);
    }

    private void drawLegoPlayers(Graphics2D g2) {
        double angle = swinging ? Math.sin(swingAnim) * 0.9 : 0.25;

        if (currentPlayer == 1 && !gameOver) {
            drawLegoGolfer(g2, 145, 530, new Color(255, 203, 5), new Color(0, 39, 118), true, angle, "P1");
            drawLegoGolfer(g2, 90, 560, new Color(255, 203, 5), new Color(0, 156, 59), false, 0.1, "P2");
        } else if (currentPlayer == 2 && !gameOver) {
            drawLegoGolfer(g2, 145, 530, new Color(255, 203, 5), new Color(0, 39, 118), false, 0.1, "P1");
            drawLegoGolfer(g2, 90, 560, new Color(255, 203, 5), new Color(0, 156, 59), true, angle, vsComputer ? "CPU" : "P2");
        } else {
            drawLegoGolfer(g2, 145, 530, new Color(255, 203, 5), new Color(0, 39, 118), false, 0.1, "P1");
            drawLegoGolfer(g2, 90, 560, new Color(255, 203, 5), new Color(0, 156, 59), false, 0.1, vsComputer ? "CPU" : "P2");
        }
    }

    private void drawLegoGolfer(
            Graphics2D g2,
            int x,
            int y,
            Color skin,
            Color shirt,
            boolean active,
            double clubAngle,
            String name
    ) {
        Graphics2D g = (Graphics2D) g2.create();

        if (!active) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        }

        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(x - 20, y + 45, 75, 15);

        g.setColor(skin);
        g.fillRoundRect(x + 5, y - 75, 38, 32, 8, 8);

        g.setColor(Color.BLACK);
        g.fillOval(x + 14, y - 62, 4, 4);
        g.fillOval(x + 30, y - 62, 4, 4);
        g.drawArc(x + 16, y - 58, 16, 10, 200, 140);

        g.setColor(new Color(120, 55, 15));
        g.fillRoundRect(x + 2, y - 82, 44, 13, 5, 5);

        g.setColor(shirt);
        g.fillRoundRect(x, y - 43, 50, 48, 8, 8);

        g.setColor(Color.WHITE);
        g.fillRect(x + 18, y - 36, 14, 30);

        g.setColor(new Color(255, 203, 5));
        g.fillRoundRect(x - 12, y - 35, 15, 42, 8, 8);
        g.fillRoundRect(x + 47, y - 35, 15, 42, 8, 8);

        g.setColor(new Color(30, 30, 160));
        g.fillRoundRect(x + 5, y + 4, 17, 48, 6, 6);
        g.fillRoundRect(x + 28, y + 4, 17, 48, 6, 6);

        g.setColor(Color.BLACK);
        g.fillRoundRect(x + 1, y + 48, 24, 10, 5, 5);
        g.fillRoundRect(x + 25, y + 48, 24, 10, 5, 5);

        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(80, 80, 80));

        int shoulderX = x + 52;
        int shoulderY = y - 23;

        AffineTransform old = g.getTransform();
        g.rotate(clubAngle, shoulderX, shoulderY);
        g.drawLine(shoulderX, shoulderY, shoulderX + 55, shoulderY + 55);

        g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(shoulderX + 55, shoulderY + 55, shoulderX + 73, shoulderY + 55);
        g.setTransform(old);

        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(active ? Color.RED : Color.DARK_GRAY);
        g.drawString(name, x + 8, y - 88);

        if (active) {
            g.setColor(new Color(255, 255, 255, 170));
            g.setStroke(new BasicStroke(2));
            g.drawOval(x - 14, y - 92, 80, 155);
        }

        g.dispose();
    }

    private void drawHUD(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(20, 565, 340, 65, 18, 18);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));

        String mode = vsComputer ? "Mode: Player vs Computer" : "Mode: Two Players";
        g2.drawString(mode, 35, 590);
        g2.drawString("Player 1 Shots: " + shotsP1, 35, 612);
        g2.drawString((vsComputer ? "Computer" : "Player 2") + " Shots: " + shotsP2, 190, 612);

        if (!gameOver) {
            String turn = currentPlayer == 1 ? "Player 1 Turn" : (vsComputer ? "Computer Turn" : "Player 2 Turn");
            g2.setColor(new Color(255, 255, 255, 230));
            g2.fillRoundRect(610, 565, 300, 65, 18, 18);

            g2.setColor(new Color(0, 39, 118));
            g2.drawString(turn, 630, 590);

            g2.setColor(Color.DARK_GRAY);
            g2.drawString("Hold SPACE, release to shoot", 630, 612);
        }

        drawPowerMeter(g2);
    }

    private void drawPowerMeter(Graphics2D g2) {
        int x = 570;
        int y = 25;
        int width = 310;
        int height = 25;

        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(x - 10, y - 5, width + 20, height + 35, 15, 15);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.drawString("Shot Power", x, y + 45);

        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect(x, y, width, height, 12, 12);

        int filled = (int) (width * (power / 100.0));

        GradientPaint powerColor = new GradientPaint(
                x, y, Color.GREEN,
                x + width, y, Color.RED
        );

        g2.setPaint(powerColor);
        g2.fillRoundRect(x, y, filled, height, 12, 12);

        g2.setColor(Color.WHITE);
        g2.drawRoundRect(x, y, width, height, 12, 12);
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 185));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(new Color(255, 203, 5));
        g2.setFont(new Font("Arial", Font.BOLD, 44));
        g2.drawString("Game Over!", 345, 230);

        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.setColor(Color.WHITE);

        String result;

        if (shotsP1 < shotsP2) {
            result = "Player 1 Wins!";
        } else if (shotsP2 < shotsP1) {
            result = vsComputer ? "Computer Wins!" : "Player 2 Wins!";
        } else {
            result = "It is a Tie!";
        }

        g2.drawString(result, 380, 280);
        g2.drawString("Player 1 Shots: " + shotsP1, 365, 325);
        g2.drawString((vsComputer ? "Computer" : "Player 2") + " Shots: " + shotsP2, 365, 360);

        g2.setColor(new Color(0, 156, 59));
        g2.drawString("Click Restart Game to play again", 305, 420);
    }

    private boolean ballIsMoving() {
        return Math.hypot(ballVX, ballVY) > 0.25;
    }

    private void updateBall() {
        ballX += ballVX;
        ballY += ballVY;

        ballVX *= 0.985;
        ballVY *= 0.985;

        if (Math.abs(ballVX) < 0.05) ballVX = 0;
        if (Math.abs(ballVY) < 0.05) ballVY = 0;

        if (ballX < 115) {
            ballX = 115;
            ballVX *= -0.55;
        }

        if (ballX > 850) {
            ballX = 850;
            ballVX *= -0.55;
        }

        if (ballY < 460) {
            ballY = 460;
            ballVY *= -0.55;
        }

        if (ballY > 595) {
            ballY = 595;
            ballVY *= -0.55;
        }

        double distanceToHole = Point2D.distance(ballX, ballY, holeX, holeY);
        double speed = Math.hypot(ballVX, ballVY);

        if (distanceToHole < 17 && speed < 5.8) {
            ballX = holeX;
            ballY = holeY;
            ballVX = 0;
            ballVY = 0;
            nextTurnOrEnd();
        }
    }

    private void nextTurnOrEnd() {
        if (currentPlayer == 1) {
            currentPlayer = 2;
            resetBallForNextPlayer();

            if (vsComputer) {
                Timer cpuTimer = new Timer(900, e -> {
                    computerShot();
                    ((Timer) e.getSource()).stop();
                });
                cpuTimer.setRepeats(false);
                cpuTimer.start();
            }
        } else {
            gameOver = true;
        }
    }

    private void resetBallForNextPlayer() {
        ballX = startX;
        ballY = startY;
        ballVX = 0;
        ballVY = 0;
        power = 0;
        charging = false;
    }

    private void shoot(double shotPower) {
        if (gameOver || ballIsMoving()) return;

        if (currentPlayer == 1) shotsP1++;
        else shotsP2++;

        double dx = holeX - ballX;
        double dy = holeY - ballY;
        double distance = Math.hypot(dx, dy);

        double angle = Math.atan2(dy, dx);

        double speed = 4 + shotPower * 0.22;

        ballVX = Math.cos(angle) * speed;
        ballVY = Math.sin(angle) * speed;

        swinging = true;
        swingAnim = 0;
    }

    private void computerShot() {
        if (gameOver || currentPlayer != 2) return;

        shotsP2++;

        double dx = holeX - ballX;
        double dy = holeY - ballY;

        double angle = Math.atan2(dy, dx);
        angle += Math.toRadians(random.nextDouble() * 12 - 6);

        double distance = Math.hypot(dx, dy);
        double cpuPower = Math.min(95, Math.max(35, distance / 6.5));
        double speed = 4 + cpuPower * 0.22;

        ballVX = Math.cos(angle) * speed;
        ballVY = Math.sin(angle) * speed;

        swinging = true;
        swingAnim = 0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        waveOffset += 2.2;
        cloudOffset += 0.35;

        if (charging && !gameOver && !ballIsMoving()) {
            if (powerUp) {
                power += 1.5;
                if (power >= 100) {
                    power = 100;
                    powerUp = false;
                }
            } else {
                power -= 1.5;
                if (power <= 0) {
                    power = 0;
                    powerUp = true;
                }
            }
        }

        if (swinging) {
            swingAnim += 0.25;
            if (swingAnim > Math.PI) {
                swinging = false;
                swingAnim = 0;
            }
        }

        if (!gameOver) {
            updateBall();

            if (!ballIsMoving() && currentPlayer == 2 && vsComputer) {
                double distanceToHole = Point2D.distance(ballX, ballY, holeX, holeY);
                if (distanceToHole < 20) {
                    gameOver = true;
                }
            }
        }

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) return;

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (!ballIsMoving() && !(vsComputer && currentPlayer == 2)) {
                charging = true;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver) return;

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (charging && !ballIsMoving() && !(vsComputer && currentPlayer == 2)) {
                charging = false;
                shoot(power);
                power = 0;
                powerUp = true;
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Virtual Golf LEGO Brazil Challenge");
            VirtualGolfLegoBrazilChallenge game = new VirtualGolfLegoBrazilChallenge();

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}
