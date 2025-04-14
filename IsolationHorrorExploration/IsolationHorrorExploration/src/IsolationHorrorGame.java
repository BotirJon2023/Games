import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

public class IsolationHorrorGame {
    private Player player;
    private ArrayList<Room> rooms;
    private Threat threat;
    private boolean gameOver;
    private Scanner scanner;
    private Random random;

    // Main game constructor
    public IsolationHorrorGame() {
        player = new Player("Explorer", 100, 0);
        rooms = new ArrayList<>();
        threat = new Threat("Shadow Entity", 3);
        gameOver = false;
        scanner = new Scanner(System.in);
        random = new Random();
        initializeRooms();
        initializeItems();
        initializeExits();
    }

    // Player class to manage player state
    static class Player {
        private String name;
        private int health;
        private int fearLevel;
        private ArrayList<Item> inventory;
        private Room currentRoom;

        public Player(String name, int health, int fearLevel) {
            this.name = name;
            this.health = health;
            this.fearLevel = fearLevel;
            this.inventory = new ArrayList<>();
        }

        public void addItem(Item item) {
            inventory.add(item);
            System.out.println("Picked up: " + item.getName());
        }

        public boolean hasItem(String itemName) {
            for (Item item : inventory) {
                if (item.getName().equalsIgnoreCase(itemName)) {
                    return true;
                }
            }
            return false;
        }

        public void increaseFear(int amount) {
            fearLevel = Math.min(fearLevel + amount, 100);
            System.out.println("Fear level increased to " + fearLevel);
        }

        public void decreaseFear(int amount) {
            fearLevel = Math.max(fearLevel - amount, 0);
            System.out.println("Fear level decreased to " + fearLevel);
        }

        public void takeDamage(int amount) {
            health = Math.max(health - amount, 0);
            System.out.println("You took " + amount + " damage. Health: " + health);
        }

        // Getters and setters
        public String getName() { return name; }
        public int getHealth() { return health; }
        public int getFearLevel() { return fearLevel; }
        public Room getCurrentRoom() { return currentRoom; }
        public void setCurrentRoom(Room room) { this.currentRoom = room; }
        public ArrayList<Item> getInventory() { return inventory; }
    }

    // Room class to represent game locations
    static class Room {
        private String name;
        private String description;
        private HashMap<String, Room> exits;
        private ArrayList<Item> items;
        private boolean isLocked;

        public Room(String name, String description) {
            this.name = name;
            this.description = description;
            this.exits = new HashMap<>();
            this.items = new ArrayList<>();
            this.isLocked = false;
        }

        public void addExit(String direction, Room room) {
            exits.put(direction, room);
        }

        public void addItem(Item item) {
            items.add(item);
        }

        public void removeItem(Item item) {
            items.remove(item);
        }

        public void setLocked(boolean locked) {
            this.isLocked = locked;
        }

        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public HashMap<String, Room> getExits() { return exits; }
        public ArrayList<Item> getItems() { return items; }
        public boolean isLocked() { return isLocked; }
    }

    // Item class for objects in the game
    static class Item {
        private String name;
        private String description;

        public Item(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
    }

    // Threat class to represent the enemy
    static class Threat {
        private String name;
        private int strength;
        private Room currentRoom;

        public Threat(String name, int strength) {
            this.name = name;
            this.strength = strength;
        }

        public void move(ArrayList<Room> rooms, Random random) {
            if (!rooms.isEmpty()) {
                currentRoom = rooms.get(random.nextInt(rooms.size()));
            }
        }

        public Room getCurrentRoom() { return currentRoom; }
        public String getName() { return name; }
        public int getStrength() { return strength; }

        public void setCurrentRoom(Room basement) {
        }
    }

    // Initialize rooms
    private void initializeRooms() {
        Room foyer = new Room("Foyer", "A dusty entrance with a chandelier flickering ominously.");
        Room library = new Room("Library", "Books line the walls, some whispering faintly.");
        Room kitchen = new Room("Kitchen", "Rusty knives and spoiled food fill the air with a stench.");
        Room basement = new Room("Basement", "Dark and damp, with strange scratches on the walls.");
        Room attic = new Room("Attic", "Creaky floorboards and old trunks hide secrets.");
        Room bedroom = new Room("Bedroom", "A broken mirror reflects your terrified face.");
        Room diningHall = new Room("Dining Hall", "A long table set for a meal never served.");
        Room secretRoom = new Room("Secret Room", "A hidden chamber with ancient symbols glowing faintly.");

        rooms.add(foyer);
        rooms.add(library);
        rooms.add(kitchen);
        rooms.add(basement);
        rooms.add(attic);
        rooms.add(bedroom);
        rooms.add(diningHall);
        rooms.add(secretRoom);

        player.setCurrentRoom(foyer);
        threat.setCurrentRoom(basement);
        secretRoom.setLocked(true);
    }

    // Initialize items
    private void initializeItems() {
        rooms.get(1).addItem(new Item("Old Key", "A rusty key that might unlock something."));
        rooms.get(2).addItem(new Item("Candle", "A flickering candle to light the way."));
        rooms.get(3).addItem(new Item("Diary", "A journal with cryptic warnings."));
        rooms.get(4).addItem(new Item("Amulet", "A glowing amulet that feels warm."));
        rooms.get(6).addItem(new Item("Map", "A tattered map of the mansion."));
    }

    // Initialize room exits
    private void initializeExits() {
        Room foyer = rooms.get(0);
        Room library = rooms.get(1);
        Room kitchen = rooms.get(2);
        Room basement = rooms.get(3);
        Room attic = rooms.get(4);
        Room bedroom = rooms.get(5);
        Room diningHall = rooms.get(6);
        Room secretRoom = rooms.get(7);

        foyer.addExit("north", library);
        foyer.addExit("east", kitchen);
        foyer.addExit("south", diningHall);

        library.addExit("south", foyer);
        library.addExit("west", bedroom);

        kitchen.addExit("west", foyer);
        kitchen.addExit("down", basement);

        basement.addExit("up", kitchen);
        basement.addExit("secret", secretRoom);

        bedroom.addExit("east", library);
        bedroom.addExit("up", attic);

        attic.addExit("down", bedroom);

        diningHall.addExit("north", foyer);

        secretRoom.addExit("back", basement);
    }

    // Main game loop
    public void play() {
        printWelcome();
        while (!gameOver) {
            printRoomDescription();
            checkThreatProximity();
            String command = scanner.nextLine().trim().toLowerCase();
            processCommand(command);
            updateGameState();
        }
        printGameOver();
    }

    // Print welcome message
    private void printWelcome() {
        System.out.println("Welcome to Isolation Horror Exploration!");
        System.out.println("You are trapped in a haunted mansion. Find a way out before fear consumes you.");
        System.out.println("Type 'help' for commands.\n");
    }

    // Print current room description
    private void printRoomDescription() {
        Room currentRoom = player.getCurrentRoom();
        System.out.println("\nYou are in the " + currentRoom.getName());
        System.out.println(currentRoom.getDescription());
        if (!currentRoom.getItems().isEmpty()) {
            System.out.print("Items here: ");
            for (Item item : currentRoom.getItems()) {
                System.out.print(item.getName() + " ");
            }
            System.out.println();
        }
        System.out.print("Exits: ");
        for (String direction : currentRoom.getExits().keySet()) {
            System.out.print(direction + " ");
        }
        System.out.println("\nWhat do you do?");
    }

    // Process player commands
    private void processCommand(String command) {
        String[] parts = command.split(" ");
        if (parts.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }

        String action = parts[0];
        String target = parts.length > 1 ? parts[1] : "";

        switch (action) {
            case "go":
                movePlayer(target);
                break;
            case "take":
                takeItem(target);
                break;
            case "use":
                useItem(target);
                break;
            case "look":
                printRoomDescription();
                break;
            case "inventory":
                showInventory();
                break;
            case "hide":
                hideFromThreat();
                break;
            case "help":
                showHelp();
                break;
            case "quit":
                gameOver = true;
                break;
            default:
                System.out.println("Unknown command. Type 'help' for options.");
        }
    }

    // Move player to a new room
    private void movePlayer(String direction) {
        Room currentRoom = player.getCurrentRoom();
        Room nextRoom = currentRoom.getExits().get(direction);
        if (nextRoom == null) {
            System.out.println("You can't go that way.");
        } else if (nextRoom.isLocked()) {
            System.out.println("The way to the " + nextRoom.getName() + " is locked.");
        } else {
            player.setCurrentRoom(nextRoom);
            player.increaseFear(5);
            System.out.println("You move to the " + nextRoom.getName());
        }
    }

    // Take an item from the room
    private void takeItem(String itemName) {
        Room currentRoom = player.getCurrentRoom();
        Item targetItem = null;
        for (Item item : currentRoom.getItems()) {
            if (item.getName().toLowerCase().contains(itemName)) {
                targetItem = item;
                break;
            }
        }
        if (targetItem != null) {
            player.addItem(targetItem);
            currentRoom.removeItem(targetItem);
        } else {
            System.out.println("No such item here.");
        }
    }

    // Use an item from inventory
    private void useItem(String itemName) {
        if (player.hasItem(itemName)) {
            if (itemName.equalsIgnoreCase("Old Key") && player.getCurrentRoom().getName().equals("Basement")) {
                rooms.get(7).setLocked(false);
                System.out.println("You unlocked the Secret Room!");
            } else if (itemName.equalsIgnoreCase("Candle")) {
                player.decreaseFear(10);
                System.out.println("The candle calms your nerves.");
            } else if (itemName.equalsIgnoreCase("Amulet") && player.getCurrentRoom() == threat.getCurrentRoom()) {
                System.out.println("The amulet repels the " + threat.getName() + "!");
                threat.move(rooms, random);
            } else {
                System.out.println("You can't use that here.");
            }
        } else {
            System.out.println("You don't have that item.");
        }
    }

    // Show player inventory
    private void showInventory() {
        if (player.getInventory().isEmpty()) {
            System.out.println("Your inventory is empty.");
        } else {
            System.out.print("Inventory: ");
            for (Item item : player.getInventory()) {
                System.out.print(item.getName() + " ");
            }
            System.out.println();
        }
    }

    // Hide from the threat
    private void hideFromThreat() {
        if (player.getCurrentRoom() == threat.getCurrentRoom()) {
            if (random.nextInt(100) < 50) {
                System.out.println("You successfully hide from the " + threat.getName() + "!");
                player.decreaseFear(5);
            } else {
                System.out.println("The " + threat.getName() + " finds you!");
                player.takeDamage(threat.getStrength() * 10);
            }
        } else {
            System.out.println("There's nothing to hide from here.");
        }
    }

    // Check if threat is nearby
    private void checkThreatProximity() {
        if (player.getCurrentRoom() == threat.getCurrentRoom()) {
            System.out.println("The " + threat.getName() + " is here! Run or hide!");
            player.increaseFear(20);
        } else if (player.getCurrentRoom().getExits().values().contains(threat.getCurrentRoom())) {
            System.out.println("You hear eerie whispers nearby...");
            player.increaseFear(10);
        }
    }

    // Update game state
    private void updateGameState() {
        threat.move(rooms, random);
        if (player.getFearLevel() >= 100) {
            System.out.println("Your fear overwhelms you. You collapse.");
            gameOver = true;
        }
        if (player.getHealth() <= 0) {
            System.out.println("You succumb to your injuries.");
            gameOver = true;
        }
        if (player.getCurrentRoom().getName().equals("Secret Room") && player.hasItem("Amulet")) {
            System.out.println("You found the exit and escaped with the amulet! You win!");
            gameOver = true;
        }
    }

    // Show help menu
    private void showHelp() {
        System.out.println("Commands:");
        System.out.println("  go <direction> - Move to another room (e.g., go north)");
        System.out.println("  take <item> - Pick up an item");
        System.out.println("  use <item> - Use an item from your inventory");
        System.out.println("  look - Examine the room");
        System.out.println("  inventory - Check your items");
        System.out.println("  hide - Try to hide from the threat");
        System.out.println("  help - Show this menu");
        System.out.println("  quit - End the game");
    }

    // Print game over message
    private void printGameOver() {
        System.out.println("\nGame Over. Thanks for playing!");
    }

    // Main method
    public static void main(String[] args) {
        IsolationHorrorGame game = new IsolationHorrorGame();
        game.play();
    }
}