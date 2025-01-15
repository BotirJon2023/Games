package org.example;

import java.awt.*;
public class PauseMenu {

    public void render(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("PAUSED", 300, 200);
        g.drawString("Press 'P' to resume", 250, 250);
    }
}
