import java.awt.*;

public class Player {
    private int x, y;
    private int width = 40;
    private int height = 80;
    private int dy = 0;
    private boolean isJumping = false;
    private final int groundLevel = 500;
    private int score = 0;

    public Player() {
        this.x = 100;
        this.y = groundLevel;
    }

    public void moveLeft() {
        x -= 10;
        if (x < 0) x = 0;
    }

    public void moveRight() {
        x += 10;
        if (x > 760) x = 760; // screen width - player width
    }

    public void jump() {
        if (!isJumping) {
            isJumping = true;
            dy = -18;
        }
    }

    public void update() {
        if (isJumping) {
            y += dy;
            dy += 1; // gravity
            if (y >= groundLevel) {
                y = groundLevel;
                dy = 0;
                isJumping = false;
            }
        }
    }

    public void draw(Graphics g) {
        g.setColor(Color.ORANGE);
        g.fillRect(x, y, width, height);
        g.setColor(Color.WHITE);
        g.drawString("Dunker", x + 5, y + 45);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public int getScore() {
        return score;
    }

    public void increaseScore() {
        score += 2; // each dunk adds 2 points
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
