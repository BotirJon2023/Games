import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class Scoreboard {

    private int strikes;
    private int balls;
    private int outs;
    private int hits;
    private int score;
    private int inning;

    private final int MAX_STRIKES = 3;
    private final int MAX_BALLS = 4;
    private final int MAX_OUTS = 3;

    public Scoreboard() {
        resetCounts();
        this.hits = 0;
        this.score = 0;
        this.inning = 1;
    }

    public void resetCounts() {
        this.strikes = 0;
        this.balls = 0;
    }

    public void addStrike() {
        strikes++;
        if (strikes >= MAX_STRIKES) {
            addOut();
        }
    }

    public void addBall() {
        balls++;
        if (balls >= MAX_BALLS) {
            // Logic for a walk (player gets to first base)
            resetCounts();
        }
    }

    public void addOut() {
        outs++;
        resetCounts(); // New batter comes up
        if (outs >= MAX_OUTS) {
            // Change inning
            nextInning();
        }
    }

    public void addHit() {
        this.hits++;
        // We'll need a more complex logic to determine single, double, etc.
        // For now, let's say a hit is a hit and resets the count.
        resetCounts();
    }

    public void addScore(int points) {
        this.score += points;
    }

    private void nextInning() {
        inning++;
        outs = 0; // Reset outs for the new inning
    }

    public boolean isGameOver() {
        // Simple game over condition, e.g., after 9 innings
        return inning > 9;
    }

    public void draw(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));

        // Display current counts
        g.drawString("Strikes: " + strikes, 600, 50);
        g.drawString("Balls: " + balls, 600, 80);
        g.drawString("Outs: " + outs, 600, 110);

        // Display other game stats
        g.drawString("Hits: " + hits, 600, 150);
        g.drawString("Score: " + score, 600, 180);
        g.drawString("Inning: " + inning, 600, 210);
    }
}