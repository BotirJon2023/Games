import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class ZombieQuestHorrorGame extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ZombieQuestHorrorGame game = new ZombieQuestHorrorGame();
            game.setVisible(true);
        });
    }

    public ZombieQuestHorrorGame() {
        setTitle("Zombie Quest Horror Game");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        setResizable(false);
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private Player player;
    private ArrayList<Zombie> zombies;
    private ArrayList<Resource> resources;
    private ArrayList<Barricade> barricades;
    private Random random;
    private int score;
    private int objectivesCompleted;
    private boolean gameOver;
    private long lastSpawnTime;
    private long lastResourceTime;
    private long spawnInterval = 5000;
    private long resourceInterval = 8000;
    private BufferedImage backgroundImage;
    private BufferedImage fogImage;
    private int cameraX, cameraY;
    private boolean[][] exploredMap;

    public GamePanel() {
        setFocusable(true);
        setDoubleBuffered(true);
        addKeyListener(this);
        timer = new Timer(16, this);
        player = new Player(2000, 2000);
        zombies = new ArrayList<>();
        resources = new ArrayList<>();
        barricades = new ArrayList<>();
        random = new Random();
        score = 0;
        objectivesCompleted = 0;
        gameOver = false;
        lastSpawnTime = System.currentTimeMillis();
        lastResourceTime = System.currentTimeMillis();
        backgroundImage = createBackgroundImage();
        fogImage = createFogImage();
        cameraX = 0;
        cameraY = 0;
        exploredMap = new boolean[400][400];
        initializeBarricades();
        timer.start();
    }

    private BufferedImage createBackgroundImage() {
        BufferedImage img = new BufferedImage(4000, 4000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(20, 20, 20));
        g.fillRect(0, 0, 4000, 4000);
        g.setColor(new Color(50, 50, 50));
        for (int i = 0; i < 200; i++) {
            int x = random.nextInt(4000);
            int y = random.nextInt(4000);
            g.fillOval(x, y, 40, 40);
        }
        g.dispose();
        return img;
    }

    private BufferedImage createFogImage() {
        BufferedImage img = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, 1000, 700);
        g.dispose();
        return img;
    }

    private void initializeBarricades() {
        barricades.add(new Barricade(1800, 1900, 100, 100));
        barricades.add(new Barricade(2200, 2100, 120, 80));
        barricades.add(new Barricade(2000, 2300, 80, 120));
        barricades.add(new Barricade(2400, 1800, 150, 90));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        int drawX = -cameraX;
        int drawY = -cameraY;
        g2d.drawImage(backgroundImage, drawX, drawY, null);

        for (Barricade barricade : barricades) {
            barricade.draw(g2d, cameraX, cameraY);
        }
        player.draw(g2d, cameraX, cameraY);
        for (Zombie zombie : zombies) {
            zombie.draw(g2d, cameraX, cameraY);
        }
        for (Resource resource : resources) {
            resource.draw(g2d, cameraX, cameraY);
        }

        drawFogOfWar(g2d);

        if (!gameOver) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Score: " + score, 10, 30);
            g2d.drawString("Objectives: " + objectivesCompleted + "/5", 10, 60);
            g2d.drawString("Health: " + player.getHealth(), 10, 90);
            g2d.drawString("Ammo: " + player.getAmmo(), 10, 120);
        } else {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 60));
            g2d.drawString("Game Over", 350, 350);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            g2d.drawString("Score: " + score, 400, 400);
        }
    }

    private void drawFogOfWar(Graphics2D g2d) {
        int playerGridX = (int) (player.getX() / 10);
        int playerGridY = (int) (player.getY() / 10);
        for (int i = Math.max(0, playerGridX - 50); i < Math.min(400, playerGridX + 50); i++) {
            for (int j = Math.max(0, playerGridY - 50); j < Math.min(400, playerGridY + 50); j++) {
                exploredMap[i][j] = true;
            }
        }

        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 70; j++) {
                int worldX = cameraX + i * 10;
                int worldY = cameraY + j * 10;
                int gridX = worldX / 10;
                int gridY = worldY / 10;
                if (gridX >= 0 && gridX < 400 && gridY >= 0 && gridY < 400 && !exploredMap[gridX][gridY]) {
                    g2d.drawImage(fogImage, i * 10, j * 10, 10, 10, null);
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            updateGame();
            spawnZombies();
            spawnResources();
            updateCamera();
        }
        repaint();
    }

    private void updateCamera() {
        cameraX = (int) (player.getX() - 500);
        cameraY = (int) (player.getY() - 350);
        cameraX = Math.max(0, Math.min(cameraX, 3000));
        cameraY = Math.max(0, Math.min(cameraY, 3300));
    }

    private void updateGame() {
        player.update(barricades);
        for (Zombie zombie : new ArrayList<>(zombies)) {
            zombie.update(player, barricades);
            if (zombie.collidesWith(player)) {
                player.takeDamage(15);
                zombies.remove(zombie);
                if (player.getHealth() <= 0) {
                    gameOver = true;
                }
            }
        }
        for (Resource resource : new ArrayList<>(resources)) {
            if (resource.collidesWith(player)) {
                resource.applyEffect(player);
                resources.remove(resource);
                score += 20;
                if (resource.getType() == 2) {
                    objectivesCompleted++;
                    if (objectivesCompleted >= 5) {
                        gameOver = true;
                    }
                }
            }
        }
    }

    private void spawnZombies() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSpawnTime > spawnInterval) {
            int zombiesToSpawn = objectivesCompleted + random.nextInt(3) + 1;
            for (int i = 0; i < zombiesToSpawn; i++) {
                int x = random.nextInt(4000);
                int y = random.nextInt(4000);
                if (Math.abs(x - player.getX()) > 500 || Math.abs(y - player.getY()) > 500) {
                    zombies.add(new Zombie(x, y, random.nextInt(2)));
                }
            }
            lastSpawnTime = currentTime;
        }
    }

    private void spawnResources() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResourceTime > resourceInterval) {
            int x = random.nextInt(3800) + 100;
            int y = random.nextInt(3800) + 100;
            resources.add(new Resource(x, y, random.nextInt(3)));
            lastResourceTime = currentTime;
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
}

class Player {
    private double x, y;
    private int health;
    private int ammo;
    private boolean up, down, left, right, shoot;
    private double speed = 5.0;
    private int animationFrame;
    private long lastAnimationTime;
    private long lastShootTime;
    private long shootDelay = 300;
    private BufferedImage[][] sprites;

    public Player(double x, double y) {
        this.x = x;
        this.y = y;
        this.health = 100;
        this.ammo = 50;
        animationFrame = 0;
        lastAnimationTime = System.currentTimeMillis();
        lastShootTime = System.currentTimeMillis();
        sprites = new BufferedImage[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                sprites[i][j] = createPlayerSprite(i, j);
            }
        }
    }

    private BufferedImage createPlayerSprite(int direction, int frame) {
        BufferedImage sprite = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sprite.createGraphics();
        g.setColor(Color.BLUE);
        g.fillOval(10, 10, 30, 30);
        g.setColor(Color.BLACK);
        g.fillRect(20 + frame * 2, 5, 10, 5);
        g.fillRect(20 + frame * 2, 40, 10, 5);
        g.rotate(Math.toRadians(90 * direction), 25, 25);
        g.setColor(Color.GRAY);
        g.fillRect(30, 20, 15, 10);
        g.dispose();
        return sprite;
    }

    public void update(ArrayList<Barricade> barricades) {
        double newX = x;
        double newY = y;
        if (up && y > 0) newY -= speed;
        if (down && y < 3950) newY += speed;
        if (left && x > 0) newX -= speed;
        if (right && x < 3950) newX += speed;

        Rectangle newBounds = new Rectangle((int) newX, (int) newY, 50, 50);
        boolean canMove = true;
        for (Barricade barricade : barricades) {
            if (newBounds.intersects(barricade.getBounds())) {
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

        if (shoot && ammo > 0 && currentTime - lastShootTime > shootDelay) {
            shootZombie();
            ammo--;
            lastShootTime = currentTime;
        }
    }

    private void shootZombie() {
        // Placeholder for shooting logic; can be expanded with bullet mechanics
    }

    public void draw(Graphics2D g, int cameraX, int cameraY) {
        int direction = up ? 0 : down ? 2 : left ? 3 : right ? 1 : 0;
        g.drawImage(sprites[direction][animationFrame], (int) x - cameraX, (int) y - cameraY, null);
    }

    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: up = true; break;
            case KeyEvent.VK_S: down = true; break;
            case KeyEvent.VK_A: left = true; break;
            case KeyEvent.VK_D: right = true; break;
            case KeyEvent.VK_SPACE: shoot = true; break;
        }
    }

    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: up = false; break;
            case KeyEvent.VK_S: down = false; break;
            case KeyEvent.VK_A: left = false; break;
            case KeyEvent.VK_D: right = false; break;
            case KeyEvent.VK_SPACE: shoot = false; break;
        }
    }

    public void takeDamage(int damage) {
        health -= damage;
    }

    public int getHealth() {
        return health;
    }

    public int getAmmo() {
        return ammo;
    }

    public void addResource(int type) {
        if (type == 0) {
            health = Math.min(100, health + 25);
        } else if (type == 1) {
            ammo += 20;
        }
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, 50, 50);
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
        this.health = type == 0 ? 60 : 120;
        this.speed = type == 0 ? 2.0 : 1.5;
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
        BufferedImage sprite = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sprite.createGraphics();
        g.setColor(type == 0 ? Color.GREEN : Color.DARK_GRAY);
        g.fillOval(10, 10, 30, 30);
        g.setColor(Color.RED);
        g.fillOval(15 + frame * 2, 15, 8, 8);
        g.rotate(Math.toRadians(90 * direction), 25, 25);
        g.setColor(Color.BLACK);
        g.fillRect(15, 5, 10, 5);
        g.fillRect(15, 40, 10, 5);
        g.dispose();
        return sprite;
    }

    public void update(Player player, ArrayList<Barricade> barricades) {
        double dx = player.getBounds().x + 25 - x;
        double dy = player.getBounds().y + 25 - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double newX = x;
        double newY = y;
        if (distance > 0 && distance < 500) {
            newX += (dx / distance) * speed;
            newY += (dy / distance) * speed;
        }

        Rectangle newBounds = new Rectangle((int) newX, (int) newY, 50, 50);
        boolean canMove = true;
        for (Barricade barricade : barricades) {
            if (newBounds.intersects(barricade.getBounds())) {
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

    public void draw(Graphics2D g, int cameraX, int cameraY) {
        int direction = (int) (Math.atan2(y - 2000, x - 2000) / (Math.PI / 2)) % 4;
        g.drawImage(sprites[direction][animationFrame], (int) x - cameraX, (int) y - cameraY, null);
    }

    public boolean collidesWith(Player player) {
        return getBounds().intersects(player.getBounds());
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, 50, 50);
    }
}

class Resource {
    private double x, y;
    private int type;

    public Resource(double x, double y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void draw(Graphics2D g, int cameraX, int cameraY) {
        g.setColor(type == 0 ? Color.RED : type == 1 ? Color.YELLOW : Color.CYAN);
        g.fillRect((int) x - cameraX, (int) y - cameraY, 30, 30);
        g.setColor(Color.BLACK);
        g.drawString(type == 0 ? "H" : type == 1 ? "A" : "O", (int) x - cameraX + 10, (int) y - cameraY + 20);
    }

    public boolean collidesWith(Player player) {
        return getBounds().intersects(player.getBounds());
    }

    public void applyEffect(Player player) {
        player.addResource(type);
    }

    public int getType() {
        return type;
    }

    private Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, 30, 30);
    }
}

class Barricade {
    private int x, y, width, height;
    private BufferedImage image;

    public Barricade(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.image = createBarricadeImage();
    }

    private BufferedImage createBarricadeImage() {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(139, 69, 19)); // Brown color for barricade
        g.fillRect(0, 0, width, height);
        g.setColor(Color.DARK_GRAY);
        for (int i = 0; i < 10; i++) {
            g.fillRect(new Random().nextInt(width), new Random().nextInt(height), 10, 10);
        }
        g.dispose();
        return img;
    }

    public void draw(Graphics2D g, int cameraX, int cameraY) {
        g.drawImage(image, x - cameraX, y - cameraY, null);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}