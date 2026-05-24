import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class FootballManagementSimulation extends Application {

    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final String[] FORMATIONS = {"4-4-2", "4-3-3", "3-5-2", "5-3-2", "4-2-3-1"};

    private GameState gameState;
    private StackPane mainContainer;
    private Timeline gameLoop;
    private MatchEngine matchEngine;

    // Game data structures
    static class Player {
        String name;
        String position;
        int rating;
        int pace, shooting, passing, defending, physical;
        int stamina = 100;
        boolean injured = false;

        Player(String name, String position, int rating) {
            this.name = name;
            this.position = position;
            this.rating = rating;
            this.pace = rating + (int)(Math.random() * 10 - 5);
            this.shooting = rating + (int)(Math.random() * 10 - 5);
            this.passing = rating + (int)(Math.random() * 10 - 5);
            this.defending = rating + (int)(Math.random() * 10 - 5);
            this.physical = rating + (int)(Math.random() * 10 - 5);
        }
    }

    static class Team {
        String name;
        String formation;
        Color primaryColor;
        Color secondaryColor;
        List<Player> squad;
        List<Player> startingXI;
        int wins, draws, losses;
        int goalsFor, goalsAgainst;
        int budget;
        int morale = 100;
        int fitness = 100;

        Team(String name, Color primary, Color secondary, int budget) {
            this.name = name;
            this.primaryColor = primary;
            this.secondaryColor = secondary;
            this.budget = budget;
            this.squad = new ArrayList<>();
            this.startingXI = new ArrayList<>();
            generateSquad();
        }

        private void generateSquad() {
            String[] positions = {"GK", "DEF", "DEF", "DEF", "DEF", "MID", "MID", "MID", "MID", "FWD", "FWD"};
            String[] names = {
                    "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
                    "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
                    "Thomas", "Taylor", "Moore", "Jackson", "Martin"
            };

            for (int i = 0; i < 18; i++) {
                String pos = i < 11 ? positions[i] : (i < 15 ? "DEF" : (i < 17 ? "MID" : "FWD"));
                int baseRating = 65 + (int)(Math.random() * 25);
                squad.add(new Player(names[i] + " " + (i + 1), pos, baseRating));
            }
            formation = "4-4-2";
            selectStartingXI();
        }

        void selectStartingXI() {
            startingXI.clear();
            Map<String, Integer> needed = parseFormation(formation);

            for (String pos : Arrays.asList("GK", "DEF", "MID", "FWD")) {
                int count = needed.getOrDefault(pos, 0);
                List<Player> available = new ArrayList<>();
                for (Player p : squad) {
                    if (p.position.equals(pos) && !startingXI.contains(p)) {
                        available.add(p);
                    }
                }
                available.sort((a, b) -> b.rating - a.rating);
                for (int i = 0; i < Math.min(count, available.size()); i++) {
                    startingXI.add(available.get(i));
                }
            }
        }

        Map<String, Integer> parseFormation(String form) {
            Map<String, Integer> map = new HashMap<>();
            map.put("GK", 1);
            String[] parts = form.split("-");
            map.put("DEF", Integer.parseInt(parts[0]));
            map.put("MID", Integer.parseInt(parts[1]));
            map.put("FWD", Integer.parseInt(parts[2]));
            return map;
        }

        int getOverallRating() {
            if (startingXI.isEmpty()) return 70;
            return startingXI.stream().mapToInt(p -> p.rating).sum() / startingXI.size();
        }
    }

    static class MatchEvent {
        int minute;
        String type; // "GOAL", "YELLOW", "RED", "SUB", "INJURY"
        String description;
        boolean isHome;

        MatchEvent(int minute, String type, String desc, boolean isHome) {
            this.minute = minute;
            this.type = type;
            this.description = desc;
            this.isHome = isHome;
        }
    }

    static class MatchResult {
        int homeScore, awayScore;
        List<MatchEvent> events = new ArrayList<>();
        int possessionHome, possessionAway;
        int shotsHome, shotsAway;
        int shotsOnTargetHome, shotsOnTargetAway;
    }

    enum GameMode { PVP, PVC }
    enum GameState { MAIN_MENU, TEAM_SETUP, MATCH, RESULTS, LEAGUE_TABLE }

    static class GameData {
        Team player1Team;
        Team player2Team;
        GameMode mode;
        List<MatchResult> matchHistory = new ArrayList<>();
    }

    @Override
    public void start(Stage primaryStage) {
        mainContainer = new StackPane();
        mainContainer.setStyle("-fx-background-color: #1a1a2e;");

        Scene scene = new Scene(mainContainer, WINDOW_WIDTH, WINDOW_HEIGHT);

        showMainMenu();

        primaryStage.setTitle("Football Management Simulation 2026");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void showMainMenu() {
        VBox menu = new VBox(30);
        menu.setAlignment(Pos.CENTER);
        menu.setPadding(new Insets(50));

        // Animated title
        Text title = new Text("⚽ FOOTBALL MANAGER");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        title.setFill(Color.WHITE);

        DropShadow glow = new DropShadow();
        glow.setColor(Color.LIMEGREEN);
        glow.setRadius(20);
        title.setEffect(glow);

        // Pulse animation
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.5), title);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.05);
        pulse.setToY(1.05);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        // Subtitle
        Text subtitle = new Text("2026 Edition");
        subtitle.setFont(Font.font("Arial", 24));
        subtitle.setFill(Color.LIGHTGRAY);

        // Menu buttons with hover effects
        Button pvpBtn = createStyledButton("👥 2 Players (PvP)", "#4CAF50");
        Button pvcBtn = createStyledButton("🤖 vs Computer (PvC)", "#2196F3");
        Button quitBtn = createStyledButton("❌ Quit", "#f44336");

        pvpBtn.setOnAction(e -> {
            gameState = GameState.TEAM_SETUP;
            showTeamSetup(GameMode.PVP);
        });

        pvcBtn.setOnAction(e -> {
            gameState = GameState.TEAM_SETUP;
            showTeamSetup(GameMode.PVC);
        });

        quitBtn.setOnAction(e -> System.exit(0));

        menu.getChildren().addAll(title, subtitle, pvpBtn, pvcBtn, quitBtn);
        mainContainer.getChildren().setAll(menu);
    }

    private Button createStyledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        btn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 15 40; " +
                        "-fx-background-radius: 30; -fx-cursor: hand;", color
        ));

        btn.setOnMouseEntered(e -> {
            btn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 15 40; " +
                            "-fx-background-radius: 30; -fx-cursor: hand; -fx-scale-x: 1.1; -fx-scale-y: 1.1;", color
            ));
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 15 40; " +
                            "-fx-background-radius: 30; -fx-cursor: hand;", color
            ));
        });

        return btn;
    }

    private void showTeamSetup(GameMode mode) {
        VBox setup = new VBox(20);
        setup.setAlignment(Pos.CENTER);
        setup.setPadding(new Insets(30));

        Text title = new Text("Team Setup");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setFill(Color.WHITE);

        HBox teamsBox = new HBox(50);
        teamsBox.setAlignment(Pos.CENTER);

        // Team 1 setup
        VBox team1Box = createTeamSetupPanel("Player 1", Color.BLUE, Color.WHITE);

        // Team 2 setup (or Computer)
        VBox team2Box = createTeamSetupPanel(mode == GameMode.PVC ? "Computer" : "Player 2", Color.RED, Color.WHITE);

        teamsBox.getChildren().addAll(team1Box, team2Box);

        Button startBtn = createStyledButton("▶ Start Match", "#FF9800");
        startBtn.setOnAction(e -> startMatch(mode));

        Button backBtn = createStyledButton("◀ Back", "#757575");
        backBtn.setOnAction(e -> showMainMenu());

        setup.getChildren().addAll(title, teamsBox, startBtn, backBtn);
        mainContainer.getChildren().setAll(setup);
    }

    private VBox createTeamSetupPanel(String playerName, Color primary, Color secondary) {
        VBox panel = new VBox(15);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 15;");
        panel.setPrefWidth(400);

        Text nameLabel = new Text(playerName);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        nameLabel.setFill(Color.WHITE);

        // Team name input
        TextField teamNameField = new TextField(playerName + " FC");
        teamNameField.setStyle("-fx-font-size: 16; -fx-padding: 10;");

        // Formation selector
        ComboBox<String> formationBox = new ComboBox<>();
        formationBox.getItems().addAll(FORMATIONS);
        formationBox.setValue("4-4-2");
        formationBox.setStyle("-fx-font-size: 14;");

        // Color preview
        HBox colorPreview = new HBox(10);
        colorPreview.setAlignment(Pos.CENTER);
        Rectangle primaryRect = new Rectangle(50, 30, primary);
        Rectangle secondaryRect = new Rectangle(50, 30, secondary);
        colorPreview.getChildren().addAll(
                new Text("Colors:") {{ setFill(Color.WHITE); }},
                primaryRect, secondaryRect
        );

        // Squad list
        ListView<String> squadList = new ListView<>();
        squadList.setPrefHeight(200);
        squadList.setStyle("-fx-font-size: 12;");

        // Generate sample squad
        String[] positions = {"GK", "DEF", "DEF", "DEF", "DEF", "MID", "MID", "MID", "MID", "FWD", "FWD"};
        String[] names = {"G.Keeper", "Defender A", "Defender B", "Defender C", "Defender D",
                "Midfielder A", "Midfielder B", "Midfielder C", "Midfielder D",
                "Striker A", "Striker B"};
        for (int i = 0; i < 11; i++) {
            squadList.getItems().add(String.format("%s - %s (Rating: %d)", positions[i], names[i], 75 + (int)(Math.random() * 15)));
        }

        panel.getChildren().addAll(nameLabel, teamNameField,
                new Text("Formation:") {{ setFill(Color.WHITE); }},
                formationBox, colorPreview,
                new Text("Starting XI:") {{ setFill(Color.WHITE); }},
                squadList);

        return panel;
    }

    private void startMatch(GameMode mode) {
        // Initialize teams
        Team homeTeam = new Team("Player 1 FC", Color.BLUE, Color.WHITE, 50000000);
        Team awayTeam = new Team(mode == GameMode.PVC ? "Computer FC" : "Player 2 FC", Color.RED, Color.WHITE, 50000000);

        matchEngine = new MatchEngine(homeTeam, awayTeam);
        showMatchScreen(homeTeam, awayTeam, mode);
    }

    private void showMatchScreen(Team home, Team away, GameMode mode) {
        BorderPane matchLayout = new BorderPane();
        matchLayout.setStyle("-fx-background-color: #0f3460;");

        // Top: Scoreboard
        HBox scoreboard = createScoreboard(home, away);
        matchLayout.setTop(scoreboard);

        // Center: Pitch visualization
        StackPane pitchContainer = createPitchVisualization(home, away);
        matchLayout.setCenter(pitchContainer);

        // Bottom: Controls and commentary
        VBox bottomPanel = createMatchControls(home, away, mode);
        matchLayout.setBottom(bottomPanel);

        // Right: Match stats
        VBox statsPanel = createStatsPanel(home, away);
        matchLayout.setRight(statsPanel);

        mainContainer.getChildren().setAll(matchLayout);

        // Start match simulation
        startMatchSimulation(home, away, mode);
    }

    private HBox createScoreboard(Team home, Team away) {
        HBox scoreboard = new HBox(30);
        scoreboard.setAlignment(Pos.CENTER);
        scoreboard.setPadding(new Insets(20));
        scoreboard.setStyle("-fx-background-color: #16213e;");

        // Home team
        VBox homeBox = new VBox(5);
        homeBox.setAlignment(Pos.CENTER);
        Circle homeBadge = new Circle(25, home.primaryColor);
        Text homeName = new Text(home.name);
        homeName.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        homeName.setFill(Color.WHITE);
        homeBox.getChildren().addAll(homeBadge, homeName);

        // Score
        VBox scoreBox = new VBox(5);
        scoreBox.setAlignment(Pos.CENTER);
        Text scoreText = new Text("0 - 0");
        scoreText.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        scoreText.setFill(Color.WHITE);
        Text timeText = new Text("0'");
        timeText.setFont(Font.font("Arial", 24));
        timeText.setFill(Color.LIMEGREEN);
        scoreBox.getChildren().addAll(scoreText, timeText);

        // Away team
        VBox awayBox = new VBox(5);
        awayBox.setAlignment(Pos.CENTER);
        Circle awayBadge = new Circle(25, away.primaryColor);
        Text awayName = new Text(away.name);
        awayName.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        awayName.setFill(Color.WHITE);
        awayBox.getChildren().addAll(awayBadge, awayName);

        scoreboard.getChildren().addAll(homeBox, scoreBox, awayBox);
        return scoreboard;
    }

    private StackPane createPitchVisualization(Team home, Team away) {
        StackPane pitch = new StackPane();

        // Grass field
        Rectangle field = new Rectangle(700, 450);
        field.setFill(Color.DARKGREEN);
        field.setStroke(Color.WHITE);
        field.setStrokeWidth(3);

        // Center line
        Rectangle centerLine = new Rectangle(2, 450);
        centerLine.setFill(Color.WHITE);

        // Center circle
        Circle centerCircle = new Circle(50);
        centerCircle.setFill(Color.TRANSPARENT);
        centerCircle.setStroke(Color.WHITE);
        centerCircle.setStrokeWidth(2);

        // Goals
        Rectangle homeGoal = new Rectangle(20, 100);
        homeGoal.setFill(Color.TRANSPARENT);
        homeGoal.setStroke(Color.WHITE);
        homeGoal.setStrokeWidth(2);
        homeGoal.setTranslateX(-340);

        Rectangle awayGoal = new Rectangle(20, 100);
        awayGoal.setFill(Color.TRANSPARENT);
        awayGoal.setStroke(Color.WHITE);
        awayGoal.setStrokeWidth(2);
        awayGoal.setTranslateX(340);

        // Player dots (simplified representation)
        Pane playersLayer = new Pane();
        playersLayer.setPrefSize(700, 450);

        // Add animated ball
        Circle ball = new Circle(8, Color.WHITE);
        ball.setEffect(new DropShadow(5, Color.BLACK));

        // Ball animation
        PathTransition ballPath = new PathTransition();
        ballPath.setDuration(Duration.seconds(3));
        ballPath.setNode(ball);
        ballPath.setCycleCount(Animation.INDEFINITE);
        ballPath.setAutoReverse(true);

        pitch.getChildren().addAll(field, centerLine, centerCircle, homeGoal, awayGoal, playersLayer, ball);

        return pitch;
    }

    private VBox createMatchControls(Team home, Team away, GameMode mode) {
        VBox controls = new VBox(10);
        controls.setPadding(new Insets(15));
        controls.setStyle("-fx-background-color: #16213e;");

        // Commentary box
        TextArea commentary = new TextArea();
        commentary.setEditable(false);
        commentary.setPrefHeight(100);
        commentary.setStyle("-fx-font-size: 14; -fx-control-inner-background: #1a1a2e; -fx-text-fill: white;");
        commentary.setText("🏟️ Match begins! Welcome to the stadium...\n");

        // Control buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button tacticsBtn = createStyledButton("📋 Tactics", "#9C27B0");
        Button subBtn = createStyledButton("🔄 Substitute", "#FF9800");
        Button simBtn = createStyledButton("⏩ Simulate", "#4CAF50");
        Button pauseBtn = createStyledButton("⏸ Pause", "#f44336");

        buttonBox.getChildren().addAll(tacticsBtn, subBtn, simBtn, pauseBtn);

        // Progress bar
        ProgressBar matchProgress = new ProgressBar(0);
        matchProgress.setPrefWidth(600);
        matchProgress.setStyle("-fx-accent: #4CAF50;");

        controls.getChildren().addAll(commentary, buttonBox, matchProgress);

        return controls;
    }

    private VBox createStatsPanel(Team home, Team away) {
        VBox stats = new VBox(15);
        stats.setPadding(new Insets(20));
        stats.setPrefWidth(250);
        stats.setStyle("-fx-background-color: #16213e;");

        Text title = new Text("📊 Match Stats");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        title.setFill(Color.WHITE);

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(10);
        statsGrid.setVgap(10);
        statsGrid.setAlignment(Pos.CENTER);

        String[] statNames = {"Possession", "Shots", "Shots on Target", "Corners", "Fouls"};
        for (int i = 0; i < statNames.length; i++) {
            Text homeVal = new Text("0");
            homeVal.setFill(Color.WHITE);
            homeVal.setFont(Font.font("Arial", FontWeight.BOLD, 16));

            Text statName = new Text(statNames[i]);
            statName.setFill(Color.LIGHTGRAY);

            Text awayVal = new Text("0");
            awayVal.setFill(Color.WHITE);
            awayVal.setFont(Font.font("Arial", FontWeight.BOLD, 16));

            statsGrid.add(homeVal, 0, i);
            statsGrid.add(statName, 1, i);
            statsGrid.add(awayVal, 2, i);
        }

        stats.getChildren().addAll(title, statsGrid);
        return stats;
    }

    private void startMatchSimulation(Team home, Team away, GameMode mode) {
        matchEngine = new MatchEngine(home, away);

        gameLoop = new Timeline(
                new KeyFrame(Duration.seconds(0.1), e -> {
                    matchEngine.simulateMinute();
                    updateMatchDisplay();

                    if (matchEngine.isMatchFinished()) {
                        gameLoop.stop();
                        showMatchResults();
                    }
                })
        );
        gameLoop.setCycleCount(90);
        gameLoop.play();
    }

    private void updateMatchDisplay() {
        // Update score, time, stats, commentary
    }

    private void showMatchResults() {
        VBox results = new VBox(20);
        results.setAlignment(Pos.CENTER);
        results.setPadding(new Insets(50));
        results.setStyle("-fx-background-color: #1a1a2e;");

        MatchResult result = matchEngine.getResult();

        Text title = new Text("🏆 MATCH RESULT");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setFill(Color.GOLD);

        Text scoreText = new Text(String.format("%d - %d", result.homeScore, result.awayScore));
        scoreText.setFont(Font.font("Arial", FontWeight.BOLD, 72));
        scoreText.setFill(Color.WHITE);

        // Match events timeline
        VBox eventsBox = new VBox(5);
        eventsBox.setAlignment(Pos.CENTER);
        for (MatchEvent event : result.events) {
            Text eventText = new Text(String.format("%d' - %s", event.minute, event.description));
            eventText.setFill(event.type.equals("GOAL") ? Color.GOLD : Color.WHITE);
            eventText.setFont(Font.font("Arial", event.type.equals("GOAL") ? FontWeight.BOLD : FontWeight.NORMAL, 14));
            eventsBox.getChildren().add(eventText);
        }

        Button continueBtn = createStyledButton("Continue", "#4CAF50");
        continueBtn.setOnAction(e -> showMainMenu());

        results.getChildren().addAll(title, scoreText, eventsBox, continueBtn);
        mainContainer.getChildren().setAll(results);
    }

    // Match Engine
    class MatchEngine {
        private Team home, away;
        private int currentMinute;
        private int homeScore, awayScore;
        private MatchResult result;
        private Random random = new Random();
        private boolean matchFinished = false;

        MatchEngine(Team home, Team away) {
            this.home = home;
            this.away = away;
            this.result = new MatchResult();
        }

        void simulateMinute() {
            currentMinute++;

            // Calculate probabilities based on team ratings
            int homeStrength = home.getOverallRating() + home.morale / 10;
            int awayStrength = away.getOverallRating() + away.morale / 10;
            int totalStrength = homeStrength + awayStrength;

            // Possession update
            result.possessionHome = (homeStrength * 100) / totalStrength;
            result.possessionAway = 100 - result.possessionHome;

            // Random events
            if (random.nextDouble() < 0.15) { // Shot attempt
                boolean homeAttacking = random.nextInt(100) < result.possessionHome;
                if (homeAttacking) {
                    result.shotsHome++;
                    if (random.nextDouble() < 0.3) { // On target
                        result.shotsOnTargetHome++;
                        if (random.nextDouble() < 0.25) { // Goal
                            homeScore++;
                            result.events.add(new MatchEvent(currentMinute, "GOAL",
                                    home.startingXI.get(random.nextInt(10)).name + " scores!", true));
                        }
                    }
                } else {
                    result.shotsAway++;
                    if (random.nextDouble() < 0.3) {
                        result.shotsOnTargetAway++;
                        if (random.nextDouble() < 0.25) {
                            awayScore++;
                            result.events.add(new MatchEvent(currentMinute, "GOAL",
                                    away.startingXI.get(random.nextInt(10)).name + " scores!", false));
                        }
                    }
                }
            }

            // Card events
            if (random.nextDouble() < 0.02) {
                boolean homeFoul = random.nextBoolean();
                result.events.add(new MatchEvent(currentMinute, "YELLOW",
                        "Yellow card shown", homeFoul));
            }
        }

        boolean isMatchFinished() { return currentMinute >= 90; }
        MatchResult getResult() {
            result.homeScore = homeScore;
            result.awayScore = awayScore;
            return result;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}