package org.example;

public class Health {

    int health = 100;

    public void takeDamage(int amount) {
        health -= amount;
        if (health <= 0) {
            health = 0;
        }
    }

    public int getHealth() {
        return health;
    }
}
