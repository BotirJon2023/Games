import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;


public class HandballChampionshipGame extends JPanel implements ActionListener, KeyListener {

    // ===================== GAME CONSTANTS =====================
    public static final int WIDTH = 1200;
    public static final int HEIGHT = 700;
    public static final int FIELD_MARGIN = 60;
    public static final int GOAL_WIDTH = 20;
    public static final int GOAL_HEIGHT = 200;

    public static final int PLAYER_RADIUS = 16;
    public static final int BALL_RADIUS = 8;

    public static final int GAME_TIME_SECONDS = 300;

    // ===================== GAME STATE =====================
    private Timer timer;
    private int fps = 16;

    private int remainingTime = GAME_TIME_SECONDS;

    private int redScore = 0;
    private int blueScore = 0;

    private boolean gameRunning = true;

    private Random random = new Random();

    // ===================== ENTITIES =====================
    private List<Player> redTeam = new ArrayList<>();
    private List<Player> blueTeam = new ArrayList<>();
    private Ball ball;

    private String crowdMessage = "Welcome to the Handball Championship!";
    private int crowdMessageTimer = 0;

    // ===================== INPUT =====================
    private boolean up, down, left, right, shoot, pass;

    // ===================== CONSTRUCTOR =====================
    public HandballChampionshipGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(30, 140, 70));
        setFocusable(true);
        addKeyListener(this);

        initTeams();
        initBall();

        timer = new Timer(fps, this);
        timer.start();
    }

    // ===================== INITIALIZATION =====================
    private void initTeams() {
        // Red Team
        for (int i = 0; i < 6; i++) {
            redTeam.add(new Player(
                    FIELD_MARGIN + 200,
                    FIELD_MARGIN + 80 + i * 80,
                    Color.RED,
                    true,
                    "RED-" + (i + 1)
            ));
        }

        // Blue Team
        for (int i = 0; i < 6; i++) {
            blueTeam.add(new Player(
                    WIDTH - FIELD_MARGIN - 200,
                    FIELD_MARGIN + 80 + i * 80,
                    Color.BLUE,
                    false,
                    "BLUE-" + (i + 1)
            ));
        }
    }

    private void initBall() {
        ball = new Ball(WIDTH / 2, HEIGHT / 2);
    }

    // ===================== GAME LOOP =====================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning) return;

        updatePlayers();
        updateBall();
        checkGoals();
        updateCrowd();
        updateTimer();

        repaint();
    }

    // ===================== UPDATE LOGIC =====================
    private void updatePlayers() {
        Player controlled = redTeam.get(0);

        if (up) controlled.y -= 4;
        if (down) controlled.y += 4;
        if (left) controlled.x -= 4;
        if (right) controlled.x += 4;

        controlled.keepInField();

        if (shoot) {
            shootBall(controlled);
        }

        if (pass) {
            passBall(controlled);
        }

        for (Player p : blueTeam) {
            p.aiMove(ball);
        }

        for (Player p : redTeam) {
            if (p != controlled) {
                p.aiSupport(ball);
            }
        }
    }

    private void updateBall() {
        ball.update();
        ball.keepInField();

        for (Player p : allPlayers()) {
            if (p.hasBall(ball)) {
                ball.follow(p);
            }
        }
    }

    private void checkGoals() {
        if (ball.x < FIELD_MARGIN && ball.y > HEIGHT / 2 - GOAL_HEIGHT / 2
                && ball.y < HEIGHT / 2 + GOAL_HEIGHT / 2) {

            blueScore++;
            resetAfterGoal("GOAL FOR BLUE!");
        }

        if (ball.x > WIDTH - FIELD_MARGIN && ball.y > HEIGHT / 2 - GOAL_HEIGHT / 2
                && ball.y < HEIGHT / 2 + GOAL_HEIGHT / 2) {

            redScore++;
            resetAfterGoal("GOAL FOR RED!");
        }
    }

    private void updateCrowd() {
        if (crowdMessageTimer > 0) {
            crowdMessageTimer--;
        } else {
            if (random.nextInt(300) == 1) {
                crowdMessage = randomCrowdMessage();
                crowdMessageTimer = 120;
            }
        }
    }

    private void updateTimer() {
        if (remainingTime > 0) {
            if (timer.getDelay() == fps) {
                remainingTime--;
            }
        } else {
            gameRunning = false;
            crowdMessage = "MATCH FINISHED!";
        }
    }

    // ===================== ACTIONS =====================
    private void shootBall(Player p) {
        if (p.hasBall(ball)) {
            int dir = p.isRed ? 1 : -1;
            ball.vx = 10 * dir;
            ball.vy = random.nextInt(5) - 2;
            crowdMessage("SHOT!");
        }
    }

    private void passBall(Player p) {
        if (!p.hasBall(ball)) return;

        List<Player> team = p.isRed ? redTeam : blueTeam;
        Player target = team.get(random.nextInt(team.size()));

        ball.vx = (target.x - p.x) / 10;
        ball.vy = (target.y - p.y) / 10;

        crowdMessage("PASS!");
    }

    private void resetAfterGoal(String msg) {
        ball.reset();
        crowdMessage(msg);
    }

    // ===================== DRAWING =====================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawField(g);
        drawGoals(g);
        drawPlayers(g);
        drawBall(g);
        drawUI(g);
    }

    private void drawField(Graphics g) {
        g.setColor(Color.WHITE);
        g.drawRect(FIELD_MARGIN, FIELD_MARGIN,
                WIDTH - FIELD_MARGIN * 2,
                HEIGHT - FIELD_MARGIN * 2);

        g.drawLine(WIDTH / 2, FIELD_MARGIN, WIDTH / 2, HEIGHT - FIELD_MARGIN);
    }

    private void drawGoals(Graphics g) {
        g.setColor(Color.WHITE);

        g.fillRect(FIELD_MARGIN - GOAL_WIDTH,
                HEIGHT / 2 - GOAL_HEIGHT / 2,
                GOAL_WIDTH,
                GOAL_HEIGHT);

        g.fillRect(WIDTH - FIELD_MARGIN,
                HEIGHT / 2 - GOAL_HEIGHT / 2,
                GOAL_WIDTH,
                GOAL_HEIGHT);
    }

    private void drawPlayers(Graphics g) {
        for (Player p : allPlayers()) {
            p.draw(g);
        }
    }

    private void drawBall(Graphics g) {
        ball.draw(g);
    }

    private void drawUI(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));

        g.drawString("RED: " + redScore, 40, 30);
        g.drawString("BLUE: " + blueScore, WIDTH - 160, 30);

        g.drawString("TIME: " + remainingTime, WIDTH / 2 - 60, 30);

        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString(crowdMessage, WIDTH / 2 - 200, HEIGHT - 20);
    }

    // ===================== HELPERS =====================
    private List<Player> allPlayers() {
        List<Player> all = new ArrayList<>();
        all.addAll(redTeam);
        all.addAll(blueTeam);
        return all;
    }

    private void crowdMessage(String msg) {
        crowdMessage = msg;
        crowdMessageTimer = 90;
    }

    private String randomCrowdMessage() {
        String[] msgs = {
                "What a pass!",
                "Amazing defense!",
                "The crowd goes wild!",
                "Incredible speed!",
                "Championship level play!"
        };
        return msgs[random.nextInt(msgs.length)];
    }

    // ===================== INPUT =====================
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> up = true;
            case KeyEvent.VK_S -> down = true;
            case KeyEvent.VK_A -> left = true;
            case KeyEvent.VK_D -> right = true;
            case KeyEvent.VK_SPACE -> shoot = true;
            case KeyEvent.VK_E -> pass = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> up = false;
            case KeyEvent.VK_S -> down = false;
            case KeyEvent.VK_A -> left = false;
            case KeyEvent.VK_D -> right = false;
            case KeyEvent.VK_SPACE -> shoot = false;
            case KeyEvent.VK_E -> pass = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // ===================== INNER CLASSES =====================
    static class Player {
        int x, y;
        Color color;
        boolean isRed;
        String name;

        Player(int x, int y, Color color, boolean isRed, String name) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.isRed = isRed;
            this.name = name;
        }

        void draw(Graphics g) {
            g.setColor(color);
            g.fillOval(x - PLAYER_RADIUS, y - PLAYER_RADIUS,
                    PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);

            g.setColor(Color.WHITE);
            g.drawString(name, x - 20, y - 20);
        }

        void keepInField() {
            x = Math.max(FIELD_MARGIN, Math.min(WIDTH - FIELD_MARGIN, x));
            y = Math.max(FIELD_MARGIN, Math.min(HEIGHT - FIELD_MARGIN, y));
        }

        boolean hasBall(Ball b) {
            return distance(x, y, b.x, b.y) < PLAYER_RADIUS + BALL_RADIUS;
        }

        void aiMove(Ball b) {
            x += Integer.compare(b.x, x);
            y += Integer.compare(b.y, y);
            keepInField();
        }

        void aiSupport(Ball b) {
            if (distance(x, y, b.x, b.y) > 120) {
                x += randomStep();
                y += randomStep();
                keepInField();
            }
        }

        int randomStep() {
            return new Random().nextInt(3) - 1;
        }

        double distance(int x1, int y1, int x2, int y2) {
            return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
        }
    }

    static class Ball {
        int x, y;
        int vx = 0;
        int vy = 0;

        Ball(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void update() {
            x += vx;
            y += vy;

            vx *= 0.98;
            vy *= 0.98;
        }

        void follow(Player p) {
            x = p.x;
            y = p.y;
            vx = 0;
            vy = 0;
        }

        void keepInField() {
            x = Math.max(FIELD_MARGIN, Math.min(WIDTH - FIELD_MARGIN, x));
            y = Math.max(FIELD_MARGIN, Math.min(HEIGHT - FIELD_MARGIN, y));
        }

        void reset() {
            x = WIDTH / 2;
            y = HEIGHT / 2;
            vx = 0;
            vy = 0;
        }

        void draw(Graphics g) {
            g.setColor(Color.ORANGE);
            g.fillOval(x - BALL_RADIUS, y - BALL_RADIUS,
                    BALL_RADIUS * 2, BALL_RADIUS * 2);
        }
    }

    // ===================== MAIN =====================
    public static void main(String[] args) {
        JFrame frame = new JFrame("Handball Championship Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new HandballChampionshipGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
