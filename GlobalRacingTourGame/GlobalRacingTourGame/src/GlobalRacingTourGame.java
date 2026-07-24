import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GlobalRacingTourGame extends JPanel implements KeyListener, ActionListener {
    // Game constants
    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;
    private static final int GROUND_Y = 600;
    private static final int MAX_LAPS = 3;

    // Game state
    private GameState state;
    private PlayerCar player;
    private List<OpponentCar> opponents;
    private List<Obstacle> obstacles;
    private List<PowerUp> powerUps;
    private List<Particle> particles;
    private Track track;
    private HUD hud;
    private Timer gameLoop = new Timer(16, this);
    private Timer animationTimer;
    private int score;
    private int lapCount;
    private long startTime;
    private boolean[] keys;
    private int frameCount;

    // Animation properties
    private float cloudOffset;
    private float treeOffset;
    private float roadOffset;
    private boolean isNightMode;
    private float dayNightCycle;

    public GlobalRacingTourGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235));
        setFocusable(true);
        addKeyListener(this);

        initializeGame();
        startGameLoop();
        startAnimationTimer();
    }

    private void initializeGame() {
        state = GameState.MENU;
        keys = new boolean[256];
        score = 0;
        lapCount = 0;
        startTime = System.currentTimeMillis();
        frameCount = 0;
        cloudOffset = 0;
        treeOffset = 0;
        roadOffset = 0;
        isNightMode = false;
        dayNightCycle = 0;

        // Initialize player
        player = new PlayerCar(400, GROUND_Y - 80, 50, 90, Color.RED);

        // Initialize opponents
        opponents = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Color[] colors = {Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA, Color.CYAN};
            OpponentCar opp = new OpponentCar(
                    100 + i * 150,
                    GROUND_Y - 80 - i * 60,
                    45, 80,
                    colors[i % colors.length]
            );
            opp.setSpeed(1.0f + i * 0.3f);
            opponents.add(opp);
        }

        // Initialize obstacles
        obstacles = new ArrayList<>();
        generateObstacles();

        // Initialize power-ups
        powerUps = new ArrayList<>();
        generatePowerUps();

        // Initialize particles
        particles = new ArrayList<>();

        // Initialize track
        track = new Track();

        // Initialize HUD
        hud = new HUD();
    }

    private void generateObstacles() {
        Random rand = new Random();
        for (int i = 0; i < 8; i++) {
            int x = 50 + rand.nextInt(800);
            int y = 50 + rand.nextInt(500);
            ObstacleType type = ObstacleType.values()[rand.nextInt(ObstacleType.values().length)];
            obstacles.add(new Obstacle(x, y, type));
        }
    }

    private void generatePowerUps() {
        Random rand = new Random();
        for (int i = 0; i < 4; i++) {
            int x = 100 + rand.nextInt(700);
            int y = 100 + rand.nextInt(400);
            PowerUpType type = PowerUpType.values()[rand.nextInt(PowerUpType.values().length)];
            powerUps.add(new PowerUp(x, y, type));
        }
    }

    private void startGameLoop() throws InterruptedException {
        gameLoop.wait();
    }

    private void startAnimationTimer() {
        animationTimer = new Timer();
        animationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateAnimation();
            }
        }, 0, 50);
    }

    private void updateAnimation() {
        cloudOffset += 0.5f;
        if (cloudOffset > WIDTH) cloudOffset = -200;

        treeOffset += 1.0f;
        if (treeOffset > WIDTH) treeOffset = -100;

        roadOffset += 2.0f;
        if (roadOffset > 100) roadOffset = 0;

        dayNightCycle += 0.01f;
        if (dayNightCycle > 1.0f) dayNightCycle = 0;
        isNightMode = dayNightCycle > 0.7f;

        // Update particles
        Iterator<Particle> iter = particles.iterator();
        while (iter.hasNext()) {
            Particle p = iter.next();
            p.update();
            if (p.isDead()) iter.remove();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameState.PLAYING) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() throws InterruptedException {
        frameCount++;

        // Update player
        player.update(keys);
        player.boundToScreen(WIDTH, HEIGHT);

        // Update opponents with AI
        for (OpponentCar opp : opponents) {
            opp.updateAI(player, obstacles);
            opp.move();
            opp.boundToScreen(WIDTH, HEIGHT);

            // Check collision with player
            if (player.getBounds().intersects(opp.getBounds())) {
                handleCollision(player, opp);
            }
        }

        // Update obstacles
        for (Obstacle obs : obstacles) {
            obs.update();
            if (player.getBounds().intersects(obs.getBounds())) {
                handleObstacleCollision(obs);
            }
        }

        // Update power-ups
        for (PowerUp pu : powerUps) {
            pu.update();
            if (player.getBounds().intersects(pu.getBounds())) {
                handlePowerUp(pu);
            }
        }

        // Update particles
        for (Particle p : particles) {
            p.update();
        }
        particles.removeIf(Particle::isDead);

        // Update track
        track.update(player.getX());

        // Update score based on distance
        if (frameCount % 5 == 0) {
            score += player.getSpeed() * 0.5f;
        }

        // Check lap completion
        if (player.getY() < 100 && player.getPrevY() > 100) {
            lapCount++;
            if (lapCount >= MAX_LAPS) {
                state = GameState.GAME_OVER;
                gameLoop.wait();
                animationTimer.cancel();
            }
        }

        // Update HUD
        hud.update(score, lapCount, player.getSpeed(), MAX_LAPS);
    }

    private void handleCollision(PlayerCar player, OpponentCar opp) {
        // Create explosion particles
        for (int i = 0; i < 30; i++) {
            particles.add(new Particle(
                    opp.getX() + opp.getWidth()/2,
                    opp.getY() + opp.getHeight()/2,
                    new Color(255, 150, 50),
                    3 + (int)(Math.random() * 5)
            ));
        }
        score -= 50;
        opp.resetPosition();
    }

    private void handleObstacleCollision(Obstacle obs) {
        score -= 20;
        obs.reset();
        for (int i = 0; i < 15; i++) {
            particles.add(new Particle(
                    obs.getX() + obs.getWidth()/2,
                    obs.getY() + obs.getHeight()/2,
                    Color.GRAY,
                    2 + (int)(Math.random() * 4)
            ));
        }
    }

    private void handlePowerUp(PowerUp pu) {
        switch (pu.getType()) {
            case SPEED:
                player.boostSpeed(1.5f, 3000);
                score += 100;
                break;
            case SHIELD:
                player.activateShield(3000);
                score += 75;
                break;
            case COIN:
                score += 200;
                break;
            case MAGNET:
                player.activateMagnet(2000);
                score += 50;
                break;
        }
        pu.respawn();
        for (int i = 0; i < 10; i++) {
            particles.add(new Particle(
                    pu.getX() + pu.getWidth()/2,
                    pu.getY() + pu.getHeight()/2,
                    Color.GREEN,
                    2 + (int)(Math.random() * 3)
            ));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (state == GameState.MENU) {
            drawMenu(g2d);
        } else if (state == GameState.PLAYING) {
            drawGame(g2d);
        } else if (state == GameState.GAME_OVER) {
            drawGameOver(g2d);
        } else if (state == GameState.PAUSED) {
            drawGame(g2d);
            drawPauseOverlay(g2d);
        }
    }

    private void drawMenu(Graphics2D g) {
        // Background gradient
        GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 30, 50),
                WIDTH, HEIGHT, new Color(10, 20, 40));
        g.setPaint(gradient);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 60));
        String title = "🏁 GLOBAL RACING TOUR 🏁";
        FontMetrics fm = g.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(title)) / 2;
        g.drawString(title, x, 200);

        // Subtitle
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.setColor(new Color(200, 200, 255));
        String subtitle = "★★★★★ WORLD CHAMPIONSHIP ★★★★★";
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(subtitle)) / 2;
        g.drawString(subtitle, x, 250);

        // Menu options
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.setColor(Color.YELLOW);
        String startMsg = "Press SPACE to Start";
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(startMsg)) / 2;
        g.drawString(startMsg, x, 400);

        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.setColor(Color.LIGHT_GRAY);
        String controls1 = "← ↑ → ↓ : Move";
        String controls2 = "P : Pause | R : Reset";
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(controls1)) / 2;
        g.drawString(controls1, x, 480);
        x = (WIDTH - fm.stringWidth(controls2)) / 2;
        g.drawString(controls2, x, 510);

        // Animated cars
        drawAnimatedCars(g);

        // Version
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(Color.GRAY);
        g.drawString("v2.0 - 600+ lines", 10, HEIGHT - 10);
    }

    private void drawAnimatedCars(Graphics2D g) {
        int time = (int)(System.currentTimeMillis() / 100);
        int offset = time % 100;

        for (int i = 0; i < 5; i++) {
            int x = 50 + i * 180 + (int)(Math.sin(time * 0.02 + i) * 30);
            int y = 320 + (int)(Math.sin(time * 0.03 + i * 0.5) * 50);
            g.setColor(new Color(100 + i * 30, 50, 150));
            g.fillRect(x, y, 40, 70);
            g.setColor(Color.WHITE);
            g.fillRect(x + 5, y + 10, 10, 15);
            g.fillRect(x + 25, y + 10, 10, 15);
            g.fillRect(x + 5, y + 45, 10, 15);
            g.fillRect(x + 25, y + 45, 10, 15);
        }
    }

    private void drawGame(Graphics2D g) {
        // Draw sky with day/night cycle
        if (isNightMode) {
            g.setColor(new Color(10, 10, 30));
        } else {
            GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 235),
                    0, HEIGHT/2, new Color(200, 230, 255));
            g.setPaint(sky);
        }
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw clouds
        drawClouds(g);

        // Draw track
        track.draw(g);

        // Draw road markings
        drawRoadMarkings(g);

        // Draw obstacles
        for (Obstacle obs : obstacles) {
            obs.draw(g);
        }

        // Draw power-ups
        for (PowerUp pu : powerUps) {
            pu.draw(g);
        }

        // Draw opponents
        for (OpponentCar opp : opponents) {
            opp.draw(g);
        }

        // Draw player
        player.draw(g);

        // Draw particles
        for (Particle p : particles) {
            p.draw(g);
        }

        // Draw trees
        drawTrees(g);

        // Draw HUD
        hud.draw(g);
    }

    private void drawClouds(Graphics2D g) {
        g.setColor(new Color(255, 255, 255, 150));
        for (int i = 0; i < 5; i++) {
            int x = (int)(i * 200 + cloudOffset) % (WIDTH + 200) - 100;
            int y = 30 + i * 25;
            g.fillOval(x, y, 80, 40);
            g.fillOval(x + 30, y - 15, 60, 35);
            g.fillOval(x + 50, y + 5, 50, 30);
        }
    }

    private void drawRoadMarkings(Graphics2D g) {
        g.setColor(Color.WHITE);
        for (int i = 0; i < 20; i++) {
            int y = (int)(i * 40 + roadOffset) % (HEIGHT + 40) - 20;
            g.fillRect(430, y, 40, 20);
        }

        // Side lines
        g.setColor(new Color(255, 255, 0, 100));
        g.fillRect(50, 0, 5, HEIGHT);
        g.fillRect(845, 0, 5, HEIGHT);
    }

    private void drawTrees(Graphics2D g) {
        int[] treeX = {100, 250, 650, 780};
        int[] treeY = {100, 200, 150, 300};

        for (int i = 0; i < treeX.length; i++) {
            int x = treeX[i];
            int y = treeY[i];
            g.setColor(new Color(101, 67, 33));
            g.fillRect(x + 10, y + 30, 20, 40);
            g.setColor(new Color(34, 139, 34));
            g.fillOval(x - 10, y, 60, 60);
            g.fillOval(x + 5, y - 15, 40, 50);
        }
    }

    private void drawPauseOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 60));
        String msg = "⏸ PAUSED";
        FontMetrics fm = g.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(msg)) / 2;
        g.drawString(msg, x, HEIGHT/2);
    }

    private void drawGameOver(Graphics2D g) {
        drawGame(g);
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 70));
        String msg = "🏆 RACE COMPLETE! 🏆";
        FontMetrics fm = g.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(msg)) / 2;
        g.drawString(msg, x, 250);

        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.YELLOW);
        String scoreMsg = "Score: " + score;
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(scoreMsg)) / 2;
        g.drawString(scoreMsg, x, 350);

        g.setFont(new Font("Arial", Font.PLAIN, 28));
        g.setColor(Color.LIGHT_GRAY);
        String lapsMsg = "Laps Completed: " + lapCount + "/" + MAX_LAPS;
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(lapsMsg)) / 2;
        g.drawString(lapsMsg, x, 420);

        long time = (System.currentTimeMillis() - startTime) / 1000;
        String timeMsg = "Time: " + time + "s";
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(timeMsg)) / 2;
        g.drawString(timeMsg, x, 470);

        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.GREEN);
        String restartMsg = "Press R to Restart";
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(restartMsg)) / 2;
        g.drawString(restartMsg, x, 550);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key >= 0 && key < keys.length) {
            keys[key] = true;
        }

        if (state == GameState.MENU && key == KeyEvent.VK_SPACE) {
            state = GameState.PLAYING;
            startTime = System.currentTimeMillis();
        } else if (state == GameState.PLAYING && key == KeyEvent.VK_P) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED && key == KeyEvent.VK_P) {
            state = GameState.PLAYING;
        } else if (key == KeyEvent.VK_R) {
            try {
                resetGame();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key >= 0 && key < keys.length) {
            keys[key] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void resetGame() throws InterruptedException {
        gameLoop.wait();
        if (animationTimer != null) animationTimer.cancel();
        initializeGame();
        startGameLoop();
        startAnimationTimer();
    }

    // Inner classes

    enum GameState {
        MENU, PLAYING, PAUSED, GAME_OVER
    }

    enum ObstacleType {
        CONE, BARREL, TIRE, ROCK
    }

    enum PowerUpType {
        SPEED, SHIELD, COIN, MAGNET
    }

    class PlayerCar {
        private int x, y, width, height;
        private Color color;
        private float speed;
        private int prevY;
        private boolean shieldActive;
        private boolean magnetActive;
        private long shieldEndTime;
        private long magnetEndTime;
        private float boostMultiplier;
        private long boostEndTime;

        public PlayerCar(int x, int y, int w, int h, Color c) {
            this.x = x; this.y = y;
            width = w; height = h;
            color = c;
            speed = 3.0f;
            prevY = y;
            shieldActive = false;
            magnetActive = false;
            boostMultiplier = 1.0f;
        }

        public void update(boolean[] keys) {
            prevY = y;
            float currentSpeed = speed * boostMultiplier;

            if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) {
                y -= currentSpeed;
            }
            if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]) {
                y += currentSpeed * 0.7f;
            }
            if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]) {
                x -= currentSpeed * 0.8f;
            }
            if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]) {
                x += currentSpeed * 0.8f;
            }

            // Update boost timer
            if (boostMultiplier > 1.0f && System.currentTimeMillis() > boostEndTime) {
                boostMultiplier = 1.0f;
            }

            // Update shield timer
            if (shieldActive && System.currentTimeMillis() > shieldEndTime) {
                shieldActive = false;
            }

            // Update magnet timer
            if (magnetActive && System.currentTimeMillis() > magnetEndTime) {
                magnetActive = false;
            }
        }

        public void boundToScreen(int screenWidth, int screenHeight) {
            x = Math.max(20, Math.min(screenWidth - width - 20, x));
            y = Math.max(20, Math.min(screenHeight - height - 20, y));
        }

        public void boostSpeed(float multiplier, long duration) {
            boostMultiplier = multiplier;
            boostEndTime = System.currentTimeMillis() + duration;
        }

        public void activateShield(long duration) {
            shieldActive = true;
            shieldEndTime = System.currentTimeMillis() + duration;
        }

        public void activateMagnet(long duration) {
            magnetActive = true;
            magnetEndTime = System.currentTimeMillis() + duration;
        }

        public void draw(Graphics2D g) {
            // Shadow
            g.setColor(new Color(0, 0, 0, 50));
            g.fillRect(x + 5, y + 5, width, height);

            // Shield glow
            if (shieldActive) {
                g.setColor(new Color(0, 255, 255, 50));
                g.fillOval(x - 10, y - 10, width + 20, height + 20);
            }

            // Magnet glow
            if (magnetActive) {
                g.setColor(new Color(255, 0, 255, 40));
                g.fillOval(x - 20, y - 20, width + 40, height + 40);
            }

            // Car body
            g.setColor(color);
            g.fillRoundRect(x, y, width, height, 10, 10);

            // Windshield
            g.setColor(new Color(135, 206, 250));
            g.fillRect(x + 5, y + 10, width - 10, 20);

            // Windows
            g.setColor(new Color(100, 180, 255));
            g.fillRect(x + 5, y + 35, 12, 20);
            g.fillRect(x + width - 17, y + 35, 12, 20);

            // Headlights
            g.setColor(Color.YELLOW);
            g.fillOval(x + 3, y + 5, 8, 8);
            g.fillOval(x + width - 11, y + 5, 8, 8);

            // Taillights
            g.setColor(Color.RED);
            g.fillOval(x + 3, y + height - 12, 8, 8);
            g.fillOval(x + width - 11, y + height - 12, 8, 8);

            // Boost effect
            if (boostMultiplier > 1.0f) {
                g.setColor(new Color(255, 100, 0, 100));
                g.fillRect(x + 10, y + height, width - 20, 10);
            }
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getPrevY() { return prevY; }
        public float getSpeed() { return speed * boostMultiplier; }
        public boolean hasShield() { return shieldActive; }
        public boolean hasMagnet() { return magnetActive; }
    }

    class OpponentCar {
        private int x, y, width, height;
        private Color color;
        private float speed;
        private int direction;
        private Random rand;
        private int aiTimer;
        private int targetX;

        public OpponentCar(int x, int y, int w, int h, Color c) {
            this.x = x; this.y = y;
            width = w; height = h;
            color = c;
            speed = 1.5f;
            direction = 1;
            rand = new Random();
            aiTimer = 0;
            targetX = x;
        }

        public void setSpeed(float s) { speed = s; }

        public void updateAI(PlayerCar player, List<Obstacle> obstacles) {
            aiTimer++;
            int dx = player.getX() - x;
            int dy = player.getY() - y - 100;

            if (aiTimer > 30) {
                aiTimer = 0;
                targetX = x + (rand.nextInt(200) - 100);
                targetX = Math.max(20, Math.min(850 - width, targetX));
            }

            // Move towards target
            if (x < targetX - 5) x += speed * 0.5f;
            else if (x > targetX + 5) x -= speed * 0.5f;

            // Avoid obstacles
            for (Obstacle obs : obstacles) {
                if (Math.abs(x - obs.getX()) < 100 && Math.abs(y - obs.getY()) < 50) {
                    if (x < obs.getX()) x -= speed;
                    else x += speed;
                }
            }
        }

        public void move() {
            y += speed * 0.3f;
            if (y > HEIGHT) {
                y = -height;
                x = 50 + rand.nextInt(750);
            }
        }

        public void boundToScreen(int screenWidth, int screenHeight) {
            x = Math.max(20, Math.min(screenWidth - width - 20, x));
        }

        public void resetPosition() {
            y = -height - rand.nextInt(200);
            x = 50 + rand.nextInt(750);
        }

        public void draw(Graphics2D g) {
            // Shadow
            g.setColor(new Color(0, 0, 0, 30));
            g.fillRect(x + 3, y + 3, width, height);

            // Car body
            g.setColor(color);
            g.fillRoundRect(x, y, width, height, 8, 8);

            // Details
            g.setColor(new Color(200, 200, 200, 100));
            g.fillRect(x + 5, y + 12, width - 10, 15);

            // Windows
            g.setColor(new Color(150, 200, 255));
            g.fillRect(x + 5, y + 30, 10, 15);
            g.fillRect(x + width - 15, y + 30, 10, 15);

            // Number
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            String num = String.valueOf(rand.nextInt(99) + 1);
            FontMetrics fm = g.getFontMetrics();
            int tx = x + (width - fm.stringWidth(num)) / 2;
            g.drawString(num, tx, y + 50);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }

    class Obstacle {
        private int x, y, width, height;
        private ObstacleType type;
        private Color color;
        private boolean active;

        public Obstacle(int x, int y, ObstacleType type) {
            this.x = x; this.y = y;
            this.type = type;
            this.active = true;

            switch(type) {
                case CONE:
                    width = 20; height = 30;
                    color = new Color(255, 165, 0);
                    break;
                case BARREL:
                    width = 30; height = 35;
                    color = new Color(139, 69, 19);
                    break;
                case TIRE:
                    width = 30; height = 30;
                    color = Color.DARK_GRAY;
                    break;
                case ROCK:
                    width = 25; height = 25;
                    color = Color.GRAY;
                    break;
            }
        }

        public void update() {
            y += 2;
            if (y > HEIGHT) {
                y = -height;
                x = 50 + (int)(Math.random() * 800);
            }
        }

        public void reset() {
            y = -height - 50;
            x = 50 + (int)(Math.random() * 800);
        }

        public void draw(Graphics2D g) {
            if (!active) return;
            g.setColor(color);
            g.fillRect(x, y, width, height);

            // Details based on type
            g.setColor(Color.BLACK);
            if (type == ObstacleType.CONE) {
                g.drawLine(x + width/2, y, x + width/2, y + height);
            } else if (type == ObstacleType.BARREL) {
                g.drawLine(x, y + height/2, x + width, y + height/2);
                g.drawLine(x, y + height/3, x + width, y + height/3);
            } else if (type == ObstacleType.TIRE) {
                g.drawOval(x + 5, y + 5, width - 10, height - 10);
            } else if (type == ObstacleType.ROCK) {
                g.drawLine(x + 5, y + 5, x + width - 5, y + height - 5);
                g.drawLine(x + width - 5, y + 5, x + 5, y + height - 5);
            }
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }

    class PowerUp {
        private int x, y, width, height;
        private PowerUpType type;
        private Color color;
        private boolean active;
        private float pulse;
        private int pulseDir;

        public PowerUp(int x, int y, PowerUpType type) {
            this.x = x; this.y = y;
            this.type = type;
            this.active = true;
            width = 25; height = 25;
            pulse = 0;
            pulseDir = 1;

            switch(type) {
                case SPEED:
                    color = new Color(0, 255, 255);
                    break;
                case SHIELD:
                    color = new Color(0, 200, 255);
                    break;
                case COIN:
                    color = new Color(255, 215, 0);
                    break;
                case MAGNET:
                    color = new Color(255, 0, 255);
                    break;
            }
        }

        public void update() {
            y += 1.5f;
            if (y > HEIGHT) {
                y = -height;
                x = 50 + (int)(Math.random() * 800);
            }

            pulse += 0.05f * pulseDir;
            if (pulse > 1.0f || pulse < 0) pulseDir *= -1;
        }

        public void respawn() {
            y = -height;
            x = 50 + (int)(Math.random() * 800);
        }

        public void draw(Graphics2D g) {
            if (!active) return;

            int size = (int)(width + pulse * 5);
            int offset = (size - width) / 2;

            // Glow
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
            g.fillOval(x - 10 + offset, y - 10 + offset, size + 20, size + 20);

            // Main shape
            g.setColor(color);
            if (type == PowerUpType.COIN) {
                g.fillOval(x - offset, y - offset, size, size);
                g.setColor(Color.YELLOW.darker());
                g.drawString("$", x + 5, y + 20);
            } else {
                g.fillRoundRect(x - offset, y - offset, size, size, 5, 5);
                g.setColor(Color.WHITE);
                String label = "";
                switch(type) {
                    case SPEED: label = "S"; break;
                    case SHIELD: label = "D"; break;
                    case MAGNET: label = "M"; break;
                    default: label = "?";
                }
                g.setFont(new Font("Arial", Font.BOLD, 14));
                FontMetrics fm = g.getFontMetrics();
                int tx = x + (width - fm.stringWidth(label)) / 2;
                g.drawString(label, tx, y + 18);
            }
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public PowerUpType getType() { return type; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }

    class Particle {
        private float x, y;
        private float vx, vy;
        private int life;
        private int maxLife;
        private Color color;
        private int size;

        public Particle(float x, float y, Color color, int size) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.size = size;
            maxLife = 30 + (int)(Math.random() * 30);
            life = maxLife;

            float angle = (float)(Math.random() * 2 * Math.PI);
            float speed = 1 + (float)(Math.random() * 4);
            vx = (float)(Math.cos(angle) * speed);
            vy = (float)(Math.sin(angle) * speed) - 1;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 0.1f;
            vx *= 0.98f;
            life--;
        }

        public boolean isDead() {
            return life <= 0;
        }

        public void draw(Graphics2D g) {
            float alpha = (float)life / maxLife;
            int r = color.getRed();
            int gCol = color.getGreen();
            int b = color.getBlue();
            g.setColor(new Color(r, gCol, b, (int)(alpha * 255)));
            int s = (int)(size * (0.5f + 0.5f * alpha));
            g.fillOval((int)x - s/2, (int)y - s/2, s, s);
        }
    }

    class Track {
        private int offset;
        private Color grass1, grass2;
        private Color roadColor;

        public Track() {
            offset = 0;
            grass1 = new Color(34, 139, 34);
            grass2 = new Color(50, 180, 50);
            roadColor = new Color(80, 80, 80);
        }

        public void update(int playerX) {
            offset = (offset + 2) % 100;
        }

        public void draw(Graphics2D g) {
            // Grass
            for (int y = 0; y < HEIGHT; y += 50) {
                int yOff = (y + offset) % 100;
                g.setColor(yOff < 50 ? grass1 : grass2);
                g.fillRect(0, y, 50, 50);
                g.fillRect(850, y, 50, 50);
            }

            // Road
            g.setColor(roadColor);
            g.fillRect(50, 0, 800, HEIGHT);

            // Road edge
            g.setColor(Color.WHITE);
            g.fillRect(50, 0, 3, HEIGHT);
            g.fillRect(847, 0, 3, HEIGHT);

            // Road shoulder
            g.setColor(new Color(100, 100, 100));
            g.fillRect(53, 0, 10, HEIGHT);
            g.fillRect(837, 0, 10, HEIGHT);
        }
    }

    class HUD {
        private int score;
        private int lap;
        private float speed;
        private int maxLaps;
        private long lastUpdate;

        public HUD() {
            score = 0;
            lap = 0;
            speed = 0;
            maxLaps = MAX_LAPS;
            lastUpdate = System.currentTimeMillis();
        }

        public void update(int score, int lap, float speed, int maxLaps) {
            this.score = score;
            this.lap = lap;
            this.speed = speed;
            this.maxLaps = maxLaps;
        }

        public void draw(Graphics2D g) {
            // Semi-transparent background
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRoundRect(10, 10, 280, 120, 15, 15);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 18));

            // Score
            g.drawString("⭐ Score: " + score, 25, 40);

            // Speed
            g.drawString("🚀 Speed: " + (int)(speed * 30) + " km/h", 25, 70);

            // Laps
            g.drawString("🏁 Laps: " + lap + "/" + maxLaps, 25, 100);

            // Boost indicator
            if (speed > 3.5f) {
                g.setColor(Color.CYAN);
                g.drawString("⚡ BOOST!", 25, 125);
            }

            // Shield indicator
            if (player.hasShield()) {
                g.setColor(new Color(0, 200, 255));
                g.drawString("🛡️ SHIELD", 160, 40);
            }

            // Magnet indicator
            if (player.hasMagnet()) {
                g.setColor(new Color(255, 0, 255));
                g.drawString("🧲 MAGNET", 160, 70);
            }

            // Time
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            long minutes = elapsed / 60;
            long seconds = elapsed % 60;
            g.setColor(new Color(200, 200, 200));
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.drawString(String.format("⏱️ %02d:%02d", minutes, seconds), 160, 100);
        }
    }

    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("🏎️ Global Racing Tour Game");
            GlobalRacingTourGame game = new GlobalRacingTourGame();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}