import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GlobalRacingTourGame extends JFrame {
    public GlobalRacingTourGame() {
        setTitle("Global Racing Tour");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GlobalRacingTourGame());
    }
}

class GamePanel extends JPanel implements Runnable, KeyListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private Thread gameThread;
    private boolean running;

    // Game States
    public enum GameState { MENU, PLAYING, GAMEOVER, TRANSITION }
    private GameState currentState;

    // Core Components
    private PlayerCar player;
    private ArrayList<EnemyCar> enemies;
    private Road road;
    private TourManager tourManager;
    private ParticleSystem particleSystem;
    private HUD hud;
    private InputHandler input;

    // Game Variables
    private double distanceTraveled;
    private int score;
    private double screenShakeX, screenShakeY;
    private double transitionAlpha;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        input = new InputHandler();
        tourManager = new TourManager();
        road = new Road(tourManager);
        particleSystem = new ParticleSystem();
        hud = new HUD();
        enemies = new ArrayList<>();

        resetGame();
        currentState = GameState.MENU;
    }

    private void resetGame() {
        player = new PlayerCar(WIDTH / 2, HEIGHT - 150);
        enemies.clear();
        particleSystem.clear();
        distanceTraveled = 0;
        score = 0;
        tourManager.reset();
        road.reset();
        screenShakeX = 0;
        screenShakeY = 0;
        transitionAlpha = 0;
    }

    public void start() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (gameThread == null) {
            start();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    private void update() {
        if (currentState == GameState.PLAYING) {
            player.update(input, road.getScrollSpeed());
            road.update(player.getSpeed());
            tourManager.update(distanceTraveled);

            distanceTraveled += player.getSpeed() * 0.1;
            score += (int)(player.getSpeed() * 0.5);

            spawnEnemies();
            updateEnemies();
            particleSystem.update();

            checkCollisions();

            // Decay screen shake
            screenShakeX *= 0.9;
            screenShakeY *= 0.9;

            // Check for tour transition
            if (tourManager.isTransitioning()) {
                currentState = GameState.TRANSITION;
                transitionAlpha = 0;
            }
        }
        else if (currentState == GameState.TRANSITION) {
            transitionAlpha += 0.05;
            if (transitionAlpha >= 1.0) {
                transitionAlpha = 1.0;
                tourManager.completeTransition();
            }
            if (tourManager.isFadingBack()) {
                transitionAlpha -= 0.05;
                if (transitionAlpha <= 0) {
                    transitionAlpha = 0;
                    currentState = GameState.PLAYING;
                }
            }
        }
        else if (currentState == GameState.MENU) {
            road.update(5); // Slow scroll for menu background
        }
    }

    private void spawnEnemies() {
        if (Math.random() < 0.02 + (distanceTraveled * 0.00001)) {
            int lane = (int)(Math.random() * 3);
            int x = 200 + (lane * 150);
            enemies.add(new EnemyCar(x, -100));
        }
    }

    private void updateEnemies() {
        Iterator<EnemyCar> it = enemies.iterator();
        while (it.hasNext()) {
            EnemyCar e = it.next();
            e.update(road.getScrollSpeed());
            if (e.getY() > HEIGHT + 100) {
                it.remove();
            }
        }
    }

    private void checkCollisions() {
        Rectangle playerBounds = player.getBounds();
        for (EnemyCar e : enemies) {
            if (playerBounds.intersects(e.getBounds())) {
                triggerCrash(e);
            }
        }
    }

    private void triggerCrash(EnemyCar e) {
        currentState = GameState.GAMEOVER;
        screenShakeX = 15;
        screenShakeY = 15;

        // Explosion particles
        for (int i = 0; i < 50; i++) {
            particleSystem.addParticle(e.getX() + e.getWidth()/2, e.getY() + e.getHeight()/2, true);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Apply screen shake
        g2d.translate((Math.random() - 0.5) * screenShakeX, (Math.random() - 0.5) * screenShakeY);

        road.draw(g2d);

        if (currentState == GameState.PLAYING || currentState == GameState.GAMEOVER || currentState == GameState.TRANSITION) {
            for (EnemyCar e : enemies) e.draw(g2d);
            player.draw(g2d);
            particleSystem.draw(g2d);
            hud.draw(g2d, score, distanceTraveled, tourManager.getCurrentLocation(), player.getSpeed());
        }

        if (currentState == GameState.MENU) {
            drawMenu(g2d);
        }

        if (currentState == GameState.GAMEOVER) {
            drawGameOver(g2d);
        }

        if (currentState == GameState.TRANSITION) {
            g2d.setColor(new Color(0, 0, 0, (float)transitionAlpha));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            if (transitionAlpha > 0.5) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 40));
                String city = tourManager.getNextLocationName();
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (WIDTH - fm.stringWidth(city)) / 2;
                g2d.drawString(city, textX, HEIGHT / 2);
            }
        }

        g2d.dispose();
    }

    private void drawMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 60));
        String title = "GLOBAL RACING TOUR";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, 200);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 24));
        String start = "Press ENTER to Start";
        fm = g2d.getFontMetrics();
        g2d.drawString(start, (WIDTH - fm.stringWidth(start)) / 2, 350);

        String controls = "Use ARROW KEYS to steer and accelerate";
        fm = g2d.getFontMetrics();
        g2d.drawString(controls, (WIDTH - fm.stringWidth(controls)) / 2, 400);
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.RED);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 70));
        String title = "CRASHED!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, 250);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 30));
        String scoreText = "Final Score: " + score;
        fm = g2d.getFontMetrics();
        g2d.drawString(scoreText, (WIDTH - fm.stringWidth(scoreText)) / 2, 350);

        String restart = "Press ENTER to Restart";
        fm = g2d.getFontMetrics();
        g2d.drawString(restart, (WIDTH - fm.stringWidth(restart)) / 2, 450);
    }

    @Override
    public void keyPressed(KeyEvent e) { input.pressed(e.getKeyCode()); }
    @Override
    public void keyReleased(KeyEvent e) {
        input.released(e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (currentState == GameState.MENU || currentState == GameState.GAMEOVER) {
                resetGame();
                currentState = GameState.PLAYING;
            }
        }
    }
    @Override
    public void keyTyped(KeyEvent e) {}
}

class InputHandler {
    public boolean up, down, left, right;
    public void pressed(int code) {
        if (code == KeyEvent.VK_UP) up = true;
        if (code == KeyEvent.VK_DOWN) down = true;
        if (code == KeyEvent.VK_LEFT) left = true;
        if (code == KeyEvent.VK_RIGHT) right = true;
    }
    public void released(int code) {
        if (code == KeyEvent.VK_UP) up = false;
        if (code == KeyEvent.VK_DOWN) down = false;
        if (code == KeyEvent.VK_LEFT) left = false;
        if (code == KeyEvent.VK_RIGHT) right = false;
    }
}

class PlayerCar {
    private double x, y, width, height;
    private double speed, maxSpeed, acceleration;
    private double wheelRotation;

    public PlayerCar(double x, double y) {
        this.x = x;
        this.y = y;
        this.width = 40;
        this.height = 80;
        this.speed = 0;
        this.maxSpeed = 12;
        this.acceleration = 0.15;
    }

    public void update(InputHandler input, double roadSpeed) {
        if (input.up) {
            speed += acceleration;
            if (speed > maxSpeed) speed = maxSpeed;
        } else if (input.down) {
            speed -= acceleration * 2;
            if (speed < 0) speed = 0;
        } else {
            speed -= 0.05;
            if (speed < 0) speed = 0;
        }

        double turnSpeed = 5 * (speed / maxSpeed);
        if (input.left) x -= turnSpeed;
        if (input.right) x += turnSpeed;

        // Boundaries
        if (x < 120) x = 120;
        if (x > 640) x = 640;

        // Wheel animation
        wheelRotation += speed * 0.5;
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, (int)width, (int)height);
    }

    public double getSpeed() { return speed; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

    public void draw(Graphics2D g2d) {
        g2d.translate(x + width/2, y + height/2);

        // Car Body (Sports Car Shape)
        Path2D body = new Path2D.Double();
        body.moveTo(-15, -35);
        body.lineTo(15, -35);
        body.quadTo(20, -20, 20, 0);
        body.lineTo(20, 25);
        body.quadTo(20, 35, 10, 35);
        body.lineTo(-10, 35);
        body.quadTo(-20, 35, -20, 25);
        body.lineTo(-20, 0);
        body.quadTo(-20, -20, -15, -35);
        body.closePath();

        GradientPaint gp = new GradientPaint(0, -35, new Color(220, 20, 60), 0, 35, new Color(139, 0, 0));
        g2d.setPaint(gp);
        g2d.fill(body);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(body);

        // Windshield
        Path2D glass = new Path2D.Double();
        glass.moveTo(-12, -15);
        glass.lineTo(12, -15);
        glass.lineTo(15, 5);
        glass.lineTo(-15, 5);
        glass.closePath();
        g2d.setColor(new Color(135, 206, 250, 200));
        g2d.fill(glass);
        g2d.setColor(Color.BLACK);
        g2d.draw(glass);

        // Rear Window
        Path2D rearGlass = new Path2D.Double();
        rearGlass.moveTo(-10, 15);
        rearGlass.lineTo(10, 15);
        rearGlass.lineTo(12, 25);
        rearGlass.lineTo(-12, 25);
        rearGlass.closePath();
        g2d.setColor(new Color(135, 206, 250, 200));
        g2d.fill(rearGlass);
        g2d.draw(rearGlass);

        // Wheels
        drawWheel(g2d, -22, -20);
        drawWheel(g2d, 14, -20);
        drawWheel(g2d, -22, 15);
        drawWheel(g2d, 14, 15);

        // Headlights
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(-12, -38, 6, 4);
        g2d.fillOval(6, -38, 6, 4);

        // Taillights
        g2d.setColor(Color.RED);
        g2d.fillOval(-15, 32, 8, 4);
        g2d.fillOval(7, 32, 8, 4);

        g2d.translate(-(x + width/2), -(y + height/2));
    }

    private void drawWheel(Graphics2D g2d, double wx, double wy) {
        g2d.translate(wx + 4, wy + 7);
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRoundRect(-4, -7, 8, 14, 4, 4);

        // Wheel spinning animation
        g2d.setColor(Color.LIGHT_GRAY);
        double angle = Math.toRadians(wheelRotation);
        double lineY1 = Math.sin(angle) * 5;
        double lineY2 = Math.sin(angle + Math.PI) * 5;
        g2d.drawLine(0, (int)lineY1, 0, (int)lineY2);

        g2d.translate(-(wx + 4), -(wy + 7));
    }
}

class EnemyCar {
    private double x, y, width, height;
    private Color color;
    private int type; // 0: Sedan, 1: Truck, 2: Sports

    public EnemyCar(double x, double y) {
        this.x = x;
        this.y = y;
        this.width = 40;
        this.height = 80;
        this.type = (int)(Math.random() * 3);

        Random rand = new Random();
        color = new Color(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255));

        if (type == 1) { // Truck
            height = 110;
            width = 45;
        }
    }

    public void update(double roadSpeed) {
        y += roadSpeed * 0.8; // Slightly slower than road to simulate relative speed
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, (int)width, (int)height);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

    public void draw(Graphics2D g2d) {
        g2d.translate(x + width/2, y + height/2);

        if (type == 0) drawSedan(g2d);
        else if (type == 1) drawTruck(g2d);
        else drawSports(g2d);

        g2d.translate(-(x + width/2), -(y + height/2));
    }

    private void drawSedan(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillRoundRect(-20, -35, 40, 70, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(-20, -35, 40, 70, 10, 10);

        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect(-15, -15, 30, 25); // Windows

        g2d.setColor(Color.RED);
        g2d.fillRect(-18, 30, 10, 5);
        g2d.fillRect(8, 30, 10, 5);
    }

    private void drawTruck(Graphics2D g2d) {
        // Trailer
        g2d.setColor(new Color(200, 200, 200));
        g2d.fillRect(-22, -40, 44, 60);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(-22, -40, 44, 60);

        // Cab
        g2d.setColor(color);
        g2d.fillRoundRect(-20, 20, 40, 30, 5, 5);
        g2d.drawRoundRect(-20, 20, 40, 30, 5, 5);

        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect(-15, 25, 30, 15);
    }

    private void drawSports(Graphics2D g2d) {
        Path2D body = new Path2D.Double();
        body.moveTo(-18, -30);
        body.lineTo(18, -30);
        body.lineTo(22, 30);
        body.lineTo(-22, 30);
        body.closePath();

        g2d.setColor(color);
        g2d.fill(body);
        g2d.setColor(Color.BLACK);
        g2d.draw(body);

        g2d.setColor(new Color(30, 30, 30));
        g2d.fillOval(-10, -5, 20, 20);
    }
}

class Road {
    private double scrollOffset;
    private double scrollSpeed;
    private TourManager tourManager;
    private ArrayList<SceneryObject> sceneryLeft;
    private ArrayList<SceneryObject> sceneryRight;

    public Road(TourManager tourManager) {
        this.tourManager = tourManager;
        scrollOffset = 0;
        scrollSpeed = 0;
        sceneryLeft = new ArrayList<>();
        sceneryRight = new ArrayList<>();
        initScenery();
    }

    private void initScenery() {
        for (int i = 0; i < 10; i++) {
            sceneryLeft.add(new SceneryObject(Math.random() * 100, i * 100));
            sceneryRight.add(new SceneryObject(700 + Math.random() * 100, i * 100));
        }
    }

    public void reset() {
        scrollOffset = 0;
    }

    public void update(double playerSpeed) {
        scrollSpeed = playerSpeed;
        scrollOffset += scrollSpeed;
        if (scrollOffset > 100) scrollOffset -= 100;

        for (SceneryObject s : sceneryLeft) {
            s.update(scrollSpeed);
            if (s.y > 600) s.reset(-100, Math.random() * 100);
        }
        for (SceneryObject s : sceneryRight) {
            s.update(scrollSpeed);
            if (s.y > 600) s.reset(-100, 700 + Math.random() * 100);
        }
    }

    public double getScrollSpeed() { return scrollSpeed; }

    public void draw(Graphics2D g2d) {
        TourManager.Location loc = tourManager.getCurrentLocation();

        // Grass / Offroad
        g2d.setColor(loc.grassColor);
        g2d.fillRect(0, 0, 800, 600);

        // Road Base
        g2d.setColor(loc.roadColor);
        g2d.fillRect(100, 0, 600, 600);

        // Rumble strips
        g2d.setColor(loc.rumbleColor);
        for (int i = -1; i < 15; i++) {
            int yPos = (int)((i * 50) + (scrollOffset % 50));
            g2d.fillRect(90, yPos, 10, 25);
            g2d.fillRect(700, yPos, 10, 25);
        }

        // Dashed Lines
        g2d.setColor(Color.WHITE);
        for (int lane = 1; lane < 4; lane++) {
            int xPos = 100 + (lane * 150);
            for (int i = -1; i < 15; i++) {
                int yPos = (int)((i * 100) + (scrollOffset % 100));
                g2d.fillRect(xPos - 3, yPos, 6, 50);
            }
        }

        // Scenery
        for (SceneryObject s : sceneryLeft) s.draw(g2d, loc);
        for (SceneryObject s : sceneryRight) s.draw(g2d, loc);
    }
}

class SceneryObject {
    public double x, y;
    private int type;

    public SceneryObject(double x, double y) {
        this.x = x;
        this.y = y;
        this.type = (int)(Math.random() * 3);
    }

    public void update(double speed) {
        y += speed;
    }

    public void reset(double newY, double newX) {
        y = newY;
        x = newX;
        type = (int)(Math.random() * 3);
    }

    public void draw(Graphics2D g2d, TourManager.Location loc) {
        if (loc == TourManager.Location.TOKYO) drawNeon(g2d);
        else if (loc == TourManager.Location.PARIS) drawTree(g2d, new Color(34, 139, 34));
        else if (loc == TourManager.Location.NEW_YORK) drawBuilding(g2d);
        else if (loc == TourManager.Location.CAIRO) drawPalm(g2d);
    }

    private void drawNeon(Graphics2D g2d) {
        g2d.setColor(new Color(255, 0, 255, 150));
        g2d.fillRect((int)x, (int)y, 20, 60);
        g2d.setColor(Color.CYAN);
        g2d.drawRect((int)x, (int)y, 20, 60);
    }

    private void drawTree(Graphics2D g2d, Color leafColor) {
        g2d.setColor(new Color(101, 67, 33));
        g2d.fillRect((int)x + 8, (int)y + 20, 4, 20);
        g2d.setColor(leafColor);
        g2d.fillOval((int)x, (int)y, 20, 25);
    }

    private void drawBuilding(Graphics2D g2d) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect((int)x, (int)y, 30, 80);
        g2d.setColor(Color.YELLOW);
        for(int i=0; i<4; i++) {
            for(int j=0; j<2; j++) {
                if(Math.random() > 0.3) g2d.fillRect((int)x + 5 + j*12, (int)y + 10 + i*15, 6, 8);
            }
        }
    }

    private void drawPalm(Graphics2D g2d) {
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect((int)x + 8, (int)y + 20, 4, 30);
        g2d.setColor(new Color(0, 100, 0));
        g2d.fillOval((int)x, (int)y, 20, 20);
    }
}

class TourManager {
    public enum Location { TOKYO, PARIS, NEW_YORK, CAIRO }

    private Location currentLocation;
    private Location nextLocation;
    private boolean transitioning;
    private boolean fadingBack;
    private double distanceThreshold;

    public TourManager() {
        reset();
    }

    public void reset() {
        currentLocation = Location.TOKYO;
        nextLocation = Location.PARIS;
        transitioning = false;
        fadingBack = false;
        distanceThreshold = 1000;
    }

    public void update(double distance) {
        if (!transitioning && !fadingBack && distance >= distanceThreshold) {
            transitioning = true;
        }
    }

    public void completeTransition() {
        if (transitioning && !fadingBack) {
            currentLocation = nextLocation;
            distanceThreshold += 1500;
            transitioning = false;
            fadingBack = true;

            if (currentLocation == Location.PARIS) nextLocation = Location.NEW_YORK;
            else if (currentLocation == Location.NEW_YORK) nextLocation = Location.CAIRO;
            else if (currentLocation == Location.CAIRO) nextLocation = Location.TOKYO;
        }
    }

    public boolean isTransitioning() { return transitioning && !fadingBack; }
    public boolean isFadingBack() { return fadingBack; }
    public Location getCurrentLocation() { return currentLocation; }

    public String getNextLocationName() {
        switch (nextLocation) {
            case Location.TOKYO: return "Entering: TOKYO";
            case Location.PARIS: return "Entering: PARIS";
            case Location.NEW_YORK: return "Entering: NEW YORK";
            case Location.CAIRO: return "Entering: CAIRO";
            default: return "";
        }
    }

    // Inner class to hold theme colors
    public static class Location {
        public static final Location TOKYO = new Location(new Color(20, 20, 30), new Color(40, 40, 50), new Color(255, 0, 100));
        public static final Location PARIS = new Location(new Color(135, 206, 235), new Color(80, 80, 80), new Color(255, 255, 255));
        public static final Location NEW_YORK = new Location(new Color(100, 100, 100), new Color(50, 50, 50), new Color(255, 255, 0));
        public static final Location CAIRO = new Location(new Color(237, 201, 175), new Color(60, 60, 60), new Color(255, 165, 0));

        Color grassColor, roadColor, rumbleColor;
        Location(Color g, Color r, Color ru) { grassColor = g; roadColor = r; rumbleColor = ru; }
    }
}

class ParticleSystem {
    private ArrayList<Particle> particles;

    public ParticleSystem() {
        particles = new ArrayList<>();
    }

    public void addParticle(double x, double y, boolean isExplosion) {
        particles.add(new Particle(x, y, isExplosion));
    }

    public void clear() {
        particles.clear();
    }

    public void update() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update();
            if (p.life <= 0) it.remove();
        }
    }

    public void draw(Graphics2D g2d) {
        for (Particle p : particles) p.draw(g2d);
    }
}

class Particle {
    double x, y, vx, vy, life, maxLife, size;
    Color color;

    public Particle(double x, double y, boolean isExplosion) {
        this.x = x;
        this.y = y;
        this.maxLife = 30 + Math.random() * 20;
        this.life = maxLife;
        this.size = 2 + Math.random() * 4;

        if (isExplosion) {
            vx = (Math.random() - 0.5) * 10;
            vy = (Math.random() - 0.5) * 10;
            int r = 200 + (int)(Math.random() * 55);
            int g = (int)(Math.random() * 150);
            color = new Color(r, g, 0);
        } else {
            vx = (Math.random() - 0.5) * 2;
            vy = Math.random() * 2 + 1;
            color = new Color(150, 150, 150, 150);
        }
    }

    public void update() {
        x += vx;
        y += vy;
        life--;
        size *= 0.95;
    }

    public void draw(Graphics2D g2d) {
        int alpha = (int)((life / maxLife) * 255);
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g2d.fillOval((int)x, (int)y, (int)size, (int)size);
    }
}

class HUD {
    public void draw(Graphics2D g2d, int score, double distance, TourManager.Location loc, double speed) {
        // Speedometer Background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(600, 480, 180, 100, 20, 20);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(600, 480, 180, 100, 20, 20);

        // Speed Text
        g2d.setFont(new Font("SansSerif", Font.BOLD, 30));
        int kmh = (int)(speed * 20);
        g2d.drawString(kmh + " km/h", 620, 530);

        // Score & Distance
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(20, 20, 250, 100, 20, 20);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(20, 20, 250, 100, 20, 20);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2d.drawString("Score: " + score, 40, 55);
        g2d.drawString("Dist: " + (int)distance + " m", 40, 85);

        // Location Name
        g2d.setFont(new Font("SansSerif", Font.ITALIC, 16));
        String locName = loc == TourManager.Location.TOKYO ? "TOKYO" :
                loc == TourManager.Location.PARIS ? "PARIS" :
                        loc == TourManager.Location.NEW_YORK ? "NEW YORK" : "CAIRO";
        g2d.drawString("Current: " + locName, 40, 110);
    }
}