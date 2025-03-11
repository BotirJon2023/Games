package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

class Enemy {
    int x, y, health, speed;
    boolean isAlive = true;

    public Enemy(int x, int y, int speed, int health) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.health = health;
    }

    public void move() {
        if (health <= 0) {
            isAlive = false;
        }
        x += speed;
    }
}

class Tower {
    int x, y, range;

    public Tower(int x, int y, int range) {
        this.x = x;
        this.y = y;
        this.range = range;
    }
}

class Bullet {
    int x, y, speed;
    Enemy target;

    public Bullet(int x, int y, int speed, Enemy target) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.target = target;
    }

    public void move() {
        if (target.isAlive) {
            x += (target.x - x) / speed;
            y += (target.y - y) / speed;
            if (Math.abs(x - target.x) < 5 && Math.abs(y - target.y) < 5) {
                target.health -= 10;
            }
        }
    }
}

public class TowerDefenseGame extends JPanel implements ActionListener {
    private Timer timer;
    private ArrayList<Enemy> enemies;
    private ArrayList<Tower> towers;
    private ArrayList<Bullet> bullets;
    private Random random;

    public TowerDefenseGame() {
        this.enemies = new ArrayList<>();
        this.towers = new ArrayList<>();
        this.bullets = new ArrayList<>();
        this.random = new Random();
        this.timer = new Timer(100, this);
        this.timer.start();

        towers.add(new Tower(300, 200, 100));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.GREEN);
        for (Tower tower : towers) {
            g.fillRect(tower.x, tower.y, 20, 20);
        }
        g.setColor(Color.RED);
        for (Enemy enemy : enemies) {
            g.fillRect(enemy.x, enemy.y, 20, 20);
        }
        g.setColor(Color.BLUE);
        for (Bullet bullet : bullets) {
            g.fillOval(bullet.x, bullet.y, 5, 5);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (random.nextInt(10) > 8) {
            enemies.add(new Enemy(0, 200, 5, 30));
        }

        for (Enemy enemy : enemies) {
            enemy.move();
        }

        for (Tower tower : towers) {
            for (Enemy enemy : enemies) {
                if (enemy.isAlive && Math.abs(tower.x - enemy.x) < tower.range) {
                    bullets.add(new Bullet(tower.x, tower.y, 10, enemy));
                }
            }
        }

        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.move();
            if (!bullet.target.isAlive) {
                bulletIterator.remove();
            }
        }

        enemies.removeIf(enemy -> !enemy.isAlive);
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tower Defense");
        TowerDefenseGame game = new TowerDefenseGame();
        frame.add(game);
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
