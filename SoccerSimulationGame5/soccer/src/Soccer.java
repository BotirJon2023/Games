import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferStrategy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class SoccerSimulationGame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }

    // GameWindow creates the frame and embeds the GameCanvas
    static class GameWindow extends JFrame {
        private final GameCanvas canvas;

        public GameWindow() {
            super("Soccer Simulation Game - 2P or vs Computer");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());
            setResizable(false);

            int width = 1280;
            int height = 720;
            canvas = new GameCanvas(width, height);
            add(canvas, BorderLayout.CENTER);
            pack();

            setLocationRelativeTo(null);
            canvas.start();
        }
    }

    // Core game canvas with loop, update, render and input handling
    static class GameCanvas extends Canvas implements Runnable, KeyListener, MouseListener, MouseMotionListener {

        private Thread loopThread;
        private volatile boolean running = false;
        private final int WIDTH;
        private final int HEIGHT;

        private final KeyState keys = new KeyState();
        private final MouseState mouse = new MouseState();

        private final Random random = new Random();

        private GameState state = GameState.MENU;
        private Menu menu;
        private Match match;

        private long lastFrameTimeNanos = 0L;
        private double accumulator = 0.0;
        private final double fixedDt = 1.0 / 60.0;

        // Camera shake
        private double shakeTime = 0.0;
        private double shakeIntensity = 0.0;

        // Fonts
        private Font fontUI;
        private Font fontBig;
        private Font fontSmall;

        public GameCanvas(int w, int h) {
            this.WIDTH = w;
            this.HEIGHT = h;
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setIgnoreRepaint(true);
            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);

            menu = new Menu();
            match = null;

            // Initialize fonts
            try {
                fontUI = new Font("SansSerif", Font.BOLD, 22);
                fontBig = new Font("SansSerif", Font.BOLD, 72);
                fontSmall = new Font("SansSerif", Font.PLAIN, 16);
                // If system lacks SansSerif variations, fall back to defaults
                if (!GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames().toString().toLowerCase().contains("sansserif")) {
                    fontUI = new Font(Font.MONOSPACED, Font.BOLD, 20);
                    fontBig = new Font(Font.MONOSPACED, Font.BOLD, 60);
                    fontSmall = new Font(Font.MONOSPACED, Font.PLAIN, 14);
                }
            } catch (Exception ex) {
                fontUI = new Font(Font.MONOSPACED, Font.BOLD, 20);
                fontBig = new Font(Font.MONOSPACED, Font.BOLD, 60);
                fontSmall = new Font(Font.MONOSPACED, Font.PLAIN, 14);
            }
        }

        public void start() {
            if (running) return;
            createBufferStrategy(3);
            requestFocus();
            running = true;
            loopThread = new Thread(this, "GameLoop");
            loopThread.start();
        }

        public void stop() {
            running = false;
            try {
                if (loopThread != null) loopThread.join();
            } catch (InterruptedException ignored) {}
        }

        @Override
        public void run() {
            lastFrameTimeNanos = System.nanoTime();
            double fpsTimer = 0.0;
            int frames = 0;

            while (running) {
                long now = System.nanoTime();
                double dt = (now - lastFrameTimeNanos) / 1_000_000_000.0;
                if (dt > 0.25) dt = 0.25; // avoid huge steps
                lastFrameTimeNanos = now;

                accumulator += dt;
                // Update with fixed timestep
                while (accumulator >= fixedDt) {
                    update(fixedDt);
                    accumulator -= fixedDt;
                }

                // Render with interpolation
                double alpha = accumulator / fixedDt;
                render(alpha);

                // Show fps roughly each second (silent; could log if needed)
                fpsTimer += dt;
                frames++;
                if (fpsTimer >= 1.0) {
                    // System.out.println("FPS: " + frames);
                    fpsTimer = 0.0;
                    frames = 0;
                }

                // Yield a bit
                Toolkit.getDefaultToolkit().sync();
            }
        }

        private void update(double dt) {
            // Global toggles
            if (keys.isPressedOnce(KeyEvent.VK_ESCAPE)) {
                // quit
                System.exit(0);
            }

            switch (state) {
                case MENU:
                    menu.update(dt, keys, mouse);
                    if (menu.startRequested) {
                        // Build new match based on menu selection
                        boolean pvp = (menu.selectedMode == MenuMode.PVP);
                        match = new Match(WIDTH, HEIGHT, pvp);
                        state = GameState.PLAYING;
                        menu.startRequested = false;
                    }
                    break;

                case PLAYING:
                    if (keys.isPressedOnce(KeyEvent.VK_P)) {
                        state = GameState.PAUSED;
                        break;
                    }
                    if (keys.isPressedOnce(KeyEvent.VK_R)) {
                        // Restart
                        boolean pvp = match != null && match.pvp;
                        match = new Match(WIDTH, HEIGHT, pvp);
                    }
                    if (keys.isPressedOnce(KeyEvent.VK_M)) {
                        state = GameState.MENU;
                        menu = new Menu();
                        break;
                    }

                    if (match != null) {
                        match.update(dt, keys);
                        if (match.eventFlashTime > 0) {
                            // maybe trigger shake on goals
                            addShake(0.25, 8.0);
                        }
                        if (match.matchOver) {
                            state = GameState.GAME_OVER;
                        }
                    }
                    updateShake(dt);
                    break;

                case PAUSED:
                    if (keys.isPressedOnce(KeyEvent.VK_P)) {
                        state = GameState.PLAYING;
                    }
                    if (keys.isPressedOnce(KeyEvent.VK_R)) {
                        boolean pvp = match != null && match.pvp;
                        match = new Match(WIDTH, HEIGHT, pvp);
                        state = GameState.PLAYING;
                    }
                    if (keys.isPressedOnce(KeyEvent.VK_M)) {
                        state = GameState.MENU;
                        menu = new Menu();
                    }
                    break;

                case GAME_OVER:
                    if (keys.isPressedOnce(KeyEvent.VK_R)) {
                        boolean pvp = match != null && match.pvp;
                        match = new Match(WIDTH, HEIGHT, pvp);
                        state = GameState.PLAYING;
                    }
                    if (keys.isPressedOnce(KeyEvent.VK_M)) {
                        state = GameState.MENU;
                        menu = new Menu();
                    }
                    break;
            }

            // Clear "pressed once" states after processing
            keys.lateUpdate();
            mouse.lateUpdate();
        }

        private void render(double alpha) {
            BufferStrategy bs = getBufferStrategy();
            if (bs == null) return;
            Graphics2D g = (Graphics2D) bs.getDrawGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                // Clear background with gradient
                GradientPaint sky = new GradientPaint(0, 0, new Color(18, 41, 82), 0, HEIGHT, new Color(4, 14, 34));
                g.setPaint(sky);
                g.fillRect(0, 0, WIDTH, HEIGHT);

                // Apply camera shake
                AffineTransform oldTx = g.getTransform();
                if (shakeTime > 0) {
                    double dx = (random.nextDouble() * 2 - 1) * shakeIntensity;
                    double dy = (random.nextDouble() * 2 - 1) * shakeIntensity;
                    g.translate(dx, dy);
                }

                // Draw stands audience
                drawStands(g);

                // Draw field and match
                if (match != null) {
                    match.render(g, alpha);
                } else {
                    // field as background in menu
                    drawField(g, WIDTH, HEIGHT, null);
                }

                // Restore transform
                g.setTransform(oldTx);

                // UI overlays by state
                switch (state) {
                    case MENU:
                        menu.render(g, WIDTH, HEIGHT, fontBig, fontUI, fontSmall, mouse);
                        break;
                    case PLAYING:
                        drawTopHUD(g);
                        break;
                    case PAUSED:
                        drawTopHUD(g);
                        drawCenteredBanner(g, "Paused", new Color(255, 255, 255, 230), new Color(0, 0, 0, 120));
                        break;
                    case GAME_OVER:
                        drawTopHUD(g);
                        drawCenteredBanner(g, "Full Time", new Color(255, 255, 255, 230), new Color(0, 0, 0, 120));
                        drawGameOverOptions(g);
                        break;
                }

                // Watermark or title
                g.setFont(fontSmall);
                g.setColor(new Color(255, 255, 255, 120));
                g.drawString("SoccerSimulationGame - WASD/Arrow keys, Space/Enter to kick, Shift to sprint, P pause, R restart, M menu, Esc quit", 12, HEIGHT - 12);

            } finally {
                g.dispose();
            }
            bs.show();
        }

        private void drawTopHUD(Graphics2D g) {
            if (match == null) return;
            // Scoreboard container
            int w = 520;
            int h = 70;
            int x = WIDTH / 2 - w / 2;
            int y = 16;
            RoundRectangle2D.Double rr = new RoundRectangle2D.Double(x, y, w, h, 16, 16);
            g.setColor(new Color(0, 0, 0, 130));
            g.fill(rr);
            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(255, 255, 255, 60));
            g.draw(rr);

            // Team labels and scores
            g.setFont(fontUI);
            g.setColor(new Color(200, 220, 255));
            String leftName = match.leftTeamName;
            String rightName = match.rightTeamName;

            g.drawString(leftName, x + 16, y + 28);
            g.drawString(rightName, x + w - 16 - g.getFontMetrics().stringWidth(rightName), y + 28);

            // Scores
            g.setFont(fontBig.deriveFont(48f));
            String score = match.leftScore + "  -  " + match.rightScore;
            int sw = g.getFontMetrics().stringWidth(score);
            g.setColor(Color.WHITE);
            g.drawString(score, x + w / 2 - sw / 2, y + 54);

            // Time
            g.setFont(fontUI);
            String time = formatTime(match.timeRemaining);
            int tw = g.getFontMetrics().stringWidth(time);
            g.setColor(new Color(255, 230, 180));
            g.drawString(time, x + w / 2 - tw / 2, y + 24);

            // Possession bar
            int barW = w - 80;
            int barH = 8;
            int bx = x + 40;
            int by = y + h + 6;
            double posRatio = match.getLeftPossessionRatio();
            Color lc1 = new Color(50, 120, 255);
            Color rc1 = new Color(240, 60, 60);
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(bx, by, barW, barH, 8, 8);
            int leftW = (int) Math.round(barW * posRatio);
            g.setColor(lc1);
            g.fillRoundRect(bx, by, leftW, barH, 8, 8);
            g.setColor(rc1);
            g.fillRoundRect(bx + leftW, by, barW - leftW, barH, 8, 8);
            g.setColor(new Color(255, 255, 255, 120));
            g.drawRoundRect(bx, by, barW, barH, 8, 8);
        }

        private void drawCenteredBanner(Graphics2D g, String text, Color fg, Color bg) {
            int bw = 500;
            int bh = 140;
            int x = WIDTH / 2 - bw / 2;
            int y = HEIGHT / 2 - bh / 2;
            RoundRectangle2D.Double rr = new RoundRectangle2D.Double(x, y, bw, bh, 24, 24);
            g.setColor(bg);
            g.fill(rr);
            g.setColor(new Color(255, 255, 255, 120));
            g.setStroke(new BasicStroke(2f));
            g.draw(rr);
            g.setFont(fontBig);
            int sw = g.getFontMetrics().stringWidth(text);
            g.setColor(fg);
            g.drawString(text, WIDTH / 2 - sw / 2, y + 84);

            g.setFont(fontSmall);
            String tip = "Press R to restart or M for menu";
            int tw = g.getFontMetrics().stringWidth(tip);
            g.setColor(new Color(255, 255, 255, 180));
            g.drawString(tip, WIDTH / 2 - tw / 2, y + bh - 16);
        }

        private void drawGameOverOptions(Graphics2D g) {
            // Display winner
            if (match == null) return;
            String winner;
            if (match.leftScore > match.rightScore) winner = match.leftTeamName + " win!";
            else if (match.leftScore < match.rightScore) winner = match.rightTeamName + " win!";
            else winner = "It's a draw!";
            g.setFont(fontUI.deriveFont(26f));
            int sw = g.getFontMetrics().stringWidth(winner);
            g.setColor(Color.WHITE);
            g.drawString(winner, WIDTH / 2 - sw / 2, HEIGHT / 2 + 120);
        }

        private void drawStands(Graphics2D g) {
            int h = 120;
            int y = 0;
            // Top stands gradient band
            GradientPaint gp = new GradientPaint(0, y, new Color(15, 15, 24), 0, y + h, new Color(28, 28, 42));
            g.setPaint(gp);
            g.fillRect(0, y, WIDTH, h);
            // Silhouette of crowd as noise bands
            g.setColor(new Color(0, 0, 0, 60));
            for (int i = 0; i < 3; i++) {
                int cy = y + 20 + i * 26;
                for (int x = 0; x < WIDTH; x += 6) {
                    int hh = 8 + (int) (Math.sin((x + i * 132) * 0.03) * 4);
                    g.fillRect(x, cy, 6, hh);
                }
            }
            // Banners
            g.setColor(new Color(220, 220, 240, 50));
            g.fillRoundRect(WIDTH / 2 - 130, 30, 260, 28, 10, 10);
            g.setColor(new Color(255, 255, 255, 110));
            g.setFont(fontSmall);
            String banner = "Welcome to the Arena";
            int bw = g.getFontMetrics().stringWidth(banner);
            g.drawString(banner, WIDTH / 2 - bw / 2, 50);
        }

        private void drawField(Graphics2D g, int w, int h, Match localMatch) {
            // Field gradient
            int fy = 100;
            int fh = h - fy - 40;
            GradientPaint grass = new GradientPaint(0, fy, new Color(30, 110, 50), 0, fy + fh, new Color(20, 90, 40));
            g.setPaint(grass);
            g.fillRect(0, fy, w, fh);

            // Mowing stripes
            int stripes = 12;
            for (int i = 0; i < stripes; i++) {
                int sy = fy + (int) (i * (fh / (double) stripes));
                int sh = (int) Math.ceil(fh / (double) stripes);
                Color c = (i % 2 == 0) ? new Color(35, 125, 55, 70) : new Color(35, 125, 55, 20);
                g.setColor(c);
                g.fillRect(0, sy, w, sh);
            }

            // Field lines and goals
            int margin = 60;
            int top = fy + margin;
            int bottom = fy + fh - margin;
            int left = margin;
            int right = w - margin;
            g.setColor(new Color(255, 255, 255, 200));
            g.setStroke(new BasicStroke(3f));

            // Outer lines
            g.drawRect(left, top, right - left, bottom - top);

            // Mid line
            g.drawLine((left + right) / 2, top, (left + right) / 2, bottom);

            // Center circle and spot
            int radius = 80;
            g.draw(new Ellipse2D.Double((left + right) / 2 - radius, (top + bottom) / 2 - radius, radius * 2, radius * 2));
            g.fill(new Ellipse2D.Double((left + right) / 2 - 3, (top + bottom) / 2 - 3, 6, 6));

            // Penalty areas and spots
            int areaW = 160;
            int areaH = 280;
            // Left
            g.drawRect(left, (top + bottom) / 2 - areaH / 2, areaW, areaH);
            // Right
            g.drawRect(right - areaW, (top + bottom) / 2 - areaH / 2, areaW, areaH);

            // Small boxes
            int boxW = 60;
            int boxH = 160;
            g.drawRect(left, (top + bottom) / 2 - boxH / 2, boxW, boxH);
            g.drawRect(right - boxW, (top + bottom) / 2 - boxH / 2, boxW, boxH);

            // Penalty spots
            g.fill(new Ellipse2D.Double(left + areaW - 12, (top + bottom) / 2 - 3, 6, 6));
            g.fill(new Ellipse2D.Double(right - areaW + 6, (top + bottom) / 2 - 3, 6, 6));

            // Goals posts
            int goalW = 14;
            int goalH = 160;
            int goalY = (top + bottom) / 2 - goalH / 2;
            g.setStroke(new BasicStroke(4f));
            // Left goal line
            g.drawLine(left, goalY, left, goalY + goalH);
            // Right goal line
            g.drawLine(right, goalY, right, goalY + goalH);

            // Nets (decorative)
            g.setColor(new Color(255, 255, 255, 80));
            for (int i = 0; i <= 10; i++) {
                int nx = left - i * 8;
                g.drawLine(left, goalY + i * (goalH / 10), nx, goalY + i * (goalH / 10));
            }
            for (int i = 0; i <= 10; i++) {
                int nx = right + i * 8;
                g.drawLine(right, goalY + i * (goalH / 10), nx, goalY + i * (goalH / 10));
            }
        }

        private void addShake(double time, double intensity) {
            shakeTime = Math.max(shakeTime, time);
            shakeIntensity = Math.max(shakeIntensity, intensity);
        }

        private void updateShake(double dt) {
            if (shakeTime > 0) {
                shakeTime -= dt;
                if (shakeTime <= 0) {
                    shakeTime = 0;
                    shakeIntensity = 0;
                } else {
                    // Dampen intensity
                    shakeIntensity *= 0.92;
                }
            }
        }

        private static String formatTime(double seconds) {
            if (seconds < 0) seconds = 0;
            int m = (int) (seconds / 60);
            int s = (int) (seconds % 60);
            return String.format("%d:%02d", m, s);
        }

        // Input events
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            keys.setKey(e.getKeyCode(), true);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            keys.setKey(e.getKeyCode(), false);
        }

        @Override
        public void mouseClicked(MouseEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) {
            mouse.setPressed(true);
            mouse.setPos(e.getX(), e.getY());
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mouse.setPressed(false);
            mouse.setPos(e.getX(), e.getY());
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            mouse.setInside(true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            mouse.setInside(false);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            mouse.setPos(e.getX(), e.getY());
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mouse.setPos(e.getX(), e.getY());
        }

        // Menu inner class
        class Menu {
            MenuMode selectedMode = MenuMode.PVC;
            boolean startRequested = false;

            private int hoverIndex = -1;

            void update(double dt, KeyState keys, MouseState mouse) {
                // Keyboard cycling
                if (keys.isPressedOnce(KeyEvent.VK_UP) || keys.isPressedOnce(KeyEvent.VK_W)) {
                    selectedMode = (selectedMode == MenuMode.PVC) ? MenuMode.PVP : MenuMode.PVC;
                }
                if (keys.isPressedOnce(KeyEvent.VK_DOWN) || keys.isPressedOnce(KeyEvent.VK_S)) {
                    selectedMode = (selectedMode == MenuMode.PVC) ? MenuMode.PVP : MenuMode.PVC;
                }
                if (keys.isPressedOnce(KeyEvent.VK_ENTER) || keys.isPressedOnce(KeyEvent.VK_SPACE)) {
                    startRequested = true;
                }

                // Mouse hover and click
                hoverIndex = -1;
                List<MenuItem> items = buildItems();
                for (int i = 0; i < items.size(); i++) {
                    MenuItem it = items.get(i);
                    if (it.bounds.contains(mouse.x, mouse.y)) {
                        hoverIndex = i;
                        if (mouse.clicked()) {
                            if (it.label.contains("Player vs Computer")) selectedMode = MenuMode.PVC;
                            if (it.label.contains("Player vs Player")) selectedMode = MenuMode.PVP;
                            if (it.label.contains("Start")) startRequested = true;
                            if (it.label.contains("Quit")) System.exit(0);
                        }
                    }
                }
            }

            void render(Graphics2D g, int w, int h, Font big, Font ui, Font small, MouseState mouse) {
                // Draw field background
                drawField(g, w, h, null);

                // Title
                g.setFont(big);
                String title = "Soccer Simulation";
                int sw = g.getFontMetrics().stringWidth(title);
                g.setColor(new Color(255, 255, 255, 240));
                g.drawString(title, w / 2 - sw / 2, 210);

                // Subtitle
                g.setFont(ui);
                String subtitle = "Choose mode";
                int su = g.getFontMetrics().stringWidth(subtitle);
                g.setColor(new Color(255, 255, 255, 180));
                g.drawString(subtitle, w / 2 - su / 2, 250);

                // Mode toggles
                int bw = 420;
                int bh = 58;
                int x = w / 2 - bw / 2;
                int y = 290;
                RoundRectangle2D.Double opt1 = new RoundRectangle2D.Double(x, y, bw, bh, 16, 16);
                RoundRectangle2D.Double opt2 = new RoundRectangle2D.Double(x, y + 70, bw, bh, 16, 16);

                drawOption(g, opt1, "Player vs Computer", selectedMode == MenuMode.PVC, hoverIndex == 0);
                drawOption(g, opt2, "Player vs Player", selectedMode == MenuMode.PVP, hoverIndex == 1);

                // Buttons
                RoundRectangle2D.Double start = new RoundRectangle2D.Double(x, y + 160, bw, bh, 16, 16);
                RoundRectangle2D.Double quit = new RoundRectangle2D.Double(x, y + 230, bw, bh, 16, 16);

                drawButton(g, start, "Start Match", hoverIndex == 2);
                drawButton(g, quit, "Quit", hoverIndex == 3);

                // Build interactive zones for hover/click
                List<MenuItem> items = buildItems();
                items.get(0).bounds = opt1.getBounds2D();
                items.get(1).bounds = opt2.getBounds2D();
                items.get(2).bounds = start.getBounds2D();
                items.get(3).bounds = quit.getBounds2D();

                // Mouse cursor highlight
                if (mouse.inside) {
                    g.setColor(new Color(255, 255, 255, 50));
                    g.fillOval(mouse.x - 10, mouse.y - 10, 20, 20);
                }

                // Tips
                g.setFont(small);
                g.setColor(new Color(255, 255, 255, 160));
                g.drawString("Tip: Hold Kick for more power. Sprint drains stamina temporarily.", 20, h - 40);
                g.drawString("Controls: P pause, R restart, M menu, Esc quit", 20, h - 20);
            }

            private void drawOption(Graphics2D g, RoundRectangle2D.Double r, String text, boolean selected, boolean hover) {
                Color base = new Color(0, 0, 0, 120);
                Color sel = new Color(0, 0, 0, 160);
                Color col = selected ? sel : base;

                g.setColor(col);
                g.fill(r);
                g.setStroke(new BasicStroke(2f));
                g.setColor(new Color(255, 255, 255, hover ? 200 : 120));
                g.draw(r);

                g.setFont(fontUI);
                int sw = g.getFontMetrics().stringWidth(text);
                g.setColor(Color.WHITE);
                g.drawString(text, (int) (r.x + r.width / 2 - sw / 2), (int) (r.y + 38));

                if (selected) {
                    g.setColor(new Color(120, 200, 255, 180));
                    g.fillRoundRect((int) (r.x + 12), (int) (r.y + 12), 20, 20, 8, 8);
                } else {
                    g.setColor(new Color(255, 255, 255, 60));
                    g.drawRoundRect((int) (r.x + 12), (int) (r.y + 12), 20, 20, 8, 8);
                }
            }

            private void drawButton(Graphics2D g, RoundRectangle2D.Double r, String text, boolean hover) {
                g.setColor(new Color(0, 0, 0, hover ? 170 : 130));
                g.fill(r);
                g.setStroke(new BasicStroke(2f));
                g.setColor(new Color(255, 255, 255, 120));
                g.draw(r);
                g.setFont(fontUI);
                int sw = g.getFontMetrics().stringWidth(text);
                g.setColor(Color.WHITE);
                g.drawString(text, (int) (r.x + r.width / 2 - sw / 2), (int) (r.y + 38));
            }

            private List<MenuItem> buildItems() {
                List<MenuItem> items = new ArrayList<>();
                items.add(new MenuItem("Player vs Computer"));
                items.add(new MenuItem("Player vs Player"));
                items.add(new MenuItem("Start Match"));
                items.add(new MenuItem("Quit"));
                return items;
            }

            class MenuItem {
                String label;
                java.awt.geom.Rectangle2D bounds;
                MenuItem(String l) { label = l; }
            }
        }
    }

    enum GameState { MENU, PLAYING, PAUSED, GAME_OVER }
    enum MenuMode { PVP, PVC }

    // KeyState utility for immediate and "pressed once" logic
    static class KeyState {
        private final boolean[] down = new boolean[256];
        private final boolean[] pressedOnce = new boolean[256];

        public void setKey(int keyCode, boolean isDown) {
            if (keyCode < 0 || keyCode >= down.length) return;
            if (isDown && !down[keyCode]) {
                pressedOnce[keyCode] = true;
            }
            down[keyCode] = isDown;
        }

        public boolean isDown(int keyCode) {
            if (keyCode < 0 || keyCode >= down.length) return false;
            return down[keyCode];
        }

        public boolean isPressedOnce(int keyCode) {
            if (keyCode < 0 || keyCode >= pressedOnce.length) return false;
            return pressedOnce[keyCode];
        }

        public void lateUpdate() {
            for (int i = 0; i < pressedOnce.length; i++) {
                pressedOnce[i] = false;
            }
        }
    }

    // Mouse state helper
    static class MouseState {
        int x = 0, y = 0;
        boolean pressed = false;
        boolean inside = false;
        boolean pressedOnce = false;

        void setPos(int nx, int ny) {
            this.x = nx;
            this.y = ny;
        }

        void setInside(boolean b) {
            this.inside = b;
        }

        void setPressed(boolean b) {
            if (b && !pressed) pressedOnce = true;
            this.pressed = b;
        }

        boolean clicked() {
            return pressedOnce;
        }

        void lateUpdate() {
            pressedOnce = false;
        }
    }

    // Math helper Vector2
    static class Vec2 {
        double x, y;

        Vec2() { this(0, 0); }
        Vec2(double x, double y) { this.x = x; this.y = y; }

        Vec2 set(double nx, double ny) { this.x = nx; this.y = ny; return this; }
        Vec2 add(Vec2 v) { this.x += v.x; this.y += v.y; return this; }
        Vec2 sub(Vec2 v) { this.x -= v.x; this.y -= v.y; return this; }
        Vec2 mul(double s) { this.x *= s; this.y *= s; return this; }
        Vec2 div(double s) { this.x /= s; this.y /= s; return this; }
        double len() { return Math.sqrt(x * x + y * y); }
        double len2() { return x * x + y * y; }
        Vec2 nor() { double l = len(); if (l > 1e-8) { x /= l; y /= l; } return this; }
        Vec2 cpy() { return new Vec2(x, y); }
        Vec2 clamp(double max) { double l = len(); if (l > max) { x = x / l * max; y = y / l * max; } return this; }
        static Vec2 fromAngle(double ang) { return new Vec2(Math.cos(ang), Math.sin(ang)); }
        double ang() { return Math.atan2(y, x); }
        static double dot(Vec2 a, Vec2 b) { return a.x * b.x + a.y * b.y; }
    }

    // Match: all entities and logic for a single game
    static class Match {

        final int WIDTH;
        final int HEIGHT;

        // Field geometry
        final int top, bottom, left, right;
        final int goalY, goalH, goalLeftX, goalRightX;
        final int centerX, centerY;

        // Teams
        final String leftTeamName = "Blue";
        final String rightTeamName = "Red";

        // Entities
        final Ball ball;
        final Player leftPlayer;
        final Player rightPlayer;
        final AIPlayer aiRight; // if pvp is false
        final AIPlayer aiLeft;  // if needed for demo (not used here)
        final List<Player> players = new ArrayList<>();

        // Particles
        final CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();
        final ArrayDeque<Vec2> ballTrail = new ArrayDeque<>();

        // Scoring and time
        int leftScore = 0;
        int rightScore = 0;
        double matchDuration = 3 * 60; // 3 minutes
        double timeRemaining = matchDuration;
        boolean matchOver = false;

        // Possession tracking
        double leftPossessionTime = 0;
        double rightPossessionTime = 0;
        int lastPossession = 0; // -1 none, 0 left, 1 right

        // Control mode
        final boolean pvp;

        // Visual events
        double eventFlashTime = 0.0; // used to show goal flash

        // Random
        final Random random = new Random();

        // Stamina
        final double maxStamina = 100.0;

        public Match(int width, int height, boolean pvp) {
            this.WIDTH = width;
            this.HEIGHT = height;
            int margin = 60;
            int fy = 100;
            int fh = HEIGHT - fy - 40;
            int fieldTop = fy + margin;
            int fieldBottom = fy + fh - margin;
            int fieldLeft = margin;
            int fieldRight = WIDTH - margin;

            this.top = fieldTop;
            this.bottom = fieldBottom;
            this.left = fieldLeft;
            this.right = fieldRight;

            this.goalH = 160;
            this.goalY = (top + bottom) / 2 - goalH / 2;
            this.goalLeftX = left;
            this.goalRightX = right;

            this.centerX = (left + right) / 2;
            this.centerY = (top + bottom) / 2;

            this.pvp = pvp;

            ball = new Ball(centerX, centerY);

            leftPlayer = new Player(true, new Color(50, 140, 255), new Color(24, 90, 210), this);
            rightPlayer = new Player(false, new Color(240, 80, 80), new Color(190, 34, 34), this);

            leftPlayer.position.set(centerX - 240, centerY);
            rightPlayer.position.set(centerX + 240, centerY);

            players.add(leftPlayer);
            players.add(rightPlayer);

            aiRight = pvp ? null : new AIPlayer(false, rightPlayer, this);
            aiLeft = null; // can be used for training mode

            resetKickoff(true);
        }

        void resetKickoff(boolean leftStarts) {
            ball.position.set(centerX, centerY);
            ball.velocity.set(0, 0);
            leftPlayer.position.set(centerX - 240, centerY);
            rightPlayer.position.set(centerX + 240, centerY);
            leftPlayer.velocity.set(0, 0);
            rightPlayer.velocity.set(0, 0);

            leftPlayer.stamina = maxStamina;
            rightPlayer.stamina = maxStamina;
            leftPlayer.kickCharge = 0;
            rightPlayer.kickCharge = 0;

            lastPossession = -1;

            // small delay visual
            eventFlashTime = 0.0;
        }

        void update(double dt, KeyState keys) {
            if (matchOver) return;

            // Time
            timeRemaining -= dt;
            if (timeRemaining <= 0) {
                timeRemaining = 0;
                matchOver = true;
            }

            // Input and AI
            leftPlayer.handleInput(dt, keys, true);
            if (pvp) {
                rightPlayer.handleInput(dt, keys, false);
            } else {
                // Player movement for right is AI-driven
                rightPlayer.handleInputForAI(dt);
                aiRight.update(dt);
            }

            // Update physics
            ball.update(dt);

            // Player updates
            for (Player p : players) {
                p.update(dt);
            }

            // Player-ball interactions
            handleBallInteractions(dt);

            // Player-player separation
            handlePlayerCollisions();

            // Ball wall collision and goal detection
            handleFieldCollisionsAndGoals();

            // Particles update
            updateParticles(dt);

            // Ball trail
            updateBallTrail(dt);

            // Possession update
            updatePossession(dt);

            // Decrease event flash
            if (eventFlashTime > 0) {
                eventFlashTime -= dt;
                if (eventFlashTime < 0) eventFlashTime = 0;
            }
        }

        void render(Graphics2D g, double alpha) {
            // Field
            drawField(g);

            // Goals nets dynamic ripple when goal
            drawGoals(g);

            // Ball trail
            drawBallTrail(g);

            // Players
            for (Player p : players) {
                p.render(g, alpha);
            }

            // Ball
            ball.render(g, alpha);

            // Particles
            for (Particle pe : particles) {
                pe.render(g);
            }

            // Flash for goal
            if (eventFlashTime > 0) {
                int a = (int) (Math.sin(eventFlashTime * 20) * 60 + 80);
                g.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, a))));
                g.setStroke(new BasicStroke(6f));
                g.drawRect(left, top, right - left, bottom - top);
            }
        }

        private void drawField(Graphics2D g) {
            // Use the outer method in canvas for consistent visuals, but we need match for geometry
            // Re-implement minimal field due to no direct canvas reference
            int w = WIDTH;
            int h = HEIGHT;
            // Field gradient
            int fy = 100;
            int fh = h - fy - 40;
            GradientPaint grass = new GradientPaint(0, fy, new Color(30, 110, 50), 0, fy + fh, new Color(20, 90, 40));
            g.setPaint(grass);
            g.fillRect(0, fy, w, fh);

            // Mowing stripes
            int stripes = 12;
            for (int i = 0; i < stripes; i++) {
                int sy = fy + (int) (i * (fh / (double) stripes));
                int sh = (int) Math.ceil(fh / (double) stripes);
                Color c = (i % 2 == 0) ? new Color(35, 125, 55, 70) : new Color(35, 125, 55, 20);
                g.setColor(c);
                g.fillRect(0, sy, w, sh);
            }

            // Lines
            g.setColor(new Color(255, 255, 255, 200));
            g.setStroke(new BasicStroke(3f));

            // Outer lines
            g.drawRect(left, top, right - left, bottom - top);

            // Mid line
            g.drawLine((left + right) / 2, top, (left + right) / 2, bottom);

            // Center circle and spot
            int radius = 80;
            g.draw(new Ellipse2D.Double((left + right) / 2 - radius, (top + bottom) / 2 - radius, radius * 2, radius * 2));
            g.fill(new Ellipse2D.Double((left + right) / 2 - 3, (top + bottom) / 2 - 3, 6, 6));

            // Penalty areas and spots
            int areaW = 160;
            int areaH = 280;
            g.drawRect(left, (top + bottom) / 2 - areaH / 2, areaW, areaH);
            g.drawRect(right - areaW, (top + bottom) / 2 - areaH / 2, areaW, areaH);

            int boxW = 60;
            int boxH = 160;
            g.drawRect(left, (top + bottom) / 2 - boxH / 2, boxW, boxH);
            g.drawRect(right - boxW, (top + bottom) / 2 - boxH / 2, boxW, boxH);

            // Penalty spots
            g.fill(new Ellipse2D.Double(left + areaW - 12, (top + bottom) / 2 - 3, 6, 6));
            g.fill(new Ellipse2D.Double(right - areaW + 6, (top + bottom) / 2 - 3, 6, 6));

            // Corner arcs (decorative)
            g.drawArc(left - 12, top - 12, 24, 24, 0, 90);
            g.drawArc(right - 12, top - 12, 24, 24, 90, 90);
            g.drawArc(left - 12, bottom - 12, 24, 24, 270, 90);
            g.drawArc(right - 12, bottom - 12, 24, 24, 180, 90);
        }

        private void drawGoals(Graphics2D g) {
            g.setStroke(new BasicStroke(4f));
            g.setColor(new Color(255, 255, 255, 200));

            // Posts
            g.draw(new Line2D.Double(left, goalY, left, goalY + goalH));
            g.draw(new Line2D.Double(right, goalY, right, goalY + goalH));

            // Nets with slight curve if recent goal
            double ripple = (eventFlashTime > 0) ? (Math.sin((1.0 - eventFlashTime) * Math.PI) * 10.0) : 0.0;
            g.setColor(new Color(255, 255, 255, 90));

            for (int i = 0; i <= 10; i++) {
                int yy = goalY + i * (goalH / 10);
                int nxL = (int) (left - i * 10 - ripple);
                int nxR = (int) (right + i * 10 + ripple);
                g.drawLine(left, yy, nxL, yy);
                g.drawLine(right, yy, nxR, yy);
            }
        }

        private void handleBallInteractions(double dt) {
            double playerRadius = 18;
            double kickDistance = 26;

            // Dribble coefficient
            double dribblePull = 38;

            for (Player p : players) {
                // Separation and dribble
                Vec2 toBall = ball.position.cpy().sub(p.position);
                double dist = toBall.len();
                // Kick handling
                if (p.wantKickRelease && dist < kickDistance + 8) {
                    // Kick power scaling
                    double basePower = 420;
                    double power = basePower * (0.2 + 0.8 * Math.min(1.0, p.kickCharge / p.maxKickCharge));
                    // Direction: blend player's facing and ball vector
                    Vec2 dir = new Vec2(Math.cos(p.facing), Math.sin(p.facing));
                    if (dist > 1e-3) {
                        Vec2 toBallN = toBall.cpy().nor();
                        dir.mul(0.65).add(toBallN.mul(0.35)).nor();
                    }
                    ball.velocity.add(dir.mul(power));

                    // Spin chance: small perpendicular tweak
                    double spin = (random() - 0.5) * 60;
                    double ang = Math.atan2(ball.velocity.y, ball.velocity.x);
                    ang += Math.toRadians(spin / 180.0);
                    double spd = ball.velocity.len();
                    ball.velocity.set(Math.cos(ang) * spd, Math.sin(ang) * spd);

                    // Particle burst
                    spawnKickParticles(p, dir, power);

                    // Camera shake-ish via eventFlash triggers outside
                    p.wantKickRelease = false;
                    p.kickCharge = 0;
                } else if (!p.wantKickRelease && p.kickCharging) {
                    // continue charging while holding
                } else {
                    // Dribble if close: attract the ball to the player's front when moving slowly relative to ball
                    double att = Math.max(0, dribblePull - dist);
                    if (att > 0) {
                        Vec2 target = p.position.cpy().add(Vec2.fromAngle(p.facing).mul(18));
                        Vec2 toTarget = target.sub(ball.position);
                        ball.velocity.add(toTarget.mul(6 * dt));
                    }
                }

                // If overlapping with ball, separate
                if (dist < playerRadius + ball.radius) {
                    Vec2 n = (dist > 1e-6) ? toBall.div(dist) : new Vec2(1, 0);
                    double pen = (playerRadius + ball.radius) - dist;
                    ball.position.add(n.cpy().mul(pen + 0.5));
                    // Add slight push to ball
                    ball.velocity.add(n.mul(80));
                }
            }
        }

        private void handlePlayerCollisions() {
            // Only two players here; but generic for list
            for (int i = 0; i < players.size(); i++) {
                for (int j = i + 1; j < players.size(); j++) {
                    Player a = players.get(i);
                    Player b = players.get(j);
                    double r = 18 + 18;
                    Vec2 ab = b.position.cpy().sub(a.position);
                    double d2 = ab.len2();
                    if (d2 < r * r) {
                        double d = Math.sqrt(Math.max(1e-6, d2));
                        Vec2 n = (d > 1e-6) ? ab.cpy().div(d) : new Vec2(1, 0);
                        double pen = r - d;
                        a.position.add(n.cpy().mul(-pen * 0.5));
                        b.position.add(n.cpy().mul(pen * 0.5));

                        // Bounce velocities slightly
                        double sep = 40;
                        a.velocity.add(n.cpy().mul(-sep));
                        b.velocity.add(n.cpy().mul(sep));
                    }
                }
            }
        }

        private void handleFieldCollisionsAndGoals() {
            // Ball with field boundaries
            // Top and bottom reflect
            if (ball.position.y - ball.radius < top) {
                ball.position.y = top + ball.radius;
                ball.velocity.y = -ball.velocity.y * 0.7;
            }
            if (ball.position.y + ball.radius > bottom) {
                ball.position.y = bottom - ball.radius;
                ball.velocity.y = -ball.velocity.y * 0.7;
            }

            // Left and right: detect if within goal mouth
            boolean inGoalMouthY = (ball.position.y > goalY && ball.position.y < goalY + goalH);
            if (ball.position.x - ball.radius < left) {
                if (inGoalMouthY) {
                    // Goal for right team
                    rightScore++;
                    celebrateGoal(false);
                    resetKickoff(true);
                } else {
                    ball.position.x = left + ball.radius;
                    ball.velocity.x = -ball.velocity.x * 0.7;
                }
            }
            if (ball.position.x + ball.radius > right) {
                if (inGoalMouthY) {
                    // Goal for left team
                    leftScore++;
                    celebrateGoal(true);
                    resetKickoff(false);
                } else {
                    ball.position.x = right - ball.radius;
                    ball.velocity.x = -ball.velocity.x * 0.7;
                }
            }
        }

        private void celebrateGoal(boolean leftTeam) {
            eventFlashTime = 1.2;
            // Burst particles at goal line
            Vec2 pos = new Vec2(leftTeam ? right - 4 : left + 4, centerY);
            for (int i = 0; i < 120; i++) {
                double ang = random() * Math.PI * 2;
                double spd = 200 + random() * 200;
                Vec2 v = new Vec2(Math.cos(ang) * spd, Math.sin(ang) * spd);
                Color c = leftTeam ? new Color(80, 180, 255) : new Color(255, 100, 100);
                particles.add(Particle.spark(pos.cpy(), v, c));
            }
        }

        private void updateParticles(double dt) {
            for (Particle p : particles) {
                p.update(dt);
                if (p.dead) particles.remove(p);
            }
        }

        private void spawnKickParticles(Player p, Vec2 dir, double power) {
            Vec2 origin = ball.position.cpy();
            int count = (int) Math.min(20, 6 + power / 60);
            for (int i = 0; i < count; i++) {
                double spread = (random() - 0.5) * 0.8;
                Vec2 v = dir.cpy().nor().mul(120 + random() * 200);
                double ca = Math.cos(spread), sa = Math.sin(spread);
                double vx = v.x * ca - v.y * sa;
                double vy = v.x * sa + v.y * ca;
                Color c = new Color(255, 240, 200, 220);
                particles.add(Particle.dust(origin.cpy(), new Vec2(vx, vy), c));
            }
            // Grass spray
            for (int i = 0; i < 12; i++) {
                double ang = random() * Math.PI * 2;
                double spd = 60 + random() * 120;
                Vec2 v = new Vec2(Math.cos(ang) * spd, Math.sin(ang) * spd);
                Color c = new Color(30, 120, 60, 200);
                particles.add(Particle.grass(origin.cpy(), v, c));
            }
        }

        private void updateBallTrail(double dt) {
            // Add current ball position to trail
            if (ballTrail.size() > 60) ballTrail.pollFirst();
            ballTrail.addLast(ball.position.cpy());
        }

        private void drawBallTrail(Graphics2D g) {
            if (ballTrail.isEmpty()) return;
            Vec2 prev = null;
            int idx = 0;
            int n = ballTrail.size();
            for (Vec2 v : ballTrail) {
                if (prev != null) {
                    float a = (float) idx / (float) n;
                    int alpha = (int) (a * 80);
                    g.setColor(new Color(255, 255, 255, alpha));
                    g.setStroke(new BasicStroke(2f));
                    g.drawLine((int) prev.x, (int) prev.y, (int) v.x, (int) v.y);
                }
                prev = v;
                idx++;
            }
        }

        private void updatePossession(double dt) {
            // Who is closest to ball?
            double dL = leftPlayer.position.cpy().sub(ball.position).len();
            double dR = rightPlayer.position.cpy().sub(ball.position).len();
            int pos = -1;
            if (dL < dR && dL < 48) pos = 0;
            else if (dR < dL && dR < 48) pos = 1;

            if (pos == 0) leftPossessionTime += dt;
            else if (pos == 1) rightPossessionTime += dt;

            if (pos != -1) lastPossession = pos;
        }

        double getLeftPossessionRatio() {
            double sum = leftPossessionTime + rightPossessionTime;
            if (sum < 1e-6) return 0.5;
            return leftPossessionTime / sum;
        }

        private double random() {
            return random.nextDouble();
        }
    }

    // Player entity
    static class Player {

        final boolean isLeftTeam;
        final Color shirtColor;
        final Color darkColor;
        final Match match;

        final Vec2 position = new Vec2();
        final Vec2 velocity = new Vec2();
        double facing = 0; // radians
        double speed = 220;
        double sprintSpeed = 320;
        double stamina = 100.0;
        double staminaRecover = 18.0;
        double staminaDrain = 28.0;

        boolean sprinting = false;

        // Kick charge
        double kickCharge = 0;
        double maxKickCharge = 0.75;
        boolean kickCharging = false;
        boolean wantKickRelease = false;

        // Input mapping
        int upKey, downKey, leftKey, rightKey, kickKey, sprintKey, switchKey;

        // Visual bob
        double bobTime = 0;

        // For AI control bridging
        Vec2 aiMoveDir = new Vec2(0, 0);
        boolean aiKickHold = false;
        boolean aiKickRelease = false;
        boolean aiSprint = false;

        public Player(boolean isLeftTeam, Color shirtColor, Color darkColor, Match match) {
            this.isLeftTeam = isLeftTeam;
            this.shirtColor = shirtColor;
            this.darkColor = darkColor;
            this.match = match;
            assignKeys();
        }

        private void assignKeys() {
            if (isLeftTeam) {
                upKey = KeyEvent.VK_W;
                downKey = KeyEvent.VK_S;
                leftKey = KeyEvent.VK_A;
                rightKey = KeyEvent.VK_D;
                kickKey = KeyEvent.VK_SPACE;
                sprintKey = KeyEvent.VK_SHIFT;
                switchKey = KeyEvent.VK_Q;
            } else {
                upKey = KeyEvent.VK_UP;
                downKey = KeyEvent.VK_DOWN;
                leftKey = KeyEvent.VK_LEFT;
                rightKey = KeyEvent.VK_RIGHT;
                kickKey = KeyEvent.VK_ENTER;
                sprintKey = KeyEvent.VK_SHIFT; // right shift also VK_SHIFT reported same; ok.
                switchKey = KeyEvent.VK_SLASH;
            }
        }

        void handleInput(double dt, KeyState keys, boolean isLeftPlayer) {
            // Direction
            Vec2 dir = new Vec2(0, 0);
            if (keys.isDown(upKey)) dir.y -= 1;
            if (keys.isDown(downKey)) dir.y += 1;
            if (keys.isDown(leftKey)) dir.x -= 1;
            if (keys.isDown(rightKey)) dir.x += 1;
            if (dir.len2() > 0) dir.nor();

            // Sprint
            sprinting = keys.isDown(sprintKey) && stamina > 8;
            double maxSpd = sprinting ? sprintSpeed : speed;

            // Accelerate towards target dir
            double accel = sprinting ? 880 : 740;
            velocity.add(dir.cpy().mul(accel * dt));

            // Friction
            double fric = 6.0;
            velocity.mul(Math.pow(1.0 - dt, fric));

            // Clamp speed
            velocity.clamp(maxSpd);

            // Position integrate
            position.add(velocity.cpy().mul(dt));

            // Keep inside field
            clampToField();

            // Facing direction
            if (dir.len2() > 0.1) facing = Math.atan2(dir.y, dir.x);
            else if (match.ball != null) {
                // Look a bit towards ball if idle
                Vec2 look = match.ball.position.cpy().sub(position);
                if (look.len2() > 1) facing = Math.atan2(look.y, look.x) * 0.1 + facing * 0.9;
            }

            // Kick control
            boolean kickPressed = keys.isDown(kickKey);
            if (kickPressed) {
                kickCharging = true;
                kickCharge += dt;
                if (kickCharge > maxKickCharge) kickCharge = maxKickCharge;
            } else {
                if (kickCharging) {
                    wantKickRelease = true;
                }
                kickCharging = false;
            }

            // Stamina
            if (sprinting) {
                stamina -= staminaDrain * dt;
                if (stamina < 0) stamina = 0;
            } else {
                stamina += staminaRecover * dt;
                if (stamina > 100) stamina = 100;
            }

            // Visual bob
            if (dir.len2() > 0.001) bobTime += dt * (sprinting ? 12 : 8);
            else bobTime *= Math.pow(0.7, dt);

            // AI bridging clear
            aiKickRelease = false;
        }

        void handleInputForAI(double dt) {
            // Mirror of handleInput but fed by aiMoveDir and ai flags
            Vec2 dir = aiMoveDir.cpy();
            if (dir.len2() > 1) dir.nor();

            sprinting = aiSprint && stamina > 8;
            double maxSpd = sprinting ? sprintSpeed : speed;
            double accel = sprinting ? 880 : 740;

            velocity.add(dir.cpy().mul(accel * dt));
            double fric = 6.0;
            velocity.mul(Math.pow(1.0 - dt, fric));
            velocity.clamp(maxSpd);
            position.add(velocity.cpy().mul(dt));
            clampToField();

            if (dir.len2() > 0.1) facing = Math.atan2(dir.y, dir.x);
            else if (match.ball != null) {
                Vec2 look = match.ball.position.cpy().sub(position);
                if (look.len2() > 1) facing = Math.atan2(look.y, look.x) * 0.2 + facing * 0.8;
            }

            // Kick
            if (aiKickHold) {
                kickCharging = true;
                kickCharge += dt;
                if (kickCharge > maxKickCharge) kickCharge = maxKickCharge;
            } else {
                if (kickCharging) {
                    wantKickRelease = true;
                }
                kickCharging = false;
            }

            if (sprinting) {
                stamina -= staminaDrain * dt;
                if (stamina < 0) stamina = 0;
            } else {
                stamina += staminaRecover * dt;
                if (stamina > 100) stamina = 100;
            }

            if (dir.len2() > 0.001) bobTime += dt * (sprinting ? 12 : 8);
            else bobTime *= Math.pow(0.7, dt);

            aiKickRelease = false;
        }

        private void clampToField() {
            int m = 14;
            if (position.x < match.left + m) position.x = match.left + m;
            if (position.x > match.right - m) position.x = match.right - m;
            if (position.y < match.top + m) position.y = match.top + m;
            if (position.y > match.bottom - m) position.y = match.bottom - m;
        }

        void render(Graphics2D g, double alpha) {
            // Interpolate position for smoothness: using velocity only (no prev state stored)
            Vec2 ipos = position.cpy().add(velocity.cpy().mul(alpha * 1 / 60.0));

            // Shadow
            g.setColor(new Color(0, 0, 0, 80));
            g.fillOval((int) (ipos.x - 16), (int) (ipos.y - 6), 32, 12);

            // Bobbing offset
            double bob = Math.sin(bobTime) * 2.0;

            // Body group transform
            AffineTransform old = g.getTransform();
            g.translate(ipos.x, ipos.y + bob);
            g.rotate(facing);

            // Jersey
            g.setColor(shirtColor);
            g.fillRoundRect(-14, -12, 28, 24, 10, 10);

            // Outline
            g.setColor(new Color(0, 0, 0, 120));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(-14, -12, 28, 24, 10, 10);

            // Head
            g.setColor(new Color(255, 230, 200));
            g.fillOval(-10, -26, 20, 20);
            g.setColor(new Color(0, 0, 0, 150));
            g.drawOval(-10, -26, 20, 20);

            // Arms
            g.setColor(shirtColor.darker());
            g.fillRoundRect(-20, -10, 10, 8, 4, 4);
            g.fillRoundRect(10, -10, 10, 8, 4, 4);

            // Shorts
            g.setColor(darkColor);
            g.fillRoundRect(-14, 2, 28, 12, 6, 6);

            // Legs
            g.setColor(new Color(255, 230, 200));
            g.fillRoundRect(-10, 12, 8, 12, 4, 4);
            g.fillRoundRect(2, 12, 8, 12, 4, 4);

            // Shoes
            g.setColor(darkColor.darker());
            g.fillRoundRect(-12, 22, 10, 6, 3, 3);
            g.fillRoundRect(2, 22, 10, 6, 3, 3);

            // Number dot
            g.setColor(new Color(255, 255, 255, 180));
            g.fillOval(-4, -6, 8, 8);

            // Kick charge indicator (front arc)
            if (kickCharging) {
                double pct = kickCharge / maxKickCharge;
                int arcRadius = 22;
                g.setColor(new Color(255, 255, 255, 160));
                g.setStroke(new BasicStroke(3f));
                g.drawArc(-arcRadius, -arcRadius, arcRadius * 2, arcRadius * 2, 300, (int) (pct * 300));
            }

            // Stamina bar
            g.setTransform(old);
            int bw = 36;
            int bh = 6;
            int bx = (int) ipos.x - bw / 2;
            int by = (int) ipos.y - 36;
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(bx, by, bw, bh, 4, 4);
            int fw = (int) (bw * (stamina / 100.0));
            Color sc = isLeftTeam ? new Color(90, 170, 255) : new Color(255, 120, 120);
            g.setColor(sc);
            g.fillRoundRect(bx, by, fw, bh, 4, 4);
            g.setColor(new Color(255, 255, 255, 80));
            g.drawRoundRect(bx, by, bw, bh, 4, 4);
        }
    }

    // Simple AI for one player
    static class AIPlayer {

        final boolean isLeft;
        final Player controlled;
        final Match match;

        double thinkTimer = 0;
        Vec2 desiredDir = new Vec2();
        boolean wantKick = false;
        boolean wantSprint = false;

        public AIPlayer(boolean isLeft, Player controlled, Match match) {
            this.isLeft = isLeft;
            this.controlled = controlled;
            this.match = match;
        }

        void update(double dt) {
            thinkTimer -= dt;
            if (thinkTimer <= 0) {
                thinkTimer = 0.06 + Math.random() * 0.06;
                think();
            }

            // Feed into player
            controlled.aiMoveDir = desiredDir;
            controlled.aiKickHold = wantKick;
            controlled.aiSprint = wantSprint;

            // AI release logic: randomly release after some charge or when angle aligns
            if (wantKick && controlled.kickCharge > 0.45) {
                // Aim check: if facing near opponent goal direction, release
                double goalDir = Math.atan2(0, isLeft ? -1 : 1);
                double df = angleDiff(controlled.facing, goalDir);
                if (Math.abs(df) < Math.toRadians(35)) {
                    controlled.aiKickHold = false; // release next tick
                    wantKick = false;
                } else if (Math.random() < 0.15) {
                    controlled.aiKickHold = false;
                    wantKick = false;
                }
            }
        }

        private void think() {
            // Basic strategy:
            // - Get to ball
            // - If near ball and on attack side, charge kick toward enemy goal
            // - Sprint when chasing or open field
            Vec2 ballPos = match.ball.position.cpy();
            Vec2 myPos = controlled.position.cpy();
            Vec2 toBall = ballPos.cpy().sub(myPos);

            // If ball near own goal dangerous -> clear
            boolean ballDangerLeft = match.ball.position.x < match.centerX - 60;
            boolean ballDangerRight = match.ball.position.x > match.centerX + 60;

            // Move target
            desiredDir.set(0, 0);

            // If close, approach with more precise steer
            double dist = toBall.len();
            if (dist > 6) desiredDir = toBall.nor();

            // Try to approach slightly from own side to get better shooting angle
            if (dist < 90) {
                double offset = isLeft ? 28 : -28;
                Vec2 approach = ballPos.cpy().add(new Vec2(offset, 0));
                desiredDir = approach.sub(myPos).nor();
            }

            // Kicking decision
            if (dist < 30) {
                // if on attack half
                boolean onAttack = isLeft ? (myPos.x > match.centerX) : (myPos.x < match.centerX);
                boolean danger = isLeft ? ballDangerLeft : ballDangerRight;

                if (onAttack || danger) {
                    wantKick = true; // hold to charge
                } else {
                    // gentle dribble, maybe no kick
                    wantKick = Math.random() < 0.15;
                }
            } else {
                wantKick = false;
            }

            // Sprinting decision
            wantSprint = dist > 80 || (Math.random() < 0.2);
        }

        private double angleDiff(double a, double b) {
            double d = a - b;
            while (d > Math.PI) d -= Math.PI * 2;
            while (d < -Math.PI) d += Math.PI * 2;
            return d;
        }
    }

    // Ball entity
    static class Ball {
        final Vec2 position = new Vec2();
        final Vec2 velocity = new Vec2();
        final double radius = 10;
        final double damping = 0.993;

        public Ball(double x, double y) {
            position.set(x, y);
        }

        void update(double dt) {
            position.add(velocity.cpy().mul(dt));

            // Friction and clamp
            velocity.mul(Math.pow(damping, Math.max(1, dt * 60)));

            // Small cutoff to zero
            if (velocity.len2() < 1.0) velocity.set(0, 0);
        }

        void render(Graphics2D g, double alpha) {
            Vec2 ipos = position.cpy().add(velocity.cpy().mul(alpha * 1 / 60.0));

            // Shadow
            g.setColor(new Color(0, 0, 0, 80));
            g.fillOval((int) (ipos.x - 12), (int) (ipos.y - 6), 24, 12);

            // Ball
            g.setColor(Color.WHITE);
            g.fillOval((int) (ipos.x - radius), (int) (ipos.y - radius), (int) (radius * 2), (int) (radius * 2));
            g.setColor(new Color(0, 0, 0, 140));
            g.drawOval((int) (ipos.x - radius), (int) (ipos.y - radius), (int) (radius * 2), (int) (radius * 2));

            // Panels pattern (simple pentagons hints)
            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(0, 0, 0, 110));
            double ang = Math.atan2(velocity.y, velocity.x);
            for (int i = 0; i < 5; i++) {
                double a = ang + i * (2 * Math.PI / 5);
                double x1 = ipos.x + Math.cos(a) * (radius * 0.6);
                double y1 = ipos.y + Math.sin(a) * (radius * 0.6);
                double x2 = ipos.x + Math.cos(a + 0.3) * (radius * 0.9);
                double y2 = ipos.y + Math.sin(a + 0.3) * (radius * 0.9);
                g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
            }
        }
    }

    // Particle system
    static class Particle {
        Vec2 pos;
        Vec2 vel;
        double life;
        double maxLife;
        Color color;
        double size;
        boolean dead = false;
        int type = 0; // 0 dust, 1 spark, 2 grass

        static Particle dust(Vec2 p, Vec2 v, Color c) {
            Particle pa = new Particle();
            pa.pos = p;
            pa.vel = v;
            pa.life = pa.maxLife = 0.6 + Math.random() * 0.4;
            pa.color = c;
            pa.size = 4 + Math.random() * 2;
            pa.type = 0;
            return pa;
        }

        static Particle spark(Vec2 p, Vec2 v, Color c) {
            Particle pa = new Particle();
            pa.pos = p;
            pa.vel = v;
            pa.life = pa.maxLife = 1.2 + Math.random() * 0.6;
            pa.color = c;
            pa.size = 3 + Math.random() * 2;
            pa.type = 1;
            return pa;
        }

        static Particle grass(Vec2 p, Vec2 v, Color c) {
            Particle pa = new Particle();
            pa.pos = p;
            pa.vel = v;
            pa.life = pa.maxLife = 0.8 + Math.random() * 0.5;
            pa.color = c;
            pa.size = 3 + Math.random() * 2;
            pa.type = 2;
            return pa;
        }

        void update(double dt) {
            if (dead) return;
            life -= dt;
            if (life <= 0) {
                dead = true;
                return;
            }
            // Gravity-ish
            vel.y += (type == 1 ? 60 : 30) * dt;
            // Drag
            vel.mul(0.98);
            pos.add(vel.cpy().mul(dt));
        }

        void render(Graphics2D g) {
            if (dead) return;
            float a = (float) Math.max(0, Math.min(1, life / maxLife));
            int alpha = (int) (a * 220);
            Color draw = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            g.setColor(draw);
            int s = (int) size;
            switch (type) {
                case 0:
                    g.fillOval((int) (pos.x - s / 2), (int) (pos.y - s / 2), s, s);
                    break;
                case 1:
                    g.setStroke(new BasicStroke(2f));
                    g.drawLine((int) pos.x, (int) pos.y, (int) (pos.x - vel.x * 0.03), (int) (pos.y - vel.y * 0.03));
                    break;
                case 2:
                    g.setStroke(new BasicStroke(2f));
                    g.drawLine((int) pos.x, (int) pos.y, (int) (pos.x - vel.x * 0.02), (int) (pos.y - vel.y * 0.02));
                    break;
            }
        }
    }
}