package org.example;

import java.util.Scanner;

public class PegSolitaireGame {


    // Define board size and structure
    private static final int BOARD_SIZE = 7;
    private char[][] board;

    public PegSolitaireGame() {
        board = new char[BOARD_SIZE][BOARD_SIZE];
        initializeBoard();
    }

    // Initialize the board with 'O' for pegs and '.' for empty spaces.
    private void initializeBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if ((row == 3 && col == 3)) {
                    board[row][col] = '.'; // The starting empty spot
                } else if ((row >= 0 && row <= 2 && col >= 0 && col <= 2) ||
                        (row >= 4 && row <= 6 && col >= 4 && col <= 6) ||
                        (row >= 0 && row <= 6 && col == 3)) {
                    board[row][col] = 'O'; // Pegs
                } else {
                    board[row][col] = '.'; // Empty spaces
                }
            }
        }
    }

    // Display the current board state
    public void displayBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
    }

    // Check if a move is valid (moving a peg)
    public boolean isValidMove(int startX, int startY, int endX, int endY) {
        // Ensure the move is within bounds and follows the rules (2 positions jump)
        if (startX < 0 || startX >= BOARD_SIZE || startY < 0 || startY >= BOARD_SIZE ||
                endX < 0 || endX >= BOARD_SIZE || endY < 0 || endY >= BOARD_SIZE) {
            return false;
        }

        if (board[startX][startY] != 'O' || board[endX][endY] != '.') {
            return false; // No peg at start or destination is not empty
        }

        // Check if it's a valid jump (2 spaces)
        int dx = Math.abs(startX - endX);
        int dy = Math.abs(startY - endY);
        if ((dx == 2 && dy == 0) || (dy == 2 && dx == 0)) {
            int middleX = (startX + endX) / 2;
            int middleY = (startY + endY) / 2;
            if (board[middleX][middleY] == 'O') {
                return true; // Valid jump, middle space has a peg
            }
        }

        return false;
    }

    // Make the move (move a peg)
    public void makeMove(int startX, int startY, int endX, int endY) {
        board[startX][startY] = '.';   // Remove the peg from start
        board[endX][endY] = 'O';       // Place the peg in the destination
        int middleX = (startX + endX) / 2;
        int middleY = (startY + endY) / 2;
        board[middleX][middleY] = '.'; // Remove the jumped-over peg
    }

    // Check if the game is over (no valid moves left)
    public boolean isGameOver() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == 'O') {
                    if (isValidMove(i, j, i + 2, j) || isValidMove(i, j, i - 2, j) ||
                            isValidMove(i, j, i, j + 2) || isValidMove(i, j, i, j - 2)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // Main game loop
    public void startGame() {
        Scanner scanner = new Scanner(System.in);
        while (!isGameOver()) {
            displayBoard();
            System.out.println("Enter your move (e.g., 2 2 4 2 to move a peg from (2,2) to (4,2)): ");
            int startX = scanner.nextInt();
            int startY = scanner.nextInt();
            int endX = scanner.nextInt();
            int endY = scanner.nextInt();

            if (isValidMove(startX, startY, endX, endY)) {
                makeMove(startX, startY, endX, endY);
            } else {
                System.out.println("Invalid move, try again.");
            }
        }

        displayBoard();
        System.out.println("Game Over!");
        scanner.close();
    }

    // Main method to start the game
    public static void main(String[] args) {
        PegSolitaireGame game = new PegSolitaireGame();
        game.startGame();
    }
}
