import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;


public class GlobalRacingTourGame extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GlobalRacingTourGame().setVisible(true));
    }

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    public GlobalRacingTourGame() {
        setTitle("Global Racing Tour");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setResizable(false);

        root.add(new MenuPanel(this), "menu");
        setContentPane(root);
    }

    public void startRace(boolean vsComputer, String track, int laps) {
        RacePanel race = new RacePanel(this, vsComputer, track, laps);
        root.add(race, "race");
        cards.show(root, "race");
        race.requestFocusInWindow();
        race.begin();
    }

    public void showMenu() {
        cards.show(root, "menu");
    }

    // =========================================================================
    // MENU
    // =========================================================================
    static class MenuPanel extends JPanel {
        MenuPanel(GlobalRacingTourGame frame) {
            setLayout(new GridBagLayout());
            setBackground(new Color(15, 20, 40));

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.insets = new Insets(8, 8, 8, 8); c.fill = GridBagConstraints.HORIZONTAL;

            JLabel title = new JLabel("GLOBAL RACING TOUR");
            title.setFont(new Font("SansSerif", Font.BOLD, 42));
            title.setForeground(new Color(255, 210, 80));
            c.gridy = 0; add(title, c);

            JLabel sub = new JLabel("Choose your race", SwingConstants.CENTER);
            sub.setFont(new Font("SansSerif", Font.PLAIN, 18));
            sub.setForeground(Color.LIGHT_GRAY);
            c.gridy = 1; add(sub, c);

            JComboBox<String> trackBox = new JComboBox<>(new String[]{"Tokyo", "Paris", "New York"});
            JComboBox<Integer> lapsBox = new JComboBox<>(new Integer[]{2, 3, 5, 8});
            lapsBox.setSelectedItem(3);

            JPanel opts = new JPanel(new GridLayout(2, 2, 10, 10));
            opts.setOpaque(false);
            opts.add(labeled("Track", trackBox));
            opts.add(labeled("Laps", lapsBox));
            c.gridy = 2; add(opts, c);

            JButton twoP = bigButton("2 Players");
            JButton vsAi = bigButton("Vs Computer");
            c.gridy = 3; add(twoP, c);
            c.gridy = 4; add(vsAi, c);

            JLabel help = new JLabel(
                "<html><div style='text-align:center;color:#aaa'>P1: W A S D + Shift (boost) &nbsp;&nbsp; " +
                "P2: Arrows + Right Ctrl (boost)</div></html>", SwingConstants.CENTER);
            c.gridy = 5; add(help, c);

            twoP.addActionListener(e -> frame.startRace(false,
                (String) trackBox.getSelectedItem(), (Integer) lapsBox.getSelectedItem()));
            vsAi.addActionListener(e -> frame.startRace(true,
                (String) trackBox.getSelectedItem(), (Integer) lapsBox.getSelectedItem()));
        }

        private JPanel labeled(String text, JComponent comp) {
            JPanel p = new JPanel(new BorderLayout(6, 6));
            p.setOpaque(false);
            JLabel l = new JLabel(text);
            l.setForeground(Color.WHITE);
            p.add(l, BorderLayout.NORTH);
            p.add(comp, BorderLayout.CENTER);
            return p;
        }

        private JButton bigButton(String text) {
            JButton b = new JButton(text);
            b.setFont(new Font("SansSerif", Font.BOLD, 22));
            b.setForeground(new Color(20, 20, 20));
            b.setBackground(new Color(255, 210, 80));
            b.setFocusPainted(false);
            b.setPreferredSize(new Dimension(320, 60));
            return b;
        }
    }

    // =========================================================================
    // TRACK
    // =========================================================================
    static class Track {
        // Oval racing line, defined by center + radii.
        final double cx, cy, rx, ry;      // outer racing line center/radii
        final double laneWidth = 90;      // track width
        final Color asphalt = new Color(48, 48, 55);
        final Color kerb1 = new Color(220, 60, 60);
        final Color kerb2 = new Color(240, 240, 240);
        final String name;

        Track(String name, int w, int h) {
            this.name = name;
            this.cx = w / 2.0;
            this.cy = h / 2.0 + 30;
            this.rx = w * 0.36;
            this.ry = h * 0.28;
        }

        /** Racing-line point at angle t (radians, 0 = right, CCW). */
        Point2D.Double point(double t) {
            return new Point2D.Double(cx + rx * Math.cos(t), cy + ry * Math.sin(t));
        }

        /** Tangent angle at parameter t (direction of travel, CCW). */
        double tangent(double t) {
            double dx = -rx * Math.sin(t);
            double dy =  ry * Math.cos(t);
            return Math.atan2(dy, dx);
        }

        void drawBackground(Graphics2D g, int w, int h, double scroll) {
            // Sky gradient per track.
            Color top, bot;
            switch (name) {
                case "Tokyo":     top = new Color(20, 20, 60);  bot = new Color(220, 90, 140); break;
                case "Paris":     top = new Color(90, 130, 200); bot = new Color(250, 220, 190); break;
                default:          top = new Color(40, 60, 110); bot = new Color(180, 200, 230); break; // NY
            }
            g.setPaint(new GradientPaint(0, 0, top, 0, h, bot));
            g.fillRect(0, 0, w, h);

            // Distant skyline silhouette (parallax).
            g.setColor(new Color(0, 0, 0, 130));
            int base = (int)(h * 0.45);
            Random rnd = new Random(name.hashCode());
            int x = -((int) scroll % 40);
            while (x < w) {
                int bw = 20 + rnd.nextInt(50);
                int bh = 30 + rnd.nextInt(140);
                g.fillRect(x, base - bh, bw, bh);
                if (name.equals("Paris") && rnd.nextInt(8) == 0) {
                    g.fillPolygon(new int[]{x, x + bw / 2, x + bw},
                                  new int[]{base - bh, base - bh - 40, base - bh}, 3);
                }
                x += bw + 4;
            }
            // Grass / ground.
            g.setColor(new Color(30, 90, 40));
            g.fillRect(0, base, w, h - base);
        }

        void drawTrack(Graphics2D g) {
            // Asphalt band = outer ellipse minus inner ellipse.
            Area outer = new Area(new Ellipse2D.Double(cx - rx - laneWidth/2, cy - ry - laneWidth/2,
                    2*rx + laneWidth, 2*ry + laneWidth));
            Area inner = new Area(new Ellipse2D.Double(cx - rx + laneWidth/2, cy - ry + laneWidth/2,
                    2*rx - laneWidth, 2*ry - laneWidth));
            outer.subtract(inner);
            g.setColor(asphalt);
            g.fill(outer);

            // Kerbs (dashed).
            Stroke old = g.getStroke();
            g.setStroke(new BasicStroke(6, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{18f, 18f}, 0f));
            g.setColor(kerb1);
            g.draw(new Ellipse2D.Double(cx - rx - laneWidth/2 + 3, cy - ry - laneWidth/2 + 3,
                    2*rx + laneWidth - 6, 2*ry + laneWidth - 6));
            g.setStroke(new BasicStroke(6, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{18f, 18f}, 18f));
            g.setColor(kerb2);
            g.draw(new Ellipse2D.Double(cx - rx + laneWidth/2 - 3, cy - ry + laneWidth/2 - 3,
                    2*rx - laneWidth + 6, 2*ry - laneWidth + 6));

            // Center dashed lane line.
            g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{20f, 20f}, 0f));
            g.setColor(new Color(255, 255, 255, 180));
            g.draw(new Ellipse2D.Double(cx - rx, cy - ry, 2*rx, 2*ry));
            g.setStroke(old);

            // Start / finish line at t = -PI/2 (top of oval).
            Point2D.Double p = point(-Math.PI / 2);
            double tan = tangent(-Math.PI / 2);
            double nx = Math.cos(tan + Math.PI / 2), ny = Math.sin(tan + Math.PI / 2);
            AffineTransform save = g.getTransform();
            g.translate(p.x, p.y);
            g.rotate(Math.atan2(ny, nx));
            // Checkerboard.
            int cols = 10, rows = 2, cw = 9;
            for (int r = 0; r < rows; r++)
                for (int cc = 0; cc < cols; cc++) {
                    g.setColor(((r + cc) & 1) == 0 ? Color.WHITE : Color.BLACK);
                    g.fillRect(-cols * cw / 2 + cc * cw, -rows * cw / 2 + r * cw, cw, cw);
                }
            g.setTransform(save);
        }
    }

    // =========================================================================
    // CAR
    // =========================================================================
    static class Car {
        double x, y;            // world position
        double heading;         // radians
        double speed;           // px / second
        double maxSpeed = 320;
        double accel = 220;
        double brake = 320;
        double friction = 90;
        double turnRate = 2.4;  // rad/s at low speed
        double boost = 100;     // 0..100
        boolean boosting = false;
        Color body;
        String name;
        int lap = 0;
        double lastAngle;       // last angular position on track
        double progress = 0;    // cumulative angular progress (unwrapped)
        boolean finished = false;
        double wheelSpin = 0;
        double bounce = 0;

        Car(String name, Color body) { this.name = name; this.body = body; }

        void update(double dt, boolean up, boolean down, boolean left, boolean right, boolean boostKey) {
            // Boost.
            if (boostKey && boost > 5 && up) {
                boosting = true;
                boost -= 40 * dt;
            } else {
                boosting = false;
                boost = Math.min(100, boost + 12 * dt);
            }
            double top = boosting ? maxSpeed * 1.55 : maxSpeed;

            if (up)         speed += (boosting ? accel * 1.8 : accel) * dt;
            else if (down)  speed -= brake * dt;
            else            speed -= Math.signum(speed) * friction * dt;

            speed = Math.max(-maxSpeed * 0.4, Math.min(top, speed));

            // Steering scales with speed (harder to turn at low speed).
            double steerFactor = Math.min(1.0, Math.abs(speed) / 60.0);
            double turn = turnRate * steerFactor;
            if (left)  heading -= turn * dt * Math.signum(speed == 0 ? 1 : speed);
            if (right) heading += turn * dt * Math.signum(speed == 0 ? 1 : speed);

            x += Math.cos(heading) * speed * dt;
            y += Math.sin(heading) * speed * dt;

            wheelSpin += speed * dt * 0.08;
            bounce = Math.sin(System.nanoTime() / 60_000_000.0) * Math.min(1.5, Math.abs(speed) / 200.0);
        }

        void draw(Graphics2D g) {
            AffineTransform save = g.getTransform();
            g.translate(x, y);
            g.rotate(heading);
            g.translate(0, bounce);

            // Shadow.
            g.setColor(new Color(0, 0, 0, 90));
            g.fill(new Ellipse2D.Double(-22, -12, 44, 24));

            // Body.
            g.setColor(body);
            g.fillRoundRect(-20, -10, 40, 20, 8, 8);
            // Hood accent.
            g.setColor(body.darker());
            g.fillRoundRect(-20, -10, 12, 20, 6, 6);
            // Windshield.
            g.setColor(new Color(90, 180, 220, 220));
            g.fillRoundRect(-4, -8, 12, 16, 4, 4);
            // Racing stripe.
            g.setColor(Color.WHITE);
            g.fillRect(-18, -2, 36, 2);

            // Wheels (rotate with wheelSpin).
            drawWheel(g, -12, -12);
            drawWheel(g,  12, -12);
            drawWheel(g, -12,  10);
            drawWheel(g,  12,  10);

            g.setTransform(save);
        }

        private void drawWheel(Graphics2D g, double wx, double wy) {
            AffineTransform s = g.getTransform();
            g.translate(wx, wy);
            g.rotate(wheelSpin);
            g.setColor(Color.BLACK);
            g.fillRoundRect(-5, -3, 10, 6, 3, 3);
            g.setColor(new Color(200, 200, 200));
            g.fillRect(-4, -1, 8, 2);
            g.setTransform(s);
        }
    }

    // =========================================================================
    // PARTICLE (dust / boost flames)
    // =========================================================================
    static class Particle {
        double x, y, vx, vy, life, maxLife, size;
        Color color;
        Particle(double x, double y, double vx, double vy, double life, double size, Color c) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.life = this.maxLife = life; this.size = size; this.color = c;
        }
        void update(double dt) {
            x += vx * dt; y += vy * dt;
            vx *= 0.94; vy *= 0.94;
            life -= dt;
        }
        void draw(Graphics2D g) {
            float a = (float) Math.max(0, life / maxLife);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int)(a * color.getAlpha())));
            double s = size * (0.6 + 0.4 * a);
            g.fill(new Ellipse2D.Double(x - s/2, y - s/2, s, s));
        }
    }

    // =========================================================================
    // RACE PANEL
    // =========================================================================
    static class RacePanel extends JPanel implements ActionListener {
        final GlobalRacingTourGame frame;
        final boolean vsComputer;
        final int totalLaps;
        final Track track;
        final Car p1, p2;
        final List<Particle> particles = new ArrayList<>();
        final javax.swing.Timer timer;
        final Set<Integer> keys = new HashSet<>();
        long lastNanos;
        double bgScroll = 0;
        int countdown = 3;       // 3,2,1,0(GO),-1(racing)
        double countdownTimer = 1.0;
        boolean raceOver = false;
        String winner = "";
        JButton menuBtn, againBtn;

        RacePanel(GlobalRacingTourGame frame, boolean vsComputer, String trackName, int laps) {
            this.frame = frame;
            this.vsComputer = vsComputer;
            this.totalLaps = laps;
            setPreferredSize(new Dimension(1100, 720));
            setBackground(Color.BLACK);
            setFocusable(true);
            setLayout(null);

            track = new Track(trackName, 1100, 720);

            p1 = new Car(vsComputer ? "YOU" : "P1", new Color(230, 60, 60));
            p2 = new Car(vsComputer ? "CPU" : "P2", new Color(70, 140, 240));

            // Place cars at start line (top of oval), heading CCW (tangent).
            Point2D.Double sp = track.point(-Math.PI / 2);
            double h0 = track.tangent(-Math.PI / 2);
            p1.x = sp.x - 18; p1.y = sp.y + 22; p1.heading = h0;
            p2.x = sp.x + 18; p2.y = sp.y + 22; p2.heading = h0;
            p1.lastAngle = p2.lastAngle = -Math.PI / 2;

            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e)  { keys.add(e.getKeyCode()); }
                @Override public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
            });

            timer = new javax.swing.Timer(1000 / 60, this);

            menuBtn = new JButton("Menu");
            menuBtn.setBounds(20, 20, 90, 32);
            menuBtn.setFocusable(false);
            menuBtn.addActionListener(e -> { timer.stop(); frame.showMenu(); });
            add(menuBtn);

            againBtn = new JButton("Play Again");
            againBtn.setBounds(490, 400, 140, 44);
            againBtn.setFocusable(false);
            againBtn.setVisible(false);
            againBtn.addActionListener(e -> { timer.stop(); frame.showMenu(); });
            add(againBtn);
        }

        void begin() { lastNanos = System.nanoTime(); timer.start(); }

        // ---- game loop ----
        @Override public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            double dt = Math.min(0.05, (now - lastNanos) / 1_000_000_000.0);
            lastNanos = now;

            if (countdown >= 0) {
                countdownTimer -= dt;
                if (countdownTimer <= 0) { countdown--; countdownTimer = 1.0; }
            } else if (!raceOver) {
                updateCar(p1, dt, true);
                updateCar(p2, dt, false);
                trackProgress(p1);
                trackProgress(p2);
                checkWin();
            }

            // Particles.
            for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
                Particle pt = it.next(); pt.update(dt);
                if (pt.life <= 0) it.remove();
            }
            spawnTrail(p1);
            spawnTrail(p2);

            bgScroll += (Math.abs(p1.speed) + Math.abs(p2.speed)) * dt * 0.05;

            repaint();
        }

        private void updateCar(Car c, double dt, boolean isP1) {
            if (c.finished) { c.speed *= 0.96; c.x += Math.cos(c.heading) * c.speed * dt;
                c.y += Math.sin(c.heading) * c.speed * dt; return; }

            boolean up, down, left, right, boost;
            if (isP1) {
                up    = keys.contains(KeyEvent.VK_W);
                down  = keys.contains(KeyEvent.VK_S);
                left  = keys.contains(KeyEvent.VK_A);
                right = keys.contains(KeyEvent.VK_D);
                boost = keys.contains(KeyEvent.VK_SHIFT);
            } else if (!vsComputer) {
                up    = keys.contains(KeyEvent.VK_UP);
                down  = keys.contains(KeyEvent.VK_DOWN);
                left  = keys.contains(KeyEvent.VK_LEFT);
                right = keys.contains(KeyEvent.VK_RIGHT);
                boost = keys.contains(KeyEvent.VK_CONTROL);
            } else {
                // AI
                double[] ai = aiControls(c);
                up = ai[0] > 0.5; down = ai[1] > 0.5; left = ai[2] > 0.5;
                right = ai[3] > 0.5; boost = ai[4] > 0.5;
            }
            c.update(dt, up, down, left, right, boost);
        }

        /** Simple AI: target a point slightly ahead on the racing line, steer & throttle. */
        private double[] aiControls(Car c) {
            double ang = Math.atan2(c.y - track.cy, (c.x - track.cx) * track.ry / track.rx);
            double lookAhead = 0.35;                       // radians ahead
            Point2D.Double target = track.point(ang + lookAhead);
            double desired = Math.atan2(target.y - c.y, target.x - c.x);
            double diff = normalize(desired - c.heading);

            boolean left  = diff < -0.05;
            boolean right = diff >  0.05;
            boolean up = true;
            boolean down = false;
            // Slow down slightly on tight corner correction.
            if (Math.abs(diff) > 0.6 && c.speed > c.maxSpeed * 0.7) { up = false; down = true; }
            // Boost on straights (small heading correction) when boost available.
            boolean boost = Math.abs(diff) < 0.15 && c.boost > 40;
            return new double[]{ up ? 1 : 0, down ? 1 : 0, left ? 1 : 0, right ? 1 : 0, boost ? 1 : 0 };
        }

        private double normalize(double a) {
            while (a >  Math.PI) a -= 2 * Math.PI;
            while (a < -Math.PI) a += 2 * Math.PI;
            return a;
        }

        private void trackProgress(Car c) {
            double ang = Math.atan2(c.y - track.cy, (c.x - track.cx) * track.ry / track.rx);
            double delta = normalize(ang - c.lastAngle);
            // CCW motion increases angle; oval param uses cos/sin so CCW = positive delta.
            c.progress += delta;
            c.lastAngle = ang;
            int newLap = (int) Math.floor(c.progress / (2 * Math.PI));
            if (newLap > c.lap) c.lap = newLap;
        }

        private void checkWin() {
            if (!p1.finished && p1.lap >= totalLaps) { p1.finished = true; }
            if (!p2.finished && p2.lap >= totalLaps) { p2.finished = true; }
            if (p1.finished || p2.finished) {
                if (p1.finished && !p2.finished) { raceOver = true; winner = p1.name + " wins!"; }
                else if (p2.finished && !p1.finished) { raceOver = true; winner = p2.name + " wins!"; }
                else if (p1.finished && p2.finished) { raceOver = true; winner = "Photo finish!"; }
                if (raceOver) againBtn.setVisible(true);
            }
        }

        private void spawnTrail(Car c) {
            if (Math.abs(c.speed) < 20) return;
            double back = c.heading + Math.PI;
            double bx = c.x + Math.cos(back) * 18;
            double by = c.y + Math.sin(back) * 18;
            if (c.boosting) {
                for (int i = 0; i < 3; i++) {
                    particles.add(new Particle(bx, by,
                            Math.cos(back) * 120 + rand(-30, 30),
                            Math.sin(back) * 120 + rand(-30, 30),
                            0.35, 10, new Color(255, 160, 40, 220)));
                }
            } else {
                particles.add(new Particle(bx, by,
                        Math.cos(back) * 40 + rand(-20, 20),
                        Math.sin(back) * 40 + rand(-20, 20),
                        0.6, 8, new Color(200, 200, 200, 140)));
            }
        }
        private double rand(double a, double b) { return a + Math.random() * (b - a); }

        // ---- draw ----
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            track.drawBackground(g, getWidth(), getHeight(), bgScroll);
            track.drawTrack(g);

            for (Particle p : particles) p.draw(g);

            p1.draw(g);
            p2.draw(g);

            drawHUD(g);

            if (countdown >= 0) drawCountdown(g);
            if (raceOver) drawWinner(g);
        }

        private void drawHUD(Graphics2D g) {
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRoundRect(130, 15, 380, 44, 12, 12);
            g.setColor(Color.WHITE);
            g.drawString(p1.name + "  Lap " + Math.min(p1.lap + 1, totalLaps) + "/" + totalLaps, 145, 40);
            g.drawString(p2.name + "  Lap " + Math.min(p2.lap + 1, totalLaps) + "/" + totalLaps, 330, 40);

            // Position.
            String pos = (p1.progress >= p2.progress)
                    ? p1.name + " 1st  •  " + p2.name + " 2nd"
                    : p2.name + " 1st  •  " + p1.name + " 2nd";
            g.setColor(new Color(255, 220, 90));
            g.drawString(pos, getWidth() - 260, 40);

            // Speed + boost meters, bottom-left / right.
            drawMeters(g, 20, getHeight() - 90, p1);
            drawMeters(g, getWidth() - 220, getHeight() - 90, p2);

            // Track label.
            g.setColor(new Color(255, 255, 255, 180));
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("Track: " + track.name, getWidth() - 160, getHeight() - 15);
        }

        private void drawMeters(Graphics2D g, int x, int y, Car c) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRoundRect(x, y, 200, 70, 10, 10);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString(c.name, x + 10, y + 18);
            // Speed bar
            int sw = (int) (180 * Math.min(1.0, Math.abs(c.speed) / (c.maxSpeed * 1.55)));
            g.setColor(new Color(80, 80, 80));
            g.fillRoundRect(x + 10, y + 26, 180, 10, 6, 6);
            g.setColor(c.body);
            g.fillRoundRect(x + 10, y + 26, sw, 10, 6, 6);
            // Boost bar
            g.setColor(new Color(80, 80, 80));
            g.fillRoundRect(x + 10, y + 46, 180, 10, 6, 6);
            g.setColor(new Color(255, 170, 40));
            g.fillRoundRect(x + 10, y + 46, (int)(180 * c.boost / 100.0), 10, 6, 6);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString((int) Math.abs(c.speed) + " px/s", x + 150, y + 24);
        }

        private void drawCountdown(Graphics2D g) {
            String s = countdown == 0 ? "GO!" : String.valueOf(countdown);
            g.setFont(new Font("SansSerif", Font.BOLD, 140));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(s);
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRoundRect(getWidth()/2 - w/2 - 30, getHeight()/2 - 100, w + 60, 160, 20, 20);
            g.setColor(countdown == 0 ? new Color(120, 240, 120) : new Color(255, 220, 80));
            g.drawString(s, getWidth()/2 - w/2, getHeight()/2 + 30);
        }

        private void drawWinner(Graphics2D g) {
            g.setColor(new Color(0, 0, 0, 170));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(255, 220, 80));
            g.setFont(new Font("SansSerif", Font.BOLD, 60));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(winner);
            g.drawString(winner, getWidth()/2 - w/2, getHeight()/2 - 20);
            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            g.setColor(Color.WHITE);
            String sub = "Track: " + track.name + "   •   Laps: " + totalLaps;
            int w2 = g.getFontMetrics().stringWidth(sub);
            g.drawString(sub, getWidth()/2 - w2/2, getHeight()/2 + 20);
        }
    }
}
