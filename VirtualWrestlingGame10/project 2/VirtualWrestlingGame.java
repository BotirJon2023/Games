import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VirtualWrestlingGame extends JFrame {
    private GamePanel gamePanel;
    private Timer gameTimer;
    private static final int FRAME_WIDTH = 1200;
    private static final int FRAME_HEIGHT = 700;

    public VirtualWrestlingGame() {
        setTitle("Virtual Wrestling Championship");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        setLocationRelativeTo(null);
        setVisible(true);

        gameTimer = new Timer(16, e -> gamePanel.update());
        gameTimer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VirtualWrestlingGame());
    }
}

class GamePanel extends JPanel implements KeyListener {
    private Wrestler player1;
    private Wrestler player2;
    private WrestlingRing ring;
    private List<ParticleEffect> particles;
    private GameState gameState;
    private int roundNumber;
    private String statusMessage;
    private int messageTimer;
    private HealthBar healthBar1;
    private HealthBar healthBar2;
    private Random random;
    private boolean showInstructions;
    private int animationFrame;

    public GamePanel() {
        setBackground(new Color(20, 20, 30));
        setFocusable(true);
        addKeyListener(this);

        ring = new WrestlingRing(100, 400, 1000, 250);
        player1 = new Wrestler(250, 500, "BLUE THUNDER", Color.CYAN, true);
        player2 = new Wrestler(850, 500, "RED DESTROYER", Color.RED, false);

        particles = new ArrayList<>();
        random = new Random();
        gameState = GameState.READY;
        roundNumber = 1;
        statusMessage = "PRESS SPACE TO START!";
        messageTimer = 0;
        animationFrame = 0;
        showInstructions = true;

        healthBar1 = new HealthBar(50, 30, 300, 30, Color.CYAN);
        healthBar2 = new HealthBar(850, 30, 300, 30, Color.RED);
    }

    public void update() {
        animationFrame++;

        if (gameState == GameState.FIGHTING) {
            player1.update();
            player2.update();

            checkCollisions();
            updateAI();

            if (player1.health <= 0 || player2.health <= 0) {
                gameState = GameState.ROUND_OVER;
                if (player1.health <= 0) {
                    statusMessage = player2.name + " WINS ROUND " + roundNumber + "!";
                    player2.score++;
                } else {
                    statusMessage = player1.name + " WINS ROUND " + roundNumber + "!";
                    player1.score++;
                }
                messageTimer = 180;
            }
        } else if (gameState == GameState.ROUND_OVER) {
            messageTimer--;
            if (messageTimer <= 0) {
                if (player1.score >= 3 || player2.score >= 3) {
                    gameState = GameState.GAME_OVER;
                    if (player1.score >= 3) {
                        statusMessage = player1.name + " IS THE CHAMPION!";
                    } else {
                        statusMessage = player2.name + " IS THE CHAMPION!";
                    }
                } else {
                    startNewRound();
                }
            }
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            ParticleEffect p = particles.get(i);
            p.update();
            if (p.isDead()) {
                particles.remove(i);
            }
        }

        healthBar1.update(player1.health);
        healthBar2.update(player2.health);

        repaint();
    }

    private void updateAI() {
        double distance = Math.abs(player2.x - player1.x);

        if (random.nextInt(100) < 5) {
            if (distance < 100 && random.nextBoolean()) {
                player2.attack();
            } else if (distance < 80) {
                player2.specialAttack();
            }
        }

        if (distance > 150) {
            player2.moveLeft();
        } else if (distance < 80 && player1.isAttacking) {
            if (random.nextInt(100) < 30) {
                player2.block();
            }
        }

        if (random.nextInt(200) < 5) {
            player2.jump();
        }
    }

    private void checkCollisions() {
        double distance = Math.abs(player2.x - player1.x);

        if (distance < 80) {
            if (player1.isAttacking && !player2.isBlocking && player1.attackCooldown == player1.ATTACK_DURATION - 5) {
                player2.takeDamage(15);
                createHitEffect(player2.x, player2.y, Color.RED);
                statusMessage = "HIT! " + player1.name + " STRIKES!";
                messageTimer = 30;
            }

            if (player2.isAttacking && !player1.isBlocking && player2.attackCooldown == player2.ATTACK_DURATION - 5) {
                player1.takeDamage(15);
                createHitEffect(player1.x, player1.y, Color.RED);
                statusMessage = "HIT! " + player2.name + " STRIKES!";
                messageTimer = 30;
            }

            if (player1.isSpecialAttacking && !player2.isBlocking && player1.specialCooldown == player1.SPECIAL_DURATION - 10) {
                player2.takeDamage(30);
                createExplosionEffect(player2.x, player2.y, player1.color);
                statusMessage = "SUPER MOVE! " + player1.name + "!";
                messageTimer = 40;
            }

            if (player2.isSpecialAttacking && !player1.isBlocking && player2.specialCooldown == player2.SPECIAL_DURATION - 10) {
                player1.takeDamage(30);
                createExplosionEffect(player1.x, player1.y, player2.color);
                statusMessage = "SUPER MOVE! " + player2.name + "!";
                messageTimer = 40;
            }
        }
    }

    private void createHitEffect(double x, double y, Color color) {
        for (int i = 0; i < 15; i++) {
            particles.add(new ParticleEffect(x, y, color));
        }
    }

    private void createExplosionEffect(double x, double y, Color color) {
        for (int i = 0; i < 40; i++) {
            particles.add(new ParticleEffect(x, y, color, true));
        }
    }

    private void startNewRound() {
        roundNumber++;
        player1.reset(250, 500);
        player2.reset(850, 500);
        gameState = GameState.READY;
        statusMessage = "ROUND " + roundNumber + " - PRESS SPACE!";
        showInstructions = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2d);
        ring.draw(g2d);

        for (ParticleEffect p : particles) {
            p.draw(g2d);
        }

        player1.draw(g2d);
        player2.draw(g2d);

        healthBar1.draw(g2d, player1.name);
        healthBar2.draw(g2d, player2.name);

        drawScore(g2d);
        drawStatus(g2d);

        if (showInstructions) {
            drawInstructions(g2d);
        }

        if (gameState == GameState.GAME_OVER) {
            drawGameOver(g2d);
        }
    }

    private void drawBackground(Graphics2D g2d) {
        GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 20, 40),
                                                   0, getHeight(), new Color(40, 20, 60));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(new Color(255, 255, 255, 20));
        for (int i = 0; i < 5; i++) {
            int y = 100 + i * 120;
            g2d.drawLine(0, y, getWidth(), y);
        }
    }

    private void drawScore(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        String scoreText = "SCORE: " + player1.score + " - " + player2.score;
        FontMetrics fm = g2d.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(scoreText)) / 2;
        g2d.drawString(scoreText, x, 90);
    }

    private void drawStatus(Graphics2D g2d) {
        if (messageTimer > 0 || gameState != GameState.FIGHTING) {
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(statusMessage)) / 2;

            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRoundRect(x - 20, 130, fm.stringWidth(statusMessage) + 40, 60, 10, 10);

            g2d.setColor(Color.YELLOW);
            g2d.drawString(statusMessage, x, 170);

            if (messageTimer > 0) messageTimer--;
        }
    }

    private void drawInstructions(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRoundRect(50, 200, 400, 220, 15, 15);

        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("PLAYER 1 CONTROLS:", 70, 230);

        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.setColor(Color.WHITE);
        g2d.drawString("A / D - Move Left/Right", 70, 260);
        g2d.drawString("W - Jump", 70, 285);
        g2d.drawString("J - Attack", 70, 310);
        g2d.drawString("K - Block", 70, 335);
        g2d.drawString("L - Special Attack", 70, 360);

        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("First to 3 rounds wins!", 70, 395);
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 220));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        g2d.setColor(Color.GOLD);
        String text = "CHAMPIONSHIP WON!";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, 250);

        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.setColor(Color.WHITE);
        fm = g2d.getFontMetrics();
        x = (getWidth() - fm.stringWidth(statusMessage)) / 2;
        g2d.drawString(statusMessage, x, 330);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String restart = "Press R to Restart";
        fm = g2d.getFontMetrics();
        x = (getWidth() - fm.stringWidth(restart)) / 2;
        g2d.drawString(restart, x, 400);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_SPACE && gameState == GameState.READY) {
            gameState = GameState.FIGHTING;
            statusMessage = "FIGHT!";
            messageTimer = 60;
        }

        if (key == KeyEvent.VK_R && gameState == GameState.GAME_OVER) {
            player1.score = 0;
            player2.score = 0;
            roundNumber = 1;
            startNewRound();
        }

        if (gameState == GameState.FIGHTING) {
            switch (key) {
                case KeyEvent.VK_A: player1.moveLeft(); break;
                case KeyEvent.VK_D: player1.moveRight(); break;
                case KeyEvent.VK_W: player1.jump(); break;
                case KeyEvent.VK_J: player1.attack(); break;
                case KeyEvent.VK_K: player1.block(); break;
                case KeyEvent.VK_L: player1.specialAttack(); break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_K) {
            player1.isBlocking = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

class Wrestler {
    double x, y;
    double velocityX, velocityY;
    double groundY;
    String name;
    Color color;
    int health;
    int maxHealth;
    int score;
    boolean isJumping;
    boolean isAttacking;
    boolean isBlocking;
    boolean isSpecialAttacking;
    int attackCooldown;
    int specialCooldown;
    boolean facingRight;
    final int ATTACK_DURATION = 20;
    final int SPECIAL_DURATION = 30;

    public Wrestler(double x, double y, String name, Color color, boolean facingRight) {
        this.x = x;
        this.y = y;
        this.groundY = y;
        this.name = name;
        this.color = color;
        this.facingRight = facingRight;
        this.maxHealth = 100;
        this.health = maxHealth;
        this.score = 0;
        this.velocityX = 0;
        this.velocityY = 0;
        this.isJumping = false;
        this.isAttacking = false;
        this.isBlocking = false;
        this.isSpecialAttacking = false;
    }

    public void update() {
        velocityY += 0.8;
        y += velocityY;
        x += velocityX;

        if (y >= groundY) {
            y = groundY;
            velocityY = 0;
            isJumping = false;
        }

        velocityX *= 0.85;

        if (x < 120) x = 120;
        if (x > 1080) x = 1080;

        if (attackCooldown > 0) {
            attackCooldown--;
            if (attackCooldown == 0) {
                isAttacking = false;
            }
        }

        if (specialCooldown > 0) {
            specialCooldown--;
            if (specialCooldown == 0) {
                isSpecialAttacking = false;
            }
        }
    }

    public void moveLeft() {
        velocityX = -4;
        facingRight = false;
    }

    public void moveRight() {
        velocityX = 4;
        facingRight = true;
    }

    public void jump() {
        if (!isJumping) {
            velocityY = -18;
            isJumping = true;
        }
    }

    public void attack() {
        if (attackCooldown == 0 && !isSpecialAttacking) {
            isAttacking = true;
            attackCooldown = ATTACK_DURATION;
        }
    }

    public void block() {
        if (!isAttacking && !isSpecialAttacking) {
            isBlocking = true;
        }
    }

    public void specialAttack() {
        if (specialCooldown == 0 && !isAttacking) {
            isSpecialAttacking = true;
            specialCooldown = SPECIAL_DURATION;
        }
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health < 0) health = 0;
    }

    public void reset(double x, double y) {
        this.x = x;
        this.y = y;
        this.groundY = y;
        this.health = maxHealth;
        this.velocityX = 0;
        this.velocityY = 0;
        this.isJumping = false;
        this.isAttacking = false;
        this.isBlocking = false;
        this.isSpecialAttacking = false;
        this.attackCooldown = 0;
        this.specialCooldown = 0;
    }

    public void draw(Graphics2D g2d) {
        drawBody(g2d);
        drawEffects(g2d);
    }

    private void drawBody(Graphics2D g2d) {
        int direction = facingRight ? 1 : -1;

        g2d.setColor(color.darker());
        g2d.fillOval((int)(x - 25), (int)(y - 100), 50, 60);

        g2d.setColor(color);
        g2d.fillOval((int)(x - 20), (int)(y - 95), 40, 50);

        g2d.setColor(color.darker().darker());
        g2d.fillOval((int)(x - 15), (int)(y - 140), 30, 30);

        if (isAttacking) {
            g2d.setColor(Color.YELLOW);
            g2d.fillOval((int)(x + direction * 25), (int)(y - 80), 20, 20);
        }

        g2d.setColor(color.darker());
        if (!isAttacking) {
            g2d.fillRect((int)(x + direction * 10), (int)(y - 90), 20, 10);
        } else {
            g2d.fillRect((int)(x + direction * 20), (int)(y - 90), 25, 10);
        }

        g2d.fillRect((int)(x - 10), (int)(y - 40), 10, 30);
        g2d.fillRect((int)(x + 5), (int)(y - 40), 10, 30);

        if (isBlocking) {
            g2d.setColor(new Color(100, 200, 255, 150));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval((int)(x - 35), (int)(y - 120), 70, 110);
        }
    }

    private void drawEffects(Graphics2D g2d) {
        if (isSpecialAttacking && specialCooldown > 15) {
            for (int i = 0; i < 3; i++) {
                g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100 - i * 30));
                int size = 80 + i * 20;
                g2d.fillOval((int)(x - size/2), (int)(y - size - 20), size, size);
            }
        }
    }
}

class WrestlingRing {
    int x, y, width, height;

    public WrestlingRing(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(80, 50, 30));
        g2d.fillRect(x, y, width, height);

        g2d.setColor(new Color(100, 70, 50));
        for (int i = 0; i < width; i += 100) {
            g2d.fillRect(x + i, y, 50, height);
        }

        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect(x, y, width, height);

        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(x + 50, y, x + 50, y + height);
        g2d.drawLine(x + width - 50, y, x + width - 50, y + height);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        int centerX = x + width / 2;
        g2d.drawLine(centerX, y, centerX, y + height);
    }
}

class HealthBar {
    int x, y, width, height;
    Color color;
    int currentHealth;
    int targetHealth;

    public HealthBar(int x, int y, int width, int height, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.currentHealth = 100;
        this.targetHealth = 100;
    }

    public void update(int health) {
        targetHealth = health;
        if (currentHealth > targetHealth) {
            currentHealth -= 2;
            if (currentHealth < targetHealth) currentHealth = targetHealth;
        }
    }

    public void draw(Graphics2D g2d, String name) {
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRoundRect(x - 2, y - 2, width + 4, height + 4, 8, 8);

        g2d.setColor(new Color(100, 0, 0));
        g2d.fillRoundRect(x, y, width, height, 5, 5);

        int healthWidth = (int)(width * (currentHealth / 100.0));
        GradientPaint gradient = new GradientPaint(x, y, color.brighter(),
                                                   x + healthWidth, y, color.darker());
        g2d.setPaint(gradient);
        g2d.fillRoundRect(x, y, healthWidth, height, 5, 5);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(x, y, width, height, 5, 5);

        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString(name, x, y - 5);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String healthText = currentHealth + "/100";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = x + (width - fm.stringWidth(healthText)) / 2;
        g2d.drawString(healthText, textX, y + 20);
    }
}

class ParticleEffect {
    double x, y;
    double velocityX, velocityY;
    Color color;
    int life;
    int maxLife;
    double size;

    public ParticleEffect(double x, double y, Color color) {
        this(x, y, color, false);
    }

    public ParticleEffect(double x, double y, Color color, boolean explosion) {
        this.x = x;
        this.y = y;
        this.color = color;
        Random rand = new Random();

        if (explosion) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double speed = rand.nextDouble() * 8 + 4;
            velocityX = Math.cos(angle) * speed;
            velocityY = Math.sin(angle) * speed;
            maxLife = 40;
            size = rand.nextDouble() * 6 + 4;
        } else {
            velocityX = (rand.nextDouble() - 0.5) * 8;
            velocityY = (rand.nextDouble() - 0.5) * 8 - 2;
            maxLife = 30;
            size = rand.nextDouble() * 4 + 2;
        }

        life = maxLife;
    }

    public void update() {
        x += velocityX;
        y += velocityY;
        velocityY += 0.3;
        life--;
    }

    public boolean isDead() {
        return life <= 0;
    }

    public void draw(Graphics2D g2d) {
        float alpha = (float)life / maxLife;
        Color drawColor = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                                     (int)(alpha * 255));
        g2d.setColor(drawColor);
        g2d.fillOval((int)(x - size/2), (int)(y - size/2), (int)size, (int)size);
    }
}

enum GameState {
    READY,
    FIGHTING,
    ROUND_OVER,
    GAME_OVER
}
