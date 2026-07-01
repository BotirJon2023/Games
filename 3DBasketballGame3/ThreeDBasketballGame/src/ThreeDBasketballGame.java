import javax.swing.*;
import javax.swing.Timer; // explicit import so Timer resolves to javax.swing.Timer, not java.util.Timer
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class ThreeDBasketballGame extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final int GROUND_Y = 550;
    private static final int BASKET_HEIGHT = 180;
    private static final int BASKET_WIDTH = 120;

    private enum GameState { MENU, PLAYING, GAME_OVER }
    private enum GameMode { PLAYER_VS_PLAYER, PLAYER_VS_COMPUTER }
    private enum Difficulty { EASY, MEDIUM, HARD }

    private GameState state = GameState.MENU;
    private GameMode mode = GameMode.PLAYER_VS_PLAYER;
    private Difficulty difficulty = Difficulty.MEDIUM;

    private Player player1, player2;
    private ComputerPlayer computerPlayer;
    private Ball ball;

    private int score1 = 0, score2 = 0;
    private int maxScore = 11;
    private String winner = "";

    private Timer timer;
    private int frameCount = 0;
    private List<Particle> particles = new ArrayList<>();
    private List<Star> stars = new ArrayList<>();
    private boolean shotInProgress = false;
    private int shotTimer = 0;

    private List<CrowdMember> crowd = new ArrayList<>();
    private List<LightEffect> lights = new ArrayList<>();

    private boolean[] keys = new boolean[256];

    public ThreeDBasketballGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(20, 30, 50));
        setFocusable(true);
        addKeyListener(this);

        initGame();
        createCrowd();
        createLights();
        createStars();

        timer = new Timer(16, this);
        timer.start();
    }

    private void initGame() {
        // playerNum 1 = WASD controls, playerNum 2 = Arrow key controls
        player1 = new Player(150, GROUND_Y - 60, Color.RED, "Player 1", 1);
        player2 = new Player(WIDTH - 150, GROUND_Y - 60, Color.BLUE, "Player 2", 2);
        computerPlayer = new ComputerPlayer(WIDTH - 150, GROUND_Y - 60, Color.MAGENTA, "Computer");

        ball = new Ball(WIDTH / 2.0, GROUND_Y - 100.0);

        score1 = 0;
        score2 = 0;
        winner = "";
        shotInProgress = false;
        particles.clear();
    }

    private void createCrowd() {
        Random rand = new Random();
        for (int i = 0; i < 50; i++) {
            int x = rand.nextInt(WIDTH);
            int y = rand.nextInt(GROUND_Y - 100) + 20;
            Color color = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            crowd.add(new CrowdMember(x, y, color));
        }
    }

    private void createLights() {
        for (int i = 0; i < 8; i++) {
            lights.add(new LightEffect(50 + i * 130, 20, Color.YELLOW));
        }
    }

    private void createStars() {
        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            stars.add(new Star(rand.nextInt(WIDTH), rand.nextInt(HEIGHT / 2),
                    rand.nextFloat() * 0.5f + 0.5f));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameState.PLAYING) {
            updateGame();
        }
        repaint();
        frameCount++;
    }

    private void updateGame() {
        player1.update(keys);
        if (mode == GameMode.PLAYER_VS_PLAYER) {
            player2.update(keys);
        } else {
            computerPlayer.update(ball, difficulty);
        }

        // Ball physics always run — the original code blocked ball.update() when
        // shotInProgress was true, so the velocity set in ball.shoot() was never
        // applied and the ball never moved during a shot.
        ball.update();

        if (shotInProgress) {
            if (ball.isInBasket()) {
                int toucher = ball.getLastToucher();
                if (toucher == 1) {
                    score1++;
                    createCelebrationEffect(player1.x, player1.y);
                } else {
                    score2++;
                    int cx = (mode == GameMode.PLAYER_VS_PLAYER) ? player2.x : computerPlayer.x;
                    int cy = (mode == GameMode.PLAYER_VS_PLAYER) ? player2.y : computerPlayer.y;
                    createCelebrationEffect(cx, cy);
                }
                shotInProgress = false;
                resetBall();
            } else {
                shotTimer--;
                if (shotTimer <= 0) {
                    shotInProgress = false;
                    resetBall();
                }
            }
        }

        // Only allow a new shot when none is in progress
        if (!shotInProgress) {
            if (player1.shoot) {
                shootBall(1);
            } else if (mode == GameMode.PLAYER_VS_PLAYER && player2.shoot) {
                shootBall(2);
            } else if (mode == GameMode.PLAYER_VS_COMPUTER && computerPlayer.shoot) {
                shootBall(2);
            }
        }

        if (score1 >= maxScore || score2 >= maxScore) {
            state = GameState.GAME_OVER;
            if (score1 >= maxScore) winner = "Player 1";
            else if (mode == GameMode.PLAYER_VS_PLAYER) winner = "Player 2";
            else winner = "Computer";
        }

        updateParticles();
        for (CrowdMember c : crowd) c.update(frameCount);
    }

    private void shootBall(int player) {
        shotInProgress = true;
        shotTimer = 90; // 1.5 seconds at 60 fps — gives ball time to reach the basket
        ball.shoot(player, player == 1 ? 1 : -1);
        ball.setLastToucher(player);
        player1.shoot = false;
        player2.shoot = false;
        computerPlayer.shoot = false;
    }

    private void resetBall() {
        ball.x = WIDTH / 2.0;
        ball.y = GROUND_Y - 100.0;
        ball.vx = 0;
        ball.vy = 0;
        ball.inBasket = false;
        shotInProgress = false;
    }

    private void createCelebrationEffect(int x, int y) {
        Random rand = new Random();
        for (int i = 0; i < 30; i++) {
            particles.add(new Particle(
                    x + rand.nextInt(40) - 20,
                    y + rand.nextInt(40) - 20,
                    rand.nextDouble() * 6 - 3,
                    rand.nextDouble() * 6 - 3,
                    new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256))));
        }
    }

    private void updateParticles() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update();
            if (p.isDead()) it.remove();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (state == GameState.MENU) drawMenu(g2d);
        else if (state == GameState.PLAYING) drawGame(g2d);
        else if (state == GameState.GAME_OVER) drawGameOver(g2d);
    }

    private void drawMenu(Graphics2D g2d) {
        GradientPaint gp = new GradientPaint(0, 0, new Color(10, 20, 40),
                WIDTH, HEIGHT, new Color(30, 50, 80));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        for (Star s : stars) s.draw(g2d);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String title = "3D BASKETBALL";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, 150);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.setColor(Color.YELLOW);
        String sub = "SYDNEY STADIUM EDITION";
        fm = g2d.getFontMetrics();
        g2d.drawString(sub, (WIDTH - fm.stringWidth(sub)) / 2, 190);

        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        String[] options = {"1. Player vs Player", "2. Player vs Computer",
                "3. Difficulty: " + difficulty, "4. Start Game"};
        Color[] colors = {Color.CYAN, Color.GREEN, Color.ORANGE, Color.RED};
        for (int i = 0; i < options.length; i++) {
            g2d.setColor(colors[i]);
            g2d.drawString(options[i], 300, 280 + i * 50);
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Press 1-4 to select, ENTER to start", 300, 550);
    }

    private void drawGame(Graphics2D g2d) {
        GradientPaint skyGrad = new GradientPaint(0, 0, new Color(10, 20, 50),
                0, HEIGHT / 2, new Color(30, 60, 100));
        g2d.setPaint(skyGrad);
        g2d.fillRect(0, 0, WIDTH, HEIGHT / 2);

        for (Star s : stars) s.draw(g2d);

        drawStadium(g2d);

        g2d.setColor(new Color(40, 80, 40));
        g2d.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        g2d.setColor(new Color(60, 120, 60));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(0, GROUND_Y, WIDTH, GROUND_Y);
        g2d.drawLine(WIDTH / 2, GROUND_Y, WIDTH / 2, HEIGHT);
        g2d.drawOval(WIDTH / 2 - 100, GROUND_Y + 10, 200, 60);

        drawHoop(g2d);

        for (CrowdMember c : crowd) c.draw(g2d);

        player1.draw(g2d);
        if (mode == GameMode.PLAYER_VS_PLAYER) player2.draw(g2d);
        else computerPlayer.draw(g2d);

        ball.draw(g2d);

        for (Particle p : particles) p.draw(g2d);

        drawScoreboard(g2d);

        for (LightEffect l : lights) l.draw(g2d);

        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("P1: A/D Move, W Shoot  |  P2: Left/Right Move, Up Shoot", 20, HEIGHT - 20);
    }

    private void drawStadium(Graphics2D g2d) {
        int[] xPoints = {100, 900, 950, 50};
        int[] yPoints = {200, 200, GROUND_Y, GROUND_Y};
        g2d.setColor(new Color(60, 70, 90, 100));
        g2d.fillPolygon(xPoints, yPoints, 4);

        g2d.setColor(new Color(80, 90, 110, 150));
        g2d.fillArc(50, 180, 900, 100, 0, 180);

        for (int i = 0; i < 6; i++) {
            int x = 150 + i * 140;
            g2d.setColor(Color.YELLOW);
            g2d.fillOval(x, 180, 20, 20);
            g2d.setColor(new Color(255, 255, 0, 50));
            g2d.fillOval(x - 30, 180, 80, 40);
        }

        g2d.setColor(new Color(30, 40, 60, 100));
        for (int i = 0; i < 5; i++) {
            int x = 200 + i * 120;
            int[] operaX = {x, x + 30, x + 60, x + 30};
            int[] operaY = {GROUND_Y, GROUND_Y - 40 - i * 10, GROUND_Y, GROUND_Y - 20};
            g2d.fillPolygon(operaX, operaY, 4);
        }
    }

    private void drawHoop(Graphics2D g2d) {
        int hoopX = WIDTH / 2 - BASKET_WIDTH / 2;
        int hoopY = GROUND_Y - BASKET_HEIGHT;

        g2d.setColor(new Color(200, 200, 220, 180));
        g2d.fillRect(hoopX + 40, hoopY - 20, 40, 80);
        g2d.setColor(new Color(150, 150, 170, 100));
        g2d.fillRect(hoopX + 45, hoopY - 10, 30, 60);

        g2d.setColor(Color.ORANGE);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawArc(hoopX, hoopY + 40, BASKET_WIDTH, 20, 0, 180);

        g2d.setColor(new Color(200, 200, 200, 100));
        g2d.setStroke(new BasicStroke(1));
        for (int i = 0; i < 6; i++) {
            int x = hoopX + 20 + i * 16;
            g2d.drawLine(x, hoopY + 50, x + 5, hoopY + 70);
            g2d.drawLine(x + 5, hoopY + 70, x + 3, hoopY + 85);
        }

        g2d.setColor(Color.GRAY);
        g2d.fillRect(hoopX + 55, hoopY + 60, 10, GROUND_Y - hoopY - 60);
    }

    private void drawScoreboard(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRoundRect(WIDTH / 2 - 200, 10, 400, 60, 20, 20);
        g2d.setColor(new Color(255, 255, 255, 50));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(WIDTH / 2 - 200, 10, 400, 60, 20, 20);

        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.setColor(Color.RED);
        g2d.drawString(String.valueOf(score1), WIDTH / 2 - 100, 55);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("VS", WIDTH / 2 - 30, 50);

        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.setColor(mode == GameMode.PLAYER_VS_PLAYER ? Color.BLUE : Color.MAGENTA);
        g2d.drawString(String.valueOf(score2), WIDTH / 2 + 70, 55);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Difficulty: " + difficulty, WIDTH - 150, 40);
        g2d.drawString(mode == GameMode.PLAYER_VS_PLAYER ? "2 Players" : "vs Computer", WIDTH - 150, 60);
    }

    private void drawGameOver(Graphics2D g2d) {
        drawGame(g2d);

        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        String winText = winner + " WINS!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(winText, (WIDTH - fm.stringWidth(winText)) / 2, HEIGHT / 2 - 50);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String restart = "Press SPACE to restart or ESC to menu";
        fm = g2d.getFontMetrics();
        g2d.drawString(restart, (WIDTH - fm.stringWidth(restart)) / 2, HEIGHT / 2 + 50);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key < keys.length) keys[key] = true;

        if (state == GameState.MENU) {
            handleMenuKey(key);
        } else if (state == GameState.GAME_OVER) {
            if (key == KeyEvent.VK_SPACE) { state = GameState.PLAYING; initGame(); }
            else if (key == KeyEvent.VK_ESCAPE) { state = GameState.MENU; }
        }
    }

    private void handleMenuKey(int key) {
        if (key == KeyEvent.VK_1) {
            mode = GameMode.PLAYER_VS_PLAYER;
        } else if (key == KeyEvent.VK_2) {
            mode = GameMode.PLAYER_VS_COMPUTER;
        } else if (key == KeyEvent.VK_3) {
            switch (difficulty) {
                case EASY:   difficulty = Difficulty.MEDIUM; break;
                case MEDIUM: difficulty = Difficulty.HARD;   break;
                case HARD:   difficulty = Difficulty.EASY;   break;
            }
        } else if (key == KeyEvent.VK_4 || key == KeyEvent.VK_ENTER) {
            state = GameState.PLAYING;
            initGame();
        }
    }

    @Override public void keyReleased(KeyEvent e) { if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = false; }
    @Override public void keyTyped(KeyEvent e) {}

    // -------------------------------------------------------------------------
    // Inner classes
    // -------------------------------------------------------------------------

    class Player {
        int x, y;
        int speed = 4;
        Color color;
        String name;
        int playerNum; // 1 = WASD, 2 = Arrow keys
        boolean shoot = false;
        int armAngle = 0;

        Player(int x, int y, Color color, String name, int playerNum) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.name = name;
            this.playerNum = playerNum;
        }

        void update(boolean[] keys) {
            // Each player only reads its own control keys so the two players
            // don't interfere with each other in 2-player mode.
            boolean moveLeft  = (playerNum == 1) ? keys[KeyEvent.VK_A]    : keys[KeyEvent.VK_LEFT];
            boolean moveRight = (playerNum == 1) ? keys[KeyEvent.VK_D]    : keys[KeyEvent.VK_RIGHT];
            boolean doShoot   = (playerNum == 1) ? keys[KeyEvent.VK_W]    : keys[KeyEvent.VK_UP];

            if (moveLeft) {
                x -= speed;
                armAngle = Math.max(armAngle - 5, -30);
            } else if (moveRight) {
                x += speed;
                armAngle = Math.min(armAngle + 5, 30);
            } else {
                armAngle = (int)(armAngle * 0.9);
            }

            if (doShoot) shoot = true;

            x = Math.max(30, Math.min(WIDTH - 30, x));
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillOval(x - 15, GROUND_Y - 5, 30, 10);

            g2d.setColor(color);
            g2d.fillRoundRect(x - 15, y - 40, 30, 40, 10, 10);
            g2d.fillOval(x - 12, y - 55, 24, 24);

            g2d.setColor(Color.WHITE);
            g2d.fillOval(x - 8, y - 50, 6, 6);
            g2d.fillOval(x + 2, y - 50, 6, 6);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x - 6, y - 48, 3, 3);
            g2d.fillOval(x + 4, y - 48, 3, 3);

            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(6));
            g2d.drawLine(x - 10, y - 30, x - 20 + armAngle / 3, y - 15);
            g2d.drawLine(x + 10, y - 30, x + 20 + armAngle / 3, y - 15);

            g2d.setStroke(new BasicStroke(8));
            g2d.drawLine(x - 8, y, x - 12, y + 20);
            g2d.drawLine(x + 8, y, x + 12, y + 20);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            g2d.drawString(name, x - 15, y - 70);
        }
    }

    class ComputerPlayer extends Player {
        Random rand = new Random();
        int decisionTimer = 0;
        double accuracy = 0.7;

        ComputerPlayer(int x, int y, Color color, String name) {
            super(x, y, color, name, 2);
        }

        void update(Ball ball, Difficulty diff) {
            switch (diff) {
                case EASY:   speed = 2; accuracy = 0.4; break;
                case MEDIUM: speed = 3; accuracy = 0.7; break;
                case HARD:   speed = 5; accuracy = 0.9; break;
            }

            int targetX = (int)ball.x;
            targetX += (int)((rand.nextDouble() - 0.5) * 100 * (1 - accuracy));

            if (Math.abs(x - targetX) > 20) {
                if (x < targetX) x += speed;
                else x -= speed;
            }

            decisionTimer--;
            if (decisionTimer <= 0) {
                decisionTimer = 20 + rand.nextInt(40);
                if (rand.nextDouble() < accuracy / 2 && !shotInProgress) {
                    shoot = true;
                }
            }

            x = Math.max(30, Math.min(WIDTH - 30, x));
        }
    }

    class Ball {
        // Use double so fractional velocities accumulate correctly instead of
        // being silently truncated to zero each frame (was int x, y before).
        double x, y;
        double vx = 0, vy = 0;
        int radius = 15;
        double rotation = 0;
        boolean inBasket = false;
        int lastToucher = 0;
        final double gravity = 0.3;
        final double friction = 0.99;

        Ball(double x, double y) {
            this.x = x;
            this.y = y;
        }

        void shoot(int player, int direction) {
            vx = direction * 5 + (Math.random() - 0.5) * 2;
            vy = -12 - Math.random() * 2;
            rotation = direction * 10;
        }

        void update() {
            if (!inBasket) {
                x += vx;
                y += vy;
                vy += gravity;
                vx *= friction;

                if (x - radius < 0 || x + radius > WIDTH) {
                    vx *= -0.8;
                    x = Math.max(radius, Math.min(WIDTH - radius, x));
                }

                if (y + radius > GROUND_Y) {
                    y = GROUND_Y - radius;
                    vy *= -0.5;
                    if (Math.abs(vy) < 0.5) vy = 0;
                }

                // Basket detection
                int hoopX = WIDTH / 2 - BASKET_WIDTH / 2;
                int hoopY = GROUND_Y - BASKET_HEIGHT;
                if (x > hoopX + 20 && x < hoopX + BASKET_WIDTH - 20 &&
                        y > hoopY + 30 && y < hoopY + 70) {
                    inBasket = true;
                }
            }

            rotation += vx;
        }

        boolean isInBasket() { return inBasket; }
        void setLastToucher(int player) { lastToucher = player; }
        int getLastToucher() { return lastToucher; }

        void draw(Graphics2D g2d) {
            int ix = (int)x, iy = (int)y;

            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.fillOval(ix - radius + 5, GROUND_Y - 5, radius * 2, 8);

            RadialGradientPaint rgp = new RadialGradientPaint(
                    ix - 5, iy - 5, radius,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{Color.ORANGE, new Color(200, 100, 0), new Color(150, 70, 0)});
            g2d.setPaint(rgp);
            g2d.fillOval(ix - radius, iy - radius, radius * 2, radius * 2);

            g2d.setColor(new Color(100, 50, 0, 80));
            g2d.setStroke(new BasicStroke(1));
            for (int i = 0; i < 3; i++) {
                double angle = Math.toRadians(i * 60 + rotation);
                int x1 = (int)(ix + Math.cos(angle) * radius * 0.8);
                int y1 = (int)(iy + Math.sin(angle) * radius * 0.8);
                int x2 = (int)(ix + Math.cos(angle + Math.PI) * radius * 0.8);
                int y2 = (int)(iy + Math.sin(angle + Math.PI) * radius * 0.8);
                g2d.drawLine(x1, y1, x2, y2);
            }

            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.fillOval(ix - 5, iy - 8, 6, 6);
        }
    }

    class Particle {
        double x, y, vx, vy;
        Color color;
        int life = 60;

        Particle(double x, double y, double vx, double vy, Color color) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.color = color;
        }

        void update() { x += vx; y += vy; vy += 0.1; life--; }
        boolean isDead() { return life <= 0; }

        void draw(Graphics2D g2d) {
            int alpha = life * 255 / 60;
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            int size = 4 + (60 - life) / 30;
            g2d.fillOval((int)x - size / 2, (int)y - size / 2, size, size);
        }
    }

    class CrowdMember {
        int x, y;
        Color color;
        int animationOffset;

        CrowdMember(int x, int y, Color color) {
            this.x = x; this.y = y; this.color = color;
            this.animationOffset = new Random().nextInt(100);
        }

        void update(int frame) {}

        void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.fillRect(x - 3, y, 6, 10);
            g2d.fillOval(x - 4, y - 6, 8, 8);
            if ((frameCount + animationOffset) % 120 < 60) {
                g2d.setColor(color.darker());
                g2d.drawLine(x + 4, y - 2, x + 10, y - 6);
            }
        }
    }

    class LightEffect {
        int x, y;
        Color color;
        int phase;

        LightEffect(int x, int y, Color color) {
            this.x = x; this.y = y; this.color = color;
            this.phase = new Random().nextInt(100);
        }

        void draw(Graphics2D g2d) {
            float alpha = 0.5f + 0.3f * (float)Math.sin((frameCount + phase) * 0.05);
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255)));
            g2d.fillOval(x - 20, y - 20, 40, 40);
            g2d.setColor(new Color(255, 255, 200, (int)(alpha * 30)));
            g2d.fillOval(x - 60, y + 20, 120, 80);
        }
    }

    class Star {
        int x, y;
        float brightness;

        Star(int x, int y, float brightness) {
            this.x = x; this.y = y; this.brightness = brightness;
        }

        void draw(Graphics2D g2d) {
            float b = brightness * (0.7f + 0.3f * (float)Math.sin(frameCount * 0.02 + x));
            g2d.setColor(new Color(255, 255, 255, (int)(b * 200)));
            g2d.fillOval(x, y, 2, 2);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3D Basketball Game - Sydney Stadium");
            ThreeDBasketballGame game = new ThreeDBasketballGame();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
            game.requestFocus();
        });
    }
}
