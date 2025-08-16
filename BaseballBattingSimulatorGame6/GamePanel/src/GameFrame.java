// GameFrame.java
import javax.swing.*;

public class GameFrame extends JFrame {
    public GameFrame() {
        setTitle("Baseball Batting Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        panel.startGame();
    }

    public static void main(String[] args) {
        new GameFrame();
    }
}
