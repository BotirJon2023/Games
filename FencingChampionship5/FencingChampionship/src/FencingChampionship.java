import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class FencingChampionship extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int GROUND_LEVEL = 500;
    private static final int FENCER_WIDTH = 40;
    private static final int FENCER_HEIGHT = 100;
    private static final int SWORD_LENGTH = 80;
    private static final int MAX_HEALTH = 10;
    private static final int MAX_ROUNDS = 5;
    private static final int ANIMATION_DELAY = 20;

    // Game states
    private enum GameState { MENU, PLAYING, PAUSED, GAME_OVER, ROUND_OVER }
    private GameState currentState = GameState.MENU;

    // Fencers
    private Fencer fencer1;
    private Fencer fencer2;

    // Game variables
    private int round = 1;
    private int score1 = 0;
    private int score2 = 0;
    private Timer timer;
    private Random random = new Random();

    // Visual effects
    private List<SparkEffect> effects = new ArrayList<SparkEffect>();

    // Menu items
    private String[] menuItems = {"Start Game", "Instructions", "Exit"};
    private int selectedMenuItem = 0;

    public FencingChampionship() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        timer = new Timer(ANIMATION_DELAY, this);
        timer.start();

        resetGame();
    }

    private void resetGame() {
        fencer1 = new Fencer(200, GROUND_LEVEL - FENCER_HEIGHT, Color.RED, KeyEvent.VK_A,
                KeyEvent.VK_D, KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_SPACE);
        fencer2 = new Fencer(600, GROUND_LEVEL - FENCER_HEIGHT, Color.BLUE, KeyEvent.VK_LEFT,
                KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_ENTER);
        round = 1;
        score1 = 0;
        score2 = 0;
        effects.clear();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (currentState) {
            case MENU:
                drawMenu(g2d);
                break;
            case PLAYING:
                drawGame(g2d);
                break;
            case PAUSED:
                drawGame(g2d);
                drawPauseScreen(g2d);
                break;
            case ROUND_OVER:
                drawGame(g2d);
                drawRoundOverScreen(g2d);
                break;
            case GAME_OVER:
                drawGame(g2d);
                drawGameOverScreen(g2d);
                break;
        }
    }

    private void drawMenu(Graphics2D g2d) {
        // Draw title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString("FENCING CHAMPIONSHIP", WIDTH/2 - 240, 100);

        // Draw menu items
        g2d.setFont(new Font("Arial", Font.PLAIN, 36));
        for (int i = 0; i < menuItems.length; i++) {
            if (i == selectedMenuItem) {
                g2d.setColor(Color.YELLOW);
            } else {
                g2d.setColor(Color.WHITE);
            }
            g2d.drawString(menuItems[i], WIDTH/2 - 100, 200 + i * 50);
        }

        // Draw instructions at bottom
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Use UP/DOWN arrows to navigate, ENTER to select", WIDTH/2 - 180, HEIGHT - 50);
    }

    private void drawGame(Graphics2D g2d) {
        // Draw background
        g2d.setColor(new Color(20, 20, 40));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw piste (fencing strip)
        g2d.setColor(new Color(60, 60, 80));
        g2d.fillRect(100, GROUND_LEVEL - 20, WIDTH - 200, 40);
        g2d.setColor(Color.WHITE);
        g2d.drawLine(WIDTH/2, GROUND_LEVEL - 20, WIDTH/2, GROUND_LEVEL + 20);

        // Draw center line
        g2d.setColor(Color.WHITE);
        g2d.drawLine(WIDTH/2, 0, WIDTH/2, HEIGHT);

        // Draw fencers
        fencer1.draw(g2d);
        fencer2.draw(g2d);

        // Draw effects
        for (SparkEffect effect : effects) {
            effect.draw(g2d);
        }

        // Draw score and round info
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("Round: " + round + "/" + MAX_ROUNDS, 20, 30);
        g2d.drawString("Red: " + score1, 20, 60);
        g2d.drawString("Blue: " + score2, WIDTH - 100, 60);

        // Draw health bars
        drawHealthBar(g2d, 20, 80, fencer1.health, Color.RED);
        drawHealthBar(g2d, WIDTH - 120, 80, fencer2.health, Color.BLUE);
    }

    private void drawHealthBar(Graphics2D g2d, int x, int y, int health, Color color) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(x, y, 100, 15);
        g2d.setColor(color);
        g2d.fillRect(x, y, health * 10, 15);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(x, y, 100, 15);
    }

    private void drawPauseScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString("PAUSED", WIDTH/2 - 100, HEIGHT/2);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Press P to continue", WIDTH/2 - 120, HEIGHT/2 + 50);
    }

    private void drawRoundOverScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));

        if (fencer1.health <= 0 && fencer2.health <= 0) {
            g2d.drawString("Double Touch!", WIDTH/2 - 100, HEIGHT/2 - 50);
        } else if (fencer1.health <= 0) {
            g2d.drawString("Blue scores!", WIDTH/2 - 100, HEIGHT/2 - 50);
        } else {
            g2d.drawString("Red scores!", WIDTH/2 - 100, HEIGHT/2 - 50);
        }

        g2d.drawString("Round " + round + " over", WIDTH/2 - 120, HEIGHT/2);
        g2d.drawString("Press SPACE to continue", WIDTH/2 - 180, HEIGHT/2 + 50);
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString("GAME OVER", WIDTH/2 - 150, HEIGHT/2 - 50);

        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        if (score1 > score2) {
            g2d.drawString("Red wins the match!", WIDTH/2 - 180, HEIGHT/2);
        } else if (score2 > score1) {
            g2d.drawString("Blue wins the match!", WIDTH/2 - 180, HEIGHT/2);
        } else {
            g2d.drawString("It's a draw!", WIDTH/2 - 120, HEIGHT/2);
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Final Score: Red " + score1 + " - Blue " + score2, WIDTH/2 - 150, HEIGHT/2 + 50);
        g2d.drawString("Press M for menu", WIDTH/2 - 120, HEIGHT/2 + 100);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentState == GameState.PLAYING) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        // Update fencers
        fencer1.update();
        fencer2.update();

        // Check for hits
        checkHits();

        // Update effects
        updateEffects();

        // Check if round is over
        if (fencer1.health <= 0 || fencer2.health <= 0) {
            if (fencer1.health <= 0 && fencer2.health > 0) {
                score2++;
            } else if (fencer2.health <= 0 && fencer1.health > 0) {
                score1++;
            }
            currentState = GameState.ROUND_OVER;
        }
    }

    private void checkHits() {
        // Check if fencer1 hits fencer2
        if (fencer1.isAttacking() && fencer1.getSwordTip().distance(fencer2.getBodyCenter()) < 30) {
            fencer2.health--;
            effects.add(new SparkEffect(fencer1.getSwordTip().x, fencer1.getSwordTip().y));
        }

        // Check if fencer2 hits fencer1
        if (fencer2.isAttacking() && fencer2.getSwordTip().distance(fencer1.getBodyCenter()) < 30) {
            fencer1.health--;
            effects.add(new SparkEffect(fencer2.getSwordTip().x, fencer2.getSwordTip().y));
        }
    }

    private void updateEffects() {
        Iterator<SparkEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            SparkEffect effect = iterator.next();
            effect.update();
            if (effect.isFinished()) {
                iterator.remove();
            }
        }
    }

    private void startNewRound() {
        fencer1.reset();
        fencer2.reset();
        effects.clear();

        if (round >= MAX_ROUNDS) {
            currentState = GameState.GAME_OVER;
        } else {
            round++;
            currentState = GameState.PLAYING;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (currentState) {
            case MENU:
                handleMenuInput(key);
                break;
            case PLAYING:
                handlePlayingInput(key);
                break;
            case PAUSED:
                if (key == KeyEvent.VK_P) {
                    currentState = GameState.PLAYING;
                }
                break;
            case ROUND_OVER:
                if (key == KeyEvent.VK_SPACE) {
                    startNewRound();
                }
                break;
            case GAME_OVER:
                if (key == KeyEvent.VK_M) {
                    currentState = GameState.MENU;
                    resetGame();
                }
                break;
        }
    }

    private void handleMenuInput(int key) {
        if (key == KeyEvent.VK_UP) {
            selectedMenuItem = (selectedMenuItem - 1 + menuItems.length) % menuItems.length;
        } else if (key == KeyEvent.VK_DOWN) {
            selectedMenuItem = (selectedMenuItem + 1) % menuItems.length;
        } else if (key == KeyEvent.VK_ENTER) {
            switch (selectedMenuItem) {
                case 0: // Start Game
                    currentState = GameState.PLAYING;
                    resetGame();
                    break;
                case 1: // Instructions
                    showInstructions();
                    break;
                case 2: // Exit
                    System.exit(0);
                    break;
            }
        }
    }

    private void showInstructions() {
        String message = "FENCING CHAMPIONSHIP - INSTRUCTIONS\n\n" +
                "Player 1 (Red):\n" +
                "  Move: A/D\n" +
                "  Lunge: W\n" +
                "  Retreat: S\n" +
                "  Attack: SPACE\n\n" +
                "Player 2 (Blue):\n" +
                "  Move: LEFT/RIGHT\n" +
                "  Lunge: UP\n" +
                "  Retreat: DOWN\n" +
                "  Attack: ENTER\n\n" +
                "Game Rules:\n" +
                "- First to hit wins the point\n" +
                "- Double touches result in no points\n" +
                "- First to " + MAX_ROUNDS + " points wins the match\n\n" +
                "Press OK to return to menu";

        JOptionPane.showMessageDialog(this, message, "Instructions", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handlePlayingInput(int key) {
        if (key == KeyEvent.VK_P) {
            currentState = GameState.PAUSED;
            return;
        }

        fencer1.handleKeyPress(key);
        fencer2.handleKeyPress(key);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        fencer1.handleKeyRelease(e.getKeyCode());
        fencer2.handleKeyRelease(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Fencer class representing each player
    class Fencer {
        private int x, y;
        private int originalX, originalY;
        private Color color;
        private int leftKey, rightKey, lungeKey, retreatKey, attackKey;
        private boolean movingLeft = false;
        private boolean movingRight = false;
        private boolean lunging = false;
        private boolean retreating = false;
        private boolean attacking = false;
        private int attackCooldown = 0;
        private int lungeCooldown = 0;
        private int health = MAX_HEALTH;
        private boolean facingRight;

        public Fencer(int x, int y, Color color, int leftKey, int rightKey,
                      int lungeKey, int retreatKey, int attackKey) {
            this.x = x;
            this.y = y;
            this.originalX = x;
            this.originalY = y;
            this.color = color;
            this.leftKey = leftKey;
            this.rightKey = rightKey;
            this.lungeKey = lungeKey;
            this.retreatKey = retreatKey;
            this.attackKey = attackKey;
            this.facingRight = (x < WIDTH/2);
        }

        public void update() {
            // Handle movement
            if (movingLeft && x > 100) {
                x -= 3;
                facingRight = false;
            }
            if (movingRight && x < WIDTH - 100 - FENCER_WIDTH) {
                x += 3;
                facingRight = true;
            }

            // Handle lunging
            if (lunging && lungeCooldown <= 0) {
                if (facingRight) {
                    x += 15;
                } else {
                    x -= 15;
                }
                lungeCooldown = 30;
            }

            // Handle retreating
            if (retreating) {
                if (facingRight) {
                    x -= 5;
                } else {
                    x += 5;
                }
            }

            // Update cooldowns
            if (attackCooldown > 0) attackCooldown--;
            if (lungeCooldown > 0) lungeCooldown--;

            // Ensure fencer stays within bounds
            if (x < 100) x = 100;
            if (x > WIDTH - 100 - FENCER_WIDTH) x = WIDTH - 100 - FENCER_WIDTH;
        }

        public void draw(Graphics2D g2d) {
            // Draw body
            g2d.setColor(color);
            g2d.fillRect(x, y, FENCER_WIDTH, FENCER_HEIGHT);

            // Draw mask
            g2d.setColor(Color.BLACK);
            g2d.fillRect(x + 5, y + 5, FENCER_WIDTH - 10, 20);

            // Draw sword
            g2d.setColor(Color.GRAY);
            int swordX = facingRight ? x + FENCER_WIDTH : x - SWORD_LENGTH;
            int swordY = y + FENCER_HEIGHT/2;

            if (attacking && attackCooldown > 20) {
                // Extended sword for attack
                if (facingRight) {
                    g2d.drawLine(x + FENCER_WIDTH, swordY, x + FENCER_WIDTH + SWORD_LENGTH + 20, swordY - 10);
                } else {
                    g2d.drawLine(x, swordY, x - SWORD_LENGTH - 20, swordY - 10);
                }
            } else {
                // Normal sword position
                if (facingRight) {
                    g2d.drawLine(x + FENCER_WIDTH, swordY, x + FENCER_WIDTH + SWORD_LENGTH, swordY);
                } else {
                    g2d.drawLine(x, swordY, x - SWORD_LENGTH, swordY);
                }
            }
        }

        public void handleKeyPress(int key) {
            if (key == leftKey) movingLeft = true;
            if (key == rightKey) movingRight = true;
            if (key == lungeKey && lungeCooldown <= 0) lunging = true;
            if (key == retreatKey) retreating = true;
            if (key == attackKey && attackCooldown <= 0) {
                attacking = true;
                attackCooldown = 40;
            }
        }

        public void handleKeyRelease(int key) {
            if (key == leftKey) movingLeft = false;
            if (key == rightKey) movingRight = false;
            if (key == lungeKey) lunging = false;
            if (key == retreatKey) retreating = false;
            if (key == attackKey) attacking = false;
        }

        public Point getSwordTip() {
            if (facingRight) {
                if (attacking && attackCooldown > 20) {
                    return new Point(x + FENCER_WIDTH + SWORD_LENGTH + 20, y + FENCER_HEIGHT/2 - 10);
                } else {
                    return new Point(x + FENCER_WIDTH + SWORD_LENGTH, y + FENCER_HEIGHT/2);
                }
            } else {
                if (attacking && attackCooldown > 20) {
                    return new Point(x - SWORD_LENGTH - 20, y + FENCER_HEIGHT/2 - 10);
                } else {
                    return new Point(x - SWORD_LENGTH, y + FENCER_HEIGHT/2);
                }
            }
        }

        public Point getBodyCenter() {
            return new Point(x + FENCER_WIDTH/2, y + FENCER_HEIGHT/2);
        }

        public boolean isAttacking() {
            return attacking && attackCooldown > 20;
        }

        public void reset() {
            x = originalX;
            y = originalY;
            health = MAX_HEALTH;
            attacking = false;
            lunging = false;
            attackCooldown = 0;
            lungeCooldown = 0;
        }
    }

    // Visual effect class for sparks when hits occur
    class SparkEffect {
        private int x, y;
        private int life = 20;
        private List<Spark> sparks = new ArrayList<>();

        public SparkEffect(int x, int y) {
            this.x = x;
            this.y = y;

            // Create random sparks
            for (int i = 0; i < 10; i++) {
                sparks.add(new Spark(x, y));
            }
        }

        public void update() {
            life--;
            for (Spark spark : sparks) {
                spark.update();
            }
        }

        public void draw(Graphics2D g2d) {
            for (Spark spark : sparks) {
                spark.draw(g2d);
            }
        }

        public boolean isFinished() {
            return life <= 0;
        }

        class Spark {
            private double x, y;
            private double dx, dy;
            private Color color;
            private int size;

            public Spark(int originX, int originY) {
                this.x = originX;
                this.y = originY;
                this.dx = (Math.random() - 0.5) * 5;
                this.dy = (Math.random() - 0.5) * 5;
                this.color = new Color(255, 255, (int)(Math.random() * 155 + 100));
                this.size = (int)(Math.random() * 3 + 1);
            }

            public void update() {
                x += dx;
                y += dy;
                dy += 0.1; // Gravity
            }

            public void draw(Graphics2D g2d) {
                g2d.setColor(color);
                g2d.fillOval((int)x, (int)y, size, size);
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Fencing Championship");
        FencingChampionship game = new FencingChampionship();
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