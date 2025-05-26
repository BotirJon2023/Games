import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimpleBowlingSimulator3 extends JPanel implements ActionListener {

    // --- Game Constants ---
    private static final int LANE_WIDTH = 600;
    private static final int LANE_HEIGHT = 400;
    private static final int BALL_RADIUS = 15;
    private static final int PIN_WIDTH = 10;
    private static final int PIN_HEIGHT = 30;
    private static final int PIN_OFFSET_Y = 50; // Distance from top of lane for pins
    private static final int BALL_START_X = LANE_WIDTH / 2;
    private static final int BALL_START_Y = LANE_HEIGHT - BALL_RADIUS - 10;
    private static final int BALL_SPEED = 5; // Pixels per animation frame
    private static final int ANIMATION_DELAY = 20; // Milliseconds per frame

    // --- Game State Variables ---
    private Timer gameTimer;
    private int ballX, ballY;
    private boolean ballRolling;
    private List<Pin> pins;
    private int score;
    private int rollsInFrame;
    private int frameNumber;
    private int pinsKnockedDownThisRoll;

    private JLabel scoreLabel;
    private JLabel frameLabel;
    private JButton rollButton;
    private JButton resetButton;

    public SimpleBowlingSimulator3() {
        // Set the preferred size of the panel (the drawing area for the game)
        // Add extra height for control buttons and labels
        setPreferredSize(new Dimension(LANE_WIDTH, LANE_HEIGHT + 100));
        // Set a background color for the area outside the lane
        setBackground(new Color(150, 200, 255)); // Light blue for background
        // Use absolute positioning for UI components (labels, buttons)
        setLayout(null);

        // Initialize game state when the simulator starts
        resetGame();

        // Setup UI elements (labels and buttons)
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 20));
        // Set bounds: x, y, width, height
        scoreLabel.setBounds(10, LANE_HEIGHT + 10, 200, 30);
        add(scoreLabel);

        frameLabel = new JLabel("Frame: 1/10");
        frameLabel.setFont(new Font("Arial", Font.BOLD, 20));
        frameLabel.setBounds(10, LANE_HEIGHT + 40, 200, 30);
        add(frameLabel);

        rollButton = new JButton("Roll Ball");
        rollButton.setBounds(LANE_WIDTH - 150, LANE_HEIGHT + 10, 120, 40);
        // Add an ActionListener to the button to trigger the ball roll
        rollButton.addActionListener(e -> startRoll());
        add(rollButton);

        resetButton = new JButton("Reset Game");
        resetButton.setBounds(LANE_WIDTH - 150, LANE_HEIGHT + 60, 120, 40);
        resetButton.addActionListener(e -> resetGame());
        add(resetButton);

        // Initialize the Swing Timer for animation
        // This timer will call actionPerformed() every ANIMATION_DELAY milliseconds
        gameTimer = new Timer(ANIMATION_DELAY, this);
    }

    /**
     * Resets the game to its initial state.
     */
    private void resetGame() {
        ballX = BALL_START_X;
        ballY = BALL_START_Y;
        ballRolling = false;
        score = 0;
        rollsInFrame = 0;
        frameNumber = 1;
        pinsKnockedDownThisRoll = 0;
        setupPins(); // Re-setup all 10 pins
        updateLabels(); // Update score and frame display
        rollButton.setEnabled(true); // Enable roll button
        if (gameTimer.isRunning()) {
            gameTimer.stop(); // Stop any ongoing animation
        }
        repaint(); // Redraw the panel to show the reset state
    }

    /**
     * Arranges the 10 bowling pins in their standard triangular formation.
     */
    private void setupPins() {
        pins = new ArrayList<>();
        // 1st row (1 pin)
        pins.add(new Pin(LANE_WIDTH / 2, PIN_OFFSET_Y));
        // 2nd row (2 pins)
        pins.add(new Pin(LANE_WIDTH / 2 - PIN_WIDTH, PIN_OFFSET_Y + PIN_HEIGHT));
        pins.add(new Pin(LANE_WIDTH / 2 + PIN_WIDTH, PIN_OFFSET_Y + PIN_HEIGHT));
        // 3rd row (3 pins)
        pins.add(new Pin((int)(LANE_WIDTH / 2 - PIN_WIDTH * 1.5), PIN_OFFSET_Y + PIN_HEIGHT * 2));
        pins.add(new Pin(LANE_WIDTH / 2, PIN_OFFSET_Y + PIN_HEIGHT * 2));
        pins.add(new Pin((int)(LANE_WIDTH / 2 + PIN_WIDTH * 1.5), PIN_OFFSET_Y + PIN_HEIGHT * 2));
        // 4th row (4 pins)
        pins.add(new Pin(LANE_WIDTH / 2 - PIN_WIDTH * 2, PIN_OFFSET_Y + PIN_HEIGHT * 3));
        pins.add(new Pin(LANE_WIDTH / 2 - PIN_WIDTH, PIN_OFFSET_Y + PIN_HEIGHT * 3));
        pins.add(new Pin(LANE_WIDTH / 2 + PIN_WIDTH, PIN_OFFSET_Y + PIN_HEIGHT * 3));
        pins.add(new Pin(LANE_WIDTH / 2 + PIN_WIDTH * 2, PIN_OFFSET_Y + PIN_HEIGHT * 3));
    }

    /**
     * Initiates the ball rolling animation if not already rolling and game is not over.
     */
    private void startRoll() {
        // Only allow rolling if ball is not already rolling and within game frames
        if (!ballRolling && frameNumber <= 10) {
            ballRolling = true;
            ballX = BALL_START_X; // Reset ball position
            ballY = BALL_START_Y;
            pinsKnockedDownThisRoll = 0; // Reset count for current roll
            rollButton.setEnabled(false); // Disable button during roll
            gameTimer.start(); // Start the animation timer
        }
    }

    /**
     * This method is called by the Swing framework to draw the components on the panel.
     * @param g The Graphics object to draw with.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Always call super.paintComponent for proper Swing painting

        // Draw lane
        g.setColor(new Color(100, 70, 0)); // Brown color for the lane
        g.fillRect(0, 0, LANE_WIDTH, LANE_HEIGHT);

        // Draw ball
        g.setColor(Color.RED);
        // fillOval uses top-left corner, so adjust x,y by radius
        g.fillOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS, BALL_RADIUS * 2, BALL_RADIUS * 2);

        // Draw pins (only if they are standing)
        for (Pin pin : pins) {
            if (pin.isStanding()) {
                g.setColor(Color.WHITE);
                g.fillRect(pin.getX() - PIN_WIDTH / 2, pin.getY() - PIN_HEIGHT / 2, PIN_WIDTH, PIN_HEIGHT);
                g.setColor(Color.BLACK); // Outline for the pin
                g.drawRect(pin.getX() - PIN_WIDTH / 2, pin.getY() - PIN_HEIGHT / 2, PIN_WIDTH, PIN_HEIGHT);
            }
        }
    }

    /**
     * This method is called repeatedly by the `gameTimer` to update game state for animation.
     * @param e The ActionEvent generated by the timer.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (ballRolling) {
            ballY -= BALL_SPEED; // Move ball upwards (towards the pins)

            // Check for collision with pins (simplified approximation)
            // The ball is considered in the pin area when its Y coordinate is past a certain point
            if (ballY <= PIN_OFFSET_Y + PIN_HEIGHT * 2 && ballY > PIN_OFFSET_Y - BALL_RADIUS) {
                Random rand = new Random();
                int pinsBeforeRoll = (int) pins.stream().filter(Pin::isStanding).count();
                for (Pin pin : pins) {
                    if (pin.isStanding()) {
                        // Calculate distance between ball center and pin center
                        double dist = Math.sqrt(Math.pow(ballX - pin.getX(), 2) + Math.pow(ballY - pin.getY(), 2));
                        // If the ball is "close enough" to a pin and random chance allows, knock it down
                        if (dist < BALL_RADIUS + PIN_WIDTH / 2 + 5 && rand.nextDouble() < 0.7) { // 70% chance to knock down
                            pin.knockDown();
                        }
                    }
                }
                // Update count of pins knocked down in this specific roll
                pinsKnockedDownThisRoll = pinsBeforeRoll - (int) pins.stream().filter(Pin::isStanding).count();
            }

            // If ball has moved past the top of the lane (off-screen)
            if (ballY < -BALL_RADIUS) {
                gameTimer.stop(); // Stop the animation
                ballRolling = false; // Ball is no longer rolling
                processRoll(); // Process the outcome of the roll
            }
            repaint(); // Request a redraw of the panel to show updated ball/pin positions
        }
    }

    /**
     * Processes the outcome of a single roll of the ball, updates score, and manages frames.
     */
    private void processRoll() {
        rollsInFrame++; // Increment roll count for the current frame
        score += pinsKnockedDownThisRoll; // Add pins knocked down to total score

        // --- Simplified Scoring Logic (No complex strike/spare bonuses) ---
        // This only adds the knocked-down pins to the score directly.
        // Proper bowling scoring for strikes and spares (e.g., adding bonus points from next rolls)
        // would significantly increase code complexity and line count.

        int standingPinsAfterRoll = (int) pins.stream().filter(Pin::isStanding).count();

        if (rollsInFrame == 1) { // First roll of the frame
            if (pinsKnockedDownThisRoll == 10) { // Strike on first roll
                JOptionPane.showMessageDialog(this, "Strike!");
                frameNumber++; // Move to next frame
                rollsInFrame = 0; // Reset rolls for the new frame
                setupPins(); // Reset all pins for the next frame
            } else {
                // If not a strike, pins remain as they are for the second roll
                // No need to call setupPins() here.
            }
        } else if (rollsInFrame == 2) { // Second roll of the frame
            if (standingPinsAfterRoll == 0) { // All pins knocked down in two rolls (Spare)
                JOptionPane.showMessageDialog(this, "Spare!");
            }
            frameNumber++; // Move to next frame
            rollsInFrame = 0; // Reset rolls for the new frame
            setupPins(); // Reset all pins for the next frame
        }

        // Check if the game is over (all 10 frames completed)
        if (frameNumber > 10) {
            JOptionPane.showMessageDialog(this, "Game Over! Final Score: " + score);
            rollButton.setEnabled(false); // Disable rolling after game ends
        } else {
            rollButton.setEnabled(true); // Enable button for next roll/frame
        }
        updateLabels(); // Update score and frame display
    }

    /**
     * Updates the text of the score and frame labels.
     */
    private void updateLabels() {
        scoreLabel.setText("Score: " + score);
        frameLabel.setText("Frame: " + frameNumber + "/10");
    }

    // --- Helper Class for Bowling Pins ---
    // This is an inner static class, meaning it doesn't need an instance of
    // SimpleBowlingSimulator to be created, but it's logically grouped with it.
    private static class Pin {
        private int x, y; // Position of the pin's center
        private boolean standing; // True if the pin is still standing

        public Pin(int x, int y) {
            this.x = x;
            this.y = y;
            this.standing = true; // Pins start standing
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public boolean isStanding() {
            return standing;
        }

        public void knockDown() {
            this.standing = false; // Mark pin as knocked down
        }
    }

    // --- Main Method ---
    // This is the entry point of the Java application.
    public static void main(String[] args) {
        // Swing applications should always be run on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Simple Bowling Simulator"); // Create the main window
            SimpleBowlingSimulator3 game = new SimpleBowlingSimulator3(); // Create an instance of our game panel
            frame.add(game); // Add the game panel to the frame

            frame.pack(); // Size the frame to fit the preferred size of its contents (our game panel)
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close the application when the frame is closed
            frame.setLocationRelativeTo(null); // Center the window on the screen
            frame.setResizable(false); // Prevent resizing the window
            frame.setVisible(true); // Make the frame visible
        });
    }
}