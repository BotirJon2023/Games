import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Random;

public class CursedArtifactHorrorGame extends JFrame {
    private GamePanel gamePanel;
    private boolean gameOver = false;
    private boolean gameWon = false;

    public CursedArtifactHorrorGame() {
        setTitle("Cursed Artifact Horror Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        gamePanel.startGame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CursedArtifactHorrorGame());
    }

    class GamePanel extends JPanel implements ActionListener, KeyListener {
        private static final int WIDTH = 800;
        private static final int HEIGHT = 600;
        private static final int TILE_SIZE = 32;
        private static final int PLAYER_SPEED = 4;
        private static final int GHOST_SPEED = 2;
        private static final int ANIMATION_SPEED = 150;

        private Timer timer;
        private Player player;
        private ArrayList<Ghost> ghosts;
        private Artifact artifact;
        private int[][] map;
        private BufferedImage wallImage, floorImage, playerSpriteSheet, ghostSpriteSheet, artifactImage;
        private Clip backgroundMusic, pickupSound, scareSound;
        private boolean lightsOn = true;
        private int flickerCounter = 0;
        private Random random = new Random();

        // Animation variables
        private int playerFrame = 0;
        private int ghostFrame = 0;
        private long lastAnimationTime = 0;

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            addKeyListener(this);

            loadResources();
            initializeMap();
            initializeGameObjects();
            timer = new Timer(16, this); // ~60 FPS
        }

        private void loadResources() {
            try {
                // Placeholder for images (replace with actual paths)
                wallImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = wallImage.createGraphics();
                g.setColor(Color.DARK_GRAY);
                g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
                g.dispose();

                floorImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
                g = floorImage.createGraphics();
                g.setColor(new Color(50, 30, 20));
                g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
                g.dispose();

                // Simulated sprite sheets (4 frames each direction)
                playerSpriteSheet = new BufferedImage(TILE_SIZE * 4, TILE_SIZE * 4, BufferedImage.TYPE_INT_ARGB);
                g = playerSpriteSheet.createGraphics();
                g.setColor(Color.BLUE);
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        g.fillRect(j * TILE_SIZE, i * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
                g.dispose();

                ghostSpriteSheet = new BufferedImage(TILE_SIZE * 4, TILE_SIZE * 4, BufferedImage.TYPE_INT_ARGB);
                g = ghostSpriteSheet.createGraphics();
                g.setColor(new Color(200, 200, 200, 150));
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        g.fillOval(j * TILE_SIZE, i * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
                g.dispose();

                artifactImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
                g = artifactImage.createGraphics();
                g.setColor(Color.YELLOW);
                g.fillOval(0, 0, TILE_SIZE, TILE_SIZE);
                g.dispose();

                // Load sounds (placeholder paths)
                AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(new byte[0]));
                backgroundMusic = AudioSystem.getClip();
                pickupSound = AudioSystem.getClip();
                scareSound = AudioSystem.getClip();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void initializeMap() {
            map = new int[][] {
                    {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
                    {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
                    {1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,0,1},
                    {1,0,0,0,1,0,0,0,1,0,0,0,1,0,0,0,1,0,0,0,1,0,1,0,1},
                    {1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,0,1,0,1},
                    {1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,1,0,1},
                    {1,0,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,0,1,1,1,0,1,0,1},
                    {1,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0,1},
                    {1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,0,1,0,1},
                    {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
                    {1,0,1,1,1,1,1,0,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,0,1},
                    {1,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,1},
                    {1,1,1,1,1,0,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,0,1},
                    {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
                    {1,0,1,1,1,1,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,1,1,0,1},
                    {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
                    {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
            };
        }

        private void initializeGameObjects() {
            player = new Player(2 * TILE_SIZE, 2 * TILE_SIZE, 0);
            ghosts = new ArrayList<>();
            ghosts.add(new Ghost(10 * TILE_SIZE, 10 * TILE_SIZE));
            ghosts.add(new Ghost(15 * TILE_SIZE, 5 * TILE_SIZE));
            ghosts.add(new Ghost(5 * TILE_SIZE, 15 * TILE_SIZE));
            artifact = new Artifact(20 * TILE_SIZE, 14 * TILE_SIZE);
        }

        public void startGame() {
            try {
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            } catch (Exception e) {
                e.printStackTrace();
            }
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Apply flickering light effect
            if (!lightsOn) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            }

            // Draw map
            for (int y = 0; y < map.length; y++) {
                for (int x = 0; x < map[0].length; x++) {
                    if (map[y][x] == 1) {
                        g2d.drawImage(wallImage, x * TILE_SIZE, y * TILE_SIZE, this);
                    } else {
                        g2d.drawImage(floorImage, x * TILE_SIZE, y * TILE_SIZE, this);
                    }
                }
            }

            // Draw artifact
            g2d.drawImage(artifactImage, artifact.x, artifact.y, this);

            // Draw ghosts
            for (Ghost ghost : ghosts) {
                int frameX = (ghostFrame % 4) * TILE_SIZE;
                int frameY = ghost.direction * TILE_SIZE;
                g2d.drawImage(ghostSpriteSheet.getSubimage(frameX, frameY, TILE_SIZE, TILE_SIZE),
                        ghost.x, ghost.y, this);
            }

            // Draw player
            int frameX = (playerFrame % 4) * TILE_SIZE;
            int frameY = player.direction * TILE_SIZE;
            g2d.drawImage(playerSpriteSheet.getSubimage(frameX, frameY, TILE_SIZE, TILE_SIZE),
                    player.x, player.y, this);

            // Draw game over or win screen
            if (gameOver) {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                g2d.drawString("Game Over!", WIDTH / 2 - 100, HEIGHT / 2);
            } else if (gameWon) {
                g2d.setColor(Color.GREEN);
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                g2d.drawString("You Won!", WIDTH / 2 - 100, HEIGHT / 2);
            }

            Toolkit.getDefaultToolkit().sync(); // Smooth animation on Linux[](https://zetcode.com/javagames/animation/)
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameOver || gameWon) return;

            // Update animation frames
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAnimationTime > ANIMATION_SPEED) {
                playerFrame = (playerFrame + 1) % 4;
                ghostFrame = (ghostFrame + 1) % 4;
                lastAnimationTime = currentTime;
            }

            // Update player
            player.update();

            // Update ghosts
            for (Ghost ghost : ghosts) {
                ghost.update();
            }

            // Flicker lights
            flickerCounter++;
            if (flickerCounter > 60 && random.nextInt(100) < 5) {
                lightsOn = !lightsOn;
                flickerCounter = 0;
                if (!lightsOn) {
                    try {
                        scareSound.setFramePosition(0);
                        scareSound.start();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            // Check collisions
            checkCollisions();

            repaint();
        }

        private void checkCollisions() {
            // Player-Artifact collision
            Rectangle playerRect = new Rectangle(player.x, player.y, TILE_SIZE, TILE_SIZE);
            Rectangle artifactRect = new Rectangle(artifact.x, artifact.y, TILE_SIZE, TILE_SIZE);
            if (playerRect.intersects(artifactRect)) {
                gameWon = true;
                try {
                    pickupSound.setFramePosition(0);
                    pickupSound.start();
                    backgroundMusic.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Player-Ghost collision
            for (Ghost ghost : ghosts) {
                Rectangle ghostRect = new Rectangle(ghost.x, ghost.y, TILE_SIZE, TILE_SIZE);
                if (playerRect.intersects(ghostRect)) {
                    gameOver = true;
                    try {
                        scareSound.setFramePosition(0);
                        scareSound.start();
                        backgroundMusic.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                    player.direction = 0;
                    player.dy = -PLAYER_SPEED;
                    break;
                case KeyEvent.VK_DOWN:
                    player.direction = 1;
                    player.dy = PLAYER_SPEED;
                    break;
                case KeyEvent.VK_LEFT:
                    player.direction = 2;
                    player.dx = -PLAYER_SPEED;
                    break;
                case KeyEvent.VK_RIGHT:
                    player.direction = 3;
                    player.dx = PLAYER_SPEED;
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                case KeyEvent.VK_DOWN:
                    player.dy = 0;
                    break;
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_RIGHT:
                    player.dx = 0;
                    break;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {}

        class Player {
            int x, y, dx, dy, direction;

            public Player(int x, int y, int direction) {
                this.x = x;
                this.y = y;
                this.direction = direction;
            }

            public void update() {
                int newX = x + dx;
                int newY = y + dy;

                if (!isCollision(newX, newY)) {
                    x = newX;
                    y = newY;
                }
            }

            private boolean isCollision(int x, int y) {
                int mapX = x / TILE_SIZE;
                int mapY = y / TILE_SIZE;
                if (mapX < 0 || mapX >= map[0].length || mapY < 0 || mapY >= map.length) {
                    return true;
                }
                return map[mapY][mapX] == 1;
            }
        }

        class Ghost {
            int x, y, direction;
            long lastMoveTime;

            public Ghost(int x, int y) {
                this.x = x;
                this.y = y;
                this.direction = random.nextInt(4);
                this.lastMoveTime = System.currentTimeMillis();
            }

            public void update() {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMoveTime < 500) return;

                // Move towards player
                int dx = player.x - x;
                int dy = player.y - y;
                int newX = x;
                int newY = y;

                if (Math.abs(dx) > Math.abs(dy)) {
                    newX += dx > 0 ? GHOST_SPEED : -GHOST_SPEED;
                    direction = dx > 0 ? 3 : 2;
                } else {
                    newY += dy > 0 ? GHOST_SPEED : -GHOST_SPEED;
                    direction = dy > 0 ? 1 : 0;
                }

                if (!isCollision(newX, newY)) {
                    x = newX;
                    y = newY;
                } else {
                    // Random direction if blocked
                    direction = random.nextInt(4);
                }

                lastMoveTime = currentTime;
            }

            private boolean isCollision(int x, int y) {
                int mapX = x / TILE_SIZE;
                int mapY = y / TILE_SIZE;
                if (mapX < 0 || mapX >= map[0].length || mapY < 0 || mapY >= map.length) {
                    return true;
                }
                return map[mapY][mapX] == 1;
            }
        }

        class Artifact {
            int x, y;

            public Artifact(int x, int y) {
                this.x = x;
                this.y = y;
            }
        }
    }
}