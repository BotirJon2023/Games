// Scoreboard.java
import java.awt.*;

public class Scoreboard {
    private int hits;
    private int attempts;

    public Scoreboard() {
        hits = 0;
        attempts = 0;
    }

    public void addHit() {
        hits++;
        attempts++;
    }

    public void addMiss() {
        attempts++;
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Hits: " + hits, 20, 30);
        g.drawString("Attempts: " + attempts, 20, 60);
        if (attempts > 0) {
            int accuracy = (int)(((double)hits / attempts) * 100);
            g.drawString("Accuracy: " + accuracy + "%", 20, 90);
        }
    }
}
