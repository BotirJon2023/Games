package org.example;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Timer;

import static com.sun.java.accessibility.util.AWTEventMonitor.addKeyListener;
import static javax.swing.text.StyleConstants.setBackground;

public class MultipleLanesRacingGame {

    public class MultipleLanesGame extends JPanel implements ActionListener {
        private Timer timer;
        private int carX = 200, carY = 450, carWidth = 50, carHeight = 100;
        private int carSpeed = 5;
        private int roadSpeed = 5;
        private int roadPosition = 0;
        private int laneWidth = 100;
        private int laneCount = 3;

        public MultipleLanesGame() {
            setPreferredSize(new Dimension(500, 600));
            setBackground(Color.gray);
            setFocusable(true);

            timer = new Timer(1000 / 60, this);
            timer.start();

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_LEFT && carX > laneWidth) {
                        carX -= laneWidth;
                    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && carX < getWidth() - carWidth - laneWidth) {
                        carX += laneWidth;
                    }
                }
            });
        }

        private void setFocusable(boolean b) {
        }

        private void setPreferredSize(Dimension dimension) {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            roadPosition += roadSpeed;
            if (roadPosition > getHeight()) {
                roadPosition = 0;
            }
            repaint();
        }

        private void repaint() {
        }

        private int getHeight() {
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

            // Draw lanes
            g.setColor(Color.black);
            for (int i = 1; i < laneCount; i++) {
                int x = i * laneWidth;
                g.fillRect(x, 0, 5, getHeight());
            }
        }

        private int getWidth() {
        }

        public static void main(String[] args) {
            JFrame frame = new JFrame("Multiple Lanes Racing Game");
            MultipleLanesGame gamePanel = new MultipleLanesGame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(gamePanel);
            frame.pack();
            frame.setVisible(true);
        }
    }

}
