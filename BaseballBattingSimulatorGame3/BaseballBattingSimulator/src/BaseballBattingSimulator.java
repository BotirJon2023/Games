import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;


public class BaseballBattingSimulator extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int GROUND_Y = 500;
    private static final int BATTER_X = 200;
    private static final int BATTER_Y = GROUND_Y - 100;
    private static final int PITCHER_X = 700;
    private static final int PITCHER_Y = GROUND_Y - 50;
    private static final int BALL_RADIUS = 10;
    private static final int BAT_LENGTH = 80;

    // Game states
    private enum GameState { READY, PITCHING, SWINGING, HIT, MISS, GAME_OVER }
    private GameState currentState = GameState.READY;

    // Game objects
    private Ball ball;
    private Batter batter;
    private Pitcher pitcher;
    private ArrayList<Ball> hitBalls = new ArrayList<>();
    private ArrayList<Ball> foulBalls = new ArrayList<>();
    private ArrayList<Cloud> clouds = new ArrayList<>();

    // Game stats
    private int score = 0;
    private int strikes = 0;
    private int balls = 0;
    private int outs = 0;
    private int inning = 1;
    private boolean topInning = true;
    private int totalHits = 0;
    private int totalSwings = 0;

    // Timing and animation
    private Timer gameTimer;
    private int animationFrame = 0;
    private int swingPower = 0;
    private boolean powerIncreasing = true;
    private boolean swingStarted = false;

    // UI elements
    private JButton startButton;
    private JLabel scoreLabel;
    private JLabel statsLabel;

    public BaseballBattingSimulator() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky blue

        // Initialize game objects
        batter = new Batter(BATTER_X, BATTER_Y);
        pitcher = new Pitcher(PITCHER_X, PITCHER_Y);

        // Create clouds for background
        for (int i = 0; i < 5; i++) {
            clouds.add(new Cloud());
        }

        // Set up UI
        setupUI();

        // Set up game timer
        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();

        // Add key listener
        setFocusable(true);
        addKeyListener(this);
    }

    private void setupUI() {
        setLayout(null);

        startButton = new JButton("Start Game");
        startButton.setBounds(WIDTH/2 - 80, HEIGHT/2 - 15, 160, 30);
        startButton.addActionListener(e -> startGame());
        add(startButton);

        scoreLabel = new JLabel("", JLabel.CENTER);
        scoreLabel.setBounds(20, 20, 200, 30);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 16));
        scoreLabel.setForeground(Color.BLACK);
        add(scoreLabel);

        statsLabel = new JLabel("", JLabel.CENTER);
        statsLabel.setBounds(WIDTH - 220, 20, 200, 60);
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        statsLabel.setForeground(Color.BLACK);
        add(statsLabel);
    }

    private void startGame() {
        currentState = GameState.READY;
        score = 0;
        strikes = 0;
        balls = 0;
        outs = 0;
        inning = 1;
        topInning = true;
        totalHits = 0;
        totalSwings = 0;
        hitBalls.clear();
        foulBalls.clear();
        startButton.setVisible(false);
        updateUI();
        requestFocus();
    }

    public void updateUI() {
        scoreLabel.setText(String.format("Score: %d | Inning: %d %s | Outs: %d",
                score, inning, topInning ? "Top" : "Bottom", outs));

        statsLabel.setText(String.format("<html>Balls: %d | Strikes: %d<br>Hits: %d/%d (%.1f%%)</html>",
                balls, strikes, totalHits, totalSwings, totalSwings > 0 ? (totalHits * 100.0 / totalSwings) : 0));
    }

    private void pitchBall() {
        currentState = GameState.PITCHING;
        ball = new Ball(PITCHER_X, PITCHER_Y, BATTER_X, BATTER_Y);
        swingPower = 0;
        powerIncreasing = true;
        swingStarted = false;
    }

    private void swingBat() {
        totalSwings++;
        swingStarted = true;
        currentState = GameState.SWINGING;
        animationFrame = 0;

        // Check if contact was made
        if (ball != null && batter.checkContact(ball, swingPower)) {
            currentState = GameState.HIT;
            totalHits++;

            // Calculate hit power and angle
            double hitPower = swingPower / 10.0;
            double hitAngle = Math.toRadians(30 + Math.random() * 30);

            // Determine if it's a foul ball
            boolean isFoul = Math.random() < 0.3;

            if (isFoul) {
                foulBalls.add(new Ball(ball.x, ball.y, hitAngle, hitPower * 0.7, Color.RED));
                strikes = Math.min(strikes + 1, 2);
            } else {
                hitBalls.add(new Ball(ball.x, ball.y, hitAngle, hitPower, Color.ORANGE));

                // Score based on hit power
                if (hitPower > 8) {
                    score += 4; // Home run
                } else if (hitPower > 6) {
                    score += 2; // Double or triple
                } else {
                    score += 1; // Single
                }
            }
        } else {
            currentState = GameState.MISS;
            strikes++;
        }

        if (strikes >= 3) {
            outs++;
            strikes = 0;
            balls = 0;

            if (outs >= 3) {
                if (topInning) {
                    topInning = false;
                } else {
                    inning++;
                    topInning = true;
                }
                outs = 0;

                if (inning > 9) {
                    currentState = GameState.GAME_OVER;
                    startButton.setText("Play Again");
                    startButton.setVisible(true);
                }
            }
        }

        updateUI();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw sky and field
        drawField(g2d);

        // Draw clouds
        for (Cloud cloud : clouds) {
            cloud.draw(g2d);
        }

        // Draw batter
        batter.draw(g2d);

        // Draw pitcher
        pitcher.draw(g2d);

        // Draw ball if in play
        if (ball != null && currentState != GameState.HIT && currentState != GameState.MISS) {
            ball.draw(g2d);
        }

        // Draw hit balls
        for (Ball hitBall : hitBalls) {
            hitBall.draw(g2d);
        }

        // Draw foul balls
        for (Ball foulBall : foulBalls) {
            foulBall.draw(g2d);
        }

        // Draw power meter if charging swing
        if (currentState == GameState.PITCHING && !swingStarted) {
            drawPowerMeter(g2d);
        }

        // Draw game messages
        drawMessages(g2d);
    }

    private void drawField(Graphics2D g2d) {
        // Draw grass
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        // Draw dirt
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect(0, GROUND_Y, WIDTH, 20);

        // Draw bases
        g2d.setColor(Color.WHITE);
        int baseSize = 20;
        g2d.fillRect(BATTER_X - baseSize/2, GROUND_Y - baseSize, baseSize, baseSize); // Home plate
        g2d.fillRect(BATTER_X + 100, GROUND_Y - 100, baseSize, baseSize); // First base
        g2d.fillRect(BATTER_X + 100, GROUND_Y - 200, baseSize, baseSize); // Second base
        g2d.fillRect(BATTER_X, GROUND_Y - 200, baseSize, baseSize); // Third base

        // Draw pitcher's mound
        g2d.setColor(new Color(205, 133, 63));
        g2d.fillOval(PITCHER_X - 30, PITCHER_Y - 30, 60, 60);
    }

    private void drawPowerMeter(Graphics2D g2d) {
        int meterWidth = 200;
        int meterHeight = 20;
        int meterX = WIDTH / 2 - meterWidth / 2;
        int meterY = 50;

        // Draw meter background
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(meterX, meterY, meterWidth, meterHeight);

        // Draw power level
        g2d.setColor(new Color(0, 128 + swingPower * 2, 0));
        g2d.fillRect(meterX, meterY, swingPower * 2, meterHeight);

        // Draw meter border
        g2d.setColor(Color.BLACK);
        g2d.drawRect(meterX, meterY, meterWidth, meterHeight);

        // Draw instructions
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Hold SPACE to charge swing, release to swing", meterX - 50, meterY + 40);
    }

    private void drawMessages(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.BLACK);

        switch (currentState) {
            case READY:
                g2d.drawString("Press SPACE to start pitch", WIDTH/2 - 150, HEIGHT/2 - 50);
                break;
            case MISS:
                g2d.drawString("STRIKE!", WIDTH/2 - 50, HEIGHT/2 - 50);
                break;
            case HIT:
                g2d.drawString("HIT!", WIDTH/2 - 30, HEIGHT/2 - 50);
                break;
            case GAME_OVER:
                g2d.drawString("GAME OVER", WIDTH/2 - 80, HEIGHT/2 - 80);
                g2d.drawString("Final Score: " + score, WIDTH/2 - 80, HEIGHT/2 - 50);
                break;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Update game objects
        if (ball != null) {
            ball.update();
        }

        // Update hit balls
        Iterator<Ball> hitIterator = hitBalls.iterator();
        while (hitIterator.hasNext()) {
            Ball hitBall = hitIterator.next();
            hitBall.update();
            if (hitBall.y > GROUND_Y) {
                hitIterator.remove();
            }
        }

        // Update foul balls
        Iterator<Ball> foulIterator = foulBalls.iterator();
        while (foulIterator.hasNext()) {
            Ball foulBall = foulIterator.next();
            foulBall.update();
            if (foulBall.y > GROUND_Y) {
                foulIterator.remove();
            }
        }

        // Update clouds
        for (Cloud cloud : clouds) {
            cloud.update();
        }

        // Update batter animation
        if (currentState == GameState.SWINGING) {
            animationFrame++;
            batter.swing(animationFrame);

            if (animationFrame > 20) {
                if (currentState == GameState.HIT || currentState == GameState.MISS) {
                    // Reset for next pitch
                    if (currentState != GameState.GAME_OVER) {
                        currentState = GameState.READY;
                    }
                    ball = null;
                }
            }
        }

        // Update power meter
        if (currentState == GameState.PITCHING && !swingStarted) {
            if (powerIncreasing) {
                swingPower += 2;
                if (swingPower >= 100) {
                    powerIncreasing = false;
                }
            } else {
                swingPower -= 2;
                if (swingPower <= 0) {
                    powerIncreasing = true;
                }
            }
        }

        // Check if pitch is complete (missed swing)
        if (ball != null && ball.y > GROUND_Y && currentState == GameState.PITCHING && !swingStarted) {
            currentState = GameState.MISS;
            strikes++;
            totalSwings++;
            updateUI();

            if (strikes >= 3) {
                outs++;
                strikes = 0;
                balls = 0;

                if (outs >= 3) {
                    if (topInning) {
                        topInning = false;
                    } else {
                        inning++;
                        topInning = true;
                    }
                    outs = 0;

                    if (inning > 9) {
                        currentState = GameState.GAME_OVER;
                        startButton.setText("Play Again");
                        startButton.setVisible(true);
                    }
                }
            }
        }

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (currentState == GameState.READY) {
                pitchBall();
            } else if (currentState == GameState.PITCHING && !swingStarted) {
                swingStarted = true;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (currentState == GameState.PITCHING && swingStarted) {
                swingBat();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Inner classes for game objects
    class Batter {
        int x, y;
        double batAngle = 45;
        boolean isSwining = false;

        public Batter(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void draw(Graphics2D g2d) {
            // Draw body
            g2d.setColor(new Color(160, 82, 45)); // Brown
            g2d.fillOval(x - 15, y - 30, 30, 30); // Head

            g2d.setColor(Color.BLUE);
            g2d.fillRect(x - 10, y, 20, 40); // Torso

            // Draw legs
            g2d.setColor(Color.BLACK);
            g2d.fillRect(x - 10, y + 40, 8, 30);
            g2d.fillRect(x + 2, y + 40, 8, 30);

            // Draw arms
            g2d.setColor(Color.BLUE);
            g2d.fillRect(x - 20, y + 5, 10, 5); // Left arm
            g2d.fillRect(x + 10, y + 5, 10, 5); // Right arm

            // Draw bat
            g2d.setColor(new Color(101, 67, 33));
            int batX = x + (int)(Math.cos(Math.toRadians(batAngle)) * BAT_LENGTH);
            int batY = y - (int)(Math.sin(Math.toRadians(batAngle)) * BAT_LENGTH);
            g2d.setStroke(new BasicStroke(5));
            g2d.drawLine(x, y, batX, batY);
        }

        public void swing(int frame) {
            if (frame < 10) {
                batAngle = 45 + frame * 9; // Swing forward
            } else {
                batAngle = 135 - (frame - 10) * 4.5; // Return to resting position
            }
        }

        public boolean checkContact(Ball ball, int power) {
            // Calculate bat position
            double batEndX = x + Math.cos(Math.toRadians(batAngle)) * BAT_LENGTH;
            double batEndY = y - Math.sin(Math.toRadians(batAngle)) * BAT_LENGTH;

            // Check distance from ball to bat
            double distToBat = Line2D.ptSegDist(x, y, batEndX, batEndY, ball.x, ball.y);

            return distToBat < BALL_RADIUS + 5 && batAngle > 60 && batAngle < 120;
        }
    }

    class Pitcher {
        int x, y;

        public Pitcher(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void draw(Graphics2D g2d) {
            // Draw body
            g2d.setColor(new Color(160, 82, 45)); // Brown
            g2d.fillOval(x - 15, y - 30, 30, 30); // Head

            g2d.setColor(Color.RED);
            g2d.fillRect(x - 10, y, 20, 40); // Torso

            // Draw legs
            g2d.setColor(Color.BLACK);
            g2d.fillRect(x - 10, y + 40, 8, 30);
            g2d.fillRect(x + 2, y + 40, 8, 30);

            // Draw arms
            g2d.setColor(Color.RED);
            g2d.fillRect(x - 20, y + 5, 10, 5); // Left arm
            g2d.fillRect(x + 10, y + 5, 10, 5); // Right arm
        }
    }

    class Ball {
        double x, y;
        double vx, vy;
        double gravity = 0.2;
        Color color;

        public Ball(double startX, double startY, double targetX, double targetY) {
            this.x = startX;
            this.y = startY;
            this.color = Color.WHITE;

            // Calculate initial velocity to reach target
            double dx = targetX - startX;
            double dy = targetY - startY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            // Adjust for gravity
            double speed = 8 + Math.random() * 4;
            double angle = Math.atan2(dy, dx);

            // Add some randomness to make it more challenging
            angle += (Math.random() - 0.5) * 0.2;

            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed;
        }

        public Ball(double x, double y, double angle, double power, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;

            this.vx = Math.cos(angle) * power;
            this.vy = -Math.sin(angle) * power;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += gravity;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.fillOval((int)(x - BALL_RADIUS), (int)(y - BALL_RADIUS), BALL_RADIUS * 2, BALL_RADIUS * 2);

            // Add stitching for baseball
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawArc((int)(x - BALL_RADIUS/2), (int)(y - BALL_RADIUS), BALL_RADIUS, BALL_RADIUS, 0, 180);
            g2d.drawArc((int)(x - BALL_RADIUS), (int)(y - BALL_RADIUS/2), BALL_RADIUS * 2, BALL_RADIUS, 0, 180);
        }
    }

    class Cloud {
        double x, y;
        double speed;
        int width, height;

        public Cloud() {
            this.x = Math.random() * WIDTH;
            this.y = 50 + Math.random() * 150;
            this.speed = 0.2 + Math.random() * 0.5;
            this.width = 60 + (int)(Math.random() * 60);
            this.height = 30 + (int)(Math.random() * 30);
        }

        public void update() {
            x += speed;
            if (x > WIDTH + width) {
                x = -width;
                y = 50 + Math.random() * 150;
            }
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int)x, (int)y, width, height);
            g2d.fillOval((int)x - width/3, (int)y + height/4, width/2, height/2);
            g2d.fillOval((int)x + width/3, (int)y + height/4, width/2, height/2);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Baseball Batting Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        BaseballBattingSimulator game = new BaseballBattingSimulator();
        frame.add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}