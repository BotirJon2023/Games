import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.util.*;
import java.util.List;

public class Paddle {

    class Paddle {
        String side; // "left" or "right"
        double x, y;
        double w = 14;
        double h = 90;
        double baseH = 90;
        double speed = 520;
        Color color;
        double control = 0;
        double padShake = 0;

        double enlarge = 0;
        double shrink = 0;
        double ghost = 0;

        Paddle(String side) {
            this.side = side;
            if ("left".equals(side)) {
                x = 40;
                color = themes.get(themeIndex).left;
            } else {
                x = VIRTUAL_WIDTH - 40 - w;
                color = themes.get(themeIndex).right;
            }
            y = VIRTUAL_HEIGHT / 2.0 - h / 2.0;
        }

        void update(double dt, boolean isAI, Set<Integer> keys, int keyUp, int keyDown) {
            // timers
            if (enlarge > 0) enlarge -= dt;
            if (shrink > 0) shrink -= dt;
            if (ghost > 0) ghost -= dt;

            // height adjust
            double sizeMul = (enlarge > 0 ? 1.35 : 1.0) * (shrink > 0 ? 0.7 : 1.0);
            h = clamp(baseH * sizeMul, 50, 220);

            double axis = 0.0;
            if (isAI) {
                // Simple AI: pursue nearest ball heading toward
                Ball target = null; double bestT = Double.POSITIVE_INFINITY;
                for (Ball b : balls) {
                    if (!b.alive) continue;
                    boolean towards = "left".equals(side) ? b.vx < 0 : b.vx > 0;
                    if (towards) {
                        double tx = ("left".equals(side) ? (x + w/2.0) - b.x : b.x - (x + w/2.0));
                        double t = Math.abs(tx / (b.vx == 0 ? 1 : b.vx));
                        if (t < bestT) { bestT = t; target = b; }
                    }
                }
                double aim = target != null ? target.y : VIRTUAL_HEIGHT / 2.0;
                double center = y + h / 2.0;
                double diff = aim - center;
                axis = clamp(diff / 80.0, -1.0, 1.0);
                axis += (rng.nextDouble()*2-1) * 0.08; // jitter
            } else {
                boolean up = keys.contains(keyUp);
                boolean down = keys.contains(keyDown);
                axis = (down ? 1.0 : 0.0) - (up ? 1.0 : 0.0);
            }

            control = clamp(axis, -1, 1);
            double vy = control * speed;
            y += vy * dt;
            y = clamp(y, 10, VIRTUAL_HEIGHT - h - 10);

            padShake *= 0.9;
        }

        void draw(Graphics2D g) {
            // paddle slight jitter when hit
            double sx = (rng.nextDouble()*2 - 1) * padShake;
            double sy = (rng.nextDouble()*2 - 1) * padShake;

            if (fxGlow) {
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
                g.fill(new Rectangle2D.Double(x - 4 + sx, y - 4 + sy, w + 8, h + 8));
            }
            g.setColor(color);
            g.fill(new Rectangle2D.Double(x + sx, y + sy, w, h));

            if (ghost > 0) {
                float a = (float) clamp(ghost / 6.0, 0, 1);
                g.setColor(new Color(255, 255, 255, (int)(60 * a)));
                g.fill(new Rectangle2D.Double(x - 3, y - 3, w + 6, h + 6));
            }
        }

        void onHit() {
            padShake = 1.5;
        }
    }

}
