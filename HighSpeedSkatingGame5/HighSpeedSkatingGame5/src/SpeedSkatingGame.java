import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class SpeedSkatingGame extends JPanel implements ActionListener, KeyListener {
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int TRACK_WIDTH = 1000;
    private static final int TRACK_HEIGHT = 600;
    private static final int TRACK_X = (WINDOW_WIDTH - TRACK_WIDTH) / 2;
    private static final int TRACK_Y = (WINDOW_HEIGHT - TRACK_HEIGHT) / 2;
    private static final int LANE_WIDTH = 60;
    private static final int NUM_LANES = 8;
    private static final double FINISH_LINE = TRACK_WIDTH - 100;

    private Timer gameTimer;
    private List<Skater> skaters;
    private boolean gameRunning;
    private boolean gameStarted;
    private int countdown;
    private long gameStartTime;
    private Random random;
    private Set<Integer> pressedKeys;
    private ParticleSystem particleSystem;
    private int playerLane;
    private boolean[] keyPressed = new boolean[256];
    private long lastLeftPress = 0;
    private long lastRightPress = 0;
    private int consecutiveAlternations = 0;
    private boolean lastKeyWasLeft = false;
    private GameState gameState;
    private int raceDistance = 1000; // meters
    private double pixelsPerMeter;

    enum GameState {
        MENU, COUNTDOWN, RACING, FINISHED
    }

    public SpeedSkatingGame() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(Color.WHITE);
        setFocusable(true);
        addKeyListener(this);

        random = new Random();
        pressedKeys = new HashSet<>();
        particleSystem = new ParticleSystem();
        gameState = GameState.MENU;
        pixelsPerMeter = (double) TRACK_WIDTH / raceDistance;

        initializeGame();

        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();
    }

    private void initializeGame() {
        skaters = new ArrayList<>();
        playerLane = 0;

        // Create player skater
        skaters.add(new Skater("YOU", Color.BLUE, playerLane, true));

        // Create AI skaters
        String[] aiNames = {"Anna", "Boris", "Chen", "Diana", "Erik", "Fiona", "Gustav"};
        Color[] colors = {Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA,
                Color.CYAN, Color.PINK, Color.YELLOW};

        for (int i = 0; i < Math.min(NUM_LANES - 1, aiNames.length); i++) {
            skaters.add(new Skater(aiNames[i], colors[i], i + 1, false));
        }

        gameRunning = false;
        gameStarted = false;
        countdown = 0;
    }

    public void startRace() {
        gameState = GameState.COUNTDOWN;
        countdown = 180; // 3 seconds at 60fps
        gameStartTime = System.currentTimeMillis();

        for (Skater skater : skaters) {
            skater.reset();
        }
        particleSystem.clear();
        consecutiveAlternations = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (gameState) {
            case MENU:
                drawMenu(g2d);
                break;
            case COUNTDOWN:
            case RACING:
            case FINISHED:
                drawGame(g2d);
                break;
        }
    }

    private void drawMenu(Graphics2D g2d) {
        g2d.setColor(new Color(135, 206, 235)); // Sky blue background
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g2d.getFontMetrics();
        String title = "HIGH-SPEED SPEED SKATING";
        int titleX = (WINDOW_WIDTH - fm.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, 200);

        // Instructions
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        fm = g2d.getFontMetrics();
        String[] instructions = {
                "Alternate LEFT and RIGHT arrow keys as fast as possible!",
                "The faster you alternate, the faster you skate!",
                "Race against 7 AI opponents in a " + raceDistance + "m sprint!",
                "",
                "Press SPACE to start the race",
                "Press R to restart during the race"
        };

        int startY = 350;
        for (String instruction : instructions) {
            int x = (WINDOW_WIDTH - fm.stringWidth(instruction)) / 2;
            g2d.drawString(instruction, x, startY);
            startY += 30;
        }

        // Draw animated skater
        drawAnimatedMenuSkater(g2d);
    }

    private void drawAnimatedMenuSkater(Graphics2D g2d) {
        int centerX = WINDOW_WIDTH / 2;
        int centerY = 600;
        double time = System.currentTimeMillis() * 0.003;
        int bobOffset = (int) (Math.sin(time * 2) * 5);

        // Skater body
        g2d.setColor(Color.BLUE);
        int bodyX = centerX - 15;
        int bodyY = centerY - 30 + bobOffset;
        g2d.fillOval(bodyX, bodyY, 30, 40);

        // Arms (swinging)
        double armSwing = Math.sin(time * 3) * 0.3;
        g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int armY = bodyY + 10;
        g2d.drawLine(bodyX, armY, bodyX - (int)(20 * Math.cos(armSwing)), armY + (int)(15 * Math.sin(armSwing)));
        g2d.drawLine(bodyX + 30, armY, bodyX + 30 + (int)(20 * Math.cos(armSwing)), armY + (int)(15 * Math.sin(armSwing)));

        // Legs (skating motion)
        double legSwing = Math.sin(time * 4) * 0.4;
        int legY = bodyY + 35;
        g2d.drawLine(centerX, legY, centerX - (int)(25 * Math.sin(legSwing)), legY + 20);
        g2d.drawLine(centerX, legY, centerX + (int)(25 * Math.sin(legSwing)), legY + 20);
    }

    private void drawGame(Graphics2D g2d) {
        // Background
        g2d.setColor(new Color(240, 248, 255)); // Alice blue
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        drawTrack(g2d);
        drawSkaters(g2d);
        drawUI(g2d);
        particleSystem.draw(g2d);

        if (gameState == GameState.COUNTDOWN) {
            drawCountdown(g2d);
        } else if (gameState == GameState.FINISHED) {
            drawResults(g2d);
        }
    }

    private void drawTrack(Graphics2D g2d) {
        // Track background
        g2d.setColor(new Color(200, 200, 200));
        g2d.fillRoundRect(TRACK_X, TRACK_Y, TRACK_WIDTH, TRACK_HEIGHT, 20, 20);

        // Lane lines
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        for (int i = 1; i < NUM_LANES; i++) {
            int y = TRACK_Y + i * LANE_WIDTH;
            // Dashed line effect
            for (int x = TRACK_X; x < TRACK_X + TRACK_WIDTH; x += 20) {
                g2d.drawLine(x, y, x + 10, y);
            }
        }

        // Start line
        g2d.setColor(Color.GREEN);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(TRACK_X + 50, TRACK_Y, TRACK_X + 50, TRACK_Y + TRACK_HEIGHT);

        // Finish line
        g2d.setColor(Color.RED);
        int finishX = TRACK_X + (int) FINISH_LINE;
        g2d.drawLine(finishX, TRACK_Y, finishX, TRACK_Y + TRACK_HEIGHT);

        // Distance markers
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        for (int distance = 200; distance < raceDistance; distance += 200) {
            int x = TRACK_X + 50 + (int) (distance * pixelsPerMeter);
            g2d.drawLine(x, TRACK_Y - 5, x, TRACK_Y + 5);
            g2d.drawString(distance + "m", x - 15, TRACK_Y - 10);
        }
    }

    private void drawSkaters(Graphics2D g2d) {
        for (Skater skater : skaters) {
            skater.draw(g2d);
        }
    }

    private void drawUI(Graphics2D g2d) {
        // Speed indicator for player
        Skater player = skaters.get(0);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Speed: " + String.format("%.1f", player.speed * 3.6) + " km/h", 20, 30);
        g2d.drawString("Distance: " + String.format("%.0f", player.distance) + "m", 20, 50);

        // Time
        if (gameStarted && gameState == GameState.RACING) {
            long elapsed = System.currentTimeMillis() - gameStartTime;
            g2d.drawString("Time: " + String.format("%.2f", elapsed / 1000.0) + "s", 20, 70);
        }

        // Leaderboard
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Leaderboard:", WINDOW_WIDTH - 200, 30);

        List<Skater> sortedSkaters = new ArrayList<>(skaters);
        sortedSkaters.sort((a, b) -> Double.compare(b.distance, a.distance));

        for (int i = 0; i < sortedSkaters.size(); i++) {
            Skater skater = sortedSkaters.get(i);
            g2d.setColor(skater.color);
            String position = (i + 1) + ". " + skater.name + " (" + String.format("%.0f", skater.distance) + "m)";
            g2d.drawString(position, WINDOW_WIDTH - 200, 50 + i * 20);
        }

        // Controls hint
        if (gameState == GameState.RACING) {
            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("Alternate ← → keys quickly! Press R to restart", 20, WINDOW_HEIGHT - 20);
        }
    }

    private void drawCountdown(Graphics2D g2d) {
        int seconds = (countdown / 60) + 1;
        if (seconds > 0) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 72));
            FontMetrics fm = g2d.getFontMetrics();
            String countText = String.valueOf(seconds);
            int x = (WINDOW_WIDTH - fm.stringWidth(countText)) / 2;
            int y = WINDOW_HEIGHT / 2 - 100;
            g2d.drawString(countText, x, y);
        } else {
            g2d.setColor(Color.GREEN);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            String goText = "GO!";
            int x = (WINDOW_WIDTH - fm.stringWidth(goText)) / 2;
            int y = WINDOW_HEIGHT / 2 - 100;
            g2d.drawString(goText, x, y);
        }
    }

    private void drawResults(Graphics2D g2d) {
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 128));
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Results panel
        g2d.setColor(Color.WHITE);
        int panelWidth = 400;
        int panelHeight = 300;
        int panelX = (WINDOW_WIDTH - panelWidth) / 2;
        int panelY = (WINDOW_HEIGHT - panelHeight) / 2;
        g2d.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 20, 20);

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 20, 20);

        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics fm = g2d.getFontMetrics();
        String title = "RACE RESULTS";
        int titleX = panelX + (panelWidth - fm.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, panelY + 40);

        // Results
        List<Skater> sortedSkaters = new ArrayList<>(skaters);
        sortedSkaters.sort((a, b) -> {
            if (a.finished && b.finished) {
                return Long.compare(a.finishTime, b.finishTime);
            } else if (a.finished) {
                return -1;
            } else if (b.finished) {
                return 1;
            } else {
                return Double.compare(b.distance, a.distance);
            }
        });

        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        int startY = panelY + 80;
        for (int i = 0; i < Math.min(5, sortedSkaters.size()); i++) {
            Skater skater = sortedSkaters.get(i);
            g2d.setColor(skater.color);
            String result;
            if (skater.finished) {
                double time = skater.finishTime / 1000.0;
                result = (i + 1) + ". " + skater.name + " - " + String.format("%.2f", time) + "s";
            } else {
                result = (i + 1) + ". " + skater.name + " - DNF (" + String.format("%.0f", skater.distance) + "m)";
            }
            g2d.drawString(result, panelX + 20, startY + i * 25);
        }

        // Restart instruction
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        fm = g2d.getFontMetrics();
        String instruction = "Press SPACE for new race, ESC for menu";
        int instX = panelX + (panelWidth - fm.stringWidth(instruction)) / 2;
        g2d.drawString(instruction, instX, panelY + panelHeight - 20);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        repaint();
    }

    private void update() {
        switch (gameState) {
            case COUNTDOWN:
                updateCountdown();
                break;
            case RACING:
                updateRacing();
                break;
        }
        particleSystem.update();
    }

    private void updateCountdown() {
        countdown--;
        if (countdown <= 0) {
            gameState = GameState.RACING;
            gameRunning = true;
            gameStarted = true;
            gameStartTime = System.currentTimeMillis();
        }
    }

    private void updateRacing() {
        boolean anySkaterStillRacing = false;

        for (Skater skater : skaters) {
            if (!skater.finished) {
                skater.update();
                if (skater.distance < raceDistance) {
                    anySkaterStillRacing = true;
                } else if (!skater.finished) {
                    skater.finished = true;
                    skater.finishTime = System.currentTimeMillis() - gameStartTime;
                }
            }
        }

        // Generate particles for fast-moving skaters
        for (Skater skater : skaters) {
            if (skater.speed > 8.0 && random.nextDouble() < 0.3) {
                int x = TRACK_X + 50 + (int) (skater.distance * pixelsPerMeter) - 20;
                int y = TRACK_Y + skater.lane * LANE_WIDTH + LANE_WIDTH / 2;
                particleSystem.addParticle(x, y, skater.color);
            }
        }

        if (!anySkaterStillRacing) {
            gameState = GameState.FINISHED;
            gameRunning = false;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (gameState == GameState.MENU) {
            if (key == KeyEvent.VK_SPACE) {
                startRace();
            }
        } else if (gameState == GameState.RACING) {
            if (key == KeyEvent.VK_R) {
                initializeGame();
                startRace();
            } else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                handlePlayerInput(key);
            }
        } else if (gameState == GameState.FINISHED) {
            if (key == KeyEvent.VK_SPACE) {
                initializeGame();
                startRace();
            } else if (key == KeyEvent.VK_ESCAPE) {
                gameState = GameState.MENU;
                initializeGame();
            }
        }

        keyPressed[key] = true;
    }

    private void handlePlayerInput(int key) {
        long currentTime = System.currentTimeMillis();
        Skater player = skaters.get(0);

        if (key == KeyEvent.VK_LEFT) {
            if (!lastKeyWasLeft && currentTime - lastRightPress < 500) {
                consecutiveAlternations++;
                player.boost(Math.min(consecutiveAlternations * 0.1, 2.0));
            } else {
                consecutiveAlternations = Math.max(0, consecutiveAlternations - 1);
            }
            lastLeftPress = currentTime;
            lastKeyWasLeft = true;
        } else if (key == KeyEvent.VK_RIGHT) {
            if (lastKeyWasLeft && currentTime - lastLeftPress < 500) {
                consecutiveAlternations++;
                player.boost(Math.min(consecutiveAlternations * 0.1, 2.0));
            } else {
                consecutiveAlternations = Math.max(0, consecutiveAlternations - 1);
            }
            lastRightPress = currentTime;
            lastKeyWasLeft = false;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keyPressed[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Skater class
    class Skater {
        String name;
        Color color;
        int lane;
        boolean isPlayer;
        double distance;
        double speed;
        double acceleration;
        boolean finished;
        long finishTime;
        double animationPhase;
        double maxSpeed;
        double baseSpeed;

        public Skater(String name, Color color, int lane, boolean isPlayer) {
            this.name = name;
            this.color = color;
            this.lane = lane;
            this.isPlayer = isPlayer;
            this.maxSpeed = isPlayer ? 15.0 : 10.0 + random.nextDouble() * 4.0;
            this.baseSpeed = isPlayer ? 6.0 : 4.0 + random.nextDouble() * 3.0;
            reset();
        }

        public void reset() {
            distance = 0;
            speed = baseSpeed;
            acceleration = 0;
            finished = false;
            finishTime = 0;
            animationPhase = 0;
        }

        public void boost(double amount) {
            if (isPlayer) {
                speed = Math.min(maxSpeed, speed + amount);
            }
        }

        public void update() {
            if (finished) return;

            if (!isPlayer) {
                // AI behavior - random acceleration with some intelligence
                double targetSpeed = baseSpeed + Math.sin(distance * 0.01) * 2.0 + random.nextGaussian() * 0.5;
                targetSpeed = Math.max(2.0, Math.min(maxSpeed, targetSpeed));

                if (speed < targetSpeed) {
                    acceleration = 0.2;
                } else {
                    acceleration = -0.1;
                }

                // Sprint at the end
                if (distance > raceDistance * 0.8) {
                    acceleration += 0.3;
                }
            } else {
                // Player - gradual deceleration without input
                acceleration = -0.05;
                if (speed < baseSpeed) speed = baseSpeed;
            }

            speed += acceleration;
            speed = Math.max(1.0, Math.min(maxSpeed, speed));

            distance += speed * 0.1; // Convert to reasonable units
            animationPhase += speed * 0.2;
        }

        public void draw(Graphics2D g2d) {
            int x = TRACK_X + 50 + (int) (distance * pixelsPerMeter);
            int y = TRACK_Y + lane * LANE_WIDTH + LANE_WIDTH / 2;

            // Don't draw if off screen
            if (x < TRACK_X - 50 || x > TRACK_X + TRACK_WIDTH + 50) return;

            // Skating animation
            double bobOffset = Math.sin(animationPhase) * 3;
            double armSwing = Math.sin(animationPhase * 1.5) * 0.4;
            double legSwing = Math.sin(animationPhase * 2) * 0.3;

            // Body
            g2d.setColor(color);
            int bodyWidth = 20;
            int bodyHeight = 30;
            g2d.fillOval(x - bodyWidth/2, (int)(y - bodyHeight/2 + bobOffset), bodyWidth, bodyHeight);

            // Arms
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int armY = (int)(y + bobOffset);
            g2d.drawLine(x - bodyWidth/2, armY,
                    x - bodyWidth/2 - (int)(15 * Math.cos(armSwing)),
                    armY + (int)(10 * Math.sin(armSwing)));
            g2d.drawLine(x + bodyWidth/2, armY,
                    x + bodyWidth/2 + (int)(15 * Math.cos(armSwing)),
                    armY + (int)(10 * Math.sin(armSwing)));

            // Legs
            int legY = (int)(y + bodyHeight/2 + bobOffset);
            g2d.drawLine(x, legY, x - (int)(20 * Math.sin(legSwing)), legY + 15);
            g2d.drawLine(x, legY, x + (int)(20 * Math.sin(legSwing)), legY + 15);

            // Name
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(name, x - fm.stringWidth(name)/2, y - 25);

            // Speed indicator
            if (speed > maxSpeed * 0.8) {
                g2d.setColor(Color.YELLOW);
                for (int i = 0; i < 3; i++) {
                    g2d.drawLine(x - 25 - i*3, y + random.nextInt(10) - 5,
                            x - 30 - i*3, y + random.nextInt(10) - 5);
                }
            }
        }
    }

    // Particle system for visual effects
    class ParticleSystem {
        private List<Particle> particles;

        public ParticleSystem() {
            particles = new ArrayList<>();
        }

        public void addParticle(int x, int y, Color color) {
            particles.add(new Particle(x, y, color));
        }

        public void update() {
            particles.removeIf(p -> p.life <= 0);
            for (Particle p : particles) {
                p.update();
            }
        }

        public void draw(Graphics2D g2d) {
            for (Particle p : particles) {
                p.draw(g2d);
            }
        }

        public void clear() {
            particles.clear();
        }
    }

    class Particle {
        double x, y, vx, vy;
        Color color;
        int life;
        int maxLife;

        public Particle(int x, int y, Color color) {
            this.x = x;
            this.y = y;
            this.vx = (random.nextDouble() - 0.5) * 4;
            this.vy = (random.nextDouble() - 0.5) * 4;
            this.color = color;
            this.maxLife = 30 + random.nextInt(20);
            this.life = maxLife;
        }

        public void update() {
            x += vx;
            y += vy;
            vx *= 0.98;
            vy *= 0.98;
            life--;
        }

        public void draw(Graphics2D g2d) {
            float alpha = (float) life / maxLife;
            Color fadeColor = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int)(alpha * 128));
            g2d.setColor(fadeColor);
            int size = (int)(alpha * 6) + 2;
            g2d.fillOval((int)x - size/2, (int)y - size/2, size, size);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("High-Speed Speed Skating");
            SpeedSkatingGame game = new SpeedSkatingGame();

            frame.add(game);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}