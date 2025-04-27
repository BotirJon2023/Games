import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class DarkFantasyHorrorAdventureExpanded extends JFrame {
    private GamePanel gamePanel;
    private boolean gameOver = false;
    private boolean gameWon = false;

    public DarkFantasyHorrorAdventureExpanded() {
        setTitle("Dark Fantasy Horror Adventure");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        gamePanel.startGame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DarkFantasyHorrorAdventureExpanded());
    }

    class GamePanel extends JPanel implements ActionListener, KeyListener {
        private static final int WIDTH = 800;
        private static final int HEIGHT = 600;
        private static final int TILE_SIZE = 32;
        private static final int PLAYER_SPEED = 4;
        private static final int ENEMY_SPEED = 2;
        private static final int ANIMATION_SPEED = 150;
        private static final int MAX_HEALTH = 100;

        private Timer timer;
        private Player player;
        private ArrayList<SkeletalWarrior> enemies;
        private Relic relic;
        private ArrayList<Trap> traps;
        private ArrayList<PoisonCloud> poisonClouds;
        private ArrayList<Item> items;
        private int[][] map;
        private BufferedImage wallImage, floorImage, playerSpriteSheet, enemySpriteSheet, relicImage, trapImage, cloudImage, potionImage;
        private Clip backgroundMusic, relicSound, hitSound, potionSound, cloudSound;
        private boolean fogActive = true;
        private boolean lightsOn = true;
        private int fogCounter = 0;
        private int lightCounter = 0;
        private Random random = new Random();
        private Font gameFont;

        // Animation variables
        private int playerFrame = 0;
        private int enemyFrame = 0;
        private long lastAnimationTime = 0;

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            addKeyListener(this);

            loadResources();
            initializeMap();
            initializeGameObjects();
            timer = new Timer(16, this); // ~60 FPS
        }

        private void loadResources() {
            try {
                // Load font
                gameFont = new Font("Serif", Font.BOLD, 24);

                // Placeholder images
                wallImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = wallImage.createGraphics();
                g.setColor(new Color(60, 60, 60));
                g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, TILE_SIZE - 1, TILE_SIZE - 1);
                g.dispose();

                floorImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
                g = floorImage.createGraphics();
                g.setColor(new Color(30, 20, 10));
                g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
                g.dispose();

                playerSpriteSheet = new BufferedImage(TILE_SIZE * 4, TILE_SIZE * 4, BufferedImage.TYPE_INT_ARGB);
                g = playerSpriteSheet.createGraphics();
                g.setColor(new Color(0, 100, 200));
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        g.fillRect(j * TILE_SIZE, i * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        g.setColor(Color.WHITE);
                        g.drawString(String.valueOf(j + 1), j * TILE_SIZE + 10, i * TILE_SIZE + 20);
                    }
                }
                g.dispose();

                enemySpriteSheet = new BufferedImage(TILE_SIZE * 4, TILE_SIZE * 4, BufferedImage.TYPE_INT_ARGB);
                g = enemySpriteSheet.createGraphics();
                g.setColor(new Color(200, 200, 200));
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        g.fillRect(j * TILE_SIZE, i * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
                g.dispose();

                relicImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
                g = relicImage.createGraphics();
                g.setColor(new Color(200, 200, 0));
                g.fillOval(0, 0, TILE_SIZE, TILE_SIZE);
                g.dispose();

                trapImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
                g = trapImage.createGraphics();
                g.setColor(Color.RED);
                g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
                g.dispose();

                cloudImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
                g = cloudImage.createGraphics();
                g.setColor(new Color(0, 150, 0, 100));
                g.fillOval(0, 0, TILE_SIZE, TILE_SIZE);
                g.dispose();

                potionImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
                g = potionImage.createGraphics();
                g.setColor(new Color(0, 200, 200));
                g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
                g.dispose();

                // Placeholder sounds
                AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(new byte[0]));
                backgroundMusic = AudioSystem.getClip();
                relicSound = AudioSystem.getClip();
                hitSound = AudioSystem.getClip();
                potionSound = AudioSystem.getClip();
                cloudSound = AudioSystem.getClip();
            } catch (Exception e) {
                e.printStackTrace();
                // Log error using text block (JDK 23 compatible)
                String errorLog = """
                        Error loading resources:
                        %s
                        """.formatted(e.getMessage());
                System.err.println(errorLog);
            }
        }

        private void initializeMap() {
            map = new int[][] {
                    {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
                    {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
                    {1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,0,1},
                    {1,0,0,0,1,0,0,0,1,0,0,0,1,0,0,0,1,0,0,0,1,0,0,0,1},
                    {1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1},
                    {1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,1,0,1},
                    {1,0,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,0,1,1,1,0,1,0,1},
                    {1,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,1},
                    {1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,1,1,0,1},
                    {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
                    {1,0,1,1,1,1,1,0,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,0,1},
                    {1,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,1},
                    {1,1,1,1,1,0,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,0,1},
                    {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
                    {1,0,1,1,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1},
                    {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
                    {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
            };
        }

        private void initializeGameObjects() {
            player = new Player(2 * TILE_SIZE, 2 * TILE_SIZE, 0);
            enemies = new ArrayList<>();
            enemies.add(new SkeletalWarrior(12 * TILE_SIZE, 8 * TILE_SIZE));
            enemies.add(new SkeletalWarrior(8 * TILE_SIZE, 12 * TILE_SIZE));
            enemies.add(new SkeletalWarrior(15 * TILE_SIZE, 5 * TILE_SIZE));
            enemies.add(new SkeletalWarrior(5 * TILE_SIZE, 15 * TILE_SIZE));
            enemies.add(new SkeletalWarrior(10 * TILE_SIZE, 3 * TILE_SIZE));
            relic = new Relic(20 * TILE_SIZE, 14 * TILE_SIZE);
            traps = new ArrayList<>();
            traps.add(new Trap(10 * TILE_SIZE, 10 * TILE_SIZE));
            traps.add(new Trap(5 * TILE_SIZE, 7 * TILE_SIZE));
            traps.add(new Trap(15 * TILE_SIZE, 10 * TILE_SIZE));
            traps.add(new Trap(3 * TILE_SIZE, 12 * TILE_SIZE));
            poisonClouds = new ArrayList<>();
            poisonClouds.add(new PoisonCloud(7 * TILE_SIZE, 9 * TILE_SIZE));
            poisonClouds.add(new PoisonCloud(13 * TILE_SIZE, 6 * TILE_SIZE));
            poisonClouds.add(new PoisonCloud(9 * TILE_SIZE, 14 * TILE_SIZE));
            items = new ArrayList<>();
            items.add(new Item(8 * TILE_SIZE, 8 * TILE_SIZE, "potion"));
            items.add(new Item(12 * TILE_SIZE, 12 * TILE_SIZE, "potion"));
            items.add(new Item(6 * TILE_SIZE, 4 * TILE_SIZE, "potion"));
        }

        public void startGame() {
            try {
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            } catch (Exception e) {
                e.printStackTrace();
            }
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Apply lighting effect
            float alpha = lightsOn ? 1.0f : 0.4f;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            // Draw map
            for (int y = 0; y < map.length; y++) {
                for (int x = 0; x < map[0].length; x++) {
                    g2d.drawImage(map[y][x] == 1 ? wallImage : floorImage, x * TILE_SIZE, y * TILE_SIZE, this);
                }
            }

            // Draw traps
            for (Trap trap : traps) {
                g2d.drawImage(trapImage, trap.x, trap.y, this);
            }

            // Draw poison clouds
            for (PoisonCloud cloud : poisonClouds) {
                g2d.drawImage(cloudImage, cloud.x, cloud.y, this);
            }

            // Draw items
            for (Item item : items) {
                switch (item.type) {
                    case "potion" -> g2d.drawImage(potionImage, item.x, item.y, this);
                }
            }

            // Draw relic
            g2d.drawImage(relicImage, relic.x, relic.y, this);

            // Draw enemies
            for (SkeletalWarrior enemy : enemies) {
                int frameX = (enemyFrame % 4) * TILE_SIZE;
                int frameY = enemy.direction * TILE_SIZE;
                g2d.drawImage(enemySpriteSheet.getSubimage(frameX, frameY, TILE_SIZE, TILE_SIZE),
                        enemy.x, enemy.y, this);
            }

            // Draw player
            int frameX = (playerFrame % 4) * TILE_SIZE;
            int frameY = player.direction * TILE_SIZE;
            g2d.drawImage(playerSpriteSheet.getSubimage(frameX, frameY, TILE_SIZE, TILE_SIZE),
                    player.x, player.y, this);

            // Draw fog-of-war
            if (fogActive) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                g2d.setColor(Color.BLACK);
                for (int y = 0; y < HEIGHT; y += TILE_SIZE) {
                    for (int x = 0; x < WIDTH; x += TILE_SIZE) {
                        double distance = Math.hypot(x - player.x, y - player.y);
                        if (distance > 120) {
                            g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                        }
                    }
                }
            }

            // Draw HUD
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2d.setFont(gameFont);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Health: %d".formatted(player.health), 10, 30);
            g2d.drawString("Potions: %d".formatted(player.inventory.size()), 10, 60);
            g2d.setColor(Color.RED);
            g2d.fillRect(10, 80, player.health * 2, 20);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(10, 80, 200, 20);

            // Draw game over or win screen
            if (gameOver) {
                g2d.setColor(new Color(150, 0, 0, 200));
                g2d.fillRect(0, 0, WIDTH, HEIGHT);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                g2d.drawString("Game Over!", WIDTH / 2 - 100, HEIGHT / 2);
            } else if (gameWon) {
                g2d.setColor(new Color(0, 150, 0, 200));
                g2d.fillRect(0, 0, WIDTH, HEIGHT);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                g2d.drawString("Victory!", WIDTH / 2 - 100, HEIGHT / 2);
            }

            Toolkit.getDefaultToolkit().sync();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameOver || gameWon) return;

            // Update animation frames
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAnimationTime > ANIMATION_SPEED) {
                playerFrame = (playerFrame + 1) % 4;
                enemyFrame = (enemyFrame + 1) % 4;
                lastAnimationTime = currentTime;
            }

            // Update player
            player.update();

            // Update enemies
            for (SkeletalWarrior enemy : enemies) {
                enemy.update();
            }

            // Update poison clouds
            for (PoisonCloud cloud : poisonClouds) {
                cloud.update();
            }

            // Update environmental effects
            fogCounter++;
            if (fogCounter > 100 && random.nextInt(100) < 5) {
                fogActive = !fogActive;
                fogCounter = 0;
                if (fogActive) {
                    try {
                        hitSound.setFramePosition(0);
                        hitSound.start();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            lightCounter++;
            if (lightCounter > 80 && random.nextInt(100) < 3) {
                lightsOn = !lightsOn;
                lightCounter = 0;
                if (!lightsOn) {
                    try {
                        hitSound.setFramePosition(0);
                        hitSound.start();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            // Check collisions
            checkCollisions();

            repaint();
        }

        private void checkCollisions() {
            Rectangle playerRect = new Rectangle(player.x, player.y, TILE_SIZE, TILE_SIZE);

            // Player-Relic collision
            if (playerRect.intersects(new Rectangle(relic.x, relic.y, TILE_SIZE, TILE_SIZE))) {
                gameWon = true;
                try {
                    relicSound.setFramePosition(0);
                    relicSound.start();
                    backgroundMusic.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Player-Enemy collision
            for (SkeletalWarrior enemy : enemies) {
                if (playerRect.intersects(new Rectangle(enemy.x, enemy.y, TILE_SIZE, TILE_SIZE))) {
                    player.takeDamage(20);
                    try {
                        hitSound.setFramePosition(0);
                        hitSound.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // Player-Trap collision
            for (Trap trap : traps) {
                if (playerRect.intersects(new Rectangle(trap.x, trap.y, TILE_SIZE, TILE_SIZE))) {
                    player.takeDamage(30);
                    try {
                        hitSound.setFramePosition(0);
                        hitSound.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // Player-Poison Cloud collision
            for (PoisonCloud cloud : poisonClouds) {
                if (playerRect.intersects(new Rectangle(cloud.x, cloud.y, TILE_SIZE, TILE_SIZE))) {
                    player.takeDamage(5);
                    try {
                        cloudSound.setFramePosition(0);
                        cloudSound.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // Player-Item collision
            for (int i = items.size() - 1; i >= 0; i--) {
                Item item = items.get(i);
                if (playerRect.intersects(new Rectangle(item.x, item.y, TILE_SIZE, TILE_SIZE))) {
                    switch (item.type) {
                        case "potion" -> {
                            player.addPotion();
                            items.remove(i);
                            try {
                                potionSound.setFramePosition(0);
                                potionSound.start();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W -> {
                    player.direction = 0;
                    player.dy = -PLAYER_SPEED;
                }
                case KeyEvent.VK_S -> {
                    player.direction = 1;
                    player.dy = PLAYER_SPEED;
                }
                case KeyEvent.VK_A -> {
                    player.direction = 2;
                    player.dx = -PLAYER_SPEED;
                }
                case KeyEvent.VK_D -> {
                    player.direction = 3;
                    player.dx = PLAYER_SPEED;
                }
                case KeyEvent.VK_SPACE -> player.usePotion();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W, KeyEvent.VK_S -> player.dy = 0;
                case KeyEvent.VK_A, KeyEvent.VK_D -> player.dx = 0;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {}

        class Player {
            int x, y, dx, dy, direction, health;
            ArrayList<String> inventory;
            String id;

            public Player(int x, int y, int direction) {
                this.x = x;
                this.y = y;
                this.direction = direction;
                this.health = MAX_HEALTH;
                this.inventory = new ArrayList<>();
                this.id = UUID.randomUUID().toString();
            }

            public void update() {
                int newX = x + dx;
                int newY = y + dy;

                if (!isCollision(newX, newY)) {
                    x = newX;
                    y = newY;
                }
            }

            public void takeDamage(int damage) {
                health -= damage;
                if (health <= 0) {
                    health = 0;
                    gameOver = true;
                    try {
                        backgroundMusic.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void addPotion() {
                inventory.add("potion");
            }

            public void usePotion() {
                if (inventory.contains("potion") && health < MAX_HEALTH) {
                    inventory.remove("potion");
                    health = Math.min(health + 50, MAX_HEALTH);
                    try {
                        potionSound.setFramePosition(0);
                        potionSound.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            private boolean isCollision(int x, int y) {
                int mapX = x / TILE_SIZE;
                int mapY = y / TILE_SIZE;
                return mapX < 0 || mapX >= map[0].length || mapY < 0 || mapY >= map.length || map[mapY][mapX] == 1;
            }
        }

        class SkeletalWarrior {
            int x, y, direction;
            long lastMoveTime;
            String id;

            public SkeletalWarrior(int x, int y) {
                this.x = x;
                this.y = y;
                this.direction = random.nextInt(4);
                this.lastMoveTime = System.currentTimeMillis();
                this.id = UUID.randomUUID().toString();
            }

            public void update() {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMoveTime < 600) return;

                int dx = player.x - x;
                int dy = player.y - y;
                int newX = x;
                int newY = y;

                if (Math.hypot(dx, dy) < 150 && random.nextInt(100) < 70) {
                    if (Math.abs(dx) > Math.abs(dy)) {
                        newX += dx > 0 ? ENEMY_SPEED : -ENEMY_SPEED;
                        direction = dx > 0 ? 3 : 2;
                    } else {
                        newY += dy > 0 ? ENEMY_SPEED : -ENEMY_SPEED;
                        direction = dy > 0 ? 1 : 0;
                    }
                } else {
                    direction = random.nextInt(4);
                    switch (direction) {
                        case 0 -> newY -= ENEMY_SPEED;
                        case 1 -> newY += ENEMY_SPEED;
                        case 2 -> newX -= ENEMY_SPEED;
                        case 3 -> newX += ENEMY_SPEED;
                    }
                }

                if (!isCollision(newX, newY)) {
                    x = newX;
                    y = newY;
                }

                lastMoveTime = currentTime;
            }

            private boolean isCollision(int x, int y) {
                int mapX = x / TILE_SIZE;
                int mapY = y / TILE_SIZE;
                return mapX < 0 || mapX >= map[0].length || mapY < 0 || mapY >= map.length || map[mapY][mapX] == 1;
            }
        }

        class Relic {
            int x, y;
            String id;

            public Relic(int x, int y) {
                this.x = x;
                this.y = y;
                this.id = UUID.randomUUID().toString();
            }
        }

        class Trap {
            int x, y;
            String id;

            public Trap(int x, int y) {
                this.x = x;
                this.y = y;
                this.id = UUID.randomUUID().toString();
            }
        }

        class PoisonCloud {
            int x, y;
            long lastMoveTime;
            String id;

            public PoisonCloud(int x, int y) {
                this.x = x;
                this.y = y;
                this.lastMoveTime = System.currentTimeMillis();
                this.id = UUID.randomUUID().toString();
            }

            public void update() {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMoveTime < 1000) return;

                int newX = x + (random.nextInt(3) - 1) * TILE_SIZE;
                int newY = y + (random.nextInt(3) - 1) * TILE_SIZE;

                if (!isCollision(newX, newY)) {
                    x = newX;
                    y = newY;
                }

                lastMoveTime = currentTime;
            }

            private boolean isCollision(int x, int y) {
                int mapX = x / TILE_SIZE;
                int mapY = y / TILE_SIZE;
                return mapX < 0 || mapX >= map[0].length || mapY < 0 || mapY >= map.length || map[mapY][mapX] == 1;
            }
        }

        class Item {
            int x, y;
            String type, id;

            public Item(int x, int y, String type) {
                this.x = x;
                this.y = y;
                this.type = type;
                this.id = UUID.randomUUID().toString();
            }
        }
    }
}