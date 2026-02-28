import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class VolleyballGameSimulation extends JPanel implements ActionListener, KeyListener {

    // ==============================
    // WINDOW SETTINGS
    // ==============================

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 700;

    private static final int GROUND_HEIGHT = 100;
    private static final int NET_WIDTH = 20;
    private static final int NET_HEIGHT = 250;

    private static final int PLAYER_WIDTH = 60;
    private static final int PLAYER_HEIGHT = 120;

    private static final int BALL_SIZE = 30;

    // ==============================
    // GAME VARIABLES
    // ==============================

    private Timer timer;
    private boolean running = true;

    private Player leftPlayer;
    private Player rightPlayer;
    private Ball ball;
    private Net net;

    private int leftScore = 0;
    private int rightScore = 0;

    private boolean gameOver = false;

    private Random random = new Random();

    // ==============================
    // CONSTRUCTOR
    // ==============================

    public VolleyballGameSimulation() {

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235));
        setFocusable(true);
        addKeyListener(this);

        initializeGame();

        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    // ==============================
    // GAME INITIALIZATION
    // ==============================

    private void initializeGame() {

        leftPlayer = new Player(
                200,
                HEIGHT - GROUND_HEIGHT - PLAYER_HEIGHT,
                Color.RED
        );

        rightPlayer = new Player(
                WIDTH - 260,
                HEIGHT - GROUND_HEIGHT - PLAYER_HEIGHT,
                Color.BLUE
        );

        ball = new Ball(
                WIDTH / 2,
                HEIGHT / 3
        );

        net = new Net(
                WIDTH / 2 - NET_WIDTH / 2,
                HEIGHT - GROUND_HEIGHT - NET_HEIGHT
        );
    }

    // ==============================
    // GAME LOOP
    // ==============================

    @Override
    public void actionPerformed(ActionEvent e) {

        if (!gameOver) {

            updateGame();
            repaint();
        }
    }

    // ==============================
    // UPDATE GAME LOGIC
    // ==============================

    private void updateGame() {

        leftPlayer.update();
        rightPlayer.update();
        ball.update();

        handleBallCollisions();
        checkScore();
    }

    // ==============================
    // COLLISION HANDLING
    // ==============================

    private void handleBallCollisions() {

        // Ground collision
        if (ball.y + BALL_SIZE >= HEIGHT - GROUND_HEIGHT) {

            if (ball.x < WIDTH / 2) {
                rightScore++;
            } else {
                leftScore++;
            }

            resetRound();
            return;
        }

        // Wall collisions
        if (ball.x <= 0 || ball.x + BALL_SIZE >= WIDTH) {
            ball.velocityX *= -1;
        }

        // Net collision
        if (ball.getBounds().intersects(net.getBounds())) {
            ball.velocityX *= -1;
        }

        // Player collisions
        if (ball.getBounds().intersects(leftPlayer.getBounds())) {
            ball.velocityY = -15;
            ball.velocityX = -8 + random.nextInt(16);
        }

        if (ball.getBounds().intersects(rightPlayer.getBounds())) {
            ball.velocityY = -15;
            ball.velocityX = -8 + random.nextInt(16);
        }
    }

    // ==============================
    // SCORE CHECK
    // ==============================

    private void checkScore() {

        if (leftScore >= 10 || rightScore >= 10) {
            gameOver = true;
        }
    }

    // ==============================
    // RESET ROUND
    // ==============================

    private void resetRound() {

        ball.x = WIDTH / 2;
        ball.y = HEIGHT / 3;

        ball.velocityX = random.nextBoolean() ? 6 : -6;
        ball.velocityY = -10;
    }

    // ==============================
    // RENDERING
    // ==============================

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        drawGround(g2);
        net.draw(g2);
        leftPlayer.draw(g2);
        rightPlayer.draw(g2);
        ball.draw(g2);

        drawScore(g2);

        if (gameOver) {
            drawGameOver(g2);
        }
    }

    private void drawGround(Graphics2D g2) {

        g2.setColor(new Color(34, 139, 34));
        g2.fillRect(0, HEIGHT - GROUND_HEIGHT, WIDTH, GROUND_HEIGHT);
    }

    private void drawScore(Graphics2D g2) {

        g2.setFont(new Font("Arial", Font.BOLD, 40));
        g2.setColor(Color.BLACK);

        g2.drawString(String.valueOf(leftScore), WIDTH / 4, 80);
        g2.drawString(String.valueOf(rightScore), WIDTH * 3 / 4, 80);
    }

    private void drawGameOver(Graphics2D g2) {

        g2.setFont(new Font("Arial", Font.BOLD, 60));
        g2.setColor(Color.BLACK);

        String text = leftScore > rightScore ? "Left Player Wins!" : "Right Player Wins!";
        g2.drawString(text, WIDTH / 2 - 250, HEIGHT / 2);
    }

    // ==============================
    // KEY LISTENER
    // ==============================

    @Override
    public void keyPressed(KeyEvent e) {

        int key = e.getKeyCode();

        // Left Player
        if (key == KeyEvent.VK_A) {
            leftPlayer.moveLeft = true;
        }
        if (key == KeyEvent.VK_D) {
            leftPlayer.moveRight = true;
        }
        if (key == KeyEvent.VK_W) {
            leftPlayer.jump();
        }

        // Right Player
        if (key == KeyEvent.VK_LEFT) {
            rightPlayer.moveLeft = true;
        }
        if (key == KeyEvent.VK_RIGHT) {
            rightPlayer.moveRight = true;
        }
        if (key == KeyEvent.VK_UP) {
            rightPlayer.jump();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

        int key = e.getKeyCode();

        if (key == KeyEvent.VK_A) {
            leftPlayer.moveLeft = false;
        }
        if (key == KeyEvent.VK_D) {
            leftPlayer.moveRight = false;
        }
        if (key == KeyEvent.VK_LEFT) {
            rightPlayer.moveLeft = false;
        }
        if (key == KeyEvent.VK_RIGHT) {
            rightPlayer.moveRight = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // ==============================
    // PLAYER CLASS
    // ==============================

    class Player {

        int x, y;
        int velocityY = 0;

        boolean moveLeft = false;
        boolean moveRight = false;

        boolean onGround = true;

        Color color;

        public Player(int x, int y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }

        public void update() {

            if (moveLeft && x > 0) {
                x -= 7;
            }

            if (moveRight && x + PLAYER_WIDTH < WIDTH) {
                x += 7;
            }

            // Gravity
            velocityY += 1;
            y += velocityY;

            if (y >= HEIGHT - GROUND_HEIGHT - PLAYER_HEIGHT) {
                y = HEIGHT - GROUND_HEIGHT - PLAYER_HEIGHT;
                velocityY = 0;
                onGround = true;
            }
        }

        public void jump() {
            if (onGround) {
                velocityY = -18;
                onGround = false;
            }
        }

        public void draw(Graphics2D g2) {

            g2.setColor(color);
            g2.fillRect(x, y, PLAYER_WIDTH, PLAYER_HEIGHT);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, PLAYER_WIDTH, PLAYER_HEIGHT);
        }
    }

    // ==============================
    // BALL CLASS
    // ==============================

    class Ball {

        int x, y;
        int velocityX = 6;
        int velocityY = -8;

        public Ball(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void update() {

            velocityY += 1;
            x += velocityX;
            y += velocityY;
        }

        public void draw(Graphics2D g2) {

            g2.setColor(Color.WHITE);
            g2.fillOval(x, y, BALL_SIZE, BALL_SIZE);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, BALL_SIZE, BALL_SIZE);
        }
    }

    // ==============================
    // NET CLASS
    // ==============================

    class Net {

        int x, y;

        public Net(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void draw(Graphics2D g2) {

            g2.setColor(Color.BLACK);
            g2.fillRect(x, y, NET_WIDTH, NET_HEIGHT);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, NET_WIDTH, NET_HEIGHT);
        }
    }

    // ==============================
    // MAIN METHOD
    // ==============================

    public static void main(String[] args) {

        JFrame frame = new JFrame("Volleyball Game Simulation");
        VolleyballGameSimulation game = new VolleyballGameSimulation();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}