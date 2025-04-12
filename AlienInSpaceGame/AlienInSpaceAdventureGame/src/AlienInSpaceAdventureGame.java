import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.HashMap;

public class AlienInSpaceAdventureGame {
    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}

class Game {
    private Player player;
    private SpaceStation station;
    private ArrayList<Alien> aliens;
    private Scanner scanner;
    private Random random;
    private boolean gameOver;
    private int turnCount;
    private HashMap<String, Integer> resourceCosts;

    public Game() {
        scanner = new Scanner(System.in);
        random = new Random();
        player = new Player("Scavenger", 100, 10, 50);
        station = new SpaceStation();
        aliens = new ArrayList<>();
        initializeAliens();
        gameOver = false;
        turnCount = 0;
        resourceCosts = new HashMap<>();
        initializeResourceCosts();
    }

    private void initializeAliens() {
        aliens.add(new Alien("Scout Alien", 30, 10, 5));  // Starts in Crew Quarters
        aliens.add(new Alien("Warrior Alien", 50, 15, 8)); // Starts in Engineering
        aliens.add(new Alien("Stealth Alien", 20, 8, 10)); // Starts in Storage Bay
    }

    private void initializeResourceCosts() {
        resourceCosts.put("repair", 20);
        resourceCosts.put("hack", 15);
        resourceCosts.put("craft", 25);
    }

    public void start() {
        System.out.println("--- Alien in Space Adventure Game ---");
        System.out.println("You are a scavenger on an abandoned space station.");
        System.out.println("Hostile aliens roam the corridors, and systems are failing.");
        System.out.println("Find a shuttle in the Hangar and escape before it's too late.");
        System.out.println("Type 'help' for commands.\n");

        while (!gameOver) {
            turnCount++;
            displayStatus();
            String input = getPlayerInput();
            processCommand(input);
            if (!gameOver) {
                moveAliens();
                checkAlienEncounters();
                checkEnvironmentalHazards();
                checkStationStability();
                checkRandomEvent();
            }
        }
        scanner.close();
    }

    private void displayStatus() {
        Room currentRoom = station.getRoom(player.getLocation());
        System.out.println("\nTurn: " + turnCount);
        System.out.println("Location: " + currentRoom.getName());
        System.out.println(currentRoom.getDescription());
        if (currentRoom.isLocked()) {
            System.out.println("This room is locked or requires repair!");
        }
        System.out.println("Exits: " + currentRoom.getExits());
        if (!currentRoom.getItems().isEmpty()) {
            System.out.println("Items here: " + currentRoom.getItems());
        }
        ArrayList<Alien> aliensHere = getAliensInRoom(player.getLocation());
        if (!aliensHere.isEmpty()) {
            System.out.println("Aliens here: " + aliensHere);
        }
        System.out.println("Your Health: " + player.getHealth());
        System.out.println("Your Energy: " + player.getEnergy());
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
                attackAlien(argument);
                break;
            case "repair":
                repairRoom();
                break;
            case "hack":
                hackSystem();
                break;
            case "craft":
                craftItem();
                break;
            case "scan":
                scanArea();
                break;
            case "look":
                displayStatus();
                break;
            case "help":
                displayHelp();
                break;
            case "quit":
                gameOver = true;
                System.out.println("You give up and succumb to the darkness. Game Over.");
                break;
            default:
                System.out.println("Unknown command. Type 'help' for commands.");
        }
    }

    private void movePlayer(String direction) {
        Room currentRoom = station.getRoom(player.getLocation());
        if (currentRoom.isLocked()) {
            System.out.println("This room is locked or broken. Try 'repair' or 'hack'.");
            return;
        }
        int newLocation = currentRoom.getExit(direction);
        if (newLocation != -1) {
            if (player.getEnergy() < 5) {
                System.out.println("You're too exhausted to move! Rest or use an energy pack.");
                return;
            }
            player.setLocation(newLocation);
            player.consumeEnergy(5);
            System.out.println("You move to " + station.getRoom(newLocation).getName() + ".");
        } else {
            System.out.println("No exit that way.");
        }
    }

    private void takeItem(String itemName) {
        Room currentRoom = station.getRoom(player.getLocation());
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

        Room currentRoom = station.getRoom(player.getLocation());
        switch (item.getName().toLowerCase()) {
            case "medkit":
                player.heal(50);
                player.removeItem(item);
                System.out.println("You use the medkit and restore 50 health.");
                break;
            case "energy pack":
                player.restoreEnergy(30);
                player.removeItem(item);
                System.out.println("You use the energy pack and restore 30 energy.");
                break;
            case "shuttle key":
                if (currentRoom.getName().equals("Shuttle Hangar")) {
                    System.out.println("You use the shuttle key to activate the shuttle!");
                    System.out.println("You escape the station! YOU WIN!");
                    gameOver = true;
                } else {
                    System.out.println("The shuttle key doesn't work here.");
                }
                break;
            case "plasma cutter":
                System.out.println("The plasma cutter is ready. Use 'attack [alien]' to fire.");
                break;
            case "scanner":
                scanArea();
                break;
            default:
                System.out.println("You can't use that item.");
        }
    }

    private void attackAlien(String alienName) {
        ArrayList<Alien> aliensHere = getAliensInRoom(player.getLocation());
        Alien target = null;
        for (Alien alien : aliensHere) {
            if (alien.getName().toLowerCase().contains(alienName.toLowerCase())) {
                target = alien;
                break;
            }
        }

        if (target == null) {
            System.out.println("No such alien here.");
            return;
        }

        if (player.getEnergy() < 10) {
            System.out.println("You're too exhausted to fight! Rest or use an energy pack.");
            return;
        }

        Item weapon = player.getItem("plasma cutter");
        int damage = weapon != null ? player.getAttackPower() + 20 : player.getAttackPower();
        target.takeDamage(damage);
        player.consumeEnergy(10);
        System.out.println("You attack the " + target.getName() + " for " + damage + " damage!");

        if (target.getHealth() <= 0) {
            System.out.println("You defeated the " + target.getName() + "!");
            aliens.remove(target);
            if (aliens.isEmpty()) {
                System.out.println("All aliens are defeated! YOU WIN!");
                gameOver = true;
            }
        } else {
            player.takeDamage(target.getAttackPower());
            System.out.println("The " + target.getName() + " strikes back for " + target.getAttackPower() + " damage!");
            checkPlayerDeath();
        }
    }

    private void repairRoom() {
        Room currentRoom = station.getRoom(player.getLocation());
        if (!currentRoom.isLocked()) {
            System.out.println("This room doesn't need repair.");
            return;
        }
        if (player.getEnergy() < resourceCosts.get("repair")) {
            System.out.println("You need " + resourceCosts.get("repair") + " energy to repair this room.");
            return;
        }
        currentRoom.unlock();
        player.consumeEnergy(resourceCosts.get("repair"));
        System.out.println("You repair the room's systems. It is now accessible.");
    }

    private void hackSystem() {
        Room currentRoom = station.getRoom(player.getLocation());
        if (!currentRoom.isLocked()) {
            System.out.println("This room doesn't need hacking.");
            return;
        }
        if (player.getEnergy() < resourceCosts.get("hack")) {
            System.out.println("You need " + resourceCosts.get("hack") + " energy to hack this room.");
            return;
        }
        currentRoom.unlock();
        player.consumeEnergy(resourceCosts.get("hack"));
        System.out.println("You hack the room's security. It is now accessible.");
    }

    private void craftItem() {
        if (player.getEnergy() < resourceCosts.get("craft")) {
            System.out.println("You need " + resourceCosts.get("craft") + " energy to craft an item.");
            return;
        }
        player.consumeEnergy(resourceCosts.get("craft"));
        String[] craftableItems = {"medkit", "energy pack"};
        String craftedItem = craftableItems[random.nextInt(craftableItems.length)];
        player.addItem(new Item(craftedItem));
        System.out.println("You craft a " + craftedItem + "!");
    }

    private void scanArea() {
        if (player.getEnergy() < 5) {
            System.out.println("You need 5 energy to use the scanner.");
            return;
        }
        player.consumeEnergy(5);
        System.out.println("Scanning area...");
        for (Alien alien : aliens) {
            if (alien.getHealth() > 0) {
                System.out.println(alien.getName() + " is in " + station.getRoom(alien.getLocation()).getName());
            }
        }
        Room currentRoom = station.getRoom(player.getLocation());
        if (!currentRoom.getItems().isEmpty()) {
            System.out.println("Items detected: " + currentRoom.getItems());
        }
    }

    private void moveAliens() {
        for (Alien alien : aliens) {
            if (alien.getHealth() <= 0) continue;
            if (random.nextDouble() < 0.7) {
                Room currentRoom = station.getRoom(alien.getLocation());
                if (currentRoom.isLocked()) continue;
                ArrayList<String> exits = currentRoom.getExitDirections();
                if (!exits.isEmpty()) {
                    String direction = exits.get(random.nextInt(exits.size()));
                    int newLocation = currentRoom.getExit(direction);
                    if (newLocation != -1 && !station.getRoom(newLocation).isLocked()) {
                        alien.setLocation(newLocation);
                    }
                }
            }
        }
    }

    private void checkAlienEncounters() {
        ArrayList<Alien> aliensHere = getAliensInRoom(player.getLocation());
        for (Alien alien : aliensHere) {
            if (alien.getHealth() > 0) {
                System.out.println("A " + alien.getName() + " ambushes you!");
                player.takeDamage(alien.getAttackPower());
                System.out.println("It deals " + alien.getAttackPower() + " damage!");
                checkPlayerDeath();
                if (gameOver) break;
            }
        }
    }

    private ArrayList<Alien> getAliensInRoom(int location) {
        ArrayList<Alien> aliensHere = new ArrayList<>();
        for (Alien alien : aliens) {
            if (alien.getLocation() == location && alien.getHealth() > 0) {
                aliensHere.add(alien);
            }
        }
        return aliensHere;
    }

    private void checkPlayerDeath() {
        if (player.getHealth() <= 0) {
            System.out.println("You collapse, overwhelmed by your injuries. Game Over.");
            gameOver = true;
        }
    }

    private void checkEnvironmentalHazards() {
        Room currentRoom = station.getRoom(player.getLocation());
        if (currentRoom.isHazardous() && random.nextDouble() < 0.3) {
            System.out.println("Environmental hazard! " + currentRoom.getHazardDescription());
            player.takeDamage(10);
            checkPlayerDeath();
        }
    }

    private void checkStationStability() {
        if (turnCount > 50 && random.nextDouble() < 0.1) {
            System.out.println("The station's orbit is decaying! You must escape soon!");
        }
        if (turnCount > 75) {
            System.out.println("The station plummets into the planet! Game Over.");
            gameOver = true;
        }
    }

    private void checkRandomEvent() {
        if (random.nextDouble() < 0.2) {
            switch (random.nextInt(5)) {
                case 0:
                    System.out.println("A power surge fries nearby systems! You lose 10 energy.");
                    player.consumeEnergy(10);
                    break;
                case 1:
                    System.out.println("You hear alien screeches echoing through the vents...");
                    break;
                case 2:
                    System.out.println("You find a hidden supply cache!");
                    station.getRoom(player.getLocation()).addItem(new Item(random.nextBoolean() ? "medkit" : "energy pack"));
                    break;
                case 3:
                    System.out.println("A loose panel falls, striking you! You lose 10 health.");
                    player.takeDamage(10);
                    checkPlayerDeath();
                    break;
                case 4:
                    System.out.println("The station's gravity fluctuates, slowing you down.");
                    player.consumeEnergy(5);
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
        System.out.println("  attack [alien] - Attack an alien (e.g., 'attack scout')");
        System.out.println("  repair - Repair a locked or broken room (costs energy)");
        System.out.println("  hack - Hack a locked room's security (costs energy)");
        System.out.println("  craft - Craft a random item (costs energy)");
        System.out.println("  scan - Scan for aliens and items (costs energy)");
        System.out.println("  look - Look around the current room");
        System.out.println("  help - Show this help message");
        System.out.println("  quit - End the game");
    }
}

class Player {
    private String name;
    private int health;
    private int attackPower;
    private int energy;
    private int location;
    private ArrayList<Item> inventory;

    public Player(String name, int health, int attackPower, int energy) {
        this.name = name;
        this.health = health;
        this.attackPower = attackPower;
        this.energy = energy;
        this.location = 0; // Start at Central Hub
        this.inventory = new ArrayList<>();
    }

    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getAttackPower() { return attackPower; }
    public int getEnergy() { return energy; }
    public int getLocation() { return location; }
    public ArrayList<Item> getInventory() { return inventory; }

    public void setLocation(int location) { this.location = location; }

    public void takeDamage(int damage) {
        health = Math.max(0, health - damage);
    }

    public void heal(int amount) {
        health = Math.min(100, health + amount);
    }

    public void consumeEnergy(int amount) {
        energy = Math.max(0, energy - amount);
    }

    public void restoreEnergy(int amount) {
        energy = Math.min(100, energy + amount);
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
        return name + " (Health: " + health + ", Energy: " + energy + ")";
    }
}

class Alien {
    private String name;
    private int health;
    private int attackPower;
    private int location;

    public Alien(String name, int health, int attackPower, int location) {
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
    private boolean isLocked;
    private boolean isHazardous;
    private String hazardDescription;

    public Room(String name, String description, boolean isLocked, boolean isHazardous, String hazardDescription) {
        this.name = name;
        this.description = description;
        this.exits = new int[]{-1, -1, -1, -1}; // -1 means no exit
        this.items = new ArrayList<>();
        this.isLocked = isLocked;
        this.isHazardous = isHazardous;
        this.hazardDescription = hazardDescription;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public ArrayList<Item> getItems() { return items; }
    public boolean isLocked() { return isLocked; }
    public boolean isHazardous() { return isHazardous; }
    public String getHazardDescription() { return hazardDescription; }

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

    public void unlock() {
        isLocked = false;
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

class SpaceStation {
    private ArrayList<Room> rooms;

    public SpaceStation() {
        rooms = new ArrayList<>();
        initializeRooms();
    }

    private void initializeRooms() {
        Room centralHub = new Room("Central Hub", "The heart of the station with multiple corridors.", false, false, "");
        Room crewQuarters = new Room("Crew Quarters", "Abandoned bunks with personal belongings.", false, true, "Leaking coolant pipes burn your skin!");
        Room medbay = new Room("Medbay", "A sterile room with overturned medical equipment.", false, false, "");
        Room engineering = new Room("Engineering", "A maze of pipes and malfunctioning generators.", true, true, "Sparking wires shock you!");
        Room storageBay = new Room("Storage Bay", "Crates and containers, some leaking strange fluids.", false, true, "Toxic fumes choke you!");
        Room commandCenter = new Room("Command Center", "Consoles flicker with encrypted data.", true, false, "");
        Room researchLab = new Room("Research Lab", "Vials and strange specimens are scattered.", false, true, "Acidic residue burns you!");
        Room maintenance = new Room("Maintenance", "Tools and spare parts litter the floor.", false, false, "");
        Room commsTower = new Room("Comms Tower", "Damaged antennas and broken screens.", true, false, "");
        Room shuttleHangar = new Room("Shuttle Hangar", "A dusty shuttle awaits, but it needs a key.", false, false, "");

        // Set exits
        centralHub.setExit("north", 1);
        centralHub.setExit("east", 2);
        centralHub.setExit("south", 4);
        centralHub.setExit("west", 5);
        crewQuarters.setExit("south", 0);
        crewQuarters.setExit("east", 3);
        medbay.setExit("west", 0);
        medbay.setExit("south", 6);
        engineering.setExit("west", 1);
        engineering.setExit("south", 7);
        storageBay.setExit("north", 0);
        storageBay.setExit("east", 8);
        commandCenter.setExit("east", 0);
        commandCenter.setExit("south", 9);
        researchLab.setExit("north", 2);
        researchLab.setExit("west", 7);
        maintenance.setExit("north", 3);
        maintenance.setExit("east", 6);
        commsTower.setExit("west", 4);
        commsTower.setExit("north", 9);
        shuttleHangar.setExit("north", 5);
        shuttleHangar.setExit("west", 8);

        // Add items
        medbay.addItem(new Item("medkit"));
        storageBay.addItem(new Item("energy pack"));
        maintenance.addItem(new Item("plasma cutter"));
        commandCenter.addItem(new Item("shuttle key"));
        researchLab.addItem(new Item("scanner"));

        // Add rooms to station
        rooms.add(centralHub);
        rooms.add(crewQuarters);
        rooms.add(medbay);
        rooms.add(engineering);
        rooms.add(storageBay);
        rooms.add(commandCenter);
        rooms.add(researchLab);
        rooms.add(maintenance);
        rooms.add(commsTower);
        rooms.add(shuttleHangar);
    }

    public Room getRoom(int index) {
        return rooms.get(index);
    }
}