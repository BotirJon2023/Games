// DemolitionRacingGame4.java  — pure Java, no external libraries
// Paris Edition: Eiffel Tower sparkle, rotating searchlight, Sacré-Cœur & golden bokeh at night
// Compile: javac DemolitionRacingGame4.java
// Run:     java DemolitionRacingGame4
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class DemolitionRacingGame4 extends JPanel implements ActionListener, KeyListener {

    static final int W = 1280, H = 720;
    static final int HZ = 150;                 // horizon: city skyline above, street below
    static final double FRICTION = 0.92, TURN = 0.055, ACCEL = 0.32;
    static final double LAMP_DIM = 1.0;          // street lamp glow strength (day < night)

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

    // Ambient animation (golden bokeh lights floating over Paris)
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
    Random rng = new Random(20241123);
    BufferedImage streetImg;   // the street never changes, so it is painted once and cached

    public DemolitionRacingGame4() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        fBig   = new Font("Serif", Font.BOLD, 66);
        fMed   = new Font("Serif", Font.BOLD, 20);
        fSmall = new Font("SansSerif", Font.BOLD, 14);

        initGame();

        // Paris street lamps along top & bottom
        for (int i = 0; i < 10; i++) lamps.add(new StreetLamp(80 + i * 130, HZ + 35));
        for (int i = 0; i < 10; i++) lamps.add(new StreetLamp(80 + i * 130, H - 95));

        // golden bokeh lights floating over Paris
        Color[] bc = { new Color(255, 210, 120, 45), new Color(255, 170, 200, 40), new Color(255, 245, 225, 40) };
        for (int i = 0; i < 40; i++)
            ambient.add(new Ambient(rnd() * W, rnd() * H, -6 + rnd() * 12, -4 - rnd() * 8,
                    12 + rnd() * 34, pick(bc)));

        new javax.swing.Timer(16, this).start();
    }

    double rnd() { return rng.nextDouble(); }
    Color pick(Color... cs) { return cs[rng.nextInt(cs.length)]; }
    static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    void initGame() {
        cars.clear(); particles.clear(); skids.clear();
        gameOver = false; winnerText = ""; camShake = 0;

        // French tricolore palette — bleu, blanc, rouge
        Color trim = new Color(245, 240, 230);
        p1 = new Car(400, 400, new Color(0, 70, 170), trim, false, "P1");
        // Set isAI = false for local 2-player
        p2 = new Car(880, 400, new Color(205, 30, 50), trim, true, "CPU");

        cars.add(p1); cars.add(p2);

        Color[] pal = { new Color(230, 190, 80), new Color(40, 40, 50), new Color(150, 90, 170) };
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
            winnerText = w == null ? "DÉMOLITION TOTALE !"
                    : w == p1 ? "JOUEUR 1 GAGNE — P1 WINS!"
                    : w == p2 ? (p2.isAI ? "CPU GAGNE — CPU WINS!" : "JOUEUR 2 GAGNE — P2 WINS!")
                    : "L'ADVERSAIRE GAGNE — AI WINS!";
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

    // ---------- Ambient: golden bokeh lights floating over Paris ----------
    void updateAmbient(double dt) {
        for (Ambient a : ambient) {
            a.x += a.vx * dt; a.y += a.vy * dt;
            if (a.y > H + 20) { a.y = -20; a.x = rnd() * W; }
            if (a.y < -60) { a.y = H + 40; a.x = rnd() * W; }
            if (a.x < -40) a.x += W + 80;
            if (a.x > W + 40) a.x -= W + 80;
        }
    }

    void drawAmbient(Graphics2D g) {
        g.setStroke(new BasicStroke(1f));
        for (Ambient a : ambient) {
            double p = 0.5 + 0.5 * Math.sin(time * 1.2 + a.phase);
            int al = (int) (a.c.getAlpha() * p);
            g.setColor(new Color(a.c.getRed(), a.c.getGreen(), a.c.getBlue(), al));
            g.fill(new Ellipse2D.Double(a.x - a.size / 2, a.y - a.size / 2, a.size, a.size));
            g.setColor(new Color(a.c.getRed(), a.c.getGreen(), a.c.getBlue(), Math.min(255, al * 2)));
            g.draw(new Ellipse2D.Double(a.x - a.size / 2, a.y - a.size / 2, a.size, a.size));
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

        // Sky + Paris skyline (static, doesn't shake)
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

    // ---------- Paris skyline ----------
    void drawSkyline(Graphics2D g) {
        skyGradient(g, new Color(12, 14, 46), new Color(92, 62, 120));
        stars(g, 70);
        moon(g, 110, 42, 20);
        // Montmartre hill with the Sacré-Cœur
        g.setColor(new Color(34, 28, 62));
        g.fill(new Ellipse2D.Double(40, HZ - 42, 340, 110));
        drawSacreCoeur(g, 210, HZ - 40);
        drawEiffel(g, W / 2 + 200, HZ - 6);
        // Haussmann rooftops and the Seine
        cityRow(g, 75, 22, 48, 44, HZ, new Color(28, 24, 58), new Color(255, 214, 150, 130));
        water(g, HZ - 10, HZ, new Color(30, 30, 80), new Color(20, 20, 50), new Color(255, 220, 150, 120));
    }

    void drawSacreCoeur(Graphics2D g, int cx, int base) {
        g.setColor(new Color(255, 235, 200, 30));
        g.fill(new Ellipse2D.Double(cx - 80, base - 100, 160, 120));
        Color stone = new Color(222, 216, 236), shade = new Color(170, 160, 195);
        g.setColor(stone);
        g.fillRect(cx - 48, base - 30, 96, 30);
        g.fill(new Ellipse2D.Double(cx - 22, base - 70, 44, 50));
        g.fillRect(cx - 18, base - 46, 36, 18);
        g.fill(new Ellipse2D.Double(cx - 44, base - 48, 22, 24));
        g.fill(new Ellipse2D.Double(cx + 22, base - 48, 22, 24));
        g.fillRect(cx - 2, base - 84, 4, 16);
        g.fill(new Ellipse2D.Double(cx - 5, base - 76, 10, 10));
        g.setColor(shade);
        for (int i = 0; i < 6; i++) g.fillRect(cx - 40 + i * 15, base - 22, 5, 14);
    }

    void drawEiffel(Graphics2D g, int cx, int base) {
        int top = base - 132;
        // rotating searchlight
        for (int k = 0; k < 2; k++) {
            double ang = time * 0.8 + k * Math.PI;
            double ex = cx + Math.cos(ang) * 620, ey = top - 6 + Math.sin(ang) * 26;
            Path2D beam = new Path2D.Double();
            beam.moveTo(cx, top);
            beam.lineTo(ex, ey - 22);
            beam.lineTo(ex, ey + 22);
            beam.closePath();
            g.setPaint(new GradientPaint(cx, top, new Color(255, 240, 200, 70),
                    (float) ex, (float) ey, new Color(255, 240, 200, 0)));
            g.fill(beam);
        }
        Path2D p = new Path2D.Double();
        p.moveTo(cx - 50, base);
        p.quadTo(cx - 24, base - 40, cx - 15, base - 72);
        p.lineTo(cx - 7, base - 108);
        p.lineTo(cx - 2, top);
        p.lineTo(cx + 2, top);
        p.lineTo(cx + 7, base - 108);
        p.lineTo(cx + 15, base - 72);
        p.quadTo(cx + 24, base - 40, cx + 50, base);
        p.lineTo(cx + 32, base);
        p.quadTo(cx, base - 42, cx - 32, base);
        p.closePath();
        g.setPaint(new GradientPaint(cx, top, new Color(255, 196, 110), cx, base, new Color(170, 100, 50)));
        g.fill(p);
        // iron lattice
        Shape oldClip = g.getClip();
        g.clip(p);
        g.setColor(new Color(90, 45, 20, 120));
        g.setStroke(new BasicStroke(1f));
        for (int d = -160; d < 160; d += 7) {
            g.drawLine(cx + d, base, cx + d + 140, top);
            g.drawLine(cx + d, base, cx + d - 140, top);
        }
        g.setClip(oldClip);
        g.setColor(new Color(120, 70, 35));
        g.fillRect(cx - 26, base - 46, 52, 5);
        g.fillRect(cx - 14, base - 80, 28, 4);
        g.fillRect(cx - 5, top + 8, 10, 4);
        // the famous sparkle
        if (Math.sin(time * 0.9) > 0.2) {
            Random sr = new Random((long) (time * 14));
            g.setColor(new Color(255, 255, 255, 230));
            for (int i = 0; i < 45; i++) {
                double t = sr.nextDouble();
                double y = base - t * (base - top);
                double hw = 2 + 48 * Math.pow(1 - t, 2.2);
                double x = cx + (sr.nextDouble() * 2 - 1) * hw;
                g.fill(new Ellipse2D.Double(x - 1.5, y - 1.5, 3, 3));
            }
        }
        g.setColor(new Color(255, 230, 160));
        g.fill(new Ellipse2D.Double(cx - 3, top - 4, 6, 6));
    }


    void skyGradient(Graphics2D g, Color top, Color bottom) {
        g.setPaint(new GradientPaint(0, 0, top, 0, HZ, bottom));
        g.fillRect(0, 0, W, HZ);
    }

    void stars(Graphics2D g, int n) {
        for (int i = 0; i < n; i++) {
            int sx = (i * 137 + 41) % W, sy = (i * 53) % (HZ - 40);
            int a = 110 + (int) (80 * Math.sin(time * 2 + i));
            g.setColor(new Color(255, 245, 220, Math.max(30, a)));
            g.fillRect(sx, sy, 2, 2);
        }
    }

    void moon(Graphics2D g, double x, double y, double r) {
        g.setColor(new Color(255, 250, 225, 35));
        g.fill(new Ellipse2D.Double(x - r * 2, y - r * 2, r * 4, r * 4));
        g.setColor(new Color(255, 248, 225, 230));
        g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        g.setColor(new Color(210, 200, 170, 110));
        g.fill(new Ellipse2D.Double(x - r * 0.4, y - r * 0.3, r * 0.6, r * 0.5));
        g.fill(new Ellipse2D.Double(x + r * 0.2, y + r * 0.25, r * 0.35, r * 0.3));
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

    void water(Graphics2D g, int y0, int y1, Color top, Color bottom, Color glint) {
        g.setPaint(new GradientPaint(0, y0, top, 0, y1, bottom));
        g.fillRect(0, y0, W, y1 - y0);
        g.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < 46; i++) {
            double yy = y0 + 3 + (i * 7.3) % Math.max(4, y1 - y0 - 4);
            double xx = (i * 211) % W + Math.sin(time * 0.9 + i) * 30;
            double len = 14 + (i % 5) * 8;
            int a = (int) (glint.getAlpha() * (0.5 + 0.5 * Math.sin(time * 2.2 + i * 1.7)));
            g.setColor(new Color(glint.getRed(), glint.getGreen(), glint.getBlue(), Math.max(0, a)));
            g.draw(new Line2D.Double(xx, yy, xx + len, yy));
        }
    }

    // ---------- Street ----------
    void buildStreet(Graphics2D g) {
        g.setPaint(new GradientPaint(0, HZ, new Color(46, 42, 62), 0, H, new Color(20, 18, 30)));
        g.fillRect(0, HZ, W, H - HZ);
        Random tr = new Random(42);   // asphalt texture speckles
        g.setColor(new Color(255, 255, 255, 10));
        for (int i = 0; i < 500; i++) g.fillRect(tr.nextInt(W), HZ + tr.nextInt(H - HZ), 2, 2);

        // lane markings
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{30f, 30f}, 0f));
        g.setColor(new Color(255, 220, 150, 90));
        g.drawLine(0, H/2 + 40, W, H/2 + 40);

        // colourful band along the bottom
        int bandY = H - 70;
        Color[] band = { new Color(0, 70, 170), new Color(245, 245, 245), new Color(205, 30, 50) };
        for (int x = 0; x < W; x += 40) {
            g.setColor(band[(x / 40) % band.length]);
            g.fillRect(x, bandY, 40, 6);
        }
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, bandY + 6, W, 24);
        g.setColor(new Color(255, 240, 210, 170));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("PARIS  ·  DERBY DE DÉMOLITION  ·  PARIS", 30, bandY + 22);
    }

    void drawLampGlow(Graphics2D g, StreetLamp L) {
        double pulse = 0.85 + 0.15 * Math.sin(time * 2 + L.x);
        Color lc = new Color(255, 214, 150);
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
        g.setColor(new Color(40, 95, 70));
        g.fillRect(x + 4, y + 12, 4, 46);
        g.fillRect(x + 50, y + 12, 4, 46);
        g.setStroke(new BasicStroke(3f));
        g.draw(new QuadCurve2D.Double(x + 6, y + 14, x + 29, y - 6, x + 52, y + 14));
        g.setColor(new Color(245, 225, 160));
        g.fillRect(x + 6, y + 8, 46, 12);
        g.setColor(new Color(40, 95, 70));
        g.setFont(new Font("Serif", Font.BOLD, 10));
        g.drawString("MÉTRO", x + 12, y + 18);
        double f = 0.75 + 0.25 * Math.sin(time * 2);
        for (int s = 0; s < 2; s++) {
            int lx = s == 0 ? x + 6 : x + 52;
            g.setColor(new Color(255, 140, 60, (int) (50 * f)));
            g.fill(new Ellipse2D.Double(lx - 12, y - 7, 24, 26));
            g.setColor(new Color(255, 140, 60, (int) (220 * f)));
            g.fill(new Ellipse2D.Double(lx - 4, y + 1, 8, 10));
        }
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

        drawHealthBar(g, 30, 28, p1, "J1  ·  Joueur 1");
        drawHealthBar(g, W-330, 28, p2, p2.isAI ? "CPU  ·  Ordinateur" : "J2  ·  Joueur 2");

        g.setFont(fSmall);
        g.setColor(new Color(15, 15, 20, 150));
        g.fillRoundRect(20, H - 38, W - 40, 26, 10, 10);
        g.setColor(new Color(255,230,190,220));
        g.drawString("P1: WASD    P2: Arrows    R: Restart", 30, H - 20);
        String foot = "PARIS — DERBY DE DÉMOLITION";
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
        g.setColor(new Color(240, 205, 120));
        Font f = fBig;
        FontMetrics fm = g.getFontMetrics(f);
        while (fm.stringWidth(winnerText) > W - 80 && f.getSize() > 24) {
            f = f.deriveFont((float) (f.getSize() - 2));
            fm = g.getFontMetrics(f);
        }
        g.setFont(f);
        g.drawString(winnerText, (W - fm.stringWidth(winnerText))/2, H/2);
        g.setFont(fMed);
        String sub = "R — recommencer  ·  Restart";
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
            JFrame f = new JFrame("Demolition Racing 4 — Paris Edition");
            DemolitionRacingGame4 panel = new DemolitionRacingGame4();
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
