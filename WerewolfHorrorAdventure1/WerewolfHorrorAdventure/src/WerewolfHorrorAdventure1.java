import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class WerewolfHorrorAdventure1 extends JFrame {
    private GamePanel gamePanel;
    private int playerX = 300, playerY = 300;
    private int werewolfX, werewolfY;
    private boolean torchOn = true;
    private int torchFlicker = 0;

    public WerewolfHorrorAdventure1() {
        setTitle("Werewolf Horror Adventure - Exploration");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gamePanel = new GamePanel();
        add(gamePanel);
        setVisible(true);

        Timer timer = new Timer(16, e -> {
            updateGame();
            gamePanel.repaint();
        });
        timer.start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                movePlayer(e);
            }
        });
        setFocusable(true);

        Random rand = new Random();
        werewolfX = rand.nextInt(700) + 50;
        werewolfY = rand.nextInt(500) + 50;
    }

    private void movePlayer(KeyEvent e) {
        int speed = 5;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: playerY -= speed; break;
            case KeyEvent.VK_S: playerY += speed; break;
            case KeyEvent.VK_A: playerX -= speed; break;
            case KeyEvent.VK_D: playerX += speed; break;
            case KeyEvent.VK_SPACE: torchOn = !torchOn; break;
        }
        if (playerX < 0) playerX = 0;
        if (playerX > 750) playerX = 750;
        if (playerY < 0) playerY = 0;
        if (playerY > 550) playerY = 550;
    }

    private void updateGame() {
        torchFlicker = (torchFlicker + 1) % 20;
        int dx = playerX - werewolfX;
        int dy = playerY - werewolfY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 50) {
            JOptionPane.showMessageDialog(this, "The werewolf caught you! Game Over.");
            System.exit(0);
        }
        if (distance < 200) {
            werewolfX += dx > 0 ? 2 : -2;
            werewolfY += dy > 0 ? 2 : -2;
        }
    }

    class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Background
            g.setColor(new Color(20, 40, 20));
            g.fillRect(0, 0, getWidth(), getHeight());

            // Trees
            g.setColor(new Color(50, 100, 50));
            for (int i = 0; i < 20; i++) {
                int tx = (int) (Math.random() * 800);
                int ty = (int) (Math.random() * 600);
                g.fillRect(tx, ty, 20, 40);
            }

            // Torch effect
            if (torchOn) {
                int alpha = torchFlicker > 10 ? 100 : 50;
                g.setColor(new Color(255, 200, 0, alpha));
                g.fillOval(playerX - 50, playerY - 50, 100, 100);
            }

            // Player
            g.setColor(Color.WHITE);
            g.fillOval(playerX, playerY, 20, 20);

            // Werewolf
            g.setColor(Color.RED);
            g.fillRect(werewolfX, werewolfY, 30, 30);

            // HUD
            g.setColor(Color.YELLOW);
            g.drawString("Press SPACE to toggle torch", 10, 20);
        }
    }

    public static void main(String[] args) {
        new WerewolfHorrorAdventure1();
    }
}