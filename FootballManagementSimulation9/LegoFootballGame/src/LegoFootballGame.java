import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;

public class LegoFootballGame extends JFrame {
    private static final int WIDTH = 1100;
    private static final int HEIGHT = 720;

    public LegoFootballGame() {
        setTitle("Lego Brick Football 11v11 Simulation");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        add(new MenuPanel(this));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LegoFootballGame().setVisible(true));
    }
}

class MenuPanel extends JPanel {
    private LegoFootballGame frame;

    public MenuPanel(LegoFootballGame frame) {
        this.frame = frame;
        setLayout(new GridBagLayout());
        setBackground(new Color(16, 124, 16)); // Lego Green Plate

        JLabel title = new JLabel("BRICK-KICKERS 11v11");
        title.setFont(new Font("Arial Black", Font.BOLD, 50));
        title.setForeground(Color.YELLOW);

        JButton pvpButton = createStyledButton("2-Player (Lego Match)");
        JButton pvcButton = createStyledButton("Play vs Computer AI");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.insets = new Insets(15, 15, 15, 15);

        gbc.gridy = 0; add(title, gbc);
        gbc.gridy = 1; add(pvpButton, gbc);
        gbc.gridy = 2; add(pvcButton, gbc);

        pvpButton.addActionListener(e -> startGame(false));
        pvcButton.addActionListener(e -> startGame(true));
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial Black", Font.PLAIN, 16));
        btn.setPreferredSize(new Dimension(300, 55));
        btn.setBackground(Color.RED);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createBevelBorder(0, Color.YELLOW, Color.DARK_GRAY));
        return btn;
    }

    private void startGame(boolean vsComputer) {
        frame.getContentPane().removeAll();
        GamePanel gamePanel = new GamePanel(vsComputer);
        frame.add(gamePanel);
        frame.revalidate();
        gamePanel.requestFocusInWindow();
    }
}

class LegoPlayer {
    double x, y;
    double homeX, homeY;
    double radius = 18; // Slightly larger to accommodate the Lego shape box bounds
    int jerseyNumber;
    String role;

    public LegoPlayer(int id, String role, double homeX, double homeY) {
        this.jerseyNumber = id + 1;
        this.role = role;
        this.homeX = homeX;
        this.homeY = homeY;
        resetToHome();
    }

    public void resetToHome() {
        this.x = homeX;
        this.y = homeY;
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private boolean vsComputer;
    private Timer timer;

    // Scores & Match UI Text
    private int scoreRed = 0;
    private int scoreBlue = 0;
    private String matchMsg = "BRICKS READY... GO!";
    private int msgTimer = 100;

    // Ball
    private double ballX = 550, ballY = 360, ballRadius = 11;
    private double ballVX = 0, ballVY = 0;
    private final double friction = 0.982;

    // Rosters
    private ArrayList<LegoPlayer> teamRed = new ArrayList<>();
    private ArrayList<LegoPlayer> teamBlue = new ArrayList<>();

    private int activeRedIdx = 0;
    private int activeBlueIdx = 0;

    // Field System Boundaries
    private final int marginX = 60, marginY = 60;
    private final int pitchW = 980, pitchH = 580;
    private final int goalH = 140, goalW = 15;
    private final int goalY = 280;

    private boolean[] keys = new boolean[256];

    public GamePanel(boolean vsComputer) {
        this.vsComputer = vsComputer;
        setFocusable(true);
        addKeyListener(this);
        setBackground(new Color(34, 139, 34));

        setupFormations();
        timer = new Timer(16, this); // ~60 FPS animation tick
        timer.start();
    }

    private void setupFormations() {
        teamRed.clear(); teamBlue.clear();

        // Red Team 4-4-2 Grid Placement
        teamRed.add(new LegoPlayer(0, "GK",  marginX + 50,   marginY + pitchH/2));
        teamRed.add(new LegoPlayer(1, "DEF", marginX + 180,  marginY + 120));
        teamRed.add(new LegoPlayer(2, "DEF", marginX + 160,  marginY + 240));
        teamRed.add(new LegoPlayer(3, "DEF", marginX + 160,  marginY + 340));
        teamRed.add(new LegoPlayer(4, "DEF", marginX + 180,  marginY + 460));
        teamRed.add(new LegoPlayer(5, "MID", marginX + 400,  marginY + 100));
        teamRed.add(new LegoPlayer(6, "MID", marginX + 380,  marginY + 220));
        teamRed.add(new LegoPlayer(7, "MID", marginX + 380,  marginY + 360));
        teamRed.add(new LegoPlayer(8, "MID", marginX + 400,  marginY + 480));
        teamRed.add(new LegoPlayer(9, "FWD", marginX + 510,  marginY + 200));
        teamRed.add(new LegoPlayer(10,"FWD", marginX + 510,  marginY + 380));

        // Blue Team Mirror 4-4-2 Grid Placement
        teamBlue.add(new LegoPlayer(0, "GK",  marginX + pitchW - 50,  marginY + pitchH/2));
        teamBlue.add(new LegoPlayer(1, "DEF", marginX + pitchW - 180, marginY + 120));
        teamBlue.add(new LegoPlayer(2, "DEF", marginX + pitchW - 160, marginY + 240));
        teamBlue.add(new LegoPlayer(3, "DEF", marginX + pitchW - 160, marginY + 340));
        teamBlue.add(new LegoPlayer(4, "DEF", marginX + pitchW - 180, marginY + 460));
        teamBlue.add(new LegoPlayer(5, "MID", marginX + pitchW - 400, marginY + 100));
        teamBlue.add(new LegoPlayer(6, "MID", marginX + pitchW - 380, marginY + 220));
        teamBlue.add(new LegoPlayer(7, "MID", marginX + pitchW - 380, marginY + 360));
        teamBlue.add(new LegoPlayer(8, "MID", marginX + pitchW - 400, marginY + 480));
        teamBlue.add(new LegoPlayer(9, "FWD", marginX + pitchW - 510, marginY + 200));
        teamBlue.add(new LegoPlayer(10,"FWD", marginX + pitchW - 510, marginY + 380));

        resetMatchPositions();
    }

    private void resetMatchPositions() {
        ballX = marginX + pitchW / 2;
        ballY = marginY + pitchH / 2;
        ballVX = 0; ballVY = 0;
        for (LegoPlayer p : teamRed) p.resetToHome();
        for (LegoPlayer p : teamBlue) p.resetToHome();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawLegoPitch(g2d);

        // Render Lego Figures
        for (int i = 0; i < teamRed.size(); i++) {
            drawLegoMinifigure(g2d, teamRed.get(i), new Color(220, 20, 20), i == activeRedIdx);
        }
        for (int i = 0; i < teamBlue.size(); i++) {
            drawLegoMinifigure(g2d, teamBlue.get(i), new Color(20, 80, 230), i == activeBlueIdx);
        }

        drawSoccerBall(g2d);
        drawHUD(g2d);
    }

    private void drawLegoPitch(Graphics2D g2d) {
        // Draw segmented green Lego grid studs pattern background
        g2d.setColor(new Color(25, 115, 25));
        g2d.fillRect(marginX, marginY, pitchW, pitchH);

        g2d.setColor(new Color(30, 130, 30));
        for (int x = marginX + 10; x < marginX + pitchW; x += 30) {
            for (int y = marginY + 10; y < marginY + pitchH; y += 30) {
                g2d.fillOval(x, y, 6, 6); // Lego base plate stud rendering simulation
            }
        }

        // Operational Chalk Boundaries
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawRect(marginX, marginY, pitchW, pitchH);
        g2d.drawLine(marginX + pitchW/2, marginY, marginX + pitchW/2, marginY + pitchH);
        g2d.drawOval(marginX + pitchW/2 - 75, marginY + pitchH/2 - 75, 150, 150);

        // Penalty regions
        g2d.drawRect(marginX, marginY + 140, 130, 300);
        g2d.drawRect(marginX + pitchW - 130, marginY + 140, 130, 300);

        // Net posts
        g2d.fillRect(marginX - goalW, marginY + goalY, goalW, goalH);
        g2d.fillRect(marginX + pitchW, marginY + goalY, goalW, goalH);
    }

    // Custom Modular Graphics pipeline representing Lego Minifigures from a top-down view
    private void drawLegoMinifigure(Graphics2D g2d, LegoPlayer p, Color jerseyColor, boolean isActiveControl) {
        int x = (int) p.x;
        int y = (int) p.y;

        // 1. Active User Selector Base Ring Animation
        if (isActiveControl) {
            g2d.setColor(new Color(255, 255, 0, 140));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval(x - 24, y - 24, 48, 48);
        }

        // 2. Toy Circular Stand Base Plate
        g2d.setColor(new Color(40, 40, 40, 160));
        g2d.fillOval(x - 20, y - 20, 40, 40);

        // 3. Lego Boxy Torso Block (Width: 26, Height: 16)
        g2d.setColor(jerseyColor);
        g2d.fill(new RoundRectangle2D.Double(x - 14, y - 9, 28, 18, 4, 4));
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(new RoundRectangle2D.Double(x - 14, y - 9, 28, 18, 4, 4));

        // 4. Round Stud Hands
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(x - 18, y - 4, 6, 6); // Left hand
        g2d.fillOval(x + 12, y - 4, 6, 6); // Right hand

        // 5. Classic Lego Yellow Cylinder Head (Top Center)
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(x - 7, y - 7, 14, 14);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x - 7, y - 7, 14, 14);

        // 6. Hair/Cap piece block layering overlay
        g2d.setColor(new Color(70, 40, 15)); // Brown Hair
        g2d.fillArc(x - 7, y - 8, 14, 10, 0, 180);

        // 7. Text Overlay UI for Role/Number Identification
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial Black", Font.PLAIN, 10));
        g2d.drawString(String.valueOf(p.jerseyNumber), x - 4, y + 22);
    }

    private void drawSoccerBall(Graphics2D g2d) {
        // Render specialized checkered Lego soccer pattern ball
        g2d.setColor(Color.WHITE);
        g2d.fill(new Ellipse2D.Double(ballX - ballRadius, ballY - ballRadius, ballRadius*2, ballRadius*2));
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval((int)(ballX - ballRadius), (int)(ballY - ballRadius), (int)ballRadius * 2, (int)ballRadius * 2);

        // Cross brick lines on the ball
        g2d.drawLine((int)ballX, (int)ballY - (int)ballRadius, (int)ballX, (int)ballY + (int)ballRadius);
        g2d.drawLine((int)ballX - (int)ballRadius, (int)ballY, (int)ballX + (int)ballRadius, (int)ballY);
    }

    private void drawHUD(Graphics2D g2d) {
        // Structured Score overlay boards
        g2d.setColor(new Color(30, 30, 30));
        g2d.fillRect(440, 12, 220, 40);
        g2d.setColor(Color.YELLOW);
        g2d.setColor((Color) BorderFactory.createLineBorder(Color.WHITE));
        g2d.setFont(new Font("Arial Black", Font.PLAIN, 20));
        g2d.drawString("RED " + scoreRed + " - " + scoreBlue + " BLU", 468, 40);

        // Goal celebration animation text string loops
        if (msgTimer > 0) {
            g2d.setFont(new Font("Arial Black", Font.BOLD, 42));
            g2d.setColor(Color.ORANGE);
            g2d.drawString(matchMsg, 310, 350);
            msgTimer--;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        calculateClosestActiveSelections();
        handleUserMovement();
        processTeammateAI();
        updateBallVectorPhysics();
        checkGoalIntersection();
        repaint();
    }

    private void calculateClosestActiveSelections() {
        double minRed = Double.MAX_VALUE;
        for (int i = 0; i < teamRed.size(); i++) {
            double dist = Math.hypot(teamRed.get(i).x - ballX, teamRed.get(i).y - ballY);
            if (dist < minRed) { minRed = dist; activeRedIdx = i; }
        }

        double minBlue = Double.MAX_VALUE;
        for (int i = 0; i < teamBlue.size(); i++) {
            double dist = Math.hypot(teamBlue.get(i).x - ballX, teamBlue.get(i).y - ballY);
            if (dist < minBlue) { minBlue = dist; activeBlueIdx = i; }
        }
    }

    private void handleUserMovement() {
        double speed = 4.5;
        LegoPlayer red = teamRed.get(activeRedIdx);

        if (keys[KeyEvent.VK_W]) red.y -= speed;
        if (keys[KeyEvent.VK_S]) red.y += speed;
        if (keys[KeyEvent.VK_A]) red.x -= speed;
        if (keys[KeyEvent.VK_D]) red.x += speed;
        keepInsidePitch(red);

        if (!vsComputer) {
            LegoPlayer blue = teamBlue.get(activeBlueIdx);
            if (keys[KeyEvent.VK_UP])    blue.y -= speed;
            if (keys[KeyEvent.VK_DOWN])  blue.y += speed;
            if (keys[KeyEvent.VK_LEFT])  blue.x -= speed;
            if (keys[KeyEvent.VK_RIGHT]) blue.x += speed;
            keepInsidePitch(blue);
        }
    }

    private void processTeammateAI() {
        double aiSpeed = 2.2;

        // Red Squad Tactical Flow
        for (int i = 0; i < teamRed.size(); i++) {
            if (i == activeRedIdx) continue;
            trackStrategicBase(teamRed.get(i), ballX, ballY, true);
        }

        // Blue Squad / Computer Matrix
        for (int i = 0; i < teamBlue.size(); i++) {
            LegoPlayer p = teamBlue.get(i);
            if (vsComputer && i == activeBlueIdx) {
                // Aggressive Computer tracking intercept paths toward ball vector
                double dx = ballX - p.x;
                double dy = ballY - p.y;
                double d = Math.hypot(dx, dy);
                if (d > 5) {
                    p.x += (dx / d) * (aiSpeed + 0.8);
                    p.y += (dy / d) * (aiSpeed + 0.8);
                }
                keepInsidePitch(p);
            } else if (i != activeBlueIdx) {
                trackStrategicBase(p, ballX, ballY, false);
            }
        }
    }

    private void trackStrategicBase(LegoPlayer p, double bx, double by, boolean isRedTeam) {
        // Move towards standard home position shifted dynamically based on the ball's location
        double targetX = p.homeX + (bx - (marginX + pitchW/2)) * 0.35;
        double targetY = p.homeY + (by - (marginY + pitchH/2)) * 0.45;

        if (p.role.equals("GK")) { // Special constraint logic for Goalkeeper tracking limits
            targetX = isRedTeam ? marginX + 45 : marginX + pitchW - 45;
            targetY = Math.max(marginY + goalY, Math.min(marginY + goalY + goalH, by));
        }

        double dx = targetX - p.x;
        double dy = targetY - p.y;
        double dist = Math.hypot(dx, dy);

        if (dist > 6) {
            p.x += (dx / dist) * 1.8;
            p.y += (dy / dist) * 1.8;
        }
        keepInsidePitch(p);
    }

    private void keepInsidePitch(LegoPlayer p) {
        p.x = Math.max(marginX + p.radius, Math.min(marginX + pitchW - p.radius, p.x));
        p.y = Math.max(marginY + p.radius, Math.min(marginY + pitchH - p.radius, p.y));
    }

    private void updateBallVectorPhysics() {
        ballX += ballVX; ballY += ballVY;
        ballVX *= friction; ballVY *= friction;

        // Top/Bottom wall impacts
        if (ballY - ballRadius <= marginY || ballY + ballRadius >= marginY + pitchH) {
            ballVY = -ballVY;
            ballY = (ballY - ballRadius <= marginY) ? marginY + ballRadius : marginY + pitchH - ballRadius;
        }

        // Left/Right side lines (excluding the goal mouth area)
        if (ballY < marginY + goalY || ballY > marginY + goalY + goalH) {
            if (ballX - ballRadius <= marginX) {
                ballVX = -ballVX; ballX = marginX + ballRadius;
            }
            if (ballX + ballRadius >= marginX + pitchW) {
                ballVX = -ballVX; ballX = marginX + pitchW - ballRadius;
            }
        }

        // Handle structural figure collisions
        for (LegoPlayer p : teamRed) handleLegoKick(p);
        for (LegoPlayer p : teamBlue) handleLegoKick(p);
    }

    private void handleLegoKick(LegoPlayer p) {
        double dx = ballX - p.x;
        double dy = ballY - p.y;
        double dist = Math.hypot(dx, dy);
        double minDist = p.radius + ballRadius;

        if (dist < minDist) {
            double nX = dx / dist;
            double nY = dy / dist;

            // Instantly push the ball out of the player's collision bounds
            ballX = p.x + nX * minDist;
            ballY = p.y + nY * minDist;

            // Kick power simulation
            double force = 8.2;
            ballVX = nX * force;
            ballVY = nY * force;
        }
    }

    private void checkGoalIntersection() {
        if (ballX < marginX) {
            scoreBlue++;
            matchMsg = "LEGO BLUE SCORES!";
            msgTimer = 95;
            resetMatchPositions();
        } else if (ballX > marginX + pitchW) {
            scoreRed++;
            matchMsg = "LEGO RED SCORES!";
            msgTimer = 95;
            resetMatchPositions();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = true;
    }
    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = false;
    }
    @Override public void keyTyped(KeyEvent e) {}
}