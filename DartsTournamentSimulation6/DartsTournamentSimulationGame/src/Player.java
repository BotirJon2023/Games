public class Player {
    private String name;
    private int score;

    public Player(String name) {
        this.name = name;
        this.score = 501; // Starting score
    }

    public void throwDart() {
        // Simulate a dart throw (e.g., random score between 0–60)
        int points = (int)(Math.random() * 60);
        score = Math.max(score - points, 0);
    }

    public boolean hasWon() {
        return score == 0;
    }

    // Getters and setters
}
