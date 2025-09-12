import java.awt.*;

public class Opponent {
    private int x, y;
    private int direction = 1;

    public void update(Player player) {
        if (player.getX() < x) x -= 2;
        else x += 2;

        // Random attack
        if (Math.random() < 0.01) {
            lunge();
        }
    }

    public void lunge() {
        // Attack animation and collision check
    }

    public void draw(Graphics g) {
        // Draw opponent sprite
    }
}
