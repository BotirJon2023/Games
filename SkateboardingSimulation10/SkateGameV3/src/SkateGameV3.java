import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class SkateGameV3 extends JPanel implements ActionListener, KeyListener, MouseListener {

    private static final int    WIDTH          = 1280;
    private static final int    HEIGHT         = 720;
    private static final int    GROUND_BASE    = HEIGHT - 100;

    private static final double GRAVITY        = 0.54;
    private static final double JUMP_VEL       = -15.2;
    private static final double MAX_RUN_SPEED  = 9.4;
    private static final double GROUND_FRICT   = 0.976;
    private static final double AIR_FRICT      = 0.993;
    private static final double CAMERA_LAG     = 0.092;

    private static final int    FPS            = 60;

    enum GameState { MENU, PLAYING, PAUSED, GAME_OVER }
    private GameState state = GameState.MENU;

    private double px = 220, py;
    private double vx = 0, vy = 0;
    private double cameraX = 0;

    private boolean left, right, up, down, debug;
    private boolean onGround = true;
    private boolean grinding = false;
    private boolean crashed = false;

    private double distance = 0;
    private long   score = 0;
    private int    combo = 1;
    private double comboTimer = 0;

    private final List<Particle> particles = new ArrayList<>();
    private final List<Terrain> terrainSegments = new ArrayList<>();
    private final Random random = new Random(1337420);

    private Timer timer;
    private long lastNano;

    private String currentTrick = "";
    private int trickDisplayFrames = 0;
    private int landingQuality = 0; // -2 bail, -1 sketchy, 0 ok, 1 good, 2 perfect


    private final Rectangle btnPlay   = new Rectangle(WIDTH/2-140, 220, 280, 70);
    private final Rectangle btnQuit   = new Rectangle(WIDTH/2-140, 320, 280, 70);
    private final Rectangle btnResume = new Rectangle(WIDTH/2-140, 220, 280, 70);
    private final Rectangle btnRestart= new Rectangle(WIDTH/2-140, 310, 280, 70);

    static class Particle {
        double x, y, vx, vy;
        int life;
        Color color;
        Particle(double x, double y, double vx, double vy, int life, Color c) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.life = life; this.color = c;
        }
        void update() { x += vx; y += vy; vy += 0.24; life--; }
        void draw(Graphics2D g) {
            int alpha = (int)(255 * (life / 45.0));
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, alpha)));
            g.fillOval((int)(x-4), (int)(y-4), 8, 8);
        }
    }

    static class Terrain {
        double x1, y1, x2, y2;
        boolean isGap;
        boolean isRail;
        Terrain(double x1, double y1, double x2, double y2, boolean gap, boolean rail) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
            this.isGap = gap; this.isRail = rail;
        }
        double getHeightAt(double wx) {
            if (Math.abs(x2 - x1) < 0.001) return y1;
            double t = (wx - x1) / (x2 - x1);
            if (t < 0 || t > 1) return Double.NaN;
            return y1 + t * (y2 - y1);
        }
    }

    public SkateGameV3() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(95, 165, 230));
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);

        resetGame();
        generateLevel();

        timer = new Timer(1000 / FPS, this);
        lastNano = System.nanoTime();
        timer.start();
    }

    private void resetGame() {
        px = 220; py = GROUND_BASE - 74;
        vx = vy = 0; cameraX = 0;
        onGround = true; grinding = false; crashed = false;
        distance = 0; score = 0; combo = 1; comboTimer = 0;
        currentTrick = ""; trickDisplayFrames = 0; landingQuality = 0;
        particles.clear();
        terrainSegments.clear();
        generateLevel();
        state = GameState.MENU;
    }

    private void generateLevel() {
        double cx = -800;
        double cy = GROUND_BASE;

        terrainSegments.add(new Terrain(cx - 2000, cy + 220, cx, cy, false, false));

        while (cx < 30000) {
            double len = 100 + random.nextDouble() * 320;
            double dy  = -60 + random.nextDouble() * 120;

            if (random.nextDouble() < 0.22) len += 180 + random.nextDouble() * 300;

            double nx = cx + len;
            double ny = cy + dy;

            boolean gap  = random.nextDouble() < 0.13 && len > 200;
            boolean rail = !gap && random.nextDouble() < 0.09 && len > 140;

            terrainSegments.add(new Terrain(cx, cy, nx, ny, gap, rail));

            cx = nx;
            cy = ny;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = (now - lastNano) / 1_000_000_000.0;
        lastNano = now;
        if (dt > 0.15) dt = 0.15;

        if (state == GameState.PLAYING) {
            updatePhysics(dt);
            updateCamera();
            updateTricksAndCombo(dt);
            updateParticles();
            checkGrinding();
            checkLanding();
        }

        repaint();
    }

    private void updatePhysics(double dt) {
        // Input
        double accel = 0;
        if (left)  accel -= 1.3;
        if (right) accel += 1.3;

        if (onGround && !grinding) {
            vx += accel * 52 * dt;
            vx *= GROUND_FRICT;
            if (Math.abs(vx) > MAX_RUN_SPEED) vx = Math.signum(vx) * MAX_RUN_SPEED;
        } else {
            vx += accel * 14 * dt;
            vx *= AIR_FRICT;
        }

        // Jump
        if (up && onGround) {
            vy = JUMP_VEL - (grinding ? 3.5 : 0); // weaker jump off grind
            onGround = false;
            grinding = false;
        }

        vy += GRAVITY * 68 * dt;

        px += vx * 68 * dt;
        py += vy * 68 * dt;

        distance = Math.max(distance, px - 220);
    }

    private void updateCamera() {
        double target = px - WIDTH * 0.36;
        cameraX += (target - cameraX) * CAMERA_LAG;
    }

    private Terrain findClosestSegment() {
        Terrain best = null;
        double minDist = 1e9;

        for (Terrain t : terrainSegments) {
            if (px < t.x1 - 120 || px > t.x2 + 120) continue;
            double ty = t.getHeightAt(px);
            if (Double.isNaN(ty)) continue;
            double dy = Math.abs(ty - py);
            if (dy < minDist) {
                minDist = dy;
                best = t;
            }
        }
        return best;
    }

    private void checkGrinding() {
        grinding = false;
        Terrain seg = findClosestSegment();
        if (seg == null || !seg.isRail || !down) return;

        double ty = seg.getHeightAt(px);
        if (Math.abs(ty - py) < 38 && Math.abs(vy) < 5.5) {
            grinding = true;
            py = ty - 28;
            vy = 0;
            onGround = true;
        }
    }

    private void checkLanding() {
        if (!onGround) return;

        double absAngle = Math.abs(boardAngle());
        if (absAngle > 0.9) {
            if (absAngle > 1.8) {
                landingQuality = -2; // bail
                crash();
            } else {
                landingQuality = -1; // sketchy
            }
        } else if (absAngle < 0.25) {
            landingQuality = 2; // perfect
        } else {
            landingQuality = 1; // good
        }
    }

    private double boardAngle() {
        return Math.sin(px * 0.018 + vx * 0.4) * 0.35 + (grinding ? 0.18 : 0);
    }

    private void crash() {
        crashed = true;
        state = GameState.GAME_OVER;
        vx *= 0.38;
        vy = -7.5 + Math.random() * -6;

        for (int i = 0; i < 80; i++) {
            double a = Math.random() * Math.PI * 2;
            double s = 2.5 + Math.random() * 11;
            Color c = Math.random() < 0.6 ? new Color(200,40,30) : new Color(240,200,70);
            particles.add(new Particle(
                    px + (Math.random()-0.5)*36,
                    py + (Math.random()-0.5)*60,
                    Math.cos(a)*s + vx*0.6,
                    Math.sin(a)*s + vy*0.4 - 6,
                    30 + (int)(Math.random()*50), c));
        }
    }

    private void updateTricksAndCombo(double dt) {
        comboTimer -= dt;
        if (comboTimer <= 0 && combo > 1) combo--;

        if (trickDisplayFrames > 0) trickDisplayFrames--;

        if (grinding && trickDisplayFrames == 0) {
            currentTrick = "Grind";
            trickDisplayFrames = 50;
            score += 120 * combo;
            comboTimer = 4.2;
            combo = Math.min(combo + 1, 6);
        }

        if (onGround && vy == 0 && trickDisplayFrames == 0) {
            if (Math.abs(boardAngle()) > 1.1 && Math.abs(vx) > 3.8) {
                currentTrick = "180";
                trickDisplayFrames = 60;
                score += 280 * combo;
                comboTimer = 5;
                combo++;
            }
        }

        if (!onGround && vy < -2.5 && trickDisplayFrames == 0) {
            if (random.nextDouble() < 0.38) {
                currentTrick = "Ollie";
                trickDisplayFrames = 45;
                score += 100 * combo;
                comboTimer = 3.8;
                combo = Math.max(combo, 2);
            }
        }
    }

    private void updateParticles() {
        particles.removeIf(p -> p.life <= 0);
        particles.forEach(Particle::update);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2);
        g2.translate(-cameraX, 0);

        drawTerrain(g2);
        drawPlayer(g2);
        drawParticles(g2);

        g2.translate(cameraX, 0);
        drawHUD(g2);

        if (state != GameState.PLAYING) {
            drawOverlay(g2);
        }
    }

    private void drawBackground(Graphics2D g) {
        // Sky gradient
        GradientPaint sky = new GradientPaint(0, 0, new Color(70, 140, 220),
                0, HEIGHT, new Color(170, 220, 255));
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Clouds (parallax)
        g.setColor(new Color(255,255,255,140));
        drawCloud(g,  300 + cameraX*0.08,  90, 160);
        drawCloud(g,  980 + cameraX*0.11, 140, 220);
        drawCloud(g, 1950 + cameraX*0.07,  60, 130);

        // Distant hills
        g.setColor(new Color(110, 150, 100, 180));
        fillParallaxHill(g, cameraX*0.15 + 400,  GROUND_BASE - 80, 480, 260);
        fillParallaxHill(g, cameraX*0.22 + 1400, GROUND_BASE - 120, 620, 340);
    }

    private void drawCloud(Graphics2D g, double x, double y, double r) {
        g.fillOval((int)(x-r*0.8), (int)(y-r*0.5), (int)(r*1.6), (int)(r*1.0));
        g.fillOval((int)(x-r*0.2), (int)(y-r*0.9), (int)(r*1.3), (int)(r*0.9));
        g.fillOval((int)(x+r*0.4), (int)(y-r*0.6), (int)(r*1.1), (int)(r*0.8));
    }

    private void fillParallaxHill(Graphics2D g, double x, double baseY, double w, double h) {
        Path2D p = new Path2D.Double();
        p.moveTo(x-w, HEIGHT);
        p.quadTo(x-w/2, baseY - h*0.3, x, baseY - h);
        p.quadTo(x+w/2, baseY - h*0.4, x+w, baseY - h*0.6);
        p.lineTo(x+w, HEIGHT);
        p.closePath();
        g.fill(p);
    }

    private void drawTerrain(Graphics2D g) {
        Path2D path = new Path2D.Double();
        boolean started = false;

        for (Terrain t : terrainSegments) {
            if (t.x2 < cameraX - 400 || t.x1 > cameraX + WIDTH + 400) continue;

            if (!started) {
                path.moveTo(t.x1, t.y1 + 600);
                started = true;
            }

            if (t.isGap) {
                path.lineTo(t.x1, t.y1 + 600);
                path.lineTo(t.x2, t.y2 + 600);
            } else {
                path.lineTo(t.x1, t.y1);
                path.lineTo(t.x2, t.y2);
            }
        }

        if (started) {
            Rectangle2D bounds = path.getBounds2D();
            path.lineTo(bounds.getMaxX(), HEIGHT + 300);
            path.lineTo(bounds.getMinX() - 500, HEIGHT + 300);
            path.closePath();

            g.setColor(new Color(40, 100, 35));
            g.fill(path);

            g.setColor(new Color(65, 145, 55));
            g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(path);
            g.setStroke(new BasicStroke(1));
        }

        // Draw rails
        g.setColor(new Color(90, 90, 110));
        g.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Terrain t : terrainSegments) {
            if (!t.isRail || t.x2 < cameraX - 200 || t.x1 > cameraX + WIDTH + 200) continue;
            g.drawLine((int)t.x1, (int)t.y1 - 6, (int)t.x2, (int)t.y2 - 6);
        }
        g.setStroke(new BasicStroke(1));
    }

    private void drawPlayer(Graphics2D g) {
        double bx = px;
        double by = py + (down ? 12 : 0);

        // Board shadow
        g.setColor(new Color(0,0,0,90));
        g.fillOval((int)(bx-50), (int)(by+14), 100, 24);

        AffineTransform old = g.getTransform();
        g.translate(bx, by + 12);
        g.rotate(boardAngle());

        // Deck
        g.setColor(new Color(20, 20, 90));
        g.fillRoundRect(-48, -10, 96, 20, 16, 16);
        // Grip tape
        g.setColor(new Color(50,50,50,220));
        g.fillRoundRect(-46, -8, 92, 12, 12, 12);
        // Wheels
        g.setColor(Color.BLACK);
        g.fillOval(-34, -16, 22, 22);
        g.fillOval( 12, -16, 22, 22);
        g.setColor(new Color(220,220,240));
        g.fillOval(-29, -11, 12, 12);
        g.fillOval( 17, -11, 12, 12);

        g.setTransform(old);

        // Skater
        g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        double legAngle = onGround ? Math.sin(px*0.06)*0.55 : -1.0 + vy*0.09;
        if (down) legAngle *= 0.6;

        double armAngle = Math.sin(px*0.09 + 0.8)*1.1;

        // Torso + t-shirt
        int torsoY = (int)(py - 48 - (down?24:0));
        g.setColor(new Color(240, 80, 40)); // shirt
        g.fillRoundRect((int)px-18, torsoY-32, 36, 44, 18, 18);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("SK8", (int)px-12, torsoY-4);

        // Head + helmet
        g.setColor(new Color(255, 230, 180));
        g.fillOval((int)px-18, (int)py-90, 36, 36);
        g.setColor(new Color(0, 120, 220));
        g.fillArc((int)px-18, (int)py-90, 36, 36, 0, 180); // helmet
        g.setColor(Color.BLACK);
        g.fillOval((int)px-6, (int)py-76, 7, 11); // eye

        // Arms
        g.setColor(new Color(240, 80, 40));
        int armLen = 34;
        g.drawLine((int)px, torsoY-8,
                (int)(px + Math.cos(armAngle+1.7)*armLen),
                (int)(torsoY-8 + Math.sin(armAngle+1.7)*armLen));
        g.drawLine((int)px, torsoY-8,
                (int)(px + Math.cos(-armAngle-0.6)*armLen),
                (int)(torsoY-8 + Math.sin(-armAngle-0.6)*armLen));

        // Legs + shoes
        g.setColor(new Color(30, 30, 130));
        int legLen = down ? 24 : 42;
        g.drawLine((int)px, torsoY+10,
                (int)(px + Math.cos(legAngle+1.0)*legLen),
                (int)(torsoY+10 + Math.sin(legAngle+1.0)*legLen));
        g.drawLine((int)px, torsoY+10,
                (int)(px + Math.cos(-legAngle-0.7)*legLen),
                (int)(torsoY+10 + Math.sin(-legAngle-0.7)*legLen));

        // Shoes
        g.setColor(new Color(220, 30, 30));
        g.fillOval((int)(px + Math.cos(legAngle+1.0)*legLen - 10),
                (int)(torsoY+10 + Math.sin(legAngle+1.0)*legLen - 8), 20, 14);
        g.fillOval((int)(px + Math.cos(-legAngle-0.7)*legLen - 10),
                (int)(torsoY+10 + Math.sin(-legAngle-0.7)*legLen - 8), 20, 14);

        g.setStroke(new BasicStroke(1));
    }

    private void drawParticles(Graphics2D g) {
        for (Particle p : particles) p.draw(g);
    }

    private void drawHUD(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString(String.format("%,d", score), 40, 60);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString(String.format("Dist: %.0f m", distance), 40, 100);
        g.drawString("×" + combo, 40, 135);

        // Trick popup
        if (trickDisplayFrames > 0) {
            float alpha = Math.min(1f, trickDisplayFrames / 35f);
            g.setColor(new Color(255, 240, 80, (int)(alpha * 255)));
            g.setFont(new Font("Arial", Font.BOLD, 68));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(currentTrick);
            g.drawString(currentTrick, WIDTH/2 - w/2, 210);
        }

        // Speed
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(new Color(255,255,255,200));
        g.drawString(String.format("%.1f km/h", Math.abs(vx)*3.6), WIDTH-220, 60);
    }

    private void drawOverlay(Graphics2D g) {
        g.setColor(new Color(0,0,0,160));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 92));

        FontMetrics fm = g.getFontMetrics();

        if (state == GameState.MENU) {
            String title = "SKATE SIM";
            g.drawString(title, WIDTH/2 - fm.stringWidth(title)/2, 140);

            g.setFont(new Font("Arial", Font.BOLD, 48));
            fm = g.getFontMetrics();

            g.setColor(btnPlay.contains(getMousePosition()) ? new Color(80,220,80) : Color.WHITE);
            g.drawString("PLAY", btnPlay.x + 60, btnPlay.y + 50);

            g.setColor(btnQuit.contains(getMousePosition()) ? new Color(220,80,80) : Color.WHITE);
            g.drawString("QUIT", btnQuit.x + 70, btnQuit.y + 50);
        }
        else if (state == GameState.PAUSED) {
            String txt = "PAUSED";
            g.drawString(txt, WIDTH/2 - fm.stringWidth(txt)/2, HEIGHT/2 - 80);

            g.setFont(new Font("Arial", Font.BOLD, 48));
            fm = g.getFontMetrics();

            g.setColor(btnResume.contains(getMousePosition()) ? new Color(100,220,100) : Color.WHITE);
            g.drawString("RESUME", btnResume.x + 30, btnResume.y + 50);

            g.setColor(btnRestart.contains(getMousePosition()) ? new Color(220,180,60) : Color.WHITE);
            g.drawString("RESTART", btnRestart.x + 30, btnRestart.y + 50);
        }
        else if (state == GameState.GAME_OVER) {
            String txt = "CRASHED!";
            g.setColor(new Color(255,60,60));
            g.drawString(txt, WIDTH/2 - fm.stringWidth(txt)/2, HEIGHT/2 - 120);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 52));
            fm = g.getFontMetrics();
            txt = String.format("Score: %,d", score);
            g.drawString(txt, WIDTH/2 - fm.stringWidth(txt)/2, HEIGHT/2 - 20);

            g.setFont(new Font("Arial", Font.PLAIN, 38));
            g.drawString("Press R or click RESTART", WIDTH/2 - 220, HEIGHT/2 + 100);
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT  -> left  = true;
            case KeyEvent.VK_RIGHT -> right = true;
            case KeyEvent.VK_UP, KeyEvent.VK_SPACE -> up = true;
            case KeyEvent.VK_DOWN  -> down  = true;
            case KeyEvent.VK_P     -> { if (state == GameState.PLAYING) state = GameState.PAUSED; }
            case KeyEvent.VK_ESCAPE-> { if (state == GameState.PAUSED)  state = GameState.PLAYING; }
            case KeyEvent.VK_R     -> { if (state == GameState.GAME_OVER) resetGame(); }
        }
    }

    @Override public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT  -> left  = false;
            case KeyEvent.VK_RIGHT -> right = false;
            case KeyEvent.VK_UP, KeyEvent.VK_SPACE -> up = false;
            case KeyEvent.VK_DOWN  -> down  = false;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}

    @Override public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        if (state == GameState.MENU) {
            if (btnPlay.contains(p))   { state = GameState.PLAYING; }
            if (btnQuit.contains(p))   { System.exit(0); }
        }
        else if (state == GameState.PAUSED) {
            if (btnResume.contains(p))  { state = GameState.PLAYING; }
            if (btnRestart.contains(p)) { resetGame(); }
        }
        else if (state == GameState.GAME_OVER) {
            if (btnRestart.contains(p)) { resetGame(); }
        }
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Skateboarding Simulation – v3");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new SkateGameV3());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}