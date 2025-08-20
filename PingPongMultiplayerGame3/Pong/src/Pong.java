import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Pong extends Application {

    // Game constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PADDLE_WIDTH = 10;
    private static final int PADDLE_HEIGHT = 100;
    private static final int BALL_RADIUS = 8;

    // Game state variables
    private double ballX = WIDTH / 2.0;
    private double ballY = HEIGHT / 2.0;
    private double ballSpeedX = 300; // Pixels per second
    private double ballSpeedY = 300;

    private double leftPaddleY = HEIGHT / 2.0 - PADDLE_HEIGHT / 2.0;
    private double rightPaddleY = HEIGHT / 2.0 - PADDLE_HEIGHT / 2.0;
    private double paddleSpeed = 400;

    private int leftScore = 0;
    private int rightScore = 0;

    // Keyboard input state
    private boolean moveLeftUp = false;
    private boolean moveLeftDown = false;
    private boolean moveRightUp = false;
    private boolean moveRightDown = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Pong Game");

        Group root = new Group();
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        // Handle keyboard input
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.W) moveLeftUp = true;
            if (e.getCode() == KeyCode.S) moveLeftDown = true;
            if (e.getCode() == KeyCode.UP) moveRightUp = true;
            if (e.getCode() == KeyCode.DOWN) moveRightDown = true;
        });

        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.W) moveLeftUp = false;
            if (e.getCode() == KeyCode.S) moveLeftDown = false;
            if (e.getCode() == KeyCode.UP) moveRightUp = false;
            if (e.getCode() == KeyCode.DOWN) moveRightDown = false;
        });

        // The game loop
        new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;

                update(deltaTime);
                render(gc);
            }
        }.start();

        primaryStage.show();
    }

    private void update(double deltaTime) {
        // Update paddles based on input
        if (moveLeftUp) leftPaddleY -= paddleSpeed * deltaTime;
        if (moveLeftDown) leftPaddleY += paddleSpeed * deltaTime;
        if (moveRightUp) rightPaddleY -= paddleSpeed * deltaTime;
        if (moveRightDown) rightPaddleY += paddleSpeed * deltaTime;

        // Keep paddles within bounds
        if (leftPaddleY < 0) leftPaddleY = 0;
        if (leftPaddleY > HEIGHT - PADDLE_HEIGHT) leftPaddleY = HEIGHT - PADDLE_HEIGHT;
        if (rightPaddleY < 0) rightPaddleY = 0;
        if (rightPaddleY > HEIGHT - PADDLE_HEIGHT) rightPaddleY = HEIGHT - PADDLE_HEIGHT;

        // Update ball position
        ballX += ballSpeedX * deltaTime;
        ballY += ballSpeedY * deltaTime;

        // Ball collision with top/bottom walls
        if (ballY <= 0 || ballY >= HEIGHT - BALL_RADIUS * 2) {
            ballSpeedY *= -1;
        }

        // Ball collision with paddles
        // Left paddle
        if (ballX <= PADDLE_WIDTH && ballY + BALL_RADIUS * 2 >= leftPaddleY && ballY <= leftPaddleY + PADDLE_HEIGHT) {
            ballSpeedX *= -1;
            // Add some "English" based on where the ball hit the paddle
            double hitPoint = (ballY - leftPaddleY) / PADDLE_HEIGHT;
            ballSpeedY = (hitPoint - 0.5) * 2 * 300;
        }

        // Right paddle
        if (ballX >= WIDTH - PADDLE_WIDTH - BALL_RADIUS * 2 && ballY + BALL_RADIUS * 2 >= rightPaddleY && ballY <= rightPaddleY + PADDLE_HEIGHT) {
            ballSpeedX *= -1;
            // Add some "English" based on where the ball hit the paddle
            double hitPoint = (ballY - rightPaddleY) / PADDLE_HEIGHT;
            ballSpeedY = (hitPoint - 0.5) * 2 * 300;
        }

        // Ball goes out of bounds
        if (ballX < 0) {
            rightScore++;
            resetBall();
        } else if (ballX > WIDTH - BALL_RADIUS * 2) {
            leftScore++;
            resetBall();
        }
    }

    private void render(GraphicsContext gc) {
        // Clear screen
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw paddles and ball
        gc.setFill(Color.WHITE);
        gc.fillRect(0, leftPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT);
        gc.fillRect(WIDTH - PADDLE_WIDTH, rightPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT);
        gc.fillOval(ballX, ballY, BALL_RADIUS * 2, BALL_RADIUS * 2);

        // Draw scores
        gc.fillText("Player 1: " + leftScore, 50, 50);
        gc.fillText("Player 2: " + rightScore, WIDTH - 100, 50);
    }

    private void resetBall() {
        ballX = WIDTH / 2.0;
        ballY = HEIGHT / 2.0;
        ballSpeedX = (Math.random() > 0.5) ? 300 : -300;
        ballSpeedY = (Math.random() > 0.5) ? 300 : -300;
    }
}