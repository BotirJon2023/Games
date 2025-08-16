// Ball.java
import java.awt.*;

public class Ball {
    private int x, y;
    private int vx, vy;
    private boolean inAir;

    public Ball() {
        reset();
    }

    public void reset() {
        x = 400;
        y = 500;
        vx = 0;
        vy = 0;
        inAir = false;
    }

    public void hit(int pitchSpeed) {
        vx = (int)(Math.random() * 10 - 5); // Random horizontal direction
        vy = -pitchSpeed * 2;
        inAir = true;
    }

    public void update() {
        if (inAir) {
            x += vx;
            y += vy;
            vy += 1; // gravity
            if (y > 600) {
                inAir = false;
                reset();
            }
        }
    }

    public void draw(Graphics g) {
        if (inAir) {
            g.setColor(Color.ORANGE);
            g.fillOval(x, y, 15, 15);
        }
    }
}
