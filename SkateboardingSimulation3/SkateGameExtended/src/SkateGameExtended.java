import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class SkateGameExtended extends JPanel implements ActionListener, KeyListener {

    // ───────────────────────────────────────────────
    //  Game Constants
    // ───────────────────────────────────────────────
    private static final int SCREEN_W = 1280;
    private static final int SCREEN_H = 720;
    private static final int GROUND_Y  = SCREEN_H - 90;

    private static final double GRAVITY       = 0.52;
    private static final double JUMP_FORCE    = -14.8;
    private static final double PUSH_FORCE    = 0.45;
    private static final double MAX_SPEED     = 8.9;
    private static final double GROUND_FRICTION = 0.978;
    private static final double AIR_FRICTION   = 0.992;

    private static final int   TARGET_FPS     = 60;
    private static final double CAMERA_SMOOTH = 0.09;

    // ───────────────────────────────────────────────
    //  Game State
    // ───────────────────────────────────────────────
    private double px = 180, py = GROUND_Y - 68;
    private double vx = 0, vy = 0;
    private double cameraX = 0;

    private boolean left, right, jump, duck, debug;
    private boolean onGround = true;
    private boolean crashed = false;
    private boolean gameActive = true;

    private double score = 0;
    private double distance = 0;
    private int    combo = 1;
    private int    highScore = 0;

    private String trickName = "";
    private int    trickShowMs = 0;

    private double boardAngle = 0;
    private double boardSpin = 0;
    private double targetBoardAngle = 0;

    private final ArrayList<Particle> particles = new ArrayList<>();
    private final ArrayList<TerrainChunk> terrain = new ArrayList<>();
    private final Random rnd = new Random(420691);

    private Timer gameTimer;
    private long prevNano;

    // ───────────────────────────────────────────────
    //  Inner Classes
    // ───────────────────────────────────────────────
    static class Particle {
        double x,y,vx,vy;
        int life, maxLife;
        Color col;
        Particle(double x, double y, double vx, double vy, int life, Color c) {
            this.x=x; this.y=y; this.vx=vx; this.vy=vy;
            this.life = this.maxLife = life; this.col = c;
        }
        void tick() { x+=vx; y+=vy; vy+=0.22; life--; }
        void render(Graphics2D g) {
            float a = (float)life / maxLife;
            g.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(), (int)(a*255)));
            g.fillOval((int)(x-3.5), (int)(y-3.5), 7,7);
        }
    }

    static class TerrainChunk {
        double x1,y1,x2,y2;
        boolean gap;
        String type; // "flat", "upRamp", "downRamp", "rail"
        TerrainChunk(double a,double b,double c,double d, boolean g, String t) {
            x1=a; y1=b; x2=c; y2=d; gap=g; type=t;
        }
        double yAt(double worldX) {
            if (Math.abs(x2-x1) < 1e-4) return y1;
            double t = (worldX - x1) / (x2 - x1);
            return y1 + t * (y2 - y1);
        }
    }

    // ───────────────────────────────────────────────
    //  Constructor & Setup
    // ───────────────────────────────────────────────
    public SkateGameExtended() {
        setPreferredSize(new Dimension(SCREEN_W, SCREEN_H));
        setBackground(new Color(110, 180, 240));
        setFocusable(true);
        addKeyListener(this);

        generateLongLevel();

        gameTimer = new Timer(1000 / TARGET_FPS, this);
        prevNano = System.nanoTime();
        gameTimer.start();
    }

    private void generateLongLevel() {
        terrain.clear();
        double cx = -600;
        double cy = GROUND_Y;

        terrain.add(new TerrainChunk(cx-1200, cy+180, cx, cy, false, "flat"));

        while (cx < 22000) {
            double segLen   = 90 + rnd.nextDouble()*280;
            double heightΔ  = -48 + rnd.nextDouble()*96;
            String segType  = "flat";

            if (rnd.nextDouble() < 0.18) { // bigger ramp/jump
                segLen = 220 + rnd.nextDouble()*420;
                heightΔ = -110 + rnd.nextDouble()*220;
                segType = heightΔ > 0 ? "upRamp" : "downRamp";
            }

            if (rnd.nextDouble() < 0.07 && segLen > 180) {
                segType = "rail";
            }

            double nx = cx + segLen;
            double ny = cy + heightΔ;

            if (rnd.nextDouble() < 0.14) ny = cy; // flat section

            boolean isGap = rnd.nextDouble() < 0.11 && segLen > 160;

            terrain.add(new TerrainChunk(cx, cy, nx, ny, isGap, segType));

            cx = nx;
            cy = ny;
        }
    }

    // ───────────────────────────────────────────────
    //  Main Update Loop
    // ───────────────────────────────────────────────
    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = (now - prevNano) / 1_000_000_000.0;
        prevNano = now;
        if (dt > 0.12) dt = 0.12;

        if (gameActive && !crashed) {
            updateInput(dt);
            applyPhysics(dt);
            detectCollision();
            updateTricks();
            updateCamera();
            updateParticles();
            updateTrickDisplay();
        }

        repaint();
    }

    private void updateInput(double dt) {
        double push = 0;
        if (left)  push -= 1.15;
        if (right) push += 1.15;

        if (onGround) {
            vx += push * 48 * dt;
            vx *= GROUND_FRICTION;
            if (Math.abs(vx) > MAX_SPEED) vx = Math.signum(vx) * MAX_SPEED;
        } else {
            vx += push * 11 * dt;
            vx *= AIR_FRICTION;
        }

        if (jump && onGround && !duck) {
            vy = JUMP_FORCE;
            onGround = false;
            boardSpin = 0;
            targetBoardAngle = 0;
            // future: play jump sound
        }

        duck = duck; // crouch lowers center → used in drawing
    }

    private void applyPhysics(double dt) {
        vy += GRAVITY * 62 * dt;

        px += vx * 62 * dt;
        py += vy * 62 * dt;

        distance = Math.max(distance, px - 180);
        score = distance * 0.8 + combo * 120;

        boardAngle = lerpAngle(boardAngle, targetBoardAngle, 0.28);
        boardSpin *= 0.94;
    }

    private void detectCollision() {
        TerrainChunk best = null;
        double bestDy = 1e9;

        for (TerrainChunk c : terrain) {
            if (px < c.x1 - 80 || px > c.x2 + 80) continue;
            double ty = c.yAt(px);
            double dy = ty - py;
            if (dy > -120 && dy < 140) {
                if (Math.abs(dy) < Math.abs(bestDy)) {
                    bestDy = dy;
                    best = c;
                }
            }
        }

        if (best != null) {
            double ty = best.yAt(px);

            if (py + 42 >= ty && vy >= -0.8) {
                py = ty - 42;
                vy = 0;
                onGround = true;

                // landing angle penalty
                double absAng = Math.abs(boardAngle);
                if (absAng > 0.7) {
                    if (absAng > 1.4 || Math.abs(boardSpin) > 4.5) {
                        startCrash();
                    } else {
                        targetBoardAngle = 0;
                        boardSpin *= 0.3;
                    }
                } else {
                    targetBoardAngle = 0;
                }

                // hard landing bounce
                if (vy > 14) {
                    vy = -vy * 0.32;
                    onGround = false;
                }
            }
        } else {
            onGround = false;
        }

        if (py > SCREEN_H + 300) startCrash();
    }

    private void startCrash() {
        crashed = true;
        gameActive = false;
        vx *= 0.35;
        vy = -8 + Math.random()*-5;

        // ragdoll particles
        for (int i = 0; i < 68; i++) {
            double ang = Math.random()*Math.PI*2;
            double spd = 1.8 + Math.random()*9;
            Color c = Math.random()<0.55 ? new Color(220,50,30) : new Color(255,180,60);
            particles.add(new Particle(
                    px + rnd.nextGaussian()*18,
                    py + rnd.nextGaussian()*32,
                    Math.cos(ang)*spd + vx*0.7,
                    Math.sin(ang)*spd + vy*0.6 - 5,
                    40 + rnd.nextInt(55), c));
        }
        // future: crash sound
    }

    private void updateCamera() {
        double ideal = px - SCREEN_W * 0.38;
        cameraX += (ideal - cameraX) * CAMERA_SMOOTH;
    }

    private void updateTricks() {
        if (trickShowMs > 0) trickShowMs -= 16;

        if (onGround) {
            if (Math.abs(boardSpin) > 7) {
                if (Math.abs(boardSpin) > 14) {
                    showTrick("360 Flip!", 1800, 650);
                } else {
                    showTrick("Kickflip", 1200, 320);
                }
                combo++;
                boardSpin = 0;
            } else if (Math.abs(boardAngle) > 1.6 && Math.abs(vx) > 4) {
                showTrick("180", 900, 220);
                combo++;
            }
        } else if (vy < -3 && trickShowMs == 0) {
            if (rnd.nextDouble() < 0.42) {
                showTrick("Ollie", 1100, 140);
                combo = Math.max(combo, 2);
            }
        }
    }

    private void showTrick(String name, int ms, int points) {
        trickName = name;
        trickShowMs = ms;
        score += points * combo;
    }

    private void updateParticles() {
        particles.removeIf(p -> p.life <= 0);
        particles.forEach(Particle::tick);
    }

    private void updateTrickDisplay() {
        // fade logic handled in rendering
    }

    private double lerpAngle(double a, double b, double t) {
        double d = b - a;
        while (d > Math.PI)  d -= 2*Math.PI;
        while (d < -Math.PI) d += 2*Math.PI;
        return a + d*t;
    }

    // ───────────────────────────────────────────────
    //  Rendering
    // ───────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawParallaxBackground(g2);
        g2.translate(-cameraX, 0);

        drawTerrain(g2);
        drawPlayerAndBoard(g2);
        drawParticles(g2);

        g2.translate(cameraX, 0);
        drawUI(g2);

        if (!gameActive) drawGameOverScreen(g2);
    }

    private void drawParallaxBackground(Graphics2D g) {
        // distant sky gradient
        GradientPaint sky = new GradientPaint(0,0,new Color(80,140,220),
                0,SCREEN_H,new Color(160,210,255));
        g.setPaint(sky);
        g.fillRect(0,0,SCREEN_W,SCREEN_H);

        // far hills
        g.setColor(new Color(100,140,100,160));
        fillHill(g, 0.12, 180, 320, 420);
        fillHill(g, 0.18, 680, 240, 380);

        // mid trees (simple)
        g.setColor(new Color(40,100,40,200));
        drawSimpleTree(g, 400 + cameraX*0.35,  GROUND_Y-80, 110);
        drawSimpleTree(g, 1400 + cameraX*0.42, GROUND_Y-120, 140);
        drawSimpleTree(g, 2200 + cameraX*0.38, GROUND_Y-60,  90);
    }

    private void fillHill(Graphics2D g, double parallax, int baseX, int w, int h) {
        int x = (int)(baseX + cameraX * parallax);
        Path2D p = new Path2D.Double();
        p.moveTo(x-w, SCREEN_H);
        p.lineTo(x-w, GROUND_Y - h*0.7);
        p.quadTo(x, GROUND_Y - h, x+w, GROUND_Y - h*0.6);
        p.lineTo(x+w, SCREEN_H);
        p.closePath();
        g.fill(p);
    }

    private void drawSimpleTree(Graphics2D g, double wx, double baseY, double size) {
        int x = (int)wx;
        int y = (int)baseY;
        g.fillRect(x-12, y-80, 24, 80); // trunk
        g.setColor(new Color(20,140,20));
        g.fillOval(x-50, y-160, 100, 100);
        g.fillOval(x-70, y-130, 140, 90);
    }

    private void drawTerrain(Graphics2D g) {
        Path2D ground = new Path2D.Double();
        boolean started = false;

        for (TerrainChunk c : terrain) {
            if (c.x2 < cameraX - 300 || c.x1 > cameraX + SCREEN_W + 300) continue;

            if (!started) {
                ground.moveTo(c.x1, c.y1 + 400);
                started = true;
            }

            if (c.gap) {
                ground.lineTo(c.x1, c.y1 + 400);
                ground.lineTo(c.x2, c.y2 + 400);
            } else {
                ground.lineTo(c.x1, c.y1);
                ground.lineTo(c.x2, c.y2);
            }
        }

        if (started) {
            Rectangle2D b = ground.getBounds2D();
            ground.lineTo(b.getMaxX(), SCREEN_H + 200);
            ground.lineTo(b.getMinX() - 400, SCREEN_H + 200);
            ground.closePath();

            g.setColor(new Color(48, 110, 38));
            g.fill(ground);

            g.setColor(new Color(70, 160, 55));
            g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(ground);
            g.setStroke(new BasicStroke(1));
        }
    }

    private void drawPlayerAndBoard(Graphics2D g) {
        double bx = px;
        double by = py + (duck ? 8 : 0);

        // Board
        AffineTransform tx = g.getTransform();
        g.translate(bx, by + 10);
        g.rotate(boardAngle + boardSpin * 0.08);

        // deck
        g.setColor(new Color(20,20,100));
        g.fillRoundRect(-42,-8,84,16,14,14);
        // grip
        g.setColor(new Color(55,55,55,200));
        g.fillRoundRect(-40,-6,80,10,10,10);
        // wheels
        g.setColor(Color.BLACK);
        g.fillOval(-32,-14,20,20);
        g.fillOval( 12,-14,20,20);
        g.setColor(Color.WHITE);
        g.fillOval(-27,-9,10,10);
        g.fillOval( 17,-9,10,10);

        g.setTransform(tx);

        // ─── Skater ───────────────────────────────────────
        g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        double lean = duck ? -0.55 : Math.sin(px * 0.05) * 0.48;
        double armSwing = Math.sin(px * 0.08 + 1) * 0.9;

        // torso
        int torsoY = (int)(py - 22 - (duck?18:0));
        g.setColor(new Color(220,40,40)); // shirt
        g.fillRect((int)px-14, torsoY-28, 28, 36);

        // head + helmet
        g.setColor(new Color(240,210,160));
        g.fillOval((int)px-16, (int)py-78, 32, 32);
        g.setColor(new Color(255,80,20));
        g.fillArc((int)px-16, (int)py-78, 32, 32, 0, 180); // helmet
        g.setColor(Color.BLACK);
        g.fillOval((int)px-5, (int)py-66, 6, 10); // eye

        // arms
        g.setColor(new Color(220,40,40));
        int al = 32;
        g.drawLine((int)px, torsoY-10,
                (int)(px + Math.cos(armSwing+1.6)*al),
                (int)(torsoY-10 + Math.sin(armSwing+1.6)*al));
        g.drawLine((int)px, torsoY-10,
                (int)(px + Math.cos(-armSwing-0.3)*al),
                (int)(torsoY-10 + Math.sin(-armSwing-0.3)*al));

        // legs
        g.setColor(new Color(30,30,120)); // pants
        int ll = duck ? 22 : 38;
        double legA = onGround ? lean : -0.9 + vy*0.08;
        g.drawLine((int)px, torsoY+8,
                (int)(px + Math.cos(legA+0.8)*ll),
                (int)(torsoY+8 + Math.sin(legA+0.8)*ll));
        g.drawLine((int)px, torsoY+8,
                (int)(px + Math.cos(-legA-0.5)*ll),
                (int)(torsoY+8 + Math.sin(-legA-0.5)*ll));

        g.setStroke(new BasicStroke(1));
    }

    private void drawParticles(Graphics2D g) {
        for (Particle p : particles) p.render(g);
    }

    private void drawUI(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.drawString(String.format("Score: %,d", (int)score), 30, 55);
        g.drawString(String.format("Dist: %.0fm", distance), 30, 100);
        g.drawString(String.format("×%d", combo), 30, 145);

        // speedometer
        double speed = Math.abs(vx);
        g.setColor(new Color(255,255,255,180));
        g.fillRoundRect(SCREEN_W-180, 30, 150, 60, 16,16);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.drawString(String.format("%.1f m/s", speed*3.6), SCREEN_W-165, 70);

        // trick popup
        if (trickShowMs > 0) {
            float alpha = Math.min(1f, trickShowMs / 800f);
            g.setColor(new Color(255,255,80,(int)(alpha*240)));
            g.setFont(new Font("Arial", Font.BOLD, 64));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(trickName);
            g.drawString(trickName, SCREEN_W/2 - tw/2, 220);
        }

        if (debug) {
            g.setFont(new Font("Monospaced", Font.PLAIN, 18));
            g.setColor(new Color(255,255,200,220));
            g.drawString(String.format("px=%.1f py=%.1f vx=%.2f vy=%.2f cam=%.1f", px,py,vx,vy,cameraX), 30, SCREEN_H-40);
            g.drawString("onGround="+onGround+" duck="+duck, 30, SCREEN_H-20);
        }
    }

    private void drawGameOverScreen(Graphics2D g) {
        g.setColor(new Color(0,0,0,180));
        g.fillRect(0,0,SCREEN_W,SCREEN_H);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 90));
        FontMetrics fm = g.getFontMetrics();
        String txt = "GAME OVER";
        g.drawString(txt, SCREEN_W/2 - fm.stringWidth(txt)/2, SCREEN_H/2 - 80);

        g.setFont(new Font("Arial", Font.BOLD, 52));
        txt = String.format("Score: %,d", (int)score);
        g.drawString(txt, SCREEN_W/2 - fm.stringWidth(txt)/2, SCREEN_H/2 + 10);

        if ((int)score > highScore) {
            highScore = (int)score;
            g.setColor(new Color(255,220,60));
            g.setFont(new Font("Arial", Font.BOLD, 42));
            txt = "NEW HIGH SCORE!";
            g.drawString(txt, SCREEN_W/2 - fm.stringWidth(txt)/2, SCREEN_H/2 + 80);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 36));
        g.drawString("Press R to restart", SCREEN_W/2 - 160, SCREEN_H/2 + 160);
    }

    // ───────────────────────────────────────────────
    //  Controls
    // ───────────────────────────────────────────────
    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT  -> left  = true;
            case KeyEvent.VK_RIGHT -> right = true;
            case KeyEvent.VK_UP, KeyEvent.VK_SPACE -> jump = true;
            case KeyEvent.VK_DOWN  -> duck  = true;
            case KeyEvent.VK_D     -> debug = !debug;
            case KeyEvent.VK_R     -> {
                if (!gameActive) reset();
            }
        }
    }

    @Override public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT  -> left  = false;
            case KeyEvent.VK_RIGHT -> right = false;
            case KeyEvent.VK_UP, KeyEvent.VK_SPACE -> jump = false;
            case KeyEvent.VK_DOWN  -> duck  = false;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}

    private void reset() {
        px = 180; py = GROUND_Y - 68;
        vx = vy = 0;
        cameraX = 0;
        boardAngle = boardSpin = targetBoardAngle = 0;
        onGround = true; crashed = false; gameActive = true;
        score = distance = 0; combo = 1;
        trickName = ""; trickShowMs = 0;
        particles.clear();
        // terrain regenerated for variety
        generateLongLevel();
    }

    // ───────────────────────────────────────────────
    //  Entry Point
    // ───────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Skate Game – Extended");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new SkateGameExtended());
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}