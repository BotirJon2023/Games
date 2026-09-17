import javax.swing.*;

public class ExtremeMountainBikeRacingGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Extreme Mountain Bike Racing");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            // true = two players, false = player vs computer
            boolean twoPlayers = true; // change as you like

            GamePanel panel = new GamePanel(twoPlayers);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            panel.startGame();
        });
    }
}
