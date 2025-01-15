package org.example;

import java.awt.Graphics;

public class Enemy {

    int x, y;
    int health = 1;

    public Enemy(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }

    public void update() {
        y += 2; // Move downward
    }

    public void render(Graphics g) {
        g.fillRect(x, y, 50, 50); // Enemy rectangle
    }
}
