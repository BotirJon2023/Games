import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class ArcheryAccuracyGame extends JPanel implements ActionListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int TARGET_SIZE = 50;
    private static final int ARROW_SPEED = 10;
    private static final int TARGET_SPEED = 3;

    private Timer timer;
    private int score = 0;
    private int targetX, targetY;
    private int targetDirX = TARGET_SPEED;
    private ArrayList<Arrow> arrows;
    private boolean gameOver = false;
    private Random random;

    private class Arrow {
        int x, y;
        boolean active;

        Arrow(int x, int y) {
            this.x = x;
            this.y = y;
            this.active = true;
        }
    }

    public ArcheryAccuracyGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.WHITE);
        arrows = new ArrayList<>();
        random = new Random();
        resetTarget();

        timer = new Timer(16, this); // ~60 FPS
        timer.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!gameOver) {
                    arrows.add(new Arrow(WIDTH / 2, HEIGHT - 50));
                } else {
                    resetGame();
                }
            }
        });
    }

    private void resetTarget() {
        targetX = random.nextInt(WIDTH - TARGET_SIZE);
        targetY = random.nextInt(HEIGHT / 2);
        targetDirX = random.nextBoolean() ? TARGET_SPEED : -TARGET_SPEED;
    }

    private void resetGame() {
        score = 0;
        arrows.clear();
        gameOver = false;
        resetTarget();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw bow
        g2d.setColor(Color.BLACK);
        g2d.fillRect(WIDTH / 2 - 5, HEIGHT - 50, 10, 30);

        // Draw target
        g2d.setColor(Color.RED);
        g2d.fillOval(targetX, targetY, TARGET_SIZE, TARGET_SIZE);
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(targetX + 10, targetY + 10, TARGET_SIZE - 20, TARGET_SIZE - 20);

        // Draw arrows
        g2d.setColor(Color.BLUE);
        for (Arrow arrow : arrows) {
            if (arrow.active) {
                g2d.fillRect(arrow.x - 2, arrow.y, 4, 10);
            }
        }

        // Draw score
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Score: " + score, 10, 30);

        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            g2d.drawString("Game Over! Score: " + score, WIDTH / 2 - 150, HEIGHT / 2);
            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            g2d.drawString("Click to Restart", WIDTH / 2 - 80, HEIGHT / 2 + 40);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            // Update target position
            targetX += targetDirX;
            if (targetX <= 0 || targetX >= WIDTH - TARGET_SIZE) {
                targetDirX = -targetDirX;
            }

            // Update arrows
            for (int i = arrows.size() - 1; i >= 0; i--) {
                Arrow arrow = arrows.get(i);
                if (arrow.active) {
                    arrow.y -= ARROW_SPEED;

                    // Check collision
                    if (arrow.y <= targetY + TARGET_SIZE &&
                            arrow.y >= targetY &&
                            arrow.x >= targetX &&
                            arrow.x <= targetX + TARGET_SIZE) {
                        score += 10;
                        arrow.active = false;
                        resetTarget();
                    }

                    // Remove arrows that go off-screen
                    if (arrow.y < 0) {
                        arrow.active = false;
                        if (arrows.stream().noneMatch(a -> a.active)) {
                            gameOver = true;
                        }
                    }
                }
            }
        }
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Archery Accuracy Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new ArcheryAccuracyGame());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}