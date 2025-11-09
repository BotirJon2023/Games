import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class BMXGame extends JPanel implements KeyListener {
    // Game constants
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    public static final int BIKE_WIDTH = 20;
    public static final int BIKE_HEIGHT = 20;
    public static final int GRAVITY = 1;

    // Bike properties
    private int bikeX = WIDTH / 2;
    private int bikeY = HEIGHT / 2;
    private int bikeVelX = 0;
    private int bikeVelY = 0;
    private boolean isJumping = false;

    // Obstacles
    private int obstacleX = WIDTH;
    private int obstacleY = HEIGHT - 50;
    private int obstacleWidth = 50;
    private int obstacleHeight = 50;

    // Game state
    private boolean isGameOver = false;
    private int score = 0;

    // Graphics
    private BufferedImage bikeImage;
    private BufferedImage obstacleImage;

    public BMXGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        // Load images
        try {
            bikeImage = ImageIO.read(new File("bike.png"));
            obstacleImage = ImageIO.read(new File("obstacle.png"));
        } catch (IOException e) {
            System.out.println("Error loading images: " + e.getMessage());
        }

        // Game loop
        Timer timer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGame();
                repaint();
            }
        });
        timer.start();
    }

    private void updateGame() {
        // Update bike position
        bikeX += bikeVelX;
        bikeY += bikeVelY;

        // Apply gravity
        if (bikeY < HEIGHT - BIKE_HEIGHT) {
            bikeVelY += GRAVITY;
        } else {
            bikeY = HEIGHT - BIKE_HEIGHT;
            bikeVelY = 0;
            isJumping = false;
        }

        // Check collision with obstacle
        if (bikeX + BIKE_WIDTH > obstacleX && bikeX < obstacleX + obstacleWidth
                && bikeY + BIKE_HEIGHT > obstacleY && bikeY < obstacleY + obstacleHeight) {
            isGameOver = true;
        }

        // Update obstacle position
        obstacleX -= 5;
        if (obstacleX < -obstacleWidth) {
            obstacleX = WIDTH;
            score++;
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw bike
        g.drawImage(bikeImage, bikeX, bikeY, BIKE_WIDTH, BIKE_HEIGHT, null);

        // Draw obstacle
        g.drawImage(obstacleImage, obstacleX, obstacleY, obstacleWidth, obstacleHeight, null);

        // Draw score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Score: " + score, 10, 30);

        // Draw game over message
        if (isGameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.drawString("Game Over!", WIDTH / 2 - 100, HEIGHT / 2);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                bikeVelX = -5;
                break;
            case KeyEvent.VK_RIGHT:
                bikeVelX = 5;
                break;
            case KeyEvent.VK_SPACE:
                if (!isJumping) {
                    bikeVelY = -20;
                    isJumping = true;
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
                bikeVelX = 0;
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("BMX Stunt Racing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new BMXGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}