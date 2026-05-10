import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class VirtualGolfChallenge extends JFrame {
    private static final int WIDTH = 900;
    private static final int HEIGHT = 650;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String mode = (String) JOptionPane.showInputDialog(
                    null, "Select Game Mode:", "Virtual Golf Challenge",
                    JOptionPane.PLAIN_MESSAGE, null,
                    new String[]{"2 Players", "1 Player vs Computer"}, "2 Players");

            boolean isVsComputer = mode != null && mode.equals("1 Player vs Computer");
            new VirtualGolfChallenge(isVsComputer);
        });
    }

    public VirtualGolfChallenge(boolean isVsComputer) {
        setTitle("🌴 Seaside Golf Challenge");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);

        add(new GolfPanel(isVsComputer));
        setVisible(true);
    }
}

class GolfPanel extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
    // Game State
    private enum State { AIMING, MOVING, HOLE_MADE, NEXT_HOLE }
    private State state = State.AIMING;

    // Constants
    private final int GREEN_X = 350, GREEN_Y = 250, GREEN_W = 400, GREEN_H = 250;
    private final int HOLE_RADIUS = 12;
    private final int BALL_RADIUS = 8;
    private final double FRICTION = 0.975;
    private final double MAX_POWER = 15.0;

    // Objects
    private Ball ball;
    private Hole hole;
    private Player[] players;
    private int currentPlayer = 0;
    private boolean isVsComputer;
    private Random rand = new Random();

    // Animation & Input
    private Timer gameTimer;
    private double waveOffset = 0;
    private Point dragStart = null;
    private Point dragCurrent = null;
    private int aiShotDelay = 0;

    // UI
    private Font bigFont = new Font("SansSerif", Font.BOLD, 22);
    private Font smallFont = new Font("SansSerif", Font.PLAIN, 16);

    public GolfPanel(boolean isVsComputer) {
        this.isVsComputer = isVsComputer;
        players = new Player[2];
        players[0] = new Player("Player 1", !isVsComputer);
        players[1] = new Player(isVsComputer ? "Computer" : "Player 2", isVsComputer);

        resetHole(0);

        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();

        addMouseListener(this);
        addMouseMotionListener(this);
        setBackground(Color.CYAN);
        setFocusable(true);
    }

    private void resetHole(int holeIndex) {
        // Randomize ball and hole positions within the green
        int margin = 30;
        ball = new Ball(
                GREEN_X + margin + rand.nextInt(GREEN_W - 2 * margin),
                GREEN_Y + margin + rand.nextInt(GREEN_H - 2 * margin)
        );

        hole = new Hole(
                GREEN_X + margin + rand.nextInt(GREEN_W - 2 * margin),
                GREEN_Y + margin + rand.nextInt(GREEN_H - 2 * margin),
                HOLE_RADIUS
        );

        // Ensure hole isn't too close to ball
        while (Math.hypot(ball.x - hole.x, ball.y - hole.y) < 100) {
            hole.x = GREEN_X + margin + rand.nextInt(GREEN_W - 2 * margin);
            hole.y = GREEN_Y + margin + rand.nextInt(GREEN_H - 2 * margin);
        }

        state = State.AIMING;
        aiShotDelay = 60; // 1 second delay for AI
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Sky
        g2.setPaint(new GradientPaint(0, 0, new Color(25, 118, 210), 0, 200, new Color(135, 206, 250)));
        g2.fillRect(0, 0, getWidth(), 200);

        // 2. Sun
        g2.setColor(new Color(255, 223, 0));
        g2.fillOval(750, 30, 60, 60);
        g2.setColor(new Color(255, 255, 200, 80));
        g2.fillOval(740, 20, 80, 80);

        // 3. Ocean with animated waves
        g2.setColor(new Color(30, 80, 160));
        g2.fillRect(0, 200, getWidth(), 50);

        g2.setColor(new Color(100, 180, 255, 150));
        for (int x = 0; x < getWidth(); x += 15) {
            double y = 205 + Math.sin((x + waveOffset) * 0.05) * 8;
            g2.drawLine(x, (int)y, x, (int)y + 20);
        }

        // 4. Sand
        g2.setPaint(new GradientPaint(0, 240, new Color(235, 195, 140), 0, 300, new Color(245, 222, 175)));
        g2.fillRect(0, 240, getWidth(), HEIGHT - 240);

        // 5. Palm Trees
        drawPalmTree(g2, 100, 480);
        drawPalmTree(g2, 800, 450);
        drawPalmTree(g2, 450, 550);

        // 6. Putting Green
        g2.setColor(new Color(40, 120, 50));
        g2.fillRoundRect(GREEN_X, GREEN_Y, GREEN_W, GREEN_H, 40, 40);
        g2.setColor(new Color(50, 140, 60));
        g2.fillRoundRect(GREEN_X + 10, GREEN_Y + 10, GREEN_W - 20, GREEN_H - 20, 30, 30);

        // 7. Hole & Flag
        g2.setColor(new Color(20, 20, 20));
        g2.fillOval(hole.x - hole.r, hole.y - hole.r, hole.r * 2, hole.r * 2);
        // Flag pole
        g2.setStroke(new BasicStroke(2));
        g2.setColor(Color.GRAY);
        g2.drawLine(hole.x, hole.y, hole.x, hole.y - 45);
        // Flag
        g2.setColor(Color.RED);
        g2.fillPolygon(new int[]{hole.x, hole.x + 20, hole.x}, new int[]{hole.y - 45, hole.y - 35, hole.y - 25}, 3);

        // 8. Aim Line & Power Meter
        if (state == State.AIMING && dragStart != null && dragCurrent != null) {
            double dx = dragStart.x - dragCurrent.x;
            double dy = dragStart.y - dragCurrent.y;
            double power = Math.min(Math.hypot(dx, dy) / 10.0, MAX_POWER);

            g2.setStroke(new BasicStroke(3));
            g2.setColor(Color.YELLOW);
            g2.drawLine((int) ball.x, (int) ball.y, (int)(ball.x + dx * 0.5), (int)(ball.y + dy * 0.5));

            // Power meter
            int meterW = 150, meterH = 15;
            int mx = getWidth() / 2 - meterW / 2, my = HEIGHT - 40;
            g2.setColor(Color.DARK_GRAY);
            g2.fillRoundRect(mx, my, meterW, meterH, 5, 5);
            g2.setColor(new Color(0, 200, 0));
            g2.fillRoundRect(mx, my, (int)(meterW * (power / MAX_POWER)), meterH, 5, 5);
            g2.setColor(Color.WHITE);
            g2.setFont(smallFont);
            g2.drawString("POWER", mx, my - 5);
        }

        // 9. Ball
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillOval((int) (ball.x - BALL_RADIUS + 2), (int) (ball.y - BALL_RADIUS + 3), BALL_RADIUS * 2, BALL_RADIUS * 2);
        g2.setColor(Color.WHITE);
        g2.fillOval((int) (ball.x - BALL_RADIUS), (int) (ball.y - BALL_RADIUS), BALL_RADIUS * 2, BALL_RADIUS * 2);

        // 10. UI Overlay
        drawUI(g2);
    }

    private void drawPalmTree(Graphics2D g2, int x, int y) {
        // Trunk
        g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(139, 90, 43));
        g2.drawLine(x, y, x + 15, y - 80);
        g2.drawLine(x, y, x - 5, y - 70);

        // Leaves
        g2.setColor(new Color(30, 130, 30));
        int[] xs = {x+15, x+40, x+20, x, x-20, x-30};
        int[] ys = {y-80, y-90, y-60, y-75, y-60, y-85};
        g2.fillPolygon(xs, ys, 6);
        xs = new int[]{x+15, x+35, x+25, x+5, x-10};
        ys = new int[]{y-80, y-70, y-50, y-55, y-70};
        g2.fillPolygon(xs, ys, 5);
    }

    private void drawUI(Graphics2D g2) {
        g2.setFont(bigFont);
        g2.setColor(Color.WHITE);
        g2.drawString("⛳ Seaside Golf Challenge", 20, 35);

        g2.setFont(smallFont);
        g2.setColor(Color.WHITE);
        g2.drawString(players[0].name + ": " + players[0].score + " strokes", 20, 65);
        g2.drawString(players[1].name + ": " + players[1].score + " strokes", 20, 90);

        String turnText = "🎯 " + players[currentPlayer].name + "'s Turn";
        if (state == State.AIMING && players[currentPlayer].isComputer) turnText += " (Computing...)";
        g2.drawString(turnText, 20, 125);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        waveOffset += 2;

        if (state == State.MOVING) {
            // Physics
            ball.vx *= FRICTION;
            ball.vy *= FRICTION;
            ball.x += ball.vx;
            ball.y += ball.vy;

            // Green bounds bounce
            if (ball.x < GREEN_X + BALL_RADIUS) { ball.x = GREEN_X + BALL_RADIUS; ball.vx *= -0.5; }
            if (ball.x > GREEN_X + GREEN_W - BALL_RADIUS) { ball.x = GREEN_X + GREEN_W - BALL_RADIUS; ball.vx *= -0.5; }
            if (ball.y < GREEN_Y + BALL_RADIUS) { ball.y = GREEN_Y + BALL_RADIUS; ball.vy *= -0.5; }
            if (ball.y > GREEN_Y + GREEN_H - BALL_RADIUS) { ball.y = GREEN_Y + GREEN_H - BALL_RADIUS; ball.vy *= -0.5; }

            // Stop check
            if (Math.abs(ball.vx) < 0.1 && Math.abs(ball.vy) < 0.1) {
                ball.vx = 0; ball.vy = 0;
                checkHole();
            }
        }

        if (state == State.HOLE_MADE) {
            // Switch player after short delay
            if (--aiShotDelay <= 0) {
                currentPlayer = 1 - currentPlayer;
                resetHole(0); // Simplified: always new random layout per shot
            }
        }

        // AI Logic
        if (state == State.AIMING && players[currentPlayer].isComputer) {
            if (aiShotDelay <= 0) {
                performAIShot();
            } else {
                aiShotDelay--;
            }
        }

        repaint();
    }

    private void checkHole() {
        double dist = Math.hypot(ball.x - hole.x, ball.y - hole.y);
        if (dist < hole.r) {
            players[currentPlayer].score++;
            JOptionPane.showMessageDialog(this,
                    players[currentPlayer].name + " made the hole in " + players[currentPlayer].score + " strokes!",
                    "🏆 Hole Complete!", JOptionPane.INFORMATION_MESSAGE);
            state = State.HOLE_MADE;
            aiShotDelay = 60;
        }
    }

    private void performAIShot() {
        // Calculate vector to hole
        double dx = hole.x - ball.x;
        double dy = hole.y - ball.y;
        double dist = Math.hypot(dx, dy);

        // Add human-like error
        double errorX = (rand.nextDouble() - 0.5) * 30;
        double errorY = (rand.nextDouble() - 0.5) * 30;

        double targetX = dx + errorX;
        double targetY = dy + errorY;

        // Scale to power
        double power = Math.min(dist / 25.0, MAX_POWER);
        if (dist < 50) power = Math.min(dist / 15.0, MAX_POWER);

        double angle = Math.atan2(targetY, targetX);
        ball.vx = Math.cos(angle) * power;
        ball.vy = Math.sin(angle) * power;

        state = State.MOVING;
    }

    // Mouse Handling
    @Override
    public void mousePressed(MouseEvent e) {
        if (state == State.AIMING && !players[currentPlayer].isComputer) {
            dragStart = e.getPoint();
            dragCurrent = e.getPoint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (state == State.AIMING && dragStart != null && !players[currentPlayer].isComputer) {
            dragCurrent = e.getPoint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (state == State.AIMING && dragStart != null && !players[currentPlayer].isComputer) {
            double dx = dragStart.x - e.getX();
            double dy = dragStart.y - e.getY();
            double power = Math.min(Math.hypot(dx, dy) / 10.0, MAX_POWER);

            if (power > 0.5) {
                double angle = Math.atan2(dy, dx);
                ball.vx = Math.cos(angle) * power;
                ball.vy = Math.sin(angle) * power;
                state = State.MOVING;
            }
            dragStart = null;
            dragCurrent = null;
        }
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}

    // Helper Classes
    static class Ball {
        double x, y, vx, vy;
        Ball(int x, int y) { this.x = x; this.y = y; }
    }

    static class Hole {
        int x, y, r;
        Hole(int x, int y, int r) { this.x = x; this.y = y; this.r = r; }
    }

    static class Player {
        String name;
        int score = 0;
        boolean isComputer;
        Player(String name, boolean isComputer) { this.name = name; this.isComputer = isComputer; }
    }
}