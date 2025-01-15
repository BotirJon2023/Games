package org.example;

public class GameTimer {

    private long startTime;

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public int getTimeLeft() {
        long elapsed = System.currentTimeMillis() - startTime;
        return (int)(30 - (elapsed / 1000));
    }
}
