import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class RugbyGameSimulation extends JPanel implements ActionListener, KeyListener {
    // [ALL PREVIOUS CODE REMAINS THE SAME - from line 1 to line 1368]
    // ... (all the classes and methods defined above remain exactly the same)

    // [CONTINUING FROM WHERE WE LEFT OFF - adding the mouse handling]

    public RugbyGameSimulation() {
        int width = 0;
        setPreferredSize(new Dimension(width, getHeight()));
        setBackground(new Color(100, 180, 100));

        // Initialize game objects
        initGame();

        // Setup timer for game loop
        Timer gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();

        // Setup input
        addKeyListener(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseClick(e);
            }
        });
        setFocusable(true);
        requestFocus();
    }

    private void initGame() {
    }

    private void handleMouseClick(MouseEvent e) {
        if (gameState == GameState.MENU) {
            int x = e.getX();
            int y = e.getY();

            if (playButton.contains(x, y)) {
                initGame();
                gameState = GameState.PLAYING;
            } else if (quitButton.contains(x, y)) {
                System.exit(0);
            }
        } else if (gameState == GameState.PLAYING) {
            // Convert screen coordinates to world coordinates
            double worldX = e.getX() + cameraX;
            double worldY = e.getY() + cameraY;

            // Find clicked player
            Player clickedPlayer = null;
            double minDist = Double.MAX_VALUE;

            for (Player player : players) {
                double dist = Math.sqrt(Math.pow(player.x - worldX, 2) +
                        Math.pow(player.y - worldY, 2));
                if (dist < 20 && dist < minDist) { // 20 is player radius
                    minDist = dist;
                    clickedPlayer = player;
                }
            }

            if (clickedPlayer != null) {
                selectedPlayer = clickedPlayer;
            }
        }
    }

    // Main method to run the game
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Rugby Game Simulation");
            RugbyGameSimulation game = new RugbyGameSimulation();

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);

            // Ensure game gets focus for keyboard input
            game.requestFocusInWindow();
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}