import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class SkateboardingGame extends JPanel implements ActionListener, KeyListener {

    // -------------------------
    // Window / game constants
    // -------------------------
    public static final int WIDTH = 960;
    public static final int HEIGHT = 540;

    private static final int TARGET_FPS = 60;
    private static final int DELAY = 1000 / TARGET_FPS;

    // -------------------------
    // Game objects
    // -------------------------
    private Skater skater;
    private ArrayList<Obstacle> obstacles;
    private ParallaxBackground background;

    // Input state (instead of a separate InputHandler class)
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;
    private boolean restartPressed;

    private Timer timer;
    private Random random;

    private int groundY;
    private int score;
    private int highScore;
    private boolean running;
    private boolean gameOver;
    private long startTime;
    private long lastSpawnTime;
    private long spawnInterval;

    // -------------------------
    // Entry point
    // -------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Skateboarding Simulation (Single File)");
            SkateboardingGame gamePanel = new SkateboardingGame();

            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);
            window.add(gamePanel);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }

    // -------------------------
    // Constructor
    // -------------------------
    public SkateboardingGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        requestFocusInWindow();
        addKeyListener(this);

        groundY = (int) (HEIGHT * 0.75);
        random = new Random();

        background = new ParallaxBackground(WIDTH, HEIGHT);
        skater = new Skater(120, groundY - 60, groundY);

        obstacles = new ArrayList<>();

        running = true;
        gameOver = false;
        score = 0;
        highScore = 0;
        startTime = System.currentTimeMillis();
        lastSpawnTime = startTime;
        spawnInterval = 1500;

        timer = new Timer(DELAY, this);
        timer.start();
    }

    // -------------------------
    // Game loop callback
    // -------------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!running) {
            repaint();
            return;
        }

        long now = System.currentTimeMillis();
        updateGame(now);
        repaint();
    }

    // -------------------------
    // Update game state
    // -------------------------
    private void updateGame(long now) {
        background.update();

        handleInput();

        skater.update();

        updateObstacles(now);

        checkCollisions();

        updateScore(now);
    }

    private void handleInput() {
        if (leftPressed) {
            skater.moveLeft();
        } else if (rightPressed) {
            skater.moveRight();
        } else {
            skater.idle();
        }

        if (jumpPressed) {
            skater.jump();
        }

        if (gameOver && restartPressed) {
            resetGame();
        }
    }

    private void updateObstacles(long now) {
        if (now - lastSpawnTime > spawnInterval) {
            spawnObstacle();
            lastSpawnTime = now;

            if (spawnInterval > 600) {
                spawnInterval -= 20;
            }
        }

        Iterator<Obstacle> it = obstacles.iterator();
        while (it.hasNext()) {
            Obstacle o = it.next();
            o.update();

            if (o.getX() + o.getWidth() < 0) {
                it.remove();
            }
        }
    }

    private void spawnObstacle() {
        int type = random.nextInt(3);
        int baseHeight = 30 + random.nextInt(40);
        int width = 30 + random.nextInt(40);

        int y = groundY - baseHeight;
        int speed = 5 + random.nextInt(4);

        Obstacle o = new Obstacle(WIDTH + 40, y, width, baseHeight, speed, type);
        obstacles.add(o);
    }

    private void checkCollisions() {
        for (Obstacle o : obstacles) {
            if (skater.getBounds().intersects(o.getBounds())) {
                running = false;
                gameOver = true;
                if (score > highScore) {
                    highScore = score;
                }
                break;
            }
        }
    }

    private void updateScore(long now) {
        score = (int) ((now - startTime) / 100);
    }

    private void resetGame() {
        obstacles.clear();
        skater.reset(120, groundY - 60);
        running = true;
        gameOver = false;
        startTime = System.currentTimeMillis();
        lastSpawnTime = startTime;
        spawnInterval = 1500;
    }

    // -------------------------
    // Rendering
    // -------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawScene((Graphics2D) g);
    }

    private void drawScene(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        background.draw(g2d);

        g2d.setColor(new Color(60, 60, 60));
        g2d.fillRect(0, groundY, WIDTH, HEIGHT - groundY);

        g2d.setColor(new Color(200, 200, 200));
        int stripeY = groundY + 18;
        for (int x = -40; x < WIDTH + 80; x += 80) {
            g2d.fillRect(x, stripeY, 60, 4);
        }

        for (Obstacle o : obstacles) {
            o.draw(g2d);
        }

        skater.draw(g2d);

        drawHUD(g2d);
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Verdana", Font.BOLD, 18));

        String scoreText = "Score: " + score;
        g2d.drawString(scoreText, 20, 30);

        String hsText = "High Score: " + highScore;
        g2d.drawString(hsText, 20, 60);

        if (gameOver) {
            g2d.setFont(new Font("Verdana", Font.BOLD, 36));
            String over = "GAME OVER";
            int w = g2d.getFontMetrics().stringWidth(over);
            g2d.drawString(over, (WIDTH - w) / 2, HEIGHT / 2 - 40);

            g2d.setFont(new Font("Verdana", Font.PLAIN, 18));
            String tip = "Press ENTER to restart";
            int w2 = g2d.getFontMetrics().stringWidth(tip);
            g2d.drawString(tip, (WIDTH - w2) / 2, HEIGHT / 2);
        }

        g2d.setFont(new Font("Verdana", Font.PLAIN, 14));
        String controlText = "Controls: A/D or Left/Right to move, SPACE to jump, ENTER to restart";
        int w = g2d.getFontMetrics().stringWidth(controlText);
        g2d.drawString(controlText, (WIDTH - w) / 2, HEIGHT - 20);
    }

    // -------------------------
    // KeyListener methods
    // -------------------------
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            leftPressed = true;
        }
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            rightPressed = true;
        }
        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            jumpPressed = true;
        }
        if (code == KeyEvent.VK_ENTER) {
            restartPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            leftPressed = false;
        }
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            rightPressed = false;
        }
        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            jumpPressed = false;
        }
        if (code == KeyEvent.VK_ENTER) {
            restartPressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // not used
    }

    // ==========================================================
    // Inner classes: Skater, Obstacle, ParallaxBackground, Animation
    // ==========================================================

    // -------------------------
    // Skater
    // -------------------------
    private static class Skater {

        private int x;
        private int y;
        private int width;
        private int height;

        private double velX;
        private double velY;

        private final double moveSpeed = 4.0;
        private final double jumpStrength = -13.0;
        private final double gravity = 0.7;
        private final double friction = 0.85;

        private int groundY;

        private Animation idleAnimation;
        private Animation pushAnimation;
        private Animation airAnimation;

        private enum State {
            IDLE, PUSHING, AIR
        }

        private State state;

        public Skater(int startX, int startY, int groundY) {
            this.x = startX;
            this.y = startY;
            this.width = 40;
            this.height = 60;
            this.groundY = groundY;

            state = State.IDLE;

            idleAnimation = Animation.createSimple(6, 10);
            pushAnimation = Animation.createSimple(8, 4);
            airAnimation = Animation.createSimple(4, 8);
        }

        public void update() {
            velY += gravity;
            y += velY;

            if (y + height > groundY) {
                y = groundY - height;
                velY = 0;
                if (state == State.AIR) {
                    state = State.IDLE;
                }
            }

            x += velX;
            velX *= friction;

            if (x < 0) {
                x = 0;
                velX = 0;
            }

            if (x + width > WIDTH) {
                x = WIDTH - width;
                velX = 0;
            }

            switch (state) {
                case IDLE:
                    idleAnimation.update();
                    break;
                case PUSHING:
                    pushAnimation.update();
                    break;
                case AIR:
                    airAnimation.update();
                    break;
            }
        }

        public void moveLeft() {
            velX = -moveSpeed;
            if (isOnGround()) {
                state = State.PUSHING;
            }
        }

        public void moveRight() {
            velX = moveSpeed;
            if (isOnGround()) {
                state = State.PUSHING;
            }
        }

        public void idle() {
            if (isOnGround()) {
                state = State.IDLE;
            }
        }

        public void jump() {
            if (isOnGround()) {
                velY = jumpStrength;
                state = State.AIR;
            }
        }

        public boolean isOnGround() {
            return y + height >= groundY - 1;
        }

        public void draw(Graphics2D g2d) {
            Animation current;
            switch (state) {
                case PUSHING:
                    current = pushAnimation;
                    break;
                case AIR:
                    current = airAnimation;
                    break;
                case IDLE:
                default:
                    current = idleAnimation;
                    break;
            }

            int frame = current.getCurrentFrame();

            if (state == State.AIR) {
                double t = (frame / (double) current.getFrameCount());
                int deckTilt = (int) (t * 20) - 10;
                drawSkaterSimple(g2d, deckTilt);
            } else {
                drawSkaterSimple(g2d, 0);
            }
        }

        private void drawSkaterSimple(Graphics2D g2d, int deckTiltDegrees) {
            int deckWidth = width + 20;
            int deckHeight = 6;
            int deckY = y + height - 10;

            g2d.setColor(new Color(90, 60, 30));
            g2d.fillRoundRect(x - 10, deckY, deckWidth, deckHeight, 6, 6);

            g2d.setColor(Color.DARK_GRAY);
            int r = 5;
            g2d.fillOval(x - 5, deckY + deckHeight - r, r, r);
            g2d.fillOval(x + deckWidth - r - 5, deckY + deckHeight - r, r, r);

            int bodyX = x + width / 2;
            int bodyYTop = y + 10;
            int bodyHeight = 30;

            g2d.setColor(new Color(240, 200, 160));
            g2d.fillOval(bodyX - 10, bodyYTop - 24, 20, 20);

            g2d.setColor(new Color(100, 180, 255));
            g2d.fillRoundRect(bodyX - 12, bodyYTop - 4, 24, bodyHeight, 8, 8);

            g2d.setColor(new Color(240, 200, 160));
            g2d.fillRect(bodyX - 16, bodyYTop + 2, 8, 20);
            g2d.fillRect(bodyX + 8, bodyYTop + 2, 8, 20);

            g2d.setColor(new Color(80, 80, 80));
            g2d.fillRect(bodyX - 10, bodyYTop + bodyHeight, 8, 20);
            g2d.fillRect(bodyX + 2, bodyYTop + bodyHeight, 8, 20);
        }

        public Rectangle getBounds() {
            return new Rectangle(x - 10, y, width + 20, height);
        }

        public void reset(int startX, int startY) {
            this.x = startX;
            this.y = startY;
            velX = 0;
            velY = 0;
            state = State.IDLE;
            idleAnimation.reset();
            pushAnimation.reset();
            airAnimation.reset();
        }
    }

    // -------------------------
    // Obstacle
    // -------------------------
    private static class Obstacle {

        private int x;
        private int y;
        private int width;
        private int height;
        private int speed;
        private int type;

        public Obstacle(int x, int y, int width, int height, int speed, int type) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.speed = speed;
            this.type = type;
        }

        public void update() {
            x -= speed;
        }

        public void draw(Graphics2D g2d) {
            switch (type) {
                case 0:
                    g2d.setColor(new Color(150, 80, 40));
                    g2d.fillRect(x, y, width, height);
                    break;
                case 1:
                    g2d.setColor(new Color(120, 120, 120));
                    g2d.fillRect(x, y + height / 3, width, 3 * height / 4);
                    g2d.setColor(new Color(200, 200, 200));
                    g2d.fillRect(x + width / 4, y, width / 2, height / 3);
                    break;
                case 2:
                    g2d.setColor(new Color(100, 200, 100));
                    g2d.fillRect(x, y, width, height);
                    break;
                default:
                    g2d.setColor(Color.RED);
                    g2d.fillRect(x, y, width, height);
                    break;
            }
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public int getX() {
            return x;
        }

        public int getWidth() {
            return width;
        }
    }

    // -------------------------
    // Parallax background
    // -------------------------
    private static class ParallaxBackground {

        private int width;
        private int height;

        private double cloudOffset;
        private double buildingOffset;

        public ParallaxBackground(int width, int height) {
            this.width = width;
            this.height = height;
            this.cloudOffset = 0;
            this.buildingOffset = 0;
        }

        public void update() {
            cloudOffset -= 0.5;
            buildingOffset -= 1.5;

            if (cloudOffset < -width) {
                cloudOffset += width;
            }
            if (buildingOffset < -width) {
                buildingOffset += width;
            }
        }

        public void draw(Graphics2D g2d) {
            GradientPaint sky = new GradientPaint(
                    0, 0, new Color(120, 160, 255),
                    0, height, new Color(20, 40, 90));
            g2d.setPaint(sky);
            g2d.fill(new Rectangle2D.Double(0, 0, width, height));

            g2d.setColor(new Color(255, 255, 255, 200));
            drawClouds(g2d, cloudOffset);
            drawClouds(g2d, cloudOffset + width);

            g2d.setColor(new Color(40, 40, 60));
            drawBuildings(g2d, buildingOffset);
            drawBuildings(g2d, buildingOffset + width);
        }

        private void drawClouds(Graphics2D g2d, double offset) {
            int baseY = 60;
            g2d.fillOval((int) (offset + 50), baseY, 80, 30);
            g2d.fillOval((int) (offset + 90), baseY - 10, 80, 40);
            g2d.fillOval((int) (offset + 130), baseY, 80, 30);

            g2d.fillOval((int) (offset + 300), baseY + 40, 90, 35);
            g2d.fillOval((int) (offset + 350), baseY + 30, 90, 45);
            g2d.fillOval((int) (offset + 390), baseY + 40, 90, 35);
        }

        private void drawBuildings(Graphics2D g2d, double offset) {
            int baseY = (int) (height * 0.75);

            g2d.fillRect((int) (offset + 40), baseY - 120, 80, 120);
            g2d.fillRect((int) (offset + 160), baseY - 160, 60, 160);
            g2d.fillRect((int) (offset + 260), baseY - 140, 100, 140);
            g2d.fillRect((int) (offset + 400), baseY - 180, 70, 180);
            g2d.fillRect((int) (offset + 500), baseY - 110, 90, 110);
        }
    }

    // -------------------------
    // Animation helper
    // -------------------------
    private static class Animation {

        private int frameCount;
        private int currentFrame;
        private int updatesPerFrame;
        private int counter;

        private Animation(int frameCount, int updatesPerFrame) {
            this.frameCount = frameCount;
            this.updatesPerFrame = updatesPerFrame;
            this.currentFrame = 0;
            this.counter = 0;
        }

        public static Animation createSimple(int frameCount, int updatesPerFrame) {
            return new Animation(frameCount, updatesPerFrame);
        }

        public void update() {
            counter++;
            if (counter >= updatesPerFrame) {
                counter = 0;
                currentFrame++;
                if (currentFrame >= frameCount) {
                    currentFrame = 0;
                }
            }
        }

        public int getCurrentFrame() {
            return currentFrame;
        }

        public int getFrameCount() {
            return frameCount;
        }

        public void reset() {
            currentFrame = 0;
            counter = 0;
        }
    }
}
