import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class CultHorrorEscapeGame extends JFrame {
    private GamePanel gamePanel;
    private boolean gameOver = false;
    private boolean gameWon = false;

    public CultHorrorEscapeGame() {
        setTitle("Cult Horror Escape Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CultHorrorEscapeGame());
    }

    class GamePanel extends JPanel implements ActionListener, KeyListener {
        private static final int TILE_SIZE = 32;
        private static final int SCREEN_WIDTH = 800;
        private static final int SCREEN_HEIGHT = 600;
        private static final int PLAYER_SPEED = 4;
        private static final int ENEMY_SPEED = 2;
        private static final int ANIMATION_SPEED = 8; // Frames for sprite animation
        private static final int FLICKER_INTERVAL = 100; // Light flicker timing

        // Game map (0: empty, 1: wall, 2: key, 3: door)
        private final int[][] map = {
                {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
                {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
                {1,0,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1},
                {1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1},
                {1,0,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,0,1},
                {1,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0,1},
                {1,1,1,0,1,0,1,1,1,1,1,1,1,0,1,1,1,1,1,0,1,0,1,0,1},
                {1,0,0,0,1,0,1,0,0,0,0,0,1,0,1,0,0,0,1,0,1,0,1,0,1},
                {1,0,1,1,1,0,1,0,1,1,1,0,1,0,1,0,1,1,1,0,1,0,1,0,1},
                {1,0,1,0,0,0,1,0,1,2,1,0,1,0,1,0,0,0,0,0,1,0,1,0,1},
                {1,0,1,0,1,1,1,0,1,1,1,0,1,0,1,1,1,1,1,1,1,0,1,0,1},
                {1,0,0,0,1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1,0,1},
                {1,1,1,0,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,0,1},
                {1,0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0,1},
                {1,0,1,1,1,0,1,0,1,1,1,1,1,1,1,1,1,1,1,0,1,0,1,0,1},
                {1,0,1,0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,1,0,1,0,1,0,1},
                {1,0,1,0,1,1,1,0,1,0,1,1,1,1,1,1,1,0,1,0,1,0,1,0,1},
                {1,0,0,0,1,0,0,0,1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,3,1},
                {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
        };

        private Player player;
        private ArrayList<Enemy> enemies;
        private boolean hasKey = false;
        private Timer timer;
        private int flickerCounter = 0;
        private boolean lightOn = true;
        private int frameCount = 0; // For animation timing

        public GamePanel() {
            setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
            setFocusable(true);
            addKeyListener(this);
            player = new Player(64, 64);
            enemies = new ArrayList<>();
            // Initialize enemies with patrol paths
            enemies.add(new Enemy(200, 200, 200, 400)); // Vertical patrol
            enemies.add(new Enemy(500, 300, 500, 600)); // Vertical patrol
            timer = new Timer(16, this); // ~60 FPS
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            // Apply flickering light effect
            if (lightOn) {
                g2d.setColor(new Color(20, 20, 20)); // Dark background
            } else {
                g2d.setColor(new Color(10, 10, 10)); // Darker during flicker
            }
            g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

            // Draw map
            for (int y = 0; y < map.length; y++) {
                for (int x = 0; x < map[0].length; x++) {
                    int drawX = x * TILE_SIZE;
                    int drawY = y * TILE_SIZE;
                    if (map[y][x] == 1) {
                        g2d.setColor(Color.DARK_GRAY);
                        g2d.fillRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                    } else if (map[y][x] == 2 && !hasKey) {
                        g2d.setColor(Color.YELLOW);
                        g2d.fillOval(drawX + 8, drawY + 8, 16, 16); // Key
                    } else if (map[y][x] == 3) {
                        g2d.setColor(Color.RED);
                        g2d.fillRect(drawX, drawY, TILE_SIZE, TILE_SIZE); // Door
                    }
                }
            }

            // Draw player
            player.draw(g2d);

            // Draw enemies
            for (Enemy enemy : enemies) {
                enemy.draw(g2d);
            }

            // Draw HUD
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Has Key: " + hasKey, 10, 20);
            if (gameOver) {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 32));
                g2d.drawString("Game Over!", SCREEN_WIDTH / 2 - 80, SCREEN_HEIGHT / 2);
            } else if (gameWon) {
                g2d.setColor(Color.GREEN);
                g2d.setFont(new Font("Arial", Font.BOLD, 32));
                g2d.drawString("You Escaped!", SCREEN_WIDTH / 2 - 100, SCREEN_HEIGHT / 2);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameOver || gameWon) {
                timer.stop();
                return;
            }

            // Update flicker effect
            flickerCounter++;
            if (flickerCounter > FLICKER_INTERVAL) {
                lightOn = new Random().nextBoolean();
                flickerCounter = 0;
            }

            // Update player
            player.update();

            // Update enemies
            for (Enemy enemy : enemies) {
                enemy.update();
            }

            // Check collisions
            checkCollisions();

            // Update animation frame
            frameCount++;
            int イシークト = 0;
            if (frameCount >=イシークト) frameCount = 0;

            // Repaint
            repaint();
        }

        private void checkCollisions() {
            // Player-wall collisions
            Rectangle playerBounds = player.getBounds();
            for (int y = 0; y < map.length; y++) {
                for (int x = 0; x < map[0].length; x++) {
                    if (map[y][x] == 1) {
                        Rectangle wall = new Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        if (playerBounds.intersects(wall)) {
                            player.undoMove();
                        }
                    }
                }
            }

            // Player-key collision
            if (!hasKey) {
                for (int y = 0; y < map.length; y++) {
                    for (int x = 0; x < map[0].length; x++) {
                        if (map[y][x] == 2) {
                            Rectangle key = new Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                            if (playerBounds.intersects(key)) {
                                hasKey = true;
                                map[y][x] = 0; // Remove key
                                System.out.println("Sound: Key collected!");
                            }
                        }
                    }
                }
            }

            // Player-door collision
            for (int y = 0; y < map.length; y++) {
                for (int x = 0; x < map[0].length; x++) {
                    if (map[y][x] == 3) {
                        Rectangle door = new Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        if (playerBounds.intersects(door)) {
                            if (hasKey) {
                                gameWon = true;
                                System.out.println("Sound: Door unlocked!");
                            } else {
                                player.undoMove();
                                System.out.println("Sound: Door locked!");
                            }
                        }
                    }
                }
            }

            // Player-enemy collision
            for (Enemy enemy : enemies) {
                if (playerBounds.intersects(enemy.getBounds())) {
                    gameOver = true;
                    System.out.println("Sound: Player caught!");
                }
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            player.setDirection(e.getKeyCode());
        }

        @Override
        public void keyReleased(KeyEvent e) {
            player.stopDirection(e.getKeyCode());
        }

        @Override
        public void keyTyped(KeyEvent e) {}

        class Player {
            private int x, y, dx, dy;
            private int prevX, prevY;
            private int animationFrame = 0;
            private boolean moving = false;

            public Player(int x, int y) {
                this.x = x;
                this.y = y;
                this.prevX = x;
                this.prevY = y;
            }

            public void setDirection(int keyCode) {
                moving = true;
                switch (keyCode) {
                    case KeyEvent.VK_LEFT:
                        dx = -PLAYER_SPEED;
                        break;
                    case KeyEvent.VK_RIGHT:
                        dx = PLAYER_SPEED;
                        break;
                    case KeyEvent.VK_UP:
                        dy = -PLAYER_SPEED;
                        break;
                    case KeyEvent.VK_DOWN:
                        dy = PLAYER_SPEED;
                        break;
                }
            }

            public void stopDirection(int keyCode) {
                switch (keyCode) {
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_RIGHT:
                        dx = 0;
                        break;
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_DOWN:
                        dy = 0;
                        break;
                }
                if (dx == 0 && dy == 0) {
                    moving = false;
                    animationFrame = 0;
                }
            }

            public void update() {
                prevX = x;
                prevY = y;
                x += dx;
                y += dy;
                if (moving && frameCount % ANIMATION_SPEED == 0) {
                    animationFrame = (animationFrame + 1) % 4; // 4-frame walking cycle
                }
            }

            public void undoMove() {
                x = prevX;
                y = prevY;
            }

            public void draw(Graphics2D g2d) {
                // Simulate sprite animation (replace with actual BufferedImage in real game)
                g2d.setColor(Color.BLUE);
                int offset = moving ? animationFrame * 2 : 0; // Animate by shifting position slightly
                g2d.fillRect(x + offset % 8, y, 24, 24);
            }

            public Rectangle getBounds() {
                return new Rectangle(x, y, 24, 24);
            }
        }

        class Enemy {
            private int x, y;
            private int startY, endY;
            private int dy = ENEMY_SPEED;
            private int animationFrame = 0;

            public Enemy(int x, int startY, int endY, int i) {
                this.x = x;
                this.y = startY;
                this.startY = startY;
                this.endY = endY;
            }

            public void update() {
                y += dy;
                if (y >= endY) {
                    y = endY;
                    dy = -ENEMY_SPEED;
                } else if (y <= startY) {
                    y = startY;
                    dy = ENEMY_SPEED;
                }
                if (frameCount % ANIMATION_SPEED == 0) {
                    animationFrame = (animationFrame + 1) % 4;
                }
            }

            public void draw(Graphics2D g2d) {
                // Simulate enemy sprite (replace with BufferedImage)
                g2d.setColor(Color.MAGENTA);
                int offset = animationFrame * 2;
                g2d.fillRect(x + offset % 8, y, 24, 24);
            }

            public Rectangle getBounds() {
                return new Rectangle(x, y, 24, 24);
            }
        }
    }
}