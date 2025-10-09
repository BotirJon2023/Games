import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class GolfResortManager extends JFrame {
    private GamePanel gamePanel;

    public GolfResortManager() {
        setTitle("Golf Resort Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GolfResortManager());
    }
}

class GamePanel extends JPanel {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final int GRID_SIZE = 60;

    private Resort resort;
    private Timer gameTimer;
    private Timer animationTimer;
    private BuildingType selectedBuilding;
    private Point hoverCell;
    private List<AnimatedGolfer> golfers;
    private List<CloudAnimation> clouds;
    private List<Particle> particles;
    private Random rand;
    private int animationFrame;
    private Font titleFont;
    private Font normalFont;
    private Font smallFont;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(34, 139, 34));

        resort = new Resort();
        golfers = new ArrayList<>();
        clouds = new ArrayList<>();
        particles = new ArrayList<>();
        rand = new Random();
        animationFrame = 0;
        selectedBuilding = null;

        titleFont = new Font("Arial", Font.BOLD, 24);
        normalFont = new Font("Arial", Font.PLAIN, 14);
        smallFont = new Font("Arial", Font.PLAIN, 12);

        initializeClouds();
        spawnInitialGolfers();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getPoint());
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHover(e.getPoint());
            }
        });

        gameTimer = new Timer(1000, e -> updateGame());
        gameTimer.start();

        animationTimer = new Timer(50, e -> {
            animationFrame++;
            updateAnimations();
            repaint();
        });
        animationTimer.start();
    }

    private void initializeClouds() {
        for (int i = 0; i < 5; i++) {
            clouds.add(new CloudAnimation(
                    rand.nextInt(WIDTH),
                    rand.nextInt(200) + 50,
                    rand.nextDouble() * 0.5 + 0.3
            ));
        }
    }

    private void spawnInitialGolfers() {
        for (int i = 0; i < 3; i++) {
            spawnGolfer();
        }
    }

    private void spawnGolfer() {
        int startX = rand.nextInt(WIDTH - 100) + 50;
        golfers.add(new AnimatedGolfer(startX, HEIGHT - 150));
    }

    private void updateHover(Point p) {
        int gridX = (p.x - 50) / GRID_SIZE;
        int gridY = (p.y - 150) / GRID_SIZE;

        if (gridX >= 0 && gridX < 15 && gridY >= 0 && gridY < 8) {
            hoverCell = new Point(gridX, gridY);
        } else {
            hoverCell = null;
        }
        repaint();
    }

    private void handleClick(Point p) {
        // Check building selection buttons
        if (p.y < 120) {
            int btnWidth = 180;
            int btnHeight = 40;
            int startX = 50;
            int y = 60;

            BuildingType[] types = BuildingType.values();
            for (int i = 0; i < types.length; i++) {
                Rectangle btn = new Rectangle(startX + i * (btnWidth + 10), y, btnWidth, btnHeight);
                if (btn.contains(p)) {
                    selectedBuilding = types[i];
                    return;
                }
            }
        }

        // Check grid placement
        int gridX = (p.x - 50) / GRID_SIZE;
        int gridY = (p.y - 150) / GRID_SIZE;

        if (selectedBuilding != null && gridX >= 0 && gridX < 15 && gridY >= 0 && gridY < 8) {
            if (resort.canBuild(gridX, gridY, selectedBuilding)) {
                if (resort.buildFacility(gridX, gridY, selectedBuilding)) {
                    createBuildParticles(50 + gridX * GRID_SIZE, 150 + gridY * GRID_SIZE);
                    selectedBuilding = null;
                }
            }
        }
    }

    private void createBuildParticles(int x, int y) {
        for (int i = 0; i < 20; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double speed = rand.nextDouble() * 3 + 1;
            particles.add(new Particle(
                    x + GRID_SIZE / 2,
                    y + GRID_SIZE / 2,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    new Color(255, 215, 0)
            ));
        }
    }

    private void updateGame() {
        resort.update();

        if (rand.nextDouble() < 0.3 && golfers.size() < 15) {
            spawnGolfer();
        }
    }

    private void updateAnimations() {
        // Update golfers
        Iterator<AnimatedGolfer> golferIt = golfers.iterator();
        while (golferIt.hasNext()) {
            AnimatedGolfer g = golferIt.next();
            g.update();
            if (g.isOffScreen(WIDTH)) {
                golferIt.remove();
            }
        }

        // Update clouds
        for (CloudAnimation c : clouds) {
            c.update(WIDTH);
        }

        // Update particles
        Iterator<Particle> particleIt = particles.iterator();
        while (particleIt.hasNext()) {
            Particle p = particleIt.next();
            p.update();
            if (p.isDead()) {
                particleIt.remove();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawSky(g2d);
        drawClouds(g2d);
        drawHeader(g2d);
        drawBuildingButtons(g2d);
        drawGrid(g2d);
        drawBuildings(g2d);
        drawGolfers(g2d);
        drawParticles(g2d);
        drawStats(g2d);
    }

    private void drawSky(Graphics2D g2d) {
        GradientPaint skyGradient = new GradientPaint(
                0, 0, new Color(135, 206, 235),
                0, HEIGHT, new Color(176, 224, 230)
        );
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawClouds(Graphics2D g2d) {
        for (CloudAnimation cloud : clouds) {
            cloud.draw(g2d);
        }
    }

    private void drawHeader(Graphics2D g2d) {
        g2d.setColor(new Color(0, 100, 0, 200));
        g2d.fillRect(0, 0, WIDTH, 120);

        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        g2d.drawString("⛳ Golf Resort Manager", 50, 35);
    }

    private void drawBuildingButtons(Graphics2D g2d) {
        BuildingType[] types = BuildingType.values();
        int btnWidth = 180;
        int btnHeight = 40;
        int startX = 50;
        int y = 60;

        for (int i = 0; i < types.length; i++) {
            BuildingType type = types[i];
            int x = startX + i * (btnWidth + 10);

            boolean isSelected = type == selectedBuilding;
            boolean canAfford = resort.getMoney() >= type.cost;

            if (isSelected) {
                g2d.setColor(new Color(255, 215, 0));
            } else if (canAfford) {
                g2d.setColor(new Color(60, 179, 113));
            } else {
                g2d.setColor(new Color(100, 100, 100));
            }

            g2d.fillRoundRect(x, y, btnWidth, btnHeight, 10, 10);

            g2d.setColor(Color.WHITE);
            g2d.drawRoundRect(x, y, btnWidth, btnHeight, 10, 10);

            g2d.setFont(normalFont);
            String name = type.name().replace("_", " ");
            g2d.drawString(name, x + 10, y + 20);

            g2d.setFont(smallFont);
            g2d.drawString("$" + type.cost + " | +" + type.income + "/s", x + 10, y + 35);
        }
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(0, 100, 0, 100));

        for (int x = 0; x < 15; x++) {
            for (int y = 0; y < 8; y++) {
                int px = 50 + x * GRID_SIZE;
                int py = 150 + y * GRID_SIZE;

                if (hoverCell != null && hoverCell.x == x && hoverCell.y == y) {
                    if (selectedBuilding != null && resort.canBuild(x, y, selectedBuilding)) {
                        g2d.setColor(new Color(0, 255, 0, 100));
                    } else {
                        g2d.setColor(new Color(255, 0, 0, 100));
                    }
                    g2d.fillRect(px, py, GRID_SIZE, GRID_SIZE);
                }

                g2d.setColor(new Color(255, 255, 255, 50));
                g2d.drawRect(px, py, GRID_SIZE, GRID_SIZE);
            }
        }
    }

    private void drawBuildings(Graphics2D g2d) {
        for (Building b : resort.getBuildings()) {
            int px = 50 + b.x * GRID_SIZE;
            int py = 150 + b.y * GRID_SIZE;

            drawBuilding(g2d, px, py, b.type);

            // Draw income animation
            if (animationFrame % 60 < 30) {
                g2d.setColor(new Color(255, 215, 0));
                g2d.setFont(smallFont);
                g2d.drawString("+$" + b.type.income, px + 5, py - 5);
            }
        }
    }

    private void drawBuilding(Graphics2D g2d, int x, int y, BuildingType type) {
        switch (type) {
            case GOLF_COURSE:
                g2d.setColor(new Color(34, 139, 34));
                g2d.fillRoundRect(x + 5, y + 5, GRID_SIZE - 10, GRID_SIZE - 10, 10, 10);
                g2d.setColor(new Color(255, 255, 255));
                g2d.fillOval(x + GRID_SIZE / 2 - 5, y + GRID_SIZE / 2 - 5, 10, 10);
                g2d.setColor(Color.BLACK);
                g2d.drawString("⛳", x + 20, y + 35);
                break;

            case CLUBHOUSE:
                g2d.setColor(new Color(139, 69, 19));
                g2d.fillRect(x + 10, y + 20, GRID_SIZE - 20, GRID_SIZE - 25);
                g2d.setColor(new Color(178, 34, 34));
                int[] xPoints = {x + 10, x + GRID_SIZE / 2, x + GRID_SIZE - 10};
                int[] yPoints = {y + 20, y + 5, y + 20};
                g2d.fillPolygon(xPoints, yPoints, 3);
                g2d.setColor(new Color(255, 255, 200));
                g2d.fillRect(x + 20, y + 30, 10, 15);
                break;

            case PRO_SHOP:
                g2d.setColor(new Color(70, 130, 180));
                g2d.fillRect(x + 10, y + 15, GRID_SIZE - 20, GRID_SIZE - 20);
                g2d.setColor(new Color(255, 215, 0));
                g2d.fillRect(x + 15, y + 20, GRID_SIZE - 30, 15);
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.drawString("SHOP", x + 18, y + 30);
                break;

            case RESTAURANT:
                g2d.setColor(new Color(255, 140, 0));
                g2d.fillRoundRect(x + 8, y + 15, GRID_SIZE - 16, GRID_SIZE - 20, 8, 8);
                g2d.setColor(new Color(255, 255, 255));
                g2d.fillOval(x + 20, y + 25, 20, 20);
                g2d.setColor(Color.BLACK);
                g2d.drawString("🍴", x + 22, y + 40);
                break;

            case HOTEL:
                g2d.setColor(new Color(169, 169, 169));
                g2d.fillRect(x + 5, y + 10, GRID_SIZE - 10, GRID_SIZE - 15);
                g2d.setColor(new Color(135, 206, 250));
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        g2d.fillRect(x + 12 + col * 12, y + 18 + row * 12, 8, 8);
                    }
                }
                break;

            case PRACTICE_RANGE:
                g2d.setColor(new Color(154, 205, 50));
                g2d.fillRect(x + 5, y + 5, GRID_SIZE - 10, GRID_SIZE - 10);
                g2d.setColor(new Color(255, 255, 255));
                for (int i = 0; i < 4; i++) {
                    g2d.fillOval(x + 10 + i * 12, y + 30, 6, 6);
                }
                break;
        }

        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.drawRoundRect(x + 5, y + 5, GRID_SIZE - 10, GRID_SIZE - 10, 10, 10);
    }

    private void drawGolfers(Graphics2D g2d) {
        for (AnimatedGolfer golfer : golfers) {
            golfer.draw(g2d);
        }
    }

    private void drawParticles(Graphics2D g2d) {
        for (Particle p : particles) {
            p.draw(g2d);
        }
    }

    private void drawStats(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(WIDTH - 250, 10, 230, 100, 15, 15);

        g2d.setColor(Color.WHITE);
        g2d.setFont(normalFont);
        g2d.drawString("💰 Money: $" + resort.getMoney(), WIDTH - 235, 35);
        g2d.drawString("📊 Income: $" + resort.getIncome() + "/sec", WIDTH - 235, 55);
        g2d.drawString("🏢 Buildings: " + resort.getBuildings().size(), WIDTH - 235, 75);
        g2d.drawString("⭐ Reputation: " + resort.getReputation(), WIDTH - 235, 95);
    }
}

class Resort {
    private int money;
    private int income;
    private int reputation;
    private List<Building> buildings;
    private boolean[][] grid;

    public Resort() {
        money = 1000;
        income = 0;
        reputation = 0;
        buildings = new ArrayList<>();
        grid = new boolean[15][8];
    }

    public boolean canBuild(int x, int y, BuildingType type) {
        return !grid[x][y] && money >= type.cost;
    }

    public boolean buildFacility(int x, int y, BuildingType type) {
        if (canBuild(x, y, type)) {
            money -= type.cost;
            income += type.income;
            reputation += type.reputation;
            buildings.add(new Building(x, y, type));
            grid[x][y] = true;
            return true;
        }
        return false;
    }

    public void update() {
        money += income;
    }

    public int getMoney() { return money; }
    public int getIncome() { return income; }
    public int getReputation() { return reputation; }
    public List<Building> getBuildings() { return buildings; }
}

class Building {
    int x, y;
    BuildingType type;

    public Building(int x, int y, BuildingType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
}

enum BuildingType {
    GOLF_COURSE(300, 15, 5),
    CLUBHOUSE(500, 25, 10),
    PRO_SHOP(400, 20, 8),
    RESTAURANT(450, 22, 7),
    HOTEL(800, 40, 15),
    PRACTICE_RANGE(350, 18, 6);

    final int cost;
    final int income;
    final int reputation;

    BuildingType(int cost, int income, int reputation) {
        this.cost = cost;
        this.income = income;
        this.reputation = reputation;
    }
}

class AnimatedGolfer {
    private double x, y;
    private double vx;
    private int frame;
    private Color shirtColor;
    private boolean swinging;
    private int swingFrame;

    public AnimatedGolfer(double x, double y) {
        this.x = x;
        this.y = y;
        this.vx = Math.random() * 1.5 + 0.5;
        this.frame = 0;
        this.shirtColor = new Color(
                (int) (Math.random() * 156 + 100),
                (int) (Math.random() * 156 + 100),
                (int) (Math.random() * 156 + 100)
        );
        this.swinging = Math.random() < 0.3;
        this.swingFrame = 0;
    }

    public void update() {
        x += vx;
        frame++;

        if (swinging) {
            swingFrame++;
            if (swingFrame > 40) {
                swinging = false;
                swingFrame = 0;
            }
        }
    }

    public void draw(Graphics2D g2d) {
        int px = (int) x;
        int py = (int) y;

        // Shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(px - 8, py + 25, 16, 6);

        // Legs
        int legOffset = (int) (Math.sin(frame * 0.2) * 3);
        g2d.setColor(new Color(101, 67, 33));
        g2d.fillRect(px - 4, py + 10, 3, 12);
        g2d.fillRect(px + 1, py + 10 + legOffset, 3, 12 - legOffset);

        // Body
        g2d.setColor(shirtColor);
        g2d.fillRect(px - 6, py, 12, 12);

        // Head
        g2d.setColor(new Color(255, 220, 177));
        g2d.fillOval(px - 5, py - 8, 10, 10);

        // Hat
        g2d.setColor(Color.RED);
        g2d.fillArc(px - 6, py - 10, 12, 8, 0, 180);

        // Golf club if swinging
        if (swinging) {
            double angle = Math.PI / 4 + Math.sin(swingFrame * 0.2) * Math.PI / 3;
            int clubX = px + (int) (Math.cos(angle) * 15);
            int clubY = py + 5 + (int) (Math.sin(angle) * 15);

            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(px, py + 5, clubX, clubY);
            g2d.setStroke(new BasicStroke(1));
        }
    }

    public boolean isOffScreen(int width) {
        return x > width + 50;
    }
}

class CloudAnimation {
    private double x, y;
    private double speed;
    private int size;

    public CloudAnimation(double x, double y, double speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.size = (int) (Math.random() * 40 + 40);
    }

    public void update(int width) {
        x += speed;
        if (x > width + size) {
            x = -size;
        }
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 180));

        int px = (int) x;
        int py = (int) y;

        g2d.fillOval(px, py, size, size / 2);
        g2d.fillOval(px + size / 4, py - size / 4, size / 2, size / 2);
        g2d.fillOval(px + size / 2, py, size / 2, size / 2);
    }
}

class Particle {
    private double x, y;
    private double vx, vy;
    private Color color;
    private int life;

    public Particle(double x, double y, double vx, double vy, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.life = 30;
    }

    public void update() {
        x += vx;
        y += vy;
        vy += 0.2;
        life--;
    }

    public void draw(Graphics2D g2d) {
        int alpha = (int) (255 * (life / 30.0));
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g2d.fillOval((int) x - 3, (int) y - 3, 6, 6);
    }

    public boolean isDead() {
        return life <= 0;
    }
}