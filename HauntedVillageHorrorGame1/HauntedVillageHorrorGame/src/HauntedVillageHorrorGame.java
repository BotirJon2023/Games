import java.util.*;

class HauntedVillageHorrorGame {
    private static Scanner scanner = new Scanner(System.in);
    private static Player player;
    private static boolean gameRunning = true;
    private static Map<String, Location> locations = new HashMap<>();

    public static void main(String[] args) {
        setupGame();
        System.out.println("\nWelcome to the Haunted Village! Your goal is to survive and uncover its dark secrets.\n");

        while (gameRunning) {
            playerStatus();
            System.out.println("What would you like to do?");
            System.out.println("1. Move");
            System.out.println("2. Search");
            System.out.println("3. Check Inventory");
            System.out.println("4. Quit");

            int choice = getIntInput(1, 4);
            switch (choice) {
                case 1:
                    movePlayer();
                    break;
                case 2:
                    searchLocation();
                    break;
                case 3:
                    player.checkInventory();
                    break;
                case 4:
                    gameRunning = false;
                    System.out.println("You have left the haunted village. Game Over.");
                    break;
            }
        }
    }

    private static void setupGame() {
        player = new Player("Explorer");

        Location villageSquare = new Location("Village Square", "A dimly lit square with a ghostly presence.");
        Location hauntedHouse = new Location("Haunted House", "An abandoned house with eerie sounds echoing inside.");
        Location graveyard = new Location("Graveyard", "Tombstones are scattered, and whispers fill the air.");
        Location forest = new Location("Dark Forest", "Tall trees and strange shadows make it unsettling.");

        villageSquare.addPath("north", hauntedHouse);
        villageSquare.addPath("east", graveyard);
        villageSquare.addPath("west", forest);

        hauntedHouse.addPath("south", villageSquare);
        graveyard.addPath("west", villageSquare);
        forest.addPath("east", villageSquare);

        hauntedHouse.addItem("Old Key");
        graveyard.addItem("Ancient Scroll");
        forest.addItem("Silver Dagger");

        locations.put("Village Square", villageSquare);
        locations.put("Haunted House", hauntedHouse);
        locations.put("Graveyard", graveyard);
        locations.put("Dark Forest", forest);

        player.setLocation(villageSquare);
    }

    private static void movePlayer() {
        System.out.println("Available paths:");
        for (String direction : player.getLocation().getPaths().keySet()) {
            System.out.println("- " + direction);
        }
        System.out.print("Enter direction: ");
        String direction = scanner.nextLine().trim().toLowerCase();

        if (player.move(direction)) {
            System.out.println("You move " + direction + " and arrive at " + player.getLocation().getName());
        } else {
            System.out.println("You cannot go that way!");
        }
    }

    private static void searchLocation() {
        if (player.getLocation().hasItems()) {
            System.out.println("You found: " + player.getLocation().getItems());
            player.collectItems(player.getLocation().getItems());
            player.getLocation().clearItems();
        } else {
            System.out.println("There is nothing to be found here.");
        }
    }

    private static void playerStatus() {
        System.out.println("\n[Location: " + player.getLocation().getName() + "]");
        System.out.println(player.getLocation().getDescription());
    }

    private static int getIntInput(int min, int max) {
        while (true) {
            try {
                int input = Integer.parseInt(scanner.nextLine().trim());
                if (input >= min && input <= max) {
                    return input;
                }
                System.out.println("Invalid choice. Please try again.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}

class Player {
    private String name;
    private Location location;
    private List<String> inventory = new ArrayList<>();

    public Player(String name) {
        this.name = name;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public boolean move(String direction) {
        Location newLocation = location.getPaths().get(direction);
        if (newLocation != null) {
            setLocation(newLocation);
            return true;
        }
        return false;
    }

    public void checkInventory() {
        if (inventory.isEmpty()) {
            System.out.println("Your inventory is empty.");
        } else {
            System.out.println("Inventory: " + inventory);
        }
    }

    public void collectItems(List<String> items) {
        inventory.addAll(items);
    }
}

class Location {
    private String name;
    private String description;
    private List<String> items = new ArrayList<>();
    private Map<String, Location> paths = new HashMap<>();

    public Location(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void addPath(String direction, Location location) {
        paths.put(direction, location);
    }

    public Map<String, Location> getPaths() {
        return paths;
    }

    public void addItem(String item) {
        items.add(item);
    }

    public List<String> getItems() {
        return new ArrayList<>(items);
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }

    public void clearItems() {
        items.clear();
    }
}
