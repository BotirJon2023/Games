// File: PingPongBattleAI.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PingPongBattleAI {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Ping-Pong Battle with AI");
        GamePanel panel = new GamePanel();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
        frame.add(panel);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}
