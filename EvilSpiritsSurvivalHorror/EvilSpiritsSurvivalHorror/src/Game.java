import java.util.*;

// Player class
class Player {
    private String name;
    private int health;
    private int x, y;
    private List<String> inventory;

    public Player(String name, int startX, int startY) {
        this.name = name;
        this.health = 100;
        this.x = startX;
        this.y = startY;
        this.inventory = new ArrayList<>();
    }

    public void move(String direction) {
        switch (direction.toLowerCase()) {
            case "up": y--; break;
            case "down": y++; break;
            case "left": x--; break;
            case "right": x++; break;
            default: System.out.println("Invalid direction!");
        }
    }

    public void takeDamage(int damage) {
        health -= damage;
        System.out.println(name + " took " + damage + " damage! Health: " + health);
        if (health <= 0) {
            System.out.println(name + " has died...");
        }
    }

    public void addItem(String item) {
        inventory.add(item);
        System.out.println(name + " picked up " + item + "!");
    }

    public boolean hasItem(String item) {
        return inventory.contains(item);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getHealth() { return health; }
}

// Enemy (Evil Spirit) class
class EvilSpirit {
    private int x, y;
    private int damage;

    public EvilSpirit(int startX, int startY, int damage) {
        this.x = startX;
        this.y = startY;
        this.damage = damage;
    }

    public void moveRandom() {
        Random rand = new Random();
        x += rand.nextInt(3) - 1; // -1, 0, or 1
        y += rand.nextInt(3) - 1;
    }

    public boolean isNear(Player player) {
        return Math.abs(player.getX() - x) <= 1 && Math.abs(player.getY() - y) <= 1;
    }

    public void attack(Player player) {
        if (isNear(player)) {
            System.out.println("The evil spirit attacks!");
            player.takeDamage(damage);
        }
    }
}

// Game class
public class Game {
    private Player player;
    private List<EvilSpirit> spirits;
    private Map<String, int[]> items;
    private boolean running;

    public Game() {
        this.player = new Player("Hero", 5, 5);
        this.spirits = new ArrayList<>();
        this.items = new HashMap<>();
        this.running = true;
        setupGame();
    }

    private void setupGame() {
        spirits.add(new EvilSpirit(3, 3, 20));
        spirits.add(new EvilSpirit(7, 7, 15));
        items.put("Key", new int[]{2, 2});
        items.put("Health Potion", new int[]{6, 6});
    }

    public void play() {
        Scanner scanner = new Scanner(System.in);
        while (running && player.getHealth() > 0) {
            System.out.println("Enter command (move up/down/left/right, pickup, quit):");
            String command = scanner.nextLine();

            if (command.startsWith("move")) {
                String[] parts = command.split(" ");
                if (parts.length > 1) {
                    player.move(parts[1]);
                }
            } else if (command.equals("pickup")) {
                checkItemPickup();
            } else if (command.equals("quit")) {
                running = false;
                System.out.println("Game Over!");
            }

            updateSpirits();
        }
        scanner.close();
    }

    private void checkItemPickup() {
        for (Map.Entry<String, int[]> entry : items.entrySet()) {
            int[] pos = entry.getValue();
            if (player.getX() == pos[0] && player.getY() == pos[1]) {
                player.addItem(entry.getKey());
                items.remove(entry.getKey());
                return;
            }
        }
        System.out.println("No items here!");
    }

    private void updateSpirits() {
        for (EvilSpirit spirit : spirits) {
            spirit.moveRandom();
            spirit.attack(player);
        }
    }

    public static void main(String[] args) {
        Game game = new Game();
        game.play();
    }
}
