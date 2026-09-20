import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public class ExtremeMountainBikeRacingGame extends JPanel implements Runnable {

    // --- Window Settings ---
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 640;

    // --- Game States ---
    private enum State { MENU, SINGLE_PLAYER, TWO_PLAYER }
    private State currentState = State.MENU;
    private boolean isRunning = false;

    // --- World & Track Parameters ---
    private static final int SEGMENT_LENGTH = 200;
    private static final int RUMBLE_LENGTH = 3;
    private static final int ROAD_WIDTH = 2000;
    private final List<TrackSegment> track = new ArrayList<>();
    private int trackLength = 0;

    // --- Players ---
    private Rider p1;
    private Rider p2;

    // --- Input Tracking ---
    private final boolean[] keys = new boolean[256];

    public ExtremeMountainBikeRacingGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        setBackground(Color.BLACK);

        addKeyListener(new KeyListener() {
            @Override public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = true;
                if (currentState == State.MENU) {
                    if (e.getKeyCode() == KeyEvent.VK_1) startMatch(State.SINGLE_PLAYER);
                    if (e.getKeyCode() == KeyEvent.VK_2) startMatch(State.TWO_PLAYER);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    currentState = State.MENU;
                    isRunning = false;
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = false;
            }
        });

        buildTrack();
        p1 = new Rider("Player 1", Color.RED, false);
        p2 = new Rider("Rival", Color.BLUE, true);
    }

    private void buildTrack() {
        track.clear();
        // Generate a mountain course with elevation changes and winding curves
        addTrackSection(100, 0, 0);       // Straight flat start
        addTrackSection(200, 4, 60);      // Right curve downhill
        addTrackSection(150, -3, -80);    // Left curve steep uphill
        addTrackSection(200, 0, 40);      // Downhill jump section
        addTrackSection(150, -5, 0);      // Sharp left
        addTrackSection(200, 3, -50);     // Right curve uphill
        addTrackSection(100, 0, 0);       // Finish straight
        trackLength = track.size() * SEGMENT_LENGTH;
    }

    private void addTrackSection(int count, double curve, double hill) {
        for (int i = 0; i < count; i++) {
            double startY = track.isEmpty() ? 0 : track.get(track.size() - 1).y2;
            double endY = startY + hill;
            TrackSegment seg = new TrackSegment(track.size(), startY, endY, curve);

            // Add roadside mountain trees & rocks
            if (i % 4 == 0) seg.decorType = (i % 8 == 0) ? 1 : 2;
            track.add(seg);
        }
    }

    private void startMatch(State mode) {
        currentState = mode;
        p1.reset(0.2);
        if (mode == State.SINGLE_PLAYER) {
            p2.reset(-0.2);
            p2.isAI = true;
            p2.name = "Rival AI";
        } else {
            p2.reset(-0.2);
            p2.isAI = false;
            p2.name = "Player 2";
        }
        isRunning = true;
        new Thread(this).start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60.0;

        while (isRunning) {
            long now = System.nanoTime();
            if (now - lastTime >= nsPerTick) {
                update();
                repaint();
                lastTime = now;
            }
            try { Thread.sleep(2); } catch (InterruptedException ignored) {}
        }
    }

    private void update() {
        if (currentState == State.MENU) return;

        // Player 1 Control: UP (Accel), LEFT/RIGHT (Steer)
        p1.updateInput(keys[KeyEvent.VK_UP], keys[KeyEvent.VK_LEFT], keys[KeyEvent.VK_RIGHT]);
        p1.updatePhysics(track, trackLength);

        // Player 2 / AI Control: W (Accel), A/D (Steer)
        if (p2.isAI) {
            p2.updateAI(track, trackLength);
        } else {
            p2.updateInput(keys[KeyEvent.VK_W], keys[KeyEvent.VK_A], keys[KeyEvent.VK_D]);
        }
        p2.updatePhysics(track, trackLength);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (currentState == State.MENU) {
            renderMenu(g2);
            return;
        }

        if (currentState == State.SINGLE_PLAYER) {
            renderDriverView(g2, p1, 0, 0, WIDTH, HEIGHT);
            renderHUD(g2, p1, 0, 0, WIDTH);
        } else if (currentState == State.TWO_PLAYER) {
            // Split-Screen Render
            int halfH = HEIGHT / 2;
            renderDriverView(g2, p1, 0, 0, WIDTH, halfH);
            renderHUD(g2, p1, 0, 0, WIDTH);

            renderDriverView(g2, p2, 0, halfH, WIDTH, halfH);
            renderHUD(g2, p2, 0, halfH, WIDTH);

            // Split Line
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(0, halfH, WIDTH, halfH);
        }
    }

    private void renderDriverView(Graphics2D g2, Rider rider, int vx, int vy, int vw, int vh) {
        Shape oldClip = g2.getClip();
        g2.setClip(vx, vy, vw, vh);

        // Sky Background with Horizon Tilt
        g2.setColor(new Color(110, 165, 230));
        g2.fillRect(vx, vy, vw, vh);

        int baseSegmentIndex = (int) (rider.z / SEGMENT_LENGTH) % track.size();
        TrackSegment baseSegment = track.get(baseSegmentIndex);

        double cameraX = rider.x * ROAD_WIDTH;
        double cameraY = rider.cameraHeight + baseSegment.y1;
        double cameraZ = rider.z;

        double dx = -baseSegment.curve * (rider.z % SEGMENT_LENGTH) / SEGMENT_LENGTH;
        double currentX = 0;

        int drawDistance = 120;
        double maxY = vy + vh;

        // Render Pseudo-3D Track Segments (Back to Front)
        for (int n = 0; n < drawDistance; n++) {
            int segmentIndex = (baseSegmentIndex + n) % track.size();
            TrackSegment segment = track.get(segmentIndex);

            // Loop track z wrap
            double segmentLoopedZ = segment.index < baseSegmentIndex ? segment.z + trackLength : segment.z;

            project(segment.p1, segment.x1 - cameraX - currentX, segment.y1 - cameraY, segmentLoopedZ - cameraZ, vw, vh, vx, vy);
            project(segment.p2, segment.x2 - cameraX - currentX - dx, segment.y2 - cameraY, segmentLoopedZ + SEGMENT_LENGTH - cameraZ, vw, vh, vx, vy);

            currentX += dx;
            dx += segment.curve;

            if (segment.p1.screenZ <= 0 || segment.p2.screenY >= maxY) continue;

            if (segment.p2.screenY < maxY) {
                renderSegment(g2, segment, segment.p1, segment.p2);
                maxY = segment.p2.screenY;
            }
        }

        // Draw Handlebars (Driver's Cockpit View)
        renderHandlebars(g2, rider, vx, vy, vw, vh);

        g2.setClip(oldClip);
    }

    private void project(Point3D p, double worldX, double worldY, double worldZ, int vw, int vh, int vx, int vy) {
        p.screenZ = worldZ;
        if (worldZ <= 0) return;

        double cameraDepth = 0.8;
        double scale = cameraDepth / (worldZ / 1000.0);
        p.screenX = (int) (vx + (vw / 2.0) + (scale * worldX * vw / 2.0));
        p.screenY = (int) (vy + (vh / 2.0) - (scale * worldY * vh / 2.0));
        p.scale = scale;
    }

    private void renderSegment(Graphics2D g2, TrackSegment seg, Point3D p1, Point3D p2) {
        // Grass
        g2.setColor((seg.index / RUMBLE_LENGTH) % 2 == 0 ? new Color(40, 140, 40) : new Color(30, 120, 30));
        g2.fillRect(0, (int) p2.screenY, WIDTH, (int) (p1.screenY - p2.screenY));

        // Road Polygon
        int w1 = (int) (p1.scale * ROAD_WIDTH * WIDTH / 2.0);
        int w2 = (int) (p2.scale * ROAD_WIDTH * WIDTH / 2.0);

        int[] rx = { (int) p1.screenX - w1, (int) p2.screenX - w2, (int) p2.screenX + w2, (int) p1.screenX + w1 };
        int[] ry = { (int) p1.screenY, (int) p2.screenY, (int) p2.screenY, (int) p1.screenY };

        g2.setColor((seg.index / RUMBLE_LENGTH) % 2 == 0 ? new Color(80, 80, 80) : new Color(60, 60, 60));
        g2.fillPolygon(rx, ry, 4);

        // Roadside Scenery (Trees / Rocks)
        if (seg.decorType > 0) {
            int decorX = (int) (p1.screenX + (seg.decorType == 1 ? -w1 - 100 * p1.scale : w1 + 100 * p1.scale));
            int decorSize = (int) (120 * p1.scale * 10);
            if (decorSize > 2) {
                g2.setColor(seg.decorType == 1 ? new Color(15, 80, 15) : Color.DARK_GRAY);
                g2.fillOval(decorX - decorSize / 2, (int) p1.screenY - decorSize, decorSize, decorSize);
            }
        }
    }

    private void renderHandlebars(Graphics2D g2, Rider rider, int vx, int vy, int vw, int vh) {
        int centerX = vx + vw / 2 + (int) (rider.steering * 40);
        int centerY = vy + vh - 20 + (int) (Math.sin(System.currentTimeMillis() * 0.03) * (rider.speed * 2));

        g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Color.BLACK);

        // Handlebar stem and bar
        g2.drawLine(centerX - 160, centerY - 20, centerX + 160, centerY - 20);
        g2.drawLine(centerX, centerY + 80, centerX, centerY - 20);

        // Grips
        g2.setColor(rider.color);
        g2.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(centerX - 170, centerY - 20, centerX - 120, centerY - 20);
        g2.drawLine(centerX + 120, centerY - 20, centerX + 170, centerY - 20);

        // Suspension Fork Top
        g2.setColor(Color.LIGHT_GRAY);
        g2.setStroke(new BasicStroke(6f));
        g2.drawOval(centerX - 25, centerY + 10, 50, 30);
    }

    private void renderHUD(Graphics2D g2, Rider rider, int vx, int vy, int vw) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString(rider.name + " | Speed: " + (int) (rider.speed * 120) + " km/h", vx + 20, vy + 35);

        // Minimap Progress Bar
        int mapW = 200;
        int mapX = vx + vw - mapW - 20;
        int mapY = vy + 25;
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(mapX, mapY, mapW, 10);

        double progress = (rider.z % trackLength) / trackLength;
        g2.setColor(rider.color);
        g2.fillRect(mapX + (int) (progress * mapW), mapY - 3, 8, 16);
    }

    private void renderMenu(Graphics2D g2) {
        g2.setColor(new Color(15, 20, 30));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setColor(Color.ORANGE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 38));
        g2.drawString("EXTREME MOUNTAIN BIKE RACING", 150, 180);

        g2.setColor(Color.CYAN);
        g2.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2.drawString("[First-Person Driver's View]", 350, 230);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g2.drawString("Press [1] - Single Player (vs AI Driver)", 320, 340);
        g2.drawString("Press [2] - 2 Player Split-Screen (P1: Arrows / P2: W,A,D)", 230, 400);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Extreme Mountain Bike Racing - First Person");
        ExtremeMountainBikeRacingGame game = new ExtremeMountainBikeRacingGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    // --- Helper Classes ---

    static class Point3D {
        double screenX, screenY, screenZ, scale;
    }

    static class TrackSegment {
        public double x1;
        public double x2;
        int index;
        double y1, y2, z;
        double curve;
        int decorType = 0; // 0: None, 1: Tree, 2: Rock
        Point3D p1 = new Point3D();
        Point3D p2 = new Point3D();

        public TrackSegment(int index, double y1, double y2, double curve) {
            this.index = index;
            this.y1 = y1;
            this.y2 = y2;
            this.z = index * SEGMENT_LENGTH;
            this.curve = curve;
        }
    }

    static class Rider {
        String name;
        Color color;
        double x = 0;            // Normalized road position (-1.0 to 1.0)
        double z = 0;            // Distance along track
        double speed = 0;
        double maxSpeed = 1.2;
        double steering = 0;     // Visual handlebar lean angle
        double cameraHeight = 500;
        boolean isAI;

        public Rider(String name, Color color, boolean isAI) {
            this.name = name;
            this.color = color;
            this.isAI = isAI;
        }

        public void reset(double startX) {
            this.x = startX;
            this.z = 0;
            this.speed = 0;
            this.steering = 0;
        }

        public void updateInput(boolean accel, boolean left, boolean right) {
            if (accel) speed += 0.015;
            else speed *= 0.98; // Friction deceleration

            if (left) {
                x -= 0.025;
                steering = -1.0;
            } else if (right) {
                x += 0.025;
                steering = 1.0;
            } else {
                steering *= 0.8;
            }
        }

        public void updateAI(List<TrackSegment> track, int trackLength) {
            int currentSegmentIndex = (int) (z / SEGMENT_LENGTH) % track.size();
            TrackSegment currentSeg = track.get(currentSegmentIndex);

            // AI accelerates and automatically leans into upcoming track curves
            speed += 0.012;
            x -= currentSeg.curve * 0.02;
            steering = -currentSeg.curve;
        }

        public void updatePhysics(List<TrackSegment> track, int trackLength) {
            speed = Math.min(Math.max(speed, 0), maxSpeed);
            z += speed * 40;

            // Off-road slow down penalty
            if (x < -1.0 || x > 1.0) {
                speed *= 0.95;
            }
        }
    }
}