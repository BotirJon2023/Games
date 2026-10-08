import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class SupercarRacingChallenge3D extends JFrame {

    public SupercarRacingChallenge3D() {
        setTitle("SUPERCAR RACING CHALLENGE 3D — 80s EDITION");
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
    //  VEC3
    // ================================================================
    static class Vec3 {
        double x, y, z;
        Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        Vec3 add(Vec3 o) { return new Vec3(x + o.x, y + o.y, z + o.z); }
        Vec3 sub(Vec3 o) { return new Vec3(x - o.x, y - o.y, z - o.z); }
        Vec3 mul(double s) { return new Vec3(x * s, y * s, z * s); }
        double dot(Vec3 o) { return x * o.x + y * o.y + z * o.z; }
        Vec3 cross(Vec3 o) { return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
        double length() { return Math.sqrt(x * x + y * y + z * z); }
        Vec3 normalize() { double l = length(); return l > 0 ? mul(1.0 / l) : new Vec3(0, 0, 0); }
        Vec3 rotateY(double a) {
            double c = Math.cos(a), s = Math.sin(a);
            return new Vec3(x * c + z * s, y, -x * s + z * c);
        }
    }

    // ================================================================
    //  PARTICLE
    // ================================================================
    static class Particle3D {
        Vec3 pos, vel;
        Color color;
        int life, maxLife;
        double size;
        Particle3D(Vec3 p, Vec3 v, Color c, int life) {
            pos = p; vel = v; color = c; this.life = life; this.maxLife = life;
            size = 3 + Math.random() * 3;
        }
        void update() {
            pos = pos.add(vel);
            vel.y += 0.08;
            vel = vel.mul(0.97);
            life--;
        }
    }

    // ================================================================
    //  CAR — 3D car with physics
    // ================================================================
    static class Car3D {
        Vec3 pos;
        double heading = 0;
        double speed = 0;
        int lap = 0;
        boolean onTrack = true;
        boolean passedStart = true;
        Color bodyColor, accentColor;
        String name;

        static final double MAX_SPEED = 3.5;
        static final double ACCEL = 0.08;
        static final double BRAKE = 0.12;
        static final double FRICTION = 0.015;
        static final double TURN = 0.025;

        static final double W = 2.5, L = 4.5, H = 1.5;

        Car3D(Vec3 pos, Color bodyColor, Color accentColor, String name) {
            this.pos = pos; this.bodyColor = bodyColor;
            this.accentColor = accentColor; this.name = name;
        }

        void update(boolean accel, boolean brake, boolean left, boolean right, GamePanel gp) {
            if (accel) speed += ACCEL;
            else if (brake) speed -= BRAKE;
            else {
                if (speed > 0) speed -= FRICTION;
                else if (speed < 0) speed += FRICTION;
                if (Math.abs(speed) < FRICTION) speed = 0;
            }
            if (speed > MAX_SPEED) speed = MAX_SPEED;
            if (speed < -MAX_SPEED / 2) speed = -MAX_SPEED / 2;
            if (!onTrack) speed *= 0.93;

            if (Math.abs(speed) > 0.1) {
                int nearest = gp.findNearestTrackPoint(pos.x, pos.z);
                int lookAhead = (nearest + 5) % gp.trackPath.size();
                Vec3 target = gp.trackPath.get(lookAhead);
                double targetAngle = Math.atan2(target.x - pos.x, target.z - pos.z);
                double diff = normalizeAngle(targetAngle - heading);
                double maxAssist = (!left && !right) ? 0.04 : 0.01;
                double assist = Math.signum(diff) * Math.min(Math.abs(diff), maxAssist);
                heading += assist;

                Vec3 nearestPt = gp.trackPath.get(nearest);
                double latX = pos.x - nearestPt.x;
                double latZ = pos.z - nearestPt.z;
                double latDist = Math.sqrt(latX * latX + latZ * latZ);
                if (latDist > 1.0) {
                    double pullStrength = 0.3 * Math.min(latDist / (gp.trackWidth / 2), 1.0);
                    pos.x -= latX / latDist * pullStrength;
                    pos.z -= latZ / latDist * pullStrength;
                }
            }

            if (Math.abs(speed) > 0.1) {
                double turnFactor = Math.min(Math.abs(speed) / 3, 1.0);
                double dir = speed > 0 ? 1 : -1;
                if (left) heading += TURN * turnFactor * dir;
                if (right) heading -= TURN * turnFactor * dir;
            }

            pos.x += Math.sin(heading) * speed;
            pos.z += Math.cos(heading) * speed;
        }

        double normalizeAngle(double a) {
            while (a > Math.PI) a -= 2 * Math.PI;
            while (a < -Math.PI) a += 2 * Math.PI;
            return a;
        }

        void updateY(GamePanel gp) {
            int nearest = gp.findNearestTrackPoint(pos.x, pos.z);
            double trackY = gp.trackPath.get(nearest).y;
            pos.y = trackY;
        }

        // ---- Build 3D polygons for rendering ----
        List<Poly3D> buildPolygons() {
            List<Poly3D> polys = new ArrayList<>();
            double w = W, l = L, h = H;
            Vec3 posL = pos;

            // Local vertices
            Vec3[] v = {
                    new Vec3(-w, 0, -l), new Vec3(w, 0, -l),   // 0,1 rear-bottom
                    new Vec3(w, 0, l), new Vec3(-w, 0, l),     // 2,3 front-bottom
                    new Vec3(-w * 0.9, h, -l * 0.7), new Vec3(w * 0.9, h, -l * 0.7),  // 4,5 rear-top
                    new Vec3(w * 0.9, h * 0.8, l * 0.6), new Vec3(-w * 0.9, h * 0.8, l * 0.6), // 6,7 front-top
                    new Vec3(-w * 0.85, h * 0.5, l * 0.65), new Vec3(w * 0.85, h * 0.5, l * 0.65), // 8,9 hood-front
                    new Vec3(-w * 0.85, h * 0.5, l * 0.2), new Vec3(w * 0.85, h * 0.5, l * 0.2),   // 10,11 hood-mid
                    // Wheels (8 verts each approximated as simple boxes)
                    new Vec3(-w * 1.15, 0.5, -l * 0.65), new Vec3(-w * 0.75, 0.5, -l * 0.65),
                    new Vec3(-w * 1.15, 0.0, -l * 0.65), new Vec3(-w * 0.75, 0.0, -l * 0.65),
                    new Vec3(-w * 1.15, 0.5, -l * 0.45), new Vec3(-w * 0.75, 0.5, -l * 0.45),
                    new Vec3(-w * 1.15, 0.0, -l * 0.45), new Vec3(-w * 0.75, 0.0, -l * 0.45), // 12-19 rear-left wheel
                    new Vec3(w * 0.75, 0.5, -l * 0.65), new Vec3(w * 1.15, 0.5, -l * 0.65),
                    new Vec3(w * 0.75, 0.0, -l * 0.65), new Vec3(w * 1.15, 0.0, -l * 0.65),
                    new Vec3(w * 0.75, 0.5, -l * 0.45), new Vec3(w * 1.15, 0.5, -l * 0.45),
                    new Vec3(w * 0.75, 0.0, -l * 0.45), new Vec3(w * 1.15, 0.0, -l * 0.45), // 20-27 rear-right
                    new Vec3(-w * 1.15, 0.5, l * 0.55), new Vec3(-w * 0.75, 0.5, l * 0.55),
                    new Vec3(-w * 1.15, 0.0, l * 0.55), new Vec3(-w * 0.75, 0.0, l * 0.55),
                    new Vec3(-w * 1.15, 0.5, l * 0.35), new Vec3(-w * 0.75, 0.5, l * 0.35),
                    new Vec3(-w * 1.15, 0.0, l * 0.35), new Vec3(-w * 0.75, 0.0, l * 0.35), // 28-35 front-left
                    new Vec3(w * 0.75, 0.5, l * 0.55), new Vec3(w * 1.15, 0.5, l * 0.55),
                    new Vec3(w * 0.75, 0.0, l * 0.55), new Vec3(w * 1.15, 0.0, l * 0.55),
                    new Vec3(w * 0.75, 0.5, l * 0.35), new Vec3(w * 1.15, 0.5, l * 0.35),
                    new Vec3(w * 0.75, 0.0, l * 0.35), new Vec3(w * 1.15, 0.0, l * 0.35), // 36-43 front-right
            };

            // Transform all to world space
            Vec3[] wv = new Vec3[v.length];
            for (int i = 0; i < v.length; i++) {
                wv[i] = v[i].rotateY(heading).add(posL);
            }

            Color bodyDark = bodyColor.darker();
            Color bodyBright = bodyColor.brighter();
            Color glassColor = new Color(40, 80, 120, 220);
            Color wheelColor = new Color(20, 20, 25);
            Color rimColor = new Color(180, 180, 200);

            // Body top
            polys.add(new Poly3D(new Vec3[]{wv[4], wv[5], wv[7], wv[6]}, bodyBright, false));
            // Rear
            polys.add(new Poly3D(new Vec3[]{wv[1], wv[0], wv[5], wv[4]}, bodyDark, false));
            // Front
            polys.add(new Poly3D(new Vec3[]{wv[3], wv[2], wv[7], wv[6]}, bodyColor, false));
            // Left side
            polys.add(new Poly3D(new Vec3[]{wv[0], wv[3], wv[7], wv[4]}, bodyDark, false));
            // Right side
            polys.add(new Poly3D(new Vec3[]{wv[2], wv[1], wv[5], wv[6]}, bodyDark, false));
            // Hood
            polys.add(new Poly3D(new Vec3[]{wv[8], wv[9], wv[11], wv[10]}, bodyBright, false));
            // Windshield
            polys.add(new Poly3D(new Vec3[]{wv[7], wv[6], wv[9], wv[8]}, glassColor, true));
            // Rear window
            polys.add(new Poly3D(new Vec3[]{wv[4], wv[5], wv[10], wv[11]}, glassColor, true));

            // Spoiler
            Vec3 spoilerL1 = new Vec3(-w * 0.95, h * 1.3, -l * 0.85).rotateY(heading).add(posL);
            Vec3 spoilerL2 = new Vec3(-w * 0.95, h * 1.3, -l * 0.7).rotateY(heading).add(posL);
            Vec3 spoilerR1 = new Vec3(w * 0.95, h * 1.3, -l * 0.85).rotateY(heading).add(posL);
            Vec3 spoilerR2 = new Vec3(w * 0.95, h * 1.3, -l * 0.7).rotateY(heading).add(posL);
            polys.add(new Poly3D(new Vec3[]{spoilerL1, spoilerR1, spoilerR2, spoilerL2}, accentColor, false));

            // Wheels (4 boxes, dark with rim accent)
            polys.add(buildWheelBox(wv, 12, 13, 14, 15, 16, 17, 18, 19, wheelColor, rimColor));
            polys.add(buildWheelBox(wv, 20, 21, 22, 23, 24, 25, 26, 27, wheelColor, rimColor));
            polys.add(buildWheelBox(wv, 28, 29, 30, 31, 32, 33, 34, 35, wheelColor, rimColor));
            polys.add(buildWheelBox(wv, 36, 37, 38, 39, 40, 41, 42, 43, wheelColor, rimColor));

            return polys;
        }

        private Poly3D buildWheelBox(Vec3[] wv, int a, int b, int c, int d,
                                     int e, int f, int g, int h, Color wheel, Color rim) {
            // Just top face + front face for simplicity (wheel visible as dark box)
            return new Poly3D(new Vec3[]{wv[a], wv[b], wv[e], wv[f]}, wheel, false);
        }

        void addExhaustParticles(List<Particle3D> particles) {
            if (Math.random() > 0.5) return;
            Vec3 rear = new Vec3(0, 0.6, -L - 0.3).rotateY(heading).add(pos);
            double spread = (Math.random() - 0.5) * 0.4;
            Vec3 vel = new Vec3(Math.sin(heading + Math.PI + spread) * 0.15,
                    0.05 + Math.random() * 0.1,
                    Math.cos(heading + Math.PI + spread) * 0.15);
            Color c = speed > 2.5
                    ? new Color(255, 220, 80, 220)
                    : new Color(255, 100, 220, 180); // neon magenta idle smoke
            particles.add(new Particle3D(rear, vel, c, 25 + (int)(Math.random() * 15)));
        }
    }

    // ================================================================
    //  POLY3D
    // ================================================================
    static class Poly3D {
        Vec3[] verts;
        Color color;
        boolean doubleSided;
        Poly3D(Vec3[] verts, Color color, boolean doubleSided) {
            this.verts = verts; this.color = color; this.doubleSided = doubleSided;
        }
    }

    // ================================================================
    //  GAME PANEL
    // ================================================================
    static class GamePanel extends JPanel {

        static final int WIDTH = 1200;
        static final int HEIGHT = 750;

        enum State { MENU, COUNTDOWN, RACING, FINISHED }
        State state = State.MENU;
        boolean twoPlayer = false;
        int countdown = 3, countdownTick = 0;

        // Track
        List<Vec3> trackPath = new ArrayList<>();
        double trackWidth = 14;
        int numTrackPoints = 240;
        int lapsToWin = 2;

        // Cars
        Car3D car1, car2;

        // Camera
        Vec3 cameraPos = new Vec3(0, 8, -18);
        Vec3 cameraTarget = new Vec3(0, 0, 0);
        double cameraShakeX = 0, cameraShakeY = 0;

        // Particles
        List<Particle3D> particles = new ArrayList<>();

        // Input
        Set<Integer> keys = new HashSet<>();

        // Projection
        double focalLength = 520;
        double cameraPitch = 0.28;

        // Effects
        int frameCounter = 0;
        int horizonScan = 0;

        // Timing
        long lastTime = System.nanoTime();
        double fps = 60;

        // Scenery (palm trees + neon pylons)
        List<Vec3> scenery = new ArrayList<>();
        List<Integer> sceneryType = new ArrayList<>();

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setBackground(Color.BLACK);
            setFocusable(true);
            requestFocusInWindow();

            buildTrack();
            buildScenery();
            car1 = new Car3D(new Vec3(trackPath.get(0).x - 2, trackPath.get(0).y, trackPath.get(0).z),
                    new Color(255, 40, 120), new Color(0, 255, 255), "P1");
            car2 = new Car3D(new Vec3(trackPath.get(0).x + 2, trackPath.get(0).y, trackPath.get(0).z),
                    new Color(0, 200, 255), new Color(255, 220, 0), "P2");

            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    keys.add(e.getKeyCode());
                    if (state == State.MENU) {
                        if (e.getKeyCode() == KeyEvent.VK_1) { twoPlayer = false; startRace(); }
                        if (e.getKeyCode() == KeyEvent.VK_2) { twoPlayer = true; startRace(); }
                    } else if (state == State.FINISHED && e.getKeyCode() == KeyEvent.VK_R) {
                        state = State.MENU;
                    }
                }
                @Override public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
            });

            javax.swing.Timer timer = new javax.swing.Timer(1000 / 60, e -> {
                long now = System.nanoTime();
                double dt = Math.min(0.05, (now - lastTime) / 1_000_000_000.0);
                lastTime = now;
                fps = fps * 0.9 + (1.0 / Math.max(dt, 0.001)) * 0.1;
                update(dt);
                repaint();
            });
            timer.start();
        }

        void buildTrack() {
            trackPath.clear();
            double cx = 0, cz = 0;
            double baseRadius = 120;
            for (int i = 0; i < numTrackPoints; i++) {
                double t = (double) i / numTrackPoints;
                double angle = t * Math.PI * 2;
                // Wavy oval — varies radius and adds gentle hills
                double r = baseRadius + 35 * Math.sin(angle * 3) + 20 * Math.cos(angle * 5);
                double x = cx + r * Math.cos(angle);
                double z = cz + r * Math.sin(angle) * 0.85;
                double y = 3 * Math.sin(angle * 4) + 2 * Math.cos(angle * 2);
                trackPath.add(new Vec3(x, y, z));
            }
        }

        void buildScenery() {
            scenery.clear();
            sceneryType.clear();
            Random r = new Random(99);
            for (int i = 0; i < trackPath.size(); i += 3) {
                Vec3 p = trackPath.get(i);
                // Two side objects per track point (left & right)
                for (int side = -1; side <= 1; side += 2) {
                    int nearestNext = (i + 1) % trackPath.size();
                    Vec3 next = trackPath.get(nearestNext);
                    double dx = next.x - p.x, dz = next.z - p.z;
                    double len = Math.sqrt(dx * dx + dz * dz);
                    if (len < 0.001) continue;
                    double nx = -dz / len, nz = dx / len; // perpendicular
                    double offset = trackWidth * 0.9 + 2 + r.nextDouble() * 4;
                    Vec3 sc = new Vec3(p.x + nx * offset * side, p.y, p.z + nz * offset * side);
                    scenery.add(sc);
                    sceneryType.add(r.nextInt(3)); // 0=neon pylon, 1=palm, 2=sign
                }
            }
        }

        void startRace() {
            car1.pos = new Vec3(trackPath.get(0).x - 2, trackPath.get(0).y, trackPath.get(0).z);
            car2.pos = new Vec3(trackPath.get(0).x + 2, trackPath.get(0).y, trackPath.get(0).z);
            car1.heading = 0; car2.heading = 0;
            car1.speed = 0; car2.speed = 0;
            car1.lap = 0; car2.lap = 0;
            particles.clear();
            state = State.COUNTDOWN;
            countdown = 3; countdownTick = 0;
        }

        // ---------------- UPDATE ----------------
        void update(double dt) {
            frameCounter++;
            horizonScan = (horizonScan + 2) % 4;

            if (state == State.COUNTDOWN) {
                countdownTick++;
                if (countdownTick >= 60) { countdownTick = 0; countdown--; if (countdown < 0) state = State.RACING; }
            }

            if (state == State.RACING) {
                boolean p1Up = keys.contains(KeyEvent.VK_UP);
                boolean p1Down = keys.contains(KeyEvent.VK_DOWN);
                boolean p1Left = keys.contains(KeyEvent.VK_LEFT);
                boolean p1Right = keys.contains(KeyEvent.VK_RIGHT);
                car1.update(p1Up, p1Down, p1Left, p1Right, this);

                if (twoPlayer) {
                    boolean p2Up = keys.contains(KeyEvent.VK_W);
                    boolean p2Down = keys.contains(KeyEvent.VK_S);
                    boolean p2Left = keys.contains(KeyEvent.VK_A);
                    boolean p2Right = keys.contains(KeyEvent.VK_D);
                    car2.update(p2Up, p2Down, p2Left, p2Right, this);
                } else {
                    updateAI();
                }

                car1.updateY(this);
                car2.updateY(this);

                // Track on/off detection
                car1.onTrack = distanceToTrack(car1.pos) < trackWidth / 2 + 1;
                car2.onTrack = distanceToTrack(car2.pos) < trackWidth / 2 + 1;

                checkLap(car1);
                checkLap(car2);

                // Exhaust particles
                if (car1.speed > 0.5) car1.addExhaustParticles(particles);
                if (car2.speed > 0.5) car2.addExhaustParticles(particles);

                // Win condition
                if (car1.lap >= lapsToWin || car2.lap >= lapsToWin) {
                    state = State.FINISHED;
                    Car3D winner = car1.lap >= lapsToWin ? car1 : car2;
                    for (int i = 0; i < 120; i++) {
                        Vec3 p = winner.pos.add(new Vec3((Math.random() - 0.5) * 4, 1 + Math.random() * 2, (Math.random() - 0.5) * 4));
                        Vec3 v = new Vec3((Math.random() - 0.5) * 0.4, Math.random() * 0.5, (Math.random() - 0.5) * 0.4);
                        Color[] neon = { new Color(255, 40, 120), new Color(0, 255, 255),
                                new Color(255, 220, 0), new Color(180, 80, 255) };
                        particles.add(new Particle3D(p, v, neon[(int)(Math.random() * 4)], 90));
                    }
                }
            }

            // Update particles
            Iterator<Particle3D> it = particles.iterator();
            while (it.hasNext()) {
                Particle3D p = it.next();
                p.update();
                if (p.life <= 0) it.remove();
            }

            // Camera follow
            Car3D focus = car1;
            double camDist = 14;
            double camHeight = 7;
            double behindX = focus.pos.x - Math.sin(focus.heading) * camDist;
            double behindZ = focus.pos.z - Math.cos(focus.heading) * camDist;
            Vec3 desired = new Vec3(behindX, focus.pos.y + camHeight, behindZ);
            cameraPos = cameraPos.mul(0.85).add(desired.mul(0.15));
            cameraTarget = cameraTarget.mul(0.85).add(focus.pos.add(new Vec3(0, 1.5, 0)).mul(0.15));

            // Camera shake at high speed
            double sh = Math.abs(focus.speed) * 0.15;
            cameraShakeX = (Math.random() - 0.5) * sh;
            cameraShakeY = (Math.random() - 0.5) * sh;
        }

        void updateAI() {
            if (car2.speed < Car3D.MAX_SPEED * 0.9) {
                car2.update(true, false, false, false, this);
            }
            int nearest = findNearestTrackPoint(car2.pos.x, car2.pos.z);
            int lookAhead = (nearest + 6) % trackPath.size();
            Vec3 target = trackPath.get(lookAhead);
            double targetAngle = Math.atan2(target.x - car2.pos.x, target.z - car2.pos.z);
            double diff = car2.normalizeAngle(targetAngle - car2.heading);
            boolean left = diff > 0.03;
            boolean right = diff < -0.03;
            car2.update(car2.speed < Car3D.MAX_SPEED * 0.9, false, left, right, this);
        }

        double distanceToTrack(Vec3 p) {
            int idx = findNearestTrackPoint(p.x, p.z);
            Vec3 c = trackPath.get(idx);
            return Math.sqrt((p.x - c.x) * (p.x - c.x) + (p.z - c.z) * (p.z - c.z));
        }

        int findNearestTrackPoint(double x, double z) {
            int best = 0;
            double bestDist = Double.MAX_VALUE;
            for (int i = 0; i < trackPath.size(); i++) {
                Vec3 p = trackPath.get(i);
                double d = (p.x - x) * (p.x - x) + (p.z - z) * (p.z - z);
                if (d < bestDist) { bestDist = d; best = i; }
            }
            return best;
        }

        void checkLap(Car3D car) {
            int idx = findNearestTrackPoint(car.pos.x, car.pos.z);
            boolean nearStart = idx < 4 || idx > trackPath.size() - 4;
            if (nearStart && !car.passedStart) {
                car.lap++;
                car.passedStart = true;
                for (int i = 0; i < 40; i++) {
                    Vec3 p = car.pos.add(new Vec3((Math.random() - 0.5) * 3, 0.5 + Math.random() * 2, (Math.random() - 0.5) * 3));
                    Vec3 v = new Vec3((Math.random() - 0.5) * 0.3, Math.random() * 0.4, (Math.random() - 0.5) * 0.3);
                    particles.add(new Particle3D(p, v, new Color(255, 220, 80), 60));
                }
            } else if (!nearStart) {
                car.passedStart = false;
            }
        }

        // ---------------- RENDER ----------------
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            if (state == State.MENU) {
                drawMenu(g2);
                drawScanlines(g2);
                return;
            }

            drawSky(g2);
            drawGround(g2);
            drawTrack(g2);
            drawScenery(g2);
            drawParticles(g2);
            drawCar(g2, car1);
            drawCar(g2, car2);
            drawChaseCamera(g2);
            drawHUD(g2);

            if (state == State.COUNTDOWN) drawCountdown(g2);
            if (state == State.FINISHED) drawResults(g2);

            drawScanlines(g2);
            drawVignette(g2);
        }

        // ---------- 80s Sky ----------
        void drawSky(Graphics2D g2) {
            // Neon gradient sky
            int horizonY = HEIGHT / 2 - 40;
            GradientPaint sky = new GradientPaint(0, 0, new Color(10, 5, 30),
                    0, horizonY, new Color(180, 40, 120));
            g2.setPaint(sky);
            g2.fillRect(0, 0, WIDTH, horizonY);

            // Huge scanline sun
            int sunX = WIDTH / 2;
            int sunY = horizonY - 40;
            int sunR = 140;
            for (int i = 0; i < sunR; i++) {
                double t = (double) i / sunR;
                int r = (int)(255);
                int gg = (int)(220 * (1 - t * 0.3));
                int b = (int)(80 * (1 - t));
                g2.setColor(new Color(r, gg, b, 60));
                g2.fillOval(sunX - sunR + i, sunY - sunR + i, (sunR - i) * 2, (sunR - i) * 2);
            }
            g2.setColor(new Color(255, 220, 120));
            g2.fillOval(sunX - sunR + 30, sunY - sunR + 30, (sunR - 30) * 2, (sunR - 30) * 2);

            // Sun scanlines
            g2.setColor(new Color(10, 5, 30));
            for (int y = sunY - 20; y < sunY + sunR; y += 12) {
                int thickness = 3 + (y - (sunY - 20)) / 30;
                g2.fillRect(sunX - sunR, y, sunR * 2, thickness);
            }

            // Neon stars
            Random r = new Random(5);
            g2.setColor(new Color(255, 255, 255, 200));
            for (int i = 0; i < 80; i++) {
                int sx = r.nextInt(WIDTH);
                int sy = r.nextInt(horizonY - 80);
                int sz = 1 + r.nextInt(2);
                g2.fillOval(sx, sy, sz, sz);
            }

            // Distant neon city silhouette
            g2.setColor(new Color(20, 5, 40));
            for (int i = 0; i < WIDTH; i += 40) {
                int h = 40 + (int)(Math.sin(i * 0.05) * 30) + (i % 3) * 15;
                g2.fillRect(i, horizonY - h, 40, h);
            }
            // City neon windows
            g2.setColor(new Color(0, 255, 255, 180));
            Random rw = new Random(7);
            for (int i = 0; i < WIDTH; i += 40) {
                int h = 40 + (int)(Math.sin(i * 0.05) * 30) + (i % 3) * 15;
                for (int wy = horizonY - h + 8; wy < horizonY - 8; wy += 10) {
                    for (int wx = i + 6; wx < i + 34; wx += 10) {
                        if (rw.nextBoolean()) {
                            g2.setColor(new Color(rw.nextInt(2) == 0 ? 255 : 0, rw.nextInt(2) == 0 ? 255 : 0, 255, 180));
                            g2.fillRect(wx, wy, 3, 4);
                        }
                    }
                }
            }
        }

        // ---------- 80s Ground / Neon Grid ----------
        void drawGround(Graphics2D g2) {
            int horizonY = HEIGHT / 2 - 40;
            GradientPaint ground = new GradientPaint(0, horizonY, new Color(30, 5, 50),
                    0, HEIGHT, new Color(5, 0, 15));
            g2.setPaint(ground);
            g2.fillRect(0, horizonY, WIDTH, HEIGHT - horizonY);

            // Neon perspective grid
            g2.setColor(new Color(255, 40, 200, 90));
            g2.setStroke(new BasicStroke(1.5f));
            // Vertical lines converging to vanishing point
            for (int i = -30; i <= 30; i++) {
                int xBottom = WIDTH / 2 + i * 60;
                g2.drawLine(WIDTH / 2, horizonY, xBottom, HEIGHT);
            }
            // Horizontal lines with perspective spacing
            double spacing = 4;
            for (int i = 0; i < 40; i++) {
                double z = i * spacing + 1;
                double scale = 1.0 / z;
                int y = (int)(horizonY + (HEIGHT - horizonY) * scale * 4);
                if (y > HEIGHT + 50) break;
                int alpha = Math.max(20, 120 - i * 3);
                g2.setColor(new Color(0, 255, 255, alpha));
                g2.drawLine(0, y, WIDTH, y);
            }
        }

        // ---------- 3D Track ----------
        void drawTrack(Graphics2D g2) {
            // Build left/right edges of track
            int n = trackPath.size();
            Vec3[] leftEdge = new Vec3[n];
            Vec3[] rightEdge = new Vec3[n];
            for (int i = 0; i < n; i++) {
                Vec3 p = trackPath.get(i);
                Vec3 next = trackPath.get((i + 1) % n);
                double dx = next.x - p.x, dz = next.z - p.z;
                double len = Math.sqrt(dx * dx + dz * dz);
                double nx = -dz / len, nz = dx / len;
                leftEdge[i]  = new Vec3(p.x + nx * trackWidth / 2, p.y, p.z + nz * trackWidth / 2);
                rightEdge[i] = new Vec3(p.x - nx * trackWidth / 2, p.y, p.z - nz * trackWidth / 2);
            }

            // Draw track segments
            List<TrackSeg> segs = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                Vec3[] quad = { leftEdge[i], rightEdge[i], rightEdge[j], leftEdge[j] };
                double avgDist = 0;
                for (Vec3 v : quad) avgDist += v.sub(cameraPos).length();
                avgDist /= 4;
                TrackSeg ts = new TrackSeg();
                ts.quad = quad;
                ts.depth = avgDist;
                ts.index = i;
                segs.add(ts);
            }
            segs.sort((a, b) -> Double.compare(b.depth, a.depth));

            // Project and draw each segment
            for (TrackSeg ts : segs) {
                Point[] pts = new Point[4];
                boolean visible = true;
                for (int k = 0; k < 4; k++) {
                    pts[k] = project(ts.quad[k]);
                    if (pts[k] == null) { visible = false; break; }
                }
                if (!visible) continue;

                // Alternating dark asphalt
                Color road = (ts.index / 4) % 2 == 0
                        ? new Color(25, 15, 40)
                        : new Color(30, 20, 48);
                g2.setColor(road);
                g2.fillPolygon(new int[]{pts[0].x, pts[1].x, pts[2].x, pts[3].x},
                        new int[]{pts[0].y, pts[1].y, pts[2].y, pts[3].y}, 4);

                // Neon edges (magenta outer, cyan inner)
                g2.setColor(new Color(255, 40, 200, 220));
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(pts[0].x, pts[0].y, pts[3].x, pts[3].y);
                g2.drawLine(pts[1].x, pts[1].y, pts[2].x, pts[2].y);

                // Center dashed line (yellow neon) every other segment
                if (ts.index % 4 < 2) {
                    Vec3 mid1 = ts.quad[0].add(ts.quad[1]).mul(0.5);
                    Vec3 mid2 = ts.quad[3].add(ts.quad[2]).mul(0.5);
                    Point pm1 = project(mid1);
                    Point pm2 = project(mid2);
                    if (pm1 != null && pm2 != null) {
                        g2.setColor(new Color(255, 240, 80, 200));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawLine(pm1.x, pm1.y, pm2.x, pm2.y);
                    }
                }
            }

            // Start/finish line
            Vec3 sp = trackPath.get(0);
            Vec3 sn = trackPath.get(1);
            double dx = sn.x - sp.x, dz = sn.z - sp.z;
            double len = Math.sqrt(dx * dx + dz * dz);
            double nx = -dz / len, nz = dx / len;
            Vec3 sL = new Vec3(sp.x + nx * trackWidth / 2, sp.y + 0.05, sp.z + nz * trackWidth / 2);
            Vec3 sR = new Vec3(sp.x - nx * trackWidth / 2, sp.y + 0.05, sp.z - nz * trackWidth / 2);
            Vec3 eL = new Vec3(sL.x + dx * 0.5, sL.y, sL.z + dz * 0.5);
            Vec3 eR = new Vec3(sR.x + dx * 0.5, sR.y, sR.z + dz * 0.5);
            Point[] fp = { project(sL), project(sR), project(eR), project(eL) };
            if (fp[0] != null && fp[1] != null && fp[2] != null && fp[3] != null) {
                g2.setColor(new Color(240, 240, 240));
                g2.fillPolygon(new int[]{fp[0].x, fp[1].x, fp[2].x, fp[3].x},
                        new int[]{fp[0].y, fp[1].y, fp[2].y, fp[3].y}, 4);
                // Checkerboard
                g2.setColor(new Color(20, 20, 30));
                for (int ci = 0; ci < 8; ci++) {
                    for (int cj = 0; cj < 2; cj++) {
                        if ((ci + cj) % 2 == 0) continue;
                        // Simple split
                    }
                }
            }
        }

        static class TrackSeg {
            Vec3[] quad;
            double depth;
            int index;
        }

        // ---------- Scenery (palm trees, neon pylons) ----------
        void drawScenery(Graphics2D g2) {
            List<SceneryItem> items = new ArrayList<>();
            for (int i = 0; i < scenery.size(); i++) {
                Vec3 p = scenery.get(i);
                double d = p.sub(cameraPos).length();
                if (d < 2 || d > 180) continue;
                SceneryItem si = new SceneryItem();
                si.pos = p;
                si.type = sceneryType.get(i);
                si.depth = d;
                items.add(si);
            }
            items.sort((a, b) -> Double.compare(b.depth, a.depth));

            for (SceneryItem si : items) {
                Point base = project(si.pos);
                if (base == null) continue;
                double d = si.depth;
                double h = 600.0 / d; // height in pixels
                switch (si.type) {
                    case 0 -> { // Neon pylon
                        int w = Math.max(2, (int)(h / 12));
                        // Glow
                        g2.setColor(new Color(0, 255, 255, 80));
                        g2.fillOval(base.x - w * 2, base.y - (int)h - w * 2, w * 4, w * 4);
                        g2.setColor(new Color(0, 255, 255));
                        g2.fillRect(base.x - w / 2, base.y - (int)h, w, (int)h);
                        g2.setColor(new Color(255, 40, 200));
                        g2.fillRect(base.x - w, base.y - (int)h - w * 2, w * 2, w * 2);
                    }
                    case 1 -> { // Palm tree silhouette
                        int trunkW = Math.max(2, (int)(h / 15));
                        g2.setColor(new Color(15, 5, 25));
                        g2.fillRect(base.x - trunkW / 2, base.y - (int)h, trunkW, (int)h);
                        // Fronds
                        int frondR = (int)(h / 3);
                        for (int a = 0; a < 8; a++) {
                            double ang = a * Math.PI / 4 - Math.PI / 2;
                            int fx = base.x + (int)(Math.cos(ang) * frondR);
                            int fy = base.y - (int)h + (int)(Math.sin(ang) * frondR * 0.5);
                            g2.setStroke(new BasicStroke(Math.max(2, h / 25)));
                            g2.drawLine(base.x, base.y - (int)h, fx, fy);
                        }
                    }
                    default -> { // Neon sign
                        int w = Math.max(6, (int)(h / 2));
                        int hh = Math.max(4, (int)(h / 3));
                        g2.setColor(new Color(255, 220, 0, 100));
                        g2.fillRoundRect(base.x - w, base.y - (int)h - hh * 2, w * 2, hh * 2, 6, 6);
                        g2.setColor(new Color(255, 220, 0));
                        g2.setStroke(new BasicStroke(2f));
                        g2.drawRoundRect(base.x - w, base.y - (int)h - hh * 2, w * 2, hh * 2, 6, 6);
                    }
                }
            }
        }

        static class SceneryItem {
            Vec3 pos;
            int type;
            double depth;
        }

        // ---------- Car rendering (3D polys) ----------
        void drawCar(Graphics2D g2, Car3D car) {
            List<Poly3D> polys = car.buildPolygons();
            // Sort by average depth (far first)
            List<SortedPoly> sp = new ArrayList<>();
            for (Poly3D p : polys) {
                double d = 0;
                for (Vec3 v : p.verts) d += v.sub(cameraPos).length();
                d /= p.verts.length;
                SortedPoly s = new SortedPoly();
                s.poly = p;
                s.depth = d;
                sp.add(s);
            }
            sp.sort((a, b) -> Double.compare(b.depth, a.depth));

            for (SortedPoly s : sp) {
                Point[] pts = new Point[s.poly.verts.length];
                boolean visible = true;
                for (int i = 0; i < s.poly.verts.length; i++) {
                    pts[i] = project(s.poly.verts[i]);
                    if (pts[i] == null) { visible = false; break; }
                }
                if (!visible) continue;

                int[] xs = new int[pts.length];
                int[] ys = new int[pts.length];
                for (int i = 0; i < pts.length; i++) { xs[i] = pts[i].x; ys[i] = pts[i].y; }

                // Backface culling for solid polys (not double-sided)
                if (!s.poly.doubleSided) {
                    // Compute normal in world space
                    Vec3 a = s.poly.verts[1].sub(s.poly.verts[0]);
                    Vec3 b = s.poly.verts[2].sub(s.poly.verts[0]);
                    Vec3 normal = a.cross(b).normalize();
                    Vec3 viewDir = cameraPos.sub(s.poly.verts[0]).normalize();
                    if (normal.dot(viewDir) < 0) continue;
                }

                // Shade based on depth
                double shade = Math.max(0.35, Math.min(1.0, 60.0 / s.depth));
                Color base = s.poly.color;
                int r = (int)(base.getRed() * shade);
                int gg = (int)(base.getGreen() * shade);
                int bb = (int)(base.getBlue() * shade);
                g2.setColor(new Color(
                        Math.min(255, r), Math.min(255, gg), Math.min(255, bb),
                        base.getAlpha()));

                g2.fillPolygon(xs, ys, pts.length);

                // Neon outline on cars
                g2.setColor(new Color(255, 40, 200, 120));
                g2.setStroke(new BasicStroke(1f));
                g2.drawPolygon(xs, ys, pts.length);
            }

            // Name label above car
            Point label = project(car.pos.add(new Vec3(0, 4, 0)));
            if (label != null) {
                g2.setFont(new Font("Arial Black", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int lw = fm.stringWidth(car.name);
                g2.setColor(new Color(0, 0, 0, 180));
                g2.fillRoundRect(label.x - lw / 2 - 4, label.y - 14, lw + 8, 16, 6, 6);
                g2.setColor(car.bodyColor);
                g2.drawString(car.name, label.x - lw / 2, label.y - 2);
            }
        }

        static class SortedPoly {
            Poly3D poly;
            double depth;
        }

        // ---------- Projection ----------
        Point project(Vec3 world) {
            Vec3 rel = world.sub(cameraPos);
            // Rotate to camera space (yaw only)
            double yaw = Math.atan2(cameraTarget.x - cameraPos.x, cameraTarget.z - cameraPos.z);
            Vec3 cam = rel.rotateY(-yaw);
            // Pitch camera
            double cy = Math.cos(cameraPitch), sy = Math.sin(cameraPitch);
            double y1 = cam.y * cy - cam.z * sy;
            double z1 = cam.y * sy + cam.z * cy;
            cam = new Vec3(cam.x, y1, z1);
            if (cam.z < 0.5) return null; // behind camera
            double scale = focalLength / cam.z;
            int sx = (int)(WIDTH / 2 + cam.x * scale + cameraShakeX);
            int sy = (int)(HEIGHT / 2 - cam.y * scale + cameraShakeY);
            return new Point(sx, sy);
        }

        // ---------- Particles ----------
        void drawParticles(Graphics2D g2) {
            List<Particle3D> sorted = new ArrayList<>(particles);
            sorted.sort((a, b) -> Double.compare(
                    b.pos.sub(cameraPos).length(),
                    a.pos.sub(cameraPos).length()));
            for (Particle3D p : sorted) {
                Point pt = project(p.pos);
                if (pt == null) continue;
                float alpha = Math.max(0, Math.min(1, p.life / (float)p.maxLife));
                Color c = p.color;
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(alpha * c.getAlpha())));
                int s = (int)(p.size * (0.5 + alpha * 0.5));
                g2.fillOval(pt.x - s, pt.y - s, s * 2, s * 2);
            }
        }

        // ---------- Chase camera "cockpit glow" ----------
        void drawChaseCamera(Graphics2D g2) {
            // Subtle neon bottom glow (dashboard-like)
            GradientPaint dash = new GradientPaint(0, HEIGHT - 120, new Color(255, 40, 200, 0),
                    0, HEIGHT, new Color(255, 40, 200, 90));
            g2.setPaint(dash);
            g2.fillRect(0, HEIGHT - 120, WIDTH, 120);

            // Chrome rear-view mirror style top glow
            GradientPaint top = new GradientPaint(0, 0, new Color(0, 255, 255, 60),
                    0, 40, new Color(0, 255, 255, 0));
            g2.setPaint(top);
            g2.fillRect(0, 0, WIDTH, 40);
        }

        // ---------- HUD ----------
        void drawHUD(Graphics2D g2) {
            // Chrome frame title
            drawChromeText(g2, "SUPERCAR RACING 3D", WIDTH / 2, 40, 22, TextAlign.CENTER);

            // Player 1 panel
            drawPlayerHUD(g2, 20, 80, car1, "PLAYER 1", new Color(255, 40, 120));
            // Player 2 / CPU
            drawPlayerHUD(g2, WIDTH - 260, 80, car2,
                    twoPlayer ? "PLAYER 2" : "COMPUTER",
                    new Color(0, 200, 255));

            // Lap counter chrome
            drawChromeText(g2, "LAP " + Math.min(car1.lap + 1, lapsToWin) + " / " + lapsToWin,
                    WIDTH / 2, 80, 18, TextAlign.CENTER);

            // FPS (retro VHS style)
            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(new Color(0, 255, 180, 200));
            g2.drawString("REC ● " + (int)fps + " FPS", 20, HEIGHT - 20);
        }

        enum TextAlign { LEFT, CENTER, RIGHT }

        void drawChromeText(Graphics2D g2, String text, int x, int y, int size, TextAlign align) {
            Font f = new Font("Arial Black", Font.BOLD, size);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            int tx = switch (align) {
                case CENTER -> x - tw / 2;
                case RIGHT -> x - tw;
                default -> x;
            };
            // Chrome gradient
            GradientPaint chrome = new GradientPaint(0, y - size, Color.WHITE,
                    0, y, new Color(180, 180, 220));
            g2.setPaint(chrome);
            g2.drawString(text, tx, y);
            // Neon outline
            g2.setColor(new Color(255, 40, 200));
            g2.setStroke(new BasicStroke(1f));
            g2.drawString(text, tx - 1, y);
            g2.drawString(text, tx + 1, y);
            g2.drawString(text, tx, y - 1);
            g2.drawString(text, tx, y + 1);
        }

        void drawPlayerHUD(Graphics2D g2, int x, int y, Car3D car, String label, Color accent) {
            int w = 240, h = 100;
            // Panel background
            g2.setColor(new Color(10, 5, 25, 200));
            g2.fillRoundRect(x, y, w, h, 12, 12);
            g2.setColor(accent);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, w, h, 12, 12);

            // Label
            g2.setFont(new Font("Arial Black", Font.BOLD, 14));
            g2.setColor(accent);
            g2.drawString(label, x + 12, y + 22);

            // Speed number
            g2.setFont(new Font("Consolas", Font.BOLD, 30));
            g2.setColor(Color.WHITE);
            g2.drawString(String.valueOf((int)(Math.abs(car.speed) * 80)), x + 12, y + 58);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.setColor(new Color(200, 200, 220));
            g2.drawString("km/h", x + 80, y + 58);

            // Speed bar (neon)
            int bx = x + 12, by = y + 74, bw = w - 24, bh = 14;
            g2.setColor(new Color(30, 10, 50));
            g2.fillRoundRect(bx, by, bw, bh, 7, 7);
            int fill = (int)(Math.min(1.0, Math.abs(car.speed) / Car3D.MAX_SPEED) * bw);
            GradientPaint gp = new GradientPaint(bx, by, new Color(0, 255, 255),
                    bx + bw, by, new Color(255, 40, 200));
            g2.setPaint(gp);
            g2.fillRoundRect(bx, by, fill, bh, 7, 7);
            g2.setColor(new Color(255, 255, 255, 100));
            g2.drawRoundRect(bx, by, bw, bh, 7, 7);

            // Lap indicator
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            g2.setColor(new Color(220, 220, 240));
            g2.drawString("LAP " + Math.min(car.lap + 1, lapsToWin) + "/" + lapsToWin, x + w - 70, y + 22);
        }

        // ---------- Countdown ----------
        void drawCountdown(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            String t = countdown > 0 ? String.valueOf(countdown) : "GO!";
            drawChromeText(g2, t, WIDTH / 2, HEIGHT / 2 + 40, 160, TextAlign.CENTER);
            g2.setColor(countdown > 0 ? new Color(255, 220, 80) : new Color(80, 255, 120));
            g2.setFont(new Font("Arial Black", Font.BOLD, 160));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(t, WIDTH / 2 - fm.stringWidth(t) / 2, HEIGHT / 2 + 40);
        }

        // ---------- Results ----------
        void drawResults(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            Car3D winner = car1.lap >= car2.lap ? car1 : car2;
            String winText = (winner == car1 ? "PLAYER 1" : (twoPlayer ? "PLAYER 2" : "COMPUTER")) + " WINS!";
            drawChromeText(g2, "RACE COMPLETE", WIDTH / 2, HEIGHT / 2 - 60, 44, TextAlign.CENTER);
            drawChromeText(g2, winText, WIDTH / 2, HEIGHT / 2 + 10, 34, TextAlign.CENTER);
            drawChromeText(g2, "PRESS R TO RETURN", WIDTH / 2, HEIGHT / 2 + 90, 20, TextAlign.CENTER);
        }

        // ---------- Menu ----------
        void drawMenu(Graphics2D g2) {
            // 80s menu sky
            GradientPaint bg = new GradientPaint(0, 0, new Color(10, 5, 30),
                    0, HEIGHT, new Color(80, 10, 80));
            g2.setPaint(bg);
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            // Neon grid floor
            int horizonY = HEIGHT / 2;
            for (int i = -30; i <= 30; i++) {
                g2.setColor(new Color(255, 40, 200, 120));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(WIDTH / 2, horizonY, WIDTH / 2 + i * 60, HEIGHT);
            }
            for (int i = 0; i < 30; i++) {
                int y = horizonY + i * i * 2;
                if (y > HEIGHT) break;
                g2.setColor(new Color(0, 255, 255, Math.max(20, 140 - i * 5)));
                g2.drawLine(0, y, WIDTH, y);
            }

            // Sun
            int sunR = 130;
            for (int i = 0; i < sunR; i++) {
                double t = (double) i / sunR;
                g2.setColor(new Color(255, (int)(200 * (1 - t * 0.4)), (int)(80 * (1 - t)), 40));
                g2.fillOval(WIDTH / 2 - sunR + i, horizonY - sunR - 40 + i, (sunR - i) * 2, (sunR - i) * 2);
            }
            g2.setColor(new Color(255, 220, 120));
            g2.fillOval(WIDTH / 2 - sunR + 30, horizonY - sunR - 10, (sunR - 30) * 2, (sunR - 30) * 2);
            g2.setColor(new Color(10, 5, 30));
            for (int y = horizonY - 60; y < horizonY + 60; y += 12) {
                g2.fillRect(WIDTH / 2 - sunR, y, sunR * 2, 3);
            }

            // Chrome title
            drawChromeText(g2, "SUPERCAR", WIDTH / 2, 150, 72, TextAlign.CENTER);
            drawChromeText(g2, "RACING CHALLENGE 3D", WIDTH / 2, 220, 36, TextAlign.CENTER);

            // Blinking menu options
            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            if (blink) drawChromeText(g2, "[ 1 ]  RACE VS COMPUTER", WIDTH / 2, HEIGHT - 220, 26, TextAlign.CENTER);
            drawChromeText(g2, "[ 2 ]  RACE 2 PLAYERS", WIDTH / 2, HEIGHT - 170, 26, TextAlign.CENTER);

            // Controls
            g2.setFont(new Font("Consolas", Font.BOLD, 14));
            g2.setColor(new Color(200, 220, 255));
            String c1 = "P1: ARROW KEYS    |    P2: W A S D";
            String c2 = "WIN 2 LAPS TO CLAIM VICTORY";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(c1, WIDTH / 2 - fm.stringWidth(c1) / 2, HEIGHT - 90);
            g2.drawString(c2, WIDTH / 2 - fm.stringWidth(c2) / 2, HEIGHT - 65);
        }

        // ---------- CRT scanlines + vignette ----------
        void drawScanlines(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 40));
            for (int y = horizonScan; y < HEIGHT; y += 4) {
                g2.fillRect(0, y, WIDTH, 2);
            }
        }

        void drawVignette(Graphics2D g2) {
            RadialGradientPaint vp = new RadialGradientPaint(
                    new Point(WIDTH / 2, HEIGHT / 2),
                    Math.max(WIDTH, HEIGHT) * 0.75f,
                    new float[]{0.6f, 1.0f},
                    new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 180)});
            g2.setPaint(vp);
            g2.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }
}