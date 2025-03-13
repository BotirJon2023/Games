import java.util.Random;
import java.util.Scanner;

class BloodCurdlingMysteryHorror {
    private static final Scanner scanner = new Scanner(System.in);
    private static final Random random = new Random();
    private static boolean hasKey = false;
    private static boolean hasWeapon = false;
    private static boolean monsterDefeated = false;
    private static boolean isAlive = true;
    private static int sanity = 100;

    public static void main(String[] args) {
        System.out.println("Welcome to BloodCurdling Mystery Horror!");
        System.out.println("You find yourself trapped inside a haunted mansion.");

        while (isAlive) {
            showMenu();
        }
    }

    private static void showMenu() {
        System.out.println("\nWhat would you like to do?");
        System.out.println("1. Explore the dark hallway");
        System.out.println("2. Enter the old library");
        System.out.println("3. Check the abandoned kitchen");
        System.out.println("4. Open the eerie basement");
        System.out.println("5. Try to escape through the front door");
        System.out.println("6. Check your sanity level");

        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                exploreHallway();
                break;
            case 2:
                enterLibrary();
                break;
            case 3:
                checkKitchen();
                break;
            case 4:
                openBasement();
                break;
            case 5:
                tryEscape();
                break;
            case 6:
                checkSanity();
                break;
            default:
                System.out.println("Invalid choice. Try again.");
        }
    }

    private static void exploreHallway() {
        System.out.println("You cautiously walk down the hallway. The air is cold.");
        if (!hasWeapon) {
            System.out.println("You find an old rusty knife. It might be useful.");
            hasWeapon = true;
        } else {
            System.out.println("You hear whispers but find nothing new.");
            reduceSanity(5);
        }
    }

    private static void enterLibrary() {
        System.out.println("The library is full of dusty books and strange symbols.");
        if (!hasKey) {
            System.out.println("You find a small key hidden in a book.");
            hasKey = true;
        } else {
            System.out.println("The books seem to rearrange themselves mysteriously.");
            reduceSanity(10);
        }
    }

    private static void checkKitchen() {
        System.out.println("The kitchen smells rotten. Old utensils lay scattered.");
        if (random.nextBoolean()) {
            System.out.println("A dark figure appears suddenly! It's the monster!");
            encounterMonster();
        } else {
            System.out.println("You find nothing but decayed food.");
            reduceSanity(5);
        }
    }

    private static void openBasement() {
        if (monsterDefeated) {
            System.out.println("The basement holds nothing but old furniture.");
        } else {
            System.out.println("You descend into darkness. A monstrous growl echoes!");
            encounterMonster();
        }
    }

    private static void encounterMonster() {
        System.out.println("The monster lunges at you!");
        if (hasWeapon) {
            System.out.println("You bravely fight with your weapon and defeat the monster!");
            monsterDefeated = true;
        } else {
            System.out.println("You have no weapon! The monster devours you...");
            isAlive = false;
            System.out.println("GAME OVER.");
        }
    }

    private static void tryEscape() {
        if (hasKey && monsterDefeated) {
            System.out.println("You use the key to unlock the front door and escape! You survived!");
            isAlive = false;
        } else if (!hasKey) {
            System.out.println("The door is locked. You need a key!");
        } else {
            System.out.println("The monster is still out there. You can't leave yet!");
        }
    }

    private static void checkSanity() {
        System.out.println("Your sanity level is: " + sanity + "%");
        if (sanity <= 20) {
            System.out.println("You feel an overwhelming sense of dread...");
        }
    }

    private static void reduceSanity(int amount) {
        sanity -= amount;
        if (sanity <= 0) {
            System.out.println("Your mind collapses under the terror. You go insane.");
            isAlive = false;
            System.out.println("GAME OVER.");
        }
    }
}
