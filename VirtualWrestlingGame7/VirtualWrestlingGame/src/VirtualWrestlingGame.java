import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class VirtualWrestlingGame extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private Wrestler wrestler1;
    private Wrestler wrestler2;
    private Random random;
    private int gameState; // 0 = intro, 1 = playing, 2 = ended

    public VirtualWrestlingGame() {
        setPreferredSize(new Dimension(800, 600));
        setFocusable(true);
        addKeyListener(this);
        random = new Random();
        initGame();
        timer = new Timer(16, this); // roughly 60 FPS
        timer.start();
    }

    private void initGame() {
        wrestler1 = new Wrestler("Alpha", 100, 300, Color.RED, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W);
        wrestler2 = new Wrestler("Bravo", 600, 300, Color.BLUE, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP);
        gameState = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Background
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, getWidth(), getHeight());

        drawRing(g);

        if (gameState == 0) { // intro screen
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 32));
            drawCenteredString(g, "Virtual Wrestling Game", getWidth(), getHeight() / 2 - 40);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            drawCenteredString(g, "Press ENTER to Start", getWidth(), getHeight() / 2);
        } else if (gameState == 1) { // playing
            wrestler1.draw(g);
            wrestler2.draw(g);
            drawHealthBars(g);
            drawInstructions(g);
        } else if (gameState == 2) { // ended
            String winner;
            if (wrestler1.health > 0 && wrestler2.health <= 0) winner = wrestler1.name + " Wins!";
            else if (wrestler2.health > 0 && wrestler1.health <= 0) winner = wrestler2.name + " Wins!";
            else winner = "Draw!";
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            drawCenteredString(g, winner, getWidth(), getHeight() / 2 - 20);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            drawCenteredString(g, "Press R to Restart", getWidth(), getHeight() / 2 + 40);
        }
    }

    private void drawRing(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(100, 200, 600, 200);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(100, 200, 600, 200);

        g.setColor(Color.RED);
        g.drawLine(400, 200, 400, 400);
        g.drawLine(100, 300, 700, 300);
    }

    private void drawHealthBars(Graphics g) {
        int barWidth = 200;
        int barHeight = 20;

        // Wrestler 1 Health Bar
        g.setColor(Color.RED);
        g.fillRect(50, 50, (int)(barWidth * (wrestler1.health / 100.0)), barHeight);
        g.setColor(Color.WHITE);
        g.drawRect(50, 50, barWidth, barHeight);
        g.drawString(wrestler1.name, 50, 45);

        // Wrestler 2 Health Bar
        g.setColor(Color.BLUE);
        g.fillRect(550, 50, (int)(barWidth * (wrestler2.health / 100.0)), barHeight);
        g.setColor(Color.WHITE);
        g.drawRect(550, 50, barWidth, barHeight);
        g.drawString(wrestler2.name, 550, 45);
    }

    private void drawInstructions(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Controls:", 10, 580);
        g.drawString(wrestler1.name + ": A=Left D=Right W=Attack", 10, 595);
        g.drawString(wrestler2.name + ": Left=Left Right=Right Up=Attack", 450, 595);
    }

    private void drawCenteredString(Graphics g, String text, int width, int y) {
        FontMetrics metrics = g.getFontMetrics();
        int x = (width - metrics.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == 1) {
            wrestler1.update();
            wrestler2.update();
            checkCollisions();
            checkGameOver();
        }
        repaint();
    }

    private void checkCollisions() {
        if (wrestler1.isAttacking && wrestler1.getBounds().intersects(wrestler2.getBounds())) {
            wrestler2.takeDamage(1);
        }
        if (wrestler2.isAttacking && wrestler2.getBounds().intersects(wrestler1.getBounds())) {
            wrestler1.takeDamage(1);
        }
    }

    private void checkGameOver() {
        if (wrestler1.health <= 0 || wrestler2.health <= 0) {
            gameState = 2;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (gameState == 0 && key == KeyEvent.VK_ENTER) {
            gameState = 1;
        } else if (gameState == 1) {
            wrestler1.keyPressed(key);
            wrestler2.keyPressed(key);
        } else if (gameState == 2 && key == KeyEvent.VK_R) {
            initGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        wrestler1.keyReleased(key);
        wrestler2.keyReleased(key);
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Virtual Wrestling Game");
        VirtualWrestlingGame game = new VirtualWrestlingGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(game);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Inner class Wrestler
    private class Wrestler {
        String name;
        int x, y;
        int width = 50, height = 100;
        Color color;
        int health = 100;
        int speed = 5;
        boolean movingLeft = false, movingRight = false;
        boolean isAttacking = false;
        int attackDuration = 0;
        int attackCooldown = 0;
        int keyLeft, keyRight, keyAttack;

        public Wrestler(String name, int x, int y, Color color, int keyLeft, int keyRight, int keyAttack) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.color = color;
            this.keyLeft = keyLeft;
            this.keyRight = keyRight;
            this.keyAttack = keyAttack;
        }

        public void draw(Graphics g) {
            // Draw body
            g.setColor(color);
            g.fillRect(x, y, width, height);
            // Draw attack effect
            if (isAttacking) {
                g.setColor(Color.YELLOW);
                g.fillOval(x + width, y + height / 3, 30, 30);
            }
        }

        public void update() {
            if (movingLeft) x -= speed;
            if (movingRight) x += speed;
            x = Math.max(100, Math.min(x, 700 - width));
            // Attack logic
            if (isAttacking) {
                attackDuration--;
                if (attackDuration <= 0) {
                    isAttacking = false;
                    attackCooldown = 30;
                }
            } else {
                if (attackCooldown > 0) attackCooldown--;
            }
        }

        public void takeDamage(int amount) {
            health -= amount;
            health = Math.max(0, health);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public void keyPressed(int key) {
            if (key == keyLeft) movingLeft = true;
            if (key == keyRight) movingRight = true;
            if (key == keyAttack && !isAttacking && attackCooldown == 0) {
                isAttacking = true;
                attackDuration = 10;
            }
        }

        public void keyReleased(int key) {
            if (key == keyLeft) movingLeft = false;
            if (key == keyRight) movingRight = false;
        }
    }
}
