package org.example;

import java.util.*;

public class CrosswordPuzzleGenerator {

    private static final int SIZE = 15; // Define the grid size
    private char[][] grid = new char[SIZE][SIZE];
    private List<String> wordList;
    private Map<String, List<int[]>> wordCoordinates = new HashMap<>();

    public CrosswordPuzzleGenerator(List<String> words) {
        this.wordList = words;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = '*'; // Initialize all cells with '*'
            }
        }
    }

    public boolean placeWord(String word, int row, int col, boolean horizontal) {
        if (horizontal) {
            if (col + word.length() > SIZE) return false; // Check bounds
            for (int i = 0; i < word.length(); i++) {
                if (grid[row][col + i] != '*' && grid[row][col + i] != word.charAt(i)) {
                    return false; // Conflict with existing word
                }
            }
            for (int i = 0; i < word.length(); i++) {
                grid[row][col + i] = word.charAt(i); // Place word horizontally
                wordCoordinates.computeIfAbsent(word, k -> new ArrayList<>())
                        .add(new int[]{row, col + i});
            }
        } else {
            if (row + word.length() > SIZE) return false; // Check bounds
            for (int i = 0; i < word.length(); i++) {
                if (grid[row + i][col] != '*' && grid[row + i][col] != word.charAt(i)) {
                    return false; // Conflict with existing word
                }
            }
            for (int i = 0; i < word.length(); i++) {
                grid[row + i][col] = word.charAt(i); // Place word vertically
                wordCoordinates.computeIfAbsent(word, k -> new ArrayList<>())
                        .add(new int[]{row + i, col});
            }
        }
        return true;
    }

    public boolean tryPlaceWord(String word) {
        Random rand = new Random();
        int attempts = 100;
        while (attempts-- > 0) {
            int row = rand.nextInt(SIZE);
            int col = rand.nextInt(SIZE);
            boolean horizontal = rand.nextBoolean();
            if (placeWord(word, row, col, horizontal)) {
                return true;
            }
        }
        return false;
    }

    public void generatePuzzle() {
        Collections.shuffle(wordList); // Shuffle words for random placement
        for (String word : wordList) {
            if (!tryPlaceWord(word)) {
                System.out.println("Failed to place word: " + word);
            }
        }
        fillEmptySpaces();
    }

    private void fillEmptySpaces() {
        Random rand = new Random();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grid[i][j] == '*') {
                    grid[i][j] = (char) ('A' + rand.nextInt(26)); // Fill with random letters
                }
            }
        }
    }

    public void printGrid() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                System.out.print(grid[i][j] + " ");
            }
            System.out.println();
        }
    }

    public void printHints() {
        System.out.println("\nHints:");
        for (String word : wordList) {
            List<int[]> coordinates = wordCoordinates.get(word);
            StringBuilder hint = new StringBuilder(word + " : ");
            for (int[] coordinate : coordinates) {
                hint.append("[").append(coordinate[0]).append(", ").append(coordinate[1]).append("] ");
            }
            System.out.println(hint);
        }
    }

    public void printWordList() {
        System.out.println("\nWord List: ");
        for (String word : wordList) {
            System.out.println(word);
        }
    }

    public boolean isWordPresentInGrid(String word, int row, int col, boolean horizontal) {
        if (horizontal) {
            if (col + word.length() > SIZE) return false;
            for (int i = 0; i < word.length(); i++) {
                if (grid[row][col + i] != '*' && grid[row][col + i] != word.charAt(i)) {
                    return false;
                }
            }
            return true;
        } else {
            if (row + word.length() > SIZE) return false;
            for (int i = 0; i < word.length(); i++) {
                if (grid[row + i][col] != '*' && grid[row + i][col] != word.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
    }

    public void addWordToGrid(String word, int row, int col, boolean horizontal) {
        if (isWordPresentInGrid(word, row, col, horizontal)) {
            placeWord(word, row, col, horizontal);
        }
    }

    public void interactivePuzzleGame() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the crossword puzzle game!");
        printWordList();
        while (true) {
            System.out.println("\nEnter a word to guess or 'quit' to exit:");
            String input = scanner.nextLine().toUpperCase();
            if (input.equals("QUIT")) {
                System.out.println("Exiting the game...");
                break;
            }
            if (wordList.contains(input)) {
                System.out.println("Great! You found the word: " + input);
                wordList.remove(input);
                if (wordList.isEmpty()) {
                    System.out.println("Congratulations, you've found all the words!");
                    break;
                }
            } else {
                System.out.println("Word not in list or already found. Try again.");
            }
        }
        scanner.close();
    }

    public static void main(String[] args) {
        List<String> words = Arrays.asList("JAVA", "PUZZLE", "PROGRAMMING", "CROSSWORD", "JAVADEVELOPER", "CODING", "COMPUTER", "ALGORITHM");
        CrosswordPuzzleGenerator generator = new CrosswordPuzzleGenerator(words);
        generator.generatePuzzle();
        generator.printGrid();
        generator.printHints();
        generator.interactivePuzzleGame();
    }
}