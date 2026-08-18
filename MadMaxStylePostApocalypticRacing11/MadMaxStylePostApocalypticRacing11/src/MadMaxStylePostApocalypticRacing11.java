import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class MadMaxStylePostApocalypticRacing11 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Neon Wasteland — Post-Apocalyptic Night Racing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        GamePanel panel = new GamePanel();
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.requestFocusInWindow();
        panel.start();
    }
}

final class Util {
    private Util() {}

    static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static double normalizeAngle(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }

    static double distPointToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double len2 = dx * dx + dy * dy;
        double t = len2 < 1e-9 ? 0 : ((px - ax) * dx + (py - ay) * dy) / len2;
        t = clamp(t, 0, 1);
        double cx = ax + dx * t, cy = ay + dy * t;
        return Math.hypot(px - cx, py - cy);
    }
}

class InputState {
    double throttle; // -1..1
    double steer;    // -1..1
    boolean boost;
}

class Particle {
    double x, y, vx, vy;
    double life, maxLife;
    double size, growth;
    Color color;
    ParticleKind kind;

    enum ParticleKind { DUST, SMOKE, SPARK, DEBRIS }

    Particle(double x, double y, double vx, double vy, double life, double size, double growth, Color color, ParticleKind kind) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.life = life; this.maxLife = life;
        this.size = size; this.growth = growth;
        this.color = color; this.kind = kind;
    }

    boolean update(double dt) {
        x += vx * dt;
        y += vy * dt;
        vx *= 0.98;
        vy *= 0.98;
        size += growth * dt;
        life -= dt;
        return life > 0;
    }

    float alpha() {
        return (float) Util.clamp(life / maxLife, 0, 1);
    }
}

class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();

    synchronized void add(Particle p) { particles.add(p); }

    synchronized void update(double dt) {
        particles.removeIf(p -> !p.update(dt));
    }

    synchronized void draw(Graphics2D g) {
        for (Particle p : particles) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.alpha()));
            g.setColor(p.color);
            double s = p.size;
            g.fill(new Ellipse2D.Double(p.x - s / 2, p.y - s / 2, s, s));
            g.setComposite(old);
        }
    }
}

/**
 * Rounded-hexagon track. Six vertices of a regular hexagon are joined by straight
 * edges; each vertex is replaced by a quadratic-bezier fillet so the loop stays a
 * simple (non-self-intersecting) convex-ish shape. The centerline is sampled into a
 * dense point list with a parallel cumulative arc-length array, exactly like the
 * proven reference architecture, used both for lap counting and for offset-based
 * edge rendering / collision via per-point normals.
 */
class Track {
    final double centerX, centerY, circumradius, cornerCut, trackWidth, halfWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;

    Track(double centerX, double centerY, double circumradius, double cornerCut, double trackWidth) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.circumradius = circumradius;
        this.cornerCut = cornerCut;
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        build();
    }

    private void build() {
        int vertices = 6;
        Point2D.Double[] verts = new Point2D.Double[vertices];
        for (int i = 0; i < vertices; i++) {
            double ang = Math.toRadians(-90 + i * 60);
            verts[i] = new Point2D.Double(centerX + circumradius * Math.cos(ang), centerY + circumradius * Math.sin(ang));
        }

        Point2D.Double[] pin = new Point2D.Double[vertices];
        Point2D.Double[] pout = new Point2D.Double[vertices];
        for (int i = 0; i < vertices; i++) {
            Point2D.Double prev = verts[(i - 1 + vertices) % vertices];
            Point2D.Double cur = verts[i];
            Point2D.Double next = verts[(i + 1) % vertices];
            double dxIn = cur.x - prev.x, dyIn = cur.y - prev.y;
            double lenIn = Math.hypot(dxIn, dyIn);
            pin[i] = new Point2D.Double(cur.x - dxIn / lenIn * cornerCut, cur.y - dyIn / lenIn * cornerCut);
            double dxOut = next.x - cur.x, dyOut = next.y - cur.y;
            double lenOut = Math.hypot(dxOut, dyOut);
            pout[i] = new Point2D.Double(cur.x + dxOut / lenOut * cornerCut, cur.y + dyOut / lenOut * cornerCut);
        }

        int nCorner = 30, nStraight = 40;
        for (int i = 0; i < vertices; i++) {
            for (int k = 0; k < nCorner; k++) {
                double t = k / (double) nCorner;
                double omt = 1 - t;
                double x = omt * omt * pin[i].x + 2 * omt * t * verts[i].x + t * t * pout[i].x;
                double y = omt * omt * pin[i].y + 2 * omt * t * verts[i].y + t * t * pout[i].y;
                centerline.add(new Point2D.Double(x, y));
            }
            Point2D.Double a = pout[i];
            Point2D.Double b = pin[(i + 1) % vertices];
            for (int k = 0; k < nStraight; k++) {
                double t = k / (double) nStraight;
                centerline.add(new Point2D.Double(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t));
            }
        }

        int n = centerline.size();
        cumulativeLength = new double[n];
        cumulativeLength[0] = 0;
        for (int i = 1; i < n; i++) {
            cumulativeLength[i] = cumulativeLength[i - 1] + centerline.get(i - 1).distance(centerline.get(i));
        }
        totalLength = cumulativeLength[n - 1] + centerline.get(n - 1).distance(centerline.get(0));

        for (int i = 0; i < n; i++) {
            Point2D.Double prev = centerline.get((i - 1 + n) % n);
            Point2D.Double next = centerline.get((i + 1) % n);
            double tx = next.x - prev.x, ty = next.y - prev.y;
            double len = Math.hypot(tx, ty);
            if (len < 1e-6) len = 1;
            normals.add(new Point2D.Double(-ty / len, tx / len));
        }
    }

    int nearestIndex(double x, double y) {
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < centerline.size(); i++) {
            double d = centerline.get(i).distanceSq(x, y);
            if (d < bestD) { bestD = d; best = i; }
        }
        return best;
    }

    double progress(double x, double y) {
        return cumulativeLength[nearestIndex(x, y)];
    }

    double distanceFromCenterline(double x, double y) {
        int i = nearestIndex(x, y);
        return centerline.get(i).distance(x, y);
    }

    Point2D.Double pointAtOffset(int index, double offset) {
        Point2D.Double p = centerline.get(index);
        Point2D.Double n = normals.get(index);
        return new Point2D.Double(p.x + n.x * offset, p.y + n.y * offset);
    }

    Point2D.Double startPosition(double laneOffset) {
        return pointAtOffset(4, laneOffset);
    }

    double startAngle() {
        Point2D.Double a = centerline.get(0);
        Point2D.Double b = centerline.get(8);
        return Math.atan2(b.y - a.y, b.x - a.x);
    }

    void draw(Graphics2D g) {
        Path2D.Double outer = new Path2D.Double();
        Path2D.Double inner = new Path2D.Double();
        for (int i = 0; i < centerline.size(); i++) {
            Point2D.Double o = pointAtOffset(i, halfWidth);
            Point2D.Double in = pointAtOffset(i, -halfWidth);
            if (i == 0) { outer.moveTo(o.x, o.y); inner.moveTo(in.x, in.y); }
            else { outer.lineTo(o.x, o.y); inner.lineTo(in.x, in.y); }
        }
        outer.closePath();
        inner.closePath();

        Area road = new Area(outer);
        road.subtract(new Area(inner));
        g.setColor(new Color(26, 21, 40));
        g.fill(road);

        drawGlowPath(g, inner, new Color(60, 225, 255));
        drawGlowPath(g, outer, new Color(255, 45, 205));

        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{12, 16}, 0));
        g.setColor(new Color(255, 210, 90, 90));
        Path2D.Double mid = new Path2D.Double();
        for (int i = 0; i < centerline.size(); i++) {
            Point2D.Double p = centerline.get(i);
            if (i == 0) mid.moveTo(p.x, p.y); else mid.lineTo(p.x, p.y);
        }
        mid.closePath();
        g.draw(mid);
        g.setStroke(new BasicStroke(1));

        drawStartLine(g);
    }

    private void drawGlowPath(Graphics2D g, Path2D.Double path, Color core) {
        // soft, wide, low-alpha duplicate underneath...
        g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(core.getRed(), core.getGreen(), core.getBlue(), 30));
        g.draw(path);
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(core.getRed(), core.getGreen(), core.getBlue(), 80));
        g.draw(path);
        // ...bright thin line on top.
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(Math.min(255, core.getRed() + 45), Math.min(255, core.getGreen() + 45), Math.min(255, core.getBlue() + 45)));
        g.draw(path);
    }

    private void drawStartLine(Graphics2D g) {
        Point2D.Double o = pointAtOffset(0, halfWidth);
        Point2D.Double in = pointAtOffset(0, -halfWidth);
        int squares = 8;
        double dx = (o.x - in.x) / squares, dy = (o.y - in.y) / squares;
        for (int i = 0; i < squares; i++) {
            double sx = in.x + dx * i, sy = in.y + dy * i;
            g.setColor(i % 2 == 0 ? Color.WHITE : new Color(40, 220, 255));
            g.fill(new Rectangle2D.Double(sx - 4, sy - 4, 8, 8));
        }
    }
}

class Obstacle {
    enum Kind { ROCK, CACTUS, WRECK, TUMBLEWEED }

    double x, y, radius, rotation;
    Kind kind;
    double vx, vy, spin;

    Obstacle(Kind kind, double x, double y, double radius) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.rotation = Math.random() * Math.PI * 2;
        if (kind == Kind.TUMBLEWEED) this.spin = 4 + Math.random() * 3;
    }

    void update(double dt, double minX, double maxX, double minY, double maxY) {
        if (kind == Kind.TUMBLEWEED) {
            x += vx * dt;
            y += vy * dt;
            rotation += spin * dt;
            if (x < minX) x = maxX;
            if (x > maxX) x = minX;
            if (y < minY) y = maxY;
            if (y > maxY) y = minY;
        }
    }

    void draw(Graphics2D g) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(rotation);
        switch (kind) {
            case ROCK:
                g.setColor(new Color(58, 54, 70));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(28, 26, 38));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                break;
            case CACTUS:
                g.setColor(new Color(32, 68, 58));
                g.fillRoundRect((int) (-radius / 3), (int) -radius, (int) (radius * 2 / 3), (int) (radius * 2), 8, 8);
                g.fillRoundRect((int) -radius, (int) (-radius / 3), (int) radius, (int) (radius * 2 / 3), 8, 8);
                g.fillRoundRect((int) (radius / 3), (int) (-radius * 0.7), (int) (radius * 2 / 3), (int) radius, 8, 8);
                break;
            case WRECK:
                g.setColor(new Color(42, 30, 42));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(new Color(255, 70, 170, 130));
                g.drawRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                break;
            case TUMBLEWEED:
                g.setColor(new Color(95, 85, 75));
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3;
                    g.drawLine(0, 0, (int) (Math.cos(a) * radius), (int) (Math.sin(a) * radius));
                }
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                break;
        }
        g.setTransform(old);
    }
}

/** Decorative, non-colliding dead-neon billboard silhouette scattered around the wasteland. */
class NeonSign {
    final double x, y, w, h;
    final Color neon;
    final double phase, speed;

    NeonSign(double x, double y, double w, double h, Color neon, double phase, double speed) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.neon = neon; this.phase = phase; this.speed = speed;
    }

    void draw(Graphics2D g, double t) {
        double flicker = 0.55 + 0.45 * Math.max(0, Math.sin(t * speed + phase));
        if (Math.sin(t * speed * 2.7 + phase * 3) > 0.94) flicker *= 0.15;

        g.setColor(new Color(14, 12, 20));
        g.fillRect((int) (x + w / 2 - 3), (int) (y + h), 6, 24);

        Path2D.Double frame = new Path2D.Double();
        frame.moveTo(x, y + h * 0.15);
        frame.lineTo(x + w * 0.35, y);
        frame.lineTo(x + w, y + h * 0.05);
        frame.lineTo(x + w * 0.85, y + h);
        frame.lineTo(x + w * 0.1, y + h * 0.9);
        frame.closePath();
        g.setColor(new Color(10, 9, 16));
        g.fill(frame);

        int a1 = (int) (45 * flicker);
        int a2 = (int) Util.clamp(60 + 160 * flicker, 0, 255);
        g.setStroke(new BasicStroke(6f));
        g.setColor(new Color(neon.getRed(), neon.getGreen(), neon.getBlue(), a1));
        g.draw(frame);
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(new Color(neon.getRed(), neon.getGreen(), neon.getBlue(), a2));
        g.draw(frame);
        g.setStroke(new BasicStroke(1));
    }
}

/**
 * Signature mechanic: a pair of posts facing each other across a gap in the track.
 * Cycles DORMANT -> CHARGING (telegraph glow builds up) -> FIRING (jagged lightning
 * bolt + spark burst, damages/spins out any car in the gap) -> back to DORMANT.
 */
class TeslaHazard {
    enum Phase { DORMANT, CHARGING, FIRING }

    final Point2D.Double postA, postB;
    Phase phase = Phase.DORMANT;
    double timer;
    final List<Point2D.Double> bolt = new ArrayList<>();
    final Set<Car> zappedThisCycle = new HashSet<>();

    static final double DORMANT_TIME = 4.5;
    static final double CHARGE_TIME = 1.6;
    static final double FIRE_TIME = 0.32;
    static final double HIT_RADIUS = 16;

    TeslaHazard(Point2D.Double a, Point2D.Double b, double phaseOffset) {
        postA = a; postB = b;
        double t = phaseOffset % DORMANT_TIME;
        timer = DORMANT_TIME - t;
        if (timer <= 0.1) timer = 0.5;
    }

    Point2D.Double midpoint() {
        return new Point2D.Double((postA.x + postB.x) / 2, (postA.y + postB.y) / 2);
    }

    void update(double dt, List<Car> cars, ParticleSystem particles) {
        timer -= dt;
        if (phase == Phase.DORMANT && timer <= 0) {
            phase = Phase.CHARGING;
            timer = CHARGE_TIME;
        } else if (phase == Phase.CHARGING && timer <= 0) {
            phase = Phase.FIRING;
            timer = FIRE_TIME;
            zappedThisCycle.clear();
            spawnFireBurst(particles);
        } else if (phase == Phase.FIRING && timer <= 0) {
            phase = Phase.DORMANT;
            timer = DORMANT_TIME;
            bolt.clear();
        }

        if (phase == Phase.FIRING) {
            regenerateBolt();
            for (Car c : cars) {
                if (!c.alive || zappedThisCycle.contains(c)) continue;
                double d = Util.distPointToSegment(c.x, c.y, postA.x, postA.y, postB.x, postB.y);
                if (d < HIT_RADIUS) {
                    c.damage(26);
                    c.spinOut(1.1);
                    zappedThisCycle.add(c);
                    for (int i = 0; i < 14; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double sp = 80 + Math.random() * 180;
                        particles.add(new Particle(c.x, c.y, Math.cos(a) * sp, Math.sin(a) * sp,
                                0.3 + Math.random() * 0.3, 3 + Math.random() * 4, 4,
                                new Color(140, 225, 255), Particle.ParticleKind.SPARK));
                    }
                }
            }
        }
    }

    private void regenerateBolt() {
        bolt.clear();
        double dx = postB.x - postA.x, dy = postB.y - postA.y;
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) len = 1;
        double nx = -dy / len, ny = dx / len;
        int segments = 9;
        bolt.add(postA);
        for (int i = 1; i < segments; i++) {
            double t = i / (double) segments;
            double bx = postA.x + dx * t, by = postA.y + dy * t;
            double off = (Math.random() - 0.5) * 22;
            bolt.add(new Point2D.Double(bx + nx * off, by + ny * off));
        }
        bolt.add(postB);
    }

    private void spawnFireBurst(ParticleSystem particles) {
        for (Point2D.Double p : new Point2D.Double[]{postA, postB}) {
            for (int i = 0; i < 10; i++) {
                double a = Math.random() * Math.PI * 2;
                double sp = 40 + Math.random() * 120;
                particles.add(new Particle(p.x, p.y, Math.cos(a) * sp, Math.sin(a) * sp,
                        0.25 + Math.random() * 0.3, 3 + Math.random() * 3, 3,
                        new Color(150, 230, 255), Particle.ParticleKind.SPARK));
            }
        }
    }

    void draw(Graphics2D g) {
        double chargeT = phase == Phase.CHARGING ? 1 - Util.clamp(timer / CHARGE_TIME, 0, 1) : 0;
        drawPost(g, postA, chargeT);
        drawPost(g, postB, chargeT);

        if (phase == Phase.CHARGING) {
            g.setStroke(new BasicStroke(2f));
            int a = (int) (70 * chargeT);
            g.setColor(new Color(120, 220, 255, a));
            g.draw(new Line2D.Double(postA, postB));
        } else if (phase == Phase.FIRING && bolt.size() > 1) {
            Path2D.Double path = new Path2D.Double();
            path.moveTo(bolt.get(0).x, bolt.get(0).y);
            for (int i = 1; i < bolt.size(); i++) path.lineTo(bolt.get(i).x, bolt.get(i).y);
            g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(140, 220, 255, 70));
            g.draw(path);
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(190, 240, 255, 160));
            g.draw(path);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Color.WHITE);
            g.draw(path);
            g.setStroke(new BasicStroke(1));
        }
    }

    private void drawPost(Graphics2D g, Point2D.Double p, double chargeT) {
        g.setColor(new Color(20, 18, 28));
        g.fillRect((int) p.x - 3, (int) p.y - 18, 6, 36);
        double glowA = phase == Phase.FIRING ? 220 : phase == Phase.CHARGING ? 60 + chargeT * 140 : 40;
        Color orb = phase == Phase.FIRING ? new Color(210, 245, 255) : new Color(90, 200, 255);
        g.setColor(new Color(orb.getRed(), orb.getGreen(), orb.getBlue(), (int) Util.clamp(glowA, 0, 255)));
        int r = phase == Phase.FIRING ? 9 : 6;
        g.fillOval((int) p.x - r, (int) p.y - 18 - r / 2, r * 2, r * 2);
    }
}

class Car {
    double x, y, angle;
    double vx, vy, speed;
    double health = 100;
    double boostFuel = 100;
    boolean boosting;
    boolean alive = true;
    double respawnTimer;
    int lap = 0;
    double lastArcLen;
    double unwrappedDistance;
    boolean finished = false;
    double finishTime = -1;
    double spinTimer = 0;

    final Color bodyColor;
    final Color trimColor;
    final String label;
    final ParticleSystem particles;

    private static final double MAX_SPEED = 430;
    private static final double MAX_REVERSE = -160;
    private static final double ACCEL = 300;
    private static final double BRAKE = 560;
    private static final double NATURAL_FRICTION = 150;
    private static final double OFFROAD_MULT = 2.4;
    private static final double TURN_RATE = 3.0;
    private static final double GRIP = 7.5;
    private static final double BOOST_MULT = 1.55;
    private static final double RADIUS = 17;

    private double dustTimer = 0;

    Car(double x, double y, double angle, Color bodyColor, Color trimColor, String label, ParticleSystem particles) {
        this.x = x; this.y = y; this.angle = angle;
        this.bodyColor = bodyColor; this.trimColor = trimColor;
        this.label = label;
        this.particles = particles;
    }

    double radius() { return RADIUS; }

    void update(double dt, InputState in, Track track) {
        if (!alive) {
            respawnTimer -= dt;
            if (respawnTimer <= 0) respawn(track);
            return;
        }
        if (spinTimer > 0) {
            spinTimer -= dt;
            angle += 6 * dt;
            speed *= 0.97;
        } else {
            applyControls(dt, in, track);
        }

        double dvx = Math.cos(angle) * speed;
        double dvy = Math.sin(angle) * speed;
        double gripNow = Math.max(2.0, GRIP - Util.clamp(Math.abs(speed) / MAX_SPEED, 0, 1) * 3.0);
        double blend = Util.clamp(gripNow * dt, 0, 1);
        vx += (dvx - vx) * blend;
        vy += (dvy - vy) * blend;

        boolean onTrack = track.distanceFromCenterline(x, y) < track.halfWidth;
        x += vx * dt;
        y += vy * dt;

        double s = track.progress(x, y);
        double delta = s - lastArcLen;
        if (delta < -track.totalLength / 2) delta += track.totalLength;
        else if (delta > track.totalLength / 2) delta -= track.totalLength;
        unwrappedDistance += delta;
        lastArcLen = s;
        lap = Math.max(0, (int) Math.floor(unwrappedDistance / track.totalLength));

        updateBoost();
        emitDust(dt, onTrack);
    }

    private void applyControls(double dt, InputState in, Track track) {
        boolean onTrack = track.distanceFromCenterline(x, y) < track.halfWidth;
        double frictionMult = onTrack ? 1.0 : OFFROAD_MULT;
        boosting = in.boost && boostFuel > 5 && in.throttle > 0;
        double topSpeed = MAX_SPEED * (boosting ? BOOST_MULT : 1.0) * (onTrack ? 1.0 : 0.55);

        if (in.throttle > 0.05) {
            speed += ACCEL * (boosting ? BOOST_MULT : 1.0) * dt;
        } else if (in.throttle < -0.05) {
            if (speed > 10) speed -= BRAKE * dt;
            else speed -= ACCEL * 0.6 * dt;
        } else {
            if (speed > 0) speed = Math.max(0, speed - NATURAL_FRICTION * frictionMult * dt);
            else if (speed < 0) speed = Math.min(0, speed + NATURAL_FRICTION * frictionMult * dt);
        }
        if (!onTrack && speed > 0) speed = Math.max(0, speed - NATURAL_FRICTION * (OFFROAD_MULT - 1) * dt);

        speed = Util.clamp(speed, MAX_REVERSE, topSpeed);

        double speedFactor = Util.clamp(Math.abs(speed) / MAX_SPEED, 0.35, 1.0);
        double dir = speed < 0 ? -1 : 1;
        angle += in.steer * TURN_RATE * dt * speedFactor * dir;
    }

    private void updateBoost() {
        if (boosting) boostFuel = Math.max(0, boostFuel - 0.7);
        else boostFuel = Math.min(100, boostFuel + 0.23);
    }

    private void emitDust(double dt, boolean onTrack) {
        dustTimer -= dt;
        double spd = Math.hypot(vx, vy);
        if (spd < 30 || dustTimer > 0) return;
        dustTimer = onTrack ? 0.06 : 0.03;
        double rearX = x - Math.cos(angle) * radius();
        double rearY = y - Math.sin(angle) * radius();
        Color c = onTrack ? new Color(150, 150, 170, 110) : new Color(120, 110, 130, 150);
        for (int i = 0; i < (onTrack ? 1 : 2); i++) {
            double jx = (Math.random() - 0.5) * 10;
            double jy = (Math.random() - 0.5) * 10;
            particles.add(new Particle(rearX + jx, rearY + jy,
                    -vx * 0.15 + (Math.random() - 0.5) * 20, -vy * 0.15 + (Math.random() - 0.5) * 20,
                    0.5 + Math.random() * 0.4, 6 + Math.random() * 6, 8, c, Particle.ParticleKind.DUST));
        }
        if (boosting) {
            particles.add(new Particle(rearX, rearY, -vx * 0.3, -vy * 0.3, 0.35, 10, 6,
                    new Color(255, 140, 220, 200), Particle.ParticleKind.SPARK));
        }
    }

    void damage(double amount) {
        if (!alive) return;
        health -= amount;
        if (health <= 0) {
            health = 0;
            wreck();
        }
    }

    private void wreck() {
        alive = false;
        respawnTimer = 2.2;
        for (int i = 0; i < 26; i++) {
            double a = Math.random() * Math.PI * 2;
            double sp = 60 + Math.random() * 160;
            particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp,
                    0.5 + Math.random() * 0.6, 4 + Math.random() * 6, 10,
                    Math.random() < 0.5 ? new Color(255, 120, 200) : new Color(90, 90, 100), Particle.ParticleKind.SMOKE));
        }
    }

    private void respawn(Track track) {
        alive = true;
        health = 60;
        speed = 0; vx = 0; vy = 0;
        int idx = track.nearestIndex(x, y);
        Point2D.Double p = track.centerline.get(idx);
        Point2D.Double next = track.centerline.get((idx + 5) % track.centerline.size());
        x = p.x; y = p.y;
        angle = Math.atan2(next.y - p.y, next.x - p.x);
        spinTimer = 0;
    }

    void spinOut(double intensity) {
        spinTimer = Math.max(spinTimer, intensity);
    }

    void draw(Graphics2D g) {
        if (!alive) return;
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(angle);

        if (boosting) {
            g.setColor(new Color(255, 150, 230, 200));
            g.fillOval(-(int) radius() - 14, -5, 14, 10);
        }

        g.setColor(Color.BLACK);
        g.fillRoundRect(-18, -13, 10, 6, 3, 3);
        g.fillRoundRect(-18, 7, 10, 6, 3, 3);
        g.fillRoundRect(8, -13, 10, 6, 3, 3);
        g.fillRoundRect(8, 7, 10, 6, 3, 3);

        g.setColor(bodyColor);
        g.fillRoundRect(-18, -11, 36, 22, 8, 8);
        g.setColor(trimColor);
        g.fillRoundRect(-6, -9, 16, 18, 6, 6);
        g.setColor(new Color(30, 30, 40));
        g.fillRect(14, -4, 8, 2);
        g.fillRect(14, 2, 8, 2);
        g.setColor(new Color(160, 230, 255));
        g.fillOval(14, -6, 4, 4);
        g.fillOval(14, 2, 4, 4);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.drawString(label, -5, 4);

        g.setTransform(old);

        double hpFrac = Util.clamp(health / 100.0, 0, 1);
        g.setColor(Color.DARK_GRAY);
        g.fillRect((int) x - 16, (int) y - 26, 32, 4);
        g.setColor(hpFrac > 0.5 ? new Color(90, 220, 140) : hpFrac > 0.25 ? Color.ORANGE : Color.RED);
        g.fillRect((int) x - 16, (int) y - 26, (int) (32 * hpFrac), 4);
    }
}

class AIDriver {
    private final Car car;
    private final Track track;
    private double boostCooldown = 0;

    AIDriver(Car car, Track track) {
        this.car = car;
        this.track = track;
    }

    InputState think(double dt, List<Obstacle> obstacles, List<TeslaHazard> hazards) {
        InputState in = new InputState();
        int idx = track.nearestIndex(car.x, car.y);
        int lookahead = 16;
        Point2D.Double target = track.centerline.get((idx + lookahead) % track.centerline.size());
        double desiredAngle = Math.atan2(target.y - car.y, target.x - car.x);
        double diff = Util.normalizeAngle(desiredAngle - car.angle);

        for (Obstacle o : obstacles) {
            double dx = o.x - car.x, dy = o.y - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < 90) {
                double angToObs = Util.normalizeAngle(Math.atan2(dy, dx) - car.angle);
                if (Math.abs(angToObs) < 0.9) {
                    diff += angToObs < 0 ? 0.6 : -0.6;
                }
            }
        }

        for (TeslaHazard h : hazards) {
            if (h.phase == TeslaHazard.Phase.DORMANT) continue;
            Point2D.Double mid = h.midpoint();
            double dx = mid.x - car.x, dy = mid.y - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < 110) {
                double angToH = Util.normalizeAngle(Math.atan2(dy, dx) - car.angle);
                if (Math.abs(angToH) < 1.1) {
                    diff += angToH < 0 ? 0.8 : -0.8;
                }
            }
        }

        in.steer = Util.clamp(diff * 1.6, -1, 1);
        in.throttle = Math.abs(diff) > 1.3 ? 0.2 : 1.0;

        boostCooldown -= dt;
        if (Math.abs(diff) < 0.25 && boostCooldown <= 0 && Math.random() < 0.01) {
            in.boost = true;
            boostCooldown = 1.5;
        }
        return in;
    }
}

class GamePanel extends JPanel implements Runnable, KeyListener {
    private static final int W = 1280, H = 720;
    private static final int TOTAL_LAPS = 3;

    private enum State { MENU, COUNTDOWN, RACING, FINISHED }

    private State state = State.MENU;
    private boolean vsAI = true;
    private Thread thread;
    private volatile boolean running = true;

    private final Set<Integer> keys = new HashSet<>();
    private Track track;
    private List<Car> cars = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<TeslaHazard> hazards = new ArrayList<>();
    private final List<NeonSign> neonSigns = new ArrayList<>();
    private final List<Point2D.Double> stars = new ArrayList<>();
    private ParticleSystem particles = new ParticleSystem();
    private AIDriver aiDriver;

    private double countdownTimer;
    private double raceTime;
    private double nightPulse = 0;
    private final List<Point2D.Double> mountains = new ArrayList<>();
    private final BufferedImage sceneBuffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        setupWorld();
        buildMountains();
        buildStars();
    }

    private void buildMountains() {
        Random r = new Random(7);
        double x = -50;
        while (x < W + 50) {
            mountains.add(new Point2D.Double(x, 90 + r.nextInt(70)));
            x += 40 + r.nextInt(40);
        }
    }

    private void buildStars() {
        Random r = new Random(21);
        for (int i = 0; i < 110; i++) {
            stars.add(new Point2D.Double(r.nextInt(W), r.nextInt((int) (H * 0.5))));
        }
    }

    private void buildNeonSigns() {
        Random r = new Random(99);
        Color[] hues = {
                new Color(255, 40, 200), new Color(60, 230, 255),
                new Color(255, 150, 40), new Color(120, 255, 150)
        };
        int tries = 0;
        while (neonSigns.size() < 9 && tries < 800) {
            tries++;
            double x = 30 + r.nextDouble() * (W - 100);
            double y = 90 + r.nextDouble() * (H - 220);
            if (track.distanceFromCenterline(x, y) < track.halfWidth + 100) continue;
            double w = 34 + r.nextInt(30);
            double h = 46 + r.nextInt(40);
            neonSigns.add(new NeonSign(x, y, w, h, hues[r.nextInt(hues.length)],
                    r.nextDouble() * Math.PI * 2, 1.4 + r.nextDouble() * 2.2));
        }
    }

    private void setupWorld() {
        track = new Track(W / 2.0, H / 2.0 + 20, 235, 68, 140);
        obstacles.clear();
        hazards.clear();
        Random r = new Random(42);
        int n = track.centerline.size();
        for (int i = 0; i < n; i += 11) {
            if (r.nextDouble() < 0.5) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 20 + r.nextDouble() * 55;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.ROCK : Obstacle.Kind.CACTUS;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(10)));
            }
        }
        int[] wreckIdx = { n / 8, n * 3 / 8, n * 5 / 8, n * 7 / 8 };
        for (int idx : wreckIdx) {
            double off = (r.nextDouble() - 0.5) * track.trackWidth * 0.5;
            Point2D.Double p = track.pointAtOffset(idx, off);
            obstacles.add(new Obstacle(Obstacle.Kind.WRECK, p.x, p.y, 16));
        }
        for (int i = 0; i < 3; i++) {
            Obstacle tw = new Obstacle(Obstacle.Kind.TUMBLEWEED, r.nextInt(W), r.nextInt(H), 14);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 40 + r.nextDouble() * 40;
            tw.vx = Math.cos(a) * sp;
            tw.vy = Math.sin(a) * sp;
            obstacles.add(tw);
        }

        int hazardCount = 4;
        for (int k = 0; k < hazardCount; k++) {
            int idx = ((n / hazardCount) * k + n / 14) % n;
            Point2D.Double a = track.pointAtOffset(idx, -(track.halfWidth + 16));
            Point2D.Double b = track.pointAtOffset(idx, track.halfWidth + 16);
            hazards.add(new TeslaHazard(a, b, k * (TeslaHazard.DORMANT_TIME / hazardCount)));
        }

        buildNeonSigns();
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-25);
        Point2D.Double p2pos = track.startPosition(25);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(255, 60, 130), new Color(255, 210, 90), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(60, 200, 255), new Color(230, 230, 255), vsAI ? "AI" : "2", particles);
        p1.lastArcLen = track.progress(p1.x, p1.y);
        p2.lastArcLen = track.progress(p2.x, p2.y);
        cars.add(p1);
        cars.add(p2);
        aiDriver = vsAI ? new AIDriver(p2, track) : null;
        countdownTimer = 3.999;
        raceTime = 0;
        state = State.COUNTDOWN;
    }

    void start() {
        thread = new Thread(this, "game-loop");
        thread.start();
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        double accumulator = 0;
        double dt = 1.0 / 60.0;
        while (running) {
            long now = System.nanoTime();
            double frameTime = (now - last) / 1_000_000_000.0;
            last = now;
            if (frameTime > 0.25) frameTime = 0.25;
            accumulator += frameTime;
            while (accumulator >= dt) {
                update(dt);
                accumulator -= dt;
            }
            repaint();
            long sleep = 16 - (System.nanoTime() - now) / 1_000_000;
            try { if (sleep > 0) Thread.sleep(sleep); } catch (InterruptedException ignored) {}
        }
    }

    private void update(double dt) {
        nightPulse += dt;
        for (Obstacle o : obstacles) o.update(dt, -30, W + 30, -30, H + 30);
        for (TeslaHazard hz : hazards) hz.update(dt, cars, particles);

        switch (state) {
            case COUNTDOWN:
                countdownTimer -= dt;
                if (countdownTimer <= 0) state = State.RACING;
                break;
            case RACING:
                updateRace(dt);
                break;
            default:
                break;
        }
        particles.update(dt);
    }

    private void updateRace(double dt) {
        raceTime += dt;
        Car p1 = cars.get(0);
        Car p2 = cars.get(1);

        InputState in1 = new InputState();
        in1.throttle = keys.contains(KeyEvent.VK_W) ? 1 : keys.contains(KeyEvent.VK_S) ? -1 : 0;
        in1.steer = keys.contains(KeyEvent.VK_A) ? -1 : keys.contains(KeyEvent.VK_D) ? 1 : 0;
        in1.boost = keys.contains(KeyEvent.VK_SPACE);

        InputState in2;
        if (vsAI) {
            in2 = aiDriver.think(dt, obstacles, hazards);
        } else {
            in2 = new InputState();
            in2.throttle = keys.contains(KeyEvent.VK_UP) ? 1 : keys.contains(KeyEvent.VK_DOWN) ? -1 : 0;
            in2.steer = keys.contains(KeyEvent.VK_LEFT) ? -1 : keys.contains(KeyEvent.VK_RIGHT) ? 1 : 0;
            in2.boost = keys.contains(KeyEvent.VK_ENTER) || keys.contains(KeyEvent.VK_SHIFT);
        }

        p1.update(dt, in1, track);
        p2.update(dt, in2, track);

        resolveCarCollision(p1, p2);
        for (Obstacle o : obstacles) {
            resolveObstacleCollision(p1, o);
            resolveObstacleCollision(p2, o);
        }

        for (Car c : cars) {
            if (!c.finished && c.lap >= TOTAL_LAPS) {
                c.finished = true;
                c.finishTime = raceTime;
            }
        }
        if (p1.finished && p2.finished) state = State.FINISHED;
    }

    private void resolveCarCollision(Car a, Car b) {
        if (!a.alive || !b.alive) return;
        double dx = b.x - a.x, dy = b.y - a.y;
        double dist = Math.hypot(dx, dy);
        double minDist = a.radius() + b.radius();
        if (dist < minDist && dist > 0.0001) {
            double nx = dx / dist, ny = dy / dist;
            double overlap = minDist - dist;
            a.x -= nx * overlap / 2; a.y -= ny * overlap / 2;
            b.x += nx * overlap / 2; b.y += ny * overlap / 2;
            double relSpeed = Math.hypot(a.vx - b.vx, a.vy - b.vy);
            double dmg = relSpeed * 0.05;
            a.damage(dmg); b.damage(dmg);
            double as = a.speed, bs = b.speed;
            a.speed = bs * 0.5; b.speed = as * 0.5;
            if (relSpeed > 100) { a.spinOut(0.5); b.spinOut(0.5); }
            for (int i = 0; i < 8; i++) {
                double ang = Math.random() * Math.PI * 2;
                particles.add(new Particle((a.x + b.x) / 2, (a.y + b.y) / 2,
                        Math.cos(ang) * 120, Math.sin(ang) * 120, 0.3, 3, 2,
                        new Color(255, 220, 120), Particle.ParticleKind.SPARK));
            }
        }
    }

    private void resolveObstacleCollision(Car c, Obstacle o) {
        if (!c.alive) return;
        double dx = c.x - o.x, dy = c.y - o.y;
        double dist = Math.hypot(dx, dy);
        double minDist = c.radius() + o.radius;
        if (dist < minDist && dist > 0.0001) {
            double nx = dx / dist, ny = dy / dist;
            double overlap = minDist - dist;
            c.x += nx * overlap;
            c.y += ny * overlap;
            double impact = Math.abs(c.speed);
            if (o.kind == Obstacle.Kind.TUMBLEWEED) {
                c.damage(impact * 0.015);
                c.speed *= 0.7;
                o.vx = -o.vx * 0.5; o.vy = -o.vy * 0.5;
            } else {
                c.damage(impact * 0.08);
                c.speed *= -0.35;
                c.spinOut(0.45);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D sg = sceneBuffer.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBackground(sg);
        track.draw(sg);
        for (Obstacle o : obstacles) o.draw(sg);
        for (TeslaHazard hz : hazards) hz.draw(sg);
        particles.draw(sg);
        for (Car c : cars) c.draw(sg);
        sg.dispose();

        Graphics2D g = (Graphics2D) g0;
        g.drawImage(sceneBuffer, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (state) {
            case MENU: drawMenu(g); break;
            case COUNTDOWN: drawCountdown(g); break;
            case RACING: drawHud(g); break;
            case FINISHED: drawFinish(g); break;
        }
    }

    private void drawBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(16, 9, 38), 0, (float) (H * 0.55), new Color(32, 22, 62)));
        g.fillRect(0, 0, W, (int) (H * 0.55));

        for (Point2D.Double s : stars) {
            double tw = 0.5 + 0.5 * Math.sin(nightPulse * 2 + s.x);
            g.setColor(new Color(255, 255, 255, (int) (90 + 100 * tw)));
            g.fillOval((int) s.x, (int) s.y, 2, 2);
        }

        double pulse = 1 + 0.02 * Math.sin(nightPulse);
        int moonR = (int) (46 * pulse);
        g.setPaint(new RadialGradientPaint(new Point(180, 100), moonR * 3,
                new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(210, 225, 255, 180), new Color(150, 180, 230, 70), new Color(150, 180, 230, 0)}));
        g.fillOval(180 - moonR * 3, 100 - moonR * 3, moonR * 6, moonR * 6);
        g.setColor(new Color(230, 238, 255));
        g.fillOval(180 - moonR / 2, 100 - moonR / 2, moonR, moonR);
        g.setColor(new Color(195, 210, 235));
        g.fillOval(180 - moonR / 4, 100 - moonR / 3, moonR / 4, moonR / 4);

        g.setColor(new Color(34, 26, 55));
        Path2D.Double range = new Path2D.Double();
        range.moveTo(-50, H * 0.5);
        for (Point2D.Double m : mountains) range.lineTo(m.x, H * 0.5 - m.y * 0.5);
        range.lineTo(W + 50, H * 0.5);
        range.closePath();
        g.fill(range);

        for (NeonSign s : neonSigns) s.draw(g, nightPulse);

        g.setPaint(new GradientPaint(0, (float) (H * 0.42), new Color(36, 27, 46), 0, H, new Color(18, 15, 28)));
        g.fillRect(0, (int) (H * 0.42), W, (int) (H * 0.58));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, W, H);
        g.setFont(new Font("SansSerif", Font.BOLD, 56));
        drawNeonText(g, "NEON WASTELAND", H / 2 - 150, new Color(255, 40, 200));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(new Color(150, 230, 255));
        centerText(g, "Tesla-arc desert racing after dark", H / 2 - 95);

        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        g.setColor(Color.WHITE);
        centerText(g, "[1] Player vs Player      [2] Player vs Computer", H / 2 - 20);
        centerText(g, "P1: W A S D  +  SPACE boost", H / 2 + 20);
        centerText(g, "P2: Arrow Keys + ENTER boost  (disabled vs Computer)", H / 2 + 46);
        g.setColor(new Color(140, 220, 255));
        centerText(g, "Watch the posts along the track — a charging glow means a lightning arc is coming", H / 2 + 78);
        g.setColor(Color.WHITE);
        centerText(g, "Press 1 or 2 to start", H / 2 + 118);
    }

    private void drawNeonText(Graphics2D g, String s, int y, Color core) {
        FontMetrics fm = g.getFontMetrics();
        int x = (W - fm.stringWidth(s)) / 2;
        g.setColor(new Color(core.getRed(), core.getGreen(), core.getBlue(), 90));
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                if (dx != 0 || dy != 0) g.drawString(s, x + dx, y + dy);
            }
        }
        g.setColor(Color.WHITE);
        g.drawString(s, x, y);
    }

    private void centerText(Graphics2D g, String s, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (W - fm.stringWidth(s)) / 2, y);
    }

    private void drawCountdown(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 90));
        g.setColor(new Color(255, 60, 200));
        String txt = countdownTimer > 3 ? "READY" : String.valueOf((int) Math.ceil(countdownTimer));
        if (countdownTimer <= 0) txt = "GO!";
        centerText(g, txt, H / 2);
        drawHud(g);
    }

    private void drawHud(Graphics2D g) {
        Car p1 = cars.get(0), p2 = cars.get(1);
        drawDriverHud(g, p1, 20, "P1");
        drawDriverHud(g, p2, W - 210, vsAI ? "CPU" : "P2");

        g.setColor(new Color(160, 230, 255));
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        String info = String.format("LAP %d/%d   LEADER: %s   TIME %.1fs",
                Math.min(TOTAL_LAPS, Math.max(p1.lap, p2.lap) + 1), TOTAL_LAPS, leadingCarLabel(), raceTime);
        centerText(g, info, 26);
    }

    private String leadingCarLabel() {
        Car p1 = cars.get(0), p2 = cars.get(1);
        return p1.unwrappedDistance >= p2.unwrappedDistance ? p1.label : p2.label;
    }

    private void drawDriverHud(Graphics2D g, Car c, int x, String tag) {
        g.setColor(new Color(10, 8, 24, 160));
        g.fillRoundRect(x, 16, 190, 60, 10, 10);
        g.setColor(new Color(255, 60, 200, 120));
        g.drawRoundRect(x, 16, 190, 60, 10, 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString(tag + "  LAP " + Math.min(c.lap + 1, TOTAL_LAPS), x + 10, 32);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 10, 40, 170, 8);
        double hp = Util.clamp(c.health / 100.0, 0, 1);
        g.setColor(hp > 0.5 ? new Color(90, 220, 140) : hp > 0.25 ? Color.ORANGE : Color.RED);
        g.fillRect(x + 10, 40, (int) (170 * hp), 8);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 10, 54, 170, 8);
        g.setColor(new Color(80, 200, 255));
        g.fillRect(x + 10, 54, (int) (170 * Util.clamp(c.boostFuel / 100.0, 0, 1)), 8);
    }

    private void drawFinish(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, W, H);
        g.setFont(new Font("SansSerif", Font.BOLD, 46));
        drawNeonText(g, "RACE FINISHED", H / 2 - 80, new Color(60, 230, 255));
        Car p1 = cars.get(0), p2 = cars.get(1);
        Car winner = p1.finishTime <= p2.finishTime ? p1 : p2;
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        centerText(g, "WINNER: " + winner.label + "  (" + String.format("%.1fs", winner.finishTime) + ")", H / 2 - 20);
        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        centerText(g, "Press R to restart or M for menu", H / 2 + 40);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys.add(e.getKeyCode());
        if (state == State.MENU) {
            if (e.getKeyCode() == KeyEvent.VK_1) { vsAI = false; setupRace(); }
            if (e.getKeyCode() == KeyEvent.VK_2) { vsAI = true; setupRace(); }
        } else if (state == State.FINISHED) {
            if (e.getKeyCode() == KeyEvent.VK_R) setupRace();
            if (e.getKeyCode() == KeyEvent.VK_M) state = State.MENU;
        }
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
    }

    @Override public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
    @Override public void keyTyped(KeyEvent e) {}
}
