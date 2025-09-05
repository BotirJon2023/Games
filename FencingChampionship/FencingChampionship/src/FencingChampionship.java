import Application.Application;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.animation.AnimationTimer;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

// Main game class extending JavaFX Application
public class FencingChampionship extends Application {
    // Game constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int FENCER_WIDTH = 50;
    private static final int FENCER_HEIGHT = 100;
    private static final int SWORD_LENGTH = 60;
    private static final double ATTACK_SPEED = 5.0;
    private static final double MOVE_SPEED = 3.0;
    private static final int MATCH_POINTS = 5;
    private static final double AI_REACTION_TIME = 0.5;
    private static final double ATTACK_COOLDOWN = 0.8;

    // Game state variables
    private double playerX = 200;
    private double playerY = HEIGHT - FENCER_HEIGHT - 50;
    private double opponentX = WIDTH - 200 - FENCER_WIDTH;
    private double opponentY = HEIGHT - FENCER_HEIGHT - 50;
    private double playerSwordAngle = 0;
    private double opponentSwordAngle = 0;
    private int playerScore = 0;
    private int opponentScore = 0;
    private boolean playerAttacking = false;
    private boolean opponentAttacking = false;
    private double playerAttackTime = 0;
    private double opponentAttackTime = 0;
    private boolean gameOver = false;
    private boolean playerMovingLeft = false;
    private boolean playerMovingRight = false;
    private boolean playerParrying = false;
    private boolean opponentParrying = false;
    private double lastPlayerAttack = 0;
    private double lastOpponentAttack = 0;
    private List<Particle> particles = new ArrayList<>();
    private Random random = new Random();

    // UI elements
    private Text scoreText;
    private Text statusText;
    private Canvas canvas;
    private GraphicsContext gc;

    @Override
    public void start(Stage primaryStage) {
        // Initialize the canvas and graphics context
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        Pane root = new Pane(canvas);

        // Initialize score and status text
        scoreText = new Text(20, 30, "Player: 0  Opponent: 0");
        scoreText.setFont(new Font("Arial", 20));
        scoreText.setFill(Color.WHITE);

        statusText = new Text(WIDTH / 2 - 100, HEIGHT / 2, "");
        statusText.setFont(new Font("Arial", 30));
        statusText.setFill(Color.RED);

        root.getChildren().addAll(scoreText, statusText);

        // Create the scene
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.setFill(Color.DARKGRAY);

        // Set up key event handlers
        scene.setOnKeyPressed(this::handleKeyPress);
        scene.setOnKeyReleased(this::handleKeyRelease);

        // Start the game loop
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(now);
                render();
            }
        };
        gameLoop.start();

        // Set up the stage
        primaryStage.setTitle("Fencing Championship");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // Handle key presses
    private void handleKeyPress(KeyEvent event) {
        if (gameOver) {
            if (event.getCode() == KeyCode.SPACE) {
                resetGame();
            }
            return;
        }

        switch (event.getCode()) {
            case A:
                playerMovingLeft = true;
                break;
            case D:
                playerMovingRight = true;
                break;
            case J:
                if (!playerAttacking && lastPlayerAttack <= 0) {
                    playerAttacking = true;
                    playerAttackTime = 0;
                    lastPlayerAttack = ATTACK_COOLDOWN;
                }
                break;
            case K:
                playerParrying = true;
                break;
        }
    }

    // Handle key releases
    private void handleKeyRelease(KeyEvent event) {
        switch (event.getCode()) {
            case A:
                playerMovingLeft = false;
                break;
            case D:
                playerMovingRight = false;
                break;
            case K:
                playerParrying = false;
                break;
        }
    }

    // Update game state
    private void update(long now) {
        if (gameOver) return;

        // Update player position
        if (playerMovingLeft && playerX > 0) {
            playerX -= MOVE_SPEED;
        }
        if (playerMovingRight && playerX < WIDTH - FENCER_WIDTH) {
            playerX += MOVE_SPEED;
        }

        // Update player attack animation
        if (playerAttacking) {
            playerAttackTime += 0.1;
            playerSwordAngle = Math.sin(playerAttackTime * ATTACK_SPEED) * 45;
            if (playerAttackTime >= Math.PI / ATTACK_SPEED) {
                playerAttacking = false;
                playerSwordAngle = 0;
            }
        }

        // Update opponent AI
        updateOpponent();

        // Update opponent attack animation
        if (opponentAttacking) {
            opponentAttackTime += 0.1;
            opponentSwordAngle = Math.sin(opponentAttackTime * ATTACK_SPEED) * -45;
            if (opponentAttackTime >= Math.PI / ATTACK_SPEED) {
                opponentAttacking = false;
                opponentSwordAngle = 0;
            }
        }

        // Check for hits
        checkHits();

        // Update particles
        updateParticles();

        // Update cooldowns
        lastPlayerAttack = Math.max(0, lastPlayerAttack - 0.016);
        lastOpponentAttack = Math.max(0, lastOpponentAttack - 0.016);

        // Update score display
        scoreText.setText(String.format("Player: %d  Opponent: %d", playerScore, opponentScore));
    }

    // Update opponent AI
    private void updateOpponent() {
        // Simple AI: move towards player and attack randomly
        double distanceToPlayer = playerX - opponentX;
        if (distanceToPlayer > 100 && opponentX < WIDTH - FENCER_WIDTH) {
            opponentX += MOVE_SPEED * 0.8;
        } else if (distanceToPlayer < -100 && opponentX > 0) {
            opponentX -= MOVE_SPEED * 0.8;
        }

        // Randomly decide to attack or parry
        if (lastOpponentAttack <= 0 && random.nextDouble() < 0.02) {
            if (!opponentAttacking) {
                opponentAttacking = true;
                opponentAttackTime = 0;
                lastOpponentAttack = ATTACK_COOLDOWN;
            }
        }

        // Parry if player is attacking
        if (playerAttacking && random.nextDouble() < 0.3) {
            opponentParrying = true;
        } else if (random.nextDouble() < 0.05) {
            opponentParrying = false;
        }
    }

    // Check for hits and update scores
    private void checkHits() {
        if (playerAttacking && !opponentParrying && Math.abs(playerX + FENCER_WIDTH + SWORD_LENGTH - opponentX) < 50) {
            opponentScore++;
            createHitParticles(opponentX, opponentY + FENCER_HEIGHT / 2);
            checkGameOver();
        }
        if (opponentAttacking && !playerParrying && Math.abs(opponentX - (playerX + FENCER_WIDTH + SWORD_LENGTH)) < 50) {
            playerScore++;
            createHitParticles(playerX + FENCER_WIDTH, playerY + FENCER_HEIGHT / 2);
            checkGameOver();
        }
    }

    // Check if game is over
    private void checkGameOver() {
        if (playerScore >= MATCH_POINTS || opponentScore >= MATCH_POINTS) {
            gameOver = true;
            String winner = playerScore >= MATCH_POINTS ? "Player Wins!" : "Opponent Wins!";
            statusText.setText(winner + "\nPress SPACE to restart");
        }
    }

    // Reset game state
    private void resetGame() {
        playerX = 200;
        opponentX = WIDTH - 200 - FENCER_WIDTH;
        playerScore = 0;
        opponentScore = 0;
        playerAttacking = false;
        opponentAttacking = false;
        playerSwordAngle = 0;
        opponentSwordAngle = 0;
        gameOver = false;
        statusText.setText("");
        particles.clear();
    }

    // Create hit particles for visual effect
    private void createHitParticles(double x, double y) {
        for (int i = 0; i < 10; i++) {
            particles.add(new Particle(x, y));
        }
    }

    // Update particles
    private void updateParticles() {
        List<Particle> particlesToRemove = new ArrayList<>();
        for (Particle p : particles) {
            p.update();
            if (p.lifetime <= 0) {
                particlesToRemove.add(p);
            }
        }
        particles.removeAll(particlesToRemove);
    }

    // Render the game
    private void render() {
        // Clear canvas
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw piste (fencing strip)
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(50, HEIGHT - 150, WIDTH - 100, 100);

        // Draw player
        drawFencer(playerX, playerY, playerSwordAngle, Color.BLUE, true);

        // Draw opponent
        drawFencer(opponentX, opponentY, opponentSwordAngle, Color.RED, false);

        // Draw particles
        for (Particle p : particles) {
            p.render(gc);
        }
    }

    // Draw a fencer
    private void drawFencer(double x, double y, double swordAngle, Color color, boolean isPlayer) {
        // Draw body
        gc.setFill(color);
        gc.fillRect(x, y, FENCER_WIDTH, FENCER_HEIGHT);

        // Draw head
        gc.setFill(Color.BEIGE);
        gc.fillOval(x + FENCER_WIDTH / 4, y - 20, FENCER_WIDTH / 2, 20);

        // Draw sword
        gc.save();
        gc.translate(x + FENCER_WIDTH, y + FENCER_HEIGHT / 2);
        if (!isPlayer) {
            gc.translate(-FENCER_WIDTH, 0);
            gc.scale(-1, 1);
        }
        gc.rotate(swordAngle);
        gc.setFill(Color.SILVER);
        gc.fillRect(0, -5, SWORD_LENGTH, 10);
        gc.restore();

        // Draw parry indicator
        if ((isPlayer && playerParrying) || (!isPlayer && opponentParrying)) {
            gc.setFill(Color.YELLOW);
            gc.fillOval(x + FENCER_WIDTH / 4, y - 40, 20, 20);
        }
    }

    // Particle class for hit effects
    private class Particle {
        private double x, y;
        private double vx, vy;
        private double lifetime;

        public Particle(double x, double y) {
            this.x = x;
            this.y = y;
            this.vx = (random.nextDouble() - 0.5) * 5;
            this.vy = (random.nextDouble() - 0.5) * 5;
            this.lifetime = 1.0;
        }

        public void update() {
            x += vx;
            y += vy;
            lifetime -= 0.05;
        }

        public void render(GraphicsContext gc) {
            gc.setFill(Color.YELLOW.deriveColor(0, 1, 1, lifetime));
            gc.fillOval(x, y, 5, 5);
        }
    }

    // Main method to launch the application
    public static void main(String[] args) {
        launch(args);
    }

    // Additional utility methods for game enhancements
    private void drawBackground() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(50, HEIGHT - 150, WIDTH - 100, 100);
        gc.setStroke(Color.WHITE);
        gc.strokeLine(WIDTH / 2, HEIGHT - 150, WIDTH / 2, HEIGHT - 50);
    }

    private void drawScoreBoard() {
        gc.setFill(Color.BLACK);
        gc.fillRect(10, 10, 200, 40);
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Arial", 16));
        gc.fillText(String.format("Player: %d  Opponent: %d", playerScore, opponentScore), 20, 35);
    }

    private void drawGameOverScreen() {
        if (gameOver) {
            gc.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.7));
            gc.fillRect(0, 0, WIDTH, HEIGHT);
            gc.setFill(Color.WHITE);
            gc.setFont(new Font("Arial", 40));
            String winner = playerScore >= MATCH_POINTS ? "Player Wins!" : "Opponent Wins!";
            gc.fillText(winner, WIDTH / 2 - 100, HEIGHT / 2 - 20);
            gc.setFont(new Font("Arial", 20));
            gc.fillText("Press SPACE to restart", WIDTH / 2 - 80, HEIGHT / 2 + 20);
        }
    }

    private void playSoundEffect(String effect) {
        // Placeholder for sound effects (JavaFX AudioClip can be used here)
        // System.out.println("Playing sound: " + effect);
    }

    private void drawHealthBars() {
        // Player health bar
        gc.setFill(Color.RED);
        gc.fillRect(20, 60, 100, 10);
        gc.setFill(Color.GREEN);
        gc.fillRect(20, 60, 100 * (MATCH_POINTS - opponentScore) / MATCH_POINTS, 10);

        // Opponent health bar
        gc.setFill(Color.RED);
        gc.fillRect(WIDTH - 120, 60, 100, 10);
        gc.setFill(Color.GREEN);
        gc.fillRect(WIDTH - 120, 60, 100 * (MATCH_POINTS - playerScore) / MATCH_POINTS, 10);
    }

    private void updateAnimations() {
        // Additional animation logic can be added here
        if (playerAttacking) {
            playSoundEffect("sword_swing");
        }
        if (opponentAttacking) {
            playSoundEffect("sword_swing");
        }
    }

    private void drawInstructions() {
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Arial", 14));
        gc.fillText("Controls: A/D to move, J to attack, K to parry", 20, HEIGHT - 20);
    }

    // Override render to include additional elements
    private void render() {
        drawBackground();
        drawFencer(playerX, playerY, playerSwordAngle, Color.BLUE, true);
        drawFencer(opponentX, opponentY, opponentSwordAngle, Color.RED, false);
        drawHealthBars();
        drawInstructions();
        for (Particle p : particles) {
            p.render(gc);
        }
        drawGameOverScreen();
        updateAnimations();
    }

    // Additional AI logic for more complex opponent behavior
    private void advancedOpponentAI() {
        double distanceToPlayer = Math.abs(playerX - opponentX);
        if (distanceToPlayer < 150 && random.nextDouble() < 0.1) {
            opponentParrying = true;
        } else if (distanceToPlayer > 200) {
            opponentParrying = false;
        }

        if (playerAttacking && distanceToPlayer < 100 && random.nextDouble() < 0.4) {
            opponentAttacking = true;
            opponentAttackTime = 0;
            lastOpponentAttack = ATTACK_COOLDOWN;
        }
    }

    // Enhanced hit detection
    private void enhancedHitDetection() {
        if (playerAttacking && !opponentParrying) {
            double swordTipX = playerX + FENCER_WIDTH + Math.cos(Math.toRadians(playerSwordAngle)) * SWORD_LENGTH;
            double swordTipY = playerY + FENCER_HEIGHT / 2 + Math.sin(Math.toRadians(playerSwordAngle)) * SWORD_LENGTH;
            if (Math.abs(swordTipX - opponentX) < 20 && Math.abs(swordTipY - (opponentY + FENCER_HEIGHT / 2)) < 20) {
                opponentScore++;
                createHitParticles(opponentX, opponentY + FENCER_HEIGHT / 2);
                playSoundEffect("hit");
                checkGameOver();
            }
        }
        if (opponentAttacking && !playerParrying) {
            double swordTipX = opponentX + Math.cos(Math.toRadians(opponentSwordAngle)) * SWORD_LENGTH;
            double swordTipY = opponentY + FENCER_HEIGHT / 2 + Math.sin(Math.toRadians(opponentSwordAngle)) * SWORD_LENGTH;
            if (Math.abs(swordTipX - (playerX + FENCER_WIDTH)) < 20 && Math.abs(swordTipY - (playerY + FENCER_HEIGHT / 2)) < 20) {
                playerScore++;
                createHitParticles(playerX + FENCER_WIDTH, playerY + FENCER_HEIGHT / 2);
                playSoundEffect("hit");
                checkGameOver();
            }
        }
    }

    // Update method with additional logic
    private void update(long now) {
        if (gameOver) return;

        // Update player movement
        if (playerMovingLeft && playerX > 0) {
            playerX -= MOVE_SPEED;
        }
        if (playerMovingRight && playerX < WIDTH - FENCER_WIDTH) {
            playerX += MOVE_SPEED;
        }

        // Update attack animations
        if (playerAttacking) {
            playerAttackTime += 0.1;
            playerSwordAngle = Math.sin(playerAttackTime * ATTACK_SPEED) * 45;
            if (playerAttackTime >= Math.PI / ATTACK_SPEED) {
                playerAttacking = false;
                playerSwordAngle = 0;
            }
        }

        // Update opponent
        updateOpponent();
        advancedOpponentAI();

        // Update opponent attack animation
        if (opponentAttacking) {
            opponentAttackTime += 0.1;
            opponentSwordAngle = Math.sin(opponentAttackTime * ATTACK_SPEED) * -45;
            if (opponentAttackTime >= Math.PI / ATTACK_SPEED) {
                opponentAttacking = false;
                opponentSwordAngle = 0;
            }
        }

        // Check hits with enhanced detection
        enhancedHitDetection();

        // Update particles
        updateParticles();

        // Update cooldowns
        lastPlayerAttack = Math.max(0, lastPlayerAttack - 0.016);
        lastOpponentAttack = Math.max(0, lastOpponentAttack - 0.016);

        // Update score display
        scoreText.setText(String.format("Player: %d  Opponent: %d", playerScore, opponentScore));
    }
}