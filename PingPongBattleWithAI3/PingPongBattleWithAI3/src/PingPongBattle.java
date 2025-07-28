import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import javax.sound.sampled.*;
import java.io.*;

public class PingPongBattle extends JFrame {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PADDLE_WIDTH = 15;
    private static final int PADDLE_HEIGHT = 100;
    private static final int BALL_SIZE = 20;
    private static final int PADDLE_SPEED = 8;
    private static final int AI_PADDLE_SPEED = 6;
    private static final int BALL_SPEED = 5;
    private static final int WINNING_SCORE = 5;

    private int playerScore = 0;
    private int aiScore = 0;
    private int playerY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private int aiY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private int ballX = WIDTH / 2 - BALL_SIZE / 2;
    private int ballY = HEIGHT / 2 - BALL_SIZE / 2;
    private int ballSpeedX = BALL_SPEED;
    private int ballSpeedY = BALL_SPEED;

    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean gamePaused = false;
    private boolean gameStarted = false;
    private boolean gameOver = false;

    private Random random = new Random();
    private Clip paddleSound;
    private Clip wallSound;
    private Clip scoreSound;
    private Clip gameOverSound;

    private Color[] trailColors = new Color[10];
    private int[] trailX = new int[10];
    private int[] trailY = new int[10];
    private int trailIndex = 0;

    private Color backgroundColor = new Color(0, 0, 30);
    private Color paddleColor = new Color(100, 255, 100);
    private Color aiPaddleColor = new Color(255, 100, 100);
    private Color ballColor = new Color(255, 255, 255);
    private Color textColor = new Color(255, 255, 255);

    public PingPongBattle() {
        setTitle("Ping-Pong Battle with AI");
        setSize(WIDTH, HEIGHT);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize trail colors
        for (int i = 0; i < trailColors.length; i++) {
            trailColors[i] = new Color(255, 255, 255, 25 * i);
        }

        // Load sounds
        try {
            paddleSound = AudioSystem.getClip();
            wallSound = AudioSystem.getClip();
            scoreSound = AudioSystem.getClip();
            gameOverSound = AudioSystem.getClip();

            loadSound(paddleSound, "paddle.wav");
            loadSound(wallSound, "wall.wav");
            loadSound(scoreSound, "score.wav");
            loadSound(gameOverSound, "gameover.wav");
        } catch (Exception e) {
            System.out.println("Error loading sounds: " + e.getMessage());
        }

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                handleKeyRelease(e);
            }
        });

        // Game loop
        new Thread(() -> {
            while (true) {
                if (!gamePaused && gameStarted && !gameOver) {
                    updateGame();
                }
                repaint();
                try {
                    Thread.sleep(16); // ~60 FPS
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void loadSound(Clip clip, String filename) throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/" + filename);
        if (inputStream == null) {
            // Try loading from file system if not found in resources
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filename));
            clip.open(audioInputStream);
        } else {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
            clip.open(audioInputStream);
        }
    }

    private void playSound(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    private void handleKeyPress(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                upPressed = true;
                break;
            case KeyEvent.VK_DOWN:
                downPressed = true;
                break;
            case KeyEvent.VK_SPACE:
                if (!gameStarted) {
                    gameStarted = true;
                } else if (gameOver) {
                    resetGame();
                } else {
                    gamePaused = !gamePaused;
                }
                break;
            case KeyEvent.VK_R:
                resetGame();
                break;
            case KeyEvent.VK_ESCAPE:
                System.exit(0);
                break;
        }
    }

    private void handleKeyRelease(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                upPressed = false;
                break;
            case KeyEvent.VK_DOWN:
                downPressed = false;
                break;
        }
    }

    private void updateGame() {
        // Update player paddle position
        if (upPressed && playerY > 0) {
            playerY -= PADDLE_SPEED;
        }
        if (downPressed && playerY < HEIGHT - PADDLE_HEIGHT) {
            playerY += PADDLE_SPEED;
        }

        // Update AI paddle position (with some imperfection)
        int aiTargetY = ballY - PADDLE_HEIGHT / 2;
        if (aiY + PADDLE_HEIGHT / 2 < aiTargetY) {
            // Add some randomness to AI performance
            if (random.nextInt(10) > 2) {
                aiY += AI_PADDLE_SPEED;
            }
        } else if (aiY + PADDLE_HEIGHT / 2 > aiTargetY) {
            if (random.nextInt(10) > 2) {
                aiY -= AI_PADDLE_SPEED;
            }
        }

        // Keep AI paddle within bounds
        if (aiY < 0) aiY = 0;
        if (aiY > HEIGHT - PADDLE_HEIGHT) aiY = HEIGHT - PADDLE_HEIGHT;

        // Update ball position
        ballX += ballSpeedX;
        ballY += ballSpeedY;

        // Add ball position to trail
        trailX[trailIndex] = ballX;
        trailY[trailIndex] = ballY;
        trailIndex = (trailIndex + 1) % trailX.length;

        // Ball collision with top and bottom walls
        if (ballY <= 0 || ballY >= HEIGHT - BALL_SIZE) {
            ballSpeedY = -ballSpeedY;
            playSound(wallSound);

            // Add some randomness to bounce
            if (random.nextBoolean()) {
                ballSpeedY += random.nextInt(2) - 1;
            }
        }

        // Ball collision with player paddle
        if (ballX <= PADDLE_WIDTH &&
                ballY + BALL_SIZE >= playerY &&
                ballY <= playerY + PADDLE_HEIGHT) {

            ballSpeedX = Math.abs(ballSpeedX);

            // Add angle based on where ball hits paddle
            double relativeIntersectY = (playerY + (PADDLE_HEIGHT / 2)) - (ballY + (BALL_SIZE / 2));
            double normalizedRelativeIntersectionY = relativeIntersectY / (PADDLE_HEIGHT / 2);
            double bounceAngle = normalizedRelativeIntersectionY * (Math.PI / 4);

            ballSpeedX = (int) (BALL_SPEED * Math.cos(bounceAngle));
            ballSpeedY = (int) (-BALL_SPEED * Math.sin(bounceAngle));

            // Increase speed slightly
            ballSpeedX *= 1.05;
            ballSpeedY *= 1.05;

            playSound(paddleSound);
        }

        // Ball collision with AI paddle
        if (ballX + BALL_SIZE >= WIDTH - PADDLE_WIDTH &&
                ballY + BALL_SIZE >= aiY &&
                ballY <= aiY + PADDLE_HEIGHT) {

            ballSpeedX = -Math.abs(ballSpeedX);

            // Add angle based on where ball hits paddle
            double relativeIntersectY = (aiY + (PADDLE_HEIGHT / 2)) - (ballY + (BALL_SIZE / 2));
            double normalizedRelativeIntersectionY = relativeIntersectY / (PADDLE_HEIGHT / 2);
            double bounceAngle = normalizedRelativeIntersectionY * (Math.PI / 4);

            ballSpeedX = (int) (-BALL_SPEED * Math.cos(bounceAngle));
            ballSpeedY = (int) (-BALL_SPEED * Math.sin(bounceAngle));

            playSound(paddleSound);
        }

        // Ball out of bounds - scoring
        if (ballX < 0) {
            aiScore++;
            playSound(scoreSound);
            resetBall();
            if (aiScore >= WINNING_SCORE) {
                gameOver = true;
                playSound(gameOverSound);
            }
        } else if (ballX > WIDTH) {
            playerScore++;
            playSound(scoreSound);
            resetBall();
            if (playerScore >= WINNING_SCORE) {
                gameOver = true;
                playSound(gameOverSound);
            }
        }
    }

    private void resetBall() {
        ballX = WIDTH / 2 - BALL_SIZE / 2;
        ballY = HEIGHT / 2 - BALL_SIZE / 2;

        // Random initial direction
        ballSpeedX = BALL_SPEED * (random.nextBoolean() ? 1 : -1);
        ballSpeedY = BALL_SPEED * (random.nextBoolean() ? 1 : -1);

        // Clear trail
        for (int i = 0; i < trailX.length; i++) {
            trailX[i] = ballX;
            trailY[i] = ballY;
        }

        gamePaused = true;
        if (!gameOver) {
            new Timer(1000, e -> {
                gamePaused = false;
                ((Timer)e.getSource()).stop();
            }).start();
        }
    }

    private void resetGame() {
        playerScore = 0;
        aiScore = 0;
        playerY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
        aiY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
        gameOver = false;
        gameStarted = false;
        resetBall();
    }

    @Override
    public void paint(Graphics g) {
        // Double buffering
        Image buffer = createImage(getWidth(), getHeight());
        Graphics bufferGraphics = buffer.getGraphics();
        drawGame(bufferGraphics);
        g.drawImage(buffer, 0, 0, this);
    }

    private void drawGame(Graphics g) {
        // Draw background
        g.setColor(backgroundColor);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw center line
        g.setColor(new Color(255, 255, 255, 50));
        for (int i = 0; i < HEIGHT; i += 20) {
            g.fillRect(WIDTH / 2 - 1, i, 2, 10);
        }

        // Draw ball trail
        for (int i = 0; i < trailX.length; i++) {
            int index = (trailIndex + i) % trailX.length;
            g.setColor(trailColors[i]);
            g.fillOval(trailX[index], trailY[index], BALL_SIZE, BALL_SIZE);
        }

        // Draw paddles
        g.setColor(paddleColor);
        g.fillRect(0, playerY, PADDLE_WIDTH, PADDLE_HEIGHT);

        g.setColor(aiPaddleColor);
        g.fillRect(WIDTH - PADDLE_WIDTH, aiY, PADDLE_WIDTH, PADDLE_HEIGHT);

        // Draw ball
        g.setColor(ballColor);
        g.fillOval(ballX, ballY, BALL_SIZE, BALL_SIZE);

        // Draw scores
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(textColor);
        g.drawString(String.valueOf(playerScore), WIDTH / 4, 50);
        g.drawString(String.valueOf(aiScore), 3 * WIDTH / 4, 50);

        // Draw game messages
        g.setFont(new Font("Arial", Font.BOLD, 30));
        if (!gameStarted) {
            drawCenteredString(g, "PING-PONG BATTLE WITH AI", WIDTH / 2, HEIGHT / 2 - 60);
            drawCenteredString(g, "Press SPACE to Start", WIDTH / 2, HEIGHT / 2);
            drawCenteredString(g, "Use UP/DOWN arrows to move", WIDTH / 2, HEIGHT / 2 + 40);
            drawCenteredString(g, "ESC to Quit | R to Reset", WIDTH / 2, HEIGHT / 2 + 80);
        } else if (gamePaused && !gameOver) {
            drawCenteredString(g, "PAUSED", WIDTH / 2, HEIGHT / 2);
            drawCenteredString(g, "Press SPACE to Resume", WIDTH / 2, HEIGHT / 2 + 40);
        } else if (gameOver) {
            String winner = playerScore > aiScore ? "PLAYER WINS!" : "AI WINS!";
            drawCenteredString(g, winner, WIDTH / 2, HEIGHT / 2 - 40);
            drawCenteredString(g, "Final Score: " + playerScore + " - " + aiScore, WIDTH / 2, HEIGHT / 2);
            drawCenteredString(g, "Press SPACE to Play Again", WIDTH / 2, HEIGHT / 2 + 40);
            drawCenteredString(g, "ESC to Quit", WIDTH / 2, HEIGHT / 2 + 80);
        }
    }

    private void drawCenteredString(Graphics g, String text, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g.drawString(text, x - textWidth / 2, y);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PingPongBattle game = new PingPongBattle();
            game.setVisible(true);
        });
    }
}