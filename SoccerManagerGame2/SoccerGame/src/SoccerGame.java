import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SoccerGame extends Application {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    @Override
    public void start(Stage primaryStage) {
        Pane root = new Pane();
        root.setPrefSize(WIDTH, HEIGHT);

        // Create player circles
        Circle player1 = new Circle(50, 50, 20);
        Circle player2 = new Circle(700, 50, 20);

        // Add players to the root
        root.getChildren().addAll(player1, player2);

        // Create animation
        TranslateTransition transition = new TranslateTransition();
        transition.setNode(player1);
        transition.setDuration(Duration.seconds(2));
        transition.setToX(200);
        transition.setCycleCount(TranslateTransition.INDEFINITE);
        transition.setAutoReverse(true);
        transition.play();

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}