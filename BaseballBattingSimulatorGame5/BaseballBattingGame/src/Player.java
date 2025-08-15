import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;

public class Player {

    private int x, y; // The player's position on the screen
    private Image playerSprite; // The image for the player (batter)
    private final int WIDTH = 60;
    private final int HEIGHT = 100;

    public Player(int startX, int startY) {
        this.x = startX;
        this.y = startY;

        // Load the player's sprite. Replace with your actual image path.
        // It's good practice to use an AssetsManager class for this, but for simplicity, we do it here.
        playerSprite = Toolkit.getDefaultToolkit().getImage("assets/batter_stance.png");
    }

    public void draw(Graphics g) {
        // Draw the player sprite at the current position.
        // The image is scaled to the defined width and height.
        g.drawImage(playerSprite, x, y, WIDTH, HEIGHT, null);
    }

    // Getters for the player's position
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return WIDTH;
    }

    public int getHeight() {
        return HEIGHT;
    }
}