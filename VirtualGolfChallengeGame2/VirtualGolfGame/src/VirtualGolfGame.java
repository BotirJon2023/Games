import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VirtualGolfGame extends JFrame {
    private GamePanel gamePanel;
    private JLabel statusLabel;
    private JButton player1Btn, player2Btn, computerBtn, resetBtn;

    public VirtualGolfGame() {
        setTitle("🏖️ Virtual Golf Challenge - Seaside Beach");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Game panel
        gamePanel = new GamePanel();
        mainPanel.add(gamePanel, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        controlPanel.setBackground(new Color(20, 40, 80));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        statusLabel = new JLabel("Select Game Mode");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusLabel.setForeground(Color.WHITE);

        player1Btn = createStyledButton("👤 2 Players", new Color(255, 165, 0));
        computerBtn = createStyledButton("🤖 vs Computer", new Color(50, 205, 50));
        resetBtn = createStyledButton("🔄 New Game", new Color(220, 20, 60));

        player1Btn.addActionListener(e -> gamePanel.startGame(2));
        computerBtn.addActionListener(e -> gamePanel.startGame(1));
        resetBtn.addActionListener(e -> gamePanel.resetGame());

        controlPanel.add(statusLabel);
        controlPanel.add(Box.createHorizontalStrut(30));
        controlPanel.add(player1Btn);
        controlPanel.add(computerBtn);
        controlPanel.add(resetBtn);

        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        add(mainPanel);

        // Timer to update status
        Timer statusTimer = new Timer(100, e -> updateStatus());
        statusTimer.start();
    }

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createRaisedBevelBorder());
        btn.setPreferredSize(new Dimension(150, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.brighter());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });
        return btn;
    }

    private void updateStatus() {
        if (gamePanel.gameState == GameState.PLAYING) {
            String current = gamePanel.currentPlayer == 1 ? "Player 1 (Blue)" :
                    (gamePanel.vsComputer ? "Computer (Red)" : "Player 2 (Red)");
            statusLabel.setText("Turn: " + current + " | Strokes: " +
                    gamePanel.players[gamePanel.currentPlayer-1].strokes);
        } else if (gamePanel.gameState == GameState.GAME_OVER) {
            statusLabel.setText(gamePanel.winnerMessage);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new VirtualGolfGame().setVisible(true);
        });
    }
}

enum GameState { MENU, PLAYING, AIMING, BALL_MOVING, GAME_OVER }

class Player {
    int strokes = 0;
    Color ballColor;
    Point2D.Double position;
    boolean finished = false;
    int score = 0;

    public Player(Color color, Point2D.Double startPos) {
        this.ballColor = color;
        this.position = startPos;
    }
}

class GamePanel extends JPanel {
    GameState gameState = GameState.MENU;
    Player[] players = new Player[2];
    int currentPlayer = 1;
    boolean vsComputer = false;
    String winnerMessage = "";

    // Game constants
    Point2D.Double holePosition = new Point2D.Double(1000, 350);
    double holeRadius = 20;
    Point2D.Double startPosition = new Point2D.Double(150, 350);

    // Physics
    Point2D.Double ballVelocity = new Point2D.Double(0, 0);
    double friction = 0.985;
    double stopThreshold = 0.15;

    // Aiming
    Point mousePos = new Point();
    boolean dragging = false;
    double maxPower = 25;

    // Animation
    double waterOffset = 0;
    double cloudOffset = 0;
    double flagWave = 0;
    List<Point2D.Double> sandParticles = new ArrayList<>();
    Random random = new Random();

    // Obstacles
    List<Rectangle2D.Double> sandTraps = new ArrayList<>();
    List<Rectangle2D.Double> waterHazards = new ArrayList<>();
    List<Rectangle2D.Double> trees = new ArrayList<>();

    // Particles
    List<Particle> particles = new ArrayList<>();

    // Computer AI
    Timer computerTimer;
    double computerPower = 0;
    double computerAngle = 0;

    public GamePanel() {
        setPreferredSize(new Dimension(1200, 700));
        setBackground(new Color(135, 206, 235));
        setFocusable(true);

        // Initialize obstacles
        sandTraps.add(new Rectangle2D.Double(400, 300, 120, 100));
        sandTraps.add(new Rectangle2D.Double(700, 450, 100, 80));
        waterHazards.add(new Rectangle2D.Double(550, 200, 150, 60));
        trees.add(new Rectangle2D.Double(300, 250, 40, 40));
        trees.add(new Rectangle2D.Double(800, 300, 50, 50));
        trees.add(new Rectangle2D.Double(600, 500, 45, 45));

        // Generate sand particles
        for (int i = 0; i < 50; i++) {
            sandParticles.add(new Point2D.Double(
                    random.nextDouble() * 1200,
                    550 + random.nextDouble() * 150
            ));
        }

        // Mouse listeners for aiming
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (gameState == GameState.PLAYING && currentPlayer == 1 ||
                        (gameState == GameState.PLAYING && !vsComputer)) {
                    dragging = true;
                    mousePos = e.getPoint();
                    gameState = GameState.AIMING;
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (gameState == GameState.AIMING) {
                    shootBall();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (gameState == GameState.AIMING) {
                    mousePos = e.getPoint();
                    repaint();
                }
            }

            public void mouseMoved(MouseEvent e) {
                mousePos = e.getPoint();
                if (gameState == GameState.PLAYING || gameState == GameState.AIMING) {
                    repaint();
                }
            }
        });

        // Animation timer
        Timer animTimer = new Timer(16, e -> {
            updateAnimation();
            if (gameState == GameState.BALL_MOVING) {
                updatePhysics();
            }
            repaint();
        });
        animTimer.start();

        // Computer AI timer
        computerTimer = new Timer(2000, e -> {
            if (gameState == GameState.PLAYING && vsComputer && currentPlayer == 2) {
                computerAimAndShoot();
            }
        });
    }

    public void startGame(int mode) {
        vsComputer = (mode == 1);
        players[0] = new Player(new Color(30, 144, 255), new Point2D.Double(startPosition.x, startPosition.y));
        players[1] = new Player(new Color(220, 20, 60), new Point2D.Double(startPosition.x + 30, startPosition.y));
        currentPlayer = 1;
        gameState = GameState.PLAYING;
        winnerMessage = "";
        particles.clear();
        computerTimer.start();
        repaint();
    }

    public void resetGame() {
        gameState = GameState.MENU;
        computerTimer.stop();
        repaint();
    }

    private void computerAimAndShoot() {
        Player comp = players[1];
        double dx = holePosition.x - comp.position.x;
        double dy = holePosition.y - comp.position.y;
        double distance = Math.sqrt(dx*dx + dy*dy);

        // Add some randomness to make it imperfect
        double accuracy = 0.85 + random.nextDouble() * 0.15;
        double power = Math.min(distance * 0.08 * accuracy, maxPower);
        double angle = Math.atan2(dy, dx) + (random.nextDouble() - 0.5) * 0.2;

        computerPower = power;
        computerAngle = angle;

        // Visual feedback
        gameState = GameState.AIMING;
        Timer shootTimer = new Timer(500, ev -> {
            ballVelocity.x = Math.cos(angle) * power;
            ballVelocity.y = Math.sin(angle) * power;
            players[currentPlayer-1].strokes++;
            gameState = GameState.BALL_MOVING;
            createDustParticles(comp.position);
        });
        shootTimer.setRepeats(false);
        shootTimer.start();
    }

    private void shootBall() {
        Player current = players[currentPlayer-1];
        double dx = mousePos.x - current.position.x;
        double dy = mousePos.y - current.position.y;
        double distance = Math.sqrt(dx*dx + dy*dy);
        double power = Math.min(distance * 0.15, maxPower);
        double angle = Math.atan2(dy, dx);

        ballVelocity.x = Math.cos(angle) * power;
        ballVelocity.y = Math.sin(angle) * power;
        current.strokes++;
        gameState = GameState.BALL_MOVING;
        createDustParticles(current.position);
    }

    private void createDustParticles(Point2D.Double pos) {
        for (int i = 0; i < 15; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = random.nextDouble() * 3 + 1;
            particles.add(new Particle(
                    pos.x, pos.y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    new Color(194, 178, 128, 200),
                    random.nextInt(20) + 10
            ));
        }
    }

    private void createSplashParticles(Point2D.Double pos) {
        for (int i = 0; i < 20; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = random.nextDouble() * 4 + 2;
            particles.add(new Particle(
                    pos.x, pos.y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 2,
                    new Color(200, 230, 255, 220),
                    random.nextInt(15) + 8
            ));
        }
    }

    private void updatePhysics() {
        Player current = players[currentPlayer-1];

        // Apply velocity
        current.position.x += ballVelocity.x;
        current.position.y += ballVelocity.y;

        // Apply friction
        ballVelocity.x *= friction;
        ballVelocity.y *= friction;

        // Check sand traps (more friction)
        for (Rectangle2D.Double trap : sandTraps) {
            if (trap.contains(current.position.x, current.position.y)) {
                ballVelocity.x *= 0.85;
                ballVelocity.y *= 0.85;
                if (random.nextDouble() < 0.3) {
                    particles.add(new Particle(
                            current.position.x, current.position.y,
                            (random.nextDouble() - 0.5) * 2,
                            (random.nextDouble() - 0.5) * 2 - 1,
                            new Color(194, 178, 128, 150),
                            random.nextInt(10) + 5
                    ));
                }
            }
        }

        // Check water hazards
        for (Rectangle2D.Double water : waterHazards) {
            if (water.contains(current.position.x, current.position.y)) {
                createSplashParticles(current.position);
                // Reset to previous position with penalty
                current.position.x = startPosition.x;
                current.position.y = startPosition.y + (currentPlayer == 1 ? 0 : 30);
                current.strokes += 2; // Penalty
                ballVelocity.x = 0;
                ballVelocity.y = 0;
                gameState = GameState.PLAYING;
                switchPlayer();
                return;
            }
        }

        // Check tree collisions
        for (Rectangle2D.Double tree : trees) {
            if (tree.contains(current.position.x, current.position.y)) {
                ballVelocity.x *= -0.5;
                ballVelocity.y *= -0.5;
                createDustParticles(current.position);
            }
        }

        // Boundary checks
        if (current.position.x < 20 || current.position.x > 1180) {
            ballVelocity.x *= -0.7;
            current.position.x = Math.max(20, Math.min(1180, current.position.x));
        }
        if (current.position.y < 150 || current.position.y > 680) {
            ballVelocity.y *= -0.7;
            current.position.y = Math.max(150, Math.min(680, current.position.y));
        }

        // Check if stopped
        if (Math.abs(ballVelocity.x) < stopThreshold && Math.abs(ballVelocity.y) < stopThreshold) {
            ballVelocity.x = 0;
            ballVelocity.y = 0;

            // Check hole
            double dx = holePosition.x - current.position.x;
            double dy = holePosition.y - current.position.y;
            double distToHole = Math.sqrt(dx*dx + dy*dy);

            if (distToHole < holeRadius) {
                current.finished = true;
                current.score = current.strokes;

                // Create celebration particles
                for (int i = 0; i < 30; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double speed = random.nextDouble() * 5 + 2;
                    particles.add(new Particle(
                            holePosition.x, holePosition.y,
                            Math.cos(angle) * speed,
                            Math.sin(angle) * speed - 3,
                            new Color(255, 215, 0, 255),
                            random.nextInt(25) + 15
                    ));
                }

                if (players[0].finished && players[1].finished) {
                    gameState = GameState.GAME_OVER;
                    if (players[0].score < players[1].score) {
                        winnerMessage = "🏆 Player 1 Wins! " + players[0].score + " vs " + players[1].score;
                    } else if (players[1].score < players[0].score) {
                        winnerMessage = vsComputer ? "🤖 Computer Wins! " + players[1].score + " vs " + players[0].score
                                : "🏆 Player 2 Wins! " + players[1].score + " vs " + players[0].score;
                    } else {
                        winnerMessage = "🤝 Tie Game! " + players[0].score + " strokes each";
                    }
                    computerTimer.stop();
                } else {
                    switchPlayer();
                }
            } else {
                switchPlayer();
            }
        }
    }

    private void switchPlayer() {
        if (currentPlayer == 1 && !players[1].finished) {
            currentPlayer = 2;
        } else if (currentPlayer == 2 && !players[0].finished) {
            currentPlayer = 1;
        }
        gameState = GameState.PLAYING;
    }

    private void updateAnimation() {
        waterOffset += 0.02;
        cloudOffset += 0.1;
        flagWave += 0.1;

        // Update particles
        particles.removeIf(p -> {
            p.update();
            return p.life <= 0;
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (gameState == GameState.MENU) {
            drawMenu(g2d);
            return;
        }

        drawBackground(g2d);
        drawObstacles(g2d);
        drawHole(g2d);
        drawPlayers(g2d);
        drawAimingLine(g2d);
        drawParticles(g2d);
        drawUI(g2d);
    }

    private void drawMenu(Graphics2D g2d) {
        // Sky gradient
        GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, 700, new Color(255, 228, 196));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, 1200, 700);

        // Animated waves
        drawWaves(g2d, 450);

        // Beach sand
        GradientPaint sand = new GradientPaint(0, 500, new Color(238, 214, 175),
                0, 700, new Color(194, 178, 128));
        g2d.setPaint(sand);
        g2d.fillRect(0, 500, 1200, 200);

        // Palm trees decoration
        drawPalmTree(g2d, 100, 480, 1.2f);
        drawPalmTree(g2d, 1100, 490, 1.0f);

        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        g2d.setColor(new Color(20, 60, 100));
        String title = "🏖️ VIRTUAL GOLF CHALLENGE";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (1200 - fm.stringWidth(title)) / 2, 200);

        // Subtitle
        g2d.setFont(new Font("Arial", Font.ITALIC, 24));
        g2d.setColor(new Color(100, 80, 60));
        String sub = "Select a game mode below to begin";
        fm = g2d.getFontMetrics();
        g2d.drawString(sub, (1200 - fm.stringWidth(sub)) / 2, 260);

        // Golf ball decoration
        drawGolfBall(g2d, 600, 350, 30, Color.WHITE);

        // Seagulls
        drawSeagull(g2d, 300 + (int)(Math.sin(cloudOffset * 0.05) * 50), 150);
        drawSeagull(g2d, 800 + (int)(Math.cos(cloudOffset * 0.03) * 30), 120);
    }

    private void drawBackground(Graphics2D g2d) {
        // Sky with gradient
        GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, 400, new Color(255, 248, 220));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, 1200, 400);

        // Sun with glow
        RadialGradientPaint sunGlow = new RadialGradientPaint(
                new Point2D.Float(1000, 80), 100,
                new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(255, 255, 200, 200),
                        new Color(255, 220, 100, 100),
                        new Color(255, 200, 50, 0)}
        );
        g2d.setPaint(sunGlow);
        g2d.fillOval(900, -20, 200, 200);

        g2d.setColor(new Color(255, 220, 50));
        g2d.fillOval(960, 40, 80, 80);

        // Clouds
        drawCloud(g2d, 200 + (int)(cloudOffset % 1400) - 200, 80, 0.8f);
        drawCloud(g2d, 500 + (int)(cloudOffset * 0.7 % 1400) - 200, 120, 1.0f);
        drawCloud(g2d, 800 + (int)(cloudOffset * 0.5 % 1400) - 200, 60, 0.9f);

        // Ocean
        GradientPaint ocean = new GradientPaint(0, 400, new Color(0, 119, 190),
                0, 550, new Color(0, 80, 150));
        g2d.setPaint(ocean);
        g2d.fillRect(0, 400, 1200, 150);

        // Animated waves
        drawWaves(g2d, 400);

        // Beach
        GradientPaint beach = new GradientPaint(0, 550, new Color(238, 214, 175),
                0, 700, new Color(194, 178, 128));
        g2d.setPaint(beach);
        g2d.fillRect(0, 550, 1200, 150);

        // Sand texture
        g2d.setColor(new Color(210, 190, 140, 100));
        for (Point2D.Double p : sandParticles) {
            g2d.fillOval((int)p.x, (int)p.y, 3, 3);
        }

        // Green (fairway)
        GradientPaint green = new GradientPaint(0, 250, new Color(34, 139, 34),
                0, 550, new Color(50, 160, 50));
        g2d.setPaint(green);
        Path2D.Double fairway = new Path2D.Double();
        fairway.moveTo(50, 300);
        fairway.curveTo(200, 250, 400, 280, 600, 300);
        fairway.curveTo(800, 320, 1000, 280, 1150, 320);
        fairway.lineTo(1150, 500);
        fairway.curveTo(1000, 520, 800, 500, 600, 480);
        fairway.curveTo(400, 460, 200, 500, 50, 480);
        fairway.closePath();
        g2d.fill(fairway);

        // Palm trees
        drawPalmTree(g2d, 80, 520, 1.0f);
        drawPalmTree(g2d, 1120, 530, 0.9f);
    }

    private void drawWaves(Graphics2D g2d, int baseY) {
        g2d.setColor(new Color(255, 255, 255, 80));
        for (int i = 0; i < 5; i++) {
            int y = baseY + i * 25;
            for (int x = 0; x < 1250; x += 100) {
                int waveY = y + (int)(Math.sin((x + waterOffset * 50 + i * 30) * 0.02) * 10);
                g2d.drawArc(x, waveY, 50, 15, 0, 180);
            }
        }
    }

    private void drawCloud(Graphics2D g2d, int x, int y, float scale) {
        g2d.setColor(new Color(255, 255, 255, 220));
        int size = (int)(30 * scale);
        g2d.fillOval(x, y, size * 2, size);
        g2d.fillOval(x + size/2, y - size/3, (int) (size * 1.5f), size);
        g2d.fillOval(x + size, y, (int) (size * 1.8f), size);
    }

    private void drawPalmTree(Graphics2D g2d, int x, int y, float scale) {
        int trunkW = (int)(15 * scale);
        int trunkH = (int)(120 * scale);

        // Trunk
        g2d.setColor(new Color(139, 90, 43));
        g2d.fillRect(x - trunkW/2, y - trunkH, trunkW, trunkH);

        // Trunk texture
        g2d.setColor(new Color(160, 110, 60));
        for (int i = 0; i < 5; i++) {
            g2d.drawLine(x - trunkW/2, y - trunkH + i * 25, x + trunkW/2, y - trunkH + i * 25 + 5);
        }

        // Leaves
        g2d.setColor(new Color(34, 139, 34));
        int leafLen = (int)(60 * scale);
        int cx = x;
        int cy = y - trunkH;

        for (int i = 0; i < 7; i++) {
            double angle = -Math.PI / 2 + (i - 3) * 0.4;
            int ex = cx + (int)(Math.cos(angle) * leafLen);
            int ey = cy + (int)(Math.sin(angle) * leafLen);

            Path2D.Double leaf = new Path2D.Double();
            leaf.moveTo(cx, cy);
            leaf.quadTo(cx + (ex-cx)/2, cy - 20, ex, ey);
            leaf.lineTo(ex + 5, ey + 5);
            leaf.quadTo(cx + (ex-cx)/2 + 5, cy - 15, cx + 5, cy);
            leaf.closePath();
            g2d.fill(leaf);
        }

        // Coconuts
        g2d.setColor(new Color(101, 67, 33));
        g2d.fillOval(cx - 8, cy + 5, 12, 15);
        g2d.fillOval(cx + 2, cy + 8, 12, 15);
    }

    private void drawSeagull(Graphics2D g2d, int x, int y) {
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawArc(x - 15, y, 15, 10, 0, 180);
        g2d.drawArc(x, y, 15, 10, 0, 180);
    }

    private void drawObstacles(Graphics2D g2d) {
        // Sand traps
        for (Rectangle2D.Double trap : sandTraps) {
            GradientPaint sand = new GradientPaint(
                    (float)trap.x, (float)trap.y, new Color(238, 214, 175),
                    (float)trap.x, (float)(trap.y + trap.height), new Color(210, 190, 140)
            );
            g2d.setPaint(sand);
            g2d.fillRoundRect((int)trap.x, (int)trap.y, (int)trap.width, (int)trap.height, 20, 20);

            // Border
            g2d.setColor(new Color(180, 160, 120));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect((int)trap.x, (int)trap.y, (int)trap.width, (int)trap.height, 20, 20);
        }

        // Water hazards
        for (Rectangle2D.Double water : waterHazards) {
            GradientPaint waterGrad = new GradientPaint(
                    (float)water.x, (float)water.y, new Color(0, 150, 220, 180),
                    (float)water.x, (float)(water.y + water.height), new Color(0, 100, 180, 180)
            );
            g2d.setPaint(waterGrad);
            g2d.fillRoundRect((int)water.x, (int)water.y, (int)water.width, (int)water.height, 15, 15);

            // Water shimmer
            g2d.setColor(new Color(255, 255, 255, 100));
            for (int i = 0; i < 3; i++) {
                int wx = (int)(water.x + 10 + random.nextDouble() * (water.width - 20));
                int wy = (int)(water.y + 10 + random.nextDouble() * (water.height - 20));
                g2d.drawOval(wx, wy, 8, 4);
            }
        }

        // Trees
        for (Rectangle2D.Double tree : trees) {
            int x = (int)tree.x;
            int y = (int)tree.y;
            int w = (int)tree.width;

            // Shadow
            g2d.setColor(new Color(0, 50, 0, 80));
            g2d.fillOval(x + 5, y + w - 10, w, w/3);

            // Tree top
            GradientPaint treeGrad = new GradientPaint(
                    x, y, new Color(34, 139, 34),
                    x, y + w, new Color(0, 100, 0)
            );
            g2d.setPaint(treeGrad);
            g2d.fillOval(x, y, w, w);

            // Trunk
            g2d.setColor(new Color(101, 67, 33));
            g2d.fillRect(x + w/2 - 5, y + w - 10, 10, 20);
        }
    }

    private void drawHole(Graphics2D g2d) {
        // Hole shadow
        g2d.setColor(new Color(0, 50, 0, 100));
        g2d.fillOval((int)holePosition.x - 22, (int)holePosition.y - 18, 44, 40);

        // Hole
        RadialGradientPaint hole = new RadialGradientPaint(
                holePosition, (float)holeRadius,
                new float[]{0f, 1f},
                new Color[]{new Color(50, 30, 10), new Color(101, 67, 33)}
        );
        g2d.setPaint(hole);
        g2d.fillOval((int)holePosition.x - (int)holeRadius,
                (int)holePosition.y - (int)holeRadius,
                (int)holeRadius * 2, (int)holeRadius * 2);

        // Flag pole
        g2d.setColor(new Color(192, 192, 192));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine((int)holePosition.x, (int)holePosition.y,
                (int)holePosition.x, (int)holePosition.y - 80);

        // Flag
        g2d.setColor(Color.RED);
        int flagWaveY = (int)(Math.sin(flagWave) * 5);
        Path2D.Double flag = new Path2D.Double();
        flag.moveTo(holePosition.x, holePosition.y - 80);
        flag.lineTo(holePosition.x + 40, holePosition.y - 70 + flagWaveY);
        flag.lineTo(holePosition.x, holePosition.y - 60);
        flag.closePath();
        g2d.fill(flag);

        // Flag pole top
        g2d.setColor(Color.YELLOW);
        g2d.fillOval((int)holePosition.x - 3, (int)holePosition.y - 83, 6, 6);
    }

    private void drawPlayers(Graphics2D g2d) {
        for (int i = 0; i < 2; i++) {
            if (players[i] == null) continue;

            Player p = players[i];

            // Ball shadow
            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.fillOval((int)p.position.x - 8, (int)p.position.y - 4, 16, 10);

            // Ball
            drawGolfBall(g2d, p.position.x, p.position.y, 10, p.ballColor);

            // Player indicator
            if (currentPlayer == i + 1 && gameState != GameState.GAME_OVER) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval((int)p.position.x - 15, (int)p.position.y - 15, 30, 30);

                // Arrow pointing to current player
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                String label = (i == 0) ? "P1" : (vsComputer ? "CPU" : "P2");
                g2d.drawString(label, (int)p.position.x - 10, (int)p.position.y - 25);
            }
        }
    }

    private void drawGolfBall(Graphics2D g2d, double x, double y, int r, Color color) {
        RadialGradientPaint ball = new RadialGradientPaint(
                new Point2D.Double(x - r/3, y - r/3), r,
                new float[]{0f, 0.7f, 1f},
                new Color[]{Color.WHITE, color, color.darker()}
        );
        g2d.setPaint(ball);
        g2d.fillOval((int)x - r, (int)y - r, r * 2, r * 2);

        // Dimples
        g2d.setColor(new Color(0, 0, 0, 30));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int dx = (int)(x - r/2 + i * r/2);
                int dy = (int)(y - r/2 + j * r/2);
                g2d.fillOval(dx, dy, 2, 2);
            }
        }
    }

    private void drawAimingLine(Graphics2D g2d) {
        if (gameState != GameState.AIMING || players[currentPlayer-1] == null) return;

        Player current = players[currentPlayer-1];

        double dx = mousePos.x - current.position.x;
        double dy = mousePos.y - current.position.y;
        double distance = Math.sqrt(dx*dx + dy*dy);
        double power = Math.min(distance * 0.15, maxPower);

        // Calculate trajectory points
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{10, 5}, 0));

        // Color based on power
        Color lineColor;
        if (power < maxPower * 0.5) lineColor = Color.GREEN;
        else if (power < maxPower * 0.8) lineColor = Color.YELLOW;
        else lineColor = Color.RED;

        g2d.setColor(lineColor);

        // Dashed line to mouse
        g2d.drawLine((int)current.position.x, (int)current.position.y, mousePos.x, mousePos.y);

        // Power indicator
        g2d.setStroke(new BasicStroke(2));
        int barWidth = 100;
        int barHeight = 10;
        int barX = (int)current.position.x - barWidth/2;
        int barY = (int)current.position.y - 40;

        g2d.setColor(Color.BLACK);
        g2d.drawRect(barX, barY, barWidth, barHeight);

        int fillWidth = (int)((power / maxPower) * barWidth);
        g2d.setColor(lineColor);
        g2d.fillRect(barX + 1, barY + 1, fillWidth, barHeight - 1);

        // Arrow head
        double angle = Math.atan2(dy, dx);
        int arrowLen = 15;
        g2d.setColor(lineColor);
        g2d.drawLine(mousePos.x, mousePos.y,
                (int)(mousePos.x - arrowLen * Math.cos(angle - 0.5)),
                (int)(mousePos.y - arrowLen * Math.sin(angle - 0.5)));
        g2d.drawLine(mousePos.x, mousePos.y,
                (int)(mousePos.x - arrowLen * Math.cos(angle + 0.5)),
                (int)(mousePos.y - arrowLen * Math.sin(angle + 0.5)));
    }

    private void drawParticles(Graphics2D g2d) {
        for (Particle p : particles) {
            g2d.setColor(p.color);
            g2d.fillOval((int)p.x, (int)p.y, p.size, p.size);
        }
    }

    private void drawUI(Graphics2D g2d) {
        // Score board
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 250, 80, 15, 15);

        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.WHITE);

        if (players[0] != null) {
            g2d.setColor(players[0].ballColor);
            g2d.fillOval(25, 25, 12, 12);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Player 1: " + players[0].strokes + " strokes", 45, 35);
        }

        if (players[1] != null) {
            g2d.setColor(players[1].ballColor);
            g2d.fillOval(25, 55, 12, 12);
            g2d.setColor(Color.WHITE);
            String label = vsComputer ? "Computer: " : "Player 2: ";
            g2d.drawString(label + players[1].strokes + " strokes", 45, 65);
        }

        // Wind indicator
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("💨 Wind: " + (int)(Math.sin(waterOffset) * 10) + " mph", 1050, 30);

        // Game over overlay
        if (gameState == GameState.GAME_OVER) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, 1200, 700);

            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.setColor(Color.YELLOW);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(winnerMessage, (1200 - fm.stringWidth(winnerMessage)) / 2, 350);

            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            g2d.setColor(Color.WHITE);
            String restart = "Click 'New Game' to play again";
            fm = g2d.getFontMetrics();
            g2d.drawString(restart, (1200 - fm.stringWidth(restart)) / 2, 400);
        }
    }
}

class Particle {
    double x, y;
    double vx, vy;
    Color color;
    int life;
    int size;

    public Particle(double x, double y, double vx, double vy, Color color, int life) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.life = life;
        this.size = life / 3;
    }

    public void update() {
        x += vx;
        y += vy;
        vy += 0.1; // gravity
        life--;
        size = Math.max(1, life / 3);
    }
}