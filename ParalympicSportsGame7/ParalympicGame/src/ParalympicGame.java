import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.Set;

public class ParalympicGame extends Application {

    private static final double WIDTH = 800;
    private static final double HEIGHT = 600;

    private GraphicsContext gc;
    private Player player;
    private Ball ball;
    private Set<KeyCode> pressedKeys = new HashSet<>();

    private long lastFrameTime = 0;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    // Load assets
    private Image playerImage = new Image("file:src/main/resources/player.png");
    private Image ballImage = new Image("file:src/main/resources/ball.png");

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

        initializeGame();

        primaryStage.setTitle("Paralympic Sports Game");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Game loop
        new AnimationTimer() {
            @Override
            public void handle(long currentNanoTime) {
                if (lastFrameTime == 0) {
                    lastFrameTime = currentNanoTime;
                    return;
                }
                double deltaTime = (currentNanoTime - lastFrameTime) / NANOS_PER_SECOND;
                lastFrameTime = currentNanoTime;

                update(deltaTime);
                render();
            }
        }.start();
    }

    private void initializeGame() {
        // Initialize game objects
        player = new Player(100, HEIGHT / 2 - 25, 50, 50, playerImage);
        ball = new Ball(WIDTH / 2, HEIGHT / 2, 20, 20, ballImage);
    }

    private void update(double deltaTime) {
        // Handle player input
        if (pressedKeys.contains(KeyCode.UP)) {
            player.move(0, -player.getSpeed() * deltaTime);
        }
        if (pressedKeys.contains(KeyCode.DOWN)) {
            player.move(0, player.getSpeed() * deltaTime);
        }
        if (pressedKeys.contains(KeyCode.LEFT)) {
            player.move(-player.getSpeed() * deltaTime, 0);
        }
        if (pressedKeys.contains(KeyCode.RIGHT)) {
            player.move(player.getSpeed() * deltaTime, 0);
        }

        // Update game objects (e.g., ball physics, collision detection)
        ball.update(deltaTime);

        // Check for collision
        if (player.collidesWith(ball)) {
            // Implement collision logic (e.g., player "hits" the ball)
            System.out.println("Collision detected!");
            ball.setVelocity(-ball.getVx(), -ball.getVy()); // Simple bounce
        }

        // Game boundaries
        player.keepInBounds(WIDTH, HEIGHT);
        ball.keepInBounds(WIDTH, HEIGHT);
    }

    private void render() {
        // Clear the canvas
        gc.setFill(Color.LIGHTGREEN);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw game objects
        player.draw(gc);
        ball.draw(gc);

        // You would add score display, goal posts, etc. here.
    }

    public static void main(String[] args) {
        launch(args);
    }
}