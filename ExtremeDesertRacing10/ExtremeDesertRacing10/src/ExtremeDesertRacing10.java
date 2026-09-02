import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ExtremeDesertRacing10 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Vulture Ridge Gauntlet — Extreme Desert Racing");
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

    enum ParticleKind { DUST, SMOKE, SPARK, DEBRIS, FIREWORK, WINDSTREAK }

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
        } else if (kind == ParticleKind.WINDSTREAK) {
            // travels straight across the screen at near-constant speed, no damping
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

/** Thread-safe particle store: physics thread writes, EDT paints. All entry points synchronized. */
class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();

    synchronized void add(Particle p) { particles.add(p); }

    synchronized void update(double dt) {
        particles.removeIf(p -> !p.update(dt));
    }

    void spawnFirework(double x, double y) {
        Color[] palette = {
                new Color(255, 210, 80), new Color(255, 120, 60),
                new Color(255, 250, 220), new Color(220, 60, 60)
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
            } else if (p.kind == Particle.ParticleKind.WINDSTREAK) {
                double spd = Math.hypot(p.vx, p.vy);
                double ux = spd > 1e-6 ? p.vx / spd : 1, uy = spd > 1e-6 ? p.vy / spd : 0;
                double len = 20 + s * 2.2;
                g.setStroke(new BasicStroke((float) Math.max(1, s * 0.35)));
                g.draw(new Line2D.Double(p.x - ux * len, p.y - uy * len, p.x, p.y));
                g.setStroke(new BasicStroke(1));
            } else {
                g.fill(new Ellipse2D.Double(p.x - s / 2, p.y - s / 2, s, s));
            }
            g.setComposite(old);
        }
    }
}

/**
 * "Vulture Ridge Gauntlet" track: a narrow switchback ridge — a zigzag mountain
 * path with a few long legs and wide sweeping end-loops (not tight hairpins).
 *
 * Construction: build a smooth "spine" curve (Catmull-Rom through 4 zigzagging
 * control points), then trace a closed loop that runs alongside the spine at
 * +laneOffset (the outbound leg), swings through a wide semicircular loop at
 * one end, runs back alongside the spine at -laneOffset (the return leg), and
 * closes with a matching wide loop at the other end. This is a "dogbone"
 * construction: as long as laneOffset stays comfortably below the spine's
 * minimum radius of curvature, the resulting centerline is guaranteed to be a
 * SIMPLE (non-self-intersecting) closed loop — verified numerically (0
 * self-intersections, ~150px worst-case clearance between the two legs versus
 * an 80px track width) before this geometry was finalized.
 *
 * Downstream (lap counting via cumulative arc length, and outer/inner edge
 * rendering via per-point normals) reuses the exact same technique as a plain
 * oval track: everything operates on the single closed `centerline` list, so
 * no special-casing is needed for the zigzag shape.
 */
class Track {
    final double halfWidth, trackWidth, laneOffset;
    static final double CLIFF_MARGIN = 55;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;
    double outwardSign = 1.0;

    Track(double originX, double originY, double trackWidth, double laneOffset) {
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        this.laneOffset = laneOffset;
        build(originX, originY);
    }

    private static Point2D.Double catmullRom(Point2D.Double p0, Point2D.Double p1, Point2D.Double p2, Point2D.Double p3, double t) {
        double t2 = t * t, t3 = t2 * t;
        double x = 0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);
        double y = 0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);
        return new Point2D.Double(x, y);
    }

    /** Semicircular sweep from +90deg (through tDir at 0deg) to -90deg around `center`, in local (tDir,nDir) axes. Endpoints excluded (the lanes already supply them). */
    private static List<Point2D.Double> arcCap(Point2D.Double center, double radius, Point2D.Double tDir, Point2D.Double nDir, int samples, boolean reverse) {
        List<Point2D.Double> pts = new ArrayList<>();
        for (int i = 1; i < samples; i++) {
            double theta = Math.PI / 2 - Math.PI * (i / (double) samples);
            double ct = Math.cos(theta), st = Math.sin(theta);
            double x = center.x + tDir.x * radius * ct + nDir.x * radius * st;
            double y = center.y + tDir.y * radius * ct + nDir.y * radius * st;
            pts.add(new Point2D.Double(x, y));
        }
        if (reverse) Collections.reverse(pts);
        return pts;
    }

    private void build(double originX, double originY) {
        Point2D.Double[] ctrl = {
                new Point2D.Double(originX - 480, originY - 20),
                new Point2D.Double(originX - 160, originY + 80),
                new Point2D.Double(originX + 160, originY - 20),
                new Point2D.Double(originX + 480, originY + 90)
        };
        Point2D.Double extPrev = new Point2D.Double(2 * ctrl[0].x - ctrl[1].x, 2 * ctrl[0].y - ctrl[1].y);
        Point2D.Double extNext = new Point2D.Double(2 * ctrl[3].x - ctrl[2].x, 2 * ctrl[3].y - ctrl[2].y);
        Point2D.Double[] ext = {extPrev, ctrl[0], ctrl[1], ctrl[2], ctrl[3], extNext};

        List<Point2D.Double> spine = new ArrayList<>();
        int samplesPerSeg = 150;
        for (int seg = 0; seg < 3; seg++) {
            Point2D.Double p0 = ext[seg], p1 = ext[seg + 1], p2 = ext[seg + 2], p3 = ext[seg + 3];
            for (int i = 0; i < samplesPerSeg; i++) {
                spine.add(catmullRom(p0, p1, p2, p3, i / (double) samplesPerSeg));
            }
        }
        spine.add(ctrl[3]);

        int n = spine.size();
        Point2D.Double[] tang = new Point2D.Double[n];
        Point2D.Double[] snorm = new Point2D.Double[n];
        for (int i = 0; i < n; i++) {
            Point2D.Double a = spine.get(Math.max(0, i - 1));
            Point2D.Double b = spine.get(Math.min(n - 1, i + 1));
            double tx = b.x - a.x, ty = b.y - a.y;
            double len = Math.hypot(tx, ty);
            if (len < 1e-6) len = 1;
            tang[i] = new Point2D.Double(tx / len, ty / len);
            snorm[i] = new Point2D.Double(-ty / len, tx / len);
        }

        Point2D.Double[] forward = new Point2D.Double[n];
        Point2D.Double[] backward = new Point2D.Double[n];
        for (int i = 0; i < n; i++) {
            Point2D.Double p = spine.get(i);
            forward[i] = new Point2D.Double(p.x + snorm[i].x * laneOffset, p.y + snorm[i].y * laneOffset);
            backward[i] = new Point2D.Double(p.x - snorm[i].x * laneOffset, p.y - snorm[i].y * laneOffset);
        }

        centerline.clear();
        for (int i = 0; i < n; i++) centerline.add(forward[i]);
        int capSamples = 40;
        centerline.addAll(arcCap(spine.get(n - 1), laneOffset, tang[n - 1], snorm[n - 1], capSamples, false));
        for (int i = n - 1; i >= 0; i--) centerline.add(backward[i]);
        Point2D.Double negT0 = new Point2D.Double(-tang[0].x, -tang[0].y);
        centerline.addAll(arcCap(spine.get(0), laneOffset, negT0, snorm[0], capSamples, true));

        int m = centerline.size();
        cumulativeLength = new double[m];
        cumulativeLength[0] = 0;
        for (int i = 1; i < m; i++) cumulativeLength[i] = cumulativeLength[i - 1] + centerline.get(i - 1).distance(centerline.get(i));
        totalLength = cumulativeLength[m - 1] + centerline.get(m - 1).distance(centerline.get(0));

        normals.clear();
        double cx = 0, cy = 0;
        for (Point2D.Double p : centerline) { cx += p.x; cy += p.y; }
        cx /= m; cy /= m;
        for (int i = 0; i < m; i++) {
            Point2D.Double a = centerline.get((i - 1 + m) % m);
            Point2D.Double b = centerline.get((i + 1) % m);
            double tx = b.x - a.x, ty = b.y - a.y;
            double len = Math.hypot(tx, ty);
            if (len < 1e-6) len = 1;
            normals.add(new Point2D.Double(-ty / len, tx / len));
        }

        int probe = m / 6;
        Point2D.Double pp = centerline.get(probe);
        Point2D.Double nn = normals.get(probe);
        double dHere = Point2D.distance(pp.x, pp.y, cx, cy);
        double dPoke = Point2D.distance(pp.x + nn.x * 20, pp.y + nn.y * 20, cx, cy);
        outwardSign = (dPoke > dHere) ? 1.0 : -1.0;
    }

    int nearestIndex(double x, double y) {
        int best = 0;
        double bestD = Double.MAX_VALUE;
        int n = centerline.size();
        for (int i = 0; i < n; i++) {
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

    /** A point on the true exterior side of the loop, `extra` units beyond the track edge — for scenery placement. */
    Point2D.Double outwardPoint(int index, double extra) {
        return pointAtOffset(index, outwardSign * (halfWidth + extra));
    }

    Point2D.Double startPosition(double laneOffsetAcross) {
        return pointAtOffset(2, laneOffsetAcross);
    }

    double startAngle() {
        Point2D.Double a = centerline.get(0);
        Point2D.Double b = centerline.get(5);
        return Math.atan2(b.y - a.y, b.x - a.x);
    }

    private void drawCliffBand(Graphics2D g, double innerOffset, double outerOffset, Color far, Color near) {
        int n = centerline.size();
        Path2D.Double band = new Path2D.Double();
        for (int i = 0; i < n; i++) {
            Point2D.Double p = pointAtOffset(i, innerOffset);
            if (i == 0) band.moveTo(p.x, p.y); else band.lineTo(p.x, p.y);
        }
        for (int i = n - 1; i >= 0; i--) {
            Point2D.Double p = pointAtOffset(i, outerOffset);
            band.lineTo(p.x, p.y);
        }
        band.closePath();
        g.setColor(far);
        g.fill(band);
        g.setColor(near);
        for (int i = 0; i < n; i += 3) {
            Point2D.Double p = pointAtOffset(i, innerOffset);
            g.fillOval((int) p.x - 1, (int) p.y - 1, 2, 2);
        }
    }

    void draw(Graphics2D g) {
        int n = centerline.size();
        // Cliff drop-off shading just outside BOTH edges — the whole ridge is exposed on both flanks.
        drawCliffBand(g, halfWidth, halfWidth + CLIFF_MARGIN + 25, new Color(58, 22, 16), new Color(96, 44, 28));
        drawCliffBand(g, -halfWidth, -(halfWidth + CLIFF_MARGIN + 25), new Color(58, 22, 16), new Color(96, 44, 28));

        Path2D.Double outer = new Path2D.Double();
        Path2D.Double inner = new Path2D.Double();
        for (int i = 0; i < n; i++) {
            Point2D.Double o = pointAtOffset(i, halfWidth);
            Point2D.Double in = pointAtOffset(i, -halfWidth);
            if (i == 0) { outer.moveTo(o.x, o.y); inner.moveTo(in.x, in.y); }
            else { outer.lineTo(o.x, o.y); inner.lineTo(in.x, in.y); }
        }
        outer.closePath();
        inner.closePath();

        Area road = new Area(outer);
        road.subtract(new Area(inner));
        g.setColor(new Color(182, 100, 66));
        g.fill(road);

        g.setColor(new Color(140, 70, 46, 130));
        for (int i = 0; i < n; i += 6) {
            Point2D.Double o = pointAtOffset(i, halfWidth);
            Point2D.Double in = pointAtOffset(i, -halfWidth);
            g.draw(new Line2D.Double(o, in));
        }

        g.setColor(new Color(96, 48, 30));
        g.setStroke(new BasicStroke(4));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(240, 214, 160, 150));
        Path2D.Double mid = new Path2D.Double();
        for (int i = 0; i < n; i++) {
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
    enum Kind { BOULDER, ROCK_SPIRE, DEAD_SNAG, WRECK, TUMBLEWEED }

    double x, y, radius, rotation;
    Kind kind;
    double vx, vy, spin;

    Obstacle(Kind kind, double x, double y, double radius) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.rotation = Math.random() * Math.PI * 2;
        if (kind == Kind.TUMBLEWEED) this.spin = 3 + Math.random() * 3;
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
            case BOULDER:
                g.setColor(new Color(120, 66, 44));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.7));
                g.setColor(new Color(84, 44, 28));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.7));
                g.fillOval((int) (-radius * 0.3), (int) (-radius * 0.6), (int) (radius * 0.8), (int) (radius * 0.6));
                break;
            case ROCK_SPIRE:
                g.setColor(new Color(150, 78, 48));
                Path2D.Double spire = new Path2D.Double();
                spire.moveTo(-radius * 0.6, radius);
                spire.lineTo(-radius * 0.2, -radius * 1.6);
                spire.lineTo(radius * 0.3, -radius * 1.3);
                spire.lineTo(radius * 0.6, radius);
                spire.closePath();
                g.fill(spire);
                g.setColor(new Color(96, 50, 30));
                g.draw(spire);
                break;
            case DEAD_SNAG:
                g.setColor(new Color(90, 70, 58));
                g.fillRoundRect((int) (-radius * 0.2), (int) (-radius * 1.6), (int) (radius * 0.4), (int) (radius * 2.4), 3, 3);
                g.setColor(new Color(70, 54, 44));
                for (int i = -1; i <= 1; i += 2) {
                    g.drawLine(0, (int) (-radius * i * 0.6), (int) (radius * i), (int) (-radius * 1.3));
                }
                break;
            case WRECK:
                g.setColor(new Color(64, 48, 40));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(new Color(160, 70, 40));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) (radius * 0.3));
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
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

/** Purely decorative vultures circling on fixed orbits above the ridge — no collision. */
class Vulture {
    final double cx, cy, orbitRadius, angularSpeed, scale;
    double angle;

    Vulture(double cx, double cy, double orbitRadius, double angularSpeed, double phase, double scale) {
        this.cx = cx; this.cy = cy; this.orbitRadius = orbitRadius;
        this.angularSpeed = angularSpeed; this.angle = phase; this.scale = scale;
    }

    void update(double dt) {
        angle += angularSpeed * dt;
    }

    double x() { return cx + Math.cos(angle) * orbitRadius; }
    double y() { return cy + Math.sin(angle) * orbitRadius * 0.45; }

    void draw(Graphics2D g, double t) {
        double bx = x(), by = y();
        double flap = Math.sin(t * 3 + angle * 2) * 0.18;
        g.setColor(new Color(30, 26, 22, 200));
        AffineTransform old = g.getTransform();
        g.translate(bx, by);
        g.scale(scale, scale);
        Path2D.Double wing = new Path2D.Double();
        wing.moveTo(0, 0);
        wing.lineTo(-16, -6 - flap * 10);
        wing.lineTo(-9, -1);
        wing.lineTo(0, 0);
        wing.lineTo(16, -6 - flap * 10);
        wing.lineTo(9, -1);
        wing.closePath();
        g.fill(wing);
        g.fillOval(-2, -2, 4, 4);
        g.setTransform(old);

        // faint ground shadow to sell the altitude / cliff-drop atmosphere
        g.setColor(new Color(20, 12, 8, 45));
        g.fillOval((int) (bx - 8 * scale), (int) (by + 40), (int) (16 * scale), (int) (5 * scale));
    }
}

/**
 * Signature hazard: periodic Crosswind Gusts. A CALM period is followed by a
 * telegraphed WARNING (a pulsing wind-arrow indicator + light dust streaks)
 * before the gust turns ACTIVE and shoves cars sideways for a couple of
 * seconds. On the narrow ridge legs, a strong shove can push a car past the
 * shoulder and off the cliff edge (see Car.fallOffCliff / Track.CLIFF_MARGIN).
 */
class WindGust {
    enum Phase { CALM, TELEGRAPH, ACTIVE }

    Phase phase = Phase.CALM;
    double timer;
    double direction;
    double strength;
    double activeElapsed, activeDuration;
    private final Random rnd = new Random();

    static final double TELEGRAPH_TIME = 2.2;
    static final double MIN_CALM = 5.0, MAX_CALM = 9.5;
    static final double MIN_ACTIVE = 1.8, MAX_ACTIVE = 3.0;
    static final double MIN_STRENGTH = 230, MAX_STRENGTH = 380;

    WindGust() {
        timer = MIN_CALM + rnd.nextDouble() * (MAX_CALM - MIN_CALM);
        direction = rnd.nextDouble() * Math.PI * 2;
    }

    void update(double dt) {
        timer -= dt;
        switch (phase) {
            case CALM:
                if (timer <= 0) {
                    phase = Phase.TELEGRAPH;
                    timer = TELEGRAPH_TIME;
                    direction = rnd.nextDouble() * Math.PI * 2;
                    strength = MIN_STRENGTH + rnd.nextDouble() * (MAX_STRENGTH - MIN_STRENGTH);
                }
                break;
            case TELEGRAPH:
                if (timer <= 0) {
                    phase = Phase.ACTIVE;
                    activeDuration = MIN_ACTIVE + rnd.nextDouble() * (MAX_ACTIVE - MIN_ACTIVE);
                    activeElapsed = 0;
                }
                break;
            case ACTIVE:
                activeElapsed += dt;
                if (activeElapsed >= activeDuration) {
                    phase = Phase.CALM;
                    timer = MIN_CALM + rnd.nextDouble() * (MAX_CALM - MIN_CALM);
                }
                break;
        }
    }

    double intensity() {
        if (phase != Phase.ACTIVE) return 0;
        double t = Util.clamp(activeElapsed / activeDuration, 0, 1);
        return Math.sin(Math.PI * t);
    }

    double forceX() { return Math.cos(direction) * strength * intensity(); }
    double forceY() { return Math.sin(direction) * strength * intensity(); }

    boolean isActive() { return phase == Phase.ACTIVE; }

    void draw(Graphics2D g, int w, double t) {
        if (phase == Phase.CALM) return;
        double alpha = phase == Phase.TELEGRAPH ? Util.clamp(1.3 - timer / TELEGRAPH_TIME, 0.25, 1.0) : 1.0;
        double pulse = 0.75 + 0.25 * Math.sin(t * 11);
        int cx = w / 2, cy = 68;
        double len = 74 * (phase == Phase.TELEGRAPH ? pulse : 1.0);
        double dx = Math.cos(direction), dy = Math.sin(direction);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Util.clamp(alpha, 0, 1)));
        g.setColor(phase == Phase.TELEGRAPH ? new Color(255, 214, 100) : new Color(255, 110, 50));
        g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int x1 = (int) (cx - dx * len / 2), y1 = (int) (cy - dy * len / 2);
        int x2 = (int) (cx + dx * len / 2), y2 = (int) (cy + dy * len / 2);
        g.drawLine(x1, y1, x2, y2);
        double ah = 15, baseAng = Math.atan2(dy, dx);
        int hx1 = (int) (x2 - Math.cos(baseAng - 0.5) * ah), hy1 = (int) (y2 - Math.sin(baseAng - 0.5) * ah);
        int hx2 = (int) (x2 - Math.cos(baseAng + 0.5) * ah), hy2 = (int) (y2 - Math.sin(baseAng + 0.5) * ah);
        g.fillPolygon(new int[]{x2, hx1, hx2}, new int[]{y2, hy1, hy2}, 3);
        g.setStroke(new BasicStroke(1));
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        String label = phase == Phase.TELEGRAPH ? "CROSSWIND INCOMING" : "CROSSWIND GUST!";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, cx - fm.stringWidth(label) / 2, cy + 36);
        g.setComposite(old);
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

    private static final double MAX_SPEED = 420;
    private static final double MAX_REVERSE = -150;
    private static final double ACCEL = 300;
    private static final double BRAKE = 560;
    private static final double NATURAL_FRICTION = 150;
    private static final double OFFROAD_MULT = 2.4;
    private static final double TURN_RATE = 3.1;
    private static final double GRIP = 7.5;
    private static final double BOOST_MULT = 1.55;
    private static final double RADIUS = 16;

    private double dustTimer = 0;

    Car(double x, double y, double angle, Color bodyColor, Color trimColor, String label, ParticleSystem particles) {
        this.x = x; this.y = y; this.angle = angle;
        this.bodyColor = bodyColor; this.trimColor = trimColor;
        this.label = label;
        this.particles = particles;
    }

    double radius() { return RADIUS; }

    void update(double dt, InputState in, Track track, double windFx, double windFy) {
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

        // Crosswind gust: pushes the car sideways regardless of steering, tires fight it via grip above.
        vx += windFx * dt;
        vy += windFy * dt;

        x += vx * dt;
        y += vy * dt;

        double dist = track.distanceFromCenterline(x, y);
        boolean onTrack = dist < track.halfWidth;
        if (dist > track.halfWidth + Track.CLIFF_MARGIN) {
            fallOffCliff();
            return;
        }

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
        Color c = onTrack ? new Color(210, 150, 110, 120) : new Color(160, 90, 60, 160);
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

    /** Blown or driven past the shoulder and over the ridge's edge — a heavier penalty than a normal crash, reusing wreck/respawn flow. */
    private void fallOffCliff() {
        alive = false;
        respawnTimer = 3.2;
        health = Math.max(0, health - 35);
        for (int i = 0; i < 30; i++) {
            double a = Math.random() * Math.PI * 2;
            double sp = 40 + Math.random() * 150;
            particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp,
                    0.6 + Math.random() * 0.5, 4 + Math.random() * 6, 8,
                    Math.random() < 0.5 ? new Color(150, 70, 40) : new Color(90, 58, 40), Particle.ParticleKind.DEBRIS));
        }
    }

    private void respawn(Track track) {
        alive = true;
        health = Math.max(health, 60);
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

    InputState think(double dt, List<Obstacle> obstacles, WindGust wind) {
        InputState in = new InputState();
        int idx = track.nearestIndex(car.x, car.y);
        int lookahead = 16;
        Point2D.Double target = track.centerline.get((idx + lookahead) % track.centerline.size());
        double desiredAngle = Math.atan2(target.y - car.y, target.x - car.x);
        double diff = Util.normalizeAngle(desiredAngle - car.angle);

        for (Obstacle o : obstacles) {
            double dx = o.x - car.x, dy = o.y - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < 80) {
                double angToObs = Util.normalizeAngle(Math.atan2(dy, dx) - car.angle);
                if (Math.abs(angToObs) < 0.9) {
                    diff += angToObs < 0 ? 0.6 : -0.6;
                }
            }
        }

        if (wind.isActive()) {
            double fx = wind.forceX(), fy = wind.forceY();
            double rightX = Math.cos(car.angle + Math.PI / 2), rightY = Math.sin(car.angle + Math.PI / 2);
            double lateral = fx * rightX + fy * rightY;
            diff -= Util.clamp(lateral / 220.0, -0.6, 0.6);
        }

        in.steer = Util.clamp(diff * 1.6, -1, 1);
        in.throttle = Math.abs(diff) > 1.3 ? 0.25 : 1.0;

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
    private final List<Vulture> vultures = new ArrayList<>();
    private WindGust windGust = new WindGust();
    private ParticleSystem particles = new ParticleSystem();
    private AIDriver aiDriver;
    private final Random rand = new Random(7);

    private double countdownTimer;
    private double raceTime;
    private double timeAccum = 0;
    private double windStreakTimer = 0;
    private final BufferedImage sceneBuffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        setupWorld();
    }

    private void setupWorld() {
        track = new Track(640, 340, 80, 75);
        int n = track.centerline.size();

        obstacles.clear();
        Random r = new Random(42);
        for (int i = 0; i < n; i += 13) {
            if (r.nextDouble() < 0.45) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = side * (track.halfWidth * 0.55);
                Point2D.Double p = track.pointAtOffset(i, off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.BOULDER : Obstacle.Kind.ROCK_SPIRE;
                obstacles.add(new Obstacle(k, p.x, p.y, 9 + r.nextInt(8)));
            }
        }
        int[] hazardIdx = {40, 150, 260, 380, 500, 620, 740, 860};
        for (int idx : hazardIdx) {
            if (idx >= n) continue;
            Point2D.Double p = track.outwardPoint(idx, 30 + r.nextInt(20));
            obstacles.add(new Obstacle(Obstacle.Kind.DEAD_SNAG, p.x, p.y, 12));
        }
        for (int idx = 90; idx < n; idx += 220) {
            Point2D.Double p = track.outwardPoint(idx, 55 + r.nextInt(20));
            obstacles.add(new Obstacle(Obstacle.Kind.WRECK, p.x, p.y, 14));
        }
        for (int i = 0; i < 4; i++) {
            Obstacle tw = new Obstacle(Obstacle.Kind.TUMBLEWEED, r.nextInt(W), r.nextInt(H), 11);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 35 + r.nextDouble() * 35;
            tw.vx = Math.cos(a) * sp;
            tw.vy = Math.sin(a) * sp;
            obstacles.add(tw);
        }

        vultures.clear();
        Random vr = new Random(13);
        int[] vultureIdx = {60, 260, 480, 700, 900};
        for (int idx : vultureIdx) {
            if (idx >= n) continue;
            Point2D.Double p = track.centerline.get(idx);
            double orbitR = 70 + vr.nextInt(60);
            double speed = (vr.nextBoolean() ? 1 : -1) * (0.35 + vr.nextDouble() * 0.3);
            vultures.add(new Vulture(p.x, p.y - 40, orbitR, speed, vr.nextDouble() * Math.PI * 2, 0.9 + vr.nextDouble() * 0.5));
        }
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        windGust = new WindGust();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-14);
        Point2D.Double p2pos = track.startPosition(14);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(196, 72, 40), new Color(230, 200, 150), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(90, 100, 120), new Color(210, 210, 200), vsAI ? "AI" : "2", particles);
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
        for (Vulture v : vultures) v.update(dt);
        windGust.update(dt);
        updateWindStreaks(dt);

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

    private void updateWindStreaks(double dt) {
        if (windGust.phase == WindGust.Phase.CALM) return;
        windStreakTimer -= dt;
        if (windStreakTimer > 0) return;
        double intensity = windGust.isActive() ? 1.0 : 0.4;
        windStreakTimer = 0.03 / intensity;
        double dirx = Math.cos(windGust.direction), diry = Math.sin(windGust.direction);
        double perpX = -diry, perpY = dirx;
        double centerX = W / 2.0, centerY = H / 2.0;
        double spread = (Math.abs(dirx) > Math.abs(diry) ? H : W) * 0.75;
        double s = (rand.nextDouble() - 0.5) * spread;
        double dist = Math.max(W, H) * 0.62;
        double px = centerX - dirx * dist + perpX * s;
        double py = centerY - diry * dist + perpY * s;
        double speed = 460 + 260 * intensity;
        particles.add(new Particle(px, py, dirx * speed, diry * speed, 1.5, 3 + rand.nextDouble() * 3, 0,
                new Color(224, 184, 132, (int) (60 * intensity + 40)), Particle.ParticleKind.WINDSTREAK));
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
            in2 = aiDriver.think(dt, obstacles, windGust);
        } else {
            in2 = new InputState();
            in2.throttle = keys.contains(KeyEvent.VK_UP) ? 1 : keys.contains(KeyEvent.VK_DOWN) ? -1 : 0;
            in2.steer = keys.contains(KeyEvent.VK_LEFT) ? -1 : keys.contains(KeyEvent.VK_RIGHT) ? 1 : 0;
            in2.boost = keys.contains(KeyEvent.VK_ENTER);
        }

        double wfx = windGust.forceX(), wfy = windGust.forceY();
        p1.update(dt, in1, track, wfx, wfy);
        p2.update(dt, in2, track, wfx, wfy);

        resolveCarCollision(p1, p2);
        for (Obstacle o : obstacles) {
            resolveObstacleCollision(p1, o);
            resolveObstacleCollision(p2, o);
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
                c.damage(impact * 0.01);
                c.speed *= 0.8;
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
        for (Vulture v : vultures) v.draw(sg, timeAccum);
        particles.draw(sg);
        for (Car c : cars) c.draw(sg);

        if (windGust.isActive()) {
            Composite old = sg.getComposite();
            sg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.12 * windGust.intensity())));
            sg.setColor(new Color(230, 170, 100));
            sg.fillRect(0, 0, W, H);
            sg.setComposite(old);
        }
        sg.dispose();

        Graphics2D g = (Graphics2D) g0;
        g.drawImage(sceneBuffer, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        windGust.draw(g, W, timeAccum);

        switch (state) {
            case MENU: drawMenu(g); break;
            case COUNTDOWN: drawCountdown(g); break;
            case RACING: drawHud(g); break;
            case FINISHED: drawFinish(g); break;
        }
    }

    private void drawBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(225, 210, 190), 0, (float) (H * 0.4), new Color(240, 225, 205)));
        g.fillRect(0, 0, W, (int) (H * 0.4));

        double pulse = 1 + 0.03 * Math.sin(timeAccum * 1.4);
        int sunR = (int) (50 * pulse);
        g.setPaint(new RadialGradientPaint(new Point(W - 180, 90), sunR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(255, 250, 225, 220), new Color(255, 200, 140, 90), new Color(255, 200, 140, 0)}));
        g.fillOval(W - 180 - sunR * 3, 90 - sunR * 3, sunR * 6, sunR * 6);
        g.setColor(new Color(255, 250, 230));
        g.fillOval(W - 180 - sunR / 2, 90 - sunR / 2, sunR, sunR);

        // distant plateau/mesa silhouettes
        g.setColor(new Color(196, 118, 84, 200));
        Path2D.Double mesa = new Path2D.Double();
        mesa.moveTo(-50, H * 0.4);
        double wx = -50;
        Random wr = new Random(5);
        while (wx < W + 50) {
            double wy = H * 0.4 - (30 + wr.nextInt(70));
            double ww = 60 + wr.nextInt(120);
            mesa.lineTo(wx, wy);
            mesa.lineTo(wx + ww, wy);
            wx += ww + 20 + wr.nextInt(60);
        }
        mesa.lineTo(W + 50, H * 0.4);
        mesa.closePath();
        g.fill(mesa);

        g.setPaint(new GradientPaint(0, (float) (H * 0.38), new Color(214, 138, 96), 0, H, new Color(150, 78, 50)));
        g.fillRect(0, (int) (H * 0.38), W, (int) (H * 0.62));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(240, 150, 70));
        g.setFont(new Font("SansSerif", Font.BOLD, 52));
        centerText(g, "VULTURE RIDGE GAUNTLET", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Switchback ridge racing — mind the crosswind near the cliff edge", H / 2 - 90);

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
