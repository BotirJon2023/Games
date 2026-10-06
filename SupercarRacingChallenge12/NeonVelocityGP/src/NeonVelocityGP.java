import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class NeonVelocityGP extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NeonVelocityGP().setVisible(true));
    }

    static final int WIDTH = 1000;
    static final int HEIGHT = 760;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);
    private GamePanel gamePanel;

    public NeonVelocityGP() {
        super("Neon Velocity GP");
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

        private float phase = 0f;
        private final Timer animTimer;

        MenuPanel(StartListener listener) {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setLayout(null);
            setBackground(Color.BLACK);

            JLabel title = new JLabel("NEON VELOCITY GP", SwingConstants.CENTER);
            title.setFont(new Font("Arial Black", Font.BOLD, 48));
            title.setForeground(new Color(255, 60, 180));
            title.setBounds(0, 150, WIDTH, 60);
            add(title);

            JLabel subtitle = new JLabel("A winding neon circuit \u2014 choose your race", SwingConstants.CENTER);
            subtitle.setFont(new Font("Arial", Font.PLAIN, 20));
            subtitle.setForeground(new Color(180, 220, 255));
            subtitle.setBounds(0, 215, WIDTH, 30);
            add(subtitle);

            JButton twoPlayers = styledButton("2 PLAYERS", new Color(0, 220, 200));
            twoPlayers.setBounds(WIDTH / 2 - 220, 330, 440, 70);
            twoPlayers.addActionListener(e -> listener.start(false));
            add(twoPlayers);

            JButton vsComputer = styledButton("VS COMPUTER", new Color(255, 70, 160));
            vsComputer.setBounds(WIDTH / 2 - 220, 430, 440, 70);
            vsComputer.addActionListener(e -> listener.start(true));
            add(vsComputer);

            JLabel controls = new JLabel(
                    "<html><center>Player 1: A / D steer, W accelerate, S brake, Left-Shift nitro<br>"
                            + "Player 2 (2P mode): &larr; / &rarr; steer, &uarr; accelerate, &darr; brake, Right-Shift nitro<br>"
                            + "3 laps \u2014 first across the line wins</center></html>",
                    SwingConstants.CENTER);
            controls.setFont(new Font("Arial", Font.PLAIN, 14));
            controls.setForeground(new Color(160, 170, 190));
            controls.setBounds(0, 580, WIDTH, 70);
            add(controls);

            animTimer = new Timer(30, e -> {
                phase += 0.01f;
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

            g2.setPaint(new GradientPaint(0, 0, new Color(15, 8, 40), 0, h, new Color(60, 10, 60)));
            g2.fillRect(0, 0, w, h);

            g2.setStroke(new BasicStroke(2f));
            for (int i = 0; i < 8; i++) {
                float t = (i / 8f + phase * 0.05f) % 1f;
                int y = (int) (h * 0.55 + t * h * 0.5);
                int shrink = (int) (t * 380);
                g2.setColor(new Color(255, 60, 200, (int) (40 + 120 * t)));
                g2.drawLine(shrink, y, w - shrink, y);
            }

            g2.setColor(new Color(255, 255, 255, 160));
            for (int i = 0; i < 60; i++) {
                double sx = (i * 37 % w);
                double sy = (i * 53 % (int) (h * 0.5));
                double twinkle = 0.5 + 0.5 * Math.sin(phase * 3 + i);
                g2.setColor(new Color(255, 255, 255, (int) (80 * twinkle)));
                g2.fillOval((int) sx, (int) sy, 2, 2);
            }
        }
    }

    // ======================================================================
    // GAME
    // ======================================================================
    static class GamePanel extends JPanel implements ActionListener {

        enum State { COUNTDOWN, RACING, FINISHED }

        static final int VIEW_W = WIDTH / 2;
        static final int VIEW_H = HEIGHT;
        static final int HORIZON_Y = (int) (VIEW_H * 0.40);

        static final double ROAD_HALF_WIDTH = 4.0;
        static final double CAMERA_NEAR = 1.0;
        static final double FAR_DRAW = 240.0;
        static final double MIN_U = CAMERA_NEAR / FAR_DRAW;
        static final double FOV = 60.0;

        static final double LAP_LENGTH = 900.0;
        static final int LAPS = 3;
        static final double FINISH_DISTANCE = LAP_LENGTH * LAPS;

        static final double MAX_SPEED = 46.0;      // m/s
        static final double NITRO_SPEED_MULT = 1.55;
        static final double ACCELERATION = 26.0;
        static final double BRAKE_DECEL = 46.0;
        static final double FRICTION = 12.0;
        static final double OFFROAD_DRAG = 34.0;
        static final double STEER_ACCEL = 20.0;
        static final double STEER_MAX_V = 7.5;
        static final double STEER_LIMIT = 9.0;

        static final double OBSTACLE_SPACING = 150.0;
        static final double CAR_HALF_WIDTH = 0.9;
        static final double CAR_HALF_LENGTH = 1.5;

        private final boolean vsComputer;
        private final Runnable onExit;
        private final Timer timer;
        private long lastNanos;
        private double elapsed = 0;

        private final Set<Integer> pressed = new HashSet<>();
        private final Set<Integer> pressedLoc = new HashSet<>(); // encode (keyCode<<2 | location)

        private final Racer p1;
        private final Racer p2;

        private State state = State.COUNTDOWN;
        private double countdownRemaining = 3.999;
        private double goFlashRemaining = 0;
        private Racer winner = null;
        private final List<Particle> confetti = new ArrayList<>();
        private boolean debugAutoStart = false;

        GamePanel(boolean vsComputer, Runnable onExit) {
            this.vsComputer = vsComputer;
            this.onExit = onExit;
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            setBackground(Color.BLACK);

            p1 = new Racer("PLAYER 1", new Color(0, 220, 200), new Color(140, 255, 240), false);
            p2 = new Racer(vsComputer ? "COMPUTER" : "PLAYER 2",
                    new Color(255, 70, 160), new Color(255, 160, 210), vsComputer);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    pressed.add(e.getKeyCode());
                    pressedLoc.add(encode(e.getKeyCode(), e.getKeyLocation()));
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
                    pressedLoc.remove(encode(e.getKeyCode(), e.getKeyLocation()));
                }
            });

            timer = new Timer(16, this);
        }

        private static int encode(int keyCode, int location) {
            return (keyCode << 2) | (location & 0x3);
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

        static double trackLateral(double d) {
            return 4.2 * Math.sin(d * (2 * Math.PI / 150.0))
                    + 1.8 * Math.sin(d * (2 * Math.PI / 60.0) + 1.3);
        }

        static double laneOffsetHash(long k) {
            double s = Math.sin(k * 12.9898) * 43758.5453;
            double frac = s - Math.floor(s);
            return (frac - 0.5) * 2.0 * 2.6;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            double dt = Math.min((now - lastNanos) / 1_000_000_000.0, 0.05);
            lastNanos = now;
            elapsed += dt;
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
                return;
            }

            if (goFlashRemaining > 0) goFlashRemaining -= dt;

            if (state == State.RACING) {
                boolean p1Left = pressed.contains(KeyEvent.VK_A);
                boolean p1Right = pressed.contains(KeyEvent.VK_D);
                boolean p1Accel = pressed.contains(KeyEvent.VK_W);
                boolean p1Brake = pressed.contains(KeyEvent.VK_S);
                boolean p1Nitro = pressedLoc.contains(encode(KeyEvent.VK_SHIFT, KeyEvent.KEY_LOCATION_LEFT));
                p1.applyInput(p1Left, p1Right, p1Accel, p1Brake, p1Nitro, dt);
                p1.update(dt);

                if (vsComputer) {
                    p2.driveAI(dt);
                } else {
                    boolean p2Left = pressed.contains(KeyEvent.VK_LEFT);
                    boolean p2Right = pressed.contains(KeyEvent.VK_RIGHT);
                    boolean p2Accel = pressed.contains(KeyEvent.VK_UP);
                    boolean p2Brake = pressed.contains(KeyEvent.VK_DOWN);
                    boolean p2Nitro = pressedLoc.contains(encode(KeyEvent.VK_SHIFT, KeyEvent.KEY_LOCATION_RIGHT));
                    p2.applyInput(p2Left, p2Right, p2Accel, p2Brake, p2Nitro, dt);
                }
                p2.update(dt);

                if (p1.distance >= FINISH_DISTANCE || p2.distance >= FINISH_DISTANCE) {
                    winner = p1.distance >= p2.distance ? p1 : p2;
                    state = State.FINISHED;
                    spawnConfetti();
                }
            } else if (state == State.FINISHED) {
                for (Particle particle : confetti) particle.update(dt);
                confetti.removeIf(particle -> particle.life <= 0);
                if (confetti.isEmpty() && Math.random() < 0.02) spawnConfetti();
            }
        }

        private void spawnConfetti() {
            Color[] palette = {
                    new Color(0, 220, 200), new Color(255, 70, 160), new Color(255, 220, 0),
                    new Color(120, 200, 255), new Color(230, 90, 230)
            };
            for (int i = 0; i < 140; i++) {
                double x = Math.random() * WIDTH;
                double y = -Math.random() * 200;
                double vx = (Math.random() - 0.5) * 80;
                double vy = 80 + Math.random() * 120;
                Color c = palette[(int) (Math.random() * palette.length)];
                confetti.add(new Particle(x, y, vx, vy, c, 3 + Math.random() * 4, 4.0));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawSide(g2, 0, p1);
            drawSide(g2, VIEW_W, p2);
            drawDivider(g2);

            if (state == State.COUNTDOWN) drawCountdown(g2);
            if (goFlashRemaining > 0) drawGoFlash(g2);
            if (state == State.FINISHED) drawFinishOverlay(g2);
        }

        private void drawDivider(Graphics2D g2) {
            g2.setColor(new Color(10, 10, 15));
            g2.fillRect(VIEW_W - 3, 0, 6, HEIGHT);
            g2.setFont(new Font("Arial Black", Font.BOLD, 20));
            g2.setColor(new Color(255, 255, 255, 210));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("VS", VIEW_W - fm.stringWidth("VS") / 2, 26);
        }

        private void drawSide(Graphics2D g2, int offsetX, Racer racer) {
            Shape oldClip = g2.getClip();
            g2.setClip(offsetX, 0, VIEW_W, VIEW_H);
            g2.translate(offsetX, 0);

            drawSky(g2, racer);
            drawRoad(g2, racer);
            drawPlayerCar(g2, racer);
            drawHud(g2, racer);

            g2.translate(-offsetX, 0);
            g2.setClip(oldClip);
        }

        private void drawSky(Graphics2D g2, Racer racer) {
            double dayPhase = (Math.sin(elapsed * 0.04) + 1) / 2.0; // 0=night 1=day, slow cycle
            Color top = lerp(new Color(10, 8, 35), new Color(120, 170, 230), dayPhase);
            Color bottom = lerp(new Color(40, 20, 60), new Color(230, 200, 170), dayPhase);
            g2.setPaint(new GradientPaint(0, 0, top, 0, HORIZON_Y, bottom));
            g2.fillRect(0, 0, VIEW_W, HORIZON_Y);

            if (dayPhase < 0.6) {
                float starAlpha = (float) (1 - dayPhase / 0.6);
                for (int i = 0; i < 40; i++) {
                    int sx = (i * 71) % VIEW_W;
                    int sy = (i * 37) % (HORIZON_Y - 10);
                    double twinkle = 0.5 + 0.5 * Math.sin(elapsed * 3 + i * 1.7);
                    g2.setColor(new Color(255, 255, 255, (int) (200 * starAlpha * twinkle)));
                    g2.fillOval(sx, sy, 2, 2);
                }
            }

            int glowX = (int) (VIEW_W * (0.15 + 0.7 * ((elapsed * 0.01) % 1)));
            int glowY = (int) (HORIZON_Y * (1.1 - dayPhase * 0.9));
            Color glow = dayPhase > 0.5 ? new Color(255, 230, 140) : new Color(210, 210, 255);
            g2.setColor(glow);
            g2.fillOval(glowX - 22, glowY - 22, 44, 44);

            g2.setColor(new Color(30, 20, 45, 160));
            int baseY = HORIZON_Y - 4;
            for (int i = -1; i < 6; i++) {
                int mx = (int) (i * 140 - (racer.distance * 0.02) % 140);
                int peak = 40 + (i % 3) * 20;
                Polygon mountain = new Polygon();
                mountain.addPoint(mx - 90, baseY);
                mountain.addPoint(mx, baseY - peak);
                mountain.addPoint(mx + 90, baseY);
                g2.fillPolygon(mountain);
            }
        }

        private static Color lerp(Color a, Color b, double t) {
            t = Math.max(0, Math.min(1, t));
            return new Color(
                    (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                    (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                    (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
        }

        private void drawRoad(Graphics2D g2, Racer racer) {
            double camDistance = racer.distance;
            Color grassA = new Color(20, 90, 45);
            Color grassB = new Color(24, 100, 50);
            Color roadColor = new Color(55, 55, 62);
            Color rumbleA = new Color(210, 30, 30);
            Color rumbleB = new Color(230, 230, 230);

            for (int screenY = HORIZON_Y; screenY < VIEW_H; screenY++) {
                double u = (screenY - HORIZON_Y) / (double) (VIEW_H - HORIZON_Y);
                double uu = Math.max(u, MIN_U);
                double depth = CAMERA_NEAR / uu;
                double widthScale = FOV / depth;
                double curveWorld = trackLateral(camDistance + depth) - trackLateral(camDistance);
                double centerPx = VIEW_W / 2.0 + (curveWorld - racer.steer) * widthScale;
                double halfRoadPx = ROAD_HALF_WIDTH * widthScale;

                g2.setColor(((screenY / 6) % 2 == 0) ? grassA : grassB);
                g2.fillRect(0, screenY, VIEW_W, 1);

                int roadLeft = (int) (centerPx - halfRoadPx);
                int roadRight = (int) (centerPx + halfRoadPx);
                int rumble = Math.max(2, (int) (halfRoadPx * 0.12));
                boolean stripe = (((long) ((camDistance + depth) / 6)) % 2 == 0);
                g2.setColor(stripe ? rumbleA : rumbleB);
                g2.fillRect(roadLeft - rumble, screenY, rumble, 1);
                g2.fillRect(roadRight, screenY, rumble, 1);

                g2.setColor(roadColor);
                g2.fillRect(roadLeft, screenY, Math.max(1, roadRight - roadLeft), 1);

                if (halfRoadPx > 6) {
                    boolean dash = (((long) ((camDistance + depth) / 5)) % 2 == 0);
                    if (dash) {
                        g2.setColor(new Color(240, 220, 120));
                        g2.fillRect((int) (centerPx - 2), screenY, 4, 1);
                    }
                }
            }

            drawLapLine(g2, racer);
            drawObstacles(g2, racer);
        }

        private void drawLapLine(Graphics2D g2, Racer racer) {
            double nextLap = (Math.floor(racer.distance / LAP_LENGTH) + 1) * LAP_LENGTH;
            double depthAhead = nextLap - racer.distance;
            if (depthAhead <= 0.5 || depthAhead >= FAR_DRAW) return;

            double u = Math.max(MIN_U, Math.min(1.0, CAMERA_NEAR / depthAhead));
            int screenY = (int) (HORIZON_Y + u * (VIEW_H - HORIZON_Y));
            double widthScale = FOV / depthAhead;
            double curveWorld = trackLateral(nextLap) - trackLateral(racer.distance);
            double centerPx = VIEW_W / 2.0 + (curveWorld - racer.steer) * widthScale;
            double halfRoadPx = ROAD_HALF_WIDTH * widthScale;
            int bandH = Math.max(1, (int) (widthScale * 0.9));

            int squares = 10;
            double sq = (halfRoadPx * 2) / squares;
            for (int i = 0; i < squares; i++) {
                boolean black = (i % 2 == 0);
                g2.setColor(black ? Color.BLACK : Color.WHITE);
                g2.fillRect((int) (centerPx - halfRoadPx + i * sq), screenY - bandH / 2, (int) Math.ceil(sq) + 1, bandH);
            }
        }

        private void drawObstacles(Graphics2D g2, Racer racer) {
            long firstIndex = (long) Math.floor((racer.distance - 30) / OBSTACLE_SPACING);
            List<double[]> toDraw = new ArrayList<>();
            for (long k = firstIndex; k < firstIndex + 6; k++) {
                double obstacleDistance = k * OBSTACLE_SPACING + 40;
                double depthAhead = obstacleDistance - racer.distance;
                if (depthAhead <= 0.4 || depthAhead >= FAR_DRAW) continue;
                double laneOffset = laneOffsetHash(k);
                double worldLateral = trackLateral(obstacleDistance) + laneOffset;
                toDraw.add(new double[]{depthAhead, worldLateral, k});
            }
            toDraw.sort((a, b) -> Double.compare(b[0], a[0])); // far to near

            for (double[] entry : toDraw) {
                double depthAhead = entry[0];
                double worldLateral = entry[1];
                double u = Math.max(MIN_U, Math.min(1.0, CAMERA_NEAR / depthAhead));
                int screenY = (int) (HORIZON_Y + u * (VIEW_H - HORIZON_Y));
                double widthScale = FOV / depthAhead;
                double centerPx = VIEW_W / 2.0 + (worldLateral - trackLateral(racer.distance) - racer.steer) * widthScale;
                // rendered a bit larger than the true collision box so rivals stay readable from farther away
                double carHalfPx = CAR_HALF_WIDTH * widthScale * 2.1;
                if (carHalfPx < 1.2) continue;

                int cw = (int) (carHalfPx * 2);
                int ch = (int) (carHalfPx * 3.0);
                Color body = new Color(230, 210, 60);
                g2.setPaint(new GradientPaint((float) (centerPx - carHalfPx), 0, body.darker(), (float) (centerPx + carHalfPx), 0, body));
                g2.fillRoundRect((int) (centerPx - carHalfPx), screenY - ch, Math.max(2, cw), Math.max(2, ch), cw / 3 + 1, cw / 3 + 1);
                g2.setColor(new Color(20, 20, 20));
                g2.fillRoundRect((int) (centerPx - carHalfPx * 0.7), screenY - ch + (int) (ch * 0.15), Math.max(1, (int) (carHalfPx * 1.4)), Math.max(1, (int) (ch * 0.3)), 4, 4);
            }
        }

        private void drawPlayerCar(Graphics2D g2, Racer racer) {
            int cx = VIEW_W / 2;
            int cy = VIEW_H - 90;
            double tilt = Math.max(-16, Math.min(16, racer.steerVelocity * 2.2));

            for (Particle p : racer.effects) p.draw(g2, cx, cy);

            AffineTransform old = g2.getTransform();
            g2.translate(cx, cy);
            g2.rotate(Math.toRadians(tilt));

            int w = 76, h = 130;
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillOval(-w / 2 + 6, h / 2 - 14, w - 12, 22);

            Path2D body = new Path2D.Double();
            body.moveTo(-w / 2.0, h * 0.38);
            body.curveTo(-w / 2.0, -h * 0.05, -w * 0.30, -h * 0.5, 0, -h / 2.0);
            body.curveTo(w * 0.30, -h * 0.5, w / 2.0, -h * 0.05, w / 2.0, h * 0.38);
            body.curveTo(w / 2.0, h * 0.5, -w / 2.0, h * 0.5, -w / 2.0, h * 0.38);
            body.closePath();

            Color mainColor = racer.crashFlash > 0 ? new Color(255, 60, 60) : racer.color;
            g2.setPaint(new GradientPaint(-w / 2f, 0, mainColor.darker(), w / 2f, 0, racer.colorLight));
            g2.fill(body);
            g2.setColor(new Color(0, 0, 0, 100));
            g2.setStroke(new BasicStroke(2));
            g2.draw(body);

            g2.setColor(new Color(20, 30, 55, 235));
            g2.fill(new RoundRectangle2D.Double(-w * 0.26, -h * 0.34, w * 0.52, h * 0.30, 12, 12));
            g2.setColor(new Color(255, 255, 255, 100));
            g2.fillOval((int) (-w * 0.18), (int) (-h * 0.32), (int) (w * 0.16), (int) (h * 0.1));

            g2.setColor(new Color(255, 240, 170));
            g2.fillOval(-w / 2, (int) (-h * 0.44), 10, 8);
            g2.fillOval(w / 2 - 10, (int) (-h * 0.44), 10, 8);

            boolean nitroActive = racer.nitroActive;
            g2.setColor(nitroActive ? new Color(120, 200, 255) : new Color(255, 40, 40));
            g2.fillRect(-w / 2 + 2, (int) (h * 0.44), 10, 5);
            g2.fillRect(w / 2 - 12, (int) (h * 0.44), 10, 5);

            g2.setColor(new Color(15, 15, 15));
            g2.fillRoundRect((int) (-w * 0.55), (int) (h * 0.42), (int) (w * 1.1), 8, 4, 4);

            if (nitroActive) {
                g2.setColor(new Color(150, 220, 255, 220));
                Polygon flame = new Polygon();
                flame.addPoint(-10, (int) (h * 0.48));
                flame.addPoint(10, (int) (h * 0.48));
                flame.addPoint(0, (int) (h * 0.48 + 24 + Math.random() * 10));
                g2.fillPolygon(flame);
            }

            g2.setTransform(old);
        }

        private void drawHud(Graphics2D g2, Racer racer) {
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.setColor(Color.WHITE);
            g2.drawString(racer.name, 16, 24);

            int lap = Math.min(LAPS, (int) (racer.distance / LAP_LENGTH) + 1);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("LAP " + lap + "/" + LAPS, 16, 44);

            int barX = 16, barY = 52, barW = VIEW_W - 32, barH = 8;
            g2.setColor(new Color(255, 255, 255, 60));
            g2.fillRoundRect(barX, barY, barW, barH, 6, 6);
            int fillW = (int) (barW * Math.min(1.0, racer.distance / FINISH_DISTANCE));
            g2.setPaint(new GradientPaint(barX, 0, racer.color, barX + barW, 0, racer.colorLight));
            g2.fillRoundRect(barX, barY, Math.max(0, fillW), barH, 6, 6);

            int nitroBarW = 140, nitroBarH = 10;
            int nx = 16, ny = VIEW_H - 30;
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(nx - 2, ny - 2, nitroBarW + 4, nitroBarH + 4, 8, 8);
            g2.setColor(new Color(255, 255, 255, 50));
            g2.fillRoundRect(nx, ny, nitroBarW, nitroBarH, 6, 6);
            int nFill = (int) (nitroBarW * (racer.nitroFuel / 100.0));
            g2.setPaint(new GradientPaint(nx, 0, new Color(80, 160, 255), nx + nitroBarW, 0, new Color(180, 240, 255)));
            g2.fillRoundRect(nx, ny, Math.max(0, nFill), nitroBarH, 6, 6);
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.setColor(Color.WHITE);
            g2.drawString("NITRO", nx, ny - 4);

            int speedKmh = (int) (racer.speed * 3.2);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.setColor(new Color(255, 255, 255, 230));
            String txt = speedKmh + " km/h";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(txt, VIEW_W - fm.stringWidth(txt) - 16, VIEW_H - 20);
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

            g2.setColor(new Color(0, 230, 255));
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
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            for (Particle p : confetti) p.draw(g2, 0, 0);

            String text = winner.name + " WINS THE RACE!";
            g2.setFont(new Font("Arial Black", Font.BOLD, 46));
            FontMetrics fm = g2.getFontMetrics();
            int tx = WIDTH / 2 - fm.stringWidth(text) / 2;
            g2.setColor(new Color(0, 230, 255));
            g2.drawString(text, tx, HEIGHT / 2 - 30);

            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            String hint = "Press ENTER to race again, ESC for menu";
            fm = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(hint, WIDTH / 2 - fm.stringWidth(hint) / 2, HEIGHT / 2 + 30);
        }
    }

    // ======================================================================
    // RACER
    // ======================================================================
    static class Racer {
        final String name;
        final Color color;
        final Color colorLight;
        final boolean ai;

        double steer = 0;
        double steerVelocity = 0;
        double speed = 0;
        double distance = 0;
        double crashFlash = 0;
        double nitroFuel = 100;
        boolean nitroActive = false;
        double aiCooldown = 0;

        final List<Particle> effects = new ArrayList<>();

        Racer(String name, Color color, Color colorLight, boolean ai) {
            this.name = name;
            this.color = color;
            this.colorLight = colorLight;
            this.ai = ai;
        }

        void reset() {
            steer = 0;
            steerVelocity = 0;
            speed = 0;
            distance = 0;
            crashFlash = 0;
            nitroFuel = 100;
            nitroActive = false;
            effects.clear();
        }

        void applyInput(boolean left, boolean right, boolean accel, boolean brake, boolean nitroRequested, double dt) {
            if (left && !right) steerVelocity -= GamePanel.STEER_ACCEL * dt;
            else if (right && !left) steerVelocity += GamePanel.STEER_ACCEL * dt;
            else steerVelocity *= Math.max(0, 1 - dt * 6);
            steerVelocity = Math.max(-GamePanel.STEER_MAX_V, Math.min(GamePanel.STEER_MAX_V, steerVelocity));

            double effectiveness = 0.25 + 0.75 * (speed / GamePanel.MAX_SPEED);
            steer += steerVelocity * dt * effectiveness;
            steer = Math.max(-GamePanel.STEER_LIMIT, Math.min(GamePanel.STEER_LIMIT, steer));

            boolean offRoad = Math.abs(steer) > GamePanel.ROAD_HALF_WIDTH;

            nitroActive = nitroRequested && nitroFuel > 2;
            double maxSpeed = GamePanel.MAX_SPEED * (nitroActive ? GamePanel.NITRO_SPEED_MULT : 1.0);
            double accelRate = GamePanel.ACCELERATION * (nitroActive ? 1.6 : 1.0);

            if (accel) speed += accelRate * dt;
            else if (brake) speed -= GamePanel.BRAKE_DECEL * dt;
            else speed -= GamePanel.FRICTION * dt;

            if (offRoad) speed -= GamePanel.OFFROAD_DRAG * dt;

            speed = Math.max(0, Math.min(maxSpeed, speed));

            if (nitroActive) nitroFuel = Math.max(0, nitroFuel - dt * 34);
            else nitroFuel = Math.min(100, nitroFuel + dt * 10);
        }

        void driveAI(double dt) {
            aiCooldown -= dt;
            double lookAheadDistance = 40;
            double futureLateral = GamePanel.trackLateral(distance + lookAheadDistance) - GamePanel.trackLateral(distance);

            long firstIndex = (long) Math.floor((distance - 10) / GamePanel.OBSTACLE_SPACING);
            Double avoidTarget = null;
            for (long k = firstIndex; k < firstIndex + 3; k++) {
                double obstacleDistance = k * GamePanel.OBSTACLE_SPACING + 40;
                double depthAhead = obstacleDistance - distance;
                if (depthAhead > 4 && depthAhead < 55) {
                    double laneOffset = GamePanel.laneOffsetHash(k);
                    double obstacleWorldLateral = GamePanel.trackLateral(obstacleDistance) + laneOffset;
                    double obstacleRelative = obstacleWorldLateral - GamePanel.trackLateral(distance);
                    if (Math.abs(obstacleRelative - steer) < GamePanel.CAR_HALF_WIDTH * 2.4) {
                        avoidTarget = obstacleRelative > 0 ? obstacleRelative - 3.2 : obstacleRelative + 3.2;
                    }
                }
            }

            double targetSteer = avoidTarget != null ? avoidTarget : futureLateral * 0.6;
            boolean left = steer > targetSteer + 0.3;
            boolean right = steer < targetSteer - 0.3;
            boolean nitroWanted = avoidTarget == null && nitroFuel > 40 && aiCooldown <= 0;
            if (nitroWanted && Math.random() < 0.01) aiCooldown = 2.0;

            applyInput(left, right, true, false, nitroWanted, dt);
        }

        void update(double dt) {
            distance += speed * dt;

            long firstIndex = (long) Math.floor((distance - 5) / GamePanel.OBSTACLE_SPACING);
            for (long k = firstIndex; k < firstIndex + 3; k++) {
                double obstacleDistance = k * GamePanel.OBSTACLE_SPACING + 40;
                double depthAhead = obstacleDistance - distance;
                if (Math.abs(depthAhead) < GamePanel.CAR_HALF_LENGTH * 1.4) {
                    double laneOffset = GamePanel.laneOffsetHash(k);
                    double obstacleWorldLateral = GamePanel.trackLateral(obstacleDistance) + laneOffset;
                    double obstacleRelative = obstacleWorldLateral - GamePanel.trackLateral(distance);
                    if (Math.abs(obstacleRelative - steer) < GamePanel.CAR_HALF_WIDTH * 1.9 && crashFlash <= 0) {
                        speed *= 0.4;
                        crashFlash = 0.35;
                        spawnCrashBurst();
                    }
                }
            }
            crashFlash = Math.max(0, crashFlash - dt);

            if (speed > 6) {
                if (Math.random() < dt * (nitroActive ? 40 : 10)) {
                    Color c = nitroActive ? new Color(150, 210, 255) : new Color(210, 210, 210);
                    double life = nitroActive ? 0.4 : 0.6;
                    effects.add(new Particle((Math.random() - 0.5) * 20, 62 + Math.random() * 10,
                            (Math.random() - 0.5) * 12, 40 + Math.random() * 30, c, nitroActive ? 6 : 5, life));
                }
            }
            if (Math.abs(steerVelocity) > 4 && speed > GamePanel.MAX_SPEED * 0.55 && Math.random() < dt * 20) {
                effects.add(new Particle((steerVelocity > 0 ? -22 : 22), 55, (Math.random() - 0.5) * 6,
                        20 + Math.random() * 10, new Color(230, 230, 230), 5, 0.5));
            }

            for (Particle p : effects) p.update(dt);
            effects.removeIf(p -> p.life <= 0);
        }

        private void spawnCrashBurst() {
            for (int i = 0; i < 12; i++) {
                double angle = Math.random() * Math.PI * 2;
                double v = 40 + Math.random() * 60;
                effects.add(new Particle(0, 20, Math.cos(angle) * v, Math.sin(angle) * v,
                        new Color(255, 180, 60), 4, 0.5));
            }
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
            vy += 40 * dt;
            rotation += dt * 5;
            life -= dt;
        }

        void draw(Graphics2D g2, int originX, int originY) {
            float alpha = (float) Math.max(0, Math.min(1, life / maxLife));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(color);
            AffineTransform oldT = g2.getTransform();
            g2.translate(originX + x, originY + y);
            g2.rotate(rotation);
            g2.fillRect((int) (-size / 2), (int) (-size / 2), (int) size, (int) size);
            g2.setTransform(oldT);
            g2.setComposite(old);
        }
    }
}