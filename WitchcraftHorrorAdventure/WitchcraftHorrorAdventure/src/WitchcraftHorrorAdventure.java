import java.util.Scanner;

public class WitchcraftHorrorAdventure {

    private static Scanner scanner = new Scanner(System.in);
    private static boolean hasCandle = false;
    private static boolean hasKey = false;
    private static boolean hasAmulet = false;
    private static int sanity = 100;
    private static String currentRoom = "Forest Clearing";

    public static void main(String[] args) {
        System.out.println("Welcome to the Witchcraft Horror Adventure!");
        System.out.println("You awaken in a dark, misty forest clearing.");
        playGame();
    }

    private static void playGame() {
        while (sanity > 0) {
            displayRoom();
            processInput();
            if (sanity <= 0) {
                System.out.println("Your sanity has completely shattered. The darkness consumes you.");
                System.out.println("Game Over.");
                return;
            }
            if (currentRoom.equals("Witch's Chamber") && hasAmulet) {
                System.out.println("The amulet pulses with power, the witch recoils and vanishes. You have escaped.");
                System.out.println("You have won!");
                return;
            }
        }
    }

    private static void displayRoom() {
        System.out.println("\n--- " + currentRoom + " ---");
        switch (currentRoom) {
            case "Forest Clearing":
                System.out.println("The air is thick with fog. Twisted trees surround you.");
                System.out.println("You see a path leading NORTH and a dark cave to the EAST.");
                break;
            case "Dark Cave":
                System.out.println("The cave is cold and damp. You hear dripping water.");
                System.out.println("You see a flickering light in the distance. And a small candle on the ground. You can go WEST to the clearing.");
                if (!hasCandle) {
                    System.out.println("A small, unlit candle lies on the ground.");
                }
                break;
            case "Old Cottage":
                System.out.println("A dilapidated cottage stands before you. The door is slightly ajar.");
                System.out.println("You can go SOUTH to the forest path, or enter the cottage.");
                break;
            case "Cottage Interior":
                System.out.println("The cottage is filled with cobwebs and strange symbols.");
                System.out.println("You see a locked chest and a staircase leading UP.");
                if (!hasKey) {
                    System.out.println("A rusty key lies on a table.");
                }
                break;
            case "Cottage Attic":
                System.out.println("The attic is dusty and filled with old books.");
                System.out.println("You see a strange, glowing amulet.");
                if (!hasAmulet) {
                    System.out.println("A glowing amulet rests on a pedestal.");
                }
                break;
            case "Forest Path":
                System.out.println("A narrow path winds through the dense forest.");
                System.out.println("You can go NORTH to the cottage, or SOUTH further into the forest.");
                break;
            case "Deeper Forest":
                System.out.println("The forest is much darker here. You feel a sense of dread.");
                System.out.println("You can go NORTH back to the forest path, or EAST to a strange clearing.");
                break;
            case "Strange Clearing":
                System.out.println("An unnatural silence fills this clearing. A stone altar stands in the center.");
                System.out.println("You see a dark path leading EAST, and you can go WEST back to the deeper forest.");
                break;
            case "Witch's Chamber":
                System.out.println("The air crackles with dark energy. A figure stands before you, it's the witch!");
                System.out.println("You can only go WEST back to the strange clearing.");
                break;
            default:
                System.out.println("You are lost.");
                break;
        }
        System.out.println("Sanity: " + sanity);
    }

    private static void processInput() {
        System.out.print("What do you do? ");
        String input = scanner.nextLine().toLowerCase();

        switch (currentRoom) {
            case "Forest Clearing":
                if (input.equals("north")) {
                    currentRoom = "Old Cottage";
                } else if (input.equals("east")) {
                    currentRoom = "Dark Cave";
                } else {
                    System.out.println("Invalid command.");
                }
                break;
            case "Dark Cave":
                if (input.equals("west")) {
                    currentRoom = "Forest Clearing";
                } else if (input.equals("take candle") && !hasCandle) {
                    hasCandle = true;
                    System.out.println("You take the candle.");
                } else {
                    System.out.println("Invalid command.");
                }
                break;
            case "Old Cottage":
                if (input.equals("south")) {
                    currentRoom = "Forest Path";
                } else if (input.equals("enter")) {
                    currentRoom = "Cottage Interior";
                } else {
                    System.out.println("Invalid command.");
                }
                break;
            case "Cottage Interior":
                if (input.equals("up")) {
                    currentRoom = "Cottage Attic";
                } else if (input.equals("take key") && !hasKey) {
                    hasKey = true;
                    System.out.println("You take the rusty key.");
                } else {
                    System.out.println("Invalid command.");
                }
                break;
            case "Cottage Attic":
                if (input.equals("take amulet") && !hasAmulet) {
                    hasAmulet = true;
                    System.out.println("You take the glowing amulet");
                } else {
                    System.out.println("Invalid command.");
                }
                break;
            case "Forest Path":
                if (input.equals("north")) {
                    currentRoom = "Old Cottage";
                } else if (input.equals("south")) {
                    currentRoom = "Deeper Forest";
                } else {
                    System.out.println("Invalid command.");
                }
                break;
            case "Deeper Forest":
                if (input.equals("north")) {
                    currentRoom = "Forest Path";
                } else if (input.equals("east")) {
                    currentRoom = "Strange Clearing";
                } else {
                    System.out.println("Invalid command.");
                }
                break;
            case "Strange Clearing":
                if (input.equals("west")) {
                    currentRoom = "Deeper Forest";
                } else if (input.equals("east")) {
                    currentRoom = "Witch's Chamber";
                } else {
                    System.out.println("Invalid command.");
                }
                break;
            case "Witch's Chamber":
                if (input.equals("west")) {
                    currentRoom = "Strange Clearing";
                } else {
                    System.out.println("Invalid command.");
                }
                break;
            default:
                System.out.println("You are lost.");
                break;
        }
        sanity -= 5; // Sanity decreases with each move.
    }
}