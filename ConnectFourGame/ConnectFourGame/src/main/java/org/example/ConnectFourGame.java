package org.example;

import java.util.Scanner;

public class ConnectFourGame {

    private char[][] board;
    private char currentPlayer;
    private int rows;
    private int cols;
    private Scanner scanner;
    private boolean gameWon;
    private boolean gameDraw;

    public ConnectFourGame(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        board = new char[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                board[i][j] = ' ';
            }
        }
        currentPlayer = 'X';
        scanner = new Scanner(System.in);
        gameWon = false;
        gameDraw = false;
    }

    public void initializeBoard() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                board[i][j] = ' ';
            }
        }
    }

    public void printBoard() {
        System.out.println("-----------------------------");
        for (int i = 0; i < rows; i++) {
            System.out.print("| ");
            for (int j = 0; j < cols; j++) {
                System.out.print(board[i][j] + " | ");
            }
            System.out.println();
            System.out.println("-----------------------------");
        }
        System.out.print("  ");
        for (int j = 0; j < cols; j++) {
            System.out.print(j + "   ");
        }
        System.out.println();
    }

    public boolean isValidMove(int col) {
        if (col < 0 || col >= cols) {
            System.out.println("Column out of bounds.");
            return false;
        }
        if (board[0][col] != ' ') {
            System.out.println("Column is full.");
            return false;
        }
        return true;
    }

    public boolean makeMove(int col) {
        if (!isValidMove(col)) {
            return false;
        }
        for (int i = rows - 1; i >= 0; i--) {
            if (board[i][col] == ' ') {
                board[i][col] = currentPlayer;
                return true;
            }
        }
        return false;
    }

    public boolean checkHorizontalWin() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j <= cols - 4; j++) {
                if (board[i][j] == currentPlayer && board[i][j + 1] == currentPlayer &&
                        board[i][j + 2] == currentPlayer && board[i][j + 3] == currentPlayer) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkVerticalWin() {
        for (int i = 0; i <= rows - 4; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] == currentPlayer && board[i + 1][j] == currentPlayer &&
                        board[i + 2][j] == currentPlayer && board[i + 3][j] == currentPlayer) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkDiagonalWin() {
        // Top-left to bottom-right
        for (int i = 0; i <= rows - 4; i++) {
            for (int j = 0; j <= cols - 4; j++) {
                if (board[i][j] == currentPlayer && board[i + 1][j + 1] == currentPlayer &&
                        board[i + 2][j + 2] == currentPlayer && board[i + 3][j + 3] == currentPlayer) {
                    return true;
                }
            }
        }

        // Top-right to bottom-left
        for (int i = 0; i <= rows - 4; i++) {
            for (int j = 3; j < cols; j++) {
                if (board[i][j] == currentPlayer && board[i + 1][j - 1] == currentPlayer &&
                        board[i + 2][j - 2] == currentPlayer && board[i + 3][j - 3] == currentPlayer) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkWin() {
        return checkHorizontalWin() || checkVerticalWin() || checkDiagonalWin();
    }

    public boolean isBoardFull() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] == ' ') {
                    return false;
                }
            }
        }
        return true;
    }

    public void switchPlayer() {
        currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
    }

    public void displayWinMessage() {
        printBoard();
        System.out.println("Player " + currentPlayer + " wins!");
        gameWon = true;
    }

    public void displayDrawMessage() {
        printBoard();
        System.out.println("It's a draw!");
        gameDraw = true;
    }

    public void playGameLoop() {
        while (!gameWon && !gameDraw) {
            printBoard();
            System.out.print("Player " + currentPlayer + ", enter column (0-" + (cols - 1) + "): ");
            int col = scanner.nextInt();

            if (makeMove(col)) {
                if (checkWin()) {
                    displayWinMessage();
                } else if (isBoardFull()) {
                    displayDrawMessage();
                } else {
                    switchPlayer();
                }
            } else {
                System.out.println("Invalid move. Try again.");
            }
        }
    }

    public void play() {
        playGameLoop();
        scanner.close();
    }

    public static void main(String[] args) {
        ConnectFourGame game = new ConnectFourGame(6, 7);
        game.play();
    }
}
