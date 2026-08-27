import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * ExtremeDesertRacing5 — "Camel Trail Switchback"
 *
 * A dusty caravan-trail switchback climb: a mountain zigzag of angled legs
 * joined by two wide hairpin end-loops. Signature hazard is a wandering camel
 * herd that slowly threads back and forth across the road at two crossing
 * points — players must time their run to slip through the gaps.
 */
public class ExtremeDesertRacing5 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Camel Trail Switchback — Extreme Desert Racing");
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

    static double easeInOut(double t) {
        t = clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
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

/** Thread-safe particle pool: physics thread adds/updates, EDT draws — all synchronized. */
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
 * "Camel Trail Switchback" track: two long zigzag corridors (angled climbing
 * legs, corners rounded by a fixed-radius fillet so the offset road edges
 * never pinch) joined by wide semicircular hairpin end-loops. The whole
 * shape is one simple, non-self-intersecting closed loop — verified offline
 * (0 self-intersections; nearest path-far-apart approach distance ~127px vs
 * an 80px track width, and fillet radius 70px comfortably exceeds the 40px
 * half-width so no corner ever pinches).
 *
 * Lap counting and rendering reuse the standard technique: a centerline
 * point list with a parallel cumulative arc-length array (robust signed
 * progress with wraparound) and per-point normals for offset edges.
 */
class Track {
    final double halfWidth, trackWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;
    double innerSign = 1.0;
    double centerX, centerY;

    final double x0, x1, yTop, yBot, loopRadius, amplitude;

    Track(double x0, double x1, double yTop, double yBot, double loopRadius, double amplitude, double trackWidth) {
        this.x0 = x0; this.x1 = x1; this.yTop = yTop; this.yBot = yBot;
        this.loopRadius = loopRadius; this.amplitude = amplitude;
        this.trackWidth = trackWidth; this.halfWidth = trackWidth / 2;
        build();
    }

    /** Rounds every interior vertex of an open polyline with a circular arc of radius r (clamped to the leg lengths). */
    private static List<Point2D.Double> roundedPolyline(List<Point2D.Double> verts, double r, int arcSamples) {
        List<Point2D.Double> out = new ArrayList<>();
        int n = verts.size();
        out.add(verts.get(0));
        for (int i = 1; i < n - 1; i++) {
            Point2D.Double A = verts.get(i - 1), B = verts.get(i), C = verts.get(i + 1);
            double ux = A.x - B.x, uy = A.y - B.y;
            double ulen = Math.hypot(ux, uy); ux /= ulen; uy /= ulen;
            double vx = C.x - B.x, vy = C.y - B.y;
            double vlen = Math.hypot(vx, vy); vx /= vlen; vy /= vlen;
            double dot = Util.clamp(ux * vx + uy * vy, -1, 1);
            double theta = Math.acos(dot);
            if (theta > Math.PI - 1e-6 || theta < 1e-6) {
                out.add(B);
                continue;
            }
            double rr = Math.min(r, Math.min(ulen, vlen) * 0.45);
            double t = rr / Math.tan(theta / 2);
            double centerDist = rr / Math.sin(theta / 2);
            double bx = ux + vx, by = uy + vy;
            double blen = Math.hypot(bx, by);
            bx /= blen; by /= blen;
            double t1x = B.x + ux * t, t1y = B.y + uy * t;
            double t2x = B.x + vx * t, t2y = B.y + vy * t;
            double ox = B.x + bx * centerDist, oy = B.y + by * centerDist;
            double a1 = Math.atan2(t1y - oy, t1x - ox);
            double a2 = Math.atan2(t2y - oy, t2x - ox);
            double diff = a2 - a1;
            while (diff > Math.PI) diff -= 2 * Math.PI;
            while (diff < -Math.PI) diff += 2 * Math.PI;
            out.add(new Point2D.Double(t1x, t1y));
            for (int s = 1; s < arcSamples; s++) {
                double f = s / (double) arcSamples;
                double a = a1 + diff * f;
                out.add(new Point2D.Double(ox + rr * Math.cos(a), oy + rr * Math.sin(a)));
            }
            out.add(new Point2D.Double(t2x, t2y));
        }
        out.add(verts.get(n - 1));
        return out;
    }

    private static List<Point2D.Double> zigzagCorridor(double xStart, double xEnd, double yBase, double amplitude, double[] signs, double filletR, int arcSamples) {
        int legs = signs.length + 1;
        List<Point2D.Double> verts = new ArrayList<>();
        verts.add(new Point2D.Double(xStart, yBase));
        for (int i = 1; i < legs; i++) {
            double x = xStart + (xEnd - xStart) * i / (double) legs;
            double y = yBase + amplitude * signs[i - 1];
            verts.add(new Point2D.Double(x, y));
        }
        verts.add(new Point2D.Double(xEnd, yBase));
        return roundedPolyline(verts, filletR, arcSamples);
    }

    private void build() {
        double filletR = 70;
        int arcSamples = 24;
        int loopSamples = 150;
        double ymid = (yTop + yBot) / 2.0;

        double[] topSigns = {-1, 1, -1, 1, -1};
        double[] botSigns = {1, -1, 1, -1, 1};

        centerline.addAll(zigzagCorridor(x0, x1, yTop, amplitude, topSigns, filletR, arcSamples));

        for (int i = 1; i < loopSamples; i++) {
            double t = i / (double) loopSamples;
            double ang = -Math.PI / 2 + t * Math.PI;
            centerline.add(new Point2D.Double(x1 + loopRadius * Math.cos(ang), ymid + loopRadius * Math.sin(ang)));
        }

        List<Point2D.Double> bottom = zigzagCorridor(x1, x0, yBot, amplitude, botSigns, filletR, arcSamples);
        for (int i = 1; i < bottom.size(); i++) centerline.add(bottom.get(i));

        for (int i = 1; i < loopSamples; i++) {
            double t = i / (double) loopSamples;
            double ang = Math.PI / 2 + t * Math.PI;
            centerline.add(new Point2D.Double(x0 + loopRadius * Math.cos(ang), ymid + loopRadius * Math.sin(ang)));
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

        centerX = 0; centerY = 0;
        for (Point2D.Double p : centerline) { centerX += p.x; centerY += p.y; }
        centerX /= n; centerY /= n;

        int probe = 5;
        Point2D.Double p = centerline.get(probe);
        Point2D.Double nrm = normals.get(probe);
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
        Point2D.Double p = centerline.get(index);
        Point2D.Double n = normals.get(index);
        return new Point2D.Double(p.x + n.x * offset, p.y + n.y * offset);
    }

    /** A point `depth` units further outward than the outer track edge, at index (for roadside dressing). */
    Point2D.Double outwardPoint(int index, double depth) {
        return pointAtOffset(index, -innerSign * (halfWidth + depth));
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
        g.setColor(new Color(196, 160, 108));
        g.fill(road);

        // packed-dirt tread seams across the road
        g.setColor(new Color(150, 118, 76, 110));
        for (int i = 0; i < centerline.size(); i += 6) {
            Point2D.Double o = pointAtOffset(i, halfWidth);
            Point2D.Double in = pointAtOffset(i, -halfWidth);
            g.draw(new Line2D.Double(o, in));
        }

        g.setColor(new Color(120, 88, 52));
        g.setStroke(new BasicStroke(4));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(240, 214, 160, 150));
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
            g.setColor(i % 2 == 0 ? Color.WHITE : new Color(60, 40, 20));
            g.fill(new Rectangle2D.Double(sx - 4, sy - 4, 8, 8));
        }
    }
}

class Obstacle {
    enum Kind { ROCK, SCRUB, CRATE, TUMBLEWEED }

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
                g.setColor(new Color(120, 100, 78));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.7));
                g.setColor(new Color(84, 68, 50));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.7));
                g.setColor(new Color(150, 128, 100));
                g.fillOval((int) (-radius * 0.4), (int) (-radius * 0.5), (int) radius, (int) (radius * 0.8));
                break;
            case SCRUB:
                g.setColor(new Color(120, 96, 58));
                for (int i = 0; i < 7; i++) {
                    double ang = i * (Math.PI * 2 / 7);
                    g.drawLine(0, 0, (int) (Math.cos(ang) * radius), (int) (Math.sin(ang) * radius * 0.7));
                }
                g.setColor(new Color(150, 130, 70));
                g.fillOval((int) (-radius * 0.3), (int) (-radius * 0.3), (int) (radius * 0.6), (int) (radius * 0.5));
                break;
            case CRATE:
                g.setColor(new Color(140, 104, 58));
                g.fillRect((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                g.setColor(new Color(90, 62, 30));
                g.drawRect((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                g.drawLine((int) -radius, (int) -radius, (int) radius, (int) radius);
                g.drawLine((int) -radius, (int) radius, (int) radius, (int) -radius);
                break;
            case TUMBLEWEED:
                g.setColor(new Color(150, 120, 70));
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI / 4;
                    g.drawLine((int) (Math.cos(ang) * -radius * 0.4), (int) (Math.sin(ang) * -radius * 0.4),
                            (int) (Math.cos(ang) * radius), (int) (Math.sin(ang) * radius));
                }
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                break;
        }
        g.setTransform(old);
    }
}

/** Purely decorative desert dressing along the trail shoulders: no collision. */
class Scenery {
    enum Kind { BOULDER, DEAD_TREE, CACTUS }
    final Kind kind;
    final double x, y, scale, tilt;

    Scenery(Kind kind, double x, double y, double scale, double tilt) {
        this.kind = kind; this.x = x; this.y = y; this.scale = scale; this.tilt = tilt;
    }

    void draw(Graphics2D g) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(tilt);
        g.scale(scale, scale);
        switch (kind) {
            case BOULDER:
                g.setColor(new Color(128, 100, 72, 220));
                g.fillOval(-16, -14, 32, 26);
                g.setColor(new Color(96, 74, 52, 220));
                g.drawOval(-16, -14, 32, 26);
                break;
            case DEAD_TREE:
                g.setColor(new Color(80, 56, 36, 220));
                g.fillRect(-3, -34, 6, 34);
                g.drawLine(0, -34, -16, -50);
                g.drawLine(0, -26, 15, -44);
                g.drawLine(0, -18, -14, -32);
                break;
            case CACTUS:
                g.setColor(new Color(70, 96, 56, 220));
                g.fillRoundRect(-5, -40, 10, 40, 5, 5);
                g.fillRoundRect(-16, -28, 12, 8, 4, 4);
                g.fillRoundRect(6, -34, 12, 8, 4, 4);
                break;
        }
        g.setTransform(old);
    }
}

/** Distant camel-caravan silhouette drifting slowly along the horizon: purely decorative, no collision. */
class CaravanSilhouette {
    final double baseX, y, scale, speed;

    CaravanSilhouette(double baseX, double y, double scale, double speed) {
        this.baseX = baseX; this.y = y; this.scale = scale; this.speed = speed;
    }

    void draw(Graphics2D g, double t, int worldWidth) {
        double x = ((baseX + t * speed) % (worldWidth + 400)) - 200;
        g.setColor(new Color(90, 62, 40, 150));
        for (int i = 0; i < 4; i++) {
            double cx = x + i * 34 * scale;
            double bob = Math.sin(t * 1.4 + i) * 1.5;
            drawCamel(g, cx, y + bob, scale);
        }
    }

    private void drawCamel(Graphics2D g, double x, double y, double s) {
        Path2D.Double body = new Path2D.Double();
        body.moveTo(x - 14 * s, y);
        body.curveTo(x - 12 * s, y - 10 * s, x - 6 * s, y - 14 * s, x - 2 * s, y - 8 * s);
        body.curveTo(x + 2 * s, y - 14 * s, x + 8 * s, y - 10 * s, x + 10 * s, y);
        body.lineTo(x + 14 * s, y + 2 * s);
        body.lineTo(x + 10 * s, y + 16 * s);
        body.lineTo(x + 6 * s, y + 16 * s);
        body.lineTo(x + 6 * s, y + 4 * s);
        body.lineTo(x - 8 * s, y + 4 * s);
        body.lineTo(x - 10 * s, y + 16 * s);
        body.lineTo(x - 14 * s, y + 16 * s);
        body.closePath();
        g.fill(body);
        // neck and head
        g.fillRect((int) (x + 10 * s), (int) (y - 20 * s), (int) (3 * s), (int) (14 * s));
        g.fillOval((int) (x + 9 * s), (int) (y - 24 * s), (int) (6 * s), (int) (5 * s));
    }
}

/**
 * One camel of a wandering herd. Its position is anchored to a fixed track
 * index and walks back and forth purely along the local track normal (i.e.
 * straight across the road), driven by its own phase-offset timer so herd
 * members cross out of sync with each other.
 */
class Camel {
    final Point2D.Double basePoint;
    final Point2D.Double normal;
    final double reach, radius;
    private final double crossDuration, waitDuration;
    private double phaseTimer;
    double crossOffset;
    double facing = 1;
    double walkPhase;

    Camel(Point2D.Double basePoint, Point2D.Double normal, double reach, double crossDuration, double waitDuration, double phaseSeed) {
        this.basePoint = basePoint;
        this.normal = normal;
        this.reach = reach;
        this.crossDuration = crossDuration;
        this.waitDuration = waitDuration;
        this.radius = 17;
        this.phaseTimer = phaseSeed;
        updatePosition();
    }

    void update(double dt) {
        phaseTimer += dt;
        walkPhase += dt * 6;
        updatePosition();
    }

    private void updatePosition() {
        double cycle = 2 * crossDuration + 2 * waitDuration;
        double local = phaseTimer % cycle;
        if (local < 0) local += cycle;
        if (local < waitDuration) {
            crossOffset = -reach;
            facing = 1;
        } else if (local < waitDuration + crossDuration) {
            double f = (local - waitDuration) / crossDuration;
            crossOffset = -reach + Util.easeInOut(f) * (2 * reach);
            facing = 1;
        } else if (local < 2 * waitDuration + crossDuration) {
            crossOffset = reach;
            facing = -1;
        } else {
            double f = (local - 2 * waitDuration - crossDuration) / crossDuration;
            crossOffset = reach - Util.easeInOut(f) * (2 * reach);
            facing = -1;
        }
    }

    double x() { return basePoint.x + normal.x * crossOffset; }
    double y() { return basePoint.y + normal.y * crossOffset; }

    boolean isMoving() {
        double cycle = 2 * crossDuration + 2 * waitDuration;
        double local = phaseTimer % cycle;
        if (local < 0) local += cycle;
        return !(local < waitDuration) && !(local >= waitDuration + crossDuration && local < 2 * waitDuration + crossDuration);
    }

    void draw(Graphics2D g) {
        double cx = x(), cy = y();
        double ang = Math.atan2(normal.y * facing, normal.x * facing);
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.rotate(ang);

        double legSwing = isMoving() ? Math.sin(walkPhase) * 6 : 0;
        g.setColor(new Color(70, 50, 30));
        g.setStroke(new BasicStroke(3));
        g.drawLine(-9, 10, -9 + (int) legSwing, 20);
        g.drawLine(-2, 10, -2 - (int) legSwing, 20);
        g.drawLine(6, 10, 6 + (int) legSwing, 20);
        g.drawLine(12, 10, 12 - (int) legSwing, 20);
        g.setStroke(new BasicStroke(1));

        Path2D.Double body = new Path2D.Double();
        body.moveTo(-16, 10);
        body.curveTo(-14, -6, -6, -14, -2, -4);
        body.curveTo(2, -16, 10, -10, 14, 4);
        body.lineTo(16, 10);
        body.lineTo(-16, 10);
        body.closePath();
        g.setColor(new Color(196, 158, 104));
        g.fill(body);
        g.setColor(new Color(140, 108, 66));
        g.draw(body);

        g.setColor(new Color(196, 158, 104));
        g.fillRect(12, -22, 4, 18);
        g.fillOval(11, -27, 9, 8);
        g.setColor(new Color(140, 108, 66));
        g.drawRect(12, -22, 4, 18);

        g.setTransform(old);
    }
}

/** Signature hazard: a small herd of camels that wanders back and forth across one crossing point on the trail. */
class CamelHerd {
    final List<Camel> camels = new ArrayList<>();

    CamelHerd(Track track, int anchorIndex, int count, double reach, double crossDuration, double waitDuration, double stagger, int[] indexOffsets) {
        int n = track.centerline.size();
        for (int i = 0; i < count; i++) {
            int idx = ((anchorIndex + indexOffsets[i % indexOffsets.length]) % n + n) % n;
            Point2D.Double base = track.centerline.get(idx);
            Point2D.Double normal = track.normals.get(idx);
            camels.add(new Camel(base, normal, reach, crossDuration, waitDuration, i * stagger));
        }
    }

    void update(double dt) {
        for (Camel c : camels) c.update(dt);
    }

    void draw(Graphics2D g) {
        for (Camel c : camels) c.draw(g);
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

    private static final double MAX_SPEED = 400;
    private static final double MAX_REVERSE = -150;
    private static final double ACCEL = 280;
    private static final double BRAKE = 520;
    private static final double NATURAL_FRICTION = 140;
    private static final double OFFROAD_MULT = 2.5;
    private static final double TURN_RATE = 2.9;
    private static final double GRIP = 7.2;
    private static final double BOOST_MULT = 1.5;
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
        Color c = onTrack ? new Color(210, 180, 130, 120) : new Color(180, 140, 90, 160);
        for (int i = 0; i < (onTrack ? 1 : 2); i++) {
            double jx = (Math.random() - 0.5) * 10;
            double jy = (Math.random() - 0.5) * 10;
            particles.add(new Particle(rearX + jx, rearY + jy,
                    -vx * 0.15 + (Math.random() - 0.5) * 20, -vy * 0.15 + (Math.random() - 0.5) * 20,
                    0.5 + Math.random() * 0.4, 6 + Math.random() * 6, 8, c, Particle.ParticleKind.DUST));
        }
        if (boosting) {
            particles.add(new Particle(rearX, rearY, -vx * 0.3, -vy * 0.3, 0.35, 10, 6,
                    new Color(255, 150, 50, 200), Particle.ParticleKind.SPARK));
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
        for (int i = 0; i < 24; i++) {
            double a = Math.random() * Math.PI * 2;
            double sp = 60 + Math.random() * 150;
            particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp,
                    0.5 + Math.random() * 0.6, 4 + Math.random() * 6, 10,
                    Math.random() < 0.5 ? new Color(230, 140, 40) : new Color(120, 96, 66), Particle.ParticleKind.SMOKE));
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
            g.setColor(new Color(255, 160, 50, 200));
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

    InputState think(double dt, List<Obstacle> obstacles, List<Camel> camels) {
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

        for (Camel cm : camels) {
            double dx = cm.x() - car.x, dy = cm.y() - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < 110) {
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
    private final List<Scenery> scenery = new ArrayList<>();
    private final List<CaravanSilhouette> caravans = new ArrayList<>();
    private final List<CamelHerd> herds = new ArrayList<>();
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
        track = new Track(230, 1050, 240, 520, 140, 50, 80);
        int n = track.centerline.size();

        obstacles.clear();
        Random r = new Random(77);
        for (int i = 0; i < n; i += 13) {
            if (r.nextDouble() < 0.45) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 18 + r.nextDouble() * 45;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.ROCK : Obstacle.Kind.SCRUB;
                obstacles.add(new Obstacle(k, p.x, p.y, 11 + r.nextInt(9)));
            }
        }
        int[] crateIdx = {30, 90, 150, 210, 270, 330};
        for (int idx : crateIdx) {
            if (idx >= n) continue;
            double off = (r.nextDouble() - 0.5) * track.trackWidth * 0.5;
            Point2D.Double p = track.pointAtOffset(idx, off);
            obstacles.add(new Obstacle(Obstacle.Kind.CRATE, p.x, p.y, 14));
        }
        for (int i = 0; i < 3; i++) {
            Obstacle tw = new Obstacle(Obstacle.Kind.TUMBLEWEED, r.nextInt(W), r.nextInt(H), 12);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 35 + r.nextDouble() * 35;
            tw.vx = Math.cos(a) * sp;
            tw.vy = Math.sin(a) * sp;
            obstacles.add(tw);
        }

        scenery.clear();
        Random sr = new Random(21);
        for (int i = 0; i < n; i += 9) {
            Point2D.Double p = track.outwardPoint(i, 40 + sr.nextInt(35));
            double scale = 0.8 + sr.nextDouble() * 0.6;
            double tilt = (sr.nextDouble() - 0.5) * 0.4;
            double roll = sr.nextDouble();
            Scenery.Kind k = roll < 0.5 ? Scenery.Kind.BOULDER : roll < 0.8 ? Scenery.Kind.DEAD_TREE : Scenery.Kind.CACTUS;
            scenery.add(new Scenery(k, p.x, p.y, scale, tilt));
        }

        caravans.clear();
        caravans.add(new CaravanSilhouette(-100, 150, 1.1, 14));
        caravans.add(new CaravanSilhouette(400, 190, 0.8, -9));

        // Wandering camel herd: two crossing points, one on each corridor of the switchback.
        herds.clear();
        int topCrossIdx = track.nearestIndex((track.x0 + track.x1) / 2.0, track.yTop);
        int botCrossIdx = track.nearestIndex((track.x0 + track.x1) / 2.0, track.yBot);
        double reach = track.halfWidth + 95;
        int[] offsets = {-14, -7, 0, 7, 14};
        herds.add(new CamelHerd(track, topCrossIdx, 5, reach, 2.2, 1.6, 1.4, offsets));
        herds.add(new CamelHerd(track, botCrossIdx, 4, reach, 2.6, 2.0, 1.6, new int[]{-10, -3, 4, 11}));
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-20);
        Point2D.Double p2pos = track.startPosition(20);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(180, 70, 40), new Color(230, 200, 150), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(80, 96, 110), new Color(220, 210, 195), vsAI ? "AI" : "2", particles);
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
        for (CamelHerd herd : herds) herd.update(dt);

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

    private List<Camel> allCamels() {
        List<Camel> all = new ArrayList<>();
        for (CamelHerd h : herds) all.addAll(h.camels);
        return all;
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
        List<Camel> camels = allCamels();
        if (vsAI) {
            in2 = aiDriver.think(dt, obstacles, camels);
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
        for (Camel c : camels) {
            resolveCamelCollision(p1, c);
            resolveCamelCollision(p2, c);
        }

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

    private void spawnLapBurst(double x, double y) {
        for (int i = 0; i < 20; i++) {
            double a = Math.random() * Math.PI * 2;
            double sp = 60 + Math.random() * 120;
            particles.add(new Particle(x, y - 10, Math.cos(a) * sp, Math.sin(a) * sp - 30,
                    0.5 + Math.random() * 0.4, 4 + Math.random() * 3, -2,
                    new Color(255, 210, 120), Particle.ParticleKind.SPARK));
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

    /** Signature hazard collision: bump + damage against whichever camel currently blocks the road. */
    private void resolveCamelCollision(Car c, Camel camel) {
        if (!c.alive) return;
        double cx = camel.x(), cy = camel.y();
        double dx = c.x - cx, dy = c.y - cy;
        double dist = Math.hypot(dx, dy);
        double minDist = c.radius() + camel.radius;
        if (dist < minDist && dist > 0.0001) {
            double nx = dx / dist, ny = dy / dist;
            double overlap = minDist - dist;
            c.x += nx * overlap;
            c.y += ny * overlap;
            double impact = Math.max(70, Math.abs(c.speed));
            c.damage(impact * 0.10 + 6);
            c.speed *= -0.4;
            c.spinOut(0.7);
            for (int i = 0; i < 12; i++) {
                double ang = Math.random() * Math.PI * 2;
                double sp = 60 + Math.random() * 130;
                particles.add(new Particle(c.x, c.y, Math.cos(ang) * sp, Math.sin(ang) * sp,
                        0.4 + Math.random() * 0.3, 4, 4, new Color(210, 180, 130), Particle.ParticleKind.DEBRIS));
            }
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
        for (Obstacle o : obstacles) o.draw(sg);
        for (CamelHerd h : herds) h.draw(sg);
        particles.draw(sg);
        for (Car c : cars) c.draw(sg);
        sg.dispose();

        applyDustHaze(sceneBuffer);

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

    private void applyDustHaze(BufferedImage img) {
        int top = (int) (H * 0.34), bandH = 40;
        for (int y = top; y < top + bandH && y < H; y++) {
            double t = (y - top) / (double) bandH;
            int shift = (int) (Math.sin(y * 0.22 + timeAccum * 2.6) * 3 * (1 - t));
            if (shift == 0) continue;
            int[] row = img.getRGB(0, y, W, 1, null, 0, W);
            int[] shifted = new int[W];
            for (int x = 0; x < W; x++) {
                int sx = x - shift;
                if (sx < 0) sx = 0;
                if (sx >= W) sx = W - 1;
                shifted[x] = row[sx];
            }
            img.setRGB(0, y, W, 1, shifted, 0, W);
        }
    }

    private void drawBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(232, 176, 110), 0, (float) (H * 0.4), new Color(224, 196, 150)));
        g.fillRect(0, 0, W, (int) (H * 0.4));

        double pulse = 1 + 0.03 * Math.sin(timeAccum * 1.4);
        int sunR = (int) (56 * pulse);
        g.setPaint(new RadialGradientPaint(new Point(W - 190, 100), sunR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(255, 240, 200, 215), new Color(240, 190, 120, 95), new Color(240, 190, 120, 0)}));
        g.fillOval(W - 190 - sunR * 3, 100 - sunR * 3, sunR * 6, sunR * 6);
        g.setColor(new Color(255, 246, 220));
        g.fillOval(W - 190 - sunR / 2, 100 - sunR / 2, sunR, sunR);

        for (CaravanSilhouette c : caravans) c.draw(g, timeAccum, W);

        g.setColor(new Color(150, 108, 68));
        Path2D.Double dunes = new Path2D.Double();
        dunes.moveTo(-50, H * 0.4);
        double wx = -50;
        Random wr = new Random(11);
        while (wx < W + 50) {
            double wy = H * 0.4 - (14 + wr.nextInt(30));
            dunes.curveTo(wx + 20, wy - 8, wx + 40, wy, wx + 60, H * 0.4 - (10 + wr.nextInt(10)));
            wx += 60;
        }
        dunes.lineTo(W + 50, H * 0.4);
        dunes.closePath();
        g.fill(dunes);

        g.setPaint(new GradientPaint(0, (float) (H * 0.38), new Color(198, 158, 108), 0, H, new Color(150, 112, 70)));
        g.fillRect(0, (int) (H * 0.38), W, (int) (H * 0.62));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(232, 178, 92));
        g.setFont(new Font("SansSerif", Font.BOLD, 52));
        centerText(g, "CAMEL TRAIL SWITCHBACK", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Mountain caravan trail racing — thread the wandering camel herd", H / 2 - 90);

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
        g.setColor(new Color(90, 170, 230));
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
