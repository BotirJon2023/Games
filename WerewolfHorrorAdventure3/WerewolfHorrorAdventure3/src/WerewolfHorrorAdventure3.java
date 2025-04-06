import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class WerewolfHorrorAdventure3 extends JFrame {
    private GamePanel gamePanel;
    private int moonPhase = 0;
    private ArrayList<String> inventory = new ArrayList<>();
    private boolean hasKey = false, doorOpen = false;

    public WerewolfHorrorAdventure3() {
        setTitle("Werewolf Horror Adventure - Puzzle");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gamePanel = new GamePanel();
        add(gamePanel);
        setVisible(true);

        Timer timer = new Timer(1000, e -> {
            moonPhase = (moonPhase + 1) % 4;
            gamePanel.repaint();
        });
        timer.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
        setFocusable(true);
    }

    private void handleClick(int x, int y) {
        // Key pickup
        if (x > 300 && x < 350 && y > 400 && y < 450 && !hasKey) {
            inventory.add("Key");
            hasKey = true;
        }
        // Door interaction
        if (x > 600 && x < 700 && y > 300 && y < 500 && hasKey) {
            doorOpen = true;
        }
        if (doorOpen && moonPhase == 3) {
            JOptionPane.showMessageDialog(this, "The werewolf emerges under the full moon! Game Over.");
            System.exit(0);
        }
        gamePanel.repaint();
    }

    class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(0, 0, 20));
            g.fillRect(0, 0, getWidth(), getHeight());

            // Moon animation
            g.setColor(Color.WHITE);
            if (moonPhase == 0) g.fillOval(50, 50, 50, 50); // Full moon
            else if (moonPhase == 1) g.fillArc(50, 50, 50, 50, 90, 180); // Waning
            else if (moonPhase == 2) g.fillOval(75, 50, 25, 50); // Crescent
            else g.fillArc(50, 50, 50, 50, 270, 180); // Waxing

            // Scene
            g.setColor(Color.GRAY);
            g.fillRect(600, 300, 100, 200); // Door
            if (!hasKey) {
                g.setColor(Color.YELLOW);
                g.fillRect(300, 400, 50, 50); // Key
            }
            if (doorOpen) {
                g.setColor(Color.BLACK);
                g.fillRect(620, 320, 60, 160); // Open door
            }

            // Inventory
            g.setColor(Color.WHITE);
            g.drawString("Inventory: " + inventory.toString(), 10, 20);
            g.drawString("Click the key, then the door", 10, 40);
        }
    }

    public static void main(String[] args) {
        new WerewolfHorrorAdventure3();
    }
}