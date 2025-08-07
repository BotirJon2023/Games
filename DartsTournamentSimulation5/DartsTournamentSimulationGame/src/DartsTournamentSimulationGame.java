import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform; // For more advanced transformations

public class GamePanel extends JPanel implements ActionListener {

    // --- Game State Variables ---
    private final int WIDTH = 800;
    private final int HEIGHT = 600;
    private final int FPS = 60; // Frames per second
    private Timer gameTimer;

    // Example animation variable: A dart's position
    private double dartX = WIDTH / 2;
    private double dartY = HEIGHT - 50; // Starting at the bottom
    private double targetDartY = 100;   // Target Y for animation
    private double dartSpeed = 5;       // Pixels per frame
    private boolean dartAnimating = false;

    // Game Objects (placeholders)
    private Dartboard dartboard;
    private Player currentPlayer;
    // ... other game objects like Players, GameLogic, etc.

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.DARK_GRAY);
        setFocusable(true); // Important for keyboard input

        // Initialize game objects
        dartboard = new Dartboard(WIDTH / 2, HEIGHT / 2, 200); // Center, radius 200
        currentPlayer = new Player("Player 1");

        // Add mouse listener for dart throwing (example)
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!dartAnimating) {
                    // Simulate throwing a dart towards the targetDartY
                    // In a real game, this would involve more complex physics/aiming
                    dartX = e.getX(); // Start dart from mouse click X
                    dartY = HEIGHT - 50; // Reset Y
                    dartAnimating = true;
                }
            }
        });
    }

    public void startGameLoop() {
        gameTimer = new Timer(1000 / FPS, this); // Timer fires every (1000/FPS) milliseconds
        gameTimer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // This method is called repeatedly by the gameTimer
        updateGameState();
        repaint(); // Request a redraw of the panel
    }

    private void updateGameState() {
        // --- Animation Logic ---
        if (dartAnimating) {
            dartY -= dartSpeed; // Move dart upwards

            if (dartY <= targetDartY) {
                dartY = targetDartY;
                dartAnimating = false; // Animation finished
                // Here, you would calculate dart hit and update score
                System.out.println("Dart landed! Calculating score...");
                // Simulate hitting a random section for now
                int score = dartboard.calculateRandomHitScore();
                currentPlayer.addScore(score);
                System.out.println(currentPlayer.getName() + " scored: " + score + ". Total: " + currentPlayer.getScore());
                // Reset dart for next throw or next player
                dartX = WIDTH / 2;
                dartY = HEIGHT - 50;
            }
        }

        // --- Other Game Logic Updates ---
        // e.g., checking game state, player turns, AI decisions, etc.
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Clears the panel and paints background

        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smoother graphics
        Object Rendering2D = null;
        g2d.setRenderingHint(Rendering2D.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- Draw Game Elements ---

        // 1. Draw Dartboard
        dartboard.draw(g2d);

        // 2. Draw Current Score/Player Info
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        String scoreText = "Score: " + currentPlayer.getScore();
        String playerText = "Player: " + currentPlayer.getName();
        g2d.drawString(playerText, 20, 30);
        g2d.drawString(scoreText, 20, 60);

        // 3. Draw the Dart (if animating or waiting for throw)
        if (dartAnimating || !dartAnimating) { // Always draw for now
            drawDart(g2d, (int) dartX, (int) dartY);
        }

        // You would draw other elements here: e.g., aiming reticle,
        // other players' scores, game messages, etc.
    }

    private void drawDart(Graphics2D g2d, int x, int y) {
        // Simple dart shape
        int dartWidth = 10;
        int dartHeight = 30;

        g2d.setColor(Color.RED);
        g2d.fillRect(x - dartWidth / 2, y, dartWidth, dartHeight); // Body
        g2d.setColor(Color.LIGHT_GRAY);
        int[] xPoints = {x - dartWidth / 2, x + dartWidth / 2, x};
        int[] yPoints = {y, y, y - 15}; // Pointy tip
        g2d.fillPolygon(xPoints, yPoints, 3);
    }
}