import Ball.Ball;
import Pin.Pin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.Timer;


public class BowlingSimulator extends JPanel implements ActionListener, MouseListener, MouseMotionListener {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final int LANE_Y = 100;
    private static final int LANE_HEIGHT = 500;
    private static final int PIN_BASE_Y = LANE_Y + 80;
    private static final int BALL_START_X = 500;
    private static final int BALL_START_Y = LANE_Y + LANE_HEIGHT - 80;

    // Game state
    private Ball ball;
    private List<Pin> pins;
    private List<Pin> fallenPins;
    private Timer timer;
    private boolean rolling = false;
    private boolean gameOver = false;

    // Scoring
    private int currentFrame = 0;
    private int rollInFrame = 0;
    private int[] frameScores = new int[10];
    private int[] rollScores = new int[21]; // Max 21 rolls (10th frame can have 3)
    private int rollIndex = 0;
    private int totalScore = 0;

    // Mouse control
    private Point mouseStart;
    private Point mouseEnd;
    private double power = 0;
    private double angle = 0;

    public BowlingSimulator() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(40, 40, 60));
        addMouseListener(this);
        addMouseMotionListener(this);

        timer = new Timer(16, this); // ~60 FPS
        initGame();
    }

    private void initGame() {
        ball = new Ball(BALL_START_X, BALL_START_Y);
        pins = new ArrayList<>();
        fallenPins = new ArrayList<>();

        // Create 10 pins in classic triangle
        double[][] positions = {
                {500, PIN_BASE_Y},                    // Pin 1 (head)
                {470, PIN_BASE_Y + 40}, {485, PIN_BASE_Y + 40}, {515, PIN_BASE_Y + 40}, // Row 2
                {455, PIN_BASE_Y + 80}, {485, PIN_BASE_Y + 80}, {515, PIN_BASE_Y + 80}, {545, PIN_BASE_Y + {440, PIN_BASE_Y + 120}, {470, PIN_BASE_Y + 120}, {500, PIN_BASE_Y + 120}, {530, PIN_BASE_Y + 120}, {560, PIN_BASE_Y + 120} // Row 4
        };

        for (double[] pos : positions) {
            pins.add(new Pin((int) pos[0], (int) pos[1]));
        }

        rolling = false;
        mouseStart = null;
        mouseEnd = null;
        power = 0;
        angle = 0;
    }

        private void resetForNextFrame () {
            ball = new Ball(BALL_START_X, BALL_START_Y);
            fallenPins.clear();
            for (Pin pin : pins) {
                pin.reset();
            }
            rolling = false;
            mouseStart = null;
            mouseEnd = null;
            repaint();
        }

        @Override
        protected void paintComponent (Graphics g){
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw lane
            g2d.setColor(new Color(180, 140, 100));
            g2d.fillRect(200, LANE_Y, 600, LANE_HEIGHT);
            g2d.setColor(new Color(150, 110, 70));
            for (int i = 0; i < 12; i++) {
                g2d.drawLine(200, LANE_Y + i * 40, 800, LANE_Y + i * 40);
            }

            // Draw gutters
            g2d.setColor(new Color(50, 50, 50));
            g2d.fillRect(100, LANE_Y, 100, LANE_HEIGHT);
            g2d.fillRect(800, LANE_Y, 100, LANE_HEIGHT);

            // Draw approach dots
            g2d.setColor(Color.WHITE);
            for (int i = 0; i < 7; i++) {
                int x = 350 + i * 30;
                g2d.fillOval(x, BALL_START_Y + 50, 10, 20);
            }

            // Draw pins
            for (Pin pin : pins) {
                pin.draw(g2d);
            }

            // Draw fallen pins
            for (Pin pin : fallenPins) {
                pin.draw(g2d);
            }

            // Draw ball
            if (ball != null) {
                ball.draw(g2d);
            }

            // Draw aim line when dragging
            if (mouseStart != null && mouseEnd != null && !rolling) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(mouseStart.x, mouseStart.y, mouseEnd.x, mouseEnd.y);

                // Power indicator
                double dist = mouseStart.distance(mouseEnd);
                String powerText = String.format("Power: %.0f%%", Math.min(dist / 2, 100));
                g2d.setColor(Color.CYAN);
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                g2d.drawString(powerText, mouseEnd.x + 10, mouseEnd.y - 10);
            }

            drawScoreboard(g2d);
        }

        private void drawScoreboard (Graphics2D g2d){
            g2d.setColor(new Color(20, 20, 40, 220));
            g2d.fillRoundRect(50, 20, WIDTH - 100, 60, 20, 20);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.BOLD, 24));
            g2d.drawString("BOWLING SIMULATOR", 380, 55);

            // Frame boxes
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            for (int i = 0; i < 10; i++) {
                int x = 100 + i * 80;
                g2d.setColor(Color.DARK_GRAY);
                g2d.fillRoundRect(x, 100, 70, 90, 10, 10);
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.drawRoundRect(x, 100, 70, 90, 10, 10);

                // Frame number
                g2d.setColor(Color.YELLOW);
                g2d.drawString(String.valueOf(i + 1), x + 30, 120);

                // Rolls
                if (rollIndex > i * 2) {
                    String r1 = rollScores[i * 2] == 10 && i < 9 ? "X" : String.valueOf(rollScores[i * 2]);
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(r1, x + 10, 150);
                }
                if (rollIndex > i * 2 + 1 && (rollScores[i * 2] + (i < 9 ? rollScores[i * 2 + 1] : 0) < 10 || i == 9)) {
                    String r2 = rollScores[i * 2 + 1] == (10 - rollScores[i * 2]) ? "/" : String.valueOf(rollScores[i * 2 + 1]);
                    g2d.drawString(r2, x + 40, 150);
                }

                // Frame total
                if (frameScores[i] > 0) {
                    g2d.setColor(Color.CYAN);
                    g2d.setFont(new Font("Arial", Font.BOLD, 22));
                    g2d.drawString(String.valueOf(frameScores[i]), x + 15, 180);
                }
            }

            // Current total
            g2d.setColor(Color.GREEN);
            g2d.setFont(new Font("Arial", Font.BOLD, 32));
            g2d.drawString("TOTAL: " + totalScore, WIDTH - 250, 50);

            if (gameOver) {
                g2d.setColor(new Color(255, 255, 100, 200));
                g2d.setFont(new Font("Arial", Font.BOLD, 60));
                FontMetrics fm = g2d.getFontMetrics();
                String msg = "GAME OVER - Final Score: " + totalScore;
                int w = fm.stringWidth(msg);
                g2d.drawString(msg, (WIDTH - w) / 2, HEIGHT / 2);
            }

            // Instructions
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            g2d.drawString("Click and drag from ball to aim & set power → Release to roll", 150, HEIGHT - 30);
        }

        @Override
        public void actionPerformed (ActionEvent e){
            if (rolling && ball != null) {
                ball.update();

                // Check collision with pins
                Iterator<Pin> iter = pins.iterator();
                while (iter.hasNext()) {
                    Pin pin = iter.next();
                    if (!pin.isFallen() && ball.collidesWith(pin)) {
                        pin.knockDown(ball.vx * 0.3, ball.vy * 0.3);
                        fallenPins.add(pin);
                        iter.remove();
                        ball.vx *= 0.7; // Lose some energy
                    }
                }

                // Pin-to-pin collisions
                for (int i = 0; i < fallenPins.size(); i++) {
                    Pin p1 = fallenPins.get(i);
                    p1.update();
                    for (int j = i + 1; j < fallenPins.size(); j++) {
                        Pin p2 = fallenPins.get(j);
                        if (p1.collidesWith(p2)) {
                            double dx = p2.x - p1.x;
                            double dy = p2.y - p1.y;
                            double dist = Math.sqrt(dx * dx + dy * dy);
                            if (dist < Pin.WIDTH) {
                                double nx = dx / dist;
                                double ny = dy / dist;
                                p1.vx -= nx * 3;
                                p1.vy -= ny * 3;
                                p2.vx += nx * 3;
                                p2.vy += ny * 3;
                            }
                        }
                    }
                }

                // Stop ball if too slow or off lane
                if (ball.y > BALL_START_Y + 100 || Math.abs(ball.vx) < 0.5) {
                    finishRoll();
                }

                repaint();
            }
        }

        private void finishRoll () {
            timer.stop();
            rolling = false;

            int pinsDown = 10 - pins.size();
            rollScores[rollIndex++] = pinsDown;

            boolean isStrike = (pinsDown == 10 && rollInFrame == 0);
            boolean isSpare = (pinsDown + (rollInFrame == 1 ? rollScores[rollIndex - 2] : 0) == 10 && rollInFrame == 1);

            if (isStrike && currentFrame < 9) {
                rollInFrame = 0;
                currentFrame++;
            } else {
                rollInFrame++;
                if (rollInFrame == 2 || (currentFrame == 9 && pinsDown == 10)) {
                    rollInFrame = 0;
                    currentFrame++;
                }
            }

            // Calculate scores with look-ahead
            calculateScores();

            if (currentFrame >= 10 && rollInFrame == 0 && (rollIndex < 20 || rollScores[18] + rollScores[19] < 10)) {
                gameOver = true;
            }

            if (!gameOver) {
                Timer delay = new Timer(1500, evt -> resetForNextFrame());
                delay.setRepeats(false);
                delay.start();
            }

            repaint();
        }

        private void calculateScores () {
            totalScore = 0;
            int roll = 0;
            for (int frame = 0; frame < 10; frame++) {
                if (rollScores[roll] == 10) { // Strike
                    int bonus1 = (roll + 1 < rollIndex) ? rollScores[roll + 1] : 0;
                    int bonus2 = (roll + 2 < rollIndex) ? rollScores[roll + 2] : 0;
                    if (bonus1 == 10 && roll + 2 < rollIndex) bonus2 = rollScores[roll + 2];
                    frameScores[frame] = 10 + bonus1 + bonus2;
                    roll++;
                } else if (roll + 1 < rollIndex && rollScores[roll] + rollScores[roll + 1] == 10) { // Spare
                    int bonus = (roll + 2 < rollIndex) ? rollScores[roll + 2] : 0;
                    frameScores[frame] = 10 + bonus;
                    roll += 2;
                } else {
                    frameScores[frame] = (roll < rollIndex ? rollScores[roll] : 0) + (roll + 1 < rollIndex ? rollScores[roll + 1] : 0);
                    roll += 2;
                }
                totalScore += frameScores[frame];
            }
        }

        // Mouse events for aiming
        @Override
        public void mousePressed (MouseEvent e){
            if (!rolling && !gameOver && ball != null) {
                if (e.getPoint().distance(ball.x, ball.y) < 40) {
                    mouseStart = e.getPoint();
                }
            }
        }

        @Override
        public void mouseReleased (MouseEvent e){
            if (mouseStart != null && !rolling && !gameOver) {
                mouseEnd = e.getPoint();
                double dx = mouseStart.x - mouseEnd.x;
                double dy = mouseStart.y - mouseEnd.y;
                power = Math.min(mouseStart.distance(mouseEnd)) / 2.0;
                power = Math.min(power, 18.0);
                angle = Math.atan2(dy, dx);

                ball.vx = power * Math.cos(angle);
                ball.vy = power * Math.sin(angle) * 0.5; // Less vertical speed

                rolling = true;
                timer.start();
                mouseStart = null;
                mouseEnd = null;
            }
        }

        @Override
        public void mouseDragged (MouseEvent e){
            if (mouseStart != null) {
                mouseEnd = e.getPoint();
                repaint();
            }
        }

        @Override public void mouseMoved (MouseEvent e){
        }
        @Override public void mouseClicked (MouseEvent e){
        }
        @Override public void mouseEntered (MouseEvent e){
        }
        @Override public void mouseExited (MouseEvent e){
        }

        // Ball class
        static class Ball {
            int x, y;
            double vx = 0, vy = 0;
            static final int RADIUS = 20;

            Ball(int x, int y) {
                this.x = x;
                this.y = y;
            }

            void update() {
                vx *= 0.98; // Friction
                vy *= 0.98;
                x += vx;
                y += vy;

                // Lane boundaries
                if (x < 220) {
                    x = 220;
                    vx = -vx * 0.6;
                }
                if (x > 780) {
                    x = 780;
                    vx = -vx * 0.6;
                }
            }

            boolean collidesWith(Pin pin) {
                double dx = x - pin.x;
                double dy = y - pin.y;
                return Math.sqrt(dx * dx + dy * dy) < RADIUS + Pin.WIDTH / 2;
            }

            void draw(Graphics2D g) {
                g.setColor(Color.WHITE);
                g.fillOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);
                g.setColor(Color.RED);
                g.fillOval(x - 8, y - 8, 16, 16);
                g.setColor(Color.BLACK);
                g.drawOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);
            }
        }

        // Pin class
        static class Pin {
            int x, y;
            double vx = 0, vy = 0;
            double angle = 0;
            double angularVelocity = 0;
            boolean fallen = false;
            static final int WIDTH = 30;
            static final int HEIGHT = 80;

            Pin(int x, int y) {
                this.x = x;
                this.y = y;
            }

            void knockDown(double impulseX, double impulseY) {
                vx = impulseX + (Math.random() - 0.5) * 4;
                vy = impulseY - 3 - Math.random() * 2;
                angularVelocity = (Math.random() - 0.5) * 0.3;
                fallen = true;
            }

            void update() {
                if (fallen) {
                    vx *= 0.96;
                    vy *= 0.96;
                    vy += 0.3; // Gravity
                    x += vx;
                    y += vy;
                    angle += angularVelocity;
                    angularVelocity *= 0.96;

                    // Ground friction
                    if (y > PIN_BASE_Y + 200) {
                        vy = 0;
                        vx *= 0.8;
                    }
                }
            }

            boolean isFallen() {
                return fallen;
            }

            void reset() {
                fallen = false;
                vx = vy = angularVelocity = 0;
                angle = 0;
            }

            boolean collidesWith(Pin other) {
                double dx = x - other.x;
                double dy = y - other.y;
                return Math.sqrt(dx * dx + dy * dy) < WIDTH;
            }

            void draw(Graphics2D g) {
                AffineTransform old = g.getTransform();
                g.translate(x, y);
                g.rotate(angle);

                // Pin body
                g.setColor(fallen ? new Color(200, 180, 140) : Color.WHITE);
                RoundRectangle2D body = new RoundRectangle2D.Double(-WIDTH / 2, -HEIGHT / 2, WIDTH, HEIGHT, 15, 15);
                g.fill(body);

                // Red neck band
                g.setColor(Color.RED);
                g.fillRect(-10, -10, 20, 20);

                // White stripes
                g.setColor(Color.WHITE);
                g.fillRect(-12, -30, 24, 8);
                g.fillRect(-12, 10, 24, 8);

                g.setTransform(old);
            }
        }

        public static void main (String[]args){
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("Bowling Simulator - Full Animated Game");
                BowlingSimulator game = new BowlingSimulator();
                frame.add(game);
                frame.pack();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLocationRelativeTo(null);
                frame.setResizable(false);
                frame.setVisible(true);

                // Add restart button
                JButton restart = new JButton("New Game");
                restart.setFont(new Font("Arial", Font.BOLD, 16));
                restart.addActionListener(e -> {
                    game.currentFrame = 0;
                    game.rollInFrame = 0;
                    game.rollIndex = 0;
                    game.totalScore = 0;
                    Arrays.fill(game.frameScores, 0);
                    Arrays.fill(game.rollScores, 0);
                    game.gameOver = false;
                    game.initGame();
                    game.repaint();
                });
                frame.add(restart, BorderLayout.SOUTH);
            });
        }
    }
}