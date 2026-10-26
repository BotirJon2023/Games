import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class HoverboardMachuPicchuRacing extends JPanel implements ActionListener, KeyListener {
    // Screen settings
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final int TRACK_LENGTH = 12000;

    // Game states
    private enum State { MENU, RACING, FINISHED }
    private State state = State.MENU;
    private enum Mode { VS_COMPUTER, VS_PLAYER }
    private Mode mode = Mode.VS_COMPUTER;

    // Player 1 (turquoise hoverboard - Incan gold accents)
    private double p1X = 200;
    private double p1Y = HEIGHT - 200;
    private double p1Speed = 0;
    private double p1Distance = 0;
    private double p1Tilt = 0;
    private boolean p1Up, p1Down, p1Left, p1Right, p1Boost;
    private double p1BoostFuel = 100;
    private double p1BoostCooldown = 0;

    // Player 2 / AI (gold/red hoverboard - Incan sun)
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
    private ArrayList<Firefly> fireflies = new ArrayList<>();
    private ArrayList<Cloud> clouds = new ArrayList<>();
    private ArrayList<Tree> trees = new ArrayList<>();
    private ArrayList<TerracedField> terraces = new ArrayList<>();
    private ArrayList<Llamas> llamas = new ArrayList<>();
    private ArrayList<Condor> condors = new ArrayList<>();
    private ArrayList<IncaTemple> temples = new ArrayList<>();
    private ArrayList<IntiRay> sunRays = new ArrayList<>();
    private ArrayList<FloatingFeather> feathers = new ArrayList<>();

    private double cameraShake = 0;
    private double worldOffset = 0;
    private double waveTime = 0;
    private double sunPulse = 0;
    private double condorWing = 0;

    // Game loop
    private Timer timer;
    private int winner = -1;

    public HoverboardMachuPicchuRacing() {
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

        // Terraced fields (Andean agricultural terraces)
        for (int i = 0; i < 25; i++) {
            terraces.add(new TerracedField(i * 180 + r.nextInt(80), 0.4, 3 + r.nextInt(4)));
        }
        // Trees (Andean trees - Polylepis / Queñua)
        for (int i = 0; i < 40; i++) {
            trees.add(new Tree(i * 110 + r.nextInt(70), 3 + r.nextInt(3)));
        }
        // Clouds
        for (int i = 0; i < 12; i++) {
            clouds.add(new Cloud(r.nextInt(WIDTH), 40 + r.nextInt(220),
                    60 + r.nextInt(90), 0.2 + r.nextDouble() * 0.6));
        }
        // Fireflies (fireflies/glowing spirits)
        for (int i = 0; i < 40; i++) {
            fireflies.add(new Firefly(r.nextInt(WIDTH), r.nextInt(HEIGHT),
                    r.nextDouble() * Math.PI * 2, 0.5 + r.nextDouble()));
        }
        // Llamas grazing on terraces
        for (int i = 0; i < 6; i++) {
            llamas.add(new Llamas(200 + i * 250 + r.nextInt(100), HEIGHT - 130, 0.5 + r.nextDouble() * 0.3));
        }
        // Flying condors (Andean condor)
        for (int i = 0; i < 3; i++) {
            condors.add(new Condor(-200 - r.nextInt(800), 120 + r.nextInt(180), 0.7 + r.nextDouble() * 0.5));
        }
        // Distant Inca temples
        for (int i = 0; i < 4; i++) {
            temples.add(new IncaTemple(i * 500 + r.nextInt(300), 0.3));
        }
        // Sun rays (Inti - the sun god)
        for (int i = 0; i < 8; i++) {
            sunRays.add(new IntiRay(i * 0.4));
        }
        // Floating feathers (condor feathers)
        for (int i = 0; i < 25; i++) {
            feathers.add(new FloatingFeather(r.nextInt(WIDTH), r.nextInt(HEIGHT),
                    r.nextDouble() * Math.PI * 2, 0.6 + r.nextDouble() * 0.7));
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
        sunPulse += 0.02;
        condorWing += 0.15;
        updateEnvironment();
        repaint();
    }

    private void updateEnvironment() {
        for (Firefly f : fireflies) {
            f.x += Math.cos(f.phase) * f.speed;
            f.y += Math.sin(f.phase * 1.3) * f.speed * 0.5;
            f.phase += 0.05;
            if (f.x < 0) f.x = WIDTH;
            if (f.x > WIDTH) f.x = 0;
            if (f.y < 0) f.y = HEIGHT;
            if (f.y > HEIGHT) f.y = 0;
        }
        for (Cloud c : clouds) {
            c.x -= c.speed;
            if (c.x < -c.w - 20) { c.x = WIDTH + 20; c.y = 40 + random.nextInt(220); }
        }
        for (Condor c : condors) {
            c.x += c.speed;
            c.y += Math.sin(waveTime + c.x * 0.02) * 0.4;
            if (c.x > WIDTH + 150) {
                c.x = -250 - random.nextInt(500);
                c.y = 120 + random.nextInt(180);
            }
        }
        for (FloatingFeather f : feathers) {
            f.y += f.speed * 0.7;
            f.x += Math.sin(f.rotation) * 1.4;
            f.rotation += 0.025;
            if (f.y > HEIGHT) { f.y = -20; f.x = random.nextInt(WIDTH); }
            if (f.x < -20) f.x = WIDTH + 20;
            if (f.x > WIDTH + 20) f.x = -20;
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
            spawnBoostParticles(p1X, p1Y, new Color(80, 240, 220));
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
                spawnBoostParticles(p2X, p2Y, new Color(255, 200, 80));
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
                    spawnExplosion(sx, p.y, new Color(255, 220, 100));
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
        trails.add(new Trail(p1X - 30, p1Y + 5, new Color(80, 240, 220)));
        trails.add(new Trail(p2X - 30, p2Y + 5, new Color(255, 200, 80)));
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
            spawnBoostParticles(p2X, p2Y, new Color(255, 200, 80));
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
        drawIntiSun(g2);
        drawSunRays(g2);
        drawMountains(g2, 0.15, 0);
        drawMountains(g2, 0.35, 1);
        drawMountains(g2, 0.55, 2);
        drawClouds(g2);
        drawCondors(g2);
        drawTemples(g2);
        drawTerraces(g2);
        drawTrees(g2);
        drawLlamas(g2);
        drawFireflies(g2);
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
        drawHoverboard(g2, p1X, p1Y, p1Tilt, new Color(80, 240, 220), new Color(10, 120, 130), 1);
        drawHoverboard(g2, p2X, p2Y, p2Tilt, new Color(255, 200, 80), new Color(180, 80, 20), 2);

        // Particles
        for (Particle p : particles) {
            int alpha = Math.min(255, p.life * 8);
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alpha));
            g2.fillOval((int)p.x, (int)p.y, 5, 5);
        }

        drawFloatingFeathers(g2);
        drawHUD(g2);
        if (state == State.FINISHED) drawFinish(g2);
    }

    private void drawSky(Graphics2D g2) {
        // Andean sky - deep blue to warm golden at horizon
        GradientPaint sky = new GradientPaint((float) 0, 0, new Color(15, 45, 110),
                0, (float) (HEIGHT * 0.7), new Color(255, 150, 80));
        g2.setPaint(sky);
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        // Golden horizon glow (Inti - the sun god's presence)
        GradientPaint horizon = new GradientPaint(0, HEIGHT * 0.58f, new Color(255, 200, 100, 0),
                0, HEIGHT * 0.75f, new Color(255, 200, 100, 200));
        g2.setPaint(horizon);
        g2.fillRect(0, (int)(HEIGHT * 0.58), WIDTH, (int)(HEIGHT * 0.17));
    }

    private void drawIntiSun(Graphics2D g2) {
        int sunX = 750;
        int sunY = 150;
        double pulse = 1 + Math.sin(sunPulse) * 0.08;

        // Outer glow (Inti - sun god)
        for (int i = 10; i > 0; i--) {
            g2.setColor(new Color(255, 220, 100, 10));
            int r = (int)((45 + i * 16) * pulse);
            g2.fillOval(sunX - r, sunY - r, r * 2, r * 2);
        }
        // Sun body
        RadialGradientPaint sunGrad = new RadialGradientPaint(
                new Point(sunX, sunY), (float)(65 * pulse),
                new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(255, 255, 230), new Color(255, 220, 80), new Color(255, 140, 40)});
        g2.setPaint(sunGrad);
        int r = (int)(65 * pulse);
        g2.fillOval(sunX - r, sunY - r, r * 2, r * 2);

        // Inti face suggestion (Incan sun god)
        g2.setColor(new Color(200, 100, 20, 100));
        g2.setStroke(new BasicStroke(2));
        // rays around sun
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8 + sunPulse * 0.3;
            int x1 = sunX + (int)(Math.cos(angle) * (r + 8));
            int y1 = sunY + (int)(Math.sin(angle) * (r + 8));
            int x2 = sunX + (int)(Math.cos(angle) * (r + 22));
            int y2 = sunY + (int)(Math.sin(angle) * (r + 22));
            g2.setColor(new Color(255, 200, 80, 180));
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawSunRays(Graphics2D g2) {
        // Golden sun rays sweeping across the sky
        g2.setColor(new Color(255, 220, 120, 15));
        for (IntiRay r : sunRays) {
            double angle = r.angle + sunPulse * 0.2;
            int cx = 750;
            int cy = 150;
            int length = 900;
            int x2 = cx + (int)(Math.cos(angle + 2.2) * length);
            int y2 = cy + (int)(Math.sin(angle + 2.2) * length);
            Path2D ray = new Path2D.Double();
            ray.moveTo(cx, cy);
            ray.lineTo(x2 - 40, y2);
            ray.lineTo(x2 + 40, y2);
            ray.closePath();
            g2.fill(ray);
        }
    }

    private void drawMountains(Graphics2D g2, double parallax, int layer) {
        int offset = (int)(worldOffset * parallax) % WIDTH;
        // Andean colors - deep green, olive, purple
        Color[] colors = {
                new Color(80, 60, 120),      // far - purple
                new Color(60, 75, 110),      // mid - blue-green
                new Color(45, 60, 90)        // near - dark green-blue
        };
        Color snow = new Color(255, 250, 240, 235);
        Color snowShade = new Color(220, 210, 220, 200);
        Color goldTint = new Color(255, 220, 140, 40);

        g2.setColor(colors[layer]);
        for (int repeat = -1; repeat <= 1; repeat++) {
            for (int i = 0; i < 6; i++) {
                int baseX = i * 220 - offset + repeat * WIDTH - 200;
                int peakY = 180 + layer * 50 + (i % 3) * 45;
                int baseY = HEIGHT;
                int width = 270 + (i % 2) * 90;

                Path2D mountain = new Path2D.Double();
                mountain.moveTo(baseX, baseY);
                mountain.lineTo(baseX + width / 2, peakY);
                mountain.lineTo(baseX + width, baseY);
                mountain.closePath();
                g2.setColor(colors[layer]);
                g2.fill(mountain);

                // Andes snowcap (golden-tinted in the sunlight)
                double snowLine = peakY + (baseY - peakY) * 0.32;
                Path2D snowCap = new Path2D.Double();
                snowCap.moveTo(baseX + width / 2, peakY);
                double t = 0.32;
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

                // Golden sunlit side
                g2.setColor(goldTint);
                Path2D goldGlow = new Path2D.Double();
                goldGlow.moveTo(baseX + width / 2, peakY);
                goldGlow.lineTo(baseX + width, baseY);
                goldGlow.lineTo(baseX + width / 2, baseY);
                goldGlow.closePath();
                g2.fill(goldGlow);

                // Shaded side
                Path2D shade = new Path2D.Double();
                shade.moveTo(baseX + width / 2, peakY);
                shade.lineTo(baseX, baseY);
                shade.lineTo(baseX + width / 2, baseY);
                shade.closePath();
                g2.setColor(new Color(0, 0, 50, 70));
                g2.fill(shade);
            }
        }
    }

    private void drawClouds(Graphics2D g2) {
        for (Cloud c : clouds) {
            g2.setColor(new Color(255, 230, 210, 110));
            g2.fillOval((int)c.x, (int)c.y, (int) c.w, (int) (c.w * 2 / 3));
            g2.fillOval((int)(c.x + c.w * 0.3), (int)(c.y - c.w / 4), (int)(c.w * 0.7), (int)(c.w * 0.7));
            g2.fillOval((int)(c.x + c.w * 0.6), (int)c.y, (int)(c.w * 0.7), (int)(c.w * 0.55));
            g2.setColor(new Color(255, 255, 255, 70));
            g2.fillOval((int)(c.x + c.w * 0.2), (int)(c.y - c.w / 6), (int)(c.w * 0.4), (int)(c.w * 0.35));
        }
    }

    private void drawCondors(Graphics2D g2) {
        // Andean condors soaring
        for (Condor c : condors) {
            double flap = Math.sin(condorWing + c.x * 0.01) * 0.5 + 0.5;
            g2.setColor(new Color(20, 20, 30, 220));
            // Body
            g2.fillOval((int)c.x, (int)c.y, 36, 14);
            // Head
            g2.fillOval((int)c.x + 32, (int)c.y + 1, 10, 8);
            // Beak
            g2.setColor(new Color(240, 200, 100));
            g2.fillOval((int)c.x + 40, (int)c.y + 3, 6, 4);
            // Wings (long, spread)
            g2.setColor(new Color(25, 25, 35, 230));
            Path2D leftWing = new Path2D.Double();
            leftWing.moveTo(c.x + 20, c.y + 5);
            leftWing.lineTo(c.x - 25, c.y - 5 - flap * 8);
            leftWing.lineTo(c.x - 35, c.y + 8 - flap * 4);
            leftWing.lineTo(c.x + 5, c.y + 12);
            leftWing.closePath();
            g2.fill(leftWing);

            Path2D rightWing = new Path2D.Double();
            rightWing.moveTo(c.x + 15, c.y + 5);
            rightWing.lineTo(c.x + 55, c.y - 5 - flap * 8);
            rightWing.lineTo(c.x + 65, c.y + 8 - flap * 4);
            rightWing.lineTo(c.x + 30, c.y + 12);
            rightWing.closePath();
            g2.fill(rightWing);

            // White wing tips (condor markings)
            g2.setColor(new Color(220, 220, 220, 200));
            g2.fillOval((int)c.x - 20, (int)(c.y - 5 - flap * 8), 8, 6);
            g2.fillOval((int)c.x + 50, (int)(c.y - 5 - flap * 8), 8, 6);
        }
    }

    private void drawTemples(Graphics2D g2) {
        int offset = (int)(worldOffset * 0.3) % WIDTH;
        for (IncaTemple t : temples) {
            for (int rep = -1; rep <= 1; rep++) {
                int x = (int)(t.baseX - offset + rep * (WIDTH + 300));
                if (x < -200 || x > WIDTH + 200) continue;
                int baseY = HEIGHT - 130;

                // Stone base (stepped trapezoidal - classic Inca architecture)
                for (int step = 0; step < 4; step++) {
                    int stepW = 140 - step * 20;
                    int stepY = baseY - step * 18;
                    int stepH = 20;
                    // stone color
                    g2.setColor(new Color(140 - step * 8, 120 - step * 6, 90 - step * 4));
                    g2.fillRect(x - stepW / 2, stepY, stepW, stepH);
                    // stone lines
                    g2.setColor(new Color(90, 70, 50, 150));
                    g2.setStroke(new BasicStroke(1));
                    for (int s = 1; s < 6; s++) {
                        int sx = x - stepW / 2 + s * stepW / 6;
                        g2.drawLine(sx, stepY, sx, stepY + stepH);
                    }
                    g2.drawLine(x - stepW / 2, stepY + stepH - 1, x + stepW / 2, stepY + stepH - 1);
                }
                // Golden roof / top structure
                g2.setColor(new Color(255, 210, 80, 230));
                g2.fillRect(x - 20, baseY - 4 * 18 - 14, 40, 14);
                // Golden glow
                g2.setColor(new Color(255, 230, 100, 100));
                g2.fillOval(x - 30, baseY - 4 * 18 - 30, 60, 30);

                // Doorway with firelight
                g2.setColor(new Color(60, 40, 20));
                g2.fillRect(x - 8, baseY - 18, 16, 18);
                g2.setColor(new Color(255, 200, 80, 200));
                g2.fillOval(x - 5, baseY - 12, 10, 8);
            }
        }
    }

    private void drawTerraces(Graphics2D g2) {
        int offset = (int)(worldOffset * 0.4) % WIDTH;
        // Andean agricultural terraces (green steps)
        for (TerracedField tf : terraces) {
            for (int rep = -1; rep <= 1; rep++) {
                int x = (int)(tf.baseX - offset + rep * (WIDTH + 200));
                if (x < -100 || x > WIDTH + 100) continue;
                int baseY = HEIGHT - 60;
                int steps = tf.steps;
                for (int s = 0; s < steps; s++) {
                    int sw = 100 - s * 8;
                    int sy = baseY - s * 8;
                    // Green terrace with slight color variation
                    int green = 100 + s * 10;
                    g2.setColor(new Color(60 + s * 8, green, 40 + s * 5));
                    g2.fillRect(x - sw / 2, sy, sw, 8);
                    // Terrace wall (stone)
                    g2.setColor(new Color(120, 100, 70, 200));
                    g2.fillRect(x - sw / 2, sy + 7, sw, 3);
                    g2.setColor(new Color(70, 55, 35, 180));
                    g2.drawLine(x - sw / 2, sy + 9, x + sw / 2, sy + 9);
                }
            }
        }
    }

    private void drawTrees(Graphics2D g2) {
        int offset = (int)(worldOffset * 0.7) % WIDTH;
        // Andean trees (Polylepis - knobby, dark green)
        for (Tree t : trees) {
            for (int rep = -1; rep <= 1; rep++) {
                int x = (int)(t.baseX - offset + rep * WIDTH);
                if (x < -50 || x > WIDTH + 50) continue;
                int baseY = HEIGHT - 40;
                int h = t.size * 25;

                // Trunk
                g2.setColor(new Color(80, 50, 30));
                g2.fillRect(x - 4, baseY - h, 8, h);
                // Knotty trunk lines
                g2.setColor(new Color(50, 30, 15));
                for (int k = 0; k < 4; k++) {
                    int ky = baseY - h + 10 + k * (h / 4);
                    g2.drawLine(x - 4, ky, x + 4, ky + 2);
                }
                // Canopy - rounded, olive-green
                g2.setColor(new Color(60, 90, 40));
                g2.fillOval(x - 30, baseY - h - 30, 60, 40);
                g2.setColor(new Color(90, 130, 60));
                g2.fillOval(x - 24, baseY - h - 38, 48, 30);
                g2.setColor(new Color(130, 170, 90, 200));
                g2.fillOval(x - 16, baseY - h - 42, 32, 20);
            }
        }
    }

    private void drawLlamas(Graphics2D g2) {
        // Llamas / alpacas grazing on the terraces
        for (Llamas l : llamas) {
            double screenX = (l.baseX - worldOffset * 0.4) % (WIDTH + 400);
            if (screenX < -80) screenX += WIDTH + 400;
            if (screenX < -80 || screenX > WIDTH + 80) continue;
            int x = (int)screenX;
            int y = (int)l.baseY;
            int size = 12;

            // Body (fluffy white/cream)
            g2.setColor(new Color(245, 235, 210));
            g2.fillOval(x, y - size, size * 2, size);
            // Fluff texture
            g2.setColor(new Color(220, 205, 180));
            g2.fillOval(x + 3, y - size - 2, size, 4);
            g2.fillOval(x + size, y - size - 2, size, 4);

            // Neck (long)
            g2.setColor(new Color(240, 230, 205));
            g2.fillRect(x + size + 4, y - size - 12, 6, 14);
            // Head
            g2.fillOval(x + size + 2, y - size - 18, 12, 10);
            // Ears
            g2.setColor(new Color(200, 185, 160));
            g2.fillOval(x + size + 2, y - size - 22, 3, 5);
            g2.fillOval(x + size + 11, y - size - 22, 3, 5);
            // Eye
            g2.setColor(Color.BLACK);
            g2.fillOval(x + size + 10, y - size - 14, 2, 2);
            // Legs
            g2.setColor(new Color(225, 210, 185));
            g2.fillRect(x + 2, y - 2, 3, 8);
            g2.fillRect(x + size - 2, y - 2, 3, 8);
            g2.fillRect(x + size + 4, y - 2, 3, 8);
            g2.fillRect(x + size * 2 - 4, y - 2, 3, 8);
        }
    }

    private void drawFireflies(Graphics2D g2) {
        // Golden glowing fireflies (spirit lights)
        for (Firefly f : fireflies) {
            double glow = 0.5 + Math.sin(f.phase * 3) * 0.5;
            int r = (int)(3 + glow * 4);
            for (int i = 3; i > 0; i--) {
                g2.setColor(new Color(255, 220, 100, (int)(40 * glow / i)));
                g2.fillOval((int)f.x - r * i, (int)f.y - r * i, r * i * 2, r * i * 2);
            }
            g2.setColor(new Color(255, 240, 180, (int)(200 * glow + 55)));
            g2.fillOval((int)f.x - 3, (int)f.y - 3, 6, 6);
        }
    }

    private void drawGround(Graphics2D g2) {
        int groundTop = HEIGHT - 60;
        // Golden-green ground with Andean warmth
        GradientPaint ground = new GradientPaint(0, groundTop, new Color(200, 210, 150),
                0, HEIGHT, new Color(120, 130, 90));
        g2.setPaint(ground);
        g2.fillRect(0, groundTop, WIDTH, HEIGHT - groundTop);

        // Stone path hint
        g2.setColor(new Color(160, 140, 100, 150));
        for (int i = 0; i < 20; i++) {
            int x = (int)((i * 70 - worldOffset * 0.9) % (WIDTH + 70));
            if (x < 0) x += WIDTH + 70;
            g2.fillRect(x, groundTop + 10 + (i % 3) * 15, 22, 3);
        }

        // Track edge glow (golden Incan line)
        g2.setColor(new Color(255, 200, 80, 120));
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(0, groundTop + 5, WIDTH, groundTop + 5);
        g2.setColor(new Color(255, 240, 180, 100));
        g2.drawLine(0, groundTop + 9, WIDTH, groundTop + 9);
    }

    private void drawFloatingFeathers(Graphics2D g2) {
        // Condor feathers drifting down
        for (FloatingFeather f : feathers) {
            AffineTransform old = g2.getTransform();
            g2.translate(f.x, f.y);
            g2.rotate(f.rotation);
            g2.setColor(new Color(60, 60, 70, 210));
            // Simple feather shape
            Path2D feather = new Path2D.Double();
            feather.moveTo(0, -10);
            feather.quadTo(6, 0, 0, 10);
            feather.quadTo(-6, 0, 0, -10);
            feather.closePath();
            g2.fill(feather);
            // White tip
            g2.setColor(new Color(230, 230, 230, 220));
            g2.fillOval(-2, 5, 4, 4);
            g2.setTransform(old);
        }
    }

    private void drawHoverboard(Graphics2D g2, double x, double y, double tilt, Color glow, Color body, int num) {
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(tilt);

        // Glow beneath
        for (int i = 4; i > 0; i--) {
            g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 30));
            g2.fillOval(-60 - i * 5, 10 - i * 2, 120 + i * 10, 25 + i * 4);
        }
        // Shadow
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillOval(-45, 12, 90, 18);
        // Board - turquoise with golden trim for P1, gold/red for P2
        GradientPaint boardGrad = new GradientPaint(0, -15, glow, 0, 15, body);
        g2.setPaint(boardGrad);
        RoundRectangle2D board = new RoundRectangle2D.Double(-45, -10, 90, 22, 20, 20);
        g2.fill(board);
        // Golden trim
        g2.setColor(new Color(255, 210, 80));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(-42, -8, 84, 18, 16, 16);
        // Highlight
        g2.setColor(new Color(255, 255, 255, 130));
        g2.fillRoundRect(-40, -8, 80, 6, 10, 10);
        // Neon edge
        g2.setStroke(new BasicStroke(2));
        g2.setColor(glow);
        g2.draw(board);

        // Tiny golden sun decal (Inti)
        g2.setColor(new Color(255, 220, 100));
        g2.fillOval(24, -3, 8, 8);
        g2.setColor(new Color(255, 180, 60));
        g2.setStroke(new BasicStroke(1));
        for (int a = 0; a < 6; a++) {
            double angle = a * Math.PI / 3;
            g2.drawLine(28, 1,
                    28 + (int)(Math.cos(angle) * 6),
                    1 + (int)(Math.sin(angle) * 6));
        }

        // Rider with Andean poncho/hat suggestion
        g2.setColor(new Color(40, 40, 60));
        g2.fillOval(-8, -42, 16, 16);
        g2.setColor(num == 1 ? new Color(80, 240, 220) : new Color(255, 200, 80));
        g2.fillOval(-9, -43, 18, 18);
        // Hat brim (Andean chullo)
        g2.setColor(num == 1 ? new Color(200, 60, 40) : new Color(60, 30, 120));
        g2.fillRect(-11, -45, 22, 5);
        g2.setColor(new Color(30, 30, 50));
        g2.fillRect(-6, -26, 12, 18);
        // Poncho color splash
        g2.setColor(num == 1 ? new Color(255, 100, 60) : new Color(200, 60, 150));
        g2.fillRect(-7, -22, 14, 10);
        // Arms
        g2.setStroke(new BasicStroke(4));
        g2.setColor(new Color(30, 30, 50));
        g2.drawLine(-6, -20, -18, -28);
        g2.drawLine(6, -20, 18, -28);
        // Legs
        g2.drawLine(-3, -8, -8, 0);
        g2.drawLine(3, -8, 8, 0);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("P" + num, -5, -50);

        g2.setTransform(old);
    }

    private void drawObstacle(Graphics2D g2, double x, double y, int type) {
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);

        if (type == 0) {
            // Sacred stone pyramid (mini)
            g2.setColor(new Color(160, 130, 90, 230));
            int[] xs = {0, -30, 30};
            int[] ys = {-30, 30, 30};
            g2.fillPolygon(xs, ys, 3);
            // Stone texture lines
            g2.setColor(new Color(100, 80, 50));
            g2.setStroke(new BasicStroke(2));
            for (int i = -20; i <= 20; i += 10) {
                g2.drawLine(i, -5, i, 30);
            }
            // Outline
            g2.setColor(new Color(255, 210, 100));
            g2.setStroke(new BasicStroke(3));
            g2.drawPolygon(xs, ys, 3);
            // Golden top
            g2.setColor(new Color(255, 220, 80));
            g2.fillOval(-6, -34, 12, 8);
        } else if (type == 1) {
            // Golden ceremonial vessel with glow
            for (int i = 5; i > 0; i--) {
                g2.setColor(new Color(255, 220, 100, 30));
                g2.fillOval(-i * 8 - 5, -i * 8 - 5, (i * 8 + 5) * 2, (i * 8 + 5) * 2);
            }
            g2.setColor(new Color(255, 200, 60));
            g2.fillOval(-18, -18, 36, 36);
            g2.setColor(new Color(180, 120, 30));
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(-18, -18, 36, 36);
            // Inner decoration
            g2.setColor(new Color(200, 60, 40));
            g2.fillOval(-8, -8, 16, 16);
            g2.setColor(new Color(255, 240, 180));
            g2.fillOval(-3, -3, 6, 6);
        } else if (type == 2) {
            // Stone idol / monolith
            g2.setColor(new Color(90, 70, 50, 240));
            Path2D p = new Path2D.Double();
            p.moveTo(0, -35);
            p.lineTo(20, -10);
            p.lineTo(25, 30);
            p.lineTo(-25, 30);
            p.lineTo(-20, -10);
            p.closePath();
            g2.fill(p);
            // Carved pattern
            g2.setColor(new Color(50, 35, 20));
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(-15, -15, 15, -15);
            g2.drawLine(-15, 0, 15, 0);
            g2.drawLine(-15, 15, 15, 15);
            // Golden eye
            g2.setColor(new Color(255, 220, 80));
            g2.fillOval(-6, -8, 12, 6);
            g2.setColor(new Color(200, 40, 30));
            g2.fillOval(-3, -6, 6, 3);
            // Outline
            g2.setColor(new Color(255, 220, 100));
            g2.setStroke(new BasicStroke(2));
            g2.draw(p);
        } else {
            // Flaming torch / fire hazard
            g2.setColor(new Color(120, 60, 20));
            g2.fillRect(-4, -10, 8, 40);
            // Flame
            for (int i = 5; i > 0; i--) {
                int fh = 20 + i * 6;
                g2.setColor(new Color(255, 150, 30, 40 + i * 10));
                g2.fillOval(-i * 3 - 3, -20 - fh / 2, i * 6 + 6, fh);
            }
            g2.setColor(new Color(255, 220, 80));
            g2.fillOval(-8, -30, 16, 20);
            g2.setColor(new Color(255, 100, 30));
            g2.fillOval(-5, -24, 10, 14);
        }
        g2.setTransform(old);
    }

    private void drawPowerUp(Graphics2D g2, double x, double y) {
        double pulse = 1 + Math.sin(waveTime * 4) * 0.15;
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.scale(pulse, pulse);

        // Golden Inti sun power-up
        for (int i = 6; i > 0; i--) {
            g2.setColor(new Color(255, 220, 100, 25));
            g2.fillOval(-i * 5, -i * 5, i * 10, i * 10);
        }
        g2.rotate(waveTime * 0.3);
        // Rays
        g2.setColor(new Color(255, 240, 140));
        g2.setStroke(new BasicStroke(2));
        for (int a = 0; a < 12; a++) {
            double angle = a * Math.PI / 6;
            int x1 = (int)(Math.cos(angle) * 18);
            int y1 = (int)(Math.sin(angle) * 18);
            int x2 = (int)(Math.cos(angle) * 24);
            int y2 = (int)(Math.sin(angle) * 24);
            g2.drawLine(x1, y1, x2, y2);
        }
        // Sun disc
        g2.setColor(new Color(255, 200, 60));
        g2.fillOval(-15, -15, 30, 30);
        g2.setColor(new Color(255, 240, 180));
        g2.fillOval(-9, -9, 18, 18);
        // Face suggestion
        g2.setColor(new Color(180, 100, 20));
        g2.fillOval(-6, -4, 3, 3);
        g2.fillOval(3, -4, 3, 3);
        g2.drawArc(-4, -2, 8, 6, 200, 140);

        g2.setTransform(old);
    }

    private void drawHUD(Graphics2D g2) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));

        g2.setColor(Color.WHITE);
        g2.drawString("P1", 30, 30);
        drawProgressBar(g2, 60, 20, 300, 16, p1Distance / TRACK_LENGTH, new Color(80, 240, 220));
        drawBoostBar(g2, 60, 44, 200, 8, p1BoostFuel / 100.0, new Color(0, 220, 200));

        g2.drawString(mode == Mode.VS_COMPUTER ? "CPU" : "P2", 30, 80);
        drawProgressBar(g2, 60, 70, 300, 16, p2Distance / TRACK_LENGTH, new Color(255, 200, 80));
        drawBoostBar(g2, 60, 94, 200, 8, p2BoostFuel / 100.0, new Color(255, 200, 80));

        g2.setColor(new Color(255, 255, 255, 200));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString(String.format("Distance: %.0f / %d", p1Distance, TRACK_LENGTH), WIDTH - 220, 30);

        // Small Inti sun icon in HUD corner
        drawMiniIntiIcon(g2, WIDTH - 60, 60);
    }

    private void drawMiniIntiIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(255, 200, 60));
        g2.fillOval(x - 10, y - 10, 20, 20);
        g2.setColor(new Color(255, 240, 140));
        g2.fillOval(x - 6, y - 6, 12, 12);
        g2.setColor(new Color(255, 200, 60));
        g2.setStroke(new BasicStroke(1.5f));
        for (int a = 0; a < 8; a++) {
            double angle = a * Math.PI / 4;
            g2.drawLine(x + (int)(Math.cos(angle) * 12), y + (int)(Math.sin(angle) * 12),
                    x + (int)(Math.cos(angle) * 16), y + (int)(Math.sin(angle) * 16));
        }
    }

    private void drawProgressBar(Graphics2D g2, int x, int y, int w, int h, double progress, Color color) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(x, y, w, h, h, h);
        g2.setColor(color);
        g2.fillRoundRect(x, y, (int)(w * Math.min(1, progress)), h, h, h);
        g2.setColor(new Color(255, 220, 100));
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
        drawIntiSun(g2);
        drawSunRays(g2);
        drawMountains(g2, 0.15, 0);
        drawMountains(g2, 0.35, 1);
        drawClouds(g2);
        drawCondors(g2);
        drawTemples(g2);
        drawTrees(g2);
        drawFireflies(g2);
        drawFloatingFeathers(g2);

        // Title
        g2.setFont(new Font("SansSerif", Font.BOLD, 52));
        String title = "HOVERBOARD";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (WIDTH - fm.stringWidth(title)) / 2;
        GradientPaint titleGrad = new GradientPaint(0, 100, new Color(255, 220, 100),
                WIDTH, 160, new Color(80, 240, 220));
        g2.setPaint(titleGrad);
        g2.drawString(title, tx, 180);

        g2.setFont(new Font("SansSerif", Font.BOLD, 32));
        String title2 = "MACHU PICCHU ADVENTURE";
        fm = g2.getFontMetrics();
        tx = (WIDTH - fm.stringWidth(title2)) / 2;
        GradientPaint t2Grad = new GradientPaint(0, 210, new Color(255, 240, 200),
                WIDTH, 240, new Color(255, 150, 60));
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
                "Collect golden Inti suns to boost!",
                "Avoid stone pyramids, idols, and flames!"
        };
        Color[] colors = {
                new Color(150, 240, 220), new Color(255, 220, 120), Color.WHITE, Color.WHITE,
                new Color(150, 240, 220), new Color(255, 220, 120), Color.WHITE,
                new Color(255, 220, 100), new Color(255, 180, 120)
        };
        for (int i = 0; i < options.length; i++) {
            fm = g2.getFontMetrics();
            int w = fm.stringWidth(options[i]);
            g2.setColor(colors[i]);
            g2.drawString(options[i], (WIDTH - w) / 2, 320 + i * 34);
        }

        // Decorative hoverboards
        drawHoverboard(g2, 150, HEIGHT - 100, Math.sin(waveTime) * 0.2,
                new Color(80, 240, 220), new Color(10, 120, 130), 1);
        drawHoverboard(g2, WIDTH - 150, HEIGHT - 100, -Math.sin(waveTime) * 0.2,
                new Color(255, 200, 80), new Color(180, 80, 20), 2);
    }

    private void drawFinish(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setFont(new Font("SansSerif", Font.BOLD, 60));
        String msg;
        Color c;
        if (winner == 1) {
            msg = "PLAYER 1 WINS!";
            c = new Color(120, 240, 220);
        } else {
            msg = mode == Mode.VS_COMPUTER ? "COMPUTER WINS!" : "PLAYER 2 WINS!";
            c = new Color(255, 220, 120);
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

        // Inti icon
        drawMiniIntiIcon(g2, WIDTH / 2, HEIGHT / 2 + 120);
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
    static class Firefly {
        double x, y, phase, speed;
        Firefly(double x, double y, double p, double s) { this.x = x; this.y = y; phase = p; speed = s; }
    }
    static class Cloud {
        double x, y, w, speed;
        Cloud(double x, double y, double w, double s) { this.x = x; this.y = y; this.w = w; speed = s; }
    }
    static class Tree {
        double baseX; int size;
        Tree(double x, int s) { baseX = x; size = s; }
    }
    static class TerracedField {
        double baseX, parallax; int steps;
        TerracedField(double x, double p, int s) { baseX = x; parallax = p; steps = s; }
    }
    static class Llamas {
        double baseX, baseY, parallax;
        Llamas(double x, double y, double p) { baseX = x; baseY = y; parallax = p; }
    }
    static class Condor {
        double x, y, speed;
        Condor(double x, double y, double s) { this.x = x; this.y = y; speed = s; }
    }
    static class IncaTemple {
        double baseX, parallax;
        IncaTemple(double x, double p) { baseX = x; parallax = p; }
    }
    static class IntiRay {
        double angle;
        IntiRay(double a) { angle = a; }
    }
    static class FloatingFeather {
        double x, y, rotation, speed;
        FloatingFeather(double x, double y, double r, double s) {
            this.x = x; this.y = y; rotation = r; speed = s;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hoverboard Machu Picchu Racing Adventure 🦙🏔️");
            HoverboardMachuPicchuRacing game = new HoverboardMachuPicchuRacing();
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