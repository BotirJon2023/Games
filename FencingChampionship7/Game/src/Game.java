import javax.swing.*;
import java.awt.*;

public class Game extends JPanel implements Runnable {
    private JFrame frame;
    private boolean running = false;
    private Thread gameThread;

    public Game() {
        frame = new JFrame("Fencing Championship");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.setVisible(true);
        startGame();
    }

    public void startGame() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void run() {
        while (running) {
            update();
            repaint();
            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {
        // Update player, opponent, game state
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw arena, player, opponent, UI
    }

    public static void main(String[] args) {
        new Game();
    }
}
