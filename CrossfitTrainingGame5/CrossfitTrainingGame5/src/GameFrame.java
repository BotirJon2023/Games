import javax.swing.JFrame;
import java.awt.Dimension;

public class GameFrame extends JFrame {

    private GamePanel gamePanel;

    public GameFrame() {
        setTitle("Crossfit Training Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false); // For simplicity, fixed size

        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(800, 600)); // Set preferred size for the panel
        add(gamePanel); // Add the game panel to the frame

        pack(); // Adjusts frame size to fit its components
        setLocationRelativeTo(null); // Center the window on the screen
    }

    public void startGame() {
        gamePanel.startGameThread();
    }

    // You might add methods here to switch panels (e.g., for menus)
    // public void showMainMenu() { ... }
    // public void showGameScreen() { ... }
}