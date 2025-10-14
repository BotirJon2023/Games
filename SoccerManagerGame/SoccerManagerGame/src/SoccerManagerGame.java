import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.animation.AnimationTimer;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import java.util.ArrayList;
import java.util.Random;

public class SoccerManagerGame extends Application {
    private static final int FIELD_WIDTH = 800;
    private static final int FIELD_HEIGHT = 600;
    private static final int PLAYER_SIZE = 20;
    private static final int BALL_SIZE = 10;

    private Team homeTeam;
    private Team awayTeam;
    private Ball ball;
    private GameState gameState;
    private Canvas canvas;
    private GraphicsContext gc;
    private Label scoreLabel;
    private Label timeLabel;
    private boolean isMatchRunning;
    private double gameTime;
    private Random random;

    // Team management variables
    private ArrayList<Player> availablePlayers;
    private VBox managementPanel;
    private TextField teamNameField;
    private Button startMatchButton;

    private enum GameState {
        MENU, TEAM_MANAGEMENT, MATCH, POST_MATCH
    }

    static class Player {
        String name;
        int attack;
        int defense;
        int stamina;
        double x, y;
        boolean hasBall;
        Team team;

        Player(String name, int attack, int defense, int stamina) {
            this.name = name;
            this.attack = attack;
            this.defense = defense;
            this.stamina = stamina;
            this.hasBall = false;
        }
    }

    static class Team {
        String name;
        ArrayList<Player> players;
        int score;
        Color color;

        Team(String name, Color color) {
            this.name = name;
            this.players = new ArrayList<>();
            this.score = 0;
            this.color = color;
        }

        void addPlayer(Player player) {
            player.team = this;
            players.add(player);
        }
    }

    static class Ball {
        double x, y;
        double dx, dy;
        Player possessor;

        Ball(double x, double y) {
            this.x = x;
            this.y = y;
            this.dx = 0;
            this.dy = 0;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        random = new Random();
        gameState = GameState.MENU;
        isMatchRunning = false;
        gameTime = 0;

        // Initialize teams and ball
        homeTeam = new Team("Home Team", Color.BLUE);
        awayTeam = new Team("Away Team", Color.RED);
        ball = new Ball(FIELD_WIDTH / 2, FIELD_HEIGHT / 2);

        // Initialize available players
        initializeAvailablePlayers();

        // Set up canvas
        canvas = new Canvas(FIELD_WIDTH, FIELD_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        // Set up UI components
        scoreLabel = new Label("Score: 0 - 0");
        scoreLabel.setFont(new Font("Arial", 20));
        timeLabel = new Label("Time: 0:00");
        timeLabel.setFont(new Font("Arial", 20));

        // Team management UI
        teamNameField = new TextField("Enter Team Name");
        startMatchButton = new Button("Start Match");
        startMatchButton.setOnAction(e -> startMatch());

        managementPanel = new VBox(10);
        managementPanel.setPadding(new Insets(10));
        managementPanel.getChildren().addAll(
                new Label("Team Management"),
                teamNameField,
                startMatchButton
        );

        // Main layout
        Pane root = new Pane();
        root.getChildren().addAll(canvas, scoreLabel, timeLabel, managementPanel);
        scoreLabel.setLayoutY(10);
        timeLabel.setLayoutX(FIELD_WIDTH - 100);
        timeLabel.setLayoutY(10);
        managementPanel.setLayoutX(FIELD_WIDTH + 10);

        Scene scene = new Scene(root, FIELD_WIDTH + 200, FIELD_HEIGHT);
        primaryStage.setTitle("Soccer Manager Game");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Initialize player positions
        initializePlayerPositions();

        // Start animation
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render();
            }
        }.start();
    }

    private void initializeAvailablePlayers() {
        availablePlayers = new ArrayList<>();
        String[] names = {"Messi", "Ronaldo", "Neymar", "Mbappe", "Salah", "De Bruyne",
                "Lewandowski", "Kane", "Haaland", "Benzema"};

        for (String name : names) {
            availablePlayers.add(new Player(name,
                    random.nextInt(50) + 50,
                    random.nextInt(50) + 50,
                    random.nextInt(50) + 50));
        }
    }

    private void initializePlayerPositions() {
        // Home team 4-4-2 formation
        double[][] homePositions = {
                {100, FIELD_HEIGHT/2}, // Goalkeeper
                {200, 100}, {200, 250}, {200, 350}, {200, 500}, // Defenders
                {350, 100}, {350, 250}, {350, 350}, {350, 500}, // Midfielders
                {500, 200}, {500, 400} // Forwards
        };

        // Away team 4-3-3 formation
        double[][] awayPositions = {
                {FIELD_WIDTH-100, FIELD_HEIGHT/2}, // Goalkeeper
                {FIELD_WIDTH-200, 100}, {FIELD_WIDTH-200, 250},
                {FIELD_WIDTH-200, 350}, {FIELD_WIDTH-200, 500}, // Defenders
                {FIELD_WIDTH-350, 200}, {FIELD_WIDTH-350, 300},
                {FIELD_WIDTH-350, 400}, // Midfielders
                {FIELD_WIDTH-500, 150}, {FIELD_WIDTH-500, 300},
                {FIELD_WIDTH-500, 450} // Forwards
        };

        // Assign players to teams if not already assigned
        if (homeTeam.players.isEmpty()) {
            for (int i = 0; i < 11 && i < availablePlayers.size(); i++) {
                Player player = availablePlayers.get(i);
                player.x = homePositions[i][0];
                player.y = homePositions[i][1];
                homeTeam.addPlayer(player);
            }
        }

        if (awayTeam.players.isEmpty()) {
            for (int i = 0; i < 11 && i < availablePlayers.size(); i++) {
                Player player = new Player("Away " + availablePlayers.get(i).name,
                        random.nextInt(50) + 50,
                        random.nextInt(50) + 50,
                        random.nextInt(50) + 50);
                player.x = awayPositions[i][0];
                player.y = awayPositions[i][1];
                awayTeam.addPlayer(player);
            }
        }
    }

    private void startMatch() {
        if (!teamNameField.getText().isEmpty()) {
            homeTeam.name = teamNameField.getText();
        }
        gameState = GameState.MATCH;
        isMatchRunning = true;
        gameTime = 0;
        homeTeam.score = 0;
        awayTeam.score = 0;
        managementPanel.setVisible(false);
        initializePlayerPositions();
        ball.x = FIELD_WIDTH / 2;
        ball.y = FIELD_HEIGHT / 2;
        ball.dx = 0;
        ball.dy = 0;
        ball.possessor = null;
    }

    private void update() {
        if (gameState != GameState.MATCH || !isMatchRunning) return;

        gameTime += 1.0/60.0; // Update time (assuming 60 FPS)

        // Update time display
        int minutes = (int)(gameTime / 60);
        int seconds = (int)(gameTime % 60);
        timeLabel.setText(String.format("Time: %d:%02d", minutes, seconds));

        // Check if match ended
        if (gameTime >= 90) {
            isMatchRunning = false;
            gameState = GameState.POST_MATCH;
            return;
        }

        // Update ball possession
        if (ball.possessor == null) {
            findNewBallPossessor();
        }

        // Update player movements
        updatePlayerMovements();

        // Update ball movement
        updateBallMovement();

        // Check for goal scoring
        checkForGoals();
    }

    private void findNewBallPossessor() {
        Player closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Player player : homeTeam.players) {
            double distance = Math.hypot(player.x - ball.x, player.y - ball.y);
            if (distance < minDistance && distance < 30) {
                minDistance = distance;
                closest = player;
            }
        }

        for (Player player : awayTeam.players) {
            double distance = Math.hypot(player.x - ball.x, player.y - ball.y);
            if (distance < minDistance && distance < 30) {
                minDistance = distance;
                closest = player;
            }
        }

        if (closest != null) {
            ball.possessor = closest;
            closest.hasBall = true;
            ball.dx = 0;
            ball.dy = 0;
        }
    }

    private void updatePlayerMovements() {
        for (Player player : homeTeam.players) {
            updatePlayerPosition(player);
        }
        for (Player player : awayTeam.players) {
            updatePlayerPosition(player);
        }
    }

    private void updatePlayerPosition(Player player) {
        if (player.hasBall) {
            // Player with ball moves toward opponent goal
            double targetX = player.team == homeTeam ? FIELD_WIDTH - 100 : 100;
            double dx = targetX - player.x;
            double dy = FIELD_HEIGHT/2 - player.y;
            double distance = Math.hypot(dx, dy);
            if (distance > 5) {
                double speed = player.stamina * 0.05;
                player.x += (dx / distance) * speed;
                player.y += (dy / distance) * speed;
            }
        } else {
            // Chase ball or return to position
            double targetX = ball.x;
            double targetY = ball.y;
            double dx = targetX - player.x;
            double dy = targetY - player.y;
            double distance = Math.hypot(dx, dy);
            if (distance > 5 && distance < 200) {
                double speed = player.stamina * 0.03;
                player.x += (dx / distance) * speed;
                player.y += (dy / distance) * speed;
            }
        }
    }

    private void updateBallMovement() {
        if (ball.possessor != null) {
            ball.x = ball.possessor.x;
            ball.y = ball.possessor.y;

            // Chance to pass or shoot
            if (random.nextDouble() < 0.01) {
                if (random.nextDouble() < 0.3 &&
                        ((ball.possessor.team == homeTeam && ball.x > FIELD_WIDTH-200) ||
                                (ball.possessor.team == awayTeam && ball.x < 200))) {
                    shootBall();
                } else {
                    passBall();
                }
            }
        } else {
            ball.x += ball.dx;
            ball.y += ball.dy;
            ball.dx *= 0.99; // Friction
            ball.dy *= 0.99;

            // Keep ball in bounds
            if (ball.x < 0 || ball.x > FIELD_WIDTH) ball.dx = -ball.dx;
            if (ball.y < 0 || ball.y > FIELD_HEIGHT) ball.dy = -ball.dy;
        }
    }

    private void passBall() {
        if (ball.possessor == null) return;

        ArrayList<Player> teammates = ball.possessor.team.players;
        Player target = teammates.get(random.nextInt(teammates.size()));
        if (target != ball.possessor) {
            ball.possessor.hasBall = false;
            ball.possessor = target;
            target.hasBall = true;
        }
    }

    private void shootBall() {
        if (ball.possessor == null) return;

        ball.possessor.hasBall = false;
        ball.possessor = null;
        double targetX = ball.possessor.team == homeTeam ? FIELD_WIDTH : 0;
        double targetY = FIELD_HEIGHT/2;
        double dx = targetX - ball.x;
        double dy = targetY - ball.y;
        double distance = Math.hypot(dx, dy);
        ball.dx = (dx / distance) * 10;
        ball.dy = (dy / distance) * 10;
    }

    private void checkForGoals() {
        if (ball.x < 50 && ball.y > FIELD_HEIGHT/2 - 50 && ball.y < FIELD_HEIGHT/2 + 50) {
            awayTeam.score++;
            resetAfterGoal();
        } else if (ball.x > FIELD_WIDTH-50 && ball.y > FIELD_HEIGHT/2 - 50 && ball.y < FIELD_HEIGHT/2 + 50) {
            homeTeam.score++;
            resetAfterGoal();
        }
        scoreLabel.setText(String.format("Score: %d - %d", homeTeam.score, awayTeam.score));
    }

    private void resetAfterGoal() {
        ball.x = FIELD_WIDTH/2;
        ball.y = FIELD_HEIGHT/2;
        ball.dx = 0;
        ball.dy = 0;
        if (ball.possessor != null) {
            ball.possessor.hasBall = false;
            ball.possessor = null;
        }
        initializePlayerPositions();
    }

    private void render() {
        // Clear canvas
        gc.setFill(Color.GREEN);
        gc.fillRect(0, 0, FIELD_WIDTH, FIELD_HEIGHT);

        // Draw field markings
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, FIELD_WIDTH, FIELD_HEIGHT);
        gc.strokeLine(FIELD_WIDTH/2, 0, FIELD_WIDTH/2, FIELD_HEIGHT);
        gc.strokeOval(FIELD_WIDTH/2 - 50, FIELD_HEIGHT/2 - 50, 100, 100);

        // Draw goals
        gc.strokeRect(0, FIELD_HEIGHT/2 - 50, 50, 100);
        gc.strokeRect(FIELD_WIDTH-50, FIELD_HEIGHT/2 - 50, 50, 100);

        // Draw players
        for (Player player : homeTeam.players) {
            gc.setFill(homeTeam.color);
            gc.fillOval(player.x - PLAYER_SIZE/2, player.y - PLAYER_SIZE/2,
                    PLAYER_SIZE, PLAYER_SIZE);
            gc.setFill(Color.WHITE);
            gc.fillText(player.name.substring(0, 3), player.x - 10, player.y - 10);
        }

        for (Player player : awayTeam.players) {
            gc.setFill(awayTeam.color);
            gc.fillOval(player.x - PLAYER_SIZE/2, player.y - PLAYER_SIZE/2,
                    PLAYER_SIZE, PLAYER_SIZE);
            gc.setFill(Color.WHITE);
            gc.fillText(player.name.substring(0, 3), player.x - 10, player.y - 10);
        }

        // Draw ball
        gc.setFill(Color.WHITE);
        gc.fillOval(ball.x - BALL_SIZE/2, ball.y - BALL_SIZE/2, BALL_SIZE, BALL_SIZE);

        // Draw UI based on game state
        gc.setFill(Color.BLACK);
        gc.setFont(new Font("Arial", 30));
        if (gameState == GameState.MENU) {
            gc.fillText("Soccer Manager Game", FIELD_WIDTH/2 - 150, FIELD_HEIGHT/2);
            gc.fillText("Click 'Team Management' to start", FIELD_WIDTH/2 - 150, FIELD_HEIGHT/2 + 40);
            managementPanel.setVisible(true);
        } else if (gameState == GameState.POST_MATCH) {
            gc.fillText(String.format("Final Score: %d - %d", homeTeam.score, awayTeam.score),
                    FIELD_WIDTH/2 - 100, FIELD_HEIGHT/2);
            gc.fillText("Press Start Match to play again", FIELD_WIDTH/2 - 150, FIELD_HEIGHT/2 + 40);
            managementPanel.setVisible(true);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}