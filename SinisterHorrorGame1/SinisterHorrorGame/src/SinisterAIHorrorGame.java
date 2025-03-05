import java.util.Random;
import java.util.Scanner;

public class SinisterAIHorrorGame {

    private static final int MAX_HEALTH = 100;
    private static final int MAX_SANITY = 100;

    private int playerHealth;
    private int playerSanity;
    private int location;
    private boolean hasKey;
    private Random random;
    private Scanner scanner;

    public SinisterAIHorrorGame() {
        playerHealth = MAX_HEALTH;
        playerSanity = MAX_SANITY;
        location = 0; // Starting location
        hasKey = false;
        random = new Random();
        scanner = new Scanner(System.in);
    }

    public void startGame() {
        System.out.println("Welcome to SinisterAIHorrorGame.");
        System.out.println("You awaken in a dark, damp room...");
        while (playerHealth > 0 && playerSanity > 0) {
            playTurn();
        }
        gameOver();
    }

    private void playTurn() {
        describeLocation();
        displayStatus();
        getPlayerAction();
        randomEvents();
    }

    private void describeLocation() {
        switch (location) {
            case 0:
                System.out.println("You are in a dimly lit cell. Cold stone walls surround you.");
                System.out.println("A rusty door is to the north.");
                break;
            case 1:
                System.out.println("You are in a long, narrow hallway. Flickering lights cast eerie shadows.");
                System.out.println("A door to the west and a door to the east.");
                if (!hasKey) {
                    System.out.println("A Key is on the floor.");
                }
                break;
            case 2:
                System.out.println("You've entered what appears to be a storage room. Crates and barrels are scattered everywhere.");
                System.out.println("There is a strange humming sound.");
                break;
            case 3:
                System.out.println("You are in a room with a large, pulsating machine. Wires and tubes snake across the floor.");
                System.out.println("The machine seems to be the source of the humming.");
                break;
            default:
                System.out.println("You are in an unknown location. Something is terribly wrong.");
                break;
        }
    }

    private void displayStatus() {
        System.out.println("Health: " + playerHealth + " | Sanity: " + playerSanity);
    }

    private void getPlayerAction() {
        System.out.println("What do you do? (north/south/east/west/examine/take)");
        String action = scanner.nextLine().toLowerCase();
        switch (action) {
            case "north":
                moveNorth();
                break;
            case "south":
                moveSouth();
                break;
            case "east":
                moveEast();
                break;
            case "west":
                moveWest();
                break;
            case "examine":
                examineLocation();
                break;
            case "take":
                takeItem();
                break;
            default:
                System.out.println("Invalid action.");
                break;
        }
    }

    private void moveNorth() {
        if (location == 0) {
            location = 1;
            System.out.println("You move north.");
        } else {
            System.out.println("You cannot go that way.");
        }
    }

    private void moveSouth() {
        if (location == 1) {
            location = 0;
            System.out.println("You move south.");
        } else {
            System.out.println("You cannot go that way.");
        }
    }

    private void moveEast() {
        if (location == 1) {
            location = 3;
            System.out.println("You move east.");
        } else {
            System.out.println("You cannot go that way.");
        }
    }

    private void moveWest() {
        if (location == 1) {
            location = 2;
            System.out.println("You move west.");
        } else {
            System.out.println("You cannot go that way.");
        }
    }

    private void examineLocation() {
        switch (location) {
            case 0:
                System.out.println("The cell is cold and damp. You see scratch marks on the walls.");
                break;
            case 1:
                System.out.println("The hallway is long and dark. You hear faint whispers.");
                break;
            case 2:
                System.out.println("The storage room is filled with dusty crates and barrels. Some are labeled with strange symbols.");
                break;
            case 3:
                System.out.println("The machine pulses with an eerie light. You feel a sense of dread.");
                break;
            default:
                System.out.println("You see nothing of note.");
                break;
        }
    }

    private void takeItem() {
        if (location == 1 && !hasKey) {
            hasKey = true;
            System.out.println("You picked up the key.");
        } else {
            System.out.println("There is nothing to take.");
        }
    }

    private void randomEvents() {
        if (random.nextInt(10) < 3) { // 30% chance of an event
            int event = random.nextInt(3);
            switch (event) {
                case 0:
                    System.out.println("A sudden noise startles you. Sanity -10.");
                    playerSanity -= 10;
                    break;
                case 1:
                    System.out.println("You feel a cold draft. Health -5.");
                    playerHealth -= 5;
                    break;
                case 2:
                    System.out.println("A shadow flickers in the corner of your eye.");
                    break;
                default:
                    break;
            }
        }
    }

    private void gameOver() {
        if (playerHealth <= 0) {
            System.out.println("You have succumbed to your injuries. Game over.");
        } else if (playerSanity <= 0) {
            System.out.println("You have lost your sanity. Game over.");
        } else {
            System.out.println("You have escaped... for now.");
        }
    }

    public static void main(String[] args) {
        SinisterAIHorrorGame game = new SinisterAIHorrorGame();
        game.startGame();
    }
}