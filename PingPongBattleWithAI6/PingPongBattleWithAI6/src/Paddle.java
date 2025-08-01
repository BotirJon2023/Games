import java.awt.*;

class Paddle {
    private final int x;
    private int y;
    private final int width = 10;
    private final int height = 100;
    private final int speed = 5;
    private final boolean isPlayer;

    public Paddle(int x, int y, boolean isPlayer) {
        this.x = x;
        this.y = y;
        this.isPlayer = isPlayer;
    }

    public void moveUp() {
        if (y - speed >= 0) y -= speed;
    }

    public void moveDown() {
        if (y + height + speed <= 480) y += speed;
    }

    public void followBall(Ball ball) {
        if (!isPlayer) {
            if (ball.getY() < y + height / 2 && y > 0) {
                y -= speed;
            } else if (ball.getY() > y + height / 2 && y + height < 480) {
                y += speed;
            }
        }
    }

    public void reset() {
        y = 200;
    }

    public void draw(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, width, height);
    }

    public Rectangle getRect() {
        return new Rectangle(x, y, width, height);
    }

    public int getY() {
        return y;
    }
}
