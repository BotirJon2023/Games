import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


class RacingGame extends JPanel implements KeyListener {
    private static final int WIDTH = 1100;
    private static final int HEIGHT = 720;
    private static final int ROAD_SEGMENTS = 38;
    private static final double RACE_DISTANCE = 2400.0;

    private enum Screen { MENU, COUNTDOWN, RACING, FINISHED }
    private enum Mode { TWO_PLAYERS, COMPUTER }

    private final Timer timer;
    private final boolean[] keys = new boolean[512];
    private final Random random = new Random(17);
    private final List<Particle> particles = new ArrayList<>();
    private final List<Star> stars = new ArrayList<>();
    private final List<Opponent> traffic = new ArrayList<>();

    private Screen screen = Screen.MENU;
    private Mode mode = Mode.COMPUTER;
    private PlayerCar playerOne;
    private PlayerCar playerTwo;
    private double elapsed;
    private double countdown;
    private double roadScroll;
    private String winnerText = "";
    private long lastNanos;

    RacingGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        setBackground(new Color(7, 10, 25));
        createStars();
        timer = new Timer(16, this::tick);
        timer.start();
    }

    private void createStars() {
        for (int i = 0; i < 100; i++) {
            stars.add(new Star(random.nextDouble(), random.nextDouble() * 0.57,
                    0.45 + random.nextDouble() * 1.9, random.nextDouble()));
        }
    }

    private void beginRace(Mode selectedMode) {
        mode = selectedMode;
        playerOne = new PlayerCar("P1", new Color(255, 74, 125),
                new Color(255, 196, 91), -0.35);
        playerTwo = new PlayerCar(mode == Mode.COMPUTER ? "CPU" : "P2",
                new Color(49, 211, 255), new Color(105, 113, 255), 0.35);

        traffic.clear();
        for (int i = 0; i < 9; i++) {
            double lane = (i % 3 - 1) * 0.58
                    + (random.nextDouble() - 0.5) * 0.12;
            traffic.add(new Opponent(lane, 0.22 + i * 0.085, i % 2 == 0));
        }

        particles.clear();
        elapsed = 0;
        roadScroll = 0;
        countdown = 3.7;
        winnerText = "";
        screen = Screen.COUNTDOWN;
        requestFocusInWindow();
    }

    private void tick(ActionEvent ignored) {
        long now = System.nanoTime();
        if (lastNanos == 0) lastNanos = now;
        double dt = Math.min((now - lastNanos) / 1_000_000_000.0, 0.04);
        lastNanos = now;

        if (screen == Screen.COUNTDOWN) {
            countdown -= dt;
            if (countdown <= 0) screen = Screen.RACING;
        } else if (screen == Screen.RACING) {
            updateRace(dt);
        } else if (screen == Screen.MENU) {
            roadScroll += dt * 0.16;
        }

        updateParticles(dt);
        repaint();
    }

    private void updateRace(double dt) {
        elapsed += dt;

        playerOne.update(dt,
                keys[KeyEvent.VK_W],
                keys[KeyEvent.VK_S],
                keys[KeyEvent.VK_A],
                keys[KeyEvent.VK_D],
                keys[KeyEvent.VK_SHIFT],
                playerTwo);

        if (mode == Mode.COMPUTER) {
            playerTwo.updateComputer(dt, playerOne, traffic);
        } else {
            playerTwo.update(dt,
                    keys[KeyEvent.VK_UP],
                    keys[KeyEvent.VK_DOWN],
                    keys[KeyEvent.VK_LEFT],
                    keys[KeyEvent.VK_RIGHT],
                    keys[KeyEvent.VK_CONTROL],
                    playerOne);
        }

        roadScroll += (playerOne.speed + playerTwo.speed) * dt * 0.35;

        for (Opponent car : traffic) {
            car.z -= (0.22 + Math.max(playerOne.speed, playerTwo.speed) * 0.15) * dt;
            car.wobble += dt * (0.8 + car.lane * 0.2);

            if (car.z < -0.1) {
                car.z = 1.1 + random.nextDouble() * 0.45;
                car.lane = (random.nextInt(3) - 1) * 0.58
                        + (random.nextDouble() - 0.5) * 0.1;
            }
        }

        checkCollisions(playerOne);
        checkCollisions(playerTwo);

        if (playerOne.distance >= RACE_DISTANCE
                || playerTwo.distance >= RACE_DISTANCE) {
            if (Math.abs(playerOne.distance - playerTwo.distance) < 20) {
                winnerText = "PHOTO FINISH — BOTH DRIVERS!";
            } else {
                winnerText = playerOne.distance > playerTwo.distance
                        ? "P1 TAKES THE CHECKERED FLAG!"
                        : playerTwo.name + " WINS THE RACE!";
            }
            screen = Screen.FINISHED;
        }
    }

    private void checkCollisions(PlayerCar player) {
        for (Opponent car : traffic) {
            if (Math.abs(car.z - 0.12) < 0.06
                    && Math.abs(car.lane - player.x) < 0.18) {
                player.speed *= 0.58;
                player.distance = Math.max(0, player.distance - 4);
                spawnBurst(roadX(player.x, 0.12),
                        roadY(0.12) + 5,
                        new Color(255, 180, 64));
                car.z -= 0.08;
            }
        }
    }

    private void updateParticles(double dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vy += 38 * dt;
            p.life -= dt;
            if (p.life <= 0) particles.remove(i);
        }

        if (screen == Screen.RACING) {
            spawnExhaust(playerOne);
            spawnExhaust(playerTwo);
        }
    }

    private void spawnExhaust(PlayerCar car) {
        if (car == null || car.speed < 0.2 || random.nextDouble() > 0.72) {
            return;
        }

        double x = roadX(car.x, 0.12) + (car == playerOne ? -10 : 10);
        double y = roadY(0.12) + 34;
        Color color = car.boosting
                ? new Color(118, 226, 255, 190)
                : new Color(179, 194, 255, 100);

        particles.add(new Particle(
                x, y,
                (random.nextDouble() - 0.5) * 22,
                12 + random.nextDouble() * 24,
                0.25 + random.nextDouble() * 0.3,
                color,
                2 + random.nextDouble() * 3));
    }

    private void spawnBurst(double x, double y, Color color) {
        for (int i = 0; i < 18; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 35 + random.nextDouble() * 85;

            particles.add(new Particle(
                    x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    0.35 + random.nextDouble() * 0.4,
                    color,
                    2 + random.nextDouble() * 4));
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        drawWorld(g);

        if (screen == Screen.MENU) drawMenu(g);
        if (screen == Screen.COUNTDOWN) drawCountdown(g);
        if (screen == Screen.FINISHED) drawFinish(g);

        g.dispose();
    }

    private void drawWorld(Graphics2D g) {
        int horizon = 225;

        GradientPaint sky = new GradientPaint(
                0, 0, new Color(17, 19, 55),
                0, horizon, new Color(102, 40, 93));
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, horizon + 2);

        g.setColor(new Color(255, 137, 105, 130));
        g.fillOval(WIDTH - 210, 72, 92, 92);
        g.setColor(new Color(255, 188, 128, 35));
        g.fillOval(WIDTH - 242, 40, 156, 156);

        drawStars(g, horizon);
        drawMountainRange(g, horizon);

        g.setColor(new Color(18, 26, 32));
        g.fillRect(0, horizon, WIDTH, HEIGHT - horizon);

        drawTrack(g, horizon);
        drawRoadsideLights(g, horizon);

        if (screen == Screen.RACING
                || screen == Screen.COUNTDOWN
                || screen == Screen.FINISHED) {
            for (Opponent car : traffic) drawOpponent(g, car);

            drawCar(g, playerOne,
                    roadX(playerOne.x, 0.12), roadY(0.12) + 2, 1.0);
            drawCar(g, playerTwo,
                    roadX(playerTwo.x, 0.12), roadY(0.12) + 2, 1.0);
        }

        drawParticles(g);
        if (screen != Screen.MENU) drawHud(g);
    }

    private void drawStars(Graphics2D g, int horizon) {
        for (Star star : stars) {
            float alpha = (float) (0.25 + 0.55
                    * (0.5 + 0.5 * Math.sin(elapsed * 1.5 + star.twinkle * 8)));

            g.setColor(new Color(215, 228, 255,
                    Math.min(255, (int) (alpha * 255))));

            float size = (float) star.size;
            g.fill(new Ellipse2D.Double(
                    star.x * WIDTH,
                    star.y * horizon,
                    size,
                    size));
        }
    }

    private void drawMountainRange(Graphics2D g, int horizon) {
        Polygon back = new Polygon();
        back.addPoint(0, horizon);

        for (int x = 0; x <= WIDTH; x += 45) {
            int y = horizon - 20
                    - (int) (28 * Math.sin(x * 0.009)
                    + 20 * Math.sin(x * 0.024 + 2));
            back.addPoint(x, y);
        }

        back.addPoint(WIDTH, horizon);
        g.setColor(new Color(30, 28, 67));
        g.fillPolygon(back);

        Polygon front = new Polygon();
        front.addPoint(0, horizon + 16);

        for (int x = 0; x <= WIDTH; x += 38) {
            int y = horizon + 7
                    - (int) (20 * Math.sin(x * 0.012 + 1)
                    + 13 * Math.sin(x * 0.035));
            front.addPoint(x, y);
        }

        front.addPoint(WIDTH, horizon + 16);
        g.setColor(new Color(21, 27, 48));
        g.fillPolygon(front);
    }

    private void drawTrack(Graphics2D g, int horizon) {
        for (int i = 0; i < ROAD_SEGMENTS; i++) {
            double z1 = i / (double) ROAD_SEGMENTS;
            double z2 = (i + 1) / (double) ROAD_SEGMENTS;

            int y1 = roadY(z1);
            int y2 = roadY(z2);
            int center1 = roadCenter(z1);
            int center2 = roadCenter(z2);
            int roadHalf1 = roadHalfWidth(z1);
            int roadHalf2 = roadHalfWidth(z2);

            Polygon grass = new Polygon(
                    new int[]{0, WIDTH, WIDTH, 0},
                    new int[]{y1, y1, y2, y2},
                    4);

            g.setColor(i % 2 == 0
                    ? new Color(18, 52, 49)
                    : new Color(20, 59, 51));
            g.fillPolygon(grass);

            Polygon road = new Polygon(
                    new int[]{
                            center1 - roadHalf1,
                            center1 + roadHalf1,
                            center2 + roadHalf2,
                            center2 - roadHalf2
                    },
                    new int[]{y1, y1, y2, y2},
                    4);

            g.setColor(i % 2 == 0
                    ? new Color(35, 38, 51)
                    : new Color(39, 42, 56));
            g.fillPolygon(road);

            int curb1 = Math.max(2, roadHalf1 / 10);
            int curb2 = Math.max(2, roadHalf2 / 10);

            g.setColor(i % 2 == 0
                    ? new Color(250, 239, 219)
                    : new Color(239, 69, 102));

            g.fillPolygon(new Polygon(
                    new int[]{
                            center1 - roadHalf1,
                            center1 - roadHalf1 + curb1,
                            center2 - roadHalf2 + curb2,
                            center2 - roadHalf2
                    },
                    new int[]{y1, y1, y2, y2},
                    4));

            g.fillPolygon(new Polygon(
                    new int[]{
                            center1 + roadHalf1 - curb1,
                            center1 + roadHalf1,
                            center2 + roadHalf2,
                            center2 + roadHalf2 - curb2
                    },
                    new int[]{y1, y1, y2, y2},
                    4));

            if (i % 2 == 0) {
                for (int lane = 1; lane < 3; lane++) {
                    int lx1 = center1 - roadHalf1
                            + lane * roadHalf1 * 2 / 3;
                    int lx2 = center2 - roadHalf2
                            + lane * roadHalf2 * 2 / 3;

                    g.setColor(new Color(214, 223, 232, 120));
                    g.fillPolygon(new Polygon(
                            new int[]{lx1 - 2, lx1 + 2, lx2 + 3, lx2 - 3},
                            new int[]{y1, y1, y2, y2},
                            4));
                }
            }
        }
    }

    private void drawRoadsideLights(Graphics2D g, int horizon) {
        for (int i = 1; i < 13; i++) {
            double z = (i / 13.0 + roadScroll * 0.12) % 1.0;
            int y = roadY(z);
            int center = roadCenter(z);
            int half = roadHalfWidth(z);
            int postHeight = Math.max(5, (int) (z * 47));

            drawLamp(g, center - half - postHeight / 3, y, postHeight, false);
            drawLamp(g, center + half + postHeight / 3, y, postHeight, true);
        }
    }

    private void drawLamp(Graphics2D g, int x, int y, int height, boolean right) {
        g.setColor(new Color(102, 113, 144, 150));
        g.setStroke(new BasicStroke(Math.max(1, height / 7f)));
        g.drawLine(x, y, x, y - height);

        g.setColor(new Color(114, 224, 255, 190));
        g.fillOval(
                x - Math.max(2, height / 6),
                y - height - Math.max(2, height / 9),
                Math.max(4, height / 3),
                Math.max(4, height / 4));

        if (height > 14) {
            g.setColor(new Color(82, 207, 255, 35));
            g.fillOval(x - height, y - height * 2, height * 2, height * 2);
        }
    }

    private void drawOpponent(Graphics2D g, Opponent car) {
        if (car.z <= 0 || car.z > 1.2) return;

        int x = roadX(car.lane, car.z);
        int y = roadY(car.z);
        double scale = 0.23 + car.z * 0.92;

        drawCar(g,
                new PlayerCar("traffic",
                        new Color(255, 176, 70),
                        new Color(249, 80, 117),
                        car.lane),
                x, y, scale);
    }

    private void drawCar(Graphics2D g, PlayerCar car, int x, int y, double scale) {
        if (car == null) return;

        int w = (int) (66 * scale);
        int h = (int) (112 * scale);
        int glow = Math.max(3, (int) (13 * scale));

        g.setColor(new Color(
                car.primary.getRed(),
                car.primary.getGreen(),
                car.primary.getBlue(),
                36));
        g.fillOval(x - w / 2 - glow, y + h / 2 - glow,
                w + glow * 2, glow * 3);

        g.setColor(new Color(7, 8, 15, 145));
        g.fillOval(x - w / 2, y + h / 2, w, h / 3);

        Path2D body = new Path2D.Double();
        body.moveTo(x - w * 0.38, y + h * 0.48);
        body.lineTo(x - w * 0.49, y + h * 0.08);
        body.lineTo(x - w * 0.33, y - h * 0.42);
        body.lineTo(x - w * 0.16, y - h * 0.53);
        body.lineTo(x + w * 0.16, y - h * 0.53);
        body.lineTo(x + w * 0.33, y - h * 0.42);
        body.lineTo(x + w * 0.49, y + h * 0.08);
        body.lineTo(x + w * 0.38, y + h * 0.48);
        body.closePath();

        g.setPaint(new GradientPaint(
                x - w / 2, y, car.primary,
                x + w / 2, y + h, car.secondary));
        g.fill(body);

        g.setColor(new Color(255, 255, 255, 65));
        g.draw(body);

        Polygon windshield = new Polygon(
                new int[]{
                        x - (int) (w * .21),
                        x + (int) (w * .21),
                        x + (int) (w * .28),
                        x - (int) (w * .28)
                },
                new int[]{
                        y - (int) (h * .36),
                        y - (int) (h * .36),
                        y - (int) (h * .08),
                        y - (int) (h * .08)
                },
                4);

        g.setColor(new Color(13, 22, 48, 230));
        g.fillPolygon(windshield);

        g.setColor(new Color(144, 232, 255, 100));
        g.drawLine(
                x - (int) (w * .17),
                y - (int) (h * .30),
                x + (int) (w * .19),
                y - (int) (h * .30));

        g.setColor(new Color(255, 248, 219, 230));
        g.fillOval(
                x - (int) (w * .36),
                y - (int) (h * .04),
                Math.max(2, (int) (w * .16)),
                Math.max(2, (int) (h * .1)));
        g.fillOval(
                x + (int) (w * .20),
                y - (int) (h * .04),
                Math.max(2, (int) (w * .16)),
                Math.max(2, (int) (h * .1)));

        g.setColor(new Color(15, 13, 25, 220));
        g.fillRect(
                x - (int) (w * .37),
                y + (int) (h * .25),
                Math.max(2, (int) (w * .74)),
                Math.max(2, (int) (h * .1)));

        g.setColor(new Color(255, 67, 103, 210));
        g.fillRect(
                x - (int) (w * .30),
                y + (int) (h * .29),
                Math.max(2, (int) (w * .18)),
                Math.max(2, (int) (h * .07)));
        g.fillRect(
                x + (int) (w * .12),
                y + (int) (h * .29),
                Math.max(2, (int) (w * .18)),
                Math.max(2, (int) (h * .07)));
    }

    private void drawParticles(Graphics2D g) {
        for (Particle p : particles) {
            int alpha = Math.max(0,
                    Math.min(255, (int) (255 * Math.min(1, p.life * 2.8))));

            g.setColor(new Color(
                    p.color.getRed(),
                    p.color.getGreen(),
                    p.color.getBlue(),
                    alpha));

            g.fillOval(
                    (int) (p.x - p.size / 2),
                    (int) (p.y - p.size / 2),
                    (int) p.size,
                    (int) p.size);
        }
    }

    private void drawHud(Graphics2D g) {
        panel(g, 22, 20, 310, 98, new Color(9, 13, 32, 215));
        panel(g, WIDTH - 332, 20, 310, 98, new Color(9, 13, 32, 215));

        drawPlayerHud(g, playerOne, 40, 47);
        drawPlayerHud(g, playerTwo, WIDTH - 314, 47);

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(219, 231, 255));

        String distance = String.format(
                "DISTANCE  %04dm / %04dm",
                (int) Math.min(RACE_DISTANCE,
                        Math.max(playerOne.distance, playerTwo.distance)),
                (int) RACE_DISTANCE);

        centerText(g, distance, WIDTH / 2, 40);

        g.setColor(new Color(135, 149, 187));
        centerText(g,
                mode == Mode.COMPUTER
                        ? "SINGLE PLAYER // NEON CIRCUIT"
                        : "LOCAL VERSUS // NEON CIRCUIT",
                WIDTH / 2, 60);
    }

    private void drawPlayerHud(Graphics2D g, PlayerCar car, int x, int y) {
        g.setColor(car.primary);
        g.fillOval(x, y - 11, 9, 9);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString(car.name, x + 18, y);

        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        g.drawString(String.format("%03d KM/H", (int) (car.speed * 220)),
                x + 18, y + 23);

        g.setColor(new Color(87, 100, 139));
        g.fillRoundRect(x + 18, y + 34, 170, 7, 7, 7);

        g.setColor(car.boosting
                ? new Color(107, 240, 255)
                : car.primary);
        g.fillRoundRect(
                x + 18,
                y + 34,
                (int) (170 * Math.min(1, car.boost / 100)),
                7, 7, 7);

        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(new Color(161, 177, 213));
        g.drawString(car.boosting ? "BOOST" : "NITRO", x + 194, y + 41);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(4, 6, 19, 135));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(108, 229, 255, 75));
        g.setStroke(new BasicStroke(2));
        g.drawLine(74, 164, 320, 164);
        g.drawLine(WIDTH - 320, 164, WIDTH - 74, 164);

        g.setFont(new Font("SansSerif", Font.BOLD, 68));
        g.setColor(new Color(250, 247, 255));
        centerText(g, "SUPERCAR", WIDTH / 2, 146);

        g.setColor(new Color(96, 231, 255));
        centerText(g, "RACING CHALLENGE", WIDTH / 2, 208);

        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(new Color(165, 180, 224));
        centerText(g, "NEON CIRCUIT / 01", WIDTH / 2, 237);

        panel(g, 240, 300, 270, 140,
                mode == Mode.TWO_PLAYERS
                        ? new Color(33, 36, 77, 235)
                        : new Color(16, 20, 50, 235));

        panel(g, 590, 300, 270, 140,
                mode == Mode.COMPUTER
                        ? new Color(33, 36, 77, 235)
                        : new Color(16, 20, 50, 235));

        g.setFont(new Font("SansSerif", Font.BOLD, 21));
        g.setColor(new Color(255, 95, 146));
        centerText(g, "1", 270, 338);

        g.setColor(Color.WHITE);
        centerText(g, "TWO PLAYERS", 375, 350);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(170, 186, 222));
        centerText(g, "P1: W A S D     P2: ARROWS", 375, 380);

        g.setFont(new Font("SansSerif", Font.BOLD, 21));
        g.setColor(new Color(89, 224, 255));
        centerText(g, "2", 620, 338);

        g.setColor(Color.WHITE);
        centerText(g, "VS COMPUTER", 725, 350);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(170, 186, 222));
        centerText(g, "P1: W A S D     SMART AI RIVAL", 725, 380);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(255, 213, 113));
        centerText(g,
                "PRESS  1  OR  2  TO SELECT  •  ENTER TO LAUNCH",
                WIDTH / 2, 512);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(144, 157, 198));
        centerText(g,
                "A fast, neon-lit sprint through the midnight city.",
                WIDTH / 2, 546);
    }

    private void drawCountdown(Graphics2D g) {
        g.setColor(new Color(4, 6, 19, 90));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        String text = countdown > 2.7 ? "3"
                : countdown > 1.7 ? "2"
                : countdown > 0.7 ? "1"
                : "GO!";

        g.setFont(new Font("SansSerif", Font.BOLD,
                text.equals("GO!") ? 92 : 138));

        g.setColor(text.equals("GO!")
                ? new Color(93, 242, 255)
                : Color.WHITE);

        centerText(g, text, WIDTH / 2, 365);

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(new Color(196, 210, 250));
        centerText(g, "LIGHTS OUT — FIND YOUR LINE", WIDTH / 2, 424);
    }

    private void drawFinish(Graphics2D g) {
        g.setColor(new Color(4, 6, 19, 165));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        panel(g, 250, 220, 600, 275, new Color(10, 14, 37, 240));

        g.setFont(new Font("SansSerif", Font.BOLD, 46));
        g.setColor(new Color(250, 247, 255));
        centerText(g, "RACE COMPLETE", WIDTH / 2, 290);

        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(new Color(100, 230, 255));
        centerText(g, winnerText, WIDTH / 2, 344);

        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(new Color(173, 187, 225));
        centerText(g,
                String.format("P1  %04dm       %s  %04dm",
                        (int) playerOne.distance,
                        playerTwo.name,
                        (int) playerTwo.distance),
                WIDTH / 2, 388);

        g.setColor(new Color(255, 211, 114));
        centerText(g, "R  REPLAY        M  MAIN MENU", WIDTH / 2, 450);
    }

    private void panel(Graphics2D g, int x, int y, int w, int h, Color color) {
        g.setColor(color);
        g.fillRoundRect(x, y, w, h, 18, 18);

        g.setColor(new Color(111, 221, 255, 75));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(x, y, w, h, 18, 18);
    }

    private void centerText(Graphics2D g, String text, int x, int y) {
        int width = g.getFontMetrics().stringWidth(text);
        g.drawString(text, x - width / 2, y);
    }

    private int roadY(double z) {
        return 225 + (int) (Math.pow(z, 0.82) * 495);
    }

    private int roadCenter(double z) {
        double curve = Math.sin(roadScroll * 1.25 + z * 4.7) * 74 * z * z
                + Math.sin(roadScroll * 0.63 + z * 8.2) * 19 * z;

        return WIDTH / 2 + (int) curve;
    }

    private int roadHalfWidth(double z) {
        return 18 + (int) (Math.pow(z, 0.74) * 355);
    }

    private int roadX(double normalized, double z) {
        return roadCenter(z)
                + (int) (normalized * roadHalfWidth(z) * 0.72);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < keys.length) keys[code] = true;

        if (screen == Screen.MENU) {
            if (code == KeyEvent.VK_1) mode = Mode.TWO_PLAYERS;
            if (code == KeyEvent.VK_2) mode = Mode.COMPUTER;
            if (code == KeyEvent.VK_ENTER) beginRace(mode);
        } else if (screen == Screen.FINISHED) {
            if (code == KeyEvent.VK_R) beginRace(mode);
            if (code == KeyEvent.VK_M) screen = Screen.MENU;
        } else if ((screen == Screen.RACING || screen == Screen.COUNTDOWN)
                && code == KeyEvent.VK_M) {
            screen = Screen.MENU;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < keys.length) keys[code] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // KeyListener requires this method; keyPressed/keyReleased do the work.
    }

    private static class Star {
        final double x;
        final double y;
        final double size;
        final double twinkle;

        Star(double x, double y, double size, double twinkle) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.twinkle = twinkle;
        }
    }

    private static class Particle {
        double x, y, vx, vy, life, size;
        final Color color;

        Particle(double x, double y, double vx, double vy,
                 double life, Color color, double size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.color = color;
            this.size = size;
        }
    }

    private static class Opponent {
        double lane;
        double z;
        double wobble;
        final boolean dark;

        Opponent(double lane, double z, boolean dark) {
            this.lane = lane;
            this.z = z;
            this.dark = dark;
        }
    }

    private static class PlayerCar {
        final String name;
        final Color primary;
        final Color secondary;
        double x;
        double speed;
        double distance;
        double boost = 100;
        boolean boosting;
        double aiTimer;

        PlayerCar(String name, Color primary, Color secondary, double x) {
            this.name = name;
            this.primary = primary;
            this.secondary = secondary;
            this.x = x;
        }

        void update(double dt,
                    boolean accelerate,
                    boolean brake,
                    boolean left,
                    boolean right,
                    boolean boostKey,
                    PlayerCar other) {
            boolean steer = left || right;

            if (accelerate) speed += 0.95 * dt;
            else speed -= 0.35 * dt;

            if (brake) speed -= 1.1 * dt;

            boosting = boostKey && accelerate && boost > 0;

            if (boosting && boost > 0) {
                speed += 0.72 * dt;
                boost -= 17 * dt;
            } else {
                boost = Math.min(100, boost + 4.5 * dt);
            }

            if (!steer) x += Math.sin(distance * 0.0002) * 0.00008;
            if (left) x -= (0.95 + speed * 0.35) * dt;
            if (right) x += (0.95 + speed * 0.35) * dt;

            speed = Math.max(0.05, Math.min(1.25, speed));
            x = Math.max(-0.82, Math.min(0.82, x));
            distance += speed * 115 * dt;

            if (other != null
                    && Math.abs(x - other.x) < 0.1
                    && Math.abs(distance - other.distance) < 12) {
                speed *= 0.985;
            }
        }

        void updateComputer(double dt, PlayerCar rival, List<Opponent> traffic) {
            aiTimer += dt;
            double target = Math.sin(aiTimer * 0.45) * 0.42;

            for (Opponent car : traffic) {
                if (car.z > 0.02
                        && car.z < 0.28
                        && Math.abs(car.lane - x) < 0.18) {
                    target += car.lane < x ? 0.24 : -0.24;
                }
            }

            boolean accelerate = speed < 1.08
                    || Math.sin(aiTimer * 0.7) > -0.2;

            if (accelerate) speed += 0.66 * dt;
            else speed -= 0.25 * dt;

            if (target < x - 0.04) x -= 0.58 * dt;
            if (target > x + 0.04) x += 0.58 * dt;

            speed = Math.max(0.28, Math.min(1.12, speed));
            x = Math.max(-0.82, Math.min(0.82, x));
            distance += speed * 115 * dt;
            boost = Math.min(100, boost + 3.8 * dt);
            boosting = false;

            if (Math.abs(distance - rival.distance) > 175) {
                speed = Math.min(1.19, speed + 0.08);
            }
        }
    }
}