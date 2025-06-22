import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MixedMartialArtsSimulator5 extends JFrame implements ActionListener, KeyListener {
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int OCTAGON_SIZE = 400;
    private static final int FPS = 60;

    private Timer gameTimer;
    private GamePanel gamePanel;
    private Fighter fighter1, fighter2;
    private GameState gameState;
    private SoundManager soundManager;
    private ParticleSystem particleSystem;
    private Camera camera;
    private InputHandler inputHandler;
    private HUD hud;
    private Random random;

    // Game states
    private enum GameState {
        MENU, FIGHTER_SELECT, FIGHTING, PAUSED, ROUND_END, GAME_OVER
    }

    public MixedMartialArtsSimulator5() {
        initializeGame();
        setupUI();
        startGameLoop();
    }

    private void initializeGame() {
        gameState = GameState.MENU;
        random = new Random();
        particleSystem = new ParticleSystem();
        camera = new Camera();
        inputHandler = new InputHandler();
        hud = new HUD();
        soundManager = new SoundManager();

        // Initialize fighters
        fighter1 = new Fighter("Connor", 200, 300, Color.RED, true);
        fighter2 = new Fighter("Anderson", 600, 300, Color.BLUE, false);

        gamePanel = new GamePanel();
    }

    private void setupUI() {
        setTitle("Mixed Martial Arts Simulator");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        add(gamePanel);
        addKeyListener(this);
        setFocusable(true);

        gameTimer = new Timer(1000 / FPS, this);
    }

    private void startGameLoop() {
        gameTimer.start();
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.FIGHTING) {
            updateGame();
        }
        gamePanel.repaint();
    }

    private void updateGame() {
        fighter1.update();
        fighter2.update();

        // AI for fighter 2
        updateAI();

        // Check collisions
        checkCollisions();

        // Update particles
        particleSystem.update();

        // Check win conditions
        checkWinConditions();

        // Update camera
        camera.update(fighter1, fighter2);
    }

    private void updateAI() {
        double distance = Math.sqrt(Math.pow(fighter2.x - fighter1.x, 2) +
                Math.pow(fighter2.y - fighter1.y, 2));

        if (distance < 100 && random.nextInt(30) == 0) {
            if (random.nextBoolean()) {
                fighter2.punch();
            } else {
                fighter2.kick();
            }
        } else if (distance > 150) {
            // Move towards player
            if (fighter1.x < fighter2.x) fighter2.moveLeft();
            else fighter2.moveRight();
            if (fighter1.y < fighter2.y) fighter2.moveUp();
            else fighter2.moveDown();
        } else if (distance < 50 && random.nextInt(20) == 0) {
            fighter2.block();
        }
    }

    private void checkCollisions() {
        // Fighter 1 attacks Fighter 2
        if (fighter1.isAttacking() && !fighter2.isBlocking() &&
                getDistance(fighter1, fighter2) < fighter1.getAttackRange()) {

            int damage = fighter1.getAttackDamage();
            fighter2.takeDamage(damage);

            // Create impact particles
            particleSystem.createImpactEffect(fighter2.x, fighter2.y);

            // Screen shake
            camera.shake();

            // Knockback
            applyKnockback(fighter2, fighter1);
        }

        // Fighter 2 attacks Fighter 1
        if (fighter2.isAttacking() && !fighter1.isBlocking() &&
                getDistance(fighter1, fighter2) < fighter2.getAttackRange()) {

            int damage = fighter2.getAttackDamage();
            fighter1.takeDamage(damage);

            particleSystem.createImpactEffect(fighter1.x, fighter1.y);
            camera.shake();
            applyKnockback(fighter1, fighter2);
        }
    }

    private void applyKnockback(Fighter target, Fighter attacker) {
        double angle = Math.atan2(target.y - attacker.y, target.x - attacker.x);
        target.velocity.x += Math.cos(angle) * 5;
        target.velocity.y += Math.sin(angle) * 5;
    }

    private double getDistance(Fighter f1, Fighter f2) {
        return Math.sqrt(Math.pow(f1.x - f2.x, 2) + Math.pow(f1.y - f2.y, 2));
    }

    private void checkWinConditions() {
        if (fighter1.health <= 0) {
            gameState = GameState.GAME_OVER;
        } else if (fighter2.health <= 0) {
            gameState = GameState.GAME_OVER;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        inputHandler.keyPressed(e.getKeyCode());

        if (gameState == GameState.FIGHTING) {
            handleFightingInput(e.getKeyCode());
        } else if (gameState == GameState.MENU) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                gameState = GameState.FIGHTING;
            }
        }
    }

    private void handleFightingInput(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_A: fighter1.moveLeft(); break;
            case KeyEvent.VK_D: fighter1.moveRight(); break;
            case KeyEvent.VK_W: fighter1.moveUp(); break;
            case KeyEvent.VK_S: fighter1.moveDown(); break;
            case KeyEvent.VK_J: fighter1.punch(); break;
            case KeyEvent.VK_K: fighter1.kick(); break;
            case KeyEvent.VK_L: fighter1.block(); break;
            case KeyEvent.VK_SPACE: fighter1.dodge(); break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        inputHandler.keyReleased(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Fighter class
    class Fighter {
        String name;
        double x, y;
        int health, maxHealth;
        int stamina, maxStamina;
        Color color;
        boolean isPlayer;

        // Combat stats
        int strength = 20;
        int speed = 5;
        int defense = 10;

        // Animation and state
        Vector2D velocity;
        double rotation;
        int animationFrame;
        int animationTimer;

        // Combat state
        boolean attacking;
        boolean blocking;
        boolean dodging;
        int attackTimer;
        int blockTimer;
        int dodgeTimer;
        AttackType currentAttack;

        // Visual effects
        Color hitFlashColor;
        int hitFlashTimer;

        enum AttackType { PUNCH, KICK, NONE }

        public Fighter(String name, double x, double y, Color color, boolean isPlayer) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.color = color;
            this.isPlayer = isPlayer;
            this.maxHealth = 100;
            this.health = maxHealth;
            this.maxStamina = 100;
            this.stamina = maxStamina;
            this.velocity = new Vector2D(0, 0);
            this.currentAttack = AttackType.NONE;
        }

        public void update() {
            // Update position
            x += velocity.x;
            y += velocity.y;

            // Apply friction
            velocity.x *= 0.9;
            velocity.y *= 0.9;

            // Boundary collision
            x = Math.max(50, Math.min(WINDOW_WIDTH - 50, x));
            y = Math.max(50, Math.min(WINDOW_HEIGHT - 50, y));

            // Update timers
            if (attackTimer > 0) {
                attackTimer--;
                if (attackTimer == 0) {
                    attacking = false;
                    currentAttack = AttackType.NONE;
                }
            }

            if (blockTimer > 0) {
                blockTimer--;
                if (blockTimer == 0) blocking = false;
            }

            if (dodgeTimer > 0) {
                dodgeTimer--;
                if (dodgeTimer == 0) dodging = false;
            }

            if (hitFlashTimer > 0) hitFlashTimer--;

            // Regenerate stamina
            if (stamina < maxStamina && !attacking && !blocking) {
                stamina = Math.min(maxStamina, stamina + 1);
            }

            // Update animation
            animationTimer++;
            if (animationTimer % 10 == 0) {
                animationFrame = (animationFrame + 1) % 4;
            }
        }

        public void moveLeft() {
            if (stamina > 0 && !attacking && !blocking) {
                velocity.x -= speed;
                stamina--;
            }
        }

        public void moveRight() {
            if (stamina > 0 && !attacking && !blocking) {
                velocity.x += speed;
                stamina--;
            }
        }

        public void moveUp() {
            if (stamina > 0 && !attacking && !blocking) {
                velocity.y -= speed;
                stamina--;
            }
        }

        public void moveDown() {
            if (stamina > 0 && !attacking && !blocking) {
                velocity.y += speed;
                stamina--;
            }
        }

        public void punch() {
            if (!attacking && !blocking && stamina >= 20) {
                attacking = true;
                attackTimer = 20;
                currentAttack = AttackType.PUNCH;
                stamina -= 20;
            }
        }

        public void kick() {
            if (!attacking && !blocking && stamina >= 30) {
                attacking = true;
                attackTimer = 30;
                currentAttack = AttackType.KICK;
                stamina -= 30;
            }
        }

        public void block() {
            if (!attacking && stamina >= 10) {
                blocking = true;
                blockTimer = 30;
                stamina -= 10;
            }
        }

        public void dodge() {
            if (!attacking && !blocking && stamina >= 25) {
                dodging = true;
                dodgeTimer = 20;
                stamina -= 25;

                // Quick movement in opposite direction of opponent
                Fighter opponent = (this == fighter1) ? fighter2 : fighter1;
                double angle = Math.atan2(y - opponent.y, x - opponent.x);
                velocity.x += Math.cos(angle) * 10;
                velocity.y += Math.sin(angle) * 10;
            }
        }

        public void takeDamage(int damage) {
            if (!dodging) {
                int actualDamage = Math.max(1, damage - defense);
                if (blocking) actualDamage /= 2;

                health = Math.max(0, health - actualDamage);
                hitFlashTimer = 10;
                hitFlashColor = Color.WHITE;
            }
        }

        public boolean isAttacking() { return attacking; }
        public boolean isBlocking() { return blocking; }
        public boolean isDodging() { return dodging; }

        public int getAttackDamage() {
            return currentAttack == AttackType.KICK ? strength + 10 : strength;
        }

        public int getAttackRange() {
            return currentAttack == AttackType.KICK ? 80 : 60;
        }

        public void draw(Graphics2D g2d) {
            // Save original transform
            AffineTransform original = g2d.getTransform();

            // Apply camera transform
            g2d.translate(-camera.x, -camera.y);

            // Hit flash effect
            if (hitFlashTimer > 0) {
                g2d.setColor(hitFlashColor);
                g2d.fillOval((int)x - 35, (int)y - 35, 70, 70);
            }

            // Draw fighter body
            g2d.setColor(dodging ? color.brighter() : color);
            g2d.fillOval((int)x - 25, (int)y - 25, 50, 50);

            // Draw blocking pose
            if (blocking) {
                g2d.setColor(Color.GRAY);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval((int)x - 30, (int)y - 30, 60, 60);
            }

            // Draw attack effects
            if (attacking) {
                g2d.setColor(Color.YELLOW);
                if (currentAttack == AttackType.PUNCH) {
                    int range = getAttackRange();
                    g2d.fillOval((int)x - range/2, (int)y - 10, range, 20);
                } else if (currentAttack == AttackType.KICK) {
                    int range = getAttackRange();
                    g2d.fillArc((int)x - range, (int)y - range, range*2, range*2, 0, 180);
                }
            }

            // Draw name
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            int nameWidth = fm.stringWidth(name);
            g2d.drawString(name, (int)x - nameWidth/2, (int)y - 40);

            // Restore transform
            g2d.setTransform(original);
        }
    }

    // Particle System
    class ParticleSystem {
        private List<Particle> particles;

        public ParticleSystem() {
            particles = new CopyOnWriteArrayList<>();
        }

        public void createImpactEffect(double x, double y) {
            for (int i = 0; i < 10; i++) {
                particles.add(new Particle(x, y, Color.ORANGE));
            }
        }

        public void update() {
            particles.removeIf(p -> p.isDead());
            particles.forEach(Particle::update);
        }

        public void draw(Graphics2D g2d) {
            particles.forEach(p -> p.draw(g2d));
        }
    }

    class Particle {
        double x, y, vx, vy;
        Color color;
        int life, maxLife;

        public Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.maxLife = this.life = 30;

            double angle = random.nextDouble() * Math.PI * 2;
            double speed = random.nextDouble() * 5 + 2;
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 0.2; // gravity
            life--;
        }

        public boolean isDead() {
            return life <= 0;
        }

        public void draw(Graphics2D g2d) {
            float alpha = (float)life / maxLife;
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255 * alpha)));
            g2d.fillOval((int)x - 2, (int)y - 2, 4, 4);
        }
    }

    // Camera class for screen effects
    class Camera {
        double x, y;
        double shakeIntensity;
        int shakeTimer;

        public void update(Fighter f1, Fighter f2) {
            // Follow fighters
            x = (f1.x + f2.x) / 2 - WINDOW_WIDTH / 2;
            y = (f1.y + f2.y) / 2 - WINDOW_HEIGHT / 2;

            // Apply screen shake
            if (shakeTimer > 0) {
                x += (random.nextDouble() - 0.5) * shakeIntensity;
                y += (random.nextDouble() - 0.5) * shakeIntensity;
                shakeTimer--;
                shakeIntensity *= 0.9;
            }
        }

        public void shake() {
            shakeIntensity = 10;
            shakeTimer = 15;
        }
    }

    // Input handling system
    class InputHandler {
        private Set<Integer> pressedKeys = new HashSet<>();

        public void keyPressed(int keyCode) {
            pressedKeys.add(keyCode);
        }

        public void keyReleased(int keyCode) {
            pressedKeys.remove(keyCode);
        }

        public boolean isKeyPressed(int keyCode) {
            return pressedKeys.contains(keyCode);
        }
    }

    // HUD (Heads-Up Display)
    class HUD {
        public void draw(Graphics2D g2d) {
            // Health bars
            drawHealthBar(g2d, fighter1, 50, 50);
            drawHealthBar(g2d, fighter2, WINDOW_WIDTH - 350, 50);

            // Stamina bars
            drawStaminaBar(g2d, fighter1, 50, 80);
            drawStaminaBar(g2d, fighter2, WINDOW_WIDTH - 350, 80);

            // Controls
            if (gameState == GameState.MENU) {
                drawMenu(g2d);
            } else if (gameState == GameState.FIGHTING) {
                drawControls(g2d);
            }
        }

        private void drawHealthBar(Graphics2D g2d, Fighter fighter, int x, int y) {
            // Background
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(x, y, 300, 20);

            // Health
            g2d.setColor(Color.RED);
            int healthWidth = (int)(300.0 * fighter.health / fighter.maxHealth);
            g2d.fillRect(x, y, healthWidth, 20);

            // Border
            g2d.setColor(Color.WHITE);
            g2d.drawRect(x, y, 300, 20);

            // Text
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString(fighter.name + " HP: " + fighter.health + "/" + fighter.maxHealth, x, y - 5);
        }

        private void drawStaminaBar(Graphics2D g2d, Fighter fighter, int x, int y) {
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(x, y, 300, 15);

            g2d.setColor(Color.BLUE);
            int staminaWidth = (int)(300.0 * fighter.stamina / fighter.maxStamina);
            g2d.fillRect(x, y, staminaWidth, 15);

            g2d.setColor(Color.WHITE);
            g2d.drawRect(x, y, 300, 15);
        }

        private void drawMenu(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            String title = "MMA SIMULATOR";
            g2d.drawString(title, WINDOW_WIDTH/2 - fm.stringWidth(title)/2, WINDOW_HEIGHT/2 - 50);

            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            fm = g2d.getFontMetrics();
            String start = "Press ENTER to Start";
            g2d.drawString(start, WINDOW_WIDTH/2 - fm.stringWidth(start)/2, WINDOW_HEIGHT/2 + 50);
        }

        private void drawControls(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            String[] controls = {
                    "WASD: Move", "J: Punch", "K: Kick", "L: Block", "SPACE: Dodge"
            };
            for (int i = 0; i < controls.length; i++) {
                g2d.drawString(controls[i], 50, WINDOW_HEIGHT - 100 + i * 15);
            }
        }
    }

    // Sound manager (placeholder)
    class SoundManager {
        public void playSound(String soundName) {
            // Sound implementation would go here
        }
    }

    // Utility classes
    class Vector2D {
        double x, y;

        public Vector2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    // Main game panel
    class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            // Enable antialiasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background
            drawBackground(g2d);

            if (gameState == GameState.FIGHTING || gameState == GameState.PAUSED) {
                // Draw fighters
                fighter1.draw(g2d);
                fighter2.draw(g2d);

                // Draw particles
                particleSystem.draw(g2d);
            }

            // Draw HUD
            hud.draw(g2d);

            if (gameState == GameState.GAME_OVER) {
                drawGameOver(g2d);
            }
        }

        private void drawBackground(Graphics2D g2d) {
            // Arena background
            g2d.setColor(new Color(40, 40, 40));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

            // Octagon
            g2d.setColor(new Color(80, 80, 80));
            int centerX = WINDOW_WIDTH / 2;
            int centerY = WINDOW_HEIGHT / 2;
            g2d.fillOval(centerX - OCTAGON_SIZE/2, centerY - OCTAGON_SIZE/2, OCTAGON_SIZE, OCTAGON_SIZE);

            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval(centerX - OCTAGON_SIZE/2, centerY - OCTAGON_SIZE/2, OCTAGON_SIZE, OCTAGON_SIZE);

            // Center line
            g2d.drawLine(centerX, centerY - OCTAGON_SIZE/2, centerX, centerY + OCTAGON_SIZE/2);
        }

        private void drawGameOver(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();

            String winner = fighter1.health > 0 ? fighter1.name + " WINS!" : fighter2.name + " WINS!";
            g2d.drawString(winner, WINDOW_WIDTH/2 - fm.stringWidth(winner)/2, WINDOW_HEIGHT/2);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new MixedMartialArtsSimulator5();
        });
    }
}