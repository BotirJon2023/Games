import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class GamePanel extends JPanel implements ActionListener, KeyListener {

    private final int FPS = 60; // Frames per second
    private final int DELAY = 1000 / FPS; // Milliseconds between frames
    private Timer gameTimer;

    private Player player;
    private List<Exercise> currentExercises; // Could be a queue or current active exercises
    private long gameStartTime;
    private int score;
    private GameState gameState; // Enum for game states: MENU, PLAYING, PAUSED, GAME_OVER

    // Animation variables for the player
    private BufferedImage playerStandImage;
    private BufferedImage playerJumpImage; // Example for an action animation
    private boolean isJumping; // State to trigger jump animation

    // Enum for game states
    public enum GameState {
        MENU,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    public GamePanel() {
        setFocusable(true); // Panel can receive keyboard input
        addKeyListener(this); // Register for key events
        setBackground(Color.DARK_GRAY); // Set background color

        loadGameAssets();
        initializeGame();
    }

    private void loadGameAssets() {
        try {
            // Load player images. Make sure these paths are correct!
            playerStandImage = ImageIO.read(getClass().getResourceAsStream("/com/crossfitgame/resources/player_stand.png"));
            playerJumpImage = ImageIO.read(getClass().getResourceAsStream("/com/crossfitgame/resources/player_jump.png"));
            System.out.println("Player images loaded successfully.");
        } catch (IOException e) {
            System.err.println("Failed to load player images: " + e.getMessage());
            e.printStackTrace();
            // Provide a fallback or exit if essential assets can't be loaded
            playerStandImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB); // Placeholder
            playerJumpImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB); // Placeholder
            Graphics2D g2d = playerStandImage.createGraphics();
            g2d.setColor(Color.BLUE);
            g2d.fillRect(0, 0, 50, 50);
            g2d.dispose();
            g2d = playerJumpImage.createGraphics();
            g2d.setColor(Color.CYAN);
            g2d.fillRect(0, 0, 50, 50);
            g2d.dispose();
        }
    }

    private void initializeGame() {
        player = new Player(100, 400, 50, 70); // x, y, width, height
        currentExercises = new ArrayList<>();
        score = 0;
        gameState = GameState.PLAYING; // Start in playing state for this demo
        gameStartTime = System.currentTimeMillis();

        // Add some conceptual exercises
        currentExercises.add(new Exercise("Squats", 10, "Perform 10 squats.", 0, 0));
        currentExercises.add(new Exercise("Burpees", 5, "Complete 5 burpees.", 0, 0));
    }

    public void startGameThread() {
        if (gameTimer == null || !gameTimer.isRunning()) {
            gameTimer = new Timer(DELAY, this); // 'this' means actionPerformed in this class will be called
            gameTimer.start();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // This method is called repeatedly by the Timer (game loop)
        if (gameState == GameState.PLAYING) {
            updateGame();
            repaint(); // Request the panel to redraw itself
        }
    }

    private void updateGame() {
        // Update game logic here
        player.update(); // Update player's position, state, etc.

        // Example: If player completes an exercise, advance to next
        if (!currentExercises.isEmpty()) {
            Exercise current = currentExercises.get(0);
            // In a real game, you'd have more complex logic
            // e.g., if player performs a specific input sequence, complete exercise
            // For now, let's simulate completion after a few seconds
            if (System.currentTimeMillis() - gameStartTime > 5000 && current.getName().equals("Squats")) {
                System.out.println("Squats completed!");
                currentExercises.remove(0);
                score += 100;
                gameStartTime = System.currentTimeMillis(); // Reset timer for next action/exercise
            } else if (System.currentTimeMillis() - gameStartTime > 3000 && current.getName().equals("Burpees") && currentExercises.size() < 2) {
                System.out.println("Burpees completed!");
                currentExercises.remove(0);
                score += 150;
                gameStartTime = System.currentTimeMillis();
            }
        }

        if (currentExercises.isEmpty() && gameState == GameState.PLAYING) {
            gameState = GameState.GAME_OVER; // Example: all exercises done, game over
            gameTimer.stop();
            System.out.println("All exercises completed! Final Score: " + score);
        }

        // Logic for jumping animation state
        if (isJumping) {
            // After a short duration, reset jumping state
            // In a real game, this would be tied to player's y-velocity or ground detection
            if (System.currentTimeMillis() % 1000 < 500) { // Simple toggle for demo
                isJumping = false;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Clears the panel
        Graphics2D g2d = (Graphics2D) g;

        // Draw player
        BufferedImage playerImageToDraw = isJumping ? playerJumpImage : playerStandImage;
        if (playerImageToDraw != null) {
            g2d.drawImage(playerImageToDraw, player.getX(), player.getY(), player.getWidth(), player.getHeight(), null);
        } else {
            // Fallback for when image is not loaded
            g2d.setColor(Color.BLUE);
            g2d.fillRect(player.getX(), player.getY(), player.getWidth(), player.getHeight());
        }


        // Draw current exercise info
        g2d.setColor(Color.WHITE);
        g2d.setFont(g2d.getFont().deriveFont(20f));
        if (!currentExercises.isEmpty()) {
            Exercise current = currentExercises.get(0);
            g2d.drawString("Current Exercise: " + current.getName() + " (" + current.getRepetitions() + ")", 50, 50);
            g2d.drawString(current.getDescription(), 50, 80);
        } else if (gameState == GameState.GAME_OVER) {
            g2d.drawString("GAME OVER! All exercises completed!", getWidth() / 2 - 200, getHeight() / 2 - 50);
        } else {
            g2d.drawString("No exercises left. Waiting...", 50, 50);
        }

        // Draw score
        g2d.drawString("Score: " + score, 50, 110);

        // Debug info (optional)
        // g2d.setColor(Color.RED);
        // g2d.drawRect(player.getX(), player.getY(), player.getWidth(), player.getHeight()); // Player hitbox
    }

    // --- KeyListener Implementations ---
    @Override
    public void keyTyped(KeyEvent e) {
        // Not typically used for game input, but can be for text input
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (gameState == GameState.PLAYING) {
            switch (key) {
                case KeyEvent.VK_LEFT:
                    player.setX(player.getX() - player.getSpeed());
                    break;
                case KeyEvent.VK_RIGHT:
                    player.setX(player.getX() + player.getSpeed());
                    break;
                case KeyEvent.VK_SPACE:
                    // Trigger jump animation and potentially player jump logic
                    if (!isJumping) { // Prevent multiple jumps in mid-air (simple check)
                        player.jump(); // Player object handles its jump physics
                        isJumping = true;
                        System.out.println("Player jumps!");
                    }
                    break;
                case KeyEvent.VK_P: // Pause game
                    if (gameTimer.isRunning()) {
                        gameTimer.stop();
                        gameState = GameState.PAUSED;
                        System.out.println("Game Paused");
                    } else {
                        gameTimer.start();
                        gameState = GameState.PLAYING;
                        System.out.println("Game Resumed");
                    }
                    break;
                // Add more keys for specific exercise actions (e.g., 'S' for squat, 'B' for burpee)
                // case KeyEvent.VK_S:
                //     System.out.println("Squat action triggered!");
                //     // Logic to check if current exercise is squat and complete it
                //     break;
            }
        } else if (gameState == GameState.GAME_OVER) {
            if (key == KeyEvent.VK_R) { // Restart game
                initializeGame();
                startGameThread();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Often used for stopping movement when a key is released
        // For this simple demo, we're not using it for continuous movement stopping
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_SPACE) {
            // In a real game, you might reset jump state here if it was a "press and hold" jump
            // For now, it's handled in updateGame() for simplicity.
        }
    }
}