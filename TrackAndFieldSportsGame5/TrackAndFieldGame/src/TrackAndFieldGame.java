import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Track and Field Sports Game
 * Features multiple events: 100m Sprint, Long Jump, and Javelin Throw
 * with smooth animations and competitive gameplay
 */
public class TrackAndFieldGame extends JFrame {
    private GamePanel gamePanel;
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 700;

    public TrackAndFieldGame() {
        setTitle("Track and Field Sports Championship");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        gamePanel = new GamePanel();
        add(gamePanel);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TrackAndFieldGame());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private Timer gameTimer;
    private GameState currentState;
    private int selectedEvent = 0;
    private String[] events = {"100M SPRINT", "LONG JUMP", "JAVELIN THROW"};

    // Sprint variables
    private Athlete player;
    private List<Athlete> opponents;
    private int sprintPower = 0;
    private boolean sprintActive = false;
    private long raceStartTime = 0;
    private long raceEndTime = 0;
    private boolean raceFinished = false;
    private int keyPressCount = 0;
    private long lastKeyPressTime = 0;

    // Long Jump variables
    private boolean jumpRunning = false;
    private boolean jumpInAir = false;
    private double jumpSpeed = 0;
    private double jumpHeight = 0;
    private double jumpDistance = 0;
    private double jumperX = 50;
    private double jumperY = 400;
    private double jumpVelocityY = 0;
    private double jumpVelocityX = 0;
    private boolean jumpCompleted = false;
    private int jumpAttempts = 0;
    private double bestJump = 0;

    // Javelin variables
    private boolean javelinRunning = false;
    private boolean javelinThrown = false;
    private double javelinAngle = 45;
    private double javelinPower = 0;
    private double javelinX = 100;
    private double javelinY = 400;
    private double javelinVX = 0;
    private double javelinVY = 0;
    private double javelinDistance = 0;
    private boolean javelinCompleted = false;
    private int javelinAttempts = 0;
    private double bestThrow = 0;
    private boolean chargingThrow = false;

    // Animation variables
    private int animationFrame = 0;
    private int cloudOffset = 0;
    private List<Cloud> clouds;
    private List<Spectator> spectators;

    // Colors and fonts
    private Color trackColor = new Color(139, 69, 19);
    private Color grassColor = new Color(34, 139, 34);
    private Color skyColor = new Color(135, 206, 235);
    private Font titleFont = new Font("Arial", Font.BOLD, 36);
    private Font normalFont = new Font("Arial", Font.PLAIN, 18);
    private Font smallFont = new Font("Arial", Font.PLAIN, 14);

    enum GameState {
        MENU, SPRINT, LONG_JUMP, JAVELIN, RESULTS
    }

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        setBackground(skyColor);

        currentState = GameState.MENU;
        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();

        initializeGame();
    }

    private void initializeGame() {
        // Initialize clouds
        clouds = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < 8; i++) {
            clouds.add(new Cloud(rand.nextInt(1200), rand.nextInt(200),
                    rand.nextInt(60) + 40, rand.nextInt(30) + 20));
        }

        // Initialize spectators
        spectators = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            spectators.add(new Spectator(50 + i * 55, 150,
                    new Color(rand.nextInt(200) + 55, rand.nextInt(200) + 55, rand.nextInt(200) + 55)));
        }
    }

    private void initializeSprint() {
        player = new Athlete(50, 400, "PLAYER", new Color(0, 100, 255));
        opponents = new ArrayList<>();
        opponents.add(new Athlete(50, 350, "RIVAL 1", new Color(255, 0, 0)));
        opponents.add(new Athlete(50, 450, "RIVAL 2", new Color(0, 200, 0)));
        opponents.add(new Athlete(50, 500, "RIVAL 3", new Color(255, 165, 0)));

        sprintPower = 0;
        sprintActive = false;
        raceFinished = false;
        keyPressCount = 0;
        player.reset();
        for (Athlete opp : opponents) {
            opp.reset();
        }
    }

    private void initializeLongJump() {
        jumpRunning = false;
        jumpInAir = false;
        jumpSpeed = 0;
        jumpHeight = 0;
        jumpDistance = 0;
        jumperX = 50;
        jumperY = 400;
        jumpVelocityY = 0;
        jumpVelocityX = 0;
        jumpCompleted = false;
    }

    private void initializeJavelin() {
        javelinRunning = false;
        javelinThrown = false;
        javelinAngle = 45;
        javelinPower = 0;
        javelinX = 100;
        javelinY = 400;
        javelinVX = 0;
        javelinVY = 0;
        javelinDistance = 0;
        javelinCompleted = false;
        chargingThrow = false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        animationFrame++;
        cloudOffset = (cloudOffset + 1) % 1200;

        switch (currentState) {
            case SPRINT:
                updateSprint();
                break;
            case LONG_JUMP:
                updateLongJump();
                break;
            case JAVELIN:
                updateJavelin();
                break;
        }

        repaint();
    }

    private void updateSprint() {
        if (sprintActive && !raceFinished) {
            // Update player
            player.update(sprintPower / 100.0);

            // Update opponents with AI
            for (Athlete opp : opponents) {
                double aiSpeed = 0.85 + Math.random() * 0.2;
                opp.update(aiSpeed);
            }

            // Check if race finished
            if (player.x >= 1100) {
                raceFinished = true;
                raceEndTime = System.currentTimeMillis();
                player.finishTime = (raceEndTime - raceStartTime) / 1000.0;
            }

            for (Athlete opp : opponents) {
                if (opp.x >= 1100 && opp.finishTime == 0) {
                    opp.finishTime = (System.currentTimeMillis() - raceStartTime) / 1000.0;
                }
            }

            // Decay power
            if (sprintPower > 0) {
                sprintPower -= 0.5;
            }
        }
    }

    private void updateLongJump() {
        if (jumpRunning && !jumpInAir) {
            // Running phase
            jumperX += jumpSpeed;
            animationFrame++;

            if (jumperX >= 600) {
                jumpInAir = true;
                jumpVelocityY = -15 - (jumpSpeed * 0.5);
                jumpVelocityX = jumpSpeed;
            }
        } else if (jumpInAir) {
            // Jump phase
            jumperX += jumpVelocityX;
            jumperY += jumpVelocityY;
            jumpVelocityY += 0.6; // Gravity
            jumpHeight = Math.max(jumpHeight, 400 - jumperY);

            if (jumperY >= 400) {
                jumperY = 400;
                jumpInAir = false;
                jumpRunning = false;
                jumpCompleted = true;
                jumpDistance = (jumperX - 600) / 5.0; // Convert to meters
                if (jumpDistance > bestJump) {
                    bestJump = jumpDistance;
                }
            }
        }
    }

    private void updateJavelin() {
        if (javelinThrown) {
            javelinX += javelinVX;
            javelinY += javelinVY;
            javelinVY += 0.4; // Gravity

            // Update angle based on velocity
            if (javelinVX != 0) {
                javelinAngle = Math.toDegrees(Math.atan2(javelinVY, javelinVX));
            }

            if (javelinY >= 400) {
                javelinY = 400;
                javelinThrown = false;
                javelinCompleted = true;
                javelinDistance = (javelinX - 100) / 8.0; // Convert to meters
                if (javelinDistance > bestThrow) {
                    bestThrow = javelinDistance;
                }
            }
        }

        if (chargingThrow) {
            javelinPower = Math.min(100, javelinPower + 2);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw clouds
        for (Cloud cloud : clouds) {
            cloud.draw(g2d, cloudOffset);
        }

        switch (currentState) {
            case MENU:
                drawMenu(g2d);
                break;
            case SPRINT:
                drawSprint(g2d);
                break;
            case LONG_JUMP:
                drawLongJump(g2d);
                break;
            case JAVELIN:
                drawJavelin(g2d);
                break;
            case RESULTS:
                drawResults(g2d);
                break;
        }
    }

    private void drawMenu(Graphics2D g2d) {
        // Title
        g2d.setFont(titleFont);
        g2d.setColor(new Color(255, 215, 0));
        drawCenteredString(g2d, "TRACK & FIELD CHAMPIONSHIP", 100);

        // Draw stadium ground
        g2d.setColor(grassColor);
        g2d.fillRect(0, 500, getWidth(), 200);

        // Draw spectators
        for (Spectator spec : spectators) {
            spec.draw(g2d, animationFrame);
        }

        // Event selection
        g2d.setFont(normalFont);
        g2d.setColor(Color.WHITE);
        drawCenteredString(g2d, "SELECT EVENT (↑/↓ arrows, ENTER to start)", 250);

        for (int i = 0; i < events.length; i++) {
            if (i == selectedEvent) {
                g2d.setColor(new Color(255, 215, 0));
                g2d.fillRoundRect(350, 320 + i * 60, 500, 50, 15, 15);
                g2d.setColor(Color.BLACK);
            } else {
                g2d.setColor(new Color(100, 100, 100, 150));
                g2d.fillRoundRect(350, 320 + i * 60, 500, 50, 15, 15);
                g2d.setColor(Color.WHITE);
            }
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            drawCenteredString(g2d, events[i], 350 + i * 60);
        }

        // Instructions
        g2d.setFont(smallFont);
        g2d.setColor(Color.LIGHT_GRAY);
        drawCenteredString(g2d, "Press ESC to return to menu anytime", 650);
    }

    private void drawSprint(Graphics2D g2d) {
        // Draw grass
        g2d.setColor(grassColor);
        g2d.fillRect(0, 300, getWidth(), 400);

        // Draw track lanes
        for (int i = 0; i < 5; i++) {
            int y = 320 + i * 70;
            g2d.setColor(trackColor);
            g2d.fillRect(0, y, getWidth(), 60);

            // Lane lines
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10}, 0));
            if (i < 4) {
                g2d.drawLine(0, y + 60, getWidth(), y + 60);
            }
        }

        // Draw finish line
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(5));
        g2d.drawLine(1100, 300, 1100, 650);
        for (int i = 0; i < 20; i++) {
            if (i % 2 == 0) {
                g2d.setColor(Color.BLACK);
            } else {
                g2d.setColor(Color.WHITE);
            }
            g2d.fillRect(1095, 300 + i * 17, 10, 17);
        }

        // Draw spectators
        for (Spectator spec : spectators) {
            spec.draw(g2d, animationFrame);
        }

        // Draw athletes
        player.draw(g2d, animationFrame);
        for (Athlete opp : opponents) {
            opp.draw(g2d, animationFrame);
        }

        // Draw UI
        g2d.setFont(normalFont);
        g2d.setColor(Color.WHITE);
        g2d.drawString("100M SPRINT", 20, 30);

        if (!sprintActive) {
            g2d.setColor(new Color(255, 215, 0));
            drawCenteredString(g2d, "Press SPACE rapidly to start and run!", 150);
            drawCenteredString(g2d, "The faster you tap, the faster you run!", 180);
        } else {
            // Power meter
            g2d.setColor(new Color(50, 50, 50, 200));
            g2d.fillRoundRect(20, 60, 200, 30, 10, 10);
            g2d.setColor(new Color(0, 255, 0));
            g2d.fillRoundRect(22, 62, (int)(sprintPower * 1.96), 26, 8, 8);
            g2d.setColor(Color.WHITE);
            g2d.drawString("SPEED", 25, 82);

            // Distance
            int distance = (int)((player.x / 1100.0) * 100);
            g2d.drawString("Distance: " + distance + "m", 20, 120);

            if (raceFinished) {
                g2d.setFont(new Font("Arial", Font.BOLD, 30));
                g2d.setColor(new Color(255, 215, 0));
                drawCenteredString(g2d, "FINISH! Time: " + String.format("%.2f", player.finishTime) + "s", 150);
                g2d.setFont(normalFont);
                g2d.drawString("Press ENTER for results", 450, 200);
            }
        }
    }

    private void drawLongJump(Graphics2D g2d) {
        // Draw grass
        g2d.setColor(grassColor);
        g2d.fillRect(0, 450, getWidth(), 250);

        // Draw runway
        g2d.setColor(trackColor);
        g2d.fillRect(0, 400, 700, 50);

        // Draw sand pit
        g2d.setColor(new Color(210, 180, 140));
        g2d.fillRect(600, 400, 600, 50);

        // Draw takeoff board
        g2d.setColor(Color.WHITE);
        g2d.fillRect(590, 400, 10, 50);

        // Draw distance markers
        g2d.setFont(smallFont);
        g2d.setColor(Color.BLACK);
        for (int i = 0; i <= 10; i++) {
            int x = 600 + i * 50;
            g2d.drawLine(x, 445, x, 455);
            g2d.drawString(i + "m", x - 10, 470);
        }

        // Draw jumper
        drawJumper(g2d);

        // Draw UI
        g2d.setFont(normalFont);
        g2d.setColor(Color.WHITE);
        g2d.drawString("LONG JUMP", 20, 30);
        g2d.drawString("Attempts: " + jumpAttempts + "/3", 20, 60);
        g2d.drawString("Best: " + String.format("%.2f", bestJump) + "m", 20, 90);

        if (!jumpRunning && !jumpCompleted) {
            g2d.setColor(new Color(255, 215, 0));
            drawCenteredString(g2d, "Hold SPACE to build speed, release at takeoff board!", 150);

            if (jumpSpeed > 0) {
                // Speed meter
                g2d.setColor(new Color(50, 50, 50, 200));
                g2d.fillRoundRect(450, 200, 300, 30, 10, 10);
                g2d.setColor(new Color(0, 255, 0));
                g2d.fillRoundRect(452, 202, (int)(jumpSpeed * 15), 26, 8, 8);
                g2d.setColor(Color.WHITE);
                g2d.drawString("SPEED", 570, 220);
            }
        } else if (jumpCompleted) {
            g2d.setFont(new Font("Arial", Font.BOLD, 30));
            g2d.setColor(new Color(255, 215, 0));
            drawCenteredString(g2d, "Jump: " + String.format("%.2f", jumpDistance) + "m", 150);
            g2d.setFont(normalFont);
            if (jumpAttempts < 3) {
                g2d.drawString("Press SPACE for next attempt", 450, 200);
            } else {
                g2d.drawString("Press ENTER for results", 480, 200);
            }
        }
    }

    private void drawJumper(Graphics2D g2d) {
        int x = (int)jumperX;
        int y = (int)jumperY;

        // Body
        g2d.setColor(new Color(0, 100, 255));
        g2d.fillOval(x - 15, y - 50, 30, 35);

        // Head
        g2d.setColor(new Color(255, 220, 177));
        g2d.fillOval(x - 12, y - 70, 24, 24);

        // Limbs (animated based on state)
        g2d.setStroke(new BasicStroke(4));
        if (jumpInAir) {
            // Arms forward
            g2d.drawLine(x, y - 40, x + 20, y - 30);
            g2d.drawLine(x, y - 40, x - 20, y - 30);
            // Legs together
            g2d.drawLine(x, y - 15, x + 10, y + 10);
            g2d.drawLine(x, y - 15, x - 10, y + 10);
        } else {
            // Running animation
            int legOffset = (animationFrame / 3) % 20 - 10;
            g2d.drawLine(x, y - 40, x + 15, y - 25);
            g2d.drawLine(x, y - 40, x - 15, y - 25);
            g2d.drawLine(x, y - 15, x + legOffset, y + 15);
            g2d.drawLine(x, y - 15, x - legOffset, y + 15);
        }
    }

    private void drawJavelin(Graphics2D g2d) {
        // Draw grass
        g2d.setColor(grassColor);
        g2d.fillRect(0, 450, getWidth(), 250);

        // Draw runway
        g2d.setColor(trackColor);
        g2d.fillRect(0, 400, 200, 50);

        // Draw throwing area
        g2d.setColor(new Color(160, 82, 45));
        g2d.fillOval(150, 380, 100, 90);

        // Draw distance markers
        g2d.setFont(smallFont);
        g2d.setColor(Color.BLACK);
        for (int i = 0; i <= 100; i += 10) {
            int x = 200 + i * 10;
            if (x < getWidth()) {
                g2d.drawLine(x, 445, x, 455);
                g2d.drawString(i + "m", x - 10, 470);
            }
        }

        // Draw thrower
        if (!javelinThrown) {
            drawThrower(g2d, (int)javelinX, (int)javelinY, javelinAngle);
        }

        // Draw javelin
        drawJavelinProjectile(g2d);

        // Draw UI
        g2d.setFont(normalFont);
        g2d.setColor(Color.WHITE);
        g2d.drawString("JAVELIN THROW", 20, 30);
        g2d.drawString("Attempts: " + javelinAttempts + "/3", 20, 60);
        g2d.drawString("Best: " + String.format("%.2f", bestThrow) + "m", 20, 90);

        if (!javelinThrown && !javelinCompleted) {
            g2d.setColor(new Color(255, 215, 0));
            drawCenteredString(g2d, "↑/↓ to adjust angle, Hold SPACE to charge power, Release to throw!", 150);

            // Angle indicator
            g2d.setColor(Color.WHITE);
            g2d.drawString("Angle: " + (int)javelinAngle + "°", 450, 200);

            if (chargingThrow) {
                // Power meter
                g2d.setColor(new Color(50, 50, 50, 200));
                g2d.fillRoundRect(450, 220, 300, 30, 10, 10);
                g2d.setColor(new Color(255, 0, 0));
                g2d.fillRoundRect(452, 222, (int)(javelinPower * 2.96), 26, 8, 8);
                g2d.setColor(Color.WHITE);
                g2d.drawString("POWER", 570, 240);
            }
        } else if (javelinCompleted) {
            g2d.setFont(new Font("Arial", Font.BOLD, 30));
            g2d.setColor(new Color(255, 215, 0));
            drawCenteredString(g2d, "Throw: " + String.format("%.2f", javelinDistance) + "m", 150);
            g2d.setFont(normalFont);
            if (javelinAttempts < 3) {
                g2d.drawString("Press SPACE for next attempt", 450, 200);
            } else {
                g2d.drawString("Press ENTER for results", 480, 200);
            }
        }
    }

    private void drawThrower(Graphics2D g2d, int x, int y, double angle) {
        // Body
        g2d.setColor(new Color(255, 0, 0));
        g2d.fillOval(x - 15, y - 50, 30, 35);

        // Head
        g2d.setColor(new Color(255, 220, 177));
        g2d.fillOval(x - 12, y - 70, 24, 24);

        // Arms (throwing position)
        g2d.setStroke(new BasicStroke(4));
        g2d.setColor(new Color(255, 220, 177));
        int armX = x + (int)(25 * Math.cos(Math.toRadians(angle - 90)));
        int armY = y - 35 + (int)(25 * Math.sin(Math.toRadians(angle - 90)));
        g2d.drawLine(x, y - 35, armX, armY);

        // Legs
        g2d.drawLine(x, y - 15, x + 10, y + 15);
        g2d.drawLine(x, y - 15, x - 10, y + 15);
    }

    private void drawJavelinProjectile(Graphics2D g2d) {
        g2d.setColor(new Color(192, 192, 192));
        g2d.setStroke(new BasicStroke(3));

        int jx = (int)javelinX;
        int jy = (int)javelinY;
        int length = 40;

        int endX = jx + (int)(length * Math.cos(Math.toRadians(javelinAngle)));
        int endY = jy + (int)(length * Math.sin(Math.toRadians(javelinAngle)));

        g2d.drawLine(jx, jy, endX, endY);

        // Javelin tip
        g2d.setColor(new Color(139, 69, 19));
        int tipX = jx + (int)((length + 10) * Math.cos(Math.toRadians(javelinAngle)));
        int tipY = jy + (int)((length + 10) * Math.sin(Math.toRadians(javelinAngle)));
        g2d.drawLine(endX, endY, tipX, tipY);
    }

    private void drawResults(Graphics2D g2d) {
        g2d.setColor(grassColor);
        g2d.fillRect(0, 400, getWidth(), 300);

        g2d.setFont(titleFont);
        g2d.setColor(new Color(255, 215, 0));
        drawCenteredString(g2d, "EVENT COMPLETE!", 100);

        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);

        String result = "";
        switch (selectedEvent) {
            case 0: // Sprint
                result = "Your Time: " + String.format("%.2f", player.finishTime) + " seconds";
                break;
            case 1: // Long Jump
                result = "Best Jump: " + String.format("%.2f", bestJump) + " meters";
                break;
            case 2: // Javelin
                result = "Best Throw: " + String.format("%.2f", bestThrow) + " meters";
                break;
        }

        drawCenteredString(g2d, result, 250);

        g2d.setFont(normalFont);
        drawCenteredString(g2d, "Press ENTER to return to menu", 400);
        drawCenteredString(g2d, "Press SPACE to try again", 440);
    }

    private void drawCenteredString(Graphics2D g2d, String text, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, y);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_ESCAPE) {
            currentState = GameState.MENU;
            return;
        }

        switch (currentState) {
            case MENU:
                handleMenuKeys(key);
                break;
            case SPRINT:
                handleSprintKeys(key);
                break;
            case LONG_JUMP:
                handleLongJumpKeys(key);
                break;
            case JAVELIN:
                handleJavelinKeys(key);
                break;
            case RESULTS:
                handleResultsKeys(key);
                break;
        }
    }

    private void handleMenuKeys(int key) {
        if (key == KeyEvent.VK_UP) {
            selectedEvent = (selectedEvent - 1 + events.length) % events.length;
        } else if (key == KeyEvent.VK_DOWN) {
            selectedEvent = (selectedEvent + 1) % events.length;
        } else if (key == KeyEvent.VK_ENTER) {
            switch (selectedEvent) {
                case 0:
                    currentState = GameState.SPRINT;
                    initializeSprint();
                    break;
                case 1:
                    currentState = GameState.LONG_JUMP;
                    initializeLongJump();
                    jumpAttempts = 0;
                    bestJump = 0;
                    break;
                case 2:
                    currentState = GameState.JAVELIN;
                    initializeJavelin();
                    javelinAttempts = 0;
                    bestThrow = 0;
                    break;
            }
        }
    }

    private void handleSprintKeys(int key) {
        if (key == KeyEvent.VK_SPACE && !raceFinished) {
            long currentTime = System.currentTimeMillis();
            if (!sprintActive) {
                sprintActive = true;
                raceStartTime = currentTime;
            }

            // Check for rapid key presses
            if (currentTime - lastKeyPressTime < 200) {
                sprintPower = Math.min(100, sprintPower + 15);
                keyPressCount++;
            } else {
                keyPressCount = 0;
            }
            lastKeyPressTime = currentTime;
        } else if (key == KeyEvent.VK_ENTER && raceFinished) {
            currentState = GameState.RESULTS;
        }
    }

    private void handleLongJumpKeys(int key) {
        if (key == KeyEvent.VK_SPACE) {
            if (!jumpRunning && !jumpCompleted) {
                jumpSpeed = Math.min(20, jumpSpeed + 0.8);
            } else if (jumpCompleted) {
                jumpAttempts++;
                if (jumpAttempts < 3) {
                    initializeLongJump();
                }
            }
        } else if (key == KeyEvent.VK_ENTER && jumpAttempts >= 3) {
            currentState = GameState.RESULTS;
        }
    }

    private void handleJavelinKeys(int key) {
        if (key == KeyEvent.VK_UP && !javelinThrown) {
            javelinAngle = Math.min(80, javelinAngle + 2);
        } else if (key == KeyEvent.VK_DOWN && !javelinThrown) {
            javelinAngle = Math.max(20, javelinAngle - 2);
        } else if (key == KeyEvent.VK_SPACE) {
            if (!javelinThrown && !javelinCompleted) {
                chargingThrow = true;
            } else if (javelinCompleted) {
                javelinAttempts++;
                if (javelinAttempts < 3) {
                    initializeJavelin();
                }
            }
        } else if (key == KeyEvent.VK_ENTER && javelinAttempts >= 3) {
            currentState = GameState.RESULTS;
        }
    }

    private void handleResultsKeys(int key) {
        if (key == KeyEvent.VK_ENTER) {
            currentState = GameState.MENU;
        } else if (key == KeyEvent.VK_SPACE) {
            switch (selectedEvent) {
                case 0:
                    currentState = GameState.SPRINT;
                    initializeSprint();
                    break;
                case 1:
                    currentState = GameState.LONG_JUMP;
                    initializeLongJump();
                    jumpAttempts = 0;
                    bestJump = 0;
                    break;
                case 2:
                    currentState = GameState.JAVELIN;
                    initializeJavelin();
                    javelinAttempts = 0;
                    bestThrow = 0;
                    break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (currentState == GameState.LONG_JUMP && key == KeyEvent.VK_SPACE && !jumpInAir && !jumpCompleted) {
            if (jumpSpeed > 5) {
                jumpRunning = true;
            } else {
                jumpSpeed = 0;
            }
        } else if (currentState == GameState.JAVELIN && key == KeyEvent.VK_SPACE && chargingThrow) {
            chargingThrow = false;
            javelinThrown = true;
            double powerFactor = javelinPower / 100.0;
            javelinVX = powerFactor * 15 * Math.cos(Math.toRadians(javelinAngle));
            javelinVY = powerFactor * 15 * Math.sin(Math.toRadians(javelinAngle));
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

class Athlete {
    double x, y;
    String name;
    Color color;
    double finishTime = 0;

    public Athlete(double x, double y, String name, Color color) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.color = color;
    }

    public void reset() {
        x = 50;
        finishTime = 0;
    }

    public void update(double speed) {
        x += speed * 3.5;
    }

    public void draw(Graphics2D g2d, int frame) {
        int ix = (int)x;
        int iy = (int)y;

        // Body
        g2d.setColor(color);
        g2d.fillOval(ix - 10, iy - 35, 20, 25);

        // Head
        g2d.setColor(new Color(255, 220, 177));
        g2d.fillOval(ix - 8, iy - 50, 16, 16);

        // Animated limbs
        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(color);
        int legPhase = (frame / 2) % 20 - 10;
        g2d.drawLine(ix, iy - 25, ix + 8, iy - 15);
        g2d.drawLine(ix, iy - 25, ix - 8, iy - 15);
        g2d.drawLine(ix, iy - 10, ix + legPhase, iy + 5);
        g2d.drawLine(ix, iy - 10, ix - legPhase, iy + 5);

        // Name tag
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.setColor(Color.WHITE);
        g2d.drawString(name, ix - 20, iy - 55);
    }
}

class Cloud {
    int x, y, width, height;

    public Cloud(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void draw(Graphics2D g2d, int offset) {
        g2d.setColor(new Color(255, 255, 255, 180));
        int drawX = (x + offset / 2) % 1400 - 200;
        g2d.fillOval(drawX, y, width, height);
        g2d.fillOval(drawX + width / 3, y - height / 3, width, height);
        g2d.fillOval(drawX + width * 2 / 3, y, width, height);
    }
}

class Spectator {
    int x, y;
    Color color;

    public Spectator(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public void draw(Graphics2D g2d, int frame) {
        // Simple spectator silhouette
        g2d.setColor(color);
        g2d.fillOval(x - 5, y - 15, 10, 10); // Head
        g2d.fillRect(x - 6, y - 5, 12, 15); // Body

        // Animated arms (waving)
        int armWave = (int)(Math.sin(frame * 0.1 + x) * 5);
        g2d.drawLine(x, y, x - 8, y + armWave);
        g2d.drawLine(x, y, x + 8, y - armWave);
    }
}