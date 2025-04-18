package org.example;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.animation.AnimationTimer;
import javafx.scene.input.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Main class for Clash Royale Clone
public class ClashRoyaleClone extends Application {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PLAYER_TOWER_X = 200;
    private static final int ENEMY_TOWER_X = 600;
    private static final int TOWER_Y = 100;
    private static final int TOWER_WIDTH = 50;
    private static final int TOWER_HEIGHT = 100;
    private static final int CARD_AREA_Y = 500;
    private static final int CARD_WIDTH = 80;
    private static final int CARD_HEIGHT = 100;

    private Pane root;
    private Canvas canvas;
    private GraphicsContext gc;
    private Tower playerTower;
    private Tower enemyTower;
    private List<Troop> troops;
    private List<Card> deck;
    private List<Card> hand;
    private double elixir = 10.0;
    private double elixirRegenRate = 0.1;
    private Random random = new Random();
    private boolean gameOver = false;
    private String winner = "";

    // Troop class to represent units
    class Troop {
        double x, y;
        double speed;
        int health;
        int damage;
        boolean isPlayerTroop;
        String type;

        Troop(double x, double y, String type, boolean isPlayerTroop) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.isPlayerTroop = isPlayerTroop;
            if (type.equals("Knight")) {
                this.speed = 2.0;
                this.health = 100;
                this.damage = 20;
            } else if (type.equals("Archer")) {
                this.speed = 1.5;
                this.health = 50;
                this.damage = 10;
            }
        }

        void update() {
            if (isPlayerTroop) {
                x += speed; // Move right toward enemy
            } else {
                x -= speed; // Move left toward player
            }
            // Attack enemy troops or tower
            for (Troop other : troops) {
                if (other.isPlayerTroop != isPlayerTroop && Math.abs(other.x - x) < 20 && Math.abs(other.y - y) < 20) {
                    other.health -= damage;
                    return;
                }
            }
            // Attack tower
            if (isPlayerTroop && Math.abs(x - enemyTower.x) < 20) {
                enemyTower.health -= damage;
            } else if (!isPlayerTroop && Math.abs(x - playerTower.x) < 20) {
                playerTower.health -= damage;
            }
        }

        void render(GraphicsContext gc) {
            gc.setFill(isPlayerTroop ? Color.BLUE : Color.RED);
            gc.fillOval(x - 10, y - 10, 20, 20);
            gc.setFill(Color.WHITE);
            gc.fillText(type + " (" + health + ")", x - 20, y - 15);
        }

        boolean isDead() {
            return health <= 0;
        }
    }

    // Tower class to represent player and enemy towers
    class Tower {
        double x, y;
        int health;
        boolean isPlayerTower;

        Tower(double x, double y, boolean isPlayerTower) {
            this.x = x;
            this.y = y;
            this.health = 500;
            this.isPlayerTower = isPlayerTower;
        }

        void render(@org.jetbrains.annotations.NotNull GraphicsContext gc) {
            gc.setFill(isPlayerTower ? Color.GREEN : Color.PURPLE);
            gc.fillRect(x, y, TOWER_WIDTH, TOWER_HEIGHT);
            gc.setFill(Color.BLACK);
            gc.fillText("Health: " + health, x, y - 10);
        }

        boolean isDestroyed() {
            return health <= 0;
        }
    }

    // Card class to represent deployable troops
    class Card {
        String type;
        double elixirCost;
        int x, y;

        Card(String type, double elixirCost, int x, int y) {
            this.type = type;
            this.elixirCost = elixirCost;
            this.x = x;
            this.y = y;
        }

        void render(GraphicsContext gc) {
            gc.setFill(Color.YELLOW);
            gc.fillRect(x, y, CARD_WIDTH, CARD_HEIGHT);
            gc.setStroke(Color.BLACK);
            gc.strokeRect(x, y, CARD_WIDTH, CARD_HEIGHT);
            gc.setFill(Color.BLACK);
            gc.fillText(type + " (" + elixirCost + ")", x + 10, y + 50);
        }

        boolean isClicked(double mx, double my) {
            return mx >= x && mx <= x + CARD_WIDTH && my >= y && my <= y + CARD_HEIGHT;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        root = new Pane();
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        // Initialize game objects
        playerTower = new Tower(PLAYER_TOWER_X, TOWER_Y, true);
        enemyTower = new Tower(ENEMY_TOWER_X, TOWER_Y, false);
        troops = new ArrayList<>();
        deck = new ArrayList<>();
        hand = new ArrayList<>();

        // Create deck
        deck.add(new Card("Knight", 3.0, 0, 0));
        deck.add(new Card("Archer", 2.0, 0, 0));
        deck.add(new Card("Knight", 3.0, 0, 0));
        deck.add(new Card("Archer", 2.0, 0, 0));

        // Draw initial hand
        drawHand();

        // Handle mouse clicks for card deployment
        canvas.setOnMouseClicked(this::handleMouseClick);

        // Game loop
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render();
            }
        }.start();

        // Set up scene and stage
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        primaryStage.setTitle("Clash Royale Clone");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Draw cards into hand
    private void drawHand() {
        hand.clear();
        for (int i = 0; i < 4; i++) {
            Card card = deck.get(random.nextInt(deck.size()));
            card.x = 100 + i * 100;
            card.y = CARD_AREA_Y;
            hand.add(card);
        }
    }

    // Handle mouse clicks to deploy troops
    private void handleMouseClick(MouseEvent event) {
        if (gameOver) return;
        double mx = event.getX();
        double my = event.getY();
        for (Card card : hand) {
            if (card.isClicked(mx, my) && elixir >= card.elixirCost) {
                elixir -= card.elixirCost;
                troops.add(new Troop(PLAYER_TOWER_X, TOWER_Y + TOWER_HEIGHT, card.type, true));
                drawHand(); // Replace card
                break;
            }
        }
    }

    // Update game state
    private void update() {
        if (gameOver) return;

        // Regenerate elixir
        elixir = Math.min(10.0, elixir + elixirRegenRate);

        // Update troops
        List<Troop> toRemove = new ArrayList<>();
        for (Troop troop : troops) {
            troop.update();
            if (troop.isDead()) {
                toRemove.add(troop);
            }
        }
        troops.removeAll(toRemove);

        // Enemy AI: Deploy troops randomly
        if (random.nextDouble() < 0.02 && elixir > 2.0) {
            String type = random.nextBoolean() ? "Knight" : "Archer";
            troops.add(new Troop(ENEMY_TOWER_X, TOWER_Y + TOWER_HEIGHT, type, false));
        }

        // Check win/lose conditions
        if (playerTower.isDestroyed()) {
            gameOver = true;
            winner = "Enemy Wins!";
        } else if (enemyTower.isDestroyed()) {
            gameOver = true;
            winner = "Player Wins!";
        }
    }

    // Render game
    private void render() {
        // Clear canvas
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw arena
        gc.setFill(Color.DARKGREEN);
        gc.fillRect(0, HEIGHT / 2 - 50, WIDTH, 100);

        // Draw towers
        playerTower.render(gc);
        enemyTower.render(gc);

        // Draw troops
        for (Troop troop : troops) {
            troop.render(gc);
        }

        // Draw cards
        for (Card card : hand) {
            card.render(gc);
        }

        // Draw elixir bar
        gc.setFill(Color.PURPLE);
        gc.fillRect(10, 10, elixir * 20, 20);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(10, 10, 200, 20);
        gc.setFill(Color.BLACK);
        gc.fillText("Elixir: " + String.format("%.1f", elixir), 220, 25);

        // Draw game over message
        if (gameOver) {
            gc.setFill(Color.BLACK);
            gc.setFont(new Font("Arial", 50));
            gc.fillText(winner, WIDTH / 2 - 100, HEIGHT / 2);
        }
    }

    // Main method to launch the application
    public static void main(String[] args) {
        launch(args);
    }
}