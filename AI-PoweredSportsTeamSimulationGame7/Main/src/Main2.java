import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.Random;

public class Main extends Application {

    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;

    private Pane root;
    private Ball ball;
    private Player playerA, playerB;
    private AILogic ai;

    private long lastUpdateTime;

    @Override
    public void start(Stage primaryStage) {
        root = new Pane();
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

        setupGameObjects();
        setupAI();
        setupAnimationLoop();

        primaryStage.setTitle("AI-Powered Sports Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupGameObjects() {
        // Create game elements
        ball = new Ball(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2, 10, Color.RED);
        playerA = new Player(100, WINDOW_HEIGHT / 2, 20, Color.BLUE);
        playerB = new Player(700, WINDOW_HEIGHT / 2, 20, Color.GREEN);

        // Add them to the scene
        root.getChildren().addAll(ball.getShape(), playerA.getShape(), playerB.getShape());
    }

    private void setupAI() {
        // Initialize the AI with a reference to the game objects
        ai = new AILogic(playerB, ball);
    }

    private void setupAnimationLoop() {
        lastUpdateTime = System.nanoTime();

        new AnimationTimer() {
            @Override
            public void handle(long currentNanoTime) {
                // Calculate time elapsed since last update
                double elapsedTime = (currentNanoTime - lastUpdateTime) / 1_000_000_000.0;
                lastUpdateTime = currentNanoTime;

                // Game logic
                updateGame(elapsedTime);
            }
        }.start();
    }

    private void updateGame(double elapsedTime) {
        // Update player positions (simplistic movement for demonstration)
        playerA.move(elapsedTime); // Player A could be user-controlled
        ai.makeDecision(elapsedTime); // AI controls player B

        // Update ball position and check for collisions
        ball.move(elapsedTime);
        checkCollisions();

        // Update UI based on new positions
        ball.getShape().setLayoutX(ball.getX());
        ball.getShape().setLayoutY(ball.getY());
        playerA.getShape().setLayoutY(playerA.getY());
        playerB.getShape().setLayoutY(playerB.getY());
    }

    private void checkCollisions() {
        // Simple collision detection (e.g., if ball touches a player)
        if (ball.getShape().getBoundsInParent().intersects(playerA.getShape().getBoundsInParent())) {
            ball.reverseX();
        }
        if (ball.getShape().getBoundsInParent().intersects(playerB.getShape().getBoundsInParent())) {
            ball.reverseX();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}