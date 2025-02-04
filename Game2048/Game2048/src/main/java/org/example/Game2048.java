package org.example;

import java.util.Random;
import java.util.Scanner;

public class Game2048 {
    static final int SIZE = 4;
    static int[][] board = new int[SIZE][SIZE];
    static boolean gameOver = false;

    public static void main(String[] args) {
        initializeBoard();
        printBoard();

        Scanner scanner = new Scanner(System.in);

        while (!gameOver) {
            System.out.println("Enter move (W/A/S/D): ");
            String move = scanner.nextLine().toUpperCase();

            switch (move) {
                case "W":
                    moveUp();
                    break;
                case "A":
                    moveLeft();
                    break;
                case "S":
                    moveDown();
                    break;
                case "D":
                    moveRight();
                    break;
                default:
                    System.out.println("Invalid move! Use W, A, S, or D.");
                    continue;
            }

            addRandomTile();
            printBoard();

            if (isGameOver()) {
                System.out.println("Game Over!");
                gameOver = true;
            }
        }
        scanner.close();
    }

    // Initialize the board with two random tiles
    public static void initializeBoard() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = 0;
            }
        }
        addRandomTile();
        addRandomTile();
    }

    // Add a random tile (either 2 or 4) to an empty spot on the board
    public static void addRandomTile() {
        Random rand = new Random();
        int emptySpaces = 0;

        // Count empty spaces
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == 0) {
                    emptySpaces++;
                }
            }
        }

        if (emptySpaces == 0) {
            return;
        }

        // Pick a random empty space
        int randomIndex = rand.nextInt(emptySpaces);
        int value = rand.nextInt(2) == 0 ? 2 : 4;

        emptySpaces = 0;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == 0) {
                    if (emptySpaces == randomIndex) {
                        board[i][j] = value;
                        return;
                    }
                    emptySpaces++;
                }
            }
        }
    }

    // Check if the game is over (no more moves available)
    public static boolean isGameOver() {
        // Check for empty spaces
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == 0) {
                    return false;
                }
            }
        }

        // Check for adjacent tiles with the same value
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (i < SIZE - 1 && board[i][j] == board[i + 1][j]) {
                    return false;
                }
                if (j < SIZE - 1 && board[i][j] == board[i][j + 1]) {
                    return false;
                }
            }
        }

        return true;
    }

    // Move all tiles to the left
    public static void moveLeft() {
        for (int i = 0; i < SIZE; i++) {
            int[] row = board[i];
            mergeLeft(row);
        }
    }

    // Merge tiles to the left
    private static void mergeLeft(int[] row) {
        int[] newRow = new int[SIZE];
        int newIndex = 0;

        // Shift non-zero tiles to the left
        for (int i = 0; i < SIZE; i++) {
            if (row[i] != 0) {
                newRow[newIndex++] = row[i];
            }
        }

        // Merge adjacent equal tiles
        for (int i = 0; i < SIZE - 1; i++) {
            if (newRow[i] == newRow[i + 1] && newRow[i] != 0) {
                newRow[i] *= 2;
                newRow[i + 1] = 0;
                i++; // Skip the next tile after merging
            }
        }

        // Shift merged tiles to the left
        int[] finalRow = new int[SIZE];
        newIndex = 0;
        for (int i = 0; i < SIZE; i++) {
            if (newRow[i] != 0) {
                finalRow[newIndex++] = newRow[i];
            }
        }

        // Update the row
        System.arraycopy(finalRow, 0, row, 0, SIZE);
    }

    // Move all tiles to the right
    public static void moveRight() {
        for (int i = 0; i < SIZE; i++) {
            reverseRow(board[i]);
            mergeLeft(board[i]);
            reverseRow(board[i]);
        }
    }

    // Reverse the order of a row
    private static void reverseRow(int[] row) {
        for (int i = 0; i < SIZE / 2; i++) {
            int temp = row[i];
            row[i] = row[SIZE - 1 - i];
            row[SIZE - 1 - i] = temp;
        }
    }

    // Move all tiles up
    public static void moveUp() {
        for (int j = 0; j < SIZE; j++) {
            int[] column = new int[SIZE];
            for (int i = 0; i < SIZE; i++) {
                column[i] = board[i][j];
            }
            mergeLeft(column);
            for (int i = 0; i < SIZE; i++) {
                board[i][j] = column[i];
            }
        }
    }

    // Move all tiles down
    public static void moveDown() {
        for (int j = 0; j < SIZE; j++) {
            int[] column = new int[SIZE];
            for (int i = 0; i < SIZE; i++) {
                column[i] = board[i][j];
            }
            reverseRow(column);
            mergeLeft(column);
            reverseRow(column);
            for (int i = 0; i < SIZE; i++) {
                board[i][j] = column[i];
            }
        }
    }

    // Print the current board
    public static void printBoard() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                System.out.print(board[i][j] == 0 ? "." : board[i][j]);
                System.out.print("\t");
            }
            System.out.println();
        }
        System.out.println();
    }
}