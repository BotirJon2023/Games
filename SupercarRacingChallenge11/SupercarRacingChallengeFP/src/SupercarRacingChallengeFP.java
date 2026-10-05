import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;


public class SupercarRacingChallengeFP extends JFrame {

    public SupercarRacingChallengeFP() {
        setTitle("🏁 Supercar Racing Challenge — Driver View 🏎");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(new GamePanel());
        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SupercarRacingChallengeFP().setVisible(true));
    }

    // ================================================================
    //  GAME PANEL
    // ================================================================
    static class GamePanel extends JPanel {

        // --- Screen ---
        static final int WIDTH = 1280;
        static final int HEIGHT = 760;
        static final int HORIZON = 280;

        // --- Game state ---
        enum State { MENU, COUNTDOWN, RACING, FINISHED }
        State state = State.MENU;
        boolean twoPlayer = false;
        int countdown = 3;
        int countdownTick = 0;

        // --- World ---
        double worldZ = 0;
        double curve = 0, targetCurve = 0;
        double hill = 0, targetHill = 0;

        // --- Cars ---
        Car p1 = new Car("YOU", new Color(220, 40, 40));
        Car p2 = new Car("PLAYER 2", new Color(60, 130, 240));

        // --- AI ---
        double aiTargetX = 0;
        int aiTimer = 0;

        // --- World objects ---
        List<WorldObject> objects = new ArrayList<>();

        // --- Particles ---
        List<Particle> particles = new ArrayList<>();

        // --- Keys ---
        Set<Integer> keys = new HashSet<>();

        // --- Timing ---
        long lastTime = System.nanoTime();

        // --- Windshield rain / dirt effect ---
        List<Point2D.Double> rainDrops = new ArrayList<>();
        int rainFrame = 0;

        // --- G-force effect ---
        double gForceX = 0;

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setBackground(Color.BLACK);
            setFocusable(true);
            requestFocusInWindow();

            for (int i = 0; i < 200; i++) spawnObject(200 + i * 55);
            for (int i = 0; i < 40; i++) rainDrops.add(new Point2D.Double(Math.random() * WIDTH, Math.random() * HEIGHT));

            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    keys.add(e.getKeyCode());
                    if (state == State.MENU) {
                        if (e.getKeyCode() == KeyEvent.VK_1) { twoPlayer = false; startRace(); }
                        if (e.getKeyCode() == KeyEvent.VK_2) { twoPlayer = true;  startRace(); }
                    } else if (state == State.FINISHED && e.getKeyCode() == KeyEvent.VK_R) {
                        state = State.MENU;
                    }
                }
                @Override public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
            });

            javax.swing.Timer t = new javax.swing.Timer(1000 / 60, e -> {
                long now = System.nanoTime();
                double dt = Math.min(0.05, (now - lastTime) / 1_000_000_000.0);
                lastTime = now;
                update(dt);
                repaint();
            });
            t.start();
        }

        void startRace() {
            p1.reset(); p2.reset();
            worldZ = 0; curve = 0; targetCurve = 0; hill = 0; targetHill = 0;
            objects.clear(); particles.clear();
            for (int i = 0; i < 200; i++) spawnObject(200 + i * 55);
            state = State.COUNTDOWN;
            countdown = 3; countdownTick = 0;
        }

        void spawnObject(double z) {
            Random r = new Random();
            WorldObject o = new WorldObject();
            o.z = z;
            int type = r.nextInt(12);
            if (type < 3) {
                o.kind = WorldObject.Kind.OBSTACLE;
                o.x = -0.5 + r.nextDouble();
                o.color = new Color(200 + r.nextInt(55), 80 + r.nextInt(80), 40);
            } else if (type < 8) {
                o.kind = WorldObject.Kind.TREE;
                o.x = (r.nextBoolean() ? -1.05 : 1.05) - r.nextDouble() * 0.4;
                o.color = new Color(30, 120 + r.nextInt(80), 40);
            } else if (type < 10) {
                o.kind = WorldObject.Kind.POLE;
                o.x = (r.nextBoolean() ? -1.15 : 1.15);
                o.color = new Color(200, 200, 220);
            } else {
                o.kind = WorldObject.Kind.BILLBOARD;
                o.x = (r.nextBoolean() ? -1.3 : 1.3) - r.nextDouble() * 0.3;
                o.color = new Color(r.nextInt(155) + 100, r.nextInt(155) + 100, r.nextInt(155) + 100);
            }
            objects.add(o);
        }

        // =========================================================
        //  UPDATE
        // =========================================================
        void update(double dt) {
            if (state == State.COUNTDOWN) {
                countdownTick++;
                if (countdownTick >= 60) { countdownTick = 0; countdown--; if (countdown < 0) state = State.RACING; }
            }

            if (state == State.RACING) {
                boolean p1Up = keys.contains(KeyEvent.VK_UP);
                boolean p1Down = keys.contains(KeyEvent.VK_DOWN);
                boolean p1Left = keys.contains(KeyEvent.VK_LEFT);
                boolean p1Right = keys.contains(KeyEvent.VK_RIGHT);
                p1.control(p1Up, p1Down, p1Left, p1Right, dt);

                if (twoPlayer) {
                    boolean p2Up = keys.contains(KeyEvent.VK_W);
                    boolean p2Down = keys.contains(KeyEvent.VK_S);
                    boolean p2Left = keys.contains(KeyEvent.VK_A);
                    boolean p2Right = keys.contains(KeyEvent.VK_D);
                    p2.control(p2Up, p2Down, p2Left, p2Right, dt);
                } else {
                    updateAI(dt);
                }

                // Advance world based on player 1 (player 2 shares same track in split view)
                worldZ += p1.speed * 40 * dt;

                if (Math.random() < 0.006) targetCurve = (Math.random() - 0.5) * 1.6;
                if (Math.random() < 0.004) targetHill = (Math.random() - 0.5) * 1.2;
                curve += (targetCurve - curve) * dt * 0.7;
                hill  += (targetHill  - hill)  * dt * 0.7;

                // Recycle objects
                Iterator<WorldObject> it = objects.iterator();
                while (it.hasNext()) {
                    WorldObject o = it.next();
                    if (o.z < worldZ - 50) {
                        it.remove();
                        spawnObject(worldZ + 1300 + Math.random() * 400);
                    }
                }

                checkCollision(p1);
                checkCollision(p2);

                if (Math.random() < 0.7) particles.add(exhaustP1());
                if (Math.random() < 0.7) particles.add(exhaustP2());

                // G-force based on steering
                double targetG = (p1Left ? -1 : p1Right ? 1 : 0) * (p1.speed / 6.0);
                gForceX += (targetG - gForceX) * dt * 4;
            }

            // Particles
            Iterator<Particle> pit = particles.iterator();
            while (pit.hasNext()) {
                Particle p = pit.next();
                p.x += p.vx * dt * 60;
                p.y += p.vy * dt * 60;
                p.life -= dt * 60;
                if (p.life <= 0) pit.remove();
            }

            // Rain drops slide down windshield
            rainFrame++;
            if (rainFrame % 3 == 0) {
                for (Point2D.Double d : rainDrops) {
                    d.y += 1.5 + Math.random() * 1.5;
                    d.x += (Math.random() - 0.5) * 0.5 - gForceX * 2;
                    if (d.y > HEIGHT) { d.y = -10; d.x = Math.random() * WIDTH; }
                    if (d.x < 0) d.x = WIDTH; if (d.x > WIDTH) d.x = 0;
                }
            }
        }

        void updateAI(double dt) {
            aiTimer++;
            double desiredX = 0;
            WorldObject nearest = null;
            double bestZ = Double.MAX_VALUE;
            for (WorldObject o : objects) {
                if (o.kind == WorldObject.Kind.OBSTACLE && o.z > worldZ + 25 && o.z - worldZ < bestZ) {
                    bestZ = o.z - worldZ;
                    nearest = o;
                }
            }
            if (nearest != null && bestZ < 380) {
                desiredX = nearest.x > 0 ? -0.5 : 0.5;
            } else {
                if (aiTimer > 150) { aiTimer = 0; aiTargetX = (Math.random() - 0.5) * 0.7; }
                desiredX = aiTargetX;
            }
            boolean left  = p2.x > desiredX + 0.05;
            boolean right = p2.x < desiredX - 0.05;
            boolean up    = Math.abs(p2.x - desiredX) < 0.55;
            boolean down  = Math.abs(p2.x - desiredX) > 0.85 && p2.speed > 3;
            p2.control(up, down, left, right, dt);
        }

        void checkCollision(Car car) {
            for (WorldObject o : objects) {
                if (o.kind != WorldObject.Kind.OBSTACLE) continue;
                double dz = Math.abs(o.z - worldZ - 30);
                if (dz < 15 && Math.abs(o.x - car.x) < 0.16) {
                    state = State.FINISHED;
                    for (int i = 0; i < 80; i++) {
                        Particle p = new Particle();
                        p.x = WIDTH / 2 + car.x * 320;
                        p.y = HEIGHT * 0.55;
                        double a = Math.random() * Math.PI * 2;
                        double s = 2 + Math.random() * 8;
                        p.vx = Math.cos(a) * s; p.vy = Math.sin(a) * s;
                        p.life = 40 + Math.random() * 40;
                        p.color = car.color;
                        p.size = 3 + Math.random() * 6;
                        particles.add(p);
                    }
                    return;
                }
            }
        }

        Particle exhaustP1() {
            Particle p = new Particle();
            p.x = WIDTH / 2 - 320 + (Math.random() - 0.5) * 25;
            p.y = HEIGHT - 60 + (Math.random() - 0.5) * 15;
            p.vx = (Math.random() - 0.5) * 1.2; p.vy = -1.5 - Math.random() * 2;
            p.life = 25 + Math.random() * 15;
            p.color = p1.speed > 4 ? new Color(255, 200, 80, 220) : new Color(200, 200, 200, 160);
            p.size = 4 + Math.random() * 6;
            return p;
        }
        Particle exhaustP2() {
            Particle p = new Particle();
            p.x = WIDTH / 2 + 320 + (Math.random() - 0.5) * 25;
            p.y = HEIGHT - 60 + (Math.random() - 0.5) * 15;
            p.vx = (Math.random() - 0.5) * 1.2; p.vy = -1.5 - Math.random() * 2;
            p.life = 25 + Math.random() * 15;
            p.color = p2.speed > 4 ? new Color(255, 200, 80, 220) : new Color(200, 200, 200, 160);
            p.size = 4 + Math.random() * 6;
            return p;
        }

        // =========================================================
        //  RENDER
        // =========================================================
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            if (state == State.MENU) {
                drawMenu(g2);
                return;
            }

            if (twoPlayer) {
                // Split-screen: left = p1 cockpit, right = p2 cockpit
                drawCockpitView(g2, new Rectangle(0, 0, WIDTH / 2, HEIGHT), p1, true);
                drawCockpitView(g2, new Rectangle(WIDTH / 2, 0, WIDTH / 2, HEIGHT), p2, false);
                // Divider
                g2.setColor(new Color(0, 0, 0, 200));
                g2.fillRect(WIDTH / 2 - 3, 0, 6, HEIGHT);
                g2.setColor(new Color(255, 220, 80));
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(WIDTH / 2, 0, WIDTH / 2, HEIGHT);
            } else {
                // Single cockpit + AI rearview mirror
                drawCockpitView(g2, new Rectangle(0, 0, WIDTH, HEIGHT), p1, true);
                drawAIRearView(g2);
            }

            if (state == State.COUNTDOWN) drawCountdown(g2);
            if (state == State.FINISHED) drawResults(g2);
        }

        // ---------- Full cockpit view ----------
        void drawCockpitView(Graphics2D g2, Rectangle view, Car car, boolean isLeft) {
            int vx = view.x, vy = view.y, vw = view.width, vh = view.height;
            int vHorizon = vy + (int)(vh * 0.36);
            int vCenterX = vx + vw / 2;

            // ============ Windshield area (clip) ============
            Shape oldClip = g2.getClip();
            // Windshield shape — trapezoid with rounded corners
            int dashTop = vy + (int)(vh * 0.62);
            Path2D windshield = new Path2D.Double();
            windshield.moveTo(vx + vw * 0.04, dashTop);
            windshield.lineTo(vx + vw * 0.10, vy + vh * 0.10);
            windshield.lineTo(vx + vw * 0.90, vy + vh * 0.10);
            windshield.lineTo(vx + vw * 0.96, dashTop);
            windshield.closePath();
            g2.clip(windshield);

            // --- Sky ---
            GradientPaint sky = new GradientPaint(0, vy, new Color(20, 20, 60),
                    0, vHorizon, new Color(255, 140, 90));
            g2.setPaint(sky);
            g2.fillRect(vx, vy, vw, vHorizon - vy);

            // Sun
            g2.setColor(new Color(255, 220, 120, 220));
            g2.fillOval(vCenterX - 50, vHorizon - 80, 100, 100);
            g2.setColor(new Color(255, 240, 180, 90));
            g2.fillOval(vCenterX - 80, vHorizon - 110, 160, 160);

            // Stars
            Random rs = new Random(11);
            g2.setColor(new Color(255, 255, 255, 160));
            for (int i = 0; i < 40; i++) {
                g2.fillOval(vx + rs.nextInt(vw), vy + rs.nextInt(Math.max(1, vHorizon - vy - 60)), 2, 2);
            }

            // Distant mountains (parallax)
            int parallax = (int)(curve * 50 + car.x * -60);
            g2.setColor(new Color(45, 35, 75));
            for (int i = -1; i < vw / 55 + 1; i++) {
                int h = 40 + (int)(Math.sin(i * 0.7) * 25) + (i % 3) * 15;
                int x = vx + i * 55 + parallax;
                Polygon mtn = new Polygon(
                        new int[]{x - 30, x + 30, x + 90},
                        new int[]{vHorizon, vHorizon - h, vHorizon}, 3);
                g2.fillPolygon(mtn);
            }

            // Horizon glow
            g2.setColor(new Color(255, 200, 150, 120));
            g2.fillRect(vx, vHorizon - 2, vw, 4);

            // --- Ground below horizon ---
            GradientPaint ground = new GradientPaint(0, vHorizon, new Color(60, 40, 80),
                    0, dashTop, new Color(15, 10, 30));
            g2.setPaint(ground);
            g2.fillRect(vx, vHorizon, vw, dashTop - vHorizon);

            // --- Road with perspective ---
            drawPerspectiveRoad(g2, vx, vy, vw, vh, vHorizon, vCenterX, car);

            // --- Objects on the road ---
            drawPerspectiveObjects(g2, vx, vw, vHorizon, vCenterX, car, view);

            // --- Particles ---
            for (Particle p : particles) {
                float alpha = Math.max(0, Math.min(1, p.life / 40f));
                Color c = p.color;
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(alpha * c.getAlpha())));
                double px = isLeft ? p.x - WIDTH / 2 + vw / 2 : p.x + (isLeft ? 0 : 0);
                double py = p.y;
                g2.fillOval((int)(px - p.size), (int)(py - p.size), (int)(p.size * 2), (int)(p.size * 2));
            }

            // --- Rain drops on windshield ---
            g2.setColor(new Color(180, 220, 255, 90));
            for (Point2D.Double d : rainDrops) {
                double dx = isLeft ? d.x : d.x - WIDTH / 2;
                if (dx >= 0 && dx <= vw) {
                    g2.fillOval((int)(vx + dx), (int)d.y, 3, 5);
                    g2.setColor(new Color(180, 220, 255, 40));
                    g2.fillOval((int)(vx + dx - 3), (int)d.y - 2, 10, 10);
                    g2.setColor(new Color(180, 220, 255, 90));
                }
            }

            // Subtle windshield reflection
            g2.setColor(new Color(255, 255, 255, 15));
            g2.fillPolygon(new int[]{vx + vw / 4, vx + vw / 3, vx + vw / 3, vx + vw / 4},
                    new int[]{vy, vy, vh / 2, vh / 2}, 4);

            // === End windshield clip ===
            g2.setClip(oldClip);

            // ============ Dashboard ============
            drawDashboard(g2, vx, vy, vw, vh, car, isLeft);

            // ============ A-pillars & frame ============
            drawCockpitFrame(g2, vx, vy, vw, vh, dashTop);
        }

        // ---------- Perspective road ----------
        void drawPerspectiveRoad(Graphics2D g2, int vx, int vy, int vw, int vh,
                                 int vHorizon, int vCenterX, Car car) {
            int segments = 100;
            double totalDepth = 2600;
            int dashTop = vy + (int)(vh * 0.62);

            for (int i = segments; i >= 0; i--) {
                double z0 = i * (totalDepth / segments);
                double z1 = (i + 1) * (totalDepth / segments);
                double zRel0 = z0 - (worldZ % (totalDepth / segments));
                double zRel1 = z1 - (worldZ % (totalDepth / segments));
                if (zRel0 < 8) continue;

                double scale0 = 1.0 / zRel0;
                double scale1 = 1.0 / zRel1;

                // Curve & hill offsets
                double curveOffset0 = curve * 260 * scale0 * zRel0 * 0.006;
                double curveOffset1 = curve * 260 * scale1 * zRel1 * 0.006;
                double hillOffset0 = hill * 40 * scale0 * zRel0 * 0.03;
                double hillOffset1 = hill * 40 * scale1 * zRel1 * 0.03;

                int y0 = (int)(vHorizon + (dashTop - vHorizon) * (1 - scale0) + hillOffset0);
                int y1 = (int)(vHorizon + (dashTop - vHorizon) * (1 - scale1) + hillOffset1);

                int w0 = (int)(vw * 0.10 + vw * 1.6 * (1 - scale0));
                int w1 = (int)(vw * 0.10 + vw * 1.6 * (1 - scale1));

                int cx0 = vCenterX + (int)curveOffset0;
                int cx1 = vCenterX + (int)curveOffset1;

                // Curb
                int curbW0 = w0 + 30;
                int curbW1 = w1 + 30;
                g2.setColor((i / 2) % 2 == 0 ? new Color(220, 40, 40) : Color.WHITE);
                g2.fillPolygon(
                        new int[]{cx0 - curbW0 / 2, cx0 + curbW0 / 2, cx1 + curbW1 / 2, cx1 - curbW1 / 2},
                        new int[]{y0, y0, y1, y1}, 4);

                // Asphalt
                g2.setColor((i / 4) % 2 == 0 ? new Color(50, 50, 55) : new Color(45, 45, 50));
                g2.fillPolygon(
                        new int[]{cx0 - w0 / 2, cx0 + w0 / 2, cx1 + w1 / 2, cx1 - w1 / 2},
                        new int[]{y0, y0, y1, y1}, 4);

                // Center dashes
                if (i % 2 == 0) {
                    int lw0 = Math.max(2, w0 / 40);
                    int lw1 = Math.max(2, w1 / 40);
                    g2.setColor(new Color(255, 240, 200, 200));
                    g2.fillPolygon(
                            new int[]{cx0 - lw0 / 2, cx0 + lw0 / 2, cx1 + lw1 / 2, cx1 - lw1 / 2},
                            new int[]{y0, y0, y1, y1}, 4);
                }

                // Edge lines
                g2.setColor(new Color(255, 255, 255, 220));
                int edge = 4;
                g2.fillPolygon(new int[]{cx0 - w0 / 2, cx0 - w0 / 2 + edge, cx1 - w1 / 2 + edge, cx1 - w1 / 2},
                        new int[]{y0, y0, y1, y1}, 4);
                g2.fillPolygon(new int[]{cx0 + w0 / 2 - edge, cx0 + w0 / 2, cx1 + w1 / 2, cx1 + w1 / 2 - edge},
                        new int[]{y0, y0, y1, y1}, 4);
            }

            // Player car hood (bottom of windshield)
            int hoodTop = dashTop - 40;
            g2.setColor(car.color.darker());
            g2.fillRect(vx, hoodTop + 40, vw, vh);
            GradientPaint hood = new GradientPaint(0, hoodTop, car.color,
                    0, dashTop, car.color.darker());
            g2.setPaint(hood);
            g2.fillRect(vx, hoodTop, vw, 60);

            // Hood details — vents & center stripe
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRect(vx, hoodTop + 30, vw, 4);
            g2.setColor(new Color(255, 255, 255, 60));
            g2.fillRect(vCenterX - 30, hoodTop, 60, 60);
            // Hood scoops
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(vCenterX - 120, hoodTop + 12, 60, 14, 6, 6);
            g2.fillRoundRect(vCenterX + 60, hoodTop + 12, 60, 14, 6, 6);
        }

        // ---------- Perspective objects ----------
        void drawPerspectiveObjects(Graphics2D g2, int vx, int vw, int vHorizon,
                                    int vCenterX, Car car, Rectangle view) {
            int dashTop = view.y + (int)(view.height * 0.62);
            List<WorldObject> sorted = new ArrayList<>(objects);
            sorted.sort((a, b) -> Double.compare(b.z, a.z));

            for (WorldObject o : sorted) {
                double zRel = o.z - worldZ;
                if (zRel < 8 || zRel > 2200) continue;

                double scale = 1.0 / zRel;
                double curveOffset = curve * 260 * scale * zRel * 0.006;
                double hillOffset = hill * 40 * scale * zRel * 0.03;

                int y = (int)(vHorizon + (dashTop - vHorizon) * (1 - scale) + hillOffset);
                int cx = vCenterX + (int)curveOffset;
                int w = (int)(vw * 0.10 + vw * 1.6 * (1 - scale));
                int x = cx + (int)((o.x - car.x * 0.5) * w / 2);

                int size = (int)(scale * 900);
                if (size < 2) size = 2;
                if (size > 240) size = 240;

                switch (o.kind) {
                    case OBSTACLE -> {
                        int bw = size / 3, bh = size / 2;
                        g2.setColor(new Color(0, 0, 0, 100));
                        g2.fillRect(x - bw / 2 + 4, y - bh + 4, bw, bh);
                        g2.setColor(o.color);
                        g2.fillRect(x - bw / 2, y - bh, bw, bh);
                        g2.setColor(o.color.brighter());
                        g2.fillRect(x - bw / 2, y - bh, bw, bh / 4);
                        g2.setColor(Color.YELLOW);
                        g2.fillRect(x - bw / 2, y - bh + bh / 2, bw, Math.max(2, bh / 12));
                    }
                    case TREE -> {
                        int tw = size / 6, th = size;
                        g2.setColor(new Color(80, 50, 30));
                        g2.fillRect(x - tw / 4, y - th / 3, tw / 2, th / 3);
                        g2.setColor(o.color);
                        g2.fillOval(x - tw, y - th, tw * 2, (int)(th * 0.8));
                        g2.setColor(o.color.brighter());
                        g2.fillOval(x - tw + tw / 3, y - th + th / 4, tw / 2, tw / 2);
                    }
                    case POLE -> {
                        int pw = Math.max(2, size / 20), ph = size;
                        g2.setColor(o.color);
                        g2.fillRect(x - pw / 2, y - ph, pw, ph);
                        g2.setColor(Color.WHITE);
                        g2.fillRect(x - pw * 2, y - ph, pw * 4, Math.max(2, ph / 20));
                    }
                    case BILLBOARD -> {
                        int bw = size, bh = size / 2;
                        g2.setColor(new Color(40, 40, 40));
                        g2.fillRect(x - bw / 2, y - bh, bw, bh);
                        g2.setColor(o.color);
                        g2.fillRect(x - bw / 2 + 4, y - bh + 4, bw - 8, bh - 8);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Arial", Font.BOLD, Math.max(8, size / 8)));
                        g2.drawString("FAST", x - size / 6, y - bh / 2);
                    }
                }
            }
        }

        // ---------- Dashboard ----------
        void drawDashboard(Graphics2D g2, int vx, int vy, int vw, int vh, Car car, boolean isLeft) {
            int dashTop = vy + (int)(vh * 0.62);
            int dashBottom = vy + vh;

            // Dashboard base (dark leather with stitch)
            GradientPaint dash = new GradientPaint(0, dashTop, new Color(35, 30, 30),
                    0, dashBottom, new Color(15, 12, 12));
            g2.setPaint(dash);
            g2.fillRect(vx, dashTop, vw, dashBottom - dashTop);

            // Top edge highlight
            g2.setColor(new Color(90, 80, 80));
            g2.fillRect(vx, dashTop, vw, 3);

            // Stitching
            g2.setColor(new Color(180, 60, 60));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10, new float[]{4, 6}, 0));
            g2.drawLine(vx + 20, dashTop + 12, vx + vw - 20, dashTop + 12);
            g2.setStroke(new BasicStroke(1f));

            // Center console
            int consoleW = (int)(vw * 0.32);
            int consoleX = vx + vw / 2 - consoleW / 2;
            int consoleY = dashTop + 20;
            int consoleH = dashBottom - consoleY - 20;
            g2.setColor(new Color(20, 18, 18));
            g2.fillRoundRect(consoleX, consoleY, consoleW, consoleH, 20, 20);
            g2.setColor(new Color(60, 55, 55));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(consoleX, consoleY, consoleW, consoleH, 20, 20);

            // Air vents
            g2.setColor(new Color(10, 10, 10));
            g2.fillRoundRect(consoleX + 20, consoleY + 20, 60, 22, 6, 6);
            g2.fillRoundRect(consoleX + consoleW - 80, consoleY + 20, 60, 22, 6, 6);
            g2.setColor(new Color(70, 70, 70));
            for (int i = 0; i < 5; i++) {
                g2.drawLine(consoleX + 24 + i * 11, consoleY + 22, consoleX + 24 + i * 11, consoleY + 40);
                g2.drawLine(consoleX + consoleW - 76 + i * 11, consoleY + 22,
                        consoleX + consoleW - 76 + i * 11, consoleY + 40);
            }

            // ===== Digital speedometer / tach =====
            int gaugeCx = vx + vw / 2;
            int gaugeCy = dashBottom - 70;

            // Digital speed readout
            g2.setFont(new Font("Consolas", Font.BOLD, 64));
            FontMetrics fm = g2.getFontMetrics();
            String speedTxt = String.valueOf((int)(car.speed * 40));
            int speedW = fm.stringWidth(speedTxt);
            g2.setColor(new Color(0, 255, 180));
            g2.drawString(speedTxt, gaugeCx - speedW / 2, gaugeCy - 8);
            g2.setColor(new Color(0, 255, 180, 60));
            g2.drawString(speedTxt, gaugeCx - speedW / 2 - 2, gaugeCy - 10);

            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            fm = g2.getFontMetrics();
            String unit = "km/h";
            g2.setColor(new Color(200, 200, 200));
            g2.drawString(unit, gaugeCx - fm.stringWidth(unit) / 2, gaugeCy + 15);

            // ===== Tachometer arc =====
            int tachCx = vx + (isLeft ? vw - 130 : 130);
            int tachCy = dashBottom - 70;
            int tachR = 55;

            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillOval(tachCx - tachR - 4, tachCy - tachR - 4, (tachR + 4) * 2, (tachR + 4) * 2);

            // Arc background
            g2.setColor(new Color(60, 60, 60));
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawArc(tachCx - tachR, tachCy - tachR, tachR * 2, tachR * 2, 210, -240);

            // RPM arc (filled)
            double rpmPct = Math.min(1.0, car.speed / 6.0);
            int arcAngle = (int)(-240 * rpmPct);
            GradientPaint rpmGrad = new GradientPaint(tachCx - tachR, tachCy, new Color(0, 255, 150),
                    tachCx + tachR, tachCy, new Color(255, 40, 40));
            g2.setPaint(rpmGrad);
            g2.setStroke(new BasicStroke(0x1.8p2f, BasicStroke.CAP_ROUND));
            g2.drawArc(tachCx - tachR, tachCy - tachR, tachR * 2, tachR * 2, 210, arcAngle);

            // Needle
            double needleAngle = Math.toRadians(210 + arcAngle);
            int nx = tachCx + (int)(Math.cos(needleAngle) * (tachR - 8));
            int ny = tachCy - (int)(Math.sin(needleAngle) * (tachR - 8));
            g2.setColor(new Color(255, 60, 60));
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(tachCx, tachCy, nx, ny);
            g2.setColor(Color.WHITE);
            g2.fillOval(tachCx - 5, tachCy - 5, 10, 10);

            // RPM digits
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.setColor(new Color(220, 220, 220));
            fm = g2.getFontMetrics();
            String rpmStr = String.format("%.1f", rpmPct * 8);
            g2.drawString(rpmStr, tachCx - fm.stringWidth(rpmStr) / 2, tachCy + tachR + 20);

            // ===== Steering wheel =====
            drawSteeringWheel(g2, vx, vy, vw, vh, car);

            // ===== Gear / lap indicators =====
            drawIndicators(g2, vx, vy, vw, vh, car);
        }

        // ---------- Steering wheel (turns with steering) ----------
        void drawSteeringWheel(Graphics2D g2, int vx, int vy, int vw, int vh, Car car) {
            int cx = vx + vw / 2;
            int cy = vy + vh - 30;
            int R = 130;

            AffineTransform old = g2.getTransform();
            g2.rotate(car.steer * 0.55, cx, cy);

            // Wheel shadow
            g2.setColor(new Color(0, 0, 0, 150));
            g2.setStroke(new BasicStroke(30f, BasicStroke.CAP_ROUND));
            g2.drawArc(cx - R - 3, cy - R - 3, R * 2, R * 2, 200, 140);

            // Wheel rim (leather)
            g2.setColor(new Color(25, 22, 22));
            g2.setStroke(new BasicStroke(28f, BasicStroke.CAP_ROUND));
            g2.drawArc(cx - R, cy - R, R * 2, R * 2, 200, 140);

            // Wheel rim highlight
            g2.setColor(new Color(70, 65, 65));
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND));
            g2.drawArc(cx - R, cy - R, R * 2, R * 2, 200, 140);

            // Spokes (3-spoke sport design)
            g2.setColor(new Color(20, 18, 18));
            g2.setStroke(new BasicStroke(22f, BasicStroke.CAP_ROUND));
            // Left spoke
            g2.drawLine(cx, cy, cx - (int)(Math.cos(Math.toRadians(20)) * R), cy + (int)(Math.sin(Math.toRadians(20)) * R));
            // Right spoke
            g2.drawLine(cx, cy, cx + (int)(Math.cos(Math.toRadians(20)) * R), cy + (int)(Math.sin(Math.toRadians(20)) * R));
            // Bottom spoke
            g2.drawLine(cx, cy, cx, cy + R - 15);

            // Center hub
            g2.setColor(new Color(15, 15, 15));
            g2.fillOval(cx - 38, cy - 38, 76, 76);
            g2.setColor(new Color(60, 60, 60));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - 38, cy - 38, 76, 76);

            // Horn / logo
            g2.setColor(car.color);
            g2.fillOval(cx - 20, cy - 20, 40, 40);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            FontMetrics fm = g2.getFontMetrics();
            String logo = "SC";
            g2.drawString(logo, cx - fm.stringWidth(logo) / 2, cy + 6);

            // Thumb grips
            g2.setColor(new Color(40, 35, 35));
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND));
            g2.drawArc(cx - R + 20, cy - R + 15, 40, 30, 200, 90);
            g2.drawArc(cx + R - 60, cy - R + 15, 40, 30, -110, 90);

            g2.setTransform(old);
        }

        // ---------- Indicator lights ----------
        void drawIndicators(Graphics2D g2, int vx, int vy, int vw, int vh, Car car) {
            int panelY = vy + (int)(vh * 0.66);
            int panelX = vx + 30;
            g2.setFont(new Font("Arial", Font.BOLD, 11));

            // Left indicators
            boolean[] lights = {car.speed > 0, car.braking, false};
            Color[] colors = {new Color(60, 200, 60), new Color(255, 60, 60), new Color(255, 200, 60)};
            String[] labels = {"PWR", "BRK", "TRN"};
            for (int i = 0; i < 3; i++) {
                int lx = panelX + i * 42;
                g2.setColor(lights[i] ? colors[i] : new Color(40, 40, 40));
                g2.fillOval(lx, panelY, 16, 16);
                g2.setColor(lights[i] ? colors[i].brighter() : new Color(60, 60, 60));
                g2.fillOval(lx + 4, panelY + 4, 8, 8);
                g2.setColor(new Color(180, 180, 180));
                g2.drawString(labels[i], lx - 2, panelY + 30);
            }

            // Right side — NOS / lap
            g2.setFont(new Font("Consolas", Font.BOLD, 18));
            g2.setColor(new Color(0, 220, 255));
            String lapStr = "LAP " + Math.max(1, (int)(worldZ / 1000) + 1);
            g2.drawString(lapStr, vx + vw - 120, panelY + 20);

            g2.setFont(new Font("Arial", Font.BOLD, 11));
            g2.setColor(new Color(180, 180, 180));
            String distStr = (int)(worldZ) + " m";
            g2.drawString(distStr, vx + vw - 120, panelY + 40);
        }

        // ---------- Cockpit frame (A-pillars, roof) ----------
        void drawCockpitFrame(Graphics2D g2, int vx, int vy, int vw, int vh, int dashTop) {
            // Roof
            g2.setColor(new Color(15, 12, 12));
            g2.fillRect(vx, vy, vw, (int)(vh * 0.10));

            // A-pillars
            Path2D leftPillar = new Path2D.Double();
            leftPillar.moveTo(vx, vy + vh * 0.10);
            leftPillar.lineTo(vx + vw * 0.10, vy + vh * 0.10);
            leftPillar.lineTo(vx + vw * 0.06, dashTop);
            leftPillar.lineTo(vx, dashTop);
            leftPillar.closePath();
            g2.setColor(new Color(15, 12, 12));
            g2.fill(leftPillar);

            Path2D rightPillar = new Path2D.Double();
            rightPillar.moveTo(vx + vw, vy + vh * 0.10);
            rightPillar.lineTo(vx + vw * 0.90, vy + vh * 0.10);
            rightPillar.lineTo(vx + vw * 0.94, dashTop);
            rightPillar.lineTo(vx + vw, dashTop);
            rightPillar.closePath();
            g2.fill(rightPillar);

            // Pillar highlights
            g2.setColor(new Color(60, 55, 55));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(vx + vw * 0.10, vy + vh * 0.10, vx + vw * 0.06, dashTop);
            g2.drawLine(vx + vw * 0.90, vy + vh * 0.10, vx + vw * 0.94, dashTop);

            // Rear-view mirror
            int mirrorW = (int)(vw * 0.28);
            int mirrorH = 60;
            int mirrorX = vx + vw / 2 - mirrorW / 2;
            int mirrorY = vy + (int)(vh * 0.06);

            g2.setColor(new Color(20, 18, 18));
            g2.fillRoundRect(mirrorX - 8, mirrorY - 8, mirrorW + 16, mirrorH + 16, 20, 20);
            g2.setColor(new Color(50, 45, 45));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(mirrorX - 8, mirrorY - 8, mirrorW + 16, mirrorH + 16, 20, 20);

            // Mirror glass — shows rear view (simplified)
            g2.setColor(new Color(80, 100, 120));
            g2.fillRoundRect(mirrorX, mirrorY, mirrorW, mirrorH, 10, 10);
            GradientPaint mirrorSky = new GradientPaint(0, mirrorY, new Color(120, 100, 130),
                    0, mirrorY + mirrorH, new Color(40, 30, 50));
            g2.setPaint(mirrorSky);
            g2.fillRoundRect(mirrorX + 3, mirrorY + 3, mirrorW - 6, mirrorH - 6, 8, 8);

            // Rear car shape in mirror
            g2.setColor(new Color(220, 40, 40));
            int rcx = mirrorX + mirrorW / 2;
            int rcy = mirrorY + mirrorH - 15;
            g2.fillRoundRect(rcx - 25, rcy - 10, 50, 14, 6, 6);
            g2.setColor(new Color(180, 30, 30));
            g2.fillRoundRect(rcx - 15, rcy - 16, 30, 10, 4, 4);
            g2.setColor(Color.RED);
            g2.fillRect(rcx - 22, rcy - 4, 10, 4);
            g2.fillRect(rcx + 12, rcy - 4, 10, 4);

            // Mirror glare
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillPolygon(new int[]{mirrorX + 10, mirrorX + 60, mirrorX + 30, mirrorX},
                    new int[]{mirrorY, mirrorY, mirrorY + mirrorH, mirrorY + mirrorH}, 4);
        }

        // ---------- AI rear-view (small corner mirror for vs Computer) ----------
        void drawAIRearView(Graphics2D g2) {
            int mw = 220, mh = 90;
            int mx = WIDTH - mw - 30;
            int my = 30;

            g2.setColor(new Color(20, 18, 18));
            g2.fillRoundRect(mx - 6, my - 6, mw + 12, mh + 12, 14, 14);
            g2.setColor(new Color(255, 180, 40));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(mx - 6, my - 6, mw + 12, mh + 12, 14, 14);

            g2.setColor(new Color(50, 60, 80));
            g2.fillRoundRect(mx, my, mw, mh, 10, 10);

            // AI car in mirror (blue car ahead)
            g2.setColor(new Color(60, 130, 240));
            int rcx = mx + mw / 2 + (int)(p2.x * 60);
            int rcy = my + mh - 25;
            g2.fillRoundRect(rcx - 30, rcy - 12, 60, 16, 8, 8);
            g2.setColor(new Color(40, 100, 200));
            g2.fillRoundRect(rcx - 18, rcy - 20, 36, 12, 6, 6);
            g2.setColor(Color.RED);
            g2.fillRect(rcx - 27, rcy - 6, 12, 5);
            g2.fillRect(rcx + 15, rcy - 6, 12, 5);

            // Label
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.setColor(new Color(255, 180, 40));
            g2.drawString("COMPUTER", mx, my - 12);
        }

        // ---------- Menu ----------
        void drawMenu(Graphics2D g2) {
            // Background
            GradientPaint bg = new GradientPaint(0, 0, new Color(10, 10, 30),
                    0, HEIGHT, new Color(40, 20, 60));
            g2.setPaint(bg);
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            // Grid floor effect
            g2.setColor(new Color(80, 60, 140, 100));
            g2.setStroke(new BasicStroke(1f));
            int vpX = WIDTH / 2, vpY = HEIGHT / 2;
            for (int i = -20; i <= 20; i++) {
                g2.drawLine(vpX, vpY, vpX + i * 120, HEIGHT);
            }
            for (int y = vpY; y < HEIGHT; y += 30) {
                int alpha = 100 - (y - vpY) / 3;
                if (alpha > 0) {
                    g2.setColor(new Color(80, 60, 140, alpha));
                    g2.drawLine(0, y, WIDTH, y);
                }
            }

            long t = System.currentTimeMillis();
            int pulse = (int)(Math.sin(t / 300.0) * 20 + 200);

            g2.setFont(new Font("Arial Black", Font.BOLD, 84));
            FontMetrics fm = g2.getFontMetrics();
            String title = "SUPERCAR";
            int tx = (WIDTH - fm.stringWidth(title)) / 2;
            g2.setColor(new Color(255, 100, 40, 100));
            g2.drawString(title, tx - 5, HEIGHT / 2 - 130);
            g2.setColor(new Color(255, 220, 80));
            g2.drawString(title, tx, HEIGHT / 2 - 135);

            g2.setFont(new Font("Arial", Font.BOLD, 28));
            fm = g2.getFontMetrics();
            String sub = "— DRIVER VIEW —";
            g2.setColor(new Color(180, 220, 255));
            g2.drawString(sub, (WIDTH - fm.stringWidth(sub)) / 2, HEIGHT / 2 - 80);

            g2.setFont(new Font("Arial", Font.BOLD, 28));
            fm = g2.getFontMetrics();
            int oy = HEIGHT / 2 + 20;
            String o1 = "[ 1 ]  Race vs COMPUTER";
            String o2 = "[ 2 ]  Race 2 PLAYERS";

            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            if (blink) {
                g2.setColor(new Color(80, 255, 120));
                g2.drawString(o1, (WIDTH - fm.stringWidth(o1)) / 2, oy);
            }
            g2.setColor(new Color(80, 200, 255));
            g2.drawString(o2, (WIDTH - fm.stringWidth(o2)) / 2, oy + 50);

            g2.setFont(new Font("Arial", Font.PLAIN, 15));
            fm = g2.getFontMetrics();
            g2.setColor(new Color(220, 220, 220));
            String c1 = "Player 1: ↑ ↓ ← →     •     Player 2: W S A D";
            String c2 = "First-person cockpit • Avoid obstacles • Drive as far as you can";
            g2.drawString(c1, (WIDTH - fm.stringWidth(c1)) / 2, HEIGHT - 110);
            g2.drawString(c2, (WIDTH - fm.stringWidth(c2)) / 2, HEIGHT - 85);
        }

        // ---------- Countdown ----------
        void drawCountdown(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            String t = countdown > 0 ? String.valueOf(countdown) : "GO!";
            g2.setFont(new Font("Arial Black", Font.BOLD, 200));
            FontMetrics fm = g2.getFontMetrics();
            int x = (WIDTH - fm.stringWidth(t)) / 2;
            int y = HEIGHT / 2 + fm.getAscent() / 3;
            g2.setColor(new Color(255, 220, 80, 100));
            g2.drawString(t, x - 5, y - 5);
            g2.drawString(t, x + 5, y + 5);
            g2.setColor(countdown > 0 ? new Color(255, 220, 80) : new Color(80, 255, 120));
            g2.drawString(t, x, y);
        }

        // ---------- Results ----------
        void drawResults(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            g2.setFont(new Font("Arial Black", Font.BOLD, 80));
            FontMetrics fm = g2.getFontMetrics();
            String title = "💥 CRASH! 💥";
            g2.setColor(Color.RED);
            g2.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, HEIGHT / 2 - 70);

            g2.setFont(new Font("Arial", Font.BOLD, 28));
            fm = g2.getFontMetrics();
            String info = "You drove " + (int)worldZ + " meters!";
            g2.setColor(Color.WHITE);
            g2.drawString(info, (WIDTH - fm.stringWidth(info)) / 2, HEIGHT / 2);

            g2.setFont(new Font("Arial", Font.BOLD, 22));
            fm = g2.getFontMetrics();
            g2.setColor(new Color(200, 200, 200));
            String hint = "Press R to return to menu";
            g2.drawString(hint, (WIDTH - fm.stringWidth(hint)) / 2, HEIGHT / 2 + 70);
        }
    }

    // ============================================================
    //  CAR
    // ============================================================
    static class Car {
        String name;
        Color color;
        double x = 0;
        double speed = 0;
        double steer = 0;
        boolean braking = false;

        Car(String name, Color color) { this.name = name; this.color = color; }

        void reset() { x = 0; speed = 0; steer = 0; braking = false; }

        void control(boolean up, boolean down, boolean left, boolean right, double dt) {
            double accel = 4.0 * dt;
            double brake = 8.0 * dt;
            double friction = 1.6 * dt;
            double steerRate = 1.6 * dt;

            if (up)   speed += accel;
            if (down) { speed -= brake; braking = true; } else braking = false;
            if (!up && !down) speed = Math.max(0, speed - friction);
            speed = Math.max(0, Math.min(6, speed));

            if (left)  steer = Math.max(-1, steer - steerRate * 3);
            else if (right) steer = Math.min(1, steer + steerRate * 3);
            else steer *= 0.88;

            x += steer * steerRate * (0.25 + speed / 12.0);
            x = Math.max(-0.85, Math.min(0.85, x));
        }
    }

    // ============================================================
    //  WORLD OBJECT
    // ============================================================
    static class WorldObject {
        enum Kind { OBSTACLE, TREE, POLE, BILLBOARD }
        Kind kind;
        double z;
        double x;
        Color color;
    }

    // ============================================================
    //  PARTICLE
    // ============================================================
    static class Particle {
        double x, y, vx, vy, life;
        Color color;
        double size;
    }
}