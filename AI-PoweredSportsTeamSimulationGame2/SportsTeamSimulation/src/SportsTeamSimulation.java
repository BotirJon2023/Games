import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class SportsTeamSimulation extends JPanel implements ActionListener {

    private final int WIDTH = 800;
    private final int HEIGHT = 600;
    private final int PLAYER_SIZE = 20;
    private final int BALL_SIZE = 10;

    private int[] teamAPlayersX = new int[5];
    private int[] teamAPlayersY = new int[5];
    private int[] teamBPlayersX = new int[5];
    private int[] teamBPlayersY = new int[5];
    private int ballX = WIDTH / 2;
    private int ballY = HEIGHT / 2;
    private int ballSpeedX = 2;
    private int ballSpeedY = 2;

    private int teamAScore = 0;
    private int teamBScore = 0;

    private Timer timer;
    private Random random = new Random();

    public SportsTeamSimulation() {
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));

        // Initialize team A players
        for (int i = 0; i < 5; i++) {
            teamAPlayersX[i] = random.nextInt(200);
            teamAPlayersY[i] = random.nextInt(HEIGHT);
        }

        // Initialize team B players
        for (int i = 0; i < 5; i++) {
            teamBPlayersX[i] = random.nextInt(200) + 600;
            teamBPlayersY[i] = random.nextInt(HEIGHT);
        }

        timer = new Timer(16, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);

        // Draw team A players
        for (int i = 0; i < 5; i++) {
            g.fillOval(teamAPlayersX[i], teamAPlayersY[i], PLAYER_SIZE, PLAYER_SIZE);
        }

        // Draw team B players
        for (int i = 0; i < 5; i++) {
            g.fillOval(teamBPlayersX[i], teamBPlayersY[i], PLAYER_SIZE, PLAYER_SIZE);
        }

        // Draw ball
        g.fillOval(ballX, ballY, BALL_SIZE, BALL_SIZE);

        // Draw scores
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Team A: " + teamAScore, 10, 30);
        g.drawString("Team B: " + teamBScore, WIDTH - 150, 30);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Move team A players
        for (int i = 0; i < 5; i++) {
            if (teamAPlayersX[i] < ballX) {
                teamAPlayersX[i] += 1;
            } else if (teamAPlayersX[i] > ballX) {
                teamAPlayersX[i] -= 1;
            }
            if (teamAPlayersY[i] < ballY) {
                teamAPlayersY[i] += 1;
            } else if (teamAPlayersY[i] > ballY) {
                teamAPlayersY[i] -= 1;
            }
        }

        // Move team B players
        for (int i = 0; i < 5; i++) {
            if (teamBPlayersX[i] < ballX) {
                teamBPlayersX[i] += 1;
            } else if (teamBPlayersX[i] > ballX) {
                teamBPlayersX[i] -= 1;
            }
            if (teamBPlayersY[i] < ballY) {
                teamBPlayersY[i] += 1;
            } else if (teamBPlayersY[i] > ballY) {
                teamBPlayersY[i] -= 1;
            }
        }

        // Move ball
        ballX += ballSpeedX;
        ballY += ballSpeedY;

        // Ball collision with walls
        if (ballY < 0 || ballY > HEIGHT - BALL_SIZE) {
            ballSpeedY *= -1;
        }

        // Ball collision with team A goal
        if (ballX < 0) {
            teamBScore++;
            ballX = WIDTH / 2;
            ballY = HEIGHT / 2;
        }

        // Ball collision with team B goal