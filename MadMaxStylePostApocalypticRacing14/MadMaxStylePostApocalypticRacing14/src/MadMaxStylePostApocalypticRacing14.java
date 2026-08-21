import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class MadMaxStylePostApocalypticRacing14 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Vulture Pass — High Desert Mountain Racing");
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
 * Mountain switchback pass: two long "legs" (a descending zigzag hairpin leg
 * on the left, an ascending zigzag hairpin leg on the right) joined by wide
 * loop turns at the top and bottom. Because each leg is a function x(y) with
 * y strictly monotonic, a leg can never self-intersect, and because the two
 * legs occupy well-separated x ranges (and the two end turns occupy
 * well-separated y ranges) the whole closed loop cannot self-intersect
 * either, no matter how the hairpin zigzags wiggle within their leg.
 */
class Track {
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;
    final double trackWidth, halfWidth;
    boolean[] cliffZone;
    int[] cliffSide;

    Track(double trackWidth) {
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        build();
    }

    private static double keyframe(double t, double[] ts, double[] vs) {
        for (int i = 0; i < ts.length - 1; i++) {
            if (t <= ts[i + 1] + 1e-9) {
                double local = (t - ts[i]) / (ts[i + 1] - ts[i]);
                return vs[i] + (vs[i + 1] - vs[i]) * local;
            }
        }
        return vs[vs.length - 1];
    }

    private void build() {
        double legLeftX = 260, legRightX = 1020;
        double yTopJoin = 190, yBottomJoin = 560;
        double topApexY = 110, bottomApexY = 610;
        double amp = 65;
        double midX = (legLeftX + legRightX) / 2.0;
        double rx = (legRightX - legLeftX) / 2.0;

        List<Point2D.Double> raw = new ArrayList<>();
        List<Boolean> cliffRaw = new ArrayList<>();

        // Left leg: descending switchback, 3 hairpins. Outer apex of the
        // first hairpin (t=0.25) is marked as a cliff-edge stretch.
        double[] leftT = {0.0, 0.25, 0.5, 0.75, 1.0};
        double[] leftV = {0.0, 1.0, -1.0, 1.0, 0.0};
        int legN = 90;
        for (int i = 0; i < legN; i++) {
            double t = i / (double) (legN - 1);
            double y = yTopJoin + t * (yBottomJoin - yTopJoin);
            double v = keyframe(t, leftT, leftV);
            double x = legLeftX + v * amp;
            raw.add(new Point2D.Double(x, y));
            cliffRaw.add(Math.abs(t - 0.25) < 0.045);
        }

        // Bottom wide loop turn (safe, no cliff).
        int turnN = 50;
        for (int i = 0; i < turnN; i++) {
            double f = i / (double) (turnN - 1);
            double theta = Math.PI * (1 - f);
            double x = midX + rx * Math.cos(theta);
            double y = yBottomJoin + (bottomApexY - yBottomJoin) * Math.sin(theta);
            raw.add(new Point2D.Double(x, y));
            cliffRaw.add(false);
        }

        // Right leg: ascending switchback, 2 hairpins. Outer apex of the
        // second hairpin (t=0.66) is marked as a cliff-edge stretch.
        double[] rightT = {0.0, 0.33, 0.66, 1.0};
        double[] rightV = {0.0, -1.0, 1.0, 0.0};
        int legN2 = 70;
        for (int i = 0; i < legN2; i++) {
            double t = i / (double) (legN2 - 1);
            double y = yBottomJoin + t * (yTopJoin - yBottomJoin);
            double v = keyframe(t, rightT, rightV);
            double x = legRightX + v * amp;
            raw.add(new Point2D.Double(x, y));
            cliffRaw.add(Math.abs(t - 0.66) < 0.045);
        }

        // Top wide loop turn (safe, no cliff) - closes the loop.
        for (int i = 0; i < turnN; i++) {
            double f = i / (double) (turnN - 1);
            double theta = Math.PI * f;
            double x = midX + rx * Math.cos(theta);
            double y = yTopJoin - (yTopJoin - topApexY) * Math.sin(theta);
            raw.add(new Point2D.Double(x, y));
            cliffRaw.add(false);
        }

        List<Point2D.Double> sm = smooth(raw, 2);
        int n = sm.size();
        centerline.addAll(sm);
        cliffZone = new boolean[n];
        for (int i = 0; i < n; i++) cliffZone[i] = cliffRaw.get(i);

        cumulativeLength = new double[n];
        cumulativeLength[0] = 0;
        for (int i = 1; i < n; i++) {
            cumulativeLength[i] = cumulativeLength[i - 1] + centerline.get(i - 1).distance(centerline.get(i));
        }
        totalLength = cumulativeLength[n - 1] + centerline.get(n - 1).distance(centerline.get(0));

        cliffSide = new int[n];
        for (int i = 0; i < n; i++) {
            Point2D.Double prev = centerline.get((i - 1 + n) % n);
            Point2D.Double p = centerline.get(i);
            Point2D.Double next = centerline.get((i + 1) % n);
            double tx = next.x - prev.x, ty = next.y - prev.y;
            double len = Math.hypot(tx, ty);
            if (len < 1e-6) len = 1;
            normals.add(new Point2D.Double(-ty / len, tx / len));
            double t1x = p.x - prev.x, t1y = p.y - prev.y;
            double t2x = next.x - p.x, t2y = next.y - p.y;
            double cross = t1x * t2y - t1y * t2x;
            cliffSide[i] = cross >= 0 ? 1 : -1;
        }
    }

    private List<Point2D.Double> smooth(List<Point2D.Double> pts, int iters) {
        List<Point2D.Double> cur = pts;
        int n = pts.size();
        for (int it = 0; it < iters; it++) {
            List<Point2D.Double> next = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                Point2D.Double prev = cur.get((i - 1 + n) % n);
                Point2D.Double p = cur.get(i);
                Point2D.Double nx = cur.get((i + 1) % n);
                next.add(new Point2D.Double(0.25 * prev.x + 0.5 * p.x + 0.25 * nx.x,
                        0.25 * prev.y + 0.5 * p.y + 0.25 * nx.y));
            }
            cur = next;
        }
        return cur;
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

    double signedOffsetAt(double x, double y, int idx) {
        Point2D.Double p = centerline.get(idx);
        Point2D.Double n = normals.get(idx);
        return (x - p.x) * n.x + (y - p.y) * n.y;
    }

    boolean isCliffAt(int idx) { return cliffZone[idx]; }

    int cliffSideAt(int idx) { return cliffSide[idx]; }

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
        g.setColor(new Color(132, 88, 62));
        g.fill(road);

        g.setColor(new Color(96, 66, 48));
        g.setStroke(new BasicStroke(3));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(224, 178, 120, 150));
        Path2D.Double mid = new Path2D.Double();
        for (int i = 0; i < centerline.size(); i++) {
            Point2D.Double p = centerline.get(i);
            if (i == 0) mid.moveTo(p.x, p.y); else mid.lineTo(p.x, p.y);
        }
        mid.closePath();
        g.draw(mid);
        g.setStroke(new BasicStroke(1));

        drawCliffs(g);
        drawStartLine(g);
    }

    private void drawCliffs(Graphics2D g) {
        int n = centerline.size();
        int i = 0;
        while (i < n) {
            if (!cliffZone[i]) { i++; continue; }
            int start = i;
            while (i < n && cliffZone[i]) i++;
            drawCliffSegment(g, start, i - 1);
        }
    }

    private void drawCliffSegment(Graphics2D g, int start, int end) {
        int side = cliffSide[(start + end) / 2];
        Path2D.Double chasm = new Path2D.Double();
        boolean first = true;
        for (int i = start; i <= end; i++) {
            Point2D.Double p = pointAtOffset(i, side * halfWidth);
            if (first) { chasm.moveTo(p.x, p.y); first = false; } else chasm.lineTo(p.x, p.y);
        }
        for (int i = end; i >= start; i--) {
            Point2D.Double p = pointAtOffset(i, side * (halfWidth + 55));
            chasm.lineTo(p.x, p.y);
        }
        chasm.closePath();
        g.setColor(new Color(32, 18, 16));
        g.fill(chasm);
        g.setColor(new Color(64, 32, 24));
        g.setStroke(new BasicStroke(2));
        g.draw(chasm);

        g.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{10, 8}, 0));
        g.setColor(new Color(232, 172, 40));
        Path2D.Double lip = new Path2D.Double();
        first = true;
        for (int i = start; i <= end; i++) {
            Point2D.Double p = pointAtOffset(i, side * halfWidth);
            if (first) { lip.moveTo(p.x, p.y); first = false; } else lip.lineTo(p.x, p.y);
        }
        g.draw(lip);
        g.setStroke(new BasicStroke(1));
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
                g.setColor(new Color(104, 76, 60));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(76, 52, 40));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                break;
            case CACTUS:
                g.setColor(new Color(66, 104, 62));
                g.fillRoundRect((int) (-radius / 3), (int) -radius, (int) (radius * 2 / 3), (int) (radius * 2), 8, 8);
                g.fillRoundRect((int) -radius, (int) (-radius / 3), (int) radius, (int) (radius * 2 / 3), 8, 8);
                g.fillRoundRect((int) (radius / 3), (int) (-radius * 0.7), (int) (radius * 2 / 3), (int) radius, 8, 8);
                break;
            case WRECK:
                g.setColor(new Color(86, 42, 30));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                break;
            case TUMBLEWEED:
                g.setColor(new Color(160, 124, 62));
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

    boolean falling = false;
    double fallTimer = 0;
    int lastGoodIndex = 0;

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
    private static final double FALL_MARGIN = 45;
    private static final double FALL_DURATION = 0.65;

    private double dustTimer = 0;

    Car(double x, double y, double angle, Color bodyColor, Color trimColor, String label, ParticleSystem particles) {
        this.x = x; this.y = y; this.angle = angle;
        this.bodyColor = bodyColor; this.trimColor = trimColor;
        this.label = label;
        this.particles = particles;
    }

    double radius() { return RADIUS; }

    void update(double dt, InputState in, Track track) {
        if (falling) {
            fallTimer -= dt;
            angle += 16 * dt;
            speed *= 0.92;
            vx *= 0.9; vy *= 0.9;
            x += vx * dt * 0.3;
            y += vy * dt * 0.3;
            if (fallTimer <= 0) {
                falling = false;
                health = 0;
                wreck();
            }
            return;
        }
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

        boolean onTrackBefore = track.distanceFromCenterline(x, y) < track.halfWidth;
        x += vx * dt;
        y += vy * dt;

        int idx = track.nearestIndex(x, y);
        double signedOff = track.signedOffsetAt(x, y, idx);
        double absOff = Math.abs(signedOff);
        boolean onTrackNow = absOff < track.halfWidth;
        if (onTrackNow) lastGoodIndex = idx;

        boolean cliffFall = !onTrackNow && track.isCliffAt(idx)
                && absOff > track.halfWidth + FALL_MARGIN
                && ((signedOff > 0 && track.cliffSideAt(idx) > 0) || (signedOff < 0 && track.cliffSideAt(idx) < 0));
        if (cliffFall) {
            startFalling();
        }

        double s = track.cumulativeLength[idx];
        double delta = s - lastArcLen;
        if (delta < -track.totalLength / 2) delta += track.totalLength;
        else if (delta > track.totalLength / 2) delta -= track.totalLength;
        unwrappedDistance += delta;
        lastArcLen = s;
        lap = Math.max(0, (int) Math.floor(unwrappedDistance / track.totalLength));

        updateBoost();
        emitDust(dt, onTrackBefore);
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
        Color c = onTrack ? new Color(210, 175, 130, 120) : new Color(190, 130, 80, 160);
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
        if (!alive || falling) return;
        health -= amount;
        if (health <= 0) {
            health = 0;
            wreck();
        }
    }

    private void startFalling() {
        if (falling || !alive) return;
        falling = true;
        fallTimer = FALL_DURATION;
        for (int i = 0; i < 14; i++) {
            double a = Math.random() * Math.PI * 2;
            double sp = 30 + Math.random() * 80;
            particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp,
                    0.4 + Math.random() * 0.3, 3 + Math.random() * 4, 4,
                    new Color(150, 95, 65), Particle.ParticleKind.DUST));
        }
    }

    private void wreck() {
        alive = false;
        respawnTimer = 1.6;
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
        int idx = lastGoodIndex;
        Point2D.Double p = track.centerline.get(idx);
        Point2D.Double next = track.centerline.get((idx + 5) % track.centerline.size());
        x = p.x; y = p.y;
        angle = Math.atan2(next.y - p.y, next.x - p.x);
        spinTimer = 0;
        falling = false;
        fallTimer = 0;
    }

    void spinOut(double intensity) {
        spinTimer = Math.max(spinTimer, intensity);
    }

    void draw(Graphics2D g) {
        if (!alive) return;
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(angle);
        Composite oldComp = g.getComposite();
        double scale = 1.0;
        if (falling) {
            scale = Util.clamp(fallTimer / FALL_DURATION, 0, 1);
            g.scale(scale, scale);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Util.clamp(scale, 0.05, 1)));
        }

        if (boosting && !falling) {
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

        g.setComposite(oldComp);
        g.setTransform(old);

        if (!falling) {
            double hpFrac = Util.clamp(health / 100.0, 0, 1);
            g.setColor(Color.DARK_GRAY);
            g.fillRect((int) x - 16, (int) y - 26, 32, 4);
            g.setColor(hpFrac > 0.5 ? new Color(90, 200, 90) : hpFrac > 0.25 ? Color.ORANGE : Color.RED);
            g.fillRect((int) x - 16, (int) y - 26, (int) (32 * hpFrac), 4);
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

    InputState think(double dt, List<Obstacle> obstacles) {
        InputState in = new InputState();
        int idx = track.nearestIndex(car.x, car.y);
        int lookahead = 14;
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

/** Purely decorative vulture that loops around an elliptical path in the sky. */
class Vulture {
    private final double cx, cy, rx, ry, speed;
    private double angle;

    Vulture(double cx, double cy, double rx, double ry, double angle0, double speed) {
        this.cx = cx; this.cy = cy; this.rx = rx; this.ry = ry;
        this.angle = angle0; this.speed = speed;
    }

    void update(double dt) {
        angle += speed * dt;
    }

    double x() { return cx + Math.cos(angle) * rx; }
    double y() { return cy + Math.sin(angle) * ry; }

    void draw(Graphics2D g) {
        double px = x(), py = y();
        double tx = -rx * Math.sin(angle) * Math.signum(speed);
        double ty = ry * Math.cos(angle) * Math.signum(speed);
        double dirAngle = Math.atan2(ty, tx);
        double flap = Math.sin(angle * 7) * 3;

        AffineTransform old = g.getTransform();
        g.translate(px, py);
        g.rotate(dirAngle);
        g.setColor(new Color(25, 22, 18, 220));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(0, 0, -12, -6 - flap));
        g.draw(new Line2D.Double(0, 0, -12, 6 + flap));
        g.setTransform(old);
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
    private ParticleSystem particles = new ParticleSystem();
    private AIDriver aiDriver;

    private double countdownTimer;
    private double raceTime;
    private double sunPulse = 0;
    private final List<Point2D.Double> mountainsFar = new ArrayList<>();
    private final List<Point2D.Double> mountainsMid = new ArrayList<>();
    private final List<Point2D.Double> mountainsNear = new ArrayList<>();
    private final List<Vulture> vultures = new ArrayList<>();
    private final BufferedImage sceneBuffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        setupWorld();
        buildMountainBand(mountainsFar, 11, 30, 60, 70, 110);
        buildMountainBand(mountainsMid, 23, 55, 100, 55, 90);
        buildMountainBand(mountainsNear, 37, 90, 155, 45, 80);
        buildVultures();
    }

    private void buildMountainBand(List<Point2D.Double> list, long seed, int minH, int maxH, int minGap, int maxGap) {
        Random r = new Random(seed);
        double x = -60;
        while (x < W + 60) {
            list.add(new Point2D.Double(x, minH + r.nextInt(maxH - minH + 1)));
            x += minGap + r.nextInt(maxGap - minGap + 1);
        }
    }

    private void buildVultures() {
        Random r = new Random(99);
        for (int i = 0; i < 5; i++) {
            double cx = 120 + r.nextInt(1040);
            double cy = 55 + r.nextInt(110);
            double rx = 45 + r.nextInt(90);
            double ry = 16 + r.nextInt(18);
            double angle0 = r.nextDouble() * Math.PI * 2;
            double speed = (0.15 + r.nextDouble() * 0.25) * (r.nextBoolean() ? 1 : -1);
            vultures.add(new Vulture(cx, cy, rx, ry, angle0, speed));
        }
    }

    private void setupWorld() {
        track = new Track(140);
        obstacles.clear();
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
        int[] hazardIdx = {(int) (n * 0.1), (int) (n * 0.38), (int) (n * 0.58), (int) (n * 0.85)};
        for (int idx : hazardIdx) {
            if (idx >= n) continue;
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
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-25);
        Point2D.Double p2pos = track.startPosition(25);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(205, 60, 40), new Color(230, 195, 110), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(70, 100, 165), new Color(220, 220, 210), vsAI ? "AI" : "2", particles);
        p1.lastArcLen = track.progress(p1.x, p1.y);
        p2.lastArcLen = track.progress(p2.x, p2.y);
        p1.lastGoodIndex = track.nearestIndex(p1.x, p1.y);
        p2.lastGoodIndex = track.nearestIndex(p2.x, p2.y);
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
        for (Vulture v : vultures) v.update(dt);

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

        for (Car c : cars) {
            if (!c.finished && c.lap >= TOTAL_LAPS) {
                c.finished = true;
                c.finishTime = raceTime;
            }
        }
        if (p1.finished && p2.finished) state = State.FINISHED;
    }

    private void resolveCarCollision(Car a, Car b) {
        if (!a.alive || !b.alive || a.falling || b.falling) return;
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
        if (!c.alive || c.falling) return;
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
        g.setPaint(new GradientPaint(0, 0, new Color(255, 232, 195), 0, (float) (H * 0.46), new Color(232, 178, 120)));
        g.fillRect(0, 0, W, (int) (H * 0.46));

        double pulse = 1 + 0.03 * Math.sin(sunPulse * 1.5);
        int sunR = (int) (66 * pulse);
        int sunX = W - 230, sunY = 100;
        g.setPaint(new RadialGradientPaint(new Point(sunX, sunY), sunR * 3,
                new float[]{0f, 0.35f, 1f},
                new Color[]{new Color(255, 253, 235, 235), new Color(255, 205, 110, 100), new Color(255, 205, 110, 0)}));
        g.fillOval(sunX - sunR * 3, sunY - sunR * 3, sunR * 6, sunR * 6);
        g.setColor(new Color(255, 250, 225));
        g.fillOval(sunX - sunR / 2, sunY - sunR / 2, sunR, sunR);

        for (Vulture v : vultures) v.draw(g);

        drawMountainBand(g, mountainsFar, H * 0.40, new Color(92, 55, 50));
        drawMountainBand(g, mountainsMid, H * 0.43, new Color(128, 76, 58));
        drawMountainBand(g, mountainsNear, H * 0.46, new Color(163, 96, 64));

        g.setPaint(new GradientPaint(0, (float) (H * 0.44), new Color(198, 132, 90), 0, H, new Color(150, 92, 58)));
        g.fillRect(0, (int) (H * 0.44), W, (int) (H * 0.56));
    }

    private void drawMountainBand(Graphics2D g, List<Point2D.Double> pts, double baselineY, Color color) {
        g.setColor(color);
        Path2D.Double p = new Path2D.Double();
        p.moveTo(-60, baselineY);
        for (Point2D.Double m : pts) p.lineTo(m.x, baselineY - m.y);
        p.lineTo(W + 60, baselineY);
        p.closePath();
        g.fill(p);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(230, 120, 40));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        centerText(g, "VULTURE PASS", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "High desert mountain-pass racing — mind the cliff edges", H / 2 - 90);

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
        g.setColor(new Color(235, 140, 55));
        g.fillRect(x + 10, 54, (int) (170 * Util.clamp(c.boostFuel / 100.0, 0, 1)), 8);
    }

    private void drawFinish(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, W, H);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 46));
        centerText(g, "PASS CONQUERED", H / 2 - 80);
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
