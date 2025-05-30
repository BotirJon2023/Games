import java.awt.*;

public class Opponent {
    private int x, y;
    private final int width = 30, height = 30;
    private final int speed = 2;

    public Opponent(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void chaseFrisbee(Frisbee f) {
        if (!f.isMoving()) return;

        if (x > f.getX()) x -= speed;
        if (x < f.getX()) x += speed;
        if (y > f.getY()) y -= speed;
        if (y < f.getY()) y += speed;
    }

    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillRect(x, y, width, height);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}
