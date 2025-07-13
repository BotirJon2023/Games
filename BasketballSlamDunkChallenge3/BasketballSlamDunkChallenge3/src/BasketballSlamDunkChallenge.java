import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BasketballSlamDunkChallenge extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int GROUND_HEIGHT = 100;
    private static final int BASKET_HEIGHT = 300;
    private static final int BASKET_WIDTH = 100;
    private static final double GRAVITY = 0.5;
    private static final double BOUNCE_DAMPENING = 0.7;
    private static final double AIR_RESISTANCE = 0.98;

    private Timer gameTimer;
    private Player player;
    private Basketball ball;
    private Basket basket;
    private List<Particle> particles;
    private List<PowerUp> powerUps;
    private GameState gameState;
    private SoundManager soundManager;
    private ScoreManager scoreManager;
    private AnimationManager animationManager;
    private BackgroundManager backgroundManager;
    private ComboManager comboManager;
    private Random random;

    private boolean[] keys;
    private boolean mousePressed;
    private Point mousePosition;
    private Point mouseDragStart;

    private int gameTime;
    private int level;
    private boolean showTrajectory;
    private double windForce;
    private boolean slowMotion;
    private int slowMotionTimer;

    public BasketballSlamDunkChallenge() {
        this.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(this);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);

        initializeGame();

        gameTimer = new Timer(16, this); // 60 FPS
        gameTimer.start();
    }

    private void initializeGame() {
        player = new Player(100, WINDOW_HEIGHT - GROUND_HEIGHT - 120);
        ball = new Basketball(150, WINDOW_HEIGHT - GROUND_HEIGHT - 50);
        basket = new Basket(WINDOW_WIDTH - 200, WINDOW_HEIGHT - GROUND_HEIGHT - BASKET_HEIGHT);
        particles = new ArrayList<>();
        powerUps = new ArrayList<>();
        gameState = GameState.PLAYING;
        soundManager = new SoundManager();
        scoreManager = new ScoreManager();
        animationManager = new AnimationManager();
        backgroundManager = new BackgroundManager();
        comboManager = new ComboManager();
        random = new Random();

        keys = new boolean[256];
        mousePosition = new Point(0, 0);
        mouseDragStart = new Point(0, 0);

        gameTime = 0;
        level = 1;
        showTrajectory = false;
        windForce = 0;
        slowMotion = false;
        slowMotionTimer = 0;

        spawnRandomPowerUp();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.PLAYING) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        gameTime++;

        // Update wind force
        if (gameTime % 300 == 0) {
            windForce = (random.nextDouble() - 0.5) * 2.0;
        }

        // Update slow motion
        if (slowMotion) {
            slowMotionTimer--;
            if (slowMotionTimer <= 0) {
                slowMotion = false;
            }
        }

        // Update player
        player.update(keys, slowMotion);

        // Update ball
        ball.update(windForce, slowMotion);

        // Check ball collision with ground
        if (ball.y >= WINDOW_HEIGHT - GROUND_HEIGHT - ball.radius) {
            ball.y = WINDOW_HEIGHT - GROUND_HEIGHT - ball.radius;
            ball.velocityY *= -BOUNCE_DAMPENING;
            ball.velocityX *= 0.9;
            createBounceParticles(ball.x, ball.y + ball.radius);
        }

        // Check ball collision with basket
        checkBasketCollision();

        // Check ball collision with player
        if (ball.isColliding(player)) {
            handlePlayerBallCollision();
        }

        // Update particles
        updateParticles();

        // Update power-ups
        updatePowerUps();

        // Update animations
        animationManager.update();

        // Update background
        backgroundManager.update();

        // Update combo system
        comboManager.update();

        // Check for level progression
        if (scoreManager.getScore() >= level * 1000) {
            level++;
            spawnRandomPowerUp();
        }

        // Spawn power-ups periodically
        if (gameTime % 600 == 0) {
            spawnRandomPowerUp();
        }

        // Reset ball if it goes off screen
        if (ball.x < -50 || ball.x > WINDOW_WIDTH + 50 || ball.y > WINDOW_HEIGHT + 50) {
            resetBall();
        }
    }

    private void checkBasketCollision() {
        Rectangle2D basketBounds = new Rectangle2D.Double(
                basket.x - BASKET_WIDTH/2, basket.y - 10,
                BASKET_WIDTH, 20
        );

        if (basketBounds.contains(ball.x, ball.y) && ball.velocityY > 0) {
            // Score!
            int points = calculateScore();
            scoreManager.addScore(points);
            comboManager.addCombo();

            // Create celebration particles
            createCelebrationParticles(basket.x, basket.y);

            // Add screen shake
            animationManager.addScreenShake(10);

            // Reset ball
            resetBall();

            // Sound effect
            soundManager.playScoreSound();
        }
    }

    private int calculateScore() {
        int baseScore = 100;
        int comboMultiplier = comboManager.getComboMultiplier();
        int distanceBonus = (int)(Math.abs(ball.x - player.x) / 10);
        int styleBonus = player.isJumping() ? 50 : 0;

        return (baseScore + distanceBonus + styleBonus) * comboMultiplier;
    }

    private void handlePlayerBallCollision() {
        double dx = ball.x - player.x;
        double dy = ball.y - player.y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < ball.radius + player.radius) {
            // Calculate collision response
            double angle = Math.atan2(dy, dx);
            double force = 15.0;

            if (player.isJumping()) {
                force *= 1.5; // More power when jumping
            }

            ball.velocityX = Math.cos(angle) * force;
            ball.velocityY = Math.sin(angle) * force;

            // Add player momentum
            ball.velocityX += player.velocityX * 0.5;
            ball.velocityY += player.velocityY * 0.5;

            // Create impact particles
            createImpactParticles(ball.x, ball.y);

            // Sound effect
            soundManager.playBounceSound();
        }
    }

    private void updateParticles() {
        particles.removeIf(p -> {
            p.update();
            return p.isDead();
        });
    }

    private void updatePowerUps() {
        powerUps.removeIf(powerUp -> {
            powerUp.update();

            // Check collision with player
            if (powerUp.isColliding(player)) {
                applyPowerUp(powerUp);
                return true;
            }

            return powerUp.isDead();
        });
    }

    private void applyPowerUp(PowerUp powerUp) {
        switch (powerUp.type) {
            case SPEED_BOOST:
                player.setSpeedBoost(300); // 5 seconds
                break;
            case JUMP_BOOST:
                player.setJumpBoost(300);
                break;
            case SLOW_MOTION:
                slowMotion = true;
                slowMotionTimer = 180; // 3 seconds
                break;
            case SCORE_MULTIPLIER:
                comboManager.addMultiplier(120); // 2 seconds
                break;
        }

        // Create power-up particles
        createPowerUpParticles(powerUp.x, powerUp.y, powerUp.type);
        soundManager.playPowerUpSound();
    }

    private void createBounceParticles(double x, double y) {
        for (int i = 0; i < 5; i++) {
            particles.add(new Particle(x, y, Color.ORANGE, 30));
        }
    }

    private void createImpactParticles(double x, double y) {
        for (int i = 0; i < 8; i++) {
            particles.add(new Particle(x, y, Color.YELLOW, 20));
        }
    }

    private void createCelebrationParticles(double x, double y) {
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(x, y, Color.GREEN, 60));
        }
    }

    private void createPowerUpParticles(double x, double y, PowerUpType type) {
        Color color = getPowerUpColor(type);
        for (int i = 0; i < 10; i++) {
            particles.add(new Particle(x, y, color, 40));
        }
    }

    private Color getPowerUpColor(PowerUpType type) {
        switch (type) {
            case SPEED_BOOST: return Color.RED;
            case JUMP_BOOST: return Color.BLUE;
            case SLOW_MOTION: return new Color(128, 0, 128); // Purple
            case SCORE_MULTIPLIER: return new Color(255, 215, 0); // Gold
            default: return Color.WHITE;
        }
    }

    private void spawnRandomPowerUp() {
        PowerUpType[] types = PowerUpType.values();
        PowerUpType type = types[random.nextInt(types.length)];

        double x = random.nextDouble() * (WINDOW_WIDTH - 100) + 50;
        double y = random.nextDouble() * (WINDOW_HEIGHT - 200) + 100;

        powerUps.add(new PowerUp(x, y, type));
    }

    private void resetBall() {
        ball.x = player.x + 30;
        ball.y = player.y;
        ball.velocityX = 0;
        ball.velocityY = 0;
        comboManager.resetCombo();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Apply screen shake
        if (animationManager.hasScreenShake()) {
            Point shake = animationManager.getScreenShake();
            g2d.translate(shake.x, shake.y);
        }

        // Draw background
        backgroundManager.draw(g2d);

        // Draw ground
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect(0, WINDOW_HEIGHT - GROUND_HEIGHT, WINDOW_WIDTH, GROUND_HEIGHT);

        // Draw trajectory if enabled
        if (showTrajectory && mousePressed) {
            drawTrajectory(g2d);
        }

        // Draw wind indicator
        drawWindIndicator(g2d);

        // Draw game objects
        basket.draw(g2d);
        player.draw(g2d);
        ball.draw(g2d);

        // Draw particles
        for (Particle particle : particles) {
            particle.draw(g2d);
        }

        // Draw power-ups
        for (PowerUp powerUp : powerUps) {
            powerUp.draw(g2d);
        }

        // Draw UI
        drawUI(g2d);

        // Draw slow motion effect
        if (slowMotion) {
            g2d.setColor(new Color(0, 0, 255, 30));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        }
    }

    private void drawTrajectory(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5, 5}, 0));

        double startX = ball.x;
        double startY = ball.y;
        double velX = (mouseDragStart.x - mousePosition.x) * 0.3;
        double velY = (mouseDragStart.y - mousePosition.y) * 0.3;

        for (int i = 0; i < 50; i++) {
            double t = i * 0.5;
            double x = startX + velX * t;
            double y = startY + velY * t + 0.5 * GRAVITY * t * t;

            if (y > WINDOW_HEIGHT - GROUND_HEIGHT) break;

            g2d.fillOval((int)x - 2, (int)y - 2, 4, 4);
        }
    }

    private void drawWindIndicator(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawString("Wind: " + String.format("%.1f", windForce), 20, 100);

        // Draw wind arrow
        int arrowX = 100;
        int arrowY = 85;
        int arrowLength = (int)(windForce * 20);

        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(arrowX, arrowY, arrowX + arrowLength, arrowY);

        if (Math.abs(windForce) > 0.1) {
            // Draw arrow head
            int headSize = 5;
            int direction = windForce > 0 ? 1 : -1;
            g2d.drawLine(arrowX + arrowLength, arrowY,
                    arrowX + arrowLength - headSize * direction, arrowY - headSize);
            g2d.drawLine(arrowX + arrowLength, arrowY,
                    arrowX + arrowLength - headSize * direction, arrowY + headSize);
        }
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));

        // Score
        g2d.drawString("Score: " + scoreManager.getScore(), 20, 30);

        // Level
        g2d.drawString("Level: " + level, 20, 60);

        // Combo
        if (comboManager.getCombo() > 1) {
            g2d.setColor(Color.YELLOW);
            g2d.drawString("Combo: " + comboManager.getCombo() + "x", 200, 30);
        }

        // Power-up status
        drawPowerUpStatus(g2d);

        // Instructions
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("WASD: Move player | Space: Jump | Mouse: Aim and shoot ball | T: Toggle trajectory", 20, WINDOW_HEIGHT - 20);
    }

    private void drawPowerUpStatus(Graphics2D g2d) {
        int y = 150;
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));

        if (player.hasSpeedBoost()) {
            g2d.setColor(Color.RED);
            g2d.drawString("Speed Boost!", 20, y);
            y += 20;
        }

        if (player.hasJumpBoost()) {
            g2d.setColor(Color.BLUE);
            g2d.drawString("Jump Boost!", 20, y);
            y += 20;
        }

        if (slowMotion) {
            g2d.setColor(new Color(128, 0, 128));
            g2d.drawString("Slow Motion!", 20, y);
            y += 20;
        }

        if (comboManager.hasMultiplier()) {
            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString("Score Multiplier!", 20, y);
        }
    }

    // Key and mouse event handlers
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key < keys.length) {
            keys[key] = true;
        }

        if (key == KeyEvent.VK_T) {
            showTrajectory = !showTrajectory;
        }

        if (key == KeyEvent.VK_R) {
            resetBall();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key < keys.length) {
            keys[key] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        mousePressed = true;
        mouseDragStart = new Point(e.getX(), e.getY());
        mousePosition = new Point(e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (mousePressed) {
            // Launch ball
            double velX = (mouseDragStart.x - e.getX()) * 0.3;
            double velY = (mouseDragStart.y - e.getY()) * 0.3;

            ball.velocityX = velX;
            ball.velocityY = velY;

            mousePressed = false;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mousePosition = new Point(e.getX(), e.getY());
    }

    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mouseMoved(MouseEvent e) {}

    // Game classes
    class Player {
        double x, y;
        double velocityX, velocityY;
        double radius = 25;
        boolean isJumping = false;
        boolean onGround = true;

        int speedBoostTimer = 0;
        int jumpBoostTimer = 0;

        double baseSpeed = 5.0;
        double jumpPower = 15.0;

        public Player(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void update(boolean[] keys, boolean slowMotion) {
            double timeMultiplier = slowMotion ? 0.3 : 1.0;

            // Update power-up timers
            if (speedBoostTimer > 0) speedBoostTimer--;
            if (jumpBoostTimer > 0) jumpBoostTimer--;

            // Calculate speeds
            double currentSpeed = baseSpeed * (hasSpeedBoost() ? 2.0 : 1.0);
            double currentJumpPower = jumpPower * (hasJumpBoost() ? 1.5 : 1.0);

            // Horizontal movement
            if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT]) {
                velocityX = -currentSpeed * timeMultiplier;
            } else if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT]) {
                velocityX = currentSpeed * timeMultiplier;
            } else {
                velocityX *= 0.8; // Friction
            }

            // Jumping
            if ((keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_SPACE]) && onGround) {
                velocityY = -currentJumpPower;
                isJumping = true;
                onGround = false;
            }

            // Apply gravity
            velocityY += GRAVITY * timeMultiplier;

            // Update position
            x += velocityX;
            y += velocityY * timeMultiplier;

            // Ground collision
            if (y >= WINDOW_HEIGHT - GROUND_HEIGHT - radius) {
                y = WINDOW_HEIGHT - GROUND_HEIGHT - radius;
                velocityY = 0;
                isJumping = false;
                onGround = true;
            }

            // Keep player on screen
            if (x < radius) x = radius;
            if (x > WINDOW_WIDTH - radius) x = WINDOW_WIDTH - radius;
        }

        public void draw(Graphics2D g2d) {
            // Draw player shadow
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillOval((int)(x - radius), (int)(WINDOW_HEIGHT - GROUND_HEIGHT - 5),
                    (int)(radius * 2), 10);

            // Draw player
            Color playerColor = Color.BLUE;
            if (hasSpeedBoost()) {
                playerColor = Color.RED;
            } else if (hasJumpBoost()) {
                playerColor = Color.CYAN;
            }

            g2d.setColor(playerColor);
            g2d.fillOval((int)(x - radius), (int)(y - radius),
                    (int)(radius * 2), (int)(radius * 2));

            // Draw player details
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int)(x - 8), (int)(y - 8), 4, 4); // Left eye
            g2d.fillOval((int)(x + 4), (int)(y - 8), 4, 4); // Right eye

            // Draw power-up effects
            if (hasSpeedBoost()) {
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval((int)(x - radius - 5), (int)(y - radius - 5),
                        (int)(radius * 2 + 10), (int)(radius * 2 + 10));
            }

            if (hasJumpBoost()) {
                g2d.setColor(Color.CYAN);
                g2d.setStroke(new BasicStroke(2));
                for (int i = 0; i < 8; i++) {
                    double angle = i * Math.PI / 4;
                    int x1 = (int)(x + Math.cos(angle) * (radius + 10));
                    int y1 = (int)(y + Math.sin(angle) * (radius + 10));
                    int x2 = (int)(x + Math.cos(angle) * (radius + 15));
                    int y2 = (int)(y + Math.sin(angle) * (radius + 15));
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }
        }

        public boolean isJumping() { return isJumping; }
        public boolean hasSpeedBoost() { return speedBoostTimer > 0; }
        public boolean hasJumpBoost() { return jumpBoostTimer > 0; }

        public void setSpeedBoost(int duration) { speedBoostTimer = duration; }
        public void setJumpBoost(int duration) { jumpBoostTimer = duration; }
    }

    class Basketball {
        double x, y;
        double velocityX, velocityY;
        double radius = 15;
        double rotation = 0;

        public Basketball(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void update(double windForce, boolean slowMotion) {
            double timeMultiplier = slowMotion ? 0.3 : 1.0;

            // Apply gravity
            velocityY += GRAVITY * timeMultiplier;

            // Apply wind force
            velocityX += windForce * 0.1 * timeMultiplier;

            // Apply air resistance
            velocityX *= Math.pow(AIR_RESISTANCE, timeMultiplier);
            velocityY *= Math.pow(AIR_RESISTANCE, timeMultiplier);

            // Update position
            x += velocityX * timeMultiplier;
            y += velocityY * timeMultiplier;

            // Update rotation based on velocity
            rotation += (velocityX * 0.1) * timeMultiplier;
        }

        public void draw(Graphics2D g2d) {
            // Draw ball shadow
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillOval((int)(x - radius), (int)(WINDOW_HEIGHT - GROUND_HEIGHT - 5),
                    (int)(radius * 2), 10);

            // Save transform
            AffineTransform oldTransform = g2d.getTransform();

            // Rotate for ball
            g2d.translate(x, y);
            g2d.rotate(rotation);

            // Draw basketball
            g2d.setColor(Color.ORANGE);
            g2d.fillOval((int)(-radius), (int)(-radius),
                    (int)(radius * 2), (int)(radius * 2));

            // Draw basketball lines
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(0, (int)(-radius), 0, (int)(radius)); // Vertical line
            g2d.drawLine((int)(-radius), 0, (int)(radius), 0); // Horizontal line

            // Draw curved lines
            g2d.drawArc((int)(-radius), (int)(-radius), (int)(radius * 2), (int)(radius * 2), 0, 180);
            g2d.drawArc((int)(-radius), (int)(-radius), (int)(radius * 2), (int)(radius * 2), 180, 180);

            // Restore transform
            g2d.setTransform(oldTransform);
        }

        public boolean isColliding(Player player) {
            double dx = x - player.x;
            double dy = y - player.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            return distance < radius + player.radius;
        }
    }

    class Basket {
        double x, y;

        public Basket(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void draw(Graphics2D g2d) {
            // Draw backboard
            g2d.setColor(Color.WHITE);
            g2d.fillRect((int)(x + BASKET_WIDTH/2), (int)(y - 100), 10, 120);

            // Draw rim
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(5));
            g2d.drawLine((int)(x - BASKET_WIDTH/2), (int)y, (int)(x + BASKET_WIDTH/2), (int)y);

            // Draw net
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            for (int i = 0; i < 6; i++) {
                double netX = x - BASKET_WIDTH/2 + (i * BASKET_WIDTH/5);
                g2d.drawLine((int)netX, (int)y, (int)(netX + 5), (int)(y + 25));
            }

            // Draw pole
            g2d.setColor(Color.GRAY);
            g2d.fillRect((int)(x + BASKET_WIDTH/2 + 5), (int)y, 10,
                    (int)(WINDOW_HEIGHT - GROUND_HEIGHT - y));
        }
    }

    class Particle {
        double x, y;
        double velocityX, velocityY;
        Color color;
        int life, maxLife;

        public Particle(double x, double y, Color color, int life) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.life = life;
            this.maxLife = life;

            // Random velocity
            Random rand = new Random();
            velocityX = (rand.nextDouble() - 0.5) * 10;
            velocityY = (rand.nextDouble() - 0.5) * 10 - 5;
        }

        public void update() {
            x += velocityX;
            y += velocityY;
            velocityY += 0.3; // Gravity
            life--;
        }

        public void draw(Graphics2D g2d) {
            float alpha = (float)life / maxLife;
            Color fadeColor = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int)(alpha * 255));
            g2d.setColor(fadeColor);
            g2d.fillOval((int)(x - 3), (int)(y - 3), 6, 6);
        }

        public boolean isDead() { return life <= 0; }
    }

    enum PowerUpType {
        SPEED_BOOST, JUMP_BOOST, SLOW_MOTION, SCORE_MULTIPLIER
    }

    class PowerUp {
        double x, y;
        PowerUpType type;
        int life = 600; // 10 seconds
        double bobOffset = 0;

        public PowerUp(double x, double y, PowerUpType type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }

        public void update() {
            life--;
            bobOffset += 0.1;
        }

        public void draw(Graphics2D g2d) {
            double drawY = y + Math.sin(bobOffset) * 5;

            // Draw glow effect
            Color glowColor = getPowerUpColor(type);
            g2d.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 50));
            g2d.fillOval((int)(x - 25), (int)(drawY - 25), 50, 50);

            // Draw power-up icon
            g2d.setColor(glowColor);
            g2d.fillOval((int)(x - 15), (int)(drawY - 15), 30, 30);

            // Draw symbol
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            String symbol = getPowerUpSymbol(type);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(symbol);
            g2d.drawString(symbol, (int)(x - textWidth/2), (int)(drawY + 5));
        }

        private String getPowerUpSymbol(PowerUpType type) {
            switch (type) {
                case SPEED_BOOST: return "S";
                case JUMP_BOOST: return "J";
                case SLOW_MOTION: return "T";
                case SCORE_MULTIPLIER: return "X";
                default: return "?";
            }
        }

        public boolean isColliding(Player player) {
            double dx = x - player.x;
            double dy = y - player.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            return distance < 15 + player.radius;
        }

        public boolean isDead() { return life <= 0; }
    }

    class ScoreManager {
        private int score = 0;
        private int highScore = 0;

        public void addScore(int points) {
            score += points;
            if (score > highScore) {
                highScore = score;
            }
        }

        public int getScore() { return score; }
        public int getHighScore() { return highScore; }

        public void reset() {
            score = 0;
        }
    }

    class ComboManager {
        private int combo = 0;
        private int comboTimer = 0;
        private int multiplierTimer = 0;

        public void addCombo() {
            combo++;
            comboTimer = 300; // 5 seconds to maintain combo
        }

        public void resetCombo() {
            combo = 0;
            comboTimer = 0;
        }

        public void addMultiplier(int duration) {
            multiplierTimer = duration;
        }

        public void update() {
            if (comboTimer > 0) {
                comboTimer--;
                if (comboTimer <= 0) {
                    combo = 0;
                }
            }

            if (multiplierTimer > 0) {
                multiplierTimer--;
            }
        }

        public int getCombo() { return combo; }
        public int getComboMultiplier() {
            int baseMultiplier = Math.max(1, combo);
            return hasMultiplier() ? baseMultiplier * 2 : baseMultiplier;
        }
        public boolean hasMultiplier() { return multiplierTimer > 0; }
    }

    class AnimationManager {
        private Point screenShake = new Point(0, 0);
        private int shakeIntensity = 0;
        private int shakeTimer = 0;
        private Random random = new Random();

        public void addScreenShake(int intensity) {
            shakeIntensity = intensity;
            shakeTimer = 30; // 0.5 seconds
        }

        public void update() {
            if (shakeTimer > 0) {
                shakeTimer--;
                screenShake.x = random.nextInt(shakeIntensity * 2) - shakeIntensity;
                screenShake.y = random.nextInt(shakeIntensity * 2) - shakeIntensity;

                // Gradually reduce shake intensity
                shakeIntensity = (int)(shakeIntensity * 0.9);
            } else {
                screenShake.x = 0;
                screenShake.y = 0;
            }
        }

        public boolean hasScreenShake() { return shakeTimer > 0; }
        public Point getScreenShake() { return screenShake; }
    }

    class BackgroundManager {
        private List<Cloud> clouds;
        private List<Star> stars;
        private Color skyColor;
        private int timeOfDay = 0;

        public BackgroundManager() {
            clouds = new ArrayList<>();
            stars = new ArrayList<>();
            initializeClouds();
            initializeStars();
        }

        private void initializeClouds() {
            Random rand = new Random();
            for (int i = 0; i < 5; i++) {
                clouds.add(new Cloud(rand.nextDouble() * WINDOW_WIDTH,
                        rand.nextDouble() * 200 + 50));
            }
        }

        private void initializeStars() {
            Random rand = new Random();
            for (int i = 0; i < 50; i++) {
                stars.add(new Star(rand.nextDouble() * WINDOW_WIDTH,
                        rand.nextDouble() * 300 + 50));
            }
        }

        public void update() {
            timeOfDay++;

            // Update sky color based on time
            float dayProgress = (timeOfDay % 3600) / 3600.0f;
            if (dayProgress < 0.5f) {
                // Day to evening
                skyColor = interpolateColor(Color.CYAN, Color.ORANGE, dayProgress * 2);
            } else {
                // Evening to night
                skyColor = interpolateColor(Color.ORANGE, new Color(0, 0, 139), (dayProgress - 0.5f) * 2);
            }

            // Update clouds
            for (Cloud cloud : clouds) {
                cloud.update();
            }

            // Update stars
            for (Star star : stars) {
                star.update();
            }
        }

        private Color interpolateColor(Color c1, Color c2, float ratio) {
            int r = (int)(c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
            int g = (int)(c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
            int b = (int)(c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
            return new Color(r, g, b);
        }

        public void draw(Graphics2D g2d) {
            // Draw sky gradient
            GradientPaint skyGradient = new GradientPaint(0, 0, skyColor,
                    0, WINDOW_HEIGHT/2, Color.WHITE);
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT/2);

            // Draw stars (only visible at night)
            if (skyColor.getBlue() > 100) {
                for (Star star : stars) {
                    star.draw(g2d);
                }
            }

            // Draw clouds
            for (Cloud cloud : clouds) {
                cloud.draw(g2d);
            }
        }

        class Cloud {
            double x, y;
            double speed;
            double size;

            public Cloud(double x, double y) {
                this.x = x;
                this.y = y;
                this.speed = 0.5 + Math.random() * 0.5;
                this.size = 30 + Math.random() * 40;
            }

            public void update() {
                x += speed;
                if (x > WINDOW_WIDTH + size) {
                    x = -size;
                }
            }

            public void draw(Graphics2D g2d) {
                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.fillOval((int)(x - size/2), (int)(y - size/4), (int)size, (int)(size/2));
                g2d.fillOval((int)(x - size/3), (int)(y - size/3), (int)(size/1.5), (int)(size/1.5));
                g2d.fillOval((int)(x + size/4), (int)(y - size/4), (int)(size/1.2), (int)(size/1.2));
            }
        }

        class Star {
            double x, y;
            double brightness;
            double twinkleSpeed;

            public Star(double x, double y) {
                this.x = x;
                this.y = y;
                this.brightness = Math.random();
                this.twinkleSpeed = 0.02 + Math.random() * 0.03;
            }

            public void update() {
                brightness += twinkleSpeed;
                if (brightness > 1.0 || brightness < 0.2) {
                    twinkleSpeed *= -1;
                }
            }

            public void draw(Graphics2D g2d) {
                int alpha = (int)(brightness * 255);
                g2d.setColor(new Color(255, 255, 255, alpha));
                g2d.fillOval((int)x, (int)y, 2, 2);
            }
        }
    }

    class SoundManager {
        // Simple sound simulation through visual feedback
        public void playBounceSound() {
            // In a real implementation, this would play a sound
            System.out.println("*bounce*");
        }

        public void playScoreSound() {
            System.out.println("*score*");
        }

        public void playPowerUpSound() {
            System.out.println("*power-up*");
        }
    }

    enum GameState {
        MENU, PLAYING, PAUSED, GAME_OVER
    }

    // Main method and game setup
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Basketball Slam Dunk Challenge");
            BasketballSlamDunkChallenge game = new BasketballSlamDunkChallenge();

            frame.add(game);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Focus on the game panel for key events
            game.requestFocusInWindow();
        });
    }
}