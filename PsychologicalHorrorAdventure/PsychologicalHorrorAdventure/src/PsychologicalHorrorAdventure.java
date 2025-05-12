
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class PsychologicalHorrorAdventure extends JFrame {
    private JTextArea textArea;
    private JTextField inputField;
    private JPanel imagePanel;
    private ImageIcon currentImage;
    private JLabel imageLabel;
    private List<String> inventory;
    private GameState currentState;
    private boolean hasFlashlight;
    private boolean knowsAboutFigure;
    private boolean hasMirror;
    private boolean hasKey;
    private boolean endingTriggered;
    private boolean sanityLow;
    private int sanityLevel;
    private Random random;

    // Game state enum
    private enum GameState {
        START, LIVING_ROOM, KITCHEN, BEDROOM, BASEMENT, ATTIC, HALLWAY, FINAL
    }

    public PsychologicalHorrorAdventure() {
        super("Shadows of the Mind");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // Initialize game variables
        inventory = new ArrayList<>();
        currentState = GameState.START;
        hasFlashlight = false;
        knowsAboutFigure = false;
        hasMirror = false;
        hasKey = false;
        endingTriggered = false;
        sanityLevel = 100;
        sanityLow = false;
        random = new Random();

        // Create components
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setBackground(Color.BLACK);
        textArea.setForeground(Color.WHITE);
        textArea.setFont(new Font("Courier New", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);

        inputField = new JTextField();
        inputField.setBackground(Color.BLACK);
        inputField.setForeground(Color.WHITE);
        inputField.setFont(new Font("Courier New", Font.PLAIN, 14));
        inputField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                processInput(inputField.getText());
                inputField.setText("");
            }
        });

        imagePanel = new JPanel();
        imagePanel.setBackground(Color.BLACK);
        currentImage = new ImageIcon(getClass().getResource("/images/start.jpg"));
        imageLabel = new JLabel(currentImage);
        imagePanel.add(imageLabel);

        // Add components to frame
        add(scrollPane, BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);
        add(imagePanel, BorderLayout.EAST);

        // Start the game
        displayStartScreen();

        setVisible(true);
    }

    private void displayStartScreen() {
        animateText("You wake up in an unfamiliar house. The air is thick with an unsettling presence.\n\n" +
                "The last thing you remember is driving home from work... but this is definitely not your home.\n\n" +
                "The walls whisper secrets in a language you almost understand. Shadows move when you're not looking.\n\n" +
                "Type 'help' for available commands at any time.\n\n" +
                "What do you do?", 20);

        currentState = GameState.LIVING_ROOM;
        changeImage("living_room.jpg");
    }

    private void animateText(String text, int delay) {
        new Thread(() -> {
            for (int i = 0; i <= text.length(); i++) {
                final String part = text.substring(0, i);
                SwingUtilities.invokeLater(() -> textArea.setText(part));
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void processInput(String input) {
        input = input.toLowerCase().trim();

        // Check for random sanity effects
        checkSanityEffects();

        // Process commands
        if (input.equals("help")) {
            showHelp();
        } else if (input.equals("look")) {
            lookAround();
        } else if (input.equals("inventory")) {
            showInventory();
        } else if (input.startsWith("go ")) {
            String direction = input.substring(3);
            move(direction);
        } else if (input.startsWith("take ")) {
            String item = input.substring(5);
            takeItem(item);
        } else if (input.startsWith("use ")) {
            String item = input.substring(4);
            useItem(item);
        } else if (input.equals("examine surroundings")) {
            examineSurroundings();
        } else if (input.equals("check sanity")) {
            checkSanity();
        } else {
            animateText("I don't understand that command. Type 'help' for available commands.", 20);
        }
    }

    private void showHelp() {
        animateText("Available commands:\n" +
                "----------------\n" +
                "help - Show this help message\n" +
                "look - Look around the current room\n" +
                "inventory - Show your inventory\n" +
                "go [direction] - Move in specified direction (north, south, east, west, up, down)\n" +
                "take [item] - Take an item\n" +
                "use [item] - Use an item from your inventory\n" +
                "examine surroundings - Carefully examine your environment\n" +
                "check sanity - Check your current mental state", 10);
    }

    private void lookAround() {
        switch (currentState) {
            case LIVING_ROOM:
                animateText("You're in a dimly lit living room. The furniture is covered in dusty sheets.\n" +
                        "A grandfather clock ticks irregularly in the corner. There are doors to the north (kitchen),\n" +
                        "east (hallway), and west (bedroom). A staircase leads up to the attic.", 20);
                break;
            case KITCHEN:
                animateText("The kitchen smells of spoiled food. Cabinets hang open, their contents spilled.\n" +
                        "A flickering light reveals a knife block with one missing knife. There's a door to\n" +
                        "the south (living room) and a basement door with a broken lock.", 20);
                break;
            case BEDROOM:
                animateText("The bedroom has an unmade bed with stained sheets. A dresser mirror is cracked.\n" +
                        "You notice a small key on the nightstand. The only exit is east (living room).", 20);
                break;
            case BASEMENT:
                animateText("The basement is pitch black. The air is damp and smells of earth and something metallic.\n" +
                        "You hear faint scratching sounds from the corners. The only light comes from the stairs\n" +
                        "leading back up to the kitchen.", 20);
                break;
            case ATTIC:
                animateText("The attic is filled with cobwebs and old trunks. A small window lets in moonlight,\n" +
                        "casting eerie shadows. There's a strange symbol drawn on the floor in what looks like\n" +
                        "dried blood. The only exit is down the stairs (living room).", 20);
                break;
            case HALLWAY:
                animateText("A long hallway with peeling wallpaper. Family portraits hang crookedly, their faces\n" +
                        "scratched out. At the end of the hallway is a locked door. Exits are west (living room)\n" +
                        "and the locked door to the east.", 20);
                break;
            default:
                animateText("You see nothing but darkness.", 20);
        }
    }

    private void showInventory() {
        if (inventory.isEmpty()) {
            animateText("Your inventory is empty.", 20);
        } else {
            StringBuilder sb = new StringBuilder("You are carrying:\n");
            for (String item : inventory) {
                sb.append("- ").append(item).append("\n");
            }
            animateText(sb.toString(), 20);
        }
    }

    private void move(String direction) {
        switch (currentState) {
            case LIVING_ROOM:
                if (direction.equals("north")) {
                    currentState = GameState.KITCHEN;
                    changeImage("kitchen.jpg");
                    animateText("You enter the kitchen.", 20);
                } else if (direction.equals("west")) {
                    currentState = GameState.BEDROOM;
                    changeImage("bedroom.jpg");
                    animateText("You enter the bedroom.", 20);
                } else if (direction.equals("east")) {
                    currentState = GameState.HALLWAY;
                    changeImage("hallway.jpg");
                    animateText("You enter the hallway.", 20);
                } else if (direction.equals("up")) {
                    currentState = GameState.ATTIC;
                    changeImage("attic.jpg");
                    animateText("You climb the creaky stairs to the attic.", 20);
                } else {
                    animateText("You can't go that way.", 20);
                }
                break;
            case KITCHEN:
                if (direction.equals("south")) {
                    currentState = GameState.LIVING_ROOM;
                    changeImage("living_room.jpg");
                    animateText("You return to the living room.", 20);
                } else if (direction.equals("down")) {
                    currentState = GameState.BASEMENT;
                    changeImage("basement.jpg");
                    animateText("You descend into the dark basement.", 20);
                    if (!hasFlashlight) {
                        animateText("\nIt's too dark to see anything! You might need a light source.", 30);
                    }
                } else {
                    animateText("You can't go that way.", 20);
                }
                break;
            case BEDROOM:
                if (direction.equals("east")) {
                    currentState = GameState.LIVING_ROOM;
                    changeImage("living_room.jpg");
                    animateText("You return to the living room.", 20);
                } else {
                    animateText("You can't go that way.", 20);
                }
                break;
            case BASEMENT:
                if (direction.equals("up")) {
                    currentState = GameState.KITCHEN;
                    changeImage("kitchen.jpg");
                    animateText("You return to the kitchen.", 20);
                } else {
                    animateText("You can't go that way.", 20);
                }
                break;
            case ATTIC:
                if (direction.equals("down")) {
                    currentState = GameState.LIVING_ROOM;
                    changeImage("living_room.jpg");
                    animateText("You return to the living room.", 20);
                } else {
                    animateText("You can't go that way.", 20);
                }
                break;
            case HALLWAY:
                if (direction.equals("west")) {
                    currentState = GameState.LIVING_ROOM;
                    changeImage("living_room.jpg");
                    animateText("You return to the living room.", 20);
                } else if (direction.equals("east")) {
                    if (hasKey) {
                        currentState = GameState.FINAL;
                        changeImage("final_room.jpg");
                        triggerEnding();
                    } else {
                        animateText("The door is locked. You need a key.", 20);
                    }
                } else {
                    animateText("You can't go that way.", 20);
                }
                break;
            default:
                animateText("You can't move anymore...", 20);
        }

        // Random sanity check after moving
        if (random.nextInt(100) < 30) {
            sanityCheck();
        }
    }

    private void takeItem(String item) {
        switch (currentState) {
            case BEDROOM:
                if (item.equals("key") && !hasKey) {
                    hasKey = true;
                    inventory.add("small key");
                    animateText("You take the small key from the nightstand.", 20);
                } else if (item.equals("mirror") && !hasMirror) {
                    hasMirror = true;
                    inventory.add("broken mirror");
                    animateText("You carefully take the broken mirror from the dresser.", 20);
                } else {
                    animateText("You can't take that.", 20);
                }
                break;
            case KITCHEN:
                if (item.equals("knife")) {
                    inventory.add("knife");
                    animateText("You take a knife from the block. It feels cold in your hand.", 20);
                    sanityLevel -= 10; // Taking a weapon reduces sanity
                } else if (item.equals("flashlight") && !hasFlashlight) {
                    hasFlashlight = true;
                    inventory.add("flashlight");
                    animateText("You find a working flashlight in a drawer!", 20);
                } else {
                    animateText("You can't take that.", 20);
                }
                break;
            default:
                animateText("There's nothing like that to take here.", 20);
        }
    }

    private void useItem(String item) {
        if (!inventory.contains(item)) {
            animateText("You don't have that item.", 20);
            return;
        }

        switch (item) {
            case "flashlight":
                if (currentState == GameState.BASEMENT) {
                    animateText("You turn on the flashlight. The beam reveals disturbing markings on the walls\n" +
                            "and what looks like dried blood on the floor. In the corner, you see a shadow\n" +
                            "that moves just outside the light's reach.", 30);
                    knowsAboutFigure = true;
                    sanityLevel -= 15;
                } else {
                    animateText("You turn on the flashlight, but it doesn't reveal anything new here.", 20);
                }
                break;
            case "broken mirror":
                animateText("You look into the broken mirror. For a moment, you see a figure standing behind you,\n" +
                        "but when you turn around, no one is there. Your reflection seems to smile at you.", 30);
                knowsAboutFigure = true;
                sanityLevel -= 20;
                break;
            case "knife":
                animateText("Holding the knife makes you feel paranoid. You keep seeing movement in your periphery.", 20);
                sanityLevel -= 5;
                break;
            case "small key":
                if (currentState == GameState.HALLWAY) {
                    animateText("You use the key on the locked door. It turns with a satisfying click.", 20);
                } else {
                    animateText("There's nothing to use the key on here.", 20);
                }
                break;
            default:
                animateText("Using that doesn't do anything here.", 20);
        }

        sanityCheck();
    }

    private void examineSurroundings() {
        switch (currentState) {
            case LIVING_ROOM:
                animateText("Looking closer, you notice the clock's hands are moving backwards.\n" +
                        "The dust on the furniture forms strange patterns, almost like fingerprints.\n" +
                        "One of the sheet-covered chairs looks like someone is sitting in it...", 30);
                sanityLevel -= 5;
                break;
            case KITCHEN:
                animateText("Examining the kitchen reveals the spoiled food is moving slightly,\n" +
                        "as if something is living inside it. The missing knife slot has dark stains.\n" +
                        "The basement door has scratch marks on the inside.", 30);
                sanityLevel -= 10;
                break;
            case BEDROOM:
                animateText("The stains on the sheets form a disturbing pattern when examined closely.\n" +
                        "The cracked mirror seems to reflect the room differently than it appears.\n" +
                        "You hear faint whispering when you put your ear to the wall.", 30);
                sanityLevel -= 15;
                break;
            case BASEMENT:
                if (hasFlashlight) {
                    animateText("With the flashlight, you see the walls are covered in strange symbols.\n" +
                            "The scratching sounds grow louder. Something is written in what looks like blood:\n" +
                            "'IT WATCHES FROM THE MIRRORS'", 40);
                    knowsAboutFigure = true;
                    sanityLevel -= 25;
                } else {
                    animateText("It's too dark to examine anything properly.", 20);
                }
                break;
            case ATTIC:
                animateText("The symbol on the floor seems to pulse when you stare at it.\n" +
                        "Opening one of the trunks reveals old photographs with the faces torn out.\n" +
                        "The shadows in the corner seem deeper than they should be.", 30);
                sanityLevel -= 20;
                break;
            case HALLWAY:
                animateText("Looking closely at the portraits, you realize they're of people\n" +
                        "with their eyes sewn shut. The wallpaper isn't peeling - it's covered\n" +
                        "in tiny handprints. The locked door has a nameplate that reads 'YOUR ROOM'.", 40);
                sanityLevel -= 15;
                break;
            default:
                animateText("There's nothing more to see.", 20);
        }

        sanityCheck();
    }

    private void checkSanity() {
        String sanityStatus;
        if (sanityLevel > 75) {
            sanityStatus = "You feel relatively calm, though uneasy.";
        } else if (sanityLevel > 50) {
            sanityStatus = "Your hands are shaking slightly. The shadows seem to move.";
        } else if (sanityLevel > 25) {
            sanityStatus = "You're breathing heavily. The whispers are getting louder.";
            sanityLow = true;
        } else {
            sanityStatus = "Reality is fraying at the edges. You can't trust your own eyes.";
            sanityLow = true;
        }

        animateText("Sanity Level: " + sanityLevel + "/100\n" + sanityStatus, 20);
    }

    private void sanityCheck() {
        if (sanityLevel <= 0 && !endingTriggered) {
            endingTriggered = true;
            animateText("\n\nYour mind finally breaks. The figure in the mirror steps out and reaches for you.\n" +
                    "As its cold hands close around your throat, you realize it was you all along...\n\n" +
                    "GAME OVER - INSANITY ENDING", 50);
            currentState = GameState.FINAL;
            changeImage("insanity_ending.jpg");
        } else if (sanityLevel <= 25 && !sanityLow) {
            sanityLow = true;
            animateText("\nThe walls are breathing. You hear your name whispered from every direction.\n" +
                    "Something is very wrong here. You need to find a way out soon!", 30);
        }
    }

    private void checkSanityEffects() {
        if (sanityLow && random.nextInt(100) < 20) {
            int effect = random.nextInt(3);
            switch (effect) {
                case 0:
                    animateText("\nYou see a shadow move quickly across the room!", 20);
                    break;
                case 1:
                    animateText("\nA cold hand brushes against your neck, but no one is there.", 20);
                    break;
                case 2:
                    animateText("\nThe lights flicker violently for a moment.", 20);
                    break;
            }
            sanityLevel -= 5;
            sanityCheck();
        }
    }

    private void triggerEnding() {
        animateText("\n\nYou unlock the door and step inside. The room is empty except for a large mirror.\n" +
                "As you approach, you see your reflection... but it's not moving with you.\n\n" +
                "The reflection smiles and steps forward, reaching through the glass.\n" +
                "In your final moments, you understand - you were never really here at all.\n\n" +
                "GAME OVER - TRUTH ENDING", 50);
    }

    private void changeImage(String imageName) {
        // In a real implementation, this would load the image from resources
        // For this example, we'll just simulate it
        currentImage = new ImageIcon("images/" + imageName);
        imageLabel.setIcon(currentImage);
        imagePanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PsychologicalHorrorAdventure();
        });
    }
}