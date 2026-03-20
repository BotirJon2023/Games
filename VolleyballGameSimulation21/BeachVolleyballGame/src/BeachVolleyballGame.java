import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class BeachVolleyballGame extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BeachVolleyballGame game = new BeachVolleyballGame();
            game.setVisible(true);
        });
    }

    public BeachVolleyballGame() {
        setTitle("🏐 Beach Volleyball Championship");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        setLocationRelativeTo(null);

        // Add menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");
        JMenuItem newGame = new JMenuItem("New Game");
        JMenuItem exit = new JMenuItem("Exit");

        newGame.addActionListener(e -> gamePanel.resetGame());
        exit.addActionListener(e -> System.exit(0));

        gameMenu.add(newGame);
        gameMenu.add(exit);
        menuBar.add(gameMenu);
        setJMenuBar(menuBar);
    }
}

class GamePanel extends JPanel implements Runnable, KeyListener {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 700;
    private static final int GROUND_Y = 600;
    private static final int NET_X = WIDTH / 2;
    private static final int NET_HEIGHT = 150;

    private Thread gameThread;
    private boolean running = false;

    // Game objects
    private Player player1;
    private Player player2;
    private Ball ball;
    private Net net;

    // Game state
    private int score1 = 0;
    private int score2 = 0;
    private boolean gameOver = false;
    private String winner = "";
    private int round = 1;

    // Game mode
    private boolean vsComputer = true;
    private Difficulty difficulty = Difficulty.NORMAL;

    // Animation
    private int frameCount = 0;
    private Cloud[] clouds;
    private Wave[] waves;
    private ParticleSystem particles;

    // Input
    private boolean[] keys = new boolean[256];

    enum Difficulty {
        EASY(0.6, 0.7, 0.8),
        NORMAL(0.8, 0.85, 0.9),
        HARD(0.95, 0.95, 0.98),
        EXPERT(1.0, 1.0, 1.0);

        final double speedMultiplier;
        final double accuracy;
        final double reactionTime;

        Difficulty(double speed, double acc, double react) {
            this.speedMultiplier = speed;
            this.accuracy = acc;
            this.reactionTime = react;
        }
    }

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky blue
        setFocusable(true);
        addKeyListener(this);

        initGame();
        startGame();
    }

    private void initGame() {
        // Initialize players
        player1 = new Player(150, GROUND_Y - 100, Color.RED, true);
        player2 = new Player(WIDTH - 250, GROUND_Y - 100, Color.BLUE, false);

        // Initialize ball
        ball = new Ball(WIDTH/2, 200);

        // Initialize net
        net = new Net(NET_X, GROUND_Y - NET_HEIGHT, 20, NET_HEIGHT);

        // Initialize clouds
        clouds = new Cloud[5];
        for (int i = 0; i < clouds.length; i++) {
            clouds[i] = new Cloud();
        }

        // Initialize waves
        waves = new Wave[3];
        for (int i = 0; i < waves.length; i++) {
            waves[i] = new Wave(i * 400);
        }

        // Initialize particles
        particles = new ParticleSystem();

        // Show mode selection dialog
        showModeSelection();
    }

    private void showModeSelection() {
        String[] options = {"vs Computer", "2 Players"};
        int choice = JOptionPane.showOptionDialog(this,
                "Select Game Mode",
                "Beach Volleyball",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        vsComputer = (choice == 0);

        if (vsComputer) {
            String[] levels = {"Easy", "Normal", "Hard", "Expert"};
            String level = (String) JOptionPane.showInputDialog(this,
                    "Select Difficulty:",
                    "Difficulty Level",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    levels,
                    levels[1]);

            switch (level) {
                case "Easy": difficulty = Difficulty.EASY; break;
                case "Hard": difficulty = Difficulty.HARD; break;
                case "Expert": difficulty = Difficulty.EXPERT; break;
                default: difficulty = Difficulty.NORMAL;
            }
        }
    }

    public void startGame() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void resetGame() {
        score1 = 0;
        score2 = 0;
        round = 1;
        gameOver = false;
        winner = "";
        resetRound();
        showModeSelection();
    }

    private void resetRound() {
        ball.reset();
        player1.reset();
        player2.reset();
        particles.clear();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60.0;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            while (delta >= 1) {
                update();
                delta--;
            }

            repaint();

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        if (gameOver) return;

        frameCount++;

        // Update clouds
        for (Cloud cloud : clouds) {
            cloud.update();
        }

        // Update waves
        for (Wave wave : waves) {
            wave.update();
        }

        // Update particles
        particles.update();

        // Player 1 controls (WASD + Space)
        if (keys[KeyEvent.VK_A]) player1.moveLeft();
        if (keys[KeyEvent.VK_D]) player1.moveRight();
        if (keys[KeyEvent.VK_W] && player1.onGround) player1.jump();
        if (keys[KeyEvent.VK_SPACE]) player1.dive();

        // Player 2 controls or AI
        if (!vsComputer) {
            if (keys[KeyEvent.VK_LEFT]) player2.moveLeft();
            if (keys[KeyEvent.VK_RIGHT]) player2.moveRight();
            if (keys[KeyEvent.VK_UP] && player2.onGround) player2.jump();
            if (keys[KeyEvent.VK_ENTER]) player2.dive();
        } else {
            updateAI();
        }

        // Update players
        player1.update();
        player2.update();

        // Update ball
        ball.update();

        // Check collisions
        checkCollisions();

        // Check scoring
        checkScore();
    }

    private void updateAI() {
        // AI behavior based on difficulty
        double targetX = ball.x;
        double reactThreshold = 100 * (2 - difficulty.reactionTime);

        // Only react if ball is on AI side or coming to AI side
        if (ball.vx > 0 || ball.x > NET_X) {
            double distance = Math.abs(player2.x - targetX);

            // Movement
            if (distance > reactThreshold) {
                if (player2.x < targetX - 20) {
                    player2.moveRight();
                } else if (player2.x > targetX + 20) {
                    player2.moveLeft();
                }
            }

            // Jumping logic
            if (ball.y < GROUND_Y - 200 && ball.vy < 0 &&
                    Math.abs(ball.x - player2.x) < 100 && player2.onGround) {
                if (Math.random() < difficulty.accuracy) {
                    player2.jump();
                }
            }

            // Diving for far balls
            if (Math.abs(ball.x - player2.x) > 150 && ball.y > GROUND_Y - 150) {
                if (Math.random() < difficulty.accuracy * 0.5) {
                    player2.dive();
                }
            }
        } else {
            // Return to center when ball is far
            double centerX = WIDTH - 300;
            if (player2.x < centerX - 50) {
                player2.moveRight();
            } else if (player2.x > centerX + 50) {
                player2.moveLeft();
            }
        }

        // Speed adjustment based on difficulty
        player2.speed = player2.baseSpeed * difficulty.speedMultiplier;
    }

    private void checkCollisions() {
        // Ball with net
        if (ball.getBounds().intersects(net.getBounds())) {
            if (ball.vx > 0 && ball.x < NET_X + 10) {
                ball.vx = -ball.vx * 0.5;
                ball.x = NET_X - ball.radius;
            } else if (ball.vx < 0 && ball.x > NET_X - 10) {
                ball.vx = -ball.vx * 0.5;
                ball.x = NET_X + net.width + ball.radius;
            }
            ball.vy *= 0.8;
        }

        // Ball with players
        if (ball.collidesWith(player1)) {
            ball.hit(player1, true);
            particles.createExplosion((int)ball.x, (int)ball.y, Color.RED);
        }

        if (ball.collidesWith(player2)) {
            ball.hit(player2, false);
            particles.createExplosion((int)ball.x, (int)ball.y, Color.BLUE);
        }

        // Ball with ground
        if (ball.y + ball.radius >= GROUND_Y) {
            ball.y = GROUND_Y - ball.radius;
            ball.vy = -ball.vy * 0.7;
            ball.vx *= 0.9;

            if (Math.abs(ball.vy) < 2) ball.vy = 0;
        }
    }

    private void checkScore() {
        // Ball hits ground
        if (ball.y + ball.radius >= GROUND_Y - 5 && Math.abs(ball.vy) < 1) {
            if (ball.x < NET_X) {
                score2++;
                checkWin();
            } else {
                score1++;
                checkWin();
            }
            if (!gameOver) {
                resetRound();
            }
        }

        // Ball out of bounds
        if (ball.x < 0 || ball.x > WIDTH) {
            if (ball.x < 0) {
                score2++;
            } else {
                score1++;
            }
            checkWin();
            if (!gameOver) {
                resetRound();
            }
        }
    }

    private void checkWin() {
        if (score1 >= 5 || score2 >= 5) {
            if (Math.abs(score1 - score2) >= 2) {
                gameOver = true;
                winner = score1 > score2 ? "Player 1 Wins!" :
                        (vsComputer ? "Computer Wins!" : "Player 2 Wins!");
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw sky gradient
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, HEIGHT/2, new Color(255, 218, 185));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT/2);

        // Draw sun
        g2d.setColor(Color.YELLOW);
        int sunX = WIDTH - 150;
        int sunY = 80;
        g2d.fillOval(sunX - 40, sunY - 40, 80, 80);
        g2d.setColor(new Color(255, 255, 200, 100));
        g2d.fillOval(sunX - 60, sunY - 60, 120, 120);

        // Draw clouds
        for (Cloud cloud : clouds) {
            cloud.draw(g2d);
        }

        // Draw ocean
        g2d.setColor(new Color(0, 119, 190));
        g2d.fillRect(0, HEIGHT/2, WIDTH, HEIGHT/2);

        // Draw waves
        for (Wave wave : waves) {
            wave.draw(g2d);
        }

        // Draw beach (sand)
        int[] sandX = {0, WIDTH, WIDTH, 0};
        int[] sandY = {GROUND_Y, GROUND_Y, HEIGHT, HEIGHT};
        g2d.setColor(new Color(238, 214, 175));
        g2d.fillPolygon(sandX, sandY, 4);

        // Draw sand texture
        g2d.setColor(new Color(218, 194, 155));
        Random rand = new Random(42);
        for (int i = 0; i < 100; i++) {
            int x = rand.nextInt(WIDTH);
            int y = GROUND_Y + rand.nextInt(HEIGHT - GROUND_Y);
            g2d.fillOval(x, y, 3, 2);
        }

        // Draw court lines
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(50, GROUND_Y, WIDTH - 50, GROUND_Y);
        g2d.drawLine(NET_X, GROUND_Y - NET_HEIGHT, NET_X, GROUND_Y);

        // Draw net shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillRect(NET_X + 5, GROUND_Y - NET_HEIGHT + 5, (int) net.width, NET_HEIGHT);

        // Draw game objects
        net.draw(g2d);
        player1.draw(g2d);
        player2.draw(g2d);
        ball.draw(g2d);
        particles.draw(g2d);

        // Draw UI
        drawUI(g2d);

        // Draw game over
        if (gameOver) {
            drawGameOver(g2d);
        }
    }

    private void drawUI(Graphics2D g2d) {
        // Scoreboard background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(WIDTH/2 - 150, 20, 300, 80, 20, 20);

        // Scores
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.setColor(Color.WHITE);
        String score = score1 + " - " + score2;
        FontMetrics fm = g2d.getFontMetrics();
        int scoreX = WIDTH/2 - fm.stringWidth(score)/2;
        g2d.drawString(score, scoreX, 75);

        // Player labels
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.RED);
        g2d.drawString("P1", WIDTH/2 - 120, 50);
        g2d.setColor(Color.BLUE);
        g2d.drawString(vsComputer ? "CPU" : "P2", WIDTH/2 + 100, 50);

        // Difficulty indicator
        if (vsComputer) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("Level: " + difficulty.name(), 20, 30);
        }

        // Controls help
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawString("P1: A/D=Move, W=Jump, Space=Dive", 20, HEIGHT - 40);
        if (!vsComputer) {
            g2d.drawString("P2: ←/→=Move, ↑=Jump, Enter=Dive", WIDTH - 250, HEIGHT - 40);
        }
    }

    private void drawGameOver(Graphics2D g2d) {
        // Dark overlay
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Winner text
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        g2d.setColor(Color.YELLOW);
        FontMetrics fm = g2d.getFontMetrics();
        int textX = WIDTH/2 - fm.stringWidth(winner)/2;
        g2d.drawString(winner, textX, HEIGHT/2);

        // Restart instruction
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.setColor(Color.WHITE);
        String restart = "Press R to Restart or select New Game from menu";
        int restartX = WIDTH/2 - g2d.getFontMetrics().stringWidth(restart)/2;
        g2d.drawString(restart, restartX, HEIGHT/2 + 60);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;

        if (e.getKeyCode() == KeyEvent.VK_R && gameOver) {
            resetGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

// Player class with animation
class Player {
    double x, y;
    double vx, vy;
    double width = 50, height = 80;
    double baseSpeed = 6;
    double speed = 6;
    double jumpPower = 18;
    boolean onGround = false;
    boolean isDiving = false;
    boolean facingRight = true;
    boolean isPlayer1;
    Color color;

    // Animation
    int animationFrame = 0;
    int diveTimer = 0;

    public Player(double x, double y, Color color, boolean isPlayer1) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.isPlayer1 = isPlayer1;
        this.facingRight = !isPlayer1;
    }

    public void reset() {
        vx = 0;
        vy = 0;
        isDiving = false;
        diveTimer = 0;
        y = 600 - 100;
        x = isPlayer1 ? 150 : 950;
    }

    public void moveLeft() {
        vx = -speed;
        facingRight = false;
    }

    public void moveRight() {
        vx = speed;
        facingRight = true;
    }

    public void jump() {
        if (onGround && !isDiving) {
            vy = -jumpPower;
            onGround = false;
        }
    }

    public void dive() {
        if (!isDiving && !onGround) {
            isDiving = true;
            diveTimer = 30;
            vx = facingRight ? speed * 2.5 : -speed * 2.5;
            vy = 5;
        }
    }

    public void update() {
        // Physics
        vy += 0.8; // Gravity

        x += vx;
        y += vy;

        // Friction
        vx *= 0.85;

        // Ground collision
        if (y + height >= 600) {
            y = 600 - height;
            vy = 0;
            onGround = true;
            isDiving = false;
        } else {
            onGround = false;
        }

        // Boundaries
        if (x < 50) x = 50;
        if (x > 1150) x = 1150;

        // Dive timer
        if (diveTimer > 0) {
            diveTimer--;
            if (diveTimer == 0) isDiving = false;
        }

        // Animation frame
        if (Math.abs(vx) > 0.5 || Math.abs(vy) > 0.5) {
            animationFrame++;
        }
    }

    public Rectangle2D getBounds() {
        if (isDiving) {
            return new Rectangle2D.Double(x, y + height/2, width * 1.5, height/2);
        }
        return new Rectangle2D.Double(x, y, width, height);
    }

    public void draw(Graphics2D g2d) {
        Graphics2D g = (Graphics2D) g2d.create();

        // Shadow
        g.setColor(new Color(0, 0, 0, 50));
        int shadowWidth = isDiving ? 80 : 50;
        g.fillOval((int)x - 5, 595, shadowWidth, 10);

        // Body color with gradient
        GradientPaint bodyGradient = new GradientPaint(
                (int)x, (int)y, color,
                (int)x, (int)(y + height), color.darker()
        );
        g.setPaint(bodyGradient);

        if (isDiving) {
            // Draw diving pose (horizontal)
            AffineTransform old = g.getTransform();
            g.translate(x + width/2, y + height/2);
            g.rotate(facingRight ? Math.PI/4 : -Math.PI/4);
            g.fillRoundRect(-40, -20, 80, 40, 10, 10);
            g.setTransform(old);
        } else {
            // Draw standing/running pose
            int bob = (animationFrame / 5) % 2;

            // Body
            g.fillRoundRect((int)x, (int)y + bob, (int)width, (int)height - 20, 15, 15);

            // Head
            g.setColor(new Color(255, 220, 177));
            int headY = (int)y - 30 + bob;
            g.fillOval((int)x + 5, headY, 40, 40);

            // Headband
            g.setColor(color.darker());
            g.fillRect((int)x + 5, headY + 10, 40, 8);

            // Arms (animated)
            g.setColor(new Color(255, 220, 177));
            int armOffset = (int)(Math.sin(animationFrame * 0.2) * 10);
            if (Math.abs(vx) > 0.5) {
                g.fillRoundRect((int)x - 10, (int)y + 20 + armOffset, 15, 40, 5, 5);
                g.fillRoundRect((int) ((int)x + width - 5), (int)y + 20 - armOffset, 15, 40, 5, 5);
            } else {
                g.fillRoundRect((int)x - 10, (int)y + 20, 15, 40, 5, 5);
                g.fillRoundRect((int) ((int)x + width - 5), (int)y + 20, 15, 40, 5, 5);
            }

            // Legs
            g.setColor(color.darker());
            int legOffset = (int)(Math.sin(animationFrame * 0.3) * 5);
            g.fillRoundRect((int)x + 5, (int) ((int)y + height - 40), 15, 40 + legOffset, 5, 5);
            g.fillRoundRect((int) ((int)x + width - 20), (int) ((int)y + height - 40), 15, 40 - legOffset, 5, 5);
        }

        g.dispose();
    }
}

// Ball class with physics and trail effect
class Ball {
    double x, y;
    double vx, vy;
    double radius = 15;
    double rotation = 0;

    // Trail effect
    double[] trailX = new double[10];
    double[] trailY = new double[10];
    int trailIndex = 0;

    public Ball(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void reset() {
        x = 600;
        y = 200;
        vx = (Math.random() - 0.5) * 10;
        vy = 5;
        rotation = 0;
    }

    public void update() {
        // Save trail
        trailX[trailIndex] = x;
        trailY[trailIndex] = y;
        trailIndex = (trailIndex + 1) % trailX.length;

        // Physics
        vy += 0.6; // Gravity
        vx *= 0.995; // Air resistance
        vy *= 0.995;

        x += vx;
        y += vy;

        rotation += vx * 0.1;

        // Ceiling
        if (y - radius < 0) {
            y = radius;
            vy = -vy * 0.8;
        }
    }

    public boolean collidesWith(Player player) {
        return getBounds().intersects(player.getBounds());
    }

    public void hit(Player player, boolean isPlayer1) {
        // Calculate hit angle
        double hitX = x - (player.x + player.width/2);
        double hitY = y - (player.y + player.height/2);

        // Normalize
        double dist = Math.sqrt(hitX * hitX + hitY * hitY);
        if (dist == 0) dist = 1;

        double nx = hitX / dist;
        double ny = hitY / dist;

        // Set velocity based on hit position and player movement
        double power = 15 + Math.abs(player.vx) * 0.5;
        if (player.isDiving) power *= 1.3;

        vx = nx * power;
        vy = ny * power - 5;

        // Ensure ball goes toward opponent
        if (isPlayer1 && vx < 0) vx = -vx;
        if (!isPlayer1 && vx > 0) vx = -vx;

        // Add some randomness
        vx += (Math.random() - 0.5) * 4;
        vy += (Math.random() - 0.5) * 4;
    }

    public Ellipse2D getBounds() {
        return new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2);
    }

    public void draw(Graphics2D g2d) {
        // Draw trail
        for (int i = 0; i < trailX.length; i++) {
            int idx = (trailIndex + i) % trailX.length;
            int alpha = (i * 25);
            g2d.setColor(new Color(255, 255, 255, alpha));
            double size = radius * (0.5 + i * 0.05);
            g2d.fillOval((int)(trailX[idx] - size), (int)(trailY[idx] - size),
                    (int)(size * 2), (int)(size * 2));
        }

        // Draw ball with rotation effect (stripes)
        Graphics2D g = (Graphics2D) g2d.create();
        g.translate(x, y);
        g.rotate(rotation);

        // Ball base
        g.setColor(Color.WHITE);
        g.fillOval((int)-radius, (int)-radius, (int)(radius*2), (int)(radius*2));

        // Stripes
        g.setColor(Color.YELLOW);
        g.setStroke(new BasicStroke(3));
        for (int i = -1; i <= 1; i++) {
            g.drawLine((int)(-radius + 5), i * 8, (int)(radius - 5), i * 8);
        }

        // Outline
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawOval((int)-radius, (int)-radius, (int)(radius*2), (int)(radius*2));

        // Shine
        g.setColor(new Color(255, 255, 255, 150));
        g.fillOval((int)(-radius + 3), (int)(-radius + 3), 10, 8);

        g.dispose();
    }
}

// Net class
class Net {
    double x, y, width, height;

    public Net(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x, y, width, height);
    }

    public void draw(Graphics2D g2d) {
        // Net mesh pattern
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < height; i += 10) {
            g2d.drawLine((int)x, (int)(y + i), (int)(x + width), (int)(y + i));
        }
        for (int i = 0; i < width; i += 5) {
            g2d.drawLine((int)(x + i), (int)y, (int)(x + i), (int)(y + height));
        }

        // Net posts
        g2d.setColor(Color.YELLOW);
        g2d.fillRect((int)x - 5, (int)y - 10, 10, (int)height + 20);
        g2d.fillRect((int)(x + width) - 5, (int)y - 10, 10, (int)height + 20);

        // Top tape
        g2d.setColor(Color.WHITE);
        g2d.fillRect((int)x - 5, (int)y - 5, (int)width + 10, 10);
    }
}

// Cloud animation
class Cloud {
    double x, y;
    double speed;
    double scale;

    public Cloud() {
        reset();
        x = Math.random() * 1200;
    }

    public void reset() {
        x = -200;
        y = 50 + Math.random() * 150;
        speed = 0.2 + Math.random() * 0.3;
        scale = 0.5 + Math.random() * 0.5;
    }

    public void update() {
        x += speed;
        if (x > 1300) reset();
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 200));
        int baseX = (int)x;
        int baseY = (int)y;
        int s = (int)(30 * scale);

        g2d.fillOval(baseX, baseY, s * 2, s);
        g2d.fillOval(baseX + s, baseY - s/2, s * 2, s);
        g2d.fillOval(baseX + s * 2, baseY, s * 2, s);
    }
}

// Wave animation
class Wave {
    double x;
    double offset;

    public Wave(double x) {
        this.x = x;
        this.offset = Math.random() * Math.PI * 2;
    }

    public void update() {
        offset += 0.05;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 100));
        int baseY = 450;

        for (int i = 0; i < 400; i += 20) {
            int waveHeight = (int)(Math.sin((i + x) * 0.02 + offset) * 10);
            g2d.fillOval((int)(x + i), baseY + waveHeight, 30, 15);
        }
    }
}

// Particle system for effects
class ParticleSystem {
    java.util.List<Particle> particles = new java.util.ArrayList<>();

    public void createExplosion(int x, int y, Color color) {
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    public void update() {
        particles.removeIf(p -> {
            p.update();
            return p.life <= 0;
        });
    }

    public void draw(Graphics2D g2d) {
        for (Particle p : particles) {
            p.draw(g2d);
        }
    }

    public void clear() {
        particles.clear();
    }
}

class Particle {
    double x, y, vx, vy;
    double life = 1.0;
    Color color;

    public Particle(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        double angle = Math.random() * Math.PI * 2;
        double speed = 2 + Math.random() * 4;
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        this.color = color;
    }

    public void update() {
        x += vx;
        y += vy;
        vy += 0.2; // Gravity
        life -= 0.02;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255 * life)));
        g2d.fillOval((int)x, (int)y, 6, 6);
    }
}