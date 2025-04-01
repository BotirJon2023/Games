import java.util.HashMap;
import java.util.Scanner;
import java.util.ArrayList;

public class HauntedHotelMysteryGame {
    private static Scanner scanner = new Scanner(System.in);
    private static HashMap<String, Room> rooms = new HashMap<>();
    private static Player player;
    private static boolean gameOver = false;

    public static void main(String[] args) {
        initializeGame();
        printWelcomeMessage();

        while (!gameOver) {
            System.out.print("\nWhat would you like to do? > ");
            String input = scanner.nextLine().toLowerCase();
            processCommand(input);
        }

        System.out.println("\nThanks for playing Haunted Hotel Mystery!");
    }

    private static void initializeGame() {
        // Create player
        player = new Player();

        // Create rooms
        Room lobby = new Room("Lobby",
                "The grand lobby of the old hotel. Dust covers the furniture, and the chandelier sways slightly despite no wind. " +
                        "There's a reception desk to the north, and hallways leading east and west.");

        Room reception = new Room("Reception",
                "The reception area with a large ledger book on the counter. The ink has faded with time. " +
                        "A key hangs on a hook behind the desk. To the south is the lobby.");
        reception.addItem("old key");

        Room westHall = new Room("West Hallway",
                "A long hallway with peeling wallpaper. Doors line both sides, but most are locked. " +
                        "There's a particularly ominous door at the end with strange markings. The lobby is to the east.");
        westHall.addItem("candle");

        Room eastHall = new Room("East Hallway",
                "This hallway is dimly lit by flickering wall sconces. A large mirror at the end reflects movement " +
                        "even when nothing is there. The lobby is to the west, and a dining room is to the north.");

        Room diningRoom = new Room("Dining Room",
                "A once-elegant dining room with a long table set for a banquet that never happened. " +
                        "The plates are covered in dust, but one wine glass is suspiciously clean. The east hallway is to the south.");
        diningRoom.addItem("wine glass");

        Room masterSuite = new Room("Master Suite",
                "The luxurious master suite. The bed is perfectly made, but the pillows show indentations " +
                        "as if someone just rose from them. A journal lies open on the nightstand. The west hallway is to the east.");
        masterSuite.addItem("journal");
        masterSuite.setLocked(true);

        // Connect rooms
        lobby.addExit("north", reception);
        lobby.addExit("west", westHall);
        lobby.addExit("east", eastHall);

        reception.addExit("south", lobby);

        westHall.addExit("east", lobby);
        westHall.addExit("west", masterSuite);

        eastHall.addExit("west", lobby);
        eastHall.addExit("north", diningRoom);

        diningRoom.addExit("south", eastHall);

        masterSuite.addExit("east", westHall);

        // Set starting room
        player.setCurrentRoom(lobby);

        // Add some special interactions
        masterSuite.setSpecialInteraction("read journal", () -> {
            System.out.println("\nYou read the journal:");
            System.out.println("October 31, 1899 - The hotel is complete, but strange occurrences have begun.");
            System.out.println("November 2, 1899 - The mirrors show things that aren't there. Guests are complaining.");
            System.out.println("November 15, 1899 - I've locked the master suite. The presence is strongest here.");
            System.out.println("The last entry is smudged and unreadable.");
            System.out.println("\nAs you finish reading, the journal crumbles to dust in your hands.");
            player.getCurrentRoom().removeItem("journal");
        });

        diningRoom.setSpecialInteraction("inspect wine glass", () -> {
            System.out.println("\nYou examine the wine glass closely. There's a faint red residue at the bottom.");
            System.out.println("As you tilt it, you see an inscription: 'The truth lies in the reflection'.");
        });

        eastHall.setSpecialInteraction("look in mirror", () -> {
            System.out.println("\nYou approach the mirror cautiously. At first, it shows only your reflection.");
            System.out.println("Then suddenly, your reflection grins at you and points to the west hallway.");
            System.out.println("Before you can react, the image returns to normal.");
        });
    }

    private static void printWelcomeMessage() {
        System.out.println("HAUNTED HOTEL MYSTERY");
        System.out.println("=====================");
        System.out.println("You find yourself standing outside the abandoned Grand Victoria Hotel.");
        System.out.println("Legend says the hotel was closed after unexplained disappearances in 1899.");
        System.out.println("You've come to investigate the rumors of paranormal activity.");
        System.out.println("\nType 'help' for a list of commands.");
        System.out.println("\n" + player.getCurrentRoom().getDescription());
    }

    private static void processCommand(String input) {
        String[] parts = input.split(" ");
        String command = parts[0];

        switch (command) {
            case "go":
                if (parts.length > 1) {
                    String direction = parts[1];
                    goDirection(direction);
                } else {
                    System.out.println("Go where?");
                }
                break;

            case "look":
                System.out.println("\n" + player.getCurrentRoom().getDescription());
                player.getCurrentRoom().listItems();
                break;

            case "take":
                if (parts.length > 1) {
                    String itemName = input.substring(5);
                    takeItem(itemName);
                } else {
                    System.out.println("Take what?");
                }
                break;

            case "use":
                if (parts.length > 1) {
                    String itemName = input.substring(4);
                    useItem(itemName);
                } else {
                    System.out.println("Use what?");
                }
                break;

            case "inventory":
                player.showInventory();
                break;

            case "help":
                showHelp();
                break;

            case "quit":
                gameOver = true;
                break;

            default:
                // Check for special interactions
                if (player.getCurrentRoom().hasSpecialInteraction(input)) {
                    player.getCurrentRoom().triggerSpecialInteraction(input);
                } else {
                    System.out.println("I don't understand that command. Type 'help' for a list of commands.");
                }
        }
    }

    private static void goDirection(String direction) {
        Room currentRoom = player.getCurrentRoom();
        Room nextRoom = currentRoom.getExit(direction);

        if (nextRoom != null) {
            if (nextRoom.isLocked()) {
                if (player.hasItem("old key")) {
                    nextRoom.setLocked(false);
                    System.out.println("\nYou use the old key to unlock the door.");
                    player.setCurrentRoom(nextRoom);
                    System.out.println("\n" + nextRoom.getDescription());
                } else {
                    System.out.println("\nThe door is locked. You need a key to open it.");
                }
            } else {
                player.setCurrentRoom(nextRoom);
                System.out.println("\n" + nextRoom.getDescription());
            }
        } else {
            System.out.println("\nYou can't go that way.");
        }
    }

    private static void takeItem(String itemName) {
        Room currentRoom = player.getCurrentRoom();
        if (currentRoom.hasItem(itemName)) {
            player.addItem(itemName);
            currentRoom.removeItem(itemName);
            System.out.println("\nYou took the " + itemName + ".");

            // Special case for the journal
            if (itemName.equals("journal")) {
                System.out.println("As you pick up the journal, you hear a whisper: 'Read me...'");
            }
        } else {
            System.out.println("\nThere is no " + itemName + " here.");
        }
    }

    private static void useItem(String itemName) {
        if (player.hasItem(itemName)) {
            System.out.println("\nYou use the " + itemName + ".");

            // Special item interactions
            if (itemName.equals("candle") && player.getCurrentRoom().getName().equals("West Hallway")) {
                System.out.println("The candle flickers violently, then burns with an eerie blue flame.");
                System.out.println("The strange markings on the door glow faintly in response.");
            } else if (itemName.equals("old key") && player.getCurrentRoom().getName().equals("West Hallway")) {
                System.out.println("The key fits perfectly in the lock of the ominous door.");
            } else {
                System.out.println("Nothing interesting happens.");
            }
        } else {
            System.out.println("\nYou don't have a " + itemName + " in your inventory.");
        }
    }

    private static void showHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("go [direction] - Move in the specified direction (north, south, east, west)");
        System.out.println("look - Examine your current surroundings");
        System.out.println("take [item] - Pick up an item");
        System.out.println("use [item] - Use an item from your inventory");
        System.out.println("inventory - View your inventory");
        System.out.println("help - Show this help message");
        System.out.println("quit - Exit the game");
        System.out.println("\nYou can also interact with objects by typing commands like 'read journal' or 'look in mirror'");
    }
}

class Player {
    private Room currentRoom;
    private ArrayList<String> inventory = new ArrayList<>();

    public void setCurrentRoom(Room room) {
        this.currentRoom = room;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public void addItem(String item) {
        inventory.add(item);
    }

    public boolean hasItem(String item) {
        return inventory.contains(item);
    }

    public void removeItem(String item) {
        inventory.remove(item);
    }

    public void showInventory() {
        if (inventory.isEmpty()) {
            System.out.println("\nYour inventory is empty.");
        } else {
            System.out.println("\nInventory:");
            for (String item : inventory) {
                System.out.println("- " + item);
            }
        }
    }
}

class Room {
    private String name;
    private String description;
    private HashMap<String, Room> exits = new HashMap<>();
    private ArrayList<String> items = new ArrayList<>();
    private boolean isLocked;
    private HashMap<String, Runnable> specialInteractions = new HashMap<>();

    public Room(String name, String description) {
        this.name = name;
        this.description = description;
        this.isLocked = false;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description + "\nExits: " + getExitString();
    }

    public void addExit(String direction, Room neighbor) {
        exits.put(direction, neighbor);
    }

    public Room getExit(String direction) {
        return exits.get(direction);
    }

    public String getExitString() {
        return String.join(", ", exits.keySet());
    }

    public void addItem(String item) {
        items.add(item);
    }

    public boolean hasItem(String item) {
        return items.contains(item);
    }

    public void removeItem(String item) {
        items.remove(item);
    }

    public void listItems() {
        if (!items.isEmpty()) {
            System.out.println("You see:");
            for (String item : items) {
                System.out.println("- A " + item);
            }
        }
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public void setSpecialInteraction(String command, Runnable action) {
        specialInteractions.put(command, action);
    }

    public boolean hasSpecialInteraction(String command) {
        return specialInteractions.containsKey(command);
    }

    public void triggerSpecialInteraction(String command) {
        specialInteractions.get(command).run();
    }
}