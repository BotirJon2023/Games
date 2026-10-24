import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class HoverboardCanadaRacing extends JPanel implements ActionListener, KeyListener {
    // Screen settings
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final int TRACK_LENGTH = 12000;

    // Game states
    private enum State { MENU, RACING, FINISHED }
    private State state = State.MENU;
    private enum Mode { VS_COMPUTER, VS_PLAYER }
    private Mode mode = Mode.VS_COMPUTER;

    // Player 1 (blue/cyan hoverboard - Canadian ice)
    private double p1X = 200;
    private double p1Y = HEIGHT - 200;
    private double p1Speed = 0;
    private double p1Distance = 0;
    private double p1Tilt = 0;
    private boolean p1Up, p1Down, p1Left, p1Right, p1Boost;
    private double p1BoostFuel = 100;
    private double p1BoostCooldown = 0;

    // Player 2 / AI (red hoverboard - Canadian maple)
    private double p2X = 200;
    private double p2Y = HEIGHT - 260;
    private double p2Speed = 0;
    private double p2Distance = 0;
    private double p2Tilt = 0;
    private boolean p2Up, p2Down, p2Left, p2Right, p2Boost;
    private double p2BoostFuel = 100;
    private double p2BoostCooldown = 0;

    // AI
    private Random random = new Random();
    private double aiDecisionTimer = 0;
    private double aiTargetY;

    // Obstacles & Power-ups
    private ArrayList<Obstacle> obstacles = new ArrayList<>();
    private ArrayList<PowerUp> powerUps = new ArrayList<>();

    // Visual effects
    private ArrayList<Particle> particles = new ArrayList<>();
    private ArrayList<Trail> trails = new ArrayList<>();
    private ArrayList<Snowflake> snowflakes = new ArrayList<>();
    private ArrayList<Cloud> clouds = new ArrayList<>();
    private ArrayList<Tree> trees = new ArrayList<>();
    private ArrayList<MapleLeaf> mapleLeaves = new ArrayList<>();
    private ArrayList<CanadianGoose> geese = new ArrayList<>();
    private ArrayList<LodgeLog> lodges = new ArrayList<>();

    private double cameraShake = 0;
    private double worldOffset = 0;
    private double waveTime = 0;
    private double auroraTime = 0;
    private double sunPulse = 0;

    // Game loop
    private Timer timer;
    private int winner = -1;

    public HoverboardCanadaRacing() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(16, this);
        timer.start();

        initEnvironment();
        generateWorld();
    }

    private void initEnvironment() {
        Random r = new Random(7);
        // Trees (Canadian pines and maples)
        for (int i = 0; i < 50; i++) {
            trees.add(new Tree(i * 90 + r.nextInt(60), 3 + r.nextInt(3), r.nextInt(3)));
        }
        // Clouds
        for (int i = 0; i < 10; i++) {
            clouds.add(new Cloud(r.nextInt(WIDTH), 40 + r.nextInt(200),
                    60 + r.nextInt(80), 0.2 + r.nextDouble() * 0.6));
        }
        // Snowflakes
        for (int i = 0; i < 120; i++) {
            snowflakes.add(new Snowflake(r.nextInt(WIDTH), r.nextInt(HEIGHT),
                    0.5 + r.nextDouble() * 1.5, 0.5 + r.nextDouble()));
        }
        // Maple leaves falling
        for (int i = 0; i < 30; i++) {
            mapleLeaves.add(new MapleLeaf(r.nextInt(WIDTH), r.nextInt(HEIGHT),
                    r.nextDouble() * Math.PI * 2, 1 + r.nextDouble()));
        }
        // Canadian geese flying
        for (int i = 0; i < 5; i++) {
            geese.add(new CanadianGoose(-100 - r.nextInt(600), 100 + r.nextInt(150),
                    0.8 + r.nextDouble() * 0.5));
        }
        // Wooden lodges in the distance
        for (int i = 0; i < 6; i++) {
            lodges.add(new LodgeLog(i * 400 + r.nextInt(200), 0.35));
        }
    }

    private void generateWorld() {
        obstacles.clear();
        powerUps.clear();
        Random r = new Random(42);
        for (double d = 800; d < TRACK_LENGTH - 500; d += 300 + r.nextInt(200)) {
            double y = 250 + r.nextInt(HEIGHT - 450);
            int type = r.nextInt(4);
            obstacles.add(new Obstacle(d, y, type));
        }
        for (double d = 1500; d < TRACK_LENGTH - 500; d += 1100 + r.nextInt(700)) {
            powerUps.add(new PowerUp(d, 250 + r.nextInt(HEIGHT - 450)));
        }
    }

    private void reset() {
        p1Distance = 0; p2Distance = 0;
        p1Speed = 0; p2Speed = 0;
        p1BoostFuel = 100; p2BoostFuel = 100;
        p1BoostCooldown = 0; p2BoostCooldown = 0;
        p1Tilt = 0; p2Tilt = 0;
        p1Y = HEIGHT - 200; p2Y = HEIGHT - 260;
        winner = -1;
        particles.clear();
        trails.clear();
        aiDecisionTimer = 0;
        worldOffset = 0;
        generateWorld();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == State.RACING) update();
        waveTime += 0.05;
        auroraTime += 0.02;
        sunPulse += 0.03;
        updateEnvironment();
        repaint();
    }

    private void updateEnvironment() {
        for (Snowflake s : snowflakes) {
            s.y += s.speed;
            s.x += Math.sin(waveTime + s.y * 0.05) * 0.5 + s.drift;
            if (s.y > HEIGHT) { s.y = -10; s.x = random.nextInt(WIDTH); }
            if (s.x < -10) s.x = WIDTH;
            if (s.x > WIDTH + 10) s.x = 0;
        }
        for (Cloud c : clouds) {
            c.x -= c.speed;
            if (c.x < -c.w - 20) { c.x = WIDTH + 20; c.y = 40 + random.nextInt(200); }
        }
        for (MapleLeaf m : mapleLeaves) {
            m.y += m.speed * 0.8;
            m.x += Math.sin(m.rotation) * 1.2;
            m.rotation += 0.03;
            if (m.y > HEIGHT) { m.y = -20; m.x = random.nextInt(WIDTH); }
            if (m.x < -20) m.x = WIDTH + 20;
            if (m.x > WIDTH + 20) m.x = -20;
        }
        for (CanadianGoose g : geese) {
            g.x += g.speed;
            g.y += Math.sin(waveTime + g.x * 0.02) * 0.3;
            if (g.x > WIDTH + 100) {
                g.x = -150 - random.nextInt(400);
                g.y = 100 + random.nextInt(150);
            }
        }
    }

    private void update() {
        // ---- Player 1 ----
        double accel = 0.6;
        double friction = 0.94;
        double maxSpeed = p1Boost ? 14 : 9;

        if (p1Boost && p1BoostFuel > 0 && p1BoostCooldown <= 0) {
            p1Speed += accel * 1.6;
            p1BoostFuel -= 0.8;
            spawnBoostParticles(p1X, p1Y, new Color(80, 220, 255));
        } else {
            p1Speed += accel * 0.5;
        }
        if (p1Up) p1Y -= 5;
        if (p1Down) p1Y += 5;
        if (p1Left) p1Speed -= 0.3;
        if (p1Right) p1Speed += 0.4;

        p1Speed = Math.max(1, Math.min(maxSpeed, p1Speed));
        p1Speed *= friction + 0.04;
        p1Y = Math.max(200, Math.min(HEIGHT - 80, p1Y));
        p1Distance += p1Speed;

        if (p1BoostCooldown > 0) p1BoostCooldown--;
        if (!p1Boost || p1BoostFuel <= 0) p1BoostFuel = Math.min(100, p1BoostFuel + 0.15);

        // ---- Player 2 / AI ----
        double accel2 = 0.6;
        double maxSpeed2 = p2Boost ? 14 : 9;

        if (mode == Mode.VS_COMPUTER) {
            aiUpdate();
        } else {
            if (p2Boost && p2BoostFuel > 0 && p2BoostCooldown <= 0) {
                p2Speed += accel2 * 1.6;
                p2BoostFuel -= 0.8;
                spawnBoostParticles(p2X, p2Y, new Color(255, 80, 80));
            } else {
                p2Speed += accel2 * 0.5;
            }
            if (p2Up) p2Y -= 5;
            if (p2Down) p2Y += 5;
            if (p2Left) p2Speed -= 0.3;
            if (p2Right) p2Speed += 0.4;
        }
        p2Speed = Math.max(1, Math.min(maxSpeed2, p2Speed));
        p2Speed *= friction + 0.04;
        p2Y = Math.max(200, Math.min(HEIGHT - 80, p2Y));
        p2Distance += p2Speed;

        if (p2BoostCooldown > 0) p2BoostCooldown--;
        if (!p2Boost || p2BoostFuel <= 0) p2BoostFuel = Math.min(100, p2BoostFuel + 0.15);

        // ---- World scroll ----
        worldOffset += p1Speed * 0.6;

        // ---- Tilt ----
        p1Tilt += ((p1Up ? -0.3 : p1Down ? 0.3 : 0) - p1Tilt) * 0.1;
        p2Tilt += ((p2Up ? -0.3 : p2Down ? 0.3 : 0) - p2Tilt) * 0.1;

        // ---- Collisions ----
        Rectangle2D p1Rect = new Rectangle2D.Double(p1X - 40, p1Y - 20, 80, 30);
        Rectangle2D p2Rect = new Rectangle2D.Double(p2X - 40, p2Y - 20, 80, 30);

        for (Obstacle o : obstacles) {
            double sx = o.distance - p1Distance + 400;
            if (sx > -150 && sx < WIDTH + 150) {
                Rectangle2D oRect = new Rectangle2D.Double(sx - o.size / 2, o.y - o.size / 2, o.size, o.size);
                if (p1Rect.intersects(oRect) && !o.hit1) {
                    p1Speed *= 0.35;
                    cameraShake = 12;
                    spawnExplosion(sx, o.y, new Color(255, 80, 80));
                    o.hit1 = true;
                }
                if (p2Rect.intersects(oRect) && !o.hit2) {
                    p2Speed *= 0.35;
                    cameraShake = 12;
                    spawnExplosion(sx, o.y, new Color(255, 200, 80));
                    o.hit2 = true;
                }
            }
            if (sx < -200 || sx > WIDTH + 200) { o.hit1 = false; o.hit2 = false; }
        }

        for (PowerUp p : powerUps) {
            if (p.collected) continue;
            double sx = p.distance - p1Distance + 400;
            if (sx > -100 && sx < WIDTH + 100) {
                Rectangle2D pRect = new Rectangle2D.Double(sx - 20, p.y - 20, 40, 40);
                if (p1Rect.intersects(pRect)) {
                    p.collected = true;
                    p1BoostFuel = Math.min(100, p1BoostFuel + 50);
                    spawnExplosion(sx, p.y, new Color(255, 100, 100));
                } else if (p2Rect.intersects(pRect)) {
                    p.collected = true;
                    p2BoostFuel = Math.min(100, p2BoostFuel + 50);
                    spawnExplosion(sx, p.y, new Color(255, 255, 120));
                }
            }
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx; p.y += p.vy; p.life--;
            if (p.life <= 0) particles.remove(i);
        }
        trails.add(new Trail(p1X - 30, p1Y + 5, new Color(80, 200, 255)));
        trails.add(new Trail(p2X - 30, p2Y + 5, new Color(255, 130, 130)));
        for (int i = trails.size() - 1; i >= 0; i--) {
            Trail t = trails.get(i);
            t.life -= 0.03;
            if (t.life <= 0) trails.remove(i);
        }

        if (cameraShake > 0) cameraShake *= 0.9;

        if (p1Distance >= TRACK_LENGTH && winner == -1) winner = 1;
        if (p2Distance >= TRACK_LENGTH && winner == -1) winner = 2;
        if (winner != -1) state = State.FINISHED;
    }

    private void aiUpdate() {
        aiDecisionTimer--;
        if (aiDecisionTimer <= 0) {
            aiDecisionTimer = 20 + random.nextInt(30);
            aiTargetY = 250 + random.nextInt(HEIGHT - 350);
            for (Obstacle o : obstacles) {
                double dx = o.distance - p2Distance;
                if (dx > 100 && dx < 600 && Math.abs(o.y - p2Y) < 60) {
                    aiTargetY = o.y < p2Y ? o.y + 80 : o.y - 80;
                }
            }
            for (PowerUp p : powerUps) {
                if (p.collected) continue;
                double dx = p.distance - p2Distance;
                if (dx > 100 && dx < 500) { aiTargetY = p.y; break; }
            }
        }
        double diff = aiTargetY - p2Y;
        if (diff < -3) { p2Up = true; p2Down = false; }
        else if (diff > 3) { p2Down = true; p2Up = false; }
        else { p2Up = false; p2Down = false; }

        if (p2BoostFuel > 50 && random.nextInt(200) < 2) p2Boost = true;
        if (p2BoostFuel < 20 || random.nextInt(100) < 2) p2Boost = false;

        double gap = p1Distance - p2Distance;
        if (gap > 800) p2Speed += 0.4;
        else if (gap < -800) p2Speed -= 0.2;

        if (p2Boost && p2BoostFuel > 0 && p2BoostCooldown <= 0) {
            p2Speed += 0.9;
            p2BoostFuel -= 0.8;
            spawnBoostParticles(p2X, p2Y, new Color(255, 80, 80));
        }
    }

    private void spawnBoostParticles(double x, double y, Color c) {
        for (int i = 0; i < 2; i++) {
            particles.add(new Particle(x - 30, y + (random.nextDouble() - 0.5) * 20,
                    -2 - random.nextDouble() * 3, (random.nextDouble() - 0.5) * 2, c, 30));
        }
    }

    private void spawnExplosion(double x, double y, Color c) {
        for (int i = 0; i < 30; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double s = 1 + random.nextDouble() * 6;
            particles.add(new Particle(x, y, Math.cos(a) * s, Math.sin(a) * s, c, 40 + random.nextInt(20)));
        }
    }

    // ============ RENDERING ============

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int shakeX = 0, shakeY = 0;
        if (cameraShake > 0.5) {
            shakeX = (int)((random.nextDouble() - 0.5) * cameraShake);
            shakeY = (int)((random.nextDouble() - 0.5) * cameraShake);
        }
        g2.translate(shakeX, shakeY);

        if (state == State.MENU) { drawMenu(g2); return; }

        drawSky(g2);
        drawAurora(g2);
        drawSun(g2);
        drawCanadianFlagFaint(g2);
        drawMountains(g2, 0.15, 0);
        drawMountains(g2, 0.35, 1);
        drawMountains(g2, 0.55, 2);
        drawClouds(g2);
        drawGeese(g2);
        drawLodges(g2);
        drawTrees(g2);
        drawGround(g2);

        // Power-ups
        for (PowerUp p : powerUps) {
            if (p.collected) continue;
            double sx = p.distance - p1Distance + 400;
            if (sx < -100 || sx > WIDTH + 100) continue;
            drawPowerUp(g2, sx, p.y);
        }

        // Obstacles
        for (Obstacle o : obstacles) {
            double sx = o.distance - p1Distance + 400;
            if (sx < -150 || sx > WIDTH + 150) continue;
            drawObstacle(g2, sx, o.y, o.type);
        }

        // Trails
        for (Trail t : trails) {
            g2.setColor(new Color(t.color.getRed(), t.color.getGreen(), t.color.getBlue(),
                    (int)(t.life * 180)));
            g2.fillOval((int)t.x, (int)t.y, (int)(12 * t.life), (int)(8 * t.life));
        }

        // Hoverboards
        drawHoverboard(g2, p1X, p1Y, p1Tilt, new Color(80, 180, 255), new Color(0, 80, 200), 1);
        drawHoverboard(g2, p2X, p2Y, p2Tilt, new Color(255, 80, 80), new Color(180, 20, 20), 2);

        // Particles
        for (Particle p : particles) {
            int alpha = Math.min(255, p.life * 8);
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alpha));
            g2.fillOval((int)p.x, (int)p.y, 5, 5);
        }

        // Falling maple leaves & snow overlay
        drawMapleLeaves(g2);
        drawSnow(g2);

        drawHUD(g2);
        if (state == State.FINISHED) drawFinish(g2);
    }

    private void drawSky(Graphics2D g2) {
        // Canadian northern sky - deep blue to red twilight
        GradientPaint sky = new GradientPaint((float) 0, (float) 0, new Color(15, 25, 75),
                (float) 0, (float) (HEIGHT * 0.7), new Color(180, 80, 110));
        g2.setPaint(sky);
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        // Warm horizon with subtle red maple tint
        GradientPaint horizon = new GradientPaint(0, HEIGHT * 0.6f, new Color(255, 140, 130, 0),
                0, HEIGHT * 0.75f, new Color(255, 130, 120, 160));
        g2.setPaint(horizon);
        g2.fillRect(0, (int)(HEIGHT * 0.6), WIDTH, (int)(HEIGHT * 0.15));
    }

    private void drawAurora(Graphics2D g2) {
        // Northern Lights (Canada's iconic aurora borealis)
        for (int layer = 0; layer < 4; layer++) {
            Color c;
            switch (layer) {
                case 0: c = new Color(120, 255, 180, 55); break;
                case 1: c = new Color(100, 255, 220, 50); break;
                case 2: c = new Color(180, 130, 255, 45); break;
                default: c = new Color(80, 200, 255, 40);
            }
            g2.setColor(c);
            Path2D path = new Path2D.Double();
            int baseY = 70 + layer * 50;
            path.moveTo(0, baseY);
            for (int x = 0; x <= WIDTH; x += 15) {
                double y = baseY + Math.sin((x + auroraTime * 120 + layer * 200) * 0.008) * 45
                        + Math.cos((x + auroraTime * 80) * 0.012) * 25;
                path.lineTo(x, y);
            }
            for (int x = WIDTH; x >= 0; x -= 15) {
                double y = baseY + 45 + Math.sin((x + auroraTime * 120 + layer * 200) * 0.008) * 45
                        + Math.cos((x + auroraTime * 80) * 0.012) * 25;
                path.lineTo(x, y);
            }
            path.closePath();
            g2.fill(path);
        }
    }

    private void drawSun(Graphics2D g2) {
        int sunX = 200;
        int sunY = 180;
        double pulse = 1 + Math.sin(sunPulse) * 0.06;

        // Glow
        for (int i = 8; i > 0; i--) {
            g2.setColor(new Color(255, 220, 200, 10));
            int r = (int)((50 + i * 15) * pulse);
            g2.fillOval(sunX - r, sunY - r, r * 2, r * 2);
        }
        // Body - warm Canadian sunset
        RadialGradientPaint sunGrad = new RadialGradientPaint(
                new Point(sunX, sunY), (float)(60 * pulse),
                new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(255, 255, 220), new Color(255, 180, 120), new Color(255, 100, 100)});
        g2.setPaint(sunGrad);
        int r = (int)(60 * pulse);
        g2.fillOval(sunX - r, sunY - r, r * 2, r * 2);
    }

    private void drawCanadianFlagFaint(Graphics2D g2) {
        // A subtle stylized Canadian flag suggestion - the maple leaf shape
        // at the top center of the sky, adding national flavor
        AffineTransform old = g2.getTransform();
        g2.translate(WIDTH / 2, 100);
        g2.setColor(new Color(255, 255, 255, 25));
        // Simple maple leaf silhouette
        Path2D leaf = new Path2D.Double();
        int s = 35;
        leaf.moveTo(0, -s);
        leaf.lineTo(6, -s * 0.35);
        leaf.lineTo(s * 0.55, -s * 0.35);
        leaf.lineTo(s * 0.25, 0);
        leaf.lineTo(s * 0.45, s * 0.15);
        leaf.lineTo(s * 0.15, s * 0.4);
        leaf.lineTo(s * 0.3, s * 0.7);
        leaf.lineTo(0, s * 0.45);
        leaf.lineTo(-s * 0.3, s * 0.7);
        leaf.lineTo(-s * 0.15, s * 0.4);
        leaf.lineTo(-s * 0.45, s * 0.15);
        leaf.lineTo(-s * 0.25, 0);
        leaf.lineTo(-s * 0.55, -s * 0.35);
        leaf.lineTo(-6, -s * 0.35);
        leaf.closePath();
        g2.fill(leaf);
        // red tint over it
        g2.setColor(new Color(255, 40, 40, 15));
        g2.fill(leaf);
        g2.setTransform(old);
    }

    private void drawMountains(Graphics2D g2, double parallax, int layer) {
        int offset = (int)(worldOffset * parallax) % WIDTH;
        Color[] colors = {
                new Color(85, 65, 140),     // far - purple
                new Color(60, 70, 150),     // mid - blue
                new Color(35, 50, 110)      // near - deep blue
        };
        Color snow = new Color(255, 255, 255, 230);
        Color snowShade = new Color(200, 220, 255, 200);

        g2.setColor(colors[layer]);
        for (int repeat = -1; repeat <= 1; repeat++) {
            for (int i = 0; i < 6; i++) {
                int baseX = i * 220 - offset + repeat * WIDTH - 200;
                int peakY = 200 + layer * 40 + (i % 3) * 40;
                int baseY = HEIGHT;
                int width = 260 + (i % 2) * 80;

                Path2D mountain = new Path2D.Double();
                mountain.moveTo(baseX, baseY);
                mountain.lineTo(baseX + width / 2, peakY);
                mountain.lineTo(baseX + width, baseY);
                mountain.closePath();
                g2.setColor(colors[layer]);
                g2.fill(mountain);

                // Snowcap (Canadian Rockies are famous for snowcaps)
                double snowLine = peakY + (baseY - peakY) * 0.3;
                Path2D snowCap = new Path2D.Double();
                snowCap.moveTo(baseX + width / 2, peakY);
                double t = 0.3;
                snowCap.lineTo(baseX + width * t, snowLine);
                for (int z = 0; z < 5; z++) {
                    double lx = baseX + width * t + z * (width * (0.5 - t) / 5);
                    double ly = snowLine + (z % 2 == 0 ? 14 : -8);
                    snowCap.lineTo(lx, ly);
                }
                snowCap.lineTo(baseX + width / 2 + 2, peakY + 5);
                for (int z = 4; z >= 0; z--) {
                    double lx = baseX + width / 2 - z * (width * (0.5 - t) / 5);
                    double ly = snowLine + (z % 2 == 0 ? 14 : -8);
                    snowCap.lineTo(lx, ly);
                }
                snowCap.lineTo(baseX + width * (1 - t), snowLine);
                snowCap.closePath();
                g2.setColor(snow);
                g2.fill(snowCap);

                // Sun-lit side
                Path2D shade = new Path2D.Double();
                shade.moveTo(baseX + width / 2, peakY);
                shade.lineTo(baseX + width, baseY);
                shade.lineTo(baseX + width / 2, baseY);
                shade.closePath();
                g2.setColor(new Color(0, 0, 40, 70));
                g2.fill(shade);

                // Snow on shaded side
                Path2D snowShadeP = new Path2D.Double();
                snowShadeP.moveTo(baseX + width / 2, peakY);
                snowShadeP.lineTo(baseX + width * 0.72, snowLine + 5);
                snowShadeP.lineTo(baseX + width / 2, snowLine - 5);
                snowShadeP.closePath();
                g2.setColor(snowShade);
                g2.fill(snowShadeP);
            }
        }
    }

    private void drawClouds(Graphics2D g2) {
        for (Cloud c : clouds) {
            g2.setColor(new Color(255, 220, 240, 100));
            g2.fillOval((int)c.x, (int)c.y, (int) c.w, (int) (c.w * 2 / 3));
            g2.fillOval((int)(c.x + c.w * 0.3), (int)(c.y - c.w / 4), (int)(c.w * 0.7), (int)(c.w * 0.7));
            g2.fillOval((int)(c.x + c.w * 0.6), (int)c.y, (int)(c.w * 0.7), (int)(c.w * 0.55));
            g2.setColor(new Color(255, 255, 255, 60));
            g2.fillOval((int)(c.x + c.w * 0.2), (int)(c.y - c.w / 6), (int)(c.w * 0.4), (int)(c.w * 0.35));
        }
    }

    private void drawGeese(Graphics2D g2) {
        // Canadian geese in V formation
        for (CanadianGoose g : geese) {
            g2.setColor(new Color(20, 20, 20, 200));
            // body
            g2.fillOval((int)g.x, (int)g.y, 22, 8);
            // head/neck
            g2.drawLine((int)g.x + 20, (int)g.y + 3, (int)g.x + 28, (int)g.y - 3);
            g2.fillOval((int)g.x + 26, (int)g.y - 7, 8, 6);
            // wings flapping
            double flap = Math.sin(waveTime * 8 + g.x * 0.1) * 5;
            g2.setColor(new Color(30, 30, 30, 200));
            g2.fillOval((int)g.x + 6, (int)(g.y + 1 - flap), 14, 4);
            g2.setColor(new Color(200, 200, 200, 220));
            // white cheek patch
            g2.fillOval((int)g.x + 29, (int)g.y - 6, 4, 3);
        }
    }

    private void drawLodges(Graphics2D g2) {
        int offset = (int)(worldOffset * 0.35) % WIDTH;
        for (LodgeLog l : lodges) {
            for (int rep = -1; rep <= 1; rep++) {
                int x = (int)(l.baseX - offset + rep * (WIDTH + 200));
                if (x < -100 || x > WIDTH + 100) continue;
                int y = HEIGHT - 110;
                // Wooden cabin body
                g2.setColor(new Color(120, 70, 40));
                g2.fillRect(x, y, 60, 30);
                // log lines
                g2.setColor(new Color(90, 50, 25));
                for (int i = 0; i < 4; i++) g2.drawLine(x, y + i * 7 + 4, x + 60, y + i * 7 + 4);
                // Roof
                g2.setColor(new Color(80, 40, 20));
                Path2D roof = new Path2D.Double();
                roof.moveTo(x - 8, y);
                roof.lineTo(x + 30, y - 22);
                roof.lineTo(x + 68, y);
                roof.closePath();
                g2.fill(roof);
                // Snow on roof
                g2.setColor(new Color(240, 245, 255, 220));
                Path2D snowRoof = new Path2D.Double();
                snowRoof.moveTo(x - 8, y);
                snowRoof.lineTo(x + 30, y - 22);
                snowRoof.lineTo(x + 68, y);
                snowRoof.lineTo(x + 60, y - 2);
                snowRoof.lineTo(x + 30, y - 20);
                snowRoof.lineTo(x, y - 2);
                snowRoof.closePath();
                g2.fill(snowRoof);
                // Warm window glow
                g2.setColor(new Color(255, 220, 100, 220));
                g2.fillRect(x + 12, y + 10, 8, 8);
                g2.fillRect(x + 40, y + 10, 8, 8);
                // Window glow bloom
                g2.setColor(new Color(255, 200, 80, 50));
                g2.fillOval(x + 6, y + 5, 20, 20);
                g2.fillOval(x + 34, y + 5, 20, 20);
            }
        }
    }

    private void drawTrees(Graphics2D g2) {
        int offset = (int)(worldOffset * 0.7) % WIDTH;
        for (Tree t : trees) {
            for (int rep = -1; rep <= 1; rep++) {
                int x = (int)(t.baseX - offset + rep * WIDTH);
                if (x < -50 || x > WIDTH + 50) continue;
                int baseY = HEIGHT - 40;
                int h = t.size * 25;

                if (t.type == 2) {
                    // Maple tree (Canada's iconic tree)
                    g2.setColor(new Color(90, 50, 25));
                    g2.fillRect(x - 3, baseY - h, 6, h);
                    // canopy of red/orange maple foliage
                    g2.setColor(new Color(220, 50, 30));
                    g2.fillOval(x - 28, baseY - h - 30, 56, 40);
                    g2.setColor(new Color(240, 90, 40));
                    g2.fillOval(x - 24, baseY - h - 36, 48, 30);
                    g2.setColor(new Color(255, 160, 60));
                    g2.fillOval(x - 18, baseY - h - 40, 36, 20);
                    // snow hint
                    g2.setColor(new Color(255, 255, 255, 130));
                    g2.fillOval(x - 20, baseY - h - 36, 40, 12);
                } else {
                    // Pine
                    g2.setColor(new Color(70, 40, 20));
                    g2.fillRect(x - 3, baseY - h, 6, h);
                    for (int l = 0; l < 3; l++) {
                        int layerY = baseY - h - l * 15 + 20;
                        int layerW = 30 - l * 6;
                        int layerH = 30;
                        Path2D pine = new Path2D.Double();
                        pine.moveTo(x, layerY - layerH);
                        pine.lineTo(x - layerW, layerY);
                        pine.lineTo(x + layerW, layerY);
                        pine.closePath();
                        g2.setColor(new Color(20, 80 + l * 15, 50));
                        g2.fill(pine);
                        // Snow on top
                        g2.setColor(new Color(255, 255, 255, 200));
                        Path2D snowTop = new Path2D.Double();
                        snowTop.moveTo(x, layerY - layerH);
                        snowTop.lineTo(x - layerW * 0.4, layerY - layerH * 0.55);
                        snowTop.lineTo(x + layerW * 0.4, layerY - layerH * 0.55);
                        snowTop.closePath();
                        g2.fill(snowTop);
                    }
                }
            }
        }
    }

    private void drawGround(Graphics2D g2) {
        int groundTop = HEIGHT - 60;
        // Snowy Canadian ground with hint of red (flag/leaf)
        GradientPaint ground = new GradientPaint(0, groundTop, new Color(235, 240, 255),
                0, HEIGHT, new Color(180, 195, 225));
        g2.setPaint(ground);
        g2.fillRect(0, groundTop, WIDTH, HEIGHT - groundTop);

        g2.setColor(new Color(255, 255, 255, 170));
        for (int i = 0; i < 20; i++) {
            int x = (int)((i * 70 - worldOffset * 0.9) % (WIDTH + 70));
            if (x < 0) x += WIDTH + 70;
            g2.fillRect(x, groundTop + 10 + (i % 3) * 15, 25, 2);
        }

        // Track edge glow
        g2.setColor(new Color(255, 80, 80, 100));
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(0, groundTop + 5, WIDTH, groundTop + 5);
        g2.setColor(new Color(255, 255, 255, 100));
        g2.drawLine(0, groundTop + 9, WIDTH, groundTop + 9);
    }

    private void drawSnow(Graphics2D g2) {
        for (Snowflake s : snowflakes) {
            g2.setColor(new Color(255, 255, 255, (int)(150 + Math.sin(waveTime + s.y * 0.1) * 60)));
            g2.fillOval((int)s.x, (int)s.y, (int) s.size, (int) s.size);
        }
    }

    private void drawMapleLeaves(Graphics2D g2) {
        for (MapleLeaf m : mapleLeaves) {
            AffineTransform old = g2.getTransform();
            g2.translate(m.x, m.y);
            g2.rotate(m.rotation);
            g2.setColor(new Color(220 + (int)(Math.sin(m.rotation) * 20),
                    60 + (int)(Math.cos(m.rotation) * 30), 40, 200));
            // Small maple leaf shape
            Path2D leaf = new Path2D.Double();
            int s = 8;
            leaf.moveTo(0, -s);
            leaf.lineTo(2, -s * 0.3);
            leaf.lineTo(s * 0.5, -s * 0.3);
            leaf.lineTo(s * 0.2, 0);
            leaf.lineTo(s * 0.4, s * 0.2);
            leaf.lineTo(s * 0.15, s * 0.4);
            leaf.lineTo(0, s * 0.9);
            leaf.lineTo(-s * 0.15, s * 0.4);
            leaf.lineTo(-s * 0.4, s * 0.2);
            leaf.lineTo(-s * 0.2, 0);
            leaf.lineTo(-s * 0.5, -s * 0.3);
            leaf.lineTo(-2, -s * 0.3);
            leaf.closePath();
            g2.fill(leaf);
            g2.setTransform(old);
        }
    }

    private void drawHoverboard(Graphics2D g2, double x, double y, double tilt, Color glow, Color body, int num) {
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(tilt);

        for (int i = 4; i > 0; i--) {
            g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 30));
            g2.fillOval(-60 - i * 5, 10 - i * 2, 120 + i * 10, 25 + i * 4);
        }
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillOval(-45, 12, 90, 18);
        GradientPaint boardGrad = new GradientPaint(0, -15, glow, 0, 15, body);
        g2.setPaint(boardGrad);
        RoundRectangle2D board = new RoundRectangle2D.Double(-45, -10, 90, 22, 20, 20);
        g2.fill(board);
        g2.setColor(new Color(255, 255, 255, 130));
        g2.fillRoundRect(-40, -8, 80, 6, 10, 10);
        g2.setStroke(new BasicStroke(2));
        g2.setColor(glow);
        g2.draw(board);

        // Tiny maple leaf decal on the board
        g2.setColor(new Color(255, 50, 50, 220));
        Path2D miniLeaf = new Path2D.Double();
        int s = 5;
        miniLeaf.moveTo(30, -s);
        miniLeaf.lineTo(32, -s * 0.3);
        miniLeaf.lineTo(30 + s * 0.5, -s * 0.3);
        miniLeaf.lineTo(30 + s * 0.2, 0);
        miniLeaf.lineTo(30 + s * 0.4, s * 0.2);
        miniLeaf.lineTo(30, s * 0.9);
        miniLeaf.lineTo(30 - s * 0.4, s * 0.2);
        miniLeaf.lineTo(30 - s * 0.2, 0);
        miniLeaf.lineTo(30 - s * 0.5, -s * 0.3);
        miniLeaf.lineTo(32 - s * 0.5, -s * 0.3);
        miniLeaf.closePath();
        g2.fill(miniLeaf);

        // Rider
        g2.setColor(new Color(40, 40, 60));
        g2.fillOval(-8, -42, 16, 16);
        g2.setColor(num == 1 ? new Color(100, 200, 255) : new Color(255, 100, 100));
        g2.fillOval(-9, -43, 18, 18);
        g2.setColor(new Color(30, 30, 50));
        g2.fillRect(-6, -26, 12, 18);
        g2.setStroke(new BasicStroke(4));
        g2.setColor(new Color(30, 30, 50));
        g2.drawLine(-6, -20, -18, -28);
        g2.drawLine(6, -20, 18, -28);
        g2.drawLine(-3, -8, -8, 0);
        g2.drawLine(3, -8, 8, 0);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("P" + num, -5, -48);

        g2.setTransform(old);
    }

    private void drawObstacle(Graphics2D g2, double x, double y, int type) {
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);

        if (type == 0) {
            // Ice crystal (Canadian glacier shard)
            int[] xs = {0, -30, 30};
            int[] ys = {-30, 30, 30};
            g2.setColor(new Color(150, 220, 255, 130));
            g2.fillPolygon(xs, ys, 3);
            g2.setColor(new Color(200, 240, 255));
            g2.setStroke(new BasicStroke(3));
            g2.drawPolygon(xs, ys, 3);
            g2.setColor(new Color(255, 255, 255, 200));
            g2.drawLine(0, -30, 0, 30);
            g2.drawLine(-12, -5, 12, -5);
        } else if (type == 1) {
            // Snowball/rock
            for (int i = 5; i > 0; i--) {
                g2.setColor(new Color(180, 210, 255, 30));
                g2.fillOval(-i * 8 - 5, -i * 8 - 5, (i * 8 + 5) * 2, (i * 8 + 5) * 2);
            }
            g2.setColor(new Color(240, 245, 255));
            g2.fillOval(-18, -18, 36, 36);
            g2.setColor(new Color(120, 160, 220));
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(-18, -18, 36, 36);
            g2.setColor(Color.WHITE);
            g2.fillOval(-10, -12, 8, 6);
        } else if (type == 2) {
            // Pine hazard
            g2.setColor(new Color(20, 100, 60, 220));
            Path2D p = new Path2D.Double();
            p.moveTo(0, -35);
            p.lineTo(25, 0);
            p.lineTo(0, 35);
            p.lineTo(-25, 0);
            p.closePath();
            g2.fill(p);
            g2.setColor(new Color(120, 255, 180));
            g2.setStroke(new BasicStroke(3));
            g2.draw(p);
            g2.setColor(new Color(255, 255, 255, 220));
            Path2D snow = new Path2D.Double();
            snow.moveTo(0, -35);
            snow.lineTo(10, -12);
            snow.lineTo(-10, -12);
            snow.closePath();
            g2.fill(snow);
        } else {
            // Canadian hockey puck / maple obstacle
            g2.setColor(new Color(40, 40, 50, 220));
            g2.fillOval(-22, -22, 44, 44);
            g2.setColor(new Color(255, 60, 60));
            g2.setStroke(new BasicStroke(3));
            g2.drawOval(-22, -22, 44, 44);
            // Maple leaf on it
            g2.setColor(new Color(255, 50, 50));
            Path2D leaf = new Path2D.Double();
            int s = 14;
            leaf.moveTo(0, -s);
            leaf.lineTo(2, -s * 0.3);
            leaf.lineTo(s * 0.5, -s * 0.3);
            leaf.lineTo(s * 0.2, 0);
            leaf.lineTo(s * 0.4, s * 0.2);
            leaf.lineTo(s * 0.15, s * 0.4);
            leaf.lineTo(0, s * 0.9);
            leaf.lineTo(-s * 0.15, s * 0.4);
            leaf.lineTo(-s * 0.4, s * 0.2);
            leaf.lineTo(-s * 0.2, 0);
            leaf.lineTo(-s * 0.5, -s * 0.3);
            leaf.lineTo(-2, -s * 0.3);
            leaf.closePath();
            g2.fill(leaf);
        }
        g2.setTransform(old);
    }

    private void drawPowerUp(Graphics2D g2, double x, double y) {
        double pulse = 1 + Math.sin(waveTime * 4) * 0.15;
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.scale(pulse, pulse);

        // Red glowing maple leaf power-up (Canadian boost)
        for (int i = 6; i > 0; i--) {
            g2.setColor(new Color(255, 60, 60, 25));
            g2.fillOval(-i * 5, -i * 5, i * 10, i * 10);
        }
        // Rotating glow
        g2.rotate(waveTime * 0.5);
        g2.setColor(new Color(255, 255, 255, 180));
        g2.setStroke(new BasicStroke(2));
        // Outer ring
        g2.drawOval(-20, -20, 40, 40);
        // Maple leaf
        g2.setColor(new Color(255, 50, 50));
        Path2D leaf = new Path2D.Double();
        int s = 18;
        leaf.moveTo(0, -s);
        leaf.lineTo(3, -s * 0.35);
        leaf.lineTo(s * 0.6, -s * 0.35);
        leaf.lineTo(s * 0.25, 0);
        leaf.lineTo(s * 0.5, s * 0.2);
        leaf.lineTo(s * 0.2, s * 0.45);
        leaf.lineTo(0, s * 0.95);
        leaf.lineTo(-s * 0.2, s * 0.45);
        leaf.lineTo(-s * 0.5, s * 0.2);
        leaf.lineTo(-s * 0.25, 0);
        leaf.lineTo(-s * 0.6, -s * 0.35);
        leaf.lineTo(-3, -s * 0.35);
        leaf.closePath();
        g2.fill(leaf);
        // Highlight
        g2.setColor(new Color(255, 200, 200, 200));
        g2.fillOval(-4, -6, 3, 3);

        g2.setTransform(old);
    }

    private void drawHUD(Graphics2D g2) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));

        g2.setColor(Color.WHITE);
        g2.drawString("P1", 30, 30);
        drawProgressBar(g2, 60, 20, 300, 16, p1Distance / TRACK_LENGTH, new Color(80, 180, 255));
        drawBoostBar(g2, 60, 44, 200, 8, p1BoostFuel / 100.0, new Color(0, 200, 255));

        g2.drawString(mode == Mode.VS_COMPUTER ? "CPU" : "P2", 30, 80);
        drawProgressBar(g2, 60, 70, 300, 16, p2Distance / TRACK_LENGTH, new Color(255, 100, 100));
        drawBoostBar(g2, 60, 94, 200, 8, p2BoostFuel / 100.0, new Color(255, 100, 100));

        g2.setColor(new Color(255, 255, 255, 200));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString(String.format("Distance: %.0f / %d", p1Distance, TRACK_LENGTH), WIDTH - 220, 30);

        // Small flag HUD icon in top-right
        drawMiniCanadianFlag(g2, WIDTH - 60, 50);
    }

    private void drawMiniCanadianFlag(Graphics2D g2, int x, int y) {
        // Tiny Canadian flag: white with red bands and maple leaf
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillRect(x, y, 30, 20);
        g2.setColor(new Color(255, 40, 40));
        g2.fillRect(x, y, 8, 20);
        g2.fillRect(x + 22, y, 8, 20);
        // Tiny leaf center
        g2.setColor(new Color(255, 40, 40));
        Path2D leaf = new Path2D.Double();
        int cx = x + 15, cy = y + 10;
        int s = 6;
        leaf.moveTo(cx, cy - s);
        leaf.lineTo(cx + 1, cy - s * 0.3);
        leaf.lineTo(cx + s * 0.5, cy - s * 0.3);
        leaf.lineTo(cx + s * 0.2, cy);
        leaf.lineTo(cx + s * 0.4, cy + s * 0.3);
        leaf.lineTo(cx, cy + s * 0.9);
        leaf.lineTo(cx - s * 0.4, cy + s * 0.3);
        leaf.lineTo(cx - s * 0.2, cy);
        leaf.lineTo(cx - s * 0.5, cy - s * 0.3);
        leaf.lineTo(cx - 1, cy - s * 0.3);
        leaf.closePath();
        g2.fill(leaf);
        g2.setColor(new Color(100, 100, 100));
        g2.drawRect(x, y, 30, 20);
    }

    private void drawProgressBar(Graphics2D g2, int x, int y, int w, int h, double progress, Color color) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(x, y, w, h, h, h);
        g2.setColor(color);
        g2.fillRoundRect(x, y, (int)(w * Math.min(1, progress)), h, h, h);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, w, h, h, h);
    }

    private void drawBoostBar(Graphics2D g2, int x, int y, int w, int h, double progress, Color color) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(x, y, w, h);
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 200));
        g2.fillRect(x, y, (int)(w * Math.min(1, progress)), h);
    }

    private void drawMenu(Graphics2D g2) {
        drawSky(g2);
        drawAurora(g2);
        drawSun(g2);
        drawCanadianFlagFaint(g2);
        drawMountains(g2, 0.15, 0);
        drawMountains(g2, 0.35, 1);
        drawClouds(g2);
        drawGeese(g2);
        drawTrees(g2);
        drawSnow(g2);
        drawMapleLeaves(g2);

        // Title
        g2.setFont(new Font("SansSerif", Font.BOLD, 52));
        String title = "HOVERBOARD";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (WIDTH - fm.stringWidth(title)) / 2;
        GradientPaint titleGrad = new GradientPaint(0, 100, new Color(255, 80, 80),
                WIDTH, 160, new Color(255, 255, 255));
        g2.setPaint(titleGrad);
        g2.drawString(title, tx, 180);

        g2.setFont(new Font("SansSerif", Font.BOLD, 34));
        String title2 = "CANADA MOUNTAIN RACING";
        fm = g2.getFontMetrics();
        tx = (WIDTH - fm.stringWidth(title2)) / 2;
        GradientPaint t2Grad = new GradientPaint(0, 210, new Color(255, 255, 255),
                WIDTH, 240, new Color(255, 60, 60));
        g2.setPaint(t2Grad);
        g2.drawString(title2, tx, 235);

        // Options
        g2.setFont(new Font("SansSerif", Font.BOLD, 22));
        String[] options = {
                "1  -  Race vs Computer",
                "2  -  Race vs Player 2",
                "",
                "Controls:",
                "P1:  W A S D  +  SPACE to boost",
                "P2:  ARROW KEYS  +  SHIFT to boost",
                "",
                "Collect maple leaf power-ups to boost!",
                "Avoid ice crystals, snowballs, pines & pucks!"
        };
        Color[] colors = {
                new Color(150, 220, 255), new Color(255, 180, 180), Color.WHITE, Color.WHITE,
                new Color(150, 220, 255), new Color(255, 180, 180), Color.WHITE,
                new Color(255, 100, 100), new Color(255, 220, 180)
        };
        for (int i = 0; i < options.length; i++) {
            fm = g2.getFontMetrics();
            int w = fm.stringWidth(options[i]);
            g2.setColor(colors[i]);
            g2.drawString(options[i], (WIDTH - w) / 2, 320 + i * 34);
        }

        // Decorative hoverboards
        drawHoverboard(g2, 150, HEIGHT - 100, Math.sin(waveTime) * 0.2,
                new Color(80, 180, 255), new Color(0, 80, 200), 1);
        drawHoverboard(g2, WIDTH - 150, HEIGHT - 100, -Math.sin(waveTime) * 0.2,
                new Color(255, 100, 100), new Color(200, 30, 30), 2);
    }

    private void drawFinish(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setFont(new Font("SansSerif", Font.BOLD, 60));
        String msg;
        Color c;
        if (winner == 1) {
            msg = "PLAYER 1 WINS!";
            c = new Color(120, 220, 255);
        } else {
            msg = mode == Mode.VS_COMPUTER ? "COMPUTER WINS!" : "PLAYER 2 WINS!";
            c = new Color(255, 140, 140);
        }
        FontMetrics fm = g2.getFontMetrics();
        int tx = (WIDTH - fm.stringWidth(msg)) / 2;

        for (int i = 6; i > 0; i--) {
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 25));
            g2.drawString(msg, tx - i, HEIGHT / 2 - 40 + i);
            g2.drawString(msg, tx + i, HEIGHT / 2 - 40 + i);
        }
        g2.setColor(c);
        g2.drawString(msg, tx, HEIGHT / 2 - 40);

        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2.setColor(Color.WHITE);
        String restart = "Press ENTER to play again, ESC for menu";
        fm = g2.getFontMetrics();
        tx = (WIDTH - fm.stringWidth(restart)) / 2;
        g2.drawString(restart, tx, HEIGHT / 2 + 60);

        // Little Canada flag
        drawMiniCanadianFlag(g2, WIDTH / 2 - 20, HEIGHT / 2 + 100);
    }

    // ============ INPUT ============

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (state == State.MENU) {
            if (k == KeyEvent.VK_1) { mode = Mode.VS_COMPUTER; reset(); state = State.RACING; }
            else if (k == KeyEvent.VK_2) { mode = Mode.VS_PLAYER; reset(); state = State.RACING; }
            return;
        }
        if (state == State.FINISHED) {
            if (k == KeyEvent.VK_ENTER) { reset(); state = State.RACING; }
            else if (k == KeyEvent.VK_ESCAPE) { state = State.MENU; }
            return;
        }
        switch (k) {
            case KeyEvent.VK_W: p1Up = true; break;
            case KeyEvent.VK_S: p1Down = true; break;
            case KeyEvent.VK_A: p1Left = true; break;
            case KeyEvent.VK_D: p1Right = true; break;
            case KeyEvent.VK_SPACE: p1Boost = true; break;
            case KeyEvent.VK_UP: p2Up = true; break;
            case KeyEvent.VK_DOWN: p2Down = true; break;
            case KeyEvent.VK_LEFT: p2Left = true; break;
            case KeyEvent.VK_RIGHT: p2Right = true; break;
            case KeyEvent.VK_SHIFT: p2Boost = true; break;
            case KeyEvent.VK_ESCAPE: state = State.MENU; break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        switch (k) {
            case KeyEvent.VK_W: p1Up = false; break;
            case KeyEvent.VK_S: p1Down = false; break;
            case KeyEvent.VK_A: p1Left = false; break;
            case KeyEvent.VK_D: p1Right = false; break;
            case KeyEvent.VK_SPACE: p1Boost = false; break;
            case KeyEvent.VK_UP: p2Up = false; break;
            case KeyEvent.VK_DOWN: p2Down = false; break;
            case KeyEvent.VK_LEFT: p2Left = false; break;
            case KeyEvent.VK_RIGHT: p2Right = false; break;
            case KeyEvent.VK_SHIFT: p2Boost = false; break;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}

    // ============ INNER CLASSES ============

    static class Obstacle {
        double distance, y; int type; int size = 60;
        boolean hit1 = false, hit2 = false;
        Obstacle(double d, double y, int t) { distance = d; this.y = y; type = t; }
    }
    static class PowerUp {
        double distance, y; boolean collected = false;
        PowerUp(double d, double y) { distance = d; this.y = y; }
    }
    static class Particle {
        double x, y, vx, vy; Color color; int life;
        Particle(double x, double y, double vx, double vy, Color c, int life) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; color = c; this.life = life;
        }
    }
    static class Trail {
        double x, y, life = 1; Color color;
        Trail(double x, double y, Color c) { this.x = x; this.y = y; color = c; }
    }
    static class Snowflake {
        double x, y, speed, size, drift;
        Snowflake(double x, double y, double speed, double size) {
            this.x = x; this.y = y; this.speed = speed; this.size = size;
            this.drift = (Math.random() - 0.5) * 0.4;
        }
    }
    static class Cloud {
        double x, y, w, speed;
        Cloud(double x, double y, double w, double s) { this.x = x; this.y = y; this.w = w; speed = s; }
    }
    static class Tree {
        double baseX; int size, type;
        Tree(double x, int s, int t) { baseX = x; size = s; type = t; }
    }
    static class MapleLeaf {
        double x, y, rotation, speed;
        MapleLeaf(double x, double y, double r, double s) {
            this.x = x; this.y = y; rotation = r; speed = s;
        }
    }
    static class CanadianGoose {
        double x, y, speed;
        CanadianGoose(double x, double y, double s) { this.x = x; this.y = y; speed = s; }
    }
    static class LodgeLog {
        double baseX, parallax;
        LodgeLog(double x, double p) { baseX = x; parallax = p; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hoverboard Canada Mountain Racing Adventure 🍁");
            HoverboardCanadaRacing game = new HoverboardCanadaRacing();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}