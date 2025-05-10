import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;

public class GhostHuntingGame extends JPanel implements ActionListener {
    // Game constants
    private static final int TILE_SIZE = 32;
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    private static final int FPS = 60;
    private static final int PAC_ANIM_DELAY = 4;
    private static final int GHOST_ANIM_COUNT = 2;
    private static final int PLAYER_ANIM_COUNT = 4;
    private static final int MAX_GHOSTS = 4;
    private static final int PLAYER_SPEED = 4;
    private static final int GHOST_SPEED = 3;

    // Game state
    private boolean inGame = false;
    private boolean gameWon = false;
    private int score = 0;
    private int lives = 3;
    private int pacAnimCount = PAC_ANIM_DELAY;
    private int pacAnimDir = 1;
    private int pacAnimPos = 0;

    // Game objects
    private Player player;
    private ArrayList<Ghost> ghosts;
    private ArrayList<Point> points;
    private BufferedImage[] playerImagesUp, playerImagesDown, playerImagesLeft, playerImagesRight;
    private BufferedImage[] ghostImages;
    private BufferedImage pointImage;
    private BufferedImage mazeImage;
    private Clip backgroundMusic;
    private Clip pointSound;
    private Clip deathSound;

    // Maze layout (1=wall, 0=path, 2=point)
    private final int[][] mazeData = {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,2,2,2,2,2,2,2,2,2,2,2,1,2,2,2,2,2,2,2,2,2,2,2,1},
            {1,2,1,1,1,1,2,1,1,1,1,2,1,2,1,1,1,1,2,1,1,1,1,2,1},
            {1,2,1,0,0,0,2,0,0,0,0,2,1,2,0,0,0,0,2,0,0,0,1,2,1},
            {1,2,1,0,1,1,2,1,1,1,1,2,1,2,1,1,1,1,2,1,1,0,1,2,1},
            {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1},
            {1,2,1,1,1,1,2,1,1,1,1,2,1,2,1,1,1,1,2,1,1,1,1,2,1},
            {1,2,1,0,0,0,2,0,0,0,0,2,1,2,0,0,0,0,2,0,0,0,1,2,1},
            {1,2,1,0,1,1,2,1,1,1,1,2,1,2,1,1,1,1,2,1,1,0,1,2,1},
            {1,2,2,2,2,2,2,2,2,2,2,2,1,2,2,2,2,2,2,2,2,2,2,2,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };

    // Double buffering
    private BufferedImage backbuffer;
    private Graphics2D backbufferGraphics;

    // Timer for game loop
    private Timer timer;

    public GhostHuntingGame() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setFocusable(true);
        initGame();
        loadResources();
        setupInput();
    }

    private void initGame() {
        backbuffer = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB);
        backbufferGraphics = backbuffer.createGraphics();
        player = new Player(12 * TILE_SIZE, 9 * TILE_SIZE);
        ghosts = new ArrayList<>();
        points = new ArrayList<>();
        initMaze();
        timer = new Timer(1000 / FPS, this);
        timer.start();
    }

    private void loadResources() {
        try {
            // Load player images (4 frames per direction)
            playerImagesUp = new BufferedImage[PLAYER_ANIM_COUNT];
            playerImagesDown = new BufferedImage[PLAYER_ANIM_COUNT];
            playerImagesLeft = new BufferedImage[PLAYER_ANIM_COUNT];
            playerImagesRight = new BufferedImage[PLAYER_ANIM_COUNT];
            for (int i = 0; i < PLAYER_ANIM_COUNT; i++) {
                playerImagesUp[i] = ImageIO.read(getClass().getResource("/player_up_" + i + ".png"));
                playerImagesDown[i] = ImageIO.read(getClass().getResource("/player_down_" + i + ".png"));
                playerImagesLeft[i] = ImageIO.read(getClass().getResource("/player_left_" + i + ".png"));
                playerImagesRight[i] = ImageIO.read(getClass().getResource("/player_right_" + i + ".png"));
            }

            // Load ghost images (2 frames)
            ghostImages = new BufferedImage[GHOST_ANIM_COUNT];
            for (int i = 0; i < GHOST_ANIM_COUNT; i++) {
                ghostImages[i] = ImageIO.read(getClass().getResource("/ghost_" + i + ".png"));
            }

            // Load point and maze images
            pointImage = ImageIO.read(getClass().getResource("/point.png"));
            mazeImage = ImageIO.read(getClass().getResource("/maze.png"));

            // Load sounds
            backgroundMusic = loadClip("/background.wav");
            pointSound = loadClip("/point.wav");
            deathSound = loadClip("/death.wav");
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
            e.printStackTrace();
        }
    }

    private Clip loadClip(String path) throws IOException, LineUnavailableException, UnsupportedAudioFileException {
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(getClass().getResource(path));
        Clip clip = AudioSystem.getClip();
        clip.open(audioIn);
        return clip;
    }

    private void setupInput() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!inGame) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        startGame();
                    }
                    return;
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        player.setDirection(Direction.LEFT);
                        break;
                    case KeyEvent.VK_RIGHT:
                        player.setDirection(Direction.RIGHT);
                        break;
                    case KeyEvent.VK_UP:
                        player.setDirection(Direction.UP);
                        break;
                    case KeyEvent.VK_DOWN:
                        player.setDirection(Direction.DOWN);
                        break;
                    case KeyEvent.VK_ESCAPE:
                        System.exit(0);
                        break;
                }
            }
        });
    }

    private void initMaze() {
        points.clear();
        ghosts.clear();
        for (int y = 0; y < mazeData.length; y++) {
            for (int x = 0; x < mazeData[y].length; x++) {
                if (mazeData[y][x] == 2) {
                    points.add(new Point(x * TILE_SIZE, y * TILE_SIZE));
                }
            }
        }
        // Initialize ghosts
        ghosts.add(new Ghost(6 * TILE_SIZE, 5 * TILE_SIZE));
        ghosts.add(new Ghost(18 * TILE_SIZE, 5 * TILE_SIZE));
        ghosts.add(new Ghost(6 * TILE_SIZE, 7 * TILE_SIZE));
        ghosts.add(new Ghost(18 * TILE_SIZE, 7 * TILE_SIZE));
    }

    private void startGame() {
        inGame = true;
        gameWon = false;
        score = 0;
        lives = 3;
        player.reset(12 * TILE_SIZE, 9 * TILE_SIZE);
        initMaze();
        backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (inGame) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        // Update player
        player.move();
        checkCollisions();

        // Update ghosts
        for (Ghost ghost : ghosts) {
            ghost.move(player.x, player.y);
        }

        // Update animation
        pacAnimCount--;
        if (pacAnimCount <= 0) {
            pacAnimCount = PAC_ANIM_DELAY;
            pacAnimPos = pacAnimPos + pacAnimDir;
            if (pacAnimPos == (PLAYER_ANIM_COUNT - 1) || pacAnimPos == 0) {
                pacAnimDir = -pacAnimDir;
            }
        }

        // Check win condition
        if (points.isEmpty()) {
            gameWon = true;
            inGame = false;
            backgroundMusic.stop();
        }
    }

    private void checkCollisions() {
        // Check wall collisions
        Rectangle playerRect = new Rectangle(player.x, player.y, TILE_SIZE, TILE_SIZE);
        for (int y = 0; y < mazeData.length; y++) {
            for (int x = 0; x < mazeData[y].length; x++) {
                if (mazeData[y][x] == 1) {
                    Rectangle wallRect = new Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    if (playerRect.intersects(wallRect)) {
                        player.stop();
                    }
                }
            }
        }

        // Check point collisions
        for (int i = points.size() - 1; i >= 0; i--) {
            Point point = points.get(i);
            Rectangle pointRect = new Rectangle(point.x, point.y, TILE_SIZE, TILE_SIZE);
            if (playerRect.intersects(pointRect)) {
                points.remove(i);
                score += 10;
                pointSound.setFramePosition(0);
                pointSound.start();
            }
        }

        // Check ghost collisions
        for (Ghost ghost : ghosts) {
            Rectangle ghostRect = new Rectangle(ghost.x, ghost.y, TILE_SIZE, TILE_SIZE);
            if (playerRect.intersects(ghostRect)) {
                lives--;
                deathSound.setFramePosition(0);
                deathSound.start();
                if (lives <= 0) {
                    inGame = false;
                    backgroundMusic.stop();
                } else {
                    player.reset(12 * TILE_SIZE, 9 * TILE_SIZE);
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw to backbuffer
        backbufferGraphics.setColor(Color.BLACK);
        backbufferGraphics.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // Draw maze
        backbufferGraphics.drawImage(mazeImage, 0, 0, this);

        // Draw points
        for (Point point : points) {
            backbufferGraphics.drawImage(pointImage, point.x, point.y, this);
        }

        // Draw player
        BufferedImage playerImage;
        switch (player.direction) {
            case UP:
                playerImage = playerImagesUp[pacAnimPos];
                break;
            case DOWN:
                playerImage = playerImagesDown[pacAnimPos];
                break;
            case LEFT:
                playerImage = playerImagesLeft[pacAnimPos];
                break;
            case RIGHT:
            default:
                playerImage = playerImagesRight[pacAnimPos];
                break;
        }
        backbufferGraphics.drawImage(playerImage, player.x, player.y, this);

        // Draw ghosts
        for (Ghost ghost : ghosts) {
            backbufferGraphics.drawImage(ghostImages[pacAnimPos % GHOST_ANIM_COUNT], ghost.x, ghost.y, this);
        }

        // Draw HUD
        backbufferGraphics.setColor(Color.WHITE);
        backbufferGraphics.setFont(new Font("Helvetica", Font.BOLD, 16));
        backbufferGraphics.drawString("Score: " + score, 10, 20);
        backbufferGraphics.drawString("Lives: " + lives, SCREEN_WIDTH - 100, 20);

        // Draw game over or win screen
        if (!inGame) {
            backbufferGraphics.setFont(new Font("Helvetica", Font.BOLD, 24));
            String message = gameWon ? "You Win!" : "Game Over";
            backbufferGraphics.drawString(message, SCREEN_WIDTH / 2 - 50, SCREEN_HEIGHT / 2);
            backbufferGraphics.setFont(new Font("Helvetica", Font.PLAIN, 16));
            backbufferGraphics.drawString("Press SPACE to start", SCREEN_WIDTH / 2 - 80, SCREEN_HEIGHT / 2 + 30);
        }

        // Draw backbuffer to screen
        g.drawImage(backbuffer, 0, 0, this);
    }

    // Player class
    private class Player {
        int x, y;
        Direction direction;
        int dx, dy;

        Player(int x, int y) {
            this.x = x;
            this.y = y;
            this.direction = Direction.RIGHT;
            this.dx = PLAYER_SPEED;
            this.dy = 0;
        }

        void setDirection(Direction dir) {
            direction = dir;
            switch (dir) {
                case LEFT:
                    dx = -PLAYER_SPEED;
                    dy = 0;
                    break;
                case RIGHT:
                    dx = PLAYER_SPEED;
                    dy = 0;
                    break;
                case UP:
                    dx = 0;
                    dy = -PLAYER_SPEED;
                    break;
                case DOWN:
                    dx = 0;
                    dy = PLAYER_SPEED;
                    break;
            }
        }

        void move() {
            x += dx;
            y += dy;
            // Keep player in bounds
            x = Math.max(0, Math.min(x, SCREEN_WIDTH - TILE_SIZE));
            y = Math.max(0, Math.min(y, SCREEN_HEIGHT - TILE_SIZE));
        }

        void stop() {
            dx = 0;
            dy = 0;
        }

        void reset(int x, int y) {
            this.x = x;
            this.y = y;
            setDirection(Direction.RIGHT);
        }
    }

    // Ghost class
    private class Ghost {
        int x, y;
        int dx, dy;
        Random random = new Random();

        Ghost(int x, int y) {
            this.x = x;
            this.y = y;
            this.dx = GHOST_SPEED;
            this.dy = 0;
        }

        void move(int playerX, int playerY) {
            // Simple AI: chase player with occasional random movement
            if (random.nextInt(10) < 7) {
                // Move toward player
                if (playerX > x) {
                    dx = GHOST_SPEED;
                } else if (playerX < x) {
                    dx = -GHOST_SPEED;
                } else {
                    dx = 0;
                }
                if (playerY > y) {
                    dy = GHOST_SPEED;
                } else if (playerY < y) {
                    dy = -GHOST_SPEED;
                } else {
                    dy = 0;
                }
            } else {
                // Random movement
                dx = random.nextInt(3) - 1 * GHOST_SPEED;
                dy = random.nextInt(3) - 1 * GHOST_SPEED;
            }

            x += dx;
            y += dy;

            // Check wall collisions
            for (int my = 0; my < mazeData.length; my++) {
                for (int mx = 0; mx < mazeData[my].length; mx++) {
                    if (mazeData[my][mx] == 1) {
                        Rectangle wallRect = new Rectangle(mx * TILE_SIZE, my * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        Rectangle ghostRect = new Rectangle(x + dx, y + dy, TILE_SIZE, TILE_SIZE);
                        if (ghostRect.intersects(wallRect)) {
                            dx = -dx;
                            dy = -dy;
                            x += dx;
                            y += dy;
                            break;
                        }
                    }
                }
            }

            // Keep ghost in bounds
            x = Math.max(0, Math.min(x, SCREEN_WIDTH - TILE_SIZE));
            y = Math.max(0, Math.min(y, SCREEN_HEIGHT - TILE_SIZE));
        }
    }

    // Point class for collectibles
    private class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Direction enum
    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Ghost Hunting Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new GhostHuntingGame());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}