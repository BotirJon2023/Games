import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class TableTennisGame extends JPanel implements ActionListener, KeyListener {

    // =========================================================
    // WINDOW + GAME SETTINGS
    // =========================================================
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;
    private static final int FPS_DELAY = 16;

    private JFrame frame;
    private Timer timer;

    private boolean running = true;
    private boolean paused = false;
    private boolean showMenu = true;

    // =========================================================
    // INPUT FLAGS
    // =========================================================
    private boolean upPressed;
    private boolean downPressed;
    private boolean wPressed;
    private boolean sPressed;
    private boolean spacePressed;
    private boolean escPressed;

    // =========================================================
    // GAME OBJECTS
    // =========================================================
    private Paddle player;
    private Paddle ai;
    private Ball ball;

    private int playerScore = 0;
    private int aiScore = 0;

    private Random random = new Random();

    // =========================================================
    // PARTICLES
    // =========================================================
    private List<Particle> particles = new ArrayList<>();

    // =========================================================
    // GAME STATES
    // =========================================================
    private enum GameState {
        MENU,
        PLAYING,
        PAUSED,
        GAMEOVER
    }

    private GameState state = GameState.MENU;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================
    public TableTennisGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        initGame();

        timer = new Timer(FPS_DELAY, this);
        timer.start();
    }

    // =========================================================
    // INITIALIZE GAME
    // =========================================================
    private void initGame() {
        player = new Paddle(40, HEIGHT / 2 - 60, false);
        ai = new Paddle(WIDTH - 60, HEIGHT / 2 - 60, true);
        ball = new Ball();
    }

    // =========================================================
    // MAIN METHOD
    // =========================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TableTennisGame game = new TableTennisGame();

            JFrame frame = new JFrame("Table Tennis Game - Swing Animation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.frame = frame;
        });
    }

    // =========================================================
    // GAME LOOP
    // =========================================================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            updateGame();
        }
        repaint();
    }

    // =========================================================
    // UPDATE GAME
    // =========================================================
    private void updateGame() {

        if (state == GameState.MENU) {
            if (spacePressed) {
                state = GameState.PLAYING;
                showMenu = false;
                spacePressed = false;
            }
            return;
        }

        if (state == GameState.PAUSED) {
            return;
        }

        if (state == GameState.GAMEOVER) {
            if (spacePressed) {
                resetGame();
                state = GameState.PLAYING;
                spacePressed = false;
            }
            return;
        }

        updatePlayer();
        updateAI();
        ball.update();

        checkCollisions();
        updateParticles();
    }

    // =========================================================
    // RESET GAME
    // =========================================================
    private void resetGame() {
        playerScore = 0;
        aiScore = 0;
        initGame();
        particles.clear();
    }

    // =========================================================
    // PLAYER UPDATE
    // =========================================================
    private void updatePlayer() {
        if (upPressed) {
            player.move(-player.speed);
        }
        if (downPressed) {
            player.move(player.speed);
        }
    }

    // =========================================================
    // AI UPDATE
    // =========================================================
    private void updateAI() {
        double target = ball.y - ai.height / 2;
        if (ai.y < target) {
            ai.move(ai.speed * 0.85);
        } else {
            ai.move(-ai.speed * 0.85);
        }
    }

    // =========================================================
    // COLLISIONS
    // =========================================================
    private void checkCollisions() {

        if (ball.getRect().intersects(player.getRect())) {
            ball.bounceHorizontal();
            spawnParticles(ball.x, ball.y);
        }

        if (ball.getRect().intersects(ai.getRect())) {
            ball.bounceHorizontal();
            spawnParticles(ball.x, ball.y);
        }

        if (ball.y <= 0 || ball.y + ball.size >= HEIGHT) {
            ball.bounceVertical();
            spawnParticles(ball.x, ball.y);
        }

        if (ball.x < 0) {
            aiScore++;
            ball.reset();
        }

        if (ball.x > WIDTH) {
            playerScore++;
            ball.reset();
        }

        if (playerScore >= 10 || aiScore >= 10) {
            state = GameState.GAMEOVER;
        }
    }

    // =========================================================
    // PARTICLES
    // =========================================================
    private void spawnParticles(double x, double y) {
        for (int i = 0; i < 12; i++) {
            particles.add(new Particle(x, y));
        }
    }

    private void updateParticles() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update();
            if (!p.alive) {
                it.remove();
            }
        }
    }

    // =========================================================
    // DRAW
    // =========================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        drawMiddleLine(g2);
        drawScores(g2);

        player.draw(g2);
        ai.draw(g2);
        ball.draw(g2);

        drawParticles(g2);
        drawStateOverlay(g2);
    }

    // =========================================================
    // DRAW MIDDLE LINE
    // =========================================================
    private void drawMiddleLine(Graphics2D g2) {
        g2.setColor(Color.GRAY);
        for (int i = 0; i < HEIGHT; i += 30) {
            g2.fillRect(WIDTH / 2 - 2, i, 4, 15);
        }
    }

    // =========================================================
    // DRAW SCORES
    // =========================================================
    private void drawScores(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        g2.drawString(String.valueOf(playerScore), WIDTH / 2 - 80, 50);
        g2.drawString(String.valueOf(aiScore), WIDTH / 2 + 50, 50);
    }

    // =========================================================
    // DRAW PARTICLES
    // =========================================================
    private void drawParticles(Graphics2D g2) {
        for (Particle p : particles) {
            p.draw(g2);
        }
    }

    // =========================================================
    // DRAW OVERLAY
    // =========================================================
    private void drawStateOverlay(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 26));

        if (state == GameState.MENU) {
            drawCentered(g2, "TABLE TENNIS GAME", HEIGHT / 2 - 40);
            drawCentered(g2, "Press SPACE to Start", HEIGHT / 2 + 10);
        }

        if (state == GameState.PAUSED) {
            drawCentered(g2, "PAUSED", HEIGHT / 2);
        }

        if (state == GameState.GAMEOVER) {
            String winner = playerScore > aiScore ? "YOU WIN!" : "AI WINS!";
            drawCentered(g2, winner, HEIGHT / 2 - 20);
            drawCentered(g2, "Press SPACE to Restart", HEIGHT / 2 + 30);
        }
    }

    // =========================================================
    // CENTER TEXT
    // =========================================================
    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics fm = g2.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(text)) / 2;
        g2.drawString(text, x, y);
    }

    // =========================================================
    // KEY EVENTS
    // =========================================================
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> upPressed = true;
            case KeyEvent.VK_DOWN -> downPressed = true;
            case KeyEvent.VK_W -> wPressed = true;
            case KeyEvent.VK_S -> sPressed = true;
            case KeyEvent.VK_SPACE -> spacePressed = true;
            case KeyEvent.VK_ESCAPE -> togglePause();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> upPressed = false;
            case KeyEvent.VK_DOWN -> downPressed = false;
            case KeyEvent.VK_W -> wPressed = false;
            case KeyEvent.VK_S -> sPressed = false;
            case KeyEvent.VK_SPACE -> spacePressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // =========================================================
    // PAUSE TOGGLE
    // =========================================================
    private void togglePause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    // =========================================================
    // INNER CLASS: PADDLE
    // =========================================================
    class Paddle {
        double x, y;
        int width = 14;
        int height = 120;
        double speed = 6;
        boolean isAI;

        Paddle(double x, double y, boolean isAI) {
            this.x = x;
            this.y = y;
            this.isAI = isAI;
        }

        void move(double dy) {
            y += dy;
            if (y < 0) y = 0;
            if (y + height > HEIGHT) y = HEIGHT - height;
        }

        Rectangle2D getRect() {
            return new Rectangle2D.Double(x, y, width, height);
        }

        void draw(Graphics2D g2) {
            g2.setColor(Color.WHITE);
            g2.fill(new Rectangle2D.Double(x, y, width, height));
        }
    }

    // =========================================================
    // INNER CLASS: BALL
    // =========================================================
    class Ball {
        double x, y;
        int size = 16;
        double dx, dy;

        Ball() {
            reset();
        }

        void reset() {
            x = WIDTH / 2.0;
            y = HEIGHT / 2.0;
            dx = random.nextBoolean() ? 5 : -5;
            dy = (random.nextDouble() - 0.5) * 6;
        }

        void update() {
            x += dx;
            y += dy;
        }

        void bounceHorizontal() {
            dx = -dx * 1.05;
        }

        void bounceVertical() {
            dy = -dy;
        }

        Rectangle2D getRect() {
            return new Rectangle2D.Double(x, y, size, size);
        }

        void draw(Graphics2D g2) {
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(x, y, size, size));
        }
    }

    // =========================================================
    // INNER CLASS: PARTICLE
    // =========================================================
    class Particle {
        double x, y;
        double dx, dy;
        int life = 40;
        boolean alive = true;

        Particle(double x, double y) {
            this.x = x;
            this.y = y;
            dx = (random.nextDouble() - 0.5) * 6;
            dy = (random.nextDouble() - 0.5) * 6;
        }

        void update() {
            x += dx;
            y += dy;
            life--;
            if (life <= 0) alive = false;
        }

        void draw(Graphics2D g2) {
            g2.setColor(new Color(255,255,255,Math.max(0,life*6)));
            g2.fillRect((int)x,(int)y,4,4);
        }
    }
}
