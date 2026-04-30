import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class VirtualGolfChallenge extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VirtualGolfChallenge game = new VirtualGolfChallenge();
            game.setVisible(true);
        });
    }

    public VirtualGolfChallenge() {
        setTitle("Virtual Golf Challenge - Seaside Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(new GolfPanel());
        pack();
        setLocationRelativeTo(null);
    }
}

class GolfPanel extends JPanel implements ActionListener, KeyListener {
    private final int W = 1100;
    private final int H = 700;

    private final Timer timer = new Timer(16, this);
    private final Random rand = new Random();

    private double ballX = 160, ballY = 520;
    private double velX = 0, velY = 0;
    private boolean ballMoving = false;

    private int targetX = 900;
    private int targetY = 470;
    private int targetRadius = 34;

    private int shotPower = 18;
    private int shotAngle = 25; // degrees
    private int wind = 0;

    private int playerTurn = 1;
    private int[] scores = {0, 0};
    private int[] shots = {0, 0};

    private boolean vsComputer = true;
    private boolean awaitingModeSelect = true;
    private boolean shotCharging = false;
    private int charge = 0;
    private int chargeDir = 1;

    private int oceanOffset = 0;
    private int cloudOffset = 0;
    private int seagullX = 120;

    public GolfPanel() {
        setPreferredSize(new Dimension(W, H));
        setBackground(new Color(135, 206, 235));
        setFocusable(true);
        addKeyListener(this);
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        oceanOffset = (oceanOffset + 2) % 120;
        cloudOffset = (cloudOffset + 1) % W;
        seagullX += 3;
        if (seagullX > W + 60) seagullX = -60;

        if (shotCharging) {
            charge += chargeDir * 2;
            if (charge >= 100) {
                charge = 100;
                chargeDir = -1;
            }
            if (charge <= 0) {
                charge = 0;
                chargeDir = 1;
            }
        }

        if (ballMoving) {
            velY += 0.16;
            velX *= 0.995;
            velY *= 0.995;

            ballX += velX;
            ballY += velY;

            if (ballY > 610) {
                ballY = 610;
                velY = -velY * 0.45;
                velX *= 0.9;
                if (Math.abs(velY) < 0.7) velY = 0;
            }

            if (ballX < 25) {
                ballX = 25;
                velX = -velX * 0.7;
            }
            if (ballX > W - 25) {
                ballX = W - 25;
                velX = -velX * 0.7;
            }

            if (Math.abs(velX) < 0.15 && Math.abs(velY) < 0.15 && ballY >= 609) {
                ballMoving = false;
                velX = velY = 0;
                nextTurn();
            }

            double dx = ballX - targetX;
            double dy = ballY - targetY;
            if (Math.sqrt(dx * dx + dy * dy) < targetRadius - 4) {
                scores[playerTurn - 1] += 1;
                resetBallAfterScore();
            }
        }

        repaint();
    }

    private void resetBallAfterScore() {
        ballMoving = false;
        velX = velY = 0;
        ballX = 160;
        ballY = 520;
        targetX = 760 + rand.nextInt(220);
        targetY = 430 + rand.nextInt(70);
        wind = rand.nextInt(11) - 5;
        if (playerTurn == 1) {
            playerTurn = 2;
        } else {
            playerTurn = 1;
        }
    }

    private void nextTurn() {
        shots[playerTurn - 1]++;
        if (playerTurn == 1) {
            playerTurn = 2;
        } else {
            playerTurn = 1;
        }
        ballX = 160;
        ballY = 520;
        wind = rand.nextInt(11) - 5;
        shotPower = 18;
        shotAngle = 25;
        charge = 0;
    }

    private void startShot() {
        if (ballMoving || awaitingModeSelect) return;

        double power = 6 + (charge / 100.0) * 18;
        double angle = Math.toRadians(shotAngle);
        velX = power * Math.cos(angle) + wind * 0.2;
        velY = -power * Math.sin(angle);

        ballMoving = true;
        shotCharging = false;
        charge = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawSky(g2);
        drawSun(g2);
        drawClouds(g2);
        drawOcean(g2);
        drawBeach(g2);
        drawPalmTrees(g2);
        drawFlag(g2);
        drawBall(g2);
        drawUI(g2);
        drawSeagull(g2);

        if (awaitingModeSelect) {
            drawModeOverlay(g2);
        }
    }

    private void drawSky(Graphics2D g2) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(90, 190, 255), 0, H / 2, new Color(180, 240, 255));
        g2.setPaint(sky);
        g2.fillRect(0, 0, W, H / 2);
    }

    private void drawSun(Graphics2D g2) {
        g2.setColor(new Color(255, 230, 90, 180));
        g2.fillOval(860, 40, 120, 120);
        g2.setColor(new Color(255, 245, 160, 100));
        g2.fillOval(840, 20, 160, 160);
    }

    private void drawClouds(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 210));
        for (int i = 0; i < 4; i++) {
            int x = (cloudOffset + i * 280) % (W + 220) - 120;
            int y = 60 + (i % 2) * 40;
            g2.fillOval(x, y, 90, 45);
            g2.fillOval(x + 35, y - 18, 100, 60);
            g2.fillOval(x + 80, y, 95, 48);
        }
    }

    private void drawOcean(Graphics2D g2) {
        GradientPaint sea = new GradientPaint(0, 330, new Color(0, 140, 200), 0, H, new Color(0, 70, 140));
        g2.setPaint(sea);
        g2.fillRect(0, 330, W, H - 330);

        for (int y = 350; y < H; y += 28) {
            for (int x = -oceanOffset; x < W + 120; x += 120) {
                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawArc(x, y, 80, 18, 0, 180);
            }
        }
    }

    private void drawBeach(Graphics2D g2) {
        g2.setColor(new Color(245, 220, 150));
        g2.fillRect(0, 560, W, 140);
        g2.setColor(new Color(230, 200, 130));
        for (int i = 0; i < 35; i++) {
            int x = (i * 37 + oceanOffset * 3) % W;
            int y = 580 + (i % 3) * 12;
            g2.fillOval(x, y, 5, 2);
        }
    }

    private void drawPalmTrees(Graphics2D g2) {
        g2.setColor(new Color(122, 84, 43));
        g2.fillRoundRect(55, 395, 22, 170, 16, 16);
        g2.fillRoundRect(980, 380, 22, 180, 16, 16);

        g2.setColor(new Color(40, 140, 60));
        int[][] leaves = {{30, 390}, {95, 390}, {25, 430}, {100, 430}, {960, 375}, {1040, 375}, {955, 415}, {1045, 415}};
        for (int[] p : leaves) {
            g2.fillArc(p[0], p[1], 80, 50, 20, 140);
        }
    }

    private void drawFlag(Graphics2D g2) {
        g2.setColor(new Color(120, 80, 40));
        g2.fillRect(targetX - 2, targetY - 110, 4, 145);
        g2.setColor(new Color(255, 60, 120));
        g2.fillRoundRect(targetX + 2, targetY - 100, 44, 25, 8, 8);
        g2.setColor(new Color(255, 255, 255, 120));
        g2.drawOval(targetX - targetRadius, targetY - targetRadius, targetRadius * 2, targetRadius * 2);
        g2.setColor(new Color(255, 255, 255, 70));
        g2.fillOval(targetX - targetRadius + 6, targetY - targetRadius + 6, targetRadius * 2 - 12, targetRadius * 2 - 12);
    }

    private void drawBall(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.fillOval((int) ballX - 10, (int) ballY - 10, 20, 20);
        g2.setColor(new Color(210, 210, 210));
        g2.drawOval((int) ballX - 10, (int) ballY - 10, 20, 20);
        g2.setColor(new Color(255, 255, 255, 130));
        g2.fillOval((int) ballX - 6, (int) ballY - 8, 7, 7);
    }

    private void drawSeagull(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 220));
        int y = 170;
        g2.drawArc(seagullX, y, 28, 16, 10, 160);
        g2.drawArc(seagullX + 22, y, 28, 16, 10, 160);
    }

    private void drawUI(Graphics2D g2) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.setColor(new Color(0, 30, 60));
        String mode = vsComputer ? "Mode: Vs Computer" : "Mode: 2 Players";
        g2.drawString(mode, 20, 28);
        g2.drawString("Turn: Player " + playerTurn, 20, 52);
        g2.drawString("Score P1: " + scores[0] + "  Shots: " + shots[0], 20, 76);
        g2.drawString("Score P2: " + scores[1] + "  Shots: " + shots[1], 20, 100);
        g2.drawString("Angle: " + shotAngle + "   Wind: " + wind, 20, 124);

        g2.drawString("Controls: A/D angle, W/S power, SPACE charge/shot, M mode, R reset", 20, 660);

        if (shotCharging) {
            g2.setColor(new Color(255, 255, 255, 190));
            g2.fillRect(20, 140, 150, 18);
            g2.setColor(new Color(255, 100, 80));
            g2.fillRect(20, 140, charge + 1, 18);
            g2.setColor(Color.BLACK);
            g2.drawRect(20, 140, 150, 18);
        }

        if (!awaitingModeSelect && !ballMoving && playerTurn == 2 && vsComputer) {
            g2.setColor(new Color(255, 255, 255, 190));
            g2.fillRoundRect(820, 20, 250, 50, 16, 16);
            g2.setColor(Color.BLACK);
            g2.drawString("Computer ready: press SPACE", 835, 50);
        }
    }

    private void drawModeOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRect(0, 0, W, H);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 34));
        g2.drawString("Virtual Golf Challenge", 355, 245);
        g2.setFont(new Font("SansSerif", Font.BOLD, 26));
        g2.drawString("Press 1 for 2 Players", 405, 320);
        g2.drawString("Press 2 for Vs Computer", 392, 365);
        g2.drawString("Seaside Edition", 460, 415);
    }

    private void computerShot() {
        if (!vsComputer || playerTurn != 2 || ballMoving || awaitingModeSelect) return;
        shotAngle = 20 + rand.nextInt(25);
        shotPower = 14 + rand.nextInt(10);
        charge = 80;
        startShot();
    }

    private void resetGame() {
        ballX = 160;
        ballY = 520;
        velX = velY = 0;
        scores[0] = scores[1] = 0;
        shots[0] = shots[1] = 0;
        playerTurn = 1;
        wind = 0;
        shotAngle = 25;
        shotPower = 18;
        ballMoving = false;
        shotCharging = false;
        awaitingModeSelect = true;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();

        if (awaitingModeSelect) {
            if (k == KeyEvent.VK_1) {
                vsComputer = false;
                awaitingModeSelect = false;
            } else if (k == KeyEvent.VK_2) {
                vsComputer = true;
                awaitingModeSelect = false;
            }
            return;
        }

        if (k == KeyEvent.VK_M) {
            vsComputer = !vsComputer;
            return;
        }
        if (k == KeyEvent.VK_R) {
            resetGame();
            return;
        }

        if (ballMoving) return;

        if (k == KeyEvent.VK_A) shotAngle = Math.max(5, shotAngle - 1);
        if (k == KeyEvent.VK_D) shotAngle = Math.min(80, shotAngle + 1);
        if (k == KeyEvent.VK_W) shotPower = Math.min(30, shotPower + 1);
        if (k == KeyEvent.VK_S) shotPower = Math.max(6, shotPower - 1);

        if (k == KeyEvent.VK_SPACE) {
            if (!shotCharging) {
                shotCharging = true;
                chargeDir = 1;
                charge = 0;
            } else {
                startShot();
            }
        }

        if (vsComputer && playerTurn == 2 && !ballMoving) {
            computerShot();
        }
    }

    @Override public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && shotCharging && !ballMoving) {
            startShot();
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
}