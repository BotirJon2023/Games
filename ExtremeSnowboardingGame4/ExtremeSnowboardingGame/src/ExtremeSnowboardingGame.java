import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class ExtremeSnowboardingGame extends JPanel
        implements Runnable, KeyListener {

    // ==========================
    // WINDOW SETTINGS
    // ==========================
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;
    private static final int GROUND_Y = 480;

    // ==========================
    // GAME LOOP
    // ==========================
    private Thread gameThread;
    private boolean running = true;
    private final int FPS = 60;

    // ==========================
    // INPUT
    // ==========================
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;
    private boolean boostPressed;

    // ==========================
    // GAME OBJECTS
    // ==========================
    private Player player;
    private List<Obstacle> obstacles;
    private List<SnowParticle> snowParticles;
    private List<AIRider> aiRiders;

    // ==========================
    // GAME STATE
    // ==========================
    private int score = 0;
    private int speed = 6;
    private boolean gameOver = false;
    private Random random = new Random();

    // ==========================
    // CONSTRUCTOR
    // ==========================
    public ExtremeSnowboardingGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(200, 230, 255));
        setFocusable(true);
        addKeyListener(this);

        initGame();
        startGame();
    }

    // ==========================
    // INITIALIZATION
    // ==========================
    private void initGame() {
        player = new Player(200, GROUND_Y - 60);
        obstacles = new ArrayList<>();
        snowParticles = new ArrayList<>();
        aiRiders = new ArrayList<>();

        for (int i = 0; i < 150; i++) {
            snowParticles.add(new SnowParticle());
        }

        for (int i = 0; i < 3; i++) {
            aiRiders.add(new AIRider(800 + i * 200));
        }
    }

    private void startGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    // ==========================
    // GAME LOOP
    // ==========================
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
                delta--;
            }

            repaint();

            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {}
        }
    }

    // ==========================
    // UPDATE LOGIC
    // ==========================
    private void updateGame() {
        if (gameOver) return;

        score++;
        speed = 6 + score / 800;

        handleInput();
        player.update();

        updateSnow();
        updateObstacles();
        updateAIRiders();
        checkCollisions();
    }

    private void handleInput() {
        if (leftPressed) {
            player.moveLeft();
        }
        if (rightPressed) {
            player.moveRight();
        }
        if (jumpPressed) {
            player.jump();
        }
        if (boostPressed) {
            player.boost();
        }
    }

    // ==========================
    // SNOW PARTICLES
    // ==========================
    private void updateSnow() {
        for (SnowParticle s : snowParticles) {
            s.update();
        }
    }

    // ==========================
    // OBSTACLES
    // ==========================
    private void updateObstacles() {
        if (random.nextInt(100) < 2) {
            obstacles.add(new Obstacle(WIDTH + 50));
        }

        Iterator<Obstacle> it = obstacles.iterator();
        while (it.hasNext()) {
            Obstacle o = it.next();
            o.update(speed);

            if (o.x + o.width < 0) {
                it.remove();
            }
        }
    }

    // ==========================
    // AI RIDERS
    // ==========================
    private void updateAIRiders() {
        for (AIRider ai : aiRiders) {
            ai.update(speed);
        }
    }

    // ==========================
    // COLLISIONS
    // ==========================
    private void checkCollisions() {
        Rectangle p = player.getBounds();

        for (Obstacle o : obstacles) {
            if (p.intersects(o.getBounds())) {
                gameOver = true;
            }
        }
    }

    // ==========================
    // RENDERING
    // ==========================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        drawBackground(g2);
        drawSnow(g2);
        drawGround(g2);

        for (Obstacle o : obstacles) {
            o.draw(g2);
        }

        for (AIRider ai : aiRiders) {
            ai.draw(g2);
        }

        player.draw(g2);
        drawHUD(g2);

        if (gameOver) {
            drawGameOver(g2);
        }
    }

    private void drawBackground(Graphics2D g) {
        g.setColor(new Color(150, 200, 255));
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawSnow(Graphics2D g) {
        for (SnowParticle s : snowParticles) {
            s.draw(g);
        }
    }

    private void drawGround(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);
    }

    private void drawHUD(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Score: " + score, 20, 30);
        g.drawString("Speed: " + speed, 20, 55);
    }

    private void drawGameOver(Graphics2D g) {
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.setColor(Color.RED);
        g.drawString("GAME OVER", WIDTH / 2 - 150, HEIGHT / 2);
    }

    // ==========================
    // INPUT EVENTS
    // ==========================
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_A) leftPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_D) rightPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_SPACE) jumpPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) boostPressed = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_A) leftPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_D) rightPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_SPACE) jumpPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) boostPressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // ==========================
    // MAIN METHOD
    // ==========================
    public static void main(String[] args) {
        JFrame frame = new JFrame("Extreme Snowboarding Game");
        ExtremeSnowboardingGame game = new ExtremeSnowboardingGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ======================================================
    // ===================== INNER CLASSES ==================
    // ======================================================

    // --------------------------
    // PLAYER
    // --------------------------
    class Player {
        int x, y;
        int width = 40;
        int height = 60;

        double velY = 0;
        boolean onGround = true;

        Player(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void update() {
            velY += 0.8;
            y += velY;

            if (y >= GROUND_Y - height) {
                y = GROUND_Y - height;
                velY = 0;
                onGround = true;
            }
        }

        void moveLeft() {
            x -= 6;
            if (x < 0) x = 0;
        }

        void moveRight() {
            x += 6;
            if (x > WIDTH - width) x = WIDTH - width;
        }

        void jump() {
            if (onGround) {
                velY = -14;
                onGround = false;
            }
        }

        void boost() {
            x += 3;
        }

        Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        void draw(Graphics2D g) {
            g.setColor(Color.BLUE);
            g.fillRect(x, y, width, height);

            g.setColor(Color.BLACK);
            g.fillRect(x - 10, y + height - 5, width + 20, 5);
        }
    }

    // --------------------------
    // OBSTACLE
    // --------------------------
    class Obstacle {
        int x, y;
        int width = 30;
        int height = 40;

        Obstacle(int x) {
            this.x = x;
            this.y = GROUND_Y - height;
        }

        void update(int speed) {
            x -= speed;
        }

        Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        void draw(Graphics2D g) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x, y, width, height);
        }
    }

    // --------------------------
    // SNOW PARTICLE
    // --------------------------
    class SnowParticle {
        int x, y;
        int size;
        int speed;

        SnowParticle() {
            reset();
        }

        void reset() {
            x = random.nextInt(WIDTH);
            y = random.nextInt(HEIGHT);
            size = random.nextInt(3) + 1;
            speed = random.nextInt(3) + 1;
        }

        void update() {
            y += speed;
            if (y > HEIGHT) {
                reset();
                y = 0;
            }
        }

        void draw(Graphics2D g) {
            g.setColor(Color.WHITE);
            g.fillOval(x, y, size, size);
        }
    }

    // --------------------------
    // AI RIDER
    // --------------------------
    class AIRider {
        int x, y;
        int width = 35;
        int height = 55;

        AIRider(int startX) {
            x = startX;
            y = GROUND_Y - height;
        }

        void update(int speed) {
            x -= speed - 2;
            if (x < -width) {
                x = WIDTH + random.nextInt(300);
            }
        }

        void draw(Graphics2D g) {
            g.setColor(Color.RED);
            g.fillRect(x, y, width, height);

            g.setColor(Color.BLACK);
            g.fillRect(x - 8, y + height - 4, width + 16, 4);
        }
    }
}
