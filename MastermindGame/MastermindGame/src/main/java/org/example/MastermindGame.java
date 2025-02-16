package org.example;

import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class MastermindGame {

    // Length of the secret code and the number of allowed attempts
    private static final int CODE_LENGTH = 4;
    private static final int MAX_ATTEMPTS = 10;

    // Define color symbols for the code
    private static final char[] COLORS = {'R', 'G', 'B', 'Y', 'O', 'P'}; // Red, Green, Blue, Yellow, Orange, Purple

    // Randomly generate the secret code
    private static char[] generateSecretCode() {
        Random rand = new Random();
        char[] secretCode = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            secretCode[i] = COLORS[rand.nextInt(COLORS.length)];
        }
        return secretCode;
    }

    // Provide feedback based on the player's guess
    private static String provideFeedback(char[] guess, char[] secretCode) {
        int correctPosition = 0;
        int correctColor = 0;

        // Create copies of the code and guess arrays for feedback calculation
        char[] secretCopy = Arrays.copyOf(secretCode, secretCode.length);
        char[] guessCopy = Arrays.copyOf(guess, guess.length);

        // First, count the exact matches (correct color and position)
        for (int i = 0; i < CODE_LENGTH; i++) {
            if (guess[i] == secretCode[i]) {
                correctPosition++;
                secretCopy[i] = '-'; // Mark the matched position in the secret code
                guessCopy[i] = '*';  // Mark the matched position in the guess
            }
        }

        // Second, count the correct colors in incorrect positions
        for (int i = 0; i < CODE_LENGTH; i++) {
            if (guessCopy[i] != '*' && guessCopy[i] != '-') {
                for (int j = 0; j < CODE_LENGTH; j++) {
                    if (secretCopy[j] == guessCopy[i] && secretCopy[j] != '-') {
                        correctColor++;
                        secretCopy[j] = '-'; // Mark the color as used
                        break;
                    }
                }
            }
        }

        return correctPosition + " correct position, " + correctColor + " correct color(s) but wrong position.";
    }

    // Print the current state of the game
    private static void printGameState(int attempt, char[] guess, String feedback) {
        System.out.println("Attempt " + attempt + ": " + new String(guess));
        System.out.println("Feedback: " + feedback);
        System.out.println();
    }

    // Start the Mastermind game
    private static void playGame() {
        Scanner scanner = new Scanner(System.in);
        char[] secretCode = generateSecretCode();
        int attempts = 0;
        boolean gameWon = false;

        System.out.println("Welcome to Mastermind!");
        System.out.println("Guess the secret code with " + CODE_LENGTH + " colors.");
        System.out.println("Valid colors are: R (Red), G (Green), B (Blue), Y (Yellow), O (Orange), P (Purple).");
        System.out.println("You have " + MAX_ATTEMPTS + " attempts to guess the code.\n");

        while (attempts < MAX_ATTEMPTS) {
            attempts++;

            System.out.print("Enter your guess (" + CODE_LENGTH + " colors): ");
            String guessInput = scanner.nextLine().toUpperCase();

            // Validate the guess input
            if (guessInput.length() != CODE_LENGTH) {
                System.out.println("Invalid input! Your guess must be " + CODE_LENGTH + " characters long.");
                attempts--; // Do not count this as an attempt
                continue;
            }

            char[] guess = guessInput.toCharArray();

            // Validate each character in the guess
            boolean validGuess = true;
            for (char color : guess) {
                if (Arrays.binarySearch(COLORS, color) < 0) {
                    validGuess = false;
                    break;
                }
            }

            if (!validGuess) {
                System.out.println("Invalid colors! Use only the following colors: R, G, B, Y, O, P.");
                attempts--; // Do not count this as an attempt
                continue;
            }

            // Provide feedback on the guess
            String feedback = provideFeedback(guess, secretCode);
            printGameState(attempts, guess, feedback);

            if (feedback.startsWith(CODE_LENGTH + " correct position")) {
                gameWon = true;
                break;
            }
        }

        if (gameWon) {
            System.out.println("Congratulations! You guessed the secret code!");
        } else {
            System.out.println("Sorry! You've used all your attempts. The secret code was: " + new String(secretCode));
        }
    }

    public static void main(String[] args) {
        playGame();
    }
}
