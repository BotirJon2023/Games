import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class OlympicSportSimulator extends JPanel implements ActionListener {

    // Constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int TRACK_HEIGHT = 100;
    private static final int LANE_WIDTH = 50;
    private static final int NUM_LANES = 8;
    private static final int NUM_ATHLETES = 8;
    private static final int MAX_SPEED = 10;

    // Athlete class
    private class Athlete {
        int x, y;
        int speed;
        Color color;
        String name;

        Athlete(int x, int y, Color color, String name) {
            this.x = x;
            this.y = y;
            this.speed = new Random().nextInt(MAX_SPEED) + 1;
            this.color = color;
            this.name = name;
        }
    }

    // Athletes and timer
    private Athlete[] athletes;
    private Timer timer;
    private int finishTime[];

    public OlympicSportSimulator() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.WHITE);
        setFocusable(true);

        // Initialize athletes
        athletes = new Athlete[NUM_ATHLETES];
        finishTime = new int[NUM_ATHLETES];
        String[] names = {"John", "Mike", "Emma", "David", "Sophia", "Oliver", "Ava", "William"};
        for (int i = 0; i < NUM_ATHLETES; i++) {
            athletes[i] = new Athlete(0, TRACK_HEIGHT / 2 + i * LANE_WIDTH, getRandomColor(), names[i]);
        }

        // Start simulation
        timer = new Timer(1000 / 60, this);
        timer.start();
    }

    // Get a random color
    private Color getRandomColor() {
        Random rand = new Random();
        float r = rand.nextFloat();
        float g = rand.nextFloat();
        float b = rand.nextFloat();
        return new Color(r, g, b);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw track
        g.setColor(Color.BLACK);
        g.fillRect(0, TRACK_HEIGHT / 2, WIDTH, NUM_LANES * LANE_WIDTH);

        // Draw athletes
        for (int i = 0; i < NUM_ATHLETES; i++) {
            Athlete athlete = athletes[i];
            g.setColor(athlete.color);
            g.fillOval(athlete.x, athlete.y, 20, 20);
            g.setColor(Color.BLACK);
            g.drawString(athlete.name, athlete.x, athlete.y - 5);
        }

        // Draw finish line
        g.setColor(Color.WHITE);
        g.fillRect(WIDTH - 10, TRACK_HEIGHT / 2, 10, NUM_LANES * LANE_WIDTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Update athletes
        for (int i = 0; i < NUM_ATHLETES; i++) {
            Athlete athlete = athletes[i];
            athlete.x += athlete.speed;
            if (athlete.x > WIDTH - 10 && finishTime[i] == 0) {
                finishTime[i] = 1; // mark as finished
                System.out.println(athlete.name + " finished!");
            }
        }

        // Check if all athletes have finished
        boolean allFinished = true;
        for (int i = 0; i < NUM_ATHLETES; i++) {
            if (finishTime[i] == 0) {
                allFinished = false;
                break;
            }
        }
        if (allFinished) {
            timer.stop();
            System.out.println("Simulation finished!");
        }

        // Repaint
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Olympic Sport Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new OlympicSportSimulator());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}