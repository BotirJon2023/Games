import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class OlympicSportSimulator extends JPanel implements ActionListener, KeyListener {

    public static final int WIDTH = 1000;
    public static final int HEIGHT = 700;

    public static final int TIMER_DELAY = 16; // ~60 FPS


    private Timer timer;
    private boolean running = true;

    private SportType currentSport = SportType.SPRINT;

    private int globalTick = 0;


    private List<Athlete> athletes = new ArrayList<>();
    private Track track;
    private Javelin javelin;
    private SwimmingPool pool;


    private boolean leftPressed;
    private boolean rightPressed;
    private boolean spacePressed;


    public OlympicSportSimulator() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.WHITE);
        setFocusable(true);
        addKeyListener(this);

        initGame();

        timer = new Timer(TIMER_DELAY, this);
        timer.start();
    }


    private void initGame() {
        athletes.clear();

        // Create athletes
        athletes.add(new Athlete("Runner A", Color.RED, 100, 500));
        athletes.add(new Athlete("Runner B", Color.BLUE, 100, 550));
        athletes.add(new Athlete("Runner C", Color.GREEN, 100, 600));

        track = new Track();
        javelin = new Javelin();
        pool = new SwimmingPool();
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (!running) return;

        updateGame();
        repaint();
    }

    private void updateGame() {
        globalTick++;

        switch (currentSport) {
            case SPRINT:
                updateSprint();
                break;
            case JAVELIN:
                updateJavelin();
                break;
            case SWIMMING:
                updateSwimming();
                break;
        }
    }


    private void updateSprint() {
        for (Athlete a : athletes) {
            a.updateRunning();
        }
    }

    private void updateJavelin() {
        javelin.update();
    }

    private void updateSwimming() {
        for (Athlete a : athletes) {
            a.updateSwimming();
        }
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawHeader(g2d);

        switch (currentSport) {
            case SPRINT:
                drawSprint(g2d);
                break;
            case JAVELIN:
                drawJavelin(g2d);
                break;
            case SWIMMING:
                drawSwimming(g2d);
                break;
        }
    }

    private void drawHeader(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Olympic Sport Simulator", 20, 30);

        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("1 = Sprint | 2 = Javelin | 3 = Swimming | SPACE = Action", 20, 55);
        g.drawString("Current Sport: " + currentSport, 20, 75);
    }

    private void drawSprint(Graphics2D g) {
        track.draw(g);
        for (Athlete a : athletes) {
            a.draw(g);
        }
    }

    private void drawJavelin(Graphics2D g) {
        g.setColor(new Color(230, 230, 230));
        g.fillRect(0, 100, WIDTH, HEIGHT);

        javelin.draw(g);
    }

    private void drawSwimming(Graphics2D g) {
        pool.draw(g);
        for (Athlete a : athletes) {
            a.drawSwimming(g);
        }
    }


    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_1) currentSport = SportType.SPRINT;
        if (key == KeyEvent.VK_2) currentSport = SportType.JAVELIN;
        if (key == KeyEvent.VK_3) currentSport = SportType.SWIMMING;

        if (key == KeyEvent.VK_LEFT) leftPressed = true;
        if (key == KeyEvent.VK_RIGHT) rightPressed = true;
        if (key == KeyEvent.VK_SPACE) spacePressed = true;

        if (currentSport == SportType.JAVELIN && spacePressed) {
            javelin.throwJavelin();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT) leftPressed = false;
        if (key == KeyEvent.VK_RIGHT) rightPressed = false;
        if (key == KeyEvent.VK_SPACE) spacePressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}


    public static void main(String[] args) {
        JFrame frame = new JFrame("Olympic Sport Simulator");
        OlympicSportSimulator game = new OlympicSportSimulator();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

enum SportType {
    SPRINT,
    JAVELIN,
    SWIMMING
}


class Athlete {

    private String name;
    private Color color;

    private int x;
    private int y;

    private int width = 30;
    private int height = 40;

    private double speed = 2.0;
    private double swimPhase = 0;

    public Athlete(String name, Color color, int x, int y) {
        this.name = name;
        this.color = color;
        this.x = x;
        this.y = y;
    }

    public void updateRunning() {
        x += speed;
        if (x > OlympicSportSimulator.WIDTH - 50) {
            x = 100;
        }
    }

    public void updateSwimming() {
        swimPhase += 0.1;
        x += 1;
        if (x > OlympicSportSimulator.WIDTH - 50) {
            x = 100;
        }
    }

    public void draw(Graphics2D g) {
        g.setColor(color);
        g.fillRect(x, y, width, height);

        g.setColor(Color.BLACK);
        g.drawString(name, x - 10, y - 5);
    }

    public void drawSwimming(Graphics2D g) {
        g.setColor(color);
        int wave = (int) (Math.sin(swimPhase) * 5);
        g.fillOval(x, y + wave, width, height / 2);
    }
}

class Track {

    public void draw(Graphics2D g) {
        g.setColor(new Color(200, 200, 200));
        g.fillRect(0, 400, OlympicSportSimulator.WIDTH, 300);

        g.setColor(Color.WHITE);
        for (int i = 0; i < 6; i++) {
            g.drawLine(0, 420 + i * 40, OlympicSportSimulator.WIDTH, 420 + i * 40);
        }

        g.setColor(Color.BLACK);
        g.drawLine(800, 400, 800, 700);
        g.drawString("Finish", 760, 390);
    }
}


class Javelin {

    private double x = 100;
    private double y = 500;

    private double vx = 0;
    private double vy = 0;

    private boolean flying = false;

    public void throwJavelin() {
        if (!flying) {
            vx = 8 + Math.random() * 4;
            vy = -10 - Math.random() * 3;
            flying = true;
        }
    }

    public void update() {
        if (flying) {
            x += vx;
            y += vy;
            vy += 0.4;

            if (y > 550) {
                y = 550;
                flying = false;
            }
        }
    }

    public void draw(Graphics2D g) {
        g.setColor(Color.DARK_GRAY);
        g.drawLine((int) x, (int) y, (int) x + 40, (int) y - 5);

        g.setColor(Color.BLACK);
        g.drawString("Javelin Throw", 20, 120);
    }
}


class SwimmingPool {

    public void draw(Graphics2D g) {
        g.setColor(new Color(0, 120, 200));
        g.fillRect(0, 200, OlympicSportSimulator.WIDTH, 300);

        g.setColor(Color.WHITE);
        for (int i = 0; i < 5; i++) {
            g.drawLine(0, 240 + i * 50, OlympicSportSimulator.WIDTH, 240 + i * 50);
        }

        g.setColor(Color.BLACK);
        g.drawString("Swimming Competition", 20, 190);
    }
}
