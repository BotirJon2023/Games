import java.util.Random;
import java.util.Scanner;

class SerialKillerHorrorSimulation {
    private static final String[] LOCATIONS = {"Abandoned House", "Dark Forest", "Creepy Motel", "Silent Alley", "Basement", "Old Church", "Foggy Graveyard"};
    private static final String[] ACTIONS = {"Hide", "Search for weapons", "Run", "Confront the killer", "Set a trap"};
    private static final Random random = new Random();
    private static boolean isAlive = true;
    private static boolean hasWeapon = false;
    private static boolean hasEscaped = false;
    private static int trapCount = 0;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Serial Killer Horror Simulation!");
        System.out.println("You are trapped in a town where a serial killer is hunting you. Survive and escape!");

        while (isAlive && !hasEscaped) {
            String location = LOCATIONS[random.nextInt(LOCATIONS.length)];
            System.out.println("\nYou are at: " + location);
            System.out.println("What will you do?");
            for (int i = 0; i < ACTIONS.length; i++) {
                System.out.println((i + 1) + ". " + ACTIONS[i]);
            }

            int choice = scanner.nextInt();
            scanner.nextLine();
            processChoice(choice, location);
        }

        if (hasEscaped) {
            System.out.println("\nCongratulations! You managed to escape from the killer!");
        } else {
            System.out.println("\nGame Over! The killer got you...");
        }
        scanner.close();
    }

    private static void processChoice(int choice, String location) {
        switch (choice) {
            case 1 -> hide();
            case 2 -> searchForWeapons(location);
            case 3 -> run();
            case 4 -> confrontKiller();
            case 5 -> setTrap(location);
            default -> System.out.println("Invalid choice. The killer is getting closer...");
        }
    }

    private static void hide() {
        if (random.nextInt(100) < 70) {
            System.out.println("You successfully hid from the killer.");
        } else {
            System.out.println("The killer found you while you were hiding!");
            isAlive = false;
        }
    }

    private static void searchForWeapons(String location) {
        if (random.nextInt(100) < 50) {
            hasWeapon = true;
            System.out.println("You found a weapon at " + location + "! Now you can fight back.");
        } else {
            System.out.println("No weapon found at " + location + ". The killer is nearby!");
        }
    }

    private static void run() {
        if (random.nextInt(100) < 60) {
            hasEscaped = true;
            System.out.println("You managed to escape the horror!");
        } else {
            System.out.println("You tried to run but the killer is still chasing you!");
        }
    }

    private static void confrontKiller() {
        if (hasWeapon) {
            if (random.nextInt(100) < 80) {
                System.out.println("You fought back and defeated the killer! You win!");
                hasEscaped = true;
            } else {
                System.out.println("You fought bravely but the killer overpowered you.");
                isAlive = false;
            }
        } else {
            System.out.println("You have no weapon! The killer easily takes you down...");
            isAlive = false;
        }
    }

    private static void setTrap(String location) {
        if (random.nextInt(100) < 50) {
            trapCount++;
            System.out.println("You set a trap at " + location + "! The killer may fall into it.");
        } else {
            System.out.println("The trap failed to set properly. Be careful!");
        }
    }

    private static void killerEvent() {
        if (trapCount > 0 && random.nextInt(100) < 40) {
            System.out.println("The killer fell into one of your traps! You gained some time to escape!");
            trapCount--;
        } else {
            System.out.println("The killer is getting closer! You need to act fast!");
        }
    }
}
