import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class MadMaxStylePostApocalypticRacing5 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Oil Refinery Circuit — Mad Max Style Industrial Racing");
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
    // Mutated on the game-loop thread (add/update) and read on the EDT (draw),
    // so all access is synchronized on this instance to avoid ConcurrentModificationException.
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
 * Rounded-rectangle "stadium" circuit with two inward chicane doglegs on the
 * opposing long straights. Same arc-length-based lap counting / normal-offset
 * edge technique as the reference game, just a different centerline shape.
 */
class Track {
    final double centerX, centerY;
    final double halfW, halfH, cornerR, trackWidth, halfWidth;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;

    Track(double centerX, double centerY, double halfW, double halfH, double cornerR, double trackWidth) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.halfW = halfW;
        this.halfH = halfH;
        this.cornerR = cornerR;
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        build();
    }

    private void build() {
        int pStraightLong = 60, pStraightShort = 30, pArc = 35;
        double chicaneAmp = 45;
        double chicaneWidthFrac = 0.4;

        // top straight (left->right), chicane dogleg pulls inward (+y, toward center)
        addStraight(-halfW + cornerR, -halfH, halfW - cornerR, -halfH, pStraightLong, 0, 1, chicaneAmp, chicaneWidthFrac);
        // top-right corner
        addArc(halfW - cornerR, -halfH + cornerR, cornerR, -Math.PI / 2, 0, pArc);
        // right straight (top->bottom), no chicane
        addStraight(halfW, -halfH + cornerR, halfW, halfH - cornerR, pStraightShort, -1, 0, 0, 0);
        // bottom-right corner
        addArc(halfW - cornerR, halfH - cornerR, cornerR, 0, Math.PI / 2, pArc);
        // bottom straight (right->left), chicane dogleg pulls inward (-y, toward center)
        addStraight(halfW - cornerR, halfH, -halfW + cornerR, halfH, pStraightLong, 0, -1, chicaneAmp, chicaneWidthFrac);
        // bottom-left corner
        addArc(-halfW + cornerR, halfH - cornerR, cornerR, Math.PI / 2, Math.PI, pArc);
        // left straight (bottom->top), no chicane
        addStraight(-halfW, halfH - cornerR, -halfW, -halfH + cornerR, pStraightShort, 1, 0, 0, 0);
        // top-left corner
        addArc(-halfW + cornerR, -halfH + cornerR, cornerR, Math.PI, Math.PI * 1.5, pArc);

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

    private void addStraight(double x0, double y0, double x1, double y1, int n, double nx, double ny, double amp, double widthFrac) {
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double bx = x0 + (x1 - x0) * t;
            double by = y0 + (y1 - y0) * t;
            if (amp > 0) {
                double d = Math.abs(t - 0.5);
                if (d < widthFrac / 2) {
                    double bump = amp * 0.5 * (1 + Math.cos(2 * Math.PI * d / widthFrac));
                    bx += nx * bump;
                    by += ny * bump;
                }
            }
            centerline.add(new Point2D.Double(centerX + bx, centerY + by));
        }
    }

    private void addArc(double cx, double cy, double r, double a0, double a1, int n) {
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double a = a0 + (a1 - a0) * t;
            double bx = cx + r * Math.cos(a);
            double by = cy + r * Math.sin(a);
            centerline.add(new Point2D.Double(centerX + bx, centerY + by));
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
        g.setColor(new Color(92, 88, 84));
        g.fill(road);

        g.setColor(new Color(150, 90, 55));
        g.setStroke(new BasicStroke(4));
        g.draw(outer);
        g.draw(inner);

        // corrugated-metal-toned center markings: alternating light/dark short dashes
        Path2D.Double mid = new Path2D.Double();
        for (int i = 0; i < centerline.size(); i++) {
            Point2D.Double p = centerline.get(i);
            if (i == 0) mid.moveTo(p.x, p.y); else mid.lineTo(p.x, p.y);
        }
        mid.closePath();
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{9, 6}, 0));
        g.setColor(new Color(190, 190, 185, 200));
        g.draw(mid);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{9, 6}, 9));
        g.setColor(new Color(70, 68, 66, 200));
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
            g.setColor(i % 2 == 0 ? new Color(255, 200, 30) : Color.BLACK);
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
            case ROCK: // rubble / broken concrete chunk
                g.setColor(new Color(100, 98, 94));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(60, 58, 55));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                break;
            case CACTUS: // rusty vent pipe cluster
                g.setColor(new Color(130, 68, 40));
                g.fillRoundRect((int) (-radius / 3), (int) -radius, (int) (radius * 2 / 3), (int) (radius * 2), 6, 6);
                g.fillRoundRect((int) -radius, (int) (-radius / 3), (int) radius, (int) (radius * 2 / 3), 6, 6);
                g.fillRoundRect((int) (radius / 3), (int) (-radius * 0.7), (int) (radius * 2 / 3), (int) radius, 6, 6);
                g.setColor(new Color(70, 40, 25));
                g.drawRoundRect((int) (-radius / 3), (int) -radius, (int) (radius * 2 / 3), (int) (radius * 2), 6, 6);
                break;
            case WRECK: // burnt-out chassis
                g.setColor(new Color(65, 55, 52));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(new Color(30, 26, 24));
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.setColor(new Color(200, 100, 40, 150));
                g.fillRect((int) (-radius / 2), (int) (-radius / 2), (int) radius, 3);
                break;
            case TUMBLEWEED: // rolling oil drum
                g.setColor(new Color(120, 100, 40));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                g.setColor(new Color(70, 55, 20));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));
                g.drawLine((int) -radius, (int) (-radius * 0.4), (int) radius, (int) (-radius * 0.4));
                g.drawLine((int) -radius, (int) (radius * 0.4), (int) radius, (int) (radius * 0.4));
                g.setColor(new Color(200, 60, 40));
                g.fillRect((int) (-radius * 0.3), (int) -radius, (int) (radius * 0.6), (int) (radius * 0.3));
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
    boolean inFire = false;
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
        Color c = onTrack ? new Color(150, 140, 130, 120) : new Color(160, 120, 80, 160);
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

        if (inFire) {
            g.setColor(new Color(255, 140, 30, 90 + (int) (Math.random() * 60)));
            g.fillOval((int) x - 20, (int) y - 20, 40, 40);
        }

        double hpFrac = Util.clamp(health / 100.0, 0, 1);
        g.setColor(Color.DARK_GRAY);
        g.fillRect((int) x - 16, (int) y - 26, 32, 4);
        g.setColor(hpFrac > 0.5 ? new Color(90, 200, 90) : hpFrac > 0.25 ? Color.ORANGE : Color.RED);
        g.fillRect((int) x - 16, (int) y - 26, (int) (32 * hpFrac), 4);
    }
}

/**
 * Fixed hazard that repeats a telegraph -> eruption -> cooldown cycle.
 * During eruption it is a tall flickering flame column that damages and
 * slows any car standing inside it.
 */
class FireGeyser {
    static final double TELEGRAPH = 0.8;
    static final double ERUPT = 1.0;
    static final double COOLDOWN = 1.7;
    static final double PERIOD = TELEGRAPH + ERUPT + COOLDOWN;

    enum Phase { TELEGRAPH, ERUPT, COOLDOWN }

    final double x, y;
    final double flameRadius;
    double timer;
    Phase phase;

    FireGeyser(double x, double y, double phaseOffset) {
        this.x = x;
        this.y = y;
        this.flameRadius = 34;
        this.timer = phaseOffset % PERIOD;
        updatePhase();
    }

    boolean isActive() { return phase == Phase.ERUPT; }

    void update(double dt, ParticleSystem particles) {
        timer += dt;
        if (timer >= PERIOD) timer -= PERIOD;
        updatePhase();
        if (phase == Phase.TELEGRAPH) emitTelegraph(particles);
        else if (phase == Phase.ERUPT) emitFlame(particles);
    }

    private void updatePhase() {
        if (timer < TELEGRAPH) phase = Phase.TELEGRAPH;
        else if (timer < TELEGRAPH + ERUPT) phase = Phase.ERUPT;
        else phase = Phase.COOLDOWN;
    }

    private void emitTelegraph(ParticleSystem particles) {
        if (Math.random() < 0.55) {
            particles.add(new Particle(x + (Math.random() - 0.5) * 6, y - 4,
                    (Math.random() - 0.5) * 6, -32 - Math.random() * 22,
                    0.6 + Math.random() * 0.3, 5 + Math.random() * 4, 5,
                    new Color(110, 108, 105, 150), Particle.ParticleKind.SMOKE));
        }
    }

    private void emitFlame(ParticleSystem particles) {
        for (int i = 0; i < 3; i++) {
            double jx = (Math.random() - 0.5) * 16;
            Color c = Math.random() < 0.5 ? new Color(255, 140, 20) : new Color(255, 215, 60);
            particles.add(new Particle(x + jx, y - 6, (Math.random() - 0.5) * 28, -150 - Math.random() * 100,
                    0.3 + Math.random() * 0.25, 7 + Math.random() * 7, 10, c, Particle.ParticleKind.SPARK));
        }
        if (Math.random() < 0.3) {
            particles.add(new Particle(x + (Math.random() - 0.5) * 20, y - 10, (Math.random() - 0.5) * 10, -40,
                    0.5, 8, 6, new Color(80, 78, 76, 120), Particle.ParticleKind.SMOKE));
        }
    }

    void draw(Graphics2D g) {
        g.setColor(new Color(55, 50, 46));
        g.fillOval((int) x - 17, (int) y - 8, 34, 16);
        g.setColor(new Color(25, 22, 20));
        g.fillOval((int) x - 10, (int) y - 5, 20, 10);

        if (phase == Phase.ERUPT) {
            double p = (timer - TELEGRAPH) / ERUPT;
            double rise = Util.clamp(p / 0.15, 0, 1);
            double fall = Util.clamp((1 - p) / 0.15, 0, 1);
            double heightFactor = Math.min(rise, fall);
            double h = 95 * heightFactor;
            double flicker = 5 * Math.sin(System.nanoTime() * 3e-8 + x) + (Math.random() - 0.5) * 4;

            g.setColor(new Color(255, 120, 0, (int) (70 * heightFactor)));
            g.fillOval((int) (x - 42), (int) (y - 42), 84, 84);

            Path2D.Double flame = new Path2D.Double();
            flame.moveTo(x - 15, y);
            flame.curveTo(x - 22 + flicker, y - h * 0.4, x - 9, y - h * 0.75, x, y - h);
            flame.curveTo(x + 9, y - h * 0.75, x + 22 - flicker, y - h * 0.4, x + 15, y);
            flame.closePath();
            g.setPaint(new GradientPaint(0, (float) y, new Color(255, 70, 0, 235),
                    0, (float) (y - h), new Color(255, 230, 90, 190)));
            g.fill(flame);
        }
    }
}

/** Purely decorative background element: a smokestack silhouette emitting a looping smoke plume. */
class Smokestack {
    final double x, baseY, height, width;
    double emitTimer;

    Smokestack(double x, double baseY, double height, double width) {
        this.x = x; this.baseY = baseY; this.height = height; this.width = width;
        this.emitTimer = Math.random() * 0.3;
    }

    void update(double dt, ParticleSystem smoke) {
        emitTimer -= dt;
        if (emitTimer <= 0) {
            emitTimer = 0.12 + Math.random() * 0.10;
            double topX = x + (Math.random() - 0.5) * width * 0.3;
            double topY = baseY - height;
            smoke.add(new Particle(topX, topY, (Math.random() - 0.5) * 8, -20 - Math.random() * 12,
                    3.0 + Math.random() * 1.5, 9 + Math.random() * 6, 5,
                    new Color(85, 82, 80, 90), Particle.ParticleKind.SMOKE));
        }
    }

    void draw(Graphics2D g) {
        g.setColor(new Color(38, 36, 34));
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x - width / 2, baseY);
        p.lineTo(x - width * 0.35, baseY - height);
        p.lineTo(x + width * 0.35, baseY - height);
        p.lineTo(x + width / 2, baseY);
        p.closePath();
        g.fill(p);
        g.setColor(new Color(55, 42, 38));
        g.fillRect((int) (x - width * 0.42), (int) (baseY - height - 6), (int) (width * 0.84), 8);
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

    InputState think(double dt, List<Obstacle> obstacles, List<FireGeyser> geysers) {
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

        for (FireGeyser fg : geysers) {
            if (fg.phase == FireGeyser.Phase.COOLDOWN) continue;
            double dx = fg.x - car.x, dy = fg.y - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < fg.flameRadius + 70) {
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
    private static final double FLAME_DAMAGE_PER_SEC = 34;

    private enum State { MENU, COUNTDOWN, RACING, FINISHED }

    private State state = State.MENU;
    private boolean vsAI = true;
    private Thread thread;
    private volatile boolean running = true;

    private final Set<Integer> keys = new HashSet<>();
    private Track track;
    private List<Car> cars = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<FireGeyser> geysers = new ArrayList<>();
    private final List<Smokestack> smokestacks = new ArrayList<>();
    private ParticleSystem particles = new ParticleSystem();
    private final ParticleSystem backgroundSmoke = new ParticleSystem();
    private AIDriver aiDriver;

    private double countdownTimer;
    private double raceTime;
    private double sunPulse = 0;
    private final BufferedImage sceneBuffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        buildSmokestacks();
        setupWorld();
    }

    private void buildSmokestacks() {
        smokestacks.add(new Smokestack(130, H * 0.42, 120, 30));
        smokestacks.add(new Smokestack(430, H * 0.40, 95, 24));
        smokestacks.add(new Smokestack(870, H * 0.41, 135, 32));
        smokestacks.add(new Smokestack(1150, H * 0.39, 100, 26));
    }

    private void setupWorld() {
        track = new Track(W / 2.0, H / 2.0 + 20, 380, 230, 170, 150);
        obstacles.clear();
        geysers.clear();
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
        double[] hazardFracs = {0.15, 0.35, 0.55, 0.78};
        for (double f : hazardFracs) {
            int idx = (int) (f * n) % n;
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

        // Fire geysers: staggered around the loop so eruptions cascade rather than sync,
        // two of them sitting right at the chicane apexes for extra hazard.
        double[] geyserFracs = {0.09, 0.23, 0.40, 0.59, 0.75, 0.90};
        double[] geyserOffsets = {10, -45, 40, -12, 35, -35};
        double phaseStep = FireGeyser.PERIOD / geyserFracs.length;
        for (int i = 0; i < geyserFracs.length; i++) {
            int idx = (int) (geyserFracs[i] * n) % n;
            Point2D.Double p = track.pointAtOffset(idx, geyserOffsets[i]);
            geysers.add(new FireGeyser(p.x, p.y, i * phaseStep));
        }
    }

    private void setupRace() {
        cars.clear();
        particles = new ParticleSystem();
        double angle = track.startAngle();
        Point2D.Double p1pos = track.startPosition(-25);
        Point2D.Double p2pos = track.startPosition(25);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(190, 70, 40), new Color(255, 190, 40), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(95, 100, 105), new Color(200, 60, 40), vsAI ? "AI" : "2", particles);
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
        for (Smokestack s : smokestacks) s.update(dt, backgroundSmoke);
        backgroundSmoke.update(dt);
        for (FireGeyser fg : geysers) fg.update(dt, particles);

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
            in2 = aiDriver.think(dt, obstacles, geysers);
        } else {
            in2 = new InputState();
            in2.throttle = keys.contains(KeyEvent.VK_UP) ? 1 : keys.contains(KeyEvent.VK_DOWN) ? -1 : 0;
            in2.steer = keys.contains(KeyEvent.VK_LEFT) ? -1 : keys.contains(KeyEvent.VK_RIGHT) ? 1 : 0;
            in2.boost = keys.contains(KeyEvent.VK_ENTER) || keys.contains(KeyEvent.VK_SHIFT);
        }

        p1.update(dt, in1, track);
        p2.update(dt, in2, track);

        applyGeyserEffects(p1, dt);
        applyGeyserEffects(p2, dt);

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

    private void applyGeyserEffects(Car c, double dt) {
        c.inFire = false;
        if (!c.alive) return;
        for (FireGeyser fg : geysers) {
            if (!fg.isActive()) continue;
            double dist = Math.hypot(c.x - fg.x, c.y - fg.y);
            if (dist < fg.flameRadius) {
                c.inFire = true;
                c.damage(FLAME_DAMAGE_PER_SEC * dt);
                c.speed *= 0.90; // strong speed penalty while standing in the flame column
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
        for (Smokestack s : smokestacks) s.draw(sg);
        backgroundSmoke.draw(sg);
        track.draw(sg);
        for (Obstacle o : obstacles) o.draw(sg);
        for (FireGeyser fg : geysers) fg.draw(sg);
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
        // orange fire-haze sky over an industrial grey/rust wasteland
        g.setPaint(new GradientPaint(0, 0, new Color(80, 40, 30), 0, (float) (H * 0.22), new Color(190, 90, 40)));
        g.fillRect(0, 0, W, (int) (H * 0.22));
        g.setPaint(new GradientPaint(0, (float) (H * 0.22), new Color(190, 90, 40), 0, (float) (H * 0.45), new Color(210, 150, 100)));
        g.fillRect(0, (int) (H * 0.22), W, (int) (H * 0.45) - (int) (H * 0.22));

        double pulse = 1 + 0.03 * Math.sin(sunPulse * 1.5);
        int sunR = (int) (55 * pulse);
        g.setPaint(new RadialGradientPaint(new Point(W - 220, 110), sunR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(255, 210, 150, 210), new Color(255, 140, 60, 90), new Color(255, 140, 60, 0)}));
        g.fillOval(W - 220 - sunR * 3, 110 - sunR * 3, sunR * 6, sunR * 6);
        g.setColor(new Color(255, 225, 190));
        g.fillOval(W - 220 - sunR / 2, 110 - sunR / 2, sunR, sunR);

        // low industrial haze band
        g.setColor(new Color(120, 80, 60, 90));
        g.fillRect(0, (int) (H * 0.36), W, (int) (H * 0.08));

        g.setPaint(new GradientPaint(0, (float) (H * 0.4), new Color(96, 90, 82), 0, H, new Color(70, 64, 58)));
        g.fillRect(0, (int) (H * 0.4), W, (int) (H * 0.6));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(230, 120, 40));
        g.setFont(new Font("SansSerif", Font.BOLD, 50));
        centerText(g, "OIL REFINERY CIRCUIT", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Mad Max style industrial desert racing — mind the fire geysers", H / 2 - 90);

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
        g.setColor(new Color(20, 18, 16, 150));
        g.fillRoundRect(x, 16, 190, 60, 10, 10);
        g.setColor(new Color(150, 90, 55));
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
        g.setColor(new Color(230, 140, 40));
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
