import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;

public class Ball {

    private double x, y;
    private double width, height;
    private Image image;
    private double vx, vy; // velocity x and y
    private double speed = 300; // pixels per second

    public Ball(double x, double y, double width, double height, Image image) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.image = image;
        this.vx = speed * (Math.random() > 0.5 ? 1 : -1);
        this.vy = speed * (Math.random() > 0.5 ? 1 : -1);
    }

    public void update(double deltaTime) {
        this.x += vx * deltaTime;
        this.y += vy * deltaTime;
    }

    public void keepInBounds(double screenWidth, double screenHeight) {
        // Simple bounce off walls
        if (x <= 0 || x + width >= screenWidth) {
            vx = -vx;
        }
        if (y <= 0 || y + height >= screenHeight) {
            vy = -vy;
        }
    }

    public void draw(GraphicsContext gc) {
        gc.drawImage(image, x, y, width, height);
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D(x, y, width, height);
    }

    // Getters and Setters
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public void setVelocity(double vx, double vy) { this.vx = vx; this.vy = vy; }
}