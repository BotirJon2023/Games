// DemolitionRacingGame3.java  — pure Java, no external libraries
// Compile: javac DemolitionRacingGame3.java
// Run:     java DemolitionRacingGame3
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class DemolitionRacingGame3 extends JPanel implements ActionListener, KeyListener {

    static final int W = 1280, H = 720;
    static final double FRICTION = 0.92, TURN = 0.055, ACCEL = 0.32;

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

    static class RainDrop {
        double x, y, vy, len;
        RainDrop(double x,double y,double vy,double len){this.x=x;this.y=y;this.vy=vy;this.len=len;}
    }

    // ================= STATE =================
    final List<Car> cars = new ArrayList<>();
    final List<Particle> particles = new ArrayList<>();
    final List<SkidMark> skids = new ArrayList<>();
    final List<StreetLamp> lamps = new ArrayList<>();
    final List<RainDrop> rain = new ArrayList<>();
    final Set<Integer> keys = new HashSet<>();
    Car p1, p2;
    double camShake = 0, time = 0;
    boolean gameOver = false;
    String winnerText = "";
    Font fBig, fMed, fSmall;
    Random rng = new Random(20241119);

    public DemolitionRacingGame3() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        fBig   = new Font("Serif", Font.BOLD, 66);
        fMed   = new Font("Serif", Font.BOLD, 20);
        fSmall = new Font("SansSerif", Font.BOLD, 14);

        initGame();

        // Berlin street lamps along top & bottom
        for (int i = 0; i < 10; i++) {
            lamps.add(new StreetLamp(80 + i * 130, 70));
        }
        for (int i = 0; i < 10; i++) {
            lamps.add(new StreetLamp(80 + i * 130, H - 70));
        }
        // Berlin rain (drizzle)
        for (int i = 0; i < 180; i++) {
            rain.add(new RainDrop(rng.nextDouble()*W, rng.nextDouble()*H,
                    500 + rng.nextDouble()*300, 8 + rng.nextDouble()*14));
        }

        new javax.swing.Timer(16, this).start();
    }

    void initGame() {
        cars.clear(); particles.clear(); skids.clear();
        gameOver = false; winnerText = ""; camShake = 0;

        // German flag palette — 黑红金
        Color black = new Color(25, 25, 30);
        Color red   = new Color(200, 30, 40);
        Color gold  = new Color(230, 180, 50);

        p1 = new Car(400, 400, red, gold, false, "P1");
        // Set isAI = false for local 2-player
        p2 = new Car(880, 400, new Color(30, 90, 160), gold, true, "CPU");

        cars.add(p1); cars.add(p2);

        Color[] pal = { gold, black, new Color(150,40,140),
                new Color(40,150,120), new Color(230,120,60) };
        for (int i = 0; i < 3; i++) {
            cars.add(new Car(200 + rng.nextDouble()*880, 150 + rng.nextDouble()*420,
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
        if (gameOver) return;

        // Rain update
        for (RainDrop r : rain) {
            r.y += r.vy * dt;
            r.x -= 60 * dt; // wind
            if (r.y > H) { r.y = -20; r.x = rng.nextDouble()*W; }
            if (r.x < 0) r.x += W;
        }

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
            if (c.x < 30)     { c.x = 30;    c.vx = Math.abs(c.vx)*0.5; camShake = 8; }
            if (c.x > W-30)   { c.x = W-30;  c.vx = -Math.abs(c.vx)*0.5; camShake = 8; }
            if (c.y < 120)    { c.y = 120;   c.vy = Math.abs(c.vy)*0.5; camShake = 8; }
            if (c.y > H-100)  { c.y = H-100; c.vy = -Math.abs(c.vy)*0.5; camShake = 8; }
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
            winnerText = w == null ? "TOTAL DEMOLITION!"
                    : w == p1 ? "SPIELER 1 GEWINNT — P1 WINS!"
                    : w == p2 ? (p2.isAI ? "CPU GEWINNT — CPU WINS!" : "SPIELER 2 GEWINNT — P2 WINS!")
                    : "GEGNER GEWINNT — AI WINS!";
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
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double shX = (Math.random()-0.5) * camShake;
        double shY = (Math.random()-0.5) * camShake;

        // Sky + city skyline (static, doesn't shake)
        drawBerlinSkyline(g);

        g.translate(shX, shY);

        // Street surface
        drawStreet(g);

        // Lamps glow (before cars so cars get lit)
        for (StreetLamp L : lamps) drawLampGlow(g, L);

        // U-Bahn entrance signs
        drawUBahnSign(g, 120, 140);
        drawUBahnSign(g, W-220, 140);

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

        // Rain on top
        drawRain(g);

        g.translate(-shX, -shY);

        // HUD
        drawHUD(g);
        if (gameOver) drawGameOver(g);
    }

    // ---------- Berlin skyline (night) ----------
    void drawBerlinSkyline(Graphics2D g) {
        // Sky gradient — dusk blue → warm
        GradientPaint sky = new GradientPaint(0, 0, new Color(12, 22, 44),
                0, 200, new Color(40, 55, 90));
        g.setPaint(sky);
        g.fillRect(0, 0, W, 200);

        // Stars
        for (int i = 0; i < 80; i++) {
            int sx = (int)((i * 137 + 41) % W);
            int sy = (i * 53) % 180;
            int a = 100 + (int)(80 * Math.sin(time * 2 + i));
            g.setColor(new Color(255, 240, 210, Math.max(30, a)));
            g.fillRect(sx, sy, 2, 2);
        }

        // Moon
        g.setColor(new Color(255, 245, 220, 220));
        g.fill(new Ellipse2D.Double(W - 140, 30, 60, 60));
        g.setColor(new Color(200, 190, 160, 120));
        g.fill(new Ellipse2D.Double(W - 122, 44, 20, 16));

        // Distant skyline silhouettes
        g.setColor(new Color(18, 22, 38));
        int[] heights = {40, 65, 50, 80, 55, 70, 90, 60, 45, 75, 55, 85, 50, 70, 60};
        int step = W / heights.length;
        for (int i = 0; i < heights.length; i++) {
            int bx = i * step;
            int bh = heights[i];
            g.fillRect(bx, 200 - bh, step - 4, bh);
            // window dots
            g.setColor(new Color(255, 200, 120, 90));
            for (int wy = 200 - bh + 8; wy < 195; wy += 12)
                for (int wx = bx + 6; wx < bx + step - 10; wx += 12)
                    if ((wx + wy) % 3 == 0)
                        g.fillRect(wx, wy, 3, 4);
            g.setColor(new Color(18, 22, 38));
        }

        // Brandenburg Gate silhouette (center-left)
        drawBrandenburgGate(g, 250, 200);

        // TV Tower (Fernsehturm) — right side, tall with spherical observation deck
        drawTVTower(g, W - 320, 200);
    }

    void drawBrandenburgGate(Graphics2D g, int baseX, int baseY) {
        int gateW = 420, gateH = 90;
        int y = baseY - gateH;
        g.setColor(new Color(30, 34, 48));
        // main body
        g.fillRect(baseX, y + 30, gateW, gateH - 30);
        // roof
        g.fillRect(baseX - 12, y + 22, gateW + 24, 10);
        // columns
        for (int i = 0; i < 6; i++) {
            int cx = baseX + 20 + i * (gateW - 40) / 5;
            g.fillRect(cx, y + 40, 14, gateH - 40);
        }
        // Quadriga hint (tiny horse shape)
        g.setColor(new Color(30, 34, 48));
        g.fillOval(baseX + gateW/2 - 20, y - 4, 40, 22);
        g.setColor(new Color(230, 190, 90, 100));
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(baseX + gateW/2 - 20, y - 4, 40, 22);
        // warm lights between columns
        g.setColor(new Color(255, 200, 110, 140));
        for (int i = 0; i < 5; i++) {
            int cx = baseX + 38 + i * (gateW - 40) / 5;
            g.fillOval(cx - 3, y + 55, 6, 6);
        }
    }

    void drawTVTower(Graphics2D g, int baseX, int baseY) {
        // Thin steel shaft
        g.setColor(new Color(30, 34, 48));
        g.fillRect(baseX - 4, 40, 8, baseY - 40);
        // Observation sphere
        g.fill(new Ellipse2D.Double(baseX - 26, 60, 52, 44));
        // Window band
        g.setColor(new Color(255, 200, 120, 160));
        g.fill(new Ellipse2D.Double(baseX - 22, 76, 44, 8));
        // Antenna
        g.setColor(new Color(30, 34, 48));
        g.fillRect(baseX - 1, 10, 2, 55);
        // Blinking beacon
        double blink = (Math.sin(time * 3) + 1) / 2;
        int a = (int)(blink * 255);
        g.setColor(new Color(255, 40, 40, a));
        g.fill(new Ellipse2D.Double(baseX - 6, 4, 12, 12));
        g.setColor(new Color(255, 80, 80, a / 3));
        g.fill(new Ellipse2D.Double(baseX - 16, -6, 32, 32));
    }

    // ---------- Street ----------
    void drawStreet(Graphics2D g) {
        GradientPaint gp = new GradientPaint(0, 120, new Color(38, 38, 42),
                0, H, new Color(18, 18, 22));
        g.setPaint(gp);
        g.fillRect(0, 120, W, H - 120);

        // Asphalt texture speckles
        Random tr = new Random(42);
        g.setColor(new Color(255,255,255,10));
        for (int i = 0; i < 400; i++) {
            int sx = tr.nextInt(W), sy = 120 + tr.nextInt(H-120);
            g.fillRect(sx, sy, 2, 2);
        }

        // Faded lane markings
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{30f, 30f}, 0f));
        g.setColor(new Color(255, 220, 120, 90));
        g.drawLine(0, H/2 + 40, W, H/2 + 40);

        // Berlin wall mural hint — a colorful band along the bottom
        int bandY = H - 70;
        for (int x = 0; x < W; x += 40) {
            Color[] mural = { new Color(220,40,40), new Color(240,180,40),
                    new Color(40,80,160), new Color(40,160,80) };
            g.setColor(mural[(x/40) % mural.length]);
            g.fillRect(x, bandY, 40, 6);
        }
        g.setColor(new Color(0,0,0,120));
        g.fillRect(0, bandY + 6, W, 24);
        g.setColor(new Color(255,240,210,150));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("BERLIN  ·  DEMOLITION DERBY  ·  BERLIN", 30, bandY + 22);
    }

    void drawLampGlow(Graphics2D g, StreetLamp L) {
        double pulse = 0.85 + 0.15 * Math.sin(time * 2 + L.x);
        for (int i = 4; i >= 1; i--) {
            double rr = 40 * i * pulse;
            int a = (int)(20 * pulse / i);
            g.setColor(new Color(255, 220, 140, a));
            g.fill(new Ellipse2D.Double(L.x - rr, L.y - rr * 0.6, rr*2, rr*1.2));
        }
        // lamp head
        g.setColor(new Color(60, 60, 70));
        g.fillRect((int)L.x - 2, (int)L.y - 30, 4, 30);
        g.fillRect((int)L.x - 12, (int)L.y - 32, 24, 6);
        g.setColor(new Color(255, 240, 180));
        g.fill(new Ellipse2D.Double(L.x - 6, L.y - 26, 12, 8));
    }

    void drawUBahnSign(Graphics2D g, int x, int y) {
        // U-Bahn sign — white "U" on blue square
        g.setColor(new Color(20, 60, 160));
        g.fillRect(x, y, 46, 46);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 40));
        g.drawString("U", x + 12, y + 36);
        g.setColor(new Color(20, 60, 160));
        g.fillRect(x + 20, y + 46, 6, 12);
        // light halo
        g.setColor(new Color(100, 160, 255, 50));
        g.fillOval(x - 20, y - 20, 86, 86);
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

    // ---------- Rain ----------
    void drawRain(Graphics2D g) {
        g.setStroke(new BasicStroke(1f));
        for (RainDrop r : rain) {
            g.setColor(new Color(180, 200, 255, 70));
            g.draw(new Line2D.Double(r.x, r.y, r.x + 2, r.y + r.len));
        }
    }

    // ---------- HUD ----------
    void drawHUD(Graphics2D g) {
        g.setColor(new Color(15, 15, 20, 170));
        g.fillRoundRect(20, 20, 320, 56, 14, 14);
        g.fillRoundRect(W-340, 20, 320, 56, 14, 14);

        drawHealthBar(g, 30, 28, p1, "P1  ·  Spieler 1", new Color(200,30,40));
        drawHealthBar(g, W-330, 28, p2,
                p2.isAI ? "CPU  ·  Computer" : "P2  ·  Spieler 2",
                new Color(30,90,160));

        g.setFont(fSmall);
        g.setColor(new Color(255,220,180,180));
        g.drawString("P1: WASD    P2: Arrows    R: Restart", 30, H - 20);
        g.drawString("BERLIN — DEMOLITION DERBY", W - 280, H - 20);
    }

    void drawHealthBar(Graphics2D g, int x, int y, Car c, String label, Color cc) {
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
        g.setColor(new Color(240, 200, 90));
        g.setFont(fBig);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(winnerText, (W - fm.stringWidth(winnerText))/2, H/2);
        g.setFont(fMed);
        String sub = "R — neu starten  ·  Restart";
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
            JFrame f = new JFrame("Demolition Racing 3 — Berlin Edition");
            DemolitionRacingGame3 panel = new DemolitionRacingGame3();
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