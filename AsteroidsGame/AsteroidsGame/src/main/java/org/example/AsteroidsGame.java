package org.example;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class AsteroidsGame extends JPanel implements ActionListener, KeyListener {
    Timer timer = new Timer(15, this);

    // Player Ship properties
    int playerX = 250, playerY = 250, playerAngle = 0;
    double playerSpeed = 0, playerDX = 0, playerDY = 0;
    boolean isThrusting = false;

    // Bullet properties
    ArrayList<Bullet> bullets = new ArrayList<>();
    int maxBullets = 5;

    // Asteroids properties
    ArrayList<Asteroid> asteroids = new ArrayList<>();
    int asteroidSpeed = 3;

    // Game properties
    int score = 0;
    boolean gameOver = false;

    public AsteroidsGame() {
        setPreferredSize(new Dimension(600, 600));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);
        timer.start();
        resetGame();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        // Update player movement
        updatePlayer();

        // Update bullet movement
        updateBullets();

        // Update asteroid movement
        updateAsteroids();

        // Check collisions
        checkCollisions();

        // Repaint the screen
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (gameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("GAME OVER", 200, 300);
            g.drawString("Score: " + score, 230, 350);
            return;
        }

        // Draw player ship
        drawPlayer(g);

        // Draw bullets
        for (Bullet bullet : bullets) {
            bullet.draw(g);
        }

        // Draw asteroids
        for (Asteroid asteroid : asteroids) {
            asteroid.draw(g);
        }

        // Draw score
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 20);
    }

    private void drawPlayer(Graphics g) {
        g.setColor(Color.WHITE);
        int[] xPoints = {
                playerX + (int) (Math.cos(Math.toRadians(playerAngle)) * 20),
                playerX + (int) (Math.cos(Math.toRadians(playerAngle + 120)) * 20),
                playerX + (int) (Math.cos(Math.toRadians(playerAngle + 240)) * 20)
        };
        int[] yPoints = {
                playerY + (int) (Math.sin(Math.toRadians(playerAngle)) * 20),
                playerY + (int) (Math.sin(Math.toRadians(playerAngle + 120)) * 20),
                playerY + (int) (Math.sin(Math.toRadians(playerAngle + 240)) * 20)
        };
        g.fillPolygon(xPoints, yPoints, 3);
    }

    private void updatePlayer() {
        if (isThrusting) {
            playerDX += Math.cos(Math.toRadians(playerAngle)) * 0.1;
            playerDY += Math.sin(Math.toRadians(playerAngle)) * 0.1;
        }

        // Apply speed and update player position
        playerX += playerDX;
        playerY += playerDY;

        // Wrap the player around the screen
        if (playerX > getWidth()) playerX = 0;
        if (playerX < 0) playerX = getWidth();
        if (playerY > getHeight()) playerY = 0;
        if (playerY < 0) playerY = getHeight();
    }

    private void updateBullets() {
        for (int i = 0; i < bullets.size(); i++) {
            Bullet bullet = bullets.get(i);
            bullet.update();

            // Remove bullets that go off-screen
            if (bullet.isOffScreen()) {
                bullets.remove(i);
                i--;
            }
        }
    }

    private void updateAsteroids() {
        for (Asteroid asteroid : asteroids) {
            asteroid.update();

            // Wrap asteroids around the screen
            if (asteroid.x > getWidth()) asteroid.x = 0;
            if (asteroid.x < 0) asteroid.x = getWidth();
            if (asteroid.y > getHeight()) asteroid.y = 0;
            if (asteroid.y < 0) asteroid.y = getHeight();
        }
    }

    private void checkCollisions() {
        // Check if any bullet hits an asteroid
        for (int i = 0; i < bullets.size(); i++) {
            Bullet bullet = bullets.get(i);
            for (int j = 0; j < asteroids.size(); j++) {
                Asteroid asteroid = asteroids.get(j);

                if (bullet.collidesWith(asteroid)) {
                    score += 10;
                    bullets.remove(i);
                    asteroids.remove(j);
                    i--;
                    break;
                }
            }
        }

        // Check if the player collides with any asteroid
        for (Asteroid asteroid : asteroids) {
            if (asteroid.collidesWith(playerX, playerY)) {
                gameOver = true;
            }
        }

        // If there are no asteroids left, generate new ones
        if (asteroids.isEmpty()) {
            generateAsteroids();
        }
    }

    private void generateAsteroids() {
        for (int i = 0; i < 5; i++) {
            asteroids.add(new Asteroid((int) (Math.random() * getWidth()), (int) (Math.random() * getHeight()), asteroidSpeed));
        }
    }

    private void resetGame() {
        playerX = 250;
        playerY = 250;
        playerAngle = 0;
        playerSpeed = 0;
        playerDX = 0;
        playerDY = 0;
        score = 0;
        gameOver = false;
        asteroids.clear();
        generateAsteroids();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT) {
            playerAngle -= 10;
        }
        if (key == KeyEvent.VK_RIGHT) {
            playerAngle += 10;
        }
        if (key == KeyEvent.VK_UP) {
            isThrusting = true;
        }
        if (key == KeyEvent.VK_SPACE) {
            shootBullet();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_UP) {
            isThrusting = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private void shootBullet() {
        if (bullets.size() < maxBullets) {
            Bullet bullet = new Bullet(playerX, playerY, playerAngle);
            bullets.add(bullet);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Asteroids");
        AsteroidsGame game = new AsteroidsGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    // Bullet class
    class Bullet {
        int x, y;
        double angle;
        double speed = 5;

        Bullet(int x, int y, double angle) {
            this.x = x;
            this.y = y;
            this.angle = angle;
        }

        void update() {
            x += (int) (Math.cos(Math.toRadians(angle)) * speed);
            y += (int) (Math.sin(Math.toRadians(angle)) * speed);
        }

        boolean isOffScreen() {
            return x < 0 || x > getWidth() || y < 0 || y > getHeight();
        }

        boolean collidesWith(Asteroid asteroid) {
            return Math.hypot(x - asteroid.x, y - asteroid.y) < 20;
        }

        void draw(Graphics g) {
            g.setColor(Color.YELLOW);
            g.fillOval(x - 2, y - 2, 4, 4);
        }
    }

    // Asteroid class
    class Asteroid {
        int x, y, size;
        double angle;
        int speed;

        Asteroid(int x, int y, int speed) {
            this.x = x;
            this.y = y;
            this.size = (int) (Math.random() * 30 + 20);
            this.angle = Math.random() * 360;
            this.speed = speed;
        }

        void update() {
            x += (int) (Math.cos(Math.toRadians(angle)) * speed);
            y += (int) (Math.sin(Math.toRadians(angle)) * speed);
        }

        boolean collidesWith(int playerX, int playerY) {
            return Math.hypot(x - playerX, y - playerY) < size / 2 + 10;
        }

        void draw(Graphics g) {
            g.setColor(Color.GRAY);
            g.fillOval(x - size / 2, y - size / 2, size, size);
        }
    }
}
