import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;


public class SupercarRacingChallenge3D extends JFrame {

    public SupercarRacingChallenge3D() {
        setTitle("🏆 Supercar Racing Challenge — 3D 🏁");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(new GamePanel());
        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SupercarRacingChallenge3D().setVisible(true));
    }

    // ================================================================
    //  GAME PANEL
    // ================================================================
    static class GamePanel extends JPanel {

        // --- Screen ---
        static final int WIDTH = 1100;
        static final int HEIGHT = 700;
        static final int HORIZON = 240;          // y of horizon line
        static final int ROAD_WIDTH_NEAR = 1400; // projected width at bottom
        static final int ROAD_WIDTH_FAR = 120;   // projected width at horizon

        // --- Game state ---
        enum State { MENU, COUNTDOWN, RACING, FINISHED }
        State state = State.MENU;
        boolean vsComputer = true;
        int countdown = 3;
        int countdownTick = 0;

        // --- World ---
        double worldZ = 0;             // how far we've driven (in "meters")
        double curve = 0;              // current road curvature (-1..1)
        double targetCurve = 0;
        double hill = 0;               // vertical road offset (for hills)
        double targetHill = 0;

        // --- Player cars ---
        Car p1 = new Car("PLAYER 1", new Color(230, 40, 40));
        Car p2 = new Car("PLAYER 2", new Color(60, 130, 240));
        boolean twoPlayer = false;

        // --- AI ---
        double aiTargetX = 0;
        int aiDecisionTimer = 0;

        // --- World objects (obstacles, scenery) ---
        List<WorldObject> objects = new ArrayList<>();
        int objectSpawnZ = 400;

        // --- Speed / camera shake ---
        double shakeX = 0, shakeY = 0;

        // --- Particles (exhaust, sparks) ---
        List<Particle> particles = new ArrayList<>();

        // --- Keys ---
        Set<Integer> keys = new HashSet<>();

        // --- Timing ---
        long lastTime = System.nanoTime();

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setBackground(Color.BLACK);
            setFocusable(true);
            requestFocusInWindow();

            // Generate first batch of objects
            for (int i = 0; i < 200; i++) spawnObject(200 + i * 60);

            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    keys.add(e.getKeyCode());
                    if (state == State.MENU) {
                        if (e.getKeyCode() == KeyEvent.VK_1) { vsComputer = true;  twoPlayer = false; startRace(); }
                        if (e.getKeyCode() == KeyEvent.VK_2) { vsComputer = false; twoPlayer = true;  startRace(); }
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
            p1.reset();
            p2.reset();
            worldZ = 0;
            curve = 0; targetCurve = 0;
            hill = 0; targetHill = 0;
            objects.clear();
            for (int i = 0; i < 200; i++) spawnObject(200 + i * 60);
            particles.clear();
            state = State.COUNTDOWN;
            countdown = 3;
            countdownTick = 0;
        }

        void spawnObject(double z) {
            Random r = new Random();
            int type = r.nextInt(10);
            WorldObject o = new WorldObject();
            o.z = z;
            if (type < 3) {
                // Road obstacle
                o.kind = WorldObject.Kind.OBSTACLE;
                o.x = -0.55 + r.nextDouble() * 1.1; // normalized offset
                o.color = new Color(200 + r.nextInt(55), 80 + r.nextInt(80), 40);
            } else if (type < 7) {
                // Roadside scenery (tree/pole)
                o.kind = r.nextBoolean() ? WorldObject.Kind.TREE : WorldObject.Kind.POLE;
                o.x = (r.nextBoolean() ? -1.1 : 1.1) - (r.nextDouble() * 0.3);
                o.color = o.kind == WorldObject.Kind.TREE
                        ? new Color(30, 120 + r.nextInt(80), 40)
                        : new Color(180, 180, 200);
            } else {
                // Billboards / distant buildings
                o.kind = WorldObject.Kind.BILLBOARD;
                o.x = (r.nextBoolean() ? -1.4 : 1.4) - r.nextDouble() * 0.4;
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
                // ---- Input ----
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

                // ---- World advance (based on faster car's speed) ----
                double leadSpeed = Math.max(p1.speed, p2.speed);
                worldZ += leadSpeed * 40 * dt;

                // ---- Curve / hills evolve ----
                if (Math.random() < 0.005) targetCurve = (Math.random() - 0.5) * 1.6;
                if (Math.random() < 0.003) targetHill = (Math.random() - 0.5) * 1.2;
                curve += (targetCurve - curve) * dt * 0.6;
                hill  += (targetHill  - hill)  * dt * 0.6;

                // ---- Object recycling ----
                Iterator<WorldObject> it = objects.iterator();
                while (it.hasNext()) {
                    WorldObject o = it.next();
                    if (o.z < worldZ - 50) {
                        it.remove();
                        spawnObject(worldZ + 1200 + Math.random() * 400);
                    }
                }

                // ---- Collisions ----
                checkCollision(p1);
                checkCollision(p2);

                // ---- Exhaust particles ----
                if (Math.random() < 0.6) particles.add(exhaustFor(p1));
                if (Math.random() < 0.6) particles.add(exhaustFor(p2));

                // ---- Camera shake at high speed ----
                double sh = Math.max(p1.speed, p2.speed) * 0.3;
                shakeX = (Math.random() - 0.5) * sh;
                shakeY = (Math.random() - 0.5) * sh;
            }

            // ---- Particles ----
            Iterator<Particle> pit = particles.iterator();
            while (pit.hasNext()) {
                Particle p = pit.next();
                p.x += p.vx * dt * 60;
                p.y += p.vy * dt * 60;
                p.life -= dt * 60;
                if (p.life <= 0) pit.remove();
            }

            repaint();
        }

        void updateAI(double dt) {
            aiDecisionTimer++;
            // Steer towards lane center, dodge obstacles
            double desiredX = 0;
            // Find nearest obstacle ahead
            WorldObject nearest = null;
            double bestZ = Double.MAX_VALUE;
            for (WorldObject o : objects) {
                if (o.kind == WorldObject.Kind.OBSTACLE && o.z > worldZ + 30 && o.z - worldZ < bestZ) {
                    bestZ = o.z - worldZ;
                    nearest = o;
                }
            }
            if (nearest != null && bestZ < 400) {
                // Dodge: aim opposite side
                desiredX = nearest.x > 0 ? -0.5 : 0.5;
            } else {
                if (aiDecisionTimer > 120) {
                    aiDecisionTimer = 0;
                    aiTargetX = (Math.random() - 0.5) * 0.8;
                }
                desiredX = aiTargetX;
            }

            boolean left  = p2.x > desiredX + 0.05;
            boolean right = p2.x < desiredX - 0.05;
            boolean up    = Math.abs(p2.x - desiredX) < 0.6;
            boolean down  = false;
            if (Math.abs(p2.x - desiredX) > 0.9 && p2.speed > 3) down = true;

            p2.control(up, down, left, right, dt);
        }

        void checkCollision(Car car) {
            for (WorldObject o : objects) {
                if (o.kind != WorldObject.Kind.OBSTACLE) continue;
                double dz = Math.abs(o.z - worldZ - 40);
                if (dz < 15) {
                    if (Math.abs(o.x - car.x) < 0.16) {
                        // Crash!
                        state = State.FINISHED;
                        for (int i = 0; i < 60; i++) {
                            Particle p = new Particle();
                            p.x = WIDTH / 2;
                            p.y = HEIGHT * 0.7;
                            double a = Math.random() * Math.PI * 2;
                            double s = 2 + Math.random() * 6;
                            p.vx = Math.cos(a) * s;
                            p.vy = Math.sin(a) * s;
                            p.life = 40 + Math.random() * 30;
                            p.color = car == p1 ? new Color(230, 40, 40) : new Color(60, 130, 240);
                            p.size = 3 + Math.random() * 5;
                            particles.add(p);
                        }
                    }
                }
            }
        }

        Particle exhaustFor(Car car) {
            Particle p = new Particle();
            p.x = WIDTH / 2 + car.x * 320 + (Math.random() - 0.5) * 30;
            p.y = HEIGHT - 80 + (Math.random() - 0.5) * 20;
            p.vx = (Math.random() - 0.5) * 1.5;
            p.vy = -1 - Math.random() * 2;
            p.life = 20 + Math.random() * 15;
            p.color = car.speed > 4 ? new Color(255, 200, 80, 200) : new Color(200, 200, 200, 150);
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

            // Apply camera shake
            AffineTransform orig = g2.getTransform();
            g2.translate(shakeX, shakeY);

            drawSky(g2);
            drawGround(g2);

            if (state == State.MENU) {
                drawMenu(g2);
            } else {
                drawRoad(g2);
                drawObjects(g2);
                drawParticles(g2);
                drawCars(g2);
                drawHUD(g2);
                if (state == State.COUNTDOWN) drawCountdown(g2);
                if (state == State.FINISHED) drawResults(g2);
            }

            g2.setTransform(orig);
        }

        // ---------- Sky & ground ----------
        void drawSky(Graphics2D g2) {
            // Sunset gradient
            GradientPaint sky = new GradientPaint(0, 0, new Color(20, 20, 60),
                    0, HORIZON, new Color(255, 140, 90));
            g2.setPaint(sky);
            g2.fillRect(0, 0, WIDTH, HORIZON);

            // Sun
            g2.setColor(new Color(255, 220, 120, 220));
            g2.fillOval(WIDTH / 2 - 60, HORIZON - 90, 120, 120);
            g2.setColor(new Color(255, 240, 180, 100));
            g2.fillOval(WIDTH / 2 - 90, HORIZON - 120, 180, 180);

            // Stars (upper sky)
            Random r = new Random(7);
            g2.setColor(new Color(255, 255, 255, 160));
            for (int i = 0; i < 60; i++) {
                int sx = r.nextInt(WIDTH);
                int sy = r.nextInt(HORIZON - 80);
                g2.fillOval(sx, sy, 2, 2);
            }

            // Distant mountains / skyline (parallax with curve)
            int parallax = (int)(curve * 60);
            g2.setColor(new Color(40, 30, 70));
            int baseY = HORIZON - 10;
            for (int i = -1; i < WIDTH / 60 + 1; i++) {
                int h = 40 + (int)(Math.sin(i * 0.7) * 30) + (i % 3) * 20;
                int x = i * 60 + parallax;
                Polygon mtn = new Polygon(
                        new int[]{x - 30, x + 30, x + 90},
                        new int[]{baseY, baseY - h, baseY}, 3);
                g2.fillPolygon(mtn);
            }

            // Horizon line glow
            g2.setColor(new Color(255, 200, 150, 120));
            g2.fillRect(0, HORIZON - 2, WIDTH, 4);
        }

        void drawGround(Graphics2D g2) {
            // Dark ground below horizon
            GradientPaint ground = new GradientPaint(0, HORIZON, new Color(60, 40, 80),
                    0, HEIGHT, new Color(15, 10, 30));
            g2.setPaint(ground);
            g2.fillRect(0, HORIZON, WIDTH, HEIGHT - HORIZON);
        }

        // ---------- Road (perspective projection) ----------
        void drawRoad(Graphics2D g2) {
            int segments = 120;
            double totalDepth = 3000;

            // We draw from far to near so near segments overlap
            for (int i = segments; i >= 0; i--) {
                double z0 = i * (totalDepth / segments);
                double z1 = (i + 1) * (totalDepth / segments);
                double zRel0 = z0 - (worldZ % (totalDepth / segments));
                double zRel1 = z1 - (worldZ % (totalDepth / segments));

                if (zRel0 < 5) continue;

                double scale0 = 1.0 / zRel0;
                double scale1 = 1.0 / zRel1;

                // Apply curve & hill offsets
                double curveOffset0 = curve * 200 * scale0 * zRel0 * 0.005;
                double curveOffset1 = curve * 200 * scale1 * zRel1 * 0.005;
                double hillOffset0 = hill * 60 * scale0 * zRel0 * 0.03;
                double hillOffset1 = hill * 60 * scale1 * zRel1 * 0.03;

                int y0 = (int)(HORIZON + (HEIGHT - HORIZON) * (1 - scale0) + hillOffset0);
                int y1 = (int)(HORIZON + (HEIGHT - HORIZON) * (1 - scale1) + hillOffset1);

                int w0 = (int)(ROAD_WIDTH_FAR + (ROAD_WIDTH_NEAR - ROAD_WIDTH_FAR) * (1 - scale0));
                int w1 = (int)(ROAD_WIDTH_FAR + (ROAD_WIDTH_NEAR - ROAD_WIDTH_FAR) * (1 - scale1));

                int cx = WIDTH / 2;
                int cx0 = cx + (int)curveOffset0;
                int cx1 = cx + (int)curveOffset1;

                // Curb (outer white/red stripes)
                int curbW0 = w0 + 40;
                int curbW1 = w1 + 40;
                g2.setColor((i / 2) % 2 == 0 ? new Color(220, 40, 40) : Color.WHITE);
                g2.fillPolygon(
                        new int[]{cx0 - curbW0 / 2, cx0 + curbW0 / 2, cx1 + curbW1 / 2, cx1 - curbW1 / 2},
                        new int[]{y0, y0, y1, y1}, 4);

                // Asphalt
                g2.setColor((i / 4) % 2 == 0 ? new Color(50, 50, 55) : new Color(45, 45, 50));
                g2.fillPolygon(
                        new int[]{cx0 - w0 / 2, cx0 + w0 / 2, cx1 + w1 / 2, cx1 - w1 / 2},
                        new int[]{y0, y0, y1, y1}, 4);

                // Lane markings (dashed, only in middle)
                if (i % 2 == 0) {
                    int lw0 = Math.max(2, w0 / 40);
                    int lw1 = Math.max(2, w1 / 40);
                    g2.setColor(new Color(255, 240, 200, 180));
                    g2.fillPolygon(
                            new int[]{cx0 - lw0 / 2, cx0 + lw0 / 2, cx1 + lw1 / 2, cx1 - lw1 / 2},
                            new int[]{y0, y0, y1, y1}, 4);
                }

                // Edge lines
                g2.setColor(new Color(255, 255, 255, 200));
                int edge = 4;
                g2.fillPolygon(new int[]{cx0 - w0 / 2, cx0 - w0 / 2 + edge, cx1 - w1 / 2 + edge, cx1 - w1 / 2},
                        new int[]{y0, y0, y1, y1}, 4);
                g2.fillPolygon(new int[]{cx0 + w0 / 2 - edge, cx0 + w0 / 2, cx1 + w1 / 2, cx1 + w1 / 2 - edge},
                        new int[]{y0, y0, y1, y1}, 4);
            }
        }

        // ---------- Objects ----------
        void drawObjects(Graphics2D g2) {
            // Sort by z descending (far first)
            List<WorldObject> sorted = new ArrayList<>(objects);
            sorted.sort((a, b) -> Double.compare(b.z, a.z));

            for (WorldObject o : sorted) {
                double zRel = o.z - worldZ;
                if (zRel < 5 || zRel > 2500) continue;

                double scale = 1.0 / zRel;
                double curveOffset = curve * 200 * scale * zRel * 0.005;
                double hillOffset = hill * 60 * scale * zRel * 0.03;

                int y = (int)(HORIZON + (HEIGHT - HORIZON) * (1 - scale) + hillOffset);
                int cx = WIDTH / 2 + (int)curveOffset;
                int w = (int)(ROAD_WIDTH_FAR + (ROAD_WIDTH_NEAR - ROAD_WIDTH_FAR) * (1 - scale));
                int x = cx + (int)(o.x * w / 2);

                int size = (int)(scale * 900);
                if (size < 2) size = 2;
                if (size > 300) size = 300;

                switch (o.kind) {
                    case OBSTACLE -> {
                        // Perspective box
                        int bw = size / 3;
                        int bh = size / 2;
                        g2.setColor(new Color(0, 0, 0, 100));
                        g2.fillRect(x - bw / 2 + 4, y - bh + 4, bw, bh);
                        g2.setColor(o.color);
                        g2.fillRect(x - bw / 2, y - bh, bw, bh);
                        // Highlight
                        g2.setColor(o.color.brighter());
                        g2.fillRect(x - bw / 2, y - bh, bw, bh / 4);
                        // Warning stripes
                        g2.setColor(Color.YELLOW);
                        g2.fillRect(x - bw / 2, y - bh + bh / 2, bw, Math.max(2, bh / 12));
                    }
                    case TREE -> {
                        int tw = size / 6;
                        int th = size;
                        g2.setColor(new Color(80, 50, 30));
                        g2.fillRect(x - tw / 4, y - th / 3, tw / 2, th / 3);
                        g2.setColor(o.color);
                        g2.fillOval(x - tw, y - th, tw * 2, (int)(th * 0.8));
                        g2.setColor(o.color.brighter());
                        g2.fillOval(x - tw + tw / 3, y - th + th / 4, tw / 2, tw / 2);
                    }
                    case POLE -> {
                        int pw = Math.max(2, size / 20);
                        int ph = size;
                        g2.setColor(o.color);
                        g2.fillRect(x - pw / 2, y - ph, pw, ph);
                        g2.setColor(Color.WHITE);
                        g2.fillRect(x - pw * 2, y - ph, pw * 4, Math.max(2, ph / 20));
                    }
                    case BILLBOARD -> {
                        int bw = size;
                        int bh = size / 2;
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

        // ---------- Particles ----------
        void drawParticles(Graphics2D g2) {
            for (Particle p : particles) {
                float alpha = (float) Math.max(0, Math.min(1, p.life / 40f));
                Color c = p.color;
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(alpha * c.getAlpha())));
                g2.fillOval((int)(p.x - p.size), (int)(p.y - p.size), (int)(p.size * 2), (int)(p.size * 2));
            }
        }

        // ---------- Cars (pseudo-3D sprites) ----------
        void drawCars(Graphics2D g2) {
            // Player 1 (blue/red mix) — larger, closer
            drawCarSprite(g2, p1, WIDTH / 2 + (int)(p1.x * 320), HEIGHT - 110, 1.0);

            if (twoPlayer || vsComputer) {
                // Player 2 — drawn slightly higher & smaller (further ahead)
                drawCarSprite(g2, p2, WIDTH / 2 + (int)(p2.x * 320), HEIGHT - 145, 0.88);
            }
        }

        void drawCarSprite(Graphics2D g2, Car car, int cx, int cy, double scale) {
            AffineTransform old = g2.getTransform();
            g2.translate(cx, cy);
            g2.scale(scale, scale);

            // Tilt based on steering (fake)
            double tilt = (car.steer > 0 ? -0.05 : car.steer < 0 ? 0.05 : 0);
            g2.rotate(tilt);

            int w = 130, h = 55;

            // Shadow
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillOval(-w / 2, h / 2 - 6, w, 18);

            // Rear body (perspective-ish)
            GradientPaint bodyGrad = new GradientPaint(0, -h / 2, car.color.brighter(),
                    0, h / 2, car.color.darker());
            g2.setPaint(bodyGrad);
            g2.fillRoundRect(-w / 2, -h / 2, w, h, 20, 20);

            // Roof / cabin
            g2.setColor(car.color.darker());
            g2.fillRoundRect(-w / 4, -h / 2 - 14, w / 2, 18, 12, 12);

            // Rear window
            g2.setColor(new Color(60, 100, 140, 220));
            g2.fillRoundRect(-w / 4 + 4, -h / 2 - 12, w / 2 - 8, 14, 10, 10);

            // License plate area
            g2.setColor(new Color(230, 230, 230));
            g2.fillRoundRect(-20, -2, 40, 12, 4, 4);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 9));
            g2.drawString("SUPER", -18, 7);

            // Taillights
            g2.setColor(Color.RED);
            g2.fillRoundRect(-w / 2 + 6, -h / 2 + 6, 22, 10, 4, 4);
            g2.fillRoundRect(w / 2 - 28, -h / 2 + 6, 22, 10, 4, 4);
            g2.setColor(new Color(255, 200, 200, 200));
            g2.fillRoundRect(-w / 2 + 8, -h / 2 + 8, 18, 6, 3, 3);
            g2.fillRoundRect(w / 2 - 26, -h / 2 + 8, 18, 6, 3, 3);

            // Tail glow when braking
            if (car.braking) {
                g2.setColor(new Color(255, 60, 60, 120));
                g2.fillOval(-w / 2 - 5, -h / 2, 30, 25);
                g2.fillOval(w / 2 - 25, -h / 2, 30, 25);
            }

            // Rear wheels
            g2.setColor(Color.BLACK);
            g2.fillRoundRect(-w / 2 - 6, -6, 16, 22, 6, 6);
            g2.fillRoundRect(w / 2 - 10, -6, 16, 22, 6, 6);
            g2.setColor(new Color(80, 80, 80));
            g2.fillRoundRect(-w / 2 - 4, -2, 12, 14, 4, 4);
            g2.fillRoundRect(w / 2 - 8, -2, 12, 14, 4, 4);

            // Spoiler
            g2.setColor(car.color.darker().darker());
            g2.fillRoundRect(-w / 2 - 6, -h / 2 - 26, w + 12, 8, 4, 4);
            g2.fillRect(-w / 2 - 2, -h / 2 - 20, 6, 6);
            g2.fillRect(w / 2 - 4, -h / 2 - 20, 6, 6);

            // Label
            g2.setTransform(old);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(car.name, cx - fm.stringWidth(car.name) / 2, cy + 45);
        }

        // ---------- HUD ----------
        void drawHUD(Graphics2D g2) {
            g2.setFont(new Font("Arial", Font.BOLD, 16));

            // Player 1 speedometer
            drawSpeedometer(g2, 30, HEIGHT - 130, p1, "PLAYER 1", new Color(230, 60, 60));

            // Player 2 / CPU speedometer
            drawSpeedometer(g2, WIDTH - 230, HEIGHT - 130, p2,
                    twoPlayer ? "PLAYER 2" : "COMPUTER",
                    twoPlayer ? new Color(60, 130, 240) : new Color(255, 180, 40));
        }

        void drawSpeedometer(Graphics2D g2, int x, int y, Car car, String label, Color color) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(x, y, 200, 110, 14, 14);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, 200, 110, 14, 14);

            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.setColor(color);
            g2.drawString(label, x + 12, y + 22);

            // Speed number
            g2.setFont(new Font("Arial", Font.BOLD, 32));
            g2.setColor(Color.WHITE);
            g2.drawString(String.valueOf((int)(car.speed * 40)), x + 12, y + 62);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.drawString("km/h", x + 80, y + 62);

            // Speed bar
            int bx = x + 12, by = y + 80, bw = 176, bh = 12;
            g2.setColor(new Color(40, 40, 40));
            g2.fillRoundRect(bx, by, bw, bh, 6, 6);
            int fill = (int)(Math.min(1.0, car.speed / 6.0) * bw);
            GradientPaint gp = new GradientPaint(bx, by, new Color(80, 220, 80),
                    bx + bw, by, new Color(255, 60, 60));
            g2.setPaint(gp);
            g2.fillRoundRect(bx, by, fill, bh, 6, 6);

            // Tachometer needle
            double needle = Math.min(1.0, car.speed / 6.0) * Math.PI * 0.75 + Math.PI * 1.125;
            int mx = x + 150, my = y + 60, mr = 40;
            g2.setColor(new Color(255, 255, 255, 80));
            g2.setStroke(new BasicStroke(2f));
            g2.drawArc(mx - mr, my - mr, mr * 2, mr * 2,
                    (int)Math.toDegrees(Math.PI * 1.125),
                    (int)Math.toDegrees(Math.PI * 0.75));
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(3f));
            int nx = mx + (int)(Math.cos(needle) * mr);
            int ny = my + (int)(Math.sin(needle) * mr);
            g2.drawLine(mx, my, nx, ny);
        }

        void drawCountdown(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 130));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            String t = countdown > 0 ? String.valueOf(countdown) : "GO!";
            g2.setFont(new Font("Arial", Font.BOLD, 180));
            FontMetrics fm = g2.getFontMetrics();
            int x = (WIDTH - fm.stringWidth(t)) / 2;
            int y = HEIGHT / 2 + fm.getAscent() / 3;
            g2.setColor(new Color(255, 220, 80, 80));
            g2.drawString(t, x - 4, y - 4);
            g2.drawString(t, x + 4, y + 4);
            g2.setColor(countdown > 0 ? new Color(255, 220, 80) : new Color(80, 255, 120));
            g2.drawString(t, x, y);
        }

        void drawResults(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            g2.setFont(new Font("Arial", Font.BOLD, 60));
            FontMetrics fm = g2.getFontMetrics();
            String title = "💥 CRASH! 💥";
            g2.setColor(Color.RED);
            g2.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, HEIGHT / 2 - 60);

            g2.setFont(new Font("Arial", Font.BOLD, 26));
            fm = g2.getFontMetrics();
            String info = "You hit an obstacle at " + (int)(worldZ) + " meters!";
            g2.setColor(Color.WHITE);
            g2.drawString(info, (WIDTH - fm.stringWidth(info)) / 2, HEIGHT / 2 + 10);

            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            fm = g2.getFontMetrics();
            String hint = "Press R to return to menu";
            g2.setColor(new Color(200, 200, 200));
            g2.drawString(hint, (WIDTH - fm.stringWidth(hint)) / 2, HEIGHT / 2 + 70);
        }

        void drawMenu(Graphics2D g2) {
            long t = System.currentTimeMillis();
            int pulse = (int)(Math.sin(t / 300.0) * 20 + 200);

            g2.setFont(new Font("Arial", Font.BOLD, 72));
            FontMetrics fm = g2.getFontMetrics();
            String title = "SUPERCAR 3D";
            int tx = (WIDTH - fm.stringWidth(title)) / 2;
            int ty = HEIGHT / 2 - 100;

            g2.setColor(new Color(255, 100, 40, 120));
            g2.drawString(title, tx - 4, ty - 4);
            g2.setColor(new Color(255, 220, 80));
            g2.drawString(title, tx, ty);

            g2.setFont(new Font("Arial", Font.BOLD, 28));
            fm = g2.getFontMetrics();
            String sub = "RACING CHALLENGE";
            g2.setColor(new Color(200, 220, 255));
            g2.drawString(sub, (WIDTH - fm.stringWidth(sub)) / 2, ty + 45);

            g2.setFont(new Font("Arial", Font.BOLD, 26));
            fm = g2.getFontMetrics();
            int oy = HEIGHT / 2 + 30;
            String o1 = "[1]  Play vs Computer";
            String o2 = "[2]  Play 2 Players";

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
            String c1 = "Player 1: Arrow Keys  •  Player 2: WASD";
            String c2 = "Drive as far as you can. Avoid obstacles!";
            g2.drawString(c1, (WIDTH - fm.stringWidth(c1)) / 2, HEIGHT - 100);
            g2.drawString(c2, (WIDTH - fm.stringWidth(c2)) / 2, HEIGHT - 75);
        }
    }

    // ============================================================
    //  CAR
    // ============================================================
    static class Car {
        String name;
        Color color;
        double x = 0;           // -1..1 across road
        double speed = 0;       // 0..6
        double steer = 0;       // -1..1
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

            // Steering — faster at higher speed but not too twitchy
            double s = steerRate * (0.3 + speed / 6.0);
            if (left)  steer = Math.max(-1, steer - steerRate * 3);
            else if (right) steer = Math.min(1, steer + steerRate * 3);
            else steer *= 0.85;

            x += steer * s * 0.15;
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
        double x, y, vx, vy;
        double life;
        Color color;
        double size;
    }
}