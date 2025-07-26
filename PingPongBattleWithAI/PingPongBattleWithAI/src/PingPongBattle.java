import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import javax.sound.sampled.*;
import java.util.Random;

// Main game class extending JFrame for the window
public class PingPongBattle extends JFrame {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final String TITLE = "Ping-Pong Battle with AI";
    private GamePanel gamePanel;
    private boolean isGameRunning = false;

    public PingPongBattle() {
        setTitle(TITLE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        gamePanel = new GamePanel();
        add(gamePanel);
        setVisible(true);
    }

    // Main game panel handling rendering and game logic
    class GamePanel extends JPanel implements ActionListener, KeyListener {
        private static final int PADDLE_WIDTH = 15;
        private static final int PADDLE_HEIGHT = 80;
        private static final int BALL_SIZE = 20;
        private static final int PADDLE_SPEED = 5;
        private static final int BALL_SPEED = 7;
        private static final int AI_REACTION_DELAY = 2;
        private static final int FPS = 60;
        private static final int DELAY = 1000 / FPS;

        private Rectangle2D playerPaddle;
        private Rectangle2D aiPaddle;
        private Ellipse2D ball;
        private double ballDx, ballDy;
        private int playerScore, aiScore;
        private boolean gameStarted;
        private boolean gameOver;
        private Timer timer;
        private Random random;
        private Clip hitSound, scoreSound, wallSound;
        private int aiReactionCounter;
        private int winningScore = 5;
        private Color backgroundColor = Color.BLACK;
        private Color paddleColor = Color.WHITE;
        private Color ballColor = Color.RED;
        private Color textColor = Color.YELLOW;
        private Font scoreFont = new Font("Arial", Font.BOLD, 24);
        private Font menuFont = new Font("Arial", Font.BOLD, 36);
        private Font smallFont = new Font("Arial", Font.PLAIN, 18);

        public GamePanel() {
            setBackground(backgroundColor);
            setFocusable(true);
            addKeyListener(this);
            random = new Random();
            resetGame();
            loadSounds();
            timer = new Timer(DELAY, this);
            timer.start();
        }

        // Initialize or reset game state
        private void resetGame() {
            playerPaddle = new Rectangle2D.Double(50, WINDOW_HEIGHT / 2 - PADDLE_HEIGHT / 2, PADDLE_WIDTH, PADDLE_HEIGHT);
            aiPaddle = new Rectangle2D.Double(WINDOW_WIDTH - 50 - PADDLE_WIDTH, WINDOW_HEIGHT / 2 - PADDLE_HEIGHT / 2, PADDLE_WIDTH, PADDLE_HEIGHT);
            ball = new Ellipse2D.Double(WINDOW_WIDTH / 2 - BALL_SIZE / 2, WINDOW_HEIGHT / 2 - BALL_SIZE / 2, BALL_SIZE, BALL_SIZE);
            resetBall();
            playerScore = 0;
            aiScore = 0;
            gameStarted = false;
            gameOver = false;
            aiReactionCounter = 0;
        }

        // Reset ball position and direction
        private void resetBall() {
            ball.setFrame(WINDOW_WIDTH / 2 - BALL_SIZE / 2, WINDOW_HEIGHT / 2 - BALL_SIZE / 2, BALL_SIZE, BALL_SIZE);
            double angle = random.nextDouble() * Math.PI / 2 - Math.PI / 4;
            ballDx = BALL_SPEED * Math.cos(angle);
            ballDy = BALL_SPEED * Math.sin(angle);
            if (random.nextBoolean()) ballDx = -ballDx;
        }

        // Load sound effects
        private void loadSounds() {
            try {
                hitSound = loadClip("/paddle_hit.wav");
                scoreSound = loadClip("/score.wav");
                wallSound = loadClip("/wall_hit.wav");
            } catch (Exception e) {
                System.out.println("Error loading sounds: " + e.getMessage());
            }
        }

        private Clip loadClip(String path) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(getClass().getResource(path));
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            return clip;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw center line
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10}, 0));
            g2d.drawLine(WINDOW_WIDTH / 2, 0, WINDOW_WIDTH / 2, WINDOW_HEIGHT);

            // Draw paddles and ball
            g2d.setColor(paddleColor);
            g2d.fill(playerPaddle);
            g2d.fill(aiPaddle);
            g2d.setColor(ballColor);
            g2d.fill(ball);

            // Draw scores
            g2d.setColor(textColor);
            g2d.setFont(scoreFont);
            g2d.drawString("Player: " + playerScore, 50, 50);
            g2d.drawString("AI: " + aiScore, WINDOW_WIDTH - 150, 50);

            // Draw menu or game over screen
            if (!gameStarted && !gameOver) {
                drawMenu(g2d);
            } else if (gameOver) {
                drawGameOver(g2d);
            }
        }

        private void drawMenu(Graphics2D g2d) {
            g2d.setColor(textColor);
            g2d.setFont(menuFont);
            String title = "Ping-Pong Battle";
            int titleWidth = g2d.getFontMetrics().stringWidth(title);
            g2d.drawString(title, (WINDOW_WIDTH - titleWidth) / 2, WINDOW_HEIGHT / 3);
            g2d.setFont(smallFont);
            String start = "Press SPACE to Start";
            int startWidth = g2d.getFontMetrics().stringWidth(start);
            g2d.drawString(start, (WINDOW_WIDTH - startWidth) / 2, WINDOW_HEIGHT / 2);
        }

        private void drawGameOver(Graphics2D g2d) {
            g2d.setColor(textColor);
            g2d.setFont(menuFont);
            String message = playerScore >= winningScore ? "Player Wins!" : "AI Wins!";
            int msgWidth = g2d.getFontMetrics().stringWidth(message);
            g2d.drawString(message, (WINDOW_WIDTH - msgWidth) / 2, WINDOW_HEIGHT / 3);
            g2d.setFont(smallFont);
            String restart = "Press SPACE to Restart";
            int restartWidth = g2d.getFontMetrics().stringWidth(restart);
            g2d.drawString(restart, (WINDOW_WIDTH - restartWidth) / 2, WINDOW_HEIGHT / 2);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameStarted && !gameOver) {
                updateGame();
            }
            repaint();
        }

        private void updateGame() {
            // Update ball position
            ball.setFrame(ball.getX() + ballDx, ball.getY() + ballDy, BALL_SIZE, BALL_SIZE);

            // Ball collision with top and bottom
            if (ball.getY() <= 0 || ball.getY() >= WINDOW_HEIGHT - BALL_SIZE) {
                ballDy = -ballDy;
                playSound(wallSound);
            }

            // Ball collision with paddles
            if (ball.intersects(playerPaddle) || ball.intersects(aiPaddle)) {
                ballDx = -ballDx * 1.05; // Slight speed increase
                ballDy += random.nextDouble() * 0.5 - 0.25; // Add spin
                playSound(hitSound);
            }

            // Ball out of bounds
            if (ball.getX() <= 0) {
                aiScore++;
                playSound(scoreSound);
                resetBall();
                checkGameOver();
            } else if (ball.getX() >= WINDOW_WIDTH - BALL_SIZE) {
                playerScore++;
                playSound(scoreSound);
                resetBall();
                checkGameOver();
            }

            // AI movement
            updateAI();
        }

        private void updateAI() {
            aiReactionCounter++;
            if (aiReactionCounter >= AI_REACTION_DELAY) {
                double aiCenter = aiPaddle.getY() + PADDLE_HEIGHT / 2;
                double ballCenter = ball.getY() + BALL_SIZE / 2;
                double error = random.nextDouble() * 20 - 10; // AI imperfection
                if (ballCenter + error < aiCenter - 10) {
                    aiPaddle.setRect(aiPaddle.getX(), Math.max(0, aiPaddle.getY() - PADDLE_SPEED), PADDLE_WIDTH, PADDLE_HEIGHT);
                } else if (ballCenter + error > aiCenter + 10) {
                    aiPaddle.setRect(aiPaddle.getX(), Math.min(WINDOW_HEIGHT - PADDLE_HEIGHT, aiPaddle.getY() + PADDLE_SPEED), PADDLE_WIDTH, PADDLE_HEIGHT);
                }
                aiReactionCounter = 0;
            }
        }

        private void checkGameOver() {
            if (playerScore >= winningScore || aiScore >= winningScore) {
                gameStarted = false;
                gameOver = true;
            }
        }

        private void playSound(Clip clip) {
            if (clip != null) {
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (!gameStarted && !gameOver) {
                    gameStarted = true;
                } else if (gameOver) {
                    resetGame();
                    gameStarted = true;
                }
            }
            if (gameStarted && !gameOver) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    playerPaddle.setRect(playerPaddle.getX(), Math.max(0, playerPaddle.getY() - PADDLE_SPEED), PADDLE_WIDTH, PADDLE_HEIGHT);
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    playerPaddle.setRect(playerPaddle.getX(), Math.min(WINDOW_HEIGHT - PADDLE_HEIGHT, playerPaddle.getY() + PADDLE_SPEED), PADDLE_WIDTH, PADDLE_HEIGHT);
                }
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {}
        @Override
        public void keyReleased(KeyEvent e) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PingPongBattle());
    }
}