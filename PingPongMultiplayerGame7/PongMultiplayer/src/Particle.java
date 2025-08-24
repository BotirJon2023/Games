import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.util.*;
import java.util.List;

public class Particle {

    static class Particle {
        double x, y, vx, vy, life;
        double size;
        Color color;

        static Particle spray(double x, double y, Color color) {
            Particle p = new Particle();
            p.x = x; p.y = y;
            Random r = new Random();
            p.vx = (r.nextDouble()*2 - 1) * 180;
            p.vy = (r.nextDouble()*2 - 1) * 180;
            p.life = 0.25 + r.nextDouble() * 0.35;
            p.size = 2 + r.nextDouble() * 3.5;
            p.color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 200);
            return p;
        }

        boolean update(double dt) {
            x += vx * dt;
            y += vy * dt;
            vx *= 0.96;
            vy *= 0.96;
            life -= dt;
            return life > 0;
        }

        void draw(Graphics2D g) {
            float a = (float) Math.max(0, Math.min(1, life * 2));
            Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255 * a));
            g.setColor(c);
            g.fill(new Rectangle2D.Double(x, y, size, size));
        }
    }

}
