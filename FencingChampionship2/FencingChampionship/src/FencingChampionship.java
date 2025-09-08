import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class FencingChampionship extends JPanel {

    // Game constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int FENCER_WIDTH = 50;
    private static final int FENCER_HEIGHT = 100;
    private static final int PISTE_WIDTH = 600;
    private static final int PISTE_HEIGHT = 20;
    private static final int SCORE_FONT_SIZE = 24;

    // Game variables
    private int playerScore = 0;
    private int opponentScore = 0;
    private int playerX = WIDTH / 2;
    private int opponentX = WIDTH / 2;
    private int playerDirection = 0;
    private int opponentDirection = 0;
    private boolean playerAttacking = false;
    private boolean opponentAttacking = false;
    private boolean gameOver = false;
    private Timer gameTimer;
    private Random random = new Random();

    // Constructor
    public FencingChampionship() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.WHITE);
        setFocusable(true);
        requestFocus();

        // Key listener for player movement and attack
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    playerDirection = -1;
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    playerDirection = 1;
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    playerAttacking = true;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    playerDirection = 0;
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    playerAttacking = false;
                }
            }
        });

        // Game timer for updates and animation
        gameTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGame();
                repaint();
            }
        });
        gameTimer.start();
    }

    // Update game state
    private void updateGame() {
        // Move player
        playerX += playerDirection * 5;
        if (playerX < 0) {
            playerX = 0;
        } else if (playerX > WIDTH - FENCER_WIDTH) {
            playerX = WIDTH - FENCER_WIDTH;
        }

        // Move opponent
        opponentX += opponentDirection * 3;
        if (opponentX < 0) {
            opponentX = 0;
        } else if (opponentX > WIDTH - FENCER_WIDTH) {
            opponentX = WIDTH - FENCER_WIDTH;
        }

        // Opponent AI
        if (random.nextInt(100) < 5) {
            opponentDirection = random.nextBoolean() ? 1 : -1;
        } else if (random.nextInt(100) < 10) {
            opponentDirection = 0;
        }
        if (random.nextInt(100) < 2) {
            opponentAttacking = true;
        } else {
            opponentAttacking = false;
        }

        // Collision detection and scoring
        if (playerAttacking && Math.abs(playerX - opponentX) < FENCER_WIDTH) {
            playerScore++;
        }
        if (opponentAttacking && Math.abs(playerX - opponentX) < FENCER_WIDTH) {
            opponentScore++;
        }

        // Game over condition
        if (playerScore >= 10 || opponentScore >= 10) {
            gameOver = true;
            gameTimer.stop();
        }
    }

    // Draw game elements
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw piste
        g2d.setColor(Color.BLACK);
        g2d.fillRect(WIDTH / 2 - PISTE_WIDTH / 2, HEIGHT - PISTE_HEIGHT - 20, PISTE_WIDTH, PISTE_HEIGHT);

        // Draw player
        g2d.setColor(Color.BLUE);
        g2d.fillRect(playerX, HEIGHT - FENCER_HEIGHT - PISTE_HEIGHT - 40, FENCER_WIDTH, FENCER_HEIGHT);
        if (playerAttacking) {
            g2d.setColor(Color.RED);
            g2d.drawLine(playerX + FENCER_WIDTH / 2, HEIGHT - FENCER_HEIGHT - PISTE_HEIGHT - 40, playerX + FENCER_WIDTH / 2, HEIGHT - PISTE_HEIGHT - 20);
        }

        // Draw opponent
        g2d.setColor(Color.RED);
        g2d.fillRect(opponentX, HEIGHT - FENCER_HEIGHT - PISTE_HEIGHT - 40, FENCER_WIDTH, FENCER_HEIGHT);
        if (opponentAttacking) {
            g2d.setColor(Color.BLUE);
            g2d.drawLine(opponentX + FENCER_WIDTH / 2, HEIGHT - FENCER_HEIGHT - PISTE_HEIGHT - 40, opponentX + FENCER_WIDTH / 2, HEIGHT - PISTE_HEIGHT - 20);
        }

        // Draw scores
        g2d.setFont(new Font("Arial", Font.BOLD, SCORE_FONT_SIZE));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Player: " + playerScore, 20, 30);
        g2d.drawString("Opponent: " + opponentScore, WIDTH - 150, 30);

        // Draw game over text
        if (gameOver) {
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            if (playerScore >= 10) {
                g2d.drawString("You win!", WIDTH / 2 - 100, HEIGHT / 2);
            } else {
                g2d.drawString("Opponent wins!", WIDTH / 2 - 150, HEIGHT / 2);
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Fencing Championship");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new FencingChampionship());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}