import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

// --- Main Application Class ---
public class MixedMartialArtsSimulator extends Application {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private Fighter player1;
    private Fighter player2; // For AI opponent

    private Pane gameRoot;
    private Map<KeyCode, Boolean> keysPressed = new HashMap<>();

    @Override
    public void start(Stage primaryStage) throws Exception {
        gameRoot = new Pane();
        gameRoot.setPrefSize(WIDTH, HEIGHT);

        // Load a simple background image (replace with your own)
        // You'll need an image file named 'arena_background.jpg' in your resources
        try {
            Image background = new Image(getClass().getResourceAsStream("/arena_background.jpg"));
            ImageView backgroundView = new ImageView(background);
            backgroundView.setFitWidth(WIDTH);
            backgroundView.setFitHeight(HEIGHT);
            gameRoot.getChildren().add(backgroundView);
        } catch (Exception e) {
            System.err.println("Error loading background image: " + e.getMessage());
            // Fallback to a simple colored background if image fails
            gameRoot.setStyle("-fx-background-color: #303030;");
        }


        // Initialize fighters
        // You'll need sprite sheets for your fighters.
        // For this example, I'll assume a simple sprite sheet where:
        // - Frame 0-3 are for idle/walking (e.g., 4 frames)
        // - Frame 4-5 are for a punch (e.g., 2 frames)
        // Adjust these paths and values to your actual sprite sheets.
        Image player1SpriteSheet = new Image(getClass().getResourceAsStream("/fighter1_sprites.png"));
        Image player2SpriteSheet = new Image(getClass().getResourceAsStream("/fighter2_sprites.png"));

        player1 = new Fighter("Player 1", player1SpriteSheet, 0, 5, 64, 64, 100, 50); // x, y, width, height, frames, punchFrames
        player2 = new Fighter("Player 2", player2SpriteSheet, 0, 5, 64, 64, 100, 50); // x, y, width, height, frames, punchFrames

        player1.setTranslateX(100);
        player1.setTranslateY(HEIGHT - player1.getHeight() - 20); // Place at bottom
        player1.setScaleX(1); // Facing right

        player2.setTranslateX(WIDTH - player2.getWidth() - 100);
        player2.setTranslateY(HEIGHT - player2.getHeight() - 20); // Place at bottom
        player2.setScaleX(-1); // Facing left initially

        gameRoot.getChildren().addAll(player1, player2);

        Scene scene = new Scene(gameRoot);
        scene.setOnKeyPressed(event -> keysPressed.put(event.getCode(), true));
        scene.setOnKeyReleased(event -> keysPressed.put(event.getCode(), false));

        // Game Loop
        new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                updateGame(now);
            }
        }.start();

        primaryStage.setTitle("MMA Simulator");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void updateGame(long now) {
        // Player 1 Movement
        boolean moving = false;
        if (isKeyPressed(KeyCode.LEFT)) {
            player1.move(-3);
            player1.setScaleX(1); // Face left
            moving = true;
        } else if (isKeyPressed(KeyCode.RIGHT)) {
            player1.move(3);
            player1.setScaleX(-1); // Face right
            moving = true;
        }

        if (moving) {
            player1.playWalkAnimation();
        } else {
            player1.stopWalkAnimation();
        }

        // Player 1 Actions
        if (isKeyPressed(KeyCode.SPACE)) {
            player1.punch();
        }

        // Simple AI for Player 2
        // If player 1 is close, player 2 might punch
        // This is extremely basic AI
        double distance = Math.abs(player1.getTranslateX() - player2.getTranslateX());
        if (distance < 150 && Math.random() < 0.01) { // Small chance to punch if close
            player2.punch();
        } else if (distance > 200) { // If far, move towards player
            if (player1.getTranslateX() < player2.getTranslateX()) {
                player2.move(-1); // Move left
                player2.setScaleX(-1);
            } else {
                player2.move(1); // Move right
                player2.setScaleX(1);
            }
            player2.playWalkAnimation();
        } else {
            player2.stopWalkAnimation();
        }

        // --- Collision Detection and Damage (Highly simplified) ---
        // This is a very crude example. In a real game, you'd check specific attack hitboxes.
        if (player1.isAttacking() && player1.getBoundsInParent().intersects(player2.getBoundsInParent())) {
            // Apply damage if the attack frame is active
            if (player1.getCurrentFrame() == player1.getPunchStartFrame() + 1) { // Assuming punch hits on second frame
                System.out.println("Player 1 hits Player 2!");
                player2.takeDamage(10); // Example damage
            }
        }

        if (player2.isAttacking() && player2.getBoundsInParent().intersects(player1.getBoundsInParent())) {
            if (player2.getCurrentFrame() == player2.getPunchStartFrame() + 1) {
                System.out.println("Player 2 hits Player 1!");
                player1.takeDamage(10);
            }
        }
        // --- End Collision Detection ---

        // Update health (display would be UI elements)
        // In a real game, you'd update UI elements like health bars here.
        // System.out.println("Player 1 Health: " + player1.getHealth() + " | Player 2 Health: " + player2.getHealth());

        // Check for game over (simple health check)
        if (player1.getHealth() <= 0) {
            System.out.println("Player 2 Wins!");
            // Implement game over screen or restart
        } else if (player2.getHealth() <= 0) {
            System.out.println("Player 1 Wins!");
            // Implement game over screen or restart
        }
    }

    private boolean isKeyPressed(KeyCode code) {
        return keysPressed.getOrDefault(code, false);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

// --- Fighter Class ---
class Fighter extends ImageView {
    private String name;
    private double health;
    private double stamina;
    private double strength;
    private double speed;

    // Animation properties
    private final Image spriteSheet;
    private final int numFrames; // Total frames in the sprite sheet for walking/idle
    private final int punchFrames; // Number of frames specifically for the punch animation
    private final int frameWidth;
    private final int frameHeight;
    private int currentFrame = 0;
    private final Duration frameDuration = Duration.millis(100); // Speed of animation

    private Timeline animationTimeline;
    private boolean isAttacking = false;
    private final int punchStartFrame; // The frame index where punch animation begins

    public Fighter(String name, Image spriteSheet, int initialFrameX, int initialFrameY,
                   int frameWidth, int frameHeight, double health, double stamina) {
        super(spriteSheet);
        this.name = name;
        this.spriteSheet = spriteSheet;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.health = health;
        this.stamina = stamina;
        this.strength = 10; // Example strength
        this.speed = 2;   // Example speed

        // Assuming sprite sheet frames are arranged horizontally
        this.numFrames = (int) (spriteSheet.getWidth() / frameWidth);
        // Let's assume punch animation starts after the first 4 walking/idle frames
        this.punchStartFrame = 4; // Adjust based on your sprite sheet layout
        this.punchFrames = 2; // Assuming 2 frames for punch

        setViewport(new Rectangle2D(initialFrameX, initialFrameY, frameWidth, frameHeight));
        setFitWidth(frameWidth);
        setFitHeight(frameHeight);

        setupAnimation();
    }

    private void setupAnimation() {
        animationTimeline = new Timeline();
        animationTimeline.setCycleCount(Animation.INDEFINITE);

        KeyFrame kf = new KeyFrame(frameDuration, event -> {
            if (!isAttacking) {
                // Play walking/idle animation
                currentFrame = (currentFrame + 1) % punchStartFrame; // Loop through idle/walk frames
                setViewport(new Rectangle2D(currentFrame * frameWidth, 0, frameWidth, frameHeight));
            } else {
                // Play punch animation
                currentFrame++;
                if (currentFrame >= punchStartFrame + punchFrames) {
                    currentFrame = punchStartFrame; // Loop punch frames or revert to idle
                    isAttacking = false; // Punch animation finished
                }
                setViewport(new Rectangle2D(currentFrame * frameWidth, 0, frameWidth, frameHeight));
            }
        });
        animationTimeline.getKeyFrames().add(kf);
        animationTimeline.play();
    }

    public void move(double dx) {
        setTranslateX(getTranslateX() + dx * speed);
        // Keep within bounds
        if (getTranslateX() < 0) setTranslateX(0);
        if (getTranslateX() + getFitWidth() > MixedMartialArtsSimulator.WIDTH) {
            setTranslateX(MixedMartialArtsSimulator.WIDTH - getFitWidth());
        }
    }

    public void punch() {
        if (!isAttacking) {
            isAttacking = true;
            currentFrame = punchStartFrame; // Start punch animation from its first frame
            // Temporarily pause main animation to play punch
            animationTimeline.stop();
            Timeline punchTimeline = new Timeline(
                    new KeyFrame(frameDuration, event -> {
                        currentFrame++;
                        if (currentFrame >= punchStartFrame + punchFrames) {
                            isAttacking = false;
                            currentFrame = 0; // Reset to idle frame
                            animationTimeline.play(); // Resume normal animation
                        }
                        setViewport(new Rectangle2D(currentFrame * frameWidth, 0, frameWidth, frameHeight));
                    })
            );
            punchTimeline.setCycleCount(punchFrames); // Play punch animation once
            punchTimeline.setOnFinished(event -> {
                isAttacking = false;
                currentFrame = 0; // Reset to idle frame
                animationTimeline.play(); // Resume normal animation
            });
            punchTimeline.play();
        }
    }

    public void takeDamage(double damage) {
        this.health -= damage;
        if (this.health < 0) this.health = 0;
        System.out.println(name + " health: " + this.health);
    }

    public double getHealth() {
        return health;
    }

    public boolean isAttacking() {
        return isAttacking;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public int getPunchStartFrame() {
        return punchStartFrame;
    }

    public void playWalkAnimation() {
        if (animationTimeline.getStatus() == Animation.Status.STOPPED || isAttacking) {
            // Only play if not already playing or if not attacking
            if (!isAttacking) {
                animationTimeline.play();
            }
        }
    }

    public void stopWalkAnimation() {
        if (!isAttacking) {
            // Pause animation when idle
            animationTimeline.pause();
            // Set to idle frame (first frame)
            currentFrame = 0;
            setViewport(new Rectangle2D(currentFrame * frameWidth, 0, frameWidth, frameHeight));
        }
    }

    // You would add more methods for kick, block, special moves, etc.
}