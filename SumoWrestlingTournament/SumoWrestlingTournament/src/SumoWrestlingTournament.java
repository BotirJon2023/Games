import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.input.KeyCode;
import javafx.animation.AnimationTimer;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.Random;
import java.util.List;
import javafx.scene.image.Image;
import javafx.scene.text.TextAlignment;

public class SumoWrestlingTournament extends Application {
    // Constants for game dimensions and settings
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int RING_RADIUS = 200;
    private static final int RING_CENTER_X = WINDOW_WIDTH / 2;
    private static final int RING_CENTER_Y = WINDOW_HEIGHT / 2;
    private static final int PLAYER_SIZE = 50;
    private static final double PLAYER_SPEED = 5.0;
    private static final double PUSH_FORCE = 2.0;
    private static final int MAX_ROUNDS = 3;
    private static final int FPS = 60;

    // Game state variables
    private boolean gameRunning = true;
    private int round = 1;
    private int player1Score = 0;
    private int player2Score = 0;
    private boolean roundOver = false;
    private String message = "Round 1: Fight!";
    private long roundStartTime;

    // Player positions and movement
    private double player1X = RING_CENTER_X - 100;
    private double player1Y = RING_CENTER_Y;
    private double player2X = RING_CENTER_X + 100;
    private double player2Y = RING_CENTER_Y;
    private boolean player1Up, player1Down, player1Left, player1Right;
    private boolean player2Up, player2Down, player2Left, player2Right;

    // Animation-related variables
    private List<Sprite> sprites;
    private AnimationTimer gameLoop;
    private Timeline messageTimeline;

    // Images for players (optional, can be replaced with shapes)
    private Image player1Image;
    private Image player2Image;

    // Random for AI movement or effects
    private Random random = new Random();

    // Player class to handle sprite animations
    private class Sprite {
        private double x, y;
        private Image[] frames;
        private int currentFrame;
        private long lastFrameTime;
        private final long frameDuration = 100_000_000; // 100ms per frame

        public Sprite(double x, double y, String imagePath) {
            this.x = x;
            this.y = y;
            // Simulate multiple frames (placeholder images)
            frames = new Image[4];
            for (int i = 0; i < frames.length; i++) {
                frames[i] = new Image(imagePath); // Replace with actual sprite sheet
            }
            currentFrame = 0;
            lastFrameTime = System.nanoTime();
        }

        public void updateAnimation() {
            long currentTime = System.nanoTime();
            if (currentTime - lastFrameTime >= frameDuration) {
                currentFrame = (currentFrame + 1) % frames.length;
                lastFrameTime = currentTime;
            }
        }

        public void draw(GraphicsContext gc) {
            gc.drawImage(frames[currentFrame], x - PLAYER_SIZE / 2, y - PLAYER_SIZE / 2, PLAYER_SIZE, PLAYER_SIZE);
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public void setX(double x) { this.x = x; }
        public void setY(double y) { this.y = y; }
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize game components
        Pane pane = new Pane();
        Canvas canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        pane.getChildren().add(canvas);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Initialize sprites
        sprites = new ArrayList<>();
        sprites.add(new Sprite(player1X, player1Y, "/player1.png")); // Placeholder image path
        sprites.add(new Sprite(player2X, player2Y, "/player2.png")); // Placeholder image path

        // Set up input handling
        Scene scene = new Scene(pane, WINDOW_WIDTH, WINDOW_HEIGHT);
        setupInputHandling(scene);

        // Initialize game loop
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateGame();
                renderGame(gc);
            }
        };
        gameLoop.start();

        // Initialize message timeline
        messageTimeline = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> message = "")
        );
        messageTimeline.setCycleCount(1);

        // Start the first round
        roundStartTime = System.currentTimeMillis();

        // Set up stage
        primaryStage.setTitle("Sumo Wrestling Tournament");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void setupInputHandling(Scene scene) {
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            switch (code) {
                // Player 1 controls (WASD)
                case W: player1Up = true; break;
                case S: player1Down = true; break;
                case A: player1Left = true; break;
                case D: player1Right = true; break;
                // Player 2 controls (Arrow keys)
                case UP: player2Up = true; break;
                case DOWN: player2Down = true; break;
                case LEFT: player2Left = true; break;
                case RIGHT: player2Right = true; break;
            }
        });

        scene.setOnKeyReleased(event -> {
            KeyCode code = event.getCode();
            switch (code) {
                case W: player1Up = false; break;
                case S: player1Down = false; break;
                case A: player1Left = false; break;
                case D: player1Right = false; break;
                case UP: player2Up = false; break;
                case DOWN: player2Down = false; break;
                case LEFT: player2Left = false; break;
                case RIGHT: player2Right = false; break;
            }
        });
    }

    private void updateGame() {
        if (!gameRunning || roundOver) return;

        // Update player 1 position
        if (player1Up) player1Y -= PLAYER_SPEED;
        if (player1Down) player1Y += PLAYER_SPEED;
        if (player1Left) player1X -= PLAYER_SPEED;
        if (player1Right) player1X += PLAYER_SPEED;

        // Update player 2 position
        if (player2Up) player2Y -= PLAYER_SPEED;
        if (player2Down) player2Y += PLAYER_SPEED;
        if (player2Left) player2X -= PLAYER_SPEED;
        if (player2Right) player2X += PLAYER_SPEED;

        // Update sprite positions
        sprites.get(0).setX(player1X);
        sprites.get(0).setY(player1Y);
        sprites.get(1).setX(player2X);
        sprites.get(1).setY(player2Y);

        // Update animations
        for (Sprite sprite : sprites) {
            sprite.updateAnimation();
        }

        // Check for collisions
        checkCollisions();

        // Check if players are out of the ring
        checkRingBounds();

        // Check if round is over
        if (roundOver) {
            messageTimeline.playFromStart();
            if (round < MAX_ROUNDS) {
                Timeline resetTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(3), e -> resetRound())
                );
                resetTimeline.setCycleCount(1);
                resetTimeline.play();
            } else {
                gameRunning = false;
                message = player1Score > player2Score ? "Player 1 Wins Tournament!" :
                        player2Score > player1Score ? "Player 2 Wins Tournament!" : "Tournament Draw!";
            }
        }
    }

    private void checkCollisions() {
        double dx = player1X - player2X;
        double dy = player1Y - player2Y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < PLAYER_SIZE) {
            // Simple push mechanics
            double angle = Math.atan2(dy, dx);
            double pushX = Math.cos(angle) * PUSH_FORCE;
            double pushY = Math.sin(angle) * PUSH_FORCE;

            player1X += pushX;
            player1Y += pushY;
            player2X -= pushX;
            player2Y -= pushY;

            // Update sprite positions
            sprites.get(0).setX(player1X);
            sprites.get(0).setY(player1Y);
            sprites.get(1).setX(player2X);
            sprites.get(1).setY(player2Y);
        }
    }

    private void checkRingBounds() {
        double dist1 = Math.sqrt(Math.pow(player1X - RING_CENTER_X, 2) + Math.pow(player1Y - RING_CENTER_Y, 2));
        double dist2 = Math.sqrt(Math.pow(player2X - RING_CENTER_X, 2) + Math.pow(player2Y - RING_CENTER_Y, 2));

        if (dist1 > RING_RADIUS) {
            player2Score++;
            roundOver = true;
            message = "Player 2 Wins Round " + round + "!";
        } else if (dist2 > RING_RADIUS) {
            player1Score++;
            roundOver = true;
            message = "Player 1 Wins Round " + round + "!";
        }
    }

    private void resetRound() {
        round++;
        player1X = RING_CENTER_X - 100;
        player1Y = RING_CENTER_Y;
        player2X = RING_CENTER_X + 100;
        player2Y = RING_CENTER_Y;
        sprites.get(0).setX(player1X);
        sprites.get(0).setY(player1Y);
        sprites.get(1).setX(player2X);
        sprites.get(1).setY(player2Y);
        roundOver = false;
        message = "Round " + round + ": Fight!";
        roundStartTime = System.currentTimeMillis();
        messageTimeline.playFromStart();
    }

    private void renderGame(GraphicsContext gc) {
        // Clear canvas
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Draw sumo ring
        gc.setFill(Color.SANDYBROWN);
        gc.fillOval(RING_CENTER_X - RING_RADIUS, RING_CENTER_Y - RING_RADIUS, RING_RADIUS * 2, RING_RADIUS * 2);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(5);
        gc.strokeOval(RING_CENTER_X - RING_RADIUS, RING_CENTER_Y - RING_RADIUS, RING_RADIUS * 2, RING_RADIUS * 2);

        // Draw players
        for (Sprite sprite : sprites) {
            sprite.draw(gc);
        }

        // Draw score and round
        gc.setFill(Color.BLACK);
        gc.setFont(new Font("Arial", 20));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Player 1: " + player1Score, 20, 30);
        gc.fillText("Player 2: " + player2Score, WINDOW_WIDTH - 120, 30);
        gc.fillText("Round: " + round + "/" + MAX_ROUNDS, WINDOW_WIDTH / 2 - 50, 30);

        // Draw message
        if (!message.isEmpty()) {
            gc.setFill(Color.RED);
            gc.setFont(new Font("Arial", 30));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(message, WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}