import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ExtremeDesertRacing3 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Mirage Salt Sprint — Extreme Desert Racing III");
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

    enum ParticleKind { DUST, SMOKE, SPARK, DEBRIS, FIREWORK, STEAM }

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
        } else if (kind == ParticleKind.STEAM) {
            vy -= 6 * dt; // rises like heat haze
            vx *= 0.985;
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

/** MUST stay synchronized: physics thread and Swing EDT both touch this. */
class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();

    synchronized void add(Particle p) { particles.add(p); }

    synchronized void update(double dt) {
        particles.removeIf(p -> !p.update(dt));
    }

    void spawnFirework(double x, double y) {
        Color[] palette = {
                new Color(255, 250, 230), new Color(190, 225, 255),
                new Color(255, 255, 255), new Color(160, 200, 230)
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
 * "Stadium" drag-strip loop: two long straights joined by two semicircular
 * arcs. Built directly as a convex, simple, non-self-intersecting closed
 * curve (a discorectangle boundary), sampled proportionally to segment arc
 * length so cumulativeLength stays monotonic and lap counting/rendering can
 * reuse the same centerline + normals technique as the other games in the
 * series: nearest centerline point -> compare cumulative arc length to the
 * previous frame (with wraparound) for signed continuous distance, and
 * offset-by-normal for the outer/inner edges.
 */
class Track {
    final double centerX, centerY, halfLength, arcRadius, trackWidth, halfWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;
    double innerSign = 1.0;

    Track(double centerX, double centerY, double halfLength, double arcRadius, double trackWidth) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.halfLength = halfLength;
        this.arcRadius = arcRadius;
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        build();
    }

    private void build() {
        double straightLen = 2 * halfLength;
        double arcLen = Math.PI * arcRadius;
        double perimeter = 2 * straightLen + 2 * arcLen;
        int totalSamples = 800;
        double density = totalSamples / perimeter;
        int nStraight = Math.max(20, (int) Math.round(density * straightLen));
        int nArc = Math.max(20, (int) Math.round(density * arcLen));

        double leftX = centerX - halfLength, rightX = centerX + halfLength;
        double topY = centerY - arcRadius, botY = centerY + arcRadius;

        // 1) top straight: (leftX,topY) -> (rightX,topY)
        for (int i = 0; i < nStraight; i++) {
            double t = i / (double) nStraight;
            centerline.add(new Point2D.Double(leftX + (rightX - leftX) * t, topY));
        }
        // 2) right semicircle around (rightX, centerY): angle -90deg -> +90deg
        for (int i = 0; i < nArc; i++) {
            double t = i / (double) nArc;
            double ang = -Math.PI / 2 + Math.PI * t;
            centerline.add(new Point2D.Double(rightX + Math.cos(ang) * arcRadius, centerY + Math.sin(ang) * arcRadius));
        }
        // 3) bottom straight: (rightX,botY) -> (leftX,botY)
        for (int i = 0; i < nStraight; i++) {
            double t = i / (double) nStraight;
            centerline.add(new Point2D.Double(rightX + (leftX - rightX) * t, botY));
        }
        // 4) left semicircle around (leftX, centerY): angle +90deg -> +270deg
        for (int i = 0; i < nArc; i++) {
            double t = i / (double) nArc;
            double ang = Math.PI / 2 + Math.PI * t;
            centerline.add(new Point2D.Double(leftX + Math.cos(ang) * arcRadius, centerY + Math.sin(ang) * arcRadius));
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

        // The stadium loop is convex, so the center point is always strictly
        // inside it; use the same "poke toward center" test as the other
        // games to discover which normal direction points inward.
        int idx = 0;
        Point2D.Double p = centerline.get(idx);
        Point2D.Double nrm = normals.get(idx);
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
        g.setColor(new Color(224, 226, 222));
        g.fill(road);

        // Sun-baked salt crust cracks across the road surface.
        g.setColor(new Color(190, 194, 190, 110));
        for (int i = 0; i < centerline.size(); i += 6) {
            Point2D.Double o = pointAtOffset(i, halfWidth);
            Point2D.Double in = pointAtOffset(i, -halfWidth);
            g.draw(new Line2D.Double(o, in));
        }

        g.setColor(new Color(150, 158, 160));
        g.setStroke(new BasicStroke(4));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(150, 190, 210, 150));
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
    enum Kind { ROCK, SALT_CAIRN, TUMBLEWEED }

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
                g.setColor(new Color(150, 142, 130));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(110, 102, 92));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.fillOval((int) (-radius * 0.4), (int) (-radius * 0.3), (int) (radius), (int) (radius * 0.8));
                break;
            case SALT_CAIRN:
                g.setColor(new Color(232, 232, 226));
                g.fillRoundRect((int) (-radius * 0.7), (int) -radius, (int) (radius * 1.4), (int) (radius * 2), 6, 6);
                g.setColor(new Color(200, 200, 192));
                for (int i = -1; i <= 1; i++) {
                    g.drawLine((int) (-radius * 0.55), (int) (i * radius * 0.6), (int) (radius * 0.55), (int) (i * radius * 0.6));
                }
                break;
            case TUMBLEWEED:
                g.setColor(new Color(178, 150, 96));
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

/**
 * Signature mechanic: a heat-mirage patch on or near the track.
 * HAZARD zones look like a harmless glare puddle but hide a real obstacle
 * underneath — real hazards ALWAYS render a small dark rippling core dot at
 * their center, a consistent, learnable "tell". SAFE zones are pure
 * cosmetic shimmer (no core dot) and can be driven through freely, including
 * ones that visually crowd the track edge to bait an unnecessary swerve.
 */
class MirageZone {
    enum Kind { HAZARD, SAFE }

    final double x, y, radius, hazardRadius, phase, rippleFreq;
    final Kind kind;

    MirageZone(Kind kind, double x, double y, double radius, double phase) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.hazardRadius = radius * 0.55;
        this.phase = phase;
        this.rippleFreq = kind == Kind.HAZARD ? 5.5 : 3.0;
    }

    boolean isHazard() { return kind == Kind.HAZARD; }

    void draw(Graphics2D g, double t) {
        int rays = 18;
        Path2D.Double blob = new Path2D.Double();
        for (int i = 0; i <= rays; i++) {
            double a = (Math.PI * 2 * i) / rays;
            double wob = Math.sin(a * rippleFreq + t * 3.2 + phase) * (radius * 0.14);
            double rr = radius + wob;
            double px = x + Math.cos(a) * rr;
            double py = y + Math.sin(a) * rr * 0.55; // squashed, puddle-like
            if (i == 0) blob.moveTo(px, py); else blob.lineTo(px, py);
        }
        blob.closePath();

        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.42f));
        g.setColor(new Color(210, 236, 250));
        g.fill(blob);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
        g.setColor(Color.WHITE);
        g.fill(blob);
        g.setComposite(old);

        if (kind == Kind.HAZARD) {
            // The consistent "tell": a faint dark rippling core beneath the shimmer.
            double pulse = 0.5 + 0.5 * Math.sin(t * 5 + phase);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.22 + 0.16 * pulse)));
            g.setColor(new Color(60, 70, 78));
            double cr = hazardRadius * 0.5;
            g.fill(new Ellipse2D.Double(x - cr, y - cr * 0.55, cr * 2, cr * 1.1));
            g.setComposite(old);
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

    private static final double MAX_SPEED = 480;
    private static final double MAX_REVERSE = -170;
    private static final double ACCEL = 320;
    private static final double BRAKE = 580;
    private static final double NATURAL_FRICTION = 140;
    private static final double OFFROAD_MULT = 2.5;
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
        emitDust(dt, track.distanceFromCenterline(x, y) < track.halfWidth);
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
        Color c = onTrack ? new Color(255, 255, 255, 90) : new Color(225, 220, 200, 150);
        for (int i = 0; i < (onTrack ? 1 : 2); i++) {
            double jx = (Math.random() - 0.5) * 10;
            double jy = (Math.random() - 0.5) * 10;
            particles.add(new Particle(rearX + jx, rearY + jy,
                    -vx * 0.15 + (Math.random() - 0.5) * 20, -vy * 0.15 + (Math.random() - 0.5) * 20,
                    0.5 + Math.random() * 0.4, 6 + Math.random() * 6, 8, c, Particle.ParticleKind.DUST));
        }
        if (boosting) {
            particles.add(new Particle(rearX, rearY, -vx * 0.3, -vy * 0.3, 0.35, 10, 6,
                    new Color(140, 200, 255, 200), Particle.ParticleKind.SPARK));
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
                    Math.random() < 0.5 ? new Color(255, 210, 140) : new Color(200, 200, 200), Particle.ParticleKind.SMOKE));
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
            g.setColor(new Color(140, 200, 255, 200));
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
        g.setColor(new Color(255, 240, 200));
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

    InputState think(double dt, List<Obstacle> obstacles, List<MirageZone> mirages) {
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

        // AI "knows" the ground truth beneath the shimmer and steers around
        // real hazards only (it does not need to read the visual tell).
        for (MirageZone m : mirages) {
            if (!m.isHazard()) continue;
            double dx = m.x - car.x, dy = m.y - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < 100) {
                double angToObs = Util.normalizeAngle(Math.atan2(dy, dx) - car.angle);
                if (Math.abs(angToObs) < 0.9) {
                    diff += angToObs < 0 ? 0.65 : -0.65;
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
    private final List<MirageZone> mirages = new ArrayList<>();
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
        track = new Track(W / 2.0, 390, 470, 90, 78);
        int n = track.centerline.size();

        obstacles.clear();
        Random r = new Random(42);
        for (int i = 0; i < n; i += 13) {
            if (r.nextDouble() < 0.45) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 22 + r.nextDouble() * 60;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.ROCK : Obstacle.Kind.SALT_CAIRN;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(10)));
            }
        }
        for (int i = 0; i < 4; i++) {
            Obstacle d = new Obstacle(Obstacle.Kind.TUMBLEWEED, r.nextInt(W), r.nextInt(H), 12);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 40 + r.nextDouble() * 40;
            d.vx = Math.cos(a) * sp;
            d.vy = Math.sin(a) * sp;
            obstacles.add(d);
        }

        // Heat-mirage zones: mixed HAZARD/SAFE, scattered across the long
        // straights (where speed is highest and reflex matters most) plus a
        // few straddling the track edge to bait unnecessary swerves. Kind is
        // chosen so no simple position-only rule predicts it — only the
        // rendered "tell" (dark core dot) is a reliable signal.
        mirages.clear();
        Random mr = new Random(7);
        int[] mirageIdx = {30, 90, 150, 230, 300, 430, 500, 560, 630, 700, 760};
        for (int idx : mirageIdx) {
            int ci = idx % n;
            boolean onTrack = mr.nextDouble() < 0.6;
            double off = onTrack ? (mr.nextDouble() - 0.5) * track.trackWidth * 0.7
                    : track.innerSign * -1 * (track.halfWidth - 6 + mr.nextDouble() * 10) * (mr.nextBoolean() ? 1 : -1);
            Point2D.Double p = track.pointAtOffset(ci, off);
            MirageZone.Kind kind = mr.nextDouble() < 0.5 ? MirageZone.Kind.HAZARD : MirageZone.Kind.SAFE;
            double radius = 26 + mr.nextDouble() * 14;
            mirages.add(new MirageZone(kind, p.x, p.y, radius, mr.nextDouble() * Math.PI * 2));
        }
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-20);
        Point2D.Double p2pos = track.startPosition(20);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(200, 40, 40), new Color(250, 250, 245), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(30, 110, 190), new Color(230, 240, 250), vsAI ? "AI" : "2", particles);
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
            in2 = aiDriver.think(dt, obstacles, mirages);
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
        for (MirageZone m : mirages) {
            resolveMirageCollision(p1, m);
            resolveMirageCollision(p2, m);
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
                        new Color(255, 220, 140), Particle.ParticleKind.SPARK));
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

    /** Signature-mechanic collision: only HAZARD mirage zones (the hidden rock beneath the glare) hurt the car. */
    private void resolveMirageCollision(Car c, MirageZone m) {
        if (!c.alive || !m.isHazard()) return;
        double dx = c.x - m.x, dy = c.y - m.y;
        double dist = Math.hypot(dx, dy);
        double minDist = c.radius() + m.hazardRadius;
        if (dist < minDist && dist > 0.0001) {
            double nx = dx / dist, ny = dy / dist;
            double overlap = minDist - dist;
            c.x += nx * overlap;
            c.y += ny * overlap;
            double impact = Math.abs(c.speed);
            c.damage(impact * 0.09 + 4);
            c.speed *= -0.3;
            c.spinOut(0.5);
            for (int i = 0; i < 14; i++) {
                double ang = Math.random() * Math.PI * 2;
                double sp = 40 + Math.random() * 90;
                particles.add(new Particle(c.x, c.y, Math.cos(ang) * sp, Math.sin(ang) * sp,
                        0.5 + Math.random() * 0.4, 5, 5, new Color(230, 240, 245, 200), Particle.ParticleKind.STEAM));
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
        for (MirageZone m : mirages) m.draw(sg, timeAccum);
        drawLowShimmer(sg);
        particles.draw(sg);
        for (Car c : cars) c.draw(sg);
        sg.dispose();

        applyHorizonMirage(sceneBuffer);

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

    /** Pixel row-shift heat shimmer band right at the horizon line. */
    private void applyHorizonMirage(BufferedImage img) {
        int top = (int) (H * 0.30), bandH = 46;
        for (int y = top; y < top + bandH && y < H; y++) {
            double t = (y - top) / (double) bandH;
            int shift = (int) (Math.sin(y * 0.22 + timeAccum * 2.6) * 7 * (1 - t));
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

    /** Wavy alpha-blended shimmer stripes drawn low over the track surface — purely atmospheric. */
    private void drawLowShimmer(Graphics2D g) {
        Composite old = g.getComposite();
        int bandTop = (int) (H * 0.34), bandH = (int) (H * 0.42);
        for (int i = 0; i < 4; i++) {
            double y = bandTop + bandH * (i / 4.0);
            float alpha = (float) (0.05 + 0.03 * Math.sin(timeAccum * 2 + i));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.01f, alpha)));
            g.setColor(Color.WHITE);
            Path2D.Double wave = new Path2D.Double();
            wave.moveTo(0, y);
            for (int x = 0; x <= W; x += 20) {
                double wy = y + Math.sin(x * 0.02 + timeAccum * 2.2 + i * 1.3) * 6;
                wave.lineTo(x, wy);
            }
            wave.lineTo(W, y + 18);
            wave.lineTo(0, y + 18);
            wave.closePath();
            g.fill(wave);
        }
        g.setComposite(old);
    }

    private void drawBackground(Graphics2D g) {
        // Blinding pale salt-flat sky: near-white at the flat glaring horizon.
        g.setPaint(new GradientPaint(0, 0, new Color(198, 222, 236), 0, (float) (H * 0.32), new Color(250, 250, 248)));
        g.fillRect(0, 0, W, (int) (H * 0.32));

        double pulse = 1 + 0.03 * Math.sin(timeAccum * 1.5);
        int sunR = (int) (60 * pulse);
        g.setPaint(new RadialGradientPaint(new Point(W / 2, (int) (H * 0.30)), sunR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(255, 255, 250, 230), new Color(255, 255, 245, 110), new Color(255, 255, 245, 0)}));
        g.fillOval(W / 2 - sunR * 3, (int) (H * 0.30) - sunR * 3, sunR * 6, sunR * 6);
        g.setColor(new Color(255, 255, 252));
        g.fillOval(W / 2 - sunR / 2, (int) (H * 0.30) - sunR / 2, sunR, sunR);

        // Faint distant mesas breaking the flat horizon line.
        g.setColor(new Color(196, 206, 210, 160));
        Path2D.Double mesas = new Path2D.Double();
        mesas.moveTo(-50, H * 0.32);
        double wx = -50;
        Random wr = new Random(7);
        while (wx < W + 50) {
            double top = H * 0.32 - (10 + wr.nextInt(22));
            double segW = 40 + wr.nextInt(60);
            mesas.lineTo(wx, top);
            mesas.lineTo(wx + segW, top);
            wx += segW + 20 + wr.nextInt(30);
            mesas.lineTo(wx, H * 0.32);
        }
        mesas.lineTo(W + 50, H * 0.32);
        mesas.closePath();
        g.fill(mesas);

        // Glaring pale-gray-blue salt flat ground.
        g.setPaint(new GradientPaint(0, (float) (H * 0.30), new Color(238, 240, 238), 0, H, new Color(198, 202, 202)));
        g.fillRect(0, (int) (H * 0.30), W, (int) (H * 0.70));

        // Sparse cracked-salt polygon texture across the open ground.
        g.setColor(new Color(180, 186, 186, 100));
        Random cr = new Random(123);
        for (int i = 0; i < 70; i++) {
            double cx = cr.nextDouble() * W;
            double cy = H * 0.32 + cr.nextDouble() * H * 0.66;
            double s = 8 + cr.nextDouble() * 20;
            g.drawLine((int) (cx - s), (int) cy, (int) (cx + s), (int) cy);
            g.drawLine((int) cx, (int) (cy - s * 0.5), (int) cx, (int) (cy + s * 0.5));
        }
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(20, 30, 40, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(70, 150, 200));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        centerText(g, "MIRAGE SALT SPRINT", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Blinding salt flats — learn to read the shimmer before it reads you", H / 2 - 90);

        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        centerText(g, "[1] Player vs Player      [2] Player vs Computer", H / 2 - 20);
        centerText(g, "P1: W A S D  +  SPACE boost", H / 2 + 20);
        centerText(g, "P2: Arrow Keys + ENTER boost  (disabled vs Computer)", H / 2 + 46);
        centerText(g, "Tip: a faint dark ripple at a puddle's center means it's hiding a real rock", H / 2 + 78);
        centerText(g, "Press 1 or 2 to start", H / 2 + 116);
    }

    private void centerText(Graphics2D g, String s, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (W - fm.stringWidth(s)) / 2, y);
    }

    private void drawCountdown(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 90));
        g.setColor(new Color(40, 120, 170));
        String txt = countdownTimer > 3 ? "READY" : String.valueOf((int) Math.ceil(countdownTimer));
        if (countdownTimer <= 0) txt = "GO!";
        centerText(g, txt, H / 2);
        drawHud(g);
    }

    private void drawHud(Graphics2D g) {
        Car p1 = cars.get(0), p2 = cars.get(1);
        drawDriverHud(g, p1, 20, "P1");
        drawDriverHud(g, p2, W - 210, vsAI ? "CPU" : "P2");

        g.setColor(new Color(20, 40, 60));
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
        g.setColor(new Color(20, 30, 40, 150));
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
        g.setColor(new Color(80, 170, 255));
        g.fillRect(x + 10, 54, (int) (170 * Util.clamp(c.boostFuel / 100.0, 0, 1)), 8);
    }

    private void drawFinish(Graphics2D g) {
        g.setColor(new Color(10, 20, 30, 170));
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
