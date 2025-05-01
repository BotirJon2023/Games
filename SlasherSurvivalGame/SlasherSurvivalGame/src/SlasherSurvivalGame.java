import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Random;

public class SlasherSurvivalGame extends JFrame {
    private GamePanel gamePanel;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    public SlasherSurvivalGame() {
        setTitle("Slasher Survival Game");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);
        addKeyListener(gamePanel);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SlasherSurvivalGame());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int FPS = 60;
    private static final int PLAYER_SIZE = 32;
    private static final int ENEMY_SIZE = 32;
    private static final int ITEM_SIZE = 16;
    private static final int SPRITE_FRAMES = 4;
    private static final int ANIMATION_SPEED = 10;

    private Timer timer;
    private Player player;
    private Enemy enemy;
    private ArrayList<Item> items;
    private boolean gameOver;
    private int score;
    private long startTime;
    private Random random;
    private BufferedImage[] playerSprites;
    private BufferedImage[] enemySprites;
    private BufferedImage itemSprite;
    private int frameCount;
    private Font gameFont;
    private boolean[] keys;

    public GamePanel() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setFocusable(true);

        timer = new Timer(1000 / FPS, this);
        player = new Player(400, 300);
        enemy = new Enemy(100, 100);
        items = new ArrayList<>();
        random = new Random();
        keys = new boolean[256];
        gameOver = false;
        score = 0;
        startTime = System.currentTimeMillis();
        frameCount = 0;

        loadAssets();
        spawnItems();
        timer.start();
    }

    private void loadAssets() {
        try {
            // Load player sprites (4 frames for animation)
            playerSprites = new BufferedImage[SPRITE_FRAMES];
            for (int i = 0; i < SPRITE_FRAMES; i++) {
                playerSprites[i] = createPlaceholderSprite(Color.BLUE, PLAYER_SIZE, i);
            }

            // Load enemy sprites (4 frames for animation)
            enemySprites = new BufferedImage[SPRITE_FRAMES];
            for (int i = 0; i < SPRITE_FRAMES; i++) {
                enemySprites[i] = createPlaceholderSprite(Color.RED, ENEMY_SIZE, i);
            }

            // Load item sprite
            itemSprite = createPlaceholderSprite(Color.YELLOW, ITEM_SIZE, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load font
        gameFont = new Font("Arial", Font.BOLD, 20);
    }

    private BufferedImage createPlaceholderSprite(Color color, int size, int frame) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, size, size);
        g.setColor(Color.BLACK);
        g.drawString("" + frame, size / 2, size / 2);
        g.dispose();
        return image;
    }

    private void spawnItems() {
        for (int i = 0; i < 5; i++) {
            int x = random.nextInt(800 - ITEM_SIZE);
            int y = random.nextInt(600 - ITEM_SIZE);
            items.add(new Item(x, y));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw player
        int playerFrame = (frameCount / ANIMATION_SPEED) % SPRITE_FRAMES;
        g2d.drawImage(playerSprites[playerFrame], player.x, player.y, PLAYER_SIZE, PLAYER_SIZE, null);

        // Draw enemy
        int enemyFrame = (frameCount / ANIMATION_SPEED) % SPRITE_FRAMES;
        g2d.drawImage(enemySprites[enemyFrame], enemy.x, enemy.y, ENEMY_SIZE, ENEMY_SIZE, null);

        // Draw items
        for (Item item : items) {
            g2d.drawImage(itemSprite, item.x, item.y, ITEM_SIZE, ITEM_SIZE, null);
        }

        // Draw UI
        g2d.setFont(gameFont);
        g2d.setColor(Color.WHITE);
        g2d.drawString("Score: " + score, 10, 30);
        long timeSurvived = (System.currentTimeMillis() - startTime) / 1000;
        g2d.drawString("Time: " + timeSurvived + "s", 10, 60);

        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, 800, 600);
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            g2d.drawString("Game Over!", 250, 300);
            g2d.setFont(gameFont);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Final Score: " + score, 320, 350);
            g2d.drawString("Time Survived: " + timeSurvived + "s", 320, 380);
            g2d.drawString("Press R to Restart", 320, 410);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            updateGame();
            frameCount++;
        }
        repaint();
    }

    private void updateGame() {
        // Update player
        player.update();

        // Update enemy
        enemy.update(player.x, player.y);

        // Check collisions
        checkCollisions();

        // Spawn new items if needed
        if (items.isEmpty()) {
            spawnItems();
        }
    }

    private void checkCollisions() {
        // Player-Enemy collision
        Rectangle playerRect = new Rectangle(player.x, player.y, PLAYER_SIZE, PLAYER_SIZE);
        Rectangle enemyRect = new Rectangle(enemy.x, enemy.y, ENEMY_SIZE, ENEMY_SIZE);
        if (playerRect.intersects(enemyRect)) {
            gameOver = true;
        }

        // Player-Item collisions
        ArrayList<Item> itemsToRemove = new ArrayList<>();
        for (Item item : items) {
            Rectangle itemRect = new Rectangle(item.x, item.y, ITEM_SIZE, ITEM_SIZE);
            if (playerRect.intersects(itemRect)) {
                itemsToRemove.add(item);
                score += 10;
            }
        }
        items.removeAll(itemsToRemove);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
        if (e.getKeyCode() == KeyEvent.VK_R && gameOver) {
            restartGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void restartGame() {
        player = new Player(400, 300);
        enemy = new Enemy(100, 100);
        items.clear();
        spawnItems();
        gameOver = false;
        score = 0;
        startTime = System.currentTimeMillis();
        frameCount = 0;
    }

    class Player {
        int x, y;
        int speed = 5;
        int vx, vy;

        Player(int x, int y) {
            this.x = x;
            this.y = y;
            vx = 0;
            vy = 0;
        }

        void update() {
            vx = 0;
            vy = 0;

            if (keys[KeyEvent.VK_W]) vy -= speed;
            if (keys[KeyEvent.VK_S]) vy += speed;
            if (keys[KeyEvent.VK_A]) vx -= speed;
            if (keys[KeyEvent.VK_D]) vx += speed;

            x += vx;
            y += vy;

            // Keep player in bounds
            x = Math.max(0, Math.min(x, 800 - PLAYER_SIZE));
            y = Math.max(0, Math.min(y, 600 - PLAYER_SIZE));
        }
    }

    class Enemy {
        int x, y;
        int speed = 2;

        Enemy(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void update(int playerX, int playerY) {
            // Chase player
            double angle = Math.atan2(playerY - y, playerX - x);
            x += speed * Math.cos(angle);
            y += speed * Math.sin(angle);

            // Keep enemy in bounds
            x = Math.max(0, Math.min(x, 800 - ENEMY_SIZE));
            y = Math.max(0, Math.min(y, 600 - ENEMY_SIZE));
        }
    }

    class Item {
        int x, y;

        Item(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}