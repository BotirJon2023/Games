package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;

public class NumberSortingPuzzle extends JFrame {
    private JButton[] buttons;
    private int[] numbers;
    private final int GRID_SIZE = 4;
    private final int TOTAL_NUMBERS = GRID_SIZE * GRID_SIZE;

    public NumberSortingPuzzle() {
        setTitle("Number Sorting Puzzle");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(GRID_SIZE, GRID_SIZE));

        buttons = new JButton[TOTAL_NUMBERS];
        numbers = new int[TOTAL_NUMBERS];
        initializeNumbers();
        shuffleNumbers();
        createButtons();
        updateButtons();
    }

    private void initializeNumbers() {
        for (int i = 0; i < TOTAL_NUMBERS - 1; i++) {
            numbers[i] = i + 1;
        }
        numbers[TOTAL_NUMBERS - 1] = 0; // Empty space
    }

    private void shuffleNumbers() {
        ArrayList<Integer> numberList = new ArrayList<>();
        for (int num : numbers) {
            numberList.add(num);
        }
        do {
            Collections.shuffle(numberList);
            for (int i = 0; i < TOTAL_NUMBERS; i++) {
                numbers[i] = numberList.get(i);
            }
        } while (!isSolvable());
    }

    private boolean isSolvable() {
        int inversions = 0;
        for (int i = 0; i < TOTAL_NUMBERS; i++) {
            for (int j = i + 1; j < TOTAL_NUMBERS; j++) {
                if (numbers[i] > numbers[j] && numbers[i] != 0 && numbers[j] != 0) {
                    inversions++;
                }
            }
        }
        return inversions % 2 == 0;
    }

    private void createButtons() {
        for (int i = 0; i < TOTAL_NUMBERS; i++) {
            buttons[i] = new JButton();
            buttons[i].setFont(new Font("Arial", Font.BOLD, 24));
            buttons[i].addActionListener(new ButtonClickListener(i));
            add(buttons[i]);
        }
    }

    private void updateButtons() {
        for (int i = 0; i < TOTAL_NUMBERS; i++) {
            if (numbers[i] == 0) {
                buttons[i].setText("");
            } else {
                buttons[i].setText(String.valueOf(numbers[i]));
            }
        }
    }

    private boolean isValidMove(int index) {
        int emptyIndex = findEmptySpace();
        return (index == emptyIndex - 1 && index % GRID_SIZE != GRID_SIZE - 1) ||
                (index == emptyIndex + 1 && index % GRID_SIZE != 0) ||
                (index == emptyIndex - GRID_SIZE) ||
                (index == emptyIndex + GRID_SIZE);
    }

    private int findEmptySpace() {
        for (int i = 0; i < TOTAL_NUMBERS; i++) {
            if (numbers[i] == 0) return i;
        }
        return -1;
    }

    private void swap(int index1, int index2) {
        int temp = numbers[index1];
        numbers[index1] = numbers[index2];
        numbers[index2] = temp;
    }

    private boolean isSolved() {
        for (int i = 0; i < TOTAL_NUMBERS - 1; i++) {
            if (numbers[i] != i + 1) {
                return false;
            }
        }
        return numbers[TOTAL_NUMBERS - 1] == 0;
    }

    private class ButtonClickListener implements ActionListener {
        private final int index;

        public ButtonClickListener(int index) {
            this.index = index;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isValidMove(index)) {
                swap(index, findEmptySpace());
                updateButtons();
                if (isSolved()) {
                    JOptionPane.showMessageDialog(null, "Congratulations! You solved the puzzle!");
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NumberSortingPuzzle puzzle = new NumberSortingPuzzle();
            puzzle.setVisible(true);
        });
    }
}
