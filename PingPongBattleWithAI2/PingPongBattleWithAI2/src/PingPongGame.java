// Main Game Class
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class PingPongGame extends JFrame {

    private GamePanel gamePanel;
    private final int GAME_WIDTH = 800;
    private final int GAME_HEIGHT = 600;
    private final int PADDLE_SPEED = 5; // Pixels per frame
    private final int BALL_START_SPEED = 4; // Pixels per frame
    private final int GAME_TICK_DELAY = 1000 / 60; // ~60 FPS

    public PingPongGame() {
        setTitle("Ping-Pong Battle with AI");
        setSize(GAME_WIDTH, GAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null); // Center the window

        gamePanel = new GamePanel();
        add(gamePanel);

        // Add KeyListener for player input
        addKeyListener(new GameKeyListener());
        setFocusable(true); // Ensure frame can receive key events

        // Game loop using Swing Timer
        Timer gameTimer = new Timer(GAME_TICK_DELAY, new GameLoopListener());
        gameTimer.start();

        setVisible(true);
    }

    public static void main(String[] args) {
        // Run on the Event Dispatch Thread for Swing applications
        SwingUtilities.invokeLater(() -> new PingPongGame());
    }

    // --- Inner Classes for Game Logic ---

    // Game Panel where drawing occurs
    private class GamePanel extends JPanel {
        private Ball ball;
        private Paddle playerPaddle;
        private Paddle aiPaddle;
        private int playerScore;
        private int aiScore;
        private GameState currentGameState;

        public GamePanel() {
            setBackground(Color.BLACK);
            resetGame();
        }

        private void resetGame() {
            // Initial positions and states
            ball = new Ball(GAME_WIDTH / 2, GAME_HEIGHT / 2, 10, BALL_START_SPEED);
            playerPaddle = new Paddle(50, GAME_HEIGHT / 2 - 40, 15, 80, PADDLE_SPEED);
            aiPaddle = new Paddle(GAME_WIDTH - 65, GAME_HEIGHT / 2 - 40, 15, 80, PADDLE_SPEED);
            playerScore = 0;
            aiScore = 0;
            currentGameState = GameState.RUNNING;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            // Draw game elements
            ball.draw(g2d);
            playerPaddle.draw(g2d);
            aiPaddle.draw(g2d);

            // Draw scores
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 30));
            g2d.drawString("Player: " + playerScore, GAME_WIDTH / 4, 50);
            g2d.drawString("AI: " + aiScore, 3 * GAME_WIDTH / 4 - 50, 50);

            // Draw game over/paused messages
            if (currentGameState == GameState.GAME_OVER) {
                g2d.setFont(new Font("Arial", Font.BOLD, 50));
                g2d.drawString("GAME OVER!", GAME_WIDTH / 2 - 150, GAME_HEIGHT / 2);
            } else if (currentGameState == GameState.PAUSED) {
                g2d.setFont(new Font("Arial", Font.BOLD, 50));
                g2d.drawString("PAUSED", GAME_WIDTH / 2 - 100, GAME_HEIGHT / 2);
            }
        }

        public void updateGame() {
            if (currentGameState != GameState.RUNNING) {
                return;
            }

            ball.move();
            handleCollisions();
            aiPaddle.moveAI(ball, GAME_HEIGHT); // AI movement
            checkScore();
            repaint(); // Request a repaint
        }

        private void handleCollisions() {
            // Wall collisions (top/bottom)
            if (ball.getY() - ball.getRadius() < 0 || ball.getY() + ball.getRadius() > GAME_HEIGHT) {
                ball.reverseYDirection();
            }

            // Paddle collisions
            // Player Paddle
            if (ball.getBounds().intersects(playerPaddle.getBounds())) {
                ball.reverseXDirection();
                // Basic angle change based on where it hits the paddle
                ball.setDy((ball.getY() - (playerPaddle.getY() + playerPaddle.getHeight() / 2)) * 0.1);
            }

            // AI Paddle
            if (ball.getBounds().intersects(aiPaddle.getBounds())) {
                ball.reverseXDirection();
                // Basic angle change based on where it hits the paddle
                ball.setDy((ball.getY() - (aiPaddle.getY() + aiPaddle.getHeight() / 2)) * 0.1);
            }
        }

        private void checkScore() {
            if (ball.getX() - ball.getRadius() < 0) { // Ball went past player paddle
                aiScore++;
                resetBallPosition();
                if (aiScore >= 5) currentGameState = GameState.GAME_OVER; // Example end condition
            } else if (ball.getX() + ball.getRadius() > GAME_WIDTH) { // Ball went past AI paddle
                playerScore++;
                resetBallPosition();
                if (playerScore >= 5) currentGameState = GameState.GAME_OVER; // Example end condition
            }
        }

        private void resetBallPosition() {
            ball.setX(GAME_WIDTH / 2);
            ball.setY(GAME_HEIGHT / 2);
            // Randomize initial direction slightly
            ball.setDx(BALL_START_SPEED * (Math.random() > 0.5 ? 1 : -1));
            ball.setDy(BALL_START_SPEED * (Math.random() > 0.5 ? 1 : -1));
        }

        public Paddle getPlayerPaddle() {
            return playerPaddle;
        }

        public GameState getGameState() {
            return currentGameState;
        }

        public void togglePause() {
            if (currentGameState == GameState.RUNNING) {
                currentGameState = GameState.PAUSED;
            } else if (currentGameState == GameState.PAUSED) {
                currentGameState = GameState.RUNNING;
            }
        }
    }

    // Game Objects
    private class Ball {
        private double x, y;
        private double dx, dy; // Direction and speed
        private int radius;

        public Ball(double x, double y, int radius, double initialSpeed) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.dx = initialSpeed;
            this.dy = initialSpeed; // Start with some vertical movement
        }

        public void move() {
            x += dx;
            y += dy;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int) (x - radius), (int) (y - radius), radius * 2, radius * 2);
        }

        public void reverseXDirection() {
            dx *= -1;
        }

        public void reverseYDirection() {
            dy *= -1;
        }

        // Getters for collision detection
        public double getX() { return x; }
        public double getY() { return y; }
        public int getRadius() { return radius; }

        public void setX(double x) { this.x = x; }
        public void setY(double y) { this.y = y; }
        public void setDx(double dx) { this.dx = dx; }
        public void setDy(double dy) { this.dy = dy; }

        public Rectangle getBounds() {
            return new Rectangle((int) (x - radius), (int) (y - radius), radius * 2, radius * 2);
        }
    }

    private class Paddle {
        private int x, y;
        private int width, height;
        private int speed;

        public Paddle(int x, int y, int width, int height, int speed) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.speed = speed;
        }

        public void moveUp() {
            y = Math.max(0, y - speed);
        }

        public void moveDown() {
            y = Math.min(GAME_HEIGHT - height, y + speed);
        }

        public void moveAI(Ball ball, int gameHeight) {
            // Simple AI: Follow the ball's Y position
            if (ball.getY() < y + height / 2) {
                moveUp();
            } else if (ball.getY() > y + height / 2) {
                moveDown();
            }
            // Add some "imperfect" AI by not always moving perfectly
            // (e.g., occasional delays, or not always reaching perfectly center)
            // For a more advanced AI, you'd predict ball trajectory
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(x, y, width, height);
        }

        // Getters for collision detection
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    // Game States
    public enum GameState {
        RUNNING, PAUSED, GAME_OVER
    }

    // Game Loop Listener (for Swing Timer)
    private class GameLoopListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            gamePanel.updateGame();
        }
    }

    // Key Listener for Player Input
    private class GameKeyListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {
            // Not used for continuous movement
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (gamePanel.getGameState() == GameState.RUNNING) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    gamePanel.getPlayerPaddle().moveUp();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    gamePanel.getPlayerPaddle().moveDown();
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_P) {
                gamePanel.togglePause();
            }
            if (e.getKeyCode() == KeyEvent.VK_R && gamePanel.getGameState() == GameState.GAME_OVER) {
                // Restart game (you'd need to add a reset method to GamePanel)
                // gamePanel.resetGame(); // This would need to be implemented
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            // Not used for continuous movement in this simple example
            // For smoother movement, you might track pressed keys and move in the game loop
        }
    }
}