import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class MixedMartialArtsSimulator4 {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameFrame());
    }
}

class GameFrame extends JFrame {
    public GameFrame() {
        setTitle("Mixed Martial Arts Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        add(new GamePanel());
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}

class GamePanel extends JPanel implements ActionListener {
    private final int WIDTH = 800;
    private final int HEIGHT = 600;
    private final int DELAY = 30;
    private Timer timer;
    private Fighter fighter1;
    private Fighter fighter2;
    private Random random = new Random();
    private int round = 1;
    private int maxRounds = 3;
    private boolean fightOver = false;
    private String resultText = "";

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);
        initGame();
    }

    private void initGame() {
        fighter1 = new Fighter("Dragon", 100, 100, Color.RED);
        fighter2 = new Fighter("Tiger", 600, 100, Color.BLUE);
        timer = new Timer(DELAY, this);
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!fightOver) {
            fighter1.move();
            fighter2.move();
            handleCombat();
            repaint();
        }
    }

    private void handleCombat() {
        if (fighter1.getBounds().intersects(fighter2.getBounds())) {
            int damage1 = fighter1.attack();
            int damage2 = fighter2.attack();
            fighter1.takeDamage(damage2);
            fighter2.takeDamage(damage1);

            if (fighter1.isKO() || fighter2.isKO()) {
                round++;
                if (round > maxRounds) {
                    fightOver = true;
                    determineWinner();
                    timer.stop();
                } else {
                    resetRound();
                }
            }
        }
    }

    private void determineWinner() {
        if (fighter1.getWins() > fighter2.getWins()) {
            resultText = fighter1.getName() + " wins the match!";
        } else if (fighter2.getWins() > fighter1.getWins()) {
            resultText = fighter2.getName() + " wins the match!";
        } else {
            resultText = "The match is a draw!";
        }
    }

    private void resetRound() {
        fighter1.reset(100, 100);
        fighter2.reset(600, 100);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawRing(g);
        fighter1.draw(g);
        fighter2.draw(g);
        drawHUD(g);
    }

    private void drawRing(Graphics g) {
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(50, 50, WIDTH - 100, HEIGHT - 100);
    }

    private void drawHUD(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Round: " + round + "/" + maxRounds, 10, 20);
        g.drawString(fighter1.getName() + " HP: " + fighter1.getHealth(), 10, 40);
        g.drawString(fighter2.getName() + " HP: " + fighter2.getHealth(), 600, 40);
        g.drawString(fighter1.getName() + " Wins: " + fighter1.getWins(), 10, 60);
        g.drawString(fighter2.getName() + " Wins: " + fighter2.getWins(), 600, 60);
        if (fightOver) {
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.setColor(Color.YELLOW);
            g.drawString(resultText, WIDTH / 2 - 150, HEIGHT / 2);
        }
    }
}

class Fighter {
    private String name;
    private int x, y;
    private int health;
    private final int maxHealth = 100;
    private int wins = 0;
    private Color color;
    private Rectangle bounds;
    private Random random = new Random();

    public Fighter(String name, int x, int y, Color color) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.health = maxHealth;
        this.color = color;
        this.bounds = new Rectangle(x, y, 50, 100);
    }

    public void move() {
        int dx = random.nextInt(5) - 2;
        int dy = random.nextInt(3) - 1;
        x += dx;
        y += dy;
        bounds.setLocation(x, y);
    }

    public int attack() {
        return random.nextInt(15) + 5;
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            wins = Math.max(wins - 1, 0);
        }
    }

    public void reset(int newX, int newY) {
        this.x = newX;
        this.y = newY;
        this.health = maxHealth;
        bounds.setLocation(x, y);
        if (!isKO()) wins++;
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(x, y, 50, 100);
        g.setColor(Color.BLACK);
        g.drawString(name, x, y - 10);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public boolean isKO() {
        return health <= 0;
    }

    public int getHealth() {
        return health;
    }

    public String getName() {
        return name;
    }

    public int getWins() {
        return wins;
    }
}
