import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random; // For potential future random elements

/**
 * Main class for the Basketball Slam Dunk Challenge game.
 * This class sets up the JFrame and adds the GamePanel.
 */
public class BasketballSlamDunkChallenge extends JFrame {

    // Constants for the game window dimensions
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final String GAME_TITLE = "Basketball Slam Dunk Challenge";

    /**
     * Constructor for the BasketballSlamDunkChallenge class.
     * Initializes the JFrame and sets up the game panel.
     */
    public BasketballSlamDunkChallenge() {
        setTitle(GAME_TITLE);
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close operation
        setResizable(false); // Prevent resizing for consistent layout
        setLocationRelativeTo(null); // Center the window on the screen

        // Create and add the game panel
        GamePanel gamePanel = new GamePanel();
        add(gamePanel);

        // Make the frame visible
        setVisible(true);
    }

    /**
     * Main method to start the game.
     * Uses SwingUtilities.invokeLater to ensure GUI updates are on the Event Dispatch Thread.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BasketballSlamDunkChallenge());
    }

    /**
     * Inner class representing the main game panel where all game logic and drawing occurs.
     * It extends JPanel and implements ActionListener for the game loop and KeyListener for input.
     */
    private class GamePanel extends JPanel implements ActionListener, KeyListener {

        // --- Game State Variables ---
        private Timer gameTimer; // Timer for the game loop
        private final int DELAY = 15; // Delay in milliseconds for the game loop (controls frame rate)

        // Player properties
        private int playerX;
        private int playerY;
        private int playerWidth;
        private int playerHeight;
        private int playerSpeedX; // Horizontal movement speed (not used for this simple version)
        private int playerJumpStrength; // Initial upward velocity for jump
        private int playerGravity; // Gravity affecting player's vertical movement

        // Ball properties
        private int ballX;
        private int ballY;
        private int ballRadius;
        private int ballVelX; // Ball horizontal velocity
        private int ballVelY; // Ball vertical velocity
        private int ballGravity; // Gravity affecting ball's vertical movement

        // Basket properties
        private int basketX;
        private int basketY;
        private int basketWidth;
        private int basketHeight;
        private int hoopY; // Y-coordinate of the actual hoop for dunking
        private int hoopThickness; // Thickness of the hoop line

        // Game state flags
        private boolean isJumping;
        private boolean isDunking;
        private boolean ballInHand; // True if ball is with the player
        private boolean gameStarted;
        private boolean gameOver;

        // Score
        private int score;
        private int successfulDunks;
        private int attempts;

        // Animation states for player (simple state machine)
        private enum PlayerState {
            IDLE, JUMPING, DUNK_PREP, DUNK_FINISH, FALLING
        }
        private PlayerState currentPlayerState;

        // --- Constructor for GamePanel ---
        public GamePanel() {
            setFocusable(true); // Allow the panel to receive keyboard input
            addKeyListener(this); // Register key listener

            // Initialize game components and state
            initGame();

            // Set up the game timer
            gameTimer = new Timer(DELAY, this);
            gameTimer.start(); // Start the game loop
        }

        /**
         * Initializes all game variables to their starting values.
         */
        private void initGame() {
            // Player initialization
            playerWidth = 50;
            playerHeight = 100;
            playerX = WIDTH / 4 - playerWidth / 2; // Starting position on the left
            playerY = HEIGHT - playerHeight - 50; // Ground level
            playerSpeedX = 0; // No horizontal movement for this version
            playerJumpStrength = -20; // Negative value for upward movement
            playerGravity = 1; // Downward acceleration

            // Ball initialization
            ballRadius = 15;
            // Ball starts in player's hand
            ballX = playerX + playerWidth / 2;
            ballY = playerY + playerHeight / 4;
            ballVelX = 0;
            ballVelY = 0;
            ballGravity = 1;
            ballInHand = true;

            // Basket initialization
            basketWidth = 80;
            basketHeight = 150;
            basketX = WIDTH * 3 / 4 - basketWidth / 2; // Position on the right
            basketY = HEIGHT - basketHeight - 50; // Ground level for the pole
            hoopY = basketY + basketHeight / 4; // Y-coordinate of the hoop
            hoopThickness = 5;

            // Game state flags
            isJumping = false;
            isDunking = false;
            gameStarted = false; // Game starts when first key is pressed
            gameOver = false;

            // Score initialization
            score = 0;
            successfulDunks = 0;
            attempts = 0;

            currentPlayerState = PlayerState.IDLE;
        }

        /**
         * Resets the game state for a new attempt or restart.
         */
        private void resetGame() {
            playerX = WIDTH / 4 - playerWidth / 2;
            playerY = HEIGHT - playerHeight - 50;
            ballX = playerX + playerWidth / 2;
            ballY = playerY + playerHeight / 4;
            ballVelX = 0;
            ballVelY = 0;
            ballInHand = true;
            isJumping = false;
            isDunking = false;
            currentPlayerState = PlayerState.IDLE;
            gameOver = false;
            // Score is not reset on individual attempt, only on full game restart
        }

        /**
         * The main game loop, called by the Timer at regular intervals.
         * Updates game state and triggers repainting.
         *
         * @param e ActionEvent from the Timer.
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!gameStarted && !gameOver) {
                // Game logic only runs after the first key press
                repaint(); // Still repaint to show initial screen
                return;
            }

            if (gameOver) {
                repaint(); // Keep repainting game over screen
                return;
            }

            // --- Update Player State ---
            if (isJumping) {
                playerY += ballVelY; // Use ballVelY for player's vertical movement during jump
                ballVelY += playerGravity; // Apply gravity

                // Keep ball with player during jump until dunking
                if (ballInHand) {
                    ballX = playerX + playerWidth / 2;
                    ballY = playerY + playerHeight / 4;
                }

                // Check if player lands back on the ground
                if (playerY >= HEIGHT - playerHeight - 50) {
                    playerY = HEIGHT - playerHeight - 50; // Snap to ground
                    isJumping = false;
                    ballVelY = 0; // Reset vertical velocity
                    currentPlayerState = PlayerState.IDLE;
                    if (!isDunking) { // If not dunking, ball returns to hand
                        ballInHand = true;
                        ballX = playerX + playerWidth / 2;
                        ballY = playerY + playerHeight / 4;
                    }
                    isDunking = false; // Reset dunking state after landing
                }
            }

            // --- Update Ball State (if not in hand) ---
            if (!ballInHand) {
                ballX += ballVelX;
                ballY += ballVelY;
                ballVelY += ballGravity; // Apply gravity to the ball

                // Simple collision with ground
                if (ballY + ballRadius >= HEIGHT - 50) { // Ground level
                    ballY = HEIGHT - 50 - ballRadius;
                    ballVelY = (int) (-ballVelY * 0.7); // Bounce with some energy loss
                    ballVelX = (int) (ballVelX * 0.8); // Reduce horizontal speed
                    if (Math.abs(ballVelY) < 2) ballVelY = 0; // Stop bouncing if too slow
                    if (Math.abs(ballVelX) < 2) ballVelX = 0; // Stop horizontal if too slow
                }

                // Collision with left/right walls (simple bounce)
                if (ballX - ballRadius < 0 || ballX + ballRadius > WIDTH) {
                    ballVelX *= -1;
                }

                // Check for dunking condition
                checkDunk();
            }

            // Repaint the panel to show updated positions
            repaint();
        }

        /**
         * Checks if the ball has successfully gone through the basket.
         */
        private void checkDunk() {
            // Check if ball is within horizontal bounds of the hoop
            boolean inHoopX = (ballX > basketX && ballX < basketX + basketWidth);
            // Check if ball is passing through the hoop's Y-coordinate from above
            boolean passingThroughHoopY = (ballY + ballRadius > hoopY && ballY + ballRadius < hoopY + hoopThickness + ballVelY);

            if (inHoopX && passingThroughHoopY && !ballInHand) {
                // Ensure it's a new dunk, not counting multiple times for one shot
                if (currentPlayerState == PlayerState.DUNK_FINISH) { // Only count if player was in dunking state
                    score += 10;
                    successfulDunks++;
                    ballInHand = true; // Ball returns to player after successful dunk (for next attempt)
                    resetGame(); // Reset player/ball position for next dunk
                    System.out.println("DUNK! Score: " + score);
                }
            }
        }

        /**
         * Custom painting method for the game elements.
         *
         * @param g The Graphics context for drawing.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Call superclass method to clear background
            Graphics2D g2d = (Graphics2D) g; // Use Graphics2D for better rendering options

            // --- Draw Background ---
            g2d.setColor(new Color(135, 206, 235)); // Sky blue
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            g2d.setColor(new Color(34, 139, 34)); // Forest green for ground
            g2d.fillRect(0, HEIGHT - 50, WIDTH, 50);

            // --- Draw Basket ---
            g2d.setColor(Color.GRAY); // Pole color
            g2d.fillRect(basketX + basketWidth / 2 - 5, basketY, 10, basketHeight); // Pole

            g2d.setColor(Color.ORANGE); // Hoop color
            g2d.setStroke(new BasicStroke(hoopThickness)); // Set stroke for hoop thickness
            g2d.drawOval(basketX, hoopY, basketWidth, hoopThickness * 2); // Draw the hoop as an oval

            g2d.setColor(Color.RED); // Backboard color
            g2d.fillRect(basketX + basketWidth, hoopY - 30, 10, 60); // Backboard

            // --- Draw Player ---
            g2d.setColor(Color.BLUE); // Player body color
            g2d.fillRect(playerX, playerY, playerWidth, playerHeight); // Player body

            g2d.setColor(Color.BLACK); // Player head
            g2d.fillOval(playerX + playerWidth / 4, playerY - playerWidth / 2, playerWidth / 2, playerWidth / 2);

            // Simple animation based on state
            if (currentPlayerState == PlayerState.DUNK_PREP || currentPlayerState == PlayerState.DUNK_FINISH) {
                // Draw arm raised for dunking
                g2d.setStroke(new BasicStroke(5));
                g2d.drawLine(playerX + playerWidth / 2, playerY + playerHeight / 4,
                        playerX + playerWidth / 2 + 20, playerY - 10);
            } else {
                // Draw arm down (idle/jumping)
                g2d.setStroke(new BasicStroke(5));
                g2d.drawLine(playerX + playerWidth / 2, playerY + playerHeight / 4,
                        playerX + playerWidth / 2 + 10, playerY + playerHeight / 2);
            }

            // --- Draw Ball ---
            g2d.setColor(Color.ORANGE); // Basketball color
            g2d.fillOval(ballX - ballRadius, ballY - ballRadius, ballRadius * 2, ballRadius * 2);

            // Draw ball lines
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawLine(ballX, ballY - ballRadius, ballX, ballY + ballRadius); // Vertical line
            g2d.drawLine(ballX - ballRadius, ballY, ballX + ballRadius, ballY); // Horizontal line
            g2d.drawArc(ballX - ballRadius, ballY - ballRadius, ballRadius * 2, ballRadius * 2, 45, 90);
            g2d.drawArc(ballX - ballRadius, ballY - ballRadius, ballRadius * 2, ballRadius * 2, 225, 90);


            // --- Draw Score and Instructions ---
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.drawString("Score: " + score, 20, 30);
            g2d.drawString("Dunks: " + successfulDunks + " / " + attempts, 20, 60);

            if (!gameStarted && !gameOver) {
                g2d.setFont(new Font("Arial", Font.BOLD, 36));
                String startMsg = "Press SPACE to Jump, D to Dunk!";
                FontMetrics fm = g2d.getFontMetrics();
                int x = (WIDTH - fm.stringWidth(startMsg)) / 2;
                int y = HEIGHT / 2 - fm.getHeight();
                g2d.drawString(startMsg, x, y);

                String restartMsg = "Press R to Restart Game";
                x = (WIDTH - fm.stringWidth(restartMsg)) / 2;
                y = HEIGHT / 2 + fm.getHeight();
                g2d.drawString(restartMsg, x, y);
            }

            if (gameOver) {
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                String gameOverMsg = "GAME OVER!";
                FontMetrics fm = g2d.getFontMetrics();
                int x = (WIDTH - fm.stringWidth(gameOverMsg)) / 2;
                int y = HEIGHT / 2 - fm.getHeight();
                g2d.drawString(gameOverMsg, x, y);

                g2d.setFont(new Font("Arial", Font.BOLD, 30));
                String finalScoreMsg = "Final Score: " + score;
                x = (WIDTH - fm.stringWidth(finalScoreMsg)) / 2;
                y = HEIGHT / 2 + fm.getHeight();
                g2d.drawString(finalScoreMsg, x, y);

                String restartMsg = "Press R to Play Again";
                x = (WIDTH - fm.stringWidth(restartMsg)) / 2;
                y = HEIGHT / 2 + fm.getHeight() * 2;
                g2d.drawString(restartMsg, x, y);
            }

            Toolkit.getDefaultToolkit().sync(); // Ensures all graphics operations are complete
        }

        // --- KeyListener Implementations ---

        /**
         * Handles key pressed events.
         *
         * @param e KeyEvent object containing information about the key pressed.
         */
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (!gameStarted && key != KeyEvent.VK_R) { // Any key press (except R) starts the game
                gameStarted = true;
                // No specific action here, just allows game loop to proceed
            }

            if (gameOver) {
                if (key == KeyEvent.VK_R) {
                    // Reset all game states for a new game
                    initGame();
                    gameStarted = false; // Wait for first action to start
                    score = 0;
                    successfulDunks = 0;
                    attempts = 0;
                    repaint();
                }
                return; // Do not process other keys if game is over
            }

            if (key == KeyEvent.VK_SPACE && !isJumping) {
                // Initiate jump
                isJumping = true;
                ballVelY = playerJumpStrength; // Set initial upward velocity
                currentPlayerState = PlayerState.JUMPING;
                attempts++; // Count this as an attempt
                System.out.println("Player jumps!");
            }

            if (key == KeyEvent.VK_D && isJumping && ballInHand) {
                // Initiate dunk
                isDunking = true;
                ballInHand = false; // Ball is no longer in hand
                // Give the ball an initial push towards the basket
                ballVelX = (basketX + basketWidth / 2 - ballX) / 10; // Aim towards basket center
                ballVelY = 5; // Push ball downwards slightly
                currentPlayerState = PlayerState.DUNK_FINISH; // Player is now in dunking pose
                System.out.println("Player attempts dunk!");
            }

            // You can add more keys for other actions if needed
        }

        /**
         * Handles key released events (not used in this game).
         *
         * @param e KeyEvent object.
         */
        @Override
        public void keyReleased(KeyEvent e) {
            // Not used for continuous actions in this simple game
        }

        /**
         * Handles key typed events (not used in this game).
         *
         * @param e KeyEvent object.
         */
        @Override
        public void keyTyped(KeyEvent e) {
            // Not used
        }
    }
}
