import java.util.Random;
import java.util.Scanner;

// Main class for the Dark Web Horror Adventure game
public class DarkWebHorrorAdventure {
    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}

// Game class to handle story progression
class Game {
    private Player player;
    private Scanner scanner;
    private Random random;

    public Game() {
        scanner = new Scanner(System.in);
        random = new Random();
    }

    public void start() {
        System.out.println("Welcome to the Dark Web Horror Adventure...");
        System.out.print("Enter your alias: ");
        String alias = scanner.nextLine();
        player = new Player(alias);
        System.out.println("\nConnecting to the Dark Web...");
        delay(2000);
        exploreDarkWeb();
    }

    private void exploreDarkWeb() {
        System.out.println("\nYou find a mysterious website: 'The Forbidden Files'. Do you enter? (yes/no)");
        String choice = scanner.nextLine().toLowerCase();
        if (choice.equals("yes")) {
            enterForbiddenFiles();
        } else {
            System.out.println("You decide to close the browser and go to sleep... But you feel watched...");
        }
    }

    private void enterForbiddenFiles() {
        System.out.println("\nYou enter the website and see different files with strange names...");
        System.out.println("Options: 1) Open 'Rituals.txt' 2) Open 'Unknown.mp4' 3) Open 'Exit.exe'");
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                readRituals();
                break;
            case 2:
                watchUnknownVideo();
                break;
            case 3:
                executeExitFile();
                break;
            default:
                System.out.println("Invalid choice. The website crashes.");
        }
    }

    private void readRituals() {
        System.out.println("\nThe file describes a ritual to summon an unknown entity...");
        System.out.println("Do you follow the instructions? (yes/no)");
        String choice = scanner.nextLine().toLowerCase();
        if (choice.equals("yes")) {
            System.out.println("\nA shadowy figure appears behind you...");
            delay(2000);
            System.out.println("\nYour screen turns black, and you hear whispers from your speakers...");
            player.loseSanity(50);
        } else {
            System.out.println("\nYou close the file, but strange things start happening on your computer...");
        }
    }

    private void watchUnknownVideo() {
        System.out.println("\nThe video starts playing, showing dark tunnels and distorted faces...");
        delay(2000);
        if (random.nextBoolean()) {
            System.out.println("A horrifying figure stares directly into the camera and whispers your alias...");
            player.loseSanity(30);
        } else {
            System.out.println("The screen glitches, and you feel an eerie presence behind you...");
        }
    }

    private void executeExitFile() {
        System.out.println("\nYou run 'Exit.exe' and your screen flashes...");
        delay(2000);
        System.out.println("Your PC shuts down. A cold breeze fills the room...");
        if (random.nextBoolean()) {
            System.out.println("\nA message appears on your phone: 'You shouldn't have done that.'");
            player.loseSanity(40);
        } else {
            System.out.println("\nYou feel safe... for now.");
        }
    }

    private void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Player class to track sanity
class Player {
    private String name;
    private int sanity;

    public Player(String name) {
        this.name = name;
        this.sanity = 100;
    }

    public void loseSanity(int amount) {
        sanity -= amount;
        System.out.println("\nYour sanity is now: " + sanity);
        if (sanity <= 0) {
            System.out.println("\nYou have lost all sanity... The darkness consumes you.");
        }
    }
}
