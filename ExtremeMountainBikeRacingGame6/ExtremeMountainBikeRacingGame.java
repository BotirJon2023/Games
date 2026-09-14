import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Extreme Mountain Bike Racing Game
 *
 * A dependency-free Java2D racing game. It supports:
 * - one player against an adaptive computer rider
 * - local two-player racing on the same keyboard
 * - physics-inspired acceleration, slopes, jumps, landings and air control
 * - procedural mountain scenery, trees, clouds, dust and speed particles
 * - a finish line, progress HUD, countdown and restart flow
 *
 * Compile and run:
 *   javac ExtremeMountainBikeRacingGame.java
 *   java ExtremeMountainBikeRacingGame
 */
public class ExtremeMountainBikeRacingGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Extreme Mountain Bike Racing");
            GamePanel game = new GamePanel();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(game);
            frame.setSize(GamePanel.WIDTH, GamePanel.HEIGHT);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }

    static final class GamePanel extends JPanel implements ActionListener, KeyListener {
        static final int WIDTH = 1280;
        static final int HEIGHT = 720;
        static final int GROUND_BASE = 505;
        static final double FINISH_DISTANCE = 16000;

        enum Screen { MENU, RACING, FINISHED }

        private final Set<Integer> keys = new HashSet<>();
        private final List<Particle> particles = new ArrayList<>();
        private final Random random = new Random(22);
        private final Timer timer;
        private Bike riderOne;
        private Bike riderTwo;
        private Screen screen = Screen.MENU;
        private int raceMode = 1;
        private int countdown = 0;
        private int frame = 0;
        private double cameraX = 0;
        private String result = "";

        GamePanel() {
            setFocusable(true);
            addKeyListener(this);
            timer = new Timer(16, this);
            timer.start();
        }

        private void startRace(int mode) {
            raceMode = mode;
            riderOne = new Bike("YOU", 120, new Color(0xFF6B35), false, 0);
            riderTwo = new Bike(mode == 1 ? "CPU" : "PLAYER 2",
                    40, new Color(0x35A7FF), mode == 1, 1);
            riderOne.reset();
            riderTwo.reset();
            particles.clear();
            cameraX = 0;
            countdown = 150;
            frame = 0;
            result = "";
            screen = Screen.RACING;
            requestFocusInWindow();
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            frame++;
            if (screen == Screen.MENU) {
                repaint();
                return;
            }
            if (screen == Screen.RACING) {
                updateRace();
            } else if (screen == Screen.FINISHED) {
                updateParticles();
            }
            repaint();
        }

        private void updateRace() {
            if (countdown > 0) {
                countdown--;
            }
            boolean active = countdown == 0;
            riderOne.update(active);
            riderTwo.update(active);

            double leader = Math.max(riderOne.x, riderTwo.x);
            cameraX += (Math.max(0, leader - 245) - cameraX) * 0.08;
            cameraX = Math.max(0, Math.min(cameraX, FINISH_DISTANCE - WIDTH + 250));

            if (riderOne.x >= FINISH_DISTANCE || riderTwo.x >= FINISH_DISTANCE) {
                if (riderOne.x >= FINISH_DISTANCE && riderTwo.x >= FINISH_DISTANCE) {
                    result = riderOne.x >= riderTwo.x ? "YOU WIN BY A NOSE!" : "PLAYER 2 WINS!";
                } else if (riderOne.x >= FINISH_DISTANCE) {
                    result = "YOU WIN!";
                } else {
                    result = raceMode == 1 ? "THE CPU WINS" : "PLAYER 2 WINS!";
                }
                screen = Screen.FINISHED;
            }
            updateParticles();
        }

        private void updateParticles() {
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.x += p.vx;
                p.y += p.vy;
                p.vy += 0.08;
                p.life--;
                if (p.life <= 0) {
                    particles.remove(i);
                }
            }
        }

        private boolean down(int key) {
            return keys.contains(key);
        }

        private double groundY(double worldX) {
            double rolling = Math.sin(worldX * 0.0061) * 38
                    + Math.sin(worldX * 0.0157 + 1.2) * 17
                    + Math.sin(worldX * 0.0022) * 30;
            double bumps = Math.sin(worldX * 0.031 + 0.7) * 7;
            if (worldX < 420) {
                rolling *= Math.max(0, worldX / 420.0);
            }
            return GROUND_BASE + rolling + bumps;
        }

        private double groundSlope(double x) {
            return (groundY(x + 5) - groundY(x - 5)) / 10.0;
        }

        private void addDust(double x, double y, Color color, double speed) {
            if (random.nextDouble() > Math.min(0.75, speed / 14.0)) {
                return;
            }
            particles.add(new Particle(
                    x + random.nextDouble() * 18 - 9,
                    y + random.nextDouble() * 8,
                    -speed * (0.06 + random.nextDouble() * 0.08),
                    -random.nextDouble() * 1.2,
                    22 + random.nextInt(20),
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), 110),
                    2 + random.nextDouble() * 4));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            drawSky(g);
            if (screen == Screen.MENU) {
                drawMenu(g);
            } else {
                drawWorld(g);
                drawHud(g);
                if (countdown > 0 && screen == Screen.RACING) {
                    drawCountdown(g);
                }
                if (screen == Screen.FINISHED) {
                    drawFinishOverlay(g);
                }
            }
            g.dispose();
        }

        private void drawSky(Graphics2D g) {
            g.setPaint(new GradientPaint(0, 0, new Color(0x102947), 0, HEIGHT,
                    new Color(0xD8F2F0)));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setColor(new Color(255, 224, 142, 210));
            g.fillOval(930, 76, 132, 132);
            g.setColor(new Color(255, 241, 188, 42));
            g.fillOval(890, 36, 212, 212);

            drawCloud(g, 180 - cameraX * 0.05, 108, 1.0);
            drawCloud(g, 650 - cameraX * 0.08, 168, 0.72);
            drawCloud(g, 1110 - cameraX * 0.04, 95, 0.88);
            drawMountainLayer(g, 0.12, new Color(0x3C6074), 375, 120);
            drawMountainLayer(g, 0.22, new Color(0x2B4B60), 435, 150);
            drawMountainLayer(g, 0.34, new Color(0x1B394B), 492, 180);
        }

        private void drawCloud(Graphics2D g, double x, double y, double scale) {
            double wrapped = ((x % (WIDTH + 260)) + WIDTH + 260) % (WIDTH + 260) - 120;
            g.setColor(new Color(235, 250, 248, 72));
            g.fillOval((int) wrapped, (int) y, (int) (115 * scale), (int) (34 * scale));
            g.fillOval((int) (wrapped + 34 * scale), (int) (y - 18 * scale),
                    (int) (82 * scale), (int) (52 * scale));
            g.fillOval((int) (wrapped + 78 * scale), (int) (y - 8 * scale),
                    (int) (70 * scale), (int) (42 * scale));
        }

        private void drawMountainLayer(Graphics2D g, double parallax, Color color,
                                       int base, int height) {
            Path2D path = new Path2D.Double();
            path.moveTo(0, HEIGHT);
            path.lineTo(0, base);
            for (int x = 0; x <= WIDTH + 50; x += 24) {
                double world = x + cameraX * parallax;
                double wave = Math.sin(world * 0.009) * height * 0.28
                        + Math.sin(world * 0.021 + 2) * height * 0.15;
                double peak = base - height * 0.42 - wave;
                path.lineTo(x, peak);
            }
            path.lineTo(WIDTH, HEIGHT);
            path.closePath();
            g.setColor(color);
            g.fill(path);
        }

        private void drawWorld(Graphics2D g) {
            drawTrack(g);
            drawTrees(g);
            drawFinishLine(g);

            for (Particle p : particles) {
                p.draw(g, cameraX);
            }
            riderTwo.draw(g, cameraX);
            riderOne.draw(g, cameraX);
        }

        private void drawTrack(Graphics2D g) {
            Path2D ground = new Path2D.Double();
            ground.moveTo(0, HEIGHT);
            for (int sx = -15; sx <= WIDTH + 20; sx += 10) {
                ground.lineTo(sx, groundY(sx + cameraX));
            }
            ground.lineTo(WIDTH, HEIGHT);
            ground.closePath();
            g.setPaint(new GradientPaint(0, 430, new Color(0x4E874D), 0, HEIGHT,
                    new Color(0x162E2B)));
            g.fill(ground);

            g.setColor(new Color(0xD19A5E));
            g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D trail = new Path2D.Double();
            for (int sx = -10; sx <= WIDTH + 20; sx += 8) {
                double y = groundY(sx + cameraX);
                if (sx == -10) trail.moveTo(sx, y - 1);
                else trail.lineTo(sx, y - 1);
            }
            g.draw(trail);
            g.setColor(new Color(240, 201, 130, 130));
            g.setStroke(new BasicStroke(2));
            for (int i = 0; i < 12; i++) {
                int sx = i * 123 - (int) (cameraX * 0.7) % 123;
                double sy = groundY(sx + cameraX) - 8;
                g.drawLine(sx, (int) sy, sx + 20, (int) sy - 2);
            }
        }

        private void drawTrees(Graphics2D g) {
            int first = (int) Math.floor(cameraX / 170) * 170 - 340;
            for (int x = first; x < cameraX + WIDTH + 280; x += 170) {
                int screenX = (int) (x - cameraX);
                int ground = (int) groundY(x);
                int treeHeight = 62 + Math.abs((x / 170) % 4) * 14;
                g.setColor(new Color(0x342C25));
                g.fillRect(screenX - 4, ground - treeHeight / 3, 8, treeHeight / 2);
                g.setColor(new Color(0x173F38));
                drawPine(g, screenX, ground - treeHeight / 3, treeHeight);
                if ((x / 170) % 3 == 0) {
                    g.setColor(new Color(255, 230, 145, 170));
                    g.fillOval(screenX - 2, ground - treeHeight + 12, 4, 4);
                }
            }
        }

        private void drawPine(Graphics2D g, int x, int y, int height) {
            Path2D pine = new Path2D.Double();
            pine.moveTo(x, y - height);
            pine.lineTo(x - height * 0.28, y - height * 0.54);
            pine.lineTo(x - height * 0.16, y - height * 0.54);
            pine.lineTo(x - height * 0.38, y - height * 0.18);
            pine.lineTo(x + height * 0.38, y - height * 0.18);
            pine.lineTo(x + height * 0.16, y - height * 0.54);
            pine.lineTo(x + height * 0.28, y - height * 0.54);
            pine.closePath();
            g.fill(pine);
        }

        private void drawFinishLine(Graphics2D g) {
            double sx = FINISH_DISTANCE - cameraX;
            if (sx < -100 || sx > WIDTH + 100) return;
            int ground = (int) groundY(FINISH_DISTANCE);
            g.setColor(new Color(0xE5E8E4));
            g.fillRect((int) sx - 8, ground - 178, 10, 180);
            g.setColor(new Color(0xE5E8E4));
            g.fillRect((int) sx + 126, ground - 178, 10, 180);
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 8; col++) {
                    g.setColor((row + col) % 2 == 0 ? Color.WHITE : new Color(0x26343A));
                    g.fillRect((int) sx - 5 + col * 17, ground - 180 + row * 17, 17, 17);
                }
            }
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString("FINISH", (int) sx + 16, ground - 195);
        }

        private void drawHud(Graphics2D g) {
            g.setColor(new Color(7, 16, 24, 190));
            g.fillRoundRect(22, 20, WIDTH - 44, 78, 18, 18);
            g.setColor(new Color(255, 255, 255, 30));
            g.drawRoundRect(22, 20, WIDTH - 44, 78, 18, 18);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.drawString("EXTREME MOUNTAIN BIKE", 44, 50);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(185, 211, 215));
            g.drawString(raceMode == 1 ? "SOLO / VS COMPUTER" : "LOCAL TWO PLAYER", 45, 70);

            drawProgress(g, riderOne, 310);
            drawProgress(g, riderTwo, 670);
            g.setColor(new Color(180, 204, 206));
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.drawString("WASD", 1080, 49);
            g.drawString(raceMode == 1 ? "ARROWS" : "IJKL", 1080, 72);

            g.setColor(new Color(255, 255, 255, 180));
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString("R restart   •   ESC menu", 45, HEIGHT - 22);
        }

        private void drawProgress(Graphics2D g, Bike bike, int x) {
            g.setColor(bike.color);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString(bike.name, x, 48);
            g.setColor(new Color(255, 255, 255, 45));
            g.fillRoundRect(x, 57, 230, 8, 8, 8);
            g.setColor(bike.color);
            g.fillRoundRect(x, 57, (int) (230 * Math.min(1, bike.x / FINISH_DISTANCE)), 8, 8, 8);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.drawString(String.format("%03d km/h", (int) (bike.speed * 7)), x + 142, 48);
        }

        private void drawCountdown(Graphics2D g) {
            int seconds = (countdown + 49) / 50;
            String text = seconds > 0 ? String.valueOf(seconds) : "GO!";
            g.setColor(new Color(4, 11, 17, 70));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, text.equals("GO!") ? 90 : 120));
            int textWidth = g.getFontMetrics().stringWidth(text);
            g.drawString(text, (WIDTH - textWidth) / 2, 390);
        }

        private void drawMenu(Graphics2D g) {
            g.setColor(new Color(8, 19, 27, 155));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 62));
            g.drawString("EXTREME", 72, 150);
            g.setColor(new Color(255, 184, 77));
            g.drawString("MOUNTAIN BIKE", 70, 218);
            g.setColor(new Color(198, 229, 226));
            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            g.drawString("RIDE THE RIDGELINE. OUTBRAKE THE IMPOSSIBLE.", 76, 258);

            g.setColor(new Color(8, 18, 27, 218));
            g.fillRoundRect(72, 324, 480, 215, 22, 22);
            g.setColor(new Color(255, 255, 255, 35));
            g.drawRoundRect(72, 324, 480, 215, 22, 22);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 21));
            g.drawString("CHOOSE YOUR RACE", 105, 370);
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.setColor(new Color(237, 243, 239));
            g.drawString("1   Solo ride  /  challenge the CPU", 105, 420);
            g.drawString("2   Local two-player showdown", 105, 465);
            g.setColor(new Color(255, 200, 106));
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.drawString("WASD: Player 1     Arrows or IJKL: Player 2", 105, 510);

            g.setColor(new Color(236, 248, 242));
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.drawString("Built for speed.", 790, 405);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.setColor(new Color(198, 221, 220));
            g.drawString("Procedural mountain trails", 790, 438);
            g.drawString("Air control and landings", 790, 465);
            g.drawString("Dust, particles and a finish sprint", 790, 492);
        }

        private void drawFinishOverlay(Graphics2D g) {
            g.setColor(new Color(5, 13, 19, 188));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 53));
            int width = g.getFontMetrics().stringWidth(result);
            g.drawString(result, (WIDTH - width) / 2, 290);
            g.setColor(new Color(255, 196, 93));
            g.setFont(new Font("SansSerif", Font.BOLD, 19));
            String sub = "R  race again     ESC  return to menu";
            g.drawString(sub, (WIDTH - g.getFontMetrics().stringWidth(sub)) / 2, 350);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            keys.add(e.getKeyCode());
            int key = e.getKeyCode();
            if (screen == Screen.MENU) {
                if (key == KeyEvent.VK_1) startRace(1);
                if (key == KeyEvent.VK_2) startRace(2);
            } else if (key == KeyEvent.VK_R) {
                startRace(raceMode);
            } else if (key == KeyEvent.VK_ESCAPE) {
                screen = Screen.MENU;
                keys.clear();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            keys.remove(e.getKeyCode());
        }

        @Override
        public void keyTyped(KeyEvent e) {
            // KeyListener requires this method; keyPressed is used for controls.
        }

        final class Bike {
            final String name;
            final Color color;
            final boolean cpu;
            final int player;
            double x;
            double y;
            double speed;
            double verticalSpeed;
            double angle;
            double wheelRotation;
            boolean grounded;
            int boostTimer;

            Bike(String name, double startX, Color color, boolean cpu, int player) {
                this.name = name;
                this.color = color;
                this.cpu = cpu;
                this.player = player;
                this.x = startX;
            }

            void reset() {
                x = player == 0 ? 120 : 40;
                y = groundY(x) - 72;
                speed = 0;
                verticalSpeed = 0;
                angle = 0;
                wheelRotation = 0;
                grounded = true;
                boostTimer = 0;
            }

            void update(boolean active) {
                boolean left;
                boolean right;
                boolean pedal;
                boolean brake;
                if (cpu) {
                    double slope = groundSlope(x);
                    pedal = true;
                    brake = slope > 0.62 && speed > 7.2;
                    left = slope < -0.35;
                    right = slope > 0.35;
                    if (Math.sin(x * 0.013) > 0.94 && grounded) boostTimer = 9;
                } else if (player == 0) {
                    left = down(KeyEvent.VK_A);
                    right = down(KeyEvent.VK_D);
                    pedal = down(KeyEvent.VK_W);
                    brake = down(KeyEvent.VK_S);
                } else {
                    left = down(KeyEvent.VK_LEFT) || down(KeyEvent.VK_J);
                    right = down(KeyEvent.VK_RIGHT) || down(KeyEvent.VK_L);
                    pedal = down(KeyEvent.VK_UP) || down(KeyEvent.VK_I);
                    brake = down(KeyEvent.VK_DOWN) || down(KeyEvent.VK_K);
                }

                if (!active) {
                    speed *= 0.97;
                    return;
                }

                double slope = groundSlope(x);
                if (pedal) speed += cpu ? 0.075 : 0.105;
                if (brake) speed *= 0.965;
                if (slope < -0.25) speed += 0.025;
                if (slope > 0.42) speed -= 0.035;
                speed = Math.max(0.25, Math.min(12.5, speed));

                if (grounded) {
                    double targetAngle = Math.atan(slope);
                    if (left) targetAngle -= 0.22;
                    if (right) targetAngle += 0.22;
                    angle += (targetAngle - angle) * 0.17;
                    y = groundY(x) - 72;
                    verticalSpeed = 0;

                    if (speed > 5.4 && Math.abs(slope) > 0.54) {
                        verticalSpeed = -Math.min(13, 4.2 + speed * 0.55);
                        grounded = false;
                    }
                    addDust(x, groundY(x) - 5, color, speed);
                } else {
                    verticalSpeed += 0.43;
                    y += verticalSpeed;
                    double targetAngle = angle + (right ? 0.025 : 0) - (left ? 0.025 : 0);
                    angle += (targetAngle - angle) * 0.6;
                }

                x += speed * (boostTimer > 0 ? 1.13 : 1);
                if (boostTimer > 0) boostTimer--;
                wheelRotation += speed * 0.12;

                double landingY = groundY(x) - 72;
                if (y >= landingY) {
                    if (!grounded && verticalSpeed > 7) {
                        addDust(x, groundY(x) - 3, Color.WHITE, speed + 4);
                    }
                    y = landingY;
                    grounded = true;
                    verticalSpeed = 0;
                }
            }

            void draw(Graphics2D g, double camera) {
                double sx = x - camera;
                double sy = y;
                if (sx < -100 || sx > WIDTH + 100) return;

                g.setColor(new Color(0, 0, 0, 70));
                g.fillOval((int) sx - 34, (int) sy + 31, 70, 11);

                Graphics2D b = (Graphics2D) g.create();
                b.translate(sx, sy);
                b.rotate(angle);
                b.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                b.setColor(new Color(16, 22, 26));
                b.fillOval(-38, 7, 37, 37);
                b.fillOval(17, 7, 37, 37);
                b.setColor(new Color(206, 219, 213));
                b.setStroke(new BasicStroke(1.5f));
                drawWheel(b, -20, 25);
                drawWheel(b, 36, 25);

                b.setColor(color);
                b.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                b.drawLine(-20, 25, 0, 7);
                b.drawLine(0, 7, 36, 25);
                b.drawLine(-20, 25, 36, 25);
                b.drawLine(0, 7, 12, -10);
                b.drawLine(12, -10, 36, 25);
                b.setColor(new Color(240, 242, 229));
                b.setStroke(new BasicStroke(2));
                b.drawLine(12, -10, 28, -11);
                b.drawLine(28, -11, 36, 0);

                b.setColor(new Color(30, 38, 44));
                b.fillOval(-1, -39, 23, 24);
                b.setColor(color);
                b.fillRoundRect(-9, -21, 27, 29, 9, 9);
                b.setColor(new Color(235, 91, 68));
                b.fillRoundRect(0, -18, 16, 6, 3, 3);
                b.setColor(new Color(27, 32, 36));
                b.setStroke(new BasicStroke(3));
                b.drawLine(4, 4, -12, 21);
                b.drawLine(12, 4, 29, 20);
                b.drawLine(11, -13, 27, -7);
                b.drawLine(-2, -13, -16, 0);
                b.setColor(Color.WHITE);
                b.setFont(new Font("SansSerif", Font.BOLD, 9));
                b.drawString(name, -15, -47);
                b.dispose();
            }

            private void drawWheel(Graphics2D g, int x, int y) {
                g.drawOval(x - 15, y - 15, 30, 30);
                g.drawLine(x - 12, y, x + 12, y);
                g.drawLine(x, y - 12, x, y + 12);
                g.drawLine(x - 9, y - 9, x + 9, y + 9);
                g.drawLine(x - 9, y + 9, x + 9, y - 9);
            }
        }

        static final class Particle {
            double x;
            double y;
            double vx;
            double vy;
            int life;
            final int maxLife;
            final Color color;
            final double size;

            Particle(double x, double y, double vx, double vy, int life,
                     Color color, double size) {
                this.x = x;
                this.y = y;
                this.vx = vx;
                this.vy = vy;
                this.life = life;
                this.maxLife = life;
                this.color = color;
                this.size = size;
            }

            void draw(Graphics2D g, double camera) {
                float alpha = Math.max(0, Math.min(1, life / (float) maxLife));
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g.setColor(color);
                double screenX = x - camera;
                g.fill(new Ellipse2D.Double(screenX - size / 2, y - size / 2, size, size));
                g.setComposite(AlphaComposite.SrcOver);
            }
        }
    }
}