// HorseRacingSimulator.java

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class HorseRacingSimulator extends JPanel implements ActionListener {

    // Constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int HORSE_SIZE = 50;
    private static final int FINISH_LINE = WIDTH - HORSE_SIZE;
    private static final int NUM_HORSES = 5;

    // Horse class
    private class Horse {
        int x, y;
        int speed;
        String name;

        public Horse(int y, String name) {
            this.x = 0;
            this.y = y;
            this.speed = new Random().nextInt(5) + 1;
            this.name = name;
        }

        public void move() {
            x += speed;
        }
    }

    // Horses array
    private Horse[] horses;

    // Timer
    private Timer timer;

    public HorseRacingSimulator() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.WHITE);
        setFocusable(true);

        // Initialize horses
        horses = new Horse[NUM_HORSES];
        for (int i = 0; i < NUM_HORSES; i++) {
            horses[i] = new Horse(i * (HEIGHT / NUM_HORSES), "Horse " + (i + 1));
        }

        // Initialize timer
        timer = new Timer(1000 / 60, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw track
        g.setColor(Color.BLACK);
        g.drawLine(0, 0, 0, HEIGHT);
        g.drawLine(FINISH_LINE, 0, FINISH_LINE, HEIGHT);

        // Draw horses
        for (Horse horse : horses) {
            g.setColor(Color.RED);
            g.fillRect(horse.x, horse.y, HORSE_SIZE, HORSE_SIZE);
            g.setColor(Color.BLACK);
            g.drawString(horse.name, horse.x, horse.y - 10);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Move horses
        for (Horse horse : horses) {
            horse.move();
            if (horse.x > FINISH_LINE) {
                timer.stop();
                JOptionPane.showMessageDialog(this, horse.name + " wins!");
            }
        }

        // Repaint
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Horse Racing Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new HorseRacingSimulator());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}