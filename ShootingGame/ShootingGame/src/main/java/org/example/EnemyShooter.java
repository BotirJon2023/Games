package org.example;

import java.awt.Graphics;
import java.util.ArrayList;

public class EnemyShooter {

    int x, y;
    ArrayList<Bullet> bullets = new ArrayList<>();

    public EnemyShooter(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }

    public void shoot() {
        bullets.add(new Bullet(x + 20, y + 50)); // Shoot from bottom of enemy
    }

    public void update() {
        for (Bullet bullet : bullets) {
            bullet.update();
        }
    }

    public void render(Graphics g) {
        g.fillRect(x, y, 50, 50); // Draw enemy
        for (Bullet bullet : bullets) {
            bullet.render(g);
        }
    }
}
