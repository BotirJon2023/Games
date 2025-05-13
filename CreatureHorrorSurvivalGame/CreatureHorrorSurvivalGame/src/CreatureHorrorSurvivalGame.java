
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.animation.AnimationTimer;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.media.AudioClip;
import javafx.geometry.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class CreatureHorrorSurvival extends Application {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PLAYER_SIZE = 40;
    private static final int CREATURE_SIZE = 50;
    private static final int ITEM_SIZE = 20;
    private static final double PLAYER_SPEED = 5.0;
    private static final double CREATURE_SPEED = 2.0;
    private static final int MAX_CREATURES = 5;
    private static final int MAX_ITEMS = 10;

    private Pane pane;
    private Canvas canvas;
    private GraphicsContext gc;
    private boolean gameOver = false;
    private boolean gameWon = false;
    private int score = 0;
    private int health = 100;
    private long lastCreatureSpawn = 0;
    private long lastItemSpawn = 0;
    private Random random = new Random();

    // Player variables
    private double playerX = WIDTH / 2;
    private double playerY = HEIGHT / 2;
    private boolean upPressed, downPressed, leftPressed, rightPressed;
    private Image playerImage;
    private int playerFrame = 0;
    private long lastPlayerFrameUpdate = 0;
    private static final int PLAYER_ANIMATION_FRAMES = 4;
    private static final long PLAYER_ANIMATION_INTERVAL = 150_000_000;

    // Game entities
    private ArrayList<Creature> creatures = new ArrayList<>();
    private ArrayList<Item> items = new ArrayList<>();
    private Image creatureImage;
    private Image itemImage;
    private int creatureFrame = 0;
    private long lastCreatureFrameUpdate = 0;
    private static final int CREATURE_ANIMATION_FRAMES = 3;
    private static final long CREATURE_ANIMATION_INTERVAL = 200_000_000;

    // Sound effects
    private AudioClip backgroundMusic;
    private AudioClip collectSound;
    private AudioClip damageSound;
    private AudioClip gameOverSound;

    // Game environment
    private Image backgroundImage;
    private double backgroundOffsetX = 0;
    private double backgroundOffsetY = 0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        pane = new Pane();
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        pane.getChildren().add(canvas);

        Scene scene = new Scene(pane, WIDTH, HEIGHT);
        setupInput(scene);
        loadAssets();

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(now);
                render();
            }
        }.start();

        primaryStage.setTitle("Creature Horror Survival");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Start background music
        backgroundMusic.setCycleCount(AudioClip.INDEFINITE);
        backgroundMusic.play();
    }

    private void loadAssets() {
        // Load images (replace with actual image paths or use placeholders)
        playerImage = new Image("player_sprite.png", 160, 40, true, false);
        rubbingImage = new Image("creature_sprite.png", 150, 50, true, false);
        itemImage = new Image("item_sprite.png", 20, 20, true, false);
        backgroundImage = new Image("forest_background.png", WIDTH * 2, HEIGHT * 2, true, false);

        // Load sounds (replace with actual sound paths or use placeholders)
        backgroundMusic = new AudioClip(getClass().getResource("background_music.mp3").toString());
        collectSound = new AudioClip(getClass().getResource("collect.wav").toString());
        damageSound = new AudioClip(getClass().getResource("damage.wav").toString());
        gameOverSound = new AudioClip(getClass().getResource("game_over.wav").toString());
    }

    private void setupInput(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.W) upPressed = true;
            if (event.getCode() == KeyCode.S) downPressed = true;
            if (event.getCode() == KeyCode.A) leftPressed = true;
            if (event.getCode() == KeyCode.D) rightPressed = true;
            if (event.getCode() == KeyCode.R && (gameOver || gameWon)) restartGame();
        });

        scene.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.W) upPressed = false;
            if (event.getCode() == KeyCode.S) downPressed = false;
            if (event.getCode() == KeyCode.A) leftPressed = false;
            if (event.getCode() == KeyCode.D) rightPressed = false;
        });
    }

    private void update(long now) {
        if (gameOver || gameWon) return;

        // Update player position
        double dx = 0, dy = 0;
        if (upPressed) dy -= PLAYER_SPEED;
        if (downPressed) dy += PLAYER_SPEED;
        if (leftPressed) dx -= PLAYER_SPEED;
        if (rightPressed) dx += PLAYER_SPEED;

        // Normalize diagonal movement
        if (dx != 0 && dy != 0) {
            dx *= 0.7071; // cos(45°)
            dy *= 0.7071;
        }

        playerX += dx;
        playerY += dy;

        // Keep player in bounds
        playerX = Math.max(0, Math.min(WIDTH - PLAYER_SIZE, playerX));
        playerY = Math.max(0, Math.min(HEIGHT - PLAYER_SIZE, playerY));

        // Update background scrolling
        backgroundOffsetX = (backgroundOffsetX - dx * 0.5) % (WIDTH * 2);
        backgroundOffsetY = (backgroundOffsetY - dy * 0.5) % (HEIGHT * 2);

        // Update player animation
        if (now - lastPlayerFrameUpdate > PLAYER_ANIMATION_INTERVAL && (dx != 0 || dy != 0)) {
            playerFrame = (playerFrame + 1) % PLAYER_ANIMATION_FRAMES;
            lastPlayerFrameUpdate = now;
        }

        // Update creature animation
        if (now - lastCreatureFrameUpdate > CREATURE_ANIMATION_INTERVAL) {
            creatureFrame = (creatureFrame + 1) % CREATURE_ANIMATION_FRAMES;
            lastCreatureFrameUpdate = now;
        }

        // Spawn creatures
        if (now - lastCreatureSpawn > 5_000_000_000L && creatures.size() < MAX_CREATURES) {
            spawnCreature();
            lastCreatureSpawn = now;
        }

        // Spawn items
        if (now - lastItemSpawn > 3_000_000_000L && items.size() < MAX_ITEMS) {
            spawnItem();
            lastItemSpawn = now;
        }

        // Update creatures
        for (Creature creature : creatures) {
            // Move towards player
            double angle = Math.atan2(playerY - creature.y, playerX - creature.x);
            creature.x += Math.cos(angle) * CREATURE_SPEED;
            creature.y += Math.sin(angle) * CREATURE_SPEED;

            // Check collision with player
            if (checkCollision(playerX, playerY, PLAYER_SIZE, creature.x, creature.y, CREATURE_SIZE)) {
                health -= 10;
                damageSound.play();
                if (health <= 0) {
                    gameOver = true;
                    backgroundMusic.stop();
                    gameOverSound.play();
                }
            }
        }

        // Update items
        Iterator<Item> itemIterator = items.iterator();
        while (itemIterator.hasNext()) {
            Item item = itemIterator.next();
            if (checkCollision(playerX, playerY, PLAYER_SIZE, item.x, item.y, ITEM_SIZE)) {
                score += 10;
                collectSound.play();
                itemIterator.remove();
                if (score >= 100) {
                    gameWon = true;
                    backgroundMusic.stop();
                }
            }
        }
    }

    private void spawnCreature() {
        double x, y;
        do {
            x = random.nextDouble() * WIDTH;
            y = random.nextDouble() * HEIGHT;
        } while (Math.hypot(x - playerX, y - playerY) < 200); // Spawn away from player
        creatures.add(new Creature(x, y));
    }

    private void spawnItem() {
        double x = random.nextDouble() * (WIDTH - ITEM_SIZE);
        double y = random.nextDouble() * (HEIGHT - ITEM_SIZE);
        items.add(new Item(x, y));
    }

    private boolean checkCollision(double x1, double y1, double size1, double x2, double y2, double size2) {
        Rectangle2D rect1 = new Rectangle2D(x1, y1, size1, size1);
        Rectangle2D rect2 = new Rectangle2D(x2, y2, size2, size2);
        return rect1.intersects(rect2);
    }

    private void render() {
        // Clear canvas
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw background
        gc.drawImage(backgroundImage, backgroundOffsetX, backgroundOffsetY);
        gc.drawImage(backgroundImage, backgroundOffsetX + WIDTH * 2, backgroundOffsetY);
        gc.drawImage(backgroundImage, backgroundOffsetX, backgroundOffsetY + HEIGHT * 2);
        gc.drawImage(backgroundImage, backgroundOffsetX + WIDTH * 2, backgroundOffsetY + HEIGHT * 2);

        // Draw player
        gc.drawImage(playerImage, playerFrame * PLAYER_SIZE, 0, PLAYER_SIZE, PLAYER_SIZE,
                playerX, playerY, PLAYER_SIZE, PLAYER_SIZE);

        // Draw creatures
        for (Creature creature : creatures) {
            gc.drawImage(creatureImage, creatureFrame * CREATURE_SIZE, 0, CREATURE_SIZE, CREATURE_SIZE,
                    creature.x, creature.y, CREATURE_SIZE, CREATURE_SIZE);
        }

        // Draw items
        for (Item item : items) {
            gc.drawImage(itemImage, item.x, item.y);
        }

        // Draw HUD
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        gc.fillText("Score: " + score, 10, 30);
        gc.fillText("Health: " + health, 10, 60);

        // Draw game over or win screen
        if (gameOver || gameWon) {
            gc.setFill(Color.BLACK);
            gc.setGlobalAlpha(0.7);
            gc.fillRect(0, 0, WIDTH, HEIGHT);
            gc.setGlobalAlpha(1.0);

            gc.setFill(Color.RED);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 40));
            String message = gameOver ? "Game Over!" : "You Won!";
            gc.fillText(message, WIDTH / 2 - 100, HEIGHT / 2 - 20);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
            gc.fillText("Score: " + score, WIDTH / 2 - 50, HEIGHT / 2 + 20);
            gc.fillText("Press R to Restart", WIDTH / 2 - 80, HEIGHT / 2 + 50);
        }
    }

    private void restartGame() {
        gameOver = false;
        gameWon = false;
        score = 0;
        health = 100;
        playerX = WIDTH / 2;
        playerY = HEIGHT / 2;
        creatures.clear();
        items.clear();
        backgroundMusic.play();
    }

    private class Creature {
        double x, y;

        Creature(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private class Item {
        double x, y;

        Item(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}