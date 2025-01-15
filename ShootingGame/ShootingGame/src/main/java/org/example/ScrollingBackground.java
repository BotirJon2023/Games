package org.example;

import java.awt.*;
public class ScrollingBackground {

    int speed = 2;
    int y = 0;

    public void update() {
        y += speed;
        if (y >= 600) {
            y = 0; // Reset background to loop
        }
    }

    public void render(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, y, 800, 600);
    }
}
