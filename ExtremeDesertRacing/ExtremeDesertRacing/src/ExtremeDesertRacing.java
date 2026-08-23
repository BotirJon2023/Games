import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ExtremeDesertRacing {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Dune Drift Rally — Extreme Desert Racing");
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

/**
 * All mutation and iteration is synchronized because the physics/game-loop thread
 * (add/update, from Car and GamePanel) and the Swing EDT (draw, from paintComponent)
 * both touch the underlying particle list; without this an intermittent
 * ConcurrentModificationException would occur.
 */
class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();

    synchronized void add(Particle p) { particles.add(p); }

    synchronized void update(double dt) {
        particles.removeIf(p -> !p.update(dt));
    }

    void spawnFirework(double x, double y) {
        Color[] palette = {
                new Color(255, 200, 90), new Color(255, 140, 70),
                new Color(255, 245, 210), new Color(230, 90, 90)
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
 * "Dune Drift Rally" track: a sine-perturbed polar closed curve, r(theta) = R0 + sum of
 * sine harmonics with INTEGER frequencies (so r(0) == r(2*PI) exactly, the curve closes
 * cleanly) and total harmonic amplitude kept well below R0 (so r(theta) stays strictly
 * positive for every theta). Because r(theta) is a single-valued function of theta with
 * r > 0 everywhere, each ray from the center crosses the curve exactly once — two distinct
 * theta values (mod 2*PI) can never map to the same (x,y) point — so the loop is guaranteed
 * simple (non-self-intersecting) by construction. The multiple sine harmonics at different
 * frequencies/amplitudes/phases make it read as an organic chain of rolling sand dunes
 * rather than a plain circle.
 *
 * Lap counting and edge rendering reuse the standard centerline + cumulative-arc-length +
 * per-point-normal technique: nearest centerline point drives progress, offset-by-normal
 * points drive the outer/inner track edges.
 */
class Track {
    final double centerX, centerY, baseRadius, trackWidth, halfWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    final List<DuneJump> jumps = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;

    Track(double centerX, double centerY, double baseRadius, double trackWidth) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.baseRadius = baseRadius;
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        build();
    }

    private double radiusAt(double theta) {
        return baseRadius
                + 28 * Math.sin(3 * theta + 0.6)
                + 16 * Math.sin(5 * theta + 2.1)
                + 9 * Math.sin(7 * theta + 4.0)
                + 5 * Math.sin(11 * theta + 1.3);
    }

    private void build() {
        int samples = 900;
        for (int i = 0; i < samples; i++) {
            double theta = (2 * Math.PI * i) / samples;
            double r = radiusAt(theta);
            centerline.add(new Point2D.Double(centerX + r * Math.cos(theta), centerY + r * Math.sin(theta)));
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

        // Make sure normals point OUTWARD (away from the track center) consistently.
        Point2D.Double p0 = centerline.get(0);
        Point2D.Double n0 = normals.get(0);
        double dHere = Point2D.distance(p0.x, p0.y, centerX, centerY);
        double dPoke = Point2D.distance(p0.x + n0.x * 5, p0.y + n0.y * 5, centerX, centerY);
        if (dPoke < dHere) {
            for (Point2D.Double nm : normals) { nm.x = -nm.x; nm.y = -nm.y; }
        }

        int zoneLen = Math.max(12, n / 55);
        int[] centers = { n / 8, (3 * n) / 8, (5 * n) / 8, (7 * n) / 8 };
        for (int c : centers) {
            jumps.add(new DuneJump(c - zoneLen / 2, c + zoneLen / 2, c));
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
        int i = ((index % n) + n) % n;
        Point2D.Double p = centerline.get(i);
        Point2D.Double nrm = normals.get(i);
        return new Point2D.Double(p.x + nrm.x * offset, p.y + nrm.y * offset);
    }

    double tangentAngle(int index) {
        int n = centerline.size();
        int i = ((index % n) + n) % n;
        Point2D.Double nrm = normals.get(i);
        return Math.atan2(-nrm.x, nrm.y);
    }

    Point2D.Double startPosition(double laneOffset) {
        return pointAtOffset(2, laneOffset);
    }

    double startAngle() {
        return tangentAngle(2);
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
        g.setColor(new Color(196, 154, 96));
        g.fill(road);

        g.setColor(new Color(172, 130, 76, 140));
        for (int i = 0; i < centerline.size(); i += 5) {
            Point2D.Double o = pointAtOffset(i, halfWidth);
            Point2D.Double in = pointAtOffset(i, -halfWidth);
            g.draw(new Line2D.Double(o, in));
        }

        g.setColor(new Color(140, 100, 56));
        g.setStroke(new BasicStroke(4));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(240, 214, 160, 150));
        Path2D.Double mid = new Path2D.Double();
        for (int i = 0; i < centerline.size(); i++) {
            Point2D.Double p = centerline.get(i);
            if (i == 0) mid.moveTo(p.x, p.y); else mid.lineTo(p.x, p.y);
        }
        mid.closePath();
        g.draw(mid);
        g.setStroke(new BasicStroke(1));

        for (DuneJump dj : jumps) dj.draw(g, this);

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
 * Signature mechanic: a raised dune-crest ramp segment on the track (an index range along
 * the centerline). Crossing it at speed launches the car into brief airtime (see
 * Car.airborne / Car.startJump / Car.land) — while airborne, off-road friction penalties
 * are suspended, the car sprite visually scales up and casts a shrinking/moving shadow, and
 * landing well-aligned with the track direction grants a real speed boost while a hard/
 * off-angle landing triggers a bounce-spin and light damage.
 */
class DuneJump {
    final int startIndex, endIndex, midIndex;

    DuneJump(int startIndex, int endIndex, int midIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.midIndex = midIndex;
    }

    boolean contains(int idx) {
        return idx >= startIndex && idx <= endIndex;
    }

    void draw(Graphics2D g, Track track) {
        Path2D.Double ramp = new Path2D.Double();
        for (int i = startIndex; i <= endIndex; i++) {
            Point2D.Double o = track.pointAtOffset(i, track.halfWidth);
            if (i == startIndex) ramp.moveTo(o.x, o.y); else ramp.lineTo(o.x, o.y);
        }
        for (int i = endIndex; i >= startIndex; i--) {
            Point2D.Double in = track.pointAtOffset(i, -track.halfWidth);
            ramp.lineTo(in.x, in.y);
        }
        ramp.closePath();
        g.setColor(new Color(232, 202, 132, 220));
        g.fill(ramp);

        g.setColor(new Color(255, 238, 190, 200));
        g.setStroke(new BasicStroke(3));
        for (int i = startIndex; i <= endIndex; i += 3) {
            Point2D.Double o = track.pointAtOffset(i, track.halfWidth - 6);
            Point2D.Double in = track.pointAtOffset(i, -track.halfWidth + 6);
            g.draw(new Line2D.Double(o, in));
        }

        g.setColor(new Color(110, 74, 36, 220));
        g.setStroke(new BasicStroke(4));
        Point2D.Double so = track.pointAtOffset(startIndex, track.halfWidth);
        Point2D.Double si = track.pointAtOffset(startIndex, -track.halfWidth);
        g.draw(new Line2D.Double(so, si));
        Point2D.Double eo = track.pointAtOffset(endIndex, track.halfWidth);
        Point2D.Double ei = track.pointAtOffset(endIndex, -track.halfWidth);
        g.draw(new Line2D.Double(eo, ei));
        g.setStroke(new BasicStroke(1));
    }
}

class Obstacle {
    enum Kind { ROCK, CACTUS, WRECKED_JEEP, TUMBLEWEED }

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
                g.setColor(new Color(120, 96, 72));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(88, 68, 50));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.fillOval((int) (-radius * 0.4), (int) (-radius * 0.3), (int) radius, (int) (radius * 0.8));
                break;
            case CACTUS:
                g.setColor(new Color(66, 122, 72));
                g.fillRoundRect((int) (-radius * 0.35), (int) -radius, (int) (radius * 0.7), (int) (radius * 2), 8, 8);
                g.fillRoundRect((int) (-radius * 0.9), (int) (-radius * 0.3), (int) (radius * 0.6), (int) (radius * 0.5), 6, 6);
                g.fillRoundRect((int) (radius * 0.3), (int) (-radius * 0.6), (int) (radius * 0.6), (int) (radius * 0.5), 6, 6);
                g.setColor(new Color(46, 96, 52));
                g.drawRoundRect((int) (-radius * 0.35), (int) -radius, (int) (radius * 0.7), (int) (radius * 2), 8, 8);
                break;
            case WRECKED_JEEP:
                g.setColor(new Color(84, 92, 66));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(new Color(50, 40, 24));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) (radius * 0.3));
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                break;
            case TUMBLEWEED:
                g.setColor(new Color(150, 120, 70));
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

    boolean airborne = false;
    double airTimer = 0, airDuration = 0;
    double launchAngleDiff = 0;
    boolean justLandedClean = false;
    double landFlashTimer = 0;

    final Color bodyColor;
    final Color trimColor;
    final String label;
    final ParticleSystem particles;

    private static final double MAX_SPEED = 420;
    private static final double MAX_REVERSE = -150;
    private static final double ACCEL = 300;
    private static final double BRAKE = 560;
    private static final double NATURAL_FRICTION = 150;
    private static final double OFFROAD_MULT = 2.3;
    private static final double TURN_RATE = 3.0;
    private static final double GRIP = 7.5;
    private static final double BOOST_MULT = 1.55;
    private static final double RADIUS = 17;
    private static final double JUMP_MIN_SPEED = 150;

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
        if (landFlashTimer > 0) landFlashTimer -= dt;
        if (!alive) {
            respawnTimer -= dt;
            if (respawnTimer <= 0) respawn(track);
            return;
        }
        if (spinTimer > 0) {
            spinTimer -= dt;
            angle += 6 * dt;
            speed *= 0.97;
        } else if (airborne) {
            airTimer += dt;
            applyControls(dt, in, track);
            if (airTimer >= airDuration) land(track);
        } else {
            applyControls(dt, in, track);
            tryStartJump(track);
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

        boolean onTrack = airborne || track.distanceFromCenterline(x, y) < track.halfWidth;
        updateBoost();
        emitDust(dt, onTrack);
    }

    private void tryStartJump(Track track) {
        double distToCenter = track.distanceFromCenterline(x, y);
        double spd = Math.hypot(vx, vy);
        if (distToCenter > track.halfWidth + 10 || spd < JUMP_MIN_SPEED) return;
        int idx = track.nearestIndex(x, y);
        for (DuneJump dj : track.jumps) {
            if (dj.contains(idx)) {
                startJump(track, idx, spd);
                return;
            }
        }
    }

    private void startJump(Track track, int idx, double spd) {
        airborne = true;
        airTimer = 0;
        airDuration = 0.55 + Util.clamp(spd / MAX_SPEED, 0, 1) * 0.35;
        double trackAngle = track.tangentAngle(idx);
        double d1 = Math.abs(Util.normalizeAngle(angle - trackAngle));
        double d2 = Math.abs(Util.normalizeAngle(angle - trackAngle - Math.PI));
        launchAngleDiff = Math.min(d1, d2);
        for (int i = 0; i < 14; i++) {
            double a = Math.random() * Math.PI * 2;
            double sp = 40 + Math.random() * 90;
            particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp - 30,
                    0.4 + Math.random() * 0.3, 4 + Math.random() * 5, 6,
                    new Color(214, 178, 120, 200), Particle.ParticleKind.DUST));
        }
    }

    private void land(Track track) {
        airborne = false;
        boolean clean = launchAngleDiff < 0.30;
        landFlashTimer = 0.8;
        if (clean) {
            justLandedClean = true;
            speed = Math.min(MAX_SPEED * 1.05, speed * 1.2 + 40);
            for (int i = 0; i < 10; i++) {
                double a = Math.random() * Math.PI * 2;
                double sp = 60 + Math.random() * 100;
                particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp,
                        0.35 + Math.random() * 0.3, 4, 5,
                        new Color(255, 210, 120, 210), Particle.ParticleKind.SPARK));
            }
        } else {
            justLandedClean = false;
            double severity = Util.clamp((launchAngleDiff - 0.30) / 0.9, 0, 1);
            damage(6 + severity * 16);
            speed *= (1 - 0.5 * severity);
            spinOut(0.3 + severity * 0.9);
            for (int i = 0; i < 18; i++) {
                double a = Math.random() * Math.PI * 2;
                double sp = 50 + Math.random() * 140;
                particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp,
                        0.4 + Math.random() * 0.4, 5, 6,
                        new Color(180, 140, 90, 200), Particle.ParticleKind.DEBRIS));
            }
        }
    }

    private void applyControls(double dt, InputState in, Track track) {
        boolean onTrack = airborne || track.distanceFromCenterline(x, y) < track.halfWidth;
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
        Color c = onTrack ? new Color(214, 190, 140, 120) : new Color(180, 150, 96, 160);
        for (int i = 0; i < (onTrack ? 1 : 2); i++) {
            double jx = (Math.random() - 0.5) * 10;
            double jy = (Math.random() - 0.5) * 10;
            particles.add(new Particle(rearX + jx, rearY + jy,
                    -vx * 0.15 + (Math.random() - 0.5) * 20, -vy * 0.15 + (Math.random() - 0.5) * 20,
                    0.5 + Math.random() * 0.4, 6 + Math.random() * 6, 8, c, Particle.ParticleKind.DUST));
        }
        if (boosting) {
            particles.add(new Particle(rearX, rearY, -vx * 0.3, -vy * 0.3, 0.35, 10, 6,
                    new Color(255, 160, 60, 200), Particle.ParticleKind.SPARK));
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
        airborne = false;
        respawnTimer = 2.2;
        for (int i = 0; i < 26; i++) {
            double a = Math.random() * Math.PI * 2;
            double sp = 60 + Math.random() * 160;
            particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp,
                    0.5 + Math.random() * 0.6, 4 + Math.random() * 6, 10,
                    Math.random() < 0.5 ? new Color(255, 130, 40) : new Color(120, 96, 70), Particle.ParticleKind.SMOKE));
        }
    }

    private void respawn(Track track) {
        alive = true;
        health = 60;
        speed = 0; vx = 0; vy = 0;
        airborne = false;
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
        double t = airborne ? Util.clamp(airTimer / Math.max(0.001, airDuration), 0, 1) : 0;
        double heightFactor = airborne ? Math.sin(Math.PI * t) : 0;
        double scale = 1 + 0.28 * heightFactor;

        if (airborne) {
            double shadowScale = Math.max(0.35, 1 - 0.45 * heightFactor);
            double sw = radius() * 1.8 * shadowScale, sh = radius() * 1.15 * shadowScale;
            double sox = 5 * heightFactor, soy = 7 * heightFactor;
            g.setColor(new Color(50, 36, 20, (int) (120 * (1 - 0.25 * heightFactor))));
            g.fill(new Ellipse2D.Double(x - sw / 2 + sox, y - sh / 2 + soy, sw, sh));
        }

        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(angle);
        g.scale(scale, scale);

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

        if (landFlashTimer > 0) {
            g.setColor(new Color(255, 220, 100, (int) Util.clamp(landFlashTimer * 255, 0, 255)));
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString(justLandedClean ? "BOOST!" : "SPIN!", (int) x - 20, (int) y - 30);
        }

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
        track = new Track(W / 2.0, 400, 195, 76);
        int n = track.centerline.size();

        obstacles.clear();
        Random r = new Random(77);
        for (int i = 0; i < n; i += 13) {
            boolean nearJump = false;
            for (DuneJump dj : track.jumps) if (Math.abs(i - dj.midIndex) < 25) nearJump = true;
            if (nearJump) continue;
            if (r.nextDouble() < 0.45) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 18 + r.nextDouble() * 60;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.ROCK : Obstacle.Kind.CACTUS;
                obstacles.add(new Obstacle(k, p.x, p.y, 12 + r.nextInt(10)));
            }
        }
        int[] wreckIdx = { n / 6, n / 3, (2 * n) / 3, (5 * n) / 6 };
        for (int idx : wreckIdx) {
            double off = (r.nextDouble() - 0.5) * track.trackWidth * 0.5;
            Point2D.Double p = track.pointAtOffset(idx, off);
            obstacles.add(new Obstacle(Obstacle.Kind.WRECKED_JEEP, p.x, p.y, 15));
        }
        for (int i = 0; i < 4; i++) {
            Obstacle d = new Obstacle(Obstacle.Kind.TUMBLEWEED, r.nextInt(W), r.nextInt(H), 12);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 40 + r.nextDouble() * 40;
            d.vx = Math.cos(a) * sp;
            d.vy = Math.sin(a) * sp;
            obstacles.add(d);
        }
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-22);
        Point2D.Double p2pos = track.startPosition(22);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(200, 70, 40), new Color(250, 220, 160), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(60, 90, 130), new Color(220, 220, 210), vsAI ? "AI" : "2", particles);
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
            in2 = aiDriver.think(dt, obstacles);
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
        if (!a.alive || !b.alive || a.airborne || b.airborne) return;
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
        if (!c.alive || c.airborne) return;
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

    /** Heat-haze shimmer: subtle horizontal sine-wave row offsets over the sand/track band. */
    private void applyHeatHaze(BufferedImage img) {
        int top = 90, bottom = 700;
        for (int y = top; y < bottom; y += 2) {
            double t = (y - top) / (double) (bottom - top);
            int shift = (int) Math.round(Math.sin(y * 0.06 + timeAccum * 2.2) * 2.2 * (0.3 + t));
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
        double cyc = timeAccum * 0.05;
        Color skyTop = lerp(new Color(255, 140, 90), new Color(255, 90, 130), (Math.sin(cyc) + 1) / 2);
        Color skyMid = lerp(new Color(255, 190, 110), new Color(255, 150, 150), (Math.sin(cyc + 1.0) + 1) / 2);
        Color skyHorizon = lerp(new Color(255, 224, 160), new Color(255, 200, 180), (Math.sin(cyc + 2.0) + 1) / 2);

        g.setPaint(new GradientPaint(0, 0, skyTop, 0, (float) (H * 0.32), skyMid));
        g.fillRect(0, 0, W, (int) (H * 0.32));
        g.setPaint(new GradientPaint(0, (float) (H * 0.32), skyMid, 0, (float) (H * 0.42), skyHorizon));
        g.fillRect(0, (int) (H * 0.32), W, (int) (H * 0.1));

        double sunX = W * 0.5 + Math.sin(timeAccum * 0.03) * 260;
        double sunY = H * 0.30 + Math.sin(timeAccum * 0.02) * 18;
        int sunR = 46;
        g.setPaint(new RadialGradientPaint(new Point((int) sunX, (int) sunY), sunR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(255, 245, 210, 220), new Color(255, 170, 110, 100), new Color(255, 170, 110, 0)}));
        g.fillOval((int) (sunX - sunR * 3), (int) (sunY - sunR * 3), sunR * 6, sunR * 6);
        g.setColor(new Color(255, 250, 225));
        g.fillOval((int) (sunX - sunR / 2), (int) (sunY - sunR / 2), sunR, sunR);

        g.setColor(new Color(180, 100, 90, 140));
        Path2D.Double duneRidge = new Path2D.Double();
        duneRidge.moveTo(-20, H * 0.42);
        Random wr = new Random(11);
        double wx = -20;
        while (wx < W + 20) {
            double wy = H * 0.42 - 10 - 22 * Math.sin(wx * 0.006 + 1.4) - wr.nextInt(8);
            duneRidge.lineTo(wx, wy);
            wx += 20;
        }
        duneRidge.lineTo(W + 20, H * 0.42);
        duneRidge.closePath();
        g.fill(duneRidge);

        g.setPaint(new GradientPaint(0, (float) (H * 0.4), new Color(214, 168, 108), 0, H, new Color(158, 118, 72)));
        g.fillRect(0, (int) (H * 0.4), W, (int) (H * 0.6));
    }

    private Color lerp(Color a, Color b, double t) {
        t = Util.clamp(t, 0, 1);
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(255, 200, 110));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        centerText(g, "DUNE DRIFT RALLY", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Sunset desert racing — hit the dune ramps at speed to fly", H / 2 - 90);

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
        g.drawString(tag + "  LAP " + Math.min(c.lap + 1, TOTAL_LAPS) + (c.airborne ? "  AIRBORNE" : ""), x + 10, 32);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 10, 40, 170, 8);
        double hp = Util.clamp(c.health / 100.0, 0, 1);
        g.setColor(hp > 0.5 ? new Color(90, 200, 90) : hp > 0.25 ? Color.ORANGE : Color.RED);
        g.fillRect(x + 10, 40, (int) (170 * hp), 8);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 10, 54, 170, 8);
        g.setColor(new Color(255, 170, 60));
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
