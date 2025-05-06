import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class HauntedForestExploration extends JFrame {
    private GamePanel gamePanel;
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int TILE_SIZE = 32;

    public HauntedForestExploration() {
        setTitle("Haunted Forest Exploration");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HauntedForestExploration());
    }

    class GamePanel extends JPanel implements ActionListener, KeyListener {
        private Timer timer;
        private Player player;
        private ArrayList<Enemy> enemies;
        private ArrayList<Item> items;
        private Map map;
        private boolean[] keys;
        private int score;
        private boolean gameOver;
        private BufferedImage backgroundImage;
        private Random random;
        private long startTime;
        private int level;
        private ArrayList<Particle> particles;

        public GamePanel() {
            setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
            setFocusable(true);
            addKeyListener(this);

            timer = new Timer(1000 / 60, this); // 60 FPS
            keys = new boolean[256];
            random = new Random();
            particles = new ArrayList<>();

            initializeGame();
        }

        private void initializeGame() {
            player = new Player(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2);
            enemies = new ArrayList<>(); // Removed erroneous 'W'
            items = new ArrayList<>();
            map = new Map();
            score = 0;
            gameOver = false;
            level = 1;
            startTime = System.currentTimeMillis();

            // Initialize background
            backgroundImage = new BufferedImage(WINDOW_WIDTH, WINDOW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = backgroundImage.createGraphics();
            g2d.setColor(new Color(20, 30, 20));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
            g2d.dispose();

            // Spawn initial enemies and items
            spawnEnemies();
            spawnItems();

            timer.start();
        }

        private void spawnEnemies() {
            for (int i = 0; i < 5 + level; i++) {
                int x = random.nextInt(WINDOW_WIDTH - 32);
                int y = random.nextInt(WINDOW_HEIGHT - 32);
                enemies.add(new Enemy(x, y));
            }
        }

        private void spawnItems() {
            for (int i = 0; i < 3 + level; i++) {
                int x = random.nextInt(WINDOW_WIDTH - 32);
                int y = random.nextInt(WINDOW_HEIGHT - 32);
                items.add(new Item(x, y));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw background
            g2d.drawImage(backgroundImage, 0, 0, null);

            // Draw map
            map.draw(g2d);

            // Draw particles
            for (Particle particle : particles) {
                particle.draw(g2d);
            }

            // Draw items
            for (Item item : items) {
                item.draw(g2d);
            }

            // Draw enemies
            for (Enemy enemy : enemies) {
                enemy.draw(g2d);
            }

            // Draw player
            player.draw(g2d);

            // Draw HUD
            drawHUD(g2d);

            if (gameOver) {
                drawGameOver(g2d);
            }
        }

        private void drawHUD(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Score: " + score, 10, 20);
            g2d.drawString("Health: " + player.getHealth(), 10, 40);
            g2d.drawString("Level: " + level, 10, 60);

            long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
            g2d.drawString("Time: " + elapsedTime + "s", 10, 80);
        }

        private void drawGameOver(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            String text = "Game Over";
            int textWidth = g2d.getFontMetrics().stringWidth(text);
            g2d.drawString(text, (WINDOW_WIDTH - textWidth) / 2, WINDOW_HEIGHT / 2 - 20);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            text = "Score: " + score;
            textWidth = g2d.getFontMetrics().stringWidth(text);
            g2d.drawString(text, (WINDOW_WIDTH - textWidth) / 2, WINDOW_HEIGHT / 2 + 20);

            text = "Press R to Restart";
            textWidth = g2d.getFontMetrics().stringWidth(text);
            g2d.drawString(text, (WINDOW_WIDTH - textWidth) / 2, WINDOW_HEIGHT / 2 + 60);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!gameOver) {
                updateGame();
            }
            repaint();
        }

        private void updateGame() {
            // Update player
            player.update(keys, map);

            // Update enemies
            for (Enemy enemy : enemies) {
                enemy.update(player);
            }

            // Update particles
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle particle = particles.get(i);
                particle.update();
                if (particle.isDead()) {
                    particles.remove(i);
                }
            }

            // Check collisions
            checkCollisions();

            // Check level up
            if (items.isEmpty()) {
                levelUp();
            }

            // Spawn particles randomly
            if (random.nextDouble() < 0.1) {
                particles.add(new Particle(
                        random.nextInt(WINDOW_WIDTH),
                        random.nextInt(WINDOW_HEIGHT)
                ));
            }
        }

        private void checkCollisions() {
            // Player-Enemy collisions
            Rectangle playerRect = player.getBounds();
            for (int i = enemies.size() - 1; i >= 0; i--) {
                Enemy enemy = enemies.get(i);
                if (playerRect.intersects(enemy.getBounds())) {
                    player.takeDamage(10);
                    enemies.remove(i);
                    // Spawn particles on hit
                    for (int j = 0; j < 5; j++) {
                        particles.add(new Particle(enemy.getX(), enemy.getY()));
                    }
                }
            }

            // Player-Item collisions
            for (int i = items.size() - 1; i >= 0; i--) {
                Item item = items.get(i);
                if (playerRect.intersects(item.getBounds())) {
                    score += 100;
                    items.remove(i);
                    // Spawn particles on collect
                    for (int j = 0; j < 5; j++) {
                        particles.add(new Particle(item.getX(), item.getY()));
                    }
                }
            }

            // Check game over
            if (player.getHealth() <= 0) {
                gameOver = true;
                timer.stop();
            }
        }

        private void levelUp() {
            level++;
            spawnEnemies();
            spawnItems();
            player.heal(20);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if (key < 256) {
                keys[key] = true;
            }
            if (gameOver && key == KeyEvent.VK_R) {
                initializeGame();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int key = e.getKeyCode();
            if (key < 256) {
                keys[key] = false;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {}
    }

    class Player {
        private int x, y;
        private int speed;
        private int health;
        private int animationFrame;
        private long lastAnimationTime;

        public Player(int x, int y) {
            this.x = x;
            this.y = y;
            this.speed = 3;
            this.health = 100;
            this.animationFrame = 0;
            this.lastAnimationTime = System.currentTimeMillis();
        }

        public void update(boolean[] keys, Map map) {
            int newX = x;
            int newY = y;

            if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]) {
                newX -= speed;
            }
            if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]) {
                newX += speed;
            }
            if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) {
                newY -= speed;
            }
            if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]) {
                newY += speed;
            }

            // Check map collision
            if (!map.isCollision(newX, newY)) {
                x = Math.max(0, Math.min(newX, WINDOW_WIDTH - 32));
                y = Math.max(0, Math.min(newY, WINDOW_HEIGHT - 32));
            }

            // Update animation
            if (System.currentTimeMillis() - lastAnimationTime > 200) {
                animationFrame = (animationFrame + 1) % 4;
                lastAnimationTime = System.currentTimeMillis();
            }
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.BLUE);
            g2d.fillRect(x, y, 32, 32);
            // Draw simple animation effect
            g2d.setColor(Color.CYAN);
            g2d.drawRect(x + animationFrame, y + animationFrame, 32 - animationFrame * 2, 32 - animationFrame * 2);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, 32, 32);
        }

        public void takeDamage(int damage) {
            health = Math.max(0, health - damage);
        }

        public void heal(int amount) {
            health = Math.min(100, health + amount);
        }

        public int getHealth() {
            return health;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    class Enemy {
        private int x, y;
        private int speed;
        private Random random;
        private int animationFrame;
        private long lastAnimationTime;

        public Enemy(int x, int y) {
            this.x = x;
            this.y = y;
            this.speed = 2;
            this.random = new Random();
            this.animationFrame = 0;
            this.lastAnimationTime = System.currentTimeMillis();
        }

        public void update(Player player) {
            // Simple AI: move towards player with some randomness
            int dx = player.getX() - x;
            int dy = player.getY() - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > 0) {
                double moveX = dx / distance * speed + (random.nextDouble() - 0.5);
                double moveY = dy / distance * speed + (random.nextDouble() - 0.5);
                x += moveX;
                y += moveY;

                x = Math.max(0, Math.min(x, WINDOW_WIDTH - 32));
                y = Math.max(0, Math.min(y, WINDOW_HEIGHT - 32));
            }

            // Update animation
            if (System.currentTimeMillis() - lastAnimationTime > 200) {
                animationFrame = (animationFrame + 1) % 4;
                lastAnimationTime = System.currentTimeMillis();
            }
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.RED);
            g2d.fillOval(x, y, 32, 32);
            // Draw animation effect
            g2d.setColor(Color.ORANGE);
            g2d.drawOval(x + animationFrame, y + animationFrame, 32 - animationFrame * 2, 32 - animationFrame * 2);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, 32, 32);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    class Item {
        private int x, y;
        private int animationFrame;
        private long lastAnimationTime;

        public Item(int x, int y) {
            this.x = x;
            this.y = y;
            this.animationFrame = 0;
            this.lastAnimationTime = System.currentTimeMillis();
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.YELLOW);
            g2d.fillRect(x, y, 16, 16);
            // Draw animation effect
            g2d.setColor(Color.WHITE);
            g2d.drawRect(x + animationFrame, y + animationFrame, 16 - animationFrame * 2, 16 - animationFrame * 2);

            // Update animation
            if (System.currentTimeMillis() - lastAnimationTime > 200) {
                animationFrame = (animationFrame + 1) % 4;
                lastAnimationTime = System.currentTimeMillis();
            }
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, 16, 16);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    class Map {
        private int[][] tiles;
        private static final int MAP_WIDTH = WINDOW_WIDTH / TILE_SIZE;
        private static final int MAP_HEIGHT = WINDOW_HEIGHT / TILE_SIZE;

        public Map() {
            tiles = new int[MAP_WIDTH][MAP_HEIGHT];
            Random random = new Random();

            // Generate simple random obstacles
            for (int x = 0; x < MAP_WIDTH; x++) {
                for (int y = 0; y < MAP_HEIGHT; y++) {
                    if (random.nextDouble() < 0.1) {
                        tiles[x][y] = 1; // Obstacle
                    }
                }
            }

            // Clear player spawn area
            tiles[MAP_WIDTH / 2][MAP_HEIGHT / 2] = 0;
        }

        public void draw(Graphics2D g2d) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                for (int y = 0; y < MAP_HEIGHT; y++) {
                    if (tiles[x][y] == 1) {
                        g2d.setColor(new Color(50, 50, 50));
                        g2d.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        g2d.setColor(Color.DARK_GRAY);
                        g2d.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }

        public boolean isCollision(int x, int y) {
            int tileX = x / TILE_SIZE;
            int tileY = y / TILE_SIZE;

            if (tileX < 0 || tileX >= MAP_WIDTH || tileY < 0 || tileY >= MAP_HEIGHT) {
                return true;
            }

            return tiles[tileX][tileY] == 1;
        }
    }

    class Particle {
        private double x, y;
        private double vx, vy;
        private int life;
        private Color color;
        private Random random;

        public Particle(int x, int y) {
            this.x = x;
            this.y = y;
            this.random = new Random();
            this.vx = (random.nextDouble() - 0.5) * 4;
            this.vy = (random.nextDouble() - 0.5) * 4;
            this.life = 30 + random.nextInt(20);
            this.color = new Color(255, 255, 255, 200);
        }

        public void update() {
            x += vx;
            y += vy;
            life--;
            int alpha = Math.max(0, 200 - (30 - life) * 10);
            color = new Color(255, 255, 255, alpha);
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.fillOval((int)x, (int)y, 4, 4);
        }

        public boolean isDead() {
            return life <= 0;
        }
    }
}