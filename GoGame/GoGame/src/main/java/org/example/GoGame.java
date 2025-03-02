package org.example;

import java.util.Arrays;
import java.util.Scanner;

class GoGame {
    private static final int BOARD_SIZE = 9;
    private char[][] board;
    private char currentPlayer;
    private int blackCaptures;
    private int whiteCaptures;

    public GoGame() {
        board = new char[BOARD_SIZE][BOARD_SIZE];
        for (char[] row : board) {
            Arrays.fill(row, '.');
        }
        currentPlayer = 'B';
        blackCaptures = 0;
        whiteCaptures = 0;
    }

    public void displayBoard() {
        System.out.println("  0 1 2 3 4 5 6 7 8");
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.out.print(i + " ");
            for (int j = 0; j < BOARD_SIZE; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println("Black Captures: " + blackCaptures + " | White Captures: " + whiteCaptures);
    }

    public boolean placeStone(int x, int y) {
        if (!isValidMove(x, y)) {
            System.out.println("Invalid move! Try again.");
            return false;
        }
        board[x][y] = currentPlayer;
        int capturedStones = checkCapture(x, y);
        updateCaptureCount(capturedStones);
        switchPlayer();
        return true;
    }

    private boolean isValidMove(int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE && board[x][y] == '.';
    }

    private void switchPlayer() {
        currentPlayer = (currentPlayer == 'B') ? 'W' : 'B';
    }

    private int checkCapture(int x, int y) {
        char opponent = (currentPlayer == 'B') ? 'W' : 'B';
        int totalCaptured = 0;
        totalCaptured += tryCapture(x - 1, y, opponent);
        totalCaptured += tryCapture(x + 1, y, opponent);
        totalCaptured += tryCapture(x, y - 1, opponent);
        totalCaptured += tryCapture(x, y + 1, opponent);
        return totalCaptured;
    }

    private int tryCapture(int x, int y, char opponent) {
        if (isInsideBoard(x, y) && board[x][y] == opponent && isSurrounded(x, y)) {
            return removeGroup(x, y);
        }
        return 0;
    }

    private boolean isInsideBoard(int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
    }

    private boolean isSurrounded(int x, int y) {
        boolean[][] visited = new boolean[BOARD_SIZE][BOARD_SIZE];
        return isSurroundedDFS(x, y, board[x][y], visited);
    }

    private boolean isSurroundedDFS(int x, int y, char color, boolean[][] visited) {
        if (!isInsideBoard(x, y)) return false;
        if (visited[x][y] || board[x][y] == '.') return false;
        if (board[x][y] != color) return true;
        visited[x][y] = true;
        return isSurroundedDFS(x - 1, y, color, visited) &&
                isSurroundedDFS(x + 1, y, color, visited) &&
                isSurroundedDFS(x, y - 1, color, visited) &&
                isSurroundedDFS(x, y + 1, color, visited);
    }

    private int removeGroup(int x, int y) {
        char color = board[x][y];
        return removeDFS(x, y, color);
    }

    private int removeDFS(int x, int y, char color) {
        if (!isInsideBoard(x, y) || board[x][y] != color) return 0;
        board[x][y] = '.';
        return 1 + removeDFS(x - 1, y, color) + removeDFS(x + 1, y, color) +
                removeDFS(x, y - 1, color) + removeDFS(x, y + 1, color);
    }

    private void updateCaptureCount(int captured) {
        if (currentPlayer == 'B') {
            blackCaptures += captured;
        } else {
            whiteCaptures += captured;
        }
    }

    public void startGame() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            displayBoard();
            System.out.println("Current Player: " + currentPlayer);
            System.out.print("Enter row and column (or -1 to quit): ");
            int x = scanner.nextInt();
            if (x == -1) break;
            int y = scanner.nextInt();
            placeStone(x, y);
        }
        scanner.close();
        System.out.println("Game Over!");
    }

    public static void main(String[] args) {
        GoGame game = new GoGame();
        game.startGame();
    }
}

class Player {
    private String name;
    private char color;
    private int score;

    public Player(String name, char color) {
        this.name = name;
        this.color = color;
        this.score = 0;
    }

    public String getName() {
        return name;
    }

    public char getColor() {
        return color;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public void displayInfo() {
        System.out.println("Player: " + name + " | Color: " + color + " | Score: " + score);
    }
}
