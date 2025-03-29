import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Random;

public class CreepyDollHorrorAdventure extends JPanel implements ActionListener {
    private Timer timer;
    private Player player;
    private ArrayList<Doll> dolls;
    private boolean flicker = false;
    private int flickerCount = 0;
    private Random random = new Random();

    public CreepyDollHorrorAdventure() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(new KeyHandler());

        player = new Player(400, 500);
        dolls = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            dolls.add(new Doll(random.nextInt(750), random.nextInt(300)));
        }

        timer = new Timer(50, this);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (flicker) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        } else {
            player.draw(g);
            for (Doll doll : dolls) {
                doll.draw(g);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        flickerCount++;
        if (flickerCount % 20 == 0) {
            flicker = random.nextBoolean();
        }
        player.move();
        for (Doll doll : dolls) {
            doll.move();
        }
        repaint();
    }

    private class KeyHandler extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            player.keyPressed(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            player.keyReleased(e);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Creepy Doll Horror Adventure");
        CreepyDollHorrorAdventure game = new CreepyDollHorrorAdventure();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

class Player {
    private int x, y, dx;
    private final int SPEED = 5;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, 30, 30);
    }

    public void move() {
        x += dx;
        x = Math.max(0, Math.min(770, x));
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) dx = -SPEED;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) dx = SPEED;
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) dx = 0;
    }
}

class Doll {
    private int x, y, dy;
    private final int SPEED = 2;
    private Random random = new Random();

    public Doll(int x, int y) {
        this.x = x;
        this.y = y;
        this.dy = SPEED;
    }

    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillOval(x, y, 30, 30);
    }

    public void move() {
        y += dy;
        if (y > 600) {
            y = -random.nextInt(100);
            x = random.nextInt(750);
        }
    }
}
