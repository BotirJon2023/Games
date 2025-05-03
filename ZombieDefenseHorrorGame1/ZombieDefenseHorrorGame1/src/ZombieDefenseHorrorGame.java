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
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        setResizable(false);
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener, MouseMotionListener, MouseListener {
    private Timer timer;
    private Player player;
    private ArrayList<Zombie> zombies;
    private ArrayList<Bullet> bullets;
    private ArrayList<PowerUp> powerUps;
    private ArrayList<Obstacle> obstacles;
    private Random random;
    private int score;
    private int wave;
    private boolean gameOver;
    private long lastSpawnTime;
    private long lastPowerUpTime;
    private long spawnInterval = 4000;
    private long powerUpInterval = 10000;
    private BufferedImage backgroundImage;
    private int backgroundX;
    private int backgroundY;

    public GamePanel() {
        setFocusable(true);
        setDoubleBuffered(true);
        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
        timer = new Timer(16, this);
        player = new Player(500, 350);
        zombies = new ArrayList<>();
        bullets = new ArrayList<>();
        powerUps = new ArrayList<>();
        obstacles = new ArrayList<>();
        random = new Random();
        score = 0;
        wave = 1;
        gameOver = false;
        lastSpawnTime = System.currentTimeMillis();
        lastPowerUpTime = System.currentTimeMillis();
        backgroundImage = createBackgroundImage();
        backgroundX = 0;
        backgroundY = 0;
        initializeObstacles();
        timer.start();
    }

    private BufferedImage createBackgroundImage() {
        BufferedImage img = new BufferedImage(2000, 1400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(30, 30, 30));
        g.fillRect(0, 0, 2000, 1400);
        g.setColor(new Color(60, 60, 60));
        for (int i = 0; i < 100; i++) {
            int x = random.nextInt(2000);
            int y = random.nextInt(1400);
            g.fillOval(x, y, 30, 30);
        }
        g.dispose();
        return img;
    }

    private void initializeObstacles() {
        obstacles.add(new Obstacle(200, 200, 100, 100));
        obstacles.add(new Obstacle(700, 400, 120, 80));
        obstacles.add(new Obstacle(400, 500, 80, 120));
        obstacles.add(new Obstacle(600, 150, 150, 90));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.drawImage(backgroundImage, backgroundX, backgroundY, null);
        g2d.drawImage(backgroundImage, backgroundX + 2000, backgroundY, null);
        g2d.drawImage(backgroundImage, backgroundX, backgroundY + 1400, null);
        g2d.drawImage(backgroundImage, backgroundX + 2000, backgroundY + 1400, null);

        if (!gameOver) {
            for (Obstacle obstacle : obstacles) {
                obstacle.draw(g2d);
            }
            player.draw(g2d);
            for (Zombie zombie : zombies) {
                zombie.draw(g2d);
            }
            for (Bullet bullet : bullets) {
                bullet.draw(g2d);
            }
            for (PowerUp powerUp : powerUps) {
                powerUp.draw(g2d);
            }

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Score: " + score, 10, 30);
            g2d.drawString("Wave: " + wave, 10, 60);
            g2d.drawString("Health: " + player.getHealth(), 10, 90);
        } else {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 60));
            g2d.drawString("Game Over", 350, 350);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            g2d.drawString("Score: " + score, 400, 400);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            updateGame();
            spawnZombies();
            spawnPowerUps();
        }
        backgroundX -= 1;
        backgroundY -= 1;
        if (backgroundX <= -2000) backgroundX = 0;
        if (backgroundY <= -1400) backgroundY = 0;
        repaint();
    }

    private void updateGame() {
        player.update(obstacles);
        for (Zombie zombie : new ArrayList<>(zombies)) {
            zombie.update(player, obstacles);
            if (zombie.collidesWith(player)) {
                player.takeDamage(10);
                zombies.remove(zombie);
                if (player.getHealth() <= 0) {
                    gameOver = true;
                }
            }
        }
        for (Bullet bullet : new ArrayList<>(bullets)) {
            bullet.update();
            if (bullet.isOutOfBounds()) {
                bullets.remove(bullet);
            } else {
                for (Zombie zombie : new ArrayList<>(zombies)) {
                    if (bullet.collidesWith(zombie)) {
                        zombie.takeDamage(20);
                        bullets.remove(bullet);
                        if (zombie.getHealth() <= 0) {
                            zombies.remove(zombie);
                            score += 15;
                        }
                        break;
                    }
                }
            }
        }
        for (PowerUp powerUp : new ArrayList<>(powerUps)) {
            if (powerUp.collidesWith(player)) {
                powerUp.applyEffect(player);
                powerUps.remove(powerUp);
            }
        }
    }

    private void spawnZombies() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSpawnTime > spawnInterval) {
            int zombiesToSpawn = wave + random.nextInt(4);
            for (int i = 0; i < zombiesToSpawn; i++) {
                int side = random.nextInt(4);
                int x = 0, y = 0;
                switch (side) {
                    case 0: x = -60; y = random.nextInt(700); break;
                    case 1: x = 1060; y = random.nextInt(700); break;
                    case 2: x = random.nextInt(1000); y = -60; break;
                    case 3: x = random.nextInt(1000); y = 760; break;
                }
                zombies.add(new Zombie(x, y, random.nextInt(2)));
            }
            lastSpawnTime = currentTime;
            if (zombies.isEmpty()) {
                wave++;
                spawnInterval = Math.max(1500, spawnInterval - 300);
            }
        }
    }

    private void spawnPowerUps() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPowerUpTime > powerUpInterval) {
            int x = random.nextInt(900) + 50;
            int y = random.nextInt(600) + 50;
            powerUps.add(new PowerUp(x, y, random.nextInt(2)));
            lastPowerUpTime = currentTime;
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
    public void mouseMoved(MouseEvent e) {
        player.updateAim(e.getX(), e.getY());
    }

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {
        if (!gameOver) {
            player.shoot(bullets);
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
    private int health;
    private boolean up, down, left, right;
    private double speed = 4.0;
    private double angle;
    private int animationFrame;
    private long lastAnimationTime;
    private long lastShootTime;
    private long shootDelay = 200;
    private BufferedImage[][] sprites;
    private int powerUpLevel;

    public Player(double x, double y) {
        this.x = x;
        this.y = y;
        this.health = 100;
        this.angle = 0;
        animationFrame = 0;
        lastAnimationTime = System.currentTimeMillis();
        lastShootTime = System.currentTimeMillis();
        powerUpLevel = 0;
        sprites = new BufferedImage[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                sprites[i][j] = createPlayerSprite(i, j);
            }
        }
    }

    private BufferedImage createPlayerSprite(int direction, int frame) {
        BufferedImage sprite = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sprite.createGraphics();
        g.setColor(Color.BLUE);
        g.fillOval(15, 15, 30, 30);
        g.setColor(Color.BLACK);
        g.fillRect(25 + frame * 2, 10, 10, 5);
        g.fillRect(25 + frame * 2, 45, 10, 5);
        g.rotate(Math.toRadians(90 * direction), 30, 30);
        g.setColor(Color.GRAY);
        g.fillRect(35, 25, 15, 10);
        g.dispose();
        return sprite;
    }

    public void update(ArrayList<Obstacle> obstacles) {
        double newX = x;
        double newY = y;
        if (up && y > 0) newY -= speed;
        if (down && y < 640) newY += speed;
        if (left && x > 0) newX -= speed;
        if (right && x < 940) newX += speed;

        Rectangle newBounds = new Rectangle((int) newX, (int) newY, 60, 60);
        boolean canMove = true;
        for (Obstacle obstacle : obstacles) {
            if (newBounds.intersects(obstacle.getBounds())) {
                canMove = false;
                break;
            }
        }
        if (canMove) {
            x = newX;
            y = newY;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnimationTime > 150) {
            animationFrame = (animationFrame + 1) % 4;
            lastAnimationTime = currentTime;
        }
    }

    public void draw(Graphics2D g) {
        int direction = (int) (angle / 90) % 4;
        g.drawImage(sprites[direction][animationFrame], (int) x, (int) y, null);
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

    public void updateAim(int mouseX, int mouseY) {
        double dx = mouseX - (x + 30);
        double dy = mouseY - (y + 30);
        angle = Math.toDegrees(Math.atan2(dy, dx));
    }

    public void shoot(ArrayList<Bullet> bullets) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShootTime > shootDelay) {
            bullets.add(new Bullet(x + 30, y + 30, Math.toRadians(angle), powerUpLevel));
            lastShootTime = currentTime;
        }
    }

    public void takeDamage(int damage) {
        health -= damage;
    }

    public int getHealth() {
        return health;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, 60, 60);
    }

    public void applyPowerUp(int type) {
        if (type == 0) {
            health = Math.min(100, health + 20);
        } else {
            powerUpLevel = Math.min(2, powerUpLevel + 1);
            shootDelay = Math.max(100, shootDelay - 50);
        }
    }
}

class Zombie {
    private double x, y;
    private int health;
    private double speed;
    private int type;
    private int animationFrame;
    private long lastAnimationTime;
    private BufferedImage[][] sprites;

    public Zombie(double x, double y, int type) {
        this.x = x;
        this.y = y;
        this.health = type == 0 ? 50 : 100;
        this.speed = type == 0 ? 2.5 : 1.8;
        this.type = type;
        animationFrame = 0;
        lastAnimationTime = System.currentTimeMillis();
        sprites = new BufferedImage[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                sprites[i][j] = createZombieSprite(i, j, type);
            }
        }
    }

    private BufferedImage createZombieSprite(int direction, int frame, int type) {
        BufferedImage sprite = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sprite.createGraphics();
        g.setColor(type == 0 ? Color.GREEN : Color.DARK_GRAY);
        g.fillOval(15, 15, 30, 30);
        g.setColor(Color.RED);
        g.fillOval(20 + frame * 2, 20, 8, 8);
        g.rotate(Math.toRadians(90 * direction), 30, 30);
        g.setColor(Color.BLACK);
        g.fillRect(20, 10, 10, 5);
        g.fillRect(20, 45, 10, 5);
        g.dispose();
        return sprite;
    }

    public void update(Player player, ArrayList<Obstacle> obstacles) {
        double dx = player.getBounds().x + 30 - x;
        double dy = player.getBounds().y + 30 - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double newX = x;
        double newY = y;
        if (distance > 0) {
            newX += (dx / distance) * speed;
            newY += (dy / distance) * speed;
        }

        Rectangle newBounds = new Rectangle((int) newX, (int) newY, 60, 60);
        boolean canMove = true;
        for (Obstacle obstacle : obstacles) {
            if (newBounds.intersects(obstacle.getBounds())) {
                canMove = false;
                break;
            }
        }
        if (canMove) {
            x = newX;
            y = newY;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnimationTime > 200) {
            animationFrame = (animationFrame + 1) % 4;
            lastAnimationTime = currentTime;
        }
    }

    public void draw(Graphics2D g) {
        double angle = Math.atan2(y - 350, x - 500);
        int direction = (int) (angle / (Math.PI / 2)) % 4;
        g.drawImage(sprites[direction][animationFrame], (int) x, (int) y, null);
    }

    public boolean collidesWith(Player player) {
        return getBounds().intersects(player.getBounds());
    }

    public void takeDamage(int damage) {
        health -= damage;
    }

    public int getHealth() {
        return health;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, 60, 60);
    }
}

class Bullet {
    private double x, y;
    private double vx, vy;
    private double speed = 12.0;
    private int damage;

    public Bullet(double x, double y, double angle, int powerUpLevel) {
        this.x = x;
        this.y = y;
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        this.damage = 20 + powerUpLevel * 10;
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
        return x < 0 || x > 1000 || y < 0 || y > 700;
    }

    public boolean collidesWith(Zombie zombie) {
        return getBounds().intersects(zombie.getBounds());
    }

    public int getDamage() {
        return damage;
    }

    private Rectangle getBounds() {
        return new Rectangle((int) x - 5, (int) y - 5, 10, 10);
    }
}

class PowerUp {
    private double x, y;
    private int type;

    public PowerUp(double x, double y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void draw(Graphics2D g) {
        g.setColor(type == 0 ? Color.RED : Color.CYAN);
        g.fillRect((int) x, (int) y, 30, 30);
        g.setColor(Color.BLACK);
        g.drawString(type == 0 ? "H" : "P", (int) x + 10, (int) y + 20);
    }

    public boolean collidesWith(Player player) {
        return getBounds().intersects(player.getBounds());
    }

    public void applyEffect(Player player) {
        player.applyPowerUp(type);
    }

    private Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, 30, 30);
    }
}

class Obstacle {
    private int x, y, width, height;
    private BufferedImage image;

    public Obstacle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.image = createObstacleImage();
    }

    private BufferedImage createObstacleImage() {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(165, 42, 42)); // Define brown using  // Define brown using RGB values
        g.fillRect(0, 0, width, height);
        g.setColor(Color.DARK_GRAY);
        for (int i = 0; i < 10; i++) {
            g.fillRect(new Random().nextInt(width), new Random().nextInt(height), 10, 10);
        }
        g.dispose();
        return img;
    }

    public void draw(Graphics2D g) {
        g.drawImage(image, x, y, null);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}