import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class MadMaxStylePostApocalypticRacing8 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Junkyard Circuit — Post-Apocalyptic Scrapyard Racing");
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

    static double smoothstep(double t) {
        t = clamp(t, 0, 1);
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

class Track {
    final double centerX, centerY, straightLength, radius, trackWidth, halfWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double[] halfWidths;
    double totalLength;

    // squeeze "corridor" section along the bottom straight
    private static final int SQUEEZE_CENTER = 145;
    private static final double SQUEEZE_SPAN = 22;
    private static final double SQUEEZE_STRENGTH = 0.58;

    Track(double centerX, double centerY, double straightLength, double radius, double trackWidth) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.straightLength = straightLength;
        this.radius = radius;
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        build();
    }

    private void build() {
        int pStraight = 50, pArc = 70;
        double half = straightLength / 2;

        for (int i = 0; i < pStraight; i++) {
            double t = i / (double) pStraight;
            centerline.add(new Point2D.Double(centerX - half + t * straightLength, centerY - radius));
        }
        for (int i = 0; i < pArc; i++) {
            double t = i / (double) pArc;
            double ang = -Math.PI / 2 + t * Math.PI;
            centerline.add(new Point2D.Double(centerX + half + radius * Math.cos(ang), centerY + radius * Math.sin(ang)));
        }
        for (int i = 0; i < pStraight; i++) {
            double t = i / (double) pStraight;
            centerline.add(new Point2D.Double(centerX + half - t * straightLength, centerY + radius));
        }
        for (int i = 0; i < pArc; i++) {
            double t = i / (double) pArc;
            double ang = Math.PI / 2 + t * Math.PI;
            centerline.add(new Point2D.Double(centerX - half + radius * Math.cos(ang), centerY + radius * Math.sin(ang)));
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

        halfWidths = new double[n];
        for (int i = 0; i < n; i++) {
            halfWidths[i] = halfWidth * (1 - squeezeAmount(i) * SQUEEZE_STRENGTH);
        }
    }

    private double squeezeAmount(int i) {
        double dist = Math.abs(i - SQUEEZE_CENTER);
        if (dist >= SQUEEZE_SPAN) return 0;
        double t = 1 - dist / SQUEEZE_SPAN;
        return Util.smoothstep(t);
    }

    boolean inSqueezeZone(int i) {
        return squeezeAmount(i) > 0.05;
    }

    double halfWidthAt(int index) {
        return halfWidths[index];
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

    boolean isOnTrack(double x, double y) {
        int i = nearestIndex(x, y);
        return centerline.get(i).distance(x, y) < halfWidths[i];
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
            Point2D.Double o = pointAtOffset(i, halfWidths[i]);
            Point2D.Double in = pointAtOffset(i, -halfWidths[i]);
            if (i == 0) { outer.moveTo(o.x, o.y); inner.moveTo(in.x, in.y); }
            else { outer.lineTo(o.x, o.y); inner.lineTo(in.x, in.y); }
        }
        outer.closePath();
        inner.closePath();

        Area road = new Area(outer);
        road.subtract(new Area(inner));
        g.setColor(new Color(80, 76, 72));
        g.fill(road);

        g.setColor(new Color(108, 100, 90));
        g.setStroke(new BasicStroke(3));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(200, 150, 90, 120));
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
        Point2D.Double o = pointAtOffset(0, halfWidths[0]);
        Point2D.Double in = pointAtOffset(0, -halfWidths[0]);
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
    enum Kind { SCRAP, SPIKE, WRECK, TIRE }

    double x, y, radius, rotation;
    Kind kind;
    double vx, vy, spin;
    double[] jag;

    Obstacle(Kind kind, double x, double y, double radius) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.rotation = Math.random() * Math.PI * 2;
        if (kind == Kind.TIRE) this.spin = 4 + Math.random() * 3;
        if (kind == Kind.SCRAP || kind == Kind.SPIKE) {
            jag = new double[10];
            for (int i = 0; i < jag.length; i++) jag[i] = 0.65 + Math.random() * 0.75;
        }
    }

    void update(double dt, double minX, double maxX, double minY, double maxY) {
        if (kind == Kind.TIRE) {
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
            case SCRAP: {
                Polygon poly = new Polygon();
                for (int i = 0; i < jag.length; i++) {
                    double a = (Math.PI * 2 * i) / jag.length;
                    double r = radius * jag[i];
                    poly.addPoint((int) (Math.cos(a) * r), (int) (Math.sin(a) * r * 0.85));
                }
                g.setColor(new Color(95, 88, 80));
                g.fillPolygon(poly);
                g.setColor(new Color(150, 90, 60));
                g.fillRect((int) (-radius * 0.35), (int) (-radius * 0.7), (int) (radius * 0.5), (int) (radius * 0.5));
                g.setColor(new Color(60, 60, 62));
                g.fillRect((int) (-radius * 0.1), (int) (radius * 0.1), (int) (radius * 0.7), (int) (radius * 0.35));
                g.setColor(new Color(40, 38, 36));
                g.drawPolygon(poly);
                break;
            }
            case SPIKE: {
                g.setColor(new Color(70, 68, 66));
                int spikes = jag.length;
                Polygon poly = new Polygon();
                for (int i = 0; i < spikes; i++) {
                    double a = (Math.PI * 2 * i) / spikes;
                    double r = radius * (i % 2 == 0 ? jag[i] * 1.1 : jag[i] * 0.5);
                    poly.addPoint((int) (Math.cos(a) * r), (int) (Math.sin(a) * r));
                }
                g.fillPolygon(poly);
                g.setColor(new Color(130, 60, 50));
                g.drawPolygon(poly);
                break;
            }
            case WRECK:
                g.setColor(new Color(90, 55, 40));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(new Color(60, 58, 55));
                g.fillRect((int) -radius, (int) (-radius / 2 - 4), (int) (radius * 2), 5);
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                break;
            case TIRE:
                g.setColor(new Color(30, 30, 30));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                g.setColor(new Color(70, 70, 70));
                g.fillOval((int) (-radius * 0.45), (int) (-radius * 0.45), (int) (radius * 0.9), (int) (radius * 0.9));
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3;
                    g.drawLine(0, 0, (int) (Math.cos(a) * radius * 0.4), (int) (Math.sin(a) * radius * 0.4));
                }
                break;
        }
        g.setTransform(old);
    }
}

class Crusher {
    enum Phase { UP, WARNING, SLAM, DOWN, RISING }

    final double x, y;
    final double dangerRadius = 40;
    Phase phase = Phase.UP;
    double timer;
    double headOffset = 0;
    boolean[] hitFlag = new boolean[0];
    private final ParticleSystem particles;

    Crusher(double x, double y, double startDelay, ParticleSystem particles) {
        this.x = x;
        this.y = y;
        this.timer = startDelay;
        this.particles = particles;
    }

    boolean isDangerNow() {
        return phase == Phase.WARNING || phase == Phase.SLAM || phase == Phase.DOWN;
    }

    void update(double dt, List<Car> cars) {
        if (hitFlag.length != cars.size()) hitFlag = new boolean[cars.size()];
        timer -= dt;
        switch (phase) {
            case UP:
                headOffset = 0;
                if (timer <= 0) {
                    phase = Phase.WARNING;
                    timer = 0.6;
                    Arrays.fill(hitFlag, false);
                }
                break;
            case WARNING:
                headOffset = 0;
                if (timer <= 0) {
                    phase = Phase.SLAM;
                    timer = 0.14;
                }
                break;
            case SLAM:
                headOffset = 1 - Util.clamp(timer / 0.14, 0, 1);
                if (timer <= 0) {
                    phase = Phase.DOWN;
                    timer = 0.45;
                    headOffset = 1;
                    spawnSlamDust();
                }
                break;
            case DOWN:
                headOffset = 1;
                if (timer <= 0) {
                    phase = Phase.RISING;
                    timer = 0.4;
                }
                break;
            case RISING:
                headOffset = Util.clamp(timer / 0.4, 0, 1);
                if (timer <= 0) {
                    phase = Phase.UP;
                    timer = 1.6 + Math.random() * 1.8;
                }
                break;
        }

        if (phase == Phase.SLAM && headOffset > 0.55 || phase == Phase.DOWN) {
            for (int i = 0; i < cars.size(); i++) {
                Car c = cars.get(i);
                if (!c.alive || hitFlag[i]) continue;
                double d = Math.hypot(c.x - x, c.y - y);
                if (d < dangerRadius) {
                    hitFlag[i] = true;
                    c.damage(65);
                    double nx = (c.x - x) / Math.max(d, 0.001);
                    double ny = (c.y - y) / Math.max(d, 0.001);
                    if (d < 0.5) { nx = Math.cos(c.angle + Math.PI / 2); ny = Math.sin(c.angle + Math.PI / 2); }
                    c.vx = nx * 480;
                    c.vy = ny * 480;
                    c.speed = 0;
                    c.spinOut(0.9);
                    spawnHitDebris(c.x, c.y);
                }
            }
        }
    }

    private void spawnSlamDust() {
        for (int i = 0; i < 20; i++) {
            double a = Math.random() * Math.PI * 2;
            double sp = 40 + Math.random() * 120;
            particles.add(new Particle(x, y + 6, Math.cos(a) * sp, Math.sin(a) * sp * 0.4 - 20,
                    0.4 + Math.random() * 0.3, 5 + Math.random() * 6, 6,
                    new Color(150, 130, 110, 200), Particle.ParticleKind.DUST));
        }
    }

    private void spawnHitDebris(double cx, double cy) {
        for (int i = 0; i < 14; i++) {
            double a = Math.random() * Math.PI * 2;
            double sp = 80 + Math.random() * 160;
            particles.add(new Particle(cx, cy, Math.cos(a) * sp, Math.sin(a) * sp,
                    0.4 + Math.random() * 0.4, 4 + Math.random() * 5, 8,
                    new Color(255, 160, 40), Particle.ParticleKind.SPARK));
        }
    }

    void draw(Graphics2D g) {
        // base anvil / crush pad
        g.setColor(new Color(55, 52, 50));
        g.fillRect((int) (x - dangerRadius), (int) (y - 10), (int) (dangerRadius * 2), 20);
        g.setColor(new Color(90, 30, 25));
        g.fillRect((int) (x - dangerRadius + 4), (int) (y - 6), (int) (dangerRadius * 2 - 8), 4);

        // warning telegraph glow
        if (phase == Phase.WARNING) {
            double pulse = 0.5 + 0.5 * Math.sin(timer * 40);
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.35 + 0.4 * pulse)));
            g.setColor(new Color(255, 210, 40));
            g.fillOval((int) (x - dangerRadius), (int) (y - dangerRadius), (int) (dangerRadius * 2), (int) (dangerRadius * 2));
            g.setComposite(old);
        }
        if (phase == Phase.SLAM || phase == Phase.DOWN) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g.setColor(new Color(255, 90, 40));
            g.fillOval((int) (x - dangerRadius), (int) (y - dangerRadius), (int) (dangerRadius * 2), (int) (dangerRadius * 2));
            g.setComposite(old);
        }

        // mast rails
        double railTop = y - 130;
        g.setColor(new Color(70, 65, 60));
        g.fillRect((int) (x - dangerRadius - 6), (int) railTop, 8, 140);
        g.fillRect((int) (x + dangerRadius - 2), (int) railTop, 8, 140);

        // the press head sliding down according to headOffset (0 = up, 1 = down)
        double headY = railTop + 10 + headOffset * (y - 22 - (railTop + 10));
        g.setColor(new Color(120, 22, 18));
        g.fillRect((int) (x - dangerRadius - 4), (int) headY, (int) (dangerRadius * 2 + 8), 26);
        g.setColor(new Color(40, 38, 36));
        g.fillRect((int) (x - dangerRadius - 4), (int) headY + 20, (int) (dangerRadius * 2 + 8), 8);
        g.setColor(new Color(200, 190, 40));
        for (int i = 0; i < 4; i++) {
            g.fillRect((int) (x - dangerRadius + 4 + i * (dangerRadius * 2 - 8) / 4.0), (int) headY + 2, 6, 6);
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

        boolean onTrack = track.isOnTrack(x, y);
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
        boolean onTrack = track.isOnTrack(x, y);
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
        Color c = onTrack ? new Color(150, 140, 130, 120) : new Color(120, 100, 80, 160);
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
        // spiked scrap bumper for junkyard flavor
        g.setColor(new Color(70, 68, 66));
        for (int i = -1; i <= 1; i++) {
            int[] xs = {18, 24, 18};
            int[] ys = {-4 + i * 6, i * 6, 4 + i * 6};
            g.fillPolygon(xs, ys, 3);
        }
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

    InputState think(double dt, List<Obstacle> obstacles, List<Crusher> crushers) {
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

        for (Crusher c : crushers) {
            if (!c.isDangerNow()) continue;
            double dx = c.x - car.x, dy = c.y - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < 110) {
                double angToC = Util.normalizeAngle(Math.atan2(dy, dx) - car.angle);
                if (Math.abs(angToC) < 1.1) {
                    diff += angToC < 0 ? 0.75 : -0.75;
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
    private final List<Crusher> crushers = new ArrayList<>();
    private ParticleSystem particles = new ParticleSystem();
    private AIDriver aiDriver;

    private double countdownTimer;
    private double raceTime;
    private double sunPulse = 0;
    private double craneTime = 0;
    private final List<Point2D.Double> mountains = new ArrayList<>();
    private final BufferedImage sceneBuffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        setupWorld();
        buildMountains();
    }

    private void buildMountains() {
        Random r = new Random(7);
        double x = -50;
        while (x < W + 50) {
            mountains.add(new Point2D.Double(x, 90 + r.nextInt(70)));
            x += 40 + r.nextInt(40);
        }
    }

    private void setupWorld() {
        track = new Track(W / 2.0, H / 2.0 + 20, 420, 200, 170);
        obstacles.clear();
        crushers.clear();
        particles = new ParticleSystem();
        Random r = new Random(42);
        int n = track.centerline.size();
        for (int i = 0; i < n; i += 9) {
            double localHalf = track.halfWidthAt(i);
            boolean squeeze = track.inSqueezeZone(i);
            double baseChance = squeeze ? 0.85 : 0.55;
            if (r.nextDouble() < baseChance) {
                double side = r.nextBoolean() ? 1 : -1;
                double gap = squeeze ? 6 + r.nextDouble() * 18 : 20 + r.nextDouble() * 60;
                double off = localHalf + gap;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.SCRAP : Obstacle.Kind.SPIKE;
                double size = squeeze ? 14 + r.nextInt(14) : 12 + r.nextInt(10);
                obstacles.add(new Obstacle(k, p.x, p.y, size));
                if (squeeze && r.nextDouble() < 0.5) {
                    // extra pile stacked closer in to sell the tight-corridor read
                    Point2D.Double p2 = track.pointAtOffset(i, side * (localHalf + gap * 0.45));
                    obstacles.add(new Obstacle(Obstacle.Kind.SCRAP, p2.x, p2.y, 10 + r.nextInt(8)));
                }
            }
        }
        int[] hazardIdx = {30, 90, 210};
        for (int idx : hazardIdx) {
            if (idx >= n) continue;
            double off = (r.nextDouble() - 0.5) * track.halfWidthAt(idx) * 1.2;
            Point2D.Double p = track.pointAtOffset(idx, off);
            obstacles.add(new Obstacle(Obstacle.Kind.WRECK, p.x, p.y, 16));
        }
        for (int i = 0; i < 3; i++) {
            Obstacle tw = new Obstacle(Obstacle.Kind.TIRE, r.nextInt(W), r.nextInt(H), 14);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 40 + r.nextDouble() * 40;
            tw.vx = Math.cos(a) * sp;
            tw.vy = Math.sin(a) * sp;
            obstacles.add(tw);
        }

        // car crushers: fixed heavy press blocks just off the racing line
        addCrusher(25, 0.4, 0.0);
        addCrusher(200, -0.35, 1.4);
        addCrusher(158, 0.18, 2.7);
    }

    private void addCrusher(int idx, double sideFrac, double startDelay) {
        double localHalf = track.halfWidthAt(idx);
        Point2D.Double p = track.pointAtOffset(idx, sideFrac * localHalf);
        crushers.add(new Crusher(p.x, p.y, startDelay, particles));
    }

    private void setupRace() {
        cars.clear();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-25);
        Point2D.Double p2pos = track.startPosition(25);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(150, 60, 35), new Color(210, 170, 60), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(70, 95, 90), new Color(190, 190, 170), vsAI ? "AI" : "2", particles);
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
        craneTime += dt;
        for (Obstacle o : obstacles) o.update(dt, -30, W + 30, -30, H + 30);
        for (Crusher c : crushers) c.update(dt, cars);

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
            in2 = aiDriver.think(dt, obstacles, crushers);
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
            if (o.kind == Obstacle.Kind.TIRE) {
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
        for (Crusher c : crushers) c.draw(sg);
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
        g.setPaint(new GradientPaint(0, 0, new Color(150, 130, 110), 0, (float) (H * 0.45), new Color(190, 170, 150)));
        g.fillRect(0, 0, W, (int) (H * 0.45));

        double pulse = 1 + 0.03 * Math.sin(sunPulse * 1.5);
        int sunR = (int) (55 * pulse);
        g.setPaint(new RadialGradientPaint(new Point(W - 220, 110), sunR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(255, 230, 170, 200), new Color(210, 160, 100, 80), new Color(210, 160, 100, 0)}));
        g.fillOval(W - 220 - sunR * 3, 110 - sunR * 3, sunR * 6, sunR * 6);
        g.setColor(new Color(235, 220, 190));
        g.fillOval(W - 220 - sunR / 2, 110 - sunR / 2, sunR, sunR);

        g.setColor(new Color(120, 105, 95));
        Path2D.Double range = new Path2D.Double();
        range.moveTo(-50, H * 0.42);
        for (Point2D.Double m : mountains) range.lineTo(m.x, H * 0.42 - m.y * 0.5);
        range.lineTo(W + 50, H * 0.42);
        range.closePath();
        g.fill(range);

        drawMagnetCrane(g);

        g.setPaint(new GradientPaint(0, (float) (H * 0.4), new Color(120, 112, 100), 0, H, new Color(90, 84, 78)));
        g.fillRect(0, (int) (H * 0.4), W, (int) (H * 0.6));
    }

    // Pure decoration: a magnet crane silhouette that swings its arm back and forth. No gameplay effect.
    private void drawMagnetCrane(Graphics2D g) {
        double pivotX = 190, pivotY = H * 0.42 - 150;
        double mastHeight = 150;
        double boomLength = 130;
        double armAngle = Math.toRadians(28) * Math.sin(craneTime * 0.6);

        Color silhouette = new Color(55, 48, 44);
        g.setColor(silhouette);
        // mast
        g.fillRect((int) (pivotX - 8), (int) pivotY, 16, (int) mastHeight);
        // base
        g.fillRect((int) (pivotX - 30), (int) (pivotY + mastHeight - 6), 60, 10);
        // horizontal boom
        g.fillRect((int) pivotX, (int) (pivotY - 8), (int) boomLength, 12);

        double armPivotX = pivotX + boomLength - 10;
        double armPivotY = pivotY - 2;
        double armLen = 90;
        double armEndX = armPivotX + Math.sin(armAngle) * armLen;
        double armEndY = armPivotY + Math.cos(armAngle) * armLen;

        g.setStroke(new BasicStroke(4));
        g.drawLine((int) armPivotX, (int) armPivotY, (int) armEndX, (int) armEndY);
        g.setStroke(new BasicStroke(1));

        // magnet disc
        g.setColor(new Color(70, 62, 56));
        g.fillOval((int) (armEndX - 16), (int) (armEndY - 8), 32, 16);
        g.setColor(new Color(100, 40, 30));
        g.fillOval((int) (armEndX - 10), (int) (armEndY - 4), 20, 8);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(200, 90, 40));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        centerText(g, "JUNKYARD CIRCUIT", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Mad Max style scrapyard racing — watch out for the crushers", H / 2 - 90);

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
        g.setColor(new Color(255, 200, 60));
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
        g.setColor(new Color(200, 140, 40));
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
