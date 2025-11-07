import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class BMXStuntRacingGame extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private int screenWidth = 1200;
    private int screenHeight = 600;
    private int groundY = 450;
    private int gravity = 1;
    private int jumpPower = -20;
    private int maxSpeed = 15;
    private int boostPower = 25;

    // Player (BMX Rider)
    private class BMXRider {
        double x = 100;
        double y = groundY - 50;
        double velX = 0;
        double velY = 0;
        double rotation = 0; // Bike rotation for flips
        double lean = 0; // Lean angle for stunts
        boolean isJumping = false;
        boolean isFlipping = false;
        int flipDirection = 0; // 1: front, -1: back
        int score = 0;
        int tricksPerformed = 0;
        int health = 100;
        BufferedImage sprite;
        ArrayList<BufferedImage> animationFrames = new ArrayList<>();
        int currentFrame = 0;
        int animationTimer = 0;
    }

    private BMXRider rider;
    private ArrayList<Platform> platforms;
    private ArrayList<Obstacle> obstacles;
    private ArrayList<PowerUp> powerUps;
    private ArrayList<Enemy> enemies;
    private ArrayList<Particle> particles;

    // Background layers for parallax
    private BufferedImage backgroundFar;
    private BufferedImage backgroundMid;
    private BufferedImage backgroundNear;
    private int bgScrollX = 0;

    // Game states
    private enum GameState { MENU, PLAYING, PAUSED, GAMEOVER }
    private GameState state = GameState.MENU;

    // Levels
    private int currentLevel = 1;
    private int levelProgress = 0;
    private int finishLine = 10000; // Pixels to finish

    // Input
    private boolean[] keys = new boolean[KeyEvent.KEY_LAST + 1];

    // Scoring
    private int highScore = 0;
    private long startTime;
    private long elapsedTime;

    // Fonts and colors
    private Font gameFont = new Font("Arial", Font.BOLD, 24);
    private Font titleFont = new Font("Arial", Font.BOLD, 48);

    public BMXStuntRacingGame() {
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setBackground(Color.CYAN);
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(16, this); // ~60 FPS

        initGame();
        loadAssets();
        timer.start();
    }

    private void initGame() {
        rider = new BMXRider();
        platforms = new ArrayList<>();
        obstacles = new ArrayList<>();
        powerUps = new ArrayList<>();
        enemies = new ArrayList<>();
        particles = new ArrayList<>();

        generateLevel(currentLevel);
        startTime = System.currentTimeMillis();
        state = GameState.MENU;
    }

    private void loadAssets() {
        // Simulate loading sprites (in real game, load from files)
        rider.sprite = createBikeSprite(Color.RED);
        for (int i = 0; i < 8; i++) {
            rider.animationFrames.add(createAnimatedFrame(i));
        }

        backgroundFar = createGradientBackground(screenWidth * 3, screenHeight, Color.BLUE, Color.CYAN);
        backgroundMid = createHillsBackground(screenWidth * 3, screenHeight);
        backgroundNear = createGrassBackground(screenWidth * 3, screenHeight);
    }

    private BufferedImage createBikeSprite(Color color) {
        BufferedImage img = new BufferedImage(80, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        // Frame
        g.draw(new Ellipse2D.Double(10, 20, 30, 30)); // Front wheel
        g.draw(new Ellipse2D.Double(50, 20, 30, 30)); // Back wheel
        g.drawLine(25, 35, 65, 35); // Bottom bar
        g.drawLine(25, 35, 15, 15); // Front fork
        g.drawLine(65, 35, 75, 15); // Back fork
        g.drawLine(15, 15, 45, 15); // Handlebar to seat
        g.drawLine(45, 15, 75, 15); // Seat
        // Rider
        g.setColor(Color.ORANGE);
        g.fillOval(40, 5, 20, 20); // Head
        g.drawLine(50, 15, 50, 30); // Body
        g.drawLine(50, 30, 40, 45); // Leg1
        g.drawLine(50, 30, 60, 45); // Leg2
        g.drawLine(50, 20, 35, 25); // Arm1
        g.drawLine(50, 20, 65, 25); // Arm2
        g.dispose();
        return img;
    }

    private BufferedImage createAnimatedFrame(int frame) {
        // Simple animation variation
        BufferedImage img = new BufferedImage(80, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(rider.sprite, 0, (frame % 2 == 0 ? -5 : 5), null);
        g.dispose();
        return img;
    }

    private BufferedImage createGradientBackground(int w, int h, Color c1, Color c2) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        GradientPaint gp = new GradientPaint(0, 0, c1, 0, h, c2);
        g.setPaint(gp);
        g.fillRect(0, 0, w, h);
        return img;
    }

    private BufferedImage createHillsBackground(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0, 100, 0, 150));
        Random r = new Random();
        for (int i = 0; i < 20; i++) {
            int hx = r.nextInt(w);
            int hy = 300 + r.nextInt(150);
            int hr = 200 + r.nextInt(300);
            g.fillOval(hx - hr/2, hy - hr/2, hr, hr);
        }
        g.dispose();
        return img;
    }

    private BufferedImage createGrassBackground(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GREEN.darker());
        for (int i = 0; i < w; i += 20) {
            g.drawLine(i, groundY + 50, i + 10, groundY + 70);
        }
        g.dispose();
        return img;
    }

    private void generateLevel(int level) {
        platforms.clear();
        obstacles.clear();
        powerUps.clear();
        enemies.clear();

        // Ground platform
        platforms.add(new Platform(0, groundY + 50, finishLine, 200, Color.GREEN.darker()));

        Random rand = new Random(level);
        int pos = 500;
        while (pos < finishLine - 1000) {
            if (rand.nextBoolean()) {
                // Ramp for jumps
                platforms.add(new Ramp(pos, groundY + 50, 200, 100, rand.nextBoolean()));
            } else if (rand.nextInt(5) == 0) {
                // Loop-de-loop
                platforms.add(new Loop(pos, groundY + 50, 300));
            }

            // Obstacles
            if (rand.nextInt(3) == 0) {
                obstacles.add(new Obstacle(pos + 200, groundY - 50, 50, 100, Obstacle.Type.BARREL));
            } else if (rand.nextInt(4) == 0) {
                obstacles.add(new Obstacle(pos + 200, groundY - 100, 80, 80, Obstacle.Type.BIRD));
            }

            // Power-ups
            if (rand.nextInt(10) == 0) {
                powerUps.add(new PowerUp(pos + 300, groundY - 150, PowerUp.Type.BOOST));
            } else if (rand.nextInt(15) == 0) {
                powerUps.add(new PowerUp(pos + 300, groundY - 150, PowerUp.Type.HEALTH));
            }

            // Enemies (rival riders)
            if (level > 1 && rand.nextInt(8) == 0) {
                enemies.add(new Enemy(pos + 500, groundY - 50));
            }

            pos += 800 + rand.nextInt(600);
        }

        // Finish line
        platforms.add(new Platform(finishLine - 100, groundY + 50, 200, 200, Color.YELLOW));
    }

    private class Platform {
        int x, y, width, height;
        Color color;
        Platform(int x, int y, int w, int h, Color c) {
            this.x = x; this.y = y; this.width = w; this.height = h; this.color = c;
        }
    }

    private class Ramp extends Platform {
        boolean up;
        Ramp(int x, int y, int w, int h, boolean up) {
            super(x, y - h, w, h, Color.green);  // Fixed: BROWN → brown
            this.up = up;
        }
    }

    private class Loop extends Platform {
        Loop(int x, int y, int size) {
            super(x, y - size, size, size * 2, Color.GRAY);
        }
    }

    private class Obstacle {
        int x, y, width, height;
        Type type;
        enum Type { BARREL, BIRD, CAR }
        Obstacle(int x, int y, int w, int h, Type t) {
            this.x = x; this.y = y; this.width = w; this.height = h; this.type = t;
        }
    }

    private class PowerUp {
        int x, y;
        Type type;
        enum Type { BOOST, HEALTH, SCORE_MULTIPLIER }
        PowerUp(int x, int y, Type t) {
            this.x = x; this.y = y; this.type = t;
        }
    }

    private class Enemy {
        double x, y = groundY - 50;
        double speed = 5 + currentLevel;
        Enemy(int x, double y) {
            this.x = x; this.y = y;
        }
    }

    private class Particle {
        double x, y, velX, velY;
        Color color;
        int life;
        Particle(double x, double y, double vx, double vy, Color c, int l) {
            this.x = x; this.y = y; this.velX = vx; this.velY = vy; this.color = c; this.life = l;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameState.PLAYING) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        elapsedTime = System.currentTimeMillis() - startTime;

        // Update background scroll
        bgScrollX -= rider.velX * 0.5;
        if (bgScrollX <= -screenWidth * 2) bgScrollX = 0;

        // Input handling
        if (keys[KeyEvent.VK_UP]) rider.velX = Math.min(rider.velX + 0.5, maxSpeed);
        if (keys[KeyEvent.VK_DOWN]) rider.velX = Math.max(rider.velX - 1, 0);
        if (keys[KeyEvent.VK_LEFT] && rider.isJumping) {
            rider.lean = -0.5;
            if (rider.isFlipping) rider.flipDirection = -1;
        }
        if (keys[KeyEvent.VK_RIGHT] && rider.isJumping) {
            rider.lean = 0.5;
            if (rider.isFlipping) rider.flipDirection = 1;
        }
        if (keys[KeyEvent.VK_SPACE] && rider.velX > 10) {
            rider.velX += boostPower / 10.0;
            createParticles(rider.x, rider.y + 50, Color.YELLOW);
            keys[KeyEvent.VK_SPACE] = false; // One-time boost
        }

        // Physics
        rider.velY += gravity;
        rider.y += rider.velY;
        rider.x += rider.velX;

        // Ground collision
        if (rider.y >= groundY - 50) {
            rider.y = groundY - 50;
            rider.velY = 0;
            rider.isJumping = false;
            rider.rotation = 0;
            if (rider.velX > 5) createParticles(rider.x, rider.y + 50, Color.GRAY);
        }

        // Platform/Ramp collisions
        for (Platform p : platforms) {
            if (p instanceof Ramp) {
                Ramp r = (Ramp) p;
                if (rider.x > r.x && rider.x < r.x + r.width && rider.y + 50 >= r.y) {
                    rider.y = r.y - 50;
                    rider.velY = r.up ? -jumpPower * 1.5 : 0;
                    rider.isJumping = true;
                }
            } else if (rider.x > p.x && rider.x < p.x + p.width && rider.y + 50 >= p.y) {
                rider.y = p.y - 50;
                rider.velY = 0;
                rider.isJumping = false;
            }
        }

        // Flips and stunts
        if (rider.isJumping) {
            rider.rotation += rider.lean * 5;
            if (Math.abs(rider.rotation) >= 360) {
                rider.score += 500 * currentLevel;
                rider.tricksPerformed++;
                rider.rotation = 0;
                createParticles(rider.x, rider.y, Color.MAGENTA);
            }
        }

        // Obstacle collision
        for (Obstacle o : obstacles) {
            if (Math.abs(rider.x - o.x) < 50 && Math.abs(rider.y - o.y) < 50) {
                rider.health -= 20;
                rider.velX *= 0.5;
                createParticles(o.x, o.y, Color.RED);
                obstacles.remove(o);
                break;
            }
        }

        // Power-up collection
        for (PowerUp pu : powerUps) {
            if (Math.abs(rider.x - pu.x) < 50 && Math.abs(rider.y - pu.y) < 50) {
                if (pu.type == PowerUp.Type.BOOST) rider.velX += boostPower;
                else if (pu.type == PowerUp.Type.HEALTH) rider.health = Math.min(100, rider.health + 50);
                else rider.score *= 2;
                createParticles(pu.x, pu.y, Color.YELLOW);
                powerUps.remove(pu);
                break;
            }
        }

        // Enemy AI and collision
        for (Enemy en : enemies) {
            en.x -= en.speed + rider.velX * 0.5;
            if (Math.abs(rider.x - en.x) < 60 && Math.abs(rider.y - en.y) < 60) {
                rider.score += 200;
                createParticles(en.x, en.y, Color.ORANGE);
                enemies.remove(en);
            }
        }

        // Particles update
        particles.removeIf(p -> p.life-- <= 0);
        for (Particle p : particles) {
            p.x += p.velX;
            p.y += p.velY;
            p.velY += 0.5;
        }

        // Level progress
        levelProgress = (int) rider.x;
        if (rider.x >= finishLine) {
            rider.score += (int) (10000 / (elapsedTime / 1000 + 1)) * currentLevel;
            if (rider.score > highScore) highScore = rider.score;
            currentLevel++;
            generateLevel(currentLevel);
            rider.x = 100;
            rider.velX = 0;
        }

        // Game over
        if (rider.health <= 0 || rider.y > screenHeight + 100) {
            state = GameState.GAMEOVER;
            if (rider.score > highScore) highScore = rider.score;
        }

        // Animation
        rider.animationTimer++;
        if (rider.animationTimer > 5) {
            rider.currentFrame = (rider.currentFrame + 1) % rider.animationFrames.size();
            rider.animationTimer = 0;
        }

        // Auto scroll camera if needed
        if (rider.x > screenWidth / 2) {
            // Camera follows (but in this side-scroller, we move everything left)
            // For simplicity, we scroll backgrounds
        }
    }

    private void createParticles(double x, double y, Color c) {
        Random r = new Random();
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(x, y, r.nextDouble() * 10 - 5, r.nextDouble() * -10, c, 30 + r.nextInt(20)));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Parallax background
        g2d.drawImage(backgroundFar, bgScrollX / 3, 0, null);
        g2d.drawImage(backgroundFar, bgScrollX / 3 + backgroundFar.getWidth(), 0, null);
        g2d.drawImage(backgroundMid, bgScrollX / 2, 0, null);
        g2d.drawImage(backgroundMid, bgScrollX / 2 + backgroundMid.getWidth(), 0, null);
        g2d.drawImage(backgroundNear, bgScrollX, 0, null);
        g2d.drawImage(backgroundNear, bgScrollX + backgroundNear.getWidth(), 0, null);

        // Translate for camera (rider centered)
        int camX = (int) (rider.x - screenWidth / 2);
        g2d.translate(-camX, 0);

        // Draw platforms
        for (Platform p : platforms) {
            g2d.setColor(p.color);
            if (p instanceof Ramp) {
                Ramp r = (Ramp) p;
                Polygon poly = new Polygon();
                if (r.up) {
                    poly.addPoint(r.x, r.y + r.height);
                    poly.addPoint(r.x + r.width, r.y + r.height);
                    poly.addPoint(r.x + r.width, r.y);
                } else {
                    poly.addPoint(r.x, r.y + r.height);
                    poly.addPoint(r.x + r.width, r.y);
                    poly.addPoint(r.x, r.y);
                }
                g2d.fill(poly);
            } else if (p instanceof Loop) {
                g2d.setColor(Color.GRAY);
                g2d.drawOval(p.x, p.y, p.width, p.height * 2);
            } else {
                g2d.fillRect(p.x, p.y, p.width, p.height);
            }
        }

        // Draw obstacles
        for (Obstacle o : obstacles) {
            g2d.setColor(o.type == Obstacle.Type.BARREL ? Color.ORANGE : Color.BLUE);
            g2d.fillRect(o.x, o.y, o.width, o.height);
        }

        // Draw power-ups
        for (PowerUp pu : powerUps) {
            g2d.setColor(pu.type == PowerUp.Type.BOOST ? Color.YELLOW : Color.RED);
            g2d.fillOval(pu.x - 20, pu.y - 20, 40, 40);
        }

        // Draw enemies
        for (Enemy en : enemies) {
            g2d.drawImage(createBikeSprite(Color.BLUE), (int)en.x - 40, (int)en.y - 30, null);
        }

        // Draw rider with rotation and animation
        g2d.translate(rider.x, rider.y + 30);
        g2d.rotate(Math.toRadians(rider.rotation));
        g2d.drawImage(rider.animationFrames.get(rider.currentFrame), -40, -30, null);
        g2d.rotate(-Math.toRadians(rider.rotation));
        g2d.translate(-rider.x, -rider.y - 30);

        // Draw particles
        for (Particle p : particles) {
            g2d.setColor(p.color);
            g2d.fillOval((int)p.x - 5, (int)p.y - 5, 10, 10);
        }

        g2d.translate(camX, 0);

        // HUD
        g2d.setFont(gameFont);
        g2d.setColor(Color.WHITE);
        g2d.drawString("Score: " + rider.score, 20, 50);
        g2d.drawString("Health: " + rider.health, 20, 80);
        g2d.drawString("Level: " + currentLevel, 20, 110);
        g2d.drawString("Speed: " + (int)rider.velX, 20, 140);
        g2d.drawString("Time: " + (elapsedTime / 1000) + "s", 20, 170);
        g2d.drawString("Progress: " + (levelProgress / 100) + "m / " + (finishLine / 100) + "m", 20, 200);

        // Menu / Pause / GameOver
        if (state == GameState.MENU) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, screenWidth, screenHeight);
            g2d.setColor(Color.WHITE);
            g2d.setFont(titleFont);
            drawCenteredString(g2d, "BMX STUNT RACING", screenWidth / 2, 200);
            g2d.setFont(gameFont);
            drawCenteredString(g2d, "Press ENTER to Start", screenWidth / 2, 300);
            drawCenteredString(g2d, "High Score: " + highScore, screenWidth / 2, 350);
        } else if (state == GameState.PAUSED) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, screenWidth, screenHeight);
            g2d.setColor(Color.WHITE);
            g2d.setFont(titleFont);
            drawCenteredString(g2d, "PAUSED", screenWidth / 2, 250);
            drawCenteredString(g2d, "Press P to Resume", screenWidth / 2, 320);
        } else if (state == GameState.GAMEOVER) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, screenWidth, screenHeight);
            g2d.setColor(Color.RED);
            g2d.setFont(titleFont);
            drawCenteredString(g2d, "GAME OVER", screenWidth / 2, 200);
            g2d.setColor(Color.WHITE);
            g2d.setFont(gameFont);
            drawCenteredString(g2d, "Final Score: " + rider.score, screenWidth / 2, 300);
            drawCenteredString(g2d, "Press R to Restart", screenWidth / 2, 350);
        }
    }

    private void drawCenteredString(Graphics2D g, String s, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, x - fm.stringWidth(s)/2, y);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;

        if (e.getKeyCode() == KeyEvent.VK_ENTER && state == GameState.MENU) {
            state = GameState.PLAYING;
            initGame();
        }
        if (e.getKeyCode() == KeyEvent.VK_P && state == GameState.PLAYING) {
            state = GameState.PAUSED;
        } else if (e.getKeyCode() == KeyEvent.VK_P && state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
        if (e.getKeyCode() == KeyEvent.VK_R && state == GameState.GAMEOVER) {
            state = GameState.MENU;
        }
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
    }

    @Override public void keyReleased(KeyEvent e) { keys[e.getKeyCode()] = false; }
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("BMX Stunt Racing Game");
        BMXStuntRacingGame game = new BMXStuntRacingGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}