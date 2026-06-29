import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

public class BasketballGame3D extends Application {
    private Group world;
    private Pane uiPane;
    private PerspectiveCamera camera;
    private Sphere ball;
    private Label scoreLabel;
    private Label levelLabel;
    private int player1Score = 0;
    private int player2Score = 0;
    private int computerScore = 0;
    private int currentLevel = 1;
    private boolean isPlayer1Turn = true;
    private boolean isComputerTurn = false;
    private Timeline shotTimeline;
    private Timeline computerTimeline;
    private double ballX = 0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        world = new Group();

        // SubScene holds all 3D content
        SubScene subScene = new SubScene(world, 1024, 768, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.LIGHTBLUE);

        camera = new PerspectiveCamera(true);
        camera.setTranslateX(0);
        camera.setTranslateY(2);
        camera.setTranslateZ(-10);
        camera.setFieldOfView(60);
        subScene.setCamera(camera);

        // uiPane overlays 2D controls on top of the 3D subscene
        uiPane = new Pane();

        createLights();
        createCourt();
        createBasket();
        createBall();
        createUI();

        StackPane root = new StackPane(subScene, uiPane);
        StackPane.setAlignment(uiPane, Pos.TOP_LEFT);

        Scene scene = new Scene(root, 1024, 768);
        scene.setOnKeyPressed(event -> handleKeyPress(event.getCode()));

        startGame();

        primaryStage.setTitle("3D Basketball Game - 2 Players vs Computer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void createLights() {
        AmbientLight ambient = new AmbientLight(Color.rgb(120, 120, 120));
        PointLight point = new PointLight(Color.WHITE);
        point.setTranslateX(0);
        point.setTranslateY(-5);
        point.setTranslateZ(-8);
        world.getChildren().addAll(ambient, point);
    }

    private void createCourt() {
        Box ground = new Box(20, 0.5, 15);
        ground.setMaterial(new PhongMaterial(Color.SANDYBROWN));
        ground.setTranslateY(-1);
        ground.setTranslateZ(-2);
        world.getChildren().add(ground);

        Box courtLine = new Box(18, 0.1, 0.1);
        courtLine.setMaterial(new PhongMaterial(Color.WHITE));
        courtLine.setTranslateY(-0.7);
        courtLine.setTranslateZ(-2);
        world.getChildren().add(courtLine);

        // Center circle approximated with 3D box segments
        for (int i = 0; i < 36; i++) {
            Box segment = new Box(0.15, 0.05, 0.15);
            segment.setMaterial(new PhongMaterial(Color.WHITE));
            double angle = Math.toRadians(i * 10);
            segment.setTranslateX(2.0 * Math.cos(angle));
            segment.setTranslateY(-0.72);
            segment.setTranslateZ(-2.0 + 2.0 * Math.sin(angle));
            world.getChildren().add(segment);
        }
    }

    private void createBasket() {
        Cylinder pole = new Cylinder(0.2, 3);
        pole.setMaterial(new PhongMaterial(Color.GRAY));
        pole.setTranslateX(3);
        pole.setTranslateY(0.5);
        pole.setTranslateZ(-2);
        world.getChildren().add(pole);

        Box backboard = new Box(1.2, 0.8, 0.1);
        backboard.setMaterial(new PhongMaterial(Color.WHITE));
        backboard.setTranslateX(3);
        backboard.setTranslateY(1.8);
        backboard.setTranslateZ(-2);
        world.getChildren().add(backboard);

        Box square = new Box(0.5, 0.5, 0.05);
        square.setMaterial(new PhongMaterial(Color.RED));
        square.setTranslateX(3);
        square.setTranslateY(1.8);
        square.setTranslateZ(-1.95);
        world.getChildren().add(square);

        Cylinder hoop = new Cylinder(0.03, 0.8);
        hoop.setMaterial(new PhongMaterial(Color.ORANGE));
        hoop.setTranslateX(3);
        hoop.setTranslateY(1.5);
        hoop.setTranslateZ(-2);
        hoop.setRotationAxis(Rotate.X_AXIS);
        hoop.setRotate(90);
        world.getChildren().add(hoop);

        for (int i = 0; i < 8; i++) {
            Cylinder netRing = new Cylinder(0.01, 0.6);
            netRing.setMaterial(new PhongMaterial(Color.WHITE));
            double angle = i * 45;
            netRing.setTranslateX(3 + 0.4 * Math.cos(Math.toRadians(angle)));
            netRing.setTranslateY(1.3 - i * 0.05);
            netRing.setTranslateZ(-2 + 0.4 * Math.sin(Math.toRadians(angle)));
            netRing.setRotationAxis(Rotate.X_AXIS);
            netRing.setRotate(90);
            world.getChildren().add(netRing);
        }
    }

    private void createBall() {
        ball = new Sphere(0.3);
        ball.setMaterial(new PhongMaterial(Color.ORANGE));
        ball.setTranslateX(0);
        ball.setTranslateY(1);
        ball.setTranslateZ(-2);
        world.getChildren().add(ball);
    }

    private void createUI() {
        VBox uiBox = new VBox(10);
        uiBox.setLayoutX(10);
        uiBox.setLayoutY(10);

        scoreLabel = new Label("Player 1: 0 | Player 2: 0 | Computer: 0");
        scoreLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;"
                + " -fx-background-color: rgba(0,0,0,0.55); -fx-padding: 4;");

        levelLabel = new Label("Level: 1");
        levelLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;"
                + " -fx-background-color: rgba(0,0,0,0.55); -fx-padding: 4;");

        Label controlsLabel = new Label(
                "Controls:\nP1: A/D - Move | W - Shoot\nP2: Left/Right - Move | Up - Shoot\nSpace - Switch Player");
        controlsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white;"
                + " -fx-background-color: rgba(0,0,0,0.55); -fx-padding: 4;");

        uiBox.getChildren().addAll(scoreLabel, levelLabel, controlsLabel);
        uiPane.getChildren().add(uiBox);
    }

    private void startGame() {
        updateUI();
        startComputerTurn();
    }

    private void handleKeyPress(KeyCode code) {
        if (isComputerTurn) return;
        if (shotTimeline != null && shotTimeline.getStatus() == Animation.Status.RUNNING) return;

        if (code == KeyCode.SPACE) {
            isPlayer1Turn = !isPlayer1Turn;
            updateUI();
            return;
        }

        if (isPlayer1Turn) {
            handlePlayer1Controls(code);
        } else {
            handlePlayer2Controls(code);
        }
    }

    private void handlePlayer1Controls(KeyCode code) {
        switch (code) {
            case A -> { ballX -= 0.5; updateBallPosition(); }
            case D -> { ballX += 0.5; updateBallPosition(); }
            case W -> shootBall();
        }
    }

    private void handlePlayer2Controls(KeyCode code) {
        switch (code) {
            case LEFT  -> { ballX -= 0.5; updateBallPosition(); }
            case RIGHT -> { ballX += 0.5; updateBallPosition(); }
            case UP    -> shootBall();
        }
    }

    private void updateBallPosition() {
        ballX = Math.max(-2, Math.min(2, ballX));
        ball.setTranslateX(ballX);
    }

    private void shootBall() {
        if (shotTimeline != null && shotTimeline.getStatus() == Animation.Status.RUNNING) return;

        double startX = ball.getTranslateX();
        double startY = ball.getTranslateY();
        double startZ = ball.getTranslateZ();
        double endX = 3.0;
        double endY = 1.5;
        double endZ = -2.0;
        double totalDuration = 1.5; // seconds
        int totalFrames = 60;

        shotTimeline = new Timeline();

        // Build one KeyFrame per step so the ball moves smoothly across the full duration.
        // The original code put everything inside a single KeyFrame that fired at t=1.5s,
        // which meant checkScore() was called before any inner timeline had moved the ball.
        for (int i = 0; i <= totalFrames; i++) {
            final double t = (double) i / totalFrames;
            final double x = startX + (endX - startX) * t;
            // Parabolic arc: subtract makes Y smaller = upward in JavaFX 3D (Y axis points down)
            final double y = startY + (endY - startY) * t - 4.0 * t * (1.0 - t);
            final double z = startZ + (endZ - startZ) * t;

            shotTimeline.getKeyFrames().add(
                    new KeyFrame(Duration.seconds(t * totalDuration), ev -> {
                        ball.setTranslateX(x);
                        ball.setTranslateY(y);
                        ball.setTranslateZ(z);
                    })
            );
        }

        shotTimeline.setOnFinished(e -> checkScore());
        shotTimeline.play();
    }

    private void checkScore() {
        double bx = ball.getTranslateX();
        double by = ball.getTranslateY();
        double bz = ball.getTranslateZ();

        if (Math.abs(bx - 3) < 0.5 && Math.abs(by - 1.5) < 0.3 && Math.abs(bz + 2) < 0.5) {
            if (isPlayer1Turn) {
                player1Score++;
                showMessage("Player 1 scores!");
            } else if (isComputerTurn) {
                // Fixed: computer score was never incremented (original used !isComputerTurn)
                computerScore++;
                showMessage("Computer scores!");
            } else {
                player2Score++;
                showMessage("Player 2 scores!");
            }
            updateUI();
        }

        resetBall();

        if (player1Score + player2Score + computerScore >= 5 * currentLevel) {
            currentLevel++;
            levelLabel.setText("Level: " + currentLevel);
            showMessage("Level Up! Welcome to Level " + currentLevel);
            resetAllScores();
        }

        if (isComputerTurn) {
            isComputerTurn = false;
            isPlayer1Turn = true;
        } else {
            startComputerTurn();
        }
    }

    private void startComputerTurn() {
        if (isComputerTurn) return;
        isComputerTurn = true;
        isPlayer1Turn = false;
        showMessage("Computer's turn");

        // Guard against negative delay at high levels
        int delay = Math.max(100, 1000 - currentLevel * 50);
        double accuracy = Math.min(1.0, 0.6 + currentLevel * 0.05);

        computerTimeline = new Timeline(
                new KeyFrame(Duration.millis(delay), e -> {
                    ballX = -2 + Math.random() * 4;
                    updateBallPosition();

                    if (Math.random() < accuracy) {
                        shootBall();
                    } else {
                        showMessage("Computer missed!");
                        resetBall();
                        isComputerTurn = false;
                        isPlayer1Turn = true;
                    }
                })
        );
        computerTimeline.setCycleCount(1);
        computerTimeline.play();
    }

    private void resetBall() {
        ballX = 0;
        ball.setTranslateX(0);
        ball.setTranslateY(1);
        ball.setTranslateZ(-2);
    }

    private void resetAllScores() {
        player1Score = 0;
        player2Score = 0;
        computerScore = 0;
        updateUI();
    }

    private void updateUI() {
        String turn = isPlayer1Turn ? "Player 1's Turn"
                : (isComputerTurn ? "Computer's Turn" : "Player 2's Turn");
        scoreLabel.setText("Player 1: " + player1Score
                + " | Player 2: " + player2Score
                + " | Computer: " + computerScore
                + " | " + turn);
    }

    private void showMessage(String message) {
        Label lbl = new Label(message);
        lbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: gold;"
                + " -fx-background-color: rgba(0,0,0,0.6); -fx-padding: 8;");
        lbl.setLayoutX(350);
        lbl.setLayoutY(350);
        uiPane.getChildren().add(lbl);

        new Timeline(new KeyFrame(Duration.seconds(2), e -> uiPane.getChildren().remove(lbl))).play();
    }

    @Override
    public void stop() throws Exception {
        if (shotTimeline != null) shotTimeline.stop();
        if (computerTimeline != null) computerTimeline.stop();
        super.stop();
    }
}
