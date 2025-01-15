package org.example;

public class EnemyDeath {

    int x, y;
    boolean isAlive = true;

    public void update() {
        if (isAlive) {
            y += 2;
        } else {
        }
    }

    public void die() {
        isAlive = false;
    }
}
