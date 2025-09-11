import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FencingChampionship extends JFrame implements ActionListener, KeyListener {
    // Game constants
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 700;
    private static final int GROUND_Y = 550;
    private static final int FENCER_WIDTH = 60;
    private static final int FENCER_HEIGHT = 120;
    private static final int SWORD_LENGTH = 80;
    private static final int MAX_HEALTH = 100;
    private static final int ROUND_TIME = 60;

    // Game states
    private enum GameState {
        MENU, PLAYING, PAUSED, GAME_OVER, VICTORY
    }

    // Animation and timing
    private Timer gameTimer;
    private GameState currentState;
    private int animationFrame;
    private long gameStartTime;
    private int roundTimeLeft;

    // Players
    private Fencer player1;
    private Fencer player2;

    // Game data
    private int player1Score;
    private int player2Score;
    private int currentRound;
    private boolean[] keysPressed;
    private Random random;

    // Visual effects
    private List<ParticleEffect> particles;
    private List<FloatingText> floatingTexts;
    private Color backgroundColor;
    private boolean screenShake;
    private int shakeIntensity;

    // Fonts
    private Font titleFont;
    private Font gameFont;
    private Font smallFont;

    public FencingChampionship() {
        initializeGame();
        setupWindow();
    }

    private void initializeGame() {
        currentState = GameState.MENU;
        animationFrame = 0;
        keysPressed = new boolean[256];
        random = new Random();
        particles = new ArrayList<>();
        floatingTexts = new ArrayList<>();
        backgroundColor = new Color(20, 30, 40);

        // Initialize fonts
        titleFont = new Font("Arial", Font.BOLD, 48);
        gameFont = new Font("Arial", Font.BOLD, 24);
        smallFont = new Font("Arial", Font.PLAIN, 16);

        // Initialize players
        resetPlayers();

        // Game timer
        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();

        player1Score = 0;
        player2Score = 0;
        currentRound = 1;
    }

    private void resetPlayers() {
        player1 = new Fencer(200, GROUND_Y - FENCER_HEIGHT, Color.BLUE, true);
        player2 = new Fencer(700, GROUND_Y - FENCER_HEIGHT, Color.RED, false);
    }

    private void setupWindow() {
        setTitle("Fencing Championship");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
        addKeyListener(this);
        setFocusable(true);

        // Custom panel for double buffering
        JPanel gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                render((Graphics2D) g);
            }
        };
        gamePanel.setBackground(backgroundColor);
        add(gamePanel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        repaint();
    }

    private void update() {
        animationFrame++;

        switch (currentState) {
            case MENU:
                updateMenu();
                break;
            case PLAYING:
                updateGame();
                break;
            case PAUSED:
                // Game is paused, no updates
                break;
            case GAME_OVER:
            case VICTORY:
                updateEndScreen();
                break;
        }

        // Update visual effects
        updateParticles();
        updateFloatingTexts();
        updateScreenShake();
    }

    private void updateMenu() {
        // Animated background effects
        if (animationFrame % 30 == 0) {
            addMenuParticle();
        }
    }

    private void updateGame() {
        // Update timer
        long currentTime = System.currentTimeMillis();
        roundTimeLeft = ROUND_TIME - (int)((currentTime - gameStartTime) / 1000);

        if (roundTimeLeft <= 0) {
            endRound();
            return;
        }

        // Handle input
        handlePlayerInput();

        // Update players
        player1.update();
        player2.update();

        // Check collisions
        checkSwordCollisions();

        // Check victory conditions
        if (player1.health <= 0 || player2.health <= 0) {
            endRound();
        }
    }

    private void updateEndScreen() {
        // Celebration particles
        if (animationFrame % 10 == 0) {
            addCelebrationParticle();
        }
    }

    private void handlePlayerInput() {
        // Player 1 controls (WASD + F for attack, G for defend)
        if (keysPressed[KeyEvent.VK_A]) {
            player1.moveLeft();
        }
        if (keysPressed[KeyEvent.VK_D]) {
            player1.moveRight();
        }
        if (keysPressed[KeyEvent.VK_W]) {
            player1.jump();
        }
        if (keysPressed[KeyEvent.VK_F]) {
            player1.attack();
        }
        if (keysPressed[KeyEvent.VK_G]) {
            player1.defend();
        }

        // Player 2 controls (Arrow keys + K for attack, L for defend)
        if (keysPressed[KeyEvent.VK_LEFT]) {
            player2.moveLeft();
        }
        if (keysPressed[KeyEvent.VK_RIGHT]) {
            player2.moveRight();
        }
        if (keysPressed[KeyEvent.VK_UP]) {
            player2.jump();
        }
        if (keysPressed[KeyEvent.VK_K]) {
            player2.attack();
        }
        if (keysPressed[KeyEvent.VK_L]) {
            player2.defend();
        }
    }

    private void checkSwordCollisions() {
        Rectangle sword1Bounds = player1.getSwordBounds();
        Rectangle sword2Bounds = player2.getSwordBounds();
        Rectangle player1Bounds = player1.getBounds();
        Rectangle player2Bounds = player2.getBounds();

        // Player 1 hits Player 2
        if (player1.isAttacking() && !player2.isDefending() &&
                sword1Bounds.intersects(player2Bounds)) {
            hitPlayer(player2, player1, 15);
        }

        // Player 2 hits Player 1
        if (player2.isAttacking() && !player1.isDefending() &&
                sword2Bounds.intersects(player1Bounds)) {
            hitPlayer(player1, player2, 15);
        }

        // Sword clashing
        if (sword1Bounds.intersects(sword2Bounds) &&
                player1.isAttacking() && player2.isAttacking()) {
            createClashEffect(sword1Bounds.x + sword1Bounds.width/2,
                    sword1Bounds.y + sword1Bounds.height/2);
        }
    }

    private void hitPlayer(Fencer victim, Fencer attacker, int damage) {
        victim.takeDamage(damage);
        victim.knockback(attacker.facingLeft);

        // Visual effects
        createHitEffect(victim.x + FENCER_WIDTH/2, victim.y + FENCER_HEIGHT/2);
        addFloatingText("-" + damage, victim.x + FENCER_WIDTH/2, victim.y, Color.RED);
        triggerScreenShake(5);

        // Reset attack state
        attacker.finishAttack();
    }

    private void createHitEffect(int x, int y) {
        for (int i = 0; i < 10; i++) {
            particles.add(new ParticleEffect(x, y, Color.ORANGE, ParticleType.SPARK));
        }
    }

    private void createClashEffect(int x, int y) {
        for (int i = 0; i < 15; i++) {
            particles.add(new ParticleEffect(x, y, Color.YELLOW, ParticleType.SPARK));
        }
        triggerScreenShake(3);
    }

    private void addMenuParticle() {
        int x = random.nextInt(WINDOW_WIDTH);
        particles.add(new ParticleEffect(x, 0, new Color(100, 150, 255), ParticleType.SNOW));
    }

    private void addCelebrationParticle() {
        int x = random.nextInt(WINDOW_WIDTH);
        int y = random.nextInt(WINDOW_HEIGHT/2);
        Color[] colors = {Color.red, Color.YELLOW, Color.ORANGE, Color.PINK};
        particles.add(new ParticleEffect(x, y, colors[random.nextInt(colors.length)], ParticleType.CONFETTI));
    }

    private void updateParticles() {
        particles.removeIf(particle -> {
            particle.update();
            return particle.isDead();
        });
    }

    private void updateFloatingTexts() {
        floatingTexts.removeIf(text -> {
            text.update();
            return text.isDead();
        });
    }

    private void updateScreenShake() {
        if (screenShake) {
            shakeIntensity--;
            if (shakeIntensity <= 0) {
                screenShake = false;
            }
        }
    }

    private void triggerScreenShake(int intensity) {
        screenShake = true;
        shakeIntensity = intensity;
    }

    private void addFloatingText(String text, int x, int y, Color color) {
        floatingTexts.add(new FloatingText(text, x, y, color));
    }

    private void endRound() {
        if (player1.health <= 0) {
            player2Score++;
            addFloatingText("Player 2 Wins Round!", WINDOW_WIDTH/2, 200, Color.RED);
        } else if (player2.health <= 0) {
            player1Score++;
            addFloatingText("Player 1 Wins Round!", WINDOW_WIDTH/2, 200, Color.BLUE);
        } else {
            // Time's up - higher health wins
            if (player1.health > player2.health) {
                player1Score++;
                addFloatingText("Player 1 Wins Round!", WINDOW_WIDTH/2, 200, Color.BLUE);
            } else if (player2.health > player1.health) {
                player2Score++;
                addFloatingText("Player 2 Wins Round!", WINDOW_WIDTH/2, 200, Color.RED);
            } else {
                addFloatingText("Round Draw!", WINDOW_WIDTH/2, 200, Color.WHITE);
            }
        }

        currentRound++;

        // Check for game victory (first to 3 rounds)
        if (player1Score >= 3 || player2Score >= 3) {
            currentState = GameState.VICTORY;
        } else {
            // Prepare for next round
            Timer delay = new Timer(3000, e -> {
                resetPlayers();
                gameStartTime = System.currentTimeMillis();
                ((Timer)e.getSource()).stop();
            });
            delay.setRepeats(false);
            delay.start();
        }
    }

    private void render(Graphics2D g) {
        // Apply screen shake
        if (screenShake) {
            int shakeX = random.nextInt(shakeIntensity * 2) - shakeIntensity;
            int shakeY = random.nextInt(shakeIntensity * 2) - shakeIntensity;
            g.translate(shakeX, shakeY);
        }

        // Enable antialiasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (currentState) {
            case MENU:
                renderMenu(g);
                break;
            case PLAYING:
                renderGame(g);
                break;
            case PAUSED:
                renderGame(g);
                renderPauseOverlay(g);
                break;
            case GAME_OVER:
            case VICTORY:
                renderGame(g);
                renderVictoryScreen(g);
                break;
        }

        // Render particles and floating texts
        renderParticles(g);
        renderFloatingTexts(g);
    }

    private void renderMenu(Graphics2D g) {
        // Background gradient
        GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 30, 60),
                0, WINDOW_HEIGHT, new Color(60, 30, 100));
        g.setPaint(gradient);
        g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Title
        g.setFont(titleFont);
        g.setColor(Color.WHITE);
        String title = "FENCING CHAMPIONSHIP";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (WINDOW_WIDTH - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 150);

        // Subtitle with animation
        g.setFont(gameFont);
        g.setColor(new Color(200, 200, 200, 150 + (int)(50 * Math.sin(animationFrame * 0.1))));
        String subtitle = "Press SPACE to Start";
        fm = g.getFontMetrics();
        int subtitleX = (WINDOW_WIDTH - fm.stringWidth(subtitle)) / 2;
        g.drawString(subtitle, subtitleX, 400);

        // Controls
        g.setFont(smallFont);
        g.setColor(Color.LIGHT_GRAY);
        String[] controls = {
                "Player 1: WASD to move, F to attack, G to defend",
                "Player 2: Arrow keys to move, K to attack, L to defend",
                "First to 3 rounds wins!"
        };

        for (int i = 0; i < controls.length; i++) {
            fm = g.getFontMetrics();
            int controlX = (WINDOW_WIDTH - fm.stringWidth(controls[i])) / 2;
            g.drawString(controls[i], controlX, 500 + i * 25);
        }
    }

    private void renderGame(Graphics2D g) {
        // Background
        GradientPaint gradient = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, WINDOW_HEIGHT, new Color(255, 165, 0));
        g.setPaint(gradient);
        g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Ground
        g.setColor(new Color(34, 139, 34));
        g.fillRect(0, GROUND_Y, WINDOW_WIDTH, WINDOW_HEIGHT - GROUND_Y);

        // Fencing strip
        g.setColor(Color.WHITE);
        g.fillRect(100, GROUND_Y - 5, WINDOW_WIDTH - 200, 10);

        // Render players
        renderFencer(g, player1);
        renderFencer(g, player2);

        // UI
        renderUI(g);
    }

    private void renderFencer(Graphics2D g, Fencer fencer) {
        AffineTransform original = g.getTransform();

        // Fencer body
        g.setColor(fencer.color);
        g.fillRoundRect(fencer.x, fencer.y, FENCER_WIDTH, FENCER_HEIGHT, 10, 10);

        // Fencer details
        g.setColor(Color.WHITE);
        g.fillOval(fencer.x + 15, fencer.y + 10, 30, 30); // Head

        // Sword
        g.setColor(Color.LIGHT_GRAY);
        Rectangle swordBounds = fencer.getSwordBounds();
        g.fillRect(swordBounds.x, swordBounds.y, swordBounds.width, swordBounds.height);

        // Sword guard
        g.setColor(Color.red);
        int guardX = fencer.facingLeft ? swordBounds.x + swordBounds.width - 5 : swordBounds.x;
        g.fillRect(guardX, swordBounds.y - 5, 10, swordBounds.height + 10);

        // Health bar
        int healthBarWidth = FENCER_WIDTH;
        int healthBarHeight = 8;
        int healthBarX = fencer.x;
        int healthBarY = fencer.y - 15;

        // Background
        g.setColor(Color.RED);
        g.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

        // Health
        g.setColor(Color.GREEN);
        int healthWidth = (int)((double)fencer.health / MAX_HEALTH * healthBarWidth);
        g.fillRect(healthBarX, healthBarY, healthWidth, healthBarHeight);

        // Border
        g.setColor(Color.BLACK);
        g.drawRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

        // Attack animation
        if (fencer.isAttacking()) {
            g.setColor(new Color(255, 255, 0, 100));
            g.fillOval(swordBounds.x - 10, swordBounds.y - 10,
                    swordBounds.width + 20, swordBounds.height + 20);
        }

        // Defense animation
        if (fencer.isDefending()) {
            g.setColor(new Color(0, 0, 255, 100));
            g.fillOval(fencer.x - 20, fencer.y - 20,
                    FENCER_WIDTH + 40, FENCER_HEIGHT + 40);
        }

        g.setTransform(original);
    }

    private void renderUI(Graphics2D g) {
        g.setFont(gameFont);
        g.setColor(Color.WHITE);

        // Scores
        g.drawString("Player 1: " + player1Score, 50, 50);
        g.drawString("Player 2: " + player2Score, WINDOW_WIDTH - 200, 50);

        // Round
        String roundText = "Round " + currentRound;
        FontMetrics fm = g.getFontMetrics();
        int roundX = (WINDOW_WIDTH - fm.stringWidth(roundText)) / 2;
        g.drawString(roundText, roundX, 50);

        // Timer
        String timeText = "Time: " + Math.max(0, roundTimeLeft);
        int timeX = (WINDOW_WIDTH - fm.stringWidth(timeText)) / 2;
        g.drawString(timeText, timeX, 100);

        // Timer bar
        int timerBarWidth = 200;
        int timerBarHeight = 20;
        int timerBarX = (WINDOW_WIDTH - timerBarWidth) / 2;
        int timerBarY = 120;

        g.setColor(Color.RED);
        g.fillRect(timerBarX, timerBarY, timerBarWidth, timerBarHeight);

        g.setColor(Color.GREEN);
        int timeWidth = (int)((double)Math.max(0, roundTimeLeft) / ROUND_TIME * timerBarWidth);
        g.fillRect(timerBarX, timerBarY, timeWidth, timerBarHeight);

        g.setColor(Color.BLACK);
        g.drawRect(timerBarX, timerBarY, timerBarWidth, timerBarHeight);
    }

    private void renderPauseOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        g.setFont(titleFont);
        g.setColor(Color.WHITE);
        String pauseText = "PAUSED";
        FontMetrics fm = g.getFontMetrics();
        int pauseX = (WINDOW_WIDTH - fm.stringWidth(pauseText)) / 2;
        g.drawString(pauseText, pauseX, WINDOW_HEIGHT/2);

        g.setFont(gameFont);
        String resumeText = "Press P to Resume";
        fm = g.getFontMetrics();
        int resumeX = (WINDOW_WIDTH - fm.stringWidth(resumeText)) / 2;
        g.drawString(resumeText, resumeX, WINDOW_HEIGHT/2 + 60);
    }

    private void renderVictoryScreen(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        g.setFont(titleFont);
        String winner = player1Score >= 3 ? "PLAYER 1 WINS!" : "PLAYER 2 WINS!";
        Color winnerColor = player1Score >= 3 ? Color.BLUE : Color.RED;
        g.setColor(winnerColor);
        FontMetrics fm = g.getFontMetrics();
        int winnerX = (WINDOW_WIDTH - fm.stringWidth(winner)) / 2;
        g.drawString(winner, winnerX, WINDOW_HEIGHT/2 - 50);

        g.setFont(gameFont);
        g.setColor(Color.WHITE);
        String scoreText = "Final Score: " + player1Score + " - " + player2Score;
        fm = g.getFontMetrics();
        int scoreX = (WINDOW_WIDTH - fm.stringWidth(scoreText)) / 2;
        g.drawString(scoreText, scoreX, WINDOW_HEIGHT/2 + 20);

        String restartText = "Press R to Play Again or ESC to Menu";
        fm = g.getFontMetrics();
        int restartX = (WINDOW_WIDTH - fm.stringWidth(restartText)) / 2;
        g.drawString(restartText, restartX, WINDOW_HEIGHT/2 + 80);
    }

    private void renderParticles(Graphics2D g) {
        for (ParticleEffect particle : particles) {
            particle.render(g);
        }
    }

    private void renderFloatingTexts(Graphics2D g) {
        for (FloatingText text : floatingTexts) {
            text.render(g);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        keysPressed[keyCode] = true;

        switch (currentState) {
            case MENU:
                if (keyCode == KeyEvent.VK_SPACE) {
                    startNewGame();
                }
                break;
            case PLAYING:
                if (keyCode == KeyEvent.VK_P) {
                    currentState = GameState.PAUSED;
                }
                break;
            case PAUSED:
                if (keyCode == KeyEvent.VK_P) {
                    currentState = GameState.PLAYING;
                }
                break;
            case VICTORY:
                if (keyCode == KeyEvent.VK_R) {
                    startNewGame();
                } else if (keyCode == KeyEvent.VK_ESCAPE) {
                    currentState = GameState.MENU;
                }
                break;
        }

        if (keyCode == KeyEvent.VK_ESCAPE && currentState != GameState.MENU) {
            currentState = GameState.MENU;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keysPressed[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    private void startNewGame() {
        currentState = GameState.PLAYING;
        gameStartTime = System.currentTimeMillis();
        player1Score = 0;
        player2Score = 0;
        currentRound = 1;
        resetPlayers();
        particles.clear();
        floatingTexts.clear();
    }

    // Inner classes for game objects
    private class Fencer {
        int x, y;
        int health;
        Color color;
        boolean facingLeft;
        boolean isJumping;
        int velocityY;
        int attackTimer;
        int defenseTimer;
        int knockbackTimer;
        int knockbackDirection;

        public Fencer(int x, int y, Color color, boolean facingLeft) {
            this.x = x;
            this.y = y;
            this.health = MAX_HEALTH;
            this.color = color;
            this.facingLeft = facingLeft;
            this.velocityY = 0;
        }

        public void update() {
            // Gravity and jumping
            if (isJumping) {
                velocityY += 1; // Gravity
                y += velocityY;

                if (y >= GROUND_Y - FENCER_HEIGHT) {
                    y = GROUND_Y - FENCER_HEIGHT;
                    isJumping = false;
                    velocityY = 0;
                }
            }

            // Update timers
            if (attackTimer > 0) attackTimer--;
            if (defenseTimer > 0) defenseTimer--;
            if (knockbackTimer > 0) {
                knockbackTimer--;
                x += knockbackDirection * 3;
                // Keep within bounds
                x = Math.max(50, Math.min(WINDOW_WIDTH - FENCER_WIDTH - 50, x));
            }
        }

        public void moveLeft() {
            if (knockbackTimer > 0) return;
            x -= 5;
            facingLeft = true;
            x = Math.max(50, x);
        }

        public void moveRight() {
            if (knockbackTimer > 0) return;
            x += 5;
            facingLeft = false;
            x = Math.min(WINDOW_WIDTH - FENCER_WIDTH - 50, x);
        }

        public void jump() {
            if (!isJumping) {
                isJumping = true;
                velocityY = -15;
            }
        }

        public void attack() {
            if (attackTimer == 0 && defenseTimer == 0) {
                attackTimer = 20;
            }
        }

        public void defend() {
            if (attackTimer == 0) {
                defenseTimer = 30;
            }
        }

        public void takeDamage(int damage) {
            if (defenseTimer == 0) {
                health -= damage;
                health = Math.max(0, health);
            }
        }

        public void knockback(boolean fromLeft) {
            knockbackTimer = 15;
            knockbackDirection = fromLeft ? 1 : -1;
        }

        public boolean isAttacking() {
            return attackTimer > 0;
        }

        public boolean isDefending() {
            return defenseTimer > 0;
        }

        public void finishAttack() {
            attackTimer = 0;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, FENCER_WIDTH, FENCER_HEIGHT);
        }

        public Rectangle getSwordBounds() {
            if (facingLeft) {
                return new Rectangle(x - SWORD_LENGTH, y + FENCER_HEIGHT/2 - 5, SWORD_LENGTH, 10);
            } else {
                return new Rectangle(x + FENCER_WIDTH, y + FENCER_HEIGHT/2 - 5, SWORD_LENGTH, 10);
            }
        }
    }

    private enum ParticleType {
        SPARK, SNOW, CONFETTI
    }

    private class ParticleEffect {
        float x, y;
        float velocityX, velocityY;
        Color color;
        int life;
        int maxLife;
        ParticleType type;

        public ParticleEffect(int x, int y, Color color, ParticleType type) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.type = type;

            switch (type) {
                case SPARK:
                    this.velocityX = (random.nextFloat() - 0.5f) * 10;
                    this.velocityY = (random.nextFloat() - 0.5f) * 10;
                    this.maxLife = 30;
                    break;
                case SNOW:
                    this.velocityX = (random.nextFloat() - 0.5f) * 2;
                    this.velocityY = random.nextFloat() * 3 + 1;
                    this.maxLife = 200;
                    break;
                case CONFETTI:
                    this.velocityX = (random.nextFloat() - 0.5f) * 8;
                    this.velocityY = random.nextFloat() * -5 - 2;
                    this.maxLife = 100;
                    break;
            }
            this.life = maxLife;
        }

        public void update() {
            x += velocityX;
            y += velocityY;

            if (type == ParticleType.CONFETTI) {
                velocityY += 0.3f; // Gravity
            }

            life--;
        }

        public void render(Graphics2D g) {
            int alpha = (int)(255.0f * life / maxLife);
            Color renderColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            g.setColor(renderColor);

            switch (type) {
                case SPARK:
                    g.fillOval((int)x - 2, (int)y - 2, 4, 4);
                    break;
                case SNOW:
                    g.fillOval((int)x - 1, (int)y - 1, 3, 3);
                    break;
                case CONFETTI:
                    g.fillRect((int)x - 2, (int)y - 2, 4, 4);
                    break;
            }
        }

        public boolean isDead() {
            return life <= 0 || y > WINDOW_HEIGHT;
        }
    }

    private class FloatingText {
        String text;
        float x, y;
        float velocityY;
        Color color;
        int life;
        int maxLife;

        public FloatingText(String text, int x, int y, Color color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.velocityY = -2;
            this.maxLife = 120;
            this.life = maxLife;
        }

        public void update() {
            y += velocityY;
            life--;
        }

        public void render(Graphics2D g) {
            int alpha = (int)(255.0f * life / maxLife);
            Color renderColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            g.setColor(renderColor);
            g.setFont(gameFont);

            FontMetrics fm = g.getFontMetrics();
            int textX = (int)x - fm.stringWidth(text) / 2;
            g.drawString(text, textX, (int)y);
        }

        public boolean isDead() {
            return life <= 0;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new FencingChampionship().setVisible(true);
        });
    }
}