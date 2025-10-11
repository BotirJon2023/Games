import java.awt.*;
import java.awt.image.BufferedImage;

public class Guest {
    private int x, y;
    private BufferedImage sprite;
    private int frame = 0;
    private int speed = 2;

    public Guest(int x, int y) {
        this.x = x;
        this.y = y;
        this.sprite = AssetLoader.loadGuestSprite();
    }

    public void update() {
        x += speed;
        if (x > 1000) x = 0;
        frame = (frame + 1) % 4;
    }

    public void draw(Graphics g) {
        if (sprite != null) {
            g.drawImage(sprite, x, y, null);
        } else {
            g.setColor(Color.BLUE);
            g.fillOval(x, y, 30, 30);
        }
    }
}
