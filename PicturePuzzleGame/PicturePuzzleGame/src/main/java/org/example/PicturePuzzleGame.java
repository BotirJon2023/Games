package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;

public class PicturePuzzleGame extends JFrame {
    private final int GRID_SIZE = 3;
    private ArrayList<JButton> buttons = new ArrayList<>();
    private JPanel panel;
    private int emptyIndex;

    public PicturePuzzleGame() {
        setTitle("Picture Puzzle Game");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        panel = new JPanel();
        panel.setLayout(new GridLayout(GRID_SIZE, GRID_SIZE));
        initializeButtons();
        add(panel, BorderLayout.CENTER);

        JButton shuffleButton = new JButton("Shuffle");
        shuffleButton.addActionListener(e -> shuffleButtons());
        add(shuffleButton, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void initializeButtons() {
        for (int i = 0; i < GRID_SIZE * GRID_SIZE - 1; i++) {
            JButton button = new JButton(String.valueOf(i + 1));
            button.addActionListener(new ButtonClickListener());
            buttons.add(button);
            panel.add(button);
        }
        JButton emptyButton = new JButton();
        emptyButton.setEnabled(false);
        buttons.add(emptyButton);
        panel.add(emptyButton);
        emptyIndex = buttons.size() - 1;
    }

    private void shuffleButtons() {
        ArrayList<String> labels = new ArrayList<>();
        for (JButton button : buttons) {
            labels.add(button.getText());
        }
        Collections.shuffle(labels);
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setText(labels.get(i));
        }
    }

    private class ButtonClickListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JButton clickedButton = (JButton) e.getSource();
            int clickedIndex = buttons.indexOf(clickedButton);
            if (isAdjacent(clickedIndex, emptyIndex)) {
                swapButtons(clickedIndex, emptyIndex);
                if (isSolved()) {
                    JOptionPane.showMessageDialog(null, "Congratulations! You solved the puzzle.");
                }
            }
        }
    }

    private boolean isAdjacent(int index1, int index2) {
        int row1 = index1 / GRID_SIZE, col1 = index1 % GRID_SIZE;
        int row2 = index2 / GRID_SIZE, col2 = index2 % GRID_SIZE;
        return (Math.abs(row1 - row2) == 1 && col1 == col2) || (Math.abs(col1 - col2) == 1 && row1 == row2);
    }

    private void swapButtons(int index1, int index2) {
        String temp = buttons.get(index1).getText();
        buttons.get(index1).setText(buttons.get(index2).getText());
        buttons.get(index2).setText(temp);
        emptyIndex = index1;
    }

    private boolean isSolved() {
        for (int i = 0; i < buttons.size() - 1; i++) {
            if (!buttons.get(i).getText().equals(String.valueOf(i + 1))) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PicturePuzzleGame::new);
    }
}
