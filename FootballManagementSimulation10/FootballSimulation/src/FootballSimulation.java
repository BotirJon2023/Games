import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.util.Duration;

import java.util.*;

public class FootballSimulation extends Application {

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final int W = 900, H = 620;
    private static final double PITCH_X = 60, PITCH_Y = 60;
    private static final double PITCH_W = W - 120, PITCH_H = H - 160;
    private static final double GOAL_W = 12, GOAL_H = PITCH_H * 0.22;
    private static final double BALL_R = 10;
    private static final double PLAYER_R = 14;

    // ─── State ────────────────────────────────────────────────────────────────
    private Stage primaryStage;
    private boolean vsComputer = false;

    private double ballX, ballY, ballVX, ballVY;
    private int scoreA = 0, scoreB = 0;
    private int matchTime = 0;       // in seconds (0-90)
    private boolean matchRunning = false;
    private boolean paused = false;
    private String winner = null;

    private Label scoreLabel, timeLabel, commentaryLabel;
    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer gameLoop;
    private Timeline matchClock;
    private Timeline computerAI;

    private final String[] teamsA = {"FC Barcelona", "Real Madrid", "Liverpool FC", "Bayern Munich", "Juventus"};
    private final String[] teamsB = {"Manchester City", "PSG", "Chelsea FC", "Ajax", "AC Milan"};

    private String teamNameA = "Team A", teamNameB = "Team B";
    private Color teamColorA = Color.web("#e63946"), teamColorB = Color.web("#457b9d");

    private final Color[] TEAM_COLORS_A = {
        Color.web("#e63946"), Color.web("#9b2226"), Color.web("#c77dff"),
        Color.web("#e9c46a"), Color.web("#2d6a4f")
    };
    private final Color[] TEAM_COLORS_B = {
        Color.web("#457b9d"), Color.web("#0077b6"), Color.web("#06d6a0"),
        Color.web("#f4a261"), Color.web("#023e8a")
    };

    // ─── Players ──────────────────────────────────────────────────────────────
    private PlayerSprite[] playersA, playersB;

    // ─── Formation positions (relative to pitch, 0-1) ─────────────────────────
    private static final double[][] FORMATION_A = {
        {0.08, 0.50},  // GK
        {0.22, 0.18}, {0.22, 0.82}, {0.22, 0.38}, {0.22, 0.62},  // DEF
        {0.45, 0.20}, {0.45, 0.50}, {0.45, 0.80},  // MID
        {0.65, 0.30}, {0.65, 0.70}, {0.75, 0.50}   // ATT
    };
    private static final double[][] FORMATION_B = {
        {0.92, 0.50},  // GK
        {0.78, 0.18}, {0.78, 0.82}, {0.78, 0.38}, {0.78, 0.62},  // DEF
        {0.55, 0.20}, {0.55, 0.50}, {0.55, 0.80},  // MID
        {0.35, 0.30}, {0.35, 0.70}, {0.25, 0.50}   // ATT
    };

    // ─── Commentary ───────────────────────────────────────────────────────────
    private static final String[] COMMENTARY = {
        "Brilliant pass!", "Great movement!", "Into the box!", "Shot blocked!",
        "Fantastic save!", "Corner kick!", "Free kick awarded.", "Offside flag raised!",
        "Midfield battle intensifies!", "The crowd is electric!", "Incredible skill!",
        "The keeper claims it!", "Dangerous territory!", "Crossing opportunity!",
        "Pressure building up!", "What a dribble!", "Hard tackle there!",
        "Into space!", "Through ball!", "Powerful header!"
    };
    private static final String[] GOAL_COMMENTARY = {
        "GOOOOOAL!!!", "INCREDIBLE GOAL!!!", "WHAT A STRIKE!!!",
        "THE CROWD GOES WILD!!!", "SENSATIONAL!!!", "BRILLIANT FINISH!!!"
    };

    private final Random rng = new Random();
    private long lastCommentaryTime = 0;

    // ─── Entry ────────────────────────────────────────────────────────────────
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Football Manager Simulation");
        stage.setResizable(false);
        showMainMenu();
        stage.show();
    }

    // =========================================================================
    //  MAIN MENU
    // =========================================================================
    private void showMainMenu() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0d1b2a, #1b4332, #0d1b2a);");

        VBox box = new VBox(30);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));

        // Title
        Text title = new Text("⚽  FOOTBALL MANAGER");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 42));
        title.setFill(Color.WHITE);
        title.setEffect(new DropShadow(20, Color.web("#40916c")));

        Text subtitle = new Text("SIMULATION 2025");
        subtitle.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        subtitle.setFill(Color.web("#95d5b2"));

        // Animated pitch preview canvas
        Canvas mini = new Canvas(420, 200);
        drawMiniPitch(mini.getGraphicsContext2D());
        animateMiniPitch(mini.getGraphicsContext2D());

        // Mode buttons
        Button btn2P = styledButton("⚽  2 PLAYER MODE", "#40916c", "#1b4332");
        Button btnCPU = styledButton("🤖  vs COMPUTER", "#e76f51", "#9b2226");

        btn2P.setOnAction(e -> { vsComputer = false; showTeamSelect(); });
        btnCPU.setOnAction(e -> { vsComputer = true; showTeamSelect(); });

        HBox modes = new HBox(30, btn2P, btnCPU);
        modes.setAlignment(Pos.CENTER);

        Text footer = new Text("Use arrow keys / WASD to control players  •  Space to shoot");
        footer.setFont(Font.font("Arial", 13));
        footer.setFill(Color.web("#74c69d"));

        box.getChildren().addAll(title, subtitle, mini, modes, footer);
        root.getChildren().add(box);

        Scene scene = new Scene(root, W, H);
        primaryStage.setScene(scene);

        // Pulse animation on title
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(2), title);
        pulse.setFromX(1); pulse.setToX(1.05);
        pulse.setFromY(1); pulse.setToY(1.05);
        pulse.setAutoReverse(true); pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    private void drawMiniPitch(GraphicsContext gc) {
        gc.setFill(Color.web("#2d6a4f")); gc.fillRoundRect(0, 0, 420, 200, 10, 10);
        gc.setStroke(Color.web("#74c69d")); gc.setLineWidth(2);
        // boundary
        gc.strokeRect(10, 10, 400, 180);
        // centre
        gc.strokeLine(210, 10, 210, 190);
        gc.strokeOval(160, 65, 100, 70);
        gc.setFill(Color.WHITE); gc.fillOval(207, 97, 6, 6);
        // goals
        gc.setStroke(Color.WHITE);
        gc.strokeRect(10, 75, 20, 50);
        gc.strokeRect(390, 75, 20, 50);
    }

    private void animateMiniPitch(GraphicsContext gc) {
        final double[] bx = {120}, by = {100}, bvx = {2.5}, bvy = {1.5};
        new AnimationTimer() {
            public void handle(long now) {
                drawMiniPitch(gc);
                bx[0] += bvx[0]; by[0] += bvy[0];
                if (bx[0] < 20 || bx[0] > 400) bvx[0] = -bvx[0];
                if (by[0] < 20 || by[0] > 180) bvy[0] = -bvy[0];
                gc.setFill(Color.WHITE);
                gc.fillOval(bx[0] - 5, by[0] - 5, 10, 10);
            }
        }.start();
    }

    // =========================================================================
    //  TEAM SELECT
    // =========================================================================
    private void showTeamSelect() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0d1b2a, #1b2a3b);");

        VBox box = new VBox(25);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(35));

        Text title = new Text("SELECT YOUR TEAMS");
        title.setFont(Font.font("Arial Black", FontWeight.BOLD, 30));
        title.setFill(Color.WHITE);

        // Team A
        VBox aBox = teamSelectBox("TEAM A (WASD + E)", teamsA, TEAM_COLORS_A, true);
        // Team B
        String labelB = vsComputer ? "TEAM B (COMPUTER)" : "TEAM B (↑↓←→ + /)";
        VBox bBox = teamSelectBox(labelB, teamsB, TEAM_COLORS_B, false);

        HBox teams = new HBox(50, aBox, bBox);
        teams.setAlignment(Pos.CENTER);

        Button start = styledButton("▶  KICK OFF!", "#40916c", "#1b4332");
        start.setOnAction(e -> {
            initMatch();
            showMatch();
        });

        Button back = styledButton("← BACK", "#555", "#333");
        back.setOnAction(e -> showMainMenu());

        HBox btns = new HBox(20, back, start);
        btns.setAlignment(Pos.CENTER);

        box.getChildren().addAll(title, teams, btns);
        root.getChildren().add(box);
        primaryStage.setScene(new Scene(root, W, H));
    }

    private int selA = 0, selB = 0;

    private VBox teamSelectBox(String header, String[] teams, Color[] colors, boolean isA) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-background-radius: 12;");
        box.setPadding(new Insets(20));

        Text h = new Text(header);
        h.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        h.setFill(Color.web("#95d5b2"));

        ToggleGroup tg = new ToggleGroup();
        for (int i = 0; i < teams.length; i++) {
            final int idx = i;
            RadioButton rb = new RadioButton(teams[i]);
            rb.setToggleGroup(tg);
            rb.setFont(Font.font("Arial", 14));
            rb.setTextFill(Color.WHITE);
            rb.setStyle("-fx-cursor: hand;");
            rb.setSelected(i == 0);
            rb.selectedProperty().addListener((obs, o, n) -> {
                if (n) {
                    if (isA) { selA = idx; teamNameA = teams[idx]; teamColorA = colors[idx]; }
                    else      { selB = idx; teamNameB = teams[idx]; teamColorB = colors[idx]; }
                }
            });
            box.getChildren().add(rb);
        }
        box.getChildren().add(0, h);
        if (isA) { teamNameA = teams[0]; teamColorA = colors[0]; }
        else      { teamNameB = teams[0]; teamColorB = colors[0]; }
        return box;
    }

    // =========================================================================
    //  INIT MATCH
    // =========================================================================
    private void initMatch() {
        scoreA = 0; scoreB = 0; matchTime = 0; winner = null;
        paused = false; matchRunning = false;

        playersA = new PlayerSprite[11];
        playersB = new PlayerSprite[11];

        for (int i = 0; i < 11; i++) {
            double px = PITCH_X + FORMATION_A[i][0] * PITCH_W;
            double py = PITCH_Y + FORMATION_A[i][1] * PITCH_H;
            playersA[i] = new PlayerSprite(px, py, teamColorA, String.valueOf(i + 1), true);

            px = PITCH_X + FORMATION_B[i][0] * PITCH_W;
            py = PITCH_Y + FORMATION_B[i][1] * PITCH_H;
            playersB[i] = new PlayerSprite(px, py, teamColorB, String.valueOf(i + 1), false);
        }

        resetBall();
    }

    private void resetBall() {
        ballX = PITCH_X + PITCH_W / 2;
        ballY = PITCH_Y + PITCH_H / 2;
        ballVX = (rng.nextBoolean() ? 1 : -1) * (1.5 + rng.nextDouble());
        ballVY = (rng.nextBoolean() ? 1 : -1) * rng.nextDouble() * 1.5;
    }

    // =========================================================================
    //  MATCH SCREEN
    // =========================================================================
    private final Set<String> keysDown = new HashSet<>();

    private void showMatch() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0d1b2a;");

        // TOP HUD
        HBox hud = buildHUD();
        root.setTop(hud);

        // CANVAS
        canvas = new Canvas(W, H - 110);
        gc = canvas.getGraphicsContext2D();
        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setStyle("-fx-background-color: #1b4332;");
        root.setCenter(canvasPane);

        // BOTTOM HUD
        VBox bottom = buildBottom();
        root.setBottom(bottom);

        Scene scene = new Scene(root, W, H);
        scene.setOnKeyPressed(e  -> keysDown.add(e.getCode().toString()));
        scene.setOnKeyReleased(e -> keysDown.remove(e.getCode().toString()));
        primaryStage.setScene(scene);

        startGameLoop();
        startMatchClock();
        if (vsComputer) startComputerAI();
        matchRunning = true;
    }

    // ─── HUD ─────────────────────────────────────────────────────────────────
    private HBox buildHUD() {
        HBox hud = new HBox();
        hud.setAlignment(Pos.CENTER);
        hud.setPadding(new Insets(8, 20, 8, 20));
        hud.setStyle("-fx-background-color: #0d1b2a; -fx-border-color: #40916c; -fx-border-width: 0 0 2 0;");

        // Team A
        VBox aBox = new VBox(2);
        aBox.setAlignment(Pos.CENTER_LEFT);
        Text aName = new Text(teamNameA);
        aName.setFont(Font.font("Arial Black", 16));
        aName.setFill(teamColorA);
        aBox.getChildren().add(aName);

        // Score
        scoreLabel = new Label("0  -  0");
        scoreLabel.setFont(Font.font("Arial Black", FontWeight.BOLD, 32));
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setEffect(new DropShadow(10, Color.web("#40916c")));

        timeLabel = new Label("0'");
        timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        timeLabel.setTextFill(Color.web("#95d5b2"));

        VBox scoreBox = new VBox(2, scoreLabel, timeLabel);
        scoreBox.setAlignment(Pos.CENTER);

        // Team B
        VBox bBox = new VBox(2);
        bBox.setAlignment(Pos.CENTER_RIGHT);
        Text bName = new Text(teamNameB);
        bName.setFont(Font.font("Arial Black", 16));
        bName.setFill(teamColorB);
        bBox.getChildren().add(bName);

        HBox.setHgrow(aBox, Priority.ALWAYS);
        HBox.setHgrow(bBox, Priority.ALWAYS);
        aBox.setMaxWidth(Double.MAX_VALUE);
        bBox.setMaxWidth(Double.MAX_VALUE);

        hud.getChildren().addAll(aBox, scoreBox, bBox);
        return hud;
    }

    private VBox buildBottom() {
        commentaryLabel = new Label("Match is about to start...");
        commentaryLabel.setFont(Font.font("Arial", FontStyle.ITALIC, 14));
        commentaryLabel.setTextFill(Color.web("#95d5b2"));

        Button pauseBtn = styledButton("⏸  PAUSE", "#555", "#333");
        pauseBtn.setOnAction(e -> {
            paused = !paused;
            pauseBtn.setText(paused ? "▶  RESUME" : "⏸  PAUSE");
        });

        Button menuBtn = styledButton("🏠  MENU", "#e76f51", "#9b2226");
        menuBtn.setOnAction(e -> {
            if (gameLoop != null) gameLoop.stop();
            if (matchClock != null) matchClock.stop();
            if (computerAI != null) computerAI.stop();
            showMainMenu();
        });

        HBox btnRow = new HBox(12, pauseBtn, menuBtn);
        btnRow.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(6, commentaryLabel, btnRow);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(8));
        bottom.setStyle("-fx-background-color: #0d1b2a; -fx-border-color: #40916c; -fx-border-width: 2 0 0 0;");
        return bottom;
    }

    // =========================================================================
    //  GAME LOOP
    // =========================================================================
    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            long lastNano = 0;
            public void handle(long now) {
                if (lastNano == 0) { lastNano = now; return; }
                double dt = Math.min((now - lastNano) / 1e9, 0.05);
                lastNano = now;
                if (!paused && matchRunning) {
                    handleInput(dt);
                    updatePhysics(dt);
                    checkGoal();
                    randomCommentary(now);
                }
                render();
            }
        };
        gameLoop.start();
    }

    // ─── Input ────────────────────────────────────────────────────────────────
    private void handleInput(double dt) {
        double spd = 180 * dt;
        PlayerSprite[] aPlayers = { playersA[10], playersA[9] }; // forward, second
        PlayerSprite[] bPlayers = { playersB[10], playersB[9] };

        // Team A – WASD
        for (PlayerSprite p : aPlayers) {
            if (keysDown.contains("W")) p.y -= spd;
            if (keysDown.contains("S")) p.y += spd;
            if (keysDown.contains("A")) p.x -= spd;
            if (keysDown.contains("D")) p.x += spd;
        }
        // Team A – shoot (E or SPACE)
        if (keysDown.contains("E") || keysDown.contains("SPACE")) {
            shootTowards(aPlayers[0], true);
            keysDown.remove("E"); keysDown.remove("SPACE");
        }

        if (!vsComputer) {
            // Team B – Arrow keys
            for (PlayerSprite p : bPlayers) {
                if (keysDown.contains("UP"))    p.y -= spd;
                if (keysDown.contains("DOWN"))  p.y += spd;
                if (keysDown.contains("LEFT"))  p.x -= spd;
                if (keysDown.contains("RIGHT")) p.x += spd;
            }
            // Team B – shoot (/)
            if (keysDown.contains("SLASH")) {
                shootTowards(bPlayers[0], false);
                keysDown.remove("SLASH");
            }
        }

        // Clamp all players to pitch
        for (PlayerSprite p : playersA) clampPlayer(p, true);
        for (PlayerSprite p : playersB) clampPlayer(p, false);
    }

    private void clampPlayer(PlayerSprite p, boolean isA) {
        double margin = PLAYER_R;
        double minX = PITCH_X + margin;
        double maxX = PITCH_X + PITCH_W - margin;
        p.x = Math.max(minX, Math.min(maxX, p.x));
        p.y = Math.max(PITCH_Y + margin, Math.min(PITCH_Y + PITCH_H - margin, p.y));
    }

    private void shootTowards(PlayerSprite p, boolean toRight) {
        double dist = Math.hypot(ballX - p.x, ballY - p.y);
        if (dist < 60) {
            double goalY = PITCH_Y + PITCH_H / 2;
            double goalX = toRight ? PITCH_X + PITCH_W + GOAL_W : PITCH_X - GOAL_W;
            double dx = goalX - ballX, dy = goalY + (rng.nextDouble() - 0.5) * GOAL_H - ballY;
            double len = Math.hypot(dx, dy);
            ballVX = dx / len * (6 + rng.nextDouble() * 3);
            ballVY = dy / len * (6 + rng.nextDouble() * 3);
        }
    }

    // ─── Physics ──────────────────────────────────────────────────────────────
    private void updatePhysics(double dt) {
        double spd = 260 * dt;

        // AI players chase/intercept ball
        moveAIPlayers(spd);

        // Ball movement
        ballX += ballVX * spd / 3.5;
        ballY += ballVY * spd / 3.5;

        // Friction
        ballVX *= 0.992; ballVY *= 0.992;

        // Wall bounce (top/bottom)
        if (ballY - BALL_R < PITCH_Y) { ballY = PITCH_Y + BALL_R; ballVY = Math.abs(ballVY); }
        if (ballY + BALL_R > PITCH_Y + PITCH_H) { ballY = PITCH_Y + PITCH_H - BALL_R; ballVY = -Math.abs(ballVY); }

        // Left wall
        double goalTop = PITCH_Y + (PITCH_H - GOAL_H) / 2;
        double goalBot = goalTop + GOAL_H;
        if (ballX - BALL_R < PITCH_X) {
            if (ballY < goalTop || ballY > goalBot) {
                ballX = PITCH_X + BALL_R; ballVX = Math.abs(ballVX);
            }
        }
        // Right wall
        if (ballX + BALL_R > PITCH_X + PITCH_W) {
            if (ballY < goalTop || ballY > goalBot) {
                ballX = PITCH_X + PITCH_W - BALL_R; ballVX = -Math.abs(ballVX);
            }
        }

        // Player-ball collision
        checkPlayerBallCollision();
    }

    private void moveAIPlayers(double spd) {
        // Defenders track ball
        for (int i = 1; i <= 4; i++) {
            PlayerSprite p = playersA[i];
            double homeX = PITCH_X + FORMATION_A[i][0] * PITCH_W;
            double homeY = PITCH_Y + FORMATION_A[i][1] * PITCH_H;
            double tx = (ballX - homeX > 80) ? ballX : homeX;
            double ty = (ballX - homeX > 80) ? ballY : homeY;
            moveToward(p, tx, ty, spd * 0.55);
        }
        for (int i = 1; i <= 4; i++) {
            PlayerSprite p = playersB[i];
            double homeX = PITCH_X + FORMATION_B[i][0] * PITCH_W;
            double homeY = PITCH_Y + FORMATION_B[i][1] * PITCH_H;
            double tx = (homeX - ballX > 80) ? ballX : homeX;
            double ty = (homeX - ballX > 80) ? ballY : homeY;
            moveToward(p, tx, ty, spd * 0.55);
        }
        // Midfielders
        for (int i = 5; i <= 7; i++) {
            moveToward(playersA[i], ballX, ballY, spd * 0.45);
            moveToward(playersB[i], ballX, ballY, spd * 0.45);
        }
        // GKs
        playersA[0].y += (ballY - playersA[0].y) * 0.018;
        playersB[0].y += (ballY - playersB[0].y) * 0.018;
    }

    private void moveToward(PlayerSprite p, double tx, double ty, double spd) {
        double dx = tx - p.x, dy = ty - p.y;
        double d = Math.hypot(dx, dy);
        if (d > 3) { p.x += dx / d * Math.min(spd, d); p.y += dy / d * Math.min(spd, d); }
    }

    private void checkPlayerBallCollision() {
        PlayerSprite[] all = new PlayerSprite[22];
        System.arraycopy(playersA, 0, all, 0, 11);
        System.arraycopy(playersB, 0, all, 11, 11);

        for (int i = 0; i < all.length; i++) {
            PlayerSprite p = all[i];
            double dx = ballX - p.x, dy = ballY - p.y;
            double dist = Math.hypot(dx, dy);
            if (dist < PLAYER_R + BALL_R) {
                double nx = dx / dist, ny = dy / dist;
                ballX = p.x + nx * (PLAYER_R + BALL_R);
                ballY = p.y + ny * (PLAYER_R + BALL_R);
                ballVX = nx * 4 + (rng.nextDouble() - 0.5) * 1.5;
                ballVY = ny * 4 + (rng.nextDouble() - 0.5) * 1.5;
                p.flash();
            }
        }
    }

    // ─── Goal Check ───────────────────────────────────────────────────────────
    private void checkGoal() {
        double goalTop = PITCH_Y + (PITCH_H - GOAL_H) / 2;
        double goalBot = goalTop + GOAL_H;

        // Left goal → Team B scores
        if (ballX < PITCH_X - GOAL_W && ballY > goalTop && ballY < goalBot) {
            scoreB++;
            updateScore();
            triggerGoalEffect(false);
            resetBall();
            repositionPlayers();
        }
        // Right goal → Team A scores
        if (ballX > PITCH_X + PITCH_W + GOAL_W && ballY > goalTop && ballY < goalBot) {
            scoreA++;
            updateScore();
            triggerGoalEffect(true);
            resetBall();
            repositionPlayers();
        }
    }

    private void repositionPlayers() {
        for (int i = 0; i < 11; i++) {
            playersA[i].x = PITCH_X + FORMATION_A[i][0] * PITCH_W;
            playersA[i].y = PITCH_Y + FORMATION_A[i][1] * PITCH_H;
            playersB[i].x = PITCH_X + FORMATION_B[i][0] * PITCH_W;
            playersB[i].y = PITCH_Y + FORMATION_B[i][1] * PITCH_H;
        }
    }

    private void updateScore() {
        Platform.runLater(() -> scoreLabel.setText(scoreA + "  -  " + scoreB));
    }

    private void triggerGoalEffect(boolean teamA) {
        String scorer = teamA ? teamNameA : teamNameB;
        String text = GOAL_COMMENTARY[rng.nextInt(GOAL_COMMENTARY.length)] + "  " + scorer + " scores!";
        Platform.runLater(() -> {
            commentaryLabel.setText(text);
            commentaryLabel.setTextFill(Color.web("#f4a261"));
            // Flash score
            ScaleTransition st = new ScaleTransition(Duration.millis(200), scoreLabel);
            st.setToX(1.5); st.setToY(1.5); st.setAutoReverse(true); st.setCycleCount(4); st.play();
        });

        // Show goal overlay on canvas
        showGoalOverlay(scorer);
    }

    private void showGoalOverlay(String team) {
        Platform.runLater(() -> {
            // Draw GOAL! text directly on canvas via one-shot
            gc.setFill(Color.web("#f4a26180"));
            gc.fillRoundRect(W / 2.0 - 150, H / 2.0 - 60, 300, 100, 20, 20);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial Black", 52));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("⚽ GOAL!", W / 2.0, H / 2.0 - 160);
            gc.setFont(Font.font("Arial", 22));
            gc.fillText(team, W / 2.0, H / 2.0 - 120);

            // Fade after 2s
            new Timeline(new KeyFrame(Duration.seconds(2), ev -> { /* next render clears it */ })).play();
        });
    }

    // ─── Match Clock ─────────────────────────────────────────────────────────
    private void startMatchClock() {
        matchClock = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> {
            if (!paused && matchRunning) {
                matchTime++;
                Platform.runLater(() -> timeLabel.setText(matchTime + "'"));
                if (matchTime >= 90) {
                    endMatch();
                }
                // Halftime
                if (matchTime == 45) {
                    Platform.runLater(() -> commentaryLabel.setText("⏱  HALF TIME!  " + teamNameA + " " + scoreA + " - " + scoreB + " " + teamNameB));
                }
            }
        }));
        matchClock.setCycleCount(Animation.INDEFINITE);
        matchClock.play();
    }

    // ─── Computer AI ─────────────────────────────────────────────────────────
    private void startComputerAI() {
        computerAI = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            if (paused || !matchRunning) return;
            PlayerSprite forward = playersB[10];
            double dist = Math.hypot(ballX - forward.x, ballY - forward.y);
            // Chase ball
            double dx = ballX - forward.x, dy = ballY - forward.y;
            double d = Math.hypot(dx, dy);
            if (d > 5) { forward.x += dx / d * 2.8; forward.y += dy / d * 2.8; }
            // Shoot when close
            if (dist < 55 && rng.nextInt(8) == 0) {
                shootTowards(forward, false);
            }
            clampPlayer(forward, false);
            // Move attacker 2 as well
            PlayerSprite a2 = playersB[9];
            double dx2 = ballX - a2.x - 30, dy2 = ballY - a2.y;
            double d2 = Math.hypot(dx2, dy2);
            if (d2 > 5) { a2.x += dx2 / d2 * 2.2; a2.y += dy2 / d2 * 2.2; }
            clampPlayer(a2, false);
        }));
        computerAI.setCycleCount(Animation.INDEFINITE);
        computerAI.play();
    }

    // ─── Commentary ───────────────────────────────────────────────────────────
    private void randomCommentary(long now) {
        if (now - lastCommentaryTime > 4_000_000_000L) {
            lastCommentaryTime = now;
            String c = COMMENTARY[rng.nextInt(COMMENTARY.length)];
            Platform.runLater(() -> {
                commentaryLabel.setText(c);
                commentaryLabel.setTextFill(Color.web("#95d5b2"));
            });
        }
    }

    // ─── End Match ───────────────────────────────────────────────────────────
    private void endMatch() {
        matchRunning = false;
        if (gameLoop != null) gameLoop.stop();
        if (matchClock != null) matchClock.stop();
        if (computerAI != null) computerAI.stop();

        if (scoreA > scoreB) winner = teamNameA + " WIN!";
        else if (scoreB > scoreA) winner = teamNameB + " WIN!";
        else winner = "DRAW!";

        Platform.runLater(this::showResultScreen);
    }

    // =========================================================================
    //  RESULT SCREEN
    // =========================================================================
    private void showResultScreen() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0d1b2a, #1b4332, #0d1b2a);");

        VBox box = new VBox(30);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));

        Text ft = new Text("FULL TIME");
        ft.setFont(Font.font("Arial Black", 28));
        ft.setFill(Color.web("#95d5b2"));

        Text score = new Text(teamNameA + "  " + scoreA + "  -  " + scoreB + "  " + teamNameB);
        score.setFont(Font.font("Arial Black", FontWeight.BOLD, 36));
        score.setFill(Color.WHITE);
        score.setEffect(new DropShadow(20, Color.web("#40916c")));

        Text win = new Text(winner);
        win.setFont(Font.font("Arial Black", FontWeight.BOLD, 50));
        win.setFill(winner.contains("DRAW") ? Color.web("#95d5b2") : Color.web("#f4a261"));
        win.setEffect(new Glow(0.8));

        // Trophy animation
        Text trophy = new Text("🏆");
        trophy.setFont(Font.font(80));
        ScaleTransition sc = new ScaleTransition(Duration.seconds(1.2), trophy);
        sc.setFromX(0.5); sc.setToX(1.2); sc.setFromY(0.5); sc.setToY(1.2);
        sc.setAutoReverse(true); sc.setCycleCount(Animation.INDEFINITE); sc.play();

        Button playAgain = styledButton("▶  PLAY AGAIN", "#40916c", "#1b4332");
        Button menu = styledButton("🏠  MAIN MENU", "#e76f51", "#9b2226");

        playAgain.setOnAction(e -> { initMatch(); showMatch(); });
        menu.setOnAction(e -> showMainMenu());

        HBox btns = new HBox(20, playAgain, menu);
        btns.setAlignment(Pos.CENTER);

        if (!winner.contains("DRAW")) box.getChildren().addAll(ft, score, win, trophy, btns);
        else box.getChildren().addAll(ft, score, win, btns);

        root.getChildren().add(box);
        primaryStage.setScene(new Scene(root, W, H));

        // Confetti animation
        if (!winner.contains("DRAW")) launchConfetti(root);
    }

    private void launchConfetti(StackPane root) {
        Canvas cc = new Canvas(W, H);
        root.getChildren().add(cc);
        cc.setMouseTransparent(true);
        GraphicsContext cg = cc.getGraphicsContext2D();

        double[][] cx = new double[60][3]; // x, y, vy
        Color[] confetti = {Color.GOLD, Color.DEEPSKYBLUE, Color.TOMATO, Color.LIMEGREEN, Color.VIOLET};
        for (double[] p : cx) {
            p[0] = rng.nextDouble() * W;
            p[1] = rng.nextDouble() * H / 2 - H / 2;
            p[2] = 2 + rng.nextDouble() * 4;
        }
        new AnimationTimer() {
            public void handle(long now) {
                cg.clearRect(0, 0, W, H);
                for (int i = 0; i < cx.length; i++) {
                    cx[i][1] += cx[i][2];
                    if (cx[i][1] > H) { cx[i][1] = -10; cx[i][0] = rng.nextDouble() * W; }
                    cg.setFill(confetti[i % confetti.length]);
                    cg.fillRect(cx[i][0], cx[i][1], 8, 8);
                }
            }
        }.start();
    }

    // =========================================================================
    //  RENDERING
    // =========================================================================
    private double ballAngle = 0;

    private void render() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        drawPitch();
        drawGoals();
        drawPlayers();
        drawBall();
        drawControls();

        if (!matchRunning && winner != null) {
            drawEndOverlay();
        }
        if (paused) {
            drawPauseOverlay();
        }

        ballAngle += Math.sqrt(ballVX * ballVX + ballVY * ballVY) * 2;
    }

    // ─── Pitch ───────────────────────────────────────────────────────────────
    private void drawPitch() {
        // Grass stripes
        for (int i = 0; i < 8; i++) {
            double sw = PITCH_W / 8.0;
            gc.setFill(i % 2 == 0 ? Color.web("#2d6a4f") : Color.web("#40916c"));
            gc.fillRect(PITCH_X + i * sw, PITCH_Y, sw, PITCH_H);
        }

        // Boundary
        gc.setStroke(Color.web("#b7e4c7")); gc.setLineWidth(3);
        gc.strokeRect(PITCH_X, PITCH_Y, PITCH_W, PITCH_H);

        // Centre line
        gc.setLineWidth(2);
        gc.strokeLine(PITCH_X + PITCH_W / 2, PITCH_Y, PITCH_X + PITCH_W / 2, PITCH_Y + PITCH_H);

        // Centre circle
        gc.strokeOval(PITCH_X + PITCH_W / 2 - 60, PITCH_Y + PITCH_H / 2 - 60, 120, 120);
        gc.setFill(Color.web("#b7e4c7"));
        gc.fillOval(PITCH_X + PITCH_W / 2 - 4, PITCH_Y + PITCH_H / 2 - 4, 8, 8);

        // Penalty boxes
        double pbW = PITCH_W * 0.16, pbH = PITCH_H * 0.55;
        double pbY = PITCH_Y + (PITCH_H - pbH) / 2;
        gc.setLineWidth(2);
        gc.strokeRect(PITCH_X, pbY, pbW, pbH);
        gc.strokeRect(PITCH_X + PITCH_W - pbW, pbY, pbW, pbH);

        // Small boxes
        double sbW = PITCH_W * 0.07, sbH = PITCH_H * 0.30;
        double sbY = PITCH_Y + (PITCH_H - sbH) / 2;
        gc.strokeRect(PITCH_X, sbY, sbW, sbH);
        gc.strokeRect(PITCH_X + PITCH_W - sbW, sbY, sbW, sbH);

        // Penalty spots
        gc.setFill(Color.web("#b7e4c7"));
        gc.fillOval(PITCH_X + pbW - 8, PITCH_Y + PITCH_H / 2 - 4, 8, 8);
        gc.fillOval(PITCH_X + PITCH_W - pbW, PITCH_Y + PITCH_H / 2 - 4, 8, 8);

        // Corner arcs
        double ca = 12;
        gc.setStroke(Color.web("#b7e4c7")); gc.setLineWidth(2);
        gc.strokeArc(PITCH_X - ca, PITCH_Y - ca, ca * 2, ca * 2, 270, 90, Arc.ArcType.OPEN);
        gc.strokeArc(PITCH_X + PITCH_W - ca, PITCH_Y - ca, ca * 2, ca * 2, 180, 90, Arc.ArcType.OPEN);
        gc.strokeArc(PITCH_X - ca, PITCH_Y + PITCH_H - ca, ca * 2, ca * 2, 0, 90, Arc.ArcType.OPEN);
        gc.strokeArc(PITCH_X + PITCH_W - ca, PITCH_Y + PITCH_H - ca, ca * 2, ca * 2, 90, 90, Arc.ArcType.OPEN);
    }

    // ─── Goals ────────────────────────────────────────────────────────────────
    private void drawGoals() {
        double goalTop = PITCH_Y + (PITCH_H - GOAL_H) / 2;
        // Left goal (Team B scores here)
        gc.setFill(Color.web("#ffffff30"));
        gc.fillRect(PITCH_X - GOAL_W, goalTop, GOAL_W, GOAL_H);
        gc.setStroke(Color.WHITE); gc.setLineWidth(3);
        gc.strokeRect(PITCH_X - GOAL_W, goalTop, GOAL_W, GOAL_H);
        // Net pattern
        gc.setStroke(Color.web("#ffffff50")); gc.setLineWidth(1);
        for (int i = 1; i < 5; i++) {
            gc.strokeLine(PITCH_X - GOAL_W, goalTop + i * GOAL_H / 5, PITCH_X, goalTop + i * GOAL_H / 5);
        }

        // Right goal (Team A scores here)
        gc.setFill(Color.web("#ffffff30"));
        gc.fillRect(PITCH_X + PITCH_W, goalTop, GOAL_W, GOAL_H);
        gc.setStroke(Color.WHITE); gc.setLineWidth(3);
        gc.strokeRect(PITCH_X + PITCH_W, goalTop, GOAL_W, GOAL_H);
        gc.setStroke(Color.web("#ffffff50")); gc.setLineWidth(1);
        for (int i = 1; i < 5; i++) {
            gc.strokeLine(PITCH_X + PITCH_W, goalTop + i * GOAL_H / 5, PITCH_X + PITCH_W + GOAL_W, goalTop + i * GOAL_H / 5);
        }
    }

    // ─── Players ─────────────────────────────────────────────────────────────
    private void drawPlayers() {
        for (PlayerSprite p : playersA) drawPlayer(p);
        for (PlayerSprite p : playersB) drawPlayer(p);
    }

    private void drawPlayer(PlayerSprite p) {
        // Shadow
        gc.setFill(Color.web("#00000040"));
        gc.fillOval(p.x - PLAYER_R + 3, p.y - PLAYER_R + 4, PLAYER_R * 2, PLAYER_R * 2);

        // Body
        Color c = p.flashing ? Color.WHITE : p.color;
        gc.setFill(c);
        gc.fillOval(p.x - PLAYER_R, p.y - PLAYER_R, PLAYER_R * 2, PLAYER_R * 2);

        // Outline
        gc.setStroke(Color.web("#00000088")); gc.setLineWidth(2);
        gc.strokeOval(p.x - PLAYER_R, p.y - PLAYER_R, PLAYER_R * 2, PLAYER_R * 2);

        // Number
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(p.number, p.x, p.y + 4);

        p.updateFlash();
    }

    // ─── Ball ────────────────────────────────────────────────────────────────
    private void drawBall() {
        // Shadow
        gc.setFill(Color.web("#00000040"));
        gc.fillOval(ballX - BALL_R + 3, ballY - BALL_R + 4, BALL_R * 2, BALL_R * 2);

        // Ball
        gc.save();
        gc.translate(ballX, ballY);
        gc.rotate(Math.toDegrees(ballAngle));
        gc.setFill(Color.WHITE);
        gc.fillOval(-BALL_R, -BALL_R, BALL_R * 2, BALL_R * 2);

        // Pentagon pattern
        gc.setFill(Color.web("#1a1a1a"));
        double[] px = new double[5], py = new double[5];
        for (int i = 0; i < 5; i++) {
            px[i] = Math.cos(Math.PI / 2 + i * 2 * Math.PI / 5) * BALL_R * 0.45;
            py[i] = Math.sin(Math.PI / 2 + i * 2 * Math.PI / 5) * BALL_R * 0.45;
        }
        gc.fillPolygon(px, py, 5);
        gc.restore();

        // Glow when moving fast
        double speed = Math.hypot(ballVX, ballVY);
        if (speed > 4) {
            gc.setFill(Color.web("#ffffff20"));
            gc.fillOval(ballX - BALL_R - 5, ballY - BALL_R - 5, (BALL_R + 5) * 2, (BALL_R + 5) * 2);
        }
    }

    // ─── Controls help ────────────────────────────────────────────────────────
    private void drawControls() {
        gc.setFill(Color.web("#ffffff40"));
        gc.setFont(Font.font("Arial", 11));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Team A: WASD + E (shoot)", PITCH_X + 5, PITCH_Y + PITCH_H + 20);
        gc.setTextAlign(TextAlignment.RIGHT);
        String ctrlB = vsComputer ? "Team B: COMPUTER AI" : "Team B: ↑↓←→ + / (shoot)";
        gc.fillText(ctrlB, PITCH_X + PITCH_W - 5, PITCH_Y + PITCH_H + 20);
    }

    private void drawPauseOverlay() {
        gc.setFill(Color.web("#00000080"));
        gc.fillRoundRect(W / 2.0 - 120, canvas.getHeight() / 2.0 - 50, 240, 90, 20, 20);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial Black", 40));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("PAUSED", W / 2.0, canvas.getHeight() / 2.0 + 12);
    }

    private void drawEndOverlay() {
        gc.setFill(Color.web("#00000080"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial Black", 50));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("FULL TIME", W / 2.0, canvas.getHeight() / 2.0);
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================
    private Button styledButton(String text, String bg, String border) {
        Button b = new Button(text);
        b.setFont(Font.font("Arial Black", 15));
        b.setTextFill(Color.WHITE);
        b.setStyle(String.format(
            "-fx-background-color: %s; -fx-background-radius: 30; -fx-padding: 12 28; " +
            "-fx-border-color: %s; -fx-border-radius: 30; -fx-border-width: 2; -fx-cursor: hand;",
            bg, border));
        b.setOnMouseEntered(e -> b.setOpacity(0.85));
        b.setOnMouseExited(e -> b.setOpacity(1.0));
        return b;
    }

    // ─── Player Sprite ────────────────────────────────────────────────────────
    static class PlayerSprite {
        double x, y;
        Color color;
        String number;
        boolean teamA;
        boolean flashing = false;
        int flashTicks = 0;

        PlayerSprite(double x, double y, Color color, String number, boolean teamA) {
            this.x = x; this.y = y; this.color = color;
            this.number = number; this.teamA = teamA;
        }
        void flash() { flashing = true; flashTicks = 6; }
        void updateFlash() {
            if (flashTicks > 0) { flashTicks--; }
            else flashing = false;
        }
    }

    // ─── Main ─────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        launch(args);
    }
}
