import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CrossCountryRacing extends JFrame {

    public CrossCountryRacing() {
        setTitle("Cross-Country Racing Adventure");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        pack();

        setLocationRelativeTo(null);
        setVisible(true);

        gamePanel.startGame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CrossCountryRacing::new);
    }
}

class GamePanel extends JPanel implements Runnable, KeyListener {

    public static final int WIDTH = 900;
    public static final int HEIGHT = 600;

    // Game loop
    private Thread gameThread;
    private boolean running = false;
    private final int FPS = 60;

    // Game States
    private enum State { MENU, PLAYING, GAME_OVER }
    private State currentState = State.MENU;
    private boolean isMultiplayer = false;

    // Input States
    private final boolean[] keys = new boolean[256];

    // Road & Segment Engine (Pseudo-3D Projection)
    public static class Segment {
        public int index;
        public double worldZ;
        public double curve;
        public double hill;
        public Color grassColor;
        public Color rumbleColor;
        public Color roadColor;
        public List<Obstacle> obstacles = new ArrayList<>();
    }

    public static class Obstacle {
        public double xRatio; // -1.0 (left side) to +1.0 (right side)
        public int type;      // 0: Tree/Cactus, 1: Slow Hazard, 2: Traffic Car
    }

    private final List<Segment> roadSegments = new ArrayList<>();
    private final int segmentLength = 200;
    private final int totalSegments = 1600;
    private final int trackLength;

    // Player 1 Engine
    private double p1Position = 0;
    private double p1X = 0; // -1.0 to 1.0 from road center
    private double p1Speed = 0;
    private final double maxSpeed = 12000;
    private final double accel = 180;
    private final double braking = 300;
    private final double decel = 80;
    private final double offRoadDecel = 300;
    private double p1DistanceTraveled = 0;
    private int p1Laps = 0;

    // Player 2 / AI Engine
    private double p2Position = 0;
    private double p2X = 0.5;
    private double p2Speed = 0;
    private double p2DistanceTraveled = 0;
    private int p2Laps = 0;

    // Scenery & Sky Dynamics
    private double skyOffset = 0;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        buildTrack();
        trackLength = totalSegments * segmentLength;
    }

    private void buildTrack() {
        roadSegments.clear();
        for (int i = 0; i < totalSegments; i++) {
            Segment seg = new Segment();
            seg.index = i;
            seg.worldZ = i * segmentLength;

            // Curves & Elevation Changes across terrain stages
            if (i > 100 && i < 300) seg.curve = 2.5;
            else if (i > 400 && i < 600) seg.curve = -3.0;
            else if (i > 700 && i < 900) seg.hill = Math.sin(i / 20.0) * 1500;
            else if (i > 1000 && i < 1300) { seg.curve = 3.5; seg.hill = Math.cos(i / 15.0) * 2000; }

            // Dynamic Biome Colors (Desert -> Forest -> Alpine)
            boolean alt = (i / 3) % 2 == 0;
            if (i < 500) { // Desert Stage
                seg.grassColor = alt ? new Color(225, 180, 100) : new Color(210, 165, 85);
            } else if (i < 1100) { // Forest Stage
                seg.grassColor = alt ? new Color(34, 139, 34) : new Color(0, 100, 0);
            } else { // Mountain Pass
                seg.grassColor = alt ? new Color(200, 210, 220) : new Color(170, 180, 190);
            }

            seg.rumbleColor = alt ? Color.RED : Color.WHITE;
            seg.roadColor = alt ? new Color(100, 100, 100) : new Color(108, 108, 108);

            // Populate Road Hazards
            if (i % 25 == 0 && i > 50) {
                Obstacle obs = new Obstacle();
                obs.xRatio = (new Random().nextDouble() * 1.6) - 0.8;
                obs.type = new Random().nextInt(3);
                seg.obstacles.add(obs);
            }

            roadSegments.add(seg);
        }
    }

    public void startGame() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        long targetTime = 1000 / FPS;

        while (running) {
            long startTime = System.currentTimeMillis();

            updateGame();
            repaint();

            long elapsedTime = System.currentTimeMillis() - startTime;
            long waitTime = targetTime - elapsedTime;

            if (waitTime > 0) {
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateGame() {
        if (currentState != State.PLAYING) return;

        double dt = 1.0 / FPS;

        // Player 1 Physics Controls (WASD / Arrows)
        if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) {
            p1Speed += accel;
        } else if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]) {
            p1Speed -= braking;
        } else {
            p1Speed -= decel;
        }

        if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]) p1X -= 0.035 * (p1Speed / maxSpeed);
        if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]) p1X += 0.035 * (p1Speed / maxSpeed);

        // Player 2 / AI Physics
        if (isMultiplayer) {
            if (keys[KeyEvent.VK_I]) p2Speed += accel;
            else if (keys[KeyEvent.VK_K]) p2Speed -= braking;
            else p2Speed -= decel;

            if (keys[KeyEvent.VK_J]) p2X -= 0.035 * (p2Speed / maxSpeed);
            else if (keys[KeyEvent.VK_L]) p2X += 0.035 * (p2Speed / maxSpeed);
        } else {
            // Adaptive AI logic
            double targetSpeed = maxSpeed * 0.88;
            if (p2Speed < targetSpeed) p2Speed += accel * 0.8;
            else p2Speed -= decel;

            // Simple Obstacle Avoidance for AI
            int aiSegIndex = (int) (p2Position / segmentLength) % totalSegments;
            Segment currentAiSeg = roadSegments.get(aiSegIndex);
            for (Obstacle obs : currentAiSeg.obstacles) {
                if (Math.abs(p2X - obs.xRatio) < 0.3) {
                    if (p2X > 0) p2X -= 0.02;
                    else p2X += 0.02;
                }
            }
        }

        // Apply Speed Limits & Off-road Friction
        p1Speed = Math.min(Math.max(p1Speed, 0), maxSpeed);
        p2Speed = Math.min(Math.max(p2Speed, 0), maxSpeed);

        if (Math.abs(p1X) > 1.0) p1Speed = Math.max(0, p1Speed - offRoadDecel);
        if (Math.abs(p2X) > 1.0) p2Speed = Math.max(0, p2Speed - offRoadDecel);

        // Position Updates
        p1Position += p1Speed * dt * 60;
        p2Position += p2Speed * dt * 60;

        p1DistanceTraveled += p1Speed * dt;
        p2DistanceTraveled += p2Speed * dt;

        p1Laps = (int) (p1Position / trackLength);
        p2Laps = (int) (p2Position / trackLength);

        // Camera Dynamics & Horizon Shift
        int currentP1Seg = (int) (p1Position / segmentLength) % totalSegments;
        skyOffset -= roadSegments.get(currentP1Seg).curve * (p1Speed / maxSpeed) * 0.3;

        // Check Winner (First to complete 3 Laps)
        if (p1Laps >= 3 || p2Laps >= 3) {
            currentState = State.GAME_OVER;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (currentState == State.MENU) {
            drawMenu(g2);
        } else if (currentState == State.PLAYING) {
            drawRace(g2);
        } else if (currentState == State.GAME_OVER) {
            drawGameOver(g2);
        }
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(25, 25, 35));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(255, 140, 0));
        g.setFont(new Font("SansSerif", Font.BOLD, 42));
        g.drawString("CROSS-COUNTRY RACING", 180, 150);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.drawString("Press 1: Single Player (VS Computer AI)", 250, 280);
        g.drawString("Press 2: Two Player (Local Splitscreen/Key Controls)", 250, 340);

        g.setFont(new Font("SansSerif", Font.ITALIC, 16));
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("Player 1 Controls: WASD or Arrow Keys", 280, 440);
        g.drawString("Player 2 Controls: I, J, K, L", 280, 470);
    }

    private void drawRace(Graphics2D g) {
        // Dynamic Sky & Mountains
        int skyX = ((int) skyOffset) % WIDTH;
        g.setColor(new Color(110, 170, 230));
        g.fillRect(0, 0, WIDTH, HEIGHT / 2);
        g.setColor(new Color(80, 100, 120));
        g.fillOval(skyX - 200, HEIGHT / 2 - 120, 600, 200);
        g.fillOval(skyX + 400, HEIGHT / 2 - 140, 700, 220);

        // Render Pseudo-3D Projection
        int baseSegmentIndex = (int) (p1Position / segmentLength) % totalSegments;
        double startPos = p1Position % segmentLength;

        double x = 0, dx = 0;
        int maxy = HEIGHT;

        for (int n = 0; n < 180; n++) {
            Segment seg = roadSegments.get((baseSegmentIndex + n) % totalSegments);

            double camH = 1500;
            double camD = 0.8;

            double z1 = (n * segmentLength) - startPos + 1;
            double z2 = ((n + 1) * segmentLength) - startPos + 1;

            int y1 = (int) ((HEIGHT / 2.0) + (camH / z1) * (HEIGHT / 2.0));
            int y2 = (int) ((HEIGHT / 2.0) + (camH / z2) * (HEIGHT / 2.0));

            int w1 = (int) ((WIDTH * 1.8) / z1 * camD * 100);
            int w2 = (int) ((WIDTH * 1.8) / z2 * camD * 100);

            int x1 = (int) (WIDTH / 2.0 + x - (p1X * w1));
            int x2 = (int) (WIDTH / 2.0 + x + dx - (p1X * w2));

            x += dx;
            dx += seg.curve;

            if (y1 >= maxy) continue;
            maxy = y1;

            // Draw Grass, Rumble Strips, Road Surface
            g.setColor(seg.grassColor);
            g.fillRect(0, y2, WIDTH, y1 - y2);

            g.setColor(seg.rumbleColor);
            g.fillRect(x1 - w1 - w1 / 5, y2, (int) (w1 * 2.4), y1 - y2);

            g.setColor(seg.roadColor);
            g.fillRect(x1 - w1, y2, w1 * 2, y1 - y2);

            // Center Speed Line
            if ((seg.index / 3) % 2 == 0) {
                g.setColor(Color.YELLOW);
                g.fillRect(x1 - w1 / 20, y2, Math.max(2, w1 / 10), y1 - y2);
            }

            // Render Road Side Props / Hazards
            for (Obstacle obs : seg.obstacles) {
                int spriteX = (int) (x1 + obs.xRatio * w1);
                int spriteSize = Math.max(10, (int) (12000 / z1));
                drawObstacle(g, spriteX, y1 - spriteSize, spriteSize, obs.type);
            }
        }

        // Draw Player Vehicles & Animations
        drawCar(g, WIDTH / 2 + (int) (p1X * 120), HEIGHT - 110, Color.RED, "P1");

        // Render Player 2 / AI Car Relative Projection
        double relP2Pos = p2Position - p1Position;
        if (relP2Pos > -1000 && relP2Pos < 12000) {
            double p2Z = relP2Pos + 10;
            int p2ScreenY = (int) ((HEIGHT / 2.0) + (1500 / p2Z) * (HEIGHT / 2.0));
            int p2ScreenX = (int) (WIDTH / 2.0 + (p2X - p1X) * 400);
            int p2Size = Math.max(15, Math.min(100, (int) (10000 / p2Z)));
            drawCar(g, p2ScreenX, p2ScreenY - p2Size, Color.BLUE, isMultiplayer ? "P2" : "AI");
        }

        // HUD Display
        drawHUD(g);
    }

    private void drawObstacle(Graphics2D g, int x, int y, int size, int type) {
        if (type == 0) { // Tree / Cactus
            g.setColor(new Color(34, 110, 34));
            g.fillOval(x - size / 2, y, size, size);
            g.setColor(new Color(100, 60, 20));
            g.fillRect(x - size / 6, y + size, size / 3, size / 2);
        } else { // Rock / Barrier
            g.setColor(Color.GRAY);
            g.fillRect(x - size / 2, y + size / 2, size, size / 2);
        }
    }

    private void drawCar(Graphics2D g, int x, int y, Color color, String label) {
        int carW = 70;
        int carH = 35;

        // Shadow
        g.setColor(new Color(0, 0, 0, 100));
        g.fillOval(x - carW / 2, y + carH - 5, carW, 15);

        // Body
        g.setColor(color);
        g.fillRoundRect(x - carW / 2, y, carW, carH, 12, 12);

        // Windshield
        g.setColor(new Color(180, 220, 255));
        g.fillRect(x - carW / 4, y + 5, carW / 2, carH / 3);

        // Wheels
        g.setColor(Color.BLACK);
        g.fillRect(x - carW / 2 - 4, y + 5, 6, 10);
        g.fillRect(x + carW / 2 - 2, y + 5, 6, 10);
        g.fillRect(x - carW / 2 - 4, y + carH - 12, 6, 10);
        g.fillRect(x + carW / 2 - 2, y + carH - 12, 6, 10);

        // Label
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString(label, x - 8, y - 5);
    }

    private void drawHUD(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(10, 10, 260, 90);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Player 1 Speed: " + (int) (p1Speed / 100) + " MPH", 20, 32);
        g.drawString("Player 1 Lap: " + Math.min(3, p1Laps + 1) + " / 3", 20, 52);

        g.drawString(isMultiplayer ? "Player 2 Speed: " + (int) (p2Speed / 100) + " MPH" : "AI Speed: " + (int) (p2Speed / 100) + " MPH", 20, 72);
        g.drawString((isMultiplayer ? "Player 2 Lap: " : "AI Lap: ") + Math.min(3, p2Laps + 1) + " / 3", 20, 90);
    }

    private void drawGameOver(Graphics2D g) {
        drawRace(g);

        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.GOLD);
        g.setFont(new Font("SansSerif", Font.BOLD, 48));

        String winner = (p1Laps >= 3) ? "PLAYER 1 WINS!" : (isMultiplayer ? "PLAYER 2 WINS!" : "COMPUTER AI WINS!");
        g.drawString(winner, 240, 280);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString("Press ENTER to Return to Main Menu", 270, 360);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code < keys.length) keys[code] = true;

        if (currentState == State.MENU) {
            if (code == KeyEvent.VK_1) {
                isMultiplayer = false;
                resetGame();
                currentState = State.PLAYING;
            } else if (code == KeyEvent.VK_2) {
                isMultiplayer = true;
                resetGame();
                currentState = State.PLAYING;
            }
        } else if (currentState == State.GAME_OVER) {
            if (code == KeyEvent.VK_ENTER) {
                currentState = State.MENU;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code < keys.length) keys[code] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void resetGame() {
        p1Position = 0;
        p1X = 0;
        p1Speed = 0;
        p1Laps = 0;

        p2Position = 0;
        p2X = 0.4;
        p2Speed = 0;
        p2Laps = 0;
    }
}