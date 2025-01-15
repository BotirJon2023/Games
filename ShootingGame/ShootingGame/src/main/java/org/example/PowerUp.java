package org.example;

public class PowerUp {

    int x, y;
    String type;

    public PowerUp(int startX, int startY, String type) {
        this.x = startX;
        this.y = startY;
        this.type = type;
    }

    public void update() {
        y += 2;
    }

    public void collect(Player player) {
        // Modify player based on power-up type
        if (type.equals("Health")) {
            player.increaseHealth(10);
        }
    }
}
