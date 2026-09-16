import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Path2D;
import java.util.ArrayList;

public class ExtremeMountainBikeRacingGame extends JPanel implements Runnable {

    // --- Game Configuration ---
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 600;
    private static final double GRAVITY = 0.35;
    private static final double FRICTION = 0.985;
    private static final double AIR_RESISTANCE = 0.995;

    // --- Game States ---
    private enum GameMode { MENU, SINGLE_PLAYER, TWO_PLAYER }
    private GameMode currentMode = GameMode.MENU;
    private boolean isRunning = false;

    // --- World & Entities ---
    private Terrain terrain;
    private Bike player1;
    private Bike player2; // Acts as AI in Single Player

    // --- Controls ---
    private final boolean[] keys = new boolean[256];

    public ExtremeMountainBikeRacingGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        setBackground(new Color(135, 206, 235)); // Sky Blue

        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = true;
                handleMenuInputs(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = false;
            }
        });

        initGame();
    }

    private void initGame() {
        terrain = new Terrain(10000);
        player1 = new Bike(100, "Player 1", Color.RED, false);
        player2 = new Bike(100, "Player 2 / AI", Color.BLUE, false);
    }

    private void startMatch(GameMode mode) {
        currentMode = mode;
        initGame();
        if (mode == GameMode.SINGLE_PLAYER) {
            player2.isAI = true;
            player2.name = "Rival AI";
        }
        isRunning = true;
        new Thread(this).start();
    }

    private void handleMenuInputs(int keyCode) {
        if (currentMode == GameMode.MENU) {
            if (keyCode == KeyEvent.VK_1) startMatch(GameMode.SINGLE_PLAYER);
            if (keyCode == KeyEvent.VK_2) startMatch(GameMode.TWO_PLAYER);
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            currentMode = GameMode.MENU;
            isRunning = false;
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60.0; // 60 FPS Target

        while (isRunning) {
            long now = System.nanoTime();
            if (now - lastTime >= nsPerTick) {
                update();
                repaint();
                lastTime = now;
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        if (currentMode == GameMode.MENU) return;

        // Player 1 Controls (WASD / Arrows)
        player1.handleInput(
                keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D],
                keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A],
                keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]
        );

        // Player 2 / AI Controls
        if (player2.isAI) {
            player2.updateAI(terrain);
        } else {
            player2.handleInput(
                    keys[KeyEvent.VK_L],
                    keys[KeyEvent.VK_J],
                    keys[KeyEvent.VK_I]
            );
        }

        // Update Physics
        player1.update(terrain);
        player2.update(terrain);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (currentMode == GameMode.MENU) {
            renderMenu(g2);
            return;
        }

        // Camera Offset tracking the leading player or average position
        double camX = (player1.x + player2.x) / 2.0 - WIDTH / 3.0;
        double camY = (player1.y + player2.y) / 2.0 - HEIGHT / 2.0;

        g2.translate(-camX, -camY);

        // Render Game World
        terrain.render(g2, camX, camX + WIDTH);
        player1.render(g2);
        player2.render(g2);

        g2.translate(camX, camY); // Reset transformation for UI

        renderUI(g2);
    }

    private void renderMenu(Graphics2D g2) {
        g2.setColor(new Color(20, 20, 30));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setColor(Color.ORANGE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 42));
        g2.drawString("EXTREME MOUNTAIN BIKE RACING", 120, 180);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g2.drawString("Press [1] - Single Player (vs CPU)", 340, 300);
        g2.drawString("Press [2] - Local Two Player (P1: Arrow Keys / P2: I,J,L)", 220, 360);

        g2.setFont(new Font("SansSerif", Font.ITALIC, 16));
        g2.drawString("Physics: Lean forward on downhill slopes, lean back on up-hills to maintain speed!", 200, 480);
    }

    private void renderUI(Graphics2D g2) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));

        // P1 HUD
        g2.setColor(player1.color);
        g2.drawString(player1.name + " Speed: " + String.format("%.1f", player1.vx) + " km/h", 20, 30);

        // P2 HUD
        g2.setColor(player2.color);
        g2.drawString(player2.name + " Speed: " + String.format("%.1f", player2.vx) + " km/h", 20, 60);

        g2.setColor(Color.BLACK);
        g2.drawString("Press [ESC] for Main Menu", WIDTH - 240, 30);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Extreme Mountain Bike Racing");
        ExtremeMountainBikeRacingGame game = new ExtremeMountainBikeRacingGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    // --- Inner Class: Terrain System ---
    static class Terrain {
        private final double[] heightMap;

        public Terrain(int length) {
            heightMap = new double[length];
            generateTerrain();
        }

        private void generateTerrain() {
            for (int x = 0; x < heightMap.length; x++) {
                // Layered sine waves for mountain profile
                double base = 400;
                double h1 = Math.sin(x * 0.005) * 120;
                double h2 = Math.cos(x * 0.015) * 50;
                double h3 = Math.sin(x * 0.03) * 20;
                heightMap[x] = base + h1 + h2 + h3;
            }
        }

        public double getHeight(double x) {
            int ix = (int) x;
            if (ix < 0) return heightMap[0];
            if (ix >= heightMap.length - 1) return heightMap[heightMap.length - 1];
            double t = x - ix;
            return heightMap[ix] * (1 - t) + heightMap[ix + 1] * t; // Linear interpolation
        }

        public double getSlope(double x) {
            return getHeight(x + 1) - getHeight(x - 1);
        }

        public void render(Graphics2D g2, double minX, double maxX) {
            Path2D path = new Path2D.Double();
            path.moveTo(Math.max(0, minX), 1000);

            for (int x = (int) Math.max(0, minX); x < Math.min(heightMap.length, maxX); x += 5) {
                path.lineTo(x, heightMap[x]);
            }

            path.lineTo(Math.min(heightMap.length, maxX), 1000);
            path.closePath();

            // Mountain Fill
            g2.setColor(new Color(34, 139, 34)); // Forest Green
            g2.fill(path);

            // Terrain Surface Highlight
            g2.setColor(new Color(101, 67, 33)); // Dirt Brown
            g2.setStroke(new BasicStroke(6f));
            g2.draw(path);
        }
    }

    // --- Inner Class: Bike Physics & Rendering ---
    static class Bike {
        double x, y;
        double vx = 0, vy = 0;
        double angle = 0; // Bike pitch angle in radians
        double angularVelocity = 0;
        double wheelRotation = 0;
        double suspensionOffset = 0;

        String name;
        Color color;
        boolean isAI;
        boolean inAir = false;

        public Bike(double startX, String name, Color color, boolean isAI) {
            this.x = startX;
            this.y = 300;
            this.name = name;
            this.color = color;
            this.isAI = isAI;
        }

        public void handleInput(boolean throttle, boolean leanBack, boolean leanForward) {
            if (!inAir && throttle) {
                vx += 0.4; // Acceleration
            }
            if (leanBack) {
                angularVelocity -= 0.005; // Lean backwards
            }
            if (leanForward) {
                angularVelocity += 0.005; // Lean forward
            }
        }

        public void updateAI(Terrain terrain) {
            double slope = terrain.getSlope(x);
            // Basic AI logic: accelerate, lean forward on downhills, lean back on uphills
            handleInput(true, slope < -0.2, slope > 0.2);
        }

        public void update(Terrain terrain) {
            // Apply Gravity
            vy += GRAVITY;

            // Apply Velocities
            x += vx;
            y += vy;
            angle += angularVelocity;

            // Damping
            vx *= AIR_RESISTANCE;
            angularVelocity *= 0.92;

            double groundY = terrain.getHeight(x);
            double slope = terrain.getSlope(x);
            double targetAngle = Math.atan2(slope, 2.0);

            // Ground Collision & Suspension Physics
            if (y >= groundY - 20) { // Wheel radius ~ 20
                y = groundY - 20;
                vy = 0;
                inAir = false;

                // Friction
                vx *= FRICTION;

                // Auto-align bike frame to slope when grounded
                angle += (targetAngle - angle) * 0.15;

                // Dynamic suspension spring reaction
                suspensionOffset = Math.sin(System.currentTimeMillis() * 0.02) * (vx * 0.1);
            } else {
                inAir = true;
                suspensionOffset = 0;
            }

            // Wheel rotation speed based on linear velocity
            wheelRotation += vx * 0.1;
        }

        public void render(Graphics2D g2) {
            Graphics2D g = (Graphics2D) g2.create();
            g.translate(x, y + suspensionOffset);
            g.rotate(angle);

            // Wheels
            drawWheel(g, -25, 0);
            drawWheel(g, 25, 0);

            // Bike Frame
            g.setColor(color);
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Path2D frame = new Path2D.Double();
            frame.moveTo(-25, 0);  // Rear hub
            frame.lineTo(-5, -20); // Seat post
            frame.lineTo(15, -20); // Handlebar base
            frame.lineTo(25, 0);   // Front hub
            frame.moveTo(-5, -20);
            frame.lineTo(5, 0);    // Bottom bracket
            frame.lineTo(15, -20);
            frame.lineTo(-25, 0);
            g.draw(frame);

            // Suspension Fork (Front)
            g.setColor(Color.GRAY);
            g.setStroke(new BasicStroke(2f));
            g.drawLine(15, -20, 25, 0);

            // Rider Animation
            drawRider(g);

            g.dispose();
        }

        private void drawWheel(Graphics2D g, int cx, int cy) {
            int radius = 14;
            g.setColor(Color.DARK_GRAY);
            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

            g.setColor(Color.LIGHT_GRAY);
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

            // Dynamic Spokes
            g.setColor(Color.WHITE);
            g.drawLine(
                    cx + (int)(Math.cos(wheelRotation) * radius),
                    cy + (int)(Math.sin(wheelRotation) * radius),
                    cx - (int)(Math.cos(wheelRotation) * radius),
                    cy - (int)(Math.sin(wheelRotation) * radius)
            );
            g.drawLine(
                    cx + (int)(Math.cos(wheelRotation + Math.PI/2) * radius),
                    cy + (int)(Math.sin(wheelRotation + Math.PI/2) * radius),
                    cx - (int)(Math.cos(wheelRotation + Math.PI/2) * radius),
                    cy - (int)(Math.sin(wheelRotation + Math.PI/2) * radius)
            );
        }

        private void drawRider(Graphics2D g) {
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Torso
            g.drawLine(-5, -20, -10, -40);
            // Head / Helmet
            g.setColor(color);
            g.fillOval(-15, -52, 12, 12);

            // Arms / Handlebars
            g.setColor(Color.BLACK);
            g.drawLine(-10, -38, 15, -23);

            // Legs / Pedals
            double pedalAngle = wheelRotation * 0.5;
            int pedalX = (int)(Math.cos(pedalAngle) * 6);
            int pedalY = (int)(Math.sin(pedalAngle) * 6);
            g.drawLine(-5, -20, pedalX, pedalY);
        }
    }
}