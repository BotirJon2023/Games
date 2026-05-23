import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.util.*;
import java.util.List;

public class FootballManagementSim extends Application {

    enum GameMode { PVC, PVP }
    enum MatchState { PRE_MATCH, PLAYING, GOAL, ENDED }

    private Canvas canvas;
    private GraphicsContext gc;
    private final int WIDTH = 900, HEIGHT = 600;
    private MatchState state = MatchState.PRE_MATCH;
    private GameMode mode = GameMode.PVC;
    private double matchTime = 0;
    private int homeScore = 0, awayScore = 0;
    private List<Player> homeTeam = new ArrayList<>();
    private List<Player> awayTeam = new ArrayList<>();
    private Ball ball = new Ball(WIDTH / 2, HEIGHT / 2);
    private List<Particle> particles = new ArrayList<>();
    private AnimationTimer gameLoop;
    private Label scoreLabel;
    private Button startBtn;
    private ToggleButton modeBtn;
    private Random rand = new Random();

    @Override
    public void start(Stage stage) {
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();

        // UI Controls
        VBox ui = new VBox(10);
        ui.setPadding(new javafx.geometry.Insets(10));
        scoreLabel = new Label("HOME 0 - 0 AWAY");
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        startBtn = new Button("Start Match");
        modeBtn = new ToggleButton("Mode: PvC");
        modeBtn.setOnAction(e -> {
            mode = mode.equals(GameMode.PVC) ? GameMode.PVP : GameMode.PVC;
            modeBtn.setText("Mode: " + (mode == GameMode.PVC ? "PvC" : "PvP"));
        });

        startBtn.setOnAction(e -> resetMatch());
        ui.getChildren().addAll(scoreLabel, startBtn, modeBtn);

        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        root.setBottom(ui);
        root.setStyle("-fx-background-color: #1a1a1a;");

        Scene scene = new Scene(root, WIDTH, HEIGHT + 80);
        stage.setTitle("Football Management Sim");
        stage.setScene(scene);
        stage.show();

        setupInput();
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(1.0 / 60.0);
                render();
            }
        };
        gameLoop.start();
    }

    private void setupInput() {
        canvas.setOnKeyPressed(e -> {
            if (state != MatchState.PLAYING) return;
            if (mode == GameMode.PVP) {
                // Player 2 controls: Arrow keys for away team captain
                if (e.getCode().toString().startsWith("ARROW")) {
                    awayTeam.get(0).applyInput(e.getCode());
                }
            }
        });
        canvas.setOnMouseMoved(e -> {
            if (state == MatchState.PLAYING) {
                // Player 1 controls: Mouse guides home captain
                homeTeam.get(0).setTarget(e.getX(), e.getY());
            }
        });
    }

    private void resetMatch() {
        state = MatchState.PLAYING;
        matchTime = 0;
        homeScore = 0; awayScore = 0;
        homeTeam.clear(); awayTeam.clear();
        particles.clear();
        initTeams();
        ball.reset(WIDTH / 2, HEIGHT / 2);
        startBtn.setText("Restart Match");
    }

    private void initTeams() {
        // Home: 2-1-1-1 formation
        addPlayer(homeTeam, 150, 150, "#e74c3c");
        addPlayer(homeTeam, 250, 300, "#e74c3c");
        addPlayer(homeTeam, 350, 200, "#e74c3c");
        addPlayer(homeTeam, 400, 400, "#e74c3c");
        addPlayer(homeTeam, 450, 300, "#e74c3c");

        // Away: 1-1-2-1 formation
        addPlayer(awayTeam, 750, 150, "#3498db");
        addPlayer(awayTeam, 650, 300, "#3498db");
        addPlayer(awayTeam, 550, 200, "#3498db");
        addPlayer(awayTeam, 500, 400, "#3498db");
        addPlayer(awayTeam, 450, 300, "#3498db");
    }

    private void addPlayer(List<Player> team, double x, double y, String color) {
        team.add(new Player(x, y, color));
    }

    private void update(double dt) {
        if (state != MatchState.PLAYING) return;

        matchTime += dt;
        if (matchTime >= 90) { state = MatchState.ENDED; return; }

        ball.update(dt);
        homeTeam.forEach(p -> p.update(dt, ball, homeTeam, awayTeam));
        awayTeam.forEach(p -> p.update(dt, ball, awayTeam, homeTeam));

        // Simple AI for non-captain players & away captain in PvC
        if (mode == GameMode.PVC) {
            awayTeam.forEach(p -> {
                if (!p.isCaptain()) p.aiChase(ball);
            });
        }

        // Collision & scoring
        checkCollisions();
        updateParticles(dt);
    }

    private void checkCollisions() {
        for (Player p : homeTeam) {
            if (p.distanceTo(ball) < p.getRadius() + ball.getRadius()) {
                ball.kick(p, true);
                break;
            }
        }
        for (Player p : awayTeam) {
            if (p.distanceTo(ball) < p.getRadius() + ball.getRadius()) {
                ball.kick(p, false);
                break;
            }
        }

        // Goal detection
        if (ball.x < 20 && ball.y > 200 && ball.y < 400) {
            awayScore++;
            triggerGoal("AWAY");
        } else if (ball.x > WIDTH - 20 && ball.y > 200 && ball.y < 400) {
            homeScore++;
            triggerGoal("HOME");
        }
        scoreLabel.setText(String.format("HOME %d - %d AWAY | %.0f'", homeScore, awayScore, matchTime));
    }

    private void triggerGoal(String scorer) {
        state = MatchState.GOAL;
        for (int i = 0; i < 50; i++) {
            particles.add(new Particle(ball.x, ball.y, scorer.equals("HOME") ? Color.RED : Color.BLUE));
        }
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            ball.reset(WIDTH / 2, HEIGHT / 2);
            state = MatchState.PLAYING;
        }).start();
    }

    private void updateParticles(double dt) {
        particles.removeIf(p -> p.life <= 0);
        particles.forEach(p -> p.update(dt));
    }

    private void render() {
        gc.setFill(Color.web("#27ae60"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Field lines
        gc.setStroke(Color.WHITE.deriveColor(0, 0, 1, 0.7));
        gc.setLineWidth(2);
        gc.strokeRect(50, 50, WIDTH - 100, HEIGHT - 100);
        gc.strokeLine(WIDTH / 2, 50, WIDTH / 2, HEIGHT - 50);
        gc.strokeOval(WIDTH / 2 - 50, HEIGHT / 2 - 50, 100, 100);

        // Goals
        gc.setFill(Color.WHITE.deriveColor(0, 0, 1, 0.3));
        gc.fillRect(0, 200, 50, 200);
        gc.fillRect(WIDTH - 50, 200, 50, 200);

        homeTeam.forEach(this::drawPlayer);
        awayTeam.forEach(this::drawPlayer);
        drawBall();
        drawParticles();
    }

    private void drawPlayer(Player p) {
        gc.setFill(Color.web(p.color));
        gc.fillOval(p.x - p.getRadius(), p.y - p.getRadius(), p.getRadius() * 2, p.getRadius() * 2);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeOval(p.x - p.getRadius(), p.y - p.getRadius(), p.getRadius() * 2, p.getRadius() * 2);
        if (p.isCaptain()) {
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(2);
            gc.strokeOval(p.x - p.getRadius() - 3, p.y - p.getRadius() - 3, (p.getRadius() + 3) * 2, (p.getRadius() + 3) * 2);
        }
    }

    private void drawBall() {
        gc.setFill(Color.WHITE);
        gc.fillOval(ball.x - ball.getRadius(), ball.y - ball.getRadius(), ball.getRadius() * 2, ball.getRadius() * 2);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.strokeOval(ball.x - ball.getRadius(), ball.y - ball.getRadius(), ball.getRadius() * 2, ball.getRadius() * 2);
    }

    private void drawParticles() {
        particles.forEach(p -> {
            gc.setFill(p.color.deriveColor(0, 1, 1, p.life));
            gc.fillOval(p.x - 2, p.y - 2, 4, 4);
        });
    }

    // --- Inner Classes ---
    static class Player {
        double x, y, targetX, targetY, vx, vy;
        String color;
        double radius = 10;
        boolean captain;

        Player(double x, double y, String color) {
            this.x = this.targetX = x;
            this.y = this.targetY = y;
            this.color = color;
        }

        boolean isCaptain() { return x < WIDTH / 2 && y > 250 && y < 350 && color.equals("#e74c3c") ||
                x > WIDTH / 2 && y > 250 && y < 350 && color.equals("#3498db"); }

        void applyInput(javafx.scene.input.KeyCode code) {
            double step = 5;
            if (code == javafx.scene.input.KeyCode.UP) targetY -= step;
            if (code == javafx.scene.input.KeyCode.DOWN) targetY += step;
            if (code == javafx.scene.input.KeyCode.LEFT) targetX -= step;
            if (code == javafx.scene.input.KeyCode.RIGHT) targetX += step;
        }

        void setTarget(double tx, double ty) {
            this.targetX = Math.max(60, Math.min(WIDTH - 60, tx));
            this.targetY = Math.max(60, Math.min(HEIGHT - 60, ty));
        }

        void aiChase(Ball b) {
            targetX = b.x + (rand.nextDouble() - 0.5) * 30;
            targetY = b.y + (rand.nextDouble() - 0.5) * 30;
        }

        void update(double dt, Ball ball, List<Player> friends, List<Player> foes) {
            double dx = targetX - x;
            double dy = targetY - y;
            double dist = Math.hypot(dx, dy);
            if (dist > 1) {
                double speed = Math.min(2.5, dist * 0.1);
                vx = (dx / dist) * speed;
                vy = (dy / dist) * speed;
            } else {
                vx *= 0.8; vy *= 0.8;
            }
            x += vx; y += vy;

            // Keep in bounds
            x = Math.max(50, Math.min(WIDTH - 50, x));
            y = Math.max(50, Math.min(HEIGHT - 50, y));

            // Simple passing/shooting logic for captains
            if (captain && distTo(ball) < radius + ball.radius + 5) {
                double goalX = color.equals("#e74c3c") ? WIDTH - 20 : 20;
                double angle = Math.atan2(300 - y, goalX - x);
                ball.vx = Math.cos(angle) * 6;
                ball.vy = Math.sin(angle) * 6;
            }
        }

        double distTo(Ball b) { return Math.hypot(x - b.x, y - b.y); }
        double getRadius() { return radius; }
    }

    static class Ball {
        double x, y, vx, vy;
        double radius = 6;

        Ball(double x, double y) { reset(x, y); }

        void reset(double x, double y) {
            this.x = x; this.y = y; vx = 0; vy = 0;
        }

        void kick(Player p, boolean isHome) {
            double angle = Math.atan2(300 - p.y, (isHome ? WIDTH - 20 : 20) - p.x) + (Math.random() - 0.5) * 0.5;
            vx = Math.cos(angle) * 5;
            vy = Math.sin(angle) * 5;
        }

        void update(double dt) {
            x += vx; y += vy;
            vx *= 0.98; vy *= 0.98;
            if (y < 50 || y > HEIGHT - 50) vy *= -0.8;
            y = Math.max(50, Math.min(HEIGHT - 50, y));
        }

        double getRadius() { return radius; }
    }

    static class Particle {
        double x, y, vx, vy, life = 1.0;
        Color color;

        Particle(double x, double y, Color c) {
            this.x = x; this.y = y; this.color = c;
            vx = (Math.random() - 0.5) * 8;
            vy = (Math.random() - 0.5) * 8;
        }

        void update(double dt) {
            x += vx; y += vy;
            vy += 0.1;
            life -= dt * 1.2;
        }
    }

    public static void main(String[] args) { launch(args); }
}