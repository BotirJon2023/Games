import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class HauntedMansionSurvivalGame extends JFrame {
    private GamePanel gamePanel;
    private Timer gameTimer;
    private boolean isGameOver;
    private int score;
    private int playerHealth;

    public HauntedMansionSurvivalGame() {
        setTitle("Haunted Mansion Survival");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);
        setupInput();

        score = 0;
        playerHealth = 100;
        isGameOver = false;

        gameTimer = new Timer(16, e -> {
            if (!isGameOver) {
                gamePanel.update();
                gamePanel.repaint();
            }
        });
        gameTimer.start();
    }

    private void setupInput() {
        InputMap inputMap = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = gamePanel.getActionMap();

        String[] directions = {"up", "down", "left", "right", "space"};
        for (String dir : directions) {
            inputMap.put(KeyStroke.getKeyStroke(dir.equals("space") ? "SPACE" : dir.toUpperCase()), dir);
            actionMap.put(dir, new MoveAction(dir));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HauntedMansionSurvivalGame game = new HauntedMansionSurvivalGame();
            game.setVisible(true);
        });
    }

    class GamePanel extends JPanel {
        private Player player;
        private ArrayList<Ghost> ghosts;
        private ArrayList<Collectible> collectibles;
        private ArrayList<Obstacle> obstacles;
        private Random random;
        private BufferedImage background;
        private int animationFrame;

        public GamePanel() {
            setFocusable(true);
            player = new Player(400, 300);
            ghosts = new ArrayList<>();
            collectibles = new ArrayList<>();
            obstacles = new ArrayList<>();
            random = new Random();
            animationFrame = 0;

            // Initialize background
            background = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = background.createGraphics();
            g2d.setColor(new Color(20, 20, 40));
            g2d.fillRect(0, 0, 800, 600);
            g2d.setColor(new Color(60, 60, 80));
            for (int i = 0; i < 800; i += 40) {
                g2d.drawLine(i, 0, i, 600);
                g2d.drawLine(0, i, 800, i);
            }
            g2d.dispose();

            // Initialize obstacles
            for (int i = 0; i < 10; i++) {
                obstacles.add(new Obstacle(
                        random.nextInt(700) + 50,
                        random.nextInt(500) + 50
                ));
            }

            // Initialize ghosts
            for (int i = 0; i < 5; i++) {
                ghosts.add(new Ghost(
                        random.nextInt(700) + 50,
                        random.nextInt(500) + 50
                ));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw background
            g2d.drawImage(background, 0, 0, null);

            // Draw obstacles
            for (Obstacle obstacle : obstacles) {
                obstacle.draw(g2d);
            }

            // Draw collectibles
            for (Collectible collectible : collectibles) {
                collectible.draw(g2d);
            }

            // Draw ghosts
            for (Ghost ghost : ghosts) {
                ghost.draw(g2d, animationFrame);
            }

            // Draw player
            player.draw(g2d, animationFrame);

            // Draw HUD
            drawHUD(g2d);

            if (isGameOver) {
                drawGameOver(g2d);
            }
        }

        private void drawHUD(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Score: " + score, 10, 20);
            g2d.drawString("Health: " + playerHealth, 10, 40);

            // Draw health bar
            g2d.setColor(Color.RED);
            g2d.fillRect(100, 25, 100, 10);
            g2d.setColor(Color.GREEN);
            g2d.fillRect(100, 25, playerHealth, 10);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(100, 25, 100, 10);
        }

        private void drawGameOver(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, 800, 600);
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            String message = playerHealth <= 0 ? "Game Over!" : "You Survived!";
            FontMetrics fm = g2d.getFontMetrics();
            int width = fm.stringWidth(message);
            g2d.drawString(message, (800 - width) / 2, 300);
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            String scoreText = "Final Score: " + score;
            width = fm.stringWidth(scoreText);
            g2d.drawString(scoreText, (800 - width) / 2, 350);
        }

        public void update() {
            animationFrame = (animationFrame + 1) % 60;

            if (!isGameOver) {
                player.update();

                // Update ghosts
                for (Ghost ghost : ghosts) {
                    ghost.update(player);
                    if (ghost.collidesWith(player)) {
                        playerHealth -= 1;
                        if (playerHealth <= 0) {
                            isGameOver = true;
                        }
                    }
                }

                // Spawn collectibles
                if (random.nextDouble() < 0.02) {
                    collectibles.add(new Collectible(
                            random.nextInt(700) + 50,
                            random.nextInt(500) + 50
                    ));
                }

                // Check collectibles
                collectibles.removeIf(collectible -> {
                    if (collectible.collidesWith(player)) {
                        score += 10;
                        playerHealth = Math.min(100, playerHealth + 5);
                        return true;
                    }
                    return false;
                });

                // Check obstacles
                for (Obstacle obstacle : obstacles) {
                    if (obstacle.collidesWith(player)) {
                        player.undoMove();
                    }
                }
            }
        }

        public class Ghost {
            public Ghost(int i, int i1) {
            }

            public void update(Player player) {
            }

            public boolean collidesWith(Player player) {
            }

            public void draw(Graphics2D g2d, int animationFrame) {
            }
        }
    }

    class Player {
        private double x, y;
        private double prevX, prevY;
        private double speed;
        private boolean movingUp, movingDown, movingLeft, movingRight;

        public Player(double x, double y) {
            this.x = x;
            this.y = y;
            this.speed = 3.0;
        }

        public void draw(Graphics2D g2d, int frame) {
            int size = 30;
            g2d.setColor(Color.BLUE);
            int offset = (int) (Math.sin(frame * 0.1) * 2);
            g2d.fillOval((int) x - size / 2, (int) y - size / 2 + offset, size, size);

            // Draw eyes
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int) x - 8, (int) y - 8 + offset, 6, 6);
            g2d.fillOval((int) x + 2, (int) y - 8 + offset, 6, 6);
            g2d.setColor(Color.BLACK);
            g2d.fillOval((int) x - 7, (int) y - 7 + offset, 4, 4);
            g2d.fillOval((int) x + 3, (int) y - 7 + offset, 4, 4);
        }

        public void update() {
            prevX = x;
            prevY = y;

            double dx = 0, dy = 0;
            if (movingUp) dy -= speed;
            if (movingDown) dy += speed;
            if (movingLeft) dx -= speed;
            if (movingRight) dx += speed;

            // Normalize diagonal movement
            if (dx != 0 && dy != 0) {
                double length = Math.sqrt(dx * dx + dy * dy);
                dx = dx / length * speed;
                dy = dy / length * speed;
            }

            x += dx;
            y += dy;

            // Keep player in bounds
            x = Math.max(15, Math.min(785, x));
            y = Math.max(15, Math.min(585, y));
        }

        public void undoMove() {
            x = prevX;
            y = prevY;
        }

        public Rectangle getBounds() {
            return new Rectangle((int) x - 15, (int) y - 15, 30, 30);
        }
    }

    class Ghost {
        private double x, y;
        private double speed;
        private Random random;

        public Ghost(double x, double y) {
            this.x = x;
            this.y = y;
            this.speed = 1.5 + Math.random();
            this.random = new Random();
        }

        public void draw(Graphics2D g2d, int frame) {
            int size = 30;
            g2d.setColor(new Color(200, 200, 200, 180));
            int offset = (int) (Math.sin(frame * 0.15) * 3);
            g2d.fillOval((int) x - size / 2, (int) y - size / 2 + offset, size, size);

            // Draw eyes
            g2d.setColor(Color.BLACK);
            g2d.fillOval((int) x - 8, (int) y - 8 + offset, 6, 6);
            g2d.fillOval((int) x + 2, (int) y - 8 + offset, 6, 6);
        }

        public void update(Player player) {
            double dx = player.x - x;
            double dy = player.y - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            // Chase player with some randomness
            if (distance > 0) {
                dx = dx / distance * speed + (random.nextDouble() - 0.5) * 0.5;
                dy = dy / distance * speed + (random.nextDouble() - 0.5) * 0.5;
                x += dx;
                y += dy;
            }

            // Keep in bounds
            x = Math.max(15, Math.min(785, x));
            y = Math.max(15, Math.min(585, y));
        }

        public boolean collidesWith(Player player) {
            return getBounds().intersects(player.getBounds());
        }

        public Rectangle getBounds() {
            return new Rectangle((int) x - 15, (int) y - 15, 30, 30);
        }
    }

    class Collectible {
        private double x, y;
        private int timer;

        public Collectible(double x, double y) {
            this.x = x;
            this.y = y;
            this.timer = 300;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.YELLOW);
            int size = 10 + (int) (Math.sin(timer * 0.1) * 2);
            g2d.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
            timer--;
        }

        public boolean collidesWith(Player player) {
            return timer > 0 && getBounds().intersects(player.getBounds());
        }

        public Rectangle getBounds() {
            return new Rectangle((int) x - 5, (int) y - 5, 10, 10);
        }
    }

    class Obstacle {
        private double x, y;

        public Obstacle(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(new Color(100, 60, 20));
            g2d.fillRect((int) x - 20, (int) y - 20, 40, 40);
            g2d.setColor(Color.BLACK);
            g2d.drawRect((int) x - 20, (int) y - 20, 40, 40);
        }

        public boolean collidesWith(Player player) {
            return getBounds().intersects(player.getBounds());
        }

        public Rectangle getBounds() {
            return new Rectangle((int) x - 20, (int) y - 20, 40, 40);
        }
    }

    class MoveAction extends AbstractAction {
        private String direction;

        public MoveAction(String direction) {
            this.direction = direction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (direction.equals("up")) {
                gamePanel.player.movingUp = e.getID() == ActionEvent.ACTION_PERFORMED;
            } else if (direction.equals("down")) {
                gamePanel.player.movingDown = e.getID() == ActionEvent.ACTION_PERFORMED;
            } else if (direction.equals("left")) {
                gamePanel.player.movingLeft = e.getID() == ActionEvent.ACTION_PERFORMED;
            } else if (direction.equals("right")) {
                gamePanel.player.movingRight = e.getID() == ActionEvent.ACTION_PERFORMED;
            } else if (direction.equals("space")) {
                if (isGameOver && e.getID() == ActionEvent.ACTION_PERFORMED) {
                    // Restart game
                    gamePanel.player = new GamePanel().new Player(400, 300);
                    gamePanel.ghosts.clear();
                    gamePanel.collectibles.clear();
                    for (int i = 0; i < 5; i++) {
                        gamePanel.ghosts.add(gamePanel.new Ghost(
                                gamePanel.random.nextInt(700) + 50,
                                gamePanel.random.nextInt(500) + 50
                        ));
                    }
                    score = 0;
                    playerHealth = 100;
                    isGameOver = false;
                }
            }
        }
    }
}