import java.util.Random;
import java.util.Scanner;

public class HauntedAsylumSurvival {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();

        System.out.println("Welcome to Haunted Asylum Survival!");
        System.out.println("You awaken in a dark, cold cell. The air is thick with dread.");
        System.out.println("What do you do?");

        int playerHealth = 100;
        int playerSanity = 100;
        int inventoryItems = 0;
        boolean hasKey = false;
        boolean flashlightOn = false;
        int roomNumber = 1;

        while (playerHealth > 0 && playerSanity > 0) {
            System.out.println("\n--- Room " + roomNumber + " ---");
            System.out.println("Health: " + playerHealth + ", Sanity: " + playerSanity);
            System.out.println("Inventory Items: " + inventoryItems);
            if (flashlightOn) {
                System.out.println("Flashlight: ON");
            } else {
                System.out.println("Flashlight: OFF");
            }

            System.out.println("1. Explore the room.");
            System.out.println("2. Check inventory.");
            System.out.println("3. Use item.");
            System.out.println("4. Move to next room.");
            if (!flashlightOn) {
                System.out.println("5. Turn on Flashlight");
            } else {
                System.out.println("5. Turn off Flashlight");
            }
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    exploreRoom(random, playerHealth, playerSanity, hasKey, inventoryItems, flashlightOn);
                    playerSanity = adjustSanity(playerSanity, random.nextInt(20) - 10);
                    playerHealth = adjustHealth(playerHealth, random.nextInt(15) - 5);
                    break;
                case 2:
                    checkInventory(inventoryItems, hasKey);
                    break;
                case 3:
                    inventoryItems = useItem(scanner, playerHealth, playerSanity, inventoryItems);
                    break;
                case 4:
                    if (hasKey || roomNumber < 3 || flashlightOn) {
                        roomNumber++;
                        if (roomNumber > 5) {
                            System.out.println("You found the exit! You escaped the Asylum!");
                            return;
                        }
                    } else {
                        System.out.println("The door is locked.");
                    }
                    break;
                case 5:
                    flashlightOn = !flashlightOn;
                    System.out.println("Flashlight " + (flashlightOn ? "ON" : "OFF"));
                    break;
                default:
                    System.out.println("Invalid choice.");
            }

            if (random.nextInt(10) < 3) {
                encounterEvent(random, playerHealth, playerSanity);
                playerSanity = adjustSanity(playerSanity, random.nextInt(30) - 15);
                playerHealth = adjustHealth(playerHealth, random.nextInt(20) - 10);
            }
            if (random.nextInt(10) < 2) {
                if (flashlightOn) {
                    System.out.println("The flashlight flickers.");
                } else {
                    System.out.println("You hear a faint whisper.");
                }
            }
            if (roomNumber == 2 && !hasKey && random.nextInt(5) == 0) {
                System.out.println("You found a rusty key!");
                hasKey = true;
            }

            playerHealth = Math.max(0, playerHealth);
            playerSanity = Math.max(0, playerSanity);

            if (playerHealth <= 0) {
                System.out.println("You have succumbed to your injuries.");
            }
            if (playerSanity <= 0) {
                System.out.println("You have lost your sanity.");
            }
        }
    }

    private static void exploreRoom(Random random, int playerHealth, int playerSanity, boolean hasKey, int inventoryItems, boolean flashlightOn) {
        int event = random.nextInt(5);
        if (flashlightOn) {
            if (event == 0) {
                System.out.println("You find a dusty note. It reads: 'They are always watching.'");
            } else if (event == 1) {
                System.out.println("You see a shadow flicker in the corner of the room.");
            } else if (event == 2) {
                System.out.println("You find a medical kit.");
                inventoryItems++;
            } else if (event == 3) {
                System.out.println("A cold draft sweeps through the room.");
            } else {
                System.out.println("You see strange symbols on the walls.");
            }
        } else {
            System.out.println("It's too dark to see anything clearly.");
            playerSanity -= 5;
        }

    }

    private static void checkInventory(int inventoryItems, boolean hasKey) {
        System.out.println("Inventory:");
        System.out.println("Medical Kits: " + inventoryItems);
        if (hasKey) {
            System.out.println("Rusty Key");
        }
    }

    private static int useItem(Scanner scanner, int playerHealth, int playerSanity, int inventoryItems) {
        if (inventoryItems > 0) {
            System.out.println("Use medical kit? (yes/no)");
            String choice = scanner.nextLine();
            if (choice.equalsIgnoreCase("yes")) {
                playerHealth += 30;
                playerHealth = Math.min(100, playerHealth);
                inventoryItems--;
                System.out.println("You used a medical kit. Health restored.");
            }
        } else {
            System.out.println("You have no items to use.");
        }
        return inventoryItems;
    }

    private static void encounterEvent(Random random, int playerHealth, int playerSanity) {
        int event = random.nextInt(3);
        if (event == 0) {
            System.out.println("A ghostly apparition appears!");
            playerSanity -= 20;
        } else if (event == 1) {
            System.out.println("You hear a bloodcurdling scream!");
            playerSanity -= 15;
        } else {
            System.out.println("A shadowy figure lunges at you!");
            playerHealth -= 10;
        }
    }

    private static int adjustSanity(int sanity, int adjustment) {
        sanity += adjustment;
        return Math.max(0, Math.min(100, sanity));
    }

    private static int adjustHealth(int health, int adjustment) {
        health += adjustment;
        return Math.max(0, Math.min(100, health));
    }
}