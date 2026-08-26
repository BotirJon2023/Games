import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ExtremeDesertRacing4 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Cactus Gulch Grand Prix");
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

    static double[] normalize(double x, double y) {
        double len = Math.hypot(x, y);
        if (len < 1e-9) len = 1e-9;
        return new double[]{x / len, y / len};
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

    enum ParticleKind { DUST, SMOKE, SPARK, DEBRIS, CONFETTI }

    Particle(double x, double y, double vx, double vy, double life, double size, double growth, Color color, ParticleKind kind) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.life = life; this.maxLife = life;
        this.size = size; this.growth = growth;
        this.color = color; this.kind = kind;
    }

    boolean update(double dt) {
        x += vx * dt;
        y += vy * dt;
        if (kind == ParticleKind.CONFETTI) {
            vy += 50 * dt;
            vx *= 0.99;
        } else {
            vx *= 0.97;
            vy *= 0.97;
        }
        size += growth * dt;
        life -= dt;
        return life > 0;
    }

    float alpha() {
        return (float) Util.clamp(life / maxLife, 0, 1);
    }
}

/** Thread-safe particle system: physics thread and Swing EDT both touch it. */
class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();

    synchronized void add(Particle p) { particles.add(p); }

    synchronized void update(double dt) {
        particles.removeIf(p -> !p.update(dt));
    }

    void spawnBurst(double x, double y) {
        Color[] palette = {
                new Color(255, 210, 90), new Color(230, 120, 50),
                new Color(255, 250, 220), new Color(120, 170, 70)
        };
        Random r = new Random();
        int count = 30;
        for (int i = 0; i < count; i++) {
            double ang = (Math.PI * 2 * i) / count + (r.nextDouble() - 0.5) * 0.3;
            double sp = 90 + r.nextDouble() * 130;
            Color c = palette[r.nextInt(palette.length)];
            add(new Particle(x, y - 10, Math.cos(ang) * sp, Math.sin(ang) * sp - 40,
                    0.6 + r.nextDouble() * 0.5, 3 + r.nextDouble() * 4, -3, c, Particle.ParticleKind.CONFETTI));
        }
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
 * "Tri-oval" track: a rounded triangle produced by per-vertex fillet math.
 * Three straight vertices define a triangle; each corner is replaced by a
 * circular arc of the same radius, tangent to both adjoining edges, joined
 * by straight segments. Because every fillet is convex and the underlying
 * polygon is convex, the resulting sampled centerline is a single simple
 * (non-self-intersecting) closed loop.
 *
 * Lap counting and edge rendering reuse the generic centerline + cumulative
 * arc-length + normals technique: works for any simple closed point loop,
 * regardless of how it was generated.
 */
class Track {
    final double halfWidth, trackWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;
    double innerSign = 1.0;
    final double centerX, centerY;
    private int startIndex;

    Track(Point2D.Double v0, Point2D.Double v1, Point2D.Double v2, double filletRadius, double trackWidth) {
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        this.centerX = (v0.x + v1.x + v2.x) / 3.0;
        this.centerY = (v0.y + v1.y + v2.y) / 3.0;
        build(new Point2D.Double[]{v0, v1, v2}, filletRadius);
    }

    private void build(Point2D.Double[] v, double r) {
        int n = v.length;
        Point2D.Double[] t1 = new Point2D.Double[n];
        Point2D.Double[] t2 = new Point2D.Double[n];
        Point2D.Double[] arcCenter = new Point2D.Double[n];
        double[] a1 = new double[n];
        double[] a2 = new double[n];
        double[] delta = new double[n];

        for (int i = 0; i < n; i++) {
            Point2D.Double prev = v[(i - 1 + n) % n];
            Point2D.Double curr = v[i];
            Point2D.Double next = v[(i + 1) % n];
            double[] d1 = Util.normalize(prev.x - curr.x, prev.y - curr.y);
            double[] d2 = Util.normalize(next.x - curr.x, next.y - curr.y);
            double dot = Util.clamp(d1[0] * d2[0] + d1[1] * d2[1], -1, 1);
            double theta = Math.acos(dot);
            double half = theta / 2;
            double tanLen = r / Math.tan(half);
            t1[i] = new Point2D.Double(curr.x + d1[0] * tanLen, curr.y + d1[1] * tanLen);
            t2[i] = new Point2D.Double(curr.x + d2[0] * tanLen, curr.y + d2[1] * tanLen);
            double[] bis = Util.normalize(d1[0] + d2[0], d1[1] + d2[1]);
            double centerDist = r / Math.sin(half);
            arcCenter[i] = new Point2D.Double(curr.x + bis[0] * centerDist, curr.y + bis[1] * centerDist);
            a1[i] = Math.atan2(t1[i].y - arcCenter[i].y, t1[i].x - arcCenter[i].x);
            a2[i] = Math.atan2(t2[i].y - arcCenter[i].y, t2[i].x - arcCenter[i].x);
            delta[i] = Util.normalizeAngle(a2[i] - a1[i]);
        }

        int straightSamples = 44;
        int arcSamples = 56;
        centerline.clear();
        for (int i = 0; i < n; i++) {
            int prevI = (i - 1 + n) % n;
            Point2D.Double from = t2[prevI];
            Point2D.Double to = t1[i];
            if (i == 0) startIndex = straightSamples / 2;
            for (int s = 0; s < straightSamples; s++) {
                double frac = s / (double) straightSamples;
                centerline.add(new Point2D.Double(from.x + (to.x - from.x) * frac, from.y + (to.y - from.y) * frac));
            }
            for (int s = 0; s < arcSamples; s++) {
                double frac = s / (double) arcSamples;
                double ang = a1[i] + delta[i] * frac;
                centerline.add(new Point2D.Double(arcCenter[i].x + r * Math.cos(ang), arcCenter[i].y + r * Math.sin(ang)));
            }
        }

        int m = centerline.size();
        cumulativeLength = new double[m];
        cumulativeLength[0] = 0;
        for (int i = 1; i < m; i++) {
            cumulativeLength[i] = cumulativeLength[i - 1] + centerline.get(i - 1).distance(centerline.get(i));
        }
        totalLength = cumulativeLength[m - 1] + centerline.get(m - 1).distance(centerline.get(0));

        for (int i = 0; i < m; i++) {
            Point2D.Double p0 = centerline.get((i - 1 + m) % m);
            Point2D.Double p1 = centerline.get((i + 1) % m);
            double tx = p1.x - p0.x, ty = p1.y - p0.y;
            double len = Math.hypot(tx, ty);
            if (len < 1e-6) len = 1;
            normals.add(new Point2D.Double(-ty / len, tx / len));
        }

        Point2D.Double p = centerline.get(0);
        Point2D.Double nrm = normals.get(0);
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

    double progress(double x, double y) {
        return cumulativeLength[nearestIndex(x, y)];
    }

    double distanceFromCenterline(double x, double y) {
        int i = nearestIndex(x, y);
        return centerline.get(i).distance(x, y);
    }

    Point2D.Double pointAtOffset(int index, double offset) {
        int n = centerline.size();
        int i = ((index % n) + n) % n;
        Point2D.Double p = centerline.get(i);
        Point2D.Double nrm = normals.get(i);
        return new Point2D.Double(p.x + nrm.x * offset, p.y + nrm.y * offset);
    }

    Point2D.Double inwardPoint(int index, double depth) {
        return pointAtOffset(index, innerSign * (halfWidth + depth));
    }

    Point2D.Double startPosition(double laneOffset) {
        return pointAtOffset(startIndex, laneOffset);
    }

    double startAngle() {
        Point2D.Double a = centerline.get(startIndex);
        Point2D.Double b = centerline.get(startIndex + 6);
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
        g.setColor(new Color(214, 178, 122));
        g.fill(road);

        // sandy cracked-earth seams across the road
        g.setColor(new Color(178, 142, 92, 110));
        for (int i = 0; i < centerline.size(); i += 7) {
            Point2D.Double o = pointAtOffset(i, halfWidth * 0.9);
            Point2D.Double in = pointAtOffset(i, -halfWidth * 0.9);
            g.draw(new Line2D.Double(o, in));
        }

        g.setColor(new Color(128, 96, 60));
        g.setStroke(new BasicStroke(4));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(250, 232, 190, 160));
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
    enum Kind { ROCK, BARREL_CACTUS, TUMBLEWEED }

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

    private void shadow(Graphics2D g) {
        g.setColor(new Color(60, 40, 20, 70));
        g.fillOval((int) (x - radius * 0.8 + 16), (int) (y - radius * 0.4 + 7), (int) (radius * 1.8), (int) (radius * 0.9));
    }

    void draw(Graphics2D g) {
        shadow(g);
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(rotation);
        switch (kind) {
            case ROCK:
                g.setColor(new Color(140, 108, 82));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(96, 72, 52));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.fillOval((int) (-radius * 0.4), (int) (-radius * 0.3), (int) (radius), (int) (radius * 0.8));
                break;
            case BARREL_CACTUS:
                g.setColor(new Color(70, 118, 62));
                g.fillRoundRect((int) (-radius * 0.7), (int) -radius, (int) (radius * 1.4), (int) (radius * 2), 10, 10);
                g.setColor(new Color(46, 86, 42));
                for (int i = -2; i <= 2; i++) {
                    g.drawLine((int) (-radius * 0.6), (int) (i * radius * 0.35), (int) (radius * 0.6), (int) (i * radius * 0.35));
                }
                g.setColor(new Color(220, 150, 60));
                g.fillOval((int) (-radius * 0.1), (int) (-radius * 1.1), 6, 6);
                break;
            case TUMBLEWEED:
                g.setColor(new Color(150, 120, 70));
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI / 4;
                    g.drawLine(0, 0, (int) (Math.cos(ang) * radius), (int) (Math.sin(ang) * radius));
                }
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                break;
        }
        g.setTransform(old);
    }
}

/** Purely decorative desert dressing lining the route: no collision. */
class Scenery {
    enum Kind { SAGUARO, SCRUB, ROCK_FORMATION }
    final Kind kind;
    final double x, y, scale;

    Scenery(Kind kind, double x, double y, double scale) {
        this.kind = kind; this.x = x; this.y = y; this.scale = scale;
    }

    void draw(Graphics2D g) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.scale(scale, scale);

        // long golden-hour shadow, cast away from the low sun
        g.setColor(new Color(90, 60, 30, 90));
        switch (kind) {
            case SAGUARO:
                g.fillOval(6, 2, 60, 14);
                break;
            case SCRUB:
                g.fillOval(4, 4, 30, 8);
                break;
            case ROCK_FORMATION:
                g.fillOval(8, 4, 42, 10);
                break;
        }

        switch (kind) {
            case SAGUARO:
                g.setColor(new Color(52, 92, 48));
                g.fillRoundRect(-6, -78, 12, 78, 6, 6);
                g.fillRoundRect(-20, -58, 14, 8, 4, 4);
                g.fillRoundRect(-20, -66, 8, 24, 4, 4);
                g.fillRoundRect(6, -46, 14, 8, 4, 4);
                g.fillRoundRect(12, -54, 8, 20, 4, 4);
                g.setColor(new Color(34, 66, 32));
                g.drawRoundRect(-6, -78, 12, 78, 6, 6);
                break;
            case SCRUB:
                g.setColor(new Color(96, 100, 54));
                for (int i = 0; i < 7; i++) {
                    double ang = -Math.PI / 2 + (i - 3) * 0.35;
                    g.drawLine(0, 0, (int) (Math.cos(ang) * 16), (int) (Math.sin(ang) * 16));
                }
                g.fillOval(-9, -9, 18, 12);
                break;
            case ROCK_FORMATION:
                g.setColor(new Color(150, 112, 82));
                g.fillPolygon(new int[]{-18, -4, 14, 20, -14}, new int[]{6, -26, -14, 6, 6}, 5);
                g.setColor(new Color(108, 78, 56));
                g.drawPolygon(new int[]{-18, -4, 14, 20, -14}, new int[]{6, -26, -14, 6, 6}, 5);
                break;
        }
        g.setTransform(old);
    }
}

/**
 * Signature hazard: retractable cactus spike traps embedded in the track
 * surface. Each trap cycles HIDDEN -> RISING (telegraph) -> RAISED (hazard)
 * -> FALLING -> HIDDEN. Driving over a RAISED trap punctures a tire — a
 * sustained handling/top-speed debuff on the Car, distinct from spinOut.
 */
class CactusSpikeTrap {
    enum State { HIDDEN, RISING, RAISED, FALLING }
    static final double HIDDEN_DUR = 3.6, RISING_DUR = 0.6, RAISED_DUR = 2.0, FALLING_DUR = 0.5;

    final double x, y, radius;
    State state = State.HIDDEN;
    double timer;

    CactusSpikeTrap(double x, double y, double radius, double phaseOffset) {
        this.x = x; this.y = y; this.radius = radius;
        this.timer = phaseOffset % HIDDEN_DUR;
    }

    void update(double dt) {
        timer += dt;
        switch (state) {
            case HIDDEN:
                if (timer >= HIDDEN_DUR) { state = State.RISING; timer = 0; }
                break;
            case RISING:
                if (timer >= RISING_DUR) { state = State.RAISED; timer = 0; }
                break;
            case RAISED:
                if (timer >= RAISED_DUR) { state = State.FALLING; timer = 0; }
                break;
            case FALLING:
                if (timer >= FALLING_DUR) { state = State.HIDDEN; timer = 0; }
                break;
        }
    }

    double raiseFraction() {
        switch (state) {
            case RISING: return Util.clamp(timer / RISING_DUR, 0, 1);
            case RAISED: return 1.0;
            case FALLING: return Util.clamp(1 - timer / FALLING_DUR, 0, 1);
            default: return 0.0;
        }
    }

    boolean isHazardous() { return state == State.RAISED; }
    boolean isTelegraphing() { return state == State.RISING; }

    void draw(Graphics2D g) {
        double f = raiseFraction();

        g.setColor(new Color(50, 30, 10, 60));
        g.fillOval((int) (x - radius * 0.8 + 12), (int) (y - radius * 0.35 + 6), (int) (radius * 1.7), (int) (radius * 0.8));

        g.setColor(new Color(158, 118, 74));
        g.fillOval((int) (x - radius), (int) (y - radius * 0.5), (int) (radius * 2), (int) radius);
        g.setColor(new Color(112, 82, 50));
        g.drawOval((int) (x - radius), (int) (y - radius * 0.5), (int) (radius * 2), (int) radius);

        if (f > 0.01) {
            Color spineColor = state == State.RAISED ? new Color(58, 108, 52) : new Color(74, 124, 66);
            int spikes = 6;
            for (int i = 0; i < spikes; i++) {
                double ang = (Math.PI * 2 * i) / spikes;
                double baseX = x + Math.cos(ang) * radius * 0.5;
                double baseY = y + Math.sin(ang) * radius * 0.32;
                double h = radius * 1.35 * f;
                double tipX = x + Math.cos(ang) * radius * 0.15;
                double tipY = y + Math.sin(ang) * radius * 0.1 - h;
                Path2D.Double spike = new Path2D.Double();
                spike.moveTo(baseX - 3, baseY);
                spike.lineTo(tipX, tipY);
                spike.lineTo(baseX + 3, baseY);
                spike.closePath();
                g.setColor(spineColor);
                g.fill(spike);
                g.setColor(new Color(26, 54, 26));
                g.draw(spike);
            }
            if (state == State.RAISED) {
                g.setStroke(new BasicStroke(2));
                g.setColor(new Color(224, 64, 40, 130));
                g.drawOval((int) (x - radius * 1.15), (int) (y - radius * 0.6), (int) (radius * 2.3), (int) (radius * 1.2));
                g.setStroke(new BasicStroke(1));
            } else if (state == State.RISING) {
                g.setColor(new Color(230, 200, 60, 140));
                g.drawOval((int) (x - radius * 1.05), (int) (y - radius * 0.55), (int) (radius * 2.1), (int) (radius * 1.1));
            }
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
    double punctureTimer = 0;

    final Color bodyColor;
    final Color trimColor;
    final String label;
    final ParticleSystem particles;

    private static final double MAX_SPEED = 420;
    private static final double MAX_REVERSE = -150;
    private static final double ACCEL = 300;
    private static final double BRAKE = 560;
    private static final double NATURAL_FRICTION = 150;
    private static final double OFFROAD_MULT = 2.3;
    private static final double TURN_RATE = 3.0;
    private static final double GRIP = 7.5;
    private static final double BOOST_MULT = 1.5;
    private static final double RADIUS = 17;
    private static final double PUNCTURE_SPEED_MULT = 0.55;
    private static final double PUNCTURE_GRIP_PENALTY = 3.2;
    private static final double PUNCTURE_DURATION = 5.0;

    private double dustTimer = 0;
    private double leakTimer = 0;

    Car(double x, double y, double angle, Color bodyColor, Color trimColor, String label, ParticleSystem particles) {
        this.x = x; this.y = y; this.angle = angle;
        this.bodyColor = bodyColor; this.trimColor = trimColor;
        this.label = label;
        this.particles = particles;
    }

    double radius() { return RADIUS; }

    boolean punctured() { return punctureTimer > 0; }

    void puncture() {
        if (punctureTimer <= 0) {
            for (int i = 0; i < 14; i++) {
                double a = Math.random() * Math.PI * 2;
                double sp = 40 + Math.random() * 90;
                particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp,
                        0.4 + Math.random() * 0.4, 3 + Math.random() * 3, 3,
                        new Color(90, 130, 70), Particle.ParticleKind.DEBRIS));
            }
        }
        punctureTimer = PUNCTURE_DURATION;
    }

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

        if (punctureTimer > 0) punctureTimer = Math.max(0, punctureTimer - dt);

        double dvx = Math.cos(angle) * speed;
        double dvy = Math.sin(angle) * speed;
        double gripPenalty = punctureTimer > 0 ? PUNCTURE_GRIP_PENALTY : 0;
        double gripNow = Math.max(1.2, GRIP - Util.clamp(Math.abs(speed) / MAX_SPEED, 0, 1) * 3.0 - gripPenalty);
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
        emitTireLeak(dt);
    }

    private void applyControls(double dt, InputState in, Track track) {
        boolean onTrack = track.distanceFromCenterline(x, y) < track.halfWidth;
        double frictionMult = onTrack ? 1.0 : OFFROAD_MULT;
        boosting = in.boost && boostFuel > 5 && in.throttle > 0;
        double punctureMult = punctureTimer > 0 ? PUNCTURE_SPEED_MULT : 1.0;
        double topSpeed = MAX_SPEED * (boosting ? BOOST_MULT : 1.0) * (onTrack ? 1.0 : 0.55) * punctureMult;

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
        double handling = punctureTimer > 0 ? 0.6 : 1.0;
        angle += in.steer * TURN_RATE * dt * speedFactor * dir * handling;
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
        Color c = onTrack ? new Color(210, 190, 150, 120) : new Color(180, 150, 100, 160);
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

    private void emitTireLeak(double dt) {
        if (punctureTimer <= 0) return;
        leakTimer -= dt;
        if (leakTimer > 0) return;
        leakTimer = 0.12;
        particles.add(new Particle(x - Math.cos(angle) * 10, y - Math.sin(angle) * 10,
                (Math.random() - 0.5) * 10, (Math.random() - 0.5) * 10,
                0.3, 3, 2, new Color(150, 120, 80, 150), Particle.ParticleKind.DUST));
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
                    Math.random() < 0.5 ? new Color(255, 120, 30) : new Color(120, 100, 80), Particle.ParticleKind.SMOKE));
        }
    }

    private void respawn(Track track) {
        alive = true;
        health = 60;
        speed = 0; vx = 0; vy = 0;
        punctureTimer = 0;
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

        g.setColor(new Color(70, 45, 20, 90));
        g.fillOval((int) x - 14 + 12, (int) y - 6 + 6, 30, 16);

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

        if (punctured()) {
            g.setColor(new Color(220, 60, 40));
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.drawString("FLAT", (int) x - 14, (int) y - 30);
        }
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

    InputState think(double dt, List<Obstacle> obstacles, List<CactusSpikeTrap> traps) {
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

        for (CactusSpikeTrap t : traps) {
            if (!t.isHazardous() && !t.isTelegraphing()) continue;
            double dx = t.x - car.x, dy = t.y - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < 85) {
                double angToObs = Util.normalizeAngle(Math.atan2(dy, dx) - car.angle);
                if (Math.abs(angToObs) < 0.9) {
                    diff += angToObs < 0 ? 0.5 : -0.5;
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
    private final List<Scenery> scenery = new ArrayList<>();
    private final List<CactusSpikeTrap> traps = new ArrayList<>();
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
        Point2D.Double v0 = new Point2D.Double(640, 150);
        Point2D.Double v1 = new Point2D.Double(1080, 600);
        Point2D.Double v2 = new Point2D.Double(200, 600);
        track = new Track(v0, v1, v2, 130, 100);
        int n = track.centerline.size();

        obstacles.clear();
        Random r = new Random(42);
        for (int i = 0; i < n; i += 13) {
            if (r.nextDouble() < 0.5) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 20 + r.nextDouble() * 55;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.ROCK : Obstacle.Kind.BARREL_CACTUS;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(10)));
            }
        }
        for (int i = 0; i < 3; i++) {
            Obstacle tw = new Obstacle(Obstacle.Kind.TUMBLEWEED, r.nextInt(W), r.nextInt(H), 13);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 40 + r.nextDouble() * 40;
            tw.vx = Math.cos(a) * sp;
            tw.vy = Math.sin(a) * sp;
            obstacles.add(tw);
        }

        // Decorative saguaro/scrub/rock dressing lining the outer edge of the route.
        scenery.clear();
        Random sr = new Random(99);
        for (int i = 0; i < n; i += 14) {
            double off = -track.innerSign * (track.halfWidth + 45 + sr.nextInt(30));
            Point2D.Double p = track.pointAtOffset(i, off);
            double roll = sr.nextDouble();
            Scenery.Kind k = roll < 0.4 ? Scenery.Kind.SAGUARO : roll < 0.75 ? Scenery.Kind.SCRUB : Scenery.Kind.ROCK_FORMATION;
            double scale = 0.7 + sr.nextDouble() * 0.6;
            scenery.add(new Scenery(k, p.x, p.y, scale));
        }
        // A little scrub scattered on the infield too.
        for (int i = 0; i < n; i += 23) {
            double off = track.innerSign * (track.halfWidth + 30 + sr.nextInt(40));
            Point2D.Double p = track.pointAtOffset(i, off);
            scenery.add(new Scenery(Scenery.Kind.SCRUB, p.x, p.y, 0.5 + sr.nextDouble() * 0.4));
        }

        // Signature hazard: retractable cactus spike traps at fixed spots on the racing line.
        traps.clear();
        Random tr = new Random(7);
        int trapCount = 6;
        for (int k = 0; k < trapCount; k++) {
            int idx = (n * k) / trapCount + n / 12;
            double lane = (tr.nextDouble() - 0.5) * track.halfWidth * 0.8;
            Point2D.Double p = track.pointAtOffset(idx, lane);
            double phase = k * 1.7;
            traps.add(new CactusSpikeTrap(p.x, p.y, 20, phase));
        }
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-22);
        Point2D.Double p2pos = track.startPosition(22);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(200, 90, 40), new Color(230, 200, 150), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(70, 110, 130), new Color(210, 220, 210), vsAI ? "AI" : "2", particles);
        p1.lastArcLen = track.progress(p1.x, p1.y);
        p2.lastArcLen = track.progress(p2.x, p2.y);
        cars.add(p1);
        cars.add(p2);
        for (CactusSpikeTrap t : traps) { t.state = CactusSpikeTrap.State.HIDDEN; t.timer = 0; }
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
        for (CactusSpikeTrap t : traps) t.update(dt);

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
            in2 = aiDriver.think(dt, obstacles, traps);
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
        for (CactusSpikeTrap t : traps) {
            resolveTrapCollision(p1, t);
            resolveTrapCollision(p2, t);
        }

        Point2D.Double startPt = track.startPosition(0);
        if (p1.justLapped) particles.spawnBurst(startPt.x, startPt.y);
        if (p2.justLapped) particles.spawnBurst(startPt.x, startPt.y);

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

    /** Signature hazard collision: driving over a RAISED trap punctures a tire (sustained debuff, not a spin). */
    private void resolveTrapCollision(Car c, CactusSpikeTrap t) {
        if (!c.alive || !t.isHazardous()) return;
        double dx = c.x - t.x, dy = c.y - t.y;
        double dist = Math.hypot(dx, dy);
        if (dist < c.radius() + t.radius * 0.6) {
            c.puncture();
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D sg = sceneBuffer.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBackground(sg);
        for (Scenery s : scenery) s.draw(sg);
        track.draw(sg);
        for (CactusSpikeTrap t : traps) t.draw(sg);
        for (Obstacle o : obstacles) o.draw(sg);
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
        g.setPaint(new GradientPaint(0, 0, new Color(220, 140, 80), 0, (float) (H * 0.42), new Color(236, 198, 150)));
        g.fillRect(0, 0, W, (int) (H * 0.42));

        double pulse = 1 + 0.03 * Math.sin(timeAccum * 1.2);
        int sunR = (int) (52 * pulse);
        g.setPaint(new RadialGradientPaint(new Point(190, 130), sunR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(255, 235, 190, 220), new Color(240, 170, 100, 100), new Color(240, 170, 100, 0)}));
        g.fillOval(190 - sunR * 3, 130 - sunR * 3, sunR * 6, sunR * 6);
        g.setColor(new Color(255, 244, 220));
        g.fillOval(190 - sunR / 2, 130 - sunR / 2, sunR, sunR);

        // Low desert-hill silhouette on the horizon (green-brown).
        g.setColor(new Color(110, 104, 70));
        Path2D.Double hills = new Path2D.Double();
        hills.moveTo(-50, H * 0.42);
        double wx = -50;
        Random wr = new Random(7);
        while (wx < W + 50) {
            double wy = H * 0.42 - (18 + wr.nextInt(36));
            hills.lineTo(wx, wy);
            wx += 40 + wr.nextInt(50);
        }
        hills.lineTo(W + 50, H * 0.42);
        hills.closePath();
        g.fill(hills);

        g.setPaint(new GradientPaint(0, (float) (H * 0.4), new Color(198, 168, 118), 0, H, new Color(140, 118, 78)));
        g.fillRect(0, (int) (H * 0.4), W, (int) (H * 0.6));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(224, 150, 60));
        g.setFont(new Font("SansSerif", Font.BOLD, 52));
        centerText(g, "CACTUS GULCH GRAND PRIX", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Golden-hour desert tri-oval — watch for rising spike traps", H / 2 - 90);

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
        g.setColor(c.punctured() ? new Color(220, 70, 40) : new Color(80, 160, 255));
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
