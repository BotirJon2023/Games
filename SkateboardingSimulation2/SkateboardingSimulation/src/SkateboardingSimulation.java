import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SkateboardingSimulation extends Application {

    // Constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int SKATEBOARD_WIDTH = 50;
    private static final int SKATEBOARD_HEIGHT = 20;

    // Skateboard properties
    private double skateboardX = WIDTH / 2;
    private double skateboardY = HEIGHT / 2;
    private double skateboardSpeedX = 2;

    // Animation
    private Timeline timeline;

    @Override
    public void start(Stage primaryStage) {
        Group root = new Group();
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        // Create skateboard
        Rectangle skateboard = new Rectangle(SKATEBOARD_WIDTH, SKATEBOARD_HEIGHT);
        skateboard.setX(skateboardX);
        skateboard.setY(skateboardY);
        root.getChildren().add(skateboard);

        // Animation loop
        timeline = new Timeline(new KeyFrame(Duration.millis(16), e -> update()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void update() {
        // Update skateboard position
        skateboardX += skateboardSpeedX;
        if (skateboardX > WIDTH || skateboardX < 0) {
            skateboardSpeedX *= -1; // Bounce off edges
        }
        // Update skateboard graphics here...
    }

    public static void main(String[] args) {
        launch(args);
    }
}