import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class VirtualMotorsportRacingGame extends JFrame {
    private GamePanel gamePanel;
    private boolean gameStarted = false;

    public VirtualMotorsportRacingGame() {
        setTitle("Virtual Motorsport Racing Championship");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VirtualMotorsportRacingGame());
    }
}

class GamePanel extends JPanel implements KeyListener {
    private static final int PANEL_WIDTH = 1200;
    private static final int PANEL_HEIGHT = 800;
    private static final int FPS = 60;
    private static final int TRACK_WIDTH = 600;
    private static final int TRACK_HEIGHT = 700;
    private static final int TRACK_X = 300;
    private static final int TRACK_Y = 50;

    private Timer gameTimer;
    private PlayerCar playerCar;
    private List<AIRacer> aiRacers;
    private Track raceTrack;
    private RaceManager raceManager;
    private ParticleSystem particleSystem;
    private PowerUpManager powerUpManager;
    private HUD hud;
    private boolean isPaused = false;
    private GameState gameState;

    private enum GameState {
        MENU, RACING, PAUSED, FINISHED
    }

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(new Color(20, 20, 30));
        setFocusable(true);
        addKeyListener(this);

        gameState = GameState.MENU;
        initializeGame();

        gameTimer = new Timer(1000 / FPS, e -> {
            if (gameState == GameState.RACING) {
                updateGame();
            }
            repaint();
        });
        gameTimer.start();
    }

    private void initializeGame() {
        raceTrack = new Track(TRACK_X, TRACK_Y, TRACK_WIDTH, TRACK_HEIGHT);
        playerCar = new PlayerCar(TRACK_X + TRACK_WIDTH / 2 - 50, TRACK_Y + TRACK_HEIGHT - 150);

        aiRacers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            aiRacers.add(new AIRacer(
                TRACK_X + TRACK_WIDTH / 2 - 150 + i * 60,
                TRACK_Y + TRACK_HEIGHT - 150 - (i + 1) * 80,
                i
            ));
        }

        raceManager = new RaceManager(playerCar, aiRacers);
        particleSystem = new ParticleSystem();
        powerUpManager = new PowerUpManager(raceTrack);
        hud = new HUD();
    }

    private void updateGame() {
        if (isPaused) return;

        playerCar.update(raceTrack);

        for (AIRacer ai : aiRacers) {
            ai.update(raceTrack);
        }

        checkCollisions();
        particleSystem.update();
        powerUpManager.update(playerCar, aiRacers);
        raceManager.update();

        if (raceManager.isRaceFinished()) {
            gameState = GameState.FINISHED;
        }
    }

    private void checkCollisions() {
        Rectangle playerBounds = playerCar.getBounds();

        for (AIRacer ai : aiRacers) {
            if (playerBounds.intersects(ai.getBounds())) {
                handleCarCollision(playerCar, ai);
                createCollisionEffect(playerCar.getX(), playerCar.getY());
            }
        }

        for (int i = 0; i < aiRacers.size(); i++) {
            for (int j = i + 1; j < aiRacers.size(); j++) {
                if (aiRacers.get(i).getBounds().intersects(aiRacers.get(j).getBounds())) {
                    handleCarCollision(aiRacers.get(i), aiRacers.get(j));
                }
            }
        }
    }

    private void handleCarCollision(RaceCar car1, RaceCar car2) {
        double angle = Math.atan2(car2.getY() - car1.getY(), car2.getX() - car1.getX());
        double separation = 30;

        car1.setX(car1.getX() - Math.cos(angle) * separation / 2);
        car1.setY(car1.getY() - Math.sin(angle) * separation / 2);
        car2.setX(car2.getX() + Math.cos(angle) * separation / 2);
        car2.setY(car2.getY() + Math.sin(angle) * separation / 2);

        car1.setSpeed(car1.getSpeed() * 0.7);
        car2.setSpeed(car2.getSpeed() * 0.7);
    }

    private void createCollisionEffect(double x, double y) {
        for (int i = 0; i < 20; i++) {
            particleSystem.addParticle(new Particle(x, y, Color.ORANGE));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (gameState) {
            case MENU:
                drawMenu(g2d);
                break;
            case RACING:
            case PAUSED:
                drawGame(g2d);
                if (isPaused) {
                    drawPauseOverlay(g2d);
                }
                break;
            case FINISHED:
                drawGame(g2d);
                drawFinishScreen(g2d);
                break;
        }
    }

    private void drawMenu(Graphics2D g2d) {
        g2d.setColor(new Color(30, 30, 40));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2d.setColor(new Color(255, 215, 0));
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String title = "MOTORSPORT RACING";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (PANEL_WIDTH - fm.stringWidth(title)) / 2;
        g2d.drawString(title, x, 200);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 30));
        String subtitle = "Championship Edition";
        fm = g2d.getFontMetrics();
        x = (PANEL_WIDTH - fm.stringWidth(subtitle)) / 2;
        g2d.drawString(subtitle, x, 260);

        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("Press SPACE to Start", PANEL_WIDTH / 2 - 140, 400);

        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("Controls:", PANEL_WIDTH / 2 - 100, 500);
        g2d.drawString("Arrow Keys - Steer", PANEL_WIDTH / 2 - 100, 530);
        g2d.drawString("UP Arrow - Accelerate", PANEL_WIDTH / 2 - 100, 560);
        g2d.drawString("DOWN Arrow - Brake", PANEL_WIDTH / 2 - 100, 590);
        g2d.drawString("P - Pause", PANEL_WIDTH / 2 - 100, 620);

        drawAnimatedCar(g2d, PANEL_WIDTH / 2 - 40, 320);
    }

    private void drawAnimatedCar(Graphics2D g2d, int x, int y) {
        g2d.setColor(new Color(220, 20, 60));
        int[] xPoints = {x, x + 80, x + 80, x};
        int[] yPoints = {y, y - 10, y + 30, y + 40};
        g2d.fillPolygon(xPoints, yPoints, 4);

        g2d.setColor(new Color(100, 100, 255));
        g2d.fillRect(x + 10, y + 5, 25, 25);

        g2d.setColor(Color.BLACK);
        g2d.fillOval(x + 5, y + 35, 15, 15);
        g2d.fillOval(x + 60, y + 35, 15, 15);
    }

    private void drawGame(Graphics2D g2d) {
        raceTrack.draw(g2d);
        powerUpManager.draw(g2d);

        List<RaceCar> allCars = new ArrayList<>(aiRacers);
        allCars.add(playerCar);
        allCars.sort(Comparator.comparingDouble(RaceCar::getY).reversed());

        for (RaceCar car : allCars) {
            car.draw(g2d);
        }

        particleSystem.draw(g2d);
        hud.draw(g2d, raceManager, playerCar);
    }

    private void drawPauseOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String text = "PAUSED";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (PANEL_WIDTH - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, PANEL_HEIGHT / 2);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        text = "Press P to Resume";
        fm = g2d.getFontMetrics();
        x = (PANEL_WIDTH - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, PANEL_HEIGHT / 2 + 50);
    }

    private void drawFinishScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2d.setColor(new Color(255, 215, 0));
        g2d.setFont(new Font("Arial", Font.BOLD, 56));
        String text = "RACE FINISHED!";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (PANEL_WIDTH - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, 250);

        int position = raceManager.getPlayerPosition();
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        text = "Your Position: " + position + getPositionSuffix(position);
        fm = g2d.getFontMetrics();
        x = (PANEL_WIDTH - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, 330);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        text = "Press R to Restart";
        fm = g2d.getFontMetrics();
        x = (PANEL_WIDTH - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, 400);
    }

    private String getPositionSuffix(int position) {
        if (position == 1) return "st";
        if (position == 2) return "nd";
        if (position == 3) return "rd";
        return "th";
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (gameState == GameState.MENU && key == KeyEvent.VK_SPACE) {
            gameState = GameState.RACING;
            return;
        }

        if (gameState == GameState.FINISHED && key == KeyEvent.VK_R) {
            initializeGame();
            gameState = GameState.RACING;
            return;
        }

        if (gameState == GameState.RACING && key == KeyEvent.VK_P) {
            isPaused = !isPaused;
            gameState = isPaused ? GameState.PAUSED : GameState.RACING;
            return;
        }

        if (gameState == GameState.RACING) {
            playerCar.keyPressed(key);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameState == GameState.RACING) {
            playerCar.keyReleased(e.getKeyCode());
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

abstract class RaceCar {
    protected double x, y;
    protected double speed;
    protected double maxSpeed;
    protected double acceleration;
    protected double deceleration;
    protected double angle;
    protected Color bodyColor;
    protected int width = 40;
    protected int height = 60;
    protected int lapCount = 0;
    protected boolean hasBoost = false;
    protected int boostTimer = 0;

    public RaceCar(double x, double y, Color color) {
        this.x = x;
        this.y = y;
        this.bodyColor = color;
        this.speed = 0;
        this.angle = 0;
    }

    public abstract void update(Track track);

    public void draw(Graphics2D g2d) {
        AffineTransform old = g2d.getTransform();
        g2d.translate(x, y);
        g2d.rotate(angle);

        if (hasBoost) {
            g2d.setColor(new Color(255, 165, 0, 150));
            for (int i = 0; i < 3; i++) {
                g2d.fillOval(-width / 2 - 10, height / 2 + i * 8, 10, 10);
            }
        }

        g2d.setColor(bodyColor);
        int[] xPoints = {-width / 2, width / 2, width / 2, -width / 2};
        int[] yPoints = {-height / 2, -height / 2 + 10, height / 2, height / 2};
        g2d.fillPolygon(xPoints, yPoints, 4);

        g2d.setColor(new Color(100, 150, 255));
        g2d.fillRect(-width / 3, -height / 3, width * 2 / 3, height / 3);

        g2d.setColor(Color.BLACK);
        g2d.fillOval(-width / 2 + 5, height / 3, 12, 12);
        g2d.fillOval(width / 2 - 17, height / 3, 12, 12);

        g2d.setTransform(old);
    }

    public Rectangle getBounds() {
        return new Rectangle((int) (x - width / 2), (int) (y - height / 2), width, height);
    }

    public void applyBoost() {
        hasBoost = true;
        boostTimer = 180;
        speed = Math.min(speed + 3, maxSpeed * 1.5);
    }

    protected void updateBoost() {
        if (hasBoost) {
            boostTimer--;
            if (boostTimer <= 0) {
                hasBoost = false;
            }
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public int getLapCount() { return lapCount; }
    public void incrementLap() { lapCount++; }
}

class PlayerCar extends RaceCar {
    private boolean upPressed, downPressed, leftPressed, rightPressed;

    public PlayerCar(double x, double y) {
        super(x, y, new Color(220, 20, 60));
        this.maxSpeed = 8;
        this.acceleration = 0.15;
        this.deceleration = 0.1;
    }

    @Override
    public void update(Track track) {
        updateBoost();

        if (upPressed) {
            speed += acceleration;
            if (speed > maxSpeed) {
                speed = maxSpeed;
            }
        } else if (downPressed) {
            speed -= deceleration * 2;
            if (speed < -maxSpeed / 2) {
                speed = -maxSpeed / 2;
            }
        } else {
            if (speed > 0) {
                speed -= deceleration;
                if (speed < 0) speed = 0;
            } else if (speed < 0) {
                speed += deceleration;
                if (speed > 0) speed = 0;
            }
        }

        double turnSpeed = 0.05 * (speed / maxSpeed);
        if (leftPressed && Math.abs(speed) > 0.5) {
            angle -= turnSpeed;
        }
        if (rightPressed && Math.abs(speed) > 0.5) {
            angle += turnSpeed;
        }

        double dx = Math.sin(angle) * speed;
        double dy = -Math.cos(angle) * speed;

        double newX = x + dx;
        double newY = y + dy;

        if (track.isWithinBounds(newX, newY)) {
            x = newX;
            y = newY;
        } else {
            speed *= 0.5;
        }
    }

    public void keyPressed(int key) {
        if (key == KeyEvent.VK_UP) upPressed = true;
        if (key == KeyEvent.VK_DOWN) downPressed = true;
        if (key == KeyEvent.VK_LEFT) leftPressed = true;
        if (key == KeyEvent.VK_RIGHT) rightPressed = true;
    }

    public void keyReleased(int key) {
        if (key == KeyEvent.VK_UP) upPressed = false;
        if (key == KeyEvent.VK_DOWN) downPressed = false;
        if (key == KeyEvent.VK_LEFT) leftPressed = false;
        if (key == KeyEvent.VK_RIGHT) rightPressed = false;
    }
}

class AIRacer extends RaceCar {
    private int racerID;
    private double targetAngle;
    private Random random;
    private int behaviorTimer;

    public AIRacer(double x, double y, int id) {
        super(x, y, generateRandomColor(id));
        this.racerID = id;
        this.maxSpeed = 6 + random.nextDouble() * 2;
        this.acceleration = 0.1;
        this.deceleration = 0.08;
        this.random = new Random(id);
        this.behaviorTimer = 0;
    }

    private static Color generateRandomColor(int id) {
        Color[] colors = {
            new Color(50, 150, 255),
            new Color(50, 200, 50),
            new Color(255, 200, 50),
            new Color(200, 50, 200),
            new Color(50, 255, 200)
        };
        return colors[id % colors.length];
    }

    @Override
    public void update(Track track) {
        updateBoost();
        behaviorTimer++;

        if (behaviorTimer % 60 == 0) {
            targetAngle = angle + (random.nextDouble() - 0.5) * 0.3;
        }

        speed += acceleration;
        if (speed > maxSpeed) {
            speed = maxSpeed;
        }

        double angleDiff = targetAngle - angle;
        if (Math.abs(angleDiff) > 0.01) {
            angle += Math.signum(angleDiff) * 0.02;
        }

        double dx = Math.sin(angle) * speed;
        double dy = -Math.cos(angle) * speed;

        double newX = x + dx;
        double newY = y + dy;

        if (track.isWithinBounds(newX, newY)) {
            x = newX;
            y = newY;
        } else {
            angle += Math.PI / 4;
            speed *= 0.7;
        }
    }
}

class Track {
    private int x, y, width, height;
    private int innerMargin = 80;

    public Track(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(40, 80, 40));
        g2d.fillRect(x - 50, y - 50, width + 100, height + 100);

        g2d.setColor(new Color(60, 60, 70));
        g2d.fillRect(x, y, width, height);

        g2d.setColor(new Color(80, 80, 90));
        g2d.fillRect(x + innerMargin, y + innerMargin,
                     width - innerMargin * 2, height - innerMargin * 2);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT,
                      BasicStroke.JOIN_BEVEL, 0, new float[]{15, 15}, 0));
        g2d.drawRect(x + width / 2 - innerMargin, y + innerMargin,
                     innerMargin * 2, height - innerMargin * 2);

        g2d.setColor(new Color(255, 215, 0));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(x + 50, y + height - 100, x + width - 50, y + height - 100);

        for (int i = 0; i < 8; i++) {
            g2d.fillRect(x + 60 + i * 70, y + height - 110, 40, 20);
        }
    }

    public boolean isWithinBounds(double px, double py) {
        if (px < x + 20 || px > x + width - 20 || py < y + 20 || py > y + height - 20) {
            return false;
        }

        if (px > x + innerMargin + 20 && px < x + width - innerMargin - 20 &&
            py > y + innerMargin + 20 && py < y + height - innerMargin - 20) {
            return false;
        }

        return true;
    }
}

class RaceManager {
    private PlayerCar player;
    private List<AIRacer> aiRacers;
    private int totalLaps = 3;
    private boolean raceFinished = false;

    public RaceManager(PlayerCar player, List<AIRacer> aiRacers) {
        this.player = player;
        this.aiRacers = aiRacers;
    }

    public void update() {
        if (player.getLapCount() >= totalLaps) {
            raceFinished = true;
        }
    }

    public int getPlayerPosition() {
        int position = 1;
        for (AIRacer ai : aiRacers) {
            if (ai.getLapCount() > player.getLapCount() ||
                (ai.getLapCount() == player.getLapCount() && ai.getY() < player.getY())) {
                position++;
            }
        }
        return position;
    }

    public boolean isRaceFinished() {
        return raceFinished;
    }

    public int getTotalLaps() {
        return totalLaps;
    }
}

class Particle {
    private double x, y;
    private double vx, vy;
    private Color color;
    private int lifetime;
    private int maxLifetime;

    public Particle(double x, double y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
        Random rand = new Random();
        this.vx = (rand.nextDouble() - 0.5) * 4;
        this.vy = (rand.nextDouble() - 0.5) * 4;
        this.maxLifetime = 30 + rand.nextInt(30);
        this.lifetime = 0;
    }

    public void update() {
        x += vx;
        y += vy;
        vy += 0.1;
        lifetime++;
    }

    public void draw(Graphics2D g2d) {
        float alpha = 1.0f - (float) lifetime / maxLifetime;
        g2d.setColor(new Color(color.getRed(), color.getGreen(),
                               color.getBlue(), (int) (alpha * 255)));
        g2d.fillOval((int) x, (int) y, 6, 6);
    }

    public boolean isDead() {
        return lifetime >= maxLifetime;
    }
}

class ParticleSystem {
    private List<Particle> particles;

    public ParticleSystem() {
        particles = new ArrayList<>();
    }

    public void addParticle(Particle p) {
        particles.add(p);
    }

    public void update() {
        particles.removeIf(Particle::isDead);
        for (Particle p : particles) {
            p.update();
        }
    }

    public void draw(Graphics2D g2d) {
        for (Particle p : particles) {
            p.draw(g2d);
        }
    }
}

class PowerUp {
    private double x, y;
    private int size = 20;
    private Color color;
    private boolean collected = false;

    public PowerUp(double x, double y) {
        this.x = x;
        this.y = y;
        this.color = new Color(255, 215, 0);
    }

    public void draw(Graphics2D g2d) {
        if (!collected) {
            g2d.setColor(color);
            g2d.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("B", (int) x - 5, (int) y + 5);
        }
    }

    public boolean checkCollision(RaceCar car) {
        if (!collected) {
            double dx = car.getX() - x;
            double dy = car.getY() - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance < 30) {
                collected = true;
                return true;
            }
        }
        return false;
    }

    public boolean isCollected() {
        return collected;
    }
}

class PowerUpManager {
    private List<PowerUp> powerUps;
    private Track track;
    private Random random;
    private int spawnTimer;

    public PowerUpManager(Track track) {
        this.track = track;
        this.powerUps = new ArrayList<>();
        this.random = new Random();
        this.spawnTimer = 0;
    }

    public void update(PlayerCar player, List<AIRacer> aiRacers) {
        spawnTimer++;
        if (spawnTimer > 300 && powerUps.size() < 3) {
            spawnPowerUp();
            spawnTimer = 0;
        }

        powerUps.removeIf(PowerUp::isCollected);

        for (PowerUp powerUp : powerUps) {
            if (powerUp.checkCollision(player)) {
                player.applyBoost();
            }

            for (AIRacer ai : aiRacers) {
                if (powerUp.checkCollision(ai)) {
                    ai.applyBoost();
                }
            }
        }
    }

    private void spawnPowerUp() {
        double x = 350 + random.nextDouble() * 500;
        double y = 100 + random.nextDouble() * 600;
        powerUps.add(new PowerUp(x, y));
    }

    public void draw(Graphics2D g2d) {
        for (PowerUp powerUp : powerUps) {
            powerUp.draw(g2d);
        }
    }
}

class HUD {
    public void draw(Graphics2D g2d, RaceManager raceManager, PlayerCar player) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(10, 10, 280, 120);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Position: " + raceManager.getPlayerPosition(), 20, 35);
        g2d.drawString("Lap: " + player.getLapCount() + " / " +
                       raceManager.getTotalLaps(), 20, 65);

        int speedPercent = (int) ((player.getSpeed() / 8.0) * 100);
        g2d.drawString("Speed: " + speedPercent + "%", 20, 95);

        g2d.setColor(new Color(255, 215, 0));
        g2d.fillRect(20, 105, speedPercent * 2, 15);
        g2d.setColor(Color.WHITE);
        g2g.drawRect(20, 105, 200, 15);
    }
}
