import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class UltimateFrisbeeGame extends JFrame {
    public UltimateFrisbeeGame() {
        setTitle("Ultimate Frisbee Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        add(new GamePanel());
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UltimateFrisbeeGame());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private final int FIELD_WIDTH = 1000;
    private final int FIELD_HEIGHT = 600;
    private final int END_ZONE_WIDTH = 100;
    private final int PLAYER_SIZE = 20;
    private final int FRISBEE_SIZE = 10;
    private final int DELAY = 16; // ~60 FPS
    private final Timer timer;
    private ArrayList<Player> team1;
    private ArrayList<Player> team2;
    private Frisbee frisbee;
    private int team1Score;
    private int team2Score;
    private boolean gameRunning;
    private Player controlledPlayer;
    private boolean upPressed, downPressed, leftPressed, rightPressed, throwPressed;
    private double throwPower;
    private double throwAngle;
    private long throwStartTime;
    private Random random;

    public GamePanel() {
        setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        setBackground(new Color(0, 100, 0)); // Green field
        addKeyListener(this);
        setFocusable(true);
        timer = new Timer(DELAY, this);
        random = new Random();
        initializeGame();
        timer.start();
    }

    private void initializeGame() {
        team1 = new ArrayList<>();
        team2 = new ArrayList<>();
        team1Score = 0;
        team2Score = 0;
        gameRunning = true;

        // Initialize Team 1 (Player-controlled)
        team1.add(new Player(100, FIELD_HEIGHT / 2, true));
        team1.add(new Player(150, FIELD_HEIGHT / 3, false));
        team1.add(new Player(150, 2 * FIELD_HEIGHT / 3, false));
        controlledPlayer = team1.get(0);

        // Initialize Team 2 (AI-controlled)
        team2.add(new Player(FIELD_WIDTH - 100, FIELD_HEIGHT / 2, false));
        team2.add(new Player(FIELD_WIDTH - 150, FIELD_HEIGHT / 3, false));
        team2.add(new Player(FIELD_WIDTH - 150, 2 * FIELD_HEIGHT / 3, false));

        frisbee = new Frisbee(FIELD_WIDTH / 2, FIELD_HEIGHT / 2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw field
        g2d.setColor(Color.WHITE);
        g2d.drawRect(0, 0, FIELD_WIDTH, FIELD_HEIGHT);
        g2d.drawLine(END_ZONE_WIDTH, 0, END_ZONE_WIDTH, FIELD_HEIGHT);
        g2d.drawLine(FIELD_WIDTH - END_ZONE_WIDTH, 0, FIELD_WIDTH - END_ZONE_WIDTH, FIELD_HEIGHT);

        // Draw players
        for (Player player : team1) {
            player.draw(g2d);
        }
        for (Player player : team2) {
            player.draw(g2d);
        }

        // Draw frisbee
        frisbee.draw(g2d);

        // Draw score
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Team 1: " + team1Score, 20, 30);
        g2d.drawString("Team 2: " + team2Score, FIELD_WIDTH - 100, 30);

        // Draw throw power meter
        if (throwPressed && controlledPlayer.hasFrisbee) {
            g2d.setColor(Color.YELLOW);
            int meterWidth = (int) (throwPower * 100);
            g2d.fillRect(20, FIELD_HEIGHT - 30, meterWidth, 10);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(20, FIELD_HEIGHT - 30, 100, 10);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning) {
            updateGame();
            repaint();
        }
    }

    private void updateGame() {
        // Update controlled player movement
        if (upPressed) controlledPlayer.move(0, -5);
        if (downPressed) controlledPlayer.move(0, 5);
        if (leftPressed) controlledPlayer.move(-5, 0);
        if (rightPressed) controlledPlayer.move(5, 0);

        // Update throw power
        if (throwPressed && controlledPlayer.hasFrisbee) {
            long currentTime = System.currentTimeMillis();
            throwPower = 0.5 + 0.5 * Math.sin((currentTime - throwStartTime) / 500.0);
        }

        // Update frisbee
        frisbee.update();

        // AI movement
        updateAIMovement();

        // Check collisions
        checkCollisions();

        // Check scoring
        checkScoring();
    }

    private void updateAIMovement() {
        for (Player ai : team2) {
            if (!frisbee.isCaught()) {
                double dx = frisbee.x - ai.x;
                double dy = frisbee.y - ai.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance > 10) {
                    double speed = 3;
                    ai.move(dx / distance * speed, dy / distance * speed);
                }
            } else {
                // Move towards random position
                if (random.nextDouble() < 0.01) {
                    ai.setTarget(random.nextInt(FIELD_WIDTH - END_ZONE_WIDTH) + END_ZONE_WIDTH,
                            random.nextInt(FIELD_HEIGHT));
                }
                ai.moveTowardsTarget(3);
            }
        }
    }

    private void checkCollisions() {
        // Check player-frisbee collisions
        if (!frisbee.isCaught()) {
            for (Player player : team1) {
                if (player.checkCollision(frisbee)) {
                    frisbee.setCaught(true);
                    player.hasFrisbee = true;
                    if (player == controlledPlayer) {
                        frisbee.x = player.x;
                        frisbee.y = player.y;
                    }
                }
            }
            for (Player player : team2) {
                if (player.checkCollision(frisbee)) {
                    frisbee.setCaught(true);
                    player.hasFrisbee = true;
                }
            }
        }
    }

    private void checkScoring() {
        if (frisbee.isCaught()) {
            Player holder = null;
            for (Player player : team1) {
                if (player.hasFrisbee) holder = player;
            }
            for (Player player : team2) {
                if (player.hasFrisbee) holder = player;
            }
            if (holder != null) {
                if (holder.x < END_ZONE_WIDTH && team2.contains(holder)) {
                    team1Score++;
                    resetFrisbee();
                } else if (holder.x > FIELD_WIDTH - END_ZONE_WIDTH && team1.contains(holder)) {
                    team2Score++;
                    resetFrisbee();
                }
            }
        }
    }

    private void resetFrisbee() {
        frisbee = new Frisbee(FIELD_WIDTH / 2, FIELD_HEIGHT / 2);
        for (Player player : team1) player.hasFrisbee = false;
        for (Player player : team2) player.hasFrisbee = false;
        controlledPlayer.hasFrisbee = true;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP: upPressed = true; break;
            case KeyEvent.VK_DOWN: downPressed = true; break;
            case KeyEvent.VK_LEFT: leftPressed = true; break;
            case KeyEvent.VK_RIGHT: rightPressed = true; break;
            case KeyEvent.VK_SPACE:
                if (!throwPressed && controlledPlayer.hasFrisbee) {
                    throwPressed = true;
                    throwStartTime = System.currentTimeMillis();
                    throwAngle = Math.toRadians(random.nextInt(360)); // Random angle for simplicity
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP: upPressed = false; break;
            case KeyEvent.VK_DOWN: downPressed = false; break;
            case KeyEvent.VK_LEFT: leftPressed = false; break;
            case KeyEvent.VK_RIGHT: rightPressed = false; break;
            case KeyEvent.VK_SPACE:
                if (throwPressed && controlledPlayer.hasFrisbee) {
                    throwPressed = false;
                    frisbee.throwFrisbee(controlledPlayer.x, controlledPlayer.y, throwPower, throwAngle);
                    controlledPlayer.hasFrisbee = false;
                }
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

class Player {
    double x, y;
    boolean isControlled;
    boolean hasFrisbee;
    private final int SIZE = 20;
    private double targetX, targetY;

    public Player(double x, double y, boolean isControlled) {
        this.x = x;
        this.y = y;
        this.isControlled = isControlled;
        this.hasFrisbee = isControlled; // Start with controlled player holding frisbee
        this.targetX = x;
        this.targetY = y;
    }

    public void move(double dx, double dy) {
        x = Math.max(0, Math.min(1000, x + dx));
        y = Math.max(0, Math.min(600, y + dy));
        targetX = x;
        targetY = y;
    }

    public void setTarget(double targetX, double targetY) {
        this.targetX = targetX;
        this.targetY = targetY;
    }

    public void moveTowardsTarget(double speed) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > 5) {
            x += dx / distance * speed;
            y += dy / distance * speed;
        }
    }

    public boolean checkCollision(Frisbee frisbee) {
        double dx = frisbee.x - x;
        double dy = frisbee.y - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance < (SIZE + Frisbee.SIZE) / 2.0;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(isControlled ? Color.BLUE : hasFrisbee ? Color.YELLOW : Color.RED);
        g2d.fillOval((int)(x - SIZE / 2), (int)(y - SIZE / 2), SIZE, SIZE);
        g2d.setColor(Color.BLACK);
        g2d.drawOval((int)(x - SIZE / 2), (int)(y - SIZE / 2), SIZE, SIZE);
    }
}

class Frisbee {
    double x, y;
    double vx, vy;
    boolean caught;
    static final int SIZE = 10;

    public Frisbee(double x, double y) {
        this.x = x;
        this.y = y;
        this.vx = 0;
        this.vy = 0;
        this.caught = true;
    }

    public void throwFrisbee(double startX, double startY, double power, double angle) {
        this.x = startX;
        this.y = startY;
        this.vx = Math.cos(angle) * power * 20;
        this.vy = Math.sin(angle) * power * 20;
        this.caught = false;
    }

    public void update() {
        if (!caught) {
            x += vx;
            y += vy;
            vy += 0.1; // Gravity effect
            vx *= 0.99; // Air resistance
            vy *= 0.99;
            if (x < 0 || x > 1000 || y < 0 || y > 600) {
                caught = true;
                x = 500;
                y = 300;
                vx = 0;
                vy = 0;
            }
        }
    }

    public boolean isCaught() {
        return caught;
    }

    public void setCaught(boolean caught) {
        this.caught = caught;
        if (caught) {
            vx = 0;
            vy = 0;
        }
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int)(x - SIZE / 2), (int)(y - SIZE / 2), SIZE, SIZE);
        g2d.setColor(Color.BLACK);
        g2d.drawOval((int)(x - SIZE / 2), (int)(y - SIZE / 2), SIZE, SIZE);
    }
}