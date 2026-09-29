import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class SupercarRacingChallenge extends JFrame {

    public SupercarRacingChallenge() {
        setTitle("Supercar Racing Challenge");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        pack();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SupercarRacingChallenge::new);
    }
}

class GamePanel extends JPanel implements ActionListener {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final int FPS = 60;
    private static final int FINISH_DISTANCE = 50000; // Finish line distance

    private enum GameState { MENU, PLAYING, GAME_OVER }
    private GameState state = GameState.MENU;

    private boolean isVsAI = true;

    // Game Loop
    private final Timer gameLoopTimer;

    // Keys pressed state
    private final boolean[] keys = new boolean[256];

    // Track state
    private double trackOffset = 0;

    // Entities
    private Car player1;
    private Car player2;
    private List<RoadSideObject> roadSideObjects;

    private String winnerText = "";

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(30, 30, 30));
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = true;
                handleMenuKeys(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = false;
            }
        });

        gameLoopTimer = new Timer(1000 / FPS, this);
        gameLoopTimer.start();

        initGame();
    }

    private void initGame() {
        // Player 1 (Red Supercar): Left Track Lane
        player1 = new Car(WIDTH / 4 - 30, HEIGHT - 180, Color.RED, "Player 1", false);
        // Player 2 / AI (Blue Supercar): Right Track Lane
        player2 = new Car((3 * WIDTH) / 4 - 30, HEIGHT - 180, Color.CYAN, isVsAI ? "AI Racer" : "Player 2", isVsAI);

        // Populate roadside trees and lights
        roadSideObjects = new ArrayList<>();
        for (int i = -100; i < HEIGHT + 100; i += 120) {
            roadSideObjects.add(new RoadSideObject(30, i));
            roadSideObjects.add(new RoadSideObject(WIDTH - 70, i));
        }
    }

    private void handleMenuKeys(KeyEvent e) {
        if (state == GameState.MENU) {
            if (e.getKeyCode() == KeyEvent.VK_1) {
                isVsAI = true;
                initGame();
                state = GameState.PLAYING;
            } else if (e.getKeyCode() == KeyEvent.VK_2) {
                isVsAI = false;
                initGame();
                state = GameState.PLAYING;
            }
        } else if (state == GameState.GAME_OVER) {
            if (e.getKeyCode() == KeyEvent.VK_R) {
                initGame();
                state = GameState.PLAYING;
            } else if (e.getKeyCode() == KeyEvent.VK_M) {
                state = GameState.MENU;
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameState.PLAYING) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        // Player 1 Controls: WASD
        if (keys[KeyEvent.VK_W]) player1.accelerate();
        else if (keys[KeyEvent.VK_S]) player1.brake();
        else player1.decelerate();

        if (keys[KeyEvent.VK_A]) player1.steer(-1, WIDTH / 2);
        if (keys[KeyEvent.VK_D]) player1.steer(1, WIDTH / 2);

        // Player 2 Controls: Arrow Keys or AI Logic
        if (isVsAI) {
            player2.updateAI(FINISH_DISTANCE);
        } else {
            if (keys[KeyEvent.VK_UP]) player2.accelerate();
            else if (keys[KeyEvent.VK_DOWN]) player2.brake();
            else player2.decelerate();

            if (keys[KeyEvent.VK_LEFT]) player2.steer(-1, WIDTH);
            if (keys[KeyEvent.VK_RIGHT]) player2.steer(1, WIDTH);
        }

        // Update Car Positions
        player1.update();
        player2.update();

        // Speed relative track motion
        double avgSpeed = (player1.speed + player2.speed) / 2.0;
        trackOffset = (trackOffset + avgSpeed) % 80;

        // Animate roadside objects
        for (RoadSideObject obj : roadSideObjects) {
            obj.y += avgSpeed;
            if (obj.y > HEIGHT) {
                obj.y -= HEIGHT + 120;
            }
        }

        // Check Winner
        if (player1.distance >= FINISH_DISTANCE || player2.distance >= FINISH_DISTANCE) {
            state = GameState.GAME_OVER;
            if (player1.distance >= FINISH_DISTANCE && player2.distance >= FINISH_DISTANCE) {
                winnerText = "IT'S A TIE!";
            } else if (player1.distance >= FINISH_DISTANCE) {
                winnerText = "PLAYER 1 WINS!";
            } else {
                winnerText = isVsAI ? "AI RACER WINS!" : "PLAYER 2 WINS!";
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (state == GameState.MENU) {
            drawMenu(g2d);
        } else {
            drawTrack(g2d);
            drawRoadSideObjects(g2d);
            player1.draw(g2d);
            player2.draw(g2d);
            drawHUD(g2d);

            if (state == GameState.GAME_OVER) {
                drawGameOver(g2d);
            }
        }
    }

    private void drawMenu(Graphics2D g2d) {
        // Dark Gradient Background
        GradientPaint bgGradient = new GradientPaint(0, 0, new Color(15, 15, 30), 0, HEIGHT, new Color(40, 10, 20));
        g2d.setPaint(bgGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 52));
        drawCenteredString(g2d, "SUPERCAR RACING CHALLENGE", HEIGHT / 3);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        drawCenteredString(g2d, "Press [1] to Play against Computer (AI)", HEIGHT / 2);
        drawCenteredString(g2d, "Press [2] for 2 Players (Local)", HEIGHT / 2 + 50);

        g2d.setFont(new Font("Arial", Font.ITALIC, 16));
        g2d.setColor(Color.LIGHT_GRAY);
        drawCenteredString(g2d, "Player 1: [W] Accelerate | [S] Brake | [A/D] Steer", HEIGHT / 2 + 150);
        drawCenteredString(g2d, "Player 2: [UP] Accelerate | [DOWN] Brake | [LEFT/RIGHT] Steer", HEIGHT / 2 + 180);
    }

    private void drawTrack(Graphics2D g2d) {
        // Grass borders
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Asphalt Track
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect(80, 0, WIDTH - 160, HEIGHT);

        // Middle Divider separating Player 1 and Player 2 lanes
        g2d.setColor(Color.RED);
        g2d.fillRect(WIDTH / 2 - 5, 0, 10, HEIGHT);

        // Animated Road Stripes
        g2d.setColor(Color.WHITE);
        Stroke dashed = new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{30, 30}, (float) trackOffset);
        g2d.setStroke(dashed);

        // Left Track Lane Center Stripe
        g2d.drawLine(WIDTH / 4, 0, WIDTH / 4, HEIGHT);
        // Right Track Lane Center Stripe
        g2d.drawLine((3 * WIDTH) / 4, 0, (3 * WIDTH) / 4, HEIGHT);

        // Reset stroke
        g2d.setStroke(new BasicStroke(1));
    }

    private void drawRoadSideObjects(Graphics2D g2d) {
        for (RoadSideObject obj : roadSideObjects) {
            // Tree Canopy
            g2d.setColor(new Color(20, 100, 20));
            g2d.fillOval((int) obj.x, (int) obj.y, 40, 40);
            g2d.setColor(new Color(30, 140, 30));
            g2d.fillOval((int) obj.x + 5, (int) obj.y + 5, 30, 30);
        }
    }

    private void drawHUD(Graphics2D g2d) {
        // Semi-transparent overlay for HUD
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, 70);

        g2d.setFont(new Font("Arial", Font.BOLD, 18));

        // Player 1 Stats
        g2d.setColor(Color.RED);
        g2d.drawString("PLAYER 1 (RED)", 100, 25);
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.format("Speed: %.0f km/h", player1.speed * 18), 100, 50);
        drawProgressBar(g2d, 280, 30, 150, 15, player1.distance / (double) FINISH_DISTANCE, Color.RED);

        // Player 2 Stats
        g2d.setColor(Color.CYAN);
        g2d.drawString(isVsAI ? "AI RACER (BLUE)" : "PLAYER 2 (BLUE)", WIDTH - 380, 25);
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.format("Speed: %.0f km/h", player2.speed * 18), WIDTH - 380, 50);
        drawProgressBar(g2d, WIDTH - 200, 30, 150, 15, player2.distance / (double) FINISH_DISTANCE, Color.CYAN);
    }

    private void drawProgressBar(Graphics2D g2d, int x, int y, int w, int h, double progress, Color fill) {
        g2d.setColor(Color.GRAY);
        g2d.drawRect(x, y, w, h);
        g2d.setColor(fill);
        g2d.fillRect(x + 1, y + 1, (int) Math.min(w - 1, (w - 1) * progress), h - 1);
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(Color.GOLD);
        g2d.setFont(new Font("Arial", Font.BOLD, 56));
        drawCenteredString(g2d, winnerText, HEIGHT / 3);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        drawCenteredString(g2d, "Press [R] to Restart Race", HEIGHT / 2 + 20);
        drawCenteredString(g2d, "Press [M] to Return to Menu", HEIGHT / 2 + 70);
    }

    private void drawCenteredString(Graphics2D g2d, String text, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, y);
    }
}

class Car {
    double x, y;
    double speed = 0;
    double maxSpeed = 16.0;
    double acceleration = 0.25;
    double deceleration = 0.1;
    double brakeForce = 0.4;
    double steeringAngle = 0;
    double distance = 0;

    Color bodyColor;
    String name;
    boolean isAI;

    int width = 45;
    int height = 85;

    public Car(double x, double y, Color color, String name, boolean isAI) {
        this.x = x;
        this.y = y;
        this.bodyColor = color;
        this.name = name;
        this.isAI = isAI;
    }

    public void accelerate() {
        if (speed < maxSpeed) speed += acceleration;
    }

    public void brake() {
        if (speed > 0) speed -= brakeForce;
        if (speed < 0) speed = 0;
    }

    public void decelerate() {
        if (speed > 0) speed -= deceleration;
        if (speed < 0) speed = 0;
    }

    public void steer(int direction, int laneBoundaryRight) {
        if (speed > 0.5) {
            double turnRate = 4.5 * (speed / maxSpeed);
            x += direction * turnRate;
            steeringAngle = direction * 0.15; // Steering rotation animation tilt

            int laneBoundaryLeft = (laneBoundaryRight == 1000 / 2) ? 85 : 1000 / 2 + 10;
            if (x < laneBoundaryLeft) x = laneBoundaryLeft;
            if (x > laneBoundaryRight - width - 10) x = laneBoundaryRight - width - 10;
        }
    }

    public void updateAI(int finishDistance) {
        // AI Acceleration Strategy
        if (speed < maxSpeed * 0.95) {
            accelerate();
        }

        // Slight natural AI steering variation
        double centerOffset = (750 - width / 2.0) - x;
        if (Math.abs(centerOffset) > 5) {
            steer(centerOffset > 0 ? 1 : -1, 1000);
        } else {
            steeringAngle = 0;
        }
    }

    public void update() {
        distance += speed;
        // Decay steering visual tilt when straight
        steeringAngle *= 0.8;
    }

    public void draw(Graphics2D g2d) {
        AffineTransform oldTx = g2d.getTransform();

        // Rotate graphic slightly around center when steering
        g2d.translate(x + width / 2.0, y + height / 2.0);
        g2d.rotate(steeringAngle);

        int drawX = -width / 2;
        int drawY = -height / 2;

        // Shadow effect
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRoundRect(drawX + 6, drawY + 6, width, height, 15, 15);

        // Tires
        g2d.setColor(Color.BLACK);
        g2d.fillRect(drawX - 3, drawY + 10, 6, 18);  // Front-left
        g2d.fillRect(drawX + width - 3, drawY + 10, 6, 18); // Front-right
        g2d.fillRect(drawX - 3, drawY + height - 25, 6, 18); // Rear-left
        g2d.fillRect(drawX + width - 3, drawY + height - 25, 6, 18); // Rear-right

        // Aerodynamic Body Chassis
        g2d.setColor(bodyColor);
        g2d.fillRoundRect(drawX, drawY, width, height, 18, 18);

        // Racing Stripes
        g2d.setColor(Color.WHITE);
        g2d.fillRect(drawX + width / 2 - 3, drawY, 6, height);

        // Windshield and Windows
        g2d.setColor(new Color(40, 40, 60));
        g2d.fillPolygon(
                new int[]{drawX + 8, drawX + width - 8, drawX + width - 12, drawX + 12},
                new int[]{drawY + 28, drawY + 28, drawY + 48, drawY + 48}, 4
        );

        // Headlights
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(drawX + 5, drawY + 2, 8, 5);
        g2d.fillRect(drawX + width - 13, drawY + 2, 8, 5);

        // Rear Spoiler
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(drawX + 2, drawY + height - 8, width - 4, 6);

        g2d.setTransform(oldTx);
    }
}

class RoadSideObject {
    double x, y;

    public RoadSideObject(double x, double y) {
        this.x = x;
        this.y = y;
    }
}