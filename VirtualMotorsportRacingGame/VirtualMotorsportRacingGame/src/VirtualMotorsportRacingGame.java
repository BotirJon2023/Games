import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

// Main Game Class
public class VirtualMotorsportRacingGame extends JPanel implements Runnable, KeyListener {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final int FPS = 60;

    private Thread gameThread;
    private boolean running = false;

    // Game states
    private enum GameState { MENU, RACING, PAUSED, FINISHED }
    private GameState gameState = GameState.MENU;

    // Track elements
    private Track currentTrack;
    private List<Car> cars;
    private PlayerCar playerCar;
    private List<Obstacle> obstacles;
    private List<Particle> particles;
    private List<PowerUp> powerUps;

    // Animation variables
    private float cameraX, cameraY;
    private float cameraShake = 0;
    private float screenFlash = 0;
    private Color flashColor = Color.WHITE;

    // Game timing
    private long raceStartTime;
    private long lapStartTime;
    private int currentLap = 1;
    private final int TOTAL_LAPS = 3;

    // Input handling
    private boolean[] keys = new boolean[256];

    // Visual effects
    private Random random = new Random();
    private Font gameFont = new Font("Arial", Font.BOLD, 20);
    private Font bigFont = new Font("Arial", Font.BOLD, 40);

    public VirtualMotorsportRacingGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        initializeGame();
    }

    private void initializeGame() {
        // Create track
        currentTrack = new Track("Monaco Circuit", 2000, 2000);

        // Create player car
        playerCar = new PlayerCar("Player", 500, 500, Color.RED);
        cars = new ArrayList<>();
        cars.add(playerCar);

        // Add AI cars
        cars.add(new AICar("AI-1", 480, 520, Color.BLUE, currentTrack));
        cars.add(new AICar("AI-2", 520, 520, Color.GREEN, currentTrack));
        cars.add(new AICar("AI-3", 480, 480, Color.YELLOW, currentTrack));

        // Initialize obstacles
        obstacles = new ArrayList<>();
        initializeObstacles();

        // Initialize power-ups
        powerUps = new ArrayList<>();
        initializePowerUps();

        particles = new ArrayList<>();

        // Set initial camera position
        cameraX = playerCar.x - WIDTH / 2;
        cameraY = playerCar.y - HEIGHT / 2;
    }

    private void initializeObstacles() {
        // Add various obstacles around the track
        for (int i = 0; i < 15; i++) {
            float x = 300 + random.nextFloat() * 1400;
            float y = 300 + random.nextFloat() * 1400;
            obstacles.add(new Obstacle(x, y, 40, 40, Color.GRAY));
        }

        // Add oil spills
        for (int i = 0; i < 8; i++) {
            float x = 400 + random.nextFloat() * 1200;
            float y = 400 + random.nextFloat() * 1200;
            obstacles.add(new Obstacle(x, y, 60, 60, new Color(50, 50, 50), true));
        }
    }

    private void initializePowerUps() {
        // Add power-ups around the track
        for (int i = 0; i < 10; i++) {
            float x = 500 + random.nextFloat() * 1000;
            float y = 500 + random.nextFloat() * 1000;
            PowerUp.Type[] types = PowerUp.Type.values();
            PowerUp.Type type = types[random.nextInt(types.length)];
            powerUps.add(new PowerUp(x, y, type));
        }
    }

    public void startGame() {
        if (running) return;

        running = true;
        gameThread = new Thread(this);
        gameThread.start();
        raceStartTime = System.currentTimeMillis();
        lapStartTime = raceStartTime;
    }

    public void stopGame() {
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
        double nsPerFrame = 1000000000.0 / FPS;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerFrame;
            lastTime = now;

            while (delta >= 1) {
                update();
                delta--;
            }

            repaint();

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        if (gameState != GameState.RACING) return;

        // Update player car
        playerCar.update(keys);

        // Update AI cars
        for (Car car : cars) {
            if (car instanceof AICar) {
                ((AICar) car).updateAI(cars, obstacles);
            }
        }

        // Check collisions
        checkCollisions();

        // Update particles
        updateParticles();

        // Update camera
        updateCamera();

        // Update visual effects
        updateVisualEffects();

        // Check lap completion
        checkLapCompletion();

        // Check race finish
        if (currentLap > TOTAL_LAPS) {
            gameState = GameState.FINISHED;
        }
    }

    private void checkCollisions() {
        // Check car-obstacle collisions
        for (Car car : cars) {
            for (Obstacle obstacle : obstacles) {
                if (car.getBounds().intersects(obstacle.getBounds())) {
                    handleCollision(car, obstacle);
                }
            }

            // Check car-powerup collisions
            Iterator<PowerUp> powerUpIter = powerUps.iterator();
            while (powerUpIter.hasNext()) {
                PowerUp powerUp = powerUpIter.next();
                if (car.getBounds().intersects(powerUp.getBounds())) {
                    handlePowerUpCollection(car, powerUp);
                    powerUpIter.remove();
                    createParticleEffect(powerUp.x, powerUp.y, powerUp.color, 20);
                }
            }
        }

        // Check car-car collisions
        for (int i = 0; i < cars.size(); i++) {
            for (int j = i + 1; j < cars.size(); j++) {
                if (cars.get(i).getBounds().intersects(cars.get(j).getBounds())) {
                    handleCarCollision(cars.get(i), cars.get(j));
                }
            }
        }
    }

    private void handleCollision(Car car, Obstacle obstacle) {
        if (obstacle.isSlippery()) {
            // Oil spill - reduce control
            car.slide(2.0f);
            createParticleEffect(car.x, car.y, Color.BLACK, 10);
        } else {
            // Solid obstacle - bounce back
            car.bounce();
            cameraShake = 10;
            createParticleEffect(car.x, car.y, Color.ORANGE, 15);
        }
    }

    private void handleCarCollision(Car car1, Car car2) {
        // Simple elastic collision
        float tempVx = car1.vx;
        float tempVy = car1.vy;

        car1.vx = car2.vx * 0.8f;
        car1.vy = car2.vy * 0.8f;
        car2.vx = tempVx * 0.8f;
        car2.vy = tempVy * 0.8f;

        cameraShake = 8;
        createParticleEffect((car1.x + car2.x) / 2, (car1.y + car2.y) / 2, Color.RED, 20);
    }

    private void handlePowerUpCollection(Car car, PowerUp powerUp) {
        switch (powerUp.type) {
            case SPEED_BOOST:
                car.boost(2.0f);
                screenFlash = 10;
                flashColor = Color.CYAN;
                break;
            case SHIELD:
                car.activateShield(3000); // 3 seconds
                screenFlash = 10;
                flashColor = Color.BLUE;
                break;
            case NITRO:
                car.activateNitro(2000); // 2 seconds
                createParticleEffect(car.x, car.y, Color.ORANGE, 30);
                break;
            case REPAIR:
                car.repair();
                screenFlash = 10;
                flashColor = Color.GREEN;
                break;
        }
    }

    private void updateParticles() {
        Iterator<Particle> iter = particles.iterator();
        while (iter.hasNext()) {
            Particle p = iter.next();
            p.update();
            if (p.life <= 0) {
                iter.remove();
            }
        }

        // Create tire smoke for moving cars
        for (Car car : cars) {
            if (car.speed > 5 && random.nextFloat() < 0.3) {
                float offsetX = (float) (Math.cos(car.angle) * -25);
                float offsetY = (float) (Math.sin(car.angle) * -25);
                particles.add(new Particle(
                        car.x + offsetX,
                        car.y + offsetY,
                        (random.nextFloat() - 0.5f) * 2,
                        (random.nextFloat() - 0.5f) * 2,
                        new Color(100, 100, 100, 150),
                        30 + random.nextInt(30)
                ));
            }
        }
    }

    private void updateCamera() {
        // Smooth camera follow with prediction
        float targetX = playerCar.x - WIDTH / 2 + playerCar.vx * 5;
        float targetY = playerCar.y - HEIGHT / 2 + playerCar.vy * 5;

        cameraX += (targetX - cameraX) * 0.1f;
        cameraY += (targetY - cameraY) * 0.1f;

        // Apply camera shake
        if (cameraShake > 0) {
            cameraX += (random.nextFloat() - 0.5f) * cameraShake;
            cameraY += (random.nextFloat() - 0.5f) * cameraShake;
            cameraShake *= 0.9f;
        }

        // Keep camera within track bounds
        cameraX = Math.max(0, Math.min(cameraX, currentTrack.width - WIDTH));
        cameraY = Math.max(0, Math.min(cameraY, currentTrack.height - HEIGHT));
    }

    private void updateVisualEffects() {
        if (screenFlash > 0) {
            screenFlash--;
        }
    }

    private void checkLapCompletion() {
        // Simple lap detection based on position
        if (playerCar.x > 1800 && playerCar.y > 1800) {
            if (System.currentTimeMillis() - lapStartTime > 5000) { // Minimum 5 seconds per lap
                currentLap++;
                lapStartTime = System.currentTimeMillis();
                createParticleEffect(playerCar.x, playerCar.y, Color.WHITE, 50);
                screenFlash = 15;
                flashColor = Color.WHITE;
            }
        }
    }

    private void createParticleEffect(float x, float y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(
                    x, y,
                    (random.nextFloat() - 0.5f) * 8,
                    (random.nextFloat() - 0.5f) * 8,
                    color,
                    20 + random.nextInt(40)
            ));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smoother graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Apply camera transformation
        g2d.translate(-cameraX, -cameraY);

        // Draw track
        currentTrack.draw(g2d);

        // Draw obstacles
        for (Obstacle obstacle : obstacles) {
            obstacle.draw(g2d);
        }

        // Draw power-ups
        for (PowerUp powerUp : powerUps) {
            powerUp.draw(g2d);
        }

        // Draw particles
        for (Particle particle : particles) {
            particle.draw(g2d);
        }

        // Draw cars
        for (Car car : cars) {
            car.draw(g2d);
        }

        // Reset transformation for HUD
        g2d.translate(cameraX, cameraY);

        // Draw HUD
        drawHUD(g2d);

        // Draw screen flash effect
        if (screenFlash > 0) {
            g2d.setColor(new Color(flashColor.getRed(), flashColor.getGreen(), flashColor.getBlue(),
                    (int)(screenFlash * 25.5f)));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        }

        // Draw menu or game over screens
        if (gameState == GameState.MENU) {
            drawMenu(g2d);
        } else if (gameState == GameState.FINISHED) {
            drawFinishScreen(g2d);
        } else if (gameState == GameState.PAUSED) {
            drawPauseScreen(g2d);
        }
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setFont(gameFont);
        g2d.setColor(Color.WHITE);

        // Speed display
        g2d.drawString(String.format("SPEED: %.0f km/h", playerCar.speed * 20), 20, 30);

        // Lap counter
        g2d.drawString(String.format("LAP: %d/%d", currentLap, TOTAL_LAPS), 20, 60);

        // Race time
        long raceTime = System.currentTimeMillis() - raceStartTime;
        g2d.drawString(String.format("TIME: %.1fs", raceTime / 1000.0), 20, 90);

        // Position
        g2d.drawString("POSITION: 1st", 20, 120);

        // Mini-map
        drawMiniMap(g2d);

        // Power-up status
        if (playerCar.hasShield()) {
            g2d.setColor(Color.BLUE);
            g2d.drawString("SHIELD ACTIVE", WIDTH - 150, 30);
        }
        if (playerCar.hasNitro()) {
            g2d.setColor(Color.ORANGE);
            g2d.drawString("NITRO READY", WIDTH - 150, 60);
        }
    }

    private void drawMiniMap(Graphics2D g2d) {
        int mapSize = 150;
        int mapX = WIDTH - mapSize - 20;
        int mapY = HEIGHT - mapSize - 20;

        // Map background
        g2d.setColor(new Color(0, 50, 0, 200));
        g2d.fillRect(mapX, mapY, mapSize, mapSize);

        // Draw track bounds on minimap
        g2d.setColor(Color.GRAY);
        g2d.drawRect(mapX, mapY, mapSize, mapSize);

        // Draw cars on minimap
        float scaleX = mapSize / (float) currentTrack.width;
        float scaleY = mapSize / (float) currentTrack.height;

        for (Car car : cars) {
            if (car == playerCar) {
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.WHITE);
            }
            int carX = mapX + (int)(car.x * scaleX);
            int carY = mapY + (int)(car.y * scaleY);
            g2d.fillOval(carX - 2, carY - 2, 4, 4);
        }
    }

    private void drawMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setFont(bigFont);
        g2d.setColor(Color.WHITE);

        String title = "VIRTUAL MOTORSPORT RACING";
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, (WIDTH - titleWidth) / 2, HEIGHT / 2 - 50);

        g2d.setFont(gameFont);
        g2d.drawString("Press SPACE to Start Race", (WIDTH - 250) / 2, HEIGHT / 2 + 50);
        g2d.drawString("Use ARROW KEYS to drive", (WIDTH - 250) / 2, HEIGHT / 2 + 90);
        g2d.drawString("Press N for Nitro Boost", (WIDTH - 250) / 2, HEIGHT / 2 + 130);
    }

    private void drawFinishScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setFont(bigFont);
        g2d.setColor(Color.YELLOW);

        String finishText = "RACE FINISHED!";
        int textWidth = g2d.getFontMetrics().stringWidth(finishText);
        g2d.drawString(finishText, (WIDTH - textWidth) / 2, HEIGHT / 2 - 50);

        g2d.setFont(gameFont);
        g2d.setColor(Color.WHITE);

        long raceTime = System.currentTimeMillis() - raceStartTime;
        String timeText = String.format("Final Time: %.2f seconds", raceTime / 1000.0);
        int timeWidth = g2d.getFontMetrics().stringWidth(timeText);
        g2d.drawString(timeText, (WIDTH - timeWidth) / 2, HEIGHT / 2 + 20);

        g2d.drawString("Press R to Restart", (WIDTH - 150) / 2, HEIGHT / 2 + 70);
    }

    private void drawPauseScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setFont(bigFont);
        g2d.setColor(Color.WHITE);

        String pauseText = "GAME PAUSED";
        int textWidth = g2d.getFontMetrics().stringWidth(pauseText);
        g2d.drawString(pauseText, (WIDTH - textWidth) / 2, HEIGHT / 2);

        g2d.setFont(gameFont);
        g2d.drawString("Press P to Resume", (WIDTH - 150) / 2, HEIGHT / 2 + 60);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode < keys.length) {
            keys[keyCode] = true;
        }

        // Handle game state changes
        if (keyCode == KeyEvent.VK_SPACE && gameState == GameState.MENU) {
            gameState = GameState.RACING;
            startGame();
        } else if (keyCode == KeyEvent.VK_P) {
            if (gameState == GameState.RACING) {
                gameState = GameState.PAUSED;
            } else if (gameState == GameState.PAUSED) {
                gameState = GameState.RACING;
            }
        } else if (keyCode == KeyEvent.VK_R && gameState == GameState.FINISHED) {
            initializeGame();
            gameState = GameState.MENU;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode < keys.length) {
            keys[keyCode] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Virtual Motorsport Racing Game");
        VirtualMotorsportRacingGame game = new VirtualMotorsportRacingGame();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        game.startGame();
    }
}

// Track Class
class Track {
    String name;
    int width, height;

    public Track(String name, int width, int height) {
        this.name = name;
        this.width = width;
        this.height = height;
    }

    public void draw(Graphics2D g2d) {
        // Draw grass
        g2d.setColor(new Color(0, 100, 0));
        g2d.fillRect(0, 0, width, height);

        // Draw track surface
        g2d.setColor(new Color(80, 80, 80));
        g2d.fillRect(200, 200, width - 400, height - 400);

        // Draw track markings
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(5));
        g2d.drawRect(200, 200, width - 400, height - 400);

        // Draw start/finish line
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                g2d.fillRect(width - 250, 200 + i * 20, 50, 20);
            }
        }

        // Draw checkpoints
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(1800, 1800, 1900, 1900);
    }
}

// Base Car Class
abstract class Car {
    protected String name;
    protected float x, y;
    protected float vx, vy;
    protected float angle;
    protected float speed;
    protected Color color;
    protected float maxSpeed = 15;
    protected float acceleration = 0.2f;
    protected float deceleration = 0.1f;
    protected float steering = 0.05f;

    // Special abilities
    protected boolean hasShield = false;
    protected boolean hasNitro = false;
    protected long shieldEndTime = 0;
    protected long nitroEndTime = 0;

    protected Rectangle2D.Float bounds;

    public Car(String name, float x, float y, Color color) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.color = color;
        this.bounds = new Rectangle2D.Float(x - 15, y - 10, 30, 20);
    }

    public abstract void update(boolean[] keys);

    public void draw(Graphics2D g2d) {
        // Save original transform
        AffineTransform originalTransform = g2d.getTransform();

        // Apply car transform
        g2d.translate(x, y);
        g2d.rotate(angle);

        // Draw car body
        g2d.setColor(color);
        g2d.fillRect(-15, -10, 30, 20);

        // Draw car details
        g2d.setColor(Color.BLACK);
        g2d.fillRect(5, -8, 8, 16); // Rear spoiler
        g2d.fillRect(-15, -5, 5, 10); // Front

        // Draw windows
        g2d.setColor(new Color(200, 200, 255, 150));
        g2d.fillRect(-10, -8, 15, 6);
        g2d.fillRect(-10, 2, 15, 6);

        // Draw shield effect
        if (hasShield) {
            g2d.setColor(new Color(0, 100, 255, 100));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval(-20, -15, 40, 30);
        }

        // Draw nitro effect
        if (hasNitro && System.currentTimeMillis() < nitroEndTime) {
            g2d.setColor(Color.ORANGE);
            g2d.fillRect(15, -5, 10, 10);
        }

        // Restore transform
        g2d.setTransform(originalTransform);

        // Update bounds
        bounds.setRect(x - 15, y - 10, 30, 20);
    }

    public Rectangle2D.Float getBounds() {
        return bounds;
    }

    public void boost(float multiplier) {
        maxSpeed *= multiplier;
        acceleration *= multiplier;
    }

    public void slide(float factor) {
        steering *= 0.5f;
        acceleration *= 0.7f;
    }

    public void bounce() {
        vx = -vx * 0.5f;
        vy = -vy * 0.5f;
    }

    public void activateShield(long duration) {
        hasShield = true;
        shieldEndTime = System.currentTimeMillis() + duration;
    }

    public void activateNitro(long duration) {
        hasNitro = true;
        nitroEndTime = System.currentTimeMillis() + duration;
        maxSpeed *= 2.0f;
    }

    public void repair() {
        maxSpeed = 15;
        acceleration = 0.2f;
        steering = 0.05f;
    }

    public boolean hasShield() {
        return hasShield && System.currentTimeMillis() < shieldEndTime;
    }

    public boolean hasNitro() {
        return hasNitro && System.currentTimeMillis() < nitroEndTime;
    }
}

// Player Car Class
class PlayerCar extends Car {
    public PlayerCar(String name, float x, float y, Color color) {
        super(name, x, y, color);
    }

    @Override
    public void update(boolean[] keys) {
        // Handle input
        if (keys[KeyEvent.VK_UP]) {
            speed += acceleration;
        } else if (keys[KeyEvent.VK_DOWN]) {
            speed -= acceleration;
        } else {
            // Natural deceleration
            if (speed > 0) speed = Math.max(0, speed - deceleration);
            else if (speed < 0) speed = Math.min(0, speed + deceleration);
        }

        // Apply nitro boost
        if (keys[KeyEvent.VK_N] && hasNitro()) {
            speed = maxSpeed * 1.5f;
        }

        // Limit speed
        speed = Math.max(-maxSpeed * 0.5f, Math.min(speed, maxSpeed));

        // Steering
        if (keys[KeyEvent.VK_LEFT]) {
            angle -= steering * (speed / maxSpeed);
        }
        if (keys[KeyEvent.VK_RIGHT]) {
            angle += steering * (speed / maxSpeed);
        }

        // Update position based on speed and angle
        vx = (float) Math.cos(angle) * speed;
        vy = (float) Math.sin(angle) * speed;

        x += vx;
        y += vy;

        // Update special abilities
        if (hasShield && System.currentTimeMillis() >= shieldEndTime) {
            hasShield = false;
        }
        if (hasNitro && System.currentTimeMillis() >= nitroEndTime) {
            hasNitro = false;
            maxSpeed = 15;
        }
    }
}

// AI Car Class
class AICar extends Car {
    private Track track;
    private float targetX, targetY;
    private long lastTargetUpdate;

    public AICar(String name, float x, float y, Color color, Track track) {
        super(name, x, y, color);
        this.track = track;
        setNewTarget();
    }

    @Override
    public void update(boolean[] keys) {
        // AI will be updated separately in updateAI method
    }

    public void updateAI(List<Car> otherCars, List<Obstacle> obstacles) {
        // Update target periodically
        if (System.currentTimeMillis() - lastTargetUpdate > 2000) {
            setNewTarget();
        }

        // Basic AI driving towards target
        float dx = targetX - x;
        float dy = targetY - y;
        float targetAngle = (float) Math.atan2(dy, dx);

        // Adjust angle towards target
        float angleDiff = targetAngle - angle;
        while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

        if (angleDiff > 0) {
            angle += steering * 0.7f;
        } else {
            angle -= steering * 0.7f;
        }

        // Adjust speed based on distance to target
        float distanceToTarget = (float) Math.sqrt(dx * dx + dy * dy);
        if (distanceToTarget > 100) {
            speed += acceleration * 0.8f;
        } else {
            speed -= deceleration;
        }

        // Limit speed
        speed = Math.max(0, Math.min(speed, maxSpeed * 0.8f));

        // Update position
        vx = (float) Math.cos(angle) * speed;
        vy = (float) Math.sin(angle) * speed;

        x += vx;
        y += vy;

        // Simple obstacle avoidance
        for (Obstacle obstacle : obstacles) {
            if (obstacle.getBounds().intersects(getBounds())) {
                // Quick turn to avoid obstacle
                angle += Math.PI / 4;
                break;
            }
        }
    }

    private void setNewTarget() {
        Random random = new Random();
        targetX = 200 + random.nextFloat() * (track.width - 400);
        targetY = 200 + random.nextFloat() * (track.height - 400);
        lastTargetUpdate = System.currentTimeMillis();
    }
}

// Obstacle Class
class Obstacle {
    float x, y;
    int width, height;
    Color color;
    boolean slippery;

    public Obstacle(float x, float y, int width, int height, Color color) {
        this(x, y, width, height, color, false);
    }

    public Obstacle(float x, float y, int width, int height, Color color, boolean slippery) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.slippery = slippery;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillRect((int)x - width/2, (int)y - height/2, width, height);

        if (slippery) {
            // Draw oil spill pattern
            g2d.setColor(new Color(20, 20, 20));
            g2d.fillOval((int)x - width/3, (int)y - height/3, width/2, height/2);
        }
    }

    public Rectangle2D.Float getBounds() {
        return new Rectangle2D.Float(x - width/2, y - height/2, width, height);
    }

    public boolean isSlippery() {
        return slippery;
    }
}

// PowerUp Class
class PowerUp {
    enum Type { SPEED_BOOST, SHIELD, NITRO, REPAIR }

    float x, y;
    Type type;
    Color color;
    float animation = 0;

    public PowerUp(float x, float y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;

        switch (type) {
            case SPEED_BOOST: color = Color.CYAN; break;
            case SHIELD: color = Color.BLUE; break;
            case NITRO: color = Color.ORANGE; break;
            case REPAIR: color = Color.GREEN; break;
        }
    }

    public void draw(Graphics2D g2d) {
        animation += 0.1f;

        // Pulsing animation
        float scale = 1.0f + 0.2f * (float)Math.sin(animation);
        int size = (int)(20 * scale);

        g2d.setColor(color);
        g2d.fillOval((int)x - size/2, (int)y - size/2, size, size);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval((int)x - size/2, (int)y - size/2, size, size);

        // Draw symbol based on type
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        String symbol = "";
        switch (type) {
            case SPEED_BOOST: symbol = "S"; break;
            case SHIELD: symbol = "D"; break;
            case NITRO: symbol = "N"; break;
            case REPAIR: symbol = "R"; break;
        }

        int textWidth = g2d.getFontMetrics().stringWidth(symbol);
        g2d.drawString(symbol, x - textWidth/2, y + 4);
    }

    public Rectangle2D.Float getBounds() {
        return new Rectangle2D.Float(x - 15, y - 15, 30, 30);
    }
}

// Particle Effect Class
class Particle {
    float x, y;
    float vx, vy;
    Color color;
    int life;
    int maxLife;
    float size;

    public Particle(float x, float y, float vx, float vy, Color color, int life) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.life = life;
        this.maxLife = life;
        this.size = 2 + new Random().nextFloat() * 4;
    }

    public void update() {
        x += vx;
        y += vy;
        vy += 0.1f; // Gravity
        vx *= 0.98f; // Air resistance
        vy *= 0.98f;
        life--;

        // Shrink over time
        size = Math.max(1, size * 0.95f);
    }

    public void draw(Graphics2D g2d) {
        float alpha = (float) life / maxLife;
        Color particleColor = new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int)(alpha * 255)
        );

        g2d.setColor(particleColor);
        g2d.fillOval((int)(x - size/2), (int)(y - size/2), (int)size, (int)size);
    }
}