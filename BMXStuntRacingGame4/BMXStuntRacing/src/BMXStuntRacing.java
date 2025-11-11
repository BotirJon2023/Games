import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

// Main Game Class
public class BMXStuntRacing extends JPanel implements Runnable, KeyListener {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final int GROUND_LEVEL = 500;

    private Thread gameThread;
    private boolean running = false;
    private int fps = 60;
    private long targetTime = 1000 / fps;

    // Game objects
    private BMXBike bike;
    private Terrain terrain;
    private List<Obstacle> obstacles;
    private List<Ramp> ramps;
    private List<Coin> coins;

    // Game state
    private int score = 0;
    private int lives = 3;
    private boolean gameOver = false;
    private boolean paused = false;
    private int level = 1;

    // Physics constants
    private final double GRAVITY = 0.5;
    private final double BIKE_POWER = 0.8;

    // Animation variables
    private int backgroundOffset = 0;
    private ArrayList<Particle> particles;
    private ArrayList<StuntText> stuntTexts;

    public BMXStuntRacing() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky blue
        setFocusable(true);
        addKeyListener(this);

        initializeGame();
    }

    private void initializeGame() {
        // Initialize bike
        bike = new BMXBike(100, GROUND_LEVEL - 50);

        // Initialize terrain
        terrain = new Terrain(WIDTH, HEIGHT, GROUND_LEVEL);

        // Initialize obstacles and ramps
        obstacles = new ArrayList<>();
        ramps = new ArrayList<>();
        coins = new ArrayList<>();
        particles = new ArrayList<>();
        stuntTexts = new ArrayList<>();

        generateLevel();

        // Start game thread
        if (gameThread == null) {
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    private void generateLevel() {
        // Clear existing objects
        obstacles.clear();
        ramps.clear();
        coins.clear();

        // Generate ramps based on level
        int rampCount = 3 + level;
        for (int i = 0; i < rampCount; i++) {
            int x = 300 + i * 200 + (int)(Math.random() * 100);
            int width = 80 + (int)(Math.random() * 40);
            int height = 30 + level * 5;
            ramps.add(new Ramp(x, GROUND_LEVEL - height, width, height));
        }

        // Generate obstacles
        int obstacleCount = 5 + level;
        for (int i = 0; i < obstacleCount; i++) {
            int x = 400 + i * 150 + (int)(Math.random() * 100);
            int width = 20 + (int)(Math.random() * 30);
            int height = 20 + (int)(Math.random() * 30);
            obstacles.add(new Obstacle(x, GROUND_LEVEL - height, width, height));
        }

        // Generate coins
        int coinCount = 10 + level * 2;
        for (int i = 0; i < coinCount; i++) {
            int x = 200 + i * 80 + (int)(Math.random() * 50);
            int y = GROUND_LEVEL - 100 - (int)(Math.random() * 200);
            coins.add(new Coin(x, y));
        }
    }

    @Override
    public void run() {
        long startTime, elapsedTime, waitTime;

        while (running) {
            startTime = System.nanoTime();

            if (!paused && !gameOver) {
                updateGame();
            }

            repaint();

            elapsedTime = System.nanoTime() - startTime;
            waitTime = targetTime - elapsedTime / 1000000;

            if (waitTime > 0) {
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateGame() {
        // Update bike
        bike.update(terrain, ramps, obstacles);

        // Update background parallax
        backgroundOffset = (backgroundOffset + (int)(bike.getVelocityX() * 0.1)) % WIDTH;

        // Check collisions with obstacles
        for (Obstacle obstacle : obstacles) {
            if (bike.collidesWith(obstacle)) {
                handleCrash();
                break;
            }
        }

        // Check ramp landings and stunts
        for (Ramp ramp : ramps) {
            if (bike.isOnRamp(ramp) && bike.getVelocityY() < 0) {
                performStunt("Ramp Jump!");
                score += 50;
            }
        }

        // Collect coins
        Iterator<Coin> coinIterator = coins.iterator();
        while (coinIterator.hasNext()) {
            Coin coin = coinIterator.next();
            if (bike.collidesWith(coin)) {
                coinIterator.remove();
                score += 100;
                createParticles(coin.getX(), coin.getY(), Color.YELLOW, 10);
            }
        }

        // Update particles
        updateParticles();

        // Update stunt texts
        updateStuntTexts();

        // Check if level completed
        if (bike.getX() > WIDTH * level) {
            levelUp();
        }

        // Check game over conditions
        if (lives <= 0) {
            gameOver = true;
        }
    }

    private void handleCrash() {
        lives--;
        createParticles(bike.getX(), bike.getY(), Color.RED, 20);
        bike.reset(100, GROUND_LEVEL - 50);

        if (lives > 0) {
            stuntTexts.add(new StuntText("CRASH! Lives: " + lives,
                    bike.getX(), bike.getY() - 50, Color.RED, 60));
        }
    }

    private void performStunt(String stuntName) {
        score += 200;
        stuntTexts.add(new StuntText(stuntName + " +200",
                bike.getX(), bike.getY() - 100, Color.ORANGE, 90));
        createParticles(bike.getX(), bike.getY() - 20, Color.CYAN, 15);
    }

    private void levelUp() {
        level++;
        score += 1000;
        stuntTexts.add(new StuntText("LEVEL " + level + "! +1000",
                WIDTH / 2, HEIGHT / 2, Color.GREEN, 120));
        generateLevel();
        bike.reset(100, GROUND_LEVEL - 50);
    }

    private void createParticles(double x, double y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 2 + Math.random() * 4;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            int life = 30 + (int)(Math.random() * 30);
            particles.add(new Particle(x, y, vx, vy, color, life));
        }
    }

    private void updateParticles() {
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.update();
            if (!particle.isAlive()) {
                iterator.remove();
            }
        }
    }

    private void updateStuntTexts() {
        Iterator<StuntText> iterator = stuntTexts.iterator();
        while (iterator.hasNext()) {
            StuntText text = iterator.next();
            text.update();
            if (!text.isAlive()) {
                iterator.remove();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smoother graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2d);
        terrain.draw(g2d);

        // Draw game objects
        for (Ramp ramp : ramps) {
            ramp.draw(g2d);
        }

        for (Obstacle obstacle : obstacles) {
            obstacle.draw(g2d);
        }

        for (Coin coin : coins) {
            coin.draw(g2d);
        }

        // Draw particles
        for (Particle particle : particles) {
            particle.draw(g2d);
        }

        // Draw bike
        bike.draw(g2d);

        // Draw stunt texts
        for (StuntText text : stuntTexts) {
            text.draw(g2d);
        }

        drawHUD(g2d);

        if (gameOver) {
            drawGameOverScreen(g2d);
        }

        if (paused) {
            drawPauseScreen(g2d);
        }
    }

    private void drawBackground(Graphics2D g2d) {
        // Sky gradient
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, HEIGHT/2, new Color(255, 255, 255));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT/2);

        // Mountains in background (parallax)
        g2d.setColor(new Color(120, 120, 120));
        for (int i = 0; i < 5; i++) {
            int x = (i * 300 - backgroundOffset / 3) % (WIDTH + 300) - 150;
            int[] xPoints = {x, x + 150, x + 300};
            int[] yPoints = {GROUND_LEVEL - 100, GROUND_LEVEL - 250, GROUND_LEVEL - 100};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }

        // Clouds
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < 4; i++) {
            int x = (i * 250 - backgroundOffset / 2) % (WIDTH + 250) - 125;
            g2d.fillOval(x, 80, 100, 40);
            g2d.fillOval(x + 30, 60, 80, 50);
            g2d.fillOval(x + 60, 80, 70, 40);
        }
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(10, 10, 200, 80);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Score: " + score, 20, 30);
        g2d.drawString("Lives: " + lives, 20, 50);
        g2d.drawString("Level: " + level, 20, 70);
        g2d.drawString("Speed: " + String.format("%.1f", Math.abs(bike.getVelocityX())), 20, 90);
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString("GAME OVER", WIDTH/2 - 140, HEIGHT/2 - 50);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("Final Score: " + score, WIDTH/2 - 100, HEIGHT/2);
        g2d.drawString("Press R to Restart", WIDTH/2 - 120, HEIGHT/2 + 50);
    }

    private void drawPauseScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString("PAUSED", WIDTH/2 - 100, HEIGHT/2);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (gameOver) {
            if (key == KeyEvent.VK_R) {
                restartGame();
            }
            return;
        }

        switch (key) {
            case KeyEvent.VK_LEFT:
                bike.setMovingLeft(true);
                break;
            case KeyEvent.VK_RIGHT:
                bike.setMovingRight(true);
                break;
            case KeyEvent.VK_UP:
                bike.jump();
                break;
            case KeyEvent.VK_SPACE:
                if (bike.isInAir()) {
                    bike.performStunt();
                    performStunt("Backflip!");
                }
                break;
            case KeyEvent.VK_P:
                paused = !paused;
                break;
            case KeyEvent.VK_R:
                restartGame();
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_LEFT:
                bike.setMovingLeft(false);
                break;
            case KeyEvent.VK_RIGHT:
                bike.setMovingRight(false);
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void restartGame() {
        score = 0;
        lives = 3;
        level = 1;
        gameOver = false;
        paused = false;
        initializeGame();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("BMX Stunt Racing");
        BMXStuntRacing game = new BMXStuntRacing();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}

// BMX Bike Class
class BMXBike {
    private double x, y;
    private double velocityX, velocityY;
    private boolean movingLeft, movingRight;
    private boolean inAir = false;
    private boolean onRamp = false;
    private int stuntFrame = 0;
    private double rotation = 0;

    // Bike dimensions
    private final int WIDTH = 60;
    private final int HEIGHT = 30;
    private final int WHEEL_RADIUS = 10;

    public BMXBike(double startX, double startY) {
        this.x = startX;
        this.y = startY;
    }

    public void update(Terrain terrain, List<Ramp> ramps, List<Obstacle> obstacles) {
        // Handle movement
        if (movingRight) velocityX += 0.2;
        if (movingLeft) velocityX -= 0.2;

        // Apply friction
        velocityX *= 0.95;

        // Limit maximum speed
        if (velocityX > 8) velocityX = 8;
        if (velocityX < -8) velocityX = -8;

        // Apply gravity if in air
        if (inAir) {
            velocityY += 0.5; // gravity
        } else {
            velocityY = 0;
        }

        // Update position
        x += velocityX;
        y += velocityY;

        // Check terrain collision
        double groundY = terrain.getGroundY((int)x);
        if (y + HEIGHT/2 > groundY) {
            y = groundY - HEIGHT/2;
            inAir = false;
            velocityY = 0;
            stuntFrame = 0;
            rotation = 0;
        } else {
            inAir = true;
        }

        // Check ramp collisions
        onRamp = false;
        for (Ramp ramp : ramps) {
            if (isOnRamp(ramp)) {
                onRamp = true;
                double rampY = ramp.getSurfaceY((int)(x - ramp.getX()));
                if (y + HEIGHT/2 > rampY) {
                    y = rampY - HEIGHT/2;
                    inAir = false;
                    velocityY = 0;
                }
                break;
            }
        }

        // Handle stunt rotation
        if (stuntFrame > 0) {
            rotation += 15;
            stuntFrame--;
        }

        // Keep bike on screen
        if (x < WHEEL_RADIUS) {
            x = WHEEL_RADIUS;
            velocityX = 0;
        }
    }

    public void jump() {
        if (!inAir) {
            velocityY = -12;
            inAir = true;
        }
    }

    public void performStunt() {
        if (inAir && stuntFrame == 0) {
            stuntFrame = 30;
        }
    }

    public boolean isOnRamp(Ramp ramp) {
        return x + WIDTH/2 > ramp.getX() && x - WIDTH/2 < ramp.getX() + ramp.getWidth();
    }

    public boolean collidesWith(GameObject obj) {
        return x + WIDTH/2 > obj.getX() && x - WIDTH/2 < obj.getX() + obj.getWidth() &&
                y + HEIGHT/2 > obj.getY() && y - HEIGHT/2 < obj.getY() + obj.getHeight();
    }

    public void draw(Graphics2D g2d) {
        // Save original transform
        AffineTransform originalTransform = g2d.getTransform();

        // Translate to bike position and apply rotation for stunts
        g2d.translate(x, y);
        if (stuntFrame > 0) {
            g2d.rotate(Math.toRadians(rotation));
        }

        // Draw bike frame
        g2d.setColor(Color.RED);
        g2d.fillRect(-WIDTH/2, -HEIGHT/2, WIDTH, HEIGHT);

        // Draw wheels
        g2d.setColor(Color.BLACK);
        g2d.fillOval(-WIDTH/2 - WHEEL_RADIUS/2, -HEIGHT/2 - WHEEL_RADIUS, WHEEL_RADIUS*2, WHEEL_RADIUS*2);
        g2d.fillOval(WIDTH/2 - WHEEL_RADIUS*3/2, -HEIGHT/2 - WHEEL_RADIUS, WHEEL_RADIUS*2, WHEEL_RADIUS*2);

        // Draw handlebars
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(WIDTH/2 - 5, -HEIGHT/2, WIDTH/2 + 15, -HEIGHT/2 - 10);

        // Draw rider
        g2d.setColor(Color.GREEN);
        g2d.fillOval(-5, -HEIGHT/2 - 10, 10, 15);

        // Restore original transform
        g2d.setTransform(originalTransform);
    }

    public void reset(double x, double y) {
        this.x = x;
        this.y = y;
        this.velocityX = 0;
        this.velocityY = 0;
        this.inAir = false;
        this.stuntFrame = 0;
        this.rotation = 0;
    }

    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public boolean isInAir() { return inAir; }

    // Setters
    public void setMovingLeft(boolean moving) { this.movingLeft = moving; }
    public void setMovingRight(boolean moving) { this.movingRight = moving; }
}

// Terrain Class
class Terrain {
    private int width, height, groundLevel;
    private int[] terrainHeights;

    public Terrain(int width, int height, int groundLevel) {
        this.width = width;
        this.height = height;
        this.groundLevel = groundLevel;
        generateTerrain();
    }

    private void generateTerrain() {
        terrainHeights = new int[width];

        // Generate random terrain with hills and valleys
        Random random = new Random();
        double noise = 0;
        for (int i = 0; i < width; i++) {
            noise += (random.nextDouble() - 0.5) * 2;
            noise *= 0.8;

            // Add some larger hills
            double hill = Math.sin(i * 0.02) * 20;

            terrainHeights[i] = groundLevel + (int)(noise + hill);
        }
    }

    public double getGroundY(int x) {
        if (x < 0) return terrainHeights[0];
        if (x >= width) return terrainHeights[width - 1];
        return terrainHeights[x];
    }

    public void draw(Graphics2D g2d) {
        // Draw ground
        GradientPaint groundGradient = new GradientPaint(0, groundLevel - 50, new Color(139, 69, 19),
                0, groundLevel + 50, new Color(101, 67, 33));
        g2d.setPaint(groundGradient);

        Polygon groundPolygon = new Polygon();
        groundPolygon.addPoint(0, height);
        for (int i = 0; i < width; i += 10) {
            groundPolygon.addPoint(i, terrainHeights[i]);
        }
        groundPolygon.addPoint(width, height);
        g2d.fill(groundPolygon);

        // Draw grass on top
        g2d.setColor(new Color(34, 139, 34));
        for (int i = 0; i < width; i += 5) {
            int grassHeight = 5 + (i % 15);
            g2d.drawLine(i, terrainHeights[i], i, terrainHeights[i] - grassHeight);
        }
    }
}

// GameObject Interface
interface GameObject {
    double getX();
    double getY();
    double getWidth();
    double getHeight();
    void draw(Graphics2D g2d);
}

// Ramp Class
class Ramp implements GameObject {
    private double x, y;
    private double width, height;

    public Ramp(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getSurfaceY(double localX) {
        double rampSlope = height / width;
        return y + height - (localX * rampSlope);
    }

    @Override
    public void draw(Graphics2D g2d) {
        // Draw ramp
        g2d.setColor(new Color(150, 75, 0));
        Polygon rampPolygon = new Polygon();
        rampPolygon.addPoint((int)x, (int)y + (int)height);
        rampPolygon.addPoint((int)x, (int)y);
        rampPolygon.addPoint((int)(x + width), (int)y + (int)height);
        g2d.fill(rampPolygon);

        // Draw ramp surface
        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine((int)x, (int)y, (int)(x + width), (int)y + (int)height);
    }

    @Override
    public double getX() { return x; }
    @Override
    public double getY() { return y; }
    @Override
    public double getWidth() { return width; }
    @Override
    public double getHeight() { return height; }
}

// Obstacle Class
class Obstacle implements GameObject {
    private double x, y;
    private double width, height;
    private Color color;

    public Obstacle(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = new Color(100, 100, 100);
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillRect((int)x, (int)y, (int)width, (int)height);

        // Add some details
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawRect((int)x, (int)y, (int)width, (int)height);
    }

    @Override
    public double getX() { return x; }
    @Override
    public double getY() { return y; }
    @Override
    public double getWidth() { return width; }
    @Override
    public double getHeight() { return height; }
}

// Coin Class
class Coin implements GameObject {
    private double x, y;
    private int animationFrame = 0;

    public Coin(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void draw(Graphics2D g2d) {
        animationFrame++;
        double scale = 1.0 + 0.1 * Math.sin(animationFrame * 0.2);

        g2d.setColor(Color.YELLOW);
        g2d.fillOval((int)x - 8, (int)y - 8, 16, 16);

        g2d.setColor(Color.ORANGE);
        g2d.drawOval((int)x - 8, (int)y - 8, 16, 16);

        // Shine effect
        g2d.setColor(new Color(255, 255, 255, 128));
        g2d.fillOval((int)x - 4, (int)y - 4, 6, 6);
    }

    @Override
    public double getX() { return x; }
    @Override
    public double getY() { return y; }
    @Override
    public double getWidth() { return 16; }
    @Override
    public double getHeight() { return 16; }
}

// Particle Effect Class
class Particle {
    private double x, y;
    private double velocityX, velocityY;
    private Color color;
    private int life;
    private int maxLife;

    public Particle(double x, double y, double vx, double vy, Color color, int life) {
        this.x = x;
        this.y = y;
        this.velocityX = vx;
        this.velocityY = vy;
        this.color = color;
        this.life = life;
        this.maxLife = life;
    }

    public void update() {
        x += velocityX;
        y += velocityY;
        velocityY += 0.1; // gravity
        life--;
    }

    public void draw(Graphics2D g2d) {
        float alpha = (float)life / maxLife;
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255)));
        g2d.fillOval((int)x - 2, (int)y - 2, 4, 4);
    }

    public boolean isAlive() {
        return life > 0;
    }
}

// Stunt Text Animation Class
class StuntText {
    private String text;
    private double x, y;
    private Color color;
    private int life;
    private int maxLife;

    public StuntText(String text, double x, double y, Color color, int life) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.life = life;
        this.maxLife = life;
    }

    public void update() {
        y -= 1; // Float upward
        life--;
    }

    public void draw(Graphics2D g2d) {
        float alpha = (float)life / maxLife;
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255)));

        // Scale font based on remaining life
        int fontSize = 20 + (int)(10 * (1 - alpha));
        g2d.setFont(new Font("Arial", Font.BOLD, fontSize));

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g2d.drawString(text, (int)(x - textWidth/2), (int)y);
    }

    public boolean isAlive() {
        return life > 0;
    }
}