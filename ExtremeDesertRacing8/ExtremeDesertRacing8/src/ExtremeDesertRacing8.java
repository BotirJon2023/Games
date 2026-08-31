import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * ExtremeDesertRacing8 -- "Bedrock Canyon Run"
 *
 * A winding sedimentary-rock canyon loop with two collapsing timber bridge
 * crossings over gaps in the canyon floor. Reuses the reusable-engine
 * patterns (drift-physics Car, centerline+arc-length Track, thread-safe
 * ParticleSystem, lookahead AIDriver, fixed-timestep GamePanel state
 * machine) established by the collection's reference architecture.
 */
public class ExtremeDesertRacing8 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Bedrock Canyon Run -- Extreme Desert Racing 8");
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
    enum Kind { DUST, SMOKE, SPARK, DEBRIS }

    double x, y, vx, vy;
    double life, maxLife;
    double size, growth;
    Color color;
    Kind kind;

    Particle(double x, double y, double vx, double vy, double life, double size, double growth, Color color, Kind kind) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.life = life; this.maxLife = life;
        this.size = size; this.growth = growth;
        this.color = color; this.kind = kind;
    }

    boolean update(double dt) {
        x += vx * dt;
        y += vy * dt;
        if (kind == Kind.DEBRIS) {
            vy += 70 * dt; // small rock chips fall under gravity
            vx *= 0.985;
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

/**
 * Dust/smoke/spark/debris particle pool. Both the physics thread and the
 * Swing EDT touch this concurrently (physics adds/updates, paint draws), so
 * every method that walks the backing list MUST be synchronized to avoid an
 * intermittent ConcurrentModificationException.
 */
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
            double s = Math.max(0.5, p.size);
            if (p.kind == Particle.Kind.DEBRIS) {
                AffineTransform t = g.getTransform();
                g.translate(p.x, p.y);
                g.rotate(p.x * 0.13 + p.y * 0.07);
                g.setColor(p.color);
                g.fillRect((int) (-s / 2), (int) (-s / 2), (int) s, (int) s);
                g.setTransform(t);
            } else {
                g.setColor(p.color);
                g.fill(new Ellipse2D.Double(p.x - s / 2, p.y - s / 2, s, s));
            }
            g.setComposite(old);
        }
    }
}

/**
 * Bedrock Canyon centerline: built as a piecewise Cartesian path, not a
 * polar/sine-perturbed loop. A small ring of hand-placed waypoints is walked
 * in strictly increasing angle around a shared center (guaranteeing the raw
 * polygon is simple / non-self-intersecting -- a star-shaped polygon can
 * never cross itself), then each corner is rounded with a bounded quadratic
 * Bezier cut (never reaching past the midpoint of either adjacent edge, so
 * the rounding cannot introduce new crossings). The result is one simple
 * closed loop with a couple of wide sweeping bends and two tight
 * switch-corners. Lap counting and edge rendering reuse the reference
 * technique: a centerline point list, a parallel cumulative arc-length
 * array (robust, wraparound-safe lap progress) and per-point normals for
 * offset-based inner/outer edges.
 */
class Track {
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;
    final double halfWidth;
    double innerSign = 1.0;

    Track(double halfWidth) {
        this.halfWidth = halfWidth;
        build();
    }

    private void build() {
        // Hand-placed Cartesian waypoints tracing the canyon loop. Angles
        // measured from a common center strictly increase point-to-point,
        // so connecting them in order is guaranteed to be a simple polygon.
        double[][] wp = {
                {900, 380}, {889, 427}, {855, 474}, {785, 511}, {690, 526}, {594, 516},
                {523, 486}, {491, 445}, {518, 403}, {440, 380}, {405, 336}, {429, 288},
                {498, 252}, {592, 239}, {682, 257}, {725, 303}, {743, 335}, {837, 343}
        };
        List<Point2D.Double> raw = new ArrayList<>();
        for (double[] p : wp) raw.add(new Point2D.Double(p[0], p[1]));

        List<Point2D.Double> smooth = roundCorners(raw, 0.35, 16, 10);
        centerline.addAll(smooth);

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

        // Determine which normal direction points into the infield.
        Point2D.Double centroid = new Point2D.Double(0, 0);
        for (Point2D.Double p : centerline) { centroid.x += p.x; centroid.y += p.y; }
        centroid.x /= n; centroid.y /= n;
        int probe = 0;
        Point2D.Double p0 = centerline.get(probe);
        Point2D.Double n0 = normals.get(probe);
        double dHere = p0.distance(centroid);
        double dPoke = Point2D.distance(p0.x + n0.x * 5, p0.y + n0.y * 5, centroid.x, centroid.y);
        innerSign = (dPoke < dHere) ? 1.0 : -1.0;
    }

    /** Corner-cuts a closed polygon with bounded quadratic Beziers -- safe (no new self-intersections) as long as cutFrac < 0.5. */
    private static List<Point2D.Double> roundCorners(List<Point2D.Double> pts, double cutFrac, int bezierSamples, int straightSamples) {
        int n = pts.size();
        Point2D.Double[] a = new Point2D.Double[n];
        Point2D.Double[] b = new Point2D.Double[n];
        for (int i = 0; i < n; i++) {
            Point2D.Double prev = pts.get((i - 1 + n) % n);
            Point2D.Double cur = pts.get(i);
            Point2D.Double next = pts.get((i + 1) % n);
            a[i] = lerp(cur, prev, cutFrac);
            b[i] = lerp(cur, next, cutFrac);
        }
        List<Point2D.Double> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Point2D.Double startStraight = b[(i - 1 + n) % n];
            Point2D.Double endStraight = a[i];
            for (int s = 0; s < straightSamples; s++) {
                double t = s / (double) straightSamples;
                out.add(lerp(startStraight, endStraight, t));
            }
            Point2D.Double cur = pts.get(i);
            for (int s = 0; s < bezierSamples; s++) {
                double t = s / (double) bezierSamples;
                double x = quad(a[i].x, cur.x, b[i].x, t);
                double y = quad(a[i].y, cur.y, b[i].y, t);
                out.add(new Point2D.Double(x, y));
            }
        }
        return out;
    }

    private static Point2D.Double lerp(Point2D.Double from, Point2D.Double to, double t) {
        return new Point2D.Double(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t);
    }

    private static double quad(double p0, double p1, double p2, double t) {
        double u = 1 - t;
        return u * u * p0 + 2 * u * t * p1 + t * t * p2;
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

    double signedLateralOffset(double x, double y) {
        int i = nearestIndex(x, y);
        Point2D.Double p = centerline.get(i);
        Point2D.Double nrm = normals.get(i);
        return (x - p.x) * nrm.x + (y - p.y) * nrm.y;
    }

    Point2D.Double pointAtOffset(int index, double offset) {
        Point2D.Double p = centerline.get(index);
        Point2D.Double nrm = normals.get(index);
        return new Point2D.Double(p.x + nrm.x * offset, p.y + nrm.y * offset);
    }

    /** Binary-search the (monotonic) cumulative-length array for the sample index nearest an arc-length value. */
    int indexAtArc(double arc) {
        int lo = 0, hi = cumulativeLength.length - 1;
        if (arc <= cumulativeLength[0]) return 0;
        if (arc >= cumulativeLength[hi]) return hi;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (cumulativeLength[mid] < arc) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    Point2D.Double startPosition(double laneOffset) {
        return pointAtOffset(2, laneOffset);
    }

    double startAngle() {
        Point2D.Double a = centerline.get(0);
        Point2D.Double b = centerline.get(5);
        return Math.atan2(b.y - a.y, b.x - a.x);
    }

    private Path2D.Double buildOffsetPath(double offset) {
        Path2D.Double path = new Path2D.Double();
        for (int i = 0; i < centerline.size(); i++) {
            Point2D.Double p = pointAtOffset(i, offset);
            if (i == 0) path.moveTo(p.x, p.y); else path.lineTo(p.x, p.y);
        }
        path.closePath();
        return path;
    }

    void draw(Graphics2D g, int screenW, int screenH) {
        Path2D.Double outer = buildOffsetPath(halfWidth);
        Path2D.Double inner = buildOffsetPath(-halfWidth);

        Area infieldWall = new Area(inner);
        Area outerWall = new Area(new Rectangle(0, 0, screenW, screenH));
        outerWall.subtract(new Area(outer));

        drawStrata(g, infieldWall, new Rectangle(0, 0, screenW, screenH), 11L);
        drawStrata(g, outerWall, new Rectangle(0, 0, screenW, screenH), 77L);

        Area road = new Area(outer);
        road.subtract(new Area(inner));
        g.setColor(new Color(196, 150, 96));
        g.fill(road);

        g.setColor(new Color(168, 124, 78, 90));
        for (int i = 0; i < centerline.size(); i += 5) {
            Point2D.Double o = pointAtOffset(i, halfWidth);
            Point2D.Double in = pointAtOffset(i, -halfWidth);
            g.draw(new Line2D.Double(o, in));
        }

        g.setColor(new Color(110, 80, 50));
        g.setStroke(new BasicStroke(4));
        g.draw(outer);
        g.draw(inner);
        g.setStroke(new BasicStroke(1));

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(255, 232, 190, 140));
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

    /** Sedimentary strata: literal horizontal color bands (ochre/rust/tan/gray) clipped to a wall area, with a raking-light edge highlight per band and hairline cracks. */
    private void drawStrata(Graphics2D g, Area clip, Rectangle bounds, long seed) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.clip(clip);
        Color[] palette = {
                new Color(196, 142, 84), new Color(168, 96, 64), new Color(214, 178, 120),
                new Color(140, 128, 116), new Color(150, 104, 60)
        };
        int bandH = 24;
        int startY = bounds.y - (((bounds.y % bandH) + bandH) % bandH);
        int idx = (int) Math.abs(seed % palette.length);
        for (int y = startY; y < bounds.y + bounds.height; y += bandH) {
            g2.setColor(palette[idx % palette.length]);
            g2.fillRect(bounds.x, y, bounds.width, bandH);
            g2.setColor(new Color(255, 224, 170, 55));
            g2.fillRect(bounds.x, y, bounds.width, 2);
            g2.setColor(new Color(45, 28, 16, 80));
            g2.fillRect(bounds.x, y + bandH - 2, bounds.width, 2);
            idx++;
        }
        Random r = new Random(seed);
        g2.setColor(new Color(60, 38, 22, 110));
        for (int i = 0; i < 160; i++) {
            int cx = bounds.x + r.nextInt(Math.max(1, bounds.width));
            int cy = bounds.y + r.nextInt(Math.max(1, bounds.height));
            int len = 8 + r.nextInt(26);
            int jag = r.nextInt(9) - 4;
            g2.drawLine(cx, cy, cx + jag, cy + len);
        }
        g2.dispose();
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

/**
 * Signature hazard: a narrow timber bridge spanning a gap in the canyon
 * floor. On a repeating timer it cycles STABLE -> CRUMBLING -> COLLAPSED ->
 * REBUILDING -> STABLE. While COLLAPSED or REBUILDING the central span is
 * impassable (a fall wrecks the car) unless the car is going fast enough to
 * leap the gap; narrow rocky ledges on both sides of the span remain
 * passable at all times as a slower, riskier detour.
 */
class BridgeSection {
    enum Phase { STABLE, CRUMBLING, COLLAPSED, REBUILDING }

    private static final double STABLE_T = 6.0, CRUMBLE_T = 1.2, COLLAPSED_T = 3.5, REBUILD_T = 1.5;

    final Track track;
    final int startIndex, endIndex;
    final double startArc, endArc;
    final double gapHalfWidth;

    Phase phase = Phase.STABLE;
    double phaseTimer = STABLE_T;
    private double trickleTimer = 0;

    BridgeSection(Track track, double centerArc, double halfSpanArc, double gapHalfWidth, double phaseOffset) {
        this.track = track;
        this.gapHalfWidth = gapHalfWidth;
        this.startArc = centerArc - halfSpanArc;
        this.endArc = centerArc + halfSpanArc;
        this.startIndex = track.indexAtArc(startArc);
        this.endIndex = track.indexAtArc(endArc);
        if (phaseOffset > 0) update(phaseOffset, null);
    }

    boolean containsArc(double arc) {
        return arc >= startArc && arc <= endArc;
    }

    boolean isGapImpassable() {
        return phase == Phase.COLLAPSED || phase == Phase.REBUILDING;
    }

    private static double phaseDuration(Phase p) {
        switch (p) {
            case STABLE: return STABLE_T;
            case CRUMBLING: return CRUMBLE_T;
            case COLLAPSED: return COLLAPSED_T;
            default: return REBUILD_T;
        }
    }

    private static Phase nextPhase(Phase p) {
        switch (p) {
            case STABLE: return Phase.CRUMBLING;
            case CRUMBLING: return Phase.COLLAPSED;
            case COLLAPSED: return Phase.REBUILDING;
            default: return Phase.STABLE;
        }
    }

    void update(double dt, ParticleSystem ps) {
        phaseTimer -= dt;
        while (phaseTimer <= 0) {
            phase = nextPhase(phase);
            phaseTimer += phaseDuration(phase);
            onPhaseEnter(ps);
        }
        if (phase == Phase.COLLAPSED && ps != null) {
            trickleTimer -= dt;
            if (trickleTimer <= 0) {
                trickleTimer = 0.3;
                spawnDebris(ps, 2);
            }
        }
    }

    private void onPhaseEnter(ParticleSystem ps) {
        if (ps == null) return;
        if (phase == Phase.COLLAPSED) spawnDebris(ps, 22);
        if (phase == Phase.STABLE) spawnDust(ps, 10);
    }

    private void spawnDebris(ParticleSystem ps, int count) {
        Point2D.Double c = track.pointAtOffset((startIndex + endIndex) / 2, 0);
        for (int i = 0; i < count; i++) {
            double ang = Math.random() * Math.PI * 2;
            double sp = 20 + Math.random() * 90;
            ps.add(new Particle(c.x, c.y, Math.cos(ang) * sp, Math.sin(ang) * sp - 30,
                    0.5 + Math.random() * 0.6, 3 + Math.random() * 4, 2,
                    new Color(120, 90, 60), Particle.Kind.DEBRIS));
        }
    }

    private void spawnDust(ParticleSystem ps, int count) {
        Point2D.Double c = track.pointAtOffset((startIndex + endIndex) / 2, 0);
        for (int i = 0; i < count; i++) {
            double ang = Math.random() * Math.PI * 2;
            double sp = 10 + Math.random() * 40;
            ps.add(new Particle(c.x, c.y, Math.cos(ang) * sp, Math.sin(ang) * sp,
                    0.6 + Math.random() * 0.4, 5 + Math.random() * 5, 5,
                    new Color(200, 180, 140, 150), Particle.Kind.DUST));
        }
    }

    private Path2D.Double band(double offA, double offB) {
        List<Point2D.Double> sideA = new ArrayList<>();
        List<Point2D.Double> sideB = new ArrayList<>();
        for (int i = startIndex; i <= endIndex; i++) {
            sideA.add(track.pointAtOffset(i, offA));
            sideB.add(track.pointAtOffset(i, offB));
        }
        Path2D.Double p = new Path2D.Double();
        p.moveTo(sideA.get(0).x, sideA.get(0).y);
        for (Point2D.Double pt : sideA) p.lineTo(pt.x, pt.y);
        for (int i = sideB.size() - 1; i >= 0; i--) p.lineTo(sideB.get(i).x, sideB.get(i).y);
        p.closePath();
        return p;
    }

    void draw(Graphics2D g) {
        Color ledgeColor = new Color(158, 128, 96);
        g.setColor(ledgeColor);
        g.fill(band(gapHalfWidth, track.halfWidth));
        g.fill(band(-track.halfWidth, -gapHalfWidth));
        g.setColor(ledgeColor.darker());
        g.draw(band(gapHalfWidth, track.halfWidth));
        g.draw(band(-track.halfWidth, -gapHalfWidth));

        switch (phase) {
            case STABLE: drawPlanks(g, 1f, false); break;
            case CRUMBLING: drawPlanks(g, 1f, true); break;
            case COLLAPSED: drawChasm(g, 1f); break;
            case REBUILDING:
                float built = (float) Util.clamp(1 - phaseTimer / REBUILD_T, 0, 1);
                drawChasm(g, 1f - built * 0.8f);
                drawPlanks(g, built, true);
                break;
        }
    }

    private void drawPlanks(Graphics2D g, float alpha, boolean shaky) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Util.clamp(alpha, 0, 1)));
        g.setColor(new Color(120, 84, 52));
        g.fill(band(-gapHalfWidth, gapHalfWidth));
        g.setColor(new Color(80, 54, 32));
        int planks = Math.max(2, endIndex - startIndex);
        for (int i = startIndex; i <= endIndex; i += Math.max(1, (endIndex - startIndex) / 8)) {
            double jitter = shaky ? (Math.random() - 0.5) * 4 : 0;
            Point2D.Double a = track.pointAtOffset(i, -gapHalfWidth + jitter);
            Point2D.Double b = track.pointAtOffset(i, gapHalfWidth + jitter);
            g.draw(new Line2D.Double(a, b));
        }
        if (shaky) {
            g.setColor(new Color(40, 26, 14, 160));
            for (int i = 0; i < 4; i++) {
                int idx = startIndex + (int) (Math.random() * Math.max(1, endIndex - startIndex));
                double off = (Math.random() - 0.5) * gapHalfWidth * 1.6;
                Point2D.Double p1 = track.pointAtOffset(idx, off);
                Point2D.Double p2 = track.pointAtOffset(idx, off + (Math.random() - 0.5) * 14);
                g.draw(new Line2D.Double(p1, p2));
            }
        }
        g.setComposite(old);
    }

    private void drawChasm(Graphics2D g, float alpha) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Util.clamp(alpha, 0, 1)));
        g.setColor(new Color(35, 24, 16));
        g.fill(band(-gapHalfWidth, gapHalfWidth));
        g.setColor(new Color(15, 10, 6, 200));
        g.fill(band(-gapHalfWidth * 0.5, gapHalfWidth * 0.5));
        g.setColor(new Color(70, 48, 30));
        int mid = (startIndex + endIndex) / 2;
        Point2D.Double p = track.pointAtOffset(mid, -gapHalfWidth * 0.7);
        g.fillRect((int) p.x - 6, (int) p.y - 2, 14, 4);
        g.setComposite(old);
    }
}

class Obstacle {
    enum Kind { BOULDER, ROCK_SPIRE, WRECK, TUMBLEWEED }

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
            case BOULDER:
                g.setColor(new Color(122, 90, 62));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.7));
                g.setColor(new Color(84, 60, 40));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.7));
                g.fillOval((int) (-radius * 0.3), (int) (-radius * 0.6), (int) (radius * 0.7), (int) (radius * 0.5));
                break;
            case ROCK_SPIRE:
                g.setColor(new Color(168, 118, 78));
                Path2D.Double spire = new Path2D.Double();
                spire.moveTo(0, -radius * 1.6);
                spire.lineTo(radius * 0.55, radius * 0.6);
                spire.lineTo(-radius * 0.55, radius * 0.6);
                spire.closePath();
                g.fill(spire);
                g.setColor(new Color(110, 76, 48));
                g.draw(spire);
                break;
            case WRECK:
                g.setColor(new Color(90, 66, 44));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(new Color(50, 34, 20));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) (radius * 0.3));
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                break;
            case TUMBLEWEED:
                g.setColor(new Color(150, 122, 70));
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
    private static final double OFFROAD_MULT = 2.3;
    private static final double TURN_RATE = 3.0;
    private static final double GRIP = 7.5;
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
        Color c = onTrack ? new Color(214, 190, 150, 120) : new Color(184, 150, 104, 160);
        for (int i = 0; i < (onTrack ? 1 : 2); i++) {
            double jx = (Math.random() - 0.5) * 10;
            double jy = (Math.random() - 0.5) * 10;
            particles.add(new Particle(rearX + jx, rearY + jy,
                    -vx * 0.15 + (Math.random() - 0.5) * 20, -vy * 0.15 + (Math.random() - 0.5) * 20,
                    0.5 + Math.random() * 0.4, 6 + Math.random() * 6, 8, c, Particle.Kind.DUST));
        }
        if (boosting) {
            particles.add(new Particle(rearX, rearY, -vx * 0.3, -vy * 0.3, 0.35, 10, 6,
                    new Color(255, 150, 50, 200), Particle.Kind.SPARK));
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
                    Math.random() < 0.5 ? new Color(255, 120, 40) : new Color(100, 84, 66), Particle.Kind.SMOKE));
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
        g.setColor(new Color(60, 60, 60));
        g.drawLine(-10, -11, -10, 11);
        g.drawLine(2, -11, 2, 11);
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

    InputState think(double dt, List<Obstacle> obstacles, List<BridgeSection> bridges) {
        InputState in = new InputState();
        int idx = track.nearestIndex(car.x, car.y);
        int lookahead = 16;
        int aheadIdx = (idx + lookahead) % track.centerline.size();
        Point2D.Double target = track.centerline.get(aheadIdx);
        double throttleScale = 1.0;

        for (BridgeSection br : bridges) {
            double aheadArc = track.cumulativeLength[aheadIdx];
            if (br.containsArc(aheadArc) && br.isGapImpassable()) {
                double curLateral = track.signedLateralOffset(car.x, car.y);
                double sign = curLateral >= 0 ? 1 : -1;
                double ledgeOffset = sign * (br.gapHalfWidth + (track.halfWidth - br.gapHalfWidth) * 0.5);
                target = track.pointAtOffset(aheadIdx, ledgeOffset);
                throttleScale = 0.65;
                break;
            }
        }

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

        in.steer = Util.clamp(diff * 1.6, -1, 1);
        in.throttle = (Math.abs(diff) > 1.3 ? 0.2 : 1.0) * throttleScale;

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
    private static final double JUMP_SPEED_THRESHOLD = 260;

    private enum State { MENU, COUNTDOWN, RACING, FINISHED }

    private State state = State.MENU;
    private boolean vsAI = true;
    private Thread thread;
    private volatile boolean running = true;

    private final Set<Integer> keys = new HashSet<>();
    private Track track;
    private List<Car> cars = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<BridgeSection> bridges = new ArrayList<>();
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
        track = new Track(70);
        int n = track.centerline.size();

        bridges.clear();
        bridges.add(new BridgeSection(track, track.totalLength * 0.30, 34, 38, 0.0));
        bridges.add(new BridgeSection(track, track.totalLength * 0.72, 34, 38, 5.0));

        obstacles.clear();
        Random r = new Random(42);
        for (int i = 0; i < n; i += 12) {
            if (nearAnyBridge(i)) continue;
            if (r.nextDouble() < 0.5) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 20 + r.nextDouble() * 55;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.BOULDER : Obstacle.Kind.ROCK_SPIRE;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(10)));
            }
        }
        int[] hazardIdx = {50, 130, 220, 300, 380};
        for (int idx : hazardIdx) {
            if (idx >= n || nearAnyBridge(idx)) continue;
            double off = (r.nextDouble() - 0.5) * track.halfWidth * 0.9;
            Point2D.Double p = track.pointAtOffset(idx, off);
            obstacles.add(new Obstacle(Obstacle.Kind.WRECK, p.x, p.y, 15));
        }
        for (int i = 0; i < 3; i++) {
            Obstacle t = new Obstacle(Obstacle.Kind.TUMBLEWEED, r.nextInt(W), r.nextInt(H), 12);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 40 + r.nextDouble() * 40;
            t.vx = Math.cos(a) * sp;
            t.vy = Math.sin(a) * sp;
            obstacles.add(t);
        }
    }

    private boolean nearAnyBridge(int idx) {
        for (BridgeSection b : bridges) {
            if (idx >= b.startIndex - 15 && idx <= b.endIndex + 15) return true;
        }
        return false;
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-22);
        Point2D.Double p2pos = track.startPosition(22);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(200, 90, 40), new Color(230, 210, 170), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(70, 120, 130), new Color(210, 220, 210), vsAI ? "AI" : "2", particles);
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
        for (BridgeSection b : bridges) b.update(dt, particles);

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
            in2 = aiDriver.think(dt, obstacles, bridges);
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
        checkBridgeFalls(p1);
        checkBridgeFalls(p2);

        Point2D.Double startPt = track.startPosition(0);
        if (p1.justLapped) spawnLapBurst(startPt);
        if (p2.justLapped) spawnLapBurst(startPt);

        for (Car c : cars) {
            if (!c.finished && c.lap >= TOTAL_LAPS) {
                c.finished = true;
                c.finishTime = raceTime;
            }
        }
        if (p1.finished && p2.finished) state = State.FINISHED;
    }

    private void spawnLapBurst(Point2D.Double p) {
        for (int i = 0; i < 20; i++) {
            double ang = Math.random() * Math.PI * 2;
            double sp = 60 + Math.random() * 140;
            particles.add(new Particle(p.x, p.y - 8, Math.cos(ang) * sp, Math.sin(ang) * sp - 30,
                    0.5 + Math.random() * 0.4, 4 + Math.random() * 4, 3,
                    new Color(255, 210, 90), Particle.Kind.SPARK));
        }
    }

    /** Signature-hazard collision: fall into a collapsed bridge gap unless crossing fast enough to jump it. */
    private void checkBridgeFalls(Car c) {
        if (!c.alive) return;
        double arc = track.progress(c.x, c.y);
        for (BridgeSection b : bridges) {
            if (!b.containsArc(arc)) continue;
            double lateral = track.signedLateralOffset(c.x, c.y);
            if (Math.abs(lateral) > b.gapHalfWidth) continue; // safely on the side ledge
            if (!b.isGapImpassable()) continue; // bridge currently solid
            if (Math.abs(c.speed) >= JUMP_SPEED_THRESHOLD) continue; // leapt the gap successfully
            int behindIdx = ((b.startIndex - 18) % track.centerline.size() + track.centerline.size()) % track.centerline.size();
            Point2D.Double safe = track.centerline.get(behindIdx);
            c.x = safe.x; c.y = safe.y;
            c.speed = 0; c.vx = 0; c.vy = 0;
            c.damage(999);
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
                        new Color(255, 200, 80), Particle.Kind.SPARK));
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
        track.draw(sg, W, H);
        for (Obstacle o : obstacles) o.draw(sg);
        for (BridgeSection b : bridges) b.draw(sg);
        particles.draw(sg);
        for (Car c : cars) c.draw(sg);
        applyWarmLight(sg);
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

    /** Warm directional light raking across the canyon rock. */
    private void applyWarmLight(Graphics2D g) {
        Paint old = g.getPaint();
        g.setPaint(new GradientPaint(0, 0, new Color(255, 220, 160, 55), W, H, new Color(40, 20, 10, 0)));
        g.fillRect(0, 0, W, H);
        g.setPaint(old);
    }

    private void applyDustHaze(BufferedImage img) {
        int top = (int) (H * 0.30), bandH = 46;
        for (int y = top; y < top + bandH && y < H; y++) {
            double t = (y - top) / (double) bandH;
            int shift = (int) (Math.sin(y * 0.22 + timeAccum * 3.0) * 3 * (1 - t));
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
        g.setPaint(new GradientPaint(0, 0, new Color(240, 176, 110), 0, (float) (H * 0.32), new Color(226, 196, 158)));
        g.fillRect(0, 0, W, (int) (H * 0.32));

        double pulse = 1 + 0.03 * Math.sin(timeAccum * 1.5);
        int sunR = (int) (50 * pulse);
        g.setPaint(new RadialGradientPaint(new Point(W - 190, 100), sunR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(255, 240, 210, 210), new Color(240, 180, 120, 90), new Color(240, 180, 120, 0)}));
        g.fillOval(W - 190 - sunR * 3, 100 - sunR * 3, sunR * 6, sunR * 6);
        g.setColor(new Color(255, 245, 220));
        g.fillOval(W - 190 - sunR / 2, 100 - sunR / 2, sunR, sunR);

        g.setColor(new Color(150, 108, 74));
        Path2D.Double rim = new Path2D.Double();
        rim.moveTo(-50, H * 0.32);
        double wx = -50;
        Random wr = new Random(7);
        boolean up = true;
        while (wx < W + 50) {
            double wy = up ? H * 0.32 - (55 + wr.nextInt(35)) : H * 0.32 - (18 + wr.nextInt(18));
            rim.lineTo(wx, wy);
            wx += 20 + wr.nextInt(16);
            up = !up;
        }
        rim.lineTo(W + 50, H * 0.32);
        rim.closePath();
        g.fill(rim);

        g.setPaint(new GradientPaint(0, (float) (H * 0.30), new Color(206, 172, 128), 0, H, new Color(150, 118, 82)));
        g.fillRect(0, (int) (H * 0.30), W, (int) (H * 0.70));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(224, 150, 70));
        g.setFont(new Font("SansSerif", Font.BOLD, 52));
        centerText(g, "BEDROCK CANYON RUN", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Winding canyon loop -- watch for the collapsing bridges", H / 2 - 90);

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

        for (Car c : cars) {
            double arc = track.progress(c.x, c.y);
            for (BridgeSection b : bridges) {
                double dist = Math.min(Math.abs(arc - b.startArc), Math.abs(arc - b.endArc));
                if (b.containsArc(arc) || dist < 90) {
                    if (b.isGapImpassable()) {
                        g.setColor(((int) (timeAccum * 4) % 2 == 0) ? new Color(255, 70, 40) : new Color(200, 40, 20));
                        g.setFont(new Font("SansSerif", Font.BOLD, 18));
                        centerText(g, "BRIDGE OUT -- TAKE THE LEDGE OR JUMP IT", 54);
                    }
                }
            }
        }
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
