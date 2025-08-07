import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.Random;

public class Dartboard {
    private int centerX;
    private int centerY;
    private int radius; // Outer radius of the dartboard

    private Random random;

    public Dartboard(int centerX, int centerY, int radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.random = new Random();
    }

    public void draw(Graphics2D g2d) {
        // Draw the outer ring (simplistic)
        g2d.setColor(Color.BLACK);
        g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        // Draw inner white ring
        g2d.setColor(Color.WHITE);
        int innerRadius1 = (int) (radius * 0.9);
        g2d.fillOval(centerX - innerRadius1, centerY - innerRadius1, innerRadius1 * 2, innerRadius1 * 2);

        // Draw inner black ring
        g2d.setColor(Color.BLACK);
        int innerRadius2 = (int) (radius * 0.8);
        g2d.fillOval(centerX - innerRadius2, centerY - innerRadius2, innerRadius2 * 2, innerRadius2 * 2);

        // Draw bullseye (simplistic)
        g2d.setColor(Color.RED);
        int bullseyeRadius = (int) (radius * 0.1);
        g2d.fillOval(centerX - bullseyeRadius, centerY - bullseyeRadius, bullseyeRadius * 2, bullseyeRadius * 2);

        // In a real dartboard, you would draw the 20 segments, double/triple rings, etc.
        // This would involve calculating angles and drawing arcs/polygons.
    }

    // A very simple method to simulate a score based on a "hit"
    // In a real game, you'd pass the dart's landing coordinates (x,y)
    // and calculate which segment it hit based on distance from center and angle.
    public int calculateRandomHitScore() {
        // Simulate hitting a random score from 1 to 20 or bullseye
        int[] scores = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 25, 50};
        return scores[random.nextInt(scores.length)];
    }

    // More realistic: calculate score based on (x,y) coordinates
    public int calculateScore(double hitX, double hitY) {
        // This method would be complex!
        // 1. Calculate distance from center: sqrt((hitX - centerX)^2 + (hitY - centerY)^2)
        // 2. Determine if it's in outer ring, triple, inner ring, double, or bullseye.
        // 3. Calculate angle to determine the segment (e.g., 20, 1, 18, 4, etc.).
        // 4. Return the calculated score (e.g., 20, Triple 20 (60), Bullseye (50)).
        return calculateRandomHitScore(); // Placeholder for now
    }
}