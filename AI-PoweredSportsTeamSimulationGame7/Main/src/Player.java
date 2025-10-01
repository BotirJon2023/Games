import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;

public class Player {

    private double x, y;
    private Circle shape;
    private double speed = 100; // pixels per second

    public Player(double x, double y, double radius, Color color) {
        this.x = x;
        this.y = y;
        this.shape = new Circle(radius, color);
    }

    public void move(double elapsedTime) {
        // This is where a user-controlled player's movement logic would go
        // For now, it's just a placeholder
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public Shape getShape() {
        return shape;
    }
}