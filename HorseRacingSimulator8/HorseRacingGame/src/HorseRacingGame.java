import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class HorseRacingGame extends JPanel implements ActionListener, KeyListener {

    // Window
    private static final int WIDTH = 900;
    private static final int HEIGHT = 500;

    // Race
    private static final int NUM_HORSES = 5;
    private static final int START_X = 50;
    private static final int FINISH_X = 800;
    private static final int TRACK_TOP = 60;
    private static final int TRACK_HEIGHT = 60;

    private Horse[] horses;
    private Timer timer;
    private boolean raceRunning = false;
    private boolean raceFinished = false;
    private Horse winner = null;
    private Random random = new Random();

    // Player
    private int playerBetIndex = -1;
    private int playerBalance = 100;
    private int playerStake = 10;

    public HorseRacingGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(0, 130, 0));
        initHorses();

        timer = new Timer(40, this); // ~25 fps
        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();
    }

    private void initHorses() {
        horses = new Horse[NUM_HORSES];
        for (int i = 0; i < NUM_HORSES; i++) {
            int laneY = TRACK_TOP + i * TRACK_HEIGHT;
            Color color = Color.getHSBColor(i / (float) NUM_HORSES, 0.8f, 0.9f);
            horses[i] = new Horse("Horse " + (i + 1), START_X, laneY, color);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!raceRunning) return;
        updateRace();
        repaint();
    }

    private void updateRace() {
        if (raceFinished) return;

        for (Horse h : horses) {
            int baseSpeed = 3;
            int variance = random.nextInt(4); // 0–3
            int boost = random.nextInt(100) < 5 ? 4 : 0; // occasional burst
            h.x += baseSpeed + variance + boost;

            if (h.x >= FINISH_X && !raceFinished) {
                raceFinished = true;
                raceRunning = false;
                winner = h;
                resolveBet();
            }
        }
    }

    private void resolveBet() {
        if (playerBetIndex < 0 || playerBetIndex >= horses.length) return;
        if (horses[playerBetIndex] == winner) {
            playerBalance += playerStake * 2;
        } else {
            playerBalance -= playerStake;
            if (playerBalance < 0) playerBalance = 0;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawTrack(g);
        drawHorses(g);
        drawHUD(g);
        if (!raceRunning && !raceFinished) {
            drawStartScreen(g);
        } else if (raceFinished) {
            drawResultScreen(g);
        }
    }

    private void drawTrack(Graphics g) {
        g.setColor(new Color(140, 90, 40));
        g.fillRect(0, TRACK_TOP - 20, WIDTH, NUM_HORSES * TRACK_HEIGHT + 40);

        g.setColor(Color.WHITE);
        for (int i = 0; i <= NUM_HORSES; i++) {
            int y = TRACK_TOP + i * TRACK_HEIGHT - 10;
            g.drawLine(0, y, WIDTH, y);
        }

        g.setColor(Color.YELLOW);
        g.fillRect(FINISH_X, TRACK_TOP - 20, 5, NUM_HORSES * TRACK_HEIGHT + 40);
    }

    private void drawHorses(Graphics g) {
        for (Horse h : horses) {
            h.draw(g);
        }
    }

    private void drawHUD(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Balance: " + playerBalance, 20, 20);
        g.drawString("Stake: " + playerStake + "  (Up/Down to change)", 150, 20);
        g.drawString("Bet: " + (playerBetIndex >= 0 ? horses[playerBetIndex].name : "none")
                + "  (1-" + NUM_HORSES + " to choose)", 20, 40);
        g.drawString("Press SPACE to start / restart the race", 400, 20);
    }

    private void drawStartScreen(Graphics g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 26));
        g.drawString("Horse Racing Simulator", 280, 200);

        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("Choose your horse with keys 1-" + NUM_HORSES + ".", 230, 240);
        g.drawString("Adjust stake with Up/Down arrows.", 260, 270);
        g.drawString("Press SPACE to start the race.", 270, 300);
    }

    private void drawResultScreen(Graphics g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 26));
        if (winner != null) {
            g.drawString("Winner: " + winner.name, 330, 200);
        }
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("Your balance: " + playerBalance, 340, 240);
        g.drawString("Press SPACE to race again.", 320, 270);
    }

    private void resetRace() {
        raceFinished = false;
        winner = null;
        initHorses();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
            int index = code - KeyEvent.VK_1;
            if (index < NUM_HORSES) {
                playerBetIndex = index;
            }
        }

        if (code == KeyEvent.VK_SPACE) {
            if (!raceRunning) {
                if (raceFinished) {
                    resetRace();
                }
                raceRunning = true;
                timer.start();
            }
        }

        if (code == KeyEvent.VK_UP) {
            playerStake += 5;
        } else if (code == KeyEvent.VK_DOWN) {
            playerStake -= 5;
            if (playerStake < 0) playerStake = 0;
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    // Simple inner class for a horse
    private static class Horse {
        String name;
        int x;
        int laneY;
        Color color;

        Horse(String name, int x, int laneY, Color color) {
            this.name = name;
            this.x = x;
            this.laneY = laneY;
            this.color = color;
        }

        void draw(Graphics g) {
            int y = laneY - 30;
            g.setColor(color);
            g.fillRoundRect(x, y, 60, 30, 10, 10);
            g.setColor(Color.BLACK);
            g.drawString(name, x, y - 5);
        }
    }

    // Entry point
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Horse Racing Simulator");
            HorseRacingGame panel = new HorseRacingGame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
