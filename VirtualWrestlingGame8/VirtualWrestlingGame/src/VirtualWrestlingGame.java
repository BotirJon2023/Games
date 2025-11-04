// VirtualWrestlingGame.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class VirtualWrestlingGame extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VirtualWrestlingGame game = new VirtualWrestlingGame();
            game.setVisible(true);
        });
    }

    private GamePanel gamePanel;

    public VirtualWrestlingGame() {
        setTitle("Virtual Wrestling Game");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        gamePanel = new GamePanel();
        add(gamePanel);
        addKeyListener(gamePanel.getKeyHandler());
    }

    class GamePanel extends JPanel implements ActionListener {
        private Timer timer;
        private Wrestler player, opponent;
        private boolean gameOver = false;
        private String winner = "";

        public GamePanel() {
            setBackground(Color.BLACK);
            player = new Wrestler(100, 400, Color.BLUE, "Player");
            opponent = new Wrestler(600, 400, Color.RED, "Opponent");
            timer = new Timer(30, this);
            timer.start();
        }

        public KeyAdapter getKeyHandler() {
            return new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (!gameOver) {
                        switch (e.getKeyCode()) {
                            case KeyEvent.VK_LEFT -> player.move(-10, 0);
                            case KeyEvent.VK_RIGHT -> player.move(10, 0);
                            case KeyEvent.VK_UP -> player.jump();
                            case KeyEvent.VK_SPACE -> player.attack(opponent);
                        }
                    }
                }
            };
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!gameOver) {
                player.update();
                opponentAI();
                opponent.update();
                checkGameOver();
                repaint();
            }
        }

        private void opponentAI() {
            Random rand = new Random();
            int action = rand.nextInt(100);
            if (action < 10) opponent.move(-10, 0);
            else if (action < 20) opponent.move(10, 0);
            else if (action < 25) opponent.jump();
            else if (action < 30) opponent.attack(player);
        }

        private void checkGameOver() {
            if (player.health <= 0) {
                gameOver = true;
                winner = "Opponent Wins!";
            } else if (opponent.health <= 0) {
                gameOver = true;
                winner = "Player Wins!";
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            player.draw(g);
            opponent.draw(g);
            drawHealthBars(g);
            if (gameOver) {
                g.setColor(Color.YELLOW);
                g.setFont(new Font("Arial", Font.BOLD, 36));
                g.drawString(winner, 300, 300);
            }
        }

        private void drawHealthBars(Graphics g) {
            g.setColor(Color.GREEN);
            g.fillRect(50, 50, player.health * 2, 20);
            g.setColor(Color.RED);
            g.fillRect(550, 50, opponent.health * 2, 20);
            g.setColor(Color.WHITE);
            g.drawRect(50, 50, 200, 20);
            g.drawRect(550, 50, 200, 20);
        }
    }

    class Wrestler {
        int x, y;
        int health = 100;
        int velocityY = 0;
        boolean isJumping = false;
        Color color;
        String name;

        public Wrestler(int x, int y, Color color, String name) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.name = name;
        }

        public void move(int dx, int dy) {
            x += dx;
            y += dy;
            x = Math.max(0, Math.min(x, 750));
        }

        public void jump() {
            if (!isJumping) {
                velocityY = -15;
                isJumping = true;
            }
        }

        public void update() {
            if (isJumping) {
                y += velocityY;
                velocityY += 1;
                if (y >= 400) {
                    y = 400;
                    isJumping = false;
                }
            }
        }

        public void attack(Wrestler other) {
            Rectangle myBounds = new Rectangle(x, y, 50, 100);
            Rectangle otherBounds = new Rectangle(other.x, other.y, 50, 100);
            if (myBounds.intersects(otherBounds)) {
                other.health -= 10;
                other.health = Math.max(0, other.health);
            }
        }

        public void draw(Graphics g) {
            g.setColor(color);
            g.fillRect(x, y, 50, 100);
            g.setColor(Color.WHITE);
            g.drawString(name, x, y - 10);
        }
    }
}
