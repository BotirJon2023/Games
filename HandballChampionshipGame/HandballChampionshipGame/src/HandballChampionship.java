import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class HandballChampionship extends JPanel implements ActionListener {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;
    private static final int COURT_WIDTH = 800;
    private static final int COURT_HEIGHT = 500;
    private static final int PLAYER_SIZE = 30;
    private static final int BALL_SIZE = 15;
    private static final int GOAL_WIDTH = 100;
    private static final int GOAL_HEIGHT = 200;
    private static final int GAME_TIME = 180; // seconds (3 minutes per half, but simplified)

    private Timer timer;
    private Random random = new Random();

    // Teams
    private ArrayList<Player> teamRed;
    private ArrayList<Player> teamBlue;
    private Ball ball;
    private int scoreRed = 0;
    private int scoreBlue = 0;
    private int gameTimeLeft = GAME_TIME;
    private boolean gameRunning = false;

    // Court elements
    private Rectangle leftGoal = new Rectangle(50, HEIGHT / 2 - GOAL_HEIGHT / 2, 20, GOAL_HEIGHT);
    private Rectangle rightGoal = new Rectangle(WIDTH - 70, HEIGHT / 2 - GOAL_HEIGHT / 2, 20, GOAL_HEIGHT);

    public HandballChampionship() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(0, 100, 0)); // Green court-like

        initGame();
        timer = new Timer(20, this); // ~50 FPS
        timer.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!gameRunning) {
                    gameRunning = true;
                    gameTimeLeft = GAME_TIME;
                }
            }
        });
    }

    private void initGame() {
        teamRed = new ArrayList<>();
        teamBlue = new ArrayList<>();

        // Team Red (left side)
        teamRed.add(new Player(200, HEIGHT / 2, Color.RED, true)); // Goalkeeper
        for (int i = 1; i < 7; i++) {
            teamRed.add(new Player(300 + i * 50, HEIGHT / 2 + (i % 2 == 0 ? 50 : -50), Color.RED, false));
        }

        // Team Blue (right side)
        teamBlue.add(new Player(WIDTH - 200, HEIGHT / 2, Color.BLUE, true)); // Goalkeeper
        for (int i = 1; i < 7; i++) {
            teamBlue.add(new Player(WIDTH - 300 - i * 50, HEIGHT / 2 + (i % 2 == 0 ? 50 : -50), Color.BLUE, false));
        }

        ball = new Ball(WIDTH / 2, HEIGHT / 2);
        assignBallToRandomPlayer();
    }

    private void assignBallToRandomPlayer() {
        ArrayList<Player> allPlayers = new ArrayList<>();
        allPlayers.addAll(teamRed);
        allPlayers.addAll(teamBlue);
        Player holder = allPlayers.get(random.nextInt(allPlayers.size()));
        holder.hasBall = true;
        ball.x = holder.x + PLAYER_SIZE / 2 - BALL_SIZE / 2;
        ball.y = holder.y + PLAYER_SIZE / 2 - BALL_SIZE / 2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw court lines
        g2d.setColor(Color.WHITE);
        g2d.drawRect(100, 50, COURT_WIDTH, COURT_HEIGHT);
        g2d.drawLine(WIDTH / 2, 50, WIDTH / 2, 50 + COURT_HEIGHT); // Center line
        g2d.drawOval(WIDTH / 2 - 50, HEIGHT / 2 - 50, 100, 100); // Center circle

        // Draw goals
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(leftGoal.x, leftGoal.y, leftGoal.width, leftGoal.height);
        g2d.fillRect(rightGoal.x, rightGoal.y, rightGoal.width, rightGoal.height);

        // Draw players
        for (Player p : teamRed) {
            p.draw(g2d);
        }
        for (Player p : teamBlue) {
            p.draw(g2d);
        }

        // Draw ball
        ball.draw(g2d);

        // Draw HUD
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 30));
        g2d.drawString("Red: " + scoreRed, 150, 40);
        g2d.drawString("Blue: " + scoreBlue, WIDTH - 250, 40);
        g2d.drawString("Time: " + (gameTimeLeft / 60) + ":" + String.format("%02d", gameTimeLeft % 60), WIDTH / 2 - 100, 40);

        if (!gameRunning) {
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            g2d.drawString("Click to Start Handball Championship!", 100, HEIGHT / 2);
            g2d.setFont(new Font("Arial", Font.PLAIN, 30));
            g2d.drawString("Simple AI simulation - Watch teams play!", 200, HEIGHT / 2 + 60);
        }

        if (gameTimeLeft <= 0) {
            gameRunning = false;
            g2d.setFont(new Font("Arial", Font.BOLD, 60));
            g2d.drawString("Game Over!", WIDTH / 2 - 200, HEIGHT / 2);
            g2d.drawString(scoreRed > scoreBlue ? "Red Wins!" : scoreBlue > scoreRed ? "Blue Wins!" : "Draw!", WIDTH / 2 - 250, HEIGHT / 2 + 80);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning) {
            updateGame();
            gameTimeLeft--;
            if (gameTimeLeft < 0) gameTimeLeft = 0;
        }
        repaint();
    }

    private void updateGame() {
        // Update players
        for (Player p : teamRed) {
            p.update(teamBlue, ball, true);
        }
        for (Player p : teamBlue) {
            p.update(teamRed, ball, false);
        }

        // Update ball if in flight
        if (ball.inFlight) {
            ball.update();
            checkGoal();
            checkBallOut();
        }
    }

    private void checkGoal() {
        if (ball.getBounds().intersects(leftGoal)) {
            scoreBlue++;
            resetBall();
        } else if (ball.getBounds().intersects(rightGoal)) {
            scoreRed++;
            resetBall();
        }
    }

    private void checkBallOut() {
        if (ball.y < 50 || ball.y > HEIGHT - 50 || ball.x < 100 || ball.x > WIDTH - 100) {
            resetBall();
        }
    }

    private void resetBall() {
        ball.x = WIDTH / 2;
        ball.y = HEIGHT / 2;
        ball.vx = 0;
        ball.vy = 0;
        ball.inFlight = false;
        assignBallToRandomPlayer();
    }

    // Inner classes

    class Player {
        int x, y;
        Color color;
        boolean isGoalkeeper;
        boolean hasBall = false;
        double vx = 0, vy = 0;
        private static final double SPEED = 2.5;
        private static final double PASS_SPEED = 8.0;

        Player(int x, int y, Color color, boolean isGoalkeeper) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.isGoalkeeper = isGoalkeeper;
        }

        void draw(Graphics2D g) {
            g.setColor(color);
            g.fillOval(x, y, PLAYER_SIZE, PLAYER_SIZE);
            if (isGoalkeeper) {
                g.setColor(Color.WHITE);
                g.drawString("GK", x + 8, y + 20);
            }
            if (hasBall) {
                g.setColor(Color.ORANGE);
                g.fillOval(x + PLAYER_SIZE / 2 - BALL_SIZE / 2, y + PLAYER_SIZE / 2 - BALL_SIZE / 2, BALL_SIZE, BALL_SIZE);
            }
        }

        void update(ArrayList<Player> opponents, Ball ball, boolean attackingRight) {
            Player target = findNearestOpponent(opponents);
            Player teammate = findNearestTeammate(attackingRight ? teamRed : teamBlue);

            if (hasBall) {
                // Move towards goal
                int goalX = attackingRight ? WIDTH - 100 : 100;
                moveTowards(goalX, HEIGHT / 2);

                // Pass or shoot randomly
                if (random.nextInt(100) < 5) { // 5% chance per frame to pass/shoot
                    if (random.nextBoolean() && teammate != null) {
                        passTo(teammate);
                    } else {
                        shoot(attackingRight ? WIDTH : 0);
                    }
                }
            } else {
                // Defend or move to ball
                if (ball.inFlight || ball.holder == null) {
                    moveTowards(ball.x, ball.y);
                } else {
                    // Mark opponent with ball
                    Player opponentWithBall = findOpponentWithBall(opponents);
                    if (opponentWithBall != null) {
                        moveTowards(opponentWithBall.x, opponentWithBall.y);
                    }
                }
            }

            // Simple collision avoidance
            avoidCollisions();

            // Apply movement
            x += vx;
            y += vy;
            clampPosition();
        }

        private void moveTowards(int tx, int ty) {
            double dx = tx - (x + PLAYER_SIZE / 2);
            double dy = ty - (y + PLAYER_SIZE / 2);
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 5) {
                vx = (dx / dist) * SPEED;
                vy = (dy / dist) * SPEED;
            } else {
                vx = vy = 0;
            }
        }

        private void passTo(Player teammate) {
            hasBall = false;
            ball.holder = null;
            ball.inFlight = true;
            double dx = teammate.x + PLAYER_SIZE / 2 - (x + PLAYER_SIZE / 2);
            double dy = teammate.y + PLAYER_SIZE / 2 - (y + PLAYER_SIZE / 2);
            double dist = Math.sqrt(dx * dx + dy * dy);
            ball.vx = (dx / dist) * PASS_SPEED;
            ball.vy = (dy / dist) * PASS_SPEED;
            ball.x = x + PLAYER_SIZE / 2 - BALL_SIZE / 2;
            ball.y = y + PLAYER_SIZE / 2 - BALL_SIZE / 2;
            teammate.hasBall = true; // Approximate
        }

        private void shoot(int goalX) {
            hasBall = false;
            ball.holder = null;
            ball.inFlight = true;
            double dx = goalX - (x + PLAYER_SIZE / 2);
            double dy = (HEIGHT / 2) - (y + PLAYER_SIZE / 2) + random.nextInt(100) - 50; // Some inaccuracy
            double dist = Math.sqrt(dx * dx + dy * dy);
            ball.vx = (dx / dist) * (PASS_SPEED + 4);
            ball.vy = (dy / dist) * (PASS_SPEED + 4);
            ball.x = x + PLAYER_SIZE / 2 - BALL_SIZE / 2;
            ball.y = y + PLAYER_SIZE / 2 - BALL_SIZE / 2;
        }

        private Player findNearestOpponent(ArrayList<Player> opponents) {
            Player nearest = null;
            double minDist = Double.MAX_VALUE;
            for (Player o : opponents) {
                double dist = distanceTo(o);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = o;
                }
            }
            return nearest;
        }

        private Player findNearestTeammate(ArrayList<Player> team) {
            Player nearest = null;
            double minDist = Double.MAX_VALUE;
            for (Player t : team) {
                if (t != this) {
                    double dist = distanceTo(t);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = t;
                    }
                }
            }
            return nearest;
        }

        private Player findOpponentWithBall(ArrayList<Player> opponents) {
            for (Player o : opponents) {
                if (o.hasBall) return o;
            }
            return null;
        }

        private double distanceTo(Player other) {
            double dx = other.x - x;
            double dy = other.y - y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        private void avoidCollisions() {
            // Simple avoidance from all players
            ArrayList<Player> all = new ArrayList<>();
            all.addAll(teamRed);
            all.addAll(teamBlue);
            for (Player other : all) {
                if (other != this) {
                    double dx = x - other.x;
                    double dy = y - other.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < PLAYER_SIZE * 1.5 && dist > 0) {
                        vx += (dx / dist) * 0.5;
                        vy += (dy / dist) * 0.5;
                    }
                }
            }
        }

        private void clampPosition() {
            x = Math.max(100, Math.min(x, WIDTH - 100 - PLAYER_SIZE));
            y = Math.max(50, Math.min(y, HEIGHT - 50 - PLAYER_SIZE));
            if (isGoalkeeper) {
                if (color == Color.RED) x = Math.min(x, 150);
                else x = Math.max(x, WIDTH - 150 - PLAYER_SIZE);
            }
        }

        Rectangle getBounds() {
            return new Rectangle(x, y, PLAYER_SIZE, PLAYER_SIZE);
        }
    }

    class Ball {
        int x, y;
        double vx = 0, vy = 0;
        boolean inFlight = false;
        Player holder = null;

        Ball(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(Graphics2D g) {
            g.setColor(Color.ORANGE);
            g.fillOval(x, y, BALL_SIZE, BALL_SIZE);
            g.setColor(Color.BLACK);
            g.drawOval(x, y, BALL_SIZE, BALL_SIZE);
        }

        void update() {
            if (inFlight) {
                x += vx;
                y += vy;
                vy += 0.1; // Gravity effect
                vx *= 0.99; // Friction
                vy *= 0.99;

                // Bounce on court edges
                if (y <= 50 || y >= HEIGHT - 50 - BALL_SIZE) vy = -vy * 0.7;
                if (x <= 100 || x >= WIDTH - 100 - BALL_SIZE) vx = -vx * 0.7;

                // Check if caught by player
                for (Player p : teamRed) {
                    if (p.getBounds().contains(x + BALL_SIZE / 2, y + BALL_SIZE / 2)) {
                        catchBall(p);
                        return;
                    }
                }
                for (Player p : teamBlue) {
                    if (p.getBounds().contains(x + BALL_SIZE / 2, y + BALL_SIZE / 2)) {
                        catchBall(p);
                        return;
                    }
                }
            }
        }

        private void catchBall(Player p) {
            inFlight = false;
            vx = vy = 0;
            p.hasBall = true;
            holder = p;
        }

        Rectangle getBounds() {
            return new Rectangle(x, y, BALL_SIZE, BALL_SIZE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Handball Championship Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new HandballChampionship());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}