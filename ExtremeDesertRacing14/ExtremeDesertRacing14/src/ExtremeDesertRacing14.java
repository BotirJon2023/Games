import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ExtremeDesertRacing14 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Fossil Flats 500 — Sinkhole Dig Pits");
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

/** Thread-safe: physics thread adds/updates while the EDT draws, so every entry point is synchronized. */
class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();

    synchronized void add(Particle p) { particles.add(p); }

    synchronized void update(double dt) {
        particles.removeIf(p -> !p.update(dt));
    }

    void spawnFirework(double x, double y) {
        Color[] palette = {
                new Color(255, 214, 120), new Color(224, 150, 70),
                new Color(255, 248, 220), new Color(200, 90, 50)
        };
        Random r = new Random();
        int count = 32;
        for (int i = 0; i < count; i++) {
            double ang = (Math.PI * 2 * i) / count + (r.nextDouble() - 0.5) * 0.3;
            double sp = 90 + r.nextDouble() * 140;
            Color c = palette[r.nextInt(palette.length)];
            add(new Particle(x, y - 10, Math.cos(ang) * sp, Math.sin(ang) * sp - 40,
                    0.6 + r.nextDouble() * 0.5, 3 + r.nextDouble() * 4, -3, c, Particle.ParticleKind.FIREWORK));
        }
        for (int i = 0; i < 10; i++) {
            add(new Particle(x, y, (r.nextDouble() - 0.5) * 30, -60 - r.nextDouble() * 60,
                    0.5, 5, 4, new Color(255, 250, 235), Particle.ParticleKind.FIREWORK));
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
 * Wide, generously-proportioned oval dig-site loop, generated from a
 * superellipse: |x/a|^n + |y/b|^n = 1. With n close to 2 the shape stays a
 * smooth simple ellipse-like oval (a bijective parametrization of t in
 * [0,2*PI), so the centerline can never self-intersect); a mildly higher n
 * (~2.3) just opens up the flat sides a little more than a pure ellipse,
 * matching the "wider than a typical stadium" brief.
 *
 * Lap counting and edge rendering/collision reuse the same technique as the
 * reference engine: a centerline point list with a parallel cumulative
 * arc-length array (robust lap progress even off-centerline) and per-point
 * normals for offset-based inner/outer edges.
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
        g.setColor(new Color(214, 200, 172));
        g.fill(road);

        // sun-baked, sandy seams across the dig-site road
        g.setColor(new Color(178, 162, 132, 130));
        for (int i = 0; i < centerline.size(); i += 6) {
            Point2D.Double o = pointAtOffset(i, halfWidth);
            Point2D.Double in = pointAtOffset(i, -halfWidth);
            g.draw(new Line2D.Double(o, in));
        }

        g.setColor(new Color(140, 120, 92));
        g.setStroke(new BasicStroke(4));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(250, 240, 210, 150));
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
            g.setColor(i % 2 == 0 ? new Color(250, 245, 232) : new Color(60, 50, 40));
            g.fill(new Rectangle2D.Double(sx - 4, sy - 4, 8, 8));
        }
    }
}

/** Fossil-themed collidable clutter: bone piles, rib arches, big skull rocks, dig crates, tumbling bone fragments. */
class Obstacle {
    enum Kind { BONE_PILE, RIB_ARCH, SKULL_ROCK, CRATE, TUMBLING_BONE }

    double x, y, radius, rotation;
    Kind kind;
    double vx, vy, spin;

    Obstacle(Kind kind, double x, double y, double radius) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.rotation = Math.random() * Math.PI * 2;
        if (kind == Kind.TUMBLING_BONE) this.spin = 4 + Math.random() * 3;
    }

    void update(double dt, double minX, double maxX, double minY, double maxY) {
        if (kind == Kind.TUMBLING_BONE) {
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
            case BONE_PILE:
                g.setColor(new Color(226, 214, 190));
                g.fillOval((int) -radius, (int) (-radius * 0.7), (int) (radius * 1.6), (int) (radius * 1.2));
                g.fillOval((int) (-radius * 0.2), (int) (-radius * 0.3), (int) (radius * 1.2), (int) (radius * 0.9));
                g.setColor(new Color(180, 164, 136));
                g.drawOval((int) -radius, (int) (-radius * 0.7), (int) (radius * 1.6), (int) (radius * 1.2));
                break;
            case RIB_ARCH:
                g.setColor(new Color(232, 222, 198));
                for (int i = -2; i <= 2; i++) {
                    double rr = radius * (1.0 - Math.abs(i) * 0.12);
                    g.draw(new Arc2D.Double(-rr, -rr + i * 3, rr * 2, rr * 2, 20, 140, Arc2D.OPEN));
                }
                g.setStroke(new BasicStroke(3));
                g.setColor(new Color(200, 186, 156));
                g.draw(new Arc2D.Double(-radius, -radius, radius * 2, radius * 2, 20, 140, Arc2D.OPEN));
                g.setStroke(new BasicStroke(1));
                break;
            case SKULL_ROCK:
                g.setColor(new Color(214, 200, 172));
                g.fillOval((int) -radius, (int) (-radius * 0.75), (int) (radius * 1.7), (int) (radius * 1.5));
                g.setColor(new Color(214, 200, 172));
                g.fillRoundRect((int) (radius * 0.5), (int) (-radius * 0.2), (int) (radius * 0.8), (int) (radius * 0.5), 4, 4);
                g.setColor(new Color(40, 34, 26));
                g.fillOval((int) (-radius * 0.35), (int) (-radius * 0.35), (int) (radius * 0.4), (int) (radius * 0.4));
                g.fillOval((int) (radius * 0.05), (int) (-radius * 0.35), (int) (radius * 0.4), (int) (radius * 0.4));
                g.setColor(new Color(150, 138, 112));
                g.drawOval((int) -radius, (int) (-radius * 0.75), (int) (radius * 1.7), (int) (radius * 1.5));
                break;
            case CRATE:
                g.setColor(new Color(150, 108, 66));
                g.fillRect((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                g.setColor(new Color(104, 72, 42));
                g.drawRect((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                g.drawLine((int) -radius, (int) -radius, (int) radius, (int) radius);
                g.drawLine((int) radius, (int) -radius, (int) -radius, (int) radius);
                break;
            case TUMBLING_BONE:
                g.setColor(new Color(230, 220, 198));
                g.fillRoundRect((int) -radius, (int) (-radius * 0.35), (int) (radius * 2), (int) (radius * 0.7), 8, 8);
                g.fillOval((int) -radius - 3, (int) (-radius * 0.5), (int) (radius * 0.7), (int) (radius * 0.7));
                g.fillOval((int) (radius * 0.3), (int) (-radius * 0.5), (int) (radius * 0.7), (int) (radius * 0.7));
                g.setColor(new Color(180, 166, 138));
                g.drawRoundRect((int) -radius, (int) (-radius * 0.35), (int) (radius * 2), (int) (radius * 0.7), 8, 8);
                break;
            default:
                break;
        }
        g.setTransform(old);
    }
}

/** Purely decorative dig-site dressing around the outer rim: no collision. */
class SceneDecor {
    enum Kind { SPINE, TENT, DIG_PIT, FLAG }
    final Kind kind;
    final double x, y, lean, scale;

    SceneDecor(Kind kind, double x, double y, double lean, double scale) {
        this.kind = kind; this.x = x; this.y = y; this.lean = lean; this.scale = scale;
    }

    void draw(Graphics2D g) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(lean);
        g.scale(scale, scale);
        switch (kind) {
            case SPINE:
                g.setColor(new Color(220, 208, 182, 220));
                for (int i = -3; i <= 3; i++) {
                    g.fillOval(i * 12 - 5, (int) (Math.abs(i) * 1.5) - 5, 12, 10);
                }
                g.setColor(new Color(170, 156, 128, 220));
                g.drawLine(-40, 0, 40, 0);
                break;
            case TENT:
                g.setColor(new Color(196, 158, 108, 220));
                Path2D.Double tent = new Path2D.Double();
                tent.moveTo(-22, 10);
                tent.lineTo(0, -34);
                tent.lineTo(22, 10);
                tent.closePath();
                g.fill(tent);
                g.setColor(new Color(140, 106, 68, 220));
                g.draw(tent);
                g.setColor(new Color(90, 66, 40, 220));
                g.fillRect(-6, -6, 12, 16);
                break;
            case DIG_PIT:
                g.setColor(new Color(120, 96, 68, 200));
                g.fillOval(-20, -13, 40, 26);
                g.setColor(new Color(70, 54, 36, 220));
                g.fillOval(-13, -8, 26, 16);
                g.setColor(new Color(150, 130, 100, 200));
                g.drawOval(-20, -13, 40, 26);
                break;
            case FLAG:
                g.setColor(new Color(120, 100, 78, 220));
                g.fillRect(-2, -40, 4, 40);
                g.setColor(new Color(190, 60, 50, 220));
                g.fillRect(2, -40, 20, 12);
                break;
            default:
                break;
        }
        g.setTransform(old);
    }
}

/**
 * Signature hazard: a patch of ground that cycles DORMANT -> WARNING (visible
 * cracking with a brief wobble) -> OPEN (a real sinkhole that must be dodged)
 * -> CLOSING -> back to DORMANT. Many pits are seeded around the track, each
 * on its own independent, randomized timer, so the set of currently
 * dangerous spots keeps shifting over the course of a race even though the
 * candidate spots themselves are fixed on-road locations.
 */
class SinkholePit {
    enum State { DORMANT, WARNING, OPEN, CLOSING }

    final double x, y, radius;
    State state = State.DORMANT;
    private double timer;
    private double wobblePhase;
    private final Random rnd = new Random();

    SinkholePit(double x, double y, double radius, double initialDormant) {
        this.x = x; this.y = y; this.radius = radius;
        this.timer = initialDormant;
        this.wobblePhase = Math.random() * Math.PI * 2;
    }

    double collisionRadius() { return radius * 0.72; }

    void update(double dt) {
        wobblePhase += dt * 9;
        timer -= dt;
        if (timer <= 0) {
            switch (state) {
                case DORMANT:
                    state = State.WARNING;
                    timer = 1.3 + rnd.nextDouble() * 0.7;
                    break;
                case WARNING:
                    state = State.OPEN;
                    timer = 3.5 + rnd.nextDouble() * 2.5;
                    break;
                case OPEN:
                    state = State.CLOSING;
                    timer = 1.0;
                    break;
                case CLOSING:
                    state = State.DORMANT;
                    timer = 4.0 + rnd.nextDouble() * 5.0;
                    break;
            }
        }
    }

    void draw(Graphics2D g) {
        switch (state) {
            case DORMANT:
                drawCrackPatch(g, 0.3);
                break;
            case WARNING: {
                double warnFrac = 1 - Util.clamp(timer / 2.0, 0, 1);
                double wobble = Math.sin(wobblePhase) * 3 * warnFrac;
                drawCrackPatch(g, 0.4 + 0.5 * warnFrac);
                g.setColor(new Color(90, 60, 40, (int) (140 * warnFrac)));
                g.setStroke(new BasicStroke(2));
                for (int i = 0; i < 6; i++) {
                    double ang = i * Math.PI / 3 + wobblePhase * 0.1;
                    double len = radius * (0.5 + 0.4 * warnFrac);
                    g.draw(new Line2D.Double(x, y, x + Math.cos(ang) * len + wobble, y + Math.sin(ang) * len));
                }
                g.setStroke(new BasicStroke(1));
                break;
            }
            case OPEN: {
                RadialGradientPaint rp = new RadialGradientPaint(new Point2D.Double(x, y), (float) radius,
                        new float[]{0f, 0.7f, 1f},
                        new Color[]{new Color(22, 15, 10), new Color(48, 34, 22), new Color(90, 68, 46)});
                g.setPaint(rp);
                g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
                g.setColor(new Color(64, 46, 30));
                g.setStroke(new BasicStroke(3));
                g.draw(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
                g.setStroke(new BasicStroke(1));
                break;
            }
            case CLOSING: {
                double closeFrac = Util.clamp(timer / 1.0, 0, 1);
                double r = radius * closeFrac;
                if (r > 1) {
                    RadialGradientPaint rp = new RadialGradientPaint(new Point2D.Double(x, y), (float) r,
                            new float[]{0f, 1f},
                            new Color[]{new Color(32, 22, 15), new Color(78, 60, 42)});
                    g.setPaint(rp);
                    g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
                }
                drawCrackPatch(g, 0.4 * closeFrac);
                break;
            }
        }
    }

    private void drawCrackPatch(Graphics2D g, double alpha) {
        int a = (int) Util.clamp(alpha * 160, 0, 200);
        if (a <= 2) return;
        g.setColor(new Color(160, 128, 92, a));
        g.fill(new Ellipse2D.Double(x - radius * 0.8, y - radius * 0.6, radius * 1.6, radius * 1.2));
        g.setColor(new Color(96, 70, 46, a));
        g.setStroke(new BasicStroke(1.4f));
        for (int i = 0; i < 5; i++) {
            double ang = i * (Math.PI * 2 / 5) + 0.4;
            g.draw(new Line2D.Double(x, y, x + Math.cos(ang) * radius * 0.7, y + Math.sin(ang) * radius * 0.7));
        }
        g.setStroke(new BasicStroke(1));
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

    // Sinkhole "stuck" state: hard stop for a short duration, then a nudge back out.
    double stuckTimer = 0;
    private double stuckPitX, stuckPitY, climbOutRadius;

    final Color bodyColor;
    final Color trimColor;
    final String label;
    final ParticleSystem particles;

    private static final double MAX_SPEED = 420;
    private static final double MAX_REVERSE = -150;
    private static final double ACCEL = 300;
    private static final double BRAKE = 540;
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

    boolean isStuck() { return stuckTimer > 0; }

    void update(double dt, InputState in, Track track) {
        justLapped = false;
        if (!alive) {
            respawnTimer -= dt;
            if (respawnTimer <= 0) respawn(track);
            return;
        }

        if (stuckTimer > 0) {
            stuckTimer -= dt;
            speed = 0;
            vx *= 0.8; vy *= 0.8;
            x += vx * dt;
            y += vy * dt;
            if (Math.random() < 0.5) {
                particles.add(new Particle(x + (Math.random() - 0.5) * 10, y + (Math.random() - 0.5) * 10,
                        (Math.random() - 0.5) * 30, -20 - Math.random() * 30, 0.4 + Math.random() * 0.3,
                        5 + Math.random() * 4, 6, new Color(178, 148, 108, 150), Particle.ParticleKind.DUST));
            }
            if (stuckTimer <= 0) {
                double dx = x - stuckPitX, dy = y - stuckPitY;
                double d = Math.hypot(dx, dy);
                if (d < 0.001) { dx = Math.cos(angle); dy = Math.sin(angle); d = 1; }
                x = stuckPitX + dx / d * climbOutRadius;
                y = stuckPitY + dy / d * climbOutRadius;
                vx = 0; vy = 0;
            }
            updateBoost();
            trackLapProgress(track);
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

        trackLapProgress(track);

        updateBoost();
        emitDust(dt, onTrack);
    }

    private void trackLapProgress(Track track) {
        double s = track.progress(x, y);
        double delta = s - lastArcLen;
        if (delta < -track.totalLength / 2) delta += track.totalLength;
        else if (delta > track.totalLength / 2) delta -= track.totalLength;
        unwrappedDistance += delta;
        lastArcLen = s;
        int newLap = Math.max(0, (int) Math.floor(unwrappedDistance / track.totalLength));
        if (newLap > lap) justLapped = true;
        lap = newLap;
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
        Color c = onTrack ? new Color(224, 210, 180, 120) : new Color(196, 170, 120, 160);
        for (int i = 0; i < (onTrack ? 1 : 2); i++) {
            double jx = (Math.random() - 0.5) * 10;
            double jy = (Math.random() - 0.5) * 10;
            particles.add(new Particle(rearX + jx, rearY + jy,
                    -vx * 0.15 + (Math.random() - 0.5) * 20, -vy * 0.15 + (Math.random() - 0.5) * 20,
                    0.5 + Math.random() * 0.4, 6 + Math.random() * 6, 8, c, Particle.ParticleKind.DUST));
        }
        if (boosting) {
            particles.add(new Particle(rearX, rearY, -vx * 0.3, -vy * 0.3, 0.35, 10, 6,
                    new Color(255, 170, 70, 200), Particle.ParticleKind.SPARK));
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
                    Math.random() < 0.5 ? new Color(210, 170, 110) : new Color(120, 100, 80), Particle.ParticleKind.SMOKE));
        }
    }

    private void respawn(Track track) {
        alive = true;
        health = 60;
        speed = 0; vx = 0; vy = 0;
        stuckTimer = 0;
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

    /** Called on driving into an OPEN sinkhole: hard stop + damage + a timed stuck state before climbing back out. */
    void stickInSinkhole(double duration, double pitX, double pitY, double climbOutDist) {
        if (stuckTimer <= 0) {
            stuckTimer = duration;
            stuckPitX = pitX; stuckPitY = pitY;
            climbOutRadius = climbOutDist;
            speed = 0; vx = 0; vy = 0;
        }
    }

    void draw(Graphics2D g) {
        if (!alive) return;
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(angle);

        if (boosting) {
            g.setColor(new Color(255, 170, 70, 200));
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
        g.setColor(new Color(255, 230, 180));
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

        if (isStuck()) {
            g.setColor(new Color(255, 214, 70));
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.drawString("STUCK!", (int) x - 20, (int) y - 32);
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

    InputState think(double dt, List<Obstacle> obstacles, List<SinkholePit> sinkholes) {
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

        for (SinkholePit p : sinkholes) {
            if (p.state != SinkholePit.State.OPEN && p.state != SinkholePit.State.WARNING) continue;
            double dx = p.x - car.x, dy = p.y - car.y;
            double dist = Math.hypot(dx, dy);
            double threat = p.state == SinkholePit.State.OPEN ? 115 : 65;
            if (dist < threat) {
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
    private final List<SceneDecor> decor = new ArrayList<>();
    private final List<SinkholePit> sinkholes = new ArrayList<>();
    private final List<Path2D.Double> groundCracks = new ArrayList<>();
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
        track = new Track(W / 2.0, H / 2.0 - 10, 460, 225, 2.3, 150);
        int n = track.centerline.size();

        groundCracks.clear();
        Random cr = new Random(11);
        for (int i = 0; i < 140; i++) {
            double gx = cr.nextDouble() * W;
            double gy = H * 0.4 + cr.nextDouble() * H * 0.6;
            double len = 10 + cr.nextDouble() * 22;
            double ang = cr.nextDouble() * Math.PI * 2;
            Path2D.Double crack = new Path2D.Double();
            crack.moveTo(gx, gy);
            double cx = gx, cy = gy;
            int segs = 2 + cr.nextInt(2);
            for (int s = 0; s < segs; s++) {
                cx += Math.cos(ang) * (len / segs);
                cy += Math.sin(ang) * (len / segs);
                ang += (cr.nextDouble() - 0.5) * 1.1;
                crack.lineTo(cx, cy);
            }
            groundCracks.add(crack);
        }

        obstacles.clear();
        Random r = new Random(42);
        for (int i = 0; i < n; i += 11) {
            if (r.nextDouble() < 0.5) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 20 + r.nextDouble() * 55;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.BONE_PILE : Obstacle.Kind.RIB_ARCH;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(10)));
            }
        }
        int[] hazardIdx = {45, 115, 185, 255, 325, 395, 465, 535};
        for (int idx : hazardIdx) {
            if (idx >= n) continue;
            double off = (r.nextDouble() - 0.5) * track.trackWidth * 0.5;
            Point2D.Double p = track.pointAtOffset(idx, off);
            Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.SKULL_ROCK : Obstacle.Kind.CRATE;
            obstacles.add(new Obstacle(k, p.x, p.y, 15));
        }
        for (int i = 0; i < 3; i++) {
            Obstacle d = new Obstacle(Obstacle.Kind.TUMBLING_BONE, r.nextInt(W), r.nextInt(H), 11);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 35 + r.nextDouble() * 35;
            d.vx = Math.cos(a) * sp;
            d.vy = Math.sin(a) * sp;
            obstacles.add(d);
        }

        decor.clear();
        Random rr = new Random(99);
        for (int i = 0; i < n; i += 18) {
            Point2D.Double p = track.pointAtOffset(i, -track.innerSign * (track.halfWidth + 50 + rr.nextInt(30)));
            double lean = (rr.nextDouble() - 0.5) * 0.3;
            double scale = 0.8 + rr.nextDouble() * 0.5;
            double roll = rr.nextDouble();
            SceneDecor.Kind kind = roll < 0.35 ? SceneDecor.Kind.SPINE
                    : roll < 0.55 ? SceneDecor.Kind.TENT
                    : roll < 0.8 ? SceneDecor.Kind.DIG_PIT
                    : SceneDecor.Kind.FLAG;
            decor.add(new SceneDecor(kind, p.x, p.y, lean, scale));
        }

        sinkholes.clear();
        int pitCount = 9;
        Random sr = new Random(7);
        for (int i = 0; i < pitCount; i++) {
            int idx = (i * n) / pitCount;
            double off = (i % 3 - 1) * 32;
            Point2D.Double p = track.pointAtOffset(idx, off);
            double initialDormant = sr.nextDouble() * 6.0 + 0.5;
            sinkholes.add(new SinkholePit(p.x, p.y, 30 + sr.nextInt(8), initialDormant));
        }
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-22);
        Point2D.Double p2pos = track.startPosition(22);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(196, 84, 46), new Color(240, 220, 180), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(88, 108, 116), new Color(220, 214, 196), vsAI ? "AI" : "2", particles);
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
        for (SinkholePit p : sinkholes) {
            p.update(dt);
            if (p.state == SinkholePit.State.OPEN && Math.random() < 0.06) {
                particles.add(new Particle(p.x + (Math.random() - 0.5) * p.radius, p.y + (Math.random() - 0.5) * p.radius,
                        (Math.random() - 0.5) * 8, -14 - Math.random() * 10, 0.6 + Math.random() * 0.4,
                        4 + Math.random() * 3, 3, new Color(120, 95, 70, 130), Particle.ParticleKind.DUST));
            }
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
            in2 = aiDriver.think(dt, obstacles, sinkholes);
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
        resolveSinkholeCollision(p1);
        resolveSinkholeCollision(p2);

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
                        new Color(255, 210, 110), Particle.ParticleKind.SPARK));
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
            if (o.kind == Obstacle.Kind.TUMBLING_BONE) {
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

    /** Signature hazard collision: driving into an OPEN sinkhole hard-stops and stucks the car before it climbs out. */
    private void resolveSinkholeCollision(Car c) {
        if (!c.alive || c.isStuck()) return;
        for (SinkholePit p : sinkholes) {
            if (p.state != SinkholePit.State.OPEN) continue;
            double dist = Math.hypot(c.x - p.x, c.y - p.y);
            if (dist < p.collisionRadius() + c.radius() * 0.4) {
                c.damage(22);
                c.stickInSinkhole(1.3 + Math.random() * 0.4, p.x, p.y, p.radius + c.radius() + 10);
                for (int i = 0; i < 18; i++) {
                    double ang = Math.random() * Math.PI * 2;
                    double sp = 60 + Math.random() * 120;
                    particles.add(new Particle(c.x, c.y, Math.cos(ang) * sp, Math.sin(ang) * sp,
                            0.4 + Math.random() * 0.4, 4 + Math.random() * 4, 6,
                            new Color(150, 120, 85), Particle.ParticleKind.DEBRIS));
                }
                break;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D sg = sceneBuffer.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBackground(sg);
        for (SceneDecor d : decor) d.draw(sg);
        track.draw(sg);
        for (Obstacle o : obstacles) o.draw(sg);
        for (SinkholePit p : sinkholes) p.draw(sg);
        particles.draw(sg);
        for (Car c : cars) c.draw(sg);
        sg.dispose();

        applyHeatShimmer(sceneBuffer);

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

    private void applyHeatShimmer(BufferedImage img) {
        int top = (int) (H * 0.34), bandH = 34;
        for (int y = top; y < top + bandH && y < H; y++) {
            double t = (y - top) / (double) bandH;
            int shift = (int) (Math.sin(y * 0.25 + timeAccum * 3.2) * 3 * (1 - t));
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
        g.setPaint(new GradientPaint(0, 0, new Color(224, 172, 118), 0, (float) (H * 0.4), new Color(236, 208, 168)));
        g.fillRect(0, 0, W, (int) (H * 0.4));

        double pulse = 1 + 0.03 * Math.sin(timeAccum * 1.5);
        int sunR = (int) (52 * pulse);
        g.setPaint(new RadialGradientPaint(new Point(W - 190, 100), sunR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(255, 236, 200, 210), new Color(236, 180, 120, 90), new Color(236, 180, 120, 0)}));
        g.fillOval(W - 190 - sunR * 3, 100 - sunR * 3, sunR * 6, sunR * 6);
        g.setColor(new Color(255, 244, 220));
        g.fillOval(W - 190 - sunR / 2, 100 - sunR / 2, sunR, sunR);

        // Distant mesa / excavation-ridge skyline instead of mountains.
        g.setColor(new Color(178, 138, 100));
        Path2D.Double ridge = new Path2D.Double();
        ridge.moveTo(-50, H * 0.4);
        double wx = -50;
        Random wr = new Random(5);
        while (wx < W + 50) {
            double plateauW = 40 + wr.nextInt(70);
            double plateauH = H * 0.4 - (30 + wr.nextInt(45));
            ridge.lineTo(wx, plateauH);
            ridge.lineTo(wx + plateauW, plateauH);
            wx += plateauW + 14 + wr.nextInt(18);
        }
        ridge.lineTo(W + 50, H * 0.4);
        ridge.closePath();
        g.fill(ridge);

        g.setPaint(new GradientPaint(0, (float) (H * 0.4), new Color(214, 198, 168), 0, H, new Color(178, 156, 118)));
        g.fillRect(0, (int) (H * 0.4), W, (int) (H * 0.6));

        g.setColor(new Color(120, 95, 65, 60));
        for (Path2D.Double crack : groundCracks) g.draw(crack);

        // Warm sepia lighting wash.
        g.setColor(new Color(210, 150, 90, 22));
        g.fillRect(0, 0, W, H);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(224, 178, 90));
        g.setFont(new Font("SansSerif", Font.BOLD, 52));
        centerText(g, "FOSSIL FLATS 500", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Paleontology dig-site racing — mind the sinkhole pits", H / 2 - 90);

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
