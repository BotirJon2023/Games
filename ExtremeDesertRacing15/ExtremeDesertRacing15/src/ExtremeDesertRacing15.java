import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ExtremeDesertRacing15 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Meteor Crater Circuit — Extreme Desert Racing");
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
        return a + (b - a) * t;
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

    enum ParticleKind { DUST, SMOKE, SPARK, DEBRIS, FLARE, EMBER }

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
            vy += 40 * dt;
            vx *= 0.99;
            vy *= 0.99;
        } else if (kind == ParticleKind.EMBER) {
            vy -= 14 * dt; // embers drift gently upward
            vx *= 0.97;
            vy *= 0.985;
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

class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();

    synchronized void add(Particle p) { particles.add(p); }

    synchronized void update(double dt) {
        particles.removeIf(p -> !p.update(dt));
    }

    void spawnFlare(double x, double y) {
        Color[] palette = {
                new Color(255, 200, 90), new Color(255, 130, 60),
                new Color(255, 250, 220), new Color(120, 200, 255)
        };
        Random r = new Random();
        int count = 30;
        for (int i = 0; i < count; i++) {
            double ang = (Math.PI * 2 * i) / count + (r.nextDouble() - 0.5) * 0.3;
            double sp = 90 + r.nextDouble() * 140;
            Color c = palette[r.nextInt(palette.length)];
            add(new Particle(x, y - 10, Math.cos(ang) * sp, Math.sin(ang) * sp - 40,
                    0.6 + r.nextDouble() * 0.5, 3 + r.nextDouble() * 4, -3, c, Particle.ParticleKind.FLARE));
        }
    }

    synchronized void draw(Graphics2D g) {
        for (Particle p : particles) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.alpha()));
            g.setColor(p.color);
            double s = Math.max(0.5, p.size);
            if (p.kind == Particle.ParticleKind.FLARE) {
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

/** A single twinkling background star. */
class Star {
    final double x, y, r, phase, speed;

    Star(double x, double y, double r, double phase, double speed) {
        this.x = x; this.y = y; this.r = r; this.phase = phase; this.speed = speed;
    }

    void draw(Graphics2D g, double t) {
        double a = 0.35 + 0.55 * Math.abs(Math.sin(t * speed + phase));
        g.setColor(new Color(220, 230, 255, (int) (a * 255)));
        g.fill(new Ellipse2D.Double(x - r / 2, y - r / 2, r, r));
    }
}

/**
 * Wide rectangular superellipse loop: |x/a|^n + |y/b|^n = 1, with a high
 * exponent n giving sharp near-rectangular corners — the ancient, weathered
 * rim of a giant meteor impact crater. Lap counting and edge rendering both
 * reuse the proven technique: a centerline point list with a parallel
 * cumulative arc-length array (robust even off-centerline) and per-point
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
        g.setColor(new Color(72, 78, 90));
        g.fill(road);

        // weathered rock seams across the road
        g.setColor(new Color(54, 58, 70, 130));
        for (int i = 0; i < centerline.size(); i += 6) {
            Point2D.Double o = pointAtOffset(i, halfWidth);
            Point2D.Double in = pointAtOffset(i, -halfWidth);
            g.draw(new Line2D.Double(o, in));
        }

        // glowing ember-orange crater-rim edges (wide soft glow beneath a bright thin core)
        g.setStroke(new BasicStroke(9));
        g.setColor(new Color(220, 110, 40, 70));
        g.draw(outer);
        g.draw(inner);
        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(255, 150, 60, 200));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(150, 190, 230, 140));
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
            g.setColor(i % 2 == 0 ? Color.WHITE : Color.BLACK);
            g.fill(new Rectangle2D.Double(sx - 4, sy - 4, 8, 8));
        }
    }
}

class Obstacle {
    enum Kind { ROCK, SPIRE, FRAGMENT, DEBRIS }

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
            case ROCK:
                g.setColor(new Color(96, 100, 112));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(60, 64, 76));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.fillOval((int) (-radius * 0.4), (int) (-radius * 0.3), (int) (radius), (int) (radius * 0.8));
                break;
            case SPIRE:
                g.setColor(new Color(58, 56, 66));
                g.fillRoundRect((int) (-radius * 0.55), (int) -radius, (int) (radius * 1.1), (int) (radius * 2), 6, 6);
                g.setColor(new Color(230, 120, 50, 160));
                for (int i = -1; i <= 1; i++) {
                    g.drawLine((int) (-radius * 0.45), (int) (i * radius * 0.6), (int) (radius * 0.45), (int) (i * radius * 0.6));
                }
                break;
            case FRAGMENT:
                g.setColor(new Color(44, 40, 46));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(new Color(220, 110, 40, 150));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) (radius * 0.25));
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                break;
            case DEBRIS:
                g.setColor(new Color(110, 114, 124));
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

/** Purely decorative ancient crater scar glowing faintly in the terrain: no collision. */
class GlowScar {
    final double x, y, radius, phase;

    GlowScar(double x, double y, double radius, double phase) {
        this.x = x; this.y = y; this.radius = radius; this.phase = phase;
    }

    void draw(Graphics2D g, double t) {
        double pulse = 0.6 + 0.4 * Math.sin(t * 0.7 + phase);
        float alpha = (float) (0.10 + 0.10 * pulse);
        Point2D center = new Point2D.Double(x, y);
        float[] fractions = {0f, 0.6f, 1f};
        Color[] colors = {
                new Color(255, 130, 50, (int) (alpha * 255)),
                new Color(200, 80, 40, (int) (alpha * 120)),
                new Color(200, 80, 40, 0)
        };
        RadialGradientPaint rgp = new RadialGradientPaint(center, (float) radius, fractions, colors);
        g.setPaint(rgp);
        g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
    }
}

/**
 * A lingering hazard left by a meteor strike: a rough depression in the
 * ground with a glowing ember-orange rim. Cars driving through it are slowed
 * and take light continuous damage. It fades out and the ground resets once
 * its lifetime elapses.
 */
class CraterScar {
    final double x, y, radius;
    final double lifetime, fadeStart, phase;
    double age = 0;

    CraterScar(double x, double y, double radius) {
        this.x = x; this.y = y; this.radius = radius;
        this.lifetime = 9 + Math.random() * 4;
        this.fadeStart = lifetime - 2.5;
        this.phase = Math.random() * Math.PI * 2;
    }

    boolean update(double dt) {
        age += dt;
        return age < lifetime;
    }

    double alpha() {
        if (age < fadeStart) return 1.0;
        return Util.clamp(1 - (age - fadeStart) / (lifetime - fadeStart), 0, 1);
    }

    void draw(Graphics2D g, double t) {
        float a = (float) alpha();
        if (a <= 0.01) return;
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));

        g.setColor(new Color(30, 26, 26));
        g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 1.65));

        double pulse = 0.7 + 0.3 * Math.sin(t * 3 + phase);
        Point2D center = new Point2D.Double(x, y);
        float[] fractions = {0f, 0.7f, 1f};
        Color[] colors = {
                new Color(255, 160, 60, (int) (200 * pulse)),
                new Color(230, 90, 30, 90),
                new Color(230, 90, 30, 0)
        };
        RadialGradientPaint rgp = new RadialGradientPaint(center, (float) (radius * 1.1), fractions, colors);
        g.setPaint(rgp);
        g.fill(new Ellipse2D.Double(x - radius * 1.1, y - radius * 1.1, radius * 2.2, radius * 2.2));

        g.setColor(new Color(255, 140, 50, (int) (160 * pulse)));
        g.setStroke(new BasicStroke(2));
        for (int i = 0; i < 5; i++) {
            double ang = phase + i * (Math.PI * 2 / 5);
            double cx = x + Math.cos(ang) * radius * 0.85;
            double cy = y + Math.sin(ang) * radius * 0.6;
            g.draw(new Line2D.Double(x, y, cx, cy));
        }
        g.setStroke(new BasicStroke(1));
        g.setComposite(old);
    }
}

/**
 * Signature hazard: a meteor streaking down from off-screen with a visible
 * telegraph trail and a pulsing ground warning ring, before slamming into
 * the arena — triggering a radial shockwave push on nearby cars, a brief
 * screen shake, and leaving a lingering CraterScar hazard.
 */
class Meteor {
    final double startX, startY, targetX, targetY;
    final double telegraphDuration;
    final double craterRadius, shockRadius, damageRadius;
    double elapsed = 0;
    boolean impacted = false;
    double flashTimer = 0;
    static final double FLASH_DURATION = 0.35;

    Meteor(double targetX, double targetY) {
        this.targetX = targetX;
        this.targetY = targetY;
        double ang = Math.toRadians(-60 - Math.random() * 60);
        double dist = 850 + Math.random() * 350;
        this.startX = targetX + Math.cos(ang) * dist;
        this.startY = targetY + Math.sin(ang) * dist;
        this.telegraphDuration = 1.4 + Math.random() * 0.5;
        this.craterRadius = 46 + Math.random() * 38;
        this.shockRadius = craterRadius * 3.4;
        this.damageRadius = craterRadius * 1.3;
    }

    double headX() { return Util.lerp(startX, targetX, Util.clamp(elapsed / telegraphDuration, 0, 1)); }
    double headY() { return Util.lerp(startY, targetY, Util.clamp(elapsed / telegraphDuration, 0, 1)); }

    void update(double dt, ParticleSystem particles) {
        if (!impacted) {
            elapsed += dt;
            double hx = headX(), hy = headY();
            if (Math.random() < 0.7) {
                particles.add(new Particle(hx, hy, (Math.random() - 0.5) * 30, (Math.random() - 0.5) * 30,
                        0.25 + Math.random() * 0.25, 3 + Math.random() * 3, 2,
                        new Color(255, 170, 70, 220), Particle.ParticleKind.EMBER));
            }
        } else {
            flashTimer -= dt;
        }
    }

    boolean isFinished() {
        return impacted && flashTimer <= 0;
    }

    void draw(Graphics2D g) {
        if (!impacted) {
            double t = Util.clamp(elapsed / telegraphDuration, 0, 1);
            double hx = headX(), hy = headY();

            g.setStroke(new BasicStroke(3));
            g.setColor(new Color(255, 200, 120, (int) (140 + 90 * t)));
            g.draw(new Line2D.Double(startX, startY, hx, hy));
            g.setStroke(new BasicStroke(1));

            g.setColor(new Color(255, 240, 210, 230));
            g.fill(new Ellipse2D.Double(hx - 6, hy - 6, 12, 12));
            g.setColor(new Color(255, 150, 60, 140));
            g.fill(new Ellipse2D.Double(hx - 11, hy - 11, 22, 22));

            double pulse = 0.5 + 0.5 * Math.sin(elapsed * 11);
            double ringR = craterRadius * (1.7 - 0.6 * t) + pulse * 4;
            g.setStroke(new BasicStroke((float) (2 + 2 * t)));
            g.setColor(new Color(255, 70, 30, (int) (90 + 140 * t)));
            g.draw(new Ellipse2D.Double(targetX - ringR, targetY - ringR * 0.6, ringR * 2, ringR * 1.2));
            g.setStroke(new BasicStroke(1));
        } else {
            double ft = 1 - Util.clamp(flashTimer / FLASH_DURATION, 0, 1);
            float alpha = (float) Util.clamp(1 - ft, 0, 1);
            double flashR = craterRadius * (0.6 + ft * 2.0);
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(new Color(255, 240, 220));
            g.fill(new Ellipse2D.Double(targetX - flashR * 0.4, targetY - flashR * 0.4, flashR * 0.8, flashR * 0.8));
            g.setColor(new Color(255, 140, 50));
            g.setStroke(new BasicStroke(4));
            g.draw(new Ellipse2D.Double(targetX - flashR, targetY - flashR * 0.7, flashR * 2, flashR * 1.4));
            g.setStroke(new BasicStroke(1));
            g.setComposite(old);
        }
    }
}

class Car {
    double x, y, angle;
    double vx, vy, speed;
    double impulseX, impulseY;
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

    /** External impulse (e.g. a meteor shockwave) applied as a decaying push independent of drift physics. */
    void applyImpulse(double dx, double dy) {
        impulseX += dx;
        impulseY += dy;
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

        double dvx = Math.cos(angle) * speed;
        double dvy = Math.sin(angle) * speed;
        double gripNow = Math.max(2.0, GRIP - Util.clamp(Math.abs(speed) / MAX_SPEED, 0, 1) * 3.0);
        double blend = Util.clamp(gripNow * dt, 0, 1);
        vx += (dvx - vx) * blend;
        vy += (dvy - vy) * blend;

        x += (vx + impulseX) * dt;
        y += (vy + impulseY) * dt;
        double impulseDecay = Util.clamp(1 - 6.0 * dt, 0, 1);
        impulseX *= impulseDecay;
        impulseY *= impulseDecay;

        boolean onTrack = track.distanceFromCenterline(x, y) < track.halfWidth;

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
        Color c = onTrack ? new Color(150, 160, 175, 120) : new Color(120, 128, 140, 160);
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
        speed = 0; vx = 0; vy = 0; impulseX = 0; impulseY = 0;
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

    InputState think(double dt, List<Obstacle> obstacles, List<CraterScar> craterScars, List<Meteor> meteors) {
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

        for (CraterScar cs : craterScars) {
            double dx = cs.x - car.x, dy = cs.y - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < cs.radius + 70) {
                double angToObs = Util.normalizeAngle(Math.atan2(dy, dx) - car.angle);
                if (Math.abs(angToObs) < 1.0) {
                    diff += angToObs < 0 ? 0.65 : -0.65;
                }
            }
        }

        for (Meteor m : meteors) {
            if (m.impacted) continue;
            double dx = m.targetX - car.x, dy = m.targetY - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < m.craterRadius + 90) {
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
    private static final double SHAKE_DURATION = 0.4;
    private static final double MAX_SHAKE = 16;

    private enum State { MENU, COUNTDOWN, RACING, FINISHED }

    private State state = State.MENU;
    private boolean vsAI = true;
    private Thread thread;
    private volatile boolean running = true;

    private final Set<Integer> keys = new HashSet<>();
    private Track track;
    private List<Car> cars = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<GlowScar> glowScars = new ArrayList<>();
    private final List<Star> stars = new ArrayList<>();
    private List<CraterScar> craterScars = new ArrayList<>();
    private List<Meteor> meteors = new ArrayList<>();
    private ParticleSystem particles = new ParticleSystem();
    private AIDriver aiDriver;

    private double countdownTimer;
    private double raceTime;
    private double timeAccum = 0;
    private double meteorTimer;
    private double shakeTimer = 0;
    private final BufferedImage sceneBuffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        setupWorld();
    }

    private void setupWorld() {
        // Wide rectangular superellipse footprint with a high exponent: the
        // ancient, sharply-cornered rim of a giant meteor impact crater.
        track = new Track(W / 2.0, H / 2.0 + 10, 440, 150, 9.0, 85);
        int n = track.centerline.size();

        obstacles.clear();
        Random r = new Random(42);
        for (int i = 0; i < n; i += 11) {
            if (r.nextDouble() < 0.5) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 20 + r.nextDouble() * 55;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.ROCK : Obstacle.Kind.SPIRE;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(10)));
            }
        }
        int[] hazardIdx = {40, 120, 200, 280, 360, 440};
        for (int idx : hazardIdx) {
            if (idx >= n) continue;
            double off = (r.nextDouble() - 0.5) * track.trackWidth * 0.55;
            Point2D.Double p = track.pointAtOffset(idx, off);
            obstacles.add(new Obstacle(Obstacle.Kind.FRAGMENT, p.x, p.y, 15));
        }
        for (int i = 0; i < 3; i++) {
            Obstacle d = new Obstacle(Obstacle.Kind.DEBRIS, r.nextInt(W), r.nextInt(H), 13);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 40 + r.nextDouble() * 40;
            d.vx = Math.cos(a) * sp;
            d.vy = Math.sin(a) * sp;
            obstacles.add(d);
        }

        // Ancient dormant crater scars glowing faintly around the outer rim.
        glowScars.clear();
        Random gr = new Random(99);
        for (int i = 0; i < n; i += 55) {
            Point2D.Double p = track.pointAtOffset(i, -track.innerSign * (track.halfWidth + 70 + gr.nextInt(30)));
            double radius = 55 + gr.nextInt(50);
            glowScars.add(new GlowScar(p.x, p.y, radius, gr.nextDouble() * Math.PI * 2));
        }
        for (int i = 0; i < 4; i++) {
            Point2D.Double p = track.pointAtOffset((n * i) / 4 + 15, track.innerSign * (track.halfWidth + 60 + gr.nextInt(30)));
            glowScars.add(new GlowScar(p.x, p.y, 40 + gr.nextInt(35), gr.nextDouble() * Math.PI * 2));
        }

        // Night-sky starfield.
        stars.clear();
        Random sr = new Random(7);
        for (int i = 0; i < 160; i++) {
            double sx = sr.nextDouble() * W;
            double sy = sr.nextDouble() * H * 0.42;
            stars.add(new Star(sx, sy, 1 + sr.nextDouble() * 2, sr.nextDouble() * Math.PI * 2, 0.5 + sr.nextDouble() * 1.5));
        }
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        craterScars = new ArrayList<>();
        meteors = new ArrayList<>();
        meteorTimer = 4.0 + Math.random() * 2.0;
        shakeTimer = 0;
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-22);
        Point2D.Double p2pos = track.startPosition(22);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(200, 70, 50), new Color(230, 210, 170), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(70, 130, 200), new Color(210, 220, 235), vsAI ? "AI" : "2", particles);
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
        if (shakeTimer > 0) shakeTimer = Math.max(0, shakeTimer - dt);
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
            in2 = aiDriver.think(dt, obstacles, craterScars, meteors);
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

        updateMeteors(dt);
        applyCraterHazards(dt);

        Point2D.Double startPt = track.startPosition(0);
        if (p1.justLapped) particles.spawnFlare(startPt.x, startPt.y);
        if (p2.justLapped) particles.spawnFlare(startPt.x, startPt.y);

        for (Car c : cars) {
            if (!c.finished && c.lap >= TOTAL_LAPS) {
                c.finished = true;
                c.finishTime = raceTime;
            }
        }
        if (p1.finished && p2.finished) state = State.FINISHED;
    }

    /** Spawns, telegraphs and resolves the "Meteor Shower Impacts" signature hazard. */
    private void updateMeteors(double dt) {
        meteorTimer -= dt;
        if (meteorTimer <= 0) {
            spawnMeteor();
            meteorTimer = 5.0 + Math.random() * 4.0;
        }

        Iterator<Meteor> it = meteors.iterator();
        while (it.hasNext()) {
            Meteor m = it.next();
            m.update(dt, particles);
            if (!m.impacted && m.elapsed >= m.telegraphDuration) {
                triggerImpact(m);
            }
            if (m.isFinished()) it.remove();
        }

        craterScars.removeIf(cs -> !cs.update(dt));
    }

    private void spawnMeteor() {
        int idx = (int) (Math.random() * track.centerline.size());
        double lateral = (Math.random() - 0.5) * track.trackWidth * 1.4;
        Point2D.Double p = track.pointAtOffset(idx, lateral);
        meteors.add(new Meteor(p.x, p.y));
    }

    private void triggerImpact(Meteor m) {
        m.impacted = true;
        m.flashTimer = Meteor.FLASH_DURATION;
        shakeTimer = SHAKE_DURATION;
        craterScars.add(new CraterScar(m.targetX, m.targetY, m.craterRadius));

        for (int i = 0; i < 40; i++) {
            double a = Math.random() * Math.PI * 2;
            double sp = 60 + Math.random() * 220;
            Particle.ParticleKind kind = Math.random() < 0.5 ? Particle.ParticleKind.DEBRIS : Particle.ParticleKind.SMOKE;
            Color c = Math.random() < 0.5 ? new Color(255, 130, 40) : new Color(90, 88, 92);
            particles.add(new Particle(m.targetX, m.targetY, Math.cos(a) * sp, Math.sin(a) * sp,
                    0.4 + Math.random() * 0.6, 3 + Math.random() * 6, 8, c, kind));
        }

        for (Car c : cars) {
            if (!c.alive) continue;
            double dx = c.x - m.targetX, dy = c.y - m.targetY;
            double dist = Math.hypot(dx, dy);
            if (dist >= m.shockRadius) continue;
            double strength = Util.clamp(1 - dist / m.shockRadius, 0, 1);
            double nx, ny;
            if (dist > 0.001) { nx = dx / dist; ny = dy / dist; }
            else { double a = Math.random() * Math.PI * 2; nx = Math.cos(a); ny = Math.sin(a); }
            double pushMag = 260 * strength;
            c.applyImpulse(nx * pushMag, ny * pushMag);
            if (dist < m.damageRadius) {
                c.damage(28 * strength + 8);
                c.spinOut(0.7 * strength + 0.3);
            }
        }
    }

    /** Continuous slow + light damage while a car sits inside a lingering crater scar. */
    private void applyCraterHazards(double dt) {
        for (Car c : cars) {
            if (!c.alive) continue;
            for (CraterScar cs : craterScars) {
                double dist = Math.hypot(c.x - cs.x, c.y - cs.y);
                if (dist >= cs.radius) continue;
                double sev = 1 - dist / cs.radius;
                c.speed *= (1 - Util.clamp(2.5 * sev * dt, 0, 0.9));
                c.damage(sev * 40 * dt);
                if (Math.random() < sev * dt * 3) {
                    particles.add(new Particle(c.x, c.y - 4, (Math.random() - 0.5) * 20, -20 - Math.random() * 20,
                            0.4, 4, 3, new Color(255, 150, 60, 180), Particle.ParticleKind.EMBER));
                }
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
        for (GlowScar gs : glowScars) gs.draw(sg, timeAccum);
        track.draw(sg);
        for (Obstacle o : obstacles) o.draw(sg);
        for (CraterScar cs : craterScars) cs.draw(sg, timeAccum);
        for (Meteor m : meteors) m.draw(sg);
        particles.draw(sg);
        for (Car c : cars) c.draw(sg);
        sg.dispose();

        Graphics2D g = (Graphics2D) g0;
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, H);
        int ox = 0, oy = 0;
        if (shakeTimer > 0) {
            double mag = MAX_SHAKE * (shakeTimer / SHAKE_DURATION);
            ox = (int) ((Math.random() - 0.5) * 2 * mag);
            oy = (int) ((Math.random() - 0.5) * 2 * mag);
        }
        g.drawImage(sceneBuffer, ox, oy, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (state) {
            case MENU: drawMenu(g); break;
            case COUNTDOWN: drawCountdown(g); break;
            case RACING: drawHud(g); break;
            case FINISHED: drawFinish(g); break;
        }
    }

    private void drawBackground(Graphics2D g) {
        double horizon = H * 0.42;
        g.setPaint(new GradientPaint(0, 0, new Color(6, 7, 18), 0, (float) horizon, new Color(26, 22, 46)));
        g.fillRect(0, 0, W, (int) horizon);

        for (Star s : stars) s.draw(g, timeAccum);

        double pulse = 1 + 0.03 * Math.sin(timeAccum * 1.2);
        int moonR = (int) (46 * pulse);
        g.setPaint(new RadialGradientPaint(new Point(W - 190, 100), moonR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(210, 225, 255, 200), new Color(150, 180, 230, 80), new Color(150, 180, 230, 0)}));
        g.fillOval(W - 190 - moonR * 3, 100 - moonR * 3, moonR * 6, moonR * 6);
        g.setColor(new Color(225, 235, 250));
        g.fillOval(W - 190 - moonR / 2, 100 - moonR / 2, moonR, moonR);

        // Distant jagged crater-rim silhouette on the horizon.
        g.setColor(new Color(34, 38, 54));
        Path2D.Double wall = new Path2D.Double();
        wall.moveTo(-50, horizon);
        double wx = -50;
        Random wr = new Random(7);
        boolean up = true;
        while (wx < W + 50) {
            double wy = up ? horizon - (55 + wr.nextInt(35)) : horizon - (18 + wr.nextInt(18));
            wall.lineTo(wx, wy);
            wx += 18 + wr.nextInt(14);
            up = !up;
        }
        wall.lineTo(W + 50, horizon);
        wall.closePath();
        g.fill(wall);

        g.setPaint(new GradientPaint(0, (float) (H * 0.4), new Color(58, 64, 76), 0, H, new Color(30, 33, 42)));
        g.fillRect(0, (int) (H * 0.4), W, (int) (H * 0.6));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(255, 150, 70));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        centerText(g, "METEOR CRATER CIRCUIT", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Night racing in an ancient impact crater — watch the sky", H / 2 - 90);

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
        g.setColor(new Color(255, 210, 90));
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
