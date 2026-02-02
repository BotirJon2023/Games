import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class SkateboardingSimulationGame extends JPanel implements Runnable, KeyListener {

    // ==========================
    // Window & Game Configuration
    // ==========================
    public static final int WINDOW_WIDTH = 1000;
    public static final int WINDOW_HEIGHT = 600;
    public static final int GROUND_Y = 450;
    public static final int FPS = 60;

    private Thread gameThread;
    private boolean running = false;

    // ==========================
    // Game State
    // ==========================
    private Skateboarder skateboarder;
    private List<Ramp> ramps;
    private List<Obstacle> obstacles;
    private ScoreBoard scoreBoard;
    private Background background;

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;

    // ==========================
    // Constructor
    // ==========================
    public SkateboardingSimulationGame() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setFocusable(true);
        requestFocus();
        addKeyListener(this);

        initGame();
    }

    // ==========================
    // Game Initialization
    // ==========================
    private void initGame() {
        skateboarder = new Skateboarder(100, GROUND_Y - 60);
        ramps = new ArrayList<>();
        obstacles = new ArrayList<>();
        scoreBoard = new ScoreBoard();
        background = new Background();

        generateRamps();
        generateObstacles();
    }

    private void generateRamps() {
        ramps.add(new Ramp(300, GROUND_Y - 20, 120, 20));
        ramps.add(new Ramp(600, GROUND_Y - 40, 150, 40));
        ramps.add(new Ramp(850, GROUND_Y - 30, 100, 30));
    }

    private void generateObstacles() {
        obstacles.add(new Obstacle(450, GROUND_Y - 30, 30, 30));
        obstacles.add(new Obstacle(720, GROUND_Y - 25, 25, 25));
    }

    // ==========================
    // Game Loop
    // ==========================
    public void start() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerFrame = 1_000_000_000.0 / FPS;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerFrame;
            lastTime = now;

            while (delta >= 1) {
                updateGame();
                repaint();
                delta--;
            }
        }
    }

    // ==========================
    // Update Logic
    // ==========================
    private void updateGame() {
        skateboarder.update(leftPressed, rightPressed, jumpPressed);
        background.update();

        for (Ramp ramp : ramps) {
            ramp.update();
            skateboarder.checkRampCollision(ramp);
        }

        for (Obstacle obstacle : obstacles) {
            obstacle.update();
            skateboarder.checkObstacleCollision(obstacle);
        }

        scoreBoard.update(skateboarder);
    }

    // ==========================
    // Rendering
    // ==========================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        background.draw(g2d);

        drawGround(g2d);

        for (Ramp ramp : ramps) {
            ramp.draw(g2d);
        }

        for (Obstacle obstacle : obstacles) {
            obstacle.draw(g2d);
        }

        skateboarder.draw(g2d);
        scoreBoard.draw(g2d);
    }

    private void drawGround(Graphics2D g2d) {
        g2d.setColor(new Color(70, 70, 70));
        g2d.fillRect(0, GROUND_Y, WINDOW_WIDTH, WINDOW_HEIGHT - GROUND_Y);
    }

    // ==========================
    // Key Handling
    // ==========================
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            leftPressed = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            rightPressed = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            jumpPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            leftPressed = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            rightPressed = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            jumpPressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    // ==========================
    // Main Method
    // ==========================
    public static void main(String[] args) {
        JFrame frame = new JFrame("Skateboarding Simulation Game");
        SkateboardingSimulationGame game = new SkateboardingSimulationGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        game.start();
    }
}

// ==========================
// Skateboarder Class
// ==========================
class Skateboarder {

    private int x;
    private int y;
    private int width = 40;
    private int height = 60;

    private double velocityX;
    private double velocityY;

    private boolean onGround;

    private static final double GRAVITY = 0.7;
    private static final double SPEED = 4.0;
    private static final double JUMP_FORCE = -12.0;

    private int tricksPerformed;

    public Skateboarder(int x, int y) {
        this.x = x;
        this.y = y;
        this.onGround = true;
    }

    public void update(boolean left, boolean right, boolean jump) {
        if (left) {
            velocityX = -SPEED;
        } else if (right) {
            velocityX = SPEED;
        } else {
            velocityX *= 0.8;
        }

        if (jump && onGround) {
            velocityY = JUMP_FORCE;
            onGround = false;
            tricksPerformed++;
        }

        velocityY += GRAVITY;

        x += velocityX;
        y += velocityY;

        if (y >= SkateboardingSimulationGame.GROUND_Y - height) {
            y = SkateboardingSimulationGame.GROUND_Y - height;
            velocityY = 0;
            onGround = true;
        }

        if (x < 0) {
            x = 0;
        }
        if (x > SkateboardingSimulationGame.WINDOW_WIDTH - width) {
            x = SkateboardingSimulationGame.WINDOW_WIDTH - width;
        }
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.BLUE);
        g2d.fillRect(x, y, width, height);

        g2d.setColor(Color.BLACK);
        g2d.fillRect(x - 5, y + height - 5, width + 10, 5);
    }

    public void checkRampCollision(Ramp ramp) {
        Rectangle skaterRect = getBounds();
        Rectangle rampRect = ramp.getBounds();

        if (skaterRect.intersects(rampRect)) {
            y = ramp.getY() - height;
            velocityY = -8;
            onGround = false;
        }
    }

    public void checkObstacleCollision(Obstacle obstacle) {
        if (getBounds().intersects(obstacle.getBounds())) {
            velocityX = -velocityX * 0.5;
        }
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public int getTricksPerformed() {
        return tricksPerformed;
    }

    public int getX() {
        return x;
    }
}

// ==========================
// Ramp Class
// ==========================
class Ramp {

    private int x;
    private int y;
    private int width;
    private int height;

    public Ramp(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void update() {
        // Static ramp
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.ORANGE);
        g2d.fillRect(x, y, width, height);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public int getY() {
        return y;
    }
}

// ==========================
// Obstacle Class
// ==========================
class Obstacle {

    private int x;
    private int y;
    private int width;
    private int height;

    public Obstacle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void update() {
        // Static obstacle
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        g2d.fillRect(x, y, width, height);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}

// ==========================
// Background Class
// ==========================
class Background {

    private int offsetX;

    public void update() {
        offsetX--;
        if (offsetX <= -100) {
            offsetX = 0;
        }
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(135, 206, 235));
        g2d.fillRect(0, 0, SkateboardingSimulationGame.WINDOW_WIDTH, SkateboardingSimulationGame.GROUND_Y);

        g2d.setColor(Color.WHITE);
        for (int i = 0; i < 5; i++) {
            g2d.fillOval((i * 200 + offsetX), 50, 80, 40);
        }
    }
}

// ==========================
// ScoreBoard Class
// ==========================
class ScoreBoard {

    private int score;

    public void update(Skateboarder skateboarder) {
        score = skateboarder.getX() / 10 + skateboarder.getTricksPerformed() * 50;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("Score: " + score, 20, 30);
    }
}
