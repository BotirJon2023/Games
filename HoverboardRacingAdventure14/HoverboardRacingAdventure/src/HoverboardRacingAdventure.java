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
    ArrayList<PrayerFlag> flags = new ArrayList<>();

    double scrollY = 0;
    int scoreP1 = 0, scoreP2 = 0;
    int winScore = 8;
    boolean raceFinished = false;
    String winnerText = "";

    boolean[] keys = new boolean[512];

    // Himalaya color palette
    Color skyTop      = new Color(20, 50, 120);       // deep high-altitude blue
    Color skyBottom   = new Color(135, 190, 255);     // crisp mountain sky
    Color snowWhite   = new Color(250, 252, 255);
    Color rockGray    = new Color(85, 90, 105);
    Color pineGreen   = new Color(20, 70, 40);
    Color neonCyan    = new Color(0, 255, 255);
    Color neonPink    = new Color(255, 50, 150);
    Color neonGold    = new Color(255, 210, 60);
    Color flagBlue    = new Color(30, 100, 220);
    Color flagRed     = new Color(220, 40, 40);
    Color flagGreen   = new Color(30, 180, 70);
    Color flagYellow  = new Color(255, 220, 40);

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
        // Massive Himalayan peaks (parallax)
        mountains.add(new MountainLayer(0.10, new Color(40, 55, 90), 140));   // far distant giants
        mountains.add(new MountainLayer(0.25, new Color(55, 65, 95), 200));   // mid range
        mountains.add(new MountainLayer(0.48, new Color(70, 80, 100), 270));  // near towering

        // High clouds
        for (int i = 0; i < 12; i++) {
            clouds.add(new Cloud(rand.nextInt(WIDTH + 500) - 250,
                    15 + rand.nextInt(130),
                    90 + rand.nextInt(140),
                    0.2f + rand.nextFloat() * 0.5f));
        }

        // Heavy Himalayan snow
        for (int i = 0; i < 240; i++) {
            snow.add(new SnowFlake(rand.nextInt(WIDTH), rand.nextInt(HEIGHT),
                    1.3f + rand.nextFloat() * 3.5f,
                    0.6f + rand.nextFloat() * 1.7f));
        }

        // Sparse high-altitude trees
        for (int i = 0; i < 32; i++) {
            trees.add(new Tree(20 + rand.nextInt(100),
                    rand.nextInt(HEIGHT + 450) - 220,
                    45 + rand.nextInt(70), true));
            trees.add(new Tree(WIDTH - 135 - rand.nextInt(100),
                    rand.nextInt(HEIGHT + 450) - 220,
                    45 + rand.nextInt(70), false));
        }

        // Colorful prayer flags
        for (int i = 0; i < 18; i++) {
            flags.add(new PrayerFlag(40 + rand.nextInt(90),
                    rand.nextInt(HEIGHT + 300) - 150, true));
            flags.add(new PrayerFlag(WIDTH - 130 - rand.nextInt(90),
                    rand.nextInt(HEIGHT + 300) - 150, false));
        }
    }

    // ==================== HOVERBOARD ====================
    class Hoverboard {
        double x, y;
        double vx, vy;
        double angle;
        double maxSpeed = 10.6;
        double accel = 0.35;
        double friction = 0.979;
        double turnSpeed = 0.082;
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

            if (x < 125) { x = 125; vx *= -0.5; }
            if (x > WIDTH - 125) { x = WIDTH - 125; vx *= -0.5; }
            if (y < 45)  { y = 45;  vy *= -0.4; }
            if (y > HEIGHT - 40) { y = HEIGHT - 40; vy *= -0.4; }

            trail.add(new TrailPoint(x, y, trailColor));
            if (trail.size() > 38) trail.remove(0);

            if (boosting && rand.nextInt(2) == 0) {
                particles.add(new Particle(x, y, trailColor, 2.5 + rand.nextDouble() * 3.2));
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
                vx += Math.cos(angle) * 5.8;
                vy += Math.sin(angle) * 5.8;
                boostCooldown = 78;
                boosting = true;
                for (int i = 0; i < 28; i++) {
                    particles.add(new Particle(x, y, trailColor, 4.2 + rand.nextDouble() * 3.8));
                }
            }
        }

        void turn(double dir) {
            angle += dir * turnSpeed;
        }

        void draw(Graphics2D g) {
            for (int i = 0; i < trail.size(); i++) {
                TrailPoint t = trail.get(i);
                float a = (float) i / trail.size() * 0.62f;
                g.setColor(new Color(t.color.getRed(), t.color.getGreen(), t.color.getBlue(), (int)(a * 255)));
                g.fillOval((int)t.x - 5, (int)t.y - 5, 10, 10);
            }

            AffineTransform old = g.getTransform();
            g.translate(x, y);
            g.rotate(angle);

            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 85));
            g.fillRoundRect(-35, -16, 70, 32, 16, 16);

            GradientPaint gp = new GradientPaint(-31, 0, color.brighter(), 31, 0, color.darker());
            g.setPaint(gp);
            g.fillRoundRect(-31, -12, 62, 24, 14, 14);

            g.setStroke(new BasicStroke(2.8f));
            g.setColor(Color.WHITE);
            g.drawRoundRect(-31, -12, 62, 24, 14, 14);

            g.setColor(new Color(255, 255, 220));
            g.fillOval(23, -7, 14, 14);

            g.setColor(new Color(25, 30, 45));
            g.fillOval(-10, -23, 20, 16);

            g.setTransform(old);

            g.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g.setColor(color);
            g.drawString(name + (boosting ? " ⚡" : ""), (int)x - 32, (int)y - 36);
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
            size = 3.8f + rand.nextFloat() * 7f;
        }
        void update() { x += vx; y += vy; life -= 0.019f; size *= 0.96f; }
        void draw(Graphics2D g) {
            if (life <= 0) return;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(life * 235)));
            g.fillOval((int)(x - size/2), (int)(y - size/2), (int)size, (int)size);
        }
    }

    class SnowFlake {
        float x, y, size, speed, drift;
        SnowFlake(float x, float y, float size, float speed) {
            this.x = x; this.y = y; this.size = size; this.speed = speed;
            this.drift = (rand.nextFloat() - 0.5f) * 1.2f;
        }
        void update() {
            y += speed;
            x += drift + Math.sin(y * 0.015) * 0.6;
            if (y > HEIGHT + 15) {
                y = -15;
                x = rand.nextInt(WIDTH);
            }
        }
        void draw(Graphics2D g) {
            g.setColor(new Color(255, 255, 255, 200));
            g.fillOval((int)x, (int)y, (int)size, (int)size);
        }
    }

    class Cloud {
        float x, y, w, speed;
        Cloud(float x, float y, float w, float speed) {
            this.x = x; this.y = y; this.w = w; this.speed = speed;
        }
        void update() {
            x += speed * 0.28f;
            if (x > WIDTH + 260) x = -300;
        }
        void draw(Graphics2D g) {
            g.setColor(new Color(255, 255, 255, 75));
            g.fillOval((int)x, (int)y, (int)w, (int)(w * 0.38));
            g.fillOval((int)(x + w*0.2), (int)(y - 14), (int)(w*0.55), (int)(w*0.33));
            g.fillOval((int)(x + w*0.45), (int)y + 3, (int)(w*0.48), (int)(w*0.36));
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
            if (y > HEIGHT + 120) {
                y = -160 - rand.nextInt(180);
                if (leftSide) x = 15 + rand.nextInt(110);
                else x = WIDTH - 145 - rand.nextInt(110);
            }
        }
        void draw(Graphics2D g) {
            g.setColor(new Color(55, 35, 20));
            g.fillRect((int)(x + h*0.38), (int)(y + h*0.55), (int)(h*0.16), (int)(h*0.45));

            g.setColor(pineGreen);
            int[] xp = {(int)x, (int)(x + h*0.5), (int)(x + h)};
            int[] y1 = {(int)(y + h*0.55), (int)y, (int)(y + h*0.55)};
            g.fillPolygon(xp, y1, 3);

            g.setColor(new Color(12, 55, 30));
            int[] y2 = {(int)(y + h*0.7), (int)(y + h*0.2), (int)(y + h*0.7)};
            g.fillPolygon(xp, y2, 3);

            // Snow on branches
            g.setColor(snowWhite);
            g.fillOval((int)(x + h*0.35), (int)(y - 8), 15, 11);
        }
    }

    class PrayerFlag {
        float x, y;
        boolean leftSide;
        Color[] colors = {flagBlue, flagWhite(), flagRed, flagGreen, flagYellow};
        PrayerFlag(float x, float y, boolean left) {
            this.x = x; this.y = y; this.leftSide = left;
        }
        Color flagWhite() { return Color.WHITE; }
        void update(double scroll) {
            y += scroll * 0.85;
            if (y > HEIGHT + 80) {
                y = -100 - rand.nextInt(150);
                if (leftSide) x = 25 + rand.nextInt(90);
                else x = WIDTH - 140 - rand.nextInt(90);
            }
        }
        void draw(Graphics2D g) {
            for (int i = 0; i < 5; i++) {
                g.setColor(colors[i]);
                g.fillRect((int)x, (int)(y + i * 12), 28, 10);
            }
            // Pole
            g.setColor(new Color(60, 40, 20));
            g.fillRect((int)x + 12, (int)y - 5, 3, 70);
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
            peaks = new int[20];
            for (int i = 0; i < peaks.length; i++) {
                peaks[i] = 100 + rand.nextInt(190);   // very tall Himalayan peaks
            }
        }
        void draw(Graphics2D g, double scroll) {
            g.setColor(color);
            Path2D path = new Path2D.Double();
            path.moveTo(-80, HEIGHT);
            double offset = (scroll * parallax) % 420;
            for (int i = 0; i < peaks.length; i++) {
                double px = i * 88 - offset - 140;
                double py = HEIGHT - baseHeight - peaks[i];
                path.lineTo(px, py);
            }
            path.lineTo(WIDTH + 140, HEIGHT);
            path.closePath();
            g.fill(path);

            // Heavy snow caps
            g.setColor(new Color(250, 252, 255, 190));
            for (int i = 1; i < peaks.length - 1; i++) {
                double px = i * 88 - offset - 140;
                double py = HEIGHT - baseHeight - peaks[i];
                g.fillOval((int)px - 30, (int)py - 20, 60, 36);
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

        player1 = new Hoverboard(WIDTH/2 - 155, HEIGHT - 135, neonCyan, neonCyan, "PLAYER 1", false);
        if (mode == RaceMode.VS_COMPUTER) {
            player2 = new Hoverboard(WIDTH/2 + 155, HEIGHT - 135, neonPink, neonPink, "CPU", true);
        } else {
            player2 = new Hoverboard(WIDTH/2 + 155, HEIGHT - 135, neonPink, neonPink, "PLAYER 2", false);
        }
    }

    void updateAI() {
        if (!player2.isAI) return;

        double targetX = WIDTH / 2 + Math.sin(System.currentTimeMillis() * 0.0007) * 145;
        double dx = targetX - player2.x;
        double dy = 155 - player2.y;

        double desired = Math.atan2(dy, dx);
        double diff = desired - player2.angle;
        while (diff > Math.PI)  diff -= Math.PI * 2;
        while (diff < -Math.PI) diff += Math.PI * 2;

        if (Math.abs(diff) > 0.06) player2.turn(Math.signum(diff));
        player2.accelerate();
        if (rand.nextInt(140) == 0) player2.boost();
    }

    // ==================== RENDERING ====================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // === HIMALAYAN HIGH-ALTITUDE SKY ===
        GradientPaint sky = new GradientPaint(0, 0, skyTop, 0, HEIGHT * 0.7f, skyBottom);
        g2.setPaint(sky);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Soft sun
        g2.setColor(new Color(255, 240, 180, 35));
        g2.fillOval(WIDTH - 300, 20, 240, 200);

        // Mountains
        for (MountainLayer m : mountains) {
            m.draw(g2, scrollY);
        }

        // Clouds
        for (Cloud c : clouds) c.draw(g2);

        // Mountain road
        g2.setColor(new Color(70, 75, 90));
        g2.fillRect(115, 0, WIDTH - 230, HEIGHT);

        // Road markings
        g2.setColor(new Color(255, 255, 255, 160));
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{30, 25}, 0));
        double markOffset = scrollY * 2.1 % 55;
        for (int y = -55; y < HEIGHT + 55; y += 55) {
            g2.drawLine(WIDTH/2, (int)(y + markOffset), WIDTH/2, (int)(y + 28 + markOffset));
        }

        // Snow banks
        g2.setColor(new Color(245, 250, 255, 220));
        g2.fillRect(105, 0, 28, HEIGHT);
        g2.fillRect(WIDTH - 133, 0, 28, HEIGHT);

        // Trees & prayer flags
        for (Tree t : trees) t.draw(g2);
        for (PrayerFlag f : flags) f.draw(g2);

        if (gameState == GameMode.MENU) {
            drawMenu(g2);
        } else {
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.update();
                p.draw(g2);
                if (p.life <= 0) it.remove();
            }

            for (SnowFlake s : snow) {
                s.update();
                s.draw(g2);
            }

            player1.draw(g2);
            player2.draw(g2);
            drawHUD(g2);

            if (raceFinished) {
                g2.setColor(new Color(10, 20, 50, 215));
                g2.fillRect(0, 0, WIDTH, HEIGHT);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 54));
                g2.setColor(neonGold);
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
        g.setFont(new Font("Segoe UI", Font.BOLD, 48));
        String title = "HOVERBOARD RACING";
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(title);

        g.setColor(new Color(0, 220, 255, 50));
        g.drawString(title, (WIDTH-tw)/2 - 3, 140);
        g.setColor(new Color(255, 60, 150, 50));
        g.drawString(title, (WIDTH-tw)/2 + 3, 144);
        g.setColor(Color.WHITE);
        g.drawString(title, (WIDTH-tw)/2, 142);

        g.setFont(new Font("Segoe UI", Font.BOLD, 26));
        g.setColor(neonGold);
        String sub = "HIMALAYA MOUNTAINS";
        g.drawString(sub, (WIDTH - g.getFontMetrics().stringWidth(sub))/2, 190);

        drawButton(g, "1   PLAY  vs  COMPUTER", WIDTH/2, 340, neonCyan);
        drawButton(g, "2   PLAY  vs  PLAYER 2", WIDTH/2, 420, neonPink);

        g.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        g.setColor(new Color(220, 235, 255));
        g.drawString("Player 1:  W A S D  +  SPACE (boost)", 70, HEIGHT - 90);
        g.drawString("Player 2:  ↑ ← ↓ →  +  ENTER (boost)", 70, HEIGHT - 65);
        g.drawString("ESC – Return to Menu", 70, HEIGHT - 40);
    }

    void drawButton(Graphics2D g, String text, int cx, int cy, Color col) {
        int bw = 460, bh = 58;
        int x = cx - bw/2, y = cy - bh/2;
        g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 50));
        g.fillRoundRect(x-8, y-8, bw+16, bh+16, 22, 22);
        g.setColor(new Color(15, 25, 50, 235));
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
        g.setColor(new Color(30, 35, 60));
        g.fillRoundRect(x, y, 140, 13, 6, 6);
        float pct = Math.max(0, 1f - cd / 78f);
        g.setColor(c);
        g.fillRoundRect(x, y, (int)(140 * pct), 13, 6, 6);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g.drawString("BOOST", x + 48, y + 11);
    }

    // ==================== UPDATE ====================
    @Override
    public void actionPerformed(ActionEvent e) {
        scrollY += 3.6;

        for (Cloud c : clouds) c.update();
        for (Tree t : trees) t.update(3.6);
        for (PrayerFlag f : flags) f.update(3.6);

        if (gameState == GameMode.PLAYING && !raceFinished) {
            if (keys[KeyEvent.VK_W]) player1.accelerate();
            if (keys[KeyEvent.VK_A]) player1.turn(-1);
            if (keys[KeyEvent.VK_D]) player1.turn(1);
            if (keys[KeyEvent.VK_SPACE]) player1.boost();
            player1.boosting = keys[KeyEvent.VK_SPACE] && player1.boostCooldown > 55;

            if (raceMode == RaceMode.VS_PLAYER) {
                if (keys[KeyEvent.VK_UP])    player2.accelerate();
                if (keys[KeyEvent.VK_LEFT])  player2.turn(-1);
                if (keys[KeyEvent.VK_RIGHT]) player2.turn(1);
                if (keys[KeyEvent.VK_ENTER]) player2.boost();
                player2.boosting = keys[KeyEvent.VK_ENTER] && player2.boostCooldown > 55;
            } else {
                updateAI();
            }

            player1.update();
            player2.update();

            if (player1.y < 125 && player1.vy < -1.5) scoreP1++;
            if (player2.y < 125 && player2.vy < -1.5) scoreP2++;

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
            JFrame frame = new JFrame("Hoverboard Racing Adventure – Himalaya Mountains");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new HoverboardRacingAdventure());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}