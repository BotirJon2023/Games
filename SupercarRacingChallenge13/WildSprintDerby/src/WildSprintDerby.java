import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class WildSprintDerby extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WildSprintDerby().setVisible(true));
    }

    static final int WIDTH = 1000;
    static final int HEIGHT = 700;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);
    private GamePanel gamePanel;

    public WildSprintDerby() {
        super("Wild Sprint Derby");
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

            JLabel title = new JLabel("WILD SPRINT DERBY", SwingConstants.CENTER);
            title.setFont(new Font("Arial Black", Font.BOLD, 46));
            title.setForeground(new Color(255, 170, 40));
            title.setBounds(0, 140, WIDTH, 60);
            add(title);

            JLabel subtitle = new JLabel("Cheetah vs Zebra \u2014 clear the hurdles, cross first", SwingConstants.CENTER);
            subtitle.setFont(new Font("Arial", Font.PLAIN, 20));
            subtitle.setForeground(new Color(255, 235, 200));
            subtitle.setBounds(0, 205, WIDTH, 30);
            add(subtitle);

            JButton twoPlayers = styledButton("2 PLAYERS", new Color(60, 170, 90));
            twoPlayers.setBounds(WIDTH / 2 - 220, 320, 440, 70);
            twoPlayers.addActionListener(e -> listener.start(false));
            add(twoPlayers);

            JButton vsComputer = styledButton("VS COMPUTER", new Color(210, 120, 40));
            vsComputer.setBounds(WIDTH / 2 - 220, 420, 440, 70);
            vsComputer.addActionListener(e -> listener.start(true));
            add(vsComputer);

            JLabel controls = new JLabel(
                    "<html><center>Player 1 (Cheetah): W run, E jump, Left-Shift burst<br>"
                            + "Player 2 (Zebra, 2P mode): &uarr; run, L jump, Right-Shift burst<br>"
                            + "Time your jump to clear each hurdle \u2014 miss it and you stumble</center></html>",
                    SwingConstants.CENTER);
            controls.setFont(new Font("Arial", Font.PLAIN, 14));
            controls.setForeground(new Color(220, 210, 190));
            controls.setBounds(0, 560, WIDTH, 70);
            add(controls);

            animTimer = new Timer(30, e -> {
                phase += 0.015f;
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

            g2.setPaint(new GradientPaint(0, 0, new Color(255, 190, 110), 0, h * 0.6f, new Color(255, 140, 90)));
            g2.fillRect(0, 0, w, (int) (h * 0.6));
            g2.setColor(new Color(210, 150, 60));
            g2.fillRect(0, (int) (h * 0.6), w, (int) (h * 0.4));

            g2.setColor(new Color(90, 50, 20, 140));
            for (int i = 0; i < 5; i++) {
                double t = (i / 5.0 + phase * 0.03) % 1.0;
                int bx = (int) (t * (w + 200)) - 100;
                int by = (int) (h * 0.58);
                g2.fillRect(bx, by, 4, 40);
                g2.fillOval(bx - 20, by - 20, 44, 24);
            }
        }
    }

    // ======================================================================
    // GAME
    // ======================================================================
    static class GamePanel extends JPanel implements ActionListener {

        enum State { COUNTDOWN, RACING, FINISHED }

        static final int LANE_H = HEIGHT / 2;
        static final int ANIMAL_X = 230;
        static final double GROUND_Y_RATIO = 0.72;
        static final double PIXELS_PER_UNIT = 42.0;

        static final double FINISH_DISTANCE = 500.0;
        static final double MAX_SPEED = 11.0;
        static final double BURST_MULT = 1.55;
        static final double ACCELERATION = 7.0;
        static final double FRICTION = 3.4;
        static final double STUMBLE_PENALTY = 0.35;

        static final double HURDLE_SPACING = 55.0;
        static final double HURDLE_FIRST_OFFSET = 34.0;
        static final double HURDLE_HALF_WIDTH = 0.55;
        static final double JUMP_DURATION = 0.55;

        private final boolean vsComputer;
        private final Runnable onExit;
        private final Timer timer;
        private long lastNanos;
        private double elapsed = 0;

        private final Set<Integer> pressed = new HashSet<>();
        private final Set<Integer> pressedLoc = new HashSet<>();
        private final Set<Integer> justPressed = new HashSet<>();

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

            p1 = new Racer("CHEETAH", new Color(215, 150, 70), new Color(40, 30, 20), Species.CHEETAH, false);
            p2 = new Racer(vsComputer ? "ZEBRA (CPU)" : "ZEBRA",
                    new Color(250, 250, 245), new Color(30, 30, 30), Species.ZEBRA, vsComputer);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (!pressed.contains(e.getKeyCode())) justPressed.add(e.getKeyCode());
                    pressed.add(e.getKeyCode());
                    pressedLoc.add(encode(e.getKeyCode(), e.getKeyLocation()));
                    if (state == State.FINISHED && e.getKeyCode() == KeyEvent.VK_ENTER) restart();
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) onExit.run();
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

        static double hurdleDistance(long k) {
            return k * HURDLE_SPACING + HURDLE_FIRST_OFFSET;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            double dt = Math.min((now - lastNanos) / 1_000_000_000.0, 0.05);
            lastNanos = now;
            elapsed += dt;
            update(dt);
            justPressed.clear();
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
                boolean p1Run = pressed.contains(KeyEvent.VK_W);
                boolean p1Jump = justPressed.contains(KeyEvent.VK_E);
                boolean p1Burst = pressedLoc.contains(encode(KeyEvent.VK_SHIFT, KeyEvent.KEY_LOCATION_LEFT));
                p1.applyInput(p1Run, p1Jump, p1Burst, dt);
                p1.update(dt);

                if (vsComputer) {
                    p2.driveAI(dt);
                } else {
                    boolean p2Run = pressed.contains(KeyEvent.VK_UP);
                    boolean p2Jump = justPressed.contains(KeyEvent.VK_L);
                    boolean p2Burst = pressedLoc.contains(encode(KeyEvent.VK_SHIFT, KeyEvent.KEY_LOCATION_RIGHT));
                    p2.applyInput(p2Run, p2Jump, p2Burst, dt);
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
                    new Color(255, 170, 40), new Color(60, 170, 90), new Color(255, 220, 0),
                    new Color(250, 250, 245), new Color(230, 90, 90)
            };
            for (int i = 0; i < 130; i++) {
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

            drawLane(g2, 0, p1);
            drawLane(g2, LANE_H, p2);
            drawDivider(g2);

            if (state == State.COUNTDOWN) drawCountdown(g2);
            if (goFlashRemaining > 0) drawGoFlash(g2);
            if (state == State.FINISHED) drawFinishOverlay(g2);
        }

        private void drawDivider(Graphics2D g2) {
            g2.setColor(new Color(20, 15, 10));
            g2.fillRect(0, LANE_H - 3, WIDTH, 6);
            g2.setFont(new Font("Arial Black", Font.BOLD, 18));
            g2.setColor(new Color(255, 255, 255, 220));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("VS", WIDTH / 2 - fm.stringWidth("VS") / 2, LANE_H + 6);
        }

        private void drawLane(Graphics2D g2, int offsetY, Racer racer) {
            Shape oldClip = g2.getClip();
            g2.setClip(0, offsetY, WIDTH, LANE_H);
            g2.translate(0, offsetY);

            int groundY = (int) (LANE_H * GROUND_Y_RATIO);
            drawBackground(g2, racer, groundY);
            drawGround(g2, racer, groundY);
            drawFinishRibbon(g2, racer, groundY);
            drawHurdles(g2, racer, groundY);
            drawAnimal(g2, racer, groundY);
            for (Particle p : racer.effects) p.draw(g2, 0, 0);
            drawHud(g2, racer);

            g2.translate(0, -offsetY);
            g2.setClip(oldClip);
        }

        private void drawBackground(Graphics2D g2, Racer racer, int groundY) {
            g2.setPaint(new GradientPaint(0, 0, new Color(160, 205, 235), 0, groundY, new Color(235, 225, 190)));
            g2.fillRect(0, 0, WIDTH, groundY);

            g2.setColor(new Color(255, 250, 220, 200));
            double sunX = 120 + (Math.sin(elapsed * 0.05) * 30);
            g2.fillOval((int) sunX, 30, 46, 46);

            g2.setColor(new Color(255, 255, 255, 180));
            for (int i = 0; i < 4; i++) {
                double cloudX = ((i * 320 - racer.distance * PIXELS_PER_UNIT * 0.08) % (WIDTH + 200)) - 100;
                int cy = 40 + i % 2 * 30;
                g2.fillOval((int) cloudX, cy, 90, 26);
                g2.fillOval((int) cloudX + 30, cy - 10, 70, 26);
            }

            g2.setColor(new Color(80, 60, 30, 160));
            double hillOffset = (racer.distance * PIXELS_PER_UNIT * 0.25) % 260;
            for (int i = -1; i < 5; i++) {
                int hx = (int) (i * 260 - hillOffset);
                Polygon hill = new Polygon();
                hill.addPoint(hx - 130, groundY);
                hill.addPoint(hx, groundY - 55);
                hill.addPoint(hx + 130, groundY);
                g2.fillPolygon(hill);
            }

            g2.setColor(new Color(40, 70, 30, 200));
            double treeOffset = (racer.distance * PIXELS_PER_UNIT * 0.55) % 220;
            for (int i = -1; i < 6; i++) {
                int tx = (int) (i * 220 - treeOffset);
                g2.fillRect(tx - 3, groundY - 60, 6, 60);
                g2.fillOval(tx - 34, groundY - 100, 68, 46);
            }
        }

        private void drawGround(Graphics2D g2, Racer racer, int groundY) {
            g2.setColor(new Color(196, 160, 100));
            g2.fillRect(0, groundY, WIDTH, LANE_H - groundY);

            double scrollOffset = (racer.distance * PIXELS_PER_UNIT) % 60;
            g2.setColor(new Color(170, 135, 80));
            for (double x = -scrollOffset; x < WIDTH; x += 60) {
                g2.fillRect((int) x, groundY + 6, 30, 4);
            }
            g2.setColor(new Color(120, 90, 55));
            g2.fillRect(0, groundY, WIDTH, 3);
        }

        private void drawFinishRibbon(Graphics2D g2, Racer racer, int groundY) {
            double depthAhead = FINISH_DISTANCE - racer.distance;
            if (depthAhead < -3 || depthAhead > 30) return;
            int screenX = ANIMAL_X + (int) (depthAhead * PIXELS_PER_UNIT);
            int poleTop = groundY - 130;
            g2.setColor(new Color(60, 60, 60));
            g2.fillRect(screenX - 3, poleTop, 6, groundY - poleTop);
            g2.fillRect(screenX + 60 - 3, poleTop, 6, groundY - poleTop);
            int squares = 6;
            double sqW = 60.0 / squares;
            for (int i = 0; i < squares; i++) {
                g2.setColor(i % 2 == 0 ? Color.BLACK : Color.WHITE);
                g2.fillRect((int) (screenX + i * sqW), poleTop, (int) Math.ceil(sqW) + 1, 22);
            }
        }

        private void drawHurdles(Graphics2D g2, Racer racer, int groundY) {
            long firstIndex = (long) Math.floor((racer.distance - 5) / HURDLE_SPACING);
            for (long k = Math.max(0, firstIndex); k < firstIndex + 4; k++) {
                double hd = hurdleDistance(k);
                double depthAhead = hd - racer.distance;
                if (depthAhead < -1.5 || depthAhead > 16) continue;
                int screenX = ANIMAL_X + (int) (depthAhead * PIXELS_PER_UNIT);
                int hh = 46;
                g2.setColor(new Color(150, 90, 40));
                g2.fillRect(screenX - 4, groundY - hh, 8, hh);
                g2.fillRect(screenX + 26, groundY - hh, 8, hh);
                g2.setColor(new Color(200, 60, 40));
                g2.fillRect(screenX - 8, groundY - hh - 6, 46, 10);
            }
        }

        private void drawAnimal(Graphics2D g2, Racer racer, int groundY) {
            int cx = ANIMAL_X;
            double jumpArc = 0;
            if (racer.jumpTimeLeft > 0) {
                double t = 1 - (racer.jumpTimeLeft / JUMP_DURATION);
                jumpArc = Math.sin(t * Math.PI) * 60;
            }
            double stumbleShake = racer.stumbleFlash > 0 ? Math.sin(elapsed * 60) * 4 : 0;
            int bodyY = groundY - 34 - (int) jumpArc + (int) stumbleShake;

            AffineTransform old = g2.getTransform();
            g2.translate(cx, bodyY);
            if (racer.stumbleFlash > 0) g2.rotate(Math.toRadians(-18 * (racer.stumbleFlash / 0.5)));

            drawLegs(g2, racer, racer.jumpTimeLeft > 0);
            drawBody(g2, racer);

            g2.setTransform(old);
        }

        private void drawLegs(Graphics2D g2, Racer racer, boolean airborne) {
            double phaseA = racer.gaitPhase;
            double phaseB = racer.gaitPhase + Math.PI;
            Color legColor = racer.species == Species.CHEETAH ? new Color(190, 130, 60) : new Color(235, 235, 230);

            g2.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(legColor);

            if (airborne) {
                drawLeg(g2, -22, 6, -40, -8, 40);
                drawLeg(g2, -8, 8, -22, -8, 40);
                drawLeg(g2, 14, 8, 30, -6, 40);
                drawLeg(g2, 28, 6, 44, -8, 40);
            } else {
                drawLeg(g2, -24, 0, swing(phaseA, -22, 24), 40, 44);
                drawLeg(g2, -10, 2, swing(phaseB, -22, 24), 40, 44);
                drawLeg(g2, 18, 2, swing(phaseB, -18, 26), 40, 44);
                drawLeg(g2, 30, 0, swing(phaseA, -18, 26), 40, 44);
            }
        }

        private double swing(double phase, double back, double front) {
            double s = Math.sin(phase);
            return back + (front - back) * (s * 0.5 + 0.5);
        }

        private void drawLeg(Graphics2D g2, int hipX, int hipY, double footX, int footY, int kneeDrop) {
            int kneeX = (int) ((hipX + footX) / 2.0);
            int kneeY = hipY + kneeDrop;
            g2.drawLine(hipX, hipY, kneeX, kneeY);
            g2.drawLine(kneeX, kneeY, (int) footX, footY);
        }

        private void drawBody(Graphics2D g2, Racer racer) {
            Color base = racer.color;
            int bw = 92, bh = 40;

            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillOval(-bw / 2 + 10, 34, bw - 20, 12);

            Path2D torso = new Path2D.Double();
            torso.moveTo(-bw / 2.0, 6);
            torso.curveTo(-bw / 2.0, -bh / 2.0, bw * 0.1, -bh / 2.0 - 6, bw / 2.0 - 10, -bh * 0.3);
            torso.curveTo(bw / 2.0 + 6, -bh * 0.1, bw / 2.0 + 6, bh * 0.15, bw / 2.0 - 6, bh * 0.25);
            torso.curveTo(0, bh * 0.35, -bw / 2.0 + 6, bh * 0.3, -bw / 2.0, 6);
            torso.closePath();

            g2.setPaint(new GradientPaint(-bw / 2f, 0, base, bw / 2f, 0, base.darker()));
            g2.fill(torso);
            g2.setColor(new Color(0, 0, 0, 90));
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(torso);

            drawPattern(g2, racer.species, bw, bh, base);

            int neckX = (int) (bw / 2.0 - 12);
            int headCx = neckX + 26;
            int headCy = (int) (-bh * 0.55);
            g2.setColor(base);
            g2.fillRoundRect(neckX, headCy, 26, 18, 10, 10);
            g2.fillOval(headCx - 8, headCy - 12, 30, 24);

            g2.setColor(racer.species == Species.CHEETAH ? new Color(30, 30, 30) : new Color(20, 20, 20));
            g2.fillOval(headCx + 12, headCy - 10, 5, 5);
            Polygon ear1 = triangle(headCx - 2, headCy - 10, 9, -14);
            Polygon ear2 = triangle(headCx + 10, headCy - 12, 9, -14);
            g2.setColor(base);
            g2.fillPolygon(ear1);
            g2.fillPolygon(ear2);

            if (racer.species == Species.CHEETAH) {
                g2.setColor(new Color(40, 30, 20));
                g2.drawLine(headCx + 12, headCy - 8, headCx + 6, headCy + 2);
                g2.drawLine(headCx + 16, headCy - 6, headCx + 12, headCy + 4);
            } else {
                g2.setColor(new Color(20, 20, 20));
                for (int i = 0; i < 4; i++) {
                    g2.fillRoundRect(headCx - 2 + i * 5, headCy - 6, 3, 16, 2, 2);
                }
            }

            double tailWag = Math.sin(racer.gaitPhase * 0.6 + Math.PI) * 14;
            g2.setColor(base.darker());
            g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(-bw / 2 + 4, 2, (int) (-bw / 2 - 22), (int) (10 + tailWag));
        }

        private Polygon triangle(int x, int y, int w, int h) {
            Polygon p = new Polygon();
            p.addPoint(x - w / 2, y);
            p.addPoint(x + w / 2, y);
            p.addPoint(x, y + h);
            return p;
        }

        private void drawPattern(Graphics2D g2, Species species, int bw, int bh, Color base) {
            if (species == Species.CHEETAH) {
                g2.setColor(new Color(60, 40, 20));
                for (int i = 0; i < 10; i++) {
                    int sx = (int) (-bw * 0.38 + i * (bw * 0.8 / 10));
                    int sy = (int) (-bh * 0.15 + (i % 3) * 8 - 6);
                    g2.fillOval(sx, sy, 6, 6);
                }
            } else {
                g2.setColor(new Color(25, 25, 25));
                for (int i = 0; i < 7; i++) {
                    int sx = (int) (-bw * 0.42 + i * (bw * 0.85 / 7));
                    Polygon stripe = new Polygon();
                    stripe.addPoint(sx, (int) (-bh * 0.4));
                    stripe.addPoint(sx + 6, (int) (-bh * 0.4));
                    stripe.addPoint(sx + 2, (int) (bh * 0.35));
                    stripe.addPoint(sx - 4, (int) (bh * 0.35));
                    g2.fillPolygon(stripe);
                }
            }
        }

        private void drawHud(Graphics2D g2, Racer racer) {
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.setColor(new Color(40, 30, 20));
            g2.drawString(racer.name, 16, 24);

            int barX = 16, barY = 32, barW = 260, barH = 8;
            g2.setColor(new Color(0, 0, 0, 50));
            g2.fillRoundRect(barX, barY, barW, barH, 6, 6);
            int fillW = (int) (barW * Math.min(1.0, racer.distance / FINISH_DISTANCE));
            g2.setColor(racer.species == Species.CHEETAH ? new Color(215, 130, 40) : new Color(80, 80, 90));
            g2.fillRoundRect(barX, barY, Math.max(0, fillW), barH, 6, 6);

            int stX = 16, stY = LANE_H - 26, stW = 150, stH = 9;
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillRoundRect(stX, stY, stW, stH, 6, 6);
            int stFill = (int) (stW * (racer.stamina / 100.0));
            g2.setColor(new Color(60, 170, 220));
            g2.fillRoundRect(stX, stY, Math.max(0, stFill), stH, 6, 6);
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.setColor(new Color(40, 30, 20));
            g2.drawString("STAMINA", stX, stY - 3);

            g2.setFont(new Font("Arial", Font.BOLD, 16));
            String txt = String.format("%.0f m / %.0f m", Math.min(racer.distance, FINISH_DISTANCE), FINISH_DISTANCE);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(txt, WIDTH - fm.stringWidth(txt) - 16, LANE_H - 16);
        }

        private void drawCountdown(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            int value = (int) Math.ceil(countdownRemaining);
            String text = value > 0 ? String.valueOf(value) : "GO!";
            float scale = 1f + 0.4f * (float) (countdownRemaining - Math.floor(countdownRemaining));

            g2.setFont(new Font("Arial Black", Font.BOLD, (int) (100 * scale)));
            FontMetrics fm = g2.getFontMetrics();
            int tx = WIDTH / 2 - fm.stringWidth(text) / 2;
            int ty = HEIGHT / 2 + fm.getAscent() / 3;

            g2.setColor(new Color(255, 200, 60));
            g2.drawString(text, tx, ty);
        }

        private void drawGoFlash(Graphics2D g2) {
            float alpha = (float) Math.max(0, Math.min(1, goFlashRemaining / 0.8));
            g2.setFont(new Font("Arial Black", Font.BOLD, 84));
            g2.setColor(new Color(80, 255, 120, (int) (alpha * 255)));
            FontMetrics fm = g2.getFontMetrics();
            String text = "GO!";
            g2.drawString(text, WIDTH / 2 - fm.stringWidth(text) / 2, HEIGHT / 2);
        }

        private void drawFinishOverlay(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            for (Particle p : confetti) p.draw(g2, 0, 0);

            String text = winner.name + " WINS THE DERBY!";
            g2.setFont(new Font("Arial Black", Font.BOLD, 42));
            FontMetrics fm = g2.getFontMetrics();
            int tx = WIDTH / 2 - fm.stringWidth(text) / 2;
            g2.setColor(new Color(255, 200, 60));
            g2.drawString(text, tx, HEIGHT / 2 - 30);

            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            String hint = "Press ENTER to race again, ESC for menu";
            fm = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(hint, WIDTH / 2 - fm.stringWidth(hint) / 2, HEIGHT / 2 + 30);
        }
    }

    enum Species { CHEETAH, ZEBRA }

    // ======================================================================
    // RACER
    // ======================================================================
    static class Racer {
        final String name;
        final Color color;
        final Color colorLight;
        final Species species;
        final boolean ai;

        double speed = 0;
        double distance = 0;
        double gaitPhase = 0;
        double jumpTimeLeft = 0;
        double stumbleFlash = 0;
        double stamina = 100;
        boolean bursting = false;
        long lastHandledHurdle = -1;
        double aiReactionOffset;

        final List<Particle> effects = new ArrayList<>();

        Racer(String name, Color color, Color colorLight, Species species, boolean ai) {
            this.name = name;
            this.color = color;
            this.colorLight = colorLight;
            this.species = species;
            this.ai = ai;
            this.aiReactionOffset = 1.5 + Math.random() * 1.5;
        }

        void reset() {
            speed = 0;
            distance = 0;
            gaitPhase = 0;
            jumpTimeLeft = 0;
            stumbleFlash = 0;
            stamina = 100;
            bursting = false;
            lastHandledHurdle = -1;
            effects.clear();
        }

        void applyInput(boolean run, boolean jumpPressed, boolean burstRequested, double dt) {
            bursting = burstRequested && stamina > 3 && jumpTimeLeft <= 0;
            double maxSpeed = GamePanel.MAX_SPEED * (bursting ? GamePanel.BURST_MULT : 1.0);
            double accel = GamePanel.ACCELERATION * (bursting ? 1.5 : 1.0);

            if (run) speed += accel * dt;
            else speed -= GamePanel.FRICTION * dt;
            speed = Math.max(0, Math.min(maxSpeed, speed));

            if (bursting) stamina = Math.max(0, stamina - dt * 40);
            else stamina = Math.min(100, stamina + dt * 12);

            if (jumpPressed && jumpTimeLeft <= 0) {
                jumpTimeLeft = GamePanel.JUMP_DURATION;
            }
        }

        void driveAI(double dt) {
            long nextIndex = (long) Math.floor((distance) / GamePanel.HURDLE_SPACING) + 1;
            double hd = GamePanel.hurdleDistance(nextIndex - 1);
            if (hd < distance) hd = GamePanel.hurdleDistance(nextIndex);
            double depthAhead = hd - distance;

            boolean jump = jumpTimeLeft <= 0 && depthAhead > 0 && depthAhead < aiReactionOffset;
            boolean burst = stamina > 60 && depthAhead > 6 && Math.random() < dt * 0.4;
            applyInput(true, jump, burst, dt);
        }

        void update(double dt) {
            distance += speed * dt;

            double cyclesPerSecond = 0.6 + (speed / GamePanel.MAX_SPEED) * 2.6;
            gaitPhase += dt * cyclesPerSecond * Math.PI * 2;
            if (gaitPhase > Math.PI * 200) gaitPhase -= Math.PI * 200;

            if (jumpTimeLeft > 0) jumpTimeLeft = Math.max(0, jumpTimeLeft - dt);

            long firstIndex = (long) Math.floor((distance - 3) / GamePanel.HURDLE_SPACING);
            for (long k = Math.max(0, firstIndex); k < firstIndex + 3; k++) {
                double hd = GamePanel.hurdleDistance(k);
                double depthAhead = hd - distance;
                if (Math.abs(depthAhead) < GamePanel.HURDLE_HALF_WIDTH && k != lastHandledHurdle) {
                    lastHandledHurdle = k;
                    if (jumpTimeLeft <= 0) {
                        speed *= GamePanel.STUMBLE_PENALTY;
                        stumbleFlash = 0.5;
                        spawnDust(14, new Color(180, 150, 100));
                    } else {
                        spawnDust(6, new Color(255, 240, 180));
                    }
                }
            }
            stumbleFlash = Math.max(0, stumbleFlash - dt);

            if (speed > 3 && jumpTimeLeft <= 0) {
                double footPhase = Math.sin(gaitPhase);
                if (footPhase > 0.94 && Math.random() < dt * 30) {
                    spawnDust(2, new Color(200, 175, 130));
                }
            }

            for (Particle p : effects) p.update(dt);
            effects.removeIf(p -> p.life <= 0);
        }

        private void spawnDust(int count, Color color) {
            for (int i = 0; i < count; i++) {
                double angle = Math.PI + (Math.random() - 0.5) * 1.2;
                double v = 20 + Math.random() * 40;
                effects.add(new Particle(-20, 30, Math.cos(angle) * v, Math.sin(angle) * v - 20, color, 4 + Math.random() * 3, 0.5));
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
            vy += 50 * dt;
            rotation += dt * 4;
            life -= dt;
        }

        void draw(Graphics2D g2, int originX, int originY) {
            float alpha = (float) Math.max(0, Math.min(1, life / maxLife));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(color);
            AffineTransform oldT = g2.getTransform();
            g2.translate(GamePanel.ANIMAL_X + originX + x, originY + y);
            g2.rotate(rotation);
            g2.fillOval((int) (-size / 2), (int) (-size / 2), (int) size, (int) size);
            g2.setTransform(oldT);
            g2.setComposite(old);
        }
    }
}