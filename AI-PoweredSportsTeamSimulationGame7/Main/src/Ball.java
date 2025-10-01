import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import java.util.Random;

public class Ball {

    private double x, y;
    private double vx, vy;
    private Circle shape;

    public Ball(double x, double y, double radius, Color color) {
        this.x = x;
        this.y = y;
        this.shape = new Circle(radius, color);
        Random rand = new Random();
        this.vx = rand.nextDouble() * 200 - 100; // Random horizontal velocity
        this.vy = rand.nextDouble() * 200 - 100; // Random vertical velocity
    }

    public void move(double elapsedTime) {
        x += vx * elapsedTime;
        y += vy * elapsedTime;

        // Simple boundary checking
        if (x < 0 || x > 800) vx *= -1;
        if (y < 0 || y > 600) vy *= -1;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Shape getShape() {
        return shape;
    }

    public void reverseX() {
        vx *= -1;
    }
}