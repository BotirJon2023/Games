import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CrossCountryRacingAdventure extends JPanel
        implements Runnable, KeyListener {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 700;
    private static final int TRACK_LENGTH = 10000;

    private static final boolean VS_COMPUTER = true;

    private Thread gameThread;
    private boolean running = true;

    private Racer player1;
    private Racer player2;

    private boolean w, s, up, down;

    private double cameraX = 0;
    private long startTime;

    public CrossCountryRacingAdventure() {

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        setFocusable(true);
        addKeyListener(this);

        player1 = new Racer(100, 290, Color.RED);
        player2 = new Racer(100, 430, Color.BLUE);

        startTime = System.currentTimeMillis();
    }

    public void start() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {

        long lastTime = System.nanoTime();
        double ns = 1000000000.0 / 60.0;
        double delta = 0;

        while (running) {

            long now = System.nanoTime();

            delta += (now - lastTime) / ns;
            lastTime = now;

            while (delta >= 1) {
                updateGame();
                repaint();
                delta--;
            }

            try {
                Thread.sleep(2);
            } catch (Exception ignored) {
            }
        }
    }

    private void updateGame() {

        if (w) player1.accelerate();
        if (s) player1.brake();

        if (!VS_COMPUTER) {

            if (up) player2.accelerate();
            if (down) player2.brake();

        } else {

            if (player2.speed < 7.5)
                player2.accelerate();
        }

        player1.update();
        player2.update();

        cameraX = Math.max(player1.x, player2.x) - 250;

        if (cameraX < 0)
            cameraX = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        drawSky(g2);
        drawMountains(g2);
        drawClouds(g2);
        drawGround(g2);

        Graphics2D world = (Graphics2D) g2.create();
        world.translate(-cameraX, 0);

        drawTrack(world);
        drawTrees(world);

        player1.draw(world);
        player2.draw(world);

        world.dispose();

        drawHUD(g2);
    }

    private void drawSky(Graphics2D g) {

        GradientPaint sky =
                new GradientPaint(
                        0, 0,
                        new Color(80, 180, 255),
                        0, HEIGHT,
                        Color.WHITE);

        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawClouds(Graphics2D g) {

        int shift = (int) (cameraX * 0.1);

        g.setColor(new Color(255, 255, 255, 180));

        for (int i = -500; i < WIDTH + 1000; i += 250) {

            int x = i - shift;

            g.fillOval(x, 60, 100, 50);
            g.fillOval(x + 30, 40, 100, 60);
            g.fillOval(x + 70, 60, 100, 50);
        }
    }

    private void drawMountains(Graphics2D g) {

        int shift = (int) (cameraX * 0.2);

        g.setColor(new Color(120, 120, 140));

        for (int i = -1000; i < TRACK_LENGTH; i += 500) {

            int x = i - shift;

            Polygon p = new Polygon();

            p.addPoint(x, 400);
            p.addPoint(x + 250, 150);
            p.addPoint(x + 500, 400);

            g.fillPolygon(p);
        }
    }

    private void drawGround(Graphics2D g) {

        g.setColor(new Color(70, 170, 80));
        g.fillRect(0, 500, WIDTH, 250);
    }

    private void drawTrees(Graphics2D g) {

        for (int i = 0; i < TRACK_LENGTH; i += 180) {

            int x = i + 60;

            g.setColor(new Color(110, 60, 20));
            g.fillRect(x, 430, 15, 60);

            g.setColor(new Color(30, 150, 50));

            g.fillOval(x - 25, 380, 70, 70);
        }
    }

    private void drawTrack(Graphics2D g) {

        g.setColor(new Color(120, 90, 60));
        g.fillRoundRect(
                0,
                250,
                TRACK_LENGTH,
                260,
                40,
                40);

        g.setColor(Color.WHITE);

        for (int i = 0; i < TRACK_LENGTH; i += 120) {

            g.fillRect(i, 375, 60, 6);
        }

        g.setColor(Color.YELLOW);

        g.fillRect(
                TRACK_LENGTH - 20,
                250,
                10,
                260);
    }

    private void drawHUD(Graphics2D g) {

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 22));

        g.drawString(
                "RED Speed: " + (int) (player1.speed * 20),
                20,
                35);

        g.drawString(
                "BLUE Speed: " + (int) (player2.speed * 20),
                20,
                70);

        if (player1.x >= TRACK_LENGTH) {

            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("PLAYER 1 WINS!", 350, 120);
        }

        if (player2.x >= TRACK_LENGTH) {

            g.setColor(Color.BLUE);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("PLAYER 2 WINS!", 350, 120);
        }
    }

    class Racer {

        double x;
        double y;

        double speed;
        Color color;

        double wheelRotation = 0;

        Racer(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }

        void accelerate() {

            speed += 0.15;

            if (speed > 8)
                speed = 8;
        }

        void brake() {

            speed -= 0.25;

            if (speed < 0)
                speed = 0;
        }

        void update() {

            x += speed;

            wheelRotation += speed * 0.4;

            speed *= 0.985;
        }

        void draw(Graphics2D g) {

            double bounce =
                    Math.sin(wheelRotation) * 2;

            int drawX = (int) x;
            int drawY = (int) (y + bounce);

            g.setColor(color);
            g.fillRoundRect(
                    drawX,
                    drawY,
                    90,
                    35,
                    20,
                    20);

            g.setColor(Color.WHITE);
            g.fillRoundRect(
                    drawX + 25,
                    drawY + 5,
                    35,
                    15,
                    10,
                    10);

            g.setColor(Color.BLACK);

            g.fillOval(
                    drawX + 8,
                    drawY + 24,
                    20,
                    20);

            g.fillOval(
                    drawX + 62,
                    drawY + 24,
                    20,
                    20);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_W -> w = true;
            case KeyEvent.VK_S -> s = true;

            case KeyEvent.VK_UP -> up = true;
            case KeyEvent.VK_DOWN -> down = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_W -> w = false;
            case KeyEvent.VK_S -> s = false;

            case KeyEvent.VK_UP -> up = false;
            case KeyEvent.VK_DOWN -> down = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame frame =
                    new JFrame(
                            "Cross Country Racing Adventure");

            CrossCountryRacingAdventure game =
                    new CrossCountryRacingAdventure();

            frame.add(game);
            frame.pack();

            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);

            frame.setVisible(true);

            game.start();
        });
    }
}