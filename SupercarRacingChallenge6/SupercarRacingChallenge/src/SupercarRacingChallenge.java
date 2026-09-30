import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class SupercarRacingChallenge extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SupercarRacingChallenge game = new SupercarRacingChallenge();
            game.setVisible(true);
        });
    }

    public SupercarRacingChallenge() {
        setTitle("Supercar Racing Challenge");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel panel = new GamePanel();
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
    }

    static class GamePanel extends JPanel implements ActionListener, KeyListener {

        // Screen
        private final int WIDTH = 800;
        private final int HEIGHT = 600;

        // Road animation
        private int roadOffset = 0;
        private final int roadStripeHeight = 40;

        // Cars
        private Car player1;
        private Car player2;   // can be human or AI
        private boolean vsComputer = true; // toggle: true = Player1 vs AI, false = 2 players

        // Game
        private Timer timer;
        private boolean running = true;
        private Random random = new Random();

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setBackground(Color.DARK_GRAY);
            setFocusable(true);
            addKeyListener(this);

            initGame();
            timer = new Timer(16, this); // ~60 FPS
            timer.start();
        }

        private void initGame() {
            int laneWidth = 120;
            int roadWidth = laneWidth * 3;
            int roadX = (WIDTH - roadWidth) / 2;

            player1 = new Car(roadX + laneWidth / 2 - 25, HEIGHT - 140, Color.CYAN);
            player2 = new Car(roadX + laneWidth * 2 + laneWidth / 2 - 25, HEIGHT - 140, Color.MAGENTA);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!running) return;

            // Road scrolling
            roadOffset += 8;
            if (roadOffset > roadStripeHeight) {
                roadOffset = 0;
            }

            // Player 1 movement (already handled by key events, here we just apply velocity)
            player1.update();

            // Player 2: either human or AI
            if (vsComputer) {
                updateAI(player2);
            } else {
                player2.update();
            }

            // Simple bounds
            keepInBounds(player1);
            keepInBounds(player2);

            // Simple collision check
            if (player1.getBounds().intersects(player2.getBounds())) {
                running = false;
            }

            repaint();
        }

        private void keepInBounds(Car car) {
            if (car.x < 100) car.x = 100;
            if (car.x > WIDTH - 140) car.x = WIDTH - 140;
            if (car.y < 50) car.y = 50;
            if (car.y > HEIGHT - 160) car.y = HEIGHT - 160;
        }

        private void updateAI(Car ai) {
            // Simple AI: follow a sine-like lane, occasionally change direction
            if (random.nextInt(30) == 0) {
                ai.vx = (random.nextBoolean() ? 4 : -4);
            }
            if (random.nextInt(50) == 0) {
                ai.vy = (random.nextBoolean() ? 2 : -2);
            }
            ai.update();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // Smooth
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw background gradient
            GradientPaint sky = new GradientPaint(0, 0, new Color(10, 10, 30),
                    0, HEIGHT, new Color(40, 40, 80));
            g2.setPaint(sky);
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            // Road
            int laneWidth = 120;
            int roadWidth = laneWidth * 3;
            int roadX = (WIDTH - roadWidth) / 2;

            GradientPaint roadPaint = new GradientPaint(roadX, 0, new Color(30, 30, 30),
                    roadX + roadWidth, 0, new Color(10, 10, 10));
            g2.setPaint(roadPaint);
            g2.fillRoundRect(roadX, 0, roadWidth, HEIGHT, 40, 40);

            // Road edges
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(4f));
            g2.drawRoundRect(roadX, 0, roadWidth, HEIGHT, 40, 40);

            // Lane stripes (animated)
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(250, 250, 100));
            for (int y = -roadStripeHeight; y < HEIGHT + roadStripeHeight; y += roadStripeHeight * 2) {
                int stripeY = y + roadOffset;
                int lane1X = roadX + laneWidth;
                int lane2X = roadX + laneWidth * 2;
                g2.drawLine(lane1X, stripeY, lane1X, stripeY + roadStripeHeight);
                g2.drawLine(lane2X, stripeY, lane2X, stripeY + roadStripeHeight);
            }

            // Cars
            drawCar(g2, player1);
            drawCar(g2, player2);

            // HUD
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            String mode = vsComputer ? "Mode: Player vs Computer" : "Mode: 2 Players";
            g2.drawString(mode, 20, 30);

            if (!running) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 40));
                g2.setColor(new Color(255, 80, 80));
                String msg = "CRASH! GAME OVER";
                int w = g2.getFontMetrics().stringWidth(msg);
                g2.drawString(msg, (WIDTH - w) / 2, HEIGHT / 2);
            }

            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString("Controls P1: W/S/A/D   P2: Arrows   M: toggle mode   R: restart", 20, HEIGHT - 20);
        }

        private void drawCar(Graphics2D g2, Car car) {
            int w = 50;
            int h = 90;

            // Glow
            g2.setColor(new Color(car.color.getRed(), car.color.getGreen(), car.color.getBlue(), 80));
            g2.fillOval(car.x - 10, car.y + h - 10, w + 20, 30);

            // Body
            GradientPaint body = new GradientPaint(car.x, car.y, car.color,
                    car.x + w, car.y + h, car.color.darker());
            g2.setPaint(body);
            g2.fillRoundRect(car.x, car.y, w, h, 20, 20);

            // Windows
            g2.setColor(new Color(20, 20, 40, 200));
            g2.fillRoundRect(car.x + 8, car.y + 10, w - 16, h / 2, 15, 15);

            // Lights
            g2.setColor(new Color(255, 255, 180));
            g2.fillOval(car.x + 5, car.y - 5, 10, 10);
            g2.fillOval(car.x + w - 15, car.y - 5, 10, 10);

            // Tail lights
            g2.setColor(new Color(255, 80, 80));
            g2.fillOval(car.x + 5, car.y + h - 10, 10, 10);
            g2.fillOval(car.x + w - 15, car.y + h - 10, 10, 10);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();

            // Player 1: WASD
            if (code == KeyEvent.VK_A) player1.vx = -5;
            if (code == KeyEvent.VK_D) player1.vx = 5;
            if (code == KeyEvent.VK_W) player1.vy = -5;
            if (code == KeyEvent.VK_S) player1.vy = 5;

            if (!vsComputer) {
                // Player 2: arrows
                if (code == KeyEvent.VK_LEFT) player2.vx = -5;
                if (code == KeyEvent.VK_RIGHT) player2.vx = 5;
                if (code == KeyEvent.VK_UP) player2.vy = -5;
                if (code == KeyEvent.VK_DOWN) player2.vy = 5;
            }

            // Toggle mode
            if (code == KeyEvent.VK_M) {
                vsComputer = !vsComputer;
                player1.vx = player1.vy = 0;
                player2.vx = player2.vy = 0;
            }

            // Restart
            if (code == KeyEvent.VK_R) {
                running = true;
                initGame();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();

            // Stop Player 1
            if (code == KeyEvent.VK_A || code == KeyEvent.VK_D) player1.vx = 0;
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_S) player1.vy = 0;

            // Stop Player 2 (if human)
            if (!vsComputer) {
                if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT) player2.vx = 0;
                if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN) player2.vy = 0;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {}

        // Car class
        static class Car {
            int x, y;
            int vx = 0, vy = 0;
            Color color;

            Car(int x, int y, Color color) {
                this.x = x;
                this.y = y;
                this.color = color;
            }

            void update() {
                x += vx;
                y += vy;
            }

            Rectangle getBounds() {
                return new Rectangle(x, y, 50, 90);
            }
        }
    }
}
