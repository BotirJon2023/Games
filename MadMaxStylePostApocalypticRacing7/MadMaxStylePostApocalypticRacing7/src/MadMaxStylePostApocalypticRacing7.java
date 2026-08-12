import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class MadMaxStylePostApocalypticRacing7 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Radioactive Wasteland — Toxic Desert Racing");
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

    static int clampByte(int v) {
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

    enum ParticleKind { DUST, SMOKE, SPARK, DEBRIS, RAD }

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
 * Asymmetric "rounded trapezoid" wasteland loop: a convex trapezoid (top and
 * bottom straights of different lengths, skewed sides) whose four corners are
 * rounded with two independent radii (a big lazy sweep at the "left end" and
 * a tighter hairpin at the "right end") — giving a visibly lopsided,
 * kidney-like loop instead of a symmetric stadium oval. The centerline is
 * still a single simple closed polyline with a parallel cumulative
 * arc-length table, reusing the reference game's proven approach for both
 * robust lap counting and offset-based edge rendering/collision via normals.
 */
class Track {
    final double halfWidth;
    final double trackWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;

    Track(double cx, double cy, double topLen, double botLen, double edgeGap, double skew,
          double rLeft, double rRight, double trackWidth) {
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        build(cx, cy, topLen, botLen, edgeGap, skew, rLeft, rRight);
    }

    private void build(double cx, double cy, double topLen, double botLen, double edgeGap, double skew,
                        double rLeft, double rRight) {
        double half = edgeGap / 2;
        Point2D.Double[] V = {
                new Point2D.Double(cx - topLen / 2, cy - half),        // 0 topLeft
                new Point2D.Double(cx + topLen / 2, cy - half),        // 1 topRight
                new Point2D.Double(cx + botLen / 2 + skew, cy + half), // 2 botRight
                new Point2D.Double(cx - botLen / 2 + skew, cy + half)  // 3 botLeft
        };
        double[] radii = { rLeft, rRight, rRight, rLeft };
        int n = V.length;

        double[] ux1 = new double[n], uy1 = new double[n], ux2 = new double[n], uy2 = new double[n];
        double[] phi = new double[n], tlen = new double[n];

        for (int i = 0; i < n; i++) {
            Point2D.Double prevV = V[(i - 1 + n) % n], curV = V[i], nextV = V[(i + 1) % n];
            double dx1 = prevV.x - curV.x, dy1 = prevV.y - curV.y;
            double dx2 = nextV.x - curV.x, dy2 = nextV.y - curV.y;
            double l1 = Math.hypot(dx1, dy1), l2 = Math.hypot(dx2, dy2);
            ux1[i] = dx1 / l1; uy1[i] = dy1 / l1;
            ux2[i] = dx2 / l2; uy2[i] = dy2 / l2;
            double dot = Util.clamp(ux1[i] * ux2[i] + uy1[i] * uy2[i], -1, 1);
            phi[i] = Math.acos(dot);
            tlen[i] = radii[i] / Math.tan(phi[i] / 2);
        }
        // Prevent adjacent fillets on the same edge from overlapping (keeps the
        // loop a simple, non-self-intersecting curve for any parameter choice).
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double edgeLen = V[i].distance(V[j]);
            double sum = tlen[i] + tlen[j];
            double maxSum = edgeLen * 0.88;
            if (sum > maxSum && sum > 1e-6) {
                double scale = maxSum / sum;
                tlen[i] *= scale;
                tlen[j] *= scale;
            }
        }

        Point2D.Double[] T1s = new Point2D.Double[n];
        Point2D.Double[] T2s = new Point2D.Double[n];
        Point2D.Double[] centers = new Point2D.Double[n];
        double[] rEffs = new double[n];
        double[] a1s = new double[n];
        double[] deltas = new double[n];

        for (int i = 0; i < n; i++) {
            Point2D.Double curV = V[i];
            double t = tlen[i];
            double rEff = t * Math.tan(phi[i] / 2);
            Point2D.Double T1 = new Point2D.Double(curV.x + ux1[i] * t, curV.y + uy1[i] * t);
            Point2D.Double T2 = new Point2D.Double(curV.x + ux2[i] * t, curV.y + uy2[i] * t);
            double bx = ux1[i] + ux2[i], by = uy1[i] + uy2[i];
            double bl = Math.hypot(bx, by);
            if (bl < 1e-9) { bx = -uy1[i]; by = ux1[i]; bl = 1; }
            bx /= bl; by /= bl;
            double sinHalf = Math.sin(phi[i] / 2);
            double distToCenter = sinHalf > 1e-6 ? rEff / sinHalf : 0;
            Point2D.Double center = new Point2D.Double(curV.x + bx * distToCenter, curV.y + by * distToCenter);

            double a1 = Math.atan2(T1.y - center.y, T1.x - center.x);
            double a2 = Math.atan2(T2.y - center.y, T2.x - center.x);
            double shortDelta = Util.normalizeAngle(a2 - a1);
            double longDelta = shortDelta > 0 ? shortDelta - 2 * Math.PI : shortDelta + 2 * Math.PI;
            double midShort = a1 + shortDelta / 2;
            double midLong = a1 + longDelta / 2;
            Point2D.Double pShort = new Point2D.Double(center.x + Math.cos(midShort) * rEff, center.y + Math.sin(midShort) * rEff);
            Point2D.Double pLong = new Point2D.Double(center.x + Math.cos(midLong) * rEff, center.y + Math.sin(midLong) * rEff);
            double delta = pShort.distance(curV) <= pLong.distance(curV) ? shortDelta : longDelta;

            T1s[i] = T1; T2s[i] = T2; centers[i] = center; rEffs[i] = rEff; a1s[i] = a1; deltas[i] = delta;
        }

        int arcSeg = 28, straightSeg = 46;
        List<Point2D.Double> raw = new ArrayList<>();
        int[] straightStartIdx = new int[n];
        for (int i = 0; i < n; i++) {
            for (int k = 0; k <= arcSeg; k++) {
                double a = a1s[i] + deltas[i] * k / (double) arcSeg;
                raw.add(new Point2D.Double(centers[i].x + Math.cos(a) * rEffs[i], centers[i].y + Math.sin(a) * rEffs[i]));
            }
            straightStartIdx[i] = raw.size();
            int j = (i + 1) % n;
            Point2D.Double from = T2s[i], to = T1s[j];
            for (int k = 1; k < straightSeg; k++) {
                double f = k / (double) straightSeg;
                raw.add(new Point2D.Double(from.x + (to.x - from.x) * f, from.y + (to.y - from.y) * f));
            }
        }

        // Rotate the point list so index 0 sits at the start of the top
        // straight — a clean, readable spot for the start/finish line.
        int rotateAt = straightStartIdx[0];
        centerline.addAll(raw.subList(rotateAt, raw.size()));
        centerline.addAll(raw.subList(0, rotateAt));

        int n2 = centerline.size();
        cumulativeLength = new double[n2];
        cumulativeLength[0] = 0;
        for (int i = 1; i < n2; i++) {
            cumulativeLength[i] = cumulativeLength[i - 1] + centerline.get(i - 1).distance(centerline.get(i));
        }
        totalLength = cumulativeLength[n2 - 1] + centerline.get(n2 - 1).distance(centerline.get(0));

        for (int i = 0; i < n2; i++) {
            Point2D.Double prev = centerline.get((i - 1 + n2) % n2);
            Point2D.Double next = centerline.get((i + 1) % n2);
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
        int n = centerline.size();
        index = ((index % n) + n) % n;
        Point2D.Double p = centerline.get(index);
        Point2D.Double nrm = normals.get(index);
        return new Point2D.Double(p.x + nrm.x * offset, p.y + nrm.y * offset);
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
        g.setColor(new Color(68, 54, 66));
        g.fill(road);

        g.setColor(new Color(120, 150, 70, 210));
        g.setStroke(new BasicStroke(3));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(150, 255, 120, 130));
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
            g.setColor(i % 2 == 0 ? new Color(220, 255, 210) : new Color(30, 20, 35));
            g.fill(new Rectangle2D.Double(sx - 4, sy - 4, 8, 8));
        }
    }
}

/** A fixed glowing green hazard zone. Lingering inside drains health/sec. */
class RadiationPool {
    final double x, y, radius;
    final double phase;

    RadiationPool(double x, double y, double radius, double phase) {
        this.x = x; this.y = y; this.radius = radius; this.phase = phase;
    }

    void draw(Graphics2D g, double t) {
        double pulse = 0.5 + 0.5 * Math.sin(t * 2.1 + phase);
        double rVis = radius * (0.88 + 0.18 * pulse);
        int alphaCore = (int) (110 + 90 * pulse);
        Point2D center = new Point2D.Double(x, y);
        float[] fractions = {0f, 0.55f, 0.85f, 1f};
        Color[] colors = {
                new Color(200, 255, 160, Util.clampByte(alphaCore + 60)),
                new Color(120, 255, 90, Util.clampByte(alphaCore)),
                new Color(60, 200, 60, Util.clampByte(alphaCore / 2)),
                new Color(40, 160, 50, 0)
        };
        RadialGradientPaint paint = new RadialGradientPaint(center, (float) rVis, fractions, colors);
        Paint old = g.getPaint();
        g.setPaint(paint);
        g.fill(new Ellipse2D.Double(x - rVis, y - rVis, rVis * 2, rVis * 2));
        g.setPaint(old);

        g.setColor(new Color(180, 255, 150, 160));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 4, new float[]{6, 8}, (float) (t * 20)));
        g.draw(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
        g.setStroke(new BasicStroke(1));
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
                g.setColor(new Color(78, 70, 88));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(50, 44, 60));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                break;
            case CACTUS:
                g.setColor(new Color(20, 20, 22));
                g.fillRoundRect((int) (-radius / 3), (int) -radius, (int) (radius * 2 / 3), (int) (radius * 2), 8, 8);
                g.fillRoundRect((int) -radius, (int) (-radius / 3), (int) radius, (int) (radius * 2 / 3), 8, 8);
                g.fillRoundRect((int) (radius / 3), (int) (-radius * 0.7), (int) (radius * 2 / 3), (int) radius, 8, 8);
                break;
            case WRECK:
                g.setColor(new Color(70, 46, 70));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.setColor(new Color(140, 255, 110, 120));
                g.fillOval((int) (-radius / 4), (int) (-radius / 3), (int) (radius / 2), (int) (radius / 3));
                break;
            case TUMBLEWEED:
                g.setColor(new Color(40, 30, 20));
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
    boolean inRadiation = false;
    double radiationTickTimer = 0;

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
        Color c = onTrack ? new Color(150, 130, 150, 120) : new Color(120, 90, 120, 160);
        for (int i = 0; i < (onTrack ? 1 : 2); i++) {
            double jx = (Math.random() - 0.5) * 10;
            double jy = (Math.random() - 0.5) * 10;
            particles.add(new Particle(rearX + jx, rearY + jy,
                    -vx * 0.15 + (Math.random() - 0.5) * 20, -vy * 0.15 + (Math.random() - 0.5) * 20,
                    0.5 + Math.random() * 0.4, 6 + Math.random() * 6, 8, c, Particle.ParticleKind.DUST));
        }
        if (boosting) {
            particles.add(new Particle(rearX, rearY, -vx * 0.3, -vy * 0.3, 0.35, 10, 6,
                    new Color(180, 255, 110, 200), Particle.ParticleKind.SPARK));
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
                    Math.random() < 0.5 ? new Color(140, 255, 110) : new Color(90, 90, 90), Particle.ParticleKind.SMOKE));
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
            g.setColor(new Color(150, 255, 110, 200));
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
        g.setColor(new Color(20, 20, 24));
        g.fillRect(14, -4, 8, 2);
        g.fillRect(14, 2, 8, 2);
        g.setColor(new Color(200, 255, 170));
        g.fillOval(14, -6, 4, 4);
        g.fillOval(14, 2, 4, 4);

        if (inRadiation) {
            g.setColor(new Color(140, 255, 110, 160));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(-19, -14, 38, 28, 10, 10);
            g.setStroke(new BasicStroke(1));
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.drawString(label, -5, 4);

        g.setTransform(old);

        double hpFrac = Util.clamp(health / 100.0, 0, 1);
        g.setColor(Color.DARK_GRAY);
        g.fillRect((int) x - 16, (int) y - 26, 32, 4);
        g.setColor(hpFrac > 0.5 ? new Color(110, 220, 90) : hpFrac > 0.25 ? Color.ORANGE : Color.RED);
        g.fillRect((int) x - 16, (int) y - 26, (int) (32 * hpFrac), 4);
    }
}

class AIDriver {
    private final Car car;
    private final Track track;
    private final List<RadiationPool> pools;
    private double boostCooldown = 0;

    AIDriver(Car car, Track track, List<RadiationPool> pools) {
        this.car = car;
        this.track = track;
        this.pools = pools;
    }

    InputState think(double dt, List<Obstacle> obstacles) {
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

        for (RadiationPool p : pools) {
            double dx = p.x - car.x, dy = p.y - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < p.radius + 80) {
                double angToP = Util.normalizeAngle(Math.atan2(dy, dx) - car.angle);
                if (Math.abs(angToP) < 1.1) {
                    diff += angToP < 0 ? 0.5 : -0.5;
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
    private static final double RAD_DPS = 16.0; // radiation pool damage per second

    private enum State { MENU, COUNTDOWN, RACING, FINISHED }

    private State state = State.MENU;
    private boolean vsAI = true;
    private Thread thread;
    private volatile boolean running = true;

    private final Set<Integer> keys = new HashSet<>();
    private Track track;
    private List<Car> cars = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<RadiationPool> radiationPools = new ArrayList<>();
    private ParticleSystem particles = new ParticleSystem();
    private AIDriver aiDriver;

    private double countdownTimer;
    private double raceTime;
    private double worldTime = 0;
    private final double[][] deadTrees = new double[14][2]; // x, scale
    private final double[][] fogPatches = new double[6][4]; // x, y, radius, speed
    private final BufferedImage sceneBuffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        setupWorld();
        buildBackgroundDecor();
    }

    private void buildBackgroundDecor() {
        Random r = new Random(11);
        for (int i = 0; i < deadTrees.length; i++) {
            deadTrees[i][0] = r.nextDouble() * (W + 100) - 50;
            deadTrees[i][1] = 0.6 + r.nextDouble() * 0.9;
        }
        for (int i = 0; i < fogPatches.length; i++) {
            fogPatches[i][0] = r.nextDouble() * W;
            fogPatches[i][1] = H * 0.55 + r.nextDouble() * H * 0.4;
            fogPatches[i][2] = 60 + r.nextDouble() * 90;
            fogPatches[i][3] = 6 + r.nextDouble() * 10;
        }
    }

    private void setupWorld() {
        // Asymmetric rounded-trapezoid loop: unequal top/bottom straights,
        // a skewed span, and two different end radii (big lazy left sweep,
        // tight right hairpin) — a lopsided kidney shape, not a symmetric oval.
        track = new Track(640, 380, 460, 260, 300, 70, 150, 110, 140);

        obstacles.clear();
        Random r = new Random(42);
        int n = track.centerline.size();
        for (int i = 0; i < n; i += 9) {
            if (r.nextDouble() < 0.5) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 20 + r.nextDouble() * 55;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.ROCK : Obstacle.Kind.CACTUS;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(10)));
            }
        }
        int[] hazardIdx = { (int) (n * 0.1), (int) (n * 0.34), (int) (n * 0.58), (int) (n * 0.82) };
        for (int idx : hazardIdx) {
            double off = (r.nextDouble() - 0.5) * track.trackWidth * 0.6;
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

        // Radiation pools: one tempting inside-cut shortcut, one wide on the
        // outside of the big sweep, one sitting right on the track itself so
        // simply driving the racing line means passing through it quickly.
        radiationPools.clear();
        radiationPools.add(new RadiationPool(track.pointAtOffset((int) (n * 0.16), -60).x,
                track.pointAtOffset((int) (n * 0.16), -60).y, 68, 0.0));
        radiationPools.add(new RadiationPool(track.pointAtOffset((int) (n * 0.5), 95).x,
                track.pointAtOffset((int) (n * 0.5), 95).y, 62, 1.7));
        radiationPools.add(new RadiationPool(track.pointAtOffset((int) (n * 0.78), 5).x,
                track.pointAtOffset((int) (n * 0.78), 5).y, 82, 3.3));
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-25);
        Point2D.Double p2pos = track.startPosition(25);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(190, 70, 50), new Color(190, 255, 130), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(70, 70, 110), new Color(200, 140, 255), vsAI ? "AI" : "2", particles);
        p1.lastArcLen = track.progress(p1.x, p1.y);
        p2.lastArcLen = track.progress(p2.x, p2.y);
        cars.add(p1);
        cars.add(p2);
        aiDriver = vsAI ? new AIDriver(p2, track, radiationPools) : null;
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
        worldTime += dt;
        for (Obstacle o : obstacles) o.update(dt, -30, W + 30, -30, H + 30);
        for (double[] fp : fogPatches) {
            fp[0] += fp[3] * dt;
            if (fp[0] > W + 120) fp[0] = -120;
        }

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
            in2 = aiDriver.think(dt, obstacles);
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
        applyRadiation(dt);

        for (Car c : cars) {
            if (!c.finished && c.lap >= TOTAL_LAPS) {
                c.finished = true;
                c.finishTime = raceTime;
            }
        }
        if (p1.finished && p2.finished) state = State.FINISHED;
    }

    private void applyRadiation(double dt) {
        for (Car c : cars) {
            if (!c.alive) { c.inRadiation = false; continue; }
            boolean inside = false;
            for (RadiationPool p : radiationPools) {
                double d = Math.hypot(c.x - p.x, c.y - p.y);
                if (d < p.radius) {
                    inside = true;
                    c.damage(RAD_DPS * dt);
                    c.radiationTickTimer -= dt;
                    if (c.radiationTickTimer <= 0) {
                        c.radiationTickTimer = 0.12;
                        for (int k = 0; k < 3; k++) {
                            double ang = Math.random() * Math.PI * 2;
                            double sp = 20 + Math.random() * 45;
                            particles.add(new Particle(c.x, c.y, Math.cos(ang) * sp, Math.sin(ang) * sp - 25,
                                    0.4 + Math.random() * 0.3, 4 + Math.random() * 4, 6,
                                    new Color(140, 255, 100, 210), Particle.ParticleKind.RAD));
                        }
                    }
                    break;
                }
            }
            c.inRadiation = inside;
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
        for (RadiationPool p : radiationPools) p.draw(sg, worldTime);
        for (Obstacle o : obstacles) o.draw(sg);
        drawFog(sg);
        particles.draw(sg);
        for (Car c : cars) c.draw(sg);
        sg.dispose();

        applyHeatHaze(sceneBuffer);

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

    private void applyHeatHaze(BufferedImage img) {
        int top = (int) (H * 0.38), bandH = 40;
        for (int y = top; y < top + bandH && y < H; y++) {
            double t = (y - top) / (double) bandH;
            int shift = (int) (Math.sin(y * 0.25 + worldTime * 4) * 4 * (1 - t));
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
        g.setPaint(new GradientPaint(0, 0, new Color(18, 8, 26), 0, (float) (H * 0.46), new Color(88, 104, 52)));
        g.fillRect(0, 0, W, (int) (H * 0.46));

        double pulse = 1 + 0.05 * Math.sin(worldTime * 1.2);
        int moonR = (int) (52 * pulse);
        Point moonPos = new Point(190, 120);
        g.setPaint(new RadialGradientPaint(moonPos, moonR * 4f,
                new float[]{0f, 0.35f, 1f},
                new Color[]{new Color(180, 255, 150, 210), new Color(120, 220, 90, 90), new Color(120, 220, 90, 0)}));
        g.fillOval(moonPos.x - moonR * 4, moonPos.y - moonR * 4, moonR * 8, moonR * 8);
        g.setColor(new Color(196, 255, 176));
        g.fillOval(moonPos.x - moonR / 2, moonPos.y - moonR / 2, moonR, moonR);
        g.setColor(new Color(150, 215, 130));
        g.fillOval(moonPos.x - moonR / 2 + moonR / 4, moonPos.y - moonR / 2 + moonR / 5, moonR / 5, moonR / 5);
        g.fillOval(moonPos.x - moonR / 2 + moonR / 2, moonPos.y - moonR / 2 + moonR / 3, moonR / 6, moonR / 6);

        for (double[] t : deadTrees) drawDeadTree(g, t[0], H * 0.44, t[1]);

        g.setPaint(new GradientPaint(0, (float) (H * 0.42), new Color(78, 60, 82), 0, H, new Color(42, 30, 48)));
        g.fillRect(0, (int) (H * 0.4), W, (int) (H * 0.6));
    }

    private void drawDeadTree(Graphics2D g, double x, double baseY, double scale) {
        double h = 70 * scale;
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke((float) (2.5 * scale)));
        g.draw(new Line2D.Double(x, baseY, x, baseY - h));
        drawBranch(g, x, baseY - h * 0.7, -1, h * 0.5, 2);
        drawBranch(g, x, baseY - h * 0.85, 1, h * 0.45, 2);
        drawBranch(g, x, baseY - h * 0.95, -1, h * 0.3, 1);
        g.setStroke(new BasicStroke(1));
    }

    private void drawBranch(Graphics2D g, double x, double y, double dir, double len, int depth) {
        double ex = x + dir * len * 0.7, ey = y - len * 0.7;
        g.draw(new Line2D.Double(x, y, ex, ey));
        if (depth > 0) {
            drawBranch(g, ex, ey, dir, len * 0.55, depth - 1);
            drawBranch(g, ex, ey, -dir * 0.4, len * 0.4, depth - 1);
        }
    }

    private void drawFog(Graphics2D g) {
        for (double[] fp : fogPatches) {
            double x = fp[0], y = fp[1], r = fp[2];
            double pulse = 0.5 + 0.5 * Math.sin(worldTime * 0.6 + x * 0.01);
            int alpha = (int) (28 + 26 * pulse);
            RadialGradientPaint paint = new RadialGradientPaint(new Point2D.Double(x, y), (float) r,
                    new float[]{0f, 1f},
                    new Color[]{new Color(120, 255, 110, alpha), new Color(120, 255, 110, 0)});
            Paint old = g.getPaint();
            g.setPaint(paint);
            g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
            g.setPaint(old);
        }
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(10, 4, 14, 165));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(150, 255, 110));
        g.setFont(new Font("SansSerif", Font.BOLD, 52));
        centerText(g, "RADIOACTIVE WASTELAND", H / 2 - 150);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(new Color(220, 190, 255));
        centerText(g, "Toxic post-apocalyptic desert racing", H / 2 - 100);

        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        g.setColor(Color.WHITE);
        centerText(g, "[1] Player vs Player      [2] Player vs Computer", H / 2 - 30);
        centerText(g, "P1: W A S D  +  SPACE boost", H / 2 + 8);
        centerText(g, "P2: Arrow Keys + ENTER boost  (disabled vs Computer)", H / 2 + 34);
        g.setColor(new Color(150, 255, 110));
        centerText(g, "Beware the glowing green radiation pools — lingering burns your health!", H / 2 + 70);
        g.setColor(Color.WHITE);
        centerText(g, "Press 1 or 2 to start", H / 2 + 112);
    }

    private void centerText(Graphics2D g, String s, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (W - fm.stringWidth(s)) / 2, y);
    }

    private void drawCountdown(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 90));
        g.setColor(new Color(150, 255, 110));
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
        g.setColor(new Color(20, 8, 26, 150));
        g.fillRoundRect(x, 16, 190, 60, 10, 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString(tag + "  LAP " + Math.min(c.lap + 1, TOTAL_LAPS), x + 10, 32);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 10, 40, 170, 8);
        double hp = Util.clamp(c.health / 100.0, 0, 1);
        g.setColor(hp > 0.5 ? new Color(110, 220, 90) : hp > 0.25 ? Color.ORANGE : Color.RED);
        g.fillRect(x + 10, 40, (int) (170 * hp), 8);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 10, 54, 170, 8);
        g.setColor(new Color(190, 140, 255));
        g.fillRect(x + 10, 54, (int) (170 * Util.clamp(c.boostFuel / 100.0, 0, 1)), 8);

        if (c.inRadiation) {
            double pulse = 0.5 + 0.5 * Math.sin(worldTime * 10);
            g.setColor(new Color(150, 255, 110, (int) (150 + 100 * pulse)));
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.drawString("IRRADIATED", x + 10, 30);
        }
    }

    private void drawFinish(Graphics2D g) {
        g.setColor(new Color(10, 4, 14, 180));
        g.fillRect(0, 0, W, H);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 46));
        centerText(g, "RACE FINISHED", H / 2 - 80);
        Car p1 = cars.get(0), p2 = cars.get(1);
        Car winner = p1.finishTime <= p2.finishTime ? p1 : p2;
        g.setColor(new Color(150, 255, 110));
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        centerText(g, "WINNER: " + winner.label + "  (" + String.format("%.1fs", winner.finishTime) + ")", H / 2 - 20);
        g.setColor(Color.WHITE);
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
