import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.util.Random;

public class HorrorThemedPuzzleGame extends JFrame {
    private GamePanel gamePanel;
    private boolean gameOver = false;
    private boolean gameWon = false;

    public HorrorThemedPuzzleGame() {
        setTitle("Haunted Mansion Puzzle");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        gamePanel = new GamePanel();
        add(gamePanel);
        setVisible(true);
        gamePanel.startGame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HorrorThemedPuzzleGame());
    }

    class GamePanel extends JPanel implements ActionListener, KeyListener {
        private static final int TILE_SIZE = 32;
        private static final int MAP_WIDTH = 25;
        private static final int MAP_HEIGHT = 18;
        private static final int PLAYER_SPEED = 4;
        private static final int GHOST_SPEED = 2;
        private static final int ANIMATION_FRAMES = 4;
        private static final int ANIMATION_DELAY = 100;

        private char[][] map;
        private int playerX, playerY;
        private int ghostX, ghostY;
        private int keyX, keyY;
        private boolean hasKey = false;
        private int exitX, exitY;
        private Timer timer;
        private BufferedImage[] playerSprites;
        private BufferedImage[] ghostSprites;
        private BufferedImage wallImage, floorImage, keyImage, exitImage;
        private int playerFrame = 0;
        private int ghostFrame = 0;
        private long lastAnimationTime = 0;
        private boolean upPressed, upReleased, downPressed, downReleased, leftPressed, leftReleased, rightPressed, rightReleased;
        private Clip backgroundMusic;
        private Clip keySound, winSound, loseSound;
        private Random random;

        public GamePanel() {
            setFocusable(true);
            addKeyListener(this);
            timer = new Timer(16, this); // ~60 FPS
            random = new Random();
            initMap();
            loadResources();
        }

        private void initMap() {
            map = new char[][] {
                    {'#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#'},
                    {'#', ' ', ' ', ' ', ' ', '#', ' ', ' ', ' ', ' ', ' ', ' ', '#', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '#', ' ', ' ', ' ', '#'},
                    {'#', ' ', '#', '#', ' ', '#', ' ', '#', '#', '#', '#', ' ', '#', ' ', '#', '#', '#', '#', '#', ' ', '#', ' ', '#', ' ', '#'},
                    {'#', ' ', '#', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '#', ' ', '#', ' ', ' ', ' ', ' ', ' ', '#', ' ', '#', ' ', '#', ' ', '#'},
                    {'#', ' ', '#', ' ', '#', '#', '#', '#', '#', ' ', '#', ' ', ' ', ' ', '#', '#', '#', ' ', '#', ' ', '#', ' ', '#', ' ', '#'},
                    {'#', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '#', ' ', '#', ' ', '#', '#', '#', ' ', ' ', ' ', '#', ' ', ' ', ' ', ' ', ' ', '#'},
                    {'#', '#', '#', '#', '#', '#', ' ', '#', '#', ' ', '#', ' ', '#', ' ', ' ', ' ', '#', '#', '#', '#', '#', '#', '#', ' ', '#'},
                    {'#', ' ', ' ', ' ', ' ', '#', ' ', ' ', ' ', ' ', '#', ' ', '#', ' ', '#', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '#', ' ', '#'},
                    {'#', ' ', '#', '#', ' ', '#', '#', '#', '#', '#', '#', ' ', '#', ' ', '#', '#', '#', '#', '#', '#', '#', ' ', '#', ' ', '#'},
                    {'#', ' ', '#', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '#', ' ', '#', ' ', '#'},
                    {'#', ' ', '#', ' ', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', ' ', '#', ' ', '#', ' ', '#'},
                    {'#', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '#'},
                    {'#', '#', '#', '#', '#', '#', ' ', '#', '#', '#', '#', ' ', '#', ' ', '#', '#', '#', '#', '#', '#', '#', '#', '#', ' ', '#'},
                    {'#', ' ', ' ', ' ', ' ', '#', ' ', ' ', ' ', ' ', '#', ' ', '#', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '#', ' ', '#'},
                    {'#', ' ', '#', '#', ' ', '#', '#', '#', '#', ' ', '#', ' ', '#', '#', '#', '#', '#', '#', '#', ' ', '#', ' ', '#', ' ', '#'},
                    {'#', ' ', '#', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '#', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '#', ' ', '#', ' ', '#', ' ', '#'},
                    {'#', ' ', '#', ' ', '#', '#', '#', '#', '#', ' ', '#', ' ', '#', '#', '#', ' ', '#', ' ', '#', ' ', '#', ' ', '#', ' ', '#'},
                    {'#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#', '#'}
            };
            // Place player
            playerX = 1 * TILE_SIZE;
            playerY = 1 * TILE_SIZE;
            // Place ghost
            ghostX = 20 * TILE_SIZE;
            ghostY = 15 * TILE_SIZE;
            // Place key
            keyX = 10 * TILE_SIZE;
            keyY = 10 * TILE_SIZE;
            // Place exit
            exitX = 23 * TILE_SIZE;
            exitY = 1 * TILE_SIZE;
        }

        private void loadResources() {
            try {
                // Load sprites (placeholder paths; replace with actual images)
                playerSprites = new BufferedImage[ANIMATION_FRAMES];
                ghostSprites = new BufferedImage[ANIMATION_FRAMES];
                for (int i = 0; i < ANIMATION_FRAMES; i++) {
                    playerSprites[i] = createPlaceholderSprite(Color.BLUE, i);
                    ghostSprites[i] = createPlaceholderSprite(Color.RED, i);
                }
                wallImage = createPlaceholderTile(Color.DARK_GRAY);
                floorImage = createPlaceholderTile(Color.BLACK);
                keyImage = createPlaceholderSprite(Color.YELLOW, 0);
                exitImage = createPlaceholderTile(Color.GREEN);

                // Load sounds (placeholder paths; replace with actual .wav files)
                backgroundMusic = loadSound("assets/haunting.wav");
                keySound = loadSound("assets/key_pickup.wav");
                winSound = loadSound("assets/win.wav");
                loseSound = loadSound("assets/lose.wav");
                if (backgroundMusic != null) {
                    backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private BufferedImage createPlaceholderSprite(Color color, int frame) {
            BufferedImage image = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(color);
            g2d.fillOval(4 + frame, 4 + frame, TILE_SIZE - 8, TILE_SIZE - 8);
            g2d.dispose();
            return image;
        }

        private BufferedImage createPlaceholderTile(Color color) {
            BufferedImage image = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(color);
            g2d.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
            g2d.dispose();
            return image;
        }

        private Clip loadSound(String path) {
            try {
                File soundFile = new File(path);
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                return clip;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public void startGame() {
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            // Draw map
            for (int y = 0; y < MAP_HEIGHT; y++) {
                for (int x = 0; x < MAP_WIDTH; x++) {
                    BufferedImage tile = map[y][x] == '#' ? wallImage : floorImage;
                    g2d.drawImage(tile, x * TILE_SIZE, y * TILE_SIZE, null);
                }
            }

            // Draw key
            if (!hasKey) {
                g2d.drawImage(keyImage, keyX, keyY, null);
            }

            // Draw exit
            g2d.drawImage(exitImage, exitX, exitY, null);

            // Draw player
            g2d.drawImage(playerSprites[playerFrame], playerX, playerY, null);

            // Draw ghost
            g2d.drawImage(ghostSprites[ghostFrame], ghostX, ghostY, null);

            // Draw game over or win message
            if (gameOver) {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 40));
                g2d.drawString("Game Over!", 300, 300);
            } else if (gameWon) {
                g2d.setColor(Color.GREEN);
                g2d.setFont(new Font("Arial", Font.BOLD, 40));
                g2d.drawString("You Escaped!", 300, 300);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameOver || gameWon) {
                return;
            }

            updatePlayer();
            updateGhost();
            updateAnimations();
            checkCollisions();
            repaint();
        }

        private void updatePlayer() {
            int newX = playerX;
            int newY = playerY;

            if (upPressed && !downPressed) {
                newY -= PLAYER_SPEED;
            } else if (downPressed && !upPressed) {
                newY += PLAYER_SPEED;
            }
            if (leftPressed && !rightPressed) {
                newX -= PLAYER_SPEED;
            } else if (rightPressed && !leftPressed) {
                newX += PLAYER_SPEED;
            }

            // Check collision with walls
            if (!isCollision(newX, newY)) {
                playerX = newX;
                playerY = newY;
            }
        }

        private void updateGhost() {
            // Simple AI: move toward player with some randomness
            int dx = playerX - ghostX;
            int dy = playerY - ghostY;
            int newX = ghostX;
            int newY = ghostY;

            if (Math.abs(dx) > Math.abs(dy)) {
                newX += GHOST_SPEED * (dx > 0 ? 1 : -1) * (random.nextDouble() > 0.2 ? 1 : 0);
            } else {
                newY += GHOST_SPEED * (dy > 0 ? 1 : -1) * (random.nextDouble() > 0.2 ? 1 : 0);
            }

            // Check collision with walls
            if (!isCollision(newX, newY)) {
                ghostX = newX;
                ghostY = newY;
            }
        }

        private void updateAnimations() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAnimationTime >= ANIMATION_DELAY) {
                playerFrame = (playerFrame + 1) % ANIMATION_FRAMES;
                ghostFrame = (ghostFrame + 1) % ANIMATION_FRAMES;
                lastAnimationTime = currentTime;
            }
        }

        private boolean isCollision(int x, int y) {
            int tileX1 = x / TILE_SIZE;
            int tileY1 = y / TILE_SIZE;
            int tileX2 = (x + TILE_SIZE - 1) / TILE_SIZE;
            int tileY2 = (y + TILE_SIZE - 1) / TILE_SIZE;

            return tileX1 < 0 || tileX1 >= MAP_WIDTH || tileY1 < 0 || tileY1 >= MAP_HEIGHT ||
                    tileX2 < 0 || tileX2 >= MAP_WIDTH || tileY2 < 0 || tileY2 >= MAP_HEIGHT ||
                    map[tileY1][tileX1] == '#' || map[tileY1][tileX2] == '#' ||
                    map[tileY2][tileX1] == '#' || map[tileY2][tileX2] == '#';
        }

        private void checkCollisions() {
            // Check for key pickup
            if (!hasKey && Math.abs(playerX - keyX) < TILE_SIZE && Math.abs(playerY - keyY) < TILE_SIZE) {
                hasKey = true;
                if (keySound != null) {
                    keySound.start();
                }
            }

            // Check for exit
            if (hasKey && Math.abs(playerX - exitX) < TILE_SIZE && Math.abs(playerY - exitY) < TILE_SIZE) {
                gameWon = true;
                timer.stop();
                if (backgroundMusic != null) {
                    backgroundMusic.stop();
                }
                if (winSound != null) {
                    winSound.start();
                }
            }

            // Check for ghost collision
            if (Math.abs(playerX - ghostX) < TILE_SIZE && Math.abs(playerY - ghostY) < TILE_SIZE) {
                gameOver = true;
                timer.stop();
                if (backgroundMusic != null) {
                    backgroundMusic.stop();
                }
                if (loseSound != null) {
                    loseSound.start();
                }
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W:
                case KeyEvent.VK_UP:
                    upPressed = true;
                    upReleased = false;
                    break;
                case KeyEvent.VK_S:
                case KeyEvent.VK_DOWN:
                    downPressed = true;
                    downReleased = false;
                    break;
                case KeyEvent.VK_A:
                case KeyEvent.VK_LEFT:
                    leftPressed = true;
                    leftReleased = false;
                    break;
                case KeyEvent.VK_D:
                case KeyEvent.VK_RIGHT:
                    rightPressed = true;
                    rightReleased = false;
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W:
                case KeyEvent.VK_UP:
                    upReleased = true;
                    if (upReleased && !downPressed) {
                        upPressed = false;
                    }
                    break;
                case KeyEvent.VK_S:
                case KeyEvent.VK_DOWN:
                    downReleased = true;
                    if (downReleased && !upPressed) {
                        downPressed = false;
                    }
                    break;
                case KeyEvent.VK_A:
                case KeyEvent.VK_LEFT:
                    leftReleased = true;
                    if (leftReleased && !rightPressed) {
                        leftPressed = false;
                    }
                    break;
                case KeyEvent.VK_D:
                case KeyEvent.VK_RIGHT:
                    rightReleased = true;
                    if (rightReleased && !leftPressed) {
                        rightPressed = false;
                    }
                    break;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }
    }
}