// DemolitionRacingGame11.java  — pure Java, no external libraries
// Lisbon Edition: the yellow tram climbing Alfama, the red 25 de Abril bridge and jacaranda petals
// Compile: javac DemolitionRacingGame11.java
// Run:     java DemolitionRacingGame11
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class DemolitionRacingGame11 extends JPanel implements ActionListener, KeyListener {

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

    // Ambient animation (purple jacaranda petals)
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
    Random rng = new Random(20241130);
    BufferedImage streetImg;   // the street never changes, so it is painted once and cached

    public DemolitionRacingGame11() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        fBig   = new Font("Serif", Font.BOLD, 66);
        fMed   = new Font("Serif", Font.BOLD, 20);
        fSmall = new Font("SansSerif", Font.BOLD, 14);

        initGame();

        // Lisbon street lamps along top & bottom
        for (int i = 0; i < 10; i++) lamps.add(new StreetLamp(80 + i * 130, HZ + 35));
        for (int i = 0; i < 10; i++) lamps.add(new StreetLamp(80 + i * 130, H - 95));

        // purple jacaranda petals
        Color[] fc = { new Color(160, 110, 220), new Color(190, 140, 235), new Color(130, 90, 200) };
        for (int i = 0; i < 60; i++)
            ambient.add(new Ambient(rnd() * W, rnd() * H, 10 + rnd() * 30, 35 + rnd() * 45,
                    4 + rnd() * 3, pick(fc)));

        new javax.swing.Timer(16, this).start();
    }

    double rnd() { return rng.nextDouble(); }
    Color pick(Color... cs) { return cs[rng.nextInt(cs.length)]; }
    static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    void initGame() {
        cars.clear(); particles.clear(); skids.clear();
        gameOver = false; winnerText = ""; camShake = 0;

        // Portuguese palette — verde e vermelho
        Color trim = new Color(250, 210, 60);
        p1 = new Car(400, 400, new Color(0, 110, 60), trim, false, "P1");
        // Set isAI = false for local 2-player
        p2 = new Car(880, 400, new Color(210, 30, 40), trim, true, "CPU");

        cars.add(p1); cars.add(p2);

        Color[] pal = { new Color(250, 200, 40), new Color(40, 90, 170), new Color(240, 170, 160) };
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
            winnerText = w == null ? "DEMOLIÇÃO TOTAL!"
                    : w == p1 ? "JOGADOR 1 VENCE — P1 WINS!"
                    : w == p2 ? (p2.isAI ? "CPU VENCE — CPU WINS!" : "JOGADOR 2 VENCE — P2 WINS!")
                    : "ADVERSÁRIO VENCE — AI WINS!";
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

    // ---------- Ambient: purple jacaranda petals ----------
    void updateAmbient(double dt) {
        for (Ambient a : ambient) {
            a.x += (a.vx + Math.sin(time * 1.7 + a.phase) * 35) * dt;
            a.y += a.vy * dt;
            a.rot += dt * (1.5 + Math.sin(a.phase) * 1.5);
            if (a.y > H + 20) { a.y = -20; a.x = rnd() * W; }
            if (a.y < -60) { a.y = H + 40; a.x = rnd() * W; }
            if (a.x < -40) a.x += W + 80;
            if (a.x > W + 40) a.x -= W + 80;
        }
    }

    void drawAmbient(Graphics2D g) {
        g.setStroke(new BasicStroke(1f));
        for (Ambient a : ambient) {
            AffineTransform at = g.getTransform();
            g.translate(a.x, a.y);
            g.rotate(a.rot);
            g.setColor(a.c);
            g.scale(Math.max(0.25, Math.abs(Math.cos(a.rot * 1.3))), 1);
            g.fill(new Ellipse2D.Double(-a.size / 2, -a.size / 3, a.size, a.size * 0.66));
            g.setTransform(at);
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

        // Sky + Lisbon skyline (static, doesn't shake)
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

    // ---------- Lisbon skyline ----------
    void drawSkyline(Graphics2D g) {
        skyGradient(g, new Color(86, 74, 150), new Color(255, 176, 140));
        sunGlow(g, 900, HZ - 46, 26, new Color(255, 215, 150));
        clouds(g, new Color(255, 190, 190, 70), 6, 5, 40);
        drawAlfamaHill(g);
        water(g, HZ - 30, HZ, new Color(200, 130, 140), new Color(90, 80, 130), new Color(255, 225, 180, 170));
        reflection(g, 900, HZ - 28, HZ, 40, new Color(255, 210, 150, 70));
        drawPonte25(g, 640, W + 40, HZ - 30);
        drawTram(g);
    }

    void drawAlfamaHill(Graphics2D g) {
        g.setColor(new Color(120, 90, 110));
        g.fill(new Ellipse2D.Double(-120, HZ - 96, 760, 200));
        Color[] cols = { new Color(245, 215, 120), new Color(240, 170, 160), new Color(150, 200, 230),
                new Color(250, 245, 235), new Color(230, 140, 90), new Color(200, 225, 190) };
        Random r = new Random(11);
        for (int row = 0; row < 4; row++) {
            for (int x = -10; x < 640; x += 26) {
                double topY = HZ + 4 - 100 * Math.sqrt(Math.max(0, 1 - Math.pow((x + 11 - 260) / 380.0, 2)));
                int y = (int) (topY + 10 + row * 17);
                if (y > HZ - 40) continue;
                g.setColor(cols[r.nextInt(cols.length)]);
                g.fillRect(x, y, 22, 15);
                g.setColor(new Color(190, 90, 60));       // terracotta roofs
                g.fill(new Polygon(new int[]{x - 2, x + 11, x + 24}, new int[]{y, y - 6, y}, 3));
                g.setColor(new Color(80, 70, 90, 160));
                g.fillRect(x + 4, y + 4, 4, 5);
                g.fillRect(x + 13, y + 4, 4, 5);
            }
        }
        int cx = 260, cy = HZ - 98;                     // Castelo de São Jorge
        g.setColor(new Color(150, 130, 120));
        g.fillRect(cx - 50, cy - 10, 100, 16);
        for (int i = 0; i < 5; i++) g.fillRect(cx - 50 + i * 24, cy - 22, 12, 14);
        for (int i = 0; i < 9; i++) g.fillRect(cx - 48 + i * 12, cy - 14, 6, 4);
        g.setColor(new Color(60, 60, 60));
        g.fillRect(cx, cy - 36, 1, 14);
        int w = (int) (Math.sin(time * 4) * 2);
        g.setColor(new Color(0, 110, 60));
        g.fill(new Polygon(new int[]{cx + 1, cx + 6, cx + 6, cx + 1}, new int[]{cy - 36, cy - 36 + w / 2, cy - 28 + w / 2, cy - 28}, 4));
        g.setColor(new Color(210, 30, 40));
        g.fill(new Polygon(new int[]{cx + 6, cx + 15, cx + 15, cx + 6}, new int[]{cy - 36 + w / 2, cy - 36 + w, cy - 28 + w, cy - 28 + w / 2}, 4));
    }

    void drawPonte25(Graphics2D g, int x0, int x1, int deckBase) {
        int deck = deckBase - 22;
        Color red = new Color(190, 55, 45);
        int t1 = x0 + 140, t2 = x0 + 480;
        g.setColor(red);
        for (int tx : new int[]{t1, t2}) {
            g.fillRect(tx - 6, deck - 70, 4, 92);
            g.fillRect(tx + 2, deck - 70, 4, 92);
            g.fillRect(tx - 6, deck - 70, 12, 4);
            g.fillRect(tx - 6, deck - 30, 12, 3);
        }
        g.setStroke(new BasicStroke(2f));
        g.draw(new QuadCurve2D.Double(x0, deck, (x0 + t1) / 2.0, deck - 20, t1, deck - 68));
        g.draw(new QuadCurve2D.Double(t1, deck - 68, (t1 + t2) / 2.0, deck + 60, t2, deck - 68));
        g.draw(new QuadCurve2D.Double(t2, deck - 68, (t2 + x1) / 2.0, deck - 20, x1, deck));
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(190, 55, 45, 150));
        for (int x = t1 + 12; x < t2; x += 12) {
            double t = (x - t1) / (double) (t2 - t1);
            double cy = (1 - t) * (1 - t) * (deck - 68) + 2 * (1 - t) * t * (deck + 60) + t * t * (deck - 68);
            g.draw(new Line2D.Double(x, cy, x, deck));
        }
        g.setColor(red.darker());
        g.fillRect(x0, deck, x1 - x0, 5);
        g.setColor(new Color(255, 240, 200));           // tiny cars crossing the bridge
        for (int i = 0; i < 6; i++) {
            double cx = x0 + ((i * 113 + time * 40) % (x1 - x0));
            g.fillRect((int) cx, deck - 3, 4, 2);
        }
    }

    void drawTram(Graphics2D g) {
        int ry = HZ - 36;
        g.setColor(new Color(90, 80, 90));
        g.fillRect(0, ry + 1, 660, 2);
        double x = ((time * 30) % 760) - 80;
        g.setColor(new Color(250, 200, 40));
        g.fill(new RoundRectangle2D.Double(x, ry - 16, 46, 16, 6, 6));
        g.setColor(new Color(240, 240, 230));
        g.fillRect((int) x + 2, ry - 6, 42, 3);
        g.setColor(new Color(60, 80, 110));
        for (int i = 0; i < 5; i++) g.fillRect((int) x + 4 + i * 8, ry - 13, 6, 6);
        g.setColor(new Color(40, 40, 40));
        g.setStroke(new BasicStroke(1f));
        g.draw(new Line2D.Double(x + 22, ry - 16, x + 30, ry - 30));
        g.fill(new Ellipse2D.Double(x + 8, ry - 2, 6, 5));
        g.fill(new Ellipse2D.Double(x + 32, ry - 2, 6, 5));
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

    void reflection(Graphics2D g, double x, int y0, int y1, double w, Color c) {
        g.setColor(c);
        for (int yy = y0; yy < y1; yy += 3) {
            double off = Math.sin(time * 3 + yy * 0.7) * 3;
            double ww = w * (0.6 + 0.4 * Math.sin(time * 2 + yy));
            g.fill(new Rectangle2D.Double(x - ww / 2 + off, yy, ww, 2));
        }
    }

    // ---------- Street ----------
    void buildStreet(Graphics2D g) {
        g.setColor(new Color(236, 230, 215));   // calçada portuguesa: cream & basalt waves
        g.fillRect(0, HZ, W, H - HZ);
        g.setColor(new Color(55, 55, 60));
        g.setStroke(new BasicStroke(9f));
        for (int k = 0; k < 8; k++) {
            int by = HZ + 30 + k * 70;
            Path2D p = new Path2D.Double();
            p.moveTo(0, by);
            for (int x = 0; x <= W; x += 10) p.lineTo(x, by + Math.sin(x * 0.02) * 18);
            g.draw(p);
        }
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(0, 0, 0, 22));
        for (int y = HZ; y < H; y += 7)
            for (int x = (y / 7 % 2) * 3; x < W; x += 7) g.drawRect(x, y, 6, 6);
        g.setPaint(new GradientPaint(0, HZ, new Color(0, 0, 0, 0), 0, H, new Color(0, 0, 0, 80)));
        g.fillRect(0, HZ, W, H - HZ);

        // colourful band along the bottom
        int bandY = H - 70;
        Color[] band = { new Color(0, 110, 60), new Color(210, 30, 40) };
        for (int x = 0; x < W; x += 40) {
            g.setColor(band[(x / 40) % band.length]);
            g.fillRect(x, bandY, 40, 6);
        }
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, bandY + 6, W, 24);
        g.setColor(new Color(255, 240, 210, 170));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("LISBOA  ·  DERBY DE DEMOLIÇÃO  ·  LISBOA", 30, bandY + 22);
    }

    void drawLampGlow(Graphics2D g, StreetLamp L) {
        double pulse = 0.85 + 0.15 * Math.sin(time * 2 + L.x);
        Color lc = new Color(255, 210, 150);
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
        g.setColor(new Color(90, 90, 95));
        g.fillRect(x + 21, y + 44, 4, 16);
        g.setColor(new Color(245, 245, 240));        // azulejo tile
        g.fillRect(x, y, 46, 46);
        g.setColor(new Color(30, 70, 160));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(x + 2, y + 2, 42, 42);
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + time * 0.2;
            g.fill(new Ellipse2D.Double(x + 23 + Math.cos(a) * 9 - 5, y + 23 + Math.sin(a) * 9 - 5, 10, 10));
        }
        g.fill(new Arc2D.Double(x - 8, y - 8, 16, 16, 270, 90, Arc2D.PIE));
        g.fill(new Arc2D.Double(x + 38, y - 8, 16, 16, 180, 90, Arc2D.PIE));
        g.fill(new Arc2D.Double(x - 8, y + 38, 16, 16, 0, 90, Arc2D.PIE));
        g.fill(new Arc2D.Double(x + 38, y + 38, 16, 16, 90, 90, Arc2D.PIE));
        g.setColor(new Color(250, 200, 40));
        g.fill(new Ellipse2D.Double(x + 19, y + 19, 8, 8));
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

        drawHealthBar(g, 30, 28, p1, "J1  ·  Jogador 1");
        drawHealthBar(g, W-330, 28, p2, p2.isAI ? "CPU  ·  Computador" : "J2  ·  Jogador 2");

        g.setFont(fSmall);
        g.setColor(new Color(15, 15, 20, 150));
        g.fillRoundRect(20, H - 38, W - 40, 26, 10, 10);
        g.setColor(new Color(255,230,190,220));
        g.drawString("P1: WASD    P2: Arrows    R: Restart", 30, H - 20);
        String foot = "LISBOA — DERBY DE DEMOLIÇÃO";
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
        g.setColor(new Color(250, 200, 60));
        Font f = fBig;
        FontMetrics fm = g.getFontMetrics(f);
        while (fm.stringWidth(winnerText) > W - 80 && f.getSize() > 24) {
            f = f.deriveFont((float) (f.getSize() - 2));
            fm = g.getFontMetrics(f);
        }
        g.setFont(f);
        g.drawString(winnerText, (W - fm.stringWidth(winnerText))/2, H/2);
        g.setFont(fMed);
        String sub = "R — recomeçar  ·  Restart";
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
            JFrame f = new JFrame("Demolition Racing 11 — Lisbon Edition");
            DemolitionRacingGame11 panel = new DemolitionRacingGame11();
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
