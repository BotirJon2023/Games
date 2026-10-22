import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class HoverboardRacingAdventure extends JPanel implements ActionListener, KeyListener {

    // ==================== WINDOW ====================
    static final int WIDTH = 1280;
    static final int HEIGHT = 720;
    static final int FPS = 60;

    // ==================== GAME STATE ====================
    enum GameMode { MENU, PLAYING, GAME_OVER }
    enum RaceMode { VS_COMPUTER, VS_PLAYER }

    GameMode gameState = GameMode.MENU;
    RaceMode raceMode = RaceMode.VS_COMPUTER;

    Timer timer;
    Random rand = new Random();

    Hoverboard player1;
    Hoverboard player2;

    // Effects & scenery
    ArrayList<Particle> particles = new ArrayList<>();
    ArrayList<SnowFlake> snow = new ArrayList<>();
    ArrayList<Cloud> clouds = new ArrayList<>();
    ArrayList<Tree> trees = new ArrayList<>();
    ArrayList<MountainLayer> mountains = new ArrayList<>();

    double scrollY = 0;
    int scoreP1 = 0, scoreP2 = 0;
    int winScore = 8;
    boolean raceFinished = false;
    String winnerText = "";

    boolean[] keys = new boolean[512];

    // Colorado color palette
    Color skyTop      = new Color(25, 60, 140);
    Color skyBottom   = new Color(135, 206, 250);
    Color snowWhite   = new Color(245, 250, 255);
    Color pineGreen   = new Color(20, 80, 40);
    Color rockGray    = new Color(90, 90, 100);
    Color neonCyan    = new Color(0, 255, 255);
    Color neonPink    = new Color(255, 40, 150);
    Color neonOrange  = new Color(255, 160, 40);
    Color goldAspen   = new Color(255, 200, 50);

    public HoverboardRacingAdventure() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        initScenery();

        timer = new Timer(1000 / FPS, this);
        timer.start();
    }

    void initScenery() {
        // Layered mountains (parallax)
        mountains.add(new MountainLayer(0.15, new Color(40, 55, 90), 180));   // far
        mountains.add(new MountainLayer(0.30, new Color(55, 70, 100), 220));  // mid
        mountains.add(new MountainLayer(0.55, new Color(70, 85, 110), 280));  // near

        // Clouds
        for (int i = 0; i < 12; i++) {
            clouds.add(new Cloud(rand.nextInt(WIDTH + 400) - 200,
                    40 + rand.nextInt(180),
                    80 + rand.nextInt(120),
                    0.3f + rand.nextFloat() * 0.6f));
        }

        // Snow
        for (int i = 0; i < 180; i++) {
            snow.add(new SnowFlake(rand.nextInt(WIDTH), rand.nextInt(HEIGHT),
                    1.2f + rand.nextFloat() * 2.8f,
                    0.6f + rand.nextFloat() * 1.4f));
        }

        // Pine trees along sides
        for (int i = 0; i < 35; i++) {
            trees.add(new Tree(30 + rand.nextInt(90),
                    rand.nextInt(HEIGHT + 400) - 200,
                    50 + rand.nextInt(70), true));   // left
            trees.add(new Tree(WIDTH - 120 - rand.nextInt(90),
                    rand.nextInt(HEIGHT + 400) - 200,
                    50 + rand.nextInt(70), false));  // right
        }
    }

    // ==================== HOVERBOARD ====================
    class Hoverboard {
        double x, y;
        double vx, vy;
        double angle;
        double maxSpeed = 10.2;
        double accel = 0.32;
        double friction = 0.982;
        double turnSpeed = 0.075;
        Color color, trailColor;
        String name;
        boolean isAI;
        ArrayList<TrailPoint> trail = new ArrayList<>();
        int boostCooldown = 0;
        boolean boosting = false;

        Hoverboard(double x, double y, Color color, Color trailColor, String name, boolean isAI) {
            this.x = x; this.y = y;
            this.color = color; this.trailColor = trailColor;
            this.name = name; this.isAI = isAI;
            this.angle = -Math.PI / 2;
        }

        void update() {
            if (boostCooldown > 0) boostCooldown--;
            vx *= friction;
            vy *= friction;
            x += vx;
            y += vy;

            // Soft boundaries
            if (x < 110) { x = 110; vx *= -0.5; }
            if (x > WIDTH - 110) { x = WIDTH - 110; vx *= -0.5; }
            if (y < 60)  { y = 60;  vy *= -0.4; }
            if (y > HEIGHT - 50) { y = HEIGHT - 50; vy *= -0.4; }

            trail.add(new TrailPoint(x, y, trailColor));
            if (trail.size() > 32) trail.remove(0);

            if (boosting && rand.nextInt(2) == 0) {
                particles.add(new Particle(x, y, trailColor, 2 + rand.nextDouble() * 2.5));
            }
        }

        void accelerate() {
            vx += Math.cos(angle) * accel;
            vy += Math.sin(angle) * accel;
            double spd = Math.hypot(vx, vy);
            if (spd > maxSpeed) {
                vx = (vx / spd) * maxSpeed;
                vy = (vy / spd) * maxSpeed;
            }
        }

        void boost() {
            if (boostCooldown <= 0) {
                vx += Math.cos(angle) * 5.2;
                vy += Math.sin(angle) * 5.2;
                boostCooldown = 85;
                boosting = true;
                for (int i = 0; i < 22; i++) {
                    particles.add(new Particle(x, y, trailColor, 3.5 + rand.nextDouble() * 3));
                }
            }
        }

        void turn(double dir) {
            angle += dir * turnSpeed;
        }

        void draw(Graphics2D g) {
            // Glowing trail
            for (int i = 0; i < trail.size(); i++) {
                TrailPoint t = trail.get(i);
                float a = (float) i / trail.size() * 0.55f;
                g.setColor(new Color(t.color.getRed(), t.color.getGreen(), t.color.getBlue(), (int)(a * 255)));
                g.fillOval((int)t.x - 5, (int)t.y - 5, 10, 10);
            }

            AffineTransform old = g.getTransform();
            g.translate(x, y);
            g.rotate(angle);

            // Outer glow
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
            g.fillRoundRect(-32, -14, 64, 28, 14, 14);

            // Board body
            GradientPaint gp = new GradientPaint(-28, 0, color.brighter(), 28, 0, color.darker());
            g.setPaint(gp);
            g.fillRoundRect(-28, -10, 56, 20, 12, 12);

            // Neon rim
            g.setStroke(new BasicStroke(2.8f));
            g.setColor(Color.WHITE);
            g.drawRoundRect(-28, -10, 56, 20, 12, 12);

            // Front light
            g.setColor(new Color(255, 255, 200));
            g.fillOval(20, -6, 12, 12);

            // Rider
            g.setColor(new Color(25, 25, 35));
            g.fillOval(-9, -20, 18, 15);

            g.setTransform(old);

            // Name
            g.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g.setColor(color);
            g.drawString(name + (boosting ? " ⚡" : ""), (int)x - 28, (int)y - 32);
        }
    }

    class TrailPoint {
        double x, y; Color color;
        TrailPoint(double x, double y, Color c) { this.x = x; this.y = y; this.color = c; }
    }

    // ==================== PARTICLES & SCENERY ====================
    class Particle {
        double x, y, vx, vy;
        Color color;
        float life = 1f, size;
        Particle(double x, double y, Color c, double spd) {
            this.x = x; this.y = y; this.color = c;
            double a = rand.nextDouble() * Math.PI * 2;
            vx = Math.cos(a) * spd; vy = Math.sin(a) * spd;
            size = 3 + rand.nextFloat() * 6;
        }
        void update() { x += vx; y += vy; life -= 0.022f; size *= 0.965f; }
        void draw(Graphics2D g) {
            if (life <= 0) return;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(life * 230)));
            g.fillOval((int)(x - size/2), (int)(y - size/2), (int)size, (int)size);
        }
    }

    class SnowFlake {
        float x, y, size, speed, drift;
        SnowFlake(float x, float y, float size, float speed) {
            this.x = x; this.y = y; this.size = size; this.speed = speed;
            this.drift = (rand.nextFloat() - 0.5f) * 0.8f;
        }
        void update() {
            y += speed;
            x += drift + Math.sin(y * 0.02) * 0.4;
            if (y > HEIGHT + 10) {
                y = -10;
                x = rand.nextInt(WIDTH);
            }
        }
        void draw(Graphics2D g) {
            g.setColor(new Color(255, 255, 255, 180));
            g.fillOval((int)x, (int)y, (int)size, (int)size);
        }
    }

    class Cloud {
        float x, y, w, speed;
        Cloud(float x, float y, float w, float speed) {
            this.x = x; this.y = y; this.w = w; this.speed = speed;
        }
        void update() {
            x += speed * 0.4f;
            if (x > WIDTH + 200) x = -250;
        }
        void draw(Graphics2D g) {
            g.setColor(new Color(255, 255, 255, 70));
            g.fillOval((int)x, (int)y, (int)w, (int)(w * 0.45));
            g.fillOval((int)(x + w*0.25), (int)(y - 15), (int)(w*0.6), (int)(w*0.4));
            g.fillOval((int)(x + w*0.5), (int)y, (int)(w*0.55), (int)(w*0.42));
        }
    }

    class Tree {
        float x, y, h;
        boolean leftSide;
        Tree(float x, float y, float h, boolean left) {
            this.x = x; this.y = y; this.h = h; this.leftSide = left;
        }
        void update(double scroll) {
            y += scroll * 0.9;
            if (y > HEIGHT + 100) {
                y = -150 - rand.nextInt(200);
                if (leftSide) x = 25 + rand.nextInt(100);
                else x = WIDTH - 130 - rand.nextInt(100);
            }
        }
        void draw(Graphics2D g) {
            // Trunk
            g.setColor(new Color(60, 35, 20));
            g.fillRect((int)(x + h*0.35), (int)(y + h*0.55), (int)(h*0.18), (int)(h*0.45));

            // Foliage layers (pine)
            g.setColor(pineGreen);
            int[] xPoints = {(int)x, (int)(x + h*0.5), (int)(x + h)};
            int[] y1 = {(int)(y + h*0.55), (int)y, (int)(y + h*0.55)};
            g.fillPolygon(xPoints, y1, 3);

            g.setColor(new Color(15, 70, 35));
            int[] y2 = {(int)(y + h*0.7), (int)(y + h*0.25), (int)(y + h*0.7)};
            g.fillPolygon(xPoints, y2, 3);

            // Snow on tips
            g.setColor(snowWhite);
            g.fillOval((int)(x + h*0.38), (int)(y - 8), 14, 10);
        }
    }

    class MountainLayer {
        double parallax;
        Color color;
        int baseHeight;
        int[] peaks;
        MountainLayer(double parallax, Color color, int baseHeight) {
            this.parallax = parallax;
            this.color = color;
            this.baseHeight = baseHeight;
            peaks = new int[18];
            for (int i = 0; i < peaks.length; i++) {
                peaks[i] = 80 + rand.nextInt(160);
            }
        }
        void draw(Graphics2D g, double scroll) {
            g.setColor(color);
            Path2D path = new Path2D.Double();
            path.moveTo(-50, HEIGHT);
            double offset = (scroll * parallax) % 400;
            for (int i = 0; i < peaks.length; i++) {
                double px = i * 90 - offset - 100;
                double py = HEIGHT - baseHeight - peaks[i];
                path.lineTo(px, py);
            }
            path.lineTo(WIDTH + 100, HEIGHT);
            path.closePath();
            g.fill(path);

            // Snow caps
            g.setColor(new Color(240, 248, 255, 160));
            for (int i = 1; i < peaks.length - 1; i++) {
                double px = i * 90 - offset - 100;
                double py = HEIGHT - baseHeight - peaks[i];
                g.fillOval((int)px - 25, (int)py - 15, 50, 28);
            }
        }
    }

    // ==================== GAME LOGIC ====================
    void startRace(RaceMode mode) {
        raceMode = mode;
        gameState = GameMode.PLAYING;
        raceFinished = false;
        winnerText = "";
        scoreP1 = scoreP2 = 0;
        particles.clear();

        player1 = new Hoverboard(WIDTH/2 - 140, HEIGHT - 120, neonCyan, neonCyan, "PLAYER 1", false);
        if (mode == RaceMode.VS_COMPUTER) {
            player2 = new Hoverboard(WIDTH/2 + 140, HEIGHT - 120, neonPink, neonPink, "CPU", true);
        } else {
            player2 = new Hoverboard(WIDTH/2 + 140, HEIGHT - 120, neonPink, neonPink, "PLAYER 2", false);
        }
    }

    void updateAI() {
        if (!player2.isAI) return;

        // AI tries to stay in the middle of the mountain road and push forward
        double targetX = WIDTH / 2 + Math.sin(System.currentTimeMillis() * 0.0008) * 160;
        double dx = targetX - player2.x;
        double dy = 180 - player2.y;          // prefers upper half

        double desired = Math.atan2(dy, dx);
        double diff = desired - player2.angle;
        while (diff > Math.PI)  diff -= Math.PI * 2;
        while (diff < -Math.PI) diff += Math.PI * 2;

        if (Math.abs(diff) > 0.08) player2.turn(Math.signum(diff));
        player2.accelerate();
        if (rand.nextInt(160) == 0) player2.boost();
    }

    // ==================== RENDERING ====================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // === COLORADO SKY ===
        GradientPaint sky = new GradientPaint(0, 0, skyTop, 0, HEIGHT * 0.65f, skyBottom);
        g2.setPaint(sky);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Sun glow
        g2.setColor(new Color(255, 220, 120, 40));
        g2.fillOval(WIDTH - 280, 40, 200, 200);

        // Mountains (back to front)
        for (MountainLayer m : mountains) {
            m.draw(g2, scrollY);
        }

        // Clouds
        for (Cloud c : clouds) c.draw(g2);

        // Road / trail surface
        g2.setColor(new Color(55, 50, 45));
        g2.fillRect(100, 0, WIDTH - 200, HEIGHT);

        // Road markings
        g2.setColor(new Color(255, 220, 80, 160));
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{30, 25}, 0));
        double markOffset = scrollY * 1.8 % 55;
        for (int y = -50; y < HEIGHT + 50; y += 55) {
            g2.drawLine(WIDTH/2, (int)(y + markOffset), WIDTH/2, (int)(y + 28 + markOffset));
        }

        // Side snow banks
        g2.setColor(new Color(230, 240, 255, 200));
        g2.fillRect(90, 0, 25, HEIGHT);
        g2.fillRect(WIDTH - 115, 0, 25, HEIGHT);

        // Trees
        for (Tree t : trees) t.draw(g2);

        if (gameState == GameMode.MENU) {
            drawMenu(g2);
        } else {
            // Particles
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.update();
                p.draw(g2);
                if (p.life <= 0) it.remove();
            }

            // Snow
            for (SnowFlake s : snow) {
                s.update();
                s.draw(g2);
            }

            player1.draw(g2);
            player2.draw(g2);
            drawHUD(g2);

            if (raceFinished) {
                g2.setColor(new Color(0, 10, 30, 200));
                g2.fillRect(0, 0, WIDTH, HEIGHT);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 56));
                g2.setColor(goldAspen);
                FontMetrics fm = g2.getFontMetrics();
                int w = fm.stringWidth(winnerText);
                g2.drawString(winnerText, (WIDTH - w)/2, HEIGHT/2 - 30);

                g2.setFont(new Font("Segoe UI", Font.PLAIN, 24));
                g2.setColor(Color.WHITE);
                String sub = "Press ENTER to return to Menu";
                w = g2.getFontMetrics().stringWidth(sub);
                g2.drawString(sub, (WIDTH - w)/2, HEIGHT/2 + 40);
            }
        }
    }

    void drawMenu(Graphics2D g) {
        // Title
        g.setFont(new Font("Segoe UI", Font.BOLD, 52));
        String title = "HOVERBOARD RACING";
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(title);

        g.setColor(new Color(0, 200, 255, 50));
        g.drawString(title, (WIDTH-tw)/2 - 3, 150);
        g.setColor(new Color(255, 80, 160, 60));
        g.drawString(title, (WIDTH-tw)/2 + 3, 154);
        g.setColor(Color.WHITE);
        g.drawString(title, (WIDTH-tw)/2, 152);

        g.setFont(new Font("Segoe UI", Font.BOLD, 26));
        g.setColor(goldAspen);
        String sub = "COLORADO MOUNTAINS";
        g.drawString(sub, (WIDTH - g.getFontMetrics().stringWidth(sub))/2, 200);

        drawButton(g, "1   PLAY  vs  COMPUTER", WIDTH/2, 340, neonCyan);
        drawButton(g, "2   PLAY  vs  PLAYER 2", WIDTH/2, 420, neonPink);

        g.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        g.setColor(new Color(220, 230, 255));
        g.drawString("Player 1:  W A S D  +  SPACE (boost)", 70, HEIGHT - 90);
        g.drawString("Player 2:  ↑ ← ↓ →  +  ENTER (boost)", 70, HEIGHT - 65);
        g.drawString("ESC – Return to Menu", 70, HEIGHT - 40);
    }

    void drawButton(Graphics2D g, String text, int cx, int cy, Color col) {
        int bw = 460, bh = 58;
        int x = cx - bw/2, y = cy - bh/2;
        g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 45));
        g.fillRoundRect(x-8, y-8, bw+16, bh+16, 22, 22);
        g.setColor(new Color(15, 20, 40, 230));
        g.fillRoundRect(x, y, bw, bh, 16, 16);
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(col);
        g.drawRoundRect(x, y, bw, bh, 16, 16);
        g.setFont(new Font("Segoe UI", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, cx - fm.stringWidth(text)/2, cy + 8);
    }

    void drawHUD(Graphics2D g) {
        g.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g.setColor(neonCyan);
        g.drawString("P1  " + scoreP1 + "  |  Speed " + String.format("%.0f", Math.hypot(player1.vx, player1.vy)*11), 30, 36);

        g.setColor(neonPink);
        String p2name = player2.isAI ? "CPU" : "P2";
        g.drawString(p2name + "  " + scoreP2 + "  |  Speed " + String.format("%.0f", Math.hypot(player2.vx, player2.vy)*11), WIDTH - 280, 36);

        drawBoostBar(g, 30, 50, player1.boostCooldown, neonCyan);
        drawBoostBar(g, WIDTH - 180, 50, player2.boostCooldown, neonPink);
    }

    void drawBoostBar(Graphics2D g, int x, int y, int cd, Color c) {
        g.setColor(new Color(30, 30, 50));
        g.fillRoundRect(x, y, 140, 13, 6, 6);
        float pct = Math.max(0, 1f - cd / 85f);
        g.setColor(c);
        g.fillRoundRect(x, y, (int)(140 * pct), 13, 6, 6);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g.drawString("BOOST", x + 48, y + 11);
    }

    // ==================== UPDATE ====================
    @Override
    public void actionPerformed(ActionEvent e) {
        scrollY += 3.2;          // continuous mountain scroll

        // Update scenery
        for (Cloud c : clouds) c.update();
        for (Tree t : trees) t.update(3.2);

        if (gameState == GameMode.PLAYING && !raceFinished) {
            // Player 1
            if (keys[KeyEvent.VK_W]) player1.accelerate();
            if (keys[KeyEvent.VK_A]) player1.turn(-1);
            if (keys[KeyEvent.VK_D]) player1.turn(1);
            if (keys[KeyEvent.VK_SPACE]) player1.boost();
            player1.boosting = keys[KeyEvent.VK_SPACE] && player1.boostCooldown > 65;

            // Player 2 / AI
            if (raceMode == RaceMode.VS_PLAYER) {
                if (keys[KeyEvent.VK_UP])    player2.accelerate();
                if (keys[KeyEvent.VK_LEFT])  player2.turn(-1);
                if (keys[KeyEvent.VK_RIGHT]) player2.turn(1);
                if (keys[KeyEvent.VK_ENTER]) player2.boost();
                player2.boosting = keys[KeyEvent.VK_ENTER] && player2.boostCooldown > 65;
            } else {
                updateAI();
            }

            player1.update();
            player2.update();

            // Scoring – race uphill (lower Y = higher score)
            if (player1.y < 140 && player1.vy < -1.5) scoreP1++;
            if (player2.y < 140 && player2.vy < -1.5) scoreP2++;

            if (scoreP1 >= winScore) {
                raceFinished = true;
                winnerText = "PLAYER 1 WINS!";
                gameState = GameMode.GAME_OVER;
            } else if (scoreP2 >= winScore) {
                raceFinished = true;
                winnerText = player2.isAI ? "CPU WINS!" : "PLAYER 2 WINS!";
                gameState = GameMode.GAME_OVER;
            }
        }
        repaint();
    }

    // ==================== INPUT ====================
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k < keys.length) keys[k] = true;

        if (gameState == GameMode.MENU) {
            if (k == KeyEvent.VK_1) startRace(RaceMode.VS_COMPUTER);
            if (k == KeyEvent.VK_2) startRace(RaceMode.VS_PLAYER);
        }
        if (gameState == GameMode.GAME_OVER && k == KeyEvent.VK_ENTER) {
            gameState = GameMode.MENU;
        }
        if (k == KeyEvent.VK_ESCAPE) gameState = GameMode.MENU;
    }

    @Override public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = false;
    }
    @Override public void keyTyped(KeyEvent e) {}

    // ==================== MAIN ====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hoverboard Racing Adventure – Colorado Mountains");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new HoverboardRacingAdventure());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}