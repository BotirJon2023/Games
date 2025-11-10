import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class BMXStuntRacing {

    public static void main(String[] args) {
        // Ensure UI looks crisp on HiDPI where possible
        System.setProperty("sun.java2d.uiScale", "1.0");
        // Create the game frame
        javax.swing.SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }

    // Window frame setup
    static class GameFrame extends JFrame {
        GamePanel panel;

        GameFrame() {
            super("BMX Stunt Racing - Java 2D Demo");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setResizable(false);
            panel = new GamePanel(1280, 720);
            setContentPane(panel);
            pack();
            setLocationRelativeTo(null);
        }
    }

    // The main game panel and loop
    static class GamePanel extends JPanel implements ActionListener, KeyListener {

        // Dimensions
        private final int viewW;
        private final int viewH;

        // Timer for 60 FPS
        private final Timer timer;

        // World and systems
        private final World world;
        private final Camera camera;
        private final Input input;
        private final HUD hud;
        private final Random rng;

        // State
        private boolean paused = false;
        private long lastTimeNanos;
        private double accumulator;
        private final double dt = 1.0 / 60.0; // fixed step
        private int fps;
        private int frames;
        private long fpsTimer;

        // Cursor
        private Cursor blankCursor;

        GamePanel(int width, int height) {
            this.viewW = width;
            this.viewH = height;
            setPreferredSize(new Dimension(viewW, viewH));
            setFocusable(true);
            setBackground(Color.BLACK);
            addKeyListener(this);

            rng = new Random(12345);
            world = new World();
            input = new Input();
            camera = new Camera();
            hud = new HUD();

            // Set up a blank cursor for cleaner look
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            blankCursor = toolkit.createCustomCursor(
                    toolkit.createImage(new byte[0]),
                    new Point(0, 0),
                    "blank"
            );
            setCursor(blankCursor);

            timer = new Timer(1000 / 60, this);
            timer.start();
            lastTimeNanos = System.nanoTime();
            fpsTimer = System.currentTimeMillis();

            // Initial world generation
            world.generateInitial();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            double frameTime = (now - lastTimeNanos) / 1_000_000_000.0;
            lastTimeNanos = now;
            accumulator += frameTime;

            if (!paused) {
                // Fixed timestep updates
                while (accumulator >= dt) {
                    updateGame(dt);
                    accumulator -= dt;
                }
            }

            repaint();

            // FPS tracking
            frames++;
            long ms = System.currentTimeMillis();
            if (ms - fpsTimer >= 1000) {
                fps = frames;
                frames = 0;
                fpsTimer = ms;
            }
        }

        private void updateGame(double dt) {
            // Input influences world
            world.update(input, dt);

            // Camera follows player
            camera.update(world, dt);

            // Extend track forward if needed
            world.maybeExtendTrack(camera.camX, viewW, rng);

            // Update HUD metrics (score, speed, combo)
            hud.update(world, dt);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Use Graphics2D with anti-aliasing
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Background gradient sky
            drawSky(g2);

            // Parallax elements (mountains, clouds)
            drawParallax(g2);

            // Ground/ramps
            drawTrack(g2);

            // Coins, particles, player, UI
            drawCollectibles(g2);
            drawParticles(g2);
            drawPlayer(g2);

            // HUD overlay
            drawHUD(g2);

            // Pause banner
            if (paused) {
                drawPauseBanner(g2);
            }

            g2.dispose();
        }

        private void drawSky(Graphics2D g2) {
            Color top = new Color(40, 90, 200);
            Color bottom = new Color(160, 200, 255);
            GradientPaint gp = new GradientPaint(0, 0, top, 0, viewH, bottom);
            g2.setPaint(gp);
            g2.fillRect(0, 0, viewW, viewH);
        }

        private void drawParallax(Graphics2D g2) {
            double cx = camera.camX;
            double cy = camera.camY;

            // Far mountains
            g2.setColor(new Color(60, 80, 120));
            Path2D path = new Path2D.Double();
            path.moveTo(-1000 - cx * 0.2, viewH);
            double baseY = viewH * 0.65;
            for (int i = -1; i <= viewW / 200 + 2; i++) {
                double x = i * 300 - (cx * 0.2) % 300;
                double peak = baseY - 100 - 30 * Math.sin((x + 1000) * 0.002);
                path.lineTo(x, peak);
                path.lineTo(x + 150, baseY + 60);
            }
            path.lineTo(viewW + 1000, viewH);
            path.closePath();
            g2.fill(path);

            // Mid hills
            g2.setColor(new Color(80, 140, 90));
            Path2D hills = new Path2D.Double();
            hills.moveTo(-1000 - cx * 0.4, viewH);
            double baseY2 = viewH * 0.75;
            for (int x = -200; x <= viewW + 200; x += 4) {
                double wx = (x + cx * 0.4) * 0.01;
                double y = baseY2 - 40 * Math.sin(wx) - 20 * Math.sin(wx * 2.7);
                hills.lineTo(x, y);
            }
            hills.lineTo(viewW + 1000, viewH);
            hills.closePath();
            g2.fill(hills);

            // Clouds
            g2.setColor(new Color(255, 255, 255, 200));
            Random cloudRng = new Random(42);
            for (int i = 0; i < 12; i++) {
                double speed = 0.1 + (i % 3) * 0.05;
                double x = ((i * 400) - (cx * speed) % (viewW + 600)) - 200;
                double y = 80 + (i % 5) * 40 + 20 * Math.sin(i * 1.7);
                drawCloud(g2, x, y, 80 + (i % 4) * 15);
            }
        }

        private void drawCloud(Graphics2D g2, double x, double y, double size) {
            g2.fillOval((int) (x), (int) y, (int) size, (int) (size * 0.6));
            g2.fillOval((int) (x + size * 0.4), (int) (y - size * 0.2), (int) (size * 0.6), (int) (size * 0.6));
            g2.fillOval((int) (x - size * 0.2), (int) (y - size * 0.1), (int) (size * 0.7), (int) (size * 0.5));
        }

        private void drawTrack(Graphics2D g2) {
            // Ground path
            double cx = camera.camX;
            double cy = camera.camY;
            int startX = -200;
            int endX = viewW + 200;

            // Draw base ground fill
            Path2D ground = new Path2D.Double();
            ground.moveTo(startX, viewH);
            for (int sx = startX; sx <= endX; sx += 3) {
                double wx = sx + cx;
                double gy = world.groundY(wx);
                ground.lineTo(sx, gy - cy);
            }
            ground.lineTo(endX, viewH);
            ground.closePath();
            g2.setColor(new Color(100, 180, 100));
            g2.fill(ground);

            // Draw ramps
            g2.setColor(new Color(130, 120, 90));
            for (Ramp ramp : world.ramps) {
                if (!ramp.visibleInViewport(cx, cy, viewW, viewH)) continue;
                ShapeBuilder.drawRamp(g2, ramp, cx, cy);
            }

            // Track outline
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(2.0f));
            g2.setColor(new Color(80, 120, 70));
            Path2D line = new Path2D.Double();
            boolean moved = false;
            for (int sx = startX; sx <= endX; sx += 2) {
                double wx = sx + cx;
                double gy = world.groundY(wx);
                if (!moved) {
                    line.moveTo(sx, gy - cy);
                    moved = true;
                } else {
                    line.lineTo(sx, gy - cy);
                }
            }
            g2.draw(line);
            g2.setStroke(old);
        }

        private void drawCollectibles(Graphics2D g2) {
            double cx = camera.camX;
            double cy = camera.camY;

            for (Coin coin : world.coins) {
                if (!coin.visibleInViewport(cx, cy, viewW, viewH)) continue;
                double sx = coin.x - cx;
                double sy = coin.y - cy;
                double bob = Math.sin(world.time * 2.0 + coin.seed) * 2.0;
                double r = coin.radius;
                Color c1 = new Color(255, 220, 60);
                Color c2 = new Color(255, 255, 180);
                g2.setColor(c1);
                g2.fillOval((int) (sx - r), (int) (sy - r + bob), (int) (2 * r), (int) (2 * r));
                g2.setColor(c2);
                g2.drawOval((int) (sx - r), (int) (sy - r + bob), (int) (2 * r), (int) (2 * r));
                g2.setColor(new Color(255, 255, 255, 200));
                g2.fillOval((int) (sx - r * 0.4), (int) (sy - r * 0.8 + bob), (int) (r * 0.6), (int) (r * 0.4));
            }
        }

        private void drawParticles(Graphics2D g2) {
            double cx = camera.camX;
            double cy = camera.camY;

            for (Particle p : world.particles) {
                if (!p.visibleInViewport(cx, cy, viewW, viewH)) continue;
                double sx = p.x - cx;
                double sy = p.y - cy;
                Color c = new Color(p.r, p.g, p.b, (int) (p.a * 255));
                g2.setColor(c);
                if (p.type == Particle.Type.DUST) {
                    g2.fillOval((int) (sx - p.size * 0.5), (int) (sy - p.size * 0.5), (int) p.size, (int) p.size);
                } else if (p.type == Particle.Type.SPARK) {
                    g2.fillRect((int) (sx - p.size * 0.5), (int) (sy - p.size * 0.5), (int) p.size, (int) p.size);
                } else {
                    g2.fillOval((int) (sx - p.size * 0.5), (int) (sy - p.size * 0.5), (int) p.size, (int) p.size);
                }
            }
        }

        private void drawPlayer(Graphics2D g2) {
            double cx = camera.camX;
            double cy = camera.camY;

            Player p = world.player;
            double sx = p.x - cx;
            double sy = p.y - cy;

            // Shadow
            double groundY = world.groundY(p.x);
            double shadowScale = Math.max(0.1, 1.0 - Math.min(1.0, (groundY - p.y) / 180.0));
            int shadowW = (int) (60 * shadowScale);
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillOval((int) (sx - shadowW * 0.5), (int) (groundY - cy - 6), shadowW, (int) (shadowW * 0.3));

            // Bike and rider transform
            AffineTransform old = g2.getTransform();
            g2.translate(sx, sy);
            g2.rotate(p.rot);

            // Draw bike
            drawBike(g2, p);

            // Draw rider
            drawRider(g2, p);

            g2.setTransform(old);

            // Trails for flips (optional)
            if (world.time % 1.0 < 0.016 && !p.onGround) {
                world.spawnSpark(p.x, p.y, 1);
            }
        }

        private void drawBike(Graphics2D g2, Player p) {
            // Bike geometry
            double wheelR = p.wheelRadius;
            double wheelBase = p.wheelBase; // distance between wheels
            double frameHeight = 22;
            double headTube = 12;

            // Colors
            Color frameColor = new Color(40, 40, 40);
            Color accent = new Color(230, 40, 60);
            Color spoke = new Color(200, 200, 200);
            Color tire = new Color(20, 20, 20);

            // Wheels
            // Rear wheel at (-wheelBase/2, 0), front wheel at (wheelBase/2, 0)
            drawWheel(g2, -wheelBase * 0.5, 0, wheelR, p.wheelSpin, tire, spoke);
            drawWheel(g2, wheelBase * 0.5, 0, wheelR, p.wheelSpin, tire, spoke);

            // Frame
            Graphics2D gg = (Graphics2D) g2.create();
            gg.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gg.setColor(frameColor);

            // Rear axle to seat tube top
            double rearX = -wheelBase * 0.5;
            double frontX = wheelBase * 0.5;
            double seatTopX = -wheelBase * 0.1;
            double seatTopY = -frameHeight;
            double bbX = -wheelBase * 0.2;
            double bbY = -headTube * 0.2;

            // Down tube: head tube to bottom bracket
            double headX = frontX - 8;
            double headY = -frameHeight + 2;

            // Top tube: seat to head
            gg.drawLine((int) rearX, 0, (int) (bbX), (int) (bbY));
            gg.drawLine((int) bbX, (int) bbY, (int) headX, (int) headY);

            gg.setColor(accent);
            gg.drawLine((int) seatTopX, (int) seatTopY, (int) headX, (int) headY); // top tube
            gg.setColor(frameColor);
            gg.drawLine((int) rearX, 0, (int) seatTopX, (int) seatTopY); // seat stay
            gg.drawLine((int) bbX, (int) bbY, (int) rearX, 0); // chain stay

            // Fork
            gg.drawLine((int) frontX, 0, (int) (headX - 4), (int) (headY + 8));

            // Handlebar
            gg.setColor(accent);
            gg.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gg.drawLine((int) (headX - 10), (int) (headY - 6), (int) (headX + 12), (int) (headY - 12));

            // Seat
            gg.setColor(Color.DARK_GRAY);
            gg.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gg.drawLine((int) (seatTopX - 6), (int) (seatTopY - 3), (int) (seatTopX + 8), (int) (seatTopY - 3));

            // Cranks and pedals (rotate a bit with wheelSpin)
            gg.setColor(new Color(100, 100, 100));
            double crankR = 10;
            double pedalLen = 12;
            double crankA = p.wheelSpin * 1.6;
            double cpx = bbX;
            double cpy = bbY;
            gg.fillOval((int) (cpx - 3), (int) (cpy - 3), 6, 6);
            double px1 = cpx + crankR * Math.cos(crankA);
            double py1 = cpy + crankR * Math.sin(crankA);
            double px2 = cpx + crankR * Math.cos(crankA + Math.PI);
            double py2 = cpy + crankR * Math.sin(crankA + Math.PI);
            gg.setStroke(new BasicStroke(2.0f));
            gg.drawLine((int) cpx, (int) cpy, (int) px1, (int) py1);
            gg.drawLine((int) cpx, (int) cpy, (int) px2, (int) py2);

            gg.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gg.drawLine((int) px1, (int) py1, (int) (px1 + pedalLen * Math.cos(crankA)), (int) (py1 + pedalLen * Math.sin(crankA)));
            gg.drawLine((int) px2, (int) py2, (int) (px2 + pedalLen * Math.cos(crankA + Math.PI)), (int) (py2 + pedalLen * Math.sin(crankA + Math.PI)));

            gg.dispose();
        }

        private void drawWheel(Graphics2D g2, double x, double y, double r, double spin, Color tire, Color spoke) {
            Graphics2D gg = (Graphics2D) g2.create();
            gg.translate(x, y);

            // Tire
            gg.setColor(tire);
            gg.fillOval((int) (-r), (int) (-r), (int) (2 * r), (int) (2 * r));

            // Rim
            gg.setColor(new Color(80, 80, 80));
            gg.fillOval((int) (-r * 0.8), (int) (-r * 0.8), (int) (1.6 * r), (int) (1.6 * r));
            gg.setColor(new Color(200, 200, 200));
            gg.fillOval((int) (-r * 0.6), (int) (-r * 0.6), (int) (1.2 * r), (int) (1.2 * r));

            // Spokes
            gg.setColor(spoke);
            gg.setStroke(new BasicStroke(1.2f));
            gg.rotate(spin);
            for (int i = 0; i < 8; i++) {
                gg.drawLine(0, 0, (int) r, 0);
                gg.rotate(Math.PI / 4.0);
            }

            gg.dispose();
        }

        private void drawRider(Graphics2D g2, Player p) {
            Graphics2D gg = (Graphics2D) g2.create();

            // Rider lean based on pitch and control
            double lean = p.riderLean;

            // Body reference points
            double torsoLen = 26;
            double legLen = 28;
            double armLen = 24;

            // Seat position approx
            double seatX = -p.wheelBase * 0.1;
            double seatY = -22;

            // Head tube grip
            double headX = p.wheelBase * 0.5 - 8;
            double headY = -22 + 2;

            // Hip position
            double hipX = seatX + 0;
            double hipY = seatY - 2;

            // Torso direction
            double torsoAngle = -0.5 + lean;
            double chestX = hipX + torsoLen * Math.cos(torsoAngle);
            double chestY = hipY + torsoLen * Math.sin(torsoAngle);

            // Head
            double headR = 7;
            gg.setColor(new Color(240, 210, 170));
            gg.fillOval((int) (chestX - headR), (int) (chestY - headR - 10), (int) (2 * headR), (int) (2 * headR));

            // Arms to handlebar
            gg.setColor(new Color(80, 80, 80));
            gg.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gg.drawLine((int) chestX, (int) (chestY - 6), (int) headX, (int) (headY - 6));

            // Torso
            gg.setColor(new Color(30, 30, 30));
            gg.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gg.drawLine((int) hipX, (int) hipY, (int) chestX, (int) (chestY - 4));

            // Legs to pedals (approx using crank)
            double crankA = p.wheelSpin * 1.6;
            double crankR = 10;
            double bbX = -p.wheelBase * 0.2;
            double bbY = -12 * 0.2;
            double footX = bbX + crankR * Math.cos(crankA);
            double footY = bbY + crankR * Math.sin(crankA);

            gg.setColor(new Color(50, 50, 50));
            gg.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gg.drawLine((int) hipX, (int) hipY, (int) ((hipX + footX) / 2), (int) ((hipY + footY) / 2));
            gg.drawLine((int) ((hipX + footX) / 2), (int) ((hipY + footY) / 2), (int) (footX), (int) (footY));

            gg.dispose();
        }

        private void drawHUD(Graphics2D g2) {
            Player p = world.player;

            // HUD background bar
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRoundRect(16, 16, 380, 88, 12, 12);

            Font f = new Font("SansSerif", Font.BOLD, 16);
            g2.setFont(f);
            g2.setColor(Color.WHITE);

            double kmh = Math.abs(p.vx) * 3.6;
            String line1 = String.format(Locale.US, "Speed: %.1f km/h   Dist: %.1fm", kmh, world.distance / 10.0);
            String line2 = String.format(Locale.US, "Score: %d   Combo: x%.1f   Air: %.2fs", world.score, world.comboMultiplier, world.airTime);

            g2.drawString(line1, 26, 46);
            g2.drawString(line2, 26, 74);

            // Instructions
            g2.setColor(new Color(255, 255, 255, 200));
            g2.drawString("A/D or ←/→: Pedal/Brake | W/↑: Jump | Space: Flip | Shift: Wheelie | R: Reset | P: Pause", 16, viewH - 20);

            // FPS
            g2.setColor(new Color(255, 255, 255, 140));
            g2.drawString("FPS: " + fps, viewW - 100, 28);
        }

        private void drawPauseBanner(Graphics2D g2) {
            String msg = "PAUSED";
            Font f = new Font("SansSerif", Font.BOLD, 64);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(msg);
            int th = fm.getAscent();

            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect(viewW / 2 - tw / 2 - 20, viewH / 2 - th, tw + 40, th + 40, 20, 20);
            g2.setColor(new Color(255, 255, 255));
            g2.drawString(msg, viewW / 2 - tw / 2, viewH / 2 + th / 4);
        }

        // Input events
        @Override
        public void keyTyped(KeyEvent e) { }

        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) input.left = true;
            if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) input.right = true;
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) input.jump = true;
            if (code == KeyEvent.VK_SPACE) input.flip = true;
            if (code == KeyEvent.VK_SHIFT) input.wheelie = true;

            if (code == KeyEvent.VK_P) paused = !paused;
            if (code == KeyEvent.VK_R) world.resetPlayer(false);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();
            if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) input.left = false;
            if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) input.right = false;
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) input.jump = false;
            if (code == KeyEvent.VK_SPACE) input.flip = false;
            if (code == KeyEvent.VK_SHIFT) input.wheelie = false;
        }
    }

    // Simple structure for keyboard input state
    static class Input {
        boolean left, right, jump, flip, wheelie;
    }

    // Camera follows the player with smoothing
    static class Camera {
        double camX, camY;
        double targetX, targetY;

        void update(World world, double dt) {
            Player p = world.player;
            // Center ahead of player based on speed
            double ahead = Math.max(0, Math.min(200, Math.abs(p.vx) * 18));
            targetX = p.x - ahead;
            targetY = p.y - 160;

            // Smooth follow
            camX += (targetX - camX) * Math.min(1.0, dt * 4.0);
            camY += (targetY - camY) * Math.min(1.0, dt * 4.0);
        }
    }

    // Heads-up display data aggregator
    static class HUD {
        void update(World world, double dt) {
            // Could animate multipliers, messages, etc.
        }
    }

    // World contains terrain, player, collectibles, particles, scoring, etc.
    static class World {
        final Player player;
        final List<Ramp> ramps;
        final List<Coin> coins;
        final List<Particle> particles;

        double time = 0.0;

        // Scoring
        int score = 0;
        double comboMultiplier = 1.0;
        double comboTimeLeft = 0.0;
        double airTime = 0.0;
        int flipsInAir = 0;
        double distance = 0.0;

        // Ground function parameters
        final double baseY = 460.0; // baseline ground height
        final double amp1 = 40.0;
        final double amp2 = 20.0;
        final double freq1 = 1.0 / 200.0;
        final double freq2 = 1.0 / 70.0;

        // Generation trackers
        double generatedUntilX = 0.0;

        World() {
            player = new Player();
            ramps = new ArrayList<>();
            coins = new ArrayList<>();
            particles = new ArrayList<>();
        }

        void generateInitial() {
            // Place initial ramps and coins
            generatedUntilX = -200;
            maybeExtendTrack(0, 1280, new Random(12345));
            resetPlayer(true);
        }

        void resetPlayer(boolean hard) {
            player.x = 100;
            player.y = groundY(player.x) - 28;
            player.vx = 120 / 3.6; // 120 km/h -> m/s scaled
            player.vy = 0;
            player.rot = slopeAngle(player.x);
            player.angVel = 0;
            player.onGround = true;
            player.wheelSpin = 0;
            player.riderLean = 0;
            player.crashed = false;
            player.flipAngleAccumulator = 0;
            flipsInAir = 0;
            airTime = 0;
            comboMultiplier = 1.0;
            comboTimeLeft = 0.0;
            if (hard) {
                score = 0;
                distance = 0;
                coins.clear();
                ramps.clear();
                particles.clear();
                generatedUntilX = player.x + 1000;
                Random rng = new Random(12345);
                for (int i = 0; i < 8; i++) {
                    addRamp(rng, 300 + i * 400);
                }
                for (int i = 0; i < 40; i++) {
                    addCoin(rng, 200 + i * 120);
                }
            }
        }

        void update(Input input, double dt) {
            time += dt;

            // Update player physics
            player.update(this, input, dt);

            // Update particles
            updateParticles(dt);

            // Collect coins
            collectCoins();

            // Scoring: distance
            distance = Math.max(distance, player.x);

            // Combo decay
            if (comboTimeLeft > 0) {
                comboTimeLeft -= dt;
                if (comboTimeLeft <= 0) {
                    comboMultiplier = 1.0;
                }
            }
        }

        void updateParticles(double dt) {
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.update(dt);
                if (p.dead()) it.remove();
            }
        }

        void collectCoins() {
            Iterator<Coin> it = coins.iterator();
            while (it.hasNext()) {
                Coin c = it.next();
                double dx = c.x - player.x;
                double dy = c.y - player.y;
                if (dx * dx + dy * dy < (c.radius + 18) * (c.radius + 18)) {
                    // Collect coin
                    score += (int) (100 * comboMultiplier);
                    comboMultiplier = Math.min(10.0, comboMultiplier + 0.1);
                    comboTimeLeft = 3.0;

                    // Pop particle burst
                    for (int i = 0; i < 8; i++) {
                        spawnSpark(c.x, c.y, 5);
                    }

                    it.remove();
                }
            }
        }

        double groundY(double x) {
            // Base ground waves
            double y = baseY
                    - amp1 * Math.sin(x * freq1)
                    - amp2 * Math.sin(x * freq2 * 1.7);

            // Check ramps that raise ground
            // We take max of base ground and any ramp surface
            double maxY = y;
            for (Ramp r : ramps) {
                if (x >= r.x && x <= r.x + r.w) {
                    double ry = r.surfaceY(x);
                    if (ry < maxY) {
                        // Smaller y means visually higher (top of screen), but for collision we want the minimum y (highest surface)
                        maxY = ry;
                    }
                }
            }
            return maxY;
        }

        double slopeAngle(double x) {
            // Derivative of ground for angle
            double dy = -amp1 * freq1 * Math.cos(x * freq1)
                    - amp2 * freq2 * 1.7 * Math.cos(x * freq2 * 1.7);
            // Ramps contribute piecewise slopes
            double rampDy = 0;
            for (Ramp r : ramps) {
                if (x >= r.x && x <= r.x + r.w) {
                    rampDy = r.slopeAt(x);
                    // If ramp is higher than ground here, we adopt ramp slope
                    double base = baseY
                            - amp1 * Math.sin(x * freq1)
                            - amp2 * Math.sin(x * freq2 * 1.7);
                    if (r.surfaceY(x) < base) {
                        dy = rampDy;
                    }
                }
            }
            return Math.atan2(dy, 1.0);
        }

        void maybeExtendTrack(double camX, int viewW, Random rng) {
            double needUntil = camX + viewW + 1200;
            if (generatedUntilX < needUntil) {
                while (generatedUntilX < needUntil) {
                    addRamp(rng, generatedUntilX + 300 + rng.nextInt(400));
                    for (int i = 0; i < 6; i++) {
                        addCoin(rng, generatedUntilX + 100 + i * 60);
                    }
                    generatedUntilX += 600 + rng.nextInt(300);
                }
            }
            // Cleanup old ramps/coins
            double minX = camX - 600;
            ramps.removeIf(r -> r.x + r.w < minX);
            coins.removeIf(c -> c.x < minX);
        }

        void addRamp(Random rng, double x) {
            double w = 120 + rng.nextInt(120);
            double h = 30 + rng.nextInt(40);
            ramps.add(Ramp.makeRollIn(x, groundY(x), w, h, rng.nextBoolean()));
        }

        void addCoin(Random rng, double x) {
            double gy = groundY(x);
            double y = gy - (50 + rng.nextInt(100));
            coins.add(new Coin(x + rng.nextInt(80) - 40, y, 10, rng.nextDouble() * Math.PI * 2));
        }

        void spawnDust(double x, double y, int count) {
            for (int i = 0; i < count; i++) {
                Particle p = Particle.dust(x, y);
                particles.add(p);
            }
        }

        void spawnSpark(double x, double y, int count) {
            for (int i = 0; i < count; i++) {
                Particle p = Particle.spark(x, y);
                particles.add(p);
            }
        }

        void landed(double verticalImpact) {
            // Landing scoring: based on airtime and flips
            if (airTime > 0.2) {
                int base = (int) (airTime * 200);
                int flipBonus = flipsInAir * 300;
                int impactBonus = (int) Math.max(0, 50 - Math.min(50, verticalImpact * 8));
                int total = base + flipBonus + impactBonus;
                score += (int) (total * comboMultiplier);
                comboMultiplier = Math.min(10.0, comboMultiplier + 0.2 + flipsInAir * 0.5);
                comboTimeLeft = 3.5;
            }
            airTime = 0;
            flipsInAir = 0;
        }
    }

    // Player (bike + rider) state and physics
    static class Player {
        double x, y;     // position
        double vx, vy;   // velocity
        double rot;      // rotation (radians)
        double angVel;   // angular velocity
        boolean onGround;
        boolean crashed;

        double wheelBase = 80;     // distance between wheels
        double wheelRadius = 18;
        double wheelSpin = 0;      // for visual rotation
        double riderLean = 0;      // for body animation

        double flipAngleAccumulator = 0; // to count completed flips

        // Physics constants (tuned for feel)
        final double gravity = 750.0;      // px/s^2
        final double accel = 220.0;        // px/s^2
        final double brake = 320.0;        // px/s^2
        final double maxSpeed = 600.0;     // px/s
        final double airRotAccel = 5.8;    // rad/s^2
        final double airRotDamp = 1.6;     // rad/s^2
        final double jumpImpulse = 300.0;  // px/s
        final double groundFriction = 2.2; // general speed damping
        final double slopeAssist = 2.8;    // accelerate downhill
        final double wheelieMax = Math.toRadians(32);
        final double landTolerance = Math.toRadians(34); // max misalignment on landing

        void update(World world, Input in, double dt) {
            if (crashed) {
                // Slow-state while crashed: auto reset
                vy += gravity * dt;
                x += vx * dt;
                y += vy * dt;
                angVel *= Math.pow(0.98, dt * 60);
                rot += angVel * dt;
                if (y > world.groundY(x) + 200) {
                    world.resetPlayer(false);
                }
                return;
            }

            double groundY = world.groundY(x);
            double groundAngle = world.slopeAngle(x);

            // Controls and motion
            if (onGround) {
                // Accelerate/brake
                double targetAccel = 0;
                if (in.left) targetAccel -= accel;
                if (in.right) targetAccel += accel;

                // Add slope gravity assistance
                targetAccel += slopeAssist * gravity * Math.sin(groundAngle) * 0.2;

                // Friction
                double fric = -vx * groundFriction;

                double ax = targetAccel + fric;
                vx += ax * dt;

                // Clamp speed
                vx = clamp(vx, -maxSpeed, maxSpeed);

                // Jump
                if (in.jump) {
                    // Bunny hop: scale with speed
                    double hop = jumpImpulse * (0.6 + 0.4 * clamp(Math.abs(vx) / (maxSpeed * 0.6), 0.0, 1.0));
                    vy = -hop;
                    onGround = false;
                    // Dust on takeoff
                    world.spawnDust(x, y, 10);
                } else {
                    // Snap to ground
                    y = groundY - (wheelRadius + 10);
                    vy = 0;
                }

                // Orientation: align to slope unless wheelie
                double targetRot = groundAngle;
                if (in.wheelie && Math.abs(vx) > 60) {
                    // Lift front wheel: rotate up to wheelieMax relative to slope
                    targetRot = groundAngle + wheelieMax;
                }
                rot += (targetRot - rot) * Math.min(1.0, dt * 10.0);
                angVel = 0;

                // Wheel spinning based on speed
                wheelSpin += (vx / wheelRadius) * dt;

                // Subtle rider lean based on acceleration
                double leanTarget = 0.0;
                if (in.right) leanTarget = -0.2;
                if (in.left) leanTarget = 0.2;
                riderLean += (leanTarget - riderLean) * Math.min(1.0, dt * 5.0);

                // Leave dust when fast
                if (Math.abs(vx) > 220 && world.time % 0.05 < dt) {
                    world.spawnDust(x - Math.cos(groundAngle) * 20, groundY - 4, 1);
                }

            } else {
                // Airborne
                // Gravity
                vy += gravity * dt;

                // Air control
                double torque = 0;
                if (in.flip) {
                    // Hold space to rotate backwards (backflip)
                    torque -= airRotAccel * 1.0;
                }
                if (!in.flip) {
                    // Minor auto-damping
                    if (angVel > 0) torque -= airRotDamp * 0.4;
                    if (angVel < 0) torque += airRotDamp * 0.4;
                }
                // Allow subtle directional torque with left/right
                if (in.left) torque += airRotAccel * 0.5;
                if (in.right) torque -= airRotAccel * 0.5;

                angVel += torque * dt;
                angVel = clamp(angVel, -8.0, 8.0);
                rot += angVel * dt;

                // Integrate position
                x += vx * dt;
                y += vy * dt;

                // Spin wheels visually in air a bit
                wheelSpin += (vx / wheelRadius) * dt * 0.6;

                // Track flips in air
                flipAngleAccumulator += angVel * dt;
                while (flipAngleAccumulator > Math.PI * 2) {
                    flipAngleAccumulator -= Math.PI * 2;
                    world.flipsInAir++;
                }
                while (flipAngleAccumulator < -Math.PI * 2) {
                    flipAngleAccumulator += Math.PI * 2;
                    world.flipsInAir++;
                }

                world.airTime += dt;
                world.comboTimeLeft = 2.5; // keep combo alive while airborne
            }

            // Horizontal integrate when on ground too (after modifications)
            if (onGround) {
                x += vx * dt;
            }

            // Landing detection when airborne -> ground
            if (!onGround) {
                double newGroundY = world.groundY(x);
                if (y >= newGroundY - (wheelRadius + 10)) {
                    // Impact: determine if landed or crashed
                    double misalign = angleDiff(rot, world.slopeAngle(x));
                    double verticalImpact = Math.abs(vy);

                    if (Math.abs(misalign) > world.player.landTolerance || verticalImpact > 420) {
                        // Crash
                        crashed = true;
                        onGround = false;
                        // Blast dust
                        world.spawnDust(x, newGroundY, 30);
                        // Lose some score
                        world.score = Math.max(0, world.score - 200);
                        world.comboMultiplier = 1.0;
                        world.comboTimeLeft = 0.0;
                    } else {
                        // Successful landing
                        onGround = true;
                        y = newGroundY - (wheelRadius + 10);
                        vy = 0;
                        // Reduce angular velocity and align
                        rot = world.slopeAngle(x);
                        angVel = 0;
                        // Landing dust
                        world.spawnDust(x, y + wheelRadius, 12);
                        // Award score for airtime/flips
                        world.landed(verticalImpact);
                    }
                } else {
                    // Still in air: integrate horizontal
                    x += 0; // already applied
                }
            }

            // Clamp rotation to sensible range for drawing
            if (rot < -Math.PI * 4) rot += Math.PI * 8;
            if (rot > Math.PI * 4) rot -= Math.PI * 8;

            // Fail-safe: if we fall far below world, reset
            if (y > world.baseY + 1000) {
                world.resetPlayer(false);
            }
        }

        static double angleDiff(double a, double b) {
            double d = a - b;
            while (d > Math.PI) d -= Math.PI * 2;
            while (d < -Math.PI) d += Math.PI * 2;
            return d;
        }

        static double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }
    }

    // Ramp: piecewise triangular/roll-in feature superimposed on ground
    static class Ramp {
        double x;   // start x
        double y;   // base ground reference y at start
        double w;   // width
        double h;   // height (upwards)
        boolean leftToRight; // true means slope up then down left->right

        // Simple surface y: triangular bump
        double surfaceY(double wx) {
            double t = (wx - x) / w;
            t = clamp01(t);
            // Triangle profile
            if (t < 0.5) {
                return y - h * (t / 0.5);
            } else {
                return y - h * (1.0 - (t - 0.5) / 0.5);
            }
        }

        double slopeAt(double wx) {
            double t = (wx - x) / w;
            if (t <= 0.0001 || t >= 0.9999) return 0;
            if (t < 0.5) {
                // rising
                return -h / (w * 0.5);
            } else {
                // falling
                return h / (w * 0.5);
            }
        }

        boolean visibleInViewport(double camX, double camY, int vw, int vh) {
            double sx = x - camX;
            return sx < vw + 200 && sx + w > -200;
        }

        static Ramp makeRollIn(double x, double baseY, double width, double height, boolean ltr) {
            Ramp r = new Ramp();
            r.x = x;
            r.y = baseY - 6; // embed slightly
            r.w = width;
            r.h = height;
            r.leftToRight = ltr;
            return r;
        }
    }

    // Visual helper drawing for ramps
    static class ShapeBuilder {
        static void drawRamp(Graphics2D g2, Ramp r, double cx, double cy) {
            Path2D p = new Path2D.Double();
            double x0 = r.x - cx;
            double x1 = r.x + r.w - cx;
            double y0 = r.y - cy;
            double xm = (x0 + x1) * 0.5;
            double ym = r.y - r.h - cy;

            p.moveTo(x0, r.y - cy);
            p.lineTo(xm, ym);
            p.lineTo(x1, r.y - cy);
            p.lineTo(x1, r.y + 60 - cy);
            p.lineTo(x0, r.y + 60 - cy);
            p.closePath();

            g2.fill(p);

            // Edge line
            Graphics2D gg = (Graphics2D) g2.create();
            gg.setStroke(new BasicStroke(2f));
            gg.setColor(new Color(110, 100, 80));
            Path2D edge = new Path2D.Double();
            edge.moveTo(x0, r.y - cy);
            edge.lineTo(xm, ym);
            edge.lineTo(x1, r.y - cy);
            gg.draw(edge);
            gg.dispose();
        }
    }

    // Coins to collect
    static class Coin {
        double x, y, radius;
        double seed;

        Coin(double x, double y, double radius, double seed) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.seed = seed;
        }

        boolean visibleInViewport(double cx, double cy, int vw, int vh) {
            double sx = x - cx;
            double sy = y - cy;
            return sx > -50 && sx < vw + 50 && sy > -50 && sy < vh + 50;
        }
    }

    // Particles for dust/sparks
    static class Particle {
        enum Type { DUST, SPARK, POP }

        Type type;
        double x, y;
        double vx, vy;
        double life, maxLife;
        double size;
        float r, g, b, a;

        static Particle dust(double x, double y) {
            Particle p = new Particle();
            p.type = Type.DUST;
            p.x = x + rnd(-8, 8);
            p.y = y + rnd(-2, 2);
            p.vx = rnd(-40, 40);
            p.vy = rnd(-120, -60);
            p.maxLife = rnd(0.4, 0.8);
            p.life = p.maxLife;
            p.size = rnd(2, 5);
            p.r = 120; p.g = 110; p.b = 90; p.a = 1f;
            return p;
        }

        static Particle spark(double x, double y) {
            Particle p = new Particle();
            p.type = Type.SPARK;
            p.x = x + rnd(-4, 4);
            p.y = y + rnd(-4, 4);
            p.vx = rnd(-180, 180);
            p.vy = rnd(-180, 0);
            p.maxLife = rnd(0.2, 0.5);
            p.life = p.maxLife;
            p.size = rnd(2, 4);
            p.r = 255; p.g = 230; p.b = 120; p.a = 1f;
            return p;
        }

        void update(double dt) {
            // Simple gravity and damping
            vy += 600 * dt;
            x += vx * dt;
            y += vy * dt;
            vx *= Math.pow(0.98, dt * 60);
            life -= dt;
            a = (float) Math.max(0.0, life / maxLife);
        }

        boolean dead() {
            return life <= 0;
        }

        boolean visibleInViewport(double cx, double cy, int vw, int vh) {
            double sx = x - cx;
            double sy = y - cy;
            return sx > -50 && sx < vw + 50 && sy > -50 && sy < vh + 50;
        }

        static double rnd(double a, double b) {
            return a + Math.random() * (b - a);
        }
    }

    // Utility
    static double clamp01(double t) {
        if (t < 0) return 0;
        if (t > 1) return 1;
        return t;
    }
}