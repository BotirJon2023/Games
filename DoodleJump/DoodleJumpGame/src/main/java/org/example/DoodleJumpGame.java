package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class DoodleJumpGame extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PLAYER_WIDTH = 50;
    private static final int PLAYER_HEIGHT = 50;
    private static final int PLATFORM_WIDTH = 100;
    private static final int PLATFORM_HEIGHT = 20;
    private static final int GRAVITY = 1;
    private static final int JUMP_STRENGTH = -20;
    private static final int PLATFORM_VELOCITY = 3;

    private Timer timer;
    private int playerX, playerY, playerVelocityY;
    private ArrayList<Rectangle> platforms;
    private int score;
    private boolean gameOver;
    private boolean gameStarted;

    public DoodleJumpGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.cyan);
        setFocusable(true);
        addKeyListener(this);

        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 100;
        playerVelocityY = 0;
        platforms = new ArrayList<>();
        score = 0;
        gameOver = false;
        gameStarted = false;

        timer = new Timer(20, this);
        timer.start();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBackground(g);
        drawPlayer(g);
        drawPlatforms(g);
        drawScore(g);
        if (gameOver) {
            drawGameOver(g);
        }
        if (!gameStarted) {
            drawStartMessage(g);
        }
    }

    private void drawBackground(Graphics g) {
        g.setColor(Color.cyan);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(Color.green);
        g.fillRect(0, HEIGHT - 100, WIDTH, 100);
    }

    private void drawPlayer(Graphics g) {
        g.setColor(Color.red);
        g.fillRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
    }

    private void drawPlatforms(Graphics g) {
        g.setColor(Color.green);
        for (Rectangle platform : platforms) {
            g.fillRect(platform.x, platform.y, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        }
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("Score: " + score, 10, 30);
    }

    private void drawGameOver(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 100));
        g.drawString("GAME OVER", WIDTH / 2 - 250, HEIGHT / 2 - 50);
    }

    private void drawStartMessage(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        g.drawString("Press SPACE to Start", WIDTH / 2 - 200, HEIGHT / 2 - 50);
    }

    private void jump() {
        if (gameOver) {
            resetGame();
        }
        if (!gameStarted) {
            gameStarted = true;
        }
        playerVelocityY = JUMP_STRENGTH;
    }

    private void resetGame() {
        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 100;
        playerVelocityY = 0;
        platforms.clear();
        score = 0;
        gameOver = false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameStarted) {
            playerVelocityY += GRAVITY;
            playerY += playerVelocityY;

            if (playerY > HEIGHT - PLAYER_HEIGHT - 100) {
                playerY = HEIGHT - PLAYER_HEIGHT - 100;
                playerVelocityY = 0;
            }

            if (playerY < 0) {
                playerY = 0;
                playerVelocityY = 0;
            }

            addPlatforms();
            movePlatforms();
            checkCollisions();
            repaint();
        }
    }

    private void addPlatforms() {
        if (platforms.isEmpty() || platforms.get(platforms.size() - 1).y < HEIGHT - 200) {
            Random rand = new Random();
            int platformX = rand.nextInt(WIDTH - PLATFORM_WIDTH);
            int platformY = rand.nextInt(HEIGHT - 100) + 100;
            platforms.add(new Rectangle(platformX, platformY, PLATFORM_WIDTH, PLATFORM_HEIGHT));
        }
    }

    private void movePlatforms() {
        ArrayList<Rectangle> toRemove = new ArrayList<>();
        for (Rectangle platform : platforms) {
            platform.y += PLATFORM_VELOCITY;
            if (platform.y > HEIGHT) {
                toRemove.add(platform);
                score++;
            }
        }
        platforms.removeAll(toRemove);
    }

    private void checkCollisions() {
        for (Rectangle platform : platforms) {
            if (new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT).intersects(platform)) {
                if (playerVelocityY > 0 && playerY + PLAYER_HEIGHT <= platform.y) {
                    playerY = platform.y - PLAYER_HEIGHT;
                    playerVelocityY = 0;
                    break;
                }
            }
        }

        if (playerY > HEIGHT) {
            gameOver = true;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            jump();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Doodle Jump");
        DoodleJumpGame game = new DoodleJumpGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
