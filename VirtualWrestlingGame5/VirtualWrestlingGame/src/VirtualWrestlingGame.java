import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class VirtualWrestlingGame extends JPanel {

    // Game constants
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 600;
    private static final int PLAYER_SIZE = 50;

    // Player properties
    private int player1X = 100;
    private int player1Y = 100;
    private int player2X = 600;
    private int player2Y = 100;

    // Movement flags
    private boolean player1MovingUp = false;
    private boolean player1MovingDown = false;
    private boolean player1MovingLeft = false;
    private boolean player1MovingRight = false;

    private boolean player2MovingUp = false;
    private boolean player2MovingDown = false;
    private boolean player2MovingLeft = false;
    private boolean player2MovingRight = false;

    // Animation and game loop
    private Timer gameLoop;

    public VirtualWrestlingGame() {
        setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        setBackground(Color.BLACK);

        // Key listeners for player movement
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W:
                        player1MovingUp = true;
                        break;
                    case KeyEvent.VK_S:
                        player1MovingDown = true;
                        break;
                    case KeyEvent.VK_A:
                        player1MovingLeft = true;
                        break;
                    case KeyEvent.VK_D:
                        player1MovingRight = true;
                        break;

                    case KeyEvent.VK_UP:
                        player2MovingUp = true;
                        break;
                    case KeyEvent.VK_DOWN:
                        player2MovingDown = true;
                        break;
                    case KeyEvent.VK_LEFT:
                        player2MovingLeft = true;
                        break;
                    case KeyEvent.VK_RIGHT:
                        player2MovingRight = true;
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W:
                        player1MovingUp = false;
                        break;
                    case KeyEvent.VK_S:
                        player1MovingDown = false;
                        break;
                    case KeyEvent.VK_A:
                        player1MovingLeft = false;
                        break;
                    case KeyEvent.VK_D:
                        player1MovingRight = false;
                        break;

                    case KeyEvent.VK_UP:
                        player2MovingUp = false;
                        break;
                    case KeyEvent.VK_DOWN:
                        player2MovingDown = false;
                        break;
                    case KeyEvent.VK_LEFT:
                        player2MovingLeft = false;
                        break;
                    case KeyEvent.VK_RIGHT:
                        player2MovingRight = false;
                        break;
                }
            }
        });
        setFocusable(true);

        // Game loop
        gameLoop = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGame();
                repaint();
            }
        });
        gameLoop.start();
    }

    private void updateGame() {
        // Update player positions
        if (player1MovingUp) player1Y -= 5;
        if (player1MovingDown) player1Y += 5;
        if (player1MovingLeft) player1X -= 5;
        if (player1MovingRight) player1X += 5;

        if (player2MovingUp) player2Y -= 5;
        if (player2MovingDown) player2Y += 5;
        if (player2MovingLeft) player2X -= 5;
        if (player2MovingRight) player2X += 5;

        // Collision detection
        if (checkCollision(player1X, player1Y, player2X, player2Y)) {
            System.out.println("Players collided!");
        }
    }

    private boolean checkCollision(int x1, int y1, int x2, int y2) {
        Rectangle player1 = new Rectangle(x1, y1, PLAYER_SIZE, PLAYER_SIZE);
        Rectangle player2 = new Rectangle(x2, y2, PLAYER_SIZE, PLAYER_SIZE);
        return player1.intersects(player2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.fillRect(player1X, player1Y, PLAYER_SIZE, PLAYER_SIZE);
        g.fillRect(player2X, player2Y, PLAYER_SIZE, PLAYER_SIZE);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Virtual Wrestling Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new VirtualWrestlingGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}