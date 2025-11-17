import java.awt.image.BufferedImage;

public class Animation {
    private BufferedImage[] frames;
    private int currentFrame;
    private long lastTime, delay;

    public Animation(BufferedImage[] frames, long delay) {
        this.frames = frames;
        this.delay = delay;
        this.lastTime = System.currentTimeMillis();
    }

    public void update() {
        if(System.currentTimeMillis() - lastTime > delay) {
            currentFrame = (currentFrame + 1) % frames.length;
            lastTime = System.currentTimeMillis();
        }
    }
    public BufferedImage getCurrentFrame() {
        return frames[currentFrame];
    }
}
