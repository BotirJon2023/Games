package org.example;

public class Level {

    int currentLevel = 1;

    public void nextLevel() {
        currentLevel++;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }
}
