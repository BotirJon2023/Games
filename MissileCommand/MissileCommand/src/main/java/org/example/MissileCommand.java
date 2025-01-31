package org.example;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class MissileCommand extends JPanel implements ActionListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int MISSILE_COUNT = 3;

    private Timer timer;
    private ArrayList<Missile> missiles;
    private ArrayList<EnemyMissile> enemyMissiles;
    private ArrayList<Explosion> explosions;
    private Launcher[] launchers;
    private int score;
    private boolean gameOver;

    public MissileCommand() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        missiles = new ArrayList<>();
        enemyMissiles = new ArrayList<>();
        explosions = new ArrayList<>();
        launchers = new Launcher[MISSILE_COUNT];

        for (int i = 0; i < MISSILE_COUNT; i++) {
            launchers[i] = new Launcher(100 + i * 250, HEIGHT - 50);
        }

        score = 0;
        gameOver = false;

        timer = new Timer(20, this);
        timer.start();

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    launchMissile();
                }
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        updateMissiles();
        updateEnemyMissiles();
        checkCollisions();
        spawnEnemyMissile();
        repaint();
    }

    private void updateMissiles() {
        Iterator<Missile> missileIterator = missiles.iterator();
        while (missileIterator.hasNext()) {
            Missile missile = missileIterator.next();
            missile.update();
            if (missile.getY() < 0) {
                missileIterator.remove();
            }
        }
    }

    private void updateEnemyMissiles() {
        Iterator<EnemyMissile> enemyIterator = enemyMissiles.iterator();
        while (enemyIterator.hasNext()) {
            EnemyMissile enemyMissile = enemyIterator.next();
            enemyMissile.update();
            if (enemyMissile.getY() > HEIGHT) {
                enemyIterator.remove();
                score--;
            }
        }
    }

    private void checkCollisions() {
        Iterator<EnemyMissile> enemyIterator = enemyMissiles.iterator();
        while (enemyIterator.hasNext()) {
            EnemyMissile enemyMissile = enemyIterator.next();
            Iterator<Missile> missileIterator = missiles.iterator();
            while (missileIterator.hasNext()) {
                Missile missile = missileIterator.next();
                if (enemyMissile.getBounds().intersects(missile.getBounds())) {
                    enemyIterator.remove();
                    missileIterator.remove();
                    explosions.add(new Explosion(enemyMissile.x, enemyMissile.y)); // Use direct field access here
                    score++;
                    break;
                }
            }
        }
    }

    private void spawnEnemyMissile() {
        if (Math.random() < 0.02) {
            int x = (int) (Math.random() * WIDTH);
            enemyMissiles.add(new EnemyMissile(x, 0));
        }
    }

    private void launchMissile() {
        for (Launcher launcher : launchers) {
            if (!launcher.hasMissile()) {
                missiles.add(launcher.launch());
                break;
            }
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gameOver) {
            g.setColor(Color.WHITE);
            g.drawString("Game Over! Score: " + score, WIDTH / 2 - 50, HEIGHT / 2);
            return;
        }

        for (Missile missile : missiles) {
            missile.draw(g);
        }

        for (EnemyMissile enemyMissile : enemyMissiles) {
            enemyMissile.draw(g);
        }

        for (Explosion explosion : explosions) {
            explosion.draw(g);
        }

        for (Launcher launcher : launchers) {
            launcher.draw(g);
        }

        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 20);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Missile Command");
        MissileCommand game = new MissileCommand();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    // Missile Class
    class Missile {
        private int x, y, speed;

        public Missile(int x, int y) {
            this.x = x;
            this.y = y;
            this.speed = 5;
        }

        public void update() {
            y -= speed;
        }

        public void draw(Graphics g) {
            g.setColor(Color.YELLOW);
            g.fillRect(x - 2, y - 10, 4, 10);
        }

        public Rectangle getBounds() {
            return new Rectangle(x - 2, y - 10, 4, 10);
        }

        public int getY() {
            return y;
        }
    }

    // Enemy Missile Class
    class EnemyMissile {
        private int x, y, speed;

        public EnemyMissile(int x, int y) {
            this.x = x;
            this.y = y;
            this.speed = 3 + (int) (Math.random() * 3);
        }

        public void update() {
            y += speed;
        }

        public void draw(Graphics g) {
            g.setColor(Color.RED);
            g.fillRect(x - 3, y - 10, 6, 10);
        }

        public Rectangle getBounds() {
            return new Rectangle(x - 3, y - 10, 6, 10);
        }

        public int getY() {
            return y;
        }
    }

    // Explosion Class
    class Explosion {
        private int x, y, size;

        public Explosion(int x, int y) {
            this.x = x;
            this.y = y;
            this.size = 10;
        }

        public void draw(Graphics g) {
            g.setColor(Color.ORANGE);
            g.fillOval(x - size / 2, y - size / 2, size, size);
            size += 2;
            if (size > 30) {
                explosions.remove(this);
            }
        }
    }

    // Launcher Class
    class Launcher {
        private int x, y;
        private boolean hasMissile;

        public Launcher(int x, int y) {
            this.x = x;
            this.y = y;
            this.hasMissile = true;
        }

        public Missile launch() {
            hasMissile = false;
            return new Missile(x, y);
        }

        public void draw(Graphics g) {
            g.setColor(Color.GREEN);
            g.fillRect(x - 10, y, 20, 20);
        }

        public boolean hasMissile() {
            return hasMissile;
        }
    }
}