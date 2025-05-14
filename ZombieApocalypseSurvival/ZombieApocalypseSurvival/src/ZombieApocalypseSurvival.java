import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ZombieApocalypseSurvival extends JFrame implements KeyListener, ActionListener {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PLAYER_SIZE = 30;
    private static final int ZOMBIE_SIZE = 25;
    private static final int PLAYER_SPEED = 5;
    private static final int ZOMBIE_SPEED = 2;
    private static final int ZOMBIE_SPAWN_RATE = 100; // Lower value means more frequent spawns
    private static final int BULLET_SIZE = 10;
    private static final int BULLET_SPEED = 10;

    private Player player;
    private List<Zombie> zombies;
    private List<Bullet> bullets;
    private Random random = new Random();
    private Timer gameTimer;

    public ZombieApocalypseSurvival() {
        setTitle("Zombie Apocalypse Survival");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        addKeyListener(this);
        setFocusable(true);

        player = new Player(WIDTH / 2 - PLAYER_SIZE / 2, HEIGHT / 2 - PLAYER_SIZE / 2);
        zombies = new ArrayList<>();
        bullets = new ArrayList<>();

        gameTimer = new Timer(30, this); // 30 milliseconds delay for smoother animation
        gameTimer.start();

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ZombieApocalypseSurvival::new);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateGame();
        repaint();
    }

    private void updateGame() {
        // Player movement
        if (player.isMovingLeft && player.x > 0) player.x -= PLAYER_SPEED;
        if (player.isMovingRight && player.x < WIDTH - PLAYER_SIZE) player.x += PLAYER_SPEED;
        if (player.isMovingUp && player.y > 0) player.y -= PLAYER_SPEED;
        if (player.isMovingDown && player.y < HEIGHT - PLAYER_SIZE) player.y += PLAYER_SPEED;

        // Zombie spawning
        if (random.nextInt(ZOMBIE_SPAWN_RATE) == 0) {
            spawnZombie();
        }

        // Update zombie positions
        for (Zombie zombie : zombies) {
            if (zombie.x < player.x) zombie.x += ZOMBIE_SPEED;
            if (zombie.x > player.x) zombie.x -= ZOMBIE_SPEED;
            if (zombie.y < player.y) zombie.y += ZOMBIE_SPEED;
            if (zombie.y > player.y) zombie.y -= ZOMBIE_SPEED;

            // Collision detection: Player and Zombie
            if (player.intersects(zombie)) {
                // Game Over logic (you can expand this)
                System.out.println("Game Over!");
                gameTimer.stop();
            }
        }

        // Update bullet positions
        for (Bullet bullet : new ArrayList<>(bullets)) {
            bullet.x += bullet.dx * BULLET_SPEED;
            bullet.y += bullet.dy * BULLET_SPEED;

            // Bullet out of bounds
            if (bullet.x < 0 || bullet.x > WIDTH || bullet.y < 0 || bullet.y > HEIGHT) {
                bullets.remove(bullet);
                continue;
            }

            // Collision detection: Bullet and Zombie
            for (Zombie zombie : new ArrayList<>(zombies)) {
                if (bullet.intersects(zombie)) {
                    bullets.remove(bullet);
                    zombies.remove(zombie);
                    break; // One bullet can hit one zombie at a time in this simple implementation
                }
            }
        }
    }

    private void spawnZombie() {
        int side = random.nextInt(4); // 0: top, 1: bottom, 2: left, 3: right
        int x = 0, y = 0;
        switch (side) {
            case 0: x = random.nextInt(WIDTH); y = -ZOMBIE_SIZE; break;
            case 1: x = random.nextInt(WIDTH); y = HEIGHT; break;
            case 2: x = -ZOMBIE_SIZE; y = random.nextInt(HEIGHT); break;
            case 3: x = WIDTH; y = random.nextInt(HEIGHT); break;
        }
        zombies.add(new Zombie(x, y));
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw player
        g2d.setColor(Color.BLUE);
        g2d.fillRect(player.x, player.y, PLAYER_SIZE, PLAYER_SIZE);

        // Draw zombies
        g2d.setColor(Color.RED);
        for (Zombie zombie : zombies) {
            g2d.fillRect(zombie.x, zombie.y, ZOMBIE_SIZE, ZOMBIE_SIZE);
        }

        // Draw bullets
        g2d.setColor(Color.YELLOW);
        for (Bullet bullet : bullets) {
            g2d.fillRect(bullet.x, bullet.y, BULLET_SIZE, BULLET_SIZE);
        }

        Toolkit.getDefaultToolkit().sync(); // For smoother animation on some systems
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used in this basic implementation
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) player.isMovingLeft = true;
        if (key == KeyEvent.VK_RIGHT) player.isMovingRight = true;
        if (key == KeyEvent.VK_UP) player.isMovingUp = true;
        if (key == KeyEvent.VK_DOWN) player.isMovingDown = true;
        if (key == KeyEvent.VK_SPACE) shoot();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) player.isMovingLeft = false;
        if (key == KeyEvent.VK_RIGHT) player.isMovingRight = false;
        if (key == KeyEvent.VK_UP) player.isMovingUp = false;
        if (key == KeyEvent.VK_DOWN) player.isMovingDown = false;
    }

    private void shoot() {
        // Simple shooting mechanism: bullet travels in the direction the player is currently moving
        int dx = 0, dy = 0;
        if (player.isMovingLeft) dx = -1;
        if (player.isMovingRight) dx = 1;
        if (player.isMovingUp) dy = -1;
        if (player.isMovingDown) dy = 1;

        // If the player is not moving, shoot straight ahead (you can refine this)
        if (dx == 0 && dy == 0) dx = 1; // Default to shooting right

        bullets.add(new Bullet(player.x + PLAYER_SIZE / 2 - BULLET_SIZE / 2,
                player.y + PLAYER_SIZE / 2 - BULLET_SIZE / 2, dx, dy));
    }

    // Inner classes for Player, Zombie, and Bullet
    private static class Player extends Rectangle {
        boolean isMovingLeft = false;
        boolean isMovingRight = false;
        boolean isMovingUp = false;
        boolean isMovingDown = false;

        public Player(int x, int y) {
            super(x, y, PLAYER_SIZE, PLAYER_SIZE);
        }
    }

    private static class Zombie extends Rectangle {
        public Zombie(int x, int y) {
            super(x, y, ZOMBIE_SIZE, ZOMBIE_SIZE);
        }
    }

    private static class Bullet extends Rectangle {
        int dx;
        int dy;

        public Bullet(int x, int y, int dx, int dy) {
            super(x, y, BULLET_SIZE, BULLET_SIZE);
            this.dx = dx;
            this.dy = dy;
        }
    }
}