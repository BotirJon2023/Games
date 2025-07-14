import Player.Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GamePanel extends JPanel implements ActionListener {

    private Timer timer;
    private Player player;
    private Ball ball;
    private Hoop hoop;

    public GamePanel() {
        setFocusable(true);
        setBackground(Color.BLACK);
        setDoubleBuffered(true);

        player = new Player();
        ball = new Ball();
        hoop = new Hoop();

        addKeyListener(new InputHandler(player, ball));
        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        hoop.draw(g);
        player.draw(g);
        ball.draw(g);
        drawScore(g);
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + player.getScore(), 20, 30);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        player.update();
        ball.update();
        repaint();
    }
}
