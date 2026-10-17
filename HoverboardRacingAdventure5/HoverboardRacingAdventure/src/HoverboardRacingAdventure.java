import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;


public class HoverboardRacingAdventure extends JPanel implements ActionListener, KeyListener {

    static final int W = 1100, H = 700, LANE_H = H / 2;
    static final double FINISH = 12000;

    enum State { MENU, COUNTDOWN, RACE, WIN }
    State state = State.MENU;
    boolean vsComputer = false;

    final javax.swing.Timer timer = new javax.swing.Timer(16, this);
    final Set<Integer> keys = new HashSet<>();
    final Random rnd = new Random();
    long tick = 0;
    double countdown = 3;
    String winner = "";

    Racer p1, p2;
    final List<Particle> particles = new ArrayList<>();
    final List<Star> stars = new ArrayList<>();

    // ---------- Model ----------
    static class Obstacle { double x, y; boolean orb; boolean hit; Obstacle(double x, double y, boolean o) { this.x = x; this.y = y; orb = o; } }

    class Racer {
        String name; Color c1, c2; int top; boolean ai;
        double dist = 0, y, vy = 0, speed = 0, boost = 100, stun = 0;
        List<Obstacle> obs = new ArrayList<>();
        Racer(String n, Color a, Color b, int top, boolean ai) {
            name = n; c1 = a; c2 = b; this.top = top; this.ai = ai; y = LANE_H / 2.0;
            for (double x = 900; x < FINISH; x += 220 + rnd.nextInt(260)) {
                obs.add(new Obstacle(x, 60 + rnd.nextInt(LANE_H - 120), rnd.nextInt(4) == 0));
            }
        }
        void update(boolean up, boolean down, boolean boosting) {
            if (ai) { // simple AI: look ahead and dodge, chase orbs, boost on clear road
                up = down = false; boosting = false;
                Obstacle tgt = null;
                for (Obstacle o : obs) if (!o.hit && o.x > dist + 150 && o.x < dist + 600) { tgt = o; break; }
                if (tgt != null) {
                    double d = tgt.y - y;
                    if (tgt.orb) { up = d < -8; down = d > 8; }
                    else if (Math.abs(d) < 70) { if (d > 0 ^ y < 80) up = true; else down = true; if (y > LANE_H - 80) { up = true; down = false; } }
                } else boosting = boost > 40;
                if (rnd.nextInt(100) < 4) boosting = boost > 20;
            }
            double max = 9 + (boosting && boost > 0 ? 7 : 0);
            if (boosting && boost > 0) { boost -= 0.8; spawnTrail(true); }
            else boost = Math.min(100, boost + 0.08);
            if (stun > 0) { stun--; max = 3; }
            speed += (max - speed) * 0.05;
            dist += speed;
            if (up) vy -= 1.1; if (down) vy += 1.1;
            vy *= 0.85; y += vy;
            y = Math.max(40, Math.min(LANE_H - 40, y));
            spawnTrail(false);
            for (Obstacle o : obs) {
                if (o.hit) continue;
                double sx = o.x - dist;
                if (Math.abs(sx) < 40 && Math.abs(o.y - y) < 38) {
                    o.hit = true;
                    if (o.orb) { boost = Math.min(100, boost + 35); burst(200, top + o.y, new Color(80, 255, 200), 30); }
                    else { stun = 45; speed *= 0.4; burst(200, top + o.y, new Color(255, 90, 60), 45); }
                }
            }
        }
        void spawnTrail(boolean big) {
            Color c = big ? new Color(255, 230, 80) : (tick % 2 == 0 ? c1 : c2);
            particles.add(new Particle(170, top + y + 14 + rnd.nextGaussian() * 3, -3 - rnd.nextDouble() * 4 - speed * 0.3, rnd.nextGaussian() * 0.6, c, big ? 9 : 5, 30));
        }
    }

    static class Particle {
        double x, y, vx, vy, size, life, max; Color c;
        Particle(double x, double y, double vx, double vy, Color c, double s, double l) { this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.c = c; size = s; life = max = l; }
    }
    static class Star { double x, y, z; }

    void burst(double x, double y, Color c, int n) {
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, s = 1 + rnd.nextDouble() * 6;
            particles.add(new Particle(x, y, Math.cos(a) * s, Math.sin(a) * s, c, 3 + rnd.nextDouble() * 5, 40));
        }
    }

    // ---------- Setup ----------
    public HoverboardRacingAdventure() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        for (int i = 0; i < 160; i++) { Star s = new Star(); s.x = rnd.nextInt(W); s.y = rnd.nextInt(H); s.z = 0.2 + rnd.nextDouble(); stars.add(s); }
        timer.start();
    }

    void startRace(boolean cpu) {
        vsComputer = cpu;
        p1 = new Racer("PLAYER 1", new Color(0, 220, 255), new Color(0, 120, 255), 0, false);
        p2 = new Racer(cpu ? "COMPUTER" : "PLAYER 2", new Color(255, 60, 200), new Color(255, 140, 0), LANE_H, cpu);
        particles.clear();
        countdown = 3;
        state = State.COUNTDOWN;
    }

    // ---------- Loop ----------
    @Override public void actionPerformed(ActionEvent e) {
        tick++;
        for (Star s : stars) { s.x -= s.z * (state == State.RACE ? 6 : 1.5); if (s.x < 0) { s.x = W; s.y = rnd.nextInt(H); } }
        if (state == State.COUNTDOWN) { countdown -= 0.016; if (countdown <= 0) state = State.RACE; }
        if (state == State.RACE) {
            p1.update(keys.contains(KeyEvent.VK_W), keys.contains(KeyEvent.VK_S), keys.contains(KeyEvent.VK_D));
            p2.update(keys.contains(KeyEvent.VK_UP), keys.contains(KeyEvent.VK_DOWN), keys.contains(KeyEvent.VK_RIGHT));
            if (p1.dist >= FINISH || p2.dist >= FINISH) {
                winner = (p1.dist >= p2.dist ? p1.name : p2.name) + " WINS!";
                state = State.WIN;
            }
        }
        if (state == State.WIN && tick % 4 == 0)
            burst(rnd.nextInt(W), rnd.nextInt(H / 2), Color.getHSBColor(rnd.nextFloat(), 0.8f, 1f), 25);
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) { Particle p = it.next(); p.x += p.vx; p.y += p.vy; p.vy += 0.05; if (--p.life <= 0) it.remove(); }
        repaint();
    }

    // ---------- Rendering ----------
    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float hue = (tick % 1200) / 1200f;
        g.setPaint(new GradientPaint(0, 0, Color.getHSBColor(0.7f + hue * 0.1f, 0.8f, 0.25f), 0, H, Color.getHSBColor(0.9f, 0.7f, 0.15f)));
        g.fillRect(0, 0, W, H);
        for (Star s : stars) { int a = (int) (120 + 135 * s.z / 1.2); g.setColor(new Color(255, 255, 255, Math.min(255, a))); g.fill(new Ellipse2D.Double(s.x, s.y, s.z * 3, s.z * 3)); }

        if (state == State.MENU) { drawMenu(g); return; }
        drawLane(g, p1); drawLane(g, p2);
        for (Particle p : particles) {
            int a = (int) (255 * p.life / p.max);
            g.setColor(new Color(p.c.getRed(), p.c.getGreen(), p.c.getBlue(), Math.max(0, Math.min(255, a))));
            g.fill(new Ellipse2D.Double(p.x - p.size / 2, p.y - p.size / 2, p.size, p.size));
        }
        drawRacer(g, p1); drawRacer(g, p2);
        g.setColor(new Color(255, 255, 255, 180)); g.setStroke(new BasicStroke(4));
        g.drawLine(0, LANE_H, W, LANE_H);
        drawHud(g, p1); drawHud(g, p2);

        if (state == State.COUNTDOWN) {
            String t = countdown > 0.3 ? String.valueOf((int) Math.ceil(countdown)) : "GO!";
            glowText(g, t, W / 2, H / 2 + 40, 140, Color.getHSBColor((float) countdown / 3, 0.9f, 1f));
        }
        if (state == State.WIN) {
            g.setColor(new Color(0, 0, 0, 140)); g.fillRect(0, H / 2 - 120, W, 240);
            glowText(g, winner, W / 2, H / 2 + 20, 80, Color.getHSBColor(hue * 6 % 1, 0.8f, 1f));
            glowText(g, "ENTER = race again    ESC = menu", W / 2, H / 2 + 80, 26, Color.WHITE);
        }
    }

    void drawLane(Graphics2D g, Racer r) {
        int top = r.top;
        g.setColor(new Color(10, 0, 30, 110)); g.fillRect(0, top + 20, W, LANE_H - 40);
        // neon road lines scrolling
        g.setStroke(new BasicStroke(3));
        for (int i = 0; i < 2; i++) {
            g.setColor(i == 0 ? r.c1 : r.c2);
            g.drawLine(0, top + 20 + i * (LANE_H - 40), W, top + 20 + i * (LANE_H - 40));
        }
        double off = r.dist % 120;
        g.setColor(new Color(255, 255, 255, 60));
        for (double x = -off; x < W; x += 120) g.fillRect((int) x, top + LANE_H / 2 - 2, 60, 4);
        // finish line
        double fx = FINISH - r.dist + 200;
        if (fx < W + 50) for (int i = 0; i < (LANE_H - 40) / 20; i++) for (int j = 0; j < 3; j++) {
            g.setColor((i + j) % 2 == 0 ? Color.WHITE : Color.BLACK);
            g.fillRect((int) fx + j * 20, top + 20 + i * 20, 20, 20);
        }
        // obstacles & orbs
        for (Obstacle o : r.obs) {
            if (o.hit) continue;
            double sx = o.x - r.dist + 200;
            if (sx < -60 || sx > W + 60) continue;
            double cy = top + o.y;
            if (o.orb) {
                double pulse = 18 + Math.sin(tick * 0.2 + o.x) * 4;
                g.setPaint(new RadialGradientPaint(new Point2D.Double(sx, cy), (float) pulse * 2,
                        new float[]{0f, 0.4f, 1f}, new Color[]{Color.WHITE, new Color(80, 255, 200), new Color(80, 255, 200, 0)}));
                g.fill(new Ellipse2D.Double(sx - pulse * 2, cy - pulse * 2, pulse * 4, pulse * 4));
            } else {
                AffineTransform old = g.getTransform();
                g.translate(sx, cy); g.rotate(tick * 0.05 + o.x);
                Path2D spike = new Path2D.Double();
                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI / 8, rr = i % 2 == 0 ? 30 : 15;
                    if (i == 0) spike.moveTo(Math.cos(a) * rr, Math.sin(a) * rr); else spike.lineTo(Math.cos(a) * rr, Math.sin(a) * rr);
                }
                spike.closePath();
                g.setPaint(new GradientPaint(-30, -30, new Color(255, 60, 60), 30, 30, new Color(255, 180, 0)));
                g.fill(spike);
                g.setColor(Color.DARK_GRAY); g.fillOval(-8, -8, 16, 16);
                g.setTransform(old);
            }
        }
    }

    void drawRacer(Graphics2D g, Racer r) {
        double x = 200, y = r.top + r.y + Math.sin(tick * 0.15) * 4;
        AffineTransform old = g.getTransform();
        g.translate(x, y); g.rotate(r.vy * 0.04);
        if (r.stun > 0 && tick % 6 < 3) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        // glow under board
        g.setPaint(new RadialGradientPaint(new Point2D.Double(0, 22), 60, new float[]{0f, 1f}, new Color[]{new Color(r.c1.getRed(), r.c1.getGreen(), r.c1.getBlue(), 160), new Color(0, 0, 0, 0)}));
        g.fill(new Ellipse2D.Double(-60, 0, 120, 45));
        // board
        g.setPaint(new GradientPaint(-40, 0, r.c1, 40, 0, r.c2));
        g.fill(new RoundRectangle2D.Double(-42, 10, 84, 12, 12, 12));
        g.setColor(Color.WHITE); g.setStroke(new BasicStroke(2)); g.draw(new RoundRectangle2D.Double(-42, 10, 84, 12, 12, 12));
        // rider
        g.setColor(new Color(40, 40, 60));
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(-8, 10, 0, -10); g.drawLine(10, 10, 2, -10); // legs
        g.drawLine(0, -10, 6, -32);                             // body
        g.drawLine(6, -28, -14, -20); g.drawLine(6, -28, 24, -22); // arms
        g.setColor(r.c2); g.fillOval(-2, -50, 18, 18);           // helmet
        g.setColor(Color.WHITE); g.fillRect(8, -44, 9, 5);       // visor
        g.setComposite(AlphaComposite.SrcOver);
        g.setTransform(old);
    }

    void drawHud(Graphics2D g, Racer r) {
        int y = r.top + 8;
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(r.c1); g.drawString(r.name, 20, y + 30);
        // progress bar
        g.setColor(new Color(255, 255, 255, 60)); g.fillRoundRect(150, y + 16, 400, 14, 14, 14);
        g.setPaint(new GradientPaint(150, 0, r.c1, 550, 0, r.c2));
        g.fillRoundRect(150, y + 16, (int) (400 * Math.min(1, r.dist / FINISH)), 14, 14, 14);
        // boost bar
        g.setColor(Color.WHITE); g.drawString("BOOST", 590, y + 30);
        g.setColor(new Color(255, 255, 255, 60)); g.fillRoundRect(660, y + 16, 200, 14, 14, 14);
        g.setPaint(new GradientPaint(660, 0, new Color(255, 230, 80), 860, 0, new Color(255, 80, 0)));
        g.fillRoundRect(660, y + 16, (int) (2 * r.boost), 14, 14, 14);
        g.setColor(Color.WHITE); g.drawString((int) (r.speed * 12) + " km/h", 900, y + 30);
    }

    void drawMenu(Graphics2D g) {
        float hue = (tick % 360) / 360f;
        glowText(g, "HOVERBOARD", W / 2, 190, 96, Color.getHSBColor(hue, 0.8f, 1f));
        glowText(g, "RACING ADVENTURE", W / 2, 270, 56, Color.getHSBColor((hue + 0.3f) % 1, 0.8f, 1f));
        glowText(g, "Press 1  -  Player vs Player", W / 2, 390, 32, new Color(0, 220, 255));
        glowText(g, "Press 2  -  Player vs Computer", W / 2, 440, 32, new Color(255, 60, 200));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18)); g.setColor(new Color(255, 255, 255, 200));
        String[] lines = {"P1: W / S to steer, D to boost", "P2: UP / DOWN to steer, RIGHT to boost", "Grab green orbs for boost - avoid the spiky mines!"};
        for (int i = 0; i < lines.length; i++) { FontMetrics fm = g.getFontMetrics(); g.drawString(lines[i], W / 2 - fm.stringWidth(lines[i]) / 2, 540 + i * 30); }
        if (tick % 3 == 0) burst(rnd.nextInt(W), H + 5, Color.getHSBColor(rnd.nextFloat(), 0.8f, 1f), 2);
        for (Particle p : particles) { int a = (int) (255 * p.life / p.max); g.setColor(new Color(p.c.getRed(), p.c.getGreen(), p.c.getBlue(), Math.max(0, a))); g.fill(new Ellipse2D.Double(p.x, p.y, p.size, p.size)); }
    }

    void glowText(Graphics2D g, String s, int cx, int y, int size, Color c) {
        g.setFont(new Font("SansSerif", Font.BOLD, size));
        FontMetrics fm = g.getFontMetrics();
        int x = cx - fm.stringWidth(s) / 2;
        for (int i = 6; i > 0; i -= 2) { g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40)); g.drawString(s, x - i, y); g.drawString(s, x + i, y); g.drawString(s, x, y - i); g.drawString(s, x, y + i); }
        g.setColor(c); g.drawString(s, x, y);
    }

    // ---------- Input ----------
    @Override public void keyPressed(KeyEvent e) {
        keys.add(e.getKeyCode());
        if (state == State.MENU) {
            if (e.getKeyCode() == KeyEvent.VK_1) startRace(false);
            if (e.getKeyCode() == KeyEvent.VK_2) startRace(true);
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { state = State.MENU; particles.clear(); }
        else if (state == State.WIN && e.getKeyCode() == KeyEvent.VK_ENTER) startRace(vsComputer);
    }
    @Override public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Hoverboard Racing Adventure");
            HoverboardRacingAdventure game = new HoverboardRacingAdventure();
            f.add(game); f.pack(); f.setResizable(false);
            f.setLocationRelativeTo(null);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
