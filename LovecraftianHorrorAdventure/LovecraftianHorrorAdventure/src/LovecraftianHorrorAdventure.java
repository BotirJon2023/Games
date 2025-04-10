import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class LovecraftianHorrorAdventure {
    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}

class Game {
    private Player player;
    private List<Location> locations;
    private Scanner scanner;
    private Random random;
    private boolean gameRunning;

    public Game() {
        player = new Player("Investigator");
        locations = new ArrayList<>();
        scanner = new Scanner(System.in);
        random = new Random();
        gameRunning = true;
        initializeLocations();
    }

    private void initializeLocations() {
        Location townSquare = new Location("Town Square",
                "A desolate square surrounded by crooked buildings under a sickly yellow sky.");
        Location oldChurch = new Location("Old Church",
                "A crumbling church with stained glass depicting incomprehensible beings.");
        Location abandonedHouse = new Location("Abandoned House",
                "A rotting house filled with whispers from unseen corners.");
        Location darkWoods = new Location("Dark Woods",
                "Twisted trees claw at a sky that seems to writhe with unseen stars.");

        townSquare.addItem(new Item("Rusty Key", "An old key with strange carvings."));
        oldChurch.addItem(new Item("Tattered Book", "A book bound in unknown leather, filled with mad scribblings."));
        abandonedHouse.addItem(new Item("Flickering Lantern", "A lantern that casts shadows that move on their own."));
        darkWoods.addEntity(new Entity("Shoggoth",
                "A shapeless mass of eyes and tendrils, gibbering in an alien tongue.", 80));

        locations.add(townSquare);
        locations.add(oldChurch);
        locations.add(abandonedHouse);
        locations.add(darkWoods);

        townSquare.addConnection(oldChurch);
        townSquare.addConnection(abandonedHouse);
        oldChurch.addConnection(darkWoods);
        abandonedHouse.addConnection(darkWoods);
    }

    public void start() {
        System.out.println("Welcome to the Lovecraftian Horror Adventure.");
        System.out.println("You awaken in a forsaken town, your mind clouded with dread.");
        System.out.println("Explore, but beware—sanity is fragile in the face of the unknown.\n");

        Location currentLocation = locations.get(0); // Start at Town Square

        while (gameRunning && player.isAlive()) {
            displayLocation(currentLocation);
            String input = scanner.nextLine().toLowerCase();
            processCommand(input, currentLocation);
            checkSanityEffects(currentLocation);
        }

        if (!player.isAlive()) {
            System.out.println("\nYour mind shatters, and the void claims you. Game Over.");
        }
    }

    private void displayLocation(Location location) {
        System.out.println("\n--- " + location.getName() + " ---");
        System.out.println(location.getDescription());
        System.out.print("Exits: ");
        for (Location exit : location.getConnections()) {
            System.out.print(exit.getName() + " ");
        }
        System.out.println("\nItems: " + (location.getItems().isEmpty() ? "None" : location.getItems()));
        System.out.println("Entities: " + (location.getEntities().isEmpty() ? "None" : location.getEntities()));
        System.out.println("Your Sanity: " + player.getSanity() + " | Inventory: " + player.getInventory());
        System.out.print("What do you do? (look, take, use, go, flee, quit): ");
    }

    private void processCommand(String input, Location currentLocation) {
        String[] parts = input.split(" ", 2);
        String command = parts[0];

        switch (command) {
            case "look":
                System.out.println(currentLocation.getDescription());
                break;
            case "take":
                if (parts.length > 1) {
                    takeItem(parts[1], currentLocation);
                } else {
                    System.out.println("Take what?");
                }
                break;
            case "use":
                if (parts.length > 1) {
                    useItem(parts[1], currentLocation);
                } else {
                    System.out.println("Use what?");
                }
                break;
            case "go":
                if (parts.length > 1) {
                    currentLocation = goToLocation(parts[1], currentLocation);
                } else {
                    System.out.println("Go where?");
                }
                break;
            case "flee":
                flee(currentLocation);
                break;
            case "quit":
                gameRunning = false;
                System.out.println("You abandon your quest, but the nightmares will never leave you.");
                break;
            default:
                System.out.println("Unknown command.");
        }
    }

    private void takeItem(String itemName, Location location) {
        Item item = location.removeItem(itemName);
        if (item != null) {
            player.addToInventory(item);
            System.out.println("You take the " + item.getName() + ".");
        } else {
            System.out.println("No such item here.");
        }
    }

    private void useItem(String itemName, Location location) {
        Item item = player.getItem(itemName);
        if (item != null) {
            if (item.getName().equalsIgnoreCase("Flickering Lantern") && !location.getEntities().isEmpty()) {
                System.out.println("The lantern flares, driving the shadows back momentarily.");
                location.getEntities().forEach(e -> e.reduceThreat(20));
            } else if (item.getName().equalsIgnoreCase("Tattered Book")) {
                System.out.println("You read the book. Your mind reels from forbidden knowledge.");
                player.loseSanity(15);
            } else {
                System.out.println("You can't use the " + item.getName() + " here.");
            }
        } else {
            System.out.println("You don't have that item.");
        }
    }

    private Location goToLocation(String destination, Location current) {
        for (Location exit : current.getConnections()) {
            if (exit.getName().toLowerCase().contains(destination.toLowerCase())) {
                System.out.println("You move to " + exit.getName() + ".");
                return exit;
            }
        }
        System.out.println("You can't go that way.");
        return current;
    }

    private void flee(Location location) {
        if (!location.getEntities().isEmpty()) {
            System.out.println("You flee in terror, losing your grip on reality.");
            player.loseSanity(10);
            if (!location.getConnections().isEmpty()) {
                Location escape = location.getConnections().get(random.nextInt(location.getConnections().size()));
                System.out.println("You stumble into " + escape.getName() + ".");
            }
        } else {
            System.out.println("There's nothing to flee from.");
        }
    }

    private void checkSanityEffects(Location location) {
        if (!location.getEntities().isEmpty()) {
            for (Entity entity : location.getEntities()) {
                System.out.println("The " + entity.getName() + " looms closer...");
                player.loseSanity(entity.getThreatLevel() / 5);
            }
        }
        if (player.getSanity() <= 0) {
            player.setAlive(false);
        } else if (player.getSanity() < 30) {
            System.out.println("The edges of your vision twist and writhe.");
        }
    }
}

class Player {
    private String name;
    private int sanity;
    private List<Item> inventory;
    private boolean alive;

    public Player(String name) {
        this.name = name;
        this.sanity = 100;
        this.inventory = new ArrayList<>();
        this.alive = true;
    }

    public int getSanity() {
        return sanity;
    }

    public void loseSanity(int amount) {
        sanity = Math.max(0, sanity - amount);
    }

    public List<Item> getInventory() {
        return inventory;
    }

    public void addToInventory(Item item) {
        inventory.add(item);
    }

    public Item getItem(String name) {
        for (Item item : inventory) {
            if (item.getName().toLowerCase().contains(name.toLowerCase())) {
                return item;
            }
        }
        return null;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Override
    public String toString() {
        return inventory.isEmpty() ? "Empty" : inventory.toString();
    }
}

class Location {
    private String name;
    private String description;
    private List<Item> items;
    private List<Entity> entities;
    private List<Location> connections;

    public Location(String name, String description) {
        this.name = name;
        this.description = description;
        this.items = new ArrayList<>();
        this.entities = new ArrayList<>();
        this.connections = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Item> getItems() {
        return items;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public List<Location> getConnections() {
        return connections;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public Item removeItem(String name) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getName().toLowerCase().contains(name.toLowerCase())) {
                return items.remove(i);
            }
        }
        return null;
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    public void addConnection(Location location) {
        connections.add(location);
    }

    @Override
    public String toString() {
        return name;
    }
}

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

    @Override
    public String toString() {
        return name;
    }
}

class Entity {
    private String name;
    private String description;
    private int threatLevel;

    public Entity(String name, String description, int threatLevel) {
        this.name = name;
        this.description = description;
        this.threatLevel = threatLevel;
    }

    public String getName() {
        return name;
    }

    public int getThreatLevel() {
        return threatLevel;
    }

    public void reduceThreat(int amount) {
        threatLevel = Math.max(0, threatLevel - amount);
    }

    @Override
    public String toString() {
        return name + " (Threat: " + threatLevel + ")";
    }
}