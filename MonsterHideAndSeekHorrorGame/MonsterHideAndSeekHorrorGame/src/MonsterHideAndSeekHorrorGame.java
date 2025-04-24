
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Random;

public class MonsterHideAndSeekHorrorGame extends JFrame {
    private GamePanel gamePanel;
    private final int WINDOW_WIDTH = 800;
    private final int WINDOW_HEIGHT = 600;
    private final String TITLE = "Monster Hide and Seek Horror Game";

    public MonsterHideAndSeekHorrorGame() {
        setTitle(TITLE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        gamePanel = new GamePanel();
        add(gamePanel);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MonsterHideAndSeekHorrorGame());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private final int TILE_SIZE = 40;
    private final int GRID_WIDTH = 20;
    private final int GRID_HEIGHT = 15;
    private final int PLAYER_SPEED = 5;
    private final int MONSTER_SPEED = 2;
    private final int FPS = 60;
    private final int DELAY = 1000 / FPS;

    private Player player;
    private Monster monster;
    private ArrayList<Obstacle> obstacles;
    private Timer timer;
    private boolean gameOver;
    private int score;
    private int hidingSpotsFound;
    private final int MAX_HIDING_SPOTS = 3;
    private Clip backgroundMusic;
    private Clip scareSound;
    private BufferedImage playerImage;
    private BufferedImage monsterImage;
    private BufferedImage obstacleImage;
    private BufferedImage hidingSpotImage;
    private Font horrorFont;

    public GamePanel() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        initializeGame();
        timer = new Timer(DELAY, this);
        timer.start();
        loadResources();
    }

    private void initializeGame() {
        player = new Player(40, 40);
        monster = new Monster(760, 560);
        obstacles = new ArrayList<>();
        score = 0;
        hidingSpotsFound = 0;
        gameOver = false;
        generateObstacles();
        generateHidingSpots();
    }

    private void loadResources() {
        try {
            // Placeholder for images (in real implementation, load from files)
            playerImage = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = playerImage.createGraphics();
            g2d.setColor(Color.BLUE);
            g2d.fillRect(0, 0, 40, 40);
            g2d.dispose();

            monsterImage = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
            g2d = monsterImage.createGraphics();
            g2d.setColor(Color.RED);
            g2d.fillRect(0, 0, 40, 40);
            g2d.dispose();

            obstacleImage = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
            g2d = obstacleImage.createGraphics();
            g2d.setColor(Color.GRAY);
            g2d.fillRect(0, 0, 40, 40);
            g2d.dispose();

            hidingSpotImage = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
            g2d = hidingSpotImage.createGraphics();
            g2d.setColor(Color.GREEN);
            g2d.fillRect(0, 0, 40, 40);
            g2d.dispose();

            // Load font
            horrorFont = new Font("Serif", Font.BOLD, 24);

            // Load sounds
            File musicFile = new File("background.wav");
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(musicFile);
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioStream);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);

            File scareFile = new File("scare.wav");
            audioStream = AudioSystem.getAudioInputStream(scareFile);
            DebugGraphics AudioStuartAudioSystem = null;
            scareSound = (Clip) AudioStuartAudioSystem.getClip();
            scareSound.open(audioStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateObstacles() {
        Random rand = new Random();
        for (int i = 0; i < 20; i++) {
            int x = rand.nextInt(GRID_WIDTH) * TILE_SIZE;
            int y = rand.nextInt(GRID_HEIGHT) * TILE_SIZE;
            obstacles.add(new Obstacle(x, y));
        }
    }

    private void generateHidingSpots() {
        Random rand = new Random();
        for (int i = 0; i < MAX_HIDING_SPOTS; i++) {
            int x = rand.nextInt(GRID_WIDTH) * TILE_SIZE;
            int y = rand.nextInt(GRID_HEIGHT) * TILE_SIZE;
            obstacles.add(new HidingSpot(x, y));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw player
        g2d.drawImage(playerImage, player.x, player.y, null);

        // Draw monster
        g2d.drawImage(monsterImage, monster.x, monster.y, null);

        // Draw obstacles and hiding spots
        for (Obstacle obstacle : obstacles) {
            if (obstacle instanceof HidingSpot) {
                g2d.drawImage(hidingSpotImage, obstacle.x, obstacle.y, null);
            } else {
                g2d.drawImage(obstacleImage, obstacle.x, obstacle.y, null);
            }
        }

        // Draw score and status
        g2d.setFont(horrorFont);
        g2d.setColor(Color.WHITE);
        g2d.drawString("Score: " + score, 10, 30);
        g2d.drawString("Hiding Spots: " + hidingSpotsFound + "/" + MAX_HIDING_SPOTS, 10, 60);

        // Game over screen
        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(Color.RED);
            g2d.setFont(horrorFont.deriveFont(48f));
            g2d.drawString("Game Over!", 300, 300);
            g2d.setFont(horrorFont.deriveFont(24f));
            g2d.drawString("Final Score: " + score, 320, 350);
            g2d.drawString("Press R to Restart", 300, 400);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            updateGame();
            repaint();
        }
    }

    private void updateGame() {
        // Update player
        player.update();

        // Update monster
        monster.update(player.x, player.y);

        // Check collisions
        checkCollisions();

        // Update score
        score += 1;
    }

    private void checkCollisions() {
        // Player and monster collision
        Rectangle playerRect = new Rectangle(player.x, player.y, TILE_SIZE, TILE_SIZE);
        Rectangle monsterRect = new Rectangle(monster.x, monster.y, TILE_SIZE, TILE_SIZE);

        if (playerRect.intersects(monsterRect)) {
            try {
                scareSound.setFramePosition(0);
                scareSound.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            gameOver = true;
            timer.stop();
        }

        // Player and obstacle collision
        for (Obstacle obstacle : obstacles) {
            Rectangle obstacleRect = new Rectangle(obstacle.x, obstacle.y, TILE_SIZE, TILE_SIZE);
            if (playerRect.intersects(obstacleRect)) {
                if (obstacle instanceof HidingSpot) {
                    obstacles.remove(obstacle);
                    hidingSpotsFound++;
                    score += 100;
                    if (hidingSpotsFound >= MAX_HIDING_SPOTS) {
                        gameOver = true;
                        timer.stop();
                    }
                    break;
                } else {
                    player.undoMove();
                }
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) {
            player.dx = -PLAYER_SPEED;
        } else if (key == KeyEvent.VK_RIGHT) {
            player.dx = PLAYER_SPEED;
        } else if (key == KeyEvent.VK_UP) {
            player.dy = -PLAYER_SPEED;
        } else if (key == KeyEvent.VK_DOWN) {
            player.dy = PLAYER_SPEED;
        } else if (key == KeyEvent.VK_R && gameOver) {
            initializeGame();
            timer.start();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
            player.dx = 0;
        } else if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) {
            player.dy = 0;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

class Player {
    int x, y;
    int dx, dy;
    int lastX, lastY;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.dx = 0;
        this.dy = 0;
    }

    public void update() {
        lastX = x;
        lastY = y;
        x += dx;
        y += dy;

        // Boundary check
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > 760) x = 760;
        if (y > 560) y = 560;
    }

    public void undoMove() {
        x = lastX;
        y = lastY;
    }
}

class Monster {
    int x, y;
    int speed;

    public Monster(int x, int y) {
        this.x = x;
        this.y = y;
        this.speed = 2;
    }

    public void update(int playerX, int playerY) {
        // Simple AI: move towards player
        if (playerX > x) {
            x += speed;
        } else if (playerX < x) {
            x -= speed;
        }

        if (playerY > y) {
            y += speed;
        } else if (playerY < y) {
            y -= speed;
        }

        // Boundary check
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > 760) x = 760;
        if (y > 560) y = 560;
    }
}

class Obstacle {
    int x, y;

    public Obstacle(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

class HidingSpot extends Obstacle {
    public HidingSpot(int x, int y) {
        super(x, y);
    }
}

