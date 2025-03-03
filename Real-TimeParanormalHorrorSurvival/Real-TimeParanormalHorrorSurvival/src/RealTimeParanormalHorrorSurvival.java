import java.util.Random;
import java.util.Scanner;

public class RealTimeParanormalHorrorSurvival {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Player player = new Player("John Doe", 100, 100);  // Starting health and sanity
        Environment environment = new Environment();
        Random random = new Random();

        System.out.println("Welcome to the Real-Time Paranormal Horror Survival Game!");

        // Game loop
        while (player.isAlive()) {
            environment.triggerEvent(player, random);
            System.out.println("Current Health: " + player.getHealth());
            System.out.println("Current Sanity: " + player.getSanity());

            // Player's turn: Take action
            System.out.println("\nWhat will you do?");
            System.out.println("1. Explore");
            System.out.println("2. Use an item (Not implemented yet)");
            System.out.println("3. Check status");
            int choice = scanner.nextInt();

            if (choice == 1) {
                System.out.println("You are exploring...");
            } else if (choice == 2) {
                System.out.println("Item usage not implemented.");
            } else if (choice == 3) {
                player.showStatus();
            }

            // Simulate the passage of time
            try {
                Thread.sleep(1000);  // 1 second real-time wait
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Game Over!");
    }
}

class Player {
    private String name;
    private int health;
    private int sanity;

    public Player(String name, int health, int sanity) {
        this.name = name;
        this.health = health;
        this.sanity = sanity;
    }

    public boolean isAlive() {
        return health > 0 && sanity > 0;
    }

    public int getHealth() {
        return health;
    }

    public int getSanity() {
        return sanity;
    }

    public void reduceHealth(int amount) {
        this.health -= amount;
    }

    public void reduceSanity(int amount) {
        this.sanity -= amount;
    }

    public void showStatus() {
        System.out.println("Player: " + name);
        System.out.println("Health: " + health);
        System.out.println("Sanity: " + sanity);
    }
}

class Environment {
    private String[] events = {
            "A ghost appears in front of you!",
            "You hear footsteps behind you...",
            "You feel a cold breeze...",
            "A dark figure moves across the hallway!",
            "You sense something watching you!"
    };

    public void triggerEvent(Player player, Random random) {
        int eventIndex = random.nextInt(events.length);
        System.out.println("\n" + events[eventIndex]);

        // Randomly affect health and sanity
        int healthEffect = random.nextInt(10) + 1;
        int sanityEffect = random.nextInt(15) + 1;
        player.reduceHealth(healthEffect);
        player.reduceSanity(sanityEffect);

        // Print event details
        System.out.println("Health decreased by " + healthEffect);
        System.out.println("Sanity decreased by " + sanityEffect);
    }
}