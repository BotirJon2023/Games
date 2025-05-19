import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class AirHockeyGame extends JFrame {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private GamePanel gamePanel;

    public AirHockeyGame() {
        setTitle("Air Hockey Game");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        gamePanel = new GamePanel();
        add(gamePanel);
        addKeyListener(new GameKeyListener());
        setFocusable(true);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AirHockeyGame());
    }

    private class GameKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            gamePanel.handleKeyPress(e.getKeyCode());
        }

        @Override
        public void keyReleased(KeyEvent e) {
            gamePanel.handleKeyRelease(e.getKeyCode());
        }
    }
}

class GamePanel extends JPanel {
    private static final int PADDLE_SIZE = 50;
    private static final int PUCK_SIZE = 30;
    private static final int GOAL_WIDTH = 150;
    private static final int FPS = 60;
    private static final double DT = 1.0 / FPS;

    private Paddle player1, player2;
    private Puck puck;
    private int score1, score2;
    private boolean running;
    private Timer timer;
    private Random random;

    public GamePanel() {
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(800, 600));
        player1 = new Paddle(100, 300, PADDLE_SIZE, Color.BLUE);
        player2 = new Paddle(650, 300, PADDLE_SIZE, Color.RED);
        puck = new Puck(400, 300, PUCK_SIZE);
        score1 = 0;
        score2 = 0;
        running = true;
        random = new Random();

        timer = new Timer(1000 / FPS, e -> update());
        timer.start();

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                player1.updatePosition(e.getX(), e.getY());
            }
        });
    }

    private void update() {
        if (!running) return;

        movePuck();
        checkCollisions();
        checkGoals();
        repaint();
    }

    private void movePuck() {
        puck.move(DT);
        if (puck.getY() < 0 || puck.getY() > getHeight() - PUCK_SIZE) {
            puck.setVy(-puck.getVy());
        }
    }

    private void checkCollisions() {
        // Puck with player1 paddle
        if (puck.intersects(player1)) {
            puck.handlePaddleCollision(player1);
        }
        // Puck with player2 paddle
        if (puck.intersects(player2)) {
            puck.handlePaddleCollision(player2);
        }
    }

    private void checkGoals() {
        if (puck.getX() < 0 && puck.getY() > (getHeight() - GOAL_WIDTH) / 2 &&
                puck.getY() < (getHeight() + GOAL_WIDTH) / 2) {
            score2++;
            resetPuck();
        } else if (puck.getX() > getWidth() - PUCK_SIZE &&
                puck.getY() > (getHeight() - GOAL_WIDTH) / 2 &&
                puck.getY() < (getHeight() + GOAL_WIDTH) / 2) {
            score1++;
            resetPuck();
        }
    }

    private void resetPuck() {
        puck.setPosition(400, 300);
        puck.setVelocity(random.nextDouble() * 200 - 100, random.nextDouble() * 200 - 100);
    }

    public void handleKeyPress(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_W:
                player2.setDy(-300);
                break;
            case KeyEvent.VK_S:
                player2.setDy(300);
                break;
            case KeyEvent.VK_A:
                player2.setDx(-300);
                break;
            case KeyEvent.VK_D:
                player2.setDx(300);
                break;
            case KeyEvent.VK_SPACE:
                if (!running) {
                    running = true;
                    timer.start();
                }
                break;
        }
    }

    public void handleKeyRelease(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_S:
                player2.setDy(0);
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_D:
                player2.setDx(0);
                break;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw center line
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());

        // Draw goals
        g2d.setColor(Color.YELLOW);
        g2d.drawRect(0, (getHeight() - GOAL_WIDTH) / 2, 10, GOAL_WIDTH);
        g2d.drawRect(getWidth() - 10, (getHeight() - GOAL_WIDTH) / 2, 10, GOAL_WIDTH);

        // Draw scores
        g2d.setFont(new Font("Arial", Font.BOLD, 30));
        g2d.drawString(score1 + "", getWidth() / 4, 50);
        g2d.drawString(score2 + "", 3 * getWidth() / 4, 50);

        // Draw game objects
        player1.draw(g2d);
        player2.draw(g2d);
        puck.draw(g2d);

        if (!running) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            g2d.drawString("Paused - Press SPACE to Continue", 100, getHeight() / 2);
        }
    }
}

class Paddle {
    private double x, y;
    private int size;
    private Color color;
    private double dx, dy;

    public Paddle(double x, double y, int size, Color color) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = color;
        this.dx = 0;
        this.dy = 0;
    }

    public void updatePosition(double mouseX, double mouseY) {
        this.x = mouseX;
        this.y = mouseY;
    }

    public void move(double dt) {
        x += dx * dt;
        y += dy * dt;

        // Keep paddle within bounds
        if (x < size / 2) x = size / 2;
        if (x > 800 - size / 2) x = 800 - size / 2;
        if (y < size / 2) y = size / 2;
        if (y > 600 - size / 2) y = 600 - size / 2;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillOval((int)(x - size / 2), (int)(y - size / 2), size, size);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return size; }
    public void setDx(double dx) { this.dx = dx; }
    public void setDy(double dy) { this.dy = dy; }
}

class Puck {
    private double x, y;
    private int size;
    private double vx, vy;
    private Color color = Color.WHITE;

    public Puck(double x, double y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.vx = 100;
        this.vy = 100;
    }

    public void move(double dt) {
        x += vx * dt;
        y += vy * dt;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillOval((int)(x - size / 2), (int)(y - size / 2), size, size);
    }

    public boolean intersects(Paddle paddle) {
        double dx = x - paddle.getX();
        double dy = y - paddle.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance < (size + paddle.getSize()) / 2;
    }

    public void handlePaddleCollision(Paddle paddle) {
        double dx = x - paddle.getX();
        double dy = y - paddle.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        double normalX = dx / distance;
        double normalY = dy / distance;

        double relativeVx = vx;
        double relativeVy = vy;

        double dotProduct = relativeVx * normalX + relativeVy * normalY;
        vx -= 2 * dotProduct * normalX;
        vy -= 2 * dotProduct * normalY;

        // Add some randomness to avoid repetitive motion
        Random rand = new Random();
        vx += rand.nextDouble() * 20 - 10;
        vy += rand.nextDouble() * 20 - 10;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }
    public void setVelocity(double vx, double vy) { this.vx = vx; this.vy = vy; }
    public void setVx(double vx) { this.vx = vx; }
    public void setVy(double vy) { this.vy = vy; }
}

// Additional utility class for game settings
class GameSettings {
    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 600;
    public static final int PADDLE_SPEED = 300;
    public static final int PUCK_SPEED = 200;
    public static final Color BACKGROUND_COLOR = Color.BLACK;
    public static final Color CENTER_LINE_COLOR = Color.WHITE;
    public static final Color GOAL_COLOR = Color.YELLOW;
    public static final Font SCORE_FONT = new Font("Arial", Font.BOLD, 30);
    public static final Font PAUSE_FONT = new Font("Arial", Font.BOLD, 40);
}

// Class to handle game state
class GameState {
    private int score1, score2;
    private boolean isPaused;

    public GameState() {
        score1 = 0;
        score2 = 0;
        isPaused = false;
    }

    public void incrementScore1() { score1++; }
    public void incrementScore2() { score2++; }
    public int getScore1() { return score1; }
    public int getScore2() { return score2; }
    public void setPaused(boolean paused) { isPaused = paused; }
    public boolean isPaused() { return isPaused; }
}

// Class to handle animation effects
class AnimationEffect {
    private double x, y;
    private double opacity;
    private double scale;
    private long startTime;

    public AnimationEffect(double x, double y) {
        this.x = x;
        this.y = y;
        this.opacity = 1.0;
        this.scale = 1.0;
        this.startTime = System.currentTimeMillis();
    }

    public void update() {
        long elapsed = System.currentTimeMillis() - startTime;
        opacity = 1.0 - (double) elapsed / 1000;
        scale = 1.0 + (double) elapsed / 1000;
        if (opacity < 0) opacity = 0;
    }

    public void draw(Graphics2D g2d) {
        if (opacity <= 0) return;
        g2d.setColor(new Color(1.0f, 1.0f, 1.0f, (float) opacity));
        int size = (int) (20 * scale);
        g2d.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
    }

    public boolean isFinished() {
        return opacity <= 0;
    }
}

// Class to manage sound effects (placeholder for future implementation)
class SoundManager {
    public void playPaddleHit() {
        // Placeholder for sound effect
        System.out.println("Paddle hit sound played");
    }

    public void playGoalScored() {
        // Placeholder for sound effect
        System.out.println("Goal scored sound played");
    }

    public void playGameStart() {
        // Placeholder for sound effect
        System.out.println("Game start sound played");
    }
}

// Class to handle game input configuration
class InputConfig {
    public static final int KEY_UP = KeyEvent.VK_W;
    public static final int KEY_DOWN = KeyEvent.VK_S;
    public static final int KEY_LEFT = KeyEvent.VK_A;
    public static final int KEY_RIGHT = KeyEvent.VK_D;
    public static final int KEY_PAUSE = KeyEvent.VK_SPACE;
}

// Class to manage game rendering utilities
class RenderUtils {
    public static void drawCenteredText(Graphics2D g2d, String text, int y, Font font, Color color) {
        g2d.setFont(font);
        g2d.setColor(color);
        FontMetrics fm = g2d.getFontMetrics();
        int x = (800 - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, y);
    }

    public static void drawShadowedOval(Graphics2D g2d, int x, int y, int size, Color color) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillOval(x + 2, y + 2, size, size);
        g2d.setColor(color);
        g2d.fillOval(x, y, size, size);
    }
}