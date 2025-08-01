import java.awt.*;
import java.util.Random;

class Ball {
    private int x, y;
    private final int diameter = 20;
    private int xSpeed = 4, ySpeed = 4;

    public Ball(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move() {
        x += xSpeed;
        y += ySpeed;
    }

    public void checkWallCollision() {
        if (y <= 0 || y >= 480 - diameter) {
            ySpeed = -ySpeed;
        }
    }

    public void bounceFromPaddle(Paddle paddle) {
        xSpeed = -xSpeed;
        Random rand = new Random();
        ySpeed += rand.nextInt(3) - 1; // adds some variation
    }

    public void resetPosition() {
        x = 400;
        y = 250;
        xSpeed = (Math.random() > 0.5) ? 4 : -4;
        ySpeed = (Math.random() > 0.5) ? 4 : -4;
    }

    public void draw(Graphics g) {
        g.setColor(Color.YELLOW);
        g.fillOval(x, y, diameter, diameter);
    }

    public Rectangle getRect() {
        return new Rectangle(x, y, diameter, diameter);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
