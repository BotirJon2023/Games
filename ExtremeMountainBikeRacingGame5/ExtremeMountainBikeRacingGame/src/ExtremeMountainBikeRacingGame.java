import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;

public class ExtremeMountainBikeRacingGame extends JPanel implements ActionListener, KeyListener {

    static final int W = 1280, H = 720;
    static final double G_ACC = 1500.0;      // gravity px/s^2
    static final double FINISH_X = 16000.0;  // race length
    static final double TOP_SPEED = 1150.0;  // px/s
    static final double WHEEL_R = 20.0;
    static final double RIDE_H = 24.0;       // bike centre height above ground

    JFrame frame;
    Timer timer;
    double t = 0, last;
    double camX = 400, camY = 0;
    int state = 0;                          // 0 menu, 1 countdown, 2 racing, 3 finished
    double countdown = 0, raceTime = 0;
    boolean twoPlayers = false;
    Bike p1, p2;
    final ArrayList<Particle> parts = new ArrayList<>();
    final Cloud[] clouds = new Cloud[10];
    final Bird[] birds = new Bird[6];
    final boolean[] keys = new boolean[65536];

    public static void main(String[] a) {
        SwingUtilities.invokeLater(ExtremeMountainBikeRacingGame::new);
    }

    ExtremeMountainBikeRacingGame() {
        frame = new JFrame("Extreme Mountain Bike Racing");
        setPreferredSize(new Dimension(W, H));
        frame.setContentPane(this);
        frame.pack();
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.addKeyListener(this);
        for (int i = 0; i < clouds.length; i++) clouds[i] = new Cloud(i);
        for (int i = 0; i < birds.length; i++) birds[i] = new Bird(i);
        timer = new Timer(16, this);
        last = System.nanoTime() / 1e9;
        timer.start();
    }

    // ------------------------------------------------------------ helpers
    double toSX(double wx) { return wx - camX + W / 2; }
    double toSY(double wy) { return H * 0.62 - (wy - camY); }

    double groundY(double x) {
        return 110 * Math.sin(x * 0.00085 + 0.9)
                + 55 * Math.sin(x * 0.0021 + 3.7)
                + 26 * Math.sin(x * 0.0053 + 1.2)
                + 9 * Math.sin(x * 0.014 + 5.0)
                + 3.5 * Math.sin(x * 0.037 + 2.0);
    }

    double slopeAt(double x) { return (groundY(x + 2) - groundY(x - 2)) / 4.0; }
    double farH(double x)  { return Math.max(30, 150 + 150 * Math.sin(x * 0.0011 + 2.2) + 90 * Math.sin(x * 0.0027 + 0.8) + 45 * Math.sin(x * 0.006 + 4.0)); }
    double midH(double x)  { return Math.max(15, 60 + 70 * Math.sin(x * 0.0016 + 1.0) + 40 * Math.sin(x * 0.0042 + 3.3) + 18 * Math.sin(x * 0.011 + 0.4)); }

    static double normAngle(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }

    static double hash(int n) {
        double v = Math.sin(n * 127.1 + 311.7) * 43758.5453;
        return v - Math.floor(v);
    }

    String fmtTime(double s) {
        int m = (int) (s / 60);
        return String.format("%02d:%04.1f", m, s - m * 60);
    }

    void txt(Graphics2D g, String s, int x, int y, Font f, Color c) {
        g.setFont(f);
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(s, x + 2, y + 2);
        g.setColor(c);
        g.drawString(s, x, y);
    }

    // ------------------------------------------------------------ bike
    class Bike {
        String name; Color color; boolean ai;
        double x, y, angle, speed, vx, vy;
        boolean grounded = true;
        double wheelRot, crank, sus, boost = 100;
        boolean throttle, brake, boostKey;
        double lean;
        double crashT = 0, finishTime = 0, lastSafeX;
        boolean finished = false;

        Bike(String n, Color c, boolean ai, double sx) {
            name = n; color = c; this.ai = ai;
            x = sx; y = groundY(sx) + RIDE_H;
            angle = Math.atan(slopeAt(sx));
            lastSafeX = sx;
        }

        /** Simple but competent AI: reads the terrain ahead, eases off on steep
         *  drops, boosts on the flat and always lands on both wheels. */
        void aiControl() {
            double sNow = slopeAt(x);
            double sAhead = slopeAt(x + 140);
            brake = false; throttle = true;
            if ((sAhead < -0.45 && speed > 520) || (sNow < -0.55 && speed > 560)) {
                throttle = false; brake = sNow < -0.55;
            }
            if (!grounded) {
                double target = Math.atan(slopeAt(x + vx * 0.35));
                lean = Math.max(-1, Math.min(1, normAngle(target - angle) * 2.4));
            } else lean = 0;
            boostKey = speed < 430 && boost > 30 && sNow > -0.2;
        }

        void crash() {
            crashT = 1.4; speed = 0; vx = vy = 0;
            for (int i = 0; i < 24; i++)
                parts.add(new Particle(x, y, (Math.random() - 0.5) * 420, Math.random() * 320 + 60,
                        0.8 + Math.random() * 0.6, i % 3 == 0 ? color : new Color(120, 82, 48),
                        3 + Math.random() * 4, 1200));
        }

        void respawn() {
            x = Math.max(120, lastSafeX - 170);
            y = groundY(x) + RIDE_H;
            angle = Math.atan(slopeAt(x));
            grounded = true; speed = 0; crashT = 0; boost = 60;
        }

        void spawnDirt(double power) {
            double back = angle + Math.PI;
            for (int i = 0; i < 2; i++)
                parts.add(new Particle(
                        x + Math.cos(back) * 26, y - 8 + Math.sin(back) * 26,
                        Math.cos(back) * (speed * 0.25 + Math.random() * 120) + (Math.random() - 0.5) * 90,
                        60 + Math.random() * 140 * Math.min(2, 0.5 + power * 0.02),
                        0.4 + Math.random() * 0.5,
                        new Color(115 + (int) (Math.random() * 45), 82, 48),
                        2.5 + Math.random() * 3.5, 1500));
        }

        void spawnFlame() {
            double back = angle + Math.PI;
            for (int i = 0; i < 2; i++)
                parts.add(new Particle(x + Math.cos(back) * 30, y - 6 + Math.sin(back) * 30,
                        Math.cos(back) * (200 + Math.random() * 150),
                        Math.sin(back) * 120 + Math.random() * 60,
                        0.25 + Math.random() * 0.2,
                        Math.random() < 0.5 ? new Color(255, 220, 90) : new Color(255, 120, 30),
                        5 + Math.random() * 6, -300));
        }

        void spawnConfetti() {
            Color[] cols = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.MAGENTA};
            for (int i = 0; i < 40; i++)
                parts.add(new Particle(x, y + 20, (Math.random() - 0.5) * 500,
                        150 + Math.random() * 350, 1.5 + Math.random(),
                        cols[i % cols.length], 4 + Math.random() * 4, 600));
        }

        void update(double dt) {
            if (finished) { throttle = false; boostKey = false; }
            if (crashT > 0) {
                crashT -= dt;
                if (crashT <= 0) respawn();
                return;
            }
            sus *= Math.pow(0.01, dt);   // suspension rebounds

            if (grounded) {
                double a = Math.atan(slopeAt(x));
                double accel = 0;
                if (throttle) accel += ai ? 505 : 620;
                if (brake) accel -= speed > 15 ? 950 : 260;
                accel += -G_ACC * Math.sin(a);           // gravity along slope
                accel -= speed * (ai ? 0.34 : 0.28);     // rolling resistance
                if (boostKey && boost > 0 && !finished) {
                    accel += 820;
                    boost = Math.max(0, boost - 40 * dt);
                    if (Math.random() < 0.6) spawnFlame();
                } else boost = Math.min(100, boost + 13 * dt);

                speed += accel * dt;
                if (speed < 0) speed = 0;
                double maxS = ai ? 980 : TOP_SPEED;
                if (speed > maxS) speed = Math.max(maxS, speed - 600 * dt);

                x += Math.cos(a) * speed * dt;
                y = groundY(x) + RIDE_H;
                angle += normAngle(a - angle) * Math.min(1, dt * 8);
                wheelRot += speed * dt / WHEEL_R;
                crank += (throttle ? 6 + speed * 0.03 : speed * 0.015) * dt;

                // terrain falls away -> natural take-off
                double la = Math.atan(slopeAt(x + 5));
                if (la < a - 0.055 && speed > 150) {
                    grounded = false;
                    vx = Math.cos(angle) * speed;
                    vy = Math.sin(angle) * speed;
                }
                if (Math.abs(normAngle(angle - a)) > 1.35 && speed > 60) crash();
                if (speed < 900) lastSafeX = x;
            } else {
                // airborne: full gravity + air control
                vy -= G_ACC * dt;
                angle += lean * 2.7 * dt;
                vx += lean * 55 * dt;
                x += vx * dt;
                y += vy * dt;
                wheelRot += (vx * Math.cos(angle) + vy * Math.sin(angle)) * dt / WHEEL_R;
                crank += 2 * dt;

                double gg = groundY(x);
                if (y <= gg + RIDE_H) {                  // landing
                    y = gg + RIDE_H;
                    double a = Math.atan(slopeAt(x));
                    double diff = normAngle(angle - a);
                    double impact = -vy;
                    if (Math.abs(diff) > 1.15 && impact > 230) crash();
                    else {
                        grounded = true;
                        speed = vx * Math.cos(a) + vy * Math.sin(a);
                        if (speed < 0) speed = 0;
                        angle = a + diff * 0.3;
                        sus = Math.min(9, impact * 0.016);   // suspension compresses
                        for (int i = 0; i < 8; i++) spawnDirt(impact * 0.12);
                    }
                }
            }

            if (!finished && x >= FINISH_X) {
                finished = true; finishTime = raceTime;
                for (int i = 0; i < 3; i++) spawnConfetti();
            }
            if (grounded && speed > 430 && Math.random() < 0.4) spawnDirt(2);
        }
    }

    // ------------------------------------------------------------ particles / scenery
    class Particle {
        double x, y, vx, vy, life, maxLife, size, grav;
        Color c;
        Particle(double x, double y, double vx, double vy, double life, Color c, double size, double grav) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.life = life; this.maxLife = life; this.c = c; this.size = size; this.grav = grav;
        }
        boolean update(double dt) {
            vy -= grav * dt; x += vx * dt; y += vy * dt; life -= dt;
            return life > 0;
        }
        void draw(Graphics2D g) {
            float a = (float) Math.max(0, life / maxLife);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (a * 220)));
            double sx = toSX(x), sy = toSY(y);
            g.fill(new Ellipse2D.Double(sx - size / 2, sy - size / 2, size, size));
        }
    }

    class Cloud {
        double x, y, s;
        Cloud(int i) { x = i * 430 + hash(i) * 200; y = 40 + hash(i * 11) * 170; s = 0.6 + hash(i * 7) * 0.9; }
        void draw(Graphics2D g) {
            double wrap = W + 500;
            double sx = (((x + t * 12 * s - camX * 0.12) % wrap) + wrap) % wrap - 250;
            double yy = y + Math.sin(t * 0.5 + x) * 4;
            g.setColor(new Color(255, 255, 255, 165));
            g.fill(new Ellipse2D.Double(sx, yy, 90 * s, 30 * s));
            g.fill(new Ellipse2D.Double(sx + 25 * s, yy - 14 * s, 55 * s, 34 * s));
            g.fill(new Ellipse2D.Double(sx + 55 * s, yy + 4 * s, 60 * s, 24 * s));
        }
    }

    class Bird {
        double x, y, sp;
        Bird(int i) { x = i * 300; y = 60 + hash(i * 3) * 150; sp = 30 + hash(i) * 30; }
        void draw(Graphics2D g) {
            double sx = (((x + t * sp - camX * 0.25) % (W + 200)) + (W + 200)) % (W + 200) - 100;
            double fy = y + Math.sin(t + x * 0.01) * 10;
            double flap = Math.sin(t * 9 + x) * 6;
            g.setColor(new Color(40, 45, 55, 180));
            g.setStroke(new BasicStroke(2f));
            g.draw(new QuadCurve2D.Double(sx - 9, fy, sx - 3, fy - flap, sx, fy));
            g.draw(new QuadCurve2D.Double(sx, fy, sx + 3, fy - flap, sx + 9, fy));
        }
    }

    // ------------------------------------------------------------ game flow
    void startRace(boolean two) {
        twoPlayers = two;
        p1 = new Bike("PLAYER 1", new Color(255, 84, 40), false, 140);
        p2 = new Bike(two ? "PLAYER 2" : "COMPUTER", new Color(48, 150, 255), !two, 80);
        parts.clear();
        camX = 260; camY = groundY(260) + 40;
        raceTime = 0; countdown = 3.6;
        state = 1;
    }

    int winner() {
        if (p1.finished && p2.finished) return p1.finishTime <= p2.finishTime ? 1 : 2;
        if (p1.finished) return 1;
        if (p2.finished) return 2;
        return p1.x >= p2.x ? 1 : 2;
    }

    void readInputs() {
        p1.throttle = keys[KeyEvent.VK_UP];
        p1.brake = keys[KeyEvent.VK_DOWN];
        p1.lean = (keys[KeyEvent.VK_LEFT] ? 1 : 0) - (keys[KeyEvent.VK_RIGHT] ? 1 : 0);
        p1.boostKey = keys[KeyEvent.VK_SHIFT];
        if (twoPlayers) {
            p2.throttle = keys[KeyEvent.VK_W];
            p2.brake = keys[KeyEvent.VK_S];
            p2.lean = (keys[KeyEvent.VK_A] ? 1 : 0) - (keys[KeyEvent.VK_D] ? 1 : 0);
            p2.boostKey = keys[KeyEvent.VK_SPACE];
        } else p2.aiControl();
    }

    void updateCamera(double dt) {
        double tx, ty;
        if (twoPlayers) {
            Bike lead = p1.x >= p2.x ? p1 : p2;
            Bike oth = lead == p1 ? p2 : p1;
            tx = lead.x * 0.62 + oth.x * 0.38;                 // bias toward leader
            ty = (groundY(p1.x) + groundY(p2.x)) / 2 + 70;
        } else {
            tx = p1.x + 130;
            ty = groundY(p1.x) + 80;
        }
        camX += (tx - camX) * Math.min(1, dt * 4.2);
        camY += (ty - camY) * Math.min(1, dt * 2.8);
    }

    void update(double dt) {
        if (state == 1) {
            countdown -= dt;
            if (countdown <= 0) state = 2;
        } else if (state == 2) {
            raceTime += dt;
            readInputs();
            p1.update(dt);
            p2.update(dt);
            updateCamera(dt);
            if ((twoPlayers && p1.finished && p2.finished)
                    || (!twoPlayers && p1.finished)
                    || raceTime > 150) state = 3;
        } else if (state == 0) {
            if (p1 == null) {
                p1 = new Bike("PLAYER 1", new Color(255, 84, 40), false, 330);
                p2 = new Bike("COMPUTER", new Color(48, 150, 255), true, 470);
            }
            p1.wheelRot += 0.10; p1.crank += 0.10;   // idle animation
            p2.wheelRot += 0.07; p2.crank += 0.07;
        }
        for (int i = parts.size() - 1; i >= 0; i--)
            if (!parts.get(i).update(dt)) parts.remove(i);
    }

    public void actionPerformed(ActionEvent e) {
        double now = System.nanoTime() / 1e9;
        double dt = Math.min(now - last, 1.0 / 30);
        last = now;
        t += dt;
        update(dt);
        repaint();
    }

    // ------------------------------------------------------------ input
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k < keys.length) keys[k] = true;
        if (state == 0) {
            if (k == KeyEvent.VK_1) startRace(false);
            if (k == KeyEvent.VK_2) startRace(true);
        } else if (state == 3) {
            if (k == KeyEvent.VK_R) startRace(twoPlayers);
            if (k == KeyEvent.VK_M) { state = 0; p1 = null; }
        }
        if (k == KeyEvent.VK_ESCAPE) { state = 0; p1 = null; }
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = false;
    }

    public void keyTyped(KeyEvent e) {}

    // ------------------------------------------------------------ rendering
    protected void paintComponent(Graphics gg) {
        super.paintComponent(gg);
        Graphics2D g = (Graphics2D) gg.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (state == 0) { camX = 400; camY = groundY(400) + 30; }

        drawSky(g);
        for (Cloud c : clouds) c.draw(g);
        for (Bird b : birds) b.draw(g);
        drawMountainLayer(g, 0.20, H * 0.52 + camY * 0.10, 1, new Color(105, 125, 158), true);
        drawMountainLayer(g, 0.45, H * 0.70 + camY * 0.20, 0, new Color(72, 105, 78), false);
        drawTerrain(g);
        drawFinish(g);
        if (p1 != null) { drawBike(g, p1); drawBike(g, p2); }
        for (int i = parts.size() - 1; i >= 0; i--) parts.get(i).draw(g);

        if (state >= 1) drawHUD(g);
        if (state == 0) drawMenu(g);
        if (state == 1) drawCountdown(g);
        if (state == 3) drawEnd(g);
        g.dispose();
    }

    void drawSky(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(42, 92, 168), 0, H, new Color(255, 196, 130)));
        g.fillRect(0, 0, W, H);
        double sunX = W * 0.76, sunY = H * 0.20;
        for (int i = 5; i >= 1; i--) {
            g.setColor(new Color(255, 240, 200, 14));
            g.fill(new Ellipse2D.Double(sunX - i * 26, sunY - i * 26, i * 52, i * 52));
        }
        g.setColor(new Color(255, 246, 214));
        g.fill(new Ellipse2D.Double(sunX - 26, sunY - 26, 52, 52));
    }

    void drawMountainLayer(Graphics2D g, double par, double baseY, int mode, Color col, boolean snow) {
        Path2D.Double p = new Path2D.Double();
        boolean first = true;
        for (int sx = -10; sx <= W + 10; sx += 6) {
            double wx = sx + camX * par;
            double h = mode == 1 ? farH(wx) : midH(wx);
            double topY = baseY - h;
            if (first) { p.moveTo(sx, topY); first = false; }
            else p.lineTo(sx, topY);
        }
        p.lineTo(W + 10, H + 50); p.lineTo(-10, H + 50); p.closePath();
        g.setColor(col); g.fill(p);

        if (snow) {  // snow caps on the high peaks
            ArrayList<ArrayList<double[]>> runs = new ArrayList<>();
            ArrayList<double[]> cur = null;
            for (int sx = -10; sx <= W + 10; sx += 6) {
                double h = farH(sx + camX * par);
                if (h > 215) {
                    if (cur == null) { cur = new ArrayList<>(); runs.add(cur); }
                    cur.add(new double[]{sx, baseY - h});
                } else cur = null;
            }
            for (ArrayList<double[]> run : runs) {
                Path2D.Double sp = new Path2D.Double();
                sp.moveTo(run.get(0)[0], run.get(0)[1]);
                for (double[] pt : run) sp.lineTo(pt[0], pt[1]);
                for (int i = run.size() - 1; i >= 0; i--) sp.lineTo(run.get(i)[0], baseY - 198);
                sp.closePath();
                g.setColor(new Color(235, 242, 250, 220));
                g.fill(sp);
            }
        }
    }

    void drawTerrain(Graphics2D g) {
        double startX = camX - W / 2 - 12, endX = camX + W / 2 + 12;
        Path2D.Double top = new Path2D.Double(), body = new Path2D.Double();
        boolean first = true;
        for (double wx = startX; wx <= endX; wx += 6) {
            double sx = toSX(wx), sy = toSY(groundY(wx));
            if (first) { top.moveTo(sx, sy); body.moveTo(sx, sy); first = false; }
            else { top.lineTo(sx, sy); body.lineTo(sx, sy); }
        }
        body.lineTo(toSX(endX), H + 60);
        body.lineTo(toSX(startX), H + 60);
        body.closePath();
        g.setPaint(new GradientPaint(0, (float) (H * 0.35), new Color(122, 84, 52), 0, H, new Color(52, 34, 20)));
        g.fill(body);
        g.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(56, 106, 50));
        g.draw(top);
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(96, 160, 78));
        g.draw(top);
        drawRocks(g);
        drawTrees(g);
        drawGrass(g);
    }

    void drawRocks(Graphics2D g) {
        for (int i = (int) ((camX - W / 2) / 120) - 1; i <= (camX + W / 2) / 120 + 1; i++) {
            double r = hash(i * 13 + 5);
            if (r < 0.5) continue;
            double sx = toSX(i * 120 + r * 60), sy = toSY(groundY(i * 120 + r * 60));
            if (sy < -30 || sy > H + 30) continue;
            double w = 6 + r * 12;
            g.setColor(new Color(110 + (int) (r * 30), 108, 104));
            g.fill(new Ellipse2D.Double(sx - w / 2, sy - w * 0.55, w, w * 0.7));
            g.setColor(new Color(150, 148, 142));
            g.fill(new Ellipse2D.Double(sx - w / 2 + 1, sy - w * 0.55 + 1, w * 0.5, w * 0.3));
        }
    }

    void drawTrees(Graphics2D g) {
        for (int i = (int) ((camX - W / 2) / 230) - 1; i <= (camX + W / 2) / 230 + 1; i++) {
            double r = hash(i * 7 + 3);
            if (r < 0.32) continue;
            double wx = i * 230 + r * 150;
            double sx = toSX(wx), sy = toSY(groundY(wx));
            if (sx < -90 || sx > W + 90 || sy < -140 || sy > H + 40) continue;
            double s = 30 + r * 50;
            double sway = Math.sin(t * 1.4 + i * 2.1) * 2.4;
            g.setColor(new Color(96, 66, 40));
            g.fillRect((int) (sx - 2.5), (int) (sy - s * 0.30), 5, (int) (s * 0.34));
            for (int l = 0; l < 3; l++) {
                double w = s * (1 - l * 0.24);
                double top = sy - s * (0.34 + l * 0.27);
                double base = sy - s * (0.06 + l * 0.25) + 4;
                int green = 96 - l * 14 + (int) (r * 22);
                g.setColor(new Color(green / 2, green, (int) (green * 0.45)));
                Polygon tri = new Polygon();
                tri.addPoint((int) (sx + sway * l * 0.7), (int) top);
                tri.addPoint((int) (sx - w / 2), (int) base);
                tri.addPoint((int) (sx + w / 2), (int) base);
                g.fillPolygon(tri);
            }
        }
    }

    void drawGrass(Graphics2D g) {
        g.setStroke(new BasicStroke(1.5f));
        for (int i = (int) ((camX - W / 2) / 13) - 1; i <= (camX + W / 2) / 13 + 1; i++) {
            double r = hash(i * 3 + 1);
            if (r < 0.42) continue;
            double wx = i * 13 + r * 8;
            double sx = toSX(wx), sy = toSY(groundY(wx));
            if (sy < -20 || sy > H + 20) continue;
            double hgt = 5 + r * 10;
            double sway = Math.sin(t * 2.3 + i * 1.7) * (1.5 + r * 2);
            int gg = 120 + (int) (r * 50);
            g.setColor(new Color(gg / 2, gg, gg / 2 + 20));
            for (int b = -1; b <= 1; b++) {
                double bx = sx + b * 2.6;
                double hh = hgt * (1 - 0.25 * Math.abs(b));
                g.draw(new Line2D.Double(bx, sy + 2, bx + sway + b * 1.6, sy + 2 - hh));
            }
        }
    }

    void drawFinish(Graphics2D g) {
        double sx = toSX(FINISH_X), sy = toSY(groundY(FINISH_X));
        if (sx < -120 || sx > W + 120) return;
        g.setColor(new Color(60, 60, 66));
        g.fill(new RoundRectangle2D.Double(sx - 4, sy - 190, 8, 195, 4, 4));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 8; c++) {
                g.setColor((r + c) % 2 == 0 ? Color.WHITE : new Color(25, 25, 30));
                g.fillRect((int) (sx + 4 + c * 11), (int) (sy - 190 + r * 11), 11, 11);
            }
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g.drawString("FINISH", (int) sx + 8, (int) sy - 196);
    }

    void drawWheel(Graphics2D g, double cx, double cy, double rot) {
        g.setColor(new Color(28, 28, 32));
        g.fill(new Ellipse2D.Double(cx - 21, cy - 21, 42, 42));
        g.setColor(new Color(205, 208, 214));
        g.fill(new Ellipse2D.Double(cx - 14, cy - 14, 28, 28));
        g.setColor(new Color(40, 42, 48));
        g.fill(new Ellipse2D.Double(cx - 5, cy - 5, 10, 10));
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(new Color(150, 153, 160));
        for (int i = 0; i < 7; i++) {
            double a = rot + i * Math.PI * 2 / 7;
            g.draw(new Line2D.Double(cx, cy, cx + Math.cos(a) * 13, cy + Math.sin(a) * 13));
        }
    }

    /** Fully articulated bike: spinning spoked wheels, working crank & pedals,
     *  two-segment legs and arms, compressing suspension, soft shadow. */
    void drawBike(Graphics2D g, Bike b) {
        double sx = toSX(b.x), sy = toSY(b.y);
        double gy = toSY(groundY(b.x));
        double hAbove = Math.max(0, b.y - groundY(b.x));
        double sh = Math.max(0.25, 1 - hAbove / 300.0);
        g.setColor(new Color(0, 0, 0, (int) (55 * sh)));
        g.fill(new Ellipse2D.Double(sx - 34 * sh, gy - 4, 68 * sh, 8 * sh + 2));

        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(sx, sy);
        g2.rotate(-b.angle);
        if (b.crashT > 0) g2.rotate(Math.sin(t * 35) * 0.18);
        double sus = b.sus;

        drawWheel(g2, -27, 4, b.wheelRot);
        drawWheel(g2, 27, 4, b.wheelRot + 0.6);

        // frame (hubs fixed, tubes compress with suspension)
        g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(b.color.darker());
        g2.draw(new Line2D.Double(-27, 4, -4, 2 + sus));
        g2.draw(new Line2D.Double(-4, 2 + sus, 27, 4));
        g2.draw(new Line2D.Double(-27, 4, -15, -12 + sus));
        g2.draw(new Line2D.Double(-15, -12 + sus, -4, 2 + sus));
        g2.draw(new Line2D.Double(-4, 2 + sus, 12, -14 + sus));
        g2.draw(new Line2D.Double(12, -14 + sus, 27, 4));
        g2.setColor(b.color);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(-4, 2 + sus, 12, -14 + sus));
        g2.setColor(new Color(40, 40, 46));
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(12, -14 + sus, 19, -21 + sus));   // handlebar
        g2.draw(new Line2D.Double(-15, -12 + sus, -20, -13 + sus)); // seat

        // rider
        double hipX = -13, hipY = -21 + sus;
        double shoX = 1, shoY = -35 + sus;
        double ca = b.crank;
        for (int leg = 0; leg < 2; leg++) {                          // pedalling legs
            double pa = ca + leg * Math.PI;
            double px = -4 + Math.cos(pa) * 9, py = 2 + sus * 0.6 + Math.sin(pa) * 9;
            double kx = (hipX + px) / 2, ky = (hipY + py) / 2;
            double dx = px - hipX, dy = py - hipY;
            double len = Math.hypot(dx, dy) + 1e-6;
            kx += -dy / len * 8; ky += dx / len * 8;
            g2.setColor(new Color(45, 48, 70));
            g2.setStroke(new BasicStroke(4.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(hipX, hipY, kx, ky));
            g2.draw(new Line2D.Double(kx, ky, px, py));
            g2.setColor(new Color(30, 30, 34));
            g2.fill(new Ellipse2D.Double(px - 3, py - 2, 7, 5));     // shoe on pedal
        }
        g2.setColor(b.color);                                        // jersey / torso
        g2.setStroke(new BasicStroke(6.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(hipX, hipY, shoX, shoY));
        g2.setColor(new Color(235, 190, 160));                       // arms
        g2.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double elX = (shoX + 19) / 2 - 2, elY = (shoY - 21 + sus) / 2 + 4;
        g2.draw(new Line2D.Double(shoX, shoY, elX, elY));
        g2.draw(new Line2D.Double(elX, elY, 19, -21 + sus));
        g2.setColor(b.color);                                        // helmet
        g2.fill(new Ellipse2D.Double(shoX + 1, shoY - 15, 13, 12));
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fill(new Ellipse2D.Double(shoX + 8, shoY - 11, 5, 4));    // visor glint
        g2.dispose();
    }

    // ------------------------------------------------------------ HUD & screens
    void panel(Graphics2D g, Bike b, int px, int py, String label) {
        RoundRectangle2D box = new RoundRectangle2D.Double(px, py, 240, 100, 16, 16);
        g.setColor(new Color(10, 14, 22, 175));
        g.fill(box);
        g.setColor(b.color);
        g.setStroke(new BasicStroke(2));
        g.draw(box);
        txt(g, label, px + 14, py + 22, new Font(Font.SANS_SERIF, Font.BOLD, 13), new Color(220, 226, 235));
        txt(g, (int) (b.speed * 0.14) + "", px + 14, py + 62,
                new Font(Font.SANS_SERIF, Font.BOLD, 34), Color.WHITE);
        txt(g, "km/h", px + 92, py + 62, new Font(Font.SANS_SERIF, Font.PLAIN, 14), new Color(170, 178, 190));
        txt(g, (int) Math.max(0, (FINISH_X - b.x) / 100) + " m", px + 168, py + 62,
                new Font(Font.SANS_SERIF, Font.BOLD, 18), new Color(255, 220, 120));
        g.setColor(new Color(60, 66, 80));
        g.fill(new RoundRectangle2D.Double(px + 14, py + 78, 200, 10, 5, 5));
        g.setColor(new Color(255, 150 + (int) b.boost, 40));
        g.fill(new RoundRectangle2D.Double(px + 14, py + 78, 200 * b.boost / 100.0, 10, 5, 5));
    }

    void drawMinimap(Graphics2D g) {
        int mw = 380, mh = 66, mx = (W - mw) / 2, my = 8;
        g.setColor(new Color(10, 14, 22, 175));
        g.fill(new RoundRectangle2D.Double(mx, my, mw, mh, 12, 12));
        double x0 = mx + 8, x1 = mx + mw - 8;
        Path2D.Double line = new Path2D.Double();
        for (int i = 0; i <= 100; i++) {
            double wx = FINISH_X * i / 100.0;
            double sx = x0 + (x1 - x0) * i / 100.0;
            double sy = my + mh - 14 - groundY(wx) * 0.16;
            if (i == 0) line.moveTo(sx, sy); else line.lineTo(sx, sy);
        }
        g.setColor(new Color(120, 200, 110));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(line);
        g.setColor(Color.WHITE);
        g.fillRect((int) x1 - 1, my + 8, 3, mh - 22);
        double cxs = x0 + (x1 - x0) * Math.max(0, camX - W / 2) / FINISH_X;
        double cxe = x0 + (x1 - x0) * Math.min(FINISH_X, camX + W / 2) / FINISH_X;
        g.setColor(new Color(255, 255, 255, 40));
        g.fillRect((int) cxs, my + 6, (int) Math.max(2, cxe - cxs), mh - 12);
        for (Bike b : new Bike[]{p1, p2}) {
            double bx = x0 + (x1 - x0) * Math.min(FINISH_X, Math.max(0, b.x)) / FINISH_X;
            double by = my + mh - 14 - groundY(b.x) * 0.16;
            g.setColor(b.color);
            g.fill(new Ellipse2D.Double(bx - 4, by - 4, 8, 8));
            g.setColor(Color.WHITE);
            g.draw(new Ellipse2D.Double(bx - 4, by - 4, 8, 8));
        }
    }

    void drawHUD(Graphics2D g) {
        panel(g, p1, 18, H - 118, twoPlayers ? "PLAYER 1 - ARROWS + SHIFT" : "YOU - ARROWS + SHIFT");
        panel(g, p2, W - 258, H - 118, twoPlayers ? "PLAYER 2 - WASD + SPACE" : "COMPUTER");
        Font f = new Font(Font.SANS_SERIF, Font.BOLD, 22);
        String ts = fmtTime(raceTime);
        txt(g, ts, W / 2 - g.getFontMetrics(f).stringWidth(ts) / 2, 106, f, Color.WHITE);
        int place = p1.x >= p2.x ? 1 : 2;
        txt(g, place == 1 ? "1st" : "2nd", W / 2 - 22, 142,
                new Font(Font.SANS_SERIF, Font.BOLD, 30),
                place == 1 ? new Color(255, 210, 60) : new Color(200, 200, 210));
        drawMinimap(g);
    }

    void drawCountdown(Graphics2D g) {
        String s = countdown > 0.6 ? String.valueOf((int) Math.ceil(countdown - 0.6)) : "GO!";
        Font f = new Font(Font.SANS_SERIF, Font.BOLD, 130);
        int tw = g.getFontMetrics(f).stringWidth(s);
        g.setFont(f);
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(s, W / 2 - tw / 2 + 5, (int) (H * 0.42) + 5);
        g.setColor(countdown > 0.6 ? new Color(255, 210, 70) : new Color(120, 255, 130));
        g.drawString(s, W / 2 - tw / 2, (int) (H * 0.42));
    }

    void drawOption(Graphics2D g, int x, int y, int w, int h, String key, String label, Color col, String hint) {
        RoundRectangle2D r = new RoundRectangle2D.Double(x, y, w, h, 20, 20);
        g.setColor(new Color(12, 16, 26, 210));
        g.fill(r);
        g.setColor(col);
        g.setStroke(new BasicStroke(3));
        g.draw(r);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 64));
        g.drawString(key, x + 30, y + h / 2 + 22);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        g.setColor(Color.WHITE);
        g.drawString(label, x + 110, y + h / 2 + 9);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        g.setColor(new Color(180, 188, 200));
        g.drawString(hint, x + 110, y + h / 2 + 36);
    }

    void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 20, 110));
        g.fillRect(0, 0, W, H);
        Font title = new Font(Font.SANS_SERIF, Font.BOLD, 56);
        String s1 = "EXTREME MOUNTAIN BIKE RACING";
        int tw = g.getFontMetrics(title).stringWidth(s1);
        g.setFont(title);
        g.setColor(new Color(255, 120, 40));
        g.drawString(s1, W / 2 - tw / 2 + 3, 155);
        g.setColor(new Color(255, 190, 80));
        g.drawString(s1, W / 2 - tw / 2, 152);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        g.setColor(new Color(225, 232, 240));
        String s2 = "Two riders. One mountain. 16 km of airtime.";
        g.drawString(s2, W / 2 - g.getFontMetrics().stringWidth(s2) / 2, 192);

        drawOption(g, W / 2 - 430, 240, 400, 120, "1", "RACE THE COMPUTER",
                new Color(255, 84, 40), "Chase the AI rider down the mountain");
        drawOption(g, W / 2 + 30, 240, 400, 120, "2", "TWO PLAYERS",
                new Color(48, 150, 255), "Share the keyboard with a friend");

        Font c = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
        String[] lines = {
                "Player 1:  ARROW KEYS pedal / brake / lean in the air   -   SHIFT for boost",
                "Player 2:  W A S D pedal / brake / lean in the air   -   SPACE for boost",
                "Land on your wheels! Too much tilt at touchdown = crash.   ESC for menu"
        };
        for (int i = 0; i < lines.length; i++)
            txt(g, lines[i], W / 2 - g.getFontMetrics(c).stringWidth(lines[i]) / 2,
                    430 + i * 30, c, new Color(215, 222, 232));
    }

    void drawEnd(Graphics2D g) {
        g.setColor(new Color(0, 0, 10, 150));
        g.fillRect(0, 0, W, H);
        Bike win = winner() == 1 ? p1 : p2;
        Font f = new Font(Font.SANS_SERIF, Font.BOLD, 64);
        String s = win.name + " WINS!";
        int tw = g.getFontMetrics(f).stringWidth(s);
        g.setFont(f);
        g.setColor(win.color.darker());
        g.drawString(s, W / 2 - tw / 2 + 4, 284);
        g.setColor(win.color.brighter());
        g.drawString(s, W / 2 - tw / 2, 280);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        g.setColor(new Color(230, 235, 242));
        g.drawString("Player 1:  " + (p1.finished ? fmtTime(p1.finishTime) : "DNF"), W / 2 - 140, 342);
        g.drawString((twoPlayers ? "Player 2:  " : "Computer:  ") + (p2.finished ? fmtTime(p2.finishTime) : "DNF"), W / 2 - 140, 378);
        g.setColor(new Color(255, 220, 120));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        g.drawString("Press R to race again   -   M for menu", W / 2 - 215, 440);
    }
}