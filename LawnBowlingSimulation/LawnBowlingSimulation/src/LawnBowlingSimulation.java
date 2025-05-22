import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

// Main game class
public class LawnBowlingSimulation extends JFrame {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int RINK_WIDTH = 600;
    private static final int RINK_HEIGHT = 400;
    private static final int BOWL_RADIUS = 15;
    private static final int JACK_RADIUS = 10;
    private static final double FRICTION = 0.98;
    private static final double GRAVITY = 0.1;
    private static final int MAX_ENDS = 3;

    private GamePanel gamePanel;
    private JLabel scoreLabel;
    private JLabel endLabel;
    private JLabel messageLabel;
    private int score;
    private int currentEnd;
    private boolean gameOver;

    public LawnBowlingSimulation() {
        setTitle("Lawn Bowling Simulation");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        score = 0;
        currentEnd = 1;
        gameOver = false;

        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel();
        scoreLabel = new JLabel("Score: 0");
        endLabel = new JLabel("End: 1/" + MAX_ENDS);
        messageLabel = new JLabel("Press SPACE to bowl, arrow keys to aim");
        infoPanel.add(scoreLabel);
        infoPanel.add(endLabel);
        infoPanel.add(messageLabel);
        add(infoPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    // Game panel for rendering and game logic
    class GamePanel extends JPanel implements ActionListener, KeyListener {
        private Bowl playerBowl;
        private Jack jack;
        private ArrayList<Bowl> bowls;
        private double aimAngle;
        private double power;
        private boolean isBowling;
        private Timer timer;
        private Random random;

        public GamePanel() {
            setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
            setFocusable(true);
            addKeyListener(this);
            playerBowl = new Bowl(RINK_WIDTH / 2, RINK_HEIGHT - 50, BOWL_RADIUS);
            jack = new Jack(RINK_WIDTH / 2, 50);
            bowls = new ArrayList<>();
            aimAngle = Math.PI / 2; // Upward direction
            power = 5.0;
            isBowling = false;
            random = new Random();
            timer = new Timer(16, this); // ~60 FPS
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw rink
            g2d.setColor(new Color(34, 139, 34)); // Green
            g2d.fillRect((WINDOW_WIDTH - RINK_WIDTH) / 2, (WINDOW_HEIGHT - RINK_HEIGHT) / 2, RINK_WIDTH, RINK_HEIGHT);

            // Draw boundary
            g2d.setColor(Color.WHITE);
            g2d.drawRect((WINDOW_WIDTH - RINK_WIDTH) / 2, (WINDOW_HEIGHT - RINK_HEIGHT) / 2, RINK_WIDTH, RINK_HEIGHT);

            // Draw jack
            jack.draw(g2d);

            // Draw bowls
            for (Bowl bowl : bowls) {
                bowl.draw(g2d);
            }

            // Draw player bowl and aiming line
            if (!isBowling) {
                playerBowl.draw(g2d);
                drawAimingLine(g2d);
            }

            // Draw power meter
            drawPowerMeter(g2d);
        }

        private void drawAimingLine(Graphics2D g2d) {
            g2d.setColor(Color.RED);
            int startX = (int) playerBowl.x;
            int startY = (int) playerBowl.y;
            int endX = startX + (int) (50 * Math.cos(aimAngle));
            int endY = startY - (int) (50 * Math.sin(aimAngle));
            g2d.drawLine(startX, startY, endX, endY);
        }

        private void drawPowerMeter(Graphics2D g2d) {
            g2d.setColor(Color.YELLOW);
            int meterWidth = (int) (power * 10);
            g2d.fillRect(20, WINDOW_HEIGHT - 50, meterWidth, 20);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(20, WINDOW_HEIGHT - 50, 100, 20);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            updateGame();
            repaint();
        }

        private void updateGame() {
            if (gameOver) return;

            if (isBowling) {
                for (Bowl bowl : bowls) {
                    bowl.update();
                    checkCollisions(bowl);
                }

                // Check if all bowls have stopped
                boolean allStopped = true;
                for (Bowl bowl : bowls) {
                    if (bowl.isMoving()) {
                        allStopped = false;
                        break;
                    }
                }

                if (allStopped && !bowls.isEmpty()) {
                    calculateScore();
                    if (currentEnd < MAX_ENDS) {
                        currentEnd++;
                        resetForNextEnd();
                    } else {
                        gameOver = true;
                        messageLabel.setText("Game Over! Final Score: " + score);
                    }
                }
            }
        }

        private void checkCollisions(Bowl bowl) {
            // Bowl-jack collision
            double dx = bowl.x - jack.x;
            double dy = bowl.y - jack.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance < BOWL_RADIUS + JACK_RADIUS) {
                resolveCollision(bowl, jack);
            }

            // Bowl-bowl collisions
            for (Bowl other : bowls) {
                if (bowl != other) {
                    dx = bowl.x - other.x;
                    dy = bowl.y - other.y;
                    distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance < 2 * BOWL_RADIUS) {
                        resolveBowlCollision(bowl, other);
                    }
                }
            }
        }

        private void resolveCollision(Bowl bowl, Jack jack) {
            double dx = bowl.x - jack.x;
            double dy = bowl.y - jack.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            double overlap = (BOWL_RADIUS + JACK_RADIUS - distance) / 2;

            // Move bowl back to avoid overlap
            double nx = dx / distance;
            double ny = dy / distance;
            bowl.x += nx * overlap;
            bowl.y += ny * overlap;

            // Update velocities (simple elastic collision)
            double tempVx = bowl.vx;
            double tempVy = bowl.vy;
            bowl.vx = -tempVx * 0.5;
            bowl.vy = -tempVy * 0.5;
        }

        private void resolveBowlCollision(Bowl bowl1, Bowl bowl2) {
            double dx = bowl1.x - bowl2.x;
            double dy = bowl1.y - bowl2.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            double overlap = (2 * BOWL_RADIUS - distance) / 2;

            // Move bowls apart
            double nx = dx / distance;
            double ny = dy / distance;
            bowl1.x += nx * overlap;
            bowl1.y += ny * overlap;
            bowl2.x -= nx * overlap;
            bowl2.y -= ny * overlap;

            // Update velocities
            double v1n = bowl1.vx * nx + bowl1.vy * ny;
            double v2n = bowl2.vx * nx + bowl2.vy * ny;
            bowl1.vx += (v2n - v1n) * nx * 0.5;
            bowl1.vy += (v2n - v1n) * ny * 0.5;
            bowl2.vx += (v1n - v2n) * nx * 0.5;
            bowl2.vy += (v1n - v2n) * ny * 0.5;
        }

        private void calculateScore() {
            double minDistance = Double.MAX_VALUE;
            Bowl closestBowl = null;
            for (Bowl bowl : bowls) {
                double dx = bowl.x - jack.x;
                double dy = bowl.y - jack.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestBowl = bowl;
                }
            }
            if (closestBowl == playerBowl) {
                score += 10;
                scoreLabel.setText("Score: " + score);
            }
        }

        private void resetForNextEnd() {
            bowls.clear();
            playerBowl = new Bowl(RINK_WIDTH / 2, RINK_HEIGHT - 50, BOWL_RADIUS);
            jack = new Jack(RINK_WIDTH / 2, 50);
            isBowling = false;
            aimAngle = Math.PI / 2;
            power = 5.0;
            endLabel.setText("End: " + currentEnd + "/" + MAX_ENDS);
            messageLabel.setText("Press SPACE to bowl, arrow keys to aim");
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (gameOver) return;

            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    aimAngle += 0.1;
                    break;
                case KeyEvent.VK_RIGHT:
                    aimAngle -= 0.1;
                    break;
                case KeyEvent.VK_UP:
                    power = Math.min(power + 0.5, 10.0);
                    break;
                case KeyEvent.VK_DOWN:
                    power = Math.max(power - 0.5, 1.0);
                    break;
                case KeyEvent.VK_SPACE:
                    if (!isBowling) {
                        isBowling = true;
                        playerBowl.vx = power * Math.cos(aimAngle);
                        playerBowl.vy = -power * Math.sin(aimAngle);
                        bowls.add(playerBowl);
                        // Add opponent bowls
                        for (int i = 0; i < 2; i++) {
                            Bowl opponentBowl = new Bowl(
                                    RINK_WIDTH / 2 + random.nextInt(100) - 50,
                                    100 + random.nextInt(50),
                                    BOWL_RADIUS
                            );
                            opponentBowl.vx = (random.nextDouble() - 0.5) * 5;
                            opponentBowl.vy = -random.nextDouble() * 5;
                            opponentBowl.color = Color.BLACK;
                            bowls.add(opponentBowl);
                        }
                        messageLabel.setText("Bowls rolling...");
                    }
                    break;
            }
            repaint();
        }

        @Override
        public void keyReleased(KeyEvent e) {}
        @Override
        public void keyTyped(KeyEvent e) {}
    }

    // Bowl class
    class Bowl {
        double x, y, vx, vy;
        int radius;
        Color color;

        public Bowl(double x, double y, int radius) {
            this.x = x + (WINDOW_WIDTH - RINK_WIDTH) / 2;
            this.y = y + (WINDOW_HEIGHT - RINK_HEIGHT) / 2;
            this.vx = 0;
            this.vy = 0;
            this.radius = radius;
            this.color = Color.RED;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += GRAVITY;
            vx *= FRICTION;
            vy *= FRICTION;

            // Boundary collisions
            int rinkLeft = (WINDOW_WIDTH - RINK_WIDTH) / 2;
            int rinkRight = rinkLeft + RINK_WIDTH;
            int rinkTop = (WINDOW_HEIGHT - RINK_HEIGHT) / 2;
            int rinkBottom = rinkTop + RINK_HEIGHT;

            if (x - radius < rinkLeft) {
                x = rinkLeft + radius;
                vx = -vx * 0.8;
            }
            if (x + radius > rinkRight) {
                x = rinkRight - radius;
                vx = -vx * 0.8;
            }
            if (y - radius < rinkTop) {
                y = rinkTop + radius;
                vy = -vy * 0.8;
            }
            if (y + radius > rinkBottom) {
                y = rinkBottom - radius;
                vy = -vy * 0.8;
            }

            // Stop if velocity is very low
            if (Math.abs(vx) < 0.1 && Math.abs(vy) < 0.1) {
                vx = 0;
                vy = 0;
            }
        }

        public boolean isMoving() {
            return Math.abs(vx) > 0 || Math.abs(vy) > 0;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.fillOval((int)(x - radius), (int)(y - radius), 2 * radius, 2 * radius);
        }
    }

    // Jack class
    class Jack {
        double x, y;
        int radius;

        public Jack(double x, double y) {
            this.x = x + (WINDOW_WIDTH - RINK_WIDTH) / 2;
            this.y = y + (WINDOW_HEIGHT - RINK_HEIGHT) / 2;
            this.radius = JACK_RADIUS;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int)(x - radius), (int)(y - radius), 2 * radius, 2 * radius);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LawnBowlingSimulation());
    }
}