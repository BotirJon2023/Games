import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class HandballChampionshipGame extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HandballChampionshipGame::new);
    }

    public HandballChampionshipGame() {
        super("Handball Championship Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        GamePanel panel = new GamePanel();
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        panel.start();
    }

    // ============================================================
    // GamePanel: main surface and game loop
    // ============================================================
    static class GamePanel extends JPanel implements Runnable, KeyListener, MouseListener, MouseMotionListener, FocusListener {
        // Target dimensions (we will scale drawing if resized)
        private static final int BASE_WIDTH = 1280;
        private static final int BASE_HEIGHT = 720;

        // Game loop
        private Thread gameThread;
        private volatile boolean running = false;
        private volatile boolean paused = false;

        // Timing
        private long lastTimeNanos;
        private double accumulator = 0;
        private final double dt = 1.0 / 120.0; // fixed update at 120 Hz for physics
        private double fps = 0;

        // World elements
        private final Court court = new Court(BASE_WIDTH, BASE_HEIGHT);
        private final Ball ball = new Ball();
        private final Team teamBlue = new Team("Blue", new Color(0, 125, 255));
        private final Team teamRed = new Team("Red", new Color(220, 60, 60));
        private final List<Player> players = new ArrayList<>();
        private final List<Particle> particles = new CopyOnWriteArrayList<>();
        private final List<TransientText> texts = new CopyOnWriteArrayList<>();

        // Controllers / input
        private final boolean[] keys = new boolean[512];
        private Point mousePos = new Point(BASE_WIDTH / 2, BASE_HEIGHT / 2);
        private boolean mouseInside = false;

        // Human control
        private Player humanControlled;

        // Match state
        private final Scoreboard scoreboard = new Scoreboard();
        private final MatchClock clock = new MatchClock(60 * 2); // 2 minutes match for demo (120 seconds)
        private boolean countdown = true;
        private double countdownTime = 2.0; // seconds to start
        private boolean showHelp = true;

        // UI timing
        private double infoOverlayTimer = 4.0;

        // Random
        private final Random rng = new Random();

        // Pause flash timer
        private double pauseHintPulse = 0;

        // Audio stub (uses Toolkit beep only)
        private final SFX sfx = new SFX();

        // Camera shake
        private double camShakeTime = 0;
        private double camShakeStrength = 0;

        // Shot charge
        private boolean chargingShot = false;
        private double chargeTimer = 0;
        private final double maxCharge = 1.2;

        // AI difficulty (0=easy, 1=normal, 2=hard)
        private final int aiDifficulty = 1;

        public GamePanel() {
            setPreferredSize(new Dimension(BASE_WIDTH, BASE_HEIGHT));
            setBackground(new Color(32, 100, 42)); // grass-ish background
            setFocusable(true);
            requestFocusInWindow();
            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);
            addFocusListener(this);

            // Init teams: 6 players per team (typical handball on court)
            initTeamsAndPlayers();
            resetKickoff(teamBlue);
        }

        // Initialize teams, players, and positions
        private void initTeamsAndPlayers() {
            // Create players for both teams
            // Player(id, name, isGoalie, team, x, y)
            for (int i = 0; i < 6; i++) {
                boolean isGk = (i == 0); // first is goalie
                Player pBlue = new Player(i, "B" + i, isGk, teamBlue);
                Player pRed = new Player(i, "R" + i, isGk, teamRed);
                teamBlue.players.add(pBlue);
                teamRed.players.add(pRed);
                players.add(pBlue);
                players.add(pRed);
            }

            // Positions for demo layout (defensive positions)
            arrangeInitialPositions();

            // Assign human-controlled player
            humanControlled = teamBlue.players.get(1); // choose a field player
            humanControlled.isHumanControlled = true;

            // Set opponents AI
            for (Player p : players) {
                if (!p.isHumanControlled) {
                    p.ai = new BasicAI(p, court, ball, players, aiDifficulty);
                }
            }
        }

        private void arrangeInitialPositions() {
            // Blue team: left side; Red team: right side
            double midY = court.height / 2.0;
            double leftX = court.width * 0.2;
            double rightX = court.width * 0.8;

            // Blue goalie
            teamBlue.players.get(0).pos.set(court.goalLeft.getCenterX() + 30, midY);
            teamBlue.players.get(0).home.set(teamBlue.players.get(0).pos);

            // Blue line players
            double[] yOffsetsBlue = {-120, -50, 50, 120, 0};
            for (int i = 1; i < teamBlue.players.size(); i++) {
                Player p = teamBlue.players.get(i);
                p.pos.set(leftX + rng.nextDouble() * 30, midY + yOffsetsBlue[i - 1] + rng.nextDouble() * 10);
                p.home.set(p.pos);
            }

            // Red goalie
            teamRed.players.get(0).pos.set(court.goalRight.getCenterX() - 30, midY);
            teamRed.players.get(0).home.set(teamRed.players.get(0).pos);

            // Red line players
            double[] yOffsetsRed = {-120, -50, 50, 120, 0};
            for (int i = 1; i < teamRed.players.size(); i++) {
                Player p = teamRed.players.get(i);
                p.pos.set(rightX - rng.nextDouble() * 30, midY + yOffsetsRed[i - 1] + rng.nextDouble() * 10);
                p.home.set(p.pos);
            }
        }

        // Reset to kickoff after a goal; possession to team
        private void resetKickoff(Team possessingTeam) {
            // Place ball at center
            ball.pos.set(court.width / 2.0, court.height / 2.0);
            ball.vel.set(0, 0);
            ball.carrier = null;

            // Debounce
            ball.lastTouchTeam = null;
            ball.pickupCooldown = 0.2;

            // Reset players to initial formation positions
            for (Player p : players) {
                p.releaseBall();
                p.vel.set(0, 0);
                p.stamina = p.maxStamina;
                p.fatigue = 0;
                p.cooldown = 0;
                p.tackleCooldown = 0;
                p.switchCooldown = 0;
                p.recovering = false;
                p.stumbleTimer = 0;
                p.intent = PlayerIntent.Idle;
                p.passAssist = null;
                p.targetPos = new Vec2(p.home.x, p.home.y);
            }

            // Team with possession gets ball to a central player
            Team t = possessingTeam;
            Player passer = t.players.get(1); // choose a field player
            passer.pos.set(court.width / 2.0 - (t == teamBlue ? 60 : -60), court.height / 2.0);
            passer.grabBall(ball);
            ball.pos.set(passer.pos.x, passer.pos.y - 3);

            // Opponent stands back
            for (Player p : (t == teamBlue ? teamRed.players : teamBlue.players)) {
                p.pos.set(p.home.x, p.home.y);
            }

            // Countdown before restart
            countdown = true;
            countdownTime = 1.5;
            infoOverlayTimer = 3.0;
        }

        public void start() {
            if (gameThread != null && gameThread.isAlive()) return;
            running = true;
            lastTimeNanos = System.nanoTime();
            gameThread = new Thread(this, "GameLoop");
            gameThread.start();
        }

        @Override
        public void run() {
            long frames = 0;
            long fpsTimer = System.currentTimeMillis();

            while (running) {
                long now = System.nanoTime();
                double frameTime = Math.max(0, (now - lastTimeNanos) / 1e9);
                lastTimeNanos = now;

                accumulator += frameTime;

                while (accumulator >= dt) {
                    update(dt);
                    accumulator -= dt;
                }

                repaint();

                // FPS counter
                frames++;
                if (System.currentTimeMillis() - fpsTimer >= 1000) {
                    fps = frames;
                    frames = 0;
                    fpsTimer += 1000;
                }

                // Sleep a bit to avoid busy loop
                try {
                    Thread.sleep(2);
                } catch (InterruptedException ignored) {}
            }
        }

        private void update(double dt) {
            if (!hasFocus()) {
                paused = true;
            }

            // Pause toggle via P
            if (keys[KeyEvent.VK_P]) {
                // we do edge detection inside handleInput() to avoid repeated toggles
            }

            pauseHintPulse += dt * 2;

            // Update UI overlays timers
            if (infoOverlayTimer > 0) infoOverlayTimer -= dt;

            // Countdown before play resumes
            if (countdown) {
                countdownTime -= dt;
                if (countdownTime <= 0) {
                    countdown = false;
                }
            }

            handleInput(dt);

            if (paused) {
                // Pause particles update lightly
                for (Particle p : particles) {
                    p.life -= dt * 0.2;
                    p.vel.scale(0.98);
                    if (p.life <= 0) particles.remove(p);
                }
                return;
            }

            // Camera shake update
            camShakeTime -= dt;
            if (camShakeTime < 0) camShakeTime = 0;

            // Update clock (unless countdown or time up)
            if (!countdown && !clock.isFinished()) {
                clock.update(dt);
            }

            // Update ball pickup cooldown
            if (ball.pickupCooldown > 0) ball.pickupCooldown -= dt;

            // Update players
            for (Player p : players) {
                if (p.cooldown > 0) p.cooldown -= dt;
                if (p.tackleCooldown > 0) p.tackleCooldown -= dt;
                if (p.switchCooldown > 0) p.switchCooldown -= dt;
                if (p.stumbleTimer > 0) {
                    p.stumbleTimer -= dt;
                    if (p.stumbleTimer <= 0) p.recovering = false;
                }
                p.updateAI(dt, ball, players, court, scoreboard, clock, rng);
            }

            // Physics update: move players
            for (Player p : players) {
                // Player movement with stamina and friction
                p.vel.add(p.acc.x * dt, p.acc.y * dt);
                // Dampen acceleration per tick
                p.acc.set(0, 0);

                // Max speed with stamina effect
                double maxSpeed = p.getMaxSpeed();
                if (p.vel.length() > maxSpeed) {
                    p.vel.normalize().scale(maxSpeed);
                }

                p.pos.add(p.vel.x * dt, p.vel.y * dt);

                // Court boundaries for players (excluding goals area for goalies slightly)
                keepPlayerInBounds(p);

                // Friction
                p.vel.scale(p.recovering ? 0.90 : 0.92);

                // Stamina regen/drain
                p.updateStamina(dt);
            }

            // Ball physics
            if (ball.carrier != null) {
                // Ball sticks to carrier slightly ahead of player
                Player c = ball.carrier;
                Vec2 ahead = new Vec2(mousePos.x - c.pos.x, mousePos.y - c.pos.y);
                if (ahead.length() < 1) ahead.set(1, 0);
                ahead.normalize();
                ahead.scale(c.radius + ball.radius + 2);
                ball.pos.set(c.pos.x + ahead.x, c.pos.y + ahead.y);
                ball.vel.set(c.vel.x * 0.5, c.vel.y * 0.5);
            } else {
                // Gravity-like slight effect (simulate ball bounce on ground minimal)
                ball.vel.y += 0.0; // no gravity for flat court

                // Air drag / friction
                ball.vel.scale(0.993);

                // Move
                ball.pos.add(ball.vel.x * dt, ball.vel.y * dt);

                // Wall collisions
                if (ball.pos.x - ball.radius < court.playLeft) {
                    // Check if within left goal mouth
                    if (!insideGoalY(ball.pos.y, court.goalLeft)) {
                        ball.pos.x = court.playLeft + ball.radius;
                        ball.vel.x = -ball.vel.x * 0.7;
                        spawnBounce(ball.pos.x, ball.pos.y);
                    }
                }
                if (ball.pos.x + ball.radius > court.playRight) {
                    if (!insideGoalY(ball.pos.y, court.goalRight)) {
                        ball.pos.x = court.playRight - ball.radius;
                        ball.vel.x = -ball.vel.x * 0.7;
                        spawnBounce(ball.pos.x, ball.pos.y);
                    }
                }
                if (ball.pos.y - ball.radius < court.playTop) {
                    ball.pos.y = court.playTop + ball.radius;
                    ball.vel.y = -ball.vel.y * 0.7;
                    spawnBounce(ball.pos.x, ball.pos.y);
                }
                if (ball.pos.y + ball.radius > court.playBottom) {
                    ball.pos.y = court.playBottom - ball.radius;
                    ball.vel.y = -ball.vel.y * 0.7;
                    spawnBounce(ball.pos.x, ball.pos.y);
                }
            }

            // Ball trail particle
            spawnBallTrail();

            // Ball vs player control / pickup
            if (!countdown) {
                checkBallPossession();
            }

            // Player vs player collisions to reduce overlap
            resolvePlayerCollisions();

            // Check goal scored
            checkGoals();

            // Particles update
            updateParticles(dt);

            // Floating texts
            updateTexts(dt);

            // End match
            if (clock.isFinished()) {
                // Freeze ball carrier and input but allow camera
                // We'll allow restart via R key
            }
        }

        private void handleInput(double dt) {
            // Toggle pause on press P (edge detection)
            if (justPressed(KeyEvent.VK_P)) {
                paused = !paused;
            }
            if (justPressed(KeyEvent.VK_R)) {
                // Reset entire match
                scoreboard.reset();
                clock.reset();
                arrangeInitialPositions();
                resetKickoff(teamBlue);
                paused = false;
                showHelp = true;
            }
            if (!paused && !countdown && humanControlled != null && !clock.isFinished()) {
                // Movement
                Vec2 move = new Vec2();
                if (keys[KeyEvent.VK_W]) move.y -= 1;
                if (keys[KeyEvent.VK_S]) move.y += 1;
                if (keys[KeyEvent.VK_A]) move.x -= 1;
                if (keys[KeyEvent.VK_D]) move.x += 1;

                boolean sprint = keys[KeyEvent.VK_SHIFT];

                humanControlled.applyMovement(move, sprint);

                // Switch player
                if (justPressed(KeyEvent.VK_TAB) && humanControlled.switchCooldown <= 0) {
                    switchHumanControlled();
                    humanControlled.switchCooldown = 0.25;
                }

                // Shooting/Passing via Space (hold/release)
                if (keys[KeyEvent.VK_SPACE]) {
                    if (!chargingShot) {
                        chargingShot = true;
                        chargeTimer = 0;
                    }
                    chargeTimer += dt;
                    if (chargeTimer > maxCharge) chargeTimer = maxCharge;
                } else {
                    if (chargingShot) {
                        performShotOrPass(humanControlled, chargeTimer);
                        chargingShot = false;
                        chargeTimer = 0;
                    }
                }
            }
        }

        private boolean justPressed(int keyCode) {
            // We'll implement a simple static map to differentiate press/release
            // But since we only have current state, we simulate with a small cooldown on keys that we toggle manually.
            // Here, we implement an on-demand approach: we store last states in a static Map
            return KeyEdgeTracker.justPressed(keyCode, keys[keyCode]);
        }

        private void performShotOrPass(Player shooter, double charge) {
            if (shooter == null) return;
            if (shooter != ball.carrier) return;

            // Determine aim direction towards mouse
            Vec2 dir = new Vec2(mousePos.x - shooter.pos.x, mousePos.y - shooter.pos.y);
            double dist = dir.length();
            if (dist < 1) {
                dir.set(1, 0);
            } else {
                dir.scale(1.0 / dist);
            }

            // Decide pass vs shot based on charge duration threshold
            boolean isShot = charge > 0.25;

            // Power scaling
            double base = isShot ? 640 : 400;      // base speed
            double maxBonus = isShot ? 720 : 200;  // additional speed
            double power = base + maxBonus * Math.min(1.0, charge / maxCharge);

            // Add the player's velocity influence
            Vec2 initial = new Vec2(dir.x * power + shooter.vel.x * 0.2, dir.y * power + shooter.vel.y * 0.2);

            shooter.releaseBall();
            ball.carrier = null;
            ball.vel.set(initial.x, initial.y);
            ball.pos.set(shooter.pos.x + dir.x * (shooter.radius + ball.radius + 3), shooter.pos.y + dir.y * (shooter.radius + ball.radius + 3));
            ball.spin = rng.nextDouble() * 2 - 1;
            ball.pickupCooldown = 0.15;

            ball.lastTouchTeam = shooter.team;
            shooter.cooldown = 0.15; // slight action cooldown

            // Add kick particles
            spawnKick(shooter.pos.x, shooter.pos.y, dir);

            // Shot/Pass text
            if (isShot) {
                addText("Shot!", shooter.pos.x, shooter.pos.y - 30, shooter.team.color);
                sfx.beepShot();
            } else {
                addText("Pass!", shooter.pos.x, shooter.pos.y - 30, shooter.team.color);
                sfx.beepPass();
            }
        }

        private void switchHumanControlled() {
            if (humanControlled == null) return;
            Team t = humanControlled.team;
            List<Player> list = t.players;
            int idx = list.indexOf(humanControlled);
            for (int k = 1; k <= list.size(); k++) {
                int next = (idx + k) % list.size();
                Player p = list.get(next);
                if (!p.isGoalie) {
                    humanControlled.isHumanControlled = false;
                    humanControlled = p;
                    p.isHumanControlled = true;
                    addText("You control " + p.name, p.pos.x, p.pos.y - 50, new Color(255, 255, 180));
                    return;
                }
            }
        }

        private void checkBallPossession() {
            if (ball.carrier != null) return;
            if (ball.pickupCooldown > 0) return;

            Player nearest = null;
            double nearestDist2 = 9999999;
            for (Player p : players) {
                if (p.recovering) continue;
                double dx = p.pos.x - ball.pos.x;
                double dy = p.pos.y - ball.pos.y;
                double d2 = dx * dx + dy * dy;
                double catchRadius = (p.catchRadius + ball.radius);
                if (d2 < catchRadius * catchRadius) {
                    if (d2 < nearestDist2) {
                        nearestDist2 = d2;
                        nearest = p;
                    }
                }
            }
            if (nearest != null) {
                // Possess ball
                nearest.grabBall(ball);
                ball.lastTouchTeam = nearest.team;
                sfx.beepPickup();
                addText(nearest.team.name + " possession", ball.pos.x, ball.pos.y - 20, nearest.team.color);
            }
        }

        private void resolvePlayerCollisions() {
            for (int i = 0; i < players.size(); i++) {
                Player a = players.get(i);
                for (int j = i + 1; j < players.size(); j++) {
                    Player b = players.get(j);
                    double dx = b.pos.x - a.pos.x;
                    double dy = b.pos.y - a.pos.y;
                    double dist2 = dx * dx + dy * dy;
                    double minDist = a.radius + b.radius;
                    if (dist2 < minDist * minDist && dist2 > 0.00001) {
                        double dist = Math.sqrt(dist2);
                        double overlap = (minDist - dist) * 0.5;
                        double nx = dx / dist;
                        double ny = dy / dist;

                        a.pos.x -= nx * overlap;
                        a.pos.y -= ny * overlap;
                        b.pos.x += nx * overlap;
                        b.pos.y += ny * overlap;

                        // Bounce velocities a bit
                        double rvx = b.vel.x - a.vel.x;
                        double rvy = b.vel.y - a.vel.y;
                        double impulse = (rvx * nx + rvy * ny) * 0.5;
                        a.vel.x -= nx * impulse;
                        a.vel.y -= ny * impulse;
                        b.vel.x += nx * impulse;
                        b.vel.y += ny * impulse;

                        // Small chance of stumble on high-speed impact
                        if ((Math.abs(impulse) > 90) && rng.nextDouble() < 0.05) {
                            Player tumble = rng.nextBoolean() ? a : b;
                            tumble.stumble(0.6);
                            addText("Ouch!", tumble.pos.x, tumble.pos.y - 20, Color.WHITE);
                            sfx.beepTackle();
                            cameraShake(0.25, 2.0);
                        }
                    }
                }
            }
        }

        private void keepPlayerInBounds(Player p) {
            // Players cannot enter out of field; allow goalies to cross a bit into goal area
            double margin = p.isGoalie ? 2 : 0;
            if (p.pos.x - p.radius < court.playLeft + margin) {
                p.pos.x = court.playLeft + p.radius + margin;
                p.vel.x *= -0.3;
            }
            if (p.pos.x + p.radius > court.playRight - margin) {
                p.pos.x = court.playRight - p.radius - margin;
                p.vel.x *= -0.3;
            }
            if (p.pos.y - p.radius < court.playTop + margin) {
                p.pos.y = court.playTop + p.radius + margin;
                p.vel.y *= -0.3;
            }
            if (p.pos.y + p.radius > court.playBottom - margin) {
                p.pos.y = court.playBottom - p.radius - margin;
                p.vel.y *= -0.3;
            }
        }

        private boolean insideGoalY(double y, Goal goal) {
            return y >= goal.y && y <= goal.y + goal.h;
        }

        private void checkGoals() {
            // If ball passes into goal rectangles and within mouth, score
            if (ball.carrier != null) return;

            // Left goal
            if (ball.pos.x - ball.radius < court.goalLeft.x + court.goalDepth) {
                if (insideGoalY(ball.pos.y, court.goalLeft)) {
                    // Determine if fully crossed goal line
                    if (ball.pos.x < court.goalLeft.x + court.goalDepth * 0.5) {
                        score(teamRed, "Red scores!");
                    }
                }
            }

            // Right goal
            if (ball.pos.x + ball.radius > court.goalRight.x) {
                if (insideGoalY(ball.pos.y, court.goalRight)) {
                    if (ball.pos.x > court.goalRight.x + court.goalDepth * 0.5) {
                        score(teamBlue, "Blue scores!");
                    }
                }
            }
        }

        private void score(Team scoringTeam, String msg) {
            // Update scoreboard
            scoreboard.addGoal(scoringTeam);

            // Visuals
            addText(msg, court.width / 2.0, court.height * 0.2, scoringTeam.color);
            spawnConfetti(scoringTeam == teamBlue);

            // Sound
            sfx.beepGoal();

            // Camera shake
            cameraShake(0.4, 4.0);

            // Reset kickoff for conceding team
            resetKickoff(scoringTeam);
        }

        private void spawnBounce(double x, double y) {
            for (int i = 0; i < 6; i++) {
                Particle p = new Particle();
                p.pos.set(x, y);
                double a = rng.nextDouble() * Math.PI * 2;
                double sp = 20 + rng.nextDouble() * 80;
                p.vel.set(Math.cos(a) * sp, Math.sin(a) * sp);
                p.life = 0.25 + rng.nextDouble() * 0.35;
                p.size = 2 + rng.nextDouble() * 4;
                p.color = new Color(255, 255, 255, 200);
                particles.add(p);
            }
        }

        private void spawnKick(double x, double y, Vec2 dir) {
            for (int i = 0; i < 16; i++) {
                Particle p = new Particle();
                p.pos.set(x, y);
                double spread = 0.6;
                Vec2 v = new Vec2(dir.x, dir.y);
                // jitter
                double a = (rng.nextDouble() - 0.5) * spread;
                double ca = Math.cos(a), sa = Math.sin(a);
                double vx = v.x * ca - v.y * sa;
                double vy = v.x * sa + v.y * ca;
                double sp = 120 + rng.nextDouble() * 180;
                p.vel.set(vx * sp, vy * sp);
                p.life = 0.3 + rng.nextDouble() * 0.3;
                p.size = 2 + rng.nextDouble() * 3;
                p.color = new Color(255, 240, 180, 200);
                particles.add(p);
            }
        }

        private void spawnBallTrail() {
            Particle p = new Particle();
            p.pos.set(ball.pos.x, ball.pos.y);
            p.vel.set(-ball.vel.x * 0.05, -ball.vel.y * 0.05);
            p.life = 0.15 + rng.nextDouble() * 0.1;
            p.size = 2 + rng.nextDouble() * 2;
            p.color = new Color(255, 255, 255, 130);
            particles.add(p);
        }

        private void spawnConfetti(boolean leftSide) {
            for (int i = 0; i < 200; i++) {
                Particle p = new Particle();
                double x = leftSide ? rng.nextDouble() * (court.width * 0.45) : (court.width * 0.55 + rng.nextDouble() * (court.width * 0.45));
                double y = court.playTop - 10 - rng.nextDouble() * 80;
                p.pos.set(x, y);
                p.vel.set(rng.nextDouble() * 60 - 30, 50 + rng.nextDouble() * 120);
                p.life = 1.5 + rng.nextDouble() * 1.5;
                p.size = 2 + rng.nextDouble() * 3;
                Color[] cols = new Color[]{new Color(255, 80, 80), new Color(80, 160, 255), new Color(255, 230, 90), new Color(120, 255, 120)};
                p.color = cols[rng.nextInt(cols.length)];
                particles.add(p);
            }
        }

        private void cameraShake(double time, double strength) {
            camShakeTime = time;
            camShakeStrength = strength;
        }

        private void addText(String text, double x, double y, Color color) {
            texts.add(new TransientText(text, x, y, color));
        }

        private void updateParticles(double dt) {
            for (Particle p : particles) {
                p.life -= dt;
                p.vel.y += 0; // no gravity in field; confetti is purely velocity-based; but we can add a bit:
                // p.vel.y += 20 * dt;

                p.pos.add(p.vel.x * dt, p.vel.y * dt);
                if (p.life <= 0) {
                    particles.remove(p);
                }
            }
        }

        private void updateTexts(double dt) {
            for (TransientText t : texts) {
                t.life -= dt;
                t.y -= 18 * dt;
                if (t.life <= 0) texts.remove(t);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            // Enable AA
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Scale to fit current panel size while preserving aspect
            double sx = getWidth() / (double) BASE_WIDTH;
            double sy = getHeight() / (double) BASE_HEIGHT;
            double s = Math.min(sx, sy);
            int drawW = (int) (BASE_WIDTH * s);
            int drawH = (int) (BASE_HEIGHT * s);
            int offX = (getWidth() - drawW) / 2;
            int offY = (getHeight() - drawH) / 2;

            // Apply transform
            g2.translate(offX, offY);
            g2.scale(s, s);

            // Camera shake transform
            if (camShakeTime > 0) {
                double k = camShakeTime;
                double dx = (rng.nextDouble() - 0.5) * camShakeStrength * k * 2;
                double dy = (rng.nextDouble() - 0.5) * camShakeStrength * k * 2;
                g2.translate(dx, dy);
            }

            // Draw court
            court.draw(g2);

            // Draw goals
            court.drawGoals(g2);

            // Draw ball shadow
            g2.setColor(new Color(0, 0, 0, 60));
            double shadowR = ball.radius + 1;
            g2.fill(new Ellipse2D.Double(ball.pos.x - shadowR, ball.pos.y - shadowR + 3, shadowR * 2, shadowR * 2));

            // Particles under players
            for (Particle p : particles) {
                g2.setColor(applyAlpha(p.color, (float) Math.max(0, Math.min(1, p.life))));
                double r = p.size;
                g2.fill(new Ellipse2D.Double(p.pos.x - r, p.pos.y - r, r * 2, r * 2));
            }

            // Draw players
            for (Player p : players) {
                p.draw(g2);
            }

            // Draw ball
            ball.draw(g2);

            // HUD overlay
            drawHUD(g2);

            // Draw texts
            for (TransientText t : texts) {
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
                g2.setColor(new Color(0, 0, 0, 160));
                g2.drawString(t.text, (float) (t.x + 2), (float) (t.y + 2));
                g2.setColor(applyAlpha(t.color, (float) Math.max(0, Math.min(1, t.life))));
                g2.drawString(t.text, (float) t.x, (float) t.y);
            }

            // Pause overlay
            if (paused) {
                drawPauseOverlay(g2);
            }

            // Countdown overlay
            if (countdown) {
                drawCountdown(g2);
            }

            // Help overlay initially
            if (showHelp && infoOverlayTimer > 0) {
                drawHelpOverlay(g2);
            }

            g2.dispose();
        }

        private void drawHUD(Graphics2D g2) {
            // Scoreboard
            int barW = 260;
            int barH = 36;

            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect((int) (court.width / 2 - barW - 10), 10, barW * 2 + 20, barH + 40, 20, 20);

            // Team names and scores
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22f));
            g2.setColor(teamBlue.color);
            drawStringCentered(g2, teamBlue.name, court.width / 2 - 180, 36);
            g2.setColor(Color.WHITE);
            drawStringCentered(g2, String.valueOf(scoreboard.goalsBlue), court.width / 2 - 100, 36);

            g2.setColor(teamRed.color);
            drawStringCentered(g2, teamRed.name, court.width / 2 + 180, 36);
            g2.setColor(Color.WHITE);
            drawStringCentered(g2, String.valueOf(scoreboard.goalsRed), court.width / 2 + 100, 36);

            // Clock
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
            drawStringCentered(g2, clock.toDisplayString(), court.width / 2, 36);

            // FPS small
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
            g2.setColor(new Color(255, 255, 255, 160));
            g2.drawString("FPS: " + (int) fps, 10, 16);

            // Possession indicator
            String possession = (ball.carrier != null) ? ball.carrier.team.name + " ball" : "Loose ball";
            Color posCol = (ball.carrier != null) ? ball.carrier.team.color : Color.LIGHT_GRAY;
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
            g2.setColor(posCol);
            drawStringCentered(g2, possession, court.width / 2, 64);

            // Show controlled player and stamina
            if (humanControlled != null) {
                g2.setColor(new Color(0, 0, 0, 140));
                g2.fillRoundRect(10, court.height - 70, 320, 60, 16, 16);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
                g2.setColor(Color.WHITE);
                g2.drawString("You: " + humanControlled.name + (humanControlled.isGoalie ? " (GK)" : ""), 20, court.height - 44);

                // Stamina bar
                double st = humanControlled.stamina / humanControlled.maxStamina;
                int w = 280;
                int h = 12;
                int x = 20;
                int y = court.height - 28;
                g2.setColor(new Color(60, 60, 60, 160));
                g2.fillRoundRect(x - 2, y - 2, w + 4, h + 4, 10, 10);
                g2.setColor(new Color(80, 140, 255, 220));
                g2.fillRoundRect(x, y, (int) (w * st), h, 8, 8);
                g2.setColor(Color.WHITE);
                g2.drawRect(x - 2, y - 2, w + 4, h + 4);
            }

            // End match banner
            if (clock.isFinished()) {
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 40f));
                String end = "Full Time";
                drawBanner(g2, end, court.width / 2, court.height / 2 - 50, new Color(0, 0, 0, 150), Color.WHITE);

                String result;
                if (scoreboard.goalsBlue > scoreboard.goalsRed) result = "Blue Wins!";
                else if (scoreboard.goalsRed > scoreboard.goalsBlue) result = "Red Wins!";
                else result = "Draw";

                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
                drawBanner(g2, result, court.width / 2, court.height / 2, new Color(0, 0, 0, 120), Color.WHITE);

                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
                g2.setColor(new Color(255, 255, 255, 200));
                drawStringCentered(g2, "Press R to restart", court.width / 2, court.height / 2 + 40);
            }
        }

        private void drawPauseOverlay(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRect(0, 0, court.width, court.height);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 42f));

            float a = (float) (0.7 + 0.3 * Math.sin(pauseHintPulse));
            drawBanner(g2, "Paused", court.width / 2, court.height / 2 - 30, new Color(0f, 0f, 0f, 0.0f), new Color(1f, 1f, 1f, a));

            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));
            drawStringCentered(g2, "Press P to resume", court.width / 2, court.height / 2 + 10);
        }

        private void drawCountdown(Graphics2D g2) {
            double t = Math.ceil(countdownTime);
            String msg;
            if (t <= 0) msg = "Go!";
            else msg = String.valueOf((int) t);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 64f));
            drawBanner(g2, msg, court.width / 2, court.height / 2, new Color(0, 0, 0, 80), Color.WHITE);
        }

        private void drawHelpOverlay(Graphics2D g2) {
            int w = 520;
            int h = 150;
            int x = court.width / 2 - w / 2;
            int y = court.height - h - 20;
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect(x, y, w, h, 16, 16);
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
            g2.drawString("Controls", x + 18, y + 26);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
            int yy = y + 48;
            g2.drawString("- Move: W/A/S/D", x + 18, yy); yy += 18;
            g2.drawString("- Sprint: Left Shift", x + 18, yy); yy += 18;
            g2.drawString("- Aim: Mouse cursor; Hold and release Space to pass/shot (hold longer = stronger)", x + 18, yy); yy += 18;
            g2.drawString("- Switch player: Tab", x + 18, yy); yy += 18;
            g2.drawString("- Pause: P   Reset: R", x + 18, yy);

            // Fade-out
        }

        private void drawStringCentered(Graphics2D g2, String text, double x, double y) {
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(text);
            int h = fm.getAscent();
            g2.drawString(text, (int) (x - w / 2.0), (int) (y + h / 4.0));
        }

        private void drawBanner(Graphics2D g2, String text, double cx, double cy, Color bg, Color fg) {
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(text);
            int h = fm.getAscent();
            int pad = 14;
            int x = (int) (cx - w / 2.0 - pad);
            int y = (int) (cy - h - pad / 2.0);
            g2.setColor(bg);
            g2.fillRoundRect(x, y, w + pad * 2, h + pad, 16, 16);
            g2.setColor(fg);
            g2.drawString(text, (int) (cx - w / 2.0), (int) (cy));
        }

        private static Color applyAlpha(Color c, float alpha) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha * 255));
        }

        // Input events
        @Override
        public void keyTyped(KeyEvent e) {}
        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            if (code >= 0 && code < keys.length) {
                KeyEdgeTracker.update(code, keys[code], true);
                keys[code] = true;
                // Hide help on any key input
                showHelp = false;
            }
        }
        @Override
        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();
            if (code >= 0 && code < keys.length) {
                KeyEdgeTracker.update(code, keys[code], false);
                keys[code] = false;
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {}
        @Override
        public void mousePressed(MouseEvent e) {
            showHelp = false;
        }
        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseEntered(MouseEvent e) { mouseInside = true; }
        @Override
        public void mouseExited(MouseEvent e) { mouseInside = false; }
        @Override
        public void mouseDragged(MouseEvent e) {
            updateMouse(e);
        }
        @Override
        public void mouseMoved(MouseEvent e) {
            updateMouse(e);
        }

        private void updateMouse(MouseEvent e) {
            // Convert to base coordinates
            double sx = getWidth() / (double) BASE_WIDTH;
            double sy = getHeight() / (double) BASE_HEIGHT;
            double s = Math.min(sx, sy);
            int drawW = (int) (BASE_WIDTH * s);
            int drawH = (int) (BASE_HEIGHT * s);
            int offX = (getWidth() - drawW) / 2;
            int offY = (getHeight() - drawH) / 2;
            int x = (int) ((e.getX() - offX) / s);
            int y = (int) ((e.getY() - offY) / s);
            mousePos = new Point(x, y);
            showHelp = false;
        }

        @Override
        public void focusGained(FocusEvent e) {}
        @Override
        public void focusLost(FocusEvent e) {
            paused = true;
        }
    }

    // ============================================================
    // Classes and helpers
    // ============================================================

    static class Court {
        int width, height;
        int margin = 40;

        // Playable area within margins
        int playLeft, playRight, playTop, playBottom;

        Goal goalLeft, goalRight;
        int goalDepth = 18;

        // Lines
        Color courtGreen = new Color(40, 150, 60);
        Color lineWhite = new Color(240, 240, 240);
        Color centerCircle = new Color(255, 255, 255, 100);

        public Court(int width, int height) {
            this.width = width;
            this.height = height;
            this.playLeft = margin + 8;
            this.playRight = width - margin - 8;
            this.playTop = margin + 8;
            this.playBottom = height - margin - 8;

            // Handball-ish goals at center of left/right
            int goalW = 12;
            int goalH = 140; // mouth height
            int gy = height / 2 - goalH / 2;
            goalLeft = new Goal(playLeft - goalW - goalDepth, gy, goalW + goalDepth, goalH, true, playLeft - goalDepth, gy);
            goalRight = new Goal(playRight, gy, goalW + goalDepth, goalH, false, playRight, gy);
        }

        public void draw(Graphics2D g2) {
            // Background
            g2.setColor(courtGreen);
            g2.fillRect(0, 0, width, height);

            // Outer boundary
            g2.setStroke(new BasicStroke(6));
            g2.setColor(lineWhite);
            g2.drawRect(playLeft, playTop, playRight - playLeft, playBottom - playTop);

            // Center line
            g2.setStroke(new BasicStroke(4));
            g2.drawLine(width / 2, playTop, width / 2, playBottom);

            // Center circle
            g2.setColor(centerCircle);
            g2.fill(new Ellipse2D.Double(width / 2 - 60, height / 2 - 60, 120, 120));
            g2.setColor(lineWhite);
            g2.setStroke(new BasicStroke(2));
            g2.draw(new Ellipse2D.Double(width / 2 - 60, height / 2 - 60, 120, 120));

            // Semi-circles near goals (free-throw line; stylized)
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[]{10, 10}, 0));
            g2.draw(new Arc2D.Double(playLeft + 80 - 140, height / 2.0 - 140, 280, 280, -90, 180, Arc2D.OPEN));
            g2.draw(new Arc2D.Double(playRight - 80 - 140, height / 2.0 - 140, 280, 280, 90, 180, Arc2D.OPEN));
        }

        public void drawGoals(Graphics2D g2) {
            // Draw simple goals: posts at the line positions
            drawGoal(g2, goalLeft);
            drawGoal(g2, goalRight);
        }

        private void drawGoal(Graphics2D g2, Goal goal) {
            // Goal mouth rectangle (for reference)
            // Mouth posts
            g2.setStroke(new BasicStroke(6));
            g2.setColor(Color.WHITE);
            if (goal.isLeft) {
                // Post at inner and outer edges
                g2.drawLine(goal.x + goal.depth, goal.y, goal.x + goal.depth, goal.y + goal.h);
            } else {
                g2.drawLine(goal.x, goal.y, goal.x, goal.y + goal.h);
            }

            // Crossbar shading (just decorative)
            g2.setColor(new Color(255, 255, 255, 120));
            if (goal.isLeft) {
                g2.drawLine(goal.x + goal.depth, goal.y, goal.x + goal.depth + 20, goal.y);
                g2.drawLine(goal.x + goal.depth, goal.y + goal.h, goal.x + goal.depth + 20, goal.y + goal.h);
            } else {
                g2.drawLine(goal.x, goal.y, goal.x - 20, goal.y);
                g2.drawLine(goal.x, goal.y + goal.h, goal.x - 20, goal.y + goal.h);
            }
        }
    }

    static class Goal {
        int x, y, w, h;
        int depth; // how far behind goal line
        boolean isLeft;
        int mouthX, mouthY;

        public Goal(int x, int y, int w, int h, boolean isLeft, int mouthX, int mouthY) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.isLeft = isLeft;
            this.mouthX = mouthX;
            this.mouthY = mouthY;
            this.depth = w; // reuse
        }

        public double getCenterX() {
            return x + w / 2.0;
        }
    }

    static class Ball {
        Vec2 pos = new Vec2(0, 0);
        Vec2 vel = new Vec2(0, 0);
        double radius = 7;
        double spin = 0;
        Player carrier = null;
        double pickupCooldown = 0; // cannot be picked immediately after release
        Team lastTouchTeam = null;

        public void draw(Graphics2D g2) {
            // Ball body
            g2.setColor(new Color(255, 200, 60));
            g2.fill(new Ellipse2D.Double(pos.x - radius, pos.y - radius, radius * 2, radius * 2));

            // Seams (handball-like hex seams stylized)
            g2.setColor(new Color(130, 80, 20, 180));
            double r = radius * 0.7;
            g2.draw(new Ellipse2D.Double(pos.x - r, pos.y - r, r * 2, r * 2));
        }
    }

    static class Team {
        String name;
        Color color;
        List<Player> players = new ArrayList<>();

        public Team(String name, Color color) {
            this.name = name;
            this.color = color;
        }
    }

    enum PlayerIntent {
        Idle, MoveTo, ChaseBall, Attack, Defend
    }

    static class Player {
        int id;
        String name;
        boolean isGoalie;
        Team team;
        Vec2 pos = new Vec2();
        Vec2 vel = new Vec2();
        Vec2 acc = new Vec2();
        Vec2 home = new Vec2();
        Vec2 targetPos = new Vec2();

        double radius = 14;
        double catchRadius = 22;
        double baseSpeed = 220;
        double sprintSpeed = 320;
        double stamina = 100;
        double maxStamina = 100;
        double staminaRegen = 18; // per second
        double cooldown = 0;
        double tackleCooldown = 0;
        double switchCooldown = 0;
        boolean recovering = false;
        double stumbleTimer = 0;
        double fatigue = 0; // increases with sprints; reduces performance slightly

        boolean isHumanControlled = false;
        BasicAI ai;
        PlayerIntent intent = PlayerIntent.Idle;

        // For pass assist indicator displayed by AI maybe
        Player passAssist;

        public Player(int id, String name, boolean isGoalie, Team team) {
            this.id = id;
            this.name = name;
            this.isGoalie = isGoalie;
            this.team = team;
        }

        public void updateAI(double dt, Ball ball, List<Player> all, Court court, Scoreboard sb, MatchClock clock, Random rng) {
            if (isHumanControlled) {
                // Movement applied via input handler through applyMovement
                // But add some auto-aim orientation or subtle smoothing
                // Nothing else necessary
                return;
            }
            // Let AI decide movement and actions
            if (ai != null) ai.update(dt, ball, all, court, sb, clock, rng);
        }

        public void applyMovement(Vec2 input, boolean sprint) {
            if (recovering) return;

            // Normalize input
            if (input.length() > 1e-6) input.normalize();
            double spd = sprint ? sprintSpeed : baseSpeed;

            // Fatigue reduces effective speed slightly
            double fatigueFactor = 1.0 - Math.min(0.3, fatigue * 0.1);
            spd *= fatigueFactor;

            acc.add(input.x * spd * 6, input.y * spd * 6);

            // Stamina drain if sprinting or carrying ball
            if (sprint) {
                stamina -= 28 * (1.0 + (carrier() != null ? 0.3 : 0)) * (1.0 / 60.0);
                fatigue += 0.004;
            } else {
                fatigue -= 0.002;
            }
            if (fatigue < 0) fatigue = 0;
            if (fatigue > 3) fatigue = 3;
        }

        public void updateStamina(double dt) {
            // Passive regen
            if (!recovering) {
                stamina += staminaRegen * dt;
            } else {
                stamina += staminaRegen * dt * 0.5;
            }
            if (stamina > maxStamina) stamina = maxStamina;
            if (stamina < 0) stamina = 0;
        }

        public void stumble(double time) {
            recovering = true;
            stumbleTimer = time;
            vel.scale(0.2);
        }

        public double getMaxSpeed() {
            double bonus = (carrier() != null ? -30 : 0);
            double s = baseSpeed + bonus;
            double staminaFactor = 0.7 + 0.3 * (stamina / maxStamina);
            return Math.max(80, (s * staminaFactor));
        }

        public void draw(Graphics2D g2) {
            // Shadow
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fill(new Ellipse2D.Double(pos.x - radius, pos.y - radius + 5, radius * 2, radius * 2));

            // Player circle
            g2.setColor(team.color);
            g2.fill(new Ellipse2D.Double(pos.x - radius, pos.y - radius, radius * 2, radius * 2));

            // Border
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(isHumanControlled ? 3 : 2));
            g2.draw(new Ellipse2D.Double(pos.x - radius, pos.y - radius, radius * 2, radius * 2));

            // Jersey number (id)
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            String num = String.valueOf(id);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(num, (float) (pos.x - fm.stringWidth(num) / 2.0), (float) (pos.y + fm.getAscent() / 3.0));

            // Stamina ring
            double k = stamina / maxStamina;
            g2.setColor(new Color(255, 255, 255, 120));
            g2.setStroke(new BasicStroke(2));
            double a = 360 * k;
            g2.draw(new Arc2D.Double(pos.x - radius - 4, pos.y - radius - 4, (radius + 4) * 2, (radius + 4) * 2, 90, -a, Arc2D.OPEN));

            // Ball indicator if carrying
            if (carrier() == this) {
                g2.setColor(new Color(255, 255, 255, 160));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Ellipse2D.Double(pos.x - radius - 8, pos.y - radius - 8, (radius + 8) * 2, (radius + 8) * 2));
            }

            // Human-controlled marker
            if (isHumanControlled) {
                g2.setColor(new Color(255, 255, 255, 200));
                g2.fill(new Polygon(
                        new int[]{(int) pos.x, (int) pos.x - 6, (int) pos.x + 6},
                        new int[]{(int) (pos.y - radius - 16), (int) (pos.y - radius - 4), (int) (pos.y - radius - 4)}, 3));
            }
        }

        public void grabBall(Ball ball) {
            ball.carrier = this;
        }

        public void releaseBall() {
            // Nothing for player itself; handled on ball
        }

        private Player carrier() {
            // If this player carries the ball
            return (this == (ballRef != null ? ballRef.carrier : null)) ? this : null;
        }

        // ballRef is used only by carrier() helper; set by external update cycle for convenience
        private transient Ball ballRef;
    }

    // Basic AI: will move to ball or defend, attempt passes/shots.
    static class BasicAI {
        private final Player self;
        private final Court court;
        private final Ball ball;
        private final List<Player> all;
        private final int difficulty;
        private final Random rng = new Random();

        private double thinkTimer = 0;

        public BasicAI(Player self, Court court, Ball ball, List<Player> all, int difficulty) {
            this.self = self;
            this.court = court;
            this.ball = ball;
            this.all = all;
            this.difficulty = difficulty;
        }

        public void update(double dt, Ball ball, List<Player> all, Court court, Scoreboard sb, MatchClock clock, Random globalRng) {
            // Attach ballRef for helper in Player
            self.ballRef = ball;

            thinkTimer -= dt;
            if (thinkTimer <= 0) {
                thinkTimer = 0.1 + rng.nextDouble() * 0.15; // re-evaluate roughly 6-10 times per second
                decide(ball, all, court);
            }

            // Apply movement toward target
            Vec2 dir = new Vec2(self.targetPos.x - self.pos.x, self.targetPos.y - self.pos.y);
            if (dir.length() > 1) dir.normalize();
            boolean sprint = rng.nextDouble() < 0.4 && self.stamina > 30;
            self.applyMovement(dir, sprint);

            // If carrying ball, maybe pass or shoot
            if (ball.carrier == self && self.cooldown <= 0) {
                Team me = self.team;
                // Decide if shooting
                boolean shootingPosition = isShootingPosition();
                if (shootingPosition && rng.nextDouble() < shootLikelihood()) {
                    // Shoot toward opponent goal center with slight offset
                    Goal targetGoal = (me == getBlueTeam()) ? getCourt().goalRight : getCourt().goalLeft;
                    Vec2 goalCenter = new Vec2(targetGoal.isLeft ? targetGoal.x + 4 : targetGoal.x - 4, targetGoal.y + targetGoal.h * 0.5 + rng.nextDouble() * 30 - 15);
                    shootTowards(goalCenter, true);
                } else {
                    // Maybe pass to a teammate closer to goal or in space
                    Player mate = findPassMate();
                    if (mate != null && rng.nextDouble() < passLikelihood()) {
                        passTo(mate);
                    } else {
                        // Dribble: move toward space
                        Vec2 tp = targetAttackPoint();
                        self.targetPos.set(tp);
                    }
                }
            }
        }

        private double passLikelihood() {
            return switch (difficulty) {
                case 0 -> 0.25;
                case 1 -> 0.45;
                default -> 0.65;
            };
        }

        private double shootLikelihood() {
            return switch (difficulty) {
                case 0 -> 0.20;
                case 1 -> 0.35;
                default -> 0.52;
            };
        }

        private boolean isShootingPosition() {
            // Consider x proximity to opponent goal
            boolean blue = self.team == getBlueTeam();
            if (blue) {
                return self.pos.x > court.width * 0.64;
            } else {
                return self.pos.x < court.width * 0.36;
            }
        }

        private Vec2 targetAttackPoint() {
            boolean blue = self.team == getBlueTeam();
            double tx = blue ? court.width * (0.62 + rng.nextDouble() * 0.3) : court.width * (0.38 - rng.nextDouble() * 0.3);
            double ty = court.height * (0.3 + rng.nextDouble() * 0.4);
            return new Vec2(tx, ty);
        }

        private Team getBlueTeam() {
            // Find by comparing colors or name; in this single-file, simplest:
            // The static approach isn't possible here; rely on the side: left vs right
            // But we know self.team is accurate,
            return self.team; // This method is redundant in this context, kept for clarity
        }

        private Court getCourt() { return court; }

        private void decide(Ball ball, List<Player> all, Court court) {
            // If ball is loose, closest player chases
            if (ball.carrier == null) {
                Player closest = nearestTo(ball.pos.x, ball.pos.y, self.team.players);
                if (closest == self) {
                    self.intent = PlayerIntent.ChaseBall;
                    self.targetPos.set(ball.pos.x, ball.pos.y);
                } else {
                    // Move to supportive position
                    double sideX = (self.team == getBlueTeam()) ? court.width * 0.46 : court.width * 0.54;
                    double y = self.home.y + (rng.nextDouble() * 60 - 30);
                    self.targetPos.set(sideX, y);
                }
                return;
            }

            // If teammate has ball
            if (ball.carrier.team == self.team) {
                if (ball.carrier == self) {
                    // Move towards attack
                    Vec2 tp = targetAttackPoint();
                    self.targetPos.set(tp);
                } else {
                    // Support: find space ahead
                    double aheadX = (self.team == getBlueTeam()) ? self.pos.x + 70 : self.pos.x - 70;
                    aheadX = clamp(aheadX, court.playLeft + 20, court.playRight - 20);
                    double y = clamp(self.pos.y + (rng.nextDouble() * 120 - 60), court.playTop + 20, court.playBottom - 20);
                    self.targetPos.set(aheadX, y);
                }
                return;
            }

            // Opponent has ball
            if (ball.carrier.team != self.team) {
                // Defend: move between ball and goal center
                Goal ownGoal = (self.team == getBlueTeam()) ? court.goalLeft : court.goalRight;
                Vec2 g = new Vec2(ownGoal.isLeft ? ownGoal.x + ownGoal.depth : ownGoal.x, ownGoal.y + ownGoal.h * 0.5);
                Vec2 b = new Vec2(ball.pos.x, ball.pos.y);
                Vec2 mid = new Vec2((g.x * 0.7 + b.x * 0.3), (g.y * 0.7 + b.y * 0.3));
                self.targetPos.set(mid);
                self.intent = PlayerIntent.Defend;
                return;
            }

            self.intent = PlayerIntent.Idle;
            self.targetPos.set(self.home);
        }

        private Player nearestTo(double x, double y, List<Player> list) {
            Player best = null;
            double bestD2 = 1e12;
            for (Player p : list) {
                double dx = p.pos.x - x;
                double dy = p.pos.y - y;
                double d2 = dx * dx + dy * dy;
                if (d2 < bestD2) {
                    bestD2 = d2;
                    best = p;
                }
            }
            return best;
        }

        private void shootTowards(Vec2 target, boolean strong) {
            // Release ball with some power depending on distance or flag
            double dist = new Vec2(target.x - self.pos.x, target.y - self.pos.y).length();
            double base = strong ? 720 : 520;
            double power = base + Math.min(680, dist * 6);

            Vec2 dir = new Vec2(target.x - self.pos.x, target.y - self.pos.y);
            if (dir.length() < 0.001) dir.set(1, 0); else dir.normalize();

            if (self == ball.carrier) {
                ball.carrier = null;
                ball.vel.set(dir.x * power + self.vel.x * 0.25, dir.y * power + self.vel.y * 0.25);
                ball.pos.set(self.pos.x + dir.x * (self.radius + ball.radius + 2), self.pos.y + dir.y * (self.radius + ball.radius + 2));
                ball.pickupCooldown = 0.15;
                ball.lastTouchTeam = self.team;
                self.cooldown = 0.2;
            }
        }

        private void passTo(Player mate) {
            Vec2 target = new Vec2(mate.pos.x, mate.pos.y);
            // Lead pass slightly
            target.add(mate.vel.x * 0.25, mate.vel.y * 0.25);

            // Power
            double dist = new Vec2(target.x - self.pos.x, target.y - self.pos.y).length();
            double base = 380;
            double power = base + Math.min(300, dist * 3);

            Vec2 dir = new Vec2(target.x - self.pos.x, target.y - self.pos.y);
            if (dir.length() < 0.001) dir.set(1, 0); else dir.normalize();

            if (self == ball.carrier) {
                ball.carrier = null;
                ball.vel.set(dir.x * power + self.vel.x * 0.15, dir.y * power + self.vel.y * 0.15);
                ball.pos.set(self.pos.x + dir.x * (self.radius + ball.radius + 2), self.pos.y + dir.y * (self.radius + ball.radius + 2));
                ball.pickupCooldown = 0.12;
                ball.lastTouchTeam = self.team;
                self.cooldown = 0.15;
                self.passAssist = mate;
            }
        }

        private Player findPassMate() {
            Player best = null;
            double bestScore = -1e9;
            for (Player p : self.team.players) {
                if (p == self) continue;
                if (p.isGoalie) continue; // avoid passing to GK
                double dx = p.pos.x - self.pos.x;
                double dy = p.pos.y - self.pos.y;
                double d2 = dx * dx + dy * dy;
                if (d2 < 20 * 20) continue; // too close
                // Score: closer to opponent goal better, less defenders near better
                double goalX = (self.team == getBlueTeam()) ? court.goalRight.x : court.goalLeft.x + court.goalLeft.depth;
                double advance = (self.team == getBlueTeam()) ? p.pos.x : (court.width - p.pos.x);
                double defenderPenalty = defendersNear(p) * 30;
                double score = advance * 0.4 - Math.sqrt(d2) * 0.2 - defenderPenalty;
                if (score > bestScore) {
                    bestScore = score;
                    best = p;
                }
            }
            return best;
        }

        private int defendersNear(Player p) {
            int count = 0;
            for (Player other : all) {
                if (other.team != self.team) {
                    double dx = other.pos.x - p.pos.x;
                    double dy = other.pos.y - p.pos.y;
                    if (dx * dx + dy * dy < 90 * 90) count++;
                }
            }
            return count;
        }

        private static double clamp(double v, double a, double b) {
            return Math.max(a, Math.min(b, v));
        }
    }

    static class Vec2 {
        double x, y;
        Vec2() { this(0, 0); }
        Vec2(double x, double y) { this.x = x; this.y = y; }
        Vec2(Vec2 o) { this.x = o.x; this.y = o.y; }
        void set(double x, double y) { this.x = x; this.y = y; }
        void set(Vec2 v) { this.x = v.x; this.y = v.y; }
        void add(double x, double y) { this.x += x; this.y += y; }
        void add(Vec2 v) { this.x += v.x; this.y += v.y; }
        void scale(double s) { this.x *= s; this.y *= s; }
        double length() { return Math.sqrt(x * x + y * y); }
        void normalize() { double l = length(); if (l > 1e-8) { x /= l; y /= l; } }
        @Override public String toString() { return String.format(Locale.US, "(%.2f, %.2f)", x, y); }
    }

    static class Particle {
        Vec2 pos = new Vec2();
        Vec2 vel = new Vec2();
        double size = 3;
        double life = 1;
        Color color = Color.WHITE;
    }

    static class TransientText {
        String text;
        double x, y;
        double life = 1.2;
        Color color;
        public TransientText(String t, double x, double y, Color c) {
            this.text = t;
            this.x = x; this.y = y;
            this.color = c;
        }
    }

    static class Scoreboard {
        int goalsBlue = 0;
        int goalsRed = 0;

        public void addGoal(Team t) {
            if (t.name.equalsIgnoreCase("Blue")) goalsBlue++;
            else goalsRed++;
        }

        public void reset() {
            goalsBlue = 0; goalsRed = 0;
        }
    }

    static class MatchClock {
        final double duration; // seconds
        double timeLeft;
        boolean running = true;
        public MatchClock(double durationSec) {
            this.duration = durationSec;
            this.timeLeft = durationSec;
        }
        public void update(double dt) {
            if (!running) return;
            timeLeft -= dt;
            if (timeLeft <= 0) {
                timeLeft = 0;
                running = false;
            }
        }
        public boolean isFinished() { return !running && timeLeft <= 0; }
        public String toDisplayString() {
            int t = (int) Math.ceil(timeLeft);
            int m = t / 60;
            int s = t % 60;
            return String.format("%02d:%02d", m, s);
        }
        public void reset() { timeLeft = duration; running = true; }
    }

    static class SFX {
        // Minimal beeps using Toolkit to avoid external deps
        public void beepShot() { Toolkit.getDefaultToolkit().beep(); }
        public void beepPass() {  }
        public void beepPickup() {  }
        public void beepTackle() { }
        public void beepGoal() { Toolkit.getDefaultToolkit().beep(); }
    }

    // Edge tracker for keys
    static class KeyEdgeTracker {
        private static final Map<Integer, Boolean> lastState = new HashMap<>();
        private static final Map<Integer, Long> debounce = new HashMap<>();
        private static final long debounceMs = 110;

        public static boolean justPressed(int keyCode, boolean currentDown) {
            boolean last = lastState.getOrDefault(keyCode, false);
            long now = System.currentTimeMillis();
            if (currentDown && !last) {
                debounce.put(keyCode, now);
                lastState.put(keyCode, currentDown);
                return true;
            }
            // Reset last state if released
            if (!currentDown && last) {
                lastState.put(keyCode, false);
                return false;
            }
            // Debounce repeated triggers
            long lastMs = debounce.getOrDefault(keyCode, 0L);
            if (currentDown && (now - lastMs) > debounceMs) {
                debounce.put(keyCode, now);
                return true;
            }
            return false;
        }

        public static void update(int keyCode, boolean previousDown, boolean newDown) {
            lastState.put(keyCode, newDown);
            if (newDown && !previousDown) {
                debounce.put(keyCode, System.currentTimeMillis());
            }
        }
    }
}