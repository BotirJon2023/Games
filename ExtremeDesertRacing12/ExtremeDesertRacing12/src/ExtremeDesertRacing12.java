import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ExtremeDesertRacing12 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Solar Flare Speedway — Extreme Desert Racing");
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

    static double lerp(double a, double b, double t) {
        return a + (b - a) * clamp(t, 0, 1);
    }

    static Color lerpColor(Color a, Color b, double t) {
        double ct = clamp(t, 0, 1);
        int r = (int) lerp(a.getRed(), b.getRed(), ct);
        int gg = (int) lerp(a.getGreen(), b.getGreen(), ct);
        int bb = (int) lerp(a.getBlue(), b.getBlue(), ct);
        int aa = (int) lerp(a.getAlpha(), b.getAlpha(), ct);
        return new Color(r, gg, bb, aa);
    }

    static int clampAlpha(int v) {
        return (int) clamp(v, 0, 255);
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

    enum ParticleKind { DUST, SMOKE, SPARK, DEBRIS, FLARE }

    Particle(double x, double y, double vx, double vy, double life, double size, double growth, Color color, ParticleKind kind) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.life = life; this.maxLife = life;
        this.size = size; this.growth = growth;
        this.color = color; this.kind = kind;
    }

    boolean update(double dt) {
        x += vx * dt;
        y += vy * dt;
        if (kind == ParticleKind.FLARE) {
            vx *= 0.97;
            vy *= 0.97;
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

/** MUST stay thread-safe: physics thread and Swing EDT both touch this. */
class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();

    synchronized void add(Particle p) { particles.add(p); }

    synchronized void update(double dt) {
        particles.removeIf(p -> !p.update(dt));
    }

    void spawnFlareBurst(double x, double y) {
        Random r = new Random();
        for (int i = 0; i < 22; i++) {
            double ang = Math.random() * Math.PI * 2;
            double sp = 60 + r.nextDouble() * 160;
            Color c = r.nextBoolean() ? new Color(255, 245, 190) : new Color(255, 210, 90);
            add(new Particle(x, y, Math.cos(ang) * sp, Math.sin(ang) * sp,
                    0.4 + r.nextDouble() * 0.4, 4 + r.nextDouble() * 5, 3, c, Particle.ParticleKind.FLARE));
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
 * "Solar Flare Speedway" track: a superellipse rounded rectangle,
 * |x/a|^n + |y/b|^n = 1, with a wide aspect ratio (a >> b) so the two long
 * sides (near t = pi/2 and t = 3pi/2 in this parameterization) form long
 * exposed straights, and a moderate exponent n giving softer corners than a
 * sharp rounded square. One simple, non-self-intersecting closed loop.
 *
 * Same proven lap-counting/rendering technique as the reference engine: a
 * centerline point list with a parallel cumulative arc-length array (robust
 * signed progress with wraparound) plus per-point normals for offset-based
 * inner/outer edges and a signed lateral-offset query for hazard bands.
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

        int refIdx = samples / 4;
        Point2D.Double p = centerline.get(refIdx);
        Point2D.Double nrm = normals.get(refIdx);
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

    /** Signed lateral offset (along the local normal) from the centerline; used by flare-zone exposed-lane checks. */
    double signedOffset(double x, double y) {
        int i = nearestIndex(x, y);
        Point2D.Double p = centerline.get(i);
        Point2D.Double nrm = normals.get(i);
        return (x - p.x) * nrm.x + (y - p.y) * nrm.y;
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

    /** Wraparound-aware check of whether centerline index idx lies within [startIdx,endIdx] (may wrap past n). */
    boolean indexInRange(int idx, int startIdx, int endIdx) {
        int n = centerline.size();
        int s = ((startIdx % n) + n) % n;
        int e = ((endIdx % n) + n) % n;
        int rel = ((idx - s) % n + n) % n;
        int span = ((e - s) % n + n) % n;
        return rel <= span;
    }

    void draw(Graphics2D g, double duskFactor) {
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
        Color asphalt = Util.lerpColor(new Color(38, 40, 48), new Color(30, 26, 34), duskFactor);
        g.setColor(asphalt);
        g.fill(road);

        // Faint panel-seam expansion joints across the asphalt.
        g.setColor(new Color(20, 22, 28, 150));
        for (int i = 0; i < centerline.size(); i += 6) {
            Point2D.Double o = pointAtOffset(i, halfWidth);
            Point2D.Double in = pointAtOffset(i, -halfWidth);
            g.draw(new Line2D.Double(o, in));
        }

        // Glowing panel-blue edge lines: layered strokes, widest+dimmest first, for a soft glow.
        Color edgeGlow = Util.lerpColor(new Color(70, 200, 255), new Color(255, 150, 90), duskFactor * 0.6);
        int[] widths = {9, 6, 3};
        int[] alphas = {40, 90, 220};
        for (int wi = 0; wi < widths.length; wi++) {
            g.setStroke(new BasicStroke(widths[wi], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(edgeGlow.getRed(), edgeGlow.getGreen(), edgeGlow.getBlue(), alphas[wi]));
            g.draw(outer);
            g.draw(inner);
        }
        g.setStroke(new BasicStroke(1));

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(210, 220, 230, 130));
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
            g.setColor(i % 2 == 0 ? Color.WHITE : new Color(20, 20, 24));
            g.fill(new Rectangle2D.Double(sx - 4, sy - 4, 8, 8));
        }
    }
}

class Obstacle {
    enum Kind { PANEL_SHARD, PYLON, DRONE_WRECK, SPINNING_PANEL }

    double x, y, radius, rotation;
    Kind kind;
    double vx, vy, spin;

    Obstacle(Kind kind, double x, double y, double radius) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.rotation = Math.random() * Math.PI * 2;
        if (kind == Kind.SPINNING_PANEL) this.spin = 4 + Math.random() * 3;
    }

    void update(double dt, double minX, double maxX, double minY, double maxY) {
        if (kind == Kind.SPINNING_PANEL) {
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
            case PANEL_SHARD:
                g.setColor(new Color(40, 60, 80));
                g.fillRoundRect((int) -radius, (int) (-radius * 0.6), (int) (radius * 2), (int) (radius * 1.2), 3, 3);
                g.setColor(new Color(80, 190, 230, 160));
                g.drawRoundRect((int) -radius, (int) (-radius * 0.6), (int) (radius * 2), (int) (radius * 1.2), 3, 3);
                g.drawLine((int) -radius, 0, (int) radius, 0);
                break;
            case PYLON:
                g.setColor(new Color(150, 150, 158));
                g.fillRoundRect((int) (-radius * 0.5), (int) -radius, (int) (radius), (int) (radius * 2), 5, 5);
                g.setColor(new Color(100, 100, 108));
                for (int i = -1; i <= 1; i++) {
                    g.drawLine((int) (-radius * 0.4), (int) (i * radius * 0.6), (int) (radius * 0.4), (int) (i * radius * 0.6));
                }
                break;
            case DRONE_WRECK:
                g.setColor(new Color(54, 56, 62));
                g.fillRoundRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius, 6, 6);
                g.setColor(new Color(200, 60, 40));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) (radius * 0.25));
                g.setColor(new Color(20, 20, 20));
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                break;
            case SPINNING_PANEL:
                g.setColor(new Color(45, 65, 90));
                g.fillRect((int) -radius, (int) (-radius * 0.15), (int) (radius * 2), (int) (radius * 0.3));
                g.setColor(new Color(90, 210, 255, 200));
                g.drawRect((int) -radius, (int) (-radius * 0.15), (int) (radius * 2), (int) (radius * 0.3));
                break;
        }
        g.setTransform(old);
    }
}

/** Purely decorative rows of neon-accented solar panel arrays flanking the track: no collision. */
class PanelArray {
    final double x, y, rotation, scale;
    final int rows, cols;

    PanelArray(double x, double y, double rotation, double scale, int rows, int cols) {
        this.x = x; this.y = y; this.rotation = rotation; this.scale = scale;
        this.rows = rows; this.cols = cols;
    }

    void draw(Graphics2D g, double duskFactor, double t) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(rotation);
        g.scale(scale, scale);

        double cellW = 16, cellH = 10, gap = 3;
        double totalW = cols * (cellW + gap);
        double totalH = rows * (cellH + gap);

        g.setColor(new Color(30, 34, 42, 210));
        g.fillRoundRect((int) (-totalW / 2 - 4), (int) (-totalH / 2 - 4), (int) (totalW + 8), (int) (totalH + 8), 4, 4);

        Color accent = Util.lerpColor(new Color(60, 190, 255), new Color(255, 140, 70), duskFactor);
        double pulse = 0.55 + 0.45 * Math.sin(t * 1.4 + x * 0.01);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double px = -totalW / 2 + c * (cellW + gap);
                double py = -totalH / 2 + r * (cellH + gap);
                g.setColor(new Color(18, 46, 60));
                g.fillRect((int) px, (int) py, (int) cellW, (int) cellH);
                g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (150 * pulse)));
                g.drawRect((int) px, (int) py, (int) cellW, (int) cellH);
                g.drawLine((int) px, (int) (py + cellH / 2), (int) (px + cellW), (int) (py + cellH / 2));
            }
        }
        // support strut
        g.setColor(new Color(60, 62, 68));
        g.fillRect(-3, (int) (totalH / 2 + 4), 6, 14);
        g.setTransform(old);
    }
}

/**
 * Signature hazard: a "Solar Flare Pulse" hazard band across an exposed
 * straight section. Cycles DORMANT -> TELEGRAPH (visible warning glow along
 * the exposed lane, giving players time to react) -> ACTIVE (expanding
 * bright radial flash; cars caught in the exposed lane band take damage and
 * a temporary visibility-reducing glare) -> COOLDOWN -> back to DORMANT.
 * The exposed lane only covers part of the track width, leaving a narrower
 * safe strip along one edge for players who time it right.
 */
class FlareZone {
    enum Phase { DORMANT, TELEGRAPH, ACTIVE, COOLDOWN }

    final int startIndex, endIndex;
    final double laneMin, laneMax; // signed offset range (relative to centerline) that is exposed
    Phase phase = Phase.DORMANT;
    double phaseTimer;
    boolean justActivated;

    private static final double TELEGRAPH_DUR = 1.8;
    private static final double ACTIVE_DUR = 1.15;
    private static final double COOLDOWN_DUR = 1.6;

    FlareZone(int startIndex, int endIndex, double laneMin, double laneMax, double initialDormant) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.laneMin = laneMin;
        this.laneMax = laneMax;
        this.phaseTimer = initialDormant;
    }

    void update(double dt) {
        justActivated = false;
        phaseTimer -= dt;
        if (phaseTimer > 0) return;
        switch (phase) {
            case DORMANT:
                phase = Phase.TELEGRAPH;
                phaseTimer = TELEGRAPH_DUR;
                break;
            case TELEGRAPH:
                phase = Phase.ACTIVE;
                phaseTimer = ACTIVE_DUR;
                justActivated = true;
                break;
            case ACTIVE:
                phase = Phase.COOLDOWN;
                phaseTimer = COOLDOWN_DUR;
                break;
            case COOLDOWN:
                phase = Phase.DORMANT;
                phaseTimer = 6.0 + Math.random() * 6.0;
                break;
        }
    }

    boolean isActive() { return phase == Phase.ACTIVE; }

    boolean coversOffset(double offset) { return offset >= laneMin && offset <= laneMax; }

    double telegraphFraction() {
        if (phase != Phase.TELEGRAPH) return 0;
        return 1 - Util.clamp(phaseTimer / TELEGRAPH_DUR, 0, 1);
    }

    double activeFraction() {
        if (phase != Phase.ACTIVE) return 0;
        return 1 - Util.clamp(phaseTimer / ACTIVE_DUR, 0, 1);
    }

    void draw(Graphics2D g, Track track) {
        if (phase == Phase.DORMANT || phase == Phase.COOLDOWN) return;
        int n = track.centerline.size();
        Path2D.Double band = new Path2D.Double();
        boolean first = true;
        for (int idx = startIndex; ; idx = (idx + 1) % n) {
            Point2D.Double p = track.pointAtOffset(idx, laneMax);
            if (first) { band.moveTo(p.x, p.y); first = false; } else band.lineTo(p.x, p.y);
            if (idx == endIndex) break;
        }
        for (int idx = endIndex; ; idx = (idx - 1 + n) % n) {
            Point2D.Double p = track.pointAtOffset(idx, laneMin);
            band.lineTo(p.x, p.y);
            if (idx == startIndex) break;
        }
        band.closePath();

        if (phase == Phase.TELEGRAPH) {
            double f = telegraphFraction();
            double pulse = 0.5 + 0.5 * Math.sin(f * Math.PI * 6);
            int alpha = (int) (70 + 120 * f * pulse);
            g.setColor(new Color(255, 90, 40, Util.clampAlpha(alpha)));
            g.fill(band);
            g.setStroke(new BasicStroke(2 + (float) (2 * f)));
            g.setColor(new Color(255, 200, 60, Util.clampAlpha((int) (120 + 100 * pulse))));
            g.draw(band);
            g.setStroke(new BasicStroke(1));
        } else if (phase == Phase.ACTIVE) {
            double f = activeFraction();
            int alpha = (int) (220 * (1 - f * 0.4));
            g.setColor(new Color(255, 250, 220, Util.clampAlpha(alpha)));
            g.fill(band);

            // expanding radial flash centered on the zone midpoint
            int midIdx = ((startIndex + ((endIndex - startIndex + n) % n) / 2) % n);
            Point2D.Double mid = track.centerline.get(midIdx);
            double maxR = 260;
            double r = 30 + maxR * f;
            float[] fractions = {0f, 0.5f, 1f};
            Color[] colors = {
                    new Color(255, 255, 255, 230),
                    new Color(255, 235, 150, (int) (160 * (1 - f))),
                    new Color(255, 235, 150, 0)
            };
            try {
                g.setPaint(new RadialGradientPaint(new Point2D.Double(mid.x, mid.y), (float) Math.max(4, r), fractions, colors));
                g.fill(new Ellipse2D.Double(mid.x - r, mid.y - r, r * 2, r * 2));
            } catch (IllegalArgumentException ignored) {
                // degenerate radius edge case; skip this frame's flash
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
    double flareExposure = 0; // 0..1 visibility-reducing glare from being caught in a flare

    final Color bodyColor;
    final Color trimColor;
    final String label;
    final ParticleSystem particles;

    private static final double MAX_SPEED = 440;
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
        flareExposure = Math.max(0, flareExposure - dt * 0.6);
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
        Color c = onTrack ? new Color(210, 210, 220, 90) : new Color(180, 160, 120, 160);
        for (int i = 0; i < (onTrack ? 1 : 2); i++) {
            double jx = (Math.random() - 0.5) * 10;
            double jy = (Math.random() - 0.5) * 10;
            particles.add(new Particle(rearX + jx, rearY + jy,
                    -vx * 0.15 + (Math.random() - 0.5) * 20, -vy * 0.15 + (Math.random() - 0.5) * 20,
                    0.5 + Math.random() * 0.4, 6 + Math.random() * 6, 8, c, Particle.ParticleKind.DUST));
        }
        if (boosting) {
            particles.add(new Particle(rearX, rearY, -vx * 0.3, -vy * 0.3, 0.35, 10, 6,
                    new Color(90, 200, 255, 200), Particle.ParticleKind.SPARK));
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
                    Math.random() < 0.5 ? new Color(255, 140, 40) : new Color(80, 80, 90), Particle.ParticleKind.SMOKE));
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
            g.setColor(new Color(90, 200, 255, 200));
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

    InputState think(double dt, List<Obstacle> obstacles, List<FlareZone> flares) {
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

        boolean boostThroughFlare = false;
        for (FlareZone fz : flares) {
            if (fz.phase == FlareZone.Phase.TELEGRAPH || fz.phase == FlareZone.Phase.ACTIVE) {
                if (track.indexInRange(idx, fz.startIndex, fz.endIndex) ||
                        track.indexInRange((idx + lookahead) % track.centerline.size(), fz.startIndex, fz.endIndex)) {
                    double offset = track.signedOffset(car.x, car.y);
                    if (fz.coversOffset(offset)) {
                        // steer toward the safe strip (just past laneMax) rather than deeper into the lane
                        double safeOffset = fz.laneMax + 12;
                        Point2D.Double safePt = track.pointAtOffset((idx + 6) % track.centerline.size(), safeOffset);
                        double angToSafe = Util.normalizeAngle(Math.atan2(safePt.y - car.y, safePt.x - car.x) - car.angle);
                        diff = angToSafe;
                    } else if (fz.phase == FlareZone.Phase.ACTIVE) {
                        boostThroughFlare = true;
                    }
                }
            }
        }

        in.steer = Util.clamp(diff * 1.6, -1, 1);
        in.throttle = Math.abs(diff) > 1.3 ? 0.2 : 1.0;

        boostCooldown -= dt;
        if (boostThroughFlare && boostCooldown <= 0) {
            in.boost = true;
            boostCooldown = 1.0;
        } else if (Math.abs(diff) < 0.25 && boostCooldown <= 0 && Math.random() < 0.01) {
            in.boost = true;
            boostCooldown = 1.5;
        }
        return in;
    }
}

class GamePanel extends JPanel implements Runnable, KeyListener {
    private static final int W = 1280, H = 720;
    private static final int TOTAL_LAPS = 3;
    private static final double DUSK_TRANSITION_SECONDS = 100.0;
    private static final double FLARE_DAMAGE_PER_SEC = 26.0;

    private enum State { MENU, COUNTDOWN, RACING, FINISHED }

    private State state = State.MENU;
    private boolean vsAI = true;
    private Thread thread;
    private volatile boolean running = true;

    private final Set<Integer> keys = new HashSet<>();
    private Track track;
    private List<Car> cars = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<PanelArray> panelArrays = new ArrayList<>();
    private final List<FlareZone> flareZones = new ArrayList<>();
    private ParticleSystem particles = new ParticleSystem();
    private AIDriver aiDriver;
    private double globalFlareFlash = 0;

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
        track = new Track(W / 2.0, H / 2.0 + 10, 480, 190, 3.5, 92);
        int n = track.centerline.size();

        obstacles.clear();
        Random r = new Random(42);
        for (int i = 0; i < n; i += 11) {
            if (r.nextDouble() < 0.5) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 20 + r.nextDouble() * 55;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.PANEL_SHARD : Obstacle.Kind.PYLON;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(10)));
            }
        }
        int[] hazardIdx = {40, 120, 200, 280, 360, 440, 520};
        for (int idx : hazardIdx) {
            if (idx >= n) continue;
            double off = (r.nextDouble() - 0.5) * track.trackWidth * 0.5;
            Point2D.Double p = track.pointAtOffset(idx, off);
            obstacles.add(new Obstacle(Obstacle.Kind.DRONE_WRECK, p.x, p.y, 15));
        }
        for (int i = 0; i < 3; i++) {
            Obstacle d = new Obstacle(Obstacle.Kind.SPINNING_PANEL, r.nextInt(W), r.nextInt(H), 13);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 40 + r.nextDouble() * 40;
            d.vx = Math.cos(a) * sp;
            d.vy = Math.sin(a) * sp;
            obstacles.add(d);
        }

        // Rows of neon solar panel arrays flanking the outer rim.
        panelArrays.clear();
        Random pr = new Random(99);
        for (int i = 0; i < n; i += 14) {
            Point2D.Double p = track.pointAtOffset(i, -track.innerSign * (track.halfWidth + 70 + pr.nextInt(30)));
            Point2D.Double p2 = track.centerline.get((i + 3) % n);
            Point2D.Double p1 = track.centerline.get(i);
            double rot = Math.atan2(p2.y - p1.y, p2.x - p1.x) + Math.PI / 2;
            double scale = 0.85 + pr.nextDouble() * 0.4;
            panelArrays.add(new PanelArray(p.x, p.y, rot, scale, 3, 4));
        }

        // Two "Solar Flare Pulse" hazard zones on the long straights (near t = pi/2 and 3pi/2).
        flareZones.clear();
        int quarter = n / 4;
        int threeQuarter = (3 * n) / 4;
        int half = 42;
        double laneMin = -track.halfWidth * 0.85;
        double laneMax = track.halfWidth * 0.15;
        flareZones.add(new FlareZone(wrap(quarter - half, n), wrap(quarter + half, n), laneMin, laneMax, 3.0));
        flareZones.add(new FlareZone(wrap(threeQuarter - half, n), wrap(threeQuarter + half, n), laneMin, laneMax, 8.0));
    }

    private int wrap(int idx, int n) { return ((idx % n) + n) % n; }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-22);
        Point2D.Double p2pos = track.startPosition(22);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(220, 70, 40), new Color(255, 210, 90), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(50, 120, 190), new Color(200, 230, 255), vsAI ? "AI" : "2", particles);
        p1.lastArcLen = track.progress(p1.x, p1.y);
        p2.lastArcLen = track.progress(p2.x, p2.y);
        cars.add(p1);
        cars.add(p2);
        aiDriver = vsAI ? new AIDriver(p2, track) : null;
        for (FlareZone fz : flareZones) {
            fz.phase = FlareZone.Phase.DORMANT;
            fz.phaseTimer = 3.0 + Math.random() * 5.0;
        }
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
        globalFlareFlash = Math.max(0, globalFlareFlash - dt * 2.5);

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

        for (FlareZone fz : flareZones) {
            fz.update(dt);
            if (fz.justActivated) {
                globalFlareFlash = 1.0;
                Point2D.Double mid = track.centerline.get(wrap((fz.startIndex + fz.endIndex) / 2, track.centerline.size()));
                particles.spawnFlareBurst(mid.x, mid.y);
            }
        }

        InputState in1 = new InputState();
        in1.throttle = keys.contains(KeyEvent.VK_W) ? 1 : keys.contains(KeyEvent.VK_S) ? -1 : 0;
        in1.steer = keys.contains(KeyEvent.VK_A) ? -1 : keys.contains(KeyEvent.VK_D) ? 1 : 0;
        in1.boost = keys.contains(KeyEvent.VK_SPACE);

        InputState in2;
        if (vsAI) {
            in2 = aiDriver.think(dt, obstacles, flareZones);
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
        applyFlareExposure(p1, dt);
        applyFlareExposure(p2, dt);

        Point2D.Double startPt = track.startPosition(0);
        if (p1.justLapped) particles.spawnFlareBurst(startPt.x, startPt.y);
        if (p2.justLapped) particles.spawnFlareBurst(startPt.x, startPt.y);

        for (Car c : cars) {
            if (!c.finished && c.lap >= TOTAL_LAPS) {
                c.finished = true;
                c.finishTime = raceTime;
            }
        }
        if (p1.finished && p2.finished) state = State.FINISHED;
    }

    /** Signature-mechanic damage/visibility hook: cars caught in an ACTIVE flare's exposed lane take damage and glare. */
    private void applyFlareExposure(Car c, double dt) {
        if (!c.alive) return;
        int idx = track.nearestIndex(c.x, c.y);
        double offset = track.signedOffset(c.x, c.y);
        for (FlareZone fz : flareZones) {
            if (!fz.isActive()) continue;
            if (track.indexInRange(idx, fz.startIndex, fz.endIndex) && fz.coversOffset(offset)) {
                c.damage(FLARE_DAMAGE_PER_SEC * dt);
                c.flareExposure = Math.min(1, c.flareExposure + dt * 3.2);
            }
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
            if (o.kind == Obstacle.Kind.SPINNING_PANEL) {
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
        double duskFactor = Util.clamp(raceTime / DUSK_TRANSITION_SECONDS, 0, 1);
        Graphics2D sg = sceneBuffer.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBackground(sg, duskFactor);
        for (PanelArray pa : panelArrays) pa.draw(sg, duskFactor, timeAccum);
        track.draw(sg, duskFactor);
        for (FlareZone fz : flareZones) fz.draw(sg, track);
        for (Obstacle o : obstacles) o.draw(sg);
        particles.draw(sg);
        for (Car c : cars) c.draw(sg);
        sg.dispose();

        Graphics2D g = (Graphics2D) g0;
        g.drawImage(sceneBuffer, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGlareOverlay(g);
        if (globalFlareFlash > 0) {
            g.setColor(new Color(255, 255, 255, Util.clampAlpha((int) (200 * globalFlareFlash))));
            g.fillRect(0, 0, W, H);
        }

        switch (state) {
            case MENU: drawMenu(g); break;
            case COUNTDOWN: drawCountdown(g); break;
            case RACING: drawHud(g); break;
            case FINISHED: drawFinish(g); break;
        }
    }

    /** Per-car "caught in the flare" visibility-reducing bright glare washing in from the screen edges. */
    private void drawGlareOverlay(Graphics2D g) {
        double exposure = 0;
        for (Car c : cars) exposure = Math.max(exposure, c.flareExposure);
        if (exposure <= 0.01) return;
        float[] fractions = {0f, 0.55f, 1f};
        Color[] colors = {
                new Color(255, 255, 245, 0),
                new Color(255, 250, 220, Util.clampAlpha((int) (90 * exposure))),
                new Color(255, 255, 255, Util.clampAlpha((int) (230 * exposure)))
        };
        try {
            RadialGradientPaint p = new RadialGradientPaint(new Point(W / 2, H / 2), (float) (W * 0.7), fractions, colors);
            g.setPaint(p);
            g.fillRect(0, 0, W, H);
        } catch (IllegalArgumentException ignored) {
            // degenerate paint parameters; skip overlay this frame
        }
    }

    private void drawBackground(Graphics2D g, double duskFactor) {
        Color skyTop = Util.lerpColor(new Color(120, 170, 220), new Color(60, 40, 70), duskFactor);
        Color skyBottom = Util.lerpColor(new Color(235, 200, 150), new Color(200, 110, 70), duskFactor);
        g.setPaint(new GradientPaint(0, 0, skyTop, 0, (float) (H * 0.42), skyBottom));
        g.fillRect(0, 0, W, (int) (H * 0.42));

        double sunHeight = Util.lerp(90, 220, duskFactor);
        Color sunCore = Util.lerpColor(new Color(255, 250, 225), new Color(255, 150, 90), duskFactor);
        Color sunGlow = Util.lerpColor(new Color(255, 235, 190, 110), new Color(255, 120, 70, 110), duskFactor);
        double pulse = 1 + 0.03 * Math.sin(timeAccum * 1.5);
        int sunR = (int) (55 * pulse);
        int sunX = W - 220;
        int sunY = (int) sunHeight;
        g.setPaint(new RadialGradientPaint(new Point(sunX, sunY), sunR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{sunGlow, new Color(sunGlow.getRed(), sunGlow.getGreen(), sunGlow.getBlue(), 60), new Color(sunGlow.getRed(), sunGlow.getGreen(), sunGlow.getBlue(), 0)}));
        g.fillOval(sunX - sunR * 3, sunY - sunR * 3, sunR * 6, sunR * 6);
        g.setColor(sunCore);
        g.fillOval(sunX - sunR / 2, sunY - sunR / 2, sunR, sunR);

        // Distant horizon silhouette of far-off panel-array rooftops.
        g.setColor(Util.lerpColor(new Color(70, 80, 95), new Color(55, 40, 55), duskFactor));
        Path2D.Double roof = new Path2D.Double();
        roof.moveTo(-50, H * 0.42);
        double wx = -50;
        Random wr = new Random(7);
        boolean up = true;
        while (wx < W + 50) {
            double wy = up ? H * 0.42 - (36 + wr.nextInt(18)) : H * 0.42 - (10 + wr.nextInt(10));
            roof.lineTo(wx, wy);
            wx += 22 + wr.nextInt(16);
            up = !up;
        }
        roof.lineTo(W + 50, H * 0.42);
        roof.closePath();
        g.fill(roof);

        Color groundTop = Util.lerpColor(new Color(70, 62, 58), new Color(45, 34, 38), duskFactor);
        Color groundBottom = Util.lerpColor(new Color(40, 36, 34), new Color(24, 18, 22), duskFactor);
        g.setPaint(new GradientPaint(0, (float) (H * 0.4), groundTop, 0, H, groundBottom));
        g.fillRect(0, (int) (H * 0.4), W, (int) (H * 0.6));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(90, 210, 255));
        g.setFont(new Font("SansSerif", Font.BOLD, 50));
        centerText(g, "SOLAR FLARE SPEEDWAY", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Neon solar-farm desert raceway — time your run through the flare pulses", H / 2 - 90);

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
        g.setColor(new Color(90, 210, 255));
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
