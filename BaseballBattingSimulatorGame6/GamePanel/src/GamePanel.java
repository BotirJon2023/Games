// GamePanel.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GamePanel extends JPanel implements Runnable, KeyListener {
    private Thread gameThread;
    private boolean running = false;
    private Batter batter;
    private Pitch pitch;
    private Ball ball;
    private Scoreboard scoreboard;

    public GamePanel() {
        setPreferredSize(new Dimension(800, 600));
        setFocusable(true);
        addKeyListener(this);
        initGame();
    }

    private void initGame() {
        batter = new Batter(350, 500);
        pitch = new Pitch();
        ball = new Ball();
        scoreboard = new Scoreboard();
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

    private void update() {
        pitch.update();
        ball.update();
        batter.update();
        checkCollision();
    }

    private void checkCollision() {
        if (pitch.isNearBatter() && batter.isSwinging()) {
            ball.hit(pitch.getSpeed());
            scoreboard.addHit();
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        batter.draw(g);
        pitch.draw(g);
        ball.draw(g);
        scoreboard.draw(g);
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            batter.swing();
        }
    }

    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}
}
