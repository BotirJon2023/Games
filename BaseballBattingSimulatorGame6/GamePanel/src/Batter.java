// Batter.java
import java.awt.*;

public class Batter {
    private int x, y;
    private boolean swinging = false;
    private int swingTimer = 0;

    public Batter(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void swing() {
        swinging = true;
        swingTimer = 10;
    }

    public boolean isSwinging() {
        return swinging;
    }

    public void update() {
        if (swinging) {
            swingTimer--;
            if (swingTimer <= 0) swinging = false;
        }
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillRect(x, y, 50, 100);
        if (swinging) {
            g.setColor(Color.RED);
            g.drawString("Swing!", x, y - 10);
        }
    }
}
