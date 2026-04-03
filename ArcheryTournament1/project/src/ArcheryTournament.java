import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;
import javax.swing.Timer;

public class ArcheryTournament extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final int TARGET_X = 950;
    private static final int TARGET_Y = 150;
    private static final int TARGET_SIZE = 200;

    private int mouseX, mouseY;
    private boolean isDragging = false;
    private double power = 0;
    private int score = 0;
    private int arrowsLeft = 10;
    private int currentRound = 1;
    private List<Arrow> arrows = new ArrayList<>();
    private Arrow currentArrow = null;
    private Timer gameTimer;
    private boolean gameRunning = true;
    private boolean showingResult = false;
    private int resultX, resultY;
    private int resultScore = 0;
    private Color[] targetColors = {Color.WHITE, Color.BLACK, Color.BLUE, Color.RED, Color.YELLOW};
    private int[] scoreValues = {1, 2, 3, 4, 5};

    // Animation variables
    private float[] glowIntensity = {0, 0, 0, 0, 0};
    private boolean glowIncreasing = true;

    public ArcheryTournament() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(34, 139, 34)); // Forest green
        addMouseListener(this);
        addMouseMotionListener(this);

        gameTimer = new Timer(16, this);
        gameTimer.start();

        // Animation timer for target glow
        Timer glowTimer = new Timer(50, e -> {
            for (int i = 0; i < glowIntensity.length; i++) {
                if (glowIncreasing) {
                    glowIntensity[i] += 0.05f;
                    if (glowIntensity[i] >= 1.0f) glowIncreasing = false;
                } else {
                    glowIntensity[i] -= 0.05f;
                    if (glowIntensity[i] <= 0.2f) glowIncreasing = true;
                }
            }
        });
        glowTimer.start();
    }

    class Arrow {
        double x, y;
        double vx, vy;
        boolean active = true;
        boolean hit = false;
        int hitScore = 0;
        double hitX, hitY;

        Arrow(double startX, double startY, double power, int mouseX, int mouseY) {
            this.x = startX;
            this.y = startY;
            double angle = Math.atan2(mouseY - startY, mouseX - startX);
            double speed = power * 15;
            this.vx = speed * Math.cos(angle);
            this.vy = speed * Math.sin(angle);
        }

        void update() {
            if (!active) return;
            x += vx;
            y += vy;
            vy += 0.5; // Gravity

            // Check collision with target
            if (!hit && x + 10 >= TARGET_X && x - 10 <= TARGET_X + TARGET_SIZE &&
                    y + 10 >= TARGET_Y && y - 10 <= TARGET_Y + TARGET_SIZE) {
                hit = true;
                active = false;
                hitX = x;
                hitY = y;
                hitScore = calculateScore((int)x, (int)y);
                score += hitScore;
                resultScore = hitScore;
                showingResult = true;
                resultX = (int)x;
                resultY = (int)y;

                Timer resultTimer = new Timer(1500, e -> showingResult = false);
                resultTimer.setRepeats(false);
                resultTimer.start();
            }

            // Check if arrow is out of bounds
            if (x > WIDTH + 100 || x < -100 || y > HEIGHT + 100) {
                active = false;
                arrowsLeft--;
                if (arrowsLeft <= 0) {
                    endGame();
                }
            }
        }

        void draw(Graphics2D g2d) {
            if (active) {
                g2d.setColor(new Color(210, 180, 140));
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine((int)x, (int)y, (int)(x - vx * 0.5), (int)(y - vy * 0.5));
                g2d.fillOval((int)x - 3, (int)y - 3, 6, 6);
                // Arrow fletching
                g2d.setColor(Color.RED);
                double angle = Math.atan2(vy, vx);
                int fx1 = (int)(x - vx * 0.3 + 5 * Math.sin(angle));
                int fy1 = (int)(y - vy * 0.3 - 5 * Math.cos(angle));
                int fx2 = (int)(x - vx * 0.3 - 5 * Math.sin(angle));
                int fy2 = (int)(y - vy * 0.3 + 5 * Math.cos(angle));
                g2d.drawLine((int)x, (int)y, fx1, fy1);
                g2d.drawLine((int)x, (int)y, fx2, fy2);
            } else if (hit) {
                g2d.setColor(Color.RED);
                g2d.fillOval((int)hitX - 4, (int)hitY - 4, 8, 8);
                g2d.setColor(Color.WHITE);
                g2d.fillOval((int)hitX - 2, (int)hitY - 2, 4, 4);
            }
        }
    }

    private int calculateScore(int x, int y) {
        double centerX = TARGET_X + TARGET_SIZE / 2;
        double centerY = TARGET_Y + TARGET_SIZE / 2;
        double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
        double radius = TARGET_SIZE / 2;

        if (distance <= radius * 0.1) return 10; // Bullseye!
        if (distance <= radius * 0.3) return 9;
        if (distance <= radius * 0.5) return 8;
        if (distance <= radius * 0.7) return 7;
        if (distance <= radius * 0.9) return 6;
        return 5;
    }

    private void drawTarget(Graphics2D g2d) {
        int radius = TARGET_SIZE / 2;
        int centerX = TARGET_X + radius;
        int centerY = TARGET_Y + radius;

        // Draw rings with glow effect
        for (int i = 4; i >= 0; i--) {
            int ringRadius = (int)(radius * (1 - i * 0.2));
            int ringWidth = (int)(radius * 0.2);

            Color ringColor = targetColors[i];
            if (i == 4) { // Bullseye
                float intensity = 0.5f + glowIntensity[i] * 0.5f;
                ringColor = new Color(1.0f, intensity, 0.0f);
            }

            g2d.setColor(ringColor);
            g2d.fillOval(centerX - ringRadius, centerY - ringRadius, ringRadius * 2, ringRadius * 2);

            // Add glow effect for bullseye
            if (i == 4) {
                g2d.setColor(new Color(1.0f, 0.8f, 0.2f, 0.3f));
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval(centerX - ringRadius - 5, centerY - ringRadius - 5,
                        (ringRadius + 5) * 2, (ringRadius + 5) * 2);
            }
        }

        // Draw ring lines
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        for (int i = 1; i <= 4; i++) {
            int ringRadius = (int)(radius * (1 - i * 0.2));
            g2d.drawOval(centerX - ringRadius, centerY - ringRadius, ringRadius * 2, ringRadius * 2);
        }

        // Draw score numbers
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        for (int i = 0; i < scoreValues.length; i++) {
            int ringRadius = (int)(radius * (0.9 - i * 0.2));
            g2d.setColor(Color.BLACK);
            String scoreText = String.valueOf(scoreValues[4 - i]);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(scoreText, centerX + ringRadius - 15, centerY - ringRadius + 5);
        }
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("10", centerX - 10, centerY + 8);
    }

    private void drawBow(Graphics2D g2d, int mouseX, int mouseY) {
        int bowX = 150;
        int bowY = HEIGHT - 200;

        // Draw bow body
        g2d.setColor(new Color(139, 69, 19));
        g2d.setStroke(new BasicStroke(15));
        g2d.drawArc(bowX - 50, bowY - 100, 100, 200, 180, 180);

        // Draw bow string
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        double angle = Math.atan2(mouseY - bowY, mouseX - bowX);
        int stringX = (int)(bowX + power * 30 * Math.cos(angle));
        int stringY = (int)(bowY + power * 30 * Math.sin(angle));
        g2d.drawLine(bowX - 40, bowY - 80, stringX, stringY);
        g2d.drawLine(bowX - 40, bowY + 80, stringX, stringY);

        // Draw arrow on bow
        g2d.setColor(new Color(210, 180, 140));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(stringX, stringY, stringX - 60, stringY - 20);

        // Draw power indicator
        if (isDragging) {
            int powerWidth = (int)(power * 200);
            g2d.setColor(new Color(255, 100, 100, 150));
            g2d.fillRect(bowX - 100, bowY + 100, powerWidth, 20);
            g2d.setColor(Color.RED);
            g2d.drawRect(bowX - 100, bowY + 100, 200, 20);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("Power: " + (int)(power * 100) + "%", bowX - 50, bowY + 95);
        }

        // Draw archer
        g2d.setColor(new Color(255, 200, 150));
        g2d.fillOval(bowX - 30, bowY - 50, 40, 40);
        g2d.setColor(new Color(0, 100, 0));
        g2d.fillRect(bowX - 40, bowY - 10, 60, 80);
    }

    private void drawUI(Graphics2D g2d) {
        // Score panel
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(10, 10, 250, 120, 15, 15);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Archery Tournament", 20, 40);
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.drawString("Score: " + score, 20, 70);
        g2d.drawString("Arrows Left: " + arrowsLeft, 20, 95);
        g2d.drawString("Round: " + currentRound, 20, 120);

        // Instructions
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(WIDTH - 250, 10, 240, 80, 15, 15);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Click and drag to aim", WIDTH - 240, 35);
        g2d.drawString("Drag farther = more power", WIDTH - 240, 55);
        g2d.drawString("Release to shoot!", WIDTH - 240, 75);

        // Game over message
        if (arrowsLeft <= 0) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            String message = "Game Over! Final Score: " + score;
            FontMetrics fm = g2d.getFontMetrics();
            int x = (WIDTH - fm.stringWidth(message)) / 2;
            g2d.drawString(message, x, HEIGHT / 2);

            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            String restartMsg = "Press R to restart";
            fm = g2d.getFontMetrics();
            x = (WIDTH - fm.stringWidth(restartMsg)) / 2;
            g2d.drawString(restartMsg, x, HEIGHT / 2 + 50);
        }

        // Show hit result
        if (showingResult) {
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            String resultText = "+" + resultScore;
            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString(resultText, resultX + 15, resultY - 15);
        }
    }

    private void endGame() {
        gameRunning = false;
        repaint();
    }

    private void restartGame() {
        score = 0;
        arrowsLeft = 10;
        currentRound = 1;
        arrows.clear();
        gameRunning = true;
        showingResult = false;
        isDragging = false;
        power = 0;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw sky
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235),
                0, HEIGHT / 2, new Color(100, 180, 250));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT / 2);

        // Draw ground
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(0, HEIGHT / 2, WIDTH, HEIGHT / 2);

        // Draw clouds
        drawClouds(g2d);

        // Draw target
        drawTarget(g2d);

        // Draw arrows
        for (Arrow arrow : arrows) {
            arrow.draw(g2d);
        }

        // Draw bow
        drawBow(g2d, mouseX, mouseY);

        // Draw UI
        drawUI(g2d);

        // Draw wind effect
        drawWindEffect(g2d);
    }

    private void drawClouds(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillOval(200, 50, 80, 50);
        g2d.fillOval(240, 40, 100, 60);
        g2d.fillOval(280, 50, 80, 50);

        g2d.fillOval(600, 80, 70, 45);
        g2d.fillOval(635, 70, 90, 55);
        g2d.fillOval(670, 80, 70, 45);
    }

    private void drawWindEffect(Graphics2D g2d) {
        int time = (int)(System.currentTimeMillis() / 100);
        for (int i = 0; i < 5; i++) {
            int x = (time + i * 100) % WIDTH;
            g2d.setColor(new Color(200, 200, 255, 50));
            g2d.drawLine(x, 100, x + 30, 105);
            g2d.drawLine(x, 110, x + 30, 115);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentArrow != null) {
            currentArrow.update();
            if (!currentArrow.active) {
                arrows.add(currentArrow);
                currentArrow = null;
                if (arrowsLeft > 0 && gameRunning) {
                    arrowsLeft--;
                    if (arrowsLeft % 3 == 0 && arrowsLeft > 0) {
                        currentRound++;
                    }
                }
            }
        }
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!gameRunning || currentArrow != null || arrowsLeft <= 0) return;
        int bowX = 150;
        int bowY = HEIGHT - 200;
        Rectangle bowArea = new Rectangle(bowX - 50, bowY - 100, 100, 200);
        if (bowArea.contains(e.getPoint())) {
            isDragging = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isDragging && currentArrow == null && gameRunning && arrowsLeft > 0) {
            int bowX = 150;
            int bowY = HEIGHT - 200;
            currentArrow = new Arrow(bowX, bowY, power, mouseX, mouseY);
            isDragging = false;
            power = 0;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (isDragging) {
            mouseX = e.getX();
            mouseY = e.getY();
            int bowX = 150;
            int bowY = HEIGHT - 200;
            double dx = mouseX - bowX;
            double dy = mouseY - bowY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            power = Math.min(1.0, distance / 150.0);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Archery Tournament - Colorful Archery Game");
            ArcheryTournament game = new ArcheryTournament();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);

            // Add key listener for restart
            frame.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_R) {
                        game.restartGame();
                    }
                }
            });

            frame.setVisible(true);
        });
    }
}