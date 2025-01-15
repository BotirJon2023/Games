package org.example;

public class Score {

    private int score = 0;

    public void incrementScore() {
        score++;
    }

    public void decrementScore() {
        if (score > 0) score--;
    }

    public int getScore() {
        return score;
    }
}
