
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

// Main game class
public class VRHorrorExperience extends JFrame {
    private GamePanel gamePanel;
    private Timer timer;
    private boolean isGameRunning;
    private int score;
    private int playerHealth;

    public VRHorrorExperience() {
        setTitle("VR Horror Experience");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize game variables
        isGameRunning = true;
        score = 0;
        playerHealth = 100;

        // Create game panel
        gamePanel = new GamePanel();
        add(gamePanel);

        // Set up timer for game loop
        timer = new Timer(16, e -> {
            if (isGameRunning) {
                gamePanel.update();
                gamePanel.repaint();
            }
        });

        // Add key listener
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                gamePanel.handleInput(e);
            }
        });

        setFocusable(true);
        timer.start();
    }

    // Game panel class
    class GamePanel extends JPanel {
        private Player player;
        private ArrayList<Enemy> enemies;
        private ArrayList<Collectible> collectibles;
        private ArrayList<Particle> particles;
        private Background background;
        private Random random;

        public GamePanel() {
            setBackground(Color.BLACK);
            player = new Player(400, 300);
            enemies = new ArrayList<>();
            collectibles = new ArrayList<>();
            particles = new ArrayList<>();
            background = new Background();
            random = new Random();

            // Initialize enemies
            for (int i = 0; i < 5; i++) {
                spawnEnemy();
            }
        }

        private void spawnEnemy() {
            int x = random.nextInt(800);
            int y = random.nextInt(600);
            enemies.add(new Enemy(x, y));
        }

        private void spawnCollectible() {
            int x = random.nextInt(800);
            int y = random.nextInt(600);
            collectibles.add(new Collectible(x, y));
        }

        public void handleInput(KeyEvent e) {
            player.handleInput(e);
        }

        public void update() {
            // Update player
            player.update();

            // Update enemies
            for (Enemy enemy : enemies) {
                enemy.update(player);
            }

            // Update collectibles
            for (Collectible collectible : collectibles) {
                collectible.update();
            }

            // Update particles
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.update();
                if (p.isDead()) {
                    particles.remove(i);
                }
            }

            // Check collisions
            checkCollisions();

            // Spawn new entities
            if (random.nextDouble() < 0.02) {
                spawnEnemy();
            }
            if (random.nextDouble() < 0.01) {
                spawnCollectible();
            }

            // Update background
            background.update();
        }

        private void checkCollisions() {
            // Player-enemy collisions
            Rectangle playerBounds = player.getBounds();
            for (int i = enemies.size() - 1; i >= 0; i--) {
                Enemy enemy = enemies.get(i);
                if (playerBounds.intersects(enemy.getBounds())) {
                    playerHealth -= 10;
                    enemies.remove(i);
                    createExplosion(enemy.x, enemy.y);
                    if (playerHealth <= 0) {
                        isGameRunning = false;
                    }
                }
            }

            // Player-collectible collisions
            for (int i = collectibles.size() - 1; i >= 0; i--) {
                Collectible collectible = collectibles.get(i);
                if (playerBounds.intersects(collectible.getBounds())) {
                    score += 100;
                    collectibles.remove(i);
                    createSparkle(collectible.x, collectible.y);
                }
            }
        }

        private void createExplosion(float x, float y) {
            for (int i = 0; i < 20; i++) {
                particles.add(new Particle(x, y, Color.RED));
            }
        }

        private void createSparkle(float x, float y) {
            for (int i = 0; i < 10; i++) {
                particles.add(new Particle(x, y, Color.YELLOW));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw background
            background.draw(g2d);

            // Draw player
            player.draw(g2d);

            // Draw enemies
            for (Enemy enemy : enemies) {
                enemy.draw(g2d);
            }

            // Draw collectibles
            for (Collectible collectible : collectibles) {
                collectible.draw(g2d);
            }

            // Draw particles
            for (Particle particle : particles) {
                particle.draw(g2d);
            }

            // Draw HUD
            drawHUD(g2d);
        }

        private void drawHUD(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Score: " + score, 10, 30);
            g2d.drawString("Health: " + playerHealth, 10, 60);

            if (!isGameRunning) {
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 40));
                g2d.drawString("GAME OVER", 300, 300);
            }
        }
    }

    // Player class
    class Player {
        private float x, y;
        private float vx, vy;
        private float speed;
        private int size;

        public Player(float x, float y) {
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = 0;
            this.speed = 5;
            this.size = 30;
        }

        public void handleInput(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    vx = -speed;
                    break;
                case KeyEvent.VK_RIGHT:
                    vx = speed;
                    break;
                case KeyEvent.VK_UP:
                    vy = -speed;
                    break;
                case KeyEvent.VK_DOWN:
                    vy = speed;
                    break;
            }
        }

        public void update() {
            x += vx;
            y += vy;

            // Apply friction
            vx *= 0.9;
            vy *= 0.9;

            // Keep player in bounds
            x = Math.max(0, Math.min(x, 800 - size));
            y = Math.max(0, Math.min(y, 600 - size));
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.BLUE);
            g2d.fillOval((int)x, (int)y, size, size);
        }

        public Rectangle getBounds() {
            return new Rectangle((int)x, (int)y, size, size);
        }
    }

    // Enemy class
    class Enemy {
        private float x, y;
        private float speed;
        private int size;
        private double angle;

        public Enemy(float x, float y) {
            this.x = x;
            this.y = y;
            this.speed = 2;
            this.size = 40;
            this.angle = 0;
        }

        public void update(Player player) {
            // Move towards player
            double dx = player.x - x;
            double dy = player.y - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > 0) {
                dx /= distance;
                dy /= distance;
                x += dx * speed;
                y += dy * speed;
            }

            // Update animation
            angle += 0.1;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.RED);
            g2d.translate(x + size/2, y + size/2);
            g2d.rotate(angle);
            g2d.fillRect(-size/2, -size/2, size, size);
            g2d.rotate(-angle);
            g2d.translate(-(x + size/2), -(y + size/2));
        }

        public Rectangle getBounds() {
            return new Rectangle((int)x, (int)y, size, size);
        }
    }

    // Collectible class
    class Collectible {
        private float x, y;
        private int size;
        private double angle;

        public Collectible(float x, float y) {
            this.x = x;
            this.y = y;
            this.size = 20;
            this.angle = 0;
        }

        public void update() {
            angle += 0.05;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.YELLOW);
            g2d.translate(x + size/2, y + size/2);
            g2d.rotate(angle);
            g2d.fillOval(-size/2, -size/2, size, size);
            g2d.rotate(-angle);
            g2d.translate(-(x + size/2), -(y + size/2));
        }

        public Rectangle getBounds() {
            return new Rectangle((int)x, (int)y, size, size);
        }
    }

    // Particle class
    class Particle {
        private float x, y;
        private float vx, vy;
        private float size;
        private Color color;
        private float life;
        private float maxLife;

        public Particle(float x, float y, Color color) {
            this.x = x;
            this.y = y;
            this.vx = (float)(Math.random() * 6 - 3);
            this.vy = (float)(Math.random() * 6 - 3);
            this.size = (float)(Math.random() * 5 + 5);
            this.color = color;
            this.life = 1.0f;
            this.maxLife = life;
        }

        public void update() {
            x += vx;
            y += vy;
            life -= 0.02;
            size *= 0.95;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(new Color(color.getRed()/255f, color.getGreen()/255f,
                    color.getBlue()/255f, life));
            g2d.fillOval((int)(x - size/2), (int)(y - size/2),
                    (int)size, (int)size);
        }

        public boolean isDead() {
            return life <= 0;
        }
    }

    // Background class
    class Background {
        private float offset;
        private Color fogColor;

        public Background() {
            offset = 0;
            fogColor = new Color(50, 50, 50, 100);
        }

        public void update() {
            offset += 0.5;
            if (offset > 800) {
                offset -= 800;
            }
        }

        public void draw(Graphics2D g2d) {
            // Draw background pattern
            g2d.setColor(new Color(20, 20, 20));
            g2d.fillRect(0, 0, 800, 600);

            // Draw moving grid
            g2d.setColor(new Color(0, 50, 0, 50));
            for (int i = -800; i < 1600; i += 50) {
                g2d.drawLine((int)(i + offset), 0, (int)(i + offset), 600);
                g2d.drawLine(0, (int)(i + offset), 800, (int)(i + offset));
            }

            // Draw fog effect
            g2d.setColor(fogColor);
            g2d.fillRect(0, 0, 800, 600);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VRHorrorExperience().setVisible(true);
        });
    }
}