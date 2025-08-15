import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.Timer;
import java.awt.Image;
import java.awt.Toolkit;

public class GamePanel extends JPanel implements Runnable {

    // --- Game Loop Variables ---
    private Thread gameThread;
    private volatile boolean running = false;
    private final int FPS = 60; // Frames per second
    private final long TARGET_TIME_PER_FRAME = 1000 / FPS;

    // --- Game Objects ---
    private Player player;
    private Ball ball;
    private Bat bat;
    private Scoreboard scoreboard;

    // --- Animation & Rendering ---
    private Image offscreenBuffer;
    private Graphics offscreenGraphics;
    private Image background;

    // --- Game State ---
    private enum GameState { READY, PITCHING, SWINGING, GAME_OVER };
    private GameState currentState;

    public GamePanel() {
        this.setPreferredSize(new Dimension(800, 600)); // Set preferred size
        this.setFocusable(true);
        this.addKeyListener(new GameKeyAdapter());
        this.setBackground(Color.BLACK); // A default background color

        // Load assets
        loadAssets();

        // Initialize game objects
        player = new Player(100, 300); // Example initial position
        ball = new Ball();
        bat = new Bat(player.getX(), player.getY());
        scoreboard = new Scoreboard();

        currentState = GameState.READY;
    }

    // --- Initialization & Asset Loading ---
    private void loadAssets() {
        // Here you would load images for the player, ball, bat, and background
        // For example:
        // background = new ImageIcon("assets/baseball_field.png").getImage();
        background = Toolkit.getDefaultToolkit().getImage("assets/baseball_field.png");
        // And you would load other sprites as well
    }

    // --- Game Loop Thread Management ---
    public void startGameThread() {
        if (!running) {
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    public void stopGameThread() {
        running = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        long timer = System.currentTimeMillis();
        int frames = 0;

        while (running) {
            long now = System.nanoTime();
            long delta = (now - lastTime) / 1000000; // time in milliseconds
            lastTime = now;

            // --- Game Logic Updates ---
            if (currentState == GameState.PITCHING) {
                ball.update();
                bat.update(); // Update bat position to follow player

                // Check if the ball is close enough to be swung at
                if (ball.isHittable()) {
                    // Logic for a pitch coming in
                    // ...
                }
            } else if (currentState == GameState.SWINGING) {
                bat.swing(); // Start the swing animation
                // Check for collision after the swing
                if (bat.checkCollision(ball)) {
                    // Logic for a hit, foul, or miss
                    // ball.hit();
                    scoreboard.addHit();
                    // ...
                }

                // Check if the swing animation is over
                if (bat.isSwingFinished()) {
                    currentState = GameState.READY; // Or back to pitching
                }
            }

            // --- Rendering ---
            repaint();

            frames++;
            if (System.currentTimeMillis() - timer > 1000) {
                System.out.println("FPS: " + frames);
                timer += 1000;
                frames = 0;
            }

            // --- Frame Rate Control (for a smooth experience) ---
            long sleepTime = TARGET_TIME_PER_FRAME - delta;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // --- Double Buffering ---
        if (offscreenBuffer == null) {
            offscreenBuffer = createImage(getWidth(), getHeight());
            offscreenGraphics = offscreenBuffer.getGraphics();
        }

        // Clear the buffer
        offscreenGraphics.setColor(Color.WHITE);
        offscreenGraphics.fillRect(0, 0, getWidth(), getHeight());

        // Draw all game objects onto the off-screen buffer
        draw(offscreenGraphics);

        // Draw the buffer to the screen
        g.drawImage(offscreenBuffer, 0, 0, this);
    }

    private void draw(Graphics g) {
        // Draw the background
        g.drawImage(background, 0, 0, getWidth(), getHeight(), this);

        // Draw the player
        player.draw(g);

        // Draw the bat (if swinging or ready)
        if (bat != null) {
            bat.draw(g);
        }

        // Draw the ball (if a pitch is in progress)
        if (ball != null && currentState != GameState.READY) {
            ball.draw(g);
        }

        // Draw the scoreboard
        scoreboard.draw(g);

        // Display game messages (e.g., "Press Space to Swing!")
        g.setColor(Color.WHITE);
        g.drawString("State: " + currentState, 10, 20);
    }

    // --- Keyboard Input Handling ---
    private class GameKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (currentState == GameState.PITCHING) {
                    currentState = GameState.SWINGING;
                    // Trigger the bat swing logic
                    bat.startSwing();
                }
            } else if (e.getKeyCode() == KeyEvent.VK_P) {
                // For a new pitch (developer mode or a simple game start)
                if (currentState == GameState.READY || currentState == GameState.SWINGING) {
                    ball.startNewPitch();
                    currentState = GameState.PITCHING;
                }
            }
        }
    }
}