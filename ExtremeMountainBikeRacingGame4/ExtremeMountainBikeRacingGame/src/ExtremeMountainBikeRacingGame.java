import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;

/**
 * Extreme Mountain Bike Racing Game
 * Features realistic animation, terrain generation, and two-player support.
 * Control: Player1 (WASD), Player2 (Arrow Keys)
 *
 * @author AI Generated
 */
public class ExtremeMountainBikeRacingGame extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 700;
    private static final int GROUND_HEIGHT = 150;
    private static final int GRAVITY = 1;
    private static final int FRICTION = 1;
    private static final int PLAYER_WIDTH = 50;
    private static final int PLAYER_HEIGHT = 40;
    private static final int BIKE_WHEEL_RADIUS = 12;

    private Timer timer;
    private Player player1;
    private Player player2;
    private ComputerPlayer computer;
    private Terrain terrain;
    private int gameMode; // 1 = vs Computer, 2 = vs Player
    private boolean gameOver;
    private String winnerMessage = "";
    private int raceDistance = 5000;
    private Camera camera;
    private List<Particle> particles;
    private List<Obstacle> obstacles;
    private boolean showInstructions = true;
    private int instructionTimer = 0;

    public ExtremeMountainBikeRacingGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky blue
        setFocusable(true);
        addKeyListener(this);

        // Initialize game components
        terrain = new Terrain(raceDistance);
        camera = new Camera(WIDTH, HEIGHT);
        particles = new ArrayList<>();
        obstacles = new ArrayList<>();
        generateObstacles();

        // Initialize players
        player1 = new Player(100, 0, Color.RED, "P1");
        player2 = new Player(200, 0, Color.BLUE, "P2");
        computer = new ComputerPlayer(150, 0, Color.GREEN, "CPU");

        // Position players on terrain
        player1.y = terrain.getHeightAt(player1.x) - PLAYER_HEIGHT;
        player2.y = terrain.getHeightAt(player2.x) - PLAYER_HEIGHT;
        computer.y = terrain.getHeightAt(computer.x) - PLAYER_HEIGHT;

        // Select game mode
        selectGameMode();

        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    private void selectGameMode() {
        String[] options = {"vs Computer", "Two Players"};
        int choice = JOptionPane.showOptionDialog(this,
                "Select Game Mode:",
                "Extreme Mountain Bike Racing",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        gameMode = choice + 1;
    }

    private void generateObstacles() {
        Random rand = new Random();
        for (int i = 0; i < 30; i++) {
            int x = 300 + rand.nextInt(raceDistance - 600);
            int y = terrain.getHeightAt(x) - 30 - rand.nextInt(20);
            obstacles.add(new Obstacle(x, y, 20 + rand.nextInt(20)));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            updateGame();
        } else {
            if (instructionTimer < 120) {
                instructionTimer++;
            } else {
                resetGame();
            }
        }
        repaint();
    }

    private void updateGame() {
        // Update player 1
        updatePlayer(player1);

        // Update player 2 or computer
        if (gameMode == 2) {
            updatePlayer(player2);
        } else {
            computer.update(player1.x, terrain);
            // Computer physics
            applyPhysics(computer);
            checkCollisions(computer);
            checkObstacleCollision(computer);
        }

        // Update camera to follow leader
        int leaderX = Math.max(player1.x, gameMode == 2 ? player2.x : computer.x);
        camera.update(leaderX);

        // Update particles
        updateParticles();

        // Check win conditions
        if (player1.x >= raceDistance) {
            gameOver = true;
            winnerMessage = "Player 1 Wins!";
        } else if (gameMode == 2 && player2.x >= raceDistance) {
            gameOver = true;
            winnerMessage = "Player 2 Wins!";
        } else if (gameMode == 1 && computer.x >= raceDistance) {
            gameOver = true;
            winnerMessage = "Computer Wins!";
        }
    }

    private void updatePlayer(Player player) {
        player.applyGravity(GRAVITY);
        player.y += player.vy;

        // Terrain following
        int terrainHeight = terrain.getHeightAt(player.x);
        if (player.y + PLAYER_HEIGHT > terrainHeight) {
            player.y = terrainHeight - PLAYER_HEIGHT;
            player.vy = 0;
            player.onGround = true;
        } else {
            player.onGround = false;
        }

        // Apply horizontal movement
        player.x += player.vx;

        // Friction
        if (player.vx > 0) {
            player.vx = Math.max(0, player.vx - FRICTION * 0.5);
        } else if (player.vx < 0) {
            player.vx = Math.min(0, player.vx + FRICTION * 0.5);
        }

        // Clamp position
        player.x = Math.max(0, Math.min(raceDistance, player.x));

        // Generate particles for speed effect
        if (Math.abs(player.vx) > 2) {
            particles.add(new Particle(
                    player.x + PLAYER_WIDTH/2,
                    player.y + PLAYER_HEIGHT,
                    new Color(139, 69, 19),
                    3 + (int)(Math.random() * 3)
            ));
        }

        checkObstacleCollision(player);
    }

    private void applyPhysics(Player player) {
        // Similar to updatePlayer but without input
        player.applyGravity(GRAVITY);
        player.y += player.vy;

        int terrainHeight = terrain.getHeightAt(player.x);
        if (player.y + PLAYER_HEIGHT > terrainHeight) {
            player.y = terrainHeight - PLAYER_HEIGHT;
            player.vy = 0;
            player.onGround = true;
        } else {
            player.onGround = false;
        }

        player.x += player.vx;
        player.x = Math.max(0, Math.min(raceDistance, player.x));
    }

    private void checkObstacleCollision(Player player) {
        for (Obstacle obs : obstacles) {
            if (Math.abs(player.x - obs.x) < 20 &&
                    Math.abs(player.y + PLAYER_HEIGHT - obs.y) < 20) {
                // Bounce back
                player.vx = -3;
                player.vy = -5;
                // Add crash particles
                for (int i = 0; i < 10; i++) {
                    particles.add(new Particle(
                            player.x + PLAYER_WIDTH/2,
                            player.y + PLAYER_HEIGHT/2,
                            Color.ORANGE,
                            5 + (int)(Math.random() * 5)
                    ));
                }
            }
        }
    }

    private void checkCollisions(Player player) {
        // For computer - similar to obstacle check but for terrain features
        if (Math.random() < 0.01) {
            player.vy = -8 + Math.random() * 4;
        }
    }

    private void updateParticles() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update();
            if (p.life <= 0) {
                it.remove();
            }
        }
        // Limit particles
        if (particles.size() > 200) {
            particles.subList(0, particles.size() - 100).clear();
        }
    }

    private void resetGame() {
        player1.x = 100;
        player1.y = terrain.getHeightAt(100) - PLAYER_HEIGHT;
        player1.vx = 0;
        player1.vy = 0;
        player2.x = 200;
        player2.y = terrain.getHeightAt(200) - PLAYER_HEIGHT;
        player2.vx = 0;
        player2.vy = 0;
        computer.x = 150;
        computer.y = terrain.getHeightAt(150) - PLAYER_HEIGHT;
        computer.vx = 0;
        computer.vy = 0;
        particles.clear();
        gameOver = false;
        winnerMessage = "";
        instructionTimer = 0;
        camera.x = 0;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Apply camera transform
        g2d.translate(-camera.x, 0);

        // Draw sky gradient
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, HEIGHT/2, new Color(255, 255, 255));
        g2d.setPaint(skyGradient);
        g2d.fillRect(camera.x, 0, WIDTH, HEIGHT);

        // Draw sun
        g2d.setColor(new Color(255, 200, 50));
        g2d.fillOval(camera.x + WIDTH - 120, 40, 60, 60);
        g2d.setColor(new Color(255, 220, 100, 100));
        g2d.fillOval(camera.x + WIDTH - 130, 30, 80, 80);

        // Draw mountains
        drawMountains(g2d);

        // Draw terrain
        terrain.draw(g2d, camera.x);

        // Draw obstacles
        for (Obstacle obs : obstacles) {
            obs.draw(g2d);
        }

        // Draw particles
        for (Particle p : particles) {
            p.draw(g2d);
        }

        // Draw players
        player1.draw(g2d);
        if (gameMode == 2) {
            player2.draw(g2d);
        } else {
            computer.draw(g2d);
        }

        // Draw HUD
        drawHUD(g2d);

        // Draw instructions
        if (showInstructions) {
            drawInstructions(g2d);
        }

        // Draw game over message
        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(camera.x, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            String msg = winnerMessage + " Press any key to restart";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(msg, camera.x + (WIDTH - fm.stringWidth(msg)) / 2, HEIGHT/2);
        }
    }

    private void drawMountains(Graphics2D g2d) {
        // Draw 3 layers of mountains with parallax
        int[] mountainColors = {0x4A6B8A, 0x5A7B9A, 0x6A8BAA};
        for (int layer = 0; layer < 3; layer++) {
            g2d.setColor(new Color(mountainColors[layer]));
            int baseY = HEIGHT - GROUND_HEIGHT - 50 + layer * 20;
            int amplitude = 100 - layer * 30;
            int frequency = 50 + layer * 20;
            int offset = -camera.x / (5 - layer);

            Path2D mountain = new Path2D.Double();
            mountain.moveTo(camera.x - 50, baseY + amplitude);
            for (int x = camera.x - 50; x <= camera.x + WIDTH + 50; x += 2) {
                double y = baseY + amplitude * Math.sin((x + offset) * 0.01 * frequency / 50);
                mountain.lineTo(x, y);
            }
            mountain.lineTo(camera.x + WIDTH + 50, HEIGHT + 50);
            mountain.lineTo(camera.x - 50, HEIGHT + 50);
            mountain.closePath();
            g2d.fill(mountain);
        }
    }

    private void drawHUD(Graphics2D g2d) {
        // Player 1 info
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("P1: " + (int)(player1.x / raceDistance * 100) + "%", camera.x + 20, 30);
        drawSpeedometer(g2d, camera.x + 20, 40, player1.vx, Color.RED);

        if (gameMode == 2) {
            g2d.setColor(Color.BLUE);
            g2d.drawString("P2: " + (int)(player2.x / raceDistance * 100) + "%", camera.x + 120, 30);
            drawSpeedometer(g2d, camera.x + 120, 40, player2.vx, Color.BLUE);
        } else {
            g2d.setColor(Color.GREEN);
            g2d.drawString("CPU: " + (int)(computer.x / raceDistance * 100) + "%", camera.x + 120, 30);
            drawSpeedometer(g2d, camera.x + 120, 40, computer.vx, Color.GREEN);
        }

        // Race progress bar
        g2d.setColor(Color.GRAY);
        g2d.fillRect(camera.x + 20, HEIGHT - 40, 300, 20);
        g2d.setColor(Color.GREEN);
        int progress1 = (int)(player1.x / raceDistance * 300);
        g2d.fillRect(camera.x + 20, HEIGHT - 40, Math.min(progress1, 300), 20);
        if (gameMode == 2) {
            g2d.setColor(Color.BLUE);
            int progress2 = (int)(player2.x / raceDistance * 300);
            g2d.fillRect(camera.x + 20, HEIGHT - 40, Math.min(progress2, 300), 10);
        } else {
            g2d.setColor(Color.GREEN);
            int progress2 = (int)(computer.x / raceDistance * 300);
            g2d.fillRect(camera.x + 20, HEIGHT - 40, Math.min(progress2, 300), 10);
        }
    }

    private void drawSpeedometer(Graphics2D g2d, int x, int y, double speed, Color color) {
        g2d.setColor(color);
        int speedValue = (int)(Math.abs(speed) * 10);
        g2d.fillRect(x, y + 10, Math.min(speedValue, 80), 8);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(x, y + 10, 80, 8);
    }

    private void drawInstructions(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(camera.x + WIDTH/2 - 200, HEIGHT/2 - 100, 400, 200);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        String[] instructions = {
                "W/A/S/D - Player 1",
                "Arrow Keys - Player 2 (in 2P mode)",
                "Press 'R' to reset",
                "Press 'H' to toggle instructions"
        };
        for (int i = 0; i < instructions.length; i++) {
            g2d.drawString(instructions[i], camera.x + WIDTH/2 - 180, HEIGHT/2 - 60 + i * 30);
        }
        g2d.drawString("Press any key to start", camera.x + WIDTH/2 - 80, HEIGHT/2 + 60);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        showInstructions = false;

        if (gameOver) {
            resetGame();
            return;
        }

        int key = e.getKeyCode();

        // Player 1 controls (WASD)
        if (key == KeyEvent.VK_A) {
            player1.vx = -5;
        } else if (key == KeyEvent.VK_D) {
            player1.vx = 5;
        } else if (key == KeyEvent.VK_W && player1.onGround) {
            player1.vy = -12;
            player1.onGround = false;
            // Jump particles
            for (int i = 0; i < 8; i++) {
                particles.add(new Particle(
                        player1.x + PLAYER_WIDTH/2,
                        player1.y + PLAYER_HEIGHT,
                        new Color(160, 120, 80),
                        3 + (int)(Math.random() * 3)
                ));
            }
        }

        // Player 2 controls (Arrow keys)
        if (gameMode == 2) {
            if (key == KeyEvent.VK_LEFT) {
                player2.vx = -5;
            } else if (key == KeyEvent.VK_RIGHT) {
                player2.vx = 5;
            } else if (key == KeyEvent.VK_UP && player2.onGround) {
                player2.vy = -12;
                player2.onGround = false;
            }
        }

        // Reset
        if (key == KeyEvent.VK_R) {
            resetGame();
        }

        // Toggle instructions
        if (key == KeyEvent.VK_H) {
            showInstructions = !showInstructions;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_D) {
            player1.vx = 0;
        }
        if (gameMode == 2) {
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                player2.vx = 0;
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner classes
    class Player {
        int x, y;
        double vx, vy;
        Color color;
        String name;
        boolean onGround;

        Player(int x, int y, Color color, String name) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.name = name;
            this.vx = 0;
            this.vy = 0;
            this.onGround = true;
        }

        void applyGravity(int gravity) {
            vy += gravity * 0.8;
            if (vy > 15) vy = 15;
        }

        void draw(Graphics2D g2d) {
            // Body
            g2d.setColor(color);
            g2d.fillRect(x, y, PLAYER_WIDTH, PLAYER_HEIGHT);

            // Rider
            g2d.setColor(new Color(200, 100, 50));
            g2d.fillOval(x + 15, y - 15, 20, 20);

            // Helmet
            g2d.setColor(color.darker());
            g2d.fillOval(x + 12, y - 20, 26, 15);

            // Bike wheels
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x + 5, y + PLAYER_HEIGHT - BIKE_WHEEL_RADIUS, BIKE_WHEEL_RADIUS * 2, BIKE_WHEEL_RADIUS * 2);
            g2d.fillOval(x + PLAYER_WIDTH - BIKE_WHEEL_RADIUS - 5, y + PLAYER_HEIGHT - BIKE_WHEEL_RADIUS, BIKE_WHEEL_RADIUS * 2, BIKE_WHEEL_RADIUS * 2);

            // Spokes
            g2d.setColor(Color.GRAY);
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                int cx1 = x + 5 + BIKE_WHEEL_RADIUS;
                int cy1 = y + PLAYER_HEIGHT;
                g2d.drawLine(cx1, cy1,
                        (int)(cx1 + BIKE_WHEEL_RADIUS * Math.cos(angle)),
                        (int)(cy1 + BIKE_WHEEL_RADIUS * Math.sin(angle)));
                int cx2 = x + PLAYER_WIDTH - 5 - BIKE_WHEEL_RADIUS;
                int cy2 = y + PLAYER_HEIGHT;
                g2d.drawLine(cx2, cy2,
                        (int)(cx2 + BIKE_WHEEL_RADIUS * Math.cos(angle + Math.PI/4)),
                        (int)(cy2 + BIKE_WHEEL_RADIUS * Math.sin(angle + Math.PI/4)));
            }

            // Name
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(name, x + 10, y - 25);
        }
    }

    class ComputerPlayer extends Player {
        ComputerPlayer(int x, int y, Color color, String name) {
            super(x, y, color, name);
        }

        void update(int playerX, Terrain terrain) {
            // AI logic - follow player with some randomness
            int targetX = playerX + 50;
            if (x < targetX - 20) {
                vx = Math.min(vx + 0.5, 4);
            } else if (x > targetX + 20) {
                vx = Math.max(vx - 0.5, -4);
            } else {
                vx = vx * 0.95;
            }

            // Random jumps
            if (Math.random() < 0.005 && onGround) {
                vy = -10 - Math.random() * 5;
                onGround = false;
            }

            // Avoid obstacles
            for (Obstacle obs : obstacles) {
                if (Math.abs(x - obs.x) < 30 && onGround) {
                    vy = -8 - Math.random() * 6;
                    onGround = false;
                    break;
                }
            }
        }
    }

    class Terrain {
        private int[] heights;
        private int width;

        Terrain(int width) {
            this.width = width;
            heights = new int[width];
            generateTerrain();
        }

        private void generateTerrain() {
            Random rand = new Random(42);
            int baseHeight = HEIGHT - GROUND_HEIGHT;
            heights[0] = baseHeight;

            for (int i = 1; i < width; i++) {
                double change = (rand.nextDouble() - 0.5) * 8;
                // Add some hills
                double hill = Math.sin(i * 0.01) * 30 + Math.sin(i * 0.025) * 15;
                heights[i] = (int)(heights[i-1] + change + hill * 0.1);
                heights[i] = Math.max(HEIGHT - 500, Math.min(HEIGHT - 100, heights[i]));
            }
        }

        int getHeightAt(int x) {
            if (x < 0) x = 0;
            if (x >= width) x = width - 1;
            return heights[x];
        }

        void draw(Graphics2D g2d, int cameraX) {
            // Draw ground with gradient
            GradientPaint groundGradient = new GradientPaint(0, HEIGHT - GROUND_HEIGHT,
                    new Color(34, 139, 34),
                    0, HEIGHT,
                    new Color(101, 67, 33));
            g2d.setPaint(groundGradient);

            Path2D ground = new Path2D.Double();
            ground.moveTo(cameraX - 50, heights[0]);

            for (int x = Math.max(0, cameraX - 50); x < Math.min(width, cameraX + WIDTH + 50); x++) {
                ground.lineTo(x, heights[x]);
            }

            ground.lineTo(cameraX + WIDTH + 50, HEIGHT + 50);
            ground.lineTo(cameraX - 50, HEIGHT + 50);
            ground.closePath();
            g2d.fill(ground);

            // Draw grass layer
            g2d.setColor(new Color(50, 180, 50));
            for (int x = Math.max(0, cameraX - 50); x < Math.min(width, cameraX + WIDTH + 50); x += 3) {
                int y = heights[x];
                g2d.drawLine(x, y, x + 2, y - 8 + (int)(Math.sin(x * 0.5) * 3));
            }
        }
    }

    class Obstacle {
        int x, y, size;
        Color color;

        Obstacle(int x, int y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = new Color(80 + (int)(Math.random() * 50), 60 + (int)(Math.random() * 30), 40 + (int)(Math.random() * 20));
        }

        void draw(Graphics2D g2d) {
            // Rock
            g2d.setColor(color);
            int[] xPoints = {x - size/2, x, x + size/2, x + size/3, x - size/3};
            int[] yPoints = {y + size/2, y - size/2, y + size/2, y + size/3, y + size/3};
            g2d.fillPolygon(xPoints, yPoints, 5);

            // Highlight
            g2d.setColor(color.brighter());
            g2d.fillOval(x - size/4, y - size/4, size/2, size/4);
        }
    }

    class Particle {
        double x, y, vx, vy;
        int size, life, maxLife;
        Color color;

        Particle(double x, double y, Color color, int size) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.size = size;
            this.life = 30 + (int)(Math.random() * 30);
            this.maxLife = life;
            this.vx = -2 + Math.random() * 4;
            this.vy = -3 + Math.random() * 2;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.2;
            life--;
            size = Math.max(1, size - 1);
        }

        void draw(Graphics2D g2d) {
            int alpha = (int)(255 * life / maxLife);
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g2d.fillOval((int)x - size/2, (int)y - size/2, size, size);
        }
    }

    class Camera {
        int x, y;
        int screenWidth, screenHeight;

        Camera(int screenWidth, int screenHeight) {
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.x = 0;
            this.y = 0;
        }

        void update(int targetX) {
            // Smooth camera follow
            int targetCameraX = targetX - screenWidth / 2;
            x += (targetCameraX - x) * 0.1;
            x = Math.max(0, x);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Extreme Mountain Bike Racing");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            ExtremeMountainBikeRacingGame game = new ExtremeMountainBikeRacingGame();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}