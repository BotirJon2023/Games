import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RugbyGameSimulation extends JFrame {

    // Constants for the window size; panel will handle dynamic scaling.
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;

    public RugbyGameSimulation() {
        super("Rugby Game Simulation - Single File");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        GamePanel panel = new GamePanel();
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        panel.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RugbyGameSimulation::new);
    }

    // GamePanel encapsulates the game loop, rendering, and input.
    static class GamePanel extends JPanel implements ActionListener, ComponentListener, KeyListener, MouseListener, MouseMotionListener {
        // Timer for fixed updates; Swing Timer runs on EDT.
        private final Timer timer;
        private long lastNanoTime;
        private double accumulator;

        // World and game objects.
        private final Random rng = new Random(42);
        private final Pitch pitch = new Pitch();
        private final Camera camera = new Camera();
        private final HUD hud = new HUD();
        private final EventLog eventLog = new EventLog();
        private final ParticleSystem particles = new ParticleSystem();
        private final ReplayBuffer replay = new ReplayBuffer();
        private final InputState input = new InputState();
        private final Physics physics = new Physics();

        private Team teamA;    // Left side
        private Team teamB;    // Right side
        private Ball ball;

        private GameState gameState = GameState.KICKOFF;
        private double stateTimer = 0.0;
        private double matchClock = 0.0;    // seconds
        private boolean paused = false;
        private boolean showDebug = false;
        private boolean showNameTags = true;
        private boolean showShadows = true;
        private boolean showTrails = true;
        private boolean staminaEnabled = true;
        private boolean followBallCamera = false;

        // Assets
        private final Font uiFont = new Font("SansSerif", Font.BOLD, 16);
        private final Font bigFont = new Font("SansSerif", Font.BOLD, 36);
        private final Color grassLight = new Color(80, 144, 76);
        private final Color grassDark = new Color(70, 132, 66);

        // Bounds and scale for world to screen conversion.
        private int viewportWidth = WINDOW_WIDTH;
        private int viewportHeight = WINDOW_HEIGHT;

        // Kickoff side reference; left team starts.
        private Team kickoffTeam;
        private Team defendTeam;

        // For mouse interactions (not required, but included for completeness).
        private Point mousePoint = new Point();

        // Constructor sets size, focus, and timer.
        public GamePanel() {
            setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
            setBackground(new Color(30, 30, 30));
            setFocusable(true);
            requestFocusInWindow();
            addComponentListener(this);
            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);

            // 60 updates per second.
            timer = new Timer(1000 / 60, this);

            initMatch();
        }

        // Initialize match: teams, ball, placement, and kickoff.
        private void initMatch() {
            // Build teams with players and kit colors.
            teamA = new Team("Blue Bears", Side.LEFT, new Color(30, 90, 180));
            teamB = new Team("Red Rhinos", Side.RIGHT, new Color(200, 50, 50));
            teamA.opponent = teamB;
            teamB.opponent = teamA;

            // Spawn players with positions. We'll place them along formation lines.
            createTeamPlayers(teamA, 1);
            createTeamPlayers(teamB, -1);

            // Create the ball at the center spot initially.
            ball = new Ball(new Vec2(pitch.lengthMeters / 2.0, pitch.widthMeters / 2.0));

            // Determine kickoff side randomly or default to left.
            kickoffTeam = teamA;
            defendTeam = teamB;

            arrangeKickoff();
            eventLog.add("Match start: " + teamA.name + " vs " + teamB.name);
        }

        // Create player entities for a team with a given direction.
        private void createTeamPlayers(Team team, int dir) {
            // Real rugby has 15 players per side. We'll use 7 for clarity and performance.
            team.players.clear();
            int playerCount = 9;
            double baseX = (team.side == Side.LEFT) ? pitch.lengthMeters * 0.25 : pitch.lengthMeters * 0.75;
            double startX = baseX + (dir * -5.0);
            double startYCenter = pitch.widthMeters / 2.0;
            double spacingY = 6.0;

            for (int i = 0; i < playerCount; i++) {
                double yOffset = (i - (playerCount - 1) / 2.0) * spacingY;
                double px = startX + rng.nextDouble() * 2.0 - 1.0;
                double py = startYCenter + yOffset + rng.nextDouble() * 2.0 - 1.0;

                Player p = new Player(team, "P" + (i + 1), new Vec2(px, py));
                // Different roles roughly based on index
                if (i == 0) p.role = Role.FLY_HALF;
                else if (i < 3) p.role = Role.BACK;
                else if (i < 6) p.role = Role.CENTER;
                else p.role = Role.FORWARD;

                // Variation stats
                p.maxSpeed = 6.5 + rng.nextDouble() * 1.0;   // m/s
                p.accel = 18 + rng.nextDouble() * 6;         // m/s^2
                p.agility = 0.85 + rng.nextDouble() * 0.15;  // turn responsiveness
                p.stamina = 100;
                p.passing = 0.65 + rng.nextDouble() * 0.25;
                p.kicking = 0.6 + rng.nextDouble() * 0.25;
                p.tackling = 0.6 + rng.nextDouble() * 0.25;
                p.number = i + 1;

                team.players.add(p);
            }

            // Default controlled index
            team.controlledIndex = 0;
        }

        // After spawn, arrange kickoff positions and possession.
        private void arrangeKickoff() {
            // Put ball at center
            ball.pos.set(pitch.lengthMeters / 2.0, pitch.widthMeters / 2.0);
            ball.vel.set(0, 0);
            ball.height = 0;
            ball.owner = null;

            // Kickoff team one player at center to kick, rest behind 10m line.
            Team atk = kickoffTeam;
            Team def = defendTeam;

            // offensive team near center
            double centerX = pitch.lengthMeters / 2.0;
            double centerY = pitch.widthMeters / 2.0;
            Player kicker = atk.players.get(0);
            kicker.pos.set(centerX - (atk.side == Side.LEFT ? 2.0 : -2.0), centerY);
            kicker.vel.set(0, 0);
            kicker.hasBall = true;
            ball.attachTo(kicker);

            // Others align behind
            double lineX = centerX - (atk.side == Side.LEFT ? 5.0 : -5.0);
            double spacingY = 5.0;
            for (int i = 1; i < atk.players.size(); i++) {
                double y = centerY + (i - (atk.players.size() - 1) / 2.0) * spacingY;
                atk.players.get(i).pos.set(lineX + rng.nextDouble() * 2 - 1, y + rng.nextDouble() * 2 - 1);
                atk.players.get(i).vel.set(0, 0);
                atk.players.get(i).hasBall = false;
                atk.players.get(i).state = PlayerState.IDLE;
            }

            // defensive team spread at their half
            double defLineX = centerX + (def.side == Side.LEFT ? -10.0 : 10.0);
            for (int i = 0; i < def.players.size(); i++) {
                double y = centerY + (i - (def.players.size() - 1) / 2.0) * spacingY;
                def.players.get(i).pos.set(defLineX + rng.nextDouble() * 2 - 1, y + rng.nextDouble() * 2 - 1);
                def.players.get(i).vel.set(0, 0);
                def.players.get(i).hasBall = false;
                def.players.get(i).state = PlayerState.IDLE;
            }

            // Reset meta
            gameState = GameState.KICKOFF;
            stateTimer = 0;
            eventLog.add(atk.name + " to kickoff.");
        }

        // Reset entire match state including scores.
        private void resetMatch() {
            teamA.resetScore();
            teamB.resetScore();
            matchClock = 0;
            kickoffTeam = rng.nextBoolean() ? teamA : teamB;
            defendTeam = (kickoffTeam == teamA) ? teamB : teamA;
            for (Team t : new Team[]{teamA, teamB}) {
                t.controlledIndex = 0;
                for (Player p : t.players) {
                    p.reset();
                }
            }
            particles.clear();
            replay.clear();
            arrangeKickoff();
        }

        // Start the timer for updates.
        public void start() {
            lastNanoTime = System.nanoTime();
            accumulator = 0;
            timer.start();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            double dt = (now - lastNanoTime) / 1_000_000_000.0;
            lastNanoTime = now;

            // clamp dt to avoid crazy jumps when pausing or dragging window
            if (dt > 0.1) dt = 0.1;

            if (!paused) {
                step(dt);
            }
            repaint();
        }

        // Game update loop.
        private void step(double dt) {
            // Keep a short replay buffer even during play.
            replay.capture(this, dt);

            // Update game clock
            if (gameState == GameState.PLAYING) {
                matchClock += dt;
            }

            // Update state timer
            stateTimer += dt;

            // Update AI and input control
            processInput(dt);
            runAI(dt);

            // Physics and collisions
            updatePlayers(dt);
            updateBall(dt);
            resolveCollisions();

            // Camera follow
            updateCamera(dt);

            // Particles
            particles.update(dt);

            // State transitions
            handleState(dt);
        }

        // Processes keyboard input for the user-controlled team.
        private void processInput(double dt) {
            Team userTeam = teamA; // assume player controls team A
            Player ctrl = userTeam.currentControlled();

            // Switch controlled player
            if (input.consumePressed(KeyEvent.VK_SPACE)) {
                userTeam.selectBestControlled(ball);
                eventLog.add("Switched control to " + userTeam.currentControlled().nameId());
            }

            // Sprint toggle
            boolean sprinting = input.isDown(KeyEvent.VK_SHIFT) || input.isDown(KeyEvent.VK_SHIFT);

            // Movement direction
            double moveX = 0, moveY = 0;
            if (input.isDown(KeyEvent.VK_W) || input.isDown(KeyEvent.VK_UP)) moveY -= 1;
            if (input.isDown(KeyEvent.VK_S) || input.isDown(KeyEvent.VK_DOWN)) moveY += 1;
            if (input.isDown(KeyEvent.VK_A) || input.isDown(KeyEvent.VK_LEFT)) moveX -= 1;
            if (input.isDown(KeyEvent.VK_D) || input.isDown(KeyEvent.VK_RIGHT)) moveX += 1;
            Vec2 desired = new Vec2(moveX, moveY).normalized();

            if (ctrl != null) {
                // Only allow movement if not tackled
                if (ctrl.state != PlayerState.TACKLED && ctrl.state != PlayerState.STUNNED) {
                    double targetSpeed = ctrl.maxSpeed * (sprinting ? 1.1 : 0.85);
                    if (!staminaEnabled) targetSpeed = ctrl.maxSpeed;

                    // stamina drain/gain
                    if (staminaEnabled) {
                        if (sprinting && desired.lengthSq() > 0.01) {
                            ctrl.stamina = Math.max(0, ctrl.stamina - dt * 10);
                            if (ctrl.stamina <= 20) targetSpeed *= 0.85;
                        } else {
                            ctrl.stamina = Math.min(100, ctrl.stamina + dt * 6);
                        }
                    }

                    // Steering
                    Vec2 targetVel = desired.scale(targetSpeed);
                    Vec2 dv = targetVel.sub(ctrl.vel);
                    double maxAccel = ctrl.accel * ctrl.agility;
                    if (dv.length() > maxAccel * dt) {
                        dv = dv.normalized().scale(maxAccel * dt);
                    }
                    ctrl.vel = ctrl.vel.add(dv);
                    if (ctrl.vel.length() > targetSpeed) {
                        ctrl.vel = ctrl.vel.normalized().scale(targetSpeed);
                    }

                    // facing direction
                    if (ctrl.vel.lengthSq() > 0.05) {
                        ctrl.heading = ctrl.vel.normalized();
                    }

                    // Visual feedback: speed lines
                    if (showTrails && ctrl.vel.lengthSq() > 10) {
                        for (int i = 0; i < 2; i++) {
                            particles.spawnTrail(ctrl.pos, ctrl.team.color);
                        }
                    }
                }

                // Passing
                if (input.consumePressed(KeyEvent.VK_Q)) {
                    if (ctrl.hasBall) {
                        Player target = userTeam.findBestPassTarget(ctrl, ball, this.pitch);
                        if (target != null) {
                            performPass(ctrl, target);
                        } else {
                            eventLog.add("No suitable pass target.");
                        }
                    } else {
                        eventLog.add("Cannot pass: no ball.");
                    }
                }

                // Kicking
                if (input.consumePressed(KeyEvent.VK_E)) {
                    if (ctrl.hasBall) {
                        performKick(ctrl);
                    } else {
                        eventLog.add("Cannot kick: no ball.");
                    }
                }

                // Tackle
                if (input.consumePressed(KeyEvent.VK_F)) {
                    performTackle(ctrl);
                }
            }

            // Pause
            if (input.consumePressed(KeyEvent.VK_P)) {
                paused = !paused;
                eventLog.add(paused ? "Paused." : "Resumed.");
            }

            // Toggles and tools
            if (input.consumePressed(KeyEvent.VK_F1)) {
                showDebug = !showDebug;
            }
            if (input.consumePressed(KeyEvent.VK_F2)) {
                showNameTags = !showNameTags;
            }
            if (input.consumePressed(KeyEvent.VK_F3)) {
                showTrails = !showTrails;
            }
            if (input.consumePressed(KeyEvent.VK_F4)) {
                showShadows = !showShadows;
            }
            if (input.consumePressed(KeyEvent.VK_F5)) {
                staminaEnabled = !staminaEnabled;
                eventLog.add("Stamina " + (staminaEnabled ? "enabled" : "disabled"));
            }
            if (input.consumePressed(KeyEvent.VK_R)) {
                eventLog.add("Resetting match.");
                resetMatch();
            }
            if (input.consumePressed(KeyEvent.VK_C)) {
                followBallCamera = !followBallCamera;
                eventLog.add("Camera: " + (followBallCamera ? "Follow Ball" : "Follow Player"));
            }
        }

        // Perform a pass from one player to another.
        private void performPass(Player passer, Player target) {
            // Basic pass: direct pass with slight lead; speed based on passer's passing skill.
            Vec2 delta = target.pos.sub(passer.pos);
            double dist = delta.length();
            double passSpeed = 14 + passer.passing * 6.0; // m/s
            Vec2 lead = target.vel.scale(Math.min(dist / passSpeed, 0.6)); // lead clamp
            Vec2 aim = delta.add(lead).normalized();
            Vec2 v = aim.scale(passSpeed);

            ball.detach();
            ball.vel.set(v.x, v.y);
            ball.pos.set(passer.pos.x + aim.x * 0.6, passer.pos.y + aim.y * 0.6);
            ball.height = 0.7; // chest-level pass
            ball.zVel = 0;

            passer.hasBall = false;
            passer.lastPassTime = stateTimer;

            eventLog.add(passer.nameId() + " passes to " + target.nameId());

            // Visual effect
            particles.spawnPassPuff(passer.pos);
        }

        // Perform a kick from a player.
        private void performKick(Player kicker) {
            // Kick direction uses heading; if barely moving, use towards opponent goal.
            Vec2 dir = kicker.heading.lengthSq() > 0.01 ? kicker.heading.normalized()
                    : (kicker.team.side == Side.LEFT ? new Vec2(1, 0) : new Vec2(-1, 0));

            double pow = 18 + kicker.kicking * 8; // m/s initial
            Vec2 v = dir.scale(pow * 0.85);
            ball.detach();
            ball.pos = kicker.pos.copy().add(dir.scale(0.8));
            ball.vel = v;
            ball.height = 1.5;
            ball.zVel = 6 + kicker.kicking * 4;

            kicker.hasBall = false;
            eventLog.add(kicker.nameId() + " kicks!");

            // Kick dust and streak
            particles.spawnKickFlash(kicker.pos);
        }

        // Attempt to tackle an opponent near the controlled player.
        private void performTackle(Player tackler) {
            // Find target: opponent in front and within radius
            Player target = tackler.team.opponent.findBestTackleTarget(tackler);
            if (target != null) {
                double dist = target.pos.sub(tackler.pos).length();
                if (dist < 3.5) {
                    // Resolve tackle: success depends on skills and relative velocity
                    double attack = tackler.tackling * (1.0 + tackler.vel.length() * 0.1);
                    double defend = 0.8 + target.vel.length() * 0.08 + rng.nextDouble() * 0.3;
                    boolean success = attack > defend;

                    // Visual approach: tackler lunges quickly
                    tackler.state = PlayerState.TACKLING;
                    tackler.tackleCooldown = 0.6;

                    if (success) {
                        target.state = PlayerState.TACKLED;
                        target.stunTimer = 1.2;
                        target.vel = target.vel.scale(0.2);
                        if (target.hasBall) {
                            // Ball spills
                            target.hasBall = false;
                            ball.detach();
                            ball.pos = target.pos.copy();
                            ball.height = 0.3;
                            ball.vel = target.heading.scale(2).add(new Vec2(rng.nextDouble() - 0.5, rng.nextDouble() - 0.5).scale(2));
                            eventLog.add(tackler.nameId() + " tackles " + target.nameId() + "! Ball loose!");
                            particles.spawnTackleImpact(target.pos);
                        } else {
                            eventLog.add(tackler.nameId() + " tackles " + target.nameId());
                            particles.spawnTackleImpact(target.pos);
                        }
                    } else {
                        // Missed tackle or slip
                        tackler.stunTimer = 0.6;
                        tackler.state = PlayerState.STUNNED;
                        eventLog.add(tackler.nameId() + " misses tackle on " + target.nameId());
                        particles.spawnTackleWhiff(tackler.pos);
                    }
                } else {
                    eventLog.add("Too far to tackle.");
                }
            } else {
                eventLog.add("No tackle target.");
            }
        }

        // AI update for both teams.
        private void runAI(double dt) {
            aiTeamBehavior(teamA, dt, teamA == teamA); // player team: AI for non-controlled players
            aiTeamBehavior(teamB, dt, true);           // full AI
        }

        // Per-team AI: each player chooses actions and target velocities.
        private void aiTeamBehavior(Team team, double dt, boolean enableAI) {
            if (!enableAI) return;

            // Determine team roles based on possession
            boolean hasBall = team.hasPossession(ball);
            Player carrier = team.findCarrier(ball);

            // For each player:
            for (int i = 0; i < team.players.size(); i++) {
                Player p = team.players.get(i);

                boolean isUserControlled = (team == teamA && i == team.controlledIndex);

                // Skip input override for user-controlled player on teamA
                if (isUserControlled && team == teamA) {
                    continue;
                }

                // If tackled or stunned, manage timers
                if (p.state == PlayerState.TACKLED || p.state == PlayerState.STUNNED) {
                    p.stunTimer -= dt;
                    if (p.stunTimer <= 0) {
                        p.state = PlayerState.IDLE;
                    }
                    continue;
                }

                // AI decisions:
                // If we have the ball and you are carrier, run towards enemy goal with slight lateral movement.
                if (p.hasBall) {
                    Vec2 goal = (p.team.side == Side.LEFT)
                            ? new Vec2(pitch.lengthMeters - 2, pitch.widthMeters / 2)
                            : new Vec2(2, pitch.widthMeters / 2);

                    // Avoid touch lines: keep within field boundaries
                    double margin = 6.0;
                    double targetY = clamp(p.pos.y, margin, pitch.widthMeters - margin);

                    // Occasionally dodge
                    double lateral = Math.sin(stateTimer * (0.7 + rng.nextDouble() * 0.6)) * 1.5;
                    targetY = clamp(targetY + lateral, margin, pitch.widthMeters - margin);

                    Vec2 toGoal = new Vec2(goal.x, targetY).sub(p.pos);
                    Vec2 desiredDir = toGoal.normalized();
                    // Slightly slower to simulate carrying
                    p.seek(desiredDir, p.maxSpeed * 0.95, dt);

                    // If defenders are close ahead, attempt a pass with some probability
                    if (rng.nextDouble() < 0.02) {
                        Player best = p.team.findBestPassTarget(p, ball, pitch);
                        if (best != null) {
                            performPass(p, best);
                        }
                    }

                    // Kick if near opponent 22 line and under pressure
                    double opp22 = (p.team.side == Side.LEFT) ? (pitch.lengthMeters * 0.78) : (pitch.lengthMeters * 0.22);
                    boolean underPressure = p.team.opponent.closestTo(p.pos).pos.distanceTo(p.pos) < 5;
                    if ((p.team.side == Side.LEFT && p.pos.x > opp22 || p.team.side == Side.RIGHT && p.pos.x < opp22)
                            && underPressure && rng.nextDouble() < 0.008) {
                        performKick(p);
                    }
                    continue;
                }

                // If our team has the ball, support attacker or form a line.
                if (hasBall) {
                    if (carrier != null) {
                        // Support line: position slightly behind and to the side of the carrier
                        double offsetX = (team.side == Side.LEFT) ? -3 : 3;
                        double offsetY = (i % 2 == 0) ? 3 : -3;
                        Vec2 support = carrier.pos.add(new Vec2(offsetX + rng.nextGaussian(), offsetY + rng.nextGaussian()));
                        p.seek(support.sub(p.pos).normalized(), p.maxSpeed * 0.9, dt);
                        // Tidy marking to not cluster
                        p.avoidTeammates(team, dt);
                    }
                } else {
                    // No possession: chase ball or mark opponent
                    Player enemyCarrier = team.opponent.findCarrier(ball);
                    if (enemyCarrier != null) {
                        // Defend: cut line to carrier
                        Vec2 ahead = enemyCarrier.pos.add(enemyCarrier.vel.scale(0.4));
                        Vec2 to = ahead.sub(p.pos);
                        p.seek(to.normalized(), p.maxSpeed, dt);

                        // If close, attempt auto tackle with some interval
                        if (p.pos.distanceTo(enemyCarrier.pos) < 3.6 && p.tackleCooldown <= 0) {
                            performTackle(p);
                        }
                    } else {
                        // Loose ball: attempt to recover
                        if (ball.owner == null) {
                            Vec2 toBall = ball.pos.sub(p.pos);
                            p.seek(toBall.normalized(), p.maxSpeed, dt);
                        } else {
                            // Mark the nearest opponent
                            Player target = team.opponent.closestTo(p.pos);
                            double spacing = 3.0;
                            Vec2 desiredPos = target.pos.add(new Vec2(team.side == Side.LEFT ? -spacing : spacing, 0));
                            p.seek(desiredPos.sub(p.pos).normalized(), p.maxSpeed * 0.85, dt);
                        }
                    }
                }

                // Cooldowns
                p.tackleCooldown = Math.max(0, p.tackleCooldown - dt);
            }
        }

        // Update player physics and animation.
        private void updatePlayers(double dt) {
            for (Team t : new Team[]{teamA, teamB}) {
                for (Player p : t.players) {
                    // Integrate velocity
                    p.pos = p.pos.add(p.vel.scale(dt));

                    // Friction and dampening
                    double drag = 2.0;
                    p.vel = p.vel.scale(Math.max(0, 1 - drag * dt));

                    // Boundary clamp within pitch
                    Vec2 clamped = pitch.clampToField(p.pos, 1.0);
                    if (!clamped.equals(p.pos)) {
                        p.pos = clamped;
                        p.vel = new Vec2(0, 0);
                    }

                    // Animate shoulder sway / step for visuals
                    p.animPhase += p.vel.length() * dt * 0.4;
                    p.animPhase %= Math.PI * 2;

                    // Carry ball attachment update
                    if (p.hasBall) {
                        ball.attachTo(p);
                    }
                }
            }
        }

        // Update ball physics, including gravity for height.
        private void updateBall(double dt) {
            if (ball.owner == null) {
                // Gravity for z
                double g = 9.81;
                ball.zVel -= g * dt;
                ball.height += ball.zVel * dt;
                if (ball.height < 0) {
                    ball.height = 0;
                    if (Math.abs(ball.zVel) > 3) {
                        // bounce
                        ball.zVel = -ball.zVel * 0.35;
                        particles.spawnBounceDust(ball.pos);
                    } else {
                        ball.zVel = 0;
                    }
                }

                // Ball air drag
                double airDrag = 0.6;
                ball.vel = ball.vel.scale(Math.max(0, 1 - airDrag * dt));

                // Integrate position
                ball.pos = ball.pos.add(ball.vel.scale(dt));

                // Collision with pitch boundaries (touch)
                Vec2 clamped = pitch.clampToField(ball.pos, 0.5);
                if (!clamped.equals(ball.pos)) {
                    // Out of bounds: line-out -> simple drop to nearest player of opponent
                    outOfBoundsRestart(ball.pos);
                    return;
                }

                // Attempt to pick up if near a player
                Player nearest = nearestPlayer(ball.pos, 1.2);
                if (nearest != null) {
                    // If ball on ground (height low), pickup
                    if (ball.height < 0.3) {
                        nearest.hasBall = true;
                        ball.attachTo(nearest);
                        eventLog.add(nearest.nameId() + " picks up the ball.");
                    }
                }
            } else {
                // Owner carrying; ensure within field
                Vec2 clamped = pitch.clampToField(ball.pos, 0.5);
                if (!clamped.equals(ball.pos)) {
                    // Force detachment to avoid bugs
                    ball.detach();
                }
            }

            // Check for scoring: crossing try line with ball in hand.
            checkScoring();
        }

        // Resolve collisions between players mildly (simple separation).
        private void resolveCollisions() {
            List<Player> all = new ArrayList<>();
            all.addAll(teamA.players);
            all.addAll(teamB.players);

            for (int i = 0; i < all.size(); i++) {
                for (int j = i + 1; j < all.size(); j++) {
                    Player a = all.get(i);
                    Player b = all.get(j);
                    double minDist = 0.8; // player radius sum
                    Vec2 d = b.pos.sub(a.pos);
                    double dist = d.length();
                    if (dist > 0 && dist < minDist) {
                        Vec2 push = d.normalized().scale((minDist - dist) * 0.5);
                        a.pos = a.pos.add(push.scale(-1));
                        b.pos = b.pos.add(push);
                        // Damp velocity on collision
                        a.vel = a.vel.scale(0.85);
                        b.vel = b.vel.scale(0.85);
                    }
                }
            }
        }

        // Manage state transitions like kickoff and after try.
        private void handleState(double dt) {
            switch (gameState) {
                case KICKOFF:
                    // Wait a moment then auto-kick
                    if (stateTimer > 1.0) {
                        // Kick long to start
                        Player kicker = kickoffTeam.currentControlled();
                        if (kicker == null) kicker = kickoffTeam.players.get(0);
                        if (kicker.hasBall) {
                            performKick(kicker);
                            gameState = GameState.PLAYING;
                            stateTimer = 0;
                            eventLog.add("Kickoff taken by " + kicker.nameId());
                        } else {
                            gameState = GameState.PLAYING;
                            stateTimer = 0;
                        }
                    }
                    break;
                case TRY_SCORED:
                    // Short celebration then restart with kickoff to conceding team
                    if (stateTimer > 3.0) {
                        kickoffTeam = defendTeam; // Now conceding team kicks off
                        defendTeam = (kickoffTeam == teamA) ? teamB : teamA;
                        arrangeKickoff();
                    }
                    break;
                case PLAYING:
                case PAUSED:
                default:
                    break;
            }
        }

        // Called when ball goes out at touch lines or dead-ball area; simple restart.
        private void outOfBoundsRestart(Vec2 pos) {
            // Hand ball to opponent at nearest line; simple drop restart.
            Team possession = (ball.owner != null) ? ball.owner.team : null;
            Team restartTeam = (pos.x < pitch.lengthMeters / 2.0) ? teamB : teamA;
            if (possession != null) restartTeam = possession.opponent;

            // Place nearest player of restartTeam at line-in and give ball
            Player taker = restartTeam.closestTo(pos);
            Vec2 inbound = pitch.clampToField(pos, 1.0);
            taker.pos.set(inbound.x, inbound.y);
            for (Player p : restartTeam.players) p.hasBall = false;
            taker.hasBall = true;
            ball.attachTo(taker);
            eventLog.add("Ball out of bounds. Restart to " + restartTeam.name + " by " + taker.nameId());
            gameState = GameState.PLAYING;
            stateTimer = 0;
        }

        // Check try scoring conditions.
        private void checkScoring() {
            // If a ball carrier crosses opponent try line, award try.
            Player aCarrier = teamA.findCarrier(ball);
            Player bCarrier = teamB.findCarrier(ball);

            if (aCarrier != null) {
                // Team A left side: must cross Team B goal line on right
                if (aCarrier.pos.x > pitch.lengthMeters - pitch.goalDepth) {
                    scoreTry(teamA, aCarrier);
                }
            } else if (bCarrier != null) {
                if (bCarrier.pos.x < pitch.goalDepth) {
                    scoreTry(teamB, bCarrier);
                }
            }
        }

        // Register a try and update state and logs.
        private void scoreTry(Team scorer, Player carrier) {
            scorer.scoreTry();
            eventLog.add("TRY! " + scorer.name + " by " + carrier.nameId() + ". Score: " + scoreLine());

            // Particles and effects
            for (int i = 0; i < 20; i++) {
                particles.spawnConfetti(carrier.pos, scorer.color);
            }

            // Store replay state
            replay.markEvent("TRY by " + carrier.nameId() + " (" + scorer.name + ")");

            // Reset ball and set next kickoff
            ball.detach();
            defendTeam = scorer;
            kickoffTeam = scorer.opponent;

            gameState = GameState.TRY_SCORED;
            stateTimer = 0;
        }

        private String scoreLine() {
            return teamA.name + " " + teamA.score + " - " + teamB.score + " " + teamB.name;
        }

        // Find the nearest player to a position within a radius.
        private Player nearestPlayer(Vec2 position, double maxDist) {
            Player best = null;
            double bestD = maxDist;
            for (Team t : new Team[]{teamA, teamB}) {
                for (Player p : t.players) {
                    double d = p.pos.distanceTo(position);
                    if (d < bestD) {
                        bestD = d;
                        best = p;
                    }
                }
            }
            return best;
        }

        // Update camera to follow controlled player or ball.
        private void updateCamera(double dt) {
            Vec2 targetWorld;
            if (followBallCamera || ball.owner == null) {
                targetWorld = ball.pos;
            } else {
                Player ctrl = teamA.currentControlled();
                targetWorld = (ctrl != null) ? ctrl.pos : ball.pos;
            }

            // Smooth follow
            camera.target.set(targetWorld.x, targetWorld.y);
            camera.update(dt, pitch, viewportWidth, viewportHeight);
        }

        // Component events to track size changes.
        @Override
        public void componentResized(ComponentEvent e) {
            viewportWidth = getWidth();
            viewportHeight = getHeight();
        }

        @Override
        public void componentMoved(ComponentEvent e) {
        }

        @Override
        public void componentShown(ComponentEvent e) {
        }

        @Override
        public void componentHidden(ComponentEvent e) {
        }

        // Painting the game: background, field, players, ball, HUD.
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            // Enable anti-aliasing for nicer visuals
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Draw pitch (grid and lines) using camera transform
            AffineTransform prev = g2.getTransform();
            AffineTransform worldToScreen = camera.worldTransform(pitch, viewportWidth, viewportHeight);
            g2.transform(worldToScreen);

            drawPitch(g2);

            // Draw particles behind players if needed
            particles.renderUnder(g2);

            // Draw players
            drawTeam(g2, teamA);
            drawTeam(g2, teamB);

            // Draw ball
            drawBall(g2);

            // Draw particles above
            particles.renderOver(g2);

            // Restore to screen space
            g2.setTransform(prev);

            // Draw HUD
            hud.render(g2, this);

            // Debug overlay
            if (showDebug) {
                drawDebug(g2);
            }

            g2.dispose();
        }

        private void drawPitch(Graphics2D g2) {
            // Grass
            Path2D pitchRect = new Path2D.Double();
            pitchRect.moveTo(0, 0);
            pitchRect.lineTo(pitch.lengthMeters, 0);
            pitchRect.lineTo(pitch.lengthMeters, pitch.widthMeters);
            pitchRect.lineTo(0, pitch.widthMeters);
            pitchRect.closePath();

            // Stripes
            int stripes = 14;
            double stripeW = pitch.widthMeters / stripes;
            for (int i = 0; i < stripes; i++) {
                g2.setColor((i % 2 == 0) ? grassLight : grassDark);
                g2.fill(new Rectangle.Double(0, i * stripeW, pitch.lengthMeters, stripeW));
            }

            // Lines: sidelines, try lines, halfway, 22m lines
            g2.setStroke(new BasicStroke(0.2f));
            g2.setColor(Color.white);
            // Sidelines
            g2.draw(new Rectangle.Double(0, 0, pitch.lengthMeters, pitch.widthMeters));
            // Try lines
            g2.draw(new Line2D.Double(pitch.goalDepth, 0, pitch.goalDepth, pitch.widthMeters));
            g2.draw(new Line2D.Double(pitch.lengthMeters - pitch.goalDepth, 0, pitch.lengthMeters - pitch.goalDepth, pitch.widthMeters));
            // Halfway
            g2.draw(new Line2D.Double(pitch.lengthMeters / 2.0, 0, pitch.lengthMeters / 2.0, pitch.widthMeters));
            // 22m
            double line22L = pitch.goalDepth + 22;
            double line22R = pitch.lengthMeters - (pitch.goalDepth + 22);
            g2.draw(new Line2D.Double(line22L, 0, line22L, pitch.widthMeters));
            g2.draw(new Line2D.Double(line22R, 0, line22R, pitch.widthMeters));
            // 10m lines from half
            double line10L = pitch.lengthMeters / 2.0 - 10;
            double line10R = pitch.lengthMeters / 2.0 + 10;
            g2.draw(new Line2D.Double(line10L, 0, line10L, pitch.widthMeters));
            g2.draw(new Line2D.Double(line10R, 0, line10R, pitch.widthMeters));

            // Goal posts simplified markers
            g2.setColor(new Color(230, 230, 255));
            double postY1 = pitch.widthMeters * 0.5 - 2.5;
            double postY2 = pitch.widthMeters * 0.5 + 2.5;
            double postXLeft = pitch.goalDepth - 0.1;
            double postXRight = pitch.lengthMeters - pitch.goalDepth + 0.1;
            g2.fill(new Rectangle.Double(postXLeft, postY1, 0.2, 5));
            g2.fill(new Rectangle.Double(postXRight - 0.2, postY1, 0.2, 5));
        }

        private void drawTeam(Graphics2D g2, Team team) {
            for (int i = 0; i < team.players.size(); i++) {
                Player p = team.players.get(i);
                drawPlayer(g2, p, i == team.controlledIndex && team == teamA);
            }
        }

        private void drawPlayer(Graphics2D g2, Player p, boolean isControlled) {
            double radius = 0.5; // meters
            // Shadow
            if (showShadows) {
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fill(new java.awt.geom.Ellipse2D.Double(p.pos.x - radius, p.pos.y - radius * 0.4, radius * 2, radius * 0.8));
            }

            // Body
            g2.setColor(p.team.color);
            g2.fill(new java.awt.geom.Ellipse2D.Double(p.pos.x - radius, p.pos.y - radius, radius * 2, radius * 2));

            // Facing arrow
            Vec2 head = p.pos.add(p.heading.scale(0.6));
            g2.setColor(Color.white);
            g2.setStroke(new BasicStroke(0.12f));
            g2.draw(new Line2D.Double(p.pos.x, p.pos.y, head.x, head.y));

            // Jersey number
            g2.setColor(Color.black);
            g2.setFont(new Font("SansSerif", Font.BOLD, 0).deriveFont(0.5f));
            // We could render text in world with scaling; omitted for clarity.

            // Controlled ring
            if (isControlled) {
                float dash[] = {0.3f, 0.15f};
                g2.setColor(new Color(255, 255, 255, 220));
                g2.setStroke(new BasicStroke(0.12f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1, dash, (float) (p.animPhase)));
                g2.draw(new java.awt.geom.Ellipse2D.Double(p.pos.x - radius - 0.3, p.pos.y - radius - 0.3, (radius + 0.3) * 2, (radius + 0.3) * 2));
            }

            // Name tags in screen space
            if (showNameTags) {
                // Convert world to screen point with camera transform
                // We'll render above head
                // We'll use a simple overlay (but we are in world transform here, so push text transform smaller)
                // For simplicity, draw a small cap above: a tiny white arc representing number spot
                g2.setColor(new Color(255, 255, 255, 160));
                g2.fill(new java.awt.geom.Ellipse2D.Double(p.pos.x - 0.18, p.pos.y - radius - 0.2, 0.36, 0.22));
            }

            // Ball indicator carried
            if (p.hasBall) {
                g2.setColor(Color.yellow);
                g2.setStroke(new BasicStroke(0.08f));
                g2.draw(new java.awt.geom.Ellipse2D.Double(p.pos.x - radius - 0.15, p.pos.y - radius - 0.15, (radius + 0.15) * 2, (radius + 0.15) * 2));
            }

            // Health/stamina arc
            if (staminaEnabled) {
                double r = radius + 0.18;
                double perc = p.stamina / 100.0;
                g2.setColor(new Color(0, 0, 0, 40));
                g2.setStroke(new BasicStroke(0.1f));
                g2.draw(new java.awt.geom.Ellipse2D.Double(p.pos.x - r, p.pos.y - r, r * 2, r * 2));
                g2.setColor(new Color(50, 200, 90, 200));
                drawArc(g2, p.pos.x, p.pos.y, r, -90, (int) (360 * perc));
            }
        }

        // Draw ball with height-based shadow offset.
        private void drawBall(Graphics2D g2) {
            double size = 0.45; // radius in meters for visual
            // Shadow
            if (showShadows) {
                g2.setColor(new Color(0, 0, 0, 60));
                double shadowScale = Math.max(0.2, 1 - ball.height * 0.1);
                g2.fill(new java.awt.geom.Ellipse2D.Double(ball.pos.x - size * shadowScale, ball.pos.y - size * 0.4 * shadowScale, size * 2 * shadowScale, size * 0.8 * shadowScale));
            }

            // Ball body (simple ellipse)
            g2.setColor(new Color(210, 190, 130));
            g2.fill(new java.awt.geom.Ellipse2D.Double(ball.drawX(), ball.drawY(), size * 2, size));

            // Seams
            g2.setColor(new Color(150, 130, 90));
            g2.setStroke(new BasicStroke(0.07f));
            g2.draw(new java.awt.geom.Ellipse2D.Double(ball.drawX(), ball.drawY(), size * 2, size));
            g2.draw(new java.awt.geom.Ellipse2D.Double(ball.drawX() + 0.2, ball.drawY() + 0.08, size * 1.6, size * 0.6));
        }

        // Draw HUD: scores, timer, minimap, logs, hints.
        private void drawHUDText(Graphics2D g2, int x, int y, String text, Color c) {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(x - 6, y - 18, g2.getFontMetrics().stringWidth(text) + 12, 26, 8, 8);
            g2.setColor(c);
            g2.drawString(text, x, y);
        }

        private void drawMinimap(Graphics2D g2, int x, int y, int w, int h) {
            // Mini-map depicts field top-down; simple rectangle
            g2.setColor(new Color(20, 60, 20));
            g2.fillRoundRect(x, y, w, h, 8, 8);
            g2.setColor(Color.white);
            g2.drawRoundRect(x, y, w, h, 8, 8);

            // Transform world positions into minimap coords
            for (Team t : new Team[]{teamA, teamB}) {
                for (Player p : t.players) {
                    int px = x + (int) (p.pos.x / pitch.lengthMeters * w);
                    int py = y + (int) (p.pos.y / pitch.widthMeters * h);
                    g2.setColor(t.color);
                    g2.fillOval(px - 3, py - 3, 6, 6);
                }
            }

            // Ball marker
            int bx = x + (int) (ball.pos.x / pitch.lengthMeters * w);
            int by = y + (int) (ball.pos.y / pitch.widthMeters * h);
            g2.setColor(Color.yellow);
            g2.fillOval(bx - 3, by - 3, 6, 6);
        }

        private void drawDebug(Graphics2D g2) {
            int y = 100;
            g2.setFont(uiFont);
            g2.setColor(Color.white);
            g2.drawString("Debug:", 10, y); y += 18;
            g2.drawString("State: " + gameState + " t=" + String.format("%.2f", stateTimer), 10, y); y += 18;
            g2.drawString("Clock: " + formatTime(matchClock), 10, y); y += 18;
            g2.drawString("Ball: pos=" + ball.pos + " h=" + String.format("%.2f", ball.height) + " v=" + ball.vel, 10, y); y += 18;
            Player ctrl = teamA.currentControlled();
            if (ctrl != null) {
                g2.drawString("Controlled: " + ctrl.nameId() + " pos=" + ctrl.pos + " v=" + ctrl.vel + " stamina=" + String.format("%.1f", ctrl.stamina), 10, y); y += 18;
            }
            g2.drawString("Score: " + scoreLine(), 10, y); y += 18;
        }

        private String formatTime(double seconds) {
            int m = (int) (seconds / 60);
            int s = (int) (seconds % 60);
            return String.format("%02d:%02d", m, s);
        }

        // HUD rendering
        class HUD {
            void render(Graphics2D g2, GamePanel panel) {
                // Scores and timer
                g2.setFont(bigFont);
                String score = teamA.name + " " + teamA.score + " - " + teamB.score + " " + teamB.name;
                int sw = g2.getFontMetrics().stringWidth(score);
                int sx = (viewportWidth - sw) / 2;
                int sy = 40;

                // Shadow
                g2.setColor(new Color(0, 0, 0, 120));
                g2.drawString(score, sx + 2, sy + 2);
                g2.setColor(Color.white);
                g2.drawString(score, sx, sy);

                // Timer
                g2.setFont(uiFont);
                String t = formatTime(matchClock);
                drawHUDText(g2, viewportWidth / 2 - g2.getFontMetrics().stringWidth(t) / 2, sy + 24, t, Color.white);

                // State message
                if (gameState == GameState.KICKOFF) {
                    String msg = "Kickoff: " + kickoffTeam.name;
                    drawHUDText(g2, (viewportWidth - g2.getFontMetrics().stringWidth(msg)) / 2, sy + 48, msg, Color.yellow);
                } else if (gameState == GameState.TRY_SCORED) {
                    String msg = "TRY! Restart soon...";
                    drawHUDText(g2, (viewportWidth - g2.getFontMetrics().stringWidth(msg)) / 2, sy + 48, msg, Color.orange);
                } else if (paused) {
                    String msg = "PAUSED";
                    drawHUDText(g2, (viewportWidth - g2.getFontMetrics().stringWidth(msg)) / 2, sy + 48, msg, Color.lightGray);
                }

                // Controls hint
                g2.setFont(uiFont);
                String hint = "Move: WASD/Arrows | Sprint: Shift | Pass: Q | Kick: E | Tackle: F | Switch: Space | Pause: P | Debug: F1";
                drawHUDText(g2, 10, viewportHeight - 10, hint, Color.white);

                // Minimap
                drawMinimap(g2, viewportWidth - 210, viewportHeight - 150, 200, 140);

                // Event log
                eventLog.render(g2, 10, viewportHeight - 200, 420, 180);
            }
        }

        // Utility method to draw an arc in world coords
        private void drawArc(Graphics2D g2, double cx, double cy, double r, double startDeg, int extentDeg) {
            g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, startDeg, extentDeg, Arc2D.OPEN));
        }

        // Input handlers. We use KeyListener for simplicity and consume toggles via InputState.
        @Override
        public void keyTyped(KeyEvent e) { }

        @Override
        public void keyPressed(KeyEvent e) {
            input.setKey(e.getKeyCode(), true);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            input.setKey(e.getKeyCode(), false);
        }

        @Override
        public void mouseClicked(MouseEvent e) { }

        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();
        }

        @Override
        public void mouseReleased(MouseEvent e) { }

        @Override
        public void mouseEntered(MouseEvent e) { }

        @Override
        public void mouseExited(MouseEvent e) { }

        @Override
        public void mouseDragged(MouseEvent e) {
            mousePoint = e.getPoint();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mousePoint = e.getPoint();
        }
    }

    // Pitch defines world units and boundaries for the Rugby field.
    static class Pitch {
        // Real rugby pitch ~100m length + in-goals; we'll set simple parameters.
        public final double lengthMeters = 120.0; // including in-goal areas
        public final double widthMeters = 70.0;
        public final double goalDepth = 10.0; // in-goal area depth

        // Convert world bounds to screen using a consistent scaling applied by camera.
        Vec2 clampToField(Vec2 p, double margin) {
            double x = clamp(p.x, 0 + margin, lengthMeters - margin);
            double y = clamp(p.y, 0 + margin, widthMeters - margin);
            return new Vec2(x, y);
        }
    }

    // Camera handles world->screen transforms and smooth following.
    static class Camera {
        Vec2 pos = new Vec2(0, 0);
        Vec2 target = new Vec2(0, 0);
        double zoom = 8; // pixels per meter (computed dynamically)
        double smooth = 6.0; // follow stiffness

        AffineTransform worldTransform(Pitch pitch, int viewportWidth, int viewportHeight) {
            // Determine zoom to fit width with margins
            double scaleX = viewportWidth / pitch.lengthMeters;
            double scaleY = viewportHeight / pitch.widthMeters;
            // Choose uniform scale keeping aspect
            double scale = Math.min(scaleX, scaleY);
            // Provide a bit of zooming adaptively
            zoom = scale * 0.9;

            // Center on pos
            double tx = viewportWidth / 2.0 - pos.x * zoom;
            double ty = viewportHeight / 2.0 - pos.y * zoom;

            AffineTransform at = new AffineTransform();
            at.translate(tx, ty);
            at.scale(zoom, zoom);
            return at;
        }

        void update(double dt, Pitch pitch, int viewportWidth, int viewportHeight) {
            // Approach target smoothly
            Vec2 to = target.sub(pos);
            double damp = Math.pow(0.001, dt * smooth);
            pos = new Vec2(pos.x + to.x * (1 - damp), pos.y + to.y * (1 - damp));

            // Clamp camera center to field
            pos = pitch.clampToField(pos, 5.0);
        }
    }

    // Player entity with movement stats and state.
    static class Player {
        Team team;
        String name;
        int number;
        Role role = Role.BACK;

        Vec2 pos;
        Vec2 vel = new Vec2(0, 0);
        Vec2 heading = new Vec2(1, 0);

        boolean hasBall = false;
        PlayerState state = PlayerState.IDLE;
        double stunTimer = 0;
        double tackleCooldown = 0;

        // Stats
        double maxSpeed = 7.5;
        double accel = 14;
        double agility = 0.9;
        double stamina = 100;

        double passing = 0.6;
        double kicking = 0.6;
        double tackling = 0.6;

        // Animation property
        double animPhase = 0;

        // Timers
        double lastPassTime = -999;

        public Player(Team team, String name, Vec2 position) {
            this.team = team;
            this.name = name;
            this.pos = position.copy();
        }

        public void reset() {
            vel.set(0, 0);
            hasBall = false;
            state = PlayerState.IDLE;
            stunTimer = 0;
            tackleCooldown = 0;
            stamina = 100;
            lastPassTime = -999;
        }

        public String nameId() {
            return name + " #" + number;
        }

        public void seek(Vec2 dir, double targetSpeed, double dt) {
            if (dir.lengthSq() < 0.0001) return;
            Vec2 desiredVel = dir.normalized().scale(targetSpeed);
            Vec2 dv = desiredVel.sub(vel);

            double maxAcc = accel * agility;
            double dl = dv.length();
            if (dl > maxAcc * dt) {
                dv = dv.scale((maxAcc * dt) / dl);
            }
            vel = vel.add(dv);

            // Facing direction follows
            if (vel.lengthSq() > 0.01) {
                heading = vel.normalized();
            }
        }

        public void avoidTeammates(Team team, double dt) {
            Vec2 push = new Vec2(0, 0);
            for (Player other : team.players) {
                if (other == this) continue;
                Vec2 d = pos.sub(other.pos);
                double dist = d.length();
                if (dist > 0 && dist < 1.0) {
                    push = push.add(d.scale((1.0 - dist) * 2.0 / dist));
                }
            }
            if (push.lengthSq() > 0.0001) {
                vel = vel.add(push.scale(dt));
            }
        }
    }

    // Team class grouping players and scoring.
    static class Team {
        String name;
        Side side;
        Color color;
        List<Player> players = new ArrayList<>();
        Team opponent;

        int score = 0; // simplified scoring

        int controlledIndex = 0;

        public Team(String name, Side side, Color color) {
            this.name = name;
            this.side = side;
            this.color = color;
        }

        public void resetScore() {
            score = 0;
        }

        public Player currentControlled() {
            if (players.isEmpty()) return null;
            controlledIndex = clampIndex(controlledIndex, 0, players.size() - 1);
            return players.get(controlledIndex);
        }

        public void selectBestControlled(Ball ball) {
            // Choose nearest to ball or ball carrier if own team holds ball
            Player best = null;
            double bestScore = -1e9;
            for (int i = 0; i < players.size(); i++) {
                Player p = players.get(i);
                double s = 0;
                if (p.hasBall) s += 1000;
                s -= p.pos.distanceTo(ball.pos);
                s -= p.state == PlayerState.STUNNED || p.state == PlayerState.TACKLED ? 20 : 0;
                if (s > bestScore) {
                    bestScore = s;
                    best = p;
                    controlledIndex = i;
                }
            }
        }

        public boolean hasPossession(Ball ball) {
            for (Player p : players) {
                if (p.hasBall) return true;
            }
            return false;
        }

        public Player findCarrier(Ball ball) {
            for (Player p : players) {
                if (p.hasBall) return p;
            }
            return null;
        }

        public void scoreTry() {
            score += 5; // try value
            // Optional: simple conversion
            score += 2;
        }

        public Player closestTo(Vec2 pos) {
            Player best = null;
            double bestD = Double.MAX_VALUE;
            for (Player p : players) {
                double d = p.pos.distanceTo(pos);
                if (d < bestD) { bestD = d; best = p; }
            }
            return best;
        }

        public Player findBestPassTarget(Player passer, Ball ball, Pitch pitch) {
            Player best = null;
            double bestScore = -1e9;
            for (Player t : players) {
                if (t == passer) continue;
                // Favor forward passes relative to team's direction,
                // but rugby forward pass is illegal; we interpret "forward" by absolute field and assume arcade style lateral/backwards.
                // We'll prioritize lateral/backwards to mimic rugby style.
                Vec2 to = t.pos.sub(passer.pos);
                boolean backwardOk = (side == Side.LEFT) ? (to.x <= 0.2) : (to.x >= -0.2);
                if (!backwardOk) continue;

                double dist = to.length();
                if (dist < 3 || dist > 28) continue;

                // Score factors: distance optimum, openness (distance to nearest opponent)
                double distScore = -Math.abs(dist - 12);
                double open = opponent.closestTo(t.pos).pos.distanceTo(t.pos);
                double openScore = open;
                double angleScore = passer.heading.dot(to.normalized()) * 2.0;

                double s = distScore + openScore + angleScore;
                if (s > bestScore) {
                    bestScore = s;
                    best = t;
                }
            }
            return best;
        }

        public Player findBestTackleTarget(Player tackler) {
            // Opponent closest in front within cone
            Player best = null;
            double bestScore = -1e9;
            for (Player opp : opponent.players) {
                Vec2 to = opp.pos.sub(tackler.pos);
                double dist = to.length();
                if (dist > 6.0) continue;
                double angle = tackler.heading.dot(to.normalized());
                if (angle < -0.2) continue; // behind
                double s = (1.0 - dist / 6.0) + angle * 0.6 + (opp.hasBall ? 1.5 : 0);
                if (s > bestScore) {
                    bestScore = s;
                    best = opp;
                }
            }
            return best;
        }
    }

    // Ball class with 3D height component for simple arcs.
    static class Ball {
        Vec2 pos;
        Vec2 vel = new Vec2(0, 0);
        double height = 0;
        double zVel = 0;
        Player owner = null;

        public Ball(Vec2 pos) {
            this.pos = pos.copy();
        }

        public void attachTo(Player p) {
            owner = p;
            p.hasBall = true;
            // Attach ball slightly forward of the player
            pos = p.pos.add(p.heading.scale(0.6));
            vel.set(0, 0);
            height = 0.4;
            zVel = 0;
        }

        public void detach() {
            if (owner != null) {
                owner.hasBall = false;
                owner = null;
            }
        }

        double drawX() { return pos.x - 0.45; }
        double drawY() { return pos.y - 0.22 - height * 0.12; }

        @Override
        public String toString() {
            return "Ball{" + "pos=" + pos + ", vel=" + vel + ", h=" + String.format("%.2f", height) + '}';
        }
    }

    // Simple event log with fixed capacity.
    static class EventLog {
        ArrayDeque<String> lines = new ArrayDeque<>();
        int capacity = 9;

        void add(String s) {
            lines.addFirst(s);
            while (lines.size() > capacity) lines.removeLast();
            System.out.println(s);
        }

        void render(Graphics2D g2, int x, int y, int w, int h) {
            g2.setColor(new Color(0, 0, 0, 130));
            g2.fillRoundRect(x, y, w, h, 10, 10);
            g2.setColor(Color.white);
            g2.drawRoundRect(x, y, w, h, 10, 10);

            int yy = y + 24;
            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            for (String s : lines) {
                g2.setColor(Color.white);
                g2.drawString(s, x + 10, yy);
                yy += 18;
            }
            g2.setColor(Color.gray);
            g2.drawString("Events", x + 10, y + 18);
        }
    }

    // Replay buffer stores a short rolling buffer of frames for a simple replay.
    static class ReplayBuffer {
        static class Frame {
            double time;
            List<Vec2> teamAPos;
            List<Vec2> teamBPos;
            Vec2 ballPos;
            double ballHeight;
            String marker;

            Frame(double time, List<Vec2> a, List<Vec2> b, Vec2 ballPos, double ballHeight) {
                this.time = time;
                this.teamAPos = a;
                this.teamBPos = b;
                this.ballPos = ballPos;
                this.ballHeight = ballHeight;
            }
        }

        ArrayDeque<Frame> buffer = new ArrayDeque<>();
        double t = 0;

        void capture(GamePanel panel, double dt) {
            t += dt;
            // Capture every frame, limit to ~10 seconds at 60 fps => ~600 frames.
            if (buffer.size() > 600) buffer.removeFirst();
            buffer.addLast(snapshot(panel));
        }

        Frame snapshot(GamePanel panel) {
            List<Vec2> a = new ArrayList<>();
            for (Player p : panel.teamA.players) a.add(p.pos.copy());
            List<Vec2> b = new ArrayList<>();
            for (Player p : panel.teamB.players) b.add(p.pos.copy());
            return new Frame(t, a, b, panel.ball.pos.copy(), panel.ball.height);
        }

        void markEvent(String s) {
            if (!buffer.isEmpty()) {
                buffer.getLast().marker = s;
            }
        }

        void clear() {
            buffer.clear();
            t = 0;
        }
    }

    // Particles for simple effects: dust, trails, confetti, tackle impact, etc.
    static class ParticleSystem {
        static class Particle {
            Vec2 pos;
            Vec2 vel;
            double life;
            double maxLife;
            Color color;
            double size;
            boolean over; // render over players if true

            Particle(Vec2 pos, Vec2 vel, double life, Color color, double size, boolean over) {
                this.pos = pos.copy();
                this.vel = vel.copy();
                this.life = life;
                this.maxLife = life;
                this.color = color;
                this.size = size;
                this.over = over;
            }

            void update(double dt) {
                pos = pos.add(vel.scale(dt));
                life -= dt;
                vel = vel.scale(Math.max(0, 1 - dt * 2.5));
            }

            void draw(Graphics2D g2) {
                float a = (float) Math.max(0, Math.min(1, life / maxLife));
                Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (a * 255));
                g2.setColor(c);
                g2.fill(new java.awt.geom.Ellipse2D.Double(pos.x - size / 2, pos.y - size / 2, size, size));
            }
        }

        List<Particle> under = new ArrayList<>();
        List<Particle> over = new ArrayList<>();
        Random rng = new Random();

        void update(double dt) {
            for (int i = under.size() - 1; i >= 0; i--) {
                Particle p = under.get(i);
                p.update(dt);
                if (p.life <= 0) under.remove(i);
            }
            for (int i = over.size() - 1; i >= 0; i--) {
                Particle p = over.get(i);
                p.update(dt);
                if (p.life <= 0) over.remove(i);
            }
        }

        void renderUnder(Graphics2D g2) {
            for (Particle p : under) p.draw(g2);
        }

        void renderOver(Graphics2D g2) {
            for (Particle p : over) p.draw(g2);
        }

        void spawnTrail(Vec2 pos, Color color) {
            for (int i = 0; i < 1; i++) {
                Vec2 v = new Vec2((rng.nextDouble() - 0.5) * 0.5, (rng.nextDouble() - 0.5) * 0.5);
                Particle p = new Particle(pos.add(new Vec2((rng.nextDouble() - 0.5) * 0.2, (rng.nextDouble() - 0.5) * 0.2)), v, 0.4 + rng.nextDouble() * 0.3, new Color(color.getRed(), color.getGreen(), color.getBlue()), 0.25 + rng.nextDouble() * 0.25, false);
                under.add(p);
            }
        }

        void spawnPassPuff(Vec2 pos) {
            for (int i = 0; i < 8; i++) {
                Vec2 v = new Vec2((rng.nextDouble() - 0.5) * 2.0, (rng.nextDouble() - 0.5) * 2.0);
                under.add(new Particle(pos, v, 0.6 + rng.nextDouble() * 0.3, new Color(220, 220, 220), 0.2 + rng.nextDouble() * 0.2, false));
            }
        }

        void spawnKickFlash(Vec2 pos) {
            for (int i = 0; i < 16; i++) {
                double a = rng.nextDouble() * Math.PI * 2;
                Vec2 v = new Vec2(Math.cos(a), Math.sin(a)).scale(3 + rng.nextDouble() * 3);
                over.add(new Particle(pos, v, 0.5 + rng.nextDouble() * 0.3, Color.yellow, 0.22 + rng.nextDouble() * 0.2, true));
            }
        }

        void spawnTackleImpact(Vec2 pos) {
            for (int i = 0; i < 14; i++) {
                double a = rng.nextDouble() * Math.PI * 2;
                Vec2 v = new Vec2(Math.cos(a), Math.sin(a)).scale(2 + rng.nextDouble() * 2);
                over.add(new Particle(pos, v, 0.6 + rng.nextDouble() * 0.3, new Color(255, 140, 100), 0.3 + rng.nextDouble() * 0.2, true));
            }
        }

        void spawnTackleWhiff(Vec2 pos) {
            for (int i = 0; i < 10; i++) {
                Vec2 v = new Vec2((rng.nextDouble() - 0.5) * 1.5, (rng.nextDouble() - 0.5) * 1.5);
                over.add(new Particle(pos, v, 0.5 + rng.nextDouble() * 0.2, new Color(180, 180, 255), 0.25, true));
            }
        }

        void spawnBounceDust(Vec2 pos) {
            for (int i = 0; i < 6; i++) {
                Vec2 v = new Vec2((rng.nextDouble() - 0.5) * 1.0, (rng.nextDouble() - 0.5) * 1.0);
                under.add(new Particle(pos, v, 0.5 + rng.nextDouble() * 0.3, new Color(200, 200, 200), 0.2 + rng.nextDouble() * 0.2, false));
            }
        }

        void spawnConfetti(Vec2 pos, Color teamColor) {
            for (int i = 0; i < 40; i++) {
                double a = rng.nextDouble() * Math.PI * 2;
                Vec2 v = new Vec2(Math.cos(a), Math.sin(a)).scale(2 + rng.nextDouble() * 2);
                Color c = rng.nextBoolean() ? teamColor : Color.white;
                over.add(new Particle(pos, v, 1.2 + rng.nextDouble() * 0.6, c, 0.3 + rng.nextDouble() * 0.3, true));
            }
        }

        void clear() {
            under.clear();
            over.clear();
        }
    }

    // Simple physics helpers if needed in the future.
    static class Physics {
        // Placeholder for future extensions (scrums, rucks).
    }

    // Input state storing pressed keys and one-shot presses.
    static class InputState {
        private final boolean[] down = new boolean[512];
        private final boolean[] pressed = new boolean[512];

        void setKey(int code, boolean isDown) {
            if (code < 0 || code >= down.length) return;
            if (isDown && !down[code]) {
                pressed[code] = true;
            }
            down[code] = isDown;
        }

        boolean isDown(int code) {
            if (code < 0 || code >= down.length) return false;
            return down[code];
        }

        boolean consumePressed(int code) {
            if (code < 0 || code >= down.length) return false;
            boolean p = pressed[code];
            pressed[code] = false;
            return p;
        }
    }

    // Basic vector math utility
    static class Vec2 {
        double x, y;

        Vec2(double x, double y) { this.x = x; this.y = y; }
        Vec2 copy() { return new Vec2(x, y); }
        Vec2 add(Vec2 o) { return new Vec2(x + o.x, y + o.y); }
        Vec2 sub(Vec2 o) { return new Vec2(x - o.x, y - o.y); }
        Vec2 scale(double s) { return new Vec2(x * s, y * s); }
        double dot(Vec2 o) { return x * o.x + y * o.y; }
        double length() { return Math.sqrt(x * x + y * y); }
        double lengthSq() { return x * x + y * y; }
        Vec2 normalized() {
            double l = length();
            if (l < 1e-6) return new Vec2(0, 0);
            return new Vec2(x / l, y / l);
        }
        void set(double nx, double ny) { x = nx; y = ny; }
        boolean equals(Vec2 o) { return Math.abs(x - o.x) < 1e-9 && Math.abs(y - o.y) < 1e-9; }
        double distanceTo(Vec2 o) { return sub(o).length(); }

        @Override
        public String toString() {
            return String.format("(%.2f, %.2f)", x, y);
        }
    }

    // Enumerations for clarity
    enum Side { LEFT, RIGHT }
    enum PlayerState { IDLE, TACKLING, TACKLED, STUNNED }
    enum GameState { KICKOFF, PLAYING, TRY_SCORED, PAUSED }
    enum Role { FORWARD, CENTER, BACK, FLY_HALF }

    // Utility clamp and index helpers
    static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    static int clampIndex(int v, int min, int max) {
        if (v < min) return min;
        return Math.min(v, max);
    }

    // Minimal Arc2D implementation using Java2D shape if missing; using java.awt.geom.Arc2D
    static class Arc2D extends java.awt.geom.Arc2D.Double {
        public Arc2D() { super(); }
        public Arc2D(double x, double y, double w, double h, double start, double extent, int type) {
            super(x, y, w, h, start, extent, type);
        }
    }
}