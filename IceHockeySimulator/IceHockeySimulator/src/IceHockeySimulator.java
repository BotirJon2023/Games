import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class IceHockeySimulator extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final int GOAL_WIDTH = 180;
    private static final int GOAL_Y = HEIGHT/2 - GOAL_WIDTH/2;
    private static final int PUCK_SIZE = 16;
    private static final int PLAYER_SIZE = 32;
    private static final int MAX_SPEED = 12;

    // Game objects
    private Puck puck;
    private Player player1, player2;
    private Goal goal1, goal2;
    private int score1 = 0, score2 = 0;
    private int period = 1;
    private int gameTime = 20 * 60; // 20 minutes in seconds
    private Timer timer;
    private boolean gameRunning = true;
    private boolean againstComputer = true;
    private boolean paused = false;

    // Input states
    private Set<Integer> pressedKeys = new HashSet<>();

    // AI variables
    private Random random = new Random();
    private int aiReactionDelay = 0;

    public IceHockeySimulator(boolean vsComputer) {
        this.againstComputer = vsComputer;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(200, 220, 255));
        setFocusable(true);
        addKeyListener(this);

        // Initialize game objects
        puck = new Puck(WIDTH/2, HEIGHT/2);
        player1 = new Player(100, HEIGHT/2, Color.RED, KeyEvent.VK_W, KeyEvent.VK_S,
                KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_SPACE);

        if (againstComputer) {
            player2 = new Player(WIDTH - 100, HEIGHT/2, Color.BLUE, 0, 0, 0, 0, 0);
            player2.isAI = true;
        } else {
            player2 = new Player(WIDTH - 100, HEIGHT/2, Color.BLUE, KeyEvent.VK_UP,
                    KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_ENTER);
        }

        goal1 = new Goal(20, GOAL_Y, 10, GOAL_WIDTH);
        goal2 = new Goal(WIDTH - 30, GOAL_Y, 10, GOAL_WIDTH);

        timer = new Timer(1000 / 60, this); // 60 FPS
        timer.start();

        // Game clock timer
        Timer gameClock = new Timer(1000, e -> {
            if (gameRunning && !paused) {
                gameTime--;
                if (gameTime <= 0) {
                    nextPeriod();
                }
            }
        });
        gameClock.start();
    }

    private void nextPeriod() {
        if (period < 3) {
            period++;
            gameTime = 20 * 60;
            resetPosition();
            showMessage("Period " + period + " Started!");
        } else {
            endGame();
        }
    }

    private void endGame() {
        gameRunning = false;
        timer.stop();
        String winner = score1 > score2 ? "Player 1 Wins!" : (score2 > score1 ? "Player 2 Wins!" : "Tie Game!");
        JOptionPane.showMessageDialog(this,
                "Game Over!\n" + winner + "\nFinal Score: " + score1 + " - " + score2,
                "Game Over", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    private void showMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Period", JOptionPane.INFORMATION_MESSAGE);
    }

    private void resetPosition() {
        puck.x = WIDTH/2;
        puck.y = HEIGHT/2;
        puck.vx = 0;
        puck.vy = 0;
        player1.x = 100;
        player1.y = HEIGHT/2;
        player2.x = WIDTH - 100;
        player2.y = HEIGHT/2;
    }

    private void checkGoal() {
        if (puck.x - PUCK_SIZE/2 < goal1.x + goal1.width &&
                puck.y + PUCK_SIZE/2 > goal1.y && puck.y - PUCK_SIZE/2 < goal1.y + goal1.height) {
            score2++;
            resetPosition();
            showMessage("GOAL! Player 2 scores! " + score1 + " - " + score2);
        } else if (puck.x + PUCK_SIZE/2 > goal2.x &&
                puck.y + PUCK_SIZE/2 > goal2.y && puck.y - PUCK_SIZE/2 < goal2.y + goal2.height) {
            score1++;
            resetPosition();
            showMessage("GOAL! Player 1 scores! " + score1 + " - " + score2);
        }
    }

    private void updatePhysics() {
        if (!gameRunning || paused) return;

        // Update player movement
        player1.update(pressedKeys);
        player2.update(pressedKeys);

        // AI logic
        if (player2.isAI) {
            aiReactionDelay++;
            if (aiReactionDelay > 5) {
                aiReactionDelay = 0;
                updateAI();
            }
        }

        // Boundary checks for players
        player1.x = Math.max(PLAYER_SIZE/2, Math.min(WIDTH/2 - 50, player1.x));
        player1.y = Math.max(PLAYER_SIZE/2, Math.min(HEIGHT - PLAYER_SIZE/2, player1.y));
        player2.x = Math.max(WIDTH/2 + 50, Math.min(WIDTH - PLAYER_SIZE/2, player2.x));
        player2.y = Math.max(PLAYER_SIZE/2, Math.min(HEIGHT - PLAYER_SIZE/2, player2.y));

        // Update puck with friction
        puck.vx *= 0.995;
        puck.vy *= 0.995;
        puck.x += puck.vx;
        puck.y += puck.vy;

        // Puck boundary collisions
        if (puck.y - PUCK_SIZE/2 < 0 || puck.y + PUCK_SIZE/2 > HEIGHT) {
            puck.vy = -puck.vy;
            puck.y = Math.max(PUCK_SIZE/2, Math.min(HEIGHT - PUCK_SIZE/2, puck.y));
        }

        if (puck.x - PUCK_SIZE/2 < 0 || puck.x + PUCK_SIZE/2 > WIDTH) {
            puck.vx = -puck.vx;
            puck.x = Math.max(PUCK_SIZE/2, Math.min(WIDTH - PUCK_SIZE/2, puck.x));
        }

        // Player collisions with puck
        handleCollision(player1, puck);
        handleCollision(player2, puck);

        // Check for goals
        checkGoal();
    }

    private void handleCollision(Player player, Puck puck) {
        double dx = puck.x - player.x;
        double dy = puck.y - player.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double minDist = (PLAYER_SIZE + PUCK_SIZE) / 2.0;

        if (distance < minDist) {
            // Collision detected
            double angle = Math.atan2(dy, dx);
            double force = Math.min(MAX_SPEED, Math.hypot(player.vx, player.vy) + 5);

            // Add player velocity to puck
            if (player.isShooting) {
                force = MAX_SPEED * 1.5;
                player.isShooting = false;
            }

            puck.vx = Math.cos(angle) * force + player.vx * 0.8;
            puck.vy = Math.sin(angle) * force + player.vy * 0.8;

            // Separate puck from player
            double overlap = minDist - distance;
            puck.x += Math.cos(angle) * overlap;
            puck.y += Math.sin(angle) * overlap;
        }
    }

    private void updateAI() {
        // Simple but realistic AI: try to intercept puck and shoot
        double dx = puck.x - player2.x;
        double dy = puck.y - player2.y;
        double distance = Math.hypot(dx, dy);

        // Move towards puck
        if (distance > 50) {
            if (dx > 10) player2.vx = Math.min(MAX_SPEED, player2.vx + 0.5);
            if (dx < -10) player2.vx = Math.max(-MAX_SPEED, player2.vx - 0.5);
            if (dy > 10) player2.vy = Math.min(MAX_SPEED, player2.vy + 0.5);
            if (dy < -10) player2.vy = Math.max(-MAX_SPEED, player2.vy - 0.5);
        } else {
            // AI aims for goal when close to puck
            player2.vx = (goal1.x + goal1.width/2 - player2.x) * 0.05;
            player2.vy = (goal1.y + goal1.height/2 - player2.y) * 0.05;

            // Random shooting
            if (random.nextInt(100) < 15) {
                player2.isShooting = true;
            }
        }

        // Apply friction to AI movement
        player2.vx *= 0.9;
        player2.vy *= 0.9;
        player2.x += player2.vx;
        player2.y += player2.vy;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw ice rink
        g2d.setColor(new Color(230, 240, 255));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        g2d.setColor(Color.WHITE);
        g2d.drawLine(WIDTH/2, 0, WIDTH/2, HEIGHT);
        g2d.drawOval(WIDTH/2 - 50, HEIGHT/2 - 50, 100, 100);

        // Draw goals
        g2d.setColor(Color.GRAY);
        g2d.fillRect(goal1.x, goal1.y, goal1.width, goal1.height);
        g2d.fillRect(goal2.x, goal2.y, goal2.width, goal2.height);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(goal1.x, goal1.y, goal1.width, goal1.height);
        g2d.drawRect(goal2.x, goal2.y, goal2.width, goal2.height);

        // Draw players
        player1.draw(g2d);
        player2.draw(g2d);

        // Draw puck
        g2d.setColor(Color.BLACK);
        g2d.fillOval((int)(puck.x - PUCK_SIZE/2), (int)(puck.y - PUCK_SIZE/2), PUCK_SIZE, PUCK_SIZE);

        // Draw scoreboard
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.setColor(Color.BLACK);
        g2d.drawString(score1 + " - " + score2, WIDTH/2 - 50, 60);

        // Draw game info
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        int minutes = gameTime / 60;
        int seconds = gameTime % 60;
        g2d.drawString(String.format("Period %d: %02d:%02d", period, minutes, seconds), 20, 50);

        if (againstComputer) {
            g2d.drawString("vs COMPUTER", WIDTH - 200, 50);
        } else {
            g2d.drawString("2 PLAYER MODE", WIDTH - 200, 50);
        }

        if (paused) {
            g2d.setFont(new Font("Arial", Font.BOLD, 72));
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.drawString("PAUSED", WIDTH/2 - 100, HEIGHT/2);
        }

        // Draw instructions
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawString("Player 1: WASD + Space (Shoot)", 20, HEIGHT - 60);
        if (againstComputer) {
            g2d.drawString("Player 2: Computer Controlled", 20, HEIGHT - 40);
        } else {
            g2d.drawString("Player 2: Arrow Keys + Enter (Shoot)", 20, HEIGHT - 40);
        }
        g2d.drawString("P - Pause | ESC - Quit", 20, HEIGHT - 20);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updatePhysics();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys.add(e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_P) {
            paused = !paused;
        }
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            System.exit(0);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner classes
    class Puck {
        double x, y, vx, vy;
        Puck(double x, double y) { this.x = x; this.y = y; vx = 0; vy = 0; }
    }

    class Player {
        double x, y, vx, vy;
        Color color;
        int up, down, left, right, shoot;
        boolean isAI = false;
        boolean isShooting = false;

        Player(int x, int y, Color color, int up, int down, int left, int right, int shoot) {
            this.x = x; this.y = y;
            this.color = color;
            this.up = up; this.down = down; this.left = left; this.right = right; this.shoot = shoot;
        }

        void update(Set<Integer> keys) {
            if (isAI) return;

            if (keys.contains(up)) vy = Math.max(-MAX_SPEED, vy - 0.5);
            if (keys.contains(down)) vy = Math.min(MAX_SPEED, vy + 0.5);
            if (keys.contains(left)) vx = Math.max(-MAX_SPEED, vx - 0.5);
            if (keys.contains(right)) vx = Math.min(MAX_SPEED, vx + 0.5);
            if (keys.contains(shoot)) isShooting = true;

            // Apply friction
            vx *= 0.95;
            vy *= 0.95;
            x += vx;
            y += vy;
        }

        void draw(Graphics2D g) {
            g.setColor(color);
            g.fillOval((int)(x - PLAYER_SIZE/2), (int)(y - PLAYER_SIZE/2), PLAYER_SIZE, PLAYER_SIZE);
            g.setColor(Color.WHITE);
            g.fillRect((int)(x - PLAYER_SIZE/4), (int)(y - PLAYER_SIZE/4), PLAYER_SIZE/2, PLAYER_SIZE/2);
            if (isShooting) {
                g.setColor(Color.YELLOW);
                g.drawOval((int)(x - PLAYER_SIZE/1.5), (int)(y - PLAYER_SIZE/1.5), PLAYER_SIZE + 10, PLAYER_SIZE + 10);
            }
        }
    }

    class Goal {
        int x, y, width, height;
        Goal(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    public static void main(String[] args) {
        String[] options = {"Play vs Computer", "Two Players"};
        int choice = JOptionPane.showOptionDialog(null, "Select Game Mode", "Ice Hockey Simulator",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        JFrame frame = new JFrame("Ice Hockey Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new IceHockeySimulator(choice == 0));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}