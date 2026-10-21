import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class HoverboardRacingAdventure extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private MenuPanel menuPanel;
    private GamePanel gamePanel;

    public HoverboardRacingAdventure() {
        setTitle("Hoverboard Racing Adventure: Dragon's Path");
        setSize(800, 650); // Extra 50px for window borders
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        menuPanel = new MenuPanel();
        gamePanel = new GamePanel();

        mainPanel.add(menuPanel, "MENU");
        mainPanel.add(gamePanel, "GAME");

        add(mainPanel);
    }

    public void showGame(boolean vsAI) {
        gamePanel.initGame(vsAI);
        cardLayout.show(mainPanel, "GAME");
        gamePanel.startGame();
    }

    public void showMenu() {
        gamePanel.stopGame();
        cardLayout.show(mainPanel, "MENU");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HoverboardRacingAdventure game = new HoverboardRacingAdventure();
            game.setVisible(true);
        });
    }

    // ==========================================
    // MENU PANEL
    // ==========================================
    class MenuPanel extends JPanel {
        public MenuPanel() {
            setLayout(new GridBagLayout());
            setBackground(new Color(20, 5, 5)); // Dark festive red/black

            JPanel centerBox = new JPanel();
            centerBox.setLayout(new BoxLayout(centerBox, BoxLayout.Y_AXIS));
            centerBox.setOpaque(false);

            JLabel title = new JLabel("HOVERBOARD RACING");
            title.setFont(new Font("Serif", Font.BOLD, 48));
            title.setForeground(new Color(255, 215, 0)); // Gold
            title.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel subtitle = new JLabel("Dragon's Path");
            subtitle.setFont(new Font("Serif", Font.ITALIC, 32));
            subtitle.setForeground(new Color(255, 100, 100)); // Light Red
            subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

            JButton btnAI = createNeonButton("1 PLAYER (VS COMPUTER)", new Color(255, 215, 0));
            JButton btnPvP = createNeonButton("2 PLAYERS (LOCAL)", new Color(255, 100, 100));

            btnAI.addActionListener(e -> showGame(true));
            btnPvP.addActionListener(e -> showGame(false));

            centerBox.add(Box.createVerticalStrut(50));
            centerBox.add(title);
            centerBox.add(subtitle);
            centerBox.add(Box.createVerticalStrut(60));
            centerBox.add(btnAI);
            centerBox.add(Box.createVerticalStrut(20));
            centerBox.add(btnPvP);

            add(centerBox);
        }

        private JButton createNeonButton(String text, Color color) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("SansSerif", Font.BOLD, 18));
            btn.setForeground(color);
            btn.setBackground(new Color(40, 10, 10));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createLineBorder(color, 2));
            btn.setMaximumSize(new Dimension(350, 50));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);

            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    btn.setBackground(color.darker());
                    btn.setForeground(Color.WHITE);
                }
                public void mouseExited(MouseEvent e) {
                    btn.setBackground(new Color(40, 10, 10));
                    btn.setForeground(color);
                }
            });
            return btn;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            // Beautiful background gradient
            GradientPaint gp = new GradientPaint(0, 0, new Color(40, 0, 0), 0, getHeight(), new Color(10, 0, 20));
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Draw decorative lanterns in the menu
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            for(int i=0; i<5; i++) {
                gamePanel.drawLantern(g2d, 100 + i * 150, 100 + (i%2)*50);
            }
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }

    // ==========================================
    // GAME PANEL
    // ==========================================
    class GamePanel extends JPanel implements ActionListener {
        private Timer gameTimer;
        private boolean isVsAI;
        private Set<Integer> keysPressed = new HashSet<>();

        private Racer player1, player2;
        private List<Obstacle> obstacles = new ArrayList<>();
        private List<Boost> boosts = new ArrayList<>();

        private double cameraY = 0;
        private double nextSpawnDist = 300;
        private String winnerText = "";
        private boolean gameOver = false;

        private static final int WIDTH = 800;
        private static final int HEIGHT = 600;
        private static final double TRACK_LEFT = 200;
        private static final double TRACK_RIGHT = 600;
        private static final double FINISH_LINE = 5000;

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);

            addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) { keysPressed.add(e.getKeyCode()); }
                public void keyReleased(KeyEvent e) { keysPressed.remove(e.getKeyCode()); }
            });
        }

        public void initGame(boolean vsAI) {
            this.isVsAI = vsAI;
            player1 = new Racer(TRACK_LEFT + 100, Color.RED, new Color(255, 100, 100), "P1");
            player2 = new Racer(TRACK_RIGHT - 100, new Color(255, 215, 0), new Color(255, 255, 150), isVsAI ? "CPU" : "P2");

            obstacles.clear();
            boosts.clear();
            cameraY = 0;
            nextSpawnDist = 300;
            winnerText = "";
            gameOver = false;
        }

        public void startGame() {
            requestFocusInWindow();
            if (gameTimer == null) {
                gameTimer = new Timer(16, this); // ~60 FPS
            }
            gameTimer.start();
        }

        public void stopGame() {
            if (gameTimer != null) gameTimer.stop();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            update();
            repaint();
        }

        private void update() {
            if (gameOver) {
                if (keysPressed.contains(KeyEvent.VK_SPACE)) {
                    showMenu();
                }
                return;
            }

            // Input
            handleInput(player1, KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D);
            if (isVsAI) {
                updateAI(player2);
            } else {
                handleInput(player2, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT);
            }

            // Update Racers
            player1.update();
            player2.update();

            // Camera follows the leader
            double leaderDist = Math.max(player1.distance, player2.distance);
            cameraY = leaderDist - 400;

            // Spawning
            nextSpawnDist -= Math.max(player1.speed, player2.speed) * 0.016;
            if (nextSpawnDist <= 0) {
                spawnEntities();
                nextSpawnDist = 200 + Math.random() * 300;
            }

            // Update & Collisions
            obstacles.removeIf(o -> o.screenY() > HEIGHT + 100);
            boosts.removeIf(b -> b.screenY() > HEIGHT + 100);

            for (Obstacle o : obstacles) {
                if (checkCollision(player1, o)) player1.crash();
                if (checkCollision(player2, o)) player2.crash();
            }

            boosts.removeIf(b -> {
                if (checkBoost(player1, b)) { player1.boost(); return true; }
                if (checkBoost(player2, b)) { player2.boost(); return true; }
                return false;
            });

            // Win Condition
            if (player1.distance >= FINISH_LINE || player2.distance >= FINISH_LINE) {
                gameOver = true;
                if (player1.distance > player2.distance) {
                    winnerText = isVsAI ? "PLAYER WINS!" : "PLAYER 1 WINS!";
                } else {
                    winnerText = isVsAI ? "COMPUTER WINS!" : "PLAYER 2 WINS!";
                }
            }
        }

        private void handleInput(Racer r, int up, int down, int left, int right) {
            if (keysPressed.contains(up)) r.accelerate();
            if (keysPressed.contains(down)) r.brake();
            if (keysPressed.contains(left)) r.steer(-1);
            if (keysPressed.contains(right)) r.steer(1);
        }

        private void updateAI(Racer ai) {
            ai.accelerate();
            Obstacle threat = null;
            double minDist = Double.MAX_VALUE;

            for (Obstacle o : obstacles) {
                double rel = o.distance - ai.distance;
                if (rel > 0 && rel < 250 && rel < minDist && Math.abs(o.x - ai.x) < 70) {
                    threat = o;
                    minDist = rel;
                }
            }

            if (threat != null) {
                if (threat.x < ai.x) ai.steer(1);
                else ai.steer(-1);
            } else {
                Boost target = null;
                minDist = Double.MAX_VALUE;
                for (Boost b : boosts) {
                    double rel = b.distance - ai.distance;
                    if (rel > 0 && rel < 350 && rel < minDist) {
                        target = b;
                        minDist = rel;
                    }
                }
                if (target != null) {
                    if (target.x < ai.x) ai.steer(-0.7);
                    else ai.steer(0.7);
                } else {
                    if (ai.x < 400) ai.steer(0.4);
                    else if (ai.x > 400) ai.steer(-0.4);
                }
            }
        }

        private void spawnEntities() {
            double spawnDist = cameraY + HEIGHT + 200;
            double obsX = TRACK_LEFT + 50 + Math.random() * (TRACK_RIGHT - TRACK_LEFT - 100);
            obstacles.add(new Obstacle(obsX, spawnDist));

            if (Math.random() > 0.4) {
                double boostX = TRACK_LEFT + 50 + Math.random() * (TRACK_RIGHT - TRACK_LEFT - 100);
                boosts.add(new Boost(boostX, spawnDist + 100));
            }
        }

        private boolean checkCollision(Racer r, Obstacle o) {
            double relY = o.distance - r.distance;
            return Math.abs(relY) < 40 && Math.abs(o.x - r.x) < 45;
        }

        private boolean checkBoost(Racer r, Boost b) {
            double relY = b.distance - r.distance;
            return Math.abs(relY) < 35 && Math.abs(b.x - r.x) < 40;
        }

        // ==========================================
        // RENDERING
        // ==========================================
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawBackground(g2d);
            drawTrack(g2d);

            for (Boost b : boosts) drawBoost(g2d, b);
            for (Obstacle o : obstacles) drawObstacle(g2d, o);

            drawRacer(g2d, player1);
            drawRacer(g2d, player2);

            drawHUD(g2d);

            if (gameOver) drawGameOver(g2d);
        }

        private void drawBackground(Graphics2D g2d) {
            GradientPaint sky = new GradientPaint(0, 0, new Color(10, 5, 30), 0, HEIGHT, new Color(40, 10, 50));
            g2d.setPaint(sky);
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            // Parallax Lanterns
            double offset = (cameraY * 0.3) % 250;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
            for (double y = -offset; y < HEIGHT; y += 250) {
                drawLantern(g2d, 80, y);
                drawLantern(g2d, 720, y + 125);
            }
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        private void drawLantern(Graphics2D g2d, double x, double y) {
            // Glow
            RadialGradientPaint glow = new RadialGradientPaint(
                    (float)x, (float)y, 40,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 50, 0, 100), new Color(255, 50, 0, 0)}
            );
            g2d.setPaint(glow);
            g2d.fillOval((int)x - 40, (int)y - 40, 80, 80);

            // Body
            g2d.setColor(new Color(200, 20, 0));
            g2d.fillOval((int)x - 20, (int)y - 30, 40, 60);

            // Gold caps
            g2d.setColor(Color.yellow);
            g2d.fillRect((int)x - 15, (int)y - 35, 30, 8);
            g2d.fillRect((int)x - 15, (int)y + 27, 30, 8);

            // Tassel
            g2d.drawLine((int)x, (int)y + 35, (int)x, (int)y + 55);
        }

        private void drawTrack(Graphics2D g2d) {
            // Jade Track
            GradientPaint trackGrad = new GradientPaint((int)TRACK_LEFT, 0, new Color(0, 60, 50), (int)TRACK_RIGHT, 0, new Color(0, 100, 80));
            g2d.setPaint(trackGrad);
            g2d.fillRect((int)TRACK_LEFT, 0, (int)(TRACK_RIGHT - TRACK_LEFT), HEIGHT);

            // Glowing Golden Borders
            g2d.setColor(new Color(255, 215, 0, 150));
            g2d.setStroke(new BasicStroke(6));
            g2d.drawLine((int)TRACK_LEFT, 0, (int)TRACK_LEFT, HEIGHT);
            g2d.drawLine((int)TRACK_RIGHT, 0, (int)TRACK_RIGHT, HEIGHT);

            // Moving Grid Lines (Speed effect)
            g2d.setColor(new Color(255, 255, 255, 30));
            g2d.setStroke(new BasicStroke(2));
            double gridSpacing = 60;
            double offset = (cameraY % gridSpacing);
            for (double y = -offset; y < HEIGHT; y += gridSpacing) {
                g2d.drawLine((int)TRACK_LEFT, (int)y, (int)TRACK_RIGHT, (int)y);
            }
        }

        private void drawRacer(Graphics2D g2d, Racer r) {
            // Trail
            for (int i = 0; i < r.trail.size(); i++) {
                TrailPoint p = r.trail.get(i);
                float alpha = (float) i / r.trail.size() * 0.6f;
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2d.setColor(r.trailColor);
                int size = 10 + i;
                g2d.fillOval((int)(p.x - size/2), (int)(p.screenY - size/2), size, size);
            }
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            // Board Glow
            RadialGradientPaint glow = new RadialGradientPaint(
                    (float)r.x, (float)r.screenY, 60,
                    new float[]{0f, 1f},
                    new Color[]{new Color(r.color1.getRed(), r.color1.getGreen(), r.color1.getBlue(), 120), new Color(0, 0, 0, 0)}
            );
            g2d.setPaint(glow);
            g2d.fillOval((int)r.x - 60, (int)r.screenY - 60, 120, 120);

            // Save transform for rotation
            AffineTransform old = g2d.getTransform();
            g2d.translate(r.x, r.screenY);
            g2d.rotate(Math.toRadians(r.angle));

            // The Board (Sleek shape)
            GradientPaint boardGrad = new GradientPaint(-35, 0, r.color1, 35, 0, r.color2);
            g2d.setPaint(boardGrad);
            g2d.fillRoundRect(-35, -15, 70, 30, 20, 20);

            // Gold Trim
            g2d.setColor(Color.yellow);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(-35, -15, 70, 30, 20, 20);

            // Little Red Flag (Chinese element)
            g2d.setColor(Color.RED);
            g2d.fillRect(-2, -35, 2, 20); // pole
            g2d.fillPolygon(new int[]{-2, 15, -2}, new int[]{-35, -25, -15}, 3);

            // Rider (Simple silhouette)
            g2d.setColor(new Color(30, 30, 30));
            g2d.fillOval(-8, -25, 16, 16);

            g2d.setTransform(old); // Restore transform
        }

        private void drawObstacle(Graphics2D g2d, Obstacle o) {
            double sy = o.screenY();
            if (sy < -50 || sy > HEIGHT + 50) return;

            // Firecracker Glow
            RadialGradientPaint glow = new RadialGradientPaint(
                    (float)o.x, (float)sy, 40,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 100, 0, 100), new Color(255, 0, 0, 0)}
            );
            g2d.setPaint(glow);
            g2d.fillOval((int)o.x - 40, (int)sy - 40, 80, 80);

            // Firecracker Body
            GradientPaint bodyGrad = new GradientPaint((float)o.x - 15, (float)sy, Color.RED, (float)o.x + 15, (float)sy, new Color(150, 0, 0));
            g2d.setPaint(bodyGrad);
            g2d.fillRoundRect((int)o.x - 15, (int)sy - 25, 30, 50, 10, 10);

            // Gold Bands
            g2d.setColor(Color.yellow);
            g2d.fillRect((int)o.x - 15, (int)sy - 15, 30, 5);
            g2d.fillRect((int)o.x - 15, (int)sy + 10, 30, 5);

            // Fuse & Sparks
            g2d.setColor(Color.ORANGE);
            g2d.drawLine((int)o.x, (int)sy - 25, (int)o.x + 10, (int)sy - 35);
            g2d.setColor(Color.YELLOW);
            g2d.fillOval((int)o.x + 8, (int)sy - 38, 6, 6);
        }

        private void drawBoost(Graphics2D g2d, Boost b) {
            double sy = b.screenY();
            if (sy < -50 || sy > HEIGHT + 50) return;

            double pulse = Math.sin(System.currentTimeMillis() / 200.0) * 5 + 30;

            // Glow
            RadialGradientPaint glow = new RadialGradientPaint(
                    (float)b.x, (float)sy, (float)pulse,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 215, 0, 150), new Color(255, 0, 0, 0)}
            );
            g2d.setPaint(glow);
            g2d.fillOval((int) ((int)b.x - pulse), (int) ((int)sy - pulse), (int) (pulse * 2), (int) (pulse * 2));

            // Hongbao (Red Envelope)
            g2d.setColor(new Color(200, 20, 0));
            g2d.fillRoundRect((int)b.x - 15, (int)sy - 20, 30, 40, 5, 5);

            // Gold Circle & "Fu" abstraction
            g2d.setColor(Color.yellow);
            g2d.fillOval((int)b.x - 8, (int)sy - 8, 16, 16);
            g2d.setColor(new Color(200, 20, 0));
            g2d.fillRect((int)b.x - 2, (int)sy - 4, 4, 8);
        }

        private void drawHUD(Graphics2D g2d) {
            // Semi-transparent HUD background
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRoundRect(10, 10, 250, 80, 10, 10);
            g2d.setColor(Color.yellow);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(10, 10, 250, 80, 10, 10);

            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2d.setColor(Color.WHITE);
            g2d.drawString("P1 (Red Dragon): " + (int)player1.distance + "m", 25, 35);
            g2d.drawString((isVsAI ? "CPU" : "P2") + " (Gold Dragon): " + (int)player2.distance + "m", 25, 65);

            // Progress bars
            g2d.setColor(new Color(50, 50, 50));
            g2d.fillRect(25, 42, 200, 8);
            g2d.fillRect(25, 72, 200, 8);

            g2d.setColor(Color.RED);
            g2d.fillRect(25, 42, (int)((player1.distance / FINISH_LINE) * 200), 8);
            g2d.setColor(Color.yellow);
            g2d.fillRect(25, 72, (int)((player2.distance / FINISH_LINE) * 200), 8);
        }

        private void drawGameOver(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            g2d.setFont(new Font("Serif", Font.BOLD, 60));
            g2d.setColor(Color.yellow);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(winnerText);
            g2d.drawString(winnerText, (WIDTH - textWidth) / 2, HEIGHT / 2);

            g2d.setFont(new Font("SansSerif", Font.PLAIN, 24));
            g2d.setColor(Color.WHITE);
            String sub = "Press SPACE to return to menu";
            int subWidth = g2d.getFontMetrics().stringWidth(sub);
            g2d.drawString(sub, (WIDTH - subWidth) / 2, HEIGHT / 2 + 50);
        }
    }

    // ==========================================
    // GAME ENTITIES
    // ==========================================
    class Racer {
        double x, distance, speed, angle, screenY;
        Color color1, color2, trailColor;
        String name;
        List<TrailPoint> trail = new ArrayList<>();
        double crashTimer = 0;

        public Racer(double startX, Color c1, Color c2, String name) {
            this.x = startX;
            this.color1 = c1;
            this.color2 = c2;
            this.trailColor = c1;
            this.name = name;
            this.distance = 0;
            this.speed = 0;
            this.angle = 0;
            this.screenY = 600 - 150; // Fixed screen Y
        }

        public void update() {
            if (crashTimer > 0) {
                crashTimer -= 0.016;
                speed *= 0.95;
            } else {
                speed *= 0.98;
                if (speed < 0) speed = 0;
            }

            distance += speed * 0.016;

            if (x < 230) { x = 230; speed *= 0.8; }
            if (x > 570) { x = 570; speed *= 0.8; }

            trail.add(new TrailPoint(x, screenY));
            if (trail.size() > 25) trail.remove(0);
        }

        public void accelerate() {
            if (crashTimer <= 0) {
                speed += 5;
                if (speed > 400) speed = 400;
            }
        }

        public void brake() {
            speed -= 8;
            if (speed < 0) speed = 0;
        }

        public void steer(double dir) {
            x += dir * 5;
            angle = dir * 15;
        }

        public void crash() {
            if (crashTimer <= 0) {
                crashTimer = 1.0;
                speed *= 0.3;
            }
        }

        public void boost() {
            speed += 150;
            if (speed > 600) speed = 600;
        }
    }

    class Obstacle {
        double x, distance;
        public Obstacle(double x, double distance) {
            this.x = x;
            this.distance = distance;
        }
        public double screenY() {
            double cameraY;
            return 600 - 150 - (distance - cameraY);
        }
    }

    class Boost {
        double x, distance;
        public Boost(double x, double distance) {
            this.x = x;
            this.distance = distance;
        }
        public double screenY() {
            return 600 - 150 - (distance - cameraY);
        }
    }

    class TrailPoint {
        double x, screenY;
        public TrailPoint(double x, double screenY) {
            this.x = x;
            this.screenY = screenY;
        }
    }
}