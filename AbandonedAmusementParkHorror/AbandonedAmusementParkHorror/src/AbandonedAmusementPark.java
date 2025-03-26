import java.util.Random;
import java.util.Scanner;

class Player {
    private String name;
    private int health;
    private boolean hasFlashlight;

    public Player(String name) {
        this.name = name;
        this.health = 100;
        this.hasFlashlight = false;
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health < 0) health = 0;
    }

    public boolean hasFlashlight() {
        return hasFlashlight;
    }

    public void findFlashlight() {
        this.hasFlashlight = true;
    }
}

class HauntedRide {
    private String name;
    private int scareFactor;

    public HauntedRide(String name, int scareFactor) {
        this.name = name;
        this.scareFactor = scareFactor;
    }

    public void experience(Player player) {
        Random random = new Random();
        int damage = random.nextInt(scareFactor);
        player.takeDamage(damage);
        System.out.println(player.getName() + " rides " + name + " and loses " + damage + " health!");
    }
}

class HauntedHouse {
    private boolean ghostEncountered;

    public HauntedHouse() {
        this.ghostEncountered = false;
    }

    public void enter(Player player) {
        if (!player.hasFlashlight()) {
            System.out.println("It's too dark! You should find a flashlight first.");
            return;
        }
        System.out.println("You enter the haunted house... Shadows move around you.");
        Random random = new Random();
        if (random.nextBoolean()) {
            System.out.println("A ghost attacks you!");
            player.takeDamage(30);
        } else {
            System.out.println("You find an old journal with cryptic warnings...");
        }
    }
}

public class AbandonedAmusementPark {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Abandoned Amusement Park...");
        System.out.print("Enter your name, brave explorer: ");
        String name = scanner.nextLine();
        Player player = new Player(name);
        HauntedHouse hauntedHouse = new HauntedHouse();

        HauntedRide[] rides = {
                new HauntedRide("Ghost Train", 20),
                new HauntedRide("Ferris Wheel of Shadows", 25),
                new HauntedRide("Tunnel of Terror", 30)
        };

        boolean flashlightFound = false;

        while (player.getHealth() > 0) {
            System.out.println("Choose an action: \n1 - Ride Ghost Train \n2 - Ride Ferris Wheel \n3 - Ride Tunnel of Terror \n4 - Enter Haunted House \n5 - Search for Flashlight \n0 - Escape");
            int choice = scanner.nextInt();

            if (choice == 0) {
                System.out.println("You run away from the park... but do you ever really escape?");
                break;
            }

            if (choice == 5) {
                if (!flashlightFound) {
                    System.out.println("You search around and find a flashlight!");
                    player.findFlashlight();
                    flashlightFound = true;
                } else {
                    System.out.println("You already have a flashlight!");
                }
                continue;
            }

            if (choice == 4) {
                hauntedHouse.enter(player);
                continue;
            }

            if (choice > 0 && choice <= 3) {
                rides[choice - 1].experience(player);
                System.out.println("Your current health: " + player.getHealth());
            }

            if (player.getHealth() <= 0) {
                System.out.println("You collapse... The park has claimed another soul!");
                break;
            }
        }
        scanner.close();
    }
}



