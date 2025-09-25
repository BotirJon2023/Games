import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class SoccerSimulation extends JFrame {
    private static final int FIELD_WIDTH = 800;
    private static final int FIELD_HEIGHT = 600;
    private static final int PLAYER_SIZE = 20;
    private static final int BALL_SIZE = 10;
    private static final int GOAL_WIDTH = 100;
    private static final int GOAL_HEIGHT = 200;
    private static final int MAX_SPEED = 5;
    private static final int MAX_KICK_FORCE = 10;
    private static final double FRICTION = 0.98;
    private static final int GAME_DURATION = 90; // Game time in seconds

    private GamePanel gamePanel;
    private ArrayList<Player> teamA;
    private ArrayList<Player> teamB;
    private Ball ball;
    private int scoreA;
    private int scoreB;
    private int gameTime;
    private boolean isRunning;
    private Random random;
    private Timer timer;

    public SoccerSimulation() {
        setTitle("AI-Powered Soccer Team Simulation");
        setSize(FIELD_WIDTH + 20, FIELD_HEIGHT + 60);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        teamA = new ArrayList<>();
        teamB = new ArrayList<>();
        ball = new Ball(FIELD_WIDTH / 2, FIELD_HEIGHT / 2);
        scoreA = 0;
        scoreB = 0;
        gameTime = 0;
        isRunning = true;
        random = new Random();

        initializeTeams();
        gamePanel = new GamePanel();
        add(gamePanel);

        timer = new Timer(16, e -> updateGame()); // ~60 FPS
        timer.start();
        setVisible(true);
    }

    private void initializeTeams() {
        // Team A (Blue, left side)
        teamA.add(new Player(100, FIELD_HEIGHT / 4, true, "A1"));
        teamA.add(new Player(100, 3 * FIELD_HEIGHT / 4, true, "A2"));
        teamA.add(new Player(200, FIELD_HEIGHT / 5, true, "A3"));
        teamA.add(new Player(200, 4 * FIELD_HEIGHT / 5, true, "A4"));
        teamA.add(new Player(300, FIELD_HEIGHT / 2, true, "A5"));
        // Team B (Red, right side)
        teamB.add(new Player(FIELD_WIDTH - 100, FIELD_HEIGHT / 4, false, "B1"));
        teamB.add(new Player(FIELD_WIDTH - 100, 3 * FIELD_HEIGHT / 4, false, "B2"));
        teamB.add(new Player(FIELD_WIDTH - 200, FIELD_HEIGHT / 5, false, "B3"));
        teamB.add(new Player(FIELD_WIDTH - 200, 4 * FIELD_HEIGHT / 5, false, "B4"));
        teamB.add(new Player(FIELD_WIDTH - 300, FIELD_HEIGHT / 2, false, "B5"));
    }

    private void updateGame() {
        if (!isRunning) return;

        gameTime += 16;
        if (gameTime >= GAME_DURATION * 1000) {
            isRunning = false;
            timer.stop();
            JOptionPane.showMessageDialog(this, "Game Over! Final Score: Team A " + scoreA + " - " + scoreB + " Team B");
            return;
        }

        // Update AI for each player
        for (Player player : teamA) {
            updatePlayerAI(player, teamA, teamB);
            player.update();
        }
        for (Player player : teamB) {
            updatePlayerAI(player, teamB, teamA);
            player.update();
        }

        // Update ball
        ball.update();

        // Check for goals
        checkGoals();

        // Repaint the panel
        gamePanel.repaint();
    }

    private void updatePlayerAI(Player player, ArrayList<Player> ownTeam, ArrayList<Player> opponentTeam) {
        // Simple AI: Move toward ball if not in possession, else move toward goal or pass
        double distToBall = distance(player.x, player.y, ball.x, ball.y);
        if (distToBall < PLAYER_SIZE && ball.owner == null) {
            ball.owner = player;
        }

        if (ball.owner == player) {
            // If player has the ball, decide to kick or move toward goal
            if (random.nextDouble() < 0.05) { // 5% chance to kick/pass
                Player target = findNearestTeammate(player, ownTeam);
                if (target != null && random.nextDouble() < 0.5) {
                    // Pass to teammate
                    double angle = Math.atan2(target.y - player.y, target.x - player.x);
                    ball.kick(angle, MAX_KICK_FORCE * random.nextDouble());
                    ball.owner = null;
                } else {
                    // Kick toward opponent goal
                    int goalX = player.isTeamA ? FIELD_WIDTH : 0;
                    double angle = Math.atan2(FIELD_HEIGHT / 2 - player.y, goalX - player.x);
                    ball.kick(angle, MAX_KICK_FORCE * random.nextDouble());
                    ball.owner = null;
                }
            } else {
                // Move toward opponent goal
                int goalX = player.isTeamA ? FIELD_WIDTH : 0;
                double angle = Math.atan2(FIELD_HEIGHT / 2 - player.y, goalX - player.x);
                player.vx = MAX_SPEED * Math.cos(angle);
                player.vy = MAX_SPEED * Math.sin(angle);
                ball.x = player.x;
                ball.y = player.y;
                ball.vx = player.vx;
                ball.vy = player.vy;
            }
        } else {
            // Move toward ball
            double angle = Math.atan2(ball.y - player.y, ball.x - player.x);
            player.vx = MAX_SPEED * Math.cos(angle);
            player.vy = MAX_SPEED * Math.sin(angle);
        }

        // Avoid collisions with other players
        avoidCollisions(player, ownTeam, opponentTeam);
    }

    private Player findNearestTeammate(Player player, ArrayList<Player> team) {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Player teammate : team) {
            if (teammate != player) {
                double dist = distance(player.x, player.y, teammate.x, teammate.y);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = teammate;
                }
            }
        }
        return nearest;
    }

    private void avoidCollisions(Player player, ArrayList<Player> ownTeam, ArrayList<Player> opponentTeam) {
        for (Player other : ownTeam) {
            if (other != player && distance(player.x, player.y, other.x, other.y) < PLAYER_SIZE * 2) {
                double angle = Math.atan2(player.y - other.y, player.x - other.x);
                player.vx += Math.cos(angle) * 0.5;
                player.vy += Math.sin(angle) * 0.5;
            }
        }
        for (Player other : opponentTeam) {
            if (distance(player.x, player.y, other.x, other.y) < PLAYER_SIZE * 2) {
                double angle = Math.atan2(player.y - other.y, player.x - other.x);
                player.vx += Math.cos(angle) * 0.5;
                player.vy += Math.sin(angle) * 0.5;
            }
        }
    }

    private void checkGoals() {
        if (ball.x > FIELD_WIDTH - BALL_SIZE && Math.abs(ball.y - FIELD_HEIGHT / 2) < GOAL_HEIGHT / 2) {
            scoreB++;
            resetBall();
        } else if (ball.x < BALL_SIZE && Math.abs(ball.y - FIELD_HEIGHT / 2) < GOAL_HEIGHT / 2) {
            scoreA++;
            resetBall();
        }
    }

    private void resetBall() {
        ball.x = FIELD_WIDTH / 2;
        ball.y = FIELD_HEIGHT / 2;
        ball.vx = 0;
        ball.vy = 0;
        ball.owner = null;
        // Reset player positions
        for (Player player : teamA) {
            player.x = 100 + random.nextInt(200);
            player.y = random.nextInt(FIELD_HEIGHT);
            player.vx = 0;
            player.vy = 0;
        }
        for (Player player : teamB) {
            player.x = FIELD_WIDTH - 100 - random.nextInt(200);
            player.y = random.nextInt(FIELD_HEIGHT);
            player.vx = 0;
            player.vy = 0;
        }
    }

    private double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    class Player {
        double x, y, vx, vy;
        boolean isTeamA;
        String name;

        Player(double x, double y, boolean isTeamA, String name) {
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = 0;
            this.isTeamA = isTeamA;
            this.name = name;
        }

        void update() {
            x += vx;
            y += vy;
            // Keep player within bounds
            x = Math.max(PLAYER_SIZE, Math.min(FIELD_WIDTH - PLAYER_SIZE, x));
            y = Math.max(PLAYER_SIZE, Math.min(FIELD_HEIGHT - PLAYER_SIZE, y));
            // Apply friction
            vx *= FRICTION;
            vy *= FRICTION;
        }
    }

    class Ball {
        double x, y, vx, vy;
        Player owner;

        Ball(double x, double y) {
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = 0;
            this.owner = null;
        }

        void update() {
            if (owner == null) {
                x += vx;
                y += vy;
                // Keep ball within bounds
                x = Math.max(BALL_SIZE, Math.min(FIELD_WIDTH - BALL_SIZE, x));
                y = Math.max(BALL_SIZE, Math.min(FIELD_HEIGHT - BALL_SIZE, y));
                // Apply friction
                vx *= FRICTION;
                vy *= FRICTION;
            }
        }

        void kick(double angle, double force) {
            vx = force * Math.cos(angle);
            vy = force * Math.sin(angle);
        }
    }

    class GamePanel extends JPanel {
        GamePanel() {
            setBackground(Color.GREEN);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw goals
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, FIELD_HEIGHT / 2 - GOAL_HEIGHT / 2, 10, GOAL_HEIGHT);
            g2d.fillRect(FIELD_WIDTH - 10, FIELD_HEIGHT / 2 - GOAL_HEIGHT / 2, 10, GOAL_HEIGHT);

            // Draw players
            for (Player player : teamA) {
                g2d.setColor(Color.BLUE);
                g2d.fillOval((int) (player.x - PLAYER_SIZE / 2), (int) (player.y - PLAYER_SIZE / 2), PLAYER_SIZE, PLAYER_SIZE);
                g2d.setColor(Color.BLACK);
                g2d.drawString(player.name, (int) player.x - 10, (int) player.y - 10);
            }
            for (Player player : teamB) {
                g2d.setColor(Color.RED);
                g2d.fillOval((int) (player.x - PLAYER_SIZE / 2), (int) (player.y - PLAYER_SIZE / 2), PLAYER_SIZE, PLAYER_SIZE);
                g2d.setColor(Color.BLACK);
                g2d.drawString(player.name, (int) player.x - 10, (int) player.y - 10);
            }

            // Draw ball
            g2d.setColor(Color.BLACK);
            g2d.fillOval((int) (ball.x - BALL_SIZE / 2), (int) (ball.y - BALL_SIZE / 2), BALL_SIZE, BALL_SIZE);

            // Draw score and time
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Team A: " + scoreA + "  Team B: " + scoreB, 10, 30);
            g2d.drawString("Time: " + (gameTime / 1000) + "s", FIELD_WIDTH - 100, 30);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SoccerSimulation::new);
    }
}