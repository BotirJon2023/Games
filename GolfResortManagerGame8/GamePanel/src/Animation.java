import java.awt.image.BufferedImage;

public class Animation {
    private BufferedImage[] frames;
    private int currentFrame;
    private int delay;
    private int counter;

    public Animation(BufferedImage[] frames, int delay) {
        this.frames = frames;
        this.delay = delay;
        this.currentFrame = 0;
        this.counter = 0;
    }

    public void update() {
        counter++;
        if (counter >= delay) {
            currentFrame = (currentFrame + 1) % frames.length;
            counter = 0;
        }
    }

    public BufferedImage getCurrentFrame() {
        return frames[currentFrame];
    }
}
