package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class JetpackJoyrideGame extends JPanel implements ActionListener, KeyListener {

    private Timer timer;
    private int playerX, playerY, playerVelocityY;
    private boolean upKeyPressed;
    private final int PLAYER_WIDTH = 50, PLAYER_HEIGHT = 50;
    private final int SCREEN_WIDTH = 800, SCREEN_HEIGHT = 600;
    private final int GRAVITY = 1;
    private final int JUMP_STRENGTH = -15;
    private boolean gameOver;
    private int score;
    private Rectangle obstacle;

    public JetpackJoyrideGame() {
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(Color.CYAN);
        this.setFocusable(true);
        this.addKeyListener(this);

        playerX = 100;
        playerY = SCREEN_HEIGHT / 2;
        playerVelocityY = 0;
        upKeyPressed = false;
        gameOver = false;
        score = 0;

        obstacle = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT - 100, 50, 50); // Simple obstacle at the bottom of the screen

        timer = new Timer(1000 / 60, this);
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            // Game mechanics
            if (upKeyPressed) {
                playerVelocityY = JUMP_STRENGTH;
            }

            // Gravity effect
            playerVelocityY += GRAVITY;
            playerY += playerVelocityY;

            // Prevent the player from falling below the ground
            if (playerY > SCREEN_HEIGHT - PLAYER_HEIGHT) {
                playerY = SCREEN_HEIGHT - PLAYER_HEIGHT;
                playerVelocityY = 0;
            }

            // Update obstacle position (moving left to simulate scrolling)
            obstacle.x -= 5;
            if (obstacle.x < 0) {
                obstacle.x = SCREEN_WIDTH;
                score++;
            }

            // Check for collisions with the obstacle
            if (obstacle.intersects(new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT))) {
                gameOver = true;
            }

            repaint();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw player (a simple rectangle for now)
        g.setColor(Color.RED);
        g.fillRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        // Draw obstacle (simple rectangle)
        g.setColor(Color.BLACK);
        g.fillRect(obstacle.x, obstacle.y, obstacle.width, obstacle.height);

        // Display score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Score: " + score, 20, 30);

        // If the game is over, show the game over message
        if (gameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.drawString("GAME OVER", SCREEN_WIDTH / 2 - 150, SCREEN_HEIGHT / 2);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            upKeyPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            upKeyPressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not needed
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Jetpack Joyride");
        JetpackJoyrideGame game = new JetpackJoyrideGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
} 