public class AILogic {

    private Player player;
    private Ball ball;

    public AILogic(Player player, Ball ball) {
        this.player = player;
        this.ball = ball;
    }

    public void makeDecision(double elapsedTime) {
        // This is a simple, reactive AI
        // It's not "intelligent," it just follows the ball
        double ballY = ball.getY();
        double playerY = player.getY();

        if (ballY > playerY) {
            // Move player down to follow the ball
            player.setY(playerY + 150 * elapsedTime);
        } else if (ballY < playerY) {
            // Move player up to follow the ball
            player.setY(playerY - 150 * elapsedTime);
        }

        // In a real game, this is where you would implement more complex logic:
        // - Look at the state of the game (score, time left)
        // - Consider other players' positions
        // - Use a pre-defined strategy or a learning algorithm
    }
}