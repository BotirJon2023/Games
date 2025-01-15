package org.example;

import javax.swing.*;
import java.awt.*;

public class GameLoop extends JPanel implements Runnable {
    private Thread thread;
    private boolean running = false;

    public GameLoop() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        running = true;
        while (running) {
            update();
            repaint();
            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {
        // Update game elements like player, enemies, bullets
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Render game elements
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Shooting Game");
        GameLoop game = new GameLoop();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        game.start();
    }

}
