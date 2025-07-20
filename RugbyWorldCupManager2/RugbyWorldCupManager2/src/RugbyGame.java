import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// --- GamePanel: Where the game is drawn and updated ---
class GamePanel extends JPanel implements ActionListener, KeyListener {

    // Constants for game dimensions and speeds
    private final int WIDTH = 800;
    private final int HEIGHT = 600;
    private final int PLAYER_SIZE = 50;
    private final int BALL_SIZE = 20;
    private final int PLAYER_SPEED = 5;
    private final int GAME_SPEED = 15; // Milliseconds per frame update

    private Timer gameTimer;
    private Player player;
    private Ball ball;
    private List<Opponent> opponents;
    private int score;
    private boolean gameOver;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(0, 100, 0)); // Rugby pitch green
        setFocusable(true);
        addKeyListener(this);

        initGame();
    }

    private void initGame() {
        player = new Player(WIDTH / 2 - PLAYER_SIZE / 2, HEIGHT - PLAYER_SIZE - 20, PLAYER_SIZE);
        ball = new Ball(WIDTH / 2 - BALL_SIZE / 2, HEIGHT / 2 - BALL_SIZE / 2, BALL_SIZE);
        opponents = new ArrayList<>();
        spawnOpponents(3); // Start with 3 opponents
        score = 0;
        gameOver = false;

        gameTimer = new Timer(GAME_SPEED, this);
        gameTimer.start();
    }

    private void spawnOpponents(int count) {
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            int x = rand.nextInt(WIDTH - PLAYER_SIZE);
            int y = rand.nextInt(HEIGHT / 2 - PLAYER_SIZE); // Spawn in upper half
            opponents.add(new Opponent(x, y, PLAYER_SIZE, rand.nextInt(3) + 2)); // Random speed
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Clears the panel

        Graphics2D g2d = (Graphics2D) g;

        // Draw pitch lines (simplified)
        g2d.setColor(Color.WHITE);
        g2d.drawRect(50, 50, WIDTH - 100, HEIGHT - 100); // Outer lines
        g2d.drawLine(50, HEIGHT / 2, WIDTH - 50, HEIGHT / 2); // Halfway line

        // Draw player
        player.draw(g2d);

        // Draw ball
        ball.draw(g2d);

        // Draw opponents
        for (Opponent opp : opponents) {
            opp.draw(g2d);
        }

        // Draw score
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("Score: " + score, 10, 30);

        if (gameOver) {
            drawGameOver(g2d);
        }

        Toolkit.getDefaultToolkit().sync(); // Ensures smooth animation
    }

    private void drawGameOver(Graphics2D g2d) {
        String msg = "Game Over! Your Score: " + score;
        String restartMsg = "Press R to Restart";
        Font font = new Font("Arial", Font.BOLD, 40);
        FontMetrics metr = getFontMetrics(font);

        g2d.setColor(Color.RED);
        g2d.setFont(font);
        g2d.drawString(msg, (WIDTH - metr.stringWidth(msg)) / 2, HEIGHT / 2 - 20);

        Font smallFont = new Font("Arial", Font.BOLD, 20);
        FontMetrics smallMetr = getFontMetrics(smallFont);
        g2d.setColor(Color.YELLOW);
        g2d.setFont(smallFont);
        g2d.drawString(restartMsg, (WIDTH - smallMetr.stringWidth(restartMsg)) / 2, HEIGHT / 2 + 30);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) {
            return;
        }

        updateGame();
        repaint(); // Request repaint to show updated positions
    }

    private void updateGame() {
        player.update(WIDTH, HEIGHT);
        ball.update();

        // Ball collision with player (simple touch)
        if (player.getBounds().intersects(ball.getBounds())) {
            // Player "picks up" the ball (for simplicity, ball follows player)
            ball.setX(player.getX() + PLAYER_SIZE / 2 - BALL_SIZE / 2);
            ball.setY(player.getY() + PLAYER_SIZE / 2 - BALL_SIZE / 2);
        }

        for (Opponent opp : opponents) {
            opp.update(WIDTH, HEIGHT, ball); // Opponents might try to intercept the ball

            // Collision between player and opponent
            if (player.getBounds().intersects(opp.getBounds())) {
                gameOver = true;
                gameTimer.stop();
            }

            // Ball collision with opponent (opponent takes ball)
            if (opp.getBounds().intersects(ball.getBounds())) {
                // Opponent "takes" the ball, game over for player
                gameOver = true;
                gameTimer.stop();
            }
        }

        // Win condition (e.g., reaching top of the screen with the ball)
        if (ball.getY() < 50 && player.getBounds().intersects(ball.getBounds())) {
            score += 100; // Example score for a "try"
            resetGame(); // Reset for next round/try
        }
    }

    private void resetGame() {
        // Reset player, ball, and opponents for a new round/try
        player = new Player(WIDTH / 2 - PLAYER_SIZE / 2, HEIGHT - PLAYER_SIZE - 20, PLAYER_SIZE);
        ball = new Ball(WIDTH / 2 - BALL_SIZE / 2, HEIGHT / 2 - BALL_SIZE / 2, BALL_SIZE);
        opponents.clear();
        spawnOpponents(score / 100 + 3); // More opponents as score increases
        gameOver = false; // Important for restarting
        gameTimer.start();
    }


    // --- KeyListener implementations ---
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (gameOver) {
            if (key == KeyEvent.VK_R) {
                initGame(); // Re-initialize everything to restart
            }
            return;
        }

        if (key == KeyEvent.VK_LEFT) {
            player.setDx(-PLAYER_SPEED);
        }
        if (key == KeyEvent.VK_RIGHT) {
            player.setDx(PLAYER_SPEED);
        }
        if (key == KeyEvent.VK_UP) {
            player.setDy(-PLAYER_SPEED);
        }
        if (key == KeyEvent.VK_DOWN) {
            player.setDy(PLAYER_SPEED);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
            player.setDx(0);
        }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) {
            player.setDy(0);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used for continuous movement
    }
}

// --- Player Class ---
class Player {
    private int x, y, size;
    private int dx, dy; // Delta X, Delta Y for movement

    public Player(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.dx = 0;
        this.dy = 0;
    }

    public void update(int maxX, int maxY) {
        x += dx;
        y += dy;

        // Keep player within bounds
        if (x < 0) x = 0;
        if (x > maxX - size) x = maxX - size;
        if (y < 0) y = 0;
        if (y > maxY - size) y = maxY - size;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.BLUE);
        g2d.fillRect(x, y, size, size);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, size, size);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }

    // Getters and Setters
    public int getX() { return x; }
    public int getY() { return y; }
    public void setDx(int dx) { this.dx = dx; }
    public void setDy(int dy) { this.dy = dy; }
}

// --- Ball Class ---
class Ball {
    private int x, y, size;
    private int dx, dy; // Ball movement (can be static for simplicity if held)

    public Ball(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.dx = 0; // For now, ball only moves if explicitly set or "kicked"
        this.dy = 0;
    }

    public void update() {
        x += dx;
        y += dy;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.ORANGE);
        g2d.fillOval(x, y, size, size);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x, y, size, size);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }

    // Getters and Setters
    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setDx(int dx) { this.dx = dx; }
    public void setDy(int dy) { this.dy = dy; }
}

// --- Opponent Class (simple AI) ---
class Opponent {
    private int x, y, size;
    private int speed;
    private Random rand;

    public Opponent(int x, int y, int size, int speed) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.speed = speed;
        this.rand = new Random();
    }

    public void update(int maxX, int maxY, Ball ball) {
        // Simple AI: Move towards the ball's Y position
        if (ball.getY() > y) {
            y += speed;
        } else if (ball.getY() < y) {
            y -= speed;
        }

        // Simple AI: Move towards the ball's X position
        if (ball.getX() > x) {
            x += speed;
        } else if (ball.getX() < x) {
            x -= speed;
        }

        // Keep opponent within bounds (or a certain play area)
        if (x < 0) x = 0;
        if (x > maxX - size) x = maxX - size;
        if (y < 0) y = 0;
        if (y > maxY - size) y = maxY - size;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        g2d.fillRect(x, y, size, size);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, size, size);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }
}

// --- Main Game Frame ---
public class RugbyGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Rugby World Cup Manager - Placeholder");
            GamePanel gamePanel = new GamePanel();
            frame.add(gamePanel);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null); // Center the window
            frame.setVisible(true);
        });
    }
}