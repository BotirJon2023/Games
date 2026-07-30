import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GlobalRacingTourGame extends JPanel implements ActionListener, KeyListener {

    // Game constants
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final int LANE_WIDTH = 200;
    private static final int ROAD_OFFSET = 200;
    private static final int FINISH_LINE = 100;

    // Game states
    private enum GameState { MENU, PLAYING, GAME_OVER }
    private GameState state = GameState.MENU;

    // Players
    private Player player1, player2, computer;
    private boolean isTwoPlayer;
    private boolean isComputerMode;

    // Road and scenery
    private List<RoadMarking> roadMarkings;
    private List<Building> buildings;
    private List<Tree> trees;
    private List<StreetLight> streetLights;
    private List<Particle> particles;
    private Timer gameTimer;
    private int gameTime = 0;

    // Animation
    private double roadOffset = 0;
    private boolean gameOver = false;
    private String winner = "";

    // Input
    private boolean[] keys = new boolean[256];

    public GlobalRacingTourGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky blue
        setFocusable(true);
        addKeyListener(this);

        initializeGame();
        startGameLoop();
    }

    private void initializeGame() {
        // Show menu selection
        String[] options = {"2 Players", "vs Computer"};
        int choice = JOptionPane.showOptionDialog(null,
                "Select Game Mode", "Global Racing Tour - Madrid",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);

        isTwoPlayer = (choice == 0);
        isComputerMode = (choice == 1);

        // Initialize players
        player1 = new Player("Player 1", Color.RED, 250);
        if (isTwoPlayer) {
            player2 = new Player("Player 2", Color.BLUE, 450);
        } else {
            computer = new Player("Computer", Color.GREEN, 450);
            computer.isComputer = true;
        }

        // Initialize road elements
        roadMarkings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            roadMarkings.add(new RoadMarking(ROAD_OFFSET + 50, i * 80));
            roadMarkings.add(new RoadMarking(ROAD_OFFSET + 250, i * 80));
            roadMarkings.add(new RoadMarking(ROAD_OFFSET + 450, i * 80));
        }

        // Initialize Madrid buildings
        buildings = new ArrayList<>();
        String[] buildingColors = {"#8B7355", "#A0522D", "#CD853F", "#D2B48C", "#8B8682"};
        int[] buildingHeights = {120, 150, 100, 180, 130};

        for (int i = 0; i < 12; i++) {
            int x = (i < 6) ? 20 + i * 60 : WIDTH - 70 - (i - 6) * 60;
            int height = buildingHeights[i % buildingHeights.length];
            Color color = Color.decode(buildingColors[i % buildingColors.length]);
            buildings.add(new Building(x, HEIGHT - height - 200, 50, height, color));
        }

        // Add Madrid landmarks (simplified)
        buildings.add(new Building(60, 500, 80, 300, Color.decode("#C41E3A"))); // Plaza Mayor
        buildings.add(new Building(WIDTH - 140, 480, 100, 320, Color.decode("#8B0000"))); // Royal Palace

        // Initialize trees
        trees = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            int x = (i < 8) ? 30 + i * 70 : WIDTH - 40 - (i - 8) * 70;
            trees.add(new Tree(x, HEIGHT - 250, 30, 60));
        }

        // Initialize street lights
        streetLights = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            streetLights.add(new StreetLight(ROAD_OFFSET - 20, 300 + i * 100));
            streetLights.add(new StreetLight(ROAD_OFFSET + 530, 300 + i * 100));
        }

        // Particle system for effects
        particles = new ArrayList<>();

        state = GameState.PLAYING;
    }

    private void startGameLoop() {
        gameTimer = new Timer();
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateGame();
                repaint();
            }
        }, 0, 16); // ~60 FPS
    }

    private void updateGame() {
        if (state != GameState.PLAYING) return;

        gameTime++;

        // Update road animation
        roadOffset = (roadOffset + 2) % 80;

        // Update players
        updatePlayer(player1, keys[KeyEvent.VK_UP], keys[KeyEvent.VK_DOWN],
                keys[KeyEvent.VK_LEFT], keys[KeyEvent.VK_RIGHT]);

        if (isTwoPlayer) {
            updatePlayer(player2, keys[KeyEvent.VK_W], keys[KeyEvent.VK_S],
                    keys[KeyEvent.VK_A], keys[KeyEvent.VK_D]);
        } else if (computer != null) {
            updateComputerPlayer();
        }

        // Check collisions and finish
        checkGameConditions();

        // Update particles
        updateParticles();
    }

    private void updatePlayer(Player player, boolean up, boolean down, boolean left, boolean right) {
        if (player.isFinished) return;

        // Speed control
        if (up) {
            player.speed = Math.min(player.speed + 0.15, player.maxSpeed);
        } else if (down) {
            player.speed = Math.max(player.speed - 0.2, 0);
        } else {
            player.speed *= 0.98; // Friction
        }

        // Steering
        if (left) {
            player.x -= 3;
        }
        if (right) {
            player.x += 3;
        }

        // Keep within road bounds
        player.x = Math.max(ROAD_OFFSET + 10, Math.min(ROAD_OFFSET + 490, player.x));

        // Move forward
        player.y -= player.speed;

        // Generate exhaust particles
        if (player.speed > 0.5) {
            particles.add(new Particle(
                    player.x + 15 + (Math.random() - 0.5) * 10,
                    player.y + 40,
                    (float)(Math.random() - 0.5) * 1.5f,
                    (float)(0.5 + Math.random() * 1),
                    10 + (int)(Math.random() * 15),
                    new Color(150, 150, 150, 100)
            ));
        }

        // Check finish
        if (player.y <= FINISH_LINE) {
            player.isFinished = true;
            player.finishTime = gameTime;
            winner = player.name;
            state = GameState.GAME_OVER;
        }
    }

    private void updateComputerPlayer() {
        if (computer.isFinished) return;

        // Computer AI logic
        double targetY = FINISH_LINE + 100 + Math.random() * 50;
        double distanceToFinish = computer.y - targetY;

        // Speed control
        if (distanceToFinish > 100) {
            computer.speed = Math.min(computer.speed + 0.2, computer.maxSpeed);
        } else if (distanceToFinish < 50) {
            computer.speed = Math.max(computer.speed - 0.3, 0.5);
        } else {
            computer.speed = Math.min(computer.speed + 0.05, computer.maxSpeed);
        }

        // Random steering with smooth following
        double targetX = ROAD_OFFSET + 250 + Math.sin(gameTime * 0.001) * 100;
        double dx = targetX - computer.x;
        computer.x += dx * 0.03;

        // Keep within road bounds
        computer.x = Math.max(ROAD_OFFSET + 10, Math.min(ROAD_OFFSET + 490, computer.x));

        // Move forward
        computer.y -= computer.speed;

        // Generate exhaust particles
        if (computer.speed > 0.5) {
            particles.add(new Particle(
                    computer.x + 15 + (Math.random() - 0.5) * 10,
                    computer.y + 40,
                    (float)(Math.random() - 0.5) * 1.5f,
                    (float)(0.5 + Math.random() * 1),
                    10 + (int)(Math.random() * 15),
                    new Color(150, 150, 150, 100)
            ));
        }

        // Check finish
        if (computer.y <= FINISH_LINE) {
            computer.isFinished = true;
            computer.finishTime = gameTime;
            winner = "Computer";
            state = GameState.GAME_OVER;
        }
    }

    private void checkGameConditions() {
        // Check if all players finished
        if (isTwoPlayer && player1.isFinished && player2.isFinished) {
            state = GameState.GAME_OVER;
            winner = (player1.finishTime < player2.finishTime) ? player1.name : player2.name;
        }
    }

    private void updateParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.isDead()) {
                particles.remove(i);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (state == GameState.MENU) {
            drawMenu(g2d);
        } else if (state == GameState.PLAYING || state == GameState.GAME_OVER) {
            drawGame(g2d);
        }
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 60));
        g.drawString("GLOBAL RACING TOUR", 250, 300);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("MADRID", 500, 370);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("Press any key to start", 450, 500);
    }

    private void drawGame(Graphics2D g) {
        // Draw sky gradient
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, HEIGHT, new Color(255, 182, 193));
        g.setPaint(skyGradient);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw Madrid scenery
        drawScenery(g);

        // Draw road
        drawRoad(g);

        // Draw players
        if (!isTwoPlayer && computer != null) {
            drawPlayer(g, computer);
        }
        drawPlayer(g, player1);
        if (isTwoPlayer) {
            drawPlayer(g, player2);
        }

        // Draw particles
        for (Particle p : particles) {
            p.draw(g);
        }

        // Draw HUD
        drawHUD(g);

        // Draw game over overlay
        if (state == GameState.GAME_OVER) {
            drawGameOver(g);
        }
    }

    private void drawRoad(Graphics2D g) {
        // Road base
        g.setColor(new Color(50, 50, 50));
        g.fillRect(ROAD_OFFSET, 0, 540, HEIGHT);

        // Road edges
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));
        g.drawRect(ROAD_OFFSET, 0, 540, HEIGHT);

        // Lane markings (with animation)
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        for (RoadMarking rm : roadMarkings) {
            double y = (rm.y + roadOffset) % (20 * 80);
            if (y < HEIGHT) {
                g.drawLine(rm.x, (int)y, rm.x, (int)(y + 30));
            }
        }

        // Finish line
        g.setColor(Color.BLACK);
        g.fillRect(ROAD_OFFSET, FINISH_LINE - 10, 540, 20);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("FINISH", ROAD_OFFSET + 230, FINISH_LINE + 5);

        // Checkered pattern
        for (int i = 0; i < 540; i += 30) {
            for (int j = 0; j < 20; j += 30) {
                boolean even = ((i / 30) + (j / 30)) % 2 == 0;
                g.setColor(even ? Color.BLACK : Color.WHITE);
                g.fillRect(ROAD_OFFSET + i, FINISH_LINE - 10 + j, 30, 30);
            }
        }
    }

    private void drawScenery(Graphics2D g) {
        // Draw buildings
        for (Building b : buildings) {
            b.draw(g);
        }

        // Draw trees
        for (Tree t : trees) {
            t.draw(g);
        }

        // Draw street lights
        for (StreetLight sl : streetLights) {
            sl.draw(g);
        }

        // Draw sun
        g.setColor(new Color(255, 200, 50));
        g.fillOval(WIDTH - 100, 50, 60, 60);

        // Draw clouds
        g.setColor(new Color(255, 255, 255, 180));
        for (int i = 0; i < 4; i++) {
            int x = (i * 300 + (int)(gameTime * 0.1)) % (WIDTH + 100) - 100;
            g.fillOval(x, 60 + i * 40, 80, 40);
            g.fillOval(x + 30, 50 + i * 40, 60, 40);
            g.fillOval(x + 60, 60 + i * 40, 70, 40);
        }
    }

    private void drawPlayer(Graphics2D g, Player p) {
        // Car body
        g.setColor(p.color);
        g.fillRoundRect(p.x, (int)p.y, 40, 60, 10, 10);

        // Car windows
        g.setColor(new Color(173, 216, 230, 150));
        g.fillRoundRect(p.x + 5, (int)p.y + 5, 12, 15, 5, 5);
        g.fillRoundRect(p.x + 23, (int)p.y + 5, 12, 15, 5, 5);
        g.fillRoundRect(p.x + 5, (int)p.y + 25, 12, 15, 5, 5);
        g.fillRoundRect(p.x + 23, (int)p.y + 25, 12, 15, 5, 5);

        // Headlights
        g.setColor(Color.YELLOW);
        g.fillOval(p.x + 5, (int)p.y - 5, 8, 5);
        g.fillOval(p.x + 27, (int)p.y - 5, 8, 5);

        // Tail lights
        g.setColor(Color.RED);
        g.fillOval(p.x + 5, (int)p.y + 60, 8, 5);
        g.fillOval(p.x + 27, (int)p.y + 60, 8, 5);

        // Player name
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(p.name, p.x, (int)p.y - 10);

        // Speed indicator
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString("Speed: " + (int)(p.speed * 10) + " km/h", p.x, (int)p.y - 25);

        // Finish indicator
        if (p.isFinished) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("✓ FINISHED!", p.x - 10, (int)p.y - 45);
        }
    }

    private void drawHUD(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));

        // Timer
        g.drawString("Time: " + (gameTime / 60) + "s", 20, 30);

        // Controls info
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Player 1: Arrow Keys", 20, 50);
        if (isTwoPlayer) {
            g.drawString("Player 2: WASD", 20, 70);
        } else {
            g.drawString("VS Computer", 20, 70);
        }
    }

    private void drawGameOver(Graphics2D g) {
        // Semi-transparent overlay
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 60));
        g.drawString("RACE FINISHED!", 350, 300);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Winner: " + winner + " 🏆", 400, 400);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("Press 'R' to Restart", 480, 500);
        g.drawString("Press 'Q' to Quit", 490, 540);
    }

    @Override
    public void actionPerformed(ActionEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;

        if (e.getKeyCode() == KeyEvent.VK_R && state == GameState.GAME_OVER) {
            restartGame();
        }
        if (e.getKeyCode() == KeyEvent.VK_Q && state == GameState.GAME_OVER) {
            System.exit(0);
        }
        if (state == GameState.MENU) {
            state = GameState.PLAYING;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void restartGame() {
        gameTimer.cancel();
        initializeGame();
        startGameLoop();
    }

    // Inner classes
    class Player {
        String name;
        Color color;
        double x, y;
        double speed = 0;
        double maxSpeed = 4.0;
        boolean isFinished = false;
        int finishTime = 0;
        boolean isComputer = false;

        Player(String name, Color color, int x) {
            this.name = name;
            this.color = color;
            this.x = x;
            this.y = HEIGHT - 100;
        }
    }

    class RoadMarking {
        int x;
        int y;
        RoadMarking(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    class Building {
        int x, y, width, height;
        Color color;
        int windows;

        Building(int x, int y, int width, int height, Color color) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
            this.windows = (width / 15) * (height / 15);
        }

        void draw(Graphics2D g) {
            g.setColor(color);
            g.fillRect(x, y, width, height);

            // Windows
            g.setColor(new Color(255, 255, 200, 100));
            int winWidth = width / 5;
            int winHeight = height / 8;
            for (int i = 0; i < width / 15; i++) {
                for (int j = 0; j < height / 15; j++) {
                    if (i % 2 == 0 || j % 2 == 0) {
                        g.fillRect(x + 5 + i * 15, y + 5 + j * 15, 10, 10);
                    }
                }
            }
        }
    }

    class Tree {
        int x, y, trunkWidth, trunkHeight;
        int leafRadius;

        Tree(int x, int y, int trunkWidth, int trunkHeight) {
            this.x = x;
            this.y = y;
            this.trunkWidth = trunkWidth;
            this.trunkHeight = trunkHeight;
            this.leafRadius = 30 + (int)(Math.random() * 20);
        }

        void draw(Graphics2D g) {
            // Trunk
            g.setColor(new Color(101, 67, 33));
            g.fillRect(x, y + trunkHeight - 20, trunkWidth, trunkHeight);

            // Leaves
            g.setColor(new Color(34, 139, 34));
            g.fillOval(x - leafRadius/2, y - 10, leafRadius, leafRadius);
            g.fillOval(x - leafRadius/2 + 10, y - 20, leafRadius - 10, leafRadius - 10);

            // Leaf highlights
            g.setColor(new Color(144, 238, 144, 100));
            g.fillOval(x - leafRadius/3, y - 5, leafRadius/2, leafRadius/2);
        }
    }

    class StreetLight {
        int x, y;
        boolean isOn = true;

        StreetLight(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g) {
            // Pole
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x, y, 4, 100);

            // Light
            if (isOn) {
                g.setColor(Color.YELLOW);
                g.fillOval(x - 8, y - 5, 20, 15);
                // Glow effect
                g.setColor(new Color(255, 255, 0, 50));
                g.fillOval(x - 20, y - 15, 44, 35);
            }
        }
    }

    class Particle {
        float x, y, vx, vy;
        int size;
        Color color;
        int life;
        int maxLife;

        Particle(float x, float y, float vx, float vy, int size, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.maxLife = 20 + (int)(Math.random() * 30);
            this.life = maxLife;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.1f; // Gravity
            life--;
            if (life < maxLife * 0.3) {
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                        (int)(life * 255 / (maxLife * 0.3)));
            }
        }

        void draw(Graphics2D g) {
            if (life > 0) {
                g.setColor(color);
                g.fillOval((int)x, (int)y, size, size);
            }
        }

        boolean isDead() {
            return life <= 0;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Global Racing Tour - Madrid");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            GlobalRacingTourGame game = new GlobalRacingTourGame();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Focus the game panel for keyboard input
            game.requestFocusInWindow();
        });
    }
}