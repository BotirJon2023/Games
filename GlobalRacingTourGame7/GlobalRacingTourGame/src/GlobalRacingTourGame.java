import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.URL;

public class GlobalRacingTourGame extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 700;
    private static final int DELAY = 16; // ~60 FPS

    // Road dimensions
    private static final int ROAD_LEFT = 200;
    private static final int ROAD_RIGHT = 1000;
    private static final int LANE_WIDTH = 100;
    private static final int NUM_LANES = 8;

    // Game state
    private Timer timer;
    private boolean gameRunning = true;
    private boolean gameOver = false;
    private boolean twoPlayerMode = false;
    private int gameSpeed = 5;
    private int score = 0;

    // Players
    private Player player1;
    private Player player2;
    private ComputerPlayer computerPlayer;
    private List<Obstacle> obstacles;
    private List<Coin> coins;
    private List<Building> buildings;
    private List<Tree> trees;

    // Road animation
    private int roadOffset = 0;
    private Random random = new Random();

    // Moscow landmarks
    private List<Landmark> landmarks;

    public GlobalRacingTourGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(50, 50, 60));
        setFocusable(true);
        addKeyListener(this);

        initializeGame();

        timer = new Timer(DELAY, this);
        timer.start();
    }

    private void initializeGame() {
        // Initialize players
        player1 = new Player(ROAD_LEFT + LANE_WIDTH * 2, HEIGHT - 100, Color.RED, "Player 1");
        player2 = new Player(ROAD_LEFT + LANE_WIDTH * 5, HEIGHT - 100, Color.BLUE, "Player 2");
        computerPlayer = new ComputerPlayer(ROAD_LEFT + LANE_WIDTH * 3, HEIGHT - 100, Color.ORANGE, "AI");

        obstacles = new ArrayList<>();
        coins = new ArrayList<>();
        buildings = new ArrayList<>();
        trees = new ArrayList<>();
        landmarks = new ArrayList<>();

        // Generate Moscow buildings
        generateMoscowCityscape();

        // Generate initial obstacles and coins
        for (int i = 0; i < 5; i++) {
            generateObstacle();
            generateCoin();
        }
    }

    private void generateMoscowCityscape() {
        // Create Moscow skyline with iconic buildings
        String[] buildingColors = {"#8B0000", "#6B3A2A", "#4A4A4A", "#5C4033", "#8B7355"};

        for (int i = 0; i < 15; i++) {
            int x = 50 + i * 80;
            int height = 100 + random.nextInt(200);
            int width = 60 + random.nextInt(40);

            // Saint Basil's Cathedral-like building
            if (i == 3 || i == 8) {
                buildings.add(new Building(x, HEIGHT - 150 - height, width, height,
                        Color.decode("#8B0000"), true));
                // Add domes
                for (int j = 0; j < 5; j++) {
                    landmarks.add(new Landmark(x + j * 15, HEIGHT - 150 - height - 30,
                            20, 30, Color.decode("#FFD700")));
                }
            } else if (i == 7) {
                // Kremlin tower
                buildings.add(new Building(x, HEIGHT - 150 - height, width, height,
                        Color.decode("#6B3A2A"), true));
                landmarks.add(new Landmark(x + width/2 - 10, HEIGHT - 150 - height - 40,
                        20, 50, Color.decode("#C0C0C0")));
            } else {
                buildings.add(new Building(x, HEIGHT - 150 - height, width, height,
                        Color.decode(buildingColors[i % buildingColors.length]), false));
            }

            // Add trees along the road
            if (i % 2 == 0) {
                trees.add(new Tree(ROAD_LEFT - 40 + i * 30, HEIGHT - 30,
                        30, 50, Color.GREEN));
                trees.add(new Tree(ROAD_RIGHT + 10 + i * 30, HEIGHT - 30,
                        30, 50, Color.GREEN));
            }
        }
    }

    private void generateObstacle() {
        int lane = random.nextInt(NUM_LANES - 1);
        int x = ROAD_LEFT + lane * LANE_WIDTH + LANE_WIDTH/2;
        int y = -50 - random.nextInt(300);

        // Different types of obstacles
        int type = random.nextInt(4);
        Color color;
        String label;
        int width = 30, height = 30;

        switch(type) {
            case 0: color = Color.RED; label = "🚗"; width = 40; height = 20; break;
            case 1: color = Color.ORANGE; label = "🚕"; width = 40; height = 20; break;
            case 2: color = Color.MAGENTA; label = "🚛"; width = 50; height = 25; break;
            default: color = Color.YELLOW; label = "⚠️"; width = 30; height = 30; break;
        }

        obstacles.add(new Obstacle(x, y, width, height, color, label, type));
    }

    private void generateCoin() {
        int lane = random.nextInt(NUM_LANES - 1);
        int x = ROAD_LEFT + lane * LANE_WIDTH + LANE_WIDTH/2;
        int y = -50 - random.nextInt(500);
        coins.add(new Coin(x, y, 20, 20, Color.YELLOW));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning) return;

        // Update game state
        updateGame();
        repaint();
    }

    private void updateGame() {
        if (gameOver) return;

        // Update road animation
        roadOffset = (roadOffset + gameSpeed) % 100;

        // Update player positions
        player1.update();
        if (twoPlayerMode) {
            player2.update();
        } else {
            // AI opponent
            computerPlayer.update(obstacles, player1.getX());
        }

        // Update obstacles
        updateObstacles();

        // Update coins
        updateCoins();

        // Check collisions
        checkCollisions();

        // Update score
        score += 1;
    }

    private void updateObstacles() {
        Iterator<Obstacle> iter = obstacles.iterator();
        while (iter.hasNext()) {
            Obstacle obs = iter.next();
            obs.y += gameSpeed;

            // Remove obstacles that have passed the screen
            if (obs.y > HEIGHT + 50) {
                iter.remove();
                generateObstacle();
            }
        }

        // Maintain obstacle count
        while (obstacles.size() < 8) {
            generateObstacle();
        }
    }

    private void updateCoins() {
        Iterator<Coin> iter = coins.iterator();
        while (iter.hasNext()) {
            Coin coin = iter.next();
            coin.y += gameSpeed;
            coin.animate();

            // Remove coins that have passed the screen
            if (coin.y > HEIGHT + 50) {
                iter.remove();
                generateCoin();
            }
        }

        // Maintain coin count
        while (coins.size() < 10) {
            generateCoin();
        }
    }

    private void checkCollisions() {
        Rectangle player1Rect = player1.getBounds();
        Rectangle player2Rect = twoPlayerMode ? player2.getBounds() : computerPlayer.getBounds();

        // Check obstacle collisions for player 1
        for (Obstacle obs : obstacles) {
            if (player1Rect.intersects(obs.getBounds())) {
                gameOver = true;
                return;
            }
            if (player2Rect.intersects(obs.getBounds())) {
                gameOver = true;
                return;
            }
        }

        // Check coin collection for player 1
        Iterator<Coin> iter = coins.iterator();
        while (iter.hasNext()) {
            Coin coin = iter.next();
            if (player1Rect.intersects(coin.getBounds())) {
                player1.addScore(10);
                iter.remove();
                generateCoin();
            }
            if (player2Rect.intersects(coin.getBounds())) {
                if (twoPlayerMode) {
                    player2.addScore(10);
                } else {
                    computerPlayer.addScore(10);
                }
                iter.remove();
                generateCoin();
            }
        }

        // Check collision between players
        if (twoPlayerMode && player1Rect.intersects(player2Rect)) {
            // Bounce effect
            player1.x += player1.dx > 0 ? -20 : 20;
            player2.x += player2.dx > 0 ? 20 : -20;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw sky gradient (Moscow sky)
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(70, 130, 180),
                0, HEIGHT, new Color(135, 206, 235));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw buildings and landmarks
        drawBuildings(g2d);
        drawLandmarks(g2d);

        // Draw road
        drawRoad(g2d);

        // Draw trees
        drawTrees(g2d);

        // Draw obstacles
        for (Obstacle obs : obstacles) {
            obs.draw(g2d);
        }

        // Draw coins
        for (Coin coin : coins) {
            coin.draw(g2d);
        }

        // Draw players
        player1.draw(g2d);
        if (twoPlayerMode) {
            player2.draw(g2d);
        } else {
            computerPlayer.draw(g2d);
        }

        // Draw UI
        drawUI(g2d);

        // Draw game over screen
        if (gameOver) {
            drawGameOver(g2d);
        }
    }

    private void drawRoad(Graphics2D g2d) {
        // Road background
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect(ROAD_LEFT, 0, ROAD_RIGHT - ROAD_LEFT, HEIGHT);

        // Road markings
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < NUM_LANES; i++) {
            int x = ROAD_LEFT + i * LANE_WIDTH;
            g2d.setStroke(new BasicStroke(2));

            // Dashed lane lines
            if (i > 0 && i < NUM_LANES) {
                for (int y = -100 + roadOffset; y < HEIGHT; y += 60) {
                    g2d.drawLine(x, y, x, y + 30);
                }
            }
        }

        // Road edges
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(ROAD_LEFT, 0, ROAD_LEFT, HEIGHT);
        g2d.drawLine(ROAD_RIGHT, 0, ROAD_RIGHT, HEIGHT);

        // Road texture (subtle)
        g2d.setColor(new Color(60, 60, 60, 50));
        for (int i = 0; i < HEIGHT; i += 20) {
            g2d.drawLine(ROAD_LEFT + 10, (i + roadOffset) % HEIGHT,
                    ROAD_RIGHT - 10, (i + roadOffset) % HEIGHT);
        }
    }

    private void drawBuildings(Graphics2D g2d) {
        for (Building building : buildings) {
            building.draw(g2d);
        }
    }

    private void drawLandmarks(Graphics2D g2d) {
        for (Landmark landmark : landmarks) {
            landmark.draw(g2d);
        }
    }

    private void drawTrees(Graphics2D g2d) {
        for (Tree tree : trees) {
            tree.draw(g2d);
        }
    }

    private void drawUI(Graphics2D g2d) {
        // Score board
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(10, 10, 300, 80, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Global Racing Tour - Moscow", 20, 35);

        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Player 1: " + player1.getScore() + " pts", 20, 60);

        if (twoPlayerMode) {
            g2d.drawString("Player 2: " + player2.getScore() + " pts", 160, 60);
        } else {
            g2d.drawString("AI: " + computerPlayer.getScore() + " pts", 160, 60);
        }

        // Speed indicator
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(WIDTH - 150, 10, 130, 40, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Speed: " + gameSpeed, WIDTH - 130, 37);

        // Game mode
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(WIDTH - 200, 60, 180, 30, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString(twoPlayerMode ? "2 Player Mode" : "vs AI Mode", WIDTH - 190, 80);
    }

    private void drawGameOver(Graphics2D g2d) {
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Game over text
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String text = "GAME OVER";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, HEIGHT/2 - 50);

        // Scores
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("Final Scores:", WIDTH/2 - 80, HEIGHT/2 + 20);
        g2d.drawString("Player 1: " + player1.getScore(), WIDTH/2 - 100, HEIGHT/2 + 60);

        if (twoPlayerMode) {
            g2d.drawString("Player 2: " + player2.getScore(), WIDTH/2 - 100, HEIGHT/2 + 100);
            String winner = player1.getScore() > player2.getScore() ? "Player 1 Wins!" :
                    (player2.getScore() > player1.getScore() ? "Player 2 Wins!" : "It's a Tie!");
            g2d.drawString(winner, WIDTH/2 - 80, HEIGHT/2 + 140);
        } else {
            g2d.drawString("AI: " + computerPlayer.getScore(), WIDTH/2 - 100, HEIGHT/2 + 100);
            String winner = player1.getScore() > computerPlayer.getScore() ? "You Win!" :
                    (computerPlayer.getScore() > player1.getScore() ? "AI Wins!" : "It's a Tie!");
            g2d.drawString(winner, WIDTH/2 - 60, HEIGHT/2 + 140);
        }

        // Restart instructions
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("Press SPACE to restart", WIDTH/2 - 100, HEIGHT/2 + 190);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_SPACE && gameOver) {
            restartGame();
            return;
        }

        if (gameOver) return;

        // Player 1 controls (WASD)
        if (key == KeyEvent.VK_W) {
            player1.dy = -5;
        }
        if (key == KeyEvent.VK_S) {
            player1.dy = 5;
        }
        if (key == KeyEvent.VK_A) {
            player1.dx = -5;
        }
        if (key == KeyEvent.VK_D) {
            player1.dx = 5;
        }

        // Player 2 controls (Arrow keys)
        if (twoPlayerMode) {
            if (key == KeyEvent.VK_UP) {
                player2.dy = -5;
            }
            if (key == KeyEvent.VK_DOWN) {
                player2.dy = 5;
            }
            if (key == KeyEvent.VK_LEFT) {
                player2.dx = -5;
            }
            if (key == KeyEvent.VK_RIGHT) {
                player2.dx = 5;
            }
        }

        // Speed controls
        if (key == KeyEvent.VK_EQUALS) {
            gameSpeed = Math.min(gameSpeed + 1, 10);
        }
        if (key == KeyEvent.VK_MINUS) {
            gameSpeed = Math.max(gameSpeed - 1, 3);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        // Player 1 controls
        if (key == KeyEvent.VK_W || key == KeyEvent.VK_S) {
            player1.dy = 0;
        }
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_D) {
            player1.dx = 0;
        }

        // Player 2 controls
        if (twoPlayerMode) {
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) {
                player2.dy = 0;
            }
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                player2.dx = 0;
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void restartGame() {
        gameOver = false;
        gameRunning = true;
        score = 0;
        gameSpeed = 5;
        player1.reset(ROAD_LEFT + LANE_WIDTH * 2, HEIGHT - 100);
        player2.reset(ROAD_LEFT + LANE_WIDTH * 5, HEIGHT - 100);
        computerPlayer.reset(ROAD_LEFT + LANE_WIDTH * 3, HEIGHT - 100);
        obstacles.clear();
        coins.clear();

        for (int i = 0; i < 5; i++) {
            generateObstacle();
            generateCoin();
        }
    }

    // Inner classes for game objects
    class Player {
        int x, y, width, height;
        int dx = 0, dy = 0;
        int score = 0;
        Color color;
        String name;
        Rectangle bounds;

        public Player(int x, int y, Color color, String name) {
            this.x = x;
            this.y = y;
            this.width = 30;
            this.height = 50;
            this.color = color;
            this.name = name;
            this.bounds = new Rectangle(x, y, width, height);
        }

        public void update() {
            x += dx;
            y += dy;

            // Keep within road bounds
            x = Math.max(ROAD_LEFT + 20, Math.min(x, ROAD_RIGHT - width - 20));
            y = Math.max(50, Math.min(y, HEIGHT - height - 50));

            bounds.setBounds(x, y, width, height);
        }

        public void draw(Graphics2D g2d) {
            // Car body
            g2d.setColor(color);
            g2d.fillRoundRect(x, y, width, height, 8, 8);

            // Windows
            g2d.setColor(Color.CYAN);
            g2d.fillRect(x + 5, y + 5, 20, 12);
            g2d.fillRect(x + 5, y + 28, 20, 12);

            // Headlights
            g2d.setColor(Color.YELLOW);
            g2d.fillOval(x + 2, y + 5, 6, 6);
            g2d.fillOval(x + 2, y + 36, 6, 6);

            // Player name
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(name, x - 5, y - 10);
        }

        public Rectangle getBounds() {
            return bounds;
        }

        public void addScore(int points) {
            score += points;
        }

        public int getScore() {
            return score;
        }

        public void reset(int x, int y) {
            this.x = x;
            this.y = y;
            dx = 0;
            dy = 0;
            score = 0;
        }

        public int getX() { return x; }
        public int getY() { return y; }
    }

    class ComputerPlayer extends Player {
        private int targetX;
        private int dodgeTimer = 0;

        public ComputerPlayer(int x, int y, Color color, String name) {
            super(x, y, color, name);
            targetX = x;
        }

        public void update(List<Obstacle> obstacles, int player1X) {
            // AI logic - dodge obstacles and chase coins
            dodgeTimer++;

            // Find nearest obstacle
            Obstacle nearestObs = null;
            int minDist = Integer.MAX_VALUE;

            for (Obstacle obs : obstacles) {
                if (obs.y > y - 100 && obs.y < y + 50) {
                    int dist = Math.abs(obs.x - x);
                    if (dist < minDist) {
                        minDist = dist;
                        nearestObs = obs;
                    }
                }
            }

            if (nearestObs != null && minDist < 100) {
                // Dodge obstacle
                if (nearestObs.x < x) {
                    targetX = x + 80;
                } else {
                    targetX = x - 80;
                }
                // Randomize sometimes
                if (dodgeTimer % 30 == 0) {
                    targetX += random.nextInt(60) - 30;
                }
            } else if (dodgeTimer % 20 == 0) {
                // Random movement to simulate human-like behavior
                targetX = x + random.nextInt(100) - 50;
            }

            // Smooth movement towards target
            if (Math.abs(targetX - x) > 5) {
                dx = (targetX > x) ? 3 : -3;
            } else {
                dx = 0;
            }

            // Sometimes chase the player
            if (dodgeTimer % 100 == 0 && random.nextInt(3) == 0) {
                targetX = player1X + random.nextInt(60) - 30;
            }

            // Keep within bounds
            targetX = Math.max(ROAD_LEFT + 20, Math.min(targetX, ROAD_RIGHT - width - 20));

            // Move vertically - slight drift
            if (dodgeTimer % 50 == 0) {
                dy = random.nextInt(3) - 1;
            }

            super.update();

            // Add slight score bonus for AI
            if (dodgeTimer % 100 == 0) {
                addScore(1);
            }
        }
    }

    class Obstacle {
        int x, y, width, height;
        Color color;
        String label;
        int type;

        public Obstacle(int x, int y, int width, int height, Color color, String label, int type) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
            this.label = label;
            this.type = type;
        }

        public void draw(Graphics2D g2d) {
            // Draw obstacle as a vehicle or hazard
            g2d.setColor(color);
            g2d.fillRoundRect(x - width/2, y - height/2, width, height, 5, 5);

            // Draw label
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString(label, x - 10, y + 5);

            // Add details based on type
            if (type == 0 || type == 1) {
                // Car details
                g2d.setColor(Color.GRAY);
                g2d.fillOval(x - width/2 + 5, y + height/2 - 8, 8, 8);
                g2d.fillOval(x + width/2 - 13, y + height/2 - 8, 8, 8);
            }
        }

        public Rectangle getBounds() {
            return new Rectangle(x - width/2, y - height/2, width, height);
        }
    }

    class Coin {
        int x, y, radius;
        Color color;
        int animationFrame = 0;

        public Coin(int x, int y, int radius, int radius2, Color color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
        }

        public void animate() {
            animationFrame = (animationFrame + 1) % 20;
        }

        public void draw(Graphics2D g2d) {
            // Animated spinning coin effect
            int scale = animationFrame < 10 ? animationFrame : 20 - animationFrame;
            int currentRadius = radius * (10 + scale) / 20;

            g2d.setColor(color);
            g2d.fillOval(x - currentRadius, y - radius/2, currentRadius * 2, radius);

            // Shine effect
            g2d.setColor(Color.WHITE);
            g2d.fillOval(x - currentRadius/3, y - radius/4, currentRadius/2, radius/3);

            // Dollar sign
            g2d.setColor(Color.ORANGE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("$", x - 5, y + 5);
        }

        public Rectangle getBounds() {
            return new Rectangle(x - radius, y - radius, radius * 2, radius * 2);
        }
    }

    class Building {
        int x, y, width, height;
        Color color;
        boolean isLandmark;

        public Building(int x, int y, int width, int height, Color color, boolean isLandmark) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
            this.isLandmark = isLandmark;
        }

        public void draw(Graphics2D g2d) {
            // Building body
            g2d.setColor(color);
            g2d.fillRect(x, y, width, height);

            // Windows
            g2d.setColor(new Color(255, 255, 200, 180));
            for (int i = 0; i < width - 10; i += 15) {
                for (int j = 0; j < height - 10; j += 20) {
                    g2d.fillRect(x + 5 + i, y + 5 + j, 8, 12);
                }
            }

            // Building outline
            g2d.setColor(Color.DARK_GRAY);
            g2d.drawRect(x, y, width, height);

            // If landmark, add special features
            if (isLandmark) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.drawString("★", x + width/2 - 5, y - 5);
            }
        }
    }

    class Landmark {
        int x, y, width, height;
        Color color;

        public Landmark(int x, int y, int width, int height, Color color) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
        }

        public void draw(Graphics2D g2d) {
            // Draw onion dome or landmark feature
            g2d.setColor(color);

            // Dome shape
            int[] xPoints = {x, x + width/2, x + width};
            int[] yPoints = {y + height, y, y + height};
            g2d.fillPolygon(xPoints, yPoints, 3);

            // Cross on top
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(x + width/2, y - 5, x + width/2, y + 5);
            g2d.drawLine(x + width/2 - 5, y, x + width/2 + 5, y);
        }
    }

    class Tree {
        int x, y, width, height;
        Color color;

        public Tree(int x, int y, int width, int height, Color color) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
        }

        public void draw(Graphics2D g2d) {
            // Trunk
            g2d.setColor(new Color(101, 67, 33));
            g2d.fillRect(x + width/2 - 4, y + height/2, 8, height/2);

            // Canopy
            g2d.setColor(color);
            g2d.fillOval(x, y, width, height);

            // Highlight
            g2d.setColor(new Color(144, 238, 144, 100));
            g2d.fillOval(x + 5, y + 5, width - 10, height - 10);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Global Racing Tour - Moscow");
            GlobalRacingTourGame game = new GlobalRacingTourGame();

            // Show mode selection dialog
            int choice = JOptionPane.showOptionDialog(frame,
                    "Select Game Mode:",
                    "Global Racing Tour",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"2 Players", "vs Computer"},
                    "vs Computer");

            if (choice == 0) {
                game.twoPlayerMode = true;
            } else {
                game.twoPlayerMode = false;
            }

            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }
}