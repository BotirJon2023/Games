import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class ParanormalInvestigationGame extends JFrame {
    private GamePanel gamePanel;
    private Timer gameTimer;
    private boolean gameOver;
    private int score;
    private Player player;
    private ArrayList<Ghost> ghosts;
    private ArrayList<Evidence> evidence;
    private Random random;
    private int level;
    private int ghostSpawnCounter;
    private int evidenceSpawnCounter;
    private JLabel scoreLabel;
    private JLabel levelLabel;
    private JPanel controlPanel;
    private JButton startButton;
    private JButton pauseButton;

    public ParanormalInvestigationGame() {
        setTitle("Paranormal Investigation Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        random = new Random();
        gamePanel = new GamePanel();
        score = 0;
        level = 1;
        gameOver = false;
        ghosts = new ArrayList<>();
        evidence = new ArrayList<>();
        player = new Player(400, 300);
        ghostSpawnCounter = 0;
        evidenceSpawnCounter = 0;

        // Initialize UI components
        initUI();

        // Game timer for animation and updates
        gameTimer = new Timer(16, e -> {
            if (!gameOver) {
                updateGame();
                gamePanel.repaint();
            }
        });
    }

    private void initUI() {
        // Set up control panel
        controlPanel = new JPanel();
        scoreLabel = new JLabel("Score: 0");
        levelLabel = new JLabel("Level: 1");
        startButton = new JButton("Start");
        pauseButton = new JButton("Pause");

        controlPanel.add(scoreLabel);
        controlPanel.add(levelLabel);
        controlPanel.add(startButton);
        controlPanel.add(pauseButton);

        add(controlPanel, BorderLayout.NORTH);
        add(gamePanel, BorderLayout.CENTER);

        // Button listeners
        startButton.addActionListener(e -> startGame());
        pauseButton.addActionListener(e -> togglePause());
    }

    private void startGame() {
        gameOver = false;
        score = 0;
        level = 1;
        player = new Player(400, 300);
        ghosts.clear();
        evidence.clear();
        scoreLabel.setText("Score: 0");
        levelLabel.setText("Level: 1");
        gameTimer.start();
        startButton.setEnabled(false);
        pauseButton.setEnabled(true);
    }

    private void togglePause() {
        if (gameTimer.isRunning()) {
            gameTimer.stop();
            pauseButton.setText("Resume");
        } else {
            gameTimer.start();
            pauseButton.setText("Pause");
        }
    }

    private void updateGame() {
        // Update player
        player.update();

        // Spawn ghosts
        ghostSpawnCounter++;
        if (ghostSpawnCounter >= 60 - level * 5) {
            spawnGhost();
            ghostSpawnCounter = 0;
        }

        // Spawn evidence
        evidenceSpawnCounter++;
        if (evidenceSpawnCounter >= 120 - level * 10) {
            spawnEvidence();
            evidenceSpawnCounter = 0;
        }

        // Update ghosts
        for (int i = ghosts.size() - 1; i >= 0; i--) {
            Ghost ghost = ghosts.get(i);
            ghost.update(player);
            if (ghost.collidesWith(player)) {
                gameOver = true;
                gameTimer.stop();
                startButton.setEnabled(true);
                pauseButton.setEnabled(false);
            }
        }

        // Update evidence
        for (int i = evidence.size() - 1; i >= 0; i--) {
            Evidence ev = evidence.get(i);
            if (ev.collidesWith(player)) {
                score += ev.getPoints();
                scoreLabel.setText("Score: " + score);
                evidence.remove(i);
                if (score >= level * 100) {
                    level++;
                    levelLabel.setText("Level: " + level);
                }
            }
        }
    }

    private void spawnGhost() {
        int x = random.nextInt(800);
        int y = random.nextInt(600);
        ghosts.add(new Ghost(x, y));
    }

    private void spawnEvidence() {
        int x = random.nextInt(800);
        int y = random.nextInt(600);
        evidence.add(new Evidence(x, y));
    }

    class GamePanel extends JPanel {
        private BufferedImage backgroundImage;

        public GamePanel() {
            setFocusable(true);
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    player.handleKeyPress(e);
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    player.handleKeyRelease(e);
                }
            });

            // Create background
            backgroundImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = backgroundImage.createGraphics();
            g2d.setColor(new Color(20, 20, 30));
            g2d.fillRect(0, 0, 800, 600);
            g2d.setColor(new Color(50, 50, 60));
            for (int i = 0; i < 50; i++) {
                int x = random.nextInt(800);
                int y = random.nextInt(600);
                g2d.fillOval(x, y, 5, 5);
            }
            g2d.dispose();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw background
            g2d.drawImage(backgroundImage, 0, 0, null);

            // Draw player
            player.draw(g2d);

            // Draw ghosts
            for (Ghost ghost : ghosts) {
                ghost.draw(g2d);
            }

            // Draw evidence
            for (Evidence ev : evidence) {
                ev.draw(g2d);
            }

            // Draw game over
            if (gameOver) {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                g2d.drawString("Game Over!", 300, 300);
                g2d.setFont(new Font("Arial", Font.PLAIN, 24));
                g2d.drawString("Final Score: " + score, 320, 350);
            }
        }
    }

    class Player {
        private int x, y;
        private int speed;
        private boolean up, down, left, right;
        private int animationFrame;
        private int animationCounter;

        public Player(int x, int y) {
            this.x = x;
            this.y = y;
            this.speed = 5;
            this.animationFrame = 0;
            this.animationCounter = 0;
        }

        public void update() {
            if (up && y > 0) y -= speed;
            if (down && y < 550) y += speed;
            if (left && x > 0) x -= speed;
            if (right && x < 750) x += speed;

            // Update animation
            animationCounter++;
            if (animationCounter >= 10) {
                animationFrame = (animationFrame + 1) % 4;
                animationCounter = 0;
            }
        }

        public void handleKeyPress(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP: up = true; break;
                case KeyEvent.VK_DOWN: down = true; break;
                case KeyEvent.VK_LEFT: left = true; break;
                case KeyEvent.VK_RIGHT: right = true; break;
            }
        }

        public void handleKeyRelease(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP: up = false; break;
                case KeyEvent.VK_DOWN: down = false; break;
                case KeyEvent.VK_LEFT: left = false; break;
                case KeyEvent.VK_RIGHT: right = false; break;
            }
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.BLUE);
            int offset = animationFrame * 5;
            g2d.fillOval(x + offset / 2, y + offset / 2, 50 - offset, 50 - offset);
            g2d.setColor(Color.WHITE);
            g2d.fillOval(x + 15, y + 15, 10, 10);
            g2d.fillOval(x + 25, y + 15, 10, 10);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, 50, 50);
        }
    }

    class Ghost {
        private int x, y;
        private int speed;
        private int animationFrame;
        private int animationCounter;
        private float alpha;

        public Ghost(int x, int y) {
            this.x = x;
            this.y = y;
            this.speed = 2 + level;
            this.animationFrame = 0;
            this.animationCounter = 0;
            this.alpha = 0.7f;
        }

        public void update(Player player) {
            // Move towards player
            if (x < player.x) x += speed;
            if (x > player.x) x -= speed;
            if (y < player.y) y += speed;
            if (y > player.y) y -= speed;

            // Update animation
            animationCounter++;
            if (animationCounter >= 15) {
                animationFrame = (animationFrame + 1) % 4;
                animationCounter = 0;
                alpha = 0.7f + random.nextFloat() * 0.3f;
            }
        }

        public void draw(Graphics2D g2d) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.setColor(Color.WHITE);
            int offset = animationFrame * 3;
            g2d.fillOval(x + offset / 2, y + offset / 2, 40 - offset, 40 - offset);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x + 10, y + 10, 8, 8);
            g2d.fillOval(x + 22, y + 10, 8, 8);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        public boolean collidesWith(Player player) {
            return getBounds().intersects(player.getBounds());
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, 40, 40);
        }
    }

    class Evidence {
        private int x, y;
        private int points;
        private int animationFrame;
        private int animationCounter;

        public Evidence(int x, int y) {
            this.x = x;
            this.y = y;
            this.points = 10 + level * 5;
            this.animationFrame = 0;
            this.animationCounter = 0;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.YELLOW);
            animationCounter++;
            if (animationCounter >= 20) {
                animationFrame = (animationFrame + 1) % 2;
                animationCounter = 0;
            }
            int size = animationFrame == 0 ? 20 : 25;
            g2d.fillRect(x, y, size, size);
        }

        public boolean collidesWith(Player player) {
            return getBounds().intersects(player.getBounds());
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, 25, 25);
        }

        public int getPoints() {
            return points;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ParanormalInvestigationGame game = new ParanormalInvestigationGame();
            game.setVisible(true);
        });
    }
}