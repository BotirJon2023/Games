package org.example;

import java.util.Scanner;

class Board {
    private static final int SIZE = 8;
    private char[][] board;

    public Board() {
        board = new char[SIZE][SIZE];
        initializeBoard();
    }

    private void initializeBoard() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = '-';
            }
        }
        board[3][3] = board[4][4] = 'O';
        board[3][4] = board[4][3] = 'X';
    }

    public void printBoard() {
        System.out.println("  0 1 2 3 4 5 6 7");
        for (int i = 0; i < SIZE; i++) {
            System.out.print(i + " ");
            for (int j = 0; j < SIZE; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
    }

    public boolean isValidMove(int row, int col, char player) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE || board[row][col] != '-') {
            return false;
        }
        return checkFlippable(row, col, player);
    }

    private boolean checkFlippable(int row, int col, char player) {
        char opponent = (player == 'X') ? 'O' : 'X';
        int[] dx = {-1, -1, -1, 0, 1, 1, 1, 0};
        int[] dy = {-1, 0, 1, 1, 1, 0, -1, -1};

        for (int d = 0; d < 8; d++) {
            int x = row + dx[d], y = col + dy[d];
            boolean hasOpponent = false;
            while (x >= 0 && x < SIZE && y >= 0 && y < SIZE && board[x][y] == opponent) {
                hasOpponent = true;
                x += dx[d];
                y += dy[d];
            }
            if (hasOpponent && x >= 0 && x < SIZE && y >= 0 && y < SIZE && board[x][y] == player) {
                return true;
            }
        }
        return false;
    }

    public boolean makeMove(int row, int col, char player) {
        if (!isValidMove(row, col, player)) {
            return false;
        }
        board[row][col] = player;
        flipPieces(row, col, player);
        return true;
    }

    private void flipPieces(int row, int col, char player) {
        char opponent = (player == 'X') ? 'O' : 'X';
        int[] dx = {-1, -1, -1, 0, 1, 1, 1, 0};
        int[] dy = {-1, 0, 1, 1, 1, 0, -1, -1};

        for (int d = 0; d < 8; d++) {
            int x = row + dx[d], y = col + dy[d];
            boolean hasOpponent = false;
            while (x >= 0 && x < SIZE && y >= 0 && y < SIZE && board[x][y] == opponent) {
                hasOpponent = true;
                x += dx[d];
                y += dy[d];
            }
            if (hasOpponent && x >= 0 && x < SIZE && y >= 0 && y < SIZE && board[x][y] == player) {
                x -= dx[d];
                y -= dy[d];
                while (x != row || y != col) {
                    board[x][y] = player;
                    x -= dx[d];
                    y -= dy[d];
                }
            }
        }
    }
}

class Player {
    private char symbol;

    public Player(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }
}

public class ReversiGame {
    private Board board;
    private Player player1, player2;
    private Scanner scanner;

    public ReversiGame() {
        board = new Board();
        player1 = new Player('X');
        player2 = new Player('O');
        scanner = new Scanner(System.in);
    }

    public void startGame() {
        boolean running = true;
        Player currentPlayer = player1;

        while (running) {
            board.printBoard();
            System.out.println("Player " + currentPlayer.getSymbol() + "'s turn. Enter row and column:");
            int row = scanner.nextInt();
            int col = scanner.nextInt();

            if (board.makeMove(row, col, currentPlayer.getSymbol())) {
                currentPlayer = (currentPlayer == player1) ? player2 : player1;
            } else {
                System.out.println("Invalid move. Try again.");
            }
        }
    }

    public static void main(String[] args) {
        ReversiGame game = new ReversiGame();
        game.startGame();
    }
}