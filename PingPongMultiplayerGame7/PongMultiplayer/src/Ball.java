import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.util.*;
import java.util.List;

public class Ball {

    class Ball {
        double x, y;
        double vx, vy;
        double radius = 8;
        double baseSpeed = 460;
        double spin = 0;
        boolean alive = true;

        // trail buffer
        static final int TRAIL_LEN = 20;
        final Deque<Point2D.Double> trail = new ArrayDeque<>();

        void reset(int dir) {
            x = VIRTUAL_WIDTH / 2.0;
            y = VIRTUAL_HEIGHT / 2.0;
            double angle = (rng.nextDouble() * 0.6 - 0.3);
            double sp = baseSpeed;
            int dirSign;
            if (dir == 0) dirSign = rng.nextBoolean()?1:-1;
            else dirSign = dir > 0 ? 1 : -1;
            vx = Math.cos(angle) * sp * dirSign;
            vy = Math.sin(angle) * sp;
            spin = 0;
            alive = true;
        }

        void serve(int dir) {
            reset(dir);
            vy += rng.nextDouble() * 160 - 80;
        }

        void update(double dt) {
            if (fxTrails) {
                trail.addLast(new Point2D.Double(x, y));
                while (trail.size() > TRAIL_LEN) trail.removeFirst();
            } else {
                trail.clear();
            }

            vy += spin * dt * 160;
            x += vx * dt;
            y += vy * dt;

            // wall bounce
            if (y < radius + 10) {
                y = radius + 10;
                vy = Math.abs(vy);
                spin *= 0.9;
                shake(2.0);
            } else if (y > VIRTUAL_HEIGHT - radius - 10) {
                y = VIRTUAL_HEIGHT - radius - 10;
                vy = -Math.abs(vy);
                spin *= 0.9;
                shake(2.0);
            }
        }

        boolean collidePaddle(Paddle p) {
            // ghost chance to pass through if paddle has ghost
            if (p.ghost > 0 && rng.nextDouble() < 0.18) {
                return false;
            }

            Rectangle2D rect = new Rectangle2D.Double(p.x, p.y, p.w, p.h);
            Ellipse2D ball = new Ellipse2D.Double(x - radius, y - radius, radius*2, radius*2);
            if (!ball.intersects(rect)) return false;

            if ("left".equals(p.side)) {
                x = p.x + p.w + radius;
                vx = Math.abs(vx);
            } else {
                x = p.x - radius;
                vx = -Math.abs(vx);
            }

            // Aim/angle based on hit position
            double py = p.y + p.h / 2.0;
            double dy = (y - py) / (p.h / 2.0);
            double angle = dy * 0.65; // approx 37 degrees
            double sp = Math.max(Math.hypot(vx, vy), 360);
            int dir = vx >= 0 ? 1 : -1;

            vx = Math.cos(angle) * sp * dir;
            vy = Math.sin(angle) * sp;

            // Spin based on paddle control
            spin = clamp(spin + p.control * 2.0, -3.5, 3.5);

            // Speed up
            double speedUp = 1.03 + Math.abs(dy) * 0.07;
            vx *= speedUp;
            vy *= speedUp;

            p.onHit();
            return true;
        }

        void draw(Graphics2D g) {
            if (fxGlow) {
                g.setColor(new Color(255, 255, 255, 70));
                g.fill(new Ellipse2D.Double(x - radius - 6, y - radius - 6, (radius + 6)*2, (radius + 6)*2));
            }
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(x - radius, y - radius, radius*2, radius*2));

            // spin indicator
            double si = Math.abs(spin) / 3.5;
            if (si > 0.05) {
                g.setColor(new Color(200,255,255, 220));
                double r = 2 + si * 2;
                g.fill(new Ellipse2D.Double(x + spin * 2 - r/2.0, y - r/2.0, r, r));
            }
        }

        void drawTrail(Graphics2D g) {
            int i = 0;
            for (Point2D.Double p : trail) {
                float a = (i / (float)TRAIL_LEN) * 0.6f;
                g.setColor(new Color(255, 255, 255, (int)(255 * a)));
                double rr = radius * (0.6 + 0.4 * (i / (double)TRAIL_LEN));
                g.fill(new Ellipse2D.Double(p.x - rr, p.y - rr, rr*2, rr*2));
                i++;
            }
        }
    }

}
