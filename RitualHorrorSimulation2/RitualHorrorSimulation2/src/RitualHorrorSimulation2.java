import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.scene.input.KeyCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// Main JavaFX application
public class RitualHorrorSimulation extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        GameController game = new GameController();
        Scene scene = new Scene(game.getRoot(), 800, 600);
        scene.setOnKeyPressed(event -> game.handleInput(event.getCode()));
        primaryStage.setTitle("Ritual Horror Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();
        game.startGameLoop();
    }
}

// Controls game logic and rendering
class GameController {
    private Pane root;
    private Player player;
    private Map<String, Room> rooms;
    private String currentRoomName;
    private EventHandler eventHandler;
    private Text statusText;
    private boolean gameRunning;
    private Random random;

    public GameController() {
        root = new Pane();
        root.setStyle("-fx-background-color: black;");
        player = new Player("Adventurer", 100, 0, 400, 300);
        rooms = new HashMap<>();
        eventHandler = new EventHandler();
        statusText = new Text(10, 20, "Explore the mansion...");
        statusText.setFont(Font.font("Verdana", 16));
        statusText.setFill(Color.WHITE);
        gameRunning = true;
        random = new Random();
        initializeRooms();
        currentRoomName = "Entrance Hall";
        root.getChildren().addAll(statusText, player.getSprite());
        updateRoom();
    }

    // Initialize rooms with items and exits
    private void initializeRooms() {
        // Entrance Hall
        Room entrance = new Room("Entrance Hall",
                new Rectangle(0, 0, 800, 600, Color.DARKGRAY), // Placeholder background
                "A gloomy hall with creaking floors.");
        entrance.addItem(new Item("Candle", "A flickering candle.", 200, 200));
        entrance.addExit("North", "Library");
        entrance.addExit("South", "Basement");

        // Library
        Room library = new Room("Library",
                new Rectangle(0, 0, 800, 600, Color.DARKGREEN),
                "Shelves of dusty books whisper secrets.");
        library.addItem(new Item("Book", "An occult ritual book.", 300, 300));
        library.addExit("South", "Entrance Hall");
        library.addExit("East", "Ritual Chamber");

        // Basement
        Room basement = new Room("Basement",
                new Rectangle(0, 0, 800, 600, Color.DARKBLUE),
                "A damp, moldy room with lurking shadows.");
        basement.addItem(new Item("Key", "A rusty key.", 400, 400));
        basement.addExit("North", "Entrance Hall");

        // Ritual Chamber
        Room ritualChamber = new Room("Ritual Chamber",
                new Rectangle(0, 0, 800, 600, Color.DARKRED),
                "A pentagram glows ominously.");
        ritualChamber.addItem(new Item("Amulet", "A protective amulet.", 500, 500));
        ritualChamber.addExit("West", "Library");
        ritualChamber.setPuzzle(new RitualPuzzle());

        rooms.put("Entrance Hall", entrance);
        rooms.put("Library", library);
        rooms.put("Basement", basement);
        rooms.put("Ritual Chamber", ritualChamber);
    }

    // Update room visuals
    private void updateRoom() {
        Room room = rooms.get(currentRoomName);
        root.getChildren().clear();
        root.getChildren().add(room.getBackground());
        for (Item item : room.getItems()) {
            root.getChildren().add(item.getSprite());
        }
        root.getChildren().addAll(statusText, player.getSprite());
        animateBackground(room.getBackground());
        statusText.setText(room.getDescription() + "\nHealth: " + player.getHealth() +
                " | Fear: " + player.getFear() + "\nInventory: " + player.getInventory());
    }

    // Animate background (flickering effect)
    private void animateBackground(Rectangle background) {
        FadeTransition flicker = new FadeTransition(Duration.seconds(1), background);
        flicker.setFromValue(1.0);
        flicker.setToValue(0.7);
        flicker.setCycleCount(Timeline.INDEFINITE);
        flicker.setAutoReverse(true);
        flicker.play();
    }

    // Handle keyboard input
    public void handleInput(KeyCode code) {
        Room room = rooms.get(currentRoomName);
        switch (code) {
            case W: movePlayer(0, -10); break;
            case S: movePlayer(0, 10); break;
            case A: movePlayer(-10, 0); break;
            case D: movePlayer(10, 0); break;
            case E: tryPickupItem(room); break;
            case U: tryUseItem(room); break;
            case N: tryMoveRoom("North"); break;
            case S: tryMoveRoom("South"); break;
            case E: tryMoveRoom("East"); break;
            case W: tryMoveRoom("West"); break;
            case Q: gameRunning = false; System.exit(0); break;
        }
    }

    // Move player with animation
    private void movePlayer(double dx, double dy) {
        double newX = player.getX() + dx;
        double newY = player.getY() + dy;
        if (newX >= 0 && newX <= 750 && newY >= 50 && newY <= 550) {
            TranslateTransition move = new TranslateTransition(Duration.millis(100), player.getSprite());
            move.setToX(newX - player.getSprite().getX());
            move.setToY(newY - player.getSprite().getY());
            move.play();
            player.setPosition(newX, newY);
        }
    }

    // Attempt to pick up an item
    private void tryPickupItem(Room room) {
        for (Item item : room.getItems()) {
            if (Math.abs(player.getX() - item.getX()) < 50 && Math.abs(player.getY() - item.getY()) < 50) {
                player.addItem(item);
                room.removeItem(item);
                FadeTransition fade = new FadeTransition(Duration.millis(500), item.getSprite());
                fade.setToValue(0);
                fade.setOnFinished(e -> root.getChildren().remove(item.getSprite()));
                fade.play();
                statusText.setText("Picked up: " + item.getName());
                updateRoom();
                return;
            }
        }
        statusText.setText("Nothing to pick up here.");
    }

    // Attempt to use an item
    private void tryUseItem(Room room) {
        if (room.getPuzzle() != null) {
            for (Item item : player.getInventory()) {
                if (room.getPuzzle().solve(item)) {
                    statusText.setText("You stopped the ritual! You win!");
                    gameRunning = false;
                    return;
                }
            }
        }
        statusText.setText("No effect.");
    }

    // Move to another room
    private void tryMoveRoom(String direction) {
        Room room = rooms.get(currentRoomName);
        String destination = room.getExit(direction.toLowerCase());
        if (destination != null) {
            currentRoomName = destination;
            player.setPosition(400, 300); // Reset position
            updateRoom();
            statusText.setText("Entered: " + destination);
        } else {
            statusText.setText("Can't go that way.");
        }
    }

    // Game loop for random events
    public void startGameLoop() {
        Timeline gameLoop = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (gameRunning && random.nextInt(100) < 30) {
                eventHandler.triggerEvent(player, root, statusText);
            }
        }));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        gameLoop.play();
    }

    public Pane getRoot() {
        return root;
    }
}

// Player class
class Player {
    private String name;
    private int health;
    private int fear;
    private List<Item> inventory;
    private Rectangle sprite;
    private double x, y;

    public Player(String name, int health, int fear, double x, double y) {
        this.name = name;
        this.health = health;
        this.fear = fear;
        this.inventory = new ArrayList<>();
        this.x = x;
        this.y = y;
        this.sprite = new Rectangle(x, y, 30, 30, Color.BLUE); // Placeholder sprite
    }

    public void addItem(Item item) {
        inventory.add(item);
    }

    public List<Item> getInventory() {
        return inventory;
    }

    public Rectangle getSprite() {
        return sprite;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        sprite.setX(x);
        sprite.setY(y);
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
        if (health <= 0) {
            System.exit(0); // Game over
        }
    }

    public int getFear() {
        return fear;
    }

    public void setFear(int fear) {
        this.fear = fear;
        if (fear >= 100) {
            System.exit(0); // Game over
        }
    }
}

// Room class
class Room {
    private String name;
    private Rectangle background;
    private String description;
    private List<Item> items;
    private Map<String, String> exits;
    private RitualPuzzle puzzle;

    public Room(String name, Rectangle background, String description) {
        this.name = name;
        this.background = background;
        this.description = description;
        this.items = new ArrayList<>();
        this.exits = new HashMap<>();
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    public List<Item> getItems() {
        return items;
    }

    public void addExit(String direction, String destination) {
        exits.put(direction.toLowerCase(), destination);
    }

    public String getExit(String direction) {
        return exits.get(direction);
    }

    public String getDescription() {
        return description;
    }

    public Rectangle getBackground() {
        return background;
    }

    public void setPuzzle(RitualPuzzle puzzle) {
        this.puzzle = puzzle;
    }

    public RitualPuzzle getPuzzle() {
        return puzzle;
    }
}

// Item class
class Item {
    private String name;
    private String description;
    private Rectangle sprite;
    private double x, y;

    public Item(String name, String description, double x, double y) {
        this.name = name;
        this.description = description;
        this.x = x;
        this.y = y;
        this.sprite = new Rectangle(x, y, 20, 20, Color.YELLOW); // Placeholder
    }

    public String getName() {
        return name;
    }

    public Rectangle getSprite() {
        return sprite;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return name;
    }
}

// Event handler for random horror events
class EventHandler {
    private Random random = new Random();
    private String[] events = {
            "A cold wind chills you. Fear +10.",
            "Creaking behind you. Fear +5.",
            "A shadow darts across. Fear +15.",
            "Something brushes you. Health -10.",
            "Whispers call your name. Fear +20."
    };

    public void triggerEvent(Player player, Pane root, Text statusText) {
        String event = events[random.nextInt(events.length)];
        statusText.setText(event);

        // Animate a shadow effect
        Rectangle shadow = new Rectangle(0, 0, 800, 600, Color.BLACK);
        shadow.setOpacity(0.5);
        root.getChildren().add(shadow);
        FadeTransition fade = new FadeTransition(Duration.seconds(2), shadow);
        fade.setFromValue(0.5);
        fade.setToValue(0);
        fade.setOnFinished(e -> root.getChildren().remove(shadow));
        fade.play();

        // Update player stats
        if (event.contains("Fear +")) {
            int fear = Integer.parseInt(event.split("Fear \\+")[1].split("\\.")[0]);
            player.setFear(player.getFear() + fear);
        } else if (event.contains("Health -")) {
            int damage = Integer.parseInt(event.split("Health -")[1].split("\\.")[0]);
            player.setHealth(player.getHealth() - damage);
        }
    }
}

// Puzzle to stop the ritual
class RitualPuzzle {
    private String solutionItem = "Amulet";

    public boolean solve(Item item) {
        return item != null && item.getName().equalsIgnoreCase(solutionItem);
    }
}