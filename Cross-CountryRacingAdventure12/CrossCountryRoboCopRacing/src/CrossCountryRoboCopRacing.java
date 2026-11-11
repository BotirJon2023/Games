import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CrossCountryRoboCopRacing extends JFrame {

    public CrossCountryRoboCopRacing() {
        setTitle("RoboCop: Delta City Cyber Cross-Country Pursuit");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        RoboCopGamePanel gamePanel = new RoboCopGamePanel();
        add(gamePanel);
        pack();

        setLocationRelativeTo(null);
        setVisible(true);

        gamePanel.startGame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CrossCountryRoboCopRacing::new);
    }
}

class RoboCopGamePanel extends JPanel implements Runnable, KeyListener {

    public static final int WIDTH = 960;
    public static final int HEIGHT = 600;

    // Loop Dynamics
    private Thread gameThread;
    private boolean running = false;
    private final int FPS = 60;

    // Game States
    private enum State { MENU, PLAYING, GAME_OVER }
    private State currentState = State.MENU;
    private boolean isMultiplayer = false;

    // Input States
    private final boolean[] keys = new boolean[256];

    // Road Projection Engine
    public static class RoadSegment {
        public int index;
        public double worldZ;
        public double curve;
        public double hill;
        public Color cityGlow;
        public Color rumbleColor;
        public Color roadColor;
        public List<Obstacle> obstacles = new ArrayList<>();
    }

    public static class Obstacle {
        public double xRatio; // -1.0 (left) to +1.0 (right)
        public int type;      // 0: Nuke Drug Barrels, 1: Rogue Drone, 2: Cyber Barricade
        public boolean destroyed = false;
    }

    // Visual Particles (Auto-9 Muzzle Flares & Thrusters)
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

    // Player 1 Engine (RoboCop / OCP Pursuit Cruiser)
    private double p1Position = 0;
    private double p1X = -0.3;
    private double p1Speed = 0;
    private double p1TargetingCooldown = 0;
    private int p1Laps = 0;

    // Player 2 / AI Engine (ED-209 / Enforcer Cruiser)
    private double p2Position = 0;
    private double p2X = 0.3;
    private double p2Speed = 0;
    private double p2TargetingCooldown = 0;
    private int p2Laps = 0;

    // Physics Dynamics
    private final double maxSpeed = 14000;
    private final double accel = 210;
    private final double braking = 380;
    private final double decel = 95;
    private final double offRoadDecel = 340;

    // Environment & Dynamic Vision
    private double skyOffset = 0;
    private double scanlinePulse = 0;

    public RoboCopGamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        buildDeltaCityTrack();
        trackLength = totalSegments * segmentLength;
    }

    private void buildDeltaCityTrack() {
        roadSegments.clear();
        Random rand = new Random(1987); // Deterministic city generation

        for (int i = 0; i < totalSegments; i++) {
            RoadSegment seg = new RoadSegment();
            seg.index = i;
            seg.worldZ = i * segmentLength;

            // Delta City Highways Profile
            if (i > 90 && i < 290) seg.curve = 3.5;
            else if (i > 380 && i < 580) seg.curve = -3.8;
            else if (i > 700 && i < 900) seg.hill = Math.sin(i / 14.0) * 1900;
            else if (i > 1100 && i < 1500) { seg.curve = 4.2; seg.hill = Math.cos(i / 10.0) * 2400; }

            // Cyber Biomes (Old Detroit -> OCP Tower Corridor -> Industrial Refinery)
            boolean alt = (i / 3) % 2 == 0;
            if (i < 600) { // Old Detroit Slums (Rust Grays & Warning Yellows)
                seg.cityGlow = alt ? new Color(22, 22, 28) : new Color(14, 14, 18);
            } else if (i < 1200) { // Delta City Plaza (Cyan & Electric Blue)
                seg.cityGlow = alt ? new Color(10, 24, 32) : new Color(6, 16, 22);
            } else { // OCP Steel Works (Toxic Orange Glow)
                seg.cityGlow = alt ? new Color(30, 16, 12) : new Color(20, 10, 8);
            }

            seg.rumbleColor = alt ? new Color(0, 220, 255) : new Color(35, 35, 45);
            seg.roadColor = alt ? new Color(28, 30, 36) : new Color(20, 22, 26);

            // Populate Road Hazards
            if (i % 20 == 0 && i > 40) {
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
        scanlinePulse += 0.08;

        // --- Player 1 Controls (WASD / Arrows + SPACE for Auto-9 Target Lock) ---
        if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) p1Speed += accel;
        else if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]) p1Speed -= braking;
        else p1Speed -= decel;

        if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]) p1X -= 0.040 * (p1Speed / maxSpeed);
        if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]) p1X += 0.040 * (p1Speed / maxSpeed);

        p1TargetingCooldown = Math.max(0, p1TargetingCooldown - dt);
        if (keys[KeyEvent.VK_SPACE] && p1TargetingCooldown <= 0) {
            fireAuto9TargetingSystem(p1Position, p1X);
            p1TargetingCooldown = 2.5; // 2.5s cooldown
        }

        // --- Player 2 / AI Controls ---
        if (isMultiplayer) {
            if (keys[KeyEvent.VK_I]) p2Speed += accel;
            else if (keys[KeyEvent.VK_K]) p2Speed -= braking;
            else p2Speed -= decel;

            if (keys[KeyEvent.VK_J]) p2X -= 0.040 * (p2Speed / maxSpeed);
            else if (keys[KeyEvent.VK_L]) p2X += 0.040 * (p2Speed / maxSpeed);

            p2TargetingCooldown = Math.max(0, p2TargetingCooldown - dt);
            if (keys[KeyEvent.VK_U] && p2TargetingCooldown <= 0) {
                fireAuto9TargetingSystem(p2Position, p2X);
                p2TargetingCooldown = 2.5;
            }
        } else {
            // Adaptive ED-209 Patrol AI
            double aiTargetSpeed = maxSpeed * 0.92;
            if (p2Speed < aiTargetSpeed) p2Speed += accel * 0.88;
            else p2Speed -= decel;

            // AI Hazard Avoidance
            int aiSegIndex = (int) (p2Position / segmentLength) % totalSegments;
            RoadSegment currentAiSeg = roadSegments.get(aiSegIndex);
            for (Obstacle obs : currentAiSeg.obstacles) {
                if (!obs.destroyed && Math.abs(p2X - obs.xRatio) < 0.35) {
                    p2X += (p2X > 0) ? -0.035 : 0.035;
                }
            }
        }

        // Speed Clipping & Off-Road Penalties
        p1Speed = Math.min(Math.max(p1Speed, 0), maxSpeed);
        p2Speed = Math.min(Math.max(p2Speed, 0), maxSpeed);

        if (Math.abs(p1X) > 1.0) p1Speed = Math.max(0, p1Speed - offRoadDecel);
        if (Math.abs(p2X) > 1.0) p2Speed = Math.max(0, p2Speed - offRoadDecel);

        // Position Updates
        p1Position += p1Speed * dt * 60;
        p2Position += p2Speed * dt * 60;

        p1Laps = (int) (p1Position / trackLength);
        p2Laps = (int) (p2Position / trackLength);

        // Exhaust / Engine Particles
        spawnParticleEffects(WIDTH / 2 + (int) (p1X * 160), HEIGHT - 90, p1Speed > 6000);

        // Update Particle Positions
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.life--;
            if (p.life <= 0) particles.remove(i);
        }

        // Horizon Camera Tilt
        int currentP1Seg = (int) (p1Position / segmentLength) % totalSegments;
        skyOffset -= roadSegments.get(currentP1Seg).curve * (p1Speed / maxSpeed) * 0.38;

        // Victory Condition Check
        if (p1Laps >= 3 || p2Laps >= 3) {
            currentState = State.GAME_OVER;
        }
    }

    private void fireAuto9TargetingSystem(double pPosition, double pX) {
        int segIndex = (int) (pPosition / segmentLength) % totalSegments;
        for (int i = 0; i < 10; i++) {
            RoadSegment seg = roadSegments.get((segIndex + i) % totalSegments);
            for (Obstacle obs : seg.obstacles) {
                if (Math.abs(pX - obs.xRatio) < 0.65) {
                    obs.destroyed = true;
                }
            }
        }

        // Fire Muzzle Flares FX
        Random rand = new Random();
        for (int k = 0; k < 12; k++) {
            Particle p = new Particle();
            p.x = WIDTH / 2 + (int) (pX * 160);
            p.y = HEIGHT - 110;
            p.vx = (rand.nextDouble() * 10 - 5);
            p.vy = -(rand.nextDouble() * 8 + 4);
            p.life = 12 + rand.nextInt(8);
            p.maxLife = p.life;
            p.color = new Color(255, 220, 50);
            p.size = 8 + rand.nextInt(6);
            particles.add(p);
        }
    }

    private void spawnParticleEffects(int x, int y, boolean highSpeed) {
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
            p.color = highSpeed ? new Color(0, 220, 255) : new Color(255, 100, 0);
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
        g.setColor(new Color(12, 14, 18));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        drawDeltaCitySkyline(g, 0);

        // OCP Holographic Header
        g.setColor(new Color(0, 220, 255));
        g.setFont(new Font("Monospaced", Font.BOLD, 38));
        g.drawString("ROBOCOP: DELTA CITY PURSUIT", 160, 260);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.drawString("PRESS 1: SINGLE PLAYER (VS ED-209 ENFORCER AI)", 210, 350);
        g.drawString("PRESS 2: TWO PLAYER (LOCAL OCP CRUISER VS ED-209)", 210, 390);

        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g.setColor(Color.GREEN);
        g.drawString("P1 COMMANDS: WASD/ARROWS (NAVIGATE) | SPACE (AUTO-9 TARGET LOCK)", 170, 480);
        g.drawString("P2 COMMANDS: IJKL (NAVIGATE)       | U (AUTO-9 TARGET LOCK)", 170, 510);
    }

    private void drawRace(Graphics2D g) {
        // Delta City Background Sky
        g.setColor(new Color(10, 12, 16));
        g.fillRect(0, 0, WIDTH, HEIGHT / 2);

        drawDeltaCitySkyline(g, (int) skyOffset);

        // Pseudo-3D Perspective Track Projection
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

            // Render Ground Terrain
            g.setColor(seg.cityGlow);
            g.fillRect(0, y2, WIDTH, y1 - y2);

            // Rumble Strips & Road Surface
            g.setColor(seg.rumbleColor);
            g.fillRect(x1 - w1 - w1 / 6, y2, (int) (w1 * 2.33), y1 - y2);

            g.setColor(seg.roadColor);
            g.fillRect(x1 - w1, y2, w1 * 2, y1 - y2);

            // Neon Blue Center Speed Lines
            if ((seg.index / 3) % 2 == 0) {
                g.setColor(new Color(0, 240, 255, 200));
                g.fillRect(x1 - w1 / 25, y2, Math.max(2, w1 / 12), y1 - y2);
            }

            // Render Hazards
            for (Obstacle obs : seg.obstacles) {
                if (obs.destroyed) continue;
                int spriteX = (int) (x1 + obs.xRatio * w1);
                int spriteSize = Math.max(12, (int) (13000 / z1));
                drawObstacle(g, spriteX, y1 - spriteSize, spriteSize, obs.type);
            }
        }

        // Render Particle Engine
        for (Particle p : particles) {
            float alpha = (float) p.life / p.maxLife;
            g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int) (alpha * 255)));
            g.fillOval((int) p.x - p.size / 2, (int) p.y - p.size / 2, p.size, p.size);
        }

        // Render P1 (OCP RoboCop Cruiser)
        drawRoboCruiser(g, WIDTH / 2 + (int) (p1X * 160), HEIGHT - 100, true);

        // Render P2 / AI (ED-209 / Enforcer Unit)
        double relP2Pos = p2Position - p1Position;
        if (relP2Pos > -1000 && relP2Pos < 12000) {
            double p2Z = relP2Pos + 10;
            int p2ScreenY = (int) ((HEIGHT / 2.0) + (1400 / p2Z) * (HEIGHT / 2.0));
            int p2ScreenX = (int) (WIDTH / 2.0 + (p2X - p1X) * 450);
            int p2Size = Math.max(20, Math.min(110, (int) (11000 / p2Z)));
            drawEd209Unit(g, p2ScreenX, p2ScreenY - p2Size, p2Size);
        }

        // Draw RoboCop Visor HUD
        drawRoboCopHUD(g);
    }

    private void drawDeltaCitySkyline(Graphics2D g, int offset) {
        g.setColor(new Color(16, 20, 28));
        int shift = (offset % 1200);
        for (int i = -1; i < 5; i++) {
            int bx = i * 300 + shift;
            g.fillRect(bx, HEIGHT / 2 - 170, 95, 170); // OCP Megastructure
            g.fillRect(bx + 110, HEIGHT / 2 - 230, 80, 230);
            g.fillRect(bx + 200, HEIGHT / 2 - 140, 75, 140);
        }
    }

    private void drawRoboCruiser(Graphics2D g, int x, int y, boolean isP1) {
        int w = 84;
        int h = 42;

        // Shadow
        g.setColor(new Color(0, 0, 0, 150));
        g.fillOval(x - w / 2, y + h - 6, w, 18);

        // OCP Steel Body Armor
        g.setColor(new Color(110, 120, 135));
        g.fillRoundRect(x - w / 2, y, w, h, 12, 12);

        // Metallic Trim Overlay
        g.setColor(new Color(180, 195, 210));
        g.fillRect(x - w / 4, y + 4, w / 2, 8);

        // Police Lightbar (Flashing Red/Blue)
        boolean flash = (System.currentTimeMillis() / 150) % 2 == 0;
        g.setColor(flash ? Color.RED : Color.BLUE);
        g.fillRect(x - 16, y + 14, 14, 6);
        g.setColor(flash ? Color.BLUE : Color.RED);
        g.fillRect(x + 2, y + 14, 14, 6);

        // Wheels
        g.setColor(new Color(15, 15, 18));
        g.fillRect(x - w / 2 - 5, y + 4, 8, 12);
        g.fillRect(x + w / 2 - 3, y + 4, 8, 12);
        g.fillRect(x - w / 2 - 5, y + h - 15, 8, 12);
        g.fillRect(x + w / 2 - 3, y + h - 15, 8, 12);

        // Label Tag
        g.setColor(Color.CYAN);
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.drawString(isP1 ? "ROBOCOP (P1)" : "P2", x - 32, y - 8);
    }

    private void drawEd209Unit(Graphics2D g, int x, int y, int size) {
        // ED-209 Mechanical Legs & Chassis
        g.setColor(new Color(80, 85, 95));
        g.fillOval(x - size / 2, y, size, size / 2 + 10);

        // Arm Auto-Cannons
        g.setColor(Color.DARK_GRAY);
        g.fillRect(x - size / 2 - 10, y + 10, 12, size / 3);
        g.fillRect(x + size / 2 - 2, y + 10, 12, size / 3);

        // Red Sensor Visor Band
        g.setColor(Color.RED);
        g.fillRect(x - size / 3, y + size / 5, (size / 3) * 2, 6);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.drawString(isMultiplayer ? "P2 ED-209" : "ED-209 AI", x - 30, y - 6);
    }

    private void drawObstacle(Graphics2D g, int x, int y, int size, int type) {
        if (type == 0) { // Nuke Drug Barrels (Glowing Magenta)
            g.setColor(new Color(240, 0, 180));
            g.fillOval(x - size / 2, y, size, size);
            g.setColor(Color.BLACK);
            g.drawString("NUKE", x - 15, y + size / 2);
        } else if (type == 1) { // Rogue Patrol Drone
            g.setColor(Color.CYAN);
            g.fillRect(x - size / 2, y, size, size / 2);
            g.setColor(Color.RED);
            g.fillOval(x - 5, y + 2, 10, 10);
        } else { // OCP Steel Road Barricade
            g.setColor(Color.GRAY);
            g.fillRect(x - size, y + size / 3, size * 2, size / 3);
            g.setColor(Color.YELLOW);
            g.fillRect(x - size / 2, y, size, size / 4);
        }
    }

    private void drawRoboCopHUD(Graphics2D g) {
        // Overlay HUD Framework (RoboCop Helmet Visor Style)
        g.setColor(new Color(0, 255, 128, 180));
        g.drawRect(20, 20, 310, 120);

        // Scanning Visor Target Reticle
        int reticleX = WIDTH / 2 + (int) (p1X * 160);
        int reticleY = HEIGHT - 120;
        g.drawOval(reticleX - 25, reticleY - 25, 50, 50);
        g.drawLine(reticleX - 35, reticleY, reticleX + 35, reticleY);
        g.drawLine(reticleX, reticleY - 35, reticleX, reticleY + 35);

        // Tactical Telemetry Text
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString("OCP TELEMETRY // DIRECTIVE 4", 32, 42);

        g.setColor(Color.WHITE);
        g.drawString("SPEED : " + (int) (p1Speed / 100) + " MPH", 32, 65);
        g.drawString("SECTOR: LAP " + Math.min(3, p1Laps + 1) + " / 3", 32, 85);

        // Auto-9 Targeting Status
        g.drawString("AUTO-9 LOCK: ", 32, 110);
        if (p1TargetingCooldown <= 0) {
            g.setColor(Color.GREEN);
            g.drawString("READY [SPACE]", 145, 110);
        } else {
            g.setColor(Color.RED);
            g.drawString(String.format("%.1fs COOLING", p1TargetingCooldown), 145, 110);
        }

        // Horizontal Scanline Visor Pulse
        int scanY = (int) ((scanlinePulse * 40) % HEIGHT);
        g.setColor(new Color(0, 255, 128, 30));
        g.fillRect(0, scanY, WIDTH, 4);
    }

    private void drawGameOver(Graphics2D g) {
        drawRace(g);

        g.setColor(new Color(0, 0, 0, 230));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(0, 240, 255));
        g.setFont(new Font("Monospaced", Font.BOLD, 42));

        String winner = (p1Laps >= 3) ? "ROBOCOP MISSION ACCOMPLISHED!" : (isMultiplayer ? "PLAYER 2 VICTORIOUS!" : "ED-209 ENFORCER VICTORIOUS!");
        g.drawString(winner, 110, 280);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        g.drawString("PRESS ENTER TO RETURN TO DIRECTIVE MENU", 250, 360);
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
        p1TargetingCooldown = 0;

        p2Position = 0;
        p2X = 0.3;
        p2Speed = 0;
        p2Laps = 0;
        p2TargetingCooldown = 0;

        particles.clear();
        buildDeltaCityTrack();
    }
}