import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class RetroArcadeRacer extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RetroArcadeRacer().setVisible(true));
    }

    static final int IW = 320;   // internal pixel-art resolution
    static final int IH = 220;
    static final int SCALE = 3;  // each internal pixel becomes SCALE x SCALE real pixels

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);
    private GamePanel gamePanel;

    public RetroArcadeRacer() {
        super("RETRO ARCADE RACER");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        MenuPanel menu = new MenuPanel(this::startGame);
        root.add(menu, "menu");
        setContentPane(root);
        pack();
        setLocationRelativeTo(null);
    }

    private void startGame(boolean vsComputer) {
        if (gamePanel != null) {
            root.remove(gamePanel);
            gamePanel.stop();
        }
        gamePanel = new GamePanel(vsComputer, () -> {
            gamePanel.stop();
            cardLayout.show(root, "menu");
        });
        root.add(gamePanel, "game");
        cardLayout.show(root, "game");
        gamePanel.requestFocusInWindow();
        gamePanel.start();
    }

    // ======================================================================
    // Shared pixel-art canvas: draw small, blit big, add CRT scanlines
    // ======================================================================
    abstract static class PixelCanvas extends JPanel {
        final BufferedImage buffer = new BufferedImage(IW, IH, BufferedImage.TYPE_INT_RGB);

        PixelCanvas() {
            setPreferredSize(new Dimension(IW * SCALE, IH * SCALE));
            setFocusable(true);
            setBackground(Color.BLACK);
        }

        abstract void renderPixels(Graphics2D g);

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D bg = buffer.createGraphics();
            bg.setColor(Color.BLACK);
            bg.fillRect(0, 0, IW, IH);
            renderPixels(bg);
            bg.dispose();

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(buffer, 0, 0, IW * SCALE, IH * SCALE, null);

            g2.setColor(new Color(0, 0, 0, 90));
            int lineH = Math.max(1, SCALE / 3);
            for (int y = 0; y < IH * SCALE; y += SCALE) {
                g2.fillRect(0, y, IW * SCALE, lineH);
            }

            Point2D center = new Point2D.Float(IW * SCALE / 2f, IH * SCALE / 2f);
            float radius = IW * SCALE * 0.75f;
            RadialGradientPaint vignette = new RadialGradientPaint(center, radius,
                    new float[]{0.6f, 1f}, new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 140)});
            g2.setPaint(vignette);
            g2.fillRect(0, 0, IW * SCALE, IH * SCALE);
        }
    }

    // ======================================================================
    // MENU  ("INSERT COIN" attract screen)
    // ======================================================================
    static class MenuPanel extends PixelCanvas {
        interface StartListener { void start(boolean vsComputer); }

        private final StartListener listener;
        private double t = 0;
        private final Timer animTimer;

        final Rectangle btn2P = new Rectangle(70, 150, 180, 20);
        final Rectangle btnCPU = new Rectangle(70, 178, 180, 20);

        MenuPanel(StartListener listener) {
            this.listener = listener;
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int px = e.getX() / SCALE, py = e.getY() / SCALE;
                    if (btn2P.contains(px, py)) listener.start(false);
                    else if (btnCPU.contains(px, py)) listener.start(true);
                }
            });
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_1) listener.start(false);
                    if (e.getKeyCode() == KeyEvent.VK_2) listener.start(true);
                }
            });
            animTimer = new Timer(33, e -> {
                t += 0.033;
                repaint();
            });
            animTimer.start();
        }

        @Override
        void renderPixels(Graphics2D g) {
            g.setColor(new Color(10, 4, 30));
            g.fillRect(0, 0, IW, IH);

            int horizon = 120;
            g.setPaint(new GradientPaint(0, 0, new Color(40, 10, 60), 0, horizon, new Color(255, 60, 140)));
            g.fillRect(0, 0, IW, horizon);

            g.setColor(new Color(255, 210, 60));
            g.fillOval(IW / 2 - 26, horizon - 46, 52, 52);
            g.setColor(new Color(10, 4, 30));
            for (int i = 0; i < 5; i++) {
                g.fillRect(IW / 2 - 26, horizon - 46 + 6 + i * 8, 52, 3);
            }

            g.setColor(new Color(255, 70, 200));
            for (int i = -8; i <= 8; i++) {
                int topX = IW / 2 + i * 6;
                int botX = IW / 2 + i * 34;
                g.drawLine(topX, horizon, botX, IH);
            }
            double scroll = (t * 40) % 16;
            for (int j = 0; j < 12; j++) {
                double u = (j * 16 + scroll) / (12.0 * 16);
                int y = (int) (horizon + u * (IH - horizon));
                if (y >= IH) continue;
                g.drawLine(0, y, IW, y);
            }

            drawPixelText(g, "RETRO ARCADE", IW / 2 - 60, 30, new Color(0, 255, 220), 2);
            drawPixelText(g, "RACER", IW / 2 - 24, 46, new Color(255, 255, 60), 2);

            boolean blink = ((int) (t * 2)) % 2 == 0;
            if (blink) {
                drawPixelText(g, "INSERT COIN", IW / 2 - 44, 130, Color.WHITE, 1);
            }

            drawButton(g, btn2P, "2 PLAYERS [1]", new Color(60, 220, 100));
            drawButton(g, btnCPU, "VS COMPUTER [2]", new Color(255, 120, 60));

            drawPixelText(g, "P1: A D W S", 90, 202, new Color(140, 220, 255), 1);
            drawPixelText(g, "P2: ARROWS", 210, 202, new Color(255, 170, 220), 1);
        }

        private void drawButton(Graphics2D g, Rectangle r, String label, Color color) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(color);
            g.drawRect(r.x, r.y, r.width, r.height);
            g.drawRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4);
            drawPixelText(g, label, r.x + 8, r.y + 6, color, 1);
        }
    }

    static void drawPixelText(Graphics2D g, String text, int x, int y, Color color, int scale) {
        g.setColor(color);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 8 * scale));
        g.drawString(text, x, y + 8 * scale);
    }

    // ======================================================================
    // GAME
    // ======================================================================
    static class GamePanel extends PixelCanvas implements ActionListener {

        enum State { COUNTDOWN, RACING, FINISHED }

        static final int HALF_W = IW / 2;
        static final int LANE_COUNT = 3;
        static final int ROAD_WIDTH = 84;
        static final int LANE_WIDTH = ROAD_WIDTH / LANE_COUNT;
        static final double FINISH_DISTANCE = 4200;
        static final double MAX_SPEED = 95;
        static final double ACCEL = 150;
        static final double BRAKE_DECEL = 220;
        static final double FRICTION = 70;
        static final double CRASH_PENALTY = 0.4;

        static final String[] CAR_SPRITE = {
                ".#####.",
                "#######",
                "#.###.#",
                "#.###.#",
                "#wwwww#",
                "#wwwww#",
                "#.###.#",
                "#.###.#",
                "#######",
                ".#####.",
        };
        static final int SPRITE_PIXEL = 2;

        private final boolean vsComputer;
        private final Runnable onExit;
        private final Timer timer;
        private long lastNanos;

        private final Set<Integer> pressed = new HashSet<>();
        private final Random random = new Random();

        private final Racer p1;
        private final Racer p2;

        private State state = State.COUNTDOWN;
        private double countdownRemaining = 3.999;
        private double goFlashRemaining = 0;
        private Racer winner = null;
        private final List<Particle> confetti = new ArrayList<>();

        GamePanel(boolean vsComputer, Runnable onExit) {
            this.vsComputer = vsComputer;
            this.onExit = onExit;

            p1 = new Racer("P1", new Color(60, 230, 100));
            p2 = new Racer(vsComputer ? "CPU" : "P2", new Color(255, 90, 200));

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    pressed.add(e.getKeyCode());
                    if (state == State.RACING) {
                        if (e.getKeyCode() == KeyEvent.VK_A) p1.changeLane(-1);
                        if (e.getKeyCode() == KeyEvent.VK_D) p1.changeLane(1);
                        if (!vsComputer) {
                            if (e.getKeyCode() == KeyEvent.VK_LEFT) p2.changeLane(-1);
                            if (e.getKeyCode() == KeyEvent.VK_RIGHT) p2.changeLane(1);
                        }
                    }
                    if (state == State.FINISHED && e.getKeyCode() == KeyEvent.VK_ENTER) restart();
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) onExit.run();
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    pressed.remove(e.getKeyCode());
                }
            });

            timer = new Timer(16, this);
        }

        void start() {
            lastNanos = System.nanoTime();
            timer.start();
        }

        void stop() {
            timer.stop();
        }

        private void restart() {
            p1.reset();
            p2.reset();
            confetti.clear();
            winner = null;
            state = State.COUNTDOWN;
            countdownRemaining = 3.999;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            double dt = Math.min((now - lastNanos) / 1_000_000_000.0, 0.05);
            lastNanos = now;
            update(dt);
            repaint();
        }

        private void update(double dt) {
            if (state == State.COUNTDOWN) {
                countdownRemaining -= dt;
                if (countdownRemaining <= 0) {
                    state = State.RACING;
                    goFlashRemaining = 0.7;
                }
                return;
            }
            if (goFlashRemaining > 0) goFlashRemaining -= dt;

            if (state == State.RACING) {
                boolean p1Accel = pressed.contains(KeyEvent.VK_W);
                boolean p1Brake = pressed.contains(KeyEvent.VK_S);
                p1.applyInput(p1Accel, p1Brake, dt);
                p1.update(dt, random);

                if (vsComputer) {
                    p2.driveAI(dt, random);
                } else {
                    boolean p2Accel = pressed.contains(KeyEvent.VK_UP);
                    boolean p2Brake = pressed.contains(KeyEvent.VK_DOWN);
                    p2.applyInput(p2Accel, p2Brake, dt);
                }
                p2.update(dt, random);

                if (p1.distance >= FINISH_DISTANCE || p2.distance >= FINISH_DISTANCE) {
                    winner = p1.distance >= p2.distance ? p1 : p2;
                    state = State.FINISHED;
                    spawnConfetti();
                }
            } else if (state == State.FINISHED) {
                for (Particle p : confetti) p.update(dt);
                confetti.removeIf(p -> p.life <= 0);
                if (confetti.isEmpty() && random.nextDouble() < 0.03) spawnConfetti();
            }
        }

        private void spawnConfetti() {
            Color[] palette = {
                    new Color(60, 230, 100), new Color(255, 90, 200), new Color(255, 255, 60),
                    new Color(90, 200, 255)
            };
            for (int i = 0; i < 60; i++) {
                double x = random.nextDouble() * IW;
                double y = -random.nextDouble() * 60;
                double vx = (random.nextDouble() - 0.5) * 40;
                double vy = 30 + random.nextDouble() * 50;
                confetti.add(new Particle(x, y, vx, vy, palette[random.nextInt(palette.length)]));
            }
        }

        @Override
        void renderPixels(Graphics2D g) {
            drawRoad(g, 0, p1);
            drawRoad(g, HALF_W, p2);

            g.setColor(new Color(10, 10, 14));
            g.fillRect(HALF_W - 1, 0, 2, IH);

            if (state == State.COUNTDOWN) drawCountdown(g);
            if (goFlashRemaining > 0) drawGoFlash(g);
            if (state == State.FINISHED) drawFinishOverlay(g);
        }

        private void drawRoad(Graphics2D g, int offsetX, Racer racer) {
            int roadX = offsetX + (HALF_W - ROAD_WIDTH) / 2;

            g.setColor(new Color(20, 60, 30));
            g.fillRect(offsetX, 0, HALF_W, IH);
            g.setColor(new Color(60, 60, 66));
            g.fillRect(roadX, 0, ROAD_WIDTH, IH);
            g.setColor(new Color(255, 210, 40));
            g.fillRect(roadX - 3, 0, 3, IH);
            g.fillRect(roadX + ROAD_WIDTH, 0, 3, IH);

            g.setColor(new Color(230, 230, 230));
            double dashOffset = racer.roadScroll % 20;
            for (int lane = 1; lane < LANE_COUNT; lane++) {
                int lx = roadX + lane * LANE_WIDTH;
                for (double y = -20 + dashOffset; y < IH + 20; y += 20) {
                    g.fillRect(lx - 1, (int) y, 2, 10);
                }
            }

            for (Obstacle o : racer.obstacles) {
                drawSprite(g, roadX + o.lane * LANE_WIDTH + LANE_WIDTH / 2, (int) o.y, o.color);
            }

            drawSprite(g, roadX + racer.laneX() + LANE_WIDTH / 2, racer.carY, racer.crashFlash > 0 ? Color.RED : racer.color);

            drawHud(g, offsetX, racer);
        }

        private void drawSprite(Graphics2D g, int cx, int cy, Color body) {
            int w = CAR_SPRITE[0].length() * SPRITE_PIXEL;
            int h = CAR_SPRITE.length * SPRITE_PIXEL;
            int startX = cx - w / 2;
            int startY = cy - h / 2;
            for (int row = 0; row < CAR_SPRITE.length; row++) {
                String line = CAR_SPRITE[row];
                for (int col = 0; col < line.length(); col++) {
                    char c = line.charAt(col);
                    if (c == '.') continue;
                    g.setColor(c == 'w' ? new Color(20, 30, 50) : body);
                    g.fillRect(startX + col * SPRITE_PIXEL, startY + row * SPRITE_PIXEL, SPRITE_PIXEL, SPRITE_PIXEL);
                }
            }
        }

        private void drawHud(Graphics2D g, int offsetX, Racer racer) {
            drawPixelText(g, racer.name, offsetX + 6, 4, Color.WHITE, 1);

            int barW = HALF_W - 12, barH = 3;
            g.setColor(new Color(255, 255, 255, 70));
            g.fillRect(offsetX + 6, 16, barW, barH);
            int fillW = (int) (barW * Math.min(1.0, racer.distance / FINISH_DISTANCE));
            g.setColor(racer.color);
            g.fillRect(offsetX + 6, 16, Math.max(0, fillW), barH);

            int speedKmh = (int) (racer.speed * 1.2);
            drawSevenSegNumber(g, offsetX + 6, IH - 20, speedKmh, 3, new Color(255, 60, 60));
        }

        private void drawCountdown(Graphics2D g) {
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRect(0, 0, IW, IH);
            int value = (int) Math.ceil(countdownRemaining);
            String text = value > 0 ? String.valueOf(value) : "GO";
            drawPixelText(g, text, IW / 2 - 10, IH / 2 - 10, new Color(255, 220, 40), 3);
        }

        private void drawGoFlash(Graphics2D g) {
            float alpha = (float) Math.max(0, Math.min(1, goFlashRemaining / 0.7));
            drawPixelText(g, "GO!", IW / 2 - 20, IH / 2 - 10, new Color(60, 255, 120, (int) (alpha * 255)), 3);
        }

        private void drawFinishOverlay(Graphics2D g) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, IW, IH);
            for (Particle p : confetti) p.draw(g);
            drawPixelText(g, winner.name + " WINS", IW / 2 - 46, IH / 2 - 20, new Color(255, 220, 40), 2);
            drawPixelText(g, "ENTER=RETRY  ESC=MENU", IW / 2 - 78, IH / 2 + 6, Color.WHITE, 1);
        }

        // -------- 7-segment LED style numeric readout --------
        private static final boolean[][] SEGMENTS = {
                {true, true, true, true, true, true, false},   // 0: a b c d e f
                {false, true, true, false, false, false, false}, // 1: b c
                {true, true, false, true, true, false, true},   // 2: a b g e d
                {true, true, true, true, false, false, true},   // 3: a b c d g
                {false, true, true, false, false, true, true},  // 4: b c f g
                {true, false, true, true, false, true, true},   // 5: a c d f g
                {true, false, true, true, true, true, true},    // 6: a c d e f g
                {true, true, true, false, false, false, false}, // 7: a b c
                {true, true, true, true, true, true, true},     // 8: all
                {true, true, true, true, false, true, true},    // 9: a b c d f g
        };

        private void drawSevenSegNumber(Graphics2D g, int x, int y, int number, int px, Color color) {
            String s = String.valueOf(Math.max(0, Math.min(999, number)));
            int digitW = 5 * px + px;
            for (int i = 0; i < s.length(); i++) {
                drawSevenSegDigit(g, x + i * digitW, y, s.charAt(i) - '0', px, color);
            }
        }

        private void drawSevenSegDigit(Graphics2D g, int x, int y, int digit, int px, Color color) {
            boolean[] seg = SEGMENTS[digit];
            g.setColor(color);
            // a: top
            if (seg[0]) g.fillRect(x + px, y, 3 * px, px);
            // b: top-right
            if (seg[1]) g.fillRect(x + 4 * px, y + px, px, 4 * px);
            // c: bottom-right
            if (seg[2]) g.fillRect(x + 4 * px, y + 6 * px, px, 4 * px);
            // d: bottom
            if (seg[3]) g.fillRect(x + px, y + 10 * px, 3 * px, px);
            // e: bottom-left
            if (seg[4]) g.fillRect(x, y + 6 * px, px, 4 * px);
            // f: top-left
            if (seg[5]) g.fillRect(x, y + px, px, 4 * px);
            // g: middle
            if (seg[6]) g.fillRect(x + px, y + 5 * px, 3 * px, px);
        }
    }

    // ======================================================================
    // RACER
    // ======================================================================
    static class Racer {
        final String name;
        final Color color;

        int lane = GamePanel.LANE_COUNT / 2;
        double laneVisual = lane;
        double speed = 0;
        double distance = 0;
        double roadScroll = 0;
        double crashFlash = 0;
        double spawnTimer;
        int carY = 190;

        final List<Obstacle> obstacles = new ArrayList<>();

        Racer(String name, Color color) {
            this.name = name;
            this.color = color;
            spawnTimer = 0.6 + Math.random();
        }

        void reset() {
            lane = GamePanel.LANE_COUNT / 2;
            laneVisual = lane;
            speed = 0;
            distance = 0;
            roadScroll = 0;
            crashFlash = 0;
            obstacles.clear();
        }

        void changeLane(int delta) {
            int nl = lane + delta;
            if (nl >= 0 && nl < GamePanel.LANE_COUNT) lane = nl;
        }

        int laneX() {
            return (int) (laneVisual * GamePanel.LANE_WIDTH);
        }

        void applyInput(boolean accel, boolean brake, double dt) {
            if (accel) speed += GamePanel.ACCEL * dt;
            else if (brake) speed -= GamePanel.BRAKE_DECEL * dt;
            else speed -= GamePanel.FRICTION * dt;
            speed = Math.max(0, Math.min(GamePanel.MAX_SPEED, speed));
        }

        void driveAI(double dt, Random random) {
            Obstacle threat = null;
            for (Obstacle o : obstacles) {
                if (o.lane == lane && o.y < carY && o.y > carY - 90) {
                    if (threat == null || o.y > threat.y) threat = o;
                }
            }
            if (threat != null) {
                for (int candidate = 0; candidate < GamePanel.LANE_COUNT; candidate++) {
                    if (candidate == lane) continue;
                    boolean clear = true;
                    for (Obstacle o : obstacles) {
                        if (o.lane == candidate && o.y > carY - 100) { clear = false; break; }
                    }
                    if (clear) {
                        changeLane(Integer.signum(candidate - lane));
                        break;
                    }
                }
            }
            boolean shouldBrake = threat != null && (threat.y - carY) > -30 && (threat.y - carY) < 20;
            applyInput(!shouldBrake, shouldBrake, dt);
        }

        void update(double dt, Random random) {
            double delta = lane - laneVisual;
            laneVisual += delta * Math.min(1, dt * 8);

            distance += speed * dt;
            roadScroll += speed * dt;

            spawnTimer -= dt;
            if (spawnTimer <= 0) {
                obstacles.add(new Obstacle(random.nextInt(GamePanel.LANE_COUNT), -20, randomColor(random)));
                spawnTimer = 0.7 + random.nextDouble() * 0.9;
            }

            double relSpeed = speed * 0.55 + 60;
            for (Obstacle o : obstacles) o.y += relSpeed * dt;
            obstacles.removeIf(o -> o.y > IH + 20);

            for (Obstacle o : obstacles) {
                if (!o.hit && o.lane == lane && Math.abs(o.y - carY) < 14) {
                    o.hit = true;
                    speed *= GamePanel.CRASH_PENALTY;
                    crashFlash = 0.25;
                }
            }
            crashFlash = Math.max(0, crashFlash - dt);
        }

        private static Color randomColor(Random random) {
            Color[] palette = {
                    new Color(255, 230, 60), new Color(90, 200, 255), new Color(230, 60, 60), new Color(255, 255, 255)
            };
            return palette[random.nextInt(palette.length)];
        }
    }

    static class Obstacle {
        final int lane;
        double y;
        final Color color;
        boolean hit = false;

        Obstacle(int lane, double y, Color color) {
            this.lane = lane;
            this.y = y;
            this.color = color;
        }
    }

    static class Particle {
        double x, y, vx, vy, life = 2.5;
        Color color;

        Particle(double x, double y, double vx, double vy, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
        }

        void update(double dt) {
            x += vx * dt;
            y += vy * dt;
            vy += 30 * dt;
            life -= dt;
        }

        void draw(Graphics2D g) {
            g.setColor(color);
            g.fillRect((int) x, (int) y, 3, 3);
        }
    }
}