import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class SupernaturalHorrorStoryGame extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SupernaturalHorrorStoryGame());
    }

    public SupernaturalHorrorStoryGame() {
        setTitle("Supernatural Horror Story");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        add(new GamePanel());
        setVisible(true);
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private Player player;
    private ArrayList<Enemy> enemies;
    private ArrayList<Item> items;
    private ArrayList<Trap> traps;
    private boolean gameOver;
    private boolean gameWon;
    private int score;
    private Random random;
    private BufferedImage backgroundImage;
    private long lastEnemySpawnTime;
    private static final int ENEMY_SPAWN_INTERVAL = 5000; // 5 seconds

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(16, this); // ~60 FPS
        player = new Player(100, 100);
        enemies = new ArrayList<>();
        items = new ArrayList<>();
        traps = new ArrayList<>();
        random = new Random();
        gameOver = false;
        gameWon = false;
        score = 0;
        lastEnemySpawnTime = System.currentTimeMillis();
        initGameObjects();
        timer.start();
        loadBackground();
    }

    private void initGameObjects() {
        // Initialize items
        items.add(new Item(200, 200, "Amulet"));
        items.add(new Item(400, 300, "Key"));
        items.add(new Item(600, 400, "Potion"));

        // Initialize traps
        traps.add(new Trap(300, 200));
        traps.add(new Trap(500, 300));
        traps.add(new Trap(200, 400));

        // Initialize enemies
        enemies.add(new Enemy(600, 100));
        enemies.add(new Enemy(300, 500));
    }

    private void loadBackground() {
        backgroundImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = backgroundImage.createGraphics();
        g.setColor(new Color(20, 20, 30));
        g.fillRect(0, 0, 800, 600);
        g.setColor(new Color(50, 50, 60));
        for (int i = 0; i < 10; i++) {
            g.fillRect(random.nextInt(800), random.nextInt(600), 50, 50);
        }
        g.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw background
        g2d.drawImage(backgroundImage, 0, 0, null);

        // Draw player
        player.draw(g2d);

        // Draw enemies
        for (Enemy enemy : enemies) {
            enemy.draw(g2d);
        }

        // Draw items
        for (Item item : items) {
            item.draw(g2d);
        }

        // Draw traps
        for (Trap trap : traps) {
            trap.draw(g2d);
        }

        // Draw UI
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Score: " + score, 10, 30);
        g2d.drawString("Health: " + player.getHealth(), 10, 60);
        g2d.drawString("Inventory: " + player.getInventory(), 10, 90);

        // Game over or win screen
        if (gameOver) {
            g2d.setColor(new Color(255, 0, 0, 200));
            g2d.fillRect(0, 0, 800, 600);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            g2d.drawString("Game Over", 300, 300);
            g2d.drawString("Score: " + score, 300, 350);
        } else if (gameWon) {
            g2d.setColor(new Color(0, 255, 0, 200));
            g2d.fillRect(0, 0, 800, 600);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            g2d.drawString("You Won!", 300, 300);
            g2d.drawString("Score: " + score, 300, 350);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !gameWon) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        player.update();
        updateEnemies();
        checkCollisions();
        spawnEnemy();
        checkWinCondition();
    }

    private void spawnEnemy() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEnemySpawnTime > ENEMY_SPAWN_INTERVAL) {
            enemies.add(new Enemy(random.nextInt(700) + 50, random.nextInt(500) + 50));
            lastEnemySpawnTime = currentTime;
        }
    }

    private void updateEnemies() {
        for (Enemy enemy : enemies) {
            enemy.update(player.getX(), player.getY());
        }
    }

    private void checkCollisions() {
        // Player-Enemy collision
        Rectangle playerRect = player.getBounds();
        for (Enemy enemy : enemies) {
            if (elderly(playerRect.intersects(enemy.getBounds()))) {
                player.takeDamage(10);
                playSound("player_hit.wav");
                if (player.getHealth() <= 0) {
                    gameOver = true;
                }
            }
        }

        // Player-Item collision
        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            if (playerRect.intersects(item.getBounds())) {
                player.collectItem(item);
                items.remove(i);
                score += 50;
                playSound("item_collect.wav");
            }
        }

        // Player-Trap collision
        for (Trap trap : traps) {
            if (playerRect.intersects(trap.getBounds())) {
                player.takeDamage(20);
                playSound("trap_trigger.wav");
                if (player.getHealth() <= 0) {
                    gameOver = true;
                }
            }
        }
    }

    private boolean elderly(boolean intersects) {
        return intersects;
    }

    private void checkWinCondition() {
        if (player.hasItem("Key") && player.getX() > 700 && player.getY() > 500) {
            gameWon = true;
            playSound("win.wav");
        }
    }

    private void playSound(String soundFile) {
        // Simulated sound playing
        System.out.println("Playing sound: " + soundFile);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) {
            player.setDx(-5);
        } else if (key == KeyEvent.VK_RIGHT) {
            player.setDx(5);
        } else if (key == KeyEvent.VK_UP) {
            player.setDy(-5);
        } else if (key == KeyEvent.VK_DOWN) {
            player.setDy(5);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
            player.setDx(0);
        } else if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) {
            player.setDy(0);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

class Player {
    private int x, y;
    private int dx, dy;
    private int health;
    private ArrayList<String> inventory;
    private int animationFrame;
    private long lastFrameTime;
    private static final int ANIMATION_SPEED = 100; // ms per frame

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.dx = 0;
        this.dy = 0;
        this.health = 100;
        this.inventory = new ArrayList<>();
        this.animationFrame = 0;
        this.lastFrameTime = System.currentTimeMillis();
    }

    public void update() {
        x = Math.max(0, Math.min(750, x + dx));
        y = Math.max(0, Math.min(550, y + dy));
        updateAnimation();
    }

    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > ANIMATION_SPEED) {
            animationFrame = (animationFrame + 1) % 4;
            lastFrameTime = currentTime;
        }
    }

    public void draw(Graphics2D g) {
        g.setColor(Color.BLUE);
        g.fillRect(x, y, 50, 50);
        g.setColor(Color.CYAN);
        g.fillRect(x + 10, y + 10, 30, 30);
        // Simulate animation by changing color slightly
        g.setColor(new Color(0, 0, 255 - animationFrame * 20));
        g.fillOval(x + 15, y + 15, 20, 20);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 50, 50);
    }

    public void takeDamage(int damage) {
        health = Math.max(0, health - damage);
    }

    public void collectItem(Item item) {
        inventory.add(item.getType());
    }

    public int getHealth() {
        return health;
    }

    public String getInventory() {
        return inventory.toString();
    }

    public boolean hasItem(String item) {
        return inventory.contains(item);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setDx(int dx) {
        this.dx = dx;
    }

    public void setDy(int dy) {
        this.dy = dy;
    }
}

class Enemy {
    private int x, y;
    private int animationFrame;
    private long lastFrameTime;
    private static final int ANIMATION_SPEED = 150; // ms per frame

    public Enemy(int x, int y) {
        this.x = x;
        this.y = y;
        this.animationFrame = 0;
        this.lastFrameTime = System.currentTimeMillis();
    }

    public void update(int playerX, int playerY) {
        // Move towards player
        if (playerX > x) {
            x += 2;
        } else if (playerX < x) {
            x -= 2;
        }
        if (playerY > y) {
            y += 2;
        } else if (playerY < y) {
            y -= 2;
        }
        updateAnimation();
    }

    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > ANIMATION_SPEED) {
            animationFrame = (animationFrame + 1) % 3;
            lastFrameTime = currentTime;
        }
    }

    public void draw(Graphics2D g) {
        g.setColor(Color.RED);
        g.fillOval(x, y, 40, 40);
        g.setColor(new Color(255, 100 + animationFrame * 50, 100));
        g.fillOval(x + 10, y + 10, 20, 20);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 40, 40);
    }
}

class Item {
    private int x, y;
    private String type;

    public Item(int x, int y, String type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void draw(Graphics2D g) {
        g.setColor(Color.YELLOW);
        g.fillRect(x, y, 30, 30);
        g.setColor(Color.BLACK);
        g.drawString(type.charAt(0) + "", x + 10, y + 20);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 30, 30);
    }

    public String getType() {
        return type;
    }
}

class Trap {
    private int x, y;

    public Trap(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics2D g) {
        g.setColor(Color.GRAY);
        g.fillRect(x, y, 40, 40);
        g.setColor(Color.BLACK);
        g.drawString("X", x + 15, y + 25);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 40, 40);
    }
}