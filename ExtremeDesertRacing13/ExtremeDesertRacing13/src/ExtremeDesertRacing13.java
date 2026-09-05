import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ExtremeDesertRacing13 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Nomad Camp Teardrop — Extreme Desert Racing");
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

    enum ParticleKind { DUST, SMOKE, SPARK, DEBRIS, EMBER }

    Particle(double x, double y, double vx, double vy, double life, double size, double growth, Color color, ParticleKind kind) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.life = life; this.maxLife = life;
        this.size = size; this.growth = growth;
        this.color = color; this.kind = kind;
    }

    boolean update(double dt) {
        x += vx * dt;
        y += vy * dt;
        if (kind == ParticleKind.EMBER) {
            vy -= 15 * dt; // embers drift upward on the fire's heat
            vx *= 0.97;
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

/** Thread-safe: physics thread adds/updates, EDT paints — both touch the same list. */
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
 * Asymmetric teardrop track: a scalene triangle whose three vertices are each
 * rounded off ("filleted") with their own radius — one apex uses a noticeably
 * smaller radius (sharp/tight) while the other two corners use larger, and
 * different, radii (rounded, uneven) so the loop reads as a lopsided teardrop
 * rather than an even tri-oval.
 *
 * Fillet math per vertex V with neighbors Vprev/Vnext: the tangent length from
 * V to each edge's touch point is r / tan(theta/2) where theta is V's interior
 * angle; the fillet circle's center sits on the interior bisector at distance
 * tangentLength / cos(theta/2). Because the source triangle is convex and the
 * three tangent lengths are iteratively shrunk so no two adjacent fillets ever
 * claim more than 90% of the edge between them, the resulting rounded curve is
 * guaranteed to stay convex and simple (non-self-intersecting) — the classic
 * "rounded polygon" construction.
 *
 * Lap counting and edge rendering reuse the proven technique: a dense
 * centerline point list with a parallel cumulative arc-length array (robust
 * signed lap progress with wraparound) and per-point normals for the
 * offset-based road edges.
 */
class Track {
    final double trackWidth, halfWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;
    double innerSign = 1.0;
    final Point2D.Double centroid;

    Track(Point2D.Double vApex, Point2D.Double vLeft, Point2D.Double vRight,
          double rApex, double rLeft, double rRight, double trackWidth) {
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        this.centroid = new Point2D.Double((vApex.x + vLeft.x + vRight.x) / 3.0,
                (vApex.y + vLeft.y + vRight.y) / 3.0);
        build(new Point2D.Double[]{vApex, vLeft, vRight}, new double[]{rApex, rLeft, rRight});
    }

    private static double[] normalize(double x, double y) {
        double len = Math.hypot(x, y);
        if (len < 1e-9) return new double[]{1, 0};
        return new double[]{x / len, y / len};
    }

    private void build(Point2D.Double[] V, double[] reqR) {
        int m = 3;
        double[] theta = new double[m];
        double[][] va = new double[m][2];
        double[][] vb = new double[m][2];
        double[] tangentDist = new double[m];

        for (int k = 0; k < m; k++) {
            Point2D.Double vk = V[k];
            Point2D.Double vp = V[(k - 1 + m) % m];
            Point2D.Double vn = V[(k + 1) % m];
            va[k] = normalize(vp.x - vk.x, vp.y - vk.y);
            vb[k] = normalize(vn.x - vk.x, vn.y - vk.y);
            double dot = Util.clamp(va[k][0] * vb[k][0] + va[k][1] * vb[k][1], -1, 1);
            theta[k] = Math.acos(dot);
            tangentDist[k] = reqR[k] / Math.tan(theta[k] / 2);
        }

        double[] edgeLen = new double[m];
        for (int k = 0; k < m; k++) edgeLen[k] = V[k].distance(V[(k + 1) % m]);

        // Iteratively shrink tangent lengths so adjacent fillets never eat more
        // than 90% of the edge between them -- guarantees a simple, convex loop.
        for (int pass = 0; pass < 4; pass++) {
            for (int k = 0; k < m; k++) {
                int kn = (k + 1) % m;
                double sum = tangentDist[k] + tangentDist[kn];
                double limit = edgeLen[k] * 0.9;
                if (sum > limit && sum > 1e-6) {
                    double factor = limit / sum;
                    tangentDist[k] *= factor;
                    tangentDist[kn] *= factor;
                }
            }
        }

        Point2D.Double[] A = new Point2D.Double[m];
        Point2D.Double[] B = new Point2D.Double[m];
        Point2D.Double[] center = new Point2D.Double[m];
        for (int k = 0; k < m; k++) {
            Point2D.Double vk = V[k];
            A[k] = new Point2D.Double(vk.x + va[k][0] * tangentDist[k], vk.y + va[k][1] * tangentDist[k]);
            B[k] = new Point2D.Double(vk.x + vb[k][0] * tangentDist[k], vk.y + vb[k][1] * tangentDist[k]);
            double[] bis = normalize(va[k][0] + vb[k][0], va[k][1] + vb[k][1]);
            double centerDist = tangentDist[k] / Math.cos(theta[k] / 2);
            center[k] = new Point2D.Double(vk.x + bis[0] * centerDist, vk.y + bis[1] * centerDist);
        }

        List<Point2D.Double> pts = new ArrayList<>();
        for (int k = 0; k < m; k++) {
            int kn = (k + 1) % m;
            appendLine(pts, B[k], A[kn]);
            appendArc(pts, A[kn], B[kn], center[kn]);
        }
        if (pts.size() > 1 && pts.get(0).distance(pts.get(pts.size() - 1)) < 0.5) {
            pts.remove(pts.size() - 1);
        }
        centerline.addAll(pts);

        int n = centerline.size();
        cumulativeLength = new double[n];
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

        int probe = n / 2;
        Point2D.Double p = centerline.get(probe);
        Point2D.Double nrm = normals.get(probe);
        double dHere = p.distance(centroid);
        double dPoke = Point2D.distance(p.x + nrm.x * 5, p.y + nrm.y * 5, centroid.x, centroid.y);
        innerSign = (dPoke < dHere) ? 1.0 : -1.0;
    }

    private void appendLine(List<Point2D.Double> pts, Point2D.Double from, Point2D.Double to) {
        double len = from.distance(to);
        int samples = Math.max(18, (int) Math.round(len / 6.0));
        int start = pts.isEmpty() ? 0 : 1;
        for (int i = start; i <= samples; i++) {
            double t = i / (double) samples;
            pts.add(new Point2D.Double(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t));
        }
    }

    private void appendArc(List<Point2D.Double> pts, Point2D.Double from, Point2D.Double to, Point2D.Double center) {
        double angA = Math.atan2(from.y - center.y, from.x - center.x);
        double angB = Math.atan2(to.y - center.y, to.x - center.x);
        double sweep = Util.normalizeAngle(angB - angA);
        double r = from.distance(center);
        int samples = Math.max(10, (int) Math.round(Math.abs(sweep) / Math.toRadians(3)));
        int start = pts.isEmpty() ? 0 : 1;
        for (int i = start; i <= samples; i++) {
            double t = i / (double) samples;
            double ang = angA + sweep * t;
            pts.add(new Point2D.Double(center.x + Math.cos(ang) * r, center.y + Math.sin(ang) * r));
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

    /** A point `depth` units further outward (away from the infield) than the outer track edge, at index. */
    Point2D.Double outwardPoint(int index, double depth) {
        return pointAtOffset(index, -innerSign * (halfWidth + depth));
    }

    Point2D.Double startPosition(double laneOffset) {
        return pointAtOffset(6, laneOffset);
    }

    double startAngle() {
        Point2D.Double a = centerline.get(6);
        Point2D.Double b = centerline.get(12);
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
        g.setColor(new Color(178, 110, 62));
        g.fill(road);

        g.setColor(new Color(140, 82, 46, 120));
        for (int i = 0; i < centerline.size(); i += 6) {
            Point2D.Double o = pointAtOffset(i, halfWidth);
            Point2D.Double in = pointAtOffset(i, -halfWidth);
            g.draw(new Line2D.Double(o, in));
        }

        g.setColor(new Color(94, 48, 26));
        g.setStroke(new BasicStroke(4));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(255, 214, 150, 150));
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
        Point2D.Double o = pointAtOffset(6, halfWidth);
        Point2D.Double in = pointAtOffset(6, -halfWidth);
        int squares = 8;
        double dx = (o.x - in.x) / squares, dy = (o.y - in.y) / squares;
        for (int i = 0; i < squares; i++) {
            double sx = in.x + dx * i, sy = in.y + dy * i;
            g.setColor(i % 2 == 0 ? Color.WHITE : new Color(40, 28, 18));
            g.fill(new Rectangle2D.Double(sx - 4, sy - 4, 8, 8));
        }
    }
}

class Obstacle {
    enum Kind { ROCK, CRATE, WAGON_WRECK, FIRE_RING }

    double x, y, radius, rotation;
    Kind kind;

    Obstacle(Kind kind, double x, double y, double radius) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.rotation = Math.random() * Math.PI * 2;
    }

    void draw(Graphics2D g) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(rotation);
        switch (kind) {
            case ROCK:
                g.setColor(new Color(150, 100, 66));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.7));
                g.setColor(new Color(104, 66, 40));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.7));
                g.fillOval((int) (-radius * 0.3), (int) (-radius * 0.3), (int) (radius * 0.8), (int) (radius * 0.6));
                break;
            case CRATE:
                g.setColor(new Color(150, 108, 58));
                g.fillRect((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                g.setColor(new Color(90, 62, 30));
                g.drawRect((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                g.drawLine((int) -radius, (int) -radius, (int) radius, (int) radius);
                g.drawLine((int) radius, (int) -radius, (int) -radius, (int) radius);
                break;
            case WAGON_WRECK:
                g.setColor(new Color(70, 46, 30));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(new Color(120, 70, 20, 200));
                g.fillRect((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius / 2));
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                break;
            case FIRE_RING:
                g.setColor(new Color(96, 86, 76));
                g.setStroke(new BasicStroke(5));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                g.setStroke(new BasicStroke(1));
                break;
        }
        g.setTransform(old);
    }
}

/** Purely decorative silhouetted nomad tents ringing the outer rim: no collision. */
class Tent {
    final double x, y, w, h, tilt;
    final Color cloth;

    Tent(double x, double y, double w, double h, double tilt, Color cloth) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.tilt = tilt; this.cloth = cloth;
    }

    void draw(Graphics2D g) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(tilt);
        Path2D.Double body = new Path2D.Double();
        body.moveTo(-w / 2, 0);
        body.lineTo(0, -h);
        body.lineTo(w / 2, 0);
        body.closePath();
        g.setColor(new Color(20, 18, 26, 235));
        g.fill(body);
        g.setColor(new Color(255, 150, 60, 70));
        g.fillRect((int) (-w * 0.06), (int) (-h * 0.55), (int) (w * 0.12), (int) (h * 0.55));
        g.setColor(new Color(10, 9, 14));
        g.draw(body);
        g.setTransform(old);
    }
}

/**
 * Central campfire: a soft radial glow bled onto the nearby track/infield, an
 * animated flicker flame, and a steady stream of drifting embers. Purely
 * atmospheric; a separate FIRE_RING Obstacle at the same spot provides the
 * actual stone-ring collision.
 */
class Campfire {
    final double x, y;

    Campfire(double x, double y) { this.x = x; this.y = y; }

    void drawGlow(Graphics2D g, double t) {
        double pulse = 1 + 0.08 * Math.sin(t * 5.0);
        int r = (int) (190 * pulse);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), r,
                new float[]{0f, 0.35f, 1f},
                new Color[]{new Color(255, 170, 60, 130), new Color(230, 110, 40, 55), new Color(230, 110, 40, 0)}));
        g.fillOval((int) (x - r), (int) (y - r), r * 2, r * 2);
    }

    void drawFlame(Graphics2D g, double t) {
        g.setColor(new Color(70, 60, 55));
        g.fillOval((int) (x - 26), (int) (y - 4), 52, 16);
        double flick = Math.sin(t * 9.0) * 3 + Math.sin(t * 13.0) * 2;
        Path2D.Double flame = new Path2D.Double();
        flame.moveTo(x - 12, y);
        flame.curveTo(x - 14, y - 22, x - 4 + flick, y - 30, x, y - 46 + flick);
        flame.curveTo(x + 4 - flick, y - 30, x + 14, y - 22, x + 12, y);
        flame.closePath();
        g.setColor(new Color(255, 140, 30, 225));
        g.fill(flame);
        g.setColor(new Color(255, 220, 90, 225));
        g.fill(new Ellipse2D.Double(x - 6, y - 22, 12, 20));
    }

    void maybeEmitEmber(ParticleSystem particles, double dt) {
        if (Math.random() < dt * 14) {
            double a = -Math.PI / 2 + (Math.random() - 0.5) * 0.8;
            double sp = 30 + Math.random() * 50;
            particles.add(new Particle(x + (Math.random() - 0.5) * 14, y - 10,
                    Math.cos(a) * sp * 0.3, Math.sin(a) * sp,
                    0.6 + Math.random() * 0.6, 2 + Math.random() * 3, -1,
                    new Color(255, 170 + (int) (Math.random() * 60), 60), Particle.ParticleKind.EMBER));
        }
    }
}

/**
 * Signature hazard: a swarm of tumbleweeds shoved around by gusty wind. Each
 * one drifts on a genuinely unpredictable bouncing path (random gust nudges to
 * its heading/speed every fraction of a second, plus bouncing softly off the
 * play-field bounds) rather than following a fixed line, and periodically
 * "respawns" — vanishing and re-entering from an off-track wind source at the
 * field's edge — to keep the swarm fresh.
 */
class Tumbleweed {
    double x, y, vx, vy, radius, rotation;
    private double gustTimer, respawnTimer;
    private final Random rnd;

    Tumbleweed(Random rnd, double minX, double maxX, double minY, double maxY) {
        this.rnd = rnd;
        respawnFromWind(minX, maxX, minY, maxY);
    }

    void update(double dt, double minX, double maxX, double minY, double maxY) {
        gustTimer -= dt;
        if (gustTimer <= 0) {
            double curAngle = Math.atan2(vy, vx);
            double newAngle = curAngle + (rnd.nextDouble() - 0.5) * 1.6;
            double speed = Util.clamp(Math.hypot(vx, vy) + (rnd.nextDouble() - 0.5) * 70, 50, 230);
            vx = Math.cos(newAngle) * speed;
            vy = Math.sin(newAngle) * speed;
            gustTimer = 0.35 + rnd.nextDouble() * 0.8;
        }
        x += vx * dt;
        y += vy * dt;
        if (x < minX) { x = minX; vx = Math.abs(vx); }
        if (x > maxX) { x = maxX; vx = -Math.abs(vx); }
        if (y < minY) { y = minY; vy = Math.abs(vy); }
        if (y > maxY) { y = maxY; vy = -Math.abs(vy); }

        double spd = Math.hypot(vx, vy);
        rotation += (spd / Math.max(6, radius)) * dt * (vx >= 0 ? 1 : -1);

        respawnTimer -= dt;
        if (respawnTimer <= 0) respawnFromWind(minX, maxX, minY, maxY);
    }

    void respawnFromWind(double minX, double maxX, double minY, double maxY) {
        int side = rnd.nextInt(4);
        double speed = 90 + rnd.nextDouble() * 90;
        switch (side) {
            case 0:
                x = minX; y = minY + rnd.nextDouble() * (maxY - minY);
                vx = speed; vy = (rnd.nextDouble() - 0.5) * 80;
                break;
            case 1:
                x = maxX; y = minY + rnd.nextDouble() * (maxY - minY);
                vx = -speed; vy = (rnd.nextDouble() - 0.5) * 80;
                break;
            case 2:
                y = minY; x = minX + rnd.nextDouble() * (maxX - minX);
                vy = speed; vx = (rnd.nextDouble() - 0.5) * 80;
                break;
            default:
                y = maxY; x = minX + rnd.nextDouble() * (maxX - minX);
                vy = -speed; vx = (rnd.nextDouble() - 0.5) * 80;
                break;
        }
        radius = 12 + rnd.nextDouble() * 9;
        gustTimer = 0.2;
        respawnTimer = 8 + rnd.nextDouble() * 9;
    }

    void draw(Graphics2D g) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(rotation);
        g.setColor(new Color(150, 112, 58));
        g.setStroke(new BasicStroke(1.4f));
        for (int i = 0; i < 7; i++) {
            double a = i * Math.PI / 7;
            double rr = radius * (0.8 + 0.2 * ((i * 37) % 5) / 5.0);
            g.draw(new Line2D.Double(-Math.cos(a) * rr, -Math.sin(a) * rr, Math.cos(a) * rr, Math.sin(a) * rr));
        }
        g.setColor(new Color(120, 88, 44));
        g.draw(new Ellipse2D.Double(-radius, -radius * 0.85, radius * 2, radius * 1.7));
        g.setStroke(new BasicStroke(1));
        g.setTransform(old);
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
        Color c = onTrack ? new Color(210, 160, 110, 120) : new Color(180, 130, 80, 160);
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

    InputState think(double dt, List<Obstacle> obstacles, List<Tumbleweed> tumbleweeds) {
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

        for (Tumbleweed t : tumbleweeds) {
            double dx = t.x - car.x, dy = t.y - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < 110) {
                double angToObs = Util.normalizeAngle(Math.atan2(dy, dx) - car.angle);
                if (Math.abs(angToObs) < 1.0) {
                    diff += angToObs < 0 ? 0.75 : -0.75;
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
    private final List<Tent> tents = new ArrayList<>();
    private final List<Tumbleweed> tumbleweeds = new ArrayList<>();
    private Campfire campfire;
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
        // Asymmetric teardrop: apex is tight/sharp, the two base corners are
        // rounded but by noticeably different amounts (lopsided teardrop feel).
        Point2D.Double vApex = new Point2D.Double(640, 190);
        Point2D.Double vLeft = new Point2D.Double(480, 590);
        Point2D.Double vRight = new Point2D.Double(800, 575);
        track = new Track(vApex, vLeft, vRight, 70, 130, 165, 80);
        int n = track.centerline.size();

        obstacles.clear();
        Random r = new Random(42);
        for (int i = 0; i < n; i += 13) {
            if (r.nextDouble() < 0.5) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 22 + r.nextDouble() * 55;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.ROCK : Obstacle.Kind.CRATE;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(9)));
            }
        }
        int[] wreckIdx = {20, 90, 160};
        for (int idx : wreckIdx) {
            if (idx >= n) continue;
            double off = (r.nextDouble() - 0.5) * track.trackWidth * 0.5;
            Point2D.Double p = track.pointAtOffset(idx, off);
            obstacles.add(new Obstacle(Obstacle.Kind.WAGON_WRECK, p.x, p.y, 15));
        }
        // Central campfire and its low stone fire-ring, right in the infield.
        campfire = new Campfire(track.centroid.x, track.centroid.y);
        obstacles.add(new Obstacle(Obstacle.Kind.FIRE_RING, track.centroid.x, track.centroid.y, 26));

        // Silhouetted tents ringing the outer rim of the camp.
        tents.clear();
        Random tr = new Random(99);
        Color[] cloths = {new Color(70, 34, 26), new Color(60, 40, 30), new Color(74, 46, 34)};
        for (int i = 0; i < n; i += 22) {
            Point2D.Double p = track.outwardPoint(i, 55 + tr.nextInt(35));
            double tilt = (tr.nextDouble() - 0.5) * 0.25;
            double w = 46 + tr.nextInt(20);
            double h = 40 + tr.nextInt(24);
            tents.add(new Tent(p.x, p.y, w, h, tilt, cloths[tr.nextInt(cloths.length)]));
        }

        // Rolling tumbleweed swarm: unpredictable gust-driven wanderers that
        // range across the whole play field (including straight across the
        // track) and periodically re-blow in from an off-field wind source.
        tumbleweeds.clear();
        Random wr = new Random(7);
        for (int i = 0; i < 6; i++) {
            tumbleweeds.add(new Tumbleweed(wr, -100, W + 100, -100, H + 100));
        }
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-20);
        Point2D.Double p2pos = track.startPosition(20);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(196, 84, 40), new Color(240, 210, 160), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(70, 90, 110), new Color(220, 210, 195), vsAI ? "AI" : "2", particles);
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
        for (Tumbleweed t : tumbleweeds) t.update(dt, -100, W + 100, -100, H + 100);
        campfire.maybeEmitEmber(particles, dt);

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
            in2 = aiDriver.think(dt, obstacles, tumbleweeds);
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
        for (Tumbleweed t : tumbleweeds) {
            resolveTumbleweedCollision(p1, t);
            resolveTumbleweedCollision(p2, t);
        }

        if (p1.justLapped) spawnLapBurst(p1);
        if (p2.justLapped) spawnLapBurst(p2);

        for (Car c : cars) {
            if (!c.finished && c.lap >= TOTAL_LAPS) {
                c.finished = true;
                c.finishTime = raceTime;
            }
        }
        if (p1.finished && p2.finished) state = State.FINISHED;
    }

    private void spawnLapBurst(Car c) {
        for (int i = 0; i < 18; i++) {
            double a = Math.random() * Math.PI * 2;
            double sp = 60 + Math.random() * 120;
            particles.add(new Particle(c.x, c.y - 20, Math.cos(a) * sp, Math.sin(a) * sp - 30,
                    0.5 + Math.random() * 0.4, 3 + Math.random() * 4, -2,
                    new Color(255, 200, 90), Particle.ParticleKind.SPARK));
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
            c.damage(impact * 0.08);
            c.speed *= -0.35;
            c.spinOut(0.45);
        }
    }

    /** Signature-mechanic collision: a moderate bump/spin, and the tumbleweed itself caroms off. */
    private void resolveTumbleweedCollision(Car c, Tumbleweed t) {
        if (!c.alive) return;
        double dx = c.x - t.x, dy = c.y - t.y;
        double dist = Math.hypot(dx, dy);
        double minDist = c.radius() + t.radius;
        if (dist < minDist && dist > 0.0001) {
            double nx = dx / dist, ny = dy / dist;
            double overlap = minDist - dist;
            c.x += nx * overlap;
            c.y += ny * overlap;
            double impact = Math.max(60, Math.abs(c.speed));
            c.damage(impact * 0.05 + 6);
            c.speed *= -0.25;
            c.spinOut(0.6);
            t.vx = -t.vx * 0.5 + nx * 90 + (Math.random() - 0.5) * 40;
            t.vy = -t.vy * 0.5 + ny * 90 + (Math.random() - 0.5) * 40;
            for (int i = 0; i < 10; i++) {
                double ang = Math.random() * Math.PI * 2;
                double sp = 50 + Math.random() * 100;
                particles.add(new Particle(t.x, t.y, Math.cos(ang) * sp, Math.sin(ang) * sp,
                        0.35 + Math.random() * 0.3, 3, 3, new Color(170, 130, 70), Particle.ParticleKind.DEBRIS));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D sg = sceneBuffer.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBackground(sg);
        for (Tent t : tents) t.draw(sg);
        track.draw(sg);
        campfire.drawGlow(sg, timeAccum);
        for (Obstacle o : obstacles) o.draw(sg);
        for (Tumbleweed tw : tumbleweeds) tw.draw(sg);
        campfire.drawFlame(sg, timeAccum);
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
        // Deep blue dusk sky fading down into warm firelit horizon.
        g.setPaint(new GradientPaint(0, 0, new Color(18, 22, 58), 0, (float) (H * 0.5), new Color(214, 112, 70)));
        g.fillRect(0, 0, W, (int) (H * 0.5));

        Random sr = new Random(3);
        g.setColor(new Color(255, 255, 255, 170));
        for (int i = 0; i < 55; i++) {
            int sx = sr.nextInt(W);
            int sy = sr.nextInt((int) (H * 0.3));
            g.fillRect(sx, sy, 2, 2);
        }

        // Distant dune silhouette.
        g.setColor(new Color(64, 42, 38));
        Path2D.Double dune = new Path2D.Double();
        double dx0 = -50;
        dune.moveTo(dx0, H * 0.5);
        Random dr = new Random(11);
        double dx = dx0;
        while (dx < W + 50) {
            double dy = H * 0.5 - (20 + dr.nextInt(50));
            dune.lineTo(dx, dy);
            dx += 40 + dr.nextInt(60);
        }
        dune.lineTo(W + 50, H * 0.5);
        dune.closePath();
        g.fill(dune);

        g.setPaint(new GradientPaint(0, (float) (H * 0.48), new Color(150, 90, 56), 0, H, new Color(96, 58, 34)));
        g.fillRect(0, (int) (H * 0.46), W, (int) (H * 0.54));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(240, 150, 70));
        g.setFont(new Font("SansSerif", Font.BOLD, 50));
        centerText(g, "NOMAD CAMP TEARDROP", H / 2 - 150);
        g.setFont(new Font("SansSerif", Font.BOLD, 19));
        g.setColor(Color.WHITE);
        centerText(g, "Dusk desert racing around the firelit camp — watch for the tumbleweed swarm", H / 2 - 98);

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
        g.setColor(new Color(255, 210, 70));
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
        g.setColor(new Color(255, 150, 60));
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
