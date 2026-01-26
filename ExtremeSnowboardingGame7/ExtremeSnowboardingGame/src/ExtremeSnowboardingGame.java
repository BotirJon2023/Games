import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.awt.image.BufferedImage;

public class ExtremeSnowboardingGame extends JPanel implements Runnable, KeyListener {

    // Game Constants
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final int GROUND_LEVEL = 600;
    private static final double GRAVITY = 0.5;
    private static final double FRICTION = 0.98;
    private static final double JUMP_FORCE = -15;
    private static final double SNOWBOARD_TURN_SPEED = 0.15;
    private static final double MAX_SPEED = 20;

    // Game State
    private enum GameState { START, PLAYING, PAUSED, GAME_OVER, LEVEL_COMPLETE }
    private GameState gameState = GameState.START;

    // Player
    private double playerX = 100;
    private double playerY = GROUND_LEVEL - 50;
    private double playerVelX = 5;
    private double playerVelY = 0;
    private double playerAngle = 0;
    private int score = 0;
    private int lives = 3;
    private int combo = 0;
    private int trickPoints = 0;
    private int airTime = 0;
    private boolean isGrinding = false;
    private boolean isInAir = false;
    private int flipCount = 0;

    // Animation States
    private enum AnimationState { RIDING, JUMPING, GRINDING, FLIPPING, CRASHING }
    private AnimationState animationState = AnimationState.RIDING;
    private int animationFrame = 0;
    private int animationDelay = 0;

    // Track Elements
    private List<Point2D.Double> trackPoints;
    private List<Obstacle> obstacles;
    private List<JumpRamp> ramps;
    private List<Coin> coins;
    private List<Particle> particles;

    // Visual Effects
    private Color[] snowColors = {new Color(240, 248, 255), new Color(224, 255, 255),
            new Color(173, 216, 230)};
    private List<Snowflake> snowflakes;
    private float parallaxOffset = 0;

    // Timing
    private long lastTime;
    private int fps = 60;
    private int levelTime = 120; // 2 minutes

    // Colors
    private Color[] terrainColors = {
            new Color(176, 224, 230), // Snow
            new Color(144, 238, 144), // Grass
            new Color(139, 69, 19),   // Dirt
            new Color(105, 105, 105)  // Rocks
    };

    // Input
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean spacePressed = false;
    private boolean trickPressed = false;

    // Images for animation
    private BufferedImage[] playerSprites;
    private BufferedImage snowboardImage;
    private BufferedImage treeImage;
    private BufferedImage rockImage;
    private BufferedImage coinImage;

    // Sound flags
    private boolean playJumpSound = false;
    private boolean playGrindSound = false;
    private boolean playCrashSound = false;
    private boolean playCoinSound = false;

    // Level design
    private int currentLevel = 1;
    private int coinsCollected = 0;
    private int totalCoins = 50;

    public ExtremeSnowboardingGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky blue
        setFocusable(true);
        addKeyListener(this);

        initializeGame();
        createSprites();

        // Start game thread
        Thread gameThread = new Thread(this);
        gameThread.start();
    }

    private void initializeGame() {
        // Initialize collections
        trackPoints = new ArrayList<>();
        obstacles = new ArrayList<>();
        ramps = new ArrayList<>();
        coins = new ArrayList<>();
        particles = new ArrayList<>();
        snowflakes = new ArrayList<>();

        // Generate track
        generateTrack();

        // Generate obstacles
        generateObstacles();

        // Generate ramps
        generateRamps();

        // Generate coins
        generateCoins();

        // Generate snowflakes
        for (int i = 0; i < 200; i++) {
            snowflakes.add(new Snowflake());
        }

        lastTime = System.currentTimeMillis();
    }

    private void createSprites() {
        playerSprites = new BufferedImage[8];
        for (int i = 0; i < playerSprites.length; i++) {
            playerSprites[i] = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = playerSprites[i].createGraphics();
            drawPlayerSprite(g2d, i);
            g2d.dispose();
        }

        // Create simple images for other objects
        snowboardImage = new BufferedImage(80, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = snowboardImage.createGraphics();
        g2d.setColor(new Color(255, 50, 50));
        g2d.fillRoundRect(0, 0, 80, 10, 5, 5);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(0, 0, 80, 10, 5, 5);
        g2d.dispose();
    }

    private void drawPlayerSprite(Graphics2D g2d, int frame) {
        // Draw a stylized snowboarder
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Body
        g2d.setColor(new Color(255, 255, 255));
        g2d.fillOval(15, 10, 30, 40);

        // Helmet
        g2d.setColor(new Color(255, 0, 0));
        g2d.fillOval(18, 5, 24, 15);

        // Goggles
        g2d.setColor(new Color(0, 100, 255));
        g2d.fillOval(20, 12, 20, 8);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(20, 12, 20, 8);

        // Arms and legs based on animation frame
        int armOffset = (frame % 4) * 3;
        g2d.setColor(new Color(0, 0, 150));
        g2d.fillRect(10, 25, 10, 15 + armOffset);
        g2d.fillRect(40, 25, 10, 15 - armOffset);

        g2d.setColor(new Color(100, 100, 100));
        g2d.fillRect(15, 45, 10, 15);
        g2d.fillRect(35, 45, 10, 15);
    }

    private void generateTrack() {
        trackPoints.clear();
        double x = 0;
        double y = GROUND_LEVEL;

        for (int i = 0; i < 50; i++) {
            trackPoints.add(new Point2D.Double(x, y));
            x += 100 + Math.random() * 100;
            y = GROUND_LEVEL - 100 + Math.sin(i * 0.5) * 150 + Math.random() * 100;
        }
    }

    private void generateObstacles() {
        obstacles.clear();
        Random rand = new Random();

        for (int i = 0; i < 20; i++) {
            double x = 300 + rand.nextDouble() * (WIDTH * 3);
            double y = getTrackHeight(x) - 30;
            obstacles.add(new Obstacle(x, y, rand.nextInt(4)));
        }
    }

    private void generateRamps() {
        ramps.clear();
        Random rand = new Random();

        for (int i = 0; i < 10; i++) {
            double x = 200 + rand.nextDouble() * (WIDTH * 3);
            double y = getTrackHeight(x) - 20;
            ramps.add(new JumpRamp(x, y, 100, 40));
        }
    }

    private void generateCoins() {
        coins.clear();
        Random rand = new Random();

        for (int i = 0; i < totalCoins; i++) {
            double x = 100 + rand.nextDouble() * (WIDTH * 3);
            double y = getTrackHeight(x) - 100 + rand.nextDouble() * 200;
            coins.add(new Coin(x, y));
        }
    }

    private double getTrackHeight(double x) {
        for (int i = 0; i < trackPoints.size() - 1; i++) {
            Point2D.Double p1 = trackPoints.get(i);
            Point2D.Double p2 = trackPoints.get(i + 1);

            if (x >= p1.x && x <= p2.x) {
                double t = (x - p1.x) / (p2.x - p1.x);
                return p1.y + t * (p2.y - p1.y);
            }
        }
        return GROUND_LEVEL;
    }

    @Override
    public void run() {
        while (true) {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - lastTime;

            if (elapsed > 1000 / fps) {
                update();
                repaint();
                lastTime = currentTime;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        if (gameState != GameState.PLAYING) return;

        // Update player movement
        updatePlayer();

        // Update physics
        applyPhysics();

        // Check collisions
        checkCollisions();

        // Update animations
        updateAnimations();

        // Update particles
        updateParticles();

        // Update snowflakes
        updateSnowflakes();

        // Update parallax
        parallaxOffset += playerVelX * 0.1;

        // Update level time
        levelTime--;
        if (levelTime <= 0) {
            gameState = GameState.GAME_OVER;
        }

        // Update combo timer
        if (combo > 0) {
            combo--;
            if (combo == 0) {
                trickPoints = 0;
            }
        }
    }

    private void updatePlayer() {
        // Handle input
        if (leftPressed) {
            playerAngle -= SNOWBOARD_TURN_SPEED;
            playerVelX *= 0.95;
        }
        if (rightPressed) {
            playerAngle += SNOWBOARD_TURN_SPEED;
            playerVelX = Math.min(playerVelX + 0.2, MAX_SPEED);
        }
        if (upPressed && !isInAir) {
            playerVelY = JUMP_FORCE;
            isInAir = true;
            playJumpSound = true;
            animationState = AnimationState.JUMPING;
            animationFrame = 0;
        }
        if (downPressed) {
            playerVelY += 1;
        }
        if (trickPressed && isInAir) {
            performTrick();
        }

        // Apply rotation
        playerAngle = playerAngle % (2 * Math.PI);

        // Apply gravity
        playerVelY += GRAVITY;

        // Update position
        playerX += playerVelX;
        playerY += playerVelY;

        // Check if on track
        double trackHeight = getTrackHeight(playerX);
        if (playerY >= trackHeight - 20) {
            playerY = trackHeight - 20;
            playerVelY = 0;
            isInAir = false;

            if (animationState == AnimationState.JUMPING ||
                    animationState == AnimationState.FLIPPING) {
                landTrick();
            }

            animationState = AnimationState.RIDING;
        } else {
            isInAir = true;
            airTime++;

            if (airTime > 30 && animationState != AnimationState.FLIPPING) {
                animationState = AnimationState.JUMPING;
            }
        }

        // Limit player to track boundaries
        if (playerX < 100) playerX = 100;
        if (playerY < 0) playerY = 0;
    }

    private void applyPhysics() {
        // Apply friction
        if (!isInAir) {
            playerVelX *= FRICTION;
        }

        // Dampen rotation
        if (!isInAir) {
            playerAngle *= 0.9;
        }
    }

    private void checkCollisions() {
        // Check obstacle collisions
        for (Obstacle obstacle : obstacles) {
            if (Math.abs(playerX - obstacle.x) < 30 &&
                    Math.abs(playerY - obstacle.y) < 30) {
                crash();
                return;
            }
        }

        // Check ramp collisions
        for (JumpRamp ramp : ramps) {
            if (playerX >= ramp.x && playerX <= ramp.x + ramp.width &&
                    playerY >= ramp.y - 10 && playerY <= ramp.y + ramp.height) {
                playerVelY = -20;
                isInAir = true;
                animationState = AnimationState.JUMPING;
                animationFrame = 0;
            }
        }

        // Check coin collisions
        Iterator<Coin> coinIter = coins.iterator();
        while (coinIter.hasNext()) {
            Coin coin = coinIter.next();
            double distance = Math.sqrt(
                    Math.pow(playerX - coin.x, 2) +
                            Math.pow(playerY - coin.y, 2)
            );

            if (distance < 25) {
                coinIter.remove();
                score += 100;
                coinsCollected++;
                playCoinSound = true;

                // Add particles
                for (int i = 0; i < 10; i++) {
                    particles.add(new Particle(
                            coin.x, coin.y,
                            Math.random() * 4 - 2,
                            Math.random() * 4 - 2,
                            new Color(255, 215, 0),
                            30
                    ));
                }

                if (coinsCollected >= totalCoins) {
                    levelComplete();
                }
            }
        }
    }

    private void performTrick() {
        if (isInAir && airTime > 10) {
            flipCount++;
            playerAngle += Math.PI / 2;
            trickPoints += 100;
            combo = 60;
            animationState = AnimationState.FLIPPING;
            animationFrame = 0;

            // Add trail particles
            particles.add(new Particle(
                    playerX, playerY,
                    Math.random() * 2 - 1,
                    Math.random() * 2 - 1,
                    new Color(255, 100, 100),
                    20
            ));
        }
    }

    private void landTrick() {
        if (flipCount > 0) {
            score += trickPoints * (1 + flipCount / 10.0);
            trickPoints = 0;
            flipCount = 0;
        }
        airTime = 0;
    }

    private void crash() {
        lives--;
        animationState = AnimationState.CRASHING;
        animationFrame = 0;
        playCrashSound = true;

        // Add crash particles
        for (int i = 0; i < 50; i++) {
            particles.add(new Particle(
                    playerX, playerY,
                    Math.random() * 10 - 5,
                    Math.random() * 10 - 5,
                    new Color(200, 200, 200),
                    60
            ));
        }

        if (lives <= 0) {
            gameState = GameState.GAME_OVER;
        } else {
            // Reset position
            playerX = 100;
            playerY = getTrackHeight(playerX) - 20;
            playerVelX = 5;
            playerVelY = 0;
        }
    }

    private void levelComplete() {
        gameState = GameState.LEVEL_COMPLETE;
        score += levelTime * 10;
    }

    private void updateAnimations() {
        animationDelay++;
        if (animationDelay > 5) {
            animationFrame = (animationFrame + 1) % 8;
            animationDelay = 0;
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
    }

    private void updateSnowflakes() {
        for (Snowflake snowflake : snowflakes) {
            snowflake.update();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background
        drawBackground(g2d);

        // Draw track
        drawTrack(g2d);

        // Draw obstacles
        drawObstacles(g2d);

        // Draw ramps
        drawRamps(g2d);

        // Draw coins
        drawCoins(g2d);

        // Draw particles
        drawParticles(g2d);

        // Draw player
        drawPlayer(g2d);

        // Draw snowflakes
        drawSnowflakes(g2d);

        // Draw UI
        drawUI(g2d);

        // Draw game state screens
        drawGameState(g2d);
    }

    private void drawBackground(Graphics2D g2d) {
        // Gradient sky
        GradientPaint skyGradient = new GradientPaint(
                0, 0, new Color(135, 206, 235),
                0, HEIGHT/2, new Color(240, 248, 255)
        );
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT/2);

        // Mountains
        g2d.setColor(new Color(120, 120, 120));
        int[] mountainX = {0, 200, 400, 600, 800, 1000, 1200};
        int[] mountainY = {400, 200, 350, 150, 300, 250, 400};
        g2d.fillPolygon(mountainX, mountainY, mountainX.length);

        // Clouds
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < 5; i++) {
            int cloudX = (int)((i * 300 + parallaxOffset * 0.2) % (WIDTH + 400)) - 200;
            g2d.fillOval(cloudX, 50, 100, 40);
            g2d.fillOval(cloudX + 40, 30, 80, 50);
            g2d.fillOval(cloudX + 80, 50, 100, 40);
        }
    }

    private void drawTrack(Graphics2D g2d) {
        // Draw track as a smooth curve
        g2d.setColor(terrainColors[0]);

        Path2D track = new Path2D.Double();
        if (trackPoints.size() > 0) {
            track.moveTo(0, HEIGHT);
            track.lineTo(0, trackPoints.get(0).y);

            for (int i = 0; i < trackPoints.size() - 1; i++) {
                Point2D.Double p1 = trackPoints.get(i);
                Point2D.Double p2 = trackPoints.get(i + 1);

                double ctrlX1 = p1.x + (p2.x - p1.x) / 3;
                double ctrlY1 = p1.y;
                double ctrlX2 = p1.x + 2 * (p2.x - p1.x) / 3;
                double ctrlY2 = p2.y;

                track.curveTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, p2.x, p2.y);
            }

            track.lineTo(WIDTH * 3, HEIGHT);
            track.closePath();
            g2d.fill(track);

            // Draw track details
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D trackLine = new Path2D.Double();
            trackLine.moveTo(0, trackPoints.get(0).y);

            for (int i = 0; i < trackPoints.size() - 1; i++) {
                Point2D.Double p1 = trackPoints.get(i);
                Point2D.Double p2 = trackPoints.get(i + 1);
                trackLine.lineTo(p2.x, p2.y);
            }
            g2d.draw(trackLine);
        }
    }

    private void drawObstacles(Graphics2D g2d) {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.x - playerX > -100 && obstacle.x - playerX < WIDTH + 100) {
                g2d.setColor(terrainColors[obstacle.type + 1]);
                g2d.fillRect((int)obstacle.x - 15, (int)obstacle.y - 15, 30, 30);

                // Add detail
                g2d.setColor(Color.DARK_GRAY);
                if (obstacle.type == 0) { // Tree
                    g2d.fillRect((int)obstacle.x - 3, (int)obstacle.y + 5, 6, 20);
                    g2d.setColor(new Color(34, 139, 34));
                    g2d.fillOval((int)obstacle.x - 15, (int)obstacle.y - 20, 30, 25);
                } else if (obstacle.type == 1) { // Rock
                    g2d.fillOval((int)obstacle.x - 10, (int)obstacle.y - 10, 20, 20);
                }
            }
        }
    }

    private void drawRamps(Graphics2D g2d) {
        for (JumpRamp ramp : ramps) {
            if (ramp.x - playerX > -100 && ramp.x - playerX < WIDTH + 100) {
                g2d.setColor(new Color(139, 90, 43));
                Polygon rampPoly = new Polygon();
                rampPoly.addPoint((int)ramp.x, (int)ramp.y);
                rampPoly.addPoint((int)ramp.x + ramp.width, (int)ramp.y - ramp.height);
                rampPoly.addPoint((int)ramp.x + ramp.width, (int)ramp.y);
                g2d.fill(rampPoly);

                g2d.setColor(new Color(160, 120, 80));
                g2d.draw(rampPoly);
            }
        }
    }

    private void drawCoins(Graphics2D g2d) {
        for (Coin coin : coins) {
            if (coin.x - playerX > -50 && coin.x - playerX < WIDTH + 50) {
                g2d.setColor(new Color(255, 215, 0));
                g2d.fillOval((int)coin.x - 10, (int)coin.y - 10, 20, 20);
                g2d.setColor(new Color(255, 255, 0));
                g2d.fillOval((int)coin.x - 7, (int)coin.y - 7, 14, 14);

                // Add shine effect
                g2d.setColor(Color.WHITE);
                g2d.fillOval((int)coin.x - 3, (int)coin.y - 8, 6, 6);
            }
        }
    }

    private void drawParticles(Graphics2D g2d) {
        for (Particle particle : particles) {
            particle.draw(g2d);
        }
    }

    private void drawPlayer(Graphics2D g2d) {
        // Save transform
        AffineTransform oldTransform = g2d.getTransform();

        // Position in center of screen
        int screenX = WIDTH / 2;
        int screenY = HEIGHT / 2;

        // Translate and rotate
        g2d.translate(screenX, screenY);
        g2d.rotate(playerAngle);

        // Draw snowboard
        g2d.drawImage(snowboardImage, -40, 5, null);

        // Draw player sprite based on animation state
        int spriteIndex = animationFrame;
        if (animationState == AnimationState.CRASHING) {
            spriteIndex = 7;
        }

        g2d.drawImage(playerSprites[spriteIndex], -30, -30, null);

        // Draw trail when going fast
        if (playerVelX > 10) {
            g2d.setColor(new Color(255, 255, 255, 100));
            for (int i = 0; i < 5; i++) {
                g2d.fillOval(-40 - i * 10, 10, 10 + i * 2, 5);
            }
        }

        // Restore transform
        g2d.setTransform(oldTransform);

        // Draw player shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(screenX - 25, screenY + 15, 50, 15);
    }

    private void drawSnowflakes(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        for (Snowflake snowflake : snowflakes) {
            g2d.fillOval((int)snowflake.x, (int)snowflake.y,
                    snowflake.size, snowflake.size);
        }
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);

        // Score
        g2d.drawString("Score: " + score, 20, 40);

        // Lives
        g2d.drawString("Lives: " + lives, 20, 80);

        // Speed
        g2d.drawString(String.format("Speed: %.1f", playerVelX), 20, 120);

        // Time
        int minutes = levelTime / 60;
        int seconds = levelTime % 60;
        g2d.drawString(String.format("Time: %02d:%02d", minutes, seconds), 20, 160);

        // Coins
        g2d.drawString("Coins: " + coinsCollected + "/" + totalCoins, 20, 200);

        // Combo
        if (combo > 0) {
            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString("Combo: " + trickPoints + " x" + (1 + flipCount / 10.0),
                    WIDTH - 300, 40);

            // Combo meter
            g2d.setColor(new Color(255, 0, 0, 100));
            g2d.fillRect(WIDTH - 300, 50, combo, 20);
            g2d.setColor(Color.RED);
            g2d.drawRect(WIDTH - 300, 50, 60, 20);
        }

        // Level
        g2d.setColor(Color.WHITE);
        g2d.drawString("Level: " + currentLevel, WIDTH - 150, 40);
    }

    private void drawGameState(Graphics2D g2d) {
        if (gameState != GameState.PLAYING) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.setColor(Color.WHITE);

            switch (gameState) {
                case START:
                    g2d.drawString("EXTREME SNOWBOARDING", WIDTH/2 - 250, HEIGHT/2 - 100);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 24));
                    g2d.drawString("Press SPACE to Start", WIDTH/2 - 100, HEIGHT/2);
                    g2d.drawString("Controls: ← → Arrow Keys to Move", WIDTH/2 - 150, HEIGHT/2 + 50);
                    g2d.drawString("↑ to Jump, ↓ to Crouch, A/D for Tricks", WIDTH/2 - 150, HEIGHT/2 + 100);
                    break;

                case PAUSED:
                    g2d.drawString("PAUSED", WIDTH/2 - 100, HEIGHT/2);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 24));
                    g2d.drawString("Press P to Resume", WIDTH/2 - 100, HEIGHT/2 + 50);
                    break;

                case GAME_OVER:
                    g2d.drawString("GAME OVER", WIDTH/2 - 150, HEIGHT/2 - 50);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 24));
                    g2d.drawString("Final Score: " + score, WIDTH/2 - 100, HEIGHT/2);
                    g2d.drawString("Press R to Restart", WIDTH/2 - 100, HEIGHT/2 + 50);
                    break;

                case LEVEL_COMPLETE:
                    g2d.drawString("LEVEL COMPLETE!", WIDTH/2 - 200, HEIGHT/2 - 50);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 24));
                    g2d.drawString("Score: " + score, WIDTH/2 - 80, HEIGHT/2);
                    g2d.drawString("Press N for Next Level", WIDTH/2 - 120, HEIGHT/2 + 50);
                    break;
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (gameState) {
            case START:
                if (key == KeyEvent.VK_SPACE) {
                    gameState = GameState.PLAYING;
                }
                break;

            case PLAYING:
                switch (key) {
                    case KeyEvent.VK_LEFT:
                        leftPressed = true;
                        break;
                    case KeyEvent.VK_RIGHT:
                        rightPressed = true;
                        break;
                    case KeyEvent.VK_UP:
                        upPressed = true;
                        break;
                    case KeyEvent.VK_DOWN:
                        downPressed = true;
                        break;
                    case KeyEvent.VK_A:
                    case KeyEvent.VK_D:
                        trickPressed = true;
                        break;
                    case KeyEvent.VK_P:
                        gameState = GameState.PAUSED;
                        break;
                }
                break;

            case PAUSED:
                if (key == KeyEvent.VK_P) {
                    gameState = GameState.PLAYING;
                }
                break;

            case GAME_OVER:
                if (key == KeyEvent.VK_R) {
                    resetGame();
                    gameState = GameState.PLAYING;
                }
                break;

            case LEVEL_COMPLETE:
                if (key == KeyEvent.VK_N) {
                    nextLevel();
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_LEFT:
                leftPressed = false;
                break;
            case KeyEvent.VK_RIGHT:
                rightPressed = false;
                break;
            case KeyEvent.VK_UP:
                upPressed = false;
                break;
            case KeyEvent.VK_DOWN:
                downPressed = false;
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_D:
                trickPressed = false;
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void resetGame() {
        score = 0;
        lives = 3;
        currentLevel = 1;
        coinsCollected = 0;
        levelTime = 120;
        initializeGame();
        playerX = 100;
        playerY = GROUND_LEVEL - 50;
        playerVelX = 5;
        playerVelY = 0;
    }

    private void nextLevel() {
        currentLevel++;
        score += 1000;
        levelTime = 120 + currentLevel * 30;
        coinsCollected = 0;
        generateTrack();
        generateObstacles();
        generateRamps();
        generateCoins();
        playerX = 100;
        playerY = getTrackHeight(playerX) - 20;
        gameState = GameState.PLAYING;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Extreme Snowboarding Game");
            ExtremeSnowboardingGame game = new ExtremeSnowboardingGame();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
            game.requestFocus();
        });
    }

    // Inner classes for game objects

    class Obstacle {
        double x, y;
        int type;

        Obstacle(double x, double y, int type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    class JumpRamp {
        double x, y;
        int width, height;

        JumpRamp(double x, double y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    class Coin {
        double x, y;

        Coin(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    class Particle {
        double x, y;
        double velX, velY;
        Color color;
        int life;

        Particle(double x, double y, double velX, double velY, Color color, int life) {
            this.x = x;
            this.y = y;
            this.velX = velX;
            this.velY = velY;
            this.color = color;
            this.life = life;
        }

        void update() {
            x += velX;
            y += velY;
            velY += 0.1;
            life--;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    life * 255 / 60));
            g2d.fillOval((int)x - 3, (int)y - 3, 6, 6);
        }
    }

    class Snowflake {
        double x, y;
        double speed;
        int size;

        Snowflake() {
            x = Math.random() * WIDTH;
            y = Math.random() * HEIGHT;
            speed = 1 + Math.random() * 3;
            size = 2 + (int)(Math.random() * 4);
        }

        void update() {
            y += speed;
            x += Math.sin(y * 0.01) * 0.5;

            // Reset if off screen
            if (y > HEIGHT) {
                y = 0;
                x = Math.random() * WIDTH;
            }
        }
    }
}