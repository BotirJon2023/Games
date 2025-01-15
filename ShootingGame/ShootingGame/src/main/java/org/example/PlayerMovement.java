package org.example;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class PlayerMovement extends KeyAdapter {
    int x = 300, y = 300;

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W) y -= 5;
        if (key == KeyEvent.VK_S) y += 5;
        if (key == KeyEvent.VK_A) x -= 5;
        if (key == KeyEvent.VK_D) x += 5;
    }
}
