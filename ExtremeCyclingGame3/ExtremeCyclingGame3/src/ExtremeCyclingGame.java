import javax.swing.*;

public class ExtremeCyclingGame {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Extreme Cycling Game");
        GamePanel panel = new GamePanel();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        panel.startGame();
    }
}
