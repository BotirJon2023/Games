package org.example;

import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class Mastermind {
    private static final int CODE_LENGTH = 4;
    private static final String[] COLORS = {"Red", "Green", "Blue", "Yellow", "Orange", "Purple"};

    private static String[] generateCode() {
        String[] code = new String[CODE_LENGTH];
        Random random = new Random();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code[i] = COLORS[random.nextInt(COLORS.length)];
        }
        return code;
    }

    private static int[] getFeedback(String[] guess, String[] code) {
        int[] feedback = new int[2]; // [Correct Position, Wrong Position]
        boolean[] codeUsed = new boolean[CODE_LENGTH];
        Arrays.fill(codeUsed, false);

        // Check for correct positions
        for (int i = 0; i < CODE_LENGTH; i++) {
            if (guess[i].equals(code[i])) {
                feedback[0]++;
                codeUsed[i] = true;
            }
        }

        // Check for wrong positions
        for (int i = 0; i < CODE_LENGTH; i++) {
            if (!guess[i].equals(code[i])) {
                for (int j = 0; j < CODE_LENGTH; j++) {
                    if (!codeUsed[j] && guess[i].equals(code[j])) {
                        feedback[1]++;
                        codeUsed[j] = true;
                        break;
                    }
                }
            }
        }

        return feedback;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String[] code = generateCode();
        int attempts = 10;

        while (attempts > 0) {
            System.out.println("Guess the code (" + CODE_LENGTH + " colors):");
            String[] guess = new String[CODE_LENGTH];
            for (int i = 0; i < CODE_LENGTH; i++) {
                System.out.print("Enter color " + (i + 1) + ": ");
                guess[i] = scanner.nextLine();
            }

            int[] feedback = getFeedback(guess, code);
            System.out.println("Correct Positions: " + feedback[0]);
            System.out.println("Wrong Positions: " + feedback[1]);

            if (feedback[0] == CODE_LENGTH) {
                System.out.println("Congratulations, you've guessed the code!");
                break;
            }

            attempts--;
            System.out.println("Attempts left: " + attempts);
        }

        if (attempts == 0) {
            System.out.println("Sorry, you've run out of attempts. The correct code was: " + Arrays.toString(code));
        }

        scanner.close();
    }
}
