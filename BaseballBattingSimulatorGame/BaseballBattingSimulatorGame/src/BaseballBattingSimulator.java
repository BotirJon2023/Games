import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class BaseballBattingSimulator extends JFrame {
    private GamePanel gamePanel;
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final String TITLE = "Baseball Batting Simulator";

    public BaseballBattingSimulator() {
        setTitle(TITLE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        gamePanel = new GamePanel();
        add(gamePanel);
        addKeyListener(new GameKeyListener());
        setVisible(true);

        // Start game loop
        new Thread(gamePanel::run).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BaseballBattingSimulator::new);
    }

    class GamePanel extends JPanel implements Runnable {
        private static final int FPS = 60;
        private static final long TARGET_TIME = 1000 / FPS;
        private Ball ball;
        private Batter batter;
        private Pitcher pitcher;
        private Score score;
        private boolean gameOver;
        private boolean swing;
        private Random random;

        public GamePanel() {
            setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
            setBackground(new Color(0, 100, 0)); // Green field
            ball = new Ball();
            batter = new Batter();
            pitcher = new Pitcher();
            score = new Score();
            random = new Random();
            gameOver = false;
            swing = false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw field
            drawField(g2d);
            // Draw batter
            batter.draw(g2d);
            // Draw pitcher
            pitcher.draw(g2d);
            // Draw ball
            ball.draw(g2d);
            // Draw score and UI
            score.draw(g2d);
            // Draw game over screen
            if (gameOver) {
                drawGameOver(g2d);
            }
        }

        private void drawField(Graphics2D g2d) {
            // Draw diamond
            g2d.setColor(new Color(139, 69, 19)); // Brown dirt
            int[] xPoints = {350, 450, 350, 250};
            int[] yPoints = {200, 300, 400, 300};
            g2d.fillPolygon(xPoints, yPoints, 4);

            // Draw home plate
            g2d.setColor(Color.WHITE);
            int[] homeX = {340, 360, 350, 340, 330};
            int[] homeY = {390, 390, 400, 410, 400};
            g2d.fillPolygon(homeX, homeY, 5);

            // Draw foul lines
            g2d.setColor(Color.WHITE);
            g2d.drawLine(350, 400, 200, 200);
            g2d.drawLine(350, 400, 500, 200);
        }

        private void drawGameOver(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            g2d.drawString("Game Over", 280, 250);
            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            g2d.drawString("Final Score: " + score.getScore(), 320, 300);
            g2d.drawString("Press R to Restart", 310, 350);
        }

        @Override
        public void run() {
            while (true) {
                long startTime = System.currentTimeMillis();
                if (!gameOver) {
                    updateGame();
                }
                repaint();
                long elapsedTime = System.currentTimeMillis() - startTime;
                long sleepTime = TARGET_TIME - elapsedTime;
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void updateGame() {
            ball.update();
            batter.update();
            pitcher.update();
            checkCollisions();
            checkGameOver();
        }

        private void checkCollisions() {
            if (swing && ball.isHittable()) {
                Rectangle batBounds = batter.getBatBounds();
                Rectangle ballBounds = ball.getBounds();
                if (batBounds.intersects(ballBounds)) {
                    // Calculate hit quality
                    double timing = Math.abs(ball.getX() - batter.getX());
                    if (timing < 10) {
                        score.addScore(100); // Perfect hit
                        ball.hit(2.0, random.nextDouble() * Math.PI / 4);
                    } else if (timing < 20) {
                        score.addScore(50); // Good hit
                        ball.hit(1.5, random.nextDouble() * Math.PI / 3);
                    } else {
                        score.addScore(10); // Weak hit
                        ball.hit(1.0, random.nextDouble() * Math.PI / 2);
                    }
                } else {
                    score.addStrike();
                }
                swing = false;
            }
        }

        private void checkGameOver() {
            if (score.getStrikes() >= 3) {
                gameOver = true;
            }
        }

        public void swingBat() {
            swing = true;
            batter.swing();
        }

        public void resetGame() {
            ball.reset();
            batter.reset();
            pitcher.reset();
            score.reset();
            gameOver = false;
            swing = false;
        }
    }

    class Ball {
        private double x, y;
        private double vx, vy;
        private static final int SIZE = 10;
        private boolean pitched;
        private double speed;

        public Ball() {
            reset();
        }

        public void reset() {
            x = 350;
            y = 100;
            vx = 0;
            vy = 0;
            pitched = false;
            speed = 5 + new Random().nextDouble() * 3;
        }

        public void pitch() {
            if (!pitched) {
                vx = (350 - x) / 50;
                vy = (400 - y) / 50;
                pitched = true;
            }
        }

        public void hit(double power, double angle) {
            vx = power * Math.cos(angle);
            vy = -power * Math.sin(angle);
            pitched = false;
        }

        public void update() {
            if (pitched) {
                x += vx * speed;
                y += vy * speed;
                if (y > 450 || x < 0 || x > WINDOW_WIDTH) {
                    reset();
                }
            } else if (vx != 0 || vy != 0) {
                x += vx;
                y += vy;
                vy += 0.1; // Gravity
                if (y > WINDOW_HEIGHT || x < 0 || x > WINDOW_WIDTH) {
                    reset();
                }
            }
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
        }

        public Rectangle getBounds() {
            return new Rectangle((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
        }

        public boolean isHittable() {
            return pitched && y > 350 && y < 450;
        }

        public double getX() {
            return x;
        }
    }

    class Batter {
        private double x, y;
        private double batAngle;
        private boolean swinging;
        private static final int BAT_LENGTH = 50;
        private static final int BAT_WIDTH = 5;

        public Batter() {
            reset();
        }

        public void reset() {
            x = 350;
            y = 400;
            batAngle = Math.PI / 4;
            swinging = false;
        }

        public void swing() {
            if (!swinging) {
                swinging = true;
                new Thread(() -> {
                    for (int i = 0; i < 10; i++) {
                        batAngle -= Math.PI / 20;
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    swinging = false;
                    batAngle = Math.PI / 4;
                }).start();
            }
        }

        public void update() {
            // Update batter position or animation if needed
        }

        public void draw(Graphics2D g2d) {
            // Draw batter
            g2d.setColor(Color.BLUE);
            g2d.fillOval((int) x - 20, (int) y - 30, 40, 60);

            // Draw bat
            g2d.setColor(Color.BLACK);
            double batX = x + Math.cos(batAngle) * BAT_LENGTH;
            double batY = y - Math.sin(batAngle) * BAT_LENGTH;
            g2d.rotate(batAngle, x, y);
            g2d.fillRect((int) x, (int) y - BAT_WIDTH / 2, BAT_LENGTH, BAT_WIDTH);
            g2d.rotate(-batAngle, x, y);
        }

        public Rectangle getBatBounds() {
            double batX = x + Math.cos(batAngle) * BAT_LENGTH / 2;
            double batY = y - Math.sin(batAngle) * BAT_LENGTH / 2;
            return new Rectangle((int) batX - BAT_LENGTH / 2, (int) batY - BAT_WIDTH / 2, BAT_LENGTH, BAT_WIDTH);
        }

        public double getX() {
            return x;
        }
    }

    class Pitcher {
        private double x, y;
        private int pitchTimer;

        public Pitcher() {
            reset();
        }

        public void reset() {
            x = 350;
            y = 100;
            pitchTimer = 0;
        }

        public void update() {
            pitchTimer++;
            if (pitchTimer > 60) {
                gamePanel.ball.pitch();
                pitchTimer = 0;
            }
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.RED);
            g2d.fillOval((int) x - 20, (int) y - 30, 40, 60);
            // Pitching animation
            if (pitchTimer > 30) {
                g2d.fillRect((int) x + 20, (int) y - 40, 10, 20); // Arm raised
            }
        }
    }

    class Score {
        private int score;
        private int strikes;

        public Score() {
            reset();
        }

        public void reset() {
            score = 0;
            strikes = 0;
        }

        public void addScore(int points) {
            score += points;
        }

        public void addStrike() {
            strikes++;
        }

        public int getScore() {
            return score;
        }

        public int getStrikes() {
            return strikes;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Score: " + score, 20, 30);
            g2d.drawString("Strikes: " + strikes + "/3", 20, 60);
            g2d.drawString("Press SPACE to swing, R to restart", 20, WINDOW_HEIGHT - 20);
        }
    }

    class GameKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE && !gamePanel.gameOver) {
                gamePanel.swingBat();
            } else if (e.getKeyCode() == KeyEvent.VK_R) {
                gamePanel.resetGame();
            }
        }
    }
}