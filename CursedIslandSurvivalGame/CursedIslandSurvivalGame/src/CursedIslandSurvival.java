import java.util.*;

class Player {
    private String name;
    private int health;
    private int hunger;
    List<String> inventory;

    public Player(String name) {
        this.name = name;
        this.health = 100;
        this.hunger = 100;
        this.inventory = new ArrayList<>();
    }

    public void eat(String food) {
        if (inventory.contains(food)) {
            inventory.remove(food);
            hunger = Math.min(hunger + 20, 100);
            System.out.println(name + " ate " + food + " and regained energy.");
        } else {
            System.out.println(food + " is not in your inventory!");
        }
    }

    public void takeDamage(int damage) {
        health -= damage;
        System.out.println(name + " took " + damage + " damage! Health: " + health);
    }

    public void addItem(String item) {
        inventory.add(item);
        System.out.println(name + " found a " + item);
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void showStatus() {
        System.out.println("Player: " + name + " | Health: " + health + " | Hunger: " + hunger);
        System.out.println("Inventory: " + inventory);
    }
}

class Island {
    private Random random;
    private List<String> resources;
    private List<String> dangers;

    public Island() {
        random = new Random();
        resources = Arrays.asList("Banana", "Coconut", "Fish", "Wood", "Stone", "Berry", "Mushroom", "Water");
        dangers = Arrays.asList("Storm", "Wild Beast", "Poisonous Plant", "Snake Attack", "Earthquake");
    }

    public String gatherResource() {
        int index = random.nextInt(resources.size());
        return resources.get(index);
    }

    public void triggerEvent(Player player) {
        int eventChance = random.nextInt(100);
        if (eventChance < 20) {
            String danger = dangers.get(random.nextInt(dangers.size()));
            System.out.println("A " + danger + " occurs! You take damage.");
            player.takeDamage(random.nextInt(20) + 5);
        } else if (eventChance < 40) {
            System.out.println("You discover an old shipwreck with valuable supplies!");
            player.addItem("Ancient Artifact");
        } else if (eventChance < 60) {
            System.out.println("A wild animal steals some of your food!");
        } else {
            System.out.println("A peaceful day passes on the island.");
        }
    }
}

class Crafting {
    public static void craftItem(Player player, String item) {
        switch (item.toLowerCase()) {
            case "spear":
                if (playerHasItems(player, "Wood", "Stone")) {
                    player.addItem("Spear");
                    System.out.println("You crafted a Spear!");
                } else {
                    System.out.println("You lack the materials.");
                }
                break;
            case "fire":
                if (playerHasItems(player, "Wood", "Stone")) {
                    player.addItem("Fire");
                    System.out.println("You created a Fire!");
                } else {
                    System.out.println("Not enough materials.");
                }
                break;
            default:
                System.out.println("Cannot craft this item.");
        }
    }

    private static boolean playerHasItems(Player player, String... items) {
        List<String> inventory = player.inventory;
        return Arrays.stream(items).allMatch(inventory::contains);
    }
}

public class CursedIslandSurvival {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your name: ");
        String playerName = scanner.nextLine();
        Player player = new Player(playerName);
        Island island = new Island();
        int daysSurvived = 0;

        while (player.isAlive()) {
            System.out.println("\nDay " + (daysSurvived + 1) + ": Choose an action - Gather, Eat, Rest, Status, Craft, Quit");
            String action = scanner.nextLine().toLowerCase();

            switch (action) {
                case "gather":
                    String resource = island.gatherResource();
                    player.addItem(resource);
                    island.triggerEvent(player);
                    break;
                case "eat":
                    System.out.println("Enter food name:");
                    String food = scanner.nextLine();
                    player.eat(food);
                    break;
                case "rest":
                    System.out.println("You rest for the night, recovering some health.");
                    daysSurvived++;
                    break;
                case "status":
                    player.showStatus();
                    break;
                case "craft":
                    System.out.println("Enter item to craft:");
                    String item = scanner.nextLine();
                    Crafting.craftItem(player, item);
                    break;
                case "quit":
                    System.out.println("You chose to end your survival challenge.");
                    return;
                default:
                    System.out.println("Invalid action. Try again.");
            }
        }

        System.out.println("Game Over! You survived " + daysSurvived + " days.");
    }
}
