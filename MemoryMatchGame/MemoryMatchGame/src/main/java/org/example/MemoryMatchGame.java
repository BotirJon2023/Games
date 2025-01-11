package org.example;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

public class MemoryMatch {
    private static final List<String> cardDeck = new ArrayList<>();

    public static void initializeDeck() {
        String[] cards = {"A", "B", "C", "D", "E", "F", "G", "H"};
        for (String card : cards) {
            cardDeck.add(card);
            cardDeck.add(card); // Add pairs
        }
        Collections.shuffle(cardDeck); // Shuffle the deck
    }

    public static void printBoard(List<String> board) {
        for (int i = 0; i < board.size(); i++) {
            System.out.print(board.get(i) + " ");
            if ((i + 1) % 4 == 0) System.out.println();
        }
    }

    public static boolean isGameOver(List<String> board) {
        return !board.contains("*"); // Check if all pairs are matched
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        initializeDeck();

        List<String> board = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            board.add("*");
        }

        int attempts = 0;

        while (!isGameOver(board)) {
            printBoard(board);

            System.out.println("Enter two card positions (1-16): ");
            int firstPos = scanner.nextInt() - 1;
            int secondPos = scanner.nextInt() - 1;

            if (cardDeck.get(firstPos).equals(cardDeck.get(secondPos))) {
                board.set(firstPos, cardDeck.get(firstPos));
                board.set(secondPos, cardDeck.get(secondPos));
            }

            attempts++;
        }

        printBoard(board);
        System.out.println("Game Over! You won in " + attempts + " attempts.");
    }
}
