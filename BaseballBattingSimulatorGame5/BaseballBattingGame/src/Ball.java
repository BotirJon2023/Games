import java.awt.Graphics;
import java.awt.Image;

public class Ball {
    private int x, y;
    private double velocityX, velocityY;
    private Image ballSprite; // The image for the ball

    public Ball() {
        // Load the ball sprite
        // ballSprite = new ImageIcon("assets/baseball.png").getImage();
        startNewPitch(); // Initial pitch
    }

    public void startNewPitch() {
        // Reset the ball's position and velocity for a new pitch
        this.x = 700; // Starting from the pitcher's mound
        this.y = 350;
        this.velocityX = -10; // Moving left towards the batter
        this.velocityY = (Math.random() - 0.5) * 5; // A little vertical variation
    }

    public void update() {
        // Move the ball
        x += velocityX;
        y += velocityY;

        // Check for "strike zone" or other conditions
        // ...
    }

    public boolean checkCollision(Bat bat) {
        // Implement collision detection logic (e.g., using AWT's Rectangle class)
        // This is a simplified check
        if (x > bat.getX() && x < bat.getX() + bat.getWidth() &&
                y > bat.getY() && y < bat.getY() + bat.getHeight()) {
            return true;
        }
        return false;
    }

    public void draw(Graphics g) {
        g.drawImage(ballSprite, x, y, 20, 20, null); // Draw the ball at its current position
    }

    // Getters and setters for x, y, etc.
    public int getX() { return x; }
    public int getY() { return y; }
}