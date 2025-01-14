package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class TimeAttackGame extends JPanel implements ActionListener {
    private Timer timer;
    private int carX = 200, carY = 450, carWidth = 50, carHeight = 100;
    private int carSpeed = 5;
    private int roadSpeed = 5;
    private int roadPosition = 0;
    private int timeRemaining = 30;  // seconds

    public TimeAttackGame() {
        setPreferredSize(new Dimension(500, 600));
        setBackground(Color.gray);
        setFocusable(true);

        timer = new Timer(1000 / 60, this);
        timer.start();

        // Countdown timer for time remaining
        Timer countdown = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (timeRemaining > 0) {
                    timeRemaining--;
                } else {
                    // Stop the game when time is up
                    timer.stop();
                }
            }
        });
        countdown.start();

        // Key listener to control car movement
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    carX -= carSpeed;
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    carX += carSpeed;
                }
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Scroll road
        roadPosition += roadSpeed;
        if (roadPosition > getHeight()) {
            roadPosition = 0;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw road
        g.setColor(Color.white);
        g.fillRect(0, roadPosition, getWidth(), getHeight());
        g.fillRect(0, roadPosition - getHeight(), getWidth(), getHeight());

        // Draw car
        g.setColor(Color.red);
        g.fillRect(carX, carY, carWidth, carHeight);

        // Draw time remaining
        g.setColor(Color.white);
        g.drawString("Time Remaining: " + timeRemaining + "s", 10, 20);
    }

    public static void main(String[] args) {
        // Create and set up the frame
        JFrame frame = new JFrame("Time Attack Racing Game");
        TimeAttackGame gamePanel = new TimeAttackGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(gamePanel);
        frame.pack();
        frame.setVisible(true);
    }
}