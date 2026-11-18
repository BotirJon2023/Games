// DemolitionRacingGame2.java  — pure Java, no external libs
// Compile: javac DemolitionRacingGame2.java
// Run:     java DemolitionRacingGame2
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class DemolitionRacingGame2 extends JPanel implements ActionListener, KeyListener {

    static final int W = 1280, H = 720;
    static final double FRICTION = 0.92, TURN = 0.055, ACCEL = 0.32;

    // ---- Game objects ----
    static class Car {
        double x, y, angle, vx, vy, speed, health = 100;
        boolean alive = true, isAI;
        Color body, trim;
        double drawX, drawY, drawAngle;      // visual smoothing
        double hitFlash = 0;                  // red flash timer
        double trailPhase = 0;
        double aiTimer = 0;
        String name;

        Car(double x, double y, Color body, Color trim, boolean ai, String name) {
            this.x = drawX = x; this.y = drawY = y;
            this.angle = drawAngle = Math.random() * Math.PI * 2;
            this.body = body; this.trim = trim; this.isAI = ai; this.name = name;
        }

        void update(double dt) {
            if (!alive) return;
            // smooth interpolation (ease-out) → fluid motion
            drawX     += (x - drawX) * 0.35;
            drawY     += (y - drawY) * 0.35;
            drawAngle += normAngle(angle - drawAngle) * 0.35;
            drawAngle  = normAngle(drawAngle);

            x += vx * dt; y += vy * dt;
            vx *= FRICTION; vy *= FRICTION;
            speed = Math.hypot(vx, vy);
            hitFlash = Math.max(0, hitFlash - dt * 4);
            trailPhase += speed * dt * 6;
        }

        void accelerate(double a) {
            vx += Math.cos(angle) * a;
            vy += Math.sin(angle) * a;
            double max = 9.0, s = Math.hypot(vx, vy);
            if (s > max) { vx = vx / s * max; vy = vy / s * max; }
        }
        void turn(double d) { angle = normAngle(angle + d * (0.35 + speed * 0.09)); }
    }

    static class Particle {
        double x,y,vx,vy,life,maxLife,size; Color c;
        Particle(double x,double y,double vx,double vy,double life,Color c,double size){
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=life;this.maxLife=life;this.c=c;this.size=size;
        }
        void update(double dt){ x+=vx*dt; y+=vy*dt; vx*=0.94; vy*=0.94; life-=dt; }
    }

    static class SkidMark { double x,y,angle,life=6; SkidMark(double x,double y,double a){this.x=x;this.y=y;this.angle=a;} }

    static class Lantern {
        double x, y, r, phase, speed;
        Lantern(double x,double y,double r){this.x=x;this.y=y;this.r=r;
            this.phase=Math.random()*Math.PI*2; this.speed=0.6+Math.random()*0.8;}
    }

    // ---- State ----
    final List<Car> cars = new ArrayList<>();
    final List<Particle> particles = new ArrayList<>();
    final List<SkidMark> skids = new ArrayList<>();
    final List<Lantern> lanterns = new ArrayList<>();
    final Set<Integer> keys = new HashSet<>();
    Car p1, p2;
    double camShake = 0, time = 0;
    boolean gameOver = false;
    String winnerText = "";
    Font fBig, fMed, fSmall;

    // Backgrounds (cached for performance)
    BufferedImage groundCache;
    Random rng = new Random(20241118);

    public DemolitionRacingGame2() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        fBig   = new Font("Serif", Font.BOLD, 68);
        fMed   = new Font("Serif", Font.BOLD, 20);
        fSmall = new Font("SansSerif", Font.BOLD, 14);

        initGame();

        // Chinese-themed lanterns around the arena
        for (int i = 0; i < 14; i++) {
            double x = 60 + rng.nextDouble() * (W - 120);
            double y = (i % 2 == 0) ? 55 : H - 55;
            lanterns.add(new Lantern(x, y, 16 + rng.nextDouble() * 8));
        }

        new javax.swing.Timer(16, this).start(); // ~60 FPS
    }

    void initGame() {
        cars.clear(); particles.clear(); skids.clear();
        gameOver = false; winnerText = ""; camShake = 0;

        // Player 1 — 赤 (red)
        p1 = new Car(400, 400, new Color(200,30,30), new Color(255,200,90), false, "P1");
        // Player 2 / AI — 青 (blue)   → set isAI = false for 2-player
        p2 = new Car(880, 400, new Color(30,110,200), new Color(255,220,120), true, "CPU");

        cars.add(p1); cars.add(p2);

        // Additional AI opponents with Chinese color palette
        Color[] pal = { new Color(230,180,40), new Color(140,40,160),
                new Color(40,160,120), new Color(230,110,60) };
        Color[] trims = { new Color(255,240,200), new Color(255,220,180),
                new Color(255,255,220), new Color(255,230,200) };
        for (int i = 0; i < 3; i++) {
            cars.add(new Car(200 + rng.nextDouble()*880, 150 + rng.nextDouble()*420,
                    pal[i % pal.length], trims[i % trims.length], true, "CPU"+(i+1)));
        }
    }

    // ================= UPDATE =================
    @Override public void actionPerformed(ActionEvent e) { // Swing timer
        update(1/60.0);
        repaint();
    }

    void update(double dt) {
        time += dt;
        if (gameOver) return;

        // --- Player 1 controls ---
        if (p1.alive) {
            if (keys.contains(KeyEvent.VK_W)) p1.accelerate(ACCEL);
            if (keys.contains(KeyEvent.VK_S)) p1.accelerate(-ACCEL * 0.7);
            if (keys.contains(KeyEvent.VK_A)) p1.turn(-TURN);
            if (keys.contains(KeyEvent.VK_D)) p1.turn(TURN);
            if (p1.speed > 3 && Math.random() < 0.55)
                skids.add(new SkidMark(p1.x - Math.cos(p1.angle)*20,
                        p1.y - Math.sin(p1.angle)*20, p1.angle));
        }

        // --- Player 2 / AI ---
        if (p2.alive) {
            if (p2.isAI) driveAI(p2, dt);
            else {
                if (keys.contains(KeyEvent.VK_UP))    p2.accelerate(ACCEL);
                if (keys.contains(KeyEvent.VK_DOWN))  p2.accelerate(-ACCEL*0.7);
                if (keys.contains(KeyEvent.VK_LEFT))  p2.turn(-TURN);
                if (keys.contains(KeyEvent.VK_RIGHT)) p2.turn(TURN);
            }
        }

        for (Car c : cars) {
            if (c.isAI && c != p2 && c.alive) driveAI(c, dt);
            c.update(dt);
            // arena walls
            if (c.x < 30)        { c.x = 30;      c.vx = Math.abs(c.vx)*0.5; camShake = 8; }
            if (c.x > W-30)      { c.x = W-30;    c.vx = -Math.abs(c.vx)*0.5; camShake = 8; }
            if (c.y < 30)        { c.y = 30;      c.vy = Math.abs(c.vy)*0.5; camShake = 8; }
            if (c.y > H-30)      { c.y = H-30;    c.vy = -Math.abs(c.vy)*0.5; camShake = 8; }
        }

        // --- Collisions ---
        for (int i = 0; i < cars.size(); i++) {
            for (int j = i+1; j < cars.size(); j++) {
                Car a = cars.get(i), b = cars.get(j);
                if (!a.alive || !b.alive) continue;
                double dx = b.x - a.x, dy = b.y - a.y;
                double dist = Math.hypot(dx, dy), minDist = 42;
                if (dist < minDist && dist > 0.01) {
                    double nx = dx/dist, ny = dy/dist, overlap = minDist - dist;
                    a.x -= nx*overlap/2; a.y -= ny*overlap/2;
                    b.x += nx*overlap/2; b.y += ny*overlap/2;
                    double rvx = b.vx - a.vx, rvy = b.vy - a.vy;
                    double rel = rvx*nx + rvy*ny;
                    if (rel < 0) {
                        double imp = -rel * 0.9;
                        a.vx -= nx*imp; a.vy -= ny*imp;
                        b.vx += nx*imp; b.vy += ny*imp;
                        double dmg = Math.abs(imp) * 12;
                        a.health -= dmg; b.health -= dmg;
                        a.hitFlash = 1; b.hitFlash = 1;
                        camShake = Math.min(16, camShake + dmg * 0.5);
                        spawnSparks((a.x+b.x)/2, (a.y+b.y)/2, imp);
                        if (a.health <= 0) { a.alive = false; explode(a.x, a.y); }
                        if (b.health <= 0) { b.alive = false; explode(b.x, b.y); }
                    }
                }
            }
        }

        // particles
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) { Particle p = it.next(); p.update(dt); if (p.life <= 0) it.remove(); }
        Iterator<SkidMark> sit = skids.iterator();
        while (sit.hasNext()) { SkidMark s = sit.next(); s.life -= dt; if (s.life <= 0) sit.remove(); }

        camShake *= 0.85;

        long alive = cars.stream().filter(c -> c.alive).count();
        if (alive <= 1) {
            gameOver = true;
            Car w = cars.stream().filter(c -> c.alive).findFirst().orElse(null);
            winnerText = w == null ? "全毁 — TOTAL DEMOLITION!"
                    : w == p1 ? "玩家一胜利 — PLAYER 1 WINS!"
                    : w == p2 ? (p2.isAI ? "电脑胜利 — CPU WINS!" : "玩家二胜利 — PLAYER 2 WINS!")
                    : "对手胜利 — AI WINS!";
        }
    }

    void driveAI(Car c, double dt) {
        Car target = null; double best = 1e9;
        for (Car o : cars) {
            if (o == c || !o.alive) continue;
            double d = Math.hypot(o.x-c.x, o.y-c.y);
            if (d < best) { best = d; target = o; }
        }
        if (target != null) {
            double desired = Math.atan2(target.y-c.y, target.x-c.x);
            double diff = normAngle(desired - c.angle);
            c.turn(Math.signum(diff) * 0.8);
            if (c.speed < 6) c.accelerate(ACCEL * 0.9);
        }
    }

    void spawnSparks(double x, double y, double power) {
        int n = (int)(6 + power * 4);
        for (int i = 0; i < n; i++) {
            double a = Math.random()*Math.PI*2, sp = 60 + Math.random()*180;
            particles.add(new Particle(x, y, Math.cos(a)*sp, Math.sin(a)*sp,
                    0.4 + Math.random()*0.5,
                    Math.random() < 0.5 ? Color.ORANGE : Color.YELLOW,
                    2 + Math.random()*2));
        }
    }

    void explode(double x, double y) {
        for (int i = 0; i < 140; i++) {
            double a = Math.random()*Math.PI*2, sp = 40 + Math.random()*380;
            Color col = Math.random()<0.5 ? Color.ORANGE
                    : Math.random()<0.5 ? Color.YELLOW : Color.DARK_GRAY;
            particles.add(new Particle(x, y, Math.cos(a)*sp, Math.sin(a)*sp,
                    0.6 + Math.random()*1.2, col, 3 + Math.random()*4));
        }
        camShake = 22;
    }

    // ================= RENDER =================
    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        double shX = (Math.random()-0.5) * camShake;
        double shY = (Math.random()-0.5) * camShake;

        // --- Background (Chinese courtyard theme) ---
        drawBackground(g);

        g.translate(shX, shY);

        // Arena border with glow (like a racing ring)
        drawArenaBorder(g);

        // Skid marks
        for (SkidMark s : skids) {
            float a = (float) Math.min(0.5, s.life/6 * 0.5);
            g.setColor(new Color(0,0,0,a));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(s.x, s.y,
                    s.x - Math.cos(s.angle)*10, s.y - Math.sin(s.angle)*10));
        }

        // Motion trail behind moving cars (motion blur)
        for (Car c : cars) {
            if (!c.alive || c.speed < 2) continue;
            drawMotionTrail(g, c);
        }

        // Cars (with shadow + glow + hit flash)
        for (Car c : cars) drawCar(g, c);

        // Particles
        for (Particle p : particles) {
            double a = Math.max(0, p.life / p.maxLife);
            int alpha = (int)(a * 255);
            g.setColor(new Color(p.c.getRed(), p.c.getGreen(), p.c.getBlue(), alpha));
            g.fill(new Ellipse2D.Double(p.x - p.size/2, p.y - p.size/2, p.size, p.size));
        }

        g.translate(-shX, -shY);

        // HUD
        drawHUD(g);

        if (gameOver) drawGameOver(g);
    }

    // ---------- Background with lanterns ----------
    void drawBackground(Graphics2D g) {
        // Ground gradient — dark stone with warm tint (Chinese night market vibe)
        GradientPaint gp = new GradientPaint(0, 0, new Color(30, 25, 22),
                0, H, new Color(18, 14, 14));
        g.setPaint(gp);
        g.fillRect(0, 0, W, H);

        // Stone-tile grid
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(255, 210, 160, 15));
        for (int x = 0; x < W; x += 64) g.drawLine(x, 0, x, H);
        for (int y = 0; y < H; y += 64) g.drawLine(0, y, W, y);

        // Glowing lanterns at top/bottom
        for (Lantern L : lanterns) {
            double pulse = 0.6 + 0.4 * Math.sin(time * L.speed + L.phase);
            // radial glow
            for (int i = 4; i >= 1; i--) {
                double rr = L.r * (i * 1.6) * pulse;
                int alpha = (int)(18 * pulse / i);
                g.setColor(new Color(255, 80, 60, alpha));
                g.fill(new Ellipse2D.Double(L.x - rr, L.y - rr, rr*2, rr*2));
            }
            // lantern body
            g.setColor(new Color(230, 40, 30));
            g.fill(new Ellipse2D.Double(L.x - L.r, L.y - L.r*0.85, L.r*2, L.r*1.7));
            // gold cap
            g.setColor(new Color(220, 170, 60));
            g.fillRect((int)(L.x - L.r*0.55), (int)(L.y - L.r*0.95), (int)(L.r*1.1), 4);
            g.fillRect((int)(L.x - L.r*0.55), (int)(L.y + L.r*0.85), (int)(L.r*1.1), 4);
            // tassel
            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(220, 60, 40));
            g.drawLine((int)L.x, (int)(L.y + L.r*0.95), (int)L.x, (int)(L.y + L.r*1.6));
        }
    }

    void drawArenaBorder(Graphics2D g) {
        // Double gold border with soft glow
        g.setColor(new Color(255, 200, 100, 70));
        g.setStroke(new BasicStroke(10f));
        g.draw(new RoundRectangle2D.Double(12, 12, W-24, H-24, 32, 32));
        g.setColor(new Color(220, 170, 80));
        g.setStroke(new BasicStroke(3f));
        g.draw(new RoundRectangle2D.Double(18, 18, W-36, H-36, 28, 28));

        // corner Chinese-style motifs (simple clouds pattern)
        g.setColor(new Color(255, 200, 100, 120));
        g.setStroke(new BasicStroke(2f));
        for (int cx : new int[]{60, W-60}) {
            for (int cy : new int[]{60, H-60}) {
                for (int i = 0; i < 3; i++) {
                    g.drawArc(cx - 20 - i*8, cy - 20 - i*8, 40 + i*16, 40 + i*16, 45, 180);
                }
            }
        }
    }

    // ---------- Car rendering ----------
    void drawMotionTrail(Graphics2D g, Car c) {
        double dirx = Math.cos(c.drawAngle), diry = Math.sin(c.drawAngle);
        int steps = 6;
        for (int i = 1; i <= steps; i++) {
            double px = c.drawX - dirx * (i * 6 * (c.speed / 6));
            double py = c.drawY - diry * (i * 6 * (c.speed / 6));
            float alpha = 0.18f * (1 - (float)i / steps);
            g.setColor(new Color(c.body.getRed(), c.body.getGreen(), c.body.getBlue(),
                    (int)(alpha*255)));
            g.fill(new Ellipse2D.Double(px - 20, py - 11, 40, 22));
        }
    }

    void drawCar(Graphics2D g, Car c) {
        if (!c.alive) return;

        // Ground shadow (soft)
        g.setColor(new Color(0,0,0,90));
        g.fill(new Ellipse2D.Double(c.drawX - 26 + 3, c.drawY - 14 + 4, 54, 30));

        AffineTransform old = g.getTransform();
        g.translate(c.drawX, c.drawY);
        g.rotate(c.drawAngle);

        // Body with gradient for 3D sheen
        GradientPaint bodyGrad = new GradientPaint(-26, -13, c.body.brighter(),
                -26, 13, c.body.darker());
        g.setPaint(bodyGrad);
        g.fill(new RoundRectangle2D.Double(-26, -13, 52, 26, 10, 10));

        // Gold trim (Chinese decorative accent)
        g.setColor(c.trim);
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Double(-24, -11, 48, 22, 8, 8));
        g.draw(new RoundRectangle2D.Double(-26, -13, 52, 26, 10, 10));

        // Roof sheen highlight
        g.setPaint(new GradientPaint(-10, -10, new Color(255,255,255,120),
                10, 10, new Color(255,255,255,0)));
        g.fill(new RoundRectangle2D.Double(-18, -8, 30, 16, 6, 6));

        // Windshield
        g.setColor(new Color(180, 220, 255, 200));
        g.fill(new RoundRectangle2D.Double(6, -9, 12, 18, 4, 4));

        // Headlights
        g.setColor(Color.YELLOW);
        g.fill(new Ellipse2D.Double(-24, -11, 6, 6));
        g.fill(new Ellipse2D.Double(-24, 5, 6, 6));

        // Tail lights (red)
        g.setColor(new Color(255, 60, 60));
        g.fill(new Ellipse2D.Double(20, -10, 4, 4));
        g.fill(new Ellipse2D.Double(20, 6, 4, 4));

        // Hit flash overlay
        if (c.hitFlash > 0) {
            int alpha = (int)(c.hitFlash * 180);
            g.setColor(new Color(255, 80, 80, alpha));
            g.fill(new RoundRectangle2D.Double(-26, -13, 52, 26, 10, 10));
        }

        g.setTransform(old);
    }

    // ---------- HUD ----------
    void drawHUD(Graphics2D g) {
        // Semi-transparent top bars
        g.setColor(new Color(20, 15, 10, 160));
        g.fillRoundRect(20, 20, 320, 56, 14, 14);
        g.fillRoundRect(W-340, 20, 320, 56, 14, 14);

        drawHealthBar(g, 30, 28, p1, "玩家一  P1", new Color(200,30,30));
        drawHealthBar(g, W-330, 28, p2,
                p2.isAI ? "电脑  CPU" : "玩家二  P2", new Color(30,110,200));

        // Controls hint
        g.setFont(fSmall);
        g.setColor(new Color(255,220,180,180));
        g.drawString("P1: WASD    P2: Arrows    R: Restart", 30, H - 20);
    }

    void drawHealthBar(Graphics2D g, int x, int y, Car c, String label, Color c1) {
        g.setFont(fMed);
        g.setColor(new Color(255,240,220));
        g.drawString(label, x, y + 14);

        g.setColor(new Color(0,0,0,120));
        g.fillRoundRect(x, y + 20, 290, 14, 6, 6);

        double w = Math.max(0, c.health / 100.0) * 286;
        Color bar = c.health > 50 ? new Color(80, 220, 90)
                : c.health > 25 ? new Color(255, 180, 40)
                : new Color(220, 60, 60);
        g.setColor(bar);
        g.fillRoundRect(x + 2, y + 22, (int)w, 10, 4, 4);
    }

    void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0,0,0,190));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(255, 200, 100));
        g.setFont(fBig);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(winnerText, (W - fm.stringWidth(winnerText))/2, H/2);
        g.setFont(fMed);
        String sub = "按 R 重新开始  —  Press R to restart";
        fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(sub, (W - fm.stringWidth(sub))/2, H/2 + 60);
    }

    // ---------- Input ----------
    @Override public void keyPressed(KeyEvent e) {
        keys.add(e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_R) initGame();
        // Toggle P2 human/AI with SPACE
        if (e.getKeyCode() == KeyEvent.VK_SPACE && !p2.isAI) p2.isAI = true;
    }
    @Override public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
    @Override public void keyTyped(KeyEvent e) {}

    // ---------- Utils ----------
    static double normAngle(double a) {
        while (a > Math.PI) a -= Math.PI * 2;
        while (a < -Math.PI) a += Math.PI * 2;
        return a;
    }

    // ---------- Main ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("拆迁赛车 Demolition Racing 2 — China Edition");
            DemolitionRacingGame2 panel = new DemolitionRacingGame2();
            f.add(panel);
            f.pack();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setLocationRelativeTo(null);
            f.setResizable(false);
            f.setVisible(true);
            panel.requestFocusInWindow();
        });
    }
}