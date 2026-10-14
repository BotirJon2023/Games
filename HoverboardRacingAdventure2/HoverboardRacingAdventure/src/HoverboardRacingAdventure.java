import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class HoverboardRacingAdventure extends JPanel implements ActionListener, KeyListener {

    // ==================== GAME CONSTANTS ====================
    static final int WIDTH = 1200;
    static final int HEIGHT = 700;
    static final int FPS = 60;

    // ==================== GAME STATE ====================
    enum GameMode { MENU, PLAYING, GAME_OVER }
    enum RaceMode { VS_COMPUTER, VS_PLAYER }

    GameMode gameState = GameMode.MENU;
    RaceMode raceMode = RaceMode.VS_COMPUTER;

    Timer timer;
    Random rand = new Random();

    // Players
    Hoverboard player1;
    Hoverboard player2;          // used for both 2P and AI

    // Track & effects
    ArrayList<Particle> particles = new ArrayList<>();
    ArrayList<Star> stars = new ArrayList<>();
    double trackOffset = 0;
    int lapP1 = 0, lapP2 = 0;
    int maxLaps = 3;
    boolean raceFinished = false;
    String winnerText = "";

    // Input
    boolean[] keys = new boolean[256];

    // Colors (neon palette)
    Color neonCyan = new Color(0, 255, 255);
    Color neonPink = new Color(255, 20, 147);
    Color neonPurple = new Color(180, 0, 255);
    Color neonGreen = new Color(57, 255, 20);
    Color neonOrange = new Color(255, 140, 0);

    public HoverboardRacingAdventure() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        // Background stars
        for (int i = 0; i < 120; i++) {
            stars.add(new Star(rand.nextInt(WIDTH), rand.nextInt(HEIGHT),
                    1 + rand.nextFloat() * 2.5f, rand.nextFloat() * 0.5f + 0.2f));
        }

        timer = new Timer(1000 / FPS, this);
        timer.start();
    }

    // ==================== HOVERBOARD CLASS ====================
    class Hoverboard {
        double x, y;
        double vx, vy;
        double angle;          // in radians
        double speed;
        double maxSpeed = 9.5;
        double accel = 0.28;
        double friction = 0.985;
        double turnSpeed = 0.07;
        Color color;
        Color trailColor;
        String name;
        boolean isAI;
        ArrayList<TrailPoint> trail = new ArrayList<>();
        int boostCooldown = 0;
        boolean boosting = false;

        Hoverboard(double x, double y, Color color, Color trailColor, String name, boolean isAI) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.trailColor = trailColor;
            this.name = name;
            this.isAI = isAI;
            this.angle = -Math.PI / 2; // facing up
        }

        void update() {
            if (boostCooldown > 0) boostCooldown--;

            // Apply friction
            vx *= friction;
            vy *= friction;

            // Move
            x += vx;
            y += vy;

            // Keep on screen with soft bounce
            if (x < 40) { x = 40; vx *= -0.4; }
            if (x > WIDTH - 40) { x = WIDTH - 40; vx *= -0.4; }
            if (y < 40) { y = 40; vy *= -0.4; }
            if (y > HEIGHT - 40) { y = HEIGHT - 40; vy *= -0.4; }

            // Trail
            trail.add(new TrailPoint(x, y, trailColor));
            if (trail.size() > 28) trail.remove(0);

            // Particles when boosting
            if (boosting && rand.nextInt(2) == 0) {
                particles.add(new Particle(x, y, trailColor, 1.5 + rand.nextDouble() * 2));
            }
        }

        void accelerate() {
            vx += Math.cos(angle) * accel;
            vy += Math.sin(angle) * accel;
            double currentSpeed = Math.hypot(vx, vy);
            if (currentSpeed > maxSpeed) {
                vx = (vx / currentSpeed) * maxSpeed;
                vy = (vy / currentSpeed) * maxSpeed;
            }
        }

        void boost() {
            if (boostCooldown <= 0) {
                vx += Math.cos(angle) * 4.5;
                vy += Math.sin(angle) * 4.5;
                boostCooldown = 90; // 1.5 seconds
                boosting = true;
                // Explosion of particles
                for (int i = 0; i < 18; i++) {
                    particles.add(new Particle(x, y, trailColor, 3 + rand.nextDouble() * 3));
                }
            }
        }

        void turn(double dir) {
            angle += dir * turnSpeed;
        }

        void draw(Graphics2D g) {
            // Trail
            for (int i = 0; i < trail.size(); i++) {
                TrailPoint t = trail.get(i);
                float alpha = (float) i / trail.size() * 0.6f;
                g.setColor(new Color(t.color.getRed(), t.color.getGreen(), t.color.getBlue(), (int)(alpha * 255)));
                g.fillOval((int)t.x - 4, (int)t.y - 4, 8, 8);
            }

            // Hoverboard body
            AffineTransform old = g.getTransform();
            g.translate(x, y);
            g.rotate(angle);

            // Glow
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
            g.fillRoundRect(-28, -12, 56, 24, 12, 12);

            // Main board
            GradientPaint gp = new GradientPaint(-25, 0, color.brighter(), 25, 0, color.darker());
            g.setPaint(gp);
            g.fillRoundRect(-25, -9, 50, 18, 10, 10);

            // Neon edge
            g.setStroke(new BasicStroke(2.5f));
            g.setColor(Color.WHITE);
            g.drawRoundRect(-25, -9, 50, 18, 10, 10);

            // Front light
            g.setColor(Color.WHITE);
            g.fillOval(18, -5, 10, 10);

            // Rider silhouette
            g.setColor(new Color(30, 30, 40));
            g.fillOval(-8, -18, 16, 14);

            g.setTransform(old);

            // Name tag
            g.setFont(new Font("Arial", Font.BOLD, 13));
            g.setColor(color);
            String label = name + (boosting ? " ⚡" : "");
            g.drawString(label, (int)x - 25, (int)y - 28);
        }
    }

    class TrailPoint {
        double x, y;
        Color color;
        TrailPoint(double x, double y, Color c) { this.x = x; this.y = y; this.color = c; }
    }

    class Particle {
        double x, y, vx, vy;
        Color color;
        float life = 1.0f;
        float size;

        Particle(double x, double y, Color c, double speed) {
            this.x = x; this.y = y; this.color = c;
            double a = rand.nextDouble() * Math.PI * 2;
            this.vx = Math.cos(a) * speed;
            this.vy = Math.sin(a) * speed;
            this.size = 3 + rand.nextFloat() * 5;
        }

        void update() {
            x += vx; y += vy;
            life -= 0.025f;
            size *= 0.97f;
        }

        void draw(Graphics2D g) {
            if (life <= 0) return;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(life * 220)));
            g.fillOval((int)(x - size/2), (int)(y - size/2), (int)size, (int)size);
        }
    }

    class Star {
        float x, y, size, speed;
        Star(float x, float y, float size, float speed) {
            this.x = x; this.y = y; this.size = size; this.speed = speed;
        }
    }

    // ==================== GAME LOGIC ====================
    void startRace(RaceMode mode) {
        raceMode = mode;
        gameState = GameMode.PLAYING;
        raceFinished = false;
        winnerText = "";
        lapP1 = 0; lapP2 = 0;
        particles.clear();

        player1 = new Hoverboard(WIDTH / 2 - 120, HEIGHT - 100, neonCyan, neonCyan, "PLAYER 1", false);
        if (mode == RaceMode.VS_COMPUTER) {
            player2 = new Hoverboard(WIDTH / 2 + 120, HEIGHT - 100, neonPink, neonPink, "CPU", true);
        } else {
            player2 = new Hoverboard(WIDTH / 2 + 120, HEIGHT - 100, neonPink, neonPink, "PLAYER 2", false);
        }
    }

    void updateAI() {
        if (!player2.isAI) return;

        // Simple but fun AI: try to stay near center and accelerate + occasional boost
        double targetX = WIDTH / 2 + Math.sin(System.currentTimeMillis() * 0.001) * 180;
        double dx = targetX - player2.x;
        double dy = (HEIGHT / 2 - 80) - player2.y;

        double desiredAngle = Math.atan2(dy, dx);
        double angleDiff = desiredAngle - player2.angle;

        // Normalize angle difference
        while (angleDiff > Math.PI) angleDiff -= Math.PI * 2;
        while (angleDiff < -Math.PI) angleDiff += Math.PI * 2;

        if (Math.abs(angleDiff) > 0.1) {
            player2.turn(Math.signum(angleDiff));
        }
        player2.accelerate();

        // Random boost
        if (rand.nextInt(180) == 0) player2.boost();
    }

    void checkLaps() {
        // Simple lap detection: crossing the top area
        if (player1.y < 80 && player1.vy < 0) {
            // rough check – in a real track you’d use checkpoints
        }
        // For this demo we use a time-based “progress” + position scoring
    }

    // ==================== RENDERING ====================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Animated starfield background
        g2.setColor(new Color(8, 5, 20));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        for (Star s : stars) {
            g2.setColor(new Color(200, 220, 255, (int)(180 * s.speed)));
            g2.fillOval((int)s.x, (int)s.y, (int)s.size, (int)s.size);
            s.y += s.speed * 1.8f;
            if (s.y > HEIGHT) {
                s.y = -5;
                s.x = rand.nextInt(WIDTH);
            }
        }

        // Glowing track lines
        g2.setStroke(new BasicStroke(3f));
        for (int i = 0; i < 8; i++) {
            float alpha = 0.15f + (float)(Math.sin(System.currentTimeMillis() * 0.003 + i) * 0.1);
            g2.setColor(new Color(0, 200, 255, (int)(alpha * 255)));
            int y = (int)((i * 90 + trackOffset) % (HEIGHT + 100)) - 50;
            g2.drawLine(80, y, WIDTH - 80, y);
        }
        trackOffset += 2.5;

        // Side neon walls
        GradientPaint leftWall = new GradientPaint(0, 0, neonPurple, 60, 0, new Color(0, 0, 0, 0));
        g2.setPaint(leftWall);
        g2.fillRect(0, 0, 60, HEIGHT);
        GradientPaint rightWall = new GradientPaint(WIDTH, 0, neonPink, WIDTH - 60, 0, new Color(0, 0, 0, 0));
        g2.setPaint(rightWall);
        g2.fillRect(WIDTH - 60, 0, 60, HEIGHT);

        if (gameState == GameMode.MENU) {
            drawMenu(g2);
        } else if (gameState == GameMode.PLAYING || gameState == GameMode.GAME_OVER) {
            // Particles
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.update();
                p.draw(g2);
                if (p.life <= 0) it.remove();
            }

            player1.draw(g2);
            player2.draw(g2);

            // HUD
            drawHUD(g2);

            if (raceFinished) {
                g2.setColor(new Color(0, 0, 0, 180));
                g2.fillRect(0, 0, WIDTH, HEIGHT);
                g2.setFont(new Font("Arial", Font.BOLD, 52));
                g2.setColor(neonGreen);
                FontMetrics fm = g2.getFontMetrics();
                int w = fm.stringWidth(winnerText);
                g2.drawString(winnerText, (WIDTH - w) / 2, HEIGHT / 2 - 20);

                g2.setFont(new Font("Arial", Font.PLAIN, 24));
                g2.setColor(Color.WHITE);
                String sub = "Press ENTER to return to Menu";
                w = g2.getFontMetrics().stringWidth(sub);
                g2.drawString(sub, (WIDTH - w) / 2, HEIGHT / 2 + 40);
            }
        }
    }

    void drawMenu(Graphics2D g) {
        // Title with glow
        g.setFont(new Font("Arial", Font.BOLD, 56));
        String title = "HOVERBOARD RACING";
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(title);

        // Glow layers
        g.setColor(new Color(0, 255, 255, 40));
        g.drawString(title, (WIDTH - tw) / 2 - 2, 160);
        g.setColor(new Color(255, 20, 147, 60));
        g.drawString(title, (WIDTH - tw) / 2 + 2, 164);
        g.setColor(Color.WHITE);
        g.drawString(title, (WIDTH - tw) / 2, 162);

        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(neonCyan);
        String sub = "ADVENTURE";
        g.drawString(sub, (WIDTH - g.getFontMetrics().stringWidth(sub)) / 2, 210);

        // Buttons
        drawButton(g, "1  –  PLAY vs COMPUTER", WIDTH / 2, 340, neonCyan);
        drawButton(g, "2  –  PLAY vs PLAYER 2", WIDTH / 2, 420, neonPink);

        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.setColor(new Color(200, 200, 255));
        g.drawString("Player 1:  W A S D  +  SPACE (boost)", 80, HEIGHT - 80);
        g.drawString("Player 2:  ↑ ← ↓ →  +  ENTER (boost)", 80, HEIGHT - 55);
        g.drawString("ESC – Pause / Menu", 80, HEIGHT - 30);
    }

    void drawButton(Graphics2D g, String text, int cx, int cy, Color col) {
        int bw = 420, bh = 55;
        int x = cx - bw / 2;
        int y = cy - bh / 2;

        // Glow
        g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 50));
        g.fillRoundRect(x - 6, y - 6, bw + 12, bh + 12, 20, 20);

        g.setColor(new Color(20, 15, 35));
        g.fillRoundRect(x, y, bw, bh, 15, 15);

        g.setStroke(new BasicStroke(2.5f));
        g.setColor(col);
        g.drawRoundRect(x, y, bw, bh, 15, 15);

        g.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, cx - fm.stringWidth(text) / 2, cy + 8);
    }

    void drawHUD(Graphics2D g) {
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(neonCyan);
        g.drawString("P1 Speed: " + String.format("%.0f", Math.hypot(player1.vx, player1.vy) * 12), 30, 35);

        g.setColor(neonPink);
        g.drawString((player2.isAI ? "CPU" : "P2") + " Speed: " +
                String.format("%.0f", Math.hypot(player2.vx, player2.vy) * 12), WIDTH - 220, 35);

        // Boost bars
        drawBoostBar(g, 30, 50, player1.boostCooldown, neonCyan);
        drawBoostBar(g, WIDTH - 180, 50, player2.boostCooldown, neonPink);
    }

    void drawBoostBar(Graphics2D g, int x, int y, int cooldown, Color c) {
        g.setColor(new Color(40, 40, 60));
        g.fillRoundRect(x, y, 140, 12, 6, 6);
        float pct = 1.0f - (cooldown / 90f);
        if (pct < 0) pct = 0;
        g.setColor(c);
        g.fillRoundRect(x, y, (int)(140 * pct), 12, 6, 6);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.drawString("BOOST", x + 45, y + 11);
    }

    // ==================== UPDATE LOOP ====================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameMode.PLAYING && !raceFinished) {
            // Player 1 controls
            if (keys[KeyEvent.VK_W]) player1.accelerate();
            if (keys[KeyEvent.VK_A]) player1.turn(-1);
            if (keys[KeyEvent.VK_D]) player1.turn(1);
            if (keys[KeyEvent.VK_SPACE]) player1.boost();
            player1.boosting = keys[KeyEvent.VK_SPACE] && player1.boostCooldown > 70;

            // Player 2 / AI
            if (raceMode == RaceMode.VS_PLAYER) {
                if (keys[KeyEvent.VK_UP]) player2.accelerate();
                if (keys[KeyEvent.VK_LEFT]) player2.turn(-1);
                if (keys[KeyEvent.VK_RIGHT]) player2.turn(1);
                if (keys[KeyEvent.VK_ENTER]) player2.boost();
                player2.boosting = keys[KeyEvent.VK_ENTER] && player2.boostCooldown > 70;
            } else {
                updateAI();
            }

            player1.update();
            player2.update();

            // Simple win condition: first to stay high (top of screen) longer or reach a score
            // For demo: whoever is higher after some time, or first to cross top many times
            // Better: use a distance / progress score
            // Here we use a simple “who is higher” race for 60 seconds or first to top
            if (player1.y < 100 && player1.vy < -1) lapP1++;
            if (player2.y < 100 && player2.vy < -1) lapP2++;

            if (lapP1 >= 5) {
                raceFinished = true;
                winnerText = "PLAYER 1 WINS!";
                gameState = GameMode.GAME_OVER;
            } else if (lapP2 >= 5) {
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
        if (k == KeyEvent.VK_ESCAPE) {
            gameState = GameMode.MENU;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k < keys.length) keys[k] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // ==================== MAIN ====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hoverboard Racing Adventure");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new HoverboardRacingAdventure());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}