package org.example;

import java.util.*;

public class Match3Game {
    static final int ROWS = 8;
    static final int COLS = 8;
    static final int NUM_TYPES = 5; // Number of different tile types
    static Tile[][] board = new Tile[ROWS][COLS];
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        initializeBoard();
        displayBoard();

        while (true) {
            System.out.println("\nEnter a swap (row1 col1 row2 col2) or 'exit' to quit:");
            String input = scanner.nextLine();
            if (input.equals("exit")) {
                System.out.println("Game Over!");
                break;
            }

            String[] tokens = input.split(" ");
            if (tokens.length == 4) {
                int row1 = Integer.parseInt(tokens[0]);
                int col1 = Integer.parseInt(tokens[1]);
                int row2 = Integer.parseInt(tokens[2]);
                int col2 = Integer.parseInt(tokens[3]);

                if (isValidSwap(row1, col1, row2, col2)) {
                    swapTiles(row1, col1, row2, col2);
                    if (checkMatches()) {
                        removeMatches();
                        fillBoard();
                    } else {
                        // Swap back if no match
                        swapTiles(row1, col1, row2, col2);
                    }
                } else {
                    System.out.println("Invalid swap! Try again.");
                }
            }
            displayBoard();
        }
    }

    // Initialize the board with random tiles
    public static void initializeBoard() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                board[i][j] = new Tile(randomTileType());
            }
        }
    }

    // Display the game board
    public static void displayBoard() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
    }

    // Check if the swap is valid (adjacent tiles)
    public static boolean isValidSwap(int row1, int col1, int row2, int col2) {
        return (Math.abs(row1 - row2) == 1 && col1 == col2) || (Math.abs(col1 - col2) == 1 && row1 == row2);
    }

    // Swap the tiles on the board
    public static void swapTiles(int row1, int col1, int row2, int col2) {
        Tile temp = board[row1][col1];
        board[row1][col1] = board[row2][col2];
        board[row2][col2] = temp;
    }

    // Check if there are any matching tiles in rows or columns
    public static boolean checkMatches() {
        boolean hasMatch = false;

        // Check rows
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS - 2; j++) {
                if (board[i][j].getType() == board[i][j + 1].getType() && board[i][j].getType() == board[i][j + 2].getType()) {
                    hasMatch = true;
                }
            }
        }

        // Check columns
        for (int j = 0; j < COLS; j++) {
            for (int i = 0; i < ROWS - 2; i++) {
                if (board[i][j].getType() == board[i + 1][j].getType() && board[i][j].getType() == board[i + 2][j].getType()) {
                    hasMatch = true;
                }
            }
        }

        return hasMatch;
    }

    // Remove matched tiles
    public static void removeMatches() {
        // Mark tiles for removal
        boolean[][] toRemove = new boolean[ROWS][COLS];

        // Check rows for matches
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS - 2; j++) {
                if (board[i][j].getType() == board[i][j + 1].getType() && board[i][j].getType() == board[i][j + 2].getType()) {
                    toRemove[i][j] = toRemove[i][j + 1] = toRemove[i][j + 2] = true;
                }
            }
        }

        // Check columns for matches
        for (int j = 0; j < COLS; j++) {
            for (int i = 0; i < ROWS - 2; i++) {
                if (board[i][j].getType() == board[i + 1][j].getType() && board[i][j].getType() == board[i + 2][j].getType()) {
                    toRemove[i][j] = toRemove[i + 1][j] = toRemove[i + 2][j] = true;
                }
            }
        }

        // Remove matched tiles
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (toRemove[i][j]) {
                    board[i][j] = new Tile(randomTileType());
                }
            }
        }
    }

    // Fill the board with new tiles after removal
    public static void fillBoard() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (board[i][j] == null || board[i][j].getType() == null) {
                    board[i][j] = new Tile(randomTileType());
                }
            }
        }
    }

    // Get a random tile type
    public static int randomTileType() {
        return (int) (Math.random() * NUM_TYPES);
    }
}
2.

Tile Class(e.g ., Tile.java)

java
        Kopieren

public static class Tile {
    private int type;

    public Tile(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.valueOf(type);
    }
}

public void main() {
}
