import java.util.*;


public class HorrorThemedVirtualRealityEscape {

    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}

// Game Engine Class
class Game {
    private Scanner scanner;
    private Player player;
    private Map<String, Room> rooms;

    public Game() {
        scanner = new Scanner(System.in);
        player = new Player("Player1");
        rooms = new HashMap<>();
        initializeRooms();
    }

    private void initializeRooms() {
        Room entrance = new Room("Entrance Hall", "A dimly lit room with flickering lights.", false);
        Room library = new Room("Library", "Books scattered on the floor, whispers echo around.", true);
        Room basement = new Room("Basement", "Dark and damp, something moves in the shadows...", true);
        Room exit = new Room("Exit Door", "A locked door with a mysterious symbol.", false);

        entrance.setExits(library, null, basement, null);
        library.setExits(null, entrance, null, exit);
        basement.setExits(entrance, null, null, null);
        exit.setExits(null, library, null, null);

        rooms.put("Entrance Hall", entrance);
        rooms.put("Library", library);
        rooms.put("Basement", basement);
        rooms.put("Exit Door", exit);

        player.setCurrentRoom(entrance);
    }

    public void start() {
        System.out.println("Welcome to the Horror VR Escape Game!");
        while (!player.hasEscaped()) {
            System.out.println("\n" + player.getCurrentRoom().getDescription());
            if (player.getCurrentRoom().hasPuzzle()) {
                System.out.println("A puzzle blocks your way!");
                if (!solvePuzzle()) {
                    System.out.println("The room seems darker now...");
                    continue;
                }
            }
            System.out.println("What would you like to do? (move/search/quit)");
            String command = scanner.nextLine().trim().toLowerCase();
            processCommand(command);
        }
        System.out.println("Congratulations! You have escaped the haunted house.");
    }

    private boolean solvePuzzle() {
        System.out.println("Solve this riddle: I have keys but open no locks. What am I?");
        String answer = scanner.nextLine().trim().toLowerCase();
        return answer.equals("keyboard");
    }

    private void processCommand(String command) {
        switch (command) {
            case "move":
                movePlayer();
                break;
            case "search":
                searchRoom();
                break;
            case "quit":
                System.out.println("Game Over.");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid command.");
        }
    }

    private void movePlayer() {
        System.out.println("Where would you like to go? (north/south/east/west)");
        String direction = scanner.nextLine().trim().toLowerCase();
        Room nextRoom = player.getCurrentRoom().getExit(direction);
        if (nextRoom != null) {
            player.setCurrentRoom(nextRoom);
            System.out.println("You move to " + nextRoom.getName());
        } else {
            System.out.println("You cannot go that way!");
        }
    }

    private void searchRoom() {
        System.out.println("You search the room...");
        if (player.getCurrentRoom().getName().equals("Exit Door")) {
            System.out.println("You found the key! The exit door unlocks.");
            player.setEscaped(true);
        } else {
            System.out.println("Nothing but dust and whispers...");
        }
    }
}

// Player Class
class Player {
    private String name;
    private Room currentRoom;
    private boolean escaped;

    public Player(String name) {
        this.name = name;
        this.escaped = false;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(Room room) {
        this.currentRoom = room;
    }

    public boolean hasEscaped() {
        return escaped;
    }

    public void setEscaped(boolean escaped) {
        this.escaped = escaped;
    }
}

// Room Class
class Room {
    private String name;
    private String description;
    private boolean hasPuzzle;
    private Map<String, Room> exits;

    public Room(String name, String description, boolean hasPuzzle) {
        this.name = name;
        this.description = description;
        this.hasPuzzle = hasPuzzle;
        this.exits = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasPuzzle() {
        return hasPuzzle;
    }

    public void setExits(Room north, Room south, Room east, Room west) {
        if (north != null) exits.put("north", north);
        if (south != null) exits.put("south", south);
        if (east != null) exits.put("east", east);
        if (west != null) exits.put("west", west);
    }

    public Room getExit(String direction) {
        return exits.get(direction);
    }
}
