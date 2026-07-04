import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

public class BasketballGame extends Application {

    // Game constants
    private static final double COURT_WIDTH = 800;
    private static final double COURT_LENGTH = 1200;
    private static final double HOOP_HEIGHT = 300;
    private static final double BALL_RADIUS = 25;

    private Group root;
    private PerspectiveCamera camera;
    private SubScene subScene;
    private Sphere ball;
    private Box hoopBackboard;
    private Cylinder hoopRim;

    // Players
    private Player player1; // Human
    private Player player2; // AI or Player 2
    private boolean isTwoPlayers = false;

    // Game state
    private int score1 = 0, score2 = 0;
    private int level = 1;
    private boolean ballInMotion = false;
    private Timeline gameLoop;

    private Label scoreLabel;

    @Override
    public void start(Stage stage) {
        root = new Group();
        camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-800);
        camera.setTranslateY(-400);
        camera.setRotationAxis(Rotate.X_AXIS);
        camera.setRotate(25);

        subScene = new SubScene(root, 1200, 800, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        subScene.setFill(Color.SKYBLUE);

        // Build court
        buildCourt();

        // Create ball
        ball = new Sphere(BALL_RADIUS);
        ball.setMaterial(new PhongMaterial(Color.ORANGE));
        ball.setTranslateY(-BALL_RADIUS - 50);
        root.getChildren().add(ball);

        // Create hoop
        buildHoop();

        // Create players
        player1 = new Player(Color.BLUE, -200, 100);
        player2 = new Player(Color.RED, 200, -400);
        root.getChildren().addAll(player1.model, player2.model);

        // UI
        VBox ui = createUI();

        BorderPane mainPane = new BorderPane();
        mainPane.setCenter(subScene);
        mainPane.setTop(ui);

        Scene scene = new Scene(mainPane, 1200, 850);
        setupControls(scene);

        stage.setTitle("3D Basketball Arena - Level " + level);
        stage.setScene(scene);
        stage.show();

        startGameLoop();
    }

    private void buildCourt() {
        // Floor
        Box floor = new Box(COURT_WIDTH, 20, COURT_LENGTH);
        floor.setMaterial(new PhongMaterial(Color.DARKGREEN));
        floor.setTranslateY(10);
        root.getChildren().add(floor);

        // Lines (simplified)
        for (int i = -1; i <= 1; i++) {
            Box line = new Box(COURT_WIDTH + 20, 5, 10);
            line.setMaterial(new PhongMaterial(Color.WHITE));
            line.setTranslateZ(i * 300);
            line.setTranslateY(15);
            root.getChildren().add(line);
        }
    }

    private void buildHoop() {
        // Backboard
        hoopBackboard = new Box(10, 150, 250);
        hoopBackboard.setMaterial(new PhongMaterial(Color.WHITE));
        hoopBackboard.setTranslateX(0);
        hoopBackboard.setTranslateY(-HOOP_HEIGHT);
        hoopBackboard.setTranslateZ(-COURT_LENGTH/2 + 100);

        // Rim
        hoopRim = new Cylinder(60, 10);
        hoopRim.setMaterial(new PhongMaterial(Color.RED));
        hoopRim.setTranslateX(0);
        hoopRim.setTranslateY(-HOOP_HEIGHT + 60);
        hoopRim.setTranslateZ(-COURT_LENGTH/2 + 100);
        hoopRim.setRotationAxis(Rotate.X_AXIS);
        hoopRim.setRotate(90);

        root.getChildren().addAll(hoopBackboard, hoopRim);
    }

    private VBox createUI() {
        scoreLabel = new Label("Player 1: 0   |   Player 2: 0   Level: " + level);
        scoreLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");
        VBox vbox = new VBox(10, scoreLabel);
        vbox.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 10;");
        return vbox;
    }

    private void setupControls(Scene scene) {
        scene.setOnKeyPressed(e -> {
            if (ballInMotion) return;

            switch (e.getCode()) {
                case SPACE -> shoot(player1, 45, 25); // power, angle
                case UP -> player1.move(0, -20);
                case DOWN -> player1.move(0, 20);
                case LEFT -> player1.move(-20, 0);
                case RIGHT -> player1.move(20, 0);
                case ENTER -> { // Switch to 2 players or AI mode
                    isTwoPlayers = !isTwoPlayers;
                    System.out.println(isTwoPlayers ? "2 Players mode" : "vs Computer");
                }
            }
        });
    }

    private void shoot(Player shooter, double power, double angle) {
        ballInMotion = true;

        // Simple projectile motion + spin animation
        TranslateTransition throwAnim = new TranslateTransition(Duration.seconds(1.2), ball);
        double targetZ = -COURT_LENGTH/2 + 120;
        double targetY = -HOOP_HEIGHT + 30;

        throwAnim.setToX(shooter.model.getTranslateX() + 50);
        throwAnim.setToY(targetY);
        throwAnim.setToZ(targetZ);

        // Add realistic arc
        throwAnim.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));

        throwAnim.setOnFinished(ev -> {
            checkIfScored();
            ballInMotion = false;
            resetBall();
        });

        // Ball rotation for realism
        RotateTransition spin = new RotateTransition(Duration.seconds(1.2), ball);
        spin.setAxis(new Point3D(1, 0.3, 0.5));
        spin.setByAngle(720 * (power / 30));

        ParallelTransition shot = new ParallelTransition(throwAnim, spin);
        shot.play();
    }

    private void checkIfScored() {
        double distToRim = Math.hypot(
                ball.getTranslateX() - hoopRim.getTranslateX(),
                ball.getTranslateZ() - hoopRim.getTranslateZ()
        );

        if (distToRim < 70 && ball.getTranslateY() < -HOOP_HEIGHT + 80) {
            score1 += (level > 2 ? 3 : 2);
            scoreLabel.setText("Player 1: " + score1 + "   |   Player 2: " + score2 + "   Level: " + level);
            levelUp();
        }
    }

    private void resetBall() {
        ball.setTranslateX(0);
        ball.setTranslateY(-BALL_RADIUS - 50);
        ball.setTranslateZ(100);
    }

    private void levelUp() {
        level++;
        // Increase difficulty: AI faster, smaller rim, etc.
        if (level % 3 == 0) {
            // You can shrink hoop radius or increase AI skill here
        }
    }

    private void startGameLoop() {
        gameLoop = new Timeline(new KeyFrame(Duration.millis(16), e -> {
            updateAI();
        }));
        gameLoop.setCycleCount(Animation.INDEFINITE);
        gameLoop.play();
    }

    private void updateAI() {
        if (isTwoPlayers || ballInMotion) return;

        // Simple AI: move toward ball and occasionally shoot
        double dx = ball.getTranslateX() - player2.model.getTranslateX();
        double dz = ball.getTranslateZ() - player2.model.getTranslateZ();

        player2.model.setTranslateX(player2.model.getTranslateX() + dx * 0.08);
        player2.model.setTranslateZ(player2.model.getTranslateZ() + dz * 0.08);

        if (Math.random() < 0.02) { // AI shoots randomly
            shoot(player2, 40 + level * 3, 30);
        }
    }

    // Inner Player class
    static class Player {
        Group model = new Group();
        Sphere body;
        Color color;

        Player(Color c, double x, double z) {
            this.color = c;
            body = new Sphere(45);
            body.setMaterial(new PhongMaterial(c));
            model.getChildren().add(body);
            model.setTranslateX(x);
            model.setTranslateZ(z);
            model.setTranslateY(-40);
        }

        void move(double dx, double dz) {
            model.setTranslateX(model.getTranslateX() + dx);
            model.setTranslateZ(model.getTranslateZ() + dz);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}