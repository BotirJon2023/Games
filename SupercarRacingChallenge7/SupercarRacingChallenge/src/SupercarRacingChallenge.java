import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;


public class SupercarRacingChallenge extends JFrame {

    private GamePanel gamePanel;

    public SupercarRacingChallenge() {
        setTitle("🏆 Supercar Racing Challenge 🏁");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        setContentPane(gamePanel);

        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SupercarRacingChallenge game = new SupercarRacingChallenge();
            game.setVisible(true);
        });
    }

    // ================================================================
    //  GAME PANEL — handles all rendering, input, and game logic
    // ================================================================
    class GamePanel extends JPanel {

        // --- Screen ---
        private static final int WIDTH = 1000;
        private static final int HEIGHT = 680;

        // --- Game States ---
        private enum GameState { MENU, COUNTDOWN, RACING, FINISHED }
        private GameState state = GameState.MENU;

        // --- Track ---
        private final int lapsToWin = 3;
        private List<Point2D.Double> trackPoints;
        private List<Double> trackAngles;
        private double trackWidth = 110;

        // --- Cars ---
        private Car car1, car2;
        private boolean twoPlayer = false;

        // --- Particles ---
        private List<Particle> particles = new ArrayList<>();

        // --- Countdown ---
        private int countdownTimer = 0;
        private int countdownNumber = 3;

        // --- Results ---
        private int finishTimer = 0;

        // --- Background ---
        private int backgroundOffset = 0;

        // --- Key State ---
        private Set<Integer> keys = new HashSet<>();

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            requestFocusInWindow();

            generateTrack();

            addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    keys.add(e.getKeyCode());
                    if (state == GameState.MENU) {
                        if (e.getKeyCode() == java.awt.event.KeyEvent.VK_1) {
                            twoPlayer = false;
                            startRace();
                        } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_2) {
                            twoPlayer = true;
                            startRace();
                        }
                    }
                    if (state == GameState.FINISHED && e.getKeyCode() == java.awt.event.KeyEvent.VK_R) {
                        state = GameState.MENU;
                        particles.clear();
                    }
                }

                @Override
                public void keyReleased(java.awt.event.KeyEvent e) {
                    keys.remove(e.getKeyCode());
                }
            });

            // Main game loop
            javax.swing.Timer timer = new javax.swing.Timer(1000 / 60, e -> update());
            timer.start();
        }

        // --- Track generation: smooth oval with curves ---
        private void generateTrack() {
            trackPoints = new ArrayList<>();
            trackAngles = new ArrayList<>();

            int cx = WIDTH / 2;
            int cy = HEIGHT / 2 + 10;
            double rx = 380;
            double ry = 240;

            int numPoints = 120;
            for (int i = 0; i < numPoints; i++) {
                double angle = (2 * Math.PI * i) / numPoints;
                double r1 = rx + 15 * Math.sin(angle * 3);
                double r2 = ry + 10 * Math.cos(angle * 2);
                double x = cx + r1 * Math.cos(angle);
                double y = cy + r2 * Math.sin(angle);
                trackPoints.add(new Point2D.Double(x, y));
            }

            for (int i = 0; i < numPoints; i++) {
                Point2D.Double p1 = trackPoints.get(i);
                Point2D.Double p2 = trackPoints.get((i + 1) % numPoints);
                trackAngles.add(Math.atan2(p2.y - p1.y, p2.x - p1.x));
            }
        }

        private void startRace() {
            int startIdx = 0;
            Point2D.Double startPos = trackPoints.get(startIdx);
            double startAngle = trackAngles.get(startIdx);

            double perpX = Math.cos(startAngle + Math.PI / 2);
            double perpY = Math.sin(startAngle + Math.PI / 2);

            car1 = new Car(startPos.x - perpX * 20, startPos.y - perpY * 20, startAngle, new Color(220, 50, 50), "P1");
            car2 = new Car(startPos.x + perpX * 20, startPos.y + perpY * 20, startAngle, new Color(50, 120, 230), twoPlayer ? "P2" : "CPU");

            car1.lap = 0;
            car2.lap = 0;
            car1.passedStart = true;
            car2.passedStart = true;

            state = GameState.COUNTDOWN;
            countdownTimer = 0;
            countdownNumber = 3;
            particles.clear();
        }

        // ============================================================
        //  UPDATE LOOP
        // ============================================================
        private void update() {
            if (state == GameState.COUNTDOWN) {
                countdownTimer++;
                if (countdownTimer > 60) {
                    countdownNumber--;
                    countdownTimer = 0;
                    if (countdownNumber < 0) {
                        state = GameState.RACING;
                    }
                }
            }

            if (state == GameState.RACING) {
                boolean p1Up = keys.contains(java.awt.event.KeyEvent.VK_UP);
                boolean p1Down = keys.contains(java.awt.event.KeyEvent.VK_DOWN);
                boolean p1Left = keys.contains(java.awt.event.KeyEvent.VK_LEFT);
                boolean p1Right = keys.contains(java.awt.event.KeyEvent.VK_RIGHT);

                car1.update(p1Up, p1Down, p1Left, p1Right, this);

                if (twoPlayer) {
                    boolean p2Up = keys.contains(java.awt.event.KeyEvent.VK_W);
                    boolean p2Down = keys.contains(java.awt.event.KeyEvent.VK_S);
                    boolean p2Left = keys.contains(java.awt.event.KeyEvent.VK_A);
                    boolean p2Right = keys.contains(java.awt.event.KeyEvent.VK_D);
                    car2.update(p2Up, p2Down, p2Left, p2Right, this);
                } else {
                    updateAI();
                }

                checkLap(car1);
                checkLap(car2);

                if (car1.lap >= lapsToWin || car2.lap >= lapsToWin) {
                    state = GameState.FINISHED;
                    finishTimer = 0;
                    Car winner = car1.lap >= lapsToWin ? car1 : car2;
                    for (int i = 0; i < 80; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double sp = 2 + Math.random() * 5;
                        particles.add(new Particle(winner.x, winner.y,
                                Math.cos(a) * sp, Math.sin(a) * sp,
                                winner.color, 60 + (int)(Math.random() * 40)));
                    }
                }
            }

            if (state == GameState.FINISHED) {
                finishTimer++;
            }

            // --- Particles ---
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.update();
                if (p.life <= 0) it.remove();
            }

            if (state == GameState.RACING) {
                if (car1.speed > 1) addExhaustParticles(car1);
                if (car2.speed > 1) addExhaustParticles(car2);
            }

            backgroundOffset = (backgroundOffset + 1) % 40;

            repaint();
        }

        private void addExhaustParticles(Car car) {
            if (Math.random() > 0.5) return;
            double rearX = car.x - Math.cos(car.angle) * 15;
            double rearY = car.y - Math.sin(car.angle) * 15;
            double pAngle = car.angle + Math.PI + (Math.random() - 0.5) * 0.6;
            double pSpeed = 0.5 + Math.random() * 1.5;
            Color c = car.speed > 4 ? new Color(255, 200, 50, 180) : new Color(180, 180, 180, 120);
            particles.add(new Particle(rearX, rearY,
                    Math.cos(pAngle) * pSpeed, Math.sin(pAngle) * pSpeed, c, 25 + (int)(Math.random() * 15)));
        }

        // --- AI logic: follow the track ---
        private void updateAI() {
            int nearestIdx = findNearestTrackPoint(car2.x, car2.y);
            int lookAheadIdx = (nearestIdx + 5) % trackPoints.size();
            Point2D.Double target = trackPoints.get(lookAheadIdx);

            double targetAngle = Math.atan2(target.y - car2.y, target.x - car2.x);
            double angleDiff = normalizeAngle(targetAngle - car2.angle);

            boolean aiUp = true;
            boolean aiDown = false;
            boolean aiLeft = angleDiff < -0.05;
            boolean aiRight = angleDiff > 0.05;

            // Slow down slightly on sharp turns
            if (Math.abs(angleDiff) > 0.6) {
                aiUp = false;
                aiDown = true;
            }

            car2.update(aiUp, aiDown, aiLeft, aiRight, this);
        }

        private int findNearestTrackPoint(double x, double y) {
            int best = 0;
            double bestDist = Double.MAX_VALUE;
            for (int i = 0; i < trackPoints.size(); i++) {
                Point2D.Double p = trackPoints.get(i);
                double d = (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y);
                if (d < bestDist) {
                    bestDist = d;
                    best = i;
                }
            }
            return best;
        }

        private double normalizeAngle(double a) {
            while (a > Math.PI) a -= 2 * Math.PI;
            while (a < -Math.PI) a += 2 * Math.PI;
            return a;
        }

        // --- Lap detection based on crossing start line ---
        private void checkLap(Car car) {
            int idx = findNearestTrackPoint(car.x, car.y);
            // Start line is near index 0
            boolean nearStart = idx < 4 || idx > trackPoints.size() - 4;

            if (nearStart && !car.passedStart) {
                // Must be moving forward through the start line
                car.lap++;
                car.passedStart = true;
                // Sparkle effect on lap completion
                for (int i = 0; i < 20; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double sp = 1 + Math.random() * 3;
                    particles.add(new Particle(car.x, car.y,
                            Math.cos(a) * sp, Math.sin(a) * sp,
                            new Color(255, 220, 80), 40));
                }
            } else if (!nearStart) {
                car.passedStart = false;
            }
        }

        // ============================================================
        //  RENDERING
        // ============================================================
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            drawBackground(g2);

            if (state == GameState.MENU) {
                drawMenu(g2);
            } else {
                drawTrack(g2);
                drawParticles(g2);
                if (car1 != null) drawCar(g2, car1);
                if (car2 != null) drawCar(g2, car2);
                drawHUD(g2);

                if (state == GameState.COUNTDOWN) {
                    drawCountdown(g2);
                }
                if (state == GameState.FINISHED) {
                    drawResults(g2);
                }
            }
        }

        private void drawBackground(Graphics2D g2) {
            // Dark gradient sky
            GradientPaint sky = new GradientPaint(0, 0, new Color(15, 15, 40),
                    0, HEIGHT, new Color(60, 30, 90));
            g2.setPaint(sky);
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            // Scrolling stars
            g2.setColor(new Color(255, 255, 255, 180));
            Random r = new Random(42);
            for (int i = 0; i < 120; i++) {
                int sx = r.nextInt(WIDTH);
                int sy = (r.nextInt(HEIGHT) + backgroundOffset * 2) % HEIGHT;
                int size = r.nextInt(3);
                g2.fillOval(sx, sy, size, size);
            }

            // Distant city silhouette
            g2.setColor(new Color(20, 15, 45));
            int baseY = HEIGHT - 200;
            for (int i = 0; i < WIDTH; i += 40) {
                int h = 40 + (int)(Math.sin(i * 0.05) * 30) + (i % 3) * 15;
                g2.fillRect(i, baseY - h, 40, h + 200);
            }
        }

        private void drawTrack(Graphics2D g2) {
            // Track shadow / border
            g2.setStroke(new BasicStroke((float)(trackWidth + 22), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(0, 0, 0, 120));
            drawTrackPath(g2, 4, 4);

            // Outer curb
            g2.setStroke(new BasicStroke((float)(trackWidth + 16), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(240, 240, 240));
            drawTrackPath(g2, 0, 0);

            // Red/white curb stripes
            g2.setStroke(new BasicStroke((float)(trackWidth + 16), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < trackPoints.size(); i += 4) {
                g2.setColor(i % 8 == 0 ? new Color(220, 40, 40) : new Color(240, 240, 240));
                Point2D.Double p1 = trackPoints.get(i);
                Point2D.Double p2 = trackPoints.get((i + 1) % trackPoints.size());
                g2.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
            }

            // Asphalt
            g2.setStroke(new BasicStroke((float)trackWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(45, 45, 50));
            drawTrackPath(g2, 0, 0);

            // Asphalt texture
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(60, 60, 65));
            for (int i = 0; i < trackPoints.size(); i++) {
                Point2D.Double p = trackPoints.get(i);
                g2.fillOval((int)p.x - 3, (int)p.y - 3, 6, 6);
            }

            // Center dashed line
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10f, new float[]{12f, 14f}, backgroundOffset * 0.5f));
            g2.setColor(new Color(255, 230, 90, 200));
            drawTrackPath(g2, 0, 0);

            // Start / finish line
            Point2D.Double sp = trackPoints.get(0);
            double sa = trackAngles.get(0);
            AffineTransform old = g2.getTransform();
            g2.translate(sp.x, sp.y);
            g2.rotate(sa);
            int squares = 10;
            double sq = trackWidth / squares;
            for (int i = 0; i < squares; i++) {
                for (int j = 0; j < 2; j++) {
                    g2.setColor((i + j) % 2 == 0 ? Color.WHITE : new Color(30, 30, 30));
                    g2.fillRect(j * 8 - 8, (int)(-trackWidth / 2 + i * sq), 8, (int)sq + 1);
                }
            }
            g2.setTransform(old);
        }

        private void drawTrackPath(Graphics2D g2, double offX, double offY) {
            Path2D.Double path = new Path2D.Double();
            Point2D.Double first = trackPoints.get(0);
            path.moveTo(first.x + offX, first.y + offY);
            for (int i = 1; i < trackPoints.size(); i++) {
                Point2D.Double p = trackPoints.get(i);
                path.lineTo(p.x + offX, p.y + offY);
            }
            path.closePath();
            g2.draw(path);
        }

        private void drawCar(Graphics2D g2, Car car) {
            AffineTransform old = g2.getTransform();
            g2.translate(car.x, car.y);
            g2.rotate(car.angle);

            // Shadow
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRoundRect(-18 + 3, -10 + 3, 36, 20, 8, 8);

            // Body
            g2.setColor(car.color);
            g2.fillRoundRect(-18, -10, 36, 20, 8, 8);

            // Body highlight
            g2.setColor(car.color.brighter());
            g2.fillRoundRect(-16, -8, 32, 8, 6, 6);

            // Windshield
            g2.setColor(new Color(120, 200, 255, 220));
            g2.fillRoundRect(-2, -8, 10, 16, 4, 4);

            // Rear window
            g2.setColor(new Color(60, 140, 200, 180));
            g2.fillRoundRect(-14, -7, 6, 14, 3, 3);

            // Racing stripe
            g2.setColor(new Color(255, 255, 255, 180));
            g2.fillRect(-18, -1, 36, 2);

            // Wheels
            g2.setColor(Color.BLACK);
            g2.fillRoundRect(-14, -13, 8, 5, 2, 2);
            g2.fillRoundRect(-14, 8, 8, 5, 2, 2);
            g2.fillRoundRect(6, -13, 8, 5, 2, 2);
            g2.fillRoundRect(6, 8, 8, 5, 2, 2);

            // Headlights
            g2.setColor(new Color(255, 255, 200));
            g2.fillOval(14, -7, 4, 4);
            g2.fillOval(14, 3, 4, 4);

            // Taillights
            g2.setColor(new Color(255, 60, 60));
            g2.fillOval(-18, -7, 3, 4);
            g2.fillOval(-18, 3, 3, 4);

            // Label
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(car.label, -fm.stringWidth(car.label) / 2, -14);

            g2.setTransform(old);

            // Speed glow
            if (car.speed > 3) {
                g2.setColor(new Color(car.color.getRed(), car.color.getGreen(), car.color.getBlue(), 40));
                double radius = 25 + car.speed * 4;
                g2.fill(new Ellipse2D.Double(car.x - radius, car.y - radius, radius * 2, radius * 2));
            }
        }

        private void drawParticles(Graphics2D g2) {
            for (Particle p : particles) {
                float alpha = Math.max(0f, Math.min(1f, p.life / 50f));
                g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(),
                        (int)(alpha * p.color.getAlpha())));
                double s = p.size * (0.5 + alpha * 0.5);
                g2.fill(new Ellipse2D.Double(p.x - s, p.y - s, s * 2, s * 2));
            }
        }

        private void drawHUD(Graphics2D g2) {
            g2.setFont(new Font("Arial", Font.BOLD, 18));

            // Player 1 panel
            drawPlayerPanel(g2, 20, 20, car1, "PLAYER 1", new Color(220, 50, 50));
            // Player 2 / CPU panel
            drawPlayerPanel(g2, WIDTH - 220, 20, car2,
                    twoPlayer ? "PLAYER 2" : "COMPUTER", new Color(50, 120, 230));

            // Lap counter big
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(new Color(255, 255, 255, 200));
            String lapText = "Laps to win: " + lapsToWin;
            g2.drawString(lapText, WIDTH / 2 - 60, 30);
        }

        private void drawPlayerPanel(Graphics2D g2, int x, int y, Car car, String title, Color color) {
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect(x, y, 200, 90, 12, 12);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, 200, 90, 12, 12);

            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(color);
            g2.drawString(title, x + 12, y + 22);

            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.setColor(Color.WHITE);
            g2.drawString("Lap " + Math.min(car.lap + 1, lapsToWin) + "/" + lapsToWin, x + 12, y + 52);

            g2.setFont(new Font("Arial", Font.PLAIN, 13));
            g2.setColor(new Color(200, 200, 200));
            g2.drawString("Speed: " + (int)(car.speed * 30) + " km/h", x + 12, y + 78);

            // Speed bar
            int barW = 100;
            int fill = (int)(Math.min(1.0, car.speed / 5.0) * barW);
            g2.setColor(new Color(60, 60, 60));
            g2.fillRect(x + 90, y + 66, barW, 10);
            GradientPaint gp = new GradientPaint(x + 90, y + 66, Color.GREEN,
                    x + 90 + barW, y + 66, Color.RED);
            g2.setPaint(gp);
            g2.fillRect(x + 90, y + 66, fill, 10);
        }

        private void drawCountdown(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            String text = countdownNumber > 0 ? String.valueOf(countdownNumber) : "GO!";
            g2.setFont(new Font("Arial", Font.BOLD, 160));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (WIDTH - fm.stringWidth(text)) / 2;
            int ty = HEIGHT / 2 + fm.getAscent() / 3;

            // Glow
            g2.setColor(new Color(255, 220, 80, 80));
            g2.drawString(text, tx - 3, ty - 3);
            g2.drawString(text, tx + 3, ty + 3);

            g2.setColor(countdownNumber > 0 ? new Color(255, 220, 80) : new Color(80, 255, 120));
            g2.drawString(text, tx, ty);
        }

        private void drawResults(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            g2.setFont(new Font("Arial", Font.BOLD, 56));
            FontMetrics fm = g2.getFontMetrics();
            String title = "🏁 RACE FINISHED 🏁";
            g2.setColor(new Color(255, 220, 80));
            g2.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, HEIGHT / 2 - 80);

            Car winner = car1.lap >= car2.lap ? car1 : car2;
            String winText = (winner == car1 ? "PLAYER 1" : (twoPlayer ? "PLAYER 2" : "COMPUTER")) + " WINS!";

            g2.setFont(new Font("Arial", Font.BOLD, 40));
            fm = g2.getFontMetrics();
            g2.setColor(winner.color);
            g2.drawString(winText, (WIDTH - fm.stringWidth(winText)) / 2, HEIGHT / 2);

            g2.setFont(new Font("Arial", Font.PLAIN, 22));
            fm = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            String restart = "Press R to return to menu";
            g2.drawString(restart, (WIDTH - fm.stringWidth(restart)) / 2, HEIGHT / 2 + 80);
        }

        private void drawMenu(Graphics2D g2) {
            // Animated title glow
            long t = System.currentTimeMillis();
            int pulse = (int)(Math.sin(t / 300.0) * 20 + 200);

            g2.setFont(new Font("Arial", Font.BOLD, 64));
            FontMetrics fm = g2.getFontMetrics();
            String title = "SUPERCAR RACING";
            int tx = (WIDTH - fm.stringWidth(title)) / 2;
            int ty = HEIGHT / 2 - 120;

            g2.setColor(new Color(255, 200 - pulse / 4, 50, 120));
            g2.drawString(title, tx - 3, ty - 3);
            g2.setColor(new Color(255, 220, 80));
            g2.drawString(title, tx, ty);

            g2.setFont(new Font("Arial", Font.BOLD, 28));
            fm = g2.getFontMetrics();
            String sub = "🏁 CHALLENGE 🏁";
            g2.setColor(new Color(200, 200, 255));
            g2.drawString(sub, (WIDTH - fm.stringWidth(sub)) / 2, ty + 50);

            // Menu options
            g2.setFont(new Font("Arial", Font.BOLD, 26));
            fm = g2.getFontMetrics();

            int optY = HEIGHT / 2 + 20;
            String opt1 = "[1]  Play vs Computer";
            String opt2 = "[2]  Play 2 Players";

            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            if (blink) {
                g2.setColor(new Color(80, 255, 120));
                g2.drawString(opt1, (WIDTH - fm.stringWidth(opt1)) / 2, optY);
            }
            g2.setColor(new Color(80, 200, 255));
            g2.drawString(opt2, (WIDTH - fm.stringWidth(opt2)) / 2, optY + 50);

            // Controls
            g2.setFont(new Font("Arial", Font.PLAIN, 16));
            fm = g2.getFontMetrics();
            g2.setColor(new Color(220, 220, 220));
            String c1 = "Player 1: Arrow Keys  •  Player 2: WASD";
            String c2 = "Race 3 laps around the track. First to finish wins!";
            g2.drawString(c1, (WIDTH - fm.stringWidth(c1)) / 2, HEIGHT - 120);
            g2.drawString(c2, (WIDTH - fm.stringWidth(c2)) / 2, HEIGHT - 90);

            // Decorative cars
            drawMenuCar(g2, 180, HEIGHT - 200, new Color(220, 50, 50), t);
            drawMenuCar(g2, WIDTH - 220, HEIGHT - 200, new Color(50, 120, 230), -t);
        }

        private void drawMenuCar(Graphics2D g2, double x, double y, Color color, long t) {
            AffineTransform old = g2.getTransform();
            g2.translate(x, y);
            g2.rotate(Math.sin(t / 800.0) * 0.15);

            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRoundRect(-28 + 3, -16 + 3, 56, 32, 12, 12);
            g2.setColor(color);
            g2.fillRoundRect(-28, -16, 56, 32, 12, 12);
            g2.setColor(color.brighter());
            g2.fillRoundRect(-24, -12, 48, 12, 8, 8);
            g2.setColor(new Color(120, 200, 255, 220));
            g2.fillRoundRect(-2, -12, 16, 24, 6, 6);
            g2.setColor(Color.BLACK);
            g2.fillRoundRect(-22, -20, 12, 6, 3, 3);
            g2.fillRoundRect(-22, 14, 12, 6, 3, 3);
            g2.fillRoundRect(10, -20, 12, 6, 3, 3);
            g2.fillRoundRect(10, 14, 12, 6, 3, 3);
            g2.setColor(new Color(255, 255, 200));
            g2.fillOval(22, -10, 5, 5);
            g2.fillOval(22, 5, 5, 5);

            g2.setTransform(old);
        }

        // ============================================================
        //  CAR CLASS
        // ============================================================
        class Car {
            double x, y;
            double angle;
            double speed = 0;
            Color color;
            String label;
            int lap = 0;
            boolean passedStart = false;

            private static final double MAX_SPEED = 5.0;
            private static final double ACCEL = 0.12;
            private static final double BRAKE = 0.25;
            private static final double FRICTION = 0.04;
            private static final double TURN_SPEED = 0.055;

            Car(double x, double y, double angle, Color color, String label) {
                this.x = x;
                this.y = y;
                this.angle = angle;
                this.color = color;
                this.label = label;
            }

            void update(boolean up, boolean down, boolean left, boolean right, GamePanel panel) {
                if (up) speed += ACCEL;
                if (down) speed -= BRAKE;
                if (!up && !down) {
                    if (speed > 0) speed = Math.max(0, speed - FRICTION);
                    else if (speed < 0) speed = Math.min(0, speed + FRICTION);
                }
                speed = Math.max(-2.0, Math.min(MAX_SPEED, speed));

                // Steering proportional to speed (can't turn in place)
                double turnFactor = Math.min(1.0, Math.abs(speed) / 2.0);
                if (left) angle -= TURN_SPEED * turnFactor * Math.signum(speed == 0 ? 1 : speed);
                if (right) angle += TURN_SPEED * turnFactor * Math.signum(speed == 0 ? 1 : speed);

                double nx = x + Math.cos(angle) * speed;
                double ny = y + Math.sin(angle) * speed;

                // Keep car on track (soft collision with track edges)
                int idx = panel.findNearestTrackPoint(nx, ny);
                Point2D.Double center = panel.trackPoints.get(idx);
                double dist = Math.hypot(nx - center.x, ny - center.y);
                double maxDist = panel.trackWidth / 2 - 10;

                if (dist > maxDist) {
                    // Push back toward track center
                    double pushAngle = Math.atan2(center.y - ny, center.x - nx);
                    double pushAmount = (dist - maxDist) * 0.5;
                    nx += Math.cos(pushAngle) * pushAmount;
                    ny += Math.sin(pushAngle) * pushAmount;
                    speed *= 0.85; // slow down when scraping walls
                }

                x = nx;
                y = ny;

                // Keep within screen
                x = Math.max(20, Math.min(panel.WIDTH - 20, x));
                y = Math.max(20, Math.min(panel.HEIGHT - 20, y));
            }
        }

        class Particle {
            double x, y, vx, vy;
            Color color;
            int life;
            int maxLife;
            double size;

            Particle(double x, double y, double vx, double vy, Color color, int life) {
                this.x = x;
                this.y = y;
                this.vx = vx;
                this.vy = vy;
                this.color = color;
                this.life = life;
                this.maxLife = life;
                this.size = 2 + Math.random() * 3;
            }

            void update() {
                x += vx;
                y += vy;
                vx *= 0.96;
                vy *= 0.96;
                life--;
            }
        }
    }
}