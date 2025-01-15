package org.example;

public class Collision2 {

    public static boolean checkBulletEnemyCollision(Bullet bullet, Enemy enemy) {
        return bullet.getBounds().intersects(enemy.getBounds());
    }
}
