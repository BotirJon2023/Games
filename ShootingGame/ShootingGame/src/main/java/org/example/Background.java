package org.example;

import java.awt.*;

public class Background {

    int y1 = 0;
    int y2 = -600;

    public void update() {
        y1 += 2;
        y2 += 2;

        if (y1 > 600) y1 = -600;
        if (y2 > 600) y2 = -600;
    }

    public void render(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillRect(0, y1, 800, 600);
        g.fillRect(0, y2, 800, 600);
    }
}
