import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class VirtualWrestlingGame extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final int RING_Y = 450;
    private static final int RING_HEIGHT = 200;
    private static final int FPS = 60;

    // Game state
    private Timer gameTimer;
    private GameState gameState;
    private Wrestler player1;
    private Wrestler player2;
    private List<Particle> particles;
    private List<FloatingText> floatingTexts;
    private int roundNumber;
    private int player1Wins;
    private int player2Wins;
    private long matchStartTime;
    private boolean showControls;

    // Input states
    private boolean p1Left, p1Right, p1Up, p1Down, p1Defend;
    private boolean p2Left, p2Right, p2Up, p2Down, p2Defend;

    // Animation variables
    private double cameraShake;
    private int hitFreeze;
    private Color flashColor;
    private int flashTimer;

    enum GameState {
        MENU, PLAYING, ROUND_END, MATCH_END
    }

    enum WrestlerState {
        IDLE, WALKING, ATTACKING, STUNNED, DEFENDING, CELEBRATING, DEFEATED
    }

    enum AttackType {
        PUNCH, KICK, GRAPPLE, SPECIAL
    }

    // Wrestler class
    class Wrestler {
        String name;
        double x, y;
        double vx, vy;
        int health;
        int maxHealth;
        int energy;
        int maxEnergy;
        WrestlerState state;
        boolean facingRight;
        int stateTimer;
        int attackCooldown;
        int specialCooldown;
        Color primaryColor;
        Color secondaryColor;
        int wins;

        // Animation variables
        double animFrame;
        double bodyAngle;
        double legAngle;
        double armAngle;
        int hitStun;
        double knockbackX;
        double knockbackY;
        boolean isPlayer1;

        Wrestler(String name, double x, boolean isPlayer1, Color primary, Color secondary) {
            this.name = name;
            this.x = x;
            this.y = RING_Y;
            this.isPlayer1 = isPlayer1;
            this.facingRight = isPlayer1;
            this.primaryColor = primary;
            this.secondaryColor = secondary;
            reset();
        }

        void reset() {
            health = maxHealth = 100;
            energy = maxEnergy = 100;
            state = WrestlerState.IDLE;
            vx = vy = 0;
            stateTimer = 0;
            attackCooldown = 0;
            specialCooldown = 0;
            animFrame = 0;
            bodyAngle = 0;
            legAngle = 0;
            armAngle = 0;
            hitStun = 0;
            knockbackX = 0;
            knockbackY = 0;
        }

        void update() {
            // Update timers
            if (stateTimer > 0) stateTimer--;
            if (attackCooldown > 0) attackCooldown--;
            if (specialCooldown > 0) specialCooldown--;
            if (hitStun > 0) hitStun--;

            // Update animation frame
            animFrame += 0.15;
            if (animFrame > 2 * Math.PI) animFrame = 0;

            // Apply knockback
            if (Math.abs(knockbackX) > 0.1) {
                x += knockbackX;
                knockbackX *= 0.85;
            } else {
                knockbackX = 0;
            }

            if (Math.abs(knockbackY) > 0.1) {
                y += knockbackY;
                knockbackY *= 0.9;
            } else {
                knockbackY = 0;
            }

            // Keep in bounds
            x = Math.max(50, Math.min(WIDTH - 50, x));
            y = Math.max(RING_Y - 50, Math.min(RING_Y + 50, y));

            // Apply velocity
            x += vx;
            y += vy;

            // Friction
            vx *= 0.85;
            vy *= 0.85;

            // Energy regeneration
            if (energy < maxEnergy && state != WrestlerState.ATTACKING) {
                energy = Math.min(maxEnergy, energy + 1);
            }

            // State machine
            if (health <= 0) {
                state = WrestlerState.DEFEATED;
            } else if (hitStun > 0) {
                state = WrestlerState.STUNNED;
            } else if (stateTimer <= 0 && state == WrestlerState.ATTACKING) {
                state = WrestlerState.IDLE;
            }

            // Update animation angles
            updateAnimationAngles();
        }

        void updateAnimationAngles() {
            switch (state) {
                case WALKING:
                    legAngle = Math.sin(animFrame * 4) * 0.3;
                    armAngle = Math.sin(animFrame * 4) * 0.2;
                    break;
                case ATTACKING:
                    armAngle = Math.sin(stateTimer * 0.5) * 1.5;
                    bodyAngle = Math.sin(stateTimer * 0.3) * 0.1;
                    break;
                case STUNNED:
                    bodyAngle = Math.sin(animFrame * 8) * 0.15;
                    break;
                case CELEBRATING:
                    armAngle = Math.sin(animFrame * 3) * 0.5 + 0.8;
                    bodyAngle = Math.sin(animFrame * 2) * 0.05;
                    break;
                case DEFEATED:
                    bodyAngle = Math.PI / 4;
                    break;
                default:
                    legAngle *= 0.9;
                    armAngle *= 0.9;
                    bodyAngle *= 0.9;
            }
        }

        void move(double dx, double dy) {
            if (hitStun > 0 || state == WrestlerState.ATTACKING) return;

            vx = dx * 3;
            vy = dy * 3;

            if (dx != 0) {
                facingRight = dx > 0;
                state = WrestlerState.WALKING;
            } else if (dy != 0) {
                state = WrestlerState.WALKING;
            } else {
                if (state != WrestlerState.DEFENDING) {
                    state = WrestlerState.IDLE;
                }
            }
        }

        void attack(AttackType type) {
            if (hitStun > 0 || attackCooldown > 0) return;
            if (type == AttackType.SPECIAL && (specialCooldown > 0 || energy < 50)) return;

            state = WrestlerState.ATTACKING;
            stateTimer = 20;
            attackCooldown = 30;

            int damage = 0;
            int energyCost = 10;
            String attackName = "";

            switch (type) {
                case PUNCH:
                    damage = 8;
                    attackName = "Punch!";
                    break;
                case KICK:
                    damage = 12;
                    energyCost = 15;
                    attackName = "Kick!";
                    break;
                case GRAPPLE:
                    damage = 15;
                    energyCost = 20;
                    attackName = "Grapple!";
                    break;
                case SPECIAL:
                    damage = 25;
                    energyCost = 50;
                    specialCooldown = 180;
                    attackName = "SPECIAL MOVE!";
                    createSpecialEffect();
                    break;
            }

            energy = Math.max(0, energy - energyCost);
            checkHit(damage, type);

            // Add floating text
            floatingTexts.add(new FloatingText(attackName, x, y - 80,
                    type == AttackType.SPECIAL ? Color.YELLOW : Color.WHITE));
        }

        void checkHit(int damage, AttackType type) {
            Wrestler opponent = isPlayer1 ? player2 : player1;
            double distance = Math.abs(x - opponent.x);

            if (distance < 80 && opponent.state != WrestlerState.DEFENDING) {
                opponent.takeDamage(damage, type);

                // Knockback
                double knockbackForce = damage * 0.5;
                opponent.knockbackX = (facingRight ? 1 : -1) * knockbackForce;

                if (type == AttackType.SPECIAL) {
                    opponent.knockbackY = -10;
                    cameraShake = 15;
                    flashColor = Color.YELLOW;
                    flashTimer = 10;
                } else {
                    cameraShake = damage * 0.3;
                }

                hitFreeze = (int) (damage * 0.4);

                // Particle effects
                createHitParticles(opponent.x, opponent.y - 40, type);
            }
        }

        void takeDamage(int damage, AttackType type) {
            health = Math.max(0, health - damage);
            hitStun = 15 + (damage / 2);
            state = WrestlerState.STUNNED;

            // Create damage text
            floatingTexts.add(new FloatingText("-" + damage, x, y - 60, Color.RED));
        }

        void defend(boolean defending) {
            if (hitStun > 0 || state == WrestlerState.ATTACKING) return;
            state = defending ? WrestlerState.DEFENDING : WrestlerState.IDLE;
        }

        void createSpecialEffect() {
            for (int i = 0; i < 30; i++) {
                double angle = Math.random() * Math.PI * 2;
                double speed = 2 + Math.random() * 5;
                particles.add(new Particle(
                        x, y - 40,
                        Math.cos(angle) * speed,
                        Math.sin(angle) * speed,
                        Color.YELLOW,
                        20 + (int) (Math.random() * 20)
                ));
            }
        }

        void draw(Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            AffineTransform old = g2d.getTransform();

            // Flip if facing left
            if (!facingRight) {
                g2d.translate(x * 2, 0);
                g2d.scale(-1, 1);
            }

            // Apply body angle rotation
            g2d.rotate(bodyAngle, x, y);

            // Shadow
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillOval((int) x - 25, RING_Y + 40, 50, 15);

            // Legs
            drawLimb(g2d, x - 10, y, 12, 35, legAngle, secondaryColor);
            drawLimb(g2d, x + 10, y, 12, 35, -legAngle, secondaryColor);

            // Body
            g2d.setColor(primaryColor);
            g2d.fillRoundRect((int) x - 20, (int) y - 50, 40, 50, 15, 15);

            // Arms
            if (state == WrestlerState.DEFENDING) {
                drawLimb(g2d, x - 20, y - 40, 10, 30, -0.5, secondaryColor);
                drawLimb(g2d, x + 20, y - 40, 10, 30, 0.5, secondaryColor);
            } else {
                drawLimb(g2d, x - 20, y - 40, 10, 30, armAngle, secondaryColor);
                drawLimb(g2d, x + 20, y - 40, 10, 30, -armAngle, secondaryColor);
            }

            // Head
            g2d.setColor(new Color(255, 220, 177));
            g2d.fillOval((int) x - 18, (int) y - 75, 36, 36);

            // Face details
            g2d.setColor(Color.BLACK);
            // Eyes
            if (state == WrestlerState.STUNNED) {
                g2d.drawString("X", (int) x - 12, (int) y - 55);
                g2d.drawString("X", (int) x + 5, (int) y - 55);
            } else {
                g2d.fillOval((int) x - 10, (int) y - 60, 6, 6);
                g2d.fillOval((int) x + 4, (int) y - 60, 6, 6);
            }
            // Mouth
            if (state == WrestlerState.CELEBRATING) {
                g2d.drawArc((int) x - 8, (int) y - 50, 16, 10, 0, -180);
            } else if (state == WrestlerState.DEFEATED) {
                g2d.drawArc((int) x - 8, (int) y - 45, 16, 10, 0, 180);
            } else {
                g2d.drawLine((int) x - 6, (int) y - 48, (int) x + 6, (int) y - 48);
            }

            g2d.setTransform(old);

            // Health bar above wrestler
            drawHealthBar(g2d, x, y - 95);
        }

        void drawLimb(Graphics2D g2d, double startX, double startY, int width, int length, double angle, Color color) {
            AffineTransform old = g2d.getTransform();
            g2d.rotate(angle, startX, startY);
            g2d.setColor(color);
            g2d.fillRoundRect((int) startX - width / 2, (int) startY, width, length, width, width);
            g2d.setTransform(old);
        }

        void drawHealthBar(Graphics2D g2d, double x, double y) {
            int barWidth = 60;
            int barHeight = 6;

            // Background
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect((int) x - barWidth / 2, (int) y, barWidth, barHeight);

            // Health
            double healthPercent = (double) health / maxHealth;
            Color healthColor = healthPercent > 0.5 ? Color.GREEN :
                    healthPercent > 0.25 ? Color.ORANGE : Color.RED;
            g2d.setColor(healthColor);
            g2d.fillRect((int) x - barWidth / 2, (int) y, (int) (barWidth * healthPercent), barHeight);

            // Border
            g2d.setColor(Color.WHITE);
            g2d.drawRect((int) x - barWidth / 2, (int) y, barWidth, barHeight);
        }
    }

    // Particle class for visual effects
    class Particle {
        double x, y, vx, vy;
        Color color;
        int life, maxLife;

        Particle(double x, double y, double vx, double vy, Color color, int life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.life = this.maxLife = life;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.2; // gravity
            life--;
        }

        void draw(Graphics2D g2d) {
            float alpha = (float) life / maxLife;
            int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
            int size = (int) (4 * alpha) + 2;
            g2d.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
        }

        boolean isDead() {
            return life <= 0;
        }
    }

    // Floating text class
    class FloatingText {
        String text;
        double x, y;
        Color color;
        int life;

        FloatingText(String text, double x, double y, Color color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.life = 60;
        }

        void update() {
            y -= 1.5;
            life--;
        }

        void draw(Graphics2D g2d) {
            float alpha = Math.min(1.0f, (float) life / 30);
            int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g2d.drawString(text, (int) x - textWidth / 2, (int) y);
        }

        boolean isDead() {
            return life <= 0;
        }
    }

    public VirtualWrestlingGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        initGame();

        gameTimer = new Timer(1000 / FPS, this);
        gameTimer.start();

        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    void initGame() {
        player1 = new Wrestler("Fighter 1", 250, true, new Color(200, 50, 50), new Color(150, 30, 30));
        player2 = new Wrestler("Fighter 2", 750, false, new Color(50, 50, 200), new Color(30, 30, 150));
        particles = new ArrayList<>();
        floatingTexts = new ArrayList<>();
        gameState = GameState.MENU;
        roundNumber = 1;
        player1Wins = 0;
        player2Wins = 0;
        cameraShake = 0;
        hitFreeze = 0;
        flashTimer = 0;
        showControls = true;

        // Reset inputs
        p1Left = p1Right = p1Up = p1Down = p1Defend = false;
        p2Left = p2Right = p2Up = p2Down = p2Defend = false;
    }

    void startNewRound() {
        player1.reset();
        player2.reset();
        player1.x = 250;
        player2.x = 750;
        particles.clear();
        floatingTexts.clear();
        gameState = GameState.PLAYING;
        matchStartTime = System.currentTimeMillis();
    }

    void checkRoundEnd() {
        if (player1.health <= 0 || player2.health <= 0) {
            gameState = GameState.ROUND_END;

            if (player1.health <= 0 && player2.health <= 0) {
                // Rare double KO, no point
                floatingTexts.add(new FloatingText("DOUBLE KO!", WIDTH / 2.0, HEIGHT / 2.0, Color.YELLOW));
            } else if (player1.health <= 0) {
                player2Wins++;
                player2.state = WrestlerState.CELEBRATING;
                floatingTexts.add(new FloatingText(player2.name + " WINS ROUND " + roundNumber + "!",
                        WIDTH / 2.0, HEIGHT / 2.0, Color.YELLOW));
            } else {
                player1Wins++;
                player1.state = WrestlerState.CELEBRATING;
                floatingTexts.add(new FloatingText(player1.name + " WINS ROUND " + roundNumber + "!",
                        WIDTH / 2.0, HEIGHT / 2.0, Color.YELLOW));
            }

            roundNumber++;

            // Check for match winner
            if (player1Wins >= 2 || player2Wins >= 2) {
                gameState = GameState.MATCH_END;
            }
        }
    }

    void createHitParticles(double x, double y, AttackType type) {
        int count = type == AttackType.SPECIAL ? 20 : 10;
        Color color = type == AttackType.SPECIAL ? Color.YELLOW : Color.WHITE;

        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 1 + Math.random() * 3;
            particles.add(new Particle(x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 2,
                    color, 15 + (int) (Math.random() * 15)));
        }
    }

    void applyInputs() {
        if (gameState != GameState.PLAYING) {
            player1.defend(false);
            player2.defend(false);
            return;
        }

        double dx1 = (p1Right ? 1 : 0) - (p1Left ? 1 : 0);
        double dy1 = (p1Down ? 1 : 0) - (p1Up ? 1 : 0);
        player1.move(dx1, dy1);
        player1.defend(p1Defend);

        double dx2 = (p2Right ? 1 : 0) - (p2Left ? 1 : 0);
        double dy2 = (p2Down ? 1 : 0) - (p2Up ? 1 : 0);
        player2.move(dx2, dy2);
        player2.defend(p2Defend);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (hitFreeze > 0) {
            hitFreeze--;
            repaint();
            return;
        }

        applyInputs();

        if (gameState == GameState.PLAYING) {
            player1.update();
            player2.update();
            checkRoundEnd();
        } else if (gameState == GameState.ROUND_END || gameState == GameState.MATCH_END) {
            player1.update();
            player2.update();
        }

        // Update particles
        particles.removeIf(Particle::isDead);
        for (Particle p : particles) {
            p.update();
        }

        // Update floating texts
        floatingTexts.removeIf(FloatingText::isDead);
        for (FloatingText ft : floatingTexts) {
            ft.update();
        }

        // Update camera shake
        if (cameraShake > 0) {
            cameraShake *= 0.9;
            if (cameraShake < 0.1) cameraShake = 0;
        }

        // Update flash
        if (flashTimer > 0) flashTimer--;

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Apply camera shake
        if (cameraShake > 0) {
            int shakeX = (int) ((Math.random() - 0.5) * cameraShake);
            int shakeY = (int) ((Math.random() - 0.5) * cameraShake);
            g2d.translate(shakeX, shakeY);
        }

        // Flash effect (clamped alpha)
        if (flashTimer > 0 && flashColor != null) {
            int a = Math.min(200, flashTimer * 20);
            g2d.setColor(new Color(flashColor.getRed(), flashColor.getGreen(),
                    flashColor.getBlue(), a));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        }

        drawBackground(g2d);

        if (gameState == GameState.MENU) {
            drawMenu(g2d);
        } else {
            drawRing(g2d);
            player1.draw(g2d);
            player2.draw(g2d);

            for (Particle p : particles) {
                p.draw(g2d);
            }

            for (FloatingText ft : floatingTexts) {
                ft.draw(g2d);
            }

            drawUI(g2d);

            if (gameState == GameState.ROUND_END || gameState == GameState.MATCH_END) {
                drawRoundEnd(g2d);
            }

            if (showControls) {
                drawControls(g2d);
            }
        }
    }

    void drawBackground(Graphics2D g2d) {
        // Gradient background
        GradientPaint gp = new GradientPaint(0, 0, new Color(20, 20, 40),
                0, HEIGHT, new Color(60, 20, 60));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Stars (static seed for consistent layout)
        g2d.setColor(new Color(255, 255, 255, 100));
        Random rand = new Random(42);
        for (int i = 0; i < 50; i++) {
            int x = rand.nextInt(WIDTH);
            int y = rand.nextInt(HEIGHT / 2);
            g2d.fillOval(x, y, 2, 2);
        }
    }

    void drawRing(Graphics2D g2d) {
        // Ring floor
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect(0, RING_Y, WIDTH, RING_HEIGHT);

        // Ring mat
        g2d.setColor(new Color(0, 100, 0));
        g2d.fillRect(50, RING_Y + 20, WIDTH - 100, RING_HEIGHT - 40);

        // Ring lines
        g2d.setColor(new Color(255, 255, 255, 100));
        for (int i = 0; i < 3; i++) {
            int y = RING_Y + 30 + i * 50;
            g2d.drawLine(50, y, WIDTH - 50, y);
        }

        // Ring ropes
        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(Color.RED);
        g2d.drawLine(50, RING_Y + 30, WIDTH - 50, RING_Y + 30);
        g2d.drawLine(50, RING_Y + 60, WIDTH - 50, RING_Y + 60);
        g2d.drawLine(50, RING_Y + 90, WIDTH - 50, RING_Y + 90);
    }

    void drawUI(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Player 1 info
        drawPlayerUI(g2d, player1, 20, 20, true);

        // Player 2 info
        drawPlayerUI(g2d, player2, WIDTH - 320, 20, false);

        // Round info
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        String roundText = "ROUND " + roundNumber;
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(roundText, WIDTH / 2 - fm.stringWidth(roundText) / 2, 40);

        // Score
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        String scoreText = player1Wins + " - " + player2Wins;
        g2d.drawString(scoreText, WIDTH / 2 - fm.stringWidth(scoreText) / 2, 65);
    }

    void drawPlayerUI(Graphics2D g2d, Wrestler w, int x, int y, boolean isLeft) {
        // Panel background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(x, y, 300, 100, 10, 10);

        // Name
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString(w.name, x + 10, y + 25);

        // Health bar
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(x + 10, y + 35, 280, 20);
        double healthPercent = (double) w.health / w.maxHealth;
        Color healthColor = healthPercent > 0.5 ? Color.GREEN :
                healthPercent > 0.25 ? Color.ORANGE : Color.RED;
        g2d.setColor(healthColor);
        g2d.fillRect(x + 10, y + 35, (int) (280 * healthPercent), 20);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(x + 10, y + 35, 280, 20);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("HP: " + w.health + "/" + w.maxHealth, x + 15, y + 50);

        // Energy bar
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(x + 10, y + 65, 280, 15);
        g2d.setColor(Color.CYAN);
        g2d.fillRect(x + 10, y + 65, (int) (280.0 * w.energy / w.maxEnergy), 15);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(x + 10, y + 65, 280, 15);
        g2d.drawString("Energy: " + w.energy, x + 15, y + 77);
    }

    void drawMenu(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String title = "VIRTUAL WRESTLING";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, WIDTH / 2 - fm.stringWidth(title) / 2, 200);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String subtitle = "Press SPACE to Start";
        fm = g2d.getFontMetrics();
        g2d.drawString(subtitle, WIDTH / 2 - fm.stringWidth(subtitle) / 2, 280);

        // Controls
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        String[] controls = {
                "Player 1 (Red): WASD to move, F = Punch, G = Kick, H = Grapple, J = Special",
                "Player 2 (Blue): Arrow keys to move, 1 = Punch, 2 = Kick, 3 = Grapple, 4 = Special",
                "Left Shift = P1 Defend, Right Shift = P2 Defend",
                "",
                "First to win 2 rounds wins the match!",
                "Press C to toggle controls overlay"
        };

        int yPos = 350;
        for (String line : controls) {
            fm = g2d.getFontMetrics();
            g2d.drawString(line, WIDTH / 2 - fm.stringWidth(line) / 2, yPos);
            yPos += 28;
        }
    }

    void drawRoundEnd(Graphics2D g2d) {
        // Overlay
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));

        String msg;
        if (gameState == GameState.MATCH_END) {
            String winner = player1Wins > player2Wins ? player1.name : player2.name;
            msg = "MATCH OVER! " + winner + " WINS!";
        } else {
            msg = "ROUND OVER";
        }
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(msg, WIDTH / 2 - fm.stringWidth(msg) / 2, HEIGHT / 2 - 40);

        g2d.setFont(new Font("Arial", Font.PLAIN, 22));
        String next = (gameState == GameState.MATCH_END)
                ? "Press ENTER to Restart Match"
                : "Press SPACE for Next Round";
        fm = g2d.getFontMetrics();
        g2d.drawString(next, WIDTH / 2 - fm.stringWidth(next) / 2, HEIGHT / 2 + 10);
    }

    void drawControls(Graphics2D g2d) {
        int w = 520, h = 130;
        int x = 20, y = HEIGHT - h - 20;

        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.fillRoundRect(x, y, w, h, 10, 10);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Controls (press C to toggle)", x + 10, y + 22);

        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        int yy = y + 40;
        String[] lines = new String[]{
                "P1: WASD to move, F=Punch, G=Kick, H=Grapple, J=Special, Left Shift=Defend",
                "P2: Arrows to move, 1=Punch, 2=Kick, 3=Grapple, 4=Special, Right Shift=Defend",
                "Space: Start/Next Round, Enter: Restart after match end, Esc: Quit"
        };
        for (String line : lines) {
            g2d.drawString(line, x + 10, yy);
            yy += 22;
        }
    }

    // Key handling
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // Global
        if (code == KeyEvent.VK_ESCAPE) System.exit(0);
        if (code == KeyEvent.VK_C) showControls = !showControls;

        if (code == KeyEvent.VK_SPACE) {
            if (gameState == GameState.MENU) {
                startNewRound();
            } else if (gameState == GameState.ROUND_END) {
                startNewRound();
            }
        }
        if (code == KeyEvent.VK_ENTER) {
            if (gameState == GameState.MATCH_END) {
                initGame();
            }
        }

        // Defend mapping with key location
        if (code == KeyEvent.VK_SHIFT) {
            int loc = e.getKeyLocation();
            if (loc == KeyEvent.KEY_LOCATION_LEFT) {
                p1Defend = true;
            } else if (loc == KeyEvent.KEY_LOCATION_RIGHT) {
                p2Defend = true;
            } else {
                // fallback
                p1Defend = true;
            }
        }

        // Player 1 movement
        if (code == KeyEvent.VK_A) p1Left = true;
        if (code == KeyEvent.VK_D) p1Right = true;
        if (code == KeyEvent.VK_W) p1Up = true;
        if (code == KeyEvent.VK_S) p1Down = true;

        // Player 2 movement
        if (code == KeyEvent.VK_LEFT) p2Left = true;
        if (code == KeyEvent.VK_RIGHT) p2Right = true;
        if (code == KeyEvent.VK_UP) p2Up = true;
        if (code == KeyEvent.VK_DOWN) p2Down = true;

        // Attacks only during PLAYING
        if (gameState == GameState.PLAYING) {
            // P1 attacks
            if (code == KeyEvent.VK_F) player1.attack(AttackType.PUNCH);
            if (code == KeyEvent.VK_G) player1.attack(AttackType.KICK);
            if (code == KeyEvent.VK_H) player1.attack(AttackType.GRAPPLE);
            if (code == KeyEvent.VK_J) player1.attack(AttackType.SPECIAL);

            // P2 attacks
            if (code == KeyEvent.VK_1) player2.attack(AttackType.PUNCH);
            if (code == KeyEvent.VK_2) player2.attack(AttackType.KICK);
            if (code == KeyEvent.VK_3) player2.attack(AttackType.GRAPPLE);
            if (code == KeyEvent.VK_4) player2.attack(AttackType.SPECIAL);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        // Defend release
        if (code == KeyEvent.VK_SHIFT) {
            int loc = e.getKeyLocation();
            if (loc == KeyEvent.KEY_LOCATION_LEFT) {
                p1Defend = false;
            } else if (loc == KeyEvent.KEY_LOCATION_RIGHT) {
                p2Defend = false;
            } else {
                // fallback
                p1Defend = false;
                p2Defend = false;
            }
        }

        // Player 1 movement
        if (code == KeyEvent.VK_A) p1Left = false;
        if (code == KeyEvent.VK_D) p1Right = false;
        if (code == KeyEvent.VK_W) p1Up = false;
        if (code == KeyEvent.VK_S) p1Down = false;

        // Player 2 movement
        if (code == KeyEvent.VK_LEFT) p2Left = false;
        if (code == KeyEvent.VK_RIGHT) p2Right = false;
        if (code == KeyEvent.VK_UP) p2Up = false;
        if (code == KeyEvent.VK_DOWN) p2Down = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Virtual Wrestling");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            VirtualWrestlingGame game = new VirtualWrestlingGame();
            f.setContentPane(game);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
