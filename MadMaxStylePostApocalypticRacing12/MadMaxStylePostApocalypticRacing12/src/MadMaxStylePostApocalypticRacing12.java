import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class MadMaxStylePostApocalypticRacing12 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Flooded Ruins — Post-Apocalyptic Desert Racing");
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

    enum ParticleKind { DUST, SMOKE, SPARK, DEBRIS, SPLASH, RIPPLE }

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

    int size() { return particles.size(); }

    synchronized void update(double dt) {
        particles.removeIf(p -> !p.update(dt));
    }

    synchronized void draw(Graphics2D g) {
        Stroke oldStroke = g.getStroke();
        for (Particle p : particles) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.alpha()));
            g.setColor(p.color);
            double s = p.size;
            if (p.kind == Particle.ParticleKind.RIPPLE) {
                g.setStroke(new BasicStroke(1.6f));
                g.draw(new Ellipse2D.Double(p.x - s / 2, p.y - s / 2, s, s));
            } else {
                g.fill(new Ellipse2D.Double(p.x - s / 2, p.y - s / 2, s, s));
            }
            g.setComposite(old);
        }
        g.setStroke(oldStroke);
    }
}

/** Ambient rain that falls continuously across the whole screen, independent of game state. */
class Rain {
    private final int n;
    private final double[] x, y, len, speed;
    private final Random r = new Random(11);
    private final int w, h;

    Rain(int w, int h, int n) {
        this.w = w; this.h = h; this.n = n;
        x = new double[n]; y = new double[n]; len = new double[n]; speed = new double[n];
        for (int i = 0; i < n; i++) reset(i, true);
    }

    private void reset(int i, boolean initial) {
        x[i] = r.nextDouble() * (w + 80) - 40;
        y[i] = initial ? r.nextDouble() * h : -20;
        len[i] = 9 + r.nextDouble() * 12;
        speed[i] = 520 + r.nextDouble() * 420;
    }

    void update(double dt) {
        for (int i = 0; i < n; i++) {
            y[i] += speed[i] * dt;
            x[i] -= 40 * dt;
            if (y[i] - len[i] > h) reset(i, false);
        }
    }

    void draw(Graphics2D g) {
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(new Color(200, 220, 225, 110));
        for (int i = 0; i < n; i++) {
            g.drawLine((int) x[i], (int) y[i], (int) (x[i] - 5), (int) (y[i] - len[i]));
        }
    }
}

class Track {
    final double centerX, centerY, straightLength, radius, trackWidth, halfWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    final List<int[]> floodZones = new ArrayList<>();
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

        // Two flooded stretches: one on the top straight, one on the bottom straight.
        floodZones.add(new int[]{14, 36});
        floodZones.add(new int[]{134, 156});
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

    boolean isFloodedIndex(int idx) {
        for (int[] z : floodZones) {
            if (idx >= z[0] && idx <= z[1]) return true;
        }
        return false;
    }

    boolean isFlooded(double x, double y) {
        return isFloodedIndex(nearestIndex(x, y));
    }

    Point2D.Double pointAtOffset(int index, double offset) {
        Point2D.Double p = centerline.get(index);
        Point2D.Double n = normals.get(index);
        return new Point2D.Double(p.x + n.x * offset, p.y + n.y * offset);
    }

    Point2D.Double randomPointInZone(int zoneIdx, Random r) {
        int[] z = floodZones.get(zoneIdx);
        int idx = z[0] + r.nextInt(Math.max(1, z[1] - z[0] + 1));
        double off = (r.nextDouble() - 0.5) * trackWidth * 0.85;
        return pointAtOffset(idx, off);
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
        g.setColor(new Color(72, 90, 92));
        g.fill(road);

        g.setColor(new Color(48, 62, 64));
        g.setStroke(new BasicStroke(3));
        g.draw(outer);
        g.draw(inner);

        // Flooded stretches: darker rippling water overlay.
        for (int[] z : floodZones) {
            Path2D.Double zone = new Path2D.Double();
            boolean first = true;
            for (int i = z[0]; i <= z[1]; i++) {
                Point2D.Double o = pointAtOffset(i, halfWidth);
                if (first) { zone.moveTo(o.x, o.y); first = false; } else zone.lineTo(o.x, o.y);
            }
            for (int i = z[1]; i >= z[0]; i--) {
                Point2D.Double in = pointAtOffset(i, -halfWidth);
                zone.lineTo(in.x, in.y);
            }
            zone.closePath();
            g.setColor(new Color(28, 55, 62, 210));
            g.fill(zone);
            g.setColor(new Color(60, 110, 120, 140));
            g.setStroke(new BasicStroke(2));
            g.draw(zone);
        }

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(170, 205, 205, 140));
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
    enum Kind { ROCK, PILLAR, WRECK, DEBRIS }

    double x, y, radius, rotation;
    Kind kind;
    double vx, vy, spin;

    Obstacle(Kind kind, double x, double y, double radius) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.rotation = Math.random() * Math.PI * 2;
        if (kind == Kind.DEBRIS) this.spin = 3 + Math.random() * 2.5;
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
                g.setColor(new Color(74, 78, 76));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(50, 54, 52));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                break;
            case PILLAR: {
                double w = radius * 0.8, h = radius * 3.2;
                // drop shadow / submerged base
                g.setColor(new Color(20, 35, 38, 140));
                g.fillOval((int) (-w * 0.9), (int) (h * 0.28), (int) (w * 1.8), (int) (w * 0.9));
                g.setColor(new Color(96, 100, 98));
                g.fillRect((int) -w / 2, (int) -h / 2, (int) w, (int) h);
                g.setColor(new Color(60, 66, 64));
                g.drawRect((int) -w / 2, (int) -h / 2, (int) w, (int) h);
                g.setColor(new Color(120, 90, 60, 200));
                g.fillRect((int) (-w / 2 + 1), (int) (-h / 2 + h * 0.35), (int) (w - 2), 2);
                g.fillRect((int) (-w / 2 + 1), (int) (-h / 2 + h * 0.6), (int) (w - 2), 2);
                g.setColor(new Color(140, 150, 148));
                g.fillRect((int) -w / 2, (int) -h / 2, (int) w, 4);
                break;
            }
            case WRECK:
                g.setColor(new Color(52, 46, 44));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                break;
            case DEBRIS:
                g.setColor(new Color(90, 82, 60));
                g.fillRoundRect((int) -radius, (int) (-radius / 4), (int) (radius * 2), (int) (radius / 2), 4, 4);
                g.setColor(new Color(70, 64, 48));
                for (int i = 0; i < 3; i++) {
                    double a = i * Math.PI / 3;
                    g.drawLine(0, 0, (int) (Math.cos(a) * radius), (int) (Math.sin(a) * radius));
                }
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
    boolean inFlood = false;
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
    private static final double FLOOD_MULT = 3.4;
    private static final double TURN_RATE = 3.0;
    private static final double GRIP = 7.5;
    private static final double FLOOD_GRIP_MULT = 0.55;
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
        if (inFlood) gripNow *= FLOOD_GRIP_MULT;
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
        lap = Math.max(0, (int) Math.floor(unwrappedDistance / track.totalLength));

        updateBoost();
        emitEffects(dt);
    }

    private void applyControls(double dt, InputState in, Track track) {
        boolean onTrack = track.distanceFromCenterline(x, y) < track.halfWidth;
        inFlood = onTrack && track.isFlooded(x, y);
        double frictionMult = inFlood ? FLOOD_MULT : (onTrack ? 1.0 : OFFROAD_MULT);
        boosting = in.boost && boostFuel > 5 && in.throttle > 0;
        double surfaceSpeedMult = inFlood ? 0.8 : (onTrack ? 1.0 : 0.55);
        double topSpeed = MAX_SPEED * (boosting ? BOOST_MULT : 1.0) * surfaceSpeedMult;

        if (in.throttle > 0.05) {
            speed += ACCEL * (boosting ? BOOST_MULT : 1.0) * dt;
        } else if (in.throttle < -0.05) {
            if (speed > 10) speed -= BRAKE * dt;
            else speed -= ACCEL * 0.6 * dt;
        } else {
            if (speed > 0) speed = Math.max(0, speed - NATURAL_FRICTION * frictionMult * dt);
            else if (speed < 0) speed = Math.min(0, speed + NATURAL_FRICTION * frictionMult * dt);
        }
        if (frictionMult > 1.0 && speed > 0) speed = Math.max(0, speed - NATURAL_FRICTION * (frictionMult - 1) * dt);

        speed = Util.clamp(speed, MAX_REVERSE, topSpeed);

        double speedFactor = Util.clamp(Math.abs(speed) / MAX_SPEED, 0.35, 1.0);
        double dir = speed < 0 ? -1 : 1;
        angle += in.steer * TURN_RATE * dt * speedFactor * dir;
    }

    private void updateBoost() {
        if (boosting) boostFuel = Math.max(0, boostFuel - 0.7);
        else boostFuel = Math.min(100, boostFuel + 0.23);
    }

    private void emitEffects(double dt) {
        dustTimer -= dt;
        double spd = Math.hypot(vx, vy);
        if (spd < 30 || dustTimer > 0) return;
        dustTimer = inFlood ? 0.045 : 0.06;
        double rearX = x - Math.cos(angle) * radius();
        double rearY = y - Math.sin(angle) * radius();
        if (inFlood) {
            for (int i = 0; i < 2; i++) {
                double jx = (Math.random() - 0.5) * 12;
                double jy = (Math.random() - 0.5) * 12;
                Color c = Math.random() < 0.5 ? new Color(210, 235, 240, 190) : new Color(120, 170, 180, 170);
                particles.add(new Particle(rearX + jx, rearY + jy,
                        -vx * 0.2 + (Math.random() - 0.5) * 40, -vy * 0.2 + (Math.random() - 0.5) * 40,
                        0.4 + Math.random() * 0.35, 5 + Math.random() * 6, 10, c, Particle.ParticleKind.SPLASH));
            }
        } else {
            Color c = new Color(150, 150, 140, 120);
            for (int i = 0; i < 1; i++) {
                double jx = (Math.random() - 0.5) * 10;
                double jy = (Math.random() - 0.5) * 10;
                particles.add(new Particle(rearX + jx, rearY + jy,
                        -vx * 0.15 + (Math.random() - 0.5) * 20, -vy * 0.15 + (Math.random() - 0.5) * 20,
                        0.5 + Math.random() * 0.4, 6 + Math.random() * 6, 8, c, Particle.ParticleKind.DUST));
            }
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
    private ParticleSystem particles = new ParticleSystem();
    private final ParticleSystem floodFx = new ParticleSystem();
    private final double[] floodRippleTimer;
    private AIDriver aiDriver;
    private final Rain rain = new Rain(W, H, 220);

    private double countdownTimer;
    private double raceTime;
    private double timePulse = 0;
    private final List<Path2D.Double> skyline = new ArrayList<>();
    private final BufferedImage sceneBuffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        setupWorld();
        buildSkyline();
        floodRippleTimer = new double[track.floodZones.size()];
    }

    private void buildSkyline() {
        Random r = new Random(7);
        double horizon = H * 0.42;
        double x = -60;
        while (x < W + 60) {
            double bw = 30 + r.nextInt(50);
            double bh = 60 + r.nextInt(160);
            Path2D.Double b = new Path2D.Double();
            double top = horizon - bh;
            b.moveTo(x, horizon);
            b.lineTo(x, top + r.nextInt(14));
            b.lineTo(x + bw * 0.3, top);
            b.lineTo(x + bw * 0.55, top + r.nextInt(18));
            b.lineTo(x + bw * 0.8, top - r.nextInt(10));
            b.lineTo(x + bw, top + r.nextInt(14));
            b.lineTo(x + bw, horizon);
            b.closePath();
            skyline.add(b);
            x += bw + 14 + r.nextInt(30);
        }
    }

    private void setupWorld() {
        track = new Track(W / 2.0, H / 2.0 + 20, 420, 200, 170);
        obstacles.clear();
        Random r = new Random(42);
        int n = track.centerline.size();
        for (int i = 0; i < n; i += 9) {
            if (r.nextDouble() < 0.55) {
                double side = r.nextBoolean() ? 1 : -1;
                double off = track.halfWidth + 20 + r.nextDouble() * 60;
                Point2D.Double p = track.pointAtOffset(i, side * off);
                Obstacle.Kind k = r.nextDouble() < 0.5 ? Obstacle.Kind.ROCK : Obstacle.Kind.PILLAR;
                obstacles.add(new Obstacle(k, p.x, p.y, k == Obstacle.Kind.PILLAR ? 9 + r.nextInt(6) : 12 + r.nextInt(10)));
            }
        }
        // A couple of pillars standing right at the edge of the racing line.
        int[] closePillarIdx = {60, 100, 190, 225};
        for (int idx : closePillarIdx) {
            if (idx >= n) continue;
            double side = r.nextBoolean() ? 1 : -1;
            double off = side * (track.halfWidth + 6 + r.nextDouble() * 10);
            Point2D.Double p = track.pointAtOffset(idx, off);
            obstacles.add(new Obstacle(Obstacle.Kind.PILLAR, p.x, p.y, 9 + r.nextInt(4)));
        }
        int[] hazardIdx = {45, 90, 165, 210};
        for (int idx : hazardIdx) {
            if (idx >= n) continue;
            double off = (r.nextDouble() - 0.5) * track.trackWidth * 0.6;
            Point2D.Double p = track.pointAtOffset(idx, off);
            obstacles.add(new Obstacle(Obstacle.Kind.WRECK, p.x, p.y, 16));
        }
        for (int i = 0; i < 3; i++) {
            Obstacle d = new Obstacle(Obstacle.Kind.DEBRIS, r.nextInt(W), r.nextInt(H), 13);
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 30 + r.nextDouble() * 30;
            d.vx = Math.cos(a) * sp;
            d.vy = Math.sin(a) * sp;
            obstacles.add(d);
        }
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-25);
        Point2D.Double p2pos = track.startPosition(25);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(190, 70, 50), new Color(225, 210, 150), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(70, 140, 150), new Color(220, 220, 220), vsAI ? "AI" : "2", particles);
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
        timePulse += dt;
        rain.update(dt);
        for (Obstacle o : obstacles) o.update(dt, -30, W + 30, -30, H + 30);
        updateFloodRipples(dt);

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
        floodFx.update(dt);
    }

    private void updateFloodRipples(double dt) {
        Random r = new Random();
        for (int i = 0; i < track.floodZones.size(); i++) {
            floodRippleTimer[i] -= dt;
            if (floodRippleTimer[i] <= 0) {
                floodRippleTimer[i] = 0.5 + Math.random() * 0.5;
                Point2D.Double p = track.randomPointInZone(i, r);
                floodFx.add(new Particle(p.x, p.y, 0, 0, 1.1 + Math.random() * 0.4, 4,
                        22 + Math.random() * 10, new Color(180, 220, 225, 160), Particle.ParticleKind.RIPPLE));
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
        track.draw(sg);
        floodFx.draw(sg);
        for (Obstacle o : obstacles) o.draw(sg);
        particles.draw(sg);
        for (Car c : cars) c.draw(sg);
        rain.draw(sg);
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
        // Overcast grey-teal sky, no sun.
        g.setPaint(new GradientPaint(0, 0, new Color(96, 106, 108), 0, (float) (H * 0.45), new Color(126, 136, 136)));
        g.fillRect(0, 0, W, (int) (H * 0.45));

        // drifting cloud bands
        g.setColor(new Color(80, 92, 94, 90));
        for (int i = 0; i < 5; i++) {
            double cy = 40 + i * 30 + Math.sin(timePulse * 0.15 + i) * 6;
            double cx = (i * 260 - (timePulse * 12) % 1400 + 1400) % (W + 300) - 150;
            g.fillOval((int) cx, (int) cy, 260, 26);
        }

        double horizon = H * 0.42;
        // Half-sunken skyscraper ruins silhouettes.
        for (int i = 0; i < skyline.size(); i++) {
            Path2D.Double b = skyline.get(i);
            g.setColor(i % 2 == 0 ? new Color(42, 56, 58) : new Color(35, 48, 50));
            g.fill(b);
        }
        // faint reflection of the ruins in the floodwater below the horizon
        AffineTransform old = g.getTransform();
        g.translate(0, horizon * 2);
        g.scale(1, -1);
        Composite oc = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
        for (int i = 0; i < skyline.size(); i++) {
            Path2D.Double b = skyline.get(i);
            g.setColor(i % 2 == 0 ? new Color(42, 56, 58) : new Color(35, 48, 50));
            g.fill(b);
        }
        g.setComposite(oc);
        g.setTransform(old);

        g.setPaint(new GradientPaint(0, (float) (H * 0.4), new Color(70, 84, 82), 0, H, new Color(52, 64, 62)));
        g.fillRect(0, (int) (H * 0.4), W, (int) (H * 0.6));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(150, 210, 215));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        centerText(g, "FLOODED RUINS", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Post-apocalyptic desert racing through the sunken city", H / 2 - 90);

        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        centerText(g, "[1] Player vs Player      [2] Player vs Computer", H / 2 - 20);
        centerText(g, "P1: W A S D  +  SPACE boost", H / 2 + 20);
        centerText(g, "P2: Arrow Keys + ENTER boost  (disabled vs Computer)", H / 2 + 46);
        centerText(g, "Watch for the flooded stretches — they sap your grip!", H / 2 + 76);
        centerText(g, "Press 1 or 2 to start", H / 2 + 116);
    }

    private void centerText(Graphics2D g, String s, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (W - fm.stringWidth(s)) / 2, y);
    }

    private void drawCountdown(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 90));
        g.setColor(new Color(170, 220, 225));
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

        if (p1.inFlood || p2.inFlood) {
            g.setColor(new Color(150, 210, 220));
            g.setFont(new Font("Monospaced", Font.BOLD, 15));
            centerText(g, "FLOODED SECTION — REDUCED GRIP", 48);
        }
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
        g.setColor(new Color(90, 170, 200));
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
