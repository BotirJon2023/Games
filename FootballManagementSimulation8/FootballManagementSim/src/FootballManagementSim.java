import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import java.util.*;

public class FootballManagementSim extends Application {

    private Team playerTeam1, playerTeam2, computerTeam;
    private boolean isTwoPlayer = false;

    @Override
    public void start(Stage stage) {
        showMainMenu(stage);
    }

    private void showMainMenu(Stage stage) {
        VBox menu = new VBox(20);
        menu.setStyle("-fx-alignment: center; -fx-padding: 50; -fx-background-color: #0a3d62;");

        Label title = new Label("⚽ FOOTBALL MANAGER SIM");
        title.setStyle("-fx-font-size: 48px; -fx-text-fill: white; -fx-font-weight: bold;");

        Button btn1P = new Button("Play vs Computer");
        Button btn2P = new Button("2 Players Hotseat");

        btn1P.setStyle("-fx-font-size: 18px; -fx-padding: 15px 30px;");
        btn2P.setStyle("-fx-font-size: 18px; -fx-padding: 15px 30px;");

        btn1P.setOnAction(e -> startGame(stage, false));
        btn2P.setOnAction(e -> startGame(stage, true));

        menu.getChildren().addAll(title, btn1P, btn2P);
        Scene scene = new Scene(menu, 900, 600);
        stage.setScene(scene);
        stage.setTitle("Football Management Simulation");
        stage.show();
    }

    private void startGame(Stage stage, boolean twoPlayers) {
        this.isTwoPlayer = twoPlayers;

        // Create teams
        playerTeam1 = new Team("Real Madrid", false);
        if (twoPlayers) {
            playerTeam2 = new Team("Barcelona", false);
        } else {
            computerTeam = new Team("Bayern Munich", true);
        }

        showTeamManagement(stage);
    }

    private void showTeamManagement(Stage stage) {
        VBox root = new VBox(15);
        root.setStyle("-fx-padding: 20; -fx-background-color: #1e272e;");

        Label teamLabel = new Label("Your Team: " + playerTeam1.name);
        teamLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");

        ListView<String> squadList = new ListView<>();
        playerTeam1.players.forEach(p ->
                squadList.getItems().add(p.name + " - " + p.position + " (STR:" + p.strength + ")"));

        Button btnContinue = new Button("Proceed to Match");
        btnContinue.setOnAction(e -> simulateMatch(stage));

        root.getChildren().addAll(teamLabel, squadList, btnContinue);
        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
    }

    private void simulateMatch(Stage stage) {
        BorderPane matchScreen = new BorderPane();
        matchScreen.setStyle("-fx-background-color: #0a3d62;");

        // Scoreboard
        HBox scoreboard = new HBox(50);
        scoreboard.setStyle("-fx-alignment: center; -fx-padding: 20;");
        Label scoreLabel = new Label(playerTeam1.name + "  0 - 0  " +
                (isTwoPlayer ? playerTeam2.name : computerTeam.name));
        scoreLabel.setStyle("-fx-font-size: 28px; -fx-text-fill: white;");
        scoreboard.getChildren().add(scoreLabel);

        // Animated Pitch
        StackPane pitchPane = createAnimatedPitch(scoreLabel);

        matchScreen.setTop(scoreboard);
        matchScreen.setCenter(pitchPane);

        Scene scene = new Scene(matchScreen, 1000, 700);
        stage.setScene(scene);

        // Simulate match with animation
        simulateMatchLogic(pitchPane, scoreLabel);
    }

    private StackPane createAnimatedPitch(Label scoreLabel) {
        StackPane pitch = new StackPane();
        pitch.setStyle("-fx-background-color: #0b6623; -fx-border-color: white; -fx-border-width: 8;");

        // Pitch lines (simple)
        Pane field = new Pane();
        field.setPrefSize(800, 500);

        // Players as animated circles
        List<CirclePlayer> homePlayers = new ArrayList<>();
        List<CirclePlayer> awayPlayers = new ArrayList<>();

        // Home team (bottom)
        for (int i = 0; i < 11; i++) {
            CirclePlayer cp = new CirclePlayer(100 + (i % 5) * 120, 350 + (i / 5) * 80, "blue", "H");
            homePlayers.add(cp);
            field.getChildren().add(cp.circle);
        }

        // Away team (top)
        for (int i = 0; i < 11; i++) {
            CirclePlayer cp = new CirclePlayer(100 + (i % 5) * 120, 50 + (i / 5) * 80, "red", "A");
            awayPlayers.add(cp);
            field.getChildren().add(cp.circle);
        }

        // Ball
        Ball ball = new Ball(400, 250);
        field.getChildren().add(ball.circle);

        pitch.getChildren().add(field);

        // Store for animation
        pitch.setUserData(new MatchData(homePlayers, awayPlayers, ball, scoreLabel));

        return pitch;
    }

    private void simulateMatchLogic(StackPane pitch, Label scoreLabel) {
        MatchData data = (MatchData) pitch.getUserData();

        Timeline matchTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    // Random player movement animation
                    animatePlayers(data);

                    // Simple goal chance
                    if (Math.random() < 0.15) {
                        int homeGoals = Integer.parseInt(scoreLabel.getText().split(" - ")[0].split(" ")[1]);
                        int awayGoals = Integer.parseInt(scoreLabel.getText().split(" - ")[1].split(" ")[0]);

                        if (Math.random() > 0.5) {
                            scoreLabel.setText(playerTeam1.name + "  " + (++homeGoals) + " - " + awayGoals + "  " +
                                    (isTwoPlayer ? playerTeam2.name : computerTeam.name));
                        } else {
                            scoreLabel.setText(playerTeam1.name + "  " + homeGoals + " - " + (++awayGoals) + "  " +
                                    (isTwoPlayer ? playerTeam2.name : computerTeam.name));
                        }
                    }
                })
        );

        matchTimeline.setCycleCount(25); // ~25 seconds match
        matchTimeline.setOnFinished(e -> showMatchResult(pitch.getScene().getWindow()));
        matchTimeline.play();
    }

    private void animatePlayers(MatchData data) {
        // Animate home players
        data.homePlayers.forEach(p -> {
            double newX = p.circle.getCenterX() + (Math.random() - 0.5) * 60;
            double newY = p.circle.getCenterY() + (Math.random() - 0.5) * 40;
            TranslateTransition tt = new TranslateTransition(Duration.millis(800), p.circle);
            tt.setToX(newX - p.circle.getCenterX());
            tt.setToY(newY - p.circle.getCenterY());
            tt.play();
        });

        // Animate ball
        TranslateTransition ballMove = new TranslateTransition(Duration.millis(1200), data.ball.circle);
        ballMove.setToX(data.ball.circle.getCenterX() + (Math.random() - 0.5) * 300);
        ballMove.setToY(data.ball.circle.getCenterY() + (Math.random() - 0.5) * 200);
        ballMove.play();
    }

    private void showMatchResult(Object window) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Match Finished!");
        alert.setHeaderText("Final Score");
        alert.setContentText("Great match! Would you like to play again?");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

// Helper classes
class Team {
    String name;
    List<Player> players = new ArrayList<>();
    boolean isAI;

    public Team(String name, boolean isAI) {
        this.name = name;
        this.isAI = isAI;
        generateSquad();
    }

    private void generateSquad() {
        String[] names = {"Ronaldo", "Messi", "Mbappe", "Haaland", "De Bruyne", "Salah", "Kane", "Lewandowski"};
        String[] positions = {"GK", "DEF", "MID", "ATT"};

        for (int i = 0; i < 11; i++) {
            players.add(new Player(names[i % names.length] + " " + (i+1),
                    positions[i % 4],
                    60 + (int)(Math.random()*35)));
        }
    }
}

class Player {
    String name, position;
    int strength;
    public Player(String name, String position, int strength) {
        this.name = name;
        this.position = position;
        this.strength = strength;
    }
}

class CirclePlayer {
    javafx.scene.shape.Circle circle;
    public CirclePlayer(double x, double y, String color, String team) {
        circle = new javafx.scene.shape.Circle(x, y, 18);
        circle.setStyle("-fx-fill: " + color + "; -fx-stroke: white; -fx-stroke-width: 3;");
    }
}

class Ball {
    javafx.scene.shape.Circle circle;
    public Ball(double x, double y) {
        circle = new javafx.scene.shape.Circle(x, y, 12);
        circle.setStyle("-fx-fill: white; -fx-stroke: black;");
    }
}

class MatchData {
    List<CirclePlayer> homePlayers, awayPlayers;
    Ball ball;
    Label scoreLabel;

    public MatchData(List<CirclePlayer> home, List<CirclePlayer> away, Ball b, Label s) {
        this.homePlayers = home;
        this.awayPlayers = away;
        this.ball = b;
        this.scoreLabel = s;
    }
}