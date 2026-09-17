import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

class GamePanel extends JPanel {

    // Screen size
    public static final int WIDTH = 1024;
    public static final int HEIGHT = 576;

    private Timer timer;
    private final int FPS = 60;
    private final boolean twoPlayers;

    private Bike player1;
    private Bike player2; // or AI bike
    private Track track;

    // Input flags
    private boolean p1Left, p1Right, p1Jump;
    private boolean p2Left, p2Right, p2Jump;

    public GamePanel(boolean twoPlayers) {
        this.twoPlayers = twoPlayers;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        requestFocusInWindow();

        initGameObjects();
        initInput();
    }

    private void initGameObjects() {
        track = new Track();

        // Start positions
        player1 = new Bike(150, 300, Color.RED, "Player 1");
        if (twoPlayers) {
            player2 = new Bike(150, 340, Color.BLUE, "Player 2");
        } else {
            player2 = new Bike(150, 340, Color.GREEN, "Computer");
            player2.setComputerControlled(true);
        }
    }

    private void initInput() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                // Player 1: A/D to move, W to jump
                if (code == KeyEvent.VK_A) p1Left = true;
                if (code == KeyEvent.VK_D) p1Right = true;
                if (code == KeyEvent.VK_W) p1Jump = true;

                // Player 2: LEFT/RIGHT to move, UP to jump
                if (code == KeyEvent.VK_LEFT) p2Left = true;
                if (code == KeyEvent.VK_RIGHT) p2Right = true;
                if (code == KeyEvent.VK_UP) p2Jump = true;
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_A) p1Left = false;
                if (code == KeyEvent.VK_D) p1Right = false;
                if (code == KeyEvent.VK_W) p1Jump = false;

                if (code == KeyEvent.VK_LEFT) p2Left = false;
                if (code == KeyEvent.VK_RIGHT) p2Right = false;
                if (code == KeyEvent.VK_UP) p2Jump = false;
            }
        });
    }

    public void startGame() {
        timer = new Timer(1000 / FPS, e -> gameLoop());
        timer.start();
    }

    private void gameLoop() {
        updateGame();
        repaint();
    }

    private void updateGame() {
        // Player 1 input
        player1.handleInput(p1Left, p1Right, p1Jump);

        // Player 2 input or AI
        if (player2.isComputerControlled()) {
            // Simple AI: follow player1 and jump on hills
            player2.aiUpdate(track, player1);
        } else {
            player2.handleInput(p2Left, p2Right, p2Jump);
        }

        // Physics & track interaction
        player1.update(track);
        player2.update(track);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Enable antialiasing for smoother visuals
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background (sky + mountains)
        drawBackground(g2);

        // Draw track
        track.draw(g2);

        // Draw bikes
        player1.draw(g2);
        player2.draw(g2);

        // HUD
        drawHUD(g2);
    }

    private void drawBackground(Graphics2D g2) {
        // Sky gradient
        GradientPaint sky = new GradientPaint(0, 0, new Color(80, 140, 255),
                0, HEIGHT, new Color(10, 30, 80));
        g2.setPaint(sky);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Simple mountains
        g2.setColor(new Color(60, 60, 60));
        g2.fillPolygon(new int[]{0, 200, 400}, new int[]{HEIGHT, 250, HEIGHT}, 3);
        g2.fillPolygon(new int[]{300, 550, 800}, new int[]{HEIGHT, 200, HEIGHT}, 3);
    }

    private void drawHUD(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 18));
        g2.drawString(player1.getName() + " speed: " +
                String.format("%.1f", player1.getSpeed()), 20, 30);
        g2.drawString(player2.getName() + " speed: " +
                String.format("%.1f", player2.getSpeed()), 20, 55);

        String mode = twoPlayers ? "Mode: Two Players" : "Mode: Player vs Computer";
        g2.drawString(mode, WIDTH - 260, 30);
    }
}
