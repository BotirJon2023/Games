import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SimpleAnimationExample extends JPanel implements ActionListener {

    private int x = 0;
    private int y = 50;
    private int xSpeed = 2;
    private final int SQUARE_SIZE = 30;

    public SimpleAnimationExample() {
        // Set preferred size for the panel
        setPreferredSize(new Dimension(600, 200));
        // Create a Timer to trigger updates at regular intervals
        // 16 milliseconds ~ 60 frames per second (1000ms / 60 frames)
        Timer timer = new Timer(16, this);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Clears the background
        Graphics2D g2d = (Graphics2D) g;

        // Set rendering hints for smoother graphics (optional but good practice)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw the moving square
        g2d.setColor(Color.BLUE);
        g2d.fillRect(x, y, SQUARE_SIZE, SQUARE_SIZE);

        // You could draw other elements here for a game
        // g2d.setColor(Color.GREEN);
        // g2d.drawLine(0, 100, getWidth(), 100);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Update the square's position
        x += xSpeed;

        // Reverse direction if it hits the edges
        if (x + SQUARE_SIZE > getWidth() || x < 0) {
            xSpeed *= -1;
        }

        // Request a repaint to update the screen
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Simple Animation Example (No JavaFX)");
        SimpleAnimationExample animationPanel = new SimpleAnimationExample();
        frame.add(animationPanel);
        frame.pack(); // Adjusts frame size based on panel's preferred size
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); // Center the frame
        frame.setVisible(true);
    }
}