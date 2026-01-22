import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.*;

public class ExtremeSnowboardingGame extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ExtremeSnowboardingGame game = new ExtremeSnowboardingGame();
            game.setVisible(true);
        });
    }

    public ExtremeSnowboardingGame() {
        super("Extreme Snowboarding Game - Single File");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        GamePanel panel = new GamePanel();
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
        panel.start();
    }

    // GamePanel contains the entire game world, logic, and rendering.
    static class GamePanel extends JPanel implements Runnable, KeyListener, ComponentListener, MouseListener {

        // --------------- Configuration constants ---------------
        private static final int DEFAULT_WIDTH = 1280;
        private static final int DEFAULT_HEIGHT = 720;
        private static final double FIXED_TIMESTEP = 1.0 / 120.0; // seconds per update step
        private static final int MAX_FRAME_SKIP = 5; // allow skip up to 5 updates if rendering lags
        private static final double GRAVITY = 1200.0; // px/s^2 downward
        private static final double AIR_DRAG = 0.08; // drag coefficient
        private static final double GROUND_FRICTION = 6.0; // friction coefficient when grounded
        private static final double CAMERA_LERP = 0.15; // smoothing for camera following
        private static final double CAMERA_Y_LERP = 0.08;
        private static final double WORLD_SCROLL_MARGIN_X = 0.35; // camera lead factor
        private static final double MAX_SPEED = 1600.0; // px/s cap
        private static final double JUMP_IMPULSE = 520.0; // upward jump velocity
        private static final double TUCK_DRAG_MULT = 0.55; // drag reduced when tucking
        private static final double CARVE_FORCE = 1300.0; // lateral carve force
        private static final double CARVE_DAMP = 0.96;
        private static final double LAND_PARTICLE_IMPULSE_THRESHOLD = 250.0; // speed threshold for landing puff

        // Terrain config
        private static final double TERRAIN_SLOPE = 0.22; // base downward slope factor
        private static final double TERRAIN_AMPLITUDE = 140.0;
        private static final double TERRAIN_FREQ1 = 0.0026;
        private static final double TERRAIN_FREQ2 = 0.0073;
        private static final double TERRAIN_FREQ3 = 0.013;
        private static final double TERRAIN_WOBBLE = 0.0015; // slow offset drift for variety

        // Spawning config
        private static final double OBSTACLE_SPAWN_DIST_MIN = 600.0;
        private static final double OBSTACLE_SPAWN_DIST_MAX = 1100.0;
        private static final double COIN_SPAWN_CHANCE = 0.55;
        private static final double RAMP_SPAWN_CHANCE = 0.22;
        private static final double TREE_SPAWN_CHANCE = 0.42;
        private static final double ROCK_SPAWN_CHANCE = 0.35;
        private static final double FLAG_SPAWN_CHANCE = 0.25;

        // Rendering config
        private static final Color SKY_TOP = new Color(100, 155, 255);
        private static final Color SKY_BOTTOM = new Color(200, 230, 255);
        private static final Color SNOW_COLOR = new Color(245, 247, 250);
        private static final Color SNOW_SHADOW = new Color(220, 230, 240);
        private static final Color BOARD_COLOR = new Color(220, 20, 60); // crimson
        private static final Color RIDER_COLOR = new Color(15, 15, 22);
        private static final Font HUD_FONT = new Font("Consolas", Font.PLAIN, 16);
        private static final Font BIG_FONT = new Font("SansSerif", Font.BOLD, 48);
        private static final Font MED_FONT = new Font("SansSerif", Font.BOLD, 24);

        // Snowfall and parallax config
        private static final int SNOWFLAKE_COUNT = 220;
        private static final double SNOWFLAKE_SPEED_MIN = 25.0;
        private static final double SNOWFLAKE_SPEED_MAX = 85.0;

        // --------------- Game state fields ---------------
        private Thread gameThread;
        private volatile boolean running = false;
        private volatile boolean focused = true;

        private long lastNanoTime;
        private double accumulator = 0.0;
        private int frames, updates;
        private double fpsTimer;
        private int fps, ups;

        private final Random rng = new Random(1337);

        private State state = State.MENU;

        private int width = DEFAULT_WIDTH;
        private int height = DEFAULT_HEIGHT;

        // Game world
        private Camera camera = new Camera();
        private Terrain terrain = new Terrain();
        private Player player = new Player();
        private ParticleSystem particles = new ParticleSystem();
        private Parallax parallax = new Parallax();
        private HUD hud = new HUD();

        private List<Obstacle> obstacles = new ArrayList<>();
        private List<Collectible> collectibles = new ArrayList<>();

        private double nextSpawnX = 800.0;

        // Toggles
        private boolean showMountains = true;
        private boolean showClouds = true;
        private boolean showSnowfall = true;
        private boolean showDebug = false;

        // Inputs
        private boolean leftPressed, rightPressed, upPressed, spacePressed;

        // Score, distance, coins
        private double distanceTraveled = 0.0;
        private int score = 0;
        private int coins = 0;

        // Game over state
        private double gameOverTimer = 0.0;

        // --------------- Constructor and setup ---------------
        public GamePanel() {
            setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
            setBackground(Color.BLACK);
            setDoubleBuffered(true);
            setFocusable(true);
            requestFocusInWindow();
            addKeyListener(this);
            addComponentListener(this);
            addMouseListener(this);
        }

        public void start() {
            if (running) return;
            running = true;
            gameThread = new Thread(this, "GameLoop");
            gameThread.start();
        }

        // --------------- Game loop ---------------
        @Override
        public void run() {
            lastNanoTime = System.nanoTime();
            fpsTimer = 0.0;
            frames = 0;
            updates = 0;

            while (running) {
                long now = System.nanoTime();
                double delta = (now - lastNanoTime) / 1_000_000_000.0;
                lastNanoTime = now;

                // Handle window focus for pause prompt
                if (!hasFocus() || !focused) {
                    // Optionally auto-pause, but we just show hint
                }

                accumulator += delta;
                int loops = 0;
                while (accumulator >= FIXED_TIMESTEP && loops < MAX_FRAME_SKIP) {
                    update(FIXED_TIMESTEP);
                    accumulator -= FIXED_TIMESTEP;
                    loops++;
                    updates++;
                }

                repaint();
                frames++;

                fpsTimer += delta;
                if (fpsTimer >= 1.0) {
                    fps = frames;
                    ups = updates;
                    frames = 0;
                    updates = 0;
                    fpsTimer -= 1.0;
                }

                Toolkit.getDefaultToolkit().sync();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
            }
        }

        // --------------- Update ---------------
        private void update(double dt) {
            switch (state) {
                case MENU:
                    updateMenu(dt);
                    break;
                case PLAYING:
                    updatePlaying(dt);
                    break;
                case PAUSED:
                    // just animate background slowly
                    updatePaused(dt);
                    break;
                case GAME_OVER:
                    updateGameOver(dt);
                    break;
            }
        }

        private void updateMenu(double dt) {
            // Animate parallax a bit
            parallax.update(dt, 0.0, 20.0);
            terrain.updateWobble(dt);

            // Idle snow/particles
            if (showSnowfall) parallax.snowfallUpdate(dt, width, height);

            // Basic hover of player for aesthetic
            player.resetForMenu(terrain, width, height, dt);
            camera.follow(player, width, height, true);

            spawnAheadIfNeeded(); // pre-populate a bit
        }

        private void updatePaused(double dt) {
            // continue light parallax movement
            parallax.update(dt, 0.0, 5.0);
            terrain.updateWobble(dt);
            if (showSnowfall) parallax.snowfallUpdate(dt, width, height);
        }

        private void updateGameOver(double dt) {
            parallax.update(dt, player.vx * 0.3, 20.0);
            terrain.updateWobble(dt);
            if (showSnowfall) parallax.snowfallUpdate(dt, width, height);

            particles.update(dt);
            for (Obstacle ob : obstacles) ob.update(dt);

            // Camera gently eases in
            camera.follow(player, width, height, false);
            gameOverTimer += dt;
        }

        private void updatePlaying(double dt) {
            // Controls -> forces
            double steer = 0.0;
            if (leftPressed) steer -= 1.0;
            if (rightPressed) steer += 1.0;
            boolean tuck = upPressed;

            // Update terrain wobble and parallax
            terrain.updateWobble(dt);
            parallax.update(dt, player.vx, 30.0);
            if (showSnowfall) parallax.snowfallUpdate(dt, width, height);

            // Player physics
            player.update(dt, steer, tuck, spacePressed, terrain, particles);

            // Distance/Score
            if (player.vx > 0) {
                distanceTraveled += player.vx * dt;
                score += (int) Math.max(0, player.vx * dt * 0.02);
            }
            if (player.didLand) {
                int trickScore = (int) Math.max(0, Math.min(1000, Math.abs(player.airRotation) * 2.5));
                if (trickScore > 0) {
                    hud.pop("+Trick " + trickScore);
                    score += trickScore;
                }
            }
            player.didLand = false;

            // Obstacles / collectibles
            spawnAheadIfNeeded();
            updateObstacles(dt);
            checkCollisions();

            // Particles
            particles.update(dt);

            // Camera
            camera.follow(player, width, height, false);

            // Death check: off-screen or severe crash
            if (player.dead) {
                state = State.GAME_OVER;
                gameOverTimer = 0.0;
            }
        }

        private void spawnAheadIfNeeded() {
            double targetX = player.x + width * 1.5;
            while (nextSpawnX < targetX) {
                // Determine base y on terrain for this x
                double ty = terrain.heightAt(nextSpawnX);

                // Randomly choose what to spawn at this x
                double roll = rng.nextDouble();
                boolean placed = false;

                if (roll < COIN_SPAWN_CHANCE) {
                    // Spawn a coin arc or a row
                    int coinsInArc = 3 + rng.nextInt(4);
                    double arcHeight = 60 + rng.nextDouble() * 80;
                    double arcWidth = 190 + rng.nextDouble() * 220;
                    for (int i = 0; i < coinsInArc; i++) {
                        double t = i / (double) (coinsInArc - 1);
                        double ax = nextSpawnX + t * arcWidth;
                        double ay = terrain.heightAt(ax) - (Math.sin(t * Math.PI) * arcHeight + 40);
                        collectibles.add(new Coin(ax, ay));
                    }
                    placed = true;
                }

                // Additional obstacle types
                if (!placed && rng.nextDouble() < RAMP_SPAWN_CHANCE) {
                    double angle = Math.toRadians(15 + rng.nextDouble() * 18);
                    double len = 120 + rng.nextDouble() * 110;
                    obstacles.add(new Ramp(nextSpawnX, ty, len, angle));
                    placed = true;
                }

                if (!placed && rng.nextDouble() < TREE_SPAWN_CHANCE) {
                    double scale = 0.8 + rng.nextDouble() * 1.8;
                    obstacles.add(new Tree(nextSpawnX + 50, terrain.heightAt(nextSpawnX + 50), scale));
                    placed = true;
                }

                if (!placed && rng.nextDouble() < ROCK_SPAWN_CHANCE) {
                    double rscale = 0.8 + rng.nextDouble() * 1.3;
                    obstacles.add(new Rock(nextSpawnX + 80, terrain.heightAt(nextSpawnX + 80), rscale));
                    placed = true;
                }

                if (!placed && rng.nextDouble() < FLAG_SPAWN_CHANCE) {
                    obstacles.add(new Flag(nextSpawnX + 60, terrain.heightAt(nextSpawnX + 60)));
                }

                // Advance next spawn
                nextSpawnX += OBSTACLE_SPAWN_DIST_MIN + rng.nextDouble() * (OBSTACLE_SPAWN_DIST_MAX - OBSTACLE_SPAWN_DIST_MIN);
            }

            // Clean up old entities behind camera
            double minX = camera.x - width * 0.6;
            obstacles.removeIf(o -> o.x + 600 < minX);
            collectibles.removeIf(c -> c.x + 600 < minX || c.collected);
        }

        private void updateObstacles(double dt) {
            for (Obstacle o : obstacles) o.update(dt);
            for (Collectible c : collectibles) c.update(dt);
        }

        private void checkCollisions() {
            // Player bounding circle for quick checks
            double pr = player.radius();
            double px = player.x;
            double py = player.y;
            double pr2 = pr * pr;

            // Coins
            for (Collectible c : collectibles) {
                if (c.collected) continue;
                double dx = c.x - px;
                double dy = c.y - py;
                double rr = (c.radius + pr);
                if (dx * dx + dy * dy < rr * rr) {
                    c.collected = true;
                    coins += 1;
                    score += 50;
                    hud.pop("+1 Coin");
                    // small sparkle particles
                    particles.sparkle(c.x, c.y, 10, new Color(255, 225, 0));
                }
            }

            // Obstacles
            for (Obstacle o : obstacles) {
                if (!o.collidable) continue;
                if (o instanceof Ramp) {
                    // Ramp special: if on ramp region and moving upward of ramp normal -> give impulse
                    Ramp r = (Ramp) o;
                    if (r.contains(px, py + 8)) {
                        // push along ramp surface
                        double tangentX = Math.cos(r.angle);
                        double tangentY = -Math.sin(r.angle);
                        double dot = player.vx * tangentX + player.vy * tangentY;
                        if (dot < 0) {
                            // ensure forward along ramp
                            dot = 0;
                        }
                        player.vx = tangentX * Math.max(250, dot + 80);
                        player.vy = tangentY * Math.max(-200, dot - 30);
                        player.onGround = true;
                        // Jump impulse if pressing space
                        if (spacePressed && !player.justJumped) {
                            player.vy -= JUMP_IMPULSE * 0.8;
                            player.onGround = false;
                            player.justJumped = true;
                        }
                        // Particle effect on ramp
                        particles.spray(player.x, player.y + 5, -tangentY * 5, tangentX * 5, 12, new Color(240, 240, 250), 0.9);
                    }
                    continue;
                }

                // Simple circle/rect or circle/circle
                if (o.collides(px, py, pr)) {
                    // Hit obstacle: crash
                    if (!player.dead) {
                        player.crash();
                        particles.explosion(px, py, 26, new Color(255, 110, 110), new Color(255, 220, 220));
                        hud.pop("CRASH!");
                    }
                }
            }
        }

        // --------------- Rendering ---------------
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background gradient sky
            paintSky(g2);

            // Parallax mountains and clouds
            if (showMountains) parallax.paintMountains(g2, width, height, camera.x);
            if (showClouds) parallax.paintClouds(g2, width, height, camera.x);

            // Translate to camera
            AffineTransform old = g2.getTransform();
            g2.translate(-camera.x, -camera.y);

            // Terrain
            terrain.paint(g2, camera.x, camera.x + width, camera.y, height);

            // Obstacles and collectibles
            for (Obstacle o : obstacles) o.paint(g2);
            for (Collectible c : collectibles) c.paint(g2);

            // Player and particles
            particles.paint(g2);
            player.paint(g2);

            // Restore transform
            g2.setTransform(old);

            // Foreground snowfall overlay
            if (showSnowfall) parallax.paintSnowfall(g2, width, height);

            // HUD and overlays
            hud.paint(g2, width, height, fps, ups, state, player, distanceTraveled, score, coins, showDebug);

            g2.dispose();
        }

        private void paintSky(Graphics2D g2) {
            GradientPaint gp = new GradientPaint(0, 0, SKY_TOP, 0, height, SKY_BOTTOM);
            g2.setPaint(gp);
            g2.fillRect(0, 0, width, height);
        }

        // --------------- Input ---------------
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A:
                    leftPressed = true;
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D:
                    rightPressed = true;
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W:
                    upPressed = true;
                    break;
                case KeyEvent.VK_SPACE:
                    spacePressed = true;
                    break;
                case KeyEvent.VK_P:
                    if (state == State.PLAYING) state = State.PAUSED;
                    else if (state == State.PAUSED) state = State.PLAYING;
                    break;
                case KeyEvent.VK_R:
                    if (state == State.GAME_OVER || state == State.PAUSED || state == State.PLAYING) resetGame();
                    break;
                case KeyEvent.VK_M:
                    showMountains = !showMountains;
                    break;
                case KeyEvent.VK_C:
                    showClouds = !showClouds;
                    break;
                case KeyEvent.VK_G:
                    showSnowfall = !showSnowfall;
                    break;
                case KeyEvent.VK_F3:
                    showDebug = !showDebug;
                    break;
                case KeyEvent.VK_ENTER:
                    if (state == State.MENU) {
                        resetGame();
                        state = State.PLAYING;
                    } else if (state == State.GAME_OVER) {
                        resetGame();
                        state = State.PLAYING;
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A:
                    leftPressed = false;
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D:
                    rightPressed = false;
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W:
                    upPressed = false;
                    break;
                case KeyEvent.VK_SPACE:
                    spacePressed = false;
                    break;
                default:
                    break;
            }
        }

        @Override
        public void addNotify() {
            super.addNotify();
            requestFocus();
        }

        @Override
        public void componentResized(ComponentEvent e) {
            width = getWidth();
            height = getHeight();
        }

        @Override
        public void componentMoved(ComponentEvent e) {}
        @Override
        public void componentShown(ComponentEvent e) {}
        @Override
        public void componentHidden(ComponentEvent e) {}

        @Override
        public void mouseClicked(MouseEvent e) {
            if (state == State.MENU) {
                resetGame();
                state = State.PLAYING;
            } else if (state == State.GAME_OVER) {
                resetGame();
                state = State.PLAYING;
            }
        }
        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseEntered(MouseEvent e) { focused = true; }
        @Override
        public void mouseExited(MouseEvent e) { focused = false; }

        // --------------- Game control ---------------
        private void resetGame() {
            // Reset world and player
            terrain = new Terrain();
            player = new Player();
            particles = new ParticleSystem();
            parallax = new Parallax();
            hud = new HUD();

            obstacles.clear();
            collectibles.clear();
            nextSpawnX = 800.0;

            distanceTraveled = 0.0;
            score = 0;
            coins = 0;
            camera = new Camera();

            player.x = 120.0;
            player.y = terrain.heightAt(player.x) - 20;
            player.vx = 350;
            player.vy = 0;

            state = State.MENU; // We'll switch to PLAYING when requested
        }

        // --------------- Enums ---------------
        private enum State {
            MENU, PLAYING, PAUSED, GAME_OVER
        }

        // --------------- Camera ---------------
        private static class Camera {
            double x, y;

            void follow(Player p, int screenW, int screenH, boolean relaxed) {
                // Look ahead in direction of motion
                double lead = screenW * (relaxed ? 0.25 : WORLD_SCROLL_MARGIN_X);
                double targetX = p.x - screenW * 0.5 + lead;
                double targetY = p.y - screenH * 0.55;

                x += (targetX - x) * CAMERA_LERP;
                y += (targetY - y) * CAMERA_Y_LERP;
            }
        }

        // --------------- Terrain ---------------
        private static class Terrain {
            double wobble = 0.0;
            double wobble2 = 1000.0;

            void updateWobble(double dt) {
                wobble += TERRAIN_WOBBLE * dt * 60;
                wobble2 += TERRAIN_WOBBLE * dt * 30;
            }

            double heightAt(double x) {
                double base = x * TERRAIN_SLOPE + 300;
                double y = base
                        + Math.sin(x * TERRAIN_FREQ1 + wobble) * (TERRAIN_AMPLITUDE * 0.6)
                        + Math.sin(x * TERRAIN_FREQ2 * 1.7 + wobble2) * (TERRAIN_AMPLITUDE * 0.3)
                        + Math.sin(x * TERRAIN_FREQ3 * 3.4) * (TERRAIN_AMPLITUDE * 0.15);
                return y;
            }

            double slopeAt(double x) {
                // derivative dy/dx
                double d = TERRAIN_SLOPE
                        + Math.cos(x * TERRAIN_FREQ1 + wobble) * (TERRAIN_AMPLITUDE * 0.6 * TERRAIN_FREQ1)
                        + Math.cos(x * TERRAIN_FREQ2 * 1.7 + wobble2) * (TERRAIN_AMPLITUDE * 0.3 * TERRAIN_FREQ2 * 1.7)
                        + Math.cos(x * TERRAIN_FREQ3 * 3.4) * (TERRAIN_AMPLITUDE * 0.15 * TERRAIN_FREQ3 * 3.4);
                return d;
            }

            void paint(Graphics2D g2, double minX, double maxX, double camY, int screenH) {
                // Draw snow surface polygon between minX..maxX
                double step = 6.0;
                int points = (int) Math.ceil((maxX - minX) / step) + 3;
                int[] xs = new int[points];
                int[] ys = new int[points];

                int idx = 0;
                for (double x = minX; x <= maxX; x += step) {
                    double y = heightAt(x);
                    xs[idx] = (int) (x);
                    ys[idx] = (int) (y);
                    idx++;
                }
                xs[idx] = (int) (maxX);
                ys[idx] = screenH + (int) camY + 10;
                idx++;
                xs[idx] = (int) (minX);
                ys[idx] = screenH + (int) camY + 10;

                // Fill snow
                g2.setColor(SNOW_COLOR);
                g2.fillPolygon(xs, ys, idx + 1);

                // Draw a soft shadow ridge by sampling normals
                Stroke old = g2.getStroke();
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(SNOW_SHADOW);
                for (double x = minX; x < maxX; x += 8) {
                    double y = heightAt(x);
                    double slope = slopeAt(x);
                    double nx = -slope;
                    double ny = 1;
                    double len = Math.hypot(nx, ny);
                    if (len == 0) continue;
                    nx /= len;
                    ny /= len;
                    double shadeLen = 8 + 10 * Math.max(0, Math.min(1, (1 - Math.abs(slope) * 0.3)));
                    g2.draw(new Line2D.Double(x, y, x + nx * shadeLen, y + ny * shadeLen));
                }
                g2.setStroke(old);
            }
        }

        // --------------- Player ---------------
        private class Player {
            double x = 120;
            double y = 320;
            double vx = 0;
            double vy = 0;
            double angle = 0; // render tilt of board
            double airRotation = 0; // rotates during jumps for trick score
            boolean onGround = true;
            boolean dead = false;
            boolean justJumped = false;
            boolean didLand = false;

            private double carveVel = 0.0;

            double radius() {
                return 16.0;
            }

            void resetForMenu(Terrain t, int screenW, int screenH, double dt) {
                // Place on left and slide slightly
                if (vx < 150) vx += 30 * dt;
                x += vx * dt;
                y = t.heightAt(x) - 24;
                onGround = true;
                angle = Math.atan(t.slopeAt(x));
            }

            void update(double dt, double steer, boolean tuck, boolean jump, Terrain t, ParticleSystem ps) {
                if (dead) {
                    vy += GRAVITY * dt;
                    x += vx * dt;
                    y += vy * dt;
                    angle *= 0.98;
                    return;
                }

                justJumped = false;

                // Gravity
                vy += GRAVITY * dt;

                // Aerodynamic drag
                double drag = AIR_DRAG * (tuck ? TUCK_DRAG_MULT : 1.0);
                vx -= vx * drag * dt;
                vy -= vy * drag * 0.5 * dt;

                // Carving (lateral acceleration perpendicular to velocity, only when grounded)
                if (onGround) {
                    carveVel += steer * CARVE_FORCE * dt;
                    carveVel *= CARVE_DAMP;
                    vx += carveVel * 0.01 * dt;
                } else {
                    // In air, steering controls trick rotation
                    airRotation += steer * 260.0 * dt;
                    angle = Math.toRadians(airRotation);
                }

                // Clamp speed
                double speed = Math.hypot(vx, vy);
                if (speed > MAX_SPEED) {
                    vx = vx / speed * MAX_SPEED;
                    vy = vy / speed * MAX_SPEED;
                }

                // Integrate
                double prevY = y;
                x += vx * dt;
                y += vy * dt;

                // Terrain collision
                double groundY = t.heightAt(x) - 20;
                double slope = t.slopeAt(x);
                double slopeAngle = Math.atan(slope);

                if (y >= groundY) {
                    if (!onGround) {
                        // Landing
                        onGround = true;
                        didLand = true;
                        // Reduce vertical energy and project along slope
                        double normalX = -Math.sin(slopeAngle);
                        double normalY = Math.cos(slopeAngle);
                        double vdotn = vx * normalX + vy * normalY;
                        if (vdotn > LAND_PARTICLE_IMPULSE_THRESHOLD) {
                            particles.spray(x, groundY, -normalX * 10, -normalY * 10, 24, new Color(240, 240, 250), 1.3);
                        }
                        // Reset trick angle on land
                        airRotation = Math.toDegrees(slopeAngle);
                        angle = slopeAngle * 0.85;
                    }

                    // Stick to ground
                    y = groundY;
                    onGround = true;

                    // Project velocity onto tangent of slope
                    double tx = Math.cos(slopeAngle);
                    double ty = Math.sin(slopeAngle);
                    double vdotT = vx * tx + vy * ty;

                    // Apply friction along tangent
                    vdotT -= vdotT * GROUND_FRICTION * dt;

                    // Rebuild velocity from tangent only (remove normal component)
                    vx = tx * vdotT;
                    vy = ty * vdotT;

                    // Tucking boosts along tangent
                    if (tuck) {
                        vx += tx * 360 * dt;
                        vy += ty * 360 * dt;
                    }

                    // Jump
                    if (jump && !justJumped) {
                        vy -= JUMP_IMPULSE;
                        onGround = false;
                        justJumped = true;
                    }

                    // Board lean for aesthetics
                    angle = angle * 0.8 + slopeAngle * 0.2 + Math.toRadians(carveVel * 0.02);
                } else {
                    // In air
                    onGround = false;
                    // Slight damping of carve while in air
                    carveVel *= 0.99;
                }

                // Particle snow spray while carving on ground
                if (onGround && Math.abs(vx) > 120) {
                    double dir = Math.signum(vx);
                    double lateral = carveVel * 0.015;
                    double sx = x - Math.sin(slopeAngle) * 10 * dir;
                    double sy = y + Math.cos(slopeAngle) * 10;
                    ps.spray(sx, sy, -dir * 10 + lateral, -3, 1 + (int) (Math.abs(carveVel) * 0.02), new Color(250, 250, 255), 0.5);
                }

                // Crash if velocity too high at wrong landing angle
                if (onGround) {
                    double verticalImpact = y - prevY;
                    if (verticalImpact > 5 && Math.abs(angle - slopeAngle) > Math.toRadians(50)) {
                        crash();
                    }
                }

                // Fall off-screen -> dead
                if (y > 10000) {
                    dead = true;
                }
            }

            void crash() {
                dead = true;
                onGround = false;
                // knock player
                vx *= 0.6;
                vy = -200;
            }

            void paint(Graphics2D g2) {
                // draw a stylized rider + board
                AffineTransform old = g2.getTransform();
                g2.translate(x, y);

                // Shadow ellipse
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fill(new Ellipse2D.Double(-16, 10, 32, 8));

                g2.rotate(angle);

                // Board
                g2.setColor(BOARD_COLOR);
                Shape board = new RoundRectangle2D.Double(-28, -6, 56, 12, 10, 10);
                g2.fill(board);

                // Rider body
                g2.setColor(RIDER_COLOR);
                g2.fill(new Ellipse2D.Double(-6, -22, 12, 12)); // head
                g2.fill(new RoundRectangle2D.Double(-8, -18, 16, 22, 6, 6)); // torso
                // Arms
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(-8, -14, -18, -6);
                g2.drawLine(8, -14, 18, -6);
                // Legs
                g2.drawLine(-6, 0, -14, 6);
                g2.drawLine(6, 0, 14, 6);

                g2.setTransform(old);

                if (showDebug) {
                    g2.setColor(Color.RED);
                    g2.draw(new Ellipse2D.Double(x - radius(), y - radius(), radius() * 2, radius() * 2));
                }
            }
        }

        // --------------- Obstacles ---------------
        private abstract static class Obstacle {
            double x, y;
            boolean collidable = true;

            Obstacle(double x, double y) {
                this.x = x;
                this.y = y;
            }

            abstract void update(double dt);

            abstract void paint(Graphics2D g2);

            abstract boolean collides(double px, double py, double pr);
        }

        private static class Tree extends Obstacle {
            double scale;

            Tree(double x, double y, double scale) {
                super(x, y);
                this.scale = scale;
            }

            @Override
            void update(double dt) {}

            @Override
            void paint(Graphics2D g2) {
                AffineTransform old = g2.getTransform();
                g2.translate(x, y);
                g2.scale(scale, scale);

                // Trunk
                g2.setColor(new Color(80, 50, 30));
                g2.fill(new Rectangle2D.Double(-6, -60, 12, 60));

                // Leaves (triangles)
                g2.setColor(new Color(30, 120, 40));
                Polygon p1 = new Polygon(new int[]{0, -36, 36}, new int[]{-90, -40, -40}, 3);
                Polygon p2 = new Polygon(new int[]{0, -30, 30}, new int[]{-70, -25, -25}, 3);
                Polygon p3 = new Polygon(new int[]{0, -24, 24}, new int[]{-50, -10, -10}, 3);
                g2.fillPolygon(p1);
                g2.fillPolygon(p2);
                g2.fillPolygon(p3);

                // Snow cap
                g2.setColor(new Color(245, 250, 255));
                g2.fill(new Ellipse2D.Double(-16, -96, 32, 12));

                g2.setTransform(old);
            }

            @Override
            boolean collides(double px, double py, double pr) {
                double rx = x;
                double ry = y - 30 * scale;
                double rad = 16 * scale + pr;
                double dx = px - rx;
                double dy = py - ry;
                return dx * dx + dy * dy < rad * rad;
            }
        }

        private static class Rock extends Obstacle {
            double scale;

            Rock(double x, double y, double scale) {
                super(x, y);
                this.scale = scale;
            }

            @Override
            void update(double dt) {}

            @Override
            void paint(Graphics2D g2) {
                AffineTransform old = g2.getTransform();
                g2.translate(x, y);

                g2.setColor(new Color(160, 160, 170));
                Shape s = new Polygon(
                        new int[]{(int) (-20 * scale), (int) (0), (int) (22 * scale), (int) (10 * scale), (int) (-14 * scale)},
                        new int[]{(int) (0), (int) (-14 * scale), (int) (-2 * scale), (int) (8 * scale), (int) (6 * scale)},
                        5);
                g2.fill(s);

                g2.setColor(new Color(120, 120, 130));
                g2.draw(s);

                g2.setTransform(old);
            }

            @Override
            boolean collides(double px, double py, double pr) {
                // simple circle around rock center
                double rr = 16 * scale + pr;
                double dx = px - x;
                double dy = py - (y - 4);
                return dx * dx + dy * dy < rr * rr;
            }
        }

        private static class Flag extends Obstacle {
            double flutter = 0;

            Flag(double x, double y) {
                super(x, y);
            }

            @Override
            void update(double dt) {
                flutter += dt * 6.0;
            }

            @Override
            void paint(Graphics2D g2) {
                AffineTransform old = g2.getTransform();
                g2.translate(x, y);

                // Pole
                g2.setColor(new Color(180, 180, 190));
                g2.fill(new RoundRectangle2D.Double(-2, -48, 4, 48, 3, 3));

                // Flag waving
                g2.setColor(new Color(255, 60, 70));
                GeneralPath flag = new GeneralPath();
                flag.moveTo(2, -44);
                for (int i = 0; i <= 6; i++) {
                    double t = i / 6.0;
                    double xx = 2 + t * 30;
                    double yy = -44 + Math.sin(flutter + t * Math.PI * 1.5) * 4;
                    flag.lineTo(xx, yy);
                }
                flag.lineTo(2, -36);
                flag.closePath();
                g2.fill(flag);

                g2.setTransform(old);
            }

            @Override
            boolean collides(double px, double py, double pr) {
                // small hit around pole
                double dx = px - x;
                double dy = py - (y - 24);
                return dx * dx + dy * dy < (8 + pr) * (8 + pr);
            }
        }

        private static class Ramp extends Obstacle {
            double length;
            double angle;

            Ramp(double x, double y, double length, double angle) {
                super(x, y);
                this.length = length;
                this.angle = angle;
            }

            boolean contains(double px, double py) {
                // Transform point into ramp local coords
                double dx = px - x;
                double dy = py - y;
                double c = Math.cos(-angle);
                double s = Math.sin(-angle);
                double rx = dx * c - dy * s;
                double ry = dx * s + dy * c;
                return rx >= 0 && rx <= length && ry >= -8 && ry <= 8;
            }

            @Override
            void update(double dt) {}

            @Override
            void paint(Graphics2D g2) {
                AffineTransform old = g2.getTransform();
                g2.translate(x, y);
                g2.rotate(angle);

                g2.setColor(new Color(255, 200, 160));
                g2.fill(new RoundRectangle2D.Double(0, -8, length, 16, 6, 6));
                g2.setColor(new Color(140, 120, 100));
                for (int i = 8; i < length; i += 18) {
                    g2.drawLine(i, -8, i, 8);
                }

                g2.setTransform(old);
            }

            @Override
            boolean collides(double px, double py, double pr) {
                // Ramp does not cause crash; handled specially
                return false;
            }
        }

        // --------------- Collectibles ---------------
        private abstract static class Collectible {
            double x, y;
            double radius;
            boolean collected = false;

            Collectible(double x, double y, double radius) {
                this.x = x;
                this.y = y;
                this.radius = radius;
            }

            abstract void update(double dt);

            abstract void paint(Graphics2D g2);
        }

        private static class Coin extends Collectible {
            double spin = 0;

            Coin(double x, double y) {
                super(x, y, 12);
            }

            @Override
            void update(double dt) {
                if (!collected) {
                    spin += dt * 6.0;
                }
            }

            @Override
            void paint(Graphics2D g2) {
                if (collected) return;
                AffineTransform old = g2.getTransform();
                g2.translate(x, y);

                // Spin effect using ellipse scale
                double s = 0.7 + 0.3 * Math.abs(Math.sin(spin));
                g2.scale(s, 1.0);

                g2.setColor(new Color(255, 210, 0));
                g2.fill(new Ellipse2D.Double(-radius, -radius, radius * 2, radius * 2));
                g2.setColor(new Color(255, 240, 120));
                g2.draw(new Ellipse2D.Double(-radius, -radius, radius * 2, radius * 2));

                g2.setTransform(old);
            }
        }

        // --------------- Particles ---------------
        private static class ParticleSystem {
            private static class Particle {
                double x, y, vx, vy, life, maxLife, size;
                Color color;
                double gravity = 300;
                double drag = 0.05;

                Particle(double x, double y, double vx, double vy, double life, double size, Color color) {
                    this.x = x;
                    this.y = y;
                    this.vx = vx;
                    this.vy = vy;
                    this.life = life;
                    this.maxLife = life;
                    this.size = size;
                    this.color = color;
                }

                void update(double dt) {
                    life -= dt;
                    vy += gravity * dt;
                    vx -= vx * drag * dt;
                    vy -= vy * drag * dt;
                    x += vx * dt;
                    y += vy * dt;
                }

                void paint(Graphics2D g2) {
                    if (life <= 0) return;
                    float a = (float) Math.max(0, Math.min(1, life / maxLife));
                    Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (a * 255));
                    g2.setColor(c);
                    g2.fill(new Ellipse2D.Double(x - size * 0.5, y - size * 0.5, size, size));
                }
            }

            private final List<Particle> particles = new ArrayList<>();

            void spray(double x, double y, double dx, double dy, int count, Color color, double spread) {
                for (int i = 0; i < count; i++) {
                    double ang = (rng().nextDouble() - 0.5) * spread;
                    double spd = 60 + rng().nextDouble() * 240;
                    double vx = dx + Math.cos(ang) * spd * 0.4;
                    double vy = dy + Math.sin(ang) * spd * 0.2 - 10;
                    double life = 0.3 + rng().nextDouble() * 0.7;
                    double size = 2 + rng().nextDouble() * 3;
                    particles.add(new Particle(x, y, vx, vy, life, size, color));
                }
            }

            void sparkle(double x, double y, int count, Color color) {
                for (int i = 0; i < count; i++) {
                    double ang = rng().nextDouble() * Math.PI * 2;
                    double spd = 50 + rng().nextDouble() * 140;
                    double vx = Math.cos(ang) * spd;
                    double vy = Math.sin(ang) * spd - 40;
                    double life = 0.35 + rng().nextDouble() * 0.5;
                    double size = 2 + rng().nextDouble() * 2;
                    particles.add(new Particle(x, y, vx, vy, life, size, color));
                }
            }

            void explosion(double x, double y, int count, Color inner, Color outer) {
                for (int i = 0; i < count; i++) {
                    double ang = rng().nextDouble() * Math.PI * 2;
                    double spd = 120 + rng().nextDouble() * 320;
                    double vx = Math.cos(ang) * spd;
                    double vy = Math.sin(ang) * spd - 80;
                    double life = 0.5 + rng().nextDouble() * 0.8;
                    double size = 3 + rng().nextDouble() * 5;
                    Color color = (i % 2 == 0) ? inner : outer;
                    Particle p = new Particle(x, y, vx, vy, life, size, color);
                    p.gravity = 500;
                    particles.add(p);
                }
            }

            void update(double dt) {
                for (int i = particles.size() - 1; i >= 0; i--) {
                    Particle p = particles.get(i);
                    p.update(dt);
                    if (p.life <= 0) particles.remove(i);
                }
            }

            void paint(Graphics2D g2) {
                for (Particle p : particles) p.paint(g2);
            }

            private Random rng() {
                return ThreadLocalRandom.current();
            }
        }

        // --------------- Parallax background ---------------
        private static class Parallax {
            private static class Mountain {
                double x, height, width, speed;
                Color color;

                Mountain(double x, double height, double width, double speed, Color color) {
                    this.x = x;
                    this.height = height;
                    this.width = width;
                    this.speed = speed;
                    this.color = color;
                }
            }

            private static class Cloud {
                double x, y, speed, w, h;
                float alpha;

                Cloud(double x, double y, double speed, double w, double h, float alpha) {
                    this.x = x;
                    this.y = y;
                    this.speed = speed;
                    this.w = w;
                    this.h = h;
                    this.alpha = alpha;
                }
            }

            private static class Snowflake {
                double x, y, vy, vx, size;

                Snowflake(double x, double y, double vy, double vx, double size) {
                    this.x = x;
                    this.y = y;
                    this.vy = vy;
                    this.vx = vx;
                    this.size = size;
                }
            }

            private final List<Mountain> mountains = new ArrayList<>();
            private final List<Cloud> clouds = new ArrayList<>();
            private final List<Snowflake> snowflakes = new ArrayList<>();

            private final Random rng = new Random(42);

            Parallax() {
                // Seed mountains
                for (int i = 0; i < 30; i++) {
                    double w = 200 + rng.nextDouble() * 900;
                    double h = 120 + rng.nextDouble() * 450;
                    double x = i * 400 + rng.nextDouble() * 300;
                    double speed = 0.2 + rng.nextDouble() * 0.6;
                    Color c = new Color(180 - (int) (speed * 70), 200 - (int) (speed * 50), 220 - (int) (speed * 40));
                    mountains.add(new Mountain(x, h, w, speed, c));
                }
                // Seed clouds
                for (int i = 0; i < 25; i++) {
                    clouds.add(new Cloud(rng.nextDouble() * 8000, 60 + rng.nextDouble() * 260,
                            12 + rng.nextDouble() * 30, 90 + rng.nextDouble() * 200, 26 + rng.nextDouble() * 50, 0.35f + rng.nextFloat() * 0.35f));
                }
                // Snowfall
                for (int i = 0; i < SNOWFLAKE_COUNT; i++) {
                    snowflakes.add(new Snowflake(rng.nextDouble() * DEFAULT_WIDTH, rng.nextDouble() * DEFAULT_HEIGHT,
                            SNOWFLAKE_SPEED_MIN + rng.nextDouble() * (SNOWFLAKE_SPEED_MAX - SNOWFLAKE_SPEED_MIN),
                            -20 + rng.nextDouble() * 40, 1 + rng.nextDouble() * 3));
                }
            }

            void update(double dt, double worldVX, double cloudDrift) {
                // Mountains move slowly relative to camera x via paint (parallax factor)
                // Clouds drift
                for (Cloud c : clouds) {
                    c.x += (c.speed + cloudDrift * 0.1) * dt * 60;
                    if (c.x > 9000) {
                        c.x = -400 - rng.nextDouble() * 500;
                        c.y = 40 + rng.nextDouble() * 280;
                        c.w = 90 + rng.nextDouble() * 200;
                        c.h = 26 + rng.nextDouble() * 50;
                        c.speed = 12 + rng.nextDouble() * 30;
                        c.alpha = 0.35f + rng.nextFloat() * 0.35f;
                    }
                }
            }

            void snowfallUpdate(double dt, int w, int h) {
                for (Snowflake s : snowflakes) {
                    s.x += s.vx * dt;
                    s.y += s.vy * dt;
                    if (s.y > h + 10) {
                        s.y = -10;
                        s.x = rng.nextDouble() * w;
                        s.vy = SNOWFLAKE_SPEED_MIN + rng.nextDouble() * (SNOWFLAKE_SPEED_MAX - SNOWFLAKE_SPEED_MIN);
                        s.vx = -20 + rng.nextDouble() * 40;
                        s.size = 1 + rng.nextDouble() * 3;
                    }
                    if (s.x < -10) s.x = w + 10;
                    if (s.x > w + 10) s.x = -10;
                }
            }

            void paintMountains(Graphics2D g2, int w, int h, double camX) {
                for (Mountain m : mountains) {
                    double px = -camX * m.speed + m.x;
                    double baseY = h * 0.88;
                    double halfW = m.width * 0.5;

                    Path2D shape = new Path2D.Double();
                    shape.moveTo(px - halfW, baseY);
                    shape.lineTo(px, baseY - m.height);
                    shape.lineTo(px + halfW, baseY);
                    shape.closePath();

                    g2.setColor(m.color);
                    g2.fill(shape);

                    g2.setColor(new Color(200, 220, 235, 50));
                    g2.draw(shape);
                }
            }

            void paintClouds(Graphics2D g2, int w, int h, double camX) {
                Composite old = g2.getComposite();
                for (Cloud c : clouds) {
                    double px = c.x - camX * 0.4;
                    float alpha = c.alpha;
                    g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
                    g2.setColor(new Color(255, 255, 255));
                    g2.fill(new RoundRectangle2D.Double(px, c.y, c.w, c.h, c.h, c.h));
                    g2.fill(new RoundRectangle2D.Double(px + c.w * 0.3, c.y - c.h * 0.6, c.w * 0.7, c.h * 1.2, c.h, c.h));
                    g2.fill(new RoundRectangle2D.Double(px - c.w * 0.2, c.y - c.h * 0.4, c.w * 0.6, c.h * 1.1, c.h, c.h));
                }
                g2.setComposite(old);
            }

            void paintSnowfall(Graphics2D g2, int w, int h) {
                g2.setColor(new Color(255, 255, 255, 150));
                for (Snowflake s : snowflakes) {
                    g2.fill(new Ellipse2D.Double(s.x, s.y, s.size, s.size));
                }
            }
        }

        // --------------- HUD and UI ---------------
        private class HUD {
            private static class Popup {
                String text;
                double x, y, time, ttl;

                Popup(String text, double x, double y) {
                    this.text = text;
                    this.x = x;
                    this.y = y;
                    this.ttl = 1.4;
                    this.time = 0.0;
                }
            }

            private final Deque<Popup> popups = new ArrayDeque<>();

            void pop(String text) {
                // spawn at top-left area; will animate up
                popups.add(new Popup(text, 20, 60 + popups.size() * 18));
            }

            void paint(Graphics2D g2, int w, int h, int fps, int ups, State state, Player p, double dist, int score, int coins, boolean debug) {
                g2.setFont(HUD_FONT);
                g2.setColor(Color.BLACK);
                // Shadow above
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillRect(0, 0, w, 26);

                // HUD stats
                g2.setColor(Color.WHITE);
                String stats = String.format("Speed: %3.0f km/h   Dist: %6.0f m   Score: %6d   Coins: %3d", Math.max(0, Math.hypot(p.vx, p.vy) * 0.18), dist / 4.0, score, coins);
                g2.drawString(stats, 10, 18);

                // FPS/UPS
                String perf = String.format("FPS: %d  UPS: %d", fps, ups);
                int sw = g2.getFontMetrics().stringWidth(perf);
                g2.drawString(perf, w - sw - 10, 18);

                // Popups
                Iterator<Popup> it = popups.iterator();
                while (it.hasNext()) {
                    Popup po = it.next();
                    po.time += FIXED_TIMESTEP; // approximate
                    double t = po.time / po.ttl;
                    if (t >= 1) {
                        it.remove();
                        continue;
                    }
                    float alpha = (float) (1 - t);
                    int yy = (int) (po.y - t * 30);
                    g2.setColor(new Color(255, 255, 255, (int) (alpha * 255)));
                    g2.drawString(po.text, (int) po.x, yy);
                }

                // Overlays for state
                if (state == State.MENU) {
                    paintCenteredText(g2, w, h, "Extreme Snowboarding", BIG_FONT, new Color(255, 255, 255));
                    paintCenteredText(g2, w, h + 60, "Press ENTER or click to start", MED_FONT, new Color(250, 250, 250));
                    paintInstructions(g2, w, h);
                } else if (state == State.PAUSED) {
                    paintCenteredText(g2, w, h, "Paused", BIG_FONT, new Color(255, 255, 255));
                    paintCenteredText(g2, w, h + 60, "Press P to resume", MED_FONT, new Color(245, 245, 245));
                } else if (state == State.GAME_OVER) {
                    paintCenteredText(g2, w, h, "Game Over", BIG_FONT, new Color(255, 120, 120));
                    paintCenteredText(g2, w, h + 60, "Press R or ENTER to restart", MED_FONT, new Color(245, 245, 245));
                    String finalStats = String.format("Distance: %.0f m    Score: %d    Coins: %d", dist / 4.0, score, coins);
                    paintCenteredText(g2, w, h + 110, finalStats, MED_FONT, new Color(245, 245, 245));
                }

                if (debug) {
                    g2.setColor(new Color(0, 0, 0, 120));
                    g2.fillRoundRect(10, h - 120, 360, 110, 8, 8);
                    g2.setColor(Color.WHITE);
                    int y0 = h - 100;
                    g2.drawString("DEBUG", 20, y0);
                    y0 += 18;
                    g2.drawString(String.format("Player x=%.1f y=%.1f vx=%.1f vy=%.1f", player.x, player.y, player.vx, player.vy), 20, y0);
                    y0 += 18;
                    g2.drawString(String.format("OnGround=%s angle=%.1f rot=%.1f", player.onGround, Math.toDegrees(player.angle), player.airRotation), 20, y0);
                    y0 += 18;
                    g2.drawString(String.format("Obstacles=%d  Coins=%d", obstacles.size(), collectibles.size()), 20, y0);
                    y0 += 18;
                    g2.drawString(String.format("Cam x=%.1f y=%.1f", camera.x, camera.y), 20, y0);
                }
            }

            private void paintCenteredText(Graphics2D g2, int w, int h, String text, Font font, Color color) {
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(text);
                int th = fm.getAscent();
                int x = (w - tw) / 2;
                int y = (h + th) / 2;
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRoundRect(x - 14, y - th - 14, tw + 28, th + 28, 16, 16);
                g2.setColor(color);
                g2.drawString(text, x, y);
            }

            private void paintInstructions(Graphics2D g2, int w, int h) {
                g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
                String[] lines = new String[]{
                        "Controls:",
                        "  Left/Right (A/D): Carve/steer",
                        "  Up (W): Tuck to go faster",
                        "  Space: Jump (use ramps or jump on ground)",
                        "  P: Pause,  R: Restart",
                        "  M/C/G: Toggle mountains/clouds/snowfall",
                        "  F3: Debug overlay",
                        "",
                        "Tips: Carve down the slope to gain speed. Jump off ramps for air and spin tricks for bonus points!"
                };
                int y = h + 120;
                int x = Math.max(60, w / 2 - 280);
                g2.setColor(new Color(0, 0, 0, 130));
                g2.fillRoundRect(x - 14, y - 24, 620, lines.length * 20 + 28, 12, 12);
                g2.setColor(Color.WHITE);
                for (int i = 0; i < lines.length; i++) {
                    g2.drawString(lines[i], x, y + i * 20);
                }
            }
        }
    }
}