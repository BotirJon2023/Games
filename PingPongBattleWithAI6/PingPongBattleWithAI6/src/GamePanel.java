import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Timer;

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private final Timer timer;
    private final Paddle player;
    private final Paddle ai;
    private final Ball ball;
    private boolean upPressed, downPressed;
    private int playerScore = 0, aiScore = 0;
    private final int WINNING_SCORE = 5;

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        setBackground(Color.BLACK);

        player = new Paddle(30, 200, true);
        ai = new Paddle(750, 200, false);
        ball = new Ball(400, 250);

        timer = new Timer(10, this);
        timer.wait();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        checkCollision();
        repaint();
    }

    private void move() {
        if (upPressed) player.moveUp();
        if (downPressed) player.moveDown();
        ai.followBall(ball);
        ball.move();
    }

    private void checkCollision() {
        ball.checkWallCollision();

        if (ball.getRect().intersects(player.getRect())) {
            ball.bounceFromPaddle(player);
        }

        if (ball.getRect().intersects(ai.getRect())) {
            ball.bounceFromPaddle(ai);
        }

        if (ball.getX() < 0) {
            aiScore++;
            resetRound();
        }

        if (ball.getX() > getWidth()) {
            playerScore++;
            resetRound();
        }
    }

    private void resetRound() {
        ball.resetPosition();
        player.reset();
        ai.reset();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawScore(g);
        player.draw(g);
        ai.draw(g);
        ball.draw(g);
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Player: " + playerScore, 50, 30);
        g.drawString("AI: " + aiScore, 650, 30);

        if (playerScore >= WINNING_SCORE || aiScore >= WINNING_SCORE) {
            String winner = playerScore > aiScore ? "Player Wins!" : "AI Wins!";
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString(winner, 300, 250);
            timer.stop();
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) upPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_DOWN) downPressed = true;
    }

    @Override public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) upPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_DOWN) downPressed = false;
    }

    @Override public void keyTyped(KeyEvent e) {}

    @Override
    public void actionPerformed(ActionEvent e) {
        
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }
}
