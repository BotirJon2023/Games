// Pitch.java
import java.awt.*;

public class Pitch {
    private int x, y;
    private int speed;
    private boolean active;

    public Pitch() {
        reset();
    }

    public void reset() {
        x = 400;
        y = 0;
        speed = 5 + (int)(Math.random() * 5); // Random speed between 5–10
        active = true;
    }

    public void update() {
        if (active) {
            y += speed;
            if (y > 600) {
                active = false;
                reset();
            }
        }
    }

    public void draw(Graphics g) {
        if (active) {
            g.setColor(Color.BLACK);
            g.fillOval(x, y, 20, 20);
        }
    }

    public boolean isNearBatter() {
        return y >= 480 && y <= 520;
    }

    public int getSpeed() {
        return speed;
    }

    public boolean isActive() {
        return active;
    }
}
