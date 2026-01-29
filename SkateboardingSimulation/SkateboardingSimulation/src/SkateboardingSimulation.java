import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class SkateboardingSimulation extends JPanel implements ActionListener, KeyListener {

    // ───────────────────────────────────────────────
    //  Constants & Game Settings
    // ───────────────────────────────────────────────
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 700;
    private static final int GROUND_BASE = HEIGHT - 80;
    private static final double GRAVITY = 0.48;
    private static final double JUMP_POWER = -13.5;
    private static final double MOVE_SPEED = 0.38;
    private static final double MAX_RUN_SPEED = 7.2;
    private static final double FRICTION = 0.982;
    private static final double AIR_FRICTION = 0.995;
    private static final int FPS = 60;
    private static final double CAMERA_LAG = 0.085;

    // ───────────────────────────────────────────────
    //  Game state variables
    // ───────────────────────────────────────────────
    private double playerX = 150;
    private double playerY = GROUND_BASE - 60;
    private double velX = 0;
    private double velY = 0;
    private double cameraX = 0;
    private boolean onGround = true;
    private boolean ducking = false;
    private boolean crashed = false;
    private boolean gameOver = false;
    private double score = 0;
    private double distance = 0;
    private int trickPoints = 0;
    private String currentTrick = "";
    private int trickTimer = 0;

    // Animation / timing
    private Timer timer;
    private long lastTime;
    private double angle = 0;           // board rotation
    private double targetAngle = 0;
    private double spin = 0;

    // Particles
    private final ArrayList<Particle> particles = new ArrayList<>();

    // Terrain
    private final ArrayList<TerrainSegment> terrain = new ArrayList<>();
    private final Random random = new Random(45123); // fixed seed for reproducibility

    // Input
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;

    // ───────────────────────────────────────────────
    //  Inner classes
    // ───────────────────────────────────────────────
    static class Particle {
        double x, y, vx, vy;
        int life, maxLife;
        Color color;

        Particle(double x, double y, double vx, double vy, int life, Color c) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.life = this.maxLife = life;
            this.color = c;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.18;
            life--;
        }

        void draw(Graphics2D g) {
            float alpha = (float) life / maxLife;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 220)));
            g.fillOval((int)(x - 3), (int)(y - 3), 6, 6);
        }
    }

    static class TerrainSegment {
        double x1, y1, x2, y2;
        boolean isGap;

        TerrainSegment(double x1, double y1, double x2, double y2, boolean gap) {
            this.x1 = x1; this.y1 = y1;
            this.x2 = x2; this.y2 = y2;
            this.isGap = gap;
        }

        double getYAtX(double px) {
            if (x2 - x1 < 0.001) return y1;
            double t = (px - x1) / (x2 - x1);
            return y1 + t * (y2 - y1);
        }
    }

    // ───────────────────────────────────────────────
    //  Constructor & Initialization
    // ───────────────────────────────────────────────
    public SkateboardingSimulation() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // sky blue
        setFocusable(true);
        addKeyListener(this);

        generateTerrain();

        timer = new Timer(1000 / FPS, this);
        lastTime = System.nanoTime();
        timer.start();
    }

    private void generateTerrain() {
        terrain.clear();
        double currentX = -400;
        double currentY = GROUND_BASE;

        terrain.add(new TerrainSegment(currentX - 800, currentY + 120, currentX, currentY, false));

        while (currentX < 15000) {
            double length = 80 + random.nextDouble() * 220;
            double heightChange = -35 + random.nextDouble() * 70;

            // sometimes make bigger jumps
            if (random.nextDouble() < 0.12) {
                length = 180 + random.nextDouble() * 340;
                heightChange = -80 + random.nextDouble() * 160;
            }

            double nextX = currentX + length;
            double nextY = currentY + heightChange;

            // occasional flat section
            if (random.nextDouble() < 0.18) {
                nextY = currentY;
            }

            boolean gap = random.nextDouble() < 0.09 && length > 140;

            terrain.add(new TerrainSegment(currentX, currentY, nextX, nextY, gap));

            currentX = nextX;
            currentY = nextY;
        }
    }

    // ───────────────────────────────────────────────
    //  Main game loop (ActionListener)
    // ───────────────────────────────────────────────
    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = (now - lastTime) / 1_000_000_000.0;
        lastTime = now;

        if (dt > 0.1) dt = 0.1; // prevent huge jumps after pause/focus loss

        if (!gameOver) {
            updatePhysics(dt);
            updateCamera();
            updateTricksAndScore();
            updateParticles();
        }

        repaint();
    }

    private void updatePhysics(double dt) {
        // ─── Input handling ────────────────────────────────
        double accel = 0;
        if (leftPressed)  accel -= 1.4;
        if (rightPressed) accel += 1.4;

        if (onGround) {
            velX += accel * 38 * dt;
            velX *= FRICTION;
            if (Math.abs(velX) > MAX_RUN_SPEED)
                velX = Math.signum(velX) * MAX_RUN_SPEED;
        } else {
            velX += accel * 8 * dt;
            velX *= AIR_FRICTION;
        }

        // Jump
        if (upPressed && onGround && !ducking) {
            velY = JUMP_POWER;
            onGround = false;
            targetAngle = 0;
            spin = 0;
        }

        // Duck / crouch (for grinds / lower center)
        ducking = downPressed;

        // Gravity
        velY += GRAVITY * 60 * dt;

        // Apply velocity
        playerX += velX * 60 * dt;
        playerY += velY * 60 * dt;

        distance = Math.max(distance, playerX - 150);

        // ─── Terrain collision ─────────────────────────────
        checkTerrainCollision();

        // ─── Rotation smoothing ────────────────────────────
        angle = lerpAngle(angle, targetAngle, 0.22);
    }

    private void checkTerrainCollision() {
        if (velY < -0.1 && playerY < GROUND_BASE - 400) return; // early out high up

        TerrainSegment current = null;
        double minDist = Double.MAX_VALUE;

        for (TerrainSegment seg : terrain) {
            if (playerX < seg.x1 - 50 || playerX > seg.x2 + 50) continue;

            double terrainY = seg.getYAtX(playerX);

            double dy = terrainY - playerY;
            if (dy > -80 && dy < 120) {
                double dist = Math.abs(dy);
                if (dist < minDist) {
                    minDist = dist;
                    current = seg;
                }
            }
        }

        if (current != null) {
            double terrainY = current.getYAtX(playerX);

            if (playerY + 38 >= terrainY && velY >= -1.2) {
                // Landed / touching
                playerY = terrainY - 38;
                velY = 0;
                onGround = true;

                if (Math.abs(angle) > 0.4) {
                    // bad landing → crash
                    if (Math.abs(angle) > 1.1 || Math.abs(spin) > 3) {
                        crash();
                    } else {
                        // small recovery
                        targetAngle = 0;
                        spin = 0;
                    }
                } else {
                    targetAngle = 0;
                }

                // small bounce if coming in fast
                if (velY > 12) {
                    velY = -velY * 0.28;
                    onGround = false;
                }
            }
            else if (playerY + 20 < terrainY && velY > 0) {
                // falling through → no collision this frame
            }
        } else {
            // no segment found → in air / over gap
            onGround = false;
        }

        // fell way below world
        if (playerY > HEIGHT + 200) {
            crash();
        }
    }

    private void crash() {
        crashed = true;
        gameOver = true;
        velX *= 0.4;
        velY = -6 + Math.random() * -4;

        // explosion particles
        for (int i = 0; i < 40; i++) {
            double a = Math.random() * Math.PI * 2;
            double s = 2 + Math.random() * 7;
            Color c = Math.random() < 0.6 ? new Color(220,80,40) : new Color(240,180,60);
            particles.add(new Particle(
                    playerX + Math.random()*20-10,
                    playerY + Math.random()*30-15,
                    Math.cos(a)*s + velX*0.5,
                    Math.sin(a)*s + velY*0.5 - 3,
                    35 + (int)(Math.random()*30), c));
        }
    }

    private void updateCamera() {
        double targetCam = playerX - WIDTH / 3.0;
        cameraX += (targetCam - cameraX) * CAMERA_LAG;
    }

    private void updateTricksAndScore() {
        score = distance * 0.6 + trickPoints;

        if (trickTimer > 0) trickTimer--;

        // very simple trick detection (ollie / air time)
        if (!onGround && velY < -2 && trickTimer == 0) {
            if (Math.random() < 0.35) {
                currentTrick = "Ollie";
                trickPoints += 80;
                trickTimer = 45;
            }
        }

        // kickflip / impossible placeholder
        if (Math.abs(spin) > 5 && onGround) {
            currentTrick = "Kickflip!";
            trickPoints += 350;
            trickTimer = 60;
            spin = 0;
        }
    }

    private void updateParticles() {
        particles.removeIf(p -> p.life <= 0);
        for (Particle p : particles) p.update();
    }

    private double lerpAngle(double a, double b, double t) {
        double diff = b - a;
        while (diff > Math.PI) diff -= Math.PI*2;
        while (diff < -Math.PI) diff += Math.PI*2;
        return a + diff * t;
    }

    // ───────────────────────────────────────────────
    //  Rendering
    // ───────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ─── Background ────────────────────────────────
        drawBackground(g2);

        // ─── Translate to camera ───────────────────────
        g2.translate(-cameraX, 0);

        // ─── Draw terrain ──────────────────────────────
        drawTerrain(g2);

        // ─── Draw player & board ───────────────────────
        drawPlayer(g2);

        // ─── Particles ─────────────────────────────────
        for (Particle p : particles) p.draw(g2);

        // ─── HUD ───────────────────────────────────────
        g2.translate(cameraX, 0); // reset camera for HUD
        drawHUD(g2);

        if (gameOver) {
            drawGameOver(g2);
        }
    }

    private void drawBackground(Graphics2D g) {
        // sky gradient
        GradientPaint sky = new GradientPaint(0, 0, new Color(90,160,230),
                0, HEIGHT, new Color(180,220,255));
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // some clouds
        g.setColor(new Color(255,255,255,100));
        drawCloud(g, 300 + cameraX*0.1, 120, 140);
        drawCloud(g, 780 + cameraX*0.07, 80, 180);
        drawCloud(g, 1400 + cameraX*0.12, 200, 110);
    }

    private void drawCloud(Graphics2D g, double x, double y, double size) {
        g.fillOval((int)(x-size*0.6), (int)(y-size*0.4), (int)(size*1.4), (int)(size*0.9));
        g.fillOval((int)(x-size*0.1), (int)(y-size*0.7), (int)(size*1.1), (int)(size*0.8));
        g.fillOval((int)(x+size*0.3), (int)(y-size*0.5), (int)(size*1.0), (int)(size*0.7));
    }

    private void drawTerrain(Graphics2D g) {
        Path2D path = new Path2D.Double();
        boolean first = true;

        for (TerrainSegment seg : terrain) {
            if (seg.x2 < cameraX - 200 || seg.x1 > cameraX + WIDTH + 200) continue;

            if (seg.isGap) {
                if (first) {
                    path.moveTo(seg.x1, seg.y1 + 300);
                    first = false;
                }
                path.lineTo(seg.x1, seg.y1 + 300);
                path.lineTo(seg.x2, seg.y2 + 300);
            } else {
                if (first) {
                    path.moveTo(seg.x1, seg.y1);
                    first = false;
                } else {
                    path.lineTo(seg.x1, seg.y1);
                }
                path.lineTo(seg.x2, seg.y2);
            }
        }

        // ground fill
        if (!first) {
            path.lineTo(path.getBounds2D().getMaxX(), HEIGHT + 200);
            path.lineTo(path.getBounds2D().getMinX() - 200, HEIGHT + 200);
            path.closePath();

            g.setColor(new Color(50, 120, 40));
            g.fill(path);

            // grass layer
            g.setColor(new Color(70, 160, 60));
            g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(path);
            g.setStroke(new BasicStroke(1));
        }
    }

    private void drawPlayer(Graphics2D g) {
        g.setColor(Color.BLACK);

        double px = playerX;
        double py = playerY;

        // ─── Board ─────────────────────────────────────
        AffineTransform old = g.getTransform();
        g.translate(px, py + 8);
        g.rotate(angle + spin * 0.07);

        // deck
        g.setColor(new Color(30, 30, 90));
        g.fillRoundRect(-36, -6, 72, 14, 12, 12);

        // grip tape
        g.setColor(new Color(60,60,60,180));
        g.fillRoundRect(-34, -4, 68, 8, 8, 8);

        // wheels
        g.setColor(Color.BLACK);
        g.fillOval(-28, -12, 16, 16);
        g.fillOval( 12, -12, 16, 16);
        g.setColor(Color.LIGHT_GRAY);
        g.fillOval(-25, -9, 10, 10);
        g.fillOval( 15, -9, 10, 10);

        g.setTransform(old);

        // ─── Skater (simple stick figure) ──────────────
        double legAngle = onGround ? (ducking ? -0.6 : Math.sin(playerX * 0.04) * 0.45) : -0.8 + velY * 0.06;
        double armAngle = onGround ? Math.sin(playerX * 0.07) * 0.7 : 1.2;

        // torso
        g.setStroke(new BasicStroke(5));
        g.drawLine((int)px, (int)(py-50), (int)px, (int)(py-15));

        // head
        g.setColor(new Color(255, 220, 180));
        g.fillOval((int)px-14, (int)py-64, 28, 28);
        g.setColor(Color.BLACK);
        g.fillOval((int)px-6, (int)py-54, 5, 8);  // eye

        // arms
        g.setColor(Color.BLACK);
        int armLen = 28;
        g.drawLine((int)px, (int)(py-40),
                (int)(px + Math.cos(armAngle + 1.5) * armLen),
                (int)(py-40 + Math.sin(armAngle + 1.5) * armLen));
        g.drawLine((int)px, (int)(py-40),
                (int)(px + Math.cos(-armAngle - 0.4) * armLen),
                (int)(py-40 + Math.sin(-armAngle - 0.4) * armLen));

        // legs
        int legLen = ducking ? 20 : 34;
        g.drawLine((int)px, (int)(py-15),
                (int)(px + Math.cos(legAngle + 0.9) * legLen),
                (int)(py-15 + Math.sin(legAngle + 0.9) * legLen));
        g.drawLine((int)px, (int)(py-15),
                (int)(px + Math.cos(-legAngle - 0.4) * legLen),
                (int)(py-15 + Math.sin(-legAngle - 0.4) * legLen));

        g.setStroke(new BasicStroke(1));
    }

    private void drawHUD(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString(String.format("Score: %.0f", score), 30, 50);
        g.drawString(String.format("Distance: %.0fm", distance), 30, 90);

        if (!currentTrick.isEmpty() && trickTimer > 0) {
            g.setFont(new Font("SansSerif", Font.BOLD, 54));
            g.setColor(new Color(255, 255, 100, 220));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(currentTrick);
            g.drawString(currentTrick, WIDTH/2 - w/2, 180);
        }

        if (crashed) {
            g.setColor(new Color(255,60,60,180));
            g.fillRect(0, HEIGHT/2 - 80, WIDTH, 160);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 72));
            g.drawString("CRASH!", WIDTH/2 - 160, HEIGHT/2 + 20);
        }
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0,0,0,160));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 80));
        FontMetrics fm = g.getFontMetrics();
        String text = "GAME OVER";
        int w = fm.stringWidth(text);
        g.drawString(text, WIDTH/2 - w/2, HEIGHT/2 - 40);

        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        text = String.format("Final Score: %.0f", score);
        w = fm.stringWidth(text);
        g.drawString(text, WIDTH/2 - w/2, HEIGHT/2 + 40);

        g.setFont(new Font("SansSerif", Font.PLAIN, 32));
        g.drawString("Press R to restart", WIDTH/2 - 140, HEIGHT/2 + 120);
    }

    // ───────────────────────────────────────────────
    //  Input handling
    // ───────────────────────────────────────────────
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT  -> leftPressed  = true;
            case KeyEvent.VK_RIGHT -> rightPressed = true;
            case KeyEvent.VK_UP    -> upPressed    = true;
            case KeyEvent.VK_DOWN  -> downPressed  = true;
            case KeyEvent.VK_SPACE -> {
                if (onGround) {
                    upPressed = true;
                }
            }
            case KeyEvent.VK_R -> {
                if (gameOver) {
                    resetGame();
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT  -> leftPressed  = false;
            case KeyEvent.VK_RIGHT -> rightPressed = false;
            case KeyEvent.VK_UP    -> upPressed    = false;
            case KeyEvent.VK_DOWN  -> downPressed  = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void resetGame() {
        playerX = 150;
        playerY = GROUND_BASE - 60;
        velX = velY = 0;
        cameraX = 0;
        angle = targetAngle = spin = 0;
        onGround = true;
        ducking = crashed = gameOver = false;
        score = distance = trickPoints = 0;
        currentTrick = "";
        trickTimer = 0;
        particles.clear();
    }

    // ───────────────────────────────────────────────
    //  Main method
    // ───────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Skateboarding Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new SkateboardingSimulation());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}