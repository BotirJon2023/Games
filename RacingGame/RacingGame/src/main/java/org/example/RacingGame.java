package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class RacingGame extends JPanel implements ActionListener {
    private Timer timer;
    private int carX = 200, carY = 450, carWidth = 50, carHeight = 100;
    private int carSpeed = 5;
    private int laneWidth = 100;
    private boolean moveLeft = false, moveRight = false;
    private int roadSpeed = 5;
    private int roadPosition = 0;

    public RacingGame() {
        setPreferredSize(new Dimension(500, 600));
        setBackground(Color.gray);
        setFocusable(true);

        timer = new Timer(1000 / 60, this);
        timer.start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    moveLeft = true;
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    moveRight = true;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    moveLeft = false;
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    moveRight = false;
                }
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (moveLeft && carX > laneWidth) {
            carX -= carSpeed;
        }
        if (moveRight && carX < getWidth() - carWidth - laneWidth) {
            carX += carSpeed;
        }

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

        // Draw the car
        g.setColor(Color.red);
        g.fillRect(carX, carY, carWidth, carHeight);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Racing Game");
        RacingGame gamePanel = new RacingGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(gamePanel);
        frame.pack();
        frame.setVisible(true);
    }
}
