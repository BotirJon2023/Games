import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class MadMaxStylePostApocalypticRacing13 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Dust Bowl Rally — Post-Apocalyptic Desert Racing");
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
 * A rounded-triangle "tri-oval" loop: three straights of clearly different
 * lengths joined by three rounded corners. The centerline is stored as a
 * simple closed polyline together with a parallel cumulative arc-length
 * array (used for robust lap counting) and per-point normals (used for
 * offset-based track-edge rendering/collision) — same technique as the
 * proven reference architecture, generalized from an oval to a polygon.
 */
class Track {
    final double trackWidth, halfWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;

    Track(double trackWidth) {
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        build();
    }

    private static double angleTo(Point2D.Double c, Point2D.Double p) {
        return Math.atan2(p.y - c.y, p.x - c.x);
    }

    private void build() {
        // Elongated, uneven triangle — flatter and more stretched than a
        // typical rounded triangle, with three noticeably different straights.
        Point2D.Double[] verts = {
                new Point2D.Double(330, 330),
                new Point2D.Double(980, 225),
                new Point2D.Double(640, 545)
        };
        double[] cornerR = {80, 85, 72};

        int n = verts.length;
        Point2D.Double[] tanPrev = new Point2D.Double[n];
        Point2D.Double[] tanNext = new Point2D.Double[n];
        Point2D.Double[] centers = new Point2D.Double[n];
        double[] arcR = new double[n];
        double[] a0 = new double[n];
        double[] a1 = new double[n];

        for (int i = 0; i < n; i++) {
            Point2D.Double prev = verts[(i - 1 + n) % n];
            Point2D.Double cur = verts[i];
            Point2D.Double next = verts[(i + 1) % n];
            double v1x = prev.x - cur.x, v1y = prev.y - cur.y;
            double len1 = Math.hypot(v1x, v1y); v1x /= len1; v1y /= len1;
            double v2x = next.x - cur.x, v2y = next.y - cur.y;
            double len2 = Math.hypot(v2x, v2y); v2x /= len2; v2y /= len2;
            double dot = Util.clamp(v1x * v2x + v1y * v2y, -1, 1);
            double theta = Math.acos(dot);
            double d = cornerR[i] / Math.tan(theta / 2);
            d = Math.min(d, Math.min(len1, len2) * 0.42);
            double r = d * Math.tan(theta / 2);
            double bx = v1x + v2x, by = v1y + v2y;
            double blen = Math.hypot(bx, by);
            if (blen < 1e-6) { bx = -v1y; by = v1x; blen = 1; }
            bx /= blen; by /= blen;
            double centerDist = r / Math.sin(theta / 2);
            centers[i] = new Point2D.Double(cur.x + bx * centerDist, cur.y + by * centerDist);
            tanPrev[i] = new Point2D.Double(cur.x + v1x * d, cur.y + v1y * d);
            tanNext[i] = new Point2D.Double(cur.x + v2x * d, cur.y + v2y * d);
            arcR[i] = centers[i].distance(tanPrev[i]);
            double rawA0 = angleTo(centers[i], tanPrev[i]);
            double rawA1 = angleTo(centers[i], tanNext[i]);
            a0[i] = rawA0;
            a1[i] = rawA0 + Util.normalizeAngle(rawA1 - rawA0);
        }

        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            Point2D.Double from = tanNext[i];
            Point2D.Double to = tanPrev[j];
            double segLen = from.distance(to);
            int nSeg = Math.max(16, (int) (segLen / 9));
            for (int k = 0; k < nSeg; k++) {
                double t = k / (double) nSeg;
                centerline.add(new Point2D.Double(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t));
            }
            int nArc = 46;
            for (int k = 0; k < nArc; k++) {
                double t = k / (double) nArc;
                double angle = a0[j] + (a1[j] - a0[j]) * t;
                centerline.add(new Point2D.Double(centers[j].x + Math.cos(angle) * arcR[j],
                        centers[j].y + Math.sin(angle) * arcR[j]));
            }
        }

        int cnt = centerline.size();
        cumulativeLength = new double[cnt];
        cumulativeLength[0] = 0;
        for (int i = 1; i < cnt; i++) {
            cumulativeLength[i] = cumulativeLength[i - 1] + centerline.get(i - 1).distance(centerline.get(i));
        }
        totalLength = cumulativeLength[cnt - 1] + centerline.get(cnt - 1).distance(centerline.get(0));

        for (int i = 0; i < cnt; i++) {
            Point2D.Double prev = centerline.get((i - 1 + cnt) % cnt);
            Point2D.Double next = centerline.get((i + 1) % cnt);
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
        g.setColor(new Color(138, 108, 72));
        g.fill(road);

        g.setColor(new Color(168, 140, 96));
        g.setStroke(new BasicStroke(3));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(224, 198, 150, 140));
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
                g.setColor(new Color(110, 94, 74));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(80, 66, 50));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                break;
            case CACTUS:
                g.setColor(new Color(90, 120, 70));
                g.fillRoundRect((int) (-radius / 3), (int) -radius, (int) (radius * 2 / 3), (int) (radius * 2), 8, 8);
                g.fillRoundRect((int) -radius, (int) (-radius / 3), (int) radius, (int) (radius * 2 / 3), 8, 8);
                g.fillRoundRect((int) (radius / 3), (int) (-radius * 0.7), (int) (radius * 2 / 3), (int) radius, 8, 8);
                break;
            case WRECK:
                g.setColor(new Color(86, 46, 34));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                break;
            case TUMBLEWEED:
                g.setColor(new Color(160, 128, 66));
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

/**
 * A roaming dust-devil hazard: a tapering, twisting spiral that wanders the
 * play field via a slow random walk. Any car caught inside is forced into a
 * strong spin and flung a short random distance, then released once the
 * spin (Car.spinTimer) counts down — reusing the existing spinOut mechanic.
 */
class DustDevil {
    double x, y;
    double dirAngle;
    double speed;
    double phase;
    double wanderTimer;
    double emitTimer;
    final double hazardRadius;
    final double visualHeight;

    DustDevil(double x, double y, double hazardRadius) {
        this.x = x;
        this.y = y;
        this.hazardRadius = hazardRadius;
        this.visualHeight = hazardRadius * 3.4;
        this.dirAngle = Math.random() * Math.PI * 2;
        this.speed = 26 + Math.random() * 30;
        this.wanderTimer = 1.2 + Math.random() * 2.4;
    }

    void update(double dt, double minX, double maxX, double minY, double maxY) {
        wanderTimer -= dt;
        if (wanderTimer <= 0) {
            dirAngle += (Math.random() - 0.5) * Math.PI * 1.4;
            speed = 22 + Math.random() * 36;
            wanderTimer = 1.2 + Math.random() * 2.6;
        }
        x += Math.cos(dirAngle) * speed * dt;
        y += Math.sin(dirAngle) * speed * dt;
        phase += dt * 5.2;
        if (x < minX) { x = minX; dirAngle = Math.PI - dirAngle; }
        if (x > maxX) { x = maxX; dirAngle = Math.PI - dirAngle; }
        if (y < minY) { y = minY; dirAngle = -dirAngle; }
        if (y > maxY) { y = maxY; dirAngle = -dirAngle; }
    }

    void emitParticles(ParticleSystem ps, double dt) {
        emitTimer -= dt;
        if (emitTimer > 0) return;
        emitTimer = 0.035;
        double ang = Math.random() * Math.PI * 2;
        double rad = hazardRadius * (0.25 + Math.random() * 0.75);
        double px = x + Math.cos(ang) * rad;
        double py = y + Math.sin(ang) * rad * 0.4;
        double tangential = 70;
        double vx = -Math.sin(ang) * tangential;
        double vy = -Math.cos(ang) * tangential * 0.4 - 30;
        ps.add(new Particle(px, py, vx, vy, 0.45 + Math.random() * 0.3, 5 + Math.random() * 5, 5,
                new Color(196, 166, 112, 150), Particle.ParticleKind.DUST));
    }

    void draw(Graphics2D g) {
        g.setColor(new Color(90, 70, 45, 70));
        g.fillOval((int) (x - hazardRadius * 0.9), (int) (y - hazardRadius * 0.32), (int) (hazardRadius * 1.8), (int) (hazardRadius * 0.65));

        int strands = 3;
        for (int s = 0; s < strands; s++) {
            Path2D.Double path = new Path2D.Double();
            double turns = 3.2;
            int steps = 50;
            double strandPhase = phase + s * (Math.PI * 2 / strands);
            double dir = (s % 2 == 0) ? 1 : -1;
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double ang = strandPhase + t * turns * Math.PI * 2 * dir;
                double rad = hazardRadius * (1 - t) * 0.9 + 3;
                double px = x + Math.cos(ang) * rad;
                double py = y - t * visualHeight + Math.sin(ang) * rad * 0.35;
                if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
            }
            g.setColor(new Color(182, 152, 104, 135));
            g.setStroke(new BasicStroke(2.1f));
            g.draw(path);
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
    double dustDevilCooldown = 0;

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
        if (dustDevilCooldown > 0) dustDevilCooldown -= dt;
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
        Color c = onTrack ? new Color(210, 190, 150, 120) : new Color(190, 150, 95, 160);
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
    private final List<DustDevil> dustDevils = new ArrayList<>();
    private ParticleSystem particles = new ParticleSystem();
    private AIDriver aiDriver;

    private double countdownTimer;
    private double raceTime;
    private double sunPulse = 0;
    private final List<Point2D.Double> mesas = new ArrayList<>();
    private final List<Path2D.Double> crackLines = new ArrayList<>();
    private final BufferedImage sceneBuffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        setupWorld();
        buildMesas();
        buildCracks();
    }

    private void buildMesas() {
        Random r = new Random(7);
        double x = -50;
        while (x < W + 50) {
            mesas.add(new Point2D.Double(x, 80 + r.nextInt(70)));
            x += 40 + r.nextInt(40);
        }
    }

    private void buildCracks() {
        Random r = new Random(99);
        for (int i = 0; i < 46; i++) {
            double sx = r.nextDouble() * W;
            double sy = H * 0.4 + r.nextDouble() * H * 0.6;
            Path2D.Double p = new Path2D.Double();
            p.moveTo(sx, sy);
            double ang = r.nextDouble() * Math.PI * 2;
            double cx = sx, cy = sy;
            int segs = 2 + r.nextInt(3);
            for (int s = 0; s < segs; s++) {
                ang += (r.nextDouble() - 0.5) * 1.6;
                double len = 14 + r.nextDouble() * 26;
                cx += Math.cos(ang) * len;
                cy += Math.sin(ang) * len;
                p.lineTo(cx, cy);
            }
            crackLines.add(p);
        }
    }

    private void setupWorld() {
        track = new Track(140);
        obstacles.clear();
        dustDevils.clear();
        Random r = new Random(42);
        int n = track.centerline.size();
        for (int i = 0; i < n; i += 9) {
            if (r.nextDouble() < 0.55) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 20 + r.nextDouble() * 60;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.ROCK : Obstacle.Kind.CACTUS;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(10)));
            }
        }
        int[] hazardIdx = {n / 8, n * 3 / 8, n * 5 / 8, n * 7 / 8};
        for (int idx : hazardIdx) {
            if (idx >= n) continue;
            double off = (r.nextDouble() - 0.5) * track.trackWidth * 0.6;
            Point2D.Double p = track.pointAtOffset(idx, off);
            obstacles.add(new Obstacle(Obstacle.Kind.WRECK, p.x, p.y, 16));
        }
        // Noticeably more tumbleweeds than the original desert racer.
        for (int i = 0; i < 13; i++) {
            Obstacle tw = new Obstacle(Obstacle.Kind.TUMBLEWEED, r.nextInt(W), r.nextInt(H), 12 + r.nextInt(6));
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 35 + r.nextDouble() * 45;
            tw.vx = Math.cos(a) * sp;
            tw.vy = Math.sin(a) * sp;
            obstacles.add(tw);
        }
        // Roaming dust-devil hazards.
        for (int i = 0; i < 3; i++) {
            double dx = 200 + r.nextDouble() * (W - 400);
            double dy = 160 + r.nextDouble() * (H - 320);
            dustDevils.add(new DustDevil(dx, dy, 34 + r.nextInt(10)));
        }
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-25);
        Point2D.Double p2pos = track.startPosition(25);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(196, 74, 44), new Color(224, 196, 138), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(90, 108, 118), new Color(210, 196, 168), vsAI ? "AI" : "2", particles);
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
        sunPulse += dt;
        for (Obstacle o : obstacles) o.update(dt, -30, W + 30, -30, H + 30);
        for (DustDevil dd : dustDevils) {
            dd.update(dt, 40, W - 40, 40, H - 40);
            dd.emitParticles(particles, dt);
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
        for (DustDevil dd : dustDevils) {
            resolveDustDevil(p1, dd);
            resolveDustDevil(p2, dd);
        }

        for (Car c : cars) {
            if (!c.finished && c.lap >= TOTAL_LAPS) {
                c.finished = true;
                c.finishTime = raceTime;
            }
        }
        if (p1.finished && p2.finished) state = State.FINISHED;
    }

    private void resolveDustDevil(Car c, DustDevil dd) {
        if (!c.alive || c.dustDevilCooldown > 0) return;
        double dist = Math.hypot(c.x - dd.x, c.y - dd.y);
        if (dist < dd.hazardRadius) {
            c.spinOut(2.0);
            double flingAngle = Math.random() * Math.PI * 2;
            double flingDist = 45 + Math.random() * 45;
            c.x += Math.cos(flingAngle) * flingDist;
            c.y += Math.sin(flingAngle) * flingDist;
            c.speed *= 0.25;
            c.dustDevilCooldown = 2.2;
            for (int i = 0; i < 18; i++) {
                double a = Math.random() * Math.PI * 2;
                double sp = 50 + Math.random() * 140;
                particles.add(new Particle(c.x, c.y, Math.cos(a) * sp, Math.sin(a) * sp,
                        0.35 + Math.random() * 0.4, 4 + Math.random() * 5, 6,
                        new Color(200, 172, 120, 190), Particle.ParticleKind.DUST));
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
        for (Obstacle o : obstacles) o.draw(sg);
        for (DustDevil dd : dustDevils) dd.draw(sg);
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
            int shift = (int) (Math.sin(y * 0.25 + sunPulse * 4) * 4 * (1 - t));
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
        g.setPaint(new GradientPaint(0, 0, new Color(232, 220, 195), 0, (float) (H * 0.42), new Color(214, 190, 150)));
        g.fillRect(0, 0, W, (int) (H * 0.42));

        double pulse = 1 + 0.025 * Math.sin(sunPulse * 1.2);
        int sunR = (int) (58 * pulse);
        Point sunPos = new Point(W - 200, 100);
        g.setPaint(new RadialGradientPaint(sunPos, sunR * 4,
                new float[]{0f, 0.35f, 1f},
                new Color[]{new Color(255, 255, 245, 235), new Color(255, 250, 220, 110), new Color(255, 250, 220, 0)}));
        g.fillOval(sunPos.x - sunR * 4, sunPos.y - sunR * 4, sunR * 8, sunR * 8);
        g.setColor(new Color(255, 255, 250));
        g.fillOval(sunPos.x - sunR / 2, sunPos.y - sunR / 2, sunR, sunR);

        g.setColor(new Color(165, 135, 100));
        Path2D.Double range = new Path2D.Double();
        range.moveTo(-50, H * 0.40);
        for (Point2D.Double m : mesas) range.lineTo(m.x, H * 0.40 - m.y * 0.45);
        range.lineTo(W + 50, H * 0.40);
        range.closePath();
        g.fill(range);

        g.setPaint(new GradientPaint(0, (float) (H * 0.38), new Color(196, 168, 122), 0, H, new Color(158, 128, 88)));
        g.fillRect(0, (int) (H * 0.38), W, (int) (H * 0.62));

        drawCracks(g);
    }

    private void drawCracks(Graphics2D g) {
        g.setStroke(new BasicStroke(1.1f));
        g.setColor(new Color(95, 70, 50, 90));
        for (Path2D.Double crack : crackLines) g.draw(crack);
        g.setStroke(new BasicStroke(1));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(224, 150, 60));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        centerText(g, "DUST BOWL RALLY", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Mad Max style post-apocalyptic desert racing", H / 2 - 90);

        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        centerText(g, "[1] Player vs Player      [2] Player vs Computer", H / 2 - 20);
        centerText(g, "P1: W A S D  +  SPACE boost", H / 2 + 20);
        centerText(g, "P2: Arrow Keys + ENTER boost  (disabled vs Computer)", H / 2 + 46);
        centerText(g, "Beware the roaming dust devils!", H / 2 + 76);
        centerText(g, "Press 1 or 2 to start", H / 2 + 110);
    }

    private void centerText(Graphics2D g, String s, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (W - fm.stringWidth(s)) / 2, y);
    }

    private void drawCountdown(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 90));
        g.setColor(new Color(224, 150, 60));
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
        g.setColor(new Color(40, 28, 18, 150));
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
        g.setColor(new Color(224, 150, 60));
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
