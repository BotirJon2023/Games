import java.util.Scanner;
import java.util.Random;
import java.util.ArrayList;

class Puzzle {
    private String question;
    private String answer;
    private String hint;

    public Puzzle(String question, String answer, String hint) {
        this.question = question;
        this.answer = answer;
        this.hint = hint;
    }

    public boolean checkAnswer(String input) {
        return input.trim().equalsIgnoreCase(answer);
    }

    public String getQuestion() {
        return question;
    }

    public String getHint() {
        return hint;
    }
}

class Inventory {
    private ArrayList<String> items;

    public Inventory() {
        items = new ArrayList<>();
    }

    public void addItem(String item) {
        items.add(item);
    }

    public void showItems() {
        if (items.isEmpty()) {
            System.out.println("Your inventory is empty.");
        } else {
            System.out.println("Your inventory contains:");
            for (String item : items) {
                System.out.println("- " + item);
            }
        }
    }

    public boolean hasItem(String item) {
        return items.contains(item);
    }
}

class Game {
    private static final String WELCOME_MESSAGE = "Welcome to the Horror-Based Puzzle Game!";
    private static final String GAME_OVER_MESSAGE = "Game Over. You have succumbed to the horrors of the puzzle world.";
    private static final String VICTORY_MESSAGE = "Congratulations! You have solved the final puzzle and escaped the horror!";

    private static final Puzzle[] puzzles = {
            new Puzzle("What is the name of the haunted mansion?", "Blackwood", "It starts with B."),
            new Puzzle("How many windows are in the attic?", "7", "Think of the number of days in a week."),
            new Puzzle("Who was the first victim in the mansion?", "Sarah", "She was the maid."),
            new Puzzle("Which room is haunted?", "Library", "It has lots of old books."),
            new Puzzle("What color is the ghost's cloak?", "Red", "It's a blood-red cloak.")
    };

    private int puzzleIndex = 0;
    private boolean gameRunning = true;
    private Inventory inventory = new Inventory();
    private Random rand = new Random();

    public void start() {
        Scanner scanner = new Scanner(System.in);
        System.out.println(WELCOME_MESSAGE);

        // Simulate an event where the player picks up an item
        inventory.addItem("Mysterious Key");

        while (gameRunning && puzzleIndex < puzzles.length) {
            Puzzle currentPuzzle = puzzles[puzzleIndex];
            System.out.println("\nPuzzle " + (puzzleIndex + 1) + ":");
            System.out.println(currentPuzzle.getQuestion());
            System.out.println("Type 'hint' for a hint or 'inventory' to check your inventory.");

            String playerAnswer = scanner.nextLine();

            if (playerAnswer.equalsIgnoreCase("hint")) {
                System.out.println("Hint: " + currentPuzzle.getHint());
                continue;
            }

            if (playerAnswer.equalsIgnoreCase("inventory")) {
                inventory.showItems();
                continue;
            }

            if (currentPuzzle.checkAnswer(playerAnswer)) {
                System.out.println("Correct! You can proceed.");
                puzzleIndex++;
                if (rand.nextInt(5) == 0) { // Random event
                    triggerRandomEvent();
                }
            } else {
                System.out.println("Wrong answer... something seems to be moving in the shadows...");
                gameRunning = false; // Game over on wrong answer
            }
        }

        if (gameRunning) {
            System.out.println(VICTORY_MESSAGE);
        } else {
            System.out.println(GAME_OVER_MESSAGE);
        }
    }

    private void triggerRandomEvent() {
        int event = rand.nextInt(3);
        switch (event) {
            case 0:
                System.out.println("You hear footsteps behind you... something is watching you.");
                break;
            case 1:
                System.out.println("A sudden gust of wind blows through the room, extinguishing the candlelight.");
                break;
            case 2:
                System.out.println("The air grows cold, and you see your breath in the chill.");
                break;
        }
    }
}

class HorrorBasedPuzzleGame {
    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}
