import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class VirtualGolfGame extends JPanel implements ActionListener, KeyListener {

    // Window
    JFrame frame;
    Timer timer;

    // Ball properties
    double ballX = 100, ballY = 400;
    double velocityX = 0, velocityY = 0;
    boolean ballMoving = false;

    // Game state
    int currentPlayer = 1;
    boolean vsComputer = false;

    // Input
    double angle = 45;
    double power = 20;

    // Environment
    double wind = 0;
    Random rand = new Random();

    // Target (hole)
    int holeX = 700;
    int holeY = 420;

    // Scores
    int strokesP1 = 0;
    int strokesP2 = 0;

    // Animation
    double flagWave = 0;

    public VirtualGolfGame() {
        frame = new JFrame("🌊 Virtual Golf Challenge");
        frame.setSize(900, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.addKeyListener(this);
        frame.setVisible(true);

        timer = new Timer(16, this);
        timer.start();

        chooseMode();
        generateWind();
    }

    void chooseMode() {
        String[] options = {"2 Players", "Play vs Computer"};
        int choice = JOptionPane.showOptionDialog(
                frame,
                "Choose Game Mode",
                "Mode",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        vsComputer = (choice == 1);
    }

    void generateWind() {
        wind = rand.nextDouble() * 2 - 1; // -1 to 1
    }

    void shootBall() {
        double rad = Math.toRadians(angle);
        velocityX = power * Math.cos(rad);
        velocityY = -power * Math.sin(rad);
        ballMoving = true;

        if (currentPlayer == 1) strokesP1++;
        else strokesP2++;
    }

    void updatePhysics() {
        if (ballMoving) {
            ballX += velocityX + wind;
            ballY += velocityY;

            velocityY += 0.5; // gravity
            velocityX *= 0.99; // friction

            // ground collision
            if (ballY > 420) {
                ballY = 420;
                velocityY *= -0.3;
                velocityX *= 0.8;

                if (Math.abs(velocityX) < 0.5 && Math.abs(velocityY) < 0.5) {
                    ballMoving = false;
                    nextTurn();
                }
            }
        }
    }

    void nextTurn() {
        // Check win
        if (Math.abs(ballX - holeX) < 15 && Math.abs(ballY - holeY) < 15) {
            String winner = (currentPlayer == 1) ? "Player 1" : "Player 2";
            JOptionPane.showMessageDialog(frame, winner + " scored! 🎉");
            resetGame();
            return;
        }

        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        generateWind();

        if (vsComputer && currentPlayer == 2) {
            computerTurn();
        }
    }

    void computerTurn() {
        angle = 30 + rand.nextInt(40);
        power = 15 + rand.nextInt(20);

        Timer aiTimer = new Timer(1000, e -> shootBall());
        aiTimer.setRepeats(false);
        aiTimer.start();
    }

    void resetGame() {
        ballX = 100;
        ballY = 400;
        strokesP1 = 0;
        strokesP2 = 0;
        currentPlayer = 1;
        generateWind();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updatePhysics();
        flagWave += 0.1;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        // 🌤 Sky
        g2.setColor(new Color(135, 206, 235));
        g2.fillRect(0, 0, 900, 300);

        // 🌊 Ocean
        g2.setColor(new Color(0, 105, 148));
        g2.fillRect(0, 300, 900, 100);

        // 🏖 Sand
        g2.setColor(new Color(194, 178, 128));
        g2.fillRect(0, 400, 900, 50);

        // 🌱 Grass
        g2.setColor(new Color(34, 139, 34));
        g2.fillRect(0, 450, 900, 150);

        // 🕳 Hole
        g2.setColor(Color.BLACK);
        g2.fillOval(holeX, holeY, 20, 10);

        // 🚩 Flag animation
        int flagHeight = 60;
        g2.setColor(Color.WHITE);
        g2.drawLine(holeX + 10, holeY, holeX + 10, holeY - flagHeight);

        int waveOffset = (int)(Math.sin(flagWave) * 10);
        g2.setColor(Color.RED);
        g2.fillPolygon(
                new int[]{holeX + 10, holeX + 40 + waveOffset, holeX + 10},
                new int[]{holeY - flagHeight, holeY - flagHeight + 10, holeY - flagHeight + 20},
                3
        );

        // 🏌️ Ball
        g2.setColor(Color.WHITE);
        g2.fillOval((int) ballX, (int) ballY, 10, 10);

        // UI
        g2.setColor(Color.BLACK);
        g2.drawString("Player: " + currentPlayer, 20, 20);
        g2.drawString("Angle: " + (int) angle, 20, 40);
        g2.drawString("Power: " + (int) power, 20, 60);
        g2.drawString("Wind: " + String.format("%.2f", wind), 20, 80);

        g2.drawString("P1 Strokes: " + strokesP1, 700, 20);
        g2.drawString("P2 Strokes: " + strokesP2, 700, 40);

        g2.drawString("Controls: ↑↓ Angle | ←→ Power | SPACE Shoot", 250, 20);
    }

    // 🎮 Controls
    @Override
    public void keyPressed(KeyEvent e) {
        if (ballMoving) return;

        if (e.getKeyCode() == KeyEvent.VK_UP) angle += 2;
        if (e.getKeyCode() == KeyEvent.VK_DOWN) angle -= 2;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) power += 1;
        if (e.getKeyCode() == KeyEvent.VK_LEFT) power -= 1;

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            shootBall();
        }

        angle = Math.max(10, Math.min(80, angle));
        power = Math.max(5, Math.min(50, power));
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        new VirtualGolfGame();
    }
}