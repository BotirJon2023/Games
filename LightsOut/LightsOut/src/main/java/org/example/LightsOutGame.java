package org.example;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class LightsOutGame extends JFrame {
    private static final int GRID_SIZE = 5;
    private static final int BUTTON_SIZE = 100;
    private JButton[][] buttons;
    private boolean[][] lightState; // True for ON, false for OFF

    public LightsOutGame() {
        setTitle("Lights Out Game");
        setSize(GRID_SIZE * BUTTON_SIZE, GRID_SIZE * BUTTON_SIZE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(GRID_SIZE, GRID_SIZE));

        buttons = new JButton[GRID_SIZE][GRID_SIZE];
        lightState = new boolean[GRID_SIZE][GRID_SIZE];

        // Initialize buttons and add action listeners
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                buttons[i][j] = new JButton();
                buttons[i][j].setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
                buttons[i][j].setBackground(Color.RED);
                buttons[i][j].setOpaque(true);
                buttons[i][j].addActionListener(new ButtonListener(i, j));
                add(buttons[i][j]);
                lightState[i][j] = true; // Initially, all lights are ON
            }
        }

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Action listener for the buttons
    private class ButtonListener implements ActionListener {
        private int x, y;

        public ButtonListener(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            toggleLight(x, y);
            if (x > 0) toggleLight(x - 1, y); // Toggle above
            if (x < GRID_SIZE - 1) toggleLight(x + 1, y); // Toggle below
            if (y > 0) toggleLight(x, y - 1); // Toggle left
            if (y < GRID_SIZE - 1) toggleLight(x, y + 1); // Toggle right

            checkWinCondition();
        }
    }

    private void toggleLight(int x, int y) {
        lightState[x][y] = !lightState[x][y];
        if (lightState[x][y]) {
            buttons[x][y].setBackground(Color.RED); // ON state
        } else {
            buttons[x][y].setBackground(Color.BLACK); // OFF state
        }
    }

    private void checkWinCondition() {
        boolean win = true;
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (lightState[i][j]) { // If any light is ON
                    win = false;
                    break;
                }
            }
        }

        if (win) {
            JOptionPane.showMessageDialog(this, "You Win!");
            resetGame();
        }
    }

    private void resetGame() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                lightState[i][j] = true; // Reset all lights to ON
                buttons[i][j].setBackground(Color.RED);
            }
        }
    }

    public static void main(String[] args) {
        // Optionally, randomize the initial state of the grid
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LightsOutGame game = new LightsOutGame();
                game.randomizeGrid();
            }
        });
    }

    private void randomizeGrid() {
        // Randomize some lights to be off at the start of the game
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (Math.random() < 0.5) {
                    toggleLight(i, j);
                }
            }
        }
    }
}