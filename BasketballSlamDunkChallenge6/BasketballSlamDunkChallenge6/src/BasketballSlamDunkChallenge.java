import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class BasketballSlamDunkChallenge extends JPanel implements Runnable, KeyListener {
    // Window dimensions
    private final int WIDTH = 800;
    private final int HEIGHT = 600;

    // Game loop
    private Thread gameThread;
    private boolean running = false;
    private final int FPS = 60;

    // Player
    private Player player;

    // Basket
    private Basket basket;

    // Game state
    private int score = 0;
    private boolean showDunkText = false;
    private int dunkTextTimer = 0;

    public BasketballSlamDunkChallenge() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.WHITE);
        setFocusable(true);
        addKeyListener(this);
        initGame();
    }

    private void initGame() {
        player = new Player(100, 400);
        basket = new Basket(650, 200);
    }

    public void startGame() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void run() {
        double drawInterval = 1000000000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (running) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    public void update() {
        player.update();
        if (player.checkDunk(basket)) {
            score++;
            showDunkText = true;
            dunkTextTimer = 60;
        }

        if (dunkTextTimer > 0) {
            dunkTextTimer--;
        } else {
            showDunkText = false;
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        basket.draw(g);
        player.draw(g);
        drawScore(g);

        if (showDunkText) {
            g.setColor(Color.MAGENTA);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString("SLAM DUNK!", WIDTH / 2 - 100, 100);
        }
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Score: " + score, 10, 20);
    }

    public void keyPressed(KeyEvent e) {
        player.keyPressed(e);
    }

    public void keyReleased(KeyEvent e) {
        player.keyReleased(e);
    }

    public void keyTyped(KeyEvent e) {}

    // --- Main method ---
    public static void main(String[] args) {
        JFrame frame = new JFrame("Basketball Slam Dunk Challenge");
        BasketballSlamDunkChallenge game = new BasketballSlamDunkChallenge();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        game.startGame();
    }
}

// --- Player class ---
class Player {
    private int x, y;
    private final int width = 50;
    private final int height = 100;
    private int velocityX = 0;
    private int velocityY = 0;
    private boolean jumping = false;
    private boolean dunking = false;
    private final int groundY = 400;
    private final int jumpPower = -15;

    public Player(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }

    public void update() {
        x += velocityX;
        y += velocityY;

        if (jumping) {
            velocityY += 1;
            if (y >= groundY) {
                y = groundY;
                velocityY = 0;
                jumping = false;
                dunking = false;
            }
        }
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillRect(x, y, width, height);
        if (dunking) {
            g.setColor(Color.ORANGE);
            g.fillOval(x + width / 2 - 10, y - 20, 20, 20);
        }
    }

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) {
            velocityX = -5;
        }
        if (key == KeyEvent.VK_RIGHT) {
            velocityX = 5;
        }
        if (key == KeyEvent.VK_SPACE && !jumping) {
            velocityY = jumpPower;
            jumping = true;
            dunking = true;
        }
    }

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
            velocityX = 0;
        }
    }

    public boolean checkDunk(Basket basket) {
        Rectangle playerRect = new Rectangle(x, y, width, height);
        Rectangle hoopRect = new Rectangle(basket.getX(), basket.getY(), basket.getWidth(), basket.getHeight());

        if (playerRect.intersects(hoopRect) && dunking) {
            dunking = false;
            return true;
        }
        return false;
    }
}

// --- Basket class ---
class Basket {
    private final int x;
    private final int y;
    private final int width = 60;
    private final int height = 10;

    public Basket(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillRect(x, y, width, height);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);
        g.drawLine(x + width / 2, y, x + width / 2, y - 50); // backboard
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
