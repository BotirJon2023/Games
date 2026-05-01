import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class VirtualGolfChallenge extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(VirtualGolfChallenge::new);
    }

    private GamePanel gamePanel;
    private JButton shootButton;
    private JButton newGameButton;
    private JButton modeButton;
    private JLabel statusLabel;

    private boolean vsComputer = false;
    private int currentPlayer = 1; // 1 or 2
    private boolean shotInProgress = false;

    public VirtualGolfChallenge() {
        super("Virtual Golf Challenge - Seaside Edition");

        gamePanel = new GamePanel();
        shootButton = new JButton("Shoot");
        newGameButton = new JButton("New Game");
        modeButton = new JButton("Mode: Player vs Player");
        statusLabel = new JLabel("Player 1's turn", SwingConstants.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        controlPanel.add(newGameButton);
        controlPanel.add(modeButton);
        controlPanel.add(shootButton);

        setLayout(new BorderLayout());
        add(gamePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.NORTH);
        add(statusLabel, BorderLayout.SOUTH);

        shootButton.addActionListener(e -> onShoot());
        newGameButton.addActionListener(e -> onNewGame());
        modeButton.addActionListener(e -> onToggleMode());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void onNewGame() {
        if (shotInProgress) return;
        gamePanel.resetGame();
        currentPlayer = 1;
        statusLabel.setText("Player 1's turn");
    }

    private void onToggleMode() {
        if (shotInProgress) return;
        vsComputer = !vsComputer;
        if (vsComputer) {
            modeButton.setText("Mode: Player vs Computer");
            statusLabel.setText("Player 1 vs Computer - Player 1's turn");
        } else {
            modeButton.setText("Mode: Player vs Player");
            statusLabel.setText("Player 1's turn");
        }
        gamePanel.resetGame();
        currentPlayer = 1;
    }

    private void onShoot() {
        if (shotInProgress) return;

        // Simple random power for demo; you can replace with sliders or mouse input
        Random rand = new Random();
        double power = 40 + rand.nextInt(40); // 40–80
        double angle = 45; // fixed angle for simplicity

        if (vsComputer && currentPlayer == 2) {
            statusLabel.setText("Computer is shooting...");
        } else {
            statusLabel.setText("Player " + currentPlayer + " is shooting...");
        }

        shotInProgress = true;
        gamePanel.shoot(currentPlayer, power, angle, () -> {
            // Callback when shot animation finishes
            shotInProgress = false;
            if (currentPlayer == 1) {
                if (vsComputer) {
                    currentPlayer = 2;
                    // Trigger computer shot automatically
                    SwingUtilities.invokeLater(() -> {
                        try {
                            Thread.sleep(600);
                        } catch (InterruptedException ignored) {}
                        onShoot();
                    });
                } else {
                    currentPlayer = 2;
                    statusLabel.setText("Player 2's turn");
                }
            } else {
                currentPlayer = 1;
                // After both have shot, evaluate who is closer
                String result = gamePanel.evaluateWinner(vsComputer);
                statusLabel.setText(result + " | New round: Player 1's turn");
            }
        });
    }

    // ---------------- Game Panel ----------------

    static class GamePanel extends JPanel {

        private static final int GROUND_Y = 420;
        private static final int HOLE_X = 750;
        private static final int HOLE_Y = GROUND_Y - 5;

        private double ballX, ballY;
        private boolean ballVisible = false;

        private double p1LastDistance = Double.MAX_VALUE;
        private double p2LastDistance = Double.MAX_VALUE;

        private Timer animationTimer;
        private double t;
        private double v0;
        private double angleRad;
        private double startX, startY;
        private int currentPlayerForShot;

        private final Color skyTop = new Color(80, 160, 255);
        private final Color skyBottom = new Color(180, 220, 255);
        private final Color seaColor = new Color(0, 120, 200);
        private final Color sandColor = new Color(230, 210, 150);
        private final Color grassColor = new Color(60, 180, 80);

        public GamePanel() {
            setBackground(Color.CYAN);
        }

        public void resetGame() {
            p1LastDistance = Double.MAX_VALUE;
            p2LastDistance = Double.MAX_VALUE;
            ballVisible = false;
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }
            repaint();
        }

        public void shoot(int player, double power, double angleDeg, Runnable onFinish) {
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }

            this.currentPlayerForShot = player;
            this.v0 = power / 2.0; // scale down
            this.angleRad = Math.toRadians(angleDeg);
            this.t = 0;

            // Start positions: Player 1 on left, Player 2 on slightly right
            if (player == 1) {
                startX = 80;
            } else {
                startX = 180;
            }
            startY = GROUND_Y - 10;

            ballVisible = true;

            animationTimer = new Timer(25, null);
            animationTimer.addActionListener(e -> {
                t += 0.15;
                double g = 9.8;
                double vx = v0 * Math.cos(angleRad);
                double vy = v0 * Math.sin(angleRad);

                ballX = startX + vx * t * 5; // scale for screen
                ballY = startY - (vy * t - 0.5 * g * t * t) * 5;

                // Stop when ball hits ground or goes off screen
                if (ballY >= GROUND_Y - 5 || ballX > getWidth() + 50) {
                    ballY = GROUND_Y - 5;
                    animationTimer.stop();
                    updateDistanceForPlayer();
                    repaint();
                    if (onFinish != null) {
                        onFinish.run();
                    }
                } else {
                    repaint();
                }
            });
            animationTimer.start();
        }

        private void updateDistanceForPlayer() {
            double dx = ballX - HOLE_X;
            double dy = ballY - HOLE_Y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (currentPlayerForShot == 1) {
                p1LastDistance = dist;
            } else {
                p2LastDistance = dist;
            }
        }

        public String evaluateWinner(boolean vsComputer) {
            String p2Name = vsComputer ? "Computer" : "Player 2";
            if (p1LastDistance < p2LastDistance) {
                return "Player 1 wins this round!";
            } else if (p2LastDistance < p1LastDistance) {
                return p2Name + " wins this round!";
            } else {
                return "It's a tie!";
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Sky gradient
            GradientPaint sky = new GradientPaint(0, 0, skyTop, 0, h / 2f, skyBottom);
            g2.setPaint(sky);
            g2.fillRect(0, 0, w, h / 2);

            // Sea
            g2.setColor(seaColor);
            g2.fillRect(0, h / 2, w, h / 2 - 80);

            // Sun
            g2.setColor(new Color(255, 230, 120));
            g2.fillOval(w - 140, 40, 80, 80);

            // Sand beach
            g2.setColor(sandColor);
            g2.fillRect(0, GROUND_Y - 40, w, 80);

            // Grass strip near hole
            g2.setColor(grassColor);
            g2.fillRect(HOLE_X - 40, GROUND_Y - 30, 120, 30);

            // Waves (simple arcs)
            g2.setColor(new Color(220, 240, 255, 180));
            for (int i = 0; i < w; i += 60) {
                g2.drawArc(i, h / 2 + 20, 60, 20, 0, 180);
            }

            // Hole flag
            g2.setColor(Color.DARK_GRAY);
            g2.fillOval(HOLE_X - 6, HOLE_Y, 12, 6);
            g2.setColor(Color.BLACK);
            g2.fillRect(HOLE_X, HOLE_Y - 60, 3, 60);
            g2.setColor(Color.RED);
            int[] fx = {HOLE_X + 3, HOLE_X + 3 + 35, HOLE_X + 3};
            int[] fy = {HOLE_Y - 60, HOLE_Y - 50, HOLE_Y - 40};
            g2.fillPolygon(fx, fy, 3);

            // Players (simple colorful characters)
            drawPlayer(g2, 80, GROUND_Y - 10, new Color(255, 100, 100), "P1");
            drawPlayer(g2, 180, GROUND_Y - 10, new Color(100, 180, 255), "P2");

            // Ball
            if (ballVisible) {
                g2.setColor(Color.WHITE);
                g2.fillOval((int) ballX - 5, (int) ballY - 5, 10, 10);
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawOval((int) ballX - 5, (int) ballY - 5, 10, 10);
            }

            // Decorative clouds
            g2.setColor(new Color(255, 255, 255, 230));
            drawCloud(g2, 80, 80);
            drawCloud(g2, 260, 60);
            drawCloud(g2, 480, 90);

            g2.dispose();
        }

        private void drawPlayer(Graphics2D g2, int x, int y, Color color, String label) {
            // Body
            g2.setColor(color);
            g2.fillOval(x - 10, y - 30, 20, 20); // head
            g2.fillRect(x - 8, y - 10, 16, 20);  // torso
            // Legs
            g2.drawLine(x - 5, y + 10, x - 10, y + 25);
            g2.drawLine(x + 5, y + 10, x + 10, y + 25);
            // Arms
            g2.drawLine(x - 8, y, x - 20, y + 5);
            g2.drawLine(x + 8, y, x + 20, y + 5);

            // Label
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            g2.drawString(label, x - 8, y - 35);
        }

        private void drawCloud(Graphics2D g2, int x, int y) {
            g2.fillOval(x, y, 40, 25);
            g2.fillOval(x + 20, y - 10, 40, 30);
            g2.fillOval(x + 35, y, 40, 25);
        }
    }
}
