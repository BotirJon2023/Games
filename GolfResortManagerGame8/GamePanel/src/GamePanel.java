import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel implements Runnable {
    private Thread gameThread;
    private boolean running = false;
    private ResortManager resortManager;

    public GamePanel() {
        setFocusable(true);
        resortManager = new ResortManager();
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
            try { Thread.sleep(16); } catch (InterruptedException e) {}
        }
    }

    public void update() {
        resortManager.update();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        resortManager.draw(g);
    }
}
