import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class BMXStuntRacing extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 700;
    private static final int GROUND_Y = 550;
    private static final double GRAVITY = 0.6;
    private static final double JUMP_POWER = -15;
    private static final double MAX_SPEED = 12;
    private static final double ACCELERATION = 0.3;
    private static final double FRICTION = 0.1;

    private BMXBiker biker;
    private ArrayList<Obstacle> obstacles;
    private ArrayList<Ramp> ramps;
    private ArrayList<Particle> particles;
    private ArrayList<Star> stars;
    private Timer timer;
    private Random random;

    private double scrollSpeed;
    private int score;
    private int bestTrick;
    private boolean gameOver;
    private boolean gamePaused;
    private int obstacleSpawnCounter;
    private int rampSpawnCounter;

    private boolean upPressed, downPressed, leftPressed, rightPressed, spacePressed;

    public BMXStuntRacing() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235));
        setFocusable(true);
        addKeyListener(this);

        random = new Random();
        biker = new BMXBiker(200, GROUND_Y);
        obstacles = new ArrayList<>();
        ramps = new ArrayList<>();
        particles = new ArrayList<>();
        stars = new ArrayList<>();

        scrollSpeed = 5;
        score = 0;
        bestTrick = 0;
        gameOver = false;
        gamePaused = false;
        obstacleSpawnCounter = 0;
        rampSpawnCounter = 0;

        timer = new Timer(16, this);
        timer.start();

        initializeObstacles();
    }

    private void initializeObstacles() {
        obstacles.add(new Obstacle(800, GROUND_Y, ObstacleType.ROCK));
        obstacles.add(new Obstacle(1200, GROUND_Y, ObstacleType.CONE));
        ramps.add(new Ramp(1500, GROUND_Y, 150, 80));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !gamePaused) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        biker.update();

        if (upPressed && !biker.isInAir()) {
            biker.jump();
        }
        if (downPressed && biker.isInAir()) {
            biker.applyDownwardForce();
        }
        if (leftPressed) {
            biker.rotateLeft();
        }
        if (rightPressed) {
            biker.rotateRight();
        }
        if (spacePressed) {
            biker.performTrick();
        }

        if (biker.getSpeed() < MAX_SPEED) {
            biker.accelerate(ACCELERATION);
        }

        scrollSpeed = biker.getSpeed();

        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obs = obstacles.get(i);
            obs.move(scrollSpeed);

            if (obs.x + obs.width < 0) {
                obstacles.remove(i);
            } else if (biker.collidesWith(obs)) {
                if (!biker.isInAir() || biker.getY() + biker.getHeight() > obs.y + 10) {
                    gameOver = true;
                }
            }
        }

        for (int i = ramps.size() - 1; i >= 0; i--) {
            Ramp ramp = ramps.get(i);
            ramp.move(scrollSpeed);

            if (ramp.x + ramp.width < 0) {
                ramps.remove(i);
            } else if (biker.isOnRamp(ramp)) {
                biker.launchFromRamp(ramp);
            }
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.isExpired()) {
                particles.remove(i);
            }
        }

        for (int i = stars.size() - 1; i >= 0; i--) {
            Star s = stars.get(i);
            s.update();
            if (s.isExpired()) {
                int trickScore = s.getValue();
                score += trickScore;
                if (trickScore > bestTrick) {
                    bestTrick = trickScore;
                }
                stars.remove(i);
            }
        }

        if (random.nextInt(100) < 2) {
            createParticle(biker.getX(), biker.getY() + biker.getHeight());
        }

        obstacleSpawnCounter++;
        rampSpawnCounter++;

        if (obstacleSpawnCounter > 100) {
            spawnObstacle();
            obstacleSpawnCounter = 0;
        }

        if (rampSpawnCounter > 200) {
            spawnRamp();
            rampSpawnCounter = 0;
        }

        score++;

        if (biker.getY() > HEIGHT) {
            gameOver = true;
        }
    }

    private void spawnObstacle() {
        ObstacleType type = random.nextBoolean() ? ObstacleType.ROCK : ObstacleType.CONE;
        obstacles.add(new Obstacle(WIDTH + 50, GROUND_Y, type));
    }

    private void spawnRamp() {
        int width = 120 + random.nextInt(80);
        int height = 60 + random.nextInt(40);
        ramps.add(new Ramp(WIDTH + 50, GROUND_Y, width, height));
    }

    private void createParticle(double x, double y) {
        particles.add(new Particle(x, y));
    }

    private void createStars(double x, double y, int value) {
        for (int i = 0; i < 5; i++) {
            stars.add(new Star(x, y, value));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2d);
        drawGround(g2d);

        for (Ramp ramp : ramps) {
            ramp.draw(g2d);
        }

        for (Obstacle obs : obstacles) {
            obs.draw(g2d);
        }

        for (Particle p : particles) {
            p.draw(g2d);
        }

        biker.draw(g2d);

        for (Star s : stars) {
            s.draw(g2d);
        }

        drawHUD(g2d);

        if (gameOver) {
            drawGameOver(g2d);
        }

        if (gamePaused) {
            drawPaused(g2d);
        }
    }

    private void drawBackground(Graphics2D g2d) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 250),
                                               0, HEIGHT / 2, new Color(255, 255, 255));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, WIDTH, HEIGHT / 2);

        g2d.setColor(new Color(255, 255, 200));
        g2d.fillOval(900, 50, 80, 80);

        g2d.setColor(new Color(34, 139, 34, 100));
        for (int i = 0; i < 5; i++) {
            int x = 100 + i * 250;
            g2d.fillOval(x, 400, 100, 150);
        }
    }

    private void drawGround(Graphics2D g2d) {
        GradientPaint ground = new GradientPaint(0, GROUND_Y, new Color(101, 67, 33),
                                                  0, HEIGHT, new Color(139, 90, 43));
        g2d.setPaint(ground);
        g2d.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        g2d.setColor(new Color(76, 187, 23));
        g2d.fillRect(0, GROUND_Y, WIDTH, 20);

        g2d.setColor(new Color(255, 255, 255));
        g2d.setStroke(new BasicStroke(3));
        for (int i = 0; i < WIDTH; i += 40) {
            g2d.drawLine(i, GROUND_Y + 25, i + 20, GROUND_Y + 25);
        }
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 250, 120, 15, 15);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Score: " + score, 25, 40);
        g2d.drawString("Speed: " + String.format("%.1f", biker.getSpeed()), 25, 70);
        g2d.drawString("Best Trick: " + bestTrick, 25, 100);
        g2d.drawString("Rotation: " + (int)biker.getRotation() + "°", 25, 130);

        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Controls: ↑Jump ←→Flip SPACE:Trick", WIDTH - 300, 30);
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        String gameOverText = "GAME OVER";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(gameOverText)) / 2;
        g2d.drawString(gameOverText, x, HEIGHT / 2 - 50);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 32));
        String scoreText = "Final Score: " + score;
        x = (WIDTH - g2d.getFontMetrics().stringWidth(scoreText)) / 2;
        g2d.drawString(scoreText, x, HEIGHT / 2 + 20);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String restartText = "Press R to Restart";
        x = (WIDTH - g2d.getFontMetrics().stringWidth(restartText)) / 2;
        g2d.drawString(restartText, x, HEIGHT / 2 + 80);
    }

    private void drawPaused(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String pauseText = "PAUSED";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(pauseText)) / 2;
        g2d.drawString(pauseText, x, HEIGHT / 2);
    }

    private void restartGame() {
        biker = new BMXBiker(200, GROUND_Y);
        obstacles.clear();
        ramps.clear();
        particles.clear();
        stars.clear();
        score = 0;
        bestTrick = 0;
        gameOver = false;
        scrollSpeed = 5;
        obstacleSpawnCounter = 0;
        rampSpawnCounter = 0;
        initializeObstacles();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP: upPressed = true; break;
            case KeyEvent.VK_DOWN: downPressed = true; break;
            case KeyEvent.VK_LEFT: leftPressed = true; break;
            case KeyEvent.VK_RIGHT: rightPressed = true; break;
            case KeyEvent.VK_SPACE: spacePressed = true; break;
            case KeyEvent.VK_P: gamePaused = !gamePaused; break;
            case KeyEvent.VK_R: if (gameOver) restartGame(); break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP: upPressed = false; break;
            case KeyEvent.VK_DOWN: downPressed = false; break;
            case KeyEvent.VK_LEFT: leftPressed = false; break;
            case KeyEvent.VK_RIGHT: rightPressed = false; break;
            case KeyEvent.VK_SPACE: spacePressed = false; break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    class BMXBiker {
        private double x, y;
        private double velocityY;
        private double speed;
        private double rotation;
        private double rotationSpeed;
        private boolean inAir;
        private int width = 50;
        private int height = 50;
        private int trickCombo;
        private boolean trickPerformed;

        public BMXBiker(double x, double y) {
            this.x = x;
            this.y = y;
            this.velocityY = 0;
            this.speed = 5;
            this.rotation = 0;
            this.rotationSpeed = 0;
            this.inAir = false;
            this.trickCombo = 0;
            this.trickPerformed = false;
        }

        public void update() {
            velocityY += GRAVITY;
            y += velocityY;

            if (y >= GROUND_Y) {
                y = GROUND_Y;
                velocityY = 0;

                if (inAir) {
                    landingEffect();
                }

                inAir = false;
                rotation = 0;
                rotationSpeed = 0;
            } else {
                inAir = true;
            }

            if (inAir) {
                rotation += rotationSpeed;
                if (rotation >= 360) rotation -= 360;
                if (rotation < 0) rotation += 360;
            }

            if (speed > 5) {
                speed -= FRICTION * 0.1;
            }
        }

        public void jump() {
            velocityY = JUMP_POWER;
            inAir = true;
            trickCombo = 0;
            trickPerformed = false;
        }

        public void applyDownwardForce() {
            velocityY += 0.5;
        }

        public void rotateLeft() {
            if (inAir) {
                rotationSpeed = -8;
                trickCombo++;
            }
        }

        public void rotateRight() {
            if (inAir) {
                rotationSpeed = 8;
                trickCombo++;
            }
        }

        public void performTrick() {
            if (inAir && !trickPerformed) {
                trickCombo += 5;
                trickPerformed = true;
            }
        }

        public void accelerate(double amount) {
            speed += amount;
        }

        public void launchFromRamp(Ramp ramp) {
            if (!inAir && y >= ramp.y - height) {
                velocityY = JUMP_POWER * 1.3;
                inAir = true;
                trickCombo = 0;
                trickPerformed = false;
            }
        }

        public boolean isOnRamp(Ramp ramp) {
            return x + width > ramp.x && x < ramp.x + ramp.width &&
                   y + height >= ramp.y - 20 && y + height <= ramp.y + 10;
        }

        private void landingEffect() {
            for (int i = 0; i < 10; i++) {
                particles.add(new Particle(x + width / 2, y + height));
            }

            int trickScore = trickCombo * 10;
            if (trickScore > 0) {
                createStars(x, y, trickScore);
            }

            trickCombo = 0;
        }

        public boolean collidesWith(Obstacle obs) {
            return x + width > obs.x && x < obs.x + obs.width &&
                   y + height > obs.y && y < obs.y + obs.height;
        }

        public void draw(Graphics2D g2d) {
            AffineTransform old = g2d.getTransform();
            g2d.translate(x + width / 2, y + height / 2);
            g2d.rotate(Math.toRadians(rotation));

            g2d.setColor(new Color(255, 69, 0));
            g2d.fillOval(-15, -5, 12, 12);
            g2d.fillOval(5, -5, 12, 12);

            g2d.setColor(new Color(64, 64, 64));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(-10, 0, 10, 0);
            g2d.drawLine(0, 0, 0, -15);
            g2d.drawLine(-10, 0, -5, 10);
            g2d.drawLine(10, 0, 5, 10);

            g2d.setColor(new Color(255, 200, 150));
            g2d.fillOval(-5, -18, 8, 8);

            g2d.setColor(new Color(0, 100, 200));
            g2d.fillRect(-4, -12, 8, 12);

            g2d.setTransform(old);
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public double getSpeed() { return speed; }
        public double getRotation() { return rotation; }
        public boolean isInAir() { return inAir; }
    }

    enum ObstacleType {
        ROCK, CONE
    }

    class Obstacle {
        double x, y;
        int width, height;
        ObstacleType type;

        public Obstacle(double x, double y, ObstacleType type) {
            this.x = x;
            this.type = type;

            if (type == ObstacleType.ROCK) {
                this.width = 40;
                this.height = 35;
                this.y = y - height;
            } else {
                this.width = 30;
                this.height = 40;
                this.y = y - height;
            }
        }

        public void move(double speed) {
            x -= speed;
        }

        public void draw(Graphics2D g2d) {
            if (type == ObstacleType.ROCK) {
                g2d.setColor(new Color(105, 105, 105));
                int[] xPoints = {(int)x, (int)x + width / 2, (int)x + width, (int)x + width / 2};
                int[] yPoints = {(int)y + height, (int)y, (int)y + height, (int)y + height - 10};
                g2d.fillPolygon(xPoints, yPoints, 4);

                g2d.setColor(new Color(80, 80, 80));
                g2d.drawPolygon(xPoints, yPoints, 4);
            } else {
                g2d.setColor(new Color(255, 140, 0));
                int[] xPoints = {(int)x + width / 2, (int)x, (int)x + width};
                int[] yPoints = {(int)y, (int)y + height, (int)y + height};
                g2d.fillPolygon(xPoints, yPoints, 3);

                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(2));
                for (int i = 0; i < 3; i++) {
                    int yPos = (int)y + 10 + i * 10;
                    g2d.drawLine((int)x + 5, yPos, (int)x + width - 5, yPos);
                }
            }
        }
    }

    class Ramp {
        double x, y;
        int width, height;

        public Ramp(double x, double y, int width, int height) {
            this.x = x;
            this.y = y - height;
            this.width = width;
            this.height = height;
        }

        public void move(double speed) {
            x -= speed;
        }

        public void draw(Graphics2D g2d) {
            GradientPaint rampGradient = new GradientPaint((float)x, (float)y,
                new Color(160, 82, 45), (float)x, (float)(y + height), new Color(139, 69, 19));
            g2d.setPaint(rampGradient);

            int[] xPoints = {(int)x, (int)x + width, (int)x + width};
            int[] yPoints = {(int)y + height, (int)y, (int)y + height};
            g2d.fillPolygon(xPoints, yPoints, 3);

            g2d.setColor(new Color(101, 67, 33));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawPolygon(xPoints, yPoints, 3);
        }
    }

    class Particle {
        double x, y;
        double velocityX, velocityY;
        int life;
        Color color;

        public Particle(double x, double y) {
            this.x = x;
            this.y = y;
            this.velocityX = (random.nextDouble() - 0.5) * 4;
            this.velocityY = -random.nextDouble() * 3;
            this.life = 30;
            this.color = new Color(139, 90, 43);
        }

        public void update() {
            x += velocityX;
            y += velocityY;
            velocityY += 0.2;
            life--;
        }

        public boolean isExpired() {
            return life <= 0;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.max(0, (int)(255 * (life / 30.0)))));
            g2d.fillOval((int)x, (int)y, 5, 5);
        }
    }

    class Star {
        double x, y;
        double velocityX, velocityY;
        int life;
        int value;

        public Star(double x, double y, int value) {
            this.x = x;
            this.y = y;
            this.velocityX = (random.nextDouble() - 0.5) * 3;
            this.velocityY = -random.nextDouble() * 5 - 2;
            this.life = 60;
            this.value = value;
        }

        public void update() {
            x += velocityX;
            y += velocityY;
            velocityY += 0.15;
            life--;
        }

        public boolean isExpired() {
            return life <= 0;
        }

        public int getValue() {
            return value;
        }

        public void draw(Graphics2D g2d) {
            int alpha = Math.max(0, (int)(255 * (life / 60.0)));
            g2d.setColor(new Color(255, 215, 0, alpha));

            int size = 15;
            int[] xPoints = new int[10];
            int[] yPoints = new int[10];

            for (int i = 0; i < 10; i++) {
                double angle = Math.PI / 5 * i;
                double radius = (i % 2 == 0) ? size : size / 2.0;
                xPoints[i] = (int)(x + Math.cos(angle) * radius);
                yPoints[i] = (int)(y + Math.sin(angle) * radius);
            }

            g2d.fillPolygon(xPoints, yPoints, 10);

            if (life > 45) {
                g2d.setColor(new Color(255, 255, 255, alpha));
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString("+" + value, (int)x + 20, (int)y);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("BMX Stunt Racing");
            BMXStuntRacing game = new BMXStuntRacing();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }
}
