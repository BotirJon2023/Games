import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ParalympicSportsGame extends JPanel implements KeyListener {

    private int athleteX = 50;
    private int athleteY = 200;
    private int hurdleX = 500;
    private int hurdleY = 220;
    private int score = 0;
    private boolean isJumping = false;
    private int jumpHeight = 0;
    private Timer timer;

    public ParalympicSportsGame() {
        setFocusable(true);
        requestFocus();
        addKeyListener(this);

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

        hurdleX -= 5;
        if (hurdleX < -50) {
            hurdleX = 500;
        }

        if (checkCollision()) {
            score--;
            hurdleX = 500;
        }
    }

    private boolean checkCollision() {
        if (athleteX + 50 > hurdleX && athleteX < hurdleX + 50) {
            if (athleteY + 50 > hurdleY) {
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

        // Draw athlete
        g.fillOval(athleteX, athleteY, 50, 50);
        g.drawLine(athleteX + 25, athleteY + 50, athleteX + 25, athleteY + 70); // body
        g.drawLine(athleteX + 25, athleteY + 60, athleteX, athleteY + 80); // left leg
        g.drawLine(athleteX + 25, athleteY + 60, athleteX + 50, athleteY + 80); // right leg
        g.drawLine(athleteX + 25, athleteY + 55, athleteX, athleteY + 40); // left arm
        g.drawLine(athleteX + 25, athleteY + 55, athleteX + 50, athleteY + 40); // right arm

        // Draw hurdle
        g.fillRect(hurdleX, hurdleY, 50, 20);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && athleteY == 200) {
            isJumping = true;
            jumpHeight = 0;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Paralympic Sports Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.add(new ParalympicSportsGame());
        frame.setVisible(true);
    }
}