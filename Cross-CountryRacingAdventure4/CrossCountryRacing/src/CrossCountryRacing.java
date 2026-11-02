import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class CrossCountryRacing extends Application {

    // Game Constants
    private static final double WIDTH = 1000;
    private static final double HEIGHT = 700;
    private static final double TRACK_LENGTH = 8000;
    private static final double ROAD_WIDTH = 180;

    // Game State
    private enum GameState { MENU, RACING, FINISHED }
    private GameState state = GameState.MENU;
    private boolean isVsAI = true;

    // Entities
    private Car player1, player2;
    private ArrayList<Particle> particles = new ArrayList<>();
    private ArrayList<Tree> trees = new ArrayList<>();
    private Set<KeyCode> keysPressed = new HashSet<>();
    private Random rand = new Random();

    // Camera
    private double cameraY = 0;

    @Override
    public void start(Stage primaryStage) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, Color.BLACK);

        scene.setOnKeyPressed(e -> keysPressed.add(e.getCode()));
        scene.setOnKeyReleased(e -> keysPressed.remove(e.getCode()));

        primaryStage.setTitle("Cross-Country Racing Adventure");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Generate Scenery
        generateScenery();

        // Game Loop
        AnimationTimer timer = new AnimationTimer() {
            private long lastTime = System.nanoTime();

            @Override
            public void handle(long now) {
                double dt = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                if (dt > 0.05) dt = 0.05; // Cap delta time

                update(dt);
                render(canvas.getGraphicsContext2D());
            }
        };
        timer.start();
    }

    private void generateScenery() {
        trees.clear();
        for (double d = 0; d < TRACK_LENGTH + 1000; d += 40) {
            double center = getRoadCenterX(d);
            // Left side trees
            trees.add(new Tree(center - ROAD_WIDTH / 2 - 50 - rand.nextDouble() * 150, d));
            // Right side trees
            trees.add(new Tree(center + ROAD_WIDTH / 2 + 50 + rand.nextDouble() * 150, d));
        }
    }

    private void initRace() {
        player1 = new Car(WIDTH / 2 - 40, 50, Color.CRIMSON, "P1");
        player2 = new Car(WIDTH / 2 + 40, 50, Color.DODGERBLUE, isVsAI ? "AI" : "P2");
        particles.clear();
        cameraY = 0;
        state = GameState.RACING;
    }

    private void update(double dt) {
        if (state == GameState.MENU) {
            if (keysPressed.contains(KeyCode.DIGIT1)) { isVsAI = true; initRace(); }
            if (keysPressed.contains(KeyCode.DIGIT2)) { isVsAI = false; initRace(); }
            return;
        }

        if (state == GameState.FINISHED) {
            if (keysPressed.contains(KeyCode.SPACE)) { state = GameState.MENU; }
            return;
        }

        // Handle Input
        handleInput(player1, KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D);
        if (!isVsAI) {
            handleInput(player2, KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT);
        } else {
            updateAI(player2, dt);
        }

        // Update Physics
        player1.update(dt, this);
        player2.update(dt, this);

        // Update Particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update(dt);
            if (p.life <= 0) particles.remove(i);
        }

        // Camera follows Player 1
        cameraY = player1.distance;

        // Check Win Condition
        if (player1.distance >= TRACK_LENGTH || player2.distance >= TRACK_LENGTH) {
            state = GameState.FINISHED;
        }
    }

    private void handleInput(Car car, KeyCode up, KeyCode down, KeyCode left, KeyCode right) {
        if (keysPressed.contains(up)) car.accelerate();
        if (keysPressed.contains(down)) car.brake();
        if (keysPressed.contains(left)) car.steer(-1);
        else if (keysPressed.contains(right)) car.steer(1);
        else car.steer(0);
    }

    private void updateAI(Car ai, double dt) {
        double lookAhead = ai.distance + 200;
        double targetX = getRoadCenterX(lookAhead);

        // Check for mud ahead
        boolean mudAhead = isMud(lookAhead);

        double diff = targetX - ai.x;
        double steerAmount = Math.max(-1, Math.min(1, diff / 120.0));
        ai.steer(steerAmount);

        ai.accelerate();
        if (Math.abs(steerAmount) > 0.6 || mudAhead) {
            ai.brake(); // Slow down for sharp curves or mud
        }
    }

    // --- RENDERING ---

    private void render(GraphicsContext gc) {
        gc.clearRect(0, 0, WIDTH, HEIGHT);

        if (state == GameState.MENU) {
            drawMenu(gc);
            return;
        }

        drawBackground(gc);
        drawTrack(gc);
        drawTrees(gc);
        drawParticles(gc);
        drawCar(gc, player1);
        drawCar(gc, player2);
        drawHUD(gc);

        if (state == GameState.FINISHED) {
            drawFinishScreen(gc);
        }
    }

    private void drawMenu(GraphicsContext gc) {
        // Background gradient
        LinearGradient bg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.FORESTGREEN), new Stop(1, Color.DARKSLATEGRAY));
        gc.setFill(bg);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 60));
        gc.fillText("CROSS-COUNTRY ADVENTURE", 120, 200);

        gc.setFont(Font.font("Arial", 30));
        gc.setFill(Color.GOLD);
        gc.fillText("Press [1] for 1 Player (vs Computer)", 250, 350);
        gc.fillText("Press [2] for 2 Players (Local)", 250, 420);

        gc.setFont(Font.font("Arial", 20));
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText("P1 Controls: W/A/S/D  |  P2 Controls: Arrow Keys", 220, 550);
    }

    private void drawBackground(GraphicsContext gc) {
        // Grass
        gc.setFill(Color.rgb(34, 80, 30));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Grass texture (simple noise)
        gc.setFill(Color.rgb(25, 60, 20));
        for (int i = 0; i < 200; i++) {
            double x = rand.nextDouble() * WIDTH;
            double y = rand.nextDouble() * HEIGHT;
            gc.fillOval(x, y, 3, 3);
        }
    }

    private void drawTrack(GraphicsContext gc) {
        // Draw road segments
        for (double screenY = 0; screenY < HEIGHT; screenY += 5) {
            double worldDist = cameraY + (HEIGHT * 0.7 - screenY);
            double centerX = getRoadCenterX(worldDist);

            boolean mud = isMud(worldDist);

            // Road base
            Color roadColor = mud ? Color.rgb(90, 60, 40) : Color.rgb(110, 100, 90);
            gc.setFill(roadColor);
            gc.fillRect(centerX - ROAD_WIDTH / 2, screenY, ROAD_WIDTH, 6);

            // Road edges (rumble strips)
            gc.setFill(mud ? Color.rgb(70, 40, 20) : Color.rgb(80, 70, 60));
            gc.fillRect(centerX - ROAD_WIDTH / 2 - 10, screenY, 10, 6);
            gc.fillRect(centerX + ROAD_WIDTH / 2, screenY, 10, 6);
        }
    }

    private void drawTrees(GraphicsContext gc) {
        for (Tree t : trees) {
            double screenY = HEIGHT * 0.7 - (t.distance - cameraY);
            if (screenY > -50 && screenY < HEIGHT + 50) {
                // Shadow
                gc.setFill(Color.rgb(0, 0, 0, 0.4));
                gc.fillOval(t.x - 15, screenY + 5, 40, 20);
                // Trunk
                gc.setFill(Color.SADDLEBROWN);
                gc.fillRect(t.x - 5, screenY - 20, 10, 30);
                // Leaves (layered for 3D effect)
                gc.setFill(Color.rgb(20, 90, 20));
                gc.fillOval(t.x - 25, screenY - 50, 50, 50);
                gc.setFill(Color.rgb(30, 120, 30));
                gc.fillOval(t.x - 15, screenY - 40, 35, 35);
            }
        }
    }

    private void drawCar(GraphicsContext gc, Car car) {
        double screenY = HEIGHT * 0.7 - (car.distance - cameraY);
        double screenX = car.x;

        if (screenY < -100 || screenY > HEIGHT + 100) return;

        gc.save();
        gc.translate(screenX, screenY);
        gc.rotate(car.angle);

        // Drop Shadow
        gc.setEffect(new DropShadow(10, 5, 5, Color.rgb(0, 0, 0, 0.6)));

        // Car Body Gradient
        LinearGradient bodyGrad = new LinearGradient(0, -25, 0, 25, false, CycleMethod.NO_CYCLE,
                new Stop(0, car.color.brighter()), new Stop(1, car.color.darker()));
        gc.setFill(bodyGrad);

        // Main Body
        gc.fillRoundRect(-18, -30, 36, 60, 10, 10);

        // Windshield
        gc.setFill(Color.rgb(150, 200, 255, 0.7));
        gc.fillRoundRect(-12, -15, 24, 20, 5, 5);

        // Rear Window
        gc.setFill(Color.rgb(100, 150, 200, 0.7));
        gc.fillRoundRect(-12, 15, 24, 10, 5, 5);

        // Wheels
        gc.setFill(Color.BLACK);
        gc.fillRect(-22, -22, 6, 14); // Front Left
        gc.fillRect(16, -22, 6, 14);  // Front Right
        gc.fillRect(-22, 12, 6, 14);  // Rear Left
        gc.fillRect(16, 12, 6, 14);   // Rear Right

        // Headlights
        gc.setFill(Color.YELLOW);
        gc.fillOval(-12, -30, 6, 4);
        gc.fillOval(6, -30, 6, 4);

        gc.setEffect(null);
        gc.restore();
    }

    private void drawParticles(GraphicsContext gc) {
        for (Particle p : particles) {
            double screenY = HEIGHT * 0.7 - (p.distance - cameraY);
            double alpha = p.life / p.maxLife;
            gc.setFill(Color.rgb(180, 150, 120, alpha * 0.6));
            gc.fillOval(p.x - p.size / 2, screenY - p.size / 2, p.size, p.size);
        }
    }

    private void drawHUD(GraphicsContext gc) {
        // Progress Bars
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRect(20, 20, 300, 20);
        gc.fillRect(20, 50, 300, 20);

        gc.setFill(player1.color);
        gc.fillRect(20, 20, (player1.distance / TRACK_LENGTH) * 300, 20);
        gc.setFill(player2.color);
        gc.fillRect(20, 50, (player2.distance / TRACK_LENGTH) * 300, 20);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 14));
        gc.fillText("P1", 330, 35);
        gc.fillText(isVsAI ? "AI" : "P2", 330, 65);

        // Speedometer
        gc.setFont(Font.font("Arial", 20));
        gc.fillText("Speed: " + (int)(player1.speed * 10) + " km/h", WIDTH - 200, 40);
    }

    private void drawFinishScreen(GraphicsContext gc) {
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial", 60));
        String winner = player1.distance >= player2.distance ? "PLAYER 1 WINS!" : (isVsAI ? "COMPUTER WINS!" : "PLAYER 2 WINS!");
        gc.fillText(winner, 250, 350);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 30));
        gc.fillText("Press SPACE to return to Menu", 300, 450);
    }

    // --- TRACK MATH ---

    public double getRoadCenterX(double distance) {
        // Creates beautiful, sweeping procedural curves
        return WIDTH / 2 + Math.sin(distance * 0.003) * 250 + Math.sin(distance * 0.007) * 120;
    }

    public boolean isMud(double distance) {
        return Math.sin(distance * 0.008) > 0.85;
    }

    public void spawnDust(double x, double distance, double angle) {
        if (rand.nextDouble() < 0.5) {
            particles.add(new Particle(x, distance, angle));
        }
    }

    // --- INNER CLASSES ---

    class Car {
        double x, distance, speed, angle, steerInput;
        double maxSpeed = 6.0;
        double acceleration = 3.0;
        double handling = 120.0;
        Color color;
        String name;

        public Car(double x, double distance, Color color, String name) {
            this.x = x;
            this.distance = distance;
            this.color = color;
            this.name = name;
        }

        public void accelerate() { speed += acceleration * 0.016; }
        public void brake() { speed -= acceleration * 1.5 * 0.016; }
        public void steer(double dir) { steerInput = dir; }

        public void update(double dt, CrossCountryRacing game) {
            // Terrain modifiers
            boolean onMud = game.isMud(distance);
            double center = game.getRoadCenterX(distance);
            boolean offRoad = Math.abs(x - center) > ROAD_WIDTH / 2;

            double friction = 0.98;
            double currentMaxSpeed = maxSpeed;

            if (onMud) { friction = 0.94; currentMaxSpeed *= 0.6; }
            if (offRoad) { friction = 0.90; currentMaxSpeed *= 0.4; }

            speed *= friction;
            if (speed > currentMaxSpeed) speed = currentMaxSpeed;
            if (speed < 0) speed = 0;

            // Steering (depends on speed)
            double turnRate = steerInput * handling * (speed / maxSpeed) * dt;
            angle += turnRate;

            // Clamp angle
            if (angle > 45) angle = 45;
            if (angle < -45) angle = -45;
            if (steerInput == 0) angle *= 0.9; // Return to center

            // Movement
            double rad = Math.toRadians(angle);
            x += Math.sin(rad) * speed;
            distance += Math.cos(rad) * speed;

            // Spawn particles if turning hard or on mud/dirt
            if (Math.abs(steerInput) > 0.5 || onMud || offRoad) {
                game.spawnDust(x, distance - 20, angle);
            }
        }
    }

    class Particle {
        double x, distance, vx, vy, life, maxLife, size;

        public Particle(double x, double distance, double angle) {
            this.x = x + (rand.nextDouble() - 0.5) * 20;
            this.distance = distance;
            this.vx = (rand.nextDouble() - 0.5) * 2;
            this.vy = (rand.nextDouble() - 0.5) * 2;
            this.maxLife = 1.0 + rand.nextDouble();
            this.life = maxLife;
            this.size = 5 + rand.nextDouble() * 10;
        }

        public void update(double dt) {
            life -= dt;
            x += vx;
            distance += vy;
            size += dt * 15; // Expand
        }
    }

    class Tree {
        double x, distance;
        public Tree(double x, double distance) {
            this.x = x;
            this.distance = distance;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}