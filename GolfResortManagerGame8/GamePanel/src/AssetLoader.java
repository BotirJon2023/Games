import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class AssetLoader {
    public static BufferedImage loadGuestSprite() {
        try {
            return ImageIO.read(new File("assets/guest.png"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
