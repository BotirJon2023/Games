// VirtualBaseballGame.java
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class VirtualBaseballGame extends JFrame {
    private GamePanel gamePanel;
    private JPanel controlPanel;
    private JLabel scoreLabel;
    private JLabel strikesLabel;
    private JLabel ballsLabel;
    private JLabel outsLabel;
    private JLabel inningLabel;

    private GameState gameState;
    private boolean isTwoPlayerMode;

    public VirtualBaseballGame() {
        setTitle("⚾ Virtual Baseball Game - Premium Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        // Show mode selection dialog
        showModeSelection();

        // Initialize game state
        gameState = new GameState();

        // Create components
        gamePanel = new GamePanel(gameState);
        createControlPanel();

        // Layout
        setLayout(new BorderLayout());
        add(gamePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // Keyboard controls
        setupKeyboardControls();

        setVisible(true);
        gamePanel.startGame();
    }

    private void showModeSelection() {
        int option = JOptionPane.showOptionDialog(
                null,
                "Select Game Mode:",
                "Virtual Baseball Game",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"2 Players (Local)", "vs Computer"},
                "2 Players (Local)"
        );
        isTwoPlayerMode = (option == JOptionPane.YES_OPTION);
    }

    private void createControlPanel() {
        controlPanel = new JPanel();
        controlPanel.setBackground(new Color(30, 30, 40));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        scoreLabel = createStyledLabel("Score: 0 - 0", new Color(255, 215, 0));
        strikesLabel = createStyledLabel("Strikes: 0", new Color(255, 100, 100));
        ballsLabel = createStyledLabel("Balls: 0", new Color(100, 255, 100));
        outsLabel = createStyledLabel("Outs: 0", new Color(255, 150, 100));
        inningLabel = createStyledLabel("Inning: 1", new Color(100, 200, 255));

        JButton resetButton = createStyledButton("🔄 New Game", new Color(70, 130, 200));
        resetButton.addActionListener(e -> resetGame());

        controlPanel.add(scoreLabel);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(strikesLabel);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(ballsLabel);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(outsLabel);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(inningLabel);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(resetButton);
    }

    private JLabel createStyledLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setForeground(color);
        return label;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        return button;
    }

    private void setupKeyboardControls() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        if (gameState.isPitching) {
                            handlePitchInput(e.getKeyCode());
                        } else if (gameState.isBatting) {
                            handleBattingInput(e.getKeyCode());
                        }
                    }
                    return false;
                });
    }

    private void handlePitchInput(int keyCode) {
        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN ||
                keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT) {

            String pitchType = "Fastball";
            if (keyCode == KeyEvent.VK_DOWN) pitchType = "Curveball";
            if (keyCode == KeyEvent.VK_LEFT) pitchType = "Slider";
            if (keyCode == KeyEvent.VK_RIGHT) pitchType = "Changeup";

            gameState.throwPitch(pitchType);
            gamePanel.repaint();

            // Update UI
            updateStatsDisplay();
        }
    }

    private void handleBattingInput(int keyCode) {
        if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ENTER) {
            gameState.swingBat();
            gamePanel.repaint();
            updateStatsDisplay();

            // Computer AI batting in single player mode
            if (!isTwoPlayerMode && !gameState.isPitching && gameState.isBatting) {
                Timer timer = new Timer(1000, e -> {
                    computerBat();
                    ((Timer)e.getSource()).stop();
                });
                timer.setRepeats(false);
                timer.start();
            }
        }
    }

    private void computerBat() {
        Random random = new Random();
        int swingTiming = random.nextInt(3); // 0=early, 1=perfect, 2=late
        gameState.computerSwing(swingTiming);
        gamePanel.repaint();
        updateStatsDisplay();
    }

    private void updateStatsDisplay() {
        scoreLabel.setText(String.format("Score: %d - %d",
                gameState.player1Score, gameState.player2Score));
        strikesLabel.setText("Strikes: " + gameState.strikes);
        ballsLabel.setText("Balls: " + gameState.balls);
        outsLabel.setText("Outs: " + gameState.outs);
        inningLabel.setText(String.format("Inning: %d.%d",
                gameState.inning, gameState.isTopInning ? 1 : 2));
    }

    private void resetGame() {
        gameState.reset();
        updateStatsDisplay();
        gamePanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new VirtualBaseballGame();
        });
    }
}

// GameState.java
class GameState {
    int player1Score = 0;
    int player2Score = 0;
    int strikes = 0;
    int balls = 0;
    int outs = 0;
    int inning = 1;
    boolean isTopInning = true;
    boolean isPitching = true;
    boolean isBatting = false;

    double pitchX = 500;
    double pitchY = 350;
    double batAngle = 0;
    boolean isSwinging = false;
    String lastHitResult = "";
    int hitPower = 0;

    Random random = new Random();

    void throwPitch(String pitchType) {
        isPitching = true;
        isBatting = true;

        // Animate pitch
        pitchX = 500;
        pitchY = 350 + random.nextInt(100) - 50;

        // Different pitch trajectories
        switch(pitchType) {
            case "Fastball":
                pitchX = 500;
                break;
            case "Curveball":
                pitchY += 30;
                break;
            case "Slider":
                pitchX -= 20;
                break;
            case "Changeup":
                pitchX += 10;
                break;
        }
    }

    void swingBat() {
        if (!isBatting) return;

        isSwinging = true;
        batAngle = 45;

        // Calculate hit result based on timing
        int timing = random.nextInt(100);
        if (timing < 30) {
            // Miss
            strikes++;
            lastHitResult = "STRIKE!";
            hitPower = 0;
            if (strikes >= 3) {
                recordOut();
            }
        } else if (timing < 60) {
            // Foul ball
            if (strikes < 2) {
                strikes++;
            }
            lastHitResult = "FOUL BALL!";
            hitPower = 0;
        } else if (timing < 85) {
            // Hit - single
            lastHitResult = "SINGLE! 🏃";
            hitPower = 70;
            handleHit(1);
        } else if (timing < 95) {
            // Double/Triple
            lastHitResult = "DOUBLE! ⚡";
            hitPower = 150;
            handleHit(2);
        } else {
            // Home run!
            lastHitResult = "HOME RUN! 🌟🏆";
            hitPower = 300;
            handleHit(4);
        }

        isBatting = false;
        isPitching = false;

        // Reset after animation
        Timer timer = new Timer(1500, e -> {
            isSwinging = false;
            batAngle = 0;
            lastHitResult = "";
            isPitching = true;
        });
        timer.setRepeats(false);
        timer.start();
    }

    void computerSwing(int swingTiming) {
        if (!isBatting) return;

        isSwinging = true;
        batAngle = 45;

        int hitChance;
        if (swingTiming == 1) { // Perfect timing
            hitChance = 85;
        } else {
            hitChance = 50;
        }

        if (random.nextInt(100) < hitChance) {
            int runs = random.nextInt(4) + 1;
            lastHitResult = runs >= 4 ? "HOME RUN! 🌟" : "HIT! ⚾";
            handleHit(runs);
        } else {
            strikes++;
            lastHitResult = "STRIKE!";
            if (strikes >= 3) {
                recordOut();
            }
        }

        isSwinging = false;
        batAngle = 0;
        isBatting = false;
        isPitching = true;
    }

    void handleHit(int bases) {
        int runsScored = bases;
        if (isTopInning) {
            player1Score += runsScored;
        } else {
            player2Score += runsScored;
        }

        // Reset count
        strikes = 0;
        balls = 0;

        // Check for inning end
        if (bases == 4) {
            // Home run excitement
            lastHitResult = "🏆 HOME RUN! +" + runsScored + " RUNS! 🏆";
        }

        // After hit, next batter
        if (random.nextInt(100) < 20) { // Random out chance
            recordOut();
        }
    }

    void recordOut() {
        outs++;
        strikes = 0;
        balls = 0;

        if (outs >= 3) {
            endInning();
        }
    }

    void endInning() {
        outs = 0;
        strikes = 0;
        balls = 0;
        isTopInning = !isTopInning;

        if (isTopInning) {
            inning++;
        }

        lastHitResult = "🏁 END OF INNING 🏁";
    }

    void reset() {
        player1Score = 0;
        player2Score = 0;
        strikes = 0;
        balls = 0;
        outs = 0;
        inning = 1;
        isTopInning = true;
        isPitching = true;
        isBatting = false;
        pitchX = 500;
        pitchY = 350;
        lastHitResult = "";
    }
}

// GamePanel.java
class GamePanel extends JPanel implements ActionListener {
    private GameState gameState;
    private Timer animationTimer;
    private double pitchProgress = 0;
    private double startPitchX, startPitchY;
    private double endPitchX, endPitchY;
    private boolean isAnimating = false;
    private List<Particle> particles = new ArrayList<>();
    private List<TrailEffect> trails = new ArrayList<>();

    public GamePanel(GameState gameState) {
        this.gameState = gameState;
        setBackground(new Color(20, 60, 30));
        setPreferredSize(new Dimension(1000, 600));

        animationTimer = new Timer(16, this);
        animationTimer.start();
    }

    void startGame() {
        gameState.isPitching = true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw field
        drawField(g2d);

        // Draw pitching mound
        drawPitchingMound(g2d);

        // Draw home plate
        drawHomePlate(g2d);

        // Draw ball with trail
        drawBall(g2d);

        // Draw bat
        drawBat(g2d);

        // Draw particles
        for (Particle p : particles) {
            p.draw(g2d);
        }

        // Draw trails
        for (TrailEffect t : trails) {
            t.draw(g2d);
        }

        // Draw status messages
        drawStatusMessages(g2d);

        // Draw instructions
        drawInstructions(g2d);
    }

    private void drawField(Graphics2D g2d) {
        // Grass gradient
        GradientPaint grassGradient = new GradientPaint(0, 0, new Color(34, 139, 34),
                0, getHeight(), new Color(20, 80, 20));
        g2d.setPaint(grassGradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Infield dirt
        g2d.setColor(new Color(210, 150, 75));
        g2d.fillOval(getWidth()/2 - 200, getHeight()/2 - 150, 400, 300);

        // Base paths
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(getWidth()/2, getHeight()/2 + 50, getWidth()/2 + 100, getHeight()/2 + 150);
        g2d.drawLine(getWidth()/2 + 100, getHeight()/2 + 150, getWidth()/2, getHeight()/2 + 250);
        g2d.drawLine(getWidth()/2, getHeight()/2 + 250, getWidth()/2 - 100, getHeight()/2 + 150);
        g2d.drawLine(getWidth()/2 - 100, getHeight()/2 + 150, getWidth()/2, getHeight()/2 + 50);

        // Draw bases
        drawBase(g2d, getWidth()/2 + 100, getHeight()/2 + 150, "1st");
        drawBase(g2d, getWidth()/2, getHeight()/2 + 250, "2nd");
        drawBase(g2d, getWidth()/2 - 100, getHeight()/2 + 150, "3rd");
    }

    private void drawBase(Graphics2D g2d, int x, int y, String label) {
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x - 15, y - 15, 30, 30);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString(label, x - 10, y - 18);
    }

    private void drawPitchingMound(Graphics2D g2d) {
        g2d.setColor(new Color(160, 100, 50));
        g2d.fillOval(getWidth()/2 - 30, getHeight()/2 + 30, 60, 60);
        g2d.setColor(Color.WHITE);
        g2d.drawOval(getWidth()/2 - 30, getHeight()/2 + 30, 60, 60);
    }

    private void drawHomePlate(Graphics2D g2d) {
        int[] xPoints = {getWidth()/2 - 15, getWidth()/2 + 15, getWidth()/2 + 10, getWidth()/2, getWidth()/2 - 10};
        int[] yPoints = {getHeight()/2 + 50, getHeight()/2 + 50, getHeight()/2 + 35, getHeight()/2 + 20, getHeight()/2 + 35};
        g2d.setColor(Color.WHITE);
        g2d.fillPolygon(xPoints, yPoints, 5);
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(xPoints, yPoints, 5);
    }

    private void drawBall(Graphics2D g2d) {
        double ballX, ballY;

        if (gameState.isPitching && !gameState.isBatting) {
            // Animate pitch
            if (isAnimating) {
                double t = pitchProgress;
                ballX = startPitchX + (endPitchX - startPitchX) * t;
                ballY = startPitchY + (endPitchY - startPitchY) * t;

                // Add trail
                if (t > 0.1) {
                    trails.add(new TrailEffect(ballX, ballY));
                }
            } else {
                ballX = getWidth()/2 - 100;
                ballY = getHeight()/2;
            }
        } else {
            ballX = gameState.pitchX;
            ballY = gameState.pitchY;
        }

        // Ball with 3D effect
        RadialGradientPaint ballPaint = new RadialGradientPaint(
                (float)ballX, (float)ballY, 15,
                new float[]{0f, 1f},
                new Color[]{Color.WHITE, new Color(200, 200, 200)}
        );
        g2d.setPaint(ballPaint);
        g2d.fillOval((int)ballX - 10, (int)ballY - 10, 20, 20);

        // Stitching
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawArc((int)ballX - 7, (int)ballY - 5, 14, 10, 0, 180);
        g2d.drawArc((int)ballX - 7, (int)ballY + 5, 14, 10, 0, -180);
    }

    private void drawBat(Graphics2D g2d) {
        if (gameState.isSwinging || gameState.batAngle > 0) {
            g2d.rotate(Math.toRadians(gameState.batAngle), getWidth()/2, getHeight()/2 + 60);
        }

        // Wood texture
        GradientPaint batPaint = new GradientPaint(
                getWidth()/2 - 5, getHeight()/2 + 40,
                new Color(139, 69, 19),
                getWidth()/2 + 5, getHeight()/2 + 100,
                new Color(101, 67, 33)
        );
        g2d.setPaint(batPaint);
        g2d.fillRoundRect(getWidth()/2 - 8, getHeight()/2 + 40, 16, 80, 8, 8);

        if (gameState.isSwinging || gameState.batAngle > 0) {
            g2d.rotate(-Math.toRadians(gameState.batAngle), getWidth()/2, getHeight()/2 + 60);
        }
    }

    private void drawStatusMessages(Graphics2D g2d) {
        if (!gameState.lastHitResult.isEmpty()) {
            g2d.setFont(new Font("Arial", Font.BOLD, 36));

            // Glow effect
            for (int i = 0; i < 3; i++) {
                g2d.setColor(new Color(255, 215, 0, 100 - i * 30));
                g2d.drawString(gameState.lastHitResult,
                        getWidth()/2 - 150 + i, getHeight()/2 - 100 + i);
            }

            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString(gameState.lastHitResult, getWidth()/2 - 150, getHeight()/2 - 100);
        }

        // Current batter info
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(Color.WHITE);
        String status = gameState.isPitching ? "⚾ PITCHER'S TURN" : "🏏 BATTER'S TURN";
        g2d.drawString(status, getWidth()/2 - 100, 50);
    }

    private void drawInstructions(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.setColor(new Color(255, 255, 255, 200));

        if (gameState.isPitching) {
            g2d.drawString("↑ Fastball | ↓ Curveball | ← Slider | → Changeup", 20, getHeight() - 20);
        } else {
            g2d.drawString("SPACE or ENTER to Swing!", 20, getHeight() - 20);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Update animations
        if (isAnimating) {
            pitchProgress += 0.05;
            if (pitchProgress >= 1) {
                isAnimating = false;
                pitchProgress = 0;
            }
        }

        // Update particles
        particles.removeIf(p -> !p.update());

        // Update trails
        trails.removeIf(t -> !t.update());

        repaint();
    }

    // Inner class for particle effects
    class Particle {
        double x, y;
        double vx, vy;
        int life;
        Color color;

        Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.vx = (Math.random() - 0.5) * 5;
            this.vy = (Math.random() - 0.5) * 5;
            this.life = 30;
            this.color = color;
        }

        boolean update() {
            x += vx;
            y += vy;
            life--;
            return life > 0;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), life * 8));
            g2d.fillOval((int)x, (int)y, 4, 4);
        }
    }

    // Inner class for trail effects
    class TrailEffect {
        double x, y;
        int life;

        TrailEffect(double x, double y) {
            this.x = x;
            this.y = y;
            this.life = 20;
        }

        boolean update() {
            life--;
            return life > 0;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(255, 255, 255, life * 10));
            g2d.fillOval((int)x - 5, (int)y - 5, 10, 10);
        }
    }
}