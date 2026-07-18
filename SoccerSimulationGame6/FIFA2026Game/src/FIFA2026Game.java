import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class FIFA2026Game extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FIFA2026Game game = new FIFA2026Game();
            game.setVisible(true);
        });
    }

    public FIFA2026Game() {
        super("FIFA 2026 Game - Java Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel panel = new GamePanel(1280, 768);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        panel.start();
    }

    // GamePanel does everything: update loop, render, input, and game state
    static class GamePanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener, Runnable {
        // Constants
        private final int WIDTH;
        private final int HEIGHT;
        private volatile boolean running = false;
        private Thread loopThread;

        private final double TARGET_FPS = 60.0;
        private final double TARGET_DT = 1.0 / TARGET_FPS;

        // Back buffer
        private BufferedImage backBuffer;
        private Graphics2D backG;

        // World elements
        private Pitch pitch;
        private Ball ball;
        private Team teamA;
        private Team teamB;
        private Goal goalLeft;
        private Goal goalRight;

        // UI and menu
        private GameState state = GameState.MENU;
        private Menu menu;
        private HUD hud;

        // Timers and match control
        private double matchTimeSeconds = 0;
        private double halfDurationSeconds = 180; // 3 minutes per half for demo
        private int currentHalf = 1;
        private boolean paused = false;
        private boolean celebration = false;
        private double celebrationTimer = 0;

        // Effects
        private ParticleSystem particleSystem;
        private NetAnimator netAnimatorLeft;
        private NetAnimator netAnimatorRight;
        private BallTrail ballTrail;

        // Input
        private final boolean[] keys = new boolean[512];

        // Random
        private Random rng = new Random();

        // Mode
        private Mode selectedMode = Mode.PLAYER_VS_CPU;

        // Fonts
        private Font fontBig;
        private Font fontMed;
        private Font fontSmall;

        // Score popup
        private ScoreFlash scoreFlash;

        public GamePanel(int width, int height) {
            this.WIDTH = width;
            this.HEIGHT = height;
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            setDoubleBuffered(false); // We'll use custom back buffer
            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);

            backBuffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            backG = backBuffer.createGraphics();
            enableQuality(backG);

            // Load fonts
            fontBig = new Font("SansSerif", Font.BOLD, 48);
            fontMed = new Font("SansSerif", Font.BOLD, 24);
            fontSmall = new Font("SansSerif", Font.PLAIN, 16);

            // Init menu
            menu = new Menu();

            // Init HUD and effects
            hud = new HUD();
            particleSystem = new ParticleSystem();
            ballTrail = new BallTrail(30);
            netAnimatorLeft = new NetAnimator();
            netAnimatorRight = new NetAnimator();
            scoreFlash = new ScoreFlash();

            // Initialize a default demo world (will be reset when mode is chosen)
            initWorld(Mode.PLAYER_VS_CPU);
        }

        private void initWorld(Mode mode) {
            // Pitch dimensions inside panel
            Rectangle2D fieldBounds = new Rectangle2D.Double(60, 60, WIDTH - 120, HEIGHT - 120);
            pitch = new Pitch(fieldBounds);

            // Goals at left and right
            double goalWidth = 16;
            double goalMouth = fieldBounds.getHeight() * 0.29;
            goalLeft = new Goal(fieldBounds.getX() - goalWidth, fieldBounds.getCenterY() - goalMouth / 2, goalWidth, goalMouth, Side.LEFT);
            goalRight = new Goal(fieldBounds.getMaxX(), fieldBounds.getCenterY() - goalMouth / 2, goalWidth, goalMouth, Side.RIGHT);

            // Ball at center
            ball = new Ball(new Vector2((float) fieldBounds.getCenterX(), (float) fieldBounds.getCenterY()));

            // Teams
            teamA = new Team("Red Falcons", new Color(200, 40, 40), new Color(240, 220, 220), Side.LEFT, false);
            teamB = new Team("Blue Titans", new Color(30, 90, 200), new Color(220, 230, 250), Side.RIGHT, false);

            // Mode: decide whether teamB is CPU
            if (mode == Mode.PLAYER_VS_CPU) {
                teamA.humanControlled = true;
                teamB.humanControlled = false;
                teamB.ai = new TeamAI(teamB);
                teamA.ai = null;
            } else if (mode == Mode.PLAYER_VS_PLAYER) {
                teamA.humanControlled = true;
                teamB.humanControlled = true;
                teamA.ai = null;
                teamB.ai = null;
            }

            // Spawn players
            spawnTeams(pitch, teamA, teamB);

            // Reset time and score if needed
            matchTimeSeconds = 0;
            currentHalf = 1;
            teamA.score = 0;
            teamB.score = 0;
            paused = false;
            celebration = false;
            celebrationTimer = 0;
            ballTrail.clear();
            particleSystem.clear();
            netAnimatorLeft.reset();
            netAnimatorRight.reset();
            scoreFlash.reset();
        }

        private void spawnTeams(Pitch pitch, Team leftTeam, Team rightTeam) {
            leftTeam.players.clear();
            rightTeam.players.clear();

            // Formation 3-3-1 (7-a-side demo)
            // Left team positions
            Rectangle2D f = pitch.playArea;
            double cx = f.getCenterX();
            double cy = f.getCenterY();
            double w = f.getWidth();
            double h = f.getHeight();

            // Left defenders
            leftTeam.addPlayer(new Player(2, leftTeam, new Vector2((float) (f.getX() + w * 0.2), (float) (cy - h * 0.25))));
            leftTeam.addPlayer(new Player(3, leftTeam, new Vector2((float) (f.getX() + w * 0.2), (float) (cy))));
            leftTeam.addPlayer(new Player(4, leftTeam, new Vector2((float) (f.getX() + w * 0.2), (float) (cy + h * 0.25))));

            // Left midfielders
            leftTeam.addPlayer(new Player(6, leftTeam, new Vector2((float) (f.getX() + w * 0.4), (float) (cy - h * 0.25))));
            leftTeam.addPlayer(new Player(7, leftTeam, new Vector2((float) (f.getX() + w * 0.4), (float) (cy))));
            leftTeam.addPlayer(new Player(8, leftTeam, new Vector2((float) (f.getX() + w * 0.4), (float) (cy + h * 0.25))));

            // Left striker
            leftTeam.addPlayer(new Player(9, leftTeam, new Vector2((float) (f.getX() + w * 0.62), (float) (cy))));

            leftTeam.goalSide = Side.LEFT;

            // Right team mirroring
            rightTeam.addPlayer(new Player(2, rightTeam, new Vector2((float) (f.getMaxX() - w * 0.2), (float) (cy - h * 0.25))));
            rightTeam.addPlayer(new Player(3, rightTeam, new Vector2((float) (f.getMaxX() - w * 0.2), (float) (cy))));
            rightTeam.addPlayer(new Player(4, rightTeam, new Vector2((float) (f.getMaxX() - w * 0.2), (float) (cy + h * 0.25))));

            rightTeam.addPlayer(new Player(6, rightTeam, new Vector2((float) (f.getMaxX() - w * 0.4), (float) (cy - h * 0.25))));
            rightTeam.addPlayer(new Player(7, rightTeam, new Vector2((float) (f.getMaxX() - w * 0.4), (float) (cy))));
            rightTeam.addPlayer(new Player(8, rightTeam, new Vector2((float) (f.getMaxX() - w * 0.4), (float) (cy + h * 0.25))));

            rightTeam.addPlayer(new Player(9, rightTeam, new Vector2((float) (f.getMaxX() - w * 0.62), (float) (cy))));

            rightTeam.goalSide = Side.RIGHT;

            // Set controllers: select nearest to ball as active initially
            leftTeam.selectClosestTo(ball.position);
            rightTeam.selectClosestTo(ball.position);

            // Baseline tactics anchor positions
            leftTeam.setHomePositionsFromCurrent();
            rightTeam.setHomePositionsFromCurrent();
        }

        public void start() {
            if (running) return;
            running = true;
            loopThread = new Thread(this, "GameLoop");
            loopThread.start();
        }

        public void stop() {
            running = false;
            try {
                if (loopThread != null) loopThread.join();
            } catch (InterruptedException ignored) {}
        }

        @Override
        public void run() {
            long last = System.nanoTime();
            double accumulator = 0;

            while (running) {
                long now = System.nanoTime();
                double dt = (now - last) / 1_000_000_000.0;
                last = now;
                accumulator += dt;

                // Clamp to avoid spiral of death
                if (accumulator > 0.25) accumulator = 0.25;

                while (accumulator >= TARGET_DT) {
                    update(TARGET_DT);
                    accumulator -= TARGET_DT;
                }

                render();

                // Try to sleep a bit
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {}
            }
        }

        private void update(double dt) {
            if (state == GameState.MENU) {
                menu.update(dt);
                return;
            }

            if (paused) return;

            // Time controls
            if (!celebration) {
                matchTimeSeconds += dt;
            }

            if (matchTimeSeconds >= halfDurationSeconds) {
                if (currentHalf == 1) {
                    currentHalf = 2;
                    matchTimeSeconds = 0;
                    // Switch sides for realism
                    swapSides();
                    kickoff(teamA.goalSide == Side.LEFT ? teamA : teamB);
                } else {
                    // Full time
                    state = GameState.GAMEOVER;
                    return;
                }
            }

            // Keyboard input
            handlePlayerInput(dt);

            // AI decisions
            if (!teamA.humanControlled && teamA.ai != null) teamA.ai.updateAI(this, dt);
            if (!teamB.humanControlled && teamB.ai != null) teamB.ai.updateAI(this, dt);

            // Update players
            for (Player p : teamA.players) p.update(this, dt);
            for (Player p : teamB.players) p.update(this, dt);

            // Ball physics
            ball.update(this, dt);

            // Ball-player interactions
            handleBallInteractions(dt);

            // Boundaries (ball)
            keepBallInBounds();

            // Goals
            checkGoals(dt);

            // Particles and effects
            particleSystem.update(dt);
            ballTrail.update(ball.position);
            netAnimatorLeft.update(dt);
            netAnimatorRight.update(dt);
            scoreFlash.update(dt);
        }

        private void swapSides() {
            // Swap team sides and mirroring positions to the other half
            Team tmp = teamA;
            teamA = teamB;
            teamB = tmp;

            teamA.goalSide = Side.LEFT;
            teamB.goalSide = Side.RIGHT;

            // Mirror positions across vertical center line
            Rectangle2D f = pitch.playArea;
            double cx = f.getCenterX();

            for (Team t : Arrays.asList(teamA, teamB)) {
                for (Player p : t.players) {
                    double dx = p.position.x - cx;
                    p.position.x = (float) (cx - dx);
                    p.velocity = p.velocity.mul(-1);
                }
                t.setHomePositionsFromCurrent();
                t.activePlayerIndex = 0;
                t.selectClosestTo(ball.position);
            }
        }

        private void kickoff(Team kickingTeam) {
            Rectangle2D f = pitch.playArea;
            ball.position.set((float) f.getCenterX(), (float) f.getCenterY());
            ball.velocity.set(0, 0);

            // Reset all players to home positions
            for (Team t : Arrays.asList(teamA, teamB)) {
                t.resetToHome();
                t.energyRegen = 0.5;
                for (Player p : t.players) {
                    p.hasBall = false;
                }
                t.selectClosestTo(ball.position);
            }

            // Give ball to striker of kicking team at center with gentle tap
            Player striker = kickingTeam.getClosestTo(ball.position);
            if (striker != null) {
                striker.position.x = (float) f.getCenterX() - (kickingTeam.goalSide == Side.LEFT ? 30 : -30);
                striker.position.y = (float) f.getCenterY();
                ball.position.set(striker.position.x + (kickingTeam.goalSide == Side.LEFT ? 14 : -14), striker.position.y);
                striker.hasBall = true;
                striker.dribbleAngle = kickingTeam.goalSide == Side.LEFT ? 0 : Math.PI;
            }
        }

        private void handlePlayerInput(double dt) {
            // Player 1 - WASD, Shift sprint, Space shoot, F pass, R switch, Esc pause
            if (keys[KeyEvent.VK_ESCAPE]) {
                keys[KeyEvent.VK_ESCAPE] = false;
                paused = !paused;
            }

            // Team A control
            if (teamA.humanControlled) {
                Player a = teamA.getActivePlayer();
                if (a != null) {
                    Vector2 dir = new Vector2(0, 0);
                    if (keys[KeyEvent.VK_W]) dir.y -= 1;
                    if (keys[KeyEvent.VK_S]) dir.y += 1;
                    if (keys[KeyEvent.VK_A]) dir.x -= 1;
                    if (keys[KeyEvent.VK_D]) dir.x += 1;
                    a.inputMove = dir.normalized();

                    a.sprint = keys[KeyEvent.VK_SHIFT];

                    if (keys[KeyEvent.VK_SPACE]) {
                        // Shoot
                        if (a.canShoot()) {
                            Vector2 target = aimTowardGoal(a, teamB);
                            shoot(a, target, 780, true);
                        }
                        keys[KeyEvent.VK_SPACE] = false;
                    }

                    if (keys[KeyEvent.VK_F]) {
                        // Pass
                        if (a.canPass()) {
                            Player mate = teamA.findBestPassTarget(a, ball, pitch.playArea);
                            if (mate != null) {
                                Vector2 target = mate.position.copy();
                                pass(a, target);
                            } else {
                                // Forward pass if no mate
                                Vector2 forward = new Vector2(teamA.goalSide == Side.LEFT ? 1 : -1, 0);
                                pass(a, a.position.add(forward.mul(200)));
                            }
                        }
                        keys[KeyEvent.VK_F] = false;
                    }

                    if (keys[KeyEvent.VK_R]) {
                        teamA.switchToClosest(ball.position);
                        keys[KeyEvent.VK_R] = false;
                    }
                }
            }

            // Team B controls - Arrows, RShift sprint, L shoot, K pass, P switch
            if (teamB.humanControlled) {
                Player b = teamB.getActivePlayer();
                if (b != null) {
                    Vector2 dir = new Vector2(0, 0);
                    if (keys[KeyEvent.VK_UP]) dir.y -= 1;
                    if (keys[KeyEvent.VK_DOWN]) dir.y += 1;
                    if (keys[KeyEvent.VK_LEFT]) dir.x -= 1;
                    if (keys[KeyEvent.VK_RIGHT]) dir.x += 1;
                    b.inputMove = dir.normalized();

                    b.sprint = keys[KeyEvent.VK_SHIFT] || keys[KeyEvent.VK_CONTROL] || keys[KeyEvent.VK_RSHIFT]; // include RShift

                    if (keys[KeyEvent.VK_L]) {
                        if (b.canShoot()) {
                            Vector2 target = aimTowardGoal(b, teamA);
                            shoot(b, target, 780, true);
                        }
                        keys[KeyEvent.VK_L] = false;
                    }

                    if (keys[KeyEvent.VK_K]) {
                        if (b.canPass()) {
                            Player mate = teamB.findBestPassTarget(b, ball, pitch.playArea);
                            if (mate != null) {
                                Vector2 target = mate.position.copy();
                                pass(b, target);
                            } else {
                                Vector2 forward = new Vector2(teamB.goalSide == Side.RIGHT ? 1 : -1, 0);
                                pass(b, b.position.add(forward.mul(200)));
                            }
                        }
                        keys[KeyEvent.VK_K] = false;
                    }

                    if (keys[KeyEvent.VK_P]) {
                        teamB.switchToClosest(ball.position);
                        keys[KeyEvent.VK_P] = false;
                    }
                }
            }
        }

        private Vector2 aimTowardGoal(Player shooter, Team opponent) {
            Rectangle2D f = pitch.playArea;
            Rectangle2D goalRect = opponent.goalSide == Side.LEFT ? goalLeft.mouth : goalRight.mouth;
            // Aim slightly randomized within the goal mouth
            double gy = goalRect.getY() + rng.nextDouble() * goalRect.getHeight();
            double gx = goalRect.getCenterX();
            return new Vector2((float) gx, (float) gy);
        }

        private void shoot(Player p, Vector2 target, double power, boolean withParticles) {
            Vector2 dir = target.sub(p.position).normalized();
            // If dribbling or close, take more control
            float base = (float) power;
            ball.velocity = dir.mul(base);
            ball.position = p.position.add(dir.mul(p.radius + ball.radius + 2));
            p.hasBall = false;
            p.kickCooldown = 0.3;
            p.passCooldown = 0.2;
            p.stamina = Math.max(0, p.stamina - 6);

            if (withParticles) {
                particleSystem.spawnShotBurst(ball.position, dir);
                ballTrail.kick();
            }

            // Net animation pre-arm
            if (target.x < pitch.playArea.getCenterX()) netAnimatorLeft.arm(dir);
            else netAnimatorRight.arm(dir);
        }

        private void pass(Player p, Vector2 target) {
            Vector2 dir = target.sub(p.position).normalized();
            float base = 480;
            // Scale based on distance
            float dist = p.position.distance(target);
            base = Math.max(280, Math.min(580, dist * 2.4f));
            ball.velocity = dir.mul(base);
            ball.position = p.position.add(dir.mul(p.radius + ball.radius + 1));
            p.hasBall = false;
            p.passCooldown = 0.35;
            p.kickCooldown = 0.15;
            p.stamina = Math.max(0, p.stamina - 2.5f);
            particleSystem.spawnPassPuffs(p.position, dir);
            ballTrail.kick();
        }

        private void handleBallInteractions(double dt) {
            // Player control attempt or collision impulse
            // Prioritize active players for both teams
            List<Player> allPlayers = new ArrayList<>();
            allPlayers.addAll(teamA.players);
            allPlayers.addAll(teamB.players);

            // Apply dribbling for those who have ball
            for (Player p : allPlayers) {
                if (p.hasBall) {
                    // Attach ball slightly in front of player's facing direction
                    Vector2 dir = p.getFacingDir();
                    ball.position = p.position.add(dir.mul(p.radius + ball.radius + 2));
                    ball.velocity = ball.velocity.mul(0.5f);
                }
            }

            // If no one has ball, allow pickups and collisions
            if (!anyPlayerHasBall()) {
                // Determine closest player to ball
                Player closest = null;
                float minDist = Float.MAX_VALUE;
                for (Player p : allPlayers) {
                    float d = p.position.distance(ball.position);
                    if (d < minDist) {
                        minDist = d;
                        closest = p;
                    }
                }

                for (Player p : allPlayers) {
                    float d = p.position.distance(ball.position);
                    float contactDist = p.radius + ball.radius;
                    if (d < contactDist) {
                        Vector2 n = ball.position.sub(p.position);
                        float len = n.len();
                        if (len > 0.0001f) n = n.mul(1f / len);
                        else n = new Vector2(1, 0);
                        // Simple impulse
                        float rel = Math.max(180, p.velocity.len() * 40);
                        ball.velocity = ball.velocity.add(n.mul(rel));
                        // Separate
                        float overlap = contactDist - d;
                        ball.position = ball.position.add(n.mul(overlap + 0.1f));
                        // Dribble capture chance if active or closest
                        boolean prefer = p == p.team.getActivePlayer() || p == closest;
                        boolean slowBall = ball.velocity.len() < 260;
                        if (prefer && slowBall && p.kickCooldown <= 0.02) {
                            p.hasBall = true;
                            // slightly reduce ball vel when captured
                            ball.velocity = ball.velocity.mul(0.25f);
                            p.dribbleAngle = Math.atan2(p.inputMove.y == 0 && p.inputMove.x == 0 ? p.velocity.y : p.inputMove.y,
                                    p.inputMove.x == 0 && p.inputMove.y == 0 ? p.velocity.x : p.inputMove.x);
                            // switch control to this player if human team and not currently active
                            if (p.team.humanControlled && p.team.getActivePlayer() != p) {
                                p.team.activePlayerIndex = p.team.players.indexOf(p);
                            }
                        }
                    }
                }
            } else {
                // If opposing player collides when someone has ball, they can steal if faster
                for (Player p : allPlayers) {
                    for (Player q : allPlayers) {
                        if (p == q) continue;
                        if (p.team == q.team) continue;
                        if (!q.hasBall) continue;
                        float d = p.position.distance(q.position);
                        float stealDist = p.radius * 1.3f + q.radius * 1.3f;
                        if (d < stealDist && p.velocity.len() > q.velocity.len() + 20 && q.kickCooldown <= 0) {
                            // Steal
                            q.hasBall = false;
                            p.hasBall = true;
                            p.kickCooldown = 0.15;
                            particleSystem.spawnTacklePuff(q.position.add(p.position).mul(0.5f));
                        }
                    }
                }
            }

            // Auto switch active player to nearest to ball for CPU
            if (!teamA.humanControlled) teamA.switchToClosest(ball.position);
            if (!teamB.humanControlled) teamB.switchToClosest(ball.position);
        }

        private boolean anyPlayerHasBall() {
            for (Player p : teamA.players) if (p.hasBall) return true;
            for (Player p : teamB.players) if (p.hasBall) return true;
            return false;
        }

        private void keepBallInBounds() {
            Rectangle2D p = pitch.playArea;
            // Out of touch lines cause rebound with low elasticity
            if (ball.position.x - ball.radius < p.getX()) {
                ball.position.x = (float) (p.getX() + ball.radius);
                ball.velocity.x = Math.abs(ball.velocity.x) * 0.7f;
                particleSystem.spawnBoundaryPuff(ball.position, new Vector2(1, 0));
            }
            if (ball.position.x + ball.radius > p.getMaxX()) {
                ball.position.x = (float) (p.getMaxX() - ball.radius);
                ball.velocity.x = -Math.abs(ball.velocity.x) * 0.7f;
                particleSystem.spawnBoundaryPuff(ball.position, new Vector2(-1, 0));
            }
            if (ball.position.y - ball.radius < p.getY()) {
                ball.position.y = (float) (p.getY() + ball.radius);
                ball.velocity.y = Math.abs(ball.velocity.y) * 0.7f;
                particleSystem.spawnBoundaryPuff(ball.position, new Vector2(0, 1));
            }
            if (ball.position.y + ball.radius > p.getMaxY()) {
                ball.position.y = (float) (p.getMaxY() - ball.radius);
                ball.velocity.y = -Math.abs(ball.velocity.y) * 0.7f;
                particleSystem.spawnBoundaryPuff(ball.position, new Vector2(0, -1));
            }
        }

        private void checkGoals(double dt) {
            // If ball crosses into goal mouth between posts x-range at left or right
            if (goalLeft.isGoal(ball)) {
                goalScored(teamB, teamA, goalLeft);
            } else if (goalRight.isGoal(ball)) {
                goalScored(teamA, teamB, goalRight);
            }
        }

        private void goalScored(Team scoringTeam, Team concedingTeam, Goal goal) {
            scoringTeam.score++;
            celebration = true;
            celebrationTimer = 3.0;
            particleSystem.spawnConfetti(pitch.playArea, goal.mouth.getCenterX(), goal.mouth.getCenterY());
            scoreFlash.flash(scoringTeam.name + " GOAL!", scoringTeam.primary);

            // Net animation burst where ball hit mouth
            if (goal.side == Side.LEFT) netAnimatorLeft.hit(ball.position.copy());
            else netAnimatorRight.hit(ball.position.copy());

            // Reset ball and players after a short delay
            for (Team t : Arrays.asList(teamA, teamB)) {
                for (Player p : t.players) {
                    p.hasBall = false;
                }
            }

            // Center ball slowly rolling to middle
            Rectangle2D f = pitch.playArea;
            ball.position.set((float) f.getCenterX(), (float) f.getCenterY());
            ball.velocity.set(0, 0);

            // Kickoff for conceding team after celebration ends
            // We'll schedule by celebration flag inside update
            // Provide immediate minimal impulse to net for "ripple"
        }

        private void endCelebrationAndKickoff() {
            celebration = false;
            // Conceding team kicks off (last goal awarding team is opposite side to kickoff)
            Team kickingTeam;
            // Last goal decided by which net was hit: if left net was hit, right team scored; left team to kickoff
            // We'll choose by who has fewer goals to kickoff fairness; but use last score: if teamA scored -> teamB kickoff
            // Simpler: alternate on every goal: here let's give kickoff to team that conceded:
            // Use which net animated last: if netAnimatorLeft.recentHit then teamA conceded, so teamA kicks off.
            if (netAnimatorLeft.recentHitTime > netAnimatorRight.recentHitTime) {
                // Left net was hit -> teamA conceded -> teamA kicks off
                kickingTeam = teamA;
            } else {
                kickingTeam = teamB;
            }
            kickoff(kickingTeam);
        }

        private void render() {
            // Clear background with gradient
            Graphics2D g = backG;
            clear(g);

            // Draw pitch
            pitch.draw(g, WIDTH, HEIGHT);

            // Draw goals back nets
            goalLeft.drawBackNet(g, netAnimatorLeft, pitch);
            goalRight.drawBackNet(g, netAnimatorRight, pitch);

            // Ball trail behind ball
            ballTrail.draw(g);

            // Draw shadows
            drawShadows(g);

            // Draw players
            teamA.draw(g);
            teamB.draw(g);

            // Draw ball
            ball.draw(g);

            // Draw goals front posts and mouth
            goalLeft.drawFront(g);
            goalRight.drawFront(g);

            // Effects
            particleSystem.draw(g);

            // HUD
            hud.draw(g, this);

            // Score flash overlay
            scoreFlash.draw(g, WIDTH, HEIGHT);

            // Menu and overlays
            if (state == GameState.MENU) {
                menu.draw(g, WIDTH, HEIGHT);
            } else if (state == GameState.GAMEOVER) {
                drawGameOver(g);
            } else if (paused) {
                drawPaused(g);
            }

            // Celebration timer
            if (celebration) {
                celebrationTimer -= TARGET_DT;
                if (celebrationTimer <= 0) {
                    endCelebrationAndKickoff();
                }
            }

            // Blit to screen
            Graphics screen = getGraphics();
            if (screen != null) {
                screen.drawImage(backBuffer, 0, 0, null);
                screen.dispose();
            }
        }

        private void clear(Graphics2D g) {
            // Sky gradient
            GradientPaint sky = new GradientPaint(0, 0, new Color(30, 40, 60), 0, HEIGHT, new Color(10, 20, 35));
            g.setPaint(sky);
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }

        private void drawShadows(Graphics2D g) {
            // Simple circular projected shadows based on a light source
            Vector2 lightDir = new Vector2(-1, 0.7f).normalized();
            float shadowLen = 8;

            // Player shadows
            Composite old = g.getComposite();
            Color shadow = new Color(0, 0, 0, 50);
            g.setColor(shadow);
            for (Player p : teamA.players) {
                Vector2 s = p.position.add(lightDir.mul(shadowLen));
                g.fill(new Ellipse2D.Float(s.x - p.radius * 1.05f, s.y - p.radius * 0.6f, p.radius * 2.1f, p.radius * 1.2f));
            }
            for (Player p : teamB.players) {
                Vector2 s = p.position.add(lightDir.mul(shadowLen));
                g.fill(new Ellipse2D.Float(s.x - p.radius * 1.05f, s.y - p.radius * 0.6f, p.radius * 2.1f, p.radius * 1.2f));
            }
            // Ball shadow
            Vector2 s = ball.position.add(lightDir.mul(shadowLen * 0.8f));
            g.fill(new Ellipse2D.Float(s.x - ball.radius * 0.9f, s.y - ball.radius * 0.5f, ball.radius * 1.8f, ball.radius));

            g.setComposite(old);
        }

        private void drawPaused(Graphics2D g) {
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(Color.WHITE);
            g.setFont(fontBig);
            String text = "PAUSED";
            int tw = g.getFontMetrics().stringWidth(text);
            g.drawString(text, (WIDTH - tw) / 2, HEIGHT / 2 - 20);

            g.setFont(fontSmall);
            String hint = "Press ESC to resume";
            int hw = g.getFontMetrics().stringWidth(hint);
            g.drawString(hint, (WIDTH - hw) / 2, HEIGHT / 2 + 20);
        }

        private void drawGameOver(Graphics2D g) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setColor(Color.WHITE);
            g.setFont(fontBig);
            String over = "FULL TIME";
            int ow = g.getFontMetrics().stringWidth(over);
            g.drawString(over, (WIDTH - ow) / 2, HEIGHT / 2 - 100);

            g.setFont(fontMed);
            String score = teamA.name + " " + teamA.score + " - " + teamB.score + " " + teamB.name;
            int sw = g.getFontMetrics().stringWidth(score);
            g.setColor(new Color(250, 250, 250));
            g.drawString(score, (WIDTH - sw) / 2, HEIGHT / 2 - 30);

            g.setFont(fontSmall);
            String prompt = "Press ENTER to return to Menu";
            int pw = g.getFontMetrics().stringWidth(prompt);
            g.setColor(Color.LIGHT_GRAY);
            g.drawString(prompt, (WIDTH - pw) / 2, HEIGHT / 2 + 10);
        }

        private void enableQuality(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, 250);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        }

        // Input handlers
        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            if (code >= 0 && code < keys.length) keys[code] = true;

            if (state == GameState.MENU) {
                if (code == KeyEvent.VK_1) {
                    selectedMode = Mode.PLAYER_VS_CPU;
                    initWorld(selectedMode);
                    kickoff(teamA);
                    state = GameState.PLAYING;
                } else if (code == KeyEvent.VK_2) {
                    selectedMode = Mode.PLAYER_VS_PLAYER;
                    initWorld(selectedMode);
                    kickoff(teamA);
                    state = GameState.PLAYING;
                }
            } else if (state == GameState.GAMEOVER && code == KeyEvent.VK_ENTER) {
                state = GameState.MENU;
                menu.reset();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();
            if (code >= 0 && code < keys.length) keys[code] = false;
        }

        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void mouseClicked(MouseEvent e) {
            if (state == GameState.MENU) {
                if (menu.click(e.getPoint())) {
                    selectedMode = menu.selectedMode;
                    initWorld(selectedMode);
                    kickoff(teamA);
                    state = GameState.PLAYING;
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseEntered(MouseEvent e) {}
        @Override
        public void mouseExited(MouseEvent e) {}
        @Override
        public void mouseDragged(MouseEvent e) {}
        @Override
        public void mouseMoved(MouseEvent e) {}

        // --- Classes inside GamePanel scope ---

        enum GameState { MENU, PLAYING, GAMEOVER }

        enum Side { LEFT, RIGHT }

        enum Mode { PLAYER_VS_CPU, PLAYER_VS_PLAYER }

        static class Vector2 {
            float x, y;
            Vector2(float x, float y) { this.x = x; this.y = y; }
            Vector2() { this(0, 0); }
            Vector2 copy() { return new Vector2(x, y); }
            void set(float nx, float ny) { x = nx; y = ny; }
            void set(Vector2 v) { x = v.x; y = v.y; }
            Vector2 add(Vector2 o) { return new Vector2(x + o.x, y + o.y); }
            Vector2 sub(Vector2 o) { return new Vector2(x - o.x, y - o.y); }
            Vector2 mul(float s) { return new Vector2(x * s, y * s); }
            Vector2 div(float s) { return new Vector2(x / s, y / s); }
            float len() { return (float)Math.sqrt(x*x + y*y); }
            float len2() { return x*x + y*y; }
            Vector2 normalized() {
                float l = len();
                if (l < 1e-6) return new Vector2(0,0);
                return new Vector2(x / l, y / l);
            }
            float distance(Vector2 o) {
                float dx = x - o.x, dy = y - o.y;
                return (float)Math.sqrt(dx*dx + dy*dy);
            }
            static Vector2 lerp(Vector2 a, Vector2 b, float t) {
                return new Vector2(a.x + (b.x - a.x)*t, a.y + (b.y - a.y)*t);
            }
            @Override public String toString() { return "("+x+","+y+")"; }
        }

        static class Pitch {
            Rectangle2D playArea;
            private List<Rectangle2D> stripes = new ArrayList<>();
            private Color grass1 = new Color(46, 131, 70);
            private Color grass2 = new Color(41, 118, 63);
            private Color lines = new Color(240, 245, 245, 230);

            Pitch(Rectangle2D playArea) {
                this.playArea = playArea;
                buildStripes();
            }

            void buildStripes() {
                stripes.clear();
                int n = 14;
                double w = playArea.getWidth() / n;
                for (int i = 0; i < n; i++) {
                    stripes.add(new Rectangle2D.Double(playArea.getX() + i * w, playArea.getY(), w, playArea.getHeight()));
                }
            }

            void draw(Graphics2D g, int width, int height) {
                // Outer vignette
                GradientPaint bg = new GradientPaint(0, 0, new Color(0, 10, 20), 0, height, new Color(0, 0, 0));
                g.setPaint(bg);
                g.fillRect(0, 0, width, height);

                // Grass stripes
                for (int i = 0; i < stripes.size(); i++) {
                    g.setColor((i % 2 == 0) ? grass1 : grass2);
                    g.fill(stripes.get(i));
                }

                // Pitch lines
                g.setColor(lines);
                g.setStroke(new BasicStroke(3f));
                g.draw(playArea);

                // Mid line and circle
                double cx = playArea.getCenterX();
                g.draw(new Line2D.Double(cx, playArea.getY(), cx, playArea.getMaxY()));
                g.draw(new Ellipse2D.Double(cx - 70, playArea.getCenterY() - 70, 140, 140));

                // Penalty areas
                double boxW = 130, boxH = playArea.getHeight() * 0.38;
                g.draw(new Rectangle2D.Double(playArea.getX(), playArea.getCenterY() - boxH / 2, boxW, boxH));
                g.draw(new Rectangle2D.Double(playArea.getMaxX() - boxW, playArea.getCenterY() - boxH / 2, boxW, boxH));

                // Penalty spots
                g.fill(new Ellipse2D.Double(playArea.getX() + 86, playArea.getCenterY() - 2, 4, 4));
                g.fill(new Ellipse2D.Double(playArea.getMaxX() - 86 - 4, playArea.getCenterY() - 2, 4, 4));

                // Corner arcs
                drawCorner(g, playArea.getX(), playArea.getY(), 1);
                drawCorner(g, playArea.getMaxX(), playArea.getY(), 2);
                drawCorner(g, playArea.getX(), playArea.getMaxY(), 3);
                drawCorner(g, playArea.getMaxX(), playArea.getMaxY(), 4);
            }

            private void drawCorner(Graphics2D g, double x, double y, int corner) {
                double r = 22;
                double sx = (corner == 1 || corner == 3) ? 1 : -1;
                double sy = (corner == 1 || corner == 2) ? 1 : -1;
                Path2D path = new Path2D.Double();
                path.moveTo(x, y);
                for (int i = 0; i <= 90; i += 3) {
                    double rad = Math.toRadians(i);
                    double px = x + sx * Math.cos(rad) * r;
                    double py = y + sy * Math.sin(rad) * r;
                    path.lineTo(px, py);
                }
                g.draw(path);
            }
        }

        static class Ball {
            Vector2 position;
            Vector2 velocity = new Vector2(0, 0);
            float radius = 8f;
            float friction = 0.985f;

            // Spin visualization
            float spin = 0;

            Ball(Vector2 pos) {
                this.position = pos.copy();
            }

            void update(GamePanel gp, double dt) {
                // Apply velocity
                position = position.add(velocity.mul((float) dt));

                // Friction
                velocity = velocity.mul((float) Math.pow(friction, dt * 60.0));
                if (velocity.len() < 3) velocity.set(0, 0);

                // Spin changes with movement
                spin += velocity.len() * dt * 0.1f;
                if (spin > Math.PI * 2) spin -= (float) (Math.PI * 2);
            }

            void draw(Graphics2D g) {
                // Draw ball with subtle panel pattern and highlight
                float x = position.x;
                float y = position.y;

                // Glow
                RadialGradientPaint glow = new RadialGradientPaint(new Point2D.Float(x, y), radius * 2.2f,
                        new float[]{0f, 1f}, new Color[]{new Color(255, 255, 255, 35), new Color(255, 255, 255, 0)});
                g.setPaint(glow);
                g.fill(new Ellipse2D.Float(x - radius * 2.2f, y - radius * 2.2f, radius * 4.4f, radius * 4.4f));

                // Ball base
                g.setColor(new Color(245, 245, 245));
                g.fill(new Ellipse2D.Float(x - radius, y - radius, radius * 2, radius * 2));
                g.setColor(new Color(50, 50, 50, 180));
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new Ellipse2D.Float(x - radius, y - radius, radius * 2, radius * 2));

                // Pattern panels (triangles/pentagons simplified)
                Graphics2D g2 = (Graphics2D) g.create();
                g2.translate(x, y);
                g2.rotate(spin);
                g2.setColor(new Color(40, 40, 40));
                for (int i = 0; i < 5; i++) {
                    double a = i * (Math.PI * 2 / 5.0);
                    double px = Math.cos(a) * radius * 0.55;
                    double py = Math.sin(a) * radius * 0.55;
                    g2.fill(new Ellipse2D.Double(px - 2, py - 2, 4, 4));
                }
                g2.dispose();
            }
        }

        static class Player {
            int number;
            Team team;
            Vector2 position;
            Vector2 velocity = new Vector2(0, 0);
            Vector2 inputMove = new Vector2(0, 0);
            float radius = 14f;
            boolean sprint = false;
            float speed = 160f;
            float sprintSpeed = 230f;
            float stamina = 100f;
            boolean hasBall = false;

            double dribbleAngle = 0;
            double kickCooldown = 0;
            double passCooldown = 0;
            Vector2 homePos;

            // Animation timer
            double runCycle = 0;

            Player(int number, Team team, Vector2 pos) {
                this.number = number;
                this.team = team;
                this.position = pos.copy();
                this.homePos = pos.copy();
            }

            void update(GamePanel gp, double dt) {
                // Cooldowns
                if (kickCooldown > 0) kickCooldown -= dt;
                if (passCooldown > 0) passCooldown -= dt;

                // Target move (human or AI set inputMove)
                Vector2 move = inputMove.copy();

                // AI will set own move when team is AI controlled
                if (!team.humanControlled && team.ai != null && this == team.getActivePlayer()) {
                    team.ai.controlPlayer(this, gp, dt);
                }

                float targetSpeed = sprint && stamina > 0 ? sprintSpeed : speed;
                Vector2 desiredVel = move.mul(targetSpeed);

                // Acceleration/drag
                Vector2 dv = desiredVel.sub(velocity);
                float accel = 800f;
                velocity = velocity.add(dv.mul((float) (accel * dt)));
                // Cap speed
                float vlen = velocity.len();
                float vmax = sprint && stamina > 0 ? sprintSpeed : speed;
                if (vlen > vmax) velocity = velocity.mul(vmax / vlen);

                // Decay when no input
                if (move.len2() < 0.0001f) velocity = velocity.mul((float) Math.pow(0.88, dt * 60.0));

                // Move
                position = position.add(velocity.mul((float) dt));

                // Stay inside pitch
                Rectangle2D f = gp.pitch.playArea;
                if (position.x - radius < f.getX()) { position.x = (float) (f.getX() + radius); velocity.x *= -0.2f; }
                if (position.x + radius > f.getMaxX()) { position.x = (float) (f.getMaxX() - radius); velocity.x *= -0.2f; }
                if (position.y - radius < f.getY()) { position.y = (float) (f.getY() + radius); velocity.y *= -0.2f; }
                if (position.y + radius > f.getMaxY()) { position.y = (float) (f.getMaxY() - radius); velocity.y *= -0.2f; }

                // Stamina drain/regain
                if (sprint && (move.len2() > 0.01f)) {
                    stamina -= 12f * dt;
                } else {
                    stamina += 9f * dt;
                }
                stamina = Math.max(0, Math.min(100, stamina));

                // Update animation
                runCycle += velocity.len() * dt * 0.07;
                if (runCycle > Math.PI * 2) runCycle -= Math.PI * 2;

                // Dribble angle follows input or velocity
                if (move.len2() > 0.001) {
                    dribbleAngle = Math.atan2(move.y, move.x);
                } else if (velocity.len2() > 0.001) {
                    dribbleAngle = Math.atan2(velocity.y, velocity.x);
                }
            }

            boolean canShoot() {
                return hasBall && kickCooldown <= 0;
            }

            boolean canPass() {
                return hasBall && passCooldown <= 0;
            }

            Vector2 getFacingDir() {
                return new Vector2((float) Math.cos(dribbleAngle), (float) Math.sin(dribbleAngle));
            }

            void draw(Graphics2D g) {
                float x = position.x;
                float y = position.y;

                // Body with two-tone jersey and number
                Color jersey = team.primary;
                Color secondary = team.secondary;

                // Outline
                g.setColor(new Color(0, 0, 0, 90));
                g.setStroke(new BasicStroke(2.0f));
                g.draw(new Ellipse2D.Float(x - radius, y - radius, radius * 2, radius * 2));

                // Jersey split gradient
                GradientPaint gp = new GradientPaint(x, y - radius, jersey, x, y + radius, jersey.darker());
                g.setPaint(gp);
                g.fill(new Ellipse2D.Float(x - radius, y - radius, radius * 2, radius * 2));

                // Shoulder stripe
                g.setColor(secondary);
                g.fill(new Ellipse2D.Float(x - radius, y - radius + 3, radius * 2, 6));

                // Legs animation lines
                g.setColor(new Color(0, 0, 0, 100));
                float lx1 = (float) (x + Math.cos(runCycle) * radius * 0.4);
                float ly1 = (float) (y + Math.abs(Math.sin(runCycle)) * radius * 0.6);
                float lx2 = (float) (x - Math.cos(runCycle) * radius * 0.4);
                float ly2 = (float) (y + Math.abs(Math.sin(runCycle + Math.PI)) * radius * 0.6);
                g.draw(new Line2D.Float(lx1, y, lx1, ly1));
                g.draw(new Line2D.Float(lx2, y, lx2, ly2));

                // Number badge
                g.setColor(new Color(255, 255, 255, 210));
                g.fill(new Ellipse2D.Float(x - 8, y - 8, 16, 16));
                g.setColor(new Color(30, 30, 30));
                g.setFont(new Font("SansSerif", Font.BOLD, 10));
                String num = String.valueOf(number);
                int tw = g.getFontMetrics().stringWidth(num);
                g.drawString(num, x - tw / 2f, y + 3);

                // Active ring for controlled player
                if (team.getActivePlayer() == this) {
                    g.setColor(new Color(255, 255, 255, 200));
                    g.setStroke(new BasicStroke(2f));
                    g.draw(new Ellipse2D.Float(x - radius - 5, y - radius - 5, (radius + 5) * 2, (radius + 5) * 2));
                }

                // Stamina bar
                int barW = 26, barH = 4;
                int bx = (int) (x - barW / 2);
                int by = (int) (y - radius - 12);
                g.setColor(new Color(0, 0, 0, 130));
                g.fillRect(bx - 1, by - 1, barW + 2, barH + 2);
                g.setColor(new Color(60, 60, 60, 180));
                g.fillRect(bx, by, barW, barH);
                g.setColor(new Color(90, 220, 90));
                g.fillRect(bx, by, (int) (barW * (stamina / 100f)), barH);
            }
        }

        static class Team {
            String name;
            Color primary;
            Color secondary;
            Side goalSide;
            boolean humanControlled = true;
            TeamAI ai;
            List<Player> players = new ArrayList<>();
            int score = 0;

            int activePlayerIndex = 0;
            double energyRegen = 0.4;

            Team(String name, Color primary, Color secondary, Side side, boolean human) {
                this.name = name;
                this.primary = primary;
                this.secondary = secondary;
                this.goalSide = side;
                this.humanControlled = human;
            }

            void addPlayer(Player p) { players.add(p); }

            void draw(Graphics2D g) {
                for (Player p : players) p.draw(g);
            }

            Player getActivePlayer() {
                if (players.isEmpty()) return null;
                if (activePlayerIndex < 0 || activePlayerIndex >= players.size()) activePlayerIndex = 0;
                return players.get(activePlayerIndex);
            }

            void selectClosestTo(Vector2 pos) {
                float min = Float.MAX_VALUE;
                int idx = 0;
                for (int i = 0; i < players.size(); i++) {
                    float d = players.get(i).position.distance(pos);
                    if (d < min) {
                        min = d;
                        idx = i;
                    }
                }
                activePlayerIndex = idx;
            }

            void switchToClosest(Vector2 pos) {
                selectClosestTo(pos);
            }

            Player getClosestTo(Vector2 pos) {
                Player best = null;
                float min = Float.MAX_VALUE;
                for (Player p : players) {
                    float d = p.position.distance(pos);
                    if (d < min) { min = d; best = p; }
                }
                return best;
            }

            Player findBestPassTarget(Player from, Ball ball, Rectangle2D field) {
                Player best = null;
                float bestScore = -1e9f;
                for (Player p : players) {
                    if (p == from) continue;
                    // Score by open lane and forward progress
                    float forward = (goalSide == Side.LEFT) ? (p.position.x - from.position.x) : (from.position.x - p.position.x);
                    float dist = from.position.distance(p.position);
                    if (dist < 40 || dist > 500) continue;
                    float lane = 1.0f;
                    float score = forward * 0.8f + lane * 100 - dist * 0.1f;
                    if (score > bestScore) {
                        bestScore = score;
                        best = p;
                    }
                }
                return best;
            }

            void setHomePositionsFromCurrent() {
                for (Player p : players) p.homePos = p.position.copy();
            }

            void resetToHome() {
                for (Player p : players) {
                    p.position.set(p.homePos);
                    p.velocity.set(0, 0);
                    p.hasBall = false;
                    p.stamina = 100;
                }
            }
        }

        static class TeamAI {
            Team team;
            Player cachedActive;
            double retargetTimer = 0;

            TeamAI(Team team) {
                this.team = team;
            }

            void updateAI(GamePanel gp, double dt) {
                retargetTimer -= dt;
                if (retargetTimer <= 0) {
                    // Active player is nearest to ball
                    team.selectClosestTo(gp.ball.position);
                    retargetTimer = 0.2 + Math.random() * 0.2;
                }
            }

            void controlPlayer(Player p, GamePanel gp, double dt) {
                Team opp = (team == gp.teamA) ? gp.teamB : gp.teamA;
                Rectangle2D f = gp.pitch.playArea;
                Vector2 toBall = gp.ball.position.sub(p.position);
                float distToBall = toBall.len();

                // Default move: go to ball if close, otherwise move to position between ball and our goal (defend)
                p.inputMove = new Vector2(0, 0);
                p.sprint = false;

                // If has ball: advance toward opponent goal
                if (p.hasBall) {
                    Vector2 goal = new Vector2((float) (opp.goalSide == Side.LEFT ? f.getX() + 20 : f.getMaxX() - 20),
                            (float) f.getCenterY());
                    Vector2 dir = goal.sub(p.position).normalized();
                    p.inputMove = dir;

                    // Decide to shoot if close to goal
                    float distToGoal = p.position.distance(goal);
                    if (distToGoal < 260 && p.canShoot()) {
                        Vector2 target = gp.aimTowardGoal(p, opp);
                        gp.shoot(p, target, 740, true);
                    } else if (p.canPass()) {
                        // Pass if a teammate is in a better forward position
                        Player mate = team.findBestPassTarget(p, gp.ball, gp.pitch.playArea);
                        if (mate != null) {
                            // If mate more central and less marked, pass
                            if (mate.position.distance(gp.ball.position) > 100 || distToGoal > 300) {
                                gp.pass(p, mate.position);
                            }
                        }
                    }

                    // Sprint when attacking
                    p.sprint = p.stamina > 20;
                    return;
                }

                // If not in possession
                Player owner = gp.getBallOwner();
                if (owner != null) {
                    if (owner.team == team) {
                        // We are supporting play: move forward into space
                        double forwardX = (team.goalSide == Side.LEFT) ? Math.min(f.getMaxX() - 40, p.position.x + 120) :
                                Math.max(f.getX() + 40, p.position.x - 120);
                        double targetY = clamp(gp.ball.position.y + (float) (Math.sin(p.number + System.currentTimeMillis() * 0.001) * 40),
                                (float) f.getY() + 30, (float) f.getMaxY() - 30);
                        Vector2 target = new Vector2((float) forwardX, (float) targetY);
                        p.inputMove = target.sub(p.position).normalized();
                        p.sprint = Math.random() < 0.2 && p.stamina > 30;
                    } else {
                        // Opponent has ball: mark the ball or intercept path
                        if (distToBall < 300) {
                            p.inputMove = toBall.normalized(); // chase
                            p.sprint = p.stamina > 20;
                        } else {
                            // Fall back toward home position
                            p.inputMove = p.homePos.sub(p.position).normalized();
                            p.sprint = false;
                        }
                    }
                } else {
                    // Ball free: closest should sprint toward ball
                    Player closest = team.getClosestTo(gp.ball.position);
                    if (closest == p) {
                        p.inputMove = toBall.normalized();
                        p.sprint = p.stamina > 20;
                    } else {
                        // Others reposition
                        p.inputMove = p.homePos.sub(p.position).normalized();
                    }
                }
            }
        }

        Player getBallOwner() {
            for (Player p : teamA.players) if (p.hasBall) return p;
            for (Player p : teamB.players) if (p.hasBall) return p;
            return null;
        }

        static class Goal {
            Rectangle2D mouth;
            Side side;
            Color postColor = new Color(240, 240, 240);

            Goal(double x, double y, double w, double h, Side side) {
                this.mouth = new Rectangle2D.Double(x, y, w, h);
                this.side = side;
            }

            boolean isGoal(Ball ball) {
                // For goal detection, check if ball center is inside goal mouth rectangle (allow slight depth)
                // Extend mouth inward a bit
                Rectangle2D test = new Rectangle2D.Double(mouth.getX() - (side == Side.LEFT ? 10 : 0),
                        mouth.getY(), mouth.getWidth() + (side == Side.RIGHT ? 10 : 0), mouth.getHeight());
                return test.contains(ball.position.x, ball.position.y);
            }

            void drawBackNet(Graphics2D g, NetAnimator net, Pitch pitch) {
                // Draw net mesh as wavy lines inside mouth area
                Graphics2D g2 = (Graphics2D) g.create();
                enableQuality(g2);
                g2.setClip(pitch.playArea);

                g2.setColor(new Color(255, 255, 255, 50));
                double depth = 40;
                double frontX = mouth.getX();
                double rearX = mouth.getX() + (side == Side.LEFT ? -depth : depth);
                int rows = 10;
                int cols = 6;

                for (int r = 0; r <= rows; r++) {
                    double ty = mouth.getY() + mouth.getHeight() * (r / (double) rows);
                    Path2D path = new Path2D.Double();
                    for (int c = 0; c <= cols; c++) {
                        double t = c / (double) cols;
                        double x = lerp(frontX, rearX, t);
                        double y = ty + net.offset(x, ty) * 4;
                        if (c == 0) path.moveTo(x, y);
                        else path.lineTo(x, y);
                    }
                    g2.draw(path);
                }

                for (int c = 0; c <= cols; c++) {
                    double tx = lerp(frontX, rearX, c / (double) cols);
                    Path2D path = new Path2D.Double();
                    for (int r = 0; r <= rows; r++) {
                        double t = r / (double) rows;
                        double y = mouth.getY() + mouth.getHeight() * t + net.offset(tx, t) * 4;
                        if (r == 0) path.moveTo(tx, y);
                        else path.lineTo(tx, y);
                    }
                    g2.draw(path);
                }

                g2.dispose();
            }

            void drawFront(Graphics2D g) {
                g.setColor(postColor);
                g.setStroke(new BasicStroke(4f));
                g.draw(mouth);
            }

            private double lerp(double a, double b, double t) {
                return a + (b - a) * t;
            }
        }

        static class NetAnimator {
            double waveTime = 0;
            double hitTimer = 0;
            Vector2 hitPoint = null;
            double recentHitTime = -999;

            void reset() {
                waveTime = 0;
                hitTimer = 0;
                hitPoint = null;
                recentHitTime = -999;
            }

            void update(double dt) {
                waveTime += dt;
                if (hitTimer > 0) hitTimer -= dt;
            }

            void arm(Vector2 dir) {
                // Placeholder to prime some state if needed
            }

            void hit(Vector2 point) {
                hitPoint = point.copy();
                hitTimer = 1.2;
                recentHitTime = System.nanoTime() / 1e9;
            }

            float offset(double x, double y) {
                double w = Math.sin(waveTime * 5 + x * 0.06 + y * 0.04);
                double h = hitTimer > 0 && hitPoint != null ? Math.exp(-((x - hitPoint.x) * (x - hitPoint.x) + (y - hitPoint.y) * (y - hitPoint.y)) / 8000.0) * Math.sin(waveTime * 20) : 0;
                return (float) (w * 1.0 + h * 4.0);
            }
        }

        static class Particle {
            Vector2 pos, vel;
            float life, maxLife;
            Color color;
            float size;
            float spin, spinSpeed;

            Particle(Vector2 pos, Vector2 vel, float life, Color color, float size) {
                this.pos = pos.copy();
                this.vel = vel.copy();
                this.life = life;
                this.maxLife = life;
                this.color = color;
                this.size = size;
                this.spin = (float) (Math.random() * Math.PI * 2);
                this.spinSpeed = (float) (Math.random() * 6 - 3);
            }

            boolean alive() { return life > 0; }

            void update(double dt) {
                pos = pos.add(vel.mul((float) dt));
                vel = vel.mul(0.98f);
                life -= dt;
                spin += spinSpeed * dt;
            }

            void draw(Graphics2D g) {
                if (life <= 0) return;
                float t = life / maxLife;
                Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * t));
                g.setColor(c);
                float s = size * (0.5f + 0.5f * t);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.translate(pos.x, pos.y);
                g2.rotate(spin);
                g2.fill(new Ellipse2D.Float(-s / 2, -s / 2, s, s));
                g2.dispose();
            }
        }

        static class ParticleSystem {
            List<Particle> particles = new ArrayList<>();
            Random rng = new Random();

            void spawnShotBurst(Vector2 pos, Vector2 dir) {
                for (int i = 0; i < 40; i++) {
                    Vector2 v = dir.mul(200 + rng.nextFloat() * 250).add(new Vector2((rng.nextFloat() - 0.5f) * 100, (rng.nextFloat() - 0.5f) * 100));
                    particles.add(new Particle(pos.copy(), v, 0.6f + rng.nextFloat() * 0.4f, new Color(255, 255, 200, 180), 5 + rng.nextFloat() * 6));
                }
            }

            void spawnPassPuffs(Vector2 pos, Vector2 dir) {
                for (int i = 0; i < 16; i++) {
                    Vector2 v = dir.mul(120 + rng.nextFloat() * 140).add(new Vector2((rng.nextFloat() - 0.5f) * 50, (rng.nextFloat() - 0.5f) * 50));
                    particles.add(new Particle(pos.copy(), v, 0.4f + rng.nextFloat() * 0.2f, new Color(255, 255, 255, 150), 4 + rng.nextFloat() * 4));
                }
            }

            void spawnTacklePuff(Vector2 pos) {
                for (int i = 0; i < 30; i++) {
                    Vector2 v = new Vector2((rng.nextFloat() - 0.5f) * 280, (rng.nextFloat() - 0.5f) * 280);
                    particles.add(new Particle(pos.copy(), v, 0.5f + rng.nextFloat() * 0.4f, new Color(255, 100, 100, 180), 5 + rng.nextFloat() * 5));
                }
            }

            void spawnBoundaryPuff(Vector2 pos, Vector2 normal) {
                for (int i = 0; i < 10; i++) {
                    Vector2 v = normal.mul(120 + rng.nextFloat() * 120).add(new Vector2((rng.nextFloat() - 0.5f) * 70, (rng.nextFloat() - 0.5f) * 70));
                    particles.add(new Particle(pos.copy(), v, 0.35f + rng.nextFloat() * 0.2f, new Color(220, 220, 220, 160), 3 + rng.nextFloat() * 4));
                }
            }

            void spawnConfetti(Rectangle2D field, double cx, double cy) {
                // Burst across top
                for (int i = 0; i < 200; i++) {
                    float x = (float) (field.getX() + Math.random() * field.getWidth());
                    float y = (float) (field.getY() - 20 - Math.random() * 60);
                    Vector2 pos = new Vector2(x, y);
                    Vector2 vel = new Vector2((float) (Math.random() * 80 - 40), (float) (60 + Math.random() * 140));
                    Color c = rainbow(i * 7);
                    particles.add(new Particle(pos, vel, 2.0f + (float) Math.random() * 1.5f, new Color(c.getRed(), c.getGreen(), c.getBlue(), 210), 3 + rng.nextFloat() * 4));
                }
            }

            Color rainbow(int i) {
                float h = (i % 360) / 360f;
                return Color.getHSBColor(h, 0.9f, 1.0f);
            }

            void update(double dt) {
                Iterator<Particle> it = particles.iterator();
                while (it.hasNext()) {
                    Particle p = it.next();
                    p.update(dt);
                    if (!p.alive()) it.remove();
                }
            }

            void draw(Graphics2D g) {
                for (Particle p : particles) p.draw(g);
            }

            void clear() { particles.clear(); }
        }

        static class HUD {
            DecimalFormat tf = new DecimalFormat("00");

            void draw(Graphics2D g, GamePanel gp) {
                // Top banner
                int w = gp.WIDTH;
                g.setColor(new Color(0, 0, 0, 100));
                g.fillRect(0, 0, w, 50);

                // Team names and scores
                g.setFont(new Font("SansSerif", Font.BOLD, 22));
                g.setColor(gp.teamA.primary);
                g.drawString(gp.teamA.name, 20, 32);
                g.setColor(new Color(255, 255, 255));
                g.drawString("" + gp.teamA.score, 260, 32);

                g.setColor(gp.teamB.primary);
                String nameB = gp.teamB.name;
                int nbw = g.getFontMetrics().stringWidth(nameB);
                g.drawString(nameB, gp.WIDTH - 20 - nbw, 32);
                g.setColor(new Color(255, 255, 255));
                String sb = "" + gp.teamB.score;
                int sbw = g.getFontMetrics().stringWidth(sb);
                g.drawString(sb, gp.WIDTH - 260 + (100 - sbw), 32);

                // Center timer
                g.setFont(new Font("SansSerif", Font.BOLD, 20));
                g.setColor(new Color(240, 240, 240));
                int m = (int) (gp.matchTimeSeconds / 60);
                int s = (int) (gp.matchTimeSeconds % 60);
                String time = "Half " + gp.currentHalf + "  " + tf.format(m) + ":" + tf.format(s);
                int tw = g.getFontMetrics().stringWidth(time);
                g.drawString(time, (gp.WIDTH - tw) / 2, 32);

                // Mode
                g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                String mode = (gp.selectedMode == Mode.PLAYER_VS_CPU) ? "Player vs CPU" : "Player vs Player";
                g.setColor(new Color(220, 220, 220, 180));
                g.drawString(mode, (gp.WIDTH - g.getFontMetrics().stringWidth(mode)) / 2, 48);

                // Legend
                g.setFont(new Font("Monospaced", Font.PLAIN, 13));
                g.setColor(new Color(255, 255, 255, 160));
                g.drawString("P1: WASD move, Shift sprint, F pass, Space shoot, R switch, ESC pause", 18, gp.HEIGHT - 20);
                g.drawString("P2: Arrows move, RShift sprint, K pass, L shoot, P switch", 18, gp.HEIGHT - 4);
            }
        }

        static class BallTrail {
            private final int maxPoints;
            private final ArrayList<Vector2> points = new ArrayList<>();
            private float alpha = 0;

            BallTrail(int maxPoints) {
                this.maxPoints = maxPoints;
            }

            void update(Vector2 pos) {
                points.add(pos.copy());
                if (points.size() > maxPoints) points.remove(0);
                alpha *= 0.95f;
            }

            void kick() {
                alpha = 1.0f;
            }

            void clear() {
                points.clear();
                alpha = 0;
            }

            void draw(Graphics2D g) {
                if (points.size() < 2 || alpha < 0.01) return;
                for (int i = 1; i < points.size(); i++) {
                    Vector2 a = points.get(i - 1);
                    Vector2 b = points.get(i);
                    float t = i / (float) points.size();
                    int al = (int) (60 * alpha * t);
                    g.setColor(new Color(255, 255, 255, al));
                    g.setStroke(new BasicStroke(2f * (1 - t)));
                    g.draw(new Line2D.Float(a.x, a.y, b.x, b.y));
                }
            }
        }

        static class ScoreFlash {
            String text = "";
            Color color = Color.WHITE;
            double timer = 0;

            void flash(String text, Color color) {
                this.text = text;
                this.color = color;
                this.timer = 2.5;
            }

            void update(double dt) {
                if (timer > 0) timer -= dt;
            }

            void draw(Graphics2D g, int width, int height) {
                if (timer <= 0) return;
                float t = (float) Math.max(0, timer / 2.5);
                int alpha = (int) (255 * Math.min(1, t * 1.2));
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                g.setFont(new Font("SansSerif", Font.BOLD, 50));
                int tw = g.getFontMetrics().stringWidth(text);
                g.drawString(text, (width - tw) / 2, height / 2 - 160);
            }

            void reset() {
                text = "";
                color = Color.WHITE;
                timer = 0;
            }
        }

        static class Menu {
            Rectangle2D btn1 = new Rectangle2D.Double(450, 420, 380, 60);
            Rectangle2D btn2 = new Rectangle2D.Double(450, 500, 380, 60);
            Mode selectedMode = Mode.PLAYER_VS_CPU;
            double anim = 0;

            void reset() { anim = 0; }

            void update(double dt) { anim += dt; }

            boolean click(Point p) {
                if (btn1.contains(p)) {
                    selectedMode = Mode.PLAYER_VS_CPU;
                    return true;
                } else if (btn2.contains(p)) {
                    selectedMode = Mode.PLAYER_VS_PLAYER;
                    return true;
                }
                return false;
            }

            void draw(Graphics2D g, int width, int height) {
                // Dim background already drawn
                // Title
                g.setFont(new Font("SansSerif", Font.BOLD, 64));
                g.setColor(Color.WHITE);
                String title = "FIFA 2026 - Java Edition";
                int tw = g.getFontMetrics().stringWidth(title);
                g.drawString(title, (width - tw) / 2, 220);

                // Subtitle flicker
                g.setFont(new Font("SansSerif", Font.PLAIN, 20));
                g.setColor(new Color(220, 240, 255, 200));
                String sub = "Select Mode";
                int sw = g.getFontMetrics().stringWidth(sub);
                g.drawString(sub, (width - sw) / 2, 260);

                // Buttons
                drawButton(g, btn1, "1) Player vs CPU", anim);
                drawButton(g, btn2, "2) Player vs Player", anim + 1.0);

                // Tip
                g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                String tip = "Tip: Use Space/L to shoot, F/K to pass. Enjoy!";
                int tw2 = g.getFontMetrics().stringWidth(tip);
                g.setColor(new Color(230, 230, 230, 180));
                g.drawString(tip, (width - tw2) / 2, (int) (btn2.getMaxY() + 40));
            }

            void drawButton(Graphics2D g, Rectangle2D r, String label, double t) {
                float pulse = (float) (0.85 + 0.15 * Math.sin(t * 2));
                g.setColor(new Color(10, 20, 40, 150));
                g.fill(r);
                g.setColor(new Color(120, 180, 255));
                g.setStroke(new BasicStroke(2.2f));
                g.draw(r);
                g.setFont(new Font("SansSerif", Font.BOLD, 22));
                int lw = g.getFontMetrics().stringWidth(label);
                g.setColor(new Color(255, 255, 255, (int) (240 * pulse)));
                g.drawString(label, (int) (r.getCenterX() - lw / 2), (int) (r.getCenterY() + 8));
            }
        }

        static float clamp(float v, float lo, float hi) {
            return Math.max(lo, Math.min(hi, v));
        }
        static double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }
    }
}