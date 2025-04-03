import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class EscapeFromAHauntedCarnival extends JFrame {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int PLAYER_SIZE = 30;
    private static final int GHOST_SIZE = 30;
    private static final int EXIT_SIZE = 40;
    private static final int MOVE_SPEED = 5;
    private static final int GHOST_SPEED = 2;

    private GamePanel gamePanel;
    private Timer timer;
    private boolean gameOver = false;
    private boolean gameWon = false;

    public EscapeFromAHauntedCarnival() {
        setTitle("Escape From A Haunted Carnival");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        timer = new Timer(20, e -> {
            gamePanel.updateGame();
            gamePanel.repaint();
        });
        timer.start();

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EscapeFromAHauntedCarnival::new);
    }

    class GamePanel extends JPanel {
        private Player player;
        private ArrayList<Ghost> ghosts;
        private Exit exit;
        private Random random;

        public GamePanel() {
            setFocusable(true);
            random = new Random();
            player = new Player(50, 50);
            ghosts = new ArrayList<>();
            exit = new Exit(WINDOW_WIDTH - 100, WINDOW_HEIGHT - 100);

            // Spawn 5 ghosts at random positions
            for (int i = 0; i < 5; i++) {
                int x = random.nextInt(WINDOW_WIDTH - GHOST_SIZE);
                int y = random.nextInt(WINDOW_HEIGHT - GHOST_SIZE);
                ghosts.add(new Ghost(x, y));
            }

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    player.move(e);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawBackground(g);
            player.draw(g);
            for (Ghost ghost : ghosts) {
                ghost.draw(g);
            }
            exit.draw(g);

            if (gameOver) {
                drawGameOver(g);
            } else if (gameWon) {
                drawGameWon(g);
            }
        }

        private void drawBackground(Graphics g) {
            g.setColor(new Color(50, 50, 50)); // Dark gray carnival ground
            g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

            // Draw some carnival-themed decorations
            g.setColor(Color.RED);
            g.fillRect(100, 100, 50, 50); // Tent
            g.setColor(Color.YELLOW);
            g.fillOval(200, 200, 30, 30); // Balloon
            g.setColor(Color.BLUE);
            g.fillRect(300, 400, 60, 20); // Carnival stall
        }

        private void drawGameOver(Graphics g) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("Game Over!", WINDOW_WIDTH / 2 - 100, WINDOW_HEIGHT / 2);
        }

        private void drawGameWon(Graphics g) {
            g.setColor(Color.GREEN);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("You Escaped!", WINDOW_WIDTH / 2 - 120, WINDOW_HEIGHT / 2);
        }

        public void updateGame() {
            if (gameOver || gameWon) return;

            // Update player position
            player.update();

            // Update ghost positions
            for (Ghost ghost : ghosts) {
                ghost.moveToward(player);
            }

            // Check collisions with ghosts
            for (Ghost ghost : ghosts) {
                if (player.collidesWith(ghost)) {
                    gameOver = true;
                    timer.stop();
                }
            }

            // Check if player reached the exit
            if (player.collidesWith(exit)) {
                gameWon = true;
                timer.stop();
            }
        }
    }

    class Player {
        private int x, y;
        private int dx, dy;

        public Player(int x, int y) {
            this.x = x;
            this.y = y;
            this.dx = 0;
            this.dy = 0;
        }

        public void move(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                    dy = -MOVE_SPEED;
                    break;
                case KeyEvent.VK_DOWN:
                    dy = MOVE_SPEED;
                    break;
                case KeyEvent.VK_LEFT:
                    dx = -MOVE_SPEED;
                    break;
                case KeyEvent.VK_RIGHT:
                    dx = MOVE_SPEED;
                    break;
            }
        }

        public void update() {
            x += dx;
            y += dy;

            // Keep player within bounds
            if (x < 0) x = 0;
            if (x > WINDOW_WIDTH - PLAYER_SIZE) x = WINDOW_WIDTH - PLAYER_SIZE;
            if (y < 0) y = 0;
            if (y > WINDOW_HEIGHT - PLAYER_SIZE) y = WINDOW_HEIGHT - PLAYER_SIZE;

            // Reset movement
            dx = 0;
            dy = 0;
        }

        public void draw(Graphics g) {
            g.setColor(Color.GREEN);
            g.fillOval(x, y, PLAYER_SIZE, PLAYER_SIZE);
            g.setColor(Color.BLACK);
            g.drawOval(x, y, PLAYER_SIZE, PLAYER_SIZE);
        }

        public boolean collidesWith(Ghost ghost) {
            Rectangle playerRect = new Rectangle(x, y, PLAYER_SIZE, PLAYER_SIZE);
            Rectangle ghostRect = new Rectangle(ghost.x, ghost.y, GHOST_SIZE, GHOST_SIZE);
            return playerRect.intersects(ghostRect);
        }

        public boolean collidesWith(Exit exit) {
            Rectangle playerRect = new Rectangle(x, y, PLAYER_SIZE, PLAYER_SIZE);
            Rectangle exitRect = new Rectangle(exit.x, exit.y, EXIT_SIZE, EXIT_SIZE);
            return playerRect.intersects(exitRect);
        }
    }

    class Ghost {
        private int x, y;

        public Ghost(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void moveToward(Player player) {
            if (player.x > x) x += GHOST_SPEED;
            if (player.x < x) x -= GHOST_SPEED;
            if (player.y > y) y += GHOST_SPEED;
            if (player.y < y) y -= GHOST_SPEED;

            // Keep ghost within bounds
            if (x < 0) x = 0;
            if (x > WINDOW_WIDTH - GHOST_SIZE) x = WINDOW_WIDTH - GHOST_SIZE;
            if (y < 0) y = 0;
            if (y > WINDOW_HEIGHT - GHOST_SIZE) y = WINDOW_HEIGHT - GHOST_SIZE;
        }

        public void draw(Graphics g) {
            g.setColor(Color.WHITE);
            g.fillOval(x, y, GHOST_SIZE, GHOST_SIZE);
            g.setColor(Color.BLACK);
            g.drawOval(x, y, GHOST_SIZE, GHOST_SIZE);
            g.fillOval(x + 8, y + 8, 5, 5); // Left eye
            g.fillOval(x + 17, y + 8, 5, 5); // Right eye
        }
    }

    class Exit {
        private int x, y;

        public Exit(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void draw(Graphics g) {
            g.setColor(Color.YELLOW);
            g.fillRect(x, y, EXIT_SIZE, EXIT_SIZE);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, EXIT_SIZE, EXIT_SIZE);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("EXIT", x + 5, y + 25);
        }
    }
}