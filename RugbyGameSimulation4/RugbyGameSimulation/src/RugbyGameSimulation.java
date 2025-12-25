import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class RugbyGameSimulation extends JPanel implements ActionListener, KeyListener {

    // ==============================
    // WINDOW SETTINGS
    // ==============================
    public static final int WIDTH = 1200;
    public static final int HEIGHT = 700;
    public static final int FIELD_MARGIN = 50;

    // ==============================
    // GAME LOOP
    // ==============================
    private Timer timer;
    private final int FPS = 16;

    // ==============================
    // GAME STATE
    // ==============================
    private boolean gameRunning = true;
    private boolean gameOver = false;

    // ==============================
    // TEAMS & BALL
    // ==============================
    private Team teamA;
    private Team teamB;
    private Ball ball;

    private int scoreA = 0;
    private int scoreB = 0;

    // ==============================
    // INPUT
    // ==============================
    private boolean up, down, left, right, pass, shoot;

    // ==============================
    // RANDOM
    // ==============================
    private Random random = new Random();

    // ==============================
    // CONSTRUCTOR
    // ==============================
    public RugbyGameSimulation() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(30, 130, 30));
        setFocusable(true);
        addKeyListener(this);

        initGame();

        timer = new Timer(FPS, this);
        timer.start();
    }

    // ==============================
    // INIT GAME
    // ==============================
    private void initGame() {
        teamA = new Team("Red Hawks", Color.RED, true);
        teamB = new Team("Blue Bulls", Color.BLUE, false);

        ball = new Ball(WIDTH / 2, HEIGHT / 2);

        scoreA = 0;
        scoreB = 0;

        gameOver = false;
    }

    // ==============================
    // GAME LOOP
    // ==============================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning && !gameOver) {
            updateGame();
        }
        repaint();
    }

    // ==============================
    // UPDATE LOGIC
    // ==============================
    private void updateGame() {
        handleInput();
        teamA.update();
        teamB.update();
        ball.update();

        checkCollisions();
        checkScore();
    }

    // ==============================
    // INPUT HANDLING
    // ==============================
    private void handleInput() {
        Player p = teamA.getControlledPlayer();

        if (p == null) return;

        int speed = 4;

        if (up) p.move(0, -speed);
        if (down) p.move(0, speed);
        if (left) p.move(-speed, 0);
        if (right) p.move(speed, 0);

        if (pass) {
            teamA.passBall(ball);
            pass = false;
        }

        if (shoot) {
            teamA.shoot(ball);
            shoot = false;
        }
    }

    // ==============================
    // COLLISIONS
    // ==============================
    private void checkCollisions() {
        for (Player a : teamA.players) {
            for (Player b : teamB.players) {
                if (a.getBounds().intersects(b.getBounds())) {
                    tackle(a, b);
                }
            }
        }
    }

    private void tackle(Player a, Player b) {
        if (ball.owner == a) {
            ball.owner = b;
        } else if (ball.owner == b) {
            ball.owner = a;
        }
    }

    // ==============================
    // SCORE CHECK
    // ==============================
    private void checkScore() {
        if (ball.x < FIELD_MARGIN) {
            scoreB += 5;
            resetAfterScore();
        }

        if (ball.x > WIDTH - FIELD_MARGIN) {
            scoreA += 5;
            resetAfterScore();
        }
    }

    private void resetAfterScore() {
        ball.reset(WIDTH / 2, HEIGHT / 2);
        teamA.resetPositions();
        teamB.resetPositions();
    }

    // ==============================
    // DRAWING
    // ==============================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawField(g);
        teamA.draw(g);
        teamB.draw(g);
        ball.draw(g);
        drawScore(g);
    }

    private void drawField(Graphics g) {
        g.setColor(Color.WHITE);

        g.drawRect(FIELD_MARGIN, FIELD_MARGIN,
                WIDTH - FIELD_MARGIN * 2,
                HEIGHT - FIELD_MARGIN * 2);

        g.drawLine(WIDTH / 2, FIELD_MARGIN, WIDTH / 2, HEIGHT - FIELD_MARGIN);
    }

    private void drawScore(Graphics g) {
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        g.drawString("Red Hawks: " + scoreA, 50, 30);
        g.drawString("Blue Bulls: " + scoreB, WIDTH - 200, 30);
    }

    // ==============================
    // KEYBOARD
    // ==============================
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> up = true;
            case KeyEvent.VK_DOWN -> down = true;
            case KeyEvent.VK_LEFT -> left = true;
            case KeyEvent.VK_RIGHT -> right = true;
            case KeyEvent.VK_SPACE -> pass = true;
            case KeyEvent.VK_ENTER -> shoot = true;
            case KeyEvent.VK_R -> initGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> up = false;
            case KeyEvent.VK_DOWN -> down = false;
            case KeyEvent.VK_LEFT -> left = false;
            case KeyEvent.VK_RIGHT -> right = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // ==============================
    // TEAM CLASS
    // ==============================
    class Team {
        String name;
        Color color;
        List<Player> players = new ArrayList<>();
        boolean isHuman;

        Team(String name, Color color, boolean isHuman) {
            this.name = name;
            this.color = color;
            this.isHuman = isHuman;
            createPlayers();
        }

        private void createPlayers() {
            for (int i = 0; i < 7; i++) {
                int x = isHuman ? 200 : WIDTH - 200;
                int y = 100 + i * 70;
                players.add(new Player(x, y, color));
            }

            if (isHuman) {
                ball.owner = players.get(0);
            }
        }

        void update() {
            if (!isHuman) {
                aiMove();
            }
        }

        void aiMove() {
            for (Player p : players) {
                int dx = random.nextInt(3) - 1;
                int dy = random.nextInt(3) - 1;
                p.move(dx, dy);
            }
        }

        Player getControlledPlayer() {
            return isHuman ? players.get(0) : null;
        }

        void passBall(Ball ball) {
            if (ball.owner == null) return;

            Player from = ball.owner;
            Player target = players.get(random.nextInt(players.size()));

            ball.pass(from, target);
        }

        void shoot(Ball ball) {
            if (ball.owner == null) return;

            ball.kick(isHuman ? 10 : -10, 0);
        }

        void resetPositions() {
            players.clear();
            createPlayers();
        }

        void draw(Graphics g) {
            for (Player p : players) {
                p.draw(g);
            }
        }
    }

    // ==============================
    // PLAYER CLASS
    // ==============================
    class Player {
        int x, y;
        int size = 22;
        Color color;

        Player(int x, int y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }

        void move(int dx, int dy) {
            x += dx;
            y += dy;

            x = Math.max(FIELD_MARGIN, Math.min(x, WIDTH - FIELD_MARGIN));
            y = Math.max(FIELD_MARGIN, Math.min(y, HEIGHT - FIELD_MARGIN));
        }

        Rectangle getBounds() {
            return new Rectangle(x, y, size, size);
        }

        void draw(Graphics g) {
            g.setColor(color);
            g.fillOval(x, y, size, size);
        }
    }

    // ==============================
    // BALL CLASS
    // ==============================
    class Ball {
        int x, y;
        int vx, vy;
        Player owner;

        Ball(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void update() {
            if (owner != null) {
                x = owner.x + 10;
                y = owner.y + 10;
            } else {
                x += vx;
                y += vy;
                vx *= 0.95;
                vy *= 0.95;
            }
        }

        void pass(Player from, Player to) {
            owner = null;
            vx = (to.x - from.x) / 10;
            vy = (to.y - from.y) / 10;
        }

        void kick(int dx, int dy) {
            owner = null;
            vx = dx;
            vy = dy;
        }

        void reset(int x, int y) {
            this.x = x;
            this.y = y;
            vx = 0;
            vy = 0;
            owner = null;
        }

        void draw(Graphics g) {
            g.setColor(Color.WHITE);
            g.fillOval(x, y, 12, 12);
        }
    }

    // ==============================
    // MAIN
    // ==============================
    public static void main(String[] args) {
        JFrame frame = new JFrame("Rugby Game Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new RugbyGameSimulation());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
