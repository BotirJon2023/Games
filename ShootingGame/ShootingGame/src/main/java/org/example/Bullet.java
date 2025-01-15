package org.example;

import java.awt.Graphics;
import java.awt.Rectangle;

public class Bullet {

    int x, y;
    int speed = 10;

    public Bullet(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }

    public void update() {
        y -= speed; // Move up
    }

    public void render(Graphics g) {
        g.fillRect(x, y, 5, 10); // Draw bullet as a small rectangle
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 5, 10);
    }
}
