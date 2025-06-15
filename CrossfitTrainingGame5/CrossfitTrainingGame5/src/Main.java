import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Ensure that the GUI is created and updated on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            GameFrame gameFrame = new GameFrame();
            gameFrame.setVisible(true);
            gameFrame.startGame(); // Start the game loop
        });
    }
}