// Cross-Country Racing Adventure - pure Java (Swing + Java2D, no JavaFX)
// Compile: javac CrossCountryRacing.java
// Run:     java CrossCountryRacing
//
// Menu:  1 = Player vs Computer   2 = Two Players
// P1 (red):  W = gas, S = brake, Left SHIFT = nitro
// P2 (blue): UP = gas, DOWN = brake, ENTER = nitro
// R = restart, ESC = menu

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class CrossCountryRacing extends JPanel implements ActionListener, KeyListener {

    static final int W = 1200, H = 760;
    static final double TRACK_LEN = 12000, GRAVITY = 900;

    enum State { MENU, COUNTDOWN, RACE, FINISHED }
    State state = State.MENU;
    boolean vsComputer = true;
    double countdown, raceTime;
    final Set<Integer> keys = new HashSet<>();
    final javax.swing.Timer timer = new javax.swing.Timer(16, this);
    long lastNano = System.nanoTime();
    final Random rnd = new Random();

    Car p1, p2;
    final List<Particle> particles = new ArrayList<>();
    final double[] treeX = new double[260], treeS = new double[260];
    final double[] cloudX = new double[14], cloudY = new double[14], cloudS = new double[14];
    String winner = "";

    // ---------- terrain ----------
    static double ground(double x) {
        if (x < 400) return 200;
        double t = x - 400;
        double ramp = Math.min(1, t / 800);
        return 200 + ramp * (70 * Math.sin(t * 0.004) + 45 * Math.sin(t * 0.011 + 1.3)
                + 22 * Math.sin(t * 0.027 + 0.7) + 120 * Math.sin(t * 0.0012));
    }
    static double slope(double x) { return (ground(x + 2) - ground(x - 2)) / 4; }

    // ---------- car ----------
    class Car {
        double x, y, vx, vy, angle, angVel, wheelRot, nitro = 100, susp, suspV;
        boolean onGround, ai, finished;
        double finishTime;
        Color body, trim;
        String name;
        Car(String n, Color b, Color t, boolean ai, double startX) {
            name = n; body = b; trim = t; this.ai = ai; x = startX; y = ground(startX);
        }

        void update(double dt, boolean gas, boolean brake, boolean boost) {
            if (finished) { gas = false; brake = true; boost = false; }
            if (ai && !finished) { // simple AI: throttle by upcoming slope, nitro on climbs
                double ahead = slope(x + 120);
                gas = !(onGround && vx > 520 && ahead < -0.5);
                brake = vx > 640 && ahead < -0.7;
                boost = nitro > 30 && ahead > 0.35 && vx < 420;
            }
            boolean nitroOn = boost && nitro > 0 && gas;
            if (nitroOn) { nitro -= 35 * dt; spawnFlame(); } else nitro = Math.min(100, nitro + 6 * dt);

            double g = ground(x);
            if (onGround) {
                double s = slope(x), ang = Math.atan(s);
                double acc = 0;
                if (gas) acc += nitroOn ? 620 : 360;
                if (brake) acc -= vx > 0 ? 700 : 200;
                acc -= GRAVITY * Math.sin(ang) * 0.9;
                acc -= vx * Math.abs(vx) * 0.0006 + vx * 0.25;
                vx += acc * Math.cos(ang) * dt;
                if (vx < -150) vx = -150;
                double newX = x + vx * dt;
                double newG = ground(newX);
                double prevVy = vy;
                vy = (newG - g) / dt;
                // launch off crests: ground falls away faster than gravity pulls us
                if (prevVy - GRAVITY * dt > vy + 40 && vx > 250) {
                    onGround = false; vy = prevVy;
                    y += vy * dt; x = newX;
                } else { x = newX; y = newG; }
                double target = ang;
                angVel = (target - angle) / dt * 0.4; angle += (target - angle) * Math.min(1, 14 * dt);
                if (Math.abs(vx) > 60 && rnd.nextDouble() < Math.abs(vx) / 600) spawnDust();
            } else {
                vy -= GRAVITY * dt;
                x += vx * dt; y += vy * dt;
                vx -= vx * 0.05 * dt;
                if (gas) angVel += 1.2 * dt; if (brake) angVel -= 1.2 * dt; // air control
                angle += angVel * dt * 0.6;
                if (y <= ground(x)) {
                    double impact = Math.max(0, -vy);
                    y = ground(x); onGround = true;
                    double ta = Math.atan(slope(x));
                    double diff = Math.abs(angle - ta);
                    if (diff > 0.9) vx *= 0.45; // bad landing
                    angle = ta; suspV -= impact * 0.06;
                    for (int i = 0; i < impact / 25; i++) spawnDust();
                }
            }
            // suspension spring
            suspV += (-susp * 180 - suspV * 12) * dt;
            susp += suspV * dt;
            susp = Math.max(-10, Math.min(8, susp));
            wheelRot += vx * dt / 18;
            if (!finished && x >= TRACK_LEN) { finished = true; finishTime = raceTime; }
        }

        void spawnDust() {
            double bx = x - 30 * Math.cos(angle), by = ground(bx);
            for (int i = 0; i < 2; i++)
                particles.add(new Particle(bx, by + 4, -vx * 0.15 + rnd.nextGaussian() * 40,
                        60 + rnd.nextDouble() * 90, 0.9 + rnd.nextDouble() * 0.6,
                        new Color(140 + rnd.nextInt(40), 110 + rnd.nextInt(30), 70), 6 + rnd.nextDouble() * 8, false));
        }
        void spawnFlame() {
            double ex = x - 48 * Math.cos(angle) + 22 * Math.sin(angle);
            double ey = y + 48 * -Math.sin(angle) + 22 * Math.cos(angle) + 8;
            particles.add(new Particle(ex, ey, -300 * Math.cos(angle) + rnd.nextGaussian() * 30,
                    -300 * Math.sin(angle) + rnd.nextGaussian() * 30, 0.25,
                    new Color(255, 120 + rnd.nextInt(120), 20), 7, true));
        }
    }

    static class Particle {
        double x, y, vx, vy, life, max, size; Color c; boolean glow;
        Particle(double x, double y, double vx, double vy, double life, Color c, double size, boolean glow) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = max = life; this.c = c; this.size = size; this.glow = glow;
        }
    }

    // ---------- setup ----------
    public CrossCountryRacing() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true); addKeyListener(this);
        for (int i = 0; i < treeX.length; i++) { treeX[i] = i * 55 + rnd.nextDouble() * 40; treeS[i] = 0.6 + rnd.nextDouble() * 0.8; }
        for (int i = 0; i < cloudX.length; i++) { cloudX[i] = rnd.nextDouble() * 3000; cloudY[i] = 30 + rnd.nextDouble() * 120; cloudS[i] = 0.5 + rnd.nextDouble(); }
        timer.start();
    }

    void startRace(boolean cpu) {
        vsComputer = cpu;
        p1 = new Car("RED", new Color(210, 40, 40), new Color(255, 200, 60), false, 250);
        p2 = new Car(cpu ? "CPU" : "BLUE", new Color(30, 90, 210), new Color(240, 240, 255), cpu, 150);
        particles.clear(); raceTime = 0; countdown = 3.5; winner = ""; state = State.COUNTDOWN;
    }

    @Override public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = Math.min(0.033, (now - lastNano) / 1e9); lastNano = now;
        for (int i = 0; i < cloudX.length; i++) cloudX[i] += 8 * cloudS[i] * dt;
        if (state == State.COUNTDOWN) { countdown -= dt; if (countdown <= 0.5) state = State.RACE; }
        if (state == State.RACE || state == State.FINISHED) {
            raceTime += dt;
            p1.update(dt, k(KeyEvent.VK_W), k(KeyEvent.VK_S), k(KeyEvent.VK_SHIFT));
            p2.update(dt, k(KeyEvent.VK_UP), k(KeyEvent.VK_DOWN), k(KeyEvent.VK_ENTER));
            if (state == State.RACE && (p1.finished || p2.finished)) {
                state = State.FINISHED;
                winner = (p1.finished && (!p2.finished || p1.finishTime <= p2.finishTime) ? p1.name : p2.name) + " WINS!";
            }
        }
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life -= dt; if (p.life <= 0) { it.remove(); continue; }
            p.x += p.vx * dt; p.y += p.vy * dt;
            if (!p.glow) { p.vy -= 120 * dt; p.vx *= 0.98; p.size += 10 * dt; }
        }
        repaint();
    }
    boolean k(int c) { return keys.contains(c); }

    // ---------- rendering ----------
    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (state == State.MENU) { drawWorld(g, 0, 0, W, H, 300 + System.currentTimeMillis() * 0.1 % 6000, null); drawMenu(g); return; }
        int half = H / 2;
        drawWorld(g, 0, 0, W, half, p1.x, p1);
        drawWorld(g, 0, half, W, half, p2.x, p2);
        g.setColor(new Color(20, 20, 20)); g.fillRect(0, half - 2, W, 4);
        drawHud(g, p1, 0); drawHud(g, p2, half);
        drawProgress(g);
        if (state == State.COUNTDOWN) bigText(g, countdown > 1.5 ? String.valueOf((int) countdown) : "GO!", H / 2 + 30, new Color(255, 220, 60));
        if (state == State.FINISHED) { bigText(g, winner, H / 2 + 30, Color.WHITE);
            smallCenter(g, "R = race again    ESC = menu", H / 2 + 80); }
    }

    void drawWorld(Graphics2D g, int ox, int oy, int w, int h, double camX, Car focus) {
        Shape oldClip = g.getClip();
        g.setClip(ox, oy, w, h);
        double camY = focus == null ? ground(camX) : focus.y * 0.7 + ground(camX) * 0.3;
        double scale = h / 520.0;
        // sky
        g.setPaint(new GradientPaint(0, oy, new Color(70, 140, 220), 0, oy + h, new Color(255, 205, 150)));
        g.fillRect(ox, oy, w, h);
        // sun
        g.setPaint(new RadialGradientPaint(new Point2D.Double(w * 0.8, oy + h * 0.22), (float) (90 * scale),
                new float[]{0f, 0.3f, 1f}, new Color[]{new Color(255, 250, 220), new Color(255, 230, 150, 180), new Color(255, 200, 120, 0)}));
        g.fillOval((int) (w * 0.8 - 90 * scale), (int) (oy + h * 0.22 - 90 * scale), (int) (180 * scale), (int) (180 * scale));
        // clouds
        for (int i = 0; i < cloudX.length; i++) {
            double cx = ((cloudX[i] - camX * 0.05) % 3000 + 3000) % 3000 - 300;
            double cy = oy + cloudY[i] * scale, s = cloudS[i] * scale;
            g.setColor(new Color(255, 255, 255, 190));
            for (int j = 0; j < 5; j++) g.fillOval((int) (cx + j * 28 * s), (int) (cy - Math.sin(j * 1.3) * 12 * s), (int) (60 * s), (int) (36 * s));
        }
        // parallax mountains
        drawLayer(g, ox, oy, w, h, camX, 0.15, new Color(120, 130, 170), 0.55, 140, 0.0021, scale);
        drawLayer(g, ox, oy, w, h, camX, 0.3, new Color(80, 120, 110), 0.68, 80, 0.0043, scale);
        drawLayer(g, ox, oy, w, h, camX, 0.5, new Color(55, 100, 60), 0.78, 40, 0.009, scale);

        AffineTransform at = g.getTransform();
        // world transform: world y up -> screen y down
        g.translate(ox + w * 0.33, oy + h * 0.62);
        g.scale(scale, scale);
        g.translate(-camX, camY);
        double left = camX - w * 0.33 / scale - 50, right = camX + w * 0.7 / scale + 50;

        // far trees (in world, behind track)
        for (int i = 0; i < treeX.length; i++) {
            double tx = treeX[i] * (TRACK_LEN / (treeX.length * 55.0));
            if (tx < left || tx > right) continue;
            drawTree(g, tx, ground(tx) + 6, treeS[i]);
        }
        // terrain
        Path2D dirt = new Path2D.Double(), grass = new Path2D.Double();
        dirt.moveTo(left, 2000); grass.moveTo(left, -ground(left));
        for (double x = left; x <= right; x += 8) { dirt.lineTo(x, -ground(x)); grass.lineTo(x, -ground(x)); }
        dirt.lineTo(right, 2000); dirt.closePath();
        g.setPaint(new GradientPaint(0, (float) -camY, new Color(120, 85, 50), 0, (float) (-camY + 400), new Color(60, 40, 25)));
        g.fill(dirt);
        g.setStroke(new BasicStroke(14, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(70, 140, 50)); g.draw(grass);
        g.setStroke(new BasicStroke(4)); g.setColor(new Color(110, 180, 70));
        g.draw(AffineTransform.getTranslateInstance(0, -5).createTransformedShape(grass));
        // rocks / stones texture
        for (double x = Math.floor(left / 37) * 37; x < right; x += 37) {
            double hsh = Math.abs(Math.sin(x * 12.9898) * 43758.5453) % 1;
            g.setColor(new Color(90, 65, 40));
            g.fillOval((int) x, (int) (-ground(x) + 15 + hsh * 50), (int) (4 + hsh * 8), (int) (3 + hsh * 5));
        }
        // start & finish lines
        drawFlagLine(g, 320, "START"); drawFlagLine(g, TRACK_LEN, "FINISH");
        // distance markers
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        for (int m = 1000; m < TRACK_LEN; m += 1000) {
            if (m < left || m > right) continue;
            double gy = -ground(m);
            g.setColor(new Color(240, 240, 240)); g.fillRect(m - 2, (int) gy - 50, 4, 50);
            g.setColor(new Color(230, 120, 30)); g.fillRect(m - 22, (int) gy - 70, 44, 22);
            g.setColor(Color.WHITE); g.drawString((m / 100) + "m", m - 15, (int) gy - 54);
        }
        // particles behind
        for (Particle p : particles) {
            float a = (float) Math.max(0, p.life / p.max);
            if (p.glow) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            else g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.55f));
            g.setColor(p.c);
            g.fill(new Ellipse2D.Double(p.x - p.size / 2, -p.y - p.size / 2, p.size, p.size));
        }
        g.setComposite(AlphaComposite.SrcOver);
        if (p1 != null && state != State.MENU) { drawCar(g, focus == p1 ? p2 : p1, true); drawCar(g, focus, false); }
        g.setTransform(at);
        g.setClip(oldClip);
    }

    void drawLayer(Graphics2D g, int ox, int oy, int w, int h, double camX, double par, Color c, double base, double amp, double freq, double scale) {
        Path2D p = new Path2D.Double();
        p.moveTo(ox, oy + h);
        for (int sx = 0; sx <= w; sx += 6) {
            double wx = sx / scale + camX * par;
            double y = oy + h * base - amp * scale * (0.6 * Math.sin(wx * freq) + 0.3 * Math.sin(wx * freq * 2.7 + 1) + 0.1 * Math.sin(wx * freq * 7.1));
            p.lineTo(ox + sx, y);
        }
        p.lineTo(ox + w, oy + h); p.closePath();
        g.setPaint(new GradientPaint(0, (float) (oy + h * base - amp * scale), c.brighter(), 0, oy + h, c.darker()));
        g.fill(p);
    }

    void drawTree(Graphics2D g, double x, double gy, double s) {
        int bx = (int) x, by = (int) -gy;
        g.setColor(new Color(80, 50, 30)); g.fillRect(bx - (int) (4 * s), by - (int) (30 * s), (int) (8 * s), (int) (30 * s));
        for (int i = 0; i < 3; i++) {
            int tw = (int) ((46 - i * 10) * s), th = (int) (40 * s);
            int ty = by - (int) ((28 + i * 22) * s);
            g.setColor(new Color(30 + i * 15, 95 + i * 15, 45 + i * 8));
            g.fillPolygon(new int[]{bx - tw / 2, bx + tw / 2, bx}, new int[]{ty, ty, ty - th}, 3);
        }
    }

    void drawFlagLine(Graphics2D g, double x, String label) {
        double gy = -ground(x);
        g.setColor(Color.DARK_GRAY); g.fillRect((int) x - 70, (int) gy - 130, 6, 130); g.fillRect((int) x + 64, (int) gy - 130, 6, 130);
        for (int i = 0; i < 14; i++) for (int j = 0; j < 2; j++) {
            g.setColor((i + j) % 2 == 0 ? Color.BLACK : Color.WHITE);
            g.fillRect((int) x - 70 + i * 10, (int) gy - 130 + j * 10, 10, 10);
        }
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(label, (int) x - 30, (int) gy - 140);
    }

    void drawCar(Graphics2D g, Car c, boolean ghost) {
        if (c == null) return;
        AffineTransform at = g.getTransform();
        // shadow
        double gy = -ground(c.x), dist = Math.max(0, c.y - ground(c.x));
        g.setColor(new Color(0, 0, 0, (int) Math.max(20, 90 - dist * 0.4)));
        g.fill(new Ellipse2D.Double(c.x - 50, gy - 6, 100, 12));
        g.translate(c.x, -c.y);
        g.rotate(-c.angle);
        if (ghost) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        double wr = 15, susp = c.susp;
        // suspension struts
        g.setStroke(new BasicStroke(4)); g.setColor(new Color(70, 70, 70));
        g.draw(new Line2D.Double(-32, -wr, -28, -wr - 18 + susp)); g.draw(new Line2D.Double(32, -wr, 28, -wr - 18 + susp));
        // body (lifted by suspension)
        g.translate(0, susp);
        Path2D body = new Path2D.Double();
        body.moveTo(-52, -22); body.lineTo(-50, -40); body.lineTo(-22, -44); body.lineTo(-10, -62);
        body.lineTo(22, -62); body.lineTo(36, -44); body.lineTo(54, -40); body.lineTo(56, -24); body.closePath();
        g.setPaint(new GradientPaint(0, -62, c.body.brighter(), 0, -20, c.body.darker()));
        g.fill(body);
        g.setStroke(new BasicStroke(2)); g.setColor(c.body.darker().darker()); g.draw(body);
        // windows
        Path2D win = new Path2D.Double();
        win.moveTo(-17, -45); win.lineTo(-7, -58); win.lineTo(19, -58); win.lineTo(30, -45); win.closePath();
        g.setPaint(new GradientPaint(-10, -58, new Color(200, 230, 255), 25, -45, new Color(60, 90, 130)));
        g.fill(win);
        g.setColor(c.body.darker()); g.fillRect(4, -58, 3, 13);
        // stripe, lights, roll bar, exhaust
        g.setColor(c.trim); g.fillRect(-50, -36, 104, 5);
        g.setColor(new Color(255, 250, 200)); g.fillOval(48, -40, 8, 7);
        g.setColor(new Color(200, 20, 20)); g.fillRect(-53, -38, 4, 7);
        g.setColor(new Color(40, 40, 40)); g.fillRect(-56, -28, 8, 4);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 11)); g.drawString(c.name, -12, -24);
        g.translate(0, -susp);
        // wheels
        drawWheel(g, -32, -wr, wr, c.wheelRot);
        drawWheel(g, 32, -wr, wr, c.wheelRot);
        g.setComposite(AlphaComposite.SrcOver);
        g.setTransform(at);
    }

    void drawWheel(Graphics2D g, double x, double y, double r, double rot) {
        g.setColor(new Color(25, 25, 25)); g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        g.setColor(new Color(50, 50, 50));
        for (int i = 0; i < 12; i++) { double a = -rot + i * Math.PI / 6;
            g.fill(new Ellipse2D.Double(x + Math.cos(a) * r * 0.88 - 2, y + Math.sin(a) * r * 0.88 - 2, 4, 4)); }
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) (r * 0.55), new float[]{0, 1}, new Color[]{new Color(230, 230, 230), new Color(130, 130, 130)}));
        g.fill(new Ellipse2D.Double(x - r * 0.55, y - r * 0.55, r * 1.1, r * 1.1));
        g.setColor(new Color(70, 70, 70)); g.setStroke(new BasicStroke(2));
        for (int i = 0; i < 5; i++) { double a = -rot + i * Math.PI * 2 / 5;
            g.draw(new Line2D.Double(x, y, x + Math.cos(a) * r * 0.5, y + Math.sin(a) * r * 0.5)); }
    }

    void drawHud(Graphics2D g, Car c, int oy) {
        g.setColor(new Color(0, 0, 0, 120)); g.fillRoundRect(12, oy + 12, 250, 74, 14, 14);
        g.setColor(c.body.brighter()); g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(c.name + (c.ai ? " (AI)" : ""), 24, oy + 36);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.drawString(String.format("%3d km/h", (int) Math.abs(c.vx * 0.25)), 150, oy + 36);
        g.drawString("NITRO", 24, oy + 64);
        g.setColor(new Color(60, 60, 60)); g.fillRoundRect(80, oy + 52, 168, 14, 7, 7);
        g.setPaint(new GradientPaint(80, 0, new Color(255, 120, 0), 248, 0, new Color(255, 230, 60)));
        g.fillRoundRect(80, oy + 52, (int) (168 * c.nitro / 100), 14, 7, 7);
        g.setColor(Color.WHITE);
        String t = c.finished ? String.format("FINISHED %.2fs", c.finishTime) : String.format("%.2fs", raceTime);
        g.drawString(t, W - 170, oy + 34);
    }

    void drawProgress(Graphics2D g) {
        int x0 = W / 2 - 250, y = H / 2 - 8;
        g.setColor(new Color(0, 0, 0, 150)); g.fillRoundRect(x0 - 10, y - 8, 520, 16, 8, 8);
        for (Car c : new Car[]{p1, p2}) {
            int px = x0 + (int) (500 * Math.min(1, Math.max(0, c.x / TRACK_LEN)));
            g.setColor(c.body.brighter()); g.fillOval(px - 6, y - 6, 12, 12);
        }
    }

    void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 140)); g.fillRoundRect(W / 2 - 330, 140, 660, 430, 30, 30);
        bigText(g, "CROSS-COUNTRY", 230, new Color(255, 200, 60));
        bigText(g, "RACING ADVENTURE", 300, Color.WHITE);
        smallCenter(g, "Press 1  -  Player vs Computer", 380);
        smallCenter(g, "Press 2  -  Two Players (split screen)", 420);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16)); g.setColor(new Color(220, 220, 220));
        center(g, "P1: W gas  S brake  L-SHIFT nitro     P2: UP gas  DOWN brake  ENTER nitro", 490);
        center(g, "Tip: in the air, gas/brake tilts the car - land flat to keep speed!", 520);
    }

    void bigText(Graphics2D g, String s, int y, Color c) {
        g.setFont(new Font("SansSerif", Font.BOLD, 60));
        g.setColor(new Color(0, 0, 0, 160)); center(g, s, y + 4, 4);
        g.setColor(c); center(g, s, y);
    }
    void smallCenter(Graphics2D g, String s, int y) { g.setFont(new Font("SansSerif", Font.BOLD, 24)); g.setColor(Color.WHITE); center(g, s, y); }
    void center(Graphics2D g, String s, int y) { center(g, s, y, 0); }
    void center(Graphics2D g, String s, int y, int dx) {
        int w = g.getFontMetrics().stringWidth(s); g.drawString(s, (W - w) / 2 + dx, y);
    }

    // ---------- input ----------
    @Override public void keyPressed(KeyEvent e) {
        keys.add(e.getKeyCode());
        if (state == State.MENU) {
            if (e.getKeyCode() == KeyEvent.VK_1) startRace(true);
            if (e.getKeyCode() == KeyEvent.VK_2) startRace(false);
        } else {
            if (e.getKeyCode() == KeyEvent.VK_R) startRace(vsComputer);
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) state = State.MENU;
        }
    }
    @Override public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "true");
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Cross-Country Racing Adventure");
            CrossCountryRacing game = new CrossCountryRacing();
            f.add(game); f.pack(); f.setResizable(false);
            f.setLocationRelativeTo(null); f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setVisible(true); game.requestFocusInWindow();
        });
    }
}
