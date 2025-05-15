import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class HauntedHouseEscapeGame extends JFrame {
    private GamePanel gamePanel;
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int PLAYER_SIZE = 40;
    private static final int GHOST_SIZE = 30;
    private static final int ITEM_SIZE = 20;

    public HauntedHouseEscapeGame() {
        setTitle("Haunted House Escape");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        gamePanel = new GamePanel();
        add(gamePanel);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HauntedHouseEscapeGame());
    }

    class GamePanel extends JPanel implements ActionListener, KeyListener {
        private Timer timer;
        private Player player;
        private ArrayList<Ghost> ghosts;
        private ArrayList<Item> items;
        private ArrayList<Room> rooms;
        private boolean gameWon;
        private boolean gameOver;
        private int currentRoomIndex;
        private Random random;

        public GamePanel() {
            setFocusable(true);
            addKeyListener(this);
            timer = new Timer(16, this); // ~60 FPS
            random = new Random();
            initializeGame();
            timer.start();
        }

        private void initializeGame() {
            player = new Player(50, 50);
            ghosts = new ArrayList<>();
            items = new ArrayList<>();
            rooms = new ArrayList<>();
            gameWon = false;
            gameOver = false;
            currentRoomIndex = 0;

            // Initialize rooms
            rooms.add(new Room("Hallway", Color.DARK_GRAY));
            rooms.add(new Room("Library", Color.BLUE.darker()));
            rooms.add(new Room("Basement", Color.GREEN.darker()));
            rooms.add(new Room("Attic", Color.RED.darker()));

            // Add items and ghosts to rooms
            rooms.get(0).addItem(new Item(200, 200, "Key"));
            rooms.get(1).addItem(new Item(300, 300, "Book"));
            rooms.get(2).addItem(new Item(400, 400, "Candle"));
            rooms.get(3).addItem(new Item(500, 500, "Master Key"));

            rooms.get(0).addGhost(new Ghost(600, 400));
            rooms.get(1).addGhost(new Ghost(500, 300));
            rooms.get(2).addGhost(new Ghost(400, 200));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw current room
            Room currentRoom = rooms.get(currentRoomIndex);
            setBackground(currentRoom.getColor());
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Room: " + currentRoom.getName(), 20, 30);
            g2d.drawString("Inventory: " + player.getInventory(), 20, 60);

            // Draw player
            player.draw(g2d);

            // Draw items
            for (Item item : currentRoom.getItems()) {
                item.draw(g2d);
            }

            // Draw ghosts
            for (Ghost ghost : currentRoom.getGhosts()) {
                ghost.draw(g2d);
            }

            // Draw game status
            if (gameWon) {
                g2d.setColor(Color.GREEN);
                g2d.setFont(new Font("Arial", Font.BOLD, 40));
                g2d.drawString("You Escaped!", WINDOW_WIDTH / 2 - 100, WINDOW_HEIGHT / 2);
            } else if (gameOver) {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 40));
                g2d.drawString("Game Over!", WINDOW_WIDTH / 2 - 100, WINDOW_HEIGHT / 2);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!gameWon && !gameOver) {
                updateGame();
            }
            repaint();
        }

        private void updateGame() {
            player.update();
            Room currentRoom = rooms.get(currentRoomIndex);

            // Update ghosts
            for (Ghost ghost : currentRoom.getGhosts()) {
                ghost.update();
                if (player.intersects(ghost)) {
                    gameOver = true;
                }
            }

            // Check for item collection
            ArrayList<Item> itemsToRemove = new ArrayList<>();
            for (Item item : currentRoom.getItems()) {
                if (player.intersects(item)) {
                    player.addItem(item);
                    itemsToRemove.add(item);
                }
            }
            currentRoom.getItems().removeAll(itemsToRemove);

            // Check for room transitions
            if (player.getX() > WINDOW_WIDTH - PLAYER_SIZE && currentRoomIndex < rooms.size() - 1) {
                if (canEnterNextRoom()) {
                    currentRoomIndex++;
                    player.setX(50);
                }
            }

            // Check win condition
            if (currentRoomIndex == rooms.size() - 1 && player.getInventory().contains("Master Key")) {
                gameWon = true;
            }
        }

        private boolean canEnterNextRoom() {
            String requiredItem = getRequiredItemForRoom(currentRoomIndex + 1);
            return player.getInventory().contains(requiredItem);
        }

        private String getRequiredItemForRoom(int roomIndex) {
            switch (roomIndex) {
                case 1: return "Key";
                case 2: return "Book";
                case 3: return "Candle";
                default: return "";
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                    player.setDy(-5);
                    break;
                case KeyEvent.VK_DOWN:
                    player.setDy(5);
                    break;
                case KeyEvent.VK_LEFT:
                    player.setDx(-5);
                    break;
                case KeyEvent.VK_RIGHT:
                    player.setDx(5);
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                case KeyEvent.VK_DOWN:
                    player.setDy(0);
                    break;
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_RIGHT:
                    player.setDx(0);
                    break;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {}
    }

    class Player {
        private int x, y;
        private int dx, dy;
        private ArrayList<String> inventory;

        public Player(int x, int y) {
            this.x = x;
            this.y = y;
            this.dx = 0;
            this.dy = 0;
            this.inventory = new ArrayList<>();
        }

        public void update() {
            x = Math.max(0, Math.min(x + dx, WINDOW_WIDTH - PLAYER_SIZE));
            y = Math.max(0, Math.min(y + dy, WINDOW_HEIGHT - PLAYER_SIZE));
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.YELLOW);
            g2d.fillOval(x, y, PLAYER_SIZE, PLAYER_SIZE);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(x, y, PLAYER_SIZE, PLAYER_SIZE);
        }

        public void addItem(Item item) {
            inventory.add(item.getName());
        }

        public boolean intersects(GameObject other) {
            Rectangle playerRect = new Rectangle(x, y, PLAYER_SIZE, PLAYER_SIZE);
            return playerRect.intersects(other.getBounds());
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public void setX(int x) { this.x = x; }
        public void setDx(int dx) { this.dx = dx; }
        public void setDy(int dy) { this.dy = dy; }
        public ArrayList<String> getInventory() { return inventory; }
    }

    class Ghost extends GameObject {
        private int dx, dy;

        public Ghost(int x, int y) {
            super(x, y, GHOST_SIZE, GHOST_SIZE);
            this.dx = (random.nextBoolean() ? 1 : -1) * 2;
            this.dy = (random.nextBoolean() ? 1 : -1) * 2;
        }

        public void update() {
            x += dx;
            y += dy;

            if (x <= 0 || x >= WINDOW_WIDTH - width) dx = -dx;
            if (y <= 0 || y >= WINDOW_HEIGHT - height) dy = -dy;

            x = Math.max(0, Math.min(x, WINDOW_WIDTH - width));
            y = Math.max(0, Math.min(y, WINDOW_HEIGHT - height));
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.fillOval(x, y, width, height);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x + 5, y + 5, 5, 5);
            g2d.fillOval(x + 15, y + 5, 5, 5);
        }
    }

    class Item extends GameObject {
        private String name;

        public Item(int x, int y, String name) {
            super(x, y, ITEM_SIZE, ITEM_SIZE);
            this.name = name;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.ORANGE);
            g2d.fillRect(x, y, width, height);
            g2d.setColor(Color.BLACK);
            g2d.drawString(name, x, y - 5);
        }

        public String getName() { return name; }
    }

    class Room {
        private String name;
        private Color color;
        private ArrayList<Ghost> ghosts;
        private ArrayList<Item> items;

        public Room(String name, Color color) {
            this.name = name;
            this.color = color;
            this.ghosts = new ArrayList<>();
            this.items = new ArrayList<>();
        }

        public void addGhost(Ghost ghost) { ghosts.add(ghost); }
        public void addItem(Item item) { items.add(item); }
        public String getName() { return name; }
        public Color getColor() { return color; }
        public ArrayList<Ghost> getGhosts() { return ghosts; }
        public ArrayList<Item> getItems() { return items; }
    }

    abstract class GameObject {
        protected int x, y, width, height;
        protected Random random = new Random();

        public GameObject(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }
}