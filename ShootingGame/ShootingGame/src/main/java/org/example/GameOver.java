package org.example;

import java.awt.*;

public class GameOver {

    public void render(Graphics g) {
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        g.drawString("GAME OVER", 250, 300);
    }
}
