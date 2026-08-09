import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class MadMaxStylePostApocalypticRacing4 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Salt Flat Drag — Mad Max Style Desert Racing #4");
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

    enum ParticleKind { DUST, SMOKE, SPARK, DEBRIS, STREAK }

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
        size = Math.max(1, size + growth * dt);
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
        Stroke oldStroke = g.getStroke();
        for (Particle p : particles) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.alpha()));
            g.setColor(p.color);
            if (p.kind == Particle.ParticleKind.STREAK) {
                g.setStroke(new BasicStroke((float) Math.max(1.2, p.size / 3.0)));
                double tailX = p.x - p.vx * 0.045;
                double tailY = p.y - p.vy * 0.045;
                g.draw(new Line2D.Double(p.x, p.y, tailX, tailY));
            } else {
                double s = p.size;
                g.fill(new Ellipse2D.Double(p.x - s / 2, p.y - s / 2, s, s));
            }
            g.setComposite(old);
        }
        g.setStroke(oldStroke);
    }
}

class Track {
    final double centerX, centerY, straightLength, radius, trackWidth, halfWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;

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
        // Elongated thin stadium: long straights, tight small-radius U-turns at each end.
        int pStraight = 100, pArc = 45;
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
        g.setColor(new Color(200, 204, 204));
        g.fill(road);

        g.setColor(new Color(150, 156, 156));
        g.setStroke(new BasicStroke(3));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(255, 120, 50, 150));
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
                g.setColor(new Color(160, 160, 156));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(120, 120, 116));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                break;
            case CACTUS:
                g.setColor(new Color(120, 130, 108));
                g.fillRoundRect((int) (-radius / 3), (int) -radius, (int) (radius * 2 / 3), (int) (radius * 2), 8, 8);
                g.fillRoundRect((int) -radius, (int) (-radius / 3), (int) radius, (int) (radius * 2 / 3), 8, 8);
                g.fillRoundRect((int) (radius / 3), (int) (-radius * 0.7), (int) (radius * 2 / 3), (int) radius, 8, 8);
                break;
            case WRECK:
                g.setColor(new Color(130, 75, 60));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                break;
            case TUMBLEWEED:
                g.setColor(new Color(195, 185, 155));
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

class NitrousPad {
    static final double COOLDOWN = 4.5;
    static final double HALF_ZONE = 24;

    final int index;
    final double arcPos;
    final Point2D.Double pos;
    final double glowPhase;
    boolean active = true;
    double cooldownRemaining = 0;

    NitrousPad(int index, double arcPos, Point2D.Double pos) {
        this.index = index;
        this.arcPos = arcPos;
        this.pos = pos;
        this.glowPhase = Math.random() * Math.PI * 2;
    }

    void update(double dt) {
        if (!active) {
            cooldownRemaining -= dt;
            if (cooldownRemaining <= 0) {
                active = true;
                cooldownRemaining = 0;
            }
        }
    }

    void trigger() {
        active = false;
        cooldownRemaining = COOLDOWN;
    }

    double rechargeFraction() {
        return active ? 1.0 : Util.clamp(1 - cooldownRemaining / COOLDOWN, 0, 1);
    }

    void reset() {
        active = true;
        cooldownRemaining = 0;
    }
}

class NitrousFlash {
    double x, y, life, maxLife;

    NitrousFlash(double x, double y, double life) {
        this.x = x; this.y = y; this.life = life; this.maxLife = life;
    }

    boolean update(double dt) {
        life -= dt;
        return life > 0;
    }

    float alpha() {
        return (float) Util.clamp(life / maxLife, 0, 1);
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
    double nitrousTimer = 0;

    final Color bodyColor;
    final Color trimColor;
    final String label;
    final ParticleSystem particles;

    private static final double MAX_SPEED = 460;
    private static final double MAX_REVERSE = -160;
    private static final double ACCEL = 320;
    private static final double BRAKE = 580;
    private static final double NATURAL_FRICTION = 150;
    private static final double OFFROAD_MULT = 2.4;
    private static final double TURN_RATE = 3.4;
    private static final double GRIP = 6.2;
    private static final double BOOST_MULT = 1.55;
    private static final double NITROUS_MULT = 2.15;
    private static final double NITROUS_ACCEL_MULT = 2.5;
    private static final double NITROUS_DURATION = 1.1;
    private static final double RADIUS = 17;

    private double dustTimer = 0;
    private double nitrousTrailTimer = 0;

    Car(double x, double y, double angle, Color bodyColor, Color trimColor, String label, ParticleSystem particles) {
        this.x = x; this.y = y; this.angle = angle;
        this.bodyColor = bodyColor; this.trimColor = trimColor;
        this.label = label;
        this.particles = particles;
    }

    double radius() { return RADIUS; }

    boolean nitrousActive() { return nitrousTimer > 0; }

    void triggerNitrous() { nitrousTimer = NITROUS_DURATION; }

    void update(double dt, InputState in, Track track) {
        if (!alive) {
            respawnTimer -= dt;
            if (respawnTimer <= 0) respawn(track);
            return;
        }
        if (nitrousTimer > 0) nitrousTimer = Math.max(0, nitrousTimer - dt);

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
        emitNitrousTrail(dt);
    }

    private void applyControls(double dt, InputState in, Track track) {
        boolean onTrack = track.distanceFromCenterline(x, y) < track.halfWidth;
        double frictionMult = onTrack ? 1.0 : OFFROAD_MULT;
        boosting = in.boost && boostFuel > 5 && in.throttle > 0;

        double speedMult = nitrousActive() ? NITROUS_MULT : (boosting ? BOOST_MULT : 1.0);
        double accelMult = nitrousActive() ? NITROUS_ACCEL_MULT : (boosting ? BOOST_MULT : 1.0);
        double topSpeed = MAX_SPEED * speedMult * (onTrack ? 1.0 : 0.55);

        if (in.throttle > 0.05) {
            speed += ACCEL * accelMult * dt;
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
        Color c = onTrack ? new Color(210, 210, 205, 110) : new Color(190, 185, 160, 150);
        for (int i = 0; i < (onTrack ? 1 : 2); i++) {
            double jx = (Math.random() - 0.5) * 10;
            double jy = (Math.random() - 0.5) * 10;
            particles.add(new Particle(rearX + jx, rearY + jy,
                    -vx * 0.15 + (Math.random() - 0.5) * 20, -vy * 0.15 + (Math.random() - 0.5) * 20,
                    0.5 + Math.random() * 0.4, 6 + Math.random() * 6, 8, c, Particle.ParticleKind.DUST));
        }
        if (boosting && !nitrousActive()) {
            particles.add(new Particle(rearX, rearY, -vx * 0.3, -vy * 0.3, 0.35, 10, 6,
                    new Color(255, 140, 40, 200), Particle.ParticleKind.SPARK));
        }
    }

    private void emitNitrousTrail(double dt) {
        if (!nitrousActive()) return;
        nitrousTrailTimer -= dt;
        if (nitrousTrailTimer > 0) return;
        nitrousTrailTimer = 0.02;
        double rearX = x - Math.cos(angle) * radius();
        double rearY = y - Math.sin(angle) * radius();
        particles.add(new Particle(rearX, rearY,
                -vx * 0.4 + (Math.random() - 0.5) * 40, -vy * 0.4 + (Math.random() - 0.5) * 40,
                0.22 + Math.random() * 0.1, 9 + Math.random() * 5, -4,
                Math.random() < 0.5 ? new Color(150, 255, 255, 230) : new Color(255, 255, 255, 220),
                Particle.ParticleKind.STREAK));
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
                    Math.random() < 0.5 ? new Color(255, 120, 30) : new Color(120, 120, 120), Particle.ParticleKind.SMOKE));
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
        nitrousTimer = 0;
    }

    void spinOut(double intensity) {
        spinTimer = Math.max(spinTimer, intensity);
    }

    void draw(Graphics2D g) {
        if (!alive) return;
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(angle);

        if (nitrousActive()) {
            g.setColor(new Color(150, 255, 255, 220));
            g.fillOval(-(int) radius() - 24, -8, 24, 16);
            g.setColor(new Color(255, 255, 255, 190));
            g.fillOval(-(int) radius() - 13, -4, 13, 8);
        } else if (boosting) {
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
        int lookahead = 10;
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

        in.steer = Util.clamp(diff * 2.0, -1, 1);
        in.throttle = Math.abs(diff) > 1.1 ? 0.25 : 1.0;

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
    private final List<NitrousPad> nitrousPads = new ArrayList<>();
    private final List<NitrousFlash> flashes = new ArrayList<>();
    private ParticleSystem particles = new ParticleSystem();
    private AIDriver aiDriver;

    private double countdownTimer;
    private double raceTime;
    private double sunPulse = 0;
    private final List<Line2D.Double> saltCracks = new ArrayList<>();
    private final BufferedImage sceneBuffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

    private static final double HORIZON_Y = H * 0.34;
    private static final int HAZE_TOP = (int) (HORIZON_Y - 15);
    private static final int HAZE_BAND_H = 70;
    private static final int MIRAGE_H = 55;

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        setupWorld();
        buildSaltCracks();
    }

    private void buildSaltCracks() {
        Random r = new Random(99);
        for (int i = 0; i < 160; i++) {
            double x = r.nextDouble() * W;
            double y = HORIZON_Y + r.nextDouble() * (H - HORIZON_Y);
            double len = 10 + r.nextDouble() * 30;
            double ang = r.nextDouble() * Math.PI * 2;
            saltCracks.add(new Line2D.Double(x, y, x + Math.cos(ang) * len, y + Math.sin(ang) * len));
        }
    }

    private void setupWorld() {
        track = new Track(W / 2.0, H / 2.0 + 20, 760, 80, 140);
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
        int[] hazardIdx = {35, 95, 160, 220};
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

        nitrousPads.clear();
        int[] padIdx = {40, 122, 190};
        for (int idx : padIdx) {
            int i = ((idx % n) + n) % n;
            Point2D.Double p = track.centerline.get(i);
            nitrousPads.add(new NitrousPad(i, track.cumulativeLength[i], p));
        }
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        flashes.clear();
        for (NitrousPad p : nitrousPads) p.reset();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-25);
        Point2D.Double p2pos = track.startPosition(25);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(216, 40, 40), new Color(255, 220, 60), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(40, 110, 220), new Color(230, 230, 230), vsAI ? "AI" : "2", particles);
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
        flashes.removeIf(f -> !f.update(dt));
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

        checkNitrousPads(p1);
        checkNitrousPads(p2);
        for (NitrousPad p : nitrousPads) p.update(dt);

        for (Car c : cars) {
            if (!c.finished && c.lap >= TOTAL_LAPS) {
                c.finished = true;
                c.finishTime = raceTime;
            }
        }
        if (p1.finished && p2.finished) state = State.FINISHED;
    }

    private void checkNitrousPads(Car c) {
        if (!c.alive) return;
        double half = track.totalLength / 2;
        for (NitrousPad pad : nitrousPads) {
            if (!pad.active) continue;
            double delta = pad.arcPos - c.lastArcLen;
            while (delta > half) delta -= track.totalLength;
            while (delta < -half) delta += track.totalLength;
            if (Math.abs(delta) < NitrousPad.HALF_ZONE && track.distanceFromCenterline(c.x, c.y) < track.halfWidth * 1.05) {
                c.triggerNitrous();
                pad.trigger();
                spawnNitrousBurst(pad.pos.x, pad.pos.y);
            }
        }
    }

    private void spawnNitrousBurst(double x, double y) {
        flashes.add(new NitrousFlash(x, y, 0.35));
        for (int i = 0; i < 22; i++) {
            double ang = Math.random() * Math.PI * 2;
            double sp = 80 + Math.random() * 220;
            particles.add(new Particle(x, y, Math.cos(ang) * sp, Math.sin(ang) * sp,
                    0.3 + Math.random() * 0.25, 4 + Math.random() * 5, -6,
                    Math.random() < 0.5 ? new Color(140, 255, 255) : new Color(255, 255, 255),
                    Particle.ParticleKind.STREAK));
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
        drawNitrousPads(sg);
        for (Obstacle o : obstacles) o.draw(sg);
        particles.draw(sg);
        for (Car c : cars) c.draw(sg);
        drawFlashes(sg);
        sg.dispose();

        applyHazeAndMirage(sceneBuffer);

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

    private void drawFlashes(Graphics2D g) {
        for (NitrousFlash f : flashes) {
            float a = f.alpha();
            float radius = (float) (28 + (1 - a) * 65);
            RadialGradientPaint rp = new RadialGradientPaint(
                    new Point2D.Double(f.x, f.y), radius,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 255, 255, (int) (215 * a)), new Color(200, 255, 255, 0)});
            g.setPaint(rp);
            g.fill(new Ellipse2D.Double(f.x - radius, f.y - radius, radius * 2, radius * 2));
        }
    }

    private void drawNitrousPads(Graphics2D g) {
        int n = track.centerline.size();
        int span = 4;
        for (NitrousPad pad : nitrousPads) {
            int i0 = ((pad.index - span) % n + n) % n;
            int i1 = ((pad.index + span) % n + n) % n;
            Point2D.Double a1 = track.pointAtOffset(i0, -track.halfWidth);
            Point2D.Double a2 = track.pointAtOffset(i0, track.halfWidth);
            Point2D.Double a3 = track.pointAtOffset(i1, track.halfWidth);
            Point2D.Double a4 = track.pointAtOffset(i1, -track.halfWidth);
            Path2D.Double quad = new Path2D.Double();
            quad.moveTo(a1.x, a1.y);
            quad.lineTo(a2.x, a2.y);
            quad.lineTo(a3.x, a3.y);
            quad.lineTo(a4.x, a4.y);
            quad.closePath();

            if (pad.active) {
                double pulse = 0.5 + 0.5 * Math.sin(sunPulse * 6 + pad.glowPhase);
                int alpha = (int) (90 + 90 * pulse);
                g.setColor(new Color(60, 255, 210, alpha));
                g.fill(quad);
                g.setColor(new Color(180, 255, 255, 220));
                g.setStroke(new BasicStroke(2.5f));
                g.draw(quad);
            } else {
                int alpha = (int) (25 + 60 * pad.rechargeFraction());
                g.setColor(new Color(110, 150, 150, alpha));
                g.fill(quad);
                g.setColor(new Color(140, 170, 170, alpha + 30));
                g.setStroke(new BasicStroke(1.5f));
                g.draw(quad);
            }
            g.setStroke(new BasicStroke(1));

            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(pad.active ? new Color(20, 70, 70) : new Color(90, 100, 100));
            FontMetrics fm = g.getFontMetrics();
            String label = "NOS";
            g.drawString(label, (int) (pad.pos.x - fm.stringWidth(label) / 2.0), (int) (pad.pos.y + 4));
        }
    }

    private void applyHazeAndMirage(BufferedImage img) {
        int mirageSrcTop = Math.max(0, HAZE_TOP - MIRAGE_H);
        int mirageDstTop = HAZE_TOP + HAZE_BAND_H;

        if (mirageDstTop + MIRAGE_H <= H && mirageSrcTop >= 0) {
            BufferedImage srcCopy = new BufferedImage(W, MIRAGE_H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg = srcCopy.createGraphics();
            cg.drawImage(img, 0, 0, W, MIRAGE_H, 0, mirageSrcTop, W, mirageSrcTop + MIRAGE_H, null);
            cg.dispose();

            Graphics2D dg = img.createGraphics();
            dg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.34f));
            // Vertically flipped draw: dy1 > dy2 flips the source vertically into the destination rect.
            dg.drawImage(srcCopy, 0, mirageDstTop + MIRAGE_H, W, mirageDstTop, 0, 0, W, MIRAGE_H, null);
            dg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
            dg.setColor(new Color(190, 225, 245));
            dg.fillRect(0, mirageDstTop, W, MIRAGE_H);
            dg.dispose();
        }

        shiftBand(img, HAZE_TOP, HAZE_BAND_H, 9.0, 6.0, 0.22, 0.0);
        shiftBand(img, mirageDstTop, MIRAGE_H, 5.0, 5.0, 0.3, 1.7);
    }

    private void shiftBand(BufferedImage img, int top, int bandH, double amplitude, double timeFreq, double rowFreq, double phase) {
        for (int y = Math.max(0, top); y < top + bandH && y < H; y++) {
            double t = (y - top) / (double) bandH;
            int shift = (int) (Math.sin(y * rowFreq + phase + sunPulse * timeFreq) * amplitude * (1 - t * 0.5));
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
        // Bright flat sky, no mountains — flat horizon.
        g.setPaint(new GradientPaint(0, 0, new Color(196, 226, 246), 0, (float) HORIZON_Y, new Color(248, 250, 248)));
        g.fillRect(0, 0, W, (int) HORIZON_Y);

        double pulse = 1 + 0.02 * Math.sin(sunPulse * 1.5);
        int sunR = (int) (46 * pulse);
        g.setPaint(new RadialGradientPaint(new Point(W - 200, 90), sunR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(255, 255, 245, 210), new Color(255, 250, 220, 90), new Color(255, 250, 220, 0)}));
        g.fillOval(W - 200 - sunR * 3, 90 - sunR * 3, sunR * 6, sunR * 6);
        g.setColor(new Color(255, 255, 250));
        g.fillOval(W - 200 - sunR / 2, 90 - sunR / 2, sunR, sunR);

        // Blinding white / pale blue-grey salt flat ground, flat to the horizon.
        g.setPaint(new GradientPaint(0, (float) HORIZON_Y, new Color(240, 244, 244), 0, H, new Color(214, 222, 224)));
        g.fillRect(0, (int) HORIZON_Y, W, (int) (H - HORIZON_Y));

        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(190, 198, 200, 90));
        for (Line2D.Double crack : saltCracks) g.draw(crack);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(60, 230, 255));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        centerText(g, "SALT FLAT DRAG", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Mad Max style drag racing across a blinding white salt pan", H / 2 - 90);

        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        centerText(g, "[1] Player vs Player      [2] Player vs Computer", H / 2 - 20);
        centerText(g, "P1: W A S D  +  SPACE boost", H / 2 + 20);
        centerText(g, "P2: Arrow Keys + ENTER boost  (disabled vs Computer)", H / 2 + 46);
        g.setColor(new Color(140, 255, 255));
        centerText(g, "Drive over glowing NOS pads for a short nitrous burst!", H / 2 + 76);
        g.setColor(Color.WHITE);
        centerText(g, "Press 1 or 2 to start", H / 2 + 110);
    }

    private void centerText(Graphics2D g, String s, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (W - fm.stringWidth(s)) / 2, y);
    }

    private void drawCountdown(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 90));
        g.setColor(new Color(60, 220, 255));
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
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(x, 16, 190, 60, 10, 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString(tag + "  LAP " + Math.min(c.lap + 1, TOTAL_LAPS), x + 10, 32);
        if (c.nitrousActive()) {
            g.setColor(new Color(140, 255, 255));
            g.drawString("NOS!", x + 140, 32);
        }

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 10, 40, 170, 8);
        double hp = Util.clamp(c.health / 100.0, 0, 1);
        g.setColor(hp > 0.5 ? new Color(90, 200, 90) : hp > 0.25 ? Color.ORANGE : Color.RED);
        g.fillRect(x + 10, 40, (int) (170 * hp), 8);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 10, 54, 170, 8);
        g.setColor(new Color(80, 200, 255));
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
