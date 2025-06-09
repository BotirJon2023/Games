import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Boat Racing Tournament Game
 * A comprehensive boat racing game with tournaments, animations, and multiple game modes
 */
public class BoatRacingTournament6 extends JFrame implements ActionListener, KeyListener {


    // Game constants
    private static final double WINDOW_WIDTH = 1200;
    private static final double WINDOW_HEIGHT = 800;
    private static final double TRACK_WIDTH = 1000;
    private static final double TRACK_HEIGHT = 600;
    private static final double BOAT_WIDTH = 60;
    private static final double BOAT_HEIGHT = 30;
    private static final double NUM_LANES = 6;
    private static final double LANE_HEIGHT = TRACK_HEIGHT / NUM_LANES;

    // Game state
    private GameState currentState;
    private Timer gameTimer;
    private List<Boat> boats;
    private Tournament tournament;
    private Player player;
    private RaceTrack raceTrack;
    private ParticleSystem particleSystem;
    private SoundManager soundManager;
    private GameRenderer renderer;
    private InputHandler inputHandler;

    // Game timing
    private long lastUpdateTime;
    private int frameCount;
    private double deltaTime;

    // Animation variables
    private double waveOffset;
    private double cloudOffset;
    private List<PowerUp> powerUps;
    private List<Obstacle> obstacles;

    public enum GameState {
        MENU, RACE_SETUP, RACING, RACE_FINISHED, TOURNAMENT_RESULTS, GAME_OVER
    }

    public BoatRacingTournament6() {
        initializeGame();
        setupWindow();
        startGameLoop();
    }

    private void initializeGame() {
        currentState = GameState.MENU;
        boats = new ArrayList<>();
        tournament = new Tournament();
        player = new Player("Player 1");
        raceTrack = new RaceTrack();
        particleSystem = new ParticleSystem();
        soundManager = new SoundManager();
        renderer = new GameRenderer();
        inputHandler = new InputHandler();
        powerUps = new ArrayList<>();
        obstacles = new ArrayList<>();

        lastUpdateTime = System.nanoTime();
        waveOffset = 0;
        cloudOffset = 0;

        createBoats();
        generatePowerUps();
        generateObstacles();
    }

    private void setupWindow() {
        setTitle("Boat Racing Tournament");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();

        gameTimer = new Timer(16, this); // ~60 FPS
    }

    private void startGameLoop() {
        gameTimer.start();
        setVisible(true);
    }

    private void createBoats() {
        String[] boatNames = {"Lightning", "Thunder", "Storm", "Tsunami", "Hurricane", "Tornado"};
        Color[] boatColors = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN};

        for (int i = 0; i < NUM_LANES; i++) {
            Boat boat = new Boat(boatNames[i], boatColors[i], i);
            boat.setPosition(50, LANE_HEIGHT * i + LANE_HEIGHT / 2);
            boats.add(boat);
        }

        // Set player boat
        boats.get(0).setPlayerControlled(true);
        player.setBoat(boats.get(0));
    }

    private void generatePowerUps() {
        Random rand = new Random();
        for (int i = 0; i < 15; i++) {
            double x = 200 + rand.nextDouble() * (TRACK_WIDTH - 400);
            double y = rand.nextDouble() * TRACK_HEIGHT;
            PowerUp.PowerUpType type = PowerUp.PowerUpType.values()[rand.nextInt(PowerUp.PowerUpType.values().length)];
            powerUps.add(new PowerUp(x, y, type));
        }
    }

    private void generateObstacles() {
        Random rand = new Random();
        for (int i = 0; i < 8; i++) {
            double x = 300 + rand.nextDouble() * (TRACK_WIDTH - 600);
            double y = rand.nextDouble() * TRACK_HEIGHT;
            obstacles.add(new Obstacle(x, y, Obstacle.ObstacleType.ROCK));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0;
        lastUpdateTime = currentTime;

        updateGame();
        repaint();
        frameCount++;
    }

    private void updateGame() {
        switch (currentState) {
            case MENU:
                updateMenu();
                break;
            case RACE_SETUP:
                updateRaceSetup();
                break;
            case RACING:
                updateRacing();
                break;
            case RACE_FINISHED:
                updateRaceFinished();
                break;
            case TOURNAMENT_RESULTS:
                updateTournamentResults();
                break;
        }

        updateAnimations();
        particleSystem.update(deltaTime);
    }

    private void updateMenu() {
        // Menu animations and logic
    }

    private void updateRaceSetup() {
        // Setup race parameters
    }

    private void updateRacing() {
        // Update boat positions
        for (Boat boat : boats) {
            boat.update(deltaTime);

            // Check collisions with power-ups
            checkPowerUpCollisions(boat);

            // Check collisions with obstacles
            checkObstacleCollisions(boat);
        }

        // Update AI boats
        updateAIBoats();

        // Check race completion
        checkRaceCompletion();

        // Update power-ups
        updatePowerUps();

        // Generate particle effects
        generateWakeEffects();
    }

    private void updateRaceFinished() {
        // Handle race completion logic
    }

    private void updateTournamentResults() {
        // Display tournament standings
    }

    private void updateAnimations() {
        waveOffset += deltaTime * 50;
        cloudOffset += deltaTime * 10;
    }

    private void updateAIBoats() {
        for (Boat boat : boats) {
            if (!boat.isPlayerControlled()) {
                // Simple AI: move forward with some randomness
                Random rand = new Random();
                double speedMultiplier = 0.8 + rand.nextDouble() * 0.4;
                boat.accelerate(speedMultiplier);

                // Occasionally change direction slightly
                if (rand.nextDouble() < 0.1) {
                    boat.steer((rand.nextDouble() - 0.5) * 0.5);
                }
            }
        }
    }

    private void checkPowerUpCollisions(Boat boat) {
        Iterator<PowerUp> iterator = powerUps.iterator();
        while (iterator.hasNext()) {
            PowerUp powerUp = iterator.next();
            if (boat.getBounds().intersects(powerUp.getBounds())) {
                boat.applyPowerUp(powerUp);
                iterator.remove();
                particleSystem.addExplosion(powerUp.getX(), powerUp.getY(), Color.YELLOW);
            }
        }
    }

    private void checkObstacleCollisions(Boat boat) {
        for (Obstacle obstacle : obstacles) {
            if (boat.getBounds().intersects(obstacle.getBounds())) {
                boat.handleCollision();
                particleSystem.addExplosion(obstacle.getX(), obstacle.getY(), Color.RED);
            }
        }
    }

    private void checkRaceCompletion() {
        for (Boat boat : boats) {
            if (boat.getX() >= TRACK_WIDTH - 100 && !boat.isFinished()) {
                boat.finish();
                tournament.recordFinish(boat);

                if (boat.isPlayerControlled()) {
                    currentState = GameState.RACE_FINISHED;
                }
            }
        }
    }

    private void updatePowerUps() {
        for (PowerUp powerUp : powerUps) {
            powerUp.update(deltaTime);
        }
    }

    private void generateWakeEffects() {
        for (Boat boat : boats) {
            if (boat.getSpeed() > 0.1) {
                particleSystem.addWakeParticle(
                        boat.getX() - 20,
                        boat.getY(),
                        Color.WHITE
                );
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        renderer.render(g2d, this);
    }

    // Key event handlers
    @Override
    public void keyPressed(KeyEvent e) {
        inputHandler.keyPressed(e, this);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        inputHandler.keyReleased(e, this);
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Getters for renderer
    public List<Boat> getBoats() { return boats; }
    public List<PowerUp> getPowerUps() { return powerUps; }
    public List<Obstacle> getObstacles() { return obstacles; }
    public ParticleSystem getParticleSystem() { return particleSystem; }
    public double getWaveOffset() { return waveOffset; }
    public double getCloudOffset() { return cloudOffset; }
    public GameState getCurrentState() { return currentState; }
    public Tournament getTournament() { return tournament; }
    public Player getPlayer() { return player; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BoatRacingTournament6());
    }
}

/**
 * Represents a racing boat with physics and abilities
 */
class Boat {
    private String name;
    private Color color;
    private double x, y;
    private double velocityX, velocityY;
    private double speed;
    private double direction;
    private int lane;
    private boolean playerControlled;
    private boolean finished;
    private long finishTime;
    private Map<PowerUp.PowerUpType, Double> activePowerUps;
    private double maxSpeed;
    private double acceleration;
    private double handling;

    public Boat(String name, Color color, int lane) {
        this.name = name;
        this.color = color;
        this.lane = lane;
        this.x = 50;
        this.y = 0;
        this.speed = 0;
        this.direction = 0;
        this.playerControlled = false;
        this.finished = false;
        this.activePowerUps = new HashMap<>();
        this.maxSpeed = 200 + Math.random() * 50; // Random max speed
        this.acceleration = 100 + Math.random() * 20;
        this.handling = 0.5 + Math.random() * 0.3;
    }

    public void update(double deltaTime) {
        // Apply power-up effects
        updatePowerUps(deltaTime);

        // Physics update
        x += velocityX * deltaTime;
        y += velocityY * deltaTime;

        // Apply friction
        velocityX *= 0.98;
        velocityY *= 0.98;

        // Keep boat in bounds
        y = Math.max(lane * (800 / 6) + 10, Math.min(y, (lane + 1) * (800 / 6) - 40));
    }

    public void accelerate(double throttle) {
        double currentMaxSpeed = maxSpeed;
        if (activePowerUps.containsKey(PowerUp.PowerUpType.SPEED_BOOST)) {
            currentMaxSpeed *= 1.5;
        }

        if (speed < currentMaxSpeed) {
            speed += acceleration * throttle * 0.016; // Assuming 60 FPS
            velocityX = Math.cos(direction) * speed;
            velocityY = Math.sin(direction) * speed;
        }
    }

    public void steer(double steerAmount) {
        direction += steerAmount * handling;
    }

    public void applyPowerUp(PowerUp powerUp) {
        activePowerUps.put(powerUp.getType(), 5.0); // 5 second duration
    }

    public void handleCollision() {
        speed *= 0.5; // Reduce speed on collision
        velocityX *= 0.5;
        velocityY *= 0.5;
    }

    public void finish() {
        finished = true;
        finishTime = System.currentTimeMillis();
    }

    private void updatePowerUps(double deltaTime) {
        Iterator<Map.Entry<PowerUp.PowerUpType, Double>> iterator = activePowerUps.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PowerUp.PowerUpType, Double> entry = iterator.next();
            entry.setValue(entry.getValue() - deltaTime);
            if (entry.getValue() <= 0) {
                iterator.remove();
            }
        }
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x, y, BoatRacingTournament6.BOAT_WIDTH, BoatRacingTournament6.BOAT_HEIGHT);
    }

    // Getters and setters
    public String getName() { return name; }
    public Color getColor() { return color; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getSpeed() { return speed; }
    public boolean isPlayerControlled() { return playerControlled; }
    public boolean isFinished() { return finished; }
    public long getFinishTime() { return finishTime; }
    public void setPlayerControlled(boolean playerControlled) { this.playerControlled = playerControlled; }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }
}

/**
 * Power-up system for temporary boat enhancements
 */
class PowerUp {
    public enum PowerUpType {
        SPEED_BOOST, SHIELD, TURBO, HANDLING_BOOST
    }

    private double x, y;
    private PowerUpType type;
    private double rotationAngle;
    private boolean collected;

    public PowerUp(double x, double y, PowerUpType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.rotationAngle = 0;
        this.collected = false;
    }

    public void update(double deltaTime) {
        rotationAngle += deltaTime * 180; // Rotate 180 degrees per second
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x - 15, y - 15, 30, 30);
    }

    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public PowerUpType getType() { return type; }
    public double getRotationAngle() { return rotationAngle; }
    public boolean isCollected() { return collected; }
    public void setCollected(boolean collected) { this.collected = collected; }
}

/**
 * Obstacles that slow down boats
 */
class Obstacle {
    public enum ObstacleType {
        ROCK, LOG, BUOY
    }

    private double x, y;
    private ObstacleType type;

    public Obstacle(double x, double y, ObstacleType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x - 20, y - 20, 40, 40);
    }

    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public ObstacleType getType() { return type; }
}

/**
 * Tournament management system
 */
class Tournament {
    private List<Boat> standings;
    private int currentRace;
    private int totalRaces;

    public Tournament() {
        this.standings = new ArrayList<>();
        this.currentRace = 1;
        this.totalRaces = 5;
    }

    public void recordFinish(Boat boat) {
        if (!standings.contains(boat)) {
            standings.add(boat);
        }
    }

    public List<Boat> getStandings() {
        return new ArrayList<>(standings);
    }

    public boolean isComplete() {
        return currentRace > totalRaces;
    }

    public void nextRace() {
        currentRace++;
        standings.clear();
    }

    // Getters
    public int getCurrentRace() { return currentRace; }
    public int getTotalRaces() { return totalRaces; }
}

/**
 * Player information and statistics
 */
class Player {
    private String name;
    private Boat boat;
    private int wins;
    private int totalRaces;

    public Player(String name) {
        this.name = name;
        this.wins = 0;
        this.totalRaces = 0;
    }

    public void recordWin() {
        wins++;
        totalRaces++;
    }

    public void recordRace() {
        totalRaces++;
    }

    // Getters and setters
    public String getName() { return name; }
    public Boat getBoat() { return boat; }
    public void setBoat(Boat boat) { this.boat = boat; }
    public int getWins() { return wins; }
    public int getTotalRaces() { return totalRaces; }
}

/**
 * Race track representation
 */
class RaceTrack {
    private int width;
    private int height;
    private List<Point> checkpoints;

    public RaceTrack() {
        this.width = 1000;
        this.height = 600;
        this.checkpoints = new ArrayList<>();
        generateCheckpoints();
    }

    private void generateCheckpoints() {
        // Generate checkpoints along the track
        for (int i = 1; i < 5; i++) {
            checkpoints.add(new Point(width * i / 5, height / 2));
        }
    }

    // Getters
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public List<Point> getCheckpoints() { return checkpoints; }
}

/**
 * Particle system for visual effects
 */
class ParticleSystem {
    private List<Particle> particles;

    public ParticleSystem() {
        this.particles = new ArrayList<>();
    }

    public void update(double deltaTime) {
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.update(deltaTime);
            if (particle.isDead()) {
                iterator.remove();
            }
        }
    }

    public void addWakeParticle(double x, double y, Color color) {
        particles.add(new Particle(x, y, color, 2.0));
    }

    public void addExplosion(double x, double y, Color color) {
        for (int i = 0; i < 10; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 50 + Math.random() * 100;
            particles.add(new Particle(x, y, color, 1.0,
                    Math.cos(angle) * speed, Math.sin(angle) * speed));
        }
    }

    public List<Particle> getParticles() {
        return particles;
    }
}

/**
 * Individual particle for effects
 */
class Particle {
    private double x, y;
    private double vx, vy;
    private Color color;
    private double life;
    private double maxLife;

    public Particle(double x, double y, Color color, double life) {
        this(x, y, color, life, 0, 0);
    }

    public Particle(double x, double y, Color color, double life, double vx, double vy) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.life = life;
        this.maxLife = life;
        this.vx = vx;
        this.vy = vy;
    }

    public void update(double deltaTime) {
        x += vx * deltaTime;
        y += vy * deltaTime;
        life -= deltaTime;

        // Apply gravity to explosion particles
        if (vx != 0 || vy != 0) {
            vy += 100 * deltaTime; // Gravity
        }
    }

    public boolean isDead() {
        return life <= 0;
    }

    public Color getCurrentColor() {
        float alpha = (float) (life / maxLife);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                (int) (255 * alpha));
    }

    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
}

/**
 * Sound management system (placeholder for actual sound implementation)
 */
class SoundManager {
    public void playEngineSound() {
        // Placeholder for engine sound
    }

    public void playCollisionSound() {
        // Placeholder for collision sound
    }

    public void playPowerUpSound() {
        // Placeholder for power-up sound
    }
}

/**
 * Game rendering system
 */
class GameRenderer {
    public void render(Graphics2D g2d, BoatRacingTournament game) {
        switch (game.getCurrentState()) {
            case MENU:
                renderMenu(g2d, game);
                break;
            case RACING:
                renderRace(g2d, game);
                break;
            case RACE_FINISHED:
                renderRaceFinished(g2d, game);
                break;
            default:
                renderRace(g2d, game);
                break;
        }
    }

    private void renderMenu(Graphics2D g2d, BoatRacingTournament game) {
        // Render main menu
        g2d.setColor(new Color(135, 206, 235)); // Sky blue
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g2d.getFontMetrics();
        String title = "Boat Racing Tournament";
        g2d.drawString(title, (WINDOW_WIDTH - fm.stringWidth(title)) / 2, 200);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Press SPACE to Start", (WINDOW_WIDTH - g2d.getFontMetrics().stringWidth("Press SPACE to Start")) / 2, 400);
    }

    private void renderRace(Graphics2D g2d, BoatRacingTournament game) {
        // Render water background
        renderWater(g2d, game.getWaveOffset());

        // Render clouds
        renderClouds(g2d, game.getCloudOffset());

        // Render lane dividers
        renderLanes(g2d);

        // Render obstacles
        for (Obstacle obstacle : game.getObstacles()) {
            renderObstacle(g2d, obstacle);
        }

        // Render power-ups
        for (PowerUp powerUp : game.getPowerUps()) {
            renderPowerUp(g2d, powerUp);
        }

        // Render boats
        for (Boat boat : game.getBoats()) {
            renderBoat(g2d, boat);
        }

        // Render particles
        for (Particle particle : game.getParticleSystem().getParticles()) {
            renderParticle(g2d, particle);
        }

        // Render UI
        renderUI(g2d, game);
    }

    private void renderWater(Graphics2D g2d, double waveOffset) {
        // Create water gradient
        GradientPaint waterGradient = new GradientPaint(0, 0, new Color(0, 119, 190),
                0, WINDOW_HEIGHT, new Color(0, 82, 147));
        g2d.setPaint(waterGradient);
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Draw wave patterns
        g2d.setColor(new Color(255, 255, 255, 30));
        for (int i = 0; i < WINDOW_WIDTH; i += 20) {
            double waveHeight = Math.sin((i + waveOffset) * 0.02) * 10;
            g2d.drawLine(i, (int) (WINDOW_HEIGHT / 2 + waveHeight),
                    i + 10, (int) (WINDOW_HEIGHT / 2 + waveHeight + 5));
        }
    }

    private void renderClouds(Graphics2D g2d, double cloudOffset) {
        g2d.setColor(new Color(255, 255, 255, 180));
        for (int i = 0; i < 5; i++) {
            int x = (int) ((i * 200 - cloudOffset) % (BoatRacingTournament6.WINDOW_WIDTH + 100));
            int y = 50 + i * 20;
            g2d.fillOval(x, y, 80, 40);
            g2d.fillOval(x + 20, y - 10, 60, 30);
            g2d.fillOval(x + 40, y, 70, 35);
        }
    }

    private void renderLanes(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{10, 10}, 0));
        for (int i = 1; i < BoatRacingTournament6.NUM_LANES; i++) {
            int y = i * BoatRacingTournament6.LANE_HEIGHT;
            g2d.drawLine(0, y, BoatRacingTournament6.TRACK_WIDTH, y);
        }
        g2d.setStroke(new BasicStroke(1));
    }

    private void renderBoat(Graphics2D g2d, Boat boat) {
        AffineTransform oldTransform = g2d.getTransform();

        // Translate to boat position
        g2d.translate(boat.getX(), boat.getY());

        // Draw boat hull
        g2d.setColor(boat.getColor());
        int[] xPoints = {0, 50, 60, 50, 0};
        int[] yPoints = {15, 0, 15, 30, 15};
        g2d.fillPolygon(xPoints, yPoints, 5);

        // Draw boat outline
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawPolygon(xPoints, yPoints, 5);

        // Draw boat name
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString(boat.getName(), 5, 20);

        g2d.setTransform(oldTransform);
    }

    private void renderPowerUp(Graphics2D g2d, PowerUp powerUp) {
        if (powerUp.isCollected()) return;

        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(powerUp.getX(), powerUp.getY());
        g2d.rotate(Math.toRadians(powerUp.getRotationAngle()));

        Color powerUpColor;
        switch (powerUp.getType()) {
            case SPEED_BOOST: powerUpColor = Color.RED; break;
            case SHIELD: powerUpColor = Color.BLUE; break;
            case TURBO: powerUpColor = Color.ORANGE; break;
            default: powerUpColor = Color.GREEN; break;
        }

        g2d.setColor(powerUpColor);
        g2d.fillOval(-10, -10, 20, 20);
        g2d.setColor(Color.WHITE);
        g2d.drawOval(-10, -10, 20, 20);

        g2d.setTransform(oldTransform);
    }

    private void renderObstacle(Graphics2D g2d, Obstacle obstacle) {
        g2d.setColor(Color.GRAY);
        g2d.fillOval((int) obstacle.getX() - 15, (int) obstacle.getY() - 15, 30, 30);
        g2d.setColor(Color.BLACK);
        g2d.drawOval((int) obstacle.getX() - 15, (int) obstacle.getY() - 15, 30, 30);
    }

    private void renderParticle(Graphics2D g2d, Particle particle) {
        g2d.setColor(particle.getCurrentColor());
        g2d.fillOval((int) particle.getX() - 2, (int) particle.getY() - 2, 4, 4);
    }

    private void renderUI(Graphics2D g2d, BoatRacingTournament6 game) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));

        // Render speed for player boat
        Boat playerBo;