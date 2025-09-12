import java.awt.*;
import java.awt.image.BufferedImage;

public class Player {
    private int x, y;
    private BufferedImage[] fencingFrames;
    private int currentFrame = 0;
    private long lastFrameTime;

    public Player() {
        Object AnimationManager = null;
        fencingFrames = AnimationManager.loadFrames("player_fencing");
        x = 100;
        y = 400;
    }

    public void update() {
        if (System.currentTimeMillis() - lastFrameTime > 100) {
            currentFrame = (currentFrame + 1) % fencingFrames.length;
            lastFrameTime = System.currentTimeMillis();
        }
    }

    public void draw(Graphics g) {
        g.drawImage(fencingFrames[currentFrame], x, y, null);
    }

    public void moveLeft() { x -= 5; }
    public void moveRight() { x += 5; }
    public void lunge() { /* attack logic */ }

    public int getX() {
    }
}
