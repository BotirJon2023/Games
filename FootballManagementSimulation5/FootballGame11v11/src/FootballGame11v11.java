import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

public class FootballGame11v11 extends JFrame {
    private static final int WIDTH = 1100;
    private static final int HEIGHT = 700;

    public FootballGame11v11() {
        setTitle("Strategic 11v11 Football Management Simulation");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        add(new MenuPanel(this));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FootballGame11v11().setVisible(true));
    }
}

class MenuPanel extends JPanel {
    private FootballGame11v11 frame;

    public MenuPanel(FootballGame11v11 frame) {
        this.frame = frame;
        setLayout(new GridBagLayout());
        setBackground(new Color(24, 105, 24));

        JLabel title = new JLabel("PRO FOOTBALL 11v11");
        title.setFont(new Font("Impact", Font.ITALIC, 54));
        title.setForeground(Color.WHITE);

        JButton pvpButton = createStyledButton("Player vs Player (Local)");
        JButton pvcButton = createStyledButton("Player vs Computer (AI)");

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
        btn.setFont(new Font("Arial", Font.BOLD, 18));
        btn.setPreferredSize(new Dimension(320, 55));
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(24, 105, 24));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 3));
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

class PlayerEntity {
    double x, y;
    double homeX, homeY; // Strategic base position
    double radius = 14;
    int id;
    String role; // "GK", "DEF", "MID", "FWD"

    public PlayerEntity(int id, String role, double homeX, double homeY) {
        this.id = id;
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

    // Scores & Canvas UI
    private int scoreTeam1 = 0;
    private int scoreTeam2 = 0;
    private String bannerMessage = "MATCH START";
    private int bannerTimer = 90;

    // Ball configurations
    private double ballX = 550, ballY = 350, ballRadius = 10;
    private double ballVX = 0, ballVY = 0;
    private final double friction = 0.985;

    // Squad rosters (11 Players per Team)
    private ArrayList<PlayerEntity> team1 = new ArrayList<>();
    private ArrayList<PlayerEntity> team2 = new ArrayList<>();

    // Controlled Indices tracking active user movement selection
    private int activeP1Index = 0;
    private int activeP2Index = 0;

    // Field System Framework
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

        initializeFormations();
        timer = new Timer(16, this); // ~60 FPS Loop
        timer.start();
    }

    private void initializeFormations() {
        team1.clear(); team2.clear();

        // --- Team 1 (Red) Formation Matrix (4-4-2 Structure) ---
        team1.add(new PlayerEntity(0, "GK",  marginX + 40,   marginY + pitchH/2));
        team1.add(new PlayerEntity(1, "DEF", marginX + 180,  marginY + 120));
        team1.add(new PlayerEntity(2, "DEF", marginX + 160,  marginY + 240));
        team1.add(new PlayerEntity(3, "DEF", marginX + 160,  marginY + 340));
        team1.add(new PlayerEntity(4, "DEF", marginX + 180,  marginY + 460));
        team1.add(new PlayerEntity(5, "MID", marginX + 400,  marginY + 100));
        team1.add(new PlayerEntity(6, "MID", marginX + 380,  marginY + 220));
        team1.add(new PlayerEntity(7, "MID", marginX + 380,  marginY + 360));
        team1.add(new PlayerEntity(8, "MID", marginX + 400,  marginY + 480));
        team1.add(new PlayerEntity(9, "FWD", marginX + 500,  marginY + 200));
        team1.add(new PlayerEntity(10,"FWD", marginX + 500,  marginY + 380));

        // --- Team 2 (Blue) Formation Matrix (4-4-2 Mirror Structure) ---
        team2.add(new PlayerEntity(0, "GK",  marginX + pitchW - 40,  marginY + pitchH/2));
        team2.add(new PlayerEntity(1, "DEF", marginX + pitchW - 180, marginY + 120));
        team2.add(new PlayerEntity(2, "DEF", marginX + pitchW - 160, marginY + 240));
        team2.add(new PlayerEntity(3, "DEF", marginX + pitchW - 160, marginY + 340));
        team2.add(new PlayerEntity(4, "DEF", marginX + pitchW - 180, marginY + 460));
        team2.add(new PlayerEntity(5, "MID", marginX + pitchW - 400, marginY + 100));
        team2.add(new PlayerEntity(6, "MID", marginX + pitchW - 380, marginY + 220));
        team2.add(new PlayerEntity(7, "MID", marginX + pitchW - 380, marginY + 360));
        team2.add(new PlayerEntity(8, "MID", marginX + pitchW - 400, marginY + 480));
        team2.add(new PlayerEntity(9, "FWD", marginX + pitchW - 500, marginY + 200));
        team2.add(new PlayerEntity(10,"FWD", marginX + pitchW - 500, marginY + 380));

        resetField();
    }

    private void resetField() {
        ballX = marginX + pitchW / 2;
        ballY = marginY + pitchH / 2;
        ballVX = 0; ballVY = 0;
        for (PlayerEntity p : team1) p.resetToHome();
        for (PlayerEntity p : team2) p.resetToHome();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawStadiumPitch(g2d);
        drawSquads(g2d);
        drawBall(g2d);
        drawOverlayUI(g2d);
    }

    private void drawStadiumPitch(Graphics2D g2d) {
        // High-fidelity lawn stripes rendering
        for (int i = 0; i < 14; i++) {
            g2d.setColor(i % 2 == 0 ? new Color(42, 152, 42) : new Color(48, 165, 48));
            g2d.fillRect(marginX + (i * (pitchW / 14)), marginY, pitchW / 14, pitchH);
        }

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(marginX, marginY, pitchW, pitchH); // Boundary line
        g2d.drawLine(marginX + pitchW/2, marginY, marginX + pitchW/2, marginY + pitchH); // Halfway line
        g2d.drawOval(marginX + pitchW/2 - 80, marginY + pitchH/2 - 80, 160, 160); // Center Circle

        // Penalty boxes
        g2d.drawRect(marginX, marginY + 140, 140, 300);
        g2d.drawRect(marginX + pitchW - 140, marginY + 140, 140, 300);

        // Goalmouth physical structures
        g2d.setColor(Color.WHITE);
        g2d.fillRect(marginX - goalW, marginY + goalY, goalW, goalH);
        g2d.fillRect(marginX + pitchW, marginY + goalY, goalW, goalH);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(marginX - goalW, marginY + goalY, goalW, goalH);
        g2d.drawRect(marginX + pitchW, marginY + goalY, goalW, goalH);
    }

    private void drawSquads(Graphics2D g2d) {
        // Draw Team 1 (Red Squad)
        for (int i = 0; i < team1.size(); i++) {
            PlayerEntity p = team1.get(i);
            g2d.setColor(new Color(215, 35, 35));
            g2d.fill(new Ellipse2D.Double(p.x - p.radius, p.y - p.radius, p.radius*2, p.radius*2));
            g2d.setColor(Color.WHITE);
            g2d.drawOval((int)(p.x - p.radius), (int)(p.y - p.radius), (int)p.radius*2, (int)p.radius*2);

            // Visual dynamic cursor representing active manual user ownership selection
            if (i == activeP1Index) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval((int)(p.x - p.radius - 4), (int)(p.y - p.radius - 4), (int)p.radius*2 + 8, (int)p.radius*2 + 8);
            }
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(p.role, (int)p.x - 8, (int)p.y + 4);
        }

        // Draw Team 2 (Blue Squad)
        for (int i = 0; i < team2.size(); i++) {
            PlayerEntity p = team2.get(i);
            g2d.setColor(new Color(30, 80, 220));
            g2d.fill(new Ellipse2D.Double(p.x - p.radius, p.y - p.radius, p.radius*2, p.radius*2));
            g2d.setColor(Color.WHITE);
            g2d.drawOval((int)(p.x - p.radius), (int)(p.y - p.radius), (int)p.radius*2, (int)p.radius*2);

            if (i == activeP2Index) {
                g2d.setColor(Color.CYAN);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval((int)(p.x - p.radius - 4), (int)(p.y - p.radius - 4), (int)p.radius*2 + 8, (int)p.radius*2 + 8);
            }
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(p.role, (int)p.x - 8, (int)p.y + 4);
        }
    }

    private void drawBall(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.fill(new Ellipse2D.Double(ballX - ballRadius, ballY - ballRadius, ballRadius * 2, ballRadius * 2));
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawOval((int)(ballX - ballRadius), (int)(ballY - ballRadius), (int)ballRadius * 2, (int)ballRadius * 2);
        g2d.fillOval((int)ballX - 2, (int)ballY - 2, 4, 4); // Synthetic core dot
    }

    private void drawOverlayUI(Graphics2D g2d) {
        // Score Display panel
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(450, 12, 200, 42);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 22));
        g2d.drawString("RED " + scoreTeam1 + ":" + scoreTeam2 + " BLU", 475, 40);

        // Splendid Action Display text overlay animations
        if (bannerTimer > 0) {
            g2d.setFont(new Font("Impact", Font.PLAIN, 50));
            g2d.setColor(Color.ORANGE);
            g2d.drawString(bannerMessage, 400, 340);
            bannerTimer--;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        selectClosestActivePlayers();
        processUserInputs();
        executeSquadAI();
        simulateBallPhysics();
        detectGoalScored();
        repaint();
    }

    // Automatically assigns user controls to the team member closest to the ball
    private void selectClosestActivePlayers() {
        double minDistP1 = Double.MAX_VALUE;
        for (int i = 0; i < team1.size(); i++) {
            double dist = Math.hypot(team1.get(i).x - ballX, team1.get(i).y - ballY);
            if (dist < minDistP1) { minDistP1 = dist; activeP1Index = i; }
        }

        if (!vsComputer) {
            double minDistP2 = Double.MAX_VALUE;
            for (int i = 0; i < team2.size(); i++) {
                double dist = Math.hypot(team2.get(i).x - ballX, team2.get(i).y - ballY);
                if (dist < minDistP2) { minDistP2 = dist; activeP2Index = i; }
            }
        } else {
            // Under AI simulation mode, auto-select closest computer player to track targets
            double minDistP2 = Double.MAX_VALUE;
            for (int i = 0; i < team2.size(); i++) {
                double dist = Math.hypot(team2.get(i).x - ballX, team2.get(i).y - ballY);
                if (dist < minDistP2) { minDistP2 = dist; activeP2Index = i; }
            }
        }
    }

    private void processUserInputs() {
        double runSpeed = 4.0;
        PlayerEntity p1 = team1.get(activeP1Index);

        if (keys[KeyEvent.VK_W]) p1.y -= runSpeed;
        if (keys[KeyEvent.VK_S]) p1.y += runSpeed;
        if (keys[KeyEvent.VK_A]) p1.x -= runSpeed;
        if (keys[KeyEvent.VK_D]) p1.x += runSpeed;
        boundPlayer(p1);

        if (!vsComputer) {
            PlayerEntity p2 = team2.get(activeP2Index);
            if (keys[KeyEvent.VK_UP])    p2.y -= runSpeed;
            if (keys[KeyEvent.VK_DOWN])  p2.y += runSpeed;
            if (keys[KeyEvent.VK_LEFT])  p2.x -= runSpeed;
            if (keys[KeyEvent.VK_RIGHT]) p2.x += runSpeed;
            boundPlayer(p2);
        }
    }

    private void executeSquadAI() {
        double aiSpeed = 2.2;

        // Team 1 Non-controlled Tactical Support Behavior
        for (int i = 0; i < team1.size(); i++) {
            if (i == activeP1Index) continue; // Skip human control assignment
            PlayerEntity p = team1.get(i);
            simulateTacticalPositioning(p, ballX, ballY, true);
        }

        // Team 2 (Blue Team) Simulation Processing Loop
        for (int i = 0; i < team2.size(); i++) {
            PlayerEntity p = team2.get(i);
            if (vsComputer && i == activeP2Index) {
                // Active Challenger AI pushes aggressive tackle paths toward the ball
                double dx = ballX - p.x;
                double dy = ballY - p.y;
                double dist = Math.hypot(dx, dy);
                if (dist > 5) {
                    p.x += (dx / dist) * (aiSpeed + 0.8);
                    p.y += (dy / dist) * (aiSpeed + 0.8);
                }
                boundPlayer(p);
            } else if (i != activeP2Index) {
                simulateTacticalPositioning(p, ballX, ballY, false);
            }
        }
    }

    private void simulateTacticalPositioning(PlayerEntity p, double bx, double by, boolean isTeam1) {
        // Target shifting parameters based on ball location offsets
        double targetX = p.homeX + (bx - (marginX + pitchW/2)) * 0.35;
        double targetY = p.homeY + (by - (marginY + pitchH/2)) * 0.4;

        if (p.role.equals("GK")) { // Goalkeeper restrictive bounds framework
            targetX = isTeam1 ? marginX + 35 : marginX + pitchW - 35;
            targetY = Math.max(marginY + goalY, Math.min(marginY + goalY + goalH, by));
        }

        double dx = targetX - p.x;
        double dy = targetY - p.y;
        double dist = Math.hypot(dx, dy);

        double rate = 1.8;
        if (dist > 5) {
            p.x += (dx / dist) * rate;
            p.y += (dy / dist) * rate;
        }
        boundPlayer(p);
    }

    private void boundPlayer(PlayerEntity p) {
        p.x = Math.max(marginX + p.radius, Math.min(marginX + pitchW - p.radius, p.x));
        p.y = Math.max(marginY + p.radius, Math.min(marginY + pitchH - p.radius, p.y));
    }

    private void simulateBallPhysics() {
        ballX += ballVX; ballY += ballVY;
        ballVX *= friction; ballVY *= friction;

        // Border limits bounces configurations
        if (ballY - ballRadius <= marginY || ballY + ballRadius >= marginY + pitchH) {
            ballVY = -ballVY;
            ballY = (ballY - ballRadius <= marginY) ? marginY + ballRadius : marginY + pitchH - ballRadius;
        }

        // Side borders (excluding target netting regions)
        if (ballY < marginY + goalY || ballY > marginY + goalY + goalH) {
            if (ballX - ballRadius <= marginX) {
                ballVX = -ballVX; ballX = marginX + ballRadius;
            }
            if (ballX + ballRadius >= marginX + pitchW) {
                ballVX = -ballVX; ballX = marginX + pitchW - ballRadius;
            }
        }

        // Check contact collisions across entire rosters
        for (PlayerEntity p : team1) handleContactElasticity(p);
        for (PlayerEntity p : team2) handleContactElasticity(p);
    }

    private void handleContactElasticity(PlayerEntity p) {
        double dx = ballX - p.x;
        double dy = ballY - p.y;
        double dist = Math.hypot(dx, dy);
        double minDist = p.radius + ballRadius;

        if (dist < minDist) {
            double nX = dx / dist;
            double nY = dy / dist;

            // Push processing out of structural overlaps
            ballX = p.x + nX * minDist;
            ballY = p.y + nY * minDist;

            double kineticForce = 7.8;
            ballVX = nX * kineticForce;
            ballVY = nY * kineticForce;
        }
    }

    private void detectGoalScored() {
        if (ballX < marginX) {
            scoreTeam2++;
            bannerMessage = "GOAL FOR BLUE!";
            bannerTimer = 95;
            resetField();
        } else if (ballX > marginX + pitchW) {
            scoreTeam1++;
            bannerMessage = "GOAL FOR RED!";
            bannerTimer = 95;
            resetField();
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