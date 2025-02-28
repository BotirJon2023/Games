package org.example;

import java.util.Random;
import java.util.Scanner;

public class Battleship {
    private static final int BOARD_SIZE = 10;
    private static final char WATER = '~';
    private static final char SHIP = 'S';
    private static final char HIT = 'X';
    private static final char MISS = 'O';

    private char[][] playerBoard = new char[BOARD_SIZE][BOARD_SIZE];
    private char[][] computerBoard = new char[BOARD_SIZE][BOARD_SIZE];
    private char[][] computerHiddenBoard = new char[BOARD_SIZE][BOARD_SIZE];

    private Random random = new Random();
    private Scanner scanner = new Scanner(System.in);

    public Battleship() {
        initializeBoard(playerBoard);
        initializeBoard(computerBoard);
        initializeBoard(computerHiddenBoard);
        placeShipsRandomly(playerBoard);
        placeShipsRandomly(computerBoard);
    }

    private void initializeBoard(char[][] board) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = WATER;
            }
        }
    }

    private void placeShipsRandomly(char[][] board) {
        int shipsToPlace = 5;
        while (shipsToPlace > 0) {
            int x = random.nextInt(BOARD_SIZE);
            int y = random.nextInt(BOARD_SIZE);
            if (board[x][y] == WATER) {
                board[x][y] = SHIP;
                shipsToPlace--;
            }
        }
    }

    private void printBoard(char[][] board) {
        System.out.print("  ");
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.out.print(i + " ");
        }
        System.out.println();
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.out.print(i + " ");
            for (int j = 0; j < BOARD_SIZE; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
    }

    public void playGame() {
        while (true) {
            System.out.println("Your Board:");
            printBoard(playerBoard);
            System.out.println("Computer Board:");
            printBoard(computerHiddenBoard);

            playerTurn();
            if (checkWin(computerBoard)) {
                System.out.println("Congratulations! You win!");
                break;
            }

            computerTurn();
            if (checkWin(playerBoard)) {
                System.out.println("Computer wins! Better luck next time.");
                break;
            }
        }
    }

    private void playerTurn() {
        int x, y;
        while (true) {
            System.out.print("Enter your attack coordinates (row and column): ");
            x = scanner.nextInt();
            y = scanner.nextInt();
            if (x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE && computerHiddenBoard[x][y] == WATER) {
                break;
            }
            System.out.println("Invalid coordinates or already attacked. Try again.");
        }
        if (computerBoard[x][y] == SHIP) {
            System.out.println("Hit!");
            computerBoard[x][y] = HIT;
            computerHiddenBoard[x][y] = HIT;
        } else {
            System.out.println("Miss!");
            computerHiddenBoard[x][y] = MISS;
        }
    }

    private void computerTurn() {
        int x, y;
        do {
            x = random.nextInt(BOARD_SIZE);
            y = random.nextInt(BOARD_SIZE);
        } while (playerBoard[x][y] == HIT || playerBoard[x][y] == MISS);

        System.out.println("Computer attacks: " + x + " " + y);
        if (playerBoard[x][y] == SHIP) {
            System.out.println("Computer hit your ship!");
            playerBoard[x][y] = HIT;
        } else {
            System.out.println("Computer missed!");
            playerBoard[x][y] = MISS;
        }
    }

    private boolean checkWin(char[][] board) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == SHIP) {
                    return false;
                }
            }
        }
        return true;
    }

    public void resetGame() {
        initializeBoard(playerBoard);
        initializeBoard(computerBoard);
        initializeBoard(computerHiddenBoard);
        placeShipsRandomly(playerBoard);
        placeShipsRandomly(computerBoard);
        System.out.println("Game has been reset!");
    }

    public void printInstructions() {
        System.out.println("Welcome to Battleship!");
        System.out.println("You will take turns with the computer to attack enemy ships.");
        System.out.println("Enter coordinates as row and column numbers (e.g., 3 5). ");
        System.out.println("The game ends when all ships of one side are destroyed. Good luck!");
    }

    public static void main(String[] args) {
        Battleship game = new Battleship();
        game.printInstructions();
        game.playGame();
    }
}

