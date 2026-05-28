import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Random;

public class FootballManagementSimulation extends Application {

    // --- Model ---
    static class Player {
        String name;
        int score = 0;

        Player(String name) {
            this.name = name;
        }

        void addGoal() {
            score++;
        }
    }

    // --- Fields ---
    private Player p1 = new Player("Player 1");
    private Player p2 = new Player("Player 2 / Computer");

    private boolean vsComputer = true;
    private boolean player1Turn = true;

    private Label scoreLabel = new Label("0 : 0");
    private Label statusLabel = new Label("Choose mode and start playing.");
    private Button attackBtn = new Button("Attack");
    private Button defendBtn = new Button("Defend");
    private Button passBtn = new Button("Pass");

    private Circle ball;
    private Rectangle field;
    private Random random = new Random();

    private String pendingActionP1 = null;
    private String pendingActionP2 = null;

    @Override
    public void start(Stage stage) {
        // Mode selection
        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton rbVsComputer = new RadioButton("Player vs Computer");
        RadioButton rbVsPlayer = new RadioButton("Player vs Player");
        rbVsComputer.setToggleGroup(modeGroup);
        rbVsPlayer.setToggleGroup(modeGroup);
        rbVsComputer.setSelected(true);

        modeGroup.selectedToggleProperty().addListener((obs, old, val) -> {
            if (val == rbVsComputer) {
                vsComputer = true;
                p2.name = "Computer";
            } else {
                vsComputer = false;
                p2.name = "Player 2";
            }
            resetGame();
        });

        // Field + ball
        field = new Rectangle(600, 300, Color.DARKGREEN);
        field.setArcWidth(40);
        field.setArcHeight(40);
        field.setStroke(Color.WHITE);
        field.setStrokeWidth(3);

        ball = new Circle(10, Color.WHITE);
        ball.setTranslateX(0);
        ball.setTranslateY(0);

        StackPane pitch = new StackPane(field, ball);
        pitch.setPadding(new Insets(20));

        // Score + status
        scoreLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");
        statusLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");

        HBox topBar = new HBox(20, scoreLabel, statusLabel);
        topBar.setAlignment(Pos.CENTER);

        // Action buttons
        attackBtn.setOnAction(e -> handleAction("ATTACK"));
        defendBtn.setOnAction(e -> handleAction("DEFEND"));
        passBtn.setOnAction(e -> handleAction("PASS"));

        attackBtn.setPrefWidth(100);
        defendBtn.setPrefWidth(100);
        passBtn.setPrefWidth(100);

        HBox actions = new HBox(20, attackBtn, defendBtn, passBtn);
        actions.setAlignment(Pos.CENTER);

        VBox modeBox = new VBox(5, new Label("Mode:"), rbVsComputer, rbVsPlayer);
        modeBox.setStyle("-fx-text-fill: white;");
        modeBox.setAlignment(Pos.CENTER_LEFT);

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        BorderPane.setMargin(topBar, new Insets(10));
        root.setCenter(pitch);
        root.setBottom(actions);
        BorderPane.setMargin(actions, new Insets(10));
        root.setLeft(modeBox);
        BorderPane.setMargin(modeBox, new Insets(10));
        root.setStyle("-fx-background-color: #003300;");

        Scene scene = new Scene(root, 900, 500);
        stage.setScene(scene);
        stage.setTitle("Football Management Simulation");
        stage.show();

        updateStatus();
    }

    private void handleAction(String action) {
        if (player1Turn) {
            pendingActionP1 = action;
            if (vsComputer) {
                pendingActionP2 = getAIAction();
                resolveTurn();
            } else {
                player1Turn = false;
                updateStatus();
            }
        } else {
            pendingActionP2 = action;
            resolveTurn();
        }
    }

    private String getAIAction() {
        String[] actions = {"ATTACK", "DEFEND", "PASS"};
        return actions[random.nextInt(actions.length)];
    }

    private void resolveTurn() {
        if (pendingActionP1 == null || pendingActionP2 == null) return;

        String a1 = pendingActionP1;
        String a2 = pendingActionP2;

        int result = resolveOutcome(a1, a2);
        String message;

        if (result == 1) {
            p1.addGoal();
            message = p1.name + " scores! (" + a1 + " vs " + a2 + ")";
            playGoalAnimation(true);
        } else if (result == -1) {
            p2.addGoal();
            message = p2.name + " scores! (" + a1 + " vs " + a2 + ")";
            playGoalAnimation(false);
        } else {
            message = "No goal. (" + a1 + " vs " + a2 + ")";
            playNeutralAnimation(a1, a2);
        }

        scoreLabel.setText(p1.score + " : " + p2.score);
        statusLabel.setText(message);

        pendingActionP1 = null;
        pendingActionP2 = null;
        player1Turn = true;
        updateStatus();
    }

    // Simple rock-paper-scissors style logic
    private int resolveOutcome(String a1, String a2) {
        if (a1.equals(a2)) return 0;

        if (a1.equals("ATTACK") && a2.equals("DEFEND")) return -1;
        if (a1.equals("DEFEND") && a2.equals("ATTACK")) return 1;

        if (a1.equals("ATTACK") && a2.equals("PASS")) return 1;
        if (a1.equals("PASS") && a2.equals("ATTACK")) return -1;

        if (a1.equals("DEFEND") && a2.equals("PASS")) return 0;
        if (a1.equals("PASS") && a2.equals("DEFEND")) return 0;

        return 0;
    }

    private void playGoalAnimation(boolean leftToRight) {
        double direction = leftToRight ? 1 : -1;
        TranslateTransition tt = new TranslateTransition(Duration.seconds(1.2), ball);
        tt.setFromX(0);
        tt.setFromY(0);
        tt.setToX(direction * 220);
        tt.setToY(0);
        tt.setAutoReverse(true);
        tt.setCycleCount(2);
        tt.play();
    }

    private void playNeutralAnimation(String a1, String a2) {
        double dx = (a1.equals("ATTACK") ? 80 : a1.equals("PASS") ? 40 : -40);
        double dy = (a2.equals("ATTACK") ? -40 : a2.equals("PASS") ? 40 : 0);

        TranslateTransition tt = new TranslateTransition(Duration.seconds(0.8), ball);
        tt.setFromX(0);
        tt.setFromY(0);
        tt.setToX(dx);
        tt.setToY(dy);
        tt.setAutoReverse(true);
        tt.setCycleCount(2);
        tt.play();
    }

    private void updateStatus() {
        Platform.runLater(() -> {
            if (player1Turn) {
                statusLabel.setText(p1.name + " turn: choose an action.");
            } else {
                statusLabel.setText(p2.name + " turn: choose an action.");
            }
        });
    }

    private void resetGame() {
        p1.score = 0;
        p2.score = 0;
        scoreLabel.setText("0 : 0");
        player1Turn = true;
        pendingActionP1 = null;
        pendingActionP2 = null;
        ball.setTranslateX(0);
        ball.setTranslateY(0);
        updateStatus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
