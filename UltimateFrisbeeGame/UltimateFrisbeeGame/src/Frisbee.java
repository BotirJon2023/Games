import java.awt.*;

public class Frisbee {
    private int x, y;
    private final int size = 15;
    private boolean moving = false;
    private final int speed = 8;
    private final Player player;

    public Frisbee(Player player) {
        this.player = player;
        reset();
    }

    public void throwFrisbee() {
        if (!moving) {
            moving = true;
        }
    }

    public void update() {
        if (moving) {
            x += speed;
        }
    }

    public void reset() {
        moving = false;
        x = player.getX() + 10;
        y = player.getY() + 10;
    }

    public void draw(Graphics g) {
        g.setColor(Color.YELLOW);
        g.fillOval(x, y, size, size);
    }

    public boolean isMoving() {
        return moving;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }

    public int getX() { return x; }

    public int getY() { return y; }
}
