import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.Timer;
import java.awt.geom.AffineTransform;

public class ExtremeMountainBikeRacingGame extends JPanel implements ActionListener, KeyListener {

    // ============ CONFIG ============
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;
    private static final int FPS = 60;
    private static final int DELAY = 1000 / FPS;

    // Physics
    private static final double GRAVITY = 0.4;
    private static final double FRICTION = 0.98;
    private static final double ACCELERATION = 0.25;
    private static final double MAX_SPEED = 12.0;
    private static final double JUMP_FORCE = -9.0;

    // Game state
    private Timer timer;
    private boolean running = false;

    // Mode
    private GameMode mode = GameMode.VS_COMPUTER; // or TWO_PLAYERS

    // Camera / scrolling
    private double cameraX = 0;

    // Track: simple piecewise-linear terrain
    private double[] trackX;
    private double[] trackY;
    private int trackSegments;

    // Players
    private Player player1;
    private Player player2;

    // Keys
    private Set<Integer> keysPressed = new HashSet<>();

    // UI
    private JLabel statusLabel;
    private JFrame frame;

    private enum GameMode {
        VS_COMPUTER,
        TWO_PLAYERS
    }

    // ============ MAIN ============
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExtremeMountainBikeRacingGame::new);
    }

    public ExtremeMountainBikeRacingGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // sky blue
        setFocusable(true);
        addKeyListener(this);

        initTrack();
        initPlayers();

        // UI frame
        frame = new JFrame("Extreme Mountain Bike Racing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        statusLabel = new JLabel("Mode: VS Computer | P1: Arrow Keys | P2/AI: WASD");
        bottom.add(statusLabel);
        frame.add(bottom, BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        startGame();
    }

    // ============ INIT ============
    private void initTrack() {
        // Create a hilly track using sine waves + noise
        trackSegments = 400;
        trackX = new double[trackSegments + 1];
        trackY = new double[trackSegments + 1];

        double baseY = HEIGHT * 0.7;
        double scale = 200.0;

        for (int i = 0; i <= trackSegments; i++) {
            double x = i * 40.0; // 40 px per segment
            trackX[i] = x;

            // Combine a few sine waves for hills
            double y = baseY
                    + 60 * Math.sin(i * 0.05)
                    + 30 * Math.sin(i * 0.12)
                    + 15 * Math.sin(i * 0.23);

            // Add some "extreme" bumps
            if (i > 50 && i < 80) {
                y -= 40 * Math.sin((i - 50) * Math.PI / 30.0);
            }
            if (i > 150 && i < 190) {
                y -= 50 * Math.sin((i - 150) * Math.PI / 40.0);
            }

            trackY[i] = y;
        }
    }

    private void initPlayers() {
        // Player 1: human
        player1 = new Player(100, 0, new Color(220, 60, 60), "P1");

        // Player 2: AI or second human
        player2 = new Player(100, 0, new Color(60, 120, 255), mode == GameMode.VS_COMPUTER ? "AI" : "P2");

        placePlayerOnTrack(player1, 5);
        placePlayerOnTrack(player2, 5);
    }

    private void placePlayerOnTrack(Player p, int segmentIndex) {
        double x = trackX[segmentIndex];
        double y = trackY[segmentIndex];
        p.x = x;
        p.y = y - 40;
        p.vx = 0;
        p.vy = 0;
        p.onGround = false;
    }

    private void startGame() {
        running = true;
        timer = new Timer(DELAY, this);
        timer.start();
    }

    // ============ GAME LOOP ============
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!running) return;

        updatePhysics();
        updateAI();
        updateCamera();
        repaint();
    }

    private void updatePhysics() {
        // Handle input
        handlePlayerInput(player1,
                keysPressed.contains(KeyEvent.VK_RIGHT),
                keysPressed.contains(KeyEvent.VK_LEFT),
                keysPressed.contains(KeyEvent.VK_UP));

        if (mode == GameMode.TWO_PLAYERS) {
            handlePlayerInput(player2,
                    keysPressed.contains(KeyEvent.VK_D),
                    keysPressed.contains(KeyEvent.VK_A),
                    keysPressed.contains(KeyEvent.VK_W));
        }

        // Update each player
        updatePlayer(player1);
        updatePlayer(player2);

        // Simple win condition: first to reach far right
        double finishX = trackX[trackSegments - 10];
        if (player1.x >= finishX || player2.x >= finishX) {
            String winner = (player1.x >= finishX && player2.x < finishX) ? "Player 1" :
                    (player2.x >= finishX && player1.x < finishX) ? (mode == GameMode.VS_COMPUTER ? "Computer" : "Player 2")
                            : "Draw";
            running = false;
            timer.stop();
            JOptionPane.showMessageDialog(this, "Race Finished!\nWinner: " + winner,
                    "Game Over", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handlePlayerInput(Player p, boolean right, boolean left, boolean jump) {
        if (right) p.vx += ACCELERATION;
        if (left) p.vx -= ACCELERATION;

        if (jump && p.onGround) {
            p.vy = JUMP_FORCE;
            p.onGround = false;
        }
    }

    private void updatePlayer(Player p) {
        // Apply gravity
        p.vy += GRAVITY;

        // Apply friction
        p.vx *= FRICTION;

        // Clamp speed
        if (p.vx > MAX_SPEED) p.vx = MAX_SPEED;
        if (p.vx < -MAX_SPEED) p.vx = -MAX_SPEED;

        // Move
        p.x += p.vx;
        p.y += p.vy;

        // Keep in track bounds horizontally
        if (p.x < trackX[0]) {
            p.x = trackX[0];
            p.vx = 0;
        }
        if (p.x > trackX[trackSegments]) {
            p.x = trackX[trackSegments];
            p.vx = 0;
        }

        // Ground collision with terrain
        int seg = findTrackSegment(p.x);
        if (seg >= 0 && seg < trackSegments) {
            double yGround = interpolateTrackY(seg, p.x);
            double bikeBottom = p.y + p.height;

            if (bikeBottom >= yGround) {
                p.y = yGround - p.height;
                p.vy = 0;
                p.onGround = true;

                // Slope influence on horizontal speed (simple)
                double slope = getSlope(seg);
                p.vx += slope * 0.05;
            } else {
                p.onGround = false;
            }
        }
    }

    private int findTrackSegment(double x) {
        // Simple linear search is fine for this size; could be optimized.
        for (int i = 0; i < trackSegments; i++) {
            if (x >= trackX[i] && x <= trackX[i + 1]) {
                return i;
            }
        }
        return x < trackX[0] ? 0 : trackSegments - 1;
    }

    private double interpolateTrackY(int seg, double x) {
        double x0 = trackX[seg];
        double x1 = trackX[seg + 1];
        double y0 = trackY[seg];
        double y1 = trackY[seg + 1];

        if (x1 == x0) return y0;
        double t = (x - x0) / (x1 - x0);
        return y0 + t * (y1 - y0);
    }

    private double getSlope(int seg) {
        double dx = trackX[seg + 1] - trackX[seg];
        double dy = trackY[seg + 1] - trackY[seg];
        if (dx == 0) return 0;
        return dy / dx; // positive = downhill to the right
    }

    // ============ AI ============
    private void updateAI() {
        if (mode != GameMode.VS_COMPUTER) return;

        // Very simple AI: try to go right, jump if needed
        Player ai = player2;
        boolean wantRight = true;
        boolean wantJump = false;

        // Look ahead a bit
        int lookSeg = findTrackSegment(ai.x + 120);
        if (lookSeg >= 0 && lookSeg < trackSegments) {
            double yAhead = interpolateTrackY(lookSeg, ai.x + 120);
            double yNow = interpolateTrackY(findTrackSegment(ai.x), ai.x);
            if (yAhead > yNow + 25) {
                // Big drop ahead, maybe slow a bit
                wantRight = ai.vx < 4;
            }
            if (yAhead < yNow - 20 && !ai.onGround) {
                // In air, do nothing special
            }
            if (yAhead > yNow + 10 && ai.onGround) {
                wantJump = true;
            }
        }

        // Apply AI "input"
        if (wantRight) ai.vx += ACCELERATION * 0.7;
        if (wantJump && ai.onGround) {
            ai.vy = JUMP_FORCE * 0.9;
            ai.onGround = false;
        }
    }

    // ============ CAMERA ============
    private void updateCamera() {
        // Center camera between players, but mostly follow leader
        double leaderX = Math.max(player1.x, player2.x);
        double targetCamX = leaderX - WIDTH * 0.4;

        // Smooth camera
        cameraX += (targetCamX - cameraX) * 0.1;

        // Clamp
        if (cameraX < trackX[0]) cameraX = trackX[0];
        if (cameraX > trackX[trackSegments] - WIDTH) cameraX = trackX[trackSegments] - WIDTH;
    }

    // ============ RENDER ============
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Clear background
        g2.setColor(getBackground());
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw distant mountains (parallax)
        drawBackgroundMountains(g2);

        // Apply camera transform
        AffineTransform old = g2.getTransform();
        g2.translate(-cameraX, 0);

        // Draw track
        drawTrack(g2);

        // Draw players
        drawPlayer(g2, player1);
        drawPlayer(g2, player2);

        // Draw finish line
        double finishX = trackX[trackSegments - 10];
        g2.setColor(new Color(255, 215, 0));
        g2.fillRect((int) finishX, (int) (trackY[trackSegments - 10] - 200), 10, 200);
        g2.setColor(Color.BLACK);
        g2.drawString("FINISH", (int) finishX - 20, (int) (trackY[trackSegments - 10] - 210));

        g2.setTransform(old);

        // HUD
        drawHUD(g2);
    }

    private void drawBackgroundMountains(Graphics2D g2) {
        g2.setColor(new Color(90, 120, 140));
        int steps = 20;
        for (int i = 0; i <= steps; i++) {
            double x = (i * (WIDTH / steps)) - (cameraX * 0.3);
            double h = 120 + 40 * Math.sin(i * 0.7);
            int x1 = (int) x;
            int x2 = (int) (x + WIDTH / steps + 2);
            int yBase = HEIGHT - 40;
            int yTop = (int) (yBase - h);

            int[] xs = {x1, x2, x2, x1};
            int[] ys = {yBase, yBase, yTop, yTop};
            g2.fillPolygon(xs, ys, 4);
        }
    }

    private void drawTrack(Graphics2D g2) {
        // Ground polygon
        g2.setColor(new Color(70, 120, 60));
        int[] xs = new int[trackSegments + 3];
        int[] ys = new int[trackSegments + 3];

        xs[0] = (int) trackX[0];
        ys[0] = HEIGHT;
        for (int i = 0; i <= trackSegments; i++) {
            xs[i + 1] = (int) trackX[i];
            ys[i + 1] = (int) trackY[i];
        }
        xs[trackSegments + 2] = (int) trackX[trackSegments];
        ys[trackSegments + 2] = HEIGHT;

        g2.fillPolygon(xs, ys, trackSegments + 3);

        // Top edge line
        g2.setColor(new Color(40, 80, 30));
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(
                (int) trackX[0], (int) trackY[0],
                (int) trackX[trackSegments], (int) trackY[trackSegments]
        );
    }

    private void drawPlayer(Graphics2D g2, Player p) {
        // Simple bike + rider representation
        double x = p.x;
        double y = p.y;

        // Shadow
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillOval((int) (x - 15), (int) (y + p.height - 6), 30, 12);

        // Bike frame
        g2.setColor(p.color);
        int bx = (int) x;
        int by = (int) (y + p.height - 10);

        // Wheels
        g2.setColor(Color.BLACK);
        g2.fillOval(bx - 18, by - 10, 20, 20);
        g2.fillOval(bx + 8, by - 10, 20, 20);

        // Frame lines
        g2.setColor(p.color);
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(bx - 8, by - 10, bx + 18, by - 10);
        g2.drawLine(bx - 8, by - 10, bx - 2, by - 28);
        g2.drawLine(bx - 2, by - 28, bx + 12, by - 28);
        g2.drawLine(bx + 12, by - 28, bx + 18, by - 10);

        // Rider (simple)
        g2.setColor(new Color(220, 220, 220));
        g2.fillOval(bx - 4, by - 40, 14, 14); // head
        g2.setStroke(new BasicStroke(4));
        g2.drawLine(bx + 3, by - 30, bx + 10, by - 18); // arm
        g2.drawLine(bx + 3, by - 30, bx - 5, by - 18); // other arm

        // Name
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString(p.name, (int) x - 10, (int) y - 10);
    }

    private void drawHUD(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(10, 10, 260, 70);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString("Mode: " + (mode == GameMode.VS_COMPUTER ? "VS Computer" : "Two Players"), 20, 30);
        g2.drawString("P1: Arrow Keys (Right/Left/Up)", 20, 50);
        g2.drawString(mode == GameMode.VS_COMPUTER ?
                "AI: Auto" :
                "P2: A/D (Left/Right), W (Jump)", 20, 70);
    }

    // ============ KEY INPUT ============
    @Override
    public void keyPressed(KeyEvent e) {
        keysPressed.add(e.getKeyCode());

        // Mode switch with M (just for fun)
        if (e.getKeyCode() == KeyEvent.VK_M) {
            mode = (mode == GameMode.VS_COMPUTER) ? GameMode.TWO_PLAYERS : GameMode.VS_COMPUTER;
            statusLabel.setText("Mode: " + (mode == GameMode.VS_COMPUTER ? "VS Computer" : "Two Players")
                    + " | P1: Arrow Keys | " + (mode == GameMode.VS_COMPUTER ? "AI: Auto" : "P2: WASD"));
            initPlayers();
            running = true;
            if (timer == null || !timer.isRunning()) {
                timer = new Timer(DELAY, this);
                timer.start();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keysPressed.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // ============ PLAYER CLASS ============
    private static class Player {
        double x, y;
        double vx, vy;
        boolean onGround;
        final int width = 40;
        final int height = 40;
        final Color color;
        final String name;

        Player(double x, double y, Color color, String name) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.name = name;
        }
    }
}