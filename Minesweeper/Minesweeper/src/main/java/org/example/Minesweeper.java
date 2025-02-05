package org.example;

import java.util.Random;
import java.util.Scanner;

public class Minesweeper {
    private static final int BOARD_SIZE = 9;
    private static final int NUM_MINES = 10;
    private static char[][] board = new char[BOARD_SIZE][BOARD_SIZE];
    private static boolean[][] revealed = new boolean[BOARD_SIZE][BOARD_SIZE];
    private static boolean[][] mines = new boolean[BOARD_SIZE][BOARD_SIZE];

    public static void main(String[] args) {
        initializeBoard();
        placeMines();
        printBoard();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Enter row (0 to " + (BOARD_SIZE - 1) + "): ");
            int row = scanner.nextInt();
            System.out.println("Enter column (0 to " + (BOARD_SIZE - 1) + "): ");
            int col = scanner.nextInt();

            if (isValidMove(row, col)) {
                if (mines[row][col]) {
                    System.out.println("Boom! You hit a mine. Game over.");
                    break;
                } else {
                    revealCell(row, col);
                    if (checkWin()) {
                        System.out.println("Congratulations! You've won the game.");
                        break;
                    }
                }
            } else {
                System.out.println("Invalid move. Try again.");
            }
        }
    }

    private static void initializeBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = '-';
                revealed[i][j] = false;
                mines[i][j] = false;
            }
        }
    }

    private static void placeMines() {
        Random rand = new Random();
        int minesPlaced = 0;
        while (minesPlaced < NUM_MINES) {
            int row = rand.nextInt(BOARD_SIZE);
            int col = rand.nextInt(BOARD_SIZE);
            if (!mines[row][col]) {
                mines[row][col] = true;
                minesPlaced++;
            }
        }
    }

    private static void printBoard() {
        System.out.println("Minesweeper Game Board:");
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (revealed[i][j]) {
                    System.out.print(board[i][j] + " ");
                } else {
                    System.out.print("- ");
                }
            }
            System.out.println();
        }
    }

    private static boolean isValidMove(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE && !revealed[row][col];
    }

    private static void revealCell(int row, int col) {
        if (revealed[row][col]) {
            return;
        }

        int adjacentMines = countAdjacentMines(row, col);
        revealed[row][col] = true;
        board[row][col] = (adjacentMines == 0) ? ' ' : (char) ('0' + adjacentMines);

        if (adjacentMines == 0) {
            // If no adjacent mines, recursively reveal neighboring cells
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int newRow = row + i;
                    int newCol = col + j;
                    if (isValidMove(newRow, newCol) && !mines[newRow][newCol]) {
                        revealCell(newRow, newCol);
                    }
                }
            }
        }
    }

    private static int countAdjacentMines(int row, int col) {
        int count = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int newRow = row + i;
                int newCol = col + j;
                if (newRow >= 0 && newRow < BOARD_SIZE && newCol >= 0 && newCol < BOARD_SIZE && mines[newRow][newCol]) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean checkWin() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (!mines[i][j] && !revealed[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }
}
