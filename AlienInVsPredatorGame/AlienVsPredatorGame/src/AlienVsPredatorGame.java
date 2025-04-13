import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class AlienVsPredatorGame {
    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}

class Game {
    private Player player;
    private Temple temple;
    private ArrayList<Enemy> enemies;
    private Scanner scanner;
    private Random random;
    private boolean gameOver;

    public Game() {
        scanner = new Scanner(System.in);
        random = new Random();
        player = new Player("Marine", 100, 10);
        temple = new Temple();
        enemies = new ArrayList<>();
        initializeEnemies();
        gameOver = false;
    }

    private void initializeEnemies() {
        enemies.add(new Enemy("Alien", 40, 12, 3)); // Starts in Ritual Chamber
        enemies.add(new Enemy("Predator", 60, 15, 7)); // Starts in Hunting Grounds
    }

    public void start() {
        System.out.println("--- Alien vs. Predator Game ---");
        System.out.println("You are a marine stranded in an ancient temple.");
        System.out.println("Aliens and Predators are hunting you and each other.");
        System.out.println("Find the dropship to escape or eliminate all enemies to survive.");
        System.out.println("Type 'help' for commands.\n");

        while (!gameOver) {
            displayStatus();
            String input = getPlayerInput();
            processCommand(input);
            if (!gameOver) {
                moveEnemies();
                checkEnemyEncounters();
                checkRandomEvent();
            }
        }
        scanner.close();
    }

    private void displayStatus() {
        Room currentRoom = temple.getRoom(player.getLocation());
        System.out.println("\nLocation: " + currentRoom.getName());
        System.out.println(currentRoom.getDescription());
        System.out.println("Exits: " + currentRoom.getExits());
        if (!currentRoom.getItems().isEmpty()) {
            System.out.println("Items here: " + currentRoom.getItems());
        }
        ArrayList<Enemy> enemiesHere = getEnemiesInRoom(player.getLocation());
        if (!enemiesHere.isEmpty()) {
            System.out.println("Enemies here: " + enemiesHere);
        }
        System.out.println("Your Health: " + player.getHealth());
        System.out.println("Inventory: " + player.getInventory());
    }

    private String getPlayerInput() {
        System.out.print("\nWhat do you do? ");
        return scanner.nextLine().trim().toLowerCase();
    }

    private void processCommand(String input) {
        String[] parts = input.split(" ", 2);
        String command = parts[0];
        String argument = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "go":
                movePlayer(argument);
                break;
            case "take":
                takeItem(argument);
                break;
            case "use":
                useItem(argument);
                break;
            case "inventory":
                System.out.println("Inventory: " + player.getInventory());
                break;
            case "attack":
                attackEnemy(argument);
                break;
            case "look":
                displayStatus();
                break;
            case "help":
                displayHelp();
                break;
            case "quit":
                gameOver = true;
                System.out.println("You abandon the fight and perish. Game Over.");
                break;
            default:
                System.out.println("Unknown command. Type 'help' for commands.");
        }
    }

    private void movePlayer(String direction) {
        Room currentRoom = temple.getRoom(player.getLocation());
        int newLocation = currentRoom.getExit(direction);
        if (newLocation != -1) {
            player.setLocation(newLocation);
            System.out.println("You move to " + temple.getRoom(newLocation).getName() + ".");
        } else {
            System.out.println("No exit that way.");
        }
    }

    private void takeItem(String itemName) {
        Room currentRoom = temple.getRoom(player.getLocation());
        Item item = currentRoom.removeItem(itemName);
        if (item != null) {
            player.addItem(item);
            System.out.println("You picked up " + item.getName() + ".");
        } else {
            System.out.println("No such item here.");
        }
    }

    private void useItem(String itemName) {
        Item item = player.getItem(itemName);
        if (item == null) {
            System.out.println("You don't have that item.");
            return;
        }

        Room currentRoom = temple.getRoom(player.getLocation());
        switch (item.getName().toLowerCase()) {
            case "medkit":
                player.heal(40);
                player.removeItem(item);
                System.out.println("You use the medkit and restore 40 health.");
                break;
            case "access code":
                if (currentRoom.getName().equals("Dropship Hangar")) {
                    System.out.println("You enter the access code and board the dropship!");
                    System.out.println("You escape the temple! YOU WIN!");
                    gameOver = true;
                } else {
                    System.out.println("The access code doesn't work here.");
                }
                break;
            case "pulse rifle":
                System.out.println("The pulse rifle is ready. Use 'attack [enemy]' to fire.");
                break;
            case "motion tracker":
                System.out.println("Motion tracker activated:");
                for (Enemy enemy : enemies) {
                    if (enemy.getHealth() > 0) {
                        System.out.println(enemy.getName() + " is in " + temple.getRoom(enemy.getLocation()).getName());
                    }
                }
                break;
            default:
                System.out.println("You can't use that item.");
        }
    }

    private void attackEnemy(String enemyName) {
        ArrayList<Enemy> enemiesHere = getEnemiesInRoom(player.getLocation());
        Enemy target = null;
        for (Enemy enemy : enemiesHere) {
            if (enemy.getName().toLowerCase().contains(enemyName.toLowerCase())) {
                target = enemy;
                break;
            }
        }

        if (target == null) {
            System.out.println("No such enemy here.");
            return;
        }

        Item weapon = player.getItem("pulse rifle");
        int damage = weapon != null ? player.getAttackPower() + 15 : player.getAttackPower();
        target.takeDamage(damage);
        System.out.println("You attack the " + target.getName() + " for " + damage + " damage!");

        if (target.getHealth() <= 0) {
            System.out.println("You defeated the " + target.getName() + "!");
            enemies.remove(target);
            if (enemies.isEmpty()) {
                System.out.println("All enemies are defeated! YOU WIN!");
                gameOver = true;
            }
        } else {
            player.takeDamage(target.getAttackPower());
            System.out.println("The " + target.getName() + " retaliates for " + target.getAttackPower() + " damage!");
            checkPlayerDeath();
        }
    }

    private void moveEnemies() {
        for (Enemy enemy : enemies) {
            if (enemy.getHealth() <= 0) continue;
            if (random.nextDouble() < 0.6) {
                Room currentRoom = temple.getRoom(enemy.getLocation());
                ArrayList<String> exits = currentRoom.getExitDirections();
                if (!exits.isEmpty()) {
                    String direction = exits.get(random.nextInt(exits.size()));
                    int newLocation = currentRoom.getExit(direction);
                    if (newLocation != -1) {
                        enemy.setLocation(newLocation);
                    }
                }
            }
        }
    }

    private void checkEnemyEncounters() {
        ArrayList<Enemy> enemiesHere = getEnemiesInRoom(player.getLocation());
        for (Enemy enemy : enemiesHere) {
            if (enemy.getHealth() > 0) {
                System.out.println("A " + enemy.getName() + " attacks you!");
                player.takeDamage(enemy.getAttackPower());
                System.out.println("It deals " + enemy.getAttackPower() + " damage!");
                checkPlayerDeath();
                if (gameOver) break;
            }
        }

        // Enemies fight each other if in the same room
        ArrayList<Enemy> enemiesInSameRoom = getEnemiesInRoom(enemies.get(0).getLocation());
        if (enemiesInSameRoom.size() > 1) {
            Enemy alien = enemiesInSameRoom.get(0);
            Enemy predator = enemiesInSameRoom.get(1);
            if (alien.getHealth() > 0 && predator.getHealth() > 0) {
                alien.takeDamage(predator.getAttackPower());
                predator.takeDamage(alien.getAttackPower());
                System.out.println("The Alien and Predator clash in " + temple.getRoom(alien.getLocation()).getName() + "!");
                if (alien.getHealth() <= 0) {
                    System.out.println("The Predator kills the Alien!");
                    enemies.remove(alien);
                }
                if (predator.getHealth() <= 0) {
                    System.out.println("The Alien kills the Predator!");
                    enemies.remove(predator);
                }
            }
        }
    }

    private ArrayList<Enemy> getEnemiesInRoom(int location) {
        ArrayList<Enemy> enemiesHere = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy.getLocation() == location && enemy.getHealth() > 0) {
                enemiesHere.add(enemy);
            }
        }
        return enemiesHere;
    }

    private void checkPlayerDeath() {
        if (player.getHealth() <= 0) {
            System.out.println("You succumb to your wounds. Game Over.");
            gameOver = true;
        }
    }

    private void checkRandomEvent() {
        if (random.nextDouble() < 0.25) {
            switch (random.nextInt(4)) {
                case 0:
                    System.out.println("A trap triggers, and spikes graze you! You lose 10 health.");
                    player.takeDamage(10);
                    checkPlayerDeath();
                    break;
                case 1:
                    System.out.println("You hear distant roars and hisses echoing through the temple...");
                    break;
                case 2:
                    System.out.println("You find a hidden medkit!");
                    temple.getRoom(player.getLocation()).addItem(new Item("medkit"));
                    break;
                case 3:
                    System.out.println("An explosion rocks the temple, disorienting you!");
                    player.takeDamage(5);
                    checkPlayerDeath();
                    break;
            }
        }
    }

    private void displayHelp() {
        System.out.println("Commands:");
        System.out.println("  go [direction] - Move to another room (e.g., 'go north')");
        System.out.println("  take [item] - Pick up an item");
        System.out.println("  use [item] - Use an item in your inventory");
        System.out.println("  inventory - Check your inventory");
        System.out.println("  attack [enemy] - Attack an enemy (e.g., 'attack alien')");
        System.out.println("  look - Look around the current room");
        System.out.println("  help - Show this help message");
        System.out.println("  quit - End the game");
    }
}

class Player {
    private String name;
    private int health;
    private int attackPower;
    private int location;
    private ArrayList<Item> inventory;

    public Player(String name, int health, int attackPower) {
        this.name = name;
        this.health = health;
        this.attackPower = attackPower;
        this.location = 0; // Start at Entrance Hall
        this.inventory = new ArrayList<>();
    }

    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getAttackPower() { return attackPower; }
    public int getLocation() { return location; }
    public ArrayList<Item> getInventory() { return inventory; }

    public void setLocation(int location) { this.location = location; }

    public void takeDamage(int damage) {
        health = Math.max(0, health - damage);
    }

    public void heal(int amount) {
        health = Math.min(100, health + amount);
    }

    public void addItem(Item item) {
        inventory.add(item);
    }

    public Item getItem(String itemName) {
        for (Item item : inventory) {
            if (item.getName().equalsIgnoreCase(itemName)) {
                return item;
            }
        }
        return null;
    }

    public void removeItem(Item item) {
        inventory.remove(item);
    }

    @Override
    public String toString() {
        return name + " (Health: " + health + ")";
    }
}

class Enemy {
    private String name;
    private int health;
    private int attackPower;
    private int location;

    public Enemy(String name, int health, int attackPower, int location) {
        this.name = name;
        this.health = health;
        this.attackPower = attackPower;
        this.location = location;
    }

    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getAttackPower() { return attackPower; }
    public int getLocation() { return location; }

    public void setLocation(int location) { this.location = location; }

    public void takeDamage(int damage) {
        health = Math.max(0, health - damage);
    }

    @Override
    public String toString() {
        return name + " (Health: " + health + ")";
    }
}

class Room {
    private String name;
    private String description;
    private int[] exits; // north, east, south, west
    private ArrayList<Item> items;

    public Room(String name, String description) {
        this.name = name;
        this.description = description;
        this.exits = new int[]{-1, -1, -1, -1}; // -1 means no exit
        this.items = new ArrayList<>();
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public ArrayList<Item> getItems() { return items; }

    public void setExit(String direction, int roomIndex) {
        switch (direction.toLowerCase()) {
            case "north": exits[0] = roomIndex; break;
            case "east": exits[1] = roomIndex; break;
            case "south": exits[2] = roomIndex; break;
            case "west": exits[3] = roomIndex; break;
        }
    }

    public int getExit(String direction) {
        switch (direction.toLowerCase()) {
            case "north": return exits[0];
            case "east": return exits[1];
            case "south": return exits[2];
            case "west": return exits[3];
            default: return -1;
        }
    }

    public ArrayList<String> getExitDirections() {
        ArrayList<String> directions = new ArrayList<>();
        if (exits[0] != -1) directions.add("north");
        if (exits[1] != -1) directions.add("east");
        if (exits[2] != -1) directions.add("south");
        if (exits[3] != -1) directions.add("west");
        return directions;
    }

    public String getExits() {
        ArrayList<String> exitList = getExitDirections();
        return exitList.isEmpty() ? "None" : String.join(", ", exitList);
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public Item removeItem(String itemName) {
        for (Item item : items) {
            if (item.getName().equalsIgnoreCase(itemName)) {
                items.remove(item);
                return item;
            }
        }
        return null;
    }
}

class Item {
    private String name;

    public Item(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}

class Temple {
    private ArrayList<Room> rooms;

    public Temple() {
        rooms = new ArrayList<>();
        initializeRooms();
    }

    private void initializeRooms() {
        // Create rooms
        Room entrance = new Room("Entrance Hall", "A grand hall with alien carvings on the walls.");
        Room corridor = new Room("Dark Corridor", "A narrow passage with flickering bioluminescent lights.");
        Room armory = new Room("Armory", "A room stocked with human and alien weapons.");
        Room ritualChamber = new Room("Ritual Chamber", "A cavernous room with a blood-stained altar.");
        Room powerCore = new Room("Power Core", "A humming chamber with unstable energy readings.");
        Room hive = new Room("Hive", "A slimy chamber filled with alien eggs.");
        Room crypt = new Room("Crypt", "A cold room with ancient Predator trophies.");
        Room huntingGrounds = new Room("Hunting Grounds", "An open area littered with bones.");
        Room dropshipHangar = new Room("Dropship Hangar", "A hangar with a dropship, but it needs an access code.");

        // Set exits
        entrance.setExit("north", 1);
        corridor.setExit("south", 0);
        corridor.setExit("north", 3);
        corridor.setExit("east", 2);
        corridor.setExit("west", 4);
        armory.setExit("west", 1);
        ritualChamber.setExit("south", 1);
        ritualChamber.setExit("east", 5);
        powerCore.setExit("east", 1);
        powerCore.setExit("north", 6);
        hive.setExit("west", 3);
        hive.setExit("north", 7);
        crypt.setExit("south", 4);
        crypt.setExit("east", 8);
        huntingGrounds.setExit("south", 5);
        huntingGrounds.setExit("west", 8);
        dropshipHangar.setExit("west", 7);

        // Add items
        armory.addItem(new Item("pulse rifle"));
        armory.addItem(new Item("medkit"));
        powerCore.addItem(new Item("access code"));
        crypt.addItem(new Item("motion tracker"));

        // Add rooms to temple
        rooms.add(entrance);
        rooms.add(corridor);
        rooms.add(armory);
        rooms.add(ritualChamber);
        rooms.add(powerCore);
        rooms.add(hive);
        rooms.add(crypt);
        rooms.add(huntingGrounds);
        rooms.add(dropshipHangar);
    }

    public Room getRoom(int index) {
        return rooms.get(index);
    }
}