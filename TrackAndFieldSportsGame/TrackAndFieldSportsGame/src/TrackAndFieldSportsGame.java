import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

import static javax.management.Query.or;

public class TrackAndFieldGame extends JPanel implements ActionListener, KeyListener {
    Timer timer;
    Random rand = new Random();

    // Game states
    enum GameState { MENU, SPRINT, LONG_JUMP, JAVELIN, SCOREBOARD, GAME_OVER }
    GameState state = GameState.MENU;

    // Player stats
    String playerName = "Athlete";
    int totalScore = 0;
    int[] eventScores = new int[3];
    String[] medals = {"", "", ""};

    // Sprint variables
    double playerX = 50;
    double playerSpeed = 0;
    double maxSpeed = 12;
    double acceleration = 0.4;
    double energy = 100;
    boolean isRunning = false;

    // Long Jump variables
    double runwayX = 50;
    double jumpVelocity = 0;
    double jumpAngle = 0;
    double gravity = 0.6;
    double jumperY = 400;
    double jumperVX = 0, jumperVY = 0;
    boolean jumping = false;
    boolean inAir = false;
    double jumpDistance = 0;
    double powerMeter = 0;
    boolean chargingPower = false;

    // Javelin variables
    double javelinX = 100, javelinY = 350;
    double javelinVX = 0, javelinVY = 0;
    boolean javelinThrown = false;
    double throwPower = 0;
    double throwAngle = 45;
    boolean settingAngle = false;

    // Animation
    int frame = 0;
    int crowdCheer = 0;

    // Fonts & Colors
    Font titleFont = new Font("Arial", Font.BOLD, 48);
    Font bigFont = new Font("Arial", Font.BOLD, 32);
    Font mediumFont = new Font("Arial", Font.BOLD, 24);
    Font smallFont = new Font("Arial", Font.PLAIN, 18);

    public TrackAndFieldGame() {
        setPreferredSize(new Dimension(1000, 600));
        setBackground(new Color(100, 180, 255));
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (state == GameState.MENU) drawMenu(g2d);
        else if (state == GameState.SPRINT) drawSprint(g2d);
        else if (state == GameState.LONG_JUMP) drawLongJump(g2d);
        else if (state == GameState.JAVELIN) drawJavelin(g2d);
        else if (state == GameState.SCOREBOARD) drawScoreboard(g2d);
        else if (state == GameState.GAME_OVER) drawGameOver(g2d);

        drawCrowd(g2d);
        frame++;
        or (frame % 30 == 0); crowdCheer = 10;
        if (crowdCheer > 0) crowdCheer--;
    }

    private void or(boolean b) {
    }

    void drawMenu(Graphics2D g) {
        setBackground(new Color(50, 150, 255));
        g.setColor(Color.WHITE);
        g.setFont(titleFont);
        drawCenteredString(g, "TRACK & FIELD CHAMPIONSHIP", 300);
        g.setFont(bigFont);
        drawCenteredString(g, "Press SPACE to Start", 400);
        g.setFont(mediumFont);
        drawCenteredString(g, "Events: 100m Sprint • Long Jump • Javelin Throw", 480);
        g.setFont(smallFont);
        g.drawString("Controls: Alternate A/D or ←/→ to run • SPACE for jump/throw", 20, 570);
    }

    void drawSprint(Graphics2D g) {
        setBackground(new Color(100, 180, 255));
        drawTrack(g);
        drawAthlete(g, (int)playerX, 380, frame);
        g.setColor(Color.WHITE);
        g.setFont(bigFont);
        g.drawString("100m Sprint", 20, 50);
        g.drawString("Energy: " + (int)energy + "%", 20, 90);
        g.drawString("Speed: " + String.format("%.1f", playerSpeed * 2) + " m/s", 20, 130);

        // Finish line
        g.setColor(Color.WHITE);
        for (int i = 0; i < 20; i++) {
            g.fillRect(900, 300 + i * 20, 20, 10);
        }

        if (playerX >= 850) {
            double time = frame / 60.0;
            int score = (int)Math.max(0, 1000 - time * 50);
            eventScores[0] = score;
            medals[0] = getMedal(score);
            totalScore += score;
            state = GameState.LONG_JUMP;
            resetSprint();
            JOptionPane.showMessageDialog(this, "100m Time: " + String.format("%.2f", time) + "s\nScore: " + score, "Event Complete!", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    void drawLongJump(Graphics2D g) {
        setBackground(new Color(100, 180, 255));
        drawTrack(g);
        drawRunway(g);

        if (!jumping) {
            drawAthlete(g, (int)runwayX, 380, frame);
            g.setColor(Color.YELLOW);
            g.fillRect(400, 550, (int)(powerMeter * 2), 20);
            g.setColor(Color.WHITE);
            g.drawRect(400, 550, 400, 20);
            g.drawString("Hold SPACE to charge power → Release at takeoff board!", 250, 520);
        } else {
            drawJumperInAir(g);
        }

        // Sand pit
        g.setColor(new Color(194, 154, 82));
        g.fillRect(600, 400, 400, 100);
        g.setColor(Color.WHITE);
        g.drawLine(600, 400, 1000, 400);

        if (jumpDistance > 0) {
            g.setColor(Color.RED);
            g.drawString("Distance: " + String.format("%.2f", jumpDistance) + "m", 400, 100);
        }
    }

    void drawJavelin(Graphics2D g) {
        setBackground(new Color(100, 180, 255));
        drawTrack(g);

        if (!javelinThrown) {
            drawAthleteWithJavelin(g, 100, 350, frame);
            g.setColor(Color.CYAN);
            g.fillRect(300, 550, (int)(throwPower * 3), 20);
            g.setColor(Color.WHITE);
            g.drawRect(300, 550, 500, 20);
            g.drawString("Angle: " + (int)throwAngle + "° ← Use UP/DOWN • SPACE to throw", 300, 520);
        } else {
            drawThrownJavelin(g);
        }
    }

    void drawScoreboard(Graphics2D g) {

    }

    void drawGameOver(Graphics2D g) {
        drawScoreboard(g);
        g.setColor(Color.CYAN);
        g.setFont(bigFont);
        drawCenteredString(g, "Thanks for playing!", 550);
    }

    void drawTrack(Graphics2D g) {
        g.setColor(new Color(180, 70, 50));
        g.fillOval(0, 300, 1000, 400);
        g.setColor(Color.WHITE);
        for (int i = 0; i < 8; i++) {
            g.drawOval(20 + i*40, 320 + i*20, 960 - i*80, 360 - i*40);
        }
    }

    void drawRunway(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.fillRect(50, 420, 600, 4);
        g.setColor(Color.RED);
        g.fillRect(580, 400, 40, 50); // Takeoff board
    }

    void drawAthlete(Graphics2D g, int x, int y, int animFrame) {
        int legFrame = animFrame % 24;
        boolean legForward = legFrame < 12;

        // Body
        g.setColor(new Color(255, 200, 150));
        g.fillOval(x + 10, y - 30, 20, 25); // head
        g.setColor(Color.BLUE);
        g.fillRect(x, y, 40, 50); // torso

        // Arms
        g.setColor(new Color(255, 200, 150));
        g.fillRect(x - 10, y + 10, 15, 40);
        g.fillRect(x + 35, y + 10, 15, 40);

        // Legs
        if (isRunning || playerSpeed > 2) {
            int swing = legForward ? 20 : -20;
            g.fillRect(x + 5, y + 50, 12, 40 + swing);
            g.fillRect(x + 23, y + 50, 12, 40 - swing);
        } else {
            g.fillRect(x + 5, y + 50, 12, 40);
            g.fillRect(x + 23, y + 50, 12, 40);
        }
    }

    void drawAthleteWithJavelin(Graphics2D g, int x, int y, int f) {
        drawAthlete(g, x, y, f);
        g.setColor(Color.ORANGE);
        Polygon javelin = new Polygon();
        javelin.addPoint(x + 40, y - 20);
        javelin.addPoint(x + 80, y - 50);
        javelin.addPoint(x + 85, y - 45);
        javelin.addPoint(x + 45, y - 15);
        g.fillPolygon(javelin);
    }

    void drawJumperInAir(Graphics2D g) {
        AffineTransform old = g.getTransform();
        g.translate(runwayX + 20, jumperY);
        if (jumperVX > 0) g.rotate(Math.atan2(jumperVY, jumperVX));
        drawAthlete(g, 0, 0, frame);
        g.setTransform(old);
    }

    void drawThrownJavelin(Graphics2D g) {
        g.setColor(Color.ORANGE);
        g.fillRect((int)javelinX, (int)javelinY, 80, 6);
        g.setColor(Color.RED);
        g.fillPolygon(new int[]{(int)javelinX+80, (int)javelinX+90, (int)javelinX+80},
                new int[]{(int)javelinY, (int)javelinY+3, (int)javelinY+6}, 3);
    }

    void drawCrowd(Graphics2D g) {
        for (int i = 0; i < 1000; i += 20) {
            int h = 100 + rand.nextInt(150);
            g.setColor(new Color(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255)));
            g.fillRect(i, 0, 18, h + (crowdCheer > 0 ? rand.nextInt(50) : 0));
        }
    }

    void drawCenteredString(Graphics2D g, String s, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(s)) / 2;
        g.drawString(s, x, y);
    }

    String getMedal(int score) {
        if (score >= 900) return "GOLD";
        if (score >= 700) return "SILVER";
        if (score >= 500) return "BRONZE";
        return "";
    }

    void resetSprint() {
        playerX = 50; playerSpeed = 0; energy = 100; isRunning = false;
    }

    void resetLongJump() {
        runwayX = 50; jumping = false; inAir = false; jumpDistance = 0;
        powerMeter = 0; chargingPower = false; jumperY = 400;
    }

    void resetJavelin() {
        javelinThrown = false; javelinX = 100; javelinY = 350;
        throwPower = 0; throwAngle = 45;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameState.SPRINT) updateSprint();
        if (state == GameState.LONG_JUMP) updateLongJump();
        if (state == GameState.JAVELIN) updateJavelin();
        repaint();
    }

    void updateSprint() {
        if (isRunning && energy > 0) {
            playerSpeed = Math.min(maxSpeed, playerSpeed + acceleration);
            playerX += playerSpeed;
            energy -= 0.3;
        } else {
            playerSpeed = Math.max(0, playerSpeed - 0.6);
        }
    }

    void updateLongJump() {
        if (!jumping) {
            if (chargingPower) {
                powerMeter = Math.min(100, powerMeter + 2.5);
            }
            runwayX += 6;
            if (runwayX > 580 && runwayX < 620 && powerMeter > 30) {
                // Perfect takeoff zone
            }
        } else if (inAir) {
            jumperVX += 0.1;
            jumperVY += gravity;
            runwayX += jumperVX;
            jumperY += jumperVY;
            if (jumperY > 400) {
                jumperY = 400;
                inAir = false;
                jumpDistance = (runwayX - 600) / 40.0;
                int score = (int)(jumpDistance * 120);
                eventScores[1] = score;
                medals[1] = getMedal(score);
                totalScore += score;
                new Timer(2000, evt -> {
                    state = GameState.JAVELIN;
                    resetJavelin();
                }).start();
            }
        }
    }

    void updateJavelin() {
        if (javelinThrown) {
            javelinVX *= 0.99;
            javelinVY += 0.6;
            javelinX += javelinVX;
            javelinY += javelinVY;
            if (javelinVY > 0 && javelinY > 380) {
                double distance = javelinX / 30.0;
                int score = (int)(distance * 100 * (1 - Math.abs(throwAngle - 42)/30.0));
                score = Math.max(0, score);
                eventScores[2] = score;
                medals[2] = getMedal(score);
                totalScore += score;
                new Timer(1500, evt -> state = GameState.SCOREBOARD).start();
            }
        }
    }

    // Key Controls
    boolean leftPressed = false, rightPressed = false;

    @Override public void keyPressed(KeyEvent e) {
        if (state == GameState.MENU && e.getKeyCode() == KeyEvent.VK_SPACE) {
            playerName = JOptionPane.showInputDialog("Enter your name:", "Champion");
            if (playerName == null || playerName.trim().isEmpty()) playerName = "Athlete";
            state = GameState.SPRINT;
            return;
        }

        if (state == GameState.SCOREBOARD && e.getKeyCode() == KeyEvent.VK_SPACE) {
            state = GameState.GAME_OVER;
        }

        if (state == GameState.SPRINT) {
            if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = true;
            if (e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = true;
            isRunning = leftPressed || rightPressed;
        }

        if (state == GameState.LONG_JUMP) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (!jumping && runwayX < 620) {
                    chargingPower = true;
                }
            }
        }

        if (state == GameState.JAVELIN) {
            if (e.getKeyCode() == KeyEvent.VK_UP) throwAngle = Math.max(20, throwAngle - 1);
            if (e.getKeyCode() == KeyEvent.VK_DOWN) throwAngle = Math.min(70, throwAngle + 1);
            if (e.getKeyCode() == KeyEvent.VK_SPACE && !javelinThrown) {
                throwPower += 15;
                if (throwPower > 100) throwPower = 100;
            }
        }
    }

    @Override public void keyReleased(KeyEvent e) {
        if (state == GameState.SPRINT) {
            if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = false;
            if (e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = false;
            isRunning = leftPressed || rightPressed;
        }

        if (state == GameState.LONG_JUMP && e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (!jumping && powerMeter > 20) {
                jumping = true;
                inAir = true;
                double angleRad = Math.toRadians(35 + powerMeter * 0.2);
                jumperVX = powerMeter * 0.25;
                jumperVY = -powerMeter * 0.35 * Math.sin(angleRad);
            }
            chargingPower = false;
        }

        if (state == GameState.JAVELIN && e.getKeyCode() == KeyEvent.VK_SPACE && !javelinThrown) {
            javelinThrown = true;
            double angleRad = Math.toRadians(throwAngle);
            javelinVX = throwPower * 0.5 * Math.cos(angleRad);
            javelinVY = -throwPower * 0.5 * Math.sin(angleRad);
        }
    }

    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Track & Field Sports Championship");
        TrackAndFieldGame game = new TrackAndFieldGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setResizable(false);
    }
}