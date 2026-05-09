import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class VirtualGolfChina3DChallenge extends JPanel implements ActionListener, KeyListener {

    private final Timer timer = new Timer(16, this);
    private final Random random = new Random();

    private int width = 1000;
    private int height = 700;

    private double ballX = 250;
    private double ballY = 560;
    private double ballZ = 0;

    private double ballVX = 0;
    private double ballVY = 0;
    private double ballVZ = 0;

    private final double holeX = 760;
    private final double holeY = 360;

    private int currentPlayer = 1;
    private int shotsP1 = 0;
    private int shotsP2 = 0;

    private boolean vsComputer = false;
    private boolean gameOver = false;
    private boolean charging = false;
    private boolean ballMoving = false;

    private int power = 0;
    private int powerDirection = 1;

    private double animationTime = 0;
    private double swingAngle = 0;
    private boolean swinging = false;

    private JButton twoPlayerButton;
    private JButton computerButton;
    private JButton restartButton;

    public VirtualGolfChina3DChallenge() {
        setPreferredSize(new Dimension(width, height));
        setFocusable(true);
        setLayout(null);
        addKeyListener(this);

        twoPlayerButton = createButton("Two Players");
        computerButton = createButton("Play With Computer");
        restartButton = createButton("Restart");

        twoPlayerButton.setBounds(20, 20, 150, 38);
        computerButton.setBounds(180, 20, 190, 38);
        restartButton.setBounds(380, 20, 120, 38);

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

        timer.start();
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(220, 30, 45));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(255, 220, 120), 2));
        return button;
    }

    private void resetGame() {
        ballX = 250;
        ballY = 560;
        ballZ = 0;

        ballVX = 0;
        ballVY = 0;
        ballVZ = 0;

        currentPlayer = 1;
        shotsP1 = 0;
        shotsP2 = 0;

        power = 0;
        charging = false;
        ballMoving = false;
        gameOver = false;
        swinging = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        width = getWidth();
        height = getHeight();

        Graphics2D g2 = (Graphics2D) g;
        enableQuality(g2);

        drawChinaSky(g2);
        drawSun(g2);
        drawClouds(g2);
        drawMountains(g2);
        drawGreatWall(g2);
        drawChineseTemple(g2);
        drawLanterns(g2);
        drawGolfCourse3D(g2);
        drawHole(g2);
        drawAimLine(g2);
        drawPlayers(g2);
        drawBall(g2);
        drawHUD(g2);

        if (gameOver) {
            drawGameOver(g2);
        }
    }

    private void enableQuality(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private void drawChinaSky(Graphics2D g2) {
        GradientPaint sky = new GradientPaint(
                0, 0, new Color(255, 170, 90),
                0, height * 2 / 3, new Color(120, 190, 255)
        );
        g2.setPaint(sky);
        g2.fillRect(0, 0, width, height);

        GradientPaint horizon = new GradientPaint(
                0, height / 2, new Color(255, 225, 150, 150),
                0, height, new Color(255, 120, 90, 0)
        );
        g2.setPaint(horizon);
        g2.fillRect(0, 0, width, height);
    }

    private void drawSun(Graphics2D g2) {
        int sunX = width - 170;
        int sunY = 95;

        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Double(sunX, sunY),
                100,
                new float[]{0f, 1f},
                new Color[]{
                        new Color(255, 250, 160, 220),
                        new Color(255, 180, 70, 0)
                }
        );

        g2.setPaint(glow);
        g2.fillOval(sunX - 100, sunY - 100, 200, 200);

        g2.setColor(new Color(255, 240, 120));
        g2.fillOval(sunX - 38, sunY - 38, 76, 76);
    }

    private void drawClouds(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 180));

        for (int i = 0; i < 5; i++) {
            int baseX = (int) ((animationTime * 12 + i * 220) % (width + 250)) - 160;
            int baseY = 80 + i * 35;

            g2.fillOval(baseX, baseY, 90, 40);
            g2.fillOval(baseX + 45, baseY - 20, 100, 55);
            g2.fillOval(baseX + 110, baseY, 90, 42);
            g2.fillOval(baseX + 35, baseY + 15, 140, 35);
        }
    }

    private void drawMountains(Graphics2D g2) {
        Path2D far = new Path2D.Double();
        far.moveTo(0, 330);
        far.lineTo(100, 210);
        far.lineTo(210, 330);
        far.lineTo(340, 180);
        far.lineTo(520, 335);
        far.lineTo(650, 220);
        far.lineTo(820, 335);
        far.lineTo(width, 190);
        far.lineTo(width, 420);
        far.lineTo(0, 420);
        far.closePath();

        GradientPaint mountainPaint = new GradientPaint(
                0, 180, new Color(75, 105, 130),
                0, 420, new Color(35, 80, 80)
        );
        g2.setPaint(mountainPaint);
        g2.fill(far);

        g2.setColor(new Color(255, 255, 255, 120));
        g2.fillPolygon(new int[]{340, 300, 370}, new int[]{180, 245, 240}, 3);
        g2.fillPolygon(new int[]{width, width - 65, width}, new int[]{190, 265, 255}, 3);
    }

    private void drawGreatWall(Graphics2D g2) {
        g2.setStroke(new BasicStroke(14, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(120, 90, 60));

        Path2D wall = new Path2D.Double();
        wall.moveTo(0, 360);
        wall.curveTo(160, 315, 260, 390, 400, 345);
        wall.curveTo(550, 295, 710, 390, 1000, 315);
        g2.draw(wall);

        g2.setStroke(new BasicStroke(3));
        g2.setColor(new Color(70, 50, 35));

        for (int i = 0; i < width; i += 55) {
            int y = (int) (345 + Math.sin(i * 0.02 + animationTime) * 24);
            g2.fillRect(i, y - 28, 28, 35);
            g2.setColor(new Color(160, 120, 75));
            g2.fillRect(i + 4, y - 24, 20, 8);
            g2.setColor(new Color(70, 50, 35));
        }
    }

    private void drawChineseTemple(Graphics2D g2) {
        int x = 90;
        int y = 300;

        g2.setColor(new Color(120, 30, 30));
        g2.fillRect(x + 35, y + 75, 130, 95);

        g2.setColor(new Color(180, 30, 30));
        g2.fillRect(x + 55, y + 95, 90, 75);

        g2.setColor(new Color(80, 30, 20));
        g2.fillRect(x + 85, y + 125, 30, 45);

        g2.setColor(new Color(220, 160, 50));
        for (int i = 0; i < 4; i++) {
            g2.fillRect(x + 45 + i * 30, y + 85, 10, 85);
        }

        Path2D roof1 = new Path2D.Double();
        roof1.moveTo(x + 15, y + 85);
        roof1.quadTo(x + 100, y + 35, x + 185, y + 85);
        roof1.lineTo(x + 165, y + 105);
        roof1.quadTo(x + 100, y + 75, x + 35, y + 105);
        roof1.closePath();

        g2.setColor(new Color(40, 120, 70));
        g2.fill(roof1);

        g2.setColor(new Color(255, 210, 80));
        g2.setStroke(new BasicStroke(4));
        g2.draw(roof1);

        Path2D roof2 = new Path2D.Double();
        roof2.moveTo(x + 40, y + 45);
        roof2.quadTo(x + 100, y + 5, x + 160, y + 45);
        roof2.lineTo(x + 145, y + 62);
        roof2.quadTo(x + 100, y + 42, x + 55, y + 62);
        roof2.closePath();

        g2.setColor(new Color(35, 100, 65));
        g2.fill(roof2);
        g2.setColor(new Color(255, 215, 95));
        g2.draw(roof2);
    }

    private void drawLanterns(Graphics2D g2) {
        for (int i = 0; i < 7; i++) {
            int x = 90 + i * 130;
            int y = 105 + (int) (Math.sin(animationTime * 2 + i) * 8);

            g2.setStroke(new BasicStroke(2));
            g2.setColor(new Color(90, 30, 20));
            g2.drawLine(x, 0, x, y);

            GradientPaint lanternPaint = new GradientPaint(
                    x - 15, y, new Color(255, 60, 45),
                    x + 15, y + 45, new Color(180, 0, 20)
            );

            g2.setPaint(lanternPaint);
            g2.fillOval(x - 18, y, 36, 45);

            g2.setColor(new Color(255, 220, 80));
            g2.drawOval(x - 18, y, 36, 45);
            g2.drawLine(x - 15, y + 14, x + 15, y + 14);
            g2.drawLine(x - 15, y + 31, x + 15, y + 31);

            g2.setColor(new Color(255, 230, 90, 150));
            g2.fillOval(x - 8, y + 12, 16, 20);
        }
    }

    private void drawGolfCourse3D(Graphics2D g2) {
        Path2D course = new Path2D.Double();
        course.moveTo(80, height);
        course.lineTo(325, 330);
        course.lineTo(675, 330);
        course.lineTo(width - 80, height);
        course.closePath();

        GradientPaint fairway = new GradientPaint(
                0, 330, new Color(90, 210, 80),
                0, height, new Color(15, 120, 45)
        );
        g2.setPaint(fairway);
        g2.fill(course);

        g2.setColor(new Color(255, 230, 140, 100));
        g2.setStroke(new BasicStroke(4));

        for (int i = 0; i < 10; i++) {
            double t = i / 10.0;
            int y = (int) (345 + t * t * 330);
            int left = (int) (325 - t * 245);
            int right = (int) (675 + t * 245);
            g2.drawLine(left, y, right, y);
        }

        g2.setColor(new Color(25, 150, 50, 120));
        for (int i = 0; i < 18; i++) {
            int y = 370 + i * 20;
            int grassHeight = 8 + i;
            int left = 300 - i * 14;
            int right = 700 + i * 14;

            for (int x = left; x < right; x += 32) {
                int wave = (int) (Math.sin(animationTime * 4 + x * 0.04) * 3);
                g2.drawLine(x, y, x + 7, y - grassHeight + wave);
            }
        }

        drawSandTrap(g2, 610, 520, 120, 55);
        drawSandTrap(g2, 305, 430, 90, 42);

        drawPond(g2);
    }

    private void drawSandTrap(Graphics2D g2, int x, int y, int w, int h) {
        GradientPaint sand = new GradientPaint(
                x, y, new Color(255, 235, 160),
                x, y + h, new Color(210, 170, 90)
        );
        g2.setPaint(sand);
        g2.fillOval(x, y, w, h);

        g2.setColor(new Color(170, 130, 70, 100));
        g2.drawOval(x + 8, y + 8, w - 16, h - 16);
        g2.drawArc(x + 20, y + 15, w - 40, h - 20, 10, 130);
    }

    private void drawPond(Graphics2D g2) {
        int x = 405;
        int y = 505;
        int w = 125;
        int h = 52;

        GradientPaint water = new GradientPaint(
                x, y, new Color(80, 210, 255),
                x, y + h, new Color(10, 100, 190)
        );

        g2.setPaint(water);
        g2.fillOval(x, y, w, h);

        g2.setColor(new Color(255, 255, 255, 140));
        for (int i = 0; i < 4; i++) {
            int yy = y + 13 + i * 8;
            g2.drawArc(x + 15, yy, 80, 12, 0, 180);
        }
    }

    private void drawHole(Graphics2D g2) {
        double scale = perspectiveScale(holeY);

        int hx = (int) holeX;
        int hy = (int) holeY;

        g2.setColor(new Color(20, 20, 20));
        g2.fillOval(hx - 12, hy - 5, 24, 10);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(hx, hy, hx, hy - 75);

        Path2D flag = new Path2D.Double();
        flag.moveTo(hx, hy - 75);
        flag.lineTo(hx + 55 * scale, hy - 58);
        flag.lineTo(hx, hy - 42);
        flag.closePath();

        g2.setColor(new Color(220, 0, 35));
        g2.fill(flag);

        g2.setColor(new Color(255, 220, 90));
        g2.draw(flag);
    }

    private void drawAimLine(Graphics2D g2) {
        if (ballMoving || gameOver) return;

        if (vsComputer && currentPlayer == 2) return;

        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{10, 10}, 0));

        g2.setColor(new Color(255, 255, 255, 180));
        g2.drawLine((int) ballX, (int) ballY, (int) holeX, (int) holeY);

        g2.setStroke(new BasicStroke(1));
    }

    private void drawPlayers(Graphics2D g2) {
        if (currentPlayer == 1) {
            drawRealisticPlayer(g2, (int) ballX - 75, (int) ballY + 8, true,
                    new Color(20, 80, 190), new Color(245, 210, 170), "Player 1");
            drawRealisticPlayer(g2, 130, 610, false,
                    new Color(180, 40, 60), new Color(235, 190, 145), "Player 2");
        } else {
            drawRealisticPlayer(g2, 100, 610, false,
                    new Color(20, 80, 190), new Color(245, 210, 170), "Player 1");
            drawRealisticPlayer(g2, (int) ballX - 75, (int) ballY + 8, true,
                    new Color(180, 40, 60), new Color(235, 190, 145),
                    vsComputer ? "Computer" : "Player 2");
        }
    }

    private void drawRealisticPlayer(
            Graphics2D g2,
            int x,
            int y,
            boolean active,
            Color shirt,
            Color skin,
            String name
    ) {
        Graphics2D p = (Graphics2D) g2.create();
        p.translate(x, y);

        double bodyBob = Math.sin(animationTime * 5) * 1.5;

        if (active) {
            swingAngle = swinging ? Math.sin(animationTime * 15) * 0.9 : Math.sin(animationTime * 2) * 0.15;
        } else {
            swingAngle = Math.sin(animationTime * 1.5) * 0.08;
        }

        p.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        p.setColor(new Color(0, 0, 0, 55));
        p.fillOval(-12, 45, 70, 14);

        p.translate(0, bodyBob);

        p.setColor(new Color(35, 35, 40));
        p.fillRoundRect(5, 12, 12, 42, 8, 8);
        p.fillRoundRect(31, 12, 12, 42, 8, 8);

        p.setColor(new Color(35, 35, 35));
        p.fillOval(1, 50, 22, 8);
        p.fillOval(28, 50, 22, 8);

        GradientPaint shirtPaint = new GradientPaint(
                0, -36, shirt.brighter(),
                42, 20, shirt.darker()
        );
        p.setPaint(shirtPaint);
        p.fillRoundRect(0, -35, 48, 55, 16, 16);

        p.setColor(new Color(255, 255, 255, 70));
        p.fillRoundRect(7, -29, 14, 42, 10, 10);

        p.setColor(skin);
        p.fillOval(6, -82, 36, 38);

        p.setColor(new Color(70, 40, 25));
        p.fillArc(5, -86, 38, 34, 0, 180);
        p.fillOval(3, -72, 8, 17);
        p.fillOval(37, -72, 8, 17);

        p.setColor(Color.WHITE);
        p.fillOval(15, -67, 7, 5);
        p.fillOval(28, -67, 7, 5);

        p.setColor(Color.BLACK);
        p.fillOval(17, -66, 3, 3);
        p.fillOval(30, -66, 3, 3);

        p.setColor(new Color(180, 70, 70));
        p.drawArc(17, -55, 16, 7, 200, 140);

        p.setColor(new Color(30, 30, 30));
        p.fillRoundRect(3, -87, 42, 8, 8, 8);
        p.setColor(new Color(220, 0, 30));
        p.fillRoundRect(0, -94, 48, 10, 8, 8);

        AffineTransform old = p.getTransform();

        p.translate(22, -20);
        p.rotate(-0.5 + swingAngle);

        p.setColor(skin);
        p.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        p.drawLine(0, 0, 28, 14);

        p.setColor(new Color(60, 40, 25));
        p.setStroke(new BasicStroke(4));
        p.drawLine(27, 14, 64, 62);

        p.setColor(new Color(190, 190, 190));
        p.setStroke(new BasicStroke(6));
        p.drawLine(64, 62, 79, 62);

        p.setTransform(old);

        p.setColor(skin);
        p.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        p.drawLine(4, -18, -12, 3);

        p.setColor(Color.WHITE);
        p.setFont(new Font("Arial", Font.BOLD, 13));

        FontMetrics fm = p.getFontMetrics();
        int textWidth = fm.stringWidth(name);

        p.setColor(new Color(0, 0, 0, 130));
        p.fillRoundRect(24 - textWidth / 2 - 8, -122, textWidth + 16, 22, 12, 12);

        p.setColor(active ? new Color(255, 230, 80) : Color.WHITE);
        p.drawString(name, 24 - textWidth / 2, -106);

        if (active) {
            p.setColor(new Color(255, 230, 80, 160));
            p.setStroke(new BasicStroke(3));
            p.drawOval(-12, -105, 72, 165);
        }

        p.dispose();
    }

    private void drawBall(Graphics2D g2) {
        double scale = perspectiveScale(ballY);
        int radius = (int) (10 * scale + 3);

        int bx = (int) ballX;
        int by = (int) (ballY - ballZ);

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(bx - radius, (int) ballY + 4, radius * 2, radius / 2 + 3);

        RadialGradientPaint ballPaint = new RadialGradientPaint(
                new Point2D.Double(bx - radius / 3.0, by - radius / 3.0),
                radius * 2,
                new float[]{0f, 0.5f, 1f},
                new Color[]{
                        Color.WHITE,
                        new Color(230, 240, 255),
                        new Color(120, 130, 150)
                }
        );

        g2.setPaint(ballPaint);
        g2.fillOval(bx - radius, by - radius, radius * 2, radius * 2);

        g2.setColor(new Color(80, 80, 90, 120));
        for (int i = -1; i <= 1; i++) {
            g2.drawArc(bx - radius + 4, by - radius + 5 + i * 4, radius * 2 - 8, radius, 0, 180);
        }
    }

    private double perspectiveScale(double y) {
        double t = (y - 330) / (height - 330.0);
        t = Math.max(0, Math.min(1, t));
        return 0.45 + t * 1.15;
    }

    private void drawHUD(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(20, 70, 330, 120, 18, 18);

        g2.setColor(new Color(255, 225, 120));
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("Virtual Golf China 3D Challenge", 38, 100);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 15));
        g2.drawString("Mode: " + (vsComputer ? "Player vs Computer" : "Two Players"), 38, 128);
        g2.drawString("Turn: " + getCurrentPlayerName(), 38, 150);
        g2.drawString("Shots - Player 1: " + shotsP1 + "   " + (vsComputer ? "Computer" : "Player 2") + ": " + shotsP2, 38, 172);

        drawPowerMeter(g2);

        g2.setColor(new Color(0, 0, 0, 145));
        g2.fillRoundRect(width - 330, 25, 295, 95, 18, 18);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 15));
        g2.drawString("Controls:", width - 305, 55);
        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        g2.drawString("Hold SPACE to charge power", width - 305, 78);
        g2.drawString("Release SPACE to hit the golf ball", width - 305, 100);
    }

    private void drawPowerMeter(Graphics2D g2) {
        int x = 38;
        int y = 207;
        int w = 270;
        int h = 25;

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(x, y, w, h, 14, 14);

        GradientPaint powerPaint;

        if (power < 40) {
            powerPaint = new GradientPaint(x, y, Color.GREEN, x + w, y, Color.YELLOW);
        } else if (power < 75) {
            powerPaint = new GradientPaint(x, y, Color.YELLOW, x + w, y, Color.ORANGE);
        } else {
            powerPaint = new GradientPaint(x, y, Color.ORANGE, x + w, y, Color.RED);
        }

        g2.setPaint(powerPaint);
        g2.fillRoundRect(x, y, (int) (w * power / 100.0), h, 14, 14);

        g2.setColor(Color.WHITE);
        g2.drawRoundRect(x, y, w, h, 14, 14);

        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString("Power: " + power + "%", x + 92, y + 18);
    }

    private String getCurrentPlayerName() {
        if (currentPlayer == 1) return "Player 1";
        return vsComputer ? "Computer" : "Player 2";
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 185));
        g2.fillRect(0, 0, width, height);

        g2.setColor(new Color(255, 230, 100));
        g2.setFont(new Font("Arial", Font.BOLD, 48));
        drawCenteredString(g2, "GAME OVER", height / 2 - 80);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 25));

        String result;

        if (shotsP1 < shotsP2) {
            result = "Player 1 Wins!";
        } else if (shotsP2 < shotsP1) {
            result = vsComputer ? "Computer Wins!" : "Player 2 Wins!";
        } else {
            result = "It is a Tie!";
        }

        drawCenteredString(g2, result, height / 2 - 25);
        drawCenteredString(g2, "Player 1 Shots: " + shotsP1, height / 2 + 25);
        drawCenteredString(g2, (vsComputer ? "Computer" : "Player 2") + " Shots: " + shotsP2, height / 2 + 60);

        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        drawCenteredString(g2, "Click Restart to play again", height / 2 + 115);
    }

    private void drawCenteredString(Graphics2D g2, String text, int y) {
        FontMetrics fm = g2.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        g2.drawString(text, x, y);
    }

    private void updateGame() {
        animationTime += 0.016;

        if (charging && !ballMoving && !gameOver) {
            power += powerDirection * 2;

            if (power >= 100) {
                power = 100;
                powerDirection = -1;
            }

            if (power <= 0) {
                power = 0;
                powerDirection = 1;
            }
        }

        if (swinging) {
            if (Math.abs(Math.sin(animationTime * 15)) < 0.1 && ballMoving) {
                swinging = false;
            }
        }

        updateBall();

        if (!gameOver && vsComputer && currentPlayer == 2 && !ballMoving && !charging) {
            Timer computerDelay = new Timer(900, e -> {
                if (!gameOver && vsComputer && currentPlayer == 2 && !ballMoving) {
                    computerShot();
                }
            });
            computerDelay.setRepeats(false);
            computerDelay.start();
        }
    }

    private void updateBall() {
        if (!ballMoving) return;

        ballX += ballVX;
        ballY += ballVY;
        ballZ += ballVZ;

        ballVZ -= 0.25;

        if (ballZ < 0) {
            ballZ = 0;
            ballVZ *= -0.35;
        }

        ballVX *= 0.986;
        ballVY *= 0.986;

        if (ballX < 95) {
            ballX = 95;
            ballVX *= -0.55;
        }

        if (ballX > width - 95) {
            ballX = width - 95;
            ballVX *= -0.55;
        }

        if (ballY < 335) {
            ballY = 335;
            ballVY *= -0.55;
        }

        if (ballY > height - 45) {
            ballY = height - 45;
            ballVY *= -0.55;
        }

        double speed = Math.sqrt(ballVX * ballVX + ballVY * ballVY);

        double distanceToHole = Point2D.distance(ballX, ballY, holeX, holeY);

        if (distanceToHole < 18 && speed < 5.8 && ballZ < 8) {
            ballX = holeX;
            ballY = holeY;
            ballZ = 0;
            ballVX = 0;
            ballVY = 0;
            ballVZ = 0;
            ballMoving = false;

            nextTurnOrEnd();
            return;
        }

        if (speed < 0.18 && Math.abs(ballVZ) < 0.25 && ballZ == 0) {
            ballVX = 0;
            ballVY = 0;
            ballVZ = 0;
            ballMoving = false;
            nextTurnOrEnd();
        }
    }

    private void shootBall() {
        if (ballMoving || gameOver) return;
        if (vsComputer && currentPlayer == 2) return;

        double dx = holeX - ballX;
        double dy = holeY - ballY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance == 0) return;

        dx /= distance;
        dy /= distance;

        double force = 4.0 + power * 0.18;

        ballVX = dx * force;
        ballVY = dy * force;
        ballVZ = 2.5 + power * 0.04;

        addShot();

        ballMoving = true;
        swinging = true;
        power = 0;
    }

    private void computerShot() {
        double dx = holeX - ballX;
        double dy = holeY - ballY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance == 0) return;

        double angle = Math.atan2(dy, dx);

        double error = Math.toRadians(-6 + random.nextDouble() * 12);
        angle += error;

        double force = Math.min(18, Math.max(7, distance / 34));

        ballVX = Math.cos(angle) * force;
        ballVY = Math.sin(angle) * force;
        ballVZ = 3 + random.nextDouble() * 2.5;

        addShot();

        ballMoving = true;
        swinging = true;
    }

    private void addShot() {
        if (currentPlayer == 1) {
            shotsP1++;
        } else {
            shotsP2++;
        }
    }

    private void nextTurnOrEnd() {
        double distanceToHole = Point2D.distance(ballX, ballY, holeX, holeY);

        if (distanceToHole < 20) {
            if (currentPlayer == 1) {
                currentPlayer = 2;
                resetBallForNextPlayer();
            } else {
                gameOver = true;
            }
        } else {
            currentPlayer = currentPlayer == 1 ? 2 : 1;
        }
    }

    private void resetBallForNextPlayer() {
        ballX = 250;
        ballY = 560;
        ballZ = 0;

        ballVX = 0;
        ballVY = 0;
        ballVZ = 0;

        power = 0;
        ballMoving = false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateGame();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) return;

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (!ballMoving && !(vsComputer && currentPlayer == 2)) {
                charging = true;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver) return;

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (charging) {
                charging = false;
                shootBall();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Virtual Golf China 3D Challenge");

            VirtualGolfChina3DChallenge game = new VirtualGolfChina3DChallenge();

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(true);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}
