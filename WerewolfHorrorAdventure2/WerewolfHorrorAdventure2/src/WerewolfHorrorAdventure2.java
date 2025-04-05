import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class WerewolfHorrorAdventure2 extends JFrame {
    private GamePanel gamePanel;
    private int playerX = 50, playerY = 400;
    private ArrayList<Werewolf> werewolves = new ArrayList<>();
    private boolean attacking = false;
    private int attackFrame = 0;

    public WerewolfHorrorAdventure2() {
        setTitle("Werewolf Horror Adventure - Combat");
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

        werewolves.add(new Werewolf(700, 400));
    }

    private void movePlayer(KeyEvent e) {
        int speed = 5;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: playerX -= speed; break;
            case KeyEvent.VK_D: playerX += speed; break;
            case KeyEvent.VK_SPACE: attacking = true; attackFrame = 0; break;
        }
        if (playerX < 0) playerX = 0;
        if (playerX > 750) playerX = 750;
    }

    private void updateGame() {
        if (attacking) attackFrame++;
        if (attackFrame > 10) attacking = false;

        for (int i = werewolves.size() - 1; i >= 0; i--) {
            Werewolf w = werewolves.get(i);
            w.update(playerX, playerY);
            if (attacking && Math.abs(playerX - w.x) < 50) {
                werewolves.remove(i);
            }
            if (Math.abs(playerX - w.x) < 20) {
                JOptionPane.showMessageDialog(this, "A werewolf bit you! Game Over.");
                System.exit(0);
            }
        }
    }

    class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(10, 10, 30));
            g.fillRect(0, 0, getWidth(), getHeight());

            // Ground
            g.setColor(new Color(50, 30, 10));
            g.fillRect(0, 450, 800, 150);

            // Player
            g.setColor(Color.BLUE);
            g.fillRect(playerX, playerY, 20, 50);
            if (attacking) {
                g.setColor(Color.YELLOW);
                g.fillRect(playerX + 20, playerY, 30, 10);
            }

            // Werewolves
            for (Werewolf w : werewolves) {
                g.setColor(Color.RED);
                g.fillRect(w.x, w.y, 30, 50);
            }

            g.setColor(Color.WHITE);
            g.drawString("Press SPACE to attack", 10, 20);
        }
    }

    class Werewolf {
        int x, y;
        Werewolf(int x, int y) {
            this.x = x;
            this.y = y;
        }
        void update(int playerX, int playerY) {
            if (playerX < x) x -= 2;
            if (playerX > x) x += 2;
        }
    }

    public static void main(String[] args) {
        new WerewolfHorrorAdventure2();
    }
}