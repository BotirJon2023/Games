import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    private Timer timer;
    private Player player;
    private ArrayList<Obstacle> obstacles;
    private ArrayList<Pickup> pickups;
    private int score;
    private boolean gameOver = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private int backgroundY = 0;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        player = new Player(WIDTH / 2 - 25, HEIGHT - 150);
        obstacles = new ArrayList<>();
        pickups = new ArrayList<>();
        timer = new Timer(20, this);
    }

    public void startGame() {
        timer.start();
        score = 0;
        gameOver = false;

        new Thread(() -> {
            Random rand = new Random();
            while (!gameOver) {
                try {
                    Thread.sleep(1200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int x = rand.nextInt(WIDTH - 60);
                obstacles.add(new Obstacle(x, -60));
                if (rand.nextBoolean()) {
                    pickups.add(new Pickup(rand.nextInt(WIDTH - 40), -40));
                }
            }
        }).start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            updateGame();
            repaint();
        }
    }

    public void updateGame() {
        backgroundY += 5;
        if (backgroundY > HEIGHT) backgroundY = 0;

        player.update(leftPressed, rightPressed);

        for (Obstacle obs : obstacles) {
            obs.move();
        }
        for (Pickup pu : pickups) {
            pu.move();
        }

        checkCollisions();

        obstacles.removeIf(obs -> obs.getY() > HEIGHT);
        pickups.removeIf(pu -> pu.getY() > HEIGHT);

        score++;
    }

    private void checkCollisions() {
        Rectangle playerRect = player.getBounds();

        Iterator<Obstacle> it = obstacles.iterator();
        while (it.hasNext()) {
            Obstacle obs = it.next();
            if (playerRect.intersects(obs.getBounds())) {
                gameOver = true;
                timer.stop();
            }
        }

        Iterator<Pickup> pit = pickups.iterator();
        while (pit.hasNext()) {
            Pickup pu = pit.next();
            if (playerRect.intersects(pu.getBounds())) {
                score += 100;
                pit.remove();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBackground(g);

        player.draw(g);

        for (Obstacle obs : obstacles) {
            obs.draw(g);
        }

        for (Pickup pu : pickups) {
            pu.draw(g);
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString("Score: " + score, 10, 30);

        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("GAME OVER", WIDTH / 2 - 130, HEIGHT / 2);
        }
    }

    private void drawBackground(Graphics g) {
        g.setColor(new Color(220, 240, 255));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(Color.GRAY);
        g.fillRect(WIDTH / 2 - 100, 0, 200, HEIGHT);

        g.setColor(Color.YELLOW);
        for (int i = backgroundY % 40; i < HEIGHT; i += 40) {
            g.fillRect(WIDTH / 2 - 5, i, 10, 20);
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = true;
    }
    @Override public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = false;
    }
    @Override public void keyTyped(KeyEvent e) {}
}
