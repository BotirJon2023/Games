package org.example;

import java.util.Scanner;
import java.util.Random;

public class Sudoku {
    private static final int SIZE = 9;
    private static final int SUBGRIDSIZE = 3;
    private int[][] board = new int[SIZE][SIZE];
    private int[][] solution = new int[SIZE][SIZE];

    public static void main(String[] args) {
        Sudoku game = new Sudoku();
        game.generateSudoku();
        game.printBoard();
        game.play();
    }

    public Sudoku() {
        initializeBoard();
    }

    private void initializeBoard() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = 0;
            }
        }
    }

    public void generateSudoku() {
        Random random = new Random();
        generateBoard();
        generateSolution();
        removeNumbers(random);
    }

    private void generateBoard() {
        fillDiagonalSubgrids();
        solve();
    }

    private void fillDiagonalSubgrids() {
        for (int i = 0; i < SIZE; i += SUBGRIDSIZE) {
            fillSubgrid(i, i);
        }
    }

    private void fillSubgrid(int row, int col) {
        Random random = new Random();
        boolean[] used = new boolean[SIZE + 1];
        for (int i = 0; i < SUBGRIDSIZE; i++) {
            for (int j = 0; j < SUBGRIDSIZE; j++) {
                int num;
                do {
                    num = random.nextInt(SIZE) + 1;
                } while (used[num]);
                board[row + i][col + j] = num;
                used[num] = true;
            }
        }
    }

    private void generateSolution() {
        copyBoard(solution);
    }

    private void copyBoard(int[][] target) {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                target[i][j] = board[i][j];
            }
        }
    }

    private void removeNumbers(Random random) {
        int numbersToRemove = SIZE * SIZE / 2;
        while (numbersToRemove > 0) {
            int row = random.nextInt(SIZE);
            int col = random.nextInt(SIZE);
            if (board[row][col] != 0) {
                board[row][col] = 0;
                numbersToRemove--;
            }
        }
    }

    public void printBoard() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == 0) {
                    System.out.print(" . ");
                } else {
                    System.out.print(" " + board[i][j] + " ");
                }
            }
            System.out.println();
        }
    }

    public void play() {
        Scanner scanner = new Scanner(System.in);
        while (!isSolved()) {
            System.out.println("\nEnter row, column and number to place (1-9): ");
            int row = scanner.nextInt() - 1;
            int col = scanner.nextInt() - 1;
            int num = scanner.nextInt();
            if (isValidMove(row, col, num)) {
                board[row][col] = num;
            } else {
                System.out.println("Invalid move! Try again.");
            }
            printBoard();
        }
        System.out.println("Congratulations, you've solved the Sudoku!");
    }

    public boolean isValidMove(int row, int col, int num) {
        return isRowValid(row, num) && isColValid(col, num) && isSubgridValid(row, col, num);
    }

    private boolean isRowValid(int row, int num) {
        for (int col = 0; col < SIZE; col++) {
            if (board[row][col] == num) {
                return false;
            }
        }
        return true;
    }

    private boolean isColValid(int col, int num) {
        for (int row = 0; row < SIZE; row++) {
            if (board[row][col] == num) {
                return false;
            }
        }
        return true;
    }

    private boolean isSubgridValid(int row, int col, int num) {
        int subgridRowStart = row / SUBGRIDSIZE * SUBGRIDSIZE;
        int subgridColStart = col / SUBGRIDSIZE * SUBGRIDSIZE;
        for (int i = subgridRowStart; i < subgridRowStart + SUBGRIDSIZE; i++) {
            for (int j = subgridColStart; j < subgridColStart + SUBGRIDSIZE; j++) {
                if (board[i][j] == num) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isSolved() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isBoardValid() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                int num = board[i][j];
                if (num != 0) {
                    board[i][j] = 0; // Temporarily set to 0 to check validity
                    if (!isValidMove(i, j, num)) {
                        return false;
                    }
                    board[i][j] = num; // Restore value
                }
            }
        }
        return true;
    }

    public void solve() {
        if (!solve(0, 0)) {
            System.out.println("No solution exists.");
        } else {
            System.out.println("Solution found:");
            printBoard();
        }
    }

    private boolean solve(int row, int col) {
        if (row == SIZE) {
            return true;
        }
        if (col == SIZE) {
            return solve(row + 1, 0);
        }
        if (board[row][col] != 0) {
            return solve(row, col + 1);
        }
        for (int num = 1; num <= SIZE; num++) {
            if (isValidMove(row, col, num)) {
                board[row][col] = num;
                if (solve(row, col + 1)) {
                    return true;
                }
                board[row][col] = 0;
            }
        }
        return false;
    }
}
