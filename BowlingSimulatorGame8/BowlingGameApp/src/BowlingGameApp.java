import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class BowlingGameApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bowling Simulator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            GamePanel panel = new GamePanel();
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.requestFocusInWindow();
        });
    }

    // === GamePanel: handles drawing, input, and animation ===
    static class GamePanel extends JPanel implements ActionListener, KeyListener {

        private static final int WIDTH = 800;
        private static final int HEIGHT = 600;
        private static final int LANE_TOP = 80;
        private static final int LANE_BOTTOM = 540;
        private static final int LANE_LEFT = 250;
        private static final int LANE_RIGHT = 550;

        private Timer timer;
        private Ball ball;
        private List<Pin> pins;

        private boolean rolling = false;
        private BowlingGame game;
        private String statusText = "Press SPACE to roll";

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setBackground(new Color(10, 80, 10)); // dark green background
            setFocusable(true);
            addKeyListener(this);

            game = new BowlingGame();
            resetPins();
            resetBall();

            timer = new Timer(16, this); // ~60 FPS
            timer.start();
        }

        private void resetBall() {
            int startX = (LANE_LEFT + LANE_RIGHT) / 2;
            int startY = LANE_BOTTOM - 40;
            ball = new Ball(startX, startY, 18);
            ball.setVelocity(0, -8); // straight up the lane
        }

        private void resetPins() {
            pins = new ArrayList<>();
            int baseY = LANE_TOP + 80;
            int centerX = (LANE_LEFT + LANE_RIGHT) / 2;
            int spacing = 30;
            int rows = 4;
            int count = 0;
            for (int row = 0; row < rows; row++) {
                int pinsInRow = row + 1;
                int rowY = baseY + row * spacing;
                int rowWidth = (pinsInRow - 1) * spacing;
                int rowStartX = centerX - rowWidth / 2;
                for (int i = 0; i < pinsInRow; i++) {
                    int px = rowStartX + i * spacing;
                    pins.add(new Pin(px, rowY, 10, count++));
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (rolling) {
                ball.update();
                checkCollisions();
                if (ball.getY() < LANE_TOP - 50) {
                    endRoll();
                }
            }
            repaint();
        }

        private void endRoll() {
            rolling = false;
            int fallen = 0;
            for (Pin p : pins) {
                if (!p.isStanding()) {
                    fallen++;
                }
            }
            game.roll(fallen);
            statusText = "Roll knocked " + fallen +
                    " pins. Frame: " + game.getCurrentFrameIndex() +
                    " Score: " + game.score() +
                    "  (SPACE to roll again)";
            resetPins();
            resetBall();
        }

        private void checkCollisions() {
            for (Pin p : pins) {
                if (p.isStanding()) {
                    double dx = ball.getX() - p.getX();
                    double dy = ball.getY() - p.getY();
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < ball.getRadius() + p.getRadius()) {
                        p.knockDown();
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            // Anti-aliasing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw lane
            g2.setColor(new Color(180, 140, 80));
            g2.fillRect(LANE_LEFT, LANE_TOP, LANE_RIGHT - LANE_LEFT, LANE_BOTTOM - LANE_TOP);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(LANE_LEFT, LANE_TOP, LANE_RIGHT - LANE_LEFT, LANE_BOTTOM - LANE_TOP);

            // Draw pins
            for (Pin p : pins) {
                p.draw(g2);
            }

            // Draw ball
            ball.draw(g2);

            // Draw HUD
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString(statusText, 20, 30);
            g2.drawString("Total Score: " + game.score(), 20, 55);

            g2.dispose();
        }

        @Override
        public void keyTyped(KeyEvent e) { }

        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            if (code == KeyEvent.VK_SPACE && !rolling) {
                rolling = true;
                statusText = "Ball rolling...";
            } else if (code == KeyEvent.VK_LEFT && !rolling) {
                ball.setX(ball.getX() - 10);
            } else if (code == KeyEvent.VK_RIGHT && !rolling) {
                ball.setX(ball.getX() + 10);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) { }
    }

    // === Ball class ===
    static class Ball {
        private double x;
        private double y;
        private double vx;
        private double vy;
        private int radius;
        private Color color = Color.BLUE;

        public Ball(double x, double y, int radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }

        public void setVelocity(double vx, double vy) {
            this.vx = vx;
            this.vy = vy;
        }

        public void update() {
            x += vx;
            y += vy;
        }

        public void draw(Graphics2D g2) {
            g2.setColor(color);
            g2.fillOval((int) (x - radius), (int) (y - radius),
                    radius * 2, radius * 2);
            g2.setColor(Color.BLACK);
            g2.drawOval((int) (x - radius), (int) (y - radius),
                    radius * 2, radius * 2);
        }

        public int getRadius() {
            return radius;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }
    }

    // === Pin class ===
    static class Pin {
        private double x;
        private double y;
        private int radius;
        private boolean standing = true;
        private int id;

        public Pin(double x, double y, int radius, int id) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.id = id;
        }

        public void knockDown() {
            standing = false;
        }

        public boolean isStanding() {
            return standing;
        }

        public void draw(Graphics2D g2) {
            if (standing) {
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(Color.LIGHT_GRAY);
            }
            int h = radius * 3;
            int w = radius * 2;
            int px = (int) (x - w / 2);
            int py = (int) (y - h);
            g2.fillRoundRect(px, py, w, h, radius, radius);
            g2.setColor(Color.RED);
            g2.drawLine(px, py + radius, px + w, py + radius);
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public int getRadius() {
            return radius;
        }
    }

    // === Bowling scoring classes (simplified) ===

    static class Frame {
        private int firstRoll = -1;
        private int secondRoll = -1;

        public void roll(int pins) {
            if (firstRoll == -1) {
                firstRoll = pins;
            } else {
                secondRoll = pins;
            }
        }

        public boolean isComplete() {
            return firstRoll == 10 || (firstRoll != -1 && secondRoll != -1);
        }

        public boolean isStrike() {
            return firstRoll == 10;
        }

        public boolean isSpare() {
            return firstRoll != 10 && firstRoll + secondRoll == 10;
        }

        public int pinsKnockedDown() {
            int a = (firstRoll < 0) ? 0 : firstRoll;
            int b = (secondRoll < 0) ? 0 : secondRoll;
            return a + b;
        }

        public int getFirstRoll() {
            return firstRoll < 0 ? 0 : firstRoll;
        }

        public int getSecondRoll() {
            return secondRoll < 0 ? 0 : secondRoll;
        }
    }

    static class BowlingGame {
        private List<Frame> frames = new ArrayList<>();

        public BowlingGame() {
            for (int i = 0; i < 10; i++) {
                frames.add(new Frame());
            }
        }

        public void roll(int pins) {
            Frame frame = currentFrame();
            frame.roll(pins);
        }

        private Frame currentFrame() {
            for (Frame f : frames) {
                if (!f.isComplete()) {
                    return f;
                }
            }
            return frames.get(frames.size() - 1);
        }

        public int getCurrentFrameIndex() {
            for (int i = 0; i < frames.size(); i++) {
                if (!frames.get(i).isComplete()) {
                    return i + 1;
                }
            }
            return 10;
        }

        public int score() {
            int total = 0;
            int frameIndex = 0;
            for (int i = 0; i < 10; i++) {
                Frame frame = frames.get(frameIndex);
                if (frame.isStrike()) {
                    total += 10 + strikeBonus(frameIndex);
                } else if (frame.isSpare()) {
                    total += 10 + spareBonus(frameIndex);
                } else {
                    total += frame.pinsKnockedDown();
                }
                frameIndex++;
            }
            return total;
        }

        private int strikeBonus(int frameIndex) {
            int bonus = 0;
            if (frameIndex + 1 < frames.size()) {
                Frame next = frames.get(frameIndex + 1);
                bonus += next.getFirstRoll();
                if (next.isStrike() && frameIndex + 2 < frames.size()) {
                    Frame next2 = frames.get(frameIndex + 2);
                    bonus += next2.getFirstRoll();
                } else {
                    bonus += next.getSecondRoll();
                }
            }
            return bonus;
        }

        private int spareBonus(int frameIndex) {
            if (frameIndex + 1 < frames.size()) {
                return frames.get(frameIndex + 1).getFirstRoll();
            }
            return 0;
        }
    }
}
