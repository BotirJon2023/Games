import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Random;

public class ParalympicSportsGame2 extends JPanel implements KeyListener {

    private int athleteX = 50;
    private int athleteY = 200;
    private int score = 0;
    private boolean isJumping = false;
    private int jumpHeight = 0;
    private Timer timer;
    private ArrayList<Hurdle> hurdles;
    private ArrayList<PowerUp> powerUps;
    private Random random;
    private boolean gameOver = false;

    public ParalympicSportsGame2() {
        setFocusable(true);
        requestFocus();
        addKeyListener(this);

        random = new Random();
        hurdles = new ArrayList<>();
        powerUps = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            hurdles.add(new Hurdle(random.nextInt(500) + 500 * i));
        }

        timer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGame();
                repaint();
            }
        });
        timer.start();
    }

    private void updateGame() {
        if (!gameOver) {
            athleteX += 2;
            if (athleteX > 600) {
                athleteX = 50;
                score++;
            }

            if (isJumping) {
                athleteY -= 5;
                jumpHeight += 5;
                if (jumpHeight >= 100) {
                    isJumping = false;
                }
            } else if (athleteY < 200) {
                athleteY += 5;
                jumpHeight -= 5;
            }

            for (Hurdle hurdle : hurdles) {
                hurdle.update();
                if (hurdle.getX() < -50) {
                    hurdle.setX(random.nextInt(500) + 500);
                }
                if (checkCollision(athleteX, athleteY, hurdle.getX(), hurdle.getY())) {
                    score--;
                    hurdle.setX(random.nextInt(500) + 500);
                }
            }

            for (PowerUp powerUp : powerUps) {
                powerUp.update();
                if (powerUp.getX() < -50) {
                    powerUps.remove(powerUp);
                    break;
                }
                if (checkCollision(athleteX, athleteY, powerUp.getX(), powerUp.getY())) {
                    score += 5;
                    powerUps.remove(powerUp);
                    break;
                }
            }

            if (random.nextInt(100) < 5) {
                powerUps.add(new PowerUp(600, random.nextInt(200) + 100));
            }
        }
    }

    private boolean checkCollision(int x1, int y1, int x2, int y2) {
        if (x1 + 50 > x2 && x1 < x2 + 50) {
            if (y1 + 50 > y2) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 600, 400);
        g.setColor(Color.BLACK);
        g.drawString("Score: " + score, 10, 20);

        if (gameOver) {
            g.drawString("Game Over!", 250, 200);
            g.drawString("Press Space to restart", 220, 220);
        } else {
            // Draw athlete
            g.fillOval(athleteX, athleteY, 50, 50);
            g.drawLine(athleteX + 25, athleteY + 50, athleteX + 25, athleteY + 70); // body
            g.drawLine(athleteX + 25, athleteY + 60, athleteX, athleteY + 80); // left leg
            g.drawLine(athleteX + 25, athleteY + 60, athleteX + 50, athleteY + 80); // right leg
            g.drawLine(athleteX + 25, athleteY + 55, athleteX, athleteY + 40); // left arm
            g.drawLine(athleteX + 25, athleteY + 55, athleteX + 50, athleteY + 40); // right arm

            // Draw hurdles
            for (Hurdle hurdle : hurdles) {
                g.fillRect(hurdle.getX(), hurdle.getY(), 50, 20);
            }

            // Draw power-ups
            for (PowerUp powerUp : powerUps) {
                g.fillOval(powerUp.getX(), powerUp.getY(), 20, 20);
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (athleteY == 200 && !gameOver) {
                isJumping = true;
                jumpHeight = 0;
            } else if (gameOver) {
                gameOver = false;
                score = 0;
                athleteX = 50;
                athleteY = 200;
                hurdles.clear();
                powerUps.clear();
                for (int i = 0; i < 5; i++) {
                    hurdles.add(new Hurdle(random.nextInt(500) + 500 * i));
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    private class Hurdle {
        private int x;
        private int y;

        public Hurdle(int x) {
            this.x = x;
            this.y = 220;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void update() {
            x -= 5;
        }
    }

    private class PowerUp {
        private int x;
        private int y;

        public PowerUp(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public void update() {
            x -= 3;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Paralympic Sports Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.add(new ParalympicSportsGame());
        frame.setVisible(true);
    }
}