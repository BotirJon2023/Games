import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class VampireThemedHorrorGame extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VampireThemedHorrorGame game = new VampireThemedHorrorGame();
            game.setVisible(true);
        });
    }

    public VampireThemedHorrorGame() {
        setTitle("Vampire's Lair: Escape the Mansion");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        add(new GamePanel());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    public static final int TILE_SIZE = 32;
    public static final int PLAYER_SPEED = 4;
    public static final int VAMPIRE_SPEED = 2;
    public static final int ANIMATION_SPEED = 10; // Frames for animation
    private static final int ROOM_WIDTH = 25;
    private static final int ROOM_HEIGHT = 18;

    private enum GameState { PLAYING, GAME_OVER, WIN }
    private GameState gameState;
    private Timer timer;
    private Player player;
    private ArrayList<Vampire> vampires;
    private ArrayList<Key> keys;
    private Room currentRoom;
    private int score;
    private int animationFrame;
    private boolean[] keysPressed;
    private Random random;

    // Mansion layout: multiple rooms
    private Room[] rooms;
    private int currentRoomIndex;

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(1000 / 60, this); // 60 FPS
        gameState = GameState.PLAYING;
        score = 0;
        animationFrame = 0;
        keysPressed = new boolean[4]; // Up, Down, Left, Right
        random = new Random();
        initializeGame();
        timer.start();
    }

    private void initializeGame() {
        // Initialize rooms
        rooms = new Room[3];
        rooms[0] = new Room(generateRoomLayout(0));
        rooms[1] = new Room(generateRoomLayout(1));
        rooms[2] = new Room(generateRoomLayout(2));
        currentRoomIndex = 0;
        currentRoom = rooms[currentRoomIndex];

        // Initialize player
        player = new Player(100, 100);

        // Initialize vampires and keys
        vampires = new ArrayList<>();
        keys = new ArrayList<>();
        spawnVampires(3);
        spawnKeys(2);
    }

    private int[][] generateRoomLayout(int roomType) {
        int[][] layout = new int[ROOM_HEIGHT][ROOM_WIDTH];
        // 0: Floor, 1: Wall, 2: Door
        for (int j = 0; j < ROOM_HEIGHT; j++) {
            for (int i = 0; i < ROOM_WIDTH; i++) {
                if (i == 0 || i == ROOM_WIDTH - 1 || j == 0 || j == ROOM_HEIGHT - 1) {
                    layout[j][i] = 1; // Walls
                } else {
                    layout[j][i] = 0; // Floor
                }
            }
        }
        // Add internal walls and doors based on room type
        if (roomType == 0) {
            for (int i = 5; i < 10; i++) layout[5][i] = 1;
            layout[5][ROOM_WIDTH - 2] = 2; // Door to next room
        } else if (roomType == 1) {
            for (int j = 5; j < 10; j++) layout[j][10] = 1;
            layout[ROOM_HEIGHT - 2][10] = 2; // Door
        } else {
            for (int i = 15; i < 20; i++) layout[8][i] = 1;
            layout[8][ROOM_WIDTH - 2] = 2; // Final door
        }
        return layout;
    }

    private void spawnVampires(int count) {
        for (int i = 0; i < count; i++) {
            int x, y;
            do {
                x = random.nextInt(ROOM_WIDTH - 2) * TILE_SIZE + TILE_SIZE;
                y = random.nextInt(ROOM_HEIGHT - 2) * TILE_SIZE + TILE_SIZE;
            } while (isCollision(x, y, currentRoom.getLayout()));
            vampires.add(new Vampire(x, y));
        }
    }

    private void spawnKeys(int count) {
        for (int i = 0; i < count; i++) {
            int x, y;
            do {
                x = random.nextInt(ROOM_WIDTH - 2) * TILE_SIZE + TILE_SIZE;
                y = random.nextInt(ROOM_HEIGHT - 2) * TILE_SIZE + TILE_SIZE;
            } while (isCollision(x, y, currentRoom.getLayout()));
            keys.add(new Key(x, y));
        }
    }

    private boolean isCollision(int x, int y, int[][] layout) {
        int tileX = x / TILE_SIZE;
        int tileY = y / TILE_SIZE;
        if (tileX < 0 || tileX >= ROOM_WIDTH || tileY < 0 || tileY >= ROOM_HEIGHT) {
            return true;
        }
        return layout[tileY][tileX] == 1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background (dark, eerie atmosphere)
        g2d.setColor(new Color(20, 20, 30));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw room
        currentRoom.draw(g2d);

        // Draw keys
        for (Key key : keys) {
            key.draw(g2d, animationFrame);
        }

        // Draw player
        player.draw(g2d, animationFrame);

        // Draw vampires
        for (Vampire vampire : vampires) {
            vampire.draw(g2d, animationFrame);
        }

        // Draw UI
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Score: " + score, 10, 20);
        g2d.drawString("Room: " + (currentRoomIndex + 1) + "/3", 10, 40);

        // Draw game state
        if (gameState == GameState.GAME_OVER) {
            g2d.setColor(new Color(255, 0, 0, 200));
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.drawString("Game Over", getWidth() / 2 - 100, getHeight() / 2);
        } else if (gameState == GameState.WIN) {
            g2d.setColor(new Color(0, 255, 0, 200));
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.drawString("You Escaped!", getWidth() / 2 - 100, getHeight() / 2);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.PLAYING) {
            updateGame();
        }
        repaint();
        animationFrame = (animationFrame + 1) % ANIMATION_SPEED;
    }

    private void updateGame() {
        // Update player
        player.update(keysPressed, currentRoom.getLayout());

        // Update vampires
        for (Vampire vampire : vampires) {
            vampire.update(player.getX(), player.getY(), currentRoom.getLayout());
        }

        // Check key collisions
        ArrayList<Key> keysToRemove = new ArrayList<>();
        for (Key key : keys) {
            if (Math.abs(player.getX() - key.getX()) < TILE_SIZE &&
                    Math.abs(player.getY() - key.getY()) < TILE_SIZE) {
                keysToRemove.add(key);
                score += 100;
                playSound("key_pickup"); // Simulated sound
            }
        }
        keys.removeAll(keysToRemove);

        // Check vampire collisions
        for (Vampire vampire : vampires) {
            if (Math.abs(player.getX() - vampire.getX()) < TILE_SIZE &&
                    Math.abs(player.getY() - vampire.getY()) < TILE_SIZE) {
                gameState = GameState.GAME_OVER;
                playSound("game_over");
            }
        }

        // Check door collision
        int playerTileX = player.getX() / TILE_SIZE;
        int playerTileY = player.getY() / TILE_SIZE;
        if (currentRoom.getLayout()[playerTileY][playerTileX] == 2 && keys.isEmpty()) {
            currentRoomIndex++;
            if (currentRoomIndex >= rooms.length) {
                gameState = GameState.WIN;
                playSound("win");
            } else {
                currentRoom = rooms[currentRoomIndex];
                player.setPosition(100, 100);
                vampires.clear();
                keys.clear();
                spawnVampires(3 + currentRoomIndex);
                spawnKeys(2);
                playSound("door_open");
            }
        }
    }

    private void playSound(String soundName) {
        // Simulated sound effect (Java Sound API can be added for real audio)
        System.out.println("Playing sound: " + soundName);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W) keysPressed[0] = true;
        if (code == KeyEvent.VK_S) keysPressed[1] = true;
        if (code == KeyEvent.VK_A) keysPressed[2] = true;
        if (code == KeyEvent.VK_D) keysPressed[3] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W) keysPressed[0] = false;
        if (code == KeyEvent.VK_S) keysPressed[1] = false;
        if (code == KeyEvent.VK_A) keysPressed[2] = false;
        if (code == KeyEvent.VK_D) keysPressed[3] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

class Player {
    private int x, y;
    private BufferedImage[] sprites;
    private int direction; // 0: Up, 1: Down, 2: Left, 3: Right

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        direction = 1;
        sprites = new BufferedImage[4]; // Simulated sprites
        // In a real game, load images here
        for (int i = 0; i < 4; i++) {
            sprites[i] = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sprites[i].createGraphics();
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, 32, 32);
            g.dispose();
        }
    }

    public void update(boolean[] keysPressed, int[][] layout) {
        int newX = x;
        int newY = y;

        if (keysPressed[0]) { newY -= GamePanel.PLAYER_SPEED; direction = 0; }
        if (keysPressed[1]) { newY += GamePanel.PLAYER_SPEED; direction = 1; }
        if (keysPressed[2]) { newX -= GamePanel.PLAYER_SPEED; direction = 2; }
        if (keysPressed[3]) { newX += GamePanel.PLAYER_SPEED; direction = 3; }

        // Check collision
        if (!isCollision(newX, newY, layout)) {
            x = newX;
            y = newY;
        }
    }

    private boolean isCollision(int x, int y, int[][] layout) {
        int tileX = x / GamePanel.TILE_SIZE;
        int tileY = y / GamePanel.TILE_SIZE;
        if (tileX < 0 || tileX >= layout[0].length || tileY < 0 || tileY >= layout.length) {
            return true;
        }
        return layout[tileY][tileX] == 1;
    }

    public void draw(Graphics2D g, int animationFrame) {
        int frame = (animationFrame / 5) % 2; // Simple animation
        g.drawImage(sprites[direction], x, y, null);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
}

class Vampire {
    private int x, y;
    private BufferedImage[] sprites;
    private int direction;

    public Vampire(int x, int y) {
        this.x = x;
        this.y = y;
        direction = 0;
        sprites = new BufferedImage[4];
        for (int i = 0; i < 4; i++) {
            sprites[i] = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sprites[i].createGraphics();
            g.setColor(Color.RED);
            g.fillRect(0, 0, 32, 32);
            g.dispose();
        }
    }

    public void update(int playerX, int playerY, int[][] layout) {
        int dx = playerX - x;
        int dy = playerY - y;
        int newX = x;
        int newY = y;

        if (Math.abs(dx) > Math.abs(dy)) {
            newX += dx > 0 ? GamePanel.VAMPIRE_SPEED : -GamePanel.VAMPIRE_SPEED;
            direction = dx > 0 ? 3 : 2;
        } else {
            newY += dy > 0 ? GamePanel.VAMPIRE_SPEED : -GamePanel.VAMPIRE_SPEED;
            direction = dy > 0 ? 1 : 0;
        }

        if (!isCollision(newX, newY, layout)) {
            x = newX;
            y = newY;
        }
    }

    private boolean isCollision(int x, int y, int[][] layout) {
        int tileX = x / GamePanel.TILE_SIZE;
        int tileY = y / GamePanel.TILE_SIZE;
        if (tileX < 0 || tileX >= layout[0].length || tileY < 0 || tileY >= layout.length) {
            return true;
        }
        return layout[tileY][tileX] == 1;
    }

    public void draw(Graphics2D g, int animationFrame) {
        int frame = (animationFrame / 5) % 2;
        g.drawImage(sprites[direction], x, y, null);
    }

    public int getX() { return x; }
    public int getY() { return y; }
}

class Key {
    private int x, y;
    private BufferedImage sprite;

    public Key(int x, int y) {
        this.x = x;
        this.y = y;
        sprite = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sprite.createGraphics();
        g.setColor(Color.YELLOW);
        g.fillOval(0, 0, 16, 16);
        g.dispose();
    }

    public void draw(Graphics2D g, int animationFrame) {
        float alpha = (float) (0.5 + 0.5 * Math.sin(animationFrame * 0.2));
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        g.setComposite(ac);
        g.drawImage(sprite, x, y, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    public int getX() { return x; }
    public int getY() { return y; }
}

class Room {
    private int[][] layout;
    private BufferedImage floorTile, wallTile, doorTile;

    public Room(int[][] layout) {
        this.layout = layout;
        floorTile = new BufferedImage(GamePanel.TILE_SIZE, GamePanel.TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = floorTile.createGraphics();
        g.setColor(new Color(50, 50, 50));
        g.fillRect(0, 0, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);
        g.dispose();

        wallTile = new BufferedImage(GamePanel.TILE_SIZE, GamePanel.TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        g = wallTile.createGraphics();
        g.setColor(new Color(100, 50, 50));
        g.fillRect(0, 0, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);
        g.dispose();

        doorTile = new BufferedImage(GamePanel.TILE_SIZE, GamePanel.TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        g = doorTile.createGraphics();
        g.setColor(new Color(150, 100, 50));
        g.fillRect(0, 0, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);
        g.dispose();
    }

    public void draw(Graphics2D g) {
        for (int j = 0; j < layout.length; j++) {
            for (int i = 0; i < layout[0].length; i++) {
                int px = i * GamePanel.TILE_SIZE;
                int py = j * GamePanel.TILE_SIZE;
                if (layout[j][i] == 0) {
                    g.drawImage(floorTile, px, py, null);
                } else if (layout[j][i] == 1) {
                    g.drawImage(wallTile, px, py, null);
                } else if (layout[j][i] == 2) {
                    g.drawImage(doorTile, px, py, null);
                }
            }
        }
    }

    public int[][] getLayout() { return layout; }
}