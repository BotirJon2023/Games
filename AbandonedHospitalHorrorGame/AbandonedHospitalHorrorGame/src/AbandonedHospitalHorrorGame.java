
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Random;

public class AbandonedHospitalHorrorGame extends JFrame {
    public AbandonedHospitalHorrorGame() {
        add(new GamePanel());
        setTitle("Abandoned Hospital Horror Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AbandonedHospitalHorrorGame());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int TILE_SIZE = 32;
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    private static final int GRID_WIDTH = SCREEN_WIDTH / TILE_SIZE;
    private static final int GRID_HEIGHT = SCREEN_HEIGHT / TILE_SIZE;
    private static final int PLAYER_SPEED = 4;
    private static final int ANIMATION_SPEED = 100; // ms per frame

    private Player player;
    private ArrayList<GameObject> objects;
    private ArrayList<JumpScare> jumpScares;
    private boolean[] keys;
    private Timer timer;
    private BufferedImage[] playerSprites;
    private BufferedImage wallImage, floorImage, keyImage;
    private int[][] map;
    private boolean gameOver;
    private int keysCollected;
    private boolean flickering;
    private long flickerStartTime;
    private Random random;
    private Clip backgroundMusic;
    private Clip jumpScareSound;

    public GamePanel() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        keys = new boolean[256];
        player = new Player(100, 100);
        objects = new ArrayList<>();
        jumpScares = new ArrayList<>();
        timer = new Timer(16, this); // ~60 FPS
        gameOver = false;
        keysCollected = 0;
        random = new Random();
        loadAssets();
        initializeMap();
        initializeObjects();
        timer.start();
    }

    private void loadAssets() {
        try {
            // Placeholder for sprite loading (in a real game, load actual images)
            playerSprites = new BufferedImage[4];
            for (int i = 0; i < 4; i++) {
                playerSprites[i] = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = playerSprites[i].createGraphics();
                g.setColor(i % 2 == 0 ? Color.BLUE : Color.CYAN);
                g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
                g.setColor(Color.BLACK);
                g.drawString("P" + i, 10, 20);
                g.dispose();
            }
            wallImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = wallImage.createGraphics();
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, TILE_SIZE - 1, TILE_SIZE - 1);
            g.dispose();

            floorImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            g = floorImage.createGraphics();
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
            g.setColor(Color.LIGHT_GRAY);
            g.drawRect(0, 0, TILE_SIZE - 1, TILE_SIZE - 1);
            g.dispose();

            keyImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            g = keyImage.createGraphics();
            g.setColor(Color.YELLOW);
            g.fillOval(8, 8, 16, 16);
            g.setColor(Color.BLACK);
            g.drawOval(8, 8, 16, 16);
            g.dispose();

            // Load sounds
            File bgMusicFile = new File("background.wav");
            if (bgMusicFile.exists()) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(bgMusicFile);
                backgroundMusic = AudioSystem.getClip();
                backgroundMusic.open(audioIn);
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            }

            File scareSoundFile = new File("jumpscare.wav");
            if (scareSoundFile.exists()) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(scareSoundFile);
                jumpScareSound = AudioSystem.getClip();
                jumpScareSound.open(audioIn);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeMap() {
        map = new int[GRID_HEIGHT][GRID_WIDTH];
        // 0 = floor, 1 = wall
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                if (y == 0 || y == GRID_HEIGHT - 1 || x == 0 || x == GRID_WIDTH - 1) {
                    map[y][x] = 1; // Walls on borders
                } else {
                    map[y][x] = 0; // Floor
                }
            }
        }
        // Add inner walls to create rooms and corridors
        for (int i = 3; i < GRID_WIDTH - 3; i++) {
            map[5][i] = 1; // Horizontal wall
            map[10][i] = 1;
        }
        for (int i = 3; i < GRID_HEIGHT - 3; i++) {
            map[i][5] = 1; // Vertical wall
            map[i][15] = 1;
        }
        // Create openings
        map[5][10] = 0;
        map[10][10] = 0;
        map[8][5] = 0;
        map[8][15] = 0;
    }

    private void initializeObjects() {
        // Add keys
        objects.add(new GameObject(200, 200, "key"));
        objects.add(new GameObject(400, 300, "key"));
        objects.add(new GameObject(600, 400, "key"));
        // Add jump scares
        jumpScares.add(new JumpScare(300, 300));
        jumpScares.add(new JumpScare(500, 200));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Apply flickering effect
        if (flickering) {
            float alpha = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 100.0));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        // Draw map
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                int drawX = x * TILE_SIZE;
                int drawY = y * TILE_SIZE;
                if (map[y][x] == 1) {
                    g2d.drawImage(wallImage, drawX, drawY, null);
                } else {
                    g2d.drawImage(floorImage, drawX, drawY, null);
                }
            }
        }

        // Draw objects
        for (GameObject obj : objects) {
            if (obj.type.equals("key")) {
                g2d.drawImage(keyImage, obj.x, obj.y, null);
            }
        }

        // Draw player
        BufferedImage currentSprite = playerSprites[player.animationFrame];
        g2d.drawImage(currentSprite, player.x, player.y, null);

        // Draw jump scares if active
        for (JumpScare scare : jumpScares) {
            if (scare.active) {
                g2d.setColor(Color.RED);
                g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 50));
                g2d.drawString("JUMP SCARE!", 300, 300);
            }
        }

        // Draw HUD
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Keys: " + keysCollected + "/3", 10, 20);

        // Draw game over or win screen
        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            g2d.drawString("GAME OVER", 300, 300);
        } else if (keysCollected >= 3) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            g2d.setColor(Color.GREEN);
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            g2d.drawString("YOU ESCAPED!", 300, 300);
        }

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver || keysCollected >= 3) {
            return;
        }

        updatePlayer();
        checkCollisions();
        updateJumpScares();
        updateFlickering();
        player.updateAnimation();
        repaint();
    }

    private void updatePlayer() {
        int newX = player.x;
        int newY = player.y;

        if (keys[KeyEvent.VK_W]) newY -= PLAYER_SPEED;
        if (keys[KeyEvent.VK_S]) newY += PLAYER_SPEED;
        if (keys[KeyEvent.VK_A]) newX -= PLAYER_SPEED;
        if (keys[KeyEvent.VK_D]) newX += PLAYER_SPEED;

        // Check collision with walls
        int gridX = newX / TILE_SIZE;
        int gridY = newY / TILE_SIZE;
        if (gridX >= 0 && gridX < GRID_WIDTH && gridY >= 0 && gridY < GRID_HEIGHT) {
            if (map[gridY][gridX] == 0) {
                player.x = newX;
                player.y = newY;
            }
        }

        // Update animation if moving
        if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_S] || keys[KeyEvent.VK_A] || keys[KeyEvent.VK_D]) {
            player.isMoving = true;
        } else {
            player.isMoving = false;
            player.animationFrame = 0;
        }
    }

    private void checkCollisions() {
        Rectangle playerRect = new Rectangle(player.x, player.y, TILE_SIZE, TILE_SIZE);
        ArrayList<GameObject> toRemove = new ArrayList<>();
        for (GameObject obj : objects) {
            Rectangle objRect = new Rectangle(obj.x, obj.y, TILE_SIZE, TILE_SIZE);
            if (playerRect.intersects(objRect) && obj.type.equals("key")) {
                toRemove.add(obj);
                keysCollected++;
                startFlickering();
            }
        }
        objects.removeAll(toRemove);

        for (JumpScare scare : jumpScares) {
            Rectangle scareRect = new Rectangle(scare.x, scare.y, TILE_SIZE, TILE_SIZE);
            if (playerRect.intersects(scareRect) && !scare.triggered) {
                scare.trigger();
                if (jumpScareSound != null) {
                    jumpScareSound.setFramePosition(0);
                    jumpScareSound.start();
                }
                startFlickering();
            }
        }
    }

    private void updateJumpScares() {
        for (JumpScare scare : jumpScares) {
            if (scare.active && System.currentTimeMillis() - scare.triggerTime > 1000) {
                scare.active = false;
            }
        }
    }

    private void startFlickering() {
        flickering = true;
        flickerStartTime = System.currentTimeMillis();
    }

    private void updateFlickering() {
        if (flickering && System.currentTimeMillis() - flickerStartTime > 3000) {
            flickering = false;
        }
        if (!flickering && random.nextInt(500) == 0) {
            startFlickering();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            gameOver = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

class Player {
    int x, y;
    int animationFrame;
    boolean isMoving;
    long lastAnimationUpdate;
    private static final int ANIMATION_SPEED = 100; // ms per frame

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.animationFrame = 0;
        this.isMoving = false;
        this.lastAnimationUpdate = System.currentTimeMillis();
    }

    public void updateAnimation() {
        if (isMoving && System.currentTimeMillis() - lastAnimationUpdate > ANIMATION_SPEED) {
            animationFrame = (animationFrame + 1) % 4;
            lastAnimationUpdate = System.currentTimeMillis();
        }
    }
}

class GameObject {
    int x, y;
    String type;

    public GameObject(int x, int y, String type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
}

class JumpScare {
    int x, y;
    boolean triggered;
    boolean active;
    long triggerTime;

    public JumpScare(int x, int y) {
        this.x = x;
        this.y = y;
        this.triggered = false;
        this.active = false;
    }

    public void trigger() {
        if (!triggered) {
            triggered = true;
            active = true;
            triggerTime = System.currentTimeMillis();
        }
    }
}
