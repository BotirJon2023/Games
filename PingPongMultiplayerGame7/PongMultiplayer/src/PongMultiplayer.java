import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.util.*;
import java.util.List;

/*
  PongMultiplayer.java

  Java Swing 2D Ping-Pong (Pong) game
  - 2 Players (Left: W/S, Right: Up/Down)
  - Optional AI opponents (toggle in Settings or Menu)
  - Animated ball and paddles with spin and friction
  - Particle effects and motion trails
  - Power-ups (enlarge, shrink, speed, slow, multiball, ghost)
  - Menu, Pause, Settings, Help screens
  - Countdown before serve, win screen
  - FPS counter and basic theme support
  - Window resize support with letterbox scaling

  Compile: javac PongMultiplayer.java
  Run:     java PongMultiplayer
*/

public class PongMultiplayer {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PongMultiplayer().start();
        });
    }

    private void start() {
        JFrame frame = new JFrame("Ping-Pong Multiplayer (Java Swing)");
        GamePanel panel = new GamePanel();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.requestFocusInWindow();
        panel.startGameThread();
    }
