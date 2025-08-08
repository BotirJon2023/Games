public class Match {
    private Player player1, player2;

    public Match(Player p1, Player p2) {
        player1 = p1;
        player2 = p2;
    }

    public Player play() {
        while (!player1.hasWon() && !player2.hasWon()) {
            player1.throwDart();
            player2.throwDart();
        }
        return player1.hasWon() ? player1 : player2;
    }
}
