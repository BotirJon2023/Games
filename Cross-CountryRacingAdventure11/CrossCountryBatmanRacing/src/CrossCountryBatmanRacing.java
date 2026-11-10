import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CrossCountryBatmanRacing extends JFrame {

    public CrossCountryBatmanRacing() {
        setTitle("Batman: Gotham Cross-Country Dark Pursuit");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        BatmanGamePanel gamePanel = new BatmanGamePanel();
        add(gamePanel);
        pack();

        setLocationRelativeTo(null);
        setVisible(true);

        gamePanel.startGame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CrossCountryBatmanRacing::new);
    }
}

class BatmanGamePanel extends JPanel implements Runnable, KeyListener {

    public static final int WIDTH = 960;
    public static final int HEIGHT = 600;

    // Core Loop
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
        public Color cityGlow;
        public Color rumbleColor;
        public Color roadColor;
        public List<Obstacle> obstacles = new ArrayList<>();
    }

    public static class Obstacle {
        public double xRatio; // -1.0 to +1.0
        public int type;      // 0: Joker Gas Bomb, 1: Spiked Barrier, 2: Police Barricade
        public boolean destroyed = false;
    }

    // Particle FX
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

    // Player 1 Engine (Batman / Tumbler)
    private double p1Position = 0;
    private double p1X = -0.3;
    private double p1Speed = 0;
    private double p1EmpCooldown = 0;
    private int p1Laps = 0;

    // Player 2 / AI Engine (Nightwing / Red Hood / AI Arkham Batwing)
    private double p2Position = 0;
    private double p2X = 0.3;
    private double p2Speed = 0;
    private double p2EmpCooldown = 0;
    private int p2Laps = 0;

    // Physics Dynamics
    private final double maxSpeed = 13500;
    private final double accel = 200;
    private final double braking = 350;
    private final double decel = 90;
    private final double offRoadDecel = 320;

    // Environment & Camera
    private double skyOffset = 0;
    private double batSignalPulse = 0;

    public BatmanGamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        buildGothamTrack();
        trackLength = totalSegments * segmentLength;
    }

    private void buildGothamTrack() {
        roadSegments.clear();
        Random rand = new Random(42); // Consistent layout

        for (int i = 0; i < totalSegments; i++) {
            RoadSegment seg = new RoadSegment();
            seg.index = i;
            seg.worldZ = i * segmentLength;

            // Curves & Gotham Docks/Highways Elevation Profile
            if (i > 80 && i < 280) seg.curve = 3.2;
            else if (i > 350 && i < 550) seg.curve = -3.5;
            else if (i > 650 && i < 850) seg.hill = Math.sin(i / 15.0) * 1800;
            else if (i > 1000 && i < 1400) { seg.curve = 4.0; seg.hill = Math.cos(i / 12.0) * 2200; }

            // Dynamic Biomes (Downtown Gotham -> Arkham Bridge -> Blackgate Docks)
            boolean alt = (i / 3) % 2 == 0;
            if (i < 600) { // Gotham Downtown (Neon Neon Blues & Purples)
                seg.cityGlow = alt ? new Color(15, 20, 35) : new Color(10, 12, 25);
            } else if (i < 1200) { // Industrial Docks (Toxic Greens & Corrosion)
                seg.cityGlow = alt ? new Color(12, 28, 18) : new Color(8, 18, 12);
            } else { // Arkham Expressway (Gothic Crimson Sky Line)
                seg.cityGlow = alt ? new Color(35, 12, 15) : new Color(22, 8, 10);
            }

            seg.rumbleColor = alt ? new Color(220, 180, 0) : new Color(40, 40, 40);
            seg.roadColor = alt ? new Color(30, 32, 38) : new Color(24, 26, 30);

            // Spawn Hazards
            if (i % 22 == 0 && i > 40) {
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
        batSignalPulse += 0.05;

        // --- Player 1 Controls (WASD / Arrows + SPACE for EMP) ---
        if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) p1Speed += accel;
        else if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]) p1Speed -= braking;
        else p1Speed -= decel;

        if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]) p1X -= 0.038 * (p1Speed / maxSpeed);
        if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]) p1X += 0.038 * (p1Speed / maxSpeed);

        p1EmpCooldown = Math.max(0, p1EmpCooldown - dt);
        if (keys[KeyEvent.VK_SPACE] && p1EmpCooldown <= 0) {
            fireEmpBlast(p1Position, p1X);
            p1EmpCooldown = 3.0; // 3 sec cooldown
        }

        // --- Player 2 / AI Controls ---
        if (isMultiplayer) {
            if (keys[KeyEvent.VK_I]) p2Speed += accel;
            else if (keys[KeyEvent.VK_K]) p2Speed -= braking;
            else p2Speed -= decel;

            if (keys[KeyEvent.VK_J]) p2X -= 0.038 * (p2Speed / maxSpeed);
            else if (keys[KeyEvent.VK_L]) p2X += 0.038 * (p2Speed / maxSpeed);

            p2EmpCooldown = Math.max(0, p2EmpCooldown - dt);
            if (keys[KeyEvent.VK_U] && p2EmpCooldown <= 0) {
                fireEmpBlast(p2Position, p2X);
                p2EmpCooldown = 3.0;
            }
        } else {
            // Adaptive Arkham Bat-AI
            double aiTargetSpeed = maxSpeed * 0.90;
            if (p2Speed < aiTargetSpeed) p2Speed += accel * 0.85;
            else p2Speed -= decel;

            // AI Hazard Avoidance
            int aiSegIndex = (int) (p2Position / segmentLength) % totalSegments;
            RoadSegment currentAiSeg = roadSegments.get(aiSegIndex);
            for (Obstacle obs : currentAiSeg.obstacles) {
                if (!obs.destroyed && Math.abs(p2X - obs.xRatio) < 0.35) {
                    p2X += (p2X > 0) ? -0.03 : 0.03;
                }
            }
        }

        // Physics Bounds & Off-road Penalty
        p1Speed = Math.min(Math.max(p1Speed, 0), maxSpeed);
        p2Speed = Math.min(Math.max(p2Speed, 0), maxSpeed);

        if (Math.abs(p1X) > 1.0) p1Speed = Math.max(0, p1Speed - offRoadDecel);
        if (Math.abs(p2X) > 1.0) p2Speed = Math.max(0, p2Speed - offRoadDecel);

        // Position Updates
        p1Position += p1Speed * dt * 60;
        p2Position += p2Speed * dt * 60;

        p1Laps = (int) (p1Position / trackLength);
        p2Laps = (int) (p2Position / trackLength);

        // Particle Thruster Effects
        spawnThrusterParticles(WIDTH / 2 + (int) (p1X * 160), HEIGHT - 90, p1Speed > 5000);

        // Update Active Particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.life--;
            if (p.life <= 0) particles.remove(i);
        }

        // Camera Pan
        int currentP1Seg = (int) (p1Position / segmentLength) % totalSegments;
        skyOffset -= roadSegments.get(currentP1Seg).curve * (p1Speed / maxSpeed) * 0.35;

        // Game Over Check
        if (p1Laps >= 3 || p2Laps >= 3) {
            currentState = State.GAME_OVER;
        }
    }

    private void fireEmpBlast(double pPosition, double pX) {
        int segIndex = (int) (pPosition / segmentLength) % totalSegments;
        for (int i = 0; i < 8; i++) { // Clear hazards ahead
            RoadSegment seg = roadSegments.get((segIndex + i) % totalSegments);
            for (Obstacle obs : seg.obstacles) {
                if (Math.abs(pX - obs.xRatio) < 0.6) {
                    obs.destroyed = true;
                }
            }
        }
    }

    private void spawnThrusterParticles(int x, int y, boolean highSpeed) {
        Random rand = new Random();
        int count = highSpeed ? 4 : 2;
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x = x + (rand.nextDouble() * 16 - 8);
            p.y = y + 10;
            p.vx = (rand.nextDouble() * 2 - 1);
            p.vy = rand.nextDouble() * 4 + 3;
            p.life = 15 + rand.nextInt(10);
            p.maxLife = p.life;
            p.color = highSpeed ? new Color(0, 180, 255) : new Color(255, 120, 0);
            p.size = 6 + rand.nextInt(6);
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
        g.setColor(new Color(10, 12, 18));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Glowing Gotham Sky Silhouette
        drawGothamSkyline(g, 0);
        drawBatSignal(g, WIDTH / 2, 140);

        g.setColor(new Color(240, 200, 0));
        g.setFont(new Font("Serif", Font.BOLD, 42));
        g.drawString("BATMAN: GOTHAM DARK PURSUIT", 140, 280);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("Press 1: Single Player (VS Arkham AI Batwing)", 230, 360);
        g.drawString("Press 2: Two Player (Local Batmobile vs Batwing)", 230, 410);

        g.setFont(new Font("SansSerif", Font.ITALIC, 15));
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("P1 Controls: WASD/Arrows (Steer/Accelerate), SPACE (EMP Shield Shockwave)", 200, 490);
        g.drawString("P2 Controls: IJKL (Steer/Accelerate), U (EMP Shockwave)", 200, 520);
    }

    private void drawRace(Graphics2D g) {
        // Gotham Atmospheric Background
        g.setColor(new Color(10, 12, 18));
        g.fillRect(0, 0, WIDTH, HEIGHT / 2);

        drawGothamSkyline(g, (int) skyOffset);
        drawBatSignal(g, (int) (WIDTH / 2 + (skyOffset % 300)), 120);

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

            // Render Ground / City Terrain
            g.setColor(seg.cityGlow);
            g.fillRect(0, y2, WIDTH, y1 - y2);

            // Rumble Strips & Road Surface
            g.setColor(seg.rumbleColor);
            g.fillRect(x1 - w1 - w1 / 6, y2, (int) (w1 * 2.33), y1 - y2);

            g.setColor(seg.roadColor);
            g.fillRect(x1 - w1, y2, w1 * 2, y1 - y2);

            // Glowing Yellow Road Dividers
            if ((seg.index / 3) % 2 == 0) {
                g.setColor(new Color(255, 210, 0, 180));
                g.fillRect(x1 - w1 / 25, y2, Math.max(2, w1 / 12), y1 - y2);
            }

            // Road Side Obstacles
            for (Obstacle obs : seg.obstacles) {
                if (obs.destroyed) continue;
                int spriteX = (int) (x1 + obs.xRatio * w1);
                int spriteSize = Math.max(12, (int) (13000 / z1));
                drawObstacle(g, spriteX, y1 - spriteSize, spriteSize, obs.type);
            }
        }

        // Draw Particle Engine
        for (Particle p : particles) {
            float alpha = (float) p.life / p.maxLife;
            g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int) (alpha * 255)));
            g.fillOval((int) p.x - p.size / 2, (int) p.y - p.size / 2, p.size, p.size);
        }

        // Draw P1 (Batmobile Tumbler)
        drawBatmobile(g, WIDTH / 2 + (int) (p1X * 160), HEIGHT - 100, true);

        // Draw P2 / AI relative vehicle
        double relP2Pos = p2Position - p1Position;
        if (relP2Pos > -1000 && relP2Pos < 12000) {
            double p2Z = relP2Pos + 10;
            int p2ScreenY = (int) ((HEIGHT / 2.0) + (1400 / p2Z) * (HEIGHT / 2.0));
            int p2ScreenX = (int) (WIDTH / 2.0 + (p2X - p1X) * 450);
            int p2Size = Math.max(20, Math.min(110, (int) (11000 / p2Z)));
            drawBatwing(g, p2ScreenX, p2ScreenY - p2Size, p2Size);
        }

        // Tactical HUD
        drawBatmanHUD(g);
    }

    private void drawGothamSkyline(Graphics2D g, int offset) {
        g.setColor(new Color(18, 22, 32));
        int shift = (offset % 1200);
        for (int i = -1; i < 5; i++) {
            int bx = i * 300 + shift;
            g.fillRect(bx, HEIGHT / 2 - 160, 90, 160);
            g.fillRect(bx + 110, HEIGHT / 2 - 220, 70, 220);
            g.fillRect(bx + 190, HEIGHT / 2 - 130, 80, 130);
        }
    }

    private void drawBatSignal(Graphics2D g, int cx, int cy) {
        // Light Beam Projection
        Polygon beam = new Polygon();
        beam.addPoint(cx - 20, HEIGHT / 2);
        beam.addPoint(cx + 20, HEIGHT / 2);
        beam.addPoint(cx + 90, cy);
        beam.addPoint(cx - 90, cy);

        g.setColor(new Color(255, 230, 120, 35));
        g.fill(beam);

        // Signal Oval
        int rX = 65, rY = 40;
        double pulse = Math.sin(batSignalPulse) * 4;
        g.setColor(new Color(255, 230, 130, 200));
        g.fillOval((int) (cx - rX - pulse / 2), (int) (cy - rY - pulse / 2), (int) (rX * 2 + pulse), (int) (rY * 2 + pulse));

        // Bat Silhouette Symbol inside Signal
        g.setColor(Color.BLACK);
        Path2D bat = new Path2D.Double();
        bat.moveTo(cx, cy - 10);
        bat.curveTo(cx + 10, cy - 25, cx + 30, cy - 10, cx + 45, cy - 20);
        bat.curveTo(cx + 35, cy + 15, cx + 15, cy + 10, cx, cy + 25);
        bat.curveTo(cx - 15, cy + 10, cx - 35, cy + 15, cx - 45, cy - 20);
        bat.curveTo(cx - 30, cy - 10, cx - 10, cy - 25, cx, cy - 10);
        g.fill(bat);
    }

    private void drawBatmobile(Graphics2D g, int x, int y, boolean isP1) {
        int w = 84;
        int h = 42;

        // Dynamic Armor Shadow
        g.setColor(new Color(0, 0, 0, 140));
        g.fillOval(x - w / 2, y + h - 6, w, 18);

        // Armor Plating Body
        g.setColor(new Color(28, 30, 36));
        g.fillRoundRect(x - w / 2, y, w, h, 14, 14);

        // Angular Cockpit Canopy
        g.setColor(new Color(80, 160, 240, 180));
        Polygon canopy = new Polygon();
        canopy.addPoint(x - 18, y + 10);
        canopy.addPoint(x + 18, y + 10);
        canopy.addPoint(x + 12, y + 22);
        canopy.addPoint(x - 12, y + 22);
        g.fill(canopy);

        // Rear Jet Thruster Exhaust
        g.setColor(new Color(220, 50, 20));
        g.fillOval(x - 10, y + h - 8, 20, 10);

        // Heavy Wheels
        g.setColor(new Color(12, 12, 14));
        g.fillRect(x - w / 2 - 6, y + 4, 10, 14);
        g.fillRect(x + w / 2 - 4, y + 4, 10, 14);
        g.fillRect(x - w / 2 - 6, y + h - 16, 10, 14);
        g.fillRect(x + w / 2 - 4, y + h - 16, 10, 14);

        // Designation Tag
        g.setColor(Color.YELLOW);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.drawString(isP1 ? "BATMAN (P1)" : "P2", x - 28, y - 8);
    }

    private void drawBatwing(Graphics2D g, int x, int y, int size) {
        g.setColor(new Color(20, 22, 28));

        // Arc Wings
        Polygon wings = new Polygon();
        wings.addPoint(x, y);
        wings.addPoint(x + size, y + size / 2);
        wings.addPoint(x + size / 2, y + size);
        wings.addPoint(x, y + size / 3 * 2);
        wings.addPoint(x - size / 2, y + size);
        wings.addPoint(x - size, y + size / 2);
        g.fill(wings);

        // Red Night-Vision Visor
        g.setColor(Color.RED);
        g.fillOval(x - size / 6, y + size / 3, size / 3, size / 5);

        g.setColor(Color.CYAN);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString(isMultiplayer ? "P2 BATWING" : "ARKHAM AI", x - 30, y - 6);
    }

    private void drawObstacle(Graphics2D g, int x, int y, int size, int type) {
        if (type == 0) { // Joker Gas Bomb (Toxic Green)
            g.setColor(new Color(50, 220, 80));
            g.fillOval(x - size / 2, y, size, size);
            g.setColor(new Color(180, 40, 200));
            g.drawString("JOKER", x - 18, y - 4);
        } else if (type == 1) { // Spiked Road Barrier
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x - size, y + size / 3, size * 2, size / 3);
            g.setColor(Color.RED);
            g.fillRect(x - size / 2, y, size, size / 3);
        } else { // Rogue Crate
            g.setColor(new Color(120, 80, 40));
            g.fillRect(x - size / 2, y, size, size);
        }
    }

    private void drawBatmanHUD(Graphics2D g) {
        // Dark Knight Tactical Frame
        g.setColor(new Color(12, 16, 24, 210));
        g.fillRect(15, 15, 290, 110);
        g.setColor(new Color(240, 190, 0));
        g.drawRect(15, 15, 290, 110);

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(Color.CYAN);
        g.drawString("BAT-COMPUTER TACTICAL HUD", 25, 38);

        g.setColor(Color.WHITE);
        g.drawString("P1 SPEED: " + (int) (p1Speed / 100) + " MPH", 25, 60);
        g.drawString("P1 LAP  : " + Math.min(3, p1Laps + 1) + " / 3", 25, 78);

        // EMP Cooldown Meter
        g.drawString("EMP SHIELD: ", 25, 100);
        if (p1EmpCooldown <= 0) {
            g.setColor(Color.GREEN);
            g.drawString("READY [SPACE]", 130, 100);
        } else {
            g.setColor(Color.RED);
            g.drawString(String.format("%.1fs RECHARGING", p1EmpCooldown), 130, 100);
        }
    }

    private void drawGameOver(Graphics2D g) {
        drawRace(g);

        g.setColor(new Color(0, 0, 0, 220));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(255, 210, 0));
        g.setFont(new Font("Serif", Font.BOLD, 48));

        String winner = (p1Laps >= 3) ? "BATMAN VICTORIOUS!" : (isMultiplayer ? "PLAYER 2 WINS!" : "ARKHAM AI WINS!");
        g.drawString(winner, 230, 280);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString("Press ENTER to return to Main Menu", 290, 360);
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
        p1EmpCooldown = 0;

        p2Position = 0;
        p2X = 0.3;
        p2Speed = 0;
        p2Laps = 0;
        p2EmpCooldown = 0;

        particles.clear();
        buildGothamTrack();
    }
}