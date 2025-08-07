import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Darts Tournament Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false); // Often good for games

            GamePanel gamePanel = new GamePanel();
            frame.add(gamePanel);
            frame.pack(); // Sizes the frame to fit its contents
            frame.setLocationRelativeTo(null); // Center the frame
            frame.setVisible(true);

            gamePanel.startGameLoop(); // Start the game's update/render loop
        });
    }
}