import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import java.util.*;

public class HoverboardRacingAdventure extends Application {

    // Game Constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final double TRACK_LEFT = 100;
    private static final double TRACK_RIGHT = 700;
    private static final double FINISH_LINE = 5000;

    // Game State
    private enum GameState { MENU, COUNTDOWN, PLAYING, GAME_OVER }
    private GameState currentState = GameState.MENU;
    private boolean isVsAI = true;

    // Entities
    private Racer player1, player2;
    private List<Obstacle> obstacles = new ArrayList<>();
    private List<BoostPad> boosts = new ArrayList<>();
    private double cameraY = 0;
    private double distanceToNextObstacle = 300;
    private double countdownTime = 3.0;
    private String winnerText = "";

    // Input Handling
    private Set<KeyCode> keysPressed = new HashSet<>();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hoverboard Racing Adventure");
        primaryStage.setResizable(false);
        showMenu(primaryStage);
        primaryStage.show();
    }

    private void showMenu(Stage stage) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 60%, #1a0b2e, #000000);");

        Text title = new Text("HOVERBOARD RACING");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 48));
        title.setFill(Color.CYAN);
        title.setEffect(new DropShadow(20, Color.CYAN));

        Text subtitle = new Text("ADVENTURE");
        subtitle.setFont(Font.font("Monospace", FontWeight.BOLD, 36));
        subtitle.setFill(Color.MAGENTA);
        subtitle.setEffect(new DropShadow(20, Color.MAGENTA));

        javafx.scene.control.Button btnAI = createNeonButton("1 PLAYER (VS COMPUTER)", Color.CYAN);
        javafx.scene.control.Button btnPvP = createNeonButton("2 PLAYERS (LOCAL)", Color.MAGENTA);

        btnAI.setOnAction(e -> { isVsAI = true; startGame(stage); });
        btnPvP.setOnAction(e -> { isVsAI = false; startGame(stage); });

        root.getChildren().addAll(title, subtitle, new Text("\n"), btnAI, btnPvP);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        stage.setScene(scene);
    }

    private javafx.scene.control.Button createNeonButton(String text, Color color) {
        javafx.scene.control.Button btn = new javafx.scene.control.Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + toHexString(color) + "; -fx-font-size: 20px; -fx-font-family: 'Monospace'; -fx-border-color: " + toHexString(color) + "; -fx-border-width: 2px; -fx-padding: 10px 20px;");
        btn.setEffect(new DropShadow(10, color));
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + toHexString(color) + "33; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-family: 'Monospace'; -fx-border-color: white; -fx-border-width: 2px; -fx-padding: 10px 20px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + toHexString(color) + "; -fx-font-size: 20px; -fx-font-family: 'Monospace'; -fx-border-color: " + toHexString(color) + "; -fx-border-width: 2px; -fx-padding: 10px 20px;"));
        return btn;
    }

    private void startGame(Stage stage) {
        StackPane root = new StackPane();
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        root.getChildren().add(canvas);

        Scene scene = new Scene(root, WIDTH, HEIGHT, Color.BLACK);
        scene.setOnKeyPressed(e -> keysPressed.add(e.getCode()));
        scene.setOnKeyReleased(e -> keysPressed.remove(e.getCode()));

        stage.setScene(scene);
        initGame();

        AnimationTimer gameLoop = new AnimationTimer() {
            private long lastTime = System.nanoTime();
            @Override
            public void handle(long now) {
                double deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                update(deltaTime);
                render(canvas.getGraphicsContext2D());
            }
        };
        gameLoop.start();
    }

    private void initGame() {
        currentState = GameState.COUNTDOWN;
        countdownTime = 3.0;
        cameraY = 0;
        obstacles.clear();
        boosts.clear();
        winnerText = "";

        // Player 1 (Cyan) - Left side
        player1 = new Racer(TRACK_LEFT + 100, HEIGHT - 100, Color.CYAN, Color.BLUE);
        // Player 2 / AI (Magenta) - Right side
        player2 = new Racer(TRACK_RIGHT - 100, HEIGHT - 100, Color.MAGENTA, Color.PURPLE);
    }

    private void update(double deltaTime) {
        if (currentState == GameState.COUNTDOWN) {
            countdownTime -= deltaTime;
            if (countdownTime <= 0) currentState = GameState.PLAYING;
            return;
        }

        if (currentState != GameState.PLAYING) return;

        // Handle Input & AI
        handlePlayerInput(player1, KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D);

        if (isVsAI) {
            updateAI(player2, deltaTime);
        } else {
            handlePlayerInput(player2, KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT);
        }

        // Update Racers
        player1.update(deltaTime);
        player2.update(deltaTime);

        // Camera follows the leader (average of both)
        double leaderY = Math.min(player1.distanceTraveled, player2.distanceTraveled);
        cameraY = leaderY - HEIGHT / 2;

        // Spawn Obstacles and Boosts
        distanceToNextObstacle -= Math.max(player1.speed, player2.speed) * deltaTime;
        if (distanceToNextObstacle <= 0) {
            spawnTrackFeatures();
            distanceToNextObstacle = 200 + Math.random() * 300;
        }

        // Move and check collisions for Obstacles
        Iterator<Obstacle> obsIt = obstacles.iterator();
        while (obsIt.hasNext()) {
            Obstacle obs = obsIt.next();
            obs.update(deltaTime, player1.speed, player2.speed); // Moves relative to camera
            if (obs.screenY > HEIGHT + 100) obsIt.remove();

            checkCollision(player1, obs);
            checkCollision(player2, obs);
        }

        // Move and check collisions for Boosts
        Iterator<BoostPad> boostIt = boosts.iterator();
        while (boostIt.hasNext()) {
            BoostPad boost = boostIt.next();
            boost.update(deltaTime, player1.speed, player2.speed);
            if (boost.screenY > HEIGHT + 100) boostIt.remove();

            if (checkBoostCollision(player1, boost)) boostIt.remove();
            if (checkBoostCollision(player2, boost)) boostIt.remove();
        }

        // Check Win Condition
        if (player1.distanceTraveled >= FINISH_LINE || player2.distanceTraveled >= FINISH_LINE) {
            currentState = GameState.GAME_OVER;
            if (player1.distanceTraveled > player2.distanceTraveled) {
                winnerText = isVsAI ? "PLAYER WINS!" : "PLAYER 1 WINS!";
            } else {
                winnerText = isVsAI ? "COMPUTER WINS!" : "PLAYER 2 WINS!";
            }
        }
    }

    private void handlePlayerInput(Racer racer, KeyCode up, KeyCode down, KeyCode left, KeyCode right) {
        if (keysPressed.contains(up)) racer.accelerate();
        if (keysPressed.contains(down)) racer.brake();
        if (keysPressed.contains(left)) racer.steer(-1);
        if (keysPressed.contains(right)) racer.steer(1);
    }

    private void updateAI(Racer ai, double deltaTime) {
        ai.accelerate(); // AI always accelerates

        // Look ahead for obstacles
        double lookAheadDist = 250;
        Obstacle threat = null;
        double minDist = Double.MAX_VALUE;

        for (Obstacle obs : obstacles) {
            double relY = obs.distance - ai.distanceTraveled;
            if (relY > 0 && relY < lookAheadDist && relY < minDist) {
                // Check if it's in our path
                if (Math.abs(obs.x - ai.x) < 80) {
                    threat = obs;
                    minDist = relY;
                }
            }
        }

        if (threat != null) {
            // Dodge left or right
            if (threat.x < ai.x) ai.steer(1);
            else ai.steer(-1);
        } else {
            // Stay near center of track
            double trackCenter = (TRACK_LEFT + TRACK_RIGHT) / 2;
            if (ai.x < trackCenter - 50) ai.steer(1);
            else if (ai.x > trackCenter + 50) ai.steer(-1);
        }

        // Look for boosts
        for (BoostPad boost : boosts) {
            double relY = boost.distance - ai.distanceTraveled;
            if (relY > 0 && relY < 300 && Math.abs(boost.x - ai.x) < 100) {
                // Steer towards boost
                if (boost.x < ai.x) ai.steer(-0.5);
                else ai.steer(0.5);
            }
        }
    }

    private void spawnTrackFeatures() {
        double spawnDist = cameraY + HEIGHT + 200;
        Random rand = new Random();

        // Spawn Obstacle
        double obsX = TRACK_LEFT + 50 + rand.nextDouble() * (TRACK_RIGHT - TRACK_LEFT - 100);
        obstacles.add(new Obstacle(obsX, spawnDist, 60, 40));

        // Sometimes spawn a boost
        if (rand.nextBoolean()) {
            double boostX = TRACK_LEFT + 50 + rand.nextDouble() * (TRACK_RIGHT - TRACK_LEFT - 100);
            boosts.add(new BoostPad(boostX, spawnDist + 100));
        }
    }

    private void checkCollision(Racer racer, Obstacle obs) {
        double relY = obs.distance - racer.distanceTraveled;
        if (Math.abs(relY) < 40 && Math.abs(obs.x - racer.x) < 50) {
            racer.crash();
            obs.destroyed = true; // Simple way to avoid multi-hit
        }
    }

    private boolean checkBoostCollision(Racer racer, BoostPad boost) {
        double relY = boost.distance - racer.distanceTraveled;
        if (Math.abs(relY) < 30 && Math.abs(boost.x - racer.x) < 40) {
            racer.boost();
            return true;
        }
        return false;
    }

    private void render(GraphicsContext gc) {
        // Clear screen with dark gradient
        gc.setFill(new LinearGradient(0, 0, 0, HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0f051d")), new Stop(1, Color.web("#000000"))));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw moving grid for speed effect
        drawGrid(gc);

        // Draw Track Boundaries
        gc.setStroke(Color.NEONPINK); // Fallback if NEONPINK isn't defined, using Color.web
        gc.setStroke(Color.web("#ff00ff"));
        gc.setLineWidth(4);
        gc.setEffect(new DropShadow(15, Color.MAGENTA));
        gc.strokeLine(TRACK_LEFT, 0, TRACK_LEFT, HEIGHT);
        gc.strokeLine(TRACK_RIGHT, 0, TRACK_RIGHT, HEIGHT);
        gc.setEffect(null);

        if (currentState == GameState.COUNTDOWN) {
            drawCountdown(gc);
            return;
        }

        // Draw Boosts
        for (BoostPad b : boosts) drawBoost(gc, b);

        // Draw Obstacles
        for (Obstacle o : obstacles) drawObstacle(gc, o);

        // Draw Racers
        drawRacer(gc, player1);
        drawRacer(gc, player2);

        // Draw HUD
        drawHUD(gc);

        if (currentState == GameState.GAME_OVER) {
            drawGameOver(gc);
        }
    }

    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.web("#2a1045"));
        gc.setLineWidth(1);
        double gridSpacing = 50;
        double offset = (cameraY % gridSpacing);

        for (double y = -offset; y < HEIGHT; y += gridSpacing) {
            gc.strokeLine(TRACK_LEFT, y, TRACK_RIGHT, y);
        }
        for (double x = TRACK_LEFT; x <= TRACK_RIGHT; x += gridSpacing) {
            gc.strokeLine(x, 0, x, HEIGHT);
        }
    }

    private void drawRacer(GraphicsContext gc, Racer racer) {
        double screenY = HEIGHT - 100; // Fixed screen Y for simplicity

        // Draw Trail
        for (int i = 0; i < racer.trail.size(); i++) {
            TrailPoint p = racer.trail.get(i);
            double alpha = (double) i / racer.trail.size();
            gc.setFill(new RadialGradient(0, 0, p.x, screenY + 20, 15, false, CycleMethod.NO_CYCLE,
                    new Stop(0, racer.color.deriveColor(0, 1, 1, alpha * 0.6)),
                    new Stop(1, racer.color.deriveColor(0, 1, 1, 0))));
            gc.fillOval(p.x - 15, screenY + 5, 30, 30);
        }

        // Draw Hoverboard Glow
        gc.setFill(new RadialGradient(0, 0, racer.x, screenY, 50, false, CycleMethod.NO_CYCLE,
                new Stop(0, racer.color.deriveColor(0, 1, 1, 0.5)),
                new Stop(1, racer.color.deriveColor(0, 1, 1, 0))));
        gc.fillOval(racer.x - 50, screenY - 50, 100, 100);

        // Draw Board
        gc.save();
        gc.translate(racer.x, screenY);
        gc.rotate(racer.angle);

        gc.setFill(racer.glowColor);
        gc.fillRoundRect(-30, -15, 60, 30, 20, 20);

        gc.setFill(racer.color);
        gc.fillRoundRect(-25, -10, 50, 20, 15, 15);

        // Rider (simple circle)
        gc.setFill(Color.WHITE);
        gc.fillOval(-8, -20, 16, 16);

        gc.restore();
    }

    private void drawObstacle(GraphicsContext gc, Obstacle obs) {
        double screenY = HEIGHT - (obs.distance - cameraY);
        if (screenY < -50 || screenY > HEIGHT + 50) return;

        gc.setFill(new LinearGradient(obs.x - 30, screenY - 20, obs.x + 30, screenY + 20, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED), new Stop(1, Color.DARKRED)));
        gc.setEffect(new DropShadow(15, Color.RED));
        gc.fillRect(obs.x - 30, screenY - 20, 60, 40);
        gc.setStroke(Color.ORANGE);
        gc.setLineWidth(2);
        gc.strokeRect(obs.x - 30, screenY - 20, 60, 40);
        gc.setEffect(null);
    }

    private void drawBoost(GraphicsContext gc, BoostPad boost) {
        double screenY = HEIGHT - (boost.distance - cameraY);
        if (screenY < -50 || screenY > HEIGHT + 50) return;

        double pulse = Math.sin(System.currentTimeMillis() / 200.0) * 5 + 25;
        gc.setFill(new RadialGradient(0, 0, boost.x, screenY, pulse, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.YELLOW), new Stop(0.5, Color.LIME), new Stop(1, Color.TRANSPARENT)));
        gc.fillOval(boost.x - pulse, screenY - pulse, pulse * 2, pulse * 2);
    }

    private void drawHUD(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Monospace", 20));
        gc.fillText("P1 Dist: " + (int)player1.distanceTraveled + "m / " + (int)FINISH_LINE + "m", 20, 30);
        gc.fillText("P2 Dist: " + (int)player2.distanceTraveled + "m / " + (int)FINISH_LINE + "m", 20, 60);

        // Progress bars
        gc.setFill(Color.CYAN);
        gc.fillRect(20, 40, (player1.distanceTraveled / FINISH_LINE) * 200, 10);
        gc.setFill(Color.MAGENTA);
        gc.fillRect(20, 70, (player2.distanceTraveled / FINISH_LINE) * 200, 10);
    }

    private void drawCountdown(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 100));
        String text = countdownTime > 0 ? String.format("%.0f", Math.ceil(countdownTime)) : "GO!";
        gc.setEffect(new DropShadow(20, Color.CYAN));
        gc.fillText(text, WIDTH / 2 - 50, HEIGHT / 2);
        gc.setEffect(null);
    }

    private void drawGameOver(GraphicsContext gc) {
        gc.setFill(new Color(0, 0, 0, 0.7));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 60));
        gc.setEffect(new DropShadow(20, Color.GOLD));
        gc.fillText(winnerText, WIDTH / 2 - 200, HEIGHT / 2);
        gc.setEffect(null);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Monospace", 24));
        gc.fillText("Press SPACE to return to menu", WIDTH / 2 - 180, HEIGHT / 2 + 60);

        if (keysPressed.contains(KeyCode.SPACE)) {
            keysPressed.clear();
            showMenu((Stage) gc.getCanvas().getScene().getWindow());
        }
    }

    private String toHexString(Color c) {
        return String.format("#%02X%02X%02X", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }

    // --- Inner Classes for Game Entities ---

    class Racer {
        double x, distanceTraveled, speed, angle;
        Color color, glowColor;
        List<TrailPoint> trail = new ArrayList<>();
        double crashTimer = 0;

        public Racer(double x, double y, Color color, Color glowColor) {
            this.x = x;
            this.distanceTraveled = 0;
            this.speed = 0;
            this.angle = 0;
            this.color = color;
            this.glowColor = glowColor;
        }

        public void update(double deltaTime) {
            if (crashTimer > 0) {
                crashTimer -= deltaTime;
                speed *= 0.95; // Slow down during crash
            } else {
                speed *= 0.98; // Natural deceleration
                if (speed < 0) speed = 0;
            }

            distanceTraveled += speed * deltaTime;

            // Keep on track
            if (x < TRACK_LEFT + 30) { x = TRACK_LEFT + 30; speed *= 0.8; }
            if (x > TRACK_RIGHT - 30) { x = TRACK_RIGHT - 30; speed *= 0.8; }

            // Update trail
            trail.add(new TrailPoint(x));
            if (trail.size() > 20) trail.remove(0);
        }

        public void accelerate() {
            if (crashTimer <= 0) {
                speed += 300 * 0.016; // Approx 60fps delta
                if (speed > 600) speed = 600;
            }
        }

        public void brake() {
            speed -= 400 * 0.016;
            if (speed < 0) speed = 0;
        }

        public void steer(double dir) {
            x += dir * 300 * 0.016;
            angle = dir * 15; // Visual tilt
        }

        public void crash() {
            if (crashTimer <= 0) {
                crashTimer = 1.0;
                speed *= 0.3;
            }
        }

        public void boost() {
            speed += 200;
            if (speed > 800) speed = 800;
        }
    }

    class Obstacle {
        double x, distance;
        double width, height;
        boolean destroyed = false;

        public Obstacle(double x, double distance, double width, double height) {
            this.x = x;
            this.distance = distance;
            this.width = width;
            this.height = height;
        }

        public void update(double deltaTime, double p1Speed, double p2Speed) {
            // Obstacles are static in the world, their screen position is calculated in render
        }
    }

    class BoostPad {
        double x, distance;

        public BoostPad(double x, double distance) {
            this.x = x;
            this.distance = distance;
        }

        public void update(double deltaTime, double p1Speed, double p2Speed) {
            // Static in world
        }
    }

    class TrailPoint {
        double x;
        public TrailPoint(double x) { this.x = x; }
    }

    public static void main(String[] args) {
        launch(args);
    }
}