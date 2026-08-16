import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class MadMaxStylePostApocalypticRacing9 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Volcanic Ridge — Mad Max Style Post-Apocalyptic Racing");
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

    enum ParticleKind { DUST, SMOKE, SPARK, DEBRIS, ASH, FIRE }

    Particle(double x, double y, double vx, double vy, double life, double size, double growth, Color color, ParticleKind kind) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.life = life; this.maxLife = life;
        this.size = size; this.growth = growth;
        this.color = color; this.kind = kind;
    }

    boolean update(double dt) {
        x += vx * dt;
        y += vy * dt;
        if (kind != ParticleKind.ASH) {
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
 * Rounded-triangle / teardrop loop track. Three vertices, each rounded with
 * its own fillet radius (uneven), connected by straights. The centerline is a
 * simple closed polyline with a parallel cumulative-arc-length array used for
 * both lap counting and offset-based edge rendering/collision (via per-point
 * normals) — same technique as the reference game, generalized to any convex
 * polygon shape instead of just an oval.
 */
class Track {
    final Point2D.Double[] vertices;
    final double[] cornerRadii;
    final double trackWidth, halfWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;

    Track(Point2D.Double[] vertices, double[] cornerRadii, double trackWidth) {
        this.vertices = vertices;
        this.cornerRadii = cornerRadii;
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        build();
    }

    private void build() {
        int n = vertices.length;
        Point2D.Double[] entry = new Point2D.Double[n];
        Point2D.Double[] exit = new Point2D.Double[n];
        Point2D.Double[] center = new Point2D.Double[n];
        double[] sweep = new double[n];
        double[] aEntry = new double[n];

        for (int i = 0; i < n; i++) {
            Point2D.Double prev = vertices[(i - 1 + n) % n];
            Point2D.Double cur = vertices[i];
            Point2D.Double next = vertices[(i + 1) % n];
            double dax = prev.x - cur.x, day = prev.y - cur.y;
            double dbx = next.x - cur.x, dby = next.y - cur.y;
            double lenA = Math.hypot(dax, day), lenB = Math.hypot(dbx, dby);
            dax /= lenA; day /= lenA; dbx /= lenB; dby /= lenB;
            double dot = Util.clamp(dax * dbx + day * dby, -1, 1);
            double theta = Math.acos(dot);
            double r = cornerRadii[i];
            double t = r / Math.tan(theta / 2);
            double d = r / Math.sin(theta / 2);
            entry[i] = new Point2D.Double(cur.x + dax * t, cur.y + day * t);
            exit[i] = new Point2D.Double(cur.x + dbx * t, cur.y + dby * t);
            double bx = dax + dbx, by = day + dby;
            double blen = Math.hypot(bx, by);
            bx /= blen; by /= blen;
            center[i] = new Point2D.Double(cur.x + bx * d, cur.y + by * d);
            double a1 = Math.atan2(entry[i].y - center[i].y, entry[i].x - center[i].x);
            double a2 = Math.atan2(exit[i].y - center[i].y, exit[i].x - center[i].x);
            sweep[i] = Util.normalizeAngle(a2 - a1);
            aEntry[i] = a1;
        }

        centerline.clear();
        for (int i = 0; i < n; i++) {
            int prevI = (i - 1 + n) % n;
            Point2D.Double from = exit[prevI];
            Point2D.Double to = entry[i];
            double segLen = from.distance(to);
            int steps = Math.max(8, (int) Math.round(segLen / 14.0));
            for (int s = 0; s < steps; s++) {
                double t = s / (double) steps;
                centerline.add(new Point2D.Double(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t));
            }
            double sw = sweep[i];
            int arcSteps = Math.max(10, (int) Math.round(Math.abs(sw) / Math.PI * 70));
            for (int s = 0; s < arcSteps; s++) {
                double t = s / (double) arcSteps;
                double a = aEntry[i] + sw * t;
                centerline.add(new Point2D.Double(center[i].x + Math.cos(a) * cornerRadii[i], center[i].y + Math.sin(a) * cornerRadii[i]));
            }
        }

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
        g.setColor(new Color(52, 48, 46));
        g.fill(road);

        g.setColor(new Color(30, 27, 26));
        g.setStroke(new BasicStroke(4));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(255, 140, 40, 150));
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

/**
 * Lava-river hazard strip. Its own sampled polyline (with cumulative length)
 * lets us pick a random point along it for the periodic fire-fountain
 * eruptions — the signature new mechanic of this game.
 */
class LavaRiver {
    final List<Point2D.Double> points = new ArrayList<>();
    final double[] cumLen;
    final double totalLen;
    final double width;

    LavaRiver(List<Point2D.Double> controlPoints, double width, int samplesPerSeg) {
        this.width = width;
        for (int i = 0; i < controlPoints.size() - 1; i++) {
            Point2D.Double a = controlPoints.get(i), b = controlPoints.get(i + 1);
            for (int s = 0; s < samplesPerSeg; s++) {
                double t = s / (double) samplesPerSeg;
                points.add(new Point2D.Double(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t));
            }
        }
        points.add(controlPoints.get(controlPoints.size() - 1));
        int n = points.size();
        cumLen = new double[n];
        cumLen[0] = 0;
        for (int i = 1; i < n; i++) cumLen[i] = cumLen[i - 1] + points.get(i - 1).distance(points.get(i));
        totalLen = cumLen[n - 1];
    }

    Point2D.Double pointAtFraction(double frac) {
        double target = Util.clamp(frac, 0, 1) * totalLen;
        for (int i = 0; i < cumLen.length - 1; i++) {
            if (target <= cumLen[i + 1]) {
                double segLen = cumLen[i + 1] - cumLen[i];
                double t = segLen < 1e-6 ? 0 : (target - cumLen[i]) / segLen;
                Point2D.Double a = points.get(i), b = points.get(i + 1);
                return new Point2D.Double(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
            }
        }
        return points.get(points.size() - 1);
    }

    void draw(Graphics2D g, double time) {
        Path2D.Double path = new Path2D.Double();
        for (int i = 0; i < points.size(); i++) {
            Point2D.Double p = points.get(i);
            if (i == 0) path.moveTo(p.x, p.y); else path.lineTo(p.x, p.y);
        }
        double pulse = 0.6 + 0.4 * Math.sin(time * 3.0);
        g.setStroke(new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(35, 14, 10));
        g.draw(path);
        g.setStroke(new BasicStroke((float) (width * 0.62), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(220, 90 + (int) (30 * pulse), 20));
        g.draw(path);
        g.setStroke(new BasicStroke((float) (width * 0.28), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 200 + (int) (40 * pulse), 80 + (int) (60 * pulse)));
        g.draw(path);
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
                g.setColor(new Color(32, 30, 30));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(255, 110, 30));
                g.drawLine((int) (-radius * 0.4), (int) (-radius * 0.5), (int) (radius * 0.3), (int) (radius * 0.2));
                g.setColor(new Color(12, 10, 10));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                break;
            case CACTUS:
                g.setColor(new Color(48, 40, 36));
                g.fillRoundRect((int) (-radius / 3), (int) -radius, (int) (radius * 2 / 3), (int) (radius * 2), 8, 8);
                g.fillRoundRect((int) -radius, (int) (-radius / 3), (int) radius, (int) (radius * 2 / 3), 8, 8);
                g.fillRoundRect((int) (radius / 3), (int) (-radius * 0.7), (int) (radius * 2 / 3), (int) radius, 8, 8);
                g.setColor(new Color(255, 120, 40));
                g.fillOval((int) (-radius / 3), (int) -radius - 2, 4, 4);
                break;
            case WRECK:
                g.setColor(new Color(50, 30, 26));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                break;
            case TUMBLEWEED:
                g.setColor(new Color(120, 112, 100));
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
        Color c = onTrack ? new Color(90, 82, 78, 130) : new Color(60, 50, 46, 170);
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
                    Math.random() < 0.5 ? new Color(255, 120, 30) : new Color(60, 58, 56), Particle.ParticleKind.SMOKE));
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
    private enum EruptionPhase { COOLDOWN, TELEGRAPH }

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
    private LavaRiver lavaRiver;
    private final List<List<Point2D.Double>> lavaCracks = new ArrayList<>();
    private Point2D.Double volcanoPeak;

    private double countdownTimer;
    private double raceTime;
    private double timePulse = 0;
    private final List<Point2D.Double> mountains = new ArrayList<>();
    private final BufferedImage sceneBuffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
    private final Random rng = new Random();

    private EruptionPhase eruptionPhase = EruptionPhase.COOLDOWN;
    private double eruptionTimer = 2.0;
    private double eruptionX, eruptionY;
    private double shakeTimer = 0;
    private double ashSpawnTimer = 0;
    private double volcanoSmokeTimer = 0;
    private double emberSpawnTimer = 0;

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        setupWorld();
        buildMountains();
        buildLavaCracks();
    }

    private void buildMountains() {
        Random r = new Random(7);
        double x = -50;
        while (x < W + 50) {
            mountains.add(new Point2D.Double(x, 90 + r.nextInt(90)));
            x += 40 + r.nextInt(40);
        }
        volcanoPeak = new Point2D.Double(190, 96);
    }

    private void buildLavaCracks() {
        Random r = new Random(99);
        double[][] anchors = { {90, 640}, {1150, 660}, {1050, 130}, {150, 320} };
        for (double[] anchor : anchors) {
            List<Point2D.Double> crack = new ArrayList<>();
            double x = anchor[0], y = anchor[1];
            crack.add(new Point2D.Double(x, y));
            double dir = r.nextDouble() * Math.PI * 2;
            for (int i = 0; i < 5; i++) {
                dir += (r.nextDouble() - 0.5) * 1.4;
                double len = 25 + r.nextDouble() * 35;
                x += Math.cos(dir) * len;
                y += Math.sin(dir) * len;
                crack.add(new Point2D.Double(x, y));
            }
            lavaCracks.add(crack);
        }
    }

    private void setupWorld() {
        Point2D.Double v1 = new Point2D.Double(300, 180);
        Point2D.Double v2 = new Point2D.Double(1000, 260);
        Point2D.Double v3 = new Point2D.Double(650, 600);
        track = new Track(new Point2D.Double[]{v1, v2, v3}, new double[]{100, 145, 90}, 120);

        List<Point2D.Double> lavaControl = new ArrayList<>();
        lavaControl.add(new Point2D.Double(355, 589));
        lavaControl.add(new Point2D.Double(430, 540));
        lavaControl.add(new Point2D.Double(500, 480));
        lavaControl.add(new Point2D.Double(560, 420));
        lavaControl.add(new Point2D.Double(693, 308));
        lavaRiver = new LavaRiver(lavaControl, 46, 10);

        obstacles.clear();
        Random r = new Random(42);
        int n = track.centerline.size();
        for (int i = 0; i < n; i += 8) {
            if (r.nextDouble() < 0.5) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 20 + r.nextDouble() * 60;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.ROCK : Obstacle.Kind.CACTUS;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(10)));
            }
        }
        double[] hazardFracs = {0.15, 0.42, 0.68, 0.85};
        for (double frac : hazardFracs) {
            int idx = (int) (n * frac);
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
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(200, 60, 40), new Color(230, 200, 120), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(60, 110, 190), new Color(220, 220, 220), vsAI ? "AI" : "2", particles);
        p1.lastArcLen = track.progress(p1.x, p1.y);
        p2.lastArcLen = track.progress(p2.x, p2.y);
        cars.add(p1);
        cars.add(p2);
        aiDriver = vsAI ? new AIDriver(p2, track) : null;
        countdownTimer = 3.999;
        raceTime = 0;
        eruptionPhase = EruptionPhase.COOLDOWN;
        eruptionTimer = 2.0;
        shakeTimer = 0;
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
        timePulse += dt;
        for (Obstacle o : obstacles) o.update(dt, -30, W + 30, -30, H + 30);

        updateAmbient(dt);
        updateEruptions(dt);
        if (shakeTimer > 0) shakeTimer = Math.max(0, shakeTimer - dt);

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

    private void updateAmbient(double dt) {
        ashSpawnTimer -= dt;
        if (ashSpawnTimer <= 0) {
            ashSpawnTimer = 0.05;
            double x = rng.nextDouble() * W;
            particles.add(new Particle(x, -10, (rng.nextDouble() - 0.5) * 10, 20 + rng.nextDouble() * 25,
                    5 + rng.nextDouble() * 4, 2 + rng.nextDouble() * 2, 0,
                    new Color(190, 188, 182, 170), Particle.ParticleKind.ASH));
        }
        volcanoSmokeTimer -= dt;
        if (volcanoSmokeTimer <= 0) {
            volcanoSmokeTimer = 0.4;
            particles.add(new Particle(volcanoPeak.x, volcanoPeak.y, (rng.nextDouble() - 0.5) * 8, -16 - rng.nextDouble() * 10,
                    3.5 + rng.nextDouble() * 2, 10 + rng.nextDouble() * 8, 9,
                    new Color(55, 50, 48, 130), Particle.ParticleKind.SMOKE));
        }
        emberSpawnTimer -= dt;
        if (emberSpawnTimer <= 0) {
            emberSpawnTimer = 0.15;
            Point2D.Double p = lavaRiver.pointAtFraction(rng.nextDouble());
            particles.add(new Particle(p.x, p.y, (rng.nextDouble() - 0.5) * 20, -30 - rng.nextDouble() * 30,
                    0.4 + rng.nextDouble() * 0.3, 2 + rng.nextDouble() * 2, 2,
                    new Color(255, 170, 60, 200), Particle.ParticleKind.FIRE));
        }
    }

    private void updateEruptions(double dt) {
        if (eruptionPhase == EruptionPhase.COOLDOWN) {
            eruptionTimer -= dt;
            if (eruptionTimer <= 0) {
                Point2D.Double p = lavaRiver.pointAtFraction(rng.nextDouble());
                eruptionX = p.x;
                eruptionY = p.y;
                eruptionPhase = EruptionPhase.TELEGRAPH;
                eruptionTimer = 0.7;
            }
        } else {
            eruptionTimer -= dt;
            if (eruptionTimer <= 0) {
                fireEruption();
                eruptionPhase = EruptionPhase.COOLDOWN;
                eruptionTimer = 2.5 + rng.nextDouble() * 2.5;
            }
        }
    }

    private void fireEruption() {
        shakeTimer = 0.3;
        for (int i = 0; i < 55; i++) {
            double a = -Math.PI / 2 + (rng.nextDouble() - 0.5) * Math.PI * 0.9;
            double sp = 90 + rng.nextDouble() * 220;
            Color c = rng.nextDouble() < 0.5 ? new Color(255, 140, 30) : new Color(255, 210, 90);
            particles.add(new Particle(eruptionX, eruptionY, Math.cos(a) * sp * 0.4, Math.sin(a) * sp,
                    0.45 + rng.nextDouble() * 0.5, 4 + rng.nextDouble() * 7, 6, c, Particle.ParticleKind.FIRE));
        }
        for (int i = 0; i < 16; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            particles.add(new Particle(eruptionX, eruptionY, Math.cos(a) * 40, Math.sin(a) * 40 - 40,
                    0.6 + rng.nextDouble() * 0.4, 8 + rng.nextDouble() * 6, 8,
                    new Color(70, 60, 58, 200), Particle.ParticleKind.SMOKE));
        }
        for (Car c : cars) {
            if (!c.alive) continue;
            double d = Math.hypot(c.x - eruptionX, c.y - eruptionY);
            if (d < 100) {
                c.damage(28);
                c.spinOut(0.4);
            }
        }
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
        lavaRiver.draw(sg, timePulse);
        if (eruptionPhase == EruptionPhase.TELEGRAPH) {
            float t = (float) Util.clamp(1 - eruptionTimer / 0.7, 0, 1);
            int r = (int) (18 + 55 * t);
            sg.setPaint(new RadialGradientPaint(new Point((int) eruptionX, (int) eruptionY), Math.max(1, r),
                    new float[]{0f, 1f}, new Color[]{new Color(255, 180, 60, (int) (190 * t)), new Color(255, 180, 60, 0)}));
            sg.fillOval((int) eruptionX - r, (int) eruptionY - r, r * 2, r * 2);
        }
        for (Obstacle o : obstacles) o.draw(sg);
        particles.draw(sg);
        for (Car c : cars) c.draw(sg);
        sg.dispose();

        Graphics2D g = (Graphics2D) g0;
        int ox = 0, oy = 0;
        if (shakeTimer > 0) {
            double mag = 6.0 * (shakeTimer / 0.3);
            ox = (int) ((rng.nextDouble() * 2 - 1) * mag);
            oy = (int) ((rng.nextDouble() * 2 - 1) * mag);
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
        g.setPaint(new GradientPaint(0, 0, new Color(70, 8, 12), 0, (float) (H * 0.5), new Color(210, 80, 25)));
        g.fillRect(0, 0, W, (int) (H * 0.5));

        double pulse = 1 + 0.04 * Math.sin(timePulse * 1.2);
        int glowR = (int) (55 * pulse);
        g.setPaint(new RadialGradientPaint(new Point(W - 200, 130), glowR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(255, 200, 120, 200), new Color(255, 120, 40, 90), new Color(255, 120, 40, 0)}));
        g.fillOval(W - 200 - glowR * 3, 130 - glowR * 3, glowR * 6, glowR * 6);

        g.setColor(new Color(18, 15, 14));
        Path2D.Double range = new Path2D.Double();
        range.moveTo(-50, H * 0.42);
        for (Point2D.Double m : mountains) range.lineTo(m.x, H * 0.42 - m.y * 0.5);
        range.lineTo(W + 50, H * 0.42);
        range.closePath();
        g.fill(range);

        double vpulse = 0.6 + 0.4 * Math.sin(timePulse * 2.2);
        g.setPaint(new RadialGradientPaint(new Point((int) volcanoPeak.x, (int) volcanoPeak.y + 4), 26,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 160 + (int) (40 * vpulse), 40, 220), new Color(255, 160, 40, 0)}));
        g.fillOval((int) volcanoPeak.x - 26, (int) volcanoPeak.y - 22, 52, 52);

        g.setPaint(new GradientPaint(0, (float) (H * 0.4), new Color(38, 34, 32), 0, H, new Color(10, 9, 9)));
        g.fillRect(0, (int) (H * 0.4), W, (int) (H * 0.6));

        drawLavaCracks(g);
    }

    private void drawLavaCracks(Graphics2D g) {
        double pulse = 0.6 + 0.4 * Math.sin(timePulse * 2.5);
        for (List<Point2D.Double> crack : lavaCracks) {
            Path2D.Double path = new Path2D.Double();
            for (int i = 0; i < crack.size(); i++) {
                Point2D.Double p = crack.get(i);
                if (i == 0) path.moveTo(p.x, p.y); else path.lineTo(p.x, p.y);
            }
            g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(20, 10, 8));
            g.draw(path);
            g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 110 + (int) (40 * pulse), 30, 210));
            g.draw(path);
        }
        g.setStroke(new BasicStroke(1));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(255, 120, 30));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        centerText(g, "VOLCANIC RIDGE", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Mad Max style post-apocalyptic racing across a volcanic mountain circuit", H / 2 - 90);

        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        centerText(g, "[1] Player vs Player      [2] Player vs Computer", H / 2 - 20);
        centerText(g, "P1: W A S D  +  SPACE boost", H / 2 + 20);
        centerText(g, "P2: Arrow Keys + ENTER boost  (disabled vs Computer)", H / 2 + 46);
        g.setColor(new Color(255, 160, 60));
        centerText(g, "Beware the lava river — eruptions damage cars and shake the ground", H / 2 + 80);
        g.setColor(Color.WHITE);
        centerText(g, "Press 1 or 2 to start", H / 2 + 116);
    }

    private void centerText(Graphics2D g, String s, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (W - fm.stringWidth(s)) / 2, y);
    }

    private void drawCountdown(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 90));
        g.setColor(new Color(255, 160, 40));
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
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(x, 16, 190, 60, 10, 10);
        g.setColor(new Color(255, 140, 60));
        g.drawRoundRect(x, 16, 190, 60, 10, 10);
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
        g.setColor(new Color(255, 140, 40));
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
        g.setColor(new Color(255, 140, 40));
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
