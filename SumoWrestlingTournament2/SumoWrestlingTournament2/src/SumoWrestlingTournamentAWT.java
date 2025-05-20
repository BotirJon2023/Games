import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Random;

public class SumoWrestlingTournamentAWT extends JFrame {
    // Game constants
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int RING_RADIUS = 200;
    private static final int RING_CENTER_X = WINDOW_WIDTH / 2;
    private static final int RING_CENTER_Y = WINDOW_HEIGHT / 2;
    private static final int PLAYER_SIZE = 50;
    private static final double PLAYER_SPEED = 5.0;
    private static final double PUSH_FORCE = 2.0;
    private static final int MAX_ROUNDS = 3;
    private static final int FPS = 60;
    private static final int FRAME_TIME_MS = 1000 / FPS;

    // Game state
    private boolean gameRunning = true;
    private int round = 1;
    private int player1Score = 0;
    private int player2Score = 0;
    private boolean roundOver = false;
    private String message = "Round 1: Fight!";
    private long roundStartTime;
    private int messageAlpha = 255; // For fading message

    // Player positions and movement
    private double player1X = RING_CENTER_X - 100;
    private double player1Y = RING_CENTER_Y;
    private double player2X = RING_CENTER_X + 100;
    private double player2Y = RING_CENTER_Y;
    private boolean player1Up, player1Down, player1Left, player1Right;
    private boolean player2Up, player2Down, player2Left, player2Right;

    // Animation variables
    private ArrayList<Sprite> sprites;
    private Timer gameTimer;
    private Random random = new Random();

    // Sprite class for players
    private class Sprite {
        private double x, y;
        private Color baseColor;
        private int animationFrame;
        private long lastFrameTime;
        private final long frameDuration = 100_000_000; // 100ms per frame
        private double scale = 1.0;

        public Sprite(double x, double y, Color baseColor) {
            this.x = x;
            this.y = y;
            this.baseColor = baseColor;
            animationFrame = 0;
            lastFrameTime = System.nanoTime();
            scale = 1.0;
        }

        public void updateAnimation() {
            long currentTime = System.nanoTime();
            if (currentTime - lastFrameTime >= frameDuration) {
                animationFrame = (animationFrame + 1) % 4;
                scale = 1.0 + 0.05 * Math.sin(animationFrame * Math.PI / 2);
                lastFrameTime = currentTime;
            }
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(animationFrame % 2 == 0 ? baseColor : baseColor.darker());
            int scaledSize = (int) (PLAYER_SIZE * scale);
            Ellipse2D circle = new Ellipse2D.Double(
                    x - scaledSize / 2, y - scaledSize / 2, scaledSize, scaledSize);
            g2d.fill(circle);
            g2d.setColor(Color.BLACK);
            g2d.draw(circle);
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public void setX(double x) { this.x = x; }
        public void setY(double y) { this.y = y; }
    }

    // Game panel for rendering
    private class GamePanel extends JPanel {
        public GamePanel() {
            setFocusable(true);
            requestFocusInWindow();
            setupInputHandling();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw background
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

            // Draw sumo ring
            g2d.setColor(new Color(194, 178, 128)); // Sandy brown
            Ellipse2D ring = new Ellipse2D.Double(
                    RING_CENTER_X - RING_RADIUS, RING_CENTER_Y - RING_RADIUS,
                    RING_RADIUS * 2, RING_RADIUS * 2);
            g2d.fill(ring);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(5));
            g2d.draw(ring);

            // Draw players
            for (Sprite sprite : sprites) {
                sprite.draw(g2d);
            }

            // Draw score and round
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Player 1: " + player1Score, 20, 30);
            g2d.drawString("Player 2: " + player2Score, WINDOW_WIDTH - 120, 30);
            g2d.drawString("Round: " + round + "/" + MAX_ROUNDS, WINDOW_WIDTH / 2 - 50, 30);

            // Draw message with fade effect
            if (!message.isEmpty() && messageAlpha > 0) {
                g2d.setColor(new Color(255, 0, 0, messageAlpha));
                g2d.setFont(new Font("Arial", Font.BOLD, 30));
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(message);
                g2d.drawString(message, (WINDOW_WIDTH - textWidth) / 2, WINDOW_HEIGHT / 2);
            }
        }

        private void setupInputHandling() {
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        // Player 1 controls (WASD)
                        case KeyEvent.VK_W: player1Up = true; break;
                        case KeyEvent.VK_S: player1Down = true; break;
                        case KeyEvent.VK_A: player1Left = true; break;
                        case KeyEvent.VK_D: player1Right = true; break;
                        // Player 2 controls (Arrow keys)
                        case KeyEvent.VK_UP: player2Up = true; break;
                        case KeyEvent.VK_DOWN: player2Down = true; break;
                        case KeyEvent.VK_LEFT: player2Left = true; break;
                        case KeyEvent.VK_RIGHT: player2Right = true; break;
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W: player1Up = false; break;
                        case KeyEvent.VK_S: player1Down = false; break;
                        case KeyEvent.VK_A: player1Left = false; break;
                        case KeyEvent.VK_D: player1Right = false; break;
                        case KeyEvent.VK_UP: player2Up = false; break;
                        case KeyEvent.VK_DOWN: player2Down = false; break;
                        case KeyEvent.VK_LEFT: player2Left = false; break;
                        case KeyEvent.VK_RIGHT: player2Right = false; break;
                    }
                }
            });
        }
    }

    public SumoWrestlingTournamentAWT() {
        // Set up window
        setTitle("Sumo Wrestling Tournament (AWT)");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Initialize game panel
        GamePanel gamePanel = new GamePanel();
        add(gamePanel);

        // Initialize sprites
        sprites = new ArrayList<>();
        sprites.add(new Sprite(player1X, player1Y, Color.BLUE)); // Player 1
        sprites.add(new Sprite(player2X, player2Y, Color.RED));  // Player 2

        // Initialize game timer
        gameTimer = new Timer(FRAME_TIME_MS, e -> {
            if (gameRunning && !roundOver) {
                updateGame();
            }
            gamePanel.repaint();
        });
        gameTimer.start();

        // Initialize round
        roundStartTime = System.currentTimeMillis();

        // Message fade timer
        Timer messageTimer = new Timer(50, e -> {
            if (!message.isEmpty() && messageAlpha > 0) {
                messageAlpha = Math.max(0, messageAlpha - 5);
                if (messageAlpha == 0) {
                    message = "";
                }
            }
            if (roundOver && System.currentTimeMillis() - roundStartTime > 3000) {
                resetRound();
            }
        });
        messageTimer.start();

        setVisible(true);
    }

    private void updateGame() {
        // Update player 1 position
        if (player1Up) player1Y -= PLAYER_SPEED;
        if (player1Down) player1Y += PLAYER_SPEED;
        if (player1Left) player1X -= PLAYER_SPEED;
        if (player1Right) player1X += PLAYER_SPEED;

        // Update player 2 position
        if (player2Up) player2Y -= PLAYER_SPEED;
        if (player2Down) player2Y += PLAYER_SPEED;
        if (player2Left) player2X -= PLAYER_SPEED;
        if (player2Right) player2X += PLAYER_SPEED;

        // Update sprite positions
        sprites.get(0).setX(player1X);
        sprites.get(0).setY(player1Y);
        sprites.get(1).setX(player2X);
        sprites.get(1).setY(player2Y);

        // Update animations
        for (Sprite sprite : sprites) {
            sprite.updateAnimation();
        }

        // Check collisions
        checkCollisions();

        // Check ring bounds
        checkRingBounds();
    }

    private void checkCollisions() {
        double dx = player1X - player2X;
        double dy = player1Y - player2Y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < PLAYER_SIZE) {
            double angle = Math.atan2(dy, dx);
            double pushX = Math.cos(angle) * PUSH_FORCE;
            double pushY = Math.sin(angle) * PUSH_FORCE;

            player1X += pushX;
            player1Y += pushY;
            player2X -= pushX;
            player2Y -= pushY;

            sprites.get(0).setX(player1X);
            sprites.get(0).setY(player1Y);
            sprites.get(1).setX(player2X);
            sprites.get(1).setY(player2Y);
        }
    }

    private void checkRingBounds() {
        double dist1 = Math.sqrt(Math.pow(player1X - RING_CENTER_X, 2) + Math.pow(player1Y - RING_CENTER_Y, 2));
        double dist2 = Math.sqrt(Math.pow(player2X - RING_CENTER_X, 2) + Math.pow(player2Y - RING_CENTER_Y, 2));

        if (dist1 > RING_RADIUS) {
            player2Score++;
            roundOver = true;
            message = "Player 2 Wins Round " + round + "!";
            messageAlpha = 255;
            roundStartTime = System.currentTimeMillis();
        } else if (dist2 > RING_RADIUS) {
            player1Score++;
            roundOver = true;
            message = "Player 1 Wins Round " + round + "!";
            messageAlpha = 255;
            roundStartTime = System.currentTimeMillis();
        }

        if (roundOver && round >= MAX_ROUNDS) {
            gameRunning = false;
            message = player1Score > player2Score ? "Player 1 Wins Tournament!" :
                    player2Score > player1Score ? "Player 2 Wins Tournament!" : "Tournament Draw!";
            messageAlpha = 255;
        }
    }

    private void resetRound() {
        if (round < MAX_ROUNDS) {
            round++;
            player1X = RING_CENTER_X - 100;
            player1Y = RING_CENTER_Y;
            player2X = RING_CENTER_X + 100;
            player2Y = RING_CENTER_Y;
            sprites.get(0).setX(player1X);
            sprites.get(0).setY(player1Y);
            sprites.get(1).setX(player2X);
            sprites.get(1).setY(player2Y);
            roundOver = false;
            message = "Round " + round + ": Fight!";
            messageAlpha = 255;
            roundStartTime = System.currentTimeMillis();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SumoWrestlingTournamentAWT());
    }
}