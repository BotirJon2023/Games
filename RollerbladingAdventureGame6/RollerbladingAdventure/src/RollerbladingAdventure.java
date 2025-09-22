import Cloud.Cloud;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class RollerbladingAdventure extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int GROUND_Y = 600;
    private static final int PLAYER_SIZE = 60;
    private static final int OBSTACLE_WIDTH = 40;
    private static final int OBSTACLE_HEIGHT = 80;
    private static final int POWERUP_SIZE = 30;
    private static final int ENEMY_SIZE = 50;

    // Game state
    private Timer gameTimer;
    private boolean gameRunning;
    private boolean gameOver;
    private boolean paused;
    private int score;
    private int level;
    private int lives;
    private long gameStartTime;

    // Player properties
    private Player player;
    private boolean[] keys = new boolean[256];

    // Game objects
    private List<Obstacle> obstacles;
    private List<PowerUp> powerUps;
    private List<Enemy> enemies;
    private List<Particle> particles;
    private List<Cloud> clouds;

    // Animation variables
    private int animationFrame;
    private double cameraX;
    private Color skyColor;

    // Sound simulation (visual feedback)
    private boolean soundEffect;
    private int soundEffectTimer;

    public RollerbladingAdventure() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky blue
        setFocusable(true);
        addKeyListener(this);

        initializeGame();

        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();
    }

    private void initializeGame() {
        player = new Player();
        obstacles = new ArrayList<>();
        powerUps = new ArrayList<>();
        enemies = new ArrayList<>();
        particles = new ArrayList<>();
        clouds = new ArrayList<>();

        gameRunning = true;
        gameOver = false;
        paused = false;
        score = 0;
        level = 1;
        lives = 3;
        animationFrame = 0;
        cameraX = 0;
        soundEffect = false;
        soundEffectTimer = 0;
        gameStartTime = System.currentTimeMillis();

        // Initialize clouds
        for (int i = 0; i < 8; i++) {
            clouds.add(new Cloud(i * 200 - 400, 50 + (int)(Math.random() * 100)));
        }

        // Initialize some obstacles
        for (int i = 0; i < 5; i++) {
            obstacles.add(new Obstacle(400 + i * 300));
        }

        // Initialize power-ups
        for (int i = 0; i < 3; i++) {
            powerUps.add(new PowerUp(600 + i * 400));
        }

        // Initialize enemies
        enemies.add(new Enemy(800));
        enemies.add(new Enemy(1400));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (gameRunning && !gameOver) {
            drawGame(g2d);
        } else if (gameOver) {
            drawGameOver(g2d);
        }

        drawUI(g2d);
    }

    private void drawGame(Graphics2D g2d) {
        // Draw animated sky gradient
        GradientPaint skyGradient = new GradientPaint(
                0, 0, new Color(135, 206, 250),
                0, GROUND_Y, new Color(255, 218, 185)
        );
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WINDOW_WIDTH, GROUND_Y);

        // Draw animated clouds
        for (Cloud cloud : clouds) {
            cloud.draw(g2d, cameraX);
        }

        // Draw sun
        g2d.setColor(new Color(255, 255, 0, 180));
        g2d.fillOval(WINDOW_WIDTH - 120, 50, 80, 80);
        g2d.setColor(new Color(255, 255, 100, 100));
        g2d.fillOval(WINDOW_WIDTH - 130, 40, 100, 100);

        // Draw ground with texture
        drawGround(g2d);

        // Draw game objects
        for (Obstacle obstacle : obstacles) {
            obstacle.draw(g2d, cameraX);
        }

        for (PowerUp powerUp : powerUps) {
            powerUp.draw(g2d, cameraX, animationFrame);
        }

        for (Enemy enemy : enemies) {
            enemy.draw(g2d, cameraX, animationFrame);
        }

        // Draw particles
        for (Particle particle : particles) {
            particle.draw(g2d, cameraX);
        }

        // Draw player
        player.draw(g2d, animationFrame);

        // Draw speed lines when player is moving fast
        if (player.velocityX > 8) {
            drawSpeedLines(g2d);
        }

        // Draw sound effect visualization
        if (soundEffect) {
            drawSoundEffect(g2d);
        }

        // Draw pause overlay
        if (paused) {
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            String pauseText = "PAUSED";
            g2d.drawString(pauseText,
                    (WINDOW_WIDTH - fm.stringWidth(pauseText)) / 2,
                    WINDOW_HEIGHT / 2);
        }
    }

    private void drawGround(Graphics2D g2d) {
        // Ground base
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(0, GROUND_Y, WINDOW_WIDTH, WINDOW_HEIGHT - GROUND_Y);

        // Ground details
        g2d.setColor(new Color(46, 125, 50));
        for (int i = 0; i < WINDOW_WIDTH; i += 20) {
            int grassHeight = (int)(Math.sin((i + cameraX * 0.1) * 0.1) * 5 + 10);
            g2d.fillRect(i, GROUND_Y, 2, grassHeight);
        }

        // Path/track
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect(0, GROUND_Y + 50, WINDOW_WIDTH, 30);

        // Path lines
        g2d.setColor(Color.YELLOW);
        for (int i = -100; i < WINDOW_WIDTH + 100; i += 60) {
            int lineX = (int)(i - (cameraX * 0.8) % 60);
            g2d.fillRect(lineX, GROUND_Y + 62, 30, 6);
        }
    }

    private void drawSpeedLines(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setStroke(new BasicStroke(2));
        for (int i = 0; i < 10; i++) {
            int y = 200 + i * 40;
            int startX = WINDOW_WIDTH;
            int endX = WINDOW_WIDTH - 100 - (int)(player.velocityX * 5);
            g2d.drawLine(startX, y, endX, y);
        }
    }

    private void drawSoundEffect(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 0, 100 + soundEffectTimer * 10));
        g2d.setStroke(new BasicStroke(3));
        int centerX = (int)(player.x - cameraX + PLAYER_SIZE / 2);
        int centerY = (int)(player.y + PLAYER_SIZE / 2);
        for (int i = 1; i <= 3; i++) {
            int radius = soundEffectTimer * 5 * i;
            g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        }
    }

    private void drawUI(Graphics2D g2d) {
        // Score and stats background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 300, 120, 15, 15);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("Score: " + score, 25, 35);
        g2d.drawString("Level: " + level, 25, 60);
        g2d.drawString("Lives: " + lives, 25, 85);
        g2d.drawString("Speed: " + String.format("%.1f", player.velocityX), 25, 110);

        // Power-up status
        if (player.powerUpTimer > 0) {
            g2d.setColor(new Color(255, 215, 0, 200));
            g2d.fillRoundRect(WINDOW_WIDTH - 220, 10, 200, 40, 10, 10);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("Power-up: " + (player.powerUpTimer / 60 + 1) + "s",
                    WINDOW_WIDTH - 210, 32);
        }

        // Mini-map
        drawMiniMap(g2d);
    }

    private void drawMiniMap(Graphics2D g2d) {
        int mapX = WINDOW_WIDTH - 250;
        int mapY = WINDOW_HEIGHT - 120;
        int mapWidth = 200;
        int mapHeight = 80;

        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(mapX, mapY, mapWidth, mapHeight, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(mapX, mapY, mapWidth, mapHeight, 10, 10);

        // Player position on mini-map
        int playerMapX = mapX + (int)((player.x / 20) % mapWidth);
        g2d.setColor(Color.GREEN);
        g2d.fillOval(playerMapX - 3, mapY + mapHeight/2 - 3, 6, 6);

        // Obstacles on mini-map
        g2d.setColor(Color.RED);
        for (Obstacle obs : obstacles) {
            if (obs.x > player.x - 1000 && obs.x < player.x + 1000) {
                int obsMapX = mapX + (int)((obs.x / 20) % mapWidth);
                g2d.fillRect(obsMapX - 1, mapY + mapHeight/2 + 5, 2, 10);
            }
        }
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        FontMetrics fm = g2d.getFontMetrics();
        String gameOverText = "GAME OVER";
        g2d.drawString(gameOverText,
                (WINDOW_WIDTH - fm.stringWidth(gameOverText)) / 2,
                WINDOW_HEIGHT / 2 - 100);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        fm = g2d.getFontMetrics();
        String finalScore = "Final Score: " + score;
        g2d.drawString(finalScore,
                (WINDOW_WIDTH - fm.stringWidth(finalScore)) / 2,
                WINDOW_HEIGHT / 2 - 20);

        String restartText = "Press R to Restart";
        g2d.drawString(restartText,
                (WINDOW_WIDTH - fm.stringWidth(restartText)) / 2,
                WINDOW_HEIGHT / 2 + 40);

        // Game stats
        long playTime = (System.currentTimeMillis() - gameStartTime) / 1000;
        String timeText = "Time Played: " + playTime + "s";
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        fm = g2d.getFontMetrics();
        g2d.drawString(timeText,
                (WINDOW_WIDTH - fm.stringWidth(timeText)) / 2,
                WINDOW_HEIGHT / 2 + 100);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning && !paused && !gameOver) {
            updateGame();
        }
        animationFrame++;
        if (soundEffect) {
            soundEffectTimer++;
            if (soundEffectTimer > 10) {
                soundEffect = false;
                soundEffectTimer = 0;
            }
        }
        repaint();
    }

    private void updateGame() {

    }

    private void addParticles(double x, double y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;

        if (e.getKeyCode() == KeyEvent.VK_P && !gameOver) {
            paused = !paused;
        }

        if (e.getKeyCode() == KeyEvent.VK_R && gameOver) {
            initializeGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Game object classes

    class Player {
        double x, y, velocityX, velocityY;
        boolean onGround, invulnerable;
        int powerUpTimer, invulnerableTimer;

        public Player() {
            x = 100;
            y = GROUND_Y - PLAYER_SIZE;
            velocityX = 5;
            velocityY = 0;
            onGround = true;
            powerUpTimer = 0;
            invulnerableTimer = 0;
        }

        public void update(boolean[] keys) {
            // Horizontal movement
            if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]) {
                velocityX = Math.max(velocityX - 0.5, -8);
            } else if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]) {
                velocityX = Math.min(velocityX + 0.3, powerUpTimer > 0 ? 15 : 12);
            } else {
                velocityX = Math.max(velocityX - 0.1, 5); // Maintain minimum speed
            }

            // Jumping
            if ((keys[KeyEvent.VK_SPACE] || keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) && onGround) {
                velocityY = -18;
                onGround = false;
            }

            // Apply gravity
            if (!onGround) {
                velocityY += 0.8;
            }

            // Update position
            x += velocityX;
            y += velocityY;

            // Ground collision
            if (y >= GROUND_Y - PLAYER_SIZE) {
                y = GROUND_Y - PLAYER_SIZE;
                velocityY = 0;
                onGround = true;
            }

            // Update timers
            if (powerUpTimer > 0) powerUpTimer--;
            if (invulnerableTimer > 0) {
                invulnerableTimer--;
                if (invulnerableTimer == 0) invulnerable = false;
            }
        }

        public void draw(Graphics2D g2d, int frame) {
            int drawX = (int)(x - cameraX);
            int drawY = (int)y;

            // Draw shadow
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillOval(drawX + 5, GROUND_Y + 5, PLAYER_SIZE - 10, 15);

            // Invulnerability flashing effect
            if (invulnerable && frame % 10 < 5) return;

            // Player body
            Color playerColor = powerUpTimer > 0 ? Color.YELLOW : Color.BLUE;
            g2d.setColor(playerColor);
            g2d.fillOval(drawX, drawY, PLAYER_SIZE, PLAYER_SIZE);

            // Player details
            g2d.setColor(Color.WHITE);
            g2d.fillOval(drawX + 15, drawY + 15, 12, 8); // Eye
            g2d.fillOval(drawX + 33, drawY + 15, 12, 8); // Eye

            g2d.setColor(Color.BLACK);
            g2d.fillOval(drawX + 17, drawY + 17, 4, 4); // Pupil
            g2d.fillOval(drawX + 35, drawY + 17, 4, 4); // Pupil

            // Helmet
            g2d.setColor(Color.RED);
            g2d.fillArc(drawX - 5, drawY - 5, PLAYER_SIZE + 10, PLAYER_SIZE / 2, 0, 180);

            // Rollerblades (animated)
            g2d.setColor(Color.BLACK);
            int legOffset = (int)(Math.sin(frame * 0.3) * 3);
            g2d.fillRect(drawX + 10 + legOffset, drawY + PLAYER_SIZE - 5, 40, 8);

            // Wheels (spinning animation)
            g2d.setColor(Color.GRAY);
            for (int i = 0; i < 4; i++) {
                int wheelX = drawX + 15 + i * 8;
                int wheelY = drawY + PLAYER_SIZE - 2;
                g2d.fillOval(wheelX, wheelY, 6, 6);

                // Spinning spokes
                g2d.setColor(Color.WHITE);
                double angle = (frame * 0.5 + i * Math.PI / 2) % (Math.PI * 2);
                int spokeX = (int)(wheelX + 3 + Math.cos(angle) * 2);
                int spokeY = (int)(wheelY + 3 + Math.sin(angle) * 2);
                g2d.fillOval(spokeX - 1, spokeY - 1, 2, 2);
                g2d.setColor(Color.GRAY);
            }
        }

        public Rectangle getBounds() {
            return new Rectangle((int)x, (int)y, PLAYER_SIZE, PLAYER_SIZE);
        }
    }

    class Obstacle {
        double x, y;
        Color color;

        public Obstacle(double x) {
            this.x = x;
            this.y = GROUND_Y - OBSTACLE_HEIGHT;
            this.color = new Color(139, 69, 19);
        }

        public void draw(Graphics2D g2d, double cameraX) {
            int drawX = (int)(x - cameraX);

            g2d.setColor(color);
            g2d.fillRect(drawX, (int)y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);

            // Obstacle details
            g2d.setColor(color.darker());
            g2d.drawRect(drawX, (int)y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
            g2d.fillRect(drawX + 5, (int)y + 10, OBSTACLE_WIDTH - 10, 5);
            g2d.fillRect(drawX + 5, (int)y + 25, OBSTACLE_WIDTH - 10, 5);
        }

        public Rectangle getBounds() {
            return new Rectangle((int)x, (int)y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
        }
    }

    class PowerUp {
        double x, y;

        public PowerUp(double x) {
            this.x = x;
            this.y = GROUND_Y - 100;
        }

        public void draw(Graphics2D g2d, double cameraX, int frame) {
            int drawX = (int)(x - cameraX);
            int drawY = (int)(y + Math.sin(frame * 0.1) * 5); // Floating animation

            // Glow effect
            g2d.setColor(new Color(255, 215, 0, 100));
            g2d.fillOval(drawX - 5, drawY - 5, POWERUP_SIZE + 10, POWERUP_SIZE + 10);

            // Power-up star
            g2d.setColor(Color.YELLOW);
            drawStar(g2d, drawX + POWERUP_SIZE/2, drawY + POWERUP_SIZE/2,
                    POWERUP_SIZE/2, POWERUP_SIZE/4, 5, frame * 0.05);
        }

        private void drawStar(Graphics2D g2d, int centerX, int centerY,
                              int outerRadius, int innerRadius, int points, double rotation) {
            int[] xPoints = new int[points * 2];
            int[] yPoints = new int[points * 2];

            for (int i = 0; i < points * 2; i++) {
                double angle = rotation + (i * Math.PI / points);
                int radius = (i % 2 == 0) ? outerRadius : innerRadius;
                xPoints[i] = (int)(centerX + Math.cos(angle) * radius);
                yPoints[i] = (int)(centerY + Math.sin(angle) * radius);
            }

            g2d.fillPolygon(xPoints, yPoints, points * 2);
        }

        public Rectangle getBounds() {
            return new Rectangle((int)x, (int)y, POWERUP_SIZE, POWERUP_SIZE);
        }
    }

    class Enemy {
        double x, y, velocityX;
        boolean defeated;
        int direction;

        public Enemy(double x) {
            this.x = x;
            this.y = GROUND_Y - ENEMY_SIZE;
            this.velocityX = 2;
            this.direction = 1;
        }

        public void update(double playerX) {
            // Simple AI - move towards player when close
            if (Math.abs(x - playerX) < 300) {
                velocityX = playerX > x ? 3 : -3;
            } else {
                velocityX = direction * 2;
            }

            x += velocityX;

            // Change direction occasionally
            if (Math.random() < 0.01) {
                direction *= -1;
            }
        }

        public void draw(Graphics2D g2d, double cameraX, int frame) {
            if (defeated) return;

            int drawX = (int)(x - cameraX);
            int drawY = (int)y;

            // Enemy body
            g2d.setColor(Color.RED);
            g2d.fillOval(drawX, drawY, ENEMY_SIZE, ENEMY_SIZE);

            // Enemy eyes (animated)
            g2d.setColor(Color.WHITE);
            g2d.fillOval(drawX + 10, drawY + 15, 8, 8);
            g2d.fillOval(drawX + 25, drawY + 15, 8, 8);

            g2d.setColor(Color.BLACK);
            int eyeOffset = (int)(Math.sin(frame * 0.2) * 2);
            g2d.fillOval(drawX + 12 + eyeOffset, drawY + 17, 4, 4);
            g2d.fillOval(drawX + 27 + eyeOffset, drawY + 17, 4, 4);

            // Enemy spikes
            g2d.setColor(Color.DARK_GRAY);
            for (int i = 0; i < 5; i++) {
                int spikeX = drawX + i * 10;
                g2d.fillPolygon(
                        new int[]{spikeX, spikeX + 5, spikeX + 10},
                        new int[]{drawY, drawY - 8, drawY},
                        3
                );
            }
        }

        public Rectangle getBounds() {
            return new Rectangle((int)x, (int)y, ENEMY_SIZE, ENEMY_SIZE);
        }
    }

    class Particle {
        double x, y, velocityX, velocityY;
        Color color;
        int life, maxLife;

        public Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.velocityX = (Math.random() - 0.5) * 10;
            this.velocityY = (Math.random() - 0.5) * 10 - 5;
        }

        public void draw(Graphics2D g2d, double cameraX) {
        }

        public void update() {
        }
    }
}