import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import javax.sound.sampled.*;
import java.awt.image.BufferedImage;

public class HorrorMazeEscapeGame extends JFrame {
    private static final int CELL_SIZE = 40;
    private static final int MAZE_SIZE = 15;
    private static final int WINDOW_WIDTH = MAZE_SIZE * CELL_SIZE;
    private static final int WINDOW_HEIGHT = MAZE_SIZE * CELL_SIZE + 50;
    private static final int PLAYER_SPEED = 5;
    private static final int GHOST_SPEED = 3;
    private static final int ANIMATION_FRAMES = 4;

    private GamePanel gamePanel;
    private Timer gameTimer;
    private boolean isGameOver = false;
    private boolean isGameWon = false;
    private int score = 0;

    // Game entities
    private Player player;
    private Ghost ghost;
    private Cell[][] maze;
    private Point exitPoint;

    // Animation resources
    private BufferedImage[] playerFrames;
    private BufferedImage[] ghostFrames;
    private int currentPlayerFrame = 0;
    private int currentGhostFrame = 0;
    private long lastFrameUpdate;
    private Clip backgroundMusic;
    private Clip screamSound;

    // Game controls
    private boolean upPressed, downPressed, leftPressed, rightPressed;

    public HorrorMazeEscapeGame() {
        setTitle("Horror Maze Escape Game");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        initializeGame();
        gamePanel = new GamePanel();
        add(gamePanel);

        addKeyListener(new GameKeyListener());
        setFocusable(true);

        loadSounds();
        startGameLoop();
    }

    private void initializeGame() {
        maze = generateMaze();
        player = new Player(1, 1);
        ghost = new Ghost(MAZE_SIZE - 2, MAZE_SIZE - 2);
        exitPoint = new Point(MAZE_SIZE - 2, MAZE_SIZE - 2);
        loadAnimations();
    }

    private void loadAnimations() {
        playerFrames = new BufferedImage[ANIMATION_FRAMES];
        ghostFrames = new BufferedImage[ANIMATION_FRAMES];

        // Simulate loading animation frames (in a real game, load from files)
        for (int i = 0; i < ANIMATION_FRAMES; i++) {
            playerFrames[i] = new BufferedImage(CELL_SIZE, CELL_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = playerFrames[i].createGraphics();
            g2d.setColor(Color.BLUE);
            g2d.fillOval(5, 5, CELL_SIZE - 10, CELL_SIZE - 10);
            g2d.setColor(Color.WHITE);
            g2d.drawString("P" + i, 15, 25);
            g2d.dispose();

            ghostFrames[i] = new BufferedImage(CELL_SIZE, CELL_SIZE, BufferedImage.TYPE_INT_ARGB);
            g2d = ghostFrames[i].createGraphics();
            g2d.setColor(Color.RED);
            g2d.fillOval(5, 5, CELL_SIZE - 10, CELL_SIZE - 10);
            g2d.setColor(Color.BLACK);
            g2d.drawString("G" + i, 15, 25);
            g2d.dispose();
        }
    }

    private void loadSounds() {
        try {
            // Simulate loading sounds (in a real game, load actual .wav files)
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(
                    new ByteArrayInputStream(new byte[1000]));
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioStream);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);

            audioStream = AudioSystem.getAudioInputStream(
                    new ByteArrayInputStream(new byte[1000]));
            screamSound = AudioSystem.getClip();
            screamSound.open(audioStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startGameLoop() {
        gameTimer = new Timer(16, e -> {
            if (!isGameOver && !isGameWon) {
                updateGame();
                gamePanel.repaint();
            }
        });
        gameTimer.start();
    }

    private void updateGame() {
        updatePlayer();
        updateGhost();
        updateAnimations();
        checkCollisions();
        checkWinCondition();
        score++;
    }

    private void updatePlayer() {
        double newX = player.x;
        double newY = player.y;

        if (upPressed) newY -= PLAYER_SPEED;
        if (downPressed) newY += PLAYER_SPEED;
        if (leftPressed) newX -= PLAYER_SPEED;
        if (rightPressed) newX += PLAYER_SPEED;

        if (!isWallCollision(newX, newY, player.width, player.height)) {
            player.x = newX;
            player.y = newY;
        }
    }

    private void updateGhost() {
        double dx = player.x - ghost.x;
        double dy = player.y - ghost.y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            double moveX = (dx / distance) * GHOST_SPEED;
            double moveY = (dy / distance) * GHOST_SPEED;

            double newX = ghost.x + moveX;
            double newY = ghost.y + moveY;

            if (!isWallCollision(newX, newY, ghost.width, ghost.height)) {
                ghost.x = newX;
                ghost.y = newY;
            }
        }
    }

    private void updateAnimations() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameUpdate > 100) {
            currentPlayerFrame = (currentPlayerFrame + 1) % ANIMATION_FRAMES;
            currentGhostFrame = (currentGhostFrame + 1) % ANIMATION_FRAMES;
            lastFrameUpdate = currentTime;
        }
    }

    private boolean isWallCollision(double x, double y, int width, int height) {
        int cellX = (int)(x / CELL_SIZE);
        int cellY = (int)(y / CELL_SIZE);

        // Check all four corners of the entity
        int[] checkX = {(int)x, (int)(x + width - 1)};
        int[] checkY = {(int)y, (int)(y + height - 1)};

        for (int cx : checkX) {
            for (int cy : checkY) {
                int gridX = cx / CELL_SIZE;
                int gridY = cy / CELL_SIZE;

                if (gridX < 0 || gridX >= MAZE_SIZE || gridY < 0 || gridY >= MAZE_SIZE) {
                    return true;
                }

                if (maze[gridY][gridX].isWall) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkCollisions() {
        Rectangle playerRect = new Rectangle(
                (int)player.x, (int)player.y, player.width, player.height);
        Rectangle ghostRect = new Rectangle(
                (int)ghost.x, (int)ghost.y, ghost.width, ghost.height);

        if (playerRect.intersects(ghostRect)) {
            isGameOver = true;
            playScreamSound();
            backgroundMusic.stop();
        }
    }

    private void checkWinCondition() {
        Rectangle playerRect = new Rectangle(
                (int)player.x, (int)player.y, player.width, player.height);
        Rectangle exitRect = new Rectangle(
                exitPoint.x * CELL_SIZE, exitPoint.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);

        if (playerRect.intersects(exitRect)) {
            isGameWon = true;
            backgroundMusic.stop();
        }
    }

    private void playScreamSound() {
        if (screamSound != null) {
            screamSound.setFramePosition(0);
            screamSound.start();
        }
    }

    private Cell[][] generateMaze() {
        Cell[][] maze = new Cell[MAZE_SIZE][MAZE_SIZE];
        for (int y = 0; y < MAZE_SIZE; y++) {
            for (int x = 0; x < MAZE_SIZE; x++) {
                maze[y][x] = new Cell(true);
            }
        }

        Random rand = new Random();
        Stack<Point> stack = new Stack<>();
        maze[1][1].isWall = false;
        stack.push(new Point(1, 1));

        while (!stack.isEmpty()) {
            Point current = stack.peek();
            int x = current.x;
            int y = current.y;

            ArrayList<Point> neighbors = new ArrayList<>();
            if (x > 1 && maze[y][x-2].isWall) neighbors.add(new Point(x-2, y));
            if (x < MAZE_SIZE-2 && maze[y][x+2].isWall) neighbors.add(new Point(x+2, y));
            if (y > 1 && maze[y-2][x].isWall) neighbors.add(new Point(x, y-2));
            if (y < MAZE_SIZE-2 && maze[y+2][x].isWall) neighbors.add(new Point(x, y+2));

            if (neighbors.isEmpty()) {
                stack.pop();
            } else {
                Point next = neighbors.get(rand.nextInt(neighbors.size()));
                int nx = next.x;
                int ny = next.y;

                maze[ny][nx].isWall = false;
                maze[(y + ny)/2][(x + nx)/2].isWall = false;
                stack.push(next);
            }
        }

        maze[MAZE_SIZE-2][MAZE_SIZE-2].isWall = false;
        return maze;
    }

    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw maze
            for (int y = 0; y < MAZE_SIZE; y++) {
                for (int x = 0; x < MAZE_SIZE; x++) {
                    if (maze[y][x].isWall) {
                        g2d.setColor(Color.BLACK);
                        g2d.fillRect(x * CELL_SIZE, y * CELL_SIZE,
                                CELL_SIZE, CELL_SIZE);
                    } else {
                        g2d.setColor(Color.DARK_GRAY);
                        g2d.fillRect(x * CELL_SIZE, y * CELL_SIZE,
                                CELL_SIZE, CELL_SIZE);
                    }
                }
            }

            // Draw exit
            g2d.setColor(Color.GREEN);
            g2d.fillRect(exitPoint.x * CELL_SIZE, exitPoint.y * CELL_SIZE,
                    CELL_SIZE, CELL_SIZE);

            // Draw player
            g2d.drawImage(playerFrames[currentPlayerFrame],
                    (int)player.x, (int)player.y, null);

            // Draw ghost
            g2d.drawImage(ghostFrames[currentGhostFrame],
                    (int)ghost.x, (int)ghost.y, null);

            // Draw score
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Score: " + score, 10, MAZE_SIZE * CELL_SIZE + 30);

            // Draw game over or win message
            if (isGameOver) {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 40));
                g2d.drawString("Game Over!", WINDOW_WIDTH/2 - 100,
                        WINDOW_HEIGHT/2);
            } else if (isGameWon) {
                g2d.setColor(Color.GREEN);
                g2d.setFont(new Font("Arial", Font.BOLD, 40));
                g2d.drawString("You Escaped!", WINDOW_WIDTH/2 - 120,
                        WINDOW_HEIGHT/2);
            }
        }
    }

    private class GameKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W: upPressed = true; break;
                case KeyEvent.VK_S: downPressed = true; break;
                case KeyEvent.VK_A: leftPressed = true; break;
                case KeyEvent.VK_D: rightPressed = true; break;
                case KeyEvent.VK_R:
                    if (isGameOver || isGameWon) {
                        resetGame();
                    }
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W: upPressed = false; break;
                case KeyEvent.VK_S: downPressed = false; break;
                case KeyEvent.VK_A: leftPressed = false; break;
                case KeyEvent.VK_D: rightPressed = false; break;
            }
        }
    }

    private void resetGame() {
        isGameOver = false;
        isGameWon = false;
        score = 0;
        initializeGame();
        backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
    }

    private class Cell {
        boolean isWall;

        Cell(boolean isWall) {
            this.isWall = isWall;
        }
    }

    private class Player {
        double x, y;
        int width, height;

        Player(int gridX, int gridY) {
            this.x = gridX * CELL_SIZE + CELL_SIZE/4;
            this.y = gridY * CELL_SIZE + CELL_SIZE/4;
            this.width = CELL_SIZE/2;
            -'+        this.height = CELL_SIZE/2;
        }
    }

    private class Ghost {
        double x, y;
        int width, height;

        Ghost(int gridX, int gridY) {
            this.x = gridX * CELL_SIZE + CELL_SIZE/4;
            this.y = gridY * CELL_SIZE + CELL_SIZE/4;
            this.width = CELL_SIZE/2;
            this.height = CELL_SIZE/2;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new HorrorMazeEscapeGame().setVisible(true);
        });
    }
}