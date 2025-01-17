package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class PacManClone extends JPanel implements ActionListener, KeyListener {
    Timer timer = new Timer(100, this);
    int pacManX = 300, pacManY = 300, pacManDirection = 0, pacManSpeed = 5;
    ArrayList<Rectangle> walls = new ArrayList<>();
    ArrayList<Point> pellets = new ArrayList<>();
    ArrayList<Point> ghosts = new ArrayList<>();
    int score = 0;
    boolean gameOver = false;

    public PacManClone() {
        setPreferredSize(new Dimension(600, 600));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);
        timer.start();
        initGame();
    }

    public void initGame() {
        // Initial pac-man position
        pacManX = 300;
        pacManY = 300;
        pacManDirection = 0;

        // Initial walls (just a few blocks for simplicity)
        walls.clear();
        walls.add(new Rectangle(100, 100, 400, 10));  // Top wall
        walls.add(new Rectangle(100, 500, 400, 10));  // Bottom wall
        walls.add(new Rectangle(100, 100, 10, 400));  // Left wall
        walls.add(new Rectangle(500, 100, 10, 400));  // Right wall

        // Initial pellet positions
        pellets.clear();
        for (int i = 100; i < 500; i += 40) {
            for (int j = 100; j < 500; j += 40) {
                if (i != 300 && j != 300) pellets.add(new Point(i, j));
            }
        }

        // Initial ghosts positions
        ghosts.clear();
        ghosts.add(new Point(200, 200));
        ghosts.add(new Point(400, 200));
        ghosts.add(new Point(200, 400));
        ghosts.add(new Point(400, 400));

        score = 0;
        gameOver = false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        movePacMan();
        checkCollisions();
        moveGhosts();
        checkPelletCollision();
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

        // Draw Pac-Man
        g.setColor(Color.YELLOW);
        g.fillArc(pacManX, pacManY, 40, 40, pacManDirection * 45, 270);

        // Draw walls
        g.setColor(Color.BLUE);
        for (Rectangle wall : walls) {
            g.fillRect(wall.x, wall.y, wall.width, wall.height);
        }

        // Draw pellets
        g.setColor(Color.WHITE);
        for (Point pellet : pellets) {
            g.fillRect(pellet.x, pellet.y, 10, 10);
        }

        // Draw ghosts
        g.setColor(Color.RED);
        for (Point ghost : ghosts) {
            g.fillOval(ghost.x, ghost.y, 40, 40);
        }

        // Draw score
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 20);
    }

    public void movePacMan() {
        if (pacManDirection == 0) pacManX += pacManSpeed; // Right
        if (pacManDirection == 1) pacManY -= pacManSpeed; // Up
        if (pacManDirection == 2) pacManX -= pacManSpeed; // Left
        if (pacManDirection == 3) pacManY += pacManSpeed; // Down

        // Handle wall collisions
        for (Rectangle wall : walls) {
            if (new Rectangle(pacManX, pacManY, 40, 40).intersects(wall)) {
                if (pacManDirection == 0) pacManX -= pacManSpeed;
                if (pacManDirection == 1) pacManY += pacManSpeed;
                if (pacManDirection == 2) pacManX += pacManSpeed;
                if (pacManDirection == 3) pacManY -= pacManSpeed;
            }
        }

        // Wrap around screen
        if (pacManX > getWidth()) pacManX = 0;
        if (pacManX < 0) pacManX = getWidth() - 40;
        if (pacManY > getHeight()) pacManY = 0;
        if (pacManY < 0) pacManY = getHeight() - 40;
    }

    public void moveGhosts() {
        for (Point ghost : ghosts) {
            if (pacManX > ghost.x) ghost.x += 2;
            if (pacManX < ghost.x) ghost.x -= 2;
            if (pacManY > ghost.y) ghost.y += 2;
            if (pacManY < ghost.y) ghost.y -= 2;
        }
    }

    public void checkCollisions() {
        // Check for ghost collision
        for (Point ghost : ghosts) {
            if (new Rectangle(pacManX, pacManY, 40, 40).intersects(new Rectangle(ghost.x, ghost.y, 40, 40))) {
                gameOver = true;
            }
        }
    }

    public void checkPelletCollision() {
        ArrayList<Point> toRemove = new ArrayList<>();
        for (Point pellet : pellets) {
            if (new Rectangle(pacManX, pacManY, 40, 40).intersects(new Rectangle(pellet.x, pellet.y, 10, 10))) {
                toRemove.add(pellet);
                score += 10;
            }
        }
        pellets.removeAll(toRemove);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT && pacManDirection != 0) pacManDirection = 2;
        if (key == KeyEvent.VK_UP && pacManDirection != 3) pacManDirection = 1;
        if (key == KeyEvent.VK_RIGHT && pacManDirection != 2) pacManDirection = 0;
        if (key == KeyEvent.VK_DOWN && pacManDirection != 1) pacManDirection = 3;
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Pac-Man Clone");
        PacManClone game = new PacManClone();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
