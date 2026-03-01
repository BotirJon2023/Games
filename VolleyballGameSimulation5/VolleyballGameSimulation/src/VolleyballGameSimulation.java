import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class VolleyballGameSimulation extends JPanel implements ActionListener, KeyListener {
    // Game state
    private enum GameState {
        MENU, PLAYING, PAUSED, GAME_OVER
    }

    private GameState currentState = GameState.MENU;
    private Timer timer;
    private Random random = new Random();

    // Court dimensions
    private final int COURT_WIDTH = 800;
    private final int COURT_HEIGHT = 500;
    private final int NET_HEIGHT = 150;
    private final int NET_Y = 200;

    // Ball properties
    private Ball ball;

    // Players
    private ArrayList<Player> team1Players;
    private ArrayList<Player> team2Players;

    // Scores
    private int team1Score = 0;
    private int team2Score = 0;
    private final int WINNING_SCORE = 25;

    // Game mechanics
    private boolean isRallyActive = false;
    private int rallyCount = 0;
    private String lastHitBy = "";
    private double windSpeed = 0;
    private double windDirection = 0;

    // Animation effects
    private ArrayList<Particle> particles;
    private boolean showTrajectory = false;
    private ArrayList<Point2D.Double> trajectoryPoints;

    // UI Components
    private JButton menuButton;
    private JButton pauseButton;
    private JButton resetButton;

    public VolleyballGameSimulation() {
        setPreferredSize(new Dimension(COURT_WIDTH, COURT_HEIGHT + 100));
        setBackground(new Color(135, 206, 235)); // Sky blue
        setLayout(null);

        // Initialize game objects
        initGame();

        // Setup timer for animation (60 FPS)
        timer = new Timer(16, this);
        timer.start();

        // Setup UI buttons
        setupButtons();

        // Add key listener
        setFocusable(true);
        addKeyListener(this);

        // Initialize particles array
        particles = new ArrayList<>();
        trajectoryPoints = new ArrayList<>();

        // Random wind effects
        updateWind();
    }

    private void initGame() {
        // Create ball
        ball = new Ball(COURT_WIDTH / 2, COURT_HEIGHT / 2 - 50);

        // Create teams
        team1Players = new ArrayList<>();
        team2Players = new ArrayList<>();

        // Team 1 (Left side - Blue)
        int[] team1X = {150, 100, 200, 120, 180};
        int[] team1Y = {300, 250, 250, 350, 350};
        String[] team1Names = {"Setter", "Spiker", "Libero", "Blocker", "Server"};

        for (int i = 0; i < 5; i++) {
            team1Players.add(new Player(team1X[i], team1Y[i], team1Names[i], 1, Color.BLUE));
        }

        // Team 2 (Right side - Red)
        int[] team2X = {650, 600, 700, 620, 680};
        int[] team2Y = {300, 250, 250, 350, 350};
        String[] team2Names = {"Setter", "Spiker", "Libero", "Blocker", "Server"};

        for (int i = 0; i < 5; i++) {
            team2Players.add(new Player(team2X[i], team2Y[i], team2Names[i], 2, Color.RED));
        }
    }

    private void setupButtons() {
        menuButton = createButton("Menu", 10, COURT_HEIGHT + 10);
        pauseButton = createButton("Pause", 90, COURT_HEIGHT + 10);
        resetButton = createButton("Reset", 170, COURT_HEIGHT + 10);

        menuButton.addActionListener(e -> currentState = GameState.MENU);
        pauseButton.addActionListener(e -> {
            if (currentState == GameState.PLAYING) {
                currentState = GameState.PAUSED;
                pauseButton.setText("Resume");
            } else if (currentState == GameState.PAUSED) {
                currentState = GameState.PLAYING;
                pauseButton.setText("Pause");
            }
        });
        resetButton.addActionListener(e -> resetGame());
    }

    private JButton createButton(String text, int x, int y) {
        JButton button = new JButton(text);
        button.setBounds(x, y, 70, 30);
        button.setFocusable(false);
        add(button);
        return button;
    }

    private void resetGame() {
        team1Score = 0;
        team2Score = 0;
        ball.reset(COURT_WIDTH / 2, COURT_HEIGHT / 2 - 50);
        isRallyActive = false;
        rallyCount = 0;
        lastHitBy = "";
        particles.clear();
        trajectoryPoints.clear();
        currentState = GameState.MENU;
        pauseButton.setText("Pause");
    }

    private void updateWind() {
        // Random wind changes
        if (random.nextInt(100) < 5) {
            windSpeed = random.nextDouble() * 3 - 1.5;
            windDirection = random.nextDouble() * Math.PI * 2;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw court
        drawCourt(g2d);

        // Draw net
        drawNet(g2d);

        // Draw players
        drawPlayers(g2d);

        // Draw ball
        ball.draw(g2d);

        // Draw particles (dust, effects)
        drawParticles(g2d);

        // Draw trajectory if enabled
        if (showTrajectory && currentState == GameState.PLAYING) {
            drawTrajectory(g2d);
        }

        // Draw UI elements
        drawUI(g2d);

        // Draw game state overlays
        drawStateOverlay(g2d);
    }

    private void drawCourt(Graphics2D g2d) {
        // Court floor
        g2d.setColor(new Color(222, 184, 135)); // Sandy color
        g2d.fillRect(50, 150, COURT_WIDTH - 100, 300);

        // Court lines
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));

        // Boundary lines
        g2d.drawRect(50, 150, COURT_WIDTH - 100, 300);

        // Center line
        g2d.drawLine(COURT_WIDTH / 2, 150, COURT_WIDTH / 2, 450);

        // Attack lines
        g2d.drawLine(200, 150, 200, 450);
        g2d.drawLine(600, 150, 600, 450);

        // Service areas
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g2d.drawLine(100, 450, 400, 450);
        g2d.drawLine(400, 450, 700, 450);
    }

    private void drawNet(Graphics2D g2d) {
        // Net posts
        g2d.setColor(Color.GRAY);
        g2d.fillRect(COURT_WIDTH / 2 - 10, 150, 5, 300);
        g2d.fillRect(COURT_WIDTH / 2 + 5, 150, 5, 300);

        // Net
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1));

        // Draw net mesh
        for (int i = 0; i < 10; i++) {
            int y = 180 + i * 25;
            g2d.drawLine(COURT_WIDTH / 2 - 8, y, COURT_WIDTH / 2 + 8, y);
        }

        for (int i = 0; i < 5; i++) {
            int x1 = COURT_WIDTH / 2 - 8 + i * 4;
            int x2 = COURT_WIDTH / 2 - 8 + i * 4;
            g2d.drawLine(x1, 160, x2, 420);
        }

        // Top of net
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(COURT_WIDTH / 2 - 12, 160, COURT_WIDTH / 2 + 12, 160);
    }

    private void drawPlayers(Graphics2D g2d) {
        for (Player player : team1Players) {
            player.draw(g2d);
        }
        for (Player player : team2Players) {
            player.draw(g2d);
        }
    }

    private void drawParticles(Graphics2D g2d) {
        synchronized (particles) {
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.update();
                p.draw(g2d);
                if (p.isDead()) {
                    particles.remove(i);
                }
            }
        }
    }

    private void drawTrajectory(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));

        for (int i = 0; i < trajectoryPoints.size() - 1; i++) {
            Point2D.Double p1 = trajectoryPoints.get(i);
            Point2D.Double p2 = trajectoryPoints.get(i + 1);
            g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
        }
    }

    private void drawUI(Graphics2D g2d) {
        // Score board
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(300, 20, 200, 50, 10, 10);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString(String.format("%02d : %02d", team1Score, team2Score), 350, 55);

        // Rally count
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Rally: " + rallyCount, 350, 80);

        // Last hit indicator
        if (!lastHitBy.isEmpty()) {
            g2d.drawString("Last hit: " + lastHitBy, 350, 100);
        }

        // Wind indicator
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.fillRect(10, 10, 200, 30);
        g2d.setColor(Color.BLACK);
        g2d.drawString(String.format("Wind: %.1f mph", windSpeed), 15, 30);

        // Draw wind direction arrow
        int arrowX = 180;
        int arrowY = 25;
        int arrowLength = 20;
        double arrowAngle = windDirection;

        int endX = arrowX + (int) (arrowLength * Math.cos(arrowAngle));
        int endY = arrowY + (int) (arrowLength * Math.sin(arrowAngle));

        g2d.drawLine(arrowX, arrowY, endX, endY);
        g2d.fillOval(endX - 3, endY - 3, 6, 6);
    }

    private void drawStateOverlay(Graphics2D g2d) {
        if (currentState == GameState.MENU) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.drawString("VOLLEYBALL", 250, 200);

            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            g2d.drawString("Press SPACE to Start", 280, 300);
            g2d.drawString("Press T for Trajectory", 280, 350);
            g2d.drawString("Press W for Wind Effect", 280, 400);
            g2d.drawString("First to 25 points wins", 280, 450);

        } else if (currentState == GameState.PAUSED) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.drawString("PAUSED", 300, 250);

        } else if (currentState == GameState.GAME_OVER) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));

            if (team1Score >= WINNING_SCORE) {
                g2d.drawString("TEAM BLUE WINS!", 200, 250);
            } else {
                g2d.drawString("TEAM RED WINS!", 200, 250);
            }

            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            g2d.drawString("Press R to Restart", 300, 350);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentState == GameState.PLAYING) {
            updateGame();
        }

        // Update wind occasionally
        updateWind();

        // Repaint
        repaint();
    }

    private void updateGame() {
        // Update ball position
        ball.update();

        // Apply wind effect
        ball.applyWind(windSpeed, windDirection);

        // Check ball collisions with boundaries
        checkBoundaryCollisions();

        // Check for scoring
        checkScoring();

        // Update players AI
        updatePlayersAI();

        // Check for player-ball collisions
        checkPlayerCollisions();

        // Update trajectory points
        if (showTrajectory) {
            updateTrajectory();
        }

        // Add random particles
        if (random.nextInt(10) == 0) {
            particles.add(new Particle(
                    ball.x + random.nextInt(20) - 10,
                    ball.y + random.nextInt(20) - 10
            ));
        }
    }

    private void checkBoundaryCollisions() {
        // Floor boundaries
        if (ball.y > 440) {
            ball.y = 440;
            ball.vy = -ball.vy * 0.5;
            createImpactParticles(ball.x, 440);
        }

        // Side boundaries
        if (ball.x < 60 || ball.x > 740) {
            ball.vx = -ball.vx * 0.7;
            ball.x = ball.x < 60 ? 60 : 740;
            createImpactParticles(ball.x, ball.y);
        }

        // Ceiling
        if (ball.y < 100) {
            ball.y = 100;
            ball.vy = -ball.vy * 0.5;
            createImpactParticles(ball.x, 100);
        }

        // Net collision
        if (ball.x > COURT_WIDTH / 2 - 15 && ball.x < COURT_WIDTH / 2 + 15) {
            if (ball.y > NET_Y - 20 && ball.y < NET_Y + NET_HEIGHT) {
                ball.vx = -ball.vx * 0.3;
                ball.x = ball.x < COURT_WIDTH / 2 ? COURT_WIDTH / 2 - 15 : COURT_WIDTH / 2 + 15;
                createImpactParticles(ball.x, ball.y);
            }
        }
    }

    private void checkScoring() {
        // Check if ball hits ground on opponent's side
        if (ball.y > 440) {
            if (ball.x < COURT_WIDTH / 2) {
                // Ball on left side - Team 2 scores
                if (lastHitBy.startsWith("Team 1") || lastHitBy.isEmpty()) {
                    team2Score++;
                    createScoreEffect(650, 100, "Team 2 scores!");
                }
            } else {
                // Ball on right side - Team 1 scores
                if (lastHitBy.startsWith("Team 2") || lastHitBy.isEmpty()) {
                    team1Score++;
                    createScoreEffect(150, 100, "Team 1 scores!");
                }
            }

            // Reset ball and rally
            ball.reset(COURT_WIDTH / 2, 200);
            isRallyActive = false;
            rallyCount = 0;
            lastHitBy = "";

            // Check for game over
            if (team1Score >= WINNING_SCORE || team2Score >= WINNING_SCORE) {
                currentState = GameState.GAME_OVER;
            }
        }
    }

    private void checkPlayerCollisions() {
        // Team 1 players hitting the ball
        for (Player player : team1Players) {
            if (player.canHitBall(ball)) {
                if (ball.x < COURT_WIDTH / 2) { // Can only hit on their side
                    double hitAngle = player.hitBall(ball);
                    ball.vx = Math.abs(ball.vx) * 0.8 + 5; // Hit towards opponent
                    ball.vy = -Math.abs(ball.vy) * 1.2 - 3;
                    rallyCount++;
                    lastHitBy = player.name + " (Team 1)";
                    createHitEffect(ball.x, ball.y);
                    player.setServing(false);
                }
            }
        }

        // Team 2 players hitting the ball
        for (Player player : team2Players) {
            if (player.canHitBall(ball)) {
                if (ball.x > COURT_WIDTH / 2) { // Can only hit on their side
                    double hitAngle = player.hitBall(ball);
                    ball.vx = -Math.abs(ball.vx) * 0.8 - 5; // Hit towards opponent
                    ball.vy = -Math.abs(ball.vy) * 1.2 - 3;
                    rallyCount++;
                    lastHitBy = player.name + " (Team 2)";
                    createHitEffect(ball.x, ball.y);
                    player.setServing(false);
                }
            }
        }
    }

    private void updatePlayersAI() {
        // Simple AI movement
        for (Player player : team1Players) {
            player.updateAI(ball, true);
        }

        for (Player player : team2Players) {
            player.updateAI(ball, false);
        }
    }

    private void updateTrajectory() {
        trajectoryPoints.clear();

        // Predict ball trajectory for next 30 frames
        Ball tempBall = new Ball(ball.x, ball.y);
        tempBall.vx = ball.vx;
        tempBall.vy = ball.vy;

        for (int i = 0; i < 30; i++) {
            tempBall.update();
            tempBall.applyWind(windSpeed, windDirection);
            trajectoryPoints.add(new Point2D.Double(tempBall.x, tempBall.y));

            // Stop if ball hits ground
            if (tempBall.y > 440) break;
        }
    }

    private void createImpactParticles(double x, double y) {
        for (int i = 0; i < 10; i++) {
            particles.add(new Particle(x + random.nextInt(20) - 10, y));
        }
    }

    private void createHitEffect(double x, double y) {
        for (int i = 0; i < 15; i++) {
            Particle p = new Particle(x, y);
            p.vx = random.nextDouble() * 8 - 4;
            p.vy = random.nextDouble() * 8 - 4;
            p.color = Color.YELLOW;
            particles.add(p);
        }
    }

    private void createScoreEffect(double x, double y, String message) {
        for (int i = 0; i < 20; i++) {
            Particle p = new Particle(x + random.nextInt(100) - 50, y);
            p.vx = random.nextDouble() * 10 - 5;
            p.vy = random.nextDouble() * 10 - 5;
            p.color = new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255));
            p.size = random.nextInt(8) + 5;
            particles.add(p);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE:
                if (currentState == GameState.MENU) {
                    currentState = GameState.PLAYING;
                } else if (currentState == GameState.PLAYING) {
                    // Serve the ball
                    if (!isRallyActive) {
                        ball.vx = random.nextDouble() * 4 - 2;
                        ball.vy = -8;
                        isRallyActive = true;

                        // Random server
                        if (random.nextBoolean()) {
                            lastHitBy = "Server (Team 1)";
                            for (Player p : team1Players) {
                                if (p.name.equals("Server")) p.setServing(true);
                            }
                        } else {
                            lastHitBy = "Server (Team 2)";
                            for (Player p : team2Players) {
                                if (p.name.equals("Server")) p.setServing(true);
                            }
                        }
                    }
                }
                break;

            case KeyEvent.VK_T:
                showTrajectory = !showTrajectory;
                break;

            case KeyEvent.VK_W:
                windSpeed = random.nextDouble() * 5;
                windDirection = random.nextDouble() * Math.PI * 2;
                break;

            case KeyEvent.VK_R:
                if (currentState == GameState.GAME_OVER) {
                    resetGame();
                }
                break;

            case KeyEvent.VK_P:
                if (currentState == GameState.PLAYING) {
                    currentState = GameState.PAUSED;
                    pauseButton.setText("Resume");
                } else if (currentState == GameState.PAUSED) {
                    currentState = GameState.PLAYING;
                    pauseButton.setText("Pause");
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner class for Ball
    class Ball {
        double x, y;
        double vx, vy;
        double initialX, initialY;
        final double GRAVITY = 0.3;
        final double BOUNCE_FACTOR = 0.7;

        public Ball(double x, double y) {
            this.x = x;
            this.y = y;
            this.initialX = x;
            this.initialY = y;
            this.vx = 0;
            this.vy = 0;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += GRAVITY;

            // Air resistance
            vx *= 0.99;
            vy *= 0.99;
        }

        public void applyWind(double speed, double direction) {
            vx += speed * Math.cos(direction) * 0.1;
            vy += speed * Math.sin(direction) * 0.05;
        }

        public void reset(double x, double y) {
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = 0;
        }

        public void draw(Graphics2D g2d) {
            // Draw ball shadow
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillOval((int) x - 8, (int) y + 5, 16, 8);

            // Draw ball
            g2d.setColor(Color.YELLOW);
            g2d.fillOval((int) x - 10, (int) y - 10, 20, 20);

            // Ball details
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawOval((int) x - 10, (int) y - 10, 20, 20);

            // Ball stripes
            g2d.drawArc((int) x - 12, (int) y - 12, 24, 24, 45, 90);
            g2d.drawArc((int) x - 12, (int) y - 12, 24, 24, 135, 90);
        }
    }

    // Inner class for Player
    class Player {
        double x, y;
        String name;
        int team;
        Color color;
        double targetX, targetY;
        double speed = 2;
        boolean isServing = false;

        public Player(double x, double y, String name, int team, Color color) {
            this.x = x;
            this.y = y;
            this.name = name;
            this.team = team;
            this.color = color;
            this.targetX = x;
            this.targetY = y;
        }

        public void updateAI(Ball ball, boolean isLeftTeam) {
            // Simple AI: move towards ball if on correct side
            if (isLeftTeam && ball.x < 400 || !isLeftTeam && ball.x > 400) {
                targetX = ball.x;
                targetY = ball.y - 30;
            }

            // Move towards target
            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > 5) {
                x += (dx / distance) * speed;
                y += (dy / distance) * speed;
            }

            // Stay in bounds
            x = Math.max(60, Math.min(740, x));
            y = Math.max(180, Math.min(420, y));
        }

        public boolean canHitBall(Ball ball) {
            double distance = Math.sqrt(Math.pow(x - ball.x, 2) + Math.pow(y - ball.y, 2));
            return distance < 40 && ball.y < y + 30;
        }

        public double hitBall(Ball ball) {
            // Calculate hit angle based on player position
            return Math.atan2(ball.y - y, ball.x - x);
        }

        public void setServing(boolean serving) {
            this.isServing = serving;
        }

        public void draw(Graphics2D g2d) {
            // Draw player shadow
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillOval((int) x - 15, (int) y + 15, 30, 10);

            // Draw player body
            g2d.setColor(color);
            g2d.fillOval((int) x - 15, (int) y - 20, 30, 40);

            // Draw player head
            g2d.setColor(color.darker());
            g2d.fillOval((int) x - 10, (int) y - 35, 20, 20);

            // Draw jersey number/name
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(name, (int) x - 15, (int) y - 5);

            // Serving indicator
            if (isServing) {
                g2d.setColor(Color.GREEN);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval((int) x - 20, (int) y - 40, 40, 50);
            }

            // Draw player arms (for spiking)
            if (name.equals("Spiker") && ball.y < y) {
                g2d.setColor(color);
                g2d.fillRect((int) x + 10, (int) y - 25, 15, 5);
            }
        }
    }

    // Inner class for Particle effects
    class Particle {
        double x, y;
        double vx, vy;
        int life;
        int maxLife;
        Color color;
        int size;

        public Particle(double x, double y) {
            this.x = x;
            this.y = y;
            this.vx = random.nextDouble() * 4 - 2;
            this.vy = random.nextDouble() * 4 - 2;
            this.life = 50;
            this.maxLife = 50;
            this.color = new Color(255, 255, 255, 255);
            this.size = random.nextInt(5) + 3;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 0.1; // Gravity
            life--;
        }

        public void draw(Graphics2D g2d) {
            int alpha = (int) (255 * ((double) life / maxLife));
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g2d.fillOval((int) x - size/2, (int) y - size/2, size, size);
        }

        public boolean isDead() {
            return life <= 0;
        }
    }

    // Main method to run the simulation
    public static void main(String[] args) {
        JFrame frame = new JFrame("Volleyball Game Simulation");
        VolleyballGameSimulation game = new VolleyballGameSimulation();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}