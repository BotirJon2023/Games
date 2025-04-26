import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class DarkFantasyHorrorAdventure extends JPanel implements Runnable, KeyListener {
    public static final float WIDTH = 1280;
    public static final float HEIGHT = 720;
    // Game Constants

    static final int FPS = 60;
    private static final long TARGET_TIME = 1000 / FPS;

    // Game State Management
    private enum GameState { MAIN_MENU, PLAYING, PAUSED, GAME_OVER }
    private GameState currentState = GameState.MAIN_MENU;

    // Player Properties
    private Player player;
    private boolean[] keys = new boolean[256];

    // Animation System
    private AnimationManager animationManager;

    // Game World
    private List<GameObject> gameObjects = new ArrayList<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();

    // Rendering
    private BufferedImage buffer;
    private Graphics2D bufferGraphics;

    // Game Resources
    private Font gameFont;
    private Image backgroundImage;

    // Game Variables
    private int score = 0;
    private int playerHealth = 100;
    private boolean gameRunning = true;

    public DarkFantasyHorrorAdventure() {
        setPreferredSize(new Dimension((int) WIDTH, (int) HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        // Initialize game resources
        initResources();

        // Initialize game objects
        initGameObjects();

        // Set up double buffering
        buffer = new BufferedImage((int) WIDTH, (int) HEIGHT, BufferedImage.TYPE_INT_ARGB);
        bufferGraphics = buffer.createGraphics();

        // Start game thread
        new Thread(this).start();
    }

    private void initResources() {
        try {
            // Load fonts (placeholder - in real game would load from file)
            gameFont = new Font("Garamond", Font.BOLD, 24);

            // Load images (placeholder - in real game would load from file)
            backgroundImage = new BufferedImage((int) WIDTH, (int) HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) backgroundImage.getGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, (int) WIDTH, (int) HEIGHT);
            g.setColor(new Color(30, 10, 40));
            for (int i = 0; i < 1000; i++) {
                g.fillRect((int)(Math.random() * WIDTH), (int)(Math.random() * HEIGHT), 2, 2);
            }
            g.dispose();

            // Initialize animation manager
            animationManager = new AnimationManager();
            setupAnimations();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void setupAnimations() {
        // Player animations
        Animation playerIdle = new Animation("Player Idle", 4, 0.2f, loadDummyFrames(4));
        Animation playerRun = new Animation("Player Run", 6, 0.15f, loadDummyFrames(6));
        Animation playerAttack = new Animation("Player Attack", 8, 0.1f, loadDummyFrames(8));

        animationManager.addAnimation("player_idle", playerIdle);
        animationManager.addAnimation("player_run", playerRun);
        animationManager.addAnimation("player_attack", playerAttack);

        // Enemy animations
        Animation enemyIdle = new Animation("Enemy Idle", 4, 0.3f, loadDummyFrames(4));
        Animation enemyChase = new Animation("Enemy Chase", 6, 0.2f, loadDummyFrames(6));

        animationManager.addAnimation("enemy_idle", enemyIdle);
        animationManager.addAnimation("enemy_chase", enemyChase);
    }

    private BufferedImage[] loadDummyFrames(int count) {
        BufferedImage[] frames = new BufferedImage[count];
        for (int i = 0; i < count; i++) {
            frames[i] = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frames[i].createGraphics();

            // Create placeholder frames (in real game would load from sprite sheets)
            g.setColor(new Color(50 + i * 10, 30, 70));
            g.fillRect(0, 0, 64, 64);
            g.setColor(Color.WHITE);
            g.drawString("Frame " + (i + 1), 10, 20);

            g.dispose();
        }
        return frames;
    }

    private void initGameObjects() {
        // Create player
        player = new Player(WIDTH / 2, HEIGHT / 2, 64, 64);
        gameObjects.add(player);

        // Create some enemies
        for (int i = 0; i < 5; i++) {
            Enemy enemy = new Enemy(
                    (int)(Math.random() * WIDTH),
                    (int)(Math.random() * HEIGHT),
                    64, 64
            );
            enemies.add(enemy);
            gameObjects.add(enemy);
        }

        // Create some environmental objects
        for (int i = 0; i < 10; i++) {
            GameObject obj = new GameObject(
                    (int)(Math.random() * WIDTH),
                    (int)(Math.random() * HEIGHT),
                    32, 32,
                    "object_" + i
            );
            gameObjects.add(obj);
        }
    }

    @Override
    public void run() {
        long startTime;
        long elapsed;
        long wait;

        // Main game loop
        while (gameRunning) {
            startTime = System.nanoTime();

            update();
            render();

            elapsed = System.nanoTime() - startTime;
            wait = TARGET_TIME - elapsed / 1000000;

            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void update() {
        switch (currentState) {
            case MAIN_MENU:
                updateMainMenu();
                break;
            case PLAYING:
                updateGame();
                break;
            case PAUSED:
                updatePaused();
                break;
            case GAME_OVER:
                updateGameOver();
                break;
        }
    }

    private void updateMainMenu() {
        // Handle menu navigation
    }

    private void updateGame() {
        // Update player
        player.update(keys);

        // Update enemies
        for (Enemy enemy : enemies) {
            enemy.update(player);

            // Simple collision detection
            if (player.getBounds().intersects(enemy.getBounds())) {
                playerHealth -= 1;
                if (playerHealth <= 0) {
                    currentState = GameState.GAME_OVER;
                }
            }
        }

        // Update particles
        Iterator<Particle> particleIter = particles.iterator();
        while (particleIter.hasNext()) {
            Particle p = particleIter.next();
            p.update();
            if (p.isExpired()) {
                particleIter.remove();
            }
        }

        // Spawn new enemies occasionally
        if (Math.random() < 0.01 && enemies.size() < 10) {
            Enemy enemy = new Enemy(
                    (int)(Math.random() * WIDTH),
                    -50,
                    64, 64
            );
            enemies.add(enemy);
            gameObjects.add(enemy);
        }

        // Update animations
        animationManager.update();
    }

    private void updatePaused() {
        // Handle pause menu
    }

    private void updateGameOver() {
        // Handle game over screen
    }

    private void render() {
        // Clear the buffer
        bufferGraphics.setColor(Color.BLACK);
        bufferGraphics.fillRect(0, 0, (int) WIDTH, (int) HEIGHT);

        // Draw background
        bufferGraphics.drawImage(backgroundImage, 0, 0, null);

        switch (currentState) {
            case MAIN_MENU:
                renderMainMenu();
                break;
            case PLAYING:
                renderGame();
                break;
            case PAUSED:
                renderPaused();
                break;
            case GAME_OVER:
                renderGameOver();
                break;
        }

        // Draw the buffer to the screen
        Graphics g = getGraphics();
        g.drawImage(buffer, 0, 0, null);
        g.dispose();
    }

    private void renderMainMenu() {
        bufferGraphics.setFont(gameFont.deriveFont(48f));
        bufferGraphics.setColor(new Color(14, 14, 14));
        drawCenteredString("DARK FANTASY HORROR ADVENTURE", (int) (WIDTH / 2), (int) (HEIGHT / 3), bufferGraphics);

        bufferGraphics.setFont(gameFont.deriveFont(24f));
        bufferGraphics.setColor(Color.WHITE);
        drawCenteredString("Press ENTER to begin your nightmare", (int) (WIDTH / 2), (int) (HEIGHT / 2), bufferGraphics);
    }

    private void renderGame() {
        // Draw game objects
        for (GameObject obj : gameObjects) {
            obj.render(bufferGraphics, animationManager);
        }

        // Draw particles
        for (Particle p : particles) {
            p.render(bufferGraphics);
        }

        // Draw HUD
        renderHUD();
    }

    private void renderHUD() {
        bufferGraphics.setFont(gameFont);
        bufferGraphics.setColor(Color.RED);
        bufferGraphics.drawString("Health: " + playerHealth, 20, 40);

        bufferGraphics.setColor(Color.WHITE);
        bufferGraphics.drawString("Score: " + score, 20, 70);

        // Draw mini-map or other UI elements
    }

    private void renderPaused() {
        renderGame();

        // Darken the game
        bufferGraphics.setColor(new Color(0, 0, 0, 150));
        bufferGraphics.fillRect(0, 0, (int) WIDTH, (int) HEIGHT);

        bufferGraphics.setFont(gameFont.deriveFont(48f));
        bufferGraphics.setColor(Color.WHITE);
        drawCenteredString("PAUSED", (int) (WIDTH / 2), (int) (HEIGHT / 2), bufferGraphics);
    }

    private void renderGameOver() {
        bufferGraphics.setFont(gameFont.deriveFont(48f));
        bufferGraphics.setColor(new Color(180, 50, 50));
        drawCenteredString("GAME OVER", (int) (WIDTH / 2), (int) (HEIGHT / 3), bufferGraphics);

        bufferGraphics.setFont(gameFont.deriveFont(24f));
        bufferGraphics.setColor(Color.WHITE);
        drawCenteredString("Final Score: " + score, (int) (WIDTH / 2), (int) (HEIGHT / 2), bufferGraphics);
        drawCenteredString("Press ENTER to return to menu", (int) (WIDTH / 2), (int) (HEIGHT / 2 + 50), bufferGraphics);
    }

    private void drawCenteredString(String text, int x, int y, Graphics2D g) {
        FontMetrics fm = g.getFontMetrics();
        int textX = x - fm.stringWidth(text) / 2;
        int textY = y - fm.getHeight() / 2 + fm.getAscent();
        g.drawString(text, textX, textY);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() < keys.length) {
            keys[e.getKeyCode()] = true;
        }

        // State transition keys
        switch (currentState) {
            case MAIN_MENU:
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    currentState = GameState.PLAYING;
                }
                break;
            case PLAYING:
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    currentState = GameState.PAUSED;
                }
                break;
            case PAUSED:
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    currentState = GameState.PLAYING;
                }
                break;
            case GAME_OVER:
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    resetGame();
                    currentState = GameState.MAIN_MENU;
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < keys.length) {
            keys[e.getKeyCode()] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    private void resetGame() {
        playerHealth = 100;
        score = 0;
        enemies.clear();
        gameObjects.clear();
        particles.clear();
        initGameObjects();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Dark Fantasy Horror Adventure");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new DarkFantasyHorrorAdventure());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

// Game Object Classes

class GameObject {
    protected float x, y;
    protected int width, height;
    protected String id;
    protected String currentAnimation;

    public GameObject(float x, float y, int width, int height, String id) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.id = id;
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, width, height);
    }

    public void update() {
        // Base game object update logic
    }

    public void render(Graphics2D g, AnimationManager am) {
        if (currentAnimation != null && am != null) {
            BufferedImage frame = am.getFrame(currentAnimation);
            if (frame != null) {
                g.drawImage(frame, (int)x, (int)y, width, height, null);
                return;
            }
        }

        // Fallback rendering
        g.setColor(Color.GRAY);
        g.fillRect((int)x, (int)y, width, height);
    }
}

class Player extends GameObject {
    private float speed = 5.0f;
    private boolean attacking = false;
    private float attackCooldown = 0;

    public Player(float x, float y, int width, int height) {
        super(x, y, width, height, "player");
        currentAnimation = "player_idle";
    }

    public void update(boolean[] keys) {
        // Movement
        float dx = 0, dy = 0;

        if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP]) dy -= speed;
        if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN]) dy += speed;
        if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT]) dx -= speed;
        if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT]) dx += speed;

        // Normalize diagonal movement
        if (dx != 0 && dy != 0) {
            float len = (float)Math.sqrt(dx * dx + dy * dy);
            dx = dx / len * speed;
            dy = dy / len * speed;
        }

        x += dx;
        y += dy;

        // Boundary checking
        x = Math.max(0, Math.min(x, DarkFantasyHorrorAdventure.WIDTH - width));
        y = Math.max(0, Math.min(y, DarkFantasyHorrorAdventure.HEIGHT - height));

        // Update animation state
        if (dx != 0 || dy != 0) {
            currentAnimation = "player_run";
        } else {
            currentAnimation = "player_idle";
        }

        // Attack handling
        if (keys[KeyEvent.VK_SPACE] && attackCooldown <= 0) {
            attacking = true;
            currentAnimation = "player_attack";
            attackCooldown = 1.0f; // 1 second cooldown
        } else {
            attacking = false;
        }

        if (attackCooldown > 0) {
            attackCooldown -= 1.0f / DarkFantasyHorrorAdventure.FPS;
        }
    }
}

class Enemy extends GameObject {
    private float speed = 2.0f;
    private int health = 100;

    public Enemy(float x, float y, int width, int height) {
        super(x, y, width, height, "enemy_" + System.currentTimeMillis());
        currentAnimation = "enemy_idle";
    }

    public void update(Player player) {
        // Simple AI: move toward player
        float dx = player.x - x;
        float dy = player.y - y;

        // Normalize direction
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len > 0) {
            dx = dx / len * speed;
            dy = dy / len * speed;
        }

        x += dx;
        y += dy;

        // Update animation
        if (len < 300) { // If player is within 300 pixels
            currentAnimation = "enemy_chase";
        } else {
            currentAnimation = "enemy_idle";
        }
    }
}

class Particle {
    private float x, y;
    private float dx, dy;
    private Color color;
    private int size;
    private float lifetime;
    private float maxLifetime;

    public Particle(float x, float y, float dx, float dy, Color color, int size, float lifetime) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.color = color;
        this.size = size;
        this.lifetime = lifetime;
        this.maxLifetime = lifetime;
    }

    public void update() {
        x += dx;
        y += dy;
        lifetime -= 1.0f / DarkFantasyHorrorAdventure.FPS;
    }

    public boolean isExpired() {
        return lifetime <= 0;
    }

    public void render(Graphics2D g) {
        float alpha = lifetime / maxLifetime;
        g.setColor(new Color(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, alpha));
        g.fillRect((int)x, (int)y, size, size);
    }
}

// Animation System

class Animation {
    private String name;
    private BufferedImage[] frames;
    private float frameDuration;
    private int currentFrame = 0;
    private float timer = 0;
    private boolean looping = true;

    public Animation(String name, int frameCount, float frameDuration, BufferedImage[] frames) {
        this.name = name;
        this.frameDuration = frameDuration;
        this.frames = frames;
    }

    public void update(float deltaTime) {
        timer += deltaTime;
        if (timer >= frameDuration) {
            timer = 0;
            currentFrame++;
            if (currentFrame >= frames.length) {
                if (looping) {
                    currentFrame = 0;
                } else {
                    currentFrame = frames.length - 1;
                }
            }
        }
    }

    public BufferedImage getCurrentFrame() {
        return frames[currentFrame];
    }

    public void reset() {
        currentFrame = 0;
        timer = 0;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }
}

class AnimationManager {
    private Map<String, Animation> animations = new HashMap<>();
    private float globalTime = 0;

    public void addAnimation(String name, Animation animation) {
        animations.put(name, animation);
    }

    public Animation getAnimation(String name) {
        return animations.get(name);
    }

    public BufferedImage getFrame(String animationName) {
        Animation anim = animations.get(animationName);
        if (anim != null) {
            return anim.getCurrentFrame();
        }
        return null;
    }

    public void update() {
        globalTime += 1.0f / DarkFantasyHorrorAdventure.FPS;
        for (Animation anim : animations.values()) {
            anim.update(1.0f / DarkFantasyHorrorAdventure.FPS);
        }
    }
}