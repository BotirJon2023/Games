import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;

public class FootballGame extends JFrame {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 650;

    public FootballGame() {
        setTitle("Retro Football Management Simulation");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        // Start with the Menu Panel
        add(new MenuPanel(this));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new FootballGame().setVisible(true);
        });
    }
}

class MenuPanel extends JPanel {
    private JButton pvpButton;
    private JButton pvcButton;
    private FootballGame frame;

    public MenuPanel(FootballGame frame) {
        this.frame = frame;
        setLayout(new GridBagLayout());
        setBackground(new Color(34, 139, 34)); // Pitch Green

        JLabel title = new JLabel("FOOTBALL SIMULATION");
        title.setFont(new Font("Arial", Font.BOLD, 48));
        title.setForeground(Color.WHITE);

        pvpButton = createStyledButton("Player vs Player (Local)");
        pvcButton = createStyledButton("Player vs Computer (AI)");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(15, 15, 15, 15);

        gbc.gridy = 0;
        add(title, gbc);
        gbc.gridy = 1;
        add(pvpButton, gbc);
        gbc.gridy = 2;
        add(pvcButton, gbc);

        pvpButton.addActionListener(e -> startGame(false));
        pvcButton.addActionListener(e -> startGame(true));
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 20));
        btn.setPreferredSize(new Dimension(300, 50));
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(34, 139, 34));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 2));
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

class GamePanel extends JPanel implements ActionListener, KeyListener {
    // Game Mode
    private boolean vsComputer;

    // Game Loop (60 FPS)
    private Timer timer;

    // Scores
    private int scoreTeam1 = 0;
    private int scoreTeam2 = 0;
    private String goalMessage = "";
    private int goalDisplayTimer = 0;

    // Entities (X, Y, Radius, Speed X, Speed Y)
    private double p1X = 200, p1Y = 300, p1Radius = 25, p1Speed = 5;
    private double p2X = 800, p2Y = 300, p2Radius = 25, p2Speed = 5;
    private double ballX = 500, ballY = 300, ballRadius = 15;
    private double ballVX = 0, ballVY = 0;
    private double friction = 0.98;

    // Pitch Dimensions
    private final int marginX = 50;
    private final int marginY = 50;
    private final int pitchWidth = 900;
    private final int pitchHeight = 500;
    private final int goalWidth = 15;
    private final int goalHeight = 150;
    private final int goalY = 225; // Center-aligned goal

    // Controls tracking
    private boolean[] keys = new boolean[256];

    public GamePanel(boolean vsComputer) {
        this.vsComputer = vsComputer;
        setFocusable(true);
        addKeyListener(this);
        setBackground(new Color(42, 170, 42));

        // Start Game Loop
        timer = new Timer(16, this); // ~60 FPS
        timer.start();
        resetPositions();
    }

    private void resetPositions() {
        p1X = 200; p1Y = 325;
        p2X = 800; p2Y = 325;
        ballX = 500; ballY = 325;
        ballVX = 0; ballVY = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawPitch(g2d);
        drawEntities(g2d);
        drawUI(g2d);
    }

    private void drawPitch(Graphics2D g2d) {
        // Grass lines (alternating green stripes for beauty)
        for (int i = 0; i < 9; i++) {
            if (i % 2 == 0) g2d.setColor(new Color(34, 145, 34));
            else g2d.setColor(new Color(40, 160, 40));
            g2d.fillRect(marginX + (i * (pitchWidth / 9)), marginY, pitchWidth / 9, pitchHeight);
        }

        // Pitch Boundaries
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect(marginX, marginY, pitchWidth, pitchHeight);

        // Center Line & Center Circle
        g2d.drawLine(marginX + pitchWidth / 2, marginY, marginX + pitchWidth / 2, marginY + pitchHeight);
        g2d.drawOval(marginX + pitchWidth / 2 - 75, marginY + pitchHeight / 2 - 75, 150, 150);
        g2d.fillOval(marginX + pitchWidth / 2 - 5, marginY + pitchHeight / 2 - 5, 10, 10);

        // Penalty Areas
        g2d.drawRect(marginX, marginY + 125, 120, 250);
        g2d.drawRect(marginX + pitchWidth - 120, marginY + 125, 120, 250);

        // Left Goal Post
        g2d.setColor(Color.WHITE);
        g2d.fillRect(marginX - goalWidth, marginY + goalY, goalWidth, goalHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(marginX - goalWidth, marginY + goalY, goalWidth, goalHeight);

        // Right Goal Post
        g2d.setColor(Color.WHITE);
        g2d.fillRect(marginX + pitchWidth, marginY + goalY, goalWidth, goalHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(marginX + pitchWidth, marginY + goalY, goalWidth, goalHeight);
    }

    private void drawEntities(Graphics2D g2d) {
        // Player 1 (Red)
        g2d.setColor(new Color(220, 53, 69));
        g2d.fill(new Ellipse2D.Double(p1X - p1Radius, p1Y - p1Radius, p1Radius * 2, p1Radius * 2));
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval((int)(p1X - p1Radius), (int)(p1Y - p1Radius), (int)p1Radius * 2, (int)p1Radius * 2);
        g2d.drawString("P1", (int)p1X - 7, (int)p1Y + 5);

        // Player 2 / AI (Blue)
        g2d.setColor(new Color(13, 110, 253));
        g2d.fill(new Ellipse2D.Double(p2X - p2Radius, p2Y - p2Radius, p2Radius * 2, p2Radius * 2));
        g2d.setColor(Color.WHITE);
        g2d.drawOval((int)(p2X - p2Radius), (int)(p2Y - p2Radius), (int)p2Radius * 2, (int)p2Radius * 2);
        g2d.drawString(vsComputer ? "AI" : "P2", (int)p2X - 7, (int)p2Y + 5);

        // The Football
        g2d.setColor(Color.WHITE);
        g2d.fill(new Ellipse2D.Double(ballX - ballRadius, ballY - ballRadius, ballRadius * 2, ballRadius * 2));
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawOval((int)(ballX - ballRadius), (int)(ballY - ballRadius), (int)ballRadius * 2, (int)ballRadius * 2);
        // Pentagonal design patterns on the ball
        g2d.fillOval((int)ballX - 3, (int)ballY - 3, 6, 6);
    }

    private void drawUI(Graphics2D g2d) {
        // Scoreboard
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(400, 10, 200, 40);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 24));
        String scoreText = scoreTeam1 + "  -  " + scoreTeam2;
        g2d.drawString(scoreText, 445, 38);

        // Goal celebration text overlay
        if (goalDisplayTimer > 0) {
            g2d.setFont(new Font("Arial", Font.BOLD, 60));
            g2d.setColor(Color.YELLOW);
            g2d.drawString(goalMessage, 320, 300);
            goalDisplayTimer--;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        movePlayers();
        if (vsComputer) handleAI();
        moveBall();
        checkCollisions();
        checkGoals();
        repaint(); // Re-render animation Frame
    }

    private void movePlayers() {
        // Player 1 movement (WASD)
        if (keys[KeyEvent.VK_W]) p1Y -= p1Speed;
        if (keys[KeyEvent.VK_S]) p1Y += p1Speed;
        if (keys[KeyEvent.VK_A]) p1X -= p1Speed;
        if (keys[KeyEvent.VK_D]) p1X += p1Speed;

        // Player 2 movement (Arrow Keys) - only active if not simulation AI mode
        if (!vsComputer) {
            if (keys[KeyEvent.VK_UP]) p2Y -= p2Speed;
            if (keys[KeyEvent.VK_DOWN]) p2Y += p2Speed;
            if (keys[KeyEvent.VK_LEFT]) p2X -= p2Speed;
            if (keys[KeyEvent.VK_RIGHT]) p2X += p2Speed;
        }

        // Bound players to pitch constraints
        p1X = Math.max(marginX + p1Radius, Math.min(marginX + pitchWidth - p1Radius, p1X));
        p1Y = Math.max(marginY + p1Radius, Math.min(marginY + pitchHeight - p1Radius, p1Y));
        p2X = Math.max(marginX + p1Radius, Math.min(marginX + pitchWidth - p1Radius, p2X));
        p2Y = Math.max(marginY + p1Radius, Math.min(marginY + pitchHeight - p1Radius, p2Y));
    }

    private void handleAI() {
        // Simple fluid AI tracking logic
        if (ballX > marginX + pitchWidth / 2 - 100) { // AI engages when ball crosses midfield
            if (p2Y < ballY - 10) p2Y += p2Speed - 1.5;
            else if (p2Y > ballY + 10) p2Y -= p2Speed - 1.5;

            if (p2X < ballX - 5) p2X += p2Speed - 1.5;
            else if (p2X > ballX + 5) p2X -= p2Speed - 1.5;
        } else {
            // Return back to defensive home base position
            if (p2X < 750) p2X += p2Speed - 2;
            if (p2X > 750) p2X -= p2Speed - 2;
            if (p2Y < 300) p2Y += p2Speed - 2;
            if (p2Y > 300) p2Y -= p2Speed - 2;
        }
    }

    private void moveBall() {
        ballX += ballVX;
        ballY += ballVY;

        // Apply natural momentum deceleration (Friction)
        ballVX *= friction;
        ballVY *= friction;

        // Wall Bounces (Top and Bottom)
        if (ballY - ballRadius <= marginY) {
            ballY = marginY + ballRadius;
            ballVY = -ballVY;
        }
        if (ballY + ballRadius >= marginY + pitchHeight) {
            ballY = marginY + pitchHeight - ballRadius;
            ballVY = -ballVY;
        }

        // Side Bounces (Outside the Goal lines)
        if (ballY < marginY + goalY || ballY > marginY + goalY + goalHeight) {
            if (ballX - ballRadius <= marginX) {
                ballX = marginX + ballRadius;
                ballVX = -ballVX;
            }
            if (ballX + ballRadius >= marginX + pitchWidth) {
                ballX = marginX + pitchWidth - ballRadius;
                ballVX = -ballVX;
            }
        }
    }

    private void checkCollisions() {
        handleElasticCollision(p1X, p1Y, p1Radius);
        handleElasticCollision(p2X, p2Y, p2Radius);
    }

    private void handleElasticCollision(double pX, double pY, double pRad) {
        double deltaX = ballX - pX;
        double deltaY = ballY - pY;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        double minDist = pRad + ballRadius;

        if (distance < minDist) {
            // Calculate Normal Vector
            double nX = deltaX / distance;
            double nY = deltaY / distance;

            // Push ball out of overlapping player mask
            ballX = pX + nX * minDist;
            ballY = pY + nY * minDist;

            // Transfer structural force vector instantly (Simulating Kicking Mechanics)
            double kickPower = 7.5;
            ballVX = nX * kickPower;
            ballVY = nY * kickPower;
        }
    }

    private void checkGoals() {
        // Left Goal Scored (Team 2 Scores)
        if (ballX < marginX) {
            scoreTeam2++;
            triggerGoalAnimation("GOAL FOR BLUE!");
        }
        // Right Goal Scored (Team 1 Scores)
        else if (ballX > marginX + pitchWidth) {
            scoreTeam1++;
            triggerGoalAnimation("GOAL FOR RED!");
        }
    }

    private void triggerGoalAnimation(String msg) {
        goalMessage = msg;
        goalDisplayTimer = 90; // Display for ~1.5 seconds
        resetPositions();
    }

    // Key Events Mapping
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code < keys.length) keys[code] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code < keys.length) keys[code] = false;
    }

    @Override public void keyTyped(KeyEvent e) {}
}