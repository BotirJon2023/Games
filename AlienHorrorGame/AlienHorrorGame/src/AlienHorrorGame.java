import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class AlienHorrorGame {
    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}

class Game {
    private Player player;
    private Ship ship;
    private Alien alien;
    private Scanner scanner;
    private Random random;
    private boolean gameOver;

    public Game() {
        scanner = new Scanner(System.in);
        random = new Random();
        player = new Player("Survivor", 100, 10);
        ship = new Ship();
        alien = new Alien(50, 15);
        gameOver = false;
    }

    public void start() {
        System.out.println("--- Alien Horror Game ---");
        System.out.println("You wake up on a derelict spaceship. An alien is hunting you.");
        System.out.println("Find the escape pod or destroy the alien to survive.");
        System.out.println("Type 'help' for commands.\n");

        while (!gameOver) {
            displayStatus();
            String input = getPlayerInput();
            processCommand(input);
            if (!gameOver) {
                alienMove();
                checkRandomEvent();
            }
        }
        scanner.close();
    }

    private void displayStatus() {
        Room currentRoom = ship.getRoom(player.getLocation());
        System.out.println("\nLocation: " + currentRoom.getName());
        System.out.println(currentRoom.getDescription());
        System.out.println("Exits: " + currentRoom.getExits());
        if (!currentRoom.getItems().isEmpty()) {
            System.out.println("Items here: " + currentRoom.getItems());
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
                attackAlien();
                break;
            case "look":
                displayStatus();
                break;
            case "help":
                displayHelp();
                break;
            case "quit":
                gameOver = true;
                System.out.println("You abandon hope and perish. Game Over.");
                break;
            default:
                System.out.println("Unknown command. Type 'help' for commands.");
        }
    }

    private void movePlayer(String direction) {
        Room currentRoom = ship.getRoom(player.getLocation());
        int newLocation = currentRoom.getExit(direction);
        if (newLocation != -1) {
            player.setLocation(newLocation);
            System.out.println("You move to " + ship.getRoom(newLocation).getName() + ".");
        } else {
            System.out.println("No exit that way.");
        }
    }

    private void takeItem(String itemName) {
        Room currentRoom = ship.getRoom(player.getLocation());
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

        Room currentRoom = ship.getRoom(player.getLocation());
        switch (item.getName().toLowerCase()) {
            case "medkit":
                player.heal(30);
                player.removeItem(item);
                System.out.println("You use the medkit and restore 30 health.");
                break;
            case "keycard":
                if (currentRoom.getName().equals("Escape Pod Bay")) {
                    System.out.println("You use the keycard to unlock the escape pod!");
                    System.out.println("You escape the ship! YOU WIN!");
                    gameOver = true;
                } else {
                    System.out.println("The keycard doesn't work here.");
                }
                break;
            case "plasma rifle":
                System.out.println("The plasma rifle is ready for combat. Use 'attack' to fire.");
                break;
            default:
                System.out.println("You can't use that item.");
        }
    }

    private void attackAlien() {
        if (player.getLocation() != alien.getLocation()) {
            System.out.println("The alien isn't here.");
            return;
        }

        Item weapon = player.getItem("plasma rifle");
        int damage = weapon != null ? player.getAttackPower() + 10 : player.getAttackPower();
        alien.takeDamage(damage);
        System.out.println("You attack the alien for " + damage + " damage!");

        if (alien.getHealth() <= 0) {
            System.out.println("You defeated the alien! YOU WIN!");
            gameOver = true;
        } else {
            player.takeDamage(alien.getAttackPower());
            System.out.println("The alien strikes back for " + alien.getAttackPower() + " damage!");
            checkPlayerDeath();
        }
    }

    private void alienMove() {
        if (random.nextDouble() < 0.5) {
            Room currentRoom = ship.getRoom(alien.getLocation());
            ArrayList<String> exits = currentRoom.getExitDirections();
            if (!exits.isEmpty()) {
                String direction = exits.get(random.nextInt(exits.size()));
                int newLocation = currentRoom.getExit(direction);
                if (newLocation != -1) {
                    alien.setLocation(newLocation);
                }
            }
        }

        if (player.getLocation() == alien.getLocation() && alien.getHealth() > 0) {
            System.out.println("The alien is here! It attacks!");
            player.takeDamage(alien.getAttackPower());
            System.out.println("The alien deals " + alien.getAttackPower() + " damage!");
            checkPlayerDeath();
        }
    }

    private void checkPlayerDeath() {
        if (player.getHealth() <= 0) {
            System.out.println("You succumb to your wounds. Game Over.");
            gameOver = true;
        }
    }

    private void checkRandomEvent() {
        if (random.nextDouble() < 0.2) {
            switch (random.nextInt(3)) {
                case 0:
                    System.out.println("A pipe bursts, spraying steam! You lose 5 health.");
                    player.takeDamage(5);
                    checkPlayerDeath();
                    break;
                case 1:
                    System.out.println("You hear the alien's screech echo through the ship...");
                    break;
                case 2:
                    System.out.println("You find a small cache with a medkit!");
                    ship.getRoom(player.getLocation()).addItem(new Item("medkit"));
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
        System.out.println("  attack - Attack the alien if it's in the room");
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
        this.location = 0; // Start at Bridge
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

class Alien {
    private int health;
    private int attackPower;
    private int location;

    public Alien(int health, int attackPower) {
        this.health = health;
        this.attackPower = attackPower;
        this.location = 5; // Start at Cargo Hold
    }

    public int getHealth() { return health; }
    public int getAttackPower() { return attackPower; }
    public int getLocation() { return location; }

    public void setLocation(int location) { this.location = location; }

    public void takeDamage(int damage) {
        health = Math.max(0, health - damage);
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
        this.exits = new int[]{ -1, -1, -1, -1 }; // -1 means no exit
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

class Ship {
    private ArrayList<Room> rooms;

    public Ship() {
        rooms = new ArrayList<>();
        initializeRooms();
    }

    private void initializeRooms() {
        // Create rooms
        Room bridge = new Room("Bridge", "The control center of the ship, filled with flickering consoles.");
        Room corridor = new Room("Corridor", "A narrow hallway with dim lights and strange noises.");
        Room medbay = new Room("Medbay", "A sterile room with medical supplies scattered around.");
        Room engineRoom = new Room("Engine Room", "A noisy room with humming machinery.");
        Room armory = new Room("Armory", "A secure room with weapons and equipment.");
        Room cargoHold = new Room("Cargo Hold", "A large area filled with crates and shadows.");
        Room escapePodBay = new Room("Escape Pod Bay", "The escape pods are here, but you need a keycard.");

        // Set exits
        bridge.setExit("south", 1);
        corridor.setExit("north", 0);
        corridor.setExit("east", 2);
        corridor.setExit("west", 3);
        corridor.setExit("south", 5);
        medbay.setExit("west", 1);
        engineRoom.setExit("east", 1);
        armory.setExit("south", 4);
        cargoHold.setExit("north", 1);
        cargoHold.setExit("east", 6);
        escapePodBay.setExit("west", 5);

        // Add items
        medbay.addItem(new Item("medkit"));
        armory.addItem(new Item("plasma rifle"));
        cargoHold.addItem(new Item("keycard"));

        // Add rooms to ship
        rooms.add(bridge);
        rooms.add(corridor);
        rooms.add(medbay);
        rooms.add(engineRoom);
        rooms.add(armory);
        rooms.add(cargoHold);
        rooms.add(escapePodBay);
    }

    public Room getRoom(int index) {
        return rooms.get(index);
    }
}