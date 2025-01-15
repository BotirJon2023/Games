package org.example;

import java.awt.*;
import java.util.ArrayList;
import java.awt.event.KeyEvent;
public class Player {

    int x = 100, y = 100;
    ArrayList<Bullet> bullets = new ArrayList<>();

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            bullets.add(new Bullet(x + 20, y));
        }
    }

    public void update() {
        for (Bullet bullet : bullets) {
            bullet.update();
        }
    }

    public void render(Graphics g) {
        g.fillRect(x, y, 50, 50); // Player rectangle
        for (Bullet bullet : bullets) {
            bullet.render(g);
        }
    }
}
