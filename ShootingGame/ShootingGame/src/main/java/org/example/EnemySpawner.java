package org.example;

import java.awt.*;
import java.util.ArrayList;
public class EnemySpawner {

    ArrayList<Enemy> enemies = new ArrayList<>();

    public void spawnEnemy() {
        enemies.add(new Enemy((int)(Math.random() * 800), -50)); // Spawn at random x position
    }

    public void update() {
        for (Enemy enemy : enemies) {
            enemy.update();
        }
    }

    public void render(Graphics g) {
        for (Enemy enemy : enemies) {
            enemy.render(g);
        }
    }
}
