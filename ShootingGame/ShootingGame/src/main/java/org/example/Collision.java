package org.example;

import java.awt.Rectangle;

public class Collision {

    public static boolean checkCollision(Rectangle player, Rectangle enemy) {
        return player.intersects(enemy);
    }
}
