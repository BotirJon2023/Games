import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ExtremeDesertRacing11 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Scrapyard Sands 500");
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

    static double smoothstep(double edge0, double edge1, double x) {
        double t = clamp((x - edge0) / (edge1 - edge0), 0, 1);
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

    enum ParticleKind { DUST, SMOKE, SPARK, DEBRIS, CONFETTI, ARC }

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

/** MUST stay thread-safe: the physics thread and the Swing EDT both touch this. */
class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();

    synchronized void add(Particle p) { particles.add(p); }

    synchronized void update(double dt) {
        particles.removeIf(p -> !p.update(dt));
    }

    void spawnConfetti(double x, double y) {
        Color[] palette = {
                new Color(255, 210, 80), new Color(220, 120, 50),
                new Color(255, 250, 220), new Color(180, 60, 50)
        };
        Random r = new Random();
        int count = 34;
        for (int i = 0; i < count; i++) {
            double ang = (Math.PI * 2 * i) / count + (r.nextDouble() - 0.5) * 0.3;
            double sp = 90 + r.nextDouble() * 140;
            Color c = palette[r.nextInt(palette.length)];
            add(new Particle(x, y - 10, Math.cos(ang) * sp, Math.sin(ang) * sp - 40,
                    0.6 + r.nextDouble() * 0.5, 3 + r.nextDouble() * 4, -3, c, Particle.ParticleKind.CONFETTI));
        }
        for (int i = 0; i < 10; i++) {
            add(new Particle(x, y, (r.nextDouble() - 0.5) * 30, -60 - r.nextDouble() * 60,
                    0.5, 5, 4, new Color(255, 255, 255), Particle.ParticleKind.CONFETTI));
        }
    }

    synchronized void draw(Graphics2D g) {
        for (Particle p : particles) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.alpha()));
            g.setColor(p.color);
            double s = Math.max(0.5, p.size);
            if (p.kind == Particle.ParticleKind.ARC) {
                g.setStroke(new BasicStroke(2));
                g.draw(new Line2D.Double(p.x - s, p.y - s * 0.4, p.x + s, p.y + s * 0.4));
                g.setStroke(new BasicStroke(1));
            } else if (p.kind == Particle.ParticleKind.CONFETTI) {
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
 * Narrowed-corridor stadium loop: two straights joined by two semicircular
 * arcs (a classic simple, convex, non-self-intersecting closed loop), but
 * unlike a plain stadium the half-width along each straight is not constant.
 * Each straight has a "pinched" junkyard-alley section in its middle (narrow
 * corridor) flanked by smooth transition ramps back to the open width near
 * the arcs. Lap counting and edge rendering reuse a centerline point list
 * with a parallel cumulative arc-length array (robust even off-centerline)
 * and per-point normals for offset-based inner/outer edges, exactly like the
 * reference architecture — the only addition is a per-point half-width array
 * instead of one fixed half-width.
 */
class Track {
    final double centerX, centerY, straightLen, arcRadius;
    final double openHalfWidth, narrowHalfWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] halfWidthAt;
    double[] cumulativeLength;
    double totalLength;
    double innerSign = 1.0;

    // Indices of the pinch-zone centers, useful for placing hazards/props.
    int topPinchIndex, bottomPinchIndex;

    Track(double centerX, double centerY, double straightLen, double arcRadius,
          double openHalfWidth, double narrowHalfWidth) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.straightLen = straightLen;
        this.arcRadius = arcRadius;
        this.openHalfWidth = openHalfWidth;
        this.narrowHalfWidth = narrowHalfWidth;
        build();
    }

    private double pinchFactor(double u) {
        double c = Math.abs(u - 0.5);
        if (c <= 0.15) return 1.0;
        if (c >= 0.35) return 0.0;
        return 1.0 - Util.smoothstep(0.15, 0.35, c);
    }

    private void build() {
        centerline.clear();
        List<Double> pinch = new ArrayList<>();
        int N1 = 150, N2 = 150;
        double L = straightLen, R = arcRadius;

        // Top straight, left -> right, at y = centerY - R.
        for (int i = 0; i < N1; i++) {
            double u = i / (double) N1;
            double x = centerX - L / 2 + u * L;
            double y = centerY - R;
            centerline.add(new Point2D.Double(x, y));
            pinch.add(pinchFactor(u));
        }
        topPinchIndex = N1 / 2;

        // Right arc, angle -90deg -> +90deg, around (centerX + L/2, centerY).
        for (int i = 0; i < N2; i++) {
            double t = i / (double) N2;
            double ang = -Math.PI / 2 + t * Math.PI;
            double x = centerX + L / 2 + R * Math.cos(ang);
            double y = centerY + R * Math.sin(ang);
            centerline.add(new Point2D.Double(x, y));
            pinch.add(0.0);
        }

        // Bottom straight, right -> left, at y = centerY + R.
        for (int i = 0; i < N1; i++) {
            double u = i / (double) N1;
            double x = centerX + L / 2 - u * L;
            double y = centerY + R;
            centerline.add(new Point2D.Double(x, y));
            pinch.add(pinchFactor(u));
        }
        bottomPinchIndex = N1 + N2 + N1 / 2;

        // Left arc, angle +90deg -> +270deg, around (centerX - L/2, centerY).
        for (int i = 0; i < N2; i++) {
            double t = i / (double) N2;
            double ang = Math.PI / 2 + t * Math.PI;
            double x = centerX - L / 2 + R * Math.cos(ang);
            double y = centerY + R * Math.sin(ang);
            centerline.add(new Point2D.Double(x, y));
            pinch.add(0.0);
        }

        int n = centerline.size();
        halfWidthAt = new double[n];
        for (int i = 0; i < n; i++) {
            halfWidthAt[i] = openHalfWidth - (openHalfWidth - narrowHalfWidth) * pinch.get(i);
        }

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

        Point2D.Double p = centerline.get(topPinchIndex);
        Point2D.Double nrm = normals.get(topPinchIndex);
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

    boolean onTrack(double x, double y) {
        int i = nearestIndex(x, y);
        return centerline.get(i).distance(x, y) < halfWidthAt[i];
    }

    Point2D.Double pointAtOffset(int index, double offset) {
        Point2D.Double p = centerline.get(index);
        Point2D.Double n = normals.get(index);
        return new Point2D.Double(p.x + n.x * offset, p.y + n.y * offset);
    }

    /** A point `depth` units further inward (toward the infield) than the local inner edge. */
    Point2D.Double inwardPoint(int index, double depth) {
        return pointAtOffset(index, innerSign * (halfWidthAt[index] + depth));
    }

    /** A point `depth` units further outward (away from the infield) than the local outer edge. */
    Point2D.Double outwardPoint(int index, double depth) {
        return pointAtOffset(index, -innerSign * (halfWidthAt[index] + depth));
    }

    Point2D.Double startPosition(double laneOffset) {
        return pointAtOffset(10, laneOffset);
    }

    double startAngle() {
        Point2D.Double a = centerline.get(10);
        Point2D.Double b = centerline.get(15);
        return Math.atan2(b.y - a.y, b.x - a.x);
    }

    void draw(Graphics2D g) {
        int n = centerline.size();
        Path2D.Double outer = new Path2D.Double();
        Path2D.Double inner = new Path2D.Double();
        for (int i = 0; i < n; i++) {
            Point2D.Double o = pointAtOffset(i, halfWidthAt[i]);
            Point2D.Double in = pointAtOffset(i, -halfWidthAt[i]);
            if (i == 0) { outer.moveTo(o.x, o.y); inner.moveTo(in.x, in.y); }
            else { outer.lineTo(o.x, o.y); inner.lineTo(in.x, in.y); }
        }
        outer.closePath();
        inner.closePath();

        Area road = new Area(outer);
        road.subtract(new Area(inner));
        g.setColor(new Color(148, 118, 90));
        g.fill(road);

        // Rust-stained tire tracks / oil-stain patches across the road.
        g.setColor(new Color(102, 82, 60, 110));
        for (int i = 0; i < n; i += 6) {
            Point2D.Double o = pointAtOffset(i, halfWidthAt[i]);
            Point2D.Double in = pointAtOffset(i, -halfWidthAt[i]);
            g.draw(new Line2D.Double(o, in));
        }

        g.setColor(new Color(90, 70, 52));
        g.setStroke(new BasicStroke(4));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(214, 176, 120, 150));
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
        Point2D.Double o = pointAtOffset(10, halfWidthAt[10]);
        Point2D.Double in = pointAtOffset(10, -halfWidthAt[10]);
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
    enum Kind { SCRAP_PILE, TIRE_STACK, CAR_WRECK, DEBRIS }

    double x, y, radius, rotation;
    Kind kind;
    double vx, vy, spin;

    Obstacle(Kind kind, double x, double y, double radius) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.rotation = Math.random() * Math.PI * 2;
        if (kind == Kind.DEBRIS) this.spin = 4 + Math.random() * 3;
    }

    void update(double dt, double minX, double maxX, double minY, double maxY) {
        if (kind == Kind.DEBRIS) {
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
            case SCRAP_PILE:
                g.setColor(new Color(120, 96, 72));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(150, 60, 40));
                g.fillOval((int) (-radius * 0.4), (int) (-radius * 0.3), (int) (radius), (int) (radius * 0.8));
                g.setColor(new Color(80, 62, 46));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                break;
            case TIRE_STACK:
                g.setColor(new Color(35, 33, 30));
                for (int i = -1; i <= 1; i++) {
                    g.fillOval((int) (-radius * 0.6), (int) (i * radius * 0.7 - radius * 0.35), (int) (radius * 1.2), (int) (radius * 0.7));
                }
                g.setColor(new Color(60, 58, 54));
                for (int i = -1; i <= 1; i++) {
                    g.drawOval((int) (-radius * 0.6), (int) (i * radius * 0.7 - radius * 0.35), (int) (radius * 1.2), (int) (radius * 0.7));
                }
                break;
            case CAR_WRECK:
                g.setColor(new Color(96, 60, 40));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(new Color(60, 34, 22));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) (radius * 0.3));
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.setColor(new Color(150, 70, 40, 150));
                g.fillRect((int) (-radius * 0.5), (int) (-radius * 0.4), (int) radius, (int) (radius * 0.2));
                break;
            case DEBRIS:
                g.setColor(new Color(150, 108, 76));
                for (int i = 0; i < 6; i++) {
                    double ang = i * Math.PI / 3;
                    g.drawLine(0, 0, (int) (Math.cos(ang) * radius), (int) (Math.sin(ang) * radius));
                }
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                break;
        }
        g.setTransform(old);
    }
}

/** Purely decorative junkyard dressing around the outer rim: no collision. */
class JunkProp {
    enum Kind { MOUND, CAR_STACK, FENCE_POST }
    final Kind kind;
    final double x, y, lean, scale;

    JunkProp(Kind kind, double x, double y, double lean, double scale) {
        this.kind = kind; this.x = x; this.y = y; this.lean = lean; this.scale = scale;
    }

    void draw(Graphics2D g) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(lean);
        g.scale(scale, scale);
        switch (kind) {
            case MOUND:
                g.setColor(new Color(96, 78, 58, 210));
                g.fillOval(-26, -20, 52, 34);
                g.setColor(new Color(70, 56, 42, 210));
                g.fillOval(-14, -30, 30, 26);
                g.setColor(new Color(130, 70, 40, 180));
                g.fillOval(-6, -12, 18, 14);
                break;
            case CAR_STACK:
                g.setColor(new Color(84, 58, 44, 210));
                g.fillRoundRect(-16, -46, 32, 22, 4, 4);
                g.setColor(new Color(110, 74, 50, 210));
                g.fillRoundRect(-14, -22, 28, 22, 4, 4);
                g.setColor(new Color(40, 38, 34, 210));
                for (int i = -10; i <= 10; i += 10) g.drawLine(i, -44, i, 0);
                break;
            case FENCE_POST:
                g.setColor(new Color(70, 64, 56, 200));
                g.fillRect(-4, -50, 8, 50);
                g.setColor(new Color(150, 40, 40, 160));
                g.fillRect(-14, -48, 28, 6); // hazard slat
                break;
        }
        g.setTransform(old);
    }
}

/** A torn tarp/plastic sheet snagged on the junkyard fence, flapping in the wind. */
class TarpFlag {
    final double x, y, w, h, phase;
    final Color cloth;

    TarpFlag(double x, double y, double w, double h, double phase, Color cloth) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.phase = phase; this.cloth = cloth;
    }

    void draw(Graphics2D g, double t) {
        double wave1 = Math.sin(t * 1.6 + phase) * 8;
        double wave2 = Math.sin(t * 1.6 + phase + 1.1) * 12;
        Path2D.Double flag = new Path2D.Double();
        flag.moveTo(x, y);
        flag.lineTo(x + w, y + wave1 * 0.3);
        flag.lineTo(x + w * 0.65 + wave2, y + h);
        flag.lineTo(x + w * 0.15, y + h * 0.85 + wave1);
        flag.closePath();
        g.setColor(cloth);
        g.fill(flag);
        g.setColor(cloth.darker());
        g.draw(flag);
        g.setColor(new Color(0, 0, 0, 60));
        g.drawLine((int) x, (int) y, (int) (x + w * 0.4 + wave2 * 0.4), (int) (y + h * 0.55));
    }
}

/**
 * Signature hazard: an overhead scrapyard crane whose magnet hangs on a rigid
 * arm from a fixed pivot and swings back and forth like a pendulum (angle
 * oscillates sinusoidally between +-maxSwingAngle around a center direction),
 * rather than sweeping a full circle. Because the arm is nearly vertical, the
 * pendulum naturally dips deepest into the corridor when centered and lifts
 * up and away at the swing extremes — so it is only "close" to the track for
 * part of each cycle. While a car is within range, it does NOT get an
 * instant positional bump; instead it feels a continuous magnetic pull
 * (an acceleration applied straight to its velocity) toward the magnet,
 * strongest the nearer and the more centered the swing is. That is what
 * gives it a dragging feel distinct from a wrecking-ball knockback.
 */
class CraneMagnet {
    final double pivotX, pivotY, armLength, centerAngle, maxSwingAngle, angularSpeed, phase;
    final double influenceRadius, maxPullAccel, magnetRadius;
    double time;
    double angleOffset;

    CraneMagnet(double pivotX, double pivotY, double armLength, double centerAngle, double maxSwingAngle,
                double angularSpeed, double phase, double influenceRadius, double maxPullAccel, double magnetRadius) {
        this.pivotX = pivotX; this.pivotY = pivotY;
        this.armLength = armLength; this.centerAngle = centerAngle;
        this.maxSwingAngle = maxSwingAngle; this.angularSpeed = angularSpeed; this.phase = phase;
        this.influenceRadius = influenceRadius; this.maxPullAccel = maxPullAccel;
        this.magnetRadius = magnetRadius;
    }

    void update(double dt) {
        time += dt;
        angleOffset = Math.sin(time * angularSpeed + phase) * maxSwingAngle;
    }

    double magnetX() { return pivotX + Math.cos(centerAngle + angleOffset) * armLength; }
    double magnetY() { return pivotY + Math.sin(centerAngle + angleOffset) * armLength; }

    /** 1.0 when the pendulum is centered (deepest/closest reach), 0.0 at the swing extremes. */
    double closeness() { return 1.0 - Math.abs(angleOffset) / maxSwingAngle; }

    void draw(Graphics2D g) {
        double mx = magnetX(), my = magnetY();
        double c = Util.clamp(closeness(), 0, 1);

        // Faint magnetic field ring, brighter the closer the swing is to center.
        if (c > 0.1) {
            g.setColor(new Color(200, 60, 40, (int) (60 * c)));
            g.setStroke(new BasicStroke(2));
            g.draw(new Ellipse2D.Double(mx - influenceRadius, my - influenceRadius, influenceRadius * 2, influenceRadius * 2));
            g.setStroke(new BasicStroke(1));
        }

        // Crane tower + boom.
        g.setColor(new Color(80, 70, 60));
        g.fillRect((int) pivotX - 6, (int) pivotY - 6, 12, 12);
        g.setColor(new Color(60, 52, 46));
        g.drawRect((int) pivotX - 6, (int) pivotY - 6, 12, 12);
        g.setColor(new Color(90, 78, 66));
        g.fillRect((int) pivotX - 3, (int) pivotY - 60, 6, 60);
        for (int i = 0; i < 4; i++) {
            int yy = (int) pivotY - 8 - i * 14;
            g.drawLine((int) pivotX - 3, yy, (int) pivotX + 3, yy - 10);
            g.drawLine((int) pivotX + 3, yy, (int) pivotX - 3, yy - 10);
        }

        // Chain.
        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(60, 58, 54));
        int links = 6;
        for (int i = 0; i <= links; i++) {
            double t = i / (double) links;
            double lx = pivotX + (mx - pivotX) * t;
            double ly = pivotY + (my - pivotY) * t;
            g.fillOval((int) lx - 3, (int) ly - 3, 6, 6);
        }
        g.setStroke(new BasicStroke(1));

        // Horseshoe magnet.
        AffineTransform old = g.getTransform();
        g.translate(mx, my);
        g.rotate(centerAngle + angleOffset - Math.PI / 2);
        g.setColor(new Color(40, 38, 36));
        g.fillRoundRect((int) -magnetRadius, (int) -magnetRadius * 0, (int) (magnetRadius * 2), (int) (magnetRadius * 1.1), 6, 6);
        g.setColor(new Color(180, 40, 30));
        g.fillRect((int) -magnetRadius, (int) (magnetRadius * 0.4), (int) (magnetRadius * 0.6), (int) (magnetRadius * 0.5));
        g.setColor(new Color(210, 210, 200));
        g.fillRect((int) (magnetRadius * 0.4), (int) (magnetRadius * 0.4), (int) (magnetRadius * 0.6), (int) (magnetRadius * 0.5));
        g.setColor(Color.BLACK);
        g.drawRoundRect((int) -magnetRadius, 0, (int) (magnetRadius * 2), (int) (magnetRadius * 1.1), 6, 6);
        g.setTransform(old);

        // Little electro-arc sparks when charged and swinging close.
        if (c > 0.55) {
            Random r = new Random((long) (time * 997));
            g.setColor(new Color(255, 220, 120, (int) (200 * c)));
            for (int i = 0; i < 3; i++) {
                double ang = r.nextDouble() * Math.PI * 2;
                double len = magnetRadius * (0.8 + r.nextDouble() * 0.6);
                g.drawLine((int) mx, (int) my, (int) (mx + Math.cos(ang) * len), (int) (my + Math.sin(ang) * len));
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

    final Color bodyColor;
    final Color trimColor;
    final String label;
    final ParticleSystem particles;

    private static final double MAX_SPEED = 420;
    private static final double MAX_REVERSE = -155;
    private static final double ACCEL = 300;
    private static final double BRAKE = 560;
    private static final double NATURAL_FRICTION = 150;
    private static final double OFFROAD_MULT = 2.4;
    private static final double TURN_RATE = 3.0;
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

        boolean onTrack = track.onTrack(x, y);
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
        boolean onTrack = track.onTrack(x, y);
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
        Color c = onTrack ? new Color(190, 160, 120, 120) : new Color(160, 120, 80, 160);
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

    /** Continuous magnetic drag force (not a positional bump): nudges velocity toward the magnet. */
    void applyMagnetPull(double ax, double ay, double dt) {
        if (!alive) return;
        vx += ax * dt;
        vy += ay * dt;
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

    InputState think(double dt, List<Obstacle> obstacles, List<CraneMagnet> cranes) {
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

        for (CraneMagnet crane : cranes) {
            double dx = crane.magnetX() - car.x, dy = crane.magnetY() - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < crane.influenceRadius * 1.2 && crane.closeness() > 0.25) {
                double angToObs = Util.normalizeAngle(Math.atan2(dy, dx) - car.angle);
                if (Math.abs(angToObs) < 1.1) {
                    diff += angToObs < 0 ? 0.8 : -0.8;
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
    private final List<JunkProp> junkProps = new ArrayList<>();
    private final List<TarpFlag> tarps = new ArrayList<>();
    private final List<CraneMagnet> cranes = new ArrayList<>();
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
        track = new Track(W / 2.0, H / 2.0 + 10, 560, 170, 95, 48);
        int n = track.centerline.size();

        obstacles.clear();
        Random r = new Random(42);
        for (int i = 0; i < n; i += 11) {
            if (r.nextDouble() < 0.5) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidthAt[i] + 20 + r.nextDouble() * 55;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.SCRAP_PILE : Obstacle.Kind.TIRE_STACK;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(10)));
            }
        }
        // Dense scrap walls hugging both pinched corridor sections.
        for (int base : new int[]{track.topPinchIndex, track.bottomPinchIndex}) {
            for (int di = -32; di <= 32; di += 8) {
                int idx = ((base + di) % n + n) % n;
                double margin = 14 + r.nextDouble() * 10;
                Point2D.Double inP = track.inwardPoint(idx, margin);
                Point2D.Double outP = track.outwardPoint(idx, margin);
                obstacles.add(new Obstacle(Obstacle.Kind.CAR_WRECK, inP.x, inP.y, 14 + r.nextInt(6)));
                obstacles.add(new Obstacle(Obstacle.Kind.SCRAP_PILE, outP.x, outP.y, 14 + r.nextInt(8)));
            }
        }
        for (int i = 0; i < 3; i++) {
            Obstacle d = new Obstacle(Obstacle.Kind.DEBRIS, r.nextInt(W), r.nextInt(H), 13);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 40 + r.nextDouble() * 40;
            d.vx = Math.cos(a) * sp;
            d.vy = Math.sin(a) * sp;
            obstacles.add(d);
        }

        // Decorative junk piles & fence posts ringing the outer rim.
        junkProps.clear();
        Random rr = new Random(99);
        for (int i = 0; i < n; i += 18) {
            Point2D.Double p = track.outwardPoint(i, 55 + rr.nextInt(30));
            double lean = (rr.nextDouble() - 0.5) * 0.3;
            double scale = 0.8 + rr.nextDouble() * 0.5;
            double roll = rr.nextDouble();
            JunkProp.Kind kind = roll < 0.4 ? JunkProp.Kind.MOUND : roll < 0.75 ? JunkProp.Kind.CAR_STACK : JunkProp.Kind.FENCE_POST;
            junkProps.add(new JunkProp(kind, p.x, p.y, lean, scale));
        }

        // Two torn tarps flapping among the junk.
        tarps.clear();
        Point2D.Double t1 = track.outwardPoint(n / 8, 70);
        Point2D.Double t2 = track.outwardPoint((5 * n) / 8, 70);
        tarps.add(new TarpFlag(t1.x - 20, t1.y - 60, 46, 70, 0.0, new Color(150, 120, 70)));
        tarps.add(new TarpFlag(t2.x - 20, t2.y - 60, 46, 70, 2.4, new Color(110, 90, 100)));

        // Signature hazard: two swinging crane magnets, one over each pinched
        // corridor section, mounted outside the track with their arms hanging
        // in toward the lane so the magnet dips deep into the corridor when
        // centered and lifts away at the swing extremes.
        cranes.clear();
        double topX = track.centerline.get(track.topPinchIndex).x;
        double topY = track.centerline.get(track.topPinchIndex).y;
        cranes.add(new CraneMagnet(topX, topY - track.openHalfWidth - 70, 92,
                Math.PI / 2, 0.9, 1.1, 0.0, 130, 620, 16));

        double botX = track.centerline.get(track.bottomPinchIndex).x;
        double botY = track.centerline.get(track.bottomPinchIndex).y;
        cranes.add(new CraneMagnet(botX, botY + track.openHalfWidth + 70, 92,
                -Math.PI / 2, 0.9, 0.85, 1.7, 130, 620, 16));
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-22);
        Point2D.Double p2pos = track.startPosition(22);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(178, 74, 36), new Color(226, 196, 150), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(96, 108, 104), new Color(200, 202, 190), vsAI ? "AI" : "2", particles);
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
        for (CraneMagnet crane : cranes) crane.update(dt);

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
            in2 = aiDriver.think(dt, obstacles, cranes);
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
        for (CraneMagnet crane : cranes) {
            resolveCraneMagnetPull(p1, crane, dt);
            resolveCraneMagnetPull(p2, crane, dt);
        }

        // Purely celebratory: confetti whenever a car crosses the start/finish line.
        Point2D.Double startPt = track.startPosition(0);
        if (p1.justLapped) particles.spawnConfetti(startPt.x, startPt.y);
        if (p2.justLapped) particles.spawnConfetti(startPt.x, startPt.y);

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
            if (o.kind == Obstacle.Kind.DEBRIS) {
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

    /**
     * Signature hazard interaction: while the car is within the magnet's
     * influence radius it feels a continuous pull toward the magnet's
     * current position (velocity nudge, no positional snapping) — stronger
     * the closer it is and the more centered the pendulum's swing currently
     * is. Only if the car ends up essentially under the magnet does it also
     * take minor scraping damage and a brief spin, distinguishing this from
     * a single hard knockback collision.
     */
    private void resolveCraneMagnetPull(Car c, CraneMagnet crane, double dt) {
        if (!c.alive) return;
        double mx = crane.magnetX(), my = crane.magnetY();
        double dx = mx - c.x, dy = my - c.y;
        double dist = Math.hypot(dx, dy);
        if (dist < crane.influenceRadius && dist > 1e-3) {
            double closenessDist = 1.0 - dist / crane.influenceRadius;
            double swingBoost = 0.6 + 0.4 * Util.clamp(crane.closeness(), 0, 1);
            double accel = crane.maxPullAccel * closenessDist * closenessDist * swingBoost;
            double nx = dx / dist, ny = dy / dist;
            c.applyMagnetPull(nx * accel, ny * accel, dt);

            if (dist < c.radius() + crane.magnetRadius) {
                c.spinOut(0.2);
                c.damage(4 * dt);
            }
            if (Math.random() < 0.5) {
                particles.add(new Particle(c.x + (Math.random() - 0.5) * 10, c.y + (Math.random() - 0.5) * 10,
                        nx * 40, ny * 40, 0.2, 6, -4, new Color(255, 210, 90, 180), Particle.ParticleKind.ARC));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D sg = sceneBuffer.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBackground(sg);
        for (JunkProp jp : junkProps) jp.draw(sg);
        track.draw(sg);
        for (Obstacle o : obstacles) o.draw(sg);
        for (CraneMagnet crane : cranes) crane.draw(sg);
        for (TarpFlag t : tarps) t.draw(sg, timeAccum);
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
        int top = (int) (H * 0.36), bandH = 36;
        for (int y = top; y < top + bandH && y < H; y++) {
            double t = (y - top) / (double) bandH;
            int shift = (int) (Math.sin(y * 0.25 + timeAccum * 3.5) * 3 * (1 - t));
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
        // Overcast, dusty sepia sky — no sun, just a flat haze gradient.
        g.setPaint(new GradientPaint(0, 0, new Color(198, 186, 164), 0, (float) (H * 0.42), new Color(210, 196, 168)));
        g.fillRect(0, 0, W, (int) (H * 0.42));

        g.setColor(new Color(180, 168, 148, 130));
        for (int i = 0; i < 5; i++) {
            double cy = 40 + i * 30 + Math.sin(timeAccum * 0.2 + i) * 6;
            g.fillOval((int) (80 + i * 240), (int) cy, 220, 34);
        }

        // Distant junkyard skyline: jagged piles of scrap instead of mountains.
        g.setColor(new Color(110, 96, 80));
        Path2D.Double skyline = new Path2D.Double();
        skyline.moveTo(-50, H * 0.42);
        double wx = -50;
        Random wr = new Random(7);
        boolean up = true;
        while (wx < W + 50) {
            double wy = up ? H * 0.42 - (60 + wr.nextInt(40)) : H * 0.42 - (20 + wr.nextInt(20));
            skyline.lineTo(wx, wy);
            wx += 18 + wr.nextInt(14);
            up = !up;
        }
        skyline.lineTo(W + 50, H * 0.42);
        skyline.closePath();
        g.fill(skyline);

        g.setPaint(new GradientPaint(0, (float) (H * 0.4), new Color(188, 164, 128), 0, H, new Color(140, 118, 92)));
        g.fillRect(0, (int) (H * 0.4), W, (int) (H * 0.6));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(214, 150, 70));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        centerText(g, "SCRAPYARD SANDS 500", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Rusted junkyard corridors — beware the swinging crane magnets", H / 2 - 90);

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
