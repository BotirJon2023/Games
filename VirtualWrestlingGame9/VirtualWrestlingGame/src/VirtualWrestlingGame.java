import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class VirtualWrestlingGame extends JFrame implements Runnable {
    // Game constants
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int DELAY = 17; // ~60 FPS
    private static final String TITLE = "Virtual Wrestling Game";

    // Game variables
    private boolean isRunning;
    private Thread gameThread;
    private BufferedImage buffer;
    private Graphics2D bufferGraphics;
    private int fps;

    // Game state
    private GameState gameState;

    // Game objects
    private Arena arena;
    private Wrestler player1;
    private Wrestler player2;
    private InputHandler inputHandler;
    private SoundManager soundManager;
    private ParticleSystem particleSystem;
    private Camera camera;
    private HUD hud;
    private Menu menu;

    private enum GameState {
        MENU, GAME, PAUSE, GAME_OVER
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VirtualWrestlingGame game = new VirtualWrestlingGame();
            game.setVisible(true);
            game.start();
        });
    }

    public VirtualWrestlingGame() {
        setTitle(TITLE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        buffer = new BufferedImage(WINDOW_WIDTH, WINDOW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        bufferGraphics = buffer.createGraphics();
        bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        arena = new Arena();
        player1 = new Wrestler("Crusher", 200, 400, true);
        player2 = new Wrestler("Smasher", 500, 400, false);
        inputHandler = new InputHandler();
        soundManager = new SoundManager();
        particleSystem = new ParticleSystem();
        camera = new Camera(0, 0);
        hud = new HUD();
        menu = new Menu();

        gameState = GameState.MENU;

        addKeyListener(inputHandler);
        setFocusable(true);
        requestFocus();
    }

    public void start() {
        if (gameThread == null || !isRunning) {
            isRunning = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    public void stopGameThread() {
        if (gameThread != null && isRunning) {
            isRunning = false;
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000.0 / amountOfTicks;
        double delta = 0;
        long timer = System.currentTimeMillis();
        int frames = 0;

        soundManager.playSound("start");

        while (isRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;

            while (delta >= 1) {
                update();
                delta--;
            }

            render();
            frames++;

            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                fps = frames;
                frames = 0;
            }

            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        stopGameThread();
    }

    private void update() {
        switch (gameState) {
            case MENU:
                menu.update(inputHandler);
                if (menu.isStartSelected()) {
                    gameState = GameState.GAME;
                    soundManager.playSound("bell");
                }
                break;

            case GAME:
                player1.update(inputHandler, player2);
                player2.update(inputHandler, player1);
                arena.update();
                camera.update(player1, player2);
                particleSystem.update();

                checkCollisions();

                if (player1.getHealth() <= 0 || player2.getHealth() <= 0) {
                    gameState = GameState.GAME_OVER;
                    soundManager.playSound("end");
                }

                if (inputHandler.isPausePressed()) {
                    gameState = GameState.PAUSE;
                    soundManager.playSound("pause");
                }
                break;

            case PAUSE:
                if (inputHandler.isPausePressed()) {
                    gameState = GameState.GAME;
                    soundManager.playSound("resume");
                }
                break;

            case GAME_OVER:
                if (inputHandler.isRestartPressed()) {
                    resetGame();
                    gameState = GameState.MENU;
                }
                break;
        }

        inputHandler.update();
    }

    private void render() {
        bufferGraphics.setColor(Color.BLACK);
        bufferGraphics.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        switch (gameState) {
            case MENU:
                menu.render(bufferGraphics);
                break;

            case GAME:
            case PAUSE: {
                AffineTransform at = bufferGraphics.getTransform();
                bufferGraphics.translate(-camera.getX(), -camera.getY());

                arena.render(bufferGraphics);
                player1.render(bufferGraphics);
                player2.render(bufferGraphics);
                particleSystem.render(bufferGraphics);

                bufferGraphics.setTransform(at);

                hud.render(bufferGraphics, player1, player2, fps);

                if (gameState == GameState.PAUSE) {
                    bufferGraphics.setColor(new Color(0, 0, 0, 150));
                    bufferGraphics.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
                    bufferGraphics.setColor(Color.WHITE);
                    bufferGraphics.setFont(new Font("Arial", Font.BOLD, 36));
                    String pauseText = "PAUSED";
                    int textWidth = bufferGraphics.getFontMetrics().stringWidth(pauseText);
                    bufferGraphics.drawString(pauseText, (WINDOW_WIDTH - textWidth) / 2, WINDOW_HEIGHT / 2);
                }
                break;
            }

            case GAME_OVER: {
                bufferGraphics.setColor(Color.BLACK);
                bufferGraphics.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
                bufferGraphics.setColor(Color.RED);
                bufferGraphics.setFont(new Font("Arial", Font.BOLD, 48));

                String gameOverText = "GAME OVER";
                int gameOverWidth = bufferGraphics.getFontMetrics().stringWidth(gameOverText);
                bufferGraphics.drawString(gameOverText, (WINDOW_WIDTH - gameOverWidth) / 2, WINDOW_HEIGHT / 3);

                String winnerText = (player1.getHealth() <= 0) ? player2.getName() + " WINS!" : player1.getName() + " WINS!";
                int winnerWidth = bufferGraphics.getFontMetrics().stringWidth(winnerText);
                bufferGraphics.drawString(winnerText, (WINDOW_WIDTH - winnerWidth) / 2, WINDOW_HEIGHT / 2);

                bufferGraphics.setFont(new Font("Arial", Font.PLAIN, 24));
                String restartText = "Press R to restart";
                int restartWidth = bufferGraphics.getFontMetrics().stringWidth(restartText);
                bufferGraphics.drawString(restartText, (WINDOW_WIDTH - restartWidth) / 2, 2 * WINDOW_HEIGHT / 3);
                break;
            }
        }

        Graphics g = getGraphics();
        if (g != null) {
            g.drawImage(buffer, 0, 0, null);
            g.dispose();
        }
    }

    private void checkCollisions() {
        if (player1.isAttacking() && player1.getAttackBounds().intersects(player2.getBounds())) {
            player2.takeDamage(player1.getAttackDamage(), player1.getX() < player2.getX() ? 1 : -1);
            particleSystem.addParticles(player2.getX(), player2.getY() - player2.height / 2.0, 10);
            soundManager.playSound("hit");
        }

        if (player2.isAttacking() && player2.getAttackBounds().intersects(player1.getBounds())) {
            player1.takeDamage(player2.getAttackDamage(), player2.getX() < player1.getX() ? 1 : -1);
            particleSystem.addParticles(player1.getX(), player1.getY() - player1.height / 2.0, 10);
            soundManager.playSound("hit");
        }

        if (!arena.getBounds().contains(player1.getBounds())) {
            player1.keepInBounds(arena.getBounds());
        }

        if (!arena.getBounds().contains(player2.getBounds())) {
            player2.keepInBounds(arena.getBounds());
        }
    }

    private void resetGame() {
        player1.reset(200, arena.getFloorY());
        player2.reset(500, arena.getFloorY());
        camera.reset();
        particleSystem.clear();
    }

    private class Arena {
        private static final int WIDTH = 1000;
        private static final int HEIGHT = 600;
        private static final int FLOOR_Y = 450;

        private Rectangle bounds;
        private Color floorColor;
        private Color ropeColor;
        private Color matColor;

        public Arena() {
            bounds = new Rectangle(0, 0, WIDTH, HEIGHT);
            floorColor = new Color(150, 75, 0);
            ropeColor = new Color(255, 0, 0);
            matColor = new Color(0, 0, 150);
        }

        public void update() {
        }

        public void render(Graphics2D g) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setColor(matColor);
            g.fillRect(100, 100, WIDTH - 200, FLOOR_Y - 100);

            g.setColor(ropeColor);
            g.setStroke(new BasicStroke(5));
            g.drawRect(100, 100, WIDTH - 200, FLOOR_Y - 100);
            g.drawLine(100, 200, WIDTH - 100, 200);

            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(95, 95, 10, 10);
            g.fillRect(WIDTH - 105, 95, 10, 10);
            g.fillRect(95, FLOOR_Y - 5, 10, 10);
            g.fillRect(WIDTH - 105, FLOOR_Y - 5, 10, 10);

            g.setColor(floorColor);
            g.fillRect(0, FLOOR_Y, WIDTH, HEIGHT - FLOOR_Y);
        }

        public Rectangle getBounds() {
            return bounds;
        }

        public int getFloorY() {
            return FLOOR_Y;
        }

        public int getWidth() {
            return WIDTH;
        }

        public int getHeight() {
            return HEIGHT;
        }
    }

    private class Wrestler {
        private String name;
        private double x, y;
        private double velocityX, velocityY;
        private int width, height;
        private int health;
        private int stamina;
        private boolean isPlayer1;
        private boolean isAttacking;
        private boolean isBlocking;
        private boolean isJumping;
        private boolean isFalling;
        private int attackDamage;
        private int attackCooldown;
        private int currentFrame;
        private int animationDelay;
        private WrestlerState state;

        private Rectangle[] idleFrames;
        private Rectangle[] walkFrames;
        private Rectangle[] attackFrames;
        private Rectangle[] blockFrames;
        private Rectangle[] jumpFrames;
        private Rectangle[] fallFrames;
        private Rectangle[] hitFrames;

        private enum WrestlerState {
            IDLE, WALKING, ATTACKING, BLOCKING, JUMPING, FALLING, HIT
        }

        public Wrestler(String name, int x, int y, boolean isPlayer1) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.isPlayer1 = isPlayer1;

            width = 80;
            height = 150;
            health = 100;
            stamina = 100;
            velocityX = 0;
            velocityY = 0;
            isAttacking = false;
            isBlocking = false;
            isJumping = false;
            isFalling = false;
            attackDamage = 10;
            attackCooldown = 0;
            currentFrame = 0;
            animationDelay = 0;
            state = WrestlerState.IDLE;

            initializeAnimationFrames();
        }

        private void initializeAnimationFrames() {
            idleFrames = new Rectangle[4];
            walkFrames = new Rectangle[6];
            attackFrames = new Rectangle[4];
            blockFrames = new Rectangle[2];
            jumpFrames = new Rectangle[4];
            fallFrames = new Rectangle[4];
            hitFrames = new Rectangle[3];

            for (int i = 0; i < idleFrames.length; i++) idleFrames[i] = new Rectangle(0, 0, width, height);
            for (int i = 0; i < walkFrames.length; i++) walkFrames[i] = new Rectangle(0, 0, width, height);
            for (int i = 0; i < attackFrames.length; i++) attackFrames[i] = new Rectangle(0, 0, width, height + 20);
            for (int i = 0; i < blockFrames.length; i++) blockFrames[i] = new Rectangle(0, 0, width, height);
            for (int i = 0; i < jumpFrames.length; i++) jumpFrames[i] = new Rectangle(0, 0, width, height - 20);
            for (int i = 0; i < fallFrames.length; i++) fallFrames[i] = new Rectangle(0, 0, width, height);
            for (int i = 0; i < hitFrames.length; i++) hitFrames[i] = new Rectangle(0, 0, width, height);
        }

        public void update(InputHandler input, Wrestler opponent) {
            animationDelay++;
            if (animationDelay >= 5) {
                currentFrame = (currentFrame + 1) % getCurrentAnimationFrames().length;
                animationDelay = 0;
            }

            if (attackCooldown > 0) {
                attackCooldown--;
            }

            if (isPlayer1) {
                handlePlayer1Input(input);
            } else {
                handleAI(opponent);
            }

            applyPhysics();

            if (stamina < 100 && !isAttacking) {
                stamina++;
            }
        }

        private void handlePlayer1Input(InputHandler input) {
            velocityX = 0;

            if (state != WrestlerState.HIT) {
                if (input.isLeftPressed() && !isBlocking) {
                    velocityX = -5;
                    if (!isJumping && !isFalling) state = WrestlerState.WALKING;
                } else if (input.isRightPressed() && !isBlocking) {
                    velocityX = 5;
                    if (!isJumping && !isFalling) state = WrestlerState.WALKING;
                } else if (!isJumping && !isFalling && !isAttacking && !isBlocking) {
                    state = WrestlerState.IDLE;
                }

                if (input.isUpPressed() && !isJumping && !isFalling) {
                    isJumping = true;
                    velocityY = -15;
                    state = WrestlerState.JUMPING;
                }

                if (input.isPunchPressed() && !isAttacking && attackCooldown == 0 && stamina >= 10) {
                    isAttacking = true;
                    attackCooldown = 20;
                    stamina -= 10;
                    state = WrestlerState.ATTACKING;
                    currentFrame = 0;
                }

                isBlocking = input.isBlockPressed();
                if (isBlocking && !isJumping && !isFalling && !isAttacking) {
                    state = WrestlerState.BLOCKING;
                }
            }
        }

        private void handleAI(Wrestler opponent) {
            velocityX = 0;

            if (state != WrestlerState.HIT) {
                double distance = Math.abs(x - opponent.getX());

                if (distance > 200) {
                    if (x < opponent.getX()) velocityX = 3; else velocityX = -3;
                    if (!isJumping && !isFalling) state = WrestlerState.WALKING;

                    if (Math.random() < 0.01 && !isJumping && !isFalling) {
                        isJumping = true;
                        velocityY = -15;
                        state = WrestlerState.JUMPING;
                    }
                } else if (distance <= 100) {
                    if (!isAttacking && attackCooldown == 0 && stamina >= 10 && Math.random() < 0.15) {
                        isAttacking = true;
                        attackCooldown = 20;
                        stamina -= 10;
                        state = WrestlerState.ATTACKING;
                        currentFrame = 0;
                    }

                    isBlocking = Math.random() < 0.05;
                    if (isBlocking && !isJumping && !isFalling && !isAttacking) {
                        state = WrestlerState.BLOCKING;
                    } else if (!isJumping && !isFalling && !isAttacking) {
                        state = WrestlerState.IDLE;
                    }
                } else {
                    if (Math.random() < 0.7) {
                        if (x < opponent.getX()) velocityX = 3; else velocityX = -3;
                        if (!isJumping && !isFalling) state = WrestlerState.WALKING;
                    } else if (!isJumping && !isFalling) {
                        state = WrestlerState.IDLE;
                    }
                }
            }
        }

        private void applyPhysics() {
            if (isJumping || isFalling) {
                velocityY += 0.8;
                if (velocityY > 0) {
                    isJumping = false;
                    isFalling = true;
                    state = WrestlerState.FALLING;
                }
            }

            x += velocityX;
            y += velocityY;

            int floorY = arena.getFloorY();
            if (y >= floorY) {
                y = floorY;
                velocityY = 0;
                if (isFalling) {
                    isFalling = false;
                    if (Math.abs(velocityX) > 0.01) state = WrestlerState.WALKING;
                    else if (!isAttacking && !isBlocking) state = WrestlerState.IDLE;
                }
            }

            if (isAttacking && currentFrame == attackFrames.length - 1) {
                isAttacking = false;
                if (!isJumping && !isFalling && !isBlocking) state = WrestlerState.IDLE;
            }

            if (state == WrestlerState.HIT && currentFrame == hitFrames.length - 1) {
                if (!isJumping && !isFalling) state = WrestlerState.IDLE;
            }
        }

        public void render(Graphics2D g) {
            Rectangle frame = getCurrentAnimationFrames()[currentFrame];

            if (isPlayer1) g.setColor(new Color(255, 0, 0, 200));
            else g.setColor(new Color(0, 0, 255, 200));

            g.fillRect((int) x - frame.width / 2, (int) y - frame.height, frame.width, frame.height);

            switch (state) {
                case IDLE: renderIdle(g); break;
                case WALKING: renderWalking(g); break;
                case ATTACKING: renderAttacking(g); break;
                case BLOCKING: renderBlocking(g); break;
                case JUMPING:
                case FALLING: renderJumping(g); break;
                case HIT: renderHit(g); break;
            }

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics metrics = g.getFontMetrics();
            int nameWidth = metrics.stringWidth(name);
            g.drawString(name, (int) x - nameWidth / 2, (int) y - height - 10);

            int healthBarWidth = 60;
            int healthBarHeight = 8;
            g.setColor(Color.BLACK);
            g.fillRect((int) x - healthBarWidth / 2, (int) y - height - 25, healthBarWidth, healthBarHeight);
            int r = Math.min(255, Math.max(0, 255 - health * 2));
            int gcol = Math.min(255, Math.max(0, health * 2));
            g.setColor(new Color(r, gcol, 0));
            g.fillRect((int) x - healthBarWidth / 2, (int) y - height - 25, Math.max(0, healthBarWidth * health / 100), healthBarHeight);
        }

        private void renderIdle(Graphics2D g) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillOval((int) x - 15, (int) y - height, 30, 30);

            g.setColor(Color.BLACK);
            g.fillOval((int) x - 10, (int) y - height + 10, 5, 5);
            g.fillOval((int) x + 5, (int) y - height + 10, 5, 5);

            g.drawLine((int) x - 5, (int) y - height + 20, (int) x + 5, (int) y - height + 20);
        }

        private void renderWalking(Graphics2D g) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillOval((int) x - 15, (int) y - height + (currentFrame % 2) * 5, 30, 30);

            g.setColor(Color.BLACK);
            g.fillOval((int) x - 10, (int) y - height + 10 + (currentFrame % 2) * 5, 5, 5);
            g.fillOval((int) x + 5, (int) y - height + 10 + (currentFrame % 2) * 5, 5, 5);

            g.drawLine((int) x - 5, (int) y - height + 20 + (currentFrame % 2) * 5,
                    (int) x + 5, (int) y - height + 20 + (currentFrame % 2) * 5);

            int legOffset = (currentFrame % 3) * 10 - 10;
            g.drawLine((int) x - 20, (int) y - height + 80, (int) x - 30, (int) y - height + 120 + legOffset);
            g.drawLine((int) x + 20, (int) y - height + 80, (int) x + 30, (int) y - height + 120 - legOffset);
        }

        private void renderAttacking(Graphics2D g) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillOval((int) x - 15, (int) y - height, 30, 30);

            g.setColor(Color.BLACK);
            g.fillOval((int) x - 10, (int) y - height + 10, 6, 6);
            g.fillOval((int) x + 5, (int) y - height + 10, 6, 6);

            g.drawLine((int) x - 5, (int) y - height + 22, (int) x + 5, (int) y - height + 22);

            int dir = isPlayer1 ? 1 : -1;
            int armExtension;
            if (currentFrame <= 2) armExtension = currentFrame * 15;
            else armExtension = (4 - currentFrame) * 15;

            g.setColor(isPlayer1 ? new Color(255, 0, 0, 200) : new Color(0, 0, 255, 200));
            int armX = (int) x + dir * (width / 2);
            int armY = (int) y - height + 40;
            g.fillRect(armX, armY, dir * armExtension, 20);

            g.setColor(Color.LIGHT_GRAY);
            int fistCenterX = armX + dir * armExtension;
            int fistCenterY = armY + 10;
            g.fillOval(fistCenterX - 10, fistCenterY - 10, 20, 20);
        }

        private void renderBlocking(Graphics2D g) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillOval((int) x - 15, (int) y - height, 30, 30);

            g.setColor(Color.BLACK);
            g.drawLine((int) x - 8, (int) y - height + 18, (int) x + 8, (int) y - height + 18);

            g.setColor(new Color(230, 230, 230));
            g.fillRect((int) x - width / 2, (int) y - height + 35, width, 15);
        }

        private void renderJumping(Graphics2D g) {
            g.setColor(Color.LIGHT_GRAY);
            int bob = (state == WrestlerState.JUMPING) ? -5 : 5;
            g.fillOval((int) x - 15, (int) y - height + bob, 30, 30);
        }

        private void renderHit(Graphics2D g) {
            g.setColor(new Color(255, 255, 0, 180));
            g.fillOval((int) x - width / 2 - 5, (int) y - height - 5, width + 10, height + 10);

            g.setColor(Color.LIGHT_GRAY);
            g.fillOval((int) x - 15, (int) y - height, 30, 30);
        }

        private Rectangle[] getCurrentAnimationFrames() {
            switch (state) {
                case IDLE: return idleFrames;
                case WALKING: return walkFrames;
                case ATTACKING: return attackFrames;
                case BLOCKING: return blockFrames;
                case JUMPING: return jumpFrames;
                case FALLING: return fallFrames;
                case HIT: return hitFrames;
                default: return idleFrames;
            }
        }

        public boolean isAttacking() {
            return isAttacking;
        }

        public Rectangle getBounds() {
            Rectangle frame = getCurrentAnimationFrames()[currentFrame];
            return new Rectangle((int) x - frame.width / 2, (int) y - frame.height, frame.width, frame.height);
        }

        public Rectangle getAttackBounds() {
            if (!isAttacking) return new Rectangle(0, 0, 0, 0);
            int dir = isPlayer1 ? 1 : -1;
            int armExtension = Math.min(45, currentFrame * 15);
            int armY = (int) y - height + 40;
            int armX = (int) x + dir * (width / 2);
            int minX = Math.min(armX, armX + dir * armExtension);
            int w = Math.abs(dir * armExtension) + 20; // include fist
            return new Rectangle(minX, armY, w, 20);
        }

        public int getAttackDamage() {
            return attackDamage;
        }

        public int getHealth() {
            return health;
        }

        public String getName() {
            return name;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public void keepInBounds(Rectangle bounds) {
            int halfW = width / 2;
            if (x - halfW < bounds.x) x = bounds.x + halfW;
            if (x + halfW > bounds.x + bounds.width) x = bounds.x + bounds.width - halfW;

            int floorY = arena.getFloorY();
            if (y > floorY) {
                y = floorY;
                velocityY = 0;
                isFalling = false;
            }
            if (y - height < bounds.y) {
                y = bounds.y + height;
                velocityY = 0.5;
                isFalling = true;
            }
        }

        public void takeDamage(int dmg, int knockbackDir) {
            int damageApplied = dmg;
            if (isBlocking) {
                damageApplied = Math.max(1, dmg / 3);
                stamina = Math.max(0, stamina - 5);
            }
            health = Math.max(0, health - damageApplied);
            state = WrestlerState.HIT;
            currentFrame = 0;

            velocityX += 2.0 * knockbackDir;
            if (Math.random() < 0.2 && y >= arena.getFloorY() - 1) {
                velocityY = -8;
                isJumping = true;
            }
        }

        public void reset(int startX, int startY) {
            this.x = startX;
            this.y = startY;
            this.velocityX = 0;
            this.velocityY = 0;
            this.health = 100;
            this.stamina = 100;
            this.isAttacking = false;
            this.isBlocking = false;
            this.isJumping = false;
            this.isFalling = false;
            this.attackCooldown = 0;
            this.currentFrame = 0;
            this.state = WrestlerState.IDLE;
        }
    }

    private static class InputHandler implements KeyListener {
        private boolean left, right, up, block, punch;
        private boolean pausePressedOnce, restartPressedOnce, startPressedOnce;

        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            switch (code) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A:
                    left = true; break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D:
                    right = true; break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W:
                    up = true; break;
                case KeyEvent.VK_SHIFT:
                case KeyEvent.VK_K:
                    block = true; break;
                case KeyEvent.VK_SPACE:
                case KeyEvent.VK_J:
                    punch = true; break;
                case KeyEvent.VK_P:
                case KeyEvent.VK_ESCAPE:
                    pausePressedOnce = true; break;
                case KeyEvent.VK_R:
                    restartPressedOnce = true; break;
                case KeyEvent.VK_ENTER:
                    startPressedOnce = true; break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();
            switch (code) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A:
                    left = false; break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D:
                    right = false; break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W:
                    up = false; break;
                case KeyEvent.VK_SHIFT:
                case KeyEvent.VK_K:
                    block = false; break;
                case KeyEvent.VK_SPACE:
                case KeyEvent.VK_J:
                    punch = false; break;
            }
        }

        public void update() {
            // Reset one-shot events each tick after being read by the game
            pausePressedOnce = false;
            restartPressedOnce = false;
            startPressedOnce = false;
        }

        public boolean isLeftPressed() { return left; }
        public boolean isRightPressed() { return right; }
        public boolean isUpPressed() { return up; }
        public boolean isBlockPressed() { return block; }
        public boolean isPunchPressed() { return punch; }
        public boolean isPausePressed() { return pausePressedOnce; }
        public boolean isRestartPressed() { return restartPressedOnce; }
        public boolean isStartPressed() { return startPressedOnce; }
    }

    private static class SoundManager {
        public void playSound(String name) {
            // Stub. Replace with real audio if desired.
            // System.out.println("Sound: " + name);
            if ("hit".equals(name)) {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    private static class ParticleSystem {
        private static class Particle {
            double x, y, vx, vy;
            int life, maxLife;
            Color color;

            Particle(double x, double y, double vx, double vy, int life, Color color) {
                this.x = x; this.y = y; this.vx = vx; this.vy = vy;
                this.life = 0; this.maxLife = life; this.color = color;
            }

            boolean update() {
                x += vx;
                y += vy;
                vy += 0.3;
                life++;
                return life < maxLife;
            }

            void render(Graphics2D g) {
                int alpha = (int) (255 * (1.0 - (double) life / maxLife));
                alpha = Math.max(0, Math.min(255, alpha));
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                g.fillOval((int) x - 2, (int) y - 2, 4, 4);
            }
        }

        private final List<Particle> particles = new ArrayList<>();
        private final Random rand = new Random();

        public void addParticles(double x, double y, int count) {
            for (int i = 0; i < count; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double speed = 2 + rand.nextDouble() * 2;
                double vx = Math.cos(angle) * speed;
                double vy = Math.sin(angle) * speed - 2;
                int life = 20 + rand.nextInt(20);
                Color c = new Color(255, rand.nextInt(100), rand.nextInt(100));
                particles.add(new Particle(x, y, vx, vy, life, c));
            }
        }

        public void update() {
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                if (!p.update()) it.remove();
            }
        }

        public void render(Graphics2D g) {
            for (Particle p : particles) {
                p.render(g);
            }
        }

        public void clear() {
            particles.clear();
        }
    }

    private class Camera {
        private double x, y;

        public Camera(double x, double y) {
            this.x = x; this.y = y;
        }

        public void update(Wrestler p1, Wrestler p2) {
            double targetX = (p1.getX() + p2.getX()) / 2.0 - WINDOW_WIDTH / 2.0;
            double targetY = 0;

            Rectangle ab = arena.getBounds();
            x = Math.max(ab.x, Math.min(targetX, ab.x + ab.width - WINDOW_WIDTH));
            y = Math.max(ab.y, Math.min(targetY, ab.y + ab.height - WINDOW_HEIGHT));
        }

        public int getX() { return (int) Math.round(x); }
        public int getY() { return (int) Math.round(y); }

        public void reset() {
            x = 0; y = 0;
        }
    }

    private static class HUD {
        public void render(Graphics2D g, Wrestler p1, Wrestler p2, int fps) {
            // P1
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.drawString(p1.getName(), 20, 30);
            drawBar(g, 20, 40, 300, 15, p1.getHealth(), new Color(200, 0, 0), new Color(0, 200, 0));
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            g.drawString("Stamina", 20, 70);
            drawBar(g, 80, 62, 200, 10, clamp(p1.stamina, 0, 100), new Color(50, 50, 255), new Color(50, 50, 255));

            // P2
            String name2 = p2.getName();
            int name2Width = g.getFontMetrics(new Font("Arial", Font.BOLD, 18)).stringWidth(name2);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.drawString(name2, WINDOW_WIDTH - 20 - name2Width, 30);
            drawBar(g, WINDOW_WIDTH - 320, 40, 300, 15, p2.getHealth(), new Color(200, 0, 0), new Color(0, 200, 0));
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            g.drawString("Stamina", WINDOW_WIDTH - 280, 70);
            drawBar(g, WINDOW_WIDTH - 280 + 60, 62, 200, 10, clamp(p2.stamina, 0, 100), new Color(50, 50, 255), new Color(50, 50, 255));

            // FPS
            g.setColor(Color.YELLOW);
            g.drawString("FPS: " + fps, WINDOW_WIDTH - 80, WINDOW_HEIGHT - 20);
        }

        private void drawBar(Graphics2D g, int x, int y, int w, int h, int value, Color bad, Color good) {
            g.setColor(Color.GRAY);
            g.drawRect(x, y, w, h);
            int fill = (int) (w * (value / 100.0));
            fill = Math.max(0, Math.min(w, fill));
            // Interpolate color
            float t = value / 100f;
            int r = (int) (bad.getRed() * (1 - t) + good.getRed() * t);
            int gr = (int) (bad.getGreen() * (1 - t) + good.getGreen() * t);
            int b = (int) (bad.getBlue() * (1 - t) + good.getBlue() * t);
            g.setColor(new Color(r, gr, b));
            g.fillRect(x + 1, y + 1, Math.max(0, fill - 1), h - 1);
        }

        private int clamp(int v, int min, int max) {
            return Math.max(min, Math.min(max, v));
        }
    }

    private class Menu {
        private boolean startSelected = false;

        public void update(InputHandler input) {
            if (input.isStartPressed()) {
                startSelected = true;
            }
        }

        public boolean isStartSelected() {
            boolean s = startSelected;
            startSelected = false;
            return s;
        }

        public void render(Graphics2D g) {
            g.setColor(new Color(20, 20, 20));
            g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String title = "Virtual Wrestling Game";
            int tw = g.getFontMetrics().stringWidth(title);
            g.drawString(title, (WINDOW_WIDTH - tw) / 2, WINDOW_HEIGHT / 3);

            g.setFont(new Font("Arial", Font.PLAIN, 24));
            String start = "Press Enter to Start";
            int sw = g.getFontMetrics().stringWidth(start);
            g.drawString(start, (WINDOW_WIDTH - sw) / 2, WINDOW_HEIGHT / 2);

            g.setFont(new Font("Arial", Font.PLAIN, 16));
            String controls1 = "P1: A/D to move, W to jump, J/Space to punch, K/Shift to block";
            int c1w = g.getFontMetrics().stringWidth(controls1);
            g.drawString(controls1, (WINDOW_WIDTH - c1w) / 2, WINDOW_HEIGHT / 2 + 40);

            String controls2 = "Pause: P or Esc | Restart: R";
            int c2w = g.getFontMetrics().stringWidth(controls2);
            g.drawString(controls2, (WINDOW_WIDTH - c2w) / 2, WINDOW_HEIGHT / 2 + 65);
        }
    }
}