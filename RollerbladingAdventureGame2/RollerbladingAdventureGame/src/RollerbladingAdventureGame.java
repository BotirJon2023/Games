import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class RollerbladingAdventureGame extends JPanel implements KeyListener {

    private int playerX = 250;
    private int playerY = 400;
    private int playerWidth = 50;
    private int playerHeight = 50;
    private int obstacleWidth = 30;
    private int obstacleHeight = 30;
    private int score = 0;

    private ArrayList<Obstacle> obstacles = new ArrayList<>();
    private Random random = new Random();
    private Timer timer;
    private boolean gameOver = false;

    public RollerbladingAdventureGame() {
        addKeyListener(this);
        setFocusable(true);
        setPreferredSize(new Dimension(600, 500));

        timer = new Timer(1000 / 60, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                update();
                repaint();
            }
        });
        timer.start();

        spawnObstacle();
    }

    private void update() {
        if (gameOver) {
            timer.stop();
            return;
        }

        for (Obstacle obstacle : obstacles) {
            obstacle.update();
            if (obstacle.getBounds().intersects(getPlayerBounds())) {
                gameOver = true;
            }
            if (obstacle.y > getHeight()) {
                obstacles.remove(obstacle);
                score++;
                spawnObstacle();
                break;
            }
        }
    }

    private void spawnObstacle() {
        int x = random.nextInt(getWidth() - obstacleWidth);
        obstacles.add(new Obstacle(x, 0));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.WHITE);
        g.fillRect(playerX, playerY, playerWidth, playerHeight);

        for (Obstacle obstacle : obstacles) {
            g.fillRect(obstacle.x, obstacle.y, obstacleWidth, obstacleHeight);
        }

        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Score: " + score, 10, 30);

        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.drawString("Game Over!", getWidth() / 2 - 120, getHeight() / 2);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Press R to restart", getWidth() / 2 - 80, getHeight() / 2 + 50);
        }
    }

    private Rectangle getPlayerBounds() {
        return new Rectangle(playerX, playerY, playerWidth, playerHeight);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT && playerX > 0) {
            playerX -= 10;
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT && playerX < getWidth() - playerWidth) {
            playerX += 10;
        }
        if (e.getKeyCode() == KeyEvent.VK_R && gameOver) {
            gameOver = false;
            playerX = 250;
            score = 0;
            obstacles.clear();
            spawnObstacle();
            timer.start();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    private class Obstacle {
        int x;
        int y;
        int speed = 5;

        public Obstacle(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void update() {
            y += speed;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, obstacleWidth, obstacleHeight);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Rollerblading Adventure Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new RollerbladingAdventureGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}