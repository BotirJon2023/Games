import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class RugbySevensSimulator2 extends JPanel implements ActionListener {
    // Constants
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;
    private static final int PLAYER_SIZE = 20;
    private static final int BALL_SIZE = 10;
    private static final int TEAM_SIZE = 7;
    private static final int GAME_DURATION = 7 * 60; // 7 minutes in seconds

    private Timer timer;
    private int timeLeft;

    private Team teamA;
    private Team teamB;
    private Ball ball;
    private String message = "";

    public RugbySevensSimulator2() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.GREEN);
        teamA = new Team("Team A", Color.BLUE, true);
        teamB = new Team("Team B", Color.RED, false);
        ball = new Ball(WIDTH / 2, HEIGHT / 2);

        timeLeft = GAME_DURATION;
        timer = new Timer(100, this);
        timer.start();
    }

    public void actionPerformed(ActionEvent e) {
        if (timeLeft <= 0) {
            message = "Game Over! " + getWinner();
            timer.stop();
            repaint();
            return;
        }

        timeLeft--;
        teamA.movePlayers();
        teamB.movePlayers();
        ball.move();
        simulatePlay();
        repaint();
    }

    private void simulatePlay() {
        Player ballCarrier = teamA.getBallCarrier() != null ? teamA.getBallCarrier() : teamB.getBallCarrier();
        if (ballCarrier != null) {
            ball.setPosition(ballCarrier.getX(), ballCarrier.getY());
            if (new Random().nextInt(100) < 5) {
                if (ballCarrier.getTeam().equals("Team A")) {
                    teamA.passBall(ball);
                } else {
                    teamB.passBall(ball);
                }
            }
            checkTry(ballCarrier);
        } else {
            Player closest = getClosestPlayer(ball.getX(), ball.getY());
            if (closest != null && closest.distanceTo(ball.getX(), ball.getY()) < 15) {
                closest.pickUpBall(ball);
            }
        }
    }

    private Player getClosestPlayer(int x, int y) {
        Player closest = null;
        double minDist = Double.MAX_VALUE;
        for (Player p : teamA.players) {
            double dist = p.distanceTo(x, y);
            if (dist < minDist) {
                minDist = dist;
                closest = p;
            }
        }
        for (Player p : teamB.players) {
            double dist = p.distanceTo(x, y);
            if (dist < minDist) {
                minDist = dist;
                closest = p;
            }
        }
        return closest;
    }

    private void checkTry(Player p) {
        if (p.getTeam().equals("Team A") && p.getX() > WIDTH - 30) {
            teamA.scoreTry();
            resetPlay();
        } else if (p.getTeam().equals("Team B") && p.getX() < 30) {
            teamB.scoreTry();
            resetPlay();
        }
    }

    private void resetPlay() {
        ball.setPosition(WIDTH / 2, HEIGHT / 2);
        teamA.resetPlayers(true);
        teamB.resetPlayers(false);
        teamA.dropBall();
        teamB.dropBall();
    }

    private String getWinner() {
        if (teamA.getScore() > teamB.getScore()) return "Team A Wins!";
        if (teamB.getScore() > teamA.getScore()) return "Team B Wins!";
        return "It's a Draw!";
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawField(g);
        drawTeam(g, teamA);
        drawTeam(g, teamB);
        drawBall(g);
        drawScore(g);
    }

    private void drawField(Graphics g) {
        g.setColor(Color.WHITE);
        g.drawRect(20, 20, WIDTH - 40, HEIGHT - 40);
        g.drawLine(WIDTH / 2, 20, WIDTH / 2, HEIGHT - 20);
        g.drawString("Time Left: " + timeLeft + "s", WIDTH / 2 - 40, 15);
        if (!message.isEmpty()) {
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString(message, WIDTH / 2 - 100, HEIGHT / 2);
        }
    }

    private void drawTeam(Graphics g, Team team) {
        for (Player p : team.players) {
            g.setColor(team.color);
            g.fillOval(p.getX() - PLAYER_SIZE / 2, p.getY() - PLAYER_SIZE / 2, PLAYER_SIZE, PLAYER_SIZE);
            g.setColor(Color.WHITE);
            g.drawString(String.valueOf(p.getNumber()), p.getX() - 5, p.getY() + 5);
        }
    }

    private void drawBall(Graphics g) {
        g.setColor(Color.ORANGE);
        g.fillOval(ball.getX() - BALL_SIZE / 2, ball.getY() - BALL_SIZE / 2, BALL_SIZE, BALL_SIZE);
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.WHITE);
        g.drawString(teamA.name + ": " + teamA.getScore(), 50, 15);
        g.drawString(teamB.name + ": " + teamB.getScore(), WIDTH - 150, 15);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Rugby Sevens Simulator");
        RugbySevensSimulator2 game = new RugbySevensSimulator2();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

class Player {
    private int x, y;
    private int number;
    private String team;
    private boolean hasBall;

    public Player(int number, String team, int x, int y) {
        this.number = number;
        this.team = team;
        this.x = x;
        this.y = y;
        this.hasBall = false;
    }

    public void move() {
        Random r = new Random();
        x += r.nextInt(7) - 3;
        y += r.nextInt(7) - 3;

        if (x < 30) x = 30;
        if (x > 970) x = 970;
        if (y < 30) y = 30;
        if (y > 570) y = 570;
    }

    public void pickUpBall(Ball ball) {
        this.hasBall = true;
        ball.setCarrier(this);
    }

    public void dropBall() {
        this.hasBall = false;
    }

    public boolean hasBall() {
        return hasBall;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getNumber() { return number; }
    public String getTeam() { return team; }

    public double distanceTo(int bx, int by) {
        return Math.sqrt((x - bx) * (x - bx) + (y - by) * (y - by));
    }
}

class Ball {
    private int x, y;
    private Player carrier;

    public Ball(int x, int y) {
        this.x = x;
        this.y = y;
        this.carrier = null;
    }

    public void move() {
        if (carrier != null) {
            x = carrier.getX();
            y = carrier.getY();
        }
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        this.carrier = null;
    }

    public void setCarrier(Player p) {
        this.carrier = p;
    }

    public Player getCarrier() {
        return carrier;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}

class Team {
    public String name;
    public Color color;
    public List<Player> players;
    private int score;

    public Team(String name, Color color, boolean leftSide) {
        this.name = name;
        this.color = color;
        this.players = new ArrayList<>();
        this.score = 0;
        initPlayers(leftSide);
    }

    private void initPlayers(boolean leftSide) {
        int baseX = leftSide ? 100 : 900;
        int baseY = 100;
        for (int i = 0; i < 7; i++) {
            players.add(new Player(i + 1, name, baseX, baseY + i * 50));
        }
    }

    public void movePlayers() {
        for (Player p : players) {
            p.move();
        }
    }

    public void resetPlayers(boolean leftSide) {
        int baseX = leftSide ? 100 : 900;
        int baseY = 100;
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            p.dropBall();
            players.set(i, new Player(i + 1, name, baseX, baseY + i * 50));
        }
    }

    public void dropBall() {
        for (Player p : players) {
            p.dropBall();
        }
    }

    public void passBall(Ball ball) {
        for (Player p : players) {
            if (!p.hasBall()) {
                Player carrier = ball.getCarrier();
                if (carrier != null) carrier.dropBall();
                p.pickUpBall(ball);
                break;
            }
        }
    }

    public Player getBallCarrier() {
        for (Player p : players) {
            if (p.hasBall()) return p;
        }
        return null;
    }

    public void scoreTry() {
        score += 5;
    }

    public int getScore() {
        return score;
    }
}
