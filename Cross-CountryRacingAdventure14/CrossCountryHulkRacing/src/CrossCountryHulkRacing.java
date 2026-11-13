import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CrossCountryHulkRacing extends JFrame {

    public CrossCountryHulkRacing() {
        setTitle("The Hulk: Gamma Smash Cross-Country Pursuit");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        HulkGamePanel gamePanel = new HulkGamePanel();
        add(gamePanel);
        pack();

        setLocationRelativeTo(null);
        setVisible(true);

        gamePanel.startGame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CrossCountryHulkRacing::new);
    }
}

class HulkGamePanel extends JPanel implements Runnable, KeyListener {

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

    // Input Tracking
    private final boolean[] keys = new boolean[256];

    // Road Projection Segment
    public static class RoadSegment {
        public int index;
        public double worldZ;
        public double curve;
        public double hill;
        public Color gammaGlow;
        public Color rumbleColor;
        public Color roadColor;
        public List<Obstacle> obstacles = new ArrayList<>();
    }

    public static class Obstacle {
        public double xRatio; // -1.0 to +1.0
        public int type;      // 0: Gamma Radiation Barrel, 1: Military Tank Barrier, 2: Fallen Boulder
        public boolean destroyed = false;
    }

    // Particle & Shockwave FX
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

    // Player 1 Engine (The Incredible Hulk)
    private double p1Position = 0;
    private double p1X = -0.3;
    private double p1Speed = 0;
    private double p1SmashCooldown = 0;
    private double p1SmashRadius = 0; // Visual shockwave ring radius
    private int p1Laps = 0;

    // Player 2 / AI Engine (Hulkbuster Mech / Red Hulk)
    private double p2Position = 0;
    private double p2X = 0.3;
    private double p2Speed = 0;
    private double p2SmashCooldown = 0;
    private double p2SmashRadius = 0;
    private int p2Laps = 0;

    // Physics Mechanics
    private final double maxSpeed = 14500;
    private final double accel = 230;
    private final double braking = 400;
    private final double decel = 90;
    private final double offRoadDecel = 300; // Hulk barely slows down off-road

    // Horizon & Gamma Visual Controls
    private double skyOffset = 0;
    private double gammaPulse = 0;

    public HulkGamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        buildGammaWastelandTrack();
        trackLength = totalSegments * segmentLength;
    }

    private void buildGammaWastelandTrack() {
        roadSegments.clear();
        Random rand = new Random(1962); // Deterministic Hulk terrain generation

        for (int i = 0; i < totalSegments; i++) {
            RoadSegment seg = new RoadSegment();
            seg.index = i;
            seg.worldZ = i * segmentLength;

            // Curved Terrain & High Mountain Jumps
            if (i > 90 && i < 290) seg.curve = 3.8;
            else if (i > 380 && i < 580) seg.curve = -4.0;
            else if (i > 700 && i < 900) seg.hill = Math.sin(i / 12.0) * 2200;
            else if (i > 1100 && i < 1500) { seg.curve = 4.5; seg.hill = Math.cos(i / 8.0) * 2600; }

            // Dynamic Biomes (Mojave Gamma Site -> Military Base Highway -> Irradiated Canyon)
            boolean alt = (i / 3) % 2 == 0;
            if (i < 600) { // Mojave Gamma Test Site (Radioactive Green & Toxic Yellow)
                seg.gammaGlow = alt ? new Color(20, 45, 15) : new Color(12, 30, 10);
            } else if (i < 1200) { // Red Hulk Desert Canyon (Crimson Dust & Rock)
                seg.gammaGlow = alt ? new Color(50, 18, 12) : new Color(32, 10, 8);
            } else { // Gamma Core Reactor Pass (Neon Green Pulse)
                seg.gammaGlow = alt ? new Color(15, 60, 20) : new Color(8, 40, 12);
            }

            seg.rumbleColor = alt ? new Color(50, 220, 50) : new Color(30, 40, 30);
            seg.roadColor = alt ? new Color(35, 38, 35) : new Color(22, 25, 22);

            // Spawn Hazards
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
        gammaPulse += 0.08;

        // --- Player 1 Controls (WASD / Arrows + SPACE for Gamma Ground Smash) ---
        if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) p1Speed += accel;
        else if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]) p1Speed -= braking;
        else p1Speed -= decel;

        if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]) p1X -= 0.045 * (p1Speed / maxSpeed);
        if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]) p1X += 0.045 * (p1Speed / maxSpeed);

        p1SmashCooldown = Math.max(0, p1SmashCooldown - dt);
        if (p1SmashRadius > 0) p1SmashRadius += 12; // Expand shockwave animation
        if (p1SmashRadius > 140) p1SmashRadius = 0;

        if (keys[KeyEvent.VK_SPACE] && p1SmashCooldown <= 0) {
            triggerGammaSmash(p1Position, p1X, true);
            p1SmashCooldown = 2.0; // 2 sec cooldown
            p1SmashRadius = 10;
        }

        // --- Player 2 / AI Controls ---
        if (isMultiplayer) {
            if (keys[KeyEvent.VK_I]) p2Speed += accel;
            else if (keys[KeyEvent.VK_K]) p2Speed -= braking;
            else p2Speed -= decel;

            if (keys[KeyEvent.VK_J]) p2X -= 0.045 * (p2Speed / maxSpeed);
            else if (keys[KeyEvent.VK_L]) p2X += 0.045 * (p2Speed / maxSpeed);

            p2SmashCooldown = Math.max(0, p2SmashCooldown - dt);
            if (p2SmashRadius > 0) p2SmashRadius += 12;
            if (p2SmashRadius > 140) p2SmashRadius = 0;

            if (keys[KeyEvent.VK_U] && p2SmashCooldown <= 0) {
                triggerGammaSmash(p2Position, p2X, false);
                p2SmashCooldown = 2.0;
                p2SmashRadius = 10;
            }
        } else {
            // Adaptive Hulkbuster AI Mechanics
            double aiTargetSpeed = maxSpeed * 0.94;
            if (p2Speed < aiTargetSpeed) p2Speed += accel * 0.92;
            else p2Speed -= decel;

            // AI Evasion
            int aiSegIndex = (int) (p2Position / segmentLength) % totalSegments;
            RoadSegment currentAiSeg = roadSegments.get(aiSegIndex);
            for (Obstacle obs : currentAiSeg.obstacles) {
                if (!obs.destroyed && Math.abs(p2X - obs.xRatio) < 0.35) {
                    p2X += (p2X > 0) ? -0.045 : 0.045;
                }
            }
        }

        // Speed Limits & Off-Road Physics
        p1Speed = Math.min(Math.max(p1Speed, 0), maxSpeed);
        p2Speed = Math.min(Math.max(p2Speed, 0), maxSpeed);

        if (Math.abs(p1X) > 1.0) p1Speed = Math.max(0, p1Speed - offRoadDecel);
        if (Math.abs(p2X) > 1.0) p2Speed = Math.max(0, p2Speed - offRoadDecel);

        // Position Logic
        p1Position += p1Speed * dt * 60;
        p2Position += p2Speed * dt * 60;

        p1Laps = (int) (p1Position / trackLength);
        p2Laps = (int) (p2Position / trackLength);

        // Ground Stomp & Dust Particles
        spawnStompParticles(WIDTH / 2 + (int) (p1X * 160), HEIGHT - 90, p1Speed > 6000);

        // Particle Update Loop
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.life--;
            if (p.life <= 0) particles.remove(i);
        }

        // Horizon Shift
        int currentP1Seg = (int) (p1Position / segmentLength) % totalSegments;
        skyOffset -= roadSegments.get(currentP1Seg).curve * (p1Speed / maxSpeed) * 0.42;

        // Victory Condition Check
        if (p1Laps >= 3 || p2Laps >= 3) {
            currentState = State.GAME_OVER;
        }
    }

    private void triggerGammaSmash(double pPosition, double pX, boolean isP1) {
        int segIndex = (int) (pPosition / segmentLength) % totalSegments;
        for (int i = 0; i < 12; i++) {
            RoadSegment seg = roadSegments.get((segIndex + i) % totalSegments);
            for (Obstacle obs : seg.obstacles) {
                if (Math.abs(pX - obs.xRatio) < 0.75) {
                    obs.destroyed = true;
                }
            }
        }

        // Gamma Explosion FX
        Random rand = new Random();
        for (int k = 0; k < 20; k++) {
            Particle p = new Particle();
            p.x = WIDTH / 2 + (int) (pX * 160);
            p.y = HEIGHT - 110;
            p.vx = (rand.nextDouble() * 16 - 8);
            p.vy = -(rand.nextDouble() * 10 + 6);
            p.life = 16 + rand.nextInt(10);
            p.maxLife = p.life;
            p.color = isP1 ? new Color(50, 255, 50) : new Color(255, 50, 50);
            p.size = 10 + rand.nextInt(8);
            particles.add(p);
        }
    }

    private void spawnStompParticles(int x, int y, boolean highSpeed) {
        Random rand = new Random();
        int count = highSpeed ? 5 : 2;
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x = x + (rand.nextDouble() * 18 - 9);
            p.y = y + 12;
            p.vx = (rand.nextDouble() * 3 - 1.5);
            p.vy = rand.nextDouble() * 4 + 2;
            p.life = 12 + rand.nextInt(8);
            p.maxLife = p.life;
            p.color = highSpeed ? new Color(80, 255, 80) : new Color(140, 110, 60);
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
        g.setColor(new Color(12, 18, 12));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        drawGammaSkyline(g, 0);

        // Hulk Banner
        g.setColor(new Color(50, 255, 50));
        g.setFont(new Font("Impact", Font.PLAIN, 46));
        g.drawString("THE HULK: GAMMA CROSS-COUNTRY SMASH", 100, 250);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.drawString("PRESS 1: SINGLE PLAYER (HULK VS HULKBUSTER AI)", 200, 340);
        g.drawString("PRESS 2: TWO PLAYER (HULK VS RED HULK / HULKBUSTER)", 180, 380);

        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g.setColor(Color.GREEN);
        g.drawString("P1 COMMANDS: WASD/ARROWS (STEER) | SPACE (GAMMA GROUND SMASH)", 160, 470);
        g.drawString("P2 COMMANDS: IJKL (STEER)       | U (GAMMA GROUND SMASH)", 160, 500);
    }

    private void drawRace(Graphics2D g) {
        // Toxic Gamma Sky
        g.setColor(new Color(10, 20, 10));
        g.fillRect(0, 0, WIDTH, HEIGHT / 2);

        drawGammaSkyline(g, (int) skyOffset);

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
            g.setColor(seg.gammaGlow);
            g.fillRect(0, y2, WIDTH, y1 - y2);

            // Rumble Strips & Road Surface
            g.setColor(seg.rumbleColor);
            g.fillRect(x1 - w1 - w1 / 6, y2, (int) (w1 * 2.33), y1 - y2);

            g.setColor(seg.roadColor);
            g.fillRect(x1 - w1, y2, w1 * 2, y1 - y2);

            // Glowing Gamma Center Lines
            if ((seg.index / 3) % 2 == 0) {
                g.setColor(new Color(50, 255, 50, 210));
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

        // Render Particle Engine
        for (Particle p : particles) {
            float alpha = (float) p.life / p.maxLife;
            g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int) (alpha * 255)));
            g.fillOval((int) p.x - p.size / 2, (int) p.y - p.size / 2, p.size, p.size);
        }

        // Render P1 (The Incredible Hulk)
        int p1CenterX = WIDTH / 2 + (int) (p1X * 160);
        int p1CenterY = HEIGHT - 100;
        drawHulkCharacter(g, p1CenterX, p1CenterY, new Color(40, 180, 40), "HULK (P1)");

        // Shockwave Ring Animation
        if (p1SmashRadius > 0) {
            g.setColor(new Color(50, 255, 50, 160));
            g.drawOval(p1CenterX - (int) p1SmashRadius, p1CenterY - (int) (p1SmashRadius / 2), (int) p1SmashRadius * 2, (int) p1SmashRadius);
        }

        // Render P2 / AI (Hulkbuster Mech / Red Hulk)
        double relP2Pos = p2Position - p1Position;
        if (relP2Pos > -1000 && relP2Pos < 12000) {
            double p2Z = relP2Pos + 10;
            int p2ScreenY = (int) ((HEIGHT / 2.0) + (1400 / p2Z) * (HEIGHT / 2.0));
            int p2ScreenX = (int) (WIDTH / 2.0 + (p2X - p1X) * 450);
            int p2Size = Math.max(20, Math.min(110, (int) (11000 / p2Z)));
            drawHulkbusterMech(g, p2ScreenX, p2ScreenY - p2Size, p2Size);
        }

        // Gamma Rage HUD Overlay
        drawHulkHUD(g);
    }

    private void drawGammaSkyline(Graphics2D g, int offset) {
        g.setColor(new Color(18, 30, 18));
        int shift = (offset % 1200);
        for (int i = -1; i < 5; i++) {
            int bx = i * 300 + shift;
            g.fillRect(bx, HEIGHT / 2 - 160, 110, 160); // Military Outpost
            g.fillRect(bx + 130, HEIGHT / 2 - 220, 80, 220); // Gamma Reactor Silo
            g.fillRect(bx + 220, HEIGHT / 2 - 140, 70, 140);
        }
    }

    private void drawHulkCharacter(Graphics2D g, int x, int y, Color skinColor, String label) {
        int w = 70;
        int h = 55;

        // Shadow
        g.setColor(new Color(0, 0, 0, 160));
        g.fillOval(x - w / 2, y + h - 10, w, 20);

        // Hulk Massive Torso
        g.setColor(skinColor);
        g.fillRoundRect(x - w / 2, y + 10, w, h - 20, 18, 18);

        // Purple Shredded Shorts
        g.setColor(new Color(110, 40, 150));
        g.fillRect(x - w / 2 + 5, y + h - 22, w - 10, 16);

        // Muscular Arms / Fists
        g.setColor(skinColor);
        g.fillOval(x - w / 2 - 10, y + 12, 18, 26);
        g.fillOval(x + w / 2 - 8, y + 12, 18, 26);

        // Head & Dark Hair
        g.fillOval(x - 14, y - 2, 28, 20);
        g.setColor(Color.BLACK);
        g.fillRect(x - 12, y - 4, 24, 8);

        // Label Tag
        g.setColor(Color.GREEN);
        g.setFont(new Font("Impact", Font.PLAIN, 15));
        g.drawString(label, x - 26, y - 10);
    }

    private void drawHulkbusterMech(Graphics2D g, int x, int y, int size) {
        // Red Armor Chassis
        g.setColor(new Color(180, 30, 30));
        g.fillRoundRect(x - size / 2, y, size, size, 14, 14);

        // Gold Trim
        g.setColor(new Color(230, 180, 40));
        g.fillRect(x - size / 4, y + 6, size / 2, size / 4);

        // Glowing Arc Reactor Eyes
        g.setColor(Color.CYAN);
        g.fillOval(x - size / 6, y + size / 3, size / 3, size / 5);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Impact", Font.PLAIN, 14));
        g.drawString(isMultiplayer ? "P2 HULKBUSTER" : "HULKBUSTER AI", x - 35, y - 8);
    }

    private void drawObstacle(Graphics2D g, int x, int y, int size, int type) {
        if (type == 0) { // Gamma Radiation Barrel
            g.setColor(new Color(50, 220, 50));
            g.fillOval(x - size / 2, y, size, size);
            g.setColor(Color.BLACK);
            g.drawString("RAD", x - 12, y + size / 2);
        } else if (type == 1) { // Military Tank Barrier
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x - size, y + size / 3, size * 2, size / 3);
            g.setColor(Color.GREEN);
            g.fillRect(x - size / 2, y, size, size / 4);
        } else { // Fallen Rock Boulder
            g.setColor(new Color(110, 90, 70));
            g.fillOval(x - size / 2, y, size, size);
        }
    }

    private void drawHulkHUD(Graphics2D g) {
        // Gamma Meter Frame
        g.setColor(new Color(15, 35, 15, 210));
        g.fillRect(20, 20, 310, 115);
        g.setColor(new Color(50, 255, 50));
        g.drawRect(20, 20, 310, 115);

        g.setFont(new Font("Impact", Font.PLAIN, 18));
        g.drawString("GAMMA RAGE TELEMETRY", 32, 45);

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(Color.WHITE);
        g.drawString("SPEED     : " + (int) (p1Speed / 100) + " MPH", 32, 68);
        g.drawString("SECTOR LAP: " + Math.min(3, p1Laps + 1) + " / 3", 32, 88);

        // Gamma Ground Smash Charge Status
        g.drawString("GAMMA SMASH: ", 32, 110);
        if (p1SmashCooldown <= 0) {
            g.setColor(Color.GREEN);
            g.drawString("READY [SPACE]", 145, 110);
        } else {
            g.setColor(Color.RED);
            g.drawString(String.format("%.1fs CHARGING", p1SmashCooldown), 145, 110);
        }

        // Radioactive Screen Pulse
        int scanY = (int) ((gammaPulse * 30) % HEIGHT);
        g.setColor(new Color(50, 255, 50, 25));
        g.fillRect(0, scanY, WIDTH, 6);
    }

    private void drawGameOver(Graphics2D g) {
        drawRace(g);

        g.setColor(new Color(0, 0, 0, 230));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(50, 255, 50));
        g.setFont(new Font("Impact", Font.PLAIN, 52));

        String winner = (p1Laps >= 3) ? "HULK IS THE STRONGEST THERE IS!" : (isMultiplayer ? "PLAYER 2 VICTORIOUS!" : "HULKBUSTER AI VICTORIOUS!");
        g.drawString(winner, 100, 280);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        g.drawString("PRESS ENTER TO RESTART GAMMA PURSUIT", 270, 360);
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
        p1SmashCooldown = 0;
        p1SmashRadius = 0;

        p2Position = 0;
        p2X = 0.3;
        p2Speed = 0;
        p2Laps = 0;
        p2SmashCooldown = 0;
        p2SmashRadius = 0;

        particles.clear();
        buildGammaWastelandTrack();
    }
}