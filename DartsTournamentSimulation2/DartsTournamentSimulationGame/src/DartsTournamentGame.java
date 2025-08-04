// File: DartsTournamentGame.java

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class DartsTournamentGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TournamentFrame frame = new TournamentFrame();
            frame.setVisible(true);
        });
    }
}

// ------------------------- TournamentFrame.java -------------------------------

class TournamentFrame extends JFrame {
    private TournamentPanel panel;

    public TournamentFrame() {
        setTitle("Darts Tournament Simulation");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        panel = new TournamentPanel();
        add(panel);
    }
}

// ------------------------- TournamentPanel.java -------------------------------

class TournamentPanel extends JPanel implements ActionListener {
    private Timer timer;
    private List<Player> players;
    private DartsGame game;
    private DartBoard board;
    private int matchIndex;
    private boolean gameOver;

    public TournamentPanel() {
        setBackground(Color.DARK_GRAY);
        setFocusable(true);
        initGame();
        timer = new Timer(60, this);
        timer.start();
    }

    private void initGame() {
        players = Arrays.asList(
                new Player("Alice"),
                new Player("Bob"),
                new Player("Charlie"),
                new Player("Diana")
        );
        matchIndex = 0;
        board = new DartBoard(650, 200);
        nextMatch();
    }

    private void nextMatch() {
        if (matchIndex >= players.size() - 1) {
            gameOver = true;
            return;
        }
        game = new DartsGame(players.get(matchIndex), players.get(matchIndex + 1), board);
        matchIndex += 2;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver && game != null) {
            game.update();
            if (game.isFinished()) {
                nextMatch();
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        board.draw(g);

        if (gameOver) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("Tournament Finished!", 350, 100);
        } else if (game != null) {
            game.draw(g);
        }
    }
}

// ------------------------- DartBoard.java -------------------------------

class DartBoard {
    private int x, y, radius;

    public DartBoard(int x, int y) {
        this.x = x;
        this.y = y;
        this.radius = 150;
    }

    public void draw(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillOval(x, y, radius * 2, radius * 2);
        g.setColor(Color.BLACK);
        g.drawOval(x, y, radius * 2, radius * 2);

        for (int i = 1; i <= 5; i++) {
            g.drawOval(x + radius - i * 25, y + radius - i * 25, i * 50, i * 50);
        }

        for (int angle = 0; angle < 360; angle += 18) {
            double rad = Math.toRadians(angle);
            int x1 = x + radius;
            int y1 = y + radius;
            int x2 = x1 + (int)(Math.cos(rad) * radius);
            int y2 = y1 + (int)(Math.sin(rad) * radius);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    public int calculateScore(int dx, int dy) {
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 20) return 50;
        if (dist < 50) return 25;
        if (dist < 75) return 10;
        if (dist < 100) return 5;
        return 0;
    }

    public Point getCenter() {
        return new Point(x + radius, y + radius);
    }
}

// ------------------------- DartsGame.java -------------------------------

class DartsGame {
    private Player p1, p2;
    private DartBoard board;
    private boolean turnP1;
    private int turnCount;
    private Random rand;
    private boolean finished;
    private Dart currentDart;
    private List<Dart> dartHistory;

    public DartsGame(Player p1, Player p2, DartBoard board) {
        this.p1 = p1;
        this.p2 = p2;
        this.board = board;
        this.turnP1 = true;
        this.turnCount = 0;
        this.rand = new Random();
        this.finished = false;
        this.dartHistory = new ArrayList<>();
        shootNewDart();
    }

    public void update() {
        if (finished) return;

        if (currentDart.update()) {
            dartHistory.add(currentDart);
            int dx = currentDart.getX() - board.getCenter().x;
            int dy = currentDart.getY() - board.getCenter().y;
            int score = board.calculateScore(dx, dy);

            if (turnP1) p1.addScore(score);
            else p2.addScore(score);

            turnCount++;
            if (turnCount >= 6) {
                finished = true;
                return;
            }

            turnP1 = !turnP1;
            shootNewDart();
        }
    }

    private void shootNewDart() {
        Point center = board.getCenter();
        int startX = rand.nextInt(300);
        int startY = rand.nextInt(500);
        currentDart = new Dart(startX, startY, center.x, center.y);
    }

    public void draw(Graphics g) {
        for (Dart dart : dartHistory) {
            dart.draw(g);
        }

        currentDart.draw(g);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.drawString(p1.getName() + ": " + p1.getScore(), 50, 40);
        g.drawString(p2.getName() + ": " + p2.getScore(), 50, 70);

        g.drawString("Turn: " + (turnP1 ? p1.getName() : p2.getName()), 50, 100);
    }

    public boolean isFinished() {
        return finished;
    }
}

// ------------------------- Dart.java -------------------------------

class Dart {
    private int x, y;
    private int targetX, targetY;
    private int velocity = 20;
    private boolean finished;

    public Dart(int x, int y, int targetX, int targetY) {
        this.x = x;
        this.y = y;
        this.targetX = targetX;
        this.targetY = targetY;
        this.finished = false;
    }

    public boolean update() {
        if (finished) return true;

        int dx = targetX - x;
        int dy = targetY - y;

        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < velocity) {
            x = targetX;
            y = targetY;
            finished = true;
            return true;
        }

        double angle = Math.atan2(dy, dx);
        x += (int)(Math.cos(angle) * velocity);
        y += (int)(Math.sin(angle) * velocity);
        return false;
    }

    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillOval(x - 5, y - 5, 10, 10);
    }

    public int getX() { return x; }
    public int getY() { return y; }
}

// ------------------------- Player.java -------------------------------

class Player {
    private String name;
    private int score;

    public Player(String name) {
        this.name = name;
        this.score = 0;
    }

    public void addScore(int s) {
        this.score += s;
    }

    public int getScore() {
        return score;
    }

    public String getName() {
        return name;
    }
}
