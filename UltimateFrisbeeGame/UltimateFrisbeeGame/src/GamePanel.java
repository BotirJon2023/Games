import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private final Timer timer;
    private final int FPS = 60;
    private final Player player;
    private final ArrayList<Opponent> opponents;
    private final Frisbee frisbee;
    private boolean isGameRunning = true;
    private int score = 0;

    public GamePanel() {
        setFocusable(true);
        setBackground(Color.GREEN.darker());
        addKeyListener(this);

        player = new Player(100, 300);
        opponents = new ArrayList<>();
        frisbee = new Frisbee(player);

        generateOpponents();

        timer = new Timer(1000 / FPS, this);
        timer.start();
    }

    private void generateOpponents() {
        Random rand = new Random();
        for (int i = 0; i < 5; i++) {
            int x = 700 + rand.nextInt(200);
            int y = 50 + rand.nextInt(500);
            opponents.add(new Opponent(x, y));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawField(g);
        drawScore(g);

        player.draw(g);
        frisbee.draw(g);
        for (Opponent o : opponents) {
            o.draw(g);
        }
    }

    private void drawField(Graphics g) {
        g.setColor(Color.WHITE);
        g.drawRect(50, 50, 900, 600);
        g.drawLine(500, 50, 500, 650); // Midfield line
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Score: " + score, 60, 40);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isGameRunning) return;

        player.move();
        frisbee.update();

        for (Opponent o : opponents) {
            o.chaseFrisbee(frisbee);
        }

        checkCatch();
        repaint();
    }

    private void checkCatch() {
        for (Opponent o : opponents) {
            if (frisbee.isMoving() && frisbee.getBounds().intersects(o.getBounds())) {
                frisbee.reset();
                score = Math.max(0, score - 1);
            }
        }

        if (frisbee.getX() > 900) {
            frisbee.reset();
            score++;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        switch (k) {
            case KeyEvent.VK_LEFT -> player.setLeft(true);
            case KeyEvent.VK_RIGHT -> player.setRight(true);
            case KeyEvent.VK_UP -> player.setUp(true);
            case KeyEvent.VK_DOWN -> player.setDown(true);
            case KeyEvent.VK_SPACE -> frisbee.throwFrisbee();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        switch (k) {
            case KeyEvent.VK_LEFT -> player.setLeft(false);
            case KeyEvent.VK_RIGHT -> player.setRight(false);
            case KeyEvent.VK_UP -> player.setUp(false);
            case KeyEvent.VK_DOWN -> player.setDown(false);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}
