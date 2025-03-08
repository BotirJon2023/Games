package org.example;

import java.util.*;

class HexGame {
    private final int size;
    private final char[][] board;
    private final char EMPTY = '.';
    private final char PLAYER1 = 'X';
    private final char PLAYER2 = 'O';
    private char currentPlayer;

    public HexGame(int size) {
        this.size = size;
        this.board = new char[size][size];
        for (int i = 0; i < size; i++) {
            Arrays.fill(board[i], EMPTY);
        }
        this.currentPlayer = PLAYER1;
    }

    public void printBoard() {
        System.out.println("Current Board:");
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < i; j++) {
                System.out.print(" ");
            }
            for (int j = 0; j < size; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
    }

    public boolean makeMove(int row, int col) {
        if (row < 0 || row >= size || col < 0 || col >= size || board[row][col] != EMPTY) {
            System.out.println("Invalid move! Try again.");
            return false;
        }
        board[row][col] = currentPlayer;
        return true;
    }

    public boolean checkWinner() {
        boolean[][] visited = new boolean[size][size];
        if (currentPlayer == PLAYER1) {
            for (int i = 0; i < size; i++) {
                if (board[i][0] == PLAYER1 && dfs(i, 0, visited, PLAYER1)) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (board[0][i] == PLAYER2 && dfs(0, i, visited, PLAYER2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dfs(int row, int col, boolean[][] visited, char player) {
        if ((player == PLAYER1 && col == size - 1) || (player == PLAYER2 && row == size - 1)) {
            return true;
        }
        visited[row][col] = true;
        int[][] directions = {{-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            if (newRow >= 0 && newRow < size && newCol >= 0 && newCol < size && !visited[newRow][newCol] && board[newRow][newCol] == player) {
                if (dfs(newRow, newCol, visited, player)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void switchPlayer() {
        currentPlayer = (currentPlayer == PLAYER1) ? PLAYER2 : PLAYER1;
    }

    public char getCurrentPlayer() {
        return currentPlayer;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the board size: ");
        int size = scanner.nextInt();
        HexGame game = new HexGame(size);

        while (true) {
            game.printBoard();
            System.out.println("Player " + game.getCurrentPlayer() + "'s turn. Enter row and column:");
            int row = scanner.nextInt();
            int col = scanner.nextInt();
            if (game.makeMove(row, col)) {
                if (game.checkWinner()) {
                    game.printBoard();
                    System.out.println("Player " + game.getCurrentPlayer() + " wins!");
                    break;
                }
                game.switchPlayer();
            }
        }
        scanner.close();
    }
}
