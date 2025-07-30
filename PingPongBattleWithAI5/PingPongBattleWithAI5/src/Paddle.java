import java.awt.*;

public class Paddle {
    private int x, y, width, height;
    private int velocity = 8;
    private boolean isAI;

    public Paddle(int x, int y, int width, int height, boolean isAI) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.isAI = isAI;
    }

    public void move(int ballY) {
        if (isAI) {
            if (ballY < y) y -= velocity;
            else if (ballY > y + height) y += velocity;
        }
    }

    public void moveUp() {
        if (!isAI) y -= velocity;
    }

    public void moveDown() {
        if (!isAI) y += velocity;
    }

    public void draw(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, width, height);
    }

    // Getters for collision
}
