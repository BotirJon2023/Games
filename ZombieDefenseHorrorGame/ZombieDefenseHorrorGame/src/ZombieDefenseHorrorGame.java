import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class ZombieDefenseHorrorGame extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ZombieDefenseHorrorGame game = new ZombieDefenseHorrorGame();
            game.setVisible(true);
        });
    }

    public ZombieDefenseHorrorGame() {
        setTitle("Zombie Defense Horror Game");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        setResizable(false);
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener {
    private Timer timer;
    private Player player;
    private ArrayList<Zombie> zombies;
    private ArrayList<Bullet> bullets;
    private Random random;
    private int score;
    private int wave;
    private boolean gameOver;
    private long lastSpawnTime;
    private long spawnInterval = 3000;
    private BufferedImage backgroundImage;
    private int backgroundX;

    public GamePanel() {
        setFocusable(true);
        setDoubleBuffered(true);
        addKeyListener(this);
        addMouseListener(this);
        timer = new Timer(16, this);
        player = new Player(400, 300);
        zombies = new ArrayList<>();
        bullets = new ArrayList<>();
        random = new Random();
        score = 0;
        wave = 1;
        gameOver = false;
        lastSpawnTime = System.currentTimeMillis();
        backgroundImage = createBackgroundImage();
        backgroundX = 0;
        timer.start();
    }

    private BufferedImage createBackgroundImage() {
        BufferedImage img = new BufferedImage(1600, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(50, 50, 50));
        g.fillRect(0, 0, 1600, 600);
        g.setColor(new Color(80, 80, 80));
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(1600);
            int y = random.nextInt(600);
            g.fillOval(x, y, 20, 20);
        }
        g.dispose();
        return img;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.drawImage(backgroundImage, backgroundX, 0, null);
        g2d.drawImage(backgroundImage, backgroundX + 1600, 0, null);

        if (!gameOver) {
            player.draw(g2d);
            for (Zombie zombie : zombies) {
                zombie.draw(g2d);
            }
            for (Bullet bullet : bullets) {
                bullet.draw(g2d);
            }

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Score: " + score, 10, 30);
            g2d.drawString("Wave: " + wave, 10, 60);
        } else {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            g2d.drawString("Game Over", 300, 300);
            g2d.drawString("Score: " + score, 300, 350);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            updateGame();
            spawnZombies();
        }
        backgroundX -= 1;
        if (backgroundX <= -1600) {
            backgroundX = 0;
        }
        repaint();
    }

    private void updateGame() {
        player.update();
        for (Zombie zombie : new ArrayList<>(zombies)) {
            zombie.update(player);
            if (zombie.collidesWith(player)) {
                gameOver = true;
            }
        }
        for (Bullet bullet : new ArrayList<>(bullets)) {
            bullet.update();
            if (bullet.isOutOfBounds()) {
                bullets.remove(bullet);
            } else {
                for (Zombie zombie : new ArrayList<>(zombies)) {
                    if (bullet.collidesWith(zombie)) {
                        zombies.remove(zombie);
                        bullets.remove(bullet);
                        score += 10;
                        break;
                    }
                }
            }
        }
    }

    private void spawnZombies() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSpawnTime > spawnInterval) {
            int zombiesToSpawn = wave + random.nextInt(3);
            for (int i = 0; i < zombiesToSpawn; i++) {
                int side = random.nextInt(4);
                int x = 0, y = 0;
                switch (side) {
                    case 0: x = -50; y = random.nextInt(600); break;
                    case 1: x = 850; y = random.nextInt(600); break;
                    case 2: x = random.nextInt(800); y = -50; break;
                    case 3: x = random.nextInt(800); y = 650; break;
                }
                zombies.add(new Zombie(x, y));
            }
            lastSpawnTime = currentTime;
            if (zombies.isEmpty()) {
                wave++;
                spawnInterval = Math.max(1000, spawnInterval - 200);
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        player.keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        player.keyReleased(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {
        if (!gameOver) {
            player.shoot(e.getX(), e.getY(), bullets);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
}

class Player {
    private double x, y;
    private boolean up, down, left, right;
    private double speed = 5.0;
    private int animationFrame;
    private long lastAnimationTime;
    private BufferedImage[] sprites;

    public Player(double x, double y) {
        this.x = x;
        this.y = y;
        animationFrame = 0;
        lastAnimationTime = System.currentTimeMillis();
        sprites = new BufferedImage[4];
        for (int i = 0; i < 4; i++) {
            sprites[i] = createPlayerSprite(i);
        }
    }

    private BufferedImage createPlayerSprite(int index) {
        BufferedImage sprite = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sprite.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(10, 10, 30, 30);
        g.setColor(Color.BLACK);
        g.fillOval(15 + index * 2, 15, 10, 10);
        g.dispose();
        return sprite;
    }

    public void update() {
        if (up && y > 0) y -= speed;
        if (down && y < 550) y += speed;
        if (left && x > 0) x -= speed;
        if (right && x < 750) x += speed;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnimationTime > 200) {
            animationFrame = (animationFrame + 1) % 4;
            lastAnimationTime = currentTime;
        }
    }

    public void draw(Graphics2D g) {
        g.drawImage(sprites[animationFrame], (int) x, (int) y, null);
    }

    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: up = true; break;
            case KeyEvent.VK_S: down = true; break;
            case KeyEvent.VK_A: left = true; break;
            case KeyEvent.VK_D: right = true; break;
        }
    }

    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: up = false; break;
            case KeyEvent.VK_S: down = false; break;
            case KeyEvent.VK_A: left = false; break;
            case KeyEvent.VK_D: right = false; break;
        }
    }

    public void shoot(int mouseX, int mouseY, ArrayList<Bullet> bullets) {
        double dx = mouseX - (x + 25);
        double dy = mouseY - (y + 25);
        double angle = Math.atan2(dy, dx);
        bullets.add(new Bullet(x + 25, y + 25, angle));
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, 50, 50);
    }
}

class Zombie {
    private double x, y;
    private double speed;
    private int animationFrame;
    private long lastAnimationTime;
    private BufferedImage[] sprites;

    public Zombie(double x, double y) {
        this.x = x;
        this.y = y;
        this.speed = 2.0 + new Random().nextDouble();
        animationFrame = 0;
        lastAnimationTime = System.currentTimeMillis();
        sprites = new BufferedImage[4];
        for (int i = 0; i < 4; i++) {
            sprites[i] = createZombieSprite(i);
        }
    }

    private BufferedImage createZombieSprite(int index) {
        BufferedImage sprite = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sprite.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(10, 10, 30, 30);
        g.setColor(Color.RED);
        g.fillOval(15 + index * 2, 15, 10, 10);
        g.dispose();
        return sprite;
    }

    public void update(Player player) {
        double dx = player.getBounds().x + 25 - x;
        double dy = player.getBounds().y + 25 - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > 0) {
            x += (dx / distance) * speed;
            y += (dy / distance) * speed;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnimationTime > 300) {
            animationFrame = (animationFrame + 1) % 4;
            lastAnimationTime = currentTime;
        }
    }

    public void draw(Graphics2D g) {
        g.drawImage(sprites[animationFrame], (int) x, (int) y, null);
    }

    public boolean collidesWith(Player player) {
        return getBounds().intersects(player.getBounds());
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, 50, 50);
    }
}

class Bullet {
    private double x, y;
    private double vx, vy;
    private double speed = 10.0;

    public Bullet(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
    }

    public void update() {
        x += vx;
        y += vy;
    }

    public void draw(Graphics2D g) {
        g.setColor(Color.YELLOW);
        g.fillOval((int) x - 5, (int) y - 5, 10, 10);
    }

    public boolean isOutOfBounds() {
        return x < 0 || x > 800 || y < 0 || y > 600;
    }

    public boolean collidesWith(Zombie zombie) {
        return getBounds().intersects(zombie.getBounds());
    }

    private Rectangle getBounds() {
        return new Rectangle((int) x - 5, (int) y - 5, 10, 10);
    }
}