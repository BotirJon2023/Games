package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class HangmanGame {

    private static final String[] WORDS = {
            "programming", "java", "computer", "hangman", "challenge", "developer",
            "algorithm", "data", "structure", "exception", "variable", "function",
            "class", "object", "inheritance", "polymorphism"
    };

    private static final int MAX_TRIES = 6;
    private String wordToGuess;
    private StringBuilder currentGuess;
    private List<Character> guessedLetters;
    private int incorrectGuesses;
    private boolean isGameOver;

    public HangmanGame() {
        resetGame();
    }

    public void resetGame() {
        Random random = new Random();
        wordToGuess = WORDS[random.nextInt(WORDS.length)];
        currentGuess = new StringBuilder("_".repeat(wordToGuess.length()));
        guessedLetters = new ArrayList<>();
        incorrectGuesses = 0;
        isGameOver = false;
    }

    public boolean guessLetter(char letter) {
        if (isGameOver) {
            System.out.println("The game is over. Please start a new game.");
            return false;
        }

        if (guessedLetters.contains(letter)) {
            System.out.println("You've already guessed that letter.");
            return false;
        }

        guessedLetters.add(letter);

        if (wordToGuess.indexOf(letter) == -1) {
            incorrectGuesses++;
            System.out.println("Incorrect guess! You have " + (MAX_TRIES - incorrectGuesses) + " tries left.");
        } else {
            for (int i = 0; i < wordToGuess.length(); i++) {
                if (wordToGuess.charAt(i) == letter) {
                    currentGuess.setCharAt(i, letter);
                }
            }
            System.out.println("Correct guess!");
        }

        checkGameStatus();
        return true;
    }

    public void checkGameStatus() {
        if (incorrectGuesses >= MAX_TRIES) {
            isGameOver = true;
            System.out.println("Game Over! The word was: " + wordToGuess);
        } else if (currentGuess.toString().equals(wordToGuess)) {
            isGameOver = true;
            System.out.println("Congratulations! You've guessed the word: " + wordToGuess);
        }
    }

    public String getCurrentGuess() {
        return currentGuess.toString();
    }

    public int getIncorrectGuesses() {
        return incorrectGuesses;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public List<Character> getGuessedLetters() {
        return guessedLetters;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        HangmanGame game = new HangmanGame();

        System.out.println("Welcome to Hangman!");
        while (!game.isGameOver()) {
            System.out.println("\nCurrent word: " + game.getCurrentGuess());
            System.out.println("Guessed letters: " + game.getGuessedLetters());
            System.out.print("Enter a letter to guess: ");
            String input = scanner.nextLine().toLowerCase();

            if (input.length() != 1 || !input.matches("[a-z]")) {
                System.out.println("Please enter a valid single letter.");
                continue;
            }

            char guessedLetter = input.charAt(0);
            game.guessLetter(guessedLetter);
        }

        System.out.println("\nGame Over! Do you want to play again? (yes/no)");
        String playAgain = scanner.nextLine().toLowerCase();
        if (playAgain.equals("yes")) {
            game.resetGame();
            main(args); // Restart the game
        } else {
            System.out.println("Thank you for playing!");
        }
    }
}