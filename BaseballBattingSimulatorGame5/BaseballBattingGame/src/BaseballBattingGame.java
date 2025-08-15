import javax.swing.JFrame;
import java.awt.Dimension;

public class BaseballBattingGame {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Baseball Batting Simulator");
        GamePanel gamePanel = new GamePanel();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(800, 600)); // Set your desired window size
        frame.setResizable(false);
        frame.add(gamePanel);
        frame.setVisible(true);

        // Start the game loop after the frame is visible
        gamePanel.startGameThread();
    }
}