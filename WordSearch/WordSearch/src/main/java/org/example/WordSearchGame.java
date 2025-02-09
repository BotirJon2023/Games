package org.example;

import java.util.*;

public class WordSearchGame {
    private static final int SIZE = 10;  // Grid size (10x10)
    private static char[][] grid;
    private static List<String> wordsToFind;
    private static List<String> foundWords;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Initialize the game
        grid = new char[SIZE][SIZE];
        wordsToFind = new ArrayList<>();
        foundWords = new ArrayList<>();

        // Define some words to place in the grid
        wordsToFind.add("JAVA");
        wordsToFind.add("SEARCH");
        wordsToFind.add("CODE");
        wordsToFind.add("PROGRAM");
        wordsToFind.add("GAME");
        wordsToFind.add("WORD");

        // Fill the grid with random letters
        fillGridWithRandomLetters();

        // Place words in the grid
        placeWordsInGrid();

        // Display the grid
        System.out.println("Welcome to the Word Search game!");
        displayGrid();

        // Start the game
        playGame(scanner);

        scanner.close();
    }

    private static void playGame(Scanner scanner) {
        while (foundWords.size() < wordsToFind.size()) {
            System.out.println("\nWords to find: " + wordsToFind);
            System.out.println("Found words: " + foundWords);
            System.out.print("Enter a word to search: ");
            String userInput = scanner.nextLine().toUpperCase().trim();

            if (userInput.equals("EXIT")) {
                System.out.println("Exiting the game...");
                break;
            }

            if (wordsToFind.contains(userInput) && !foundWords.contains(userInput)) {
                if (findWordInGrid(userInput)) {
                    foundWords.add(userInput);
                    System.out.println("You found the word: " + userInput);
                } else {
                    System.out.println("The word " + userInput + " is not in the grid.");
                }
            } else {
                System.out.println("Invalid input or word already found.");
            }

            if (foundWords.size() == wordsToFind.size()) {
                System.out.println("Congratulations! You found all the words!");
            }
        }
    }

    private static boolean findWordInGrid(String word) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (grid[row][col] == word.charAt(0)) {
                    if (checkWordInAllDirections(row, col, word)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean checkWordInAllDirections(int row, int col, String word) {
        int[] rowDir = {-1, -1, -1, 0, 1, 1, 1, 0};
        int[] colDir = {-1, 0, 1, 1, 1, 0, -1, -1};

        for (int dir = 0; dir < 8; dir++) {
            if (checkWordInDirection(row, col, word, rowDir[dir], colDir[dir])) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkWordInDirection(int row, int col, String word, int rowDir, int colDir) {
        int length = word.length();

        for (int i = 0; i < length; i++) {
            int newRow = row + i * rowDir;
            int newCol = col + i * colDir;

            if (newRow < 0 || newRow >= SIZE || newCol < 0 || newCol >= SIZE || grid[newRow][newCol] != word.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static void placeWordsInGrid() {
        for (String word : wordsToFind) {
            boolean placed = false;

            while (!placed) {
                Random random = new Random();
                int row = random.nextInt(SIZE);
                int col = random.nextInt(SIZE);

                int dir = random.nextInt(8);  // Random direction
                int rowDir = -1, colDir = -1;

                switch (dir) {
                    case 0:
                        rowDir = -1;
                        colDir = -1;
                        break; // Diagonal top-left
                    case 1:
                        rowDir = -1;
                        colDir = 0;
                        break;  // Up
                    case 2:
                        rowDir = -1;
                        colDir = 1;
                        break;  // Diagonal top-right
                    case 3:
                        rowDir = 0;
                        colDir = 1;
                        break;   // Right
                    case 4:
                        rowDir = 1;
                        colDir = 1;
                        break;   // Diagonal bottom-right
                    case 5:
                        rowDir = 1;
                        colDir = 0;
                        break;   // Down
                    case 6:
                        rowDir = 1;
                        colDir = -1;
                        break;  // Diagonal bottom-left
                    case 7:
                        rowDir = 0;
                        colDir = -1;
                        break;  // Left
                }

                if (canPlaceWord(row, col, word, rowDir, colDir)) {
                    placeWord(row, col, word, rowDir, colDir);
                    placed = true;
                }
            }
        }
    }

    private static boolean canPlaceWord(int row, int col, String word, int rowDir, int colDir) {
        int length = word.length();

        for (int i = 0; i < length; i++) {
            int newRow = row + i * rowDir;
            int newCol = col + i * colDir;

            if (newRow < 0 || newRow >= SIZE || newCol < 0 || newCol >= SIZE || grid[newRow][newCol] != ' ') {
                return false;
            }
        }
        return true;
    }

    private static void placeWord(int row, int col, String word, int rowDir, int colDir) {
        for (int i = 0; i < word.length(); i++) {
            grid[row + i * rowDir][col + i * colDir] = word.charAt(i);
        }
    }

    private static void fillGridWithRandomLetters() {
        Random random = new Random();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                grid[row][col] = (char) ('A' + random.nextInt(26));
            }
        }
    }

    private static void displayGrid() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                System.out.print(grid[row][col] + " ");
            }
            System.out.println();
        }
    }
}