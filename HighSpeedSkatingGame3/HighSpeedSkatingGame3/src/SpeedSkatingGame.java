import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class SpeedSkatingGame {
    public static void main(String[] args) {
        GameWindow window = new GameWindow();
        window.start();
    }
}

// ====================== GameWindow ==========================
class GameWindow extends JFrame {
    private GamePanel panel;

    public GameWindow() {
        setTitle("High-Speed Speed Skating Game");
        setSize(1000, 600);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        panel = new GamePanel();
        add(panel);
    }

    public void start() {
        setVisible(true);
        panel.startGame();
    }
}

// ====================== GamePanel ==========================
class GamePanel extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private Skater player;
    private ArrayList<Skater> aiSkaters;
    private final int FPS = 60;
    private Track track;
    private boolean gameRunning = false;

    public GamePanel() {
        setFocusable(true);
        setBackground(Color.WHITE);
        addKeyListener(this);
        player = new Skater("YOU", 100, 100, Color.BLUE, true);
        aiSkaters = new ArrayList<>();
        aiSkaters.add(new Skater("AI 1", 100, 140, Color.RED, false));
        aiSkaters.add(new Skater("AI 2", 100, 180, Color.GREEN, false));
        aiSkaters.add(new Skater("AI 3", 100, 220, Color.ORANGE, false));
        aiSkaters.add(new Skater("AI 4", 100, 260, Color.MAGENTA, false));
        track = new Track();
        timer = new Timer(1000 / FPS, this);
    }

    public void startGame() {
        gameRunning = true;
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        track.draw(g);

        for (Skater sk : aiSkaters) {
            sk.draw(g);
        }

        player.draw(g);

        drawHUD(g);
    }

    private void drawHUD(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Laps: " + player.getLapsCompleted() + "/3", 10, 20);
        g.drawString("Speed: " + player.getSpeed(), 10, 40);

        if (player.getLapsCompleted() >= 3) {
            g.setFont(new Font("Arial", Font.BOLD, 32));
            g.setColor(Color.BLUE);
            g.drawString("🏁 You Finished the Race! 🏁", 300, 280);
            gameRunning = false;
            timer.stop();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning) return;

        player.update(track);

        for (Skater ai : aiSkaters) {
            ai.aiUpdate(track);
        }

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        player.handleKeyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        player.handleKeyReleased(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

// ====================== Skater ==========================
class Skater {
    private String name;
    private int x, y;
    private int width = 20, height = 20;
    private double speed = 0;
    private double acceleration = 0.15;
    private double maxSpeed = 6.0;
    private double friction = 0.05;
    private double angle = 0;
    private Color color;
    private boolean isPlayer;
    private boolean up, left, right;
    private int lapsCompleted = 0;
    private boolean crossedLine = false;

    public Skater(String name, int x, int y, Color color, boolean isPlayer) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.color = color;
        this.isPlayer = isPlayer;
    }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(color);
        g2d.translate(x, y);
        g2d.rotate(angle);
        g2d.fillOval(-width/2, -height/2, width, height);
        g2d.rotate(-angle);
        g2d.translate(-x, -y);
    }

    public void update(Track track) {
        if (up) {
            speed += acceleration;
            if (speed > maxSpeed) speed = maxSpeed;
        } else {
            speed -= friction;
            if (speed < 0) speed = 0;
        }

        if (left) angle -= 0.05;
        if (right) angle += 0.05;

        x += (int)(Math.cos(angle) * speed);
        y += (int)(Math.sin(angle) * speed);

        if (track.crossedFinishLine(x, y)) {
            if (!crossedLine) {
                lapsCompleted++;
                crossedLine = true;
            }
        } else {
            crossedLine = false;
        }
    }

    public void aiUpdate(Track track) {
        speed += acceleration * 0.5;
        if (speed > maxSpeed * 0.75) speed = maxSpeed * 0.75;

        angle += Math.random() * 0.02 - 0.01;

        x += (int)(Math.cos(angle) * speed);
        y += (int)(Math.sin(angle) * speed);

        if (track.crossedFinishLine(x, y)) {
            if (!crossedLine) {
                lapsCompleted++;
                crossedLine = true;
            }
        } else {
            crossedLine = false;
        }
    }

    public void handleKeyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                up = true;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                left = true;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                right = true;
                break;
        }
    }

    public void handleKeyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                up = false;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                left = false;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                right = false;
                break;
        }
    }

    public int getLapsCompleted() {
        return lapsCompleted;
    }

    public double getSpeed() {
        return Math.round(speed * 10.0) / 10.0;
    }
}

// ====================== Track ==========================
class Track {
    private final int centerX = 500;
    private final int centerY = 300;
    private final int outerRadius = 220;
    private final int innerRadius = 150;

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // Draw outer track
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillOval(centerX - outerRadius, centerY - outerRadius,
                outerRadius * 2, outerRadius * 2);

        // Draw inner area (to simulate track width)
        g2d.setColor(Color.WHITE);
        g2d.fillOval(centerX - innerRadius, centerY - innerRadius,
                innerRadius * 2, innerRadius * 2);

        // Finish line
        g2d.setColor(Color.BLACK);
        g2d.drawString("FINISH", centerX - 20, centerY - outerRadius - 10);
        g2d.drawLine(centerX - 50, centerY - outerRadius,
                centerX + 50, centerY - outerRadius);
    }

    public boolean crossedFinishLine(int x, int y) {
        return y < (centerY - outerRadius + 5) && x > centerX - 50 && x < centerX + 50;
    }
}
