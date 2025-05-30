import javax.swing.*;

public class GameFrame extends JFrame {
    public GameFrame() {
        setTitle("Ultimate Frisbee Game");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        GamePanel panel = new GamePanel();
        add(panel);
        panel.requestFocusInWindow();
    }
}
