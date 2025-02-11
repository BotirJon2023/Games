package org.example;

import java.util.Scanner;
import java.util.Random;

public class SlidingTilePuzzle {

    private static final int SIZE = 3; // Size of the puzzle (3x3)
    private static final int EMPTY_TILE = 0; // Representing the empty space

    private int[][] board; // 2D array to represent the board
    private int emptyRow, emptyCol; // Position of the empty tile

    // Constructor to initialize the board
    public SlidingTilePuzzle() {
        board = new int[SIZE][SIZE];
        initializeBoard();
    }

    // Initialize the board with numbers 1 to SIZE*SIZE - 1, and the empty space at the last position
    private void initializeBoard() {
        int num = 1;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = num;
                num++;
            }
        }
        board[SIZE - 1][SIZE - 1] = EMPTY_TILE; // Set the empty tile
        emptyRow = SIZE - 1;
        emptyCol = SIZE - 1;
        shuffleBoard(); // Shuffle the board
    }

    // Shuffle the board randomly
    private void shuffleBoard() {
        Random rand = new Random();
        for (int i = 0; i < 1000; i++) {
            int direction = rand.nextInt(4); // 0 = up, 1 = down, 2 = left, 3 = right
            switch (direction) {
                case 0:
                    moveUp();
                    break;
                case 1:
                    moveDown();
                    break;
                case 2:
                    moveLeft();
                    break;
                case 3:
                    moveRight();
                    break;
            }
        }
    }

    // Move the empty tile up
    private void moveUp() {
        if (emptyRow > 0) {
            board[emptyRow][emptyCol] = board[emptyRow - 1][emptyCol];
            emptyRow--;
            board[emptyRow][emptyCol] = EMPTY_TILE;
        }
    }

    // Move the empty tile down
    private void moveDown() {
        if (emptyRow < SIZE - 1) {
            board[emptyRow][emptyCol] = board[emptyRow + 1][emptyCol];
            emptyRow++;
            board[emptyRow][emptyCol] = EMPTY_TILE;
        }
    }

    // Move the empty tile left
    private void moveLeft() {
        if (emptyCol > 0) {
            board[emptyRow][emptyCol] = board[emptyRow][emptyCol - 1];
            emptyCol--;
            board[emptyRow][emptyCol] = EMPTY_TILE;
        }
    }

    // Move the empty tile right
    private void moveRight() {
        if (emptyCol < SIZE - 1) {
            board[emptyRow][emptyCol] = board[emptyRow][emptyCol + 1];
            emptyCol++;
            board[emptyRow][emptyCol] = EMPTY_TILE;
        }
    }

    // Print the current state of the board
    public void printBoard() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == EMPTY_TILE) {
                    System.out.print("   "); // Empty space
                } else {
                    System.out.print(board[i][j] + "  ");
                }
            }
            System.out.println();
        }
    }

    // Check if the puzzle is solved
    public boolean isSolved() {
        int num = 1;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (i == SIZE - 1 && j == SIZE - 1) {
                    return board[i][j] == EMPTY_TILE;
                }
                if (board[i][j] != num++) {
                    return false;
                }
            }
        }
        return true;
    }

    // Handle user input to move tiles
    public void handleUserInput(String move) {
        switch (move.toLowerCase()) {
            case "w":
                moveUp();
                break;
            case "s":
                moveDown();
                break;
            case "a":
                moveLeft();
                break;
            case "d":
                moveRight();
                break;
            default:
                System.out.println("Invalid move! Use W, A, S, D to move.");
        }
    }

    // Main game loop
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        SlidingTilePuzzle game = new SlidingTilePuzzle();

        System.out.println("Sliding Tile Puzzle (3x3) - Use W, A, S, D to move the empty tile.");
        System.out.println("W = Up, S = Down, A = Left, D = Right");

        while (!game.isSolved()) {
            game.printBoard();
            System.out.print("Enter your move (W, A, S, D): ");
            String move = scanner.nextLine();
            game.handleUserInput(move);

            if (game.isSolved()) {
                game.printBoard();
                System.out.println("Congratulations! You solved the puzzle!");
                break;
            }
        }
        scanner.close();
    }
}