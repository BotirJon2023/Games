import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class SoccerManagerGame extends JPanel implements ActionListener, KeyListener {
    // --- Game Parameters ---
    public static final int WIDTH = 900, HEIGHT = 600;
    private Timer timer;
    private Team teamA, teamB;
    private Ball ball;
    private AnimationThread animation;
    private String message = "";
    private int selectedPlayerIdx = 0;
    private boolean gamePaused = false;

    // --- Main: launches JFrame with SoccerManagerGame as panel ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Soccer Manager Game");
            SoccerManagerGame panel = new SoccerManagerGame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // --- Constructor: sets up teams, ball, animation, controls ---
    public SoccerManagerGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(23, 133, 37));
        setFocusable(true);
        addKeyListener(this);

        teamA = new Team("Team A", Color.BLUE, 1);
        teamB = new Team("Team B", Color.RED, 2);
        ball = new Ball(WIDTH / 2, HEIGHT / 2);

        teamA.placePlayers(100, HEIGHT / 2 - 140, 120, 80);
        teamB.placePlayers(WIDTH - 240, HEIGHT / 2 - 140, 120, 80);

        timer = new Timer(30, this);
        timer.start();

        animation = new AnimationThread(this, teamA, teamB, ball);
        animation.start();
    }

    // --- Drawing the game scene ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawField(g);
        teamA.draw(g);
        teamB.draw(g);
        ball.draw(g);
        drawUI(g);
    }

    private void drawField(Graphics g) {
        g.setColor(Color.WHITE);
        g.drawRect(50, 50, WIDTH - 100, HEIGHT - 100);
        g.drawLine(WIDTH / 2, 50, WIDTH / 2, HEIGHT - 50);
        g.drawOval(WIDTH / 2 - 80, HEIGHT / 2 - 80, 160, 160);
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(40, HEIGHT / 2 - 60, 10, 120);
        g.fillRect(WIDTH - 50, HEIGHT / 2 - 60, 10, 120);
    }

    private void drawUI(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Controls: Arrow=move, TAB=switch player, SPACE=pass, S=shoot, P=pause", 20, 20);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        if (!message.isEmpty()) {
            g.drawString(message, 20, 40);
        }
    }

    // --- Timer loop ---
    public void actionPerformed(ActionEvent e) {
        if (!gamePaused) {
            teamA.update(ball, teamB);
            teamB.update(ball, teamA);
            ball.update(teamA, teamB);
        }
        repaint();
    }

    // --- Keyboard controls for player movement and actions ---
    public void keyPressed(KeyEvent e) {
        if (gamePaused) return;
        Player controlled = teamA.getPlayer(selectedPlayerIdx);
        switch (e.getKeyCode()) {
            case KeyEvent.VK_TAB:
                selectedPlayerIdx = (selectedPlayerIdx + 1) % teamA.getPlayers().size();
                setMessage("Selected Player " + (selectedPlayerIdx + 1));
                break;
            case KeyEvent.VK_UP:
                controlled.move(0, -15, teamA, teamB, ball);
                break;
            case KeyEvent.VK_DOWN:
                controlled.move(0, 15, teamA, teamB, ball);
                break;
            case KeyEvent.VK_LEFT:
                controlled.move(-15, 0, teamA, teamB, ball);
                break;
            case KeyEvent.VK_RIGHT:
                controlled.move(15, 0, teamA, teamB, ball);
                break;
            case KeyEvent.VK_SPACE:
                if (controlled.hasBall(ball)) {
                    controlled.pass(ball, teamA, teamB);
                    setMessage("Passed the ball!");
                }
                break;
            case KeyEvent.VK_S:
                if (controlled.hasBall(ball)) {
                    controlled.shoot(ball);
                    setMessage("Shot on goal!");
                }
                break;
            case KeyEvent.VK_P:
                gamePaused = !gamePaused;
                setMessage(gamePaused ? "Game paused." : "Game resumed.");
                break;
        }
    }
    private void setMessage(String msg) { this.message = msg;}
    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}

    // --- static inner classes for all game logic ---
    public static class Team {
        private String name;
        private List<Player> players;
        private Color color;
        private int side;
        public Team(String name, Color color, int side) {
            this.name = name;
            this.color = color;
            this.side = side;
            this.players = new List<Player>();
            for (int i = 0; i < 5; i++)
                this.players.add(new Player(i, color, side == 1));
        }
        public void placePlayers(int startX, int startY, int dx, int dy) {
            for (int i = 0; i < players.size(); i++) {
                int px = startX + (i % 2) * dx;
                int py = startY + (i / 2) * dy;
                players.get(i).setPosition(px, py);
            }
        }
        public void draw(Graphics g) {
            for (Player p : players) p.draw(g);
        }
        public void update(Ball ball, Team opponent) {
            for (Player p : players) p.update(ball, this, opponent);
        }
        public List<Player> getPlayers() { return players;}
        public Player getPlayer(int idx) { return players.get(idx);}
        public int getSide() { return side;}
        public String getName() { return name;}
    }

    public static class Player {
        private int id;
        private int x, y;
        private Color color;
        private boolean isLeftTeam;
        private static final int SIZE = 32;
        private int cooldown = 0;
        public Player(int id, Color color, boolean isLeftTeam) {
            this.id = id; this.color = color; this.isLeftTeam = isLeftTeam;
            this.x = 0; this.y = 0;
        }
        public void setPosition(int x, int y) { this.x = x; this.y = y;}
        public void move(int dx, int dy, Team own, Team opp, Ball ball) {
            int nx = x + dx, ny = y + dy;
            if (nx < 60 || nx > WIDTH - 60 - SIZE) return;
            if (ny < 60 || ny > HEIGHT - 60 - SIZE) return;
            x = nx; y = ny;
            if (hasBall(ball)) ball.setPosition(x + SIZE / 2, y + SIZE / 2);
        }
        public boolean hasBall(Ball ball) {
            double dist = Math.hypot(ball.getX() - (x + SIZE/2), ball.getY() - (y + SIZE/2));
            return dist < 28;
        }
        public void pass(Ball ball, Team own, Team opp) {
            Player closest = null; double cd = Double.MAX_VALUE;
            for (Player p : own.getPlayers()) {
                if (p == this) continue;
                double d = Math.hypot(p.x - x, p.y - y);
                if (d < cd) { cd = d; closest = p; }
            }
            if (closest != null)
                ball.setTarget(closest.x + SIZE / 2, closest.y + SIZE / 2, 12);
        }
        public void shoot(Ball ball) {
            int goalX = isLeftTeam ? WIDTH - 55 : 55;
            int goalY = (HEIGHT / 2);
            ball.setTarget(goalX, goalY, 18);
        }
        public void update(Ball ball, Team own, Team opponent) {
            if (cooldown > 0) cooldown--;
            if (!isLeftTeam) aiMove(ball, own, opponent);
            if (!hasBall(ball)) {
                if (Math.hypot(ball.getX() - (x + SIZE / 2), ball.getY() - (y + SIZE/2)) < 20 && cooldown <= 0) {
                    ball.setOwner(this);
                    cooldown = 10;
                }
            }
        }
        private void aiMove(Ball ball, Team own, Team opp) {
            int bx = ball.getX(), by = ball.getY();
            if (Math.random() < 0.2) return;
            if (bx > x) x += 2;
            if (bx < x) x -= 2;
            if (by > y) y += 2;
            if (by < y) y -= 2;
            if (hasBall(ball) && Math.random() < 0.03) shoot(ball);
            if (Math.hypot(ball.getX() - (x + SIZE/2), ball.getY() - (y + SIZE/2)) < 18)
                ball.setOwner(this);
        }
        public void draw(Graphics g) {
            g.setColor(color);
            g.fillOval(x, y, SIZE, SIZE);
            g.setColor(Color.BLACK);
            g.drawOval(x, y, SIZE, SIZE);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString(""+(id+1), x+SIZE/3, y+SIZE/2+6);
        }
        public int getXCenter() { return x + SIZE / 2; }
        public int getYCenter() { return y + SIZE / 2; }
    }

    public static class Ball {
        private int x, y;
        private double fx, fy;
        private Player owner;
        private double tx, ty, speed;
        private boolean moving = false;
        private static final int SIZE = 15;
        public Ball(int x, int y) {
            this.x = x; this.y = y; this.fx = x; this.fy = y;
            this.owner = null; setToCenter();
        }
        public void setToCenter() {
            fx = x = WIDTH / 2;
            fy = y = HEIGHT / 2;
            owner = null; moving = false;
        }
        public void setPosition(int nx, int ny) {
            fx = nx; fy = ny;
            x = nx; y = ny;
            owner = null; moving = false;
        }
        public void setTarget(int tx, int ty, double speed) {
            this.tx = tx; this.ty = ty; this.speed = speed;
            moving = true; owner = null;
        }
        public void setOwner(Player p) {
            this.owner = p; moving = false;
            fx = p.getXCenter(); fy = p.getYCenter();
            x = (int) fx; y = (int) fy;
        }
        public int getX() { return x;}
        public int getY() { return y;}
        public void update(Team a, Team b) {
            if (owner != null) {
                fx = owner.getXCenter();
                fy = owner.getYCenter();
                x = (int) fx; y = (int) fy;
            } else if (moving) {
                double dx = tx - fx, dy = ty - fy, dist = Math.hypot(dx, dy);
                if (dist <= speed || dist == 0) {
                    fx = tx; fy = ty; moving = false;
                } else {
                    fx += speed * dx / dist;
                    fy += speed * dy / dist;
                }
                x = (int) fx; y = (int) fy;
            }
            if (x > WIDTH - 54 && y >= HEIGHT / 2 - 60 && y <= HEIGHT / 2 + 60) setToCenter();
            if (x < 54 && y >= HEIGHT / 2 - 60 && y <= HEIGHT / 2 + 60) setToCenter();
        }
        public void draw(Graphics g) {
            g.setColor(Color.WHITE);
            g.fillOval(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
            g.setColor(Color.BLACK);
            g.drawOval(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
        }
    }

    public static class AnimationThread extends Thread {
        private final SoccerManagerGame panel;
        private final Team teamA, teamB;
        private final Ball ball;
        public AnimationThread(SoccerManagerGame panel, Team teamA, Team teamB, Ball ball) {
            this.panel = panel; this.teamA = teamA; this.teamB = teamB; this.ball = ball;
        }
        @Override
        public void run() {
            try {
                while (true) { Thread.sleep(17); }
            } catch (InterruptedException ex) { }
        }
    }
}
