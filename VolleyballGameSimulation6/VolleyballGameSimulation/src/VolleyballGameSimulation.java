import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VolleyballGameSimulation extends JFrame {

    // Game Constants
    public static final int WINDOW_WIDTH = 1000;
    public static final int WINDOW_HEIGHT = 700;
    public static final int COURT_WIDTH = 800;
    public static final int COURT_HEIGHT = 500;
    public static final int NET_HEIGHT = 180;
    public static final int NET_X = COURT_WIDTH / 2;

    // Game State Enums
    enum GameState {
        MENU,
        SERVING,
        PLAYING,
        POINT_OVER,
        GAME_OVER
    }

    private GamePanel gamePanel;

    public VolleyballGameSimulation() {
        super("Java Volleyball Simulation");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        this.setResizable(false);
        this.setLocationRelativeTo(null);

        gamePanel = new GamePanel();
        this.add(gamePanel);

        // Add keyboard listener for player control
        this.addKeyListener(gamePanel);
        this.setFocusable(true);

        this.setVisible(true);
    }

    public static void main(String[] args) {
        // Run on Event Dispatch Thread for Swing safety
        SwingUtilities.invokeLater(() -> new VolleyballGameSimulation());
    }

    /**
     * Inner Class: Vector2D
     * Helper class for 2D physics calculations.
     */
    static class Vector2D {
        double x, y;

        public Vector2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void add(Vector2D v) {
            this.x += v.x;
            this.y += v.y;
        }

        public void multiply(double scalar) {
            this.x *= scalar;
            this.y *= scalar;
        }

        public double magnitude() {
            return Math.sqrt(x * x + y * y);
        }

        public void normalize() {
            double mag = magnitude();
            if (mag > 0) {
                x /= mag;
                y /= mag;
            }
        }
    }

    /**
     * Inner Class: Particle
     * Used for visual effects like dust or hit sparks.
     */
    static class Particle {
        double x, y;
        double vx, vy;
        int life;
        Color color;

        public Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.vx = (Math.random() - 0.5) * 4;
            this.vy = (Math.random() - 0.5) * 4;
            this.life = 30 + (int)(Math.random() * 20);
            this.color = color;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 0.1; // Gravity for particles
            life--;
        }

        public void draw(Graphics2D g) {
            g.setColor(color);
            g.fillOval((int)x, (int)y, 3, 3);
        }

        public boolean isAlive() {
            return life > 0;
        }
    }

    /**
     * Inner Class: Ball
     * Handles ball physics and rendering.
     */
    static class Ball {
        double x, y;
        double vx, vy;
        double radius = 12;
        double rotation = 0;

        // Physics constants
        final double GRAVITY = 0.4;
        final double AIR_RESISTANCE = 0.995;
        final double BOUNCE_DAMPING = 0.75;

        public Ball() {
            reset();
        }

        public void reset() {
            x = 100;
            y = 300;
            vx = 0;
            vy = 0;
            rotation = 0;
        }

        public void serve(boolean toRight) {
            y = 250;
            x = toRight ? 100 : COURT_WIDTH - 100;
            vx = toRight ? 8 + Math.random() * 2 : -(8 + Math.random() * 2);
            vy = -6 - Math.random() * 3;
        }

        public void update() {
            // Apply Gravity
            vy += GRAVITY;

            // Apply Air Resistance
            vx *= AIR_RESISTANCE;
            vy *= AIR_RESISTANCE;

            // Update Position
            x += vx;
            y += vy;

            // Rotate ball based on velocity
            rotation += vx * 2;

            // Floor Collision
            if (y + radius > COURT_HEIGHT) {
                y = COURT_HEIGHT - radius;
                vy = -vy * BOUNCE_DAMPING;
                vx *= 0.8; // Friction on floor
                if (Math.abs(vy) < 1) vy = 0;
            }

            // Ceiling Collision
            if (y - radius < 0) {
                y = radius;
                vy = -vy * BOUNCE_DAMPING;
            }

            // Wall Collisions
            if (x - radius < 0) {
                x = radius;
                vx = -vx * BOUNCE_DAMPING;
            }
            if (x + radius > COURT_WIDTH) {
                x = COURT_WIDTH - radius;
                vx = -vx * BOUNCE_DAMPING;
            }
        }

        public void draw(Graphics2D g) {
            g.setColor(new Color(255, 165, 0)); // Orange ball
            g.fillOval((int)(x - radius), (int)(y - radius), (int)(radius * 2), (int)(radius * 2));

            // Draw seams to show rotation
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2));
            g.drawOval((int)(x - radius), (int)(y - radius), (int)(radius * 2), (int)(radius * 2));

            // Simple line to indicate spin
            int seamX = (int)(x + Math.cos(rotation) * radius);
            int seamY = (int)(y + Math.sin(rotation) * radius);
            g.drawLine((int)x, (int)y, seamX, seamY);
        }

        public Rectangle getBounds() {
            return new Rectangle((int)(x - radius), (int)(y - radius), (int)(radius * 2), (int)(radius * 2));
        }
    }

    /**
     * Inner Class: Player
     * Handles player movement, AI, and rendering.
     */
    static class Player {
        double x, y;
        double vx, vy;
        double width = 40;
        double height = 70;
        boolean isPlayer1; // True = Left side, False = Right side
        boolean isJumping = false;
        boolean isMoving = false;
        Color teamColor;

        // Stats
        final double MOVE_SPEED = 5.0;
        final double JUMP_FORCE = -14.0;
        final double GRAVITY = 0.6;

        public Player(boolean isPlayer1) {
            this.isPlayer1 = isPlayer1;
            this.teamColor = isPlayer1 ? Color.BLUE : Color.RED;
            reset();
        }

        public void reset() {
            x = isPlayer1 ? 150 : COURT_WIDTH - 190;
            y = COURT_HEIGHT - height;
            vx = 0;
            vy = 0;
        }

        public void updateInput(boolean left, boolean right, boolean jump) {
            if (isJumping) {
                vy += GRAVITY;
                y += vy;
                // Land on ground
                if (y >= COURT_HEIGHT - height) {
                    y = COURT_HEIGHT - height;
                    vy = 0;
                    isJumping = false;
                }
            } else {
                if (left) {
                    vx = -MOVE_SPEED;
                    isMoving = true;
                } else if (right) {
                    vx = MOVE_SPEED;
                    isMoving = true;
                } else {
                    vx = 0;
                    isMoving = false;
                }

                if (jump) {
                    vy = JUMP_FORCE;
                    isJumping = true;
                }

                x += vx;

                // Boundaries
                if (isPlayer1) {
                    if (x < 0) x = 0;
                    if (x > NET_X - width) x = NET_X - width;
                } else {
                    if (x < NET_X) x = NET_X;
                    if (x > COURT_WIDTH - width) x = COURT_WIDTH - width;
                }
            }
        }

        public void updateAI(Ball ball) {
            // Simple AI Logic
            double targetX = ball.x;

            // Only move if ball is on my side or coming towards me
            boolean ballOnMySide = isPlayer1 ? ball.x < NET_X : ball.x > NET_X;
            boolean ballComingToMe = isPlayer1 ? ball.vx > 0 : ball.vx < 0;

            if (ballOnMySide || ballComingToMe) {
                // Move towards ball X
                double center = x + width / 2;
                if (Math.abs(center - targetX) > 10) {
                    if (targetX < center) {
                        x -= MOVE_SPEED * 0.8; // AI is slightly slower than human
                        isMoving = true;
                    } else {
                        x += MOVE_SPEED * 0.8;
                        isMoving = true;
                    }
                } else {
                    isMoving = false;
                }

                // Jump if ball is low and close
                if (ball.y > COURT_HEIGHT - 150 && Math.abs(center - targetX) < 60 && !isJumping) {
                    // Random chance to miss or hit late for realism
                    if (Math.random() > 0.1) {
                        vy = JUMP_FORCE;
                        isJumping = true;
                    }
                }
            } else {
                // Return to base position
                double baseX = isPlayer1 ? 150 : COURT_WIDTH - 190;
                if (Math.abs(x - baseX) > 5) {
                    x += (baseX > x) ? 2 : -2;
                    isMoving = true;
                } else {
                    isMoving = false;
                }
            }

            // Apply Gravity if jumping
            if (isJumping) {
                vy += GRAVITY;
                y += vy;
                if (y >= COURT_HEIGHT - height) {
                    y = COURT_HEIGHT - height;
                    vy = 0;
                    isJumping = false;
                }
            }

            // Boundaries
            if (isPlayer1) {
                if (x < 0) x = 0;
                if (x > NET_X - width) x = NET_X - width;
            } else {
                if (x < NET_X) x = NET_X;
                if (x > COURT_WIDTH - width) x = COURT_WIDTH - width;
            }
        }

        public void draw(Graphics2D g) {
            g.setColor(teamColor);

            // Body
            g.fillRect((int)x, (int)y, (int)width, (int)height);

            // Head
            g.setColor(Color.PINK);
            g.fillOval((int)(x + 10), (int)(y - 20), 20, 20);

            // Eyes (direction based)
            g.setColor(Color.BLACK);
            int eyeOffset = isPlayer1 ? 12 : 4;
            g.fillOval((int)(x + eyeOffset), (int)(y - 15), 4, 4);

            // Jersey Number
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString(isPlayer1 ? "1" : "2", (int)(x + 15), (int)(y + 40));
        }

        public Rectangle getBounds() {
            // Hitbox slightly larger than visual for easier gameplay
            return new Rectangle((int)x - 5, (int)y - 25, (int)width + 10, (int)height + 25);
        }
    }

    /**
     * Inner Class: GamePanel
     * The main rendering and logic loop container.
     */
    class GamePanel extends JPanel implements ActionListener, KeyListener {

        private Timer timer;
        private GameState state = GameState.MENU;

        private Ball ball;
        private Player player1;
        private Player player2;
        private List<Particle> particles;

        private int score1 = 0;
        private int score2 = 0;
        private int serveTurn = 1; // 1 or 2
        private String commentary = "Press SPACE to Start";
        private int rallyCount = 0;

        private boolean keyLeft = false;
        private boolean keyRight = false;
        private boolean keyJump = false;

        public GamePanel() {
            this.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
            this.setBackground(new Color(30, 30, 40));
            this.setFocusable(true);

            ball = new Ball();
            player1 = new Player(true);
            player2 = new Player(false);
            particles = new ArrayList<>();

            timer = new Timer(1000 / 60, this); // 60 FPS
            timer.start();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            update();
            repaint();
        }

        private void update() {
            if (state == GameState.MENU || state == GameState.GAME_OVER) {
                return;
            }

            // 1. Update Entities
            player1.updateInput(keyLeft, keyRight, keyJump);
            player2.updateAI(ball);
            ball.update();

            // 2. Collision Detection: Player vs Ball
            checkPlayerBallCollision(player1);
            checkPlayerBallCollision(player2);

            // 3. Collision Detection: Ball vs Net
            checkNetCollision();

            // 4. Game Logic (Scoring)
            checkScoring();

            // 5. Update Particles
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.update();
                if (!p.isAlive()) particles.remove(i);
            }
        }

        private void checkPlayerBallCollision(Player p) {
            Rectangle pBounds = p.getBounds();
            Rectangle bBounds = ball.getBounds();

            if (pBounds.intersects(bBounds)) {
                // Calculate hit position relative to player center
                double playerCenterX = p.x + p.width / 2;
                double hitOffset = ball.x - playerCenterX;

                // Determine hit angle based on where ball hit player
                double angle = hitOffset / (p.width / 2); // -1 to 1

                // Apply velocity
                double hitPower = 12;
                if (p.isJumping) hitPower = 15; // Spike power

                ball.vx = angle * hitPower;
                ball.vy = -8 - Math.random() * 4; // Upward pop

                // Add particles
                for(int i=0; i<5; i++) {
                    particles.add(new Particle(ball.x, ball.y, Color.YELLOW));
                }

                rallyCount++;
                commentary = "Hit! Rally: " + rallyCount;
            }
        }

        private void checkNetCollision() {
            // Net is a vertical line at NET_X with height NET_HEIGHT from bottom
            int netTopY = COURT_HEIGHT - NET_HEIGHT;

            // Check if ball is within net horizontal range
            if (ball.x + ball.radius > NET_X - 5 && ball.x - ball.radius < NET_X + 5) {
                // Check if ball is hitting the net vertically
                if (ball.y + ball.radius > netTopY) {
                    // Bounce back
                    if (ball.x < NET_X) {
                        ball.x = NET_X - 5 - ball.radius;
                        ball.vx = -Math.abs(ball.vx) * 0.5;
                    } else {
                        ball.x = NET_X + 5 + ball.radius;
                        ball.vx = Math.abs(ball.vx) * 0.5;
                    }
                    commentary = "Net Touch!";
                }
            }
        }

        private void checkScoring() {
            // Ball hits floor
            if (ball.y + ball.radius >= COURT_HEIGHT) {
                handlePointEnd();
            }
            // Ball goes out of bounds horizontally (rare with walls, but possible logic)
            if (ball.x < 0 || ball.x > COURT_WIDTH) {
                handlePointEnd();
            }
        }

        private void handlePointEnd() {
            state = GameState.POINT_OVER;

            boolean p1Scored = ball.x > NET_X; // If ball lands on right side, P1 scores
            if (ball.x < NET_X) p1Scored = false;

            // Special case: if ball hits floor exactly on line, count as out (simplified)

            if (p1Scored) {
                score1++;
                commentary = "Player 1 Scores!";
                serveTurn = 1;
            } else {
                score2++;
                commentary = "Player 2 Scores!";
                serveTurn = 2;
            }

            // Check Win Condition (First to 5 for demo speed)
            if (score1 >= 5 || score2 >= 5) {
                state = GameState.GAME_OVER;
                commentary = (score1 >= 5 ? "Player 1" : "Player 2") + " Wins the Match!";
            } else {
                // Reset for next serve after short delay
                Timer resetTimer = new Timer(1500, evt -> {
                    resetRound();
                    ((Timer)evt.getSource()).stop();
                });
                resetTimer.setRepeats(false);
                resetTimer.start();
            }
        }

        private void resetRound() {
            ball.reset();
            player1.reset();
            player2.reset();
            rallyCount = 0;

            if (serveTurn == 1) {
                ball.serve(true);
                commentary = "Player 1 Serving...";
            } else {
                ball.serve(false);
                commentary = "Player 2 Serving...";
            }

            state = GameState.PLAYING;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. Draw Background / Court
            drawCourt(g2d);

            // 2. Draw Entities
            player1.draw(g2d);
            player2.draw(g2d);
            ball.draw(g2d);

            // 3. Draw Particles
            for (Particle p : particles) {
                p.draw(g2d);
            }

            // 4. Draw UI / HUD
            drawUI(g2d);

            // 5. Draw Overlays (Menu / Game Over)
            if (state == GameState.MENU) {
                drawOverlay(g2d, "VOLLEYBALL SIMULATION", "Press SPACE to Start", "Controls: Arrow Keys to Move/Jump");
            } else if (state == GameState.GAME_OVER) {
                drawOverlay(g2d, "GAME OVER", commentary, "Press SPACE to Play Again");
            } else if (state == GameState.POINT_OVER) {
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 40));
                g2d.drawString(commentary, WINDOW_WIDTH/2 - 150, WINDOW_HEIGHT/2);
            }
        }

        private void drawCourt(Graphics2D g2d) {
            // Floor
            g2d.setColor(new Color(60, 80, 120));
            g2d.fillRect(0, COURT_HEIGHT, COURT_WIDTH, WINDOW_HEIGHT - COURT_HEIGHT);

            // Court Lines
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));

            // Boundary
            g2d.drawRect(0, 0, COURT_WIDTH, COURT_HEIGHT);

            // Center Line
            g2d.drawLine(NET_X, 0, NET_X, COURT_HEIGHT);

            // Attack Line (3 meters from net approx)
            int attackLineOffset = 150;
            g2d.drawLine(NET_X - attackLineOffset, 0, NET_X - attackLineOffset, COURT_HEIGHT);
            g2d.drawLine(NET_X + attackLineOffset, 0, NET_X + attackLineOffset, COURT_HEIGHT);

            // Net
            g2d.setColor(new Color(200, 200, 200));
            g2d.setStroke(new BasicStroke(5));
            g2d.drawLine(NET_X, COURT_HEIGHT - NET_HEIGHT, NET_X, COURT_HEIGHT);

            // Net Mesh Detail
            g2d.setStroke(new BasicStroke(1));
            for (int i = 0; i < NET_HEIGHT; i += 10) {
                g2d.drawLine(NET_X - 10, COURT_HEIGHT - i, NET_X + 10, COURT_HEIGHT - i);
            }
        }

        private void drawUI(Graphics2D g2d) {
            // Scoreboard
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRoundRect(WINDOW_WIDTH/2 - 150, 20, 300, 60, 10, 10);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 30));
            g2d.drawString("P1: " + score1, WINDOW_WIDTH/2 - 130, 60);
            g2d.drawString("P2: " + score2, WINDOW_WIDTH/2 + 40, 60);

            // Commentary Bar
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRect(0, WINDOW_HEIGHT - 40, WINDOW_WIDTH, 40);
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Courier", Font.PLAIN, 16));
            g2d.drawString(commentary, 20, WINDOW_HEIGHT - 15);

            // Controls Hint
            if (state == GameState.PLAYING) {
                g2d.setColor(Color.GRAY);
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.drawString("Controls: LEFT/RIGHT to Move, UP to Jump", WINDOW_WIDTH - 250, WINDOW_HEIGHT - 15);
            }
        }

        private void drawOverlay(Graphics2D g2d, String title, String subtitle, String hint) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));

            FontMetrics fm = g2d.getFontMetrics();
            int titleW = fm.stringWidth(title);
            g2d.drawString(title, (WINDOW_WIDTH - titleW) / 2, WINDOW_HEIGHT / 2 - 40);

            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            fm = g2d.getFontMetrics();
            int subW = fm.stringWidth(subtitle);
            g2d.drawString(subtitle, (WINDOW_WIDTH - subW) / 2, WINDOW_HEIGHT / 2 + 10);

            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            fm = g2d.getFontMetrics();
            int hintW = fm.stringWidth(hint);
            g2d.drawString(hint, (WINDOW_WIDTH - hintW) / 2, WINDOW_HEIGHT / 2 + 50);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if (key == KeyEvent.VK_LEFT) keyLeft = true;
            if (key == KeyEvent.VK_RIGHT) keyRight = true;
            if (key == KeyEvent.VK_UP) keyJump = true;

            if (key == KeyEvent.VK_SPACE) {
                if (state == GameState.MENU || state == GameState.GAME_OVER) {
                    score1 = 0;
                    score2 = 0;
                    resetRound();
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int key = e.getKeyCode();
            if (key == KeyEvent.VK_LEFT) keyLeft = false;
            if (key == KeyEvent.VK_RIGHT) keyRight = false;
            if (key == KeyEvent.VK_UP) keyJump = false;
        }

        @Override
        public void keyTyped(KeyEvent e) {}
    }
}