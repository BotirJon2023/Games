// TrackAndFieldGame.java

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class TrackAndFieldGame extends JPanel {

    // Game constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int ATHLETE_WIDTH = 20;
    private static final int ATHLETE_HEIGHT = 20;
    private static final int FINISH_LINE = WIDTH - 100;

    // Athlete properties
    private int athleteX, athleteY;
    private int athleteSpeed;
    private boolean isRunning;

    // Event properties
    private String event;
    private int eventTime;
    private int score;

    // Game state
    private String gameState;

    public TrackAndFieldGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.WHITE);
        setFocusable(true);

        // Initialize game state
        gameState = "start";
        event = "";
        eventTime = 0;
        score = 0;

        // Initialize athlete properties
        athleteX = 50;
        athleteY = HEIGHT / 2;
        athleteSpeed = 2;
        isRunning = false;

        // Add key listener
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && gameState.equals("start")) {
                    startGame();
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE && gameState.equals("running")) {
                    athleteSpeed = 5;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && gameState.equals("running")) {
                    athleteSpeed = 2;
                }
            }
        });

        // Start game loop
        Timer timer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGame();
                repaint();
            }
        });
        timer.start();
    }

    private void startGame() {
        gameState = "running";
        event = getRandomEvent();
        eventTime = 0;
        score = 0;
        athleteX = 50;
        athleteY = HEIGHT / 2;
        isRunning = true;
    }

    private String getRandomEvent() {
        String[] events = {"100m Dash", "Long Jump", "Shot Put"};
        return events[new Random().nextInt(events.length)];
    }

    private void updateGame() {
        if (gameState.equals("running")) {
            eventTime++;
            if (event.equals("100m Dash")) {
                athleteX += athleteSpeed;
                if (athleteX >= FINISH_LINE) {
                    score = 100 - eventTime / 10;
                    gameState = "result";
                }
            } else if (event.equals("Long Jump")) {
                athleteX += athleteSpeed;
                if (athleteX >= FINISH_LINE / 2) {
                    athleteY -= 5;
                } else if (athleteX >= FINISH_LINE) {
                    score = athleteX / 10;
                    gameState = "result";
                }
            } else if (event.equals("Shot Put")) {
                athleteX += athleteSpeed;
                if (athleteX >= FINISH_LINE) {
                    score = athleteX / 10;
                    gameState = "result";
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (gameState.equals("start")) {
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("Track and Field Sports", WIDTH / 2 - 150, HEIGHT / 2 - 50);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Press Space to start", WIDTH / 2 - 100, HEIGHT / 2);
        } else if (gameState.equals("running")) {
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString(event, 10, 30);
            g.drawString("Time: " + eventTime / 10 + "s", 10, 60);
            g.fillOval(athleteX, athleteY, ATHLETE_WIDTH, ATHLETE_HEIGHT);
            g.drawLine(FINISH_LINE, 0, FINISH_LINE, HEIGHT);
        } else if (gameState.equals("result")) {
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("Result: " + score, WIDTH / 2 - 100, HEIGHT / 2);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Press Space to restart", WIDTH / 2 - 100, HEIGHT / 2 + 50);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Track and Field Sports");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new TrackAndFieldGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}