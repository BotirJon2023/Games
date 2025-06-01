import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class UltimateFrisbeeGame extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PLAYER_SIZE = 30;
    private static final int DISC_SIZE = 20;
    private static final int END_ZONE_WIDTH = 80;
    private static final int GROUND_HEIGHT = 50;
    private static final int PLAYER_SPEED = 5;
    private static final int JUMP_HEIGHT = 15;
    private static final int GRAVITY = 1;
    private static final int FRAME_DELAY = 30;

    // Game objects
    private Player player1;
    private Player player2;
    private Player[] team1;
    private Player[] team2;
    private Disc disc;
    private boolean[] keys;
    private int scoreTeam1 = 0;
    private int scoreTeam2 = 0;
    private boolean gameActive = true;
    private Random random = new Random();

    // Animation variables
    private ArrayList<Particle> particles = new ArrayList<>();
    private int windForce = 0;
    private int windCounter = 0;

    public UltimateFrisbeeGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(100, 200, 100));
        keys = new boolean[256];

        // Initialize players
        player1 = new Player(WIDTH / 4, HEIGHT - GROUND_HEIGHT - PLAYER_SIZE, Color.BLUE, 1);
        player2 = new Player(3 * WIDTH / 4, HEIGHT - GROUND_HEIGHT - PLAYER_SIZE, Color.RED, 2);

        // Initialize teams (7 players per team)
        team1 = new Player[7];
        team2 = new Player[7];
        initializeTeams();

        // Initialize disc
        disc = new Disc(WIDTH / 2, HEIGHT / 2);

        // Set up game loop
        Timer timer = new Timer(FRAME_DELAY, this);
        timer.start();

        addKeyListener(this);
        setFocusable(true);
    }

    private void initializeTeams() {
        // Team 1 (offense)
        for (int i = 0; i < team1.length; i++) {
            int x = WIDTH / 4 + random.nextInt(100) - 50;
            int y = HEIGHT - GROUND_HEIGHT - PLAYER_SIZE - random.nextInt(50);
            team1[i] = new Player(x, y, new Color(0, 0, 200 + random.nextInt(55)), 1);
        }
        team1[0] = player1; // Player 1 is part of team 1

        // Team 2 (defense)
        for (int i = 0; i < team2.length; i++) {
            int x = 3 * WIDTH / 4 + random.nextInt(100) - 50;
            int y = HEIGHT - GROUND_HEIGHT - PLAYER_SIZE - random.nextInt(50);
            team2[i] = new Player(x, y, new Color(200 + random.nextInt(55), 0, 0), 2);
        }
        team2[0] = player2; // Player 2 is part of team 2
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw field
        drawField(g);

        // Draw players
        for (Player p : team1) p.draw(g);
        for (Player p : team2) p.draw(g);

        // Draw disc
        disc.draw(g);

        // Draw particles
        for (Particle particle : particles) {
            particle.draw(g);
        }

        // Draw score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Team 1: " + scoreTeam1, 20, 30);
        g.drawString("Team 2: " + scoreTeam2, WIDTH - 150, 30);

        // Draw wind indicator
        drawWindIndicator(g);

        if (!gameActive) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String message = scoreTeam1 > scoreTeam2 ? "Team 1 Wins!" : "Team 2 Wins!";
            if (scoreTeam1 == scoreTeam2) message = "Tie Game!";
            g.drawString(message, WIDTH/2 - 150, HEIGHT/2);
        }
    }

    private void drawField(Graphics g) {
        // Draw grass
        g.setColor(new Color(100, 200, 100));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw ground
        g.setColor(new Color(150, 100, 50));
        g.fillRect(0, HEIGHT - GROUND_HEIGHT, WIDTH, GROUND_HEIGHT);

        // Draw end zones
        g.setColor(new Color(200, 200, 255));
        g.fillRect(0, 0, END_ZONE_WIDTH, HEIGHT - GROUND_HEIGHT);
        g.fillRect(WIDTH - END_ZONE_WIDTH, 0, END_ZONE_WIDTH, HEIGHT - GROUND_HEIGHT);

        // Draw field markings
        g.setColor(Color.WHITE);
        // Center line
        g.drawLine(WIDTH / 2, 0, WIDTH / 2, HEIGHT - GROUND_HEIGHT);
        // Brick marks (every 10 yards)
        for (int i = 1; i < 7; i++) {
            int x = END_ZONE_WIDTH + i * (WIDTH - 2 * END_ZONE_WIDTH) / 7;
            g.drawLine(x, HEIGHT - GROUND_HEIGHT - 10, x, HEIGHT - GROUND_HEIGHT);
        }
    }

    private void drawWindIndicator(Graphics g) {
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("Wind: " + windForce, WIDTH / 2 - 30, 30);

        int arrowSize = Math.abs(windForce) * 2;
        if (windForce > 0) {
            // Right arrow
            g.fillPolygon(new int[] {WIDTH/2 + 50, WIDTH/2 + 50 + arrowSize, WIDTH/2 + 50 + arrowSize},
                    new int[] {20, 20 - arrowSize/2, 20 + arrowSize/2}, 3);
        } else if (windForce < 0) {
            // Left arrow
            g.fillPolygon(new int[] {WIDTH/2 - 50, WIDTH/2 - 50 - arrowSize, WIDTH/2 - 50 - arrowSize},
                    new int[] {20, 20 - arrowSize/2, 20 + arrowSize/2}, 3);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameActive) return;

        // Handle player input
        handleInput();

        // Update wind
        updateWind();

        // Update players
        for (Player p : team1) p.update();
        for (Player p : team2) p.update();

        // Simple AI for computer players
        updateAI();

        // Update disc
        disc.update();

        // Check collisions
        checkCollisions();

        // Check scoring
        checkScoring();

        // Update particles
        updateParticles();

        repaint();
    }

    private void handleInput() {
        // Player 1 controls (WASD + Space)
        if (keys[KeyEvent.VK_A]) player1.move(-PLAYER_SPEED, 0);
        if (keys[KeyEvent.VK_D]) player1.move(PLAYER_SPEED, 0);
        if (keys[KeyEvent.VK_W] && player1.isOnGround()) player1.jump();
        if (keys[KeyEvent.VK_SPACE] && player1.hasDisc) {
            // Calculate throw direction based on mouse position
            Point mousePos = getMousePosition();
            if (mousePos != null) {
                int dx = mousePos.x - player1.x;
                int dy = mousePos.y - player1.y;
                disc.throwDisc(dx, dy, 15 + random.nextInt(5));
                player1.hasDisc = false;
                createThrowParticles(player1.x, player1.y);
            }
        }

        // Player 2 controls (Arrow keys + Enter)
        if (keys[KeyEvent.VK_LEFT]) player2.move(-PLAYER_SPEED, 0);
        if (keys[KeyEvent.VK_RIGHT]) player2.move(PLAYER_SPEED, 0);
        if (keys[KeyEvent.VK_UP] && player2.isOnGround()) player2.jump();
        if (keys[KeyEvent.VK_ENTER] && player2.hasDisc) {
            // Calculate throw direction based on mouse position
            Point mousePos = getMousePosition();
            if (mousePos != null) {
                int dx = mousePos.x - player2.x;
                int dy = mousePos.y - player2.y;
                disc.throwDisc(dx, dy, 15 + random.nextInt(5));
                player2.hasDisc = false;
                createThrowParticles(player2.x, player2.y);
            }
        }
    }

    private void updateAI() {
        // Simple AI for computer players
        for (Player p : team1) {
            if (p == player1) continue; // Skip human player

            if (!disc.inMotion && random.nextInt(100) < 2) {
                // Occasionally move toward disc
                int dx = disc.x - p.x;
                int dy = disc.y - p.y;
                p.move(dx > 0 ? PLAYER_SPEED/2 : -PLAYER_SPEED/2, 0);

                if (Math.abs(dx) < PLAYER_SIZE && Math.abs(dy) < PLAYER_SIZE && p.isOnGround()) {
                    p.jump();
                }
            }
        }

        for (Player p : team2) {
            if (p == player2) continue; // Skip human player

            if (!disc.inMotion && random.nextInt(100) < 2) {
                // Occasionally move toward disc
                int dx = disc.x - p.x;
                int dy = disc.y - p.y;
                p.move(dx > 0 ? PLAYER_SPEED/2 : -PLAYER_SPEED/2, 0);

                if (Math.abs(dx) < PLAYER_SIZE && Math.abs(dy) < PLAYER_SIZE && p.isOnGround()) {
                    p.jump();
                }
            }
        }
    }

    private void updateWind() {
        windCounter++;
        if (windCounter > 100) {
            windForce = random.nextInt(7) - 3; // -3 to +3
            windCounter = 0;
        }

        // Apply wind to disc if it's in motion
        if (disc.inMotion) {
            disc.dx += windForce * 0.1;
        }
    }

    private void checkCollisions() {
        // Check disc-player collisions
        for (Player p : team1) {
            if (disc.collidesWith(p) && !p.hasDisc && !disc.inMotion) {
                p.hasDisc = true;
                disc.inMotion = false;
                disc.x = p.x;
                disc.y = p.y - PLAYER_SIZE/2;
                createCatchParticles(p.x, p.y);
            }
        }

        for (Player p : team2) {
            if (disc.collidesWith(p) && !p.hasDisc && !disc.inMotion) {
                p.hasDisc = true;
                disc.inMotion = false;
                disc.x = p.x;
                disc.y = p.y - PLAYER_SIZE/2;
                createCatchParticles(p.x, p.y);
            }
        }

        // Check disc-ground collision
        if (disc.y + DISC_SIZE > HEIGHT - GROUND_HEIGHT) {
            disc.y = HEIGHT - GROUND_HEIGHT - DISC_SIZE;
            disc.dy = -disc.dy * 0.5; // Bounce with energy loss
            disc.dx *= 0.8; // Friction
            if (Math.abs(disc.dy) < 1) {
                disc.inMotion = false;
                disc.dy = 0;
            }
            createGroundParticles(disc.x, disc.y);
        }
    }

    private void checkScoring() {
        // Team 1 scores in right end zone
        if (disc.x + DISC_SIZE > WIDTH - END_ZONE_WIDTH && !disc.inMotion) {
            for (Player p : team1) {
                if (p.hasDisc) {
                    scoreTeam1++;
                    resetAfterScore();
                    break;
                }
            }
        }

        // Team 2 scores in left end zone
        if (disc.x < END_ZONE_WIDTH && !disc.inMotion) {
            for (Player p : team2) {
                if (p.hasDisc) {
                    scoreTeam2++;
                    resetAfterScore();
                    break;
                }
            }
        }

        // Game ends at 7 points
        if (scoreTeam1 >= 7 || scoreTeam2 >= 7) {
            gameActive = false;
        }
    }

    private void resetAfterScore() {
        // Reset all players
        initializeTeams();

        // Reset disc to center
        disc = new Disc(WIDTH / 2, HEIGHT / 2);

        // Reset wind
        windForce = 0;
        windCounter = 0;
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

    private void createThrowParticles(int x, int y) {
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(x, y,
                    random.nextDouble() * 4 - 2,
                    random.nextDouble() * 4 - 2,
                    new Color(255, 255, 200), 30));
        }
    }

    private void createCatchParticles(int x, int y) {
        for (int i = 0; i < 30; i++) {
            particles.add(new Particle(x, y - PLAYER_SIZE/2,
                    random.nextDouble() * 6 - 3,
                    random.nextDouble() * 6 - 3,
                    new Color(255, 255, 255), 40));
        }
    }

    private void createGroundParticles(int x, int y) {
        for (int i = 0; i < 15; i++) {
            particles.add(new Particle(x, y + DISC_SIZE,
                    random.nextDouble() * 4 - 2,
                    random.nextDouble() * -3,
                    new Color(150, 100, 50), 50));
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Player class
    class Player {
        int x, y;
        int dy;
        Color color;
        int team;
        boolean hasDisc = false;

        Player(int x, int y, Color color, int team) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.team = team;
        }

        void move(int dx, int dy) {
            x += dx;
            y += dy;

            // Boundary checking
            if (x < PLAYER_SIZE/2) x = PLAYER_SIZE/2;
            if (x > WIDTH - PLAYER_SIZE/2) x = WIDTH - PLAYER_SIZE/2;
            if (y < PLAYER_SIZE/2) y = PLAYER_SIZE/2;
            if (y > HEIGHT - GROUND_HEIGHT - PLAYER_SIZE/2) {
                y = HEIGHT - GROUND_HEIGHT - PLAYER_SIZE/2;
                this.dy = 0;
            }
        }

        void jump() {
            if (isOnGround()) {
                dy = -JUMP_HEIGHT;
            }
        }

        void update() {
            // Apply gravity
            dy += GRAVITY;
            move(0, dy);

            // Simple friction when on ground
            if (isOnGround()) {
                dy *= 0.8;
            }
        }

        boolean isOnGround() {
            return y >= HEIGHT - GROUND_HEIGHT - PLAYER_SIZE/2;
        }

        void draw(Graphics g) {
            // Draw player
            g.setColor(color);
            g.fillOval(x - PLAYER_SIZE/2, y - PLAYER_SIZE/2, PLAYER_SIZE, PLAYER_SIZE);

            // Draw team indicator
            g.setColor(Color.WHITE);
            g.drawString(team == 1 ? "1" : "2", x - 5, y + 5);

            // Draw disc if player has it
            if (hasDisc) {
                g.setColor(Color.WHITE);
                g.fillOval(x - DISC_SIZE/2, y - PLAYER_SIZE/2 - DISC_SIZE/2, DISC_SIZE, DISC_SIZE);
            }
        }
    }

    // Disc class
    class Disc {
        int x, y;
        double dx, dy;
        boolean inMotion = false;
        int rotation = 0;

        Disc(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void throwDisc(int dx, int dy, double speed) {
            // Normalize direction vector
            double length = Math.sqrt(dx * dx + dy * dy);
            this.dx = (dx / length) * speed;
            this.dy = (dy / length) * speed;
            inMotion = true;
        }

        void update() {
            if (inMotion) {
                x += dx;
                y += dy;

                // Apply gravity
                dy += 0.2;

                // Apply air resistance
                dx *= 0.99;
                dy *= 0.99;

                // Rotate disc based on speed
                rotation += (int)(Math.sqrt(dx * dx + dy * dy) * 2);

                // Boundary checking (left/right)
                if (x < DISC_SIZE/2) {
                    x = DISC_SIZE/2;
                    dx = -dx * 0.5;
                }
                if (x > WIDTH - DISC_SIZE/2) {
                    x = WIDTH - DISC_SIZE/2;
                    dx = -dx * 0.5;
                }
            }
        }

        boolean collidesWith(Player p) {
            int distX = Math.abs(x - p.x);
            int distY = Math.abs(y - p.y);
            return distX < (PLAYER_SIZE + DISC_SIZE)/2 && distY < (PLAYER_SIZE + DISC_SIZE)/2;
        }

        void draw(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(Color.WHITE);
            g2d.rotate(Math.toRadians(rotation), x, y);
            g2d.fillOval(x - DISC_SIZE/2, y - DISC_SIZE/2, DISC_SIZE, DISC_SIZE);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(x - DISC_SIZE/2, y - DISC_SIZE/2, DISC_SIZE, DISC_SIZE);
            g2d.dispose();
        }
    }

    // Particle class for visual effects
    class Particle {
        double x, y;
        double dx, dy;
        Color color;
        int life;
        int maxLife;

        Particle(double x, double y, double dx, double dy, Color color, int maxLife) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.color = color;
            this.maxLife = maxLife;
            this.life = maxLife;
        }

        void update() {
            x += dx;
            y += dy;
            dy += 0.1; // Gravity
            life--;
        }

        boolean isDead() {
            return life <= 0;
        }

        void draw(Graphics g) {
            float alpha = (float) life / maxLife;
            Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255));
            g.setColor(c);
            g.fillOval((int)x, (int)y, 4, 4);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Ultimate Frisbee Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new UltimateFrisbeeGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}