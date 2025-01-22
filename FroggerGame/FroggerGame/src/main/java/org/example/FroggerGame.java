package org.example;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import javax.swing.Timer;

public class FroggerGame extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 800, HEIGHT = 600;
    private static final int FROG_SIZE = 40, CAR_WIDTH = 60, CAR_HEIGHT = 40;
    private static final int NUM_CARS = 5, NUM_LANES = 5;
    private Timer timer;
    private Rectangle frog;
    private ArrayList<Rectangle> cars;
    private boolean up, down, left, right;
    private int score = 0;

    public FroggerGame() {
        frog = new Rectangle(WIDTH / 2 - FROG_SIZE / 2, HEIGHT - FROG_SIZE - 10, FROG_SIZE, FROG_SIZE);
        cars = new ArrayList<>();
        for (int i = 0; i < NUM_LANES; i++) {
            for (int j = 0; j < NUM_CARS; j++) {
                int x = j * (WIDTH / NUM_CARS) + (int) (Math.random() * (WIDTH / NUM_CARS));
                int y = i * (HEIGHT / NUM_LANES);
                cars.add(new Rectangle(x, y, CAR_WIDTH, CAR_HEIGHT));
            }
        }

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        timer = new Timer(1000 / 60, this); // 60 FPS
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (up) frog.y -= 5;
        if (down) frog.y += 5;
        if (left) frog.x -= 5;
        if (right) frog.x += 5;

        // Keep the frog within bounds
        frog.x = Math.max(0, Math.min(WIDTH - FROG_SIZE, frog.x));
        frog.y = Math.max(0, Math.min(HEIGHT - FROG_SIZE, frog.y));

        // Move cars
        for (Rectangle car : cars) {
            car.x += (Math.random() > 0.5 ? 1 : -1) * 5; // Move left or right
            if (car.x < 0) car.x = WIDTH;
            if (car.x > WIDTH) car.x = 0;
        }

        // Collision detection
        for (Rectangle car : cars) {
            if (frog.intersects(car)) {
                resetGame();
                return;
            }
        }

        // Score system: if frog reaches top
        if (frog.y < 0) {
            score++;
            resetGame();
        }

        repaint();
    }

    private void resetGame() {
        frog.setLocation(WIDTH / 2 - FROG_SIZE / 2, HEIGHT - FROG_SIZE - 10);
        cars.clear();
        for (int i = 0; i < NUM_LANES; i++) {
            for (int j = 0; j < NUM_CARS; j++) {
                int x = j * (WIDTH / NUM_CARS) + (int) (Math.random() * (WIDTH / NUM_CARS));
                int y = i * (HEIGHT / NUM_LANES);
                cars.add(new Rectangle(x, y, CAR_WIDTH, CAR_HEIGHT));
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw frog
        g.setColor(Color.GREEN);
        g.fillRect(frog.x, frog.y, frog.width, frog.height);

        // Draw cars
        g.setColor(Color.RED);
        for (Rectangle car : cars) {
            g.fillRect(car.x, car.y, car.width, car.height);
        }

        // Draw score
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 20);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                up = true;
                break;
            case KeyEvent.VK_DOWN:
                down = true;
                break;
            case KeyEvent.VK_LEFT:
                left = true;
                break;
            case KeyEvent.VK_RIGHT:
                right = true;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                up = false;
                break;
            case KeyEvent.VK_DOWN:
                down = false;
                break;
            case KeyEvent.VK_LEFT:
                left = false;
                break;
            case KeyEvent.VK_RIGHT:
                right = false;
                break;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Frogger");
        FroggerGame game = new FroggerGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(game);
        frame.pack();
        frame.setVisible(true);
    }
}
