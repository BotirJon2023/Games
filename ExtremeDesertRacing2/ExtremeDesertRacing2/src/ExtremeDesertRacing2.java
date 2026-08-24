import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ExtremeDesertRacing2 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Scorpion Canyon Clash — Extreme Desert Racing");
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

    /** Shared silhouette art for both the roaming swarm scorpions and the big background decorations. */
    static void drawScorpion(Graphics2D g, double size, double legPhase, Color color) {
        AffineTransform old = g.getTransform();
        g.scale(size / 30.0, size / 30.0);
        g.setColor(color);

        // legs (3 pairs), animated by legPhase
        g.setStroke(new BasicStroke(1.6f));
        for (int i = 0; i < 3; i++) {
            double lx = -6 + i * 6;
            double wig = Math.sin(legPhase + i * 1.3) * 4;
            g.draw(new Line2D.Double(lx, -2, lx - 8, -10 + wig));
            g.draw(new Line2D.Double(lx, -2, lx - 8, 10 - wig));
        }
        // pincers
        double pw = Math.sin(legPhase * 0.7) * 2;
        g.fill(new Ellipse2D.Double(-20, -6 - pw, 8, 6));
        g.fill(new Ellipse2D.Double(-20, 0 + pw, 8, 6));
        g.draw(new Line2D.Double(-14, -4, -8, -2));
        g.draw(new Line2D.Double(-14, 4, -8, 2));

        // body segments
        g.fill(new Ellipse2D.Double(-10, -7, 20, 14));
        g.fill(new Ellipse2D.Double(2, -5, 12, 10));

        // curled tail with stinger
        Path2D.Double tail = new Path2D.Double();
        tail.moveTo(10, 0);
        double curl = Math.sin(legPhase * 0.5) * 3;
        tail.curveTo(20, -6, 24, -18 + curl, 18, -24);
        g.setStroke(new BasicStroke(3f));
        g.draw(tail);
        g.fill(new Ellipse2D.Double(15, -27, 6, 6));

        g.setStroke(new BasicStroke(1));
        g.setTransform(old);
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
            double s = Math.max(0.5, p.size);
            g.fill(new Ellipse2D.Double(p.x - s / 2, p.y - s / 2, s, s));
            g.setComposite(old);
        }
    }
}

/**
 * Superellipse (|x/a|^n + |y/b|^n = 1) rounded-square canyon arena. A high
 * exponent (n=10) makes the near-corners read as tight and almost rectangular
 * while still guaranteeing a simple, non-self-intersecting closed loop: the
 * boundary radius r(t) is positive for every angle t in [0, 2*PI), so the
 * curve winds around the center exactly once with no crossings.
 *
 * Lap counting and edge rendering both reuse the proven technique: a
 * centerline point list with a parallel cumulative arc-length array (robust
 * even if the car strays off-centerline) and per-point normals for
 * offset-based inner/outer edges.
 */
class Track {
    final double centerX, centerY, a, b, exponent, trackWidth, halfWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;
    double innerSign = 1.0;

    Track(double centerX, double centerY, double a, double b, double exponent, double trackWidth) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.a = a;
        this.b = b;
        this.exponent = exponent;
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        build();
    }

    private void build() {
        int samples = 600;
        for (int i = 0; i < samples; i++) {
            double t = (2 * Math.PI * i) / samples;
            double ct = Math.cos(t), st = Math.sin(t);
            double px = Math.signum(ct) * Math.pow(Math.abs(ct), 2.0 / exponent) * a;
            double py = Math.signum(st) * Math.pow(Math.abs(st), 2.0 / exponent) * b;
            centerline.add(new Point2D.Double(centerX + px, centerY + py));
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

        int westIdx = samples / 2;
        Point2D.Double p = centerline.get(westIdx);
        Point2D.Double nrm = normals.get(westIdx);
        double dHere = Point2D.distance(p.x, p.y, centerX, centerY);
        double dPoke = Point2D.distance(p.x + nrm.x * 5, p.y + nrm.y * 5, centerX, centerY);
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

    /** Approximate index for a given arc-length position; good enough for cosmetic hazard patrol motion. */
    int indexAtArc(double arc) {
        double m = arc % totalLength;
        if (m < 0) m += totalLength;
        int n = centerline.size();
        int idx = (int) (m / totalLength * n);
        return ((idx % n) + n) % n;
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
        return pointAtOffset(2, laneOffset);
    }

    double startAngle() {
        Point2D.Double a = centerline.get(0);
        Point2D.Double b = centerline.get(5);
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
        g.setColor(new Color(168, 92, 48)); // warm rust-colored track surface
        g.fill(road);

        // baked-clay cracks across the road
        g.setColor(new Color(120, 60, 30, 130));
        for (int i = 0; i < centerline.size(); i += 6) {
            Point2D.Double o = pointAtOffset(i, halfWidth);
            Point2D.Double in = pointAtOffset(i, -halfWidth);
            g.draw(new Line2D.Double(o, in));
        }

        g.setColor(new Color(90, 46, 24));
        g.setStroke(new BasicStroke(4));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(240, 210, 160, 150));
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
        Point2D.Double o = pointAtOffset(0, halfWidth);
        Point2D.Double in = pointAtOffset(0, -halfWidth);
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
    enum Kind { ROCK, CACTUS, SKULL, TUMBLEWEED }

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
                g.setColor(new Color(140, 92, 66));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(96, 60, 40));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.fillOval((int) (-radius * 0.4), (int) (-radius * 0.3), (int) (radius), (int) (radius * 0.8));
                break;
            case CACTUS:
                g.setColor(new Color(60, 110, 60));
                g.fillRoundRect((int) (-radius * 0.35), (int) -radius, (int) (radius * 0.7), (int) (radius * 2), 8, 8);
                g.fillRoundRect((int) (-radius * 0.9), (int) (-radius * 0.2), (int) (radius * 0.6), (int) (radius * 0.9), 6, 6);
                g.fillRoundRect((int) (radius * 0.3), (int) (-radius * 0.5), (int) (radius * 0.6), (int) (radius * 0.9), 6, 6);
                g.setColor(new Color(40, 80, 40));
                g.drawRoundRect((int) (-radius * 0.35), (int) -radius, (int) (radius * 0.7), (int) (radius * 2), 8, 8);
                break;
            case SKULL:
                g.setColor(new Color(230, 220, 200));
                g.fillOval((int) -radius, (int) (-radius * 0.7), (int) (radius * 2), (int) (radius * 1.4));
                g.setColor(new Color(40, 36, 30));
                g.fillOval((int) (-radius * 0.5), (int) (-radius * 0.2), (int) (radius * 0.35), (int) (radius * 0.35));
                g.fillOval((int) (radius * 0.15), (int) (-radius * 0.2), (int) (radius * 0.35), (int) (radius * 0.35));
                g.drawLine(0, (int) (radius * 0.15), 0, (int) (radius * 0.5));
                break;
            case TUMBLEWEED:
                g.setColor(new Color(150, 118, 70));
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI / 4;
                    g.drawLine((int) (Math.cos(ang) * -radius * 0.6), (int) (Math.sin(ang) * -radius * 0.6),
                            (int) (Math.cos(ang) * radius), (int) (Math.sin(ang) * radius));
                }
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                break;
        }
        g.setTransform(old);
    }
}

/** Purely decorative canyon dressing around the outer rim: no collision. */
class SceneryProp {
    enum Kind { SPIRE, ARCH, SCORPION }
    final Kind kind;
    final double x, y, lean, scale;

    SceneryProp(Kind kind, double x, double y, double lean, double scale) {
        this.kind = kind; this.x = x; this.y = y; this.lean = lean; this.scale = scale;
    }

    void draw(Graphics2D g) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(lean);
        switch (kind) {
            case SPIRE:
                g.setColor(new Color(160, 76, 40, 220));
                Path2D.Double spire = new Path2D.Double();
                spire.moveTo(-16 * scale, 0);
                spire.lineTo(-6 * scale, -80 * scale);
                spire.lineTo(2 * scale, -60 * scale);
                spire.lineTo(10 * scale, -95 * scale);
                spire.lineTo(16 * scale, 0);
                spire.closePath();
                g.fill(spire);
                g.setColor(new Color(110, 48, 24, 220));
                for (int i = 1; i <= 3; i++) {
                    g.drawLine((int) (-14 * scale), (int) (-i * 20 * scale),
                            (int) (14 * scale), (int) (-i * 20 * scale));
                }
                break;
            case ARCH:
                g.setColor(new Color(176, 90, 50, 210));
                g.fillRoundRect((int) (-40 * scale), (int) (-60 * scale), (int) (16 * scale), (int) (60 * scale), 6, 6);
                g.fillRoundRect((int) (24 * scale), (int) (-60 * scale), (int) (16 * scale), (int) (60 * scale), 6, 6);
                g.fill(new Arc2D.Double(-40 * scale, -100 * scale, 80 * scale, 80 * scale, 0, 180, Arc2D.PIE));
                break;
            case SCORPION:
                g.scale(scale, scale);
                Util.drawScorpion(g, 60, 0, new Color(40, 20, 16, 200));
                break;
        }
        g.setTransform(old);
    }
}

/**
 * Signature hazard: 2-3 roaming clusters of small scorpions that slowly
 * patrol around the track (their center drifts along the centerline's arc
 * length while oscillating side to side, sometimes crossing the racing
 * lane). Each zone cycles DORMANT -> TELEGRAPH -> ACTIVE -> DORMANT so a
 * skilled player can see the warning pulse and dodge before it stings.
 */
class ScorpionSwarmZone {
    enum Phase { DORMANT, TELEGRAPH, ACTIVE }

    private static final double DORMANT_TIME = 4.0;
    private static final double TELEGRAPH_TIME = 1.7;
    private static final double ACTIVE_TIME = 2.6;

    final double radius = 58;
    double x, y;
    Phase phase = Phase.DORMANT;
    double phaseTimer;

    private final double arcStart;
    private final double arcSpeed;
    private final double perpAmplitude;
    private final double perpFreq;
    private final double perpPhase;

    private final double[] scorpAngle;
    private final double[] scorpRadius;
    private final double[] legPhase;

    private final double[] stingCooldown = new double[2];

    ScorpionSwarmZone(double arcStart, double arcSpeed, double perpAmplitude, double perpFreq, double perpPhase, double startDelay) {
        this.arcStart = arcStart;
        this.arcSpeed = arcSpeed;
        this.perpAmplitude = perpAmplitude;
        this.perpFreq = perpFreq;
        this.perpPhase = perpPhase;
        this.phaseTimer = DORMANT_TIME - startDelay;

        int count = 6;
        scorpAngle = new double[count];
        scorpRadius = new double[count];
        legPhase = new double[count];
        Random r = new Random((long) (arcStart * 1000 + 7));
        for (int i = 0; i < count; i++) {
            scorpAngle[i] = r.nextDouble() * Math.PI * 2;
            scorpRadius[i] = 12 + r.nextDouble() * (radius - 20);
            legPhase[i] = r.nextDouble() * Math.PI * 2;
        }
    }

    void update(double dt, double timeAccum, Track track) {
        double arc = arcStart + timeAccum * arcSpeed;
        int idx = track.indexAtArc(arc);
        double perp = Math.sin(timeAccum * perpFreq + perpPhase) * perpAmplitude;
        Point2D.Double p = track.pointAtOffset(idx, perp);
        x = p.x; y = p.y;

        phaseTimer -= dt;
        if (phaseTimer <= 0) {
            switch (phase) {
                case DORMANT: phase = Phase.TELEGRAPH; phaseTimer = TELEGRAPH_TIME; break;
                case TELEGRAPH: phase = Phase.ACTIVE; phaseTimer = ACTIVE_TIME; break;
                case ACTIVE: phase = Phase.DORMANT; phaseTimer = DORMANT_TIME; break;
            }
        }

        double orbitSpeed = phase == Phase.ACTIVE ? 3.2 : phase == Phase.TELEGRAPH ? 2.0 : 0.7;
        double legSpeed = phase == Phase.ACTIVE ? 14.0 : phase == Phase.TELEGRAPH ? 9.0 : 4.0;
        for (int i = 0; i < scorpAngle.length; i++) {
            scorpAngle[i] += orbitSpeed * dt * 0.3;
            legPhase[i] += legSpeed * dt;
        }
        for (int i = 0; i < stingCooldown.length; i++) stingCooldown[i] = Math.max(0, stingCooldown[i] - dt);
    }

    boolean isDangerous() { return phase != Phase.DORMANT; }

    /** Applies contact effects to a car (carId 0 or 1). Returns true if the car is currently touching the zone. */
    boolean applyContact(Car c, int carId, double dt) {
        double dist = Math.hypot(c.x - x, c.y - y);
        if (dist >= radius + c.radius()) return false;
        if (phase != Phase.ACTIVE) return true;
        c.damage(6.0 * dt);
        if (stingCooldown[carId] <= 0) {
            c.spinOut(0.8);
            stingCooldown[carId] = 1.2;
        }
        return true;
    }

    void draw(Graphics2D g) {
        Color ring;
        switch (phase) {
            case TELEGRAPH: {
                double pulse = 0.5 + 0.5 * Math.sin(legPhase[0] * 1.6);
                ring = new Color(255, 200, 40, (int) (140 + 90 * pulse));
                g.setStroke(new BasicStroke(3));
                g.setColor(ring);
                double r = radius * (0.85 + 0.25 * pulse);
                g.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
                g.setStroke(new BasicStroke(1));
                break;
            }
            case ACTIVE: {
                g.setColor(new Color(220, 30, 20, 70));
                g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
                g.setStroke(new BasicStroke(3));
                g.setColor(new Color(255, 60, 40, 200));
                g.draw(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
                g.setStroke(new BasicStroke(1));
                break;
            }
            default:
                g.setColor(new Color(120, 60, 30, 40));
                g.fill(new Ellipse2D.Double(x - radius * 0.7, y - radius * 0.7, radius * 1.4, radius * 1.4));
                break;
        }

        Color body = phase == Phase.ACTIVE ? new Color(60, 8, 8) : new Color(30, 20, 16);
        for (int i = 0; i < scorpAngle.length; i++) {
            double sx = x + Math.cos(scorpAngle[i]) * scorpRadius[i];
            double sy = y + Math.sin(scorpAngle[i]) * scorpRadius[i];
            AffineTransform old = g.getTransform();
            g.translate(sx, sy);
            g.rotate(scorpAngle[i] + Math.PI / 2);
            Util.drawScorpion(g, 16, legPhase[i], body);
            g.setTransform(old);
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
        boolean onTrack = track.distanceFromCenterline(x, y) < track.halfWidth;
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
        Color c = onTrack ? new Color(210, 140, 90, 120) : new Color(180, 110, 60, 160);
        for (int i = 0; i < (onTrack ? 1 : 2); i++) {
            double jx = (Math.random() - 0.5) * 10;
            double jy = (Math.random() - 0.5) * 10;
            particles.add(new Particle(rearX + jx, rearY + jy,
                    -vx * 0.15 + (Math.random() - 0.5) * 20, -vy * 0.15 + (Math.random() - 0.5) * 20,
                    0.5 + Math.random() * 0.4, 6 + Math.random() * 6, 8, c, Particle.ParticleKind.DUST));
        }
        if (boosting) {
            particles.add(new Particle(rearX, rearY, -vx * 0.3, -vy * 0.3, 0.35, 10, 6,
                    new Color(255, 140, 40, 200), Particle.ParticleKind.SPARK));
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
                    Math.random() < 0.5 ? new Color(255, 120, 30) : new Color(90, 90, 90), Particle.ParticleKind.SMOKE));
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
            g.setColor(new Color(255, 150, 40, 200));
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
        g.setColor(new Color(255, 220, 150));
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

    InputState think(double dt, List<Obstacle> obstacles, List<ScorpionSwarmZone> zones) {
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

        for (ScorpionSwarmZone z : zones) {
            if (!z.isDangerous()) continue;
            double dx = z.x - car.x, dy = z.y - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < z.radius + 110) {
                double angToObs = Util.normalizeAngle(Math.atan2(dy, dx) - car.angle);
                if (Math.abs(angToObs) < 1.1) {
                    diff += angToObs < 0 ? 0.85 : -0.85;
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
    private final List<SceneryProp> scenery = new ArrayList<>();
    private final List<ScorpionSwarmZone> swarmZones = new ArrayList<>();
    private ParticleSystem particles = new ParticleSystem();
    private AIDriver aiDriver;

    private double countdownTimer;
    private double raceTime;
    private double timeAccum = 0;
    private final BufferedImage sceneBuffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        setupWorld();
    }

    private void setupWorld() {
        track = new Track(W / 2.0, H / 2.0 - 10, 380, 260, 10.0, 100);
        int n = track.centerline.size();

        obstacles.clear();
        Random r = new Random(42);
        for (int i = 0; i < n; i += 11) {
            if (r.nextDouble() < 0.5) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 20 + r.nextDouble() * 55;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.ROCK : Obstacle.Kind.CACTUS;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(10)));
            }
        }
        int[] hazardIdx = {40, 120, 200, 280, 360, 440};
        for (int idx : hazardIdx) {
            if (idx >= n) continue;
            double off = (r.nextDouble() - 0.5) * track.trackWidth * 0.55;
            Point2D.Double p = track.pointAtOffset(idx, off);
            obstacles.add(new Obstacle(Obstacle.Kind.SKULL, p.x, p.y, 14));
        }
        for (int i = 0; i < 3; i++) {
            Obstacle d = new Obstacle(Obstacle.Kind.TUMBLEWEED, r.nextInt(W), r.nextInt(H), 13);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 40 + r.nextDouble() * 40;
            d.vx = Math.cos(a) * sp;
            d.vy = Math.sin(a) * sp;
            obstacles.add(d);
        }

        // Sandstone spires, rock arches and big scorpion silhouettes ringing the outer rim.
        scenery.clear();
        Random rr = new Random(99);
        for (int i = 0; i < n; i += 18) {
            Point2D.Double p = track.pointAtOffset(i, -track.innerSign * (track.halfWidth + 60 + rr.nextInt(70)));
            double lean = (rr.nextDouble() - 0.5) * 0.25;
            double scale = 0.8 + rr.nextDouble() * 0.6;
            double roll = rr.nextDouble();
            SceneryProp.Kind kind = roll < 0.45 ? SceneryProp.Kind.SPIRE : roll < 0.75 ? SceneryProp.Kind.ARCH : SceneryProp.Kind.SCORPION;
            scenery.add(new SceneryProp(kind, p.x, p.y, lean, scale));
        }

        // Signature hazard: 3 roaming scorpion swarm zones patrolling around the loop,
        // offset in starting arc position and phase so they never sync up.
        swarmZones.clear();
        swarmZones.add(new ScorpionSwarmZone(0, 14, track.trackWidth * 1.1, 0.17, 0.0, 0.0));
        swarmZones.add(new ScorpionSwarmZone(track.totalLength / 3.0, -11, track.trackWidth * 0.9, 0.13, 1.7, 2.0));
        swarmZones.add(new ScorpionSwarmZone(track.totalLength * 2.0 / 3.0, 12, track.trackWidth * 1.2, 0.21, 3.2, 4.0));
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-22);
        Point2D.Double p2pos = track.startPosition(22);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(200, 60, 40), new Color(230, 200, 150), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(70, 90, 130), new Color(210, 210, 200), vsAI ? "AI" : "2", particles);
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

        switch (state) {
            case COUNTDOWN:
                countdownTimer -= dt;
                if (countdownTimer <= 0) state = State.RACING;
                for (ScorpionSwarmZone z : swarmZones) z.update(dt, timeAccum, track);
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
        for (ScorpionSwarmZone z : swarmZones) z.update(dt, timeAccum, track);

        Car p1 = cars.get(0);
        Car p2 = cars.get(1);

        InputState in1 = new InputState();
        in1.throttle = keys.contains(KeyEvent.VK_W) ? 1 : keys.contains(KeyEvent.VK_S) ? -1 : 0;
        in1.steer = keys.contains(KeyEvent.VK_A) ? -1 : keys.contains(KeyEvent.VK_D) ? 1 : 0;
        in1.boost = keys.contains(KeyEvent.VK_SPACE);

        InputState in2;
        if (vsAI) {
            in2 = aiDriver.think(dt, obstacles, swarmZones);
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
        for (ScorpionSwarmZone z : swarmZones) {
            boolean touching1 = z.applyContact(p1, 0, dt);
            boolean touching2 = z.applyContact(p2, 1, dt);
            if ((touching1 || touching2) && z.phase == ScorpionSwarmZone.Phase.ACTIVE) {
                if (Math.random() < 0.4) spawnStingSpark(z.x, z.y);
            }
        }

        // Purely celebratory: fireworks-style spark burst whenever a car crosses the start/finish line.
        Point2D.Double startPt = track.startPosition(0);
        if (p1.justLapped) spawnLapBurst(startPt.x, startPt.y);
        if (p2.justLapped) spawnLapBurst(startPt.x, startPt.y);

        for (Car c : cars) {
            if (!c.finished && c.lap >= TOTAL_LAPS) {
                c.finished = true;
                c.finishTime = raceTime;
            }
        }
        if (p1.finished && p2.finished) state = State.FINISHED;
    }

    private void spawnStingSpark(double x, double y) {
        for (int i = 0; i < 4; i++) {
            double a = Math.random() * Math.PI * 2;
            double sp = 40 + Math.random() * 80;
            particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp,
                    0.3, 3, 3, new Color(255, 220, 60), Particle.ParticleKind.SPARK));
        }
    }

    private void spawnLapBurst(double x, double y) {
        Color[] palette = {
                new Color(255, 210, 80), new Color(255, 120, 60),
                new Color(255, 250, 220), new Color(220, 60, 60)
        };
        Random r = new Random();
        int count = 30;
        for (int i = 0; i < count; i++) {
            double ang = (Math.PI * 2 * i) / count;
            double sp = 90 + r.nextDouble() * 140;
            Color c = palette[r.nextInt(palette.length)];
            particles.add(new Particle(x, y - 10, Math.cos(ang) * sp, Math.sin(ang) * sp - 40,
                    0.6 + r.nextDouble() * 0.5, 3 + r.nextDouble() * 4, -3, c, Particle.ParticleKind.SPARK));
        }
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
                        new Color(255, 200, 80), Particle.ParticleKind.SPARK));
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
        for (SceneryProp s : scenery) s.draw(sg);
        track.draw(sg);
        for (Obstacle o : obstacles) o.draw(sg);
        if (state == State.COUNTDOWN || state == State.RACING) {
            for (ScorpionSwarmZone z : swarmZones) z.draw(sg);
        }
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
        // harsh, bright midday blue sky
        g.setPaint(new GradientPaint(0, 0, new Color(60, 150, 235), 0, (float) (H * 0.42), new Color(150, 205, 245)));
        g.fillRect(0, 0, W, (int) (H * 0.42));

        // fierce midday sun
        g.setColor(new Color(255, 250, 220, 230));
        g.fillOval(W - 140, 40, 60, 60);
        g.setPaint(new RadialGradientPaint(new Point(W - 110, 70), 110,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 250, 220, 90), new Color(255, 250, 220, 0)}));
        g.fillOval(W - 220, -40, 220, 220);

        // distant deep red/orange sandstone cliff wall silhouette framing the horizon
        g.setColor(new Color(150, 66, 34));
        Path2D.Double wall = new Path2D.Double();
        wall.moveTo(-50, H * 0.42);
        double wx = -50;
        Random wr = new Random(7);
        boolean up = true;
        while (wx < W + 50) {
            double wy = up ? H * 0.42 - (70 + wr.nextInt(50)) : H * 0.42 - (24 + wr.nextInt(24));
            wall.lineTo(wx, wy);
            wx += 18 + wr.nextInt(14);
            up = !up;
        }
        wall.lineTo(W + 50, H * 0.42);
        wall.closePath();
        g.fill(wall);
        g.setColor(new Color(110, 44, 20));
        g.draw(wall);

        // rust-colored canyon floor
        g.setPaint(new GradientPaint(0, (float) (H * 0.4), new Color(198, 122, 76), 0, H, new Color(150, 84, 48)));
        g.fillRect(0, (int) (H * 0.4), W, (int) (H * 0.6));

        // left/right jagged cliff walls framing the arena
        drawSideCliff(g, true);
        drawSideCliff(g, false);
    }

    private void drawSideCliff(Graphics2D g, boolean left) {
        g.setColor(new Color(176, 78, 40, 235));
        Path2D.Double p = new Path2D.Double();
        int baseX = left ? 0 : W;
        int dir = left ? 1 : -1;
        p.moveTo(baseX, -20);
        Random r = new Random(left ? 11 : 23);
        double y = -20;
        while (y < H + 20) {
            double edge = baseX + dir * (30 + r.nextInt(50));
            p.lineTo(edge, y);
            y += 24 + r.nextInt(30);
        }
        p.lineTo(baseX, H + 20);
        p.closePath();
        g.fill(p);
        g.setColor(new Color(120, 50, 20, 235));
        g.draw(p);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(240, 130, 60));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        centerText(g, "SCORPION CANYON CLASH", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Red rock desert racing — dodge the roaming scorpion swarms", H / 2 - 90);

        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        centerText(g, "[1] Player vs Player      [2] Player vs Computer", H / 2 - 20);
        centerText(g, "P1: W A S D  +  SPACE boost", H / 2 + 20);
        centerText(g, "P2: Arrow Keys + ENTER boost  (disabled vs Computer)", H / 2 + 46);
        centerText(g, "Watch for the yellow pulse — it means the swarm is about to sting!", H / 2 + 80);
        centerText(g, "Press 1 or 2 to start", H / 2 + 116);
    }

    private void centerText(Graphics2D g, String s, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (W - fm.stringWidth(s)) / 2, y);
    }

    private void drawCountdown(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 90));
        g.setColor(new Color(255, 220, 60));
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
        g.setColor(new Color(80, 160, 255));
        g.fillRect(x + 10, 54, (int) (170 * Util.clamp(c.boostFuel / 100.0, 0, 1)), 8);
    }

    private void drawFinish(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 170));
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
