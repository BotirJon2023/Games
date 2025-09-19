import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import java.util.*;

public class RollerbladingAdventureGame extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RollerbladingAdventureGame::new);
    }

    public RollerbladingAdventureGame() {
        super("Rollerblading Adventure Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel panel = new GamePanel();
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        panel.start();
    }

    // The game panel containing all logic and rendering
    static class GamePanel extends JPanel implements ActionListener, KeyListener {

        // Screen settings
        static final int WIDTH = 1280;
        static final int HEIGHT = 720;
        static final double TARGET_FPS = 60.0;
        static final int TIMER_DELAY_MS = (int) Math.round(1000.0 / TARGET_FPS);

        // World constants
        static final double GROUND_Y = 560; // baseline ground
        static final double GRAVITY = 2200; // px/s^2
        static final double JUMP_VELOCITY = 1000;
        static final double BASE_SPEED = 340; // px/s
        static final double MAX_SPEED = 900;
        static final double SPEED_ACC = 8; // speed increase rate
        static final double DASH_MULT = 1.6; // dash speed multiplier
        static final double DASH_STAMINA_COST = 18; // per second
        static final double STAMINA_RECOVERY = 9; // per second
        static final double STAMINA_MAX = 100;
        static final double CROUCH_HEIGHT_FACTOR = 0.6; // reduce height
        static final double LAND_ANGLE_TOLERANCE = 25; // degrees tolerance for safe landing
        static final double SPIN_SPEED = 380; // degrees per second when holding trick
        static final double FRICTION = 5.8; // ground friction slowdown
        static final double RAIL_SPEED_GAIN = 100; // speed added when grind starts
        static final double BOOST_SPEED_GAIN = 200;
        static final double MAGNET_RANGE = 200;

        // Colors
        static final Color SKY_TOP = new Color(85, 120, 255);
        static final Color SKY_BOTTOM = new Color(200, 230, 255);
        static final Color GROUND_COLOR = new Color(50, 50, 50);
        static final Color LANE_LINE = new Color(255, 255, 255, 90);
        static final Color HUD_BG = new Color(0, 0, 0, 120);
        static final Color HUD_FG = Color.WHITE;

        // Game states
        enum State { MENU, RUNNING, PAUSED, HELP, GAMEOVER }

        // Timer and timekeeping
        private final javax.swing.Timer timer;
        private long lastTimeNanos;
        private double accumulator = 0;

        // Game state
        private State state = State.MENU;

        // Input flags
        private boolean leftPressed;
        private boolean rightPressed;
        private boolean upPressed;
        private boolean downPressed;
        private boolean dashPressed;
        private boolean grindPressed;

        private boolean debug = false;

        // World variables
        private double worldX; // camera/scroll position
        private double speed; // scroll speed
        private double distanceTraveled; // meters or units for scoring
        private int score;
        private int highScore;
        private int combo;
        private double comboTimer; // combo timeout
        private boolean muted = false;

        // Entities
        private final Player player = new Player();
        private final List<Obstacle> obstacles = new ArrayList<>();
        private final List<Coin> coins = new ArrayList<>();
        private final List<Rail> rails = new ArrayList<>();
        private final List<Ramp> ramps = new ArrayList<>();
        private final List<Enemy> enemies = new ArrayList<>();
        private final List<PowerUp> powerUps = new ArrayList<>();
        private final List<Particle> particles = new ArrayList<>();

        // Parallax layers
        private final List<ParallaxLayer> layers = new ArrayList<>();

        // Randomness
        private final Random rng = new Random();

        // Spawn management
        private double nextSpawnXObstacle = 800;
        private double nextSpawnXCoin = 600;
        private double nextSpawnXRail = 1500;
        private double nextSpawnXRamp = 1200;
        private double nextSpawnXEnemy = 1800;
        private double nextSpawnXPower = 2200;

        // UI/Message
        private String message = "";
        private double messageTimer = 0;

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            setBackground(Color.BLACK);
            addKeyListener(this);
            timer = new javax.swing.Timer(TIMER_DELAY_MS, this);
            initParallaxLayers();
            resetGame();
        }

        public void start() {
            timer.start();
            lastTimeNanos = System.nanoTime();
        }

        private void initParallaxLayers() {
            layers.clear();
            // Sky - gradient drawn separately
            layers.add(new ParallaxLayer(0.02, 0, 0, false));  // far clouds
            layers.add(new ParallaxLayer(0.1, 0, HEIGHT - 400, true)); // far city
            layers.add(new ParallaxLayer(0.25, 0, HEIGHT - 300, true)); // mid trees
            layers.add(new ParallaxLayer(0.5, 0, HEIGHT - 180, true)); // foreground bushes
        }

        private void resetGame() {
            worldX = 0;
            speed = BASE_SPEED;
            distanceTraveled = 0;
            score = 0;
            combo = 0;
            comboTimer = 0;
            message = "";
            messageTimer = 0;
            nextSpawnXObstacle = 800;
            nextSpawnXCoin = 600;
            nextSpawnXRail = 1500;
            nextSpawnXRamp = 1200;
            nextSpawnXEnemy = 1800;
            nextSpawnXPower = 2200;

            obstacles.clear();
            coins.clear();
            rails.clear();
            ramps.clear();
            enemies.clear();
            powerUps.clear();
            particles.clear();

            player.reset();
            generateInitialScenery();
        }

        private void generateInitialScenery() {
            // Pre-place some coins and rails for the starting area
            for (int i = 0; i < 8; i++) {
                coins.add(new Coin(700 + i * 80, GROUND_Y - 90 - rng.nextInt(60)));
            }
            rails.add(new Rail(1000, GROUND_Y - 70, 260));
            ramps.add(new Ramp(1300, GROUND_Y, 130, 60));
            obstacles.add(new Obstacle(1500, GROUND_Y, 60, 50, Obstacle.Type.CONE));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            double dt = (now - lastTimeNanos) / 1_000_000_000.0;
            lastTimeNanos = now;

            // Clamp dt to prevent huge steps
            if (dt > 0.06) dt = 0.06;

            update(dt);
            repaint();
        }

        private void update(double dt) {
            switch (state) {
                case MENU:
                case HELP:
                case GAMEOVER:
                    // time passes for eye candy if desired
                    break;
                case PAUSED:
                    break;
                case RUNNING:
                    stepGame(dt);
                    break;
            }
        }

        private void stepGame(double dt) {
            // Update speed
            double targetSpeed = Math.min(BASE_SPEED + speed * 0.002 + SPEED_ACC, MAX_SPEED);
            speed += (targetSpeed - speed) * 0.5 * dt;

            if (dashPressed && player.stamina > 0 && !player.isGrinding) {
                speed = Math.min(speed * DASH_MULT, MAX_SPEED + 80);
                player.stamina = Math.max(0, player.stamina - DASH_STAMINA_COST * dt);
                addDust(player.x, player.getFeetY() + 3, 2, 2);
            } else {
                player.stamina = Math.min(STAMINA_MAX, player.stamina + STAMINA_RECOVERY * dt);
            }

            // Scroll worldX
            worldX += speed * dt;
            distanceTraveled += speed * dt;

            // Spawning
            spawnEntities();

            // Update entities
            player.update(dt, this);

            for (Rail r : rails) {
                r.update(dt, this);
            }
            for (Ramp r : ramps) {
                r.update(dt, this);
            }
            for (Obstacle o : obstacles) {
                o.update(dt, this);
            }
            for (Coin c : coins) {
                c.update(dt, this);
            }
            for (Enemy en : enemies) {
                en.update(dt, this);
            }
            for (PowerUp p : powerUps) {
                p.update(dt, this);
            }

            // Magnet effect
            if (player.magnetTimer > 0) {
                attractCoins(dt);
            }

            // Collisions
            checkCollisions();

            // Particles
            updateParticles(dt);

            // Remove offscreen
            cleanupEntities();

            // Combo management
            if (combo > 0) {
                comboTimer -= dt;
                if (comboTimer <= 0) {
                    combo = 0;
                }
            }

            // Messages
            if (messageTimer > 0) {
                messageTimer -= dt;
                if (messageTimer <= 0) message = "";
            }

            // Health check
            if (player.health <= 0) {
                onGameOver();
            }
        }

        private void spawnEntities() {
            // worldX indicates how far to the right we've gone.
            double ahead = worldX + WIDTH + 200;

            // Obstacles
            while (nextSpawnXObstacle < ahead) {
                double x = nextSpawnXObstacle + rng.nextInt(400);
                double typeRand = rng.nextDouble();
                Obstacle.Type type;
                if (typeRand < 0.5) {
                    type = Obstacle.Type.CONE;
                } else if (typeRand < 0.8) {
                    type = Obstacle.Type.BARRIER;
                } else {
                    type = Obstacle.Type.BENCH;
                }
                double w = type == Obstacle.Type.CONE ? 36 : (type == Obstacle.Type.BARRIER ? 88 : 140);
                double h = type == Obstacle.Type.CONE ? 38 : (type == Obstacle.Type.BARRIER ? 70 : 40);
                obstacles.add(new Obstacle(x, GROUND_Y, w, h, type));
                nextSpawnXObstacle += 380 + rng.nextInt(420);
            }

            // Coins
            while (nextSpawnXCoin < ahead) {
                double x = nextSpawnXCoin + rng.nextInt(300);
                int arc = rng.nextInt(3);
                int count = 6 + rng.nextInt(8);
                for (int i = 0; i < count; i++) {
                    double y = GROUND_Y - 80 - rng.nextInt(140);
                    double fx = x + i * 48;
                    if (arc == 1) {
                        // arc-shaped
                        y = GROUND_Y - 60 - 80 * Math.sin(i / (double) count * Math.PI);
                    } else if (arc == 2) {
                        // wave
                        y = GROUND_Y - 120 - 60 * Math.sin(i * 0.7);
                    }
                    coins.add(new Coin(fx, y));
                }
                nextSpawnXCoin += 350 + rng.nextInt(450);
            }

            // Rails
            while (nextSpawnXRail < ahead) {
                double x = nextSpawnXRail + rng.nextInt(500);
                double length = 180 + rng.nextInt(240);
                double y = GROUND_Y - (60 + rng.nextInt(90));
                rails.add(new Rail(x, y, length));
                // sometimes pair with ramp
                if (rng.nextDouble() < 0.5) {
                    ramps.add(new Ramp(x - 120, GROUND_Y, 120, y - (GROUND_Y - 16)));
                }
                nextSpawnXRail += 1200 + rng.nextInt(900);
            }

            // Ramps
            while (nextSpawnXRamp < ahead) {
                double x = nextSpawnXRamp + rng.nextInt(500);
                double w = 120 + rng.nextInt(100);
                double h = 40 + rng.nextInt(80);
                ramps.add(new Ramp(x, GROUND_Y, w, h));
                nextSpawnXRamp += 1000 + rng.nextInt(850);
            }

            // Enemies
            while (nextSpawnXEnemy < ahead) {
                double x = nextSpawnXEnemy + rng.nextInt(1200);
                Enemy.Type type = rng.nextDouble() < 0.5 ? Enemy.Type.DOG : Enemy.Type.PIGEON;
                enemies.add(new Enemy(x, GROUND_Y, type));
                nextSpawnXEnemy += 1700 + rng.nextInt(1500);
            }

            // Power-ups
            while (nextSpawnXPower < ahead) {
                double x = nextSpawnXPower + rng.nextInt(1000);
                PowerUp.Type type;
                double r = rng.nextDouble();
                if (r < 0.4) type = PowerUp.Type.MAGNET;
                else if (r < 0.7) type = PowerUp.Type.SHIELD;
                else type = PowerUp.Type.BOOST;
                double y = GROUND_Y - (60 + rng.nextInt(140));
                powerUps.add(new PowerUp(x, y, type));
                nextSpawnXPower += 2500 + rng.nextInt(2500);
            }
        }

        private void attractCoins(double dt) {
            double range2 = MAGNET_RANGE * MAGNET_RANGE;
            for (Coin c : coins) {
                double dx = (c.x - worldX) - player.x;
                double dy = c.y - player.y;
                double d2 = dx * dx + dy * dy;
                if (d2 < range2) {
                    double d = Math.sqrt(Math.max(1, d2));
                    double ax = -dx / d * 700;
                    double ay = -dy / d * 700;
                    c.vx += ax * dt;
                    c.vy += ay * dt;
                }
            }
        }

        private void checkCollisions() {
            // Player rectangle (AABB) approximated
            Rectangle2D.Double pb = player.getBounds();

            // Obstacles
            Iterator<Obstacle> itObs = obstacles.iterator();
            while (itObs.hasNext()) {
                Obstacle o = itObs.next();
                if (o.isOffscreen(worldX)) continue;
                if (o.getBounds().intersects(pb)) {
                    if (player.shieldTimer > 0) {
                        player.shieldTimer = 0;
                        addMessage("Shield saved you!", 1.2);
                        beep();
                        itObs.remove();
                        addBurst(player.x + player.width / 2, player.getFeetY() - 20, 22, new Color(100, 200, 255));
                    } else {
                        int dmg = o.damage();
                        player.health = Math.max(0, player.health - dmg);
                        addMessage("-" + dmg + " HP", 0.9);
                        addBurst(o.x - worldX + o.w / 2, o.y - o.h / 2, 18, Color.RED);
                        // knockback
                        player.vx = -120;
                        player.vy = -220;
                        itObs.remove();
                        beep();
                    }
                }
            }

            // Ramps - treat as slope, collision handled in update not here
            // Rails - handled by pressing G near mid-air or auto attach if crossing
            if (!player.isGrinding && player.vy > -50 && !player.onGround) {
                for (Rail r : rails) {
                    if (r.containsX(player.x + worldX) && Math.abs(player.y - r.y) < 20) {
                        if (downPressed || grindPressed) {
                            player.startGrind(r, this);
                            break;
                        }
                    }
                }
            }

            // Coins
            Iterator<Coin> itCoins = coins.iterator();
            while (itCoins.hasNext()) {
                Coin c = itCoins.next();
                double dx = (c.x - worldX) - (player.x + player.width * 0.5);
                double dy = c.y - (player.y - player.height * 0.5);
                double r = 18;
                if (dx * dx + dy * dy < (r + player.width * 0.4) * (r + player.width * 0.4)) {
                    int gain = 10 + combo * 2;
                    score += gain;
                    combo++;
                    comboTimer = 3.0;
                    itCoins.remove();
                    addMessage("+" + gain, 0.5);
                    addBurst(player.x + player.width / 2, player.getFeetY() - 40, 14, new Color(255, 230, 0));
                    beep();
                }
            }

            // Enemies
            Iterator<Enemy> itEn = enemies.iterator();
            while (itEn.hasNext()) {
                Enemy en = itEn.next();
                if (en.getBounds().intersects(pb)) {
                    if (player.shieldTimer > 0) {
                        player.shieldTimer = 0;
                        addMessage("Shield saved you!", 1.2);
                        beep();
                        itEn.remove();
                        addBurst(player.x + player.width / 2, player.getFeetY() - 20, 22, new Color(100, 200, 255));
                    } else {
                        int dmg = 12;
                        player.health = Math.max(0, player.health - dmg);
                        addMessage("-" + dmg + " HP", 0.9);
                        addBurst(en.x - worldX, en.y - en.h / 2, 18, Color.RED);
                        player.vx = -80;
                        player.vy = -180;
                        itEn.remove();
                        beep();
                    }
                }
            }

            // PowerUps
            Iterator<PowerUp> itP = powerUps.iterator();
            while (itP.hasNext()) {
                PowerUp p = itP.next();
                if (p.getBounds().intersects(pb)) {
                    switch (p.type) {
                        case MAGNET:
                            player.magnetTimer = 10.0;
                            addMessage("Magnet!", 1.0);
                            break;
                        case SHIELD:
                            player.shieldTimer = 12.0;
                            addMessage("Shield!", 1.0);
                            break;
                        case BOOST:
                            speed = Math.min(speed + BOOST_SPEED_GAIN, MAX_SPEED + 120);
                            addMessage("Boost!", 1.0);
                            break;
                    }
                    addBurst(player.x + player.width / 2, player.getFeetY() - 30, 18, p.type == PowerUp.Type.MAGNET ? new Color(80, 180, 255) : p.type == PowerUp.Type.SHIELD ? new Color(120, 255, 120) : new Color(255, 150, 60));
                    beep();
                    itP.remove();
                }
            }

            // Landing check for trick scoring
            if (player.justLanded) {
                player.justLanded = false;
                if (Math.abs(normalizeDeg(player.angle)) > LAND_ANGLE_TOLERANCE && player.angleSpeed > 40) {
                    // bad landing - bail penalty
                    int dmg = 8 + (int) (player.angleSpeed / 30);
                    if (player.shieldTimer > 0) {
                        player.shieldTimer = 0;
                        addMessage("Shield saved a bail!", 1.0);
                    } else {
                        player.health = Math.max(0, player.health - dmg);
                        addMessage("Bail -" + dmg + " HP", 1.2);
                        beep();
                    }
                    addBurst(player.x + player.width / 2, player.getFeetY() - 20, 20, Color.RED);
                } else {
                    // Good landing - reward trick
                    int spins = (int) Math.round(Math.abs(player.totalSpin) / 360.0);
                    if (spins > 0) {
                        int trickScore = 50 * spins + (int) (player.airTime * 30);
                        score += trickScore + combo * 5;
                        combo += spins;
                        comboTimer = 3.0;
                        addMessage("Trick +" + trickScore, 1.2);
                        addBurst(player.x + player.width / 2, player.getFeetY() - 20, 20, new Color(120, 255, 120));
                        beep();
                    }
                }
                player.totalSpin = 0;
                player.angle = 0;
                player.angleSpeed = 0;
            }
        }

        private void cleanupEntities() {
            double left = worldX - 200;
            obstacles.removeIf(o -> o.x + o.w < left);
            coins.removeIf(c -> c.x < left || c.life <= 0);
            rails.removeIf(r -> r.x + r.length < left);
            ramps.removeIf(r -> r.x + r.w < left);
            enemies.removeIf(en -> en.x + en.w < left || en.dead);
            powerUps.removeIf(p -> p.x < left || p.collected);
            particles.removeIf(p -> p.life <= 0);
        }

        private void updateParticles(double dt) {
            for (Particle p : particles) {
                p.update(dt, this);
            }
        }

        private void onGameOver() {
            state = State.GAMEOVER;
            highScore = Math.max(highScore, score);
            addMessage("Game Over", 2.0);
        }

        private void beep() {
            if (!muted) Toolkit.getDefaultToolkit().beep();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Sky gradient
            paintSky(g2);

            // Parallax
            for (int i = 0; i < layers.size(); i++) {
                layers.get(i).draw(g2, this, worldX);
            }

            // Ground and road lines
            paintGround(g2);

            // Entities in order: rails behind player? Let’s draw ramps, rails, obstacles, power-ups, coins, enemies, particles accordingly
            drawRails(g2);
            drawRamps(g2);
            drawObstacles(g2);
            drawPowerUps(g2);
            drawCoins(g2);
            drawEnemies(g2);

            // Player and particles (some particles behind, some ahead)
            drawParticlesBehind(g2);
            player.draw(g2, this);
            drawParticlesFront(g2);

            // HUD
            drawHUD(g2);

            // Overlays
            switch (state) {
                case MENU:
                    drawMenu(g2);
                    break;
                case HELP:
                    drawHelp(g2);
                    break;
                case PAUSED:
                    drawPause(g2);
                    break;
                case GAMEOVER:
                    drawGameOver(g2);
                    break;
                default:
                    break;
            }

            if (debug) drawDebug(g2);

            g2.dispose();
        }

        private void paintSky(Graphics2D g2) {
            GradientPaint gp = new GradientPaint(0, 0, SKY_TOP, 0, HEIGHT, SKY_BOTTOM);
            g2.setPaint(gp);
            g2.fillRect(0, 0, WIDTH, HEIGHT);
        }

        private void paintGround(Graphics2D g2) {
            // Asphalt
            g2.setColor(GROUND_COLOR);
            g2.fillRect(0, (int) GROUND_Y, WIDTH, HEIGHT - (int) GROUND_Y);

            // Lane lines moving with worldX
            g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(LANE_LINE);
            double spacing = 140;
            double offset = (worldX % (spacing * 2));
            for (double x = -offset; x < WIDTH + spacing; x += spacing * 2) {
                g2.drawLine((int) x, (int) (GROUND_Y + 20), (int) (x + spacing), (int) (GROUND_Y + 20));
            }

            // Curb
            g2.setColor(new Color(90, 90, 90));
            g2.fillRect(0, (int) (GROUND_Y - 12), WIDTH, 12);
            g2.setColor(new Color(60, 60, 60));
            g2.drawLine(0, (int) (GROUND_Y - 12), WIDTH, (int) (GROUND_Y - 12));
        }

        private void drawRails(Graphics2D g2) {
            for (Rail r : rails) {
                r.draw(g2, this);
            }
        }

        private void drawRamps(Graphics2D g2) {
            for (Ramp r : ramps) {
                r.draw(g2, this);
            }
        }

        private void drawObstacles(Graphics2D g2) {
            for (Obstacle o : obstacles) {
                o.draw(g2, this);
            }
        }

        private void drawCoins(Graphics2D g2) {
            for (Coin c : coins) {
                c.draw(g2, this);
            }
        }

        private void drawEnemies(Graphics2D g2) {
            for (Enemy e : enemies) {
                e.draw(g2, this);
            }
        }

        private void drawPowerUps(Graphics2D g2) {
            for (PowerUp p : powerUps) {
                p.draw(g2, this);
            }
        }

        private void drawParticlesBehind(Graphics2D g2) {
            for (Particle p : particles) {
                if (p.z < 0)
                    p.draw(g2, this);
            }
        }

        private void drawParticlesFront(Graphics2D g2) {
            for (Particle p : particles) {
                if (p.z >= 0)
                    p.draw(g2, this);
            }
        }

        private void drawHUD(Graphics2D g2) {
            // HUD background
            g2.setColor(HUD_BG);
            g2.fillRoundRect(14, 14, 360, 140, 12, 12);
            g2.fillRoundRect(WIDTH - 280, 14, 260, 140, 12, 12);

            g2.setColor(HUD_FG);
            g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
            g2.drawString("Score: " + score, 28, 40);
            g2.drawString("High: " + highScore, 28, 64);
            g2.drawString(String.format("Speed: %.0f", speed), 28, 88);
            g2.drawString(String.format("Distance: %.0f m", distanceTraveled / 10), 28, 112);
            g2.drawString("Combo: " + combo, 28, 136);

            // Health bar
            drawBar(g2, WIDTH - 260, 34, 220, 18, player.health / 100.0, new Color(250, 80, 80), "HP");
            // Stamina bar
            drawBar(g2, WIDTH - 260, 64, 220, 18, player.stamina / STAMINA_MAX, new Color(120, 200, 255), "EN");
            // Shield/Magnet timers
            String effects = "";
            if (player.shieldTimer > 0) effects += String.format("Shield %.0fs  ", player.shieldTimer);
            if (player.magnetTimer > 0) effects += String.format("Magnet %.0fs", player.magnetTimer);
            g2.drawString(effects, WIDTH - 260, 108);

            // Message
            if (!message.isEmpty()) {
                float alpha = (float) Math.max(0, Math.min(1, messageTimer));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1.0f, alpha)));
                g2.setFont(getFont().deriveFont(Font.BOLD, 24f));
                int w = g2.getFontMetrics().stringWidth(message);
                g2.setColor(new Color(0, 0, 0, 140));
                g2.fillRoundRect(WIDTH / 2 - w / 2 - 16, 18, w + 32, 44, 12, 12);
                g2.setColor(Color.WHITE);
                g2.drawString(message, WIDTH / 2 - w / 2, 48);
                g2.setComposite(AlphaComposite.SrcOver);
            }
        }

        private void drawBar(Graphics2D g2, int x, int y, int w, int h, double ratio, Color color, String label) {
            ratio = Math.max(0, Math.min(1, ratio));
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRoundRect(x - 4, y - 4, w + 8, h + 8, 10, 10);
            g2.setColor(new Color(40, 40, 40, 180));
            g2.fillRoundRect(x, y, w, h, 8, 8);
            int fill = (int) Math.round(w * ratio);
            g2.setColor(color);
            g2.fillRoundRect(x, y, fill, h, 8, 8);
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
            g2.drawString(label, x + 6, y + h - 4);
        }

        private void drawMenu(Graphics2D g2) {
            drawOverlay(g2, "ROLLERBLADING ADVENTURE", new String[]{
                    "Press ENTER to Start",
                    "Press H for Help",
                    "Tips: Jump over cones, grind rails, and collect coins!",
                    "Perform spins in the air for trick points. Land straight!"
            });
        }

        private void drawHelp(Graphics2D g2) {
            drawOverlay(g2, "Help / Controls", new String[]{
                    "Up/W/Space: Jump",
                    "Down/S: Crouch",
                    "Left/A and Right/D: Spin mid-air",
                    "Shift: Dash (uses energy)",
                    "G: Grind rail (when above)",
                    "P: Pause/Resume",
                    "R: Restart (from Game Over)",
                    "",
                    "Tricks: Spin while airborne and land within angle tolerance to score.",
                    "Combos: Consecutive coins/tricks increase combo and reward.",
                    "Power-ups: Shield, Magnet, Boost.",
                    "",
                    "Press ESC to go back"
            });
        }

        private void drawPause(Graphics2D g2) {
            drawOverlay(g2, "Paused", new String[]{
                    "Press P to Resume",
                    "Press H for Help",
            });
        }

        private void drawGameOver(Graphics2D g2) {
            drawOverlay(g2, "Game Over", new String[]{
                    "Score: " + score,
                    "High Score: " + highScore,
                    "",
                    "Press R to Restart",
                    "Press ESC for Menu"
            });
        }

        private void drawOverlay(Graphics2D g2, String title, String[] lines) {
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRoundRect(WIDTH / 6, HEIGHT / 7, WIDTH * 2 / 3, HEIGHT * 5 / 7, 16, 16);
            g2.setComposite(old);

            int cx = WIDTH / 2;
            int y = HEIGHT / 7 + 60;
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 42f));
            int tw = g2.getFontMetrics().stringWidth(title);
            g2.drawString(title, cx - tw / 2, y);
            y += 40;

            g2.setFont(getFont().deriveFont(Font.PLAIN, 22f));
            for (String s : lines) {
                y += 36;
                int w = g2.getFontMetrics().stringWidth(s);
                g2.drawString(s, cx - w / 2, y);
            }
        }

        private void drawDebug(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(10, HEIGHT - 100, 360, 90);
            g2.setColor(Color.GREEN);
            g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
            int y = HEIGHT - 80;
            g2.drawString(String.format("worldX=%.2f speed=%.2f", worldX, speed), 16, y);
            y += 18;
            g2.drawString(String.format("player x=%.1f y=%.1f vx=%.1f vy=%.1f", player.x, player.y, player.vx, player.vy), 16, y);
            y += 18;
            g2.drawString(String.format("angle=%.1f spin=%.1f onGround=%s grinding=%s", player.angle, player.totalSpin, player.onGround, player.isGrinding), 16, y);
            y += 18;
            g2.drawString(String.format("entities: obs=%d coins=%d rails=%d ramps=%d en=%d pu=%d parts=%d",
                    obstacles.size(), coins.size(), rails.size(), ramps.size(), enemies.size(), powerUps.size(), particles.size()), 16, y);
        }

        // Input
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            switch (code) {
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
                case KeyEvent.VK_SPACE:
                    upPressed = true;
                    if (state == State.MENU) {
                        state = State.RUNNING;
                        message = "";
                        messageTimer = 0;
                    } else if (state == State.RUNNING) {
                        player.tryJump(this);
                    }
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_S:
                    downPressed = true;
                    break;
                case KeyEvent.VK_SHIFT:
                    dashPressed = true;
                    break;
                case KeyEvent.VK_G:
                    grindPressed = true;
                    break;
                case KeyEvent.VK_P:
                    if (state == State.RUNNING) state = State.PAUSED;
                    else if (state == State.PAUSED) state = State.RUNNING;
                    break;
                case KeyEvent.VK_R:
                    if (state == State.GAMEOVER) {
                        resetGame();
                        state = State.RUNNING;
                    }
                    break;
                case KeyEvent.VK_H:
                    if (state == State.RUNNING) state = State.HELP;
                    else if (state == State.HELP) state = State.RUNNING;
                    else state = State.HELP;
                    break;
                case KeyEvent.VK_ENTER:
                    if (state == State.MENU) {
                        resetGame();
                        state = State.RUNNING;
                    }
                    break;
                case KeyEvent.VK_ESCAPE:
                    if (state == State.HELP || state == State.PAUSED || state == State.GAMEOVER) {
                        state = State.MENU;
                    } else if (state == State.RUNNING) {
                        state = State.PAUSED;
                    } else if (state == State.MENU) {
                        // close window perhaps?
                    }
                    break;
                case KeyEvent.VK_F3:
                    debug = !debug;
                    break;
                case KeyEvent.VK_M:
                    muted = !muted;
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();
            switch (code) {
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
                case KeyEvent.VK_SPACE:
                    upPressed = false;
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_S:
                    downPressed = false;
                    break;
                case KeyEvent.VK_SHIFT:
                    dashPressed = false;
                    break;
                case KeyEvent.VK_G:
                    grindPressed = false;
                    break;
            }
        }

        // Helpers
        private void addMessage(String msg, double time) {
            message = msg;
            messageTimer = time;
        }

        private void addDust(double x, double y, int count, int z) {
            for (int i = 0; i < count; i++) {
                double ang = rng.nextDouble() * Math.PI - Math.PI / 2;
                double spd = 60 + rng.nextDouble() * 80;
                particles.add(Particle.dust(x, y, Math.cos(ang) * spd, -Math.abs(Math.sin(ang) * spd), z));
            }
        }

        private void addBurst(double x, double y, int count, Color color) {
            for (int i = 0; i < count; i++) {
                double ang = rng.nextDouble() * Math.PI * 2;
                double spd = 120 + rng.nextDouble() * 180;
                particles.add(Particle.spark(x, y, Math.cos(ang) * spd, Math.sin(ang) * spd, color));
            }
        }

        private static double normalizeDeg(double a) {
            a %= 360.0;
            if (a > 180) a -= 360;
            if (a < -180) a += 360;
            return a;
        }

        // Inner classes

        class Player {
            double x = 240;
            double y = GROUND_Y - 50;
            double vx = 0;
            double vy = 0;
            double width = 36;
            double height = 72;

            boolean onGround = true;
            boolean isGrinding = false;
            Rail grindRail = null;
            double grindProgress = 0;

            double angle = 0; // for tricks
            double angleSpeed = 0;
            double totalSpin = 0;
            double airTime = 0;
            boolean justLanded = false;

            double health = 100;
            double stamina = STAMINA_MAX;
            double shieldTimer = 0;
            double magnetTimer = 0;

            // Animation timer
            double runPhase = 0;

            void reset() {
                x = 240;
                y = GROUND_Y - 50;
                vx = 0;
                vy = 0;
                onGround = true;
                isGrinding = false;
                grindRail = null;
                grindProgress = 0;
                angle = 0;
                angleSpeed = 0;
                totalSpin = 0;
                airTime = 0;
                justLanded = false;
                health = 100;
                stamina = STAMINA_MAX;
                shieldTimer = 0;
                magnetTimer = 0;
                runPhase = 0;
            }

            void tryJump(GamePanel gp) {
                if (isGrinding) {
                    // hop off the rail
                    vy = -JUMP_VELOCITY * 0.75;
                    vx += 40;
                    isGrinding = false;
                    grindRail = null;
                    addBurst(x + width / 2, y - height, 12, new Color(250, 250, 140));
                    return;
                }
                if (onGround) {
                    vy = -JUMP_VELOCITY;
                    onGround = false;
                    airTime = 0;
                    addDust(x, getFeetY(), 6, -1);
                }
            }

            double getFeetY() {
                return y;
            }

            Rectangle2D.Double getBounds() {
                double h = downPressed && onGround ? height * CROUCH_HEIGHT_FACTOR : height;
                return new Rectangle2D.Double(x + 4, y - h, width - 8, h);
            }

            void startGrind(Rail r, GamePanel gp) {
                if (r == null) return;
                isGrinding = true;
                onGround = false;
                grindRail = r;
                // compute progress along rail given worldX + x
                double worldPos = worldX + x;
                grindProgress = (worldPos - r.x) / r.length;
                vy = 0;
                y = r.y;
                speed = Math.min(speed + RAIL_SPEED_GAIN, MAX_SPEED + 50);
                addMessage("Grind!", 0.6);
                for (int i = 0; i < 10; i++) {
                    particles.add(Particle.spark(x + width / 2, y, 60 + rng.nextDouble() * 120, -80 + rng.nextDouble() * 160, new Color(255, 240, 120)));
                }
            }

            void update(double dt, GamePanel gp) {
                // Timers
                if (shieldTimer > 0) shieldTimer -= dt;
                if (magnetTimer > 0) magnetTimer -= dt;

                // Spin controls in air
                if (!onGround && !isGrinding) {
                    double spinDir = 0;
                    if (leftPressed) spinDir -= 1;
                    if (rightPressed) spinDir += 1;
                    angleSpeed = SPIN_SPEED * spinDir;
                    angle += angleSpeed * dt;
                    totalSpin += angleSpeed * dt;
                    airTime += dt;
                } else {
                    angleSpeed *= 0.9;
                    angle *= 0.9;
                }

                // Crouch effect
                if (downPressed && onGround) {
                    vx += 20 * dt;
                }

                // Grinding movement
                if (isGrinding && grindRail != null) {
                    // progress increases with world speed
                    double progressSpeed = (speed * dt) / grindRail.length;
                    grindProgress += progressSpeed;
                    y = grindRail.y - 2 + Math.sin(worldX * 0.02) * 0.5;
                    // sparks while grinding
                    if (rng.nextDouble() < 0.4) {
                        particles.add(Particle.spark(x + width / 2, y, 50 + rng.nextDouble() * 60, -30 + rng.nextDouble() * 60, new Color(255, 220, 120)));
                    }
                    if (grindProgress >= 1.0) {
                        // end of rail
                        isGrinding = false;
                        grindRail = null;
                        vy = -260;
                        addBurst(x + width / 2, y - height / 2, 12, new Color(250, 250, 140));
                    }
                } else {
                    // Apply gravity
                    vy += GRAVITY * dt;

                    // Apply friction if on ground
                    if (onGround) {
                        vx -= vx * Math.min(1.0, FRICTION * dt);
                    }

                    // Jump ramp impulse if on a ramp
                    for (Ramp r : ramps) {
                        if (r.affects(this, worldX)) {
                            Point2D.Double n = r.normal();
                            vx += n.x * 90 * dt;
                            vy += n.y * 350 * dt;
                        }
                    }

                    // Move
                    x += vx * dt;
                    y += vy * dt;

                    // Ground collision
                    double h = downPressed && onGround ? height * CROUCH_HEIGHT_FACTOR : height;
                    double feet = getFeetY();
                    if (feet >= GROUND_Y) {
                        // Landing
                        if (!onGround && vy > 80) {
                            justLanded = true;
                        }
                        onGround = true;
                        y = GROUND_Y;
                        vy = 0;
                        vx *= 0.8;
                        airTime = 0;
                        // generate dust trail based on speed
                        if (speed > 300 && rng.nextDouble() < 0.7) {
                            addDust(x, getFeetY(), 1, -1);
                        }
                    } else {
                        onGround = false;
                    }

                    // Keep on screen horizontally
                    x = Math.max(80, Math.min(x, WIDTH - 200));
                }

                // Trail particles while dashing
                if (dashPressed && stamina > 0) {
                    if (rng.nextDouble() < 0.6) {
                        particles.add(Particle.trail(x + width / 2, y - height / 2, -speed * 0.5 - rng.nextDouble() * 60, 0));
                    }
                }
            }

            void draw(Graphics2D g2, GamePanel gp) {
                double drawX = x;
                double drawY = y;

                // Shadow
                g2.setColor(new Color(0, 0, 0, 80));
                g2.fillOval((int) (drawX - 16), (int) (GROUND_Y - 4), 64, 10);

                // Shield effect
                if (shieldTimer > 0) {
                    float t = (float) ((Math.sin(shieldTimer * 5) * 0.25 + 0.75) * 0.5);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, t));
                    g2.setColor(new Color(120, 220, 255, 120));
                    g2.fillOval((int) (drawX - 12), (int) (drawY - height - 10), 60, (int) (height + 20));
                    g2.setComposite(AlphaComposite.SrcOver);
                }

                // Rollerblader cartoon figure (stickman-like with some body)
                // Body parts sizes
                double bodyH = downPressed && onGround ? height * CROUCH_HEIGHT_FACTOR : height;
                double bodyW = width;
                double torsoH = bodyH * 0.5;
                double headR = 12;
                double legH = bodyH * 0.5;

                AffineTransform at = g2.getTransform();
                // Apply rotation for tricks (around center)
                double cx = drawX + bodyW / 2;
                double cy = drawY - bodyH * 0.5;
                if (!onGround || isGrinding) {
                    g2.rotate(Math.toRadians(angle), cx, cy);
                }

                // Torso
                g2.setColor(new Color(60, 80, 100));
                g2.fillRoundRect((int) (drawX), (int) (drawY - bodyH), (int) bodyW, (int) torsoH, 8, 8);

                // Head
                g2.setColor(new Color(255, 225, 200));
                g2.fillOval((int) (drawX + bodyW * 0.2), (int) (drawY - bodyH - headR * 1.8), (int) (headR * 2), (int) (headR * 2));

                // Arms (animated)
                g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(80, 110, 140));
                runPhase += speed * 0.005;
                double swing = Math.sin(runPhase) * (onGround ? 10 : 2);
                double ax1 = drawX + bodyW * 0.1;
                double ay1 = drawY - bodyH + 10;
                double ax2 = drawX + bodyW * 0.1 - 14;
                double ay2 = drawY - bodyH + 40 + swing;
                g2.drawLine((int) ax1, (int) ay1, (int) ax2, (int) ay2);

                double bx1 = drawX + bodyW * 0.9;
                double by1 = drawY - bodyH + 10;
                double bx2 = drawX + bodyW * 0.9 + 14;
                double by2 = drawY - bodyH + 40 - swing;
                g2.drawLine((int) bx1, (int) by1, (int) bx2, (int) by2);

                // Legs with skates
                g2.setColor(new Color(80, 80, 80));
                double leg1x1 = drawX + bodyW * 0.3;
                double leg1y1 = drawY - bodyH + torsoH;
                double leg1x2 = leg1x1 - 4;
                double leg1y2 = drawY - 6 + Math.sin(runPhase) * 3;
                g2.drawLine((int) leg1x1, (int) leg1y1, (int) leg1x2, (int) leg1y2);

                double leg2x1 = drawX + bodyW * 0.7;
                double leg2y1 = drawY - bodyH + torsoH;
                double leg2x2 = leg2x1 + 4;
                double leg2y2 = drawY - 6 - Math.sin(runPhase) * 3;
                g2.drawLine((int) leg2x1, (int) leg2y1, (int) leg2x2, (int) leg2y2);

                // Skates
                g2.setColor(new Color(30, 30, 30));
                g2.fillRoundRect((int) (leg1x2 - 10), (int) (leg1y2 - 4), 24, 8, 4, 4);
                g2.fillRoundRect((int) (leg2x2 - 10), (int) (leg2y2 - 4), 24, 8, 4, 4);

                // Wheels
                g2.setColor(new Color(200, 200, 200));
                int wheelCount = 3;
                double wheelGap = 7;
                for (int i = 0; i < wheelCount; i++) {
                    g2.fillOval((int) (leg1x2 - 6 + i * wheelGap), (int) (leg1y2 + 2), 6, 6);
                    g2.fillOval((int) (leg2x2 - 6 + i * wheelGap), (int) (leg2y2 + 2), 6, 6);
                }

                // Restore transform
                g2.setTransform(at);

                // Magnet effect aura
                if (magnetTimer > 0) {
                    float t = (float) (0.4 + 0.2 * Math.sin(magnetTimer * 5));
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, t));
                    g2.setColor(new Color(100, 180, 255, 100));
                    g2.drawOval((int) (cx - MAGNET_RANGE / 2), (int) (cy - MAGNET_RANGE / 2), (int) MAGNET_RANGE, (int) MAGNET_RANGE);
                    g2.setComposite(AlphaComposite.SrcOver);
                }

                // Grinding indicator
                if (isGrinding) {
                    g2.setColor(new Color(255, 240, 100));
                    g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
                    g2.drawString("Grinding!", (int) (x), (int) (y - height - 16));
                }
            }
        }

        static class Obstacle {
            double x, y; // x is world coordinate, y is ground level
            double w, h; // width, height
            Type type;

            enum Type { CONE, BARRIER, BENCH }

            Obstacle(double x, double groundY, double w, double h, Type type) {
                this.x = x;
                this.y = groundY;
                this.w = w;
                this.h = h;
                this.type = type;
            }

            Rectangle2D.Double getBounds() {
                return new Rectangle2D.Double(x - w / 2, y - h, w, h);
            }

            int damage() {
                switch (type) {
                    case CONE:
                        return 6;
                    case BARRIER:
                        return 15;
                    case BENCH:
                        return 10;
                }
                return 8;
            }

            void update(double dt, GamePanel gp) {
                // No movement (world scroll handles it)
            }

            void draw(Graphics2D g2, GamePanel gp) {
                double sx = x - gp.worldX;
                if (sx + w < -50 || sx - w > WIDTH + 50) return;
                switch (type) {
                    case CONE:
                        g2.setColor(new Color(255, 120, 60));
                        Polygon cone = new Polygon();
                        cone.addPoint((int) (sx), (int) (y - h - 4));
                        cone.addPoint((int) (sx - w / 2), (int) (y));
                        cone.addPoint((int) (sx + w / 2), (int) (y));
                        g2.fillPolygon(cone);
                        g2.setColor(Color.WHITE);
                        g2.fillRect((int) (sx - w / 3), (int) (y - h / 2), (int) (2 * w / 3), 6);
                        break;
                    case BARRIER:
                        g2.setColor(new Color(180, 180, 180));
                        g2.fillRoundRect((int) (sx - w / 2), (int) (y - h), (int) w, (int) h, 6, 6);
                        g2.setColor(new Color(220, 220, 220));
                        g2.drawLine((int) (sx - w / 2), (int) (y - h / 2), (int) (sx + w / 2), (int) (y - h / 2));
                        break;
                    case BENCH:
                        g2.setColor(new Color(100, 60, 40));
                        g2.fillRect((int) (sx - w / 2), (int) (y - 10), (int) w, 10);
                        g2.fillRect((int) (sx - w / 2), (int) (y - h - 10), (int) w, 14);
                        g2.fillRect((int) (sx - w / 2 + 8), (int) (y - h - 10), 8, (int) h);
                        g2.fillRect((int) (sx + w / 2 - 16), (int) (y - h - 10), 8, (int) h);
                        break;
                }
            }

            boolean isOffscreen(double worldX) {
                return x - worldX + w < -200;
            }
        }

        static class Coin {
            double x, y;
            double vx = 0, vy = 0;
            double life = 999;

            Coin(double x, double y) {
                this.x = x;
                this.y = y;
            }

            void update(double dt, GamePanel gp) {
                // Slight float
                vy += 20 * dt;
                vx *= 0.98;
                vy *= 0.98;
                x += vx * dt;
                y += vy * dt;
                // remove if too high or too low? will be cleaned offscreen
            }

            void draw(Graphics2D g2, GamePanel gp) {
                double sx = x - gp.worldX;
                if (sx < -50 || sx > WIDTH + 50) return;
                g2.setColor(new Color(255, 230, 0));
                g2.fillOval((int) (sx - 12), (int) (y - 12), 24, 24);
                g2.setColor(new Color(255, 255, 200));
                g2.drawArc((int) (sx - 10), (int) (y - 10), 20, 20, 45, 90);
            }
        }

        static class Rail {
            double x, y, length;

            Rail(double x, double y, double length) {
                this.x = x;
                this.y = y;
                this.length = length;
            }

            boolean containsX(double worldX) {
                return worldX >= x && worldX <= x + length;
            }

            Rectangle2D.Double getBounds(double worldX) {
                return new Rectangle2D.Double(x - worldX, y - 4, length, 8);
            }

            void update(double dt, GamePanel gp) {
                // nothing
            }

            void draw(Graphics2D g2, GamePanel gp) {
                double sx = x - gp.worldX;
                g2.setColor(new Color(200, 200, 200));
                g2.setStroke(new BasicStroke(4));
                g2.drawLine((int) (sx), (int) y, (int) (sx + length), (int) y);
                g2.setColor(new Color(110, 110, 110));
                g2.fillRect((int) (sx - 4), (int) (y + 6), 8, 16);
                g2.fillRect((int) (sx + length - 4), (int) (y + 6), 8, 16);
            }
        }

        static class Ramp {
            double x, y; // base bottom-left
            double w, h; // width, height

            Ramp(double x, double groundY, double w, double h) {
                this.x = x;
                this.y = groundY;
                this.w = w;
                this.h = h;
            }

            boolean affects(Player p, double worldX) {
                double sx = x - worldX;
                Rectangle2D.Double pb = p.getBounds();
                double left = sx;
                double right = sx + w;
                // If feet are near ramp surface
                if (pb.getMaxX() > left && pb.getMinX() < right) {
                    double relX = ((p.x) - left) / w;
                    if (relX >= 0 && relX <= 1.0) {
                        double surfY = y - h * relX; // linear ramp
                        if (p.getFeetY() >= surfY - 2 && p.getFeetY() <= surfY + 8) {
                            // Land on ramp
                            p.y = surfY;
                            p.vy = Math.min(p.vy, 0);
                            p.onGround = true;
                            return true;
                        }
                    }
                }
                return false;
            }

            Point2D.Double normal() {
                // normal pointing up-leftish
                double nx = -h;
                double ny = w;
                double len = Math.sqrt(nx * nx + ny * ny);
                return new Point2D.Double(nx / len, ny / len);
            }

            void update(double dt, GamePanel gp) {}

            void draw(Graphics2D g2, GamePanel gp) {
                double sx = x - gp.worldX;
                Polygon ramp = new Polygon();
                ramp.addPoint((int) sx, (int) y);
                ramp.addPoint((int) (sx + w), (int) (y));
                ramp.addPoint((int) (sx + w), (int) (y - h));
                g2.setColor(new Color(160, 160, 160));
                g2.fillPolygon(ramp);
                g2.setColor(new Color(120, 120, 120));
                g2.drawPolygon(ramp);
            }
        }

        static class Enemy {
            double x, y, w, h;
            double vx, vy;
            boolean dead = false;
            Type type;

            enum Type { DOG, PIGEON }

            Enemy(double x, double groundY, Type type) {
                this.type = type;
                switch (type) {
                    case DOG:
                        this.w = 80;
                        this.h = 40;
                        this.y = groundY;
                        this.vx = -80 - Math.random() * 80;
                        break;
                    case PIGEON:
                        this.w = 40;
                        this.h = 20;
                        this.y = groundY - 180 - Math.random() * 80;
                        this.vx = -120 - Math.random() * 120;
                        break;
                }
                this.x = x;
            }

            Rectangle2D.Double getBounds() {
                return new Rectangle2D.Double(x - w / 2, y - h, w, h);
            }

            void update(double dt, GamePanel gp) {
                x += vx * dt;
                y += vy * dt;
                if (type == Type.PIGEON) {
                    vy = Math.sin((x + gp.worldX) * 0.01) * 20;
                }
            }

            void draw(Graphics2D g2, GamePanel gp) {
                double sx = x - gp.worldX;
                if (sx + w < -50 || sx - w > WIDTH + 50) return;
                switch (type) {
                    case DOG:
                        g2.setColor(new Color(120, 80, 60));
                        g2.fillRoundRect((int) (sx - w / 2), (int) (y - h), (int) w, (int) h, 8, 8);
                        g2.setColor(Color.BLACK);
                        g2.fillOval((int) (sx + w / 2 - 18), (int) (y - h + 6), 10, 10);
                        break;
                    case PIGEON:
                        g2.setColor(new Color(180, 180, 180));
                        g2.fillOval((int) (sx - w / 2), (int) (y - h), (int) w, (int) h);
                        g2.setColor(Color.DARK_GRAY);
                        g2.drawLine((int) (sx - 4), (int) (y - h + 8), (int) (sx - 10), (int) (y - h - 2));
                        break;
                }
            }
        }

        static class PowerUp {
            double x, y;
            Type type;
            boolean collected = false;

            enum Type { SHIELD, MAGNET, BOOST }

            PowerUp(double x, double y, Type type) {
                this.x = x;
                this.y = y;
                this.type = type;
            }

            Rectangle2D.Double getBounds() {
                return new Rectangle2D.Double(x - 16, y - 16, 32, 32);
            }

            void update(double dt, GamePanel gp) {
                // bob up and down
                y += Math.sin(x * 0.01 + gp.worldX * 0.02) * 10 * dt;
            }

            void draw(Graphics2D g2, GamePanel gp) {
                double sx = x - gp.worldX;
                if (sx < -50 || sx > WIDTH + 50) return;
                Shape shape;
                Color color;
                switch (type) {
                    case SHIELD:
                        color = new Color(120, 220, 120);
                        shape = new RoundRectangle2D.Double(sx - 14, y - 18, 28, 36, 10, 10);
                        break;
                    case MAGNET:
                        color = new Color(100, 180, 255);
                        shape = new Arc2D.Double(sx - 18, y - 18, 36, 36, 45, 270, Arc2D.OPEN);
                        break;
                    default:
                        color = new Color(255, 150, 60);
                        shape = new Ellipse2D.Double(sx - 16, y - 16, 32, 32);
                        break;
                }
                g2.setColor(color);
                g2.fill(shape);
                g2.setColor(Color.WHITE);
                g2.draw(shape);
            }
        }

        static class Particle {
            double x, y, vx, vy, life, maxLife;
            int z; // layer: behind (<0) or front (>=0)
            Color color;
            double size;
            double gravity;

            static Particle dust(double x, double y, double vx, double vy, int z) {
                Particle p = new Particle();
                p.x = x;
                p.y = y;
                p.vx = vx;
                p.vy = vy;
                p.z = z;
                p.life = p.maxLife = 0.6 + Math.random() * 0.4;
                p.color = new Color(90, 90, 90, 180);
                p.size = 4 + Math.random() * 3;
                p.gravity = 120;
                return p;
            }

            static Particle spark(double x, double y, double vx, double vy, Color c) {
                Particle p = new Particle();
                p.x = x;
                p.y = y;
                p.vx = vx;
                p.vy = vy;
                p.z = 1;
                p.life = p.maxLife = 0.5 + Math.random() * 0.5;
                p.color = c;
                p.size = 3 + Math.random() * 2;
                p.gravity = 300;
                return p;
            }

            static Particle trail(double x, double y, double vx, double vy) {
                Particle p = new Particle();
                p.x = x;
                p.y = y;
                p.vx = vx;
                p.vy = vy;
                p.z = 0;
                p.life = p.maxLife = 0.4 + Math.random() * 0.2;
                p.color = new Color(130, 200, 255, 130);
                p.size = 6 + Math.random() * 4;
                p.gravity = 0;
                return p;
            }

            void update(double dt, GamePanel gp) {
                vy += gravity * dt;
                x += vx * dt;
                y += vy * dt;
                life -= dt;
            }

            void draw(Graphics2D g2, GamePanel gp) {
                if (life <= 0) return;
                float a = (float) Math.max(0, life / maxLife);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1.0f, a)));
                g2.setColor(color);
                g2.fillOval((int) (x - size / 2), (int) (y - size / 2), (int) size, (int) size);
                g2.setComposite(AlphaComposite.SrcOver);
            }
        }

        static class ParallaxLayer {
            double speedFactor;
            double yOffset;
            double baseY;
            boolean pattern;

            ParallaxLayer(double speedFactor, double yOffset, double baseY, boolean pattern) {
                this.speedFactor = speedFactor;
                this.yOffset = yOffset;
                this.baseY = baseY;
                this.pattern = pattern;
            }

            void draw(Graphics2D g2, GamePanel gp, double worldX) {
                double xOff = -(worldX * speedFactor) % 600;
                if (!pattern) {
                    // clouds
                    g2.setColor(new Color(255, 255, 255, 180));
                    for (int i = -1; i < 4; i++) {
                        double cx = xOff + i * 600;
                        drawCloud(g2, cx + 200, 120);
                        drawCloud(g2, cx + 420, 180);
                        drawCloud(g2, cx + 80, 80);
                    }
                } else {
                    // silhouette pattern (buildings/trees/foreground)
                    Color c;
                    if (baseY > HEIGHT - 250) c = new Color(40, 90, 60, 190); // bushes
                    else if (baseY > HEIGHT - 350) c = new Color(60, 120, 80, 160); // trees
                    else c = new Color(90, 120, 160, 150); // city
                    g2.setColor(c);
                    for (int i = -1; i < 6; i++) {
                        double bx = xOff + i * 300;
                        if (baseY > HEIGHT - 350) {
                            drawTrees(g2, bx, baseY);
                        } else {
                            drawCity(g2, bx, baseY);
                        }
                    }
                }
            }

            private void drawCloud(Graphics2D g2, double x, double y) {
                g2.fillOval((int) (x - 30), (int) (y - 15), 60, 30);
                g2.fillOval((int) (x - 50), (int) (y - 10), 50, 22);
                g2.fillOval((int) (x + 10), (int) (y - 10), 50, 22);
            }

            private void drawCity(Graphics2D g2, double x, double baseY) {
                for (int i = 0; i < 7; i++) {
                    int w = 30 + (int) (Math.random() * 50);
                    int h = 60 + (int) (Math.random() * 180);
                    int bx = (int) (x + i * 40);
                    g2.fillRect(bx, (int) (baseY - h), w, h);
                }
            }

            private void drawTrees(Graphics2D g2, double x, double baseY) {
                for (int i = 0; i < 6; i++) {
                    int h = 30 + (int) (Math.random() * 50);
                    int bx = (int) (x + i * 45);
                    g2.fillOval(bx, (int) (baseY - h), 40, h);
                }
            }
        }
    }
}