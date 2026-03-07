import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class VolleyballGameSimulation extends JFrame {

    // ─── Constants ────────────────────────────────────────────────────────────
    static final int W = 1100, H = 700;
    static final int COURT_LEFT = 80, COURT_RIGHT = W - 80;
    static final int COURT_TOP = 150, COURT_BOTTOM = H - 120;
    static final int NET_X = W / 2;
    static final int NET_TOP = COURT_TOP + 30;
    static final int NET_BOTTOM = COURT_BOTTOM;
    static final int GROUND_Y = COURT_BOTTOM;
    static final int PLAYER_R = 22;
    static final int BALL_R = 14;
    static final double GRAVITY = 0.38;
    static final int MAX_SCORE = 25;
    static final int PLAYERS_PER_TEAM = 3;
    static final Color SKY_TOP    = new Color(10, 10, 40);
    static final Color SKY_BOT    = new Color(25, 25, 80);
    static final Color SAND_TOP   = new Color(210, 180, 120);
    static final Color SAND_BOT   = new Color(170, 140, 80);
    static final Color NET_COLOR  = new Color(255, 255, 255, 200);
    static final Color BALL_COL   = new Color(255, 220, 60);
    static final Color TEAM_A_COL = new Color(60, 180, 255);
    static final Color TEAM_B_COL = new Color(255, 80, 100);

    // ─── State ────────────────────────────────────────────────────────────────
    private final GamePanel panel;
    private final Timer     gameTimer;
    private boolean paused = true;
    private int     speedMult = 1;

    // ══════════════════════════════════════════════════════════════════════════
    // ENTRY POINT
    // ══════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(VolleyballGameSimulation::new);
    }

    public VolleyballGameSimulation() {
        super("Volleyball Game Simulation");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        panel = new GamePanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);

        gameTimer = new Timer(16, e -> {
            for (int i = 0; i < speedMult; i++) panel.tick();
            panel.repaint();
        });

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE  -> togglePause();
                    case KeyEvent.VK_R      -> resetGame();
                    case KeyEvent.VK_EQUALS, KeyEvent.VK_PLUS -> speedMult = Math.min(8, speedMult * 2);
                    case KeyEvent.VK_MINUS  -> speedMult = Math.max(1, speedMult / 2);
                }
            }
        });

        setVisible(true);
        setFocusable(true);
        requestFocus();
    }

    private void togglePause() {
        paused = !paused;
        if (paused) gameTimer.stop(); else gameTimer.start();
        panel.repaint();
    }

    private void resetGame() {
        gameTimer.stop();
        paused = true;
        panel.reset();
        panel.repaint();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GAME PANEL
    // ══════════════════════════════════════════════════════════════════════════
    class GamePanel extends JPanel {

        private final List<Player> teamA = new ArrayList<>();
        private final List<Player> teamB = new ArrayList<>();
        private Ball ball;
        private int scoreA = 0, scoreB = 0;
        private int hits = 0; // hit count for current possession
        private String lastEvent = "Press SPACE to start";
        private int lastEventTimer = 0;
        private boolean serving = true;
        private int serveTeam = 0; // 0=A, 1=B
        private final List<Particle> particles = new ArrayList<>();
        private int tickCount = 0;
        private boolean gameOver = false;
        private final Random rng = new Random(42);
        private float[] starX, starY, starBright;

        GamePanel() {
            setPreferredSize(new Dimension(W, H));
            setBackground(SKY_TOP);
            initStars();
            reset();
        }

        private void initStars() {
            int n = 120;
            starX = new float[n]; starY = new float[n]; starBright = new float[n];
            for (int i = 0; i < n; i++) {
                starX[i]     = rng.nextFloat() * W;
                starY[i]     = rng.nextFloat() * COURT_TOP;
                starBright[i]= 0.4f + rng.nextFloat() * 0.6f;
            }
        }

        void reset() {
            teamA.clear(); teamB.clear(); particles.clear();
            scoreA = 0; scoreB = 0; hits = 0; tickCount = 0; gameOver = false; serving = true; serveTeam = 0;
            lastEvent = "Press SPACE to start";

            // Team A (left side) - 3 players
            int[] aX = {NET_X - 200, NET_X - 340, NET_X - 140};
            int[] aZ = {GROUND_Y - PLAYER_R, GROUND_Y - PLAYER_R, GROUND_Y - PLAYER_R};
            String[] aNames = {"Alice", "Bob", "Carol"};
            for (int i = 0; i < PLAYERS_PER_TEAM; i++) {
                teamA.add(new Player(aX[i], aZ[i], 0, aNames[i], TEAM_A_COL));
            }

            // Team B (right side) - 3 players
            int[] bX = {NET_X + 200, NET_X + 340, NET_X + 140};
            String[] bNames = {"Dave", "Eve", "Frank"};
            for (int i = 0; i < PLAYERS_PER_TEAM; i++) {
                teamB.add(new Player(bX[i], aZ[i], 1, bNames[i], TEAM_B_COL));
            }

            ball = new Ball(NET_X - 150, GROUND_Y - 200);
        }

        // ─── TICK ─────────────────────────────────────────────────────────────
        void tick() {
            if (gameOver) return;
            tickCount++;
            lastEventTimer = Math.max(0, lastEventTimer - 1);

            // Update particles
            particles.removeIf(p -> !p.alive());
            particles.forEach(Particle::update);

            if (serving) {
                handleServe();
                return;
            }

            ball.update();
            updatePlayers();
            checkCollisions();
            checkBounds();
        }

        private void handleServe() {
            if (tickCount % 80 == 0) {
                serving = false;
                hits = 0;
                if (serveTeam == 0) {
                    ball.x = NET_X - 180;
                    ball.y = GROUND_Y - 120;
                    ball.vx = 5.5 + rng.nextDouble() * 2;
                    ball.vy = -10 - rng.nextDouble() * 3;
                } else {
                    ball.x = NET_X + 180;
                    ball.y = GROUND_Y - 120;
                    ball.vx = -5.5 - rng.nextDouble() * 2;
                    ball.vy = -10 - rng.nextDouble() * 3;
                }
                lastEvent = (serveTeam == 0 ? "Team A" : "Team B") + " serves!";
                lastEventTimer = 90;
            }
        }

        // ─── PLAYER AI ────────────────────────────────────────────────────────
        private void updatePlayers() {
            List<Player> myTeam, oppTeam;

            // Determine ball side
            boolean ballOnA = ball.x < NET_X;

            for (Player p : teamA) {
                updatePlayerAI(p, teamA, ballOnA);
            }
            for (Player p : teamB) {
                updatePlayerAI(p, teamB, !ballOnA);
            }
        }

        private void updatePlayerAI(Player p, List<Player> team, boolean hasBall) {
            double homeX = p.homeX;
            double targetX, targetY;

            if (hasBall) {
                // Move toward predicted ball position to hit it
                double predictedX = ball.x + ball.vx * 8;
                double predictedY = ball.y + ball.vy * 8 + 0.5 * GRAVITY * 64;

                // Only the closest player chases aggressively
                boolean isClosest = isClosestTo(p, team, ball.x, ball.y);
                if (isClosest) {
                    targetX = ball.x;
                    targetY = GROUND_Y - PLAYER_R;
                } else {
                    targetX = homeX + (ball.x - homeX) * 0.3;
                    targetY = GROUND_Y - PLAYER_R;
                }
            } else {
                // Defensive positioning
                targetX = homeX;
                targetY = GROUND_Y - PLAYER_R;
            }

            // Move toward target
            double dx = targetX - p.x;
            double dy = targetY - p.y;
            double speed = 3.5;
            if (Math.abs(dx) > speed) p.x += Math.signum(dx) * speed;
            else p.x = targetX;

            // Bounce/jump - if ball is nearby and low
            if (hasBall && isClosestTo(p, team, ball.x, ball.y)) {
                double dist = Math.hypot(ball.x - p.x, ball.y - p.y);
                if (dist < 70 && p.y >= GROUND_Y - PLAYER_R - 2 && !p.jumping) {
                    p.vy = -9;
                    p.jumping = true;
                }
            }

            // Gravity for jumping
            if (p.jumping) {
                p.vy += GRAVITY;
                p.y += p.vy;
                if (p.y >= GROUND_Y - PLAYER_R) {
                    p.y = GROUND_Y - PLAYER_R;
                    p.vy = 0;
                    p.jumping = false;
                }
            }

            // Court bounds
            int left  = (p.team == 0) ? COURT_LEFT  + PLAYER_R : NET_X + PLAYER_R + 5;
            int right = (p.team == 0) ? NET_X - PLAYER_R - 5   : COURT_RIGHT - PLAYER_R;
            p.x = Math.max(left, Math.min(right, p.x));

            p.armAngle = p.jumping ? -30 : 10;
        }

        private boolean isClosestTo(Player p, List<Player> team, double bx, double by) {
            double myDist = Math.hypot(bx - p.x, by - p.y);
            for (Player o : team) {
                if (o == p) continue;
                if (Math.hypot(bx - o.x, by - o.y) < myDist) return false;
            }
            return true;
        }

        // ─── COLLISIONS ───────────────────────────────────────────────────────
        private void checkCollisions() {
            // Ball vs net
            if (ball.x > NET_X - 8 && ball.x < NET_X + 8 && ball.y > NET_TOP) {
                ball.vx = -ball.vx * 0.7;
                ball.vy *= 0.6;
                ball.x = (ball.vx > 0) ? NET_X + 8 : NET_X - 8;
                spawnParticles((int)ball.x, (int)ball.y, NET_COLOR, 6);
            }

            // Ball vs players
            List<Player> allPlayers = new ArrayList<>();
            allPlayers.addAll(teamA); allPlayers.addAll(teamB);

            for (Player p : allPlayers) {
                double dx = ball.x - p.x;
                double dy = ball.y - p.y;
                double dist = Math.hypot(dx, dy);
                double minDist = BALL_R + PLAYER_R;

                if (dist < minDist && dist > 0.01) {
                    // Check hit limit (max 3 hits per team before must cross)
                    // Allow hit
                    double nx = dx / dist, ny = dy / dist;
                    double dot = ball.vx * nx + ball.vy * ny;
                    ball.vx -= 2 * dot * nx;
                    ball.vy -= 2 * dot * ny;

                    // Add intention: aim toward opponent's court with some force
                    boolean toRight = (p.team == 0);
                    double launchX = toRight ? 6.0 + rng.nextDouble() * 3 : -6.0 - rng.nextDouble() * 3;
                    double launchY = -9 - rng.nextDouble() * 4;
                    ball.vx = ball.vx * 0.3 + launchX * 0.7;
                    ball.vy = ball.vy * 0.3 + launchY * 0.7;

                    // Push ball out of player
                    ball.x = p.x + nx * (minDist + 1);
                    ball.y = p.y + ny * (minDist + 1);

                    hits++;
                    spawnParticles((int)ball.x, (int)ball.y, p.color, 12);
                    lastEvent = p.name + " hits!";
                    lastEventTimer = 50;
                    p.hitFlash = 10;
                }
            }
        }

        private void checkBounds() {
            // Ball hits ground
            if (ball.y + BALL_R >= GROUND_Y) {
                ball.y = GROUND_Y - BALL_R;

                // Score point
                if (ball.x < NET_X) {
                    // Ball landed on Team A side → Team B scores
                    scoreB++;
                    lastEvent = "Team B scores! 🎉";
                    serveTeam = 1;
                } else {
                    scoreA++;
                    lastEvent = "Team A scores! 🎉";
                    serveTeam = 0;
                }
                lastEventTimer = 120;
                spawnParticles((int)ball.x, GROUND_Y - 5, Color.YELLOW, 25);

                if (scoreA >= MAX_SCORE || scoreB >= MAX_SCORE) {
                    gameOver = true;
                    lastEvent = (scoreA >= MAX_SCORE ? "Team A" : "Team B") + " WINS! Press R to restart";
                    lastEventTimer = 9999;
                } else {
                    serving = true;
                    ball.vx = 0; ball.vy = 0;
                    hits = 0;
                }
                return;
            }

            // Ball out of court left/right
            if (ball.x - BALL_R < COURT_LEFT) {
                ball.vx = Math.abs(ball.vx) * 0.8;
                ball.x = COURT_LEFT + BALL_R;
            }
            if (ball.x + BALL_R > COURT_RIGHT) {
                ball.vx = -Math.abs(ball.vx) * 0.8;
                ball.x = COURT_RIGHT - BALL_R;
            }

            // Ball hits ceiling (top of court)
            if (ball.y - BALL_R < COURT_TOP) {
                ball.vy = Math.abs(ball.vy) * 0.8;
                ball.y = COURT_TOP + BALL_R;
            }
        }

        private void spawnParticles(int x, int y, Color c, int count) {
            for (int i = 0; i < count; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double speed = 1.5 + rng.nextDouble() * 4;
                particles.add(new Particle(x, y,
                        Math.cos(angle) * speed,
                        Math.sin(angle) * speed - 2,
                        c, 20 + rng.nextInt(30)));
            }
        }

        // ─── PAINT ────────────────────────────────────────────────────────────
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            drawBackground(g2);
            drawStars(g2);
            drawCourt(g2);
            drawNet(g2);
            drawParticles(g2);
            teamA.forEach(p -> drawPlayer(g2, p));
            teamB.forEach(p -> drawPlayer(g2, p));
            drawBall(g2);
            drawHUD(g2);
            drawEventText(g2);
            drawControls(g2);

            if (paused && !gameOver) drawPauseOverlay(g2);
            if (gameOver) drawGameOver(g2);
        }

        private void drawBackground(Graphics2D g2) {
            GradientPaint sky = new GradientPaint(0, 0, SKY_TOP, 0, H, SKY_BOT);
            g2.setPaint(sky);
            g2.fillRect(0, 0, W, H);

            // Moon / spotlight glow
            RadialGradientPaint glow = new RadialGradientPaint(
                    new Point2D.Float(W / 2f, COURT_TOP + 10),
                    350,
                    new float[]{0f, 1f},
                    new Color[]{new Color(100, 100, 255, 30), new Color(0, 0, 0, 0)}
            );
            g2.setPaint(glow);
            g2.fillRect(0, 0, W, H);
        }

        private void drawStars(Graphics2D g2) {
            for (int i = 0; i < starX.length; i++) {
                float flicker = starBright[i] * (0.7f + 0.3f * (float)Math.sin(tickCount * 0.05 + i));
                g2.setColor(new Color(1f, 1f, 1f, flicker));
                float sz = 1.5f + starBright[i];
                g2.fill(new Ellipse2D.Float(starX[i] - sz/2, starY[i] - sz/2, sz, sz));
            }
        }

        private void drawCourt(Graphics2D g2) {
            // Sand gradient
            GradientPaint sand = new GradientPaint(0, COURT_BOTTOM - 60, SAND_TOP, 0, H, SAND_BOT);
            g2.setPaint(sand);
            g2.fillRect(COURT_LEFT, COURT_BOTTOM, COURT_RIGHT - COURT_LEFT, H - COURT_BOTTOM);

            // Court lines
            g2.setColor(new Color(255, 255, 255, 120));
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(COURT_LEFT, COURT_TOP, COURT_RIGHT - COURT_LEFT, COURT_BOTTOM - COURT_TOP);
            g2.drawLine(NET_X, COURT_TOP, NET_X, COURT_BOTTOM); // center line

            // Attack lines
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{8, 6}, 0));
            g2.drawLine(NET_X - 180, COURT_TOP, NET_X - 180, COURT_BOTTOM);
            g2.drawLine(NET_X + 180, COURT_TOP, NET_X + 180, COURT_BOTTOM);
            g2.setStroke(new BasicStroke(1));
        }

        private void drawNet(Graphics2D g2) {
            int posts = 14;
            int netHeight = NET_BOTTOM - NET_TOP;
            int netWidth = 18;

            // Net shadow
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillRect(NET_X - 3, NET_TOP + 4, 6, netHeight);

            // Net posts (poles)
            g2.setColor(new Color(220, 220, 220));
            g2.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(NET_X, NET_TOP - 20, NET_X, NET_BOTTOM + 10);

            // Net grid
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(NET_COLOR);
            int cellH = netHeight / posts;
            int cells = (COURT_RIGHT - COURT_LEFT) / 20;

            for (int row = 0; row <= posts; row++) {
                int y = NET_TOP + row * cellH;
                g2.drawLine(NET_X - netWidth, y, NET_X + netWidth, y);
            }
            for (int col = -netWidth; col <= netWidth; col += 4) {
                g2.drawLine(NET_X + col, NET_TOP, NET_X + col, NET_BOTTOM);
            }

            // Top tape
            g2.setColor(new Color(255, 80, 80));
            g2.setStroke(new BasicStroke(4));
            g2.drawLine(NET_X - netWidth, NET_TOP, NET_X + netWidth, NET_TOP);
        }

        private void drawPlayer(Graphics2D g2, Player p) {
            int px = (int) p.x, py = (int) p.y;
            Color c = p.color;

            // Shadow
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillOval(px - PLAYER_R + 2, GROUND_Y - 6, PLAYER_R * 2, 10);

            // Hit flash
            if (p.hitFlash > 0) {
                p.hitFlash--;
                c = Color.WHITE;
            }

            // Jersey (body)
            g2.setColor(c);
            g2.fillOval(px - PLAYER_R, py - PLAYER_R, PLAYER_R * 2, PLAYER_R * 2 + 10);

            // Jersey number / letter
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            String label = p.name.substring(0, 1);
            g2.drawString(label, px - fm.stringWidth(label) / 2, py + 5);

            // Head
            g2.setColor(new Color(255, 220, 180));
            g2.fillOval(px - 11, py - PLAYER_R - 20, 22, 22);

            // Hair
            g2.setColor(p.team == 0 ? new Color(80, 50, 20) : new Color(200, 160, 50));
            g2.fillArc(px - 11, py - PLAYER_R - 22, 22, 14, 0, 180);

            // Arms - simple lines
            g2.setColor(c);
            g2.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double armRad = Math.toRadians(p.armAngle + (p.jumping ? -45 : 0));
            int armLen = 18;
            g2.drawLine(px - 12, py - 5,
                    (int)(px - 12 - Math.cos(armRad) * armLen),
                    (int)(py - 5 + Math.sin(armRad) * armLen));
            g2.drawLine(px + 12, py - 5,
                    (int)(px + 12 + Math.cos(armRad) * armLen),
                    (int)(py - 5 + Math.sin(armRad) * armLen));

            // Legs
            g2.setColor(new Color(50, 50, 150));
            int legY = py + 8;
            g2.drawLine(px, legY, px - 10, legY + 20);
            g2.drawLine(px, legY, px + 10, legY + 20);
            g2.setStroke(new BasicStroke(1));

            // Name tag
            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            g2.setColor(new Color(255, 255, 255, 200));
            fm = g2.getFontMetrics();
            g2.drawString(p.name, px - fm.stringWidth(p.name) / 2, py - PLAYER_R - 26);
        }

        private void drawBall(Graphics2D g2) {
            int bx = (int) ball.x, by = (int) ball.y;

            // Ball glow
            RadialGradientPaint glow = new RadialGradientPaint(
                    new Point2D.Float(bx - 3, by - 3), BALL_R * 2.5f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 255, 100, 120), new Color(0, 0, 0, 0)}
            );
            g2.setPaint(glow);
            g2.fillOval(bx - BALL_R * 3, by - BALL_R * 3, BALL_R * 6, BALL_R * 6);

            // Ball shadow on ground
            double shadowScale = Math.max(0.2, Math.min(1.0, 1.0 - (GROUND_Y - by) / 400.0));
            int sw = (int)(BALL_R * 2.5 * shadowScale), sh = (int)(8 * shadowScale);
            g2.setColor(new Color(0, 0, 0, (int)(80 * shadowScale)));
            g2.fillOval(bx - sw / 2, GROUND_Y - 4, sw, sh);

            // Ball body
            GradientPaint ballGrad = new GradientPaint(
                    bx - BALL_R, by - BALL_R, new Color(255, 240, 100),
                    bx + BALL_R, by + BALL_R, new Color(200, 140, 20)
            );
            g2.setPaint(ballGrad);
            g2.fillOval(bx - BALL_R, by - BALL_R, BALL_R * 2, BALL_R * 2);

            // Ball seams (rotate based on velocity)
            ball.rotation += (ball.vx * 2.5);
            g2.setColor(new Color(0, 0, 0, 100));
            g2.setStroke(new BasicStroke(1.5f));
            double r = BALL_R - 2;
            for (int i = 0; i < 3; i++) {
                double a = Math.toRadians(ball.rotation + i * 60);
                g2.drawArc((int)(bx - r), (int)(by - r), (int)(r*2), (int)(r*2),
                        (int)(Math.toDegrees(a)), 100);
            }

            // Specular highlight
            g2.setPaint(new Color(255, 255, 255, 160));
            g2.fillOval(bx - BALL_R / 2, by - BALL_R / 2, BALL_R / 2, BALL_R / 2);
            g2.setStroke(new BasicStroke(1));
        }

        private void drawParticles(Graphics2D g2) {
            for (Particle p : particles) {
                float alpha = (float) p.life / p.maxLife;
                g2.setColor(new Color(
                        p.color.getRed() / 255f,
                        p.color.getGreen() / 255f,
                        p.color.getBlue() / 255f,
                        alpha * 0.9f
                ));
                int size = (int)(p.size * alpha);
                g2.fillOval((int)p.x - size / 2, (int)p.y - size / 2, size, size);
            }
        }

        private void drawHUD(Graphics2D g2) {
            // Score board background
            int sbW = 360, sbH = 90, sbX = W / 2 - sbW / 2, sbY = 18;
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRoundRect(sbX, sbY, sbW, sbH, 20, 20);
            g2.setColor(new Color(255, 255, 255, 40));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(sbX, sbY, sbW, sbH, 20, 20);

            // Team labels
            g2.setFont(new Font("Consolas", Font.BOLD, 14));
            g2.setColor(TEAM_A_COL);
            g2.drawString("TEAM A", sbX + 30, sbY + 22);
            g2.setColor(TEAM_B_COL);
            g2.drawString("TEAM B", sbX + sbW - 30 - g2.getFontMetrics().stringWidth("TEAM B"), sbY + 22);

            // Score numbers
            g2.setFont(new Font("Arial", Font.BOLD, 46));
            g2.setColor(TEAM_A_COL);
            g2.drawString(String.valueOf(scoreA), sbX + 55, sbY + 73);
            g2.setColor(new Color(200, 200, 200));
            g2.drawString(":", sbX + sbW / 2 - 8, sbY + 73);
            g2.setColor(TEAM_B_COL);
            String bStr = String.valueOf(scoreB);
            g2.drawString(bStr, sbX + sbW - 55 - g2.getFontMetrics().stringWidth(bStr), sbY + 73);

            // Score bar
            int barW = sbW - 40;
            int barH = 6;
            int barX = sbX + 20;
            int barY = sbY + sbH - 14;
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillRoundRect(barX, barY, barW, barH, 6, 6);
            int total = scoreA + scoreB;
            if (total > 0) {
                int aW = (int)((double)scoreA / total * barW);
                g2.setColor(TEAM_A_COL);
                g2.fillRoundRect(barX, barY, aW, barH, 6, 6);
                g2.setColor(TEAM_B_COL);
                g2.fillRoundRect(barX + aW, barY, barW - aW, barH, 6, 6);
            }

            // Speed indicator
            g2.setFont(new Font("Consolas", Font.PLAIN, 11));
            g2.setColor(new Color(200, 200, 200, 160));
            g2.drawString("Speed: " + speedMult + "x", 10, H - 10);

            // Serve indicator
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(serving ?
                    (serveTeam == 0 ? TEAM_A_COL : TEAM_B_COL) : new Color(200, 200, 200, 100));
            g2.drawString("SERVE: " + (serveTeam == 0 ? "Team A ▶" : "◀ Team B"),
                    W / 2 - 50, COURT_TOP - 8);
        }

        private void drawEventText(Graphics2D g2) {
            if (lastEventTimer > 0 && !lastEvent.isEmpty()) {
                float alpha = Math.min(1f, lastEventTimer / 40f);
                g2.setFont(new Font("Arial", Font.BOLD, 22));
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(lastEvent);
                float ty = COURT_TOP + 60 - (1f - alpha) * 20;

                // Glow
                g2.setColor(new Color(0, 0, 0, (int)(160 * alpha)));
                g2.fillRoundRect(W / 2 - tw / 2 - 12, (int)ty - 22, tw + 24, 30, 10, 10);
                g2.setColor(new Color(255, 240, 80, (int)(255 * alpha)));
                g2.drawString(lastEvent, W / 2 - tw / 2, (int)ty);
            }
        }

        private void drawControls(Graphics2D g2) {
            g2.setFont(new Font("Consolas", Font.PLAIN, 11));
            g2.setColor(new Color(200, 200, 200, 130));
            g2.drawString("[SPACE] Pause  [R] Reset  [+/-] Speed", 10, 22);
        }

        private void drawPauseOverlay(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRect(0, 0, W, H);
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            g2.setColor(new Color(255, 255, 255, 220));
            String msg = "⏸ PAUSED";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, W / 2 - fm.stringWidth(msg) / 2, H / 2 - 20);
            g2.setFont(new Font("Consolas", Font.PLAIN, 18));
            g2.setColor(new Color(200, 200, 200, 180));
            String sub = "Press SPACE to continue";
            fm = g2.getFontMetrics();
            g2.drawString(sub, W / 2 - fm.stringWidth(sub) / 2, H / 2 + 25);
        }

        private void drawGameOver(Graphics2D g2) {
            // Overlay
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, W, H);

            // Trophy decoration
            String winner = scoreA >= MAX_SCORE ? "TEAM A" : "TEAM B";
            Color wColor   = scoreA >= MAX_SCORE ? TEAM_A_COL : TEAM_B_COL;

            // Pulsing glow
            float pulse = (float)(0.7 + 0.3 * Math.sin(tickCount * 0.06));
            RadialGradientPaint glow = new RadialGradientPaint(
                    new Point2D.Float(W / 2f, H / 2f - 20), 220,
                    new float[]{0f, 1f},
                    new Color[]{
                            new Color(wColor.getRed(), wColor.getGreen(), wColor.getBlue(), (int)(80 * pulse)),
                            new Color(0, 0, 0, 0)
                    }
            );
            g2.setPaint(glow);
            g2.fillRect(0, 0, W, H);

            // Main text
            g2.setFont(new Font("Arial", Font.BOLD, 62));
            FontMetrics fm = g2.getFontMetrics();
            String title = "🏆 " + winner + " WINS!";
            g2.setColor(new Color(0, 0, 0, 150));
            g2.drawString(title, W / 2 - fm.stringWidth(title) / 2 + 3, H / 2 - 10 + 3);
            g2.setColor(wColor);
            g2.drawString(title, W / 2 - fm.stringWidth(title) / 2, H / 2 - 10);

            // Final score
            g2.setFont(new Font("Consolas", Font.BOLD, 28));
            fm = g2.getFontMetrics();
            String score = "Final Score:  " + scoreA + "  –  " + scoreB;
            g2.setColor(new Color(255, 255, 255, 200));
            g2.drawString(score, W / 2 - fm.stringWidth(score) / 2, H / 2 + 45);

            // Restart
            g2.setFont(new Font("Consolas", Font.PLAIN, 16));
            fm = g2.getFontMetrics();
            String restart = "Press R to play again";
            g2.setColor(new Color(180, 180, 180, 180));
            g2.drawString(restart, W / 2 - fm.stringWidth(restart) / 2, H / 2 + 85);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ENTITY CLASSES
    // ══════════════════════════════════════════════════════════════════════════

    // ─── Ball ─────────────────────────────────────────────────────────────────
    static class Ball {
        double x, y, vx = 0, vy = 0;
        double rotation = 0;

        Ball(double x, double y) { this.x = x; this.y = y; }

        void update() {
            vy += GRAVITY;
            x  += vx;
            y  += vy;
            vx *= 0.994; // air resistance
        }
    }

    // ─── Player ───────────────────────────────────────────────────────────────
    static class Player {
        double x, y, vy = 0;
        double homeX;
        int    team;
        String name;
        Color  color;
        boolean jumping = false;
        double  armAngle = 10;
        int     hitFlash = 0;

        Player(int x, int y, int team, String name, Color color) {
            this.x = this.homeX = x;
            this.y = y;
            this.team = team;
            this.name = name;
            this.color = color;
        }
    }

    // ─── Particle ─────────────────────────────────────────────────────────────
    static class Particle {
        double x, y, vx, vy;
        Color  color;
        int    life, maxLife;
        int    size = 7;

        Particle(int x, int y, double vx, double vy, Color color, int life) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.color = color;
            this.life = this.maxLife = life;
        }

        void update() {
            x  += vx;
            y  += vy;
            vy += GRAVITY * 0.5;
            vx *= 0.96;
            life--;
        }

        boolean alive() { return life > 0; }
    }
}