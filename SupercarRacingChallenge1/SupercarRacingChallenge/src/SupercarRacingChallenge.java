import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class SupercarRacingChallenge extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final int ROAD_LEFT = 150;
    private static final int ROAD_RIGHT = 850;
    private static final int ROAD_WIDTH = ROAD_RIGHT - ROAD_LEFT;
    private static final int LANE_WIDTH = ROAD_WIDTH / 3;
    private static final int CAR_WIDTH = 40;
    private static final int CAR_HEIGHT = 70;
    private static final int MAX_SPEED = 12;
    private static final int MIN_SPEED = 3;
    private static final int OBSTACLE_WIDTH = 60;
    private static final int OBSTACLE_HEIGHT = 60;
    private static final int SCROLL_SPEED = 8;

    // Game state
    private Timer timer;
    private boolean gameRunning = true;
    private boolean gameOver = false;
    private boolean vsComputer = false;
    private int player1Score = 0;
    private int player2Score = 0;
    private int roadOffset = 0;

    // Player 1 (Blue car)
    private int p1X = ROAD_LEFT + LANE_WIDTH / 2 - CAR_WIDTH / 2;
    private int p1Y = HEIGHT - 150;
    private int p1Speed = 5;

    // Player 2 (Red car)
    private int p2X = ROAD_LEFT + 2 * LANE_WIDTH + LANE_WIDTH / 2 - CAR_WIDTH / 2;
    private int p2Y = HEIGHT - 150;
    private int p2Speed = 5;

    // Computer AI
    private Random random = new Random();
    private int aiTargetLane = 1;

    // Obstacles
    private ArrayList<Obstacle> obstacles = new ArrayList<>();
    private int obstacleSpawnCounter = 0;
    private int obstacleSpawnDelay = 60;

    // Particles for effects
    private ArrayList<Particle> particles = new ArrayList<>();

    // Road lines
    private ArrayList<RoadLine> roadLines = new ArrayList<>();

    // Key states
    private boolean up1Pressed = false;
    private boolean down1Pressed = false;
    private boolean left1Pressed = false;
    private boolean right1Pressed = false;
    private boolean up2Pressed = false;
    private boolean down2Pressed = false;
    private boolean left2Pressed = false;
    private boolean right2Pressed = false;

    public SupercarRacingChallenge() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        // Initialize road lines
        for (int i = -HEIGHT; i < HEIGHT; i += 80) {
            roadLines.add(new RoadLine(ROAD_LEFT + LANE_WIDTH, i));
            roadLines.add(new RoadLine(ROAD_LEFT + 2 * LANE_WIDTH, i));
        }

        timer = new Timer(16, this); // ~60 FPS
        timer.start();

        // Show menu
        showMenu();
    }

    private void showMenu() {
        String[] options = {"2 Players", "vs Computer", "Quit"};
        int choice = JOptionPane.showOptionDialog(this,
                "🏁 SUPER CAR RACING CHALLENGE 🏁\n\nChoose game mode:",
                "Super Car Racing Challenge",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 0) {
            vsComputer = false;
            startGame();
        } else if (choice == 1) {
            vsComputer = true;
            startGame();
        } else {
            System.exit(0);
        }
    }

    private void startGame() {
        gameRunning = true;
        gameOver = false;
        player1Score = 0;
        player2Score = 0;
        obstacles.clear();
        particles.clear();
        p1X = ROAD_LEFT + LANE_WIDTH / 2 - CAR_WIDTH / 2;
        p1Y = HEIGHT - 150;
        p2X = ROAD_LEFT + 2 * LANE_WIDTH + LANE_WIDTH / 2 - CAR_WIDTH / 2;
        p2Y = HEIGHT - 150;
        p1Speed = 5;
        p2Speed = 5;
        requestFocusInWindow();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning && !gameOver) {
            update();
        }
        repaint();
    }

    private void update() {
        // Update road scroll
        roadOffset = (roadOffset + SCROLL_SPEED) % 80;
        for (RoadLine line : roadLines) {
            line.y += SCROLL_SPEED;
            if (line.y > HEIGHT) {
                line.y -= HEIGHT + 80;
            }
        }

        // Player 1 movement
        if (up1Pressed) p1Speed = Math.min(p1Speed + 1, MAX_SPEED);
        else if (down1Pressed) p1Speed = Math.max(p1Speed - 1, MIN_SPEED);
        else p1Speed = Math.max(p1Speed - 1, 5);

        if (left1Pressed) p1X = Math.max(ROAD_LEFT + 10, p1X - 6);
        if (right1Pressed) p1X = Math.min(ROAD_RIGHT - CAR_WIDTH - 10, p1X + 6);

        // Player 2 / AI movement
        if (vsComputer) {
            // AI logic
            updateAI();
        } else {
            if (up2Pressed) p2Speed = Math.min(p2Speed + 1, MAX_SPEED);
            else if (down2Pressed) p2Speed = Math.max(p2Speed - 1, MIN_SPEED);
            else p2Speed = Math.max(p2Speed - 1, 5);

            if (left2Pressed) p2X = Math.max(ROAD_LEFT + 10, p2X - 6);
            if (right2Pressed) p2X = Math.min(ROAD_RIGHT - CAR_WIDTH - 10, p2X + 6);
        }

        // Spawn obstacles
        obstacleSpawnCounter++;
        if (obstacleSpawnCounter >= obstacleSpawnDelay) {
            spawnObstacle();
            obstacleSpawnCounter = 0;
            obstacleSpawnDelay = Math.max(25, 60 - (player1Score + player2Score) / 5);
        }

        // Update obstacles
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obs = obstacles.get(i);
            obs.y += SCROLL_SPEED + 2;

            // Check collision with player 1
            if (rectCollide(p1X, p1Y, CAR_WIDTH, CAR_HEIGHT, obs.x, obs.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT)) {
                gameOver = true;
                createExplosion(p1X + CAR_WIDTH / 2, p1Y + CAR_HEIGHT / 2, Color.BLUE);
                break;
            }

            // Check collision with player 2
            if (rectCollide(p2X, p2Y, CAR_WIDTH, CAR_HEIGHT, obs.x, obs.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT)) {
                gameOver = true;
                createExplosion(p2X + CAR_WIDTH / 2, p2Y + CAR_HEIGHT / 2, Color.RED);
                break;
            }

            if (obs.y > HEIGHT) {
                obstacles.remove(i);
                player1Score++;
                player2Score++;
            }
        }

        // Update particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.3;
            p.life--;
            if (p.life <= 0) {
                particles.remove(i);
            }
        }

        // Add exhaust particles
        if (random.nextInt(3) == 0) {
            particles.add(new Particle(p1X + CAR_WIDTH / 2, p1Y + CAR_HEIGHT,
                    random.nextDouble() * 2 - 1, 2 + random.nextDouble() * 2,
                    Color.CYAN, 15));
        }
        if (random.nextInt(3) == 0) {
            particles.add(new Particle(p2X + CAR_WIDTH / 2, p2Y + CAR_HEIGHT,
                    random.nextDouble() * 2 - 1, 2 + random.nextDouble() * 2,
                    Color.ORANGE, 15));
        }
    }

    private void updateAI() {
        // Find nearest obstacle ahead
        Obstacle nearest = null;
        int nearestDist = Integer.MAX_VALUE;

        for (Obstacle obs : obstacles) {
            if (obs.y < p2Y && obs.y > p2Y - 300) {
                int dist = p2Y - obs.y;
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = obs;
                }
            }
        }

        if (nearest != null) {
            // Dodge the obstacle
            int obsCenter = nearest.x + OBSTACLE_WIDTH / 2;
            int carCenter = p2X + CAR_WIDTH / 2;

            if (carCenter < obsCenter + 30 && carCenter > obsCenter - 30) {
                // Need to dodge
                if (carCenter < WIDTH / 2) {
                    aiTargetLane = 2; // Move right
                } else {
                    aiTargetLane = 0; // Move left
                }
            }
        } else {
            // Random lane changes occasionally
            if (random.nextInt(120) == 0) {
                aiTargetLane = random.nextInt(3);
            }
        }

        // Move toward target lane
        int targetX = ROAD_LEFT + aiTargetLane * LANE_WIDTH + LANE_WIDTH / 2 - CAR_WIDTH / 2;
        if (p2X < targetX - 5) {
            p2X = Math.min(p2X + 5, targetX);
        } else if (p2X > targetX + 5) {
            p2X = Math.max(p2X - 5, targetX);
        }

        // Adjust speed
        p2Speed = 6 + random.nextInt(3);
    }

    private void spawnObstacle() {
        int lane = random.nextInt(3);
        int x = ROAD_LEFT + lane * LANE_WIDTH + LANE_WIDTH / 2 - OBSTACLE_WIDTH / 2;
        Color color = new Color(random.nextInt(150) + 100, random.nextInt(150) + 100, random.nextInt(150) + 100);
        obstacles.add(new Obstacle(x, -OBSTACLE_HEIGHT, color));
    }

    private boolean rectCollide(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }

    private void createExplosion(int x, int y, Color baseColor) {
        for (int i = 0; i < 30; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 2 + random.nextDouble() * 8;
            particles.add(new Particle(x, y,
                    Math.cos(angle) * speed, Math.sin(angle) * speed - 3,
                    baseColor, 30 + random.nextInt(30)));
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw sky gradient
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(25, 25, 60), 0, HEIGHT, new Color(60, 30, 90));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw stars
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < 50; i++) {
            int sx = (i * 97 + roadOffset) % WIDTH;
            int sy = (i * 53) % (HEIGHT / 2);
            g2d.fillOval(sx, sy, 2, 2);
        }

        // Draw road
        g2d.setColor(new Color(40, 40, 45));
        g2d.fillRect(ROAD_LEFT, 0, ROAD_WIDTH, HEIGHT);

        // Draw road edges with glow
        g2d.setColor(new Color(80, 200, 255));
        g2d.fillRect(ROAD_LEFT - 3, 0, 6, HEIGHT);
        g2d.fillRect(ROAD_RIGHT - 3, 0, 6, HEIGHT);

        // Draw road lines
        for (RoadLine line : roadLines) {
            g2d.setColor(new Color(255, 255, 255, 180));
            g2d.fillRect((int)line.x - 2, (int)line.y, 4, 40);
        }

        // Draw road texture (subtle)
        g2d.setColor(new Color(50, 50, 55, 100));
        for (int i = 0; i < HEIGHT; i += 20) {
            g2d.fillRect(ROAD_LEFT, i, ROAD_WIDTH, 2);
        }

        // Draw obstacles
        for (Obstacle obs : obstacles) {
            // Obstacle shadow
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRoundRect(obs.x + 4, obs.y + 4, OBSTACLE_WIDTH, OBSTACLE_HEIGHT, 10, 10);

            // Obstacle body
            g2d.setColor(obs.color);
            g2d.fillRoundRect(obs.x, obs.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT, 10, 10);

            // Obstacle details
            g2d.setColor(obs.color.brighter());
            g2d.fillRoundRect(obs.x + 5, obs.y + 5, OBSTACLE_WIDTH - 10, OBSTACLE_HEIGHT - 10, 8, 8);

            // Warning stripes
            g2d.setColor(Color.YELLOW);
            g2d.fillRect(obs.x + 5, obs.y + 20, OBSTACLE_WIDTH - 10, 4);
            g2d.fillRect(obs.x + 5, obs.y + 36, OBSTACLE_WIDTH - 10, 4);
        }

        // Draw particles
        for (Particle p : particles) {
            float alpha = Math.max(0, Math.min(1, p.life / 30f));
            g2d.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int)(alpha * 255)));
            g2d.fillOval((int)p.x - p.size/2, (int)p.y - p.size/2, p.size, p.size);
        }

        // Draw Player 1 car (Blue)
        drawCar(g2d, p1X, p1Y, Color.BLUE, Color.CYAN, true);

        // Draw Player 2 car (Red) or AI
        drawCar(g2d, p2X, p2Y, vsComputer ? Color.ORANGE : Color.RED,
                vsComputer ? Color.YELLOW : Color.ORANGE, false);

        // Draw speed lines effect when going fast
        if (p1Speed > 7) {
            g2d.setColor(new Color(100, 200, 255, 100));
            for (int i = 0; i < 5; i++) {
                int lx = p1X + random.nextInt(CAR_WIDTH);
                g2d.fillRect(lx, p1Y + CAR_HEIGHT + i * 15, 2, 10 + i * 5);
            }
        }
        if (p2Speed > 7) {
            g2d.setColor(new Color(255, 150, 100, 100));
            for (int i = 0; i < 5; i++) {
                int lx = p2X + random.nextInt(CAR_WIDTH);
                g2d.fillRect(lx, p2Y + CAR_HEIGHT + i * 15, 2, 10 + i * 5);
            }
        }

        // Draw HUD
        g2d.setFont(new Font("Arial", Font.BOLD, 20));

        // Player 1 score
        g2d.setColor(Color.CYAN);
        g2d.drawString("P1: " + player1Score, 30, 40);
        g2d.fillRect(30, 50, Math.min(player1Score * 3, 150), 10);

        // Player 2 / AI score
        g2d.setColor(vsComputer ? Color.YELLOW : Color.ORANGE);
        g2d.drawString(vsComputer ? "AI: " + player2Score : "P2: " + player2Score, WIDTH - 150, 40);
        g2d.fillRect(WIDTH - 150, 50, Math.min(player2Score * 3, 150), 10);

        // Speed indicators
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Speed: " + (p1Speed * 20) + " km/h", 30, HEIGHT - 50);
        g2d.drawString("Speed: " + (p2Speed * 20) + " km/h", WIDTH - 150, HEIGHT - 50);

        // Game over overlay
        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 60));
            String msg = "CRASH!";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(msg, (WIDTH - fm.stringWidth(msg)) / 2, HEIGHT / 2 - 50);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 30));
            String winner;
            if (player1Score > player2Score) {
                winner = "Player 1 Wins!";
            } else if (player2Score > player1Score) {
                winner = vsComputer ? "Computer Wins!" : "Player 2 Wins!";
            } else {
                winner = "It's a Tie!";
            }
            fm = g2d.getFontMetrics();
            g2d.drawString(winner, (WIDTH - fm.stringWidth(winner)) / 2, HEIGHT / 2 + 20);

            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            String restart = "Press R to restart or ESC to quit";
            fm = g2d.getFontMetrics();
            g2d.drawString(restart, (WIDTH - fm.stringWidth(restart)) / 2, HEIGHT / 2 + 80);
        }

        // Instructions
        if (!gameOver) {
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("P1: WASD | P2: Arrows", ROAD_LEFT + ROAD_WIDTH / 2 - 80, HEIGHT - 20);
        }
    }

    private void drawCar(Graphics2D g2d, int x, int y, Color bodyColor, Color glowColor, boolean isPlayer1) {
        // Car shadow
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillRoundRect(x + 5, y + 5, CAR_WIDTH, CAR_HEIGHT, 15, 15);

        // Car glow
        g2d.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 60));
        g2d.fillRoundRect(x - 8, y - 8, CAR_WIDTH + 16, CAR_HEIGHT + 16, 20, 20);

        // Car body
        g2d.setColor(bodyColor);
        g2d.fillRoundRect(x, y, CAR_WIDTH, CAR_HEIGHT, 12, 12);

        // Car body highlight
        g2d.setColor(bodyColor.brighter());
        g2d.fillRoundRect(x + 3, y + 3, CAR_WIDTH - 6, CAR_HEIGHT / 2, 10, 10);

        // Windshield
        g2d.setColor(new Color(100, 200, 255, 200));
        g2d.fillRoundRect(x + 8, y + 12, CAR_WIDTH - 16, 18, 8, 8);

        // Rear window
        g2d.setColor(new Color(50, 150, 255, 150));
        g2d.fillRoundRect(x + 10, y + CAR_HEIGHT - 22, CAR_WIDTH - 20, 12, 6, 6);

        // Headlights
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x + 5, y - 3, 10, 6);
        g2d.fillOval(x + CAR_WIDTH - 15, y - 3, 10, 6);

        // Taillights
        g2d.setColor(Color.RED);
        g2d.fillOval(x + 5, y + CAR_HEIGHT - 3, 10, 6);
        g2d.fillOval(x + CAR_WIDTH - 15, y + CAR_HEIGHT - 3, 10, 6);

        // Racing stripes
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x + CAR_WIDTH / 2 - 3, y + 5, 6, CAR_HEIGHT - 10);

        // Wheels
        g2d.setColor(Color.BLACK);
        g2d.fillRoundRect(x - 5, y + 10, 8, 18, 5, 5);
        g2d.fillRoundRect(x + CAR_WIDTH - 3, y + 10, 8, 18, 5, 5);
        g2d.fillRoundRect(x - 5, y + CAR_HEIGHT - 28, 8, 18, 5, 5);
        g2d.fillRoundRect(x + CAR_WIDTH - 3, y + CAR_HEIGHT - 28, 8, 18, 5, 5);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // Player 1 controls (WASD)
        if (key == KeyEvent.VK_W) up1Pressed = true;
        if (key == KeyEvent.VK_S) down1Pressed = true;
        if (key == KeyEvent.VK_A) left1Pressed = true;
        if (key == KeyEvent.VK_D) right1Pressed = true;

        // Player 2 controls (Arrows)
        if (!vsComputer) {
            if (key == KeyEvent.VK_UP) up2Pressed = true;
            if (key == KeyEvent.VK_DOWN) down2Pressed = true;
            if (key == KeyEvent.VK_LEFT) left2Pressed = true;
            if (key == KeyEvent.VK_RIGHT) right2Pressed = true;
        }

        // Restart
        if (key == KeyEvent.VK_R && gameOver) {
            startGame();
        }

        // Quit
        if (key == KeyEvent.VK_ESCAPE) {
            System.exit(0);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        // Player 1 controls
        if (key == KeyEvent.VK_W) up1Pressed = false;
        if (key == KeyEvent.VK_S) down1Pressed = false;
        if (key == KeyEvent.VK_A) left1Pressed = false;
        if (key == KeyEvent.VK_D) right1Pressed = false;

        // Player 2 controls
        if (key == KeyEvent.VK_UP) up2Pressed = false;
        if (key == KeyEvent.VK_DOWN) down2Pressed = false;
        if (key == KeyEvent.VK_LEFT) left2Pressed = false;
        if (key == KeyEvent.VK_RIGHT) right2Pressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner classes
    static class Obstacle {
        int x, y;
        Color color;

        Obstacle(int x, int y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    static class Particle {
        double x, y, vx, vy;
        Color color;
        int life;
        int size;

        Particle(double x, double y, double vx, double vy, Color color, int life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.life = life;
            this.size = 3 + (int)(Math.random() * 4);
        }
    }

    static class RoadLine {
        double x, y;

        RoadLine(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Super Car Racing Challenge");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            SupercarRacingChallenge game = new SupercarRacingChallenge();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}