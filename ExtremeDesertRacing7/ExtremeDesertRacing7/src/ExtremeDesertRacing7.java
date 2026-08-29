import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ExtremeDesertRacing7 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Neon Dust Hex Circuit — Extreme Desert Racing");
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

    static int clampInt(int v) {
        return Math.max(0, Math.min(255, v));
    }

    static double normalizeAngle(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }

    static Point2D.Double lerp(Point2D.Double a, Point2D.Double b, double t) {
        return new Point2D.Double(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
    }

    static Point2D.Double quadBezier(Point2D.Double p0, Point2D.Double c, Point2D.Double p1, double t) {
        double u = 1 - t;
        double x = u * u * p0.x + 2 * u * t * c.x + t * t * p1.x;
        double y = u * u * p0.y + 2 * u * t * c.y + t * t * p1.y;
        return new Point2D.Double(x, y);
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

    enum ParticleKind { DUST, SMOKE, SPARK, DEBRIS, SAND, FIREWORK }

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
        } else if (kind == ParticleKind.SAND) {
            vx *= 0.9;
            vy *= 0.9;
            vy -= 6 * dt; // churned grains drift slightly upward before settling
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

    void spawnFirework(double x, double y) {
        Color[] palette = {
                new Color(80, 240, 255), new Color(255, 70, 220),
                new Color(255, 250, 220), new Color(190, 120, 255)
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
            } else {
                g.fill(new Ellipse2D.Double(p.x - s / 2, p.y - s / 2, s, s));
            }
            g.setComposite(old);
        }
    }
}

/**
 * Signature hazard: a soft-ground patch. Driving into one sharply cuts the
 * car's top speed and applies strong extra drag (well beyond the mere
 * off-road friction penalty). Escaping requires either fighting through for
 * a couple of seconds or spending boost fuel to break free faster.
 */
class QuicksandPit {
    final double x, y, radius;
    final double phase;

    QuicksandPit(double x, double y, double radius) {
        this.x = x; this.y = y; this.radius = radius;
        this.phase = Math.random() * Math.PI * 2;
    }

    boolean contains(double px, double py) {
        return Point2D.distance(px, py, x, y) < radius;
    }

    void draw(Graphics2D g, double t) {
        double pulse = 0.5 + 0.5 * Math.sin(t * 1.3 + phase);
        g.setColor(new Color(118, 92, 54, 205));
        g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
        double r2 = radius * (0.58 + 0.14 * pulse);
        g.setColor(new Color(88, 66, 36, 190));
        g.fill(new Ellipse2D.Double(x - r2, y - r2, r2 * 2, r2 * 2));
        g.setColor(new Color(150, 118, 66, 130));
        g.setStroke(new BasicStroke(2));
        double r3 = radius * (0.82 + 0.08 * pulse);
        g.draw(new Ellipse2D.Double(x - r3, y - r3, r3 * 2, r3 * 2));
        g.setStroke(new BasicStroke(1));
    }
}

/**
 * Rounded hexagon centerline built from six vertices whose corners are cut
 * back and joined with a quadratic-bezier fillet (control point = original
 * vertex). Each fillet consumes only 35% of each adjacent edge, so the two
 * fillets sharing an edge never meet — the loop stays a single simple,
 * non-self-intersecting closed curve (fillets are convex arcs bulging only
 * as far as the original convex vertex, never crossing a neighboring arc).
 */
class Track {
    final double centerX, centerY, halfWidth, trackWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;
    double innerSign = 1.0;

    Track(double centerX, double centerY, double hexR, double scaleX, double scaleY, double trackWidth) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        build(hexR, scaleX, scaleY);
    }

    private void build(double hexR, double scaleX, double scaleY) {
        Point2D.Double[] v = new Point2D.Double[6];
        for (int i = 0; i < 6; i++) {
            double rad = i * Math.PI / 3.0;
            v[i] = new Point2D.Double(centerX + Math.cos(rad) * hexR * scaleX,
                    centerY + Math.sin(rad) * hexR * scaleY);
        }

        Point2D.Double[] A = new Point2D.Double[6];
        Point2D.Double[] B = new Point2D.Double[6];
        for (int i = 0; i < 6; i++) {
            Point2D.Double prev = v[(i + 5) % 6];
            Point2D.Double curr = v[i];
            Point2D.Double next = v[(i + 1) % 6];
            double dIn = curr.distance(prev);
            double dOut = curr.distance(next);
            double fin = dIn * 0.35;
            double fout = dOut * 0.35;
            double uix = (curr.x - prev.x) / dIn, uiy = (curr.y - prev.y) / dIn;
            double uox = (next.x - curr.x) / dOut, uoy = (next.y - curr.y) / dOut;
            A[i] = new Point2D.Double(curr.x - uix * fin, curr.y - uiy * fin);
            B[i] = new Point2D.Double(curr.x + uox * fout, curr.y + uoy * fout);
        }

        int straightSamples = 20;
        int bezierSamples = 30;
        for (int i = 0; i < 6; i++) {
            Point2D.Double bPrev = B[(i + 5) % 6];
            for (int s = 0; s < straightSamples; s++) {
                double t = s / (double) straightSamples;
                centerline.add(Util.lerp(bPrev, A[i], t));
            }
            for (int s = 0; s < bezierSamples; s++) {
                double t = s / (double) bezierSamples;
                centerline.add(Util.quadBezier(A[i], v[i], B[i], t));
            }
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

        int westIdx = n / 2;
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

    Point2D.Double inwardPoint(int index, double depth) {
        return pointAtOffset(index, innerSign * (halfWidth + depth));
    }

    Point2D.Double startPosition(double laneOffset) {
        return pointAtOffset(2, laneOffset);
    }

    double startAngle() {
        Point2D.Double a = centerline.get(0);
        Point2D.Double b = centerline.get(5);
        return Math.atan2(b.y - a.y, b.x - a.x);
    }

    private void strokeGlow(Graphics2D g, Shape shape, Color c) {
        int[] widths = {14, 9, 5};
        int[] alphas = {28, 60, 120};
        for (int i = 0; i < widths.length; i++) {
            g.setStroke(new BasicStroke(widths[i], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alphas[i]));
            g.draw(shape);
        }
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(new Color(235, 255, 255, 235));
        g.draw(shape);
    }

    void draw(Graphics2D g, double t) {
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
        g.setColor(new Color(44, 36, 56));
        g.fill(road);

        g.setColor(new Color(70, 58, 84, 110));
        for (int i = 0; i < centerline.size(); i += 6) {
            Point2D.Double o = pointAtOffset(i, halfWidth);
            Point2D.Double in = pointAtOffset(i, -halfWidth);
            g.draw(new Line2D.Double(o, in));
        }

        strokeGlow(g, outer, new Color(50, 235, 255));
        strokeGlow(g, inner, new Color(255, 60, 220));

        Path2D.Double mid = new Path2D.Double();
        for (int i = 0; i < centerline.size(); i++) {
            Point2D.Double p = centerline.get(i);
            if (i == 0) mid.moveTo(p.x, p.y); else mid.lineTo(p.x, p.y);
        }
        mid.closePath();
        float dashPhase = (float) ((t * 44) % 36);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10, new float[]{16, 20}, dashPhase));
        int a = (int) (140 + 100 * Math.sin(t * 3));
        g.setColor(new Color(255, 235, 140, Util.clampInt(a)));
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
    enum Kind { RUBBLE, PILLAR_STUMP, WRECK, DEBRIS }

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
            case RUBBLE:
                g.setColor(new Color(112, 100, 92));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(80, 70, 64));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.fillOval((int) (-radius * 0.4), (int) (-radius * 0.3), (int) (radius), (int) (radius * 0.8));
                break;
            case PILLAR_STUMP:
                g.setColor(new Color(150, 128, 150));
                g.fillRoundRect((int) (-radius * 0.6), (int) -radius, (int) (radius * 1.2), (int) (radius * 2), 6, 6);
                g.setColor(new Color(96, 80, 104));
                for (int i = -1; i <= 1; i++) {
                    g.drawLine((int) (-radius * 0.5), (int) (i * radius * 0.6), (int) (radius * 0.5), (int) (i * radius * 0.6));
                }
                break;
            case WRECK:
                g.setColor(new Color(58, 50, 62));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(new Color(200, 60, 160));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) (radius * 0.25));
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                break;
            case DEBRIS:
                g.setColor(new Color(130, 118, 128));
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

/** Purely decorative glowing rave light pole around the rim: no collision. */
class NeonPole {
    final double x, y;
    final Color glow;

    NeonPole(double x, double y, Color glow) {
        this.x = x; this.y = y; this.glow = glow;
    }

    void draw(Graphics2D g, double t) {
        double pulse = 0.6 + 0.4 * Math.sin(t * 2.4 + x * 0.01);
        g.setColor(new Color(30, 26, 40));
        g.fillRect((int) x - 3, (int) y - 46, 6, 46);
        for (int i = 3; i >= 1; i--) {
            int alpha = (int) (55 * pulse / i);
            g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), Util.clampInt(alpha)));
            int rad = 6 * i;
            g.fillOval((int) x - rad, (int) (y - 46) - rad, rad * 2, rad * 2);
        }
        g.setColor(Color.WHITE);
        g.fillOval((int) x - 4, (int) (y - 46) - 4, 8, 8);
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

    boolean inSand = false;
    double sandEscapeProgress = 0;
    double sinkDepth = 0;
    private double sandParticleTimer = 0;

    final Color bodyColor;
    final Color trimColor;
    final String label;
    final ParticleSystem particles;

    private static final double MAX_SPEED = 420;
    private static final double MAX_REVERSE = -150;
    private static final double ACCEL = 290;
    private static final double BRAKE = 540;
    private static final double NATURAL_FRICTION = 150;
    private static final double OFFROAD_MULT = 2.2;
    private static final double TURN_RATE = 3.0;
    private static final double GRIP = 7.5;
    private static final double BOOST_MULT = 1.5;
    private static final double RADIUS = 17;

    private static final double SAND_FRICTION_MULT = 5.2;
    private static final double SAND_MAX_SPEED_FRAC = 0.30;
    private static final double SAND_DRAG = 2.3;
    private static final double SAND_ESCAPE_TIME = 2.0;
    private static final double SAND_SINK_MAX = 7.0;

    private double dustTimer = 0;

    Car(double x, double y, double angle, Color bodyColor, Color trimColor, String label, ParticleSystem particles) {
        this.x = x; this.y = y; this.angle = angle;
        this.bodyColor = bodyColor; this.trimColor = trimColor;
        this.label = label;
        this.particles = particles;
    }

    double radius() { return RADIUS; }

    void update(double dt, InputState in, Track track, List<QuicksandPit> pits) {
        justLapped = false;
        if (!alive) {
            respawnTimer -= dt;
            if (respawnTimer <= 0) respawn(track);
            return;
        }
        updateQuicksand(dt, in, pits);
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

    private QuicksandPit findSand(List<QuicksandPit> pits) {
        for (QuicksandPit p : pits) {
            if (p.contains(x, y)) return p;
        }
        return null;
    }

    private void updateQuicksand(double dt, InputState in, List<QuicksandPit> pits) {
        QuicksandPit pit = findSand(pits);
        if (pit != null) {
            if (!inSand) { inSand = true; sandEscapeProgress = 0; }
            boolean fighting = in.boost && boostFuel > 1;
            sandEscapeProgress += dt * (fighting ? 2.4 : 1.0) / SAND_ESCAPE_TIME;
            if (fighting) boostFuel = Math.max(0, boostFuel - 20 * dt);
            sinkDepth = Math.min(SAND_SINK_MAX, sinkDepth + 14 * dt);

            sandParticleTimer -= dt;
            if (sandParticleTimer <= 0) {
                sandParticleTimer = 0.05;
                for (int i = 0; i < 2; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double sp = 10 + Math.random() * 30;
                    particles.add(new Particle(x + (Math.random() - 0.5) * 14, y + (Math.random() - 0.5) * 14,
                            Math.cos(a) * sp, Math.sin(a) * sp - 12,
                            0.35 + Math.random() * 0.3, 4 + Math.random() * 4, 5,
                            new Color(150, 118, 66, 200), Particle.ParticleKind.SAND));
                }
            }

            if (sandEscapeProgress >= 1.0) {
                inSand = false;
                sandEscapeProgress = 0;
                sinkDepth = 0;
                double dir = speed < 0 ? -1 : 1;
                speed = dir * Math.max(Math.abs(speed), MAX_SPEED * SAND_MAX_SPEED_FRAC * 1.5);
                for (int i = 0; i < 14; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double sp = 60 + Math.random() * 120;
                    particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp,
                            0.4 + Math.random() * 0.3, 5, 6,
                            new Color(150, 118, 66, 220), Particle.ParticleKind.SAND));
                }
            }
        } else {
            inSand = false;
            sandEscapeProgress = 0;
            sinkDepth = Math.max(0, sinkDepth - 20 * dt);
        }
    }

    private void applyControls(double dt, InputState in, Track track) {
        boolean onTrack = track.distanceFromCenterline(x, y) < track.halfWidth;
        double frictionMult = onTrack ? 1.0 : OFFROAD_MULT;
        if (inSand) frictionMult = SAND_FRICTION_MULT;
        boosting = in.boost && boostFuel > 5 && in.throttle > 0;
        double topSpeed = MAX_SPEED * (boosting ? BOOST_MULT : 1.0) * (onTrack ? 1.0 : 0.55);
        if (inSand) topSpeed = Math.min(topSpeed, MAX_SPEED * SAND_MAX_SPEED_FRAC);

        if (in.throttle > 0.05) {
            speed += ACCEL * (boosting ? BOOST_MULT : 1.0) * dt;
        } else if (in.throttle < -0.05) {
            if (speed > 10) speed -= BRAKE * dt;
            else speed -= ACCEL * 0.6 * dt;
        } else {
            if (speed > 0) speed = Math.max(0, speed - NATURAL_FRICTION * frictionMult * dt);
            else if (speed < 0) speed = Math.min(0, speed + NATURAL_FRICTION * frictionMult * dt);
        }
        if (!onTrack && speed > 0 && !inSand) speed = Math.max(0, speed - NATURAL_FRICTION * (OFFROAD_MULT - 1) * dt);

        if (inSand) speed *= Math.max(0, 1 - SAND_DRAG * dt);

        speed = Util.clamp(speed, inSand ? -MAX_SPEED * SAND_MAX_SPEED_FRAC : MAX_REVERSE, topSpeed);

        double speedFactor = Util.clamp(Math.abs(speed) / MAX_SPEED, 0.35, 1.0);
        double dir = speed < 0 ? -1 : 1;
        double steerMult = inSand ? 0.55 : 1.0;
        angle += in.steer * TURN_RATE * dt * speedFactor * dir * steerMult;
    }

    private void updateBoost() {
        if (boosting) boostFuel = Math.max(0, boostFuel - 0.7);
        else boostFuel = Math.min(100, boostFuel + 0.23);
    }

    private void emitDust(double dt, boolean onTrack) {
        dustTimer -= dt;
        double spd = Math.hypot(vx, vy);
        if (spd < 30 || dustTimer > 0 || inSand) return;
        dustTimer = onTrack ? 0.06 : 0.03;
        double rearX = x - Math.cos(angle) * radius();
        double rearY = y - Math.sin(angle) * radius();
        Color c = onTrack ? new Color(210, 200, 235, 110) : new Color(180, 150, 120, 150);
        for (int i = 0; i < (onTrack ? 1 : 2); i++) {
            double jx = (Math.random() - 0.5) * 10;
            double jy = (Math.random() - 0.5) * 10;
            particles.add(new Particle(rearX + jx, rearY + jy,
                    -vx * 0.15 + (Math.random() - 0.5) * 20, -vy * 0.15 + (Math.random() - 0.5) * 20,
                    0.5 + Math.random() * 0.4, 6 + Math.random() * 6, 8, c, Particle.ParticleKind.DUST));
        }
        if (boosting) {
            particles.add(new Particle(rearX, rearY, -vx * 0.3, -vy * 0.3, 0.35, 10, 6,
                    new Color(255, 90, 220, 210), Particle.ParticleKind.SPARK));
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
                    Math.random() < 0.5 ? new Color(255, 90, 220) : new Color(90, 90, 90), Particle.ParticleKind.SMOKE));
        }
    }

    private void respawn(Track track) {
        alive = true;
        health = 60;
        speed = 0; vx = 0; vy = 0;
        inSand = false; sandEscapeProgress = 0; sinkDepth = 0;
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
        double drawY = y + sinkDepth;

        if (sinkDepth > 0.5) {
            g.setColor(new Color(90, 70, 40, (int) Util.clamp(sinkDepth * 14, 0, 140)));
            g.fill(new Ellipse2D.Double(x - radius() * 1.3, drawY - radius() * 0.7, radius() * 2.6, radius() * 1.4));
        }

        AffineTransform old = g.getTransform();
        g.translate(x, drawY);
        g.rotate(angle);

        if (boosting) {
            g.setColor(new Color(255, 120, 230, 200));
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
        g.setColor(new Color(30, 30, 40));
        g.fillRect(14, -4, 8, 2);
        g.fillRect(14, 2, 8, 2);
        g.setColor(new Color(255, 220, 250));
        g.fillOval(14, -6, 4, 4);
        g.fillOval(14, 2, 4, 4);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.drawString(label, -5, 4);

        g.setTransform(old);

        double hpFrac = Util.clamp(health / 100.0, 0, 1);
        g.setColor(Color.DARK_GRAY);
        g.fillRect((int) x - 16, (int) drawY - 26, 32, 4);
        g.setColor(hpFrac > 0.5 ? new Color(90, 220, 140) : hpFrac > 0.25 ? Color.ORANGE : Color.RED);
        g.fillRect((int) x - 16, (int) drawY - 26, (int) (32 * hpFrac), 4);

        if (inSand) {
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.setColor(new Color(255, 220, 120));
            String txt = "STUCK!";
            g.drawString(txt, (int) x - 16, (int) drawY - 32);
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

    InputState think(double dt, List<Obstacle> obstacles, List<QuicksandPit> pits) {
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

        for (QuicksandPit p : pits) {
            double dx = p.x - car.x, dy = p.y - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < p.radius + 90) {
                double angToObs = Util.normalizeAngle(Math.atan2(dy, dx) - car.angle);
                if (Math.abs(angToObs) < 1.0) {
                    diff += angToObs < 0 ? 0.7 : -0.7;
                }
            }
        }

        in.steer = Util.clamp(diff * 1.6, -1, 1);
        in.throttle = Math.abs(diff) > 1.3 ? 0.2 : 1.0;

        boostCooldown -= dt;
        if (car.inSand) {
            in.boost = true;
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

    private enum State { MENU, COUNTDOWN, RACING, FINISHED }

    private State state = State.MENU;
    private boolean vsAI = true;
    private Thread thread;
    private volatile boolean running = true;

    private final Set<Integer> keys = new HashSet<>();
    private Track track;
    private List<Car> cars = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<QuicksandPit> pits = new ArrayList<>();
    private final List<NeonPole> poles = new ArrayList<>();
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
        track = new Track(W / 2.0, H / 2.0 + 20, 245, 1.5, 1.0, 110);
        int n = track.centerline.size();

        obstacles.clear();
        Random r = new Random(42);
        for (int i = 0; i < n; i += 13) {
            if (r.nextDouble() < 0.5) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 18 + r.nextDouble() * 45;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.RUBBLE : Obstacle.Kind.PILLAR_STUMP;
                obstacles.add(new Obstacle(k, p.x, p.y, 11 + r.nextInt(9)));
            }
        }
        for (int i = 0; i < 2; i++) {
            Obstacle d = new Obstacle(Obstacle.Kind.DEBRIS, r.nextInt(W), r.nextInt(H), 12);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 35 + r.nextDouble() * 35;
            d.vx = Math.cos(a) * sp;
            d.vy = Math.sin(a) * sp;
            obstacles.add(d);
        }

        // Quicksand pits: a mix of on-track shoulder pits and near-track patches.
        pits.clear();
        int[] pitIdx = {14, 62, 118, 168, 222, 268, 310};
        double[] pitOff = {8, -30, 22, -10, 34, -24, 0};
        double[] pitRad = {42, 50, 44, 55, 40, 48, 46};
        for (int k = 0; k < pitIdx.length; k++) {
            int idx = pitIdx[k] % n;
            Point2D.Double p = track.pointAtOffset(idx, pitOff[k]);
            pits.add(new QuicksandPit(p.x, p.y, pitRad[k]));
        }

        // Decorative rave light poles ringing the outer rim.
        poles.clear();
        Random pr = new Random(7);
        Color[] glowColors = {new Color(60, 230, 255), new Color(255, 70, 210), new Color(190, 120, 255)};
        for (int i = 0; i < n; i += 24) {
            Point2D.Double p = track.pointAtOffset(i, -track.innerSign * (track.halfWidth + 46 + pr.nextInt(20)));
            poles.add(new NeonPole(p.x, p.y, glowColors[pr.nextInt(glowColors.length)]));
        }
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-22);
        Point2D.Double p2pos = track.startPosition(22);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(60, 220, 255), new Color(20, 40, 60), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(255, 80, 200), new Color(60, 20, 50), vsAI ? "AI" : "2", particles);
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
            in2 = aiDriver.think(dt, obstacles, pits);
        } else {
            in2 = new InputState();
            in2.throttle = keys.contains(KeyEvent.VK_UP) ? 1 : keys.contains(KeyEvent.VK_DOWN) ? -1 : 0;
            in2.steer = keys.contains(KeyEvent.VK_LEFT) ? -1 : keys.contains(KeyEvent.VK_RIGHT) ? 1 : 0;
            in2.boost = keys.contains(KeyEvent.VK_ENTER);
        }

        p1.update(dt, in1, track, pits);
        p2.update(dt, in2, track, pits);

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
                        new Color(255, 220, 90), Particle.ParticleKind.SPARK));
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

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D sg = sceneBuffer.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBackground(sg);
        for (NeonPole p : poles) p.draw(sg, timeAccum);
        track.draw(sg, timeAccum);
        for (Obstacle o : obstacles) o.draw(sg);
        for (QuicksandPit p : pits) p.draw(sg, timeAccum);
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
        g.setPaint(new GradientPaint(0, 0, new Color(34, 12, 58), 0, (float) (H * 0.56), new Color(255, 112, 64)));
        g.fillRect(0, 0, W, (int) (H * 0.56));

        double pulse = 1 + 0.04 * Math.sin(timeAccum * 1.4);
        int glowR = (int) (85 * pulse);
        g.setPaint(new RadialGradientPaint(new Point(220, 130), glowR * 3,
                new float[]{0f, 0.45f, 1f},
                new Color[]{new Color(255, 130, 230, 210), new Color(170, 90, 230, 90), new Color(170, 90, 230, 0)}));
        g.fillOval(220 - glowR * 3, 130 - glowR * 3, glowR * 6, glowR * 6);
        g.setColor(new Color(255, 235, 250));
        g.fillOval(220 - glowR / 2, 130 - glowR / 2, glowR, glowR);

        drawDuneLayer(g, H * 0.50, 26, new Color(60, 34, 60, 200), 0.010, 0.3);
        drawDuneLayer(g, H * 0.55, 40, new Color(44, 24, 46, 230), 0.007, 1.7);
        drawDuneLayer(g, H * 0.60, 55, new Color(30, 16, 34, 255), 0.005, 4.1);

        g.setPaint(new GradientPaint(0, (float) (H * 0.55), new Color(52, 34, 60), 0, H, new Color(26, 18, 34)));
        g.fillRect(0, (int) (H * 0.55), W, (int) (H * 0.45));
    }

    private void drawDuneLayer(Graphics2D g, double baseY, double amplitude, Color color, double freq, double phase) {
        Path2D.Double dune = new Path2D.Double();
        dune.moveTo(-50, H);
        dune.lineTo(-50, baseY);
        for (double x = -50; x <= W + 50; x += 12) {
            double y = baseY - amplitude * (0.5 + 0.5 * Math.sin(x * freq + phase));
            dune.lineTo(x, y);
        }
        dune.lineTo(W + 50, H);
        dune.closePath();
        g.setColor(color);
        g.fill(dune);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(90, 235, 255));
        g.setFont(new Font("SansSerif", Font.BOLD, 52));
        centerText(g, "NEON DUST HEX CIRCUIT", H / 2 - 150);
        g.setFont(new Font("SansSerif", Font.BOLD, 19));
        g.setColor(new Color(255, 130, 220));
        centerText(g, "Dusk desert rave racing — beware the quicksand pits", H / 2 - 100);

        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        g.setColor(Color.WHITE);
        centerText(g, "[1] Player vs Player      [2] Player vs Computer", H / 2 - 20);
        centerText(g, "P1: W A S D  +  SPACE boost", H / 2 + 20);
        centerText(g, "P2: Arrow Keys + ENTER boost  (disabled vs Computer)", H / 2 + 46);
        centerText(g, "Quicksand cuts your speed hard — fight through or burn boost to escape", H / 2 + 80);
        centerText(g, "Press 1 or 2 to start", H / 2 + 116);
    }

    private void centerText(Graphics2D g, String s, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (W - fm.stringWidth(s)) / 2, y);
    }

    private void drawCountdown(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 90));
        g.setColor(new Color(255, 235, 90));
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
        g.setColor(hp > 0.5 ? new Color(90, 220, 140) : hp > 0.25 ? Color.ORANGE : Color.RED);
        g.fillRect(x + 10, 40, (int) (170 * hp), 8);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 10, 54, 170, 8);
        g.setColor(new Color(90, 200, 255));
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
