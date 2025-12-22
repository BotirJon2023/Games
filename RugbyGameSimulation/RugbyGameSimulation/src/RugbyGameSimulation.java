import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;
import java.util.*;
import java.util.List;


public class RugbyGameSimulation extends JPanel implements ActionListener, KeyListener {

    // Window dimensions
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;

    // Field dimensions (scaled)
    private static final int FIELD_WIDTH = 900;
    private static final int FIELD_HEIGHT = 500;
    private static final int FIELD_X = 50;
    private static final int FIELD_Y = 50;

    // Try zones
    private static final int TRY_ZONE_DEPTH = 50;

    // Player and ball settings
    private static final int PLAYER_SIZE = 20;
    private static final int BALL_SIZE = 12;
    private static final double PLAYER_SPEED = 2.5;
    private static final double BALL_SPEED = 8.0;

    // Game state
    private Timer timer;
    private Random random = new Random();

    // Teams
    private List<Player> teamA; // Blue - attacking from left to right initially
    private List<Player> teamB; // Red

    private Ball ball;

    private int scoreA = 0;
    private int scoreB = 0;

    private boolean paused = true;
    private String message = "Press SPACE to start the game!";

    // Possession: 0 = Team A, 1 = Team B, -1 = no possession (kick/off ground)
    private int possession = 0;

    public RugbyGameSimulation() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        initGame();

        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    private void initGame() {
        teamA = new ArrayList<>();
        teamB = new ArrayList<>();

        // Initialize Team A (blue) positions - starting on left side
        for (int i = 0; i < 11; i++) {
            double x = FIELD_X + 100 + random.nextInt(200);
            double y = FIELD_Y + 50 + random.nextInt(FIELD_HEIGHT - 100);
            teamA.add(new Player(x, y, Color.BLUE, "A" + (i + 1)));
        }

        // Initialize Team B (red) positions - starting on right side
        for (int i = 0; i < 11; i++) {
            double x = FIELD_X + FIELD_WIDTH - 300 + random.nextInt(200);
            double y = FIELD_Y + 50 + random.nextInt(FIELD_HEIGHT - 100);
            teamB.add(new Player(x, y, Color.RED, "B" + (i + 1)));
        }

        // Ball starts with Team A
        ball = new Ball(teamA.get(0).x, teamA.get(0).y);
        possession = 0;
        teamA.get(0).hasBall = true;

        scoreA = 0;
        scoreB = 0;
        message = "";
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw field
        g2d.setColor(new Color(0, 128, 0)); // Dark green
        g2d.fillRect(FIELD_X, FIELD_Y, FIELD_WIDTH, FIELD_HEIGHT);

        // Field lines
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(FIELD_X, FIELD_Y, FIELD_WIDTH, FIELD_HEIGHT);

        // Halfway line
        g2d.drawLine(FIELD_X + FIELD_WIDTH / 2, FIELD_Y, FIELD_X + FIELD_WIDTH / 2, FIELD_Y + FIELD_HEIGHT);

        // 22m lines (approximate)
        int line1 = FIELD_X + (FIELD_WIDTH / 5);
        int line2 = FIELD_X + 4 * (FIELD_WIDTH / 5);
        g2d.drawLine(line1, FIELD_Y, line1, FIELD_Y + FIELD_HEIGHT);
        g2d.drawLine(line2, FIELD_Y, line2, FIELD_Y + FIELD_HEIGHT);

        // Try lines
        g2d.setStroke(new BasicStroke(5));
        g2d.drawLine(FIELD_X, FIELD_Y + TRY_ZONE_DEPTH, FIELD_X + FIELD_WIDTH, FIELD_Y + TRY_ZONE_DEPTH);
        g2d.drawLine(FIELD_X, FIELD_Y + FIELD_HEIGHT - TRY_ZONE_DEPTH, FIELD_X + FIELD_WIDTH, FIELD_Y + FIELD_HEIGHT - TRY_ZONE_DEPTH);

        // Draw players
        for (Player p : teamA) {
            p.draw(g2d);
        }
        for (Player p : teamB) {
            p.draw(g2d);
        }

        // Draw ball
        ball.draw(g2d);

        // Draw scores and messages
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("Team A (Blue): " + scoreA, 20, 30);
        g2d.drawString("Team B (Red): " + scoreB, WIDTH - 250, 30);

        if (!message.isEmpty()) {
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            g2d.setColor(Color.YELLOW);
            FontMetrics fm = g2d.getFontMetrics();
            int msgWidth = fm.stringWidth(message);
            g2d.drawString(message, (WIDTH - msgWidth) / 2, HEIGHT / 2);
        }

        if (paused && message.isEmpty()) {
            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawString("SPACE - Start/Pause | R - Reset", WIDTH / 2 - 150, HEIGHT - 30);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!paused) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        List<Player> attackingTeam = possession == 0 ? teamA : teamB;
        List<Player> defendingTeam = possession == 0 ? teamB : teamA;

        Player ballCarrier = null;
        for (Player p : attackingTeam) {
            if (p.hasBall) {
                ballCarrier = p;
                break;
            }
        }

        // If no ball carrier (e.g., kick), move ball independently
        if (ballCarrier == null) {
            ball.updatePosition(); // free ball movement
            checkBallPickup();
            return;
        }

        // Sync ball with carrier
        ball.x = ballCarrier.x;
        ball.y = ballCarrier.y;

        // Move attacking team towards opponent's try line
        double targetX = possession == 0 ? FIELD_X + FIELD_WIDTH - 50 : FIELD_X + 50;

        for (Player p : attackingTeam) {
            moveTowards(p, targetX, ballCarrier.y + random.nextDouble() * 100 - 50);
        }

        // Defending team moves to tackle
        for (Player defender : defendingTeam) {
            moveTowards(defender, ballCarrier.x, ballCarrier.y);
        }

        // Check for tackles
        for (Player defender : defendingTeam) {
            if (distance(defender, ballCarrier) < PLAYER_SIZE) {
                handleTackle(ballCarrier, defender);
                return; // tackle happened
            }
        }

        // Random events: pass or kick forward
        if (random.nextInt(100) < 5) { // 5% chance per frame to pass/kick
            if (random.nextBoolean()) {
                // Pass to nearby teammate
                handlePass(ballCarrier);
            } else {
                // Kick forward
                handleKick(ballCarrier);
            }
        }

        // Check for try
        if (possession == 0 && ballCarrier.x > FIELD_X + FIELD_WIDTH - TRY_ZONE_DEPTH) {
            scoreTry(0);
        } else if (possession == 1 && ballCarrier.x < FIELD_X + TRY_ZONE_DEPTH) {
            scoreTry(1);
        }
    }

    private void moveTowards(Player p, double targetX, double targetY) {
        double dx = targetX - p.x;
        double dy = targetY - p.y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > 5) {
            p.x += (dx / dist) * PLAYER_SPEED;
            p.y += (dy / dist) * PLAYER_SPEED;

            // Keep within field bounds
            p.x = Math.max(FIELD_X + PLAYER_SIZE, Math.min(FIELD_X + FIELD_WIDTH - PLAYER_SIZE, p.x));
            p.y = Math.max(FIELD_Y + PLAYER_SIZE, Math.min(FIELD_Y + FIELD_HEIGHT - PLAYER_SIZE, p.y));
        }
    }

    private double distance(Player a, Player b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    private void handleTackle(Player carrier, Player tackler) {
        carrier.hasBall = false;
        message = "Tackle! Turnover possible...";
        // 50% chance of turnover
        if (random.nextBoolean()) {
            possession = possession == 0 ? 1 : 0;
            tackler.hasBall = true;
            message = "Turnover! " + (possession == 0 ? "Team A" : "Team B") + " has the ball!";
        } else {
            // Ruck - attacking team retains
            message = "Ruck won by attackers!";
        }
        // Brief pause for message
        Timer msgTimer = new Timer(2000, e -> message = "");
        msgTimer.setRepeats(false);
        msgTimer.start();
    }

    private void handlePass(Player carrier) {
        List<Player> team = possession == 0 ? teamA : teamB;
        Player target = null;
        double minDist = Double.MAX_VALUE;
        for (Player p : team) {
            if (p != carrier && distance(p, carrier) < 150) {
                double d = distance(p, carrier);
                if (d < minDist) {
                    minDist = d;
                    target = p;
                }
            }
        }
        if (target != null) {
            carrier.hasBall = false;
            target.hasBall = true;
            message = "Pass completed!";
            Timer msgTimer = new Timer(1000, e -> message = "");
            msgTimer.setRepeats(false);
            msgTimer.start();
        }
    }

    private void handleKick(Player carrier) {
        carrier.hasBall = false;
        double direction = possession == 0 ? 1 : -1; // forward
        ball.vx = direction * (BALL_SPEED + random.nextDouble() * 3);
        ball.vy = random.nextDouble() * 4 - 2;
        message = "Kick ahead!";
        Timer msgTimer = new Timer(1500, e -> message = "");
        msgTimer.setRepeats(false);
        msgTimer.start();
    }

    private void checkBallPickup() {
        // Check if any player is close to loose ball
        for (Player p : teamA) {
            if (distance(p, ball.x, ball.y) < PLAYER_SIZE + BALL_SIZE) {
                p.hasBall = true;
                possession = 0;
                message = "Team A picks up loose ball!";
                Timer msgTimer = new Timer(1500, e -> message = "");
                msgTimer.setRepeats(false);
                msgTimer.start();
                return;
            }
        }
        for (Player p : teamB) {
            if (distance(p, ball.x, ball.y) < PLAYER_SIZE + BALL_SIZE) {
                p.hasBall = true;
                possession = 1;
                message = "Team B picks up loose ball!";
                Timer msgTimer = new Timer(1500, e -> message = "");
                msgTimer.setRepeats(false);
                msgTimer.start();
                return;
            }
        }
    }

    private double distance(Player p, double bx, double by) {
        return Math.sqrt(Math.pow(p.x - bx, 2) + Math.pow(p.y - by, 2));
    }

    private void scoreTry(int team) {
        if (team == 0) {
            scoreA += 5;
            message = "TRY! Team A scores!";
        } else {
            scoreB += 5;
            message = "TRY! Team B scores!";
        }
        paused = true; // Pause after try
        Timer msgTimer = new Timer(3000, e -> {
            message = "Kickoff restart... Press SPACE to continue";
        });
        msgTimer.setRepeats(false);
        msgTimer.start();

        // Reset positions roughly
        initGame();
    }

    // Player class
    private static class Player {
        double x, y;
        Color color;
        String label;
        boolean hasBall = false;

        Player(double x, double y, Color color, String label) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.label = label;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.fillOval((int) (x - PLAYER_SIZE / 2), (int) (y - PLAYER_SIZE / 2), PLAYER_SIZE, PLAYER_SIZE);
            if (hasBall) {
                g2d.setColor(Color.ORANGE);
                g2d.fillOval((int) (x - BALL_SIZE / 2), (int) (y - BALL_SIZE / 2), BALL_SIZE, BALL_SIZE * 2);
            }
            g2d.setColor(Color.WHITE);
            FontMetrics fm = g2d.getFontMetrics();
            int labelW = fm.stringWidth(label);
            g2d.drawString(label, (int) x - labelW / 2, (int) y + 5);
        }
    }

    // Ball class (for loose ball)
    private static class Ball {
        double x, y;
        double vx = 0, vy = 0;

        Ball(double x, double y) {
            this.x = x;
            this.y = y;
        }

        void updatePosition() {
            x += vx;
            y += vy;
            vy += 0.2; // gravity
            vx *= 0.98; // friction

            // Bounce on ground
            if (y > FIELD_Y + FIELD_HEIGHT - PLAYER_SIZE) {
                y = FIELD_Y + FIELD_HEIGHT - PLAYER_SIZE;
                vy = -vy * 0.6;
                vx *= 0.8;
            }
            // Bounds
            x = Math.max(FIELD_X + BALL_SIZE, Math.min(FIELD_X + FIELD_WIDTH - BALL_SIZE, x));
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(Color.ORANGE.darker());
            g2d.fillOval((int) (x - BALL_SIZE / 2), (int) (y - BALL_SIZE), BALL_SIZE, BALL_SIZE * 2);
        }
    }

    // Key controls
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            paused = !paused;
            if (paused) {
                message = "Paused";
            } else {
                message = "";
            }
        } else if (e.getKeyCode() == KeyEvent.VK_R) {
            initGame();
            paused = true;
            message = "Game reset! Press SPACE to start";
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Rugby Game Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new RugbyGameSimulation());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}