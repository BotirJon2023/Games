import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RugbyGameSimulation2 extends JFrame {

    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;

    public RugbyGameSimulation2() {
        super("Rugby Game Simulation 2 - Single File (700+ lines)");
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
        SwingUtilities.invokeLater(RugbyGameSimulation2::new);
    }

    // The main game panel encapsulating the loop, render, input, and game logic.
    static class GamePanel extends JPanel implements ActionListener, ComponentListener, KeyListener, MouseListener, MouseMotionListener {

        // Timing
        private final Timer timer;
        private long lastNano;
        private double accumulator;

        // World systems
        private final Random rng = new Random(1337);
        private final Pitch pitch = new Pitch();
        private final Camera camera = new Camera();
        private final ParticleSystem particles = new ParticleSystem();
        private final EventLog eventLog = new EventLog();
        private final Weather weather = new Weather();
        private final SetPieces setPieces = new SetPieces();
        private final Replay replay = new Replay();

        // Input and state
        private final Input input = new Input();

        // Teams and ball
        private Team home;
        private Team away;
        private Ball ball;

        // Game state
        private GamePhase phase = GamePhase.PRE_KICKOFF;
        private double phaseTimer = 0;
        private double matchClock = 0; // seconds

        // Options / toggles
        private boolean paused = false;
        private boolean debug = false;
        private boolean showLabels = true;
        private boolean showShadows = true;
        private boolean showTrails = true;
        private boolean staminaEnabled = true;
        private boolean followBall = false;

        // Visual/UI
        private final Font fontUI = new Font("SansSerif", Font.BOLD, 16);
        private final Font fontBig = new Font("SansSerif", Font.BOLD, 36);
        private final Color grassA = new Color(82, 155, 80);
        private final Color grassB = new Color(74, 143, 72);

        // Camera shake parameters
        private double shakeTime = 0;
        private double shakeIntensity = 0;

        // Viewport tracking
        private int viewW = WINDOW_WIDTH;
        private int viewH = WINDOW_HEIGHT;

        // Kickoff sides
        private Team kickingTeam;
        private Team receivingTeam;

        // Mouse (not crucial, but kept)
        private Point mouse = new Point();

        public GamePanel() {
            setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
            setBackground(new Color(22, 22, 22));
            setFocusable(true);
            addComponentListener(this);
            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);
            timer = new Timer(1000 / 60, this);

            initMatch();
        }

        private void initMatch() {
            // Create teams with kit colors
            home = new Team("Emerald Eagles", Side.LEFT, new Color(30, 160, 70));
            away = new Team("Crimson Cobras", Side.RIGHT, new Color(190, 40, 40));
            home.opponent = away;
            away.opponent = home;

            createTeam(home, Side.LEFT);
            createTeam(away, Side.RIGHT);

            ball = new Ball(new Vec2(pitch.length * 0.5, pitch.width * 0.5));

            kickingTeam = home;
            receivingTeam = away;

            placeForKickoff();
            weather.randomize(rng);
            eventLog.add("Welcome! " + home.name + " vs " + away.name + " - Weather: " + weather.describe());

            // Reset meta systems
            replay.clear();
            particles.clear();
            matchClock = 0;
        }

        private void createTeam(Team team, Side side) {
            team.players.clear();
            // We'll spawn 10 players per team for more density.
            int count = 10;
            double baseX = (side == Side.LEFT) ? pitch.length * 0.25 : pitch.length * 0.75;
            double baseY = pitch.width * 0.5;
            double spacingY = 6.0;

            for (int i = 0; i < count; i++) {
                double y = baseY + (i - (count - 1) / 2.0) * spacingY + rng.nextDouble() * 2 - 1;
                double x = baseX + rng.nextDouble() * 3 - 1.5;
                Player p = new Player(team, "P" + (i + 1), new Vec2(x, y));
                p.number = i + 1;
                p.role = (i == 0) ? Role.FLY_HALF : (i < 4) ? Role.BACK : (i < 7) ? Role.CENTER : Role.FORWARD;

                p.maxSpeed = 6.7 + rng.nextDouble(); // m/s
                p.accel = 16 + rng.nextDouble() * 6; // m/s^2
                p.agility = 0.8 + rng.nextDouble() * 0.2;
                p.stamina = 100;
                p.passing = 0.6 + rng.nextDouble() * 0.3;
                p.kicking = 0.55 + rng.nextDouble() * 0.35;
                p.tackling = 0.55 + rng.nextDouble() * 0.35;

                team.players.add(p);
            }
            team.controlledIndex = 0;
        }

        private void placeForKickoff() {
            ball.owner = null;
            ball.pos.set(pitch.length * 0.5, pitch.width * 0.5);
            ball.vel.set(0, 0);
            ball.height = 0;
            ball.zVel = 0;

            // Kicker stands near center
            Player kicker = kickingTeam.players.get(0);
            kicker.pos.set(ball.pos.x - (kickingTeam.side == Side.LEFT ? 2.0 : -2.0), ball.pos.y);
            kicker.vel.set(0, 0);
            kicker.heading = (kickingTeam.side == Side.LEFT ? new Vec2(1, 0) : new Vec2(-1, 0));
            kicker.hasBall = true;
            ball.attachTo(kicker, 0.5);

            // Others spread behind 10m line
            double lineX = pitch.length * 0.5 - (kickingTeam.side == Side.LEFT ? 6.0 : -6.0);
            double spacingY = 5.5;
            for (int i = 1; i < kickingTeam.players.size(); i++) {
                double y = pitch.width * 0.5 + (i - (kickingTeam.players.size() - 1) / 2.0) * spacingY;
                kickingTeam.players.get(i).pos.set(lineX + rng.nextDouble() * 1.5 - 0.75, y + rng.nextDouble() * 2 - 1.0);
                kickingTeam.players.get(i).vel.set(0, 0);
                kickingTeam.players.get(i).hasBall = false;
                kickingTeam.players.get(i).state = PlayerState.IDLE;
            }

            // Defenders aligned on their side near 10m
            double defX = pitch.length * 0.5 + (receivingTeam.side == Side.LEFT ? -8.0 : 8.0);
            for (int i = 0; i < receivingTeam.players.size(); i++) {
                double y = pitch.width * 0.5 + (i - (receivingTeam.players.size() - 1) / 2.0) * spacingY;
                receivingTeam.players.get(i).pos.set(defX + rng.nextDouble() * 1.5 - 0.75, y + rng.nextDouble() * 2 - 1.0);
                receivingTeam.players.get(i).vel.set(0, 0);
                receivingTeam.players.get(i).hasBall = false;
                receivingTeam.players.get(i).state = PlayerState.IDLE;
            }

            phase = GamePhase.KICKOFF;
            phaseTimer = 0;
            eventLog.add(kickingTeam.name + " to kick off.");
        }

        public void start() {
            lastNano = System.nanoTime();
            accumulator = 0;
            timer.start();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            double dt = (now - lastNano) / 1_000_000_000.0;
            lastNano = now;

            if (dt > 0.1) dt = 0.1;

            if (!paused) {
                step(dt);
            }
            repaint();
        }

        private void step(double dt) {
            // Capture replay frame
            replay.capture(this, dt);

            // Update timers
            phaseTimer += dt;
            if (phase == GamePhase.PLAY || phase == GamePhase.SCRUM || phase == GamePhase.LINEOUT || phase == GamePhase.CONVERSION) {
                matchClock += dt;
            }

            shakeTime = Math.max(0, shakeTime - dt);
            if (shakeTime <= 0) shakeIntensity = 0;

            // Input
            processInput(dt);

            // AI and Systems
            runAI(dt);

            // Physics
            updatePlayers(dt);
            updateBall(dt);

            // Set-piece management
            handleSetPieces(dt);

            // Collisions between players (lite)
            separatePlayers();

            // Camera
            updateCamera(dt);

            // Particles (includes rain)
            particles.update(dt);
            weather.update(dt);
            spawnWeatherParticles(dt);

            // Phase transitions
            handlePhase(dt);
        }

        private void processInput(double dt) {
            Team user = home;
            Player ctrl = user.getControlled();

            if (input.consumePressed(KeyEvent.VK_SPACE)) {
                user.selectBestController(ball);
                eventLog.add("Control: " + user.getControlled().nameID());
            }
            if (input.consumePressed(KeyEvent.VK_P)) {
                paused = !paused;
                eventLog.add(paused ? "Paused" : "Resumed");
            }
            if (input.consumePressed(KeyEvent.VK_F1)) debug = !debug;
            if (input.consumePressed(KeyEvent.VK_F2)) showLabels = !showLabels;
            if (input.consumePressed(KeyEvent.VK_F3)) showTrails = !showTrails;
            if (input.consumePressed(KeyEvent.VK_F4)) showShadows = !showShadows;
            if (input.consumePressed(KeyEvent.VK_F5)) {
                staminaEnabled = !staminaEnabled;
                eventLog.add("Stamina " + (staminaEnabled ? "On" : "Off"));
            }
            if (input.consumePressed(KeyEvent.VK_C)) {
                followBall = !followBall;
                eventLog.add("Camera: " + (followBall ? "Follow Ball" : "Follow Player"));
            }
            if (input.consumePressed(KeyEvent.VK_R)) {
                eventLog.add("Match reset.");
                initMatch();
                return;
            }

            if (ctrl != null) {
                // Movement
                double mx = 0, my = 0;
                if (input.isDown(KeyEvent.VK_W) || input.isDown(KeyEvent.VK_UP)) my -= 1;
                if (input.isDown(KeyEvent.VK_S) || input.isDown(KeyEvent.VK_DOWN)) my += 1;
                if (input.isDown(KeyEvent.VK_A) || input.isDown(KeyEvent.VK_LEFT)) mx -= 1;
                if (input.isDown(KeyEvent.VK_D) || input.isDown(KeyEvent.VK_RIGHT)) mx += 1;

                boolean sprint = input.isDown(KeyEvent.VK_SHIFT);

                Vec2 dir = new Vec2(mx, my).normalized();
                double targetSpeed = ctrl.maxSpeed * (sprint ? 1.1 : 0.85);
                if (!staminaEnabled) targetSpeed = ctrl.maxSpeed;

                if (staminaEnabled) {
                    if (sprint && dir.lengthSq() > 0.02) ctrl.stamina = Math.max(0, ctrl.stamina - dt * 10);
                    else ctrl.stamina = Math.min(100, ctrl.stamina + dt * 6);

                    if (ctrl.stamina < 20) targetSpeed *= 0.85;
                }

                if (ctrl.state == PlayerState.IDLE || ctrl.state == PlayerState.RUN) {
                    ctrl.seek(dir, targetSpeed, dt);
                    if (ctrl.vel.lengthSq() > 0.05) ctrl.state = PlayerState.RUN;
                    else ctrl.state = PlayerState.IDLE;
                }

                // Passing
                if (input.consumePressed(KeyEvent.VK_Q)) {
                    if (ctrl.hasBall) {
                        Player tgt = user.bestPass(ctrl, ball, pitch);
                        if (tgt != null) {
                            doPass(ctrl, tgt);
                        } else {
                            eventLog.add("No pass target.");
                        }
                    } else {
                        eventLog.add("No ball to pass.");
                    }
                }

                // Kick
                if (input.consumePressed(KeyEvent.VK_E)) {
                    if (ctrl.hasBall) {
                        doKick(ctrl);
                    } else {
                        eventLog.add("No ball to kick.");
                    }
                }

                // Tackle
                if (input.consumePressed(KeyEvent.VK_F)) {
                    tryTackle(ctrl);
                }

                // Visual trail when running fast
                if (showTrails && ctrl.vel.lengthSq() > 8) {
                    particles.spawnFootTrail(ctrl.pos, ctrl.team.color);
                }
            }
        }

        private void runAI(double dt) {
            // AI for the non-controlled home players and all away players
            aiForTeam(home, dt, true); // controlled one is skipped inside
            aiForTeam(away, dt, true);
        }

        private void aiForTeam(Team team, double dt, boolean enabled) {
            if (!enabled) return;

            boolean ourPossession = team.hasBall(ball);
            Player carrier = team.carrier(ball);

            for (int i = 0; i < team.players.size(); i++) {
                Player p = team.players.get(i);

                boolean userControlled = (team == home && i == team.controlledIndex);
                if (userControlled) continue; // let user control

                // Recovery from stunned/tackled
                if (p.state == PlayerState.TACKLED || p.state == PlayerState.STUNNED) {
                    p.recoverTimer -= dt;
                    if (p.recoverTimer <= 0) p.state = PlayerState.IDLE;
                    continue;
                }

                // Cooldowns
                p.tackleCooldown = Math.max(0, p.tackleCooldown - dt);

                // Set-piece override: players lock positions (handled in setPieces)
                if (phase == GamePhase.SCRUM || phase == GamePhase.LINEOUT) {
                    // setPieces handles their velocities; AIs avoid interfering
                    continue;
                }

                // If carrying ball
                if (p.hasBall) {
                    Vec2 goal = (team.side == Side.LEFT) ? new Vec2(pitch.length - 1.5, pitch.width * 0.5) : new Vec2(1.5, pitch.width * 0.5);

                    // Strafe a little to be less predictable
                    double sideWiggle = Math.sin(phaseTimer * 1.3 + i) * 1.2;
                    Vec2 target = new Vec2(goal.x, clamp(p.pos.y + sideWiggle, 3, pitch.width - 3));
                    p.seek(target.sub(p.pos).normalized(), p.maxSpeed * 0.95, dt);

                    // Pass if defenders crowd
                    double threat = team.opponent.distanceToNearest(p.pos);
                    if (threat < 4.0 && rng.nextDouble() < 0.02) {
                        Player best = team.bestPass(p, ball, pitch);
                        if (best != null) doPass(p, best);
                    }

                    // Situational kick near 22 under pressure
                    double line22 = (team.side == Side.LEFT) ? pitch.length * 0.78 : pitch.length * 0.22;
                    boolean near22 = (team.side == Side.LEFT) ? (p.pos.x > line22) : (p.pos.x < line22);
                    if (near22 && threat < 4 && rng.nextDouble() < 0.008) {
                        doKick(p);
                    }
                    continue;
                }

                if (ourPossession) {
                    // Support runs
                    if (carrier != null) {
                        // Position slightly behind and offset
                        double dx = (team.side == Side.LEFT) ? -3.0 : 3.0;
                        double dy = ((i % 2 == 0) ? -1 : 1) * 3.0;
                        Vec2 desired = carrier.pos.add(new Vec2(dx + rng.nextGaussian() * 0.6, dy + rng.nextGaussian() * 0.6));
                        p.seek(desired.sub(p.pos).normalized(), p.maxSpeed * 0.9, dt);
                        p.avoidMates(team, dt);
                    }
                } else {
                    // Defend: mark or chase ball
                    Player oppCarrier = team.opponent.carrier(ball);
                    if (oppCarrier != null) {
                        // Move to cut angle
                        Vec2 ahead = oppCarrier.pos.add(oppCarrier.vel.scale(0.4));
                        p.seek(ahead.sub(p.pos).normalized(), p.maxSpeed, dt);
                        // Auto tackle if close
                        if (p.pos.distanceTo(oppCarrier.pos) < 3.3 && p.tackleCooldown <= 0) {
                            tryTackle(p);
                        }
                    } else {
                        // Loose ball
                        if (ball.owner == null) {
                            p.seek(ball.pos.sub(p.pos).normalized(), p.maxSpeed, dt);
                        } else {
                            // Mark nearest
                            Player mark = team.opponent.nearestTo(p.pos);
                            if (mark != null) {
                                double gap = 3.0;
                                Vec2 pos = mark.pos.add(new Vec2(team.side == Side.LEFT ? -gap : gap, 0));
                                p.seek(pos.sub(p.pos).normalized(), p.maxSpeed * 0.85, dt);
                            }
                        }
                    }
                }
            }
        }

        private void updatePlayers(double dt) {
            for (Team t : listTeams()) {
                for (Player p : t.players) {
                    // Integration
                    p.pos = p.pos.add(p.vel.scale(dt));
                    // Drag
                    p.vel = p.vel.scale(Math.max(0, 1 - 2.2 * dt));
                    // Boundaries
                    Vec2 cl = pitch.clamp(p.pos, 1.0);
                    if (!cl.equals(p.pos)) {
                        p.pos = cl;
                        p.vel.set(0, 0);
                    }
                    // Animation
                    p.anim += p.vel.length() * dt * 0.5;
                    p.anim %= Math.PI * 2;

                    // Carry
                    if (p.hasBall) {
                        ball.attachTo(p, 0.55);
                    }
                }
            }
        }

        private void updateBall(double dt) {
            if (ball.owner == null) {
                // Apply gravity
                ball.zVel -= 9.81 * dt;
                ball.height += ball.zVel * dt;
                if (ball.height < 0) {
                    ball.height = 0;
                    if (Math.abs(ball.zVel) > 3) {
                        ball.zVel = -ball.zVel * 0.35;
                        particles.spawnBounce(ball.pos);
                    } else {
                        ball.zVel = 0;
                    }
                }

                // Air drag
                ball.vel = ball.vel.scale(Math.max(0, 1 - 0.55 * dt));

                // Wind from weather
                ball.vel = ball.vel.add(weather.wind.scale(dt * weather.windBallInfluence()));

                // Integrate
                ball.pos = ball.pos.add(ball.vel.scale(dt));

                // Boundary check
                Vec2 cl = pitch.clamp(ball.pos, 0.5);
                if (!cl.equals(ball.pos)) {
                    // Went out: lineout for opponent from side
                    Team throwTeam = (ball.lastTouchedBy != null) ? ball.lastTouchedBy.team.opponent : (ball.pos.x < pitch.length * 0.5 ? away : home);
                    setPieces.startLineout(throwTeam, ball.pos, pitch, this);
                    eventLog.add("Ball out! Lineout to " + throwTeam.name + ".");
                    return;
                }

                // Attempt pickup if on ground
                if (ball.height < 0.25) {
                    Player near = nearestPlayer(ball.pos, 1.1);
                    if (near != null) {
                        near.hasBall = true;
                        ball.attachTo(near, 0.5);
                        ball.lastTouchedBy = near;
                        eventLog.add(near.nameID() + " picks up.");
                    }
                }
            } else {
                // Keep owner within field - already done in players
            }

            // Try scoring
            checkTry();
        }

        private void handleSetPieces(double dt) {
            // Only process if we are in a set piece phase
            if (phase == GamePhase.SCRUM) {
                setPieces.updateScrum(dt, this);
            } else if (phase == GamePhase.LINEOUT) {
                setPieces.updateLineout(dt, this);
            }
        }

        private void separatePlayers() {
            List<Player> all = new ArrayList<>();
            all.addAll(home.players);
            all.addAll(away.players);
            for (int i = 0; i < all.size(); i++) {
                for (int j = i + 1; j < all.size(); j++) {
                    Player a = all.get(i);
                    Player b = all.get(j);
                    double r = 0.85; // radius sum
                    Vec2 d = b.pos.sub(a.pos);
                    double dist = d.length();
                    if (dist > 0 && dist < r) {
                        Vec2 push = d.normalized().scale((r - dist) * 0.5);
                        a.pos = a.pos.add(push.scale(-1));
                        b.pos = b.pos.add(push);
                        a.vel = a.vel.scale(0.85);
                        b.vel = b.vel.scale(0.85);
                    }
                }
            }
        }

        private void updateCamera(double dt) {
            // Target is ball or controlled
            Vec2 target;
            if (followBall || ball.owner == null) target = ball.pos;
            else target = home.getControlled().pos;

            camera.target.set(target.x, target.y);
            // Shake offsets for visuals
            Vec2 shake = new Vec2(0, 0);
            if (shakeTime > 0 && shakeIntensity > 0) {
                double s = shakeIntensity * (shakeTime);
                shake.x = (rng.nextDouble() - 0.5) * s;
                shake.y = (rng.nextDouble() - 0.5) * s;
            }
            camera.update(dt, pitch, viewW, viewH, shake);
        }

        private void handlePhase(double dt) {
            switch (phase) {
                case PRE_KICKOFF:
                    // Should not remain
                    break;
                case KICKOFF:
                    if (phaseTimer > 1.1) {
                        Player kicker = kickingTeam.getControlled() != null ? kickingTeam.getControlled() : kickingTeam.players.get(0);
                        if (kicker.hasBall) {
                            // Kick
                            doKick(kicker);
                            phase = GamePhase.PLAY;
                            phaseTimer = 0;
                            eventLog.add("Kickoff taken by " + kicker.nameID());
                        } else {
                            phase = GamePhase.PLAY;
                            phaseTimer = 0;
                        }
                    }
                    break;
                case PLAY:
                    // If heavy collision leads to knock-on (simple chance) -> scrum
                    // We'll trigger scrums occasionally on loose ball near midfield
                    if (ball.owner == null && rng.nextDouble() < 0.001 && ball.pos.x > 20 && ball.pos.x < pitch.length - 20) {
                        Team feed = (rng.nextBoolean() ? home : away);
                        setPieces.startScrum(feed, ball.pos, this);
                        eventLog.add("Scrum called. Feed: " + feed.name);
                    }
                    break;
                case SCRUM:
                case LINEOUT:
                    // Set pieces will internally transition back to PLAY after completion
                    break;
                case TRY:
                    if (phaseTimer > 3.0) {
                        // After try, restart with kickoff
                        kickingTeam = receivingTeam;     // conceding team kicks
                        receivingTeam = (kickingTeam == home) ? away : home;
                        placeForKickoff();
                    }
                    break;
                case CONVERSION:
                    // For simplicity automatically award 2 points directly on try in this arcade variant,
                    // but we keep this phase in case of future expansion.
                    phase = GamePhase.TRY; // skip to TRY display
                    phaseTimer = 0;
                    break;
                case FULLTIME:
                    // Not implemented; match keeps running
                    break;
                case PAUSED:
                    // handled by paused boolean; not used as phase
                    break;
            }
        }

        private void tryTackle(Player tackler) {
            Player victim = tackler.team.opponent.bestTackleTarget(tackler);
            if (victim == null) {
                eventLog.add("No tackle target.");
                return;
            }
            double dist = tackler.pos.distanceTo(victim.pos);
            if (dist > 3.5) {
                eventLog.add("Too far to tackle.");
                return;
            }

            double att = tackler.tackling * (1.0 + tackler.vel.length() * 0.08);
            double def = 0.8 + victim.vel.length() * 0.08 + rng.nextDouble() * 0.35;
            boolean success = att > def;

            tackler.state = PlayerState.TACKLING;
            tackler.tackleCooldown = 0.7;
            if (success) {
                victim.state = PlayerState.TACKLED;
                victim.recoverTimer = 1.3;
                victim.vel = victim.vel.scale(0.2);
                if (victim.hasBall) {
                    victim.hasBall = false;
                    ball.detach();
                    ball.pos = victim.pos.copy();
                    ball.height = 0.4;
                    ball.vel = victim.heading.scale(2.2).add(new Vec2(rng.nextDouble() - 0.5, rng.nextDouble() - 0.5).scale(2.5));
                    ball.lastTouchedBy = tackler; // tackler last contact
                    eventLog.add(tackler.nameID() + " tackles " + victim.nameID() + "! Ball loose!");
                } else {
                    eventLog.add(tackler.nameID() + " tackles " + victim.nameID());
                }
                particles.impact(victim.pos);
                shake(0.6, 0.5);
            } else {
                tackler.state = PlayerState.STUNNED;
                tackler.recoverTimer = 0.7;
                eventLog.add(tackler.nameID() + " misses tackle on " + victim.nameID());
                particles.whiff(tackler.pos);
            }
        }

        private void doPass(Player passer, Player target) {
            Vec2 delta = target.pos.sub(passer.pos);
            double dist = delta.length();
            double speed = 14 + passer.passing * 6.0;
            Vec2 lead = target.vel.scale(Math.min(dist / speed, 0.6));
            Vec2 aim = delta.add(lead).normalized();
            Vec2 v = aim.scale(speed);

            ball.detach();
            ball.pos = passer.pos.add(aim.scale(0.6));
            ball.vel = v;
            ball.height = 0.7;
            ball.zVel = 0;
            ball.lastTouchedBy = passer;
            passer.hasBall = false;

            eventLog.add(passer.nameID() + " passes to " + target.nameID());
            particles.passPuff(passer.pos);
        }

        private void doKick(Player kicker) {
            Vec2 dir = kicker.heading.lengthSq() > 0.01 ? kicker.heading.normalized()
                    : (kicker.team.side == Side.LEFT ? new Vec2(1, 0) : new Vec2(-1, 0));
            double power = 18 + kicker.kicking * 8;
            Vec2 v = dir.scale(power * 0.85).add(weather.wind.scale(0.6)); // initial wind bias
            ball.detach();
            ball.pos = kicker.pos.add(dir.scale(0.85));
            ball.vel = v;
            ball.height = 1.5;
            ball.zVel = 6 + kicker.kicking * 4;
            ball.lastTouchedBy = kicker;
            kicker.hasBall = false;

            eventLog.add(kicker.nameID() + " kicks!");
            particles.kickFlash(kicker.pos);
            shake(0.3, 0.35);
        }

        private void checkTry() {
            Player aCarrier = home.carrier(ball);
            Player bCarrier = away.carrier(ball);

            if (aCarrier != null) {
                if (aCarrier.pos.x > pitch.length - pitch.tryDepth) {
                    awardTry(home, aCarrier);
                }
            } else if (bCarrier != null) {
                if (bCarrier.pos.x < pitch.tryDepth) {
                    awardTry(away, bCarrier);
                }
            }
        }

        private void awardTry(Team team, Player scorer) {
            team.score += 5;
            // quick conversion as bonus
            team.score += 2;
            eventLog.add("TRY! " + team.name + " by " + scorer.nameID() + ". Score: " + scoreLine());
            for (int i = 0; i < 35; i++) particles.confetti(scorer.pos, team.color);
            replay.mark("TRY by " + team.name + " (" + scorer.nameID() + ")");
            ball.detach();
            receivingTeam = team;
            kickingTeam = team.opponent;

            phase = GamePhase.TRY;
            phaseTimer = 0;
            shake(0.6, 0.6);
        }

        private String scoreLine() {
            return home.name + " " + home.score + " - " + away.score + " " + away.name;
        }

        private Player nearestPlayer(Vec2 pos, double maxR) {
            Player best = null;
            double bestD = maxR;
            for (Team t : listTeams()) {
                for (Player p : t.players) {
                    double d = p.pos.distanceTo(pos);
                    if (d < bestD) {
                        bestD = d;
                        best = p;
                    }
                }
            }
            return best;
        }

        private List<Team> listTeams() {
            List<Team> list = new ArrayList<>();
            list.add(home);
            list.add(away);
            return list;
        }

        private void shake(double time, double intensity) {
            shakeTime = Math.max(shakeTime, time);
            shakeIntensity = Math.max(shakeIntensity, intensity);
        }

        private void spawnWeatherParticles(double dt) {
            if (weather.type == WeatherType.RAIN) {
                // Spawn rain droplets and ground splashes
                int drops = (int) (viewW * viewH * 0.00002);
                for (int i = 0; i < drops; i++) {
                    // World-space random spawn across the visible area
                    double wx = rng.nextDouble() * pitch.length;
                    double wy = rng.nextDouble() * pitch.width;
                    particles.rainDrop(new Vec2(wx, wy), weather.wind);
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // World transform
            AffineTransform prev = g2.getTransform();
            AffineTransform world = camera.worldToScreen(pitch, viewW, viewH);
            g2.transform(world);

            // Draw pitch
            drawPitch(g2);

            // Particles under
            particles.renderUnder(g2);

            // Draw teams
            drawTeam(g2, home, home == kickingTeam && phase == GamePhase.KICKOFF);
            drawTeam(g2, away, away == kickingTeam && phase == GamePhase.KICKOFF);

            // Ball
            drawBall(g2);

            // Particles over
            particles.renderOver(g2);

            // Restore
            g2.setTransform(prev);

            // HUD
            drawHUD(g2);

            // Debug overlay
            if (debug) drawDebug(g2);

            g2.dispose();
        }

        private void drawPitch(Graphics2D g2) {
            // Grass stripes
            int stripes = 14;
            double stripeH = pitch.width / stripes;
            for (int i = 0; i < stripes; i++) {
                g2.setColor((i % 2 == 0) ? grassA : grassB);
                Rectangle2D r = new Rectangle2D.Double(0, i * stripeH, pitch.length, stripeH);
                g2.fill(r);
            }

            // Lines: boundary, try lines, halfway, 22m, 10m
            g2.setColor(Color.white);
            g2.setStroke(new BasicStroke(0.2f));
            g2.draw(new Rectangle2D.Double(0, 0, pitch.length, pitch.width));
            g2.draw(new Line2D.Double(pitch.tryDepth, 0, pitch.tryDepth, pitch.width));
            g2.draw(new Line2D.Double(pitch.length - pitch.tryDepth, 0, pitch.length - pitch.tryDepth, pitch.width));
            g2.draw(new Line2D.Double(pitch.length * 0.5, 0, pitch.length * 0.5, pitch.width));

            double l22L = pitch.tryDepth + 22;
            double l22R = pitch.length - (pitch.tryDepth + 22);
            g2.draw(new Line2D.Double(l22L, 0, l22L, pitch.width));
            g2.draw(new Line2D.Double(l22R, 0, l22R, pitch.width));

            double l10L = pitch.length * 0.5 - 10;
            double l10R = pitch.length * 0.5 + 10;
            g2.draw(new Line2D.Double(l10L, 0, l10L, pitch.width));
            g2.draw(new Line2D.Double(l10R, 0, l10R, pitch.width));

            // Goal posts simplified
            g2.setColor(new Color(235, 235, 255));
            double postY1 = pitch.width * 0.5 - 2.5;
            double postY2 = pitch.width * 0.5 + 2.5;
            double xL = pitch.tryDepth - 0.1;
            double xR = pitch.length - pitch.tryDepth + 0.1;
            g2.fill(new Rectangle2D.Double(xL, postY1, 0.2, 5));
            g2.fill(new Rectangle2D.Double(xR - 0.2, postY1, 0.2, 5));

            // Weather tint
            if (weather.type == WeatherType.RAIN) {
                g2.setColor(new Color(80, 100, 120, 35));
                g2.fill(new Rectangle2D.Double(0, 0, pitch.length, pitch.width));
            }
        }

        private void drawTeam(Graphics2D g2, Team team, boolean kickerHighlight) {
            for (int i = 0; i < team.players.size(); i++) {
                Player p = team.players.get(i);
                drawPlayer(g2, p, (team == home && i == team.controlledIndex), kickerHighlight && i == 0);
            }
        }

        private void drawPlayer(Graphics2D g2, Player p, boolean isControlled, boolean kickoffKicker) {
            double r = 0.52;

            // Shadow
            if (showShadows) {
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fill(new Ellipse2D.Double(p.pos.x - r, p.pos.y - r * 0.4, r * 2, r * 0.8));
            }

            // Body - jersey ring
            g2.setColor(p.team.color);
            g2.fill(new Ellipse2D.Double(p.pos.x - r, p.pos.y - r, r * 2, r * 2));

            // Accent stripes (team color variations)
            g2.setColor(new Color(255, 255, 255, 180));
            double stripeW = 0.14;
            g2.fill(new Rectangle2D.Double(p.pos.x - r, p.pos.y - stripeW / 2, r * 2, stripeW));
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fill(new Rectangle2D.Double(p.pos.x - r, p.pos.y - stripeW / 2 + 0.18, r * 2, stripeW * 0.5));

            // Facing indicator
            Vec2 head = p.pos.add(p.heading.normalized().scale(0.65));
            g2.setColor(Color.white);
            g2.setStroke(new BasicStroke(0.12f));
            g2.draw(new Line2D.Double(p.pos.x, p.pos.y, head.x, head.y));

            // Controlled ring
            if (isControlled) {
                float[] dash = {0.32f, 0.18f};
                g2.setColor(new Color(255, 255, 255, 230));
                g2.setStroke(new BasicStroke(0.12f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1, dash, (float) p.anim));
                g2.draw(new Ellipse2D.Double(p.pos.x - r - 0.3, p.pos.y - r - 0.3, (r + 0.3) * 2, (r + 0.3) * 2));
            }

            // kickoff highlight arrow
            if (kickoffKicker) {
                g2.setColor(new Color(255, 255, 0, 200));
                g2.setStroke(new BasicStroke(0.08f));
                g2.draw(new Ellipse2D.Double(p.pos.x - r - 0.5, p.pos.y - r - 0.5, (r + 0.5) * 2, (r + 0.5) * 2));
            }

            // Carried ball ring
            if (p.hasBall) {
                g2.setColor(Color.yellow);
                g2.setStroke(new BasicStroke(0.08f));
                g2.draw(new Ellipse2D.Double(p.pos.x - r - 0.15, p.pos.y - r - 0.15, (r + 0.15) * 2, (r + 0.15) * 2));
            }

            // Stamina arc
            if (staminaEnabled) {
                double rr = r + 0.18;
                double frac = p.stamina / 100.0;
                g2.setColor(new Color(0, 0, 0, 40));
                g2.setStroke(new BasicStroke(0.1f));
                g2.draw(new Ellipse2D.Double(p.pos.x - rr, p.pos.y - rr, rr * 2, rr * 2));
                g2.setColor(new Color(60, 220, 100, 200));
                drawArc(g2, p.pos.x, p.pos.y, rr, -90, (int) (360 * frac));
            }

            // Name / number tag simplified as small white cap
            if (showLabels) {
                g2.setColor(new Color(255, 255, 255, 180));
                g2.fill(new Ellipse2D.Double(p.pos.x - 0.18, p.pos.y - r - 0.2, 0.36, 0.22));
            }
        }

        private void drawBall(Graphics2D g2) {
            double size = 0.45;

            // Shadow
            if (showShadows) {
                g2.setColor(new Color(0, 0, 0, 60));
                double s = Math.max(0.2, 1 - ball.height * 0.1);
                g2.fill(new Ellipse2D.Double(ball.pos.x - size * s, ball.pos.y - size * 0.4 * s, size * 2 * s, size * 0.8 * s));
            }

            // Ball body
            g2.setColor(new Color(210, 190, 130));
            g2.fill(new Ellipse2D.Double(ball.drawX(), ball.drawY(), size * 2, size));

            // Seams
            g2.setColor(new Color(150, 130, 90));
            g2.setStroke(new BasicStroke(0.07f));
            g2.draw(new Ellipse2D.Double(ball.drawX(), ball.drawY(), size * 2, size));
            g2.draw(new Ellipse2D.Double(ball.drawX() + 0.2, ball.drawY() + 0.08, size * 1.6, size * 0.6));
        }

        private void drawHUD(Graphics2D g2) {
            int margin = 10;

            // Score line
            g2.setFont(fontBig);
            String score = home.name + " " + home.score + " - " + away.score + " " + away.name;
            int sw = g2.getFontMetrics().stringWidth(score);
            int sx = (viewW - sw) / 2;
            int sy = 44;

            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawString(score, sx + 2, sy + 2);
            g2.setColor(Color.white);
            g2.drawString(score, sx, sy);

            // Timer
            g2.setFont(fontUI);
            String clock = formatTime(matchClock);
            drawHUDText(g2, viewW / 2 - g2.getFontMetrics().stringWidth(clock) / 2, sy + 26, clock, Color.white);

            // Phase text
            String phaseText = "";
            if (phase == GamePhase.KICKOFF) phaseText = "Kickoff: " + kickingTeam.name;
            else if (phase == GamePhase.TRY) phaseText = "TRY! Restart soon...";
            else if (phase == GamePhase.SCRUM) phaseText = "Scrum: " + setPieces.scrumFeedTeamName();
            else if (phase == GamePhase.LINEOUT) phaseText = "Lineout: " + setPieces.lineoutTeamName();
            if (!phaseText.isEmpty()) {
                drawHUDText(g2, (viewW - g2.getFontMetrics().stringWidth(phaseText)) / 2, sy + 50, phaseText, Color.yellow);
            }

            // Controls hint
            String hint = "Move: WASD/Arrows | Sprint: Shift | Pass: Q | Kick: E | Tackle: F | Switch: Space | Pause: P | Debug: F1 | Camera: C";
            drawHUDText(g2, margin, viewH - margin, hint, Color.white);

            // Wind indicator
            String wind = "Wind: " + String.format("%.1f", weather.wind.length()) + " m/s " + weather.windDirectionText();
            drawHUDText(g2, margin, margin + 28, wind, Color.cyan);

            // Weather
            drawHUDText(g2, margin, margin + 52, "Weather: " + weather.describe(), Color.lightGray);

            // Mini-map (bottom-right)
            drawMinimap(g2, viewW - 208, viewH - 148, 198, 138);

            // Event log (left-bottom)
            eventLog.render(g2, margin, viewH - 210, 480, 190);
        }

        private void drawHUDText(Graphics2D g2, int x, int y, String text, Color c) {
            g2.setFont(fontUI);
            int w = g2.getFontMetrics().stringWidth(text);
            int h = g2.getFontMetrics().getHeight();
            g2.setColor(new Color(0, 0, 0, 130));
            g2.fillRoundRect(x - 6, y - h + 4, w + 12, h + 6, 8, 8);
            g2.setColor(c);
            g2.drawString(text, x, y);
        }

        private void drawMinimap(Graphics2D g2, int x, int y, int w, int h) {
            g2.setColor(new Color(20, 60, 20));
            g2.fillRoundRect(x, y, w, h, 8, 8);
            g2.setColor(Color.white);
            g2.drawRoundRect(x, y, w, h, 8, 8);

            for (Team t : listTeams()) {
                for (Player p : t.players) {
                    int px = x + (int) (p.pos.x / pitch.length * w);
                    int py = y + (int) (p.pos.y / pitch.width * h);
                    g2.setColor(t.color);
                    g2.fillOval(px - 3, py - 3, 6, 6);
                }
            }

            int bx = x + (int) (ball.pos.x / pitch.length * w);
            int by = y + (int) (ball.pos.y / pitch.width * h);
            g2.setColor(Color.yellow);
            g2.fillOval(bx - 3, by - 3, 6, 6);
        }

        private void drawDebug(Graphics2D g2) {
            int y = 120;
            g2.setColor(Color.white);
            g2.setFont(fontUI);
            g2.drawString("Debug:", 10, y); y += 18;
            g2.drawString("Phase: " + phase + " t=" + String.format("%.2f", phaseTimer), 10, y); y += 18;
            g2.drawString("Clock: " + formatTime(matchClock), 10, y); y += 18;
            g2.drawString("Ball: pos=" + ball.pos + " h=" + String.format("%.2f", ball.height) + " v=" + ball.vel, 10, y); y += 18;
            Player ctrl = home.getControlled();
            if (ctrl != null) {
                g2.drawString("Controlled: " + ctrl.nameID() + " pos=" + ctrl.pos + " v=" + ctrl.vel + " stamina=" + String.format("%.1f", ctrl.stamina), 10, y); y += 18;
            }
            g2.drawString("Score: " + scoreLine(), 10, y); y += 18;
            g2.drawString("Weather: " + weather.describe() + " wind=" + weather.wind + " gust=" + String.format("%.2f", weather.gustTimer), 10, y); y += 18;
        }

        private void drawArc(Graphics2D g2, double cx, double cy, double r, double startDeg, int extentDeg) {
            g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, startDeg, extentDeg, Arc2D.OPEN));
        }

        private String formatTime(double s) {
            int mm = (int) (s / 60);
            int ss = (int) (s % 60);
            return String.format("%02d:%02d", mm, ss);
        }

        @Override
        public void componentResized(ComponentEvent e) {
            viewW = getWidth();
            viewH = getHeight();
        }

        @Override public void componentMoved(ComponentEvent e) {}
        @Override public void componentShown(ComponentEvent e) {}
        @Override public void componentHidden(ComponentEvent e) {}

        @Override public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            input.set(e.getKeyCode(), true);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            input.set(e.getKeyCode(), false);
        }

        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); }
        @Override public void mouseReleased(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
        @Override public void mouseDragged(MouseEvent e) { mouse = e.getPoint(); }
        @Override public void mouseMoved(MouseEvent e) { mouse = e.getPoint(); }
    }

    // Pitch with metric dimensions; includes helper methods.
    static class Pitch {
        public final double length = 120.0; // include in-goals
        public final double width = 70.0;
        public final double tryDepth = 10.0;

        Vec2 clamp(Vec2 p, double margin) {
            return new Vec2(RugbyGameSimulation2.clamp(p.x, margin, length - margin), RugbyGameSimulation2.clamp(p.y, margin, width - margin));
        }
    }

    // Camera with smooth follow and optional shake offset.
    static class Camera {
        Vec2 pos = new Vec2(0, 0);
        Vec2 target = new Vec2(0, 0);
        double zoom = 8;
        double smooth = 6.0;

        void update(double dt, Pitch pitch, int vw, int vh, Vec2 shake) {
            Vec2 to = target.sub(pos);
            double damp = Math.pow(0.001, dt * smooth);
            pos = new Vec2(pos.x + to.x * (1 - damp) + shake.x, pos.y + to.y * (1 - damp) + shake.y);

            // Clamp within field
            pos = pitch.clamp(pos, 5.0);
        }

        AffineTransform worldToScreen(Pitch pitch, int vw, int vh) {
            double sx = vw / pitch.length;
            double sy = vh / pitch.width;
            double sc = Math.min(sx, sy) * 0.95;
            zoom = sc;

            double tx = vw * 0.5 - pos.x * sc;
            double ty = vh * 0.5 - pos.y * sc;

            AffineTransform at = new AffineTransform();
            at.translate(tx, ty);
            at.scale(sc, sc);
            return at;
        }
    }

    // Player entity with simple state and movement characteristics.
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
        double tackleCooldown = 0;
        double recoverTimer = 0;

        // Stats
        double maxSpeed = 7.0;
        double accel = 15.0;
        double agility = 0.9;
        double stamina = 100;

        double passing = 0.6;
        double kicking = 0.6;
        double tackling = 0.6;

        // Animation
        double anim = 0;

        Player(Team team, String name, Vec2 pos) {
            this.team = team;
            this.name = name;
            this.pos = pos.copy();
        }

        String nameID() { return name + " #" + number; }

        void seek(Vec2 dir, double speed, double dt) {
            if (dir.lengthSq() < 0.0001) return;
            Vec2 desired = dir.normalized().scale(speed);
            Vec2 dv = desired.sub(vel);
            double maxA = accel * agility;
            double dl = dv.length();
            if (dl > maxA * dt) dv = dv.scale((maxA * dt) / dl);
            vel = vel.add(dv);
            if (vel.lengthSq() > 0.01) heading = vel.normalized();
        }

        void avoidMates(Team team, double dt) {
            Vec2 push = new Vec2(0, 0);
            for (Player o : team.players) {
                if (o == this) continue;
                Vec2 d = pos.sub(o.pos);
                double dist = d.length();
                if (dist > 0 && dist < 1.0) push = push.add(d.scale((1.0 - dist) * 2.0 / dist));
            }
            if (push.lengthSq() > 0.0001) vel = vel.add(push.scale(dt));
        }
    }

    // Team grouping players and scoring; some helper AI selection methods.
    static class Team {
        String name;
        Side side;
        Color color;
        List<Player> players = new ArrayList<>();
        Team opponent;

        int score = 0;
        int controlledIndex = 0;

        Team(String name, Side side, Color color) {
            this.name = name;
            this.side = side;
            this.color = color;
        }

        Player getControlled() {
            if (players.isEmpty()) return null;
            controlledIndex = clampIndex(controlledIndex, 0, players.size() - 1);
            return players.get(controlledIndex);
        }

        void selectBestController(Ball ball) {
            Player best = null;
            double bestScore = -1e9;
            for (int i = 0; i < players.size(); i++) {
                Player p = players.get(i);
                double s = 0;
                if (p.hasBall) s += 1000;
                s -= p.pos.distanceTo(ball.pos);
                s -= (p.state == PlayerState.TACKLED || p.state == PlayerState.STUNNED) ? 20 : 0;
                if (s > bestScore) {
                    bestScore = s;
                    best = p;
                    controlledIndex = i;
                }
            }
        }

        boolean hasBall(Ball ball) {
            for (Player p : players) if (p.hasBall) return true;
            return false;
        }

        Player carrier(Ball ball) {
            for (Player p : players) if (p.hasBall) return p;
            return null;
        }

        Player nearestTo(Vec2 pos) {
            Player best = null;
            double bestD = Double.MAX_VALUE;
            for (Player p : players) {
                double d = p.pos.distanceTo(pos);
                if (d < bestD) { bestD = d; best = p; }
            }
            return best;
        }

        double distanceToNearest(Vec2 pos) {
            double bestD = Double.MAX_VALUE;
            for (Player p : players) {
                double d = p.pos.distanceTo(pos);
                if (d < bestD) bestD = d;
            }
            return bestD;
        }

        Player bestPass(Player passer, Ball ball, Pitch pitch) {
            Player best = null;
            double bestScore = -1e9;
            for (Player t : players) {
                if (t == passer) continue;
                Vec2 to = t.pos.sub(passer.pos);
                boolean backwardOK = (side == Side.LEFT) ? (to.x <= 0.2) : (to.x >= -0.2);
                if (!backwardOK) continue;
                double dist = to.length();
                if (dist < 3 || dist > 28) continue;

                double open = opponent.nearestTo(t.pos).pos.distanceTo(t.pos);
                double distScore = -Math.abs(dist - 12);
                double angleScore = passer.heading.dot(to.normalized()) * 1.8;

                double s = distScore + open + angleScore;
                if (s > bestScore) { bestScore = s; best = t; }
            }
            return best;
        }

        Player bestTackleTarget(Player tackler) {
            Player best = null;
            double bestScore = -1e9;
            for (Player o : opponent.players) {
                Vec2 to = o.pos.sub(tackler.pos);
                double dist = to.length();
                if (dist > 6.0) continue;
                double angle = tackler.heading.dot(to.normalized());
                if (angle < -0.2) continue;
                double s = (1.0 - dist / 6.0) + angle * 0.6 + (o.hasBall ? 1.6 : 0);
                if (s > bestScore) { bestScore = s; best = o; }
            }
            return best;
        }
    }

    // Ball with simple 3D height and last touched info.
    static class Ball {
        Vec2 pos;
        Vec2 vel = new Vec2(0, 0);
        double height = 0;
        double zVel = 0;
        Player owner = null;
        Player lastTouchedBy = null;

        Ball(Vec2 p) { this.pos = p.copy(); }

        void attachTo(Player p, double forward) {
            owner = p;
            p.hasBall = true;
            pos = p.pos.add(p.heading.scale(forward));
            vel.set(0, 0);
            height = 0.45;
            zVel = 0;
        }

        void detach() {
            if (owner != null) {
                owner.hasBall = false;
                owner = null;
            }
        }

        double drawX() { return pos.x - 0.45; }
        double drawY() { return pos.y - 0.22 - height * 0.12; }

        @Override
        public String toString() {
            return "Ball{pos=" + pos + ", h=" + String.format("%.2f", height) + ", vel=" + vel + "}";
        }
    }

    // Event log rendering simple lines to the HUD.
    static class EventLog {
        ArrayDeque<String> lines = new ArrayDeque<>();
        int cap = 9;

        void add(String s) {
            lines.addFirst(s);
            while (lines.size() > cap) lines.removeLast();
            System.out.println(s);
        }

        void render(Graphics2D g2, int x, int y, int w, int h) {
            g2.setColor(new Color(0, 0, 0, 135));
            g2.fillRoundRect(x, y, w, h, 10, 10);
            g2.setColor(Color.white);
            g2.drawRoundRect(x, y, w, h, 10, 10);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            int yy = y + 24;
            for (String s : lines) {
                g2.setColor(Color.white);
                g2.drawString(s, x + 10, yy);
                yy += 18;
            }
            g2.setColor(Color.gray);
            g2.drawString("Events", x + 10, y + 18);
        }
    }

    // Replay buffer (lightweight) storing positions for the last seconds.
    static class Replay {
        static class Frame {
            double time;
            List<Vec2> homePos;
            List<Vec2> awayPos;
            Vec2 ballPos;
            double ballH;
            String marker;

            Frame(double time, List<Vec2> h, List<Vec2> a, Vec2 b, double bh) {
                this.time = time;
                this.homePos = h;
                this.awayPos = a;
                this.ballPos = b;
                this.ballH = bh;
            }
        }

        ArrayDeque<Frame> buffer = new ArrayDeque<>();
        double t = 0;

        void capture(GamePanel g, double dt) {
            t += dt;
            if (buffer.size() > 600) buffer.removeFirst();
            buffer.addLast(snap(g));
        }

        Frame snap(GamePanel g) {
            List<Vec2> h = new ArrayList<>();
            for (Player p : g.home.players) h.add(p.pos.copy());
            List<Vec2> a = new ArrayList<>();
            for (Player p : g.away.players) a.add(p.pos.copy());
            return new Frame(t, h, a, g.ball.pos.copy(), g.ball.height);
        }

        void mark(String s) {
            if (!buffer.isEmpty()) buffer.getLast().marker = s;
        }

        void clear() {
            buffer.clear();
            t = 0;
        }
    }

    // Particles for visuals including trails, rain, impacts, confetti.
    static class ParticleSystem {
        static class Particle {
            Vec2 pos;
            Vec2 vel;
            double life, maxLife;
            Color color;
            double size;
            boolean over;

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
                vel = vel.scale(Math.max(0, 1 - dt * 2.0));
            }

            void draw(Graphics2D g2) {
                float a = (float) Math.max(0, Math.min(1, life / maxLife));
                Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (a * 255));
                g2.setColor(c);
                g2.fill(new Ellipse2D.Double(pos.x - size / 2, pos.y - size / 2, size, size));
            }
        }

        List<Particle> under = new ArrayList<>();
        List<Particle> over = new ArrayList<>();
        Random rng = new Random(7);

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

        void spawnFootTrail(Vec2 pos, Color color) {
            Vec2 v = new Vec2((rng.nextDouble() - 0.5) * 0.6, (rng.nextDouble() - 0.5) * 0.6);
            under.add(new Particle(pos.add(new Vec2((rng.nextDouble() - 0.5) * 0.2, (rng.nextDouble() - 0.5) * 0.2)), v, 0.4 + rng.nextDouble() * 0.3, color, 0.25 + rng.nextDouble() * 0.25, false));
        }

        void passPuff(Vec2 pos) {
            for (int i = 0; i < 8; i++) {
                Vec2 v = new Vec2((rng.nextDouble() - 0.5) * 2.0, (rng.nextDouble() - 0.5) * 2.0);
                under.add(new Particle(pos, v, 0.6 + rng.nextDouble() * 0.3, new Color(220, 220, 220), 0.2 + rng.nextDouble() * 0.2, false));
            }
        }

        void kickFlash(Vec2 pos) {
            for (int i = 0; i < 16; i++) {
                double a = rng.nextDouble() * Math.PI * 2;
                Vec2 v = new Vec2(Math.cos(a), Math.sin(a)).scale(3 + rng.nextDouble() * 3);
                over.add(new Particle(pos, v, 0.5 + rng.nextDouble() * 0.3, Color.yellow, 0.22 + rng.nextDouble() * 0.2, true));
            }
        }

        void impact(Vec2 pos) {
            for (int i = 0; i < 14; i++) {
                double a = rng.nextDouble() * Math.PI * 2;
                Vec2 v = new Vec2(Math.cos(a), Math.sin(a)).scale(2 + rng.nextDouble() * 2);
                over.add(new Particle(pos, v, 0.6 + rng.nextDouble() * 0.3, new Color(255, 140, 100), 0.3 + rng.nextDouble() * 0.2, true));
            }
        }

        void whiff(Vec2 pos) {
            for (int i = 0; i < 10; i++) {
                Vec2 v = new Vec2((rng.nextDouble() - 0.5) * 1.5, (rng.nextDouble() - 0.5) * 1.5);
                over.add(new Particle(pos, v, 0.5 + rng.nextDouble() * 0.2, new Color(180, 180, 255), 0.25, true));
            }
        }

        void bounce(Vec2 pos) {
            for (int i = 0; i < 6; i++) {
                Vec2 v = new Vec2((rng.nextDouble() - 0.5) * 1.0, (rng.nextDouble() - 0.5) * 1.0);
                under.add(new Particle(pos, v, 0.5 + rng.nextDouble() * 0.3, new Color(200, 200, 200), 0.2 + rng.nextDouble() * 0.2, false));
            }
        }

        void confetti(Vec2 pos, Color team) {
            for (int i = 0; i < 38; i++) {
                double a = rng.nextDouble() * Math.PI * 2;
                Vec2 v = new Vec2(Math.cos(a), Math.sin(a)).scale(2 + rng.nextDouble() * 2);
                Color c = rng.nextBoolean() ? team : Color.white;
                over.add(new Particle(pos, v, 1.2 + rng.nextDouble() * 0.6, c, 0.3 + rng.nextDouble() * 0.3, true));
            }
        }

        void rainDrop(Vec2 pos, Vec2 wind) {
            // Falling droplet; add slight wind offset; create small ground splash at end
            Vec2 v = new Vec2(wind.x * 0.2, 6 + rng.nextDouble() * 3);
            under.add(new Particle(pos, v, 0.4 + rng.nextDouble() * 0.2, new Color(180, 200, 255, 180), 0.18, false));
        }

        void spawnBounce(Vec2 pos) { bounce(pos); }

        void clear() {
            under.clear();
            over.clear();
        }
    }

    // Weather system with simple states and wind gusts.
    static class Weather {
        WeatherType type = WeatherType.SUNNY;
        Vec2 wind = new Vec2(0, 0);
        double gustTimer = 0;

        void randomize(Random rng) {
            type = rng.nextDouble() < 0.6 ? WeatherType.SUNNY : WeatherType.RAIN;
            double speed = rng.nextDouble() * 4.0; // m/s
            double a = rng.nextDouble() * Math.PI * 2;
            wind = new Vec2(Math.cos(a), Math.sin(a)).scale(speed);
            gustTimer = 2 + rng.nextDouble() * 4;
        }

        void update(double dt) {
            // Gusts slowly vary wind
            gustTimer -= dt;
            if (gustTimer <= 0) {
                gustTimer = 2 + Math.random() * 4;
                double change = (Math.random() - 0.5) * 2.0;
                double ang = Math.atan2(wind.y, wind.x) + (Math.random() - 0.5) * 0.3;
                double spd = clamp(wind.length() + change, 0, 6.5);
                wind = new Vec2(Math.cos(ang), Math.sin(ang)).scale(spd);
            }
        }

        String describe() {
            return type == WeatherType.SUNNY ? "Sunny" : "Rain";
        }

        String windDirectionText() {
            double ang = Math.atan2(wind.y, wind.x);
            double deg = Math.toDegrees(ang);
            String[] dirs = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};
            int idx = (int) Math.round((((deg + 360) % 360) / 45.0));
            idx = (idx + dirs.length) % dirs.length;
            return dirs[idx];
        }

        double windBallInfluence() {
            return (type == WeatherType.RAIN) ? 1.6 : 1.0;
        }
    }

    enum WeatherType { SUNNY, RAIN }

    // Simple set-piece manager for scrum and lineout (stylized).
    static class SetPieces {
        // Scrum data
        private boolean scrumActive = false;
        private Team scrumFeedTeam = null;
        private Vec2 scrumPos = new Vec2(0, 0);
        private double scrumTimer = 0;

        // Lineout data
        private boolean lineoutActive = false;
        private Team lineoutTeam = null;
        private Vec2 lineoutPos = new Vec2(0, 0);
        private double lineoutTimer = 0;

        void startScrum(Team feedTeam, Vec2 at, GamePanel gp) {
            scrumActive = true;
            lineoutActive = false;
            scrumFeedTeam = feedTeam;
            scrumPos = new Vec2(clamp(at.x, 15, gp.pitch.length - 15), clamp(at.y, 12, gp.pitch.width - 12));
            scrumTimer = 0;

            // Arrange packs: 3 forwards each side around scrum position
            arrangeScrumPacks(feedTeam, gp);

            gp.phase = GamePhase.SCRUM;
            gp.phaseTimer = 0;
        }

        void arrangeScrumPacks(Team feedTeam, GamePanel gp) {
            Team def = feedTeam.opponent;

            // Pick front-row as last three players for simplicity
            List<Player> atkPack = new ArrayList<>();
            List<Player> defPack = new ArrayList<>();
            for (int i = gp.home.players.size() - 1; i >= gp.home.players.size() - 3; i--) {
                // We'll compute per side generically below
            }
            // We will simply place the closest 5 players of each team around scrum for a better look
            placeClosestAround(gp, feedTeam, scrumPos, 5, (feedTeam.side == Side.LEFT) ? -1 : 1);
            placeClosestAround(gp, def, scrumPos, 5, (feedTeam.side == Side.LEFT) ? 1 : -1);
        }

        void placeClosestAround(GamePanel gp, Team team, Vec2 center, int count, int dir) {
            // Find nearest 'count' players to center
            List<Player> copy = new ArrayList<>(team.players);
            copy.sort((a, b) -> Double.compare(a.pos.distanceTo(center), b.pos.distanceTo(center)));
            for (int i = 0; i < Math.min(count, copy.size()); i++) {
                Player p = copy.get(i);
                p.vel.set(0, 0);
                p.hasBall = false;
                double ox = dir * (0.6 + 0.7 * (i / 3));
                double oy = (i % 3 - 1) * 0.9;
                p.pos.set(center.x + ox + (Math.random() - 0.5) * 0.1, center.y + oy + (Math.random() - 0.5) * 0.1);
                p.state = PlayerState.IDLE;
            }
            // Others back away a little
            for (int i = count; i < copy.size(); i++) {
                Player p = copy.get(i);
                Vec2 d = p.pos.sub(center);
                if (d.length() < 5) {
                    p.pos = center.add(d.normalized().scale(5));
                }
            }
        }

        void updateScrum(double dt, GamePanel gp) {
            scrumTimer += dt;
            // A scrum in this arcade: small delay, then the feed team wins 70% and picks up ball
            if (scrumTimer < 1.2) {
                // Players remain mostly static
                if (scrumTimer > 0.5 && gp.ball.owner != null) {
                    // Drop the ball to simulate feed
                    gp.ball.detach();
                    gp.ball.pos = scrumPos.copy();
                    gp.ball.height = 0;
                    gp.ball.vel.set((scrumFeedTeam.side == Side.LEFT ? 0.5 : -0.5), 0);
                }
            } else if (scrumTimer < 2.0) {
                // Contest: ball wiggles slightly
                gp.ball.vel = new Vec2((Math.random() - 0.5) * 0.6, (Math.random() - 0.5) * 0.6);
            } else {
                boolean win = Math.random() < 0.7;
                Team winner = win ? scrumFeedTeam : scrumFeedTeam.opponent;
                Player pick = winner.nearestTo(scrumPos);
                if (pick != null) {
                    pick.hasBall = true;
                    gp.ball.attachTo(pick, 0.5);
                    gp.eventLog.add("Scrum won by " + winner.name + ". " + pick.nameID() + " picks up.");
                    scrumActive = false;
                    gp.phase = GamePhase.PLAY;
                    gp.phaseTimer = 0;
                } else {
                    // fallback
                    scrumActive = false;
                    gp.phase = GamePhase.PLAY;
                    gp.phaseTimer = 0;
                }
            }
        }

        void startLineout(Team throwTeam, Vec2 at, Pitch pitch, GamePanel gp) {
            lineoutActive = true;
            scrumActive = false;
            lineoutTeam = throwTeam;
            // Clamp to touchline
            lineoutPos = new Vec2(clamp(at.x, 15, pitch.length - 15), (at.y < pitch.width / 2) ? 1.5 : pitch.width - 1.5);
            lineoutTimer = 0;

            // Arrange two lines facing each other near lineoutPos
            arrangeLineout(throwTeam, gp);

            gp.phase = GamePhase.LINEOUT;
            gp.phaseTimer = 0;
        }

        void arrangeLineout(Team throwTeam, GamePanel gp) {
            Team receiveTeam = throwTeam.opponent;
            double offs = (lineoutPos.y < gp.pitch.width / 2) ? 1.0 : -1.0; // which side of touch
            // Place best 4 nearest on each side into line
            placeLine(gp, throwTeam, lineoutPos.add(new Vec2(offs * -1.2, 0)), 4, -offs);
            placeLine(gp, receiveTeam, lineoutPos.add(new Vec2(offs * 1.2, 0)), 4, offs);

            // Thrower behind touch point
            Player thrower = throwTeam.nearestTo(lineoutPos);
            thrower.pos = lineoutPos.add(new Vec2(offs * -2.0, 0));
            thrower.vel.set(0, 0);
            thrower.state = PlayerState.IDLE;
            thrower.hasBall = true;
            gp.ball.attachTo(thrower, 0.4);
        }

        void placeLine(GamePanel gp, Team team, Vec2 origin, int count, double sideSign) {
            List<Player> copy = new ArrayList<>(team.players);
            copy.sort((a, b) -> Double.compare(a.pos.distanceTo(lineoutPos), b.pos.distanceTo(lineoutPos)));
            for (int i = 0; i < Math.min(count, copy.size()); i++) {
                Player p = copy.get(i);
                p.vel.set(0, 0);
                p.hasBall = false;
                double oy = (i - (count - 1) / 2.0) * 1.4;
                p.pos.set(origin.x, clamp(origin.y + oy, 2, gp.pitch.width - 2));
                p.state = PlayerState.IDLE;
                p.heading = new Vec2(sideSign, 0);
            }
        }

        void updateLineout(double dt, GamePanel gp) {
            lineoutTimer += dt;
            // At 1 second, throw is made
            if (lineoutTimer > 1.0 && lineoutTimer < 1.1) {
                Player thrower = lineoutTeam.nearestTo(lineoutPos);
                if (thrower != null && thrower.hasBall) {
                    // Throw slightly above center with arc
                    Player jumper = lineoutTeam.opponent.nearestTo(lineoutPos);
                    double towards = (Math.random() < 0.5) ? -1 : 1;
                    Vec2 dir = new Vec2(0, towards).normalized();
                    double speed = 12 + Math.random() * 4;
                    gp.ball.detach();
                    gp.ball.pos = lineoutPos.copy();
                    gp.ball.vel = dir.scale(speed);
                    gp.ball.height = 1.8;
                    gp.ball.zVel = 4.5;
                    gp.ball.lastTouchedBy = thrower;
                    thrower.hasBall = false;
                    gp.eventLog.add("Lineout throw by " + thrower.nameID());
                }
            } else if (lineoutTimer > 1.1) {
                // Contest: nearest player can catch when ball height drops
                if (gp.ball.height < 0.5) {
                    Player near = gp.nearestPlayer(gp.ball.pos, 1.5);
                    if (near != null) {
                        near.hasBall = true;
                        gp.ball.attachTo(near, 0.5);
                        gp.ball.lastTouchedBy = near;
                        gp.eventLog.add(near.nameID() + " wins the lineout.");
                        lineoutActive = false;
                        gp.phase = GamePhase.PLAY;
                        gp.phaseTimer = 0;
                    }
                }
            }
        }

        String scrumFeedTeamName() {
            return scrumFeedTeam != null ? scrumFeedTeam.name : "";
        }

        String lineoutTeamName() {
            return lineoutTeam != null ? lineoutTeam.name : "";
        }
    }

    // Input handling for keys with pressed and hold states.
    static class Input {
        final boolean[] down = new boolean[512];
        final boolean[] pressed = new boolean[512];

        void set(int key, boolean d) {
            if (key < 0 || key >= down.length) return;
            if (d && !down[key]) pressed[key] = true;
            down[key] = d;
        }

        boolean isDown(int key) {
            if (key < 0 || key >= down.length) return false;
            return down[key];
        }

        boolean consumePressed(int key) {
            if (key < 0 || key >= down.length) return false;
            boolean p = pressed[key];
            pressed[key] = false;
            return p;
        }
    }

    // 2D vector utilities
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
        public String toString() { return String.format("(%.2f, %.2f)", x, y); }
    }

    // Enums
    enum Side { LEFT, RIGHT }
    enum PlayerState { IDLE, RUN, TACKLING, TACKLED, STUNNED }
    enum Role { FORWARD, CENTER, BACK, FLY_HALF }
    enum GamePhase { PRE_KICKOFF, KICKOFF, PLAY, SCRUM, LINEOUT, TRY, CONVERSION, FULLTIME, PAUSED }

    // Utility clamps
    static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    static int clampIndex(int v, int min, int max) {
        if (v < min) return min;
        return Math.max(min, Math.min(max, v));
    }
}