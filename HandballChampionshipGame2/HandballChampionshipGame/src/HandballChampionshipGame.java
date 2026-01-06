// HandballChampionshipGame.java

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class HandballChampionshipGame extends JPanel implements ActionListener {

    // Constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int BALL_SIZE = 20;
    private static final int TEAM_SIZE = 7;

    // Team data
    private String teamAName;
    private String teamBName;
    private int teamAScore;
    private int teamBScore;

    // Game state
    private int ballX;
    private int ballY;
    private int ballVelX;
    private int ballVelY;
    private int[][] teamAPositions;
    private int[][] teamBPositions;
    private boolean gameRunning;

    // Timer
    private Timer timer;

    public HandballChampionshipGame(String teamAName, String teamBName) {
        this.teamAName = teamAName;
        this.teamBName = teamBName;
        this.teamAScore = 0;
        this.teamBScore = 0;

        // Initialize team positions
        teamAPositions = new int[TEAM_SIZE][2];
        teamBPositions = new int[TEAM_SIZE][2];
        for (int i = 0; i < TEAM_SIZE; i++) {
            teamAPositions[i][0] = new Random().nextInt(WIDTH / 2 - 50) + 50;
            teamAPositions[i][1] = new Random().nextInt(HEIGHT - 100) + 50;
            teamBPositions[i][0] = new Random().nextInt(WIDTH / 2 - 50) + WIDTH / 2 + 50;
            teamBPositions[i][1] = new Random().nextInt(HEIGHT - 100) + 50;
        }

        // Initialize ball position and velocity
        ballX = WIDTH / 2;
        ballY = HEIGHT / 2;
        ballVelX = new Random().nextInt(5) + 1;
        ballVelY = new Random().nextInt(5) + 1;

        gameRunning = true;

        timer = new Timer(1000 / 60, this);
        timer.start();

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.WHITE);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw teams
        g.setColor(Color.BLUE);
        for (int[] pos : teamAPositions) {
            g.fillOval(pos[0], pos[1], 30, 30);
        }
        g.setColor(Color.RED);
        for (int[] pos : teamBPositions) {
            g.fillOval(pos[0], pos[1], 30, 30);
        }

        // Draw ball
        g.setColor(Color.BLACK);
        g.fillOval(ballX, ballY, BALL_SIZE, BALL_SIZE);

        // Draw scores
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString(teamAName + ": " + teamAScore, 50, 50);
        g.drawString(teamBName + ": " + teamBScore, WIDTH - 200, 50);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning) {
            // Update ball position
            ballX += ballVelX;
            ballY += ballVelY;

            // Collision with walls
            if (ballX < 0 || ballX > WIDTH - BALL_SIZE) {
                ballVelX = -ballVelX;
            }
            if (ballY < 0 || ballY > HEIGHT - BALL_SIZE) {
                ballVelY = -ballVelY;
            }

            // Collision with teams
            for (int[] pos : teamAPositions) {
                if (isCollision(pos[0], pos[1], ballX, ballY)) {
                    ballVelX = -ballVelX;
                    if (new Random().nextInt(10) < 3) {
                        teamAScore++;
                    }
                }
            }
            for (int[] pos : teamBPositions) {
                if (isCollision(pos[0], pos[1], ballX, ballY)) {
                    ballVelX = -ballVelX;
                    if (new Random().nextInt(10) < 3) {
                        teamBScore++;
                    }
                }
            }

            // Check game over
            if (teamAScore >= 10 || teamBScore >= 10) {
                gameRunning = false;
                timer.stop();
            }

            repaint();
        }
    }

    private boolean isCollision(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2)) < 30;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Handball Championship Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new HandballChampionshipGame("Team A", "Team B"));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}