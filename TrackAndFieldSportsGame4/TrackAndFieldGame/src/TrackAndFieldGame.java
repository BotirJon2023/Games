import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.Timer;

public class TrackAndFieldGame extends JPanel implements ActionListener, KeyListener {
    // Game states
    private static final int MENU = 0;
    private static final int SPRINT = 1;
    private static final int LONG_JUMP = 2;
    private static final int HIGH_JUMP = 3;
    private static final int JAVELIN = 4;
    private static final int RESULTS = 5;

    private int gameState = MENU;

    // Game variables
    private int score = 0;
    private int currentEvent = 0;
    private String[] events = {"100m Sprint", "Long Jump", "High Jump", "Javelin Throw"};
    private String playerName = "Athlete";

    // Sprint variables
    private int sprintPosition = 0;
    private boolean[] keysPressed = new boolean[4];
    private int sprintTime = 0;
    private boolean sprintFinished = false;

    // Long Jump variables
    private double approachSpeed = 0;
    private double jumpAngle = 0;
    private double jumpPower = 0;
    private boolean inJump = false;
    private double jumpDistance = 0;
    private double jumpX = 0;
    private double jumpY = 0;
    private double jumpVelocityX = 0;
    private double jumpVelocityY = 0;

    // High Jump variables
    private double runUpSpeed = 0;
    private double jumpForce = 0;
    private boolean highJumpInProgress = false;
    private double highJumpHeight = 0;
    private double highJumpX = 0;
    private double highJumpY = 0;

    // Javelin variables
    private double runSpeed = 0;
    private double throwAngle = 0;
    private double throwPower = 0;
    private boolean javelinThrown = false;
    private double javelinDistance = 0;
    private double javelinX = 0;
    private double javelinY = 0;
    private double javelinRotation = 0;

    // Animation timer
    private Timer gameTimer;
    private int animationCounter = 0;

    // Colors
    private Color trackColor = new Color(80, 200, 120);
    private Color fieldColor = new Color(139, 69, 19);
    private Color skyColor = new Color(135, 206, 235);

    public TrackAndFieldGame() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(skyColor);
        addKeyListener(this);
        setFocusable(true);

        // Initialize game timer
        gameTimer = new Timer();
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateGame();
                repaint();
            }
        }, 0, 16); // ~60 FPS
    }

    private void updateGame() {
        animationCounter++;

        switch (gameState) {
            case SPRINT:
                updateSprint();
                break;
            case LONG_JUMP:
                updateLongJump();
                break;
            case HIGH_JUMP:
                updateHighJump();
                break;
            case JAVELIN:
                updateJavelin();
                break;
        }
    }

    private void updateSprint() {
        if (sprintFinished) return;

        // Increase sprint position based on keys pressed
        int keysCount = 0;
        for (boolean pressed : keysPressed) {
            if (pressed) keysCount++;
        }

        if (keysCount > 0) {
            sprintPosition += keysCount * 2;
            sprintTime++;
        }

        // Check if finished
        if (sprintPosition >= 700) {
            sprintFinished = true;
            score += calculateSprintScore();
            nextEvent();
        }
    }

    private void updateLongJump() {
        if (inJump) {
            // Update jump physics
            jumpX += jumpVelocityX;
            jumpY += jumpVelocityY;
            jumpVelocityY += 0.2; // Gravity

            // Check if landed
            if (jumpY >= 300) {
                inJump = false;
                jumpDistance = jumpX;
                score += calculateJumpScore(jumpDistance);
                nextEvent();
            }
        }
    }

    private void updateHighJump() {
        if (highJumpInProgress) {
            // Update high jump physics
            highJumpY -= 5; // Move upward
            highJumpX += 2; // Move slightly forward

            // Apply gravity
            if (highJumpY < 300 - highJumpHeight) {
                highJumpY += 3;
            }

            // Check if landed
            if (highJumpY >= 300) {
                highJumpInProgress = false;
                score += calculateHighJumpScore(highJumpHeight);
                nextEvent();
            }
        }
    }

    private void updateJavelin() {
        if (javelinThrown) {
            // Update javelin physics
            javelinX += 8;
            javelinY += 0.5; // Gravity effect
            javelinRotation += 5;

            // Check if landed
            if (javelinY >= 300) {
                javelinThrown = false;
                javelinDistance = javelinX;
                score += calculateJavelinScore(javelinDistance);
                nextEvent();
            }
        }
    }

    private int calculateSprintScore() {
        // Lower time = higher score
        int maxTime = 200;
        int timeScore = Math.max(0, maxTime - sprintTime);
        return timeScore * 10;
    }

    private int calculateJumpScore(double distance) {
        return (int)(distance * 10);
    }

    private int calculateHighJumpScore(double height) {
        return (int)(height * 100);
    }

    private int calculateJavelinScore(double distance) {
        return (int)(distance * 2);
    }

    private void nextEvent() {
        currentEvent++;
        if (currentEvent >= events.length) {
            gameState = RESULTS;
        } else {
            resetEvent();
        }
    }

    private void resetEvent() {
        sprintPosition = 0;
        sprintFinished = false;
        sprintTime = 0;
        inJump = false;
        highJumpInProgress = false;
        javelinThrown = false;
        Arrays.fill(keysPressed, false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (gameState) {
            case MENU:
                drawMenu(g2d);
                break;
            case SPRINT:
                drawSprint(g2d);
                break;
            case LONG_JUMP:
                drawLongJump(g2d);
                break;
            case HIGH_JUMP:
                drawHighJump(g2d);
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
        // Background
        g2d.setColor(skyColor);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Title
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString("TRACK & FIELD", 200, 100);

        // Events
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        for (int i = 0; i < events.length; i++) {
            g2d.drawString((i + 1) + ". " + events[i], 300, 200 + i * 40);
        }

        // Instructions
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("Press SPACE to start", 320, 400);
        g2d.drawString("Use A, S, D, F keys for running", 280, 450);
    }

    private void drawSprint(Graphics2D g2d) {
        // Draw track
        g2d.setColor(trackColor);
        g2d.fillRect(0, 300, getWidth(), 100);

        // Draw lane markers
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < 8; i++) {
            g2d.drawLine(0, 310 + i * 10, getWidth(), 310 + i * 10);
        }

        // Draw finish line
        g2d.setColor(Color.RED);
        g2d.fillRect(700, 300, 5, 100);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("FINISH", 705, 350);

        // Draw runner
        g2d.setColor(Color.BLUE);
        g2d.fillOval(sprintPosition, 320, 30, 30);
        g2d.setColor(Color.WHITE);
        g2d.fillOval(sprintPosition + 5, 325, 20, 20);

        // Draw UI
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("100m SPRINT", 350, 50);
        g2d.drawString("Score: " + score, 650, 50);
        g2d.drawString("Time: " + sprintTime, 350, 80);

        // Draw instructions
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.drawString("Rapidly press A, S, D, F to run!", 300, 550);
    }

    private void drawLongJump(Graphics2D g2d) {
        // Draw field
        g2d.setColor(fieldColor);
        g2d.fillRect(0, 300, getWidth(), 100);

        // Draw runway
        g2d.setColor(trackColor);
        g2d.fillRect(0, 320, 200, 60);

        // Draw sand pit
        g2d.setColor(new Color(240, 230, 140));
        g2d.fillRect(200, 320, 400, 60);

        // Draw takeoff board
        g2d.setColor(Color.WHITE);
        g2d.fillRect(190, 320, 10, 60);

        // Draw athlete
        if (inJump) {
            // Draw in air
            g2d.setColor(Color.BLUE);
            g2d.fillOval((int)jumpX, (int)jumpY, 30, 30);
        } else {
            // Draw on runway
            g2d.setColor(Color.BLUE);
            g2d.fillOval(150, 330, 30, 30);
        }

        // Draw UI
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("LONG JUMP", 350, 50);
        g2d.drawString("Score: " + score, 650, 50);

        if (inJump) {
            g2d.drawString("Distance: " + String.format("%.2f", jumpDistance) + "m", 350, 80);
        } else {
            g2d.drawString("Press SPACE to start approach", 280, 80);
            g2d.drawString("Hold SPACE for power, release to jump", 250, 110);
        }
    }

    private void drawHighJump(Graphics2D g2d) {
        // Draw field
        g2d.setColor(fieldColor);
        g2d.fillRect(0, 300, getWidth(), 100);

        // Draw high jump setup
        g2d.setColor(Color.GRAY);
        g2d.fillRect(400, 200, 10, 100); // Left post
        g2d.fillRect(500, 200, 10, 100); // Right post

        // Draw bar
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(410, 250, 90, 5);

        // Draw landing mat
        g2d.setColor(new Color(200, 0, 0));
        g2d.fillRect(380, 300, 140, 50);

        // Draw athlete
        if (highJumpInProgress) {
            g2d.setColor(Color.BLUE);
            g2d.fillOval((int)highJumpX, (int)highJumpY, 30, 30);
        } else {
            g2d.setColor(Color.BLUE);
            g2d.fillOval(300, 330, 30, 30);
        }

        // Draw UI
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("HIGH JUMP", 350, 50);
        g2d.drawString("Score: " + score, 650, 50);
        g2d.drawString("Height: " + String.format("%.2f", highJumpHeight) + "m", 350, 80);

        if (!highJumpInProgress) {
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            g2d.drawString("Press SPACE to start approach", 300, 550);
            g2d.drawString("Hold SPACE for power, release to jump", 270, 580);
        }
    }

    private void drawJavelin(Graphics2D g2d) {
        // Draw field
        g2d.setColor(fieldColor);
        g2d.fillRect(0, 300, getWidth(), 100);

        // Draw runway
        g2d.setColor(trackColor);
        g2d.fillRect(0, 320, 200, 60);

        // Draw throwing sector
        g2d.setColor(new Color(240, 230, 140));
        g2d.fillArc(150, 250, 500, 200, 0, 30);

        // Draw athlete
        g2d.setColor(Color.BLUE);
        g2d.fillOval(150, 330, 30, 30);

        // Draw javelin
        if (javelinThrown) {
            g2d.setColor(Color.RED);
            AffineTransform oldTransform = g2d.getTransform();
            g2d.rotate(Math.toRadians(javelinRotation), javelinX + 15, javelinY + 5);
            g2d.fillRect((int)javelinX, (int)javelinY, 30, 10);
            g2d.setTransform(oldTransform);
        } else {
            g2d.setColor(Color.RED);
            g2d.fillRect(180, 335, 30, 10);
        }

        // Draw UI
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("JAVELIN THROW", 330, 50);
        g2d.drawString("Score: " + score, 650, 50);

        if (javelinThrown) {
            g2d.drawString("Distance: " + String.format("%.2f", javelinDistance) + "m", 330, 80);
        } else {
            g2d.drawString("Press SPACE to start approach", 280, 80);
            g2d.drawString("Hold SPACE for power, release to throw", 250, 110);
        }
    }

    private void drawResults(Graphics2D g2d) {
        // Background
        g2d.setColor(skyColor);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Title
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.drawString("FINAL RESULTS", 280, 100);

        // Score
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString("Total Score: " + score, 250, 200);

        // Performance evaluation
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        if (score > 5000) {
            g2d.drawString("Olympic Champion!", 320, 280);
        } else if (score > 3000) {
            g2d.drawString("National Level!", 330, 280);
        } else if (score > 1500) {
            g2d.drawString("Good Performance!", 310, 280);
        } else {
            g2d.drawString("Keep Practicing!", 320, 280);
        }

        // Restart option
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Press R to restart", 340, 400);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (gameState) {
            case MENU:
                if (key == KeyEvent.VK_SPACE) {
                    gameState = SPRINT;
                    resetEvent();
                }
                break;

            case SPRINT:
                if (key >= KeyEvent.VK_A && key <= KeyEvent.VK_F) {
                    keysPressed[key - KeyEvent.VK_A] = true;
                }
                break;

            case LONG_JUMP:
                if (key == KeyEvent.VK_SPACE && !inJump) {
                    // Start approach
                    approachSpeed = 5;
                    inJump = true;
                    jumpX = 150;
                    jumpY = 330;
                    jumpVelocityX = 8;
                    jumpVelocityY = -10;
                }
                break;

            case HIGH_JUMP:
                if (key == KeyEvent.VK_SPACE && !highJumpInProgress) {
                    // Start jump
                    highJumpInProgress = true;
                    highJumpX = 300;
                    highJumpY = 330;
                    highJumpHeight = 2.0 + Math.random() * 1.0; // Random height between 2-3m
                }
                break;

            case JAVELIN:
                if (key == KeyEvent.VK_SPACE && !javelinThrown) {
                    // Throw javelin
                    javelinThrown = true;
                    javelinX = 180;
                    javelinY = 335;
                }
                break;

            case RESULTS:
                if (key == KeyEvent.VK_R) {
                    // Restart game
                    gameState = MENU;
                    currentEvent = 0;
                    score = 0;
                    resetEvent();
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (gameState == SPRINT && key >= KeyEvent.VK_A && key <= KeyEvent.VK_F) {
            keysPressed[key - KeyEvent.VK_A] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Track & Field Sports Game");
        TrackAndFieldGame game = new TrackAndFieldGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Request focus for key events
        game.requestFocusInWindow();
    }
}