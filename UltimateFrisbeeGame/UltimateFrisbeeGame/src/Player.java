import java.awt.*;

public class Player {
    private int x, y;
    private final int width = 30, height = 30;
    private boolean left, right, up, down;
    private final int speed = 5;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move() {
        if (left) x -= speed;
        if (right) x += speed;
        if (up) y -= speed;
        if (down) y += speed;

        // Clamp to field
        x = Math.max(50, Math.min(x, 950 - width));
        y = Math.max(50, Math.min(y, 650 - height));
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillRect(x, y, width, height);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public int getX() { return x; }

    public int getY() { return y; }

    public void setLeft(boolean val) { left = val; }

    public void setRight(boolean val) { right = val; }

    public void setUp(boolean val) { up = val; }

    public void setDown(boolean val) { down = val; }
}
