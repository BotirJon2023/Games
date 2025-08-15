import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;

public class Bat {

    private int x, y; // Position of the top-left corner of the bat
    private int playerX, playerY; // The player's position to follow
    private Image batSprite; // The image for the bat
    private Rectangle hitbox; // The area of the bat used for collision detection

    private final int READY_OFFSET_X = 50;
    private final int READY_OFFSET_Y = -10;

    // Animation variables
    private boolean isSwinging = false;
    private final int SWING_DURATION = 15; // Number of frames for the swing
    private int swingFrame = 0;
    private final int SWING_ARC_X = 150;
    private final int SWING_ARC_Y = -150;
    private final int SWING_ARC_WIDTH = 250;
    private final int SWING_ARC_HEIGHT = 150;

    public Bat(int playerX, int playerY) {
        this.playerX = playerX;
        this.playerY = playerY;

        // Set the initial position relative to the player
        this.x = playerX + READY_OFFSET_X;
        this.y = playerY + READY_OFFSET_Y;

        // Load the bat sprite
        batSprite = Toolkit.getDefaultToolkit().getImage("assets/baseball_bat.png");

        // Initialize the hitbox
        this.hitbox = new Rectangle(x, y, 100, 20); // Initial size, will be updated
    }

    public void update(int newPlayerX, int newPlayerY) {
        this.playerX = newPlayerX;
        this.playerY = newPlayerY;

        if (isSwinging) {
            // Logic for the swing animation
            swingFrame++;

            // A simple linear interpolation for the animation
            double progress = (double) swingFrame / SWING_DURATION;
            x = playerX + (int)(READY_OFFSET_X + progress * SWING_ARC_X);
            y = playerY + (int)(READY_OFFSET_Y + progress * SWING_ARC_Y);

            // Update the hitbox position with the bat's new position
            hitbox.setLocation(x, y);

            if (swingFrame >= SWING_DURATION) {
                isSwinging = false; // The swing animation is complete
            }
        } else {
            // Bat is in the ready position, follow the player
            x = playerX + READY_OFFSET_X;
            y = playerY + READY_OFFSET_Y;
        }
    }

    public void startSwing() {
        if (!isSwinging) {
            isSwinging = true;
            swingFrame = 0; // Reset the animation
        }
    }

    public boolean isSwingFinished() {
        return swingFrame >= SWING_DURATION;
    }

    public Rectangle getHitbox() {
        return hitbox;
    }

    public boolean checkCollision(Ball ball) {
        // Use the ball's hitbox (assuming a simple circle or rectangle for the ball)
        Rectangle ballHitbox = new Rectangle(ball.getX(), ball.getY(), 20, 20);
        return this.hitbox.intersects(ballHitbox);
    }

    public void draw(Graphics g) {
        // Draw the bat. We can also add rotation here for a more realistic swing.
        g.drawImage(batSprite, x, y, 100, 20, null);

        // (Optional) Draw the hitbox for debugging
        // g.setColor(Color.RED);
        // g.drawRect(hitbox.x, hitbox.y, hitbox.width, hitbox.height);
    }

    public int getX() {
        return 0;
    }
}