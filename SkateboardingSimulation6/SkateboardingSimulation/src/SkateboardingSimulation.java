import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class SkateboardingSimulation extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final int GROUND_HEIGHT = 600;
    private static final int FPS = 60;
    private static final double GRAVITY = 0.5;
    private static final double FRICTION = 0.97;

    // Skateboarder properties
    private double skaterX = 100;
    private double skaterY = GROUND_HEIGHT - 60;
    private double skaterVelocityX = 0;
    private double skaterVelocityY = 0;
    private boolean onGround = true;
    private boolean isCrouching = false;
    private boolean isGrinding = false;
    private boolean isFlipping = false;
    private int flipRotation = 0;
    private int score = 0;
    private int combo = 0;
    private int health = 100;

    // Skateboard properties
    private double boardX = skaterX;
    private double boardY = skaterY + 40;
    private double boardRotation = 0;

    // Game objects
    private List<Ramp> ramps;
    private List<Rail> rails;
    private List<Obstacle> obstacles;
    private List<Particle> particles;

    // Game state
    private boolean gameRunning = true;
    private boolean gameStarted = false;
    private int gameTime = 0;
    private String trickMessage = "";
    private int trickMessageTime = 0;

    // Input tracking
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean spacePressed = false;

    // Animation variables
    private int skaterFrame = 0;
    private int boardFrame = 0;
    private int animationCounter = 0;

    // Colors
    private Color skaterColor = new Color(200, 50, 50);
    private Color boardColor = new Color(30, 30, 30);
    private Color wheelColor = new Color(60, 60, 60);
    private Color rampColor = new Color(180, 120, 70);
    private Color railColor = new Color(100, 100, 120);

    public SkateboardingSimulation() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235));

        // Initialize game objects
        initializeObjects();

        // Set up game timer
        Timer timer = new Timer(1000 / FPS, this);
        timer.start();

        // Set up key listener
        setFocusable(true);
        addKeyListener(this);

        // Initialize particles
        particles = new ArrayList<>();
    }

    private void initializeObjects() {
        ramps = new ArrayList<>();
        rails = new ArrayList<>();
        obstacles = new ArrayList<>();

        // Create ramps
        ramps.add(new Ramp(300, GROUND_HEIGHT - 100, 150, 50, 30));
        ramps.add(new Ramp(500, GROUND_HEIGHT - 150, 200, 80, 45));
        ramps.add(new Ramp(750, GROUND_HEIGHT - 120, 120, 40, 25));
        ramps.add(new Ramp(850, GROUND_HEIGHT - 180, 180, 100, 50));

        // Create rails
        rails.add(new RailBuilder().setX1(400).setY(GROUND_HEIGHT - 80).setX2(500).setY(GROUND_HEIGHT - 80).createRail());
        rails.add(new RailBuilder().setX1(600).setY(GROUND_HEIGHT - 140).setX2(700).setY(GROUND_HEIGHT - 140).createRail());
        rails.add(new RailBuilder().setX1(200).setY(GROUND_HEIGHT - 60).setX2(280).setY(GROUND_HEIGHT - 60).createRail());

        // Create obstacles
        obstacles.add(new Obstacle(350, GROUND_HEIGHT - 30, 20, 30));
        obstacles.add(new Obstacle(650, GROUND_HEIGHT - 30, 20, 30));
        obstacles.add(new Obstacle(900, GROUND_HEIGHT - 30, 20, 30));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning || !gameStarted) return;

        gameTime++;
        animationCounter++;

        // Update skater animation frames
        if (animationCounter % 5 == 0) {
            skaterFrame = (skaterFrame + 1) % 4;
            boardFrame = (boardFrame + 1) % 4;
        }

        // Handle input
        handleInput();

        // Apply physics
        applyPhysics();

        // Check collisions
        checkCollisions();

        // Update particles
        updateParticles();

        // Update trick message timer
        if (trickMessageTime > 0) {
            trickMessageTime--;
        }

        // Check game over
        if (health <= 0) {
            gameRunning = false;
            trickMessage = "GAME OVER! Final Score: " + score;
            trickMessageTime = 300;
        }

        // Add random obstacles occasionally
        if (gameTime % 200 == 0 && obstacles.size() < 10) {
            obstacles.add(new Obstacle(WIDTH + 50, GROUND_HEIGHT - 30, 20, 30));
        }

        repaint();
    }

    private void handleInput() {
        // Horizontal movement
        if (leftPressed && !rightPressed) {
            skaterVelocityX = Math.max(skaterVelocityX - 0.2, -10);
        } else if (rightPressed && !leftPressed) {
            skaterVelocityX = Math.min(skaterVelocityX + 0.2, 10);
        } else {
            // Apply friction when no horizontal input
            skaterVelocityX *= FRICTION;
            if (Math.abs(skaterVelocityX) < 0.1) skaterVelocityX = 0;
        }

        // Jumping
        if (upPressed && onGround) {
            skaterVelocityY = -15;
            onGround = false;
            addParticles(skaterX + 30, skaterY + 60, 5, Color.WHITE);
        }

        // Crouching
        isCrouching = downPressed;

        // Flip trick
        if (spacePressed && !isFlipping && !onGround) {
            isFlipping = true;
            flipRotation = 0;
            performTrick("Kickflip", 50);
        }

        // Continue flip rotation
        if (isFlipping) {
            flipRotation += 20;
            if (flipRotation >= 360) {
                isFlipping = false;
                flipRotation = 0;
            }
        }
    }

    private void applyPhysics() {
        // Apply gravity if not on ground
        if (!onGround) {
            skaterVelocityY += GRAVITY;
        }

        // Update position based on velocity
        skaterX += skaterVelocityX;
        skaterY += skaterVelocityY;

        // Update board position relative to skater
        boardX = skaterX;
        boardY = skaterY + (isCrouching ? 35 : 40);

        // Update board rotation based on movement
        if (onGround) {
            boardRotation = skaterVelocityX * 2;
        } else {
            boardRotation += 5;
        }

        // Keep skater in bounds
        if (skaterX < 0) {
            skaterX = 0;
            skaterVelocityX = Math.abs(skaterVelocityX) * 0.5;
        } else if (skaterX > WIDTH - 60) {
            skaterX = WIDTH - 60;
            skaterVelocityX = -Math.abs(skaterVelocityX) * 0.5;
        }

        // Check if skater is on ground
        if (skaterY >= GROUND_HEIGHT - 60) {
            skaterY = GROUND_HEIGHT - 60;
            skaterVelocityY = 0;
            onGround = true;
            isGrinding = false;

            // Landed a trick
            if (combo > 0) {
                score += combo * 10;
                combo = 0;
            }
        } else {
            onGround = false;
        }

        // Move obstacles
        Iterator<Obstacle> obstacleIter = obstacles.iterator();
        while (obstacleIter.hasNext()) {
            Obstacle obs = obstacleIter.next();
            obs.x -= skaterVelocityX * 0.5;

            // Remove obstacles that are off screen
            if (obs.x < -50) {
                obstacleIter.remove();
            }
        }
    }

    private void checkCollisions() {
        // Check ramp collisions
        for (Ramp ramp : ramps) {
            if (skaterX + 30 > ramp.x && skaterX + 30 < ramp.x + ramp.width &&
                    skaterY + 60 > ramp.y && skaterY + 60 < ramp.y + ramp.height) {

                // Calculate ramp surface angle
                double rampAngle = Math.toRadians(ramp.angle);
                double normalX = Math.sin(rampAngle);
                double normalY = -Math.cos(rampAngle);

                // Project velocity onto ramp normal
                double dotProduct = skaterVelocityX * normalX + skaterVelocityY * normalY;

                // Bounce off ramp
                if (dotProduct < 0) {
                    skaterVelocityX -= 2 * dotProduct * normalX;
                    skaterVelocityY -= 2 * dotProduct * normalY;

                    // Add a little boost
                    skaterVelocityY -= 3;

                    // Add particles
                    addParticles(skaterX + 30, skaterY + 60, 8, Color.ORANGE);

                    // Score points
                    performTrick("Ramp Jump", 25);
                }

                onGround = true;
            }
        }

        // Check rail collisions
        for (Rail rail : rails) {
            if (!isGrinding && skaterY + 55 > rail.y - 5 && skaterY + 55 < rail.y + 5 &&
                    skaterX + 30 > rail.x1 && skaterX + 30 < rail.x2) {

                isGrinding = true;
                skaterY = rail.y - 55;
                skaterVelocityY = 0;

                // Add particles
                addParticles(skaterX + 30, skaterY + 60, 3, Color.YELLOW);

                // Score points
                performTrick("Rail Grind", 10);
            }
        }

        // Check obstacle collisions
        Iterator<Obstacle> obstacleIter = obstacles.iterator();
        while (obstacleIter.hasNext()) {
            Obstacle obs = obstacleIter.next();
            if (skaterX + 30 > obs.x && skaterX < obs.x + obs.width &&
                    skaterY + 60 > obs.y && skaterY < obs.y + obs.height) {

                // Crash!
                health -= 20;
                combo = 0;

                // Bounce back
                skaterVelocityX = -skaterVelocityX * 0.5;
                skaterVelocityY = -5;

                // Add crash particles
                addParticles(skaterX + 30, skaterY + 60, 15, Color.RED);

                trickMessage = "CRASH! -20 Health";
                trickMessageTime = 60;

                // Remove the obstacle
                obstacleIter.remove();

                // Play crash sound (in a real game)
            }
        }
    }

    private void performTrick(String trickName, int points) {
        score += points;
        combo++;

        String message = trickName + " +" + points + " pts";
        if (combo > 1) {
            message += " COMBO x" + combo + "!";
        }

        trickMessage = message;
        trickMessageTime = 90;

        // Add particles for trick
        addParticles(skaterX + 30, skaterY + 30, 5, Color.CYAN);
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

    private void addParticles(double x, double y, int count, Color color) {
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double speed = rand.nextDouble() * 3 + 1;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            int life = rand.nextInt(30) + 20;
            particles.add(new Particle(x, y, vx, vy, life, color));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw sky gradient
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235), 0, HEIGHT/2, new Color(100, 180, 255));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw clouds
        drawClouds(g2d);

        // Draw ground
        g2d.setColor(new Color(100, 180, 100));
        g2d.fillRect(0, GROUND_HEIGHT, WIDTH, HEIGHT - GROUND_HEIGHT);

        // Draw road
        g2d.setColor(new Color(80, 80, 80));
        g2d.fillRect(0, GROUND_HEIGHT - 10, WIDTH, 10);

        // Draw road lines
        g2d.setColor(Color.YELLOW);
        for (int i = 0; i < WIDTH; i += 40) {
            g2d.fillRect(i, GROUND_HEIGHT - 5, 20, 3);
        }

        // Draw ramps
        for (Ramp ramp : ramps) {
            drawRamp(g2d, ramp);
        }

        // Draw rails
        for (Rail rail : rails) {
            drawRail(g2d, rail);
        }

        // Draw obstacles
        for (Obstacle obstacle : obstacles) {
            drawObstacle(g2d, obstacle);
        }

        // Draw particles
        for (Particle particle : particles) {
            drawParticle(g2d, particle);
        }

        // Draw skateboarder
        drawSkateboarder(g2d);

        // Draw skateboard
        drawSkateboard(g2d);

        // Draw UI
        drawUI(g2d);

        // Draw start screen if game hasn't started
        if (!gameStarted) {
            drawStartScreen(g2d);
        }

        // Draw game over screen if game ended
        if (!gameRunning && gameStarted) {
            drawGameOverScreen(g2d);
        }
    }

    private void drawClouds(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);

        // Draw several clouds at different positions
        int[] cloudX = {100, 300, 600, 800, 900};
        int[] cloudY = {100, 150, 80, 120, 180};

        for (int i = 0; i < cloudX.length; i++) {
            int x = cloudX[i] + (gameTime / 2) % WIDTH;
            if (x > WIDTH) x -= WIDTH;

            g2d.fillOval(x, cloudY[i], 40, 30);
            g2d.fillOval(x + 20, cloudY[i] - 10, 40, 30);
            g2d.fillOval(x + 20, cloudY[i] + 10, 40, 30);
            g2d.fillOval(x + 40, cloudY[i], 40, 30);
        }
    }

    private void drawRamp(Graphics2D g2d, Ramp ramp) {
        g2d.setColor(rampColor);

        // Draw ramp as a polygon
        int[] xPoints = {
                (int)ramp.x,
                (int)(ramp.x + ramp.width),
                (int)(ramp.x + ramp.width),
                (int)ramp.x
        };

        int[] yPoints = {
                (int)ramp.y + ramp.height,
                (int)ramp.y,
                (int)ramp.y + ramp.height,
                (int)ramp.y + ramp.height
        };

        g2d.fillPolygon(xPoints, yPoints, 4);

        // Draw ramp surface
        g2d.setColor(new Color(160, 100, 50));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine((int)ramp.x, (int)ramp.y + ramp.height,
                (int)(ramp.x + ramp.width), (int)ramp.y);
    }

    private void drawRail(Graphics2D g2d, Rail rail) {
        g2d.setColor(railColor);
        g2d.setStroke(new BasicStroke(8));
        g2d.drawLine((int)rail.x1, (int)rail.y, (int)rail.x2, (int)rail.y);

        // Draw rail posts
        g2d.setStroke(new BasicStroke(4));
        g2d.setColor(new Color(80, 80, 80));
        g2d.drawLine((int)rail.x1, (int)rail.y, (int)rail.x1, (int)rail.y + 30);
        g2d.drawLine((int)rail.x2, (int)rail.y, (int)rail.x2, (int)rail.y + 30);
    }

    private void drawObstacle(Graphics2D g2d, Obstacle obstacle) {
        g2d.setColor(new Color(150, 100, 50));
        g2d.fillRect((int)obstacle.x, (int)obstacle.y, obstacle.width, obstacle.height);

        g2d.setColor(new Color(120, 80, 40));
        g2d.fillRect((int)obstacle.x + 2, (int)obstacle.y + 2,
                obstacle.width - 4, obstacle.height - 4);

        // Draw warning stripes
        g2d.setColor(Color.YELLOW);
        for (int i = 0; i < obstacle.height; i += 8) {
            g2d.fillRect((int)obstacle.x, (int)obstacle.y + i, obstacle.width, 4);
        }
    }

    private void drawSkateboarder(Graphics2D g2d) {
        // Apply flip rotation if flipping
        if (isFlipping) {
            g2d.rotate(Math.toRadians(flipRotation),
                    skaterX + 30, skaterY + 30);
        }

        // Draw body
        g2d.setColor(skaterColor);
        g2d.fillOval((int)skaterX + 15, (int)skaterY + 10, 30, 40);

        // Draw head
        g2d.setColor(new Color(255, 220, 180));
        g2d.fillOval((int)skaterX + 20, (int)skaterY, 20, 20);

        // Draw helmet
        g2d.setColor(Color.BLACK);
        g2d.fillArc((int)skaterX + 18, (int)skaterY - 5, 24, 15, 0, 180);

        // Draw legs based on crouching state
        g2d.setColor(Color.BLUE);
        if (isCrouching) {
            // Crouching legs
            g2d.fillRect((int)skaterX + 20, (int)skaterY + 45, 8, 15);
            g2d.fillRect((int)skaterX + 32, (int)skaterY + 45, 8, 15);

            // Bent knees
            g2d.fillOval((int)skaterX + 18, (int)skaterY + 40, 12, 10);
            g2d.fillOval((int)skaterX + 30, (int)skaterY + 40, 12, 10);
        } else {
            // Standing legs
            g2d.fillRect((int)skaterX + 20, (int)skaterY + 45, 8, 20);
            g2d.fillRect((int)skaterX + 32, (int)skaterY + 45, 8, 20);
        }

        // Draw arms
        g2d.setColor(skaterColor);
        int armOffset = (animationCounter / 10) % 2 == 0 ? 5 : -5;
        g2d.fillRect((int)skaterX + 10, (int)skaterY + 20, 10, 20);
        g2d.fillRect((int)skaterX + 40, (int)skaterY + 20, 10, 20);

        // Reset rotation if flipping
        if (isFlipping) {
            g2d.rotate(-Math.toRadians(flipRotation),
                    skaterX + 30, skaterY + 30);
        }
    }

    private void drawSkateboard(Graphics2D g2d) {
        // Save original transform
        AffineTransform oldTransform = g2d.getTransform();

        // Apply board rotation
        g2d.rotate(Math.toRadians(boardRotation), boardX + 30, boardY);

        // Draw skateboard deck
        g2d.setColor(boardColor);
        g2d.fillRoundRect((int)boardX + 5, (int)boardY - 5, 50, 10, 5, 5);

        // Draw skateboard design
        g2d.setColor(Color.RED);
        int designSize = 8 + boardFrame;
        g2d.fillOval((int)boardX + 25 - designSize/2, (int)boardY - designSize/2,
                designSize, designSize);

        // Draw wheels
        g2d.setColor(wheelColor);
        g2d.fillOval((int)boardX + 10, (int)boardY - 3, 8, 8);
        g2d.fillOval((int)boardX + 42, (int)boardY - 3, 8, 8);

        // Draw wheel highlights
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillOval((int)boardX + 12, (int)boardY - 1, 4, 4);
        g2d.fillOval((int)boardX + 44, (int)boardY - 1, 4, 4);

        // Restore transform
        g2d.setTransform(oldTransform);
    }

    private void drawParticle(Graphics2D g2d, Particle particle) {
        g2d.setColor(particle.color);
        int size = particle.life / 10 + 1;
        g2d.fillOval((int)particle.x - size/2, (int)particle.y - size/2, size, size);
    }

    private void drawUI(Graphics2D g2d) {
        // Draw score
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("Score: " + score, 20, 40);

        // Draw combo
        if (combo > 1) {
            g2d.setColor(Color.YELLOW);
            g2d.drawString("Combo: x" + combo, 20, 70);
        }

        // Draw health bar
        g2d.setColor(Color.RED);
        g2d.fillRect(WIDTH - 220, 20, 200, 20);
        g2d.setColor(Color.GREEN);
        g2d.fillRect(WIDTH - 220, 20, (int)(health * 2), 20);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(WIDTH - 220, 20, 200, 20);
        g2d.drawString("Health: " + health, WIDTH - 215, 55);

        // Draw trick message
        if (trickMessageTime > 0) {
            g2d.setFont(new Font("Arial", Font.BOLD, 28));

            // Add outline to text
            g2d.setColor(Color.BLACK);
            g2d.drawString(trickMessage, WIDTH/2 - g2d.getFontMetrics().stringWidth(trickMessage)/2 + 2, 100 + 2);

            // Actual text
            g2d.setColor(Color.CYAN);
            g2d.drawString(trickMessage, WIDTH/2 - g2d.getFontMetrics().stringWidth(trickMessage)/2, 100);
        }

        // Draw controls reminder
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Controls: Arrow Keys = Move, Space = Flip Trick", 20, HEIGHT - 20);
    }

    private void drawStartScreen(Graphics2D g2d) {
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Title
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String title = "SKATEBOARD SIMULATOR";
        g2d.drawString(title, WIDTH/2 - g2d.getFontMetrics().stringWidth(title)/2, HEIGHT/2 - 50);

        // Instructions
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String startText = "Press ENTER to Start";
        g2d.drawString(startText, WIDTH/2 - g2d.getFontMetrics().stringWidth(startText)/2, HEIGHT/2 + 50);

        // Controls
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        String[] controls = {
                "Controls:",
                "Left/Right Arrow - Move",
                "Up Arrow - Jump",
                "Down Arrow - Crouch",
                "Space - Perform Flip Trick",
                "Avoid obstacles, grind rails, and perform tricks!"
        };

        for (int i = 0; i < controls.length; i++) {
            g2d.drawString(controls[i], WIDTH/2 - 200, HEIGHT/2 + 100 + i * 30);
        }
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Game Over text
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String gameOver = "GAME OVER";
        g2d.drawString(gameOver, WIDTH/2 - g2d.getFontMetrics().stringWidth(gameOver)/2, HEIGHT/2 - 50);

        // Final score
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        String scoreText = "Final Score: " + score;
        g2d.drawString(scoreText, WIDTH/2 - g2d.getFontMetrics().stringWidth(scoreText)/2, HEIGHT/2 + 20);

        // Restart instruction
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String restartText = "Press ENTER to Restart";
        g2d.drawString(restartText, WIDTH/2 - g2d.getFontMetrics().stringWidth(restartText)/2, HEIGHT/2 + 80);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (!gameStarted && keyCode == KeyEvent.VK_ENTER) {
            gameStarted = true;
            gameRunning = true;
            health = 100;
            score = 0;
            initializeObjects();
            return;
        }

        if (!gameRunning && keyCode == KeyEvent.VK_ENTER) {
            gameRunning = true;
            gameStarted = true;
            health = 100;
            score = 0;
            skaterX = 100;
            skaterY = GROUND_HEIGHT - 60;
            skaterVelocityX = 0;
            skaterVelocityY = 0;
            initializeObjects();
            return;
        }

        switch (keyCode) {
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
            case KeyEvent.VK_SPACE:
                spacePressed = true;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();

        switch (keyCode) {
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
            case KeyEvent.VK_SPACE:
                spacePressed = false;
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner classes for game objects
    class Ramp {
        double x, y;
        int width, height;
        int angle;

        Ramp(double x, double y, int width, int height, int angle) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.angle = angle;
        }
    }

    class Rail {
        double x1, y, x2;

        Rail(double x1, double y, double x2, double y) {
            this.x1 = x1;
            this.y = y;
            this.x2 = x2;
        }
    }

    class Obstacle {
        double x, y;
        int width, height;

        Obstacle(double x, double y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    class Particle {
        double x, y;
        double vx, vy;
        int life;
        Color color;

        Particle(double x, double y, double vx, double vy, int life, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.color = color;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.1; // Gravity for particles
            life--;

            // Fade color as particle ages
            int alpha = Math.max(0, life * 255 / 30);
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Skateboarding Simulation Game");
        SkateboardingSimulation game = new SkateboardingSimulation();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Request focus for key events
        game.requestFocusInWindow();
    }
}