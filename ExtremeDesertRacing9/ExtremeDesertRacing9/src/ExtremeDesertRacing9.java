import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ExtremeDesertRacing9 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Peanut Dunes Speedway — Extreme Desert Racing");
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

    enum ParticleKind { DUST, SMOKE, SPARK, DEBRIS, FIREWORK }

    Particle(double x, double y, double vx, double vy, double life, double size, double growth, Color color, ParticleKind kind) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.life = life; this.maxLife = life;
        this.size = size; this.growth = growth;
        this.color = color; this.kind = kind;
    }

    boolean update(double dt) {
        x += vx * dt;
        y += vy * dt;
        if (kind == ParticleKind.FIREWORK) {
            vy += 40 * dt;
            vx *= 0.99;
            vy *= 0.99;
        } else {
            vx *= 0.98;
            vy *= 0.98;
        }
        size += growth * dt;
        life -= dt;
        return life > 0;
    }

    float alpha() {
        return (float) Util.clamp(life / maxLife, 0, 1);
    }
}

/** Thread-safe particle pool: physics thread adds/updates, EDT draws — every entry point is synchronized. */
class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();

    synchronized void add(Particle p) { particles.add(p); }

    synchronized void update(double dt) {
        particles.removeIf(p -> !p.update(dt));
    }

    void spawnFirework(double x, double y) {
        Color[] palette = {
                new Color(255, 210, 110), new Color(255, 140, 90),
                new Color(255, 245, 210), new Color(200, 90, 180)
        };
        Random r = new Random();
        int count = 34;
        for (int i = 0; i < count; i++) {
            double ang = (Math.PI * 2 * i) / count + (r.nextDouble() - 0.5) * 0.3;
            double sp = 90 + r.nextDouble() * 140;
            Color c = palette[r.nextInt(palette.length)];
            add(new Particle(x, y - 10, Math.cos(ang) * sp, Math.sin(ang) * sp - 40,
                    0.6 + r.nextDouble() * 0.5, 3 + r.nextDouble() * 4, -3, c, Particle.ParticleKind.FIREWORK));
        }
        for (int i = 0; i < 10; i++) {
            add(new Particle(x, y, (r.nextDouble() - 0.5) * 30, -60 - r.nextDouble() * 60,
                    0.5, 5, 4, new Color(255, 255, 255), Particle.ParticleKind.FIREWORK));
        }
    }

    synchronized void draw(Graphics2D g) {
        for (Particle p : particles) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.alpha()));
            g.setColor(p.color);
            double s = Math.max(0.5, p.size);
            if (p.kind == Particle.ParticleKind.FIREWORK) {
                g.fill(new Ellipse2D.Double(p.x - s / 2, p.y - s / 2, s, s));
                g.setColor(new Color(255, 255, 255, (int) (120 * p.alpha())));
                g.fill(new Ellipse2D.Double(p.x - s / 4, p.y - s / 4, s / 2, s / 2));
            } else {
                g.fill(new Ellipse2D.Double(p.x - s / 2, p.y - s / 2, s, s));
            }
            g.setComposite(old);
        }
    }
}

/**
 * "Peanut Dunes" track: a single closed loop generated in POLAR form around a
 * fixed center (cx, cy): radius(theta) = baseRadius(theta) * (1 + pinch*cos(2*theta)),
 * with baseRadius elongated differently in x (a) and y (b).
 *
 * Why this is guaranteed non-self-intersecting (simple):
 *   point(theta) = (cx + a*cos(theta)*rf(theta), cy + b*sin(theta)*rf(theta))
 *   where rf(theta) = 1 + pinch*cos(2*theta) > 0 for pinch < 1.
 *   The polar angle of point(theta) AS SEEN FROM THE CENTER is
 *     phi(theta) = atan2(b*sin(theta)*rf(theta), a*cos(theta)*rf(theta))
 *                = atan2(b*sin(theta), a*cos(theta))          (rf cancels — it's a
 *                                                               common positive scalar)
 *   which is the classic monotonic angle-reparametrization of an ellipse: for any
 *   a,b > 0 it strictly increases (mod 2*pi) as theta sweeps 0..2*pi. A curve whose
 *   own polar angle from a fixed interior point is monotonic is "star-shaped" w.r.t.
 *   that point — every ray from the center crosses it exactly once — which makes the
 *   closed curve simple (non-self-intersecting) by construction, independent of the
 *   exact values of a, b and pinch (as long as pinch stays below 1 so rf never
 *   touches zero). This is what produces the two rounded lobes (maxima of rf at
 *   theta = 0, pi) joined by a pinched waist (minima of rf at theta = pi/2, 3pi/2)
 *   WITHOUT the path ever crossing itself — the "fake figure-8" look.
 *
 * The second, sneakier risk at a pinch is not the centerline but the INNER edge
 * offset (centerline shifted inward by halfWidth): if halfWidth exceeds the local
 * radius of curvature, the offset curve folds over itself even though the
 * centerline is fine. Radius of curvature was solved analytically at both the
 * waist (theta=pi/2) and the lobe tip (theta=0) for a=340,b=260,pinch=0.35:
 * R_waist ~= 250, R_lobeTip ~= 132 — both comfortably above halfWidth=80
 * (ratios ~3.1x and ~1.65x), so the inner edge stays simple everywhere.
 */
class Track {
    final double cx, cy, a, b, pinch, trackWidth, halfWidth;
    final int samples;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;
    double innerSign = 1.0;

    final int startIndex, waistTopIndex, waistBottomIndex, lobeLeftIndex;

    Track(double cx, double cy, double a, double b, double pinch, double trackWidth, int samples) {
        this.cx = cx; this.cy = cy; this.a = a; this.b = b;
        this.pinch = Util.clamp(pinch, 0, 0.85);
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        this.samples = samples;
        this.startIndex = 0;
        this.waistTopIndex = samples / 4;
        this.lobeLeftIndex = samples / 2;
        this.waistBottomIndex = (3 * samples) / 4;
        build();
    }

    private void build() {
        for (int i = 0; i < samples; i++) {
            double theta = (2 * Math.PI * i) / samples;
            double rf = 1 + pinch * Math.cos(2 * theta);
            double px = a * Math.cos(theta) * rf;
            double py = b * Math.sin(theta) * rf;
            centerline.add(new Point2D.Double(cx + px, cy + py));
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

        Point2D.Double p = centerline.get(waistTopIndex);
        Point2D.Double nrm = normals.get(waistTopIndex);
        double dHere = Point2D.distance(p.x, p.y, cx, cy);
        double dPoke = Point2D.distance(p.x + nrm.x * 5, p.y + nrm.y * 5, cx, cy);
        innerSign = (dPoke < dHere) ? 1.0 : -1.0;
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
        int n = centerline.size();
        int idx = ((index % n) + n) % n;
        Point2D.Double p = centerline.get(idx);
        Point2D.Double nn = normals.get(idx);
        return new Point2D.Double(p.x + nn.x * offset, p.y + nn.y * offset);
    }

    Point2D.Double startPosition(double laneOffset) {
        return pointAtOffset(startIndex + 3, laneOffset);
    }

    double startAngle() {
        Point2D.Double p0 = centerline.get(startIndex);
        Point2D.Double p1 = centerline.get((startIndex + 5) % centerline.size());
        return Math.atan2(p1.y - p0.y, p1.x - p0.x);
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
        g.setColor(new Color(196, 158, 108));
        g.fill(road);

        // faint tire-tread stripes across the sand road
        g.setColor(new Color(150, 118, 78, 110));
        for (int i = 0; i < centerline.size(); i += 6) {
            Point2D.Double o = pointAtOffset(i, halfWidth);
            Point2D.Double in = pointAtOffset(i, -halfWidth);
            g.draw(new Line2D.Double(o, in));
        }

        g.setColor(new Color(120, 92, 58));
        g.setStroke(new BasicStroke(4));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(250, 224, 170, 150));
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

    private void drawStartLine(Graphics2D g) {
        Point2D.Double o = pointAtOffset(startIndex, halfWidth);
        Point2D.Double in = pointAtOffset(startIndex, -halfWidth);
        int squares = 8;
        double dx = (o.x - in.x) / squares, dy = (o.y - in.y) / squares;
        for (int i = 0; i < squares; i++) {
            double sx = in.x + dx * i, sy = in.y + dy * i;
            g.setColor(i % 2 == 0 ? Color.WHITE : Color.BLACK);
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
                g.setColor(new Color(120, 100, 82));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(84, 68, 54));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(150, 128, 102));
                g.fillOval((int) (-radius * 0.4), (int) (-radius * 0.3), (int) (radius), (int) (radius * 0.8));
                break;
            case CACTUS:
                g.setColor(new Color(60, 108, 74));
                g.fillRoundRect((int) (-radius * 0.35), (int) -radius, (int) (radius * 0.7), (int) (radius * 2), 6, 6);
                g.fillRoundRect((int) (-radius * 0.9), (int) (-radius * 0.2), (int) (radius * 0.6), (int) (radius * 0.9), 5, 5);
                g.fillRoundRect((int) (radius * 0.3), (int) (-radius * 0.5), (int) (radius * 0.6), (int) (radius * 0.9), 5, 5);
                g.setColor(new Color(40, 82, 54));
                g.drawRoundRect((int) (-radius * 0.35), (int) -radius, (int) (radius * 0.7), (int) (radius * 2), 6, 6);
                break;
            case WRECK:
                g.setColor(new Color(96, 62, 40));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(new Color(60, 34, 22));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) (radius * 0.3));
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                break;
            case TUMBLEWEED:
                g.setColor(new Color(168, 140, 96));
                for (int i = 0; i < 7; i++) {
                    double ang = i * Math.PI * 2 / 7;
                    g.drawLine(0, 0, (int) (Math.cos(ang) * radius), (int) (Math.sin(ang) * radius));
                }
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                break;
        }
        g.setTransform(old);
    }
}

/** Purely decorative dusk-desert dressing around the outer rim: no collision. */
class DuneDecor {
    enum Kind { ROCK_SPIRE, DEAD_TREE, BONES }
    final Kind kind;
    final double x, y, lean, scale;

    DuneDecor(Kind kind, double x, double y, double lean, double scale) {
        this.kind = kind; this.x = x; this.y = y; this.lean = lean; this.scale = scale;
    }

    void draw(Graphics2D g) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(lean);
        g.scale(scale, scale);
        switch (kind) {
            case ROCK_SPIRE:
                g.setColor(new Color(70, 54, 66, 200));
                Path2D.Double spire = new Path2D.Double();
                spire.moveTo(-14, 0);
                spire.lineTo(-6, -46);
                spire.lineTo(4, -60);
                spire.lineTo(12, -30);
                spire.lineTo(16, 0);
                spire.closePath();
                g.fill(spire);
                g.setColor(new Color(110, 84, 90, 160));
                g.drawLine(0, -20, 6, -50);
                break;
            case DEAD_TREE:
                g.setColor(new Color(50, 40, 42, 210));
                g.fillRect(-3, -40, 6, 40);
                g.drawLine(0, -30, -16, -46);
                g.drawLine(0, -34, 14, -50);
                g.drawLine(0, -22, -12, -8);
                break;
            case BONES:
                g.setColor(new Color(200, 190, 170, 190));
                g.fillOval(-10, -14, 20, 12);
                g.drawLine(-16, -8, 16, -8);
                g.fillOval(-18, -11, 6, 6);
                g.fillOval(12, -11, 6, 6);
                break;
        }
        g.setTransform(old);
    }
}

/**
 * Signature hazard: "Shifting Dune Walls". Two low sand-wall clusters flank
 * each pinch of the waist (one per side of the lane), each cluster made of a
 * handful of mounds that grow taller/thinner and creep further into the lane
 * on a slow rhythmic sine cycle, then recede — periodically narrowing and
 * widening the passable gap through the pinch instead of leaving it constant.
 */
class DuneWall {
    private static final int[] REL = {-14, -7, 0, 7, 14};
    private static final double[] WEIGHT = {0.35, 0.72, 1.0, 0.72, 0.35};

    final int centerIndex, side;
    final double period, phase, maxReach, baseRadius;
    double reachNow, heightNow;

    DuneWall(int centerIndex, int side, double period, double phase, double maxReach, double baseRadius) {
        this.centerIndex = centerIndex;
        this.side = side;
        this.period = period;
        this.phase = phase;
        this.maxReach = maxReach;
        this.baseRadius = baseRadius;
    }

    void update(double t) {
        double cyc = 0.5 + 0.5 * Math.sin(2 * Math.PI * t / period + phase);
        reachNow = cyc * maxReach;
        heightNow = 26 + cyc * 66;
    }

    int moundCount() { return REL.length; }

    private double reachAt(int i) { return reachNow * WEIGHT[i]; }

    Point2D.Double moundPos(Track track, int i) {
        int idx = centerIndex + REL[i];
        double offset = side * (track.halfWidth + 8 - reachAt(i));
        return track.pointAtOffset(idx, offset);
    }

    double moundRadius(int i) {
        double t = maxReach > 0 ? reachAt(i) / maxReach : 0;
        return baseRadius * (1.0 - 0.3 * t);
    }

    double moundHeight(int i) { return heightNow * WEIGHT[i]; }

    boolean active(int i) { return reachAt(i) > 4; }

    void draw(Graphics2D g, Track track) {
        for (int i = 0; i < moundCount(); i++) {
            Point2D.Double p = moundPos(track, i);
            double r = moundRadius(i);
            double h = moundHeight(i);
            // dune shadow (cool twilight tone) on the lee side
            g.setColor(new Color(70, 58, 96, 130));
            g.fill(new Ellipse2D.Double(p.x - r, p.y - r * 0.6 + 3, r * 2, r * 1.2));
            // tall sand spike body
            Path2D.Double dune = new Path2D.Double();
            dune.moveTo(p.x - r, p.y);
            dune.lineTo(p.x - r * 0.25, p.y - h);
            dune.lineTo(p.x + r * 0.25, p.y - h);
            dune.lineTo(p.x + r, p.y);
            dune.closePath();
            g.setColor(new Color(214, 158, 96));
            g.fill(dune);
            // warm highlight catching the last light along one flank
            g.setColor(new Color(255, 208, 140, 200));
            g.fillRect((int) (p.x - r * 0.2), (int) (p.y - h), (int) Math.max(2, r * 0.3), (int) h);
            g.setColor(new Color(120, 78, 52));
            g.draw(dune);
        }
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
    boolean justLapped = false;

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
        justLapped = false;
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
        int newLap = Math.max(0, (int) Math.floor(unwrappedDistance / track.totalLength));
        if (newLap > lap) justLapped = true;
        lap = newLap;

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
        Color c = onTrack ? new Color(214, 186, 140, 120) : new Color(190, 156, 100, 160);
        for (int i = 0; i < (onTrack ? 1 : 2); i++) {
            double jx = (Math.random() - 0.5) * 10;
            double jy = (Math.random() - 0.5) * 10;
            particles.add(new Particle(rearX + jx, rearY + jy,
                    -vx * 0.15 + (Math.random() - 0.5) * 20, -vy * 0.15 + (Math.random() - 0.5) * 20,
                    0.5 + Math.random() * 0.4, 6 + Math.random() * 6, 8, c, Particle.ParticleKind.DUST));
        }
        if (boosting) {
            particles.add(new Particle(rearX, rearY, -vx * 0.3, -vy * 0.3, 0.35, 10, 6,
                    new Color(255, 150, 60, 200), Particle.ParticleKind.SPARK));
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
                    Math.random() < 0.5 ? new Color(255, 140, 60) : new Color(90, 80, 100), Particle.ParticleKind.SMOKE));
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
            g.setColor(new Color(255, 160, 60, 200));
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
        g.setColor(new Color(40, 40, 40));
        g.fillRect(14, -4, 8, 2);
        g.fillRect(14, 2, 8, 2);
        g.setColor(new Color(255, 230, 170));
        g.fillOval(14, -6, 4, 4);
        g.fillOval(14, 2, 4, 4);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.drawString(label, -5, 4);

        g.setTransform(old);

        double hpFrac = Util.clamp(health / 100.0, 0, 1);
        g.setColor(Color.DARK_GRAY);
        g.fillRect((int) x - 16, (int) y - 26, 32, 4);
        g.setColor(hpFrac > 0.5 ? new Color(90, 200, 90) : hpFrac > 0.25 ? Color.ORANGE : Color.RED);
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

    InputState think(double dt, List<Obstacle> obstacles, List<DuneWall> duneWalls) {
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

        for (DuneWall dw : duneWalls) {
            if (!dw.active(2)) continue;
            Point2D.Double mp = dw.moundPos(track, 2);
            double dx = mp.x - car.x, dy = mp.y - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < 120) {
                double angToObs = Util.normalizeAngle(Math.atan2(dy, dx) - car.angle);
                if (Math.abs(angToObs) < 1.0) {
                    diff += angToObs < 0 ? 0.7 : -0.7;
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
    private final List<DuneDecor> decor = new ArrayList<>();
    private final List<DuneWall> duneWalls = new ArrayList<>();
    private ParticleSystem particles = new ParticleSystem();
    private AIDriver aiDriver;
    private double[][] stars; // x, y, size, phase, speed

    private double countdownTimer;
    private double raceTime;
    private double timeAccum = 0;
    private final BufferedImage sceneBuffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        setupStars();
        setupWorld();
    }

    private void setupStars() {
        Random r = new Random(2024);
        int count = 140;
        stars = new double[count][5];
        for (int i = 0; i < count; i++) {
            stars[i][0] = r.nextDouble() * W;
            stars[i][1] = r.nextDouble() * H * 0.5;
            stars[i][2] = 0.6 + r.nextDouble() * 1.8;
            stars[i][3] = r.nextDouble() * Math.PI * 2;
            stars[i][4] = 0.5 + r.nextDouble() * 1.2;
        }
    }

    private void setupWorld() {
        // a=340,b=260,pinch=0.35: keeps inner-edge radius of curvature well above
        // halfWidth=80 at both the waist and the lobe tips (see Track's javadoc).
        track = new Track(W / 2.0, H / 2.0 - 10, 340, 260, 0.35, 160, 640);
        int n = track.centerline.size();

        obstacles.clear();
        Random r = new Random(42);
        for (int i = 0; i < n; i += 13) {
            if (r.nextDouble() < 0.45) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 22 + r.nextDouble() * 55;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.ROCK : Obstacle.Kind.CACTUS;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(10)));
            }
        }
        int[] hazardIdx = {50, 130, 210, 380, 460, 540};
        for (int idx : hazardIdx) {
            if (idx >= n) continue;
            double off = (r.nextDouble() - 0.5) * track.trackWidth * 0.5;
            Point2D.Double p = track.pointAtOffset(idx, off);
            obstacles.add(new Obstacle(Obstacle.Kind.WRECK, p.x, p.y, 15));
        }
        for (int i = 0; i < 3; i++) {
            Obstacle d = new Obstacle(Obstacle.Kind.TUMBLEWEED, r.nextInt(W), r.nextInt(H), 13);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 40 + r.nextDouble() * 40;
            d.vx = Math.cos(a) * sp;
            d.vy = Math.sin(a) * sp;
            obstacles.add(d);
        }

        decor.clear();
        Random rr = new Random(99);
        for (int i = 0; i < n; i += 22) {
            Point2D.Double p = track.pointAtOffset(i, -track.innerSign * (track.halfWidth + 55 + rr.nextInt(30)));
            double lean = (rr.nextDouble() - 0.5) * 0.3;
            double scale = 0.8 + rr.nextDouble() * 0.5;
            double roll = rr.nextDouble();
            DuneDecor.Kind kind = roll < 0.4 ? DuneDecor.Kind.ROCK_SPIRE : roll < 0.75 ? DuneDecor.Kind.DEAD_TREE : DuneDecor.Kind.BONES;
            decor.add(new DuneDecor(kind, p.x, p.y, lean, scale));
        }

        // Shifting Dune Walls: flank both pinch crossings of the waist (top and
        // bottom), one wall cluster per side of the lane. The two pinch zones
        // cycle out of phase (top narrows while bottom widens, and vice versa)
        // so the squeeze point keeps moving and must be timed rather than memorized.
        duneWalls.clear();
        double period = 7.0;
        double maxReach = 50;
        double baseRadius = 20;
        duneWalls.add(new DuneWall(track.waistTopIndex, 1, period, 0, maxReach, baseRadius));
        duneWalls.add(new DuneWall(track.waistTopIndex, -1, period, 0, maxReach, baseRadius));
        duneWalls.add(new DuneWall(track.waistBottomIndex, 1, period, Math.PI, maxReach, baseRadius));
        duneWalls.add(new DuneWall(track.waistBottomIndex, -1, period, Math.PI, maxReach, baseRadius));
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-22);
        Point2D.Double p2pos = track.startPosition(22);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(200, 70, 60), new Color(250, 210, 150), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(80, 96, 150), new Color(220, 220, 235), vsAI ? "AI" : "2", particles);
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
        timeAccum += dt;
        for (Obstacle o : obstacles) o.update(dt, -30, W + 30, -30, H + 30);
        for (DuneWall dw : duneWalls) dw.update(timeAccum);

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
            in2 = aiDriver.think(dt, obstacles, duneWalls);
        } else {
            in2 = new InputState();
            in2.throttle = keys.contains(KeyEvent.VK_UP) ? 1 : keys.contains(KeyEvent.VK_DOWN) ? -1 : 0;
            in2.steer = keys.contains(KeyEvent.VK_LEFT) ? -1 : keys.contains(KeyEvent.VK_RIGHT) ? 1 : 0;
            in2.boost = keys.contains(KeyEvent.VK_ENTER);
        }

        p1.update(dt, in1, track);
        p2.update(dt, in2, track);

        resolveCarCollision(p1, p2);
        for (Obstacle o : obstacles) {
            resolveObstacleCollision(p1, o);
            resolveObstacleCollision(p2, o);
        }
        for (DuneWall dw : duneWalls) {
            resolveDuneWallCollision(p1, dw);
            resolveDuneWallCollision(p2, dw);
        }

        Point2D.Double startPt = track.startPosition(0);
        if (p1.justLapped) particles.spawnFirework(startPt.x, startPt.y);
        if (p2.justLapped) particles.spawnFirework(startPt.x, startPt.y);

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
                        new Color(255, 210, 120), Particle.ParticleKind.SPARK));
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

    /** Signature hazard collision: each active dune mound behaves as a moving circular obstacle. */
    private void resolveDuneWallCollision(Car c, DuneWall dw) {
        if (!c.alive) return;
        for (int i = 0; i < dw.moundCount(); i++) {
            if (!dw.active(i)) continue;
            Point2D.Double m = dw.moundPos(track, i);
            double r = dw.moundRadius(i);
            double dx = c.x - m.x, dy = c.y - m.y;
            double dist = Math.hypot(dx, dy);
            double minDist = c.radius() + r;
            if (dist < minDist && dist > 0.0001) {
                double nx = dx / dist, ny = dy / dist;
                double overlap = minDist - dist;
                c.x += nx * overlap;
                c.y += ny * overlap;
                double impact = Math.max(70, Math.abs(c.speed));
                c.damage(impact * 0.12 + 6);
                c.speed *= -0.3;
                c.spinOut(0.7);
                for (int k = 0; k < 10; k++) {
                    double ang = Math.random() * Math.PI * 2;
                    double sp = 60 + Math.random() * 120;
                    particles.add(new Particle(c.x, c.y, Math.cos(ang) * sp, Math.sin(ang) * sp,
                            0.35 + Math.random() * 0.3, 4, 4, new Color(214, 178, 120), Particle.ParticleKind.DEBRIS));
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D sg = sceneBuffer.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBackground(sg);
        for (DuneDecor d : decor) d.draw(sg);
        track.draw(sg);
        for (Obstacle o : obstacles) o.draw(sg);
        for (DuneWall dw : duneWalls) dw.draw(sg, track);
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
        double skyBottom = H * 0.5;
        // twilight sky: deep blue-purple up top fading to warm amber near the horizon
        g.setPaint(new GradientPaint(0, 0, new Color(28, 22, 64), 0, (float) skyBottom, new Color(232, 142, 88)));
        g.fillRect(0, 0, W, (int) skyBottom);

        // early stars, twinkling, fading out as they approach the warm horizon band
        for (double[] s : stars) {
            double fadeByHeight = 1.0 - Util.clamp((s[1] - skyBottom * 0.55) / (skyBottom * 0.45), 0, 1);
            double twinkle = 0.4 + 0.6 * (0.5 + 0.5 * Math.sin(timeAccum * s[4] + s[3]));
            float alpha = (float) Util.clamp(fadeByHeight * twinkle, 0, 1);
            if (alpha <= 0.02) continue;
            g.setColor(new Color(1f, 0.97f, 0.9f, alpha));
            double sz = s[2];
            g.fill(new Ellipse2D.Double(s[0] - sz / 2, s[1] - sz / 2, sz, sz));
        }

        // last warm glow of the setting sun low on the horizon
        double pulse = 1 + 0.03 * Math.sin(timeAccum * 1.2);
        int sunR = (int) (60 * pulse);
        int sunX = W - 260, sunY = (int) (skyBottom - 20);
        g.setPaint(new RadialGradientPaint(new Point(sunX, sunY), sunR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(255, 210, 150, 220), new Color(240, 140, 90, 100), new Color(240, 140, 90, 0)}));
        g.fillOval(sunX - sunR * 3, sunY - sunR * 3, sunR * 6, sunR * 6);
        g.setColor(new Color(255, 225, 180));
        g.fillOval(sunX - sunR / 2, sunY - sunR / 2, sunR, sunR);

        // rolling dune ridge silhouette on the horizon, catching warm highlights on top
        Path2D.Double ridge = new Path2D.Double();
        ridge.moveTo(-50, skyBottom);
        double rx = -50;
        Random wr = new Random(7);
        while (rx < W + 50) {
            double amp = 26 + wr.nextInt(22);
            double rw = 90 + wr.nextInt(70);
            ridge.curveTo(rx + rw * 0.33, skyBottom - amp, rx + rw * 0.66, skyBottom - amp, rx + rw, skyBottom);
            rx += rw;
        }
        ridge.lineTo(W + 50, skyBottom + 40);
        ridge.lineTo(-50, skyBottom + 40);
        ridge.closePath();
        g.setColor(new Color(150, 96, 84));
        g.fill(ridge);
        g.setColor(new Color(214, 160, 120, 160));
        g.setStroke(new BasicStroke(3));
        g.draw(ridge);
        g.setStroke(new BasicStroke(1));

        // sand floor: warm near the horizon, cooling with a violet twilight tint further down
        g.setPaint(new GradientPaint(0, (float) skyBottom, new Color(210, 158, 108), 0, H, new Color(118, 96, 118)));
        g.fillRect(0, (int) skyBottom, W, (int) (H - skyBottom));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(10, 6, 24, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(240, 176, 110));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        centerText(g, "PEANUT DUNES SPEEDWAY", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Twilight dune racing — time your run through the shifting waist", H / 2 - 90);

        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        centerText(g, "[1] Player vs Player      [2] Player vs Computer", H / 2 - 20);
        centerText(g, "P1: W A S D  +  SPACE boost", H / 2 + 20);
        centerText(g, "P2: Arrow Keys + ENTER boost  (disabled vs Computer)", H / 2 + 46);
        centerText(g, "Press 1 or 2 to start", H / 2 + 100);
    }

    private void centerText(Graphics2D g, String s, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (W - fm.stringWidth(s)) / 2, y);
    }

    private void drawCountdown(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 90));
        g.setColor(new Color(255, 220, 120));
        String txt = countdownTimer > 3 ? "READY" : String.valueOf((int) Math.ceil(countdownTimer));
        if (countdownTimer <= 0) txt = "GO!";
        centerText(g, txt, H / 2);
        drawHud(g);
    }

    private void drawHud(Graphics2D g) {
        Car p1 = cars.get(0), p2 = cars.get(1);
        drawDriverHud(g, p1, 20, "P1");
        drawDriverHud(g, p2, W - 210, vsAI ? "CPU" : "P2");

        g.setColor(Color.WHITE);
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
        g.setColor(new Color(0, 0, 0, 130));
        g.fillRoundRect(x, 16, 190, 60, 10, 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString(tag + "  LAP " + Math.min(c.lap + 1, TOTAL_LAPS), x + 10, 32);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 10, 40, 170, 8);
        double hp = Util.clamp(c.health / 100.0, 0, 1);
        g.setColor(hp > 0.5 ? new Color(90, 200, 90) : hp > 0.25 ? Color.ORANGE : Color.RED);
        g.fillRect(x + 10, 40, (int) (170 * hp), 8);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 10, 54, 170, 8);
        g.setColor(new Color(230, 150, 90));
        g.fillRect(x + 10, 54, (int) (170 * Util.clamp(c.boostFuel / 100.0, 0, 1)), 8);
    }

    private void drawFinish(Graphics2D g) {
        g.setColor(new Color(8, 6, 20, 170));
        g.fillRect(0, 0, W, H);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 46));
        centerText(g, "RACE FINISHED", H / 2 - 80);
        Car p1 = cars.get(0), p2 = cars.get(1);
        Car winner = p1.finishTime <= p2.finishTime ? p1 : p2;
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
