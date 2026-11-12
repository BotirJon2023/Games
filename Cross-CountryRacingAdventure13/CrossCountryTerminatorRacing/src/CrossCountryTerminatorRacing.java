import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CrossCountryTerminatorRacing extends JFrame {

    public CrossCountryTerminatorRacing() {
        setTitle("Terminator: Judgment Day Cybernetic Pursuit");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        TerminatorGamePanel gamePanel = new TerminatorGamePanel();
        add(gamePanel);
        pack();

        setLocationRelativeTo(null);
        setVisible(true);

        gamePanel.startGame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CrossCountryTerminatorRacing::new);
    }
}

class TerminatorGamePanel extends JPanel implements Runnable, KeyListener {

    public static final int WIDTH = 960;
    public static final int HEIGHT = 600;

    // Loop Control
    private Thread gameThread;
    private boolean running = false;
    private final int FPS = 60;

    // Game States
    private enum State { MENU, PLAYING, GAME_OVER }
    private State currentState = State.MENU;
    private boolean isMultiplayer = false;

    // Input Tracking
    private final boolean[] keys = new boolean[256];

    // Road Projection Segment
    public static class RoadSegment {
        public int index;
        public double worldZ;
        public double curve;
        public double hill;
        public Color wastelandGlow;
        public Color rumbleColor;
        public Color roadColor;
        public List<Obstacle> obstacles = new ArrayList<>();
    }

    public static class Obstacle {
        public double xRatio; // -1.0 to +1.0
        public int type;      // 0: Cyberdyne Mine, 1: T-800 Endoskeleton, 2: Debris Barricade
        public boolean destroyed = false;
    }

    // Particle FX Engine
    public static class Particle {
        public double x, y, vx, vy;
        public int life, maxLife;
        public Color color;
        public int size;
    }

    private final List<RoadSegment> roadSegments = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final int segmentLength = 200;
    private final int totalSegments = 1800;
    private final int trackLength;

    // Player 1 Engine (Human Resistance Vehicle)
    private double p1Position = 0;
    private double p1X = -0.3;
    private double p1Speed = 0;
    private double p1PlasmaCooldown = 0;
    private int p1Laps = 0;

    // Player 2 / AI Engine (Skynet T-800 / HK Drone)
    private double p2Position = 0;
    private double p2X = 0.3;
    private double p2Speed = 0;
    private double p2PlasmaCooldown = 0;
    private int p2Laps = 0;

    // Physics Constants
    private final double maxSpeed = 14200;
    private final double accel = 220;
    private final double braking = 390;
    private final double decel = 95;
    private final double offRoadDecel = 350;

    // Horizon & Skynet Vision Controls
    private double skyOffset = 0;
    private double skynetScanPulse = 0;

    public TerminatorGamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        buildWastelandTrack();
        trackLength = totalSegments * segmentLength;
    }

    private void buildWastelandTrack() {
        roadSegments.clear();
        Random rand = new Random(1997); // Deterministic apocalyptic layout

        for (int i = 0; i < totalSegments; i++) {
            RoadSegment seg = new RoadSegment();
            seg.index = i;
            seg.worldZ = i * segmentLength;

            // Highway Curves & Elevation Geometry
            if (i > 100 && i < 300) seg.curve = 3.6;
            else if (i > 400 && i < 600) seg.curve = -3.9;
            else if (i > 750 && i < 950) seg.hill = Math.sin(i / 13.0) * 2000;
            else if (i > 1150 && i < 1550) { seg.curve = 4.4; seg.hill = Math.cos(i / 9.0) * 2500; }

            // Dynamic Biomes (Ruined Los Angeles -> Cyberdyne Central Facility -> Burning Ash Waste)
            boolean alt = (i / 3) % 2 == 0;
            if (i < 600) { // Ruined LA (Ashen Dark Grays & Crimson Horizon)
                seg.wastelandGlow = alt ? new Color(20, 12, 14) : new Color(12, 8, 10);
            } else if (i < 1200) { // Cyberdyne Core (Neon Purple & Cyan Electric Sparks)
                seg.wastelandGlow = alt ? new Color(16, 12, 28) : new Color(10, 8, 18);
            } else { // Nuclear Fire Waste (Burning Plasma Orange)
                seg.wastelandGlow = alt ? new Color(32, 14, 8) : new Color(20, 8, 5);
            }

            seg.rumbleColor = alt ? new Color(255, 30, 0) : new Color(30, 30, 30);
            seg.roadColor = alt ? new Color(26, 26, 30) : new Color(18, 18, 22);

            // Spawn Hazards
            if (i % 21 == 0 && i > 40) {
                Obstacle obs = new Obstacle();
                obs.xRatio = (rand.nextDouble() * 1.6) - 0.8;
                obs.type = rand.nextInt(3);
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
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void updateGame() {
        if (currentState != State.PLAYING) return;

        double dt = 1.0 / FPS;
        skynetScanPulse += 0.09;

        // --- Player 1 Controls (WASD / Arrows + SPACE for Plasma Cannon) ---
        if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) p1Speed += accel;
        else if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]) p1Speed -= braking;
        else p1Speed -= decel;

        if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]) p1X -= 0.042 * (p1Speed / maxSpeed);
        if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]) p1X += 0.042 * (p1Speed / maxSpeed);

        p1PlasmaCooldown = Math.max(0, p1PlasmaCooldown - dt);
        if (keys[KeyEvent.VK_SPACE] && p1PlasmaCooldown <= 0) {
            firePlasmaCannon(p1Position, p1X);
            p1PlasmaCooldown = 2.2; // 2.2 sec cooldown
        }

        // --- Player 2 / AI Controls ---
        if (isMultiplayer) {
            if (keys[KeyEvent.VK_I]) p2Speed += accel;
            else if (keys[KeyEvent.VK_K]) p2Speed -= braking;
            else p2Speed -= decel;

            if (keys[KeyEvent.VK_J]) p2X -= 0.042 * (p2Speed / maxSpeed);
            else if (keys[KeyEvent.VK_L]) p2X += 0.042 * (p2Speed / maxSpeed);

            p2PlasmaCooldown = Math.max(0, p2PlasmaCooldown - dt);
            if (keys[KeyEvent.VK_U] && p2PlasmaCooldown <= 0) {
                firePlasmaCannon(p2Position, p2X);
                p2PlasmaCooldown = 2.2;
            }
        } else {
            // Skynet AI Infiltrator Navigation Logic
            double aiTargetSpeed = maxSpeed * 0.93;
            if (p2Speed < aiTargetSpeed) p2Speed += accel * 0.9;
            else p2Speed -= decel;

            // AI Hazard Evasion
            int aiSegIndex = (int) (p2Position / segmentLength) % totalSegments;
            RoadSegment currentAiSeg = roadSegments.get(aiSegIndex);
            for (Obstacle obs : currentAiSeg.obstacles) {
                if (!obs.destroyed && Math.abs(p2X - obs.xRatio) < 0.35) {
                    p2X += (p2X > 0) ? -0.04 : 0.04;
                }
            }
        }

        // Friction & Off-road Constraints
        p1Speed = Math.min(Math.max(p1Speed, 0), maxSpeed);
        p2Speed = Math.min(Math.max(p2Speed, 0), maxSpeed);

        if (Math.abs(p1X) > 1.0) p1Speed = Math.max(0, p1Speed - offRoadDecel);
        if (Math.abs(p2X) > 1.0) p2Speed = Math.max(0, p2Speed - offRoadDecel);

        // Position Logic
        p1Position += p1Speed * dt * 60;
        p2Position += p2Speed * dt * 60;

        p1Laps = (int) (p1Position / trackLength);
        p2Laps = (int) (p2Position / trackLength);

        // Particle Exhaust Trails
        spawnExhaustParticles(WIDTH / 2 + (int) (p1X * 160), HEIGHT - 90, p1Speed > 6000);

        // Particle Update Loop
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.life--;
            if (p.life <= 0) particles.remove(i);
        }

        // Horizon Panning
        int currentP1Seg = (int) (p1Position / segmentLength) % totalSegments;
        skyOffset -= roadSegments.get(currentP1Seg).curve * (p1Speed / maxSpeed) * 0.4;

        // Check Victory
        if (p1Laps >= 3 || p2Laps >= 3) {
            currentState = State.GAME_OVER;
        }
    }

    private void firePlasmaCannon(double pPosition, double pX) {
        int segIndex = (int) (pPosition / segmentLength) % totalSegments;
        for (int i = 0; i < 10; i++) {
            RoadSegment seg = roadSegments.get((segIndex + i) % totalSegments);
            for (Obstacle obs : seg.obstacles) {
                if (Math.abs(pX - obs.xRatio) < 0.65) {
                    obs.destroyed = true;
                }
            }
        }

        // Plasma Burst Visuals
        Random rand = new Random();
        for (int k = 0; k < 15; k++) {
            Particle p = new Particle();
            p.x = WIDTH / 2 + (int) (pX * 160);
            p.y = HEIGHT - 110;
            p.vx = (rand.nextDouble() * 12 - 6);
            p.vy = -(rand.nextDouble() * 9 + 5);
            p.life = 14 + rand.nextInt(8);
            p.maxLife = p.life;
            p.color = new Color(255, 0, 80);
            p.size = 8 + rand.nextInt(6);
            particles.add(p);
        }
    }

    private void spawnExhaustParticles(int x, int y, boolean highSpeed) {
        Random rand = new Random();
        int count = highSpeed ? 4 : 2;
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x = x + (rand.nextDouble() * 14 - 7);
            p.y = y + 10;
            p.vx = (rand.nextDouble() * 2 - 1);
            p.vy = rand.nextDouble() * 4 + 2;
            p.life = 12 + rand.nextInt(8);
            p.maxLife = p.life;
            p.color = highSpeed ? new Color(255, 0, 60) : new Color(255, 120, 0);
            p.size = 5 + rand.nextInt(5);
            particles.add(p);
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
        g.setColor(new Color(10, 8, 12));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        drawRuinedSkyline(g, 0);

        // Skynet Header
        g.setColor(new Color(255, 30, 30));
        g.setFont(new Font("Monospaced", Font.BOLD, 36));
        g.drawString("TERMINATOR: JUDGMENT DAY PURSUIT", 110, 260);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.drawString("PRESS 1: RESISTANCE (VS SKYNET T-800 AI)", 210, 350);
        g.drawString("PRESS 2: TWO PLAYER (RESISTANCE VS T-800 HK)", 210, 390);

        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g.setColor(Color.RED);
        g.drawString("P1 COMMANDS: WASD/ARROWS (NAVIGATE) | SPACE (PLASMA BLAST)", 180, 480);
        g.drawString("P2 COMMANDS: IJKL (NAVIGATE)       | U (PLASMA BLAST)", 180, 510);
    }

    private void drawRace(Graphics2D g) {
        // Red Skynet Atmosphere Sky
        g.setColor(new Color(15, 6, 8));
        g.fillRect(0, 0, WIDTH, HEIGHT / 2);

        drawRuinedSkyline(g, (int) skyOffset);

        // Pseudo-3D Perspective Renderer
        int baseSegmentIndex = (int) (p1Position / segmentLength) % totalSegments;
        double startPos = p1Position % segmentLength;

        double x = 0, dx = 0;
        int maxy = HEIGHT;

        for (int n = 0; n < 180; n++) {
            RoadSegment seg = roadSegments.get((baseSegmentIndex + n) % totalSegments);

            double camH = 1400;
            double camD = 0.82;

            double z1 = (n * segmentLength) - startPos + 1;
            double z2 = ((n + 1) * segmentLength) - startPos + 1;

            int y1 = (int) ((HEIGHT / 2.0) + (camH / z1) * (HEIGHT / 2.0));
            int y2 = (int) ((HEIGHT / 2.0) + (camH / z2) * (HEIGHT / 2.0));

            int w1 = (int) ((WIDTH * 1.9) / z1 * camD * 100);
            int w2 = (int) ((WIDTH * 1.9) / z2 * camD * 100);

            int x1 = (int) (WIDTH / 2.0 + x - (p1X * w1));
            int x2 = (int) (WIDTH / 2.0 + x + dx - (p1X * w2));

            x += dx;
            dx += seg.curve;

            if (y1 >= maxy) continue;
            maxy = y1;

            // Render Wasteland Terrain
            g.setColor(seg.wastelandGlow);
            g.fillRect(0, y2, WIDTH, y1 - y2);

            // Rumble Strips & Damaged Road Surface
            g.setColor(seg.rumbleColor);
            g.fillRect(x1 - w1 - w1 / 6, y2, (int) (w1 * 2.33), y1 - y2);

            g.setColor(seg.roadColor);
            g.fillRect(x1 - w1, y2, w1 * 2, y1 - y2);

            // Red Skynet Dividers
            if ((seg.index / 3) % 2 == 0) {
                g.setColor(new Color(255, 0, 40, 210));
                g.fillRect(x1 - w1 / 25, y2, Math.max(2, w1 / 12), y1 - y2);
            }

            // Road Hazards
            for (Obstacle obs : seg.obstacles) {
                if (obs.destroyed) continue;
                int spriteX = (int) (x1 + obs.xRatio * w1);
                int spriteSize = Math.max(12, (int) (13000 / z1));
                drawObstacle(g, spriteX, y1 - spriteSize, spriteSize, obs.type);
            }
        }

        // Particles
        for (Particle p : particles) {
            float alpha = (float) p.life / p.maxLife;
            g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int) (alpha * 255)));
            g.fillOval((int) p.x - p.size / 2, (int) p.y - p.size / 2, p.size, p.size);
        }

        // Render P1 (Resistance Armored Vehicle)
        drawResistanceVehicle(g, WIDTH / 2 + (int) (p1X * 160), HEIGHT - 100, true);

        // Render P2 / AI (Skynet HK Aerial Drone / T-800 Unit)
        double relP2Pos = p2Position - p1Position;
        if (relP2Pos > -1000 && relP2Pos < 12000) {
            double p2Z = relP2Pos + 10;
            int p2ScreenY = (int) ((HEIGHT / 2.0) + (1400 / p2Z) * (HEIGHT / 2.0));
            int p2ScreenX = (int) (WIDTH / 2.0 + (p2X - p1X) * 450);
            int p2Size = Math.max(20, Math.min(110, (int) (11000 / p2Z)));
            drawHkDrone(g, p2ScreenX, p2ScreenY - p2Size, p2Size);
        }

        // Red Skynet Vision HUD
        drawSkynetHUD(g);
    }

    private void drawRuinedSkyline(Graphics2D g, int offset) {
        g.setColor(new Color(24, 10, 12));
        int shift = (offset % 1200);
        for (int i = -1; i < 5; i++) {
            int bx = i * 300 + shift;
            g.fillRect(bx, HEIGHT / 2 - 180, 100, 180); // Ruined Skyscraper
            g.fillRect(bx + 120, HEIGHT / 2 - 240, 75, 240); // Cyberdyne Tower
            g.fillRect(bx + 210, HEIGHT / 2 - 150, 80, 150);
        }
    }

    private void drawResistanceVehicle(Graphics2D g, int x, int y, boolean isP1) {
        int w = 84;
        int h = 42;

        // Vehicle Shadow
        g.setColor(new Color(0, 0, 0, 160));
        g.fillOval(x - w / 2, y + h - 6, w, 18);

        // Reinforced Armor Plate Body
        g.setColor(new Color(60, 65, 75));
        g.fillRoundRect(x - w / 2, y, w, h, 10, 10);

        // Metal Plating Trim
        g.setColor(new Color(110, 115, 125));
        g.fillRect(x - w / 4, y + 4, w / 2, 8);

        // Red Sensor Lights
        g.setColor(Color.RED);
        g.fillRect(x - 18, y + 14, 8, 5);
        g.fillRect(x + 10, y + 14, 8, 5);

        // All-Terrain Wheels
        g.setColor(new Color(15, 15, 18));
        g.fillRect(x - w / 2 - 6, y + 4, 9, 13);
        g.fillRect(x + w / 2 - 3, y + 4, 9, 13);
        g.fillRect(x - w / 2 - 6, y + h - 15, 9, 13);
        g.fillRect(x + w / 2 - 3, y + h - 15, 9, 13);

        // Label Tag
        g.setColor(Color.RED);
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.drawString(isP1 ? "RESISTANCE (P1)" : "P2", x - 38, y - 8);
    }

    private void drawHkDrone(Graphics2D g, int x, int y, int size) {
        // Skynet HK Aerial Drone Structure
        g.setColor(new Color(120, 130, 140));

        // Chrome Wings & Chassis
        Polygon wings = new Polygon();
        wings.addPoint(x, y);
        wings.addPoint(x + size, y + size / 2);
        wings.addPoint(x + size / 2, y + size);
        wings.addPoint(x, y + size / 4 * 3);
        wings.addPoint(x - size / 2, y + size);
        wings.addPoint(x - size, y + size / 2);
        g.fill(wings);

        // Glowing Red Cybernetic Eye / Scanner
        g.setColor(Color.RED);
        g.fillOval(x - size / 5, y + size / 3, (size / 5) * 2, size / 4);

        g.setColor(Color.RED);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.drawString(isMultiplayer ? "P2 HK-DRONE" : "SKYNET AI", x - 32, y - 6);
    }

    private void drawObstacle(Graphics2D g, int x, int y, int size, int type) {
        if (type == 0) { // Cyberdyne Plasma Mine (Glowing Red)
            g.setColor(new Color(255, 0, 50));
            g.fillOval(x - size / 2, y, size, size);
            g.setColor(Color.BLACK);
            g.drawString("MINE", x - 15, y + size / 2);
        } else if (type == 1) { // T-800 Endoskeleton Infiltrator
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(x - size / 3, y, (size / 3) * 2, size);
            g.setColor(Color.RED);
            g.fillOval(x - 4, y + 4, 8, 8); // Red eye
        } else { // Debris Barricade
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x - size, y + size / 3, size * 2, size / 3);
            g.setColor(Color.RED);
            g.fillRect(x - size / 2, y, size, size / 4);
        }
    }

    private void drawSkynetHUD(Graphics2D g) {
        // Red Skynet Tactical Visor Tint Frame
        g.setColor(new Color(255, 0, 0, 180));
        g.drawRect(20, 20, 320, 120);

        // Skynet Target Lock Reticle
        int reticleX = WIDTH / 2 + (int) (p1X * 160);
        int reticleY = HEIGHT - 120;
        g.drawOval(reticleX - 25, reticleY - 25, 50, 50);
        g.drawLine(reticleX - 35, reticleY, reticleX + 35, reticleY);
        g.drawLine(reticleX, reticleY - 35, reticleX, reticleY + 35);

        // Telemetry Data Text
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString("SKYNET DEFENSE NET // TARGET LOCK", 32, 42);

        g.setColor(Color.WHITE);
        g.drawString("SPEED     : " + (int) (p1Speed / 100) + " MPH", 32, 65);
        g.drawString("SECTOR LAP: " + Math.min(3, p1Laps + 1) + " / 3", 32, 85);

        // Plasma Cannon Status
        g.drawString("PLASMA CHARGE: ", 32, 110);
        if (p1PlasmaCooldown <= 0) {
            g.setColor(Color.GREEN);
            g.drawString("READY [SPACE]", 160, 110);
        } else {
            g.setColor(Color.RED);
            g.drawString(String.format("%.1fs RECHARGING", p1PlasmaCooldown), 160, 110);
        }

        // Animated Red Scanline Overhead
        int scanY = (int) ((skynetScanPulse * 40) % HEIGHT);
        g.setColor(new Color(255, 0, 0, 35));
        g.fillRect(0, scanY, WIDTH, 5);
    }

    private void drawGameOver(Graphics2D g) {
        drawRace(g);

        g.setColor(new Color(0, 0, 0, 230));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(255, 30, 30));
        g.setFont(new Font("Monospaced", Font.BOLD, 42));

        String winner = (p1Laps >= 3) ? "HUMAN RESISTANCE VICTORIOUS!" : (isMultiplayer ? "PLAYER 2 VICTORIOUS!" : "SKYNET AI WINS JUDGMENT DAY!");
        g.drawString(winner, 90, 280);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        g.drawString("PRESS ENTER TO REBOOT SKYNET SYSTEM", 280, 360);
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
        p1X = -0.3;
        p1Speed = 0;
        p1Laps = 0;
        p1PlasmaCooldown = 0;

        p2Position = 0;
        p2X = 0.3;
        p2Speed = 0;
        p2Laps = 0;
        p2PlasmaCooldown = 0;

        particles.clear();
        buildWastelandTrack();
    }
}