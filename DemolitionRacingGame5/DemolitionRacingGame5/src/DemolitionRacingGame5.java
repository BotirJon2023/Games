// DemolitionRacingGame5.java  — pure Java, no external libraries
// Rome Edition: Colosseum & St. Peter's at sunset, swaying umbrella pines and swallows
// Compile: javac DemolitionRacingGame5.java
// Run:     java DemolitionRacingGame5
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class DemolitionRacingGame5 extends JPanel implements ActionListener, KeyListener {

    static final int W = 1280, H = 720;
    static final int HZ = 150;                 // horizon: city skyline above, street below
    static final double FRICTION = 0.92, TURN = 0.055, ACCEL = 0.32;
    static final double LAMP_DIM = 0.6;          // street lamp glow strength (day < night)

    // ================= ENTITIES =================
    static class Car {
        double x, y, angle, vx, vy, speed, health = 100;
        boolean alive = true, isAI;
        Color body, trim;
        double drawX, drawY, drawAngle;
        double hitFlash = 0;
        String name;

        Car(double x, double y, Color body, Color trim, boolean ai, String name) {
            this.x = drawX = x; this.y = drawY = y;
            this.angle = drawAngle = Math.random() * Math.PI * 2;
            this.body = body; this.trim = trim; this.isAI = ai; this.name = name;
        }

        void update(double dt) {
            if (!alive) return;
            drawX     += (x - drawX) * 0.35;
            drawY     += (y - drawY) * 0.35;
            drawAngle += normAngle(angle - drawAngle) * 0.35;
            drawAngle  = normAngle(drawAngle);

            x += vx * dt; y += vy * dt;
            vx *= FRICTION; vy *= FRICTION;
            speed = Math.hypot(vx, vy);
            hitFlash = Math.max(0, hitFlash - dt * 4);
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

    static class SkidMark { double x,y,angle,life=6;
        SkidMark(double x,double y,double a){this.x=x;this.y=y;this.angle=a;} }

    static class StreetLamp {
        double x, y;
        StreetLamp(double x,double y){this.x=x;this.y=y;}
    }

    // Ambient animation (swallows circling in the sunset sky)
    static class Ambient {
        double x, y, vx, vy, size, phase, rot, life = 1, maxLife = 1;
        Color c;
        Ambient(double x, double y, double vx, double vy, double size, Color c) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.size = size; this.c = c;
            this.phase = Math.random() * Math.PI * 2;
            this.rot = Math.random() * Math.PI * 2;
        }
    }

    // ================= STATE =================
    final List<Car> cars = new ArrayList<>();
    final List<Particle> particles = new ArrayList<>();
    final List<SkidMark> skids = new ArrayList<>();
    final List<StreetLamp> lamps = new ArrayList<>();
    final List<Ambient> ambient = new ArrayList<>();
    final Set<Integer> keys = new HashSet<>();
    Car p1, p2;
    double camShake = 0, time = 0;
    boolean gameOver = false;
    String winnerText = "";
    Font fBig, fMed, fSmall;
    Random rng = new Random(20241124);
    BufferedImage streetImg;   // the street never changes, so it is painted once and cached

    public DemolitionRacingGame5() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        fBig   = new Font("Serif", Font.BOLD, 66);
        fMed   = new Font("Serif", Font.BOLD, 20);
        fSmall = new Font("SansSerif", Font.BOLD, 14);

        initGame();

        // Rome street lamps along top & bottom
        for (int i = 0; i < 10; i++) lamps.add(new StreetLamp(80 + i * 130, HZ + 35));
        for (int i = 0; i < 10; i++) lamps.add(new StreetLamp(80 + i * 130, H - 95));

        // swallows circling in the sunset sky
        for (int i = 0; i < 16; i++)
            ambient.add(new Ambient(rnd() * W, 20 + rnd() * (HZ - 60), 35 + rnd() * 45, 0, 4 + rnd() * 4, new Color(45, 25, 35)));

        new javax.swing.Timer(16, this).start();
    }

    double rnd() { return rng.nextDouble(); }
    Color pick(Color... cs) { return cs[rng.nextInt(cs.length)]; }
    static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    void initGame() {
        cars.clear(); particles.clear(); skids.clear();
        gameOver = false; winnerText = ""; camShake = 0;

        // Italian palette — verde, bianco, rosso
        Color trim = new Color(245, 240, 225);
        p1 = new Car(400, 400, new Color(0, 140, 70), trim, false, "P1");
        // Set isAI = false for local 2-player
        p2 = new Car(880, 400, new Color(200, 35, 45), trim, true, "CPU");

        cars.add(p1); cars.add(p2);

        Color[] pal = { new Color(230, 160, 60), new Color(120, 60, 40), new Color(60, 90, 150) };
        for (int i = 0; i < 3; i++) {
            cars.add(new Car(200 + rng.nextDouble()*880, HZ + 40 + rng.nextDouble()*400,
                    pal[i], new Color(255,240,210), true, "CPU"+(i+1)));
        }
    }

    // ================= UPDATE =================
    @Override public void actionPerformed(ActionEvent e) {
        update(1/60.0);
        repaint();
    }

    void update(double dt) {
        time += dt;
        updateAmbient(dt);
        if (gameOver) return;

        // Player 1
        if (p1.alive) {
            if (keys.contains(KeyEvent.VK_W)) p1.accelerate(ACCEL);
            if (keys.contains(KeyEvent.VK_S)) p1.accelerate(-ACCEL * 0.7);
            if (keys.contains(KeyEvent.VK_A)) p1.turn(-TURN);
            if (keys.contains(KeyEvent.VK_D)) p1.turn(TURN);
            if (p1.speed > 3 && Math.random() < 0.55)
                skids.add(new SkidMark(p1.x - Math.cos(p1.angle)*20,
                        p1.y - Math.sin(p1.angle)*20, p1.angle));
        }

        // P2 / CPU
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
            if (c.x < 30)       { c.x = 30;       c.vx = Math.abs(c.vx)*0.5;  camShake = 8; }
            if (c.x > W-30)     { c.x = W-30;     c.vx = -Math.abs(c.vx)*0.5; camShake = 8; }
            if (c.y < HZ + 20)  { c.y = HZ + 20;  c.vy = Math.abs(c.vy)*0.5;  camShake = 8; }
            if (c.y > H-100)    { c.y = H-100;    c.vy = -Math.abs(c.vy)*0.5; camShake = 8; }
        }

        // Collisions
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

        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) { Particle p = it.next(); p.update(dt); if (p.life <= 0) it.remove(); }
        Iterator<SkidMark> sit = skids.iterator();
        while (sit.hasNext()) { SkidMark s = sit.next(); s.life -= dt; if (s.life <= 0) sit.remove(); }

        camShake *= 0.85;

        long alive = cars.stream().filter(c -> c.alive).count();
        if (alive <= 1) {
            gameOver = true;
            Car w = cars.stream().filter(c -> c.alive).findFirst().orElse(null);
            winnerText = w == null ? "DEMOLIZIONE TOTALE!"
                    : w == p1 ? "VINCE IL GIOCATORE 1 — P1 WINS!"
                    : w == p2 ? (p2.isAI ? "VINCE LA CPU — CPU WINS!" : "VINCE IL GIOCATORE 2 — P2 WINS!")
                    : "VINCE L'AVVERSARIO — AI WINS!";
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

    // ---------- Ambient: swallows circling in the sunset sky ----------
    void updateAmbient(double dt) {
        for (Ambient a : ambient) {
            a.x += a.vx * dt;
            a.y += Math.sin(time * 0.8 + a.phase) * 8 * dt;
            if (a.x > W + 20) { a.x = -20; a.y = 20 + rnd() * (HZ - 60); }
        }
    }

    void drawAmbient(Graphics2D g) {
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Ambient a : ambient) {
            double flap = Math.sin(time * 9 + a.phase * 3), s = a.size;
            Path2D p = new Path2D.Double();
            p.moveTo(a.x - s, a.y - flap * s * 0.7);
            p.quadTo(a.x - s * 0.4, a.y - s * 0.3, a.x, a.y);
            p.quadTo(a.x + s * 0.4, a.y - s * 0.3, a.x + s, a.y - flap * s * 0.7);
            g.setColor(a.c);
            g.draw(p);
        }
    }

    // ================= RENDER =================
    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double shX = (Math.random()-0.5) * camShake;
        double shY = (Math.random()-0.5) * camShake;

        // Sky + Rome skyline (static, doesn't shake)
        drawSkyline(g);

        g.translate(shX, shY);

        // Street surface (cached)
        if (streetImg == null) {
            streetImg = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D sg = streetImg.createGraphics();
            sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            buildStreet(sg);
            sg.dispose();
        }
        g.drawImage(streetImg, 0, 0, null);

        // Lamps glow (before cars so cars get lit)
        for (StreetLamp L : lamps) drawLampGlow(g, L);

        // Local transit signs
        drawSign(g, 120, HZ + 12);
        drawSign(g, W - 220, HZ + 12);

        // Skid marks
        for (SkidMark s : skids) {
            float a = (float) Math.min(0.5, s.life/6 * 0.5);
            g.setColor(new Color(0,0,0,a));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(s.x, s.y,
                    s.x - Math.cos(s.angle)*10, s.y - Math.sin(s.angle)*10));
        }

        // Motion trails
        for (Car c : cars) if (c.alive && c.speed >= 2) drawMotionTrail(g, c);

        // Cars
        for (Car c : cars) drawCar(g, c);

        // Particles
        for (Particle p : particles) {
            double a = Math.max(0, p.life / p.maxLife);
            g.setColor(new Color(p.c.getRed(), p.c.getGreen(), p.c.getBlue(), (int)(a*255)));
            g.fill(new Ellipse2D.Double(p.x - p.size/2, p.y - p.size/2, p.size, p.size));
        }

        // Ambient animation on top
        drawAmbient(g);

        g.translate(-shX, -shY);

        // HUD
        drawHUD(g);
        if (gameOver) drawGameOver(g);
    }

    // ---------- Rome skyline ----------
    void drawSkyline(Graphics2D g) {
        skyGradient(g, new Color(96, 64, 128), new Color(255, 170, 105));
        sunGlow(g, W * 0.58, HZ - 32, 30, new Color(255, 214, 120));
        clouds(g, new Color(255, 180, 160, 70), 6, 6, 50);
        g.setColor(new Color(150, 90, 100));      // Roman hills
        g.fill(new Ellipse2D.Double(-100, HZ - 40, 700, 120));
        g.fill(new Ellipse2D.Double(600, HZ - 30, 900, 110));
        drawStPeters(g, W - 250, HZ - 10);
        cityRow(g, 5, 18, 40, 52, HZ, new Color(150, 80, 60), new Color(255, 220, 160, 130));
        drawColosseum(g, 300, HZ);
        for (int i = 0; i < 5; i++)
            umbrellaPine(g, 640 + i * 70, HZ, 70 + (i % 2) * 14, new Color(70, 40, 35), new Color(55, 60, 40));
    }

    void drawColosseum(Graphics2D g, int x, int base) {
        int w = 260, h = 92;
        Path2D p = new Path2D.Double();     // outer ring, broken on the right
        p.moveTo(x, base);
        p.lineTo(x, base - h);
        p.lineTo(x + w * 0.55, base - h);
        p.lineTo(x + w * 0.62, base - h + 18);
        p.lineTo(x + w * 0.78, base - h + 26);
        p.lineTo(x + w * 0.86, base - h + 44);
        p.lineTo(x + w, base - h + 52);
        p.lineTo(x + w, base);
        p.closePath();
        g.setPaint(new GradientPaint(x, base - h, new Color(240, 180, 125), x + w, base, new Color(170, 100, 70)));
        g.fill(p);
        Shape old = g.getClip();
        g.clip(p);
        Color dark = new Color(110, 60, 45);
        for (int tier = 0; tier < 4; tier++) {
            int ty = base - 22 - tier * 22;
            g.setColor(new Color(150, 90, 65));
            g.fillRect(x, ty + 16, w, 3);
            if (tier == 3) continue;
            g.setColor(dark);
            for (int ax = x + 6; ax < x + w - 10; ax += 18)
                g.fill(new RoundRectangle2D.Double(ax, ty, 10, 15, 10, 10));
        }
        g.setColor(dark);
        for (int ax = x + 12; ax < x + w; ax += 36) g.fillRect(ax, base - h + 6, 6, 8);
        g.setClip(old);
        g.setColor(new Color(255, 200, 120, 70));    // warm light in the lower arches
        for (int ax = x + 6; ax < x + w - 10; ax += 18) g.fillRect(ax + 2, base - 18, 6, 10);
    }

    void drawStPeters(Graphics2D g, int cx, int base) {
        g.setColor(new Color(150, 100, 110));
        g.fillRect(cx - 80, base - 30, 160, 30);
        g.fillRect(cx - 30, base - 52, 60, 24);
        g.fill(new Arc2D.Double(cx - 34, base - 92, 68, 80, 0, 180, Arc2D.CHORD));
        g.fillRect(cx - 5, base - 102, 10, 12);
        g.fill(new Ellipse2D.Double(cx - 6, base - 108, 12, 10));
        g.fillRect(cx - 1, base - 118, 2, 10);
        g.fillRect(cx - 4, base - 114, 8, 2);
        g.setColor(new Color(255, 210, 150, 70));
        g.fill(new Arc2D.Double(cx - 34, base - 92, 68, 80, 30, 120, Arc2D.PIE));
    }


    void skyGradient(Graphics2D g, Color top, Color bottom) {
        g.setPaint(new GradientPaint(0, 0, top, 0, HZ, bottom));
        g.fillRect(0, 0, W, HZ);
    }

    void sunGlow(Graphics2D g, double x, double y, double r, Color c) {
        double pulse = 1 + 0.04 * Math.sin(time * 1.5);
        for (int i = 5; i >= 1; i--) {
            double rr = r * (1 + i * 0.55) * pulse;
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 16));
            g.fill(new Ellipse2D.Double(x - rr, y - rr, rr * 2, rr * 2));
        }
        g.setColor(c);
        g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
    }

    void clouds(Graphics2D g, Color c, double speed, int n, int yRange) {
        g.setColor(c);
        double span = W + 300;
        for (int i = 0; i < n; i++) {
            double x = ((i * 397 + time * speed * (0.6 + (i % 3) * 0.25)) % span) - 150;
            double y = 18 + (i * 71) % Math.max(20, yRange);
            double s = 0.7 + (i % 4) * 0.2;
            g.fill(new Ellipse2D.Double(x, y, 90 * s, 26 * s));
            g.fill(new Ellipse2D.Double(x + 20 * s, y - 12 * s, 50 * s, 30 * s));
            g.fill(new Ellipse2D.Double(x + 45 * s, y - 6 * s, 45 * s, 26 * s));
        }
    }

    void cityRow(Graphics2D g, long seed, int minH, int maxH, int bw, int baseY, Color body, Color win) {
        Random r = new Random(seed);
        for (int x = -10; x < W; x += bw) {
            int w = bw - 3 - r.nextInt(6), h = minH + r.nextInt(Math.max(1, maxH - minH));
            g.setColor(body);
            g.fillRect(x, baseY - h, w, h);
            for (int wy = baseY - h + 7; wy < baseY - 6; wy += 11)
                for (int wx = x + 5; wx < x + w - 6; wx += 10)
                    if (r.nextInt(3) == 0) { g.setColor(win); g.fillRect(wx, wy, 3, 4); }
        }
    }

    void umbrellaPine(Graphics2D g, double x, double base, double h, Color trunk, Color crown) {
        double sway = Math.sin(time * 0.9 + x) * 2.5;
        g.setColor(trunk);
        g.setStroke(new BasicStroke(3f));
        g.draw(new QuadCurve2D.Double(x, base, x - 4, base - h * 0.5, x + sway, base - h * 0.8));
        g.setColor(crown);
        g.fill(new Ellipse2D.Double(x + sway - h * 0.45, base - h, h * 0.9, h * 0.32));
        g.fill(new Ellipse2D.Double(x + sway - h * 0.3, base - h * 1.08, h * 0.55, h * 0.25));
    }

    void letterSign(Graphics2D g, int x, int y, int shape, Color bg, Color fg, String txt, int fs) {
        g.setColor(new Color(90, 90, 95));
        g.fillRect(x + 21, y + 44, 4, 16);
        g.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 45));
        g.fillOval(x - 18, y - 18, 82, 82);
        g.setColor(bg);
        Shape s = shape == 1 ? new Ellipse2D.Double(x, y, 46, 46)
                : shape == 2 ? new Polygon(new int[]{x + 23, x + 49, x + 23, x - 3}, new int[]{y - 3, y + 23, y + 49, y + 23}, 4)
                : new RoundRectangle2D.Double(x, y, 46, 46, 8, 8);
        g.fill(s);
        g.setColor(fg);
        g.setFont(new Font("SansSerif", Font.BOLD, fs));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(txt, x + 23 - fm.stringWidth(txt) / 2, y + 23 + fm.getAscent() / 2 - 3);
    }

    // ---------- Street ----------
    void buildStreet(Graphics2D g) {
        g.setColor(new Color(40, 36, 34));
        g.fillRect(0, HZ, W, H - HZ);
        Random tr = new Random(42);   // cobblestones
        for (int y = HZ, row = 0; y < H; y += 14, row++)
            for (int x = (row % 2) * -11; x < W; x += 22) {
                int v = tr.nextInt(24) - 12;
                g.setColor(new Color(clamp(96 + v), clamp(88 + v), clamp(82 + v)));
                g.fillRoundRect(x + 1, y + 1, 20, 12, 7, 7);
            }
        g.setPaint(new GradientPaint(0, HZ, new Color(0, 0, 0, 0), 0, H, new Color(0, 0, 0, 80)));
        g.fillRect(0, HZ, W, H - HZ);

        // colourful band along the bottom
        int bandY = H - 70;
        Color[] band = { new Color(0, 140, 70), new Color(245, 245, 240), new Color(200, 35, 45) };
        for (int x = 0; x < W; x += 40) {
            g.setColor(band[(x / 40) % band.length]);
            g.fillRect(x, bandY, 40, 6);
        }
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, bandY + 6, W, 24);
        g.setColor(new Color(255, 240, 210, 170));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("ROMA  ·  DERBY DI DEMOLIZIONE  ·  ROMA", 30, bandY + 22);
    }

    void drawLampGlow(Graphics2D g, StreetLamp L) {
        double pulse = 0.85 + 0.15 * Math.sin(time * 2 + L.x);
        Color lc = new Color(255, 200, 130);
        for (int i = 4; i >= 1; i--) {
            double rr = 40 * i * pulse;
            int a = (int)(20 * pulse / i * LAMP_DIM);
            g.setColor(new Color(lc.getRed(), lc.getGreen(), lc.getBlue(), a));
            g.fill(new Ellipse2D.Double(L.x - rr, L.y - rr * 0.6, rr*2, rr*1.2));
        }
        // lamp head
        g.setColor(new Color(60, 60, 70));
        g.fillRect((int)L.x - 2, (int)L.y - 30, 4, 30);
        g.fillRect((int)L.x - 12, (int)L.y - 32, 24, 6);
        g.setColor(lc.brighter());
        g.fill(new Ellipse2D.Double(L.x - 6, L.y - 26, 12, 8));
    }

    void drawSign(Graphics2D g, int x, int y) {
        letterSign(g, x, y, 0, new Color(190, 30, 40), Color.WHITE, "M", 32);
    }

    // ---------- Cars ----------
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

        // shadow
        g.setColor(new Color(0,0,0,90));
        g.fill(new Ellipse2D.Double(c.drawX - 26 + 3, c.drawY - 14 + 4, 54, 30));

        AffineTransform old = g.getTransform();
        g.translate(c.drawX, c.drawY);
        g.rotate(c.drawAngle);

        // body gradient
        GradientPaint bodyGrad = new GradientPaint(-26, -13, c.body.brighter(),
                -26, 13, c.body.darker());
        g.setPaint(bodyGrad);
        g.fill(new RoundRectangle2D.Double(-26, -13, 52, 26, 10, 10));

        // trim
        g.setColor(c.trim);
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Double(-24, -11, 48, 22, 8, 8));
        g.draw(new RoundRectangle2D.Double(-26, -13, 52, 26, 10, 10));

        // roof highlight
        g.setPaint(new GradientPaint(-10, -10, new Color(255,255,255,120),
                10, 10, new Color(255,255,255,0)));
        g.fill(new RoundRectangle2D.Double(-18, -8, 30, 16, 6, 6));

        // windshield
        g.setColor(new Color(180, 220, 255, 200));
        g.fill(new RoundRectangle2D.Double(6, -9, 12, 18, 4, 4));

        // headlights (yellow)
        g.setColor(Color.YELLOW);
        g.fill(new Ellipse2D.Double(-24, -11, 6, 6));
        g.fill(new Ellipse2D.Double(-24, 5, 6, 6));
        // headlight glow
        g.setColor(new Color(255, 240, 100, 60));
        g.fill(new Ellipse2D.Double(-38, -16, 20, 16));
        g.fill(new Ellipse2D.Double(-38, 2, 20, 16));

        // taillights
        g.setColor(new Color(255, 60, 60));
        g.fill(new Ellipse2D.Double(20, -10, 4, 4));
        g.fill(new Ellipse2D.Double(20, 6, 4, 4));

        if (c.hitFlash > 0) {
            int alpha = (int)(c.hitFlash * 180);
            g.setColor(new Color(255, 80, 80, alpha));
            g.fill(new RoundRectangle2D.Double(-26, -13, 52, 26, 10, 10));
        }

        g.setTransform(old);
    }

    // ---------- HUD ----------
    void drawHUD(Graphics2D g) {
        g.setColor(new Color(15, 15, 20, 170));
        g.fillRoundRect(20, 20, 320, 56, 14, 14);
        g.fillRoundRect(W-340, 20, 320, 56, 14, 14);

        drawHealthBar(g, 30, 28, p1, "G1  ·  Giocatore 1");
        drawHealthBar(g, W-330, 28, p2, p2.isAI ? "CPU  ·  Computer" : "G2  ·  Giocatore 2");

        g.setFont(fSmall);
        g.setColor(new Color(15, 15, 20, 150));
        g.fillRoundRect(20, H - 38, W - 40, 26, 10, 10);
        g.setColor(new Color(255,230,190,220));
        g.drawString("P1: WASD    P2: Arrows    R: Restart", 30, H - 20);
        String foot = "ROMA — DERBY DI DEMOLIZIONE";
        g.drawString(foot, W - 30 - g.getFontMetrics().stringWidth(foot), H - 20);
    }

    void drawHealthBar(Graphics2D g, int x, int y, Car c, String label) {
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
        g.setColor(new Color(250, 190, 100));
        Font f = fBig;
        FontMetrics fm = g.getFontMetrics(f);
        while (fm.stringWidth(winnerText) > W - 80 && f.getSize() > 24) {
            f = f.deriveFont((float) (f.getSize() - 2));
            fm = g.getFontMetrics(f);
        }
        g.setFont(f);
        g.drawString(winnerText, (W - fm.stringWidth(winnerText))/2, H/2);
        g.setFont(fMed);
        String sub = "R — ricomincia  ·  Restart";
        fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(sub, (W - fm.stringWidth(sub))/2, H/2 + 60);
    }

    // ---------- Input ----------
    @Override public void keyPressed(KeyEvent e) {
        keys.add(e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_R) initGame();
        if (e.getKeyCode() == KeyEvent.VK_SPACE && !p2.isAI) p2.isAI = true;
    }
    @Override public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
    @Override public void keyTyped(KeyEvent e) {}

    static double normAngle(double a) {
        while (a > Math.PI) a -= Math.PI * 2;
        while (a < -Math.PI) a += Math.PI * 2;
        return a;
    }

    // ---------- Main ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Demolition Racing 5 — Rome Edition");
            DemolitionRacingGame5 panel = new DemolitionRacingGame5();
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
