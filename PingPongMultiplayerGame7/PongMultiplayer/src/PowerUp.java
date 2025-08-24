import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.util.*;
import java.util.List;

public class PowerUp {

    class PowerUp {
        enum Type { ENLARGE, SHRINK, SPEED, SLOW, MULTIBALL, GHOST }

        Type type;
        double x, y;
        double vx, vy;
        double radius = 12;
        double life = 10.0;
        double spin = (rng.nextDouble()*2 - 1) * 2.0;
        Color color;
        double pulse = 0;

        PowerUp(Type t) {
            this.type = t;
            this.x = VIRTUAL_WIDTH * (0.3 + rng.nextDouble() * 0.4);
            this.y = 60 + rng.nextDouble() * (VIRTUAL_HEIGHT - 120);
            this.vx = (rng.nextDouble()*2 - 1) * 30;
            this.vy = (rng.nextDouble()*2 - 1) * 30;
            this.color = colorFor(t);
        }

        Color colorFor(Type t) {
            switch (t) {
                case ENLARGE: return new Color(0x77ddff);
                case SHRINK: return new Color(0xffdd77);
                case SPEED: return new Color(0xffaa77);
                case SLOW: return new Color(0x99ff99);
                case MULTIBALL: return new Color(0xff77ff);
                case GHOST: return new Color(0xaaaaaa);
            }
            return Color.WHITE;
        }

        boolean update(double dt) {
            pulse += dt;
            x += vx * dt;
            y += vy * dt;
            vx *= 0.99;
            vy *= 0.99;
            life -= dt;

            if (x < 20 || x > VIRTUAL_WIDTH - 20) vx *= -1;
            if (y < 20 || y > VIRTUAL_HEIGHT - 20) vy *= -1;

            return life > 0;
        }

        void apply(String side) {
            Paddle p = "left".equals(side) ? leftPaddle : rightPaddle;
            switch (type) {
                case ENLARGE:
                    p.enlarge = 8.0;
                    break;
                case SHRINK:
                    Paddle opponent = "left".equals(side) ? rightPaddle : leftPaddle;
                    opponent.shrink = 8.0;
                    break;
                case SPEED:
                    for (Ball b : balls) {
                        b.vx *= 1.25;
                        b.vy *= 1.25;
                    }
                    break;
                case SLOW:
                    for (Ball b : balls) {
                        b.vx *= 0.8;
                        b.vy *= 0.8;
                    }
                    break;
                case MULTIBALL:
                    spawnExtraBall(side);
                    break;
                case GHOST:
                    p.ghost = 6.0;
                    break;
            }
        }

        void draw(Graphics2D g) {
            double s = 1 + Math.sin(pulse*6) * 0.08;
            AffineTransform old = g.getTransform();
            g.translate(x, y);
            g.rotate(spin * pulse * 0.5);
            g.scale(s, s);
            if (fxGlow) {
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
                g.fill(new Rectangle2D.Double(-radius - 6, -radius - 6, radius*2 + 12, radius*2 + 12));
            }
            g.setColor(color);
            g.fill(new Rectangle2D.Double(-radius, -radius, radius*2, radius*2));
            g.setTransform(old);
        }
    }
} // end GamePanel


}
