import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RedSquareGolf extends JFrame {
    private GameCanvas canvas;
    private JLabel statusBar;
    private JButton pvpBtn, pvcBtn, resetBtn;

    public RedSquareGolf() {
        setTitle("🏛️ Red Square Golf Challenge - Moscow");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());

        canvas = new GameCanvas();
        mainPanel.add(canvas, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 15));
        controlPanel.setBackground(new Color(139, 0, 0));
        controlPanel.setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, new Color(218, 165, 32)));

        statusBar = new JLabel("Welcome to Red Square Golf!");
        statusBar.setFont(new Font("Georgia", Font.BOLD, 20));
        statusBar.setForeground(new Color(255, 215, 0));

        pvpBtn = createButton("👥 Player vs Player", new Color(0, 100, 0));
        pvcBtn = createButton("🤖 Player vs Computer", new Color(25, 25, 112));
        resetBtn = createButton("🔄 New Game", new Color(178, 34, 34));

        pvpBtn.addActionListener(e -> canvas.startGame(false));
        pvcBtn.addActionListener(e -> canvas.startGame(true));
        resetBtn.addActionListener(e -> canvas.resetGame());

        controlPanel.add(statusBar);
        controlPanel.add(Box.createHorizontalStrut(40));
        controlPanel.add(pvpBtn);
        controlPanel.add(pvcBtn);
        controlPanel.add(resetBtn);

        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        add(mainPanel);

        Timer statusTimer = new Timer(100, e -> updateStatus());
        statusTimer.start();
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Georgia", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    private void updateStatus() {
        if (canvas.state == GameState.PLAYING || canvas.state == GameState.AIMING) {
            String pName = canvas.currentPlayer == 1 ? "Player 1 (Blue)" :
                    (canvas.vsComputer ? "Computer (Red)" : "Player 2 (Red)");
            statusBar.setText(pName + "'s Turn | Strokes: " + canvas.players[canvas.currentPlayer-1].strokes);
        } else if (canvas.state == GameState.GAME_OVER) {
            statusBar.setText(canvas.winnerMsg);
        } else {
            statusBar.setText("Select Game Mode to Begin");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception e) { e.printStackTrace(); }
            new RedSquareGolf().setVisible(true);
        });
    }
}

enum GameState { MENU, PLAYING, AIMING, MOVING, GAME_OVER }

class Player {
    double x, y;
    int strokes = 0;
    boolean finished = false;
    Color uniformColor;
    int score = 0;
    double animFrame = 0;
    boolean facingRight = true;

    public Player(double x, double y, Color color) {
        this.x = x; this.y = y; this.uniformColor = color;
    }
}

class Obstacle {
    Rectangle2D bounds;
    Color color;
    String type;

    public Obstacle(double x, double y, double w, double h, Color c, String t) {
        bounds = new Rectangle2D.Double(x, y, w, h);
        color = c; type = t;
    }
}

class Particle {
    double x, y, vx, vy;
    Color color;
    int life, maxLife;

    public Particle(double x, double y, double vx, double vy, Color c, int life) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.color = c; this.maxLife = this.life = life;
    }

    void update() {
        x += vx; y += vy; vy += 0.08;
        life--;
    }
}

class GameCanvas extends JPanel {
    GameState state = GameState.MENU;
    Player[] players = new Player[2];
    int currentPlayer = 1;
    boolean vsComputer = false;
    String winnerMsg = "";

    Point2D.Double holePos = new Point2D.Double(1250, 400);
    double holeRadius = 18;
    Point2D.Double startPos = new Point2D.Double(120, 450);

    Point2D.Double ballVel = new Point2D.Double(0, 0);
    double friction = 0.988;
    double stopThreshold = 0.12;

    Point mousePos = new Point();
    boolean dragging = false;
    double maxPower = 22;

    List<Obstacle> obstacles = new ArrayList<>();
    List<Particle> particles = new ArrayList<>();
    Random rand = new Random();

    double time = 0;
    double snowOffset = 0;
    List<Point2D.Double> snowflakes = new ArrayList<>();
    List<Point2D.Double> cobblestones = new ArrayList<>();

    Timer compTimer;
    double compPower, compAngle;

    // Animation frames for walking
    double walkCycle = 0;

    public GameCanvas() {
        setPreferredSize(new Dimension(1400, 800));
        setFocusable(true);

        // Initialize snow
        for (int i = 0; i < 100; i++) {
            snowflakes.add(new Point2D.Double(rand.nextDouble() * 1400, rand.nextDouble() * 800));
        }

        // Initialize cobblestone pattern
        for (int row = 0; row < 60; row++) {
            for (int col = 0; col < 40; col++) {
                double x = col * 45 + (row % 2) * 22;
                double y = 200 + row * 15;
                if (x < 1400 && y < 800) {
                    cobblestones.add(new Point2D.Double(x, y));
                }
            }
        }

        // Red Square obstacles - authentic architecture
        // Kremlin Wall (right side)
        obstacles.add(new Obstacle(1100, 100, 280, 600, new Color(220, 20, 60), "wall"));
        // St. Basil's Cathedral base
        obstacles.add(new Obstacle(900, 150, 180, 200, new Color(178, 34, 34), "basil"));
        // Lenin's Mausoleum
        obstacles.add(new Obstacle(600, 300, 140, 80, new Color(105, 105, 105), "mausoleum"));
        // Spasskaya Tower base
        obstacles.add(new Obstacle(1150, 50, 100, 150, new Color(139, 0, 0), "tower"));
        // GUM facade (left background)
        obstacles.add(new Obstacle(50, 120, 200, 100, new Color(200, 50, 50), "gum"));
        // Kazan Cathedral (small)
        obstacles.add(new Obstacle(350, 180, 120, 100, new Color(255, 215, 0), "kazan"));

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (state == GameState.PLAYING && (currentPlayer == 1 || !vsComputer)) {
                    dragging = true;
                    mousePos = e.getPoint();
                    state = GameState.AIMING;
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (state == GameState.AIMING) shoot();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (state == GameState.AIMING) { mousePos = e.getPoint(); repaint(); }
            }
        });

        Timer gameLoop = new Timer(16, e -> {
            time += 0.016;
            snowOffset += 0.5;
            walkCycle += 0.15;

            // Update snow
            for (Point2D.Double s : snowflakes) {
                s.y += 0.5 + rand.nextDouble();
                s.x += Math.sin(time + s.y * 0.01) * 0.3;
                if (s.y > 800) { s.y = -5; s.x = rand.nextDouble() * 1400; }
            }

            // Update particles
            particles.removeIf(p -> { p.update(); return p.life <= 0; });

            if (state == GameState.MOVING) updatePhysics();
            repaint();
        });
        gameLoop.start();

        compTimer = new Timer(2500, e -> {
            if (state == GameState.PLAYING && vsComputer && currentPlayer == 2) computerTurn();
        });
    }

    public void startGame(boolean computer) {
        vsComputer = computer;
        players[0] = new Player(startPos.x, startPos.y, new Color(30, 144, 255));
        players[1] = new Player(startPos.x + 40, startPos.y + 20, new Color(220, 20, 60));
        currentPlayer = 1;
        state = GameState.PLAYING;
        winnerMsg = "";
        particles.clear();
        compTimer.start();
        repaint();
    }

    public void resetGame() {
        state = GameState.MENU;
        compTimer.stop();
        repaint();
    }

    private void computerTurn() {
        Player comp = players[1];
        double dx = holePos.x - comp.x;
        double dy = holePos.y - comp.y;
        double dist = Math.sqrt(dx*dx + dy*dy);

        double accuracy = 0.82 + rand.nextDouble() * 0.18;
        compPower = Math.min(dist * 0.075 * accuracy, maxPower);
        compAngle = Math.atan2(dy, dx) + (rand.nextDouble() - 0.5) * 0.25;

        state = GameState.AIMING;
        Timer shootDelay = new Timer(800, ev -> {
            ballVel.x = Math.cos(compAngle) * compPower;
            ballVel.y = Math.sin(compAngle) * compPower;
            players[currentPlayer-1].strokes++;
            state = GameState.MOVING;
            spawnParticles(comp.x, comp.y, new Color(180, 180, 180), 12);
        });
        shootDelay.setRepeats(false);
        shootDelay.start();
    }

    private void shoot() {
        Player p = players[currentPlayer-1];
        double dx = mousePos.x - p.x;
        double dy = mousePos.y - p.y;
        double dist = Math.sqrt(dx*dx + dy*dy);
        double power = Math.min(dist * 0.12, maxPower);
        double angle = Math.atan2(dy, dx);

        ballVel.x = Math.cos(angle) * power;
        ballVel.y = Math.sin(angle) * power;
        p.strokes++;
        p.facingRight = ballVel.x > 0;
        state = GameState.MOVING;
        spawnParticles(p.x, p.y, new Color(160, 160, 160), 15);
    }

    private void spawnParticles(double x, double y, Color c, int count) {
        for (int i = 0; i < count; i++) {
            double a = rand.nextDouble() * Math.PI * 2;
            double s = rand.nextDouble() * 3 + 1;
            particles.add(new Particle(x, y, Math.cos(a)*s, Math.sin(a)*s - 1,
                    new Color(c.getRed(), c.getGreen(), c.getBlue(), 200), rand.nextInt(20)+10));
        }
    }

    private void updatePhysics() {
        Player p = players[currentPlayer-1];
        p.x += ballVel.x;
        p.y += ballVel.y;
        ballVel.x *= friction;
        ballVel.y *= friction;

        // Obstacle collisions
        for (Obstacle o : obstacles) {
            if (o.bounds.contains(p.x, p.y)) {
                ballVel.x *= -0.6;
                ballVel.y *= -0.6;
                spawnParticles(p.x, p.y, o.color, 8);
                // Push out
                p.x = Math.max(o.bounds.getX() - 15, Math.min(p.x, o.bounds.getX() + o.bounds.getWidth() + 15));
            }
        }

        // Boundaries
        if (p.x < 30 || p.x > 1370) { ballVel.x *= -0.7; p.x = Math.max(30, Math.min(1370, p.x)); }
        if (p.y < 180 || p.y > 780) { ballVel.y *= -0.7; p.y = Math.max(180, Math.min(780, p.y)); }

        // Stop check
        if (Math.abs(ballVel.x) < stopThreshold && Math.abs(ballVel.y) < stopThreshold) {
            ballVel.x = 0; ballVel.y = 0;

            double dx = holePos.x - p.x;
            double dy = holePos.y - p.y;
            if (Math.sqrt(dx*dx + dy*dy) < holeRadius) {
                p.finished = true; p.score = p.strokes;
                spawnParticles(holePos.x, holePos.y, new Color(255, 215, 0), 25);

                if (players[0].finished && players[1].finished) {
                    state = GameState.GAME_OVER;
                    if (players[0].score < players[1].score)
                        winnerMsg = "🏆 Player 1 Wins! " + players[0].score + " vs " + players[1].score;
                    else if (players[1].score < players[0].score)
                        winnerMsg = vsComputer ? "🤖 Computer Wins! " + players[1].score + " vs " + players[0].score
                                : "🏆 Player 2 Wins! " + players[1].score + " vs " + players[0].score;
                    else winnerMsg = "🤝 Draw! " + players[0].score + " strokes each";
                    compTimer.stop();
                } else switchTurn();
            } else switchTurn();
        }
    }

    private void switchTurn() {
        if (currentPlayer == 1 && !players[1].finished) currentPlayer = 2;
        else if (currentPlayer == 2 && !players[0].finished) currentPlayer = 1;
        state = GameState.PLAYING;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (state == GameState.MENU) { drawMenu(g2d); return; }

        drawBackground(g2d);
        drawObstacles(g2d);
        drawHole(g2d);
        drawPlayers(g2d);
        drawAiming(g2d);
        drawParticles(g2d);
        drawOverlay(g2d);

        if (state == GameState.GAME_OVER) drawGameOver(g2d);
    }

    private void drawMenu(Graphics2D g2d) {
        // Night sky over Moscow
        GradientPaint sky = new GradientPaint(0, 0, new Color(25, 25, 60), 0, 800, new Color(70, 30, 60));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, 1400, 800);

        // Stars
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < 80; i++) {
            int sx = (i * 137) % 1400;
            int sy = (i * 89) % 400;
            g2d.fillOval(sx, sy, 2, 2);
        }

        // Moon
        g2d.setColor(new Color(255, 255, 220));
        g2d.fillOval(1200, 50, 80, 80);
        g2d.setColor(new Color(25, 25, 60));
        g2d.fillOval(1180, 40, 60, 60);

        // Red Square ground preview
        drawRedSquareGround(g2d);

        // St. Basil's silhouette
        drawStBasils(g2d, 700, 150, 0.8f);

        // Kremlin wall silhouette
        g2d.setColor(new Color(139, 0, 0, 180));
        g2d.fillRect(1000, 200, 350, 400);

        // Title
        g2d.setFont(new Font("Georgia", Font.BOLD, 56));
        g2d.setColor(new Color(255, 215, 0));
        String title = "RED SQUARE GOLF CHALLENGE";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (1400 - fm.stringWidth(title)) / 2, 120);

        // Subtitle
        g2d.setFont(new Font("Georgia", Font.ITALIC, 22));
        g2d.setColor(new Color(220, 220, 220));
        String sub = "Moscow • Kremlin • St. Basil's Cathedral";
        fm = g2d.getFontMetrics();
        g2d.drawString(sub, (1400 - fm.stringWidth(sub)) / 2, 160);

        // Decorative elements
        g2d.setColor(new Color(218, 165, 32));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(400, 190, 1000, 190);

        // Golf ball
        drawGolfBall(g2d, 700, 300, 25, Color.WHITE);

        // Player silhouettes
        drawPlayerCharacter(g2d, 500, 400, new Color(100, 149, 237), true, 0, true);
        drawPlayerCharacter(g2d, 900, 400, new Color(220, 20, 60), false, 0, true);

        // Snow
        g2d.setColor(new Color(255, 255, 255, 150));
        for (Point2D.Double s : snowflakes) {
            g2d.fillOval((int)s.x, (int)s.y, 3, 3);
        }
    }

    private void drawBackground(Graphics2D g2d) {
        // Sky - Russian winter day
        GradientPaint sky = new GradientPaint(0, 0, new Color(176, 196, 222),
                0, 250, new Color(220, 220, 240));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, 1400, 250);

        // Distant Moscow skyline
        g2d.setColor(new Color(150, 150, 170, 100));
        for (int i = 0; i < 20; i++) {
            int h = 30 + rand.nextInt(40);
            g2d.fillRect(i * 80, 250 - h, 60, h);
        }

        // Red Square cobblestones
        drawRedSquareGround(g2d);

        // Snow on ground edges
        g2d.setColor(new Color(240, 248, 255, 120));
        g2d.fillRect(0, 190, 1400, 15);
    }

    private void drawRedSquareGround(Graphics2D g2d) {
        // Base red pavement
        g2d.setColor(new Color(178, 34, 34));
        g2d.fillRect(0, 200, 1400, 600);

        // Cobblestone pattern
        for (Point2D.Double c : cobblestones) {
            int shade = 160 + (int)((Math.sin(c.x * 0.1) + Math.cos(c.y * 0.1)) * 20);
            g2d.setColor(new Color(shade, shade/3, shade/3));
            g2d.fillRect((int)c.x, (int)c.y, 40, 12);
            g2d.setColor(new Color(140, 20, 20));
            g2d.drawRect((int)c.x, (int)c.y, 40, 12);
        }

        // Snow patches
        g2d.setColor(new Color(255, 250, 250, 100));
        for (int i = 0; i < 30; i++) {
            int sx = (i * 173) % 1400;
            int sy = 220 + (i * 67) % 580;
            g2d.fillOval(sx, sy, 40 + rand.nextInt(30), 15 + rand.nextInt(10));
        }
    }

    private void drawObstacles(Graphics2D g2d) {
        for (Obstacle o : obstacles) {
            switch(o.type) {
                case "wall": drawKremlinWall(g2d, o.bounds); break;
                case "basil": drawStBasils(g2d,
                        o.bounds.y,
                        o.bounds.x,
                        1.0f); break;
                case "mausoleum": drawMausoleum(g2d, o.bounds); break;
                case "tower": drawSpasskayaTower(g2d, o.bounds); break;
                case "gum": drawGUM(g2d, o.bounds); break;
                case "kazan": drawKazanCathedral(g2d, o.bounds); break;
                default:
                    throw new IllegalStateException("Unexpected value: " + o.type);
            }
        }
    }

    private void drawKremlinWall(Graphics2D g2D, Rectangle2D r) {
        // Main wall - red brick
        GradientPaint wall = new GradientPaint((float)r.getX(), (float)r.getY(),
                new Color(178, 34, 34), (float)r.getX(), (float)(r.getY()+r.getHeight()),
                new Color(139, 0, 0));
        g2D.setPaint(wall);
        g2D.fill(r);

        // Battlements
        g2D.setColor(new Color(120, 20, 20));
        for (int i = 0; i < r.getWidth(); i += 30) {
            g2D.fillRect((int)r.getX() + i, (int)r.getY() - 15, 20, 15);
        }

        // Brick texture
        g2D.setColor(new Color(200, 50, 50, 80));
        for (int row = 0; row < r.getHeight(); row += 15) {
            for (int col = 0; col < r.getWidth(); col += 25) {
                int offset = (row/15 % 2) * 12;
                g2D.drawRect((int)r.getX() + col + offset, (int)r.getY() + row, 25, 15);
            }
        }

        // Snow on top
        g2D.setColor(new Color(255, 250, 250, 180));
        g2D.fillRect((int)r.getX(), (int)r.getY() - 5, (int)r.getWidth(), 8);
    }

    private void drawStBasils(Graphics2D g2d, double x, double y, float scale) {
        int baseW = (int)(180 * scale);
        int baseH = (int)(200 * scale);

        // Main base
        GradientPaint base = new GradientPaint((float)x, (float)y,
                new Color(178, 34, 34), (float)x, (float)(y+baseH), new Color(139, 0, 0));
        g2d.setPaint(base);
        g2d.fillRect((int)x, (int)y, baseW, baseH);

        // Central tower (tallest)
        int cx = (int)(x + baseW/2);
        int cy = (int)(y - 80 * scale);
        int cw = (int)(50 * scale);
        int ch = (int)(100 * scale);

        // Tower body
        g2d.setColor(new Color(178, 34, 34));
        g2d.fillRect(cx - cw/2, cy, cw, ch);

        // Onion dome - central (gold/green stripes)
        int domeR = (int)(35 * scale);
        int domeY = cy - domeR;

        // Dome stripes
        for (int i = 0; i < 8; i++) {
            g2d.setColor(i % 2 == 0 ? new Color(255, 215, 0) : new Color(34, 139, 34));
            int startAngle = i * 45;
            g2d.fillArc(cx - domeR, domeY, domeR*2, domeR*2, startAngle, 45);
        }

        // Dome outline
        g2d.setColor(new Color(218, 165, 32));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawArc(cx - domeR, domeY, domeR*2, domeR*2, 0, 180);

        // Cross on top
        g2d.setColor(new Color(255, 215, 0));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(cx, domeY - 15, cx, domeY - 35);
        g2d.drawLine(cx - 8, domeY - 25, cx + 8, domeY - 25);

        // Smaller surrounding domes
        int[][] smallDomes = {{-50, 20}, {50, 20}, {-30, 60}, {30, 60}};
        Color[] domeColors = {
                new Color(0, 100, 0), new Color(0, 0, 139),
                new Color(139, 0, 139), new Color(255, 140, 0)
        };

        for (int i = 0; i < smallDomes.length; i++) {
            int dx = cx + (int)(smallDomes[i][0] * scale);
            int dy = (int)(y + smallDomes[i][1] * scale);
            int dr = (int)(20 * scale);

            // Small tower
            g2d.setColor(new Color(178, 34, 34));
            g2d.fillRect(dx - 8, dy, 16, (int)(40 * scale));

            // Colored dome
            g2d.setColor(domeColors[i]);
            g2d.fillArc(dx - dr, dy - dr, dr*2, dr*2, 0, 180);

            // Stripe pattern
            g2d.setColor(new Color(255, 255, 255, 100));
            for (int s = 0; s < 3; s++) {
                g2d.drawArc(dx - dr + s*5, dy - dr + s*3, (dr-s*5)*2, (dr-s*3)*2, 0, 180);
            }
        }

        // Decorative arches
        g2d.setColor(new Color(255, 215, 0, 150));
        for (int i = 0; i < 3; i++) {
            int ax = (int)(x + 30 * scale + i * 50 * scale);
            int ay = (int)(y + 120 * scale);
            g2d.fillArc(ax, ay, (int)(30 * scale), (int)(40 * scale), 0, 180);
        }
    }

    private void drawMausoleum(Graphics2D g2d, Rectangle2D r) {
        // Red granite base
        GradientPaint granite = new GradientPaint((float)r.getX(), (float)r.getY(),
                new Color(139, 0, 0), (float)r.getX(), (float)(r.getY()+r.getHeight()),
                new Color(100, 0, 0));
        g2d.setPaint(granite);
        g2d.fill(r);

        // Black marble top
        g2d.setColor(new Color(30, 30, 30));
        g2d.fillRect((int)r.getX() + 5, (int)r.getY(), (int)r.getWidth() - 10, 15);

        // Steps
        g2d.setColor(new Color(160, 160, 160));
        for (int i = 0; i < 5; i++) {
            int stepY = (int)(r.getY() + r.getHeight() - 5 - i * 8);
            g2d.fillRect((int)r.getX() + 10 + i*5, stepY, (int)r.getWidth() - 20 - i*10, 8);
        }

        // Columns
        g2d.setColor(new Color(180, 180, 180));
        for (int i = 0; i < 4; i++) {
            int colX = (int)(r.getX() + 15 + i * ((r.getWidth()-30)/3));
            g2d.fillRect(colX, (int)r.getY() + 20, 8, (int)r.getHeight() - 40);
        }

        // "ЛЕНИН" text (simplified)
        g2d.setColor(new Color(200, 200, 200));
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("LENIN", (int)(r.getX() + r.getWidth()/2 - 20), (int)(r.getY() + r.getHeight()/2));
    }

    private void drawSpasskayaTower(Graphics2D g2d, Rectangle2D r) {
        // Tower body
        GradientPaint tower = new GradientPaint((float)r.getX(), (float)r.getY(),
                new Color(178, 34, 34), (float)r.getX(), (float)(r.getY()+r.getHeight()),
                new Color(120, 20, 20));
        g2d.setPaint(tower);
        g2d.fill(r);

        // Clock (Kremlin chimes)
        int cx = (int)(r.getX() + r.getWidth()/2);
        int cy = (int)(r.getY() + 50);
        int cr = 25;

        // Clock face
        g2d.setColor(Color.WHITE);
        g2d.fillOval(cx - cr, cy - cr, cr*2, cr*2);
        g2d.setColor(new Color(218, 165, 32));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(cx - cr, cy - cr, cr*2, cr*2);

        // Clock hands
        double hourAngle = (time * 0.5) % (Math.PI * 2);
        double minuteAngle = (time * 2) % (Math.PI * 2);

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(cx, cy,
                (int)(cx + Math.cos(hourAngle - Math.PI/2) * 12),
                (int)(cy + Math.sin(hourAngle - Math.PI/2) * 12));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(cx, cy,
                (int)(cx + Math.cos(minuteAngle - Math.PI/2) * 18),
                (int)(cy + Math.sin(minuteAngle - Math.PI/2) * 18));

        // Star on top
        int starY = (int)r.getY() - 20;
        g2d.setColor(new Color(255, 215, 0));
        drawStar(g2d, cx, starY, 15);
    }

    private void drawGUM(Graphics2D g2d, Rectangle2D r) {
        // Shopping arcade facade
        GradientPaint gum = new GradientPaint((float)r.getX(), (float)r.getY(),
                new Color(200, 50, 50), (float)r.getX(), (float)(r.getY()+r.getHeight()),
                new Color(150, 30, 30));
        g2d.setPaint(gum);
        g2d.fill(r);

        // Arched windows
        g2d.setColor(new Color(255, 250, 240));
        for (int i = 0; i < 5; i++) {
            int wx = (int)(r.getX() + 10 + i * 38);
            int wy = (int)(r.getY() + 20);
            g2d.fillArc(wx, wy, 30, 40, 0, 180);
            g2d.setColor(new Color(100, 50, 50));
            g2d.drawArc(wx, wy, 30, 40, 0, 180);
            g2d.setColor(new Color(255, 250, 240));
        }

        // Sign
        g2d.setColor(new Color(255, 215, 0));
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("GUM", (int)(r.getX() + r.getWidth()/2 - 15), (int)(r.getY() + r.getHeight() - 10));
    }

    private void drawKazanCathedral(Graphics2D g2d, Rectangle2D r) {
        // Small orthodox church
        g2d.setColor(new Color(255, 215, 0));
        g2d.fill(r);

        // Dome
        int cx = (int)(r.getX() + r.getWidth()/2);
        int cy = (int)r.getY();
        g2d.setColor(new Color(255, 223, 0));
        g2d.fillArc((int)(r.getX() + 20), cy - 30, (int)(r.getWidth() - 40), 60, 0, 180);

        // Cross
        g2d.setColor(new Color(218, 165, 32));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(cx, cy - 35, cx, cy - 50);
        g2d.drawLine(cx - 5, cy - 42, cx + 5, cy - 42);
    }

    private void drawStar(Graphics2D g2d, int x, int y, int r) {
        int[] xPoints = new int[10];
        int[] yPoints = new int[10];
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI/2 + i * Math.PI/5;
            double radius = (i % 2 == 0) ? r : r/2;
            xPoints[i] = x + (int)(Math.cos(angle) * radius);
            yPoints[i] = y - (int)(Math.sin(angle) * radius);
        }
        g2d.fillPolygon(xPoints, yPoints, 10);
    }

    private void drawHole(Graphics2D g2d) {
        // Shadow
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillOval((int)holePos.x - 20, (int)holePos.y - 15, 40, 35);

        // Hole
        RadialGradientPaint hole = new RadialGradientPaint(holePos, (float)holeRadius,
                new float[]{0f, 1f}, new Color[]{new Color(60, 40, 20), new Color(101, 67, 33)});
        g2d.setPaint(hole);
        g2d.fillOval((int)holePos.x - (int)holeRadius, (int)holePos.y - (int)holeRadius,
                (int)holeRadius*2, (int)holeRadius*2);

        // Flag with Russian tricolor
        g2d.setColor(new Color(192, 192, 192));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine((int)holePos.x, (int)holePos.y, (int)holePos.x, (int)holePos.y - 90);

        // Waving flag
        int wave = (int)(Math.sin(time * 3) * 5);
        g2d.setColor(Color.WHITE);
        g2d.fillRect((int)holePos.x, (int)holePos.y - 90, 45, 20);
        g2d.setColor(new Color(0, 50, 160));
        g2d.fillRect((int)holePos.x, (int)holePos.y - 70, 45 + wave, 20);
        g2d.setColor(new Color(210, 30, 30));
        g2d.fillRect((int)holePos.x, (int)holePos.y - 50, 45 + wave * 2, 20);

        // Golden ball on pole
        g2d.setColor(new Color(255, 215, 0));
        g2d.fillOval((int)holePos.x - 4, (int)holePos.y - 96, 8, 8);
    }

    private void drawPlayers(Graphics2D g2d) {
        for (int i = 0; i < 2; i++) {
            if (players[i] == null) continue;
            Player p = players[i];

            // Shadow
            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.fillOval((int)p.x - 12, (int)p.y + 5, 24, 10);

            // Ball
            drawGolfBall(g2d, p.x, p.y, 8, Color.WHITE);

            // Character
            boolean isCurrent = (currentPlayer == i + 1 && state != GameState.GAME_OVER);
            drawPlayerCharacter(g2d, p.x, p.y - 25, p.uniformColor, p.facingRight,
                    isCurrent ? walkCycle : 0, false);

            // Indicator
            if (isCurrent) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval((int)p.x - 18, (int)p.y - 18, 36, 36);

                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                g2d.drawString(i == 0 ? "P1" : (vsComputer ? "CPU" : "P2"),
                        (int)p.x - 8, (int)p.y - 45);
            }
        }
    }

    private void drawPlayerCharacter(Graphics2D g2d, double x, double y, Color uniform,
                                     boolean facingRight, double anim, boolean silhouette) {
        int dir = facingRight ? 1 : -1;
        int legOffset = (int)(Math.sin(anim) * 5);
        int armOffset = (int)(Math.cos(anim) * 5);

        Color skin = silhouette ? uniform.darker() : new Color(255, 220, 177);
        Color shirt = silhouette ? uniform.darker() : uniform;
        Color pants = silhouette ? uniform.darker().darker() : new Color(50, 50, 80);
        Color shoes = silhouette ? Color.BLACK : new Color(80, 50, 30);
        Color hair = silhouette ? Color.BLACK : new Color(80, 60, 40);

        // Back leg
        g2d.setColor(pants);
        g2d.fillRect((int)(x - 6 * dir + legOffset), (int)(y + 20), 5, 18);

        // Back arm
        g2d.setColor(shirt);
        g2d.fillRect((int)(x + 8 * dir), (int)(y - 5 + armOffset), 4, 16);

        // Body
        g2d.setColor(shirt);
        g2d.fillRoundRect((int)(x - 10), (int)(y - 15), 20, 35, 5, 5);

        // Front leg
        g2d.setColor(pants);
        g2d.fillRect((int)(x - 2 * dir - legOffset), (int)(y + 20), 5, 18);

        // Front arm (holding club)
        g2d.setColor(shirt);
        g2d.fillRect((int)(x - 12 * dir), (int)(y - 5 - armOffset), 4, 16);

        // Golf club
        g2d.setColor(new Color(139, 90, 43));
        g2d.setStroke(new BasicStroke(2));
        int clubX = (int)(x - 15 * dir);
        g2d.drawLine(clubX, (int)(y + 10), clubX, (int)(y + 35));
        g2d.setColor(new Color(192, 192, 192));
        g2d.fillOval(clubX - 3, (int)(y + 33), 8, 6);

        // Head
        g2d.setColor(skin);
        g2d.fillOval((int)(x - 8), (int)(y - 28), 16, 18);

        // Hair
        g2d.setColor(hair);
        g2d.fillArc((int)(x - 8), (int)(y - 30), 16, 12, 0, 180);

        // Cap
        g2d.setColor(shirt.darker());
        g2d.fillArc((int)(x - 9), (int)(y - 32), 18, 10, 0, 180);
        g2d.fillRect((int)(x + (facingRight ? 6 : -15)), (int)(y - 28), 9, 3);

        // Face details
        if (!silhouette) {
            g2d.setColor(Color.BLACK);
            g2d.fillOval((int)(x + 3 * dir), (int)(y - 22), 3, 3); // Eye
            g2d.setColor(new Color(200, 150, 120));
            g2d.fillArc((int)(x - 3), (int)(y - 18), 6, 4, 0, -180); // Mouth
        }

        // Shoes
        g2d.setColor(shoes);
        g2d.fillOval((int)(x - 8 * dir + legOffset), (int)(y + 36), 10, 6);
        g2d.fillOval((int)(x - 4 * dir - legOffset), (int)(y + 36), 10, 6);
    }

    private void drawGolfBall(Graphics2D g2d, double x, double y, int r, Color c) {
        RadialGradientPaint ball = new RadialGradientPaint(
                new Point2D.Double(x - r/3, y - r/3), r,
                new float[]{0f, 0.6f, 1f},
                new Color[]{Color.WHITE, c, c.darker()});
        g2d.setPaint(ball);
        g2d.fillOval((int)x - r, (int)y - r, r*2, r*2);

        // Dimples
        g2d.setColor(new Color(0, 0, 0, 25));
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                g2d.fillOval((int)(x + i*r/2), (int)(y + j*r/2), 2, 2);
            }
        }
    }

    private void drawAiming(Graphics2D g2d) {
        if (state != GameState.AIMING || players[currentPlayer-1] == null) return;

        Player p = players[currentPlayer-1];
        double dx = mousePos.x - p.x;
        double dy = mousePos.y - p.y;
        double dist = Math.sqrt(dx*dx + dy*dy);
        double power = Math.min(dist * 0.1, maxPower);

        // Trajectory line
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{12, 6}, 0));

        Color lineColor = power < maxPower*0.4 ? new Color(50, 205, 50) :
                power < maxPower*0.75 ? new Color(255, 215, 0) : new Color(220, 20, 60);
        g2d.setColor(lineColor);
        g2d.drawLine((int)p.x, (int)p.y, mousePos.x, mousePos.y);

        // Power bar
        int bw = 120, bh = 14;
        int bx = (int)p.x - bw/2, by = (int)p.y - 55;
        g2d.setColor(Color.BLACK);
        g2d.drawRect(bx, by, bw, bh);
        g2d.setColor(lineColor);
        g2d.fillRect(bx + 1, by + 1, (int)((power/maxPower)*bw) - 1, bh - 1);

        // Arrow head
        double angle = Math.atan2(dy, dx);
        g2d.setColor(lineColor);
        g2d.setStroke(new BasicStroke(2));
        int al = 12;
        g2d.drawLine(mousePos.x, mousePos.y,
                (int)(mousePos.x - al*Math.cos(angle-0.5)), (int)(mousePos.y - al*Math.sin(angle-0.5)));
        g2d.drawLine(mousePos.x, mousePos.y,
                (int)(mousePos.x - al*Math.cos(angle+0.5)), (int)(mousePos.y - al*Math.sin(angle+0.5)));
    }

    private void drawParticles(Graphics2D g2d) {
        for (Particle p : particles) {
            int alpha = (int)(255.0 * p.life / p.maxLife);
            g2d.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alpha));
            g2d.fillOval((int)p.x, (int)p.y, 4, 4);
        }
    }

    private void drawOverlay(Graphics2D g2d) {
        // Score panel
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.fillRoundRect(15, 15, 280, 90, 15, 15);
        g2d.setColor(new Color(218, 165, 32));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(15, 15, 280, 90, 15, 15);

        g2d.setFont(new Font("Georgia", Font.BOLD, 16));
        if (players[0] != null) {
            g2d.setColor(players[0].uniformColor);
            g2d.fillOval(30, 28, 12, 12);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Player 1: " + players[0].strokes + " strokes", 50, 40);
        }
        if (players[1] != null) {
            g2d.setColor(players[1].uniformColor);
            g2d.fillOval(30, 58, 12, 12);
            g2d.setColor(Color.WHITE);
            String label = vsComputer ? "Computer: " : "Player 2: ";
            g2d.drawString(label + players[1].strokes + " strokes", 50, 70);
        }

        // Snow overlay
        g2d.setColor(new Color(255, 255, 255, 180));
        for (Point2D.Double s : snowflakes) {
            g2d.fillOval((int)s.x, (int)s.y, 3, 3);
        }

        // Weather info
        g2d.setFont(new Font("Georgia", Font.BOLD, 14));
        g2d.setColor(new Color(255, 250, 250, 200));
        g2d.drawString("❄️ Moscow  -5°C  Wind: " + (int)(Math.sin(time)*8) + " km/h", 1150, 30);
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, 1400, 800);

        g2d.setFont(new Font("Georgia", Font.BOLD, 52));
        g2d.setColor(new Color(255, 215, 0));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(winnerMsg, (1400 - fm.stringWidth(winnerMsg)) / 2, 360);

        g2d.setFont(new Font("Georgia", Font.PLAIN, 24));
        g2d.setColor(Color.WHITE);
        String msg = "Click 'New Game' for another round in Red Square";
        fm = g2d.getFontMetrics();
        g2d.drawString(msg, (1400 - fm.stringWidth(msg)) / 2, 420);

        // Fireworks
        for (int i = 0; i < 5; i++) {
            int fx = 200 + i * 250;
            int fy = 150 + (int)(Math.sin(time * 3 + i) * 30);
            drawFirework(g2d, fx, fy, i);
        }
    }

    private void drawFirework(Graphics2D g2d, int x, int y, int seed) {
        Color[] colors = {Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA};
        g2d.setColor(colors[seed % colors.length]);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6 + time * 2;
            int r = 30 + (int)(Math.sin(time * 4 + seed) * 10);
            g2d.drawLine(x, y,
                    (int)(x + Math.cos(a) * r), (int)(y + Math.sin(a) * r));
        }
    }
}