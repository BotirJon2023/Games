import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

// Main game class
public class SurvivalHorrorOnAShip {
    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}

// Game class to manage overall flow
class Game {
    private Player player;
    private Ship ship;
    private Scanner scanner;
    private boolean gameOver;

    public Game() {
        player = new Player("Survivor");
        ship = new Ship();
        player.setCurrentRoom(ship.getStartingRoom()); // Fix: Set the starting room
        scanner = new Scanner(System.in);
        gameOver = false;
    }

    public void start() {
        System.out.println("Welcome to Survival Horror on a Ship!");
        System.out.println("You awaken on a derelict ship drifting in the ocean...");
        System.out.println("Your goal: survive and escape the horrors within.\n");

        while (!gameOver) {
            displayStatus();
            processCommand();
        }
        scanner.close();
        System.out.println("Game Over. Thanks for playing!");
    }

    private void displayStatus() {
        System.out.println("\n--- Status ---");
        System.out.println("Location: " + player.getCurrentRoom().getName());
        System.out.println("Health: " + player.getHealth() + "/100");
        System.out.println("Inventory: " + player.getInventory());
        System.out.println("Exits: " + player.getCurrentRoom().getExits());
        System.out.println("Items here: " + player.getCurrentRoom().getItems());
        if (player.getCurrentRoom().hasEnemy()) {
            System.out.println("Danger: " + player.getCurrentRoom().getEnemy().getName() + " is here!");
        }
        System.out.println("--------------\n");
    }

    private void processCommand() {
        System.out.print("Enter command (look, move, take, use, attack, quit): ");
        String input = scanner.nextLine().trim().toLowerCase();
        String[] parts = input.split(" ", 2);
        String command = parts[0];
        String argument = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "look":
                System.out.println(player.getCurrentRoom().getDescription());
                break;
            case "move":
                move(argument);
                break;
            case "take":
                take(argument);
                break;
            case "use":
                use(argument);
                break;
            case "attack":
                attack();
                break;
            case "quit":
                gameOver = true;
                break;
            default:
                System.out.println("Unknown command.");
        }
        checkGameOver();
    }

    private void move(String direction) {
        Room nextRoom = player.getCurrentRoom().getExit(direction);
        if (nextRoom != null) {
            player.setCurrentRoom(nextRoom);
            System.out.println("You move to " + nextRoom.getName() + ".");
            if (nextRoom.hasEnemy()) {
                System.out.println("A " + nextRoom.getEnemy().getName() + " appears!");
            }
        } else {
            System.out.println("You can't go that way.");
        }
    }

    private void take(String itemName) {
        Item item = player.getCurrentRoom().removeItem(itemName);
        if (item != null) {
            player.addItem(item);
            System.out.println("You picked up: " + item.getName());
        } else {
            System.out.println("No such item here.");
        }
    }

    private void use(String itemName) {
        Item item = player.getItem(itemName);
        if (item != null) {
            item.use(player, this);
        } else {
            System.out.println("You don't have that item.");
        }
    }

    private void attack() {
        if (!player.getCurrentRoom().hasEnemy()) {
            System.out.println("There's nothing to attack here.");
            return;
        }
        Enemy enemy = player.getCurrentRoom().getEnemy();
        int damage = player.attack();
        enemy.takeDamage(damage);
        System.out.println("You deal " + damage + " damage to " + enemy.getName() + ".");
        if (enemy.isDead()) {
            System.out.println(enemy.getName() + " is defeated!");
            player.getCurrentRoom().removeEnemy();
        } else {
            retaliate(enemy);
        }
    }

    private void retaliate(Enemy enemy) {
        int damage = enemy.attack();
        player.takeDamage(damage);
        System.out.println(enemy.getName() + " hits you for " + damage + " damage!");
    }

    private void checkGameOver() {
        if (player.getHealth() <= 0) {
            System.out.println("You have succumbed to the horrors of the ship...");
            gameOver = true;
        } else if (player.getCurrentRoom().getName().equals("Lifeboat Deck") && player.hasItem("Lifeboat Key")) {
            System.out.println("You unlock the lifeboat and escape the ship! You win!");
            gameOver = true;
        }
    }

    public Player getPlayer() {
        return player;
    }
}

// Player class
class Player {
    private String name;
    private int health;
    private Room currentRoom;
    private ArrayList<Item> inventory;
    private Random random;

    public Player(String name) {
        this.name = name;
        this.health = 100;
        this.inventory = new ArrayList<>();
        this.random = new Random();
    }

    public void setCurrentRoom(Room room) {
        this.currentRoom = room;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public int getHealth() {
        return health;
    }

    public void takeDamage(int damage) {
        health = Math.max(0, health - damage);
    }

    public void heal(int amount) {
        health = Math.min(100, health + amount);
    }

    public void addItem(Item item) {
        inventory.add(item);
    }

    public Item getItem(String name) {
        for (Item item : inventory) {
            if (item.getName().equalsIgnoreCase(name)) {
                return item;
            }
        }
        return null;
    }

    public boolean hasItem(String name) {
        return getItem(name) != null;
    }

    public ArrayList<Item> getInventory() {
        return inventory;
    }

    public int attack() {
        int baseDamage = random.nextInt(10) + 5; // 5-14 damage
        if (hasItem("Rusty Knife")) {
            baseDamage += 10; // Bonus damage with weapon
        }
        return baseDamage;
    }
}

// Room class
class Room {
    private String name;
    private String description;
    private ArrayList<Item> items;
    private Enemy enemy;
    private ArrayList<Exit> exits;

    public Room(String name, String description) {
        this.name = name;
        this.description = description;
        this.items = new ArrayList<>();
        this.exits = new ArrayList<>();
    }

    public void addExit(String direction, Room room) {
        exits.add(new Exit(direction, room));
    }

    public Room getExit(String direction) {
        for (Exit exit : exits) {
            if (exit.getDirection().equalsIgnoreCase(direction)) {
                return exit.getDestination();
            }
        }
        return null;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public Item removeItem(String name) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getName().equalsIgnoreCase(name)) {
                return items.remove(i);
            }
        }
        return null;
    }

    public void setEnemy(Enemy enemy) {
        this.enemy = enemy;
    }

    public Enemy getEnemy() {
        return enemy;
    }

    public boolean hasEnemy() {
        return enemy != null && !enemy.isDead();
    }

    public void removeEnemy() {
        this.enemy = null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ArrayList<Item> getItems() {
        return items;
    }

    public ArrayList<String> getExits() {
        ArrayList<String> exitDirections = new ArrayList<>();
        for (Exit exit : exits) {
            exitDirections.add(exit.getDirection());
        }
        return exitDirections;
    }
}

// Exit class
class Exit {
    private String direction;
    private Room destination;

    public Exit(String direction, Room destination) {
        this.direction = direction;
        this.destination = destination;
    }

    public String getDirection() {
        return direction;
    }

    public Room getDestination() {
        return destination;
    }
}

// Item class
class Item {
    private String name;
    private String description;

    public Item(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void use(Player player, Game game) {
        switch (name.toLowerCase()) {
            case "medkit":
                player.heal(30);
                System.out.println("You use the medkit and restore 30 health.");
                player.getInventory().remove(this);
                break;
            case "rusty knife":
                System.out.println("You equip the rusty knife. It increases your attack power.");
                break;
            case "lifeboat key":
                if (player.getCurrentRoom().getName().equals("Lifeboat Deck")) {
                    System.out.println("You can use the Lifeboat Key here to escape!");
                } else {
                    System.out.println("You can't use the Lifeboat Key here.");
                }
                break;
            default:
                System.out.println("You can't use that item.");
        }
    }
}

// Enemy class
class Enemy {
    private String name;
    private int health;
    private int attackPower;
    private Random random;

    public Enemy(String name, int health, int attackPower) {
        this.name = name;
        this.health = health;
        this.attackPower = attackPower;
        this.random = new Random();
    }

    public String getName() {
        return name;
    }

    public void takeDamage(int damage) {
        health = Math.max(0, health - damage);
    }

    public boolean isDead() {
        return health <= 0;
    }

    public int attack() {
        return random.nextInt(attackPower) + 5; // 5 to attackPower+5 damage
    }
}

// Ship class to set up the game world
class Ship {
    private Room startingRoom;

    public Ship() {
        // Create rooms
        Room cargoHold = new Room("Cargo Hold", "A dark, damp room filled with crates and strange noises.");
        Room engineRoom = new Room("Engine Room", "A noisy room with flickering lights and a massive engine.");
        Room crewQuarters = new Room("Crew Quarters", "Abandoned bunks and personal items scattered around.");
        Room bridge = new Room("Bridge", "The ship's control center, with broken equipment and a eerie silence.");
        Room lifeboatDeck = new Room("Lifeboat Deck", "A deck with a locked lifeboat, your only hope of escape.");

        // Set up exits
        cargoHold.addExit("north", engineRoom);
        engineRoom.addExit("south", cargoHold);
        engineRoom.addExit("east", crewQuarters);
        crewQuarters.addExit("west", engineRoom);
        crewQuarters.addExit("north", bridge);
        bridge.addExit("south", crewQuarters);
        bridge.addExit("east", lifeboatDeck);
        lifeboatDeck.addExit("west", bridge);

        // Add items
        cargoHold.addItem(new Item("Medkit", "Restores 30 health."));
        engineRoom.addItem(new Item("Rusty Knife", "Increases attack damage."));
        bridge.addItem(new Item("Lifeboat Key", "Unlocks the lifeboat."));

        // Add enemies
        cargoHold.setEnemy(new Enemy("Mutated Crewmember", 30, 10));
        crewQuarters.setEnemy(new Enemy("Ghostly Figure", 20, 8));
        lifeboatDeck.setEnemy(new Enemy("Sea Monster", 40, 15));

        startingRoom = cargoHold;
    }

    public Room getStartingRoom() {
        return startingRoom;
    }
}