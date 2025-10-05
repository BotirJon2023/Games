import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class GolfResortManager {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Golf Resort Manager - Swing Edition");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setSize(1280, 800);
            f.setLocationRelativeTo(null);

            GamePanel panel = new GamePanel(96, 72, 16); // grid 96x72, 16px tile base size
            f.setContentPane(panel);
            f.setVisible(true);
            panel.requestFocusInWindow();
        });
    }
}

// Core game panel with update/render loop and input handling
class GamePanel extends JPanel implements ActionListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    // Loop timing
    private final Timer timer;
    private long lastNano;
    private double accumulator = 0;
    private final double step = 1.0 / 60.0; // 60 fps logic

    // World state
    private final World world;
    private final Camera camera;
    private final Random rng = new Random(42);

    // UI state and gameplay
    private BuildTool currentTool = BuildTool.FAIRWAY;
    private boolean paused = false;
    private boolean showGrid = true;
    private boolean showFPS = false;
    private boolean showHelp = true;
    private boolean muteEffects = false;

    private int selectedRotation = 0; // orientation for certain builds
    private int cash = 50000;
    private double reputation = 1.0; // 0-5 effective
    private double dayTime = 6.0; // 0..24
    private Weather weather = Weather.SUNNY;
    private double weatherTimer = 0;
    private double spawnTimer = 0;
    private double financeTimer = 0;
    private int dayCount = 1;
    private final Finance finance = new Finance();

    // Entities
    private final List<Golfer> golfers = new ArrayList<>();
    private final List<Cart> carts = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Cloud> clouds = new ArrayList<>();
    private final List<Staff> staff = new ArrayList<>();

    // UI layout metrics
    private final int sidePanelWidth = 222;
    private final int rightPanelWidth = 260;
    private final int topBarHeight = 38;

    // Inputs
    private int mouseX, mouseY;
    private boolean leftDown = false;
    private boolean rightDown = false;
    private int lastDragX, lastDragY;

    // Performance tracking
    private int frames = 0;
    private double fpsTime = 0;
    private double currentFPS = 0;

    // Hover/selection
    private int hoverTx = -1, hoverTy = -1;
    private String lastMessage = "Welcome! Build fairways and greens, connect paths, place a clubhouse, and attract golfers.";

    public GamePanel(int width, int height, int baseTileSize) {
        setBackground(new Color(30, 120, 48));
        setFocusable(true);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);

        world = new World(width, height);
        camera = new Camera(width, height, baseTileSize);

        generateInitialWorld();

        // Clouds
        for (int i = 0; i < 12; i++) {
            double x = rng.nextDouble() * (width * baseTileSize);
            double y = rng.nextDouble() * 400 + 50;
            double sp = 10 + rng.nextDouble() * 20;
            double sc = 0.8 + rng.nextDouble() * 1.8;
            clouds.add(new Cloud(x, y, sp, sc, new Color(255, 255, 255, 200)));
        }

        // Staff
        staff.add(new Staff(StaffType.GROUNDSKEEPER, world.width / 2.0, world.height / 2.0, 24));
        staff.add(new Staff(StaffType.JANITOR, world.width / 2.0 + 4, world.height / 2.0 - 2, 20));

        timer = new Timer(16, this);
        timer.start();
        lastNano = System.nanoTime();
    }

    private void generateInitialWorld() {
        // Basic grass with a small blueprint path and green to get started.
        for (int y = 0; y < world.height; y++) {
            for (int x = 0; x < world.width; x++) {
                Tile t = world.get(x, y);
                t.type = TileType.GRASS;
                t.quality = 0.8;
                t.wear = 0.0;
                t.decor = 0;
            }
        }

        // Place starter fairway and green
        for (int x = 30; x < 60; x++) {
            for (int y = 30; y < 35; y++) {
                Tile t = world.get(x, y);
                t.type = TileType.FAIRWAY;
                t.quality = 0.85;
            }
        }
        for (int x = 58; x < 64; x++) {
            for (int y = 31; y < 34; y++) {
                Tile t = world.get(x, y);
                t.type = TileType.GREEN;
                t.quality = 0.92;
                t.flag = true;
            }
        }
        for (int x = 24; x < 31; x++) {
            Tile t = world.get(x, 33);
            t.type = TileType.PATH;
        }
        // Clubhouse near the path start
        world.get(24, 33).type = TileType.CLUBHOUSE;
        world.get(25, 33).type = TileType.PATH;
        world.get(26, 33).type = TileType.PATH;
        lastMessage = "Build fairway to tee off, green with flag, connect with paths, and watch golfers arrive!";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double delta = (now - lastNano) / 1_000_000_000.0;
        lastNano = now;

        if (!paused) {
            accumulator += delta;
            while (accumulator >= step) {
                update(step);
                accumulator -= step;
            }
        }
        repaint();

        // FPS
        fpsTime += delta;
        frames++;
        if (fpsTime >= 1.0) {
            currentFPS = frames / fpsTime;
            frames = 0;
            fpsTime = 0;
        }
    }

    private void update(double dt) {
        // Time of day
        dayTime += dt * 0.25 * (weather == Weather.RAIN ? 0.8 : 1.0);
        if (dayTime >= 24) {
            dayTime -= 24;
            dayCount++;
            endOfDay();
        }

        // Weather cycle
        weatherTimer += dt;
        if (weatherTimer >= 25) {
            weatherTimer = 0;
            double r = rng.nextDouble();
            if (r < 0.6) weather = Weather.SUNNY;
            else if (r < 0.84) weather = Weather.CLOUDY;
            else weather = Weather.RAIN;
            lastMessage = "Weather changed: " + weather;
        }

        // Spawn golfers
        double spawnRate = 0.45 + reputation * 0.15;
        if (weather == Weather.RAIN) spawnRate *= 0.5;
        if (isNight()) spawnRate *= 0.25;
        spawnTimer += dt * spawnRate;
        if (spawnTimer >= 1.0) {
            spawnGolfer();
            spawnTimer = 0;
        }

        // Update clouds
        for (Cloud c : clouds) {
            c.x += c.speed * dt;
            if (c.x > world.width * camera.baseTileSize + 200) c.x = -200;
        }

        // Update golfers and carts
        for (Iterator<Golfer> it = golfers.iterator(); it.hasNext(); ) {
            Golfer g = it.next();
            g.update(world, dt, weather, particles, carts, rng);
            if (g.done && g.ageOutTimer > 2.0) {
                it.remove();
            }
        }
        for (Iterator<Cart> it2 = carts.iterator(); it2.hasNext(); ) {
            Cart c = it2.next();
            c.update(dt);
            if (c.dead) it2.remove();
        }

        // Update staff and maintenance (reduce wear)
        for (Staff s : staff) {
            s.update(world, dt, rng);
        }

        // Particles
        for (Iterator<Particle> itp = particles.iterator(); itp.hasNext(); ) {
            Particle p = itp.next();
            p.update(dt);
            if (p.life <= 0) itp.remove();
        }

        // Finances: periodic income/expenses
        financeTimer += dt;
        if (financeTimer >= 5.0) {
            financeTimer = 0;
            // Ongoing maintenance costs
            int staffCost = 0;
            for (Staff s : staff) staffCost += s.wage;
            spend(staffCost / 24); // approximate per "period"
            finance.recordExpense("Staff wages", staffCost / 24);
            // Watering costs during day, more in sunny weather
            if (!isNight()) {
                int waterCost = weather == Weather.SUNNY ? 180 : weather == Weather.CLOUDY ? 90 : 40;
                spend(waterCost / 3);
                finance.recordExpense("Upkeep", waterCost / 3);
                // Slight quality recovery from watering
                for (int i = 0; i < 60; i++) {
                    int x = rng.nextInt(world.width);
                    int y = rng.nextInt(world.height);
                    Tile t = world.get(x, y);
                    if (t.type == TileType.FAIRWAY || t.type == TileType.GREEN) {
                        t.quality = Math.min(1.0, t.quality + (weather == Weather.RAIN ? 0.004 : 0.002));
                    }
                }
            }
        }

        // Camera inertia panning easing (optional)
        camera.update(dt);
    }

    private void endOfDay() {
        // Daily summary, adjust reputation based on average satisfaction
        if (finance.dailyPlayers > 0) {
            double avg = finance.dailySatisfactionSum / finance.dailyPlayers;
            reputation = Math.max(0, Math.min(5.0, 0.9 * reputation + 0.1 * (avg * 5.0)));
            lastMessage = "Day " + dayCount + " ended. Reputation: " + String.format("%.2f", reputation);
        }
        finance.endOfDay(dayCount);
        // Minor wear increase due to overnight dryness unless rain
        if (weather == Weather.SUNNY) {
            for (int i = 0; i < 400; i++) {
                int x = rng.nextInt(world.width);
                int y = rng.nextInt(world.height);
                Tile t = world.get(x, y);
                if (t.type == TileType.FAIRWAY || t.type == TileType.GREEN) {
                    t.wear = Math.min(1.0, t.wear + 0.01);
                    t.quality = Math.max(0, t.quality - 0.004);
                }
            }
        }
    }

    private boolean isNight() {
        return (dayTime < 6.0 || dayTime > 19.5);
    }

    private void spawnGolfer() {
        Point clubhouse = world.findAny(TileType.CLUBHOUSE);
        if (clubhouse == null) {
            // No clubhouse yet; few golfers come, maybe hikers
            clubhouse = new Point(world.width / 2, world.height / 2);
        }
        double gx = clubhouse.x + rng.nextDouble() * 2 - 0.5;
        double gy = clubhouse.y + rng.nextDouble() * 2 - 0.5;
        boolean premium = rng.nextDouble() < 0.15 + 0.05 * (reputation / 5.0);
        double skill = 0.45 + 0.5 * rng.nextDouble(); // 0.45..0.95
        Golfer g = new Golfer(gx, gy, skill, premium);
        g.chooseHole(world, rng);
        golfers.add(g);

        // Income on arrival (green fee)
        int fee = 80 + (int)(reputation * 40) + (premium ? 40 : 0);
        earn(fee);
        finance.recordIncome("Green fee", fee);
        finance.dailyPlayers++;
    }

    private void earn(int amount) {
        cash += amount;
    }

    private void spend(int amount) {
        cash -= amount;
        if (cash < -100000) cash = -100000; // floor to avoid overflow
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D gg = (Graphics2D) g.create();
        gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Backdrop based on day/night and weather
        drawSkyBackdrop(gg, w, h);

        // Camera transform
        AffineTransform old = gg.getTransform();
        applyWorldTransform(gg);

        // Draw ground tiles (only visible area)
        drawTiles(gg);

        // Draw water sparkle and sand noise
        drawTileOverlays(gg);

        // Draw flags and holes
        drawHoles(gg);

        // Draw carts behind golfers (if needed for layering)
        for (Cart c : carts) {
            c.draw(gg, camera);
        }

        // Draw golfers
        for (Golfer gl : golfers) {
            gl.draw(gg, camera);
        }

        // Draw staff
        for (Staff s : staff) {
            s.draw(gg, camera);
        }

        // Particles
        for (Particle p : particles) {
            p.draw(gg, camera);
        }

        // Grid and hover
        if (showGrid) drawGrid(gg);
        drawHover(gg);

        // Reset transform for UI
        gg.setTransform(old);

        // Clouds
        drawClouds(gg, w, h);

        // UI overlays
        drawTopBar(gg, w);
        drawLeftBar(gg);
        drawRightPanel(gg);
        drawHelpOverlay(gg, w, h);

        gg.dispose();
    }

    private void drawSkyBackdrop(Graphics2D g, int w, int h) {
        // Day-night gradient
        float t = (float) ((Math.cos((dayTime - 12.0) / 24.0 * Math.PI * 2) + 1) / 2.0);
        // t=0 darkest night, t=1 noon. Use two colors based on weather
        Color top, bottom;
        if (weather == Weather.SUNNY) {
            top = mix(new Color(10, 20, 60), new Color(110, 180, 255), t);
            bottom = mix(new Color(20, 30, 60), new Color(160, 210, 255), t);
        } else if (weather == Weather.CLOUDY) {
            top = mix(new Color(20, 30, 50), new Color(130, 160, 190), t);
            bottom = mix(new Color(30, 40, 60), new Color(150, 170, 200), t);
        } else {
            top = mix(new Color(10, 10, 20), new Color(80, 100, 140), t);
            bottom = mix(new Color(16, 16, 32), new Color(100, 110, 150), t);
        }
        GradientPaint gp = new GradientPaint(0, 0, top, 0, h, bottom);
        g.setPaint(gp);
        g.fillRect(0, 0, w, h);
    }

    private Color mix(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int)(a.getRed() * (1 - t) + b.getRed() * t);
        int g = (int)(a.getGreen() * (1 - t) + b.getGreen() * t);
        int bl = (int)(a.getBlue() * (1 - t) + b.getBlue() * t);
        return new Color(r, g, bl);
    }

    private void applyWorldTransform(Graphics2D g) {
        g.translate(sidePanelWidth, topBarHeight);
        g.scale(camera.zoom, camera.zoom);
        g.translate(-camera.x, -camera.y);
    }

    private void drawTiles(Graphics2D g) {
        int px0 = (int) Math.floor(camera.x / camera.tileSize);
        int py0 = (int) Math.floor(camera.y / camera.tileSize);
        int px1 = (int) Math.ceil((camera.x + (getWidth() - sidePanelWidth) / camera.zoom) / camera.tileSize) + 1;
        int py1 = (int) Math.ceil((camera.y + (getHeight() - topBarHeight) / camera.zoom) / camera.tileSize) + 1;
        px0 = Math.max(0, px0);
        py0 = Math.max(0, py0);
        px1 = Math.min(world.width, px1);
        py1 = Math.min(world.height, py1);

        for (int y = py0; y < py1; y++) {
            for (int x = px0; x < px1; x++) {
                Tile t = world.get(x, y);
                int ts = camera.tileSize;

                Color c;
                switch (t.type) {
                    case GRASS:
                        c = new Color(44, (int)(120 + 30 * t.quality), 48);
                        break;
                    case FAIRWAY:
                        c = new Color(60, (int)(155 + 60 * t.quality), 60);
                        break;
                    case GREEN:
                        c = new Color(80, (int)(200 + 55 * t.quality), 90);
                        break;
                    case SAND:
                        c = new Color(225, 208, 130);
                        break;
                    case WATER:
                        float wr = 0.8f + 0.15f * (float)Math.sin((x + y + System.currentTimeMillis() * 0.001) * 0.5);
                        c = new Color((int)(20 * wr), (int)(80 * wr), (int)(140 * wr));
                        break;
                    case TREE:
                        c = new Color(30, 90, 40);
                        break;
                    case PATH:
                        c = new Color(160, 140, 120);
                        break;
                    case CLUBHOUSE:
                        c = new Color(140, 120, 100);
                        break;
                    case DECOR:
                        c = new Color(110, 110, 110);
                        break;
                    default:
                        c = Color.MAGENTA;
                }
                // Slight darkening at night
                if (isNight()) {
                    c = c.darker().darker();
                }

                g.setColor(c);
                g.fillRect(x * ts, y * ts, ts, ts);

                // Wear overlay
                if ((t.type == TileType.FAIRWAY || t.type == TileType.GREEN) && t.wear > 0.05) {
                    int a = (int) (Math.min(1.0, t.wear) * 110);
                    g.setColor(new Color(100, 70, 40, a));
                    g.fillRect(x * ts, y * ts, ts, ts);
                }
            }
        }
    }

    private void drawTileOverlays(Graphics2D g) {
        // Animate water shimmer, sand texture hints, and decor items
        int px0 = (int) Math.floor(camera.x / camera.tileSize);
        int py0 = (int) Math.floor(camera.y / camera.tileSize);
        int px1 = (int) Math.ceil((camera.x + (getWidth() - sidePanelWidth) / camera.zoom) / camera.tileSize) + 1;
        int py1 = (int) Math.ceil((camera.y + (getHeight() - topBarHeight) / camera.zoom) / camera.tileSize) + 1;
        px0 = Math.max(0, px0);
        py0 = Math.max(0, py0);
        px1 = Math.min(world.width, px1);
        py1 = Math.min(world.height, py1);

        int ts = camera.tileSize;
        double t = System.currentTimeMillis() * 0.001;

        for (int y = py0; y < py1; y++) {
            for (int x = px0; x < px1; x++) {
                Tile tile = world.get(x, y);
                int sx = x * ts;
                int sy = y * ts;

                if (tile.type == TileType.WATER) {
                    // shimmer lines
                    g.setColor(new Color(255, 255, 255, 70));
                    for (int i = 0; i < 4; i++) {
                        int yy = sy + (int)((i * 4 + (t * 20) % 8)) % ts;
                        g.drawLine(sx, yy, sx + ts, yy);
                    }
                } else if (tile.type == TileType.SAND) {
                    g.setColor(new Color(200, 180, 120, 70));
                    g.drawLine(sx, sy, sx + ts, sy + ts);
                    g.drawLine(sx + ts, sy, sx, sy + ts);
                }

                if (tile.decor > 0) {
                    drawDecor(g, sx, sy, ts, tile.decor);
                }

                if (tile.type == TileType.CLUBHOUSE) {
                    drawClubhouse(g, sx, sy, ts, selectedRotation);
                }
            }
        }
    }

    private void drawDecor(Graphics2D g, int x, int y, int ts, int decorType) {
        // Simple decorations
        switch (decorType) {
            case 1: // bench
                g.setColor(new Color(110, 80, 50));
                g.fillRect(x + ts / 4, y + ts / 2, ts / 2, ts / 6);
                g.setColor(new Color(80, 60, 40));
                g.fillRect(x + ts / 4 + 2, y + ts / 2 - ts / 8, ts / 2 - 4, ts / 8);
                break;
            case 2: // flower
                g.setColor(Color.GREEN.darker());
                g.fillOval(x + ts / 3, y + ts / 3, ts / 3, ts / 3);
                g.setColor(new Color(200, 60, 60));
                g.fillOval(x + ts / 3 + 3, y + ts / 3 + 3, ts / 5, ts / 5);
                break;
            case 3: // lamp (glows at night)
                g.setColor(new Color(80, 80, 80));
                g.fillRect(x + ts / 2 - 2, y + ts / 6, 4, ts / 2);
                g.setColor(new Color(245, 230, 140, isNight() ? 200 : 80));
                g.fillOval(x + ts / 2 - 6, y + ts / 6 - 8, 12, 12);
                break;
            default:
                g.setColor(new Color(140, 140, 140, 160));
                g.fillRect(x + ts / 3, y + ts / 3, ts / 3, ts / 3);
                break;
        }
    }

    private void drawClubhouse(Graphics2D g, int x, int y, int ts, int rot) {
        // simple building silhouette
        g.setColor(new Color(150, 130, 110));
        g.fillRect(x + ts / 8, y + ts / 8, (int)(ts * 0.75), (int)(ts * 0.75));
        g.setColor(new Color(90, 70, 60));
        g.fillRect(x + ts / 3, y + ts / 2, ts / 3, ts / 3);
        // roof
        g.setColor(new Color(80, 60, 50));
        Path2D roof = new Path2D.Double();
        roof.moveTo(x + ts / 8, y + ts / 8);
        roof.lineTo(x + ts / 2, y);
        roof.lineTo(x + ts - ts / 8, y + ts / 8);
        roof.closePath();
        g.fill(roof);
        // flag if day
        if (!isNight()) {
            g.setColor(new Color(200, 180, 100));
            g.fillRect(x + ts - ts / 6, y + ts / 8, ts / 16, ts / 2);
        }
    }

    private void drawHoles(Graphics2D g) {
        // Flags on green
        int ts = camera.tileSize;
        double t = System.currentTimeMillis() * 0.002;
        for (int y = 0; y < world.height; y++) {
            for (int x = 0; x < world.width; x++) {
                Tile tile = world.get(x, y);
                if (tile.type == TileType.GREEN && tile.flag) {
                    int px = x * ts + ts / 2;
                    int py = y * ts + ts / 2;
                    g.setColor(new Color(120, 120, 120));
                    g.fillOval(px - 6, py + 6, 12, 6);
                    g.setColor(new Color(180, 170, 160));
                    g.fillRect(px - 1, py - 18, 2, 18);
                    int len = 12;
                    int wave = (int)(Math.sin(t * 4 + x * 0.6 + y * 0.4) * 3);
                    g.setColor(new Color(230, 40, 40));
                    Polygon flag = new Polygon();
                    flag.addPoint(px, py - 18);
                    flag.addPoint(px + len, py - 18 + 5 + wave);
                    flag.addPoint(px, py - 10);
                    g.fillPolygon(flag);
                }
            }
        }
    }

    private void drawGrid(Graphics2D g) {
        int ts = camera.tileSize;
        int px0 = 0;
        int py0 = 0;
        int px1 = world.width;
        int py1 = world.height;
        g.setColor(new Color(0, 0, 0, 35));
        for (int x = px0; x <= px1; x++) {
            int sx = x * ts;
            g.drawLine(sx, py0 * ts, sx, py1 * ts);
        }
        for (int y = py0; y <= py1; y++) {
            int sy = y * ts;
            g.drawLine(px0 * ts, sy, px1 * ts, sy);
        }
    }

    private void drawHover(Graphics2D g) {
        if (hoverTx >= 0 && hoverTy >= 0) {
            int ts = camera.tileSize;
            int sx = hoverTx * ts;
            int sy = hoverTy * ts;
            g.setColor(new Color(255, 255, 255, 70));
            g.fillRect(sx, sy, ts, ts);
            // show cost preview
            int cost = getToolCost(currentTool);
            if (cost != 0) {
                g.setColor(new Color(cost > 0 ? 0 : 180, cost > 0 ? 180 : 0, 0, 160));
                g.setFont(g.getFont().deriveFont(Font.BOLD, 10));
                String str = cost > 0 ? "-" + cost : "+" + (-cost);
                g.drawString(str, sx + 4, sy + 12);
            }
        }
    }

    private void drawClouds(Graphics2D g, int w, int h) {
        for (Cloud c : clouds) {
            c.draw(g);
        }
        // Rain overlay if raining
        if (weather == Weather.RAIN) {
            g.setColor(new Color(210, 210, 255, 60));
            int lines = getWidth() / 8;
            double t = System.currentTimeMillis() * 0.003;
            for (int i = 0; i < lines; i++) {
                int x = (int)((i * 8 + t * 80) % (w + 16)) - 16;
                g.drawLine(x, topBarHeight, x + 8, h);
            }
        }
    }

    private void drawTopBar(Graphics2D g, int w) {
        g.setColor(new Color(18, 18, 18, 180));
        g.fillRect(0, 0, w, topBarHeight);

        int x = 10;
        int y = 24;
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14));

        g.drawString("Cash: $" + cash, x, y);
        x += 160;
        g.drawString("Reputation: " + String.format("%.2f", reputation), x, y);
        x += 180;
        g.drawString("Day " + dayCount + " - " + timeString(), x, y);
        x += 210;
        g.drawString("Weather: " + weather, x, y);

        if (showFPS) {
            x += 150;
            g.drawString(String.format("FPS: %.1f", currentFPS), x, y);
        }
    }

    private String timeString() {
        int hh = (int)Math.floor(dayTime);
        int mm = (int)((dayTime - hh) * 60);
        return String.format("%02d:%02d", hh, mm);
    }

    private void drawLeftBar(Graphics2D g) {
        g.setColor(new Color(22, 24, 24, 210));
        g.fillRect(0, topBarHeight, sidePanelWidth, getHeight() - topBarHeight);

        int x = 10;
        int y = topBarHeight + 14;
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14));
        g.drawString("Build Tools", x, y);
        y += 8;

        // Buttons
        int btnW = sidePanelWidth - 20;
        int btnH = 34;
        y += 10;
        for (BuildTool tool : BuildTool.values()) {
            Rectangle r = new Rectangle(10, y, btnW, btnH);
            boolean hover = r.contains(mouseX, mouseY);
            boolean selected = (tool == currentTool);
            g.setColor(selected ? new Color(40, 100, 160) : hover ? new Color(60, 60, 60) : new Color(40, 40, 40));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 13));
            String label = tool.label + "  $" + getToolCost(tool);
            g.drawString(label, r.x + 10, r.y + 22);
            y += btnH + 8;
        }

        y += 8;
        g.setColor(new Color(255, 255, 255, 220));
        g.drawString("Rotate: R | Grid: G | Pause: Space", 10, y);
        y += 18;
        g.drawString("Pan: Right-click drag | Zoom: Wheel", 10, y);
    }

    private void drawRightPanel(Graphics2D g) {
        int x0 = getWidth() - rightPanelWidth;
        g.setColor(new Color(22, 24, 24, 210));
        g.fillRect(x0, topBarHeight, rightPanelWidth, getHeight() - topBarHeight);

        int x = x0 + 10;
        int y = topBarHeight + 14;
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14));
        g.drawString("Resort Info", x, y);

        y += 24;
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 13));
        g.drawString("Guests on course: " + golfers.size(), x, y);
        y += 18;
        g.drawString("Carts in use: " + carts.size(), x, y);
        y += 18;
        g.drawString("Staff: " + staff.size(), x, y);
        y += 18;

        y += 6;
        g.setFont(g.getFont().deriveFont(Font.BOLD, 13));
        g.drawString("Recent Transactions:", x, y);
        y += 18;

        g.setFont(g.getFont().deriveFont(Font.PLAIN, 12));
        int shown = 0;
        for (int i = finance.transactions.size() - 1; i >= 0 && shown < 8; i--) {
            Finance.Transaction tr = finance.transactions.get(i);
            Color c = tr.amount >= 0 ? new Color(130, 220, 130) : new Color(220, 130, 130);
            g.setColor(c);
            String s = (tr.amount >= 0 ? "+$" : "-$") + Math.abs(tr.amount) + " " + tr.label;
            g.drawString(s, x, y);
            y += 16;
            shown++;
        }

        y += 12;
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 13));
        g.drawString("Messages:", x, y);
        y += 16;
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 12));
        drawWrapped(g, lastMessage, x, y, rightPanelWidth - 20);
    }

    private void drawHelpOverlay(Graphics2D g, int w, int h) {
        if (!showHelp) return;
        String[] lines = new String[]{
                "Hints:",
                "- Place a clubhouse to attract golfers.",
                "- Build fairways, greens (with flags), and connect with paths.",
                "- Golfers spawn more in day and good weather.",
                "- Maintenance is handled by staff; they reduce wear.",
                "- Reputation affects income and spawn rate.",
                "- Watch your cash! Each build has a cost."
        };
        int x = sidePanelWidth + 12;
        int y = getHeight() - 10 - lines.length * 16;
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(x - 10, y - 18, 560, 18 + lines.length * 16, 8, 8);
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 13));
        for (String s : lines) {
            g.drawString(s, x, y);
            y += 16;
        }
    }

    private void drawWrapped(Graphics2D g, String text, int x, int y, int width) {
        if (text == null) return;
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        FontMetrics fm = g.getFontMetrics();
        for (String w : words) {
            String tmp = line + (line.length() == 0 ? "" : " ") + w;
            if (fm.stringWidth(tmp) > width) {
                g.drawString(line.toString(), x, y);
                y += 14;
                line = new StringBuilder(w);
            } else {
                line = new StringBuilder(tmp);
            }
        }
        if (line.length() > 0) {
            g.drawString(line.toString(), x, y);
        }
    }

    private int getToolCost(BuildTool tool) {
        switch (tool) {
            case GRASS: return 5;
            case FAIRWAY: return 15;
            case GREEN: return 30;
            case SAND: return 12;
            case WATER: return 25;
            case TREE: return 6;
            case PATH: return 8;
            case CLUBHOUSE: return 300;
            case DECOR: return 10;
            default: return 10;
        }
    }

    private void buildAt(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= world.width || ty >= world.height) return;
        Tile tile = world.get(tx, ty);

        int cost = getToolCost(currentTool);
        if (cash < cost) {
            lastMessage = "Not enough cash for " + currentTool.label + " ($" + cost + ")";
            return;
        }

        boolean changed = false;
        switch (currentTool) {
            case GRASS:
                tile.type = TileType.GRASS;
                tile.flag = false;
                tile.decor = 0;
                tile.quality = 0.8;
                tile.wear = Math.max(0, tile.wear - 0.2);
                changed = true;
                break;
            case FAIRWAY:
                tile.type = TileType.FAIRWAY;
                tile.flag = false;
                tile.decor = 0;
                tile.quality = 0.9;
                changed = true;
                break;
            case GREEN:
                tile.type = TileType.GREEN;
                tile.quality = 0.95;
                tile.flag = true;
                tile.decor = 0;
                changed = true;
                break;
            case SAND:
                tile.type = TileType.SAND;
                tile.flag = false;
                tile.decor = 0;
                tile.quality = 0.7;
                changed = true;
                break;
            case WATER:
                tile.type = TileType.WATER;
                tile.flag = false;
                tile.decor = 0;
                tile.quality = 1.0;
                changed = true;
                break;
            case TREE:
                tile.type = TileType.TREE;
                tile.flag = false;
                tile.quality = 0.9;
                tile.decor = 0;
                changed = true;
                break;
            case PATH:
                tile.type = TileType.PATH;
                tile.flag = false;
                changed = true;
                break;
            case CLUBHOUSE:
                tile.type = TileType.CLUBHOUSE;
                tile.flag = false;
                changed = true;
                break;
            case DECOR:
                tile.decor = 1 + rng.nextInt(3);
                changed = true;
                break;
        }

        if (changed) {
            spend(cost);
            finance.recordExpense("Build " + currentTool.label, cost);
            // Add subtle effect
            if (!muteEffects) {
                for (int i = 0; i < 6; i++) {
                    double px = tx + rng.nextDouble();
                    double py = ty + rng.nextDouble();
                    particles.add(Particle.makeBurst(px, py, currentTool));
                }
            }
        }
    }

    private Point screenToTile(int sx, int sy) {
        double x = (sx - sidePanelWidth) / camera.zoom + camera.x;
        double y = (sy - topBarHeight) / camera.zoom + camera.y;
        int tx = (int) Math.floor(x / camera.tileSize);
        int ty = (int) Math.floor(y / camera.tileSize);
        return new Point(tx, ty);
    }

    // Input handlers
    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        mouseX = e.getX();
        mouseY = e.getY();
        if (SwingUtilities.isLeftMouseButton(e)) {
            leftDown = true;
            Point p = screenToTile(mouseX, mouseY);
            if (mouseX < sidePanelWidth) {
                // Tool selection
                int y = topBarHeight + 14 + 10 + 8; // alignment with drawLeftBar
                int btnH = 34;
                for (BuildTool tool : BuildTool.values()) {
                    Rectangle r = new Rectangle(10, y, sidePanelWidth - 20, btnH);
                    if (r.contains(mouseX, mouseY)) {
                        currentTool = tool;
                        lastMessage = "Selected tool: " + tool.label;
                        break;
                    }
                    y += btnH + 8;
                }
            } else if (mouseX > getWidth() - rightPanelWidth) {
                // right panel interactions (future)
            } else {
                buildAt(p.x, p.y);
            }
        } else if (SwingUtilities.isRightMouseButton(e)) {
            rightDown = true;
            lastDragX = mouseX;
            lastDragY = mouseY;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) leftDown = false;
        if (SwingUtilities.isRightMouseButton(e)) rightDown = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {
        hoverTx = hoverTy = -1;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (leftDown) {
            // Build while dragging
            if (mouseX >= sidePanelWidth && mouseX <= getWidth() - rightPanelWidth) {
                Point p = screenToTile(mouseX, mouseY);
                if (p.x != hoverTx || p.y != hoverTy) {
                    buildAt(p.x, p.y);
                }
            }
        }
        if (rightDown) {
            int dx = mouseX - lastDragX;
            int dy = mouseY - lastDragY;
            camera.x -= dx / camera.zoom;
            camera.y -= dy / camera.zoom;
            lastDragX = mouseX;
            lastDragY = mouseY;
        }
        Point p = screenToTile(mouseX, mouseY);
        hoverTx = p.x;
        hoverTy = p.y;
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        Point p = screenToTile(mouseX, mouseY);
        hoverTx = p.x;
        hoverTy = p.y;
        repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int rot = e.getWheelRotation();
        double mx = (mouseX - sidePanelWidth) / camera.zoom + camera.x;
        double my = (mouseY - topBarHeight) / camera.zoom + camera.y;
        if (rot < 0) camera.zoomAt(1.1, mx, my);
        else camera.zoomAt(1 / 1.1, mx, my);
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE:
                paused = !paused;
                lastMessage = paused ? "Paused" : "Unpaused";
                break;
            case KeyEvent.VK_G:
                showGrid = !showGrid;
                break;
            case KeyEvent.VK_F:
                showFPS = !showFPS;
                break;
            case KeyEvent.VK_H:
                showHelp = !showHelp;
                break;
            case KeyEvent.VK_R:
                selectedRotation = (selectedRotation + 1) % 4;
                lastMessage = "Rotation: " + selectedRotation;
                break;
            case KeyEvent.VK_M:
                muteEffects = !muteEffects;
                lastMessage = muteEffects ? "Effects muted" : "Effects enabled";
                break;
            case KeyEvent.VK_1: currentTool = BuildTool.FAIRWAY; break;
            case KeyEvent.VK_2: currentTool = BuildTool.GREEN; break;
            case KeyEvent.VK_3: currentTool = BuildTool.SAND; break;
            case KeyEvent.VK_4: currentTool = BuildTool.WATER; break;
            case KeyEvent.VK_5: currentTool = BuildTool.TREE; break;
            case KeyEvent.VK_6: currentTool = BuildTool.PATH; break;
            case KeyEvent.VK_7: currentTool = BuildTool.CLUBHOUSE; break;
            case KeyEvent.VK_8: currentTool = BuildTool.DECOR; break;
        }
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}

// World and tiles

class World {
    final int width, height;
    final Tile[][] tiles;

    World(int w, int h) {
        this.width = w;
        this.height = h;
        this.tiles = new Tile[w][h];
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                tiles[x][y] = new Tile();
    }

    Tile get(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return new Tile();
        return tiles[x][y];
    }

    Point findAny(TileType type) {
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (tiles[x][y].type == type)
                    return new Point(x, y);
        return null;
    }

    // Helper: find nearest tile of specified type to a point
    Point findNearest(int sx, int sy, TileType type, int maxRadius) {
        int bestD = Integer.MAX_VALUE;
        Point best = null;
        for (int y = Math.max(0, sy - maxRadius); y < Math.min(height, sy + maxRadius + 1); y++) {
            for (int x = Math.max(0, sx - maxRadius); x < Math.min(width, sx + maxRadius + 1); x++) {
                if (tiles[x][y].type == type) {
                    int dx = x - sx;
                    int dy = y - sy;
                    int d = dx * dx + dy * dy;
                    if (d < bestD) {
                        bestD = d;
                        best = new Point(x, y);
                    }
                }
            }
        }
        return best;
    }
}

class Tile {
    TileType type = TileType.GRASS;
    double quality = 0.8;
    double wear = 0;
    boolean flag = false;
    int decor = 0;
}

enum TileType {
    GRASS, FAIRWAY, GREEN, SAND, WATER, TREE, PATH, CLUBHOUSE, DECOR
}

enum BuildTool {
    GRASS("Grass"),
    FAIRWAY("Fairway"),
    GREEN("Green + Flag"),
    SAND("Bunker"),
    WATER("Water"),
    TREE("Tree"),
    PATH("Path"),
    CLUBHOUSE("Clubhouse"),
    DECOR("Decor");

    final String label;
    BuildTool(String label) {
        this.label = label;
    }
}

enum Weather {
    SUNNY, CLOUDY, RAIN
}

// Camera for pan/zoom
class Camera {
    double x = 0, y = 0;
    double zoom = 1.0;
    final int baseTileSize;
    int tileSize;

    double vx = 0, vy = 0;

    final int worldW, worldH;

    Camera(int worldW, int worldH, int baseTile) {
        this.worldW = worldW;
        this.worldH = worldH;
        this.baseTileSize = baseTile;
        this.tileSize = baseTileSize;
        // Start centered
        x = worldW * baseTile / 2.0 - 400;
        y = worldH * baseTile / 2.0 - 300;
        clamp();
    }

    void update(double dt) {
        // friction
        vx *= 0.9;
        vy *= 0.9;
        x += vx * dt;
        y += vy * dt;
        clamp();
    }

    void zoomAt(double factor, double worldX, double worldY) {
        double oldZoom = zoom;
        zoom *= factor;
        if (zoom < 0.4) zoom = 0.4;
        if (zoom > 3.0) zoom = 3.0;
        tileSize = (int) Math.max(8, Math.round(baseTileSize * zoom));
        // keep focus point stable
        x = worldX - (worldX - x) * (oldZoom / zoom);
        y = worldY - (worldY - y) * (oldZoom / zoom);
        clamp();
    }

    void clamp() {
        if (x < -200) x = -200;
        if (y < -200) y = -200;
        double maxX = worldW * tileSize + 200;
        double maxY = worldH * tileSize + 200;
        if (x > maxX) x = maxX;
        if (y > maxY) y = maxY;
    }
}

// Entities

class Golfer {
    double x, y;
    double speed = 1.5;
    double skill; // 0..1
    boolean premium;
    double satisfaction = 0.7;
    double patience = 1.0; // decreases when stuck in water/sand
    double swingCooldown = 0.0;
    boolean aiming = false;
    boolean done = false;
    double ageOutTimer = 0.0;

    int targetX = -1, targetY = -1;

    int steps = 0;
    Color shirt;

    Golfer(double x, double y, double skill, boolean premium) {
        this.x = x;
        this.y = y;
        this.skill = skill;
        this.premium = premium;
        this.speed = 1.4 + skill * 1.2 + (premium ? 0.2 : 0.0);
        this.shirt = new Color(140 + (int)(Math.random() * 115), 80 + (int)(Math.random() * 100), 60 + (int)(Math.random() * 100));
    }

    void chooseHole(World world, Random rng) {
        // Select nearest green with flag as target; nearest path as tee
        Point cp = world.findAny(TileType.CLUBHOUSE);
        if (cp == null) {
            // fallback: use any green
            cp = new Point((int)x, (int)y);
        }
        Point g = world.findNearest(cp.x, cp.y, TileType.GREEN, 60);
        if (g != null) {
            targetX = g.x;
            targetY = g.y;
        } else {
            // roam random
            targetX = Math.min(world.width - 2, Math.max(1, (int)(x + rng.nextInt(12) - 6)));
            targetY = Math.min(world.height - 2, Math.max(1, (int)(y + rng.nextInt(12) - 6)));
        }
    }

    void update(World world, double dt, Weather weather, List<Particle> particles, List<Cart> carts, Random rng) {
        if (done) {
            ageOutTimer += dt;
            return;
        }

        swingCooldown -= dt;
        if (swingCooldown < 0) swingCooldown = 0;

        // Environmental effects
        if (weather == Weather.RAIN) {
            speed *= 0.999; // slightly slower accumulation
            satisfaction -= 0.0003;
        } else {
            satisfaction += 0.0003;
        }
        satisfaction = Math.max(0, Math.min(1, satisfaction));

        // Walk towards target
        double dx = targetX + 0.5 - x;
        double dy = targetY + 0.5 - y;
        double dist = Math.hypot(dx, dy);

        // Evaluate tile underfoot
        Tile cur = world.get((int)Math.floor(x), (int)Math.floor(y));
        double mod = 1.0;
        if (cur.type == TileType.SAND) {
            mod = 0.6;
            satisfaction -= 0.0005;
            patience -= 0.0004;
        } else if (cur.type == TileType.WATER) {
            mod = 0.3;
            satisfaction -= 0.001;
            patience -= 0.001;
        } else if (cur.type == TileType.FAIRWAY) {
            mod = 1.2;
            satisfaction += 0.0005;
            cur.wear = Math.min(1.0, cur.wear + 0.0006);
            cur.quality = Math.max(0, cur.quality - 0.0004);
        } else if (cur.type == TileType.GREEN) {
            mod = 1.0 + skill * 0.2;
            satisfaction += 0.0008;
            cur.wear = Math.min(1.0, cur.wear + 0.0003);
            cur.quality = Math.max(0, cur.quality - 0.0002);
        } else if (cur.type == TileType.PATH) {
            mod = 1.25;
        } else if (cur.type == TileType.TREE) {
            mod = 0.7;
            satisfaction -= 0.0002;
        }

        // If close to green with flag, "swing" animation then finish
        Tile targetTile = world.get(targetX, targetY);
        if (targetTile.type == TileType.GREEN && targetTile.flag && dist < 1.3) {
            if (swingCooldown == 0) {
                aiming = true;
                swingCooldown = 1.0 + (1.0 - skill) * 1.0;
                // particle arc
                for (int i = 0; i < 8; i++) {
                    particles.add(Particle.makeSwing(x + 0.5, y + 0.5));
                }
            } else {
                // after cooldown, finish
                if (swingCooldown < 0.1) {
                    done = true;
                }
            }
        } else {
            aiming = false;
        }

        // Move
        if (!aiming && dist > 0.05) {
            double dirx = dx / dist;
            double diry = dy / dist;
            double spd = speed * mod * dt;
            x += dirx * spd;
            y += diry * spd;
            steps++;
        } else if (!aiming) {
            // Reached current target: choose next one or return to clubhouse
            if (targetTile.type == TileType.GREEN && targetTile.flag) {
                // return to clubhouse
                Point c = world.findAny(TileType.CLUBHOUSE);
                if (c != null) {
                    targetX = c.x;
                    targetY = c.y;
                } else {
                    // wander away and then disappear
                    targetX += (int)(rng.nextInt(5) - 2);
                    targetY += (int)(rng.nextInt(5) - 2);
                    ageOutTimer += 0.01;
                }
            } else {
                // pick another interesting tile
                Point next = world.findNearest(targetX, targetY, TileType.GREEN, 60);
                if (next != null) {
                    targetX = next.x;
                    targetY = next.y;
                } else {
                    targetX += (int)(rng.nextInt(9) - 4);
                    targetY += (int)(rng.nextInt(9) - 4);
                }
            }
        }

        if (patience <= 0.1) {
            // leave angry
            done = true;
            satisfaction *= 0.5;
        }
    }

    void draw(Graphics2D g, Camera cam) {
        int ts = cam.tileSize;
        int px = (int) (x * ts);
        int py = (int) (y * ts);

        // shadow
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(px - 4, py - 4, 12, 8);

        // body
        g.setColor(shirt);
        g.fillOval(px - 3, py - 10, 12, 12);
        g.setColor(new Color(240, 224, 200));
        g.fillOval(px + 2, py - 14, 6, 6);

        if (aiming) {
            g.setColor(new Color(255, 255, 255, 200));
            g.drawLine(px + 5, py - 12, px + 12, py - 18);
        }

        // aim line to target
        g.setColor(new Color(255, 255, 255, 60));
        g.drawLine(px + 4, py - 10, (int)((targetX + 0.5) * ts), (int)((targetY + 0.5) * ts));
    }
}

class Cart {
    double x, y;
    double tx, ty;
    double speed = 3.0;
    boolean dead = false;

    Cart(double x, double y, double tx, double ty) {
        this.x = x; this.y = y; this.tx = tx; this.ty = ty;
    }

    void update(double dt) {
        double dx = tx - x;
        double dy = ty - y;
        double dist = Math.hypot(dx, dy);
        if (dist < 0.05) {
            dead = true;
            return;
        }
        double dirx = dx / dist;
        double diry = dy / dist;
        x += dirx * speed * dt;
        y += diry * speed * dt;
    }

    void draw(Graphics2D g, Camera cam) {
        int ts = cam.tileSize;
        int px = (int)(x * ts);
        int py = (int)(y * ts);
        g.setColor(new Color(60, 140, 180));
        g.fillRect(px - 6, py - 6, 14, 10);
        g.setColor(Color.DARK_GRAY);
        g.fillOval(px - 7, py + 2, 6, 6);
        g.fillOval(px + 6, py + 2, 6, 6);
    }
}

enum StaffType {
    GROUNDSKEEPER, JANITOR
}

class Staff {
    StaffType type;
    double x, y;
    double speed = 1.2;
    int wage;
    int tx = -1, ty = -1;
    double retarget = 0;

    Color color;

    Staff(StaffType type, double x, double y, int wage) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.wage = wage;
        this.color = type == StaffType.GROUNDSKEEPER ? new Color(20, 180, 90) : new Color(200, 200, 60);
    }

    void update(World world, double dt, Random rng) {
        retarget -= dt;
        if (retarget <= 0 || tx < 0) {
            retarget = 2.0 + rng.nextDouble() * 3.0;
            // Seek tile to improve
            int bx = (int)x, by = (int)y;
            double bestScore = -999;
            int bestX = bx, bestY = by;
            for (int i = 0; i < 80; i++) {
                int xx = Math.min(world.width - 2, Math.max(1, bx + rng.nextInt(25) - 12));
                int yy = Math.min(world.height - 2, Math.max(1, by + rng.nextInt(25) - 12));
                Tile t = world.get(xx, yy);
                double s = 0;
                if (type == StaffType.GROUNDSKEEPER) {
                    if (t.type == TileType.FAIRWAY || t.type == TileType.GREEN) {
                        s = t.wear * 2 + (1.0 - t.quality);
                    }
                } else {
                    if (t.type == TileType.PATH || t.type == TileType.CLUBHOUSE) s = 0.5 + rng.nextDouble();
                    if (t.decor > 0) s += 0.2;
                }
                if (s > bestScore) {
                    bestScore = s;
                    bestX = xx;
                    bestY = yy;
                }
            }
            tx = bestX; ty = bestY;
        }

        // Move
        double dx = tx + 0.5 - x;
        double dy = ty + 0.5 - y;
        double dist = Math.hypot(dx, dy);
        if (dist > 0.1) {
            x += dx / dist * speed * dt;
            y += dy / dist * speed * dt;
        } else {
            // Work on tile
            Tile t = world.get(tx, ty);
            if (type == StaffType.GROUNDSKEEPER) {
                if (t.type == TileType.FAIRWAY || t.type == TileType.GREEN) {
                    t.wear = Math.max(0, t.wear - 0.05);
                    t.quality = Math.min(1.0, t.quality + 0.02);
                }
            } else {
                // janitor "polish"
                if (t.type == TileType.PATH || t.type == TileType.CLUBHOUSE) {
                    // purely cosmetic
                }
            }
        }
    }

    void draw(Graphics2D g, Camera cam) {
        int ts = cam.tileSize;
        int px = (int)(x * ts);
        int py = (int)(y * ts);
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(px - 5, py - 3, 12, 7);
        g.setColor(color);
        g.fillOval(px - 3, py - 9, 10, 10);
        g.setColor(Color.BLACK);
        g.drawLine(px + 2, py - 5, px + 6, py - 10);
    }
}

// Visuals

class Particle {
    double x, y;
    double vx, vy;
    double life;
    Color color;

    Particle(double x, double y, double vx, double vy, double life, Color color) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = life; this.color = color;
    }

    static Particle makeBurst(double x, double y, BuildTool tool) {
        Random r = new Random();
        double ang = r.nextDouble() * Math.PI * 2;
        double sp = 1 + r.nextDouble() * 2;
        Color c;
        switch (tool) {
            case FAIRWAY: c = new Color(60, 200, 80); break;
            case GREEN: c = new Color(80, 240, 120); break;
            case SAND: c = new Color(220, 200, 140); break;
            case WATER: c = new Color(80, 160, 220); break;
            case TREE: c = new Color(50, 120, 60); break;
            case PATH: c = new Color(160, 140, 120); break;
            case CLUBHOUSE: c = new Color(180, 160, 140); break;
            case DECOR: c = new Color(130, 130, 130); break;
            default: c = Color.WHITE;
        }
        return new Particle(x, y, Math.cos(ang) * sp, Math.sin(ang) * sp, 0.6, c);
    }

    static Particle makeSwing(double x, double y) {
        Random r = new Random();
        double ang = r.nextDouble() * Math.PI * 2;
        double sp = 1 + r.nextDouble() * 3;
        return new Particle(x, y, Math.cos(ang) * sp, -Math.abs(Math.sin(ang) * sp), 0.9, new Color(255, 255, 255, 200));
    }

    void update(double dt) {
        x += vx * dt;
        y += vy * dt;
        vy += 0.6 * dt;
        life -= dt;
    }

    void draw(Graphics2D g, Camera cam) {
        if (life <= 0) return;
        int ts = cam.tileSize;
        int px = (int)(x * ts);
        int py = (int)(y * ts);
        int a = (int)(Math.max(0, Math.min(1.0, life)) * 255);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
        g.fillOval(px - 3, py - 3, 6, 6);
    }
}

class Cloud {
    double x, y, speed, scale;
    Color color;

    Cloud(double x, double y, double speed, double scale, Color color) {
        this.x = x; this.y = y; this.speed = speed; this.scale = scale; this.color = color;
    }

    void draw(Graphics2D g) {
        g.setColor(color);
        int sx = (int)x;
        int sy = (int)y;
        int s = (int)(40 * scale);
        g.fillOval(sx, sy, s, s / 2);
        g.fillOval(sx + s / 3, sy - s / 6, s, s / 2);
        g.fillOval(sx + s / 2, sy, s, s / 2);
    }
}

// Finance

class Finance {
    static class Transaction {
        final String label;
        final int amount;
        final long time;
        Transaction(String label, int amount) {
            this.label = label; this.amount = amount; this.time = System.currentTimeMillis();
        }
    }

    final List<Transaction> transactions = new ArrayList<>();
    int dailyPlayers = 0;
    double dailySatisfactionSum = 0.0;

    void recordIncome(String label, int amount) {
        transactions.add(new Transaction(label, amount));
        // small bonus to satisfaction metric
        dailySatisfactionSum += 0.7;
    }

    void recordExpense(String label, int amount) {
        transactions.add(new Transaction(label, -amount));
    }

    void recordSatisfaction(double normalized) {
        dailySatisfactionSum += normalized;
    }

    void endOfDay(int dayCount) {
        transactions.add(new Transaction("End of day " + dayCount, 0));
        dailyPlayers = 0;
        dailySatisfactionSum = 0;
    }
}

// Utility

class Point {
    int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }
}
