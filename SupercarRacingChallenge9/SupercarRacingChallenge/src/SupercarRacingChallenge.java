import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class SupercarRacingChallenge extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SupercarRacingChallenge().setVisible(true));
    }

    static final int WIDTH = 1000;
    static final int HEIGHT = 720;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);
    private GamePanel gamePanel;

    public SupercarRacingChallenge() {
        super("Supercar Racing Challenge");
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
    // MENU
    // ======================================================================
    static class MenuPanel extends JPanel {
        interface StartListener { void start(boolean vsComputer); }

        private float hue = 0.58f;
        private final Timer animTimer;

        MenuPanel(StartListener listener) {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setLayout(null);
            setBackground(Color.BLACK);

            JLabel title = new JLabel("SUPERCAR RACING CHALLENGE", SwingConstants.CENTER);
            title.setFont(new Font("Arial Black", Font.BOLD, 42));
            title.setForeground(Color.WHITE);
            title.setBounds(0, 160, WIDTH, 60);
            add(title);

            JLabel subtitle = new JLabel("Choose your race mode", SwingConstants.CENTER);
            subtitle.setFont(new Font("Arial", Font.PLAIN, 20));
            subtitle.setForeground(new Color(200, 200, 220));
            subtitle.setBounds(0, 225, WIDTH, 30);
            add(subtitle);

            JButton twoPlayers = styledButton("2 PLAYERS", new Color(255, 140, 0));
            twoPlayers.setBounds(WIDTH / 2 - 220, 340, 440, 70);
            twoPlayers.addActionListener(e -> listener.start(false));
            add(twoPlayers);

            JButton vsComputer = styledButton("VS COMPUTER", new Color(0, 170, 255));
            vsComputer.setBounds(WIDTH / 2 - 220, 440, 440, 70);
            vsComputer.addActionListener(e -> listener.start(true));
            add(vsComputer);

            JLabel controls = new JLabel(
                    "<html><center>Player 1: A / D to change lane, W accelerate, S brake<br>"
                            + "Player 2 (2P mode): &larr; / &rarr; to change lane, &uarr; accelerate, &darr; brake</center></html>",
                    SwingConstants.CENTER);
            controls.setFont(new Font("Arial", Font.PLAIN, 14));
            controls.setForeground(new Color(160, 160, 180));
            controls.setBounds(0, 560, WIDTH, 60);
            add(controls);

            animTimer = new Timer(30, e -> {
                hue += 0.0015f;
                if (hue > 1f) hue -= 1f;
                repaint();
            });
            animTimer.start();
        }

        private JButton styledButton(String text, Color color) {
            JButton b = new JButton(text);
            b.setFont(new Font("Arial", Font.BOLD, 26));
            b.setFocusPainted(false);
            b.setBackground(color);
            b.setForeground(Color.WHITE);
            b.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return b;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            Color c1 = Color.getHSBColor(hue, 0.65f, 0.22f);
            Color c2 = Color.getHSBColor((hue + 0.5f) % 1f, 0.65f, 0.05f);
            g2.setPaint(new GradientPaint(0, 0, c1, 0, h, c2));
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(255, 255, 255, 18));
            for (int i = 0; i < 26; i++) {
                double t = (i * 53 + hue * 3000) % (h + 200) - 100;
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(0, (int) t, w, (int) (t - 60));
            }
        }
    }

    // ======================================================================
    // GAME
    // ======================================================================
    static class GamePanel extends JPanel implements ActionListener {

        enum State { COUNTDOWN, RACING, FINISHED }

        static final int LANE_COUNT = 3;
        static final int ROAD_WIDTH = 300;
        static final int HALF_WIDTH = WIDTH / 2;
        static final int LANE_WIDTH = ROAD_WIDTH / LANE_COUNT;
        static final int CAR_WIDTH = 46;
        static final int CAR_HEIGHT = 84;
        static final double FINISH_DISTANCE = 5200;
        static final double MAX_SPEED = 260;      // units / second
        static final double ACCELERATION = 140;
        static final double BRAKE_DECEL = 220;
        static final double FRICTION = 60;
        static final double CRASH_PENALTY = 0.45; // speed multiplier on collision

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
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            setBackground(Color.BLACK);

            p1 = new Racer("PLAYER 1", new Color(255, 90, 40), new Color(255, 170, 60), false, random);
            p2 = new Racer(vsComputer ? "COMPUTER" : "PLAYER 2",
                    new Color(40, 150, 255), new Color(120, 210, 255), vsComputer, random);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    pressed.add(e.getKeyCode());
                    handleLaneChange(e.getKeyCode());
                    if (state == State.FINISHED && e.getKeyCode() == KeyEvent.VK_ENTER) {
                        restart();
                    }
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        onExit.run();
                    }
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

        private void handleLaneChange(int keyCode) {
            if (state != State.RACING) return;
            if (keyCode == KeyEvent.VK_A) p1.changeLane(-1);
            if (keyCode == KeyEvent.VK_D) p1.changeLane(1);
            if (!vsComputer) {
                if (keyCode == KeyEvent.VK_LEFT) p2.changeLane(-1);
                if (keyCode == KeyEvent.VK_RIGHT) p2.changeLane(1);
            }
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
                    goFlashRemaining = 0.8;
                }
                p1.updateIdle(dt, random);
                p2.updateIdle(dt, random);
                return;
            }

            if (goFlashRemaining > 0) goFlashRemaining -= dt;

            if (state == State.RACING) {
                boolean p1Accel = pressed.contains(KeyEvent.VK_W);
                boolean p1Brake = pressed.contains(KeyEvent.VK_S);
                p1.applyInput(p1Accel, p1Brake, dt);

                if (vsComputer) {
                    p2.driveAI(dt, random);
                } else {
                    boolean p2Accel = pressed.contains(KeyEvent.VK_UP);
                    boolean p2Brake = pressed.contains(KeyEvent.VK_DOWN);
                    p2.applyInput(p2Accel, p2Brake, dt);
                }

                p1.update(dt, random);
                p2.update(dt, random);

                if (p1.distance >= FINISH_DISTANCE || p2.distance >= FINISH_DISTANCE) {
                    winner = p1.distance >= p2.distance ? p1 : p2;
                    state = State.FINISHED;
                    spawnConfetti();
                }
            } else if (state == State.FINISHED) {
                for (Particle particle : confetti) particle.update(dt);
                confetti.removeIf(particle -> particle.life <= 0);
                if (confetti.isEmpty() && random.nextDouble() < 0.02) spawnConfetti();
                p1.updateVisualsOnly(dt);
                p2.updateVisualsOnly(dt);
            }
        }

        private void spawnConfetti() {
            Color[] palette = {
                    new Color(255, 90, 40), new Color(40, 150, 255), new Color(255, 220, 0),
                    new Color(80, 220, 100), new Color(230, 90, 230)
            };
            for (int i = 0; i < 140; i++) {
                double x = random.nextDouble() * WIDTH;
                double y = -random.nextDouble() * 200;
                double vx = (random.nextDouble() - 0.5) * 80;
                double vy = 80 + random.nextDouble() * 120;
                Color c = palette[random.nextInt(palette.length)];
                confetti.add(new Particle(x, y, vx, vy, c, 3 + random.nextDouble() * 4, 4.0));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawRoad(g2, 0, p1);
            drawRoad(g2, HALF_WIDTH, p2);
            drawDivider(g2);

            if (state == State.COUNTDOWN) drawCountdown(g2);
            if (goFlashRemaining > 0) drawGoFlash(g2);
            if (state == State.FINISHED) drawFinishOverlay(g2);
        }

        private void drawDivider(Graphics2D g2) {
            g2.setColor(new Color(20, 20, 25));
            g2.fillRect(HALF_WIDTH - 4, 0, 8, HEIGHT);
            g2.setColor(new Color(255, 255, 255, 60));
            for (int y = 0; y < HEIGHT; y += 24) g2.fillRect(HALF_WIDTH - 2, y, 4, 12);

            g2.setFont(new Font("Arial Black", Font.BOLD, 22));
            g2.setColor(new Color(255, 255, 255, 200));
            FontMetrics fm = g2.getFontMetrics();
            String vs = "VS";
            g2.drawString(vs, HALF_WIDTH - fm.stringWidth(vs) / 2, 30);
        }

        private void drawRoad(Graphics2D g2, int offsetX, Racer racer) {
            int roadX = offsetX + (HALF_WIDTH - ROAD_WIDTH) / 2;

            g2.setPaint(new GradientPaint(0, 0, new Color(10, 12, 30), 0, HEIGHT, new Color(30, 34, 55)));
            g2.fillRect(offsetX, 0, HALF_WIDTH, HEIGHT);

            g2.setColor(new Color(55, 55, 60));
            g2.fillRect(roadX, 0, ROAD_WIDTH, HEIGHT);

            g2.setColor(new Color(255, 200, 0));
            g2.fillRect(roadX - 10, 0, 6, HEIGHT);
            g2.fillRect(roadX + ROAD_WIDTH + 4, 0, 6, HEIGHT);

            g2.setColor(new Color(230, 230, 230, 200));
            g2.setStroke(new BasicStroke(4));
            double dashOffset = racer.roadScroll % 60;
            for (int lane = 1; lane < LANE_COUNT; lane++) {
                int lx = roadX + lane * LANE_WIDTH;
                for (double y = -60 + dashOffset; y < HEIGHT + 60; y += 60) {
                    g2.drawLine(lx, (int) y, lx, (int) (y + 30));
                }
            }

            for (Obstacle o : racer.obstacles) {
                drawTraffic(g2, roadX + o.lane * LANE_WIDTH + LANE_WIDTH / 2, (int) o.y, o.color);
            }

            for (Particle p : racer.exhaust) p.draw(g2);

            drawCar(g2, roadX + racer.laneX() + LANE_WIDTH / 2, (int) racer.carY, racer.tilt,
                    racer.color, racer.colorLight, racer.crashFlash > 0);

            drawHud(g2, offsetX, racer);
        }

        private void drawTraffic(Graphics2D g2, int cx, int cy, Color color) {
            int w = CAR_WIDTH - 6, h = CAR_HEIGHT - 10;
            RoundRectangle2D body = new RoundRectangle2D.Double(cx - w / 2.0, cy - h / 2.0, w, h, 16, 16);
            g2.setPaint(new GradientPaint(cx - w / 2f, 0, color.darker(), cx + w / 2f, 0, color));
            g2.fill(body);
            g2.setColor(new Color(20, 20, 20));
            g2.fillRoundRect(cx - w / 2 + 4, cy - h / 2 + 10, w - 8, 18, 6, 6);
            g2.setColor(new Color(255, 60, 60, 220));
            g2.fillOval(cx - w / 2 + 2, cy + h / 2 - 8, 6, 5);
            g2.fillOval(cx + w / 2 - 8, cy + h / 2 - 8, 6, 5);
        }

        private void drawCar(Graphics2D g2, int cx, int cy, double tilt, Color color, Color light, boolean crashed) {
            AffineTransform old = g2.getTransform();
            g2.translate(cx, cy);
            g2.rotate(Math.toRadians(tilt));

            int w = CAR_WIDTH, h = CAR_HEIGHT;

            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval(-w / 2 + 4, h / 2 - 10, w - 8, 16);

            Path2D body = new Path2D.Double();
            body.moveTo(-w / 2.0, h * 0.35);
            body.curveTo(-w / 2.0, -h * 0.1, -w * 0.32, -h * 0.5, 0, -h / 2.0);
            body.curveTo(w * 0.32, -h * 0.5, w / 2.0, -h * 0.1, w / 2.0, h * 0.35);
            body.curveTo(w / 2.0, h * 0.5, -w / 2.0, h * 0.5, -w / 2.0, h * 0.35);
            body.closePath();

            Color mainColor = crashed ? new Color(255, 60, 60) : color;
            g2.setPaint(new GradientPaint(-w / 2f, 0, mainColor.darker(), w / 2f, 0, light));
            g2.fill(body);
            g2.setColor(new Color(0, 0, 0, 90));
            g2.setStroke(new BasicStroke(2));
            g2.draw(body);

            g2.setColor(new Color(30, 40, 60, 230));
            g2.fill(new RoundRectangle2D.Double(-w * 0.28, -h * 0.32, w * 0.56, h * 0.32, 10, 10));
            g2.setColor(new Color(255, 255, 255, 90));
            g2.fillOval((int) (-w * 0.2), (int) (-h * 0.30), (int) (w * 0.18), (int) (h * 0.12));

            g2.setColor(new Color(255, 235, 150));
            g2.fillOval(-w / 2, (int) (-h * 0.42), 8, 6);
            g2.fillOval(w / 2 - 8, (int) (-h * 0.42), 8, 6);

            g2.setColor(new Color(255, 40, 40));
            g2.fillRect(-w / 2 + 2, (int) (h * 0.42), 8, 4);
            g2.fillRect(w / 2 - 10, (int) (h * 0.42), 8, 4);

            g2.setColor(new Color(15, 15, 15));
            g2.fillRoundRect((int) (-w * 0.55), (int) (h * 0.40), (int) (w * 1.1), 8, 4, 4);

            g2.setTransform(old);
        }

        private void drawHud(Graphics2D g2, int offsetX, Racer racer) {
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.setColor(Color.WHITE);
            g2.drawString(racer.name, offsetX + 16, 26);

            int barX = offsetX + 16, barY = 34, barW = HALF_WIDTH - 32, barH = 10;
            g2.setColor(new Color(255, 255, 255, 60));
            g2.fillRoundRect(barX, barY, barW, barH, 8, 8);
            int fillW = (int) (barW * Math.min(1.0, racer.distance / FINISH_DISTANCE));
            g2.setPaint(new GradientPaint(barX, 0, racer.color, barX + barW, 0, racer.colorLight));
            g2.fillRoundRect(barX, barY, Math.max(0, fillW), barH, 8, 8);

            int speedKmh = (int) (racer.speed * 1.3);
            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            g2.setColor(new Color(230, 230, 230));
            g2.drawString(speedKmh + " km/h", offsetX + 16, HEIGHT - 20);
            drawSpeedGauge(g2, offsetX + HALF_WIDTH - 70, HEIGHT - 70, 46, racer.speed, MAX_SPEED);
        }

        private void drawSpeedGauge(Graphics2D g2, int cx, int cy, int radius, double speed, double max) {
            g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 255, 255, 40));
            g2.drawArc(cx - radius, cy - radius, radius * 2, radius * 2, -30, -240);

            double ratio = Math.max(0, Math.min(1, speed / max));
            g2.setColor(new Color(255, (int) (200 - 120 * ratio), 40));
            g2.drawArc(cx - radius, cy - radius, radius * 2, radius * 2, -30, (int) (-240 * ratio));
        }

        private void drawCountdown(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            int value = (int) Math.ceil(countdownRemaining);
            String text = value > 0 ? String.valueOf(value) : "GO!";
            float scale = 1f + 0.4f * (float) (countdownRemaining - Math.floor(countdownRemaining));

            g2.setFont(new Font("Arial Black", Font.BOLD, (int) (110 * scale)));
            FontMetrics fm = g2.getFontMetrics();
            int tx = WIDTH / 2 - fm.stringWidth(text) / 2;
            int ty = HEIGHT / 2 + fm.getAscent() / 3;

            g2.setColor(new Color(255, 210, 60));
            g2.drawString(text, tx, ty);
        }

        private void drawGoFlash(Graphics2D g2) {
            float alpha = (float) Math.max(0, Math.min(1, goFlashRemaining / 0.8));
            g2.setFont(new Font("Arial Black", Font.BOLD, 90));
            g2.setColor(new Color(80, 255, 120, (int) (alpha * 255)));
            FontMetrics fm = g2.getFontMetrics();
            String text = "GO!";
            g2.drawString(text, WIDTH / 2 - fm.stringWidth(text) / 2, HEIGHT / 2);
        }

        private void drawFinishOverlay(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            for (Particle p : confetti) p.draw(g2);

            String text = winner.name + " WINS!";
            g2.setFont(new Font("Arial Black", Font.BOLD, 56));
            FontMetrics fm = g2.getFontMetrics();
            int tx = WIDTH / 2 - fm.stringWidth(text) / 2;
            g2.setColor(new Color(255, 215, 60));
            g2.drawString(text, tx, HEIGHT / 2 - 30);

            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            String hint = "Press ENTER to race again, ESC for menu";
            fm = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(hint, WIDTH / 2 - fm.stringWidth(hint) / 2, HEIGHT / 2 + 30);
        }
    }

    // ======================================================================
    // RACER (car + lane logic + obstacles + particles)
    // ======================================================================
    static class Racer {
        final String name;
        final Color color;
        final Color colorLight;
        final boolean ai;

        int lane = GamePanel.LANE_COUNT / 2;
        double laneVisual = lane;
        double tilt = 0;
        double speed = 0;
        double distance = 0;
        double roadScroll = 0;
        double carY = GamePanel.HEIGHT - 150;
        double crashFlash = 0;
        double spawnTimer = 0;
        double aiDecisionCooldown = 0;

        final List<Obstacle> obstacles = new ArrayList<>();
        final List<Particle> exhaust = new ArrayList<>();

        Racer(String name, Color color, Color colorLight, boolean ai, Random random) {
            this.name = name;
            this.color = color;
            this.colorLight = colorLight;
            this.ai = ai;
            spawnTimer = 0.5 + random.nextDouble();
        }

        void reset() {
            lane = GamePanel.LANE_COUNT / 2;
            laneVisual = lane;
            tilt = 0;
            speed = 0;
            distance = 0;
            roadScroll = 0;
            crashFlash = 0;
            obstacles.clear();
            exhaust.clear();
        }

        void changeLane(int delta) {
            int newLane = lane + delta;
            if (newLane >= 0 && newLane < GamePanel.LANE_COUNT) {
                lane = newLane;
            }
        }

        int laneX() {
            return (int) (laneVisual * GamePanel.LANE_WIDTH);
        }

        void applyInput(boolean accel, boolean brake, double dt) {
            if (accel) speed += GamePanel.ACCELERATION * dt;
            else if (brake) speed -= GamePanel.BRAKE_DECEL * dt;
            else speed -= GamePanel.FRICTION * dt;
            speed = Math.max(0, Math.min(GamePanel.MAX_SPEED, speed));
        }

        void driveAI(double dt, Random random) {
            aiDecisionCooldown -= dt;
            Obstacle threat = null;
            for (Obstacle o : obstacles) {
                if (o.lane == lane && o.y < carY && o.y > carY - 260) {
                    if (threat == null || o.y > threat.y) threat = o;
                }
            }

            if (threat != null && aiDecisionCooldown <= 0) {
                int[] preferred = {0, 1, 2};
                for (int candidate : preferred) {
                    if (candidate == lane) continue;
                    boolean clear = true;
                    for (Obstacle o : obstacles) {
                        if (o.lane == candidate && o.y > carY - 300) { clear = false; break; }
                    }
                    if (clear) {
                        changeLane(Integer.signum(candidate - lane));
                        break;
                    }
                }
                aiDecisionCooldown = 0.35;
            }

            boolean shouldBrake = threat != null && (threat.y - carY) > -70 && (threat.y - carY) < 40;
            applyInput(!shouldBrake, shouldBrake, dt);
        }

        void updateIdle(double dt, Random random) {
            laneVisual += (lane - laneVisual) * Math.min(1, dt * 8);
            tilt *= 0.9;
            spawnExhaust(random, dt, false);
            for (Particle p : exhaust) p.update(dt);
            exhaust.removeIf(p -> p.life <= 0);
        }

        void updateVisualsOnly(double dt) {
            laneVisual += (lane - laneVisual) * Math.min(1, dt * 8);
        }

        void update(double dt, Random random) {
            double laneDelta = lane - laneVisual;
            laneVisual += laneDelta * Math.min(1, dt * 8);
            tilt = Math.max(-18, Math.min(18, laneDelta * 40));

            distance += speed * dt;
            roadScroll += speed * dt;

            spawnTimer -= dt;
            if (spawnTimer <= 0) {
                int obstacleLane = random.nextInt(GamePanel.LANE_COUNT);
                obstacles.add(new Obstacle(obstacleLane, -GamePanel.CAR_HEIGHT,
                        randomTrafficColor(random)));
                spawnTimer = 0.9 + random.nextDouble() * 1.1;
            }

            double relativeSpeed = speed * 0.6 + 90;
            for (Obstacle o : obstacles) {
                o.y += relativeSpeed * dt;
            }
            obstacles.removeIf(o -> o.y > GamePanel.HEIGHT + 100);

            for (Obstacle o : obstacles) {
                if (!o.hit && o.lane == lane && Math.abs(o.y - carY) < GamePanel.CAR_HEIGHT * 0.55) {
                    o.hit = true;
                    speed *= GamePanel.CRASH_PENALTY;
                    crashFlash = 0.3;
                    spawnCrashBurst(random);
                }
            }

            crashFlash = Math.max(0, crashFlash - dt);

            spawnExhaust(random, dt, speed > 40);
            for (Particle p : exhaust) p.update(dt);
            exhaust.removeIf(p -> p.life <= 0);
        }

        private void spawnExhaust(Random random, double dt, boolean active) {
            if (!active) return;
            if (random.nextDouble() < dt * 12) {
                double px = laneX() + GamePanel.LANE_WIDTH / 2.0 + (random.nextDouble() - 0.5) * 12;
                double py = carY + GamePanel.CAR_HEIGHT * 0.45;
                exhaust.add(new Particle(px, py, (random.nextDouble() - 0.5) * 10,
                        30 + random.nextDouble() * 20, new Color(200, 200, 200), 5, 0.6));
            }
        }

        private void spawnCrashBurst(Random random) {
            double px = laneX() + GamePanel.LANE_WIDTH / 2.0;
            double py = carY;
            for (int i = 0; i < 14; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double v = 40 + random.nextDouble() * 60;
                exhaust.add(new Particle(px, py, Math.cos(angle) * v, Math.sin(angle) * v,
                        new Color(255, 180, 60), 4, 0.5));
            }
        }

        private static Color randomTrafficColor(Random random) {
            Color[] palette = {
                    new Color(200, 200, 200), new Color(60, 60, 70), new Color(210, 40, 40),
                    new Color(30, 120, 60), new Color(230, 200, 40)
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
        double x, y, vx, vy;
        Color color;
        double size;
        double life;
        double maxLife;
        double rotation = 0;

        Particle(double x, double y, double vx, double vy, Color color, double size, double life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.size = size;
            this.life = life;
            this.maxLife = life;
        }

        void update(double dt) {
            x += vx * dt;
            y += vy * dt;
            vy += 60 * dt; // gravity, mostly relevant for confetti
            rotation += dt * 6;
            life -= dt;
        }

        void draw(Graphics2D g2) {
            float alpha = (float) Math.max(0, Math.min(1, life / maxLife));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(color);
            AffineTransform oldT = g2.getTransform();
            g2.translate(x, y);
            g2.rotate(rotation);
            g2.fillRect((int) (-size / 2), (int) (-size / 2), (int) size, (int) size);
            g2.setTransform(oldT);
            g2.setComposite(old);
        }
    }
}