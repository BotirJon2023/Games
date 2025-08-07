import java.util.ArrayList;
import java.util.List;

public class GameLogic {
    private List<Player> players;
    private int currentPlayerIndex;
    private int dartsThrownThisTurn;
    private final int STARTING_SCORE = 501;
    private boolean gameOver;

    public GameLogic(List<Player> players) {
        this.players = players;
        for (Player p : players) {
            p.resetScore(); // Ensure all players start at 0 or 501
            p.addScore(STARTING_SCORE); // Initialize for 501
        }
        this.currentPlayerIndex = 0;
        this.dartsThrownThisTurn = 0;
        this.gameOver = false;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void processDartHit(int score) {
        if (gameOver) return;

        Player current = getCurrentPlayer();
        int newScore = current.getScore() - score;

        if (newScore < 0 || (newScore == 0 && score % 2 != 0)) { // Bust or not a double out
            System.out.println(current.getName() + " BUST! Score remains: " + current.getScore());
            // No score change for bust
        } else if (newScore == 0) { // Perfect double out
            current.addScore(-score); // Update score to 0
            System.out.println(current.getName() + " wins with a double out!");
            gameOver = true;
        } else {
            current.addScore(-score); // Deduct score
        }

        dartsThrownThisTurn++;
        if (dartsThrownThisTurn == 3 || gameOver) { // End of turn or game over
            endTurn();
        }
    }

    private void endTurn() {
        dartsThrownThisTurn = 0;
        if (!gameOver) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            System.out.println("\n--- " + getCurrentPlayer().getName() + "'s Turn (Score: " + getCurrentPlayer().getScore() + ") ---");
        }
    }

    // You would add methods for starting a new game,
    // switching game types (e.g., 501, Cricket), etc.
}