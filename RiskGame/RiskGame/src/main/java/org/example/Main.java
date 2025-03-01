package org.example;

import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        List<String> playerNames = Arrays.asList("Alice", "Bob", "Charlie");
        RiskGame game = new RiskGame(playerNames);
        game.startGame();
    }
}
