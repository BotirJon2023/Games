package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class PandemicSimulation extends JPanel {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int POPULATION = 200;
    private static final int PERSON_SIZE = 5;
    private static final double INFECTION_PROBABILITY = 0.05;
    private static final int RECOVERY_TIME = 500;

    private final ArrayList<Person> people;
    private final Random random;

    public PandemicSimulation() {
        this.people = new ArrayList<>();
        this.random = new Random();

        for (int i = 0; i < POPULATION; i++) {
            people.add(new Person(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextBoolean()));
        }
        people.get(0).infected = true; // Patient zero

        new Timer(20, e -> updateSimulation()).start();
    }

    private void updateSimulation() {
        for (Person p : people) {
            p.move();
            p.updateInfectionStatus();
            for (Person other : people) {
                if (p != other && p.isCloseTo(other) && p.infected && !other.recovered) {
                    if (random.nextDouble() < INFECTION_PROBABILITY) {
                        other.infected = true;
                    }
                }
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Person p : people) {
            g.setColor(p.recovered ? Color.BLUE : (p.infected ? Color.RED : Color.GREEN));
            g.fillOval(p.x, p.y, PERSON_SIZE, PERSON_SIZE);
        }
    }

    private static class Person {
        int x, y, dx, dy, infectionTime;
        boolean infected, recovered;

        public Person(int x, int y, boolean moving) {
            this.x = x;
            this.y = y;
            this.dx = moving ? (int) (Math.random() * 3 - 1) : 0;
            this.dy = moving ? (int) (Math.random() * 3 - 1) : 0;
            this.infected = false;
            this.recovered = false;
            this.infectionTime = 0;
        }

        public void move() {
            if (!recovered) {
                x += dx;
                y += dy;
                if (x < 0 || x > WIDTH) dx = -dx;
                if (y < 0 || y > HEIGHT) dy = -dy;
            }
        }

        public boolean isCloseTo(Person other) {
            int distance = (x - other.x) * (x - other.x) + (y - other.y) * (y - other.y);
            return distance < 100;
        }

        public void updateInfectionStatus() {
            if (infected) {
                infectionTime++;
                if (infectionTime > RECOVERY_TIME) {
                    infected = false;
                    recovered = true;
                }
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Pandemic Simulation");
        PandemicSimulation simulation = new PandemicSimulation();
        frame.add(simulation);
        frame.setSize(WIDTH, HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
