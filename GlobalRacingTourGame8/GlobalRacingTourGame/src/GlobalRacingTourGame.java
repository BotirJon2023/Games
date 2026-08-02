import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;

public class GlobalRacingTourGame extends JFrame {
    private GamePanel gamePanel;
    private JPanel controlPanel;
    private JButton startButton, pauseButton, resetButton;
    private JLabel statusLabel, player1Score, player2Score;
    private Timer gameTimer;
    private boolean isRunning = false;
    private boolean isPaused = false;

    public GlobalRacingTourGame() {
        setTitle("🏁 Global Racing Tour - New York");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        // Initialize game panel
        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        // Control panel
        controlPanel = new JPanel();
        controlPanel.setBackground(new Color(30, 30, 40));
        controlPanel.setPreferredSize(new Dimension(800, 80));
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));

        startButton = createStyledButton("▶ Start", new Color(0, 180, 0));
        pauseButton = createStyledButton("⏸ Pause", new Color(255, 165, 0));
        resetButton = createStyledButton("🔄 Reset", new Color(200, 50, 50));

        pauseButton.setEnabled(false);

        statusLabel = new JLabel("🏎️ Press Start to Race!");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));

        player1Score = new JLabel("Player 1: 0");
        player1Score.setForeground(Color.CYAN);
        player1Score.setFont(new Font("Arial", Font.BOLD, 14));

        player2Score = new JLabel("Player 2: 0");
        player2Score.setForeground(Color.MAGENTA);
        player2Score.setFont(new Font("Arial", Font.BOLD, 14));

        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(resetButton);
        controlPanel.add(statusLabel);
        controlPanel.add(player1Score);
        controlPanel.add(player2Score);

        add(controlPanel, BorderLayout.SOUTH);

        // Add keyboard listener
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                gamePanel.keyPressed(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                gamePanel.keyReleased(e);
            }
        });

        setFocusable(true);
        setSize(900, 700);
        setLocationRelativeTo(null);

        // Game loop timer
        gameTimer = new Timer(16, e -> {
            if (isRunning && !isPaused) {
                gamePanel.update();
                updateScores();
                repaint();
            }
        });

        // Button actions
        startButton.addActionListener(e -> startGame());
        pauseButton.addActionListener(e -> togglePause());
        resetButton.addActionListener(e -> resetGame());
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void startGame() {
        if (!isRunning) {
            isRunning = true;
            isPaused = false;
            gamePanel.startRace();
            startButton.setEnabled(false);
            pauseButton.setEnabled(true);
            statusLabel.setText("🏁 Race in Progress!");
            gameTimer.start();
            playSound("race_start.wav");
        }
    }

    private void togglePause() {
        isPaused = !isPaused;
        pauseButton.setText(isPaused ? "▶ Resume" : "⏸ Pause");
        statusLabel.setText(isPaused ? "⏸ Paused" : "🏁 Racing...");
    }

    private void resetGame() {
        isRunning = false;
        isPaused = false;
        gameTimer.stop();
        gamePanel.resetGame();
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        pauseButton.setText("⏸ Pause");
        statusLabel.setText("🔄 Game Reset");
        player1Score.setText("Player 1: 0");
        player2Score.setText("Player 2: 0");
        repaint();
    }

    private void updateScores() {
        player1Score.setText("Player 1: " + gamePanel.getPlayer1Score());
        player2Score.setText("Player 2: " + gamePanel.getPlayer2Score());
    }

    private void playSound(String filename) {
        try {
            // Simulate sound - in real implementation would load audio
            // Clip clip = AudioSystem.getClip();
            // AudioInputStream ais = AudioSystem.getAudioInputStream(getClass().getResource(filename));
            // clip.open(ais);
            // clip.start();
        } catch (Exception e) {
            // Silent fail for demo
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GlobalRacingTourGame game = new GlobalRacingTourGame();
            game.setVisible(true);
        });
    }
}

class GamePanel extends JPanel {
    private List<Car> cars;
    private List<Obstacle> obstacles;
    private List<Particle> particles;
    private Road road;
    private Building[] buildings;
    private TrafficLight trafficLight;
    private int frameCount = 0;
    private Random random = new Random();
    private boolean raceStarted = false;
    private int player1Score = 0;
    private int player2Score = 0;
    private long lastObstacleTime = 0;
    private boolean gameOver = false;

    // Car controls
    private boolean p1Up, p1Down, p1Left, p1Right;
    private boolean p2Up, p2Down, p2Left, p2Right;
    private boolean boostP1 = false, boostP2 = false;

    public GamePanel() {
        setBackground(new Color(30, 30, 35));
        setPreferredSize(new Dimension(900, 600));
        initGame();

        // Mouse listener for boost
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Click to boost - will be handled per player
            }
        });
    }

    private void initGame() {
        cars = new ArrayList<>();
        obstacles = new ArrayList<>();
        particles = new ArrayList<>();
        buildings = new Building[12];
        road = new Road();
        trafficLight = new TrafficLight(450, 100);

        // Create buildings (NYC skyline)
        String[] buildingNames = {"Empire State", "Chrysler", "One World", "Times Sq",
                "Flatiron", "Woolworth", "Bank of America", "MetLife",
                "Trump Tower", "Rockefeller", "NY Times", "Hearst"};
        for (int i = 0; i < buildings.length; i++) {
            int x = 50 + i * 70;
            int height = 80 + random.nextInt(120);
            buildings[i] = new Building(x, 600 - height, 50, height, buildingNames[i]);
        }

        // Initialize cars
        Car player1 = new Car(350, 520, 40, 60, Color.CYAN, "P1", true);
        Car player2 = new Car(450, 520, 40, 60, Color.MAGENTA, "P2", false);
        cars.add(player1);
        cars.add(player2);
    }

    public void startRace() {
        raceStarted = true;
        gameOver = false;
        for (Car car : cars) {
            car.setPosition(car == cars.get(0) ? 350 : 450, 520);
            car.setSpeed(0);
        }
        obstacles.clear();
        particles.clear();
        player1Score = 0;
        player2Score = 0;
    }

    public void resetGame() {
        raceStarted = false;
        gameOver = false;
        initGame();
        repaint();
    }

    public void update() {
        if (!raceStarted || gameOver) return;

        frameCount++;

        // Update cars
        updateCar(cars.get(0), p1Up, p1Down, p1Left, p1Right, boostP1);
        updateCar(cars.get(1), p2Up, p2Down, p2Left, p2Right, boostP2);

        // Computer AI for player 2 if not human
        if (!cars.get(1).isHuman()) {
            updateAI(cars.get(1));
        }

        // Update traffic light
        trafficLight.update();

        // Spawn obstacles
        if (System.currentTimeMillis() - lastObstacleTime > 2000 + random.nextInt(3000)) {
            spawnObstacle();
            lastObstacleTime = System.currentTimeMillis();
        }

        // Update obstacles
        Iterator<Obstacle> obsIter = obstacles.iterator();
        while (obsIter.hasNext()) {
            Obstacle obs = obsIter.next();
            obs.update();

            // Check collision with cars
            for (Car car : cars) {
                if (car.getBounds().intersects(obs.getBounds())) {
                    handleCollision(car, obs);
                    obsIter.remove();
                    break;
                }
            }

            // Remove off-screen obstacles
            if (obs.getY() > 700) {
                obsIter.remove();
            }
        }

        // Update particles
        Iterator<Particle> partIter = particles.iterator();
        while (partIter.hasNext()) {
            Particle p = partIter.next();
            p.update();
            if (p.isDead()) {
                partIter.remove();
            }
        }

        // Check race progress
        checkRaceProgress();
    }

    private void updateCar(Car car, boolean up, boolean down, boolean left, boolean right, boolean boost) {
        if (car.isHuman()) {
            double speed = car.getSpeed();
            if (up) {
                speed = Math.min(speed + 0.3, boost ? 7.0 : 5.0);
                if (boost) {
                    addExhaustParticles(car);
                }
            } else if (down) {
                speed = Math.max(speed - 0.4, -2.0);
            } else {
                speed *= 0.98; // Friction
                if (Math.abs(speed) < 0.1) speed = 0;
            }
            car.setSpeed(speed);

            if (left) {
                car.setAngle(car.getAngle() - 3);
            }
            if (right) {
                car.setAngle(car.getAngle() + 3);
            }
        }

        // Move car
        car.move();

        // Keep car within bounds
        Rectangle bounds = car.getBounds();
        if (bounds.x < 50) car.setX(50);
        if (bounds.x + bounds.width > 850) car.setX(850 - bounds.width);
        if (bounds.y < 50) car.setY(50);
        if (bounds.y + bounds.height > 570) car.setY(570 - bounds.height);
    }

    private void updateAI(Car ai) {
        // Simple AI: follow road with some randomness
        double targetSpeed = 3.0 + random.nextDouble() * 2;
        if (ai.getSpeed() < targetSpeed) {
            ai.setSpeed(ai.getSpeed() + 0.2);
        }

        // Avoid obstacles
        for (Obstacle obs : obstacles) {
            if (Math.abs(obs.getX() - ai.getX()) < 60 &&
                    obs.getY() > ai.getY() && obs.getY() < ai.getY() + 200) {
                // Swerve
                if (obs.getX() < ai.getX()) {
                    ai.setAngle(ai.getAngle() + 2);
                } else {
                    ai.setAngle(ai.getAngle() - 2);
                }
            }
        }

        // Random steering
        if (random.nextInt(100) < 5) {
            ai.setAngle(ai.getAngle() + (random.nextBoolean() ? 5 : -5));
        }

        ai.move();

        // Keep AI on road
        if (ai.getX() < 100) ai.setX(100);
        if (ai.getX() > 800) ai.setX(800);
    }

    private void spawnObstacle() {
        int type = random.nextInt(3);
        int x = 100 + random.nextInt(700);
        Obstacle obs;
        switch(type) {
            case 0:
                obs = new Obstacle(x, 0, 30, 30, Color.RED, "🚧");
                break;
            case 1:
                obs = new Obstacle(x, 0, 40, 20, Color.ORANGE, "🛑");
                break;
            default:
                obs = new Obstacle(x, 0, 25, 25, Color.YELLOW, "⭐");
                break;
        }
        obstacles.add(obs);
    }

    private void handleCollision(Car car, Obstacle obs) {
        // Create explosion particles
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(
                    obs.getX() + obs.getWidth()/2,
                    obs.getY() + obs.getHeight()/2,
                    random.nextInt(10) - 5,
                    random.nextInt(10) - 5,
                    new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)),
                    30 + random.nextInt(30)
            ));
        }

        // Apply damage and score
        if (car.isHuman()) {
            car.setSpeed(car.getSpeed() * 0.5);
            if (car.getId().equals("P1")) {
                player1Score = Math.max(0, player1Score - 10);
            } else {
                player2Score = Math.max(0, player2Score - 10);
            }
        }

        // Bonus for computer car hitting
        if (!car.isHuman()) {
            if (car.getId().equals("P2")) {
                player2Score = Math.max(0, player2Score - 5);
            }
        }

        // Check if game over
        if (player1Score < -50 || player2Score < -50) {
            gameOver = true;
            raceStarted = false;
        }
    }

    private void addExhaustParticles(Car car) {
        if (frameCount % 2 == 0) {
            double angle = Math.toRadians(car.getAngle());
            particles.add(new Particle(
                    car.getX() - 10,
                    car.getY() + car.getHeight()/2,
                    -Math.cos(angle) * 3 + random.nextDouble() * 2 - 1,
                    -Math.sin(angle) * 3 + random.nextDouble() * 2 - 1,
                    new Color(200, 200, 255, 150),
                    15 + random.nextInt(10)
            ));
        }
    }

    private void checkRaceProgress() {
        // Racing logic - first to 100 points wins
        if (player1Score >= 100) {
            gameOver = true;
            raceStarted = false;
            JOptionPane.showMessageDialog(this, "🏆 Player 1 Wins the Race! 🏆");
        } else if (player2Score >= 100) {
            gameOver = true;
            raceStarted = false;
            JOptionPane.showMessageDialog(this, "🏆 Player 2 Wins the Race! 🏆");
        }

        // Add points over time for both players
        if (frameCount % 60 == 0) { // Every second
            if (cars.get(0).getSpeed() > 1) player1Score += 1;
            if (cars.get(1).getSpeed() > 1) player2Score += 1;
        }
    }

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        // Player 1: WASD
        if (key == KeyEvent.VK_W) p1Up = true;
        if (key == KeyEvent.VK_S) p1Down = true;
        if (key == KeyEvent.VK_A) p1Left = true;
        if (key == KeyEvent.VK_D) p1Right = true;
        if (key == KeyEvent.VK_SHIFT) boostP1 = true;

        // Player 2: Arrow Keys
        if (key == KeyEvent.VK_UP) p2Up = true;
        if (key == KeyEvent.VK_DOWN) p2Down = true;
        if (key == KeyEvent.VK_LEFT) p2Left = true;
        if (key == KeyEvent.VK_RIGHT) p2Right = true;
        if (key == KeyEvent.VK_CONTROL) boostP2 = true;
    }

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W) p1Up = false;
        if (key == KeyEvent.VK_S) p1Down = false;
        if (key == KeyEvent.VK_A) p1Left = false;
        if (key == KeyEvent.VK_D) p1Right = false;
        if (key == KeyEvent.VK_SHIFT) boostP1 = false;

        if (key == KeyEvent.VK_UP) p2Up = false;
        if (key == KeyEvent.VK_DOWN) p2Down = false;
        if (key == KeyEvent.VK_LEFT) p2Left = false;
        if (key == KeyEvent.VK_RIGHT) p2Right = false;
        if (key == KeyEvent.VK_CONTROL) boostP2 = false;
    }

    public int getPlayer1Score() { return player1Score; }
    public int getPlayer2Score() { return player2Score; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw New York background
        drawBackground(g2d);

        // Draw road
        road.draw(g2d);

        // Draw buildings
        for (Building b : buildings) {
            b.draw(g2d);
        }

        // Draw traffic light
        trafficLight.draw(g2d);

        // Draw obstacles
        for (Obstacle obs : obstacles) {
            obs.draw(g2d);
        }

        // Draw cars
        for (Car car : cars) {
            car.draw(g2d);
        }

        // Draw particles
        for (Particle p : particles) {
            p.draw(g2d);
        }

        // Draw HUD
        drawHUD(g2d);

        // Draw game over overlay
        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            String msg = "🏁 RACE COMPLETE! 🏁";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
        }
    }

    private void drawBackground(Graphics2D g) {
        // Sky gradient (NYC sunset)
        GradientPaint skyGrad = new GradientPaint(0, 0, new Color(25, 25, 45),
                0, getHeight(), new Color(10, 10, 20));
        g.setPaint(skyGrad);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Stars
        g.setColor(Color.WHITE);
        for (int i = 0; i < 50; i++) {
            int x = (i * 37 + frameCount) % getWidth();
            int y = (i * 53) % 300;
            g.fillOval(x, y, 2, 2);
        }
    }

    private void drawHUD(Graphics2D g) {
        // Speed indicators
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("Player 1: " + String.format("%.1f", cars.get(0).getSpeed()), 20, 20);
        g.drawString("Player 2: " + String.format("%.1f", cars.get(1).getSpeed()), 20, 40);

        // Boost indicator
        if (boostP1) {
            g.setColor(Color.CYAN);
            g.drawString("🚀 BOOST!", 120, 20);
        }
        if (boostP2) {
            g.setColor(Color.MAGENTA);
            g.drawString("🚀 BOOST!", 120, 40);
        }
    }
}

class Car {
    private double x, y;
    private int width, height;
    private Color color;
    private String id;
    private double speed = 0;
    private double angle = 0;
    private boolean human;

    public Car(int x, int y, int width, int height, Color color, String id, boolean human) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.id = id;
        this.human = human;
    }

    public void move() {
        x += Math.cos(Math.toRadians(angle)) * speed;
        y += Math.sin(Math.toRadians(angle)) * speed;
    }

    public void draw(Graphics2D g) {
        g.translate(x + width/2, y + height/2);
        g.rotate(Math.toRadians(angle));

        // Car body
        g.setColor(color);
        g.fillRoundRect(-width/2, -height/2, width, height, 10, 10);

        // Windows
        g.setColor(Color.BLACK);
        g.fillRoundRect(-width/2 + 5, -height/2 + 5, width - 10, height/2 - 10, 5, 5);
        g.fillRoundRect(-width/2 + 5, 5, width - 10, height/2 - 10, 5, 5);

        // Headlights
        g.setColor(Color.YELLOW);
        g.fillOval(-width/2 + 2, -height/2 + 5, 6, 6);
        g.fillOval(-width/2 + 2, height/2 - 11, 6, 6);

        // Label
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString(id, -10, 5);

        g.rotate(-Math.toRadians(angle));
        g.translate(-x - width/2, -y - height/2);

        // Shadow
        g.setColor(new Color(0, 0, 0, 50));
        g.fillOval((int)x + 5, (int)y + height, width - 10, 5);
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, width, height);
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // Getters and setters
    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public double getAngle() { return angle; }
    public void setAngle(double angle) { this.angle = angle; }
    public String getId() { return id; }
    public boolean isHuman() { return human; }
}

class Road {
    private int[] lanes = {200, 300, 400, 500, 600};
    private int offset = 0;

    public void draw(Graphics2D g) {
        // Road base
        g.setColor(new Color(50, 50, 50));
        g.fillRect(50, 0, 800, 600);

        // Lane markings
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                0, new float[]{20, 15}, offset));

        for (int lane : lanes) {
            g.drawLine(lane, 0, lane, 600);
        }

        // Road edges
        g.setColor(Color.YELLOW);
        g.setStroke(new BasicStroke(4));
        g.drawLine(50, 0, 50, 600);
        g.drawLine(850, 0, 850, 600);

        // Street lights
        for (int i = 0; i < 600; i += 100) {
            g.setColor(new Color(255, 255, 100, 100));
            g.fillOval(45, i, 10, 10);
            g.fillOval(845, i, 10, 10);
        }

        // Animate road
        offset = (offset + 2) % 35;
    }
}

class Building {
    private int x, y, width, height;
    private Color color;
    private String name;
    private boolean lit;

    public Building(int x, int y, int width, int height, String name) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.name = name;
        this.color = new Color(40 + new Random().nextInt(30),
                40 + new Random().nextInt(30),
                50 + new Random().nextInt(20));
        this.lit = new Random().nextBoolean();
    }

    public void draw(Graphics2D g) {
        // Building body
        g.setColor(color);
        g.fillRect(x, y, width, height);

        // Windows
        g.setColor(lit ? new Color(255, 255, 150, 150) : new Color(100, 100, 150, 100));
        for (int wy = y + 5; wy < y + height - 5; wy += 12) {
            for (int wx = x + 5; wx < x + width - 5; wx += 12) {
                if (Math.random() < 0.7) {
                    g.fillRect(wx, wy, 6, 8);
                }
            }
        }

        // Building outline
        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);

        // Name
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 8));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(name, x + (width - fm.stringWidth(name)) / 2, y + height + 15);
    }
}

class TrafficLight {
    private int x, y;
    private int state = 0; // 0=red, 1=yellow, 2=green
    private long lastChange = 0;

    public TrafficLight(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void update() {
        if (System.currentTimeMillis() - lastChange > 3000) {
            state = (state + 1) % 3;
            lastChange = System.currentTimeMillis();
        }
    }

    public void draw(Graphics2D g) {
        g.setColor(new Color(50, 50, 50));
        g.fillRect(x - 10, y - 30, 20, 60);
        g.setColor(Color.BLACK);
        g.drawRect(x - 10, y - 30, 20, 60);

        // Lights
        Color[] colors = {Color.RED, Color.YELLOW, Color.GREEN};
        for (int i = 0; i < 3; i++) {
            g.setColor(i == state ? colors[i] : colors[i].darker());
            g.fillOval(x - 6, y - 25 + i * 20, 12, 12);
        }
    }
}

class Obstacle {
    private int x, y, width, height;
    private Color color;
    private String symbol;
    private double speed = 2 + Math.random() * 2;

    public Obstacle(int x, int y, int width, int height, Color color, String symbol) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.symbol = symbol;
    }

    public void update() {
        y += speed;
    }

    public void draw(Graphics2D g) {
        g.setColor(color);
        g.fillRoundRect(x, y, width, height, 5, 5);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString(symbol, x + width/2 - 8, y + height/2 + 5);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}

class Particle {
    private double x, y, vx, vy;
    private Color color;
    private int life;
    private int maxLife;
    private int size;

    public Particle(double x, double y, double vx, double vy, Color color, int life) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.life = life;
        this.maxLife = life;
        this.size = 3 + (int)(Math.random() * 5);
    }

    public void update() {
        x += vx;
        y += vy;
        vy += 0.1; // Gravity
        life--;
    }

    public void draw(Graphics2D g) {
        float alpha = (float)life / maxLife;
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                (int)(alpha * 255)));
        g.fillOval((int)x - size/2, (int)y - size/2, size, size);
    }

    public boolean isDead() {
        return life <= 0;
    }
}