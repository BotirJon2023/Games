import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class MadMaxStylePostApocalypticRacing3 {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Night Raiders Highway — Mad Max Style Night Desert Racing");
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

    /** Draws a translucent triangular headlight cone from (x,y) facing angle, fading out with distance. */
    static void drawHeadlightCone(Graphics2D g, double x, double y, double angle, double startOffset,
                                   double length, double halfAngle, Color bright) {
        double nx = x + Math.cos(angle) * startOffset;
        double ny = y + Math.sin(angle) * startOffset;
        double leftAng = angle - halfAngle;
        double rightAng = angle + halfAngle;
        double flx = nx + Math.cos(leftAng) * length;
        double fly = ny + Math.sin(leftAng) * length;
        double frx = nx + Math.cos(rightAng) * length;
        double fry = ny + Math.sin(rightAng) * length;
        double midx = nx + Math.cos(angle) * length;
        double midy = ny + Math.sin(angle) * length;

        Path2D.Double tri = new Path2D.Double();
        tri.moveTo(nx, ny);
        tri.lineTo(flx, fly);
        tri.lineTo(frx, fry);
        tri.closePath();

        Color transparent = new Color(bright.getRed(), bright.getGreen(), bright.getBlue(), 0);
        GradientPaint gp = new GradientPaint((float) nx, (float) ny, bright, (float) midx, (float) midy, transparent);
        Paint old = g.getPaint();
        g.setPaint(gp);
        g.fill(tri);
        g.setPaint(old);
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

class Star {
    double x, y, radius, baseAlpha, phase, speed;
    boolean twinkle;

    Star(double x, double y, double radius, double baseAlpha, boolean twinkle, double phase, double speed) {
        this.x = x; this.y = y; this.radius = radius; this.baseAlpha = baseAlpha;
        this.twinkle = twinkle; this.phase = phase; this.speed = speed;
    }
}

/**
 * Rounded-rectangle "highway loop" track with sharp-ish corners and one wide serpentine
 * S-bulge cut into a long straight. The centerline is a simple closed loop; lap counting
 * and edge rendering/collision both rely on the parallel cumulative arc-length array and
 * per-point normals, exactly like the proven oval-track approach — that technique works
 * for any simple non-self-intersecting closed loop, not just an oval.
 */
class Track {
    final double centerX, centerY, width, height, cornerRadius, trackWidth, halfWidth, bulgeAmplitude;
    final double halfW, halfH;
    final List<Point2D.Double> centerline = new ArrayList<>();
    final List<Point2D.Double> normals = new ArrayList<>();
    double[] cumulativeLength;
    double totalLength;

    Track(double centerX, double centerY, double width, double height, double cornerRadius,
          double trackWidth, double bulgeAmplitude) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.width = width;
        this.height = height;
        this.cornerRadius = cornerRadius;
        this.trackWidth = trackWidth;
        this.halfWidth = trackWidth / 2;
        this.bulgeAmplitude = bulgeAmplitude;
        this.halfW = width / 2;
        this.halfH = height / 2;
        build();
    }

    private void addLocal(List<double[]> pts, double x, double y) {
        pts.add(new double[]{x, y});
    }

    private void build() {
        int pLong = 100, pShort = 50, pArc = 40;
        double r = cornerRadius;
        double Lx = width - 2 * r;
        double Ly = height - 2 * r;
        double bulgeStart = 0.05, bulgeEnd = 0.95;

        List<double[]> pts = new ArrayList<>();

        // 1. top edge, moving +x
        for (int i = 0; i < pLong; i++) {
            double t = i / (double) pLong;
            addLocal(pts, -halfW + r + t * Lx, -halfH);
        }
        // 2. top-right corner, angle -90 -> 0
        for (int i = 0; i < pArc; i++) {
            double t = i / (double) pArc;
            double ang = -Math.PI / 2 + t * (Math.PI / 2);
            double cx = halfW - r, cy = -halfH + r;
            addLocal(pts, cx + r * Math.cos(ang), cy + r * Math.sin(ang));
        }
        // 3. right edge, moving +y
        for (int i = 0; i < pShort; i++) {
            double t = i / (double) pShort;
            addLocal(pts, halfW, -halfH + r + t * Ly);
        }
        // 4. bottom-right corner, angle 0 -> 90
        for (int i = 0; i < pArc; i++) {
            double t = i / (double) pArc;
            double ang = t * (Math.PI / 2);
            double cx = halfW - r, cy = halfH - r;
            addLocal(pts, cx + r * Math.cos(ang), cy + r * Math.sin(ang));
        }
        // 5. bottom edge (with serpentine S-bulge), moving -x
        for (int i = 0; i < pLong; i++) {
            double t = i / (double) pLong;
            double x = (halfW - r) - t * Lx;
            double y = halfH;
            if (t >= bulgeStart && t <= bulgeEnd) {
                double u = (t - bulgeStart) / (bulgeEnd - bulgeStart);
                double taper = Math.sin(Math.PI * u);
                y += bulgeAmplitude * Math.sin(2 * Math.PI * u) * taper;
            }
            addLocal(pts, x, y);
        }
        // 6. bottom-left corner, angle 90 -> 180
        for (int i = 0; i < pArc; i++) {
            double t = i / (double) pArc;
            double ang = Math.PI / 2 + t * (Math.PI / 2);
            double cx = -halfW + r, cy = halfH - r;
            addLocal(pts, cx + r * Math.cos(ang), cy + r * Math.sin(ang));
        }
        // 7. left edge, moving -y
        for (int i = 0; i < pShort; i++) {
            double t = i / (double) pShort;
            addLocal(pts, -halfW, halfH - r - t * Ly);
        }
        // 8. top-left corner, angle 180 -> 270
        for (int i = 0; i < pArc; i++) {
            double t = i / (double) pArc;
            double ang = Math.PI + t * (Math.PI / 2);
            double cx = -halfW + r, cy = -halfH + r;
            addLocal(pts, cx + r * Math.cos(ang), cy + r * Math.sin(ang));
        }

        for (double[] p : pts) {
            centerline.add(new Point2D.Double(centerX + p[0], centerY + p[1]));
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
        g.setColor(new Color(32, 32, 36));
        g.fill(road);

        g.setColor(new Color(70, 70, 78));
        g.setStroke(new BasicStroke(3));
        g.draw(outer);
        g.draw(inner);

        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{14, 18}, 0));
        g.setColor(new Color(215, 190, 110, 160));
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
                g.setColor(new Color(64, 66, 74));
                g.fillOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                g.setColor(new Color(40, 42, 48));
                g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 1.6));
                break;
            case CACTUS:
                g.setColor(new Color(28, 48, 34));
                g.fillRoundRect((int) (-radius / 3), (int) -radius, (int) (radius * 2 / 3), (int) (radius * 2), 8, 8);
                g.fillRoundRect((int) -radius, (int) (-radius / 3), (int) radius, (int) (radius * 2 / 3), 8, 8);
                g.fillRoundRect((int) (radius / 3), (int) (-radius * 0.7), (int) (radius * 2 / 3), (int) radius, 8, 8);
                break;
            case WRECK:
                g.setColor(new Color(48, 26, 22));
                g.fillRect((int) -radius, (int) (-radius / 2), (int) (radius * 2), (int) radius);
                g.setColor(Color.BLACK);
                g.fillOval((int) -radius, (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.fillOval((int) (radius / 2), (int) (-radius / 2), (int) (radius / 2), (int) (radius / 2));
                g.setColor(new Color(255, 110, 40, 150));
                g.fillOval(-3, -3, 6, 6);
                break;
            case TUMBLEWEED:
                g.setColor(new Color(110, 100, 80));
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
        Color c = onTrack ? new Color(150, 150, 150, 110) : new Color(110, 90, 70, 150);
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
                    Math.random() < 0.5 ? new Color(255, 120, 30) : new Color(70, 70, 70), Particle.ParticleKind.SMOKE));
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
        g.setColor(new Color(20, 20, 24));
        g.fillRect(14, -4, 8, 2);
        g.fillRect(14, 2, 8, 2);
        g.setColor(new Color(255, 250, 210));
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

    InputState think(double dt, List<Obstacle> obstacles, RaiderBuggy buggy) {
        InputState in = new InputState();
        int idx = track.nearestIndex(car.x, car.y);
        int lookahead = 22;
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

        if (buggy != null) {
            double dx = buggy.x - car.x, dy = buggy.y - car.y;
            double dist = Math.hypot(dx, dy);
            if (dist < 150) {
                double angToObs = Util.normalizeAngle(Math.atan2(dy, dx) - car.angle);
                if (Math.abs(angToObs) < 1.0) {
                    diff += angToObs < 0 ? 0.9 : -0.9;
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

/**
 * The signature new hazard for this game: an independent hostile vehicle that is not a
 * racer and not part of lap/finish logic. It relentlessly chases whichever of the two
 * racing cars is currently closer, at a steady speed, and rams with heavy damage and
 * knockback on contact — bigger, faster and nastier than a tumbleweed, and it never
 * wanders off; it keeps hunting.
 */
class RaiderBuggy {
    double x, y, angle;
    final double speed = 195;
    final double radius = 22;
    double ramCooldown = 0;
    private final double turnRate = 2.6;
    private final ParticleSystem particles;

    RaiderBuggy(double x, double y, double angle, ParticleSystem particles) {
        this.x = x; this.y = y; this.angle = angle;
        this.particles = particles;
    }

    void update(double dt, Car target1, Car target2, double minX, double maxX, double minY, double maxY) {
        Car target = pickTarget(target1, target2);
        if (target != null) {
            double desired = Math.atan2(target.y - y, target.x - x);
            double diff = Util.normalizeAngle(desired - angle);
            double maxTurn = turnRate * dt;
            angle += Util.clamp(diff, -maxTurn, maxTurn);
        }
        x += Math.cos(angle) * speed * dt;
        y += Math.sin(angle) * speed * dt;
        x = Util.clamp(x, minX, maxX);
        y = Util.clamp(y, minY, maxY);

        if (ramCooldown > 0) ramCooldown -= dt;

        if (Math.random() < 0.35) {
            double rx = x - Math.cos(angle) * radius;
            double ry = y - Math.sin(angle) * radius;
            particles.add(new Particle(rx, ry, -Math.cos(angle) * 40 + (Math.random() - 0.5) * 30,
                    -Math.sin(angle) * 40 + (Math.random() - 0.5) * 30,
                    0.4 + Math.random() * 0.3, 5 + Math.random() * 5, 6,
                    new Color(120, 90, 60, 140), Particle.ParticleKind.DUST));
        }
    }

    private Car pickTarget(Car a, Car b) {
        boolean aOk = a != null && a.alive;
        boolean bOk = b != null && b.alive;
        if (aOk && bOk) {
            double da = Math.hypot(a.x - x, a.y - y);
            double db = Math.hypot(b.x - x, b.y - y);
            return da <= db ? a : b;
        } else if (aOk) return a;
        else if (bOk) return b;
        return null;
    }

    void draw(Graphics2D g) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(angle);

        g.setColor(new Color(18, 18, 18));
        g.fillRoundRect(-22, -15, 12, 8, 3, 3);
        g.fillRoundRect(-22, 7, 12, 8, 3, 3);
        g.fillRoundRect(10, -15, 12, 8, 3, 3);
        g.fillRoundRect(10, 7, 12, 8, 3, 3);

        g.setColor(new Color(58, 24, 20));
        g.fillRoundRect(-22, -13, 44, 26, 6, 6);
        g.setColor(new Color(26, 26, 28));
        g.fillRoundRect(-8, -10, 18, 20, 5, 5);

        g.setColor(new Color(90, 88, 86));
        g.fillRect(18, -3, 8, 2);
        g.fillRect(18, 3, 8, 2);

        g.setColor(new Color(70, 68, 70));
        for (int i = -1; i <= 1; i++) {
            int by = i * 8;
            Polygon spike = new Polygon();
            spike.addPoint(22, by - 2);
            spike.addPoint(30, by);
            spike.addPoint(22, by + 2);
            g.fillPolygon(spike);
        }

        g.setColor(new Color(255, 205, 130));
        g.fillOval(19, -6, 4, 4);
        g.fillOval(19, 2, 4, 4);

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
    private RaiderBuggy buggy;

    private double countdownTimer;
    private double raceTime;
    private double starTime = 0;
    private final List<Point2D.Double> mountains = new ArrayList<>();
    private final List<Star> stars = new ArrayList<>();

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        setupWorld();
        buildMountains();
        buildStars();
    }

    private void buildMountains() {
        Random r = new Random(7);
        double x = -50;
        while (x < W + 50) {
            mountains.add(new Point2D.Double(x, 90 + r.nextInt(70)));
            x += 40 + r.nextInt(40);
        }
    }

    private void buildStars() {
        Random r = new Random(19);
        for (int i = 0; i < 220; i++) {
            double x = r.nextDouble() * W;
            double y = r.nextDouble() * H * 0.55;
            double radius = 0.6 + r.nextDouble() * 1.6;
            double baseAlpha = 0.35 + r.nextDouble() * 0.55;
            boolean twinkle = r.nextDouble() < 0.35;
            double phase = r.nextDouble() * Math.PI * 2;
            double speed = 1.0 + r.nextDouble() * 2.5;
            stars.add(new Star(x, y, radius, baseAlpha, twinkle, phase, speed));
        }
    }

    private void setupWorld() {
        track = new Track(640, 390, 760, 390, 115, 150, 35);
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
        int[] hazardIdx = {n / 10, n * 3 / 10, n / 2, n * 7 / 10};
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
        Point2D.Double p1pos = track.startPosition(-20);
        Point2D.Double p2pos = track.startPosition(20);
        Car p1 = new Car(p1pos.x, p1pos.y, angle, new Color(205, 65, 40), new Color(230, 200, 120), "1", particles);
        Car p2 = new Car(p2pos.x, p2pos.y, angle, new Color(65, 130, 200), new Color(220, 220, 220), vsAI ? "AI" : "2", particles);
        p1.lastArcLen = track.progress(p1.x, p1.y);
        p2.lastArcLen = track.progress(p2.x, p2.y);
        cars.add(p1);
        cars.add(p2);
        aiDriver = vsAI ? new AIDriver(p2, track) : null;

        int n = track.centerline.size();
        int buggyIdx = (n * 55) / 100;
        Point2D.Double bp = track.pointAtOffset(buggyIdx, 0);
        Point2D.Double bNext = track.centerline.get((buggyIdx + 3) % n);
        buggy = new RaiderBuggy(bp.x, bp.y, Math.atan2(bNext.y - bp.y, bNext.x - bp.x), particles);

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
        starTime += dt;
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
            in2 = aiDriver.think(dt, obstacles, buggy);
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

        buggy.update(dt, p1, p2, -40, W + 40, -40, H + 40);
        resolveBuggyCollision(p1, buggy);
        resolveBuggyCollision(p2, buggy);

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

    private void resolveBuggyCollision(Car c, RaiderBuggy b) {
        if (!c.alive) return;
        double dx = c.x - b.x, dy = c.y - b.y;
        double dist = Math.hypot(dx, dy);
        double minDist = c.radius() + b.radius;
        if (dist < minDist && dist > 0.0001) {
            double nx = dx / dist, ny = dy / dist;
            double overlap = minDist - dist;
            c.x += nx * overlap;
            c.y += ny * overlap;
            if (b.ramCooldown <= 0) {
                double buggyVx = Math.cos(b.angle) * b.speed;
                double buggyVy = Math.sin(b.angle) * b.speed;
                double impact = Math.hypot(c.vx - buggyVx, c.vy - buggyVy);
                c.damage(42 + impact * 0.05);
                c.vx += nx * 320;
                c.vy += ny * 320;
                c.speed = -Math.abs(c.speed) * 0.4;
                c.spinOut(1.1);
                b.ramCooldown = 0.6;
                for (int i = 0; i < 16; i++) {
                    double ang = Math.random() * Math.PI * 2;
                    double sp = 80 + Math.random() * 200;
                    particles.add(new Particle((c.x + b.x) / 2, (c.y + b.y) / 2,
                            Math.cos(ang) * sp, Math.sin(ang) * sp, 0.4 + Math.random() * 0.3,
                            4 + Math.random() * 6, 8,
                            Math.random() < 0.5 ? new Color(255, 140, 40) : new Color(255, 60, 40),
                            Particle.ParticleKind.SPARK));
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g);
        track.draw(g);

        for (Car c : cars) {
            if (c.alive) {
                Util.drawHeadlightCone(g, c.x, c.y, c.angle, c.radius() - 4, 260,
                        Math.toRadians(18), new Color(255, 250, 210, 95));
            }
        }
        if (buggy != null) {
            double lateral = 8;
            double lx = buggy.x - Math.sin(buggy.angle) * lateral, ly = buggy.y + Math.cos(buggy.angle) * lateral;
            double rx = buggy.x + Math.sin(buggy.angle) * lateral, ry = buggy.y - Math.cos(buggy.angle) * lateral;
            Util.drawHeadlightCone(g, lx, ly, buggy.angle, buggy.radius - 6, 170,
                    Math.toRadians(10), new Color(255, 210, 150, 85));
            Util.drawHeadlightCone(g, rx, ry, buggy.angle, buggy.radius - 6, 170,
                    Math.toRadians(10), new Color(255, 210, 150, 85));
        }

        for (Obstacle o : obstacles) o.draw(g);
        if (buggy != null) buggy.draw(g);
        particles.draw(g);
        for (Car c : cars) c.draw(g);

        switch (state) {
            case MENU: drawMenu(g); break;
            case COUNTDOWN: drawCountdown(g); break;
            case RACING: drawHud(g); break;
            case FINISHED: drawFinish(g); break;
        }
    }

    private void drawBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(5, 7, 16), 0, (float) (H * 0.55), new Color(16, 18, 32)));
        g.fillRect(0, 0, W, (int) (H * 0.55));

        for (Star s : stars) {
            float a = (float) s.baseAlpha;
            if (s.twinkle) {
                a = (float) Util.clamp(s.baseAlpha * (0.4 + 0.6 * (0.5 + 0.5 * Math.sin(s.phase + starTime * s.speed))), 0, 1);
            }
            g.setColor(new Color(1f, 1f, 1f, a));
            g.fill(new Ellipse2D.Double(s.x - s.radius / 2, s.y - s.radius / 2, s.radius, s.radius));
        }

        double pulse = 1 + 0.02 * Math.sin(starTime * 1.1);
        int moonR = (int) (46 * pulse);
        int mx = W - 200, my = 100;
        g.setPaint(new RadialGradientPaint(new Point(mx, my), moonR * 3,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(230, 235, 255, 200), new Color(200, 210, 235, 70), new Color(200, 210, 235, 0)}));
        g.fillOval(mx - moonR * 3, my - moonR * 3, moonR * 6, moonR * 6);
        g.setColor(new Color(235, 238, 250));
        g.fillOval(mx - moonR / 2, my - moonR / 2, moonR, moonR);
        g.setColor(new Color(202, 208, 224));
        g.fillOval(mx - moonR / 2 + 6, my - moonR / 2 + 8, moonR / 4, moonR / 4);
        g.fillOval(mx - moonR / 2 + 18, my - moonR / 2 + 2, moonR / 5, moonR / 5);

        g.setColor(new Color(13, 12, 18));
        Path2D.Double range = new Path2D.Double();
        range.moveTo(-50, H * 0.5);
        for (Point2D.Double m : mountains) range.lineTo(m.x, H * 0.5 - m.y * 0.5);
        range.lineTo(W + 50, H * 0.5);
        range.closePath();
        g.fill(range);

        g.setPaint(new GradientPaint(0, (float) (H * 0.46), new Color(36, 30, 26), 0, H, new Color(16, 14, 13)));
        g.fillRect(0, (int) (H * 0.46), W, (int) (H * 0.54));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(255, 160, 60));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        centerText(g, "NIGHT RAIDERS HIGHWAY", H / 2 - 140);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        centerText(g, "Mad Max style night desert racing — outrun the Raider Buggy", H / 2 - 90);

        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        centerText(g, "[1] Player vs Player      [2] Player vs Computer", H / 2 - 20);
        centerText(g, "P1: W A S D  +  SPACE boost", H / 2 + 20);
        centerText(g, "P2: Arrow Keys + ENTER boost  (disabled vs Computer)", H / 2 + 46);
        g.setColor(new Color(255, 120, 100));
        centerText(g, "Beware: a hostile Raider Buggy hunts whoever is closest", H / 2 + 76);
        g.setColor(Color.WHITE);
        centerText(g, "Press 1 or 2 to start", H / 2 + 110);
    }

    private void centerText(Graphics2D g, String s, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (W - fm.stringWidth(s)) / 2, y);
    }

    private void drawCountdown(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 90));
        g.setColor(new Color(255, 220, 90));
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

        if (buggy != null) {
            double d1 = p1.alive ? Math.hypot(p1.x - buggy.x, p1.y - buggy.y) : Double.MAX_VALUE;
            double d2 = p2.alive ? Math.hypot(p2.x - buggy.x, p2.y - buggy.y) : Double.MAX_VALUE;
            double closest = Math.min(d1, d2);
            if (closest < 220 && ((int) (raceTime * 6)) % 2 == 0) {
                g.setColor(new Color(255, 60, 50));
                g.setFont(new Font("SansSerif", Font.BOLD, 20));
                centerText(g, "! RAIDER BUGGY INCOMING !", 58);
            }
        }
    }

    private String leadingCarLabel() {
        Car p1 = cars.get(0), p2 = cars.get(1);
        return p1.unwrappedDistance >= p2.unwrappedDistance ? p1.label : p2.label;
    }

    private void drawDriverHud(Graphics2D g, Car c, int x, String tag) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(x, 16, 190, 60, 10, 10);
        g.setColor(new Color(255, 200, 130));
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString(tag + "  LAP " + Math.min(c.lap + 1, TOTAL_LAPS), x + 10, 32);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 10, 40, 170, 8);
        double hp = Util.clamp(c.health / 100.0, 0, 1);
        g.setColor(hp > 0.5 ? new Color(90, 200, 90) : hp > 0.25 ? Color.ORANGE : Color.RED);
        g.fillRect(x + 10, 40, (int) (170 * hp), 8);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 10, 54, 170, 8);
        g.setColor(new Color(120, 170, 255));
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
        g.setColor(new Color(255, 160, 60));
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
