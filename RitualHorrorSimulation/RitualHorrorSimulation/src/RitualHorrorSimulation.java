import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

// Hauptklasse des Spiels
public class RitualHorrorSimulation {
    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}

// Klasse für das Spiel, steuert die Hauptschleife
class Game {
    private Player player;
    private Map<String, Room> rooms;
    private Scanner scanner;
    private boolean gameRunning;
    private EventHandler eventHandler;

    public Game() {
        // Initialisierung des Spiels
        scanner = new Scanner(System.in);
        player = new Player("Abenteurer", 100, 0);
        rooms = new HashMap<>();
        eventHandler = new EventHandler();
        gameRunning = true;
        initializeRooms();
    }

    // Initialisiert die Räume des Herrenhauses
    private void initializeRooms() {
        // Eingangshalle
        Room entrance = new Room("Eingangshalle",
                "Eine düstere Halle mit knarrendem Boden. Ein kalter Wind weht durch die Fenster.");
        entrance.addItem(new Item("Kerze", "Eine alte Kerze, die schwach leuchtet."));
        entrance.addExit("Bibliothek", "Norden");
        entrance.addExit("Keller", "Süden");

        // Bibliothek
        Room library = new Room("Bibliothek",
                "Regale voller staubiger Bücher. Ein seltsames Flüstern erfüllt den Raum.");
        library.addItem(new Item("Altes Buch", "Ein Buch über okkulte Rituale."));
        library.addExit("Eingangshalle", "Süden");
        library.addExit("Ritualkammer", "Osten");

        // Keller
        Room basement = new Room("Keller",
                "Ein feuchter, modriger Raum. Etwas scheint in der Dunkelheit zu lauern.");
        basement.addItem(new Item("Schlüssel", "Ein rostiger Schlüssel."));
        basement.addExit("Eingangshalle", "Norden");

        // Ritualkammer
        Room ritualChamber = new Room("Ritualkammer",
                "Ein Pentagramm ist auf den Boden gezeichnet. Die Luft ist schwer vor Magie.");
        ritualChamber.addItem(new Item("Amulet", "Ein Amulett, das vor bösen Kräften schützt."));
        ritualChamber.addExit("Bibliothek", "Westen");
        ritualChamber.setPuzzle(new RitualPuzzle());

        // Räume zur Karte hinzufügen
        rooms.put("Eingangshalle", entrance);
        rooms.put("Bibliothek", library);
        rooms.put("Keller", basement);
        rooms.put("Ritualkammer", ritualChamber);
    }

    // Startet das Spiel
    public void start() {
        System.out.println("Willkommen bei RitualHorrorSimulation!");
        System.out.println("Du bist in einem verlassenen Herrenhaus, um ein dunkles Ritual zu stoppen.");
        System.out.println("Befehle: gehen <Richtung>, nehmen <Gegenstand>, benutzen <Gegenstand>, inventar, untersuchen, beenden");

        String currentRoom = "Eingangshalle";
        while (gameRunning) {
            Room room = rooms.get(currentRoom);
            System.out.println("\n" + room.getDescription());
            System.out.println("Gegenstände hier: " + room.getItems());
            System.out.println("Ausgänge: " + room.getExits());
            System.out.println("Dein Zustand: Gesundheit=" + player.getHealth() + ", Angst=" + player.getFear());

            // Zufälliges Ereignis auslösen
            eventHandler.triggerEvent(player);

            // Spieleraktionen verarbeiten
            System.out.print("\nWas möchtest du tun? ");
            String input = scanner.nextLine().toLowerCase();
            String[] command = input.split(" ", 2);

            try {
                switch (command[0]) {
                    case "gehen":
                        currentRoom = handleMove(room, command[1], currentRoom);
                        break;
                    case "nehmen":
                        handleTake(room, command[1]);
                        break;
                    case "benutzen":
                        handleUse(room, command[1]);
                        break;
                    case "inventar":
                        System.out.println("Inventar: " + player.getInventory());
                        break;
                    case "untersuchen":
                        System.out.println(room.getDetailedDescription());
                        break;
                    case "beenden":
                        gameRunning = false;
                        System.out.println("Spiel beendet.");
                        break;
                    default:
                        System.out.println("Unbekannter Befehl.");
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Bitte gib einen vollständigen Befehl ein.");
            }
        }
        scanner.close();
    }

    // Behandelt die Bewegung des Spielers
    private String handleMove(Room room, String direction, String currentRoom) {
        String destination = room.getExit(direction);
        if (destination != null) {
            System.out.println("Du gehst nach " + destination + ".");
            return destination;
        } else {
            System.out.println("Dorthin kannst du nicht gehen.");
            return currentRoom;
        }
    }

    // Behandelt das Aufnehmen von Gegenständen
    private void handleTake(Room room, String itemName) {
        Item item = room.removeItem(itemName);
        if (item != null) {
            player.addItem(item);
            System.out.println("Du hast " + itemName + " aufgenommen.");
        } else {
            System.out.println("Hier gibt es keinen " + itemName + ".");
        }
    }

    // Behandelt die Benutzung von Gegenständen
    private void handleUse(Room room, String itemName) {
        Item item = player.getItem(itemName);
        if (item != null) {
            if (room.getPuzzle() != null && room.getPuzzle().solve(item)) {
                System.out.println("Du hast das Ritual gestoppt! Du hast gewonnen!");
                gameRunning = false;
            } else {
                System.out.println("Das hat keinen Effekt.");
            }
        } else {
            System.out.println("Du hast keinen " + itemName + ".");
        }
    }
}

// Klasse für den Spieler
class Player {
    private String name;
    private int health;
    private int fear;
    private List<Item> inventory;

    public Player(String name, int health, int fear) {
        this.name = name;
        this.health = health;
        this.fear = fear;
        this.inventory = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
        if (this.health <= 0) {
            System.out.println("Du bist gestorben! Spiel vorbei.");
            System.exit(0);
        }
    }

    public int getFear() {
        return fear;
    }

    public void setFear(int fear) {
        this.fear = fear;
        if (this.fear >= 100) {
            System.out.println("Deine Angst überwältigt dich! Spiel vorbei.");
            System.exit(0);
        }
    }

    public void addItem(Item item) {
        inventory.add(item);
    }

    public Item getItem(String itemName) {
        for (Item item : inventory) {
            if (item.getName().equalsIgnoreCase(itemName)) {
                return item;
            }
        }
        return null;
    }

    public List<Item> getInventory() {
        return inventory;
    }
}

// Klasse für Räume
class Room {
    private String name;
    private String description;
    private List<Item> items;
    private Map<String, String> exits;
    private RitualPuzzle puzzle;

    public Room(String name, String description) {
        this.name = name;
        this.description = description;
        this.items = new ArrayList<>();
        this.exits = new HashMap<>();
        this.puzzle = null;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public Item removeItem(String itemName) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getName().equalsIgnoreCase(itemName)) {
                return items.remove(i);
            }
        }
        return null;
    }

    public void addExit(String direction, String destination) {
        exits.put(direction.toLowerCase(), destination);
    }

    public String getExit(String direction) {
        return exits.get(direction.toLowerCase());
    }

    public String getDescription() {
        return "[" + name + "] " + description;
    }

    public String getDetailedDescription() {
        return "Du schaust dich um: " + description + " Du siehst: " + items + ". Ausgänge: " + exits.keySet();
    }

    public List<Item> getItems() {
        return items;
    }

    public Map<String, String> getExits() {
        return exits;
    }

    public void setPuzzle(RitualPuzzle puzzle) {
        this.puzzle = puzzle;
    }

    public RitualPuzzle getPuzzle() {
        return puzzle;
    }
}

// Klasse für Gegenstände
class Item {
    private String name;
    private String description;

    public Item(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }
}

// Klasse für zufällige Ereignisse
class EventHandler {
    private Random random;
    private String[] events;

    public EventHandler() {
        random = new Random();
        events = new String[] {
                "Ein kalter Windhauch lässt dich erschaudern. Deine Angst steigt um 10.",
                "Du hörst ein Knarren hinter dir, aber da ist nichts. Angst +5.",
                "Ein Schatten huscht über die Wand. Angst +15.",
                "Etwas streift deinen Arm in der Dunkelheit. Gesundheit -10.",
                "Ein leises Flüstern ruft deinen Namen. Angst +20.",
                "Nichts passiert... diesmal."
        };
    }

    public void triggerEvent(Player player) {
        if (random.nextInt(100) < 30) { // 30% Chance für ein Ereignis
            String event = events[random.nextInt(events.length)];
            System.out.println("*** " + event + " ***");
            if (event.contains("Angst +")) {
                int fearIncrease = Integer.parseInt(event.split("Angst \\+")[1].split("\\.")[0]);
                player.setFear(player.getFear() + fearIncrease);
            } else if (event.contains("Gesundheit -")) {
                int healthDecrease = Integer.parseInt(event.split("Gesundheit -")[1].split("\\.")[0]);
                player.setHealth(player.getHealth() - healthDecrease);
            }
        }
    }
}

// Klasse für das Ritualrätsel
class RitualPuzzle {
    private boolean solved;
    private String solutionItem;

    public RitualPuzzle() {
        this.solved = false;
        this.solutionItem = "Amulet";
    }

    public boolean solve(Item item) {
        if (item != null && item.getName().equalsIgnoreCase(solutionItem)) {
            solved = true;
            return true;
        }
        return false;
    }

    public boolean isSolved() {
        return solved;
    }
}