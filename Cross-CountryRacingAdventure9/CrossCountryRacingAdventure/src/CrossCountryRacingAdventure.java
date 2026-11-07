import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.*;

public class CrossCountryRacingAdventure extends JPanel implements ActionListener {
    private static final int W = 1000, H = 700, HORIZON = 145;
    private static final double FINISH = 100.0;

    private final javax.swing.Timer timer = new javax.swing.Timer(16, this);
    private final Set<Integer> keys = new HashSet<>();
    private final ArrayList<Obstacle> obstacles = new ArrayList<>();
    private final Random random = new Random();

    private int state = 0; // 0 menu, 1 racing, 2 results
    private boolean versusComputer = true;
    private String result = "";
    private double elapsed = 0, spawnTimer = 0;
    private double playerX = .32, rivalX = .68;
    private double playerSpeed = .45, rivalSpeed = .82;
    private double playerProgress = 0, rivalProgress = 0;

    public CrossCountryRacingAdventure() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        bind(KeyEvent.VK_1, false, () -> startGame(true));
        bind(KeyEvent.VK_2, false, () -> startGame(false));
        bind(KeyEvent.VK_ENTER, false, () -> {
            if (state != 1) startGame(versusComputer);
        });
        bind(KeyEvent.VK_W, false, () -> keys.add(KeyEvent.VK_W));
        bind(KeyEvent.VK_W, true,  () -> keys.remove(KeyEvent.VK_W));
        bind(KeyEvent.VK_A, false, () -> keys.add(KeyEvent.VK_A));
        bind(KeyEvent.VK_A, true,  () -> keys.remove(KeyEvent.VK_A));
        bind(KeyEvent.VK_S, false, () -> keys.add(KeyEvent.VK_S));
        bind(KeyEvent.VK_S, true,  () -> keys.remove(KeyEvent.VK_S));
        bind(KeyEvent.VK_D, false, () -> keys.add(KeyEvent.VK_D));
        bind(KeyEvent.VK_D, true,  () -> keys.remove(KeyEvent.VK_D));
        bind(KeyEvent.VK_UP, false, () -> keys.add(KeyEvent.VK_UP));
        bind(KeyEvent.VK_UP, true,  () -> keys.remove(KeyEvent.VK_UP));
        bind(KeyEvent.VK_DOWN, false, () -> keys.add(KeyEvent.VK_DOWN));
        bind(KeyEvent.VK_DOWN, true,  () -> keys.remove(KeyEvent.VK_DOWN));
        bind(KeyEvent.VK_LEFT, false, () -> keys.add(KeyEvent.VK_LEFT));
        bind(KeyEvent.VK_LEFT, true,  () -> keys.remove(KeyEvent.VK_LEFT));
        bind(KeyEvent.VK_RIGHT, false, () -> keys.add(KeyEvent.VK_RIGHT));
        bind(KeyEvent.VK_RIGHT, true,  () -> keys.remove(KeyEvent.VK_RIGHT));
        timer.start();
    }

    private void bind(int key, boolean release, Runnable action) {
        String name = "key_" + key + "_" + release;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0, release), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void startGame(boolean cpu) {
        versusComputer = cpu;
        state = 1;
        elapsed = 0;
        spawnTimer = .4;
        playerX = .32;
        rivalX = .68;
        playerSpeed = .45;
        rivalSpeed = cpu ? .82 : .45;
        playerProgress = rivalProgress = 0;
        obstacles.clear();
        keys.clear();
    }

    @Override public void actionPerformed(ActionEvent e) {
        double dt = .016;
        elapsed += dt;
        if (state == 1) updateRace(dt);
        repaint();
    }

    private void updateRace(double dt) {
        double steer = (keys.contains(KeyEvent.VK_D) ? 1 : 0)
                - (keys.contains(KeyEvent.VK_A) ? 1 : 0);
        playerX = clamp(playerX + steer * .78 * dt, .06, .94);
        if (keys.contains(KeyEvent.VK_W)) playerSpeed = Math.min(1.25, playerSpeed + .55 * dt);
        else playerSpeed = Math.max(.42, playerSpeed - .10 * dt);
        if (keys.contains(KeyEvent.VK_S)) playerSpeed = Math.max(.05, playerSpeed - .85 * dt);

        if (versusComputer) {
            double desired = .62 + Math.sin(elapsed * .65) * .12;
            for (Obstacle o : obstacles) {
                if (o.z < .38 && o.z > .04 && Math.abs(o.x - rivalX) < .19)
                    desired = o.x > .5 ? .28 : .72;
            }
            rivalX += (desired - rivalX) * Math.min(1, dt * 1.7);
            rivalSpeed = .83 + Math.sin(elapsed * .9) * .06;
        } else {
            double s2 = (keys.contains(KeyEvent.VK_RIGHT) ? 1 : 0)
                    - (keys.contains(KeyEvent.VK_LEFT) ? 1 : 0);
            rivalX = clamp(rivalX + s2 * .78 * dt, .06, .94);
            if (keys.contains(KeyEvent.VK_UP)) rivalSpeed = Math.min(1.25, rivalSpeed + .55 * dt);
            else rivalSpeed = Math.max(.42, rivalSpeed - .10 * dt);
            if (keys.contains(KeyEvent.VK_DOWN)) rivalSpeed = Math.max(.05, rivalSpeed - .85 * dt);
        }

        spawnTimer -= dt;
        if (spawnTimer <= 0) {
            obstacles.add(new Obstacle(.18 + random.nextDouble() * .64));
            spawnTimer = .85 + random.nextDouble() * .75;
        }
        Iterator<Obstacle> it = obstacles.iterator();
        while (it.hasNext()) {
            Obstacle o = it.next();
            o.z -= dt * (.20 + (playerSpeed + rivalSpeed) * .065);
            if (o.z < .055 && o.z > 0) {
                if (!o.playerHit && Math.abs(o.x - playerX) < .095) {
                    playerSpeed = Math.max(.25, playerSpeed * .60);
                    o.playerHit = true;
                }
                if (!o.rivalHit && Math.abs(o.x - rivalX) < .095) {
                    rivalSpeed = Math.max(.25, rivalSpeed * .60);
                    o.rivalHit = true;
                }
            }
            if (o.z <= 0) it.remove();
        }

        playerProgress += playerSpeed * dt * 1.8;
        rivalProgress += rivalSpeed * dt * 1.8;
        if (playerProgress >= FINISH || rivalProgress >= FINISH) {
            state = 2;
            if (playerProgress >= FINISH && rivalProgress >= FINISH)
                result = playerProgress >= rivalProgress ? "Player 1 wins!" : (versusComputer ? "Computer wins!" : "Player 2 wins!");
            else if (playerProgress >= FINISH) result = "Player 1 wins!";
            else result = versusComputer ? "Computer wins!" : "Player 2 wins!";
        }
    }

    private static double clamp(double n, double lo, double hi) { return Math.max(lo, Math.min(hi, n)); }
    private double roadWidth(double y) { return 190 + (y - HORIZON) * .92; }
    private double roadCenter(double y) { return W / 2.0 + Math.sin((y - HORIZON) * .008 + elapsed * .25) * 62; }
    private double trackX(double position, double y) { return roadCenter(y) + (position - .5) * roadWidth(y) * .82; }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawLandscape(g);
        drawRoad(g);
        drawTrees(g);
        if (state == 1) {
            drawObstacles(g);
            drawCar(g, (int) trackX(playerX, 565), 565, new Color(220, 54, 46));
            drawCar(g, (int) trackX(rivalX, 625), 625, versusComputer ? new Color(55, 155, 220) : new Color(247, 185, 48));
            drawHud(g);
        } else if (state == 0) drawMenu(g);
        else drawResults(g);
        g.dispose();
    }

    private void drawLandscape(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(105, 190, 235), 0, HORIZON + 100, new Color(222, 232, 188)));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(113, 157, 104));
        int hillShift = (int)(Math.sin(elapsed * .12) * 24);
        int[] hx = {-100, 100, 280, 470, 660, 850, 1100};
        int[] hy = {HORIZON + 35, HORIZON - 12, HORIZON + 18, HORIZON - 28, HORIZON + 25, HORIZON - 8, HORIZON + 40};
        for (int i = 0; i < hx.length; i++) hy[i] += hillShift;
        Polygon hills = new Polygon(hx, hy, hx.length);
        hills.addPoint(W + 100, HORIZON + 130);
        hills.addPoint(-100, HORIZON + 130);
        g.fillPolygon(hills);
        g.setColor(new Color(89, 143, 82));
        g.fillRect(0, HORIZON + 92, W, H - HORIZON);
    }

    private void drawRoad(Graphics2D g) {
        for (int y = HORIZON + 92; y < H; y += 28) {
            g.setColor(((y / 28) % 2 == 0) ? new Color(81, 139, 73) : new Color(88, 148, 78));
            g.fillRect(0, y, W, 14);
        }
        Path2D road = new Path2D.Double();
        boolean first = true;
        for (int y = HORIZON; y <= H; y += 6) {
            double x = roadCenter(y) - roadWidth(y) / 2;
            if (first) { road.moveTo(x, y); first = false; } else road.lineTo(x, y);
        }
        for (int y = H; y >= HORIZON; y -= 6) road.lineTo(roadCenter(y) + roadWidth(y) / 2, y);
        road.closePath();
        g.setColor(new Color(190, 174, 135));
        g.setStroke(new BasicStroke(15, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int y = HORIZON; y < H; y += 5) {
            g.drawLine((int)(roadCenter(y) - roadWidth(y) / 2), y,
                    (int)(roadCenter(y + 5) - roadWidth(y + 5) / 2), y + 5);
            g.drawLine((int)(roadCenter(y) + roadWidth(y) / 2), y,
                    (int)(roadCenter(y + 5) + roadWidth(y + 5) / 2), y + 5);
        }
        g.setColor(new Color(67, 70, 71));
        g.fill(road);
        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(244, 221, 135));
        for (int y = HORIZON + (int)(elapsed * 95 % 58); y < H; y += 58) {
            int yy = Math.min(y, H - 1);
            g.drawLine((int)roadCenter(yy), yy, (int)roadCenter(Math.min(yy + 25, H)), Math.min(yy + 25, H));
        }
    }

    private void drawTrees(Graphics2D g) {
        int span = H - HORIZON;
        for (int i = 0; i < 22; i++) {
            int y = HORIZON + (int)((i * 47 + elapsed * 72) % span);
            double scale = .25 + (y - HORIZON) / (double)span * .85;
            int size = (int)(34 * scale);
            int left = (int)(roadCenter(y) - roadWidth(y) / 2 - size * 1.2);
            int right = (int)(roadCenter(y) + roadWidth(y) / 2 + size * 1.2);
            drawPine(g, left, y, size);
            drawPine(g, right, y, size);
        }
    }

    private void drawPine(Graphics2D g, int x, int y, int size) {
        g.setColor(new Color(62, 92, 54));
        g.fillRect(x - size / 12, y - size / 4, size / 6, size / 2);
        g.setColor(new Color(28, 91, 57));
        for (int j = 0; j < 3; j++) {
            int top = y - size + j * size / 4;
            int half = size / 2 - j * size / 10;
            int[] xs = {x, x - half, x + half};
            int[] ys = {top, top + size / 2, top + size / 2};
            g.fillPolygon(xs, ys, 3);
        }
    }

    private void drawObstacles(Graphics2D g) {
        for (Obstacle o : obstacles) {
            int y = HORIZON + (int)((1 - o.z) * (H - HORIZON));
            int x = (int)trackX(o.x, y);
            int r = Math.max(5, (int)(7 + (1 - o.z) * 24));
            g.setColor(new Color(45, 43, 40, 80));
            g.fillOval(x - r, y + r / 2, r * 2, r / 2);
            g.setColor(new Color(112, 106, 94));
            int[] xs = {x - r, x - r / 2, x + r, x + r / 2, x - r};
            int[] ys = {y, y - r, y - r / 2, y + r / 2, y + r / 2};
            g.fillPolygon(xs, ys, xs.length);
            g.setColor(new Color(164, 153, 133));
            g.drawLine(x - r / 2, y - r / 3, x, y - r / 2);
        }
    }

    private void drawCar(Graphics2D g, int x, int y, Color body) {
        int w = 48, h = 78;
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(x - w / 2 - 5, y + h / 2 - 2, w + 10, 16);
        g.setColor(new Color(28, 29, 30));
        g.fillRoundRect(x - w / 2 - 5, y - h / 2 + 8, 11, 23, 5, 5);
        g.fillRoundRect(x + w / 2 - 6, y - h / 2 + 8, 11, 23, 5, 5);
        g.fillRoundRect(x - w / 2 - 5, y + h / 2 - 28, 11, 23, 5, 5);
        g.fillRoundRect(x + w / 2 - 6, y + h / 2 - 28, 11, 23, 5, 5);
        g.setColor(body);
        g.fillRoundRect(x - w / 2, y - h / 2, w, h, 15, 15);
        g.setColor(body.brighter());
        g.fillRoundRect(x - w / 3, y - h / 2 + 8, 2 * w / 3, 24, 10, 10);
        g.setColor(new Color(156, 219, 228));
        g.fillRoundRect(x - w / 3 + 3, y - h / 2 + 13, 2 * w / 3 - 6, 15, 7, 7);
        g.setColor(new Color(255, 246, 198));
        g.fillRoundRect(x - w / 2 + 5, y + h / 2 - 15, 10, 6, 3, 3);
        g.fillRoundRect(x + w / 2 - 15, y + h / 2 - 15, 10, 6, 3, 3);
        g.setColor(new Color(255, 255, 255, 120));
        g.drawLine(x, y - h / 2 + 5, x, y + h / 2 - 5);
    }

    private void drawHud(Graphics2D g) {
        g.setColor(new Color(14, 24, 28, 205));
        g.fillRoundRect(22, 20, 330, 105, 18, 18);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        g.drawString("PLAYER 1", 40, 48);
        drawBar(g, 130, 34, 190, 15, playerProgress / FINISH, new Color(235, 76, 61));
        g.drawString("RIVAL", 40, 78);
        drawBar(g, 130, 64, 190, 15, rivalProgress / FINISH, new Color(69, 169, 225));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(237, 243, 238));
        g.drawString("W / Up accelerate    S / Down brake", 40, 108);
        String mode = versusComputer ? "VS COMPUTER" : "TWO PLAYERS";
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString(mode, W - 155, 34);
    }

    private void drawBar(Graphics2D g, int x, int y, int w, int h, double value, Color color) {
        g.setColor(new Color(255, 255, 255, 50));
        g.fillRoundRect(x, y, w, h, h, h);
        g.setColor(color);
        g.fillRoundRect(x, y, (int)(w * clamp(value, 0, 1)), h, h, h);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(9, 24, 30, 190));
        g.fillRoundRect(210, 165, 580, 350, 30, 30);
        g.setColor(new Color(255, 255, 255, 35));
        g.drawRoundRect(210, 165, 580, 350, 30, 30);
        g.setFont(new Font("SansSerif", Font.BOLD, 38));
        g.setColor(Color.WHITE);
        centered(g, "CROSS-COUNTRY", 250);
        g.setColor(new Color(255, 204, 86));
        centered(g, "RACING ADVENTURE", 298);
        g.setFont(new Font("SansSerif", Font.BOLD, 21));
        g.setColor(Color.WHITE);
        centered(g, "Press 1  -  Race the computer", 376);
        centered(g, "Press 2  -  Two-player race", 417);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(new Color(220, 231, 229));
        centered(g, "P1: W/A/S/D     P2: Arrow keys", 466);
        centered(g, "Dodge rocks, hold accelerate, and reach the finish first!", 493);
    }

    private void drawResults(Graphics2D g) {
        g.setColor(new Color(10, 20, 27, 205));
        g.fillRoundRect(280, 220, 440, 220, 28, 28);
        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        g.setColor(new Color(255, 205, 87));
        centered(g, result, 305);
        g.setFont(new Font("SansSerif", Font.PLAIN, 19));
        g.setColor(Color.WHITE);
        centered(g, "Press Enter to race again", 365);
        centered(g, "Press 1 or 2 to switch game mode", 402);
    }

    private void centered(Graphics2D g, String text, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (W - fm.stringWidth(text)) / 2, y);
    }

    private static class Obstacle {
        final double x;
        double z = 1.0;
        boolean playerHit = false, rivalHit = false;
        Obstacle(double x) { this.x = x; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Cross-Country Racing Adventure");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setContentPane(new CrossCountryRacingAdventure());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}