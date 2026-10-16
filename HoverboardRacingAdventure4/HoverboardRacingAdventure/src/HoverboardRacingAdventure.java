import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class HoverboardRacingAdventure extends JFrame {

    public HoverboardRacingAdventure() {
        setTitle("Hoverboard Racing Adventure");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HoverboardRacingAdventure::new);
    }

    // ============================================================= GAME PANEL
    class GamePanel extends JPanel implements ActionListener, KeyListener {

        static final int W = 1100, H = 720;
        static final int TOTAL_LAPS = 3;
        static final int CHECKPOINTS = 16;

        // Track geometry (ellipse)
        double cx = W / 2.0, cy = H / 2.0;
        double rx = 400, ry = 250;
        double roadW = 96;

        enum State { MENU, COUNTDOWN, RACING, FINISHED }
        State state = State.MENU;

        Racer p1, p2;
        boolean vsComputer = false;
        int countdownTick = 0;
        long stateTimer = 0;
        String winnerText = "";
        int menuPulse = 0;

        ArrayList<Coin> coins = new ArrayList<>();
        ArrayList<BoostPad> boosts = new ArrayList<>();
        ArrayList<Particle> particles = new ArrayList<>();
        ArrayList<Star> stars = new ArrayList<>();
        Random rnd = new Random();

        public GamePanel() {
            setPreferredSize(new Dimension(W, H));
            setFocusable(true);
            addKeyListener(this);
            for (int i = 0; i < 90; i++)
                stars.add(new Star(rnd.nextDouble() * W, rnd.nextDouble() * H,
                        rnd.nextDouble() * 2 + 0.5, rnd.nextDouble()));
            new Timer(16, this).start();
        }

        // ---------------------------------------------------------- setup
        void setupRace(boolean computer) {
            vsComputer = computer;
            particles.clear();
            coins.clear();
            boosts.clear();

            p1 = new Racer("PLAYER 1", new Color(0, 210, 255), 130, 70);
            p2 = new Racer(computer ? "COMPUTER" : "PLAYER 2",
                    new Color(255, 60, 140), 130, 110);

            // both start near the start line (angle = -PI/2 -> top of ellipse)
            placeOnGrid(p1, -6);
            placeOnGrid(p2, -14);
            if (computer) p2.isAI = true;

            // coins along track
            for (int i = 0; i < 10; i++) {
                double a = (Math.PI * 2 / 10) * i + 0.31;
                double rr = 0.55 + rnd.nextDouble() * 0.3;
                coins.add(new Coin(cx + rx * rr * Math.cos(a), cy + ry * rr * Math.sin(a)));
            }
            // boost pads
            double[] ba = { 0.05, Math.PI * 0.62, Math.PI * 1.18, Math.PI * 1.72 };
            for (double a : ba)
                boosts.add(new BoostPad(cx + rx * Math.cos(a), cy + ry * Math.sin(a)));

            state = State.COUNTDOWN;
            countdownTick = 180; // ~3 seconds
            stateTimer = System.currentTimeMillis();
        }

        void placeOnGrid(Racer r, double angleDeg) {
            double a = -Math.PI / 2 + Math.toRadians(angleDeg);
            r.x = cx + rx * Math.cos(a);
            r.y = cy + ry * Math.sin(a);
            // face along tangent
            r.angle = Math.atan2(ry * Math.cos(a), -rx * Math.sin(a));
            r.speed = 0;
        }

        // ---------------------------------------------------------- game loop
        public void actionPerformed(ActionEvent e) {
            menuPulse++;
            if (state == State.COUNTDOWN) {
                countdownTick--;
                if (countdownTick <= 0) state = State.RACING;
            } else if (state == State.RACING) {
                updateRacer(p1);
                updateRacer(p2);
                handleCollision();
                updateCoinsAndBoosts(p1);
                updateCoinsAndBoosts(p2);
                checkWinner();
            }
            // particles always animate (nice on menu / finish too)
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle pt = particles.get(i);
                pt.update();
                if (pt.life <= 0) particles.remove(i);
            }
            for (Star s : stars) s.twinkle();
            repaint();
        }

        void updateRacer(Racer r) {
            boolean up, left, right, down;

            if (r.isAI) {
                // AI: aim at a checkpoint a bit ahead of its current target
                int look = (r.nextCp + 2) % CHECKPOINTS;
                double a = cpAngle(look);
                double tx = cx + rx * Math.cos(a);
                double ty = cy + ry * Math.sin(a);
                double want = Math.atan2(ty - r.y, tx - r.x);
                double diff = angleDiff(r.angle, want);
                left = diff < -0.06;
                right = diff > 0.06;
                up = Math.abs(diff) < 1.2;
                down = false;
                // gentle rubber-band: catch up slightly if far behind
                if (progressOf(r) + 1.2 < Math.max(progressOf(p1), progressOf(p2))) r.speed += 0.02;
            } else if (r == p1) {
                up = keys[KeyEvent.VK_W];
                left = keys[KeyEvent.VK_A];
                right = keys[KeyEvent.VK_D];
                down = keys[KeyEvent.VK_S];
            } else {
                up = keys[KeyEvent.VK_UP];
                left = keys[KeyEvent.VK_LEFT];
                right = keys[KeyEvent.VK_RIGHT];
                down = keys[KeyEvent.VK_DOWN];
            }

            // steering (turning is easier at speed)
            double steerPower = 0.09 * Math.min(1, Math.abs(r.speed) / 2 + 0.35);
            if (left) { r.angle -= steerPower; r.tilt = Math.max(-1, r.tilt - 0.15); }
            if (right) { r.angle += steerPower; r.tilt = Math.min(1, r.tilt + 0.15); }
            r.tilt *= 0.90; // ease tilt back

            // throttle / brake / reverse
            if (up) r.speed += 0.16;
            if (down) r.speed -= 0.14;
            r.speed *= 0.985;          // drag
            r.speed = clamp(r.speed, -2.2, 6.2 + (r.boostTimer > 0 ? 2.2 : 0));

            // hoverboard bobbing
            r.bob += 0.18 + Math.abs(r.speed) * 0.03;

            double nx = r.x + Math.cos(r.angle) * r.speed;
            double ny = r.y + Math.sin(r.angle) * r.speed;

            // off-track slowdown (grass)
            double d = distToCenter(nx, ny);
            double ideal = ellipseRadiusAt(Math.atan2((ny - cy) / ry, (nx - cx) / rx));
            double off = Math.abs(Math.hypot(nx - cx, ny - cy) - ideal);
            if (off > roadW / 2) {
                r.speed *= 0.94; // grass slows you down
                if (rnd.nextInt(6) == 0)
                    spawnParticle(nx, ny, new Color(80, 200, 90), 2, 1.5, 20);
            }

            r.x = clamp(nx, 20, W - 20);
            r.y = clamp(ny, 20, H - 20);

            // boost timer
            if (r.boostTimer > 0) r.boostTimer--;

            // trail particles
            if (Math.abs(r.speed) > 1.5 && rnd.nextInt(2) == 0) {
                Color trail = r.boostTimer > 0 ? Color.ORANGE : r.color;
                spawnParticle(r.x - Math.cos(r.angle) * 18,
                        r.y - Math.sin(r.angle) * 18,
                        trail, 3.2, r.boostTimer > 0 ? 3 : 1.8, 26);
            }

            // checkpoints / laps
            int target = r.nextCp % CHECKPOINTS;
            double ta = cpAngle(target);
            double txx = cx + rx * Math.cos(ta), tyy = cy + ry * Math.sin(ta);
            if (Math.hypot(r.x - txx, r.y - tyy) < 70) {
                r.nextCp++;
                if (r.nextCp % CHECKPOINTS == 0) {
                    r.lap++;
                    burst(r.x, r.y, r.color, 26);
                    if (r.lap <= TOTAL_LAPS) r.flash = 30;
                }
            }
        }

        void handleCollision() {
            double dx = p2.x - p1.x, dy = p2.y - p1.y;
            double d = Math.hypot(dx, dy);
            if (d < 34 && d > 0.01) {
                double push = (34 - d) / 2;
                double ux = dx / d, uy = dy / d;
                p1.x -= ux * push; p1.y -= uy * push;
                p2.x += ux * push; p2.y += uy * push;
                double tmp = p1.speed; p1.speed = p2.speed * 0.9; p2.speed = tmp * 0.9;
                for (int i = 0; i < 8; i++)
                    spawnParticle((p1.x + p2.x) / 2, (p1.y + p2.y) / 2,
                            Color.WHITE, 3, 2.2, 18);
            }
        }

        void updateCoinsAndBoosts(Racer r) {
            for (Coin c : coins) {
                c.spin += 0.08;
                if (!c.taken && Math.hypot(r.x - c.x, r.y - c.y) < 30) {
                    c.taken = true;
                    c.respawn = 300;
                    r.score += 10;
                    burst(c.x, c.y, new Color(255, 215, 0), 14);
                }
            }
            for (Coin c : coins) {
                if (c.taken && --c.respawn <= 0) { c.taken = false; burst(c.x, c.y, Color.YELLOW, 8); }
            }
            for (BoostPad b : boosts) {
                b.pulse += 0.1;
                if (Math.hypot(r.x - b.x, r.y - b.y) < 34 && r.boostTimer <= 0) {
                    r.boostTimer = 65;
                    r.speed = Math.max(r.speed, 7.6);
                    burst(r.x, r.y, new Color(120, 255, 180), 18);
                }
            }
        }

        void checkWinner() {
            if (p1.lap > TOTAL_LAPS || p2.lap > TOTAL_LAPS) {
                state = State.FINISHED;
                Racer w = p1.lap > p2.lap ? p1 : p2;
                winnerText = w.name + " WINS!  Score: " + w.score;
                burst(w.x, w.y, w.color, 80);
                burst(W / 2.0, H / 2.0, Color.ORANGE, 120);
            }
        }

        // ---------------------------------------------------------- helpers
        double cpAngle(int i) { return (Math.PI * 2 / CHECKPOINTS) * i; }

        double progressOf(Racer r) {
            int target = r.nextCp % CHECKPOINTS;
            double ta = cpAngle(target);
            double tx = cx + rx * Math.cos(ta), ty = cy + ry * Math.sin(ta);
            double frac = 1 - Math.min(1, Math.hypot(r.x - tx, r.y - ty) / 400.0);
            return (r.lap - 1) * CHECKPOINTS + (r.nextCp % CHECKPOINTS) + frac;
        }

        double distToCenter(double x, double y) { return Math.hypot(x - cx, y - cy); }

        double ellipseRadiusAt(double a) {
            // distance from center to ellipse point
            return Math.hypot(rx * Math.cos(a), ry * Math.sin(a));
        }

        double angleDiff(double from, double to) {
            double d = to - from;
            while (d > Math.PI) d -= Math.PI * 2;
            while (d < -Math.PI) d += Math.PI * 2;
            return d;
        }

        double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

        void spawnParticle(double x, double y, Color c, double size, double sp, int life) {
            if (particles.size() > 700) return;
            double a = rnd.nextDouble() * Math.PI * 2;
            particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp - 0.3,
                    size, life, c));
        }

        void burst(double x, double y, Color c, int n) {
            for (int i = 0; i < n; i++) {
                double a = rnd.nextDouble() * Math.PI * 2;
                double sp = rnd.nextDouble() * 4 + 1;
                particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp,
                        rnd.nextDouble() * 3 + 2, 40 + rnd.nextInt(25), c));
            }
        }

        // ---------------------------------------------------------- rendering
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawBackground(g2);
            drawTrack(g2);
            for (Coin c : coins) c.draw(g2);
            for (BoostPad b : boosts) b.draw(g2);

            if (state != State.MENU && p1 != null) {
                // draw trailing player first so leader is on top
                Racer first = progressOf(p1) >= progressOf(p2) ? p2 : p1;
                Racer second = first == p1 ? p2 : p1;
                first.draw(g2);
                second.draw(g2);
            }

            for (Particle p : particles) p.draw(g2);

            if (state == State.MENU) drawMenu(g2);
            else {
                drawHUD(g2);
                if (state == State.COUNTDOWN) drawCountdown(g2);
                if (state == State.FINISHED) drawFinish(g2);
            }
        }

        void drawBackground(Graphics2D g2) {
            GradientPaint sky = new GradientPaint(0, 0, new Color(18, 12, 48),
                    0, H, new Color(60, 20, 90));
            g2.setPaint(sky);
            g2.fillRect(0, 0, W, H);

            // twinkling stars
            for (Star s : stars) {
                g2.setColor(new Color(255, 255, 255, (int) (120 + 120 * s.brightness)));
                g2.fill(new Ellipse2D.Double(s.x, s.y, s.size, s.size));
            }
            // aurora-like glow blobs
            float pulse = (float) (Math.sin(menuPulse * 0.01) * 0.5 + 0.5);
            RadialGradientPaint glow = new RadialGradientPaint(
                    new Point2D.Float(W * 0.5f, H * 0.35f), 420,
                    new float[]{0f, 1f},
                    new Color[]{new Color(0.4f, 0.1f, 0.8f, 0.22f + 0.1f * pulse), new Color(0, 0, 0, 0)});
            g2.setPaint(glow);
            g2.fillRect(0, 0, W, H);
        }

        void drawTrack(Graphics2D g2) {
            // grass
            g2.setColor(new Color(28, 90, 50));
            g2.fillRect(0, 0, W, H);

            // colorful checkerboard grass shimmer
            g2.setColor(new Color(34, 105, 58, 90));
            for (int i = 0; i < 14; i++)
                g2.fill(new Ellipse2D.Double(
                        (i * 173 + menuPulse * 0.1) % W - 60,
                        (i * 97) % H, 120, 60));

            // road
            g2.setStroke(new BasicStroke((float) roadW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(52, 55, 75));
            g2.draw(trackPath());

            // glowing rainbow edge
            g2.setStroke(new BasicStroke((float) roadW + 10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float hue = (menuPulse * 0.002f) % 1f;
            g2.setColor(Color.getHSBColor(hue, 0.75f, 0.95f));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.10f));
            g2.draw(trackPath());
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

            // edge lines
            for (double off : new double[]{-roadW / 2, roadW / 2}) {
                g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(255, 255, 255, 160));
                g2.draw(offsetPath(off));
            }

            // animated center dashed line
            g2.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    0, new float[]{16, 22}, menuPulse * 0.8f));
            g2.setColor(new Color(255, 230, 90, 180));
            g2.draw(trackPath());

            // start/finish banner
            double sx = cx + rx * Math.cos(-Math.PI / 2), sy = cy + ry * Math.sin(-Math.PI / 2);
            double tangent = Math.atan2(ry * Math.cos(-Math.PI / 2), -rx * Math.sin(-Math.PI / 2));
            g2.rotate(tangent, sx, sy);
            for (int i = -4; i < 4; i++) {
                g2.setColor(i % 2 == 0 ? Color.WHITE : Color.BLACK);
                g2.fillRect((int) sx - 8, (int) sy + i * 6, 16, 6);
            }
            g2.rotate(-tangent, sx, sy);
        }

        Path2D trackPath() {
            Path2D p = new Path2D.Double();
            for (int i = 0; i <= 120; i++) {
                double a = Math.PI * 2 * i / 120;
                double x = cx + rx * Math.cos(a), y = cy + ry * Math.sin(a);
                if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            p.closePath();
            return p;
        }

        Path2D offsetPath(double off) {
            Path2D p = new Path2D.Double();
            for (int i = 0; i <= 120; i++) {
                double a = Math.PI * 2 * i / 120;
                double rr = ellipseRadiusAt(a);
                double ux = rx * Math.cos(a) / rr, uy = ry * Math.sin(a) / rr;
                double x = cx + rx * Math.cos(a) + ux * off;
                double y = cy + ry * Math.sin(a) + uy * off;
                if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            p.closePath();
            return p;
        }

        void drawMenu(Graphics2D g2) {
            float pulse = (float) (Math.sin(menuPulse * 0.05) * 0.5 + 0.5);
            g2.setFont(new Font("Arial Black", Font.BOLD, 58));
            g2.setColor(new Color(1f, 0.9f, 0.2f));
            centerText(g2, "HOVERBOARD", H / 2 - 120);
            g2.setColor(Color.getHSBColor((menuPulse * 0.003f) % 1f, 0.8f, 1f));
            centerText(g2, "RACING ADVENTURE", H / 2 - 60);

            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.setColor(new Color(0.4f + pulse * 0.6f, 1f, 1f));
            centerText(g2, "Press  [1]  Play vs Friend", H / 2 + 30);
            g2.setColor(new Color(1f, 0.5f + pulse * 0.5f, 0.7f));
            centerText(g2, "Press  [2]  Play vs Computer", H / 2 + 70);

            g2.setFont(new Font("Arial", Font.PLAIN, 16));
            g2.setColor(new Color(200, 200, 220));
            centerText(g2, "Player 1: W A S D     Player 2: Arrow Keys", H / 2 + 130);
            centerText(g2, "Grab coins, hit boost pads, avoid grass!  First to "
                    + TOTAL_LAPS + " laps wins.", H / 2 + 158);
        }

        void drawHUD(Graphics2D g2) {
            g2.setFont(new Font("Arial Black", Font.BOLD, 18));
            hudBox(g2, 20, 16, p1, "WASD");
            hudBox(g2, W - 250, 16, p2, p2.isAI ? "AI" : "ARROWS");

            // position indicator
            boolean p1Leads = progressOf(p1) >= progressOf(p2);
            g2.setFont(new Font("Arial Black", Font.BOLD, 26));
            g2.setColor(new Color(255, 215, 0));
            centerText(g2, p1Leads ? "P1  1st" : "P1  2nd", 40);
            g2.setColor(p2.isAI ? new Color(255, 120, 180) : new Color(255, 120, 180));
            centerText(g2, p1Leads ? (p2.isAI ? "CPU 2nd" : "P2  2nd") : (p2.isAI ? "CPU 1st" : "P2  1st"), 68);
        }

        void hudBox(Graphics2D g2, int x, int y, Racer r, String keysLabel) {
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect(x, y, 230, 64, 14, 14);
            g2.setColor(r.color);
            g2.fillOval(x + 12, y + 12, 16, 16);
            g2.setFont(new Font("Arial Black", Font.BOLD, 15));
            g2.drawString(r.name, x + 38, y + 25);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(Color.WHITE);
            g2.drawString("Lap " + Math.min(r.lap, TOTAL_LAPS) + "/" + TOTAL_LAPS
                    + "   Coins: " + r.score, x + 12, y + 50);
            g2.setColor(new Color(180, 180, 200));
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.drawString(keysLabel, x + 175, y + 25);
        }

        void drawCountdown(Graphics2D g2) {
            int cd = (countdownTick + 59) / 60;
            float frac = (countdownTick % 60) / 60f;
            String txt = cd > 0 ? String.valueOf(cd) : "GO!";
            g2.setColor(new Color(0, 0, 0, 110));
            g2.fillRect(0, 0, W, H);
            g2.setFont(new Font("Arial Black", Font.BOLD, (int) (140 + (1 - frac) * 30)));
            g2.setColor(Color.getHSBColor(cd / 4f, 0.85f, 1f));
            centerText(g2, txt, H / 2 + 20);
        }

        void drawFinish(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, W, H);
            g2.setFont(new Font("Arial Black", Font.BOLD, 52));
            g2.setColor(new Color(255, 215, 0));
            centerText(g2, winnerText, H / 2 - 30);
            g2.setFont(new Font("Arial", Font.BOLD, 22));
            g2.setColor(Color.WHITE);
            centerText(g2, "Press [R] to race again   |   [M] for menu", H / 2 + 30);
        }

        void centerText(Graphics2D g2, String s, int y) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, (W - fm.stringWidth(s)) / 2, y);
        }

        // ---------------------------------------------------------- input
        boolean[] keys = new boolean[600];

        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            keys[k] = true;
            if (state == State.MENU) {
                if (k == KeyEvent.VK_1) setupRace(false);
                if (k == KeyEvent.VK_2) setupRace(true);
            } else if (state == State.FINISHED) {
                if (k == KeyEvent.VK_R) setupRace(vsComputer);
                if (k == KeyEvent.VK_M) state = State.MENU;
            }
        }

        public void keyReleased(KeyEvent e) { keys[e.getKeyCode()] = false; }
        public void keyTyped(KeyEvent e) {}
    }

    // ============================================================= RACER
    class Racer {
        String name;
        Color color;
        double x, y, angle, speed;
        double tilt = 0, bob = 0;
        int lap = 1, nextCp = 0, score = 0, boostTimer = 0, flash = 0;
        boolean isAI = false;

        Racer(String name, Color color, int x, int y) {
            this.name = name; this.color = color; this.x = x; this.y = y;
        }

        void draw(Graphics2D g2) {
            if (flash > 0) flash--;
            AffineTransform old = g2.getTransform();
            g2.translate(x, y + Math.sin(bob) * 2.5);
            g2.rotate(angle);
            g2.rotate(tilt * 0.22); // lean into turns

            // shadow
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval(-20, 10, 40, 10);

            // boost flame
            if (boostTimer > 0) {
                int fl = 12 + (int) (Math.random() * 14);
                GradientPaint flame = new GradientPaint(-16, 0, Color.YELLOW, -16 - fl, 0, new Color(255, 60, 0, 0));
                g2.setPaint(flame);
                g2.fillPolygon(new int[]{-14, -14 - fl, -14}, new int[]{-5, 0, 5}, 3);
            }

            // board glow
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
            g2.fillOval(-24, -12, 48, 24);

            // board
            g2.setColor(new Color(35, 35, 50));
            g2.fillRoundRect(-22, -7, 44, 14, 12, 12);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(-22, -7, 44, 14, 12, 12);
            g2.setColor(color.brighter());
            g2.fillRoundRect(-10, -4, 20, 8, 6, 6);

            // rider body
            g2.setColor(color.darker());
            g2.fillOval(-7, -17, 16, 14);
            // helmet
            g2.setColor(color);
            g2.fillOval(-4, -22, 14, 12);
            g2.setColor(new Color(180, 240, 255));
            g2.fillOval(2, -20, 8, 6); // visor

            g2.setTransform(old);

            // lap-up flash ring
            if (flash > 0) {
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), flash * 6));
                g2.setStroke(new BasicStroke(4));
                g2.drawOval((int) x - 30, (int) y - 30, 60, 60);
            }
        }
    }

    // ============================================================= COIN
    class Coin {
        double x, y, spin;
        boolean taken = false;
        int respawn = 0;

        Coin(double x, double y) {
            this.x = x; this.y = y;
        }

        void draw(Graphics2D g2) {
            if (taken) return;
            double w = Math.abs(Math.cos(spin)) * 12 + 2;
            // glow
            g2.setColor(new Color(255, 215, 0, 60));
            g2.fillOval((int) (x - 16), (int) (y - 16), 32, 32);
            g2.setColor(new Color(255, 215, 0));
            g2.fillOval((int) (x - w), (int) (y - 12), (int) (2 * w), 24);
            g2.setColor(new Color(255, 245, 180));
            g2.fillOval((int) (x - w * 0.5), (int) (y - 6), (int) w, 12);
        }
    }

    // ============================================================= BOOST PAD
    class BoostPad {
        double x, y, pulse = Math.random() * 6;

        BoostPad(double x, double y) {
            this.x = x; this.y = y;
        }

        void draw(Graphics2D g2) {
            float a = (float) (Math.sin(pulse) * 0.4 + 0.6);
            g2.setColor(new Color(0, 255, 170, (int) (60 * a)));
            g2.fillOval((int) x - 28, (int) y - 28, 56, 56);
            g2.setColor(new Color(0, 255, 170, (int) (200 * a)));
            for (int i = 0; i < 3; i++) {
                int ox = -12 + i * 10;
                g2.fillPolygon(
                        new int[]{(int) x + ox, (int) x + ox, (int) x + ox + 8},
                        new int[]{(int) y - 12, (int) y + 12, (int) y}, 3);
            }
        }
    }

    // ============================================================= PARTICLE
    class Particle {
        double x, y, vx, vy, size;
        int life, maxLife;
        Color color;

        Particle(double x, double y, double vx, double vy, double size, int life, Color color) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.size = size; this.life = life; this.maxLife = life; this.color = color;
        }

        void update() {
            x += vx; y += vy;
            vy += 0.02;          // slight gravity
            vx *= 0.97; vy *= 0.97;
            life--;
        }

        void draw(Graphics2D g2) {
            float a = Math.max(0, life / (float) maxLife);
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (a * 220)));
            g2.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
        }
    }

    // ============================================================= STAR
    class Star {
        double x, y, size;
        double phase;
        double brightness;

        Star(double x, double y, double size, double phase) {
            this.x = x; this.y = y; this.size = size; this.phase = phase;
        }

        void twinkle() {
            brightness = Math.sin(System.currentTimeMillis() * 0.002 + phase * 10) * 0.5 + 0.5;
        }
    }
}