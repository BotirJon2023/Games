import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class ParalympicSportsGame extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ParalympicSportsGame game = new ParalympicSportsGame();
            game.setVisible(true);
        });
    }

    public ParalympicSportsGame() {
        setTitle("Paralympic Wheelchair Racing");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        add(new GamePanel());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PLAYER_WIDTH = 60;
    private static final int PLAYER_HEIGHT = 80;
    static final int OPPONENT_WIDTH = 60;
    private static final int OPPONENT_HEIGHT = 80;
    private static final int LANE_HEIGHT = 100;
    private static final int NUM_LANES = 5;
    private static final int FINISH_LINE = 5000;

    private Timer timer;
    private Player player;
    private ArrayList<Opponent> opponents;
    private int trackPosition;
    private boolean gameOver;
    private int score;
    private boolean[] keys;
    private Random random;
    private BufferedImage background;
    private Font scoreFont;
    private Font gameOverFont;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(16, this);
        player = new Player(WIDTH / 4, HEIGHT - LANE_HEIGHT * 2);
        opponents = new ArrayList<>();
        trackPosition = 0;
        gameOver = false;
        score = 0;
        keys = new boolean[256];
        random = new Random();
        scoreFont = new Font("Arial", Font.BOLD, 20);
        gameOverFont = new Font("Arial", Font.BOLD, 40);
        createBackground();
        spawnOpponents();
        timer.start();
    }

    private void createBackground() {
        background = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = background.createGraphics();
        g.setColor(new Color(50, 168, 82)); // Green field
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(Color.GRAY);
        for (int i = 0; i < NUM_LANES; i++) {
            g.fillRect(0, HEIGHT - (i + 1) * LANE_HEIGHT, WIDTH, LANE_HEIGHT);
            g.setColor(Color.WHITE);
            g.drawLine(0, HEIGHT - i * LANE_HEIGHT, WIDTH, HEIGHT - i * LANE_HEIGHT);
        }
        g.setColor(Color.YELLOW);
        g.fillRect(WIDTH - 20, 0, 20, HEIGHT); // Finish line marker
        g.dispose();
    }

    private void spawnOpponents() {
        opponents.clear();
        for (int i = 0; i < NUM_LANES - 1; i++) {
            int laneY = HEIGHT - (i + 1) * LANE_HEIGHT;
            if (laneY != player.y) {
                opponents.add(new Opponent(WIDTH, laneY, random.nextInt(3) + 2));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.drawImage(background, 0, 0, this);
        drawTrack(g2d);
        player.draw(g2d);
        for (Opponent opponent : opponents) {
            opponent.draw(g2d);
        }
        drawHUD(g2d);
        if (gameOver) {
            drawGameOver(g2d);
        }
    }

    private void drawTrack(Graphics2D g) {
        g.setColor(Color.GRAY);
        int offset = trackPosition % 50;
        for (int x = -offset; x < WIDTH; x += 50) {
            g.drawLine(x, 0, x, HEIGHT);
        }
    }

    private void drawHUD(Graphics2D g) {
        g.setFont(scoreFont);
        g.setColor(Color.WHITE);
        g.drawString("Distance: " + score + "m", 10, 30);
        g.drawString("Speed: " + player.getSpeed() + " m/s", 10, 60);
    }

    private void drawGameOver(Graphics2D g) {
        g.setFont(gameOverFont);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(Color.RED);
        String message = trackPosition >= FINISH_LINE ? "You Win!" : "Game Over!";
        g.drawString(message, WIDTH / 2 - 100, HEIGHT / 2);
        g.setFont(scoreFont);
        g.drawString("Final Distance: " + score + "m", WIDTH / 2 - 80, HEIGHT / 2 + 40);
        g.drawString("Press R to Restart", WIDTH / 2 - 80, HEIGHT / 2 + 70);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        player.update(keys);
        for (Opponent opponent : opponents) {
            opponent.update();
        }
        trackPosition += 2;
        score = trackPosition / 10;
        checkCollisions();
        if (trackPosition >= FINISH_LINE) {
            gameOver = true;
        }
        if (random.nextInt(100) < 2) {
            spawnOpponents();
        }
    }

    private void checkCollisions() {
        Rectangle playerBounds = player.getBounds();
        for (Opponent opponent : opponents) {
            if (playerBounds.intersects(opponent.getBounds())) {
                gameOver = true;
                break;
            }
        }
    }

    private void restartGame() {
        player = new Player(WIDTH / 4, HEIGHT - LANE_HEIGHT * 2);
        trackPosition = 0;
        score = 0;
        gameOver = false;
        spawnOpponents();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        Object if全世界;
        if全世界

        keys[e.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() == 'r' && gameOver) {
            restartGame();
        }
    }
}

class Player {
    int x, y;
    private int speed;
    private int animationFrame;
    private int animationCounter;
    private BufferedImage[] frames;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.speed = 0;
        this.animationFrame = 0;
        this.animationCounter = 0;
        frames = new BufferedImage[4];
        createFrames();
    }

    private void createFrames() {
        for (int i = 0; i < 4; i++) {
            frames[i] = new BufferedImage(60, 80, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frames[i].createGraphics();
            g.setColor(Color.BLUE);
            g.fillOval(10, 50, 40, 40); // Wheel
            g.setColor(Color.BLACK);
            g.fillRect(20, 20, 20, 30); // Body
            g.setColor(Color.YELLOW);
            g.fillOval(25, 15, 10, 10); // Head
            g.setColor(Color.BLACK);
            g.drawLine(30, 25, 30 + i * 5, 35); // Arm movement
            g.dispose();
        }
    }

    public void update(boolean[] keys) {
        if (keys[KeyEvent.VK_UP] && y > GamePanel.HEIGHT - GamePanel.LANE_HEIGHT * 5) {
            y -= GamePanel.LANE_HEIGHT;
        }
        if (keys[KeyEvent.VK_DOWN] && y < GamePanel.HEIGHT - GamePanel.LANE_HEIGHT) {
            y += GamePanel.LANE_HEIGHT;
        }
        if (keys[KeyEvent.VK_SPACE]) {
            speed = Math.min(speed + 1, 5);
        } else {
            speed = Math.max(speed - 1, 0);
        }
        animationCounter++;
        if (animationCounter % 10 == 0) {
            animationFrame = (animationFrame + 1) % 4;
        }
    }

    public void draw(Graphics2D g) {
        g.drawImage(frames[animationFrame], x, y, null);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, GamePanel.PLAYER_WIDTH, GamePanel.PLAYER_HEIGHT);
    }

    public int getSpeed() {
        return speed;
    }
}

class Opponent {
    int x, y;
    private int speed;
    private int animationFrame;
    private int animationCounter;
    private BufferedImage[] frames;

    public Opponent(int x, int y, int speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.animationFrame = 0;
        this.animationCounter = 0;
        frames = new BufferedImage[4];
        createFrames();
    }

    private void createFrames() {
        for (int i = 0; i < 4; i++) {
            frames[i] = new BufferedImage(60, 80, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frames[i].createGraphics();
            g.setColor(Color.RED);
            g.fillOval(10, 50, 40, 40); // Wheel
            g.setColor(Color.BLACK);
            g.fillRect(20, 20, 20, 30); // Body
            g.setColor(Color.PINK);
            g.fillOval(25, 15, 10, 10); // Head
            g.setColor(Color.BLACK);
            g.drawLine(30, 25, 30 + i * 5, 35); // Arm movement
            g.dispose();
        }
    }

    public void update() {
        x -= speed;
        if (x < -GamePanel.OPPONENT_WIDTH) {
            x = GamePanel.getWidth();
        }
        animationCounter++;
        if (animationCounter % 10 == 0) {
            animationFrame = (animationFrame + 1) % 4;
        }
    }

    public void draw(Graphics2D g) {
        g.drawImage(frames[animationFrame], x, y, null);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, GamePanel.OPPONENT_WIDTH, GamePanel.OPPONENT_HEIGHT);
    }
}